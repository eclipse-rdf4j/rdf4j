# Sparse two-pattern join aggregation

This experimental native LMDB strategy computes grouped counts directly from weighted adjacency runs. It is based on HASMAC-AS/rdf4j `main` at `ec7e48248eb20c4f7681d8ea5cbed3100718527a`. The strategy is named `sparseJoinAggregate` and is **disabled by default**.

For example:

```sparql
SELECT ?left ?right (COUNT(?join) AS ?count)
WHERE {
  ?left <urn:p> ?join .
  ?join <urn:q> ?right .
}
GROUP BY ?left ?right
HAVING (COUNT(?join) >= 10)
```

`COUNT(*)` also works. In this mandatory fragment the shared variable is always bound, so these two counts agree. The existing serial packed factorized implementation accepts the bound-variable form; the benchmark uses that same form for all three routes and checks the star form separately.

## Enable and select

Enable the candidate before preparing the query:

```java
System.setProperty("rdf4j.lmdb.sparseJoinAggregate.enabled", "true");
```

The native engine needs canonical physical term IDs and complete adjacency views whose runs are ordered by unsigned `(neighbor, context)`. Existing native-evaluation/direct-adjacency store configuration and snapshot ownership remain authoritative. The benchmark explicitly builds the persistent adjacency before running queries; the new strategy does not build a persistent index on demand.

For an exact comparison, use the existing public force API on a `SailTupleQuery`:

```java
SailTupleQuery query = (SailTupleQuery) connection.prepareTupleQuery(sparql);
query.setForcedLmdbExecutionStrategy("sparseJoinAggregate");
try (TupleQueryResult result = query.evaluate()) {
    while (result.hasNext()) {
        BindingSet group = result.next();
        // Consume the endpoint bindings and count.
    }
}
```

This is a selection example; the complete executable fixtures are in `SparseJoinAggregateAdmissionTest` and `SparseJoinAggregateBenchmark`. A forced strategy that is unavailable raises the existing forced-strategy error. With automatic selection, ineligible queries and clean resource refusals continue through existing alternatives. Enabling the property admits a candidate into the existing arbiter; it does not guarantee that the arbiter will select it.

The strategy publishes `sparseJoinAggregateWork` in executed explanations. Its counters report right rows/fibers/endpoints, left rows/fibers, multiply-adds, pruned rows/products, and emitted groups. These are work counts, not elapsed-time estimates. The physical plan deliberately supplies no invented cost quote: remapping cost, overlap, and output density are not determined by input cardinalities alone.

## Computation and storage

Let `L(x,z)` and `R(z,y)` be positive multiplicities after the query's context restrictions have been applied. The output is:

```text
C(x,y) = sum over matching z of L(x,z) * R(z,y)
```

`SparseJoinAggregateKernel.multiply` performs row-wise sparse multiplication. It reads the right adjacency plane once, combines duplicate neighbors, assigns observed right endpoints dense integer ordinals, and stores compressed rows. The left side stays a run stream. For one left endpoint, each matching right row contributes directly into an ordinal-indexed counter array. A touched-ordinal list identifies the only counters that need emission and clearing. The kernel never creates an intermediate binding set for each joined pair, and never allocates an endpoint-by-endpoint matrix.

Unit-weight right planes omit the weight array. Weighted fibers use checked multiplication and addition. All 64 bits of physical IDs are supported; IDs are hash keys rather than direct array subscripts. Real result counts follow the current factorized arithmetic and fail above `Long.MAX_VALUE`. Saturation is used only for safe pruning bounds and diagnostic work counters.

If `J` is the number of shared rows on the right, `F_R` the number of right fibers, `Y` the number of observed right endpoints, and `D_L` the largest relevant left run, working memory is `O(J + F_R + Y + D_L)`, plus bounded input buffers and the separately owned result replay. The left-run buffer is needed only for thresholds greater than one. Primitive maps have expected constant-time lookups. With no pruning, arithmetic work is proportional to the number of matching fiber products; setup also scans the input runs, and emission costs at least the number of output groups.

The implementation still remaps the entire eligible right plane on each execution. The adapter copies bounded batches of raw neighbors and contexts, then folds accepted incidences into weights; it still scans the represented quads. This preserves cancellation responsiveness inside a large repeated-context run. Output density can dominate result construction, and a low-fanout join can be cheaper through an existing route. There is no cross-query matrix cache, AMX path, native library, or JNI dependency.

## HAVING bounds

For an inclusive required count `t > 1`, preparation computes the maximum weight of each right row and the sum of each right column. For one left endpoint, two upper bounds are safe:

1. `sum_z L(x,z) * max_y R(z,y)` bounds every output count in that left row. If it is less than `t`, the entire row is skipped.
2. `max_z L(x,z) * sum_z R(z,y)` bounds the count at one right endpoint. Preparation converts the column sum to `ceil(t / columnSum)`, leaving one comparison per possible contribution in the inner loop.

Duplicate left fibers are combined before computing their maximum. Bound arithmetic saturates at `Long.MAX_VALUE`, so an overflow cannot turn a large upper bound into an unsafe small one. Column pruning skips counter updates but still visits the corresponding right entries. A loose bound may prune nothing; a threshold returning an empty result does not by itself prove that all multiplication was avoided.

The integration recognizes necessary exact-integer comparisons through the existing HAVING helper, including reversed comparisons and supported conjunctions. Strict comparisons are adjusted without overflow. Decimal comparisons, disjunctions, and other expressions remain with the existing filter. **The complete outer HAVING expression always remains authoritative**, including its expression-error behavior.

## SPARQL boundaries

| Input or aggregate | Behavior |
| --- | --- |
| Exactly two mandatory fixed-predicate patterns, sharing one subject/object variable | Eligible when their other two endpoint variables are exactly the group keys |
| Subject-subject, subject-object, object-subject, object-object joins | Supported |
| Predicate self-join | Supported; both pair orders and diagonal groups are retained |
| `COUNT(*)` or non-distinct COUNT of a proven-bound endpoint/shared variable | Supported |
| Multiple equivalent supported COUNT outputs, including HAVING's anonymous output | Supported |
| Fixed `GRAPH` and compiled dataset context restrictions | Filtered before multiplicities are formed |
| The same incidence in several admitted contexts | Preserved as a weight; the product of input weights contributes to COUNT |
| Optional inputs, extra patterns, residual filters, VALUES/extension input, extra join dimensions | Existing alternatives |
| Graph variables, repeated pattern slots, range-constrained patterns, bound incoming active slots | Existing alternatives |
| DISTINCT, other aggregates, possibly unbound COUNT arguments | Existing alternatives |
| Expression COUNT, including parser-lowered `COUNT(1)` and `COUNT(1 / 0)` | Existing alternatives; no new expression-error or unbound semantics |
| Empty or disjoint inputs | No grouped result rows; zero-count endpoint pairs are not synthesized |

The adapter follows RDF4J's existing physical/context semantics. It does not impose a new interpretation of default datasets. Generic-versus-sparse tests exercise unrestricted contexts, fixed graphs and `FROM` datasets. Repeating an identical quad in the fixture would be deduplicated by storage; benchmark weights therefore come from distinct contexts.

## Ownership and operational behavior

Workspace arrays, primitive maps, context buffers, and transient array growth are charged to the query's existing memory account before allocation. Growth accounts for old and replacement arrays simultaneously. Producer cursors, adjacency views and probes close before a successful result replay is transferred. A refusal after some groups have been buffered closes that private replay and releases its claims before the automatic dispatcher can select another strategy.

Input adaptation checks cancellation for each bounded batch; kernel input, remapping, growth, multiplication and output loops poll cancellation. The adapter also polls while searching a large context constraint. Existing probe materialization limits apply to emitted groups. Explicit resource unwinding preserves a real cleanup failure even when the primary cancellation control disables suppressed exceptions.

## Verification and reproduction

The completed [measurement report](sparse-join-aggregate-results/2026-11-04/report.md) includes the exact validation scope, raw records, environment, and interpretation. The preselected high-fanout JMH case measured 2.039× over serial AUTO and 3.017× over packed factorized execution; the low-fanout case did not show a reliable gain. This evidence supports keeping the strategy opt-in.

The four new test classes are `SparseJoinAggregateAdmissionTest`, `SparseJoinAggregateKernelTest`, `SparseJoinAggregateSemanticsTest`, and `SparseJoinAggregateAdapterTest`. They cover the public query route, exact weighted arithmetic and randomized reference comparisons, graph/SPARQL parity, memory refusal, private replay lifetime, cancellation, overflow, full-width IDs and boundary-spanning duplicate runs. Existing adjacency, strategy-arbiter, strategy-preference, aggregate-explanation and output-memory tests are also used for regression checking.

See [`scripts/sparse-join-aggregate-benchmarks.md`](../../../scripts/sparse-join-aggregate-benchmarks.md) for complete reproduction commands and timing definitions. The benchmark includes fresh query preparation, optimization, right-plane remapping, accumulator allocation, aggregation and full result draining in every timed operation. Fixture loading and persistent adjacency construction are recorded separately. Every result is checked against an independent weighted oracle after the timer stops. First-use and warmed samples are separate; first-use means a query on an already loaded, adjacency-built store, with no filesystem-cache eviction.

## Source of the algorithm

The sparse accumulation and aggregate-bound approach is inspired by [Tessera](https://arxiv.org/abs/2610.40620). The design was informed by source review of its published [C++ accumulation core](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/core/tessera.cpp), [query preprocessing](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/core/query.cpp), and [reproduction entry point](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/repro/run.sh). The Java kernel is integrated with RDF4J's existing IDs, memory, cancellation, graph semantics and execution arbiter. Tessera itself was not benchmarked here, and its reported speedups and hardware results are not RDF4J measurements.
