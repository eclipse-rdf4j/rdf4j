# Range-partitioned parallel root scans for the LMDB native engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (repository root relative path).

## Purpose / Big Picture

The LMDB native query engine can already evaluate large SPARQL queries in parallel: one producer thread scans the query's "root" triple pattern and feeds raw quads through a bounded queue to N worker threads, which run the rest of the join pipeline. The producer is a bottleneck — the scan itself is serial, every quad crosses a thread boundary through an `ArrayBlockingQueue`, and profiling shows queue offer/poll and the producer's cursor `fill` dominating scan-heavy queries.

After this change, when the feature flag `rdf4j.lmdb.parallel.rangePartition.enabled` is set to `true`, an eligible parallel query splits the root pattern's LMDB key range into disjoint subranges ("partitions"), and each worker thread scans its own partitions directly with its own LMDB cursor — no producer thread, no queue for input quads. A user can see it working by running any large parallel query with the flag on and `explain(Telemetry)`: the reported strategy string becomes `parallelPipelines(rangePartitioned=N)` (or `parallelAggregation(rangePartitioned=N)`), and the JMH benchmarks in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ParallelAggregationBenchmark.java` get faster because the scan now scales with cores.

Everything is delivered behind the flag (default off). With the flag off, behavior and query plans are byte-identical to today.

## Progress

- [x] (2026-07-15 21:20Z) Design finalized (three exploration passes + one design pass over the current tree; all anchors verified this session).
- [x] (2026-07-15 21:22Z) M1: `LmdbKeyRange` + bounded `LmdbRecordIterator` constructor (+`upperExclusive`, guard removals, reset clears the flag) + `TripleStore.getTriplesRange` + `LmdbRecordIteratorRangeBoundsTest` (9 tests green, incl. the previously uncovered txn-renew path); existing seek (4) and skip-scan (3) suites green after the guard removals.
- [x] (2026-07-15 21:27Z) M2: `TripleStore.planRootSplitKeys` (interpolate + snap + dedupe) + `LmdbRootScanPartition` + `NativeLmdbQuerySource` default methods, implemented on `LmdbSailDataset`, `ParallelSnapshotSource` (both via shared `toRootScanPartitions`) and `CompositeNativeLmdbQuerySource` (+`CompositeParallelSource` delegation). `TripleStoreRangePartitionTest` 6 tests green (uniform, clustered/skew, few-keys refusal, empty range, inferred db, bound prefix; all with the tiling property check).
- [x] (2026-07-15 21:52Z) M3: `PatternPlan.partitionableScanQuad` (openRaw-mirroring resolution + shape gates); `LmdbNativeExchange.tryPlanRootPartitions` + `PartitionCursor` (dynamic partition queue, per-partition bounded scans, empty-partition counter); pipelines integration (plan before reservation, `threads` tasks/sources, no producer, `strategyLabel()` → `parallelPipelines(rangePartitioned=N)`); aggregation integration (sample-only preflight on the query thread's source, `threads` sources, no produce call, thread-local `consumeLastStrategyLabel()` → `parallelAggregation(rangePartitioned=N)`); flag `rdf4j.lmdb.parallel.rangePartition.enabled` (default off) + `.factor` (default 4); counters `RANGE_PARTITION_RUNS/RANGE_PARTITIONS_PLANNED/RANGE_PARTITIONS_EMPTY/RANGE_SPLIT_PLANNING_NANOS` + `LAST_RANGE_REJECTION`. Tests: new `LmdbNativeRangePartitionedScanTest` (5 green: chain parity across ranged/morsel/sequential/generic, GROUP BY parity, flag-off rejection reason, skew parity, composite explicit+inferred parity); existing parallel suites green (15+28+35+4); differential fuzz green with the flag forced on + minRootEstimate=0 (18 tests).
- [ ] Full module verify (`mvnf core/sail/lmdb`) — pending (one pre-existing pathologically slow
      sketch-estimator test wedges the module run; documented in Surprises, chip spawned).
- [ ] M4 in progress (2026-07-16 session): local JMH evidence on theme MEDICAL q8 (the correlated-entry
      guard query) shows NO regression with the flag on — baseline 69.8±7.5 ms, rangePartition 71.8±2.0 ms,
      csr+range 68.2±8.9 ms (all within error; q8's root is below the 50k gate so partitioning correctly
      does not engage). Engagement-shaped runs (ParallelAggregationBenchmark countHub morsel vs ranged)
      running; docker-loop validation and the default-flip decision remain open.

## Surprises & Discoveries

- Observation: The triple indexes are opened with `MDB_CREATE` only — no `MDB_DUPSORT` (`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleIndex.java:82-87`). Every quad is one key; values are empty.
  Evidence: `mdb_dbi_open(txn, name, MDB_CREATE, dbiPtr)` in `TripleIndex` constructor. Consequence: half-open key ranges tile the index with no duplicate/gap hazard; a split key can never fall "inside" a duplicate run.
- Observation: `isOutOfRange()` already returns `false` when `maxKeyBuf == null`, so the `rangePrefixLength == 0 &&` guards at its two call sites are redundant today and can be dropped to let an injected upper bound coexist with a bound key prefix.

## Decision Log

- Decision: Split points are computed by interpolated `MDB_SET_RANGE` seeks that snap to actual keys (a strip-down of `cardinalityUsingSamplingEstimator`), not by parsing B-tree branch pages.
  Rationale: reuses proven code (`TripleStore.bucketStart`), costs 2+(P−1) seeks (sub-millisecond against the ≥50k-row gate), and needs no config-gated page estimator. B-tree branch-page splits via `LmdbBtreeRangeCounter` remain the documented upgrade if benchmarks show skew-driven straggling.
  Date/Author: 2026-07-15 / Claude (approved plan).
- Decision: Over-partition 4× the worker count into a shared `ConcurrentLinkedQueue`; workers pull partitions dynamically.
  Rationale: the shared morsel queue we remove *was* the load balancer; its replacement must come from partition granularity. Fixed one-partition-per-worker serializes the tail on the densest subrange. 4× bounds worst-case static imbalance at ~25% with negligible planning overhead.
  Date/Author: 2026-07-15 / Claude (approved plan).
- Decision: In partition mode the aggregation floating SUM/AVG preflight becomes sample-only: it runs the same probe logic over a throwaway cursor's first morsel and closes it, instead of transferring the open cursor to the producer (`PreparedRootScan` stays morsel-mode-only).
  Rationale: there is no producer to hand the cursor to; re-scanning 1024 rows is noise against ≥50k; the real guarantee (dynamic worker fallback for late floating literals) is mode-independent.
  Date/Author: 2026-07-15 / Claude (approved plan).
- Decision: Partition mode is attempted only after every existing parallel gate has passed, and any refusal falls back to morsel mode transparently (recorded in `LAST_RANGE_REJECTION`, which is deliberately distinct from `LAST_REJECTION` — the query still runs parallel).
  Rationale: keeps flag-off and refusal behavior byte-identical to today; the entry-bound-awareness lesson from phase 2 (q8 regression) demands that new engagement paths never add per-outer-row setup cost.
  Date/Author: 2026-07-15 / Claude (approved plan).

## Outcomes & Retrospective

(To be filled at milestone completions.)

## Context and Orientation

All paths are relative to the repository root. The module is `core/sail/lmdb`; the main source root is `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/` (below, `…/lmdb/`), and the query-engine subpackage is `…/lmdb/evaluation/`.

Key terms, defined plainly:

- **LMDB**: an embedded B+tree key-value store accessed over JNI via LWJGL (`org.lwjgl.util.lmdb.LMDB`). The RDF store keeps each quad (subject, predicate, object, context — four 64-bit ids) as one key in several index orderings.
- **Triple index**: a class (`…/lmdb/TripleIndex.java`) representing one key ordering, named by its field sequence, e.g. `spoc` or `posc` (default set: `spoc,posc`, `…/lmdb/TripleStore.java:135`). A key is the four ids written as order-preserving unsigned varints in the index's field order (`TripleIndex.toKey`, `TripleIndex.java:349-365`). `getMinKey`/`getMaxKey` (`:288-302`) substitute 0 / `Long.MAX_VALUE` for unbound fields; `getRangePrefixLength` (`:246-286`) counts leading bound fields.
- **Root pattern / root scan**: the first (leftmost) triple pattern of a parallel query plan, whose full scan feeds all downstream join work. Physically it is a `PatternPlan` (`…/lmdb/evaluation/LmdbNativePatternPlan.java`); the scan is an `LmdbRecordIterator`.
- **`LmdbRecordIterator`** (`…/lmdb/LmdbRecordIterator.java`): the cursor wrapper that positions with `MDB_SET_RANGE` at a computed `minKeyBuf`, steps with `MDB_NEXT`, decodes keys into `long[4]` quads, and (only when no leading field is bound, `rangePrefixLength == 0`) enforces an upper bound `maxKeyBuf` in Java via `isOutOfRange()` (`:477-495`, an unsigned byte-wise compare). `fill(long[] buffer, int maxRows)` (`:311-407`) is the batch read. `seekForward` (`:426-440`) re-aims the lower bound. On transaction version change it renews the cursor and repositions (`:234-259`, `:336-358`).
- **Morsel exchange**: `…/lmdb/evaluation/LmdbNativeExchange.java`. `produceMorsels` (`:44-64`) is the single producer loop; `MorselCursor` (`:129-207`) is the worker-side cursor that re-binds raw quads into the worker's `RowState`. `MORSEL_ROWS = 1024` (`:35`).
- **Parallel frameworks**: `…/lmdb/evaluation/LmdbNativeParallelPipelines.java` (SELECT rows; `tryOpen` gates at `:112-186`, worker loop `runWorker` `:518-581`, producer submission `start()` `:382-415`, `produce()` `:507-516`) and `…/lmdb/evaluation/LmdbNativeParallelAggregation.java` (GROUP BY; `tryEvaluate` gates `:81-123`, worker `:603-677`, producer-on-query-thread `:499-506`, floating SUM/AVG preflight `prepareFloatingRootSample` `:206-257` with `PreparedRootScan` transfer `:394-411`).
- **Snapshot family**: `LmdbSailStore.LmdbSailDataset.openParallelSources(count)` (`…/lmdb/LmdbSailStore.java:3273-3351`) opens `count` untracked read transactions, verifies they all have the same `mdb_txn_id`, and wraps them in `ParallelSnapshotSource`s sharing a refcounted `ParallelSnapshotLease` (`:2784-2816`) that holds a read stamp so commits cannot interleave. It refuses (returns null) when the dataset has uncommitted local writes (`storeTxnStarted`, `:3276-3278`). The composite variant cross-checks snapshot ids across explicit/inferred member sources (`…/lmdb/evaluation/CompositeNativeLmdbQuerySource.java:194-306`).
- **Gates already enforced** (must all still pass before partition mode is even attempted): flag `rdf4j.lmdb.parallel.enabled`; no LIMIT/OFFSET slice; root plan is a `MultiJoinPlan` of ≥2 `PatternPlan` children; **not** a correlated entry (`hasRuntimeBoundSlot`); ≥2 configured threads; root estimate ≥ `rdf4j.lmdb.parallel.minRootEstimate` (default 50,000); forkable filters; task budget reservation (`TaskReservation`, CAS on `RESERVED_TASKS`).

Current behavior to replace (only when the new flag is on and planning succeeds): pipelines reserve `threads+1` pool tasks and open `threads+1` snapshot sources — the extra one is the producer (`LmdbNativeParallelPipelines.java:152, :162`); aggregation reserves `threads` and opens `threads+1` (producer runs on the query thread, `LmdbNativeParallelAggregation.java:109, :123`).

## Plan of Work

### Milestone 1 — bounded iterator + TripleStore API (independent; behavior-neutral when unused)

Scope: after this milestone the storage layer can open a triple-pattern scan constrained to an arbitrary raw key subrange `[lowKey, highKeyExclusive)`. Nothing uses it yet in production; new unit tests prove it.

1. New file `…/lmdb/LmdbKeyRange.java` (package `org.eclipse.rdf4j.sail.lmdb`, annotated `@InternalUseOnly` like `LmdbPrefixRunPlan`): an immutable holder of `byte[] lowKey` (inclusive; null = the pattern's natural start), `byte[] highKeyExclusive` (null = natural end), and `String indexFieldSeq` (e.g. `"posc"`; lets the executor assert it re-derived the same index the planner used).
2. `…/lmdb/LmdbRecordIterator.java`:
   - Add field `private final boolean upperExclusive;` (existing constructors set it `false`).
   - Add a constructor taking the existing arguments plus `LmdbKeyRange range`. It runs the existing setup, then: if `range.lowKey != null`, overwrite `minKeyBuf` with those bytes; if `range.highKeyExclusive != null`, set `maxKeyBuf` to those bytes and `upperExclusive = true` (this is the generalization: today `maxKeyBuf` exists only when `rangePrefixLength == 0`, `:147-154`). `rangePrefixLength` and the `keyToQuadMatchStatus` prefix exit stay exactly as computed from the pattern — they remain an independent, second termination condition and can never terminate early-but-wrong (any key outside a bound prefix is byte-wise greater than every in-prefix split key, so `isOutOfRange` fires first).
   - In `isOutOfRange()` change the final comparison so exclusive bounds treat equality as out of range: `return upperExclusive ? keyLength >= maxLength : keyLength > maxLength;` — with LMDB's unsigned, shorter-prefix-first ordering this is exactly "key ≥ bound → out". (Read the method first; the comparison variable names may differ — the semantic change is: on byte-equal prefixes, an exclusive bound excludes the exact bound key.)
   - Drop the `rangePrefixLength == 0 &&` conjunct guarding the `isOutOfRange()` calls at the two call sites (`next()` around `:278`, `fill()` around `:377`). Behavior-neutral today because `maxKeyBuf == null` short-circuits to false; required so injected bounds work under a bound prefix.
3. `…/lmdb/TripleStore.java`: new method `RecordIterator getTriplesRange(Txn txn, long subj, long pred, long obj, long context, boolean explicit, LmdbKeyRange range)` next to `getTriples` (`:1455`). It re-derives the index by bound mask exactly like `getTriples`, throws `IllegalStateException` if `index.toString()` differs from `range.indexFieldSeq`, and constructs the bounded `LmdbRecordIterator`.
4. Tests: new `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIteratorRangeBoundsTest.java`, modeled on the existing seek/skip-scan tests in the same package. Cases: (a) prefix-bound pattern (`?s rdf:type ?o` shape on posc) with bounds strictly inside the prefix subrange; (b) exclusive boundary — a split key equal to an existing key appears only in the right-hand partition; (c) concatenation property — hand-build 3–5 ranges covering the natural range, assert the concatenated scan equals the unbounded scan (order and multiset); (d) skip-scan crossing a boundary (bind a non-leading field to force `KEY_FILTERED` runs near the bound); (e) `seekForward` within bounds; (f) bounds survive a txn-version renew (commit a write on another connection mid-iteration where the harness allows).

New Java files get the standard copyright header (current year 2026) and the agent signature comment convention used in this repo.

### Milestone 2 — split planner + source plumbing

Scope: after this milestone the store can propose disjoint partitions for a root pattern, and every `NativeLmdbQuerySource` implementation can open a bounded scan for one partition; still nothing engages in production.

1. `…/lmdb/TripleStore.java`: new method `List<byte[]> planRootSplitKeys(Txn txn, long subj, long pred, long obj, long context, boolean explicit, int targetPartitions)`. Algorithm (a strip-down of `cardinalityUsingSamplingEstimator`, `:2346-2511`): open one temp cursor; `MDB_SET_RANGE(getMinKey)` → `firstKey` (decode ids via `Varint.readListUnsigned`); `MDB_SET_RANGE(getMaxKey)` then `MDB_PREV` (the `:2408-2416` dance) → `lastKey`. For `i` in `1..targetPartitions-1`: interpolate with `bucketStart(i/(double)target, firstValues, lastValues, startValues)` (`:2148-2160` — interpolates the first differing field, which under a bound prefix is automatically the first varying one), write the candidate key with `Varint.writeListUnsigned`, `MDB_SET_RANGE`, and record a copy of the **actual key found**. Dedupe adjacent equal keys; drop splits ≤ `firstKey` or > `lastKey`. Return survivors (possibly empty ⇒ caller refuses partitioning).
2. New file `…/lmdb/LmdbRootScanPartition.java`: immutable `{ int member; LmdbKeyRange range; }` — `member` is the composite-source ordinal (0 for plain stores).
3. `…/lmdb/evaluation/NativeLmdbQuerySource.java`: two default methods —
   `default LmdbRootScanPartition[] planRootScanPartitions(long subj, long pred, long obj, long context, int targetPartitions) throws IOException { return null; }` (null = unsupported/refused) and
   `default RecordIterator statements(long subj, long pred, long obj, long context, LmdbRootScanPartition partition) throws IOException { throw new UnsupportedOperationException(); }`.
4. Implement both on the plain dataset source and on `ParallelSnapshotSource` (`…/lmdb/LmdbSailStore.java:2894-2899` area): plan → `tripleStore.planRootSplitKeys(txn, …)` wrapped into `[null,k1), [k1,k2), …, [kn,null)` ranges tagged with the executing index's field sequence; open → `tripleStore.getTriplesRange(txn, …, partition.range)`.
5. Implement on `CompositeNativeLmdbQuerySource` (and its composite ParallelSource): fan planning out to `activeSources`, re-tagging `member` with each source's ordinal; refuse (return null) if any active member refuses. `statements(partition)` dispatches on `member`. Member ordinals are stable across snapshot siblings because composite `openParallelSources` builds them in `activeSources` order — assert the member count matches when opening.
6. Tests: new `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/TripleStoreRangePartitionTest.java`: uniform ids; clustered ids (keys sharing long prefixes); fewer distinct keys than targets (dedupe → fewer/zero splits → empty result); outputs sorted and strictly inside the range; tiling property — scan every partition via `getTriplesRange` and diff the union against `getTriples` (both explicit and inferred DBs).

### Milestone 3 — exchange partition mode

Scope: after this milestone, with `rdf4j.lmdb.parallel.rangePartition.enabled=true`, eligible parallel queries run producer-less over partitions; with the flag off nothing changes. Every refusal transparently falls back to today's morsel mode.

1. `…/lmdb/evaluation/LmdbNativeExchange.java`:
   - New `static final class PartitionCursor implements RowCursor`: constructor `(Queue<LmdbRootScanPartition> partitions, PatternPlan root, RowState row, AtomicReference<Throwable> failure, AtomicBoolean cancelled)`. It resolves the root's four id terms once via the same lookups `openRaw` uses (`LmdbNativePatternPlan.java:175-178`; identical on every worker because correlated entries are refused and worker seeding asserts equality). `next()` is byte-for-byte `MorselCursor`'s bind/mark/rollback loop (`:155-182`) over a worker-local `long[4 * MORSEL_ROWS]` buffer, refilled by `scan.fill(quads, MORSEL_ROWS)`; when a partition's iterator is exhausted, close it, `partitions.poll()` the next, open via `row.source.statements(s, p, o, c, partition)`; null poll ⇒ done. Check `throwIfAborted(failure, cancelled)` between refills. `close()` rolls back the row mark and closes the current scan (the queue is shared — other workers keep draining it).
   - New static helper `LmdbRootScanPartition[] tryPlanRootPartitions(NativeLmdbQuerySource source, PatternPlan root, RowState row, int targetPartitions)` owning the structural gates: flag on; `root.statementOrder == null`; single-range root — refuse fixed multi-context concatenations (`LmdbNativePatternPlan.java:186-193`); planner returned ≥ 2 usable ranges. Every refusal records a reason (see step 4) and returns null.
2. `…/lmdb/evaluation/LmdbNativeParallelPipelines.java`: in `tryOpen` after all existing gates and before reservation, attempt `tryPlanRootPartitions(step.source, root, consumerRow, threads * factor)` where `factor` = `rdf4j.lmdb.parallel.rangePartition.factor` (default 4) and target is clamped to `[threads, min(64, rootEstimate / 8192)]`. Partition mode: `tryReserveTasks(threads)` (not `threads+1`), `openParallelSources(threads)` (not `threads+1`), `ParallelRowCursor` gains a `partitions` queue field, `start()` submits only workers, latch counts `workerPlans.length`, `produce()` not invoked, and `runWorker` picks `new PartitionCursor(...)` instead of `new MorselCursor(...)` at the `leftmost` assignment (`:531`). Everything else (per-worker plan fork, prefix strategies, output pages, entrySlots restore, metrics children commit, failure/cancel plumbing) is untouched.
3. `…/lmdb/evaluation/LmdbNativeParallelAggregation.java`: same swap at the worker's `leftmost` (`:626`); plan partitions on the query thread with the step's source before `openParallelSources`; open `threads` sources (not `threads+1`); skip `produce(...)`. The floating SUM/AVG preflight becomes sample-only: extract the body of `prepareFloatingRootSample` (`:206-257`) into a shared method with a transfer/no-transfer switch; partition mode runs it on a throwaway `root.openRaw(sampleRow)` cursor over the first morsel and closes it (partition 0's worker will re-read those 1024 rows — noise at ≥50k); `EncounterOrderFallback` propagation is unchanged. Refuse partition mode when the query-thread seed fails, mirroring the "unseedable base ⇒ poison only" behavior of morsel mode.
4. Observability: static `AtomicReference<String> LAST_RANGE_REJECTION` on `LmdbNativeParallelPipelines` (values: `flag-off`, `ordered-root`, `multi-context-root`, `planner-refused`, `too-few-splits`, `composite-member-refused`, `seed-unavailable`); `AtomicLong` counters `RANGE_PARTITION_RUNS`, `RANGE_PARTITIONS_PLANNED`, `RANGE_PARTITIONS_EMPTY`, `RANGE_SPLIT_PLANNING_NANOS`. Explain: pipelines cursor exposes a strategy label — `parallelPipelines(rangePartitioned=N)` only when engaged; aggregation defers `parallelAggregation(rangePartitioned=N)` through its existing metrics/strategy path. Flag-off plan snapshots stay byte-identical.
5. Tests: flag-on variants (system property set/cleared in try/finally or via `@BeforeEach`/`@AfterEach`) of `LmdbNativeParallelPipelinesTest` and `LmdbNativeParallelAggregationTest` asserting identical results vs flag-off plus `RANGE_PARTITION_RUNS` ticked; pinned `LAST_RANGE_REJECTION` for refusal shapes (ordered root, multi-context root, tiny range); a heavy-skew fixture; a flag-on round in `LmdbNativeDifferentialFuzzTest`; partition-mode variants of the cleanup and preflight-failure suites (they assert no leaked pool tasks/txns and preflight fallback).

### Milestone 4 — benchmark validation

Run `ParallelAggregationBenchmark` and `ThemeQueryBenchmark` with `-Drdf4j.lmdb.parallel.rangePartition.enabled=true` through `scripts/run-single-benchmark.sh` (and the docker-jfr-benchmark-loop skill for JFR): acceptance is a clear scan-scaling improvement on aggregation-over-large-roots and JFR showing the queue offer/poll and producer `fill` frames gone from the hot set, with no regression on morsel-mode-refused shapes. Record numbers in `profiles/lmdb-opt/jmh/` and the flip-or-not decision in this file's Decision Log. Revisit `rdf4j.lmdb.parallel.minRootEstimate` (50k) afterwards.

## Concrete Steps

All commands run at the repository root.

    # once per session
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{s=1} s{print}'

    # formatting + header check before finalizing any milestone
    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

    # targeted tests (examples)
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRecordIteratorRangeBoundsTest
    python3 .codex/skills/mvnf/scripts/mvnf.py TripleStoreRangePartitionTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelPipelinesTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelAggregationTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb

Expected: each selection reports `Tests run: N, Failures: 0, Errors: 0` in the printed Surefire summary. Never pass `-am` or `-q` to test runs.

## Validation and Acceptance

- M1: `LmdbRecordIteratorRangeBoundsTest` passes; `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` green (no behavior change elsewhere — the two guard removals are pinned by the existing seek/skip-scan suites).
- M2: `TripleStoreRangePartitionTest` tiling property green on both explicit and inferred DBs.
- M3: flag-on parity suites green; with the flag off, `LmdbImprovedQueryPlanSnapshotIT`/plan snapshots byte-identical; differential fuzz green in both flag states. Behavior check: a large `GROUP BY` query on a theme dataset with the flag on reports `parallelAggregation(rangePartitioned=N)` with N ≥ 2 in `explain(Telemetry)` and returns results identical to flag-off.
- M4: benchmark deltas recorded under `profiles/lmdb-opt/jmh/`; decision logged.

## Idempotence and Recovery

Every milestone is additive and flag-gated; re-running any step is safe. If a milestone must be abandoned mid-way, `git status --short --untracked-files=no` shows only the new/modified files listed above — revert them and the tree is back to baseline. The only edits to hot existing code paths before M3 are the two `isOutOfRange` guard removals; if they cause any unexpected test failure, restore the guards (bounded constructors then simply require `rangePrefixLength == 0`, which M3 must gate on) and record the finding here.

## Artifacts and Notes

Evidence blocks (mvnf output + Surefire snippets) are appended here per milestone as work proceeds.

M1 evidence (2026-07-15):

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRecordIteratorRangeBoundsTest
    Report: core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbRecordIteratorRangeBoundsTest.txt
    Snippet: [mvnf] Summary: tests=9, failures=0, errors=0, skipped=0, time=0.323s

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRecordIteratorSeekTest
    Snippet: [mvnf] Summary: tests=4, failures=0, errors=0, skipped=0, time=0.223s

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRecordIteratorSkipScanTest
    Snippet: [mvnf] Summary: tests=3, failures=0, errors=0, skipped=0, time=0.395s

Note: the renew test (`boundsSurviveTransactionVersionRenew`) is, per the TODO comment at the renew
branch in `LmdbRecordIterator.next()`, the first in-repo test to exercise the cursor-renew path.

M2 evidence (2026-07-15):

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py TripleStoreRangePartitionTest
    Report: core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.TripleStoreRangePartitionTest.txt
    Snippet: [mvnf] Summary: tests=6, failures=0, errors=0, skipped=0, time=0.287s

M3 evidence (2026-07-15):

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeRangePartitionedScanTest
    Report: core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeRangePartitionedScanTest.txt
    Snippet: [mvnf] Summary: tests=5, failures=0, errors=0, skipped=0, time=0.596s

    Existing suites after integration:
    LmdbNativeParallelPipelinesTest              tests=15, failures=0, errors=0
    LmdbNativeParallelAggregationTest            tests=28, failures=0, errors=0
    LmdbNativeParallelAggregationCleanupTest     tests=35, failures=0, errors=0
    LmdbNativeParallelAggregationPreflightFailureTest tests=4, failures=0, errors=0

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest -- \
        -Drdf4j.lmdb.parallel.rangePartition.enabled=true -Drdf4j.lmdb.parallel.minRootEstimate=0
    Snippet: [mvnf] Summary: tests=18, failures=0, errors=0, skipped=0, time=16.049s

Implementation note discovered during M3: `ParallelRowCursor.close()` unconditionally cleared the
input queue; in partition mode `input` is null (no producer), fixed with a null guard plus clearing
the partition queue instead.

## Interfaces and Dependencies

In `org.eclipse.rdf4j.sail.lmdb` (module `core/sail/lmdb`):

    public final class LmdbKeyRange {
        final byte[] lowKey;            // inclusive; null = natural start
        final byte[] highKeyExclusive;  // null = natural end
        final String indexFieldSeq;     // e.g. "posc"
    }

    public final class LmdbRootScanPartition {
        final int member;               // composite source ordinal, 0 for plain stores
        final LmdbKeyRange range;
    }

    // TripleStore
    RecordIterator getTriplesRange(Txn txn, long s, long p, long o, long c, boolean explicit, LmdbKeyRange range)
    List<byte[]> planRootSplitKeys(Txn txn, long s, long p, long o, long c, boolean explicit, int targetPartitions)

In `org.eclipse.rdf4j.sail.lmdb.evaluation`:

    // NativeLmdbQuerySource (default-refusing)
    LmdbRootScanPartition[] planRootScanPartitions(long s, long p, long o, long c, int targetPartitions)
    RecordIterator statements(long s, long p, long o, long c, LmdbRootScanPartition partition)

    // LmdbNativeExchange
    static final class PartitionCursor implements RowCursor { ... }
    static LmdbRootScanPartition[] tryPlanRootPartitions(NativeLmdbQuerySource source, PatternPlan root, RowState row, int target)

System properties: `rdf4j.lmdb.parallel.rangePartition.enabled` (default `false`), `rdf4j.lmdb.parallel.rangePartition.factor` (default `4`). Depends only on existing in-repo machinery (LWJGL LMDB bindings, `Varint`, `TripleIndex`, `TxnManager`, the exchange and parallel frameworks); no new external dependencies.
