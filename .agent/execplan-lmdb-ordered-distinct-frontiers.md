# Eliminate DISTINCT hash state when LMDB order proves values cannot recur

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J's LMDB native evaluator currently allocates a `LongHashSet` for every `COUNT(DISTINCT ...)`, `SUM(DISTINCT ...)`, and `AVG(DISTINCT ...)` aggregate group, even when the selected LMDB index already guarantees that a value can never reappear after the scan advances. The same evaluator always retains a complete native tuple-DISTINCT table when a safe ordered prefix could bound that state. This work teaches the evaluator to use an exact lexicographic order proof: adjacent duplicate suppression when the complete DISTINCT identity is ordered, partition-local hashing when only a leading identity prefix is ordered, and the existing global hash fallback otherwise.

The change also closes four independently confirmed regressions discovered while reviewing the same optimization series: unbounded process-wide parallel query threads, stale canonical values retained by `DynamicModel`, lazy native rows that fail after repository shutdown, and escaped hashes that confuse the `STABLE_INDEX` query preprocessor. A user can observe completion by running the focused and module test suites, then the two JDK 26 theme benchmarks. `LIBRARY/6` must complete in at most 3635 ms/op and `HIGHLY_CONNECTED/6` in at most 502 ms/op, with profiles showing no aggregate `LongHashSet` or `PrimitiveTupleTable` work in either target.

## Progress

- [x] (2026-07-13 11:35Z) Read `.agent/PLANS.md`, the high-performance Java guidance, and the `mvnf` workflow.
- [x] (2026-07-13 11:35Z) Audited the existing branch and preserved four unrelated tracked edits plus all untracked artifacts.
- [x] (2026-07-13 11:35Z) Ran the required root `-Pquick clean install`; all reactor modules passed in 33.286 seconds.
- [x] (2026-07-13 11:47Z) Added and ran the smallest failing tests for all four isolated bugs; appended six compact Surefire failures to `initial-evidence.txt` (two model paths and two parallel callers).
- [x] (2026-07-13 11:57Z) Implemented bounded process-wide task-group admission and verified row-pipeline and aggregate fallback/release tests.
- [x] (2026-07-13 11:55Z) Implemented amortized `DynamicModel` canonical-cache rebuilding plus empty-model clearing; both focused tests pass.
- [x] (2026-07-13 11:56Z) Implemented native result materialization/detachment at the Sail boundary; retained rows pass after shutdown.
- [x] (2026-07-13 11:54Z) Fixed escaped-hash parsing by consecutive-backslash parity; the focused parser test passes.
- [x] (2026-07-13 13:10Z) Added 18 differential ordered-DISTINCT scenarios, including repeated partitions, complete and unsafe orders, shared channels, aliases, UNION, unbound values, early close, and multiple fixed contexts.
- [x] (2026-07-13 13:10Z) Implemented exact native order proofs, complete-signature ordered UNION/source merging, fail-closed propagation, and deterministic aggregate-plan ranking.
- [x] (2026-07-13 13:10Z) Specialized aggregate channels and tuple DISTINCT state for hash, partitioned/monotonic, adjacent, and constant-once execution.
- [x] (2026-07-13 13:16Z) Ran the focused parser, model, lifecycle, and ordered/parallel LMDB classes; the combined LMDB selection passed 135 tests.
- [x] (2026-07-13 13:58Z) Ran the parser, model, and initial LMDB module verifications; the LMDB run passed 1566 tests with 3 skips before the final control refinements.
- [x] (2026-07-13 14:36Z) Repaired the ordered/factorized dispatch control and removed per-group empty-channel allocations; the 39-test aggregate/DISTINCT neighborhood passes.
- [x] (2026-07-13 14:43Z) Ran exact JDK 26 target and control benchmarks. Both targets meet their historical guardrails, and paired controls remain within three percent.
- [x] (2026-07-13 14:47Z) Re-profiled both targets; neither profile contains `LongHashSet` nor `PrimitiveTupleTable` aggregation work.
- [x] (2026-07-13 14:48Z) Ran formatting, copyright validation, and `git diff --check`; all passed.
- [x] (2026-07-13 15:11Z) Completed the final formatted LMDB module verification: 1568 tests, zero failures/errors, and three skips.
- [x] (2026-07-13 15:12Z) Preserved the final evidence, then completed the required root `-Pquick clean install` in 35.204 seconds.
- [x] (2026-07-13 15:13Z) Recorded final outcomes, acceptance evidence, and retrospective.

## Surprises & Discoveries

- Observation: Generic tuple-level DISTINCT already has the correct partial-prefix behavior. `LmdbPartitionedDistinctIteration` retains a full projected-row set for every repeated value of its ordered binding and clears only when that binding changes.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbPartitionedDistinctIteration.java` compares the current partition binding before calling `seen.clear()`.

- Observation: Aggregate DISTINCT is separate from tuple-level DISTINCT. `AggState` allocates one `LongHashSet` per DISTINCT aggregate specification and `NativeDistinctTracker` allocates either `PrimitiveTupleTable` or `HashMap` state for native tuple projections.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregateState.java` and `LmdbNativePrimitiveTupleTable.java`.

- Observation: Both parallel callers share the same cached executor, but row pipelines submit workers plus a producer while parallel aggregation submits only workers and produces on the query thread. Admission therefore must reserve different group sizes while sharing one process budget.
  Evidence: `LmdbNativeParallelPipelines.ParallelRowCursor.start()` and `LmdbNativeParallelAggregation.evaluate()`.

- Observation: The target queries can obtain the complete aggregate identity from one anchor statement pattern. `LIBRARY/6` can scan `borrowedBy` through `ospc` as `(member, loan)`. `HIGHLY_CONNECTED/6` can scan its outgoing arm through `spoc` and incoming arm through `ospc`, both as `(node, neighbor)`.
  Evidence: the original queries embedded in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-07-13.md`.

- Observation: Merely obtaining a lazy `Value` after shutdown does not fail; dereferencing it with `stringValue()` does. The smallest boundary test must therefore force value initialization after repository shutdown.
  Evidence: the first focused row-lifetime assertion passed when it checked only non-null values; the tightened test failed in `LmdbIRI.stringValue()` through `ValueStore.resolveValue()`.

- Observation: A fixed threshold test for canonical-cache churn must inspect the cache size rather than demand immediate eviction of the most recently removed value. Amortized rebuilding intentionally permits less-than-threshold debt.
  Evidence: the approved algorithm rebuilds at `max(1024, cacheSize / 2)`; the same-package test now asserts the cache stays within 1024 entries after 1000 unique removals.

- Observation: A partial aggregate group frontier cannot be found by considering only one complete group-key order. Each leading permutation of group slots can expose a different safe partition before the DISTINCT argument.
  Evidence: the initial planner selected `HASH` with prefix length zero for a valid one-slot group partition; the focused failure is preserved in `initial-evidence.txt`, and the planner now enumerates every deterministic group-prefix permutation.

- Observation: A DISTINCT argument that is already a group slot, a fixed constant, or absent is constant within one aggregate state and needs neither a hash set nor last-seen state.
  Evidence: the group-key DISTINCT regression initially explained `distinctChannels=[HASH]`; after classifying it as constant-once, the focused test passes and ordered specialization is selected only when a real hash channel is removed.

- Observation: Concatenating scans for multiple fixed contexts does not preserve the underlying index order across context boundaries, even though each individual scan is ordered.
  Evidence: a two-context adjacent-DISTINCT test emitted the same subject three times (`s1, s2, s1`) before the source selected `OrderedRecordIterator` for an explicit complete-signature merge.

- Observation: Counting removed statements is insufficient removal debt for nested RDF-star values because one statement may introduce many canonical triple terms and components.
  Evidence: a nested triple-term churn test retained 6603 canonical values under statement-count debt. Removal accounting now walks the exact canonical value graph, and the focused plus full 30-test model class pass.

- Observation: An ordered DISTINCT proof does not automatically make its sequential implementation faster than an already factorized and parallel aggregate plan.
  Evidence: the first control run moved `ParallelAggregationBenchmark.countDistinctTail` from about 5 ms/op to 182 ms/op because ordered dispatch preempted the factorized tail. A focused dispatch test now protects the factorized/parallel path, while the two target UNION shapes still use ordered aggregation.

- Observation: The first ordered UNION merge copied a complete row snapshot on every branch advance even though each branch already owns an isolated `RowState`.
  Evidence: `HIGHLY_CONNECTED/6` initially remained near 515 ms/op. Reusing one branch-owned head array removed the per-row copy and produced exact runs at 491.731 ms/op and 501.201 ms/op without changing merge semantics.

- Observation: Even a plain non-DISTINCT aggregate paid for fresh zero-length channel arrays in every group after the channel refactor.
  Evidence: the `groupByTwoKeys` control profile exposed `AggState` construction. Shared immutable empty arrays and one precomputed hash-channel plan restored the exact control to 8.266 ms/op versus 8.285 ms/op at baseline.

- Observation: The parallel microbenchmark is sensitive to run order and system noise, so a mixed three-variant table can give a misleading result for one variant.
  Evidence: the mixed table reported `countDistinctTail` at 5.728 ms/op versus 4.919 ms/op, but an immediately paired isolated baseline/candidate run measured 6.092 versus 5.251 ms/op. The candidate is 13.805 percent faster in the controlled pair.

## Decision Log

- Decision: A repeated ordered value is a partition boundary only when it changes; it is not an adjacent-DISTINCT proof by itself.
  Rationale: For `DISTINCT (?person, ?birthday)` ordered by birthday, rows for one birthday may be `person1, person2, person1`. A person set must live for the entire birthday run. Only an order whose safe leading prefix covers both birthday and person makes duplicates adjacent.
  Date/Author: 2026-07-13 / Codex.

- Decision: The first implementation recognizes only DISTINCT/group identity slots and direct variable aliases as determinants of an ordered partition.
  Rationale: RDF data does not imply functional dependencies. A person may have several names, so order by an unrelated `?name` cannot prove that `(person, birthday)` will not recur. Schema-level dependency inference would add a separate correctness surface and is deliberately out of scope.
  Date/Author: 2026-07-13 / Codex.

- Decision: Exact order metadata names the complete sequence of varying native slots, after fixed index fields are removed.
  Rationale: `StatementOrder` describes one requested component and is insufficient to prove a composite identity. The LMDB index field sequence is the actual source of lexicographic order.
  Date/Author: 2026-07-13 / Codex.

- Decision: The ordered aggregate path may supersede parallel or factorized execution only when it removes at least one DISTINCT membership set.
  Rationale: Parallel and factorized paths intentionally destroy input order and remain useful controls. Avoiding a proven large hash state is the explicit algorithmic reason to choose the sequential ordered specialization.
  Date/Author: 2026-07-13 / Codex.

- Decision: The parallel task budget defaults to available processors and is clamped to 1 through 65.
  Rationale: The existing worker configuration is capped at 64 and a row pipeline needs one producer in addition to those workers. A bounded process-wide reservation prevents concurrent queries from multiplying active threads by core count.
  Date/Author: 2026-07-13 / Codex.

- Decision: `DynamicModel` will rebuild its canonical-value cache amortized rather than reference-counting every value.
  Rationale: Exact reference counts would add work to every mutation and recursively nested RDF-star value. Clearing on empty plus rebuilding after removal debt reaches `max(1024, cacheSize / 2)` bounds retention while keeping common removals cheap.
  Date/Author: 2026-07-13 / Codex.

- Decision: Keep the executor physically bounded at the maximum legal 65 tasks and apply the current `rdf4j.lmdb.parallel.maxTasks` value through atomic admission on every group.
  Rationale: Tests and embedding applications can change system properties after class initialization. Dynamic admission preserves the configured bound without recreating a process-wide executor, while the executor itself can never exceed the documented 65-task ceiling.
  Date/Author: 2026-07-13 / Codex.

- Decision: A source with multiple fixed contexts must use an explicit full-index merge before it may publish a native slot-order proof.
  Rationale: Per-context cursors are individually ordered, but simple concatenation can restart every non-context component. An explicit ordered request keeps the proof paired with the cursor that actually enforces it.
  Date/Author: 2026-07-13 / Codex.

- Decision: Preserve a proven factorized/parallel aggregate candidate ahead of sequential ordered execution when the direct multi-join tail supports it.
  Rationale: Order is a correctness proof for eliminating hash state, not an unconditional cost proof. The factorized path can avoid materializing the same join expansion and parallelize it; target UNION plans do not satisfy this exception and continue to use the ordered specialization.
  Date/Author: 2026-07-13 / Codex.

- Decision: Ordered UNION lookahead is branch-owned reusable state rather than a newly allocated row snapshot.
  Rationale: A branch cursor mutates only its own `RowState`, so its complete signature can remain in one reusable array until the branch advances. This preserves the full lexicographic comparison and bag semantics while avoiding a hot allocation/copy.
  Date/Author: 2026-07-13 / Codex.

## Outcomes & Retrospective

The implemented order frontier distinguishes complete-key adjacency, safe prefix partitions, and unsafe global recurrence. Aggregate membership is shared per unique argument and uses hash, monotonic last-id, or constant-once state as proved for each group subsequence. Ordered UNION and multi-source scans merge complete signatures, including repeated and overlapping branch values, and retain bag multiplicity.

All four review regressions have focused failing-then-passing evidence in `initial-evidence.txt`. The parser and model modules pass, as do the ordered-DISTINCT, projection-lifetime, primitive-grouping, and parallel admission/aggregation classes. The final formatted LMDB verification passed 1568 tests with zero failures or errors and three skips in 23:05; its retained log is `logs/mvnf/20260713-144758-verify.log`. The final root `-Pquick clean install` then passed every reactor module in 35.204 seconds.

The final exact JDK 26 target measurements are 3048.449 ms/op for `LIBRARY/6` and 501.201 ms/op for `HIGHLY_CONNECTED/6`. These are respectively 14.45 percent faster and 1.82 percent slower than July 9, and both satisfy the approved limits of 3635 and 502 ms/op. The exact `groupByTwoKeys` control improved 0.229 percent. The immediately paired isolated parallel DISTINCT control improved 13.805 percent; the other mixed-table controls improved 13.868 and 1.758 percent. Final CPU profiles are `/tmp/rdf4j-async-profiler/library6-final/profile-24433.txt` and `/tmp/rdf4j-async-profiler/hc6-final/profile-14344.txt`; neither contains `LongHashSet` or `PrimitiveTupleTable`. The small generic atomic samples in the connected profile belong to existing cursor pools, queues, and caches; the new ordered paths introduce no per-row atomic diagnostics.

## Context and Orientation

The relevant Maven modules are `core/sail/lmdb`, `core/model`, and `core/queryparser/sparql`. The LMDB native evaluator represents RDF values as 64-bit identifiers stored in a mutable `RowState`. A `PatternPlan` opens an LMDB statement index cursor, joins and filters expand those rows, `NativeGroupIteration` aggregates them, and `NativeRowsIteration` projects ordinary query rows. A lexicographic order means that rows are sorted first by one native slot, then by the next when the first is equal, and so on.

For tuple-level DISTINCT, the identity is every projected slot. If the input order begins with a subset of those slots, all rows with that prefix are contiguous and a residual-key set can be cleared when the prefix changes. If the order covers every identity slot before any unrelated varying slot, equal identities are contiguous and only the previous identity is needed. If an unrelated varying slot appears first, a later identity slot can reset within each unrelated partition and cannot justify eviction.

For aggregate DISTINCT, each `AggState` belongs to a complete group key. Group slots are therefore fixed when reasoning about one state's input subsequence. Remove those group slots from the exact input order; a DISTINCT argument is monotonic within the group only when it is the first remaining varying slot. Separately, an initial run of group slots partitions the group map. If that leading run covers the complete group key, the evaluator can emit and discard one group at a time.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeParallelPipelines.java` owns the process-wide executor used by native row pipelines. `LmdbNativeParallelAggregation.java` shares it. `core/model/src/main/java/org/eclipse/rdf4j/model/impl/DynamicModel.java` interns equal RDF values in `canonicalValues` while its compact map representation is active. `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet.java` lazily resolves native ids through a live `NativeLmdbQuerySource`. `core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/SPARQLParser.java` rewrites the LMDB-specific `STABLE_INDEX` spelling before invoking the generated SPARQL parser.

## Plan of Work

First, follow Routine A for the four isolated behavior bugs. Add the smallest test for each issue and run each method before changing its production class. Append compact Surefire evidence to the existing root `initial-evidence.txt`; do not overwrite prior entries. The parser test must parse a prefixed local name containing `\#` followed later on the same line by `ORDER BY STABLE_INDEX(?s)`. The model tests must demonstrate that an empty model releases all canonical references and that churn while one live statement remains is bounded. The LMDB result test must drain a native result, close its connection, shut down the repository, then read every retained binding. Parallel admission tests must reserve the complete budget and prove a second qualifying row pipeline and aggregation fall back without submitting a partial task group.

Implement a package-private process-wide admission controller next to `LmdbNativeParallelPipelines`. Read `rdf4j.lmdb.parallel.maxTasks` for each admission attempt, default to `Runtime.getRuntime().availableProcessors()`, and clamp to 1 through 65. Back the singleton with a bounded daemon `ThreadPoolExecutor` whose physical maximum is the legal 65-task ceiling; atomic admission enforces the smaller current property value and prevents its bounded queue from accumulating excess long-lived work. A compare-and-set reservation must acquire all requested permits before sources or tasks are opened. A row pipeline reserves `configuredThreads() + 1`; parallel aggregation reserves `configuredThreads()` because its producer remains on the query thread. If reservation fails, return the existing sequential fallback. Every construction failure, task failure, cancellation, early close, and normal exhaustion must release exactly once after owned tasks are terminal.

In `DynamicModel`, make canonical cache lifecycle explicit. All compact-map removal paths must report removed-statement debt, including pattern removal, `Iterator.remove`, `remove(Object)`, `removeAll`, `retainAll`, term-view mutation, `removeTermIteration`, and `clear`. When no statements remain, immediately clear the cache and reset debt even if `clear()` is called after another removal already emptied the model. Otherwise rebuild the cache from all live statement subject, predicate, object, context, and recursively nested triple-term components when debt reaches `max(1024, canonicalValues.size() / 2)`. The cache remains an implementation detail and is rebuilt after Java deserialization. Add a package-private size accessor for deterministic same-package tests rather than reflection.

In `NativeProjectedBindingSet`, allow the source reference to be detached. `materializeAndDetach()` must resolve every bound id, call `init()` for any `LmdbValue`, retain the resolved `Value` objects, and only null the source after all values succeed. `LmdbStoreConnection.evaluateInternal().next()` must call this method for native rows while preserving the existing eager initialization of ordinary binding sets. Native execution remains lazy internally; only the Sail-facing result boundary materializes.

In `SPARQLParser.rewriteStableIndexFunctions`, treat `#` as a comment introducer only when it is not escaped. Count consecutive backslashes immediately before the hash: odd means escaped PN_LOCAL content and even means comment syntax. Existing IRI and quoted-literal copying remains unchanged.

Then add ordered-DISTINCT strategy tests and internal order metadata. Introduce a package-private immutable `NativeSlotOrder` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`. It contains the complete varying-slot sequence, supports direct-alias normalization, and derives tuple `GLOBAL_HASH`, `PARTITIONED_HASH`, or `ADJACENT` decisions plus aggregate group-prefix and per-argument monotonic decisions. Add an exact ordered-scan request to `NativeLmdbQuerySource` and its concrete sources so the selected `TripleIndex` and complete field sequence remain paired. Fixed constants and slots bound by the enclosing row are omitted from the varying order.

Order propagation is fail-closed. Filters, projection copies, and direct aliases preserve the incoming order. A left-deep nested-loop join preserves only its outer input's proven order; the first implementation never appends inner order because it can reset for repeated outer rows. Left join and minus preserve a proven outer prefix when their implementation emits all expansion rows before advancing the outer row. Hash joins, factorized rows, spill sorts with a different comparator, and parallel pipelines report no usable natural order. An optimizer candidate may reorder join children so a pattern containing the desired group and argument slots becomes the outer anchor.

For union, add an ordered merge cursor only when every branch supplies the same requested signature in the shared native id space. Each branch owns its own `RowState`; the merge keeps one snapshot per branch, compares the complete order with unsigned native identifiers and a consistent unbound sentinel position, copies the winning snapshot to the consumer row, then advances that branch. This preserves bag multiplicity. `CompositeNativeLmdbQuerySource` uses the same complete-vector merge across active sources because it is only constructed when they share one id space. Concatenation remains the fallback when no order is requested.

Refactor aggregate DISTINCT membership by unique argument. Each `AggregateSpec` that is DISTINCT and duplicate-sensitive (`COUNT`, `SUM`, or `AVG`) references one channel shared by every specification with the same slot or constant. A hash channel owns the existing `LongHashSet`. A monotonic channel owns an initialized flag and last native id. A distinct constant owns a scalar seen flag. `AggState.add` computes freshness once per channel per row and applies it to every dependent aggregate. `MIN` and `MAX` remain without DISTINCT state because duplicates cannot change their result. Parallel and factorized partial states always construct hash channels so merging remains set union.

Generalize ordered aggregation before the parallel/factorized dispatch. A complete group prefix streams one `AggState`; a partial prefix stores only complete groups in the current prefix partition and flushes them when the prefix changes; no prefix uses the existing `LongAggStateMap`, `PrimitiveTupleTable`, or object map. A monotonic distinct channel uses last-id state inside each complete group even when groups interleave. The planner uses an ordered specialization only when it eliminates at least one hash channel, then ranks candidates by number of eliminated channels, complete group streaming, group-prefix length, existing estimate, and deterministic index order.

Update `NativeDistinctTracker` to accept a derived tuple strategy. Global mode keeps its current state. Partitioned mode stores the current prefix and hashes only residual identity slots, clearing on a real prefix transition. Adjacent mode compares the complete identity with a reusable previous-key buffer and allocates no table. Give `PrimitiveTupleTable` a clear operation that visits occupied buckets rather than filling its entire capacity, and clear the wide-key `HashMap` normally. Do not add per-row atomics; verify strategy selection with package-private plan inspection or explain output.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`. The initial baseline command has already passed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

For each Routine A bug, run its focused test through `mvnf` with retained logs before and after the production change. Representative commands are:

    python3 .codex/skills/mvnf/scripts/mvnf.py SPARQLParserTest#escapedHashInPrefixedNameDoesNotHideStableIndex --module core/queryparser/sparql --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py DynamicModelTest#removedCanonicalValuesDoNotAccumulate --module core/model --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLazyResultAfterCloseTest#nativeRowsRemainReadableAfterRepositoryShutdown --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelPipelinesTest#reservedWorkerGroupForcesSequentialFallback --module core/sail/lmdb --retain-logs

After focused tests pass, run the affected classes and modules:

    python3 .codex/skills/mvnf/scripts/mvnf.py SPARQLParserTest --module core/queryparser/sparql --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py DynamicModelTest --module core/model --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLazyResultAfterCloseTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelPipelinesTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelAggregationTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryparser/sparql --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/model --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before final verification, run `cd scripts && ./checkCopyrightPresent.sh`, then format with:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Build the LMDB benchmark jar and run `ThemeQueryBenchmark.executeQuery` on JDK 26 with one fork, four 3-second warmups, and four 2-second measurements for `(LIBRARY, 6)` and `(HIGHLY_CONNECTED, 6)`. Use the repository benchmark helper where its options can express the selector; otherwise invoke the generated JMH jar with equivalent `-f 1 -wi 4 -w 3s -i 4 -r 2s` settings. Run `NativeExecutionKernelBenchmark.groupByTwoKeys` and `ParallelAggregationBenchmark` as controls under identical baseline/candidate JVM settings.

Profile both target invocations with the repository macOS async-profiler workflow after reading its skill instructions. Capture CPU and allocation evidence if practical. The expected CPU profiles contain no aggregate calls to `LongHashSet.add`, `LongHashSet.rehash`, or `PrimitiveTupleTable.findOrInsert` for either target.

## Validation and Acceptance

The parser accepts `PREFIX ex: <urn:> SELECT * WHERE { ex:foo\#bar ?p ?o } ORDER BY STABLE_INDEX(?o)` and still recognizes an ordinary unescaped comment. The focused parser class and complete parser module pass.

An empty compact `DynamicModel` retains zero canonical values after any removal route. Repeated unique add/remove churn while one statement remains keeps cache size bounded by live canonical values plus the amortized threshold. Java serialization round trips rebuild a cache containing only live values. The focused model class and complete model module pass.

Every binding of a drained native result remains readable after connection close and repository shutdown. Internal native result construction still avoids value materialization before crossing the Sail connection boundary. The focused LMDB result tests pass.

At no time can active parallel tasks exceed `rdf4j.lmdb.parallel.maxTasks`. A query whose complete task group cannot be reserved runs sequentially; it never submits a partial producer/worker group. Reservations return to zero after success, early close, task failure, and cancellation. Parallel-enabled results match sequential native and generic results.

Tuple DISTINCT emits exactly one `(person, birthday)` for an ordered birthday partition containing `person1, person2, person1`; its residual set is cleared only when birthday changes. Complete `(birthday, person)` order selects adjacent mode and allocates no tuple table. Name-first order selects global hash and remains correct. Aggregate tests demonstrate group-only, argument-only, `(group, argument)`, multiple-argument, shared-channel, optional/unbound, union-overlap, multi-source, and early-close behavior against generic execution.

On JDK 26, `LIBRARY/6` is at most 3635 ms/op and `HIGHLY_CONNECTED/6` is at most 502 ms/op. Neither target profile contains aggregate `LongHashSet` or `PrimitiveTupleTable` work. `NativeExecutionKernelBenchmark.groupByTwoKeys` and the parallel aggregation controls regress by no more than 3 percent. If any acceptance threshold fails, do not claim completion: preserve the result, profile the remaining cost, update this ExecPlan, and continue with the smallest root-cause change.

## Idempotence and Recovery

All inspection, build, and test commands are safe to repeat. `mvnf` deliberately removes only stale module test reports and rebuilds the workspace-local `.m2_repo`; it must never be invoked with test-time `-am` or `-q`. Preserve every untracked artifact. If offline Maven resolution fails, rerun the exact command once without `-o`, then return to offline execution.

The working tree already contains unrelated edits in `.agent/execplan-lmdb-double-tps.md`, `.agent/execplan-lmdb-load-ten-x.md`, and two `jmh-benchmark-compare` skill scripts. Do not overwrite, stage, or reformat them. Do not use destructive Git commands. `initial-evidence.txt` contains earlier evidence and must only be appended. New Java source files require the repository copyright header and `// Some portions generated by Codex` immediately below it.

## Artifacts and Notes

The pre-change profiles supplied in this workspace attribute approximately 8.17 percent of `LIBRARY/6` CPU and 1.71 percent of `HIGHLY_CONNECTED/6` CPU to `LongHashSet.add`, with additional rehash cost. The July 9 historical acceptance values are 3563.356 ms/op for `LIBRARY/6` and 492.219 ms/op for `HIGHLY_CONNECTED/6`. The approved thresholds allow at most two percent above those values.

The root baseline transcript ended with:

    [INFO] RDF4J: LmdbStore ................................... SUCCESS [  4.526 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  33.286 s (Wall Clock)

## Interfaces and Dependencies

The only user-visible interface addition is the system property `rdf4j.lmdb.parallel.maxTasks`. Its integer value is the process-wide maximum active native parallel tasks, defaults to available processors, and is clamped to 1 through 65. Existing `rdf4j.lmdb.parallel.threads`, `rdf4j.lmdb.parallel.enabled`, and root-estimate properties retain their meaning.

All ordered-DISTINCT interfaces are package-private within `org.eclipse.rdf4j.sail.lmdb.evaluation`. `NativeSlotOrder` represents exact varying-slot order. The tuple strategy has `GLOBAL_HASH`, `PARTITIONED_HASH`, and `ADJACENT` modes with prefix and residual slot arrays. Aggregate membership channels have hash, monotonic, and constant-once behavior. `NativeProjectedBindingSet.materializeAndDetach()` is public only as required for the existing cross-package Sail connection boundary and remains annotated for internal/experimental use.

No new dependency, storage format, supported query API, or SPARQL semantic extension is introduced. The escaped-hash change only makes the existing preprocessor respect valid SPARQL prefixed local-name escaping. Commits use `GH-0000` because no issue number was provided. Pushing is not part of this plan unless the user requests it separately.

Plan revision note (2026-07-13 11:35Z): created the initial self-contained execution plan from the approved design, current source audit, baseline build, and historical benchmark/profile evidence.

Plan revision note (2026-07-13 11:58Z): recorded all isolated failing/passing evidence, the bounded-executor design refinement, and the transition to ordered-DISTINCT tests.

Plan revision note (2026-07-13 13:17Z): recorded the completed ordered implementation, new correctness discoveries, 18 differential frontier cases, and the 135-test LMDB neighborhood pass before module verification.

Plan revision note (2026-07-13 15:13Z): recorded the dispatch/allocation control refinements, exact JDK 26 benchmark comparisons, final profiles, 1568-test LMDB pass, and final root clean-install result. All acceptance gates are complete.
