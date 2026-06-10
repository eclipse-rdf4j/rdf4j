# Migrate Omni Witness Persistence To Mmap Snapshots

This ExecPlan is a living document. It follows `.agent/PLANS.md` and must be kept current as work proceeds.

## Purpose / Big Picture

The Omni join estimator currently persists witness weights as a single framed byte payload. Loading that payload either creates a giant byte array or later materializes `Long2ObjectOpenHashMap` witness maps. After this change, persisted base witness data is immutable mmap-backed data, while only new updates live in small heap overlays. A human can observe the change by running the Omni persistence tests and by rerunning `ThemeQueryPlanRunBenchmark.runQuery` without seeing eager base witness map loading in the profile.

## Progress

- [x] (2026-06-10 05:20+02:00) Captured q4 CPU profile showing current hot path is repeated planning/costing, not eager `OmniJoinEstimator` load.
- [x] (2026-06-10 05:20+02:00) Chose first milestone: dedicated mapped witness snapshot store with heap overlay, leaving full planner route tuning for a later milestone.
- [x] (2026-06-10 06:58+02:00) Added failing mapped snapshot/value-record tests and persisted initial red evidence.
- [x] (2026-06-10 07:04+02:00) Implemented mapped witness file reader as top-level `MappedWitnessIndex`.
- [x] (2026-06-10 07:04+02:00) Wired `OmniJoinEstimator.AttributeIndex` to mapped base plus heap overlay through `CompositeWitnessIndex`.
- [x] (2026-06-10 07:04+02:00) Replaced framed Omni join payload persistence with dedicated snapshot store for current OMNI state.
- [x] (2026-06-10 07:55+02:00) Verified focused tests and reran q4 benchmark smoke.

## Surprises & Discoveries

- Observation: after the prior lazy decode patch, `MEDICAL_RECORDS:4` completed instead of hanging, but teardown waited for persist for about 70 seconds on the first fresh run.
  Evidence: benchmark output showed repeated `Waiting for join estimator persist executor to terminate` followed by `232.102 ms/op`.
- Observation: the requested 30-second async-profiler CPU capture did not sample `OmniJoinEstimator`; q4 CPU was dominated by Cascades planning and finite-anchor surface costing.
  Evidence: `profiles/lmdb/omni-q4-cpu-1ms-30s.html` had `CascadesPlanner.optimize` at about 85% inclusive and no sampled `OmniJoinEstimator` frame.
- Observation: the first mmap persistence regression failed exactly where expected.
  Evidence: `OmniWitnessPersistenceStoreTest` reported value records were 24 bytes instead of 32 and
  `MappedWitnessIndex` was not a top-level abstraction.
- Observation: q4 teardown was still waiting on a full background rebuild after the mmap writer change.
  Evidence: the first smoke run logged `Waiting for join estimator persist executor to terminate` while
  `RdfJoinEstimator: Rebuilding bufB` advanced through 8.7M statements.
- Observation: forced rebuild markers did not set the quiet-window mutation timestamp.
  Evidence: `backgroundRebuildWaitsForQuietLoadPeriodAfterForcedRebuildMarker` failed before the fix because the
  rebuild entered within 250 ms.

## Decision Log

- Decision: implement the mmap design as an additive `OmniWitnessPersistenceStore` first, then connect the existing estimator to it.
  Rationale: this isolates file layout and read semantics from planner behavior, gives focused tests, and avoids destabilizing the existing query estimator while replacing the persistence bottleneck.
  Date/Author: 2026-06-10 / Codex.
- Decision: keep a mutable `UpdateOmniSketch` in `AttributeIndex` for now, but move persisted witness postings to mapped base segments and keep only updates in heap overlay maps.
  Rationale: the user asked specifically to stop loading `Long2ObjectOpenHashMap` base witness maps. Keeping the sketch merge path for the first milestone keeps risk bounded while removing the largest object graph.
  Date/Author: 2026-06-10 / Codex.

## Outcomes & Retrospective

This section will be updated after each milestone. The first expected outcome is a passing test proving a persisted mapped snapshot can be probed without materializing base witness maps.

Milestone update 2026-06-10: `OmniWitnessPersistenceStoreTest`, `OmniJoinEstimatorTest`, and
`SketchBasedJoinEstimatorPersistenceTest` pass. Snapshot records now carry sampling probability and min-detectable
fields, mapped/overlay/composite witness indexes are explicit top-level abstractions, and mapped base snapshot
extraction walks one value cursor at a time instead of loading all base postings into a heap map.

Benchmark update 2026-06-10: after marking forced rebuilds as recent mutations, the q4 smoke no longer emitted
`Waiting for join estimator` or `RdfJoinEstimator: Rebuilding` during teardown. The single cold measurement completed
in 39 seconds total with `ThemeQueryPlanRunBenchmark.runQuery MEDICAL_RECORDS:4 omni = 791.543 ms/op`. The generated
plan still chose `lmdb-guarantee-options` / standard winner rather than `omni-join-estimator`, so planner adoption is
not complete.

## Context and Orientation

The relevant module is `core/queryalgebra/evaluation`. `OmniJoinEstimator` owns relations and attributes used by `SketchBasedJoinEstimator` to estimate RDF joins. Each attribute currently has an `UpdateOmniSketch` plus witness weights keyed by value hash. A witness is a sampled identifier such as a statement hash, subject hash, or ordered tuple hash; its weight estimates bag multiplicity.

The current persistence seam is in `SketchBasedJoinEstimator.flushOmniJoinEstimatorPayload`, which calls `state.omniJoinEstimator.toByteArray()` and writes the result through `SketchEstimatorPersistenceStore.appendFramedPayload`. Reload uses `loadOmniJoinEstimatorPayload`, reads the framed payload as a `MemorySegment`, then copies it with `payload.toArray(ValueLayout.JAVA_BYTE)`.

The target design introduces a dedicated `OmniWitnessPersistenceStore`. A mapped base snapshot is read-only and generation-stamped. New updates go to a heap overlay. Reads merge a base cursor and overlay cursor for one value hash. Snapshot writing streams a new file and atomically swaps the manifest.

## Plan of Work

First, add tests in `OmniJoinEstimatorTest` and a dedicated `OmniWitnessPersistenceStoreTest`. The tests must fail before production code by referencing the new mapped snapshot API and by asserting that probing a mapped persisted value leaves the heap `weightsByValueHash` map empty.

Second, add package-private interfaces and classes in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch`: `WitnessIndex`, `WitnessCursor`, `MappedWitnessIndex`, `OverlayWitnessIndex`, `CompositeWitnessIndex`, `MappedOmniJoinSnapshot`, and `OmniWitnessPersistenceStore`. Use Java FFM `MemorySegment` and `FileChannel.map` for read-only segments. Keep the file layout simple but compatible with the user-provided design: manifest, attribute segment table, per-attribute compact sketch bytes, value hash table, sorted value records, and sorted posting entries.

Third, modify `OmniJoinEstimator.AttributeIndex`. It should hold a nullable mapped base witness index, a heap overlay map for updates, and optional legacy lazy bytes only for old framed snapshots during migration. `probePredicate` and `probeJoin` must read base postings through cursors and merge overlay postings without materializing base maps.

Fourth, replace the framed payload path for Omni in `SketchBasedJoinEstimator` with `OmniWitnessPersistenceStore`. Existing metadata can keep the old refs as a compatibility fallback while the new manifest exists under `join-estimator/`.

## Concrete Steps

Run from repository root:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -DskipITs -Dtest=OmniJoinEstimatorTest#mappedSnapshotProbeDoesNotMaterializeBaseWitnessMaps verify

Before production edits, expect the new test to fail to compile or fail its assertion. After implementation, expect it to pass with `Tests run: 1, Failures: 0, Errors: 0`.

## Validation and Acceptance

Focused acceptance starts with:

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -DskipITs -Dtest=OmniJoinEstimatorTest,OmniWitnessPersistenceStoreTest,SketchBasedJoinEstimatorPersistenceTest verify

Then run:

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbEvaluationStatisticsMemoizationTest,LmdbStoreConfigTest,LmdbOptimizerPipelineTest,LmdbCascadesMinusFilterPushdownTest verify

Benchmark acceptance:

    ./scripts/run-single-benchmark.sh --theme-plan-run --theme-query MEDICAL_RECORDS:4 --no-build --warmup-iterations 0 --measurement-iterations 20 --forks 1 --param sketchEstimatorEnabled=true --param sketchEstimatorStrategy=omni

The benchmark must complete, optimized SPARQL must still remove the unused `OPTIONAL`, and async-profiler must not show base witness map materialization or `payload.toArray` in the Omni load path.

## Idempotence and Recovery

The new snapshot writer writes temp files and then atomically moves `manifest.tmp` to `manifest.bin`. If a write is interrupted, the existing manifest remains valid and can be reopened. If a mapped snapshot cannot be read, the estimator should fall back to the existing framed payload migration path or rebuild from source data.

## Artifacts and Notes

The current profile artifact is:

    profiles/lmdb/omni-q4-cpu-1ms-30s.html

The q4 benchmark after lazy decode but before mmap snapshot persistence:

    ThemeQueryPlanRunBenchmark.runQuery MEDICAL_RECORDS:4 omni
    20 iterations: 133.032 ± 18.783 ms/op

## Interfaces and Dependencies

Use only JDK FFM and the existing vendored OmniSketch/DataSketches code. Do not add dependencies.

Define package-private interfaces:

    interface WitnessIndex {
        WitnessCursor cursor(long valueHash);
    }

    interface WitnessCursor {
        boolean next();
        long witnessHash();
        double weight();
    }

Define snapshot classes:

    final class OmniWitnessPersistenceStore implements AutoCloseable
    final class MappedOmniJoinSnapshot implements AutoCloseable

`OmniJoinEstimator` should expose package-private methods for snapshot writer access and should accept a mapped snapshot for load.
