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
 - Test results are typically found in the `target/surefire-reports` folder of the module. For instance: [org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest.txt](core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest.txt)


DO NOT CHANGE ANYTHING ABOVE THIS LINE.
-----------------------------------------------------------

There are two failing tests probably due to there being something not quite right with the normalizeZeroOrOneSubselect IR transform.
