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

The row-tail switches are `rdf4j.lmdb.factorizedRows.enabled`,
`.chunkedPrefix.enabled`, and `.keysOnlyRoot.enabled`; each is enabled unless
its property is exactly lowercase `false`. For an eligible factorized-row
plan, `rdf4j.lmdb.chunkPipeline.enabled` gates the internal chunk-pipeline
prefix. In the normal ordered-root pipeline, `.merge.enabled` and `.sip.enabled`
separately enable merge walks and sideways-information-passing optimizations;
neither is a prerequisite for that prefix. `.externalRoot.experimental` gates
the worker-supplied root variant, which omits merge walks and SIP because
workers do not share one globally ordered root stream. The
`.orderedFactorizedRows.enabled` gate controls the separate ORDER BY
factorized producer. See [query configuration](query-configuration.md) for all
defaults and read scopes.

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

## Worked joins over the same IDs

Use the [five-quad dataset and symbolic IDs](query-physical-access.md#a-dataset-and-the-ids-used-below): `a/b/c` are Alice/Bob/Carol, `k/l` are knows/label, `B/C` are the label literals, and `t` is the team graph. The active dataset restricts ordinary patterns to context `0` and exposes `t` as a named graph. Each example below explains an execution that is valid **if that operator is admitted and opened**. These tiny relations do not predict cost estimates, automatic admission, or the selected optimizer plan; in particular, merge/hash minimum-row gates can reject them.

### A chain passes the first lookup's IDs into the second

```sparql
PREFIX : <urn:example:>
SELECT ?friend ?label WHERE {
  :alice :knows ?friend .
  ?friend :label ?label .
  FILTER(?label = "Bob")
}
```

For a nested-loop order that starts with `knows`, the first pattern requests `(a,k,*,0)`. Outgoing adjacency returns two accepted quads, or `spoc` returns the same quads after context filtering. [`JoinCursor.next()` and `openRight()`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeJoinPlans.java) keep the left row installed while opening the right pattern:

| Current outer slots | Inner request | Inner binding before the filter | Filter/result |
|---|---|---|---|
| `friend=b, label=*` | `(b,l,*,0)` | `friend=b, label=B` | `"Bob" = "Bob"`; emit Bob / `"Bob"`. |
| `friend=c, label=*` | `(c,l,*,0)` | `friend=c, label=C` | `"Carol" = "Bob"` is false; emit nothing. |

The right probe is created once and reopened for `b` and then `c`; it can reuse the predicate ordinal, page lookup cursor, and run iterator for adjacency, or reposition a retained LMDB iterator. There is no dictionary lookup of `:bob` between these two patterns: the ID already occupies `friend`. Closing/advancing the right cursor rolls back its new slots; advancing the left cursor rolls back `friend`. Compatibility checks at `PatternPlan.bind()` reject conflicts or repeated-variable mismatches before a solution reaches the filter.

The `FILTER` might instead be placed at an earlier legal depth or absorbed into a safe range/constant restriction; that changes candidate work, not the required result. Literal `=` uses the native expression's SPARQL value semantics, not an unconditional comparison of raw literal IDs. If an expression needs an RDF value, the source resolves it through `lazyValue()`; [value resolution](storage-value-overlay.md) is separate from the statement lookup. If the `label` predicate is outside selected adjacency coverage, the first pattern can use adjacency and the second can use LMDB. A join does not require every child to use the same physical representation.

For `GRAPH :team { :alice :knows ?friend }` followed by the default-context label pattern, the first request is `(a,k,*,t)` and supplies only `friend=b`; the next request is still `(b,l,*,0)`. The graph of the outer quad does not automatically become the inner pattern's graph.

### A star keeps independent fanouts independent

```sparql
PREFIX : <urn:example:>
SELECT ?person ?left ?right WHERE {
  ?person :knows ?left .
  ?person :knows ?right .
}
```

After root `person=a` is fixed, each branch requests `(a,k,*,0)` and produces `[b,c]`. Their fresh slots differ. An ordinary nested loop emits `(a,b,b)`, `(a,b,c)`, `(a,c,b)`, `(a,c,c)`: the shared subject establishes compatibility, and the two independent object choices multiply. It must not intersect the object lists, zip `b` with `b` and `c` with `c`, or deduplicate the product to two rows.

For an eligible trailing suffix, [`LmdbNativeFactorizedRows.tryCreate()` and `TailBranch.result()`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedRows.java) retain/probe branch results by the prefix key rather than repeat them for every expanded row. In this standalone star, the first pattern introduces `person`, which the second probe needs, so it remains in the prefix. The two prefix rows `(a,b)` and `(a,c)` each combine with the tail's `ENUM` result `[b,c]`, yielding the four pairs above. With `SELECT ?person`, the tail can instead retain `COUNT=2` and give each of the two prefix rows weight `2`; Alice still appears four times. With `SELECT DISTINCT ?person`, the tail needs only `EXISTS`, and the final distinct result contains Alice once. If a later pattern or filter consumes `left` or `right`, the planner must retain the required scalar values or keep that branch in the flat prefix; it cannot replace it by a count.

The row-tail and packed-tree planners have different admission rules. A connected fixed-predicate region admitted by [`LmdbNativePackedFtree`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree.java) can retain an `a` root lane and separate child slices for the two fanouts, with their `2 × 2` product implicit. `Runtime.openRootProducer()` first tries equivalent complete adjacency seeds, then its implemented LMDB `ScanRootProducer`. `EdgeRuntime` can likewise use a run or its statement-probe path. Missing adjacency therefore does not invariably discard a packed tree. Unsupported algebra, masks, filters, or resource admission can still decline the candidate before output.

### Shared variables are intersected; EXISTS asks for a verdict

```sparql
PREFIX : <urn:example:>
SELECT ?friend WHERE {
  :alice :knows ?friend .
  ?friend :label "Bob" .
}
```

Here both patterns constrain **the same** fresh `friend` slot. The outgoing `knows` run at `a` allows `[b,c]`; the incoming `label` run at object `B` allows `[b]`. Their context-0 intersection is `[b]`. A general nested loop obtains that answer with `(b,l,B,0)` and `(c,l,B,0)` existence/probe requests. In an admitted packed tree, unary/ancestor constraints can check the same condition while producing a child. Its `Runtime.intersectRuns()` intersects ordered primary/constraint runs, advances lagging values to a common ID, and multiplies each surviving value's incidence counts. A constraint with no match contributes zero, not a new independent fanout. If a constraint cannot supply an ordered run, `scanPrimaryRun()` performs its implemented checks instead.

The related semijoin is:

```sparql
PREFIX : <urn:example:>
SELECT ?friend WHERE {
  :alice :knows ?friend .
  FILTER EXISTS { ?friend :label ?label }
}
```

With `friend=b`, the inner pattern asks whether `(b,l,*,0)` has any accepted quad; with `friend=c`, it asks `(c,l,*,0)`. Both survive once per outer row. EXISTS does not expose `label` or multiply the outer row by the number of label witnesses. NOT EXISTS reverses the verdict while preserving outer multiplicity. MINUS additionally has its shared-bound-domain rules, so it cannot indiscriminately reuse every surrounding physical slot.

There is a concrete context restriction on the direct semijoin shortcut: [`AdjacencyIntersectionProbe.tryCreate()`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeMembership.java) rejects fixed contexts, named-graph scope, and a constant context term. Thus **these context-0 examples use the exact pattern/membership fallback**, even if its statement probe itself uses adjacency. For an unrestricted-context single fixed-predicate pattern, the shortcut can use a complete outgoing/incoming key-domain presence test, or a lower-bound neighbor test for two bound endpoints. It returns `NOT_APPLICABLE` if it cannot prove an answer, so the caller retains its normal existence path. A domain is useful for rejection only within the scope and completeness it certifies.

For cycles such as `?a :edge ?b . ?b :edge ?c . ?a :edge ?c`, the final edge constrains already-related variables. [`LmdbNativeLeapfrogJoin`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeLeapfrogJoin.java) recognizes an eligible cyclic sub-bag, enumerates one variable at a time, and intersects each pattern's allowed ID frontier under the current prefix. Direct `adjacencyFrontier()` additionally requires a known predicate and compatible subject/object position, and declines fixed/named/bound contexts, repeated slots, physical ranges, and preordered patterns. The record-iterator frontier path scans, sorts, and deduplicates values with incidence counts instead. The direct-frontier switch does not make the fixed-context example eligible for that shortcut, or make every star a leapfrog plan.

### Ordered merge and hash over these relations

For the chain without its filter, the two input relations are:

```text
knows in context 0:  (friend=b), (friend=c)
label in context 0:  (friend=b,label=B), (friend=c,label=C)
```

A merge proposal must obtain a common ordered key sequence for `friend`. [`LmdbNativeMergeJoin.tryPlan()`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeMergeJoin.java) constructs ordered sides only after its two-pattern, shared-key, context, correlation, estimate, and configured source-order checks. For this shape, an incoming `knows` root scan ordered by object can provide the left key; an outgoing `label` root scan ordered by subject provides the right key. Under the default durable indexes, `posc` orders the fixed-`k` input by object, but `spoc` can require a wider subject-ordered scan with residual `l` filtering on the right. Complete adjacency has its corresponding ordered root routes. The planner may cost those scans differently or reject merge.

If opened, the merge cursor compares the ID keys in unsigned order, advances/seeks the lagging side, and collects equal-key runs. Bob's one left row and one right row emit `(b,B)`; Carol's emit `(c,C)`. If a key has `m` accepted rows on one side and `n` on the other, the equal-key run emits `m × n` compatible pairs. Source ID order is not automatically the RDF term order required by arbitrary SPARQL ORDER BY expressions. Run capacity refusal uses the operator's local fallback/rescan logic; an ordered plan's fallback must preserve its promised key order.

A hash proposal does not need ordered scans. [`LmdbNativeHashJoin.tryPlan()` and `HashJoinBatchCursor`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeHashJoin.java) choose a build side from estimates after two-pattern, key-width, correlation, context, cost, and size checks. Suppose the `label` side is built: its ID table retains `b → [B]` and `c → [C]` as keys plus payload rows. Scanning `knows` probes key `b`, emits `(b,B)`, then probes `c`, emits `(c,C)`. Extra payloads under one key stay in that bucket's row chain; duplicate probe rows each produce the matching payloads, preserving bag multiplicity.

Hash build-row limits, optional byte admission, and actual build success still apply after proposal. A refusal before published output leaves an implemented nested-loop alternative available. Grouped factor output can transport a retained **tuple** payload relation plus multiplicity; it must keep multiple payload columns zipped as rows, not turn their columns into independent fanouts. A query-memory or source failure after rows have been published propagates with cleanup; it cannot restart the whole query through another join strategy.

### Algebra boundaries keep their own multiplicity

In `{ ?s :knows ?x OPTIONAL { ?s :label ?y } } UNION { ?s :label ?z }`, an unmatched OPTIONAL preserves its left mapping with `y` unbound; UNION adds the branch bags. Those are separate operations from the star's product or the shared-slot intersection. Reusing a borrowed CSF run, memo, or hash table cannot cross a binding/scope boundary without the corresponding planner proof. Adjacency statement-delta overlays only choose the correct visible rows; the [dictionary value overlay](storage-value-overlay.md) only accelerates term/ID resolution. Neither overlay changes the join algebra.

## Extension and tests

When adding a factor-capable operator, define its exact produced mask and read mask, state whether the current operation preserves encounter order and expression/error effects, and specify its empty relation and multiplicity rules. Keep source-backed leaves borrowed only while their owner lease is live. If a shape cannot be proved at open, return a local decline before that leaf advances. Test both scalar rows and weighted rows, duplicates, nullable OPTIONAL bindings, nested UNION, cyclic filters, correlated entry bindings, cancellation, and source close ordering.

Relevant source tests include [`LmdbNativeFactorizedCostTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedCostTest.java), [`LmdbFactorizedBatchTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbFactorizedBatchTest.java), [`LmdbNativeFactorizedReorderTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedReorderTest.java), [`LmdbNativeFactorizedSinkTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedSinkTest.java), [`LmdbNativePackedFtreeCapacityTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtreeCapacityTest.java), [`LmdbNativePackedFtreeWitnessTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtreeWitnessTest.java), [`LmdbNativePackedFtreeParallelFailureTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtreeParallelFailureTest.java), and [`LmdbNativeInteriorIslandTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeInteriorIslandTest.java). These links identify source contracts, not current pass evidence.
