# LMDB query routing and plan hosts

This guide follows one query from the LMDB evaluation-strategy entry point through root compilation, tuple compilation, generic execution, and runtime candidate selection. Its central distinction is that algebra classification, compile capability, strategy eligibility, winner selection, successful open, and row production are separate facts. The branch delta catalogued as Q1–Q2 is the LMDB query host and native routing layer; generic evaluator behavior and shared query context below are compatibility contracts used to explain its boundaries, not claims that those semantics originated in this branch.

## Entry point and native-source requirements

[`LmdbNativeEvaluationStrategy`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategy.java) extracts a native query source from the Sail dataset triple source. If the extracted source is missing or its IDs are not canonical, the strategy declines to use ID-keyed native plans. That restriction matters for old stores whose value dictionary could assign more than one ID to a term; comparing those raw IDs would not preserve the generic evaluator's term equality.

`rdf4j.lmdb.nativeQueryEngine.enabled` defaults to `true` and is read while the strategy is constructed. If disabled, the strategy follows the generic evaluator path. When enabled and a canonical native source exists, a `QueryRoot` goes through a classified root-host decision. `NativeLmdbQuerySource.hasStatementsInSource()` is a proof that a selected dataset branch cannot hold statements, not a count query: a normal empty store still qualifies for native planning, including a `VALUES`-only query. A provably empty branch skips native root proposals but still gets a generic host.

Non-root precompiles are different. The strategy tries a native tuple plan only when the native source may contain statements; when that compile returns no step, it delegates to the generic superclass. This is how the ordinary generic plan can contain LMDB-accelerated fragments without claiming that its whole root is native.

## Root host and root compilation order

[`LmdbNativeAggregateCompiler`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregateCompiler.java) returns a typed `CompileOutcome` for roots: supported plan, capacity decline, or unsupported shape with a reason. A decline is not an evaluation failure. The strategy wraps it in [`GenericRootPlan`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/GenericRootPlan.java), which evaluates the generic root and records a generic-host decision. With native evaluation active, a root therefore has an explicit plan classification even when its root shape is generic.

For a supported root, the compiler tries the current specializations in this order:

1. `FILTER` directly over `GROUP`, then a bare `GROUP`.
2. `DESCRIBE`, aggregate-root, and multi-projection-root compilation.
3. The ordinary row-root compiler first when the wrapper stack is not an aggregate modifier spine.
4. [`NativeRootPipeline`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeRootPipeline.java) for supported `ORDER`, `SLICE`, `DISTINCT`, and `REDUCED` modifier spines; this lets a grouped source reach the aggregate compiler instead of being mistaken for an ordinary row projection.
5. A row-root retry if the pipeline declined.
6. For a root that exceeded the 60 native-slot limit, the bounded internal-variable demotion attempt in [`LmdbNativeWideQueryDemotion`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeWideQueryDemotion.java).
7. A projection-less bare root when `rdf4j.lmdb.bareFragments.enabled` permits it.

The exact order matters: the semantic root pipeline is a completeness floor, not a replacement for row plans that can handle the whole wrapper stack with a more specialized physical strategy. The root pipeline composes modifier stages in algebra order. `REDUCED` is implemented with exact duplicate elimination, which is one valid `REDUCED` result. A sort key that cannot be compiled makes the pipeline decline so later root attempts can proceed. The explicit `LmdbIndexOrderFunction` marker is different: its compiler branch raises a `QueryEvaluationException` rather than silently declining.

## Census is classification, not a coverage promise

[`LmdbNativeAlgebraCensus`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAlgebraCensus.java) explicitly classifies the current tuple-expression types:

| Census disposition | Current classes |
|---|---|
| `NATIVE` | `SingletonSet`, `StatementPattern`, `Join`, `LeftJoin`, `Union`, `Difference`, `Filter`, `Extension`, `BindingSetAssignment`, `ArbitraryLengthPath`, `EmptySet`, `ZeroLengthPath`, `TripleRef`, `ReifiedTripleRef`, `AnnotationTripleRef`, `TupleFunctionCall`, and `Lateral` |
| `ROOT` | `QueryRoot`, `Projection`, `MultiProjection`, `DescribeOperator`, `Distinct`, `Reduced`, `Order`, `Slice`, and `Group` |
| `ISLAND` | `Intersection` |
| `PERMANENT_ISLAND` | `Service` |

This census exists so a new concrete algebra type must receive an explicit disposition. It does not mean every instance compiles, every function has a native implementation, or every plan is eligible for the same physical strategy. For example, tuple functions are native only for the registered implementation and binding shape that the compiler supports; exact-visibility `LATERAL` cases can compile, while cases whose visibility cannot be proved use the generic path. `SERVICE` remains generic because federation is external evaluation. The census class contains historical comments that conflict with its current map for tuple functions and LATERAL; the map and current compiler/runtime branches are the source of truth.

## Generic root hosting and generic islands

There are two distinct generic boundaries:

- **Generic root host.** The root is evaluated by generic RDF4J evaluation. In the default mode its generic preparation still recurses through this native strategy, so supported interior fragments may use native plans. Setting `rdf4j.lmdb.nativeQueryEngine.forceRootGeneric=true` chooses the claim-suppressed strategy view; generic callbacks then cannot re-enter native compilation.
- **Generic island.** A `GenericEvalPlan` is an opaque, position-pinned `SlotPlan` inside a larger native row plan. It streams generic solutions into the native row, binds only declared output names, rejects conflicts using the surrounding join's term-binding rules, and rolls back row state before trying the next solution. The surrounding plan cannot flatten or freely reorder the island, preserving evaluation order and effects.

[`GenericEvalPlan`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/GenericEvalPlan.java) separates the prepared generic step from reusable result buffers. `NativeExecutionContext.genericStep` caches the prepared step by descriptor identity inside one evaluation; it uses that evaluation's shared query context, preserving evaluation-scoped functions such as `NOW()` and blank-node generation. Replay and keyed-memo state have a different scope: an immutable entry-binding snapshot and the active LATERAL occurrence keep separate child frames from sharing one frame's buffered rows. Independent, non-mapping-parameterized island results can be drained once and replayed across opens in that frame; mapping-parameterized paths, SERVICE, LATERAL, TripleRef, and tuple-function evaluations use their defined per-input semantics. For otherwise correlated islands, keyed reuse is legal only when facts establish repeatability, no mapping-parameterized behavior, and no query-fatal error whose raise count could be observable. The memo key projects to input variables the island reads; key equality is RDF term `Value.equals`, never SPARQL value-equality. The implementation caps the number of distinct keys at 256, but this key-count cap does not bound the number of buffered rows or their bytes.

The `rdf4j.lmdb.islands.enabled` gate controls interior island support (default on); `rdf4j.lmdb.nativeQueryEngine.forceInteriorIslands` can force that path for diagnosis. `rdf4j.lmdb.islands.memo.enabled` controls eligible keyed reuse. The debug-only `rdf4j.lmdb.islands.forceReuseMode` affects reuse-mode testing and must not be treated as an ordinary production optimization. Because replay buffers retain copied `BindingSet` rows until the owning evaluation/entry frame is released, their retained memory follows result cardinality; the 256-key cap is not a byte budget. See [query memory and result lifecycle](query-memory-and-result-lifecycle.md) for the distinction between query-memory reservations and other evaluation-owned heap state.

The 60-slot demotion is narrow. It flattens eligible inner joins, finds variables used only by one operand and not required by the projection, order keys, or another operand, then tries demoting the operands with the most relief as restricted islands. It makes at most four greedy attempts before one all-relieving-operands attempt. It cannot reduce the externally visible output width; a genuinely wide projected result remains a generic-root-host case.

## What a runtime fallback can and cannot do

Compilation decline and runtime decline happen at different layers. A root compiler decline happens before rows exist and becomes a generic root host. A non-root compiler decline returns control to the generic evaluator. At a strategy-arbitration point, an opener may decline before any row is committed; the arbiter can remove that proposal, close any resources it acquired, and try a remaining proposal or the exact native fallback for that dispatch. Which alternatives exist is decision-site-specific.

Once a plan has returned its first binding, restarting it under a second evaluator would duplicate or omit rows and could repeat observable expression effects. Runtime exceptions, cancellation, and resource failures after output therefore propagate through the selected execution path and trigger cleanup; they are not silently converted into a fresh whole-query generic run. See [strategy arbitration](query-arbitration-and-explanation.md) and [result lifecycle](query-memory-and-result-lifecycle.md) for the detailed failure boundary.

## Configuration and developer checks

`nativeQueryEngine.enabled` and `forceRootGeneric` are constructor inputs to the evaluation strategy, so changing their system properties does not rewrite a strategy already constructed. `rootPipeline.enabled`, `bareFragments.enabled`, and island switches are sampled at their compile/planning boundaries. The full defaults, parsers, and read-time table is in [query-configuration.md](query-configuration.md).

When extending the engine, update the algebra census for every new tuple-expression class, then trace the root compiler and tuple compiler separately. Add a generic-host/island disposition if native compilation is partial; add explicit failure behavior for both compile-time decline and candidate-open decline; preserve generic scope, binding, ordering, and cleanup semantics. Source-level contracts and their tests are mapped in [query-test-map.md](query-test-map.md).

## Illustrative route walkthrough (source-only)

Consider this illustrative query (the remote service must be configured by the caller):

```sparql
SELECT ?person ?remoteLabel
WHERE {
  ?person <urn:local:knows> ?friend .
  SERVICE <urn:configured-endpoint> {
    ?friend <urn:remote:label> ?remoteLabel
  }
}
ORDER BY ?person
LIMIT 10
```

The parsed root can be generic-hosted or compiled as a native row root depending on wrapper, source, expression, and slot checks. A successful native row root can scan the local pattern, then keep `SERVICE` at its pinned generic-island position. Since the service body consumes `?friend` from each incoming row and is mapping-parameterized, it is evaluated with each corresponding frame rather than buffered as one independent replay. The `ORDER BY` stage still has to respect its blocking semantics; if the root sort key cannot be lowered, the root pipeline declines and the generic root host remains available before output. If the chosen service or native operator fails after rows have already escaped, the evaluator propagates the failure and closes resources; it cannot restart the whole query without risking repeated rows or remote effects.

Source contracts: [`LmdbNativeGenericRootHostTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGenericRootHostTest.java), [`LmdbNativeInteriorIslandTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeInteriorIslandTest.java), [`LmdbNativeIslandPostureTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeIslandPostureTest.java), [`LmdbNativeGenericBridgeScopeTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGenericBridgeScopeTest.java), and [`LmdbNativeWideQueryTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeWideQueryTest.java). These links identify source contracts, not current pass evidence.
