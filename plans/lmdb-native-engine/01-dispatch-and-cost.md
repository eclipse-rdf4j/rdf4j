# 01 — Runtime dispatch and the cost substrate

Goal: strategy selection stops being an ordered if/else ladder for the pairs that genuinely overlap, and
every admission decision consumes a defensible work estimate instead of a single-pattern row count or a
magic constant. Sketch/DP planner code is untouched (00-overview scope exclusion); everything here is
runtime-side.


## Current state

`NativeRowsIteration.initialize()` (`evaluation/LmdbNativeRowStep.java:1215-1284`) is seven rungs, each
`return true` on success: A prefixRun (`:1215-1220`), B orderedDistinct (`:1221-1225`), C batch
(`:1239-1249`), D parallel (`:1250-1259`), E factorized (`:1262-1276`), F adaptive (`:1277-1281`),
G nestedLoop (`:1282-1284`). Rung C nests its own two-rung ladder: merge join then hash join
(`LmdbNativeJoinPlans.java:84-89`). The aggregate path mirrors this with seven rungs
(`LmdbNativeGroupStep.java:293-373`). ORDER BY forks before the ladder entirely
(`LmdbNativeRowStep.java:186-189`).

Admission quantities in use today: `root.estimate(row) >= 50_000` for parallelism
(`LmdbNativeParallelPipelines.java:183-186`) — resolves to `PatternPlan.estimate`
(`LmdbNativePatternPlan.java:284-294`), i.e. the ROOT pattern's own cardinality with no term for
downstream fan-out; `DEFAULT_MIN_ROWS = 4096` on BOTH sides of merge/hash join
(`LmdbNativeMergeJoin.java:39, :87-90`; `LmdbNativeHashJoin.java:32, :61-64`); top-K cliff
`emitCap <= 100_000` (`LmdbNativeRowStep.java:300, :424`); per-probe fan-out guesses hardcoded as
`termPseudoCardinality` constants 64/256/4096 (`LmdbNativePatternPlan.java:325-334`), consumed as
`rowsPerProbe` (`LmdbNativeSlotOrder.java:604-609`). `MultiJoinPlan.estimate`
(`LmdbNativeJoinPlans.java:451-460`) is a naive independence product consulted by exactly one caller
(`LmdbNativeSlotOrder.java:732`) and by no admission decision. The only true cost comparison anywhere in
dispatch is adaptive placement's internal target choice,
`estimatedCostUnits * (legalDepthCount - 1)` (`LmdbNativeAdaptiveFilterPlacement.java:104-117`).

Proven jointly-satisfiable overlaps (from the composition analysis, each adversarially confirmed):
C∩D — an unfiltered 2-pattern join with both sides ≥4096 and root ≥50 000 always gets the single-threaded
batch join; D∩F and E∩F — a multi-way join with an eligible expensive filter always loses adaptive
placement (E additionally pins every filter at earliest depth via `derive(mask, true)`,
`LmdbNativeJoinPlans.java:236-238`).


## Work item 1 — Execution-path telemetry as the change-safety net

Before touching dispatch, make its behavior observable and assertable.

`recordExecutionPath` already tags each root iteration with the winning strategy
(`LmdbNativeRowStep.java`, call sites at each rung, e.g. `"nestedLoop"` at `:1283`). Steps:

1. Enumerate the complete path-tag vocabulary (grep `recordExecutionPath(` across `evaluation/`) and
   freeze it as constants in `LmdbNativeAttemptMetrics`.
2. Add a test-visible accessor returning the per-query tag plus, for rungs that *attempted* and declined,
   the decline reason (the `reject("...")` strings in `LmdbNativeParallelPipelines.tryOpen` already exist;
   mirror the pattern in `LmdbNativeMergeJoin.tryOpen`/`LmdbNativeHashJoin.tryOpen`, which today return
   bare null).
3. Extend `LmdbNativeStrategyPriorityTest` (exists, currently pins ladder order) into the
   dispatch-contract test: for a corpus of plan shapes, assert which strategy wins and why. Every work
   item below lands with an update to this test showing the intended before/after.

Acceptance: for every shape in the plan-snapshot corpus (plan 13), the winning strategy and all decline
reasons are queryable from tests without debugger use.


## Work item 2 — Per-predicate fan-out statistic (the missing cost input)

Replace the 64/256/4096 `termPseudoCardinality` guesses with measured per-predicate fan-out. This is the
single cost input every other admission improvement consumes.

Design: for a bound-subject probe on predicate p, expected rows ≈ `triples(p) / distinctSubjects(p)`;
mirrored for bound-object. Sources, in preference order:

1. **CSR prefix sums** — when `LmdbCsrAdjacencyCache` holds an entry for p, `runStart` is exactly the
   per-key degree distribution (`LmdbCsrAdjacencyCache.java:251-262, :458`): `nKeys` gives distinct
   subjects (BY_SUBJECT) or objects (BY_OBJECT), `nPairs/nKeys` the mean fan-out, and
   `runStart[k+1]-runStart[k]` exact per-key degree. Expose
   `OptionalDouble meanFanOut(long pred, boolean bySubject)` and
   `OptionalLong exactDegree(long pred, long key, boolean bySubject)` on the cache; both are O(1) reads
   of existing arrays.
2. **Maintained counters** — a small on-heap map predicate → (tripleCount, distinctSubjEstimate,
   distinctObjEstimate), updated incrementally on the write path where `TripleStore` already touches
   per-predicate state, distinct estimated by a 64-register HyperLogLog per predicate (fixed 512 bytes
   each, capped to the top-K predicates by triple count, K=4096). This survives when no CSR entry exists.
   Note `TripleStore.prefixLength` (`TripleStore.java:1515-1546`) cannot supply `distinctSubjects(p)`
   under `spoc,posc` — that is why maintenance, not derivation, is needed.
3. **Fallback** — the existing constants, retained so behavior is identical when neither source has data.

Steps: add `LmdbFanOutStats` (new class, `org.eclipse.rdf4j.sail.lmdb`), wire source 1, then source 2
behind a write-path hook; change `PatternPlan.termPseudoCardinality` and
`PatternPlan.estimateForBoundMask` (`LmdbNativePatternPlan.java:302-308, :325-334`) to consult it; make
`MultiJoinPlan.estimate` (`LmdbNativeJoinPlans.java:451-460`) chain per-step estimates using
`estimateForBoundMask` with the bound mask accumulated along the derived order, so it becomes a work
product rather than an independence product.

Correctness: estimates influence only performance decisions; results are unchanged by construction.
Staleness: counters are advisory; CSR-derived numbers are revision-checked by the cache itself
(`LmdbCsrAdjacencyCache.java:350-352`).

Tests: unit tests for the estimator against a store with known skew (one hub predicate, one uniform);
dispatch-contract assertions that admission flips where intended (see items 3–5).

Acceptance: `rowsPerProbe` consumers (`LmdbNativeSlotOrder.java:604-609`) and parallel admission see
measured values on a warmed store; estimator overhead unmeasurable in the theme benchmark (<0.5%).


## Work item 3 — Proposal-based arbitration for C∩D

Scope deliberately minimal: only the batch-join rung and the parallel rung learn to compare; all other
rungs keep source order.

Design: extract the guard bodies of rungs C and D into
`Optional<StrategyProposal> proposeBatch(...)` / `proposeParallel(...)` where
`StrategyProposal { Supplier<RowCursor> open; double estCost; String tag; }`. Costs:

- Batch join: `cost_C = leftRows + buildRows` (merge: `leftRows + rightRows`), from item 2's estimates.
- Parallel: `cost_D = totalWork / workers + startupOverhead`, with `totalWork` =
  `MultiJoinPlan.estimate` (now a work product), `workers` the group size elastic admission grants
  (plan 04 §1), `startupOverhead` a measured constant (calibrate once via the concurrency benchmark,
  plan 13; initial value 250_000 cost units, stored as a named constant with the calibration recipe in
  its javadoc).

Dispatcher: if both propose, pick min cost; record both costs in telemetry so miscalibration is visible.
If only one proposes, behavior is exactly today's.

Trap: rung C's nested merge-then-hash order stays internal to `proposeBatch` — do not flatten it into the
outer comparison (merge vs hash have a real dominance order given both are admissible on the same key).

Tests: dispatch-contract cases — (a) 2-pattern unfiltered join, both sides 1M, 8-core: parallel must win;
(b) same shape, 2-core: batch must win; (c) any shape where only one proposes: unchanged tag.

Acceptance: benchmark `?s :p ?x . ?s :q ?y` with both predicates ≥1M triples improves ≥2× on an 8-core
machine versus the single-threaded batch join baseline; no regression on shapes where only one strategy
proposes.

D∩F and E∩F are NOT arbitrated here — they dissolve when adaptive placement becomes a decorator
(plan 02 §4); arbitration would be temporary scaffolding.


## Work item 4 — Slice-tolerant parallelism and budget-derived top-K

Two admission constants replaced by principled rules.

(a) `reject("slice")` (`LmdbNativeParallelPipelines.java:154-156`). The slice is applied entirely
consumer-side with multiplicity-aware offset skipping (`LmdbNativeRowStep.java:1088-1089, :1140-1165`),
early termination propagates (`:1116-1121`, `finish()` at `:1103`), and worker cancellation works and is
tested (`ParallelRowCursor.close` `LmdbNativeParallelPipelines.java:751-773`;
`earlyCloseCancelsWorkersAndLeavesStoreUsable`). Replace presence-rejection with:
reject only when `isShortFiniteSlice(offset, limit)` (reuse the predicate shape from
`LmdbNativeAdaptiveFilterPlacement.java:88-91`: both under 256) — a short slice's answer arrives before
worker startup pays for itself. Document the accepted behavior change: unordered LIMIT results become
run-to-run nondeterministic (spec-legal; stable paging within a snapshot requires ORDER BY). Update
`sliceAndActiveWriteStaySequential` (`LmdbNativeParallelPipelinesTest.java:494-514`) to pin the new rule.

(b) The top-K cliff. `emitCap <= 100_000` (`LmdbNativeRowStep.java:300`) guards a genuinely eager
allocation — the non-budget `NativeSortBuffer` constructor allocates `capacity * slotCount` longs upfront
(`LmdbNativeSort.java:67-75`) with an unchecked int multiply. Route `:305-306` through the budget-aware
constructor (`LmdbNativeSort.java:364-371`, lazy growth, `BUDGET_REFUSED` protocol) so capacity derives
from the memory budget and row width; add periodic reduce for k too large to hold outright (Kuzu's
pattern: when buffer exceeds `max(MIN_REDUCE, 2*(offset+limit))` rows, sort, truncate to offset+limit,
record the boundary key, pre-filter subsequent input against it); keep a hard
`emitCap < Integer.MAX_VALUE / slotCount` guard — the current `(int)` casts silently produce an EMPTY
result above overflow (`LmdbNativeSort.java:353, :392-394`), which is a latent correctness bug to fix in
the same change and cover with a test at `emitCap = Integer.MAX_VALUE + 1L`.

Acceptance: `ORDER BY ?x LIMIT 1_000_000` runs in bounded memory with no cliff at 100_001; a paged query
`OFFSET 100_000 LIMIT 1_000` uses the heap path; the overflow test passes.


## Work item 5 — Decline-reason parity for the aggregate ladder

The aggregate dispatch (`LmdbNativeGroupStep.java:293-373`) gets the same telemetry (item 1) and the same
C∩D arbitration (item 3) where `LmdbNativeParallelAggregation.tryEvaluate` (`:316`) and the factorized
path (`:335`) overlap — the parallel-aggregation admission mirrors the pipelines' predicate
(`LmdbNativeParallelAggregation.java:85-104`), so the same estimates and the same elastic group size
apply. No separate design; this item is the port.

Acceptance: aggregate dispatch-contract tests mirror item 3's cases with a COUNT wrapped around the same
shapes.


## Dependencies and hand-offs

Item 2 feeds plans 04 (admission), 05 (order-aware physical choice), 09 (build-side selection, NLJ-vs-hash
floor), 11 (path cardinality). Item 3's `StrategyProposal` type is reused by plan 04 §5 (per-worker
builds) and plan 09 §2 (merge floor replacement). Item 4(b) is completed by plan 05 §2 (live-slot pruning
shrinks the row the budget prices).
