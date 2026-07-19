# 03 — Vectorized execution mechanics

Goal: the batch layer extracts the memory-level parallelism and allocation discipline the slot-major
layout was built for: batched hash probes with independent loads in flight, no per-row shared-state
writes, no physical replication of carried columns, and batch sizes chosen by measurement rather than
history.


## Current state

`NativeBatch` is slot-major (`slots[slot * capacity + row]`, `LmdbNativeBatch.java:83-89`), default
capacity 1024 (`:33`). The hash join probe is scalar and dependent-load-serialized: one left row is
hashed, probed, and its chain chased before the next row is touched
(`LmdbNativeHashJoin.java:146-149`; `PrimitiveHashJoinTable.lookup:328-338`; `hash(...):379-385`).
The table layout compounds it: `keys` is jagged `long[][]` (`:280, :292, :415`), so each key compare in
`matches` (`:362-369`) is a double load, on top of `occupied[bucket]` (`byte[]`, `:331`) and
`heads[bucket]` (`int[]`, `:333`) — three to four distinct arrays per bucket versus one slot read in
Kuzu's table (`join_hash_table.cpp:148-151` resolves ALL slot pointers in one independent-load loop; no
explicit prefetch anywhere — latency hiding is purely many misses in flight).

`PrimitiveTupleTable` (group/distinct/keyed-matches) has the same scalar shape (`find`/`findOrInsert`,
`LmdbNativePrimitiveTupleTable.java:54-92`) plus two unconditional static `AtomicLong`s incremented once
per row (`PROBES`/`INSERTIONS`, `:30-31`, fired `:55, :71, :83`) — write-only in production, read by two
test classes; every parallel aggregation worker RMWs the same two cache lines per input row while the
correct gated per-instance pattern sits in the same file pair
(`LmdbNativeGroupTable.commitRowMetrics:405-412`).

Carried columns are physically replicated at all six `NativeBatch` allocation sites; `boundMask` is
recomputed from scratch per row (`recomputeBoundMask` inside the filter loops,
`LmdbNativeSpecialization.java:285-286`) though it is constant across a batch whose bound slot set is
fixed. There is no identity fast path for unfiltered batches — every batch pays selection indirection.
Fill batches are 64 rows in the factorized tail and left-join probe against 1024 elsewhere.


## Work item 1 — Kill the hot-loop atomics (prerequisite for item 2)

Convert `PROBES`/`INSERTIONS` (and the parallel `PRIMITIVE_ROWS`/`OBJECT_ROWS` statics) to per-instance
plain longs, gated like `rowMetrics` (`LmdbNativeGroupTable.java:61-63, :186-187`), flushed once at close
into the existing statics — `LmdbNativePrimitiveGroupingTest:104-302` and
`LmdbNativePackedSortTest:148-258` read the statics and must keep passing unchanged. Scope note: only
TUPLE_COUNTS/TUPLE_STATES modes reach these counters; LONG_* modes pay nothing
(`LmdbNativeGroupTable.java:165-172`). Also fix the inverted gating on the parallel path: worker tables
are built with `rowMetrics = false` (`LmdbNativeParallelAggregation.java:696-697`) — the cheap counters
are off exactly where the expensive atomics fire.

Acceptance: parallel GROUP BY over ≥1M rows shows the counters' cost gone from profile; test suite green.


## Work item 2 — Batched hash-then-probe

Split every hash probe into columnwise passes over the batch:

    pass 1: hash[i]  = mix(keys columns)         — stride-1 reads of slot-major columns
    pass 2: head[i]  = table.head(hash[i])       — independent loads, ~capacity misses in flight
    pass 3: chain walk + key compare per i       — resumable, preserving current mid-chain cursor state

Targets in order: `PrimitiveHashJoinTable` (`LmdbNativeHashJoin.java` — no counter problem, correct
first target; the cursor already carries mid-chain resume state `:98-99`, and bag cardinality is
preserved by the `next[]` walk `:143`); then `PrimitiveTupleTable.find/findOrInsert` consumers —
`NativeDistinctTracker.add:225` and `KeyedMatches` widths 2–4 (`LmdbNativeKeyedMatches.java:131, :143`)
— after item 1. Scratch arrays sized by batch capacity, owned by the cursor, reused across fills.

Flatten the layout while touching it: replace jagged `long[][] keys` with one flat
`long[width * capacity]` array and store an 8-bit hash fingerprint alongside `heads` so most non-matching
chain entries are rejected on one byte compare without touching keys (this also serves the group table —
plan 10 §3). Store the full hash with each entry so growth rehashes nothing (today growth re-derives).

Tests: equivalence property test vs the scalar path across widths 1–4, collisions forced by a
constant-hash test hook; benchmark with build side ≫ L2.
Acceptance: hash-join probe throughput ≥2× at 16M-row build on the join benchmark; DISTINCT and GROUP BY
probe-bound shapes improve proportionally.


## Work item 3 — Batch-constant work hoisted out of the row loop

(a) `boundMask`: when a filter/kernel executes over a batch whose bound slot set is fixed (always true
for pattern-produced batches — the producer binds the same slots every row), compute the mask once per
fill and skip `recomputeBoundMask` per row. The kernel keyed on `(slotCount, readMask)` (plan 02 §1)
receives it as a parameter.
(b) Identity selection: when no filter removed rows, pass a shared identity selection marker instead of
materializing `sel[i] = i` and indirecting every consumer; consumers branch once per batch on
`sel == IDENTITY`.
(c) Unify fill sizes: lift the 64-row fills in the factorized tail and left-join payload probe to the
batch default (1024), making the size a single named constant consulted everywhere, and add an adaptive
clamp: `capacity = min(1024, max(64, budgetBytes / (rowWidth * 8)))` so ultra-wide rows do not blow the
cache — measured, not assumed, via the benchmark matrix in plan 13.

Acceptance: no behavior change (equivalence tests); batch-stage profile shows `recomputeBoundMask`
gone from the hot path.


## Work item 4 — Stop physically replicating carried columns

Design: a `NativeBatch` column becomes either OWNED (a slice of the batch's arena, as today) or SHARED
(a reference to the producing batch's column plus a row-index remap that is the identity when no
selection intervened). Carried columns — slots produced upstream and merely passed through a stage — are
SHARED; only newly produced slots are OWNED. When a stage compacts a selection, SHARED columns get the
compacted remap, not a copy; a copy happens only when a consumer needs a contiguous column
(sort packing, exchange pages, spill) — exactly the sites that already copy today.

This is the RDF4J-shaped equivalent of Kuzu's factorized `DataChunkState` sharing without adopting
flat/unflat semantics (factorization proper is plan 06's concern; this item is purely about not copying
what is already resident).

Steps: introduce the column descriptor on `NativeBatch`; convert the six allocation sites; audit every
direct `slots[...]` indexer to go through the accessor (mechanical; the accessor compiles to the same
arithmetic for OWNED). Keep spill/exchange paths copying — cross-thread pages must own their memory
(`LmdbNativeExchange` output pages, `LmdbNativeParallelPipelines.java:632`).

Risk: this is the most invasive item in this plan; it lands last, behind the equivalence corpus, and its
win case (wide multi-stage chunk pipelines) must show ≥1.3× on the pipeline benchmark to justify keeping
it — otherwise revert (the abstraction must not cost the narrow case anything; verify OWNED-only plans
are performance-neutral first).


## Work item 5 — Specialization coverage after unification

With the kernel serving both engines (plan 02 §1), re-examine what is worth generating: today the kernel
unrolls only the slot copy and still emits an `invokeinterface` per row for `accept`
(`LmdbNativeSpecialization.java:285-286, :313`). Once `selectBatch` exists (plan 02 §1), the generated
kernel's job narrows to fusing copy+mask+`selectBatch` dispatch for the hot `(slotCount, readMask)`
shapes; measure whether generation still pays versus the plain `selectBatch` path and delete it if not —
carrying a bytecode generator that loses to its interpreter is complexity with negative return. Decision
recorded either way in plan 13's rollout table.
