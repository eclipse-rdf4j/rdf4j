# LMDB query optimization and statistics

LMDB planning combines a source-cardinality estimate, an optional sketch-based join-order planner, filter-pass estimates, and physical index-access costs. These are advisory planning inputs: missing, unready, or stale data reduces the planner's confidence and leads to conservative fallback; it does not make query evaluation depend on a statistics answer. The branch delta catalogued as Q8–Q9 covers LMDB planning rewrites and estimate consumers; the underlying statement/value statistics and generic optimizer contracts are included as dependencies, not attributed wholesale to this branch.

## Which optimizer pipeline is active

[`LmdbNativeEvaluationStrategyFactory`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategyFactory.java) selects an automatic optimizer pipeline from the `EvaluationStatistics` supplied for that evaluation:

- When `supportsJoinEstimation()` is true, it uses [`LmdbQueryOptimizerPipeline`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java). This assembles constant and expression rewrites, constraint splitting, same-term and union-scope normalization, projection/filter/iterative optimizations, LMDB filter simplification, the sketch join optimizer, VALUES filter optimization, order/limit rewriting, and LMDB ORDER BY optimization.
- Otherwise it uses [`LmdbStandardQueryOptimizerPipeline`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStandardQueryOptimizerPipeline.java), which delegates to RDF4J's standard pipeline through an order-blind triple source and inserts a small LMDB physical join-order pass only when the standard join estimator is not ready. The fallback pass preserves any plan containing a finite `BindingSetAssignment` anchor, and reorders only a leading contiguous prefix of ordinary statement patterns within a safe inner-join region.
- If an application supplies its own `QueryOptimizerPipeline`, the factory uses it rather than the automatic choice.

Pipeline selection occurs when the evaluation strategy is created. The estimation-readiness check is nonblocking; no query waits for a background sketch rebuild just to choose join order. Inspect the evaluation's query explanation and optimizer decision trace when determining which planner path actually ran.

## Statistics available to query planning

| Input | What it estimates | Availability and consumer |
|---|---|---|
| Statement-pattern cardinality source | Candidate rows for a specific subject/predicate/object/context pattern, from LMDB page/index statistics where possible. | Used by `LmdbEvaluationStatistics` for statement patterns even when robust sketch join estimation is unavailable; generic RDF4J cardinality is the fallback when this source cannot answer. The on-disk page-estimate lifecycle is covered by storage guides. |
| Sketch join estimator | Join and filter cardinality, pattern relationships, and access-path shapes used by join enumeration and factor costs. | Store-owned, built/refreshed asynchronously. `supportsJoinEstimation` is true only when it exists and `isReadyNonBlocking()` is true. Its persisted snapshots, staleness and rebuild control belong to storage; see [storage overview](storage-overview.md). |
| Predicate fan-out stats | Per-predicate statement count, estimated distinct subjects/objects, conditional mean fan-out, and estimated distinct probe keys. | A bounded store-lifetime advisory summary. Each tracked predicate uses two 64-register HLL sketches; admission uses a four-row, width-8,192 frequency sketch and a tracked-predicate cap of 4,096. Add/remove mutations update it; an aborted write clears advisory state. |
| Filter selectivity sidecar | Persisted observations and bounded samples for filter-on-pattern and filter-template pass ratios. | `LmdbFilterSelectivityStats` is connected to the sketch estimator when enabled. Its sample reservoir has 256 entries; the sidecar and sampler lifecycle is described with estimator storage. |
| Learned filter selectivity | Volatile pass/reject counts keyed by exact filter, template, then unqualified pattern. | `LmdbLearnedFilterSelectivity` is per store, not per query. It caps each tier at 4,096 entries and requires 16 rows of evidence for one tier; evidence is reported with counts so the optimizer's Wilson interval can widen appropriately. It is not persisted. |
| Node-domain synopsis/presence | Exact union of root nodes over a fixed-predicate SOC/OSC plane; one form preserves bag multiplicity, one is membership-only. | Query consumers use it to price or execute node-domain intersections, existence grouping, and selected aggregate routes. The synopsis may be refused by the shared adjacency-memory account; storage documents that budget and generation lifetime in [storage adjacency lifecycle](storage-adjacency-lifecycle.md). |

The predicate fan-out estimate divides the tracked predicate's triple count by the HLL estimate of distinct subject or object keys. The paired `distinctKeys` estimate is kept separate: expected rows per candidate key depends on both the chance that the key matches and its conditional fan-out. This distinction prevents a rare, high-fanout predicate from being priced as though every key matches. The HLL values are estimates, not exact counts; an absent predicate summary returns “unknown.” The fixed arrays and HLL registers bound retained sketch memory, while per-writer updates and synchronized reads avoid query-row hot-loop synchronization.

`LmdbNodeDomainSynopsis` is exact for the immutable plane it was built from: it unions roots across fixed-predicate domains, storing dense ID ordinal spans as bitmaps and sparse ID classes as unsigned sorted arrays. `LmdbNodeDomainPresence` retains membership without multiplicities. Dense/sparse intersections are performed with word operations or unsigned merges, and the resulting `LmdbNodeDomainIntersection` exposes a partitioned cursor rather than materializing all common values. The build itself scans/merges each relevant root domain twice and requests a modeled shared-memory charge before allocating; a refusal yields the caller's ordinary physical fallback. The synopsis is attached to a source generation, so it must not be retained across generation/source replacement.

## Join order and cost planning

`LmdbEvaluationStatistics` implements the LMDB-facing `JoinOrderPlanner` and `JoinFactorCostModel`. When ready, it asks the sketch estimator to plan a safe region using either dynamic programming or its bounded greedy/beam alternative, then enriches each plan step with LMDB access-path details. A step cost distinguishes planned output rows from index-access work rows before filters. Metrics record the selected index path, lookup components, access rows, candidate count, and row estimates where available. A direct lookup's repeated nested-invocation work is priced using the smaller of outer rows and distinct lookup bindings; it is not assumed to issue one independent full scan per outer row.

For at least seven join/filter actions when the caller requests dynamic programming, `LmdbEvaluationStatistics` first tries GREEDY. It accepts that bounded attempt only when either estimated work is at most 10,000 rows and final output at most 1,024 rows within the uncertainty cap, or it is a finite direct-lookup plan with at least two direct lookups, at most 50,000 work rows, at most 4,096 final/prefix rows, and bounded uncertainty. Otherwise it runs the requested dynamic-programming attempt. The selected and rejected path/estimates are recorded in optimizer decision telemetry. These thresholds bound planning work and selectivity risk; they do not claim an evaluation runtime.

The fallback cost model combines current bound variables, access-prefix length from available index access paths, estimated rows before/after a local filter, repeated invocation count, and available filter-cost constraints. For a join with multiple flattenable factors and a ready estimator, it can call the join-order planner; otherwise it uses per-factor output and physical access cost. Missing access-shape information keeps the output estimate and charges it as work. A cost-model result changes order only within the compiler's legal reorder region; it does not authorize crossing `OPTIONAL`, `MINUS`, scope-changing subqueries, effectful expressions, or hidden dependencies.

### Semantic rewrites before/around enumeration

The LMDB optimizer includes guarded rewrites for null-rejecting filters over OPTIONAL, overlapping/mutually exclusive optional shapes, no-new-binding OPTIONAL/EXISTS probes, vacuous/redundant pattern MINUS forms, and safe correlated MINUS-to-NOT-EXISTS conversions. It can factor conjunctions, defer filters until their inputs are available, use finite VALUES anchors, and attach correlated condition/subquery invocation estimates to the appropriate scope. It does not treat algebra as freely reorderable because two nodes have the same variable names: assured bindings, shared-bound MINUS semantics, incoming bindings, scope visibility, encounter order, repeatability, and error/effect behavior participate in the guards.

`LmdbFilterSimplifierOptimizer`, `LmdbDeferredFilterPlacer`, `LmdbUnionFilterDistributor`, and `LmdbOrderByOptimizer` are separate planner stages/helpers. Filter simplification uses `EvaluationStatistics`; deferred-filter and UNION distribution are scope-aware; order optimization can replace a selected index-order wrapper only where source order and expressions permit. Consult the named classes before expanding an existing rewrite: the central optimizer is not the only code that creates or moves LMDB query structure.

## Filter-selectivity feedback

The optimizer checks filter evidence in order of specificity: exact filter key, constant-abstracted filter template, then pattern-level evidence (only for unqualified local-bound shape). `LmdbLearnedFilterSelectivity` returns a tier after 16 observations; downstream `FilterPassEstimate` derives a Wilson 95% interval rather than treating a small sample as a precise probability. When no tier is usable, normal heuristic/statistics fallback applies.

When a sketch estimator is present, `LmdbEvaluationStatistics` delegates filter pass estimation to it. In the sketch-free path it reads the in-memory learned filter summary and falls back to inherited evaluation statistics if no estimate exists. When an executed recording filter closes, pass/reject counts are reported to the statistics owner. The learned sketch-free summaries are updated only while `rdf4j.lmdb.learnedFilterSelectivity.enabled` remains enabled; see the config reference for default, parser, and sampling controls.

`LmdbFilterSelectivityStats` also owns bounded persisted observations and sampling. Sampling may scan candidate pattern rows, uses a reservoir (256 rows), and stops at configured row/time budgets; it schedules background raw-sample work through a bounded set of requests. A zero-hit sampled filter is not treated as evidence of impossibility before its minimum evidence threshold. Mutations invalidate or clear relevant filter summaries. This sampling can do work during optimizer planning, so its row/time configuration and background behavior should be considered when diagnosing planning latency; no latency measurements are made in this guide.

## Physical consumer: source shape, not just cardinality

The query planner and executor consume store-backed data through `NativeLmdbQuerySource`. A normal planner may use a pattern cardinality; native physical dispatch may separately choose a prefix run, type matrix, node-domain route, adjacency candidate, or ordinary source/index cursor. A plan estimate therefore describes its planning model, not a guarantee that a particular physical operator opened. [`LmdbStatementAccessArbiter`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStatementAccessArbiter.java) and query source adapters arbitrate the available path at operator open. Storage describes the data-generation and asynchronous publication contract; see [adjacency lifecycle](storage-adjacency-lifecycle.md) and [overview](storage-overview.md).

This code does not currently expose a query-side `FrontierStatistics` class or a distinct Frontier planner subsystem. If a guide, issue, or telemetry note uses that older term, trace it to the present `LmdbEvaluationStatistics`, fan-out, node-domain, and sketch-estimator consumers rather than assuming an active Frontier API.

## Illustrative optimizer walkthrough (source-only)

For a query such as:

```sparql
SELECT ?person ?email
WHERE {
  ?person <urn:works-for> <urn:team> .
  ?person <urn:email> ?email .
  FILTER(STRSTARTS(?email, "ops-"))
  ?person <urn:active> true
}
```

The planner can compare initial pattern access and whether the email filter is ready at each legal join step. Page/index cardinality and ready sketch/access information can distinguish a broad scan from a bound direct lookup; observed filter pass ratios can refine post-filter rows. A filter or join-order rewrite must still preserve which bindings exist when evaluating its expression. If estimator state is not ready, optimizer selection uses the standard pipeline/fallback instead of blocking. This source-only walkthrough illustrates estimate inputs and semantic constraints; it is not a captured plan.

## Extension and source test map

New estimates should define whether they are exact or sampled, their update/invalidation boundary, memory bound, unknown sentinel, and consumer scope. New optimizer transforms need positive and negative semantic guards around OPTIONAL/MINUS/EXISTS/LATERAL/VALUES, prebound inputs, nested groups, and effectful/erroring expressions. Add source tests for both a selected rewrite and a declined-near-miss, then test its interaction with the relevant pipeline and execution explanation.

Representative current contracts: [`LmdbOptimizerPipelineTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOptimizerPipelineTest.java), [`LmdbSketchJoinOptimizerTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizerTest.java), [`LmdbSketchJoinOptimizerAnchorTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizerAnchorTest.java), [`LmdbSketchJoinOptimizerOptionalRewriteTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizerOptionalRewriteTest.java), [`LmdbSketchJoinOptimizerOptionalOverlapRewriteTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizerOptionalOverlapRewriteTest.java), [`LmdbEvaluationStatisticsMemoizationTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatisticsMemoizationTest.java), [`LmdbLearnedFilterSelectivityTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbLearnedFilterSelectivityTest.java), [`LmdbFanOutStatsTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbFanOutStatsTest.java), [`LmdbNodeDomainSynopsisTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNodeDomainSynopsisTest.java), and [`LmdbNativeFanOutEstimateTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFanOutEstimateTest.java). These links identify source contracts, not current pass evidence.
