# Critical self-review: phases 7–10 vs the approved plan (2026-07-16)

Scope reviewed: the full uncommitted working tree on `optimize-lmdb` (~1,150 insertions across 24
modified main-source files, 4 new main classes, 7 new test classes, 4 new ExecPlans), implementing
the approved master plan (`~/.claude/plans/make-a-plan-to-declarative-rabbit.md`).

## What was delivered vs planned

Phase 7 (range-partitioned parallel root scans): M1–M3 delivered as designed — bounded
`LmdbRecordIterator` (`LmdbKeyRange`, exclusive upper bounds), interpolate-and-snap split planner,
producer-less `PartitionCursor` mode in both parallel frameworks behind
`rdf4j.lmdb.parallel.rangePartition.enabled`. 20 new storage/planner tests + 5 parity tests; fuzz
green with the flag forced. M4 (benchmark + default flip) in progress this session.

Phase 8 (CSR adjacency cache): M1–M3 delivered — `LmdbCsrAdjacencyCache` (open-addressed key table,
prefix-sum runs, revision-validated, budgeted), probe decoration at the single funnel, engine
awareness (`adjacencyCacheBacked()` memo/hash-build skips), count/has serving. M4's stats half
(min/max + all-ordered-integers) landed; the hash-build-publication half deferred.

Phase 9 (factorized chunk prefix): NOT implemented — by design. The ExecPlan's M0 honesty gate plus
the design analysis (tail branches already own the asymptotics; parent-pointer chunks are a
memory-traffic optimization for wide deep prefixes) place it below the CSR/pushdown successors in
value. Deferred to the successor plan with the M0 measurement as its entry condition.

Phase 10 (ordered numeric ids): M0–M2 + M4-stats delivered — 9 ordered type codes (biased),
store-property writer gate + VERSION 3, decode-free ORDER BY and filter-vs-constant fast paths.
M3 (range pushdown) deferred after discovering the naive design is UNSOUND (see below).

## Defects found by this review (fixed in-tree)

1. CSR cache vs pinned-snapshot readers: untracked datasets (the estimator refresh thread) keep an
   LMDB snapshot across commits, while the cache validates against the LATEST revision — the cache
   could have served data newer than the reader's snapshot. Fixed: `csrEligible` gate on
   `LmdbSailDataset` (probes, count, has). Parallel snapshot sources are lease-protected (commits
   blocked while open) and remain eligible.

## Pre-existing defects found and fixed along the way

1. `ValueStore` nextId recovery used `>> 2` where the layout has 7 low bits — ~32× id-space
   inflation per store reopen (Routine A, failing test first: first=2, second=65).
2. `StoreProperties` setters overwrote the dirty flag, so `store.properties` was never persisted
   for new stores (each reopen re-ran new-store initialization).
3. `ValueStore.initTermIndexes` persisted the raw (possibly null) config term-index string instead
   of the effective set — fatal on reopen once persistence actually worked (#2 masked it).

## Honest criticism — what should have been built differently

1. **The M3 pushdown design in the approved plan was wrong.** "Bounded scan + residual filter is
   always sound" ignored that the scan RESTRICTS candidates: matching doubles/decimals/stored
   numerics outside the integer window are silently dropped, and a post-filter cannot restore rows.
   The plan's reviewer (me) should have caught this at design time; the honest gate is per-predicate
   type statistics, which is why stats landed with Phase 8's sweep and pushdown moved behind them.
2. **CSR entries should serve eligible ROOT SCANS, not just probes.** A `?s p ?v` root over a cached
   predicate can be enumerated from the entry (all keys × runs, index order, zero JNI). That subsumes
   the pushdown for cached predicates, accelerates the Phase 7 partition mode's biggest remaining
   serial cost (the scan itself), and would let PartitionCursor partition over the CSR arrays
   instead of LMDB key ranges. This unification — CSR as the primary in-memory scan/probe substrate,
   with LMDB as the cold path — is the single highest-leverage successor design.
3. **Phase 7's dynamic partition queue is good, but the per-worker LMDB scan still pays the per-triple
   JNI cost.** The original gap analysis said the access path is the floor; partitioning parallelizes
   the floor but does not raise it. Combining P7's partitioning with P8's CSR (partition the entry,
   not the B+tree) multiplies the two wins; that lands naturally with successor item 2.
4. **The CSR budget-vs-heap interaction is coarse.** `max(256 MiB, maxMemory/4)` is a global CAS
   counter with LRU-on-insert; concurrent builds racing the budget can both refuse. Acceptable v1;
   a reservation queue would be cleaner.
5. **HITS/probe-counter atomics on the hot path** (one `incrementAndGet` per probe open) are cheap
   relative to what they replace but measurable at CSR speeds; batch them per-probe like the miss
   counter batches per 256 opens.
6. **Explain-string coverage for CSR engagement was cut** (metrics/counters only) because
   engagement is discovered mid-execution while strategy strings are recorded at dispatch. The
   deferred-strategy plumbing supports late commits; wiring `+csr` through it belongs in the
   successor plan.

## Test evidence summary (this session)

LmdbRecordIteratorRangeBoundsTest 9, TripleStoreRangePartitionTest 6, LmdbNativeRangePartitionedScanTest 5,
existing parallel suites 15+28+35+4, LmdbCsrAdjacencyCacheTest 9, LmdbCsrCacheQueryTest 3,
LmdbOrderedNumericIdsTest 6, ValueStoreTest 26 (incl. new reopen test), LmdbNativeOrderedFactorizedTest 20,
differential fuzz 18 in four configurations (default, rangePartition forced, csrCache forced,
ordered-default stores). Full-module verify pending (one pre-existing pathologically slow
sketch-estimator test documented separately; chip spawned).
