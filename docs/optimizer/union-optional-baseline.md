# UNION + OPTIONAL Optimizer Baseline (RDF4J)

This document captures the current RDF4J pipeline and evaluation path relevant to UNION and OPTIONAL before new rules are introduced.

## Algebra construction (parser → tuple expr)

- SPARQL entry point: `core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/SPARQLParser.java`.
- The parser uses `TupleExprBuilder` to build a `TupleExpr` tree.
- OPTIONAL is represented as `LeftJoin` nodes built by `GraphPattern.buildOptionalTE` in `core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/GraphPattern.java`.
- UNION is represented as `Union` nodes built in `TupleExprBuilder.visit(ASTUnionGraphPattern)` in `core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/TupleExprBuilder.java`.
- UNION sets `variableScopeChange` when appropriate; `UnionScopeChangeOptimizer` may later toggle this when safe.

## Optimizer pipeline order

The default pipeline is in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java`.

Order (as of 5.3.0-SNAPSHOT):

1. BindingAssignerOptimizer
2. BindingSetAssignmentInlinerOptimizer
3. ConstantOptimizer
4. RegexAsStringFunctionOptimizer
5. CompareOptimizer
6. ConjunctiveConstraintSplitterOptimizer
7. DisjunctiveConstraintOptimizer
8. SameTermFilterOptimizer
9. UnionScopeChangeOptimizer
10. QueryModelNormalizerOptimizer
11. ProjectionRemovalOptimizer
12. QueryJoinOptimizer
13. IterativeEvaluationOptimizer
14. FilterOptimizer
15. OrderLimitOptimizer

Note: In assertion-enabled builds, `ParentReferenceChecker` is inserted before and after each optimizer.

## Existing UNION/OPTIONAL-related rewrites

- `QueryModelNormalizerOptimizer` distributes Join over Union and reorders well‑designed LeftJoin above Join. This may already change UNION/OPTIONAL shape.
- `FilterOptimizer` can push filters into Union arms and into a LeftJoin’s left side when variables are bound there.
- `UnionScopeChangeOptimizer` toggles scope change for UNION when safe for binding injection.

Any new UNION/OPTIONAL rules must be compatible with these transformations.

## Evaluation classes for UNION and OPTIONAL

- Evaluation strategy entry point: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`.
- UNION evaluation step: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/UnionQueryEvaluationStep.java`.
- OPTIONAL evaluation step: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/LeftJoinQueryEvaluationStep.java`.
  - Chooses `LeftJoinIterator` or `BadlyDesignedLeftJoinIterator` based on well‑designed checks.
  - Annotates `LeftJoin` with algorithm name (visible in plan output).

## Statistics and estimates

- Cardinality heuristics live in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/EvaluationStatistics.java`.
- `EvaluationStatistics.getCardinality(TupleExpr)` assigns estimated result sizes per node, stored as `resultSizeEstimate`.
- `QueryJoinOptimizer` uses these estimates to reorder join arguments.
- Runtime actuals can be collected by enabling `EvaluationStrategy.setTrackResultSize(true)` and `setTrackTime(true)`; these populate `resultSizeActual` and `totalTimeActual` (milliseconds) on query model nodes.

## Plan inspection

- `QueryModelTreePrinter` prints algebra trees with estimates and actuals.
- `QueryModelTreeToGenericPlanNode` converts a tuple expr to `GenericPlanNode` for `Query.explain(...)`, preserving cost/estimate/actual/time and algorithm annotations.
