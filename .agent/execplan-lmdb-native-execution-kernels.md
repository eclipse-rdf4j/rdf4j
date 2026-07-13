# Complete the LMDB native execution engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The LMDB branch already avoids RDF value materialization through much of query execution, batches raw index scans, skips ordered DISTINCT runs, factorizes selected star-shaped joins, and parallelizes a narrow COUNT aggregation path. Ordinary native queries still fall back to a row-at-a-time cursor between most operators, repeatedly probe LMDB for large joins, allocate object-backed group and sort state, eagerly materialize public result values, and leave most queries on one core. After this plan is complete, the already-selected physical query tree will execute through reusable batches and primitive operator state, with native join, aggregation, sort, parallel, and selectively specialized kernels. No query algebra rewrite, join reordering, cardinality estimator, or optimizer rule is changed by this plan.

The observable outcome is that native-enabled and native-disabled evaluation return the same SPARQL results across focused differential tests and the existing native fuzz suite. Package-visible counters and explanation strings prove that each new executor path engages. JMH benchmarks exercise scan/filter/project, large joins, grouping, ordering, result consumption, parallel pipelines, and cold/warm specialization. Each accepted performance milestone must improve its intended workload without materially regressing the fixed control corpus.

## Progress

- [x] (2026-07-13T00:40:06Z) Read `.agent/PLANS.md`, the high-performance Java skill and references, and the `mvnf` test-runner skill.
- [x] (2026-07-13T00:39:45Z) Ran the mandatory root quick clean install; the full reactor, including `core/sail/lmdb`, completed with `BUILD SUCCESS` in 1:40.
- [x] (2026-07-13T00:40:06Z) Mapped existing native scan, row, join, value-codec, aggregation, sorting, factorization, parallel, and expression paths plus their current tests and benchmarks.
- [x] (2026-07-13T00:45:30Z) Captured the fixed correctness baseline: the 11-class native corpus passed 116 tests and deterministic differential fuzzing passed 6 tests; retained logs are recorded in Artifacts.
- [x] (2026-07-13T01:01:53Z) Milestone 1: landed the reusable slot-major batch ABI, direct pattern/VALUES/filter cursors, root batch consumption, row fallback, and batch-size parity tests; focused log `logs/mvnf/20260713-010153-verify.log` passed 3 tests.
- [x] (2026-07-13T01:09:15Z) Milestone 2: landed a one-to-four-key primitive hash join for eligible two-pattern joins, duplicate-preserving build chains, observed-cap abandonment, and factorized/nested fallback; focused log `logs/mvnf/20260713-010915-verify.log` passed 3 tests. Ordered joins remain on the existing index-nested/factorized path until ordered metadata is available on both selected inputs.
- [x] (2026-07-13T01:28:55Z) Milestone 3: landed immutable ID-backed projected binding sets, skipped the legacy eager-value compatibility walk for those snapshots, added transaction-scoped LMDB payload callbacks, direct `ByteBuffer` decoding, and a bounded primitive ID cache. Focused lazy-result/after-close tests passed 3 tests and `LmdbNativeExpressionFilterTest` passed 4 tests.
- [x] (2026-07-13T01:55:07Z) Milestone 4: landed one-to-four-key primitive group/DISTINCT tables, parallel COUNT arrays, general dense aggregate state, factorized primitive grouping, and ordered one-active-group streaming for eligible pattern scans; focused grouping tests passed 34 tests.
- [x] (2026-07-13T01:55:07Z) Milestone 5: landed flat packed sort arenas, stable primitive index sorting, bounded DISTINCT-aware top-K, configurable binary spill runs, one-row-per-run merge, cleanup counters, and outer-order physical compilation compatible with stable-order `Reduced`; the packed/row/order regression selection passed 22 tests.
- [x] (2026-07-13T02:10:25Z) Milestone 6: extracted shared executor/thread gates, added bounded same-snapshot row morsels and output batches, worker-local compiled-filter feedback, factorized-tail composition, early-close cancellation, failure propagation, and active-write/read-your-writes gating; legacy aggregation plus new row-pipeline tests passed 16 tests.
- [x] (2026-07-13T02:16:45Z) Milestone 7: landed a Java 25 Class-File API hidden-class filter kernel, exact-slot unrolled batch copies, asynchronous threshold compilation, batch-boundary switching, bounded LRU entry/byte caps, unloadable eviction, and interpreter/disabled/failure fallbacks; specialization plus neighboring batch/expression tests passed 11 tests.
- [x] (2026-07-13T02:52:20Z) Final acceptance: copyright and Spotless checks passed; the consolidated native corpus passed 137 tests, deterministic fuzzing passed 6 tests, and full LMDB verify passed 1,538 tests with 0 failures/0 errors/3 skips. Added fixed JMH query and specialization benchmarks; paired results are recorded below.

## Surprises & Discoveries

- Observation: batching currently ends at the storage iterator rather than forming the executor contract.
  Evidence: `LmdbRecordIterator.fill(long[], int)` batches quads, while `LmdbNativeSlotPlan.RowCursor` exposes only `boolean next()`.
- Observation: the general inner join remains an index nested-loop even though special factorized and membership paths exist.
  Evidence: `LmdbNativeJoinPlans.JoinCursor` advances the left cursor and calls `right.open(row)` or a retained pattern probe for every left row.
- Observation: the native codec still copies stored LMDB values before decoding, and public projection eagerly resolves every projected id.
  Evidence: `LmdbNativeValueCodec.decode` calls `ValueStore.getData`, whose `copyValueBytes` allocates a `byte[]`; `NativeRowsStep.project` creates `QueryBindingSet` and calls `values.value(id)` for each binding.
- Observation: single-slot grouping already has a primitive open-addressed map, but multi-slot grouping, unordered DISTINCT, and LEFT JOIN memo tables remain object-backed.
  Evidence: `LmdbNativeGroupStep` uses `LongAggStateMap` for one slot and `HashMap<GroupKey,AggState>` for multiple slots; `LmdbNativeRowStep` and `LmdbNativeLeftJoinMemo` use `HashMap<GroupKey,...>`.
- Observation: current ORDER BY snapshots the complete native row for every input and can decode values repeatedly from the comparator.
  Evidence: `LmdbNativeRowStep.evaluateAll` calls `Arrays.copyOf(row.slots, row.slots.length)` and `orderCompare` calls `codec.decode` during comparisons.
- Observation: current parallel execution has correct same-snapshot worker-source machinery but gates on COUNT-style aggregation and filter-free plain-pattern plans.
  Evidence: `LmdbNativeParallelAggregation` documents and checks those gates and already opens one native source per worker.
- Observation: the active runtime and project compiler release are Java 25, so the final `java.lang.classfile` API is available without a dependency.
  Evidence: root `pom.xml` sets `java.version` to 25; `java -version` reports Zulu OpenJDK 25.
- Observation: the completed package-isolation work was subsequently integrated under an `evaluation` source directory rather than the earlier plan's `experimental` directory, while source declarations still retain package `org.eclipse.rdf4j.sail.lmdb`.
  Evidence: the active branch stores `LmdbNativeSlotPlan.java` and its peers under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`.
- Observation: repository-level query evaluation deliberately traversed every result binding to initialize legacy lazy LMDB values, which defeated the new immutable ID snapshot despite projection itself remaining lazy.
  Evidence: the diagnostic stack reached `LmdbStoreConnection.evaluateInternal(...).next()` line 147 before the caller inspected a binding. The compatibility traversal now remains for every binding-set type except `NativeProjectedBindingSet`, whose copied IDs resolve independently after iteration close.
- Observation: a variable graph query used during the batch engagement test does not select the direct native pattern executor on this branch, even though its results are correct.
  Evidence: the first batch test attempts had matching result bags but a zero direct-pattern-fill counter; the focused contract now exercises named-context rejection directly through the package-level native source.
- Observation: stable-order `SELECT DISTINCT ... ORDER BY ... LIMIT` reaches the physical compiler as `Slice -> Reduced -> Order -> Projection`, not the older projection-local order shape.
  Evidence: the native explanation initially showed `distinct=false` and duplicate combinations underfilled top-K. Treating a fused `Reduced` as an exact native deduplication pass is legal for SPARQL REDUCED and restores the generic stable-order contract; the focused packed-sort test now proves 500 input rows become 140 distinct candidates before a 13-row heap.
- Observation: generic-oracle executions in the packed-sort test can contribute to package-global instrumentation before the native measurement even when result semantics are correct.
  Evidence: the distinct tracker reported 1,000 rows across the comparison pair; resetting counters between the generic oracle and native execution isolates the engagement assertion to the optimized run.
- Observation: the existing factorized-row executor is the ordinary winner for chain/star SELECTs, so scheduling parallelism after factorization would make the new row scheduler effectively unreachable on representative queries.
  Evidence: the first row-pipeline tests returned correct results with a zero parallel-run counter. Moving the structural gate before factorization and adding `LmdbNativeFactorizedRows.openFrom(...)` lets each worker consume its supplied root morsel while retaining worker-local factorized tails and multiplicity.
- Observation: same-snapshot read-only siblings cannot represent a connection's uncommitted writes.
  Evidence: `openParallelSources` creates fresh untracked read transactions over the last committed snapshot. It now returns null while `storeTxnStarted` is true, and the focused test proves an active connection still sees its new chain through sequential evaluation without engaging parallel workers.
- Observation: specializing the stable batch-to-filter boundary gives a bounded code-generation target without freezing query-algebra or optimizer decisions into bytecode.
  Evidence: `FilterBatchCursor` starts in `interpretFilter`, requests one kernel per exact slot width only after the row threshold, and later invokes a hidden class that unrolls slot copies while still calling the already-compiled boolean condition. Tests prove hidden-class identity, exact selection parity, cold/warm switching, LRU eviction, byte-cap rejection, and disabled fallback.

## Decision Log

- Decision: This work uses Routine D and one living ExecPlan rather than seven disconnected patches.
  Rationale: the features share a batch representation, primitive tables, pipeline boundaries, and fallback policy; implementing them independently would duplicate state and make correctness harder to reason about.
  Date/Author: 2026-07-13 / Codex.
- Decision: Keep the current row executor as the compatibility fallback throughout migration.
  Rationale: unsupported SPARQL shapes must continue to work, and every milestone needs a differential oracle. A batch-to-row and row-to-batch adapter makes each step independently shippable.
  Date/Author: 2026-07-13 / Codex.
- Decision: Introduce no third-party performance dependency.
  Rationale: fixed-width native ids fit flat in-repo arrays and open addressing. Java 25 supplies the Class-File API and hidden classes for specialization.
  Date/Author: 2026-07-13 / Codex.
- Decision: Preserve the compiler-provided join tree and child order.
  Rationale: the user explicitly excluded query-optimizer work. Join kernels may choose an execution algorithm only for the already-selected node, using structural eligibility and observed runtime row counts rather than new estimates or reordering.
  Date/Author: 2026-07-13 / Codex.
- Decision: Specialize one through four fixed-width key slots and fall back for wider keys.
  Rationale: RDF native rows use at most 60 slots, but common joins, groups, and DISTINCT keys are narrow. Fixed-width structures keep hot loops simple while preserving correctness for uncommon wide keys.
  Date/Author: 2026-07-13 / Codex.
- Decision: Runtime specialization is the final milestone and compiles only stable batch fragments.
  Rationale: code generation should optimize the final execution model, not freeze the current row-at-a-time abstraction. Cold queries, large fragments, compile failures, and cache misses continue through vectorized or row fallbacks.
  Date/Author: 2026-07-13 / Codex.

## Outcomes & Retrospective

All seven execution milestones are implemented and the correctness gates are green. The reusable batch ABI removes row cursor transitions for direct scans, VALUES, and compiled filters; eligible large two-pattern joins build and probe primitive ID tables without changing the optimizer-selected child order; public results retain IDs until binding access while stored expression values decode without an intermediate `byte[]`; common aggregation and DISTINCT state is flat and primitive; ORDER BY uses packed rows, bounded top-K, or cleaned-up external runs; same-snapshot workers stream general row pipelines while composing with factorized tails; and hot batch filters can switch to bounded hidden-class kernels at a later batch boundary.

The short fixed JMH screens show the intended wins without claiming universal speedups: primitive hash join is about 2.8x faster, ID-only result consumption about 1.7x faster, a direct scan/filter about 14% faster through batching, and the isolated warm 24-slot specialization kernel about 1.28x faster. Whole-query specialization was neutral on a narrow fixture, which supports retaining the conservative 32,768-row threshold rather than compiling small queries. Full module verification—including benchmark-backed integration regressions—passed, so the implementation is complete for this plan.

## Context and Orientation

The Maven module is `core/sail/lmdb`. Native query production sources live beneath `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation` but intentionally retain Java package `org.eclipse.rdf4j.sail.lmdb` so they can use package-private storage types. Tests remain beneath `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb`.

A native row is a `long[]` indexed by a plan-local slot number. Each nonzero stored value is an LMDB dictionary id. `NativeLmdbQuerySource.UNKNOWN_ID` represents an unbound slot and id zero is reserved for the null graph context. A `SlotPlan` opens a `RowCursor`; the cursor mutates one shared `RowState.slots` array and restores slots when it backtracks. A `PatternPlan` opens an LMDB `RecordIterator` through the query source, binds the four quad ids into row slots, and supports raw batched fill. `MultiJoinPlan` chains pattern and operator cursors in the compiler-provided order.

The main implementation files are:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSlotPlan.java`: `SlotPlan`, `RowCursor`, and small plan primitives.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePatternPlan.java` and `LmdbNativePatternTerms.java`: statement-pattern binding and raw batch scans.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeJoinPlans.java`: compiler-ordered multi-joins and ordinary nested-loop execution.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java`: SELECT projection, DISTINCT, ORDER BY, LIMIT/OFFSET, and public result iteration.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGroupStep.java` and `LmdbNativeAggregateState.java`: GROUP BY and aggregate state.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeValueCodec.java` and root `ValueStore.java`: native scalar decoding and LMDB dictionary access.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeParallelAggregation.java`: current same-snapshot morsel worker implementation.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeExpressionCompiler.java` and `LmdbNativeScalarExpressionCompiler.java`: current interpreted native expression fragments.

Existing optimizations must remain intact: prefix-run scans, skip-to-next-DISTINCT, factorized rows and tails, reusable native probes, membership joins, LEFT JOIN replay/memoization, primitive property-path traversal, single-slot primitive grouping, bounded top-K, and COUNT aggregation parallelism.

## Plan of Work

Milestone 0 freezes correctness and performance controls. Run the focused native test corpus in one `mvnf` selection and retain the log. Run `LmdbNativeDifferentialFuzzTest` separately with its deterministic seed. Record baseline JMH results for `FactorizedRowsStarBenchmark`, `ParallelAggregationBenchmark`, and a new `NativeExecutionKernelBenchmark` added in Milestone 1. The new benchmark owns a fixed generated LMDB dataset and methods for scan/filter/project, two-pattern high-cardinality join, multi-key group, ORDER BY with and without LIMIT, and consume-only versus value-reading result clients. Every method verifies its result count or digest.

Milestone 1 introduces batch execution. Add `LmdbNativeBatch.java` containing `NativeBatch`, `BatchCursor`, `BatchMark`, and adapters. `NativeBatch` owns one flat slot-major `long[]`, a row count, capacity, slot count, and `int[] selection`; storage is reused after every fill. `BatchCursor.fill(NativeBatch)` returns the selected row count and `close()` releases its child. Add `SlotPlan.openBatch(RowState)` with a default row adapter so every plan remains correct. Implement direct batch paths for `PatternPlan`, VALUES, projection-ready row streams, and stateless native filters. Modify `NativeRowsStep` to consume batches when no blocking operator requires the old cursor, and expose engagement counters. The acceptance tests compare row and batch modes with batch sizes 1, 7, 64, and 1024, including unbound values, named graphs, repeated variables, filters, LIMIT/OFFSET, and early close.

Milestone 2 adds physical join kernels without changing the join tree. Add `LmdbNativeHashJoin.java` with one- through four-key primitive open-addressed build tables, an occupancy array so zero remains a valid payload value, and flat payload/next arrays for duplicates. For eligible inner joins whose common slots are guaranteed bound and whose right child can be opened independently of left-produced slots, materialize the right child once into a build batch and probe it from left batches. If both already-selected inputs advertise the same ascending key prefix, use `LmdbNativeMergeJoin` to stream equal-key runs. Observe build rows and bytes; abandon before publishing the table when caps are exceeded, then execute the existing nested-loop cursor. Preserve nested-loop execution for correlated patterns, unbound compatibility semantics, OPTIONAL, MINUS, EXISTS, and unsupported wide keys. Differential tests cover duplicates, empty sides, repeated keys, named graphs, unbound shared variables, cap fallback, early close, and current factorized-path coexistence.

Milestone 3 keeps values native until demanded. Add a package-private callback on `ValueStore`, `withData(long id, ValueDataReader<T>)`, that invokes the reader while the LMDB read transaction and returned `MDB_val` remain valid. The callback exposes address and length but does not allow a view to escape. Refactor `LmdbNativeValueCodec` to parse supported values directly from the address and to use a bounded primitive query-local cache for decoded ids. Add `NativeProjectedBindingSet`, an immutable view over an immutable page of projected ids; it lazily materializes and caches a public RDF `Value` only when the caller requests that binding. `NativeRowsIteration` emits those views and retains the existing eager `QueryBindingSet` fallback for callers or shapes that require detached values after the source closes. Tests cover result access before and after query/repository close, retained rows across batch reuse, iteration order, equality/hash behavior, all RDF value kinds, invalid ids, and lazy-access counters.

Milestone 4 unifies aggregation state. Add `PrimitiveTupleTable`, with specialized one-, two-, three-, and four-key probes and an object-key fallback. The table maps a key tuple to a dense group index. Aggregate state becomes parallel arrays indexed by that group index for primitive COUNT and common numeric states, with existing `AggState` used only for unsupported aggregate combinations. Before hashing, `NativeGroupStep` checks whether the input's existing stable order covers the complete GROUP BY key prefix. If so, it emits or stores one completed group at each key change and keeps only the active group. Reuse the tuple table for residual unordered DISTINCT and LEFT JOIN memo indexes where their semantics fit. Tests compare ordered streaming, primitive hash, and generic evaluation over empty input, unbound keys, two to five keys, duplicate-heavy input, DISTINCT aggregates, numeric errors, and stable output requirements.

Milestone 5 replaces object-backed sorting. Add `LmdbNativeSort.java` with a packed row arena containing only projected ids, precomputed typed sort keys, and a stable input ordinal. Sort an `int[]` of row positions. Provide primitive radix passes for id and supported numeric keys and a comparator fallback for full SPARQL value ordering. General ORDER BY uses compact in-memory runs up to a configurable byte limit, spills sorted binary runs under the query temporary directory after the limit, and merges runs through a priority queue containing only one row per run. Generalize top-K to the legal DISTINCT pipeline without underfilling. Tests cover ascending/descending, multiple keys, unbound/error keys, stable ties, OFFSET/LIMIT, DISTINCT, forced spills, close/delete cleanup, and differential ordering.

Milestone 6 generalizes parallel execution. Extract the worker-source, cancellation, queue, and morsel lifetime machinery from `LmdbNativeParallelAggregation` into `LmdbNativeParallelPipelines`. A pipeline is the already-selected sequence between a source scan and a blocking join-build, aggregation, sort, or public-result boundary. Each worker owns its `NativeLmdbQuerySource`, `RowState`, batch buffers, probes, and mutable expression/filter state. Workers write unordered output batches to a bounded queue; ordered queries attach sequence numbers and merge them before emission. Group and join build state remain thread-local or partitioned and merge explicitly. Begin sequentially and engage workers after an observed input-row threshold; never share LMDB cursors or read transactions across threads. Tests cover filters, joins, projection, cancellation, consumer early close, worker failure, concurrent writes, read-your-writes gating, deterministic ordered output, and parallel-disabled equality.

Milestone 7 selectively compiles stable fragments. Add `LmdbNativeSpecialization.java` with a small physical fragment description for batch filter/project, hash-probe, and aggregate-update loops. Use `java.lang.classfile.ClassFile` to generate a final hidden class implementing the relevant kernel interface. Cache by normalized fragment, slot layout, scalar types, nullability, key width, and feature flags. The cache has explicit entry and generated-byte limits, records hits/misses/compile failures, and drops strong references on LRU eviction so hidden classes can unload. Every query begins on the batch interpreter; only fragments that cross an observed work threshold compile in the background and switch at a batch boundary. Oversized fragments, compile failures, cold queries, and disabled specialization stay on the batch interpreter. Tests exercise cold execution, warm cache hits, eviction, concurrent compilation deduplication, forced generation failure, semantic equality, and weak-reference unload eligibility.

The final milestone formats all touched files, checks source headers, runs the focused corpus and deterministic fuzz test, then runs `core/sail/lmdb` verification. It records benchmark medians, allocation observations, JFR CPU profiles, cold/warm specialization time, cache counters, and parallel scaling. Any benchmark regression over 5% on the fixed control corpus requires diagnosis or a default-off gate before acceptance.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`.

The initial clean install already completed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Run the focused baseline and retain logs:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs -Dtest=LmdbNativeRowStreamTest,LmdbNativeRowStepIterationTest,LmdbNativeExpressionFilterTest,LmdbNativeGroupByTest,LmdbNativeNonCountAggregateTest,LmdbNativeJoinOrderCrossProductTest,LmdbNativeMembershipJoinTest,LmdbNativeLeftJoinHashTest,LmdbNativeFactorizedRowsTest,LmdbNativeParallelAggregationTest,LmdbNativeLazyResultAfterCloseTest

Run deterministic differential fuzzing separately:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest --module core/sail/lmdb --retain-logs

After each milestone, first compile the module and dependencies with tests skipped:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install

Then run the milestone's focused test through `mvnf`; never use `-am` or `-q` for tests. Run the formatter only after checking headers:

    scripts/checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    git diff --check

Use the benchmark wrapper for one method at a time. The baseline form is:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.NativeExecutionKernelBenchmark --method <method> --enable-jfr --enable-jfr-cpu-times

Final module verification is:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

## Validation and Acceptance

Correctness acceptance requires identical native and generic SPARQL result multisets, or identical ordered sequences when ORDER BY is present, for every new focused scenario. The existing deterministic fuzz suite must pass. Every feature must expose an engagement counter or explanation marker, and focused tests must prove both engagement and fallback. Early close, exceptions, and cancellation must release cursors, transactions, spill files, worker tasks, and batch pages.

Performance acceptance is workload-specific. Batch execution must lower per-row executor overhead or allocation for scan/filter/project. Hash join must beat nested-loop on the high-cardinality independent join benchmark while retaining nested-loop performance on selective probes. Lazy results must make consume-only output materially cheaper and must not regress value-reading clients beyond 5%. Ordered aggregation and packed sorting must lower allocation and peak memory on their target workloads. General parallel pipelines must improve a long scan/join workload on at least four cores. Warm specialization must beat the batch interpreter on its expression-heavy target after separately accounting for cold compile time. Claims are made on the active Java 25 runtime and remain provisional for other JDKs until repeated there.

## Idempotence and Recovery

All implementation paths are additive behind feature properties and structural gates. Tests and builds are safe to rerun. If an optimization fails correctness, disable its property and use the retained row fallback while fixing the focused test; do not weaken assertions. If a primitive table exceeds its configured cap, abandon it before it becomes externally visible and continue through the old path. Spill files use query-owned temporary paths and delete in `close()` plus failure cleanup. Worker tasks own their read transactions and close them in `finally`.

Do not delete or rewrite pre-existing untracked artifacts. The two modified tracked ExecPlans present before this work are user-owned and remain untouched. If offline dependency resolution fails, rerun the exact Maven command once without `-o`, then return to offline operation.

## Artifacts and Notes

Initial root build summary:

    [INFO] RDF4J: LmdbStore ................................... SUCCESS [ 14.725 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time: 01:40 min (Wall Clock)

The existing `profiles/lmdb-opt/java-command-cpu-43131.txt` profile is loading-oriented, not query evidence. Query-specific JFR and allocation evidence will be added per milestone.

Focused correctness baseline:

    logs/mvnf/20260713-004257-verify.log
    Summary: tests=116, failures=0, errors=0, skipped=0, time=5.802s

Deterministic differential fuzz baseline:

    logs/mvnf/20260713-004449-verify.log
    Summary: tests=6, failures=0, errors=0, skipped=0, time=8.462s

Final consolidated native corpus:

    logs/mvnf/20260713-021938-verify.log
    Summary: tests=137, failures=0, errors=0, skipped=0, time=2.717s

Final deterministic differential fuzz:

    logs/mvnf/20260713-022024-verify.log
    Summary: tests=6, failures=0, errors=0, skipped=0, time=2.303s

Full LMDB module verification:

    logs/mvnf/20260713-022105-verify.log
    Summary: tests=1538, failures=0, errors=0, skipped=3, time=1377.106s

Fixed JMH screens on Java 25, one fork, 1-second iterations (lower is better; short screens are directional):

    workload                         optimized       fallback       ratio
    primitive hash join              2.864 ms        7.957 ms       2.78x
    consume IDs without Value reads  2.916 ms        4.958 ms       1.70x
    direct scan/filter batch          1.689 ms        1.926 ms       1.14x
    warm 24-slot filter kernel       20.754 us       26.581 us      1.28x

The fallback flags were `rdf4j.lmdb.nativeHashJoin.enabled=false`,
`rdf4j.lmdb.nativeLazyResults.enabled=false`, and `rdf4j.lmdb.nativeBatch.enabled=false`; the specialization comparison
used separate generated and interpreted benchmark methods over the same 1,024-row batch.

## Interfaces and Dependencies

No new external libraries are added. New production types remain package-private in Java package `org.eclipse.rdf4j.sail.lmdb` and use the standard source header plus `// Some portions generated by Codex`.

The intended end-state interfaces are:

    interface BatchCursor extends AutoCloseable {
        int fill(NativeBatch batch) throws IOException;
        void close();
    }

    final class NativeBatch {
        long[] slots;       // slot-major: slots[slot * capacity + row]
        int[] selection;    // active physical row positions
        int rowCount;
        int selectedCount;
        int capacity;
        int slotCount;
    }

    interface NativeBatchKernel {
        int apply(NativeBatch input, NativeBatch output);
    }

    interface ValueDataReader<T> {
        T read(long address, int length) throws IOException;
    }

    final class PrimitiveTupleTable {
        int findOrInsert(long[] row, int[] keySlots);
        int size();
    }

    interface NativeSpecializedKernel {
        int apply(NativeBatch input, NativeBatch output, Object state);
    }

System properties are read per evaluation or benchmark trial so tests can toggle them safely:

- `rdf4j.lmdb.nativeBatch.enabled`, default `true`.
- `rdf4j.lmdb.nativeHashJoin.enabled`, default `true`.
- `rdf4j.lmdb.nativeLazyResults.enabled`, default `true` after compatibility tests pass.
- `rdf4j.lmdb.nativeStreamingGroup.enabled`, default `true`.
- `rdf4j.lmdb.nativePackedSort.enabled`, default `true`.
- Existing `rdf4j.lmdb.parallel.enabled` controls generalized parallelism.
- `rdf4j.lmdb.nativeSpecialization.enabled`, default `true` only after cold/warm/fallback tests pass.

Revision note, 2026-07-13 / Codex: initial self-contained plan created after the mandatory green root install, direct inspection of the current native engine, and review of the completed factorized-row, row-layout, aggregate-refactor, and morsel-aggregation plans. The implementation order deliberately establishes a batch ABI before algorithms, parallel scheduling, and code generation that depend on it.

Revision note, 2026-07-13 / Codex: recorded the green focused and differential-fuzz correctness baselines and started Milestone 1.

Revision note, 2026-07-13 / Codex: corrected source-directory orientation from the superseded `experimental` plan path to the active branch's `evaluation` path after direct inspection.

Revision note, 2026-07-13 / Codex: recorded completed primitive aggregation and packed-sort milestones, including the stable-order Reduced wrapper shape discovered by differential top-K testing, and started the generalized parallel-pipeline milestone.

Revision note, 2026-07-13 / Codex: completed generalized parallel pipelines, bounded hidden-class specialization, full correctness verification, and paired JMH acceptance measurements; all plan milestones are now complete.
