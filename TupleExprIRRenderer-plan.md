# Plan for improving TupleExprIRRenderer, IR transforms, and rendering

Main rendering path — TupleExpr → raw IR → transformed IR → SPARQL.

The TupleExprt → raw IR step should have as little logic as possible, just enough to create a good representation of the TupleExpr tree. All the logic should be in the IR transforms, or if *really* needed, in the final rendering step.

- Module: core/queryrender
- Test class: org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest

Read the following files before you start:
 - [IrTransforms.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir/util/IrTransforms.java)
 - [TupleExprIRRenderer.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/TupleExprIRRenderer.java)
 - All the files in [ir](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir)
 - All the files in [transform](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir/util/transform)

Keep these in your context.

Nice to know:
 - Variables generated during SPARQL parsing typically have a prefix that tells you why they were generated. Such as the prefixes "_anon_path_" or "_anon_collection_" or "_anon_having_".
 - Test results are typically found in the `target/surefire-reports` folder of the module. For instance: [org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest.txt](core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest.txt)


DO NOT CHANGE ANYTHING ABOVE THIS LINE.
-----------------------------------------------------------

# Current task
I want you to work on reducing the use of "(" and ")" in the generated SPARQL queries. Create a helper method that will determine if parentheses are needed by checking if the current expression is simple enough to not require them or if it already has them. 

As a last step when printing the IrPathTriple you can trim any unnecessary parentheses around the path.

# Overall plan
TODO

# Step by step plan
TODO

# Work log
TODO
