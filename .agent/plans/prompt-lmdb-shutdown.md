# Cancel active work before draining LMDB shutdown

This ExecPlan is a living document maintained according to root `PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current. This task supersedes the startup/CI repair objective; `.agent/plans/bounded-lmdb-startup.md` retains that task's implementation and evidence. Existing GitHub monitoring may continue passively, but must not delay shutdown work.

## Purpose / Big Picture

Calling `SailRepository.shutDown()` on a busy LMDB repository should immediately reject new operations, cancel active uncommitted work, and close promptly once admitted native calls have exited. The user specifically requires concurrent uploads, statement iteration, size requests, and transactions with native execution and direct adjacency independently enabled or disabled. Every committed statement, namespace, identifier, and shared or nested value dependency must survive shutdown and reopen. Uncommitted changes must not become committed merely because the repository closes.

Cancellation is cooperative: code requests interruption and closes owned resources that support cancellation. It must never close an LMDB environment while a Java/native call can still use it. Work that has crossed an irreversible native commit boundary must finish reconciliation and preserve its committed result. Tests will distinguish cancellation before that boundary from shutdown overlapping an already committed transaction.

The user additionally requires a non-volatile shutdown field that all workloads periodically poll. Access it through a VarHandle with opaque or acquire reads and release publication; plain unsynchronized reads can be hoisted outside a loop. Capture a lifecycle cancellation signal once per workload and never reset an old task's signal during reinitialization. Thread interruption complements polling and unblocks waits. CPU-bound regressions must prove cancellation without relying on interrupt delivery.

## Progress

- [done] (2026-10-24) Finish requested formatting and publication check.
- [done] (2026-10-24) Audit shutdown and upload ownership paths.
- [done] Add deterministic size cancellation regression.
- [done] Add statement-iteration cancellation regression.
- [done] Capture closed-iteration compatibility regression.
- [done] Repair closed-iteration shutdown semantics.
- [done] Reproduce upload cancellation red.
- [done] Reproduce admission during drain.
- [done] Reproduce concurrent-close ordering.
- [done] Reproduce real socket upload red.
- [done] Repair admission and close completion.
- [done] Verify cancellation and iterator regressions.
- [done] Inspect exact-head shutdown CI failures.
- [done] Verify shared signal CPU cancellation.
- [done] (2026-10-25) Integrate external operation resource cancellation.
- [in_progress] Measure per-row result admission overhead.
- [done] Test cancellation signal ownership semantics.
- [done] Reproduce deep and nested signal gaps.
- [done] Verify composed cancellation signal lifetimes.
- [done] Publish verified CI fixture checkpoint.
- [done] (2026-10-24) Repair transferred result creation lineage.
- [done] (2026-10-24) Drain concurrent delegate close before teardown.
- [done] (2026-10-24) Close parked results during Sail shutdown.
- [todo] Reproduce PostReady null write-transaction crash.
- [todo] Capture unresolved hosted deferred timeout.
- [todo] Expand captured signal workload coverage.
- [todo] Verify four configuration concurrency matrix.
- [todo] Validate commit boundaries and durable reopen.
- [todo] Format verify and publish scoped results.

## Surprises & Discoveries

The original shutdown waited its connection grace period, interrupted each connection's creator, and joined it for up to one second. The active task can run on a different thread. Real size and transferred-statement-pull regressions failed before the first patch; both now pass with a monitor-protected registry of executing task scopes. LMDB opts into immediate broadcast while other Sails retain default grace. The entire seven-test shared API iteration-cleanup class also passes, including a new closed-result exception compatibility regression.

`NativeShutdownLockBarrierTest` independently proves transaction-manager, value-store, and triple-store shutdown must wait for an active native read interval. Its assertions remain unchanged. Prompt cancellation must reduce avoidable waiting without removing this safety barrier.

Resumed session (2026-10-24 23:30): the delegate-close regression compiles on current source. Fresh reds: the transferred unconsumed result pulled without error after its creator connection was cancelled, and shutdown neither interrupted nor drained a task inside a native delegate `close()` (`logs/prompt-shutdown/lineage-delegate-close-red/`). A new parked-result control showed LMDB already closes the parked native delegate, but the generic `SailBaseIteration` stayed open, shutdown logged `Connection closed before all iterations were closed`, and a later pull threw a raw `IllegalStateException` (`logs/prompt-shutdown/parked-result-red/`). Of the two retained `hs_err` crashes, the term-index maintenance copy crash predates `c2940902d8`, which moved that read under the transaction-manager barrier. The PostReady crash dereferences a zero native transaction in `removeRetiredIds -> resizeMap(writeTxn, ...) -> getNewSize`; that call shape still exists and needs a deterministic shutdown/value-GC reproduction before any repair.

Performance discovery (2026-10-25): the new `SailResultPullBenchmark` (MemoryStore, 1M rows, `core/sail/memory`) ran 222.4 ± 4.2 ms/op on the uncommitted tree. Run from the same jar with HEAD's four Sail API helper classes first on the classpath, it ran 43.3 ± 0.5 ms/op, a 5.1x regression for every Sail's public result pull. JFR attributed about half of all samples to `endOperation` monitor exit/notify, `CancellationSignal.isCancelled` and `ThreadLocalMap.set` (`logs/prompt-shutdown/pull-overhead-benchmark/`). The repository's benchmark jar also contained no JMH-generated classes on JDK 25 until `-Dmaven.compiler.proc=full` was passed. The script itself is unchanged.

## Decision Log

Decision: Finish the explicit formatter/commit/push request before changing shutdown. Rationale: the user requested this ordering. Copyright and exact root formatting passed with no byte changes, so no empty commit was created; normal push reported `Everything up-to-date`. Date/Author: 2026-10-24, Codex.

Decision: Use test-first Routine D for shutdown. Rationale: cancellation changes concurrency, admission, error propagation, and I/O lifetime. No behavior-changing production edit is permitted until the smallest real failing automated regression and report have been retained. Date/Author: 2026-10-24, Codex.

Decision: Cancel all affected connections before waiting for any one of them. Rationale: serial connection grace periods or creator-thread joins cannot provide prompt cancellation across many concurrent operations. The exact reusable lifecycle hook and scope will be selected after the smallest reproduction. Date/Author: 2026-10-24, Codex.

Decision: Preserve native ownership and irreversible commits. Rationale: the existing opaque `WriterTransaction` ticket identifies a logical writer independently of its native transaction pointer and survives checkpoints. Shutdown must use the actual operation's authority, never infer ownership from a global writer or thread. Date/Author: 2026-10-24, Codex.

Decision: Separate external upload registration from admitted Sail/native drain counts. Rationale: parser code blocked in arbitrary I/O owns no currently executing LMDB call. Once connection admission is terminally fenced, future callbacks fail; cancellation-only resource closure can unblock supported sockets without holding a registry lock or closing caller streams on normal completion. Actual admitted native calls and irreversible publications must still finish before teardown. Date/Author: 2026-10-24, Codex.

Decision: Use non-volatile backing state with explicit VarHandle access, then propagate a captured lifecycle signal to existing bounded loop/batch checkpoints. Rationale: explicit user constraint; safe polling must not become an optimized infinite loop, and old tasks must remain canceled after reinitialization. Add CPU-bound causal regressions before claiming this coverage complete. Date/Author: 2026-10-24, Codex.

Decision: Keep the signal independent of connection and compiled-plan ownership. Rationale: `CancellationSignal` in the common iterator module contains only a non-volatile boolean and optional parent signal. It uses release cancellation and opaque polling. A new Sail initialization gets a fresh root; connection/task signals retain their original parent, so old work cannot revive. Scopes bind a signal at runtime admission and loop code captures it once. Internal close runs with no application cancellation signal so acknowledged snapshot-store changes can still flush safely. Date/Author: 2026-10-24, Codex.

Decision: Results capture their creation lineage once, at registration, and every pull composes it with the pulling workload's signal through a per-result cached immutable pair. Result close registers as a cleanup operation that ignores application cancellation but is interrupted and drained by shutdown. Connection close drains twice, before and after closing results, and publishes the final drain under the operation monitor. Cleanup that arrives after that point waits for close completion rather than racing teardown. Rationale: transferred results must not outlive their creator's cancellation, and native teardown must never overlap an admitted delegate close. Waiting only after the final drain avoids a deadlock with `CleanerIteration`'s state monitor. Date/Author: 2026-10-24, Claude.

Decision: Close every registered result of a closing connection, not only abandoned ones, once that connection's Sail lifetime signal is cancelled. Ordinary close keeps its existing `reachableUnclosedIterationStillRejectsConnectionClose` semantics. Rationale: repository shutdown owns all of its results, and garbage-collection polling is neither prompt nor deterministic. Date/Author: 2026-10-24, Claude.

Decision: Uploads register an `AbstractSailConnection.ExternalWorkload` for the whole `add(InputStream|Reader|File|URL|Iterable|CloseableIteration)` call from `SailRepositoryConnection`. The registration unwraps `SailConnectionWrapper` to the base connection. It is outside the native drain count: shutdown interrupts the thread, and for InputStream/Reader uploads it closes the caller input once, outside the operation monitor. Normal completion never closes input. File/URL streams are opened inside `RDFLoader`, so those are interrupt-only. Rollback is cleanup and is admitted after cancellation. `AbstractRepositoryConnection` keeps the original upload failure and adds any rollback failure as suppressed. Rationale: the user explicitly chose "close on cancellation" (2026-10-25) over interrupt-only or socket-class detection. The interruptible-stream test now asserts that the caller input is closed on cancellation, and a new guard test asserts it stays open on normal completion. Date/Author: 2026-10-25, Claude.

Decision: Result pulls bypass the operation monitor and use no atomic updates. A pull publishes its thread in the result's volatile puller slot, then reads `isOpen` with volatile semantics. Close writes `isOpen` with volatile semantics and its drain waits for the operation counters and for every registered result's puller slot to clear. The connection's volatile `attention` flag is raised only while a drain waits or shutdown interrupts pullers under the monitor. A finishing pull takes the monitor only when that flag is set: to wake the drain, or to wait until an interrupt aimed at it has been delivered, so a reused thread is never interrupted afterwards. `CancellationSignal` keeps one owner-free mutable slot per thread instead of `ThreadLocal.set` on every swap. A first CAS-plus-LongAdder variant measured 125.6 ms/op against the 42.5 ms baseline; the stall was attributed to the atomics. Rationale: a multi-x per-row regression is not acceptable for an API every Sail shares. Missing an interrupt in the cancel-versus-entry race is tolerated, because the pull is still drained and polls the installed signal. Date/Author: 2026-10-25, Claude.

## Outcomes & Retrospective

The prior formatting task and read-only shutdown/upload audit are complete. After the first lifecycle patch and closed-iterator compatibility repair, the focused closed-iteration method passes, all seven `AbstractSailConnectionIterationCleanupTest` methods pass, and the five-method non-upload LMDB shutdown selection passes. Evidence is retained under `logs/prompt-shutdown/first-cancellation-green/` and `logs/prompt-shutdown/shared-signal-gate/`. The CPU polling method passes without interrupt delivery, and `CancellationSignalTest` passes its five ownership, monotonicity, fresh-root, and scope-restoration checks. Two new causal controls are retained red: a 50,000-signal parent chain overflows recursive cancellation polling, and an inner connection workload does not observe cancellation of its outer connection because the current thread scope replaces the outer signal. Evidence is retained under `logs/prompt-shutdown/deep-signal-red/` and `logs/prompt-shutdown/crossconnection-signal-red/`. Six API cleanup errors from Mockito's null-default `shutdownSignal()` were fixed in the test fixture by using a constructor-backed partial mock; the matching full API class passes 7/0/0/0, with original assertions unchanged. The latch-backed and real socket upload controls remain red under `logs/prompt-shutdown/upload-red/` and `logs/prompt-shutdown/socket-upload-red/`. Nested ambient propagation and read-only composed-view lifetime now pass their matching controls. The active step diagnoses and repairs the user-reported CI failures on an isolated published source baseline, while broader shutdown work remains unfinished. The final composed-signal gate passes all seven common iterator tests, six selected LMDB methods, and seven shared API cleanup tests. Retained evidence is under `logs/prompt-shutdown/composed-signal-final-gate/`. Creation-time lineage for transferred results and both upload controls remain red; no comprehensive workload polling or stress result is claimed.

Session 2026-10-24/25 outcome: the full `LmdbShutdownCancellationTest` class passes 12/0/0/0, including both former upload reds, the transferred-lineage and delegate-close reds, and the new parked-result regression. Its evidence is in `logs/prompt-shutdown/full-class-green-upload-close/`. Module suites on the pre-close-policy tree passed: core/sail/api 105, core/repository/api 63, core/repository/sail 54, core/sail/memory 1327 with 3 skips, under `logs/prompt-shutdown/module-suites/`. They must be re-run on the final tree, together with the LMDB module suite. Still open: workload polling coverage, the commit-boundary audit, auxiliary/background shutdown bounding, the PostReady null-transaction crash, the four-configuration matrix, the hosted deferred-close timeout and `LmdbServiceReplayIT`.

## Context and Orientation

Work from `/Users/havardottestad/Documents/Programming/rdf4j-merge` on branch `GH-0000-shacl-lexer-millisecond-deadlines`. The initial head is `34f9e29993a2d5e479b23a8e9879cfdc369b4e0e`. Preserve all existing commits and seven untracked artifacts. Do not switch branches, reset, stash, restore, clean, amend, force-push, or merge a pull request.

`core/sail/api/src/main/java/org/eclipse/rdf4j/sail/helpers/AbstractSail.java` owns Sail initialization, connection registration, and shutdown. `AbstractSailConnection.java` owns connection open state, operation accounting, transaction entry points, and final connection close. `core/repository/sail` adapts these to the public repository API; repository uploads parse an `InputStream` or `Reader` into statement-add calls. Inspect parser ownership and cancellation before adding any resource-close behavior.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java` owns the LMDB Sail and environment lifetime. `LmdbStoreConnection.java` tracks operation-scoped and transaction-scoped read attempts, including active threads and retained result iterations. `LmdbSailStore.java` coordinates application writers, background jobs, and growth. `ValueStore`, `TripleStore`, and `TxnManager` protect native readers and writers with barriers; their shutdown must still wait until admitted JNI calls exit. JNI is Java's native-call interface to LMDB.

The four requested configurations are the Cartesian product of native query execution enabled/disabled and adjacency enabled/disabled. Adjacency is an optional derived graph index; the authoritative LMDB data remains necessary in every configuration. Use real public repository operations in tests, with supported configuration setters and their current semantics.

## Plan of Work

First trace repository and Sail admission through shutdown, connection operations, parser uploads, tracked result iteration, native readers, asynchronous commits, and background maintenance. Identify existing cancellation controls and tests. Add the smallest deterministic regression that holds a real operation in an interruption-aware latch and starts shutdown on a separate thread. The test must show cancellation occurs without releasing the operation manually, and its failure cleanup must always release latches and join owned threads before closing native owners. Capture the actual red report before touching runtime behavior.

Implement the smallest reusable lifecycle protocol justified by that red. Mark shutdown admission closed before broadcasting cancellation. Snapshot active connections safely, request cancellation for all, then drain connections and native resources. A connection cancellation hook should request termination without prematurely releasing native read/write authority. Audit uploads whose parser may block outside Sail operation accounting; cancellation must reach the real executing thread/resource through the proper repository abstraction rather than a creator-thread guess. Preserve explicit reinitialization where the existing lifecycle supports it, and reject races that otherwise create a connection while shutdown owns the lifecycle.

Expand tests across all four configurations and concurrent operation types. Use explicit barriers around real execution and native commit seams. Seed committed rows, namespaces, shared datatype/namespace dependencies, and nested RDF-star terms; add distinct uncommitted data. On reopen assert exact committed contents and stable identifiers where the existing API guarantees them, absence of uncommitted data, and trustworthy native/count authority. Add repeated shutdown, cancellation cleanup failures, active readers, pending writer admission, partial transaction start, and commit-boundary controls only where the audit identifies relevant paths.

After focused greens, run related lifecycle, cancellation, replay, native barrier, upload, and repository tests. Then broaden to relevant Sail/repository modules and the LMDB module, retaining all terminal evidence. Run copyright auditing and the required root formatter in an exclusive source/Maven window. Any publication stages only reviewed shutdown files and this plan; no artifacts or unrelated WIP enter a commit.

## Concrete Steps

Run commands from the repository root. Maven and Java edits must be serialized. The mandatory initial root install already passed during the ongoing session; each `mvnf` run performs the required prerequisite install.

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs --tail 40
    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName --retain-logs --tail 40
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --tail 40

Use offline `.m2_repo`, no tests with `-am` or `-q`, and preserve scoped full logs/reports under `logs/prompt-shutdown/`. Append compact evidence to existing root `initial-evidence.txt`; never overwrite prior evidence. Report actual qualified Surefire/Failsafe filenames. Before final formatting:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Long commands use background execution and event-driven waits. The coordinator owns runtime/source decisions and serialized integration; healthy Luna owns complete routine verification/retention workflows. The parent reviews read-only and does not run duplicate Maven jobs.

## Validation and Acceptance

The smallest regression must fail before the repair because shutdown does not promptly signal the real active operation. After repair it must pass without test-thread release, sleeps, timeouts that conceal races, reflection/state mutation, or unsafe native closure. Bounded `await` assertions are failure guards around deterministic handshakes, not delay-based synchronization.

All four configurations must pass a concurrent matrix with uploads, `getStatements`, `size`, uncommitted writes and transactions. After shutdown starts, new work must fail promptly. All admitted cancellable operations must settle, shutdown must finish within a documented bounded test deadline, and reopened contents must equal the exact committed seed. An intentionally held native interval must keep environment close blocked until that interval exits, even after cancellation is requested. Tests around commit prove cancellation before irreversible commit and preservation/reconciliation afterward. Stress repeats validate the protocol rather than relaxing exact outcomes.

## Idempotence and Recovery

Tests create isolated temporary directories and release every owned latch/thread in `finally`, before native owners are closed. Preserve reports immediately because later Maven runs replace target reports. If a run hangs, capture thread dumps and ownership before terminating only an identified owned test process; never close a live environment from a timeout helper. If external source changes occur, stop and attribute them before combining evidence. No destructive cleanup is necessary.

## Artifacts and Notes

The completed formatting publication evidence is `logs/startup-ci/34f9e29993a2d5e479b23a8e9879cfdc369b4e0e/format-commit-push/post-push-equality-native.json`; all four native head values are equal and forty hexadecimal characters. New shutdown red/green artifacts will be added here as actual tests finish.

## Interfaces and Dependencies

Use the existing Sail/repository lifecycle abstractions, Java interruption, bounded resource ownership, existing native barriers, and opaque `WriterTransaction` authority. No dependency addition is planned. Lifecycle hooks must express request-cancellation separately from resource-drain/close so implementations can broadcast promptly while preserving JNI safety. Exact hook names and signatures will be recorded after the first causal regression and source review.

Revision note (2026-10-24): Created a separate shutdown plan after explicit user steering, recorded the completed formatter/no-op push, native-safety boundary, four-configuration acceptance, and first audit/reproduction milestone.

Revision note (2026-10-24): Recorded initial cancellation and closed-iteration red/green results plus four later causal reds. Added the user's non-volatile polling requirement and external-I/O/native-drain separation; next work repairs admission and concurrent-close completion before upload integration.

Revision note (2026-10-24): Admission/concurrent-close matching tests now pass with the shared API cleanup class and original cancellation pair. Retained actual socket and CPU-without-interrupt reds before new functional work; the first owner-free common signal and captured-read checkpoint integration are ready for verification. Exact-head CI artifacts preserve the Linux deferred-close timeout, reader-slot setup failure, and SERVICE growth premise. Only the explicitly authorized `predicateOnlyAccessIsCostedAsPrefixScan` sketch test was disabled; its class passes with one skip. No four-configuration or complete polling claim is made yet.

Revision note (2026-10-24): The CPU signal control passed. Added actual deep-chain StackOverflowError and cross-connection ambient-cancellation reds, then implemented iterative polling and cached stable signal composition with reusable nesting frames. A new cached-view mutation/isolation control is being run before enforcing read-only composition. Inactive thread-local entries retain no connection/store/thread owners, and empty current-signal entries are reused rather than allocated per iterator pull.

Revision note (2026-10-24): The composed signal gate is terminal (7 common iterator, 6 LMDB, 7 Sail API tests, no failures/errors/skips). Per latest user priority, pause broad shutdown expansion to diagnose the actual Linux deferred-close timeout and paired-reader-slot failure on isolated published HEAD34f. Added inert cached coordinator plus actual virtual-driver diagnostics and a smallest child-mode selector; no runtime repair is justified until causal local evidence. The original working tree must retain every later verified CI hunk as well as shutdown WIP; publish only an isolated-verified cached patch and prove exact tree equality.

Revision note (2026-10-24): Isolated Linux Temurin25.0.4.1 matches the hosted failing JVM patch. Deferred-close smallest1, wholeclass12, and declared fixed20 child stress all pass locally; the hosted OPENER_SIGNAL_CLEANUP deadlock remains unresolved, and inert actual virtual-driver diagnostics are not a fix. Paired-reader smallest1, fixed20, and wholeclass54 (including20 diagnostic repetitions) pass, all observed baselines ownerREADY/indexIDLE/no ordinary readers. The hosted exact127-versus126 failure remains authoritative. Its cleanup trace reaches the real PostReady unused-ID stat preflight, which uses an ordinary-preferred reader; a controlled actual-owner rendezvous is being added before the fixture completion boundary is corrected. Publication may include only isolated-verified fixture fixes plus inert hosted diagnostics, with every shutdown runtime hunk excluded and original working files carrying the same CI hunks to avoid inverse residual changes.

Revision note (2026-10-24): Published only six isolated-verified CI paths as5b3ab57a65e6c561d13c159927ca9790d7eeaca8; candidate/index/commit tree equality and full40 local/upstream/liveRemote/PR equality are retained under logs/prompt-shutdown/ci-fix-candidate.57 tests pass with one explicitly authorized sketch-method skip. Actual PostReady unused-stat reader control proves126→127 and the paired fixture now establishes that owner/layout boundary. Hosted Deferred timeout remains unresolved with inert virtual-driver diagnostics; no runtime repair claim. All17 outside-scope shutdown/unrelated artifacts remain intact. Resume retained result lineage (existing red) and a new safe real delegate-close interruption/drain control before expanding uploads and the four-configuration matrix.
