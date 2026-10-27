# Replace the LMDB direct-adjacency synchronisation with a single commit log and a single applier

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.


## Purpose / Big Picture

The LMDB store in `core/sail/lmdb` answers many statement lookups from the "direct adjacency index": an in-memory, compressed copy of the LMDB triple indexes. LMDB stays authoritative; the adjacency index is a cache that must give, for any reader pinned at a revision, exactly the rows LMDB holds at that revision, or decline so the reader falls back to LMDB. Keeping the two in step is today spread over three executors, two commit paths, three gap markers, a lock-free phase enum written from four threads, and nineteen interleaving hooks that tests drive by hand. Since late September 2026 roughly forty-five commits in this module have been "await", "stabilize", "synchronize" or "barrier" fixes, and CI still fails intermittently in `LmdbRealEstateNativeParityTest`, `LmdbDirectAdjacencyQueryTest`, `LmdbDirectAdjacencyRecoveryTest` and their neighbours.

After this plan is implemented, the synchronisation has exactly one shape. Every triple commit appends one record to an in-memory, revision-ordered commit log, on the committing thread, inside the same critical section that already makes the new revision visible. Exactly one thread, the applier, consumes that log in order and is the only thread that ever publishes a new adjacency state. Building, catching up, compacting, folding and rebuilding are tasks the applier performs between log records; they can never race a delta. A reader captures its snapshot revision, the current adjacency state and the log position in one lock-free step and decides locally whether that state serves its revision exactly. Tests stop steering interleavings through hooks: they wait on `awaitApplied(revision)` or drive the applier one step at a time through one injected scheduler. A user sees the same query results as before, with the module's adjacency tests passing without timing-dependent waits, and `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` green under load.


## Progress

- [x] (2026-10-27 09:30Z) Read the current implementation end to end (build, catch-up, cutover, commit listener, deferred commits, read acquisition, compaction, startup, initial import) and catalogued the stabilisation history and open CI failures. Findings are recorded in `Context and Orientation` and `Surprises & Discoveries` below.
- [x] (2026-10-27 12:25Z) Milestone 1: `AdjacencyCommitLog`, `AdjacencyState`, `AdjacencyApplier`, `AdjacencyIndexOps`, `AdjacencyCompactionPolicy`/`Plan`, `AdjacencyAcquisition`, `AdjacencyRetentionHold`, `AdjacencyGapException`, `AdjacencyDelta`, `ApplierScheduler` (+ `ThreadApplierScheduler`), `AdjacencyWorkerPool` (+ `ExecutorAdjacencyWorkerPool`) exist as new, unwired files; `AdjacencyApplierModelTest` with `SteppingApplierScheduler` and `DirectWorkerPool` passes (6 tests, 1.1 s; 89,664 interleavings exhaustively at depth 7 plus 1,500 random sequences of 200 operations). No existing file changed. Evidence in `initial-evidence.txt`.
- [x] (2026-10-27 13:30Z) Milestone 2: `TripleStore` owns an `AdjacencyCommitLog`, stages the seal in `prepareDerivedCommit`, confirms or appends a gap in `publishDerivedCommit` before `dataRevision.set`, retracts on a failed or abandoned commit; `LmdbDirectAdjacencyCommitDelta.sealAsync` returns a `CompletableFuture` so the log mirrors the seal through `AdjacencyDeltaMirror` without a waiting thread. `AdjacencyCommitLogMirrorsDataRevisionTest` (9 tests) covers every isolation level, rollback, clear, re-adding an existing statement, a NONE transaction with several updates, MAP_FULL growth with replay, no collector, a forced overflow and the initial NONE import. The old listener still runs unchanged.
- [x] (2026-10-27 17:45Z) Milestone 3: readers acquire views from the applier's state through `AdjacencyAcquisition` and an immutable `LmdbAdjacencyPublishedState` projection; `LmdbDirectAdjacencyStore` binds the applier through `IndexOps` and lost its build/catch-up/cutover/synchronous-publication/gap-marker machinery (5,550 to about 3,200 lines; `AdjacencyDeltaMirror` deleted). `LmdbAdjacencyApplierCutoverTest` (6 scenarios on the stepping scheduler with an LMDB oracle) drives the hazards; the surviving hooks fire at the points listed in the Decision Log. 113 test files were kept green by rebinding hooks, rewriting the tests that encoded the retired protocol (see Decision Log), disabling compaction tests until Milestone 4 and making the fold-down helpers rebuild. Full module: 10,831 tests, one failure, a double release of a sealed delta between the applier and an injected listener (see Surprises; fixed, `LmdbCommitAtomicityTest` then passed 8 of 8). CI's `LmdbFrozenSpillReplayIT` exposed the commit-log retention leak and passes with the fix. Deviations from the milestone text: `maintenanceState()` and `emergencyGapFromRevision` stay as diagnostics; `LmdbDirectAdjacencyConcurrencyStressTest` three-round soak is left to Milestone 6. Evidence in `initial-evidence.txt`; draft PR eclipse-rdf4j/rdf4j#6289.
- [ ] Milestone 4: compaction, base fold, quiescent rebuild, initial-import deferral and memory refusal become applier tasks; the compaction and preparation executors become stateless workers.
- [ ] Milestone 5: all `...ForTest` interleaving hooks in `LmdbDirectAdjacencyStore` are removed; tests use `awaitApplied`, `awaitServing` and the step-mode scheduler.
- [ ] Milestone 6: stress, soak, and CI parity; retrospective.


## Surprises & Discoveries

- Observation: the first NONE write into an empty store starts the initial NONE import whenever direct adjacency (or the value overlay) is enabled, regardless of `initialRunImportEnabled`, and detaches the collector for its duration (`LmdbSailStore.beginInitialNoneImport`, `:8038-8047`, gated on `tripleStore.isEmpty()`). With the TripleStore-owned log those revisions become `OVERFLOWED` records and the log stays contiguous, which is what the test now asserts. The run import (`LmdbInitialRunImport`) is a different mechanism.
  Evidence: `AdjacencyCommitLogMirrorsDataRevisionTest#theInitialNoneImportKeepsTheLogContiguous`; the first version of `everyIsolationLevelProducesExactlyOneRecordPerCommit` failed with "expected DELTA but was OVERFLOWED" because NONE was its first commit.
- Observation: no `EMPTY` record appeared in any Milestone 2 scenario, including re-adding an existing statement under READ_COMMITTED and NONE; the "empty delta publishes no marker" hole is therefore theoretical for the write paths exercised so far, and the log still guards it.
  Evidence: `assertMirrored` in `AdjacencyCommitLogMirrorsDataRevisionTest` asserts `kind != EMPTY` for every record.
- Observation: the model test caught four defects in the first applier draft before any LMDB wiring: the loop reported work while a rebuild was waiting for readers to release a dropped base (a livelock under `drain()`); rows of unapplied records leaked at close; a stale "base dropped for rebuild" flag blocked rebuilds after suspend/resume; and the harness itself had to learn that a record may legitimately become a gap after a view was acquired (the view's pending set already covers its rows).
  Evidence: the failing runs are in `logs/mvnf/20261027-11*-verify.log`; the green run is `logs/mvnf/20261027-112147-verify.log` and `initial-evidence.txt`.

- Observation: the adjacency index is strictly in-memory. Nothing adjacency-specific is ever written to disk, and the revision counter it is keyed on, `TripleStore.dataRevision`, is a plain `AtomicLong` that restarts at zero on every open. The redesign therefore needs no durable markers; the only durable identities in play (LMDB environment transaction ids, the bulk generation UUID, the value-store CLEAN marker) belong to LMDB-side startup trust and are untouched by this plan.
  Evidence: `LmdbDirectAdjacencyStore.java:65-68` ("no code path here creates a file (invariant I1)"), `LmdbAdjacencyArena.java:23-31`, `TripleStore.java:542`.
- Observation: a commit's adjacency delta can reach the index by three different routes decided per commit under `publicationLock`: synchronous publication into the live base by the preparation thread while the committer blocks under the LMDB write lock; deferred publication after the revision is already visible when a connection write is admitted; or a pending-table marker plus a queued delta drained by the maintenance thread. Each route has its own validation and retry protocol.
  Evidence: `LmdbDirectAdjacencyStore.java:485-595` (`prepareAsyncCommit`), `:829-890` (`beforeRevisionBump`), `:914-968` (`applyCommitted`), `:1176-1226` (`drainApplyQueue`).
- Observation: the authoritative revision is mirrored into at least six loosely coupled shadows that must agree: `published.appliedRevision` and `horizonRevision`, `published.gapFromRevision`, `emergencyGap` (two fields, cleared only by the cutover CAS), `asyncPublicationRevision` (a never-reset maximum bumped in one place), `catchUpTargetRevision`, and the head of `applyQueue`. The `MaintenanceState` enum is a bare volatile written from the maintenance, compaction, committing and reader threads.
  Evidence: `LmdbDirectAdjacencyStore.java:132-175, 244, 1720, 2056, 3290-3301, 4704`.
- Observation: an empty sealed delta publishes no pending marker, so a reader pinned at that revision sees its revision above the horizon and is pushed into `REVISION_GAP` with a throttled gap-recovery request until the applier advances. It is benign only if every triple mutation records at least one event; nothing asserts that.
  Evidence: `LmdbDirectAdjacencyStore.java:850-853, 3659-3662`.
- Observation: compaction and base fold exclude synchronous preparations (`preparationsInFlight`) but not the maintenance thread's `applyOne`. A fold published between `retainPublishedState` and `publishPrepared` changes the base identity, `publishPrepared` throws, and a healthy delta drives the store into `APPLY_STALLED` and a full quiescent rebuild.
  Evidence: `LmdbDirectAdjacencyStore.java:1394-1402, 1461-1466, 1748-1770, 2010-2019`.
- Observation: the build captures the gap marker before pinning its snapshot and aborts the whole scan whenever the gap sequence changes mid-build. Deferred commits record gaps after the revision bump, outside the lock that argument relies on, so under steady degraded traffic an expensive build can livelock.
  Evidence: `LmdbDirectAdjacencyStore.java:3045-3049, 3243-3245, 3259-3263`; commit `272ac65ff2` in PR #6206 and its rationale.
- Observation: of the stabilisation commits since 2026-09-20, the adjacency build/cutover/catch-up group and the write-admission group account for every missed wake-up and ordering hole; the two other groups (NONE writer-thread ownership and async hand-off; map-growth read-view invalidation and cancellation) are separate mechanisms and are out of scope here.
  Evidence: commits `97b4422725`, `542b7217eb`, PR #6261 `e93cc47538`, PR #6206 `272ac65ff2`, `d43939d99e`, `7250c3da97`; CI runs 40293617950 (two-minute hang in `LmdbDirectAdjacencyRecoveryTest`), 40182420556 (900-second hang in `LmdbNativeCensusAggregateTest`).

- Observation (Milestone 3): the retired hand-off hook `beforeApplyQueueAdmissionForTest` used to run on the thread that handed a sealed delta to the apply queue; fired from the committing thread it deadlocked five tests for their full fifteen-minute timeout, because each test blocks the hook until the same thread has returned from its commit. The closest equivalent point in the new design is the capture thread right after the seal completes (the record is sealed and confirmed, the applier may apply it), which is where it fires now.
  Evidence: `logs/m3-batch2.log` (five 900-second timeouts in `LmdbDirectAdjacencyQueryTest` and `LmdbDirectAdjacencySnapshotTest`, all parked in `awaitQueueAdmissionRelease` under `applyCommitted`).
- Observation (Milestone 3): `idle()` could report idle between the applier polling an inbox unit and running it, so `buildNowForTest` returned before the rebuild it had just requested started; the fold-down helpers then saw the old base. A `runningUnit` flag raised before the poll and dropped after the unit closes the window, and `startRebuild` raises `buildInFlight` before it drops `rebuildRequested`.
  Evidence: `logs/m3-diag2.log` (`buildsStarted=1` before and `2` after `rebuildNowForTest`, state unchanged).
- Observation (Milestone 3): the old tests encode two different answers to "does a synchronous committer wait behind a build?": an empty store's startup build is waited for (`emptyStoreDefaultsToSynchronousMaintenanceBeforeStartupReturns`, `combinedRepositoryCommitPublishesBothPlanesBeforeReturning`), a populated store's startup build is not (`synchronousMaintenanceReturnsDuringPopulatedStartupAndWaitsForSubsequentCommits`, `backlogPausesBackingWritesWhileLmdbReadersRemainAvailable`). Waiting unconditionally made the populated cases time out at 60 seconds; never waiting made the first commits of a new store fall back for a moment. The applier binding now records whether the running build pinned an empty store and committers wait only behind that build.
  Evidence: `logs/m3-batch4.log` (`Commit at revision 1 has waited 30004 ms ... phase=BUILDING` in the backlog test), `logs/m3-batch3.log`.

- Observation (Milestone 3): with the adjacency index disabled nothing consumed the TripleStore's commit log, so every revision bump left an `OVERFLOWED` record behind forever. `LmdbFrozenSpillReplayIT` caught it: its 300,000-row streamed load in a 128 MiB child JVM ran out of heap in CI and locally, and passed again once the log drops records whenever no applier listens (the floor follows the head). The log is a hand-off, not a history; a store without a consumer must remember only its head revision.
  Evidence: CI run 40537386304 (`integration-tests`), `logs/m3-spill-it.log` (fails) and `logs/m3-spill-it2.log` (passes).

- Observation (Milestone 3): `LmdbCommitAtomicityTest`'s multi-threaded variant failed about one run in three on this branch and never on the base. A sealed delta now has two releasers: the commit log releases a record's rows on the applier thread once applied, and a derived-state listener (the test injects one) aborts the same sealed delta on the committing thread. `SealedDirectDelta.releasePayload` read and cleared its charge without synchronisation, so both threads released and the second threw "releasing 33856 bytes exceeds the 0 bytes charged", which rode along as a suppressed exception and made `LmdbSailStore` treat the optional derived failure as fatal. The release is now atomic and idempotent.
  Evidence: `logs/m3-atomdiag-class-2.log` (full trace with the suppressed release failure), `/tmp/rdf4j-base/run-*.log` (5 of 5 pass on the base), `logs/m3-atomfix-*.log`.

- Observation (Milestone 3): a paused applier does not make the pending window deterministic by itself, because the seal still completes asynchronously on the capture thread: `pendingWindowFallsBackOnlyForTouchedRows` acquired its view a hair before the seal and saw `PENDING_ROW` instead of a row-exact view. The committer's post-commit seam now waits (bounded) for the unapplied records to seal whenever the scheduler is paused, which is a test-only state, so every pause-then-commit test observes a sealed record.
  Evidence: `logs/m3-batch8.log` (the failure), `logs/m3-batch9.log` (eight suites green after the change).


## Decision Log

- Decision: a rebuild candidate is published as soon as it arrives, at its own `appliedThrough`, and the applier continues from there through the normal apply path; the build worker stops its private replay at the first unsealed record instead of waiting for the seal.
  Rationale: replaying the remainder before publishing would make the applier hold two bases while it works, and waiting for a seal inside a worker would make the worker depend on another worker. Readers between the candidate's applied revision and the previous state's applied revision get row-level pending views for a moment; the retention hold guarantees those records exist.
  Date/Author: 2026-10-27, Claude Code (Milestone 1).
- Decision: when a rebuild had to drop the published base first and readers still hold that base, the applier re-checks the memory fit against the retained base before deciding to wait, and `hasWork()` reports no work while it waits.
  Rationale: the model test found a livelock where the loop reported work it then did not perform, and a stale "dropped" flag kept a rebuild waiting after a suspend/resume although a second base would have fit.
  Date/Author: 2026-10-27, Claude Code (Milestone 1).
- Decision: the worker and scheduler seams are `AdjacencyWorkerPool` and `ApplierScheduler`; the index-specific work is one strategy interface `AdjacencyIndexOps<B, O, G>` with explicit reference-ownership rules, and the applier is generic in the base, overlay-set and generation types.
  Rationale: the applier's correctness argument is about sequencing and ownership, not rows; keeping rows behind a strategy lets the model test exercise every interleaving with a 24-quad universe in about a second, and lets Milestone 3 plug in `LmdbInMemoryAdjacencyIndex`, `LmdbAdjacencyOverlaySet` and `LmdbAdjacencyDeltaGeneration` without touching the applier.
  Date/Author: 2026-10-27, Claude Code (Milestone 1).

- Decision: one append-only, revision-ordered commit log is the only channel from commits to the adjacency index. There is no synchronous publication by the committing thread, no deferred publication by the preparation thread, and no separate pending-table publication.
  Rationale: every race catalogued above is between two of those three channels, or between a channel and the build's catch-up loop. One channel with one consumer makes "the record for revision N exists before any reader can pin N" the whole correctness argument.
  Date/Author: 2026-10-27, Claude Code.
- Decision: exactly one thread, the applier, publishes adjacency states. Heavy work (sealing, generation preparation, base scans, range merges) runs on stateless worker threads that return candidates; only the applier decides whether and when a candidate becomes the published state.
  Rationale: with a single publisher the premises a candidate was built on (base identity, overlay prefix, applied revision) can only be invalidated by the applier's own later actions, which are appends, so validation reduces to "is the prefix unchanged", and compaction can never race a delta apply.
  Date/Author: 2026-10-27, Claude Code.
- Decision: the committing thread may publish a provisional record before the physical LMDB commit so a worker can start sealing and preparing the generation during `mdb_txn_commit`; the log head only moves when the committer confirms the record after `MDB_SUCCESS`, still under the TxnManager write lock. Readers never see provisional records.
  Rationale: this keeps the overlap between preparation and the physical commit that the current synchronous path relies on for commit latency, without handing publication ownership to the committer. At most one provisional record can exist because the write lock serialises commits.
  Date/Author: 2026-10-27, Claude Code.
- Decision: a gap is a kind of log record (`OVERFLOWED`), not a side marker. The applier reacts to it by publishing a state whose `gapFromRevision` is that revision and requesting a rebuild; readers below the gap keep exact views.
  Rationale: today a gap is recorded through `markGap` from seven call sites into a marker that lives beside the published state and is cleared only by the cutover CAS; making it a record puts it in the one ordered structure everyone already reads.
  Date/Author: 2026-10-27, Claude Code.
- Decision: write admission depends only on the number of unapplied bytes in the log. There is no cutover closure and no LMDB read fence during a build.
  Rationale: with the build expressed as "pin at revision B, scan, then replay the log from B+1", there is nothing for writers to wait for; the log is the hand-off. The byte bound still protects memory.
  Date/Author: 2026-10-27, Claude Code.
- Decision: the public and test-facing readiness contract is `awaitApplied(revision)` and `awaitServing(revision)`, plus one injectable applier scheduler with a step mode. Every `...ForTest` interleaving hook in `LmdbDirectAdjacencyStore` is deleted.
  Rationale: the hooks are a hand-driven clock that the tests must advance in exactly the right order; two of the stabilisation commits only reordered that clock. A revision-based wait and a deterministic scheduler express every interleaving the existing tests need.
  Date/Author: 2026-10-27, Claude Code.
- Decision: the log is owned by `TripleStore` and appended on every revision bump; `DELTA` records carry a `sealed` flag and split rows/pending payloads; gaps discovered by the applier or reported by readers are written into the log with `markGap`; write admission admits writers whenever the applier is not consuming and the applier then gaps and releases the backlog; the connection-level admission wait stays at its pre-branch-lock site.
  Rationale: an independent review of the first draft against the code found that otherwise (a) a reader pinned right after confirm would read a pending table that the sealing worker has not produced yet, (b) writers would wedge on the byte bound whenever the applier is suspended, refused or failed, (c) applied deltas would be retained for as long as any old view lived, (d) gaps found during apply or by SHADOW comparison would be invisible to the build worker, (e) the initial import would leave holes in the log while the listener is nulled, and (f) moving the connection wait after the Sail branch locks inverts the lock order the current code documents.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): the applier binds to the LMDB index through one inner strategy (`LmdbDirectAdjacencyStore.IndexOps`) with `B = LmdbInMemoryAdjacencyIndex`, `O = LmdbAdjacencyOverlaySet` (null while no generation exists, as the row resolver's fast paths expect) and `G = PreparedApply` (a generation plus the context catalog, extra selected predicates, plane statistics update and structural statistics that travel with it). Plane statistics move into the overlay set; `LmdbAdjacencyPublishedState` becomes an immutable projection built per exact view from the applier's state and the pending tables of the log records in `(applied, snapshot]`, owning nothing.
  Rationale: the row-resolution half of the store (about 2,000 lines) and five other classes read the published state through its getters; projecting keeps them untouched while ownership moves to `AdjacencyAcquisition`.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): the retired interleaving hooks keep firing at the closest lock-free points of the new design (build worker before pin, after pin, after scan and before private replay; preparation before and after; exact-view return; before an admission wait; the post-commit seam in `applyCommitted`) until Milestone 5 rewrites the tests that set them. `catchUpWaitForTest` and the compaction hooks have no equivalent and never fire. The production scheduler gained `pause`/`resume`/`stepWhilePaused` so `pauseApplierForTest` keeps its meaning and tests can step units; a commit never waits for a paused applier.
  Rationale: 113 test files reference these seams; rewriting them all inside the cutover would make the cutover unreviewable. The bridge keeps the module green with the old tests while the new `LmdbAdjacencyApplierCutoverTest` expresses the hazards on the stepping applier.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): the collector's sealing executor stays single-threaded (`captureExecutor`), separate from the two-thread worker pool for builds and preparation.
  Rationale: the collector submits event batches and then the seal to the same executor and relies on their order; on a two-thread pool the seal overtook a batch and produced an empty delta (caught by `AdjacencyCommitLogMirrorsDataRevisionTest`).
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): `secondBaseFits` estimates the new base at the size of the current one and lets a rebuild run beside the live base when the account has room, so routine rebuilds keep readers exact; only when it does not fit does the applier drop the live base first.
  Rationale: returning false unconditionally reproduced the quiescent-rebuild behaviour for every rebuild and made `buildNowForTest` flip a serving store to UNAVAILABLE.
  Date/Author: 2026-10-27, Claude Code.
- Decision: the redesign is done additively (new classes beside the old, dual publication for one milestone, then readers switch, then the old code is deleted) rather than by rewriting `LmdbDirectAdjacencyStore` in place.
  Rationale: `LmdbDirectAdjacencyStore` is 5,550 lines and the row-resolution half (`resolveRow`, kernels, synopsis, node-predicate projection) is unaffected; the module has 500-plus adjacency tests that must stay green at every milestone.
  Date/Author: 2026-10-27, Claude Code.

- Decision (Milestone 3): a synchronous committer waits until its revision is served. It stops waiting when the index is closed, gapped at or below its revision, or not serving, with one exception: a build that pinned an empty store (a new store's startup build) is waited for, because it is trivial and it is what makes the first commits of a new store exact at once. A populated store's build never blocks committers; the live base keeps applying beside a rebuild, so a rebuild over a live base never blocks them either.
  Rationale: see the matching observation above; this is the rule the existing tests already agreed on without stating it.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): the hooks that survive until Milestone 5 are bound as follows. `beforePreparationForTest` fires on the capture thread before each full capture batch and before the seal (the retired "preparation" was the seal plus the generation, and tests use it to hold a commit's delta); `afterPreparationForTest` fires on the applier worker after a generation is prepared; `beforeApplyQueueAdmissionForTest` fires on the capture thread once the sealed record is visible; `beforePreparedFinalizeWaitForTest` fires when a committer starts its publication wait; `beforeQuiescentRebuildStepForTest` fires only for rebuilds the store requests itself, so tests that must hold a rebuild the applier scheduled on its own use `beforeBuildSnapshotForTest`.
  Rationale: each binding is the closest lock-free point with the same observable meaning; two tests that encoded "the hand-off is in progress" (`resumingApplierWaitsForCommittedPreparation[queue admission]`, `readinessWaitsForAnUnpublishedCommitToReachTheApplyQueue`) now hold the seal instead, because a sealed record is already available to the applier.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): strict mode (`failOnMaintenanceError`) remembers the first unexpected build or prepare failure in the store and throws it, with the original failure as cause, from `acquire` and from the committer's publication wait; gaps (overflow, `markGap`, selected-predicate classification), memory refusal and a build that cannot run are supported fallbacks and never throw.
  Rationale: the applier itself treats every failure as "degrade, request a rebuild, retry"; strict mode is a store-level contract about surfacing bugs in tests and must not make the applier's recovery conditional.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): a node-predicate projection inconsistency reports corruption at the current revision (`reportCorruption`), so every reader falls back until the rebuild publishes a fresh base, instead of only switching the projection off while the rest of the index keeps serving.
  Rationale: a base whose projection lies is not trusted for anything; the old test (`aCorruptedProjectionDegradesThenRecovers`) asserts the whole index declines while the rebuild is parked, and that is the safer contract.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): `AdjacencyApplier.close` shuts the scheduler down (the production scheduler joins its loop thread), shuts the workers down, waits for them to go idle and then runs the units still in the inbox on the closing thread.
  Rationale: a worker interrupted mid-prepare or mid-build reports its outcome into the inbox; nothing else would run that unit once the loop thread is gone, and the unit is what releases the predecessor state and the candidate, so without the drain a close during an in-flight apply leaked the published base (`closeWaitsForInFlightApplyBeforeRetiringPublishedBase`).
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): `maintenanceState()` stays as a diagnostic projection of the applier phase and `emergencyGapFromRevision` stays in the metrics snapshot as an alias of `gapFromRevision`, so the readiness description and the Workbench status page keep their shape; the acceptance grep for Milestone 3 is adjusted accordingly. Compaction and fold are disabled by policy (`LmdbDirectAdjacencyConsolidationTest` is `@Disabled` until Milestone 4, and the node-predicate fold-down helpers publish a new base through a rebuild instead), which keeps the lifetime and parity assertions meaningful.
  Rationale: the plan allows the temporary policy; the retention argument those tests make (a pinned view keeps its base, the last lease frees it) is the same for a rebuilt base as for a folded one.
  Date/Author: 2026-10-27, Claude Code.
- Decision (Milestone 3): tests that asserted retired mechanisms were retired or rewritten rather than bridged: the queue-metadata reservation tests (`asynchronousCaptureChargesMetadataBeforeWorkerExecution`, `emptyCollectorRevisionsChargeTheirQueueMetadata`: the log holds records, the memory account holds the pending charge, there is no queue), the catch-up-thread test (`aGapRecordedWhileCatchUpWaitsForItsRevisionEndsTheBuild`: the model test's `COMMIT_DURING_SCAN` plus `SEAL_OVERFLOWED` covers a gap during a build), the admission-closure assertions (`tryBeginBackingWrite` is false only on backlog), the "publish before revision visibility" assertion (the revision is visible when LMDB commits; the committer is who waits), and the cutover metrics (`lastCutover*` report zero).
  Rationale: a bridge that fakes the old observable would test the bridge.
  Date/Author: 2026-10-27, Claude Code.

- Decision (Milestone 3): `AdjacencyCommitLog` trims itself through its head on every confirm and gap append while no listener is installed; records are retained only once an applier has attached.
  Rationale: the adjacency index is optional and off by default; a TripleStore must not accumulate per-commit objects for an index that does not exist (see the frozen-spill observation above).
  Date/Author: 2026-10-27, Claude Code.


## Outcomes & Retrospective

(To be written at each milestone and at completion.)


## Context and Orientation

All production code is under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/` and all tests under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/`. Paths below are relative to those directories unless written in full. Line numbers refer to commit `f0adec73f4` on `main`.

A "revision" is the value of `TripleStore.dataRevision` (`TripleStore.java:542`), a process-local counter that goes up by one for every committed transaction that changed triples. A "snapshot" is an LMDB read transaction (`TxnManager.Txn`) plus the revision it was pinned at. A pinned reader is created in `TxnManager.ReaderReservation.startPinned` (`TxnManager.java:518-556`): it takes the TxnManager read barrier, begins the LMDB read transaction, then reads `dataRevision`. Commits hold the same lock in write mode from before the physical LMDB commit until after `dataRevision.set` (`TripleStore.java:7523-7546`, `:6087-6090`), so a pinned reader's revision and its LMDB snapshot always agree. That lock is the one place this plan builds on; nothing in it changes.

The "direct adjacency index" is `LmdbDirectAdjacencyStore`. Its published unit is `LmdbAdjacencyPublishedState` (`LmdbAdjacencyPublishedState.java`): an immutable, reference-counted tuple of a base (`LmdbInMemoryAdjacencyIndex`, a compressed copy of all rows at `baseRevision`), an overlay set (`LmdbAdjacencyOverlaySet`, one `LmdbAdjacencyDeltaGeneration` per applied commit or per merged range of commits), an array of pending tables (sorted node keys touched by committed-but-not-yet-applied deltas), `appliedRevision`, `gapFromRevision`, a serving state and `minSnapshotRevision` (the oldest revision the overlays can still reconstruct after compaction). A reader acquires a `LmdbAdjacencyReadView` (`LmdbDirectAdjacencyStore.acquire`, `:3608-3688`); it is "exact" when the state serves the reader's revision and otherwise carries a `FallbackReason` (`LmdbAdjacencyMetrics.java:32-50`) and the dataset reads LMDB instead. An exact view at revision R uses only overlays with revision at most R and treats any row touched by a pending table at or below R as "read it from LMDB".

A commit produces an `LmdbDirectAdjacencyCommitDelta.SealedDirectDelta` (`LmdbDirectAdjacencyCommitDelta.java:1303`): the recorded adds and removes of one transaction as parallel long arrays plus a `PendingTable` (`:1193`). Events are recorded while the transaction runs, only for mutations that really happened (`TripleStore.java:6373-6414, 7370-7384`), so duplicate adds and absent removes produce nothing and `clear` is just many removes. A delta whose memory exceeds `commitMaxBytes` is "overflowed": it carries no rows and the index cannot be brought past that revision by replay.

The TripleStore hands the delta to the adjacency store through `TripleStore.DirectAdjacencyCommitListener` (`TripleStore.java:5923-5962`) inside the commit critical section: `prepareAsync` before the physical commit (`:7533`), `finalizeCommit` after `MDB_SUCCESS` and before `dataRevision.set` (`:6057-6090`), `abort` on failure. The sail store then enqueues the returned delta outside the lock (`LmdbSailStore.java:8246-8273`). Section `Surprises & Discoveries` lists the three routes the adjacency store takes from there.

Everything that follows replaces the lifecycle half of `LmdbDirectAdjacencyStore` (roughly lines 124-3500: fields, commit listener, prepared commits, apply queue, write admission, compaction loop, consolidation, quiescent rebuild, `buildOnce`, `catchUpAndPublish`, `acquire`) and leaves the row-resolution half (from `resolveRow` at `:3737` onward) untouched except for reading from the new state type.


## Plan of Work

### The new model

Three new package-private classes carry the whole synchronisation. Define them in new files beside the existing ones.

`AdjacencyCommitLog` is the single channel from commits to the index. It is owned by `TripleStore`, so that a record is appended for every revision bump whether or not an adjacency store is attached or a collector was begun (today `prepareDerivedCommit` returns null when the listener is nulled or the collector has not begun, `TripleStore.java:6009-6012`, and the revision still advances). It holds one `Record` per revision. A record is one of three kinds: `DELTA`, `EMPTY` (the transaction changed triples but recorded no events, which should not happen and is kept only so the log has no holes; see Milestone 2) and `OVERFLOWED` (the delta exceeded its budget, sealing failed, the commit's derived step failed after the physical commit, no collector was attached, or the applier later declared the revision a gap). A `DELTA` record has two separable payloads: `rows` (the sealed delta's subject, predicate, object, context and flag arrays, needed only to apply) and `pending` (the small sorted key table readers use to decline rows touched at that revision). Because sealing runs on a worker (`LmdbDirectAdjacencyCommitDelta.sealAsync`, `:274-290`, builds the pending table inside `seal`), a `DELTA` record is created unsealed and carries a `Future<SealedDirectDelta>`; the worker publishes `sealed()` (a volatile flag) when both payloads exist, or `sealFailed()`. Every record knows its revision and its current byte charge.

The log has a `headRevision` (the newest confirmed record, equal to `dataRevision` at all times) and a `floorRevision` (records at or below the floor have been trimmed). Only the committing thread, inside the TripleStore writer transaction and holding the TxnManager write lock at the moment of the call, mutates the head: `stage(revision, sealing)` creates a provisional `DELTA` record so a worker may seal and prepare while `mdb_txn_commit` runs; `confirm()` makes it the head; `retract()` discards it; `appendOverflowed(revision)` records a gap. The log asserts that a confirmed or appended revision equals `headRevision + 1`. At most one provisional record exists because `TripleStore` has at most one writer transaction (`writeTxn`, `TripleStore.java:7449-7451`), not because of the write lock: a `MAP_FULL` retry releases the write lock between `stage` and `confirm` (`:7546`, `:7564`, `resizeAndReplay` re-locks at `:5714` and replays with derived recording suppressed), so a staged record may outlive one lock hold, and a generation prepared against it stays valid because the replayed delta is byte-identical.

The applier is the only other mutator: `markGap(revision)` replaces a record above `applied` with an `OVERFLOWED` record of the same revision (used when a seal fails, when applying the record fails, when SELECTED-coverage classification cannot prove a predicate's absence, or when a reader reports corruption), `releaseRows(revision)` drops the rows payload after apply, and `trimThrough(revision)` drops whole records; the floor rule is under "Retention". Readers and the applier call `record(revision)` and `forEachAfter(from, to)`, lock-free reads of an immutable segment array whose elements are replaced, never mutated. `unappliedBytes()` is the byte sum of rows payloads not yet released, and drives write admission.

`AdjacencyApplier` is the single owner of the published state. It runs on one dedicated thread (reuse the existing `maintenanceThreadFactory`), consumes the log in revision order, and holds an `AtomicReference<AdjacencyState>` that only it writes. Its loop is: take the next unit of work, which is either the next log record above `state.applied`, a completed worker result, or a queued task (`Rebuild`, `Compact(range)`, `Fold`, `Suspend`, `Resume`, `ReportCorruption(revision, reason)`, `ViewReleased`, `Close`), then publish at most one new state, then signal `applied` waiters and write-admission waiters. Two rules keep it live: the loop never joins a worker future on its own thread (workers complete futures whose callbacks enqueue a result unit, so `awaitApplied` latency never becomes build duration), and the loop body catches `Throwable`, publishes `UNAVAILABLE(FAILED)` with the description and signals every waiter, so a dead applier can never hang a synchronous committer. A worker is a stateless function on the existing `preparationExecutor` or a build pool: `seal(delta)`, `prepareGeneration(record, workingSet)`, `scanBase(hold)`, `mergeRange(state, from, to)`, `foldBase(state)`, where a `workingSet` is the tuple (base, generations so far, extra selected predicates, context catalog, plane statistics) that today's `catchUpAndPublish` accumulates (`LmdbDirectAdjacencyStore.java:3278-3280, 3338-3347`), because a worker replaying privately cannot prepare against a published state. Workers never touch `state` or the log except through the result they return.

`AdjacencyState` replaces `LmdbAdjacencyPublishedState`. It keeps the base, overlays, `appliedRevision`, `minSnapshotRevision`, plane statistics and the reference-counting word, and adds `status` (`SERVING`, `UNAVAILABLE(reason)`, `CLOSED`), `gapFromRevision` and `phase` (an informational enum for metrics: `EMPTY`, `BUILDING`, `ACTIVE`, `CONSOLIDATING`, `DEGRADED_GAP`, `MEMORY_REFUSED`, `FAILED`, `DEFERRED`, `CLOSED`; today's `CATCHING_UP`, `APPLY_STALLED` and `QUIESCING_FOR_REBUILD` no longer exist as phases and `FAILED_CORRUPT` folds into `FAILED` with a reason; written only by the applier as part of the tuple). It drops the pending-table array: pending rows at revision R are the pending payloads of log records in `(appliedRevision, R]`, read from the log at acquisition time. The `horizonRevision` concept disappears because the horizon is always the log head. The applier's single `publish` method keeps today's `LmdbAdjacencyGenerationTransition.validate`, `activateAdditions` and `deactivateRemovals` sequence (`:3477-3487`) and clears `nodePredicateInconsistent` on a successful rebuild publish (`:3085`).

### Commit path

In `TripleStore.endTransaction` the listener interface shrinks to three calls and the log append becomes unconditional. Before the physical commit and under the write lock, where `prepareDerivedCommit` is called today (`TripleStore.java:7533`), `stage(nextRevision, Future<SealedDirectDelta>)` creates a provisional `DELTA` record whose sealing runs on a worker, if a collector is attached and has begun; otherwise nothing is staged. After `MDB_SUCCESS`, still under the lock and immediately before `dataRevision.set` (`:6089`), `publishDerivedCommit` calls `confirm()` if a record was staged and `appendOverflowed(nextRevision)` otherwise, so every revision has a record. On any failure between stage and confirm, `retract()` discards the staged record and, if the physical commit had already succeeded, `appendOverflowed` records the gap so the log stays contiguous. A seal failure surfaces in the worker's future, not at confirm; the applier handles it with `markGap` when it reaches the record. The committer never waits for sealing, preparation or publication inside the lock.

Replace the current `DirectAdjacencyCommitListener` (`prepare`, `prepareAsync`, `finalizeCommit`, `abort`, `physicalCommitFailedBeforeRevisionBump`, `beforeRevisionBump`) with this `stage`/`confirm`/`retract` shape. Delete `TripleStore.sealedDirectAdjacencyDelta` and `drainDirectAdjacencyCommitDelta` (`TripleStore.java:5981-5985`); the sail store no longer forwards deltas. Delete `LmdbSailStore.finishCommittedDerivedState`'s `applyCommitted` and `captureBackingWritePublication` calls (`LmdbSailStore.java:8253-8262`), keeping the admission release.

Synchronous maintenance (`rdf4j.lmdb.directAdjacency.synchronousMaintenance`, default true) keeps its meaning, that a connection's commit returns only once the index has applied its last revision, but becomes a wait outside every lock: at the end of `LmdbStoreConnection.commitInternal` where `endLogicalCommitBatch` is called today (`LmdbStoreConnection.java:524`), call `applier.awaitApplied(lastRevisionCommittedByThisConnection)` when the option is on (when it is off the commit path skips the wait, as `awaitPublication` returns early today, `:996-999`; `awaitApplied` itself always works so tests can use it either way). This is the same point at which today's deferred route waits (`awaitBackingWritePublication`, `:1141-1152`), so deadlock exposure is not widened. The wait completes when `state.applied >= revision`, or when the applier publishes a state whose `status` is `UNAVAILABLE` or `CLOSED`, or when `gapFromRevision <= revision` (the index will never apply it); in those cases the commit still succeeds, because LMDB is authoritative and readers fall back. Deadlock argument, to be kept as a comment on the method: by this point the committer has released the TxnManager write lock, `sinkStoreAccessLock`, `txnLock` and its writer lease (`LmdbStoreConnection.java:575-596`, `LmdbSailStore.java:7490, 7588`); the applier needs no lock a committer can hold (a rebuild worker pins through `TxnManager.createReadTxnPinnedFamily`, which takes only the read barrier and reader permits), the applier never joins a worker on its own thread, and the applier's loop converts any failure into a published `UNAVAILABLE` state that completes the wait.

### Reader path

`acquire(snapshotRevision R)` becomes: read `state` once and `tryRetain` it (if that fails the applier has just replaced it; re-read and try again, bounded, then `STATE_CHURN`); with the state retained, if `state.status != SERVING` release it and return the matching fallback; if `R < state.minSnapshotRevision` release and return `SNAPSHOT_BEFORE_BASE`; if `state.gapFromRevision <= R` release and return `REVISION_GAP`; if `R > state.applied`, first check `R - state.applied <= MAX_PENDING_TABLES` (64, as today) and otherwise release and return `PENDING_ROW` so a lookup never scans an unbounded number of pending tables; then collect the records in `(state.applied, R]` from the log (they exist and cannot have been trimmed: R's record was confirmed under the write lock before R could be pinned, and the retained state holds the floor at or below `state.applied`); if any is `OVERFLOWED`, release and return `REVISION_GAP`; if any is not yet `sealed()`, release and return `PENDING_ROW` (this is today's wholesale fallback at `:3638-3642`, which the deleted `asyncPublicationRevision` implemented); otherwise build an exact view whose pending set is those records' pending payloads. If `R <= state.applied`, return an exact view with an empty pending set. The retain must precede reading the log because the floor is derived from live states (see "Retention"). Readers never request gap recovery, because the applier reads the same records; a reader that detects corruption (a SHADOW-mode mismatch, `:4702-4705`, or an `LmdbNodePredicateInconsistencyException`) enqueues `ReportCorruption(viewRevision, reason)` and the applier marks the gap and schedules the rebuild. When a view's last lease is released it enqueues `ViewReleased` rather than consulting the compaction policy on the reader thread (`:3709` today).

Preparation may start before confirmation: when a provisional record is staged and `state.applied == revision - 1`, the applier may submit `prepareGeneration` against the current state at once, so the heavy part of applying overlaps `mdb_txn_commit` as it does today; the prepared generation is published only after the record is confirmed and discarded if it is retracted.

The per-row pending check (`nodePredicateDecline`, `PENDING_ROW`) and the overlay revision filter (`hasApplicableOverlay`) stay as they are, reading the view's pending set and overlays.

### Build and rebuild as applier tasks

A `Rebuild` task is the only way an index comes into existence, whether at startup, after a gap, after a memory refusal, after a shadow mismatch, after a node-predicate inconsistency, or after the initial NONE import. The applier runs it as follows. It first asks the memory account whether a second base fits beside the published one (see below for the case where it does not), then submits `scanBase` to a build worker. The worker pins an `LmdbAdjacencyBuildTxnFamily` (unchanged: all readers under one read barrier, stamped with `dataRevision`, cross-checked on revision and LMDB snapshot id), records `baseRevision B`, resolves coverage from the pinned transactions (unchanged, after the pin), scans the base, closes the family, and then, still off the applier thread, replays the log records in `(B, log.head]` into private generations, returning a `Candidate(base at B, generations through C)`. While the worker scans, the applier keeps applying new records to the previously published state, if there is one, so writers never stall; the worker holds a retention pin at B (see "Retention") so the records it will replay are not trimmed. When the candidate arrives, the applier replays `(C, state.applied]` onto it (usually a handful of records; if `C > state.applied` because the old state was stalled, the new state is published at `C`), publishes it as the new `SERVING` state with `gapFromRevision = MAX`, and releases the old state. The worker's private replay uses the same per-record classification the applier uses (today's `classifyForCatchUp`, `:3394-3424`, must gap rather than skip when SELECTED coverage cannot prove a predicate's absence, matching `classifySelected` at `:2487-2490`), and returns a failed candidate if it meets an `OVERFLOWED` record; the applier then discards the candidate and requeues the rebuild, because the next pin will be at or beyond the gap. Applying any record N can fail (preparation failure, memory refusal, classification impossibility); the applier then calls `log.markGap(N)`, publishes the current state with `gapFromRevision = N` and phase `DEGRADED_GAP`, and enqueues `Rebuild`. Readers already holding exact views are unaffected, exactly as today, because a view never re-reads the state it retained.

Replacing `buildOnce` and `catchUpAndPublish` with this removes `closeWriteAdmission`, `openWriteAdmission`, the LMDB read fence at cutover, `awaitCatchUpQueue`, `catchUpTargetRevision`, `suspendSynchronousPublicationForBuild`, `preparationsInFlight`, `noPreparationsInFlight`, `emergencyGap`, `markGap`, `markGapAndScheduleRecovery`, `requestGapRecovery`, `lastGapRecoveryNanos`, `asyncPublicationRevision`, `unpublishedRevisions`, `lastCommittedPublication`, `lastPublicationRevision`, `commitPublication`, `logicalCommitBatchDepth`, `rebuildPending`, `rebuildFuture`, `buildRetryFuture`, `quiescentRebuildPending`, `quiescentRebuildTaskScheduled`, `quiescentRebuildAwaitingViews`, `quiescentRebuildRetryAllowed`, `maintenanceUnavailable`, `maintenanceDeferred`, `synchronousUpdatesActivated` and the volatile `maintenanceState`.

Memory for two bases: today a quiescent rebuild always drops the old base first (`:2413-2422`), so building beside a live base is new and must be gated before `scanBase` is submitted. When the memory account cannot hold a second base beside the published one, the applier first publishes `UNAVAILABLE(REBUILDING)` and enqueues the `Rebuild` to run once the old state's reference count reaches zero (the release callback enqueues `ViewReleased` rather than running anything itself). This is the current "quiescent rebuild yielding to views" expressed as a task state; the store-wide `activeAcquisitionsAndViews` counter goes away because the state's own reference count is the thing being waited for.

Failure of a rebuild (`LmdbAdjacencyBuildUnavailableException`, `LmdbAdjacencyMemoryRefusedException`, any other exception) publishes `UNAVAILABLE(reason)` with the failure description and requeues `Rebuild` after `buildRetryMillis` through the applier's scheduled executor. There is no separate retry future to cancel: a `Close` task drains the queue.

### Compaction and fold as applier tasks

After every publish the applier asks `LmdbAdjacencyTieredCompactionPolicy` whether a range should be merged or the base folded, and enqueues at most one `Compact(range)` or `Fold` task if none is pending. A task submits `mergeRange` or `foldBase` to a worker with the current state retained, then continues applying log records while the worker runs. When the worker's candidate arrives, the applier checks that the candidate's base is the current base and its overlay range is still a prefix of the current overlays. Both checks can only fail because of the applier's own later publishes (another compaction, a rebuild), never because of a concurrent apply, so the candidate is either spliced in with `rangeReplacement` or discarded and the policy consulted again. `minSnapshotRevision` rises exactly as today. The compaction executor becomes one of the worker pools; `compactionRequested`, `compactionTaskScheduled`, `lockCompactionPublication` and `consolidatedContextCatalog` are deleted, together with the dead `consolidateOverlayOnly` and `mergeGenerations`.

### Write admission

`tryBeginBackingWrite`, `awaitWriteAdmission` and `beginConnectionWrite` reduce to one rule: a writer is admitted when the applier is not consuming, or when `log.unappliedBytes() < backlogMaxBytes`; otherwise it waits on the applier's admission condition, which the applier signals after every apply, row release, trim and status change. "Consuming" means the state is `SERVING`, or a rebuild worker is in flight with a retention hold; a build in flight counts because the worker will consume the records above its hold. In every other state (`DEFERRED`, `MEMORY_REFUSED`, `FAILED` awaiting retry, `DISABLED`, `CLOSED`) nothing will ever apply the backlog, so the applier releases the rows of every `DELTA` record above `applied`, marks them `OVERFLOWED`, publishes `gapFromRevision = applied + 1`, and admits writers; the eventual `Rebuild` pins beyond the gap. This reproduces what `completeUnavailablePublications` does today (`:1340-1352`) as one rule instead of a flag, and is the invariant "a writer blocked on admission is released by any publish with `status != SERVING`" in the model test.

Where the wait happens matters for lock order. Today `beginConnectionWrite` is taken in `LmdbStoreConnection.prepareInternal` before `super.prepareInternal` takes the shared Sail branch locks (`LmdbStoreConnection.java:413-423`; comment at `LmdbDirectAdjacencyStore.java:1278`, "Reserve the commit before it takes shared Sail branch locks"), and while it is held the backing-write check is bypassed. Keep that placement: connections wait for admission at the `prepareInternal` site, before branch locks, and `acquireAdjacencyAdmission` in the sail store does not wait for a connection writer that already passed it. What goes away is the exclusivity (`admittedWrites == 0`, which serialised concurrent connection prepares store-wide; no test asserts it), the store-wide `connectionWriteAdmitted` flag and its capture budget, since their only remaining purpose was to select the deferred publication route so the committer did not block under the write lock, and no route blocks under the write lock any more. The per-commit byte budget stays in `LmdbDirectAdjacencyCommitDelta`. The owner-tracking fix for replay decisions from commit `7250c3da97` lives in the sail store and stays. Test-facing accessors that disappear with the cutover and must be replaced in the tests that read them: `publicationDiagnostics()` (`LmdbDirectAdjacencyQueryTest.java:2913-3264`, `LmdbDirectAdjacencyCommitTest.java:230`, which assert `admittedWrites=0`) and `lastCutoverRevisions`, `lastCutoverQueuedCommits`, `lastCutoverNanos` (`LmdbAdjacencyStartupConcurrencyTest.java:467-469`, `LmdbAsyncStartupProfile.java:75-76`).

### Initial import and startup

`deferMaintenanceForInitialImport` becomes `applier.enqueue(Suspend)`: the applier publishes `UNAVAILABLE(DEFERRED)`, releases and gaps any backlog as described under "Write admission", and stops consuming records until `Resume`. During the import the sail store today installs null hooks so no delta is collected (`LmdbSailStore.java:8047`, reinstated at `:8087`); because the log is owned by `TripleStore` and appended on every revision bump, those commits produce `OVERFLOWED` records (one word each) without any change to the hook nulling, the log stays contiguous, and the applier's first action after `Resume` is a `Rebuild`, which is what `rebuildAfterInitialNoneImport` does today. `triggerBuild` at post-ready initialisation becomes `enqueue(Rebuild)`; `startupScheduling` is replaced by the applier reporting `UNAVAILABLE(BUILDING)` until then, and `awaitServing` waits through it.

### Retention

The two payloads have different lifetimes. A record's rows are needed only to apply it, so the applier releases them (`releaseRows`) immediately after applying the record, unless a build worker holds a retention hold at or below that revision, in which case they are released when the hold is released; this matches today's `releaseDelta` right after `applyOne` (`:1210, 1240-1243`) and keeps memory bounded by the backlog, not by reader lifetimes. A record's pending payload is small and is needed by any reader whose state is older than the record, so whole records are trimmed only at or below `min(state.applied over all live states, every build worker's retention hold)`. Live states are tracked by a small intrusive list the applier maintains (each `AdjacencyState` knows its predecessor until its reference count hits zero). A build worker takes a retention hold at the applier's current `applied` revision before it pins (the pin revision B cannot be lower than that), narrows the hold to B once the family is pinned, and releases it when its candidate is consumed or discarded. Because the floor only moves when the applier publishes or a state is fully released (`ViewReleased`), the trimming happens on the applier thread in response to those events; no other thread trims. A long-lived view therefore retains only pending tables, and never more than its own span of them is consulted per lookup (see the span bound under "Reader path").

### Readiness API

`awaitApplied(revision, timeout)` and `awaitServing(revision, timeout)` are the only waits. `awaitServing` additionally requires `status == SERVING` and `gapFromRevision > revision`. `LmdbStore.awaitDirectAdjacencyReady` (`LmdbStore.java:811`) delegates to `awaitServing(tripleStore.getDataRevision())`. Both return false when the applier publishes a state that can never satisfy them (`UNAVAILABLE(MEMORY_REFUSED)`, `UNAVAILABLE(DISABLED)`, `CLOSED`) and otherwise wait on a condition the applier signals after every publish.

### Test scheduler

The applier takes an `ApplierScheduler` at construction. The production implementation is a single daemon thread. The test implementation, `SteppingApplierScheduler` in the test tree, runs the applier loop only when a test calls `step()` (one unit of work) or `drain()` (until idle), and exposes `pendingUnits()`. Build and compaction workers are likewise given a `DirectWorkerPool` in tests that runs each submitted function inline on `step()`. This lets a test commit between "pin and scan completed" and "candidate consumed", or between two applies, without any hook in production code. The existing deterministic matrix (`LmdbDirectAdjacencyBuildInterleavingTest`) and stress test (`LmdbDirectAdjacencyConcurrencyStressTest`) are rewritten on top of this.


## Milestones

### Milestone 1: the log and the applier, tested without LMDB

Done 2026-10-27; the files named in `Interfaces and Dependencies` exist. Create `AdjacencyCommitLog`, `AdjacencyApplier`, `AdjacencyState`, `ApplierScheduler` and the worker function interfaces as new files. Give the applier a pluggable `BaseBuilder` and `GenerationPreparer` so the unit tests can drive it with a tiny in-memory model: a map from `(subject, predicate, object, context)` to presence, where the "base" is a copy of the model at a revision and a "generation" is a delta. Write `AdjacencyApplierModelTest` first. It enumerates interleavings of the operations `stage`, `unlockRelock` (the staged record survives a simulated MAP_FULL retry), `confirm`, `retract`, `appendOverflowed`, `sealCompletes`, `sealFails`, `applyNext`, `applyFails`, `builderCompletes`, `builderMeetsGap`, `compactionCompletes`, `foldCompletes`, `suspend`, `resume`, `reportCorruption`, `releaseState`, `writerRequestsAdmission`, `acquire(R)` for small sequences (depth up to eight, exhaustive) and random long sequences (seeded), and asserts after every operation that: the log has no holes between floor and head; every exact view at R reads exactly `model(R)`; a view is never exact at R while any record in `(applied, R]` is unsealed or `OVERFLOWED`; `state.applied` never decreases; `unappliedBytes` equals the byte sum of unreleased rows payloads; every applied record's rows are released unless a build hold covers it; no record below the floor is reachable; reference counts balance; and a writer blocked on admission is released by any publish whose status is not `SERVING`. Also assert the liveness properties that matter: once no `OVERFLOWED` record is above `applied` and the builder has returned, `drain()` reaches `applied == head`; `awaitApplied(head)` is completed by that drain; and `awaitApplied(N)` is completed by a publish with `gapFromRevision <= N` or a non-`SERVING` status. This milestone compiles and passes with the old `LmdbDirectAdjacencyStore` untouched.

Commands, from the repository root:

    python3 .codex/skills/mvnf/scripts/mvnf.py AdjacencyApplierModelTest --module core/sail/lmdb --retain-logs

Acceptance: the test fails before the classes exist (compilation) and passes after; the Surefire report under `core/sail/lmdb/target/surefire-reports/` shows the exhaustive case count.

### Milestone 2: TripleStore appends to the log, dual publication

Done 2026-10-27. `TripleStore` has an `AdjacencyCommitLog` field and appends to it in `publishDerivedCommit` on every revision bump, independently of whether the old listener or the collector is attached (`appendOverflowed` when nothing was staged); `prepareDerivedCommit` stages the seal (as a `CompletableFuture` mapped through `AdjacencyDeltaMirror`) beside the existing `prepareAsync`, so both the old listener and the new log see every commit. The listener interface itself is unchanged until Milestone 3. NONE chunk checkpoints (262,144 rows or 64 MiB per chunk) are not exercised by the test; the multi-revision NONE case is covered through SPARQL updates, which publish per update. Add a test, `AdjacencyCommitLogMirrorsDataRevisionTest`, that runs commits at every isolation level including NONE chunking, rollbacks, `clear`, MAP_FULL growth with replay, and the initial NONE import, and asserts after each that `log.headRevision() == tripleStore.getDataRevision()`, that the log is contiguous, and that every `DELTA` record's pending table equals the old sealed delta's. It should also assert that no `EMPTY` record is ever produced; if that assertion ever fails, record the case in `Surprises & Discoveries`, because it means `tripleMutation` can be true with no recorded event and the old code has the `REVISION_GAP` hole described above. Readers still use the old store.

Acceptance: the new test passes; `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` is green.

### Milestone 3: readers switch, old lifecycle deleted

Instantiate `AdjacencyApplier` inside `LmdbDirectAdjacencyStore`, implement `BaseBuilder` with the existing `LmdbAdjacencyBuildTxnFamily` plus `LmdbPagedCsfBaseBuilder` path from `buildOnce`, implement `GenerationPreparer` with the existing `prepareApply`, and point `acquire` at the applier's state as described under "Reader path". Rewrite `awaitCurrentRevisionReady` as `awaitServing`. Remove the old commit listener and everything listed under "Build and rebuild as applier tasks", plus `LmdbAdjacencyPublishedState` and the pending-table array. Compaction and fold are temporarily disabled by policy in this milestone (they return in Milestone 4), so the overlay set may grow; the existing `maxDeltaGenerations` test expectations are adjusted temporarily and restored in Milestone 4.

Before editing production code, re-express the tests that pin today's hazards so they fail on the old code for the right reason and pass on the new one: the selected-predicate-first-committed-during-build case (`LmdbDirectAdjacencyQueryTest#selectedPredicateFirstCommittedWhileABuildStartsKeepsItsBaseRows`), the gap-recorded-while-a-rebuild-waits case (`LmdbDirectAdjacencyRecoveryTest`), the rebuild-over-a-live-base case (`LmdbDirectAdjacencyCommitTest#rebuildOverALiveBaseCatchesUpACommitThatLandsDuringTheScan`), and the writer-waiting-for-admission-survives-another-rollback case (`LmdbAdjacencyWriteAdmissionTest`). Each becomes a scenario on `SteppingApplierScheduler`: commit, `step()` to the point of interest, commit again, `drain()`, assert the oracle (every pattern's answer equals the matching subset of a full LMDB scan on the same pinned snapshot, as `LmdbDirectAdjacencyConcurrencyStressTest` already does).

Acceptance: `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` green; `LmdbDirectAdjacencyConcurrencyStressTest` passes three rounds under load (`-Drdf4j.lmdb.adjacencyStress.millis=30000` with 24 busy processes, as in the snapshot-consistency plan); `rg -n "emergencyGap|asyncPublicationRevision|closeWriteAdmission|catchUpAndPublish|MaintenanceState maintenanceState" core/sail/lmdb/src/main` prints nothing.

### Milestone 4: compaction, fold, quiescent rebuild, initial import, memory refusal as tasks

Implement `Compact`, `Fold`, `Suspend`, `Resume` and the two-base memory rule. Re-enable the compaction policy. `deferMaintenanceForInitialImport` and `resumeMaintenanceAfterInitialImport` become `enqueue(Suspend)` and `enqueue(Resume)`; the sail store may keep nulling the collector hooks during the initial import because the TripleStore-owned log already records those revisions as `OVERFLOWED` (Milestone 2). Port `LmdbDirectAdjacencyConsolidationTest` to the stepping scheduler, adding one scenario that could not be written before: a delta apply, a compaction candidate for an older range and a fold candidate all pending at once, asserting that whichever the applier consumes first, the other is validated against the new prefix and either spliced or discarded, and that every exact view throughout reads the model.

Acceptance: module green; `LmdbDirectAdjacencyConsolidationTest`, `LmdbInitialRunImportTest`, `LmdbBoundedStartupTest`, `LmdbAdjacencyStartupConcurrencyTest` pass without `pauseApplierForTest`.

### Milestone 5: remove the hooks

Delete every `volatile Runnable ...ForTest` field and every `...ForTest` method from `LmdbDirectAdjacencyStore` (the full list is in `Context and Orientation` of the audit: `beforeBuildSnapshotForTest`, `afterBuildSnapshotForTest`, `afterBuildScanForTest`, `catchUpWaitForTest`, `beforeOnlineCatchUpForTest`, `afterWriteAdmissionClosedForTest`, `beforeWriteAdmissionWaitForTest`, `beforePagedCsfPublicationForTest`, `beforeDeltaCompactionPublicationForTest`, `beforeApplyQueueAdmissionForTest`, `beforeApplyDrainForTest`, `beforePreparationForTest`, `afterPreparationForTest`, `beforePreparedFinalizeWaitForTest`, `beforeExactViewReturnForTest`, `beforeQuiescentBuildForTest`, `beforeQuiescentRebuildStepForTest`, `applierPausedForTest`, `pauseApplierForTest`, `awaitCommitCleanupForTest`, `awaitCompactionForTest`, `requestCompactionForTest`, `buildNowForTest`, `rebuildNowForTest`, `consolidateNowForTest`, `queuedCommitsForTest`, `synchronousUpdatesActivatedForTest`, `awaitAdaptiveAccelerationForTest`). Keep `publishedStateForTest` renamed to `stateForTest` (a read-only accessor) and `snapshotMetrics`. Update the 113 test files that reference a hook: in most, `buildNowForTest` becomes `awaitServing`, `pauseApplierForTest(true)` becomes constructing the store with `SteppingApplierScheduler`, and `awaitDirectAdjacencyReady` stays. `AdjacencyEngagementTestAccess` gains `stepping(store)` and `drain(store)` helpers.

Acceptance: `rg -n "ForTest" core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyStore.java` prints only `stateForTest`; module green; the module's wall-clock time does not increase by more than ten percent compared with the Milestone 2 run (record both in `Artifacts and Notes`).

### Milestone 6: soak and CI parity

Run the module three times under CPU load, then `LmdbRealEstateNativeParityTest` and `LmdbDirectAdjacencyQueryTest` one hundred times each with `-Dsurefire.rerunFailingTestsCount=0`. Open the pull request with `GH-0000` unless an issue number is provided, and watch two CI rounds. Write `Outcomes & Retrospective`.


## Concrete Steps

From the repository root, before any work:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Run a single test (preferred):

    python3 .codex/skills/mvnf/scripts/mvnf.py AdjacencyApplierModelTest --module core/sail/lmdb --retain-logs

Run the module:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Repeat a class to look for timing dependence (replace N):

    for i in $(seq 1 N); do python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRealEstateNativeParityTest --module core/sail/lmdb || break; done

Format before each commit:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Every new Java file carries the repository header and the line `// Some portions generated by Claude` directly below it; run `cd scripts && ./checkCopyrightPresent.sh` after creating files.


## Validation and Acceptance

The plan is complete when all of the following hold. `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` passes, and passes three times in a row with 24 busy processes on the machine. `LmdbDirectAdjacencyConcurrencyStressTest` passes a sixty-second soak per case. `AdjacencyApplierModelTest` passes with its exhaustive interleaving depth at eight. The only production-code test seams in `LmdbDirectAdjacencyStore` are `stateForTest` and `snapshotMetrics`; `ApplierScheduler` is a constructor parameter, not a field tests mutate. `TripleStore.getDataRevision()` equals `AdjacencyCommitLog.headRevision()` at every point a test can observe, and a test asserts it. A deliberately reintroduced defect is caught: for example, moving `confirm()` after `dataRevision.set` in `TripleStore.publishDerivedCommit` makes `AdjacencyCommitLogMirrorsDataRevisionTest` fail, and making the applier publish a compaction candidate without the prefix check makes `AdjacencyApplierModelTest` fail.


## Idempotence and Recovery

Every milestone leaves the module green and is committed on its own, so work can stop or restart at any milestone boundary. Milestones 1 and 2 are purely additive and can be reverted by deleting the new files and the dual-publication calls. Milestone 3 is the point of no return for the old lifecycle; if it must be abandoned, revert that commit rather than trying to re-enable the old paths piecemeal. Tests use `@TempDir` stores and restore the system properties they set.


## Artifacts and Notes

Audit summaries that informed this plan (all read-only, taken at `f0adec73f4`):

    Hook fields in LmdbDirectAdjacencyStore:        18   (lines 206-243: 17 Runnable hooks plus applierPausedForTest)
    ...ForTest methods in LmdbDirectAdjacencyStore:  11
    Test files referencing a ForTest symbol:        113
    Test files using LmdbStore.awaitDirectAdjacencyReady: 46
    Shadow copies of the revision counter:            6   (applied/horizon, gapFrom, emergencyGap x2, asyncPublicationRevision, catchUpTarget, applyQueue head)
    Threads writing the volatile maintenanceState:    4   (maintenance, compaction, committer, reader)

Open CI failures at the time of writing, to be used as regression targets in Milestone 6:

    LmdbRealEstateNativeParityTest.nativeResultsMatchGeneric[5]   7 runs, COUNT expected 3104 got 1
    LmdbDirectAdjacencyQueryTest.disabledDoublyBoundSpecializationRetainsUniversalCandidate:3289
    LmdbDirectAdjacencyQueryTest.genericSailDatasetUnorderedStatementsUseSelectedAdjacencyWhenAvailable:355
    LmdbDirectAdjacencyRecoveryTest.aGapRecordedWhileARebuildWaitsForAViewStillRebuildsOnceTheViewCloses (2 min timeout)

Out of scope, each deserving its own plan: NONE writer-thread ownership and the async ingest hand-off (`LmdbSailStoreVirtualThreadTest`, `LmdbAsyncHandoffTest`, `LmdbNoneStreamingIngestionTest`), and map-growth read-view invalidation and cancellation (`LmdbReadAttemptCancellationTest`, `LmdbMapGrowthCoordinatorTest`, `LmdbSnapshotValueLifetimeTest`).


## Interfaces and Dependencies

No new third-party dependencies. The following package-private types exist since Milestone 1 (signatures below are the shape, not verbatim; the files are authoritative) in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. The applier and state are generic in the base type `B`, the overlay-set type `O` and the generation type `G`; Milestone 3 binds them to `LmdbInMemoryAdjacencyIndex`, `LmdbAdjacencyOverlaySet` and `LmdbAdjacencyDeltaGeneration` through one `AdjacencyIndexOps` implementation:

    final class AdjacencyCommitLog {
        enum Kind { DELTA, EMPTY, OVERFLOWED }
        static final class Record {
            long revision(); Kind kind();
            boolean sealed(); boolean sealFailed();                      // published by the sealing worker
            SealedDirectDelta rows();                                    // null once released
            PendingTable pending();                                      // present once sealed
            long bytes();
        }
        long headRevision();
        long floorRevision();
        void stage(long revision, Future<SealedDirectDelta> sealing);   // committer, under TxnManager write lock
        void confirm();                                                  // committer, same lock; moves head
        void retract();                                                  // committer, same lock
        void appendOverflowed(long revision);                            // committer, same lock; no collector, or failed derived step
        Record record(long revision);                                    // lock-free
        void forEachAfter(long fromExclusive, long toInclusive, Consumer<Record> consumer);
        long unappliedBytes();                                           // unreleased rows bytes
        void markGap(long revision);                                     // applier only; replaces the record with OVERFLOWED
        void releaseRows(long revision);                                 // applier only
        void trimThrough(long revision);                                 // applier only
    }

    final class AdjacencyState {
        enum Status { SERVING, UNAVAILABLE, CLOSED }
        enum Phase { EMPTY, BUILDING, ACTIVE, CONSOLIDATING, DEGRADED_GAP, MEMORY_REFUSED, FAILED, DEFERRED, CLOSED }
        Status status(); Phase phase(); FallbackReason unavailableReason();
        LmdbInMemoryAdjacencyIndex base(); LmdbAdjacencyOverlaySet overlays();
        long appliedRevision(); long gapFromRevision(); long minSnapshotRevision();
        boolean tryRetain(); void release();
    }

    interface ApplierScheduler { void start(Runnable loop); void wake(); void schedule(Runnable task, long delayMillis); void shutdown(); }

    final class AdjacencyApplier implements AutoCloseable {
        AdjacencyApplier(AdjacencyCommitLog log, BaseBuilder builder, GenerationPreparer preparer,
                         LmdbAdjacencyTieredCompactionPolicy policy, LmdbAdjacencyMemoryAccount account,
                         ApplierScheduler scheduler, LmdbAdjacencyMetrics metrics);
        AdjacencyState state();                                         // lock-free
        void enqueue(Task task);                                        // Rebuild, Compact, Fold, Suspend, Resume, ReportCorruption, ViewReleased, Close
        boolean awaitApplied(long revision, long timeout, TimeUnit unit) throws InterruptedException;
        boolean awaitServing(long revision, long timeout, TimeUnit unit) throws InterruptedException;
        void awaitWriteAdmission(long backlogMaxBytes) throws InterruptedException;
        boolean writeAdmitted(long backlogMaxBytes);
    }

    interface BaseBuilder { Candidate build(RetentionHold hold) throws IOException; }   // pins, scans, replays log tail privately
    interface GenerationPreparer { PreparedGeneration prepare(AdjacencyCommitLog.Record record, WorkingSet predecessor); }
    final class WorkingSet { base, generations, extraSelectedPredicates, contextCatalog, planeStatistics }  // what catchUpAndPublish accumulates today

In `TripleStore`, the listener becomes:

    interface DirectAdjacencyCommitListener {
        void stage(long nextRevision, Future<SealedDirectDelta> sealing);   // before mdb_txn_commit
        void confirm(long nextRevision);                                    // after MDB_SUCCESS, before dataRevision.set
        void retract(long nextRevision, boolean physicalCommitSucceeded);   // on failure; appends OVERFLOWED when true
    }

`TripleStore.beforePhysicalCommitForTest` (`TripleStore.java:522`) is a TripleStore hook outside this plan's scope and stays.

In the test tree, `SteppingApplierScheduler implements ApplierScheduler` with `step()`, `drain()` and `pendingUnits()`, and `DirectWorkerPool` runs submitted worker functions inline on `step()`.
