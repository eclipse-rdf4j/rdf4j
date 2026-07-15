# CSR as the primary in-memory scan substrate (successor plan after the phases 7-10 retrospective)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

The phases 7-10 retrospective (`.agent/review-phases-7-10-retrospective.md`) concluded that the three
features built there converge on one better design that none of them reached alone: the per-predicate CSR
adjacency cache (`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCsrAdjacencyCache.java`)
should serve not only PROBES but whole ROOT SCANS. A root pattern `?s <p> ?o` over a cached predicate can
be enumerated straight from the entry's flat arrays — every key with its contiguous run, in exactly the
spoc emission order the engine already expects — with zero LMDB/JNI work. That subsumes the deferred
Phase 10 range pushdown for cached predicates (iterate only keys/runs that intersect the wanted window —
the entry's `allNeighborsOrderedIntegers`/min/max stats make it sound), and it multiplies with Phase 7's
partitioning (workers claim slices of the entry's key space instead of LMDB key ranges — no split
planning, no B+tree at all).

After this change, the second execution of a scan-heavy query over a hot predicate does its root scan at
memory speed: observable via a new counter (`CSR_ROOT_SCANS`), the explain strategy string, and a
benchmark delta on `ThemeQueryBenchmark` MEDICAL_RECORDS. The PROOF milestone (M1) exists precisely to
demonstrate, with benchmark evidence, that this approach beats the phases 7-10 implementation on the same
workload — per the user's mandate.

## Progress

- [x] (2026-07-16) Retrospective and this plan authored.
- [ ] M1 (PROOF): `LmdbCsrScanIterator` — a `RecordIterator` over an ENTIRE `CsrEntry` (all keys, all
      runs, positional quads, optional bound-context filtering). Serve it from the two decorated probe
      paths' owning sources when a full scan `(-1, p, -1, c)` is requested and a valid BY_SUBJECT entry
      exists (`statements(...)` in `LmdbSailDataset`/`ParallelSnapshotSource`, same `csrEligible` and
      revision gates as probes). Counter `CSR_ROOT_SCANS`. Tests: order/content parity vs
      `getTriples(txn, -1, p, -1, -1)` (the entry stores runs in exactly this order — assert byte
      equality), invalidation, fuzz with cache forced hot. BENCHMARK: theme MEDICAL q8 + a
      root-scan-dominated query, cache-on vs phases-7-10 tree — the prove-it-better evidence.
- [ ] M2: partition mode over CSR entries — `PartitionCursor` claims key-index slices `[k0, k1)` of the
      entry instead of LMDB key ranges when the root is CSR-served (no split planning; equal key-count
      slices; workers read disjoint array sections). Extends `LmdbNativeExchange`.
- [ ] M3: sound numeric range pushdown, CSR-gated — when a depth-0 `CachedCompareFilter` (see
      `evaluation/LmdbNativeFilters.java`, field `constantIntegerValue`) targets the root's object slot
      and the entry has `allNeighborsOrderedIntegers`, the CSR scan iterator skips runs/values outside
      the window (`ValueIds.orderedIntegerValue` compare). The residual filter stays. This delivers the
      deferred Phase 10 M3 soundly.
- [ ] M4: explain plumbing — thread `+csr(rootScan|probes)` through the deferred-strategy path
      (`LmdbNativeAttemptMetrics.deferStrategy`) so `explain(Telemetry)` reports cache engagement.
- [ ] M5: Phase 9 honesty gate measurement (wide-slot `ChunkPipelineChainBenchmark` variant + JFR via the
      docker loop) — go/no-go on the parent-pointer prefix, informed by how much CSR-served scans shrink
      the remaining copy cost.

## Surprises & Discoveries

(To be filled.)

## Decision Log

- Decision: Prove M1 before M2-M5; the mandate requires benchmark evidence that this design beats the
  phases 7-10 implementation before investing further.
  Rationale: user instruction (2026-07-15 hard gate).
  Date/Author: 2026-07-16 / Claude.

## Outcomes & Retrospective

(To be filled.)

## Context and Orientation

Read `.agent/review-phases-7-10-retrospective.md` first; then
`.agent/execplan-lmdb-phase8-csr-adjacency-cache.md` (cache design, revision validation, budgets) and
`.agent/execplan-lmdb-phase7-range-partitioned-scans.md` (exchange/partition machinery). Key facts:
`CsrEntry` holds `runStart[]` prefix sums over `neighbors[]`/`contexts[]` with keys in first-encounter
(index) order via `tableKeys/tableSlotPlus1`; BUT full-scan emission needs keys in KEY order — for
BY_SUBJECT entries built from posc sweeps the first-encounter order of subjects is NOT sorted, while the
spoc scan order is (subject-sorted). M1 therefore needs a sorted key view: either build a
`int[] keysSortedIndex` (argsort of a dense-id→key array, built once at publish, ~4 bytes/key) or emit in
posc order and only serve callers that don't require spoc order (the chunk pipeline's ScanStage consumes
`statements(-1,p,-1,-1)` whose LMDB implementation picks posc for the p-bound mask — CHECK
`bestIndexByBoundMask` first: if posc is what LMDB would serve, emitting entry pairs grouped by OBJECT
(BY_OBJECT entry!) in first-encounter order IS the posc order, and the BY_OBJECT entry is the natural
full-scan source). Verify against `TripleStore.getTriples` selection before coding; the parity test will
pin whichever is correct.

## Plan of Work / Concrete Steps / Validation and Acceptance

M1 acceptance: parity tests green; fuzz green with cache hot; benchmark shows the CSR-served
configuration beating the phases 7-10 tree on the same query set (report ms/op before/after in
Artifacts). Commands: `python3 .codex/skills/mvnf/scripts/mvnf.py <TestClass>`;
`./scripts/run-single-benchmark.sh --no-build --module core/sail/lmdb --class
org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery --param
themeName=MEDICAL_RECORDS --param z_queryIndex=8 --jvm-arg -Drdf4j.lmdb.csrCache.enabled=true ...`.

## Idempotence and Recovery

All additive behind the existing `rdf4j.lmdb.csrCache.enabled` flag (plus `.rootScans` sub-flag,
default on when the cache is on, so M1 can be isolated in benchmarks).

## Artifacts and Notes

(Benchmark evidence lands here.)

## Interfaces and Dependencies

New `LmdbCsrScanIterator implements RecordIterator` (package `org.eclipse.rdf4j.sail.lmdb`); a
`CsrEntry.keysSortedIndex` (or the BY_OBJECT-entry decision per Context); counter `CSR_ROOT_SCANS`;
property `rdf4j.lmdb.csrCache.rootScans` (default true).
