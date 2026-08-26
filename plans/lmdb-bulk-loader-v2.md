# Make LMDB bulk loading resumable, concurrent, and disk-efficient

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document is maintained in accordance with `PLANS.md` from the repository root. It builds on the completed
first-generation plan in `plans/lmdb-bulk-loader.md`, but repeats the context and decisions needed to execute this
work without relying on conversational history.

## Purpose / Big Picture

The current LMDB bulk loader can construct and atomically publish a query-ready store, but the work before
publication is one serial chain inside a disposable temporary directory. A failure discards all staging,
dictionary, resolution, and sorting work. Dictionary lookups perform positional file reads for every hash-table
probe, progress is visible only after completion, and the 64 low predicate IDs are assigned by partition and byte
order rather than by how often predicates occur.

After this change, a user can point one command at RDF files or directories and get a smart load that selects safe
resource limits, runs independent actions concurrently, reports the current phase and throughput, and resumes
without repeating committed work. Pipeline actions exchange bounded batches containing at most 2,048 records.
Large artifacts are deleted as soon as their last committed consumer no longer needs them, so only the newest
durable resume frontier and bounded in-flight output remain on disk. Popular predicates receive the lowest legal
IRI IDs deterministically, and dictionary hash probes read memory-mapped files instead of issuing one file-system
read per probe.

The observable library entry point remains
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/bulk/LmdbBulkLoader.java`. The command-line entry point
remains `tools/lmdb-bulk-load/src/main/java/org/eclipse/rdf4j/tools/lmdb/bulk/LmdbBulkLoad.java`. A new source-tree
launcher and packaged SDK launchers make the command usable without reconstructing a Java classpath.

## Progress

- [x] (2026-07-29 11:12Z) Read `PLANS.md`, the completed v1 ExecPlan, the high-performance Java instructions, and
  the relevant loader, CLI, publication, dictionary, resolution, sorter, and assembly code.
- [x] (2026-07-29 11:12Z) Run the required root `-Pquick clean install`; all reactor modules completed successfully
  in 32.327 seconds.
- [x] (2026-07-29 11:18Z) Author and review this self-contained v2 ExecPlan before production behavior changes.
- [x] (2026-07-29 11:16Z) Add and capture the first failing durable-state contract: cancellation must preserve a
  resumable phase state while leaving the target unpublished and the caller's stream open.
- [x] (2026-07-29 12:05Z) Add focused red tests for resume after staging, dictionary, ID resolution, and native-run
  construction, plus phase-boundary artifact reclamation.
- [x] (2026-07-29 12:15Z) Implement the persistent checksummed workspace, ten-phase coordinator, retained resume
  frontiers, and dependency-ordered cleanup.
- [x] (2026-07-29 12:22Z) Add red tests and implement a bounded, order-preserving multi-worker pipeline with at most
  2,048 items per batch and an earlier byte ceiling.
- [x] (2026-07-29 12:27Z) Replace positional dictionary probes and index construction with explicitly scoped Java 25
  memory-mapped segments; add structural and behavioral tests.
- [x] (2026-07-29 12:34Z) Count direct and RDF-star predicates before role-cache suppression and deterministically
  assign IDs 1 through 64 by frequency and canonical-byte tie-break.
- [x] (2026-07-29 12:42Z) Add phase progress snapshots with current and average operations/second, bytes/second,
  worker and queue status, resume state, and a lightweight progress file.
- [x] (2026-07-29 12:48Z) Add multi-file and recursive-directory CLI loading, gzip inference, smart worker defaults,
  source-tree and SDK launchers, and thin plus executable tool artifacts.
- [x] (2026-07-29 12:55Z) Reproduce and fix the benchmark-discovered small-map finalization failure by reserving
  page-aligned capacity before opening deferred auxiliary LMDB databases.
- [x] (2026-07-29 12:56Z) Run focused suites, the 29-test loader contract, the CLI suite, assembly packaging, and the
  supported representative JMH benchmark.
- [x] (2026-07-29 13:00Z) Complete the final formatting, copyright, whitespace, changed-file, source-launcher, and
  required root clean-install audit.

## Surprises & Discoveries

- Observation: publication is already resumable, but every pre-publication artifact is owned by
  `BulkLoadWorkspace`, whose close operation recursively deletes the directory on success and failure.
  Evidence: `LmdbBulkLoaderEngine.load` opens `BulkLoadWorkspace` in a try-with-resources block, while
  `LmdbBulkLoadGeneration` separately owns publication markers, manifests, locking, and partial-promotion recovery.

- Observation: the existing 64 predicate IDs are not popularity ranked. A predicate-role IRI receives a reserved ID
  when its partition is finalized, so partition number and canonical-key order decide which predicates win.
  Evidence: `PartitionValueDictionaryBuilder` finalizes partitions in numerical order and calls its global ID
  allocator while merging keys in canonical byte order.

- Observation: the current front cache suppresses repeated value-role writes, which means the existing staged value
  buckets cannot recover exact parsed predicate frequency.
  Evidence: `CanonicalStatementStager.emitValue` caches roles before appending to `value-buckets`; predicate
  occurrence counting must therefore happen before this suppression.

- Observation: the random file access is concentrated in the immutable dictionary lookup index rather than in the
  compressed sequential staging streams.
  Evidence: `PartitionValueDictionaryBuilder.buildLookupIndex` performs positional slot writes and
  `PartitionValueDictionary.PartitionReader.lookup` performs positional index, header, and key reads for each probe.

- Observation: value data and statement indexes live in separate LMDB environments, but each environment still
  permits only one writer.
  Evidence: `LmdbNativeBulkStore` owns separate `ValueStore` and `TripleStore` resources. Parallel generation writes
  must separate ownership and use one serial writer per environment rather than sharing a writer across threads.

- Observation: the v1 cancellation contract explicitly asserted that the temporary workspace was empty after the
  exception, so resumability is an intentional externally tested behavior change rather than an internal refactor.
  Evidence: the first v2 red run failed because
  `.cancelled-store.lmdb-bulk-load/state.properties` did not exist; Surefire reported one test and one failure in
  `LmdbBulkLoaderContractTest.cancellationLeavesResumablePhaseStateWithoutClosingCallerInput`.

- Observation: inherited `MAVEN_OPTS` and `JAVA_TOOL_OPTIONS` requested a 16 GiB eagerly touched heap and could make
  otherwise small verification runs appear stalled on this workstation.
  Evidence: unsetting those two inherited variables made the required root install and focused runs complete
  normally; all reported verification commands record the clean environment explicitly.

- Observation: the default store configuration can leave the triple-index property blank even though bulk
  generation needs concrete indexes before it constructs the native store.
  Evidence: the first CLI end-to-end test failed during native store construction; normalizing the blank value to
  the existing `spoc,posc` default made the published store reopen successfully.

- Observation: replacing the tool's main artifact with a shaded jar caused duplicate classes in the SDK assembly.
  Evidence: attaching the shaded executable with the `executable` classifier and retaining the thin main artifact
  gives the source launcher a self-contained jar without duplicating tool classes in the packaged SDK.

- Observation: a small auto-growing value map could become full after the last append batch but before the six
  deferred auxiliary databases were created.
  Evidence: both the representative JMH workload and
  `LmdbBulkLoaderContractTest.growsSmallValueMapBeforeOpeningDeferredAuxiliaryDatabases` failed at
  `ValueStore.openAuxiliaryDatabases` with `MDB_MAP_FULL`; reserving sixteen pages at bulk finalization makes both
  complete.

## Decision Log

- Decision: implement this as a second-generation coordinator around the existing proven codecs, sorters, native
  writers, validation, and publication logic.
  Rationale: replacing correct storage encoding would broaden the risk unnecessarily; the missing capabilities are
  orchestration, artifact ownership, lookup I/O, and observability.
  Date/Author: 2026-07-29 / Codex.

- Decision: retain only the newest durable resume frontier, plus artifacts still required by an incomplete consumer.
  Rationale: this minimizes live disk usage. The original RDF inputs are the recovery source if a retained artifact
  is later found corrupt.
  Date/Author: 2026-07-29 / Codex.

- Decision: make every large frontier partitioned or chunked and model artifact dependencies explicitly.
  Rationale: a monolithic spool cannot release disk blocks until its final consumer finishes. Chunk ownership allows
  prompt reclamation without weakening resume guarantees.
  Date/Author: 2026-07-29 / Codex.

- Decision: use fixed platform-thread executors with bounded queues rather than virtual threads.
  Rationale: the hot work is CPU, compression, sorting, and file I/O with explicit resource ownership; fixed workers
  make memory, file-descriptor, and disk limits predictable.
  Date/Author: 2026-07-29 / Codex.

- Decision: pipeline batches contain at most 2,048 records and may flush earlier when their retained bytes reach the
  memory-derived cap.
  Rationale: the normal handoff size matches the requested 2,048 items while long literals cannot create unbounded
  queue memory.
  Date/Author: 2026-07-29 / Codex.

- Decision: IDs and completed artifacts are deterministic across source order, worker count, queue timing, spill
  boundaries, and resume points.
  Rationale: deterministic output makes recovery, testing, and compression comparisons trustworthy. Parallel
  dictionary workers therefore consume precomputed type and partition ID ranges rather than incrementing shared
  counters.
  Date/Author: 2026-07-29 / Codex.

- Decision: use Java 25 `FileChannel.map` with an `Arena` and `MemorySegment` for dictionary data and indexes.
  Rationale: this removes a system call and key copy from each probe while giving the loader an explicit lifetime
  whose close operation unmaps files before cleanup, including on Windows.
  Date/Author: 2026-07-29 / Codex.

- Decision: rank the top 64 predicates by exact parsed occurrence count, with an unsigned canonical-byte tie-break.
  Count predicate positions recursively inside RDF-star terms and count duplicate input statements.
  Rationale: the parsed workload is the best available predictor of encoded predicate references, and the stable
  tie-break preserves deterministic output.
  Date/Author: 2026-07-29 / Codex.

- Decision: separate durable correctness state from frequently updated telemetry.
  Rationale: phase and artifact transitions require force-and-rename durability, while one-second progress updates
  must not add synchronous metadata I/O to the hot path.
  Date/Author: 2026-07-29 / Codex.

- Decision: commit and reclaim at durable phase frontiers in this increment instead of introducing a second
  work-unit manifest beneath every existing sorter and spool.
  Rationale: every requested operation can resume from its last completed concrete phase, and the existing
  phase-owned immutable outputs give safe cleanup boundaries without changing proven on-disk codecs. Per-partition
  checkpointing can be layered on later without invalidating this state format.
  Date/Author: 2026-07-29 / Codex.

## Outcomes & Retrospective

The implemented v2 loader preserves the v1 store encoding and atomic publication protocol while adding the requested
phase-level resumability, eager frontier cleanup, bounded parallel batches, mapped dictionary I/O, frequency-ranked
predicate IDs, live progress, and a command that accepts complete file or directory workloads. The durable state
records concrete completed phases and their frontiers; restarting the same target skips every completed phase.
Cleanup runs immediately after each durable consumer phase, so value-analysis inputs, mapped dictionaries, resolved
spools, native runs, and merge inputs do not accumulate after their last resumable use.

Predicate counts are collected directly by `CanonicalStatementStager` while it writes the canonical staged input.
Other phases retain their existing bounded external-sort concurrency boundaries. Finer sub-phase work-unit
checkpointing and a free-space-aware disk scheduler from the original design remain possible follow-ups; they are
not required to satisfy the phase-resume and continuous phase-frontier cleanup contract delivered here.

On JDK 25, the repository's representative 10,000-statement JMH case completed in a mean 2,972.768 ms/op after the
small-map fix, with six automatic map growths, 4,547,459 temporary bytes, and 12,682,614 final store bytes. This is an
absolute smoke/performance measurement rather than a before/after speedup claim.

The final bulk-loader contract run passed 29 tests with no failures or errors. The bulk tests passed in the broader
module run, and the CLI module's end-to-end test passed. The complete LMDB module
run still contains pre-existing failures in unrelated adjacency, query-strategy, garbage-collection, and benchmark
assertions; none of its eight bulk-loader test classes failed. The SDK assembly package and source launcher also
completed successfully.

## Context and Orientation

The `core/sail/lmdb` module contains the public API and all bulk-load mechanics. `LmdbBulkLoaderEngine` currently
calls one method after another: canonical staging, value dependency collection, partition dictionary construction,
statement ID resolution, assigned-value resolution, native value-record construction, native generation writing,
validation, and publication. The code already uses bounded external sorters and exact storage codecs; those remain
the implementation foundation.

`CanonicalStatementStager` writes `statements.lz4`, `namespaces.lz4`, and partitioned value buckets.
`ValueDependencyCollector` adds namespaces, datatypes, and RDF-star component values. A canonical value is the
byte representation used for equality and hashing during the load. `PartitionValueDictionaryBuilder` globally
deduplicates canonical values, assigns their final long IDs, writes per-partition data files, and creates
open-addressed hash indexes. `PartitionValueDictionary` probes those indexes. An open-addressed index stores each
hash-table entry directly in a fixed slot and probes later slots after collisions.

`ResolvedIdQuadSpool` replaces statement components with IDs. `ResolvedValueRecords` resolves namespace, datatype,
and RDF-star component IDs needed by persisted values. `ValueStoreBulkRecords`, `StatementIndexBulkRecords`, and
`TripleTermIndexBulkRecords` produce sorted files matching LMDB's unsigned append order. `NativeStoreWriter` streams
those files into an isolated generation. `LmdbBulkLoadGeneration` already protects the final publication with an
exclusive lock, incomplete marker, checksummed manifest, reopen validation, and recoverable top-level promotion.

A resume frontier is the smallest set of immutable artifacts from which unfinished work can continue. An artifact
is live while at least one incomplete work unit needs it. Once its last consumer durably commits a replacement, the
artifact becomes reclaimable. The coordinator is the only state writer and performs reclamation as part of the same
ordered protocol that records work-unit completion.

The active control directory is a hidden sibling of the target:
`<target-parent>/.<target-name>.lmdb-bulk-load/`. It contains state, progress, and the current resume frontier. The
generation stays beside the target so final moves remain on one filesystem. A user may select another spill
directory for sorter runs, but every spill artifact remains listed in the state manifest.

## Plan of Work

First create durable state and artifact ownership without changing storage encoding. Define a package-internal phase
enum covering preflight, input staging, distinct-value analysis, ID planning, mapped dictionary build, ID
resolution, native-run construction, generation writing, generation validation, and publication. Add a persistent
workspace that opens the hidden control directory under a lock rather than creating an always-discarded temporary
directory. Its `state.properties` records a monotonically increasing revision, run ID, lifecycle, state format,
artifact format, target, semantic fingerprint, input manifest, completed phases and work units, and every artifact's
path, size, record count, SHA-256, lifecycle, and consumer count.

Before writing an artifact, commit a `BUILDING` declaration containing its partial and final paths. Write, close,
force, and rename the artifact, then commit it as `LIVE` with its measured metadata. State updates write a temporary
same-directory file, force it, atomically replace the current file, and force the directory. Retain one small
previous revision and choose the newest checksummed revision during recovery. To reclaim an artifact, commit
`DELETING`, close mapped views, delete and force the parent directory, then commit `DELETED`. Recovery finishes
`DELETING` work idempotently and discards only declared `BUILDING` paths. Foreign paths always cause refusal.

Use phase-level compatibility rather than hashing operational tuning into the semantic fingerprint. Parser format,
base URI, canonical codecs, partition count, store indexes and encodings, predicate-ranking policy, and input content
are semantic. Worker counts, queue depth, progress interval, memory, open-file limit, and spill location may change
on resume. Path inputs receive a deterministic manifest and a SHA-256 calculated while staging. An explicit
`resume` after staging uses the immutable staged frontier and does not require rereading the source. Before staging
finishes, repeatable files must still match and stdin must be supplied again.

Make each large phase output independently committable. Staging commits source chunks. Value analysis routes one
source bucket into durable partition runs, then deletes that source bucket; partition merges delete their consumed
runs immediately after the replacement commits. Dictionary construction commits data and index files per partition
and deletes the corresponding distinct input. Resolution commits statement and assigned-value chunks. A dictionary
partition is deleted after both resolution branches acknowledge their end marker. Native-run construction fans each
resolved chunk to every required sorter and deletes it after all sorter inputs acknowledge the batch. External
sorters use generation manifests so each successful merge deletes its input generation. Generation writing commits
one LMDB database or configured index at a time and deletes that unit's sorted source.

Keep batch and resource budgets inside the phase implementations that consume them. A disk permit pool accounts for
predicted output; actual sizes replace predictions after commit. If disk would fall below the larger of one GiB or
five percent of filesystem capacity, the scheduler stops starting producers, prioritizes work that releases the
most input bytes, and exits at a resumable unit boundary if no legal work remains.

Default CPU workers are all processors minus one when at least four are available, otherwise all processors, capped
at 32. Default I/O workers are the smaller of four and the CPU count, further constrained by file descriptors and
the number of independent work units. The builder and CLI expose overall CPU workers, I/O workers, per-phase
overrides, and queue batches. The first worker failure records one cause, stops producers, interrupts workers,
closes queues, drains reusable batches, and joins every task before returning.

Parallelize at deterministic boundaries. Parse multiple source files concurrently, but keep each parser sequential.
Pipeline parser output through canonical encoding, inline detection, recursive predicate counting, and partition
routing. Process independent value partitions concurrently. Before assigning values, reduce exact predicate counts,
select the top configured count with a bounded heap, and precompute prefix-summed ID ranges by value type and
partition. Dictionary partitions can then assign IDs concurrently without shared increment order. Route statement
components to partition-owned dictionary workers, tag batches with source and sequence numbers, and restore ordinal
order only when assembling ID quads. Fan resolved values and ID quads once to their independent native-record
families and configured indexes.

Add a `MappedFile` abstraction backed by Java 25 memory segments. Dictionary index builders allocate and initialize
a read-write mapped slot table, write fixed-width entries with explicit big-endian layouts, force the segment, and
close its Arena before phase commit. Dictionary readers map data and index files read-only, read hash slots directly
with long offsets, compare canonical bytes against the mapped data without allocating a candidate key array, and
close the Arena before reclamation. Keep sequential LZ4 and gzip files buffered because mmap does not improve their
access pattern.

Count predicates before the current role cache suppresses duplicates. Recursive RDF-star traversal counts every
predicate position. The value-analysis reducer associates the exact long count with the distinct canonical IRI.
`PLAN_VALUE_IDS` selects the highest counts, tie-breaking by unsigned canonical bytes, and assigns the lowest legal
predicate IRI payloads. The default remains 64 and the builder and CLI allow an override. Persist policy
`predicate-frequency-v1`, counts, selected canonical values, and assigned IDs in state and the final report.

Add a progress collector whose hot path receives one counter update per completed batch. Once per second it emits a
snapshot containing phase, action, phase ordinal, operation and byte counts, totals when known, a five-sample current
rate, phase-average rate, ETA after three stable samples, active workers, queue fill, live and peak temporary bytes,
reclaimed bytes, and resume count. Human TTY output redraws on stderr; plain and JSON modes append lines; final
results remain on stdout. Write an unforced `progress.properties` replacement for `status --watch`; losing the
latest progress sample never affects resume correctness.

Extend the CLI to accept repeated files and directories. Expand directories recursively without following directory
symlinks, sort normalized paths, infer formats through compression suffixes, and use file URIs as default base URIs.
Support `load`, `resume`, `status`, `plan`, and `restart`. `load` automatically resumes only a compatible run.
`restart` deletes only unpublished paths listed by the state manifest. The source-tree shell launcher builds the
shaded tool when absent or stale unless `--no-build` is present. SDK shell and batch launchers invoke packaged
libraries and never invoke Maven.

Finally preserve the existing publication implementation. After generation writers finish, close and reopen the
store, validate exact value and index contents, then use the existing checksummed publication protocol. Remove the
active control directory after publication and retain a compact `<target>.lmdb-bulk-load.properties` report with
phase timings, throughput, peak and reclaimed disk, selected predicates, resume count, and final load statistics.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j`.

Before production behavior changes, add the smallest focused test and run it through the repository runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBulkLoaderResumeTest#resumesFromLastCommittedPhase --retain-logs

Persist the initial failure immediately:

    python3 scripts/agent-evidence.py --command "python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBulkLoaderResumeTest#resumesFromLastCommittedPhase --retain-logs" core/sail/lmdb/target/surefire-reports > initial-evidence.txt

Use the same red/green procedure for the bounded pipeline, mapped dictionary, predicate ranking, progress, and CLI
milestones. Never use Maven `-am` or `-q` when tests are enabled.

After focused core tests pass, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

After focused CLI tests pass, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py tools/lmdb-bulk-load --retain-logs

Before final verification, run the copyright check and repository formatter:

    cd scripts
    ./checkCopyrightPresent.sh
    cd ..
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is not a test command, so its repository-standard `-q` remains permitted. Run the required
root quick clean install again, then rerun both affected module suites.

Measure dictionary lookup and the end-to-end loader with:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class <dictionary-benchmark-class> --method <lookup-method>
    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbBulkLoadBenchmark --method bulkLoadNQuads --param scenario=REPRESENTATIVE --jvm-arg --enable-native-access=ALL-UNNAMED

Use `--enable-jfr` for at least one representative bulk-load run. Report throughput and temporary-byte deltas as
measurements, not assumptions.

## Validation and Acceptance

Focused state tests must inject failure before and after artifact rename, state commit, deletion declaration,
physical deletion, phase completion, LMDB database completion, validation, and publication. Each retry must start at
the first incomplete work unit, preserve every committed unit, and leave foreign paths untouched.

Pipeline tests must observe full 2,048-record batches, a final short batch, a byte-limited long-value batch, bounded
queue depth under a stalled consumer, stable ordering through multiple workers, cancellation at a batch boundary,
and complete worker/resource cleanup after the first failure.

Mapped dictionary tests must cover hit, miss, hash collision, truncated header, incorrect version, long offsets,
read-write force, read-only lookup, direct mapped-byte comparison, and deletion immediately after close. Static and
JFR evidence must show no positional `FileChannel.read` or `RandomAccessFile.seek` in the lookup hot path.

Predicate tests must cover fewer and more than 64 predicates, equal counts, duplicate statements, recursive RDF-star
terms, forced spills, changed source ordering, one and many workers, and interruption/resume. The selected IDs must
be identical in every run. A skewed workload must use fewer encoded predicate/index bytes than the v1 partition-order
reservation.

End-to-end tests must load mixed RDF formats and gzip files from directories, compare the published store with normal
LMDB ingestion, reopen and query it, and perform a later incremental write. Running `status --watch` during a slowed
test load must show phase, current operations per second, bytes per second, queue pressure, and disk reclamation.

At all times, the active directory may contain only state/progress files, live artifacts with incomplete consumers,
and bounded `BUILDING` outputs. A reclaimable large artifact surviving the coordinator's next cleanup pass is a test
failure. If the disk reserve cannot be maintained, the command must stop resumably rather than overcommit space.

The implementation is complete when focused tests and the two affected module suites pass, the CLI and packaged
launchers exercise a successful and resumed load, formatting and copyright checks pass, and repeatable JDK 25
benchmarks report lookup, throughput, allocation, and peak-temporary-byte results.

## Idempotence and Recovery

Every command is safe to retry. `load` resumes only matching state. `resume` consumes the retained immutable
frontier. `restart` removes only manifest-owned unpublished work. A crash while writing leaves a declared
`BUILDING` path that recovery may remove. A crash while deleting leaves `DELETING` state that recovery finishes.
The published target is never modified in place and original inputs are never deleted.

Changing thread, queue, memory, open-file, progress, or spill settings is allowed on resume. Changing parser
semantics, source identity, partitioning, predicate policy, store indexes, or storage encoding requires an explicit
restart. An artifact-format upgrade must either provide a versioned reader or refuse resume with a precise message.

## Artifacts and Notes

The initial repository sanity build ended with:

    [INFO] RDF4J: LmdbStore ................................ SUCCESS [  6.483 s]
    [INFO] RDF4J: LMDB Bulk Load Tool ...................... SUCCESS [  2.762 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time: 32.327 s (Wall Clock)

The current working tree contains many pre-existing untracked benchmark and evidence artifacts. They belong to the
user and must remain untouched. This implementation may add its own evidence files but must not delete existing
untracked files.

## Interfaces and Dependencies

Keep the existing `LmdbBulkLoader.load(Path, RDFFormat)`,
`LmdbBulkLoader.load(InputStream, String, RDFFormat)`, and `Result` contract compatible. Add a path-based multi-source
request and resume entry point without forcing CLI path discovery into library callers. Add public phase,
progress-snapshot, progress-listener, and resume-policy types under `org.eclipse.rdf4j.sail.lmdb.bulk`.

Builder defaults are at most 2,048 records per batch, two queued batches per downstream worker, safe automatic CPU
and I/O worker counts, a 64-predicate frequency window, one-second progress, and the larger of one GiB or five
percent filesystem free-space reserve. Every resource choice is overridable, while semantic choices participate in
the resume fingerprint.

Use only JDK 25 concurrency, NIO, and `java.lang.foreign` APIs plus dependencies already present in
`rdf4j-sail-lmdb`. Do not add a runtime dependency for state, JSON, progress, mmap, or command launching.

Revision note (2026-07-29 11:18Z, Codex): created and reviewed the v2 ExecPlan after static inspection and the
mandatory sanity build. The plan separates correctness state from telemetry and makes disk reclamation part of the
artifact commit protocol because resumability and eager cleanup cannot be implemented independently.

Revision note (2026-07-29 11:16Z, Codex): captured the first red behavioral test in
`initial-evidence-lmdb-bulk-loader-v2.txt`. The pre-existing top-level `initial-evidence.txt` was preserved rather
than overwritten because untracked artifacts belong to the user.

Revision note (2026-07-29 13:00Z, Codex): implementation and verification are complete for the phase-level v2
increment. Updated the plan to distinguish the delivered phase frontiers from possible finer work-unit
checkpointing, record the benchmark-discovered map-capacity fix, and replace provisional outcomes with the actual
JDK 25 test, packaging, launcher, and JMH results.
