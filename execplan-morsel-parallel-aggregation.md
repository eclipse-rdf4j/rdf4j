# Intra-query morsel parallelism for native LMDB aggregation

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (from the repository root).

## Purpose / Big Picture

Nothing in the LMDB native query engine uses more than one core per query: `NativeGroupIteration.evaluateAll` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeGroupStep.java) drives the whole join and aggregation on the calling thread. Yet LMDB is exactly the storage engine where readers scale linearly — MVCC over a shared memory map, no read locks between read transactions — and aggregation roots partition perfectly: any subset of the root scan's rows can be joined and aggregated independently and the per-group counters merged at the end. After this change, a COUNT-style aggregate over a large basic graph pattern splits its *root scan* into morsels (batches of raw quads), hands them to worker threads that each run the identical join pipeline against their own LMDB read transaction and accumulate into their own group table, and merges the thread-local tables into the final result. On an N-core machine a scan- and aggregation-heavy query approaches N-fold speedup, and this multiplies with the batched-scan and factorized-tail work already on this branch (each worker uses them internally).

Observable outcome: aggregate queries return identical results with parallelism on and off (differential tests), a package-visible counter proves the parallel path engages, and a JMH benchmark shows near-linear scaling on a large star/scan aggregate.

## Progress

- [x] (2026-07-06 13:40Z) Transaction-machinery facts established (see Context); ExecPlan authored.
- [x] (2026-07-06 14:20Z) M1: `NativeLmdbQuerySource.ParallelSource` + default-null `openParallelSources(int)`; `LmdbSailDataset` implementation opening untracked read txns back-to-back under `txnManager.lockManager().readLock()` (same-snapshot by construction); `ParallelSnapshotSource` mirroring statements/newProbe/count/has/estimate over the worker txn; `CompositeNativeLmdbQuerySource.openParallelSources` zips member sources so explicit+inferred stores (the common case — this was the first engagement bug) support parallelism.
- [x] (2026-07-06 14:50Z) M2: `LmdbNativeParallelAggregation` (gates, morsel queue with poison pills and failure-flag abort, per-worker RowState/AggContext/FactorizedTail, thread-local group tables, uninterruptible future collection so worker txns never close under a live worker, merge via `AggState.mergeCountsFrom`/`LongHashSet.addAll`); `MultiJoinPlan.openChainFrom` for the worker-side chain; bare `PatternPlan` aggregation roots wrapped as one-child MultiJoinPlans so single-scan COUNTs parallelize too.
- [x] (2026-07-06 15:10Z) M3: `LmdbNativeParallelAggregationTest` 12/12 green (three-way differential: parallel vs sequential-native vs generic; engagement via PARALLEL_RUNS; filter gate and default-threshold gate proven). Full module suite: 1354 tests, only the pre-existing `LmdbEvaluationStatisticsMemoizationTest` failure.
- [x] (2026-07-06 15:40Z) M4: `ParallelAggregationBenchmark` (10k hubs × fan-outs, 300k triples), 16-core machine, threads default (15): countHub 20.9→9.2 ms (2.3x), countDistinctTail 23.5→14.2 ms (1.7x), groupByHub 25.1→13.8 ms (1.8x). Below the ≥3x target but above the 1.5x floor — see Outcomes for the Amdahl analysis and follow-ups.

## Surprises & Discoveries

- Observation: tracked read txns are *reset and renewed on every write commit* (`TxnManager.reset()`, resettableActiveTransactions), so today's single-threaded queries already tolerate a snapshot change mid-query via per-iterator version checks. Untracked read txns (`createReadTxnUntracked`) skip that reset and pin their snapshot for their lifetime — which is exactly what parallel workers need for cross-worker consistency, and is *stronger* than the status quo.
  Evidence: TxnManager.java:209-224 (untracked semantics), LmdbRecordIterator.java txnRefVersion checks.

## Decision Log

- Decision: producer/consumer morsels over the root scan (query thread fills `long[]` quad batches from the depth-0 pattern; workers consume) rather than key-range partitioning of the index.
  Rationale: raw quads are plain longs, safe to hand across threads; the root scan is sequential and cheap relative to join work; no index-shape assumptions; the queue provides natural load balancing (the whole point of morsel-driven scheduling).
  Date/Author: 2026-07-06, Claude Code.
- Decision: every worker (and the root scan itself) uses a fresh *untracked* read-only txn, all opened while holding one `txnManager.lockManager().readLock()` stamp.
  Rationale: writers commit under that manager's `writeLock()` (TripleStore.java:404-420), so no commit can interleave with the opens — all txns observe the identical last-committed LMDB snapshot by construction, and untracked txns keep it for the query's lifetime. No version comparison needed.
  Date/Author: 2026-07-06, Claude Code.
- Decision: v1 gates — parallelism engages only when: the group arg is a `MultiJoinPlan` whose ordered children are all `PatternPlan`s; the plan has **no filters** (compiled filter objects such as `CachedCompareFilter`/`ExistsFilter` carry per-instance memo state and are not thread-safe to share); every `AggregateSpec` is COUNT-kind (`allCounts()`); no synthetic VALUES ids (the step source must be the plain `LmdbSailDataset`); the root pattern's estimate meets a threshold. Widening to stateless ID filters and per-worker-compiled filters is a recorded follow-up.
  Rationale: correctness first; each gate is an independently testable widening, mirroring how the factorized tail landed.
  Date/Author: 2026-07-06, Claude Code.
- Decision: configuration via system properties read per evaluation (not class-load): `rdf4j.lmdb.parallel.enabled` (default true), `rdf4j.lmdb.parallel.threads` (default `availableProcessors()-1`, min 1), `rdf4j.lmdb.parallel.minRootEstimate` (default 50000 — below it the sequential path runs).
  Rationale: per-evaluation reads let tests toggle without JVM restarts; the threshold keeps small queries away from queue/thread overhead.
  Date/Author: 2026-07-06, Claude Code.
- Decision: workers each create their own `FactorizedTail` (and probe/cursor state) over the shared immutable `OrderedPlan`; only `AggState` tables are merged.
  Rationale: plans and patterns are immutable and shareable; all mutable state (RowState, probes, memos, group tables) stays thread-confined; merge is COUNT addition plus `LongHashSet` union.
  Date/Author: 2026-07-06, Claude Code.

## Outcomes & Retrospective

All milestones landed; results correct under a three-way differential oracle and engagement proven. The measured speedup (1.7–2.3x on 16 cores) is real but far from linear: the root scan is a single-threaded producer, and the per-worker join work is already so cheap after the factorized-tail and batched-scan work that queue handoff and thread-local table construction are a visible fraction of the runtime — Amdahl caps the composition on this dataset. The architecture is in place and correct; scaling further is an optimization exercise, recorded as follow-ups: (1) partition the root scan itself (per-worker index sub-ranges instead of one producer), (2) larger/adaptive morsel sizes, (3) widen the filter gate to stateless ID filters, (4) worker-count tuning for hybrid P/E-core machines, (5) non-COUNT aggregate merges (SUM/MIN/MAX with value-typed merge), (6) pin the composite explicit+inferred member snapshots under one lock section instead of two.

## Artifacts and Notes (results)

ParallelAggregationBenchmark, avgt ms/op, 4+4 iterations, single fork, 16-core machine (Apple Silicon), default thread count (cores-1 = 15), 10,000 hubs × (p1 fan-out 10 + p2 fan-out 20) = 300k triples, counts verified per invocation; ON = defaults, OFF = -Drdf4j.lmdb.parallel.enabled=false:

    variant             ON (ms)          OFF (ms)         speedup
    countHub             9.195 ± 0.205   20.868 ± 0.532   2.3x
    countDistinctTail   14.248 ± 0.873   23.494 ± 4.083   1.7x
    groupByHub          13.784 ± 0.515   25.130 ± 1.179   1.8x

Logs: session scratchpad bench-parallel-on.log / bench-parallel-off.log. Test evidence: LmdbNativeParallelAggregationTest `tests=12, failures=0, errors=0` (logs/mvnf/20260706-115528-verify.log); full module 1354/1/0/3 with the failure pre-existing at HEAD.

## Context and Orientation

All paths repository-relative; module `core/sail/lmdb`, package `org.eclipse.rdf4j.sail.lmdb`. The native aggregate engine compiles `SELECT (COUNT(...) ...) WHERE {...} [GROUP BY ...]` into `NativeGroupStep` (LmdbNativeGroupStep.java) whose `NativeGroupIteration.evaluateAll` opens the compiled `SlotPlan` (usually a `MultiJoinPlan` of `PatternPlan`s, LmdbNativeJoinPlans.java) as a cursor chain and feeds each row into `AggState` (LmdbNativeAggregateState.java) — one `long` counter per non-distinct COUNT and one `LongHashSet` per distinct one — keyed by a `GroupKey` (or a `LongAggStateMap` for single-slot GROUP BY). `MultiJoinPlan.derivedPlan(RowState)` memoizes the join order and filter placement per bound-slot mask; `openChain(derived, upTo, row)` builds the cursor chain. The store side: `LmdbSailStore.LmdbSailDataset` implements `NativeLmdbQuerySource` (statements/probes over 64-bit ids) holding one `TxnManager.Txn` read transaction; `TripleStore.CursorPool` pools LMDB cursors per thread and per dbi, keyed to read-only txns; every `LmdbRecordIterator.next/fill` runs under `txnManager.lockManager()`'s read lock, and writers commit under its write lock. A *morsel* is one batch of raw root-scan quads (`long[4*1024]` plus a row count) handed to a worker as a unit of work.

## Plan of Work

Milestone 1 — worker snapshots. Add to `NativeLmdbQuerySource` a default method `NativeLmdbQuerySource[] openParallelSources(int count)` returning null (unsupported). Implement it on `LmdbSailDataset`: acquire `tripleStore.getTxnManager().lockManager().readLock()`; open `count` untracked read txns (`createReadTxnUntracked`); release the lock; return lightweight `NativeLmdbQuerySource` instances that mirror `LmdbSailDataset`'s statements/newProbe/has/count/estimate over the new txn (same `explicit` flag, same valueStore for idOf/lazyValue — the value store is append-only for readers and safe to share) and whose `close()` closes their txn. `CompositeNativeLmdbQuerySource` and `SyntheticValueSource` inherit the null default, gating themselves out.

Milestone 2 — parallel evaluation. In `NativeGroupIteration.evaluateAll`, after the sequential fast paths are chosen, test the gates (Decision Log); on success run: query thread opens `sources[0]`'s scan of the depth-0 ordered pattern via `PatternPlan.openRaw` + `PatternCursor.fill` and enqueues morsels into an `ArrayBlockingQueue` (capacity 2× workers, poison pill per worker at end); each worker (static daemon pool, `lmdb-native-parallel-*`) builds its own `RowState` over its own source, seeds it identically (`initializeRow` with the same base bindings, ids translated? — ids are store-global, no translation needed), and runs a `MorselRowCursor` (binds each morsel quad through `pattern.bind`) wrapped by the chain for depths 1..n-1 (new `MultiJoinPlan.openChainFrom(derived, leftmost, row)` mirroring `openChain` but with a caller-supplied leftmost cursor), feeding rows into a worker-local group table through the same aggregation helpers as the sequential loop (including a per-worker `FactorizedTail` when it applies). On completion the query thread merges worker tables: `AggState.mergeCountsFrom(other)` (counts add, distinct sets union via a new `LongHashSet.addAll`). Any worker exception cancels the run (drain queue, close sources) and rethrows; a `PARALLEL_RUNS` AtomicLong counter records engagement.

Milestone 3 — tests. `LmdbNativeParallelAggregationTest`: differential vs the generic evaluator (`rdf4j.lmdb.nativeQueryEngine.enabled=false`) *and* vs the sequential native path (`rdf4j.lmdb.parallel.enabled=false`), over a dataset large enough to pass a test-lowered threshold (`rdf4j.lmdb.parallel.minRootEstimate=0`, restored in finally): plain COUNT, COUNT DISTINCT, GROUP BY single slot, GROUP BY multi slot, zero-match branches, named-graph patterns, a filtered query (gate → sequential, counter must not grow), correlated seeds. Engagement asserted via `PARALLEL_RUNS`. Full module verify must stay at the pre-existing failure count only.

Milestone 4 — benchmark. `ParallelAggregationBenchmark` (benchmark-only): ~2M-triple star (10k hubs × two predicates × fanouts), COUNT and GROUP BY variants, run with parallelism on vs off via `-Drdf4j.lmdb.parallel.enabled`; record the table in Artifacts with the machine's core count.

## Concrete Steps

From the repository root: compile `mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Pquick install`; test `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelAggregationTest --retain-logs`; full module `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs`; benchmark `scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ParallelAggregationBenchmark --method executeQuery` twice (second with `--no-build --jvm-arg -Drdf4j.lmdb.parallel.enabled=false`). Format + headers per house rules before finishing.

## Validation and Acceptance

`LmdbNativeParallelAggregationTest` green with both oracles; `PARALLEL_RUNS` grows for gated-in queries and not for gated-out ones; full lmdb unit suite shows no new failures beyond the pre-existing `LmdbEvaluationStatisticsMemoizationTest.recordsLearnedFilterPassRatioForExternalBoundPatternLocalFilter`; the benchmark shows a multi-core speedup on the COUNT variants (target ≥3x on ≥8 cores for the scan-heavy variant; anything below 1.5x means the gates or morsel size need revisiting before acceptance).

## Idempotence and Recovery

All changes additive behind per-evaluation gates; `rdf4j.lmdb.parallel.enabled=false` restores today's sequential execution exactly. Worker txns and sources close in finally blocks; a failed run closes everything before rethrowing. Re-running builds/tests is safe.

## Artifacts and Notes

(Evidence and benchmark tables to be added as milestones complete.)

## Interfaces and Dependencies

No new libraries. End state:

    // NativeLmdbQuerySource (NativeLmdbQuerySource.java)
    default NativeLmdbQuerySource[] openParallelSources(int count) { return null; }  // null = unsupported

    // LmdbNativeGroupStep.java
    static final AtomicLong PARALLEL_RUNS;   // engagement counter

    // LmdbNativeJoinPlans.java, on MultiJoinPlan
    RowCursor openChainFrom(OrderedPlan plan, RowCursor leftmost, RowState row) throws IOException;

    // LmdbNativeAggregateState.java
    void AggState.mergeCountsFrom(AggState other);   // COUNT-only merge: counts add, distinct sets union
    void LongHashSet.addAll(LongHashSet other);

Plan revision note (2026-07-06): initial version, grounded in TxnManager/TripleStore lock facts (writers commit under lockManager.writeLock(); untracked read txns pin their snapshot).
