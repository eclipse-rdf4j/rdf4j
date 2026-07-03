# Opaque-factor join enumeration for the LMDB Cascades planner

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It must be maintained in accordance with `.agent/PLANS.md` (from the repository root).

## Purpose / Big Picture

Today the LMDB store's cost-based join planner gives up on any join that mixes plain triple patterns with anything else. A query such as

    SELECT * WHERE {
      ?person ex:worksAt ?org .
      OPTIONAL { ?person ex:nickname ?nick }
      ?org ex:locatedIn ?city .
      ?city ex:population ?pop .
    }

contains a "join island" (a maximal tree of inner joins) of four members, but because one member is an OPTIONAL, the planner refuses to reorder *any* of them and executes the query in written order. The same happens with BIND, UNION, sub-SELECT, SERVICE, GROUP BY subtrees and RDF-star triple references. After this change, the dynamic-programming join planner treats such members as "opaque factors": black boxes with known output variables and a cost estimate that can be placed anywhere legal in the join order. Users see the planner pick a cheap execution order for queries that mix basic graph patterns with OPTIONAL/BIND — observable both in query runtimes and in the query explanation (`query.explain(Explanation.Level.Optimized)`), which will show the reordered factors.

## Progress

- [x] (2026-07-03) Research: gating chain, cost fallback, seed admission and separator consumers mapped (see Context).
- [x] Milestone 1: Extension (BIND) factors — committed as 6be48d4023 (remaining for a later milestone: end-to-end explain test on a real store).
  - [x] (2026-07-03 19:47Z) Failing test evidence: `tests=2, failures=2` — "Expected the connected planner to own the BIND-bearing island ==> expected: <true> but was: <false>".
  - [x] (2026-07-03 19:51Z) Production change green: `tests=2, failures=0, errors=0`; flag-off gate test and unsafe-shared-output guard test added, `tests=4, failures=0`.
  - [x] (2026-07-03 21:00Z) Regression triage: `LmdbCascadesOptimizerTest` 53 tests / 1 known-red only. Full module run has 16 failing classes — ALL verified pre-existing: identical failures with `-Drdf4j.optimizer.lmdb.cascades.opaqueFactors=false` at HEAD AND at pre-Phase-0 commit d343482b74 after a fresh root `-Pquick` install (54 tests / 18 failures in the 11 newly-triaged classes, per-class counts identical). Milestone 1 introduces zero new failures.
  - [x] Formatted, header check green, committed (6be48d4023).
- [x] (2026-07-03 21:10Z) Milestone 2: LeftJoin (OPTIONAL) factors — committed 3ee60835ad. Red: `tests=7, failures=2` (island not owned); green after `supportedFactor` LeftJoin clause + generalized `opaqueFactorNamesDisjoint` guard (optional-only vars = bindingNames − assuredBindingNames, plus nested BIND assignment names via `nestedAssignmentNames`). OPTIONAL-suite regression green (only known-reds).
- [x] (2026-07-03 21:20Z) Milestone 3: Union factors — committed 87350e56dd (with M4). Red: `tests=10, failures=2`; green via Union clause + same maybe-var guard. Regression found and fixed: DP ownership of `Join(Union, Extension(SP))` islands dropped the `directLookup` annotation the old bound-lookup rule put on the inner statement pattern — fixed by stamping access metrics through row-preserving wrapper chains onto the underlying pattern (`rowPreservingAccessTarget`/`stampAccessMetrics` in `LmdbCascadesConnectedJoinPlanner`), restoring `LmdbCascadesContextPropagationTest` to baseline.
- [x] (2026-07-03 21:20Z) Milestone 4: scope-change subselect (Projection) factors — committed 87350e56dd. Red: `tests=13, failures=2`; green via scope-Projection clause; unsafe names = projected-but-not-assured vars plus projected BIND outputs (hidden assignments cannot leak).
- [x] (2026-07-03 21:25Z) Milestone 5: Service (pinned late), Group, TripleRef factors — committed 2c56c357b3. Red: `tests=18, failures=4`; green. Service requires-bound = all its variables + variable endpoint ref (planner intersects with island-bindable vars → pinned after every var-sharing local factor, proven by `serviceFactorIsPinnedAfterVarSharingFactorsEvenWhenCheap`). Group: assured keys are safe join surfaces, aggregate outputs unsafe. TripleRef admitted plainly.
- [x] (2026-07-03 21:45Z) Full-module comparison vs known-red baseline: only new red was `LmdbCascadesConnectedRuleAdmissibilityTest#projectionWrappingScopedSubqueryDoesNotSuppressGenericImplementation`, which asserted the OLD contract (subselects not reorderable) — updated to the new contract and committed (05f9055b2a). Every baseline class's per-class failure count re-verified identical (e.g., DistinctCursorSkip 5/2/1, ThemeQ9 2, NestedBoundLookup 2+1, PredObjDomain 1/16). Zero new failures from M1–M5.
- [x] (2026-07-03 23:50Z) W3C compliance gate: `LmdbSPARQLComplianceTest` + `LmdbSPARQL11QueryComplianceTest` (compliance/sparql, failsafe) — all failures verified PRE-EXISTING via `-Drdf4j.optimizer.lmdb.cascades.opaqueFactors=false` rerun (identical 10 failures in LmdbSPARQLComplianceTest: propertyPath[8], filterScopeTests[1,4,5,7], optional[2], bind[3,5], union[1], builtinFunction[16]; SPARQL11: 6 on vs 7 off — the feature loses no test and gains one). memory/native stores share union()[1]. Zero compliance regressions from M1–M5; follow-up chip filed for the pre-existing failures (root-cause lead: NPE in PatternKeys.predicateKey on unbound-predicate patterns via estimateFilterPass).
- [ ] Final: ThemeQueryPlanRunBenchmark planQuery/runQuery A/B (≥2×5 s warmup, ≥3×5 s measurement, back-to-back; see memory note lmdb-theme-benchmark-harness for store-dir mapping) — remaining. End-to-end explain tests on a real store per factor class remain future work (M1 explain-shape test sketch is in Plan of Work).

## Surprises & Discoveries

- Observation: opaque factors are already costable. `LmdbCascadesConnectedJoinPlanner.estimateStep` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedJoinPlanner.java:488) asks the `JoinFactorCostModel` first and then falls back to `fallbackEstimate(factor, fallbackStatistics)`, which evaluates *any* `TupleExpr` through `LmdbEvaluationStatistics`. The blocker is purely the admission gate, not costing.
  Evidence: estimateStep body at LmdbCascadesConnectedJoinPlanner.java:495–528.
- Observation: `LmdbJoinPlanSupport.isJoinOrderSeparator` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbJoinPlanSupport.java:546) is consumed by the legacy `LmdbSketchJoinOptimizer` at 10 call sites. Relaxing it in place would silently change the legacy (non-cascades) planner. The relaxation must live in a new cascades-scoped predicate instead.
- Observation: a helper for exactly the "safe BIND" shape already exists: `LmdbJoinIslandConnectivity.rowPreservingNonShadowingExtension` (LmdbJoinIslandConnectivity.java:220) — reuse it for Extension factor admission.
- Observation: `TupleExprs.containsExtension` (core/queryalgebra/model/.../helpers/TupleExprs.java:88) returns false immediately when it reaches a `Join`, subquery `Projection`, or `Service` node — so the `containsExtension` clause of `isJoinOrderSeparator` never fired for the `Join` inputs `reorderableJoin` passes it. The island freeze for BIND-bearing islands came solely from `supportedFactor`; the Milestone 1 separator split (`isJoinOrderSeparatorIgnoringExtensions`) is behavior-neutral documentation of intent, not the functional fix.
  Evidence: containsExtension body — `else if (n instanceof Join …) { return false; }`; the failing test turned green from the `supportedFactor`/planner changes alone.
- Observation: the module suite at HEAD has 16 failing classes, but flag-off (`opaqueFactors=false`) reruns show none of them are caused by Milestone 1; `LmdbIndexAwareJoinOrderPlanningTest`'s 6 failures reproduce identically at pre-Phase-0 commit d343482b74. The branch's earlier "wip"/"more leo" commits shipped with a substantially red module suite. Causality for the remaining 11 classes vs the Phase 0 caching commit is being established with a properly isolated rerun at d343482b74 (root `-Pquick` install of pre-Phase-0 artifacts into `.m2_repo` before testing — an earlier attempt without the reinstall would have mixed HEAD queryalgebra jars with old LMDB sources and was discarded).

## Decision Log

- Decision: implement factor classes incrementally (Extension first, then LeftJoin, Union, subselect, Service/Group/TripleRef), each behind the same new system property and each introduced by its own failing test.
  Rationale: every factor class has its own correctness rules; a single big-bang change would be unreviewable and unbisectable. Extension is first because the correctness rule is smallest (expression inputs must be bound) and a safety helper already exists.
  Date/Author: 2026-07-03 / Claude
- Decision: do not modify `isJoinOrderSeparator`; add `LmdbJoinIslandConnectivity`-local logic that ignores `containsExtension`/`containsSubquery` for flattening while still honoring variable-scope changes and the planner-hint metrics (`optimizer.joinAlgorithmHint`, `optimizer.connectedEnumeration`).
  Rationale: the legacy sketch optimizer depends on the current semantics; the cascades path needs different ones.
  Date/Author: 2026-07-03 / Claude
- Decision: new behavior is ON by default, with `rdf4j.optimizer.lmdb.cascades.opaqueFactors=false` restoring the old gate for one release.
  Rationale: approved roadmap requires the coverage win to actually ship, while giving an escape hatch.
  Date/Author: 2026-07-03 / Claude
- Decision: admit LeftJoin/Union/Service/Group factors regardless of their variable-scope-change flag (unlike Filter/Projection native factors).
  Rationale: the factor is reordered as an unaltered black box; join commutativity is semantic and independent of scoping flags, which only matter when merging content across the boundary. Scope-change Projection is admitted separately as a sub-SELECT relation.
  Date/Author: 2026-07-03 / Claude
- Decision: one shared safety guard for all opaque factor kinds — `unsafeSharedNames(factor) ∩ runtimeVars(other factor) = ∅` — where unsafe names are BIND assignment names (`ExtensionIterator` overwrites via `context.setBinding`, it does not join-filter) and maybe-bound names (`bindingNames − assuredBindingNames`: optional-only vars, single-branch union vars, non-assured projections, aggregate outputs).
  Rationale: one rule covers the overwrite hazard and the unbound-compatibility hazard uniformly; verified per class by dedicated frozen-island tests.
  Date/Author: 2026-07-03 / Claude
- Decision: MINUS (`Difference`) deliberately NOT admitted as an opaque factor in this plan.
  Rationale: `LmdbMaterializedMinusAntiSemiRule`/`LmdbCorrelatedMinusAntiExistsRule` and the minus-prefix pushdown already own those shapes; admitting Difference would compete with them for no demonstrated win. Revisit with evidence.
  Date/Author: 2026-07-03 / Claude

## Outcomes & Retrospective

Milestone 1 (2026-07-03): BIND-bearing join islands are now owned and reordered by the connected hypergraph planner — the first factor class unfrozen. The change was far smaller than designed because costing already worked for arbitrary factors and the separator's extension clause turned out to be inert for joins; the real gate was one boolean in `supportedFactor`. The expensive part was regression triage: the branch's module suite was already red in 16 classes from earlier commits, which required three causality runs (flag-off at HEAD, and pre-Phase-0 with correctly reinstalled artifacts) before Milestone 1 could be declared clean. Lesson: refresh the known-red baseline at the start of every milestone on this branch — the July-2 list was already stale. Follow-up chips were filed to bisect and fix the pre-existing failures.

Milestones 2–5 (2026-07-03): OPTIONAL, UNION, sub-SELECT, SERVICE, GROUP BY and RDF-star triple-reference factors all unfrozen in one day using the same recipe (failing test → classifier clause → shared safety guard → regression triage → commit). The unified guard (`unsafeSharedNames` ∩ other factors' vars) held up for every class; the only real integration bug was M3's loss of the `directLookup` annotation on row-preserving `Extension(SP)` factors, caught by `LmdbCascadesContextPropagationTest` and fixed by stamping access metrics through wrapper chains. One test contract (`projectionWrappingScopedSubqueryDoesNotSuppressGenericImplementation`) encoded the old freeze and was deliberately inverted. Verification: per-class failure counts identical to the recorded known-red baseline across the module suite, and the W3C compliance gate shows zero regressions (flag-off comparison; the feature even recovers one SPARQL 1.1 test). What remains before calling the whole plan done: benchmark A/B for planning-time impact, and end-to-end explain-shape tests on real stores. What this unlocked: every SPARQL query mixing BGPs with OPTIONAL/BIND/UNION/sub-SELECT/SERVICE/GROUP BY now gets cost-based join ordering instead of written order — the single biggest coverage gap in the roadmap is closed at the enumeration layer. The complementary quality work (Phase 2: registering the ~14 missing LMDB rules and real SERVICE/TripleRef/Group cardinalities) is the natural next ExecPlan.

## Context and Orientation

All paths are relative to the repository root of the rdf4j multi-module Maven build.

The LMDB storage backend ("LMDB Sail", module `core/sail/lmdb`) plans SPARQL queries with a Cascades-style optimizer. "Cascades" means: the query algebra tree (`TupleExpr` nodes from `core/queryalgebra/model`) is interned into a *memo* — a table of groups of logically-equivalent expressions — and transformation/implementation rules generate alternatives whose costs are compared (framework in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/`, entry point `LmdbCascadesOptimizer` in `core/sail/lmdb`).

Join ordering is not done by generic rules but by a dedicated dynamic-programming planner. The chain is:

1. `LmdbConnectedHypergraphJoinImplementationRule` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java, around line 494) fires on a `Join` memo expression, but only if
2. `LmdbJoinIslandConnectivity.connectedJoinProviderCanOwn(tupleExpr)` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbJoinIslandConnectivity.java:55) returns true. That method flattens the join tree into *factors* (`flattenFactors`, :307 — a factor is a maximal non-join subtree, e.g. one triple pattern), and requires `island.supported()`, which is the AND of `supportedFactor(factor)` (:375) over all factors. `supportedFactor` currently admits only: `StatementPattern`, `BindingSetAssignment` (VALUES), `EmptySet`, property paths (`ArbitraryLengthPath`/`ZeroLengthPath`), and non-scope-change `Projection`/`Filter`. Anything else — `LeftJoin` (OPTIONAL), `Extension` (BIND), `Union`, scope-change `Projection` (sub-SELECT), `Service`, `Group`, `Difference` (MINUS), `TripleRef` (RDF-star) — makes the whole island unsupported, so the DP rule never fires and the join order stays as written.
3. Flattening itself stops early: `flatten` only recurses into a child `Join` if `reorderableJoin` (:366) holds, which consults `LmdbJoinPlanSupport.isJoinOrderSeparator` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbJoinPlanSupport.java:546). That predicate returns true — stopping flattening — if the subtree *contains* any `Extension` or subquery anywhere below it. So a BIND nested three joins down freezes the entire island even before `supportedFactor` is consulted.
4. When the gate passes, `LmdbCascadesConnectedJoinPlanner.plan(...)` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedJoinPlanner.java:78) runs a DP over connected left-deep prefixes: factors are nodes of a hypergraph whose hyperedges are shared variable names; seeds are admitted by `admissibleSeed` (:814 — today only property paths have a special requirement: an unbound path is not a seed if another factor can bind an endpoint); prefixes are extended one factor at a time (`dpPlan`, :126), each extension costed by `estimateStep` (:488). `estimateStep` works for arbitrary `TupleExpr` factors because it falls back to `fallbackEstimate(factor, fallbackStatistics)` backed by `LmdbEvaluationStatistics` (which knows Join/LeftJoin/Filter/Union and defaults for the rest).
5. The winning `PlanTemplate` is turned back into a left-deep `Join` tree over the *original factor subtrees* (`PlanTemplate.toPlan`, :1430) — factors are reattached as-is, so an opaque factor never needs new physical operators; the evaluation strategy (`LmdbEvaluationStrategy`) executes the reordered join tree with its ordinary join operators.

"Runtime variables" of a factor are its unbound variable names as seen by the planner (`LmdbJoinPlanSupport.runtimeBindingNames`). "Well-designed" OPTIONAL means the variables that appear only in the optional right-hand side are not referenced anywhere else in the query; reordering a non-well-designed OPTIONAL relative to factors that share those right-side-only variables changes results, so such islands must not be reordered across that factor.

Pre-existing known-red tests on this branch (record before starting; they must not be blamed on this work): `LmdbCascadesOptimizerTest#budgetedScopedUnionOptionalKeepsDecomposedOptionalWinner`, `LmdbThemeQ9EstimateRegressionTest` (2 failures), `LmdbNestedBoundLookupEstimateTest` (2 failures + 1 error), `LmdbCascadesContextPropagationTest#scopeChangingUnionHidesOnlyBranchLocalBindOutputs`, and — verified 2026-07-03 by rerunning at commit d343482b74 (identical 6 failures) and with `-Drdf4j.optimizer.lmdb.cascades.opaqueFactors=false` at HEAD — `LmdbIndexAwareJoinOrderPlanningTest` (6 failures: libraryQ7BenchmarkShapeUsesFiniteBranchNameFanout plus five medicalQ7 anti-filter/semi-filter placement tests; summary at d343482b74: `tests=33, failures=6, errors=0, skipped=11`).

## Plan of Work

Milestone 1 (Extension factors) proceeds strictly test-first. First add a failing JUnit test, new class `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOpaqueFactorJoinPlanningTest.java` (copy the copyright header from a neighboring test; add the `// Some portions generated by Claude` signature comment convention used by this agent). The test builds a small LMDB store where the data makes one join order clearly cheapest, issues a query whose algebra is `Join(Join(SP_expensive, Extension(SP_cheap, bind)), SP_selective)` — a BIND whose expression only uses variables of its own argument — and asserts via the explained/optimized plan that the DP planner owned the island (the winning plan carries the `lmdbCascadesConnectedHypergraphJoin` access-path marker, constant `LmdbCascadesConnectedJoinPlanner.ACCESS_PATH`) and that the selective pattern was moved first. Model the assertions on existing plan-shape tests such as `LmdbIndexAwareJoinOrderPlanningTest` (same directory) which show how to build a store, run the optimizer pipeline, and inspect the optimized `TupleExpr`. Run it, capture the Surefire failure snippet into this plan.

Then make it pass with these production edits, all in `core/sail/lmdb`:

1. `LmdbJoinIslandConnectivity.java`: introduce a factor classification. Replace the boolean accumulation `supported &= supportedFactor(factor)` inside `analyze` (:317) with a per-factor `FactorKind` (`NATIVE` for the current allow-list, `OPAQUE_EXTENSION` for an `Extension` — wrapped or not in row-preserving `Filter`s — that satisfies either `rowPreservingNonShadowingExtension` (:220) or a new "correlated extension" check whose expression variables not bound by its own argument form a *requires-bound set*, `UNSUPPORTED` otherwise). `Island` gains `opaqueFactorCount` and keeps `supported()` meaning "no UNSUPPORTED factor". Gate the whole classification behind a new system property `rdf4j.optimizer.lmdb.cascades.opaqueFactors` (default `true`; `false` restores the old allow-list exactly). Extend `reorderableJoin`/`flatten` so that flattening no longer consults the `containsExtension`/`containsSubquery` part of `isJoinOrderSeparator` (keep honoring `TupleExprs.isVariableScopeChange` and the two planner-hint metrics by checking them directly). Do NOT edit `LmdbJoinPlanSupport.isJoinOrderSeparator` itself.
2. `LmdbCascadesConnectedJoinPlanner.java`: teach seed/extension admission the requires-bound rule. Compute per-factor requires-bound sets during `plan(...)` (a new helper mirroring `pathHasEndpointBinder`, :141): an opaque Extension factor with a non-empty requires-bound set is not an admissible seed unless the initial bound vars cover the set, and a prefix extension with it is rejected until `state.boundVars()` covers the set (add the check next to the existing path-endpoint logic in `dpPlan`). Self-contained extensions (empty requires-bound set) need no constraint. Thread the factor kinds from `LmdbJoinIslandConnectivity` (expose a package-private `classifyFactors(List<TupleExpr>)` there rather than duplicating logic).
3. `LmdbCascadesRuleProvider.java` (implementation rule near :494) needs no change if `connectedJoinProviderCanOwn` starts returning true for these islands, but verify the rule does not re-check `supportedFactor` on its own; if it does, route it through the same classification.

Keep `estimateStep` untouched — the statistics fallback already covers Extension. After the test passes, run the wider suites listed under Validation and record evidence here. Later milestones follow the same recipe per factor class; LeftJoin (Milestone 2) additionally requires the well-designed guard: collect the right-side-only variables of the `LeftJoin` factor and classify the island `UNSUPPORTED` if any other factor references them.

## Concrete Steps

All commands run from the repository root.

Before any production edit (mandatory once per session):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{summary=1} summary{print}'

Run the new failing test (expect FAIL first, PASS after the production change):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOpaqueFactorJoinPlanningTest --module core/sail/lmdb --retain-logs

Then the neighboring suites:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbIndexAwareJoinOrderPlanningTest --module core/sail/lmdb
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --module core/sail/lmdb
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb

Format + header check before finishing:

    cd scripts && ./checkCopyrightPresent.sh && cd ..
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Never pass `-am` or `-q` to test runs. Test reports land in `core/sail/lmdb/target/surefire-reports/`; paste 1–30 line snippets into this plan as evidence at every stopping point.

## Validation and Acceptance

Acceptance for Milestone 1: `LmdbOpaqueFactorJoinPlanningTest` fails before the production change with an assertion showing the island was not owned (no `lmdbCascadesConnectedHypergraphJoin` marker / written order preserved) and passes after; `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` shows no new failures beyond the recorded known-red list; with `-Drdf4j.optimizer.lmdb.cascades.opaqueFactors=false` the new test's reordering assertion is skipped (JUnit assumption) and the legacy suites still pass. Final acceptance for the whole plan additionally requires the SPARQL compliance suites (`python3 .codex/skills/mvnf/scripts/mvnf.py testsuites/sparql`, plus `compliance/sparql` if runnable offline) green, and a back-to-back `ThemeQueryPlanRunBenchmark` planQuery/runQuery A/B (≥2×5 s warmup, ≥3×5 s measurement) showing no planning-time regression on unaffected queries.

## Idempotence and Recovery

All steps are additive and re-runnable. If a milestone must be abandoned, set `rdf4j.optimizer.lmdb.cascades.opaqueFactors=false` (old behavior) — the classification code is inert behind it. Commits go on branch `GH-0000-lmdb-predicate-guarantees` prefixed `GH-0000`; commit after each green milestone so `git revert` of a single milestone is possible.

## Artifacts and Notes

Milestone 1 red (2026-07-03, `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOpaqueFactorJoinPlanningTest --module core/sail/lmdb`):

    [mvnf] Summary: tests=2, failures=2, errors=0, skipped=0, time=0.047s
    - selectivePatternIsOrderedBeforeSelfContainedExtensionFactor: Expected the connected planner to own the
      BIND-bearing island ==> expected: <true> but was: <false>
    - selfContainedExtensionFactorIslandIsOwnedByConnectedPlanner: An island whose only non-pattern factor is a
      self-contained BIND should be reorderable ==> expected: <true> but was: <false>

Milestone 1 green (same selection, after the production change):

    [mvnf] Summary: tests=2, failures=0, errors=0, skipped=0, time=0.054s

Implementation notes: the separator split lives in `LmdbJoinPlanSupport.isJoinOrderSeparatorIgnoringExtensions`; classification, `plannableExtensionFactor`, `opaqueFactorRequiredVars` and the extension-output-disjointness island guard live in `LmdbJoinIslandConnectivity`; the requires-bound admission is enforced in both `dpPlan` and `greedyPlan` via `opaqueRequiredVars(...)` (variables no other factor can bind impose no precedence). The island guard rejects reordering when a BIND output name is also a runtime variable of another factor, because written order joins on the shared name while a reordered Extension would overwrite it.

## Interfaces and Dependencies

No new external dependencies. New package-private API in `core/sail/lmdb`:

    // LmdbJoinIslandConnectivity
    enum FactorKind { NATIVE, OPAQUE_EXTENSION, /* later: OPAQUE_LEFT_JOIN, ... */ UNSUPPORTED }
    static List<ClassifiedFactor> classifyFactors(List<TupleExpr> factors)
    record ClassifiedFactor(TupleExpr factor, FactorKind kind, Set<String> requiresBoundVars)

`LmdbCascadesConnectedJoinPlanner.plan(...)` consumes `classifyFactors` for admission; its public signature is unchanged. The system property name `rdf4j.optimizer.lmdb.cascades.opaqueFactors` is read once per planning invocation alongside the existing `rdf4j.optimizer.lmdb.cascades.*` properties.
