# Clean up retired LMDB code and repair query regressions

This living ExecPlan follows `.agent/PLANS.md` and repository-root `PLANS.md`. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current.

## Purpose / Big Picture

Remove code left behind by this branch's planner/statistics replacements, and make the LMDB and query-evaluation tests pass without weakening valid query semantics or lifecycle guarantees. The user confirmed scope is this branch's LMDB/query-engine changes. Root build, focused reproductions, and full affected module verification establish the outcome.

## Progress

- [x] Establish current build and failure baseline: root build green; evaluation 79/48/2, LMDB 60/50/0.
- [x] Verify dead candidates and remove decoder closure, parser, logging helper, private planner overload, checkpoint field, and branch-only filter estimate.
- [x] Repair reproduced failures: evaluation focused 87/0/0 and LMDB 60/0/0.
- [x] Verify complete affected suites after integration repairs.
- [x] Audit changes and report remaining limitations.

## Surprises & Discoveries

The checkout starts at d13767b49c on GH-0000-lmdb-predicate-guarantees with no tracked edits. Many untracked regression tests and evidence files already exist and must be retained. Prior local reports describe failures in these tests but are not evidence of the current run. The previous cleanup removed the older Frontier implementation; mapped statistics remain active.

## Decision Log

Use existing untracked regression tests as first reproduction candidates, preserve their assertions unless the contract proves them wrong, and preserve all existing artifacts. Use Routine D for this multi-module task while retaining focused failing evidence before production changes. Do not commit, push, or change branches. No additional dependencies are planned.

## Outcomes & Retrospective

All initially reproduced failures are repaired: the 79-case evaluation baseline had 48 failures and two errors, and the 60-case LMDB baseline had 50 failures. The matching repaired selections pass. Dead-code cleanup removes the unused ByteBuffer decoder and exclusive helpers, duplicate index parser, diagnostic method, private planner adapter, unused checkpoint field, and branch-only filter-estimate record. Final complete verification reports evaluation 1582 tests, LMDB units 2051 tests, and LMDB integrations 143 tests, with zero failures/errors throughout and 104 existing skips across LMDB suites. Test verification uses japicmp.skip=true because ordinary verify separately fails the branch's pre-existing binary compatibility gate for removed public Frontier settings. No compatibility policy was changed, no tests were deleted or disabled, and changes remain uncommitted.

## Context and Orientation

`core/queryalgebra/evaluation` contains shared evaluation iterators and the packed query optimizer. `core/sail/lmdb` integrates these with native storage, statistics, and LMDB optimizers. Regression tests in both modules include existing untracked files from earlier investigations. `testsuites/benchmark` and query compliance modules may require follow-up when changed interfaces cross module boundaries. A dead-code candidate needs source and runtime registration checks, including reflection, service configuration, subclasses, and tests; lack of direct calls alone is insufficient.

## Plan of Work

First complete the required root quick clean install and reproduce existing failing test classes using `.codex/skills/mvnf/scripts/mvnf.py`. Persist the first current results by appending a clearly dated section to `initial-evidence.txt` and retain separate task-specific evidence so existing history is preserved. Diagnose each failure against query results or lifecycle contracts, extend tests for related cases, and fix the responsible implementation. Audit retired code using existing scan tools only as candidate generators; inspect each deletion before applying it. Run the same focused selection after each repair and then both complete module suites. Review disabled integration coverage and run relevant feasible integration selections.

## Concrete Steps

Run commands from the repository root. The initial build is `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install` with full output in `maven-build.log`. Use `python3 .codex/skills/mvnf/scripts/mvnf.py <selector> --retain-logs` for focused tests and each affected module. Never use `-am` or `-q` with tests. If the current branch's API removals prevent japicmp verification, record that separately and use a documented test-only verification pass, without concealing the compatibility failure. Read Surefire and Failsafe text/XML reports, preserve exact commands and logs, and update this document with measured outcomes.

## Validation and Acceptance

Correct SPARQL results must hold under nested OPTIONAL, MINUS, EXISTS, filters, duplicate-sensitive operators, and user variable names. Iterators must close owned resources on exhaustion, errors and cancellation. LMDB transaction and datatype cache tests must preserve storage consistency and termination. Every removed implementation must have no remaining live use. Both affected module test suites must have zero failures/errors; any existing skips or external gate limitations must be stated explicitly.

## Idempotence and Recovery

Re-run focused tests after fixes and retain the original red snippets before reports are overwritten. Do not remove unrelated untracked files or broadly restore files. Keep changes reviewable and tied to demonstrated causes. Existing `initial-evidence.txt` must not be overwritten.

## Artifacts and Notes

Current build log: `maven-build.log`. Current task evidence will use `initial-evidence.cleanup-repair-20260909.txt` and `post-evidence.cleanup-repair-20260909.txt`. Test logs are under `logs/mvnf/` and reports under the affected module's `target/surefire-reports` or `target/failsafe-reports`.

## Interfaces and Dependencies

Preserve public API and upstream compatibility unless a necessary correction is justified by the actual call path. Retain mapped Frontier statistics and active native planner interfaces. No new external dependencies or configuration switches are required by this plan.

Current evidence: `initial-evidence.cleanup-repair-20260909.txt`, `initial-evidence.cleanup-repair-lmdb-20260909.txt`, and `post-evidence.cleanup-repair-evaluation-focused-20260909.txt`. Reviewed saved local patches against current sources and applied only tested repair paths; untouched tests were retained. Corrected two hash-join contract assertions that wrongly inferred constants from user variable spelling.

Cleanup baseline: 61/0/0 in `initial-evidence.cleanup-neutral-20260909.txt`. The broad source scanner was treated as advisory: preserve VarHandle-accessed previousNamespaceEntry, JFR event fields, reflection test contracts, constant holders, and upstream public mask APIs. Root formatting succeeded; its two unrelated query-model test whitespace edits were reversed precisely. LMDB full verify runs with `-Djapicmp.skip=true` because the ordinary focused run passed all tests then failed on pre-existing removed public Frontier settings.

Header verification: stopped the repository-wide script after it entered historical `.mvnf` outputs, then ran the unmodified script on copies of all 16 edited Java files. It exposed an existing license URL typo in PackedSearchCheckpoint; corrected that comment and reran successfully. Evidence: `cleanup-repair-scoped-copyright.log`.

The full integration sweep exposed `IllegalArgumentException: mapped semi/anti estimate is invalid` from LmdbPackedCostModel.mappedSemiAntiEstimate, in LmdbFrontierThemeCoverageIT. Retained evidence in `initial-evidence.cleanup-integration-20260909.txt`; diagnose and reproduce narrowly before another production change.

Follow-up integration triage: first full LMDB run completed 2043 unit tests (0 failures/errors, 7 skips); integration failures saved in initial-evidence.cleanup-full-lmdb-20260909.txt and full reports archived at /tmp/cleanup-first-full-lmdb-reports.tgz. Two new zero-confidence estimator cases failed, then passed after rejecting untrusted mapped semi/anti estimates. Cache publication now uses acquire/release and value initialization flags are volatile; 63 focused tests pass. The medical null-label failure passed when run alone, so report it as intermittent and retain the broad red evidence.

Full evaluation run was interrupted in unbounded 17-factor sparse planning (thread dump /tmp/cleanup-evaluation-thread-dump.txt), after 715 tests and one stale BindingMask assertion failed. Corrected the legacy-name assertion and bounded the prefix-order test search while keeping every behavior assertion. Added leaf/join filter invocation-domain tests; awaiting observed results before changing planner costing.

Filter invocation accounting: new tests for filters over a leaf and a join both reported expected 10 invocations versus actual 1. Added the existing complete-domain certification at the filter composition boundary. Matching 33-test selection now passes, including the corrected BindingMask and bounded sparse-prefix tests. Evidence: initial-evidence.cleanup-filter-invocations-20260909.txt and post-evidence.cleanup-filter-invocations-20260909.txt. Benchmark setup now closes its repository on initialization/planning failure, retaining suppressed cleanup exceptions, to prevent the observed subsequent store-lock error. Full evaluation rerun in progress.

Complete query-evaluation verification is green: 1580 tests, zero failures/errors/skips, log logs/mvnf/20260908-234630-verify.log, retained evidence post-evidence.cleanup-full-evaluation-20260909.txt. Final root formatting succeeded; reversed only its two unrelated query-model whitespace changes. All 28 edited/new Java source copies pass the unmodified copyright script; git diff --check passes. Final complete LMDB verify started with log logs/mvnf/20260908-235139-verify.log, after root install passed in 23.006 seconds.

Final LMDB unit phase passed: 2046 tests, zero failures/errors, seven existing skips. Evidence is post-evidence.cleanup-final-lmdb-unit-20260909.txt. Integration phase is still in progress; goal remains active.

Second complete LMDB sweep: 2046 unit tests green (7 skips); 143 integration tests, five assertion failures, zero errors, 97 skips. All initial runtime exceptions and the pharma invocation-cost regression are repaired. Retained full evidence in initial-evidence.cleanup-second-full-lmdb-20260909.txt and /tmp/cleanup-second-full-lmdb-reports.tgz. Fresh and cached PackedPlanCacheTest regressions both reproduce a dropped database_exact guarantee (2 failures); materialization now republishes the typed guarantee for runtime learning. Full cache-class verification is in progress, followed by medical restart and affected integrations. Corrected q9 finite-domain and forced-hash expectations to accept proven cheaper indexed alternatives while preserving semantic and execution checks.

The integration rerun exposed two remaining causes: scalar fallback summaries become UNRESOLVED when detached/restored, and contextual FILTER learning applies a per-invocation posterior to the whole inherited prefix (100K to 1), underpricing correlated anti-joins. Preserved reports in initial-evidence.cleanup-learning-context-it-20260909.txt and stopped the failing sweep to run newly added narrow regressions before production edits. Fresh/cache guarantee tests are green (47-case class).

Narrow learning regressions reproduced four failures across five tests: whole-prefix 100000 rows became a one-invocation exact fact of 1; detached positive/zero scalar fallbacks became UNRESOLVED. The mapped learning session now verifies prefix membership against the canonical logical operator group and keeps physical-only feedback for inherited contexts. Scalar fallback audit summaries preserve their explicit classification and uncertainty. Complete 27-case mapped estimator/learning selection is green, log logs/mvnf/20260909-003423-verify.log. Current integration sweep log logs/mvnf/20260909-003526-verify.log. All 35 edited/new Java source copies pass the copyright check; git diff --check is clean.

Complete final LMDB integrations passed: 143 tests, zero failures/errors, 97 existing skips; 489.022 seconds total test time. Evidence: post-evidence.cleanup-final-integration-20260909.txt, full log logs/mvnf/20260909-003526-verify.log, archive /tmp/cleanup-final-integration-reports.tgz. The medical learning/stability class passed all three tests in 17.92 seconds (formerly 256 seconds with failure/timeout). Final root formatting succeeded and only its two unrelated query-model whitespace changes were reversed. Final complete LMDB unit run is now active, followed by complete query evaluation verification.

The final unit sweep completed 2046 tests with one allocation assertion failure (196608 bytes / 4096 feedback publications), all other tests green. Saved initial-evidence.cleanup-final-allocation-20260909.txt and /tmp/cleanup-final-unit-allocation-reports.tgz. The same allocation method passes alone. The newly added learning test spies on final LmdbOperatorFeedbackStats, globally instrumenting the production class with Mockito; testing this fixture interaction before replacing the spy with real feedback training. Count comparison also found the earlier archive included an additional five-test vector subprocess report; final counts will use the current complete Maven reports, without assuming a fixed total.

Confirmed the allocation regression was test instrumentation: running the new learning class before the zero-allocation method reproduced exactly 196608 bytes. Replaced its Mockito spy on final LmdbOperatorFeedbackStats with real feedback-target training/publication. The identical four-test combined selection now passes with zero-allocation assertion unchanged (logs/mvnf/20260909-010907-verify.log; initial/post-evidence.cleanup-mock-instrumentation-20260909.txt). No production allocation workaround was added. Final formatting and complete suite reruns follow.

Final completion: complete evaluation reports 1582 tests, zero failures/errors/skips (post-evidence.cleanup-final-evaluation-20260909.txt). Complete LMDB units report 2051 tests, zero failures/errors, seven skips (post-evidence.cleanup-complete-lmdb-unit-20260909.txt); this report set includes the five vector subprocess cases. Complete LMDB integrations report 143 tests, zero failures/errors, 97 skips (post-evidence.cleanup-final-integration-20260909.txt). Root formatting, scoped copyright validation of all 35 edited/new Java files, and final git diff --check pass. Final status is saved in cleanup-repair-final-status.txt; 33 tracked files changed within the two scoped modules, plus two new regression classes, with prior untracked artifacts preserved. Branch and HEAD remain GH-0000-lmdb-predicate-guarantees at d13767b49c. No commits or pushes. Ordinary verify still has the separately documented pre-existing japicmp incompatibility gate; these complete test runs skip only that gate.
