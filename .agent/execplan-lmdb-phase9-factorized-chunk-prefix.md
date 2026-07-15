# Factorized chunk prefix (parent-pointer chunks) for the LMDB chunk pipeline

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

Inside the LMDB native engine's chunk pipeline (`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeChunkPipeline.java`), every join match currently copies the ENTIRE upstream slot vector into the output batch: `out.copyFromRow(scratch, outputRows)` at the three emission sites (drainOpenProbe ~:1404, emitReplay ~:1454, drainMergeKey ~:1162). A 4-deep, 10-slot prefix writes each logical row's upstream values ~5 times full-width. This phase replaces that with parent-pointer chunks: each stage's output batch stores ONLY its fresh slot columns plus one `int parentRow` per row; upstream values are written once per upstream row and gathered lazily. Expected effect: ~7× less write traffic on wide prefixes, L2-resident boundary working sets, cheaper per-row gathers everywhere downstream.

HONESTY GATE (M0): the design analysis concluded this is a memory-traffic/cache optimization, NOT an asymptotic one — `analyzeSplit` (LmdbNativeFactorizedRows.java:282-334) already peels every multiplicity-tolerant trailing pattern into tail branches, so everything left flat is genuinely consumed per-element downstream. M0 measures first: widen `ChunkPipelineChainBenchmark` with a wide-slot variant and profile; if row-copying is under ~15% of the wide 4-deep chain profile, STOP the phase and report that in Outcomes.

## Progress

- [x] (2026-07-15 21:20Z) Design finalized (representation chosen; alternatives rejected with reasons in the Decision Log).
- [ ] M0: honesty-gate benchmark (wide ChunkPipelineChainBenchmark variant + allocation/copy profile; run with Phase 8 CSR flag both on and off).
- [ ] M1: `FactorizedChunk` struct + gather + classic-probe port (ScanStage, startRow, drainOpenProbe, FilterStage, ChunkPrefixRowCursor) + struct/routing unit tests + fuzz flag-on.
- [ ] M2: acceleration ports (emitReplay, drainMergeKey, recordReplay, hash-bucket replay, ExternalRootBatchStage) + short-fill discipline + counter-pinning tests.
- [ ] M3: gate (`flatCount >= 2 && (flatCount >= 3 || slotCount >= 6)`), flag `rdf4j.lmdb.chunkPipeline.factorizedPrefix.enabled` (default off), explain `factorizedPrefix=true`, correlated-entry allocation-parity test.
- [ ] M4: benchmarks (acceptance: >=15-20% on wide deep chains, +-3% elsewhere) + flip decision.
- [ ] M5 (deferred): generation ring only if SHORT_FILLS shows fill-degree collapse.

## Surprises & Discoveries

(To be filled.)

## Decision Log

- Decision: Candidate A, parent-pointer chunks with a single-generation in-place-refill rule (a stage refills its input only while its output batch is empty; mid-fill exhaustion returns a short batch — precedent: the pendingHashBuild break ~:926-928).
  Rationale: induction over the pull chain proves no live row references an old generation at refill time, so no pooling/refcounting/GC churn; memory strictly shrinks. Kuzu-style flat/unflat pairs (B) degenerate to the row chain here because the flat-prefix invariant guarantees downstream per-element reads, and B forfeits batch-granularity accelerations (replay/memo/merge key off batched scalars). Run-length headers (C) are a compression of A's naturally-RLE parentRow; C's idea is kept on the READ side as run-skipping gathers.
  Date/Author: 2026-07-15 / approved plan.
- Decision: NO sink changes in v1 — `ChunkPrefixRowCursor` stays the single expansion point (run-skipping gather into the scalar `row.slots`). Ordered-factorized sort keeps full-width packed rows (chains must not outlive generations); parallel OutputPages keep pre-expansion (chains must not cross threads).
  Rationale: soundness; the second-order win (amortized O(freshSlots) gathers) arrives anyway.
  Date/Author: 2026-07-15 / approved plan.

## Outcomes & Retrospective

(To be filled — including the M0 stop/go verdict.)

## Context and Orientation

The chunk pipeline is a pull chain of `BatchStage { int fill(NativeBatch out); }` stages (ScanStage → ProbeStage… → FilterStage…), terminating in `ChunkPrefixRowCursor` which copies each selected batch row into the query-wide scalar `RowState.slots` (`long[]`, one live value per slot) before tail branches/filters/sinks consume it. THE CORE INVARIANT: every downstream consumer (probe-key lookups via `scratch[keySlots[i]]` and `PatternPlan.openRaw`'s `s.lookup(row.slots)`, `FilterStage`'s `filter.accept(row)`, `TailBranch.probeKey.refill(row.slots, keySlots)`) reads scalar slot values; a slot may only be consumed while its producing chunk position is pinned. `analyzeSplit` keeps a pattern flat exactly when a later pattern or filter reads its fresh slots.

New struct (nested in LmdbNativeChunkPipeline or its own file):

    final class FactorizedChunk {
        final FactorizedChunk parent;   // fixed at construction; null at root
        final int[] columnSlots;        // the slots THIS level binds (the stage's fresh slots)
        final long[] columns;           // columnSlots.length * capacity, column-major
        final int[] parentRow;          // physical row index in parent, per local physical row
        final int[] selection;          // same discipline as NativeBatch
        int rowCount, selectedCount;
        long generation;                // assert-only
    }

A logical row is the leaf→root path. Slot resolution precomputes `producerLevel[]`/`producerColumn[]` per slot (seed-bound slots served from the seed template). Hot paths never walk chains randomly: probe keys come from the stage's incrementally-maintained full-width `scratch` (copy an ancestor level only when its resolved physical row changed — adjacent rows usually share parents), and full gathers happen only at FilterStage and the cursor boundary, both run-skipping (skip re-copying an unchanged ancestor level and everything above it). `LmdbNativeBatch.java` stays untouched; `FactorizedChunk` is pipeline-private.

## Plan of Work

M0: add a wide-slot (slotCount >= 10), 4-deep chain variant to `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ChunkPipelineChainBenchmark.java`; measure flag-off baseline and profile (async-profiler/JFR alloc+cpu via the docker loop or local JFR); compute the fraction attributable to copyFromRow/copyToRow at stage boundaries. Gate: >= ~15% to proceed.

M1: struct + `bindQuadFactorized` (constant compare / seed compare / fresh column write with in-quad repeated-slot conflict check) + ScanStage port (drop per-row seed copies — seeds live in the seed template) + ProbeStage.startRow incremental scratch + drainOpenProbe emission (`parentRow[out] = currentInputRow` + fresh binds) + FilterStage and ChunkPrefixRowCursor run-skipping gathers + refill discipline in fill(). Unit tests: 3-level gather, selection compaction, generation asserts, short fills, bindSlot routing bug-class tests (seed-bound conflict on probe pattern; repeated fresh slot forced flat; shared-slot keys). Parameterize `LmdbNativeChunkPipelineTest`/`LmdbNativeChunkPipelineSetupFailureTest` over the flag; fuzz flag-on.

M2: port emitReplay/drainMergeKey/recordReplay (read fresh columns just written)/bucket replay/ExternalRootBatchStage; SHORT_FILLS counter; counter-pinning tests must show IDENTICAL RUN_REPLAYS/MEMO_REPLAYS/HASH_BUILDS/MERGE_WALKS deltas flag-on vs flag-off (the accelerations are representation-agnostic).

M3: engagement gate + flag + explain (`describeEngagement` gains `factorizedPrefix=true`) + `LmdbNativeAttemptMetrics.recordChunkFactorizedPrefix/recordChunkShortFill` + correlated-entry allocation-parity test (pattern of `correlatedFragmentEntryNeverBuildsPerOuterRow`).

M4: ChunkPipelineChainBenchmark (both widths/flags), FactorizedRowsStarBenchmark + FactorizedTailStarBenchmark (parity required), ThemeQueryBenchmark MEDICAL q8 (correlated parity). Acceptance as in Purpose; otherwise flag stays experimental.

## Concrete Steps

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeChunkPipelineTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest -- -Drdf4j.lmdb.chunkPipeline.factorizedPrefix.enabled=true
    scripts/run-single-benchmark.sh (ChunkPipelineChainBenchmark selection; see script --help)

## Validation and Acceptance

Differential suite green at EVERY milestone. M0 verdict recorded. M4 acceptance: >=15-20% on the wide deep chain, +-3% elsewhere, non-degenerate SHORT_FILLS.

## Idempotence and Recovery

Representation chosen at construction; refusal builds today's NativeBatch pipeline — no mid-flight fallback needed (no budget can run out mid-flight in the single-generation scheme). Flag default off throughout.

## Artifacts and Notes

(Evidence per milestone.)

## Interfaces and Dependencies

Pipeline-private `FactorizedChunk` (fields above); flag `rdf4j.lmdb.chunkPipeline.factorizedPrefix.enabled`; counters on `LmdbNativeChunkPipeline` + `LmdbNativeAttemptMetrics`. No external dependencies.
