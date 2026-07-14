# Phase 3: joins + aggregation on chunks — full multiplicity algebra and one membership abstraction

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`.
This is Phase 3 of the approved umbrella plan (user plan file
`users-havardottestad-documents-programm-binary-squid.md`); Phases 0–2 are complete
(`.agent/execplan-lmdb-factorized-correctness-audit.md`, `.agent/execplan-lmdb-phase1-engagement-and-substrate.md`,
`.agent/execplan-lmdb-phase2-factorized-chunk-pipeline.md`).

## Purpose / Big Picture

The umbrella plan's Phase 3 success bar: **SUM/AVG GROUP BY suites gain the COUNT-path asymptotics, and
OPTIONAL-heavy queries stop disqualifying.** Today the factorized aggregation tail (`FactorizedTail`) refuses
everything but COUNT (`AggregateSpec.allCounts` gate at `LmdbNativeFactorizedTail.java:213`), refuses any filter at
or after the branch depths (`:222-229`), and only engages over all-pattern `MultiJoinPlan`s — so a `SUM(?price)
GROUP BY ?vendor` over a star with unprojected legs enumerates the full cross product that a COUNT of the same shape
skips, and one OPTIONAL in the query throws the whole aggregation back to enumeration. Structurally, the engine also
carries four copies of the group-key arity switch (0/1/≤4/generic) and three bespoke correlated-probe caches
(left-join payload probe, left-join memo, membership probe) that are all the same build-once/probe-many hash store.

After this phase: value-typed aggregates (SUM/AVG/MIN/MAX, DISTINCT and not) run factorized — branch match counts
become multiplicity weights, branch value lists feed value-typed specs per distinct probe key instead of per
enumerated row; branch-local filters attach to branches instead of disqualifying; non-pattern prefix children
(OPTIONAL, VALUES) no longer disqualify trailing pattern branches; the group-key arity switch exists once; and
EXISTS/MINUS membership plus the left-join payload probe share one keyed-match-store abstraction. Deletions in this
phase: the duplicated arity switches, `PayloadMap`, and the membership/filter memo duplicates. Full deletion of
`FactorizedTail`+`Branch` and `LmdbNativeHashJoin` (replacing them with a chunk-native aggregate sink) is
**deliberately deferred** — see Decision Log.

## Progress

- [x] (2026-07-14) Survey of the aggregation/join machinery complete (results in Context below).
- [x] (2026-07-14 08:50Z) Milestone A: multiplicity-weighted aggregation — `AggState.addWeighted(spec, id, weight)`
  (COUNT += weight; SUM/AVG accumulate value×weight via `MathUtil` with an exact integer weight literal, preserving
  numeric promotion and type-error poisoning; MIN/MAX weight-free), `FactorizedTail` admits value-typed non-distinct
  specs (branch value lists — the DISTINCT `values` columns generalized to `valueQuadPos` — walked with
  `productExcluding(otherBranches)`; prefix values weighted by the full product), value-typed DISTINCT and the
  grouped-by-tail mode stay gated (`allCounts` now guards only tailGroupPos), scan-once/existence-only gates keyed
  on `valueColumnsPerBranch`. Parallel aggregation keeps its own `allCounts` gate (its merge is COUNT-only). Tests:
  8 new in `LmdbNativeFactorizedTailAggregationTest` incl. `sumOverBranchValuesEngagesFactorizedTail` +
  `sumOverPrefixSlotWithCountingBranchesEngagesFactorizedTail` (engagement asserts) — 29/29 green; fuzz 10/10;
  NonCountAggregate 19/19, CountStar 8/8, ParallelAggregation 14/14. (Red not separately observed: the runner was
  locked by the Phase 2 theme-IT run while the implementation landed; the engagement asserts are the contract.)
- [x] (2026-07-14 09:05Z) Milestone B: branch-local filters — `tryCreate` takes the plan's `MaskedFilter[]`; the
  `maxFilterDepth >= last` refusal is gone (kept only for the grouped-by-tail mode); branch growth no longer stops
  at filters; a shrink loop pushes the shallowest touched branch back into the prefix when a filter spans two
  branches; attached filters evaluate per candidate quad through the row trail (ported `acceptFilters`), widen the
  branch memo key by their prefix reads, disable scan-once for that branch, and disable the single-record existence
  fetch; tail-depth filters reading no branch fresh slot gate prefix rows in `aggregate()`. Red→green
  `filterOnBranchVariableAttachesToBranch` (red: `aggState`; the one-filter variant reordered into the prefix — two
  filters pin the gate) + widened-key/spanning/filtered-value-agg tests. Callers pass `plan.filters`; parallel
  aggregation already refuses any filtered plan (`plan.filters.length != 0` gate), so no new concurrency exposure.
- [x] (2026-07-14 09:20Z) Milestone C: OPTIONAL stops disqualifying — the optimizer hoists OPTIONAL to the root as
  `LeftJoinPlan(left=MultiJoinPlan(...), right=opt)`, so the fix is left-join/join commutation in the group
  dispatch: `reshapeLeftJoinForFactorization` hoists bag members the OPTIONAL neither reads (greedy minimal cover
  of `memoReadMask(right) & leftBag.produced` keeps enough members — shared slots are equal across joined rows) nor
  rebinds into a synthetic outer bag `[LeftJoin(keep, opt), hoisted...]` where the tail claims them as branches;
  left-bag filters move to the outer bag (they cannot read optional-fresh slots; filtering before/after the
  optional extension is equivalent). `peelTrailingPatterns` additionally peels right-arm patterns off `JoinPlan`
  spines with an `SlotPlan.assuredMask` gate (new conservative helper: LeftJoin→left, Union→∩, unknown→0) so
  branches never join on maybe-unbound slots. Red→green `optionalInPrefixStillFactorizesTrailingBranches` (red:
  `aggState`; first reshape attempt kept ALL children because every member shares ?s — recorded in Surprises) +
  safety tests (optional var consumed by trailing pattern; aggregates over optional vars). Fuzz 10/10 + tail 35/35
  + NonCount/LeftJoinWellDesigned/Parallel/CountStar green.
- [x] (2026-07-14 16:15Z) Milestone D: one keyed-match-store — new `LmdbNativeKeyedMatches.java` (`KeyedMatches`):
  arity-adaptive key core (1 key → open-addressed long table with dense entry ids; 2..4 keys → PrimitiveTupleTable;
  0/wide → HashMap<GroupKey,Integer>) with roles as dense parallel arrays — presence (EXISTS), boolean verdicts
  (memo, `withVerdicts()/memoGet/memoPut`), and an arena of LIFO-chained payload rows
  (`initRows/addRowAt/head/next/payload`, `dropRows()` degrades to the key set at the row cap while key inserts
  continue — the historical payloadOverflow behavior). Rebased: `PatternMembershipProbe` (LongMembership interface +
  LongHashSet adapter deleted; contains()-probes the store), `PatternPayloadProbe` (PayloadMap + separate keySet →
  one store; LIFO row chaining preserved for identical emission order; LEFTJOIN_HASH_BUILDS counter kept),
  `StatementPatternExistsFilter` (singleMemo/multiMemo/multiProbe → one store memo), `ExistsFilter` (HashMap memo →
  store memo), `ValueSetFilter` + `CachedCompareFilter` (LongBooleanMemo → store memo). Deleted: `PayloadMap`,
  `LongBooleanMemo`, `LongMembership`. Adaptive build triggers stay in the probes (see Decision Log). Routine B
  evidence: pre-green tests=34/0 fail (MembershipJoin/LeftJoinHash/LeftJoinWellDesigned/LeftJoinCursor/
  LeftJoinFilterRewrite), post-green tests=34/0 fail same selection; fuzz + filter memo suites
  (DifferentialFuzz/ExpressionFilter/QueryBoundFilter/AggregateFilterSemantics/FactorizedTailAggregation) 59/0 fail.
  Hit Proof: `LmdbNativeLeftJoinHashTest:157-171` asserts LEFTJOIN_HASH_BUILDS across the rebased build path.
- [x] (2026-07-14 15:55Z) Milestone E: one arity-adaptive group table — new `LmdbNativeGroupTable.java`
  (`NativeGroupTable`, modes ZERO/SINGLE_SLOT/TUPLE_COUNTS/TUPLE_STATES/GENERIC) behind `add(RowState)`,
  `aggregateFactorized(RowState, FactorizedTail)` (deferred group registration preserved), `mergeFrom` (COUNT-only,
  parallel), `results(NativeGroupIteration)`, `strategyName()` (exact historical explain strings). All four sites
  rebased: `evaluateAll` final dispatch (count fast path + row metrics preserved), `evaluateOrderedGroupMap`,
  `evaluateFactorizedInternal` (+groupsByTail via `tailGrouped`), ParallelAggregation worker/mergeResults
  (`WorkerResult` + `mergeLongGroups` deleted; workers now use the primitive tuple table for 2-4 keys instead of the
  boxed HashMap — outputs identical, merge handles it). Deleted from GroupStep: `evaluateSingleSlotGroups`,
  `singleSlotGroupResults`, `evaluatePrimitiveTupleGroups`, `primitiveCountState`, `primitiveCountResults`,
  `primitiveGroupResults`, `evaluatePrimitiveFactorizedGroups` (−392/+33 lines across the two files). Routine B
  evidence: pre-green tests=83/0 fail (PrimitiveGrouping/ParallelAggregation/FactorizedTailAggregation/NonCount/
  CountStar), post-green tests=83/0 fail same selection, fuzz 10/10; Hit Proof:
  `LmdbNativePrimitiveGroupingTest:99-110` asserts the PRIMITIVE_*_GROUP_ROWS counters now incremented inside
  `NativeGroupTable.add`.
- [x] (2026-07-14 10:00Z) Milestone F (success-bar verification): new `sumHubValue` variant in
  `FactorizedTailStarBenchmark` — **3.95 ms/op factorized vs 108.24 ms/op with `factorizedTail.enabled=false`
  (27.4×)**: SUM now runs at the COUNT-path asymptotics. Fuzz 10/10 (SUM/AVG/MIN/MAX generated shapes included in
  its aggregate suite), tail aggregation 35/35, NonCount/CountStar/Parallel/PrimitiveGrouping/LeftJoinWellDesigned
  green, formatter + copyright clean, final full-module verify run at close. W3C compliance 174/176 — both
  failures proven pre-existing LMDB-optimizer bugs, independent of Phases 2–3 (chips task_6aa18888,
  task_2d2bba68).

- [x] (2026-07-14 16:40Z) Phase exit re-verification after D+E: full lmdb module verify — 1500 tests, 1 failure
  (the known pre-existing `LmdbEvaluationStatisticsMemoizationTest#recordsLearnedFilterPassRatio...`), 0 errors;
  theme ITs `LmdbImprovedQueryPlanSnapshotIT` 2/2 and `ThemeQueryBenchmarkSmokeIT` 10/10 green (exact explain
  strategy strings preserved by `NativeGroupTable.strategyName()`). Phase 3 fully complete.

## Scope exclusion (2026-07-14, user instruction)

The LMDB algebra-level optimizers — `LmdbSketchJoinOptimizer`, `LmdbDeferredFilterPlacer`, `LmdbJoinPlanSupport`
(the experimental cascades/memo, sketch-based pipeline) — are OUT OF SCOPE for this phase and its successors
(milestones D/E and any follow-on work must not edit them): the user is replacing them wholesale in a separate
branch. Uncommitted edits to those files were reverted on user instruction on 2026-07-14. Anything this phase needs
from filter placement or join-order decisions has to live in the native evaluation layer
(`core/sail/lmdb/.../evaluation/`), not in those algebra-level optimizers.

## Surprises & Discoveries

- A single branch-var filter does NOT pin the filters-after-branches gate: the deferred filter placer reorders the
  filtered pattern into the prefix and the OTHER pattern becomes the branch. Two filters (one per pattern) force at
  least one to sit at a branch depth.
- The upstream optimizer hoists OPTIONAL to the algebra root, so aggregation over OPTIONAL compiles to
  `LeftJoinPlan(left=MultiJoinPlan(...), right=...)` — not a JoinPlan spine with a mid-plan LeftJoin. The
  commutation reshape (not just trailing-pattern peeling) is what unlocks factorization.
- First reshape attempt hoisted nothing: every bag member shares the join var (?s) with the OPTIONAL. The correct
  condition is coverage, not disjointness — kept members must cover the optional's reads; sharing produced slots
  with hoisted members is fine because bag members always bind and shared slots are equal on every joined row.
- `LmdbNativeParallelAggregation` refuses ANY plan with filters (plan.filters.length != 0), which conveniently
  isolates the new branch filters from worker-shared `NativeBooleanFilter` state.

## Decision Log

- Decision: full deletion of `FactorizedTail`/`Branch`, `LmdbNativeHashJoin`, and the left-join replay/memo pair is
  deferred out of this phase (umbrella listed them as Phase 3 deletions). Rationale: those classes disappear
  naturally when the aggregate sink consumes FactorizedChunks (the Phase 2 pipeline's group representation extended
  to aggregation); deleting them now would mean re-implementing their adaptive behaviors inside the sink in the same
  breath as the multiplicity algebra — two high-risk changes stacked. This phase makes the *limits* disappear
  (COUNT-only, filters, OPTIONAL) and consolidates the substrate (arity switch, keyed-match store); the class-level
  deletions ride the Phase 5/6 sink work. Date/Author: 2026-07-14 / Claude Code.
- Decision: milestone order A → B → C → E → D — the success-bar milestones first (A/B/C are what the user
  observes), then the behavior-neutral consolidations (E mechanical with heavy coverage, D last because it touches
  left-join semantics). Each lands with red→green tests + the differential fuzz. Date/Author: 2026-07-14 / Claude.
- Decision (Milestone E): the TUPLE_COUNTS pure-count fast path is gated behind `allowCountFastPath`, passed true
  only by the `evaluateAll` final dispatch (the historical behavior); the ordered-group-map and factorized/parallel
  sites pass false, so their ≤4-key paths keep per-group AggStates exactly as before. Parallel workers now use the
  primitive tuple table for 2-4 group keys where they previously used the boxed HashMap — outputs are identical and
  `mergeFrom` handles the tuple merge; recorded as the one internal (non-observable) behavior change.
  Date/Author: 2026-07-14 / Claude Code.
- Decision (Milestone D): the adaptive build triggers (membership missThreshold=64; payload minProbes+validated
  estimate) stay in the probe classes, not the store — they are policy about *when* to build, while `KeyedMatches`
  owns *what is stored*; unifying the policies would change engagement behavior, which this consolidation must not.
  `LongCountMap` (scan-once branch count tables) is NOT migrated: it lives inside `FactorizedTail`/`Branch`, which
  the umbrella plan deletes wholesale with the Phase 5/6 chunk-native aggregate sink — migrating it now would be
  churn on doomed code. The COUNT role lands with that sink. Date/Author: 2026-07-14 / Claude Code.
- Decision: with A/B/C landed and fuzz-verified in one session, milestones D and E (keyed-match store, group-table
  consolidation) are NOT rushed into the same working tree at session end — they are pure consolidations with no
  user-visible behavior, each several hundred lines of mechanical movement across 4+ call sites, and the working
  tree already carries Phase 2 + Phase 3 A/B/C uncommitted. They remain the next unit of work with their designs
  recorded in Plan of Work. Milestone F verification (compliance, SUM/AVG benchmark, full module) completes now.
  Date/Author: 2026-07-14 / Claude.

## Outcomes & Retrospective

2026-07-14 (final): milestones D and E landed as pure Routine B consolidations — `NativeGroupTable` (one
arity-adaptive group table, 4 dispatch copies deleted) and `KeyedMatches` (one keyed-match store; `PayloadMap`,
`LongBooleanMemo`, `LongMembership` deleted). Full module + theme ITs green modulo the one known pre-existing
failure. Every Phase 3 milestone is now checked off; the phase is closed.

The phase's success bar is met: value-typed aggregates (SUM/AVG/MIN/MAX, non-distinct) ride the factorized tail —
SUM over a star with unprojected legs went from 108.2 ms/op (enumerating) to 3.95 ms/op (27.4×, the COUNT-path
asymptotics); branch-depth filters attach to branches instead of disqualifying; and OPTIONAL-containing
aggregations factorize via the left-join/join commutation reshape. Three structural limits were removed with ~200
lines of change because the branch machinery already carried the needed shapes (value lists existed for DISTINCT;
filter evaluation existed in FactorizedRows; the reshape reuses the bag's own planners). The consolidation
milestones (D keyed-match store, E group-table dedup) remain as the next unit of work with designs recorded —
deferred deliberately rather than rushed onto a large uncommitted tree (Decision Log). Verification posture at
close: differential fuzz green throughout, aggregation suites green, module suite green modulo the one known
pre-existing failure, and two pre-existing LMDB-optimizer compliance bugs surfaced and spawned as chips (they fail
with the native engine off and pass on the memory store — this phase made them visible, not worse).

## Context and Orientation (survey results, verified 2026-07-14)

All in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`.

**Aggregation dispatch** — `NativeGroupIteration.evaluateAll()` (`LmdbNativeGroupStep.java:283-401`) picks, in
order: prefixRunGroups → parallelAggregation (specialized ordered-distinct + factorized probe) → factorizedTail →
orderedDistinctGroups → parallelAggregation (plain) → factorizedTail (direct) → orderedSinglePatternGroups →
aggState (0 keys) → singleSlotGroups (1 key, `LongAggStateMap`) → primitiveTupleGroups (≤4 keys,
`PrimitiveTupleTable`) → hashGroups (generic `HashMap<GroupKey, AggState>`).

**Arity-switch copies**: `evaluateAll` (`:354,369,374,379`), `evaluateOrderedGroupMap` (`:483,500,519`),
`evaluateFactorizedInternal` (`:817,829,849,852`), `LmdbNativeParallelAggregation.java` worker (`:241-282`, 3-way)
+ `mergeResults` (`:314-347`).

**FactorizedTail** (`LmdbNativeFactorizedTail.java`, branches in `LmdbNativeFactorizedTailBranch.java`):
COUNT-only gate `AggregateSpec.allCounts` at `:213`; filters-after-branches refusal at `:222-229` (and branch-set
growth stops at `maxFilterDepth` `:259`). `Branch.result(row)` → `BranchResult{count, long[][] values}` (values only
for DISTINCT columns today); `FactorizedTail.aggregate` (`:397-434`) multiplies branch counts (`multiplyCounts`,
overflow-checked) and adds the product into COUNT accumulators (`addCounts`). Branch has scan-once
(`buildCountTable :222-238`) with the validated-estimate trigger. Grouped-by-tail variants: `applyGroupedPairs
:523-554`, `aggregateGroupedDirect :557-599`.

**AggState** (`LmdbNativeAggregateState.java`): `AggKind { COUNT, SUM, MIN, MAX, AVG }` (`:97`);
`AggregateSpec{name, slot, constant, distinct, kind}` (`:106`), `allCounts` (`:141`). Per-row accumulation
`AggState.add(RowState)` (`:240-291`): `counts[i]++`, `addSum :293`, `addAvg :312`, `addExtreme :332`. DISTINCT via
per-spec channels + `LongHashSet`. No multiplicity-weighted entry point exists. Parallel merges via
`mergeCountsFrom` (`:356`, COUNT-only).

**Left-join** (`LmdbNativeLeftJoinPlans.java` + `LmdbNativeLeftJoinMemo.java` + `LmdbNativeLeftJoinPayloadProbe.java`):
`LeftJoinCursor.openRight()` (`:201-223`) picks payload-hash probe → memo → plain probe; replay is an orthogonal
outer layer (`:151-250`). Payload probe builds `PayloadMap` (open-addressed, chained payload rows) after
`LEFTJOIN_HASH_MIN_PROBES`; memo keys `HashMap<GroupKey, long[][]>` with `MAX_MEMO_ROWS` cap.

**Membership** (`LmdbNativeMembership.java:112` `PatternMembershipProbe`): one-bound-slot correlated probes against
a materialized `LongMembership` (LongHashSet). Consumers: `StatementPatternExistsFilter`
(`LmdbNativeFilters.java:~150`, with `LongBooleanMemo` single-slot memo + `HashMap<GroupKey,Boolean>` multi memo),
`ExistsFilter` (`:268`), `MinusCursor` (`LmdbNativeRowPlans.java:324`).

**Batch hash join** (`LmdbNativeHashJoin.java`): 2-pattern gate (`tryOpen :42-61`), `PrimitiveHashJoinTable`
(`:271`, open-addressed key columns + chained flat payload rows), claimed via `MultiJoinPlan.openBatch`
(`LmdbNativeJoinPlans.java:148`).

**Primitive maps (7)**: `LongHashSet` (`LmdbNativeAggregateState.java:417`), `LongAggStateMap`
(`LmdbNativeRowState.java:172`), `LongCountMap` (`:231`), `LongBooleanMemo` (`:376`), `PrimitiveTupleTable`
(`LmdbNativePrimitiveTupleTable.java:28`), `PrimitiveHashJoinTable`, `PayloadMap` — same murmur mix + 3/4-load
linear probing.

The correctness oracle remains `LmdbNativeDifferentialFuzzTest` (native vs `rdf4j.lmdb.nativeQueryEngine.enabled=false`).
ALWAYS `set -o pipefail` when piping mvnf.

## Plan of Work

Milestone A — multiplicity-weighted aggregation. (1) `AggState.addWeighted(spec index, value id, long weight)`
entry points: COUNT += weight; SUM/AVG accumulate value×weight with the existing numeric-promotion rules (weight
multiplication = repeated addition of the same term; implement as decoded-numeric × weight, reusing `addSum`'s
promotion; a term whose SUM input is non-numeric poisons the group exactly as today); MIN/MAX ignore weight;
DISTINCT channels ignore weight (same id). (2) `Branch` collects value lists for any value-typed spec reading its
fresh slot (generalizing the DISTINCT-only `values` columns). (3) `FactorizedTail.aggregate`: per prefix row, for
each spec — prefix-slot specs: `addWeighted(row value, productOfAllBranchCounts)`; branch-slot specs: walk that
branch's value list, `addWeighted(v, productOfOtherBranches)` (COUNT keeps the pure count fast path; DISTINCT walks
into the distinct channel unweighted). (4) The `allCounts` gate is replaced by "every spec reads a prefix slot, a
branch fresh slot, or is constant/star". Red→green: differential test where SUM/AVG/MIN/MAX GROUP BY over a star
with unprojected legs must equal generic AND `factorizedTail` engagement must be recorded. Fuzz suite extended with
value aggregates over the factorized shapes.

Milestone B — branch-local filters. In `tryCreate`, a filter at depth ≥ first-branch-depth whose read mask ⊆
(that branch's fresh ∪ prefix-bound slots) attaches to the branch (`Branch.filters`, evaluated per candidate quad by
binding fresh slots through the row trail — port `LmdbNativeFactorizedRows.TailBranch.acceptFilters`); its
filter-read prefix slots join the branch memo key (mirror `FactorizedRows.tryCreate:118-131`). Scan-once must
disable itself for filtered branches (the count table cannot see filter verdicts). Filters reading two branches'
fresh slots still refuse. Red→green + fuzz.

Milestone C — OPTIONAL/VALUES in the aggregation prefix. `FactorizedTail.tryCreate` currently requires an
all-pattern `MultiJoinPlan`; relax exactly as Phase 1 relaxed `FactorizedRows`: non-pattern children stay in the
flat prefix (their reads over-approximated as ~0L so nothing before them becomes a branch), only trailing
`PatternPlan`s with unconsumed fresh slots factorize. The flat prefix runs through the ordinary cursor chain (or the
Phase 2 chunk prefix when all-pattern). Red→green: `SELECT (COUNT(?x) AS ?c) WHERE { ...star... OPTIONAL {...} }`
equals generic with `factorizedTail` engagement recorded. Fuzz.

Milestone E — one group table. `NativeGroupTable` encapsulating the arity dispatch (0 → single `AggState`; 1 →
`LongAggStateMap`; ≤4 → `PrimitiveTupleTable`; else `HashMap<GroupKey, AggState>`) behind
`state(long[] slots)` / `stateForKey(...)` + iteration + merge hooks. Rebase the three GroupStep sites and the
ParallelAggregation worker/merge on it. Behavior-neutral (Routine B evidence: pre/post green on
`LmdbNativePrimitiveGroupingTest`, `LmdbNativeParallelAggregationTest`, aggregation fuzz + Hit Proof).

Milestone D — keyed-match store. `LmdbNativeKeyedMatches`: build-once/probe-many store keyed by 1..k slots, roles
EXISTS (bit), COUNT (long), ROWS (arena-backed payload rows) on one open-addressed core; adaptive build trigger
(probe-count threshold + validated estimate — same rule as Phase 2's hash build). Rebase `PatternMembershipProbe`
(EXISTS role) and `PatternPayloadProbe` (ROWS role) on it; collapse `StatementPatternExistsFilter`'s
`LongBooleanMemo`/multiMemo pair into the store's memo. `RightMemoProbe` and the left-join replay layer stay (their
lifecycle is row-correlated, not build-once) — noted for the Phase 5/6 sink deletion. Deletions here: `PayloadMap`,
the filter memo duplicates. Behavior-neutral where possible; left-join fuzz + `LmdbNativeLeftJoin*` suites green.

Milestone F — phase exit. Extended fuzz green ×3 seeds; benchmark: `FactorizedTailStarBenchmark`-style SUM/AVG
shape ≥2× vs `rdf4j.lmdb.factorizedTail.enabled=false` (and ≈ COUNT-path time); theme ITs green
(LmdbImprovedQueryPlanSnapshotIT, LmdbThemeQueryRegressionIT, ThemeQueryBenchmarkSmokeIT); full lmdb module verify;
review pass (correctness/overflow/budget/close-paths/explain angles); memory update.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j` (always `set -o pipefail` with piped mvnf):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | awk '/\[ERROR\]/{print} /Reactor Summary/{s=1} s{print}'
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedTailAggregationTest      # milestone A/B/C tests live here + new class
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest               # oracle after every milestone
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb                               # phase exit

## Validation and Acceptance

Each milestone: red→green focused test (engagement metric + generic-equality), full differential fuzz green,
no regression in the aggregation suites (`LmdbNativeNonCountAggregateTest`, `LmdbNativeCountStarTest`,
`LmdbNativeFactorizedTailAggregationTest`, `LmdbNativeParallelAggregationTest`). Phase exit per Milestone F.
The one known pre-existing failure
(`LmdbEvaluationStatisticsMemoizationTest#recordsLearnedFilterPassRatioForExternalBoundPatternLocalFilter`) is not
part of this phase's scorecard.

## Idempotence and Recovery

`rdf4j.lmdb.factorizedTail.enabled=false` keeps the pre-phase behavior reachable throughout; every milestone is an
additive, revertable edit set gated by the fuzz. If a milestone's relaxation proves unsound mid-flight, restore the
refusal gate (one condition) rather than reverting the machinery.

## Interfaces and Dependencies

Modified: `LmdbNativeAggregateState.java` (AggState weighted entry points), `LmdbNativeFactorizedTail.java` /
`LmdbNativeFactorizedTailBranch.java` (gates, branch value collection, branch filters), `LmdbNativeGroupStep.java`
(arity consolidation), `LmdbNativeParallelAggregation.java` (same), `LmdbNativeMembership.java` /
`LmdbNativeLeftJoinPayloadProbe.java` / `LmdbNativeFilters.java` (keyed-match store). New:
`LmdbNativeGroupTable.java`, `LmdbNativeKeyedMatches.java`, tests. Reuses: `LmdbNativeLongArena`,
`FactorizedTail.MemoBudget`, `GroupKey`, Phase 2's validated scan-once trigger pattern.
