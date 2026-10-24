# Remove bytecode-verified dead sketch and query-optimizer code (second pass)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while the work proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The branch `GH-0000-lmdb-predicate-guarantees` carries the packed Cascades planner, the Frontier synopsis, the
sketch-based estimators, and the learned-feedback (LEO) machinery. A first cleanup on 2026-08-23 (see
`.agent/execplans/GH-0000-lmdb-dead-code-cleanup.md`) removed whole retired types. This second pass removes the
member-level residue: methods, constructors, fields, constants, nested types, and whole abstraction-only interfaces
that no compiled class in the repository references any more. After this change the two affected modules,
`core/queryalgebra/evaluation` and `core/sail/lmdb`, compile and their unit suites run exactly as before, with
6,876 fewer source lines to read past when working on the live planner path.

"Dead" here has one precise meaning: no invoke, field-access, method-handle, or lambda bootstrap instruction in any
`target/classes` or `target/test-classes` directory of the whole reactor resolves to the member, and the member does
not override anything in a JDK or third-party supertype. Members used only by tests are not dead by this definition
and were left alone (see the Decision Log).

## Progress

- [x] (2026-09-03) Ran the mandatory root `-Pquick clean install`; `BUILD SUCCESS`.
- [x] (2026-09-03) Wrote a `java.lang.classfile` scanner (`.agent/research/dead-code-scan/DeadScan.java`) that
  walks every `target/classes` and `target/test-classes` tree, resolves every invoke/field/method-handle/indy
  reference through the class hierarchy, and reports unreferenced members of `org.eclipse.rdf4j.sail.lmdb.**` and
  `org.eclipse.rdf4j.query.algebra.evaluation.optimizer.**`. Members that also exist on `main` were excluded as
  upstream API.
- [x] (2026-09-03) Wrote a source-level member remover (`.agent/research/dead-code-scan/remove_members.py`) that
  deletes a declaration by name and descriptor, including its javadoc and annotations, and prunes unused imports.
- [x] (2026-09-03) Iterated scan → remove → compile eight times until the scan reached a fixpoint. The full manifest
  of removed members is `.agent/research/dead-code-scan/removed-members-2026-09-03.tsv` (769 entries).
- [x] (2026-09-03) Removed abstraction-only interfaces by hand: `RdfStatisticsProvider` (with
  `EstimationDecision` and `EvaluationStatisticsAdapter`), `PatternFilterSamplingEstimator`, `QuadSynopsis`,
  `LeoSurfaceProvider`, and the dead default methods of `LeoLearnedEvidenceService`; dropped the corresponding
  `implements` clauses and `@Override` annotations on the concrete classes whose methods stay live.
- [x] (2026-09-03) Removed the `appliedFiniteRelations` plumbing from `PhysicalProperties` (always empty since its
  only producer was dead) together with `FiniteRelationContract`.
- [x] (2026-09-03) Removed the binding-flow arena experiment (`PackedBindingFlowArena`, `PackedPathFactIndex`,
  `PackedQuery.BindingFlowState`) and its reflection-only test `PackedBindingFlowArenaTest`.
- [x] (2026-09-03) Deleted five empty package directories left behind by earlier work (`lmdb/hypergraph`,
  `lmdb/sketch/omni`, `cascades/join`, `cascades/ir`, `cascades/dsl`).
- [x] (2026-09-03) Ran the formatter for both modules and confirmed every modified file still carries the RDF4J
  copyright header; both modules complete `-Pquick install` with `BUILD SUCCESS`.
- [x] (2026-09-03) First full unit-suite runs went red in 16 test classes (18 reds in query-evaluation, 60 in
  LMDB), all `NoSuchMethodException`/`NoSuchFieldException`/"missing contract" from tests that reach members through
  reflection. Built the restore set from the surefire messages plus verified `getDeclaredMethod("…")` sites (77
  members, `.agent/research/dead-code-scan/restored-reflective-members-2026-09-03.tsv`), reset the 29 affected
  files to `HEAD`, re-applied only the remaining removals, restored the private helpers those members need, and
  re-ran the 16 classes: 97/0/0 in query-evaluation and 246/0/0 in LMDB.
- [x] (2026-09-03) Re-ran the formatter and a clean `-Pquick install` (`BUILD SUCCESS`); final full suites are
  recorded under `Outcomes & Retrospective`.

## Surprises & Discoveries

- Observation: bytecode reference counting misses four kinds of live use, each of which produced a false positive
  that had to be filtered by hand.
  Evidence: `FrontierOmniCellFlags` holds only compile-time constants (inlined by javac, so no `getstatic`);
  `PackedHotPath` is an annotation type (class attributes, not instructions); `FiniteRelationContract` appeared only
  in generic signatures (`Set<FiniteRelationContract>`, erased in bytecode); `LmdbSailStore.LmdbStorePathEvent` is a
  JFR event whose fields are read reflectively by the JDK.
- Observation: `JoinOrderPlanner` is a legacy dynamic-programming planner that `LmdbEvaluationStatistics` still
  extends, but the only caller of its planning entry points is `LmdbThemeQueryRegressionIT`, which reaches it through
  reflection on `LmdbStore.getBackingStore()`.
  Evidence: the scanner reports the class as test-only; grep shows `planJoinOrderAttempt` invoked at three places
  in the IT and nowhere in production code.
  Follow-up (2026-09-03): removed. `FilterConditionCostModel` now owns `FILTER_COST_CHEAP`/`FILTER_COST_EXPENSIVE`
  (the only production dependency); the three IT tests that drove the SPI directly, their helpers, and the fully
  commented-out `QueryJoinOptimizerReorderSafetyTest` were deleted.
  Follow-up 2 (2026-09-04): transitive closure removed by re-running the scanner to fixpoint (4 passes, 41 members;
  manifest `removed-members-2026-09-04-joinorderplanner-closure.tsv`): `JoinStatsProvider` (abstraction-only
  interface, only `LmdbFilterSelectivityStats` implemented it; the class was detached and its unused overrides
  dropped), `JoinFactorCostModel.FilterCostEstimate`, the `String[]`+mask `CostContext.of`/ctor family plus the
  `VariableMaskSet` view that only that family could reach, dead ctor overloads on `LmdbEvaluationStatistics` and
  `LmdbFilterSelectivityStats`, `FilterSelectivityKeys.qualifyFilterKey`/`patternLocalBaseForFilter`,
  `LmdbJoinPlanSupport.isFiniteNonNegative`, `LmdbStatementPatternCardinalitySource.estimate*` non-ID overloads,
  `PackedBindingFacts.bindingValueCount()`. Verified: both modules compile; 1,528 evaluation + 484 lmdb tests in
  the touched classes' test families green. Still dead but deliberately left: on-`main` upstream-shaped members
  (`Varint`, `TripleStore`, `TripleIndex`, `TxnRecordCache`, `ValueStore`, `LmdbSailStore`, `inlined/*`, the
  `estimate/` LMDB page readers) and the ~295 test-only production members.
- Observation: several `private final` fields were write-only because their accessors had been removed in an
  earlier pass; removing the field also required removing the constructor assignment, and doing that by regex over
  the whole outer type once deleted an assignment in a nested class with a same-named field.
  Evidence: `FrontierQueryIndex.Builder.identity`/`descriptor` and `FrontierStatisticsHeapGovernor.Lease.purpose`
  failed with "might not have been initialized"; both were restored. A diff audit of every auto-removed assignment
  against remaining same-named field declarations found no other case.
- Observation: a fifth kind of live use is invisible to bytecode scanning: the packed-planner and Frontier unit
  tests reach package-private members through `Class.forName`, `getDeclaredMethod`, `getDeclaredConstructor`, and
  `getDeclaredField`, often with the class or method name assembled from string tables.
  Evidence: the first full suite runs failed in 16 classes (`OptimizerRuleCatalogTest`, `PackedPlanCacheTest`,
  `PackedDphypEnumeratorTest`, `PackedCostAlgebraTest`, `LmdbOperatorFeedbackStatsTest`, `LmdbPlanDecisionCacheTest`,
  `FrontierPayloadBlockFormatTest`, and others) with `NoSuchMethodException: …PackedNodeSetArena.<init>(int)`,
  `missing cost-algebra method …PackedCostDimensionRegistry#dimensionId`, and similar. Those 77 members were restored
  from `HEAD`; a plain string scan of test sources over-matches badly (`"of"`, `"size"`, `"merge"`), so the surefire
  messages plus class-aware `getDeclaredMethod("…")` sites were the usable signal.
- Observation: `PackedBindingFlowArena` was never reachable from production code: its only construction path was
  `PackedQuery.BindingFlowState`, whose accessor no production class called, and its test exercised it purely through
  reflection.

## Decision Log

- Decision: scope "dead" to members with zero bytecode references from any main or test class, excluding members
  that exist on `main`.
  Rationale: this is verifiable mechanically and cannot remove upstream RDF4J API or anything a test relies on.
  Date/Author: 2026-09-03 / Claude
- Decision: leave test-only production members in place (284 methods, 59 constructors, 3 classes after the
  fixpoint) and report them instead.
  Rationale: they are exercised by real unit tests (planner entry points, arena contracts, feedback-stats seams,
  `FrontierStatisticsHashBatch` which the in-progress vector-batching plan depends on); deleting them means deleting
  tests, which is a product decision.
  Date/Author: 2026-09-03 / Claude
- Decision: keep `FrontierBottomKRows`, `OptimizerCatalogRenderer`, `PackedHotPath`, `FrontierOmniCellFlags`,
  `JoinOrderPlanner` (later removed, see the follow-up above), the `ValueStore.onBatchResolve*` test hooks,
  `PackedPredicateRangeProvider.predicateRangeVersion`,
  `DistributionSketch.innerProduct`, and the record canonical constructors flagged by the scanner.
  Rationale: the first three were explicitly preserved by the previous cleanup or are reached by name from tests;
  the rest are constant holders, reflection targets, or interface methods whose implementations live in tests.
  Date/Author: 2026-09-03 / Claude
- Decision: remove interfaces whose every method is unreferenced through the interface even when concrete
  implementations of those methods stay live (`RdfStatisticsProvider`, `PatternFilterSamplingEstimator`,
  `QuadSynopsis`, `LeoSurfaceProvider`).
  Rationale: an interface nobody dispatches through is pure indirection; the concrete methods keep their behavior.
  Date/Author: 2026-09-03 / Claude
- Decision: treat members that tests reach only through reflection as test-only (kept), and restore every one the
  first suite runs exposed rather than editing the tests.
  Rationale: the same rule already protects members tests call directly; reflection is just a different call
  mechanism, and deleting those tests would be a coverage decision, not a cleanup.
  Date/Author: 2026-09-03 / Claude
- Decision: keep now-unused constructor parameters (for example `PackedSearchCheckpoint` generations,
  `RuleApplicabilityResult.setApplicable(..., affectedFactMask)`) and only drop the stored field.
  Rationale: changing those signatures touches many live call sites for no behavioral gain; a follow-up can trim them.
  Date/Author: 2026-09-03 / Claude

## Outcomes & Retrospective

The tracked diff is 167 files: 61 insertions and 6,187 deletions. Seventeen files were deleted outright:
`CardinalityEstimate`, `FiniteRelationContract`, `RdfStatisticsProvider`, `PackedBindingFlowArena`,
`PackedOptimizerRule`, `PackedPathFactIndex`, `PackedRuleContext`, `LeoBindingShape`, `LeoEstimateDiff`,
`LeoSurfaceProvider`, `PackedBindingFlowArenaTest`, `BoundJoinProductEstimate`, `OptionalBridgeProductEstimate`,
`QuadSynopsis`, `CharacteristicSetEstimate`, `PatternFilterSamplingEstimator`, and `PropertyPathEstimate`. The
final removal manifest (`.agent/research/dead-code-scan/removed-members-2026-09-03.tsv`) counts 566 methods, 39
constructors, 44 write-only fields, 11 constants, 1 field, and 6 nested/whole classes; 77 members that tests reach
through reflection were restored (`restored-reflective-members-2026-09-03.tsv`).

No behavior changed by construction: every removed member had no caller, and the only non-mechanical edits were
dropping `implements`/`@Override` on methods that remain, deleting always-empty `appliedFiniteRelations` handling,
and removing stored-but-never-read fields.

Test results: the 16 test classes that went red after the first pass are green after the restore (query-evaluation
97/0/0, LMDB 246/0/0). Final full unit suites after formatting: `core/queryalgebra/evaluation` 1,528 run, 0
failures, 0 errors, 0 skipped; `core/sail/lmdb` 2,357 run, 0 failures, 0 errors, 7 skipped (plus 5/0/0 in the
`frontier-vector-tests` execution). Both `BUILD SUCCESS`. Integration tests (Failsafe) were not run.

## Context and Orientation

The live optimizer path is `LmdbQueryOptimizerPipeline` → `LmdbCascadesOptimizer` → the packed Cascades/DPhyp
planner under `core/queryalgebra/evaluation/.../optimizer/cascades/packed`. Cost evidence comes from the Frontier
synopsis (`core/sail/lmdb/.../frontier`), the quad synopsis and sketch estimators (`.../estimation`, `.../sketch`),
and learned feedback (`LmdbOperatorFeedbackStats`, `optimizer/leo`). `LmdbEvaluationStatistics` is the LMDB
`EvaluationStatistics` implementation that adapts all of those to the planner.

## Plan of Work

Run the root quick install. Run the scanner over every compiled class directory with the lmdb and optimizer
packages as the scope and the lmdb test classpath for supertype reflection:

    ROOTS=$(find . -type d \( -path '*/target/classes' -o -path '*/target/test-classes' \) -not -path './.mvnf/*' | tr '\n' ',' | sed 's/,$//')
    java .agent/research/dead-code-scan/DeadScan.java "$ROOTS" "org/eclipse/rdf4j/sail/lmdb/,org/eclipse/rdf4j/query/algebra/evaluation/optimizer/" "$(cat lmdb-cp.txt)" > dead.tsv

Refine with `refine.py` (adds the on-`main` check, `@Override` detection, and source counts for inlined constants).
Build a manifest of `DEAD-*` rows that are not on `main`, skipping private no-arg constructors, JFR event fields,
annotation types, constant holders, and abstract methods with any live overrider. Run `remove_members.py` on the
manifest, compile both modules with `-Pquick install`, fix the handful of compile errors (assignments to removed
write-only fields, `@Override` on methods whose interface method went away), and repeat until the scan reports only
the intentional keeps. Finish with the formatter, the copyright header check, and both module unit suites.

## Validation

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation,core/sail/lmdb -Pquick install
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation test
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb test

Both installs must print `BUILD SUCCESS`; the test runs must show no new failures compared with the branch head
(the LMDB module had five pre-existing join-telemetry overlap errors before this work).
