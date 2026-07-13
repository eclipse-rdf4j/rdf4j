# Split LMDB Planner God Classes

> Superseded for `SketchBasedJoinEstimator` and `LmdbEvaluationStatistics` by
> `.agent/execplans/GH-0000-lmdb-estimation-engine-rewrite.md`. The replacement plan removes the pass-through
> extraction architecture instead of extending it.

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root. Keep this file self-contained enough that a new
agent can resume from it without prior chat context.

## Purpose / Big Picture

The LMDB planner stack currently concentrates too many responsibilities in a few very large files:
`LmdbEvaluationStatistics.java`, `LmdbCascadesRuleProvider.java`, and
`SketchBasedJoinEstimator.java`. This makes ordinary planner fixes risky because estimation, memo caches, sketch
storage, rule registration, and execution feedback all change in the same files. After this work, the same LMDB query
behavior should be available through smaller units with clearer ownership: sketch implementation lives in LMDB, the
statistics class remains the existing facade, and Cascades rules are split into focused rule classes.

This is a behavior-neutral refactor except for any dead Cascades rule deletion that is proven by current tests and
trace evidence before removal. If implementation discovers a required behavior change, switch that slice to Routine A:
write and run the smallest failing in-repo regression test before touching production code.

## Progress

- [x] (2026-07-13 04:58Z) Marked the two estimator-facade slices superseded by the unified-engine rewrite plan.
- [x] (2026-07-08 07:23+02:00) Ran mandatory root quick install before source edits.
- [x] (2026-07-08 07:30+02:00) Created this ExecPlan and `initial-evidence.txt`.
- [x] (2026-07-08 08:12+02:00) Relocated sketch package and adapter into `core/sail/lmdb`.
- [x] (2026-07-08 08:21+02:00) Extracted sketch provider/request/response SPIs and diagnostic/work-adjuster types.
- [x] (2026-07-08 08:41+02:00) Extracted the sketch churn sampler out of `SketchBasedJoinEstimator`.
- [x] (2026-07-08 08:48+02:00) Introduced `LmdbPlannerServices` and rewired production concrete-statistics callers.
- [x] (2026-07-08 09:11+02:00) Split the first LMDB Cascades rule groups while preserving
  `LmdbCascadesRuleProvider.rules(...)`.
- [x] (2026-07-08 09:25+02:00) Fixed two relocation-only sketch test misses after moving the sketch tests into LMDB.
- [x] (2026-07-08 10:11+02:00) Proved the reduced LMDB planner verification failures are current-branch
  baseline behavior from a clean `git archive HEAD` snapshot.
- [x] (2026-07-08 10:34+02:00) Extracted the LMDB access-path Cascades rule classes from
  `LmdbCascadesRuleProvider`.
- [x] (2026-07-08 10:39+02:00) Extracted statistics cache-key and cache-entry infrastructure from
  `LmdbEvaluationStatistics`.
- [x] (2026-07-08 10:44+02:00) Extracted top-level statistics response records for bound-join and optional-bridge
  estimates.
- [x] (2026-07-08 10:50+02:00) Extracted guarantee-option Cascades condition, semantic-prefix, and finite-anchor
  ordering helpers while keeping each new helper under 500 LOC.
- [x] (2026-07-08 11:27+02:00) Extracted safe sketch estimator carrier records into a focused same-package
  `SketchEstimatorRecords` file.
- [x] (2026-07-08 11:48+02:00) Ran final closeout verification and hygiene. The full LMDB module run is still red
  with eight current-branch baseline planner/estimate failures, but no relocation errors remain.
- [x] (2026-07-12 closeout) Extracted guarantee/access-path implementations and removed the private guarantee planner.
- [x] (2026-07-12 closeout) Composed `LmdbEvaluationStatistics` through the requested package-private services and
  value records.
- [x] (2026-07-12 closeout) Extracted estimator ingest, persistence, Omni, scope, frequency, and join-order services.

## Surprises & Discoveries

- Observation: `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailStoreStatementSource.java` imports the
  sketch SPI, so a strict sketch move must also move this LMDB-only adapter or replace it with an LMDB-local adapter.
  Evidence: `rg -n "SketchStatementSource" core/sail/base/src/main/java core/sail/lmdb/src/main/java`.
- Observation: The branch already starts with `GH-0000`, so use `GH-0000` for this plan and any commits unless the user
  later supplies a real issue number.
  Evidence: `git branch --show-current` returned `GH-0000-lmdb-predicate-guarantees`.
- Observation: After the sketch tests moved into `core/sail/lmdb`, LMDB module-level surefire system properties overlaid
  `SketchBasedJoinEstimator.Config` in tests that previously ran in `core/queryalgebra/evaluation`.
  Evidence: the first LMDB relocation selector failed `SketchBasedJoinEstimatorConfigTest` with values like
  `subjectBucketCount=4096` and `nominalEntries=64`; adding test-local property isolation restored the old unit-test
  assumptions.
- Observation: `-Dtest=... verify` also enters Failsafe and can run unrelated benchmark ITs. For the relocation
  milestone, the meaningful proof is the same Surefire selector with `-DskipITs`.
  Evidence: the broad verify lifecycle timed out in `LmdbMedicalOptimizedQueryRegressionIT`, while the relocation
  Surefire selector passed 109 tests with `-DskipITs`.
- Observation: The easiest safe first `SketchBasedJoinEstimator` split is to lift provider/request/response SPIs and
  diagnostic records out of the facade. The remaining large chunks are persistence directory, ingest/update execution,
  OMNI estimation, and join-order planning.
  Evidence: after extracting those types, the sketch selector passed 109 tests and the OMNI diagnostic selector passed
  5 tests.
- Observation: Production concrete-statistics coupling was outside the statistics facade in join-planning state,
  optimizer pipeline, Cascades explain finalizer, connected-join planner scoped caches, and tuple estimate annotation.
  Evidence: after introducing `LmdbPlannerServices`, `rg` finds concrete-statistics `instanceof` checks only inside the
  service boundary.
- Observation: The proof-gated dead-rule deletion step did not have enough current-branch evidence in this pass, so no
  Cascades rules were deleted.
  Evidence: all registered rule ids remain in `LmdbCascadesRuleProvider.rules(...)`; focused Cascades coverage passed
  after each extraction.
- Observation: Moving the sketch tests into the LMDB module exposed two relocation-specific misses: one source-tree
  path in `FastAgmsEstimatorMigrationTest` still pointed at the old queryalgebra directory, and
  `SketchBasedJoinEstimatorMemoryLayoutTest` needed to clear LMDB module estimator system properties before constructing
  its deliberately tiny estimator.
  Evidence: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=FastAgmsEstimatorMigrationTest,SketchBasedJoinEstimatorMemoryLayoutTest verify`
  now passes 5 tests.
- Observation: After those relocation misses were fixed, the reduced failing LMDB selector still has eight planner or
  estimate failures unrelated to missing moved files.
  Evidence:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbAASPropertyProjectionPlanningTest,LmdbCharacteristicSetEstimateTest,LmdbOmniJoinEstimationGeneratedShapeTest,LmdbPredicateObjectDomainIndexTest,LmdbSparsePrefixCostTest,QueryBenchmarkTest verify`
  reports 44 tests, 8 failures, 0 errors. Failures include AAS required-island first-bind order,
  characteristic-set subject-star detection, sparse-prefix planned rows/work rows, predicate-object non-canonical integer
  filtering, OMNI generated-shape accuracy, and a QueryBenchmark group-plan metadata assertion.
- Observation: The same reduced planner failure count reproduces from a clean archive of the current branch before this
  refactor slice, so those planner assertions are not introduced by the package move or first extraction pass.
  Evidence:
  `mvn -o -Dmaven.repo.local=/Users/havardottestad/Documents/Programming/rdf4j-small-things/.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbAASPropertyProjectionPlanningTest,LmdbCharacteristicSetEstimateTest,LmdbOmniJoinEstimationGeneratedShapeTest,LmdbPredicateObjectDomainIndexTest,LmdbSparsePrefixCostTest,QueryBenchmarkTest verify`
  in `/private/tmp/rdf4j-lmdb-baseline-20260708/work` reports 39 tests, 8 failures, 0 errors.
- Observation: This pass is only a first extraction slice, not the full god-class split. `wc -l` currently reports
  `LmdbEvaluationStatistics.java` at 11629 LOC, `LmdbCascadesRuleProvider.java` at 3948 LOC, and moved
  `SketchBasedJoinEstimator.java` at 17271 LOC.
  Evidence: `wc -l core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/SketchBasedJoinEstimator.java`.
- Observation: The access-path Cascades extraction is compile-green with each new rule file under the 500 LOC project
  guideline.
  Evidence:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipTests compile` passes, and `wc -l` reports
  `LmdbAccessPathImplementationRule.java` 392 LOC, `LmdbInnerJoinBoundLookupRule.java` 263 LOC,
  `LmdbRowPreservingSubplanAccessPathRule.java` 108 LOC, and `LmdbDistinctCursorSkipRule.java` 69 LOC.
- Observation: The focused Cascades coverage still passes after the access-path rule extraction.
  Evidence:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbCascadesOptimizerTest,LmdbRuleRegistryCoverageTest,LmdbCascadesOrFilterRewriteCoverageTest,LmdbCascadesOptionalEliminationCoverageTest,LmdbCorrelatedFilterRuleAdmissibilityTest verify`
  reports 111 tests, 0 failures, 0 errors, 0 skipped.
- Observation: The statistics cache-key extraction broke two tests that reflected on the old nested
  `LmdbEvaluationStatistics$FactorCostCacheKey`; the production compatibility shim was intentionally not restored.
  The test now calls the new same-package `FactorCostCacheKey.factorFingerprint(...)` helper directly.
  Evidence: the first statistics selector failed with two `ClassNotFoundException` errors for the old nested class,
  and the rerun after the test update reports 165 tests, 0 failures, 0 errors, 5 skipped.
- Observation: The statistics class now holds fewer cache infrastructure types inline, but still owns service
  algorithms and private estimate records.
  Evidence: `wc -l` reports `LmdbEvaluationStatistics.java` 11225 LOC, `FactorCostCacheKey.java` 227 LOC,
  `LmdbEstimatorCacheKeys.java` 147 LOC, `SetFactorCostCacheEntry.java` 61 LOC, and
  `MaskFactorCostCacheEntry.java` 61 LOC.
- Observation: Moving `BoundJoinProductEstimate` and `OptionalBridgeProductEstimate` to top-level same-package records
  preserved the focused statistics selector.
  Evidence:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbEvaluationStatisticsMemoizationTest,LmdbBoundJoinProductBlendTest,LmdbCascadesOptimizerTest verify`
  reports 165 tests, 0 failures, 0 errors, 5 skipped.
- Observation: The guarantee-option rule is now thinner but still inline; condition walkers, semantic-prefix ordering,
  and finite-anchor fixed ordering live in focused helper files.
  Evidence: `wc -l` reports `LmdbCascadesRuleProvider.java` 3230 LOC,
  `LmdbGuaranteeConditionSupport.java` 186 LOC, `LmdbGuaranteeSemanticPrefixPlanner.java` 385 LOC, and
  `LmdbGuaranteeFiniteAnchorOrdering.java` 254 LOC.
- Observation: The focused Cascades coverage still passes after the guarantee helper extractions.
  Evidence:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbCascadesOptimizerTest,LmdbRuleRegistryCoverageTest,LmdbCascadesOrFilterRewriteCoverageTest,LmdbCascadesOptionalEliminationCoverageTest,LmdbCorrelatedFilterRuleAdmissibilityTest verify`
  reports 111 tests, 0 failures, 0 errors, 0 skipped.
- Observation: Extracting sketch records required converting former enclosing-class private field access to record
  accessors and restoring helper methods on `SummaryStats`, `StateComponents`, and `LookupAnalysis`.
  Evidence: `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick clean install`
  passes after the accessor conversion.
- Observation: The formatted sketch record extraction preserves the focused sketch behavior.
  Evidence:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=SketchBasedJoinEstimatorConfigTest,SketchBasedJoinEstimatorPersistenceTest,SketchBasedJoinEstimatorRebuildParityTest,OmniJoinEstimatorTest,OmniWitnessPersistenceStoreTest verify`
  reports 109 tests, 0 failures, 0 errors, 0 skipped.
- Observation: The current split still leaves three large facades for later slices, but the newest extracted sketch file
  remains under the project guideline.
  Evidence: `wc -l` reports `SketchBasedJoinEstimator.java` 17112 LOC, `SketchEstimatorRecords.java` 184 LOC,
  `LmdbEvaluationStatistics.java` 11075 LOC, and `LmdbCascadesRuleProvider.java` 3230 LOC.
- Observation: Final full LMDB module verification no longer has the earlier sketch relocation error, but remains red
  with the same eight planner/estimate assertion failures proven from a clean current-branch archive.
  Evidence:
  `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` reports 1886 tests, 8 failures, 0
  errors, 54 skipped. The retained log is `logs/mvnf/20260708-093313-verify.log`; top failures are
  `LmdbAASPropertyProjectionPlanningTest`, `LmdbCharacteristicSetEstimateTest`,
  `LmdbOmniJoinEstimationGeneratedShapeTest`, `LmdbPredicateObjectDomainIndexTest`,
  `LmdbSparsePrefixCostTest` (three methods), and `QueryBenchmarkTest`.
- Observation: The generic queryalgebra evaluation module remains green after the strict sketch relocation.
  Evidence:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify` reports 942 tests, 0 failures, 0
  errors, 0 skipped.
- Observation: Final hygiene and package-boundary checks pass.
  Evidence: `./checkCopyrightPresent.sh` reports all files have valid copyright headers and SPDX lines;
  `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` exits 0; `git diff --check` exits 0;
  `rg -n "org\.eclipse\.rdf4j\.query\.algebra\.evaluation\.sketch|evaluation\.sketch" core/queryalgebra/evaluation/src/main/java core/sail/base/src/main/java core/sail/lmdb/src/main/java`
  finds only legacy property-string references in `SketchBasedJoinEstimator`; concrete statistics `instanceof` checks
  are centralized in `LmdbPlannerServices`.

## Decision Log

- Decision: Use strict relocation of sketch implementation into `org.eclipse.rdf4j.sail.lmdb.sketch` with only legacy
  system-property fallback prefixes preserved.
  Rationale: The current production consumers outside the sketch package are LMDB plus the base adapter that exists only
  to feed LMDB sketching. Keeping deprecated old-package facades would preserve the generic-module leak this work is
  meant to remove.
  Date/Author: 2026-07-08 / Codex.

- Decision: Keep `LmdbEvaluationStatistics` as the `EvaluationStatistics` facade and introduce package-private service
  accessors rather than changing external optimizer entry points first.
  Rationale: LMDB callers already downcast to `LmdbEvaluationStatistics`; reducing that coupling should be incremental
  and guarded by tests instead of a public API break.
  Date/Author: 2026-07-08 / Codex.

- Decision: Delete dead Cascades rules only when current tests or traces prove a concrete rule id is unreachable or
  already guarded as deleted.
  Rationale: Rule deletion is the only potentially observable behavior change in this plan, so it must be proof-gated.
  Date/Author: 2026-07-08 / Codex.

## Outcomes & Retrospective

Closed by `.agent/execplans/GH-0000-lmdb-architecture-closeout.md`. Public facades retain lifecycle and compatibility
descriptors; package-private collaborators own the implementation kernels. Remaining SIP, WCOJ, robust-plan selection,
and mid-query replanning work is architectural research, not residual god-class extraction.

Relocation completed. The sketch production tree now lives under
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch`, the moved unit tests live under
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/sketch`, and the former base adapter is now
`LmdbSketchStatementSource` in LMDB. The old queryalgebra and sail-base system-property prefixes remain read fallbacks
behind the new LMDB prefix.

The first sketch facade split is complete: exact-join request/response SPIs, pattern cardinality/filter sampling SPIs,
OMNI probe estimates, OMNI diagnostics, `JoinOrderWorkAdjuster`, and a focused group of small immutable carrier records
now live in top-level files in the moved sketch package.

The first statistics split is complete: `LmdbPlannerServices` is now the package-private service boundary for
LMDB-specific planner services. Production callers no longer downcast directly to `LmdbEvaluationStatistics`; the
concrete `instanceof` checks are centralized in `LmdbPlannerServices`. The scoped factor-cost cache keys and cache-entry
records now also live in focused same-package files, and the bound-join / optional-bridge response records are top-level
same-package records. The cardinality, join-factor cost, join-order, execution-feedback, and remaining private
estimate-record delegates remain for the next statistics extraction slices.

The first Cascades split is complete: `LmdbRule`, planned runtime metrics, connected join ordering, connected hypergraph
implementation, finite/value rewrites, optional rules, semi/anti rules, star scans, and property-path implementation now
live in focused same-package files. The low-level access-path rules now also live in focused same-package files.
`LmdbCascadesRuleProvider` remains the registry entry point and shared-helper holder. The larger guarantee-option
implementation remains inline, but its condition walkers, semantic-prefix planner, and finite-anchor fixed ordering now
live in focused helper files. The remaining guarantee-option costing/materialization/diagnostics logic is the next
Cascades extraction slice.

The full acceptance criteria are not met yet. The current tree compiles, the focused relocation/statistics/Cascades
selectors pass, and the generic `core/queryalgebra/evaluation` module is green after the strict sketch move. The full
LMDB module run remains red with eight planner/estimate assertion failures, and those failures have been proven as
current-branch baseline behavior from a clean archive. The next implementation pass should continue with the remaining
service/delegate extractions and keep the full LMDB module verification blocker documented until the baseline planner
issues are fixed separately.

## Context and Orientation

The repository is a Maven multi-module RDF4J checkout. Work starts at
`/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Important current files:

- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`
  is about 17k LOC and mixes public configuration, background rebuild execution, incremental ingest, persistence,
  count-min/FastAGMS estimation, OMNI estimation, and join-order planning.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/**` is the sketch
  package. Production consumers are LMDB and `SailStoreStatementSource`; the latter is an adapter for LMDB sketching.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java` is about 11k LOC and
  implements `JoinOrderPlanner`, `JoinFactorCostModel`, `QueryOptimizationScopeProvider`,
  `LmdbPredicateObjectDomainSource`, `RdfStatisticsProvider`, and `LeoLearnedEvidenceService` in one class.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java` is about 6k LOC and defines
  the LMDB Cascades registry plus many nested rule classes.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` constructs the LMDB statistics and
  sketch estimator through the store lifecycle.

Important test selectors:

- Sketch relocation and split: `SketchBasedJoinEstimatorConfigTest`,
  `SketchBasedJoinEstimatorPersistenceTest`, `SketchBasedJoinEstimatorRebuildParityTest`, `OmniJoinEstimatorTest`,
  and `OmniWitnessPersistenceStoreTest`.
- Statistics split: `LmdbEvaluationStatisticsMemoizationTest`, `LmdbBoundJoinProductBlendTest`, and
  `LmdbCascadesOptimizerTest`.
- Cascades split: `LmdbCascadesOptimizerTest`, `LmdbRuleRegistryCoverageTest`,
  `LmdbCascadesOrFilterRewriteCoverageTest`, `LmdbCascadesOptionalEliminationCoverageTest`, and
  `LmdbCorrelatedFilterRuleAdmissibilityTest`.

## Plan of Work

First, preserve current behavior with a clean build and focused pre-change test evidence. The root quick install has
already passed and is recorded in `initial-evidence.txt`; before each refactor milestone, run the smallest relevant
selector and capture matching post-change evidence after the edit.

Second, relocate the sketch package. Move production files from
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/**` to
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/**`, and move tests from the matching
`core/queryalgebra/evaluation/src/test/java/.../sketch/**` package to the LMDB test tree. Update imports and package
declarations. Move `SailStoreStatementSource` to LMDB as `LmdbSketchStatementSource`, then remove the old base adapter
so `core/queryalgebra/evaluation` and `core/sail/base` no longer contain production imports from `*.sketch`.

Third, split `SketchBasedJoinEstimator` without changing public behavior. Keep constructor/configuration and externally
used methods on the facade. Extract focused package-private helpers for persistence/cache-directory work, incremental
ingest execution, count-min/FastAGMS estimation, OMNI estimation, join-order planning, and optimization-scope caches.
Do not introduce new dependencies. Preserve legacy system-property prefixes:
`org.eclipse.rdf4j.query.algebra.evaluation.sketch.SketchBasedJoinEstimator.` and
`org.eclipse.rdf4j.sail.base.SketchBasedJoinEstimator.` as fallbacks behind the new
`org.eclipse.rdf4j.sail.lmdb.sketch.SketchBasedJoinEstimator.` prefix.

Fourth, split `LmdbEvaluationStatistics` behind a package-private `LmdbPlannerServices` composition object. Keep the
existing class as the public package facade returned by `LmdbSailStore.getEvaluationStatistics()`. Move service-specific
logic into `LmdbCardinalityEstimator`, `LmdbJoinFactorCostEstimator`, `LmdbJoinOrderEstimator`,
`LmdbExecutionFeedbackRecorder`, and `LmdbEstimatorScope` plus focused cache-key classes. Convert LMDB callers that
currently downcast only for service access to `LmdbPlannerServices.from(statistics)` where practical.

Fifth, split `LmdbCascadesRuleProvider`. Preserve `rules(EvaluationStatistics)` and rule ids. Move `LmdbRule` and
shared rule utilities to focused package-private classes, then move rule groups into separate files by behavior:
connected join, finite filter/value rewrites, access-path implementations, optional/left-join rules, and semi/anti
rules. Do not delete dead rules unless a current test or trace proves the concrete rule id is already unused or guarded
as deleted.

## Concrete Steps

Run all commands from the repository root.

1. Baseline quick install:

       mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

   Expected: `BUILD SUCCESS`.

2. Before each milestone, run the focused selector for that milestone using Maven without `-am` and without `-q`.
   Keep the command and Surefire/Failsafe report snippet in the implementation thread.

3. After each milestone, run the same selector again and record post-change evidence from the same report family.

4. Before final handoff, run:

       python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
       mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify
       cd scripts && ./checkCopyrightPresent.sh
       mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
       git diff --check

## Validation and Acceptance

Acceptance is behavior-neutral: the same targeted LMDB planner and sketch tests pass before and after each split, and
the final LMDB module verification is green or any unrelated existing failures are documented with report paths.

Additionally:

- `rg -n "org\.eclipse\.rdf4j\.query\.algebra\.evaluation\.sketch|evaluation\.sketch" core/queryalgebra/evaluation/src/main/java core/sail/base/src/main/java core/sail/lmdb/src/main/java`
  must show no production imports from the old sketch package outside intentionally documented legacy property strings.
- `wc -l` should show the original god-class files have materially shrunk or become facades.
- `LmdbCascadesRuleProvider.rules(...)` remains the only registry entry point used by `LmdbCascadesOptimizer`.

## Idempotence and Recovery

Use `git status --short --untracked-files=no` before each edit group. Do not revert unrelated files. If a package move
fails partway through, inspect `git status`, complete the move mechanically, then compile before doing semantic edits.
If broad tests fail after a behavior-neutral slice, rerun the smallest selector that covers the touched path and compare
with baseline evidence before changing behavior.

## Artifacts and Notes

Baseline evidence is saved in `initial-evidence.txt`. The full root quick-install log is in `maven-build.log`.
Latest focused Cascades evidence: the selector
`LmdbCascadesOptimizerTest,LmdbRuleRegistryCoverageTest,LmdbCascadesOrFilterRewriteCoverageTest,LmdbCascadesOptionalEliminationCoverageTest,LmdbCorrelatedFilterRuleAdmissibilityTest`
passed 111 tests after the rule-group and access-path rule extractions.

Latest relocation-fix evidence: the selector
`FastAgmsEstimatorMigrationTest,SketchBasedJoinEstimatorMemoryLayoutTest` passed 5 tests after updating the moved source
path and clearing estimator system properties around the tiny memory-layout estimator construction.

Current full-LMDB blocker evidence: full module verification via
`python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` retained
`logs/mvnf/20260708-093313-verify.log` and reports 1886 tests, 8 failures, 0 errors, 54 skipped. The failures are:
`LmdbAASPropertyProjectionPlanningTest.propertyProjectionStartsRequiredIslandFromConstantSubmodelLookup`,
`LmdbCharacteristicSetEstimateTest.usesCharacteristicSetForCorrelatedSubjectStar`,
`LmdbOmniJoinEstimationGeneratedShapeTest.omniEstimatesGeneratedShapesAndComparesToLmdbExecution`,
`LmdbPredicateObjectDomainIndexTest.queryWithStoredNonCanonicalIntegerStillMatchesIntegerFilter`,
three `LmdbSparsePrefixCostTest` methods, and
`QueryBenchmarkTest.subSelectPlanKeepsTopGroupCardinalityBounded`.

Reduced blocker evidence: the selector
`LmdbAASPropertyProjectionPlanningTest,LmdbCharacteristicSetEstimateTest,LmdbOmniJoinEstimationGeneratedShapeTest,LmdbPredicateObjectDomainIndexTest,LmdbSparsePrefixCostTest,QueryBenchmarkTest`
fails 8 of 44 tests. The prior full `mvnf` run retained `logs/mvnf/20260708-071311-verify.log` and reported 1886 tests,
9 failures, 1 error before the two sketch relocation fixes were applied.

Clean-archive baseline comparison: the same reduced selector run in
`/private/tmp/rdf4j-lmdb-baseline-20260708/work` against `git archive HEAD` reports 39 tests, 8 failures, 0 errors.
That proves the current reduced planner failures are branch baseline behavior, not a regression introduced by this
refactor slice.

## Interfaces and Dependencies

New package-private service surface in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`:

- `LmdbPlannerServices` exposes optional views over LMDB-specific statistics services and hides direct
  `LmdbEvaluationStatistics` downcasts from callers.
- `LmdbEstimatorScope` is still planned; this pass only moved factor-cost cache keys, cache entries, and selected
  estimate records to top-level same-package files.
- Shared Cascades helpers still live on `LmdbCascadesRuleProvider`; extracting them into a dedicated support class is
  part of the remaining guarantee/access-path slice.
- `LmdbRule`, `LmdbPlannedRuntimeMetrics`, `LmdbConnectedJoinOrderingRule`, `LmdbConnectedHypergraphJoinImplementationRule`,
  `LmdbFiniteValueRewriteRules`, `LmdbOptionalRules`, `LmdbCorrelatedAntiRules`, `LmdbMaterializedSemiAntiRules`,
  `LmdbStarMultiPredicateScanRule`, and `LmdbPropertyPathImplementationRule` own the first extracted Cascades rule
  groups.

Moved sketch package in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch`:

- `SketchBasedJoinEstimator` remains the primary facade.
- `SketchStatementSource`, `SketchStatementSourceException`, `SketchKeyProvider`, and estimate records remain
  package APIs for LMDB internals and LMDB tests.
- No new third-party dependencies are introduced.
