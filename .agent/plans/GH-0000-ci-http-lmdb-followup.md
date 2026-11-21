# Resolve HTTP cancellation and LMDB estimation CI failures

This ExecPlan follows `.agent/PLANS.md`. It starts from `HASMAC-AS/rdf4j:GH-0000-union-scope-inputs` at `05cb676a97e76825fa6e9a1afd1226dc29914d9e`, which incorporates upstream main `1acc280c489577aa51aeac5226e41b32889782a7`.

## Purpose / Big Picture

Determine whether the remaining Java CI failures reveal production defects or unreliable test coordination, and correct the demonstrated causes. Preserve the UNION optimizer and MemoryStore fixes already verified by the preceding successful CI run. The user has authorized investigation, implementation, and publication of the necessary corrections on this branch.

## Progress

- [x] Reproduce and explain both Java failures.
- [x] Implement minimal corrections with regression coverage.
- [x] Verify focused cases and HTTP module.
- [x] Format and review the completed corrections.
- [ ] [in_progress] Publish and verify full module CI.

## Context and Orientation

The active repository is `/workspace/scratch/5c2cf927d55d/rdf4j-review`. The prior transient checkout and build cache expired, so the branch has been cloned again. Temurin JDK 25.0.5 and Maven 3.9.11 are restored outside the repository; the initial root quick clean install passed for all 125 modules. Existing `AGENTS.md`, `.agent/PLANS.md`, and the `mvnf` skill are unchanged from the earlier verified branch.

The failing HTTP test is `core/http/client/src/test/java/org/eclipse/rdf4j/http/client/CancellableOperationCoordinatorTest.java`, method `remoteCancellationErrorsCompleteAsyncWaitersAndDoNotRetry`. The corresponding coordinator owns cancellation attempts and delivers their results to concurrent callers. The reported failure says that an expected exception was absent.

The failing LMDB test is `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatisticsMemoizationTest.java`, method `usesSketchEstimatorForFilterCardinalityWhenReady`. It loads eight rows, prepares the statistics estimator, and expects the equality FILTER's estimated cardinality to be lower than that of the underlying statement pattern. A sketch is a compact statistical summary used to estimate cardinalities. The test calls statistics directly rather than the UNION/join-order optimization pipeline.

GitHub run `43032411685` also has six browser jobs that failed to resolve Ubuntu package servers during dependency installation. Those jobs executed no browser tests. An attempt to rerun failed jobs through the connector returned HTTP 403, `Resource not accessible by integration`; the connector lacks Actions write permission. Meaningful source corrections can still be pushed through the authorized branch workflow and receive its normal PR CI run.

## Plan of Work

Inspect both failing tests and their production paths independently. Reproduce the relevant schedules or estimator states with the smallest in-repository tests and preserve Surefire reports before any production change. For HTTP cancellation, distinguish callers already waiting on an attempt from callers that arrive after the handle is removed. Fix test synchronization if the test incorrectly assumes task submission means the task has joined an in-flight attempt; change production only if its contract is demonstrably broken.

For LMDB, record the actual filter and pattern estimates and identify the controlling sketch snapshot, live sampling, fallback, or readiness decision. Use deterministic inputs and explicit coordination where required. Do not hide a real estimation defect by weakening the inequality, lengthening a timeout, or retrying until a lucky sample passes.

After the focused regressions pass, verify the HTTP client module and the full LMDB unit suite. Keep local Maven executions serialized because the repository and dependency cache are shared. The preceding LMDB CI suite took 26 minutes, so run final local LMDB verification alongside the new remote CI run after focused verification, HTTP module verification, and formatting pass. Record that full LMDB verification is still running at publication; do not claim it passed until the reports exist. This avoids serializing two long independent validations. Preserve unrelated work and publish with an expected-head check. Inspect the new CI run and diagnose any remaining failures before final handoff.

## Concrete Steps

Run from the repository root, using the restored JDK 25/Maven wrapper in `../build_support/bin` when available. First perform the required offline root quick install; if dependencies are missing, repeat online, then return to offline builds. Do not use `-am` or `-q` in test runs.

    python3 .codex/skills/mvnf/scripts/mvnf.py 'CancellableOperationCoordinatorTest#remoteCancellationErrorsCompleteAsyncWaitersAndDoNotRetry' --module core/http/client --retain-logs --no-offline
    python3 .codex/skills/mvnf/scripts/mvnf.py 'LmdbEvaluationStatisticsMemoizationTest#usesSketchEstimatorForFilterCardinalityWhenReady+doesNotLetOneRowSampleOverrideSelectiveFilterCardinality+requiresEnoughEvidenceForFilterSampling+revalidatesPersistedFilterSamplingEvidence' --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/http/client --retain-logs -- -DskipITs -Dmaven.compiler.showWarnings=false
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -T 2C process-resources

The HTTP reproduction used the deliberately controlled late-arrival fixture before the final test correction. The LMDB selection ran before and after its production change. The full standard LMDB unit-suite command to run alongside remote CI is:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs -PskipSlowTests -Dmaven.compiler.showWarnings=false

Evidence belongs under `../evidence/ci-http-lmdb-*`; compact first-failure output is preserved in root `initial-evidence.txt` before another test run overwrites reports.

## Validation and Acceptance

Both original assertions must retain their intended strength. A concurrent HTTP waiter must observe the attempt's fatal failure, while a genuinely late caller must receive the documented completed-handle behavior. A ready LMDB estimator must use the applicable equality selectivity without producing a false positive from an undersized or biased sample. Any additional behavior established by reproduction must be documented and tested.

Focused reproductions must fail before the corresponding correction and pass afterward. All applicable affected-module tests must pass. Formatting, source headers, whitespace checks, and publication verification must pass. Distinguish completed CI results from pending, skipped, or infrastructure-blocked jobs.

## Surprises & Discoveries

The HTTP failure is a test scheduling race: it releases the remote callback immediately after submitting waiter tasks, so cancellation can finish and remove the handle before those tasks enter the coordinator. A test-only controlled late-arrival schedule reproduced the exact CI assertion locally: one test, one failure, zero errors, `Expecting code to raise a throwable`. The baseline source and report are preserved in `../evidence/ci-http-lmdb-http-before`; the compact report is appended to `initial-evidence.txt`. No production cancellation defect was established.

The LMDB sampling evidence policy accepts one row for populations of up to 64 rows, and also lowers the required evidence to an undersized scan budget for larger populations. A one-row all-pass prefix can therefore override the existing selective equality estimate. Sampled cache entries, including version-3 persisted entries, are returned before that policy is checked. Deterministic regression tests and positive controls have been prepared before any production change.

The LMDB test-only baseline ran 19 invocations with nine failures and zero errors. The deterministic one-row case reported `pattern=8.0, filter=8.0, pass ratio=1.0, source=SAMPLED, evidence=1`, where the filtered estimate must be 1. Other failures demonstrate insufficient live evidence, persisted samples bypassing the policy, and cardinality exceptions disguised as a one-row population. Ten controls, including the original timing-sensitive CI test, passed. Reports and baseline sources are preserved in `../evidence/ci-http-lmdb-lmdb-before` and compact output in `initial-evidence.txt`.

The first post-fix selection passed 18 of 19 cases. The remaining failure was an incorrect expectation in the new 128-row control: the existing heuristic deliberately clamps selectivity to a minimum of 0.05, while the test had assumed exact 1/128 selectivity. `SketchBasedJoinEstimator.MIN_FILTER_MULTIPLIER` and `normalizeFilterMultiplier` establish this behavior. Correct the new control to expect 0.05 while keeping its exact HEURISTIC source and absent-sample-evidence assertions; the original eight-row cardinality assertion remains unchanged. Preserve this intermediate report in `../evidence/ci-http-lmdb-lmdb-first-after`.

The first focused HTTP verify could not reach tests offline because formatter and test dependencies were absent from the restored cache. Its root quick install passed. Retry the same selection online once, then return to offline execution.

## Decision Log

Investigate the HTTP and LMDB failures in parallel with disjoint file ownership; serialize builds and test execution. Date: 2026-11-11.

Do not alter the browser workflow based on a transient DNS failure alone. The connector's rerun permission error is reported, and no alternative credential or permission workaround is used. Date: 2026-11-11.

Overlap full local LMDB validation with normal GitHub PR CI after the focused regressions, HTTP module suite, and formatting pass. The last LMDB CI job took 26:12; the validations use independent machines and identical stable source, so overlap reduces elapsed time without relaxing any final verification requirement. Date: 2026-11-11.

## Outcomes & Retrospective

Both causes now have local failing evidence. The final HTTP fixture correction has been applied without a production cancellation change. The narrowly scoped LMDB production correction is applied: enforce the existing 16-row floor, cap it by a positive finite estimated population, validate cached evidence, reject an inadequate row budget before native sampling, and retain unknown status after a cardinality exception. The independent review found no new semantic or cache-cost regression in this approach. The estimate remains a heuristic; it does not prove exhaustive or unbiased sampling.

The matching LMDB focused selection now passes all 19 invocations, with zero failures, errors, or skips. The complete HTTP client module reports 218 tests, zero failures, zero errors, and eight existing skips. Its cancellation coordinator class reports 19 successful tests, including the original in-flight failure assertion and the separate late-caller control. Full reports are preserved in `../evidence/ci-http-lmdb-lmdb-focused-after` and `../evidence/ci-http-lmdb-http-after`.

The repository-wide formatter completed successfully, the copyright/SPDX check passed, and `git diff --check` is clean. Publication and full LMDB/remote CI validation are the remaining checkpoint. Their results must be reported separately from the focused and HTTP checks already completed.

## Idempotence and Recovery

Preserve baseline and final reports outside target directories. Do not revert another contributor's changes or overwrite a changed remote head. If a test-only explanation is established, keep production unchanged and explain the corrected assertion preconditions. If an implementation edit happens before a failing local reproduction, undo only that edit and restore the test-first order.

## Artifacts and Notes

Original HTTP CI job: https://github.com/eclipse-rdf4j/rdf4j/actions/runs/43032411685/job/130563991801.

Original LMDB CI job: https://github.com/eclipse-rdf4j/rdf4j/actions/runs/43032411685/job/130563992083.

The prior complete green run is https://github.com/eclipse-rdf4j/rdf4j/actions/runs/43008345880. It includes the MemoryStore snapshot correction at `56ca866f15b4dac4794669b3279bd7fc8abb52fb`.

## Interfaces and Dependencies

No new dependency, public API, production test hook, or persisted schema was added. The HTTP fixture uses public thread state and futures; the LMDB checks reuse the existing candidate population and sample-size fields. Cached validation adds constant-time work and no additional native cardinality probe.

Revision: created the living investigation plan before implementation changes, including the expired environment and denied Actions rerun.

Revision 2026-11-11: recorded the restored build, prepared deterministic test-only reproductions, and identified the sampled-cache path that must enforce the same evidence policy.

Revision 2026-11-11: recorded the reproduced failures, implemented both corrections, corrected the new test's unsupported exact-heuristic assumption, and verified the focused regressions plus the HTTP module before the publication checkpoint.
