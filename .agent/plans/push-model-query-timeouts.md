# Push query timeouts down instead of polling deadlines inside queries

This ExecPlan is a living document maintained according to `.agent/PLANS.md` at the repository root. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current as work proceeds. It builds on `.agent/plans/query-deadline-millis.md` (untracked; it migrated `QueryExecutionDeadline` to wall-clock milliseconds) and `.agent/plans/prompt-lmdb-shutdown.md` (checked in; prompt cancellation on Sail shutdown). Everything needed from them is restated here.

## Purpose / Big Picture

A query with a maximum execution time must stop promptly when its time is up. Today two mechanisms enforce that. The first is a wrapper iteration, `org.eclipse.rdf4j.common.iteration.TimeLimitIteration`, that wraps the query result and closes it when the time is up. The second is a thread-local deadline object, `org.eclipse.rdf4j.common.iteration.QueryExecutionDeadline`, that inner code consults: mostly the LMDB store, plus some Sail base classes. Inner code asks whether the deadline has expired, or how much time remains for a bounded wait. Before expiry each such question reads the wall clock, so every inner loop and wait is responsible for tracking elapsed time.

The user decided on 2026-10-25 that inner query code must not track time, and chose the full push model. A query timeout is enforced by the wrapper alone. When the time is up, the wrapper:

- calls `requestCancellation()` (interface `org.eclipse.rdf4j.common.iteration.CooperativeCancellation`), which every wrapper forwards down the iteration chain to the operators doing the work;
- wakes anything blocked on the query's behalf;
- closes the result.

An operator that accepts cancellation sets its own flag and stops at its next row or batch boundary. Afterwards, no inner code reads `System.currentTimeMillis()` or `System.nanoTime()` on behalf of a query timeout. Separate local budgets that are not the query timeout are out of scope and may keep their own clocks: LMDB growth-coordination grace periods, reader-admission back-off, and optimizer sampling and probe budgets.

This is distinct from Sail shutdown. Shutdown sets a non-volatile, never-reset flag (`org.eclipse.rdf4j.common.iteration.CancellationSignal`, read through a VarHandle with opaque loads) that work reads at its existing boundaries. Shutdown never needs to find running queries and call into them.

Observable outcome: a SPARQL query with `setMaxExecutionTime(1)` (seconds) against an LMDB store fails with `QueryInterruptedException` within the same bound as today, in all four native-evaluation × direct-adjacency configurations. That holds whether the work is CPU-bound in a native operator, blocked on writer admission, or between rows. And `rg 'remainingMillis\(|isExpired\(' core/sail core/queryalgebra core/http` finds no consumer that reads a clock for a query timeout.

## Progress

- [x] (2026-10-25) Inventory every QueryExecutionDeadline use by category (read-only sweep; summarised in Context and Orientation).
- [x] (2026-10-25) Characterize current timeout behavior (milestone 1). The existing suites serve as the net: core/common/iterator 86, SailSourceBranchDeadlineTest 1, SailQueryExecutionTimeoutTest 23, and an LMDB batch of 109 (`logs/push-timeouts/m1-baseline/`). The new LmdbQueryTimeoutPushTest cases are still to be added in milestone 3, alongside the push.
- [x] (2026-10-25) Make expiry flag-only (milestone 2). The red `QueryExecutionDeadlineTest#onlyTheScheduledExpiryPublishesExpirationNeverAClockRead` went green. Net: iterator 87, TxnManagerTest 44 (its pinned clock-read count updated to one clock read plus one flag check), SailQueryExecutionTimeoutTest 23, SailSourceBranchDeadlineTest 1, LMDB batch green.
- [x] (2026-10-25) Push cancellation from the expiring wrapper and forward it through LMDB results (milestone 3, commit e6eb7803e1). Reds: `TimeLimitIterationCancellationTest#deadlineExpiryPushesCancellationDownToTheActiveSource` and `LmdbCooperativeCancellationPropagationTest`. Greens: iterator 88, LMDB gate 42, SailQueryExecutionTimeoutTest 23. Not yet added: the LmdbQueryTimeoutPushTest end-to-end cases.
- [x] (2026-10-25) Replace bounded waits on remaining time with expiry-woken waits (milestone 4, commit 8513e88fb5). LMDB batch 294/0/0/0, iterator 93, SailQueryExecutionTimeoutTest 23, SailSourceBranchDeadlineTest 1. Two bugs found and fixed along the way: an early-expiry NPE that killed the shared timer, and head-of-line blocking among one deadline's listeners.
- [ ] Replace inner deadline polling on iteration paths with pushed cancellation flags; keep only non-iteration admission checks reading the flag (milestone 5).
- [ ] Remove propagation scopes without consumers; verify the four configurations and module suites (milestone 6).

## Surprises & Discoveries

`TimeLimitIteration` has two modes. Timer mode, built from a millisecond limit, already calls `requestCancellation()` on expiry (`TimeLimitIteration.interrupt()`, about line 441 onward). Deadline mode, built from a `QueryExecutionDeadline`, which the Sail query classes use, never calls it. On expiry it runs `requestCloseOnTimeout`, which closes the delegate on a virtual thread even while a call is active (`claimDelegateCloseIfIdle` returns true because `cooperativeTimeoutClose` is false in deadline mode). So the push primitive exists but is not wired to the path real queries take.

Expiry listeners registered through `QueryExecutionDeadline.onExpiration` only signal or notify; no expiry interrupts a thread. `InterruptedException` is never converted directly to `QueryInterruptedException`. Waits convert it to `InterruptedSailException`, `SailException` or `IOException`, and `TimeLimitIteration.checkInterrupted(RuntimeException)` later replaces such a failure with the timeout when the deadline has expired.

Propagation of `requestCancellation()` stops at `LmdbStoreConnection.ReplayableIteration` (extends `AbstractCloseableIteration`), `RepositoryValueIteration` (implements only `CloseableIteration`), and the common base iterations `FilterIteration`, `ConvertingIteration`, `LookAheadIteration`, `DelayedIteration` and `ThreadSafeDelayedIteration`. LMDB native operators already accept cancellation through `NativeCancellationToken` (a volatile flag also tripped by interruption of the evaluating thread), polled at row and batch boundaries.

Milestone 2 detail: `isExpired()` now returns only the expiry flag. The scheduled expiry sets the flag on the shared timer thread and runs listeners on a virtual thread, so a slow listener cannot delay other deadlines now that the timer is the only publisher. `remainingMillis()` still reads the clock for the category B waits until milestone 4. Category A `remainingMillis() <= 0` polls were switched to `isExpired()` in `TxnManager.checkQueryDeadline` and the post-acquisition read-barrier check, in `ValueStore.checkValueScopeDeadline` and `checkLookupAdmission`, and in `RepositoryValueIteration.checkActive`. An injectable `ExpiryScheduler` (package-private) lets tests fire expiry deterministically.

Milestone 4 discoveries: `StampedLongAdderLockManager.readLock()` spin-waits while a writer holds the barrier, whereas the timed path parks on the `StampedLock`. So the converted read barrier uses `tryReadLock(Long.MAX_VALUE, NANOSECONDS)`, a wrap-safe unbounded parked wait, under the expiry interrupt. `ValueStoreTest` hung in close because its Mockito-mocked deadline returned a null `ExpiryInterruption`: the NPE fired after reader admission and leaked a reader count. Both tests that mock a deadline now stub `interruptOnExpiry()`, and their pinned clock-read counts became "no `remainingMillis()` reads".

Head-of-line blocking among one deadline's listeners: running them sequentially on a single virtual thread let the writer-lease wake listener, blocked on `writerOwnerMonitor` held by a promotion, starve that deadline's other listeners. `LmdbWriterAuthorityTest#inlineDeadlineExpirationDoesNotAcquireWriterOwnerMonitor` exposed it. Each listener now runs on its own virtual thread, guarded by `QueryExecutionDeadlineTest#aBlockedListenerDoesNotDelayTheDeadlinesOtherListeners`. That test also assumed that once the wall clock passed the deadline, it was expired. Under the push model only the scheduled expiry publishes expiration, so the test now waits on an expiry listener, and it waits for post-ready maintenance before arming its promotion hook. It then passed 6 of 6 class runs.

## Decision Log

Decision: Use the existing `CooperativeCancellation.requestCancellation()` as the propagated cancel method rather than adding a second interface. Rationale: it is already optional (implemented only where useful), already forwarded by `IterationWrapper`, `CleanerIteration` and the LMDB native steps, and has the required semantics. Its contract returns true when this iteration or a downstream one accepted the request. Date/Author: 2026-10-25, Claude.

Decision: Full push model rather than a flag-backed deadline alone or a new-code-only rule. Rationale: explicit user choice on 2026-10-25. The flag-backed deadline (milestone 2) is kept as the first step because it removes every clock read at once without changing call sites. Date/Author: 2026-10-25, Claude.

Decision: Lock and semaphore waits that used remaining time (category B with `tryLock`/`tryAcquire`) become interruptible untimed waits under `QueryExecutionDeadline.interruptOnExpiry()`. The returned handle interrupts only the thread that armed it, and only while armed. Closing it disarms under the same monitor and reports whether it fired, so the caller can clear the interrupt it caused and report the timeout. Rationale: the wait needs a wake-up at expiry without reading a clock, and must never leak an interrupt into later work. Monitor and Condition waits instead use notify listeners registered outside the monitor; they are safe now that listeners never run inline under a publisher's lock. Date/Author: 2026-10-25, Claude.

## Outcomes & Retrospective

Nothing yet.

## Context and Orientation

Deadlines are created in exactly two places. `core/queryparser/api/src/main/java/org/eclipse/rdf4j/query/impl/AbstractParserQuery.java` method `startQueryExecutionDeadline()` calls `QueryExecutionDeadline.start(1000L * getMaxExecutionTime())`. Its callers are `SailTupleQuery.evaluate`, `SailGraphQuery.evaluate` and `SailBooleanQuery.evaluate` in `core/repository/sail`. Those classes enter the deadline as the thread-local current deadline around `sailCon.evaluate(...)`. They wrap the result in a `QueryInterruptIteration`, a `TimeLimitIteration` in deadline mode that owns the deadline, re-enters it around every delegate call and closes it in `handleDelegateClosed`. The second creation site is local: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbFilterSelectivityStats.java` around line 569 starts a shorter deadline for optimizer sampling and enters it in place of the caller's. Timer-mode `TimeLimitIteration` is still used by legacy `enforceMaxQueryTime` callers (`AbstractQueryPreparer`), `SailUpdateExecutor`, and the legacy `query/parser/impl/AbstractParserQuery`.

`QueryExecutionDeadline` (`core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/QueryExecutionDeadline.java`) stores an absolute epoch-millisecond deadline. It schedules one task per deadline on a shared daemon `java.util.Timer`; the task calls `expire()`. `expire()` sets an `expired` AtomicBoolean and runs the registered `onExpiration` listeners outside its lock. `remainingMillis()` returns 0 once expired, `Long.MAX_VALUE` once closed, and otherwise reads `System.currentTimeMillis()`. If that read shows the deadline has passed, it calls `expire()` inline. `isExpired()` calls `remainingMillis()` unless already expired or closed. `current()`, `enter(deadline)` (null keeps the current one), `enterScopedContext(deadline)` (null clears it) and `Scope.close()` manage the thread-local.

The inventory sorts inner uses into six categories. Category A is polling checks: `isExpired()` or `remainingMillis() <= 0` in a loop, per row or per admission. Category B is bounded blocking waits on remaining time. Category C is propagation only: `current()`, `enter`, capturing a deadline into an object or worker. Category D is creation or expiry listeners. Category E is budgets that take the minimum of the query's remaining time and a local budget. Category F is other.

Category B (bounded waits; each must become a wait woken by expiry):

- `LmdbSailStore.awaitChange` (`changed.awaitNanos`, about line 1269) via `MapGrowthCoordinator.remainingNanos` (E).
- `LmdbSailStore.requestQuiescence` replay-callback `get(timeout)` (about 2203).
- `awaitAsyncSignal` (5142, 5162), `beginPublicationScope` (6017), `acquireWriterLease` (6130, deliberately without a listener), and `awaitStablePublication` (6288).
- `LmdbStoreConnection.prepareForReadAttempt` (1952), `retirePendingReplayIfIdle` (2368, no listener), and `awaitResultReplayIfNeeded` (3018).
- `TxnManager.acquireReadBarrier` `tryReadLock` (811), `acquireReaderPermits` `tryAcquire` (1104, E), and `awaitReaderRelease` `awaitNanos` (1332, E).
- `ValueStore` `associationGate.tryLock` (4999).
- `LmdbStore` `txnLockManager.tryLock` (598) and `waitForActiveLocks` (639).
- `SailSourceBranch` `admission.completed.await` (1204).

Category A is concentrated in:

- `LmdbSailStore`: `throwIfExpired`, `checkAdmission` and `checkPublicationAdmission`, about 40 call sites, including per statement in `approveAll`. Also `ParallelSnapshotSource.checkOpen()`, with 29 call sites.
- `LmdbStoreConnection.throwIfDeadlineExpired`, 7 sites, one per tracked read.
- `TxnManager.checkQueryDeadline`, 22 sites.
- `ValueStore`: `checkValueScopeDeadline` and `checkLookupAdmission`.
- `LmdbStore.checkTransactionAdmissionDeadline`.
- `RepositoryValueIteration.checkActive`, per row.
- `QueryExecutionContext.checkpoint()`, every 1024th call.
- `TimeLimitIteration.checkInterrupted`, up to four times per call.
- `SailSourceBranch` and `SailSourceConnection` dataset-acquisition retries.

## Plan of Work

Milestone 1, characterization. Before changing behavior, find the existing timeout tests: `rg -l "setMaxExecutionTime|QueryInterruptedException" --glob '*Test*.java' core compliance testsuites`. Add the missing ones to a new `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryTimeoutPushTest.java`. Each case sets a one-second maximum execution time and asserts `QueryInterruptedException` within a bound and no leaked readers:

- a CPU-bound native aggregate;
- a query blocked on writer admission behind a held transaction;
- a query between rows (the consumer sleeps longer than the limit, then calls `hasNext`);
- a query whose result is transferred to another thread.

Run them against the unchanged code and keep the green evidence. They are the safety net for every later step.

Milestone 2, flag-only expiry. Change `QueryExecutionDeadline.isExpired()` to return only the `expired` flag and `remainingMillis()` to stop calling `expire()` inline. The timer task becomes the only publisher. Category A sites then read a flag and no clock without being edited. Add a unit test proving that `isExpired()` stays false until the timer fires, even after the wall-clock deadline, using an injected timer seam if needed, and becomes true once it fires. Re-run milestone 1.

Milestone 3, push from the wrapper. In deadline mode, make `TimeLimitIteration` expiry run the same cooperative path timer mode uses: call `requestCancellation()` downward, and defer delegate close while a call is active when cancellation was accepted. Forward `requestCancellation()` from `LmdbStoreConnection.ReplayableIteration` (to its current delegate) and `RepositoryValueIteration` (to its source). Add forwarding to the common base iterations listed in Surprises where they hold a delegate. Re-run milestone 1. The native-aggregate case must now stop through the native cancellation token rather than a deadline poll.

Milestone 4, waits. Each category B wait becomes an untimed wait on its condition that an expiry listener wakes. Listeners already exist for `awaitChange`, `awaitAsyncSignal`, publication waits and `prepareForReadAttempt`; add them where missing (`acquireWriterLease`, `retirePendingReplayIfIdle`, `TxnManager`, `ValueStore`, `LmdbStore`, `SailSourceBranch`). After waking, the waiter reads the expired flag. Category E waits keep their local budget's clock and drop the query component; expiry still wakes them through the listener. `tryLock`/`tryAcquire` with timeouts become interruptible acquisition plus a listener that interrupts only the thread registered as waiting for that acquisition. Do not use a blanket interrupt.

Milestone 5, polling on iteration paths. Where a category A check sits on a path driven by an iteration that now receives `requestCancellation()`, replace the deadline check with that iteration's own cancellation flag. Examples are per tracked read in `LmdbStoreConnection`, `RepositoryValueIteration.checkActive` and `QueryExecutionContext.checkpoint`. Checks at transaction or writer admission, which are not iteration-driven, keep reading the deadline's expired flag, which is now a flag read and not a clock read.

Milestone 6, cleanup and verification. Remove `remainingMillis()` consumers and any propagation scopes that no longer have readers. Run the LMDB module suite, `core/sail/base`, `core/repository/sail`, `core/queryparser/api` and `core/common/iterator`, plus the four-configuration timeout matrix. Measure the per-row cost with `SailResultPullBenchmark` and an LMDB query benchmark before and after.

## Concrete Steps

Run from the repository root. Tests use the workspace runner, never `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbQueryTimeoutPushTest --module core/sail/lmdb --retain-logs --tail 40
    python3 .codex/skills/mvnf/scripts/mvnf.py core/common/iterator --retain-logs --tail 40

Benchmarks need JMH annotation processing, which JDK 23+ disables by default. Build the jar with:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/memory -P benchmarks,quick -Dmaven.compiler.proc=full package
    java -cp core/sail/memory/target/jmh-benchmarks.jar org.openjdk.jmh.Main SailResultPullBenchmark.pullAllStatements -wi 5 -i 10 -w 2s -r 2s -f 1

## Validation and Acceptance

Milestone 1's tests pass before and after every milestone. A deterministic unit test shows that `isExpired()` changes only when the timer fires. With deadline-mode expiry, a CPU-bound native operator observes `requestCancellation()` (its token is set) rather than a deadline poll; assert this through the token or a test hook. No category B wait in the list above computes a timeout from `remainingMillis()`. The four LMDB configurations pass the timeout matrix.

## Idempotence and Recovery

Each milestone is a separate commit that can be reverted alone. Characterization tests must pass before and after every step. If a wait conversion deadlocks a test, revert that one wait and add a listener before retrying.

## Artifacts and Notes

Evidence goes under `logs/push-timeouts/`. The read-only inventory that seeded this plan was produced on 2026-10-25. Its category lists are reproduced in Context and Orientation, with line numbers valid at commit `385934e1df`.

## Interfaces and Dependencies

`CooperativeCancellation.requestCancellation()` returns true when this iteration or a downstream one accepted the request. `QueryExecutionDeadline` keeps `start`, `current`, `enter`, `enterScopedContext`, `onExpiration`, `close` and `isExpired`; `remainingMillis()` is removed from inner consumers by milestone 6. No new dependencies.

Revision note (2026-10-25): Created after the user directed that query timeouts be pushed through the wrapper with requestCancellation and chose the full push model. The same day the inventory was folded in and the milestones fixed.

Revision note (2026-10-25, GH-6205 merge): superseded on branch `GH-6205-lmdb-sparqloscope-native-execution`. That branch removes `QueryExecutionDeadline` entirely; see `.agent/execplans/remove-query-execution-deadline.md`. Query time limits are enforced by `InterruptingTimeLimit` around `SailConnection.evaluate()` and by the `TimeLimitIteration` ladder: cooperative cancellation, then interrupt after 2 s, then close after 4 s. That covers milestones 5 and 6 here. When main was merged into that branch, its deadline-based waits and tests were replaced by the branch's interrupt-driven versions.
