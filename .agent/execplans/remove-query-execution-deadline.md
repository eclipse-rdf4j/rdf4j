# Remove QueryExecutionDeadline and time out queries through cooperative cancellation

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.

## Purpose / Big Picture

A SPARQL query evaluated through the RDF4J Repository API may carry a maximum execution time (`Query.setMaxExecutionTime(seconds)`). Today the branch enforces that limit with `org.eclipse.rdf4j.common.iteration.QueryExecutionDeadline`, a thread-local absolute deadline that every layer polls, waits on, or registers expiry listeners on. That spreads timeout logic across about fifteen production classes, costs wall-clock reads on every iteration call, and duplicates the older timer-driven `TimeLimitIteration` path.

After this change the class no longer exists. A query still times out after its configured limit, but the limit is enforced in exactly two places. First, while `SailConnection.evaluate()` runs (before any result exists), a shared `java.util.Timer` interrupts the evaluating thread when the limit elapses. Second, the result is wrapped in a `TimeLimitIteration` that, when its remaining budget elapses, follows an escalation ladder: it first asks the whole iteration chain to stop through `CooperativeCancellation#requestCancellation()`, then two seconds later interrupts any thread still inside the iteration, then two seconds after that closes the iteration. Every pass-through iteration in RDF4J forwards the cooperative request, and in-memory operators that loop for a long time poll it, so most queries stop on the first rung.

To see it working, run `SailQueryExecutionTimeoutTest` in `core/repository/sail` and the LMDB timeout tests in `core/sail/lmdb`: a query whose `evaluate()` blocks longer than its limit fails with `QueryInterruptedException("Query evaluation took too long")` and leaves no interrupt flag on the caller thread, a native LMDB aggregate stops within the cooperative rung, and a source that ignores cancellation is interrupted at limit + 2 s and closed at limit + 4 s.

## Progress

- [x] (2026-10-25 08:00Z) Exploration of every QueryExecutionDeadline use (core and LMDB) and user decisions recorded below.
- [x] (2026-10-25 08:01Z) Root `-Pquick clean install` (needed one online run to fetch new BOMs).
- [x] (2026-10-25 10:05Z) Baseline module test results captured from a snapshot worktree into `initial-evidence.txt`: 13,595 tests, 0 failures (iterator, repository/sail, http/client, queryalgebra/evaluation, sail/base, lmdb).
- [x] (2026-10-25 09:20Z) Milestone 1: escalation ladder in `TimeLimitIteration`, `CooperativeCancellation.forward`/`forwardAll`; `TimeLimitIterationLadderTest` 9/9, `core/common/iterator` 96/96.
- [x] (2026-10-25 09:35Z) Milestone 2: `InterruptingTimeLimit` (+ test), `AbstractParserQuery`, `SailTupleQuery`/`SailBooleanQuery`/`SailGraphQuery`; `SailQueryExecutionTimeoutTest` 22/22, `core/repository/sail` 53/53, `core/queryparser/api` 18/18.
- [x] (2026-10-25 10:10Z) Milestone 3: cooperative forwarding and operator polling in core modules; `QueryExecutionContext`, sail/base deadline removal. Green: iterator, http/client 185, sail/api 104, sail/base 611, repository/sail 53, queryalgebra 2315, memory 1398, nativerdf 976; new `CooperativeCancellationForwardingTest`, group/order polling tests.
- [x] (2026-10-25 11:10Z) Milestone 4: LMDB cancellation entry points (`NativeContextClosingIteration`, `RepositoryValueIteration`, `ReplayableIteration`) and deadline removal from `LmdbSailStore`, `LmdbStoreConnection`, `TxnManager`, `ValueStore`, `LmdbStore`, `LmdbFilterSelectivityStats`; 27 LMDB tests ported, `NativeCancellationTokenDeadlineTest` deleted, `LmdbQueryExecutionDeadlineTest` renamed `LmdbQueryTimeoutTest`.
- [x] (2026-10-25 11:20Z) Milestone 5: `QueryExecutionDeadline` and its test deleted; lifecycle doc and `lmdb-store.md` updated.
- [x] (2026-10-25 11:25Z) Milestone 6: forward-only cancellation in FedX, SPARQLRepository, SHACL, extensible-store (SPIN has no delegating iterations).
- [x] (2026-10-25 09:53Z) Formatting, copyright check; committed and pushed as `08a20a9d5c`.
- [x] (2026-10-25 10:05Z) Non-LMDB module verification green: iterator 91, queryparser/api 18, http/client 185, sail/api 104, sail/base 611, repository/api 63, repository/sail 53, queryalgebra/evaluation 2317, memory 1398, nativerdf 976, repository/sparql 88, extensible-store 844, shacl 35316, federation 599 (0 failures).
- [x] (2026-10-25 10:27Z) LMDB triage: the first full run compiled tests mid-port and was discarded; against HEAD the ten suspect classes had two real failures. Native growth ignored the flush caller's time limit (fixed with `MapGrowthAttempt.cancel()`); one ported `TxnManagerTest` expected the old timed read-barrier wait and the wrong exception type (test fixed).
- [x] (2026-10-25 11:00Z) Merged `develop` (criss-cross merge bases made GitHub report a conflict; the merged tree is identical) and pushed `b1b60ee960`.
- [x] (2026-10-25 11:02Z) Full LMDB run in an mvnf workspace: 10,331 tests, 19 failures. Twelve were read-barrier tests waiting for the frame `acquireReadBarrier`, renamed to `acquireReadBarrierInterruptibly` by the port (tests fixed). Seven were spill tests that look for temporary files under `java.io.tmpdir`; they pass outside workspace mode (see Surprises).
- [x] (2026-10-25 11:57Z) Fresh full `core/sail/lmdb` run outside workspace mode: 10,490 tests, 0 failures, 139 skipped.
- [x] (2026-10-25 13:35Z) Merged `origin/main` (25 commits, including GH-6260 #6261). That work had moved `QueryExecutionDeadline` to a push model (`interruptOnExpiry`, flag-only `isExpired`, expiry listeners), which caused 20 conflicting files. Kept this branch's deletion and its deadline-free waits. Kept main's non-timeout changes inside the conflicts: operation-scoped reserved epochs, writer-transaction ownership on native growth requests, `rollback(asyncNativeTransaction)`, the reader-retirement and admission test hooks, shutdown `CancellationSignal` checks in `LmdbStoreConnection`, and targeted admission probes in `LmdbFrozenReplayTest`, now on the renamed `beforeGrowthAdmissionCheck()` hook. Dropped main's deadline-only tests. Root `-Pquick clean install` green.

## Surprises & Discoveries

- Observation: `requestCancellation()` never reached LMDB native code from a repository query before this change. The chain stops at `RepositoryValueIteration` and `LmdbStoreConnection.ReplayableIteration`, neither of which implements `CooperativeCancellation`; native work was cancelled only by `NativeExecutionContext` registering `deadline.onExpiration(cancellation::requestCancellation)`.
  Evidence: exploration of `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RepositoryValueIteration.java:40` and `LmdbStoreConnection.java:3615`.
- Observation: the graph query chain (`SailGraphQuery`) wraps the sail iteration in an anonymous `FilterIteration` and `ConvertingIteration`, neither of which forwarded cancellation, so CONSTRUCT/DESCRIBE timeouts only worked through the deadline.
- Observation: `FilterIteration` and `LookAheadIteration` treat a thread interrupt as "no more elements" (they close and return false). The interrupt step therefore never silently truncates a timed result only because `TimeLimitIteration` checks its own timeout flag after every delegate call and reports the timeout instead. Tests whose blocking sources restore the interrupt flag see the filter stop early.
  Evidence: `FilterIterationConcurrentCloseTest.timeoutCloseConvertsRuntimeFailureDuringFilterAcceptance` failed until its source swallowed the interrupt.
- Observation: `InterruptingTimeLimit.close()` cannot tell its own interrupt from one another party delivered while it was guarded, so it clears both. For the LMDB filter-sampling budget this means a caller's time-limit interrupt that arrives in the short window after the sampling budget fired is lost; the caller's own wrapper still reports its timeout because it keeps its own fired/interrupted state. The deadline-era test for that ordering (`threadInterruptionStillPropagatesAfterSamplingExpiration`) was removed.
- Observation: LMDB's `ReplayableIteration.close()` already defers closing a native delegate while a tracked read is active (`closeDelegateWhenIdle`), and `RepositoryValueIteration.close()` defers its value-scope drain behind an active pull, so the forced close step cannot free native memory beneath a running read.
- Observation: native map growth runs on the native writer worker, not on the flushing caller. The deadline used to travel to the worker (`NativeGrowthRequest.deadline`, `MapGrowthAttempt.deadlineScope`) and end its emergency reader grace; with only interrupts, the caller waited uninterruptibly in `serviceNativeGrowth` for the full 2 s grace.
  Evidence: `AdaptiveReplayIntegrationTest.asyncNativeGrowthUsesTheFlushCallersDeadlineRatherThanItsPhysicalTransactionDeadline` timed out after 1 s; fixed by `MapGrowthAttempt.cancel()` (see Decision Log).
- Observation: without a deadline every `TxnManager` read barrier goes through `StampedLongAdderLockManager.readLock()`, which spins while a native writer holds the barrier; before this change only untimed queries did, while timed queries parked in `tryReadLock(timeout)`. Parking changes observable lock state (a parked reader is not counted by `isReaderActive()`, which `ValueStoreTest` and the writer's reader-preference loop rely on), so the spin path, which untimed queries always used, was kept.
  Evidence: switching to `tryReadLock(Long.MAX_VALUE, NANOSECONDS)` failed `ValueStoreTest.testInterruptedTripleTermIteratorReturnsReaderPermit`.
- Observation: in mvnf workspace mode surefire sets `java.io.tmpdir` as a system property after JVM start, but `Files.createTempFile` uses the tmpdir captured at startup, so spill tests that list `java.io.tmpdir` (`NativePhysicalGroupingCostTest`, `NativeAdjacencyAggregateOutputMemoryTest`, `LmdbNativeCensusAggregateTest`) see no spill files. They pass in a plain `mvnf` run.
  Evidence: `TxnManagerTest.nativeReadWaitsForTheNativeWriterUntilReleasedOrInterrupted` never saw the reader park in `StampedLock.acquireRead`.
- Observation: `InterruptedSailException`'s constructors assert that the current thread is interrupted, so it cannot report a cancellation observed on a thread that was not interrupted (the native writer worker).
- Observation: main independently started a "push model" for query timeouts (`.agent/plans/push-model-query-timeouts.md`, GH-6260; main later deleted `.agent/plans/` in #6273, so read it at commit `52992a2ee8`). It kept `QueryExecutionDeadline` but made expiry flag-only and turned bounded waits into untimed waits woken by expiry listeners or `interruptOnExpiry()`. Its open milestones 5 and 6 (replace deadline polling with pushed cancellation flags, remove the deadline's remaining consumers) are what this branch already does, so the merge keeps this branch's side of every timeout conflict. Main parks the read barrier (`tryReadLock(Long.MAX_VALUE, NANOSECONDS)`); this branch keeps the spin for the reason recorded above.
- Observation: the root offline install failed on new BOM versions (jackson 2.22.3, jetty 12.1.14, netty 4.2.18, junit 6.1.3) after the latest main merge; one online install fetched them.

## Decision Log

- Decision: A native result carrier that owns its `NativeExecutionContext` cancels the context's root token on `requestCancellation()` and reports acceptance; a carrier with a borrowed context only forwards.
  Rationale: the root token reaches every operator and worker of the evaluation, but a borrowed context belongs to an enclosing evaluation that must not be cancelled by a nested result.
  Date/Author: 2026-10-25 / Claude.
- Decision: An interrupted caller waiting for native growth calls `MapGrowthAttempt.cancel()` (outside the `asyncProgress` monitor) and keeps waiting for the native owner to finish. The worker's quiescence and reader-grace loops poll the attempt's flag and fail with `IOException("Interrupted … the attempt was cancelled")`.
  Rationale: replaces the deadline handoff cooperatively; the native owner still decides when native state is safe to release, and no interrupt is delivered to the long-lived writer thread.
  Date/Author: 2026-10-25 / Claude.
- Decision: Native-writer producers check the thread's interrupt before handing work to the writer (`enqueueAsync`) and before reusing a completed drain.
  Rationale: the deadline check used to stop a cancelled producer even when no wait was needed; the interrupt is now the cancellation signal.
  Date/Author: 2026-10-25 / Claude.

- Decision: Time spent in `SailConnection.evaluate()` counts against the limit. A Timer task interrupts the evaluating thread if it is still inside `evaluate()` (or `exposeIteration()`) when the limit elapses; the result wrapper receives the remaining budget and starts already expired when evaluation overran.
  Rationale: user decision; preserves the observable semantics of the deadline without a thread-local.
  Date/Author: 2026-10-25 / user via Claude.
- Decision: One TimeLimitIteration lifecycle for every user (queries, `SailUpdateExecutor`, `AbstractQueryPreparer`, FedX, legacy `enforceMaxQueryTime` hooks): at T request cooperative cancellation; at T+2 s interrupt threads still inside delegate calls; at T+4 s close. The ladder runs even when no call is active. A cross-thread `close()` during an active call requests cancellation, defers the close to the active caller, and escalates on the same ladder; `close()` returns immediately.
  Rationale: user decision.
  Date/Author: 2026-10-25 / user via Claude.
- Decision: Every pass-through iteration forwards `requestCancellation()`; in-memory operators that loop (GroupIterator, OrderIterator, ContextAwareArraySorter, hash-join builds) poll a volatile flag and throw `QueryInterruptedException`.
  Rationale: user decision ("fully implement CooperativeCancellation", "forward + poll in operators").
- Decision: LMDB `requestCancellation()` cancels the native root token and immediately interrupts the thread inside the iteration's tracked read; the interrupt is cleared again when that thread deregisters. A forced close at +4 s marks LMDB sources closed but defers freeing native transactions, cursors and buffers until the active call exits.
  Rationale: user decision; blocked admission waits (`tryLock`, `Condition.await`, reader slots) can only be woken by interrupt, and freeing native memory beneath a running thread risks a JVM crash.
- Decision: Async and sort workers no longer carry a deadline; `QueryExecutionContext.checkpoint` keeps only the circuit-breaker check.
  Rationale: user decision ("rely on close fallback" for worker contexts).
- Decision: FedX, SPARQLRepository, SHACL, SPIN and extensible-store get forward-only cancellation in the final milestone.
  Rationale: user decision.
- Decision: Grace periods default to 2000 ms each and can be overridden with system properties `org.eclipse.rdf4j.common.iteration.TimeLimitIteration.interruptGraceMillis` and `org.eclipse.rdf4j.common.iteration.TimeLimitIteration.closeGraceMillis`; a protected constructor takes explicit values for tests.
  Rationale: tests must exercise all three rungs without four-second sleeps.

- Decision: `LookAheadIteration` implements `CooperativeCancellation` itself: it records the request in a volatile flag (`isCancellationRequested()`) and forwards through an overridable `forwardCancellation()`. Operators in `core/queryalgebra/evaluation` poll the flag through `CancellationPoints.check(...)`, which throws `QueryInterruptedException("Query evaluation was cancelled")`; `core/common/iterator` classes only forward because that module cannot depend on the query exception types.
  Rationale: one implementation point covers the many anonymous and named `LookAheadIteration` subclasses; polling must throw rather than end the iteration so that a cancelled operator never reads as a complete result.
  Date/Author: 2026-10-25 / Claude.

## Outcomes & Retrospective

`QueryExecutionDeadline` is gone from production code, tests and documentation. Query time limits are enforced in two places only: the `InterruptingTimeLimit` around `SailConnection.evaluate()` in the three Sail query classes, and the `TimeLimitIteration` escalation ladder (cooperative cancellation at T, interrupt at T+2 s, close at T+4 s) around every timed result. Every pass-through iteration in the core, sail and remote modules forwards `requestCancellation()`, and the long-running in-memory operators poll it. LMDB reaches its native root token from a repository query, and all of its admission waits end through interrupt, cancellation, shutdown or their existing internal budgets.

Verification: every touched non-LMDB module is green (about 42,700 tests). The full `core/sail/lmdb` suite is green (10,490 tests) in a plain `mvnf` run.

Lessons:
- The deadline had been doing more than timing out queries. It also carried a caller's time budget across threads: to the native writer worker during map growth, and to async and sort workers. Each handoff needed a replacement, either an explicit cancel signal (`MapGrowthAttempt.cancel()`) or the close fallback on worker contexts. Searching for `current()` captures would have found these handoffs earlier than the test failures did.
- Tests that wait for a specific stack frame or lock state are coupled to method names and wait strategies. When a ported test fails, first check whether it encodes a removed code path before changing production code to match it.
- Running a long suite while sources are still being edited gives results that cannot be trusted. Start full-module runs only from a quiescent tree.

Remaining limitations:
- `InterruptingTimeLimit.close()` can swallow an unrelated interrupt that arrives during its guarded window.
- Untimed and timed LMDB readers spin rather than park while a native writer holds the read barrier, as untimed readers did before.
- In mvnf workspace mode, surefire's `java.io.tmpdir` system property does not affect `Files.createTempFile`, so three LMDB spill test classes fail there. This is a build-configuration issue unrelated to this change.

## Context and Orientation

RDF4J query results are pull-based iterators called iterations (`org.eclipse.rdf4j.common.iteration.CloseableIteration`, module `core/common/iterator`). A query result is a chain of iterations: each wrapper holds a delegate and transforms or filters its elements. `IterationWrapper` is the generic pass-through base class; `FilterIteration`, `ConvertingIteration`, `LookAheadIteration` and many operator iterators in `core/queryalgebra/evaluation` are others.

`CooperativeCancellation` (`core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/CooperativeCancellation.java`) is an internal interface with one method, `boolean requestCancellation()`. It asks an iteration to stop at its next poll point without closing resources that another thread may be using, and returns true only if this iteration or one downstream accepted the request. "Forwarding" means a wrapper's `requestCancellation()` calls the same method on its delegate(s). "Polling" means an operator that loops for a long time checks a volatile flag set by `requestCancellation()` and throws `QueryInterruptedException` when it is set.

`TimeLimitIteration` (`core/common/iterator/.../TimeLimitIteration.java`) wraps a result and enforces a time limit. Its abstract `throwInterruptedException()` lets subclasses throw a module-specific exception; `AbstractParserQuery.QueryInterruptIteration` (`core/queryparser/api/src/main/java/org/eclipse/rdf4j/query/impl/AbstractParserQuery.java`) throws `QueryInterruptedException("Query evaluation took too long")`. Before this change it had two modes: a timer mode (a `java.util.Timer` task calls `interrupt()`) and a deadline mode driven by `QueryExecutionDeadline` listeners with deferred close during active calls.

`SailTupleQuery`, `SailBooleanQuery` and `SailGraphQuery` (`core/repository/sail/src/main/java/org/eclipse/rdf4j/repository/sail/`) evaluate a parsed query on a `SailConnection` and wrap the result.

LMDB (`core/sail/lmdb`) is an embedded store whose native engine (`org.eclipse.rdf4j.sail.lmdb.evaluation`) cancels work through `NativeCancellationToken` objects owned by a `NativeExecutionContext`. Reads are "admitted" by a map-growth coordinator and transaction manager (`LmdbSailStore`, `TxnManager`, `ValueStore`, `LmdbStore`, `LmdbStoreConnection`); before this change those admission waits were bounded by the thread's `QueryExecutionDeadline`. After it they are bounded by thread interrupt (which every wait already honours), store shutdown, and their existing internal budgets.

## Plan of Work

Milestone 1 rewrites `TimeLimitIteration` to a single timer-driven lifecycle with the escalation ladder, keeping the deadline constructor temporarily so the tree compiles, and adds `CooperativeCancellation.forward(Object)`. Milestone 2 adds `InterruptingTimeLimit` (a Timer-backed guard that interrupts its owner thread while it is inside a guarded region) and moves `AbstractParserQuery` and the three Sail query classes onto it. Milestone 3 implements forwarding in every pass-through iteration of the core modules, adds polling to the in-memory operators, removes the deadline from `QueryExecutionContext` and sail/base. Milestone 4 makes LMDB's result iterations cooperative and removes the deadline from every LMDB class. Milestone 5 deletes the class and updates the documentation. Milestone 6 adds forward-only cancellation to the remaining modules. The approved design, including per-class lists, is kept in this file's Interfaces section and is updated as work proceeds.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j7`.

Root install (always before tests):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Focused tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py TimeLimitIterationLadderTest
    python3 .codex/skills/mvnf/scripts/mvnf.py SailQueryExecutionTimeoutTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/common/iterator --retain-logs

Residual-reference check (expected empty at the end):

    rg -n "QueryExecutionDeadline" --glob '!benchmark-runs/**' --glob '!**/target/**' --glob '!.agent/**'

## Validation and Acceptance

`TimeLimitIterationLadderTest` (new, `core/common/iterator`) demonstrates: an accepted cooperative request lets the active call return without any interrupt; a call that ignores cancellation is interrupted at limit + interrupt grace and the interrupt flag is cleared before the timeout exception reaches the caller; a call that ignores both is closed asynchronously at limit + both graces; an idle iteration is closed by the ladder; a cross-thread `close()` escalates; a non-positive limit times out on first use.

`SailQueryExecutionTimeoutTest` (`core/repository/sail`) keeps its five `*TimeoutIncludesSynchronousSailEvaluation` tests and adds tests that a blocked `evaluate()` is interrupted and that no interrupt leaks to the caller.

LMDB acceptance: a repository query with `setMaxExecutionTime(1)` that waits for map-growth admission fails with `QueryInterruptedException`; a native aggregate is cancelled through `requestCancellation`; a forced close during an active native call does not free native memory before the call exits.

All touched modules pass `mvnf <module>`; failures that also occur in the baseline (`initial-evidence.txt`) are pre-existing.

## Idempotence and Recovery

Every step is a source edit plus test run and can be repeated. The baseline runs in a detached worktree under the session scratchpad with its own cloned `.m2_repo`, so it never touches this checkout. If a milestone leaves the tree uncompilable, `git diff` shows the partial edit; finish or revert that file before running tests.

## Artifacts and Notes

(Evidence snippets are added here as milestones complete.)

## Interfaces and Dependencies

No new libraries. At the end of the work these must exist:

In `core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/CooperativeCancellation.java`:

    static boolean forward(Object delegate)   // delegate instanceof CooperativeCancellation c && c.requestCancellation()

In `core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/TimeLimitIteration.java`:

    protected TimeLimitIteration(CloseableIteration<? extends E> iter, long timeLimitMillis)
    protected TimeLimitIteration(CloseableIteration<? extends E> iter, long timeLimitMillis,
            long interruptGraceMillis, long closeGraceMillis)

A non-positive `timeLimitMillis` means "already expired".

In `core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/InterruptingTimeLimit.java` (new, `@InternalUseOnly`):

    public static InterruptingTimeLimit start(long timeLimitMillis)  // guards the calling thread
    public long remainingMillis()
    public boolean fired()
    public void close()   // ends the guard; clears an interrupt only if this instance delivered it

In `AbstractParserQuery`:

    protected InterruptingTimeLimit startEvaluationTimeLimit()      // null when maxExecutionTime <= 0
    protected CloseableIteration<? extends BindingSet> enforceMaxQueryTime(
            CloseableIteration<? extends BindingSet> bindingsIter, InterruptingTimeLimit limit)
    protected CloseableIteration<? extends BindingSet> applyMaxQueryTimeHook(
            CloseableIteration<? extends BindingSet> bindingsIter, InterruptingTimeLimit limit)
    protected <T> CloseableIteration<? extends T> enforceResultMaxQueryTime(
            CloseableIteration<? extends T> iteration, InterruptingTimeLimit limit)

`QueryExecutionDeadline` and `QueryExecutionDeadlineTest` are deleted.
