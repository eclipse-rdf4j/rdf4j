# Repair four query composition regressions

This execution plan follows `.agent/PLANS.md`. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current. Each bounded behavior change follows Routine A: capture an in-repository failing automated regression before modifying its production code.

## Purpose / Big Picture

Queries must preserve their answers when DISTINCT surrounds a subquery with OFFSET, when EXISTS refers to an outer binding, when a transaction deletes one of several equivalent DISTINCT representatives, and when a resource equality surrounds OPTIONAL. The existing optimizations remove work but currently cross boundaries where that work affects the result. Fix those boundaries while retaining the safe optimizations.

## Progress

- [done] Capture baseline build and regression failures
- [done] Fix four causes with composition coverage
- [done] Run focused and relevant module verification
- [done] Format changes and audit final scope
- [done] Review remaining changes for reproducible bugs
- [done] Format follow-up fixes and finalize evidence

## Context and Orientation

Work in `/Users/havardottestad/Documents/Programming/rdf4j-small-things`, branch `GH-0000-lmdb-predicate-guarantees`, initially at `2a576f28e1f013395cb44ae61165e7ecc798cf1f`. The preceding review compared the working tree with develop merge base `2169c9bc62c3d5ace654844497b846e34abb850c`. Existing tracked edits and staged deletions concern estimator cleanup; preserve them. The initial tracked patch is retained at `/tmp/rdf4j-predicate-review-fixes-initial.patch`. Do not commit, stage, change branches, or delete unrelated artifacts.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSetSemanticsOptimizer.java` propagates a boolean saying that duplicate rows cannot affect the final result. A Slice (OFFSET/LIMIT) and a volatile scalar expression such as UUID can make duplicates observable before an outer DISTINCT. Tests belong next to the existing optimizer tests in the LMDB module.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBoundSimplifierOptimizer.java` currently derives possible bindings from a filter argument and initial query bindings. EXISTS additionally supplies bindings from the current outer result. Its nested filters must not mistake those bindings for impossible variables.

`core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailDatasetImpl.java` applies pending transaction deletions after reading the underlying dataset. LMDB DISTINCT cursor skipping selects one indexed representative per requested value; choosing that representative before deletions can lose a surviving value. `LmdbCascadesOptimizer` already obtains an exact committed snapshot capability via `SailDatasetTripleTermSource.getSnapshotEpoch()`. A dataset with a changeset overlay does not have that capability. Use the existing capability boundary to prevent unsafe physical plans if all plan reuse paths preserve the distinction; otherwise enforce the boundary in the dataset API without discarding necessary filter metadata.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/FilterIterator.java` supplies incoming variable bindings to a filtered argument. Its resource equality optimization currently traverses OPTIONAL, whose unmatched-row behavior is not preserved by arbitrary prebinding. A UNION that only sometimes binds the equality variable exposes the error.

## Plan of Work

The root agent handles set-semantics boundaries, integration, and the final audit. Independent agents handle correlated BOUND, deletion overlays, and resource equality. Each agent owns separate production and test files. They may add tests during the initial baseline build, but may not build concurrently with that raw Maven command. After baseline completion, use named Maven workspaces `review-set`, `review-bound`, `review-overlay`, and `review-equality` to separate build outputs, reports, and dependency writes. Source files remain shared, so coordinate formatting and cross-module source changes.

First add regressions that assert observable results, plus nearby variants that exercise composition and preserve safe optimizations. Run the smallest relevant method or class and immediately save the failing report before touching production. Then make the minimal general correction and rerun the identical selection. Extend coverage for nested boundaries, sibling context restoration, volatile expressions, initial query bindings, and transactions where applicable. A regression that unexpectedly passes requires renewed investigation, not a speculative production edit.

After all focused selections pass, run the relevant evaluation and LMDB suites through isolated workspaces. Triage failures against the changed paths; distinguish unrelated baseline failures and interrupted/unrun suites. Check copyright headers and serialize repository formatting. Audit the final diff against the initial tracked patch to preserve unrelated work.

## Concrete Steps

Run commands from the repository root. The baseline command is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Its full output is retained in `maven-build.log`, with errors and reactor summaries shown in the task. It skips tests. A missing offline dependency permits one identical online retry; another failure requires the same command without reactor parallelism.

For focused tests, use:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace review-set --retain-logs LmdbSetSemanticsBoundaryTest#preservesDuplicateRowsBeforeOffset

Replace workspace and selector with each issue's test. The runner performs a quick install before verify. Do not pass `-am` or `-q` to test commands. Save red evidence in `initial-evidence.<workspace>.txt` using `scripts/agent-evidence.py` and the exact report directory printed by the runner; preserve pre-existing evidence. Logs are retained under `.mvnf/workspaces/<workspace>/logs/<run-id>/` and reports under `.mvnf/workspaces/<workspace>/build/org.eclipse.rdf4j/<artifact>/6.1.0-SNAPSHOT/`.

## Validation and Acceptance

The OFFSET reproduction must return the single literal 1 for `SELECT DISTINCT ?x WHERE { { SELECT ?x WHERE { VALUES ?x { 1 1 } } OFFSET 1 } }`. UUID evaluation must preserve the number of evaluations before DISTINCT. Existing duplicate elimination in safe positive contexts must remain covered and pass.

`SELECT ?s WHERE { ?s <urn:p> ?o FILTER EXISTS { FILTER(BOUND(?s)) } }` must return subjects matching the outer triple pattern, and its NOT EXISTS variant must reject them. Nested and partially bound inputs must retain the correct behavior.

Within a snapshot transaction, removing the first indexed triple for a predicate must not remove that predicate from `SELECT DISTINCT ?p WHERE { ?s ?p ?o }` when other matching triples survive. Committed queries should still use the DISTINCT access path when safe.

The resource-equality regression must evaluate the precompiled filter with incoming `wanted=:b` and return no rows for a UNION/OPTIONAL argument where the original OPTIONAL binds `y=:a` but prebinding `y=:b` would incorrectly preserve an unmatched row. Reversed equalities and nested forms must agree; safe positive patterns must retain their lookup optimization.

## Idempotence and Recovery

Use each named workspace consistently and preserve its logs. Never replace existing evidence with a later run. If a test fails to compile or cannot start, that is not the required red behavioral evidence; fix the fixture or environment first. If production was accidentally edited before red evidence, undo only the agent's own patch and reproduce first. Do not restore unrelated working changes.

## Surprises & Discoveries

All four initial findings now have failing automated regressions. The baseline quick clean install passed in 38.415 seconds. This machine uses Temurin Java 26 and had approximately 466 GiB available at the start. A sandboxed process listing was denied; no process termination is authorized or needed.

The evaluation module's post-fix focused test passed, but its subsequent binary API check failed on pre-existing staged removals of JoinOrderPlanner and JoinStatsProvider and pre-existing public method removals in FilterSelectivityKeys and JoinFactorCostModel.CostContext. Retain this gate failure separately and pass `-Djapicmp.skip=true` on subsequent test runs; do not change the unrelated estimator cleanup.

Cross-review found two composition paths within the reported causes. COUNT DISTINCT's finite-membership rewrite independently reordered volatile filter evaluations (expected count 1, got 2); it now requires a repeatable group input. Null-rejecting OPTIONAL proof also needs correlated inputs, including the pre-group rows used by aggregate operands. The aggregate regression expected 2 but returned 0 before the correction. Unknown scalar tuple owners conservatively retain referenced incoming variables.

An exploratory ORDER BY counter fixture triggered an unrelated failure in the unoptimized OrderIterator. Replace its execution with a structural assertion that the optimizer retains all inputs to volatile ordering. A nested BIND group creates an independent scope in parsed SPARQL; test the intended per-incoming-row sibling case using an explicit in-scope algebra Extension, preserving the two-row expectation.

The first full evaluation module run encountered unchanged MINUS spill-file tests whose runtime java.io.tmpdir differs from Java 26's startup-cached NIO temp directory in a named workspace. Keep the initial failure and verify with aligned JVM startup settings if the remaining module gate permits it.

## Decision Log

Use existing effect classification and dataset capabilities rather than query-specific conditions. Avoid new dependencies. Separate test output with Maven workspaces; serialize source formatting. The user authorized fixes but did not request commits or publication.

## Artifacts and Notes

Initial build: `maven-build.log`. Initial tracked-state snapshot: `/tmp/rdf4j-predicate-review-fixes-initial.patch`. Red reports are preserved in top-level `initial-evidence.review-set.txt`, `initial-evidence.review-bound.txt`, `initial-evidence.review-overlay.txt`, and `initial-evidence.review-equality.txt`.

The initial set-boundary command above failed with `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, expected `[1]` but got `[]`. Its log is `.mvnf/workspaces/review-set/logs/20260907T201827.408000Z-77577-00bf43ef/verify.log`. BOUND's ten-case selector failed eight cases. The overlay selector lost predicate 0 (one failure). The equality selector produced an unexpected row in both equality directions (two failures).

## Interfaces and Dependencies

Reuse `ScalarEvaluationEffects` for expression repeatability and `SailDataset.getSnapshotEpoch()` for an exact committed read view where appropriate. No new public API, dependency, configuration switch, or persisted format is planned. New tests use the repository's existing JUnit, query parser, evaluation strategy, and LMDB repository fixtures.

## Outcomes & Retrospective

The four original findings are repaired in five production files with four focused regression test files. Focused selections pass 78 tests: 36 set-boundary/existing optimizer cases, 24 correlation cases, 8 deletion-overlay cases, and 10 resource-equality cases. All initial failures remain in the four initial-evidence files; correlation's final summary is also in post-evidence.review-bound.txt.

The broader evaluation module completed 1,538 tests with only the two unrelated workspace temp-directory spill-discovery failures (no errors/skips). The broader LMDB run was stopped when the user explicitly requested a quick finish: its completed reports contain 903 tests, zero failures/errors, and two skips. This is partial coverage, not a full-module pass. See post-evidence.review-lmdb-partial.txt. No compatibility-green claim is made because japicmp was skipped for the existing estimator API removals.

Scoped root Spotless formatting completed successfully and git diff --check passed. The nine changed Java files have valid copyright/SPDX and Codex signatures. The repository-wide copyright scan was stopped with the lengthy jobs at the user's request after it traversed extensive unrelated historical workspace builds; scoped header checks passed. The initial 15 tracked patches are preserved exactly. No source was staged, committed, pushed, or removed.

The user now asks to continue reviewing for more bugs and fixing confirmed cases. Keep the same branch and dirty-worktree preservation rules. Parallel read-only investigations cover evaluation runtime, LMDB logical proofs, and LMDB storage/native execution; root covers remaining packed-plan proof paths. Each new production change still requires a captured failing in-repo test. Use focused verification to keep this continuation timely; do not restart lengthy full-module gates without a new need.

The follow-up review reproduced and repaired four more defects. Finite-membership COUNT DISTINCT rewrites lost variables needed by enclosing filters or only possibly produced by remaining OPTIONAL/UNION/EXISTS expressions. The rewrite now requires assured retained bindings for every observed or counted value removed by EXISTS. Packed finite-type rewrites reused the literal variable name `type`, capturing a counted entity or code variable; generated domain names now avoid every existing symbol. ValueStore's separate cache ID/value/core arrays could expose another datatype's core classification under a colliding concurrent update; one immutable entry now carries the related fields. Memoized OPTIONAL did not publish its active materialization scan to close(); lifecycle ownership now covers materialization, delayed opening, streaming fallback, cache closure, and suppressed close errors.

Every additional fix has saved red evidence: initial-evidence.review-bound-followup.txt, initial-evidence.review-packed-followup.txt, initial-evidence.review-overlay-datatype.txt, and initial-evidence.review-equality.followup-cancellation.txt. Final focused selections pass 49 membership/set-semantics tests, 10 packed-binding safety/positive controls, 39 datatype cache/store tests, and 26 OPTIONAL lifecycle tests. Together with the earlier 24 correlation, 8 overlay, and 10 equality tests, these selections cover 166 distinct test cases with no failures, errors, or skips. Source-only suspicions about numeric equality, branch predicate compatibility, and one filter-placement scenario did not fail their regression checks and received no speculative production patch.

The final authorized Java manifest contains 16 files (eight production and eight test files), retained at /tmp/rdf4j-review-final-files.json. The original 15 tracked patches still compare exactly with the starting snapshot. All four named Maven workspaces are idle before final serialized formatting.

Final scoped root Spotless formatting succeeded (review-fixes-format.log). An audit compared Java tokens and import sets before/after formatting in every changed production file and found no semantic token changes. git diff --check passed. Full final tracked/untracked status is retained at /tmp/rdf4j-review-final-status.txt; branch remains GH-0000-lmdb-predicate-guarantees. All original 15 tracked patches are still exactly preserved. The eight fixes and focused validation are complete; all task-owned Maven runners are stopped, and no commit or push was requested or performed.

Plan created at the start of the authorized repair to preserve scope, test-first gates, and parallel ownership.
