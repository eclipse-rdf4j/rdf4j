# Move Frontier planning to a bounded mapped index

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

LMDB's authoritative Frontier estimator currently reads and verifies every persisted synopsis record while preparing
each query. The complete theme dataset has a roughly 267 MiB payload, so query planning spends about 550 ms reading
Frontier evidence even when actual query execution takes only a few milliseconds. After this work, repository startup
or a background maintenance task derives a compact memory-mapped query index from each immutable Frontier payload.
Query planning searches only the smallest relevant primitive range and either finishes within a deterministic work
budget or immediately uses the existing scalar estimator.

The observable result is that the complete `ThemeQueryPlanRunBenchmark.planQuery` corpus has Frontier preparation p95
at or below 5 ms and no preparation above 10 ms. No query thread reads payload blocks, verifies payload checksums, or
rebuilds the synopsis. Query answers and estimate semantics remain unchanged.

## Progress

- [x] (2026-07-26 11:53Z) Ran the required root quick clean install; all reactor modules succeeded.
- [x] (2026-07-26 11:55Z) Recorded the mapped-index design and acceptance criteria in this ExecPlan.
- [x] (2026-07-26 11:57Z) Added and captured the failing real-rebuild sidecar publication test.
- [x] (2026-07-26 12:13Z) Added sidecar format, lookup, corruption, no-source-open, and lease tests.
- [x] (2026-07-26 12:13Z) Implemented deterministic sidecar build, validation, mapping, and primitive lookup.
- [x] (2026-07-26 12:13Z) Added service-owned active-generation leases and safe retirement.
- [x] (2026-07-26 12:11Z) Added configuration and deterministic whole-session work preflight.
- [x] (2026-07-26 12:11Z) Replaced query-time payload scans with indexed materialization.
- [x] (2026-07-26 12:11Z) Moved dirty-state rebuilds to coalesced background maintenance.
- [ ] Run focused and LMDB module verification.
- [ ] Run planning and complete-theme benchmarks plus allocation profiling.

## Surprises & Discoveries

- Observation: Only 265,847 of 2,916,113 records in the current complete-theme payload are eligible for initial query
  materialization. A structure-of-arrays index with four integer row permutations is approximately 15 MiB rather than
  the 267 MiB source payload.
  Evidence: A read-only parser counted 9.12% eligible records in the 280,152,096-byte payload.

- Observation: `LmdbFrontierPackedCostSession.prepare()` scans the source twice, first to count and then to emit, and
  its record matching allocates temporary `long[]` instances.
  Evidence: Calls to `LmdbFrontierSynopsisService.scanSnapshot` are in the count and write phases of that method.

- Observation: The LMDB module's Spotless lifecycle rewrites an unrelated pre-existing indentation inconsistency in
  `ThemeQueryBenchmark` during every focused verify.
  Evidence: The first `mvnf` reproduction changed five parameter lines. The unrelated change was removed with a
  narrow patch and must continue to be excluded from this work.

- Observation: The first mapped-path integration run passed 29 of 31 existing cases. Its only failures were the
  intentionally changed lazy-rebuild expectation and an accidentally generalized disabled-budget fallback reason.
  Evidence: After retaining `persistent_disabled_zero_budget` and updating the insertion lifecycle assertion, all 32
  planning integration tests passed.

## Decision Log

- Decision: Keep the query sidecar derived and outside the authoritative manifest.
  Rationale: The manifest remains the sole selector of valid evidence. A missing, stale, or corrupt query index can be
  rebuilt or ignored without changing synopsis correctness or requiring a manifest migration.
  Date/Author: 2026-07-26 / Codex

- Decision: Use primitive columns and four sorted row-ID permutations rather than a boxed multi-map.
  Rationale: The workload is immutable and lookup-heavy; sort-once plus binary search changes preparation from a full
  payload scan to a bounded candidate-range scan while keeping allocation out of the per-row loop.
  Date/Author: 2026-07-26 / Codex

- Decision: Use deterministic work units, not elapsed time, for query fallback.
  Rationale: Wall-clock cutoffs make query plans depend on machine load. Candidate visits can be predicted before
  allocation and preserve deterministic planning.
  Date/Author: 2026-07-26 / Codex

- Decision: Queries fall back to scalar estimates whenever the mapped view is absent, stale, rebuilding, or over
  budget; rebuilding never occurs on the query thread.
  Rationale: Fast fallback is preferable to accepting an unbounded maintenance latency in every query benchmark.
  Date/Author: 2026-07-26 / Codex

- Decision: Retain the existing `persistent_disabled_zero_budget` reason when persistent Frontier evidence is
  explicitly disabled, while using the new query-index reasons for mapped-index availability and work limits.
  Rationale: Disabled persistence is not an index failure, and existing explanation consumers already distinguish it.
  Date/Author: 2026-07-26 / Codex

## Outcomes & Retrospective

Implementation is in progress. The initial build is green and the design is grounded in the current payload shape.

## Context and Orientation

The work is in `core/sail/lmdb`. `LmdbFrontierSynopsisService` owns the persisted Frontier manifest and payload
generations. A generation is immutable after atomic publication. `LmdbFrontierPackedCostSession` converts those
records into a query-local `FrontierStateArena`, which the estimator uses while costing query plans.
`LmdbSailStore` owns the service and an existing single-threaded `estimatorPersistExec` executor suitable for
coalesced maintenance. `ValueStoreHashFile` demonstrates this module's JDK Foreign Function and Memory API pattern:
read-only `MemorySegment` mappings are owned by an `Arena` that closes when the mapping is retired.

A query sidecar is a derived file whose header identifies its source payload. It contains only records used for
initial statement-pattern leaves: design lane zero in the subject-to-object direction. Each row has primitive
subject, predicate, object, context, and weight columns. Four arrays contain row IDs ordered independently by subject,
predicate, object, and context. Binary search finds the equal range for every bound component; query preparation scans
the shortest range and verifies the remaining constants and repeated-variable equalities.

The source manifest format and payload format remain authoritative and unchanged. Sidecar failure must never turn a
valid synopsis into corrupt evidence.

## Plan of Work

First add tests in the Frontier test package for a standalone query-index format. Cover deterministic bytes,
source-identity validation, truncated and corrupt files, all four component indexes, unbound scans, remaining-field
verification, repeated variables, context semantics, and lease retirement. Run the smallest new test selection and
record the expected failures before adding production code.

Implement `FrontierQueryIndex` and its builder in the Frontier package. The file header contains a magic number,
format version, source kind and generation, source byte length and SHA-256, snapshot identity, row count, section
offsets, and a whole-sidecar checksum. The builder streams the source once into primitive columns, sorts reusable
primitive row IDs for each component with deterministic row-ID tie-breaking, writes a temporary file, forces it,
validates it, atomically renames it, and forces the containing directory. The reader validates all lengths and
identities before mapping read-only segments with a shared arena and loading their pages. A failed build leaves the
old active view usable or makes the index unavailable; it never modifies the manifest.

Extend `LmdbFrontierSynopsisService` with `acquireQueryIndex(long requiredEpoch)`. The active view combines the mapped
base and insertion generations. Acquisition reads the active reference, retains it, rechecks status and epoch, and
returns a closeable lease without a monitor on the successful path. Publication atomically swaps views. The service
owns one reference; retired mappings close only after their final query lease closes. Service startup builds or
rebuilds missing and invalid sidecars synchronously, while insertion publication builds the corresponding small
sidecar before exposing the generation.

Add `frontierInitialMaterializationWorkUnits` to LMDB configuration, the RDF schema, store/runtime plumbing, parse and
export behavior, and configuration tests. Its default is 262,144 candidate visits; values must be nonnegative and zero
means that initial Frontier materialization is disabled.

Replace both `scanSnapshot` passes in `LmdbFrontierPackedCostSession.prepare()` with the leased index. Construct one
primitive selector per eligible statement pattern. Resolve stored constants once, binary-search all bound columns,
select the smallest range, and compute the predicted count-plus-emission visits for the complete session. If it
exceeds the configured work units or the existing query-memory budget, close the lease and use scalar estimates for
the whole session. Otherwise count and emit directly into the existing query-local arena with counted loops and no
per-row allocations. Close the lease when the session closes. Keep `scanSnapshot` only for persistence tests and
diagnostics.

Finally, change dirty insertion and deletion handling in `LmdbSailStore`. Coalesce one task on
`estimatorPersistExec`; under `sinkStoreAccessLock`, and only with no active transaction, rebuild the payload and its
sidecar and publish the new view. During dirty or rebuilding states acquisition reports an unavailable index and
queries immediately fall back. Rollback retains the prior epoch-compatible view. Stable diagnostic reasons are
`query_index_unavailable`, `query_index_epoch_mismatch`, `query_index_work_budget_exhausted`, and
`query_index_build_failed`.

## Concrete Steps

Run commands from the repository root. The required initial command has completed successfully:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

Use the repository test runner for focused work, retaining logs for evidence:

    python3 .codex/skills/mvnf/scripts/mvnf.py FrontierQueryIndexTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbFrontierSynopsisServiceTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbFrontierPlanningIntegrationTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before final verification, run the repository copyright check and formatter, then rerun the matching focused and
module tests. Do not use Maven `-am` or `-q` for tests.

Run planning performance separately:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryPlanRunBenchmark \
      --method planQuery --warmup-iterations 3 --measurement-iterations 10 --forks 2

Then run the complete `ThemeQueryBenchmark.executeQuery` suite through the repository benchmark harness and compare
the result with `results-2026-07-26.md`. Capture CPU and allocation evidence with the repository's async-profiler
skill if either latency threshold is missed.

## Validation and Acceptance

Focused tests must prove deterministic format output, identity rejection, corrupt/truncated recovery, atomic
publication, lookup semantics, insertion and rollback behavior, dirty-state fallback, background recovery, and
lease-safe generation swaps. The planning integration suite must retain all existing result and estimate assertions.
After index initialization, an instrumented query acquisition and preparation must perform zero source-payload input
opens and zero `scanSnapshot` calls.

Across the complete theme planning corpus, Frontier preparation p95 must be at most 5 ms and the maximum at most
10 ms. Work predicted above the configured cap must fall back before arena allocation. CPU and allocation profiles
must show no `FrontierPayloadBlockReader`, payload SHA/CRC work, synopsis rebuild, or per-record object/array allocation
on query threads. Validate primarily on the repository JDK 25 baseline and repeat the final benchmark with the user's
Zulu JDK 26.

If either latency threshold is missed and profiling attributes more than 30% of Frontier preparation to copying
selected rows into query-local state, add a borrowed immutable payload adapter to `FrontierStateArena`, backed by the
query-index lease; transforms that need mutation still materialize query-local outputs. Do not add this more invasive
zero-copy path when profiling identifies another bottleneck.

## Idempotence and Recovery

Sidecar builds use unique temporary files and atomic replacement, so rerunning startup or rebuild is safe. Orphan
temporary files and invalid derived sidecars may be ignored and replaced. Do not delete or rewrite authoritative
manifest or payload generations during recovery. Existing untracked repository artifacts belong to the user and must
remain untouched.

## Artifacts and Notes

Initial build evidence:

    [INFO] RDF4J: LmdbStore ............................... SUCCESS [  5.219 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  32.506 s (Wall Clock)

## Interfaces and Dependencies

Use only JDK 25 APIs and dependencies already present in the LMDB module. Add internal Frontier types representing the
mapped query index, primitive selector, immutable active view, and closeable lease. The service method is:

    FrontierQueryIndexLease acquireQueryIndex(long requiredSnapshotEpoch)

The lease reports availability or one stable fallback reason, exposes immutable base and insertion index segments,
and closes idempotently. The configuration surface adds:

    long getFrontierInitialMaterializationWorkUnits()
    LmdbStoreConfig setFrontierInitialMaterializationWorkUnits(long workUnits)

No public query result API, SPARQL behavior, manifest schema, or authoritative Frontier payload schema changes.

Revision note (2026-07-26): Created the initial implementation ExecPlan after inspecting the current query preparation,
service lifecycle, configuration, mapped-file precedent, tests, and complete-theme payload shape.

Revision note (2026-07-26): Recorded the focused failing publication test and the unrelated formatter side effect
observed during its verify run.

Revision note (2026-07-26): Recorded completion of the mapped sidecar, lease, configuration, indexed preparation, and
background-rebuild milestones plus their focused test results.
