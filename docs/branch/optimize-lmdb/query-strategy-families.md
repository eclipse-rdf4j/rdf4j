# LMDB native strategy families

This guide is a source-level map of the candidate families reached by native row, ordering, and group dispatch. It separates structural routes from competing proposals, and compile capability from successful binding/open. Strategy names in explain output are useful evidence about one execution; they do not imply that every algebra shape can use that family. The branch delta catalogued as Q3–Q4 adds and changes the LMDB physical candidates described here; RDF4J algebra semantics and the generic evaluator remain correctness baselines, not features this guide attributes to the branch.

## Dispatch points

There are three user-visible arbitration locations: unordered row/join dispatch, ORDER BY fusion, and GROUP BY/aggregate dispatch. The row and group dispatchers build candidate proposals lazily from the current `RowState`, source, and compiled slot plan. A proposer can decline structurally without entering the adaptive candidate set. An offered proposal can then decline at open, for example when source bindings or resource admission fail. The arbiter re-ranks remaining candidates after a pre-output decline.

An authoritative type-matrix plan can be the terminal aggregate fallback for its shape. The generic nested-loop aggregate is not offered when the type matrix owns that plan: the type matrix is the exact structural terminal and retries only through its captured candidate path. A forced name bypasses cost ranking and must bind at the authoritative dispatch point; a forced candidate that is unavailable reports a query evaluation error rather than silently running a different family.

## Unordered row and join candidates

`NativeRowsStep.openUnorderedInput` (implemented in [`LmdbNativeRowStep`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java)) offers eligible candidates in this source order. This is candidate-construction order, not always the winner: the arbiter uses strict cost dominance when cost intervals separate, then [`LmdbNativeStrategyPreference`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeStrategyPreference.java) for overlapping/unknown costs.

| Family | What it changes | Main eligibility/admission boundary |
|---|---|---|
| `prefixRun` | Iterates index runs by leading distinct key rather than emitting every matching row. | Offered only for unordered output; a compatible prefix-run plan must handle the current row. |
| `orderedDistinct` | Uses a natural index order to deduplicate without a general hash set. | Requires an admitted order-aware distinct plan and no ORDER BY input requirement. |
| `wcoj` | Leapfrog multi-way domain intersection for a supported join. | `LmdbNativeLeapfrogJoin.canOpen` must establish supported access and shared-variable structure. |
| `packedFtree` | Packed factorized row cursor for an eligible BGP or composed factor shape. | Automatically priced for direct `MultiJoinPlan`; composed plans only enter on an explicit force. See [joins and factors](query-joins-and-factors.md#packed-factorized-trees). |
| `factorizedRows` | Enumerates a flat prefix and keeps independent trailing branches as `ENUM`/`COUNT`/`EXISTS` results. | Requires a supported tail split and non-correlated/factorized admission for this entry. |
| `batch` | Builds one batched plan over patterns; can include the wildcard-predicate batch and internal hash/merge join subchoices. | Batch enabled, supported batch shape, and successful capacity/admission checks. Hash and merge subchoices are not independently forceable top-level strategies. |
| `parallelPipelines` | Partitions compatible source work into worker pipelines with a shared query row contract. | Parallel switch, source fork/range support, useful estimate, and worker/task/memory admission. |
| `irKernelParallel` / interpreted parallel | Runs the lowered kernel in morsels in compiled or interpreted form. | Lowering and parallel proposal admission must both succeed. |
| `irKernel` / interpreted | Runs a serial lowered row kernel. | The row plan must lower; Janino and interpreter are independent availability gates. |
| `irKernelDistinct*` | Integrates DISTINCT with the row cursor, including parallel and interpreted variants. | Offered only for unordered DISTINCT and a compatible sinkable projection; its distinct sink replaces the external de-duplicator. |
| `adaptiveFilterPlacement` | Re-places a supported filter in the eligible join order based on runtime observations. | `canAttempt` must admit the plan and filter dependencies. This is still a pre-output candidate. |
| `nestedLoop` | General native row fallback over the compiled slot plan. | Always offered at this dispatch unless the surrounding path takes an earlier structural terminal. |

Direct shortcuts bypass this ladder: `LIMIT 0`, provably empty input seed, constant-false entry filter, a bound BGP cursor, and direct EXISTS. They are recorded in execution-path vocabulary but are not all forceable strategy candidates. The bare-fragment direct scan is omitted from the preference ladder because it may be reopened per correlated outer row; repeated arbitration at that point would multiply overhead.

## ORDER BY candidates and fallback

The ordered dispatcher offers a factorized sorter and, when a codegen or interpreter tier is available, a lowered IR sort candidate. The factorized sorter can carry a product compactly through sort preparation; when there is a finite `LIMIT`, the top-K form retains only the candidate prefix needed for `LIMIT + OFFSET` if capacity arithmetic and DISTINCT-order safety are proven. Otherwise it uses a full sort path. The ordered input is blocking: no binding is returned until the selected sort preparation finishes.

If the fusion arbiter declines, the caller uses the ordinary native unordered input followed by its exact sort/materialization path. If a comparator cannot be lowered to native order slots, root pipeline compilation declines earlier; the compiler's explicit index-order marker raises its own evaluation exception. These conditions should not be conflated with a selected candidate opening and later failing.

## GROUP BY and aggregate candidates

`NativeGroupStep` has a wider, shape-dependent proposal set than row dispatch:

| Family | Work avoided/changed | Scope notes |
|---|---|---|
| `adjacencyAggregate` | Answers qualifying aggregate structure directly from in-memory adjacency. | It can be a whole-query structural answer and may bypass ordinary scan-and-group work. It needs compatible adjacency source and grouping shape. |
| `typeMatrix` | Uses a type-matrix/count index for suitable fixed node/predicate grouping. | When `typeMatrixOwned` is true, it is the registered terminal; generic nested-loop is not a substitute fallback. |
| `existsIntersection` | Computes qualifying groups by intersecting exact node domains. | Synopsis/domain preparation and grouping predicates must match the recognized structure. |
| `prefixRunGroups` | Enumerates grouped values by leading-key run. | Requires a compatible prefix run and filter/count shape. |
| `orderedDistinctGroups` | Streams grouped distinct values in source order. | This specialist may suppress an ordinary IR aggregate proposal to prevent duplicate work; that is a selection policy, not proof that IR lowering is impossible. |
| `wildcardPredicateReduced` | Uses weighted count/reduction over a wildcard-predicate physical demand. | Preserves multiplicity with counts rather than materializing each tuple. |
| `wcoj` | Uses leapfrog to enumerate supported multi-pattern combinations before aggregation. | Structural WCOJ admission still applies. |
| `packedFtreeAggregate` | Runs packed f-tree aggregation over supported direct BGP or explicitly forced composable factor plan. | Auto proposal/pricing is for a direct `MultiJoinPlan`; a composed factor algebra candidate may be opened when explicitly forced. |
| `factorizedTail` | Aggregates over a derived factorized join order without flattening all rows. | Requires a factorized derivation that carries requested aggregate demand. |
| `parallelAggregation` | Partitions admitted aggregate work over workers. | Worker submissions and result merge are resource-scoped; aggregate state must be mergeable under its exact value/count semantics. |
| `janinoAggregate` | Legacy whole-stage generated aggregate path. | It is a distinct codegen family from the current IR aggregate, and proposal shape must satisfy its compiler. |
| `irAggregate*` | Serial/parallel compiled or interpreted lowered aggregate; route variants include wildcard, node-domain intersection, and type matrix. | Each serial/parallel lowering route is admitted independently. Unsupported expression or kernel plan declines locally. A recognized algorithmic specialist may suppress normal IR offers; forcing the IR family lets its concrete lowering/bind decline be reported. |
| ordered single-pattern / sequential | Index-ordered one-pattern aggregate or native sequential aggregation. | Sequential aggregation is the general route when the type matrix does not own the shape. |

Parallel aggregation and parallel IR are separate candidates; one is not an alias for the other. A specialist's exact aggregate summary can answer the full query without row enumeration. When a selected aggregate path pre-materializes `List<BindingSet>`, retained output storage scales with emitted groups; streaming kernel/iterator alternatives have different buffer scopes, described in [memory and result lifecycle](query-memory-and-result-lifecycle.md).

## Strategy names: forceable, structural, or currently unreachable

[`LmdbNativeForceableStrategies`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeForceableStrategies.java) is the curated legal force list for row, ORDER BY, and group dispatch. Examples of forceable families include `nestedLoop`, `wcoj`, `packedFtree`, `factorizedRows`, `batch`, `parallelPipelines`, `irKernel`, `irAggregate`, `packedFtreeAggregate`, and `adjacencyAggregate`. It accepts no-force spellings `null`, blank, and `NOT_ACTIVATED`.

The execution vocabulary contains more labels than the forceable catalog. `limitZero`, `bareDirect`, `bareExists`, `orderedTopK`, `orderedFullSort`, `typeMatrix`, and group-table representation labels are structural/internal routes rather than candidates at every dispatch point. `hashJoin` and `mergeJoin` are internal batch subdecisions, not independent top-level candidates. `chunkPipeline` remains in the vocabulary and preference order, but the source catalog states that no current proposal tags it; do not describe that family as currently selected. The generic fallback is a root compiler/hosting decision, not an offered row/group candidate.

The configuration and exact tuning properties belong to [query configuration](query-configuration.md). In particular, `rdf4j.lmdb.chunkPipeline.*` switches and its presence in the preference list alone do not establish that a chunk-pipeline proposal is active. Config gates, structural proposal sites, and the forceable catalog must all agree before documenting a family as reachable.

## Failure, cancellation, and resource lifetime

Candidate `propose()` should be side-effect-light. If proposing reserves unpublished admission and then throws, that proposer owns cleanup; once a proposal is returned, the arbiter owns and releases it. The arbiter opens the currently ranked proposal and closes losers as soon as a winner opens, so losing worker reservations do not remain pinned for the lifetime of a streaming winner. If the opener returns `null` before output, the arbiter removes it and selects another remaining proposal. A registered terminal fallback is outside adaptive trial/probe consideration so it cannot be consumed and then restarted as a second execution.

Probe/hedge paths exist only at dispatch points that supply the required drain/discard or shadow-context support. They are not blanket behavior for all operators. Once output has been published, this query engine cannot safely restart another strategy; runtime failures and cancellation propagate, and owners close their cursors, leases, worker tasks, and reservations. Cancellation is checked at operator-specific poll points, so responsiveness depends on a path reaching those polls. See [arbitration](query-arbitration-and-explanation.md) and [lifecycle](query-memory-and-result-lifecycle.md).

## Source tests and extension map

Source contracts include [`LmdbNativeStrategyArbiterTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeStrategyArbiterTest.java), [`LmdbNativeArbiterInvariantTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeArbiterInvariantTest.java), [`LmdbNativeArbiterConsistencyTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeArbiterConsistencyTest.java), [`LmdbNativeDistinctOrderLimitTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeDistinctOrderLimitTest.java), [`LmdbNativeAdaptiveFilterPlacementTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAdaptiveFilterPlacementTest.java), [`LmdbNativeHashJoinChainStatsTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeHashJoinChainStatsTest.java), [`LmdbNativeLeftJoinCursorTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeLeftJoinCursorTest.java), and [`LmdbNativeIrKernelParallelTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeIrKernelParallelTest.java). No test or benchmark was executed for this documentation task. The test-map guide classifies all query tests and representative source contracts.

When adding a strategy, establish a named candidate at the real dispatch site, state its structural checks and resources, provide a cost estimate or explicitly unknown work, define pre-output decline and post-output failure semantics, add a stable explanation tag, and update the catalog only if forcing is truly legal there. Add a permutation/admission test so declaration order is not accidentally mistaken for cost preference.
