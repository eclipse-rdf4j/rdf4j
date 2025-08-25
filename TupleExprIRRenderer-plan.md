Goal: Fix remaining TupleExprIRRendererTest failures by keeping the main path — TupleExpr → textual IR → IR transforms → SPARQL — and moving any printing-time heuristics into well-scoped IR transforms when possible.

- Module: core/queryrender
- Test class: org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest

Read the following files before you start:
 - [IrTransforms.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir/util/IrTransforms.java)
 - [TupleExprIRRenderer.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/TupleExprIRRenderer.java)
 - All the files in [ir](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir)

Keep these in your context.

Nice to know:
 - Variables generated during SPARQL parsing typically have a prefix that tells you why they were generated. Such as the prefixes "_anon_path_" or "_anon_collection_" or "_anon_having_".
 - When a UNION is created because of a SPARQL path, the union does not have a new scope. If it has a new scope, then it means that there was a UNION in the original query.

DO NOT CHANGE ANYTHING ABOVE THIS LINE.
-----------------------------------------------------------

Add your plan here:

1. Make sure that the scope variable from the TupleExpr is passed down to the IR nodes during the TupleExpr → textual IR conversion.
2. Make sure that IR transformations for SPARQL paths that merge UNIONs check the scope variable. If the UNION has a new scope, it should not be merged since it indicates an original UNION in the query.
3. Change the code if necessary to ensure that the scope variable is preserved and correctly used in all relevant IR nodes and transformations.
4. Run the TupleExprIRRendererTest to see if the changes have resolved the failures.
5. Update this plan with any additional steps taken or issues encountered during the process.
