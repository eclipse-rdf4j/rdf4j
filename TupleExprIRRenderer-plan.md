# Plan for improving TupleExprIRRenderer, IR transforms, and rendering

Main rendering path — TupleExpr → raw IR → transformed IR → SPARQL.

The TupleExprt → raw IR step should have as little logic as possible, just enough to create a good representation of the TupleExpr tree. All the logic should be in the IR transforms, or if *really* needed, in the final rendering step.

- Module: core/queryrender
- Test class: [TupleExprIRRendererTest.java](core/queryrender/src/test/java/org/eclipse/rdf4j/queryrender/TupleExprIRRendererTest.java)
- Test class: [SparqlPropertyPathStreamTest.java](core/queryrender/src/test/java/org/eclipse/rdf4j/queryrender/SparqlPropertyPathStreamTest.java)

Read the following files before you start:
 - [IrTransforms.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir/util/IrTransforms.java)
 - [TupleExprIRRenderer.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/TupleExprIRRenderer.java)
 - All the files in [ir](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir)
 - All the files in [transform](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir/util/transform)

Keep these in your context.

Nice to know:
 - Variables generated during SPARQL parsing typically have a prefix that tells you why they were generated. Such as the prefixes "_anon_path_" or "_anon_collection_" or "_anon_having_".
 - Test results are typically found in the `target/surefire-reports` folder of the module. For instance: [org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest.txt](core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest.txt)

Important: Regularly run the tests in `core/queryrender` to ensure nothing breaks as you make changes.

Finally, re-read this entire plan regularly and keep it up to date as you make changes.

# Current task
I want you to run the tests and see what's failing. Start with the first failure and work on that first.

While fixing the issues, keep in mind that I want you to simplify and unify the code. Paths can usually be contain other paths, so it feels like it's a sort of problem that should be solved by recursion to some degree.

Finding a better approach to handling paths is key!

DO NOT CHANGE ANYTHING ABOVE THIS LINE.
-----------------------------------------------------------

LOOK AT THE CODE, UNDERSTAND HOW IT WORKS, MAKE A PLAN FOR HOW YOU INTEND TO FIND THE ROOT CAUSE AND HOW TO FIX IT. THEN START WORKING.

# Overall plan
TODO

# Step by step plan
TODO

# Work log
TODO
