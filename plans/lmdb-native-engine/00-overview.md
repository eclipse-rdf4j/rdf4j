# LMDB Native Query Engine — Master Plan (00: Overview)

Goal: the best native LMDB query evaluation engine — the best mechanisms from Kuzu, the best of the current
code, and the overlooked improvements neither has — such that, on the performance side, the result is a
production-ready DBMS and no further optimization program is needed afterwards.

This is the index and contract for thirteen workstream plans (01–13). Each plan is self-contained: current
state with verified file:line citations, target design, ordered work items with steps, correctness
constraints, tests, and acceptance criteria. All citations were adversarially verified against the
`optimize-lmdb` branch working tree (2026-07-19); line numbers drift as work lands — treat cited
identifiers (class/method/constant names) as the durable anchor and line numbers as the starting hint.

Source analyses (read-only artifacts, retained for reference):
`scratchpad/KUZU-VS-LMDB-FINAL.md` (92 verified gap findings vs Kuzu) and
`scratchpad/composition-analysis.md` (12×12 runtime interaction matrix, ranked losses, correctness-required
exclusions). This plan set supersedes `PLAN-lmdb-unified-composition.md`.


## The one architectural principle

Verified empirically across the whole engine: **mechanisms that are properties of the access path compose
with everything; mechanisms that are whole-plan strategies take turns.** Inlined literal IDs, LMDB
skip-scan, `seekForward`, and CSR service stack under every strategy because they are invisible to
dispatch. Batch joins, morsel parallelism, factorization, adaptive filter placement, and prefix runs are
rungs of one first-match-wins ladder (`NativeRowsIteration.initialize()`,
`evaluation/LmdbNativeRowStep.java:1215-1284`) and exactly one runs per root iteration, selected by source
order, never by cost.

Every workstream applies one of three moves:

1. **Layer, don't rung** — make the mechanism a property of any chain (decorator with an
   `openChainFrom`-shaped entry), following `MultiJoinPlan.openChainFrom`
   (`LmdbNativeJoinPlans.java:170-181`) and `LmdbNativeChunkPipeline.tryOpenPrefixFrom`.
2. **Extend reach** — remove artificial admission gates so an existing engine serves shapes it already
   handles correctly (proven by an existing code path that does exactly that).
3. **Propose-and-cost** — where two strategies' admission sets genuinely overlap, rungs return
   (cursor, cost) and the dispatcher compares, instead of first-match-wins.


## Scope exclusions (binding for all workstreams)

- **Sketch estimator and cascades-style join-order enumeration are out of scope.** Do not modify
  `LmdbSketchJoinOptimizer.java`, the sketch machinery in `LmdbEvaluationStatistics.java`,
  `LmdbFilterSelectivityStats.java`, `SketchJoinOrderPlanner`, `ParetoJoinMemoPlanner`, or the
  `estimate/` subpackage's sketch classes. Where the gap analysis recommended planner-DP changes
  (interesting orders in the DP, bushy DP enumeration, factorization-aware DP cost), the workstreams below
  recast them as **runtime-side** physical decisions (plan 05 §4, plan 09 §4) that need no DP change.
  Runtime cardinality inputs (plan 01 §2) improve what the runtime consumes, not how the optimizer
  enumerates.
- **Correctness-required exclusions** (confirmed by adversarial verification; do not "fix"):
  morsel workers shed merge walks and SIP seeks — worker key streams are non-monotonic and
  `MASK_EXHAUSTED` would truncate valid data (`LmdbNativeChunkPipeline.java:146-148, :174, :722`);
  ordered DISTINCT's adjacency proof binds it to its rewritten plan (`LmdbNativeSlotOrder.java:321-350`);
  merge join refuses pre-ordered patterns (`LmdbNativeMergeJoin.java:67-69`);
  prefix-run cursors never drive a join (`LmdbPrefixRunCursor.java:86-91`;
  `LmdbNativeAggregatePlanner.java:242, :316` are semantic guards);
  ordered-factorized's `requiredPrefixMask` stability loop
  (`LmdbNativeFactorizedRows.java:145-147, :188-193`);
  the sketch refresh reader must not be CSR-served (`LmdbSailStore.java:1547-1552`);
  the `GRAPH ?g { ... path ... }` gate (`LmdbNativeAggregatePlanner.java:426-428`) — the path plan unions
  across fixed contexts (`LmdbNativePathPlan.java:131-139`), which is wrong inside GRAPH;
  sequence property paths `(a/b)+` stay on the generic evaluator.
- **Non-goals:** NUMA placement (JVM-level, negligible at this engine's scale), OS readahead tuning
  (LMDB's mmap + OS page cache already governs base-data IO), on-disk format migrations beyond the two
  explicitly versioned encoding changes in plan 08, and any change to SPARQL-visible semantics.


## Workstream index and dependency order

    01-dispatch-and-cost.md      Cost substrate + proposal-based dispatch for proven overlaps
    02-filters.md                One filter path; decode-free compares; adaptive placement as decorator
    03-vectorization.md          Batch mechanics: batched probes, bound-mask hoisting, column sharing
    04-parallelism.md            Elastic admission, worker capabilities, parallel sort/merge/build
    05-ordering-sort.md          ORDER BY joins the engine; key/payload split; radix; order-aware physical planning
    06-factorization.md          Tail-less chunk prefix; multiplicity as an interface; GROUP-key reduction
    07-csr-storage.md            Zone maps, O(1) degree, ordered consults, SIP from CSR, cache policy
    08-range-pushdown-encoding.md  Compare→key-range scans; order-preserving encodings v2; varint wide loads
    09-joins.md                  Hash/merge internals; mark joins; leapfrog WCOJ; accumulate; bushy runtime builds
    10-aggregation.md            Budgeted spilling group tables; wider primitive keys; parallel merge; streaming
    11-paths.md                  Memoized, masked, frontier-based property paths
    12-memory-dictionary.md      Global memory budget; batched dictionary access; cache policy
    13-verification.md           Benchmarks, plan-snapshot corpus, acceptance gates, rollout switches

Dependency graph (execute left-to-right; items within a plan are ordered):

    Phase I   (independent, start immediately):
              01§1-2 (fan-out stats, telemetry), 02§1-3 (kernel dedup, decode-free, batch-join filters),
              03§1-2 (atomics, batched probe), 04§1 (elastic admission), 07§1-3 (zone map, degree, seekForward)
    Phase II  (needs Phase I):
              01§3-4 (cost-arbitrated C∩D, slice/top-K), 02§4 (adaptive-as-decorator; also needs the
              in-flight filter-authority work to land), 04§2-3 (worker vectorized prefix, ValuesPlan),
              05§1-3 (ORDER BY reach, slot pruning, key/payload), 06§1-2 (tail-less prefix, multiplicity),
              07§4-5 (ordered consult, SIP-from-CSR), 10§1-2 (budget+spill, wider keys)
    Phase III (needs Phase II):
              04§4-5 (parallel sort/merge, per-worker builds), 05§4-5 (radix, order-aware physical),
              06§3-5, 08 (all), 09§1-3, 10§3-5, 11 (all), 12 (all)
    Phase IV  (large structural, independent of each other):
              09§4 (leapfrog WCOJ), 09§5 (accumulate), 12§1 completion (global budget wired everywhere)

`13-verification.md` runs continuously: its corpus and gates are created in Phase I and every subsequent
item lands against them.


## Interaction with in-flight work

The uncommitted changes on this branch (`LmdbNativeAdaptiveFilterPlacement.java`,
`LmdbNativeJoinPlans.java`, `LmdbNativeRowStep.java`, `LmdbNativeSlotPlan.java`, `NativeFilterLease.java`,
plus `EXECPLAN-lmdb-native-admission.md`) restore optimizer authority over filter boundaries and make
adaptive movement evidence-driven. Plan 02 §4 builds directly on that outcome and must not regress its
invariants: the optimizer's filter boundary is the initial placement; movement happens only after a
complete bounded observation window. One correction to fold into that work while its files are open: the
sink-veto rationale at `LmdbNativeJoinPlans.java:343-344, :106-111` cites a tail-gate constraint that
commit `19ebab53d2` already removed (superseded by the comment at `LmdbNativeFactorizedTail.java:279-280`).


## Global definition of done ("production ready, performance side")

The program is complete when all of the following hold (measured per plan 13):

1. Every workstream's acceptance criteria are met, each backed by a paired benchmark run
   (`scripts/run-single-benchmark.sh`, compared with the jmh-benchmark-compare methodology) and, where the
   change is plan-shaped, by query-plan snapshot diffs limited to the target shape.
2. The dispatch ladder contains no pair of jointly-satisfiable strategies chosen by source order — every
   proven overlap is either cost-arbitrated or converted to a layer.
3. No optimization in the engine is dead (computed-but-unread statistics, unreachable kernels,
   default-off flags without a measured justification recorded in plan 13's rollout table).
4. Every unbounded memory structure is accounted against a budget, and every budget refusal degrades to a
   slower-but-correct path (spill or fallback), never to OOM.
5. The concurrency contract holds: N concurrent mid-size queries on a P-core machine each make parallel
   progress (no all-or-nothing admission), and early close/cancellation is honored by every new loop.
6. The full LMDB module test suite is green modulo the pre-existing baseline (24 known LMDB-only
   compliance failures documented before this program; that list must not grow).
