# Scope-safety golden query catalog

The suite contains exactly **50** checked-in SPARQL query files. Result comparison is an exact multiset comparison, except q49 which is sequence-sensitive.

| ID | Category | Rows | Ordered | Title |
|---|---|---:|:---:|---|
| q01 | boundness | 5 | no | OPTIONAL introduces a possibly unbound variable |
| q02 | union | 5 | no | UNION branch-only bindings |
| q03 | values | 2 | no | VALUES UNDEF can be supplied by a later join |
| q04 | values | 2 | no | VALUES binding constrains OPTIONAL compatibility |
| q05 | bind | 5 | no | BIND error preserves the input mapping |
| q06 | bind | 5 | no | Sequential BIND targets are immediately available |
| q07 | filter | 2 | no | FILTER textual position within one group |
| q08 | group-scope | 0 | no | Nested ordinary group is not lateral |
| q09 | group-scope | 1 | no | Same-group FILTER sees preceding VALUES |
| q10 | bind | 6 | no | BIND before producer is not retroactive |
| q11 | filter-union | 2 | no | FILTER outside UNION on branch-only binding |
| q12 | property-path | 2 | no | Property-path intermediate variable remains hidden |
| q13 | filter-join | 1 | no | FILTER depends on a variable supplied by crossed JOIN operand |
| q14 | filter-optional | 1 | no | BOUND after OPTIONAL must not move before OPTIONAL |
| q15 | filter-bag | 3 | no | OR filter is not bag-union of its disjuncts |
| q16 | filter-errors | 2 | no | Short-circuit conjunction with possibly unbound operand |
| q17 | bind-errors | 5 | no | COALESCE observes OPTIONAL boundness |
| q18 | minus-filter | 2 | no | FILTER over MINUS must not be cloned into RHS |
| q19 | minus-filter | 1 | no | Second MINUS filter regression with fallback row |
| q20 | filter-union | 4 | no | FILTER/UNION distribution preserves duplicate multiplicity |
| q21 | optional-filter | 5 | no | Filter inside OPTIONAL condition preserves fallback |
| q22 | optional-filter | 2 | no | Same filter outside OPTIONAL removes fallback |
| q23 | optional-bag | 3 | no | OPTIONAL multiple witnesses and fallback multiplicity |
| q24 | optional-union | 1 | no | OPTIONAL does not distribute over right UNION |
| q25 | optional-union | 2 | no | OPTIONAL distributes over left UNION |
| q26 | optional-correlation | 4 | no | OPTIONAL condition sees left and right bindings |
| q27 | optional-fallback | 1 | no | All OPTIONAL candidates fail condition: one fallback |
| q28 | optional-compatibility | 1 | no | OPTIONAL compatibility conflict retains left binding |
| q29 | optional-bind | 1 | no | Later JOIN and BIND after OPTIONAL fallback |
| q30 | optional-projection | 3 | no | Dead-looking OPTIONAL is multiplicity-live |
| q31 | minus | 5 | no | MINUS with disjoint domains is a no-op |
| q32 | minus-scope | 3 | no | MINUS RHS variables do not escape SELECT star |
| q33 | minus-empty-domain | 5 | no | MINUS empty mapping does not remove rows |
| q34 | minus-union | 2 | no | MINUS right UNION is existential across both branches |
| q35 | minus-optional | 3 | no | MINUS RHS OPTIONAL mappings remain witnesses |
| q36 | minus-correlation | 6 | no | MINUS RHS FILTER is not correlated |
| q37 | exists-correlation | 5 | no | NOT EXISTS is correlated unlike MINUS |
| q38 | minus-values | 1 | no | MINUS overlap depends on actual VALUES domains |
| q39 | exists | 1 | no | EXISTS sees current outer mapping |
| q40 | exists-scope | 3 | no | EXISTS-local variables do not escape SELECT star |
| q41 | exists | 2 | no | NOT EXISTS over missing property |
| q42 | exists-bag | 4 | no | EXISTS correlation varies per duplicate outer row |
| q43 | subquery-scope | 4 | no | Subselect hidden variable is independent outside |
| q44 | subquery-capture | 2 | no | Subselect flattening must not capture hidden same-name variable |
| q45 | subquery-distinct | 5 | no | Subselect DISTINCT is cardinality-visible outside |
| q46 | subquery-bag | 6 | no | Subselect without DISTINCT preserves projected duplicates |
| q47 | subquery-alias | 5 | no | Subselect alias is the exported interface |
| q48 | grouping | 3 | no | Grouping creates a query-level interface |
| q49 | subquery-sequence | 2 | yes | Subselect ORDER BY and LIMIT remain local |
| q50 | graph-scope | 2 | no | GRAPH variable is exported from named-graph matching |
