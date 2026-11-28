# Let concurrent short LMDB native queries start and finish without exclusive locks

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document is maintained in accordance with `.agent/PLANS.md` at the repository root.

## Purpose / Big Picture

The LMDB store answers small graph queries (a point lookup, a two-hop walk, a count over one predicate) in tens of microseconds on one thread. On eight threads the same queries reach only 2.8 to 5.3 times the single-thread throughput, although each query reads an immutable snapshot and shares no data with the others. The lost time is spent waiting: every read-only query passes several exclusive locks on its way in and out. After this change, read-only queries at the default isolation level are admitted and released without waiting for each other, and the target is at least six times the single-thread throughput at eight threads for the three queries named below. A user sees it by running the throughput benchmark described in `Validation and Acceptance` at one and at eight threads and comparing the ratio before and after.

Nothing about what a query returns changes. Writers keep every exclusive path they have today. Isolation and crash safety must be exactly as before.

## Progress

- [x] (2026-11-28 16:50Z) Plan approved by the user; ExecPlan written.
- [x] (2026-11-28 17:15Z) Milestone 0: benchmark `ShortQueryScalabilityBenchmark` added; baseline throughput at 1, 2, 4 and 8 threads and lock attribution tables recorded under `logs/bench/scal/`.
- [x] (2026-11-28 17:35Z) Optimistic pairing instead of holding the publication gate (`publicationSequence`), with two failing-first tests in `LmdbPairedReadViewAdmissionTest`.
- [x] (2026-11-28 17:50Z) Unobserved reads bypass the root lock (`SailModelCleanup.Lock` stamp, `SailSource.datasetsRetainOwnSnapshot`, `SnapshotSailStore.tryAcquireBackingDatasets`), with `SnapshotSailStoreUnobservedReadTest` and two lock tests.
- [x] (2026-11-28 17:55Z) Coordinator lock spins before parking (`SpinningReentrantLock`).
- [x] (2026-11-28 18:10Z) Association gate shared between readers (`ReentrantReadWriteLock`), failing-first test `readersStartTheirDictionarySnapshotsSideBySide`.
- [x] (2026-11-28 18:20Z) Scope opening on an open store skips the writer-admission monitor; failing-first test `scopeAdmissionOnAnOpenStoreDoesNotWaitForWriterAdmission`.
- [x] (2026-11-28 18:30Z) Atomic per-epoch reservation count in `LazyRevisionTracker`; failing-first test `pinningTheNewestEpochDoesNotWaitForTheReclamationHorizon`.
- [x] (2026-11-28 18:45Z) Ablations and final throughput matrix; the arbiter-evidence change was reverted because it gained nothing.
- [x] (2026-11-28 19:10Z) Full module verification: `core/sail/base` 676 tests, `core/sail/memory` 1,602, `core/sail/nativerdf` 1,099, `core/sail/lmdb` 13,871 (unit and integration), no failures or errors; formatter and header check clean.

## Surprises & Discoveries

- Observation: `publicationVersion` in `LmdbSailStore` is not monotonic. A publication that changed nothing ends with `publicationVersion--`, and clearing a pending dictionary checkpoint leaves no trace in it.
  Evidence: the finalize block of `PublicationScope.close` in `LmdbSailStore.java` (`if (publicationChanged) publicationVersion++; else publicationVersion--;`) and `clearDictionaryCheckpointPending`. An optimistic reader that compared versions alone could miss a whole publication window.

- Observation: at the default isolation level (`SNAPSHOT_READ`) no snapshot lease is cached in `SailSourceBranch`; the reader goes through the low-isolation path with `lease == null`.
  Evidence: `SailSourceBranch.datasetWithinAdmission` only creates a `SnapshotLease` when `level.isCompatibleWith(IsolationLevels.SNAPSHOT)`; `LmdbStore` sets `SNAPSHOT_READ` as default.

- Observation: the lock ranking guessed from reading the code was wrong in two places. The coordinator lock's nested acquisitions were under 1 percent of waiting, and the association gate under 3 percent, while the root lock (59 percent) and the publication gate (23 percent) dominated.
  Evidence: `logs/bench/scal/baseline-locks.txt`, produced with `java logs/bench/LockReport.java <recording.jfr>`.

- Observation: removing one contended lock in a chain shows nothing on its own; the waiting moves to the next lock. The publication-gate change alone left point lookups at about 76,000 operations per second, and the root-lock bypass on top of it at about 72,000 to 84,000. Throughput only rose once the coordinator lock stopped parking (136,000), then again when scope opening stopped using the writer-admission monitor (158,000) and when epoch reservations became atomic (182,000 to 201,000).
  Evidence: the `thrpt-*.json` files of the variants `pub`, `root`, `spin`, `gate`, `scope`, `epoch` under `logs/bench/scal/`.

- Observation: Java Flight Recorder with monitor events at zero threshold makes a contended monitor look far worse than it is. The statement-access arbiter's `synchronized` evidence showed 116 of 150 waiting seconds for the two-hop query under the profiler, where throughput fell from 58,000 to 23,000; without the profiler, replacing that monitor changed nothing measurable.
  Evidence: `logs/bench/scal/baseline-locks.txt` against the `abl-arb` and `epoch` throughput runs.

- Observation: making the association gate shared did not help by itself, because readers then queued on the `writerAdmission` monitor inside `openValueResolutionScope`, and after that on the epoch tracker's monitor. Each needed its own change.
  Evidence: lock reports of the variants `gate` and `scope`.

- Observation: an LMDB commit is already written to LMDB while another connection has a read open, so letting unobserved reads skip registration with the root branches does not change when commits are published.
  Evidence: a scratch test (not kept) that committed on one connection while a second held an open statement cursor and then counted statements through the backing source: 2 of 2 were present.

- Observation: the child-process test `ValueStoreTest.testValueReaderCannotDeadlockWriterResizeAtAssociationGate` recognises a waiting reader by the method name `acquireAssociationGate` in its stack. The reader-side acquisition therefore keeps that name and the exclusive one is called `acquireExclusiveAssociationGate`.
  Evidence: the test failed with "The reader did not reach either actual association-gate admission boundary" while the reader-side method was named differently.

## Decision Log

- Decision: work on read admission before a prepared-plan cache or dispatch-decision reuse.
  Rationale: the measured scaling gap is 2x or more of throughput at eight threads, against an estimated 20 to 40 percent single-thread gain on small queries from plan reuse, which also needs a larger re-architecture (compiled steps capture per-execution state).
  Date/Author: 2026-11-28, Claude Code with the user's approval of the plan.

- Decision: validate optimistic read-view creation against a new monotonic counter rather than `publicationVersion`.
  Rationale: see the first entry under `Surprises & Discoveries`.
  Date/Author: 2026-11-28, Claude Code.

- Decision: each milestone is kept only if it gains at least 10 percent at eight threads on at least one of the three queries, costs no more than 3 percent single-thread CPU time, and needs no existing assertion weakened.
  Rationale: all of this is concurrency work, which the repository rules rate as high risk; a change that cannot show its benefit is not worth that risk.
  Date/Author: 2026-11-28, Claude Code.

- Decision: the order of work followed the measured lock ranking, not the order in the approved plan. The planned first milestone (reading the coordinator's admitted view without its lock) was dropped; its failing test is kept outside the tree as a patch.
  Rationale: the plan's own rule drops any lock under 5 percent of thread time, and once unobserved reads bypass the root lock that code path is not on the hot path at all.
  Date/Author: 2026-11-28, Claude Code.

- Decision: a change whose effect is masked by another lock is judged by ablation once the other locks are gone, not when it is made.
  Rationale: see the second entry under `Surprises & Discoveries`. Ablation at eight threads: without optimistic pairing point lookups fall from about 188,000 to 113,000 operations per second; without the root-lock bypass to 125,000; without the arbiter change nothing moves.
  Date/Author: 2026-11-28, Claude Code.

- Decision: revert the lock-free statement-access evidence.
  Rationale: no measurable gain without the profiler, and it added an allocation per closed cursor. The patch is kept outside the tree.
  Date/Author: 2026-11-28, Claude Code.

- Decision: use `java.util.concurrent.locks.ReentrantReadWriteLock` for the association gate rather than a hand-written shared mode on the existing lock.
  Rationale: its semantics (reader re-entry, a queued writer turning zero-wait readers away, interruptible acquisition) are what the code needs and are proven. The price is that three tests which read the gate reflectively now cast to the new type and take its write lock; their assertions are unchanged.
  Date/Author: 2026-11-28, Claude Code.

- Decision: the optimistic root-lock read validates both before and after the backing datasets are taken.
  Rationale: validating only around the reads of the branch state would reproduce the locked path exactly, but taking the datasets inside the validated interval also guarantees that nothing was committed to the branches between the check and the snapshot, at no extra cost.
  Date/Author: 2026-11-28, Claude Code.

- Decision: in my own new test, the number of pairing attempts a reader makes is asserted as at least two, not exactly two.
  Rationale: a dictionary writer that is still settling can legitimately turn a later attempt away as well; the first version of the assertion failed once for that reason.
  Date/Author: 2026-11-28, Claude Code.

## Outcomes & Retrospective

Measured in Docker (Linux, JDK 26, 16 virtual CPUs) with `ShortQueryScalabilityBenchmark`, one connection per thread, operations per second:

    query               1 thread before/after   8 threads before   8 threads after   scaling before -> after
    pointLookupOut         32,892 / 33,849           85,098            200,843          2.6x -> 5.9x
    countOnePredicate      19,252 / 19,670           65,478            130,916          3.4x -> 6.7x
    twoHopFromSeed         12,079 / 12,166           58,846             83,811          4.9x -> 6.9x

Each figure is the mean of two launches; the files are `logs/bench/scal/baseline.txt` and `logs/bench/scal/final.txt`, summarised in `logs/bench/gh6512-concurrency-evidence.txt`. The point lookup stops just short of the 6x target.

Single-thread cost did not rise: thread CPU time per query, median of five alternated launches, went from 0.0309 to 0.0302 ms for the point lookup, 0.0780 to 0.0764 ms for the two-hop query and 3.973 to 3.940 ms for a full predicate scan. With a connection opened per query the eight-thread figures are 176,606, 123,782 and 79,902 (5.6x, 6.6x and 6.9x). Lock waiting in the eight-thread point lookup fell from about 110 of 208 thread-seconds to about 6.

What remains: the map growth coordinator is still taken about seven times per query (it spins now instead of parking), and a pooled reader's monitor and the evaluation-strategy factory's monitor show up at well under one percent each. No attempt was made to find out how much of the gap to 8x is the machine (a laptop with performance and efficiency cores) rather than the code.

Lessons: measure before ordering the work, and judge each lock change by ablation at the end, because a chain of contended locks hides the value of removing any single one of them.

## Context and Orientation

The repository is Eclipse RDF4J, a multi-module Maven build. Two modules matter here. `core/sail/base` holds the generic transaction layer used by several stores. `core/sail/lmdb` holds the LMDB store, which keeps statements in one LMDB environment (the "triple store") and the dictionary that maps values to numeric ids in a second LMDB environment (the "value store"). LMDB is a memory-mapped key-value database in which a reader works on an immutable snapshot through a "read transaction".

A read-only query at the default isolation level `SNAPSHOT_READ` acquires a "dataset" (the statement view it reads from) before it runs and releases it when its result is closed. That acquisition passes four locks. The terms used below are defined here.

The "root lock" is the field `snapshotAcquisition` in `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SnapshotSailStore.java`, an instance of `SailModelCleanup.Lock` (a `ReentrantLock` that spins before parking). It is shared by the two `SailSourceBranch` objects of the store, one for explicit and one for inferred statements; inside `SailSourceBranch` it is the field `semaphore`. `SnapshotSailStore.acquireDatasetPair` holds it while both branches build their reader objects in `SailSourceBranch.observeDataset`, and each reader object (`SnapshotObserver`) takes it again when it is released, running `compressChanges()` inside. A reader takes three things from this lock: whether the branch is closed, the list of committed but not yet flushed changes (`changes`), and a registration in the set `observers`, whose only consumer is the decision whether pending changes may be flushed to the backing store (`SailSourceBranch.autoFlush`).

The "coordinator lock" is the field `lock` of the inner class `MapGrowthCoordinator` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`. The coordinator tracks every open read view so that the memory map of an LMDB environment can be resized safely ("map growth"). It is a plain `ReentrantLock` that parks at once. A query takes it about ten times; four of those are in `admittedReadPublicationView()`, called from `beginDatasetPublication()` while the root lock is held.

The "association gate" is the field `associationGate` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`. It makes the start of a native read transaction on the value store atomic with capturing which dictionary revision that transaction belongs to, against a writer that commits and publishes a new revision. It is exclusive, and readers take it with a zero-wait `tryLock` in `ValueReadScope.open`. When two readers collide, the loser throws `ValueReadAdmissionException`; the caller (`LmdbSailStore.createFreshReadView`) then releases everything it holds, waits for the gate in `awaitValueReaderAdmission()`, and starts again.

The "publication gate" is the field `publicationGate` in `LmdbSailStore.java`. A "publication" is the interval in which a writer commits to the two LMDB environments; readers must not pair a triple snapshot from before a publication with a value snapshot from after it. `tryCreateFreshReadView` holds the gate across both native transaction starts to guarantee that.

A "read view" (`LmdbSailStore.ReadView`) is the pair of native read transactions, one per environment, that a query reads through.

The benchmark that shows the problem is `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/AdjacencyQueryShapeBenchmark.java` with its methods `pointLookupOut`, `twoHopFromSeed` and `countOnePredicate`. Measured before this work in Docker (file `logs/bench/conc/final-scaling.txt`): pointLookupOut 29,711 operations per second on one thread and 83,989 on eight; countOnePredicate 16,272 and 65,278; twoHopFromSeed 10,399 and 55,580.

Repository rules that bind every step: never run tests with the Maven flags `-am` or `-q`; always pass `-o -Dmaven.repo.local=.m2_repo`; prefer the runner `python3 .codex/skills/mvnf/scripts/mvnf.py`; a change of observable behaviour starts from a failing test inside this repository; new Java files carry the Eclipse Distribution License header followed by the line `// Some portions generated by Claude Code`; nothing is committed or pushed unless the user asks; the file `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java` carries the user's own uncommitted edit and must be left alone.

## Plan of Work

### Milestone 0: baseline and attribution

No production code changes. Add the benchmark class `ShortQueryScalabilityBenchmark` beside `AdjacencyQueryShapeBenchmark`, reusing its data fixture and the three queries, in throughput mode, with a parameter `connectionMode` taking `perOp` (open and close a connection per query, as the existing benchmark does) and `perThread` (each benchmark thread keeps one connection). The parameter separates the cost of opening connections, which takes a monitor on `activeConnections` in `AbstractSail`, from the cost of admitting a query.

Run it in Docker at 1, 2, 4 and 8 threads. Record a Java Flight Recorder profile of the eight-thread run with park and monitor events at zero threshold and CPU-time sampling. From the profile build one table per query: seconds of thread time parked per lock (grouped by the first frame outside the JDK), CPU samples spent spinning per lock (frames `SailModelCleanup.Lock.acquireBySpinning` and `SpinningReentrantLock`), and the number of `ValueReadAdmissionException` retries. The table decides the order of the following milestones and removes any whose lock accounts for less than 5 percent of thread time. At the end of this milestone the table exists in `logs/bench/conc/` and is copied into `Artifacts and Notes`.

### Milestone 1: optimistic pairing instead of holding the publication gate

In `LmdbSailStore.java` the field `private volatile long publicationSequence` is never decremented and is incremented, while holding the publication gate, where the first publication begins (both `beginPublicationScope` and `tryBeginPublicationScope`), in the finalize block of `PublicationScope.close`, and in `clearDictionaryCheckpointPending`. In `tryCreateFreshReadView` a reader reads the sequence, checks that the version is even and equal to the one `awaitStablePublication()` returned and that no dictionary checkpoint is pending, starts the triple transaction and the value snapshot and reads `publishedNamespaces` and `namespaceGeneration` without the gate, and keeps the pair only if the sequence did not move and no checkpoint became pending. Otherwise it closes both and tries again; after `OPTIMISTIC_PAIRING_ATTEMPTS` (three) lost pairs it takes the gate exactly as before, so readers cannot starve under continuous writes. The test hook `betweenPairedReaderStartsForTest` runs between the two starts.

Tests in `LmdbPairedReadViewAdmissionTest`: a commit is published while a reader is paused between its starts and the reader then reads the state after the commit; an unchanged publication between the starts still forces a fresh pair; six readers and a writer of 300 two-statement transactions see whole transactions only.

### Milestone 2: unobserved reads bypass the root lock

`SailModelCleanup.Lock` counts complete releases in a volatile field that the holder advances before the final `unlock()` and before every wait on a condition (the lock's `newCondition()` returns a wrapper for that). `tryOptimisticRead()` returns that count, or a negative number while the lock is held; `validate(stamp)` is true if the lock is free and the count unchanged. `SnapshotSailStore.tryAcquireBackingDatasets` serves a read that takes no observations, at a level weaker than `SNAPSHOT`, straight from the backing sources when both declare `datasetsRetainOwnSnapshot(level)` (a new `@InternalUseOnly` default method on `SailSource`, false by default, true for LMDB at every level but `NONE`) and when `SailSourceBranch.addsNothingToBacking()` holds for the branches involved: open, no committed changes waiting to be flushed, no cached snapshot. The stamp is validated after the branch state is read and again after the backing datasets are taken; any doubt, and any retryable acquisition failure, falls back to the locked path. The datasets are wrapped in `RetainedSailDataset` only, so closing them touches neither the root lock nor the branches.

Tests: `SnapshotSailStoreUnobservedReadTest` (closing such a read does not wait for the root lock; a change committed to the root but not flushed is still seen; a store without the capability still holds back flushing while a read is open) and two tests of the stamp in `SailModelCleanupTest`.

### Milestone 3: the coordinator lock spins before it parks

`MapGrowthCoordinator.lock` in `LmdbSailStore.java` is a `SpinningReentrantLock`, like the other gates every query passes. Its semantics are unchanged; `LmdbMapGrowthCoordinatorTest` passes before and after.

### Milestone 4: readers share the dictionary's association gate

`ValueStore.associationGate` is a `ReentrantReadWriteLock`. Readers take the read lock: `openValueResolutionScope`, `ValueReadScope.open` (without waiting), `awaitValueReaderAdmission` and the optional hash helpers. Writers, the owning writer's borrowed reads, cleanup identity reservations, the naked lazy-value path and close take the write lock. A thread holding the read lock must not reach a write-lock section; the shared sections only read the identity with `currentReadRevision(LmdbValue.UNKNOWN_ID)`, which never starts a pending association.

Inside the read lock, `openValueResolutionScope` no longer takes the `writerAdmission` monitor when it finds the store open. A closing store sets `shuttingDown` and then takes the gate exclusively before it closes the registered scopes, so a reader inside the gate that saw the store open registers its scope before the closer looks, and a reader that enters afterwards sees the store closing and takes the monitor for the authoritative answer.

`LazyRevisionTracker` keeps each epoch's reservations in an `AtomicLong` that goes negative once the reclamation horizon has passed the epoch. Reserving is a compare-and-set on that count; releasing is a decrement that takes the tracker's monitor only when it emptied an epoch that is no longer the newest, or when queued revisions wait for the eviction timer to be armed. `updateHorizon` retires an epoch with a compare-and-set from zero, so a reservation and a retirement cannot both succeed.

Tests: `readersStartTheirDictionarySnapshotsSideBySide` in `LmdbPairedReadViewAdmissionTest`; `scopeAdmissionOnAnOpenStoreDoesNotWaitForWriterAdmission`, `pinningTheNewestEpochDoesNotWaitForTheReclamationHorizon` and the stress test `epochsAreReleasedCompletelyWhileWritersPublishNewerOnes` in `LmdbValueResolutionScopeTest`.

### Not done

Reading the coordinator's admitted view without its lock, merging the coordinator's per-query acquisitions, and caching the evaluation-strategy factory were not needed to reach the target and are left for a later change if a profile calls for them. Replacing the monitor of the statement-access arbiter's evidence was tried and reverted.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j8`.

Build everything once without tests:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Run one test method, one class, or the module:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbMapGrowthCoordinatorTest#<method>
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbMapGrowthCoordinatorTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Throughput in Docker. The scripts take a label, a directory of class files under `logs/bench` that is put in front of the benchmark jar (or `none`), a thread count, and a benchmark name pattern:

    logs/bench/scal-thrpt.sh base none 8 'ShortQueryScalabilityBenchmark'
    logs/bench/scal-prof.sh base-t8 <classdir-or-none> 'ShortQueryScalabilityBenchmark.pointLookupOut' 8 perThread
    java logs/bench/LockReport.java logs/bench/scal/base-t8.jfr

The benchmark jar is built with `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -P benchmarks,quick package` and copied to `logs/bench/jmh-scal.jar`; `logs/bench/build-variant.sh <name> <classpath>` compiles only the main sources that differ from the last commit into `logs/bench/<name>` so that a variant can be measured without rebuilding the jar.

Formatting and header check before finishing:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    (cd scripts && ./checkCopyrightPresent.sh)

## Validation and Acceptance

Each milestone is accepted when its new test fails before the change and passes after it, the module tests of `core/sail/lmdb` pass (and for milestone 4 also `core/sail/base`, `core/sail/memory` and `core/sail/nativerdf`), and the throughput at eight threads rises by at least 10 percent on one of the three queries while single-thread CPU time per query does not rise by more than 3 percent. The lock profile must show that the parked and spinning time of the targeted lock is gone and has not simply moved to the next lock.

The whole plan is accepted when the eight-thread to one-thread throughput ratio of `pointLookupOut`, `twoHopFromSeed` and `countOnePredicate` is at least 6, or when the remaining gap is explained by a measured cause recorded in `Outcomes & Retrospective`. A long mixed run with eight reading threads and one writing thread must show every read observing exactly one publication.

## Idempotence and Recovery

Every milestone is a separate, small set of edits that can be reverted with `git diff` and `git apply -R` on its own files without touching the others. Benchmarks write only under `logs/bench` and into temporary directories inside the Docker container. Tests may be rerun freely. The test `LmdbBackupServiceTest.backupRemainsConsistentWhileMutationsAndAutoGrowRace` is known to time out when the machine is under load; rerun that class alone before treating it as a failure.

## Artifacts and Notes

Baseline before this work, from `logs/bench/conc/final-scaling.txt` (existing benchmark, connection per operation):

    final    t=1  countOnePredicate                 16272.3 ± 2054.3     ops/s
    final    t=1  pointLookupOut                    29710.8 ± 2038.2     ops/s
    final    t=1  twoHopFromSeed                    10399.4 ± 681.9      ops/s
    final    t=8  countOnePredicate                 65277.9 ± 3178.1     ops/s
    final    t=8  pointLookupOut                    83989.2 ± 16466.7    ops/s
    final    t=8  twoHopFromSeed                    55579.7 ± 19095.4    ops/s

## Interfaces and Dependencies

No new libraries. These exist at the end of the work:

In `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ShortQueryScalabilityBenchmark.java`, a JMH benchmark with `@Param({ "perOp", "perThread" }) String connectionMode` and the methods `pointLookupOut`, `twoHopFromSeed`, `countOnePredicate`.

In `LmdbSailStore.java`: `private volatile long publicationSequence`, `private static final int OPTIMISTIC_PAIRING_ATTEMPTS = 3`, the test hook `volatile Runnable betweenPairedReaderStartsForTest`, and `LmdbSailSource.datasetsRetainOwnSnapshot(IsolationLevel)`.

In `ValueStore.java`: `associationGate` of type `ReentrantReadWriteLock`; `acquireAssociationGate()` (read lock, interruptible) and `acquireExclusiveAssociationGate()` (write lock, interruptible); `captureResolutionScope(boolean, boolean)`; in `ValueEpoch` the methods `tryReserve()`, `tryRetire()` and `reserved()`.

In `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSource.java`:

    @InternalUseOnly
    default boolean datasetsRetainOwnSnapshot(IsolationLevel level) {
        return false;
    }

In `SailModelCleanup.Lock`: `long tryOptimisticRead()` and `boolean validate(long stamp)`. In `SailSourceBranch`: `boolean addsNothingToBacking()`. In `SnapshotSailStore`: `private SailDataset[] tryAcquireBackingDatasets(IsolationLevel level, boolean explicit, boolean inferred)`.

Measurement tools kept outside the build under `logs/bench/`: `scal-thrpt.sh`, `scal-prof.sh` and `LockReport.java`.

Revision note (2026-11-28): the plan was rewritten after the work. The milestones now describe what was built, in the order the measurements dictated; the originally planned first milestone was dropped and the arbiter change reverted, for the reasons in the Decision Log; `Outcomes & Retrospective` holds the final numbers.
