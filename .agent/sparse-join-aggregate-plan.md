# Add a sparse two-pattern join aggregate

This ExecPlan follows `.agent/PLANS.md`. It is a living description of the implementation, validation, and performance comparison requested for HASMAC-AS/rdf4j main at `ec7e48248eb20c4f7681d8ea5cbed3100718527a`.

## Purpose / Big Picture

Evaluate a mandatory join of two fixed-predicate triple patterns by counting matching weighted adjacency entries directly into endpoint groups. For `?left :p ?join . ?join :q ?right`, the result for each pair is the sum over join values of the left multiplicity times the right multiplicity. This is sparse matrix multiplication over nonnegative integer counts. It avoids constructing intermediate binding sets and never emits zero-count groups. The implementation is CPU-only, uses existing RDF4J storage and lifetime contracts, and compares against the existing factorized aggregate with all per-query setup included.

## Progress

- [x] Identify exact fork and base revision.
- [x] Inspect contracts and establish focused validation.
- [x] Implement weighted sparse kernel and integration.
- [x] Verify semantics, cancellation, and memory limits.
- [x] Measure fanout, density, and threshold effects.
- [x] Format sources and prepare complete deliverables.

## Surprises & Discoveries

A separate complete checkout was used for this feature. The repository requires Java 25 and a task-local Maven cache. A matching toolchain was available; missing dependencies were resolved before test execution.

## Decision Log

The initial semantic fragment is non-distinct COUNT over two mandatory patterns and grouping by their two other endpoint variables. Both endpoints and the shared variable must be proven bound by those patterns. Optional joins, additional shared variables, expression aggregates, and other unsupported shapes retain the current evaluator. Exact supported syntax will be recorded after planner inspection. This keeps SPARQL errors and unbound behavior with the established implementation.

The new route must be independently selectable for comparison, retain existing fallback paths, account for setup and accumulator memory, and poll cancellation in input and multiplication loops. There will be no AMX path and no new external dependency.

## Outcomes & Retrospective

The unchanged base passed a full Java 25 quick clean install. The in-repository public-query admission test failed with Unknown LMDB execution strategy sparseJoinAggregate, captured in initial-evidence.txt. The final focused/regression selection passes all 153 tests with zero failures, errors or skips. This includes 31 new tests and 122 existing adjacent tests. Passing evidence, the initial red report, all measured data, and the complete report are preserved in core/sail/lmdb/sparse-join-aggregate-results/2026-11-04.

The main sweep completed 36 workloads across three routes and three fresh JVMs, producing 2,592 retained raw samples with exact result checks. Preselected JMH checks used twelve independently forked JVMs. At fanout 32, sparse measured 2.039 times faster than serial AUTO and 3.017 times faster than packed factorized aggregation. At fanout one, sparse was slower by noisy JMH point estimates; two bounded-sweep packed regressions were consistent across all forks. The separate COUNT(*) sweep also completed with 768 raw records and exact parity. The strategy therefore remains opt-in. Empty-output speedups are reported separately, and work counters distinguish actual pruning from other execution savings.

The first expanded run exposed two test/admission assumptions: COUNT(1) is lowered to an expression extension and remains outside this fragment, and a directly constructed test RowState must be initialized before its unknown-slot invariant holds. Forced tests now use COUNT(*) and proven-bound COUNT(?join); expression COUNT retains generic-versus-automatic fallback parity checks. Production admission was not widened to accommodate an expression.

The factorizedTail candidate cannot group this query by both endpoints. The current packedFtreeAggregate accepts that grouping, but its serial path declines COUNT(*)'s visible-mapping reducer. The main comparison therefore uses COUNT(?join) for every route, which is exactly equivalent for these mandatory patterns; COUNT(*) is a separate ordinary-versus-sparse control. Tiny correctness sweeps confirmed the actual packed and sparse routes and exact weighted results before any performance claims.

## Context and Orientation

Production native query code lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`. `NativeLmdbQuerySource` defines adjacency views and probes. `LmdbNativeAggregatePlanner` and `LmdbNativeAggregateCompiler` choose and execute aggregation routes. `LmdbAdjacencyAggregatePlan` is the current single-pattern structural aggregate. `LmdbQueryMemoryManager` owns query memory reservations. Tests and benchmark code live in the matching `core/sail/lmdb/src/test/java` tree. `scripts/run-single-benchmark.sh` is the existing performance harness.

An adjacency run is a storage-owned sequence of neighbors for one predicate and endpoint. Multiple physical entries that represent the same logical endpoint are counted as a weight, not deduplicated. A counter for an endpoint pair receives the product of its two input weights. Sparse means storage is proportional to present entries or touched output groups, rather than all possible endpoint pairs.

## Plan of Work

First inspect existing source contracts and run the required initial build. Add focused tests demonstrating the absent sparse route before modifying production behavior. Implement the low-level weighted accumulation independently of planner admission so it can be checked against an exact reference. Add planner admission only for the proven fragment, use existing result decoding and HAVING semantics, and introduce safe aggregate bounds only where they cannot discard valid groups. Then check parity and fallback using real repositories and all supported edge directions.

Benchmark the sparse route and the existing factorized route on identical input snapshots. Vary fanout, endpoint-domain size (which changes output density), duplicate weights, and HAVING thresholds. Include initial remapping and accumulator construction in the measured operation, report first-use separately from warmed runs, and check result equality outside the timed region. Record actual path selection and workload counts so a fallback cannot be mislabeled as sparse execution.

## Concrete Steps

From the repository root, the local Maven wrapper is `/workspace/scratch/4fecb353a8fb/mvn-task`. It supplies Java 25 and this environment's proxy/trust configuration without modifying project sources. All commands use `-Dmaven.repo.local=.m2_repo`. The required initial command is:

    /workspace/scratch/4fecb353a8fb/mvn-task -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

If offline dependency resolution fails, repeat without `-o` and then return to offline use. Test commands must select `-pl core/sail/lmdb`, must not use `-am` or `-q`, and must preserve Surefire reports. Exact test class names and reproduction commands will be added as the implementation takes shape.

## Validation and Acceptance

For each supported query, the sparse result bag must equal the current factorized and ordinary evaluators. Include weighted duplicates, empty inputs, no matches, named/default context behavior, threshold boundaries, endpoint orientation, unbound or error-producing aggregates that must fall back, cancellation, and constrained memory. Arithmetic must not silently wrap. A low-fanout benchmark must be retained even if it exposes a regression. Performance claims must use completed measurements including setup rather than kernel-only extrapolation.

## Idempotence and Recovery

All code changes are isolated on `GH-0000-sparse-join-aggregate`. No issue number was supplied. Preserve unrelated and untracked files. Query-owned resources must close on success, refusal, cancellation, and failure; a refusal before publication can use the existing evaluator without leaking partial output. Reproduction scripts use temporary stores and write results to explicit paths, and can be rerun safely.

## Artifacts and Notes

Final deliverables will include complete changed source files, a Git patch, focused test evidence, benchmark data, and a report that separates measured results from limitations. Repository-backed source is persisted with Git rather than duplicated as Library files.

## Interfaces and Dependencies

Use existing Java collections or primitive structures and RDF4J query memory/cancellation interfaces. SparseJoinAggregateKernel.multiply consumes two WeightedRows streams (nextRow, key, read neighbors and weights), an inclusive minimum count, a private output sink, and the existing KernelRuntime memory/cancellation authorities. The right relation is compacted into sparse rows with dense endpoint ordinals. Unit weights omit the weight array. A reusable counter vector and touched ordinal list hold one left output row. HAVING bounds use both sum of left weights times per-shared-row right maxima, and maximum left weight times right column sums. Bounds saturate safely; actual COUNT uses the existing checked factorized arithmetic. LmdbSparseJoinAggregatePlan will be an opt-in sibling physical producer named sparseJoinAggregate, with rdf4j.lmdb.sparseJoinAggregate.enabled default false. The existing final HAVING filter remains authoritative. The borrowed adjacency view remains owned by its current query snapshot; no view may outlive its probe or snapshot.

Revision note: initial plan records scope, base revision, semantic boundaries, environment, and validation requirements before production changes.

Revision note: preserved the failing public-query test, landed the independently validated kernel and its tests, and specified the physical route and bounds.

Revision note: completed opt-in integration and ownership review, added context/semantic/resource tests, and identified a semantically identical common query for the existing serial factorized comparison.

Revision note: the final 153-test selection passes, including private-output refusal and suppression-disabled cancellation cleanup; measurement is now the sole active execution workload.

Revision note: all three measurement campaigns completed without correctness or route failures. Full data and a nuanced report are preserved. Sources are formatted; complete-file packaging and a binary-capable Git diff are derived from the final tree, with patch application and archive-content verification recorded in the handoff.
