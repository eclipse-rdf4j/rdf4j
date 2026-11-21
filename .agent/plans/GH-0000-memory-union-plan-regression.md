# Resolve MemoryStore UNION plan regression

This ExecPlan follows `.agent/PLANS.md` and records the correction following commit `61cb1d5e8fba00d365640a1c77dc3ec342444140` on `GH-0000-union-scope-inputs`.

## Purpose / Big Picture

Restore the MemoryStore module's passing tests while preserving the corrected propagation of legitimate path bindings into inner UNION planning. The reported failure must be reproduced locally, and any changed expected plan must be justified by actual evaluator behavior and query results. Production code must not change before a failing in-repository test is captured.

## Progress

- [x] Reproduce the reported MemoryStore test failure.
- [x] Determine intended plan and execution behavior.
- [x] Apply the smallest justified regression correction.
- [x] Verify affected tests and module suites.
- [x] Format and prepare the verified correction.
- [ ] [in_progress] Repository CI and pull request review.

## Context and Orientation

The repository is `/workspace/scratch/5c2cf927d55d/rdf4j-review`. The active branch contains the earlier UNION scope proof fix and the follow-up that plans join operands after rebuilding their enclosing hierarchy. `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java` implements join scheduling. `core/sail/memory/src/test/java/org/eclipse/rdf4j/sail/memory/QueryPlanRetrievalTest.java` contains integration assertions for query explanations produced by MemoryStore.

GitHub PR #6409 reports `QueryPlanRetrievalTest.multipleScopesAndFilters` failing in the Java 25 unit-test job. Its MemoryStore run reports 1,545 tests, one failure, no errors, and three skips. The branch and current local checkout both remain at `61cb1d5e8fba00d365640a1c77dc3ec342444140` at investigation start.

## Plan of Work

First reproduce the single reported method with the repository's `mvnf` skill and preserve its Surefire report outside the overwritten target directory. Compare expected and actual plans, then trace differences through the rebuilt hierarchy, binding analysis, cost selection, and evaluation behavior. If the changed plan is correct and deliberately follows the optimization improvement, update the fixture and add a meaningful behavioral assertion where needed; otherwise correct the optimizer and retain the existing expectation. Re-run the focused selection followed by MemoryStore verification and any necessary evaluation-module regression selection. Run the formatter before committing and push to the existing branch without overwriting intervening remote changes.

## Concrete Steps

Run commands from the repository root with `PATH=/workspace/scratch/5c2cf927d55d/build_support/bin:$PATH` so the existing JDK 25 and Maven setup is used.

    python3 .codex/skills/mvnf/scripts/mvnf.py QueryPlanRetrievalTest#multipleScopesAndFilters --module core/sail/memory --retain-logs

After the correction, repeat the same selection, then run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/memory --retain-logs

The helper installs dependencies before testing. Do not use `-am` or `-q` for test runs. Preserve untracked artifacts and prior evidence. Store new evidence under `../evidence/memory-union-plan-*` and append the first failure to `initial-evidence.txt`.

## Validation and Acceptance

The original failing method must pass without dropping or weakening its checks. The MemoryStore module must complete with no failures or errors. Verify any altered query plan against its intended binding inputs and exact result bag; selecting a connected pattern must preserve query semantics. No production optimizer behavior changes in this correction, so repeating the previously passing evaluation-module suite would not exercise a new implementation risk.

The post-change run of the identical method selection passes:

    Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

The complete MemoryStore module passes under its default profile:

    Tests run: 1545, Failures: 0, Errors: 0, Skipped: 3

The entire `QueryPlanRetrievalTest` class reports 29 tests, no failures, no errors, and two existing skips. Failsafe also completes successfully with zero selected integration tests under this profile. Do not claim an additional integration-test execution. The aggregate reports and existing skip names are recorded in `../evidence/memory-union-plan-final-summary.txt`.

Copyright/SPDX validation and `git diff --check` pass. The explicit MemoryStore `process-resources` formatter run succeeds with zero Java files changed. A SHA-256 comparison confirms the final test source is byte-identical to the source verified by the complete module run. This document is the final local review checkpoint; publication updates the existing branch for PR #6409, where repository CI and review continue.

## Surprises & Discoveries

Initial CI inspection identifies one textual explain-plan mismatch, with no other MemoryStore failures reported by that job. The same test first compares exact result bags from the raw parsed algebra and the optimized MemoryStore repository. Its original independent-filter query must be empty; a variant with locally placed filters and duplicate VALUES must return exactly two copies of the expected row. Those checks run before the failing snapshot assertion and passed in CI.

Source inspection explains the changed hierarchy. The independent FILTER's NOT EXISTS reads `s`, while its empty assignment argument cannot guarantee that name. VALUES and the property pattern can supply `s`; flattening the larger join can therefore change the independent FILTER's invocation input. The new input-sensitivity guard retains the source hierarchy. The scoped OPTIONAL group consequently has no incoming `s`, so its score statement must use an unbound estimate instead of the old selective estimate. Its empty scoped assignment can safely move before the score filter. No production defect is established by this failure.

## Decision Log

Use the reported method as the first reproduction instead of rerunning the whole module before the failure is understood. Existing authorization to commit and push this branch applies to this correction. Date: 2026-11-11.

Retain the exact plan assertion and both existing raw-versus-optimized result-bag checks. If local reproduction matches CI, refresh only this one expected plan; do not remove costs, scope flags, algorithms, or ordering from the comparison. Date: 2026-11-11.

## Outcomes & Retrospective

The reported method fails locally with one test, one failure, no errors, and no skips at the exact plan comparison. Both result-bag controls pass before that assertion. The baseline was captured before any test expectation or production edit. The correction updates only this expected plan and adds a short comment explaining the retained independent scopes and unbound score input. No production code or assertion helper is changed. The identical method selection and the complete MemoryStore module now pass. The preceding evaluation-only verification did not cover this downstream snapshot; validating the affected storage module closes that gap.

## Idempotence and Recovery

Keep baseline reports before subsequent Maven runs clear them. Preserve unrelated files. Inspect the remote branch head before publication; a changed head requires reconciling that work before pushing.

## Artifacts and Notes

CI job: https://github.com/eclipse-rdf4j/rdf4j/actions/runs/42998619713/job/130450170463. Local baseline reports are preserved in `../evidence/memory-union-plan-before`; the first report snippet is appended to `initial-evidence.txt`. Passing focused reports are preserved in `../evidence/memory-union-plan-focused`, and complete module reports in `../evidence/memory-union-plan-full`. The source comparison confirms all statements and helpers outside this one expected-plan literal and its comment are byte-for-byte unchanged. An independent review agrees with the plan update and found no weakening of coverage.

    Test set: org.eclipse.rdf4j.sail.memory.QueryPlanRetrievalTest
    Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    QueryPlanRetrievalTest.multipleScopesAndFilters <<< FAILURE!

## Interfaces and Dependencies

No new dependency or public API is anticipated. Prefer existing MemoryStore integration infrastructure and binding-analysis contracts.

Revision: consolidated the reproduced failure, the justified exact snapshot correction, unchanged semantic coverage, complete MemoryStore verification, and final formatting evidence. Repository CI and pull request review remain after publication.
