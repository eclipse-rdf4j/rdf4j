# Property paths and special tuple operators

This guide records the current path, triple-term, tuple-function, and LATERAL routes. The algebra census only classifies node kinds; the current planner determines whether a concrete expression has a native route, a generic island, or a generic root host. The branch delta catalogued as Q6 changes LMDB routing and execution for the path and special-operator cases below; algebra and SPARQL semantics are stated as constraints on those additions, not as newly introduced language behavior.

## Arbitrary-length property paths

For `ArbitraryLengthPath`, `LmdbNativeAggregatePlanner.compilePath` first tries `PathPlan`, the specialized primitive-ID breadth-first search (BFS). It accepts `+`/`*` (`minLength` 1 or 0) over one or more UNION alternatives of constant-predicate statement patterns with compatible endpoint and graph shape. It removes duplicate predicate/direction steps before building the plan. If that rejects, `compileGeneralPath` tries `GeneralPathPlan` when `rdf4j.lmdb.generalPath.enabled` is on. Only if both decline does the tuple compiler use a generic island or let the generic root host own the expression.

The two native engines have different contracts:

| Plan | Accepted shape | Traversal/result behavior |
|---|---|---|
| `PathPlan` | Constant-predicate alternative step patterns, supported graph scope, and `minLength` 0 or 1. | BFS over 64-bit value IDs. Bound start uses forward BFS; bound target uses backward BFS; two bound endpoints use existence/early exit; both free uses one BFS per distinct step-subject. Each level has a sorted deduplicated primitive frontier; discovered IDs live in sorted runs. No RDF `Value` is materialized during traversal. |
| `GeneralPathPlan` | A compiled step subtree, including sequence/compound steps, negated property sets (including inverse members), runtime-bound predicates, and `minLength > 1`. A generic-island step is not accepted. | Evaluates BFS against a hidden endpoint frame and materializes the complete deduplicated endpoint-pair array for each open before returning its first row. Its all-pairs cost can be high because it collects start nodes and traverses from each. |

`GeneralPathPlan` also carries an unbound GRAPH variable as part of path state: the current compiler binds it while evaluating one named-graph BFS at a time and includes context in the result triples. The class Javadoc says graph-variable scope declines, but that statement is stale relative to `compileGeneralPath` and `GraphVariablePathEvaluation`; follow the executable current compiler branches. The optimized `PathPlan` route is narrower and does decline when its alternative patterns cannot provide the expected fixed graph/predicate shape.

Both plans preserve walk/reachability semantics through endpoint deduplication: a cycle can produce the identity pair for `+` when the node is reachable from itself by a non-empty walk; `*` also adds the zero-length identity. `minLength=m` in the general path seeds with exact-length-`m` results and continues ordinary reachability, so it denotes walks of length at least `m`. Its eager result array is a memory risk proportional to the endpoint-pair set; a small result limit does not avoid that initial full traversal/materialization. By contrast, the specialized path cursor can emit BFS reachability incrementally for a bound traversal, though all-pairs still performs repeated searches.

### Path seeds, result memo, and parallel expansion

For path plans joined against a provider that can enumerate a bounded target set, `PathTargetSet` may collect those targets and let traversal prune work. Its maximum values default to 65,536 and are capped by the factorized-tail memo maximum. If it cannot safely retain the target set, planning falls back to the ordinary path cursor route.

`PathResultMemo` is created per eligible path-join cursor and tied to the path-plan identity and native query source. Its lookup key is source endpoint ID plus traversal direction. It retains completed reachability arrays only while that memo's own entry/value budget accepts them; a rejected insertion leaves execution uncached and increments bypass telemetry. The default cap is 1,048,576 values (the factorized-tail stored-value cap), applied per memo/cursor, not shared across separate memos. Do not treat memo entries as store-wide cache state or as persistent facts.

Adjacency seeds, bidirectional adjacency traversal, direct/sweep/index cursor expansion, and parallel frontier workers are independent physical choices. The native path plan can try a source-provided adjacency view and fall back to sweep or record cursors. Parallel frontier expansion also requires the general parallel pipeline gate and at least 512 current frontier nodes by default (`rdf4j.lmdb.nativePath.frontier.parallelMin`); worker source/probe views and query-memory reservations are closed with the traversal. These options alter path expansion, not the algebraic eligibility of the path node.

The native path switches and defaults are tabulated in [query configuration](query-configuration.md). Important current keys include `nativePath.enabled`, `.memo.enabled`, `.memo.maxValues`, `.targets.enabled`, `.targets.maxValues`, `.adjacencySeeds.enabled`, `.bidirectional.enabled`, and `.frontier.parallelMin`, plus `generalPath.enabled` and the general parallel gates.

## Standalone zero-length path

`ZeroLengthPathPlan` mirrors RDF4J's `ZeroLengthPathIteration` with explicit store-scope rules:

- If either endpoint is bound, it identity-binds the other endpoint without consulting the store. A term need not appear in any statement to satisfy this case.
- If both endpoints are bound, it emits one row iff they are the same RDF term. It first compares IDs, then resolves and compares values when aliases can exist.
- If both are free, it enumerates distinct subject/object terms occurring in statements in the active default/named graph scope. It never enumerates the whole value dictionary.
- It declines an unbound context variable because generic node-level deduplication and a per-graph interpretation do not provide a provably identical binding result.

This distinction is useful when a constant endpoint is absent: identity semantics are still store-free; enumeration semantics are not. Current direct contracts are in [`LmdbNativeZeroLengthPathTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeZeroLengthPathTest.java) and [`LmdbNativePathPlannerTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePathPlannerTest.java).

The parser's zero-or-one wrapper can be compiled only in the recognized form `Distinct(Projection(Union(ZeroLengthPath, step)))`; other DISTINCT-wrapped path shapes decline that special route. The fallback remains available through the normal plan/island boundary.

## TripleRef, reified and annotation references

`TripleRef` lowers to `TripleTermScanPlan` only when `rdf4j.lmdb.nativeTripleTerms.enabled` is on and the query source advertises triple-term scan support. The dedicated index cursor binds the quoted subject, predicate, object, and triple-term ID; entry bindings restrict the scan and stored constants can make it statically empty. `ReifiedTripleRef` and `AnnotationTripleRef` inherit from `TripleRef`, so they pass through the same `instanceof TripleRef` compiler branch. The census classifies them, but their actual physical support still depends on the source capability and flag.

The expression compiler separately supports `SUBJECT`, `PREDICATE`, and `OBJECT` components of triple terms and constructs the `<<(s p o)>>` form when native triple terms are enabled. A non-triple input or invalid component kind yields an expression error result, which follows normal SPARQL expression-error filtering/extension rules. If native triple terms are disabled or the source cannot scan triple terms, the tuple subtree can use the generic island; the expression itself also follows the native expression recognizer's generic fallback rules.

Stored triple-term identifiers and storage index lifetime are described by [storage values and records](storage-values-and-records.md). Related current tests: [`LmdbNativeTripleTermInFilterTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeTripleTermInFilterTest.java) and [`LmdbNativeTripleTermValueEqualityTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeTripleTermValueEqualityTest.java).

## Tuple functions and correlated LATERAL

`TupleFunctionPlan` resolves the registered function from `TupleFunctionRegistry`, evaluates its arguments against the current row, and consumes the function's closeable result iteration. The cursor checks result arity, validates result constants and already-bound names by term equality, interns computed values through the evaluation-scoped synthetic term authority, and rolls row bindings back between results. Stateful/external SPI calls are not replayable kernel fragments. An unknown function URI raises `QueryEvaluationException`; it is not treated as a native-support decline. The tuple function's actual semantics remain the registered implementation's contract.

Native `LateralPlan` is enabled unless `rdf4j.lmdb.nativeLateral.enabled=false` (case-insensitive). It is a non-flattenable correlated nested loop: the right arm sees precisely the operator-entry frame plus left bindings listed in `rightInputBindingNames`. The direct subselect path also preserves RDF4J's rule that declared inputs hidden from the subselect's projected binding domain are not injected into its body. If shared left/right names are not declared inputs, another right name collides with an enclosing binding, a nested lexical scope is unsafe, or either arm cannot compile, planner logic declines to the generic island route.

### Unexecuted shape walkthroughs

For `?x (ex:p|ex:q)+ ?y`, the optimized BFS can use two fixed-predicate step alternatives after deduping identical steps. For `?x !(ex:blocked|^ex:blocked) ?y`, the general BFS can compile the step subtree when each leaf remains a native step plan; its pair set is materialized before first output. If that leaf becomes a generic island, the native general path declines rather than embedding arbitrary generic evaluation inside the traversal.

For a native LATERAL block such as `LATERAL { BIND(?leftValue AS ?rightValue) }`, the right expression is evaluated for each left solution only if `?leftValue` is declared as a right input and exact-visibility checks pass. The plan does not hoist it above the left relation or replay it across different input frames.

## Extension and test map

When extending a path strategy, define min-length behavior, endpoint boundness modes, context scope, repeated-node/cycle semantics, eager versus streaming output, memory growth, and the exact source probes it requires. For special tuple operators, document term authority and frame visibility as carefully as algebra binding names. Declines must happen before a path/tuple cursor consumes output if a caller intends to select another implementation.

Source references include [`LmdbNativeGeneralPathTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeGeneralPathTest.java), [`LmdbNativePathPlannerTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePathPlannerTest.java), [`LmdbNativeZeroLengthPathTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeZeroLengthPathTest.java), [`LmdbNativeTripleTermInFilterTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeTripleTermInFilterTest.java), and the path sections in [query test map](query-test-map.md). These are source links only; no tests or query examples were executed.
