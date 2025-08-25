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

DO NOT CHANGE ANYTHING ABOVE THIS LINE.
-----------------------------------------------------------

There are two failing tests.

 - deep_exists_with_path_and_inner_filter()
 - deep_path_in_filter_not_exists()

You can see the raw IR from one of the tests:

```json
{
  "distinct": false,
  "reduced": false,
  "projection": [
    {
      "varName": "s"
    }
  ],
  "where": {
    "lines": [
      {
        "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter",
        "data": {
          "conditionText": "EXISTS { ?s foaf:knows+ ?_anon_path_6511cce654c441d34c76919d0b25afbaa4120123 . ?o ex:knows ?_anon_path_6511cce654c441d34c76919d0b25afbaa4120123 . FILTER (BOUND(?o)) }"
        }
      }
    ]
  },
  "groupBy": [],
  "having": [],
  "orderBy": [],
  "limit": -1,
  "offset": -1
}
```

You can see that we need to extend the IrFilter class to allow it to have a body which can be a simple IrFilterBodyText, IrNot and IrExists node (you need to make this) with a BGP, because we need to store the raw bgp inside the EXISTS, so that we can apply the path transform to it.
