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

Take a look at the following test:

```java
	@Test
	void nested_paths_extreme_1_simple() {
		String q = "SELECT ?s ?n\n" +
				"WHERE {\n" +
				"  ?s foaf:knows/^foaf:knows | !(rdf:type|^rdf:type)/ex:knows? ?n .\n" +
				"}";
		assertSameSparqlQuery(q, cfg());
	}
```

The test fails with:

```
# Original SPARQL query
SELECT ?s ?n
WHERE {
  ?s foaf:knows/^foaf:knows | !(rdf:type|^rdf:type)/ex:knows? ?n .
}

# Original TupleExpr
QueryRoot
   Projection
      ProjectionElemList
         ProjectionElem "s"
         ProjectionElem "n"
      Union
         Join
            StatementPattern
               Var (name=s)
               Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
               Var (name=_anon_path_041a9792e0fd0a24e1fa7a5784fbf23630701234, anonymous)
            StatementPattern
               Var (name=n)
               Var (name=_const_531c5f7d_uri, value=http://xmlns.com/foaf/0.1/knows, anonymous)
               Var (name=_anon_path_041a9792e0fd0a24e1fa7a5784fbf23630701234, anonymous)
         Join
            Union
               Filter
                  Compare (!=)
                     Var (name=_anon_path_341a9792e0fd0a24e1fa7a5784fbf23630701234567, anonymous)
                     ValueConstant (value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type)
                  StatementPattern
                     Var (name=_anon_path_241a9792e0fd0a24e1fa7a5784fbf2363070123456, anonymous)
                     Var (name=_anon_path_341a9792e0fd0a24e1fa7a5784fbf23630701234567, anonymous)
                     Var (name=s)
               Filter
                  Compare (!=)
                     Var (name=_anon_path_341a9792e0fd0a24e1fa7a5784fbf23630701234567, anonymous)
                     ValueConstant (value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type)
                  StatementPattern
                     Var (name=s)
                     Var (name=_anon_path_341a9792e0fd0a24e1fa7a5784fbf23630701234567, anonymous)
                     Var (name=_anon_path_241a9792e0fd0a24e1fa7a5784fbf2363070123456, anonymous)
            Distinct
               Projection
                  ProjectionElemList
                     ProjectionElem "_anon_path_241a9792e0fd0a24e1fa7a5784fbf2363070123456"
                     ProjectionElem "n"
                  Union
                     ZeroLengthPath
                        Var (name=_anon_path_241a9792e0fd0a24e1fa7a5784fbf2363070123456, anonymous)
                        Var (name=n)
                     StatementPattern
                        Var (name=_anon_path_241a9792e0fd0a24e1fa7a5784fbf2363070123456, anonymous)
                        Var (name=_const_36a43afe_uri, value=http://ex/knows, anonymous)
                        Var (name=n)



# Re-rendering with IR debug enabled for this failing test

# IR (raw)
{
  "distinct": false,
  "reduced": false,
  "projection": [
    {
      "varName": "s"
    },
    {
      "varName": "n"
    }
  ],
  "where": {
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
                    "subject": "Var (name: s)\n",
                    "predicate": "Var (name: _const_531c5f7d_uri, value: http://xmlns.com/foaf/0.1/knows, anonymous)\n",
                    "object": "Var (name: _anon_path_541a9792e0fd0a24e1fa7a5784fbf2363070, anonymous)\n"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                  "data": {
                    "subject": "Var (name: n)\n",
                    "predicate": "Var (name: _const_531c5f7d_uri, value: http://xmlns.com/foaf/0.1/knows, anonymous)\n",
                    "object": "Var (name: _anon_path_541a9792e0fd0a24e1fa7a5784fbf2363070, anonymous)\n"
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
                              "subject": "Var (name: _anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012, anonymous)\n",
                              "predicate": "Var (name: _anon_path_841a9792e0fd0a24e1fa7a5784fbf2363070123, anonymous)\n",
                              "object": "Var (name: s)\n"
                            }
                          },
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter",
                            "data": {
                              "conditionText": "?_anon_path_841a9792e0fd0a24e1fa7a5784fbf2363070123 !\u003d rdf:type"
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
                              "predicate": "Var (name: _anon_path_841a9792e0fd0a24e1fa7a5784fbf2363070123, anonymous)\n",
                              "object": "Var (name: _anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012, anonymous)\n"
                            }
                          },
                          {
                            "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter",
                            "data": {
                              "conditionText": "?_anon_path_841a9792e0fd0a24e1fa7a5784fbf2363070123 !\u003d rdf:type"
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
                      "distinct": false,
                      "reduced": false,
                      "projection": [
                        {
                          "varName": "_anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012"
                        },
                        {
                          "varName": "n"
                        }
                      ],
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
                                        "text": "FILTER (sameTerm(?_anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012, ?n))"
                                      }
                                    }
                                  ]
                                },
                                {
                                  "lines": [
                                    {
                                      "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern",
                                      "data": {
                                        "subject": "Var (name: _anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012, anonymous)\n",
                                        "predicate": "Var (name: _const_36a43afe_uri, value: http://ex/knows, anonymous)\n",
                                        "object": "Var (name: n)\n"
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
                      "groupBy": [],
                      "having": [],
                      "orderBy": [],
                      "limit": -1,
                      "offset": -1
                    }
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
  "groupBy": [],
  "having": [],
  "orderBy": [],
  "limit": -1,
  "offset": -1
}
# IR (transformed)
{
  "distinct": false,
  "reduced": false,
  "projection": [
    {
      "varName": "s"
    },
    {
      "varName": "n"
    }
  ],
  "where": {
    "lines": [
      {
        "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion",
        "data": {
          "branches": [
            {
              "lines": [
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple",
                  "data": {
                    "subject": "Var (name: s)\n",
                    "pathText": "foaf:knows/^foaf:knows",
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
                    "subject": "Var (name: _anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012, anonymous)\n",
                    "pathText": "!(rdf:type|^rdf:type)",
                    "object": "Var (name: s)\n"
                  }
                },
                {
                  "class": "org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple",
                  "data": {
                    "subject": "Var (name: _anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012)\n",
                    "pathText": "(ex:knows)?",
                    "object": "Var (name: n)\n"
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
  "groupBy": [],
  "having": [],
  "orderBy": [],
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
    ?s foaf:knows/^foaf:knows ?n .
  }
    UNION
  {
    ?_anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012 !(rdf:type|^rdf:type) ?s .
    ?_anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012 (ex:knows)? ?n .
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
    ?s foaf:knows/^foaf:knows ?n .
  }
    UNION
  {
    ?_anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012 !(rdf:type|^rdf:type) ?s .
    ?_anon_path_741a9792e0fd0a24e1fa7a5784fbf236307012 (ex:knows)? ?n .
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
  ?s foaf:knows/^foaf:knows | !(rdf:type|^rdf:type)/ex:knows? ?n .
}"
```
