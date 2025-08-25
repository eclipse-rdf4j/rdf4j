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

Try first to make your changes to the classes within package org.eclipse.rdf4j.queryrender.sparql.ir.util.transform. To fix the unsupported path expression I want you to try to create a new IrTransform that specifically targets this case, and simplify the TupleExprIRRenderer so that it doesn't need so much logic for handling paths. You will need to build out the IR with more nodes. 

Take a look at the following test:

```java
	@Test
void nested_paths_extreme_4_union_mixed_mods() {
	String q = "SELECT ?s ?n\n" +
			"WHERE {\n" +
			"  {\n" +
			"    ?s (((ex:a|^ex:b)/(ex:c/foaf:knows)? )*/(^ex:d/(ex:e|^ex:f)+))/foaf:name ?n .\n" +
			"  }\n" +
			"    UNION\n" +
			"  {\n" +
			"    ?s ((!(ex:g|^ex:h)/(ex:i|^ex:j)?)/((ex:k/foaf:knows)|(^ex:l/ex:m)))/foaf:name ?n .\n" +
			"  }\n" +
			"}";
	assertSameSparqlQuery(q, cfg());
}
```

The test fails with:

```
# Original SPARQL query
SELECT ?s ?n
WHERE {
  {
    ?s (((ex:a|^ex:b)/(ex:c/foaf:knows)? )*/(^ex:d/(ex:e|^ex:f)+))/foaf:name ?n .
  }
    UNION
  {
    ?s ((!(ex:g|^ex:h)/(ex:i|^ex:j)?)/((ex:k/foaf:knows)|(^ex:l/ex:m)))/foaf:name ?n .
  }
}

# Original TupleExpr
QueryRoot
   Projection
      ProjectionElemList
         ProjectionElem "s"
         ProjectionElem "n"
      Union (new scope)
         Join
            Join
               ArbitraryLengthPath
                  Var (name=s)
                  Join
                     Union
                        StatementPattern
                           Var (name=s)
                           Var (name=_const_1ed90317_uri, value=http://ex/a, anonymous)
                           Var (name=_anon_path_754a1c1522fbe8442eaa58479988bc143130123456, anonymous)
                        StatementPattern
                           Var (name=_anon_path_754a1c1522fbe8442eaa58479988bc143130123456, anonymous)
                           Var (name=_const_1ed90318_uri, value=http://ex/b, anonymous)
                           Var (name=s)
                     Distinct
                        Projection
                           ProjectionElemList
                              ProjectionElem "_anon_path_754a1c1522fbe8442eaa58479988bc143130123456"
                              ProjectionElem "_anon_path_654a1c1522fbe8442eaa58479988bc14313012345"
                           Union
                              ZeroLengthPath
                                 Var (name=_anon_path_754a1c1522fbe8442eaa58479988bc143130123456, anonymous)
                                 Var (name=_anon_path_654a1c1522fbe8442eaa58479988bc14313012345, anonymous)
                              Join
                                 StatementPattern
                                    Var (name=_anon_path_754a1c1522fbe8442eaa58479988bc143130123456, anonymous)
                                    Var (name=_const_1ed90319_uri, value=http://ex/c, anonymous)
                                    Var (name=_anon_path_164a1c1522fbe8442eaa58479988bc1431301, anonymous)
                                 StatementPattern
                                    Var (name=_anon_path_164a1c1522fbe8442eaa58479988bc1431301, anonymous)
                                    Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
                                    Var (name=_anon_path_654a1c1522fbe8442eaa58479988bc14313012345, anonymous)
                  Var (name=_anon_path_654a1c1522fbe8442eaa58479988bc14313012345, anonymous)
               Join
                  StatementPattern
                     Var (name=_anon_path_464a1c1522fbe8442eaa58479988bc1431301234, anonymous)
                     Var (name=_const_1ed9031a_uri, value=http://ex/d, anonymous)
                     Var (name=_anon_path_654a1c1522fbe8442eaa58479988bc14313012345, anonymous)
                  ArbitraryLengthPath
                     Var (name=_anon_path_464a1c1522fbe8442eaa58479988bc1431301234, anonymous)
                     Union
                        StatementPattern
                           Var (name=_anon_path_464a1c1522fbe8442eaa58479988bc1431301234, anonymous)
                           Var (name=_const_1ed9031b_uri, value=http://ex/e, anonymous)
                           Var (name=_anon_path_554a1c1522fbe8442eaa58479988bc1431301234, anonymous)
                        StatementPattern
                           Var (name=_anon_path_554a1c1522fbe8442eaa58479988bc1431301234, anonymous)
                           Var (name=_const_1ed9031c_uri, value=http://ex/f, anonymous)
                           Var (name=_anon_path_464a1c1522fbe8442eaa58479988bc1431301234, anonymous)
                     Var (name=_anon_path_554a1c1522fbe8442eaa58479988bc1431301234, anonymous)
            StatementPattern
               Var (name=_anon_path_554a1c1522fbe8442eaa58479988bc1431301234, anonymous)
               Var (name=_const_23b7c3b6_uri, value=http://xmlns.com/foaf/0.1/name, anonymous)
               Var (name=n)
         Join
            Join
               Join
                  Union
                     Filter
                        Compare (!=)
                           Var (name=_anon_path_274a1c1522fbe8442eaa58479988bc143130123, anonymous)
                           ValueConstant (value=http://ex/h)
                        StatementPattern
                           Var (name=_anon_path_174a1c1522fbe8442eaa58479988bc14313012, anonymous)
                           Var (name=_anon_path_274a1c1522fbe8442eaa58479988bc143130123, anonymous)
                           Var (name=s)
                     Filter
                        Compare (!=)
                           Var (name=_anon_path_274a1c1522fbe8442eaa58479988bc143130123, anonymous)
                           ValueConstant (value=http://ex/g)
                        StatementPattern
                           Var (name=s)
                           Var (name=_anon_path_274a1c1522fbe8442eaa58479988bc143130123, anonymous)
                           Var (name=_anon_path_174a1c1522fbe8442eaa58479988bc14313012, anonymous)
                  Distinct
                     Projection
                        ProjectionElemList
                           ProjectionElem "_anon_path_174a1c1522fbe8442eaa58479988bc14313012"
                           ProjectionElem "_anon_path_074a1c1522fbe8442eaa58479988bc1431301"
                        Union
                           ZeroLengthPath
                              Var (name=_anon_path_174a1c1522fbe8442eaa58479988bc14313012, anonymous)
                              Var (name=_anon_path_074a1c1522fbe8442eaa58479988bc1431301, anonymous)
                           Union
                              StatementPattern
                                 Var (name=_anon_path_174a1c1522fbe8442eaa58479988bc14313012, anonymous)
                                 Var (name=_const_1ed9031f_uri, value=http://ex/i, anonymous)
                                 Var (name=_anon_path_074a1c1522fbe8442eaa58479988bc1431301, anonymous)
                              StatementPattern
                                 Var (name=_anon_path_074a1c1522fbe8442eaa58479988bc1431301, anonymous)
                                 Var (name=_const_1ed90320_uri, value=http://ex/j, anonymous)
                                 Var (name=_anon_path_174a1c1522fbe8442eaa58479988bc14313012, anonymous)
               Union
                  Join
                     StatementPattern
                        Var (name=_anon_path_074a1c1522fbe8442eaa58479988bc1431301, anonymous)
                        Var (name=_const_1ed90321_uri, value=http://ex/k, anonymous)
                        Var (name=_anon_path_874a1c1522fbe8442eaa58479988bc143130, anonymous)
                     StatementPattern
                        Var (name=_anon_path_874a1c1522fbe8442eaa58479988bc143130, anonymous)
                        Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
                        Var (name=_anon_path_964a1c1522fbe8442eaa58479988bc143130, anonymous)
                  Join
                     StatementPattern
                        Var (name=_anon_path_184a1c1522fbe8442eaa58479988bc143130123, anonymous)
                        Var (name=_const_1ed90322_uri, value=http://ex/l, anonymous)
                        Var (name=_anon_path_074a1c1522fbe8442eaa58479988bc1431301, anonymous)
                     StatementPattern
                        Var (name=_anon_path_184a1c1522fbe8442eaa58479988bc143130123, anonymous)
                        Var (name=_const_1ed90323_uri, value=http://ex/m, anonymous)
                        Var (name=_anon_path_964a1c1522fbe8442eaa58479988bc143130, anonymous)
            StatementPattern
               Var (name=_anon_path_964a1c1522fbe8442eaa58479988bc143130, anonymous)
               Var (name=_const_23b7c3b6_uri, value=http://xmlns.com/foaf/0.1/name, anonymous)
               Var (name=n)



# Re-rendering with IR debug enabled for this failing test

# IR (raw)
{
  "projection": [
    {
      "varName": "s"
    },
    {
      "varName": "n"
    }
  ],
  "groupBy": [],
  "having": [],
  "orderBy": [],
  "distinct": false,
  "reduced": false,
  "where": {
    "lines": [
      {
        "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
        "data": {
          "branches": [
            {
              "lines": [
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrText",
                  "data": {
                    "text": "# unsupported path"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                  "data": {
                    "subject": "Var (name: _anon_path_394a1c1522fbe8442eaa58479988bc143130123456, anonymous)\n",
                    "predicate": "Var (name: _const_1ed9031a_uri, value: http://ex/d, anonymous)\n",
                    "object": "Var (name: _anon_path_584a1c1522fbe8442eaa58479988bc1431301234567, anonymous)\n"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple",
                  "data": {
                    "subject": "Var (name: _anon_path_394a1c1522fbe8442eaa58479988bc143130123456, anonymous)\n",
                    "pathText": "(ex:e|^ex:f)+",
                    "object": "Var (name: _anon_path_484a1c1522fbe8442eaa58479988bc143130123456, anonymous)\n"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                  "data": {
                    "subject": "Var (name: _anon_path_484a1c1522fbe8442eaa58479988bc143130123456, anonymous)\n",
                    "predicate": "Var (name: _const_23b7c3b6_uri, value: http://xmlns.com/foaf/0.1/name, anonymous)\n",
                    "object": "Var (name: n)\n"
                  }
                }
              ]
            },
            {
              "lines": [
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
                  "data": {
                    "branches": [
                      {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                            "data": {
                              "subject": "Var (name: _anon_path_005a1c1522fbe8442eaa58479988bc1431301234, anonymous)\n",
                              "predicate": "Var (name: _anon_path_105a1c1522fbe8442eaa58479988bc14313012345, anonymous)\n",
                              "object": "Var (name: s)\n"
                            }
                          },
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter",
                            "data": {
                              "conditionText": "?_anon_path_105a1c1522fbe8442eaa58479988bc14313012345 !\u003d ex:h"
                            }
                          }
                        ]
                      },
                      {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                            "data": {
                              "subject": "Var (name: s)\n",
                              "predicate": "Var (name: _anon_path_105a1c1522fbe8442eaa58479988bc14313012345, anonymous)\n",
                              "object": "Var (name: _anon_path_005a1c1522fbe8442eaa58479988bc1431301234, anonymous)\n"
                            }
                          },
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter",
                            "data": {
                              "conditionText": "?_anon_path_105a1c1522fbe8442eaa58479988bc14313012345 !\u003d ex:g"
                            }
                          }
                        ]
                      }
                    ],
                    "newScope": false
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect",
                  "data": {
                    "select": {
                      "projection": [
                        {
                          "varName": "_anon_path_005a1c1522fbe8442eaa58479988bc1431301234"
                        },
                        {
                          "varName": "_anon_path_994a1c1522fbe8442eaa58479988bc143130123"
                        }
                      ],
                      "groupBy": [],
                      "having": [],
                      "orderBy": [],
                      "distinct": false,
                      "reduced": false,
                      "where": {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
                            "data": {
                              "branches": [
                                {
                                  "lines": [
                                    {
                                      "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrText",
                                      "data": {
                                        "text": "FILTER (sameTerm(?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234, ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123))"
                                      }
                                    }
                                  ]
                                },
                                {
                                  "lines": [
                                    {
                                      "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                                      "data": {
                                        "subject": "Var (name: _anon_path_005a1c1522fbe8442eaa58479988bc1431301234, anonymous)\n",
                                        "predicate": "Var (name: _const_1ed9031f_uri, value: http://ex/i, anonymous)\n",
                                        "object": "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n"
                                      }
                                    }
                                  ]
                                },
                                {
                                  "lines": [
                                    {
                                      "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                                      "data": {
                                        "subject": "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n",
                                        "predicate": "Var (name: _const_1ed90320_uri, value: http://ex/j, anonymous)\n",
                                        "object": "Var (name: _anon_path_005a1c1522fbe8442eaa58479988bc1431301234, anonymous)\n"
                                      }
                                    }
                                  ]
                                }
                              ],
                              "newScope": false
                            }
                          }
                        ]
                      },
                      "limit": -1,
                      "offset": -1
                    }
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
                  "data": {
                    "branches": [
                      {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                            "data": {
                              "subject": "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n",
                              "predicate": "Var (name: _const_1ed90321_uri, value: http://ex/k, anonymous)\n",
                              "object": "Var (name: _anon_path_705a1c1522fbe8442eaa58479988bc14313012, anonymous)\n"
                            }
                          },
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                            "data": {
                              "subject": "Var (name: _anon_path_705a1c1522fbe8442eaa58479988bc14313012, anonymous)\n",
                              "predicate": "Var (name: _const_531c5f7d_uri, value: http://xmlns.com/foaf/0.1/knows, anonymous)\n",
                              "object": "Var (name: _anon_path_894a1c1522fbe8442eaa58479988bc14313012, anonymous)\n"
                            }
                          }
                        ]
                      },
                      {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                            "data": {
                              "subject": "Var (name: _anon_path_015a1c1522fbe8442eaa58479988bc14313012345, anonymous)\n",
                              "predicate": "Var (name: _const_1ed90322_uri, value: http://ex/l, anonymous)\n",
                              "object": "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n"
                            }
                          },
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                            "data": {
                              "subject": "Var (name: _anon_path_015a1c1522fbe8442eaa58479988bc14313012345, anonymous)\n",
                              "predicate": "Var (name: _const_1ed90323_uri, value: http://ex/m, anonymous)\n",
                              "object": "Var (name: _anon_path_894a1c1522fbe8442eaa58479988bc14313012, anonymous)\n"
                            }
                          }
                        ]
                      }
                    ],
                    "newScope": false
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                  "data": {
                    "subject": "Var (name: _anon_path_894a1c1522fbe8442eaa58479988bc14313012, anonymous)\n",
                    "predicate": "Var (name: _const_23b7c3b6_uri, value: http://xmlns.com/foaf/0.1/name, anonymous)\n",
                    "object": "Var (name: n)\n"
                  }
                }
              ]
            }
          ],
          "newScope": true
        }
      }
    ]
  },
  "limit": -1,
  "offset": -1
}
# IR (transformed)
{
  "projection": [
    {
      "varName": "s"
    },
    {
      "varName": "n"
    }
  ],
  "groupBy": [],
  "having": [],
  "orderBy": [],
  "distinct": false,
  "reduced": false,
  "where": {
    "lines": [
      {
        "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
        "data": {
          "branches": [
            {
              "lines": [
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrText",
                  "data": {
                    "text": "# unsupported path"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                  "data": {
                    "subject": "Var (name: _anon_path_394a1c1522fbe8442eaa58479988bc143130123456, anonymous)\n",
                    "predicate": "Var (name: _const_1ed9031a_uri, value: http://ex/d, anonymous)\n",
                    "object": "Var (name: _anon_path_584a1c1522fbe8442eaa58479988bc1431301234567, anonymous)\n"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple",
                  "data": {
                    "subject": "Var (name: _anon_path_394a1c1522fbe8442eaa58479988bc143130123456, anonymous)\n",
                    "pathText": "(ex:e|^ex:f)+/foaf:name",
                    "object": "Var (name: n)\n"
                  }
                }
              ]
            },
            {
              "lines": [
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple",
                  "data": {
                    "subject": "Var (name: _anon_path_005a1c1522fbe8442eaa58479988bc1431301234, anonymous)\n",
                    "pathText": "!(ex:h|^ex:g)",
                    "object": "Var (name: s)\n"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect",
                  "data": {
                    "select": {
                      "projection": [
                        {
                          "varName": "_anon_path_005a1c1522fbe8442eaa58479988bc1431301234"
                        },
                        {
                          "varName": "_anon_path_994a1c1522fbe8442eaa58479988bc143130123"
                        }
                      ],
                      "groupBy": [],
                      "having": [],
                      "orderBy": [],
                      "distinct": false,
                      "reduced": false,
                      "where": {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
                            "data": {
                              "branches": [
                                {
                                  "lines": [
                                    {
                                      "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrText",
                                      "data": {
                                        "text": "FILTER (sameTerm(?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234, ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123))"
                                      }
                                    }
                                  ]
                                },
                                {
                                  "lines": [
                                    {
                                      "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                                      "data": {
                                        "subject": "Var (name: _anon_path_005a1c1522fbe8442eaa58479988bc1431301234, anonymous)\n",
                                        "predicate": "Var (name: _const_1ed9031f_uri, value: http://ex/i, anonymous)\n",
                                        "object": "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n"
                                      }
                                    }
                                  ]
                                },
                                {
                                  "lines": [
                                    {
                                      "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                                      "data": {
                                        "subject": "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n",
                                        "predicate": "Var (name: _const_1ed90320_uri, value: http://ex/j, anonymous)\n",
                                        "object": "Var (name: _anon_path_005a1c1522fbe8442eaa58479988bc1431301234, anonymous)\n"
                                      }
                                    }
                                  ]
                                }
                              ],
                              "newScope": false
                            }
                          }
                        ]
                      },
                      "limit": -1,
                      "offset": -1
                    }
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
                  "data": {
                    "branches": [
                      {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple",
                            "data": {
                              "subject": "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n",
                              "pathText": "ex:k/foaf:knows",
                              "object": "Var (name: _anon_path_894a1c1522fbe8442eaa58479988bc14313012, anonymous)\n"
                            }
                          }
                        ]
                      },
                      {
                        "lines": [
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrPropertyList",
                            "data": {
                              "subject": "Var (name: _anon_path_015a1c1522fbe8442eaa58479988bc14313012345, anonymous)\n",
                              "items": [
                                {
                                  "predicate": "Var (name: _const_1ed90322_uri, value: http://ex/l, anonymous)\n",
                                  "objects": [
                                    "Var (name: _anon_path_994a1c1522fbe8442eaa58479988bc143130123, anonymous)\n"
                                  ]
                                },
                                {
                                  "predicate": "Var (name: _const_1ed90323_uri, value: http://ex/m, anonymous)\n",
                                  "objects": [
                                    "Var (name: _anon_path_894a1c1522fbe8442eaa58479988bc14313012, anonymous)\n"
                                  ]
                                }
                              ]
                            }
                          }
                        ]
                      }
                    ],
                    "newScope": false
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                  "data": {
                    "subject": "Var (name: _anon_path_894a1c1522fbe8442eaa58479988bc14313012, anonymous)\n",
                    "predicate": "Var (name: _const_23b7c3b6_uri, value: http://xmlns.com/foaf/0.1/name, anonymous)\n",
                    "object": "Var (name: n)\n"
                  }
                }
              ]
            }
          ],
          "newScope": true
        }
      }
    ]
  },
  "limit": -1,
  "offset": -1
}

# Rendered SPARQL query
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://ex/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?n
WHERE {
  {
    # unsupported path
    ?_anon_path_394a1c1522fbe8442eaa58479988bc143130123456 ex:d ?_anon_path_584a1c1522fbe8442eaa58479988bc1431301234567 .
    ?_anon_path_394a1c1522fbe8442eaa58479988bc143130123456 (ex:e|^ex:f)+/foaf:name ?n .
  }
    UNION
  {
    ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 !(ex:h|^ex:g) ?s .
    {
      SELECT ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123
      WHERE {
        {
          FILTER (sameTerm(?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234, ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123))
        }
          UNION
        {
          ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 ex:i ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 .
        }
          UNION
        {
          ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 ex:j ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 .
        }
      }
    }
    {
      ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 ex:k/foaf:knows ?_anon_path_894a1c1522fbe8442eaa58479988bc14313012 .
    }
      UNION
    {
      ?_anon_path_015a1c1522fbe8442eaa58479988bc14313012345 ex:l ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 ; ex:m ?_anon_path_894a1c1522fbe8442eaa58479988bc14313012 .
    }
    ?_anon_path_894a1c1522fbe8442eaa58479988bc14313012 foaf:name ?n .
  }
}


org.opentest4j.AssertionFailedError: 
Expecting actual:
  "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://ex/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?n
WHERE {
  {
    # unsupported path
    ?_anon_path_394a1c1522fbe8442eaa58479988bc143130123456 ex:d ?_anon_path_584a1c1522fbe8442eaa58479988bc1431301234567 .
    ?_anon_path_394a1c1522fbe8442eaa58479988bc143130123456 (ex:e|^ex:f)+/foaf:name ?n .
  }
    UNION
  {
    ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 !(ex:h|^ex:g) ?s .
    {
      SELECT ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123
      WHERE {
        {
          FILTER (sameTerm(?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234, ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123))
        }
          UNION
        {
          ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 ex:i ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 .
        }
          UNION
        {
          ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 ex:j ?_anon_path_005a1c1522fbe8442eaa58479988bc1431301234 .
        }
      }
    }
    {
      ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 ex:k/foaf:knows ?_anon_path_894a1c1522fbe8442eaa58479988bc14313012 .
    }
      UNION
    {
      ?_anon_path_015a1c1522fbe8442eaa58479988bc14313012345 ex:l ?_anon_path_994a1c1522fbe8442eaa58479988bc143130123 ; ex:m ?_anon_path_894a1c1522fbe8442eaa58479988bc14313012 .
    }
    ?_anon_path_894a1c1522fbe8442eaa58479988bc14313012 foaf:name ?n .
  }
}"
to be equal to:
  "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX ex: <http://ex/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?n
WHERE {
  {
    ?s (((ex:a|^ex:b)/(ex:c/foaf:knows)? )*/(^ex:d/(ex:e|^ex:f)+))/foaf:name ?n .
  }
    UNION
  {
    ?s ((!(ex:g|^ex:h)/(ex:i|^ex:j)?)/((ex:k/foaf:knows)|(^ex:l/ex:m)))/foaf:name ?n .
  }
}"
```
