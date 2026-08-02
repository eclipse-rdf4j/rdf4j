# GH-0000 Unified next-steps plan (2026-07-22)

Synthesis of branch `GH-0000-lmdb-predicate-guarantees` state across all ExecPlans in
`.agent/execplans/`, the code on the branch, and the papers pack in `papers2/papers/`.

## Where things stand

### Landed and production-live
- **Packed Cascades planner is the only production Cascades route.** The hard cutover of
  `GH-0000-packed-idempotent-cascades.md` Milestones 1–5 is complete: `CascadesPlanner.optimize(...)`
  delegates entirely to `PackedCascadesPlanner`; legacy memo/IR/rule DSL/shadow routes are removed.
  LMDB consumes it end-to-end (`LmdbCascadesOptimizer` → `LmdbQueryOptimizerPipeline`,
  `PackedPlanCache` owned by `LmdbSailStore`). 41 packed source files, 99 tests, JMH benchmark,
  zero TODO/FIXME. Recorded packed results crush the frozen baselines (e.g. eight-factor
  6,830 ms/10 GB → 0.065 ms/215 KB).
- **Closed plans** (git-corroborated): bounded-cold-evidence, dphyp-hypergraph-join-planner,
  omni-p1-p2-remediation, omni-surface-hard-cut, omni-vertex-witness-bridges,
  planner-god-class-split, concurrent-maven-workspaces, or-values-memo-local,
  unified-self-reactivating-query-optimizer (superseded by cascades-hardening, itself now
  largely overtaken by the packed cutover — see workstream D).

### Open work, by plan
1. **packed-idempotent-cascades** — Milestone 6 (activate LMDB predicate-object range
   guarantees in packed planning) and Milestone 7 (acceptance gates). This is the branch's
   namesake and the primary open front.
2. **lmdb-architecture-closeout** (umbrella) — final LMDB verify not green: sparse q6 still
   routes its UNION/correlated-anti path wrongly; full module/compat/benchmark/hygiene gates
   unrun; formal closure of sibling plans pending.
3. **lmdb-estimation-engine-rewrite** — code cutover done; fresh q-error, interval-coverage,
   JMH, allocation, and retained-heap measurements still outstanding.
4. **cascades-hypergraph-reference-hardening** — SUPERSEDED 2026-07-22 (predated the packed
   cutover; every class it introduced was deleted with the legacy memo). Audited item-by-item
   against the packed code — see Workstream D for the disposition table and the five residual
   items that survive as D2.

### Build caveat
Single-module compiles fail only from stale installed jars (branch-new `TelemetryMetricNames.PLANNED_*`,
`Join.isCacheable()` exist in source). A full reactor `install -DskipTests` of the upstream
modules is a precondition for everything below.

## Unified next steps (priority order)

### Workstream A — Green baseline first (no-red-baseline policy)
1. Reactor-install upstream modules (`core/query`, `core/queryalgebra/model`, then
   `core/queryalgebra/evaluation`, `core/sail/lmdb`) so per-module builds work again.
2. Fix the **sparse q6 UNION/correlated-anti routing** red from the architecture-closeout
   umbrella; triage any other reds to outdated-test vs real-bug.
3. Confirm `core/queryalgebra/evaluation` and `core/sail/lmdb` Surefire fully green.

### Workstream B — Milestone 6: predicate-range guarantees (the branch's purpose)
Follow the packed plan's own sequence, tests-first:
1. Capture failing end-to-end evidence in `core/sail/lmdb`
   (`initial-evidence.predicate-ranges.txt`, Maven workspace `predicate-ranges`) using
   `LmdbPredicateObjectDomainIndexTest` selectors + new `LmdbPackedPredicateRangePlanningTest`.
   No production changes before this exists.
2. Backend-neutral `PackedPredicateRangeProvider` / `PackedPredicateRange` +
   `predicateRangeVersion()`; planner overloads accepting the provider.
3. `LmdbPackedPredicateRangeProvider` over `LmdbEstimatorRuntime.rdfTermDomain(IRI)`; wire into
   `LmdbCascadesOptimizer`; add range version to `PackedPlanCache.Context`; bump
   `LmdbPackedCostModel.VERSION`.
4. Split packed construction into encode → fact-derive/saturate → freeze; intern ranges,
   attach to object symbols of constant-predicate patterns, propagate `PackedDomainFacts`
   through operators (Join/Lateral intersect, Union widens, Difference left-only,
   LeftJoin right-conditional).
5. Rule alternatives to quiescence: contradiction→EmptySet, tautology filter-drop,
   finite anchors (≤64 values, joined not replacing), each costed as concrete LMDB
   `[P,O]`/`[S,P,O]` lookups.
6. Diagnostics: `optimizer.guaranteeOptions=generated=…, selected=…`, stable rejection
   reasons, counters, cache invalidation on range revision. "Silence is not acceptable" —
   a known applicable range with neither a candidate nor a reason fails acceptance.

### Workstream C — Milestone 7 acceptance + deferred measurement gates (merge with plan 3)
1. Focused tests → full evaluation module → full LMDB module.
2. Versioned JMH corpus: ≥90% cells p95 < 5 ms, ≥50% < 0.5 ms; four-factor < 0.5 ms/512 KiB;
   eight-factor < 5 ms/2 MiB; cache hits < 0.5 ms; JFR shows no collection/stream allocation
   in packed hot paths; reflection architecture test for forbidden field types.
3. Plan quality: identical results; median regret ≤ 1%, p95 ≤ 10% vs golden corpus.
4. Fold in the estimation-engine rewrite's outstanding measurements (q-error,
   interval coverage, allocation, retained heap ≤384 MiB under 1 GiB).
5. Record evidence in the packed plan's Outcomes; run japicmp/compat and hygiene gates from
   the umbrella plan.

### Workstream D — Plan reconciliation and closeout

**D1. Hardening-plan audit — DONE 2026-07-22.** `GH-0000-cascades-hypergraph-reference-hardening.md`
is marked superseded (header note added). Disposition of its open M4–M8 items against the packed
implementation, verified by code audit:

| Hardening concern | Packed status | Evidence / residue |
|---|---|---|
| Typed lossless logical identity (`MemoLogicalExpressionKey`) | **Subsumed** | `PackedExpressionInterner` typed tuple `(opTag, payload, scope, domain, child groups)`, 64-bit hash + full structural equality on collision, idempotent insert, group-conflict throws `PackedMemoInvariantException`. Legacy string key deleted. |
| Log-domain cardinality (`JoinCardinalityModel`) | **Mechanism dropped, goal met differently** | Packed uses raw doubles with saturation/finiteness guards (`joinRows` clamps to `Double.MAX_VALUE`; `PackedPlanningResult` rejects non-finite/negative). Residue → D2a: port the plan's numeric regression cases (`.01*.9*.9→0.81`, `1e400×1e-336→~1e64`, positive-underflow-stays-positive, exact-zero) as packed cost-model tests. |
| Scoped join-region memo / bounded enumeration | **Subsumed** | `PackedJoinEnumerator` confines DP to one maximal inner-join region (only JOIN winners flatten; everything else is an opaque leaf); size-tiered subset kernels (≤16 dense, ≤64 `PackedLongSubsetTable`, >64 multiword); deterministic `PackedSearchBudget`. |
| Deterministic AUTO | **Subsumed** | AUTO uses a fixed 4,096-work-unit budget with `deadlineNanos = Long.MAX_VALUE`; `System.nanoTime()` feeds metrics only. Wall-clock affects BUDGETED mode only. Residue → D2b: port the plan's determinism acceptance tests (byte-identical winners+counters under load; `timeoutMillis=1` no effect on AUTO). |
| EXACT exhaustiveness / `RESOURCE_LIMIT_EXCEEDED` | **Partially subsumed** | EXACT runs full DP (`workLimit = Long.MAX_VALUE`) and reports `COMPLETE` structurally, but emits no positive exhaustiveness certificate; `OptimizationCompleteness.RESOURCE_LIMIT_EXCEEDED` is now a dead enum value never produced. Residue → D2c: decide certificate-or-document, and remove or re-wire the dead enum value. |
| Typed non-inner join edges (`JoinEdge` LEFT/SEMI/ANTI + `ConflictRule`) | **Mechanism dropped; correctness met by rewrite rules** | Packed reorders pure inner regions only; LEFT_JOIN/MINUS/EXISTS are opaque boundaries handled by `PackedLogicalRuleProgram` rewrites + safety analyzers (`OptionalBranchReorderSafety`, `IndependentExistsBranchReorderSafety`). Residue → D2d: port the rewrite-legality parity tests (permitted/prohibited rewrites, bag/multiplicity/unbound/error parity). Cross-non-inner reordering itself → Workstream E item 6 (optional plan-quality follow-up). |
| Opaque atomic factors (Service/subquery/aggregate) | **Subsumed** | Encoded as first-class opaque factors (`PackedRelOp.SERVICE`, subquery flags, aggregate payload interning); unknown operators fail loudly via `UnsupportedCascadesOperatorException` — the fail-closed invariant holds. |
| M7 legacy deletion + parity-gated cutover | **Mooted** | Packed cutover deleted `CostingReceiver`/`PlanHypergraph`/`JoinStateEnumerator` etc. wholesale (none exist in Java). The parity evidence the hardening plan wanted before deletion is now owed by the packed plan's own Milestone 7 gates → Workstream C, including re-baselining the medical query 0–10 oracle against packed. |
| M8 JMH/JFR measurement | **Durable, unrun** | Folded into Workstream C (packed acceptance gates already specify the corpus, allocation, and JFR requirements; add the JDK-26 11-query × 2-algorithm matrix if still wanted). |

**D2. Residual items harvested from the audit** (small, do alongside Workstream C):
- [ ] a. Port `JoinCardinalityModel` numeric regression cases as `PackedCostEstimate`/enumerator tests.
- [ ] b. Port AUTO-determinism acceptance tests (identical winners+counters regardless of load/timeout).
- [ ] c. Resolve EXACT completeness reporting: positive certificate or documented structural guarantee; remove/re-wire dead `RESOURCE_LIMIT_EXCEEDED`.
- [ ] d. Port non-inner rewrite-legality parity tests (LEFT/SEMI/ANTI/MINUS/EXISTS permitted+prohibited matrix) against packed.
- [ ] e. Re-baseline the medical-theme query 0–10 completeness table against the packed planner.

**D3.** Close the architecture-closeout umbrella once Workstreams A–C are green; formally close
the sibling plans it owns.

### Workstream E — Paper-guided follow-ups (after closeout; optional next branch)
The reading order (`papers2/papers/implementation_reading_order.md`) maps Phase 1 (memo
foundation) to the packed work now done and Phases 4–5 (feedback/robustness, semantic
guarantee lattice — "never let a statistical estimate authorize a semantic rewrite") to
Workstream B. Natural next extensions, in suggested order:
1. **Pareto/non-dominated frontier per group+property** (Phase 2; Trummer-Koch MOPQO) in
   `PackedWinnerTable`.
2. **WCOJ / multiway joins** (Leapfrog Triejoin, Free Join) for cyclic SPARQL join regions.
3. **E-graph scalar normalization** (equality saturation/egg) for filter/scalar rewrites.
4. **Feedback loops** (LEO-style) feeding the unified estimation engine; learned components
   only as correctors.
5. **Metamorphic correctness testing** (SQLancer/NoREC-style) + `EXPLAIN WHY` on top of
   `PackedRuleProofs`.
6. **Typed non-inner join reordering** (from the superseded hardening plan's M6): LEFT/SEMI/ANTI
   join edges with conflict rules inside `PackedJoinEnumerator`, unlocking reordering across
   OPTIONAL/EXISTS/MINUS boundaries that are currently opaque. Plan-quality upside only;
   correctness is already covered by rewrite rules + safety analyzers.

## Progress

- [~] A: reactor install green (38 s, BUILD SUCCESS 2026-07-22 09:06); `core/queryalgebra/evaluation` fully
      green (800 tests); full-suite triage COMPLETE (2026-07-22, three parallel audits over all 24 red LMDB
      classes — see "Triage results" below); sparse q6 still red (1.374e11 work rows vs 1e9 gate, root cause =
      packed planner does not parameterize property paths from the bound/finite endpoint, same family as the
      AAS/property-path reds).
- [~] B: Milestone 6 predicate-range guarantees — core slice LANDED 2026-07-22 (tests-first; see the packed
      plan's Progress for details): provider boundary, range arena, sound domain-fact lattice, encode →
      fact-derive → saturate phases, contradiction/tautology/anchor rules, objectGuarantee retention +
      silence-reasons, cache range-version. `LmdbPredicateObjectDomainIndexTest` 16/16,
      `LmdbPackedPredicateRangePlanningTest` 8/8 (new), core `PackedPredicateRangePlanningTest` 6/6 (new).
      Remainder: stored-finite anchors w/o filter (join-enumerator), calendar expansion, datatype/lang
      tautologies, per-candidate diagnostics.
- [ ] C: Milestone 7 acceptance gates + estimation-engine measurements. Note: LMDB verify now reaches japicmp,
      which fails on packed-cutover class removals vs 6.0.0 (e.g. LmdbEvaluationStatistics$LmdbCardinalityCalculator)
      — needs a compat-gate decision (exclusions vs facade), do not silently weaken.
- [x] D1: hardening-plan audit + supersession marker (2026-07-22) — disposition table above
- [~] D2: residual ports — a: DONE (`PackedCostNumericContractTest`, 5 green: saturation-not-overflow,
      exact products, positive floors, exact-zero accepted, non-finite/negative rejected); b: DONE
      (`LmdbPackedAutoDeterminismTest`, cold-store run-over-run winner+counter identity and
      timeoutMillis=1 immunity); c: DONE (EXACT structural-completeness guarantee documented on
      `OptimizationCompleteness`, dead `RESOURCE_LIMIT_EXCEEDED`/`UNSUPPORTED_ATOMIC_BOUNDARY`/
      `NO_VIABLE_PHYSICAL_PLAN` values removed — fail-fast exceptions replaced them); d (non-inner
      legality parity tests) and e (medical q0–10 re-baseline) remain.
- [ ] D3: umbrella closeout
- [ ] E: (optional, next branch) Pareto frontier / WCOJ / e-graphs / feedback

## Triage results (2026-07-22, full LMDB suite: 24 red classes, 86 failures/errors)

Fixed 2026-07-22 (estimate-audit family, second session):
- `LmdbEstimationEngineTest` (1→0): removed the flat `+1` BindingSetAssignment work surcharge in
  `LmdbAccessCostModel` (workRows now exactly relation-width × invocations).
- `LmdbOpaqueOperatorCardinalityTest` (3→0): SERVICE constant-endpoint ≥1000 floor, keyed GROUP BY
  sqrt-collapse fallback, and TripleRef statement-count proxy re-implemented in `LmdbEstimationEngine`
  (they died with `LmdbCardinalityCalculator`).
- `LmdbEstimateAuditHarnessTest` (21→11): packed planner now composes join estimates as a product over
  connected components of the join prefix (`PackedSelectedPlanContextualizer.composedPrefixRows`;
  newest member's contribution stands for its component, islands multiply, exact zero propagates);
  `PackedJoinEnumerator.joinRows`/`joinRowsForSubset` and the FILTER/BSA heuristics in
  `PackedIncumbentSearch`/`PackedFilterRules` propagate exact zeros instead of flooring to 1;
  GROUP output rows derived from keys (keyless→1) in both incumbent and contextualizer;
  `LmdbCascadesOptimizer` stamps `optimizer.connectedEnumeration` (connected-prefix-only /
  phase2_disconnected_components + cartesianFallbackReason) and lets an engine-proven exact-zero
  root estimate dominate the stamped root cardinality. `LmdbPackedCostModel.VERSION` 4→5.
- Remaining 11 reds are all other families, none of them the cutover estimation regressions:
  q7 trio needs the M6 finite-anchor bound-lookup estimate (pre-cutover planned 4.0 via
  `physical-join-bound-lookup` using enumerated ≤64-value anchors — verified empirically in a
  6cb0ed0065 worktree; engine-only estimate is 12); q44/q98 impossible-numeric-filter zeros need
  the cold filter synopsis fix (evidence source is `estimateSnapshotFilterPass`, currently
  degraded); q17/q23/specificAssetThreshold* are the property-path parameterization family; the
  two corpus rollups are dominated by those same queries. A packed `factorCost` integration was
  tried and reverted: the engine's scalar-prefix conditioning (heuristic prefix bag, no per-var
  NDV) collapses connected-leaf estimates to leaf-global rows and regressed the Cartesian family.

Fixed this session:
- `LmdbPredicateObjectDomainIndexTest` (6→0): objectGuarantee stamping (M6) + `getKnownRdfTermDomain`
  disabled/excluded leak fix in `LmdbPredicateObjectDomainSource`.
- `LmdbValueIdFilterOptimizationTest` (2 errors→0): packed codec canonical-term metadata conflict — attachTerm
  is now first-wins (per-occurrence metrics are not term identity).
- `ThemeQueryBenchmarkSparseParamTest` (1→0): re-enabled "SPARSE" in `ThemeQueryBenchmark` `@Param`
  (commented out by 577e0791cb).

Remaining, by verdict:
- Real regressions from the packed cutover (fix in production):
  - Cursor-skip family (8 tests, 3 classes): 577e0791cb deleted `LmdbDistinctCursorSkipRule` +
    `LmdbAccessPathImplementationRule`; `JoinFactorCostModel.withRequestedAccessPath` now caller-less; packed
    planner never emits `plannedIndexAccessMode=distinctCursorSkip`. Fix = packed physical access-path
    alternative rule.
  - `LmdbFilterSimplifierOptimizerTest` (8): null-rejecting OPTIONAL→INNER + literal-anchor hoisting rewrites
    in the legacy simplifier stopped firing after the big rewrite; also `LmdbCascadesObservedOptionalCoverageTest[6]`.
  - Estimate-audit family (`LmdbEstimateAuditHarnessTest` ~15 of 21, `LmdbOpaqueOperatorCardinalityTest` 3,
    `LmdbEstimationEngineTest` 1): `LmdbPackedCostModel` exact-zero row floor (`Math.max(1, prefixRows)`),
    disconnected-island Cartesian rows lost, SERVICE/GROUP-BY/TripleRef specializations lost, factorCost
    off-by-one (40001 vs 40000).
  - Operator feedback (`LmdbOperatorFeedbackPlanningTest` 3, `LmdbOperatorFeedbackStatsTest` 1): LEO feedback
    not fused into packed cost model / operators not stamped.
  - Cold filter synopsis (`LmdbColdFilterSelectivityStatsTest` 9): cold synopsis never built/persisted
    (`join-estimator.rjes.cold` missing), EXACT degrades to SAMPLED.
  - `LmdbStoreConnectionValueMaterializationTest` (7+1): BSA rows rebuilt as MapBindingSet — lazy
    value-materialization path bypassed; +1 NPE in `ValueStoreRevision.storeHash` via plan-cache fingerprint
    hashing unresolved LmdbIRI.
  - `LmdbEstimatorArchitectureTest` (1): `LmdbEstimatorRuntime` 736 lines > 700 cap — needs splitting.
- Property-path/finite-anchor gap (packed planner, plan-quality): `LmdbAASQuery2CascadesHypergraphPlanningTest`
  (2), `LmdbPropertyPathEstimateTest` (3), ~6 estimate-audit methods, plus pre-existing
  `LmdbAASPropertyProjectionPlanningTest`, `LmdbSparsePrefixCostTest` q6 (2), — all one family: paths not
  parameterized from bound/finite endpoints.
- Outdated tests — DONE 2026-07-22, all four green against packed observables:
  `LmdbFiniteValuesJoinSurfacePlanningTest#valuesNameBinding…` (planPreparedInput + planned metrics on the
  partOf pattern), `LmdbIndependentFiniteAnchorJoinPlanningTest` (SPARQL EXPLAIN; anchors-before-bridge +
  directLookup [S, P, O] — needed a REAL production fix: `LmdbPackedCostModel.finiteLookupRows` now jointly
  enumerates multiple disjoint-component VALUES anchors (cap 1024, VERSION 2→3); before, only the first
  anchor was substituted so two-anchor bridges costed as one-sided prefixScan),
  `QueryBenchmarkTest#multipleSubSelect…` (plannerId=lmdb-packed-cascades + every SP carries
  plannedIndexAccessMode), `AASQueriesBenchmarkTest#cascadesQuery3UsesPackedDirectLookupPlan` (renamed).
  `QueryBenchmarkTest#subSelect…` was a false alarm: packed KEEPS the aggregate-free Group (no
  distinct-projection proof needed) and only prints group keys in different order — label match made
  order-insensitive. The `plannedCostWorkRows` Long.MAX saturation on Order/Projection nodes remains
  worth its own look (not asserted by the test; it bounds `plannedCardinalityRows`, which is finite).
