# Shared query planning and evaluator semantics

This guide covers shared RDF4J query-algebra and evaluation changes in the
range from merge base `4aec7e9a2223d873b1c1a7703aad4c87bf8354df` to pinned
`HEAD` `a869fe298dc4700ce956bcaf9fece3745c57fe05`. These contracts apply to
generic evaluation as well as LMDB. The LMDB-specific strategy choices are
described in the [query guide set](README.md#native-query-feature-catalog). The central rule is
that a physical optimization may reorder, re-evaluate, or inject bindings only
when the evaluator can justify that the observable SPARQL result and errors
remain the same.

Relative to the merge base, this range adds function repeatability metadata
and query-scoped safety analysis, uses those facts to guard join injection and
reordering, adds indexed replay for independent operands, refines multi-row
`VALUES` placement, and makes generic ordering honor evaluation mode and
volatile keys. The unchanged SPARQL algebra and its independent-operand,
mapping-compatibility, and error-propagation rules are prerequisites for these
optimizations; the sections below distinguish those semantic contracts from
the branch-specific machinery.

## Function repeatability is a semantic promise

`Function` now has a determinism declaration. The default is `VOLATILE`, so an
unknown or third-party function does not become safe for optimizer movement
because its URI looks familiar. An implementation can opt into
`DETERMINISTIC`; that is a promise about values, errors, and effects for equal
arguments, not a hint about invocation count. `mustReturnDifferentResult()` is
stronger than either declaration: a function such as UUID is treated as
volatile even if an implementation also claims determinism.

RDF4J's `QueryEvaluationUtility` recognizes an explicit set of built-in
implementation classes and the query-scoped `NOW()` function as repeatable.
Repeatability within one query is weaker than safety for constant folding into
a reusable prepared plan: `NOW()` can be repeated within an execution, but its
value is refreshed for a later execution. This distinction prevents a
query-constant function from becoming a prepared-plan constant.

At optimization/preparation, repeatable function implementations are pinned
while the `FunctionRegistry` is synchronized. This keeps the optimizer's
classification tied to the implementation that the prepared expression will
resolve, even if the registry later changes. A per-query `QuerySafetySnapshot`
also summarizes subtree facts such as repeatability, query-fatal errors,
mapping-parameterized operators, and binding guarantees. It is identity-based,
updated as optimizer transformations edit the tree, and retained only for the
active optimization/preparation scope. The summary avoids repeatedly walking
large query trees, at the cost of temporary maps and interned variable-name
facts proportional to the analyzed tree and names.

Sources: [`Function`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/Function.java),
[`FunctionRegistry`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/FunctionRegistry.java),
and [`QueryEvaluationUtility`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/util/QueryEvaluationUtility.java).
The source tests include `FunctionDeterminismTest`,
`DeterministicFunctionProbeTest`, `QuerySafetyAnalysisReuseTest`, and
`BindingInjectionSafetyTest`; these were inspected, not run.

## Join evaluation: when bind joins are an as-if optimization

The query algebra evaluates each join operand independently and then combines
compatible mappings. A bind join can be much cheaper because it pushes each
left row's bindings into the right evaluation, but this is equivalent only if
two questions have safe answers:

1. Is re-evaluating the right subtree for each left row repeatable?
2. Can the right subtree observe the pushed bindings in a way that differs
   from independent evaluation followed by a compatible-mapping join?

`JoinQueryEvaluationStep` and `LeftJoinQueryEvaluationStep` use those checks.
For an ordinary, non-mapping-parameterized subtree, the right side stays on
the bind-join path only when it is repeatable and the exact set of injected
left binding names is permitted. The injection test is joint over that set;
per-variable answers cannot safely be unioned. It rejects, among other cases,
expressions that read names the subtree does not guarantee to bind and BIND
targets colliding with injected names.

Some operators are explicitly mapping-parameterized: property paths,
`SERVICE`, `LATERAL`, triple-reference patterns, tuple functions, and unknown
tuple extensions. Their defined evaluation consumes the input mapping, so the
evaluator keeps the correlated per-input route instead of pretending it is
independent evaluation plus a join. Conversely, when the right side is not
repeatable or injection cannot be proven safe, `MaterializedReplayJoinIterator`
evaluates the right side once with join-entry bindings, snapshots its rows,
and replays compatible candidates against the left side. Its compatibility
index uses the caller's `CollectionFactory`, preserving that factory's
in-memory or disk-backed collection policy. Candidate lookup uses a selective
shared-variable posting where available and still verifies full compatibility;
unbound mappings and duplicate rows retain their SPARQL behavior.

Replay has a deliberate space/time tradeoff. It pays to materialize/index the
right multiset once, then avoids both repeated evaluation and scans over
irrelevant right rows. Its memory and spill behavior depend on the collection
factory. It also makes a right-side error observable even when the left side is
empty, because the algebra still evaluates that operand. The indexed join
does not turn a mapping-parameterized subtree into independent evaluation.

For example, the following is an illustrative, unexecuted shape:

```sparql
SELECT ?s ?id WHERE {
  VALUES ?s { <urn:a> <urn:b> }
  OPTIONAL { BIND(UUID() AS ?id) }
}
```

`UUID()` promises a fresh result per invocation. Re-running the optional's
right side once per left row would change the number of invocations relative
to evaluating the optional operand once and replaying its multiset. The replay
path preserves the algebra's independent-operand boundary; it does not promise
that every query with this shape uses the same physical join class, since
eligibility also depends on the query tree and configured evaluator.

Sources: [`JoinQueryEvaluationStep`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/JoinQueryEvaluationStep.java),
[`LeftJoinQueryEvaluationStep`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/LeftJoinQueryEvaluationStep.java),
[`MaterializedReplayJoinIterator`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/MaterializedReplayJoinIterator.java),
and [`BindingSetCompatibilityIndex`](../../../core/collection-factory/api/src/main/java/org/eclipse/rdf4j/collection/factory/api/BindingSetCompatibilityIndex.java).
Relevant source tests include `JoinIndependentOperandTest`,
`MaterializedReplayJoinIndexTest`, and `MaterializedReplayJoinIteratorCancellationTest`.

## Join reordering and VALUES placement

`QueryJoinOptimizer` continues to optimize joins by estimated cardinality and
connectivity, but now carries assured bindings, entry bindings, and safety
facts more carefully across optimizer scopes. It avoids reordering a subtree
containing `LATERAL`; a non-repeatable subtree is also a barrier unless the
specific safe-reordering analysis can establish that the relevant extension
or external-service shape has no volatile or other unsafe barrier. When a
priority subselect contributes a computed guaranteed binding, that name can
help choose the first ordinary statement pattern that consumes it without
being treated as an already-bound value in the ordinary cardinality model.

Multi-row `VALUES` assignments are placed immediately before their first
consumer when the intervening prefix consists only of independent statement
patterns and moving the consumer does not cross a variable dependency or an
entry binding. The source checks only whether an assignment has zero, one, or
more than one row; it need not count a large VALUES table just to classify
this placement. A one-row assignment can remain early, while a multi-row
assignment can seed the connected pattern that uses it before a Cartesian
prefix is built. If the safety conditions do not hold, the ordinary ordering
is retained. The expected benefit is less intermediate work for selective
VALUES-fed patterns; the optimizer still needs statistics and the result is
query-shape dependent.

Merge join is not selected when a runtime-bound variable makes one side
selective. A merge join advances two ordered scans, and that interface does
not push a later input binding into the other scan; an indexed nested-loop
path can use that runtime value instead. The optimizer therefore distinguishes
the variable names already available at runtime from names merely produced by
the join operands. `BindingSetAssignment` separately exposes possible and
assured binding names; see [shared query structures](integration-shared-query-structures.md#values-rows-and-binding-name-guarantees).

The source test map includes
[`QueryJoinOptimizerConnectivityTest`](../../../core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/QueryJoinOptimizerConnectivityTest.java),
[`QueryJoinOptimizerMergeJoinBoundVarsTest`](../../../core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/QueryJoinOptimizerMergeJoinBoundVarsTest.java),
[`QueryJoinOptimizerTest`](../../../core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/QueryJoinOptimizerTest.java),
and [`IterativeEvaluationOptimizerTest`](../../../core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/IterativeEvaluationOptimizerTest.java).
Tests were not run.

## ORDER BY: query mode, volatile keys, and explicit hints

The generic evaluator now gives `ValueComparator` the active
`QueryEvaluationMode`, so `ORDER BY` uses the same strict-versus-standard
comparison policy as other order-sensitive evaluation. This matters for
cross-type values such as calendar datatypes: changing the mode can change
their comparison rules, so a comparator must not silently use one mode for
sorting and another for aggregate extrema.

When an ordering expression is not repeatable, `DefaultEvaluationStrategy`
computes its sort key once for each solution occurrence before sorting and
keeps the key beside (but outside) the query-visible bindings. Calling a
volatile expression from a comparator could produce a different key for the
same row across comparisons and violate comparator consistency. A row-local
`ValueExprEvaluationException` leaves the key absent, which sorts as unbound;
other failures propagate. The evaluator does not apply the order's distinct
or limit shortcut inside this path: distinct elimination belongs to the
parent, and two duplicate occurrences may have different volatile keys. The
tradeoff is an extra key array per occurrence plus the order iterator's normal
sort storage. When the order sits below a `DISTINCT`/`REDUCED` parent, the
volatile-key path disables early truncation so duplicate elimination happens
at its algebraic parent before a later slice; for an ordinary non-distinct
order, the compiled limit can still be passed to the sort iterator. The
implementation uses the configured sort iteration threshold and spill policy
where applicable; no latency or memory measurement is claimed.

`OrderByHint` marks a deterministic backend-specific function that carries an
ordering request rather than a scalar value. The generic evaluator rejects a
hint it cannot satisfy with the hint's stable unsupported-order message. The
LMDB parser's `STABLE_INDEX(?var)` annotation is deliberately restricted to a
standalone `ORDER BY` element with one variable argument; it is not a general
extension-function promise. The [language guide](integration-sparql-language.md#compatibility-notes-and-faq)
and [physical access guide](query-physical-access.md) describe that LMDB path.

Sources: [`DefaultEvaluationStrategy`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java),
[`OrderByHint`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/OrderByHint.java),
[`LmdbIndexOrderFunction`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/lmdb/LmdbIndexOrderFunction.java),
and [`ValueComparator`](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/util/ValueComparator.java).
The mode/parity source tests are `OrderByQueryEvaluationModeTest`,
`QueryEvaluationModeOptimizerParityTest`, and `DefaultEvaluationStrategyOrderByHintTest`.

## Query-fatal errors are not disposable work

Not every failed expression has the same scope. Most expression errors are
solution-local: a failed `FILTER` drops that solution, a failed `BIND` leaves
its target unbound, and a failed `ORDER BY` expression sorts as unbound. A
failed non-silent `SERVICE`, however, fails the query. The optimizer and
physical join selection must not discard such an operand solely because the
other side is empty or a fast path would otherwise short-circuit.

`QueryEvaluationUtility` classifies query-fatal-capable subtrees. Join paths
that might never open the right operand are disabled or wrapped so the right
side is evaluated before normal exhaustion. An explicit close by the caller
(for example cancellation) does not force an unopened right subtree to run.
This keeps semantic evaluation separate from cancellation cleanup. For the
shared timeout wrapper and existing HTTP cancellation protocol, see
[query lifecycle](integration-query-lifecycle.md); for federated `SERVICE`
buffering and failure handling, see [federation semantics](integration-federation-semantics.md).

The changed tests
[`DefaultEvaluationStrategyValueExprErrorTest`](../../../core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategyValueExprErrorTest.java),
[`StrictEvaluationStrategyTest`](../../../core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/StrictEvaluationStrategyTest.java),
and [`ServiceJoinIteratorTelemetryTest`](../../../core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/federation/ServiceJoinIteratorTelemetryTest.java)
distinguish row-local errors, strict evaluation, and remote query telemetry.
They are source references only.

## Safe extension checklist

When adding a function, leave it volatile unless equal arguments really do
produce the same values and equivalent errors without observable effects. A
wrong deterministic declaration can authorize result-changing rewrites.
When adding a tuple operator, declare whether it is mapping-parameterized and
whether injected bindings are proven equivalent; default to declining
independent replay or pushdown when the SPI's input-mapping behavior is
unknown. When adding a join shortcut, preserve duplicate multiplicity,
query-fatal errors, volatile-function invocation count, and complete shared
binding compatibility. When adding a sort optimization, use the query's
evaluation mode and establish each volatile key once per row occurrence.

For binding-name and metadata contracts see
[shared query structures](integration-shared-query-structures.md); for native
operator admission see [query routing](query-routing-and-hosts.md) and
[strategy families](query-strategy-families.md).
