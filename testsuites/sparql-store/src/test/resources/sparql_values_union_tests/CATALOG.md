# Test catalog

## T001 — single absent arm still needs late binding

- category: `late_binding_absent_vars`

- classification: `unsafe-prebind`

- note: Right arm does not mention ?x but late join must still add ?x there.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y WHERE {
{ ?s :p ?x } UNION { ?s :q ?y }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "")
VALUES ?x { 1 }
```



## T002 — both arms ignore VALUES variable

- category: `late_binding_absent_vars`

- classification: `unsafe-drop-values`

- note: Dropping VALUES because no arm mentions ?x is wrong: late join creates a cross-product.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{ ?s :p ?o } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?count), "")
VALUES ?x { 1 2 }
```



## T003 — variable absent from every arm but projected

- category: `late_binding_absent_vars`

- classification: `unsafe-drop-values`

- note: No arm mentions ?z, but the final solutions must still contain ?z from VALUES.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?z WHERE {
{ ?s :p ?o } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?z), "")
VALUES ?z { 10 20 }
```



## T004 — two-column late binding across asymmetric arms

- category: `late_binding_absent_vars`

- classification: `row-correlation`

- note: Each arm receives the full row after the union; missing columns are bound late.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z ?y WHERE {
{ ?s :p ?x } UNION { ?s :q ?y }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "") COALESCE(STR(?y), "")
VALUES (?x ?z) { (1 10) (2 20) }
```



## T005 — nested union with outer absent arm

- category: `late_binding_absent_vars`

- classification: `unsafe-drop-values`

- note: The :r arm never mentions ?x but still needs the row at the arm root.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{{ ?s :p ?x } UNION { ?s :q ?o }} UNION { ?s :r ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T006 — empty-group arm still cross joins with VALUES

- category: `late_binding_absent_vars`

- classification: `unsafe-drop-values`

- note: An empty/group expression arm produces one mapping that must be joined with every VALUES row.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?tag ?x ?s WHERE {
{ BIND(:T AS ?tag) } UNION { ?s :p ?o }
}
ORDER BY COALESCE(STR(?tag), "") COALESCE(STR(?x), "") COALESCE(STR(?s), "")
VALUES ?x { 1 2 }
```



## T007 — arm-local join plus absent VALUES variable

- category: `late_binding_absent_vars`

- classification: `unsafe-drop-values`

- note: Even when an arm has internal joins, late VALUES still binds absent variables.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?t WHERE {
{ ?s :p ?o . ?s :tag ?t } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?t), "")
VALUES ?x { 1 }
```



## T008 — three-way union with one binding arm and two absent arms

- category: `late_binding_absent_vars`

- classification: `unsafe-drop-values`

- note: Only the first arm mentions ?x; the others still need the VALUES rows.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 9 .
:c :p 2 .
:d :q 8 .
:e :r 5 .
:f :tag :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?x } UNION { ?s :q ?o } UNION { ?s :r ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 2 }
```



## T009 — two-column correlation across separate arms

- category: `row_correlation_duplicates_undef`

- classification: `row-correlation`

- note: Pushing columns independently would fabricate cross-paired rows.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 10) (2 20) }
```



## T010 — duplicate VALUES rows preserve bag cardinality

- category: `row_correlation_duplicates_undef`

- classification: `bag-cardinality`

- note: Duplicate VALUES rows must duplicate results; deduplication is incorrect.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 10) (1 10) }
```



## T011 — same x paired with different z values

- category: `row_correlation_duplicates_undef`

- classification: `row-correlation`

- note: If an optimizer splits the table by column, it will lose x/z correlation.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 10) (1 20) }
```



## T012 — same z paired with different x values

- category: `row_correlation_duplicates_undef`

- classification: `row-correlation`

- note: Same issue as above but keyed by the other column.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 10) (2 10) }
```



## T013 — UNDEF in second column

- category: `row_correlation_duplicates_undef`

- classification: `undef-correlation`

- note: UNDEF means unbound, not a wildcard literal, and row correlation still matters.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 UNDEF) (2 20) }
```



## T014 — UNDEF in first column

- category: `row_correlation_duplicates_undef`

- classification: `undef-correlation`

- note: This stresses late binding of an unbound first column.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (UNDEF 10) (2 20) }
```



## T015 — three-column correlation across three arms

- category: `row_correlation_duplicates_undef`

- classification: `row-correlation`

- note: All three columns are correlated inside one VALUES row.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?mid ?z WHERE {
{ ?s :p ?x } UNION { ?s :m ?mid } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?mid), "") COALESCE(STR(?z), "")
VALUES (?x ?mid ?z) { (1 :k1 10) (2 :k2 20) }
```



## T016 — mixed duplicates and UNDEF rows

- category: `row_correlation_duplicates_undef`

- classification: `bag-cardinality`

- note: Tests duplicate preservation together with unbound columns.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 10 .
:d :q 20 .
:e :m :k1 .
:f :m :k2 .
:g :p 1 .
:h :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 UNDEF) (1 UNDEF) (UNDEF 10) }
```



## T017 — BOUND filter sees early binding

- category: `filter_boundness`

- classification: `unsafe-prebind`

- note: The left-arm FILTER is false before the outer VALUES join but true after prebinding.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(BOUND(?x)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T018 — NOT BOUND filter sees early binding

- category: `filter_boundness`

- classification: `unsafe-prebind`

- note: This is the complementary case to BOUND(?x).


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(!BOUND(?x)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T019 — equality filter on formerly unbound variable

- category: `filter_boundness`

- classification: `unsafe-prebind`

- note: Unbound ?x makes the filter fail originally; prebinding makes it pass.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(?x = 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T020 — inequality filter on formerly unbound variable

- category: `filter_boundness`

- classification: `unsafe-prebind`

- note: Again, early binding changes the filter input.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(?x != 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 2 }
```



## T021 — combined BOUND and data test

- category: `filter_boundness`

- classification: `unsafe-prebind`

- note: The BOUND test short-circuits the arm in the original query.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(BOUND(?x) && ?o = 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T022 — disjunctive filter with unbound branch

- category: `filter_boundness`

- classification: `unsafe-prebind`

- note: Early binding flips the left disjunct from true to false.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(!BOUND(?x) || ?o = 99) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T023 — sameTerm filter on formerly unbound variable

- category: `filter_boundness`

- classification: `unsafe-prebind`

- note: sameTerm is especially sensitive to early binding.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(sameTerm(?x, 1)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T024 — filter outside inner group stresses wrapper placement

- category: `filter_boundness`

- classification: `filter-collection`

- note: Injecting VALUES into the same group without a wrapper can let the filter see ?x too early.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ { ?s :p ?o } FILTER(BOUND(?x)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T025 — STR on formerly unbound variable

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: STR(?x) errors when ?x is unbound but succeeds after prebinding.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(STR(?x) = "1") } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T026 — datatype() on formerly unbound variable

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: datatype(?x) also distinguishes unbound from bound.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(datatype(?x) = xsd:integer) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T027 — isLiteral() on formerly unbound variable

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: The boolean result changes once ?x is prebound.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(isLiteral(?x)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T028 — regex over STR of formerly unbound variable

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: Same observation with a function stack.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(regex(STR(?x), "^1$")) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T029 — COALESCE chooses different branch after prebinding

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: Originally COALESCE picks 0; after prebinding it picks the VALUES term.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(COALESCE(?x, 0) = 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T030 — IN short-circuit with formerly unbound lhs

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: Unbound lhs raises an error; bound lhs can short-circuit to true.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(?x IN (1, 2, 1/0)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T031 — NOT IN with error branch

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: This stresses the NOT IN error/false distinction.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(?x NOT IN (2, 1/0)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T032 — IF on BOUND chooses a different branch

- category: `function_error_sensitivity`

- classification: `unsafe-prebind`

- note: IF makes the branch difference explicit.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
:e :p "en"@en .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(IF(BOUND(?x), ?x = 1, false)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T033 — EXISTS with pushed variable in pattern

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: Without prebinding, ?x is not fixed inside EXISTS and any check value can match.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(EXISTS { ?s :check ?x }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T034 — NOT EXISTS with pushed variable in pattern

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: The EXISTS test flips once ?x is fixed too early.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(NOT EXISTS { ?s :check ?x }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T035 — EXISTS with inner FILTER using pushed variable

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: The inner FILTER can only use ?x if ?x is already bound.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(EXISTS { ?s :check ?m FILTER(?m = ?x) }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T036 — NOT EXISTS with inner FILTER using pushed variable

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: Same issue in negative form.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(NOT EXISTS { ?s :check ?m FILTER(?m = ?x) }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T037 — EXISTS with filter that mentions only pushed variable

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: Originally the inner FILTER sees ?x unbound; prebinding changes that.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(EXISTS { ?s :check ?m FILTER(?x = 1) }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T038 — NOT EXISTS with filter that mentions only pushed variable

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: This catches an optimizer that substitutes ?x too early into EXISTS/NOT EXISTS.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(NOT EXISTS { ?s :check ?m FILTER(?x = 1) }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T039 — nested EXISTS with inner pattern on pushed variable

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: Nested EXISTS amplifies substitution mistakes.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(EXISTS { ?s :check ?m FILTER(EXISTS { ?s :check ?x }) }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T040 — mixed EXISTS and NOT EXISTS across two arms

- category: `exists_not_exists`

- classification: `unsafe-prebind`

- note: Each arm observes early binding in a different way.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 ; :check 1 .
:b :p 2 ; :check 2 .
:c :q 3 ; :check 1 .
:d :q 4 .
:e :p 5 .
:f :check 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o FILTER(EXISTS { ?s :check ?x }) } UNION { ?s :q ?o FILTER(NOT EXISTS { ?s :check ?x }) }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T041 — OPTIONAL binds pushed variable directly

- category: `optional_leftjoin`

- classification: `unsafe-prebind`

- note: A late incompatibility after OPTIONAL is not the same as an early no-match.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s a :T OPTIONAL { ?s :p ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T042 — OPTIONAL right side with extra condition

- category: `optional_leftjoin`

- classification: `unsafe-prebind`

- note: The optional branch can match and later be rejected by the outer VALUES join.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s a :T OPTIONAL { ?s :p ?x . ?s :flag true } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T043 — OPTIONAL filter compares right binding to pushed variable

- category: `optional_leftjoin`

- classification: `unsafe-prebind`

- note: Originally ?x is unbound in the optional filter.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s a :T OPTIONAL { ?s :p ?m FILTER(?m = ?x) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T044 — OPTIONAL filter on directly bound pushed variable

- category: `optional_leftjoin`

- classification: `unsafe-prebind`

- note: The optional branch can bind 2 and then be rejected late, which differs from forcing 1 early.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s a :T OPTIONAL { ?s :p ?x FILTER(?x = 2) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T045 — OPTIONAL filter mentions only pushed variable

- category: `optional_leftjoin`

- classification: `unsafe-prebind`

- note: The filter is false/error in the original arm but true after early binding.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s a :T OPTIONAL { ?s :p ?m FILTER(?x = 1) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T046 — OPTIONAL around inner union

- category: `optional_leftjoin`

- classification: `unsafe-prebind`

- note: Nested UNION inside OPTIONAL is another common optimizer corner case.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s a :T OPTIONAL { { ?s :p ?x } UNION { ?s :flag ?o } } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T047 — safe OPTIONAL when left side already binds x

- category: `optional_leftjoin`

- classification: `safe-must-bound`

- note: Here ?x is already bound on the left before the optional is evaluated.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s :k ?x OPTIONAL { ?s :p ?m FILTER(?m = ?x) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T048 — duplicate VALUES rows over OPTIONAL expose bag counts

- category: `optional_leftjoin`

- classification: `bag-cardinality`

- note: Duplicate VALUES rows should duplicate surviving optional solutions.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 ; :flag true .
:b a :T .
:c a :T ; :p 1 .
:d :q 9 .
:e :q 8 .
:f :k 1 ; :p 1 ; :flag true .
:g :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{ ?s a :T OPTIONAL { ?s :p ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?count), "")
VALUES ?x { 1 1 }
```



## T049 — MINUS with pushed variable on right pattern

- category: `minus`

- classification: `unsafe-prebind`

- note: Originally any :p value removes the row; prebinding restricts the right side to :p 1.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s a :T MINUS { ?s :p ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T050 — MINUS with inner FILTER using pushed variable

- category: `minus`

- classification: `unsafe-prebind`

- note: The MINUS branch behaves differently when ?x is available inside the filter.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s a :T MINUS { ?s :p ?m FILTER(?m = ?x) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T051 — MINUS with no shared vars until early binding creates one

- category: `minus`

- classification: `unsafe-prebind`

- note: Without early binding dom(μ) and dom(μ') are disjoint, so MINUS does nothing.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s a :T MINUS { ?u :u ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T052 — MINUS with no shared vars and inner FILTER

- category: `minus`

- classification: `unsafe-prebind`

- note: This is the FILTER-based version of the same shared-variable trap.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s a :T MINUS { ?u :u ?m FILTER(?m = ?x) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T053 — MINUS after an OPTIONAL on the left

- category: `minus`

- classification: `unsafe-prebind`

- note: The left side remains free of ?x in the original query, but not after early pushdown.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?n WHERE {
{ ?s a :T OPTIONAL { ?s :note ?n } MINUS { ?u :u ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?n), "")
VALUES ?x { 1 }
```



## T054 — safe MINUS when left already binds x

- category: `minus`

- classification: `safe-must-bound`

- note: Here ?x is already bound on the left before MINUS is evaluated.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :k ?x MINUS { ?s :p ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T055 — two-column VALUES around MINUS

- category: `minus`

- classification: `row-correlation`

- note: Row correlation must be preserved even when one arm contains MINUS.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s a :T MINUS { ?u :u ?x } } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 9) (2 10) }
```



## T056 — duplicate VALUES rows over MINUS expose bag counts

- category: `minus`

- classification: `bag-cardinality`

- note: Duplicate rows must duplicate results that survive MINUS.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 2 .
:b a :T ; :p 1 .
:c a :T .
:d :u 1 .
:e :u 2 .
:f :q 9 .
:g :k 1 ; :p 1 .
:h :k 2 ; :p 2 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{ ?s a :T MINUS { ?u :u ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?count), "")
VALUES ?x { 1 1 }
```



## T057 — BIND with COALESCE reads pushed variable

- category: `bind_extend`

- classification: `unsafe-prebind`

- note: The BIND expression sees ?x only if it is pushed too early.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y ?o WHERE {
{ ?s :p ?o BIND(COALESCE(?x, 99) AS ?y) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T058 — BIND with IF(BOUND()) reads pushed variable

- category: `bind_extend`

- classification: `unsafe-prebind`

- note: This makes the dependency on prebinding explicit.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y ?o WHERE {
{ ?s :p ?o BIND(IF(BOUND(?x), ?x, 0) AS ?y) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T059 — BIND stringifies pushed variable

- category: `bind_extend`

- classification: `unsafe-prebind`

- note: The expression result changes from v0 to v1 under early binding.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y ?o WHERE {
{ ?s :p ?o BIND(CONCAT("v", STR(COALESCE(?x, 0))) AS ?y) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T060 — BIND target collides with pushed variable

- category: `bind_extend`

- classification: `bind-target-barrier`

- note: Prebinding ?x before this BIND makes Extend undefined / violates BIND scoping.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :p ?o BIND(1 AS ?x) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T061 — BIND arithmetic uses pushed variable

- category: `bind_extend`

- classification: `unsafe-prebind`

- note: Arithmetic makes the changed value easy to observe.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?sum ?o WHERE {
{ ?s :p ?o BIND(?o + COALESCE(?x, 0) AS ?sum) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?sum), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T062 — BIND result used by a FILTER

- category: `bind_extend`

- classification: `unsafe-prebind`

- note: The FILTER outcome depends on the BIND outcome.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y ?o WHERE {
{ ?s :p ?o BIND(COALESCE(?x, 0) AS ?y) FILTER(?y = 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T063 — BIND only on one arm of the union

- category: `bind_extend`

- classification: `unsafe-prebind`

- note: A per-arm optimizer must still respect the barrier on the BIND arm.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y ?o WHERE {
{ ?s :p ?o } UNION { ?s :q ?o BIND(COALESCE(?x, 0) AS ?y) }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T064 — nested group with BIND and pushed variable

- category: `bind_extend`

- classification: `unsafe-prebind`

- note: Extra braces can expose mistakes in scope-aware rewrites.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 5 .
:b :p 7 .
:c :q 2 .
:d :q 3 .
:e :p 1 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y ?o WHERE {
{ { ?s :p ?o BIND(COALESCE(?x, 0) AS ?y) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T065 — hidden subquery variable must not be captured

- category: `subquery_scope_and_barriers`

- classification: `scope-barrier`

- note: The inner ?x is hidden because the subquery projects only ?s.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x WHERE {
  { SELECT ?s WHERE { ?s :p ?x } }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T066 — hidden variable inside a union-bearing subquery

- category: `subquery_scope_and_barriers`

- classification: `scope-barrier`

- note: The outer optimizer must not push ?x into the hidden variable inside the subquery.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x WHERE {
  { SELECT ?s WHERE {
      { ?s :p ?x } UNION { ?s :r ?o }
    }
  }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T067 — subquery ORDER BY/LIMIT with hidden variable

- category: `subquery_scope_and_barriers`

- classification: `subquery-barrier`

- note: Bottom-up subquery evaluation chooses one row before the outer VALUES join.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x WHERE {
  { SELECT ?s WHERE { ?s :p ?x } ORDER BY ?x LIMIT 1 }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 2 }
```



## T068 — subquery ORDER BY DESC/LIMIT with hidden variable

- category: `subquery_scope_and_barriers`

- classification: `subquery-barrier`

- note: The selected row changes if ?x is pushed inside before LIMIT.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x WHERE {
  { SELECT ?s WHERE { ?s :p ?x } ORDER BY DESC(?x) LIMIT 1 }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T069 — subquery aggregation hides x but changes counts

- category: `subquery_scope_and_barriers`

- classification: `subquery-barrier`

- note: Pushing ?x into the aggregated subquery changes COUNT(*).


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?c WHERE {
  { SELECT ?s (COUNT(*) AS ?c) WHERE { ?s :p ?x } GROUP BY ?s }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?c), "")
VALUES ?x { 1 }
```



## T070 — subquery HAVING depends on hidden-variable counts

- category: `subquery_scope_and_barriers`

- classification: `subquery-barrier`

- note: Early restriction can change which groups satisfy HAVING.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x WHERE {
  { SELECT ?s WHERE { ?s :p ?x } GROUP BY ?s HAVING(COUNT(*) > 1) }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T071 — subquery DISTINCT over hidden x

- category: `subquery_scope_and_barriers`

- classification: `subquery-barrier`

- note: DISTINCT is applied inside the subquery before the outer VALUES join.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x WHERE {
  { SELECT DISTINCT ?s WHERE { ?s :p ?x } }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T072 — projected x inside subquery still blocked by LIMIT

- category: `subquery_scope_and_barriers`

- classification: `subquery-barrier`

- note: Even when ?x is projected, LIMIT means the subquery must be evaluated bottom up.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :r 7 .
:e :p 1, 2 .
:f :p 2 .
:g :q 8 .
:h :r 6 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x WHERE {
  { SELECT ?s ?x WHERE { ?s :p ?x } ORDER BY ?x LIMIT 1 }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 2 }
```



## T073 — COUNT(*) exposes duplicate VALUES rows

- category: `aggregation_and_modifiers`

- classification: `bag-cardinality`

- note: Duplicate VALUES rows must duplicate solutions before COUNT(*).


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT (COUNT(*) AS ?count) WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 10) (1 10) }
```



## T074 — GROUP BY x after correlated VALUES

- category: `aggregation_and_modifiers`

- classification: `row-correlation`

- note: If x/z are de-correlated, group counts change.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?x (COUNT(*) AS ?count) WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
GROUP BY ?x
ORDER BY COALESCE(STR(?x), "") COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 10) (2 20) }
```



## T075 — GROUP BY subject after correlated VALUES

- category: `aggregation_and_modifiers`

- classification: `row-correlation`

- note: Per-subject multiplicities expose duplicated or missing rows.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s (COUNT(*) AS ?count) WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
GROUP BY ?s
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 10) (2 20) }
```



## T076 — COUNT(DISTINCT ?s) with mixed arm bindings

- category: `aggregation_and_modifiers`

- classification: `row-correlation`

- note: This catches optimizers that invent extra subject/value combinations.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT (COUNT(DISTINCT ?s) AS ?count) WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 10) (1 20) }
```



## T077 — HAVING depends on bag cardinality

- category: `aggregation_and_modifiers`

- classification: `bag-cardinality`

- note: If duplicate handling is wrong, HAVING flips.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?x (COUNT(*) AS ?count) WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
GROUP BY ?x
HAVING(COUNT(*) > 1)
ORDER BY COALESCE(STR(?x), "") COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 10) (1 10) (2 20) }
```



## T078 — DISTINCT after correlated VALUES

- category: `aggregation_and_modifiers`

- classification: `row-correlation`

- note: DISTINCT removes duplicates only after the correct rows have been formed.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT DISTINCT ?x ?z WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 10) (1 10) (2 20) }
```



## T079 — ORDER BY/LIMIT after VALUES join

- category: `aggregation_and_modifiers`

- classification: `modifier-barrier`

- note: Wrong row formation changes the ordered slice.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?z WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
LIMIT 2
VALUES (?x ?z) { (1 10) (2 20) }
```



## T080 — ORDER BY/OFFSET/LIMIT with duplicate rows

- category: `aggregation_and_modifiers`

- classification: `modifier-barrier`

- note: Slicing is very sensitive to duplicate and correlation bugs.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 1 .
:c :p 2 .
:d :q 10 .
:e :q 20 .
:f :q 10 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?z WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
OFFSET 1
LIMIT 3
VALUES (?x ?z) { (1 10) (1 10) (2 20) }
```



## T081 — three-way nested union with filter in one inner arm

- category: `nested_unions`

- classification: `unsafe-prebind`

- note: Only one inner arm is sensitive, but the optimizer must recurse correctly.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{{ ?s :p ?x FILTER(BOUND(?x)) } UNION { ?s :q ?o }} UNION { ?s :r ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T082 — nested union with OPTIONAL arm

- category: `nested_unions`

- classification: `unsafe-prebind`

- note: One nested arm contains a LeftJoin barrier while the others do not.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{ ?s :r ?o } UNION {{ ?s a :T OPTIONAL { ?s :p ?x } } UNION { ?s :q ?o }}
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T083 — nested union with filter on pushed variable in deeper arm

- category: `nested_unions`

- classification: `unsafe-prebind`

- note: A deep arm uses ?x only in a FILTER.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{{ ?s :p ?x } UNION { ?s :q ?o }} UNION {{ ?s :u ?o FILTER(?x = 1) } UNION { ?s :v ?o }}
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T084 — nested union with multi-column VALUES

- category: `nested_unions`

- classification: `row-correlation`

- note: Correlation must survive recursive distribution through the nested UNION tree.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z ?o WHERE {
{{ ?s :p ?x } UNION { ?s :q ?o }} UNION {{ ?s :r ?z } UNION { ?s :v ?o }}
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "") COALESCE(STR(?o), "")
VALUES (?x ?z) { (1 3) (2 9) }
```



## T085 — nested union where one entire branch ignores VALUES variable

- category: `nested_unions`

- classification: `unsafe-drop-values`

- note: The right subtree never mentions ?x but still needs the VALUES rows.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?o WHERE {
{{ ?s :p ?x } UNION { ?s :q ?o }} UNION {{ ?s :r ?o } UNION { ?s :v ?o }}
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T086 — mixed safe and unsafe inner arms

- category: `nested_unions`

- classification: `mixed-safety`

- note: One arm is safe because it binds ?x first; another is unsafe because FILTER observes early binding.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m ?o WHERE {
{{ ?s :p ?x BIND(?x AS ?m) } UNION { ?s :q ?o FILTER(BOUND(?x)) }} UNION { ?s :r ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T087 — union with empty-group arm

- category: `nested_unions`

- classification: `unsafe-drop-values`

- note: Empty-group arms and BIND-only arms still need the VALUES join at the arm root.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?tag WHERE {
{{ } UNION { ?s :p ?x }} UNION { BIND(:T AS ?tag) }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?tag), "")
VALUES ?x { 1 }
```



## T088 — nested union with duplicate VALUES rows

- category: `nested_unions`

- classification: `bag-cardinality`

- note: Duplicates must be preserved through the whole nested UNION tree.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :r 3 .
:d :u 4 .
:e :v 5 .
:f a :T ; :p 2 .
:g a :T .
:h :q 8 .
:i :r 9 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{{ ?s :p ?x } UNION { ?s :q ?o }} UNION { ?s :r ?o }
}
ORDER BY COALESCE(STR(?count), "")
VALUES ?x { 1 1 }
```



## T089 — inner and outer VALUES on same variable

- category: `inner_values_interaction`

- classification: `values-intersection`

- note: The outer VALUES still applies after the arm-local VALUES has done its own restriction.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ VALUES ?x { 1 2 } ?s :p ?x } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T090 — inner and outer multi-column VALUES

- category: `inner_values_interaction`

- classification: `row-correlation`

- note: Both VALUES tables must be joined as rows, not flattened by column.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ VALUES (?x ?z) { (1 9) (2 10) } ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 9) }
```



## T091 — inner VALUES on unrelated variable with outer absent variable

- category: `inner_values_interaction`

- classification: `unsafe-drop-values`

- note: An existing inner VALUES must not cause the outer VALUES to be dropped.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?u ?x WHERE {
{ VALUES ?u { :a } ?s :p ?o } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?u), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T092 — inner VALUES plus filter that observes outer variable

- category: `inner_values_interaction`

- classification: `unsafe-prebind`

- note: If ?x is prebound into the arm, the filter outcome changes.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m ?o WHERE {
{ VALUES ?m { 1 } ?s :p ?o FILTER(?x = ?m) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T093 — inner VALUES inside OPTIONAL with outer VALUES

- category: `inner_values_interaction`

- classification: `unsafe-prebind`

- note: This combines inner VALUES with a LeftJoin barrier.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s a :T OPTIONAL { VALUES ?m { 2 } ?s :p ?m FILTER(?x = ?m) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 2 }
```



## T094 — inner VALUES feeding a BIND target that collides with outer variable

- category: `inner_values_interaction`

- classification: `bind-target-barrier`

- note: Pushing outer ?x before BIND would make the target variable already in-scope.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m ?o WHERE {
{ VALUES ?m { 1 } ?s :p ?o BIND(?m AS ?x) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T095 — inner duplicate VALUES crossed with outer duplicate VALUES

- category: `inner_values_interaction`

- classification: `bag-cardinality`

- note: Both inner and outer duplicate rows contribute multiplicatively.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{ VALUES ?x { 1 1 } ?s :p ?x } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?count), "")
VALUES ?x { 1 1 }
```



## T096 — inner UNDEF values with outer UNDEF

- category: `inner_values_interaction`

- classification: `undef-correlation`

- note: UNDEF handling must remain unbound at both levels.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :p 2 .
:c :q 9 .
:d :q 10 .
:e a :T ; :p 2 .
:f a :T .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m ?o WHERE {
{ VALUES ?m { UNDEF 1 } ?s :p ?o } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "") COALESCE(STR(?o), "")
VALUES ?x { UNDEF 1 }
```



## T097 — simple path arm

- category: `property_paths`

- classification: `safe-path`

- note: Simple property path behaves like a BGP for this optimization.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :next ?x } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { :b }
```



## T098 — inverse path arm

- category: `property_paths`

- classification: `safe-path`

- note: Inverse path still binds ?x directly before any observing operator.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?x ^:next ?s } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { :b }
```



## T099 — sequence path arm

- category: `property_paths`

- classification: `safe-path`

- note: Sequence paths test variable extraction from path translation.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :next/:next ?x } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { :c }
```



## T100 — alternative path arm

- category: `property_paths`

- classification: `safe-path`

- note: Alternative paths stress arm analysis without adding extra barriers.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s (:p|:q) ?x } UNION { ?s :k ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 2 }
```



## T101 — zero-or-more path followed by filter

- category: `property_paths`

- classification: `safe-must-bound`

- note: The path binds ?x before FILTER sees it.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :next* ?x FILTER(?x = :c) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { :c }
```



## T102 — one-or-more path followed by BOUND filter

- category: `property_paths`

- classification: `safe-must-bound`

- note: Another safe case where the arm itself must bind ?x first.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :next+ ?x FILTER(BOUND(?x)) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { :c }
```



## T103 — optional path with multiple VALUES rows

- category: `property_paths`

- classification: `safe-path`

- note: Zero-or-one paths can produce self-bindings and one-step bindings.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :next? ?x } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { :a :b }
```



## T104 — compound path expression

- category: `property_paths`

- classification: `safe-path`

- note: Compound path parsing and translation should not confuse variable tracking.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :next :b ;
   :p 1 .
:b :next :c ;
   :q 2 .
:c :next :d ;
   :opt :c .
:d :mark :d .
:e :k :c .
:f :q 9 .
:g :next :g .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s (:next|:mark)+ ?x } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { :d }
```



## T105 — FILTER safe because x is already bound

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: The arm itself binds ?x before the filter is evaluated.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :k ?x FILTER(?x = 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T106 — EXISTS safe because x is already bound

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: EXISTS sees the same ?x either way because the left pattern already bound it.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :k ?x FILTER(EXISTS { ?s :check ?x }) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T107 — OPTIONAL safe because left side already binds x

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: This is a positive control for the must-bound condition.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s :k ?x OPTIONAL { ?s :p ?m FILTER(?m = ?x) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T108 — MINUS safe because left side already binds x

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: Here shared-variable behavior is unchanged by early visibility of ?x.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :k ?x MINUS { ?s :p ?x } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { 1 }
```



## T109 — BIND safe because x is already bound by the arm

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: The BIND expression reads the same ?x in both plans.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y WHERE {
{ ?s :k ?x BIND(?x AS ?y) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "")
VALUES ?x { 1 }
```



## T110 — BIND plus FILTER safe after internal x binding

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: Another positive control with an expression consumer.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?y WHERE {
{ ?s :k ?x BIND(COALESCE(?x, 0) AS ?y) FILTER(?y = 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "")
VALUES ?x { 1 }
```



## T111 — nested group safe after internal x binding

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: The inner filter depends on ?x, but ?x is already fixed by the arm.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?m WHERE {
{ ?s :k ?x . { ?s :p ?m FILTER(?x = 1) } } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T112 — multi-column safe after internal bindings

- category: `safe_must_bound_cases`

- classification: `safe-must-bound`

- note: Both pushed variables are already bound by the arm before the filter.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :k 1 ; :p 1 ; :check 1 .
:b :k 2 ; :p 2 ; :check 2 .
:c :q 9 .
:d :q 8 .
:e :k 1 ; :check 1 .
:f :k 1 ; :p 3 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :k ?x ; :check ?z FILTER(?x = 1 && ?z = 1) } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 1) }
```



## T113 — empty single-column VALUES

- category: `undef_and_empty_values`

- classification: `empty-values`

- note: An empty VALUES table should annihilate the join.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT (COUNT(*) AS ?count) WHERE {
  { ?s :p ?x } UNION { ?s :q ?o }
}
VALUES ?x { }
```



## T114 — empty multi-column VALUES

- category: `undef_and_empty_values`

- classification: `empty-values`

- note: Same check for a row table.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT (COUNT(*) AS ?count) WHERE {
  { ?s :p ?x } UNION { ?s :q ?z }
}
VALUES (?x ?z) { }
```



## T115 — single UNDEF row on absent variable

- category: `undef_and_empty_values`

- classification: `undef-row`

- note: UNDEF means one row with ?x unbound, not zero rows.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x WHERE {
{ ?s :p ?o } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "")
VALUES ?x { UNDEF }
```



## T116 — UNDEF in first column of correlated row

- category: `undef_and_empty_values`

- classification: `undef-correlation`

- note: The row survives with ?x unbound and ?z bound.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (UNDEF 2) }
```



## T117 — UNDEF in second column of correlated row

- category: `undef_and_empty_values`

- classification: `undef-correlation`

- note: The row survives with ?z unbound and ?x bound.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?s ?x ?z WHERE {
{ ?s :p ?x } UNION { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 UNDEF) }
```



## T118 — all-UNDEF row still contributes one row

- category: `undef_and_empty_values`

- classification: `undef-row`

- note: A row of all UNDEF values is still a row.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT (COUNT(*) AS ?count) WHERE {
{ ?s :p ?o } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?count), "")
VALUES (?x ?z) { (UNDEF UNDEF) }
```



## T119 — empty VALUES across a nested union and subquery

- category: `undef_and_empty_values`

- classification: `empty-values`

- note: Even complex patterns should still be annihilated by an empty VALUES join.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT (COUNT(*) AS ?count) WHERE {
  { { SELECT ?s WHERE { ?s :p ?x } } UNION { ?s :q ?o } }
}
VALUES ?x { }
```



## T120 — duplicate UNDEF rows preserve bag counts

- category: `undef_and_empty_values`

- classification: `bag-cardinality`

- note: Two UNDEF rows and one bound row are three distinct rows in the VALUES table.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a :p 1 .
:b :q 2 .
:c :p 3 .
:d :q 4 .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT (COUNT(*) AS ?count) WHERE {
  { ?s :p ?o } UNION { ?s :q ?o }
}
ORDER BY COALESCE(STR(?count), "")
VALUES ?x { UNDEF UNDEF 1 }
```



## T121 — OPTIONAL + BIND + FILTER in one arm

- category: `combined_stress`

- classification: `unsafe-prebind`

- note: This combines LeftJoin, Extend, and Filter sensitivity in one arm.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?y ?m ?o WHERE {
  { ?s a :T
    OPTIONAL { ?s :p ?m FILTER(?m = ?x) }
    BIND(COALESCE(?x, 0) AS ?y)
    FILTER(?y = 1)
  }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?y), "") COALESCE(STR(?m), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T122 — EXISTS plus MINUS no-shared-var trap

- category: `combined_stress`

- classification: `unsafe-prebind`

- note: The same pushed variable affects both EXISTS substitution and MINUS compatibility.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?o WHERE {
  { ?s a :T
    FILTER(EXISTS { ?s :check ?m FILTER(?m = ?x) })
    MINUS { ?u :u ?x }
  }
  UNION
  { { SELECT ?s WHERE { ?s :q ?o } } }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T123 — nested union + multi-column duplicate VALUES

- category: `combined_stress`

- classification: `row-correlation`

- note: This mixes duplicates, correlation, OPTIONAL, and FILTER over a nested UNION.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT (COUNT(*) AS ?count) WHERE {
  {
    { ?s :p ?x OPTIONAL { ?s :check ?z } }
    UNION
    { ?s :m ?mid FILTER(?x = 1) }
  }
  UNION
  { ?s :q ?z }
}
ORDER BY COALESCE(STR(?count), "")
VALUES (?x ?z) { (1 1) (1 1) (2 2) }
```



## T124 — EXISTS with hidden subquery variable inside union arm

- category: `combined_stress`

- classification: `scope-barrier`

- note: The hidden subquery variable inside EXISTS must not be captured by name.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?o ?m WHERE {
  { ?s :q ?o
    FILTER(EXISTS { { SELECT ?s WHERE { ?s :p ?x } } })
  }
  UNION
  { ?s a :T OPTIONAL { ?s :p ?m } }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?o), "") COALESCE(STR(?m), "")
VALUES ?x { 1 }
```



## T125 — BIND target collision inside OPTIONAL

- category: `combined_stress`

- classification: `bind-target-barrier`

- note: If ?x is prebound before the inner BIND, the rewritten arm becomes invalid.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?m ?o WHERE {
  { ?s a :T
    OPTIONAL {
      ?s :check ?m
      BIND(1 AS ?x)
    }
  }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T126 — property path arm mixed with value-sensitive filter

- category: `combined_stress`

- classification: `unsafe-prebind`

- note: Path translation must not distract the optimizer from the FILTER barrier.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?z WHERE {
  { ?s :next+ ?z FILTER(?x = 1) }
  UNION
  { ?s :q ?z }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?z), "")
VALUES (?x ?z) { (1 :l) (2 10) }
```



## T127 — aggregate subquery followed by MINUS

- category: `combined_stress`

- classification: `subquery-barrier`

- note: This combines a hidden-variable aggregate barrier with the MINUS shared-variable trap.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?c ?o WHERE {
  {
    { SELECT ?s (COUNT(*) AS ?c) WHERE { ?s :p ?x } GROUP BY ?s }
    MINUS
    { ?u :u ?x }
  }
  UNION
  { ?s :q ?o }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?c), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```



## T128 — deep mixed barrier query

- category: `combined_stress`

- classification: `mixed-safety`

- note: This is a high-density regression case mixing safe and unsafe branches.


### data.ttl

```turtle
@prefix : <http://example.com/> .

:a a :T ; :p 1, 2 ; :check 1 ; :flag true ; :k 1 .
:b a :T ; :p 2 ; :check 2 ; :k 2 .
:c a :T ; :check 1 .
:d :q 9 .
:e :q 10 .
:f :u 1 .
:g :r 7 .
:h :m :k1 .
:i :m :k2 .
:j :next :k .
:k :next :l .
:l :mark :l .
```


### query.rq

```sparql
PREFIX : <http://example.com/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?s ?x ?m ?o WHERE {
  {
    { ?s :k ?x OPTIONAL { ?s :p ?m FILTER(EXISTS { ?s :check ?x }) } }
    UNION
    { ?s :q ?o FILTER(NOT EXISTS { ?s :check ?x }) }
    UNION
    { { SELECT ?s WHERE { ?s :p ?x } ORDER BY DESC(?x) LIMIT 1 } }
  }
}
ORDER BY COALESCE(STR(?s), "") COALESCE(STR(?x), "") COALESCE(STR(?m), "") COALESCE(STR(?o), "")
VALUES ?x { 1 }
```


