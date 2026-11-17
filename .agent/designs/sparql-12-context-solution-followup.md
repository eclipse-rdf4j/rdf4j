# Deferred SPARQL 1.2 context solution for EXISTS

**Status: unimplemented and outside the current branch integration.** On 2026-10-31 the user chose to keep the integration focused and document this feature separately. Do not implement this design, skip the failing tests, or alter their expected result bags as part of the integration. This document preserves a reviewed direction for a future, separately authorized cross-module change; the exact architecture remains subject to validation against the then-current W3C draft and repository source.

## User-visible problem and evidence

In the current checkout, a SPARQL 1.2 `EXISTS` body containing a sub-SELECT can lose the outer context solution even though that solution is needed inside the subquery. The two existing tests fail before any optimizer comparison, so they demonstrate a raw evaluation gap rather than an optimizer-only regression:

- `ScopedUnionFilterOptimizerTest#projectedExistsSubqueriesKeepCorrelatedInputsDuringOptimization` expects the one `x=1` solution but receives an empty bag.
- `ScopedUnionFilterOptimizerTest#projectedCorrelationMatrixPreservesExistenceAcrossSubqueryOperators` expects two duplicate `x=1` solutions for its documented 1.2 OPTIONAL/EXISTS/SELECT case but receives an empty bag.

The preserved 2026-10-31 neighbor run executed 44 tests with two failures, zero errors and zero skips. Its compact evidence is `post-evidence.filter-evaluation-known-v12-neighbors-20261031.txt`; the copied raw report is `/private/tmp/rdf4j-filter-placement-verified-reruns-20261031-13unrm3x/run-3-evaluation-neighbors/surefire-reports/org.eclipse.rdf4j.query.algebra.evaluation.impl.ScopedUnionFilterOptimizerTest.txt` and the earlier reproduction is `post-evidence.eval-failure-methods-20261031.txt`. The relevant Surefire excerpt is:

    Tests run: 21, Failures: 2, Errors: 0, Skipped: 0
    projectedExistsSubqueriesKeepCorrelatedInputsDuringOptimization: actual []
      expected ["1"^^<http://www.w3.org/2001/XMLSchema#integer>]
    projectedCorrelationMatrixPreservesExistenceAcrossSubqueryOperators: actual []
      expected two [x="1"^^<http://www.w3.org/2001/XMLSchema#integer>] rows

From the repository root, reproduce the owning class with the required JDK 25+ and offline runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py --module core/queryalgebra/evaluation ScopedUnionFilterOptimizerTest --retain-logs -- -DskipITs -Dformatter.skip=true -Dimpsort.skip=true -Dspotless.skip=true

These failures must remain included and reported in current integration verification. Before future behavior-changing work, capture a fresh in-repository failing report and an ExecPlan following `.agent/PLANS.md`; do not treat this older report as proof of a future checkout's behavior.

The semantic reference for the proposal is the [SPARQL 1.2 Query Language Working Draft, 4 October 2026](https://www.w3.org/TR/2026/WD-sparql12-query-20261004/), especially version labels, group graph pattern translation, `EXISTS`, and the context-solution and projection evaluation rules. It is a Working Draft, not a Recommendation. The general design below is an implementation proposal, not a claim that the feature already exists.

## Current exact propagation

- SyntaxTreeBuilder.VersionDecl:411-434 accepts a quoted string and stores its token image in ASTVersionDecl. ASTVersionDecl.getVersion:27 has no owning consumer in the parser; AbstractASTVisitor.visit(ASTVersionDecl):925 only traverses children.
- SPARQLParser.parseQuery:226-273 builds algebra, roots it, creates ParsedTuple/Graph/Boolean/DescribeQuery, attaches dataset, parse options and reserved blank IDs. No semantic-version value is passed.
- ParsedOperation stores source string, QueryParseOptions and reserved IDs; ParsedQuery stores tuple algebra and dataset. QueryParseOptions currently has only blank-node-reference syntax policy.
- SailTupleQuery.evaluate:61-68 passes algebra/dataset/bindings to SailConnection. Direct DefaultEvaluationStrategy.evaluate callers, including the failing tests, bypass repository query wrappers entirely.
- QueryExecutionPolicy contains only experimentalQueryOptimizations; its documentation explicitly separates optimization policy from correctness/scope. QueryOperationContext is a deadline ThreadLocal. Neither is an appropriate language-semantics switch.
- Exists has no mode field. QueryEvaluationContext only captures substituted NAME sets, not an independent context solution.
- FilterIterator.existsProbeFunction:488-510 uses old scoped substitution for complex shapes. BindingAssignerOptimizer.ScopedVarVisitor:187-215 intersects with projected source names. DefaultEvaluationStrategy.preparedStepDecorator:1014-1017 installs SubSelectScopeBoundaryQueryEvaluationStep, whose evaluate:59-68 strips ordinary sibling input while preserving request/LATERAL/graph inputs. Thus SELECT ?z neither substitutes x nor receives x as ordinary input.

## Recommended minimal principled architecture

Resolve recognized version labels in the parser. Keep absent VERSION on the branch's current legacy semantics and honor explicit 1.1 separately from 1.2/1.2-basic. Unknown-label policy should be explicit and tested, not silently interpreted as 1.2. Expose the resolved language version on ParsedOperation/ParsedQuery if needed, but do not make runtime semantics depend on the source string or query wrapper: detached/cloned algebra must be self describing.

Give Exists an intrinsic semantic mode (legacy scoped substitution / context solution), included in equality, hash, clone and packed scalar identity. Default programmatic Exists remains legacy. Internal rewrite-generated EXISTS is assigned the mode justified by that rewrite; never inherit an arbitrary enclosing query's version automatically.

Represent the 1.2 group seed explicitly as a ContextSolution tuple leaf, distinct from SingletonSet and from API/request input. Build it at each real GroupGraphPattern boundary, including empty groups, OPTIONAL/UNION/MINUS groups, sub-SELECT WHERE and nested EXISTS bodies. TupleExprBuilder.visit(ASTGraphPatternGroup):1791, optional:1863, union:1894, minus:1914 and GraphPattern.buildJoinFromRequiredTEs:224 are the construction seams. Internal GraphPattern accumulators/reset operations are not automatically new syntactic groups; introducing a seed at every `new GraphPattern` would be wrong. The seed must precede BIND and OPTIONAL, not merely be appended to completed group output.

The smallest runtime channel can be immutable per-invocation algebra lowering, avoiding a new evaluate(bindings, contextSolution) signature on every evaluation step:
1. At a 1.2 EXISTS invocation, copy the actual condition-visible solution, including its exact bound-name domain and RDF values.
2. Clone that invocation's body and replace each ContextSolution belonging to THIS invocation with a repeatable one-row BindingSetAssignment containing the frozen solution.
3. Traverse through SELECT/projection, OPTIONAL, UNION, MINUS, Group and modifiers, but STOP at nested EXISTS bodies. A nested EXISTS captures its own current solution later; it must not inherit an already-lowered outer snapshot.
4. Compile this lowered runtime subtree through precompileRuntimeSubtree, preserving the existing query/NOW/blank-node/active-graph/cancellation/feedback context. Evaluate its relation from empty ordinary sibling input (retaining genuine request and graph authority as required by the existing API).
5. At top level lower group seeds for the empty context only, again leaving EXISTS-owned seeds symbolic.

Why this is general: in the 1.2 algebra all ordinary recursive operators receive the same immutable context solution; only ContextSolution reads it. Replacing those leaves with the fixed singleton relation is an algebraic implementation of that parameter. No projection whitelist needs to be relaxed. A SELECT may hide x in output while its inner context seed still supplies x before its FILTER. Ordinary SELECT outside EXISTS has empty context and remains independent.

A full immutable invocation-context object plus ContextSolution evaluation step is another correct implementation, but would require forwarding a new context method through every generic and native QueryEvaluationContext decorator. Existing per-probe compilation already provides the necessary invocation lifetime for the lowering design; a mutable strategy field or deadline ThreadLocal would introduce reentrancy/worker hazards.

## Mandatory surrounding consumers

- Centralize mode dispatch in the EXISTS probe factory and use it for scalar EXISTS, FILTER EXISTS/NOT EXISTS, streaming/memoized/materialized/adaptive semi/anti and batched providers. Current DefaultEvaluationStrategy.prepare(Exists):3048 eagerly precompiles the body before invoking the factory; a symbolic 1.2 body cannot take that eager path unchanged. FilterIterator's specialization also precompiles `existsArg` and has a separate materializationFunction; both must observe the same contract.
- Symbolic ContextSolution must have an UNKNOWN pre-invocation binding domain, not a known-empty domain. QueryAlgebraBindingAnalysis already supports OutputFacts.unknown; condition observation domains, safety/name analyses, FilterConditionSimplifier and normalizers must recognize the dynamic context dependency. Do not erase ContextSolution as SingletonSet identity or prove a name unbound before lowering. Lowered VALUES has exact ordinary facts.
- Context dependency cannot always be keyed only by syntactically referenced variables: MINUS observes row domains. A context name that is never mentioned in either operand can make the two context-seeded operands overlap. Cache keys need a proved complete read/domain dependency or the complete frozen context mapping, with bound-name identity, graph/source/frame and mode. Nonrepeatable work still obeys existing effect/replay guards.
- PackedQueryCodec.scalar EXISTS:1428-1432 currently stores only subquery and referenced-name payloads. Extend its semantic identity with mode, and add a distinct relational context seed representation. Audit PackedPlanMaterializer:662, PackedLogicalRuleProgram's generated Exists helper around2137, scalar clone/remap/alias paths, PackedBindingFacts, QueryView, SelectedPlanContextualizer, IncumbentSearch, JoinEnumerator and dependent-subquery costing. Raw round-trip, selected recipe and cache-hit must retain mode/context dependency; no side metadata-only mode.
- LMDB's LmdbNativeEvaluationStrategy.prepare(Exists):552 calls LmdbNativeAggregateCompiler.tryCompileExists on just the tuple body. Keep native existence admission tied to the complete semantic recipe; lower context before native compilation or add the explicit operator and mode support. GenericSubplanDescriptor/evaluation-scoped contexts, NativeSubqueryPlan memo identity and worker copying must not share one outer context's values with another invocation. Native fallback is acceptable only for genuinely unsupported operators, not blanket version disablement.
- Internal MINUS-to-NOT-EXISTS rewrites keep their assured-shared certificate. Query-owned 1.2 context must not be replaced by the MINUS left row merely because a generated EXISTS appears under a 1.2 root. Mode and binding-domain proofs are owned by the transformation.

## Required first-red/control matrix

1. Keep the two existing explicit 1.2 bags unchanged. Run the same queries with absent VERSION and explicit 1.1 as legacy negative controls; SELECT ?x variants remain positive for legacy scoped substitution.
2. Independently nested ordinary SELECT with a nonprojected ancestor x stays independent in both versions, without EXISTS.
3. Nested EXISTS and NOT EXISTS capture the inner current row; two nesting levels, alias projection, DISTINCT/ORDER/LIMIT, BIND EXISTS, and duplicate/UNDEF outer rows.
4. OPTIONAL false/error/unbound condition and no-match/empty RHS; preserve null extension and evaluate explicit conditions after merge.
5. UNION duplicate arms; MINUS disjoint domain, shared optional/UNDEF domain, and a context-only overlap. Example: outer VALUES x{1}, EXISTS { VALUES y{2} MINUS { VALUES z{3} } }. Under 1.2 both group operands carry context x, so MINUS removes the row; the legacy route has disjoint operand variables. This also detects an unsafe referenced-vars-only cache key.
6. Aggregate projection: COUNT(*) with a failing inner x filter produces count0, not no aggregate row; HAVING(COUNT(*)=1) distinguishes the qualifying context. Group keys, aliases and nested aggregates need independent exact bags.
7. Parse/model clone/equality, raw and optimized packed round-trip, real selected-plan/cache hit, scalar/native/worker routes, concurrent invocations with different contexts, cancellation/early close/feedback cleanup.
8. API/request constants and inherited named GRAPH remain separate authority controls; context values must not leak into unrelated scopes or result schemas.

## Scope and related work

The AUTO FILTER structural-authority versus per-condition copy-elision issue described in the original scratch investigation was an independent integration defect. Its reviewed repair is already in this working tree and has separate regression evidence; do not reapply that historical draft as part of the deferred 1.2 work. The remaining work above must still preserve FILTER frame authority, request and graph provenance, cancellation, feedback ownership, and cache identity.

A per-invocation immutable context or carefully scoped algebra lowering is preferable to a shared mutable strategy field. The suggested lowering can minimize changes to runtime interfaces, but it is not implementation approval and must be validated against real nested, aggregate, MINUS, packed and native execution. No performance improvement is claimed.
