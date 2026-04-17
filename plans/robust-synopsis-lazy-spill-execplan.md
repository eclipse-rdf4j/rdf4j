# Reduce robust synopsis spill, rebuild, and memory pressure cost

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with [PLANS.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/PLANS.md).

## Purpose / Big Picture

After this change, the robust synopsis used by the join estimator should stop paying the full heap cost of all binary pattern synopses at load time. Persisted `RJMS` snapshots should load only resolver and unary data eagerly, then load each binary synopsis on demand. When memory pressure rises, the estimator should be able to unload those lazily loaded binary synopses before escalating to a full robust-synopsis spill or full estimator unload. Rebuild should also stop doing duplicate per-statement work for the base estimator and robust synopsis builder.

## Progress

- [x] (2026-04-10 11:04 CEST) Captured hotspot evidence and mapped the spill, merge, rebuild, and memory-accounting call sites.
- [x] (2026-04-10 11:04 CEST) Added `RJMS` v2 sketch payload support and builder-side batch registration primitives in `StoreSynopsis`.
- [x] (2026-04-10 12:10 CEST) Replaced eager binary synopsis loading with disk-backed lazy indexing in `StoreSynopsis.readFrom`; kept `readFullyFrom` for temporary spill outputs.
- [x] (2026-04-10 12:10 CEST) Connected `StoreSynopsis` binary load/unload callbacks to `SketchBasedJoinEstimator` memory tracking and pressure recovery.
- [x] (2026-04-10 12:10 CEST) Batched `rebuildOnceSlow()` through `IngestEvent[]` plus `StoreSynopsis.Builder.addStatements(...)`; added the single-segment spill fast path.
- [x] (2026-04-10 12:10 CEST) Extended persistence and robust-snapshot tests for lazy load/unload, v1/v2 compatibility, and spill equivalence.
- [ ] (2026-04-10 12:10 CEST) Finish a full downstream LMDB benchmark-style verification run; the targeted `ThemeQueryBenchmarkJoinEstimationTest` attempt was stopped after several minutes without reaching a suite summary.

## Surprises & Discoveries

- Observation: the spill hotspot is not only top-level sort work; rebuild also duplicates fingerprinting and robust-builder ingestion work.
  Evidence: `SketchBasedJoinEstimator.rebuildOnceSlow()` currently calls `ingest(...)` and then separately recomputes `valueFingerprint(str(...))` for `StoreSynopsis.Builder`.

- Observation: merge correctness only depends on top-level key ordering, not neighborhood entry ordering.
  Evidence: `StoreSynopsisSnapshotSupport.mergeBinaryEntries(...)` merges by `BinaryPatternKey`, while neighborhood maps are unioned via hash-map lookups.

- Observation: non-persistent rebuild spill cannot return a lazy disk-backed synopsis because the temporary spill directory is deleted on close.
  Evidence: `RobustSynopsisSpillBuffer.close()` always deletes the spill directory.

- Observation: slot-local persisted refs were not enough; dirty ownership also had to become slot-local.
  Evidence: during rebuild-into-`A`, the old published `B` slot still carried stale dirty bits after a successful persist, so budget eviction rewrote the canonical sidecar even though the rebuilt `A` slot was spilling to a temp sidecar.

## Decision Log

- Decision: use `RJMS` v2 native DataSketches payloads for new writes while keeping v1 read support.
  Rationale: this removes per-entry rewrite cost and allows whole-sketch union during merge without changing public API.
  Date/Author: 2026-04-10 / Codex

- Decision: make `StoreSynopsis.readFrom(Path)` lazy for binary synopses and add `StoreSynopsis.readFullyFrom(Path)` for temporary spill outputs.
  Rationale: persistent snapshots can remain disk-backed; temporary merged snapshots cannot because their files are deleted after the rebuild finishes.
  Date/Author: 2026-04-10 / Codex

- Decision: unload all loaded robust binary synopses as the first robust-memory shedding step instead of implementing partial LRU in this pass.
  Rationale: this keeps the first implementation small, deterministic, and sufficient to release large binary payloads before escalating to full spill.
  Date/Author: 2026-04-10 / Codex

## Outcomes & Retrospective

Implemented in `core/sail/base`: persisted robust synopses now stay disk-backed by default, binary synopses load on demand, estimator pressure can unload loaded binary synopses before escalating to whole-synopsis spill, rebuild reuses batched `IngestEvent` work for both estimator and robust synopsis ingestion, and canonical sketch-slot persistence no longer gets corrupted by stale dirty state from the inactive buffer slot. Focused verification is green in `core/sail/base`; an additional LMDB benchmark-style class was started but not allowed to run to completion.

## Context and Orientation

The robust join synopsis lives in `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/sketchoptimizer/StoreSynopsis.java`. It persists to `RJMS` files and is used by `PatternSynopsisAdapter` during robust join planning. `StoreSynopsisSnapshotSupport.java` merges sorted spill segments during rebuild. `SketchBasedJoinEstimator.java` owns memory accounting, rebuild, persistence, and spill/unload policy. Persistence regression tests live in `core/sail/base/src/test/java/org/eclipse/rdf4j/sail/base/SketchBasedJoinEstimatorPersistenceTest.java`. Benchmark-oriented persistence tests live in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ThemeQueryBenchmarkJoinEstimationTest.java`.

In this repository, a “binary synopsis” means the robust synopsis for a two-variable statement-pattern shape. Each binary synopsis contains two degree sketches plus two neighborhood maps. Those binary synopses dominate robust heap cost and should be disk-backed when possible.

## Plan of Work

First, rework `StoreSynopsis` so the immutable synopsis can be backed by an `RJMS` file. The eager parts remain the resolver map and unary sketches. The binary section becomes an index of keys to payload offsets and lengths, plus a small cache of loaded `BinaryPatternSynopsis` objects. Add hooks so the owning estimator can be notified before load and after load or unload. Keep `writeTo(...)` correct for both eager and lazy synopses, with a fast whole-file copy when a lazy snapshot is already backed by the same on-disk version.

Second, update `StoreSynopsisSnapshotSupport` to read both v1 and v2 segment payloads and to merge them by unioning whole sketches. Keep top-level key sorting only. Do not reintroduce nested neighborhood-map sorting.

Third, rework `SketchBasedJoinEstimator.rebuildOnceSlow()` to batch statements through existing `IngestEvent` machinery and reuse the same precomputed `hs/hp/ho/hc` values for robust-builder ingestion. Connect robust binary load/unload to estimator memory accounting and pressure recovery. Add the single-segment spill fast path in `RobustSynopsisSpillBuffer.complete()`.

Finally, extend tests to prove lazy robust loading, memory-tracking updates, unload-under-pressure behavior, v1-read/v2-write compatibility, and direct-build versus merged-spill equivalence.

## Concrete Steps

Work from repository root `/Users/havardottestad/Documents/Programming/rdf4j-stf`.

1. Edit `StoreSynopsis.java` and `StoreSynopsisSnapshotSupport.java`.
2. Edit `SketchBasedJoinEstimator.java` to connect lazy robust loading, pressure shedding, and rebuild batching.
3. Add or extend tests in `core/sail/base` and `core/sail/lmdb`.
4. Run focused verification with the repo’s Maven workflow, then broaden to the affected module tests.

Expected verification commands after implementation:

    python3 .codex/skills/mvnf/scripts/mvnf.py StoreSynopsisSnapshotCompatibilityTest
    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorPersistenceTest
    python3 .codex/skills/mvnf/scripts/mvnf.py ThemeQueryBenchmarkJoinEstimationTest

## Validation and Acceptance

Acceptance is met when a persisted estimator can load a robust synopsis without materializing every binary synopsis, robust planner access can lazily materialize a needed binary synopsis, memory tracking reflects that load and later unload, pressure handling can release loaded robust binary synopses before falling back to full robust spill, and rebuild still publishes a correct robust synopsis even when spill segmentation is forced. Tests must cover both eager temporary loads and persistent lazy loads, and merged v2 snapshots must produce the same robust answers as direct builds.

## Idempotence and Recovery

The code changes are additive and retryable. If a lazy-load attempt fails, the estimator should invalidate the robust synopsis and fall back to the existing rebuild path rather than leaving partially initialized cache state behind. Temporary spill directories remain disposable and can be recreated on the next rebuild.

## Artifacts and Notes

Initial hotspot evidence:

    Comparator.lambda$comparingInt... ~9.6%
    HashMap.getNode ~4.3%
    ByteArrayOutputStream.ensureCapacity ~2.6%
    StoreSynopsis.writeSketch(...) ~1.1%
    HeapArrayOfDoublesCompactSketch.<init> ~0.94%

## Interfaces and Dependencies

`StoreSynopsis` should expose an internal callback attachment point for binary-cache lifecycle notifications. `SketchBasedJoinEstimator` remains the owner of `MemoryCategory.ROBUST_SYNOPSIS` tracking and should call into `StoreSynopsis` only through that internal hook and an unload method. No new third-party dependencies are required; continue using Apache DataSketches for tuple sketch serialization and union.

Change note: created this ExecPlan because the task expanded from a local spill optimization into a cross-module refactor spanning snapshot format, lazy loading, and memory-pressure behavior.
