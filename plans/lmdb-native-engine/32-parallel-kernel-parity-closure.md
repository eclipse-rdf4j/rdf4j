# Parallel kernel parity closure: DISTINCT merges, scan roots, and a decline that is finally a handoff

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (repository root: `/`, plan conventions file: `.agent/PLANS.md`).

Supersedes the open milestones of `plans/lmdb-native-engine/31-parallel-kernel-parity.md` (its M5 survey, M8 ScanQuad roots, and the DISTINCT-input gap its Outcomes recorded as "a REAL parity gap").

## Purpose / Big Picture

The LMDB native engine runs GROUP BY three ways: the interpreted parallel engine (`LmdbNativeParallelAggregation`), the sequential IR kernel, and the parallel IR kernel rung (`LmdbNativeParallelKernelAggregate`). Plan 31 made the kernel rung's *filter* admission match the interpreted engine's. Everything else still diverges, and the divergence is expensive in a way that is easy to miss: **a kernel-rung decline is not a handoff.** `LmdbNativeKernelExecution.tryEvaluateAggregate` calls the parallel rung, and when it declines, drains the kernel sequentially and returns a non-null result — so `LmdbNativeGroupStep`'s cost arbiter never gets to offer `LmdbNativeParallelAggregation`. Every unclosed decline therefore means single-threaded execution, not "the older tier serves it". The rows ladder has the identical structure (`LmdbNativeRowStep.openKernelInput` before `openBatchOrParallel`).

Concretely, `SELECT (COUNT(DISTINCT ?v) AS ?c) WHERE { ?s :p ?m . ?m :d ?v }` runs multi-threaded on the interpreted engine and single-threaded the moment the IR kernel tier engages.

After this plan: every remaining kernel-rung decline names a condition under which the interpreted engine also stays sequential, any decline outside that set falls through to a costed alternative rather than to a single thread, and `rdf4j.lmdb.irAggregateParallel.enabled` defaults ON behind a benchmark gate.

## Progress

- [x] (2026-08-07) M0: parity contract tests and baseline. New `LmdbNativeIrAggregateParallelDistinctTest` (4 tests, green) covers global and grouped `COUNT(DISTINCT)`, `SUM(DISTINCT)`, `AVG(DISTINCT)` over values deliberately shared across every root key. Baseline evidence in `initial-evidence.txt`; the DISTINCT gap is confirmed LIVE (see Surprises). ScanQuad-rooted tests deferred to M3 on purpose — see Decision Log.
- [x] (2026-08-07) M1: distinct-channel sidecar on the hooks SPI. `AggregateOutput.hookDistinct` (in the shape key), `KernelHooks.accumulateDistinct` as a default method, a `LongHashSet[][]` sidecar on `LmdbNativeKernelHooks`, `KernelRuntime.LongHashSet.addAll/forEach`, and emitter routing at the field/bind/grow/update/drain sites. Three new emitter tests; `LmdbNativeKernelIrEmitterTest` green at 62/62, which is also the evidence that the flag-off path is unchanged.
- [x] (2026-08-07) M2: aggregate rung merges all DISTINCT kinds. Admission accepts `AGG_COUNT_DISTINCT`/`AGG_SUM_DISTINCT`/`AGG_AVG_DISTINCT`; the worker variant rewrites them to hook-distinct; `Partial` unions id sets; COUNT is sized from the merged set and SUM/AVG folded through `accumulateNumeric` after the union. `LmdbNativeIrAggregateParallelDistinctTest` 4/4 now assert the rung ENGAGES; `LmdbNativeIrAggregateParallelTest` 7/7 with its DISTINCT test flipped; cleanup contract 36/36; rows rung 10/10.
- [ ] M3: ScanQuad-rooted partitioning, both rungs.
- [ ] M4: residual merge and gate differences.
- [ ] M5: explain and metrics parity.
- [ ] M6: cost-arbitrated dispatch.
- [ ] M7: census and executable parity ledger.
- [ ] M8: benchmarks, flag flip, single full module verify.

## Surprises & Discoveries

- Observation (2026-08-07, M0): the DISTINCT gap is live and unshadowed. With `-Drdf4j.lmdb.janinoCodegen.debug=true`, all four new parity queries reach the IR aggregate kernel and its parallel rung, which declines exactly 593× `unmergeable-aggregate-kind-2`, 298× `-9`, 297× `-10`. The older fused `LmdbNativeJaninoAggregate` rung is consulted 2400× but declines, so it does not steal these shapes — M2's flip targets are genuinely reachable.
  Evidence: `initial-evidence.txt`, and `core/sail/lmdb/target/surefire-reports/…LmdbNativeIrAggregateParallelDistinctTest-output.txt`.
- Observation (2026-08-07, M0): none of the reachable DISTINCT shapes engages the ordered-domain COUNT(DISTINCT) fast path — every compiled shape key carries `@-1` (e.g. `agg(g=;o=2:2@-1)`). The `countDistinctOrdered` lowering needs a global aggregate over a depth-0 column with an ordered domain, which these queries do not produce. M2 must still neutralize that path in the worker variant (its `agO` counter counts *runs*, so per-partition run counts are not additive), but it cannot be covered end-to-end by these tests.
  Evidence: shape-key census in `initial-evidence.txt`.
- Observation (2026-08-07, M2): the pre-existing `distinctAggregateDeclinesParallelButKeepsTheSequentialKernel` kept PASSING after DISTINCT merging landed — it was green for a stale reason. Its loop ran `while (aggOpened() == 0)`, and `AGG_OPENED` is incremented by the parallel path too, so it exited after the first round — which legitimately declines with `worker-kernel-pending` while the variant's async compile finishes. Any test that asserts a rung does NOT engage must loop on `PARALLEL_RUNS`, never on a counter the rung itself moves.
  Evidence: the test stayed green post-change; rewritten as `distinctAggregateMergesInParallel`, looping until `PARALLEL_RUNS` moves, it engages and is green.
- Observation (2026-08-07, M2): admitting DISTINCT exposed a latent HAVING bug one layer down. The consumer tested `partial.counts[having.outputIndex]`, but a DISTINCT count never writes that column — its value is the size of the merged id set — so a `HAVING COUNT(DISTINCT ?x) > n` would have compared against a constant zero. Unreachable before, because the rung declined every DISTINCT kind. Fixed with `finalCount(...)`, used by both the HAVING test and the emission.
- Observation (2026-08-07, M2): with the arithmetic deferred, the floating-point refusal MOVES threads. Workers on a hook-distinct SUM/AVG collect ids and do no arithmetic, so `EncounterOrderFallback.floatingSumOrAvg()` is no longer thrown inside a worker — it fires on the query thread during the consumer's fold, outside the merge loop's existing catch. The packing loop needed its own catch, or a floating `SUM(DISTINCT ?x)` would have propagated out as a query failure instead of declining to the sequential drain.
- Observation (2026-08-07, review): floating-point SUM/AVG is already safe in the kernel rung, but by a *different* mechanism than the interpreted engine's preflight sample. `LmdbNativeKernelHooks` builds `new AggContext(source, false, true)` — `encounterOrderChanging=true` — so `AggState.numericLiteral` throws `EncounterOrderFallback.floatingSumOrAvg()` at accumulate time in *every* kernel route, sequential included. `LmdbNativeParallelKernelAggregate.mergeNumeric` therefore needs no floating check of its own, but nothing at that site says so; the invariant lives in another file.
  Evidence: `LmdbNativeKernelHooks.java:95`, `LmdbNativeAggregateState.java:357-368`, and the green `floatingSumNeverRunsParallelAndStaysExact` test.

## Decision Log

- Decision: skip the full-module verify at M0 and take a targeted baseline instead.
  Rationale: the standing user mandate is that all execplan milestones finish before the *single* full-module verify. Running one at M0 and again at M8 would violate it. The last recorded full-green baseline (3054/0/0/6 at commit `2543eba45b`) serves as the comparison point.
  Date/Author: 2026-08-07 / Claude.
- Decision: put the DISTINCT parity tests in a new class rather than extending `LmdbNativeIrAggregateParallelTest`.
  Rationale: the cross-partition dataset needs values shared across root keys, which is the opposite of the existing class's fan-out shape. Adding predicates to the shared `setUp` would perturb adjacency statistics for seven already-green tests for no benefit.
  Date/Author: 2026-08-07 / Claude.
- Decision: defer the ScanQuad-rooted parity tests to M3 instead of writing them at M0.
  Rationale: M0's contract is "one test per gap, asserting current behavior". A guessed query that does not actually lower to a `ScanQuad` root would assert nothing while looking like coverage. M3 opens with a spike that establishes which shape produces that root, and the test is written against the answer.
  Date/Author: 2026-08-07 / Claude.
- Decision: express the worker-side DISTINCT variant as a `hookDistinct` flag on `AggregateOutput` rather than as new `AGG_*` kinds.
  Rationale: several switches over `kind` use `default:` branches — `LmdbNativeParallelKernelAggregate.mergeRow` treats `default` as `AGG_MAX_ID` — so new kinds would be silently mishandled at sites that compile cleanly. A flag forces the handful of real decision points to be updated explicitly. The flag must be added to `Aggregate.key()`, or the variant would collide with the sequential kernel in the compile cache.
  Date/Author: 2026-08-07 / Claude.
- Decision: route the worker variant's distinct sets through the hooks sidecar, mirroring SUM/AVG, instead of adding a drain accessor to the generated kernel.
  Rationale: `AGG_SUM_DISTINCT`/`AGG_AVG_DISTINCT` already pack the *group ordinal* into the emitted row and already accumulate into `LmdbNativeKernelHooks` via `accumulateNumeric`; the parallel rung already knows how to read per-group state back through that ordinal (`numericSumAt`). Reusing that protocol for distinct sets needs no new `JaninoKernel` SPI and no new drain surface — the exact objection that made plan 31 defer this gap.
  Date/Author: 2026-08-07 / Claude.

## Outcomes & Retrospective

(To be completed as milestones land.)

## Context and Orientation

All paths are relative to the repository root. Everything lives in `core/sail/lmdb`, package `org.eclipse.rdf4j.sail.lmdb.evaluation`, plus the codegen subpackage `…evaluation.codegen`.

The parity baseline is `LmdbNativeParallelAggregation` (interpreted GROUP BY) and `LmdbNativeParallelPipelines` (interpreted row scans). The kernel rungs are `LmdbNativeParallelKernelAggregate` and `LmdbNativeParallelKernelRows`; both partition a root enumeration into contiguous windows and run one kernel instance per window on the shared pool, each worker against its own pinned-snapshot source.

Debugging: `-Drdf4j.lmdb.janinoCodegen.debug=true` prints `[ir-aggregate] shape …`, `[ir-aggregate-parallel] decline: …` and `[ir-kernel-parallel] decline: …` to stderr. Combine with `-Dmaven.test.redirectTestOutputToFile=true` to capture it into `core/sail/lmdb/target/surefire-reports/<class>-output.txt`.

Build/test workflow: root install once per session, then the mvnf skill. Never `-am` or `-q` with tests. New Java files need the standard header plus `// Some portions generated by Claude`, then `cd scripts && ./checkCopyrightPresent.sh`, then `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`.

## Plan of Work

### M1 — Distinct-channel sidecar on the hooks SPI

`LmdbNativeKernelIr.AggregateOutput` gains a `hookDistinct` flag, included in `Aggregate.key()`. `KernelHooks` gains `default boolean accumulateDistinct(int aggregateId, int groupId, long valueId)` throwing `UnsupportedOperationException` (the pattern `residualSlot`/`testResidual` already set, which keeps the `TestHooks` double in `LmdbNativeKernelIrEmitterTest` compiling). `LmdbNativeKernelHooks` gains a `LongHashSet[][]` sidecar beside `numericSums`, with growth mirroring `ensureNumericCapacity`, sets sized from `bindings.distinctExpected`, plus `distinctIdsAt` / `installDistinctPartial` mirroring `numericSumAt` / `installNumericPartial`. `LmdbNativeKernelEmitter` routes through the hook when the flag is set, packs the group ordinal for `COUNT_DISTINCT` instead of the count, and skips the ordered-domain path. With the flag unset the generated source must be byte-identical — assert that in the emitter suite.

### M2 — Aggregate rung merges all DISTINCT kinds

Drop the three `unmergeable-aggregate-kind-*` declines. Extend the existing worker-variant rewrite (already used to strip HAVING and output mods) to set `hookDistinct` and clear `orderedDomain`. `Partial` gains distinct id sets; `mergeRow` reads them via the group ordinal; `mergePartial` unions with `LongHashSet.addAll`. Final emission: `COUNT_DISTINCT` = merged set size; `SUM/AVG_DISTINCT` = fold the merged set through `AggState.numericLiteral` + `AggState.addNumeric`, mirroring `AggState.addDeferredDistinctValue` — the interpreted engine's deferred-distinct semantics. **Trap:** `bindings.hooksRequired` is false for a COUNT(DISTINCT)-only query, so `needsHooks()` returns false and the variant would NPE; the rung must force a hooks object whenever the variant sets `hookDistinct`. Add the missing comment at `mergeNumeric` recording why it needs no floating check.

### M3 — ScanQuad-rooted partitioning, both rungs

Spike first: prove a windowed scanner returns exactly a partition of the full scan, and record the finding here. Then `LmdbNativeKernelPartitions.partitionableRootScan(pipeline)` (root `ScanQuad` after any `HashBuild` preamble, no other node reading that site), a per-worker `LmdbRootScanPartition` binding on `LmdbNativeKernelScanner`, and admission in both rungs. Reuse `NativeLmdbQuerySource.planRootScanPartitions` and `statements(s,p,o,c,partition)` — both already exist and already back `LmdbNativeExchange.PartitionCursor` — plus the `rangePartition*` telemetry so the census sees identical counters from both engines. One partition per kernel instance, which fits the existing per-range worker loop with no new concurrency. Write the deferred M0 ScanQuad tests here.

### M4 — Residual merge and gate differences

Overflow-checked count merge via `FactorizedTail.addCounts`. Apply `orderComparatorStrict` in the aggregate rung. Record the deliberately stricter worker-count and `root-too-small` gates in the ledger rather than loosening them — they only ever keep the faster sequential path.

### M5 — Explain and metrics parity

`PATH_IR_AGGREGATE_PARALLEL` / `PATH_IR_KERNEL_PARALLEL`; every `debugDecline` also calls `LmdbNativeAttemptMetrics.recordDecline`; a strategy label published on success. Prerequisite for M7's ledger, which must read reasons programmatically rather than scrape stderr.

### M6 — Cost-arbitrated dispatch

Split both rungs into `propose()` / `open()` following `LmdbNativeParallelAggregation.propose` and its `AggregateCandidate`, with `reservation::close` as `releaseIfUnused`. Offer the kernel-parallel and kernel-sequential proposals into the arbiters that already exist at `LmdbNativeGroupStep.evaluateParallelOrFactorized` and `LmdbNativeRowStep.openBatchOrParallel`. Behind `rdf4j.lmdb.irKernelArbitrated.enabled`, default OFF until M8. This changes strategy selection for every kernel-eligible query and is the plan's largest benchmark risk, hence it lands late and flag-guarded.

### M7 — Census and executable parity ledger

Update `LmdbNativeKernelDeclineCensusTest` and `LmdbNativeKernelAdversarialDeclineTest`. Add a ledger test asserting that every reason the kernel rungs still emit is one the interpreted engine also declines on. Update plan 31 and `.agent/three-tier-parity-execplan.md` with superseded pointers.

### M8 — Benchmarks, flag flip, single full module verify

Theme-benchmark A/B judged by disjoint error intervals, never by score alone. Flip `irAggregateParallel.enabled` and `irKernelArbitrated.enabled` to default ON only if clean. Then the single full `core/sail/lmdb` verify, aggregated from the report XMLs.

## Validation and Acceptance

Per milestone: the milestone's test flips from "declines, results correct" to "parallel run observed (`PARALLEL_RUNS` incremented), results identical to the sequential kernel and the generic evaluator". Correctness invariants every test asserts: result multiset equality against the rung disabled; exact sequence equality under ORDER BY; group-key/value equality including the empty-input global-aggregate row.

Overall: the ledger test (M7) passes, and the single full module verify is green against the 3054/0/0/6 baseline.

Concurrency is High-risk by this repo's proportionality model, so every worker-side change also runs `LmdbNativeParallelAggregationCleanupTest` — a leaked native read stamp reproduces as a dataset-close hang.

## Idempotence and Recovery

Every milestone is additive and flag-guarded. Kill switches: `rdf4j.lmdb.irAggregateParallel.enabled=false`, `rdf4j.lmdb.irKernelParallel.enabled=false`, `rdf4j.lmdb.irKernelArbitrated.enabled=false`, `rdf4j.lmdb.parallel.enabled=false`. Revert order if something misbehaves: M6 (dispatch), then M3, then M2; M1 is inert with its flag unset. Keep all untracked benchmark artifacts; never clean the protected theme-benchmark store.

## Interfaces and Dependencies

No new external dependencies. New internal surfaces at completion:

    KernelHooks.accumulateDistinct(int aggregateId, int groupId, long valueId)          (default, M1)
    LmdbNativeKernelHooks.distinctIdsAt / installDistinctPartial                        (M1)
    LmdbNativeKernelIr.AggregateOutput.hookDistinct (+ Aggregate.key())                 (M1)
    LmdbNativeKernelPartitions.partitionableRootScan(List<Node>)                        (M3)
    LmdbNativeKernelScanner root-partition binding                                      (M3)
    LmdbNativeAttemptMetrics.PATH_IR_AGGREGATE_PARALLEL / PATH_IR_KERNEL_PARALLEL       (M5)
    LmdbNativeParallelKernelAggregate.propose / LmdbNativeParallelKernelRows.propose    (M6)

New system properties: `rdf4j.lmdb.irKernelArbitrated.enabled` (M6).

---

Revision note (2026-08-07, initial version): authored after a review of `LmdbNativeParallelKernelAggregate` against `LmdbNativeParallelAggregation` found three classes of divergence — capability (DISTINCT kinds, scan roots), dispatch (a decline is not a handoff), and observability (declines invisible to explain).
