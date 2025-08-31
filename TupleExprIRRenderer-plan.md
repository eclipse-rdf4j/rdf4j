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
 - When a test fails cfg.debugIR is automatically enabled, which prints the IR before and after transformation. This is very useful for understanding what is going on.

Important: Regularly run the tests in `core/queryrender` to ensure nothing breaks as you make changes.

Finally, re-read this entire plan regularly and keep it up to date as you make changes.

# Diffing the expected and actual from a failing test

Use the following example to diff the expected and actual algebra from a failing test. This is very useful to understand what is going on.

```bash
delta --keep-plus-minus-markers --paging=never -n core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest#testOptionalServicePathScope_SPARQL_expected.txt core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest#testOptionalServicePathScope_SPARQL_actual.txt
```
To diff the TupleExpr algebra from the expeted and actual query, use the following command:
```bash
delta --keep-plus-minus-markers --paging=never -n core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest#testOptionalServicePathScope_TupleExpr_expected.txt core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest#testOptionalServicePathScope_TupleExpr_actual.txt
```

It is also useful to look at the regular failsafe report:
```bash
tail 1000 core/queryrender/target/surefire-reports/org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest.txt
```

# Current task

Before you start fixing the test, fill in the plan below. Focus on discovering if there are any issues in the TupleExpr to IR conversion or if the issue is in a transformer or if it's during printing.

Run the tests in org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest

Use the diff command above to diff the expected and actual SPARQL and algebra from a failing test. This will help you understand what is going on.

DO NOT CHANGE ANYTHING ABOVE THIS LINE.
-----------------------------------------------------------

LOOK AT THE CODE, UNDERSTAND HOW IT WORKS, MAKE A PLAN FOR HOW YOU INTEND TO FIND THE ROOT CAUSE AND HOW TO FIX IT. THEN START WORKING.

# Overall plan
TODO

# Step by step plan
TODO

# Work log
TODO
