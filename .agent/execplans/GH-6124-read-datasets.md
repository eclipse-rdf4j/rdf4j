# Keep implicit read datasets independent of serializable tracking

This living ExecPlan follows `.agent/PLANS.md` and repository AGENTS.md. It uses Routine D, with failing-first evidence for behavior changes. All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. No branch change, commit, push, dependency, public API, or configuration change is authorized.

## Purpose / Big Picture


An auto-commit read is a read made without an active transaction. Such reads cannot commit a serializable read set, so the private roots owned by SnapshotSailStore must not track their statement patterns, even when the configured default is SERIALIZABLE. Explicit transactions retain their existing semantics. The implementation must also release datasets and owned forks when acquisition or iterator creation fails, so abandoned readers cannot prevent later writes from flushing.

## Progress


- [done] Confirm baseline and preserve existing artifacts.
- [done] Capture failing behavior and cleanup regressions.
- [done] Implement dataset acquisition and owned cleanup.
- [done] Complete lifecycle and compatibility test coverage.
- [done] Complete reviewer requested exception coverage.
- [done] Verify focused and concrete store suites.
- [done] Format, audit, and document final evidence.

## Context and Orientation


`core/sail/base` contains the shared connection and dataset machinery. `SnapshotSailStore` owns two private `SailSourceBranch` roots, one for explicit and one for inferred statements. A source is a mutable state with an optional fork; its repeated SNAPSHOT-or-higher dataset calls must retain one backing state. A dataset represents one read and remains valid until it closes. `SailSourceBranch.dataset(level, false)` already bypasses that root's own observing layer while preserving its snapshot, changeset overlays, reader registration, compression, and auto-flush behavior. `SailSourceConnection` currently opens a source and dataset separately, with missing cleanup on several exception paths.

`DelegatingSailDataset` deliberately does not forward statement counting: observing datasets rely on counting through their own statement access. A new ownership-only wrapper must explicitly forward counting to the complete acquired dataset without changing the shared wrapper. Real-store tests live in `core/sail/memory`, `core/sail/nativerdf`, `core/sail/lmdb`, and `core/sail/extensible-store`. Concrete optimistic suites initialize the shared repository factory; do not execute their shared SerializableTest alone.

## Decision Log


2026-10-06, user-approved: exempt auto-commit private Snapshot roots at every default level, including SERIALIZABLE. Do not apply the broader explicit-transaction fix described by the supplied review's Appendix A.

2026-10-06, user-approved: compare the selected overridable getter result by reference with the corresponding private root. Decorated or replaced sources retain their ordinary fork path and observation behavior. Do not unwrap sources or select arbitrary SailSourceBranch instances.

2026-10-06, architecture review: replace disposable read sources with dataset acquisition. A stateless source facade would violate repeated SNAPSHOT dataset semantics. Dataset-level acquisition avoids advertising a mutable or repeatedly queried source contract.

2026-10-06, architecture review: add package-private SourceClosingSailDataset, with count forwarding, an atomic close claim, dataset-before-source cleanup, and primary exceptions with cleanup failures suppressed. Direct root datasets retain their existing wrappers. No speed improvement is claimed without benchmarks.

## Surprises & Discoveries


The supplied review describes commit 4a64bd9911, whereas execution starts at b516d6d942. The latter already adds populated explicit/inferred routing coverage and two real-store serializable-owner commit regressions. Those must remain intact. Existing legacy tests lost effective SERIALIZABLE coverage when the test fixture advertised all literal isolation levels; named SERIALIZABLE variants will restore it.

Root quick installs skip formatting and tests. Test verify runs activate source formatting on Java 25+, so early test runs explicitly set formatter.skip and impsort.skip. The existing tracked/index `AD tools/runtime-osgi/dependency-reduced-pom-802975037613373.xml` and untracked `.codex/config.toml` and `papers2.zip` must be preserved.

The root dataset wrapper released its registration only after delegated close succeeded. The new `implicitReadReleasesRootWhenBackingDatasetCloseFails` failed with one failure and zero errors: the later backing approval was expected once but remained zero. Its artifacts are preserved at `/private/tmp/rdf4j-gh6124-close-red.66tE62/`. The fix attempts registration removal, compression, and auto-flush even after backing close fails, keeping the original exception primary.

The independent Sol review found no introduced correctness issue. It suggested a direct regression for query evaluation failure translation and cleanup because the existing setup-failure case throws SailException before the evaluation step. A fixture-selected EvaluationStrategy will throw QueryEvaluationException from its compiled step, and the test will verify that exception remains the cause while dataset/source cleanup failures remain suppressed. This requires test-only fixture changes; production is unchanged. The reviewer also identified existing non-Snapshot second-fork construction and shared-union partial-iterator suppression limitations outside the approved Snapshot root scope; no general claim about those paths is made.

The dedicated query exception case passed one test with zero failures/errors/skips; artifacts are in `/private/tmp/rdf4j-gh6124-validation.2U64HT/qee-focused/`. The first scoped formatter command reported success without changing source hashes, while some obvious wrapping/import sorting remained. This prompted verification that the selector actually matches six Java files; a successful Maven exit alone is insufficient formatting evidence.

Local Spotless 2.34 bytecode showed that `spotlessFiles` is a Java regex matched against each absolute path, rather than module-relative comma-separated globs. The corrected exact six-file regex produced: `Spotless.Java is keeping 6 files clean - 3 were changed to be clean, 3 were already clean`. Only SnapshotSailStore.java, SnapshotSailStoreTest.java, and SourceClosingSailDatasetTest.java changed; the deltas were formatting and import order only. The matching scoped check passed. Full base passed 220 tests both before and after that actual formatter correction. The post-format suite includes 194 Snapshot tests and 13 wrapper tests, with zero failures, errors, or skips; its full reports and logs are preserved at `/private/tmp/rdf4j-gh6124-validation.2U64HT/base-suite-post-format/`.

## Plan of Work


The first milestone establishes four failing public-path regressions before production changes. Its acceptance is a preserved Surefire report with four failures and zero errors against HEAD b516d6d942. That milestone and the following dataset acquisition implementation milestone are complete: the identical four-method selection now passes.

The lifecycle milestone makes each isolation/inference/priming/connection/data combination independently visible as a DynamicTest, preserves ordinary source and explicit transaction behavior, and verifies owned cleanup under failures and concurrent close. A further one-method red gate proved root registration leakage on backing close failure before changing its cleanup. The focused and complete two-class commands below must pass to complete this milestone. The final milestones run base and concrete store acceptance, verify coverage of the failed serializable commit path, then format only the six changed Java files and audit preserved workspace state.

First rename the serializable-default observation test and assert zero observations. Add public-path regressions for a fork lost when dataset acquisition throws, dataset/source leaks when statement iterator creation throws, and a root reader that blocks later flushes after failure. Observe and save failures against unchanged production before editing production.

Expose package-private `SailSourceBranch.datasetWithoutObservations(IsolationLevel)` and remove its read-only fork. Replace SnapshotSailStore's fork method with `datasetForRead(boolean, IsolationLevel)`, choosing its overridable source once. Direct owned roots use unobserved dataset acquisition; other sources use a normal fork transferred to SourceClosingSailDataset.open.

Add SourceClosingSailDataset beside DelegatingSailDataset. Its open method owns an already selected source and cleans partial acquisitions. Its close method claims closure once before cleanup, attempts dataset then source, preserves primary RuntimeException/Error with later failures suppressed, and guards self-suppression. It forwards getStatementCount to the complete delegate, while inherited forwarding preserves other dataset operations.

Centralize SailSourceConnection read acquisition and iterator creation. Inactive Snapshot roots use the optimized datasets; active transactions and other stores retain normal branch selection. Inferred datasets are acquired before explicit datasets; second-acquisition failure closes the first. Iterator factory failure closes the dataset. Scalar reads use try-with-resources, evaluation retains partial iteration cleanup, and successful iterators own their datasets. Keep transaction/write branch behavior outside this refactor.

Expand tests to independent named cases for default levels, explicit/inferred routing, cold and primed roots, same/new connections, populated/empty reads, exhaustion/early close, writer overlap, ordinary source repeated snapshots, decorator/nested fallback, explicit observation boundaries, serializable conflicts, and restored legacy coverage. Share observation and dataset-close hooks. Simplify redundant query text while retaining populated repository-result lifetime checks. Add direct ownership-wrapper capability and cleanup tests.

## Concrete Steps


The initial baseline is already saved at `/private/tmp/rdf4j-small-things-baseline-b516d6d942.60ZZF2/`: root offline clean install passed in 27.606 seconds and SnapshotSailStoreTest passed 13 tests. Do not duplicate that install.

The coordinator owns edits. Existing worker `/root/baseline_validation` owns serialized Maven execution, report preservation, polling, and ordinary recovery. Run tests with:

    python3 .codex/skills/mvnf/scripts/mvnf.py SnapshotSailStoreTest#serializableDefaultImplicitReadsDoNotObserve --retain-logs -- -Dformatter.skip=true -Dimpsort.skip=true

Run the cleanup regression methods similarly, preserving each report before another selection overwrites it. Save the first red report in repository-root initial-evidence.txt, preserving any previous initial evidence in task log artifacts first. No tests may use -am or -q. mvnf installs current modules to `.m2_repo` before tests; use offline mode, retry once online only for missing artifacts, then return offline.

After implementation run focused regressions, SourceClosingSailDatasetTest, SnapshotSailStoreTest, and the complete core/sail/base module. Run the failed SERIALIZABLE commit variant alone with `-- -Pjacoco` and inspect coverage for retained serializable sink selection and the flush catch block.

The targeted coverage command is:

    python3 .codex/skills/mvnf/scripts/mvnf.py 'SnapshotSailStoreTest#testRollbackExceptionDuringSerializableCommit' --retain-logs -- -Pjacoco -Dformatter.skip=true -Dimpsort.skip=true

It passed one test. The saved JaCoCo XML for SailSourceBranch.java covers line 344 (`prepared = serializable`, ci=4) and the flush catch at lines 384 (`changes.clear()`, ci=3), 385 (`prepared = null`, ci=3), and 386 (`throw e`, ci=2). Here ci is covered instruction count, not invocation count. This proves execution reaches the retained serializable sink and failure cleanup in the focused test. XML/HTML, Surefire reports, commands and full logs are preserved at `/private/tmp/rdf4j-gh6124-validation.2U64HT/jacoco-serializable/`.

The combined class selection is:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/base --retain-logs -- -Dtest=SnapshotSailStoreTest,SourceClosingSailDatasetTest -DskipITs -Dformatter.skip=true -Dimpsort.skip=true

The dynamic default matrix contains 144 fresh fixtures: six defaults, two inference settings, three priming modes (cold, serializable commit, serializable rollback), same or separate connection, and populated or empty data. Every case exhausts and early-closes statement and plain SELECT results, checks context/namespace/size readers, and verifies observations after guard closure before shutdown. Additional independent tests cover all six owner-conflict defaults, six populated routing defaults, five explicit transaction-local levels (NONE writes directly to roots), three retained-reader defaults, and repeated ordinary SNAPSHOT/SERIALIZABLE source datasets. SourceClosingSailDatasetTest checks specialized counting, observing counting, inherited dataset capabilities, failure suppression, and concurrent exactly-once close without changing shared delegation semantics.

Run concrete IsolationLevelTest, OptimisticIsolationTest, ConcurrencyTest, and ConnectionTest classes for MemoryStore, NativeStore, LmdbStore, and applicable ExtensibleStore classes. Verify both inherited implicit-read owner-commit methods ran in the concrete optimistic suites. Run:

    python3 .codex/skills/mvnf/scripts/mvnf.py --it LmdbStoreModelLifecycleIT#forcedOverflowTransactionsRemainConsistentAcrossLongLivedReadersAndShutdown --retain-logs

The Memory selection passed with declared report totals of 473 tests, zero failures/errors, and one skip. MemoryOptimisticIsolationTest contains 117 passing testcase elements, including both required inherited owner-commit methods, but its XML root declares tests=0. Consequently the declared aggregate excludes that suite; report the optimistic testcase count separately rather than silently treating 473 as the complete execution count. The exact report inconsistency and passing method evidence are preserved at `/private/tmp/rdf4j-gh6124-validation.2U64HT/memory-group/optimistic-method-evidence.txt`. Native/LMDB selections must preserve the same distinction if it occurs there.

The Native selection passed with declared totals of 478 tests, zero failures/errors, and one skip. NativeOptimisticIsolationTest likewise declares zero but contains 117 passing testcase elements; both required inherited methods have nonzero recorded durations and no skipped/failure/error node. Its evidence and per-report counts are preserved in `/private/tmp/rdf4j-gh6124-validation.2U64HT/nativerdf-group/`. Memory's declared classes contain 10 isolation, 451 connection, and 12 concurrency tests; the optimistic entries remain separately counted.

The LMDB selection passed with declared totals of 478 tests, zero failures/errors, and one skip. LmdbOptimisticIsolationTest declares zero but contains 117 passing testcase elements, including both required owner-commit methods with recorded times of 0.005 and 0.008 seconds and no skipped/failure/error child. Per-report counts, method evidence and complete reports/logs are preserved at `/private/tmp/rdf4j-gh6124-validation.2U64HT/lmdb-group/`. Extensible and forced-overflow lifecycle validation remain.

The Extensible selection passed 247 tests (10 isolation, 225 connection, 12 concurrency), zero failures/errors, and one skip. Declared suite counts and testcase-element counts agree for all three reports. Evidence is preserved at `/private/tmp/rdf4j-gh6124-validation.2U64HT/extensible-group/`. The named LMDB forced-overflow lifecycle integration test is the last behavioral acceptance gate.

The final lifecycle command passed four parameterized testcases, zero failures/errors/skips. Its Failsafe report, full logs, install log and exact command are preserved at `/private/tmp/rdf4j-gh6124-validation.2U64HT/lmdb-lifecycle-it/`. The evidence summarizer warned that the metadata-only failsafe-summary.xml contains no testsuite elements; the actual test report and its four testcase elements confirm the result.

Exact concrete selections (each mvnf invocation installs current dependencies first):

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/memory --retain-logs -- -Dtest=MemoryStoreIsolationLevelTest,MemoryOptimisticIsolationTest,MemoryStoreConcurrencyTest,MemoryStoreConnectionTest -DskipITs -Dformatter.skip=true -Dimpsort.skip=true
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/nativerdf --retain-logs -- -Dtest=NativeStoreIsolationLevelTest,NativeOptimisticIsolationTest,NativeStoreConcurrencyTest,NativeStoreConnectionTest -DskipITs -Dformatter.skip=true -Dimpsort.skip=true
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -Dtest=LmdbStoreIsolationLevelTest,LmdbOptimisticIsolationTest,LmdbStoreConcurrencyTest,LmdbStoreConnectionTest -DskipITs -Dformatter.skip=true -Dimpsort.skip=true
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/extensible-store --retain-logs -- -Dtest=ExtensibleStoreIsolationLevelTest,ExtensibleStoreConcurrencyTest,ExtensibleStoreConnectionTest -DskipITs -Dformatter.skip=true -Dimpsort.skip=true

The final scoped format/check used this exact absolute-path regex (six actual files match), with formatter.skip=false and impsort.skip=true:

    readDatasetFormatScope='/Users/havardottestad/Documents/Programming/rdf4j-small-things/core/sail/base/src/(main|test)/java/org/eclipse/rdf4j/sail/base/(SailSourceBranch|SailSourceConnection|SnapshotSailStore|SnapshotSailStoreTest|SourceClosingSailDataset|SourceClosingSailDatasetTest)\.java'
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/base "-DspotlessFiles=$readDatasetFormatScope" -Dformatter.skip=false -Dimpsort.skip=true process-resources
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/base "-DspotlessFiles=$readDatasetFormatScope" -Dformatter.skip=false -Dimpsort.skip=true spotless:check

Before final formatting check source headers and Codex signatures. Format only touched Java sources and audit git diff for unrelated changes. Preserve all logs, reports, and existing untracked artifacts. Use the CommandLineTools PATH workaround only if formatting actually hangs in JGit discovery.

## Validation and Acceptance


The renamed serializable-default regression must fail before production changes and pass afterward. Public failure regressions must establish that original exceptions are preserved, acquired datasets and sources close exactly once, and a later root write can flush after a failed read. Ordinary source repeated SNAPSHOT/SERIALIZABLE datasets must preserve their first state; separate implicit read operations may see later writes. Decorated and selected nested branches retain normal observation behavior. Active SERIALIZABLE owners must still detect genuine conflicting writes and commit after unrelated implicit reads. All selected concrete suites must pass or have precisely documented reproduced baseline/environment failures; a partial green result is not a full green gate.

## Idempotence and Recovery


Do not clean, reset, restore, stash, broadly stage, or delete existing files. Maven-generated reports may be overwritten only after preserving evidence. If a test fails, inspect its report, reproduce its narrow selection, and fix the cause without weakening assertions or expanding timeouts. Update this plan before any change of direction. Production behavior changes remain gated by observed in-repository red tests. Commit and push remain unauthorized.

## Artifacts and Notes


Initial red command (production still b516d6d942):

    python3 .codex/skills/mvnf/scripts/mvnf.py 'SnapshotSailStoreTest#serializableDefaultImplicitReadsDoNotObserve+implicitReadClosesOwnedForkWhenDatasetAcquisitionFails+implicitReadClosesDatasetAndForkWhenStatementIteratorCreationFails+implicitReadReleasesRootAfterStatementIteratorCreationFails' --retain-logs -- -Dformatter.skip=true -Dimpsort.skip=true
    Tests run: 4, Failures: 4, Errors: 0, Skipped: 0, Time elapsed: 0.321 s

The serializable-default case expected zero observations and found four. The acquisition and iterator failures left their owned resources open. The failed root read kept a root observer alive, so a subsequent writer expected one backing approval and found zero. Evidence is persisted in repository-root initial-evidence.txt; previous evidence, report/XML, and full logs are preserved in `/private/tmp/rdf4j-gh6124-redgate-b516d6d942.rvFu9c/`. Verify log: `logs/mvnf/20261006-183853-verify.log`. The supplied review is at `/Users/havardottestad/.codex/attachments/4516d841-3ac8-4d6f-8450-7e967dfa5465/Pasted text.txt`.

## Outcomes & Retrospective


The four initial regressions failed against unchanged production, with zero test errors. After dataset acquisition and ownership implementation, the identical selection passed four tests with zero failures or errors in 0.316 seconds. Green artifacts are preserved at `/private/tmp/rdf4j-gh6124-green.09n94U/`; verify log is `logs/mvnf/20261006-184337-verify.log`. The additional root-close regression failed before its registration-release fix. The expanded focused selection passed 25 tests (12 Snapshot and 13 ownership wrapper), and the complete classes passed 206 tests (193 Snapshot and 13 wrapper), with zero failures, errors, or skips. Their preserved artifacts are `/private/tmp/rdf4j-gh6124-expanded.ynh8Xx/` and `/private/tmp/rdf4j-gh6124-expanded.bW48ze/`. The reviewer found no introduced correctness issue and its requested direct query-exception coverage passed. Full post-format base passed 220 tests, and targeted JaCoCo proved retained SERIALIZABLE sink/failure cleanup coverage. Memory, Native, LMDB, and Extensible concrete groups are green, with separately recorded optimistic-suite testcase counts and the existing one skipped concurrency case per group. The named LMDB forced-overflow lifecycle integration selection passed all four parameterized cases. All requested gates are complete and green, with zero failures/errors and the existing testGetContextIDs concurrency skip in each concrete store group. Scoped six-file formatting/check and final diff audit passed. Parent and independent source reviews found no introduced correctness issue. The original red initial-evidence.txt remains intact; no branch, commit, push, or staging action occurred.

Revision 2026-10-06: created the self-contained execution specification from the approved dataset-level design and current baseline.

Revision 2026-10-06: recorded the completed four-test red gate and advanced the sole active milestone to production implementation.

Revision 2026-10-06: recorded matching focused green and began lifecycle coverage, including a new failing-first gate for root registration cleanup when backing close fails.

Revision 2026-10-06: recorded the independent close-failure red gate and included root registration release on delegated-close failure within the lifecycle milestone.

Revision 2026-10-06: completed the independent lifecycle/compatibility coverage implementation, including all-default real owner conflicts, and documented exact focused class acceptance. Test scaffolding compilation diagnostics were fixed without changing assertions; their logs are preserved at `/private/tmp/rdf4j-gh6124-expanded.4rKlVF/`.

Revision 2026-10-06: recorded focused 25/25 and complete-class 206/206 acceptance; advanced the sole active milestone to base/concrete-store/JaCoCo/LMDB validation and dispatched an independent read-only review. Scoped formatting is performed before these final gates to avoid redundant broad verification after whitespace-only edits.

Revision 2026-10-06: recorded independent review with no correctness finding and temporarily advanced the sole active milestone to its requested direct query exception coverage; the runner pauses after its current gate to preserve edit/build serialization.

Revision 2026-10-06: added the dedicated compiled-query-step failure regression and optional EvaluationStrategy fixture injection. It asserts the QueryEvaluationException remains the SailException cause and both cleanup failures remain suppressed, then restores the sole active validation milestone. Production is unchanged from independent review.

Revision 2026-10-06: recorded passing direct query exception evidence and requested explicit six-file formatter hit proof before final formatting acceptance.

Revision 2026-10-06: recorded actual six-file formatter hit proof and the pre-format 220-test base acceptance. The exact absolute regex resolves the false no-op check without formatting unrelated files; final base verification follows the actual formatting pass.

Revision 2026-10-06: recorded post-format 220/220 base acceptance; targeted JaCoCo and concrete store/LMDB lifecycle validation remain the sole active milestone.

Revision 2026-10-06: recorded targeted 1/1 SERIALIZABLE commit acceptance and JaCoCo hit proof for retained sink selection and flush failure cleanup; concrete store and LMDB lifecycle gates remain.

Revision 2026-10-06: recorded passing Memory concrete acceptance and its optimistic-suite report-count inconsistency; subsequent suite evidence distinguishes declared counts from testcase-element counts while keeping test tooling unchanged.

Revision 2026-10-06: recorded passing Native concrete acceptance and updated outcomes to reflect completed independent review, base/coverage gates, and remaining real-store validation.

Revision 2026-10-06: recorded passing LMDB concrete acceptance and both required owner-commit method executions; Extensible and the LMDB lifecycle integration gate remain.

Revision 2026-10-06: recorded Extensible acceptance and updated outcomes to all requested concrete unit groups green; the named LMDB lifecycle integration test remains.

Revision 2026-10-06: recorded four passing forced-overflow lifecycle cases, completed behavioral validation, and advanced the sole active milestone to final diff/workspace/evidence audit.

Revision 2026-10-06: completed all behavioral/format/review/audit gates and marked every milestone done. Final literal report summaries confirmed that isolation classes run 10 tests and concurrency classes run 12 (including one skip), correcting earlier worker shorthand labels without changing aggregate counts. Reproducible final evidence is consolidated at `/private/tmp/rdf4j-gh6124-validation.2U64HT/manifest.txt`, `phase-report-summary.txt`, and `compact-evidence.txt`.

Final audit: HEAD remains b516d6d942f8768bb8b39eada036373d589e6964 on GH-6124-implicit-default-read-tracking. The final status path list matches the checkpoint; only six task Java files and this ExecPlan are changed/added within scope. The unrelated AD dependency-reduced POM remains AD with staged blob f24f6d981105d6c5b2f1e79883ad0c3991d6dca7, and `.codex/config.toml` and `papers2.zip` remain untracked. No initial hashes were recorded for those two unrelated files, so preservation is supported by untouched paths and scope audit rather than an invented byte-identity comparison. Original initial-evidence.txt is byte-identical to its pre-validation copy. No active mvnf marker or index lock remains. A worker's optional git write-tree audit was denied by read-only .git permissions and left no changes; read-only status and staged-blob checks completed the audit. No unresolved behavioral acceptance blocker remains. Existing decorated-source observation behavior and preexisting non-Snapshot/shared-union exceptional-path limitations stay within their documented boundaries; no benchmark or speed claim is made.
