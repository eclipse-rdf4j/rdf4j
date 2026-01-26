# LMDB-Backed Isolation Up To Snapshot

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan must be maintained in accordance with `PLANS.md` at the repository root.

## Purpose / Big Picture

After this change, the LMDB Store will rely on LMDB's own transaction isolation for NONE, READ_COMMITTED, SNAPSHOT_READ, and SNAPSHOT. Snapshot and repeatable-read behavior will come from LMDB read transactions rather than the current in-memory SnapshotSailStore. Users will see the same externally observable isolation behavior (as proven by the existing isolation tests), but with simpler transaction mechanics and fewer in-memory overlays.

## Progress

- [x] (2026-01-02 09:45Z) Drafted ExecPlan and initial analysis.
- [x] (2026-01-02 13:30Z) Defined LMDB transaction context and APIs.
- [x] (2026-01-02 14:25Z) Implemented LMDB-backed SailSource/Sink/Dataset behavior.
- [x] (2026-01-02 14:25Z) Updated isolation level support and lock strategy.
- [x] (2026-01-02 13:59Z) Enforced thread-affine LMDB txn context use.
- [x] (2026-01-02 15:01Z) Ran LmdbStoreIsolationLevelTest and LmdbSailStoreTest.
- [x] (2026-01-02 15:18Z) Added commit read-lock + pinned ValueStore reads.
- [x] (2026-01-02 15:25Z) Added commit-window regression test and commit/read lock guard.
- [x] (2026-01-02 21:35Z) Added SNAPSHOT begin/commit regression test.
- [x] (2026-01-02 21:45Z) Guarded snapshot txn pinning with commit read lock.
- [x] (2026-01-03 11:39Z) Wired SNAPSHOT_READ pinning and deferred updates in LmdbTxnContext.
- [x] (2026-01-03 11:45Z) Aligned deprecated tracking with pending update buffering.
- [x] (2026-01-03 11:54Z) Treated SNAPSHOT reads as pinned + deferred to keep snapshotRead stable.
- [x] (2026-01-03 13:30Z) Added serializable conflict tracking + prepare checks for LMDB snapshots.
- [x] (2026-01-03 13:34Z) LmdbOptimisticIsolationTest green (mvnf core/sail/lmdb LmdbOptimisticIsolationTest).
- [ ] Run full LMDB module verify and isolation suite.

## Surprises & Discoveries

- Observation: LmdbStore wraps the backend with SnapshotSailStore and bypasses it only when isolation is disabled, so snapshot semantics are not coming from LMDB.
  Evidence:
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java
    this.store = new SnapshotSailStore(backingStore, ...)
    if (isIsolationDisabled()) { return backingStore.getExplicitSailSource(); }
- Observation: LmdbSailSource overrides fork() and does not use SailSourceBranch; prepare() on the source is a no-op unless we wire serializable conflict checks directly into LmdbSailSource/LmdbTxnContext.
  Evidence:
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java
    LmdbSailSource.fork() returns a new LmdbSailSource (bypassing SailSourceBranch)
    LmdbSailSource.prepare() now performs serializable conflict checks
- Observation: LmdbSailSink.flush() commits LMDB transactions immediately, which would prematurely commit changes if SnapshotSailStore were removed.
  Evidence:
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java
    tripleStore.commit(); ... valueStore.commit();
- Observation: Triple store writes can run on a background thread, so the write transaction is not on the caller thread; LMDB write transactions are thread-affine.
  Evidence:
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java
    tripleStoreExecutor.submit(() -> { tripleStore.startTransaction(); ... });
- Observation: SPARQL MODIFY updates must not see their own writes while iterating WHERE bindings.
  Evidence:
    testsuites/repository/src/main/java/org/eclipse/rdf4j/testsuite/repository/optimistic/DeleteInsertTest.java
    DeleteInsertTest initially failed until per-update changes were buffered.
- Observation: LMDB module verify appears to stall in OptimisticIsolationTest (IsolationLevelTest logging).
  Evidence:
    logs/mvnf/20260102-213127-verify.log (no completion after IsolationLevelTest stack trace).
    core/sail/lmdb/target/surefire-reports/2026-01-02T22-31-54_127-jvmRun1.dumpstream

## Decision Log

- Decision: Implement LMDB-native isolation for NONE, READ_COMMITTED, SNAPSHOT_READ, SNAPSHOT and stop using SnapshotSailStore for those levels.
  Rationale: The requirement is to rely solely on LMDB isolation up to SNAPSHOT, and the current SnapshotSailStore overlay prevents that.
  Date/Author: 2026-01-02 / Codex
- Decision: Implement SERIALIZABLE via optimistic conflict detection over LMDB snapshots.
  Rationale: Track observed statement patterns and compare pinned snapshot vs current state at prepare/commit; this preserves serializable semantics without a SnapshotSailStore overlay.
  Date/Author: 2026-01-03 / Codex
- Decision: Disable async triple-store write threading for transactions that need read-your-writes.
  Rationale: LMDB write transactions are thread-affine; read-your-writes requires reading from the same write transaction.
  Date/Author: 2026-01-02 / Codex
- Decision: Buffer per-update changes and merge on endUpdate to keep MODIFY WHERE evaluation stable.
  Rationale: SPARQL updates must not observe their own modifications while streaming bindings.
  Date/Author: 2026-01-02 / Codex
- Decision: Bind LMDB transaction contexts to the thread that opens pinned/read-write txns; reject cross-thread access.
  Rationale: LMDB read/write transactions are thread-affine, so snapshot or write transactions must stay on one thread.
  Date/Author: 2026-01-02 / Codex
- Decision: Block dataset creation during commit and pin ValueStore reads per dataset.
  Rationale: Avoid a window where triple-store commits become visible before the ValueStore commit, leading to unresolved IDs.
  Date/Author: 2026-01-02 / Codex
- Decision: Split SNAPSHOT_READ vs SNAPSHOT handling in LmdbTxnContext using explicit mode flags.
  Rationale: Defer writes for SNAPSHOT_READ while pinning read snapshots, and eagerly start write transactions for SNAPSHOT to keep snapshot boundaries deterministic.
  Date/Author: 2026-01-03 / Codex
- Decision: Track deprecated removals per pending update before merging into transaction-wide state.
  Rationale: Aborted updates must not leak deprecations into later change visibility or connection listener notifications.
  Date/Author: 2026-01-03 / Codex
- Decision: For SNAPSHOT, pin read snapshots and defer writes like SNAPSHOT_READ.
  Rationale: SnapshotRead tests require stable iterators; deferring writes avoids mid-iteration visibility while preserving read-your-writes via overlays.
  Date/Author: 2026-01-03 / Codex

## Outcomes & Retrospective

Implemented LMDB-backed transaction context, forkable sources, and snapshot handling without SnapshotSailStore. Added serializable conflict detection based on observed patterns against pinned snapshots, with prepare-time checks for LMDB-backed branches. Remaining work: run full LMDB module verify and isolation suite before final acceptance.

## Context and Orientation

The LMDB store implementation lives under core/sail/lmdb. LmdbStore is the Sail entry point. It currently wraps LmdbSailStore in SnapshotSailStore, which uses in-memory changesets to provide snapshot semantics. SailSourceConnection in core/sail/base uses SailSource.fork() to create transaction-scoped branches when isolation is not NONE, so the LMDB store must implement a forking SailSource if it is used directly. LmdbSailSink and LmdbSailDataset are the LMDB-specific write and read adapters. TxnManager and ValueStore manage LMDB read/write transactions.

Definitions used here:
- LMDB environment means the LMDB database handle opened by mdb_env_open (the value store and triple store each use separate environments).
- Snapshot isolation means repeatable reads within a transaction and no visibility of concurrent commits after the transaction begins.
- Pinned read transaction means a long-lived LMDB read transaction held open for the duration of a Sail transaction.

## Plan of Work

First, define an LMDB transaction context that is created when a Sail transaction begins and closed on commit or rollback. This context must own the LMDB read transaction used for repeatable reads (SNAPSHOT, SNAPSHOT_READ) and, when writes occur, the LMDB write transactions for the value store and triple store. It must also provide a consistent story for NONE and READ_COMMITTED: reads use short-lived read transactions unless a write transaction is active, in which case reads must use the write transaction so that read-your-writes works.

Next, make LmdbSailSource forkable and context-aware. A forked LmdbSailSource should bind to the transaction context so that datasets and sinks created from it use the same read/write transactions. LmdbSailSink.flush() must not commit for transaction-scoped sinks; commit should occur once per Sail transaction at SailSource.flush() or on connection commit. This removes the need for SnapshotSailStore to buffer changes in memory.

Then, update the LMDB transaction machinery. TxnManager must support pinned read transactions that are not reset on every commit, and ValueStore must allow a pinned read transaction for snapshot transactions instead of its current per-call renew/reset behavior. Ensure map-resize logic does not invalidate pinned snapshot transactions; if it must, detect and fail those transactions with a clear conflict error.

Finally, update LmdbStore to remove the SnapshotSailStore wrapper for SNAPSHOT and below, adjust supported isolation levels, and run the isolation and LMDB module tests to validate behavior.

## Concrete Steps

All commands are from the repository root.

1) Baseline build (required before tests):
    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

2) If adding or adjusting tests, run the smallest targeted test first:
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStoreIsolationLevelTest

   Expected tail excerpt contains "BUILD SUCCESS".

3) Run LMDB module verification after implementation:
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb

4) If module tests pass but isolation semantics are still in doubt, run the Sail isolation suite explicitly:
    python3 .codex/skills/mvnf/scripts/mvnf.py SailIsolationLevelTest

If any command fails because of missing offline artifacts, rerun the same command once without -o, then return to offline runs.

## Validation and Acceptance

Acceptance is met when:
- LmdbStoreIsolationLevelTest passes for NONE, READ_COMMITTED, SNAPSHOT_READ, and SNAPSHOT.
- Snapshot semantics are visible: repeated reads inside a transaction are stable; concurrent commits do not appear mid-transaction; read-your-writes works.
- The LMDB module test run core/sail/lmdb is green.
- LmdbStore no longer relies on SnapshotSailStore for the supported isolation levels, and LmdbSailSource.fork() is implemented.

If SERIALIZABLE is removed, tests should skip it by reporting it as unsupported.

## Idempotence and Recovery

All steps are repeatable. If a change causes isolation tests to fail, revert only the LMDB module changes, keep any new tests, and iterate. If map resize conflicts with pinned read transactions, return a clear SailConflictException and add a targeted test to document the behavior.

## Artifacts and Notes

Collect short snippets (no more than a few lines) from:
- core/sail/lmdb/target/surefire-reports/ showing passing isolation tests.
- Any new LMDB-specific tests added for snapshot or read-your-writes behavior.

Example (placeholder):
    Tests run: 12, Failures: 0, Errors: 0, Skipped: 0

## Interfaces and Dependencies

New or adjusted LMDB-facing APIs should live in core/sail/lmdb:

- New class org.eclipse.rdf4j.sail.lmdb.LmdbTxnContext
  Responsibilities: track isolation level, hold pinned read transactions, and manage write transaction lifecycle across TripleStore and ValueStore.
  Required methods (names can be adjusted as long as intent is preserved):
    - void begin(IsolationLevel level)
    - Txn acquireTripleReadTxn() (returns pinned or per-call based on level)
    - long acquireTripleWriteTxn() (write transaction handle on the caller thread)
    - ValueStore.ReadTxn acquireValueReadTxn() (pinned or per-call)
    - void markWriteStarted()
    - void commit()
    - void rollback()
    - void close()

- LmdbSailStore.LmdbSailSource
  Implement fork() and accept an optional LmdbTxnContext.
  dataset(IsolationLevel) must use context-provided read transactions.
  sink(IsolationLevel) must create sinks that write into context-managed LMDB write transactions.

- LmdbSailStore.LmdbSailSink
  Allow a transaction-scoped mode where flush() does not commit; commit is handled by the enclosing SailSource.flush() or connection commit.

- TxnManager
  Add a pinned read transaction path (not reset on commit) or a way to exclude active snapshot transactions from reset().

- ValueStore
  Add a pinned read transaction API (similar semantics to TxnManager) and a method to run reads against that pinned transaction.

- LmdbStore
  Remove the SnapshotSailStore wrapper for supported isolation levels and update setSupportedIsolationLevels(...) to stop claiming SERIALIZABLE if it is not implemented.

Each new or changed API should be documented inline with a short comment explaining how it supports LMDB-backed snapshot isolation.

## Plan Change Note

(2026-01-02) Created the initial ExecPlan document from repository analysis so implementation can proceed with a living plan.
(2026-01-03) Updated progress and decisions to reflect snapshot-read pinning and update deferral work in LmdbTxnContext.
(2026-01-03) Recorded deprecated-tracking alignment in progress and decision log.
(2026-01-03) Documented SNAPSHOT deferral decision to satisfy snapshotRead stability.
(2026-01-03) Updated serializable approach and prepare-time conflict checks after LmdbOptimisticIsolationTest coverage.
