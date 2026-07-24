# 10 — Aggregation and GROUP BY / DISTINCT

Goal: aggregation is memory-safe (budgeted, spilling), primitive far beyond arity 4, parallel including
value-typed DISTINCT aggregates, streaming where order allows, and free of per-row shared-state costs.
Together with plan 06 §5 (key reduction) this makes the idiomatic SPARQL aggregate — many forced group
keys, a few aggregates — a first-class fast path.


## Current state

`NativeGroupTable` mode is chosen by ARITY alone with a cliff at 4 (`LmdbNativeGroupTable.java:112-123`):
above it, GENERIC — a boxed `HashMap<GroupKey, AggState>` with a `storedCopy()` allocation per new group
(`:103-106, :207-216, :256-266`). `PrimitiveTupleTable` throws outside widths 1–4
(`LmdbNativePrimitiveTupleTable.java:40-42`; hand-unrolled `matches` with `default -> false`,
`:131-146`), probes with no stored hash and no fingerprint, and fires two static AtomicLongs per row
(plan 03 §1 removes). Memory: the group table has ZERO caps — unconditional doubling (`:180-182,
:198-200`), a private table per parallel worker (`LmdbNativeParallelAggregation.java:694-697`), a second
full materialization in `results(...)` (`:346-397`), and the single-slot `LongAggStateMap` grows
unbounded (`LmdbNativeRowState.java:131-142`). Parallel merge is serial on the query thread; value-typed
DISTINCT is barred: `parallelMergeable` refuses `spec.distinct && kind != COUNT`
(`LmdbNativeParallelAggregation.java:65-72`) — correct for SUM/AVG (`mergeSum` double-counts across
workers, conceded at `LmdbNativeAggregateState.java:472-478`), groundless for MIN/MAX (no distinct
channel is allocated for extrema, `LmdbNativeSlotOrder.java:252-257`; `mergeExtreme:528-545` merges
exactly). The parallel path already forces every channel to HASH (`allHash`,
`LmdbNativeParallelAggregation.java:695-696` → `LmdbNativeSlotOrder.java:274-275`), so per-group ID sets
exist wherever they are needed for a deferred recompute. Aggregation is fully blocking (all groups
materialize as boxed BindingSets before the first emit); one unsupported aggregate function or non-Var
argument disqualifies the entire Group from native compilation (GROUP_CONCAT and SAMPLE unsupported;
computed arguments unsupported). Streaming hash-free groups exist but are gated to DISTINCT-channel
plans (`LmdbNativeSlotOrder.java:309-310, :254`; `LmdbNativeGroupStep.java:418-450`).


## Work item 1 — Budget and spill

1. Byte-account both tables (group table and `LongAggStateMap`) against the global budget
   (plan 12 §1); all internal state is `long[]`, so accounting is mechanical.
2. Spill tier: on budget refusal, switch to sort-based aggregation — spill (groupKey, partial-state)
   runs through `NativeSpillSort`'s existing run writer (`LmdbNativeSort.java:666-760`), merge runs with
   state-merge on equal keys at emission. Every `AggState` already implements `mergeFrom` (the parallel
   path requires it), so run-merge reuses it; DISTINCT channels spill their ID sets sorted (dedup on
   merge is then a linear pass).
3. Kill the double materialization: `results(...)` re-materializes all groups (`:346-397`) — emit
   incrementally from the table/merged runs (item 4 makes ordering constraints explicit).

Tests: forced-refusal tests at tiny budgets asserting identical results via spill; memory-ceiling test
(N concurrent high-cardinality GROUP BYs stay within budget, plan 12's harness).
Acceptance: `GROUP BY ?s` at 50M distinct groups completes in bounded memory (today: OOM risk);
no measurable regression below the budget.


## Work item 2 — Primitive keys past arity 4

Generalize `PrimitiveTupleTable` to arbitrary width: flat `long[width*capacity]` keys, loop-based
`matches` (the unrolled switch stays for widths ≤4 — measured hot), stored hash + 8-bit fingerprint
(shared design with plans 03 §2 / 09 §1 — one implementation). The GENERIC boxed mode remains only for
keys that are genuinely non-ID (none today — group keys are slot IDs; delete the mode if the audit
confirms, recording the decision). With plan 06 §5's key reduction this makes the arity cliff
unreachable for real queries; without reduction it simply disappears.

Acceptance: 8-key GROUP BY drops the boxed path (dispatch tag + allocation profile); probe throughput
at width 8 within 1.5× of width 2.


## Work item 3 — Batched, fingerprinted group probing

Apply plan 03 §2's batched hash-then-probe to the group table's accumulate loop (TUPLE_COUNTS /
TUPLE_STATES modes): hash the batch columnwise, resolve heads with independent loads, fingerprint-reject,
then accumulate. Depends on plan 03 §1 (atomics removed first). The LONG_* single-slot modes keep their
direct path (`LmdbNativeGroupTable.java:165-172` — already cheap).

Acceptance: probe-bound GROUP BY (many groups, few rows per group) ≥1.5× on the aggregation benchmark.


## Work item 4 — Parallel merge and value-typed DISTINCT

1. MIN/MAX DISTINCT: admit immediately — narrow `parallelMergeable:67` to refuse only
   `distinct && (SUM || AVG)`; extrema merge exactly today (verified: no distinct channel allocated,
   `mergeExtreme` comparator-winner; float/double encounter-order hazard already covered by
   `preserveFloatingEncounterOrder` throwing `EncounterOrderFallback`,
   `LmdbNativeAggregateState.java:195-203`).
2. SUM/AVG DISTINCT: Kuzu's deferred recompute — during parallel accumulation, populate ONLY
   `channelSets[channel]` (skip `addSum`/`addAvg`); at merge, union the ID sets (`mergeFrom:480-484`
   already does) and fold the final sum/avg from the merged set at emission. All preconditions verified:
   `allHash` guarantees the sets exist on the parallel path; the channel value and `AggregateSpec.value`
   are the same expression (`:148-150` vs `:281-283`); `LongHashSet.table` is package-visible for
   finalize-time iteration (`:590-593`). Integer/decimal addition is exact and commutative
   (hash-order-safe); float/double keeps the existing fallback.
3. Partitioned parallel merge: plan 04 §4(b) — group-key hash partitioning makes each group
   single-owner at merge, which also makes the deferred recompute contention-free.

Tests: DISTINCT-aggregate corpus with cross-worker duplicate values (the double-count trap), floats
asserting fallback, ties asserting representative preservation
(`preserveExtremaRepresentative:205-210`).
Acceptance: `SUM(DISTINCT ?v)` / `AVG(DISTINCT ?v)` / MIN/MAX DISTINCT no longer disqualify parallel
aggregation (dispatch tag), with correct results under adversarial duplication.


## Work item 5 — Coverage and streaming

1. Computed aggregate arguments: `SUM(?a * ?b)`, `COUNT(IF(...))` etc. via a computed slot
   (`CopyBinding.computed`, plan 05 §5's machinery) feeding the existing Var-argument path — removes the
   non-Var disqualifier.
2. GROUP_CONCAT (bounded: budget-accounted string accumulation per group, spill-aware via item 1) and
   SAMPLE (trivial: first-value semantics matches encounter-order discipline) join the native set —
   one unsupported aggregate no longer voids native compilation for the whole Group; mixed groups
   compile the supported aggregates natively only when ALL are native (partial-native mixing is not
   attempted — the fallback boundary stays whole-Group, just with a larger native set).
3. Streaming emission: extend the ordered hash-free path's admission (plan 06 §5 already widens it to
   proven-ordered group prefixes); with plan 05 §5(b) order-aware planning, `GROUP BY ?s` over an
   S-ordered scan streams groups with O(1) state — assert with the existing
   `completeGroupPrefix` machinery.
4. COUNT shortcuts: `SELECT (COUNT(*)) WHERE { single pattern }` answers from `tryCount`/prefix sums
   (plan 07 §1's O(1) degree) without iteration — wire the existing `count()` store path into the
   native aggregate compiler's single-pattern case.

Acceptance: aggregate-corpus coverage report shows the native-compilation rate on the benchmark query
set rising from its measured baseline to >90%; `COUNT(*)` single-pattern answers in µs.
