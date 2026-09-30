# Remove verified dead LMDB, Cascades, and experimental optimizer code

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while the work proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The LMDB optimizer branch contains legacy planner implementations, abandoned experimental APIs, private helpers,
state, and telemetry that have no live caller. Leaving these declarations in place makes it harder to distinguish
the current packed Cascades, DPHyp, Frontier, sketch, and learned-feedback paths from retired designs. This cleanup
removes only code whose lack of use is established by source-reference, bytecode-call, reflection, registration,
documentation, and clean-compilation checks. Afterward, the affected query-evaluation and LMDB modules must compile
and pass their relevant tests with query semantics, optimizer ordering, persistence formats, concurrency behavior,
and current ranking telemetry unchanged.

The only intentional compatibility change is deletion of the explicitly selected unused `@Experimental` API. No
replacement or deprecation bridge is provided because neither production module has a caller for those types.

## Progress

- [x] (2026-08-23 20:41Z) Captured branch, commit, full status, and the pre-existing tracked patch in
  `/tmp/rdf4j-dead-cleanup-baseline.patch` with SHA-256
  `af1997911e1fa4b84a4cee534e90e707e8d931fd11abe9d745f8af04578ddc67`.
- [x] (2026-08-23 20:41Z) Authored this self-contained ExecPlan before changing production code.
- [x] (2026-08-23 20:43Z) Ran the mandatory offline root quick clean install; the reactor completed with
  `BUILD SUCCESS`, including query-algebra evaluation and LMDB.
- [x] (2026-08-23 20:48Z) Ran and preserved the five pre-change focused selections in `initial-evidence.txt`:
  LMDB pipeline 43/0/0/0, leading-field sort 1/0/0/0, rewrite certificate 9/0/0/0, stream schema 11/0/0/0, and
  optimizer catalog 8/0/0/0.
- [x] (2026-08-23 20:49Z) Deleted the eight whole dead implementations and obsolete projection-specific test;
  preserved the behavior-level packed projection-placement contract. The two affected modules completed a quick
  clean install successfully.
- [x] (2026-08-23 20:51Z) Deleted the seven-type experimental API closure and only its direct tests, then removed
  the producerless reconciliation component from `LeoEstimateDiff`. Query-evaluation and LMDB completed another
  quick clean install successfully.
- [x] (2026-08-23 20:56Z) Deleted the stale state clusters, four constants, reflection-only sort-variant test, and
  all 27 private methods in the approved manifest. The combined affected-module quick clean install passed.
- [x] (2026-08-23 20:59Z) Performed the second dependency-closure audit. It found newly orphaned private members
  `PackedMemo.crossGroupInvariant`, `LmdbFrontierPackedCostSession.closePrimitiveMemo`, `logicalGroupFactKeys`,
  `recycleEmissionScratchPool`, and `EmissionScratchPool.idle`; removed them, repeated the one-reference scan to no
  candidates, confirmed the preserved-boundary types remain, and completed another combined quick clean install
  successfully.
- [x] (2026-08-23 21:29Z) Ran copyright, the existing scoped formatter, all five post-change focused selections, and
  both affected module suites. All focused selections and query-evaluation's 1,432 tests passed. The LMDB selection
  ran 2,263 tests with zero assertion failures and five errors sharing the join-telemetry overlap invariant; the
  exact isolated error was reproduced at the pre-cleanup commit, so this is recorded as unrelated red. Neither
  excluded Theme IT produced a Surefire report.
- [x] (2026-08-23 21:38Z) Completed the final diff, source/bytecode/reference, header, artifact, and acceptance
  audits. `git diff --check` is clean; the intended tracked cleanup is 38 files with 5 insertions and 5,074
  deletions; the user's `FrontierOmniLayout.java` patch is byte-for-byte identical to its recorded baseline; and the
  final full offline quick install completed with `BUILD SUCCESS`.

## Surprises & Discoveries

- Observation: The worktree was already dirty before this cleanup.
  Evidence: the only tracked change was a formatting-only wrap in
  `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/frontier/FrontierOmniLayout.java`; numerous untracked
  ExecPlans, research files, benchmark evidence, profiles, scripts, and archives were also present. All belong to
  the user and must remain untouched.
- Observation: The root formatter already has the required artifact boundary.
  Evidence: root `pom.xml` limits Java to `src/main/java/**/*.java` and `src/test/java/**/*.java`, limits XML to
  source-layout paths, and excludes hidden, target, generated-POM, and site-theme paths. No formatter configuration
  change is needed.
- Observation: Removing callers exposed a small private-only dependency closure that was not independently dead
  before the approved methods were removed.
  Evidence: comparison against `HEAD` showed current/baseline identifier counts of 1/2 for `crossGroupInvariant`,
  1/4 for `closePrimitiveMemo`, 1/5 for `logicalGroupFactKeys`, 1/2 for `recycleEmissionScratchPool`, and finally 1/2
  for `EmissionScratchPool.idle`. The repeated changed-file one-reference scan was empty after their removal.
- Observation: The optimizer catalog's negative-test reference still pointed to a method deleted with the retired
  projection optimizer test surface.
  Evidence: the first post-change `OptimizerRuleCatalogTest` failed on the missing method; after redirecting the
  negative contract to `lmdbPipelineRunsNormalizationBeforePackedCascadesByDefault`, its documentation-equality
  test identified the one corresponding generated catalog row. Updating both yielded 8/0/0/0.
- Observation: The requested LMDB module selection is red for a pre-existing join-telemetry invariant violation.
  Evidence: the module ran 2,263 tests with five errors, all `A join telemetry side cannot have overlapping
  evaluations`. The exact smallest-scope method
  `heterogeneousUnionStateRemainsExactThroughOptionalAndNotExists` failed identically both after cleanup and in a
  detached worktree at pre-cleanup commit `fb16ef0ba4195295b153729fa08e9a4a6700f42f` (1/0/1/0 in both runs).
- Observation: The copyright checker, unlike the Maven formatter, has no source-path filter and recursively visits
  preserved `.mvnf` workspace build trees whose layout contains nested POMs outside `target` directories.
  Evidence: the required pre-format checker completed successfully, but a duplicate final invocation spent several
  minutes printing `.mvnf/workspaces/.../pom.xml` paths and was stopped. A final direct audit of all 21 modified Java
  files found both valid RDF4J copyright lines and SPDX identifiers. The formatter itself completed successfully and
  changed no generated/research artifact, so no formatter exclusion was added.
- Observation: Policy did not permit forced deletion of the temporary detached baseline worktree after its generated
  build output made it dirty.
  Evidence: `/private/tmp/rdf4j-dead-cleanup-baseline` remains registered at the baseline commit. It is outside the
  primary checkout, and no attempt was made to bypass the preserve-untracked-artifacts rule.

## Decision Log

- Decision: Use Routine D because this is a large cross-module cleanup with an intentional experimental-API source
  compatibility change.
  Rationale: a living, restartable removal manifest and incremental compilation provide safer control than treating
  the work as a collection of unrelated local refactors.
  Date/Author: 2026-08-23 / Codex
- Decision: Preserve behavior-level projection coverage and delete tests that merely instantiate or reflect over
  dead implementations.
  Rationale: tests should protect the live packed-planner invariant, not force retired classes to remain present.
  Date/Author: 2026-08-23 / Codex
- Decision: Remove `RewriteMode` and `RewriteContext` with `RewriteGuard`.
  Rationale: their only production consumer is `RewriteGuard`, so keeping them would leave an unusable experimental
  API fragment.
  Date/Author: 2026-08-23 / Codex
- Decision: Remove the producerless reconciliation component from `LeoEstimateDiff`, but preserve live plan-ranking
  and learned-evidence fields.
  Rationale: the deleted reconciler is the only writer of reconciliation decision, reason, and confidence; current
  `plannedLeoPlanRanking...` telemetry remains active and is a separate contract.
  Date/Author: 2026-08-23 / Codex
- Decision: Do not run or modify `LmdbThemeQueryRegressionIT` or `LmdbThemeFastestRunSnapshotIT`.
  Rationale: the user explicitly excluded these tests and their snapshots from this work.
  Date/Author: 2026-08-23 / Codex
- Decision: Remove only the newly orphaned private members found by the second closure scan; do not expand the public
  API removal manifest.
  Rationale: their pre-change reference counts prove they became unreachable because of this cleanup, while another
  public removal would exceed the user-approved compatibility boundary.
  Date/Author: 2026-08-23 / Codex
- Decision: Retarget the `projection-below-order` catalog's negative contract to the surviving generic pipeline test
  and regenerate only the matching catalog row.
  Rationale: this keeps the behavior-level guarantee that eager projection removal is absent without retaining a
  class-specific test for the deleted optimizer.
  Date/Author: 2026-08-23 / Codex
- Decision: Classify the five LMDB module errors as pre-existing unrelated red and do not expand this cleanup into a
  join-telemetry behavior fix.
  Rationale: the same isolated error is reproducible at the exact pre-cleanup commit; changing telemetry execution
  semantics would violate the behavior-neutral cleanup boundary and require a separate TDD bugfix.
  Date/Author: 2026-08-23 / Codex

## Outcomes & Retrospective

The cleanup is implemented. It deletes 15 production types and three class/reflection-only test types, removes all
27 approved private methods, deletes the approved state/constants, and closes five newly orphaned private members.
The intended tracked diff, excluding the user's pre-existing `FrontierOmniLayout.java` formatting patch, spans 38
files with 5 insertions and 5,074 deletions. The only additional intended artifacts are this untracked ExecPlan and a
one-row update to the already-untracked generated optimizer catalog document.

All five focused post-change selections are green: `LmdbOptimizerPipelineTest` 42/0/0/0,
`TripleStoreTest#testLeadingFieldSortIgnoresFullKeySortProperty` 1/0/0/0, `RewriteCertificateTest` 3/0/0/0,
`StreamBindingSchemaTest` 6/0/0/0, and `OptimizerRuleCatalogTest` 8/0/0/0. The full query-evaluation module is green
at 1,432/0/0/0. The LMDB unit selection is unrelated red at 2,263 tests, zero failures, five errors, and seven skips;
all five errors share the join-telemetry overlap invariant, and the exact isolated error reproduces unchanged at the
pre-cleanup commit. Because Surefire failed, the LMDB Failsafe phase was unrun. Neither excluded Theme IT produced a
report, and no excluded test, snapshot, or archived benchmark result was modified.

The final full offline quick install is green, `git diff --check` is clean, and exact live-source plus compiled-bytecode
searches contain no deleted type, reflection/registration reference, or producerless reconciliation field. Every
removed private declaration is absent from its owning source. Active `LeoPlanRanking` producers and consumers,
rewrite certificate infrastructure, packed rule contracts, `FrontierBottomKRows`, `ValueStore.logValues()`, and
`OptimizerCatalogRenderer` remain. No query behavior, persistence/sketch schema, Frontier lifecycle, locking,
optimizer ordering, or performance code was changed, and no performance improvement is claimed.

The initial dirty checkout remains intact. The unrelated `FrontierOmniLayout.java` diff still has SHA-256
`af1997911e1fa4b84a4cee534e90e707e8d931fd11abe9d745f8af04578ddc67` and compares byte-for-byte equal with the
recorded baseline patch. No file was staged, committed, or pushed. The temporary detached baseline worktree remains
because forced cleanup was rejected under the preserve-untracked-artifacts rule.

## Context and Orientation

The shared optimizer abstractions live under `core/queryalgebra/evaluation`. The LMDB-specific pipeline, packed
planner integration, store, telemetry, and native storage code live under `core/sail/lmdb`. The live optimizer path
is `LmdbQueryOptimizerPipeline` to `LmdbCascadesOptimizer` and then the packed Cascades/DPHyp implementation. Frontier
V2, synopsis/sketch evidence, and learned feedback supply cost evidence to that live path. A "memo" is the packed
planner's collection of logically equivalent alternatives; eager prepasses must not erase alternatives before memo
import.

The current checkout is branch `GH-0000-lmdb-predicate-guarantees` at
`fb16ef0ba4195295b153729fa08e9a4a6700f42f`. The baseline tracked patch is stored outside the repository so formatter
or cleanup changes can be distinguished from the user's pre-existing `FrontierOmniLayout.java` wrap.

## Plan of Work

First run the repository-mandated root quick clean install and the five focused baseline selections. Preserve compact
initial evidence at repository root and retained Maven logs. This is a behavior-neutral Routine D cleanup, so no
manufactured failing test is required. The existing projection-placement test is the behavioral contract that must
stay green.

Delete the whole dead production types `LmdbTupleExprEstimateAnnotator`, `LmdbProjectionPushdownOptimizer`,
`LmdbLongLongCounts`, `LmdbLongReservoir`, `LmdbTupleExprFacts`, `MaskFactorCostCacheEntry`,
`SetFactorCostCacheEntry`, and `LeoEstimateReconciler`. Delete `LmdbProjectionPushdownOptimizerTest`; remove the two
class-specific absence assertions from `LmdbOptimizerPipelineTest`; retain both the generic absence check for
`ProjectionRemovalOptimizer` and
`narrowingProjectionSurvivesPrepassesAndIsCostedBeforeBlockingOrder`.

Delete the unused experimental types `SemanticScope`, `IndependentExistsBranchReorderSafety`, `RewriteGuard`,
`RewriteMetadata`, `ScalarDependencyAnalyzer`, `RewriteMode`, and `RewriteContext`. Delete
`IndependentExistsBranchReorderSafetyTest`. In `RewriteCertificateTest`, remove only the reflection and behavior
coverage for `RewriteGuard`, `RewriteMetadata`, `RewriteMode`, and `RewriteContext`. In `StreamBindingSchemaTest`,
remove only test methods that invoke `ScalarDependencyAnalyzer`; keep direct schema coverage. Audit
`OptimizerRuleCatalogTest` and package documentation for exact references. Preserve `RewriteCertificate`,
`RewriteAssumption`, `RewriteSafety`, `RuleProof`, and all current certificate producers.

Delete the `decision` component and rendering from experimental `LeoEstimateDiff`. In
`LmdbOperatorFeedbackStats.explainEstimateDiff`, stop reading `plannedLeoReconciliationDecision` and call the revised
record constructor. Remove all live reads or writes of `plannedLeoReconciliationDecision`,
`plannedLeoReconciliationReason`, and `plannedLeoReconciliationConfidence`. Do not alter archived Theme benchmark
result text and do not remove current plan-ranking metrics.

Remove dead state clusters: `ValueStore.txnLock`; the `LmdbStore.defaultEvalStratFactory` field, automatic-default
getter, import, resolver-update branch, pipeline lookup, and pipeline setter branch; `TripleStore.DEFAULT_INDEXES`;
and `TripleStore.leadingFieldSortAlgorithm` with its one-value enum. Delete
`TripleStoreLeadingFieldSortVariantTest`, while preserving
`TripleStoreTest.testLeadingFieldSortIgnoresFullKeySortProperty`. Remove constants
`LmdbOperatorFeedbackStats.PLANNED_LEO_CANDIDATE_COST_SCORE`,
`LmdbOperatorFeedbackStats.PLANNED_LEO_CANDIDATE_ID`, `FrontierHeavyObjectAccumulator.UNSEEN`, and
`ScalarEvaluationEffects.FN_STRING_IRI`.

Remove this exact private-method manifest: `LmdbFilterSimplifierOptimizer.leftDeep`;
`LmdbFrontierPackedCostSession.logicalGroupFactKey`, `uncorrelatedLogicalLearningKey`, `sameExactSourceContent`,
`correlatedProbeWorkRows`, `containsExists`, `containsAnyExists`, `degradeBinaryOperator`, `saturatedLongAdd`, and
`allBoundMasks`; `LmdbStore.isSketchEstimatorReadyNonBlocking` and
`getAutomaticDefaultEvaluationStrategyFactory`; `FrontierLearningModel.observeExact`;
`LmdbOperatorFeedbackStats.actualInvocationCount`, `predictedInvocationBasis`, `sourceRowsScannedActual`, and
`firstAvailableActual`; `PackedWinnerTable.saturatedAdd`; `PackedIncumbentSearch.hasFiniteJoinAlternative`;
`PackedMemo.decisionIndex` and `ensureTargetGroup`; `PackedLogicalRuleProgram.metricString` and `metricDouble`;
`PackedQueryCodec.Builder.finiteBindingAssignment`; and `PackedJoinEnumerator.adjacentFactorMask`,
`emitJoinForInheritedPrefix`, and `selectedJoinCost`.

Compile after each major group. Then repeat source, reflection-string, service-registration, module-registration, and
bytecode-call searches. Newly orphaned private or package-private helpers may be removed only with the same proof.
Do not remove another public or protected type: record it as a later candidate instead. Clean only imports and
comments made obsolete by these exact removals.

## Concrete Steps

Run every command from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Never use `-am` or `-q`
with tests.

Perform the mandatory initial build with the root clean-install command from `AGENTS.md`, using `-B -ntp`,
`-Dmaven.compiler.showWarnings=false`, `-T 1C`, `-o`, `-Dmaven.repo.local=.m2_repo`, and `-Pquick clean install`.
Keep full output in `maven-build.log` and print only errors plus the reactor summary.

Run these baseline selections through the retained-log runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptimizerPipelineTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py TripleStoreTest#testLeadingFieldSortIgnoresFullKeySortProperty --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py RewriteCertificateTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py StreamBindingSchemaTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py OptimizerRuleCatalogTest --retain-logs

Persist the compact initial summaries in top-level `initial-evidence.txt` before later runs overwrite reports.

After the edits, rerun those five selections, then run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -Dit.test='!LmdbThemeQueryRegressionIT,!LmdbThemeFastestRunSnapshotIT'

Before formatting, run `scripts/checkCopyrightPresent.sh` from the `scripts` directory. Then run the existing root
formatter with `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`; this `-q` invocation formats sources
and does not execute tests. Compare the result with `/tmp/rdf4j-dead-cleanup-baseline.patch`. Run the mandatory final
root quick clean install, `git diff --check`, and the final symbol/reference audit.

## Validation and Acceptance

The focused selections and both affected module suites must pass. The LMDB module selection must exclude exactly
`LmdbThemeQueryRegressionIT` and `LmdbThemeFastestRunSnapshotIT`; if either runs, stop and correct the selector.
Record focused-green, module-green, unrelated-red, blocked, and unrun scopes separately.

Acceptance additionally requires that the narrowing projection reaches memo import and remains below the selected
blocking order; no live reconciliation metric has a producer or consumer; live LEO plan ranking remains; no deleted
symbol appears in live Java, reflection strings, services, or module registration; and no query semantics, optimizer
ordering, wire/on-disk format, Frontier lifecycle, sketch schema, native locking, or concurrency behavior changes.
Historical result text is exempt from the live-reference check.

The final diff must not modify either excluded Theme IT, their snapshots, archived benchmark results, or the user's
untracked artifacts. The exact initial `FrontierOmniLayout.java` diff must remain present but outside the cleanup's
intended-file set.

## Idempotence and Recovery

Reference and compile checks are safe to repeat. `mvnf` intentionally replaces report directories, so preserve each
important compact summary before the next selection. If a deletion causes a live compilation reference, restore
only that deletion using a minimal `apply_patch`, update `Surprises & Discoveries` and `Decision Log`, and reassess
the candidate; never reset or clean the dirty worktree. If formatting touches an unrelated tracked file, reconstruct
its exact pre-format state from the recorded baseline patch rather than discarding the user's work.

## Artifacts and Notes

The baseline patch is `/tmp/rdf4j-dead-cleanup-baseline.patch`. Maven full output belongs in `maven-build.log`, focused
verify logs in `logs/mvnf`, and the compact baseline in `initial-evidence.txt`. These evidence artifacts are not part
of the code removal and must not be staged unless the user later requests their inclusion.

## Interfaces and Dependencies

No dependency is added. The intentionally deleted public experimental interfaces are `SemanticScope`,
`IndependentExistsBranchReorderSafety`, `RewriteGuard`, `RewriteMetadata`, `ScalarDependencyAnalyzer`, `RewriteMode`,
and `RewriteContext`. `LeoEstimateDiff` remains public and experimental but loses its producerless `decision` record
component. All live packed planner, DPHyp, Frontier, sketch, learned-feedback, and rewrite-certificate interfaces
remain unchanged.

Preserve `FrontierBottomKRows` for migration rollback, `PackedOptimizerRule` and `PackedRuleContext` for the checked-in
roadmap contract, `ValueStore.logValues()` as a debugger hook, and `OptimizerCatalogRenderer` as a CLI/documentation
generator. Make no performance claim; no benchmark is required for behavior-neutral deletion.

Change note (2026-08-23 20:41Z): created the initial self-contained ExecPlan from the user-approved removal plan and
recorded the dirty-worktree and formatter boundaries before implementation.

Change note (2026-08-23 21:29Z): recorded the implemented removal closure, focused and module verification, catalog
manifest discovery, and baseline-confirmed unrelated LMDB suite failure.

Change note (2026-08-23 21:38Z): finalized verification and retrospective with exact diff, reference, bytecode,
artifact-preservation, module-green, unrelated-red, excluded, and unrun scopes.
