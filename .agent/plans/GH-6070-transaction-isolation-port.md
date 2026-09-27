# Port LMDB Snapshot Publication and Reader Retirement

This is a living ExecPlan and must be maintained according to `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current. The work originated on `GH-6070-reproduce-pending-commits`, initially at `978c9a64cf4e7b61daede825b5f3e4400dc97b05`, and adapts relevant behavior from frozen source commit `165a1770e27c283165140a9831460b8170c3ae62`. The user authorized implementation, creation of `GH-6070-lmdb-native-snapshot-isolation`, and a commit named `GH-6070 Port LMDB native snapshot isolation`; do not push.

## Purpose / Big Picture

LMDB commits must reach the backing environments while older query results or snapshot readers remain open. A committed change must not silently move an existing snapshot to newer data, including when either LMDB map grows. The change will publish explicit and inferred roots together, retain values needed by older snapshots, and refresh cached readers when statements or namespaces advance. Users can observe this through the existing overlapping-reader reproduction and targeted snapshot, resize, namespace, quoted-triple, recovery, and NONE-transaction tests.

## Progress

- [x] Confirm source target and worktree evidence.
- [x] Capture baseline failures before production edits.
- [x] Implement shared snapshot and writer leases.
- [x] Verify LMDB publication and map invalidation.
- [x] Recheck lifetime recovery and ownership suites.
- [x] Fence same-owner sink generations.
- [x] Reproduce resize checkpoint leakage.
- [x] Guard partial dictionary checkpoints.
- [x] Rerun focused recovery selectors.
- [x] Reproduce importer checkpoint namespace read.
- [x] Add owner-scoped metadata lookup.
- [x] Verify importer and LMDB gates.
- [x] Run neighboring store and autogrow gates.
- [x] Format changes and record final outcomes.

## Surprises & Discoveries

- Observation: the target already has `SnapshotSailStore`, but its LMDB dataset reads namespace state directly from a mutable map and `getTriples` opens a separate ValueStore reader. Those views must join snapshot admission when roots are eagerly published.
  Evidence: target `LmdbStore.initializeInternal`, `LmdbSailStore.LmdbSailDataset.getNamespace/getNamespaces/getTriples`, and `ValueStore.getTripleTerms`.
- Observation: the source branch's map-growth behavior intentionally invalidates pinned snapshots. A pinned transaction must throw a retryable SailException after resize; cursor renewal must never silently advance it.
  Evidence: source `TxnManager.Txn.setActive/ensureSnapshotValid` and `LmdbSnapshotInvalidationTest`.
- Observation: TripleStore and ValueStore are separate LMDB environments and cannot commit atomically. Dictionary-first ordering may leave harmless orphan additions if the authoritative triple commit fails; retirement recovery must recheck liveness before deletion.
  Evidence: source commit ordering and `RetiredValueIdStore` recovery path.
- Observation: an early retirement recovery test called `ValueStore.gcIds()` directly on an ID still referenced by a committed triple. That bypasses the production retirement queue contract, so its failure is retained but does not confirm a live-ID recovery defect.
  Evidence: `LmdbValueRetirementRecoveryTest` report in `logs/mvnf/20260926-121904-verify.log` is explicitly labeled an invalid fixture; the real-path recovery suite passes in `logs/mvnf/20260926-200445-verify.log`.
- Observation: the base-layer red run independently reproduced observer-lifetime, snapshot-generation, and close-error cleanup defects.
  Evidence: `SailSourceBranchTest` ran 4 tests with 2 failures and 1 error; retained log `logs/mvnf/20260926-112752-verify.log`.
- Observation: constructing a SERIALIZABLE observation sink is a read path and must not reserve the native writer. The initial red showed both an independent writer blocked by a read-only SERIALIZABLE transaction and the same connection blocked when preparing after its observation sink was created; lazy writer ownership fixed both focused cases.
  Evidence: red/green Surefire summaries and retained logs in `initial-evidence.txt` (`20260926-140706`, `20260926-141158`, `20260926-141720`, `20260926-141937`).
- Observation: snapshot-backed lazy RDF values must remain usable after a caller closes the dataset or iterator, while retired IDs still need to wait for the oldest pinned reader. The current wrapper lifetime alone does not define either contract.
  Evidence: `LmdbSnapshotValueLifetimeTest` passes 6/6 and covers escaped values and reader-horizon behavior (`logs/mvnf/20260926-200403-verify.log` and `logs/mvnf/20260926-183451-verify.log`).
- Observation: the transaction that removes a statement holds its own pre-commit reader snapshot through publication, so its retirement watermark can remain ineligible until later. Empty transactions create no publication, and namespace-only commits must not write either LMDB environment even when retirement intents are eligible.
  Evidence: `LmdbSnapshotValueLifetimeTest#retirementWaitsForOldestPinnedReaderThenDrainsAfterClose` compares native LMDB transaction IDs across empty and eligible/ineligible namespace-only commits, then observes draining on an unrelated statement commit; Surefire log `logs/mvnf/20260926-183451-verify.log`.
- Observation: post-commit TRACE stats must be captured after eligible retirement maintenance, while namespace-only commits intentionally emit no native-write snapshots.
  Evidence: the five-level logging regression was red before the ordering/test expectation correction and now passes; logs `logs/mvnf/20260926-192754-verify.log` and `logs/mvnf/20260926-193631-verify.log`.
- Observation: a later inferred-root flush failure could still publish an earlier explicit-root commit because the outer publication scope was unaware of failures that occurred before entering the LMDB sink. The connection and union publication boundaries now fail the scope on exceptional unwind, while preserving the original throwable.
  Evidence: `LmdbStoreFlushReproductionTest#failedInferredRootFlushDoesNotPublishExplicitRoot` red/green reports in `logs/mvnf/20260926-195051-verify.log` and `logs/mvnf/20260926-195359-verify.log`.
- Observation: failure injection now uses package-private backing-store factories and real test subclasses instead of replacing final collaborators through reflection. The reader permit and environment inspections remain read-only diagnostics.
  Evidence: `LmdbSnapshotValueLifetimeTest` and `LmdbValueRetirementRecoveryTest` both pass after the seam conversion in `logs/mvnf/20260926-200403-verify.log` and `logs/mvnf/20260926-200445-verify.log`.
- Observation: cleanup paths can deadlock when they wait for a prepared writer lease. A read observer's auto-flush must skip publication when the LMDB owner lease or publication gate is unavailable, and branch close must release its observer/native resources without acquiring that writer lease.
  Evidence: `readObserverCloseDoesNotWaitForAnotherPreparedWriter` and `branchCloseDoesNotWaitForAnotherPreparedWriter` each failed against the blocking path and pass with the new nonblocking hook; red/green reports are preserved in `initial-evidence.txt`.
- Observation: the first LMDB module attempt stalled in `LmdbStoreConnection.rollbackInternal -> SailSourceConnection.rollbackInternal -> UnionSailSource.close -> SailSourceBranch.close -> beginPublicationScope -> acquireWriterLease`. This was a cleanup wait cycle, not slow test execution.
  Evidence: retained thread dump from process 93643 and module log `logs/mvnf/20260926-201215-verify.log`; the direct branch-close regression passes, and the final LMDB suite passes in `logs/mvnf/20260926-224906-verify.log`.
- Observation: the next LMDB suite exposed three independent regressions: inactive-reader version accounting advanced twice, ValueStore rollback retained cached IDs allocated by its aborted writer, and an async TripleStore operation failure left its transaction un-aborted so user rollback rethrew the operation error.
  Evidence: first full-module report `logs/mvnf/20260926-204635-verify.log`; the ValueStore diagnostic red showed the object ID resolved to null after the next commit (`20260926-211203`), while the async map-full red is `20260926-211452`.
- Observation: recovery requires both clearing mutable ValueStore caches and replacing its default revision when aborting a write transaction; clearing caches alone would not invalidate IDs already cached in `LmdbValue` objects.
  Evidence: `readCommittedRollbackDiscardsEagerEstimatorUpdates` now passes against the direct native read (`logs/mvnf/20260926-211814-verify.log`).
- Observation: after an async write operation fails, the worker now aborts its active TripleStore transaction and signals completion before rollback returns; queued later operations are discarded so they cannot leak into the next writer.
  Evidence: `LmdbStoreErrorHandlingTest#testMapFullError` red/green logs `20260926-211452` and `20260926-211616`.
- Observation: a sink from an earlier physical transaction could flush a later writer when both transactions reused the same connection owner. Closing an unflushed sink also left its physical transaction active, allowing the next same-owner sink to publish abandoned writes.
  Evidence: `olderSinkCannotFlushLaterWriterGenerationForSameOwner` and `closingUnflushedSinkRollsBackItsCurrentGeneration` both failed before the generation fence, then passed after flush, rollback, and close matched the physical transaction generation; reports are preserved in `initial-evidence.txt`.
- Observation: ValueStore auto-growth can durably checkpoint quoted terms before the corresponding TripleStore mutation is committed, so a direct reader can observe a term-only partial transaction.
  Evidence: `valueMapGrowthCheckpointDoesNotPublishUnflushedQuotedTerms` failed with the unflushed quoted term visible after map growth; report `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbStoreFlushReproductionTest.txt` and log `logs/mvnf/20260926-220226-verify.log`.
- Observation: a resize checkpoint gates new paired-view admissions until its physical writer generation commits or rolls back. A reader using the same logical NONE owner receives a retryable error even when handed to another thread; unrelated readers wait without holding native reader reservations. Rollback can leave dictionary terms that were written before the resize and therefore included in its durable checkpoint, but it does not publish their uncommitted TripleStore statements.
  Evidence: the cross-thread same-owner red/green is in `logs/mvnf/20260926-222220-verify.log` and `logs/mvnf/20260926-222441-verify.log`; commit and rollback readers plus the valid pre-checkpoint orphan case pass in `logs/mvnf/20260926-222741-verify.log`. The first orphan assertion was an invalid fixture because it expected the not-yet-allocated resize-triggering value to survive; that report is classified in `initial-evidence.txt` and `logs/mvnf/20260926-222517-verify.log`.
- Observation: a normal large Turtle import queries its namespace on the same active NONE connection after ValueStore growth. The owner-scoped metadata path now snapshots mutable namespace state under the store mutation lock without admitting or caching a mixed native read view; unrelated direct readers continue waiting for publication/rollback.
  Evidence: the minimal red is `logs/mvnf/20260926-223634-verify.log`; the focused green tests clear/remove/add through both metadata APIs and check another owner resumes on rollback in `logs/mvnf/20260926-224459-verify.log`; `QueryBenchmarkTest` passes 13/13 in `logs/mvnf/20260926-224716-verify.log`.

## Decision Log

- Decision: selectively adapt the source architecture; do not cherry-pick the large source branch or import adjacency, native query evaluation, optimizer, or asynchronous bulk-import changes.
  Rationale: isolation behavior is interwoven with unrelated source work, while the target already has the basic store and snapshot abstractions.
  Date/Author: 2026-09-26 / Codex.
- Decision: preserve the existing overlap reproduction assertions and capture their red result before production changes.
  Rationale: the HEAD test already states the required externally visible behavior.
  Date/Author: 2026-09-26 / Codex.
- Decision: a pinned dataset fails and asks the caller to retry after either native map grows; never renew a pinned view silently.
  Rationale: this is the user-approved contract and matches LMDB reset/renew semantics.
  Date/Author: 2026-09-26 / Codex.
- Decision: keep target evaluation strategy selection unchanged; add only a per-connection source view and owner/generation checks for NONE writers.
  Rationale: connection ownership is required to prevent one NONE transaction from flushing or rolling back another's writer, while source evaluator changes are outside scope.
  Date/Author: 2026-09-26 / Codex.
- Decision: bind LMDB `getTriples` to a caller-owned ValueStore read lease and native cursor, preserving existing LMDB dictionary-index enumeration rather than scanning all statements.
  Rationale: the current LMDB and source paths use the term index; scanning the statement store would be a separate, expensive cross-store semantic change.
  Date/Author: 2026-09-26 / Codex.
- Decision: admission scopes share publication and namespace generations across related explicit/inferred datasets, but reader permits are reserved before acquiring the publication gate.
  Rationale: paired views must not straddle commits, and waiting for a permit while holding the gate could block publication indefinitely.
  Date/Author: 2026-09-26 / Codex.
- Decision: opportunistic retirement reclamation runs only after an actual successful TripleStore transaction; namespace-only and empty commits must not cause physical writes to either native environment.
  Rationale: namespace-only publication is required to avoid LMDB commits, and the deleting connection retains its own reader snapshot through its commit. Durable retirement stays queued until a later real TripleStore commit or startup recovery can reclaim it safely.
  Date/Author: 2026-09-26 / Codex.
- Decision: a ValueStore map-growth checkpoint gates only fresh dataset admissions from the checkpoint until its physical writer generation commits or rolls back. A competing reader waits without holding native reader permits, while admission using the checkpointing writer's thread or logical owner returns a retryable error so that writer can finish.
  Rationale: LMDB may require a ValueStore write transaction to commit before map resize, but exposing its dictionary state before TripleStore publication breaks paired snapshot consistency. Ordinary reads remain unblocked until a real resize checkpoint occurs.
  Date/Author: 2026-09-27 / Codex.
- Decision: rollback does not claim cross-environment atomicity for a dictionary entry already included in LMDB's mandatory resize checkpoint; keep it as an orphan candidate and let existing liveness checks control later reclamation.
  Rationale: the ValueStore and TripleStore are separate environments, so once the dictionary checkpoint commits, rollback can abort the still-uncommitted TripleStore statements but cannot undo that earlier durable dictionary state.
  Date/Author: 2026-09-27 / Codex.
- Decision: automatic flush and branch cleanup must never wait for another owner’s prepared writer reservation. Add a default no-op `tryBeginPublication()` hook, propagate it through source wrappers, let LMDB return unavailable when the writer lease or publication gate is busy, and leave pending changes for an explicit flush. Branch close only retires its own snapshot and closes resources.
  Rationale: observer or branch close may be the action that releases reader capacity or lets the prepared writer finish; waiting for that writer from cleanup can prevent its release. Explicit flush retains its blocking publication semantics.
  Date/Author: 2026-09-26 / Codex.

## Outcomes & Retrospective

Baseline: root `-Pquick clean install` succeeded after the required online dependency resolution. `mvnf LmdbStoreFlushReproductionTest --no-offline --retain-logs` ran 4 tests with 2 failures: overlapping snapshot readers observed 0 instead of 8, and overlapping tuple results observed 64 instead of 72. The exact command, Surefire paths, failure summary, and retained log are in `initial-evidence.txt`. The base-layer focused red run also reported 2 failures and 1 error; its exact report evidence is retained there. These baseline failures were later addressed by the implementation below.

Recovery-test triage: `LmdbValueRetirementRecoveryTest` initially injected a still-live ID through direct `ValueStore.gcIds()` and failed to find the statement after reopen. This is not a valid regression against the approved durable retirement path, so that result is explicitly classified as an invalid fixture in `initial-evidence.txt`. The corrected durable-API test now passes: reopen preserves recursively quoted terms still referenced by committed triples and reclaims the dead orphan ID.

Focused ownership milestone: both SERIALIZABLE lease regressions now pass after removing constructor-time writer reservation and transaction-wide prepare reservation. The same-connection case includes an explicit `prepare()` before `commit()`. The base focused suite passes 19 tests, and the LMDB stable-currentness, older-transaction/cache-compatibility, prepared-writer, and retry-interruption selectors pass.

Reader-horizon milestone: the durable retirement queue remains populated while the older reader is open and across empty plus namespace-only commits. The test proves both native environment transaction IDs remain unchanged for eligible and ineligible namespace-only commits. An unrelated statement commit after the reader closes drains the queue; the real durable reopen/live-value recovery test also passes (1/0 in `logs/mvnf/20260926-182917-verify.log`).

Publication and resize milestone: the full `LmdbStoreFlushReproductionTest` initially passed all 15 cases, including unchanged overlapping-reader assertions, paired-generation/currentness checks, prepared writer ownership, deterministic ValueStore map growth, and paired-root failure handling. After adding nonblocking cleanup regressions, the integrated reproduction passes all 17 cases (`logs/mvnf/20260926-204541-verify.log`). The map-growth test writes through the native backing source, confirms the ValueStore map increased, observes the pinned iterator fail, and checks both environments return the old reader permits when the invalidated dataset closes. It also verifies that distinct NONE source owners cannot commit or roll back each other's pending writer. `ReadView.close` materializes escaped values while the TripleStore revision horizon remains pinned, then closes the TripleStore lease in a `finally` path.

Focused lifetime/recovery milestone: `LmdbSnapshotValueLifetimeTest` passes all 6 cases, covering escaped statement and quoted-term values, second-environment snapshot-start failure cleanup, paired reader saturation/recursive resolution, and retirement horizon/no-write behavior (`logs/mvnf/20260926-200403-verify.log`). `LmdbValueRetirementRecoveryTest` passes both durable recovery cases, including a dictionary commit followed by injected authoritative TripleStore failure and reopen (`logs/mvnf/20260926-200445-verify.log`). Both suites now inject failures through real collaborator subclasses created by package-private factories; they no longer mutate final fields with reflection. `LmdbStatsLoggingTest#logsWritesAcrossIsolationLevels` passes all five isolation values after aligning logging with completed maintenance and asserting namespace-only no-native-write behavior (`logs/mvnf/20260926-193631-verify.log`).

Final LMDB failure-triage milestone: the three regressions from `20260926-204635-verify.log` now have focused green evidence. Untracked reader activation returns to one version increment (`20260926-210532`); the direct sink rollback/recommit behavior is green after ValueStore rollback invalidates aborted IDs (`20260926-211814`); and async `MDB_MAP_FULL` rollback completes cleanly (`20260926-211616`). The simplified direct-store regression retains only public behavior assertions. Rerun integrated reproduction and complete module gates, then all neighboring-store, autogrow, and formatting checks.

Complete LMDB milestone: the earlier 17-case reproduction passed (`logs/mvnf/20260926-211919-verify.log`), and an earlier complete LMDB module run passed with Surefire 1,314 tests (3 skipped) plus Failsafe 103 tests (99 skipped), zero failures/errors (`logs/mvnf/20260926-211952-verify.log`). Later checkpoint, namespace, and importer fixes were retested by the final 23-case reproduction, a full LMDB module rerun, and the QueryBenchmark selector.

Base cleanup milestone: the full `core/sail/base` module passes 37/37 after updating lifecycle tests for the approved nonblocking auto-flush behavior (`logs/mvnf/20260926-213209-verify.log`).

Writer-generation milestone: same-owner sink operations now retain the physical transaction generation they joined. Flush and sink-local rollback skip a stale generation; closing an unflushed sink rolls back only the matching generation. Both focused regressions failed before the fix (the stale sink published two statements instead of one; closing an unflushed sink allowed two statements to commit) and now pass (`20260926-213539`, `20260926-214040`, `20260926-214156`, `20260926-214243`). The final integrated reproduction, full LMDB module, neighboring-store suites, and autogrow selector are recorded below.

Final outcome (2026-09-27): the port is implemented and validated. `LmdbStoreFlushReproductionTest` passes 23/23, `QueryBenchmarkTest` passes 13/13 after exposing same-connection namespace reads during a dictionary resize checkpoint, and full LMDB Surefire/Failsafe pass with totals 1,320 (3 skipped) and 103 (99 skipped). Base, MemoryStore, and NativeStore pass 37/37, 785 (3 skipped), and 738 (2 skipped); `TripleStoreAutoGrowTest` passes 9/9. No LMDB autogrow Failsafe `*IT` exists in the test tree, so no such selector was run. `process-resources`, the repository copyright scan, and `git diff --check` pass. The final post-format root `-Pquick clean install` passes in 27.223 seconds (`maven-build.log`). `initial-evidence.txt` retains the original red evidence, failure triage, and final outcomes; all Maven verify logs remain under `logs/mvnf/`.

## Context and Orientation

`core/sail/base` owns `SailDataset` snapshots and `SailSourceConnection` dataset admission. `SailSourceBranch` caches root datasets for observers, so it must know when a cached snapshot is no longer current and must keep it alive until the last observer releases it. `core/sail/lmdb` has two independent LMDB environments: `TripleStore` holds statements and contexts; `ValueStore` holds dictionary values and quoted triple terms. `LmdbStoreConnection` commits branches and owns the transaction lock. `LmdbSailStore` owns native writer lifetimes and flushes. `NamespaceStore` is a separate file-backed map and must be snapshotted independently. A pinned reader is an LMDB read transaction whose data generation cannot change under a caller; if map growth forces LMDB to reset it, the reader is invalid and must fail instead of renewing to newer data. A reader lease is the caller-owned lifetime token that keeps such a view open until datasets and iterators close.

The root reproduction is `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreFlushReproductionTest.java`. It already asserts that commits become visible in the backing store while other readers remain open. Add focused tests near LMDB snapshot/GC tests for namespace-only freshness, paired explicit/inferred admission, ValueStore-backed `getTriples`, map growth, NONE owner isolation, and restart recovery.

## Plan of Work

First run the repository-required root clean install and the existing reproduction, before modifying production code. Keep the first red Surefire report in `initial-evidence.txt` and preserve the complete Maven logs. Do not weaken the existing assertions.

Next adapt the base layer. Add `SailDataset.isSnapshotCurrent()` with a default of true and delegation through `DelegatingSailDataset`. Update `SailSourceBranch` to hand snapshots out through observer-counted leases and retire stale snapshots only after the last observer closes. Add a protected default no-op dataset-admission scope to `SailSourceConnection`; route all single and grouped dataset acquisitions through it, including union primary/additional acquisition and explicit/inferred pairs used by inferred statement operations. The LMDB override reserves capacity before taking its publication gate, opens all native views at one publication generation, then releases the gate as soon as admission completes.

Add pinned read leases to the shared `TxnManager` and use them for both native environments. `ValueStore` gains an internal caller-owned `ReadSnapshot` and an overload that scans its quoted-term index through that lease. The existing self-owned iterator API remains available. `LmdbSailDataset.getTriples` uses the dataset's lease and the native cursor, validates before reading and renewing a cursor, and fails after ValueStore map growth just as it does after TripleStore growth. Preserve the changeset overlay for read-own-writes. Close both leases on dataset close and every failed initialization path. For transaction isolation `SNAPSHOT` and `SERIALIZABLE`, the first admitted view must anchor a transaction-wide paired generation reused by later per-operation admissions; `SNAPSHOT_READ` remains query-scoped.

Snapshot namespaces as immutable map views captured with the native publication generation. Namespace-only commits increment a separate namespace generation used by `isSnapshotCurrent()`, including NONE commits that do not physically commit either LMDB environment. Add a connection-owned source view for NONE transactions. Its unique owner token flows through `LmdbSailSource`, `LmdbSailSink`, start, commit, and rollback so a connection waits for a different active writer and cannot publish or discard another owner's changes. Keep the existing evaluation strategy factory.

Serialize logical publication across writers, namespace changes, root flushes, and dataset admission. After `super.commitInternal()` succeeds, flush both explicit and inferred roots; empty changesets must not create native commits. For synchronous store writes, drain any existing queued operations, commit dictionary additions first, then commit TripleStore as the authoritative statement boundary. Perform retirement/GC only after authoritative triple commit. Do not import a broad asynchronous DML redesign. Add the source's additive `RetiredValueIdStore` named databases and raise ValueStore max DB count from 18 to 21. On reopen, and before every reclamation cascade, revalidate candidates against currently committed triples and normal quoted-term/context reference rules. If triple commit fails after dictionary commit, preserve orphan additions safely and never reclaim a value still referenced by live triples.

Finally, format touched files and run focused tests, the LMDB module suite, neighboring MemoryStore and NativeStore tests for shared base behavior, and LMDB autogrow coverage. Keep Maven invocations serial. Preserve red and green evidence in `initial-evidence.txt` and retain test logs.

## Concrete Steps

Working directory for all commands: `/Users/havardottestad/Documents/Programming/rdf4j-6`.

1. Run the required root install, preserving full output in `maven-build.log`:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

If the offline install fails only because a dependency or plugin is missing, retry that exact command once without `-o`, then return to offline mode. For another failure, retry without `-T 1C`. Do not run a test command with `-am` or `-q`.

2. Capture the pre-fix failure:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStoreFlushReproductionTest --retain-logs

Append the exact command, report path, and compact failing Surefire snippet to `initial-evidence.txt`; do not overwrite earlier content if the file exists.

3. During implementation, run single test selectors through `mvnf`; do not start another Maven process while one is active. Keep all successful verify logs using `--retain-logs`.

4. Final gates, after focused tests are green, include:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStoreFlushReproductionTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/base --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/memory --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/nativerdf --retain-logs

Run the LMDB `TripleStoreAutoGrowTest` selector and pinned-growth reproduction cases; repository inventory has no autogrow Failsafe `*IT` class. Run `mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources` formatting only after edits are complete. Use no concurrent Maven jobs.

## Validation and Acceptance

The existing overlapping-reader reproduction must fail before production changes and pass unchanged afterward. With open older readers, each successful commit must reach the backing LMDB counts; closing the final observer must not be required for publication. A stale cached snapshot must retire after a NONE commit, while an outstanding observer continues to see its original view and the old dataset closes only after its final borrower releases it.

Explicit and inferred snapshots admitted as one logical view must have one publication generation. A namespace-only commit changes old/new namespace visibility and freshness without creating a physical TripleStore or ValueStore commit. An already-open snapshot's `getNamespace` and `getNamespaces` remain unchanged after the commit; a newly admitted dataset sees it. Fresh dataset admission waits through a partial ValueStore growth checkpoint and resumes after its owner commits or rolls back; the owner itself gets an explicit retry error instead of self-deadlocking.

For `SNAPSHOT` and `SERIALIZABLE`, separate operations in one transaction must retain the same transaction generation: after an explicit-only read, a concurrent writer may update explicit, inferred, namespace, and term data, but a later inferred or metadata read must still see the established transaction view or fail explicitly if map growth invalidated it. Admission-capacity waits must happen before acquiring publication gates or branch semaphores needed by dataset close; direct branch and backing-source acquisition paths must remain able to make progress under saturation. Tests must use deterministic coordination rather than sleeps.

`getTriples` must enumerate through a caller-owned ValueStore native cursor at the same snapshot generation as TripleStore. Read-own-writes remain visible through the existing changeset overlay. A ValueStore map resize invalidates an open pinned iterator and produces a retryable SailException instead of cursor renewal. Reserved read slots must be returned after normal close, failure, and interruption. A `SNAPSHOT` transaction that reads explicit data, then races a committed explicit/inferred/namespace update, must keep one generation for later inferred and metadata reads. Reader-capacity waits occur before publication gates and branch semaphores; direct root branch acquisition must also have a non-blocking-under-lock progress path.

NONE transaction tests must show two connections cannot flush or roll back one another's writer. A failed authoritative triple commit may leave dictionary-only records but cannot delete live IDs. Reopening the store drains retirement intents only after current-triple liveness checks and recursive quote/context GC checks. The additive named databases open existing stores; no data encoding or value ID format changes.

Final acceptance result: the reproduction and focused suites pass; shared base regressions, the LMDB module, and existing autogrow coverage pass; formatting, repository copyright scan, diff check, and the post-format root quick install pass. The only inapplicable selector is an LMDB autogrow Failsafe `*IT`; no such test exists, and the native autogrow unit suite plus pinned-growth regressions pass.

Focused acceptance evidence includes `LmdbStoreFlushReproductionTest` (23/23), `LmdbSnapshotValueLifetimeTest` (6/6), `LmdbValueRetirementRecoveryTest` (2/2), `LmdbStatsLoggingTest#logsWritesAcrossIsolationLevels` (5/5), and `TripleStoreAutoGrowTest` (9/9). Each passed after a recorded failing reproduction where applicable.

Neighboring-store milestone: the full base module passes 37/37, MemoryStore passes 785 tests with 3 skipped, and NativeStore passes 738 tests with 2 skipped. Final retained logs are `20260926-225545-verify.log`, `20260926-225614-verify.log`, and `20260926-225657-verify.log`.

Revision note (2026-09-26 18:35Z): corrected the retirement maintenance contract after a native-transaction-ID regression showed that eligible namespace-only commits were writing ValueStore. Reclamation now runs only after an actual successful TripleStore transaction; the focused horizon selector proves no native writes for empty and namespace-only commits plus successful later draining. The durable reopen/live-ID recovery selector also passes. Remaining LMDB integration and broad validation gates stay in progress.

Revision note (2026-09-26 20:10Z): paired-root publication now marks the outer scope failed when a later root throws before native sink entry; the deterministic test proves the earlier explicit root is not committed. Both failure-injection suites now use internal collaborator factories rather than reflection mutation. Their focused runs, the 15-case LMDB reproduction, and existing lifetime/retirement acceptance suites are green. Complete base, LMDB module, neighboring-store, and autogrow integration gates remain in progress.

Revision note (2026-09-26 20:45Z): full LMDB verification exposed a teardown wait on another prepared writer. Added independent observer-close and branch-close regressions, captured each failure before its fix, and passed both after adding nonblocking LMDB publication acquisition for automatic flush and removing writer acquisition from branch cleanup. The focused reproduction now has 17 cases; rerun it and the complete LMDB module, then finish formatting and autogrow gates.

Final validation note (2026-09-27): importer namespace checkpoint regression passes (23-case reproduction and `QueryBenchmarkTest` 13/13). Full LMDB Surefire passes 1,320 tests with 3 skipped; Failsafe passes 103 with 99 skipped. The 9-test native `TripleStoreAutoGrowTest` passes; repository inventory contains no LMDB autogrow `*IT` selector. Shared base, MemoryStore, and NativeStore gates pass at 37/37, 785/3 skipped, and 738/2 skipped respectively. Final process-resources formatting, copyright scan, and diff audit pass; retained evidence is in `initial-evidence.txt` and `logs/mvnf/`.

## Idempotence and Recovery

The implementation is additive to the existing value database; it does not rewrite value IDs or TripleStore records. Named retirement databases are created on open and old stores begin with an empty retirement queue. Do not claim downgrade compatibility. If a test run fails, inspect the retained Surefire/Failsafe report and the corresponding `logs/mvnf` verify log before retrying. Do not delete, reset, or overwrite unrelated files or evidence. Keep the initial failing report even after later test runs.

## Artifacts and Notes

The root build log is `maven-build.log`; focused verify logs live in `logs/mvnf/`; compact first-red and final evidence live in `initial-evidence.txt`. All are repository artifacts and must remain available for review.

## Interfaces and Dependencies

Keep public SAIL APIs compatible. `SailDataset.isSnapshotCurrent()` is additive and defaults to true. The dataset admission scope is protected and defaults to no-op. ValueStore snapshot and reader-reservation APIs remain package-private/internal. `getTripleTerms(Txn, ...)` borrows, but never closes, its transaction; the existing `getTripleTerms(...)` continues to own its transaction. No new dependencies are required. In the original isolation-port scope, the only new persistent structure was the retirement queue in named LMDB databases; the later crash-durability repair also added native namespace and index manifests.

Plan update note (2026-09-27): marked the final gates complete after rerunning the LMDB suite, neighboring shared-base modules, autogrow coverage, formatting, repository audits, and final root quick install; documented the authorized branch/commit flow and that the test inventory has no LMDB autogrow Failsafe selector.

Documentation follow-up (2026-09-28): [lmdb-store-transactions.md](../../site/content/documentation/programming/lmdb-store-transactions.md) now records the user-visible tradeoffs against the pre-port target, separates unchanged isolation contracts from explicit resize invalidation, and links the snapshot, retirement, migration, and recovery tests. It describes the QEMU result as a simulated volatile-device power cut and makes no comparative performance or downgrade-compatibility claim.
