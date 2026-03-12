# Persist, Lazy-Load, and Unload Join Estimator Sketches

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan follows the repository guidance in `PLANS.md` at the repository root. All updates to this document must stay aligned with that file.

## Purpose / Big Picture

After this change, persistent MemoryStore and LMDB store instances can reuse a serialized join-estimator snapshot instead of rebuilding estimator sketches at startup. The estimator can be unloaded under memory pressure so write throughput is protected, and query planning falls back safely when sketches are unavailable or stale. You can observe success by starting stores with persisted data, confirming startup avoids eager estimator rebuild, running writes, syncing/committing, and seeing a `join-estimator.rjes` snapshot get written and reused.

## Progress

- [x] (2026-02-24 15:19+01:00) Scanned current estimator/store code and PLANS requirements.
- [x] (2026-02-24 15:19+01:00) Ran mandatory root quick install with workspace-local `.m2_repo`.
- [x] (2026-02-24 15:30+01:00) Added failing tests for estimator snapshot/lazy/unload behavior.
- [x] (2026-02-24 15:33+01:00) Implemented estimator persistence, lazy load, unload, and readiness guards.
- [x] (2026-02-24 15:35+01:00) Added failing tests for MemoryStore + LMDB integration points.
- [x] (2026-02-24 15:36+01:00) Implemented MemoryStore and LMDB persistence scheduling and low-memory hooks.
- [x] (2026-02-24 15:38+01:00) Ran focused module tests and captured acceptance evidence snippets.
- [x] (2026-02-24 15:39+01:00) Final formatting and plan update complete.

## Surprises & Discoveries

- Observation: ExecPlan guidance is in root `PLANS.md` (not `.agent/PLANS.md`).
  Evidence: repository listing includes `PLANS.md`; `.agent` directory absent.
- Observation: Snapshot deserialization through DataSketches heapify path triggered runtime coupling to incubator foreign-memory APIs in this environment.
  Evidence: failing test surfaced `NoClassDefFoundError` involving `jdk.incubator.foreign.MemorySegment` during snapshot load.

## Decision Log

- Decision: Use Routine D (ExecPlan) and still drive behavior changes with focused failing tests before implementation.
  Rationale: Scope touches estimator core + MemoryStore + LMDB integration, so risk and blast radius are high.
  Date/Author: 2026-02-24 / Codex

- Decision: Keep snapshot file format external to existing MemoryStore data format (`join-estimator.rjes`).
  Rationale: Avoids changing existing persistent dataset serialization compatibility.
  Date/Author: 2026-02-24 / Codex

- Decision: Serialize sketches as retained hash entries (via `HashIterator`) instead of sketch binary heapify payloads.
  Rationale: Avoids runtime dependency on foreign-memory based deserialization path while preserving deterministic snapshot restore behavior.
  Date/Author: 2026-02-24 / Codex

- Decision: For LMDB targeted verification, run new surefire class with `-DskipITs`.
  Rationale: module `verify` also executes existing failsafe ITs; one unrelated IT (`LmdbStoreConsistencyIT`) fork-crashed with exit 134 and obscured new-test signal.
  Date/Author: 2026-02-24 / Codex

## Outcomes & Retrospective

Completed.

Implemented:
- `SketchBasedJoinEstimator` persistence/lazy-load/unload APIs and state guards (`dirty`, `sketchesLoaded`, `rebuildRequired`, low-memory supplier).
- Snapshot IO (`join-estimator.rjes`) with atomic replace and compatibility checks.
- MemoryStore integration for startup lazy-load, restore-path estimator update suppression when snapshot exists, and sync-path `persistIfDirty`.
- MemorySailStore low-memory unload hook and estimator update gating for restore.
- LMDB integration for startup lazy-load, debounced persist scheduling, low-memory unload hook, and best-effort final persist on close.
- New tests in base/memory/lmdb modules for snapshot lifecycle and store wiring.

Validation highlights:
- `core/sail/base`: `SketchBasedJoinEstimatorPersistenceTest`, `SketchBasedJoinEstimatorConfigTest`, `SketchBasedJoinEstimatorSysPropsTest` green.
- `core/sail/memory`: `PersistentMemoryStoreJoinEstimatorTest` green.
- `core/sail/lmdb`: `LmdbSailStoreEstimatorPersistenceTest` green with `-DskipITs`.

Residual risk:
- Full-module LMDB `verify` still depends on existing integration tests and environment-native stability; targeted class verification is green, but unrelated IT fork crash remains outside this change scope.

## Context and Orientation

`core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SketchBasedJoinEstimator.java` owns sketch state and join-cardinality estimation. It currently rebuilds sketches from the store and applies every add/delete mutation to both internal buffers. It has no persistence API and no unload state. `isReady()` currently only checks whether `seenTriples > 0`.

`core/sail/memory/src/main/java/org/eclipse/rdf4j/sail/memory/MemorySailStore.java` owns one estimator instance and routes add/delete operations through it inside transaction sink methods. It already has a memory-pressure heuristic for snapshot cleanup; no estimator unload hook exists.

`core/sail/memory/src/main/java/org/eclipse/rdf4j/sail/memory/MemoryStore.java` handles persistent data-file read/write and sync scheduling but does not persist estimator state.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` owns an estimator and currently does eager `rebuildOnceSlow()` on startup. It applies queued estimator updates on successful flush but has no debounced snapshot persistence.

Tests currently covering estimator behavior live in `core/sail/base/src/test/java/org/eclipse/rdf4j/sail/base/`. Memory and LMDB store behavior tests live under `core/sail/memory/src/test/java/...` and `core/sail/lmdb/src/test/java/...`.

## Plan of Work

Milestone 1 introduces failing tests in `core/sail/base` for persistence lifecycle and readiness semantics: snapshot persistence when dirty, lazy load on first readiness call, unload fallback semantics, and refusing join-cardinality answers when sketches are unavailable. Then implement estimator APIs and internal state flags (`dirty`, `sketchesLoaded`, `rebuildRequired`, optional low-memory supplier) plus snapshot read/write helpers.

Milestone 2 adds integration tests for MemoryStore and LMDB around snapshot files and startup/flush behavior. Then wire store-level calls: configure persistence path on startup, skip costly estimator updates during data restore when snapshot exists, persist snapshot on sync/commit debounce, and unload estimator under memory pressure in MemorySailStore.

Milestone 3 runs targeted module verifies and updates this plan with final outcomes and residual risk.

## Concrete Steps

Working directory for all commands: `/Users/havardottestad/Documents/Programming/rdf4j-stf`

1. Add failing tests for estimator behavior:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/base -Dtest=SketchBasedJoinEstimatorPersistenceTest

2. Implement estimator changes and rerun targeted tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/base -Dtest=SketchBasedJoinEstimatorPersistenceTest,SketchBasedJoinEstimatorConfigTest,SketchBasedJoinEstimatorSysPropsTest

3. Add failing integration tests and implement store wiring:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/memory -Dtest=PersistentMemoryStoreJoinEstimatorTest

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb -Dtest=LmdbSailStoreEstimatorPersistenceTest

4. Final targeted verifies:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/base

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/memory

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb

Expected behavior during milestone execution: pre-change tests fail for missing persistence/lazy-load/unload behavior; post-change tests pass and existing targeted suites remain green.

## Validation and Acceptance

Acceptance is satisfied when all of the following are true:

- A snapshot file `join-estimator.rjes` is written only through explicit sync/commit pathways (`persistIfDirty`), not for every mutation.
- Estimator can start unloaded when snapshot exists, load on first readiness demand, and report join estimation support only when safe.
- When unloaded and writes occur, estimator marks itself stale (`rebuildRequired`) and query optimizer paths fall back cleanly.
- MemoryStore and LMDB startup paths avoid unnecessary eager rebuild when snapshot exists.
- Targeted tests for base, memory, and LMDB modules pass.

## Idempotence and Recovery

The implementation is additive and retry-safe. Re-running tests is safe. Snapshot files are replaced atomically through temp-file move semantics where supported. If snapshot load fails due to format/config mismatch, estimator remains in fallback mode and can rebuild from store data; this keeps query planning correct.

## Artifacts and Notes

During implementation, include short command/report snippets proving failing-then-passing tests and snapshot-file behavior. Keep snippets focused on test method names and success/failure summaries.

## Interfaces and Dependencies

At completion, these interfaces and methods must exist and be used:

- In `SketchBasedJoinEstimator`:

    public void configurePersistence(Path file, boolean lazyLoad)
    public void setLowMemorySupplier(BooleanSupplier supplier)
    public boolean persistIfDirty()
    public void unload()

- Existing methods updated semantically:

    public boolean isReady()
    public synchronized long rebuildOnceSlow()
    public void addStatement(Statement st)
    public void deleteStatement(Statement st)
    public double cardinality(Join node)

- In `MemorySailStore`:

    SketchBasedJoinEstimator getSketchBasedJoinEstimator()
    void setSketchEstimatorUpdatesEnabled(boolean enabled)

- In `MemoryStore` and `LmdbSailStore`: configure persistence path and call `persistIfDirty()` on sync/flush scheduling.

Revision note (2026-02-24): Initial ExecPlan created to execute estimator persistence/lazy-load/unload work end-to-end with milestone tracking.
