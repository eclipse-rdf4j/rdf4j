# Preserve API bindings across independent query branches

Maintain this ExecPlan according to `.agent/PLANS.md`. API bindings are values supplied through `TupleQuery.setBinding`; an assignment is a query BIND, aggregate target, or exported projection alias that can replace one of those values.

## Purpose / Big Picture

An API binding must remain usable in an independent UNION arm even when another arm assigns the same variable name. Local assignments must still win where their results are read. The regression test uses an empty MemoryStore, API `x=1`, left `BIND(2 AS ?x) BIND(?x AS ?y)`, and right `FILTER(?x=1) BIND(3 AS ?y)`. Its exact result must contain both integer values 2 and 3, including when the unrelated left target is renamed.

## Progress

- [x] (2026-10-02) Preserve live user work and evidence.
- [x] (2026-10-02) Install live dependencies and observe focused failure.
- [x] (2026-10-02) Implement per-occurrence API substitution provenance.
- [x] (2026-10-02) Cover independent branches and local assignments.
- [x] (2026-10-02) Verify affected modules and preserve evidence.
- [x] (2026-10-02) Audit formatting, source scope, and user work.
- [x] (2026-10-02) Deliver verified source and evidence handoff.
- [ ] [in_progress] Commit scoped fix and verify remote.

## Surprises & Discoveries

Runtime filter visibility is unsuitable for deciding API substitution: a scoped filter can hide an API value before the optimizer substitutes it. The existing optimizer only uses that restrictive runtime context when a whole-query name collector sees any assignment of the same name, so an unrelated branch changes the result. Projection transfer also omits exported assignment effects, which must be retained for the new substitution phase without changing ordinary runtime analysis.

Initial live evidence: `MemoryStoreLocalBindInputRegressionTest#apiBindingRemainsVisibleInIndependentUnionBranch` ran one test, one failure, zero errors/skips, 0.225 seconds. It returned only integer 2 and missed integer 3. The initial report and log are copied under `/tmp/rdf4j-pr6067-live-backup.tMbJQp/evidence/api-union-red/`.

Independent source review identified two additional boundaries in this phase: MultiProjection combined outputs dropped exported writes, and Group treated compatible identity keys as assignments. Permanent direct tests each captured one failure before the corresponding follow-up production changes. Their reports are preserved under `evidence/multiprojection-red/` and `evidence/group-identity-red-20261002-1408/` in the same backup.

The original regression and both follow-up red contracts are now green. The full model suite passed (85 tests, 1 skipped) and evaluation passed (1328 tests, 1 skipped). Full memory ran 876 tests with 12 failures, zero errors, and 3 skips; all failures are plan-text assertions in QueryPlanRetrievalTest. The controlled isolated pre-fix snapshot retained exact original user sources and reproduced all twelve failure identities and XML failure messages/bodies byte-for-byte. No golden plan or cost source was edited for this task.

## Decision Log

Decision (2026-10-02, Codex): use a distinct API-substitution phase in shared binding analysis, rather than special-casing UNION or restoring root values after assignments. Reuse cached evaluator-position traversal and assignment effects. Keep the default analysis phase unchanged. Preserve only API values still live at the preceding phase; actual or possible assignments and unknown operators must prevent unsafe replacement.

Decision (2026-10-02, Codex): during implementation, preserve original tracked/untracked content and append evidence to the existing evidence file after saving its original. No commits, pushes, branch changes, or review comments were in scope for that implementation phase.

Decision (2026-10-02, Codex): combine exported writes across MultiProjection alternatives, and distinguish Group identity keys from aggregate targets and exported child writes only in substitution mode. Collect optimizer replacements from the unmodified algebra and apply them once. Eligible variables already have the same guaranteed fixed API value, so substitution does not change another occurrence's eligibility; preserving the cached analysis avoids a repeated full traversal per occurrence.

Decision (2026-10-02, Codex): establish a passing/failing control for the observed memory plan assertions using pinned pre-fix HEAD plus the preserved original dirty and untracked Java sources. Keep the live source untouched, install baseline sources before baseline tests, then reinstall the live fixed source to restore Maven artifact alignment. Report baseline failures explicitly if the identities and assertion differences match.

## Outcomes & Retrospective

Implementation and validation are complete. The original focused regression now returns exact values 2 and 3 for both original and alpha-renamed queries. Direct analysis tests pass all 36 cases; local BIND/API MemoryStore tests pass all 10. Model passes 85 tests (1 skip), evaluation passes 1328 (1 skip), and federated SERVICE integration passes 29 (no skips). Full memory remains baseline-failing with 12 plan assertions among 876 tests (3 skips); an exact same-selection pre-fix control confirms identical failures rather than a new regression. Existing MemoryScopeSafetyTest passes all 60 cases.

The final LIVE root quick install succeeded after the isolated baseline restored older Maven artifacts. Installed BindingAssignerOptimizer and QueryAlgebraBindingAnalysis classes match the live compiled classes. Copyright, formatting, diff checking, original user artifact hashes, and the evidence-file original-prefix audit passed. At the implementation handoff, the task source changes were the two production classes and two permanent test classes named below, plus this ExecPlan; no commits, pushes, branch changes, or original user-content changes had occurred.

Final evidence report: `/tmp/rdf4j-pr6067-api-binding-fix-handoff-20261002.md`. Baseline comparison: `/tmp/rdf4j-pr6067-baseline.9oFZks/evidence/live-equivalent-plan-control-20261002/comparison.txt`. Final install and preservation audits: `/tmp/rdf4j-pr6067-live-backup.tMbJQp/evidence/final-live-audit-20261002/`. At the implementation handoff, the active plan step was delivery of this evidence.

Decision (2026-10-02, Codex): the user authorized immediate publication of the four implementation/test sources and this plan. Commit only those five paths on the existing branch, preserve all separate filter-cost and benchmark work, and verify the remote branch SHA after pushing.

Revision note (2026-10-02): the implementation handoff was delivered; the active step now tracks the explicitly authorized scoped commit and remote verification.

## Context and Orientation

Work from `/Users/havardottestad/Documents/Programming/rdf4j-temp`, branch `GH-6066-shared-binding-scope-analysis`, original HEAD `89c33837b6d3f3ab102a6d04d93633ecc5e6b2b1`. `core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/helpers/QueryAlgebraBindingAnalysis.java` computes immutable binding contexts at each algebra node and caches output facts. `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/BindingAssignerOptimizer.java` replaces variables with API values. The end-to-end suite is `core/sail/memory/src/test/java/org/eclipse/rdf4j/sail/memory/MemoryStoreLocalBindInputRegressionTest.java`; direct optimizer/analysis contract tests belong in evaluation's `OptimizerBindingAnalysisTest.java`.

## Plan of Work

Add a factory for the API-substitution analysis phase. Its context transfer preserves live API parameters across compatible relational outputs and runtime condition projection, while using the existing sequential BIND, aggregate, and scope logic to kill overwritten provenance. In this phase, projection effects map exported child writes and aliases to their output names. Replace the optimizer's global assignment-name collector with the per-occurrence context. Add tests for reversed/nested branches, sequential/failed writes, exported aliases, optional writes, and MINUS/EXISTS interactions. Existing custom-operator tests must retain their conservative behavior.

## Concrete Steps

The initial required live root clean install passed in 30.171 seconds:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

The failing live command was:

    python3 .codex/skills/mvnf/scripts/mvnf.py MemoryStoreLocalBindInputRegressionTest#apiBindingRemainsVisibleInIndependentUnionBranch --retain-logs

After edits, the exclusive Maven runner performs copyright checking before formatting, then focused tests, followed by the model, evaluation, and memory affected suites. Retain logs and copy red/green reports before later tests overwrite them. Every Maven invocation uses `.m2_repo`; tests never use `-am` or `-q`.

## Validation and Acceptance

The original permanent test must pass with exact integer results 2 and 3 for both original and renamed queries. Related tests must show independent branches retain API values, local/possible writes prevent replacement, and opaque operators stay conservative. Run:

    python3 .codex/skills/mvnf/scripts/mvnf.py OptimizerBindingAnalysisTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py MemoryStoreLocalBindInputRegressionTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/model --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/memory --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --it RepositoryFederatedServiceIntegrationTest --module compliance/repository --retain-logs

No new failures/errors are acceptable. Capture exact totals, skips, commands, and report paths. Final `git diff --check` and original-work hashes must pass.

## Idempotence and Recovery

The runner owns all Maven activity in the live local repository. Retry missing offline dependencies once online, then return offline; retry non-resolution parallel build failures without `-T 1C`. Preserve all artifacts. Original file copies/hashes and the original full binary diff are in `/tmp/rdf4j-pr6067-live-backup.tMbJQp`. If formatting touches original user changes, reverse only identified task-generated formatting hunks after verifying no concurrent user edits; never reset or clean the checkout.

## Artifacts and Notes

The live initial Surefire report is `core/sail/memory/target/surefire-reports/org.eclipse.rdf4j.sail.memory.MemoryStoreLocalBindInputRegressionTest.txt`. Immutable initial red evidence and the original `initial-evidence.txt` are in the backup directory above; the live evidence file now has an appended task section. Build output belongs in `maven-build.log`, test output in retained `logs/mvnf` logs.

## Interfaces and Dependencies

Add a shared analysis factory returning `QueryAlgebraBindingAnalysis` for API-value substitution. Existing factories remain runtime analyses. This requires no new dependencies and no public configuration. The optimizer consumes cached immutable contexts from the unmodified algebra, then applies its recorded replacements and discards the invocation-local analysis.

Revision note (2026-10-02): recorded the distinct substitution phase and preserved-work constraints after the required live failing test, before production changes.

Revision note (2026-10-02): implemented the phase through cached context transfers, preserving default runtime transfer behavior. Added direct phase contracts and end-to-end compositions; moved the single active step to validation.

Revision note (2026-10-02): recorded additional red contracts, fixed MultiProjection and Group effects, and batched substitutions. Added the bounded existing SERVICE integration gate because the changed optimizer path also applies to SERVICE inputs.

Revision note (2026-10-02): recorded focused and module greens plus the full-memory plan assertions. Added an isolated matching-source control without changing the active validation step.

Revision note (2026-10-02): recorded the byte-identical pre-fix memory control, SERVICE green, final live artifact alignment, and preservation audit; moved the single active step to evidence handoff.
