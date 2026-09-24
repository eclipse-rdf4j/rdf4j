# LMDB joins and factorized execution

This guide covers the bag semantics and physical joins shared by the row and aggregation planners, then describes the separate factorized tail and packed f-tree paths. A compact factor relation preserves exact multiplicity; it does not mean that SPARQL duplicate rows may be dropped. The branch delta catalogued as Q3–Q4 includes the LMDB join, factorized-row, and packed-tree paths; the guide also explains unchanged bag and scope contracts because every physical path must preserve them.

## Join planning starts with algebra boundaries

The native planner can reorder eligible basic graph pattern (BGP) inner joins, but the plan is not a flat bag of triple patterns. `LmdbNativeAggregatePlannerBase` and the join-plan helpers preserve scope, binding dependencies, filter depth, and encounter-order requirements while constructing a `SlotPlan`. The emitted order is only a candidate order: each cursor and strategy still checks its concrete input bindings and source capabilities at open time.

Several constructs remain semantic boundaries:

| Algebra | Boundary behavior |
|---|---|
| Inner `Join` / BGP | A multi-join plan may enumerate/reorder eligible children while respecting dependency and filter-placement checks. |
| `LeftJoin` / `OPTIONAL` | The left input is preserved when the right side has no compatible result. Lexical-scope cases use dedicated frame-aware cursors; they cannot be rewritten as ordinary correlated joins. |
| `Union` | Branch solution bags are concatenated. Branch multiplicities add; they must not be multiplied as if branches were independent factors. |
| `Difference` / `MINUS` | Shared-bound compatibility and non-empty-domain rules are handled by the native difference/mark/membership paths; it is not a plain anti-join on every variable name. |
| `Extension` / `BIND` | A computed assignment can share a factor only when its read mask, target compatibility, repeatability, and encounter-order constraints make this safe. Otherwise evaluation crosses back to logical rows. |
| `Filter` | A deterministic known dependency mask can restrict factor selections before expansion. Opaque/effectful predicates are evaluated in logical encounter order. |
| `Lateral`, SERVICE, mapping-parameterized tuple operators | Their invocation frame is semantically visible; replay and reordering are restricted to proven-safe cases. |

`LmdbNativeFactorAlgebra` composes `UnionPlan`, `LeftJoinPlan`, `ExtensionPlan`, `FilterPlan`, `MinusPlan`, ordinary joins, and multi-join regions. OPTIONAL and UNION are bag-algebra boundaries, not edges in an f-tree. If a packed physical leaf cannot prove its shape before advancing, that leaf declines locally and its ordinary native cursor remains available.

## Join families and their roles

The row arbiter offers a collection of physical choices when its structural dispatcher is reached. Important current families include:

- **Nested/index-loop plans.** The general native fallback opens child operators in the current plan order and carries outer bindings into dependent children. It handles shapes that do not meet a specialized admission rule, subject to the child plan itself being native-capable.
- **Hash joins.** Build/probe plans are admitted only under the hash join's enabled, build-row, and byte-admission checks. The hash build retains keys and row/payload state; `maxBuildRows`, `minRows`, and the optional byte admission are separate from query-memory admission. If a build cannot bind/admit, the proposal declines before publication and the arbiter re-ranks other candidates.
- **Merge joins.** These require compatible ordered input runs and bounded run admission. Run rows are retained while matching keys are merged; `minRows` and `maxRunRows` affect admission/capacity. If input order or capacity cannot be established, it declines to another candidate.
- **Worst-case-optimal joins (WCOJ).** The leapfrog join intersects sorted domains for shared variables rather than enumerating the full pairwise product. It is offered only when `LmdbNativeLeapfrogJoin.canOpen` says the current plan/bindings have the required access and variable structure. Direct-frontier/streaming-frontier flags tune concrete variants; they do not make unsupported shapes eligible.
- **Semi-join reduction / SIP.** Known tuple filters, membership structures, adjacency-backed masks, and source-domain intersections can reduce values passed into later joins. Their purpose is to prune candidate domains before expanding tuples; eligibility depends on the physical source route and read/write masks.
- **Mark, left-join payload and replay plans.** These preserve the distinctions between a matched OPTIONAL payload, a failed match, and a membership verdict. Some are selected as direct join plans rather than independent members of the main row proposal ladder.
- **Wildcard batches and prefix runs.** The batch route groups probes and can exploit wildcard-predicate or adjacency data; the prefix-run route advances one distinct key/run at the chosen index level. Its usefulness depends on the selected key order and run metadata.

The full strategy ladder, family-specific constraints, and force semantics are in [query strategy families](query-strategy-families.md). Structural direct routes such as a fully bound BGP or boolean EXISTS are not ordinary candidate-ladder rungs: they avoid reopening an arbiter for every outer binding.

## Factorized row tail

`LmdbNativeFactorizedRows` handles a restricted trailing join suffix in a non-aggregate row plan. It identifies a flat prefix followed by one or more plain pattern branches whose fresh variables are not consumed by later probes or filters. The prefix may be a more general slot plan, but any pattern whose fresh variables are needed later stays in the prefix.

For each distinct prefix probe key, tail branches are probed once. A tail branch can have one of three roles:

| Role | Retained representation | Output rule |
|---|---|---|
| `ENUM` | Projected fresh-variable value batch | A row odometer combines enumerated branch values at emission. |
| `COUNT` | Exact per-key match count | Plain SELECT emits the projected row with that multiplicity; DISTINCT can reduce it to existence. |
| `EXISTS` | Existence verdict | DISTINCT needs only a first-match semi-join probe. |

This avoids repeating independent tail probes for every flat-prefix row and delays the product expansion until an output consumer needs those values. It still preserves bag multiplicity for ordinary SELECT. The per-iteration `BATCH_ROWS` is 1,024; memo entry and value limits are respectively 65,536 and 1,048,576, with bypass accounting when an entry would exceed the caps. Those caps bound this memo's entries/values; they are not a global byte reservation and do not cap every other query-owned structure.

The related switches are `rdf4j.lmdb.factorizedRows.enabled`, `.chunkedPrefix.enabled`, and `.keysOnlyRoot.enabled`; all three are enabled unless the property is exactly lowercase `false`. The `.orderedFactorizedRows.enabled` gate controls the separate ORDER BY factorized producer. See [query configuration](query-configuration.md) for all defaults and read scopes.

## Packed factorized trees

`LmdbNativePackedFtree` is a physical representation for supported fixed-predicate BGP regions, with composable bag algebra supplied by `LmdbNativeFactorAlgebra`. It is not a general replacement for the row engine. Its planner admits connected subject/object structures with constant endpoint restrictions, graph restrictions, duplicate statement multiplicity, and cyclic constraints. Tree edges expand child vectors; additional relations to an already-bound ancestor are enforced by ordered multi-way intersections. Variable-predicate shapes and other unsupported forms stay on general paths.

The representation keeps large relations factorized. An unrestricted terminal may retain a snapshot-owned borrowed group and exact summary per parent. A materialized variable uses a packed value vector. Materialized fanout children use offsets that map each parent to a contiguous child slice; non-expanding children can share the parent's selector. Independent branches remain separate, so their Cartesian product is implied until a flat row consumer asks for it. Aggregate reductions propagate by deltas through the AGG/SCATTER cascade before dependent states consume them.

Borrowed factor rows carry relation descriptors and multiplicities, not necessarily copied scalar bindings. Tuple columns stay zipped, never become independent factors. A copied `long[]` scratch window is temporary; source-backed payload and leases remain owned by the immutable query snapshot. Readers must close before the lease/source closes. This shared snapshot and memory accounting contract is defined in [storage adjacency lifecycle](storage-adjacency-lifecycle.md); the query-side `FactorEnvironment` and product cursor track selected descriptors and logical multiplicity.

Current planner and dispatch constraints:

- Automatic row f-tree proposals are made for a direct non-empty `MultiJoinPlan`. A composed factor tree can be proposed when `packedFtree` is explicitly forced. Ordinary candidate selection does not price arbitrary composed factor algebra as though it were a direct BGP.
- For grouped aggregation, automatic packed-tree pricing likewise applies to a direct multi-join. A forced `packedFtreeAggregate` can reach composable factor algebra where its structural candidate check succeeds.
- The planner may decline a shape, correlated entry mask, unavailable adjacency leaf, oversized physical slot schema, or resource admission before publishing results. It does not switch to an alternative after exposing output.
- `rdf4j.lmdb.packedFtree.enabled`, `structuredGroups.enabled`, `bulkRuns.enabled`, and `parallel.enabled` default on unless set to exact lowercase `false`. The row/vector settings and bounds are documented in [query configuration](query-configuration.md).
- `rdf4j.lmdb.factor.borrowed.enabled` and `rdf4j.lmdb.packedFtree.algebra.enabled` also default on unless lowercase `false`; composable factor algebra additionally requires packed f-tree and factor transport to remain enabled.

The packed vector size defaults to 2,048 rows, is clamped to 64–65,536, and is rounded down to the nearest power of two. Root batches default to 1,024 but are limited to vector size and floored at 64. Initial node capacity defaults to vector size, with the same 64 floor and vector ceiling. Exact variable ordering begins at three variables and defaults to a threshold of nine; the beam defaults to 128 and has a minimum of eight. Bulk run copies use a fixed 4,096-lane window. These are row/lane counts, not byte ceilings.

## Worked examples (not executed)

**Independent fanouts.** For `?person ex:knows ?friend . ?person ex:tag ?tag`, once `?person` is fixed, neither tail variable is needed to probe the other branch. A factorized tail can count or retain each branch's values per key. `SELECT ?person` can preserve the product multiplicity without building every pair; `SELECT DISTINCT ?person` needs only that each branch be nonempty. `SELECT ?friend ?tag` must enumerate the cross product when it returns bindings.

**Cyclic constraints.** For `?a ex:edge ?b . ?b ex:edge ?c . ?a ex:edge ?c`, the third triple is a constraint against an already-bound ancestor pair. The f-tree cannot treat it as an independent child product; the planner must enforce the cyclic relation with intersection logic or decline to another join path.

**OPTIONAL and UNION.** In `{ ?s ex:p ?x OPTIONAL { ?s ex:q ?y } } UNION { ?s ex:r ?z }`, missing right-side OPTIONAL results contribute the left mapping with the right names unbound. The UNION adds branch rows, including duplicates. A future factor operator must preserve those semantics at the boundary rather than multiplying `?x`, `?y`, and `?z` into one global product.

## Extension and tests

When adding a factor-capable operator, define its exact produced mask and read mask, state whether the current operation preserves encounter order and expression/error effects, and specify its empty relation and multiplicity rules. Keep source-backed leaves borrowed only while their owner lease is live. If a shape cannot be proved at open, return a local decline before that leaf advances. Test both scalar rows and weighted rows, duplicates, nullable OPTIONAL bindings, nested UNION, cyclic filters, correlated entry bindings, cancellation, and source close ordering.

Relevant source tests include [`LmdbNativeFactorizedCostTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedCostTest.java), [`LmdbFactorizedBatchTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbFactorizedBatchTest.java), [`LmdbNativeFactorizedReorderTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedReorderTest.java), [`LmdbNativeFactorizedSinkTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedSinkTest.java), [`LmdbNativePackedFtreeCapacityTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtreeCapacityTest.java), [`LmdbNativePackedFtreeWitnessTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtreeWitnessTest.java), [`LmdbNativePackedFtreeParallelFailureTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtreeParallelFailureTest.java), and [`LmdbNativeInteriorIslandTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeInteriorIslandTest.java). They are source references only; no test or benchmark was run for this guide.
