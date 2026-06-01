# Vectorize LMDB query data access and transient ID sets

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan follows `.agent/PLANS.md`.

## Purpose / Big Picture

LMDB query evaluation currently has several hot paths that read one statement or one ID pair at a time, even when the underlying LMDB cursor is moving sequentially. After this change, LMDB-specific evaluation can read batches of primitive IDs from a cursor, feed those batches into membership and distinct-count operators, and use Roaring bitmaps when a per-query ID set grows large enough to benefit from compressed bitmap membership. The observable behavior remains the same SPARQL results; the visible proof is parity tests, regression tests, and follow-up MEDICAL_RECORDS benchmark runs with vectorization and Roaring enabled.

## Progress

- [x] (2026-05-18 00:15+02:00) Captured root quick-install evidence in `initial-evidence-vectorization.txt`.
- [x] (2026-05-18 00:20+02:00) Mapped scalar scan hooks in `LmdbEvaluationDataset`, `LmdbSailStore`, and `TripleStore`.
- [x] (2026-05-18 00:29+02:00) Added failing tests for vector scan batches and adaptive Roaring ID sets.
- [x] (2026-05-18 01:08+02:00) Added internal batch scan interfaces and TripleStore batch forwarding.
- [x] (2026-05-18 01:24+02:00) Added adaptive `LmdbIdSet` backed by `LmdbLongHashSet` or partitioned Roaring.
- [x] (2026-05-18 01:42+02:00) Routed membership, anti-join, and distinct hot paths through vector batches and adaptive ID sets.
- [x] (2026-05-18 02:10+02:00) Ran targeted LMDB tests, formatting, copyright checks, and regression verifies.

## Surprises & Discoveries

- Observation: `TripleStore.scanTriplesUsingIndexComponents` already decodes selected key components without materializing full quads and has special selected-prefix modes for common `posc` and `ospc` shapes.
  Evidence: `selectedScanMode` and `scanSelectedKey` are present in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java`.
- Observation: the local `.m2_repo` already contains RoaringBitmap `1.3.0`, but not the approved latest `1.6.14`.
  Evidence: `find .m2_repo -path '*org/roaringbitmap/RoaringBitmap*'` listed `1.3.0` and `1.0.0`.
- Observation: LMDB ID/vector evaluation is gated to `IsolationLevels.READ_COMMITTED`.
  Evidence: `QueryBenchmarkTest.optionalRhsFilterQueryProducesExpectedCount` timed out on the scalar left-join path until the benchmark store and load transaction were switched to `READ_COMMITTED`.
- Observation: a full `core/sail/lmdb` verify still includes optimizer/explain metric assertion failures outside the data-access vectorization scope.
  Evidence: `LmdbEvaluationStatisticsMemoizationTest` and `LmdbIndexAwareJoinOrderPlanningTest` failed on missing `resultSizeEstimate` strings in parent explain nodes during the full module verify.

## Decision Log

- Decision: Implement transient Roaring-backed query-state sets first and keep persistent LMDB bitmap side indexes out of this initial patch.
  Rationale: transient sets require only query-evaluation state changes; persistent bitmap indexes would affect storage format, commit/rollback, and rebuild semantics.
  Date/Author: 2026-05-18 / Codex.
- Decision: Add batch scan APIs beside existing scalar scan APIs and keep scalar APIs as fallbacks.
  Rationale: this avoids changing RDF4J iterator contracts while letting LMDB-specific paths consume sequential cursor output in primitive blocks.
  Date/Author: 2026-05-18 / Codex.

## Outcomes & Retrospective

The initial transient query-state implementation is in place. LMDB now has additive batch scan APIs, forwarding dataset wrappers, adaptive Roaring-backed ID sets, and vector/batch consumers in the BGP, EXISTS, NOT EXISTS/MINUS, join dedupe, grouped count, distinct count, and constant-membership paths.

Persistent bitmap side indexes were intentionally not implemented in this pass. They remain a separate gated storage-track because they affect commit, rollback, cache replay, stale snapshot handling, and rebuild behavior.

The targeted vector/Roaring tests and the benchmark-critical optional-right-filter regression pass. Full module verification is not clean yet because of existing/out-of-scope optimizer explain metric failures; those should be handled in the planner/statistics track, not by hard-coding data-access behavior.

## Context and Orientation

The LMDB module is `core/sail/lmdb`. `LmdbEvaluationDataset` is the LMDB-specific query-evaluation interface used by query steps. `LmdbSailStore.LmdbSailDataset` implements it by delegating to `TripleStore`, which owns the native LMDB cursor loops. A `RecordIterator` yields one `long[]` row at a time. Existing component scans avoid some row allocation by calling `RawIdPairConsumer` once per cursor row, but still cross a Java callback per row.

`LmdbLongHashSet` is the current primitive per-query ID set. It is good for small sets and supports touched-slot clearing, but large membership, anti-join, and distinct-count states can benefit from Roaring bitmap compression and fast contains/intersection-style operations. LMDB IDs are `long`, so Roaring must be wrapped with a partitioned adapter that maps the high 32 bits of an ID to a `RoaringBitmap` over the low 32 bits.

## Plan of Work

Add `RawQuadBatchConsumer` and `RawIdPairBatchConsumer` to `LmdbEvaluationDataset`, with default methods that adapt batch calls through existing scalar scans. Implement native batch cursor loops in `TripleStore` and expose them from `LmdbSailStore.LmdbSailDataset`. Delegating and overlay datasets should forward batch calls when the underlying dataset supports them.

Add `LmdbIdSet` and `LmdbIdSets`. `LmdbLongHashSet` should implement the new interface. `LmdbIdSets.create(...)` should return an adaptive set that starts with `LmdbLongHashSet` and promotes to partitioned Roaring after a configurable threshold when `org.eclipse.rdf4j.sail.lmdb.roaring.enabled` is true.

Replace hot membership/distinct state types where safe: BGP collected membership, constant membership statement-pattern sets, early DISTINCT seen sets, component-scan DISTINCT filters, and single-slot batch join dedupe. Do not change storage or planner behavior in this patch.

## Concrete Steps

Run from `/Users/havardottestad/Documents/Programming/rdf4j-stf`.

First verify baseline installation:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Then add failing tests and run:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEvaluationDatasetVectorScanTest LmdbIdSetTest --retain-logs --stream

After implementation, rerun the same tests, then:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --stream

For benchmarking, use READ_COMMITTED and compare vector/Roaring toggles on MEDICAL_RECORDS q0-q10.

## Validation and Acceptance

The vector scan test must prove that `scanIndexComponentsBatch` emits the same IDs as scalar LMDB scans while batching a sequential cursor into requested chunk sizes. The adaptive ID-set test must prove that a large set promotes to a Roaring-backed representation, handles 64-bit ID partitions, deduplicates, contains, clears, and reuses correctly.

Existing LMDB query tests must pass with the default configuration. If Roaring is disabled via system property, all behavior must fall back to `LmdbLongHashSet`.

## Idempotence and Recovery

All APIs are additive and internal. If batch scanning cannot be used by a wrapper dataset, the default method falls back to scalar scanning. If Roaring cannot be loaded or is disabled, `LmdbIdSets` returns hash-backed sets. Persistent bitmap side indexes are not created in this patch, so no LMDB data migration or recovery path is needed.

## Artifacts and Notes

Initial evidence is retained in `initial-evidence-vectorization.txt`.

## Interfaces and Dependencies

Add dependency:

    org.roaringbitmap:RoaringBitmap:1.6.14

New internal interfaces and factories:

    org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset.RawIdPairBatchConsumer
    org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset.RawQuadBatchConsumer
    org.eclipse.rdf4j.sail.lmdb.LmdbIdSet
    org.eclipse.rdf4j.sail.lmdb.LmdbIdSets
