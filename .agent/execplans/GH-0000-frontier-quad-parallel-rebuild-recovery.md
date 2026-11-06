# Implement deterministic parallel quad and Frontier rebuilds with recovery

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Large LMDB stores currently rebuild the legacy quad synopsis and Frontier Statistics V2 on one thread even though
most hash and sketch accumulator updates are independent. After this change, one thread will continue to own each
ordered LMDB snapshot cursor, while bounded worker threads process fixed primitive pages into disjoint accumulator
partitions. Serial and parallel builds must publish byte-identical deterministic artifacts. If startup finds a corrupt
Frontier generation, the store must remove only Frontier-owned invalid state, roll back to a fully valid predecessor
when possible, request a replacement rebuild, and eventually publish a healthy generation instead of repeatedly
colliding with a retained corrupt manifest.

The behavior is visible through focused tests: serial and parallel builders produce identical shard and manifest
bytes; a corrupted current generation rolls back or resets without deleting foreign files; a restarted real LMDB
store schedules and publishes a replacement; and worker failure cancels and settles siblings before page storage is
reused.

## Progress

- [x] (2026-08-23 18:33Z) Verified the supplied architecture and patch checksum, branch base, and 27-file scope.
- [x] (2026-08-23 18:34Z) Completed the required root offline quick install with `BUILD SUCCESS`.
- [x] (2026-08-23 18:35Z) Reconstructed the exact `b5dbd93636` candidate in a temporary tree and audited conflicts.
- [x] (2026-08-23 18:44Z) Added executor, deterministic-build, recovery, and real-store lifecycle tests; captured the expected missing-API compile failure.
- [x] (2026-08-23 19:02Z) Integrated bounded store-owned execution and the page-parallel legacy quad synopsis path.
- [x] (2026-08-23 19:02Z) Integrated Vector-aware Frontier page processing, disjoint collectors, and shared budgets.
- [x] (2026-08-23 19:02Z) Integrated recovery state, planner invalidation, startup scheduling, and owned cleanup.
- [x] (2026-08-23 19:34Z) Formatted sources and passed the final seven-class focused gate: 100/0/0.
- [x] (2026-08-23 19:34Z) Ran the LMDB unit suite: 2,276 tests, with task-focused tests green and unrelated telemetry errors retained.

## Surprises & Discoveries

- Observation: the full candidate patch cannot be applied directly to the active branch.
  Evidence: `git apply --check` rejected `LmdbEstimatorRuntime`, `LmdbSailStore`, `FrontierCenterBuilder`,
  `FrontierCountMinMatrix`, `FrontierOmniBuilder`, `FrontierStatisticsBuilder`, and
  `FrontierStatisticsBuilderTest`; `LmdbBulkLoaderContractTest` does not exist. The merge base is
  `0d0c75a833`, with `338` patch-side commits and `5` active-branch commits.

- Observation: this branch has a newer batched Omni witness hash path that is not present in the candidate baseline.
  Evidence: current `FrontierStatisticsBuilder` feeds `FrontierStatisticsOmniBatcher`, while the candidate full-page
  implementation calls scalar `FrontierOmniBuilder.addWitnesses` for each row.

- Observation: the candidate checksum exactly matches the architecture document.
  Evidence: SHA-256 is `52f99e4891f373c316666b6c13a2bfb0128048989f9776848ca28c163a42ae15`.

- Observation: the adapted implementation compiles with every LMDB reactor dependency on the active branch.
  Evidence: the offline quick module install reported `LmdbStore SUCCESS` and `BUILD SUCCESS` in 15.433 seconds.

- Observation: the repository-wide copyright script descends into generated named-workspace build trees.
  Evidence: it reported `.mvnf/workspaces/*/build/**/META-INF/maven/**/pom.xml`; the scan was interrupted and the five
  new Java source headers were checked directly instead. Maven Spotless reported 563 LMDB Java sources clean and did
  not traverse `.mvnf`.

- Observation: broad LMDB unit verification is red outside the rebuild/recovery surface.
  Evidence: 2,276 tests completed with one temporary architecture failure introduced by this work and five
  `JoinMetricsTracking` overlap errors. The architecture failure was fixed by moving V2 revision composition into
  `LmdbEstimatorRevisionSupport`, after which `LmdbEstimatorArchitectureTest` passed 6/0/0. One telemetry error was
  rerun alone and reproduced 0/0/1 at the untouched query-evaluation boundary.

## Decision Log

- Decision: port behavior onto the active branch instead of force-applying or switching branches.
  Rationale: the user requested implementation in this checkout, and a partial application would omit required
  lifecycle and newer hashing behavior.
  Date/Author: 2026-08-23 / Codex.

- Decision: retain one cursor owner and use page barriers; do not parallelize LMDB cursor access.
  Rationale: LMDB transactions and cursors are thread-confined, while hashing and disjoint sketch mutation are safe
  to partition after rows have been copied into primitive page arrays.
  Date/Author: 2026-08-23 / Codex.

- Decision: preserve the active branch's batched/optional Vector API Omni hashing in the parallel page path.
  Rationale: the candidate predates that optimization. Replacing it with scalar per-row hashing would be a regression
  and would violate clean integration with existing optimizations.
  Date/Author: 2026-08-23 / Codex.

- Decision: translate the absent bulk-loader lifecycle test into `LmdbFrontierStoreLifecycleTest`.
  Rationale: this branch has no `org.eclipse.rdf4j.sail.lmdb.bulk` production or test package, while the existing real
  store lifecycle fixture can prove startup, corruption recovery, rebuild scheduling, publication, and clean restart.
  Date/Author: 2026-08-23 / Codex.

- Decision: make performance conclusions only from measurements on the final active-branch implementation.
  Rationale: the architecture records older JDK 26 measurements, but this checkout runs JDK 25 and contains a newer
  hashing pipeline. Correctness and resource-boundedness can be accepted without promising a fixed speedup.
  Date/Author: 2026-08-23 / Codex.

- Decision: do not expand this change into query-evaluation telemetry repair.
  Rationale: the deterministic `JoinMetricsTracking` failures occur in an untouched subsystem, reproduce in an exact
  method outside rebuild/recovery, and are not caused by worker lifetime, spill cleanup, or publication state.
  Date/Author: 2026-08-23 / Codex.

## Outcomes & Retrospective

Implementation and focused verification are complete. The active-branch adaptation retains Vector-aware hash
batching, bounds worker and temporary-resource ownership, produces byte-identical serial and parallel artifacts, and
repairs corrupt published generations without deleting foreign files. The final focused selection passed 100/0/0 in
`logs/mvnf/20260823-193249-verify.log`. The initial missing-executor failure remains retained in
`initial-evidence.frontier-rebuild.txt`.

The broad LMDB unit suite is honestly red: 2,276 tests completed with five deterministic query-evaluation telemetry
errors outside this plan's changed subsystem. The only broad failure introduced by this implementation, the enforced
700-line `LmdbEstimatorRuntime` cap, was fixed and rerun green. The two explicitly excluded Theme integration tests
were not run in the final verification. No end-to-end benchmark was run, so no speedup is claimed.

The final file audit is whitespace-clean, `LmdbEstimatorRuntime` is 698 lines, and the branch remains uncommitted as
requested. Pre-existing untracked artifacts were preserved. A concurrent formatting-only line wrap in
`FrontierOmniLayout.java` is visible in the shared worktree but is not part of this implementation's accounting.

## Context and Orientation

The active branch is `GH-0000-lmdb-predicate-guarantees` at `b6ce533a88996cbe34ae51f5c800b81ab8661f90`.
The supplied patch targets `b5dbd93636a34d3a629a871318d2df2c0f2203f3` on a divergent history. The LMDB module is
`core/sail/lmdb`.

`LmdbQuadSynopsisService` scans RDF `Statement` objects to build a legacy in-memory quad synopsis. A synopsis is
derived state: it can be rebuilt from LMDB and is not authoritative user data. `QuadSynopsisBuilder` owns Count-Min
tables, conditioned witnesses, and a bottom-k cold sample. Those structures can be updated concurrently only when
each task has exclusive ownership of the relevant structure or mask range.

`FrontierStatisticsBuilder` performs two primitive scans of explicit and inferred statement planes. Pass one builds
Count-Min, projected-distinct HLL, heavy-predicate, and Omni population state. Pass two builds heavy-predicate/object
state, Omni witnesses, and optional center samples. `FrontierOmniBuilder` and `FrontierCenterBuilder` spill fixed-width
events to external-sort collectors before writing immutable shard files. `FrontierStatisticsManifestStore` writes a
checksummed manifest and atomically switches `CURRENT.fs2`. `LmdbStatisticsService` maps the current generation and
serves query-time statistics.

`FrontierStatisticsOmniBatcher` and `FrontierStatisticsHashBatch` are newer active-branch components. They process
primitive batches and optionally dispatch to `jdk.incubator.vector` when that module is resolved. The new concurrent
page path must continue using their bit-exact hashes or equivalent batch functions.

`LmdbSailStore` owns lifecycle, background rebuild scheduling, and query-planning visibility. It will own one
`DerivedStateBuildExecutor`, pass it to the legacy synopsis and V2 builder, cancel scheduled rebuild work before
shutdown, and close the pool after consumers have settled.

## Plan of Work

First add the shared executor and progress sampler with tests for configuration resolution, deterministic result
ordering, daemon naming, failure propagation, cancellation settlement, interruption, ten-second sampling, and
overflow-safe telemetry. Add recovery tests to `LmdbStatisticsServiceTest` before changing recovery production code.
Add a deterministic serial-versus-parallel builder test that compares manifests and every shard byte, while retaining
all newer tests in the active branch.

Then add page-parallel legacy synopsis processing. Allocate reusable page arrays once per rebuild, keep the source
`Statement` cursor on the rebuild thread, hash disjoint row ranges in workers, call a rebuild observer from one ordered
worker only, and mutate disjoint Count-Min mask ranges plus separately owned witness and cold-sample structures. The
final partial page remains on the serial path. A failure must cancel siblings and wait until all tasks settle before
the arrays may be reused.

Next add Frontier page processing. Pass-one tasks own Count-Min ranges, projected-distinct component ranges, the heavy
predicate tracker, and contiguous Omni plane/lane ranges. Preserve component-hash reuse and batch hashing from the
active branch. Pass-two work uses one batched row-priority calculation per primitive page; heavy evidence, Omni
partition collectors, and center partition collectors then consume disjoint state. The serial tail continues through
`FrontierStatisticsOmniBatcher` so it remains vector-capable. Collector partitions finish in ascending logical range
order. Divide the existing sort-memory allowance rather than multiplying it, and share one synchronized temporary
disk reservation across all collectors.

Then implement recovery. Extend the file-operations boundary with bounded directory listing, no-follow directory
checks, and recursive deletion. The manifest store recognizes only `CURRENT.fs2`, its temporary pointer, manifest and
shard name grammars, manifest temporaries, and `omni-sort-*` directories as owned. It must preserve foreign files.
`LmdbStatisticsService` sets `rebuildRequired` after invalid-state cleanup or rollback and clears it only after a
successful publication. Memory-pressure fallback must preserve durable files. Startup scheduling and the normal
already-covered short circuit must consult this state. Planning revision must incorporate V2 availability, fallback,
generation, epoch, and sequence so a cached no-Frontier plan cannot survive successful recovery.

Finally add the real-store lifecycle reproduction to `LmdbFrontierStoreLifecycleTest`, run focused selectors, broaden
to the LMDB module, run copyright and formatting checks without formatting generated/evidence directories, and audit
the final diff for unrelated files and whitespace errors.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

The baseline command already completed successfully:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use the repository runner for focused tests, retaining logs:

    python3 .codex/skills/mvnf/scripts/mvnf.py DerivedStateBuildExecutorTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbQuadSynopsisServiceTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py FrontierStatisticsBuilderTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStatisticsServiceTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbFrontierStoreLifecycleTest --retain-logs

Run the relevant combined selector after the individual tests pass:

    python3 .codex/skills/mvnf/scripts/mvnf.py DerivedStateBuildExecutorTest,LmdbQuadSynopsisServiceTest,FrontierOmniExternalSorterTest,FrontierStatisticsBuilderTest,LmdbStatisticsServiceTest,LmdbFrontierStoreLifecycleTest --retain-logs

Run module verification without `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before formatting, run:

    cd scripts
    ./checkCopyrightPresent.sh

Then from the repository root run the project formatter and final audits:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    git diff --check
    git status --short --branch

The formatter command contains `-q` because it does not execute tests; test commands above never use `-q` or `-am`.

## Validation and Acceptance

`DerivedStateBuildExecutorTest` must show all tests passing, including a worker failure where a blocked sibling observes
interruption before the call returns. `LmdbQuadSynopsisServiceTest` must prove serial and parallel rebuild snapshots
are identical, observers remain ordered, and mutation drift does not publish a stale build.

`FrontierStatisticsBuilderTest` must build at least 8,193 rows so one full 8,192-row page and one tail row execute. A
one-worker build and a multi-worker build with a fixed clock must produce equal manifests and byte-identical shard and
manifest files. Progress observations must report exact cumulative visit counts across both passes.

`FrontierOmniExternalSorterTest` must prove multiple collectors share one aggregate temporary-disk budget and release
every reservation after success or failure. Recovery tests must prove corrupt current state rolls back to a fully
validated predecessor, requests a repair rebuild, permits reuse of the deterministic generation ID after invalid
files are removed, preserves foreign files, removes recognized interrupted sort directories, and does not request a
rebuild after a subsequent clean restart.

`LmdbFrontierStoreLifecycleTest` must prove a real store with a corrupted current mandatory shard starts, schedules a
replacement base build even when a READY predecessor is available, publishes a healthy generation, and reuses it on
the next clean restart. The combined selector and LMDB module run must have zero failures and zero errors attributable
to these changes. Any unrelated pre-existing broad-suite failures must be reported separately with exact reports.

No fixed speedup is an acceptance gate. Do not state that the final active-branch implementation is faster unless a
repeatable benchmark is run with matching serial and automatic worker configurations.

## Idempotence and Recovery

All source edits are additive or local and can be reapplied safely through Git. Do not use `git apply --reject` or a
partial three-way application in the working tree. If an implementation step fails, inspect the working diff and
continue from this plan; never reset or clean because the worktree contains user-owned untracked artifacts.

Recovery cleanup is intentionally grammar-bounded. Never recursively delete the statistics root or follow symbolic
links. Recursive deletion is allowed only for a recognized `omni-sort-*` directory reached by a no-follow directory
check. Failed collectors close their private directories and release shared reservations. Failed or stale builds do
not switch `CURRENT.fs2`; startup recovery either selects a validated predecessor or removes only owned derived state.

## Artifacts and Notes

The supplied patch is
`/Users/havardottestad/Downloads/rdf4j-frontier-quad-parallel-rebuild-and-frontier-recovery-from-b5dbd93636.patch`.
Its accompanying architecture document is
`/Users/havardottestad/Downloads/rdf4j-frontier-quad-parallel-rebuild-and-frontier-recovery-ARCHITECTURE.md`.
A reconstructed candidate tree exists under `/tmp/rdf4j-frontier-quad.9eYMto` for base/current/candidate comparison;
it is not an implementation source and must never overwrite tracked files wholesale.

The root baseline transcript is retained in `maven-build.log`. Existing untracked research, profiles, evidence files,
and generated test directories are user-owned and must remain untouched.

## Interfaces and Dependencies

Add `org.eclipse.rdf4j.sail.lmdb.DerivedStateBuildExecutor`, an `AutoCloseable` fixed platform-thread pool with
`create(String)`, `create(String,int)`, `configuredThreads()`, `effectiveParallelism(...)`, `invokeAll(...)`, and
`runAll(...)`. The system property is `rdf4j.lmdb.derivedStateBuildThreads`; absent means
`max(1, availableProcessors - 1)`, positive values are processor-capped, and invalid/non-positive values warn and use
the default. Worker results preserve submission order, checked `IOException` is unwrapped, siblings are cancelled on
failure, and every submitted state settles before return.

Add `DerivedStateBuildProgress`, which samples a monotonic clock at page barriers, emits at most once per ten seconds,
and calculates finite overflow-safe current rate, average rate, and ETA. Add
`FrontierStatisticsBuildObserver` as a non-authoritative telemetry callback; callback failure is logged and contained.

Extend `FrontierFileOps` with `list(Path)`, `isDirectory(Path)`, and `deleteRecursivelyIfExists(Path)` and implement
them in `NioFrontierFileOps` without following symbolic links. Add `FrontierTemporaryDiskReservation` as the shared
synchronized aggregate budget used by every Omni and center collector in one build.

Preserve existing public builder overloads by creating and closing a private executor. Add overloads that accept a
store-owned `DerivedStateBuildExecutor` and `FrontierStatisticsBuildObserver`. `SketchBasedJoinEstimator` likewise
keeps its existing constructor and gains an internal/shared-executor constructor. `LmdbStatisticsService` exposes
`rebuildRequired()` and clears the flag only in `publish(...)` after the new generation is mapped and selected.

Change note (2026-08-23): created the initial self-contained plan after verifying the supplied artifact and auditing
the active branch. The plan records the required Vector-aware merge and replacement lifecycle fixture because those
facts are not represented by the base-targeted patch.

Change note (2026-08-23 18:44Z): recorded the completed test-first milestone and retained the missing-executor compile
failure before beginning production integration.

Change note (2026-08-23 19:02Z): recorded completion of the implementation milestones and the successful active-branch
LMDB reactor compile gate before beginning focused verification.

Change note (2026-08-23 19:34Z): recorded formatting, final 100-test focused success, broad-suite attribution, and the
decision not to conflate an untouched telemetry defect with the rebuild/recovery implementation.

Change note (2026-08-23 19:35Z): recorded the final clean diff audit, architectural line count, preserved worktree
artifacts, and the unrelated concurrent formatting-only edit.
