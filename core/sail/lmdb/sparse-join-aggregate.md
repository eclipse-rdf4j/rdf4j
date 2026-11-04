# Sparse two-pattern join aggregation

The native LMDB planner can compute grouped counts directly from weighted adjacency runs with the `sparseJoinAggregate` strategy. It is **available by default** and participates in ordinary aggregate selection, physical cost estimates, adaptive execution evidence, explanation, and resource fallback. No sparse-specific enable switch or forced strategy is required.

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

`COUNT(*)`, `COUNT(1)`, and counts of guaranteed-bound aliases also work. In this mandatory fragment they count the same solution multiplicity. The existing serial packed factorized implementation accepts the bound-variable form; the benchmark uses that common form for comparisons and checks the star form separately.

## Automatic selection and diagnostics

Ordinary query preparation considers the strategy automatically. To explicitly disable it before preparing a query:

```java
System.setProperty("rdf4j.lmdb.sparseJoinAggregate.enabled", "false");
```

The native engine needs canonical physical term IDs and complete adjacency views whose runs are ordered by unsigned `(neighbor, context)`. Existing native-evaluation/direct-adjacency store configuration and snapshot ownership remain authoritative. In this repository, a plain `LmdbStoreConfig` still defaults to the generic evaluator with direct adjacency disabled; this feature is automatically available within a configured native evaluator. The benchmark enables that evaluator and explicitly builds the persistent adjacency before running queries. The strategy does not build a persistent index on demand.

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

This is a diagnostic selection example; complete executable fixtures are in `SparseJoinAggregateAdmissionTest`, `SparseJoinAggregateAutomaticTest`, and `SparseJoinAggregateBenchmark`. A forced strategy that is unavailable raises the existing forced-strategy error. With automatic selection, ineligible queries and clean resource refusals continue through existing alternatives. Default availability does not guarantee selection for every eligible query.

The strategy publishes `sparseJoinAggregatePlan` and `sparseJoinAggregateWork` in executed explanations. The first identifies streamed and retained endpoint slots, binding names and directions, context-processing modes, and whether threshold pruning is active. Its `contextBuffers` field is a structural bound of zero, one, or two fixed context arrays; each holds 256 IDs and is allocated only when that input needs context filtering. The second reports retained rows/fibers/endpoints, streamed rows/fibers, multiply-adds, pruned rows/products, and emitted groups. After a transpose, “right” in the kernel counters names the chosen retained plane, not necessarily the second pattern as written in SPARQL. These counters describe actual work, not elapsed-time estimates.

The common `nativeAggregateCostQuotesEstimated` telemetry includes the sparse producer's physical quote. Bounded, snapshot-valid predicate counts and directional key-domain counts establish finite bounds for scanning, dictionary setup, probing, product updates, and output groups. The planner does not open input cursors to obtain that quote. Fixed graphs and dataset context restrictions also receive finite quotes when this metadata is available: filtering cannot increase the whole-plane domains or product bounds. Scoped inputs are charged for raw quad traversal, bounded context-membership comparisons, and their fixed context buffers. These are conservative bounds, not exact filtered cardinalities. Missing snapshot metadata leaves the candidate unpriced.

Shared-domain overlap and the distribution of degrees are unknown from these totals. Product and output lower bounds remain zero, and upper bounds remain conservative. The central product estimate uses uniform shared-key degrees and containment: `Fleft * Fright / max(Sleft, Sright)`, capped by the product bound, where `F` is the metadata-derived fiber estimate and `S` is the shared-key domain size. The central group estimate is capped by both expected products and the endpoint cross product. These assumptions affect expected work only; they never narrow the bounds for disjoint domains, skew, graph filtering, or split physical fibers.

For unrestricted contexts, a branching or provably weighted contraction with a kernel-state bound inside the query memory cap can be a normal first choice before timing evidence exists. This is a proof of the executable representation and bounded state, not a claim that every such query is faster. Unit-fanout input has no such initial preference. Scoped inputs also receive no cold-choice proof, because whole-plane branching does not establish useful contraction within the selected graphs. They remain eligible for normal cost and adaptive comparison. Completed timing evidence still participates in the common arbiter and can select another strategy.

The sparse producer also fills the existing adaptive cardinality and fanout bands from its captured quote. Power-of-two bands describe total expected scans, products and groups, and the expected product-to-scan ratio. This keeps exact-variant timing evidence for materially different work sizes and expansion separate, while retaining shared family evidence. The bands are unchanged by transposition or variable renaming; physical orientation and context processing remain separate recipe properties. This refinement changes neither the cost vector nor its uncertainty, and producers without a refinement retain their existing keys.

The quote also marks sparse execution as blocking: multiplication and the private result replay finish before the first group is exposed. A downstream LIMIT therefore cannot discount this completed setup and aggregation work.

Sparse is registered with the existing bounded-specialist exploration policy. An unmeasured candidate can receive a deadline-limited trial once an exactly measured fallback is available; the existing query budget, spacing, cancellation, cooldown and confirmation rules apply. A measured slower specialist receives no mandatory repeated confirmations. As with the other specialists, structural admission can also keep a legal candidate available when metadata is missing, while execution still checks adjacency availability and the query memory ledger before any results escape.

## Computation and storage

Let `L(x,z)` and `R(z,y)` be positive multiplicities after the query's context restrictions have been applied. The output is:

```text
C(x,y) = sum over matching z of L(x,z) * R(z,y)
```

`SparseJoinAggregateKernel.multiply` performs row-wise sparse multiplication. Preparation compares conservative peak memory bounds for both orientations, then binds one immutable recipe for pricing and execution. Transposing the multiplication preserves the endpoint bindings and counts while allowing the plane with the smaller state bound to be retained. Context constraints travel with their patterns, and both orientations account for the same bounded context scratch space. Missing metadata preserves the original deterministic orientation.

The kernel reads the chosen right adjacency plane once, combines duplicate neighbors, assigns observed right endpoints dense integer ordinals, and stores compressed rows. The left side stays a run stream. For one left endpoint, each matching right row contributes directly into an ordinal-indexed counter array. A touched-ordinal list identifies the only counters that need emission and clearing. The kernel never creates an intermediate binding set for each joined pair, and never allocates an endpoint-by-endpoint matrix.

Unit-weight right planes omit the weight array. Weighted fibers use checked multiplication and addition. All 64 bits of physical IDs are supported; IDs are hash keys rather than direct array subscripts. Real result counts follow the current factorized arithmetic and fail above `Long.MAX_VALUE`. Saturation is used only for safe pruning bounds and diagnostic work counters.

If `J` is the number of shared rows on the right, `F_R` the number of right fibers, `Y` the number of observed right endpoints, and `D_L` the largest relevant left run, working memory is `O(J + F_R + Y + D_L)`, plus bounded input buffers and the separately owned result replay. The left-run buffer is needed only for thresholds greater than one. Primitive maps have expected constant-time lookups. With no pruning, arithmetic work is proportional to the number of matching fiber products; setup also scans the input runs, and emission costs at least the number of output groups.

The implementation remaps the entire eligible retained plane on each execution. For unrestricted contexts, the adapter consumes physical neighbor/multiplicity batches directly when the positioned source cursor certifies bounded physical copying. Both direct and decoded adjacency cursors expose this capability for immutable single-page rows, and bounded cursor wrappers preserve it. Multiplicities therefore need not be expanded into individual quads and then recombined.

The capability is checked again after every row advance. Context-filtered, merged, generic, and multi-page rows use bounded quad batches and form weights after filtering. This fallback matters because the storage layer's general fiber copier may traverse a long same-neighbor extent chain before returning; compressed representation alone does not prove cancellation responsiveness. Output density can dominate result construction, and a low-fanout join can be cheaper through an existing route. There is no cross-query matrix cache, AMX path, native library, or JNI dependency.

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
| `COUNT(*)`, `COUNT(1)`, or non-distinct COUNT of a proven-bound endpoint/shared variable or alias | Supported |
| Pure constant/copy assignments above the join | Supported when targets are fresh and sources are already guaranteed bound |
| Multiple equivalent supported COUNT outputs, including HAVING's anonymous output | Supported |
| Fixed `GRAPH` and compiled dataset context restrictions | Filtered before multiplicities are formed |
| The same incidence in several admitted contexts | Preserved as a weight; the product of input weights contributes to COUNT |
| Optional inputs, extra patterns, residual filters, guarded VALUES input, extra join dimensions | Existing alternatives |
| Graph variables, repeated pattern slots, range-constrained patterns, bound incoming active slots | Existing alternatives |
| DISTINCT, other aggregates, possibly unbound COUNT arguments | Existing alternatives |
| Computed/effectful expression COUNT, including `COUNT(1 / 0)` | Existing alternatives; no new expression-error or unbound semantics |
| Empty or disjoint inputs | No grouped result rows; zero-count endpoint pairs are not synthesized |

The adapter follows RDF4J's existing physical/context semantics. It does not impose a new interpretation of default datasets. Generic-versus-sparse tests exercise unrestricted contexts, fixed graphs and `FROM` datasets. Repeating an identical quad in the fixture would be deduplicated by storage; benchmark weights therefore come from distinct contexts.

## Ownership and operational behavior

Workspace arrays, primitive maps, context buffers, and transient array growth are charged to the query's existing memory account before allocation. Growth accounts for old and replacement arrays simultaneously. Producer cursors, adjacency views and probes close before a successful result replay is transferred. A refusal after some groups have been buffered closes that private replay and releases its claims before the automatic dispatcher can select another strategy.

Input adaptation checks cancellation for each bounded batch; kernel input, remapping, growth, multiplication and output loops poll cancellation. The adapter also polls while searching a large context constraint. Output uses the existing byte-accounted, spillable replay, so the probe's private-array row ceiling does not limit the total number of groups. Every output still checks cancellation before allocation, and replay append, spill and finish retain their deadline checks. Resident memory and cooperative execution time remain bounded; the replay contract does not impose a fixed total spill-file size. Kernel allocation refusal can decline the candidate, while replay owner or spill-workspace refusal keeps the existing query-error behavior. Explicit resource unwinding preserves a real cleanup failure even when the primary cancellation control disables suppressed exceptions.

## Verification and reproduction

The [automatic-integration report](sparse-join-aggregate-results/2026-11-04-integration/report.md) records the completed implementation, 447 passing focused and adjacent tests, and complete-query comparisons of default automatic, explicitly disabled, and forced sparse execution. It preserves raw measurements, setup and route-witness definitions, test-first failures, and adverse cases. Automatic selection has planning and learning costs; these measurements do not establish a speedup for every eligible query.

The original [measurement report](sparse-join-aggregate-results/2026-11-04/report.md) records the opt-in PR head and remains historical evidence. Its preselected high-fanout JMH case measured 2.039× over serial AUTO and 3.017× over packed factorized execution; the low-fanout case did not show a reliable gain. That result motivates selective planning and explicit adverse-case measurements, not unconditional dispatch to sparse.

The sparse test classes cover public default and forced query routes, automatic selection and opt-out, physical quote/transpose/memory bounds, exact weighted arithmetic and randomized reference comparisons, graph/SPARQL parity, constants and aliases, memory refusal, private replay lifetime, cancellation, overflow, full-width IDs, and boundary-spanning duplicate runs. Physical cursor tests also verify direct and decoded fiber capabilities. Existing adjacency, strategy-arbiter, strategy-preference, aggregate-explanation, physical-grouping cost, and output-memory tests provide regression coverage.

See [`scripts/sparse-join-aggregate-benchmarks.md`](../../../scripts/sparse-join-aggregate-benchmarks.md) for complete reproduction commands and timing definitions. The benchmark includes fresh query preparation, optimization, right-plane remapping, accumulator allocation, aggregation and full result draining in every timed operation. Fixture loading and persistent adjacency construction are recorded separately. Every result is checked against an independent weighted oracle after the timer stops. First-use and warmed samples are separate; first-use means a query on an already loaded, adjacency-built store, with no filesystem-cache eviction.

## Source of the algorithm

The sparse accumulation and aggregate-bound approach is inspired by [Tessera](https://arxiv.org/abs/2610.40620). The design was informed by source review of its published [C++ accumulation core](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/core/tessera.cpp), [query preprocessing](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/core/query.cpp), and [reproduction entry point](https://github.com/GyuyeongKim/Tessera-public/blob/b6f6f8d3064c003f742c357ae6272667c7841334/repro/run.sh). The Java kernel is integrated with RDF4J's existing IDs, memory, cancellation, graph semantics and execution arbiter. Tessera itself was not benchmarked here, and its reported speedups and hardware results are not RDF4J measurements.

The central product estimate uses the uniform-degree/containment baseline also found in PostgreSQL's [`eqjoinsel_inner`](https://github.com/postgres/postgres/blob/REL_18_STABLE/src/backend/utils/adt/selfuncs.c#L2612) when common-value statistics are unavailable (source reviewed 2026-11-04, file blob `3abc6c1ca7dbcb2fcdfade8b00864dbef2832840`). Here it is an assumption-bearing central estimate accompanied by independent conservative bounds and adaptive execution evidence.
