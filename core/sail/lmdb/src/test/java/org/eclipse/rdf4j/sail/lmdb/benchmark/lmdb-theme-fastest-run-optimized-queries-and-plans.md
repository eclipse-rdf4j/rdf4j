# Fastest LMDB Theme Query Optimized Queries And Plans

Scope: requested benchmark result files only.
Rule: lower JMH `avgt ms/op` wins per `(themeName, z_queryIndex)`.
Important: details below are taken only from the fastest winning run. If the winning source is summary-only, no fallback plan is substituted.

## Summary

- Parsed benchmark rows: 1416
- Distinct query keys: 94
- Fastest runs with optimized query/plan blocks: 73
- Fastest runs without detailed block: 21

### Fastest Runs Missing Details

| Theme | Query | Score ms/op | Source |
| --- | ---: | ---: | --- |
| `ELECTRICAL_GRID` | 2 | 3.429 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:71) |
| `ELECTRICAL_GRID` | 4 | 3.371 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:73) |
| `ELECTRICAL_GRID` | 8 | 12.329 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:85) |
| `ELECTRICAL_GRID` | 9 | 4.255 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:78) |
| `ENGINEERING` | 0 | 209.646 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:44) |
| `ENGINEERING` | 4 | 47.834 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:40) |
| `ENGINEERING` | 6 | 200.862 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:42) |
| `ENGINEERING` | 10 | 1.578 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:54) |
| `HIGHLY_CONNECTED` | 0 | 323.587 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:47) |
| `HIGHLY_CONNECTED` | 5 | 110.966 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:52) |
| `HIGHLY_CONNECTED` | 6 | 1234.693 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:53) |
| `HIGHLY_CONNECTED` | 9 | 1202.971 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:56) |
| `MEDICAL_RECORDS` | 4 | 103.994 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:7) |
| `MEDICAL_RECORDS` | 9 | 177.495 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:12) |
| `SOCIAL_MEDIA` | 0 | 0.040 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:14) |
| `SOCIAL_MEDIA` | 1 | 4.785 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:15) |
| `SOCIAL_MEDIA` | 2 | 0.049 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:16) |
| `SOCIAL_MEDIA` | 3 | 0.049 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:17) |
| `SOCIAL_MEDIA` | 4 | 0.056 | [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:18) |
| `SOCIAL_MEDIA` | 7 | 4.841 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:21) |
| `TRAIN` | 0 | 31.827 | [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:58) |

## ELECTRICAL_GRID

### Query 0

- Fastest observed: `37.214 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:69)
- Optimized block: [7678](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:7678)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?substation) AS ?count) WHERE {
?substation a <http://example.com/theme/grid/Substation> .
OPTIONAL {
?generator <http://example.com/theme/grid/feeds> ?substation .
?generator <http://example.com/theme/grid/capacity> ?cap .
BIND(?cap AS ?optCap)
}
FILTER (?optCap > 600)
OPTIONAL {
?substation <http://example.com/theme/grid/name> ?name .
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ LeftJoin
│  ║  ├── Filter [left]
│  ║  │  ╠══ Compare (>)
│  ║  │  ║     Var (name=optCap)
│  ║  │  ║     ValueConstant (value="600"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  │  ╚══ LeftJoin
│  ║  │     ├── StatementPattern (resultSizeEstimate=9.4K) [left]
│  ║  │     │     s: Var (name=substation)
│  ║  │     │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │     │     o: Var (name=_const_ac9f03d3_uri, value=http://example.com/theme/grid/Substation, anonymous)
│  ║  │     └── Extension [right]
│  ║  │        ╠══ Join (JoinIterator) (resultSizeEstimate=9.3K)
│  ║  │        ║  ├── StatementPattern (costEstimate=97, resultSizeEstimate=37.4K) [left]
│  ║  │        ║  │     s: Var (name=generator)
│  ║  │        ║  │     p: Var (name=_const_35542676_uri, value=http://example.com/theme/grid/feeds, anonymous)
│  ║  │        ║  │     o: Var (name=substation)
│  ║  │        ║  └── StatementPattern (costEstimate=97, resultSizeEstimate=9.4K) [right]
│  ║  │        ║        s: Var (name=generator)
│  ║  │        ║        p: Var (name=_const_f300a539_uri, value=http://example.com/theme/grid/capacity, anonymous)
│  ║  │        ║        o: Var (name=cap)
│  ║  │        ╚══ ExtensionElem (optCap)
│  ║  │              Var (name=cap)
│  ║  └── StatementPattern (resultSizeEstimate=9.4K) [right]
│  ║        s: Var (name=substation)
│  ║        p: Var (name=_const_9661228a_uri, value=http://example.com/theme/grid/name, anonymous)
│  ║        o: Var (name=name)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=substation)
└── ExtensionElem (count)
Count (Distinct)
Var (name=substation)
```

### Query 1

- Fastest observed: `25.945 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:70)
- Optimized block: [4408](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:4408)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE {
  {
    ?entity a <http://example.com/theme/grid/Substation> .
    ?entity <http://example.com/theme/grid/name> ?name .
    VALUES ?target { "Substation 1" "Substation 2" }
    FILTER ((?name = ?target) || (?name = "Substation 3"))
  }
  UNION
  {
    ?entity a <http://example.com/theme/grid/Generator> .
    ?entity <http://example.com/theme/grid/feeds> ?substation .
    ?substation <http://example.com/theme/grid/name> ?name .
    VALUES ?target { "Substation 1" "Substation 2" }
    FILTER ((?name = ?target) || (?name = "Substation 3"))
  }
  OPTIONAL {
    ?entity <http://example.com/theme/grid/feeds> ?substation2 .
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 2

- Fastest observed: `0.151 ms/op`
- Source: [results-2026-04-22.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-22.md)
- Optimized block: present

Optimized query:

```sparql
SELECT ?transformer (COUNT(DISTINCT ?meter) AS ?meterCount) WHERE {
  VALUES ?name { "Substation 0" "Substation 1" "Substation 2" }
  ?substation <http://example.com/theme/grid/name> ?name .
  FILTER (?name IN ("Substation 0", "Substation 1", "Substation 2"))
  ?transformer <http://example.com/theme/grid/feeds> ?substation .
  ?transformer a <http://example.com/theme/grid/Transformer> .
  OPTIONAL {
    ?transformer <http://example.com/theme/grid/hasMeter> ?meter .
  }
}
GROUP BY ?transformer
HAVING (COUNT(?meter) > 0)
```

Query plan:

_See `./analyze-theme-query-history.sh --theme ELECTRICAL_GRID --query-index 2` for all captured optimized plans._

### Query 3

- Fastest observed: `275.005 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:72)
- Optimized block: [4529](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:4529)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?meter) AS ?count) WHERE {
  ?meter a <http://example.com/theme/grid/Meter> .
  ?meter <http://example.com/theme/grid/measures> ?load .
  OPTIONAL {
    ?load <http://example.com/theme/grid/loadValue> ?value .
    BIND(?value AS ?optValue)
  }
  FILTER (?optValue > 100)
  MINUS {
    {
      {
        ?load2 <http://example.com/theme/grid/loadValue> ?value2 .
        FILTER (?value2 > 180)
      }
    }
    ?meter <http://example.com/theme/grid/measures> ?load2 .
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 4

- Fastest observed: `3.371 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:73)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### ELECTRICAL_GRID Query 5

- Fastest observed: `4.475 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:74)
- Optimized block: [4648](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:4648)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?generator) AS ?count) WHERE {
  ?generator <http://example.com/theme/grid/capacity> ?capacity .
  FILTER (?capacity IN (700, 800, 900))
  ?generator a <http://example.com/theme/grid/Generator> .
  VALUES ?threshold { 700 }
  FILTER NOT EXISTS {
    ?generator <http://example.com/theme/grid/capacity> ?cap2 .
    FILTER (?cap2 < ?threshold)
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 6

- Fastest observed: `80.051 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:75)
- Optimized block: [4704](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:4704)

Optimized query:

```sparql
SELECT ?substation (COUNT(DISTINCT ?asset) AS ?assetCount) WHERE {
  {
    ?asset a <http://example.com/theme/grid/Transformer> .
    ?asset <http://example.com/theme/grid/feeds> ?substation .
  }
  UNION
  {
    ?asset a <http://example.com/theme/grid/Generator> .
    ?asset <http://example.com/theme/grid/feeds> ?substation .
  }
  OPTIONAL {
    ?asset <http://example.com/theme/grid/feeds> ?substation .
    BIND(?substation AS ?optSub)
  }
  FILTER (?optSub != ?asset)
}
GROUP BY ?substation
HAVING (COUNT(?asset) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 7

- Fastest observed: `13.643 ms/op`
- Source: [results-develop.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-develop.md:88)
- Optimized block: [9474](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-develop.md:9474)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?transformer) AS ?count) WHERE {
  ?substation <http://example.com/theme/grid/name> ?name .
  FILTER ((?name = "Substation 0") || (?name = "Substation 1"))
  ?transformer <http://example.com/theme/grid/feeds> ?substation .
  ?transformer a <http://example.com/theme/grid/Transformer> .
  FILTER EXISTS {
    ?transformer <http://example.com/theme/grid/hasMeter> ?meter .
  }
  MINUS {
    ?meter <http://example.com/theme/grid/measures> ?load .
    FILTER (?load = ?substation)
  }
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
   ├── Group ()
   │  ╠══ Difference
   │  ║  ├── Filter
   │  ║  │  ╠══ Exists
   │  ║  │  ║     StatementPattern (resultSizeEstimate=53.3K)
   │  ║  │  ║        s: Var (name=transformer) (bindingState=bound)
   │  ║  │  ║        p: Var (name=_const_fe6c498e_uri, value=http://example.com/theme/grid/hasMeter, anonymous)
   │  ║  │  ║        o: Var (name=meter) (bindingState=unbound)
   │  ║  │  ╚══ Join (JoinIterator)
   │  ║  │     ├── Filter [left]
   │  ║  │     │  ╠══ Or
   │  ║  │     │  ║  ├── Compare (=)
   │  ║  │     │  ║  │     Var (name=name) (bindingState=bound)
   │  ║  │     │  ║  │     ValueConstant (value="Substation 0")
   │  ║  │     │  ║  └── Compare (=)
   │  ║  │     │  ║        Var (name=name) (bindingState=bound)
   │  ║  │     │  ║        ValueConstant (value="Substation 1")
   │  ║  │     │  ╚══ StatementPattern (costEstimate=177, resultSizeEstimate=349)
   │  ║  │     │        s: Var (name=substation) (bindingState=unbound)
   │  ║  │     │        p: Var (name=_const_9661228a_uri, value=http://example.com/theme/grid/name, anonymous)
   │  ║  │     │        o: Var (name=name) (bindingState=unbound)
   │  ║  │     └── Join (JoinIterator) [right]
   │  ║  │        ╠══ StatementPattern (costEstimate=16, resultSizeEstimate=1.0K) [left]
   │  ║  │        ║     s: Var (name=transformer) (bindingState=unbound)
   │  ║  │        ║     p: Var (name=_const_35542676_uri, value=http://example.com/theme/grid/feeds, anonymous)
   │  ║  │        ║     o: Var (name=substation) (bindingState=bound)
   │  ║  │        ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=529) [right]
   │  ║  │              s: Var (name=transformer) (bindingState=bound)
   │  ║  │              p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │              o: Var (name=_const_d6ff201a_uri, value=http://example.com/theme/grid/Transformer, anonymous)
   │  ║  └── Filter (new scope)
   │  ║     ╠══ Compare (=)
   │  ║     ║     Var (name=load) (bindingState=bound)
   │  ║     ║     Var (name=substation) (bindingState=unbound)
   │  ║     ╚══ StatementPattern (resultSizeEstimate=998)
   │  ║           s: Var (name=meter) (bindingState=unbound)
   │  ║           p: Var (name=_const_bcd29754_uri, value=http://example.com/theme/grid/measures, anonymous)
   │  ║           o: Var (name=load) (bindingState=unbound)
   │  ╚══ GroupElem (count)
   │        Count (Distinct)
   │           Var (name=transformer) (bindingState=bound)
   └── ExtensionElem (count)
         Count (Distinct)
            Var (name=transformer) (bindingState=unbound)
```

### Query 8

- Fastest observed: `12.329 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:85)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 9

- Fastest observed: `4.255 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:78)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 10

- Fastest observed: `360.390 ms/op`
- Source: [results-2026-04-18.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:94)
- Optimized block: [10117](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:10117)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?meter) AS ?count) WHERE {
  {
    ?meter a <http://example.com/theme/grid/Meter> .
    ?meter <http://example.com/theme/grid/measures> ?load .
  }
  UNION
  {
    ?meter <http://example.com/theme/grid/measures> ?load .
    ?meter a <http://example.com/theme/grid/Meter> .
    ?load <http://example.com/theme/grid/loadValue> ?value .
  }
  OPTIONAL {
    ?load <http://example.com/theme/grid/loadValue> ?optValue .
  }
  FILTER ((?optValue > 200) && NOT EXISTS { ?load <http://example.com/theme/grid/loadValue> ?low . FILTER (?low < 50) })
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
   ├── Group ()
   │  ╠══ Filter
   │  ║  ├── And
   │  ║  │  ╠══ Compare (>)
   │  ║  │  ║     Var (name=optValue) (bindingState=bound)
   │  ║  │  ║     ValueConstant (value="200"^^<http://www.w3.org/2001/XMLSchema#integer>)
   │  ║  │  ╚══ Not
   │  ║  │        Exists
   │  ║  │           Filter
   │  ║  │           ├── Compare (<)
   │  ║  │           │     Var (name=low) (bindingState=bound)
   │  ║  │           │     ValueConstant (value="50"^^<http://www.w3.org/2001/XMLSchema#integer>)
   │  ║  │           └── StatementPattern (resultSizeEstimate=112.1K)
   │  ║  │                 s: Var (name=load) (bindingState=bound)
   │  ║  │                 p: Var (name=_const_3cb27b8c_uri, value=http://example.com/theme/grid/loadValue, anonymous)
   │  ║  │                 o: Var (name=low) (bindingState=unbound)
   │  ║  └── LeftJoin
   │  ║     ╠══ Union [left]
   │  ║     ║  ├── Join (new scope) (JoinIterator) (resultSizeEstimate=276.7K)
   │  ║     ║  │  ╠══ StatementPattern (costEstimate=56.0K, resultSizeEstimate=112.1K) [left]
   │  ║     ║  │  ║     s: Var (name=meter) (bindingState=unbound)
   │  ║     ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║     ║  │  ║     o: Var (name=_const_33f4134a_uri, value=http://example.com/theme/grid/Meter, anonymous)
   │  ║     ║  │  ╚══ StatementPattern (costEstimate=335, resultSizeEstimate=112.1K) [right]
   │  ║     ║  │        s: Var (name=meter) (bindingState=bound)
   │  ║     ║  │        p: Var (name=_const_bcd29754_uri, value=http://example.com/theme/grid/measures, anonymous)
   │  ║     ║  │        o: Var (name=load) (bindingState=unbound)
   │  ║     ║  └── Join (new scope) (JoinIterator) (resultSizeEstimate=205.4K)
   │  ║     ║     ╠══ Join (JoinIterator) (resultSizeEstimate=276.7K) [left]
   │  ║     ║     ║  ├── StatementPattern (costEstimate=2092.8M, resultSizeEstimate=112.1K) [left]
   │  ║     ║     ║  │     s: Var (name=meter) (bindingState=unbound)
   │  ║     ║     ║  │     p: Var (name=_const_bcd29754_uri, value=http://example.com/theme/grid/measures, anonymous)
   │  ║     ║     ║  │     o: Var (name=load) (bindingState=unbound)
   │  ║     ║     ║  └── StatementPattern (costEstimate=1.00, resultSizeEstimate=112.1K) [right]
   │  ║     ║     ║        s: Var (name=meter) (bindingState=bound)
   │  ║     ║     ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║     ║     ║        o: Var (name=_const_33f4134a_uri, value=http://example.com/theme/grid/Meter, anonymous)
   │  ║     ║     ╚══ StatementPattern (costEstimate=335, resultSizeEstimate=112.1K) [right]
   │  ║     ║           s: Var (name=load) (bindingState=bound)
   │  ║     ║           p: Var (name=_const_3cb27b8c_uri, value=http://example.com/theme/grid/loadValue, anonymous)
   │  ║     ║           o: Var (name=value) (bindingState=unbound)
   │  ║     ╚══ StatementPattern (resultSizeEstimate=112.1K) [right]
   │  ║           s: Var (name=load) (bindingState=bound)
   │  ║           p: Var (name=_const_3cb27b8c_uri, value=http://example.com/theme/grid/loadValue, anonymous)
   │  ║           o: Var (name=optValue) (bindingState=unbound)
   │  ╚══ GroupElem (count)
   │        Count (Distinct)
   │           Var (name=meter) (bindingState=bound)
   └── ExtensionElem (count)
         Count (Distinct)
            Var (name=meter) (bindingState=unbound)
```

### Query 12

- Fastest observed: `1607.592 ms/op`
- Source: [results-2026-04-18.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:96)
- Optimized block: [10350](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:10350)

Optimized query:

```sparql
SELECT ?root ?optName ?optLabel ?optFed ?optFedName ?generator ?substation ?optGeneratorCapacity ?optMeter ?optLoad ?optLoadValue ?line ?optLineCapacity WHERE {
  {
    ?root a <http://example.com/theme/grid/Substation> .
  }
  UNION
  {
    ?root a <http://example.com/theme/grid/Transformer> .
  }
  OPTIONAL {
    ?root <http://example.com/theme/grid/name> ?optName .
    BIND(?optName AS ?optLabel)
  }
  OPTIONAL {
    ?root <http://example.com/theme/grid/feeds> ?optFed .
    OPTIONAL {
      ?optFed <http://example.com/theme/grid/name> ?optFedName .
    }
  }
  OPTIONAL {
    {
      ?generator <http://example.com/theme/grid/feeds> ?root .
    }
    UNION
    {
      ?root <http://example.com/theme/grid/feeds> ?substation .
      ?generator <http://example.com/theme/grid/feeds> ?substation .
    }
    OPTIONAL {
      ?generator <http://example.com/theme/grid/capacity> ?optGeneratorCapacity .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/grid/hasMeter> ?optMeter .
    OPTIONAL {
      ?optMeter <http://example.com/theme/grid/measures> ?optLoad .
    }
    OPTIONAL {
      ?optLoad <http://example.com/theme/grid/loadValue> ?optLoadValue .
    }
  }
  OPTIONAL {
    ?line <http://example.com/theme/grid/connectsTo> ?root .
    OPTIONAL {
      ?line <http://example.com/theme/grid/capacity> ?optLineCapacity .
    }
  }
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "root"
║     ProjectionElem "optName"
║     ProjectionElem "optLabel"
║     ProjectionElem "optFed"
║     ProjectionElem "optFedName"
║     ProjectionElem "generator"
║     ProjectionElem "substation"
║     ProjectionElem "optGeneratorCapacity"
║     ProjectionElem "optMeter"
║     ProjectionElem "optLoad"
║     ProjectionElem "optLoadValue"
║     ProjectionElem "line"
║     ProjectionElem "optLineCapacity"
╚══ LeftJoin (LeftJoinIterator)
   ├── LeftJoin (LeftJoinIterator) [left]
   │  ╠══ LeftJoin (LeftJoinIterator) [left]
   │  ║  ├── LeftJoin (LeftJoinIterator) [left]
   │  ║  │  ╠══ LeftJoin (LeftJoinIterator) [left]
   │  ║  │  ║  ├── Union [left]
   │  ║  │  ║  │  ╠══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=9.4K, indexName=ospc)
   │  ║  │  ║  │  ║     s: Var (name=root) (bindingState=unbound)
   │  ║  │  ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │  ║  │  ║     o: Var (name=_const_ac9f03d3_uri, value=http://example.com/theme/grid/Substation, anonymous)
   │  ║  │  ║  │  ╚══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=28.0K, indexName=ospc)
   │  ║  │  ║  │        s: Var (name=root) (bindingState=unbound)
   │  ║  │  ║  │        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │  ║  │        o: Var (name=_const_d6ff201a_uri, value=http://example.com/theme/grid/Transformer, anonymous)
   │  ║  │  ║  └── Extension [right]
   │  ║  │  ║     ╠══ StatementPattern (resultSizeEstimate=9.4K)
   │  ║  │  ║     ║     s: Var (name=root) (bindingState=bound)
   │  ║  │  ║     ║     p: Var (name=_const_9661228a_uri, value=http://example.com/theme/grid/name, anonymous)
   │  ║  │  ║     ║     o: Var (name=optName) (bindingState=unbound)
   │  ║  │  ║     ╚══ ExtensionElem (optLabel)
   │  ║  │  ║           Var (name=optName) (bindingState=bound)
   │  ║  │  ╚══ LeftJoin [right]
   │  ║  │     ├── StatementPattern (resultSizeEstimate=37.4K) [left]
   │  ║  │     │     s: Var (name=root) (bindingState=bound)
   │  ║  │     │     p: Var (name=_const_35542676_uri, value=http://example.com/theme/grid/feeds, anonymous)
   │  ║  │     │     o: Var (name=optFed) (bindingState=unbound)
   │  ║  │     └── StatementPattern (resultSizeEstimate=9.4K) [right]
   │  ║  │           s: Var (name=optFed) (bindingState=bound)
   │  ║  │           p: Var (name=_const_9661228a_uri, value=http://example.com/theme/grid/name, anonymous)
   │  ║  │           o: Var (name=optFedName) (bindingState=unbound)
   │  ║  └── LeftJoin [right]
   │  ║     ╠══ Union [left]
   │  ║     ║  ├── StatementPattern (new scope) (resultSizeEstimate=37.4K)
   │  ║     ║  │     s: Var (name=generator) (bindingState=unbound)
   │  ║     ║  │     p: Var (name=_const_35542676_uri, value=http://example.com/theme/grid/feeds, anonymous)
   │  ║     ║  │     o: Var (name=root) (bindingState=bound)
   │  ║     ║  └── Join (new scope) (JoinIterator) (resultSizeEstimate=141.3K)
   │  ║     ║     ╠══ StatementPattern (costEstimate=97, resultSizeEstimate=37.4K) [left]
   │  ║     ║     ║     s: Var (name=root) (bindingState=bound)
   │  ║     ║     ║     p: Var (name=_const_35542676_uri, value=http://example.com/theme/grid/feeds, anonymous)
   │  ║     ║     ║     o: Var (name=substation) (bindingState=unbound)
   │  ║     ║     ╚══ StatementPattern (costEstimate=193, resultSizeEstimate=37.4K) [right]
   │  ║     ║           s: Var (name=generator) (bindingState=unbound)
   │  ║     ║           p: Var (name=_const_35542676_uri, value=http://example.com/theme/grid/feeds, anonymous)
   │  ║     ║           o: Var (name=substation) (bindingState=bound)
   │  ║     ╚══ StatementPattern (resultSizeEstimate=9.4K) [right]
   │  ║           s: Var (name=generator) (bindingState=bound)
   │  ║           p: Var (name=_const_f300a539_uri, value=http://example.com/theme/grid/capacity, anonymous)
   │  ║           o: Var (name=optGeneratorCapacity) (bindingState=unbound)
   │  ╚══ LeftJoin [right]
   │     ├── LeftJoin [left]
   │     │  ╠══ StatementPattern (resultSizeEstimate=112.1K) [left]
   │     │  ║     s: Var (name=root) (bindingState=bound)
   │     │  ║     p: Var (name=_const_fe6c498e_uri, value=http://example.com/theme/grid/hasMeter, anonymous)
   │     │  ║     o: Var (name=optMeter) (bindingState=unbound)
   │     │  ╚══ StatementPattern (resultSizeEstimate=112.1K) [right]
   │     │        s: Var (name=optMeter) (bindingState=bound)
   │     │        p: Var (name=_const_bcd29754_uri, value=http://example.com/theme/grid/measures, anonymous)
   │     │        o: Var (name=optLoad) (bindingState=unbound)
   │     └── StatementPattern (resultSizeEstimate=112.1K) [right]
   │           s: Var (name=optLoad) (bindingState=bound)
   │           p: Var (name=_const_3cb27b8c_uri, value=http://example.com/theme/grid/loadValue, anonymous)
   │           o: Var (name=optLoadValue) (bindingState=unbound)
   └── LeftJoin [right]
      ╠══ StatementPattern (resultSizeEstimate=37.5K) [left]
      ║     s: Var (name=line) (bindingState=unbound)
      ║     p: Var (name=_const_342e0de3_uri, value=http://example.com/theme/grid/connectsTo, anonymous)
      ║     o: Var (name=root) (bindingState=bound)
      ╚══ StatementPattern (resultSizeEstimate=9.4K) [right]
            s: Var (name=line) (bindingState=bound)
            p: Var (name=_const_f300a539_uri, value=http://example.com/theme/grid/capacity, anonymous)
            o: Var (name=optLineCapacity) (bindingState=unbound)
```

## ENGINEERING

### Query 0

- Fastest observed: `209.646 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:44)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 1

- Fastest observed: `138.312 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:37)
- Optimized block: [4333](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:4333)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE {
{
VALUES ?target { "REQ-1000" "REQ-1001" }
{
{
?entity a <http://example.com/theme/engineering/Requirement> .
?entity <http://example.com/theme/engineering/name> ?name .
}
}
FILTER ((?name = ?target) || (?name = "REQ-1002"))
}
UNION
{
VALUES ?target { "REQ-1000" "REQ-1001" }
{
{
?entity a <http://example.com/theme/engineering/Component> .
?entity <http://example.com/theme/engineering/name> ?name .
}
}
FILTER ((?name = ?target) || (?name = "REQ-1002"))
}
OPTIONAL {
?entity <http://example.com/theme/engineering/partOf> ?assembly .
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ LeftJoin
│  ║  ├── Union [left]
│  ║  │  ╠══ Filter
│  ║  │  ║  ├── Or
│  ║  │  ║  │  ╠══ Compare (=)
│  ║  │  ║  │  ║     Var (name=name)
│  ║  │  ║  │  ║     Var (name=target)
│  ║  │  ║  │  ╚══ Compare (=)
│  ║  │  ║  │        Var (name=name)
│  ║  │  ║  │        ValueConstant (value="REQ-1002")
│  ║  │  ║  └── Join (HashJoinIteration) (resultSizeEstimate=531)
│  ║  │  ║     ╠══ BindingSetAssignment ([[target="REQ-1000"], [target="REQ-1001"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │  ║     ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=531) [right]
│  ║  │  ║        ├── StatementPattern (costEstimate=1.6K, resultSizeEstimate=520) [left]
│  ║  │  ║        │     s: Var (name=entity)
│  ║  │  ║        │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │  ║        │     o: Var (name=_const_57f1c37d_uri, value=http://example.com/theme/engineering/Requirement, anonymous)
│  ║  │  ║        └── StatementPattern (costEstimate=366, resultSizeEstimate=134.1K) [right]
│  ║  │  ║              s: Var (name=entity)
│  ║  │  ║              p: Var (name=_const_b8416c71_uri, value=http://example.com/theme/engineering/name, anonymous)
│  ║  │  ║              o: Var (name=name)
│  ║  │  ╚══ Filter
│  ║  │     ├── Or
│  ║  │     │  ╠══ Compare (=)
│  ║  │     │  ║     Var (name=name)
│  ║  │     │  ║     Var (name=target)
│  ║  │     │  ╚══ Compare (=)
│  ║  │     │        Var (name=name)
│  ║  │     │        ValueConstant (value="REQ-1002")
│  ║  │     └── Join (HashJoinIteration) (resultSizeEstimate=137.2K)
│  ║  │        ╠══ BindingSetAssignment ([[target="REQ-1000"], [target="REQ-1001"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │        ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=137.2K) [right]
│  ║  │           ├── StatementPattern (costEstimate=104.5M, resultSizeEstimate=132.7K) [left]
│  ║  │           │     s: Var (name=entity)
│  ║  │           │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │           │     o: Var (name=_const_347c8ab7_uri, value=http://example.com/theme/engineering/Component, anonymous)
│  ║  │           └── StatementPattern (costEstimate=366, resultSizeEstimate=134.1K) [right]
│  ║  │                 s: Var (name=entity)
│  ║  │                 p: Var (name=_const_b8416c71_uri, value=http://example.com/theme/engineering/name, anonymous)
│  ║  │                 o: Var (name=name)
│  ║  └── StatementPattern (resultSizeEstimate=132.7K) [right]
│  ║        s: Var (name=entity)
│  ║        p: Var (name=_const_b1044d90_uri, value=http://example.com/theme/engineering/partOf, anonymous)
│  ║        o: Var (name=assembly)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=entity)
└── ExtensionElem (count)
Count (Distinct)
Var (name=entity)
```

### ENGINEERING Query 2

- Fastest observed: `1.069 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:38)
- Optimized block: [2541](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:2541)

Optimized query:

```sparql
SELECT ?assembly (COUNT(DISTINCT ?component) AS ?componentCount) WHERE {
    ?assembly a <http://example.com/theme/engineering/Assembly> .
    ?assembly <http://example.com/theme/engineering/name> ?assemblyName .
    FILTER (?assemblyName IN ("Assembly 1", "Assembly 2", "Assembly 3"))
    OPTIONAL {
        ?component <http://example.com/theme/engineering/partOf> ?assembly .
    }
}
GROUP BY ?assembly
HAVING (COUNT(?component) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 3

- Fastest observed: `115.909 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:39)
- Optimized block: [2594](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:2594)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?requirement) AS ?count) WHERE {
  ?requirement a <http://example.com/theme/engineering/Requirement> .
  ?requirement <http://example.com/theme/engineering/satisfies> ?component .
  OPTIONAL {
    ?requirement <http://example.com/theme/engineering/verifiedBy> ?test .
    BIND(?test AS ?optTest)
  }
  FILTER (?optTest != ?requirement)
  MINUS {
    ?component <http://example.com/theme/engineering/name> ?name .
    FILTER (CONTAINS(STR(?name), "Component 1"))
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 4

- Fastest observed: `47.834 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:40)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 5

- Fastest observed: `1.041 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:41)
- Optimized block: [2706](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:2706)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?measurement) AS ?count) WHERE {
  ?measurement <http://example.com/theme/engineering/measuredValue> ?value .
  FILTER (?value IN (0.9, 0.95))
  ?measurement a <http://example.com/theme/engineering/Measurement> .
  VALUES ?threshold { 0.85 }
  FILTER NOT EXISTS {
    ?measurement <http://example.com/theme/engineering/measuredValue> ?value2 .
    FILTER (?value2 < ?threshold)
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 6

- Fastest observed: `200.862 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:42)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 7

- Fastest observed: `3.597 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:43)
- Optimized block: [2825](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:2825)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?requirement) AS ?count) WHERE {
?requirement a <http://example.com/theme/engineering/Requirement> .
?requirement <http://example.com/theme/engineering/name> ?name .
FILTER ((?name = "REQ-1000") || (?name = "REQ-1001"))
FILTER EXISTS {
?requirement <http://example.com/theme/engineering/satisfies> ?component .
}
MINUS {
?requirement <http://example.com/theme/engineering/verifiedBy> ?test .
?test <http://example.com/theme/engineering/verifiedBy> ?measurement .
}
}
```

Query plan:

_Not present in fastest-run source file._

### Query 8

- Fastest observed: `2.043 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:44)
- Optimized block: [2883](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:2883)

Optimized query:

```sparql
SELECT ?component (COUNT(DISTINCT ?requirement) AS ?reqCount) WHERE {
?requirement <http://example.com/theme/engineering/satisfies> ?component .
?requirement a <http://example.com/theme/engineering/Requirement> .
?component a <http://example.com/theme/engineering/Component> .
?component <http://example.com/theme/engineering/partOf> ?assembly .
OPTIONAL {
?component <http://example.com/theme/engineering/dependsOn> ?dep .
BIND(?dep AS ?optDep)
}
FILTER (?optDep != ?component)
}
GROUP BY ?component
HAVING (COUNT(?requirement) >= 1)
```

Query plan:

_Not present in fastest-run source file._

### Query 9

- Fastest observed: `1.279 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:45)
- Optimized block: [2943](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:2943)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?requirement) AS ?count) WHERE {
  ?measurement <http://example.com/theme/engineering/measuredValue> ?value .
  FILTER (?value IN (0.85, 0.9, 0.95))
  ?test <http://example.com/theme/engineering/verifiedBy> ?measurement .
  ?requirement <http://example.com/theme/engineering/verifiedBy> ?test .
  ?requirement a <http://example.com/theme/engineering/Requirement> .
  VALUES ?threshold { 0.85 }
  OPTIONAL {
    ?component <http://example.com/theme/engineering/name> ?optName .
  }
  FILTER ((?optName != "") && EXISTS { ?requirement <http://example.com/theme/engineering/satisfies> ?component . })
}
```

Query plan:

_Not present in fastest-run source file._

### Query 10

- Fastest observed: `1.578 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:54)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 12

- Fastest observed: `36044.036 ms/op`
- Source: [results-2026-04-18.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:55)
- Optimized block: [6140](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:6140)

Optimized query:

```sparql
SELECT DISTINCT ?root ?optName ?optLabel ?optAssembly ?optAssemblyName ?optDependency ?optDependencyName ?optSatisfiedComponent ?optSatisfiedAssembly ?optVerification ?verificationOwner ?optMeasuredValue WHERE {
  {
    ?root a <http://example.com/theme/engineering/Component> .
  }
  UNION
  {
    ?root a <http://example.com/theme/engineering/Requirement> .
  }
  OPTIONAL {
    ?root <http://example.com/theme/engineering/name> ?optName .
    BIND(?optName AS ?optLabel)
  }
  OPTIONAL {
    ?root <http://example.com/theme/engineering/partOf> ?optAssembly .
    OPTIONAL {
      ?optAssembly <http://example.com/theme/engineering/name> ?optAssemblyName .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/engineering/dependsOn> ?optDependency .
    OPTIONAL {
      ?optDependency <http://example.com/theme/engineering/name> ?optDependencyName .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/engineering/satisfies> ?optSatisfiedComponent .
    OPTIONAL {
      ?optSatisfiedComponent <http://example.com/theme/engineering/partOf> ?optSatisfiedAssembly .
    }
  }
  OPTIONAL {
    {
      ?root <http://example.com/theme/engineering/verifiedBy> ?optVerification .
    }
    UNION
    {
      ?verificationOwner <http://example.com/theme/engineering/verifiedBy> ?optVerification .
      FILTER (?verificationOwner = ?root)
    }
    OPTIONAL {
      ?optVerification <http://example.com/theme/engineering/measuredValue> ?optMeasuredValue .
    }
  }
}
```

Query plan:

```text
Distinct
   Projection
   ├── ProjectionElemList
   │     ProjectionElem "root"
   │     ProjectionElem "optName"
   │     ProjectionElem "optLabel"
   │     ProjectionElem "optAssembly"
   │     ProjectionElem "optAssemblyName"
   │     ProjectionElem "optDependency"
   │     ProjectionElem "optDependencyName"
   │     ProjectionElem "optSatisfiedComponent"
   │     ProjectionElem "optSatisfiedAssembly"
   │     ProjectionElem "optVerification"
   │     ProjectionElem "verificationOwner"
   │     ProjectionElem "optMeasuredValue"
   └── LeftJoin (LeftJoinIterator)
      ╠══ LeftJoin (LeftJoinIterator) [left]
      ║  ├── LeftJoin (LeftJoinIterator) [left]
      ║  │  ╠══ LeftJoin (LeftJoinIterator) [left]
      ║  │  ║  ├── LeftJoin (LeftJoinIterator) [left]
      ║  │  ║  │  ╠══ Union [left]
      ║  │  ║  │  ║  ├── StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=132.7K, indexName=ospc)
      ║  │  ║  │  ║  │     s: Var (name=root) (bindingState=unbound)
      ║  │  ║  │  ║  │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
      ║  │  ║  │  ║  │     o: Var (name=_const_347c8ab7_uri, value=http://example.com/theme/engineering/Component, anonymous)
      ║  │  ║  │  ║  └── StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=520, indexName=ospc)
      ║  │  ║  │  ║        s: Var (name=root) (bindingState=unbound)
      ║  │  ║  │  ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
      ║  │  ║  │  ║        o: Var (name=_const_57f1c37d_uri, value=http://example.com/theme/engineering/Requirement, anonymous)
      ║  │  ║  │  ╚══ Extension [right]
      ║  │  ║  │     ├── StatementPattern (resultSizeEstimate=134.1K)
      ║  │  ║  │     │     s: Var (name=root) (bindingState=bound)
      ║  │  ║  │     │     p: Var (name=_const_b8416c71_uri, value=http://example.com/theme/engineering/name, anonymous)
      ║  │  ║  │     │     o: Var (name=optName) (bindingState=unbound)
      ║  │  ║  │     └── ExtensionElem (optLabel)
      ║  │  ║  │           Var (name=optName) (bindingState=bound)
      ║  │  ║  └── LeftJoin [right]
      ║  │  ║     ╠══ StatementPattern (resultSizeEstimate=132.7K) [left]
      ║  │  ║     ║     s: Var (name=root) (bindingState=bound)
      ║  │  ║     ║     p: Var (name=_const_b1044d90_uri, value=http://example.com/theme/engineering/partOf, anonymous)
      ║  │  ║     ║     o: Var (name=optAssembly) (bindingState=unbound)
      ║  │  ║     ╚══ StatementPattern (resultSizeEstimate=134.1K) [right]
      ║  │  ║           s: Var (name=optAssembly) (bindingState=bound)
      ║  │  ║           p: Var (name=_const_b8416c71_uri, value=http://example.com/theme/engineering/name, anonymous)
      ║  │  ║           o: Var (name=optAssemblyName) (bindingState=unbound)
      ║  │  ╚══ LeftJoin [right]
      ║  │     ├── StatementPattern (resultSizeEstimate=132.7K) [left]
      ║  │     │     s: Var (name=root) (bindingState=bound)
      ║  │     │     p: Var (name=_const_ce5e09a0_uri, value=http://example.com/theme/engineering/dependsOn, anonymous)
      ║  │     │     o: Var (name=optDependency) (bindingState=unbound)
      ║  │     └── StatementPattern (resultSizeEstimate=134.1K) [right]
      ║  │           s: Var (name=optDependency) (bindingState=bound)
      ║  │           p: Var (name=_const_b8416c71_uri, value=http://example.com/theme/engineering/name, anonymous)
      ║  │           o: Var (name=optDependencyName) (bindingState=unbound)
      ║  └── LeftJoin [right]
      ║     ╠══ StatementPattern (resultSizeEstimate=520) [left]
      ║     ║     s: Var (name=root) (bindingState=bound)
      ║     ║     p: Var (name=_const_b98f621b_uri, value=http://example.com/theme/engineering/satisfies, anonymous)
      ║     ║     o: Var (name=optSatisfiedComponent) (bindingState=unbound)
      ║     ╚══ StatementPattern (resultSizeEstimate=132.7K) [right]
      ║           s: Var (name=optSatisfiedComponent) (bindingState=bound)
      ║           p: Var (name=_const_b1044d90_uri, value=http://example.com/theme/engineering/partOf, anonymous)
      ║           o: Var (name=optSatisfiedAssembly) (bindingState=unbound)
      ╚══ LeftJoin [right]
         ├── Union [left]
         │  ╠══ StatementPattern (new scope) (resultSizeEstimate=3.1K)
         │  ║     s: Var (name=root) (bindingState=bound)
         │  ║     p: Var (name=_const_c08202a5_uri, value=http://example.com/theme/engineering/verifiedBy, anonymous)
         │  ║     o: Var (name=optVerification) (bindingState=unbound)
         │  ╚══ Filter (new scope)
         │     ├── Compare (=)
         │     │     Var (name=verificationOwner) (bindingState=bound)
         │     │     Var (name=root) (bindingState=bound)
         │     └── StatementPattern (resultSizeEstimate=3.1K)
         │           s: Var (name=verificationOwner) (bindingState=unbound)
         │           p: Var (name=_const_c08202a5_uri, value=http://example.com/theme/engineering/verifiedBy, anonymous)
         │           o: Var (name=optVerification) (bindingState=unbound)
         └── StatementPattern (resultSizeEstimate=1.6K) [right]
               s: Var (name=optVerification) (bindingState=bound)
               p: Var (name=_const_f682b725_uri, value=http://example.com/theme/engineering/measuredValue, anonymous)
               o: Var (name=optMeasuredValue) (bindingState=unbound)
```

## HIGHLY_CONNECTED

### Query 0

- Fastest observed: `323.587 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:47)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 1

- Fastest observed: `1013.433 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:48)
- Optimized block: [3114](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:3114)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE {
  {
    ?entity a <http://example.com/theme/connected/Node> .
    ?entity <http://example.com/theme/connected/connectsTo> ?targetNode .
    VALUES ?target { 1 2 }
  }
  UNION
  {
    VALUES ?target { 1 2 }
    {
      ?entity a <http://example.com/theme/connected/Node> .
    }
  }
  OPTIONAL {
    ?entity <http://example.com/theme/connected/weight> ?w .
  }
  FILTER ((?w = ?target) || (?w = 3))
}
```

Query plan:

_Not present in fastest-run source file._

### Query 2

- Fastest observed: `494.208 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:49)
- Optimized block: [3178](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:3178)

Optimized query:

```sparql
SELECT ?node (COUNT(DISTINCT ?neighbor) AS ?neighborCount) WHERE {
  ?node <http://example.com/theme/connected/weight> ?w .
  FILTER (?w IN (1, 2, 3))
  ?node a <http://example.com/theme/connected/Node> .
  ?node <http://example.com/theme/connected/connectsTo> ?neighbor .
  OPTIONAL {
    ?neighbor <http://example.com/theme/connected/connectsTo> ?node .
  }
}
GROUP BY ?node
HAVING (COUNT(?neighbor) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 3

- Fastest observed: `100.170 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:50)
- Optimized block: [5689](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:5689)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?node) AS ?count) WHERE {
?node a <http://example.com/theme/connected/Node> .
OPTIONAL {
?node <http://example.com/theme/connected/weight> ?w .
BIND(?w AS ?optWeight)
}
FILTER (?optWeight > 5)
MINUS {
?node <http://example.com/theme/connected/connectsTo> ?neighbor .
FILTER (?neighbor = ?node)
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Difference
│  ║  ├── Filter
│  ║  │  ╠══ Compare (>)
│  ║  │  ║     Var (name=optWeight)
│  ║  │  ║     ValueConstant (value="5"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  │  ╚══ LeftJoin
│  ║  │     ├── StatementPattern (resultSizeEstimate=40.3K) [left]
│  ║  │     │     s: Var (name=node)
│  ║  │     │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │     │     o: Var (name=_const_b000c52_uri, value=http://example.com/theme/connected/Node, anonymous)
│  ║  │     └── Extension [right]
│  ║  │        ╠══ StatementPattern (resultSizeEstimate=222.8K)
│  ║  │        ║     s: Var (name=node)
│  ║  │        ║     p: Var (name=_const_909a60a8_uri, value=http://example.com/theme/connected/weight, anonymous)
│  ║  │        ║     o: Var (name=w)
│  ║  │        ╚══ ExtensionElem (optWeight)
│  ║  │              Var (name=w)
│  ║  └── Filter (new scope)
│  ║     ╠══ Compare (=)
│  ║     ║     Var (name=neighbor)
│  ║     ║     Var (name=node)
│  ║     ╚══ StatementPattern (resultSizeEstimate=267.3K)
│  ║           s: Var (name=node)
│  ║           p: Var (name=_const_2e732754_uri, value=http://example.com/theme/connected/connectsTo, anonymous)
│  ║           o: Var (name=neighbor)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=node)
└── ExtensionElem (count)
Count (Distinct)
Var (name=node)
```

### Query 4

- Fastest observed: `213.867 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:51)
- Optimized block: [3290](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:3290)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?node) AS ?count) WHERE {
?node a <http://example.com/theme/connected/Node> .
?node <http://example.com/theme/connected/weight> ?w .
FILTER ((?w = 1) || (?w = 2))
OPTIONAL {
?neighbor <http://example.com/theme/connected/connectsTo> ?node .
}
FILTER EXISTS {
?node <http://example.com/theme/connected/connectsTo> ?neighbor .
}
}
```

Query plan:

_Not present in fastest-run source file._

### Query 5

- Fastest observed: `110.966 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:52)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 6

- Fastest observed: `1234.693 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:53)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 7

- Fastest observed: `113.267 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:54)
- Optimized block: [6085](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:6085)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?node) AS ?count) WHERE {
?node a <http://example.com/theme/connected/Node> .
?node <http://example.com/theme/connected/weight> ?w .
FILTER ((?w = 8) || (?w = 9))
FILTER EXISTS {
?node <http://example.com/theme/connected/connectsTo> ?neighbor .
}
MINUS {
?neighbor <http://example.com/theme/connected/connectsTo> ?node .
FILTER (?neighbor = ?node)
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Difference
│  ║  ├── Filter
│  ║  │  ╠══ Exists
│  ║  │  ║     StatementPattern (resultSizeEstimate=267.3K)
│  ║  │  ║        s: Var (name=node)
│  ║  │  ║        p: Var (name=_const_2e732754_uri, value=http://example.com/theme/connected/connectsTo, anonymous)
│  ║  │  ║        o: Var (name=neighbor)
│  ║  │  ╚══ Join (JoinIterator) (resultSizeEstimate=222.8K)
│  ║  │     ├── StatementPattern (costEstimate=20.1K, resultSizeEstimate=40.3K) [left]
│  ║  │     │     s: Var (name=node)
│  ║  │     │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │     │     o: Var (name=_const_b000c52_uri, value=http://example.com/theme/connected/Node, anonymous)
│  ║  │     └── Filter (costEstimate=473, resultSizeEstimate=223.9K) [right]
│  ║  │        ╠══ Or
│  ║  │        ║  ├── Compare (=)
│  ║  │        ║  │     Var (name=w)
│  ║  │        ║  │     ValueConstant (value="8"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  │        ║  └── Compare (=)
│  ║  │        ║        Var (name=w)
│  ║  │        ║        ValueConstant (value="9"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  │        ╚══ StatementPattern (resultSizeEstimate=222.8K)
│  ║  │              s: Var (name=node)
│  ║  │              p: Var (name=_const_909a60a8_uri, value=http://example.com/theme/connected/weight, anonymous)
│  ║  │              o: Var (name=w)
│  ║  └── Filter (new scope)
│  ║     ╠══ Compare (=)
│  ║     ║     Var (name=neighbor)
│  ║     ║     Var (name=node)
│  ║     ╚══ StatementPattern (resultSizeEstimate=267.3K)
│  ║           s: Var (name=neighbor)
│  ║           p: Var (name=_const_2e732754_uri, value=http://example.com/theme/connected/connectsTo, anonymous)
│  ║           o: Var (name=node)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=node)
└── ExtensionElem (count)
Count (Distinct)
Var (name=node)
```

### Query 8

- Fastest observed: `1034.976 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:55)
- Optimized block: [6186](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:6186)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?node) AS ?count) WHERE {
?node a <http://example.com/theme/connected/Node> .
?node <http://example.com/theme/connected/connectsTo> ?mid .
?mid <http://example.com/theme/connected/connectsTo> ?end .
FILTER EXISTS {
?end <http://example.com/theme/connected/connectsTo> ?node .
}
OPTIONAL {
?node <http://example.com/theme/connected/weight> ?optWeight .
}
FILTER (?optWeight IN (7, 8, 9))
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Filter
│  ║  ├── ListMemberOperator
│  ║  │     Var (name=optWeight)
│  ║  │     ValueConstant (value="7"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  │     ValueConstant (value="8"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  │     ValueConstant (value="9"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  └── LeftJoin
│  ║     ╠══ Filter [left]
│  ║     ║  ├── Exists
│  ║     ║  │     StatementPattern (resultSizeEstimate=267.3K)
│  ║     ║  │        s: Var (name=end)
│  ║     ║  │        p: Var (name=_const_2e732754_uri, value=http://example.com/theme/connected/connectsTo, anonymous)
│  ║     ║  │        o: Var (name=node)
│  ║     ║  └── Join (JoinIterator) (resultSizeEstimate=95.7K)
│  ║     ║     ╠══ Join (JoinIterator) (resultSizeEstimate=98.2K) [left]
│  ║     ║     ║  ├── StatementPattern (costEstimate=20.1K, resultSizeEstimate=40.3K) [left]
│  ║     ║     ║  │     s: Var (name=node)
│  ║     ║     ║  │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║     ║     ║  │     o: Var (name=_const_b000c52_uri, value=http://example.com/theme/connected/Node, anonymous)
│  ║     ║     ║  └── StatementPattern (costEstimate=259, resultSizeEstimate=267.3K) [right]
│  ║     ║     ║        s: Var (name=node)
│  ║     ║     ║        p: Var (name=_const_2e732754_uri, value=http://example.com/theme/connected/connectsTo, anonymous)
│  ║     ║     ║        o: Var (name=mid)
│  ║     ║     ╚══ StatementPattern (costEstimate=517, resultSizeEstimate=267.3K) [right]
│  ║     ║           s: Var (name=mid)
│  ║     ║           p: Var (name=_const_2e732754_uri, value=http://example.com/theme/connected/connectsTo, anonymous)
│  ║     ║           o: Var (name=end)
│  ║     ╚══ StatementPattern (resultSizeEstimate=222.8K) [right]
│  ║           s: Var (name=node)
│  ║           p: Var (name=_const_909a60a8_uri, value=http://example.com/theme/connected/weight, anonymous)
│  ║           o: Var (name=optWeight)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=node)
└── ExtensionElem (count)
Count (Distinct)
Var (name=node)
```

### Query 9

- Fastest observed: `1202.971 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:56)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 10

- Fastest observed: `236.960 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:57)
- Optimized block: [3635](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:3635)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?node) AS ?count) WHERE {
?node a <http://example.com/theme/connected/Node> .
?node <http://example.com/theme/connected/weight> ?w .
FILTER (?w IN (1, 2, 3, 4))
VALUES ?threshold { 3 }
FILTER NOT EXISTS {
?node <http://example.com/theme/connected/connectsTo> ?n2 .
?n2 <http://example.com/theme/connected/weight> ?w2 .
FILTER (?w2 < ?threshold)
}
MINUS {
?node <http://example.com/theme/connected/connectsTo> ?node .
BIND(?node AS ?_anon_path_60a60982f2b264b4697fd6b55a18dbd26012345)
}
}
```

Query plan:

_Not present in fastest-run source file._

## LIBRARY

### Query 0

- Fastest observed: `634.970 ms/op`
- Source: [results-2026-04-16-2.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-2.md:25)
- Optimized block: [1749](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-2.md:1749)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?book) AS ?count) WHERE {
  ?book a <http://example.com/theme/library/Book> .
  OPTIONAL {
    ?book <http://example.com/theme/library/hasCopy> ?copy .
    ?copy <http://example.com/theme/library/locatedAt> ?branch .
    BIND(?branch AS ?optBranch)
  }
  FILTER (?optBranch != ?book)
  OPTIONAL {
    ?book <http://example.com/theme/library/writtenBy> ?author .
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 1

- Fastest observed: `143.142 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:26)
- Optimized block: [3105](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:3105)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE {
{
VALUES ?target { "Member 1" "Member 2" }
{
{
?entity a <http://example.com/theme/library/Member> .
?entity <http://example.com/theme/library/name> ?name .
}
}
FILTER ((?name = ?target) || (?name = "Member 3"))
}
UNION
{
VALUES ?target { "Member 1" "Member 2" }
{
{
?entity a <http://example.com/theme/library/Book> .
?entity <http://example.com/theme/library/title> ?name .
}
}
FILTER ((?name = ?target) || (?name = "Member 3"))
}
OPTIONAL {
?entity <http://example.com/theme/library/hasCopy> ?copy .
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ LeftJoin
│  ║  ├── Union [left]
│  ║  │  ╠══ Filter
│  ║  │  ║  ├── Or
│  ║  │  ║  │  ╠══ Compare (=)
│  ║  │  ║  │  ║     Var (name=name)
│  ║  │  ║  │  ║     Var (name=target)
│  ║  │  ║  │  ╚══ Compare (=)
│  ║  │  ║  │        Var (name=name)
│  ║  │  ║  │        ValueConstant (value="Member 3")
│  ║  │  ║  └── Join (HashJoinIteration) (resultSizeEstimate=4.7K)
│  ║  │  ║     ╠══ BindingSetAssignment ([[target="Member 1"], [target="Member 2"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │  ║     ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=4.7K) [right]
│  ║  │  ║        ├── StatementPattern (costEstimate=15.3K, resultSizeEstimate=5.1K) [left]
│  ║  │  ║        │     s: Var (name=entity)
│  ║  │  ║        │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │  ║        │     o: Var (name=_const_f5728978_uri, value=http://example.com/theme/library/Member, anonymous)
│  ║  │  ║        └── StatementPattern (costEstimate=213, resultSizeEstimate=45.3K) [right]
│  ║  │  ║              s: Var (name=entity)
│  ║  │  ║              p: Var (name=_const_6d0024c9_uri, value=http://example.com/theme/library/name, anonymous)
│  ║  │  ║              o: Var (name=name)
│  ║  │  ╚══ Filter
│  ║  │     ├── Or
│  ║  │     │  ╠══ Compare (=)
│  ║  │     │  ║     Var (name=name)
│  ║  │     │  ║     Var (name=target)
│  ║  │     │  ╚══ Compare (=)
│  ║  │     │        Var (name=name)
│  ║  │     │        ValueConstant (value="Member 3")
│  ║  │     └── Join (HashJoinIteration) (resultSizeEstimate=133.3K)
│  ║  │        ╠══ BindingSetAssignment ([[target="Member 1"], [target="Member 2"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │        ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=133.3K) [right]
│  ║  │           ├── StatementPattern (costEstimate=983.1M, resultSizeEstimate=128.9K) [left]
│  ║  │           │     s: Var (name=entity)
│  ║  │           │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │           │     o: Var (name=_const_6cec5947_uri, value=http://example.com/theme/library/Book, anonymous)
│  ║  │           └── StatementPattern (costEstimate=359, resultSizeEstimate=128.9K) [right]
│  ║  │                 s: Var (name=entity)
│  ║  │                 p: Var (name=_const_335cbfda_uri, value=http://example.com/theme/library/title, anonymous)
│  ║  │                 o: Var (name=name)
│  ║  └── StatementPattern (resultSizeEstimate=386.3K) [right]
│  ║        s: Var (name=entity)
│  ║        p: Var (name=_const_469a1e31_uri, value=http://example.com/theme/library/hasCopy, anonymous)
│  ║        o: Var (name=copy)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=entity)
└── ExtensionElem (count)
Count (Distinct)
Var (name=entity)
```

### Query 2

- Fastest observed: `0.126 ms/op`
- Source: [results-2026-04-22.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-22.md)
- Optimized block: present

Optimized query:

```sparql
SELECT ?author (COUNT(DISTINCT ?book) AS ?bookCount) WHERE {
  VALUES ?authorName { "Author 1" "Author 2" "Author 3" }
  ?author <http://example.com/theme/library/name> ?authorName .
  FILTER (?authorName IN ("Author 1", "Author 2", "Author 3"))
  ?author a <http://example.com/theme/library/Author> .
  OPTIONAL {
    ?book <http://example.com/theme/library/writtenBy> ?author .
  }
}
GROUP BY ?author
HAVING (COUNT(?book) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 3

- Fastest observed: `40.574 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:28)
- Optimized block: [3335](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:3335)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?loan) AS ?count) WHERE {
?loan a <http://example.com/theme/library/Loan> .
?loan <http://example.com/theme/library/borrowedBy> ?member .
OPTIONAL {
?loan <http://example.com/theme/library/dueDate> ?due .
BIND(?due AS ?optDue)
}
FILTER (?optDue > "2024-01-10"^^<http://www.w3.org/2001/XMLSchema#date>)
MINUS {
?member <http://example.com/theme/library/name> ?name .
FILTER (CONTAINS(LCASE(STR(?name)), "member 1"))
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Difference
│  ║  ├── Filter
│  ║  │  ╠══ Compare (>)
│  ║  │  ║     Var (name=optDue)
│  ║  │  ║     ValueConstant (value="2024-01-10"^^<http://www.w3.org/2001/XMLSchema#date>)
│  ║  │  ╚══ LeftJoin
│  ║  │     ├── Join (JoinIterator) (resultSizeEstimate=10.2K) [left]
│  ║  │     │  ╠══ StatementPattern (costEstimate=5.1K, resultSizeEstimate=10.2K) [left]
│  ║  │     │  ║     s: Var (name=loan)
│  ║  │     │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │     │  ║     o: Var (name=_const_6cf0e34e_uri, value=http://example.com/theme/library/Loan, anonymous)
│  ║  │     │  ╚══ StatementPattern (costEstimate=101, resultSizeEstimate=10.2K) [right]
│  ║  │     │        s: Var (name=loan)
│  ║  │     │        p: Var (name=_const_b9a39489_uri, value=http://example.com/theme/library/borrowedBy, anonymous)
│  ║  │     │        o: Var (name=member)
│  ║  │     └── Extension [right]
│  ║  │        ╠══ StatementPattern (resultSizeEstimate=10.2K)
│  ║  │        ║     s: Var (name=loan)
│  ║  │        ║     p: Var (name=_const_945d14c4_uri, value=http://example.com/theme/library/dueDate, anonymous)
│  ║  │        ║     o: Var (name=due)
│  ║  │        ╚══ ExtensionElem (optDue)
│  ║  │              Var (name=due)
│  ║  └── Filter (new scope)
│  ║     ╠══ FunctionCall (http://www.w3.org/2005/xpath-functions#contains)
│  ║     ║  ├── FunctionCall (http://www.w3.org/2005/xpath-functions#lower-case)
│  ║     ║  │     Str
│  ║     ║  │        Var (name=name)
│  ║     ║  └── ValueConstant (value="member 1")
│  ║     ╚══ StatementPattern (resultSizeEstimate=45.3K)
│  ║           s: Var (name=member)
│  ║           p: Var (name=_const_6d0024c9_uri, value=http://example.com/theme/library/name, anonymous)
│  ║           o: Var (name=name)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=loan)
└── ExtensionElem (count)
Count (Distinct)
Var (name=loan)
```

### Query 4

- Fastest observed: `45.475 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:29)
- Optimized block: [1987](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:1987)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?book) AS ?count) WHERE {
  ?book <http://example.com/theme/library/title> ?title .
  FILTER ((?title = "Book 1") || (?title = "Book 2"))
  ?book a <http://example.com/theme/library/Book> .
  OPTIONAL {
    ?book <http://example.com/theme/library/writtenBy> ?author .
  }
  FILTER EXISTS {
    ?book <http://example.com/theme/library/hasCopy> ?copy .
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 5

- Fastest observed: `4.758 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:30)
- Optimized block: [2043](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:2043)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?loan) AS ?count) WHERE {
  ?loan <http://example.com/theme/library/loanDate> ?loanDate .
  FILTER (?loanDate IN ("2024-01-01"^^<http://www.w3.org/2001/XMLSchema#date>, "2024-01-02"^^<http://www.w3.org/2001/XMLSchema#date>))
  ?loan a <http://example.com/theme/library/Loan> .
  VALUES ?threshold { "2024-01-01"^^<http://www.w3.org/2001/XMLSchema#date> }
  FILTER NOT EXISTS {
    ?loan <http://example.com/theme/library/dueDate> ?due .
    FILTER (?due < ?threshold)
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 6

- Fastest observed: `24599.399 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:31)
- Optimized block: [3633](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:3633)

Optimized query:

```sparql
SELECT ?member (COUNT(DISTINCT ?loan) AS ?loanCount) WHERE {
{
?loan a <http://example.com/theme/library/Loan> .
?loan <http://example.com/theme/library/borrowedBy> ?member .
}
UNION
{
?member a <http://example.com/theme/library/Member> .
}
OPTIONAL {
?loan <http://example.com/theme/library/loanedCopy> ?copy .
BIND(?copy AS ?optCopy)
}
FILTER (?optCopy != ?member)
}
GROUP BY ?member
HAVING (COUNT(?loan) > 0)
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "member"
║     ProjectionElem "loanCount"
╚══ Extension
├── Extension
│  ╠══ Filter
│  ║  ├── Compare (>)
│  ║  │     Var (name=_anon_having_5144580a306a764e1c98521b839d529baf01234, anonymous)
│  ║  │     ValueConstant (value="0"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  └── Group (member)
│  ║        Filter
│  ║        ├── Compare (!=)
│  ║        │     Var (name=optCopy)
│  ║        │     Var (name=member)
│  ║        └── LeftJoin
│  ║           ╠══ Union [left]
│  ║           ║  ├── Join (new scope) (JoinIterator) (resultSizeEstimate=10.2K)
│  ║           ║  │  ╠══ StatementPattern (costEstimate=5.1K, resultSizeEstimate=10.2K) [left]
│  ║           ║  │  ║     s: Var (name=loan)
│  ║           ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║           ║  │  ║     o: Var (name=_const_6cf0e34e_uri, value=http://example.com/theme/library/Loan, anonymous)
│  ║           ║  │  ╚══ StatementPattern (costEstimate=101, resultSizeEstimate=10.2K) [right]
│  ║           ║  │        s: Var (name=loan)
│  ║           ║  │        p: Var (name=_const_b9a39489_uri, value=http://example.com/theme/library/borrowedBy, anonymous)
│  ║           ║  │        o: Var (name=member)
│  ║           ║  └── StatementPattern (new scope) (resultSizeEstimate=5.1K)
│  ║           ║        s: Var (name=member)
│  ║           ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║           ║        o: Var (name=_const_f5728978_uri, value=http://example.com/theme/library/Member, anonymous)
│  ║           ╚══ Extension [right]
│  ║              ├── StatementPattern (resultSizeEstimate=10.2K)
│  ║              │     s: Var (name=loan)
│  ║              │     p: Var (name=_const_78c99d62_uri, value=http://example.com/theme/library/loanedCopy, anonymous)
│  ║              │     o: Var (name=copy)
│  ║              └── ExtensionElem (optCopy)
│  ║                    Var (name=copy)
│  ║        GroupElem (_anon_having_5144580a306a764e1c98521b839d529baf01234)
│  ║           Count
│  ║              Var (name=loan)
│  ║        GroupElem (loanCount)
│  ║           Count (Distinct)
│  ║              Var (name=loan)
│  ╚══ ExtensionElem (_anon_having_5144580a306a764e1c98521b839d529baf01234)
│        Count
│           Var (name=loan)
└── ExtensionElem (loanCount)
Count (Distinct)
Var (name=loan)
```

### Library Query 7

- Fastest observed: `408.237 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:32)
- Optimized block: [2166](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:2166)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?copy) AS ?count) WHERE {
  ?branch <http://example.com/theme/library/name> ?branchName .
  FILTER ((?branchName = "Branch 0") || (?branchName = "Branch 1"))
  ?copy <http://example.com/theme/library/locatedAt> ?branch .
  FILTER EXISTS {
    ?copy a <http://example.com/theme/library/Copy> .
  }
  ?copy a <http://example.com/theme/library/Copy> .
  MINUS {
    ?copy <http://example.com/theme/library/locatedAt> ?branch .
    FILTER (CONTAINS(STR(?branch), "branch/0"))
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 8

- Fastest observed: `64.859 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:33)
- Optimized block: [2228](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:2228)

Optimized query:

```sparql
SELECT ?author (COUNT(DISTINCT ?loan) AS ?loanCount) WHERE {
  ?loan <http://example.com/theme/library/loanedCopy> ?copy .
  ?loan a <http://example.com/theme/library/Loan> .
  ?loan <http://example.com/theme/library/borrowedBy> ?member .
  ?book <http://example.com/theme/library/hasCopy> ?copy .
  ?copy <http://example.com/theme/library/locatedAt> ?branch .
  ?book a <http://example.com/theme/library/Book> .
  ?book <http://example.com/theme/library/writtenBy> ?author .
  OPTIONAL {
    ?member <http://example.com/theme/library/name> ?optName .
  }
  FILTER (?optName IN ("Member 1", "Member 2", "Member 3"))
}
GROUP BY ?author
HAVING (COUNT(?loan) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 9

- Fastest observed: `67.634 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:34)
- Optimized block: [2294](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:2294)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?member) AS ?count) WHERE {
  ?loan <http://example.com/theme/library/borrowedBy> ?member .
  ?member a <http://example.com/theme/library/Member> .
  ?loan a <http://example.com/theme/library/Loan> .
  ?loan <http://example.com/theme/library/loanedCopy> ?copy .
  ?book <http://example.com/theme/library/hasCopy> ?copy .
  ?book <http://example.com/theme/library/writtenBy> ?author .
  ?author <http://example.com/theme/library/name> ?authorName .
  VALUES ?target { "Author 1" "Author 2" }
  FILTER ((?authorName = ?target) || (?authorName = "Author 3"))
  OPTIONAL {
    ?book <http://example.com/theme/library/title> ?optTitle .
  }
  FILTER ((?optTitle != "") && NOT EXISTS { ?loan <http://example.com/theme/library/dueDate> ?due . FILTER (?due < "2024-01-10"^^<http://www.w3.org/2001/XMLSchema#date>) })
}
```

Query plan:

_Not present in fastest-run source file._

### Query 10

- Fastest observed: `178.230 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:35)
- Optimized block: [2358](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:2358)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?branch) AS ?count) WHERE {
{
?branch a <http://example.com/theme/library/Branch> .
}
UNION
{
?branch a <http://example.com/theme/library/Branch> .
?branch <http://example.com/theme/library/name> ?name .
}
OPTIONAL {
?copy <http://example.com/theme/library/locatedAt> ?branch .
BIND(?copy AS ?optCopy)
}
FILTER (?optCopy != ?branch)
MINUS {
?branch <http://example.com/theme/library/name> ?name2 .
FILTER (CONTAINS(LCASE(STR(?name2)), "branch 0"))
}
}
```

Query plan:

_Not present in fastest-run source file._

## MEDICAL_RECORDS

### Query 0

- Fastest observed: `44.359 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:3)
- Optimized block: [134](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:134)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?patient) AS ?count) WHERE {
  ?patient a <http://example.com/theme/medical/Patient> .
  OPTIONAL {
    ?patient <http://example.com/theme/medical/hasEncounter> ?enc .
    ?enc <http://example.com/theme/medical/recordedOn> ?date .
    BIND(?date AS ?optDate)
  }
  FILTER (?optDate >= "2024-06-01"^^<http://www.w3.org/2001/XMLSchema#date>)
  OPTIONAL {
    ?patient <http://example.com/theme/medical/hasMedication> ?med .
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 1

- Fastest observed: `99.712 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:4)
- Optimized block: [355](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:355)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE {
{
VALUES ?target { "DX-200" "DX-201" }
{
{
?entity a <http://example.com/theme/medical/Condition> .
?entity <http://example.com/theme/medical/code> ?code .
}
}
FILTER ((?code = ?target) || (?code = "DX-202"))
}
UNION
{
VALUES ?target { "DX-200" "DX-201" }
{
{
?entity a <http://example.com/theme/medical/Medication> .
?entity <http://example.com/theme/medical/code> ?code .
}
}
FILTER ((?code = ?target) || (?code = "DX-202"))
}
OPTIONAL {
?entity <http://example.com/theme/medical/code> ?alt .
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ LeftJoin
│  ║  ├── Union [left]
│  ║  │  ╠══ Filter
│  ║  │  ║  ├── Or
│  ║  │  ║  │  ╠══ Compare (=)
│  ║  │  ║  │  ║     Var (name=code)
│  ║  │  ║  │  ║     Var (name=target)
│  ║  │  ║  │  ╚══ Compare (=)
│  ║  │  ║  │        Var (name=code)
│  ║  │  ║  │        ValueConstant (value="DX-202")
│  ║  │  ║  └── Join (HashJoinIteration) (resultSizeEstimate=50.4K)
│  ║  │  ║     ╠══ BindingSetAssignment ([[target="DX-200"], [target="DX-201"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │  ║     ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=50.4K) [right]
│  ║  │  ║        ├── StatementPattern (costEstimate=149.5K, resultSizeEstimate=49.8K) [left]
│  ║  │  ║        │     s: Var (name=entity)
│  ║  │  ║        │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │  ║        │     o: Var (name=_const_d05fbbd3_uri, value=http://example.com/theme/medical/Condition, anonymous)
│  ║  │  ║        └── StatementPattern (costEstimate=258, resultSizeEstimate=66.5K) [right]
│  ║  │  ║              s: Var (name=entity)
│  ║  │  ║              p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
│  ║  │  ║              o: Var (name=code)
│  ║  │  ╚══ Filter
│  ║  │     ├── Or
│  ║  │     │  ╠══ Compare (=)
│  ║  │     │  ║     Var (name=code)
│  ║  │     │  ║     Var (name=target)
│  ║  │     │  ╚══ Compare (=)
│  ║  │     │        Var (name=code)
│  ║  │     │        ValueConstant (value="DX-202")
│  ║  │     └── Join (HashJoinIteration) (resultSizeEstimate=16.3K)
│  ║  │        ╠══ BindingSetAssignment ([[target="DX-200"], [target="DX-201"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │        ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=16.3K) [right]
│  ║  │           ├── StatementPattern (costEstimate=1248.3M, resultSizeEstimate=16.7K) [left]
│  ║  │           │     s: Var (name=entity)
│  ║  │           │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │           │     o: Var (name=_const_ea395317_uri, value=http://example.com/theme/medical/Medication, anonymous)
│  ║  │           └── StatementPattern (costEstimate=258, resultSizeEstimate=66.5K) [right]
│  ║  │                 s: Var (name=entity)
│  ║  │                 p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
│  ║  │                 o: Var (name=code)
│  ║  └── StatementPattern (resultSizeEstimate=66.5K) [right]
│  ║        s: Var (name=entity)
│  ║        p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
│  ║        o: Var (name=alt)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=entity)
└── ExtensionElem (count)
Count (Distinct)
Var (name=entity)
```

### Query 2

- Fastest observed: `14.487 ms/op`
- Source: [results-2026-04-09-2-full.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:5648)
- Optimized block: [188](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:188)

Optimized query:

```sparql
SELECT ?practitioner (COUNT(DISTINCT ?enc) AS ?encCount) WHERE {
?enc <http://example.com/theme/medical/recordedOn> ?date .
FILTER (?date IN ("2024-01-01"^^<http://www.w3.org/2001/XMLSchema#date>, "2024-02-01"^^<http://www.w3.org/2001/XMLSchema#date>))
?enc a <http://example.com/theme/medical/Encounter> .
?enc <http://example.com/theme/medical/handledBy> ?practitioner .
OPTIONAL {
?enc <http://example.com/theme/medical/hasCondition> ?cond .
}
}
GROUP BY ?practitioner
HAVING (COUNT(?enc) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 3

- Fastest observed: `76.840 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:6)
- Optimized block: [317](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:317)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?patient) AS ?count) WHERE {
?patient a <http://example.com/theme/medical/Patient> .
OPTIONAL {
?patient <http://example.com/theme/medical/hasEncounter>/<http://example.com/theme/medical/hasObservation> ?obs .
?obs <http://example.com/theme/medical/value> ?value .
BIND(?value AS ?optValue)
}
FILTER (?optValue > 60)
MINUS {
?patient <http://example.com/theme/medical/name> ?name .
FILTER (CONTAINS(LCASE(STR(?name)), "test"))
}
}
```

Query plan:

_Not present in fastest-run source file._

### Query 4

- Fastest observed: `103.994 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:7)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 5

- Fastest observed: `22.715 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:8)
- Optimized block: [803](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:803)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?patient) AS ?count) WHERE {
?obs <http://example.com/theme/medical/value> ?value .
FILTER (?value IN (50, 60, 70))
?enc <http://example.com/theme/medical/hasObservation> ?obs .
?patient <http://example.com/theme/medical/hasEncounter> ?enc .
?patient a <http://example.com/theme/medical/Patient> .
VALUES ?limit { 55 }
FILTER NOT EXISTS {
?enc <http://example.com/theme/medical/hasCondition> ?cond .
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Filter
│  ║  ├── Not
│  ║  │     Exists
│  ║  │        StatementPattern (resultSizeEstimate=49.8K)
│  ║  │           s: Var (name=enc)
│  ║  │           p: Var (name=_const_7e7389c9_uri, value=http://example.com/theme/medical/hasCondition, anonymous)
│  ║  │           o: Var (name=cond)
│  ║  └── Join (JoinIterator) (resultSizeEstimate=25.4K)
│  ║     ╠══ Join (JoinIterator) (resultSizeEstimate=25.4K) [left]
│  ║     ║  ├── Join (JoinIterator) (resultSizeEstimate=25.7K) [left]
│  ║     ║  │  ╠══ Join (JoinIterator) (resultSizeEstimate=3.1K) [left]
│  ║     ║  │  ║  ├── Filter (costEstimate=9.0K, resultSizeEstimate=3.0K) [left]
│  ║     ║  │  ║  │  ╠══ ListMemberOperator
│  ║     ║  │  ║  │  ║     Var (name=value)
│  ║     ║  │  ║  │  ║     ValueConstant (value="50"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║     ║  │  ║  │  ║     ValueConstant (value="60"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║     ║  │  ║  │  ║     ValueConstant (value="70"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║     ║  │  ║  │  ╚══ StatementPattern (resultSizeEstimate=49.7K)
│  ║     ║  │  ║  │        s: Var (name=obs)
│  ║     ║  │  ║  │        p: Var (name=_const_2949ec49_uri, value=http://example.com/theme/medical/value, anonymous)
│  ║     ║  │  ║  │        o: Var (name=value)
│  ║     ║  │  ║  └── StatementPattern (costEstimate=111, resultSizeEstimate=49.7K) [right]
│  ║     ║  │  ║        s: Var (name=enc)
│  ║     ║  │  ║        p: Var (name=_const_6f00815a_uri, value=http://example.com/theme/medical/hasObservation, anonymous)
│  ║     ║  │  ║        o: Var (name=obs)
│  ║     ║  │  ╚══ StatementPattern (costEstimate=79, resultSizeEstimate=25.0K) [right]
│  ║     ║  │        s: Var (name=patient)
│  ║     ║  │        p: Var (name=_const_ca285e1_uri, value=http://example.com/theme/medical/hasEncounter, anonymous)
│  ║     ║  │        o: Var (name=enc)
│  ║     ║  └── StatementPattern (costEstimate=1.00, resultSizeEstimate=8.3K) [right]
│  ║     ║        s: Var (name=patient)
│  ║     ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║     ║        o: Var (name=_const_24be87bd_uri, value=http://example.com/theme/medical/Patient, anonymous)
│  ║     ╚══ BindingSetAssignment ([[limit="55"^^<http://www.w3.org/2001/XMLSchema#integer>]]) (costEstimate=6.00, resultSizeEstimate=1.00) [right]
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=patient)
└── ExtensionElem (count)
Count (Distinct)
Var (name=patient)
```

### Query 6

- Fastest observed: `53.192 ms/op`
- Source: [results-2026-04-16-6.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-6.md:9)
- Optimized block: [780](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-6.md:780)

Optimized query:

```sparql
SELECT ?patient (COUNT(DISTINCT ?med) AS ?medCount) WHERE {
  {
    ?patient a <http://example.com/theme/medical/Patient> .
  }
  UNION
  {
    ?patient <http://example.com/theme/medical/hasEncounter> ?enc .
  }
  OPTIONAL {
    ?patient <http://example.com/theme/medical/hasMedication> ?med .
    BIND(?med AS ?optMed)
  }
  FILTER (?optMed != ?patient)
}
GROUP BY ?patient
HAVING (COUNT(?med) > 0)
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "patient"
║     ProjectionElem "medCount"
╚══ Extension
   ├── Extension
   │  ╠══ Filter
   │  ║  ├── Compare (>)
   │  ║  │     Var (name=_anon_having_0892072562930147436390697a179857743a, anonymous)
   │  ║  │     ValueConstant (value="0"^^<http://www.w3.org/2001/XMLSchema#integer>)
   │  ║  └── Group (patient)
   │  ║        Filter
   │  ║        ├── Compare (!=)
   │  ║        │     Var (name=optMed)
   │  ║        │     Var (name=patient)
   │  ║        └── LeftJoin
   │  ║           ╠══ Union [left]
   │  ║           ║  ├── StatementPattern (new scope) (resultSizeEstimate=8.3K)
   │  ║           ║  │     s: Var (name=patient)
   │  ║           ║  │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║           ║  │     o: Var (name=_const_24be87bd_uri, value=http://example.com/theme/medical/Patient, anonymous)
   │  ║           ║  └── StatementPattern (new scope) (resultSizeEstimate=25.0K)
   │  ║           ║        s: Var (name=patient)
   │  ║           ║        p: Var (name=_const_ca285e1_uri, value=http://example.com/theme/medical/hasEncounter, anonymous)
   │  ║           ║        o: Var (name=enc)
   │  ║           ╚══ Extension [right]
   │  ║              ├── StatementPattern (resultSizeEstimate=16.7K)
   │  ║              │     s: Var (name=patient)
   │  ║              │     p: Var (name=_const_fe9f43e1_uri, value=http://example.com/theme/medical/hasMedication, anonymous)
   │  ║              │     o: Var (name=med)
   │  ║              └── ExtensionElem (optMed)
   │  ║                    Var (name=med)
   │  ║        GroupElem (_anon_having_0892072562930147436390697a179857743a)
   │  ║           Count
   │  ║              Var (name=med)
   │  ║        GroupElem (medCount)
   │  ║           Count (Distinct)
   │  ║              Var (name=med)
   │  ╚══ ExtensionElem (_anon_having_0892072562930147436390697a179857743a)
   │        Count
   │           Var (name=med)
   └── ExtensionElem (medCount)
         Count (Distinct)
            Var (name=med)
```

### Query 7

- Fastest observed: `41.131 ms/op`
- Source: [results-2026-04-16-5.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-5.md:10)
- Optimized block: [886](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-5.md:886)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?med) AS ?count) WHERE {
  ?med a <http://example.com/theme/medical/Medication> .
  ?med <http://example.com/theme/medical/code> ?code .
  FILTER ((?code = "MED-1000") || (?code = "MED-1001"))
  FILTER EXISTS {
    ?patient <http://example.com/theme/medical/hasMedication> ?med .
  }
  MINUS {
    ?med <http://example.com/theme/medical/dosage> ?dose .
    FILTER (CONTAINS(LCASE(STR(?dose)), "x"))
  }
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
   ├── Group ()
   │  ╠══ Difference
   │  ║  ├── Filter
   │  ║  │  ╠══ Exists
   │  ║  │  ║     StatementPattern (resultSizeEstimate=16.7K)
   │  ║  │  ║        s: Var (name=patient)
   │  ║  │  ║        p: Var (name=_const_fe9f43e1_uri, value=http://example.com/theme/medical/hasMedication, anonymous)
   │  ║  │  ║        o: Var (name=med)
   │  ║  │  ╚══ Join (JoinIterator) (resultSizeEstimate=16.3K)
   │  ║  │     ├── StatementPattern (costEstimate=8.3K, resultSizeEstimate=16.7K) [left]
   │  ║  │     │     s: Var (name=med)
   │  ║  │     │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │     │     o: Var (name=_const_ea395317_uri, value=http://example.com/theme/medical/Medication, anonymous)
   │  ║  │     └── Filter (costEstimate=258, resultSizeEstimate=66.8K) [right]
   │  ║  │        ╠══ Or
   │  ║  │        ║  ├── Compare (=)
   │  ║  │        ║  │     Var (name=code)
   │  ║  │        ║  │     ValueConstant (value="MED-1000")
   │  ║  │        ║  └── Compare (=)
   │  ║  │        ║        Var (name=code)
   │  ║  │        ║        ValueConstant (value="MED-1001")
   │  ║  │        ╚══ StatementPattern (costEstimate=258, resultSizeEstimate=66.5K)
   │  ║  │              s: Var (name=med)
   │  ║  │              p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
   │  ║  │              o: Var (name=code)
   │  ║  └── Filter (new scope)
   │  ║     ╠══ FunctionCall (http://www.w3.org/2005/xpath-functions#contains)
   │  ║     ║  ├── FunctionCall (http://www.w3.org/2005/xpath-functions#lower-case)
   │  ║     ║  │     Str
   │  ║     ║  │        Var (name=dose)
   │  ║     ║  └── ValueConstant (value="x")
   │  ║     ╚══ StatementPattern (resultSizeEstimate=16.7K)
   │  ║           s: Var (name=med)
   │  ║           p: Var (name=_const_e2048edf_uri, value=http://example.com/theme/medical/dosage, anonymous)
   │  ║           o: Var (name=dose)
   │  ╚══ GroupElem (count)
   │        Count (Distinct)
   │           Var (name=med)
   └── ExtensionElem (count)
         Count (Distinct)
            Var (name=med)
```

### Query 8

- Fastest observed: `51.735 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:11)
- Optimized block: [1126](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:1126)

Optimized query:

```sparql
SELECT ?patient (COUNT(DISTINCT ?enc) AS ?encCount) WHERE {
?patient a <http://example.com/theme/medical/Patient> .
OPTIONAL {
?patient <http://example.com/theme/medical/hasEncounter> ?enc .
?enc <http://example.com/theme/medical/handledBy> ?practitioner .
BIND(?practitioner AS ?optPractitioner)
}
FILTER ((?optPractitioner != ?patient) && EXISTS { ?enc <http://example.com/theme/medical/hasCondition> ?cond . })
}
GROUP BY ?patient
HAVING (COUNT(?enc) >= 2)
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "patient"
║     ProjectionElem "encCount"
╚══ Extension
├── Extension
│  ╠══ Filter
│  ║  ├── Compare (>=)
│  ║  │     Var (name=_anon_having_5911754ba04afef348bb8da5249da1b84b3301234, anonymous)
│  ║  │     ValueConstant (value="2"^^<http://www.w3.org/2001/XMLSchema#integer>)
│  ║  └── Group (patient)
│  ║        Filter
│  ║        ├── And
│  ║        │  ╠══ Compare (!=)
│  ║        │  ║     Var (name=optPractitioner)
│  ║        │  ║     Var (name=patient)
│  ║        │  ╚══ Exists
│  ║        │        StatementPattern (resultSizeEstimate=49.8K)
│  ║        │           s: Var (name=enc)
│  ║        │           p: Var (name=_const_7e7389c9_uri, value=http://example.com/theme/medical/hasCondition, anonymous)
│  ║        │           o: Var (name=cond)
│  ║        └── LeftJoin
│  ║           ╠══ StatementPattern (resultSizeEstimate=8.3K) [left]
│  ║           ║     s: Var (name=patient)
│  ║           ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║           ║     o: Var (name=_const_24be87bd_uri, value=http://example.com/theme/medical/Patient, anonymous)
│  ║           ╚══ Extension [right]
│  ║              ├── Join (JoinIterator) (resultSizeEstimate=25.7K)
│  ║              │  ╠══ StatementPattern (costEstimate=79, resultSizeEstimate=25.0K) [left]
│  ║              │  ║     s: Var (name=patient)
│  ║              │  ║     p: Var (name=_const_ca285e1_uri, value=http://example.com/theme/medical/hasEncounter, anonymous)
│  ║              │  ║     o: Var (name=enc)
│  ║              │  ╚══ StatementPattern (costEstimate=158, resultSizeEstimate=25.0K) [right]
│  ║              │        s: Var (name=enc)
│  ║              │        p: Var (name=_const_9016af8b_uri, value=http://example.com/theme/medical/handledBy, anonymous)
│  ║              │        o: Var (name=practitioner)
│  ║              └── ExtensionElem (optPractitioner)
│  ║                    Var (name=practitioner)
│  ║        GroupElem (_anon_having_5911754ba04afef348bb8da5249da1b84b3301234)
│  ║           Count
│  ║              Var (name=enc)
│  ║        GroupElem (encCount)
│  ║           Count (Distinct)
│  ║              Var (name=enc)
│  ╚══ ExtensionElem (_anon_having_5911754ba04afef348bb8da5249da1b84b3301234)
│        Count
│           Var (name=enc)
└── ExtensionElem (encCount)
Count (Distinct)
Var (name=enc)
```

### Query 9

- Fastest observed: `177.495 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:12)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 10

- Fastest observed: `194.412 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:13)
- Optimized block: [1355](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:1355)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?patient) AS ?count) WHERE {
{
?patient a <http://example.com/theme/medical/Patient> .
?patient <http://example.com/theme/medical/hasMedication> ?med .
}
UNION
{
?patient a <http://example.com/theme/medical/Patient> .
?patient <http://example.com/theme/medical/hasEncounter> ?enc .
?enc <http://example.com/theme/medical/hasObservation> ?obs .
}
OPTIONAL {
?patient <http://example.com/theme/medical/name> ?optName .
}
FILTER ((?optName != "") && NOT EXISTS { ?patient <http://example.com/theme/medical/hasMedication> ?m2 . ?m2 <http://example.com/theme/medical/code> ?c . FILTER (?c = "MED-1005") })
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Filter
│  ║  ├── And
│  ║  │  ╠══ Compare (!=)
│  ║  │  ║     Var (name=optName)
│  ║  │  ║     ValueConstant (value="")
│  ║  │  ╚══ Not
│  ║  │        Exists
│  ║  │           Join (JoinIterator) (resultSizeEstimate=16.3K)
│  ║  │           ├── StatementPattern (costEstimate=65, resultSizeEstimate=16.7K) [left]
│  ║  │           │     s: Var (name=patient)
│  ║  │           │     p: Var (name=_const_fe9f43e1_uri, value=http://example.com/theme/medical/hasMedication, anonymous)
│  ║  │           │     o: Var (name=m2)
│  ║  │           └── Filter (costEstimate=258, resultSizeEstimate=66.8K) [right]
│  ║  │              ╠══ Compare (=)
│  ║  │              ║     Var (name=c)
│  ║  │              ║     ValueConstant (value="MED-1005")
│  ║  │              ╚══ StatementPattern (resultSizeEstimate=66.5K)
│  ║  │                    s: Var (name=m2)
│  ║  │                    p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
│  ║  │                    o: Var (name=c)
│  ║  └── LeftJoin
│  ║     ╠══ Union [left]
│  ║     ║  ├── Join (new scope) (JoinIterator) (resultSizeEstimate=16.6K)
│  ║     ║  │  ╠══ StatementPattern (costEstimate=4.2K, resultSizeEstimate=8.3K) [left]
│  ║     ║  │  ║     s: Var (name=patient)
│  ║     ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║     ║  │  ║     o: Var (name=_const_24be87bd_uri, value=http://example.com/theme/medical/Patient, anonymous)
│  ║     ║  │  ╚══ StatementPattern (costEstimate=129, resultSizeEstimate=16.7K) [right]
│  ║     ║  │        s: Var (name=patient)
│  ║     ║  │        p: Var (name=_const_fe9f43e1_uri, value=http://example.com/theme/medical/hasMedication, anonymous)
│  ║     ║  │        o: Var (name=med)
│  ║     ║  └── Join (new scope) (JoinIterator) (resultSizeEstimate=51.7K)
│  ║     ║     ╠══ Join (JoinIterator) (resultSizeEstimate=25.2K) [left]
│  ║     ║     ║  ├── StatementPattern (costEstimate=17.4M, resultSizeEstimate=8.3K) [left]
│  ║     ║     ║  │     s: Var (name=patient)
│  ║     ║     ║  │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║     ║     ║  │     o: Var (name=_const_24be87bd_uri, value=http://example.com/theme/medical/Patient, anonymous)
│  ║     ║     ║  └── StatementPattern (costEstimate=79, resultSizeEstimate=25.0K) [right]
│  ║     ║     ║        s: Var (name=patient)
│  ║     ║     ║        p: Var (name=_const_ca285e1_uri, value=http://example.com/theme/medical/hasEncounter, anonymous)
│  ║     ║     ║        o: Var (name=enc)
│  ║     ║     ╚══ StatementPattern (costEstimate=223, resultSizeEstimate=49.7K) [right]
│  ║     ║           s: Var (name=enc)
│  ║     ║           p: Var (name=_const_6f00815a_uri, value=http://example.com/theme/medical/hasObservation, anonymous)
│  ║     ║           o: Var (name=obs)
│  ║     ╚══ StatementPattern (resultSizeEstimate=21.4K) [right]
│  ║           s: Var (name=patient)
│  ║           p: Var (name=_const_99364b3_uri, value=http://example.com/theme/medical/name, anonymous)
│  ║           o: Var (name=optName)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=patient)
└── ExtensionElem (count)
Count (Distinct)
Var (name=patient)
```

### Query 11

- Fastest observed: `1483.784 ms/op`
- Source: [results-2026-04-18.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:14)
- Optimized block: [1402](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:1402)

Optimized query:

```sparql
SELECT REDUCED ?root ?encounter ?observation ?patientName ?medication ?medicationCode ?medicationDosage ?recordedOn ?practitioner ?practitionerName ?condition ?conditionCode ?conditionDescription ?observationValue ?observationUnit WHERE {
  ?root a <http://example.com/theme/medical/Patient> .
  ?root <http://example.com/theme/medical/hasEncounter> ?encounter .
  ?encounter a <http://example.com/theme/medical/Encounter> .
  ?encounter <http://example.com/theme/medical/hasObservation> ?observation .
  OPTIONAL {
    ?root <http://example.com/theme/medical/name> ?patientName .
    OPTIONAL {
      ?root <http://example.com/theme/medical/hasMedication> ?medication .
      OPTIONAL {
        ?medication <http://example.com/theme/medical/code> ?medicationCode .
        OPTIONAL {
          ?medication <http://example.com/theme/medical/dosage> ?medicationDosage .
        }
      }
    }
  }
  OPTIONAL {
    ?encounter <http://example.com/theme/medical/recordedOn> ?recordedOn .
    OPTIONAL {
      ?encounter <http://example.com/theme/medical/handledBy> ?practitioner .
      OPTIONAL {
        ?practitioner <http://example.com/theme/medical/name> ?practitionerName .
      }
    }
  }
  OPTIONAL {
    ?encounter <http://example.com/theme/medical/hasCondition> ?condition .
    OPTIONAL {
      ?condition <http://example.com/theme/medical/code> ?conditionCode .
      OPTIONAL {
        ?condition <http://example.com/theme/medical/description> ?conditionDescription .
      }
    }
  }
  OPTIONAL {
    ?observation <http://example.com/theme/medical/value> ?observationValue .
    OPTIONAL {
      ?observation <http://example.com/theme/medical/unit> ?observationUnit .
    }
  }
}
ORDER BY ?root ?encounter ?observation
```

Query plan:

```text
Reduced
   Order
      OrderElem (ASC)
         Var (name=root) (bindingState=unbound)
      OrderElem (ASC)
         Var (name=encounter) (bindingState=unbound)
      OrderElem (ASC)
         Var (name=observation) (bindingState=unbound)
      Projection
      ╠══ ProjectionElemList
      ║     ProjectionElem "root"
      ║     ProjectionElem "encounter"
      ║     ProjectionElem "observation"
      ║     ProjectionElem "patientName"
      ║     ProjectionElem "medication"
      ║     ProjectionElem "medicationCode"
      ║     ProjectionElem "medicationDosage"
      ║     ProjectionElem "recordedOn"
      ║     ProjectionElem "practitioner"
      ║     ProjectionElem "practitionerName"
      ║     ProjectionElem "condition"
      ║     ProjectionElem "conditionCode"
      ║     ProjectionElem "conditionDescription"
      ║     ProjectionElem "observationValue"
      ║     ProjectionElem "observationUnit"
      ╚══ LeftJoin (LeftJoinIterator)
         ├── LeftJoin (LeftJoinIterator) [left]
         │  ╠══ LeftJoin (LeftJoinIterator) [left]
         │  ║  ├── LeftJoin (LeftJoinIterator) [left]
         │  ║  │  ╠══ Join (JoinIterator) (resultSizeEstimate=53.4K) [left]
         │  ║  │  ║  ├── Join (JoinIterator) (resultSizeEstimate=26.3K) [left]
         │  ║  │  ║  │  ╠══ Join (JoinIterator) (resultSizeEstimate=25.2K) [left]
         │  ║  │  ║  │  ║  ├── StatementPattern [index: ospc]  (costEstimate=4.2K, resultSizeEstimate=8.3K, indexName=ospc) [left]
         │  ║  │  ║  │  ║  │     s: Var (name=root) (bindingState=unbound)
         │  ║  │  ║  │  ║  │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
         │  ║  │  ║  │  ║  │     o: Var (name=_const_24be87bd_uri, value=http://example.com/theme/medical/Patient, anonymous)
         │  ║  │  ║  │  ║  └── StatementPattern (costEstimate=53, resultSizeEstimate=25.0K) [right]
         │  ║  │  ║  │  ║        s: Var (name=root) (bindingState=bound)
         │  ║  │  ║  │  ║        p: Var (name=_const_ca285e1_uri, value=http://example.com/theme/medical/hasEncounter, anonymous)
         │  ║  │  ║  │  ║        o: Var (name=encounter) (bindingState=unbound)
         │  ║  │  ║  │  ╚══ StatementPattern (costEstimate=1.00, resultSizeEstimate=25.0K) [right]
         │  ║  │  ║  │        s: Var (name=encounter) (bindingState=bound)
         │  ║  │  ║  │        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
         │  ║  │  ║  │        o: Var (name=_const_5e8eb7eb_uri, value=http://example.com/theme/medical/Encounter, anonymous)
         │  ║  │  ║  └── StatementPattern (costEstimate=223, resultSizeEstimate=49.7K) [right]
         │  ║  │  ║        s: Var (name=encounter) (bindingState=bound)
         │  ║  │  ║        p: Var (name=_const_6f00815a_uri, value=http://example.com/theme/medical/hasObservation, anonymous)
         │  ║  │  ║        o: Var (name=observation) (bindingState=unbound)
         │  ║  │  ╚══ LeftJoin [right]
         │  ║  │     ├── StatementPattern (resultSizeEstimate=21.4K) [left]
         │  ║  │     │     s: Var (name=root) (bindingState=bound)
         │  ║  │     │     p: Var (name=_const_99364b3_uri, value=http://example.com/theme/medical/name, anonymous)
         │  ║  │     │     o: Var (name=patientName) (bindingState=unbound)
         │  ║  │     └── LeftJoin [right]
         │  ║  │        ╠══ StatementPattern (resultSizeEstimate=16.7K) [left]
         │  ║  │        ║     s: Var (name=root) (bindingState=bound)
         │  ║  │        ║     p: Var (name=_const_fe9f43e1_uri, value=http://example.com/theme/medical/hasMedication, anonymous)
         │  ║  │        ║     o: Var (name=medication) (bindingState=unbound)
         │  ║  │        ╚══ LeftJoin [right]
         │  ║  │           ├── StatementPattern (resultSizeEstimate=66.5K) [left]
         │  ║  │           │     s: Var (name=medication) (bindingState=bound)
         │  ║  │           │     p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
         │  ║  │           │     o: Var (name=medicationCode) (bindingState=unbound)
         │  ║  │           └── StatementPattern (resultSizeEstimate=16.7K) [right]
         │  ║  │                 s: Var (name=medication) (bindingState=bound)
         │  ║  │                 p: Var (name=_const_e2048edf_uri, value=http://example.com/theme/medical/dosage, anonymous)
         │  ║  │                 o: Var (name=medicationDosage) (bindingState=unbound)
         │  ║  └── LeftJoin [right]
         │  ║     ╠══ StatementPattern (resultSizeEstimate=25.0K) [left]
         │  ║     ║     s: Var (name=encounter) (bindingState=bound)
         │  ║     ║     p: Var (name=_const_2816f2d7_uri, value=http://example.com/theme/medical/recordedOn, anonymous)
         │  ║     ║     o: Var (name=recordedOn) (bindingState=unbound)
         │  ║     ╚══ LeftJoin [right]
         │  ║        ├── StatementPattern (resultSizeEstimate=25.0K) [left]
         │  ║        │     s: Var (name=encounter) (bindingState=bound)
         │  ║        │     p: Var (name=_const_9016af8b_uri, value=http://example.com/theme/medical/handledBy, anonymous)
         │  ║        │     o: Var (name=practitioner) (bindingState=unbound)
         │  ║        └── StatementPattern (resultSizeEstimate=21.4K) [right]
         │  ║              s: Var (name=practitioner) (bindingState=bound)
         │  ║              p: Var (name=_const_99364b3_uri, value=http://example.com/theme/medical/name, anonymous)
         │  ║              o: Var (name=practitionerName) (bindingState=unbound)
         │  ╚══ LeftJoin [right]
         │     ├── StatementPattern (resultSizeEstimate=49.8K) [left]
         │     │     s: Var (name=encounter) (bindingState=bound)
         │     │     p: Var (name=_const_7e7389c9_uri, value=http://example.com/theme/medical/hasCondition, anonymous)
         │     │     o: Var (name=condition) (bindingState=unbound)
         │     └── LeftJoin [right]
         │        ╠══ StatementPattern (resultSizeEstimate=66.5K) [left]
         │        ║     s: Var (name=condition) (bindingState=bound)
         │        ║     p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
         │        ║     o: Var (name=conditionCode) (bindingState=unbound)
         │        ╚══ StatementPattern (resultSizeEstimate=0) [right]
         │              s: Var (name=condition) (bindingState=bound)
         │              p: Var (name=_const_25295cd4_uri, value=http://example.com/theme/medical/description, anonymous)
         │              o: Var (name=conditionDescription) (bindingState=unbound)
         └── LeftJoin [right]
            ╠══ StatementPattern (resultSizeEstimate=49.7K) [left]
            ║     s: Var (name=observation) (bindingState=bound)
            ║     p: Var (name=_const_2949ec49_uri, value=http://example.com/theme/medical/value, anonymous)
            ║     o: Var (name=observationValue) (bindingState=unbound)
            ╚══ StatementPattern (resultSizeEstimate=0) [right]
                  s: Var (name=observation) (bindingState=bound)
                  p: Var (name=_const_996c3ac_uri, value=http://example.com/theme/medical/unit, anonymous)
                  o: Var (name=observationUnit) (bindingState=unbound)
```

### Query 12

- Fastest observed: `643.729 ms/op`
- Source: [results-2026-04-18.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:15)
- Optimized block: [1613](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:1613)

Optimized query:

```sparql
SELECT DISTINCT ?root ?optName ?optConditionCode ?optEncounter2 ?optLabel ?optEncounter ?optPractitioner ?optMedicationCode ?optDosage WHERE {
  {
    ?root a <http://example.com/theme/medical/Patient> .
  }
  UNION
  {
    ?root a <http://example.com/theme/medical/Encounter> .
  }
  OPTIONAL {
    ?root <http://example.com/theme/medical/name> ?optName .
    BIND(?optName AS ?optLabel)
  }
  OPTIONAL {
    ?root <http://example.com/theme/medical/hasEncounter> ?optEncounter .
    OPTIONAL {
      ?optEncounter <http://example.com/theme/medical/recordedOn> ?optDate .
    }
    OPTIONAL {
      ?optEncounter <http://example.com/theme/medical/handledBy> ?optPractitioner .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/medical/hasMedication> ?optMedication .
    OPTIONAL {
      ?optMedication <http://example.com/theme/medical/code> ?optMedicationCode .
    }
    OPTIONAL {
      ?optMedication <http://example.com/theme/medical/dosage> ?optDosage .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/medical/hasEncounter> ?optEncounter2 .
    ?optEncounter2 <http://example.com/theme/medical/hasCondition> ?optCondition .
    OPTIONAL {
      ?optCondition <http://example.com/theme/medical/code> ?optConditionCode .
    }
  }
}
```

Query plan:

```text
Distinct
   Projection
   ├── ProjectionElemList
   │     ProjectionElem "root"
   │     ProjectionElem "optName"
   │     ProjectionElem "optConditionCode"
   │     ProjectionElem "optEncounter2"
   │     ProjectionElem "optLabel"
   │     ProjectionElem "optEncounter"
   │     ProjectionElem "optPractitioner"
   │     ProjectionElem "optMedicationCode"
   │     ProjectionElem "optDosage"
   └── LeftJoin (LeftJoinIterator)
      ╠══ LeftJoin (LeftJoinIterator) [left]
      ║  ├── LeftJoin (LeftJoinIterator) [left]
      ║  │  ╠══ LeftJoin (LeftJoinIterator) [left]
      ║  │  ║  ├── Union [left]
      ║  │  ║  │  ╠══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=8.3K, indexName=ospc)
      ║  │  ║  │  ║     s: Var (name=root) (bindingState=unbound)
      ║  │  ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
      ║  │  ║  │  ║     o: Var (name=_const_24be87bd_uri, value=http://example.com/theme/medical/Patient, anonymous)
      ║  │  ║  │  ╚══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=25.0K, indexName=ospc)
      ║  │  ║  │        s: Var (name=root) (bindingState=unbound)
      ║  │  ║  │        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
      ║  │  ║  │        o: Var (name=_const_5e8eb7eb_uri, value=http://example.com/theme/medical/Encounter, anonymous)
      ║  │  ║  └── Extension [right]
      ║  │  ║     ╠══ StatementPattern (resultSizeEstimate=21.4K)
      ║  │  ║     ║     s: Var (name=root) (bindingState=bound)
      ║  │  ║     ║     p: Var (name=_const_99364b3_uri, value=http://example.com/theme/medical/name, anonymous)
      ║  │  ║     ║     o: Var (name=optName) (bindingState=unbound)
      ║  │  ║     ╚══ ExtensionElem (optLabel)
      ║  │  ║           Var (name=optName) (bindingState=bound)
      ║  │  ╚══ LeftJoin [right]
      ║  │     ├── LeftJoin [left]
      ║  │     │  ╠══ StatementPattern (resultSizeEstimate=25.0K) [left]
      ║  │     │  ║     s: Var (name=root) (bindingState=bound)
      ║  │     │  ║     p: Var (name=_const_ca285e1_uri, value=http://example.com/theme/medical/hasEncounter, anonymous)
      ║  │     │  ║     o: Var (name=optEncounter) (bindingState=unbound)
      ║  │     │  ╚══ StatementPattern (resultSizeEstimate=25.0K) [right]
      ║  │     │        s: Var (name=optEncounter) (bindingState=bound)
      ║  │     │        p: Var (name=_const_2816f2d7_uri, value=http://example.com/theme/medical/recordedOn, anonymous)
      ║  │     │        o: Var (name=optDate) (bindingState=unbound)
      ║  │     └── StatementPattern (resultSizeEstimate=25.0K) [right]
      ║  │           s: Var (name=optEncounter) (bindingState=bound)
      ║  │           p: Var (name=_const_9016af8b_uri, value=http://example.com/theme/medical/handledBy, anonymous)
      ║  │           o: Var (name=optPractitioner) (bindingState=unbound)
      ║  └── LeftJoin [right]
      ║     ╠══ LeftJoin [left]
      ║     ║  ├── StatementPattern (resultSizeEstimate=16.7K) [left]
      ║     ║  │     s: Var (name=root) (bindingState=bound)
      ║     ║  │     p: Var (name=_const_fe9f43e1_uri, value=http://example.com/theme/medical/hasMedication, anonymous)
      ║     ║  │     o: Var (name=optMedication) (bindingState=unbound)
      ║     ║  └── StatementPattern (resultSizeEstimate=66.5K) [right]
      ║     ║        s: Var (name=optMedication) (bindingState=bound)
      ║     ║        p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
      ║     ║        o: Var (name=optMedicationCode) (bindingState=unbound)
      ║     ╚══ StatementPattern (resultSizeEstimate=16.7K) [right]
      ║           s: Var (name=optMedication) (bindingState=bound)
      ║           p: Var (name=_const_e2048edf_uri, value=http://example.com/theme/medical/dosage, anonymous)
      ║           o: Var (name=optDosage) (bindingState=unbound)
      ╚══ LeftJoin [right]
         ├── Join (JoinIterator) (resultSizeEstimate=51.9K) [left]
         │  ╠══ StatementPattern (costEstimate=79, resultSizeEstimate=25.0K) [left]
         │  ║     s: Var (name=root) (bindingState=bound)
         │  ║     p: Var (name=_const_ca285e1_uri, value=http://example.com/theme/medical/hasEncounter, anonymous)
         │  ║     o: Var (name=optEncounter2) (bindingState=unbound)
         │  ╚══ StatementPattern (costEstimate=223, resultSizeEstimate=49.8K) [right]
         │        s: Var (name=optEncounter2) (bindingState=bound)
         │        p: Var (name=_const_7e7389c9_uri, value=http://example.com/theme/medical/hasCondition, anonymous)
         │        o: Var (name=optCondition) (bindingState=unbound)
         └── StatementPattern (resultSizeEstimate=66.5K) [right]
               s: Var (name=optCondition) (bindingState=bound)
               p: Var (name=_const_98e9815_uri, value=http://example.com/theme/medical/code, anonymous)
               o: Var (name=optConditionCode) (bindingState=unbound)
```

## PHARMA

### Query 0

- Fastest observed: `0.157 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:80)
- Optimized block: [5006](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:5006)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?drug) AS ?count) WHERE {
VALUES ?disease { <http://example.com/theme/pharma/disease/0> <http://example.com/theme/pharma/disease/1> }
?trial <http://example.com/theme/pharma/studiesDisease> ?disease .
?trial a <http://example.com/theme/pharma/ClinicalTrial> .
?trial <http://example.com/theme/pharma/hasArm> ?arm .
?arm <http://example.com/theme/pharma/hasResult> ?result .
?arm <http://example.com/theme/pharma/armDrug> ?drug .
?result <http://example.com/theme/pharma/pValue> ?p .
?result <http://example.com/theme/pharma/effectSize> ?effect .
FILTER ((?p < 0.05) || (?effect > 0.7))
OPTIONAL {
?result <http://example.com/theme/pharma/biomarker> ?marker .
BIND(?marker AS ?optMarker)
}
FILTER (?optMarker != <http://example.com/theme/pharma/biomarker/999>)
}
```

Query plan:

_Not present in fastest-run source file._

### Query 1

- Fastest observed: `1.314 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:81)
- Optimized block: [5072](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:5072)

Optimized query:

```sparql
SELECT ?combo (COUNT(DISTINCT ?drug) AS ?drugCount) WHERE {
  ?combo <http://example.com/theme/pharma/synergyScore> ?score .
  FILTER (?score > 0.7)
  ?combo a <http://example.com/theme/pharma/Combination> .
  ?combo <http://example.com/theme/pharma/combinationOf> ?drug .
  OPTIONAL {
    ?drug <http://example.com/theme/pharma/hasSideEffect> ?sideEffect .
    ?sideEffect <http://example.com/theme/pharma/severity> ?sev .
    BIND(?sev AS ?optSeverity)
  }
  FILTER (?optSeverity IN ("Mild", "Moderate"))
}
GROUP BY ?combo
HAVING (COUNT(DISTINCT ?drug) >= 2)
```

Query plan:

_Not present in fastest-run source file._

### Query 2

- Fastest observed: `25.231 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:82)
- Optimized block: [5135](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:5135)

Optimized query:

```sparql
SELECT ?target (COUNT(DISTINCT ?drug) AS ?drugCount) WHERE {
  ?target a <http://example.com/theme/pharma/Target> .
  ?target <http://example.com/theme/pharma/inPathway> ?pathway .
  ?drug <http://example.com/theme/pharma/targets> ?target .
  ?drug a <http://example.com/theme/pharma/Drug> .
  OPTIONAL {
    ?drug <http://example.com/theme/pharma/indicatedFor> ?disease .
    BIND(?disease AS ?optDisease)
  }
  FILTER ((?optDisease IN (<http://example.com/theme/pharma/disease/2>, <http://example.com/theme/pharma/disease/3>)) && EXISTS { ?arm <http://example.com/theme/pharma/armDrug> ?drug . ?trial <http://example.com/theme/pharma/hasArm> ?arm . })
}
GROUP BY ?target
HAVING (COUNT(DISTINCT ?drug) > 2)
```

Query plan:

_Not present in fastest-run source file._

### Query 3

- Fastest observed: `8.314 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:83)
- Optimized block: [5196](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:5196)

Optimized query:

```sparql
SELECT ?drug ?disease WHERE {
  ?result <http://example.com/theme/pharma/responseRate> ?rate .
  FILTER (?rate > 0.6)
  ?arm <http://example.com/theme/pharma/hasResult> ?result .
  ?trial <http://example.com/theme/pharma/hasArm> ?arm .
  ?trial a <http://example.com/theme/pharma/ClinicalTrial> .
  ?trial <http://example.com/theme/pharma/studiesDisease> ?disease .
  ?arm <http://example.com/theme/pharma/armDrug> ?drug .
  FILTER NOT EXISTS {
    ?drug <http://example.com/theme/pharma/indicatedFor> ?disease .
  }
  OPTIONAL {
    ?drug <http://example.com/theme/pharma/targets> ?target .
    BIND(?target AS ?optTarget)
  }
  FILTER (?optTarget != <http://example.com/theme/pharma/target/0>)
}
```

Query plan:

_Not present in fastest-run source file._

### Query 4

- Fastest observed: `26.885 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:84)
- Optimized block: [5264](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:5264)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?drug) AS ?count) WHERE {
  {
    ?drug a <http://example.com/theme/pharma/Drug> .
    ?drug <http://example.com/theme/pharma/hasMolecule> ?mol .
    ?mol <http://example.com/theme/pharma/inClass> ?class .
  }
  UNION
  {
    ?combo a <http://example.com/theme/pharma/Combination> .
    ?combo <http://example.com/theme/pharma/combinationOf> ?drug .
    ?drug <http://example.com/theme/pharma/hasMolecule> ?mol .
    ?mol <http://example.com/theme/pharma/inClass> ?class .
  }
  OPTIONAL {
    ?class <http://example.com/theme/pharma/name> ?optName .
    BIND(?optName AS ?optClassName)
  }
  FILTER (?optClassName != "")
  MINUS {
    ?drug <http://example.com/theme/pharma/contraindicatedFor> ?disease .
    FILTER (?disease IN (<http://example.com/theme/pharma/disease/4>, <http://example.com/theme/pharma/disease/5>))
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 5

- Fastest observed: `0.330 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:85)
- Optimized block: [5338](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:5338)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?trial) AS ?count) WHERE {
VALUES ?marker { <http://example.com/theme/pharma/biomarker/0> <http://example.com/theme/pharma/biomarker/1> <http://example.com/theme/pharma/biomarker/2> }
?result <http://example.com/theme/pharma/biomarker> ?marker .
?result <http://example.com/theme/pharma/pValue> ?p .
FILTER ((?p < 0.05) || (?p = 0.05))
?arm <http://example.com/theme/pharma/hasResult> ?result .
?trial <http://example.com/theme/pharma/hasArm> ?arm .
?trial a <http://example.com/theme/pharma/ClinicalTrial> .
OPTIONAL {
?result <http://example.com/theme/pharma/effectSize> ?effect .
BIND(?effect AS ?optEffect)
}
FILTER (?optEffect > 0.3)
}
```

Query plan:

_Not present in fastest-run source file._

### Query 6

- Fastest observed: `3.024 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:86)
- Optimized block: [5400](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:5400)

Optimized query:

```sparql
SELECT ?combo (COUNT(DISTINCT ?target) AS ?sharedTargets) WHERE {
  ?combo a <http://example.com/theme/pharma/Combination> .
  ?combo <http://example.com/theme/pharma/combinationOf> ?drugA .
  ?combo <http://example.com/theme/pharma/combinationOf> ?drugB .
  FILTER (?drugA != ?drugB)
  ?drugA <http://example.com/theme/pharma/targets> ?target .
  ?drugB <http://example.com/theme/pharma/targets> ?target .
  OPTIONAL {
    ?drugA <http://example.com/theme/pharma/hasSideEffect> ?sideEffect .
    BIND(?sideEffect AS ?optSideEffect)
  }
  FILTER (EXISTS { ?drugB <http://example.com/theme/pharma/hasSideEffect> ?sideEffect2 . } && (?optSideEffect != <http://example.com/theme/pharma/side-effect/0>))
}
GROUP BY ?combo
HAVING (COUNT(DISTINCT ?target) > 1)
```

Query plan:

_Not present in fastest-run source file._

### Query 7

- Fastest observed: `18.945 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:87)
- Optimized block: [5464](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:5464)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?arm) AS ?count) WHERE {
  ?trial a <http://example.com/theme/pharma/ClinicalTrial> .
  ?trial <http://example.com/theme/pharma/hasArm> ?arm .
  ?arm (<http://example.com/theme/pharma/armComparator>|<http://example.com/theme/pharma/armDrug>) ?comp .
  OPTIONAL {
    ?comp <http://example.com/theme/pharma/name> ?optName .
    BIND(?optName AS ?optCompName)
  }
  FILTER (NOT EXISTS { ?arm <http://example.com/theme/pharma/hasResult> ?r . ?r <http://example.com/theme/pharma/pValue> ?p . FILTER (?p IN (0.08, 0.09)) } && (?optCompName != ""))
}
```

Query plan:

_Not present in fastest-run source file._

### Query 8

- Fastest observed: `24.760 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:88)
- Optimized block: [5523](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:5523)

Optimized query:

```sparql
SELECT ?drug (COUNT(DISTINCT ?target) AS ?targetCount) WHERE {
  ?drug a <http://example.com/theme/pharma/Drug> .
  ?drug <http://example.com/theme/pharma/targets> ?target .
  OPTIONAL {
    ?drug <http://example.com/theme/pharma/hasMolecule> ?mol .
    BIND(?mol AS ?optMol)
  }
  FILTER (?optMol != <http://example.com/theme/pharma/molecule/0>)
  MINUS {
    {
      ?drug <http://example.com/theme/pharma/contraindicatedFor> <http://example.com/theme/pharma/disease/6> .
      FILTER (sameTerm(?disease, <http://example.com/theme/pharma/disease/6>))
    }
    UNION
    {
      ?drug <http://example.com/theme/pharma/contraindicatedFor> <http://example.com/theme/pharma/disease/7> .
      FILTER (sameTerm(?disease, <http://example.com/theme/pharma/disease/7>))
    }
  }
}
GROUP BY ?drug
HAVING (COUNT(DISTINCT ?target) >= 3)
```

Query plan:

_Not present in fastest-run source file._

### Query 9

- Fastest observed: `13.204 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:89)
- Optimized block: [5601](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:5601)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?drug) AS ?count) WHERE {
  {
    SELECT ?drug (AVG(?effect) AS ?avgEffect) WHERE {
      ?trial a <http://example.com/theme/pharma/ClinicalTrial> .
      ?trial <http://example.com/theme/pharma/hasArm> ?arm .
      ?arm <http://example.com/theme/pharma/hasResult> ?result .
      ?arm <http://example.com/theme/pharma/armDrug> ?drug .
      ?result <http://example.com/theme/pharma/effectSize> ?effect .
      OPTIONAL {
        ?result <http://example.com/theme/pharma/responseRate> ?rate .
        BIND(?rate AS ?optRate)
      }
      FILTER (?optRate > 0.2)
    }
    GROUP BY ?drug
    HAVING (AVG(?effect) > 0.4)
  }
  OPTIONAL {
    ?drug <http://example.com/theme/pharma/indicatedFor> ?disease .
    BIND(?disease AS ?optDisease)
  }
  FILTER ((?optDisease IN (<http://example.com/theme/pharma/disease/8>, <http://example.com/theme/pharma/disease/9>)) && EXISTS { ?drug <http://example.com/theme/pharma/hasSideEffect> ?se . })
}
```

Query plan:

_Not present in fastest-run source file._

### Query 10

- Fastest observed: `226.964 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:90)
- Optimized block: [5677](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:5677)

Optimized query:

```sparql
SELECT ?pathway (COUNT(DISTINCT ?drug) AS ?drugCount) WHERE {
?target <http://example.com/theme/pharma/inPathway> ?pathway .
?drug a <http://example.com/theme/pharma/Drug> .
?drug <http://example.com/theme/pharma/targets> ?target .
VALUES ?marker { <http://example.com/theme/pharma/biomarker/3> <http://example.com/theme/pharma/biomarker/4> }
OPTIONAL {
?drug <http://example.com/theme/pharma/testedIn> ?trial .
BIND(?trial AS ?optTrial)
}
FILTER ((?optTrial != <http://example.com/theme/pharma/trial/0>) && EXISTS { ?result <http://example.com/theme/pharma/biomarker> ?marker . ?arm <http://example.com/theme/pharma/hasResult> ?result . ?trial <http://example.com/theme/pharma/hasArm> ?arm . })
}
GROUP BY ?pathway
HAVING (COUNT(DISTINCT ?drug) > 1)
```

Query plan:

_Not present in fastest-run source file._

### Query 12

- Fastest observed: `146.343 ms/op`
- Source: [results-2026-04-18.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:110)
- Optimized block: [12126](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:12126)

Optimized query:

```sparql
SELECT ?root ?optName ?optLabel ?optTarget ?optPathway ?optIndication ?optContraindication ?optArm ?trial ?optResult ?optPValue ?optEffectSize ?combo ?optSynergy WHERE {
  {
    ?root a <http://example.com/theme/pharma/Drug> .
  }
  UNION
  {
    ?root a <http://example.com/theme/pharma/ClinicalTrial> .
  }
  OPTIONAL {
    ?root <http://example.com/theme/pharma/name> ?optName .
    BIND(?optName AS ?optLabel)
  }
  OPTIONAL {
    ?root <http://example.com/theme/pharma/targets> ?optTarget .
    OPTIONAL {
      ?optTarget <http://example.com/theme/pharma/inPathway> ?optPathway .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/pharma/indicatedFor> ?optIndication .
    OPTIONAL {
      ?root <http://example.com/theme/pharma/contraindicatedFor> ?optContraindication .
    }
  }
  OPTIONAL {
    {
      ?root <http://example.com/theme/pharma/hasArm> ?optArm .
    }
    UNION
    {
      ?optArm <http://example.com/theme/pharma/armDrug> ?root .
      ?trial <http://example.com/theme/pharma/hasArm> ?optArm .
    }
    OPTIONAL {
      ?optArm <http://example.com/theme/pharma/hasResult> ?optResult .
    }
    OPTIONAL {
      ?optResult <http://example.com/theme/pharma/pValue> ?optPValue .
    }
    OPTIONAL {
      ?optResult <http://example.com/theme/pharma/effectSize> ?optEffectSize .
    }
  }
  OPTIONAL {
    ?combo <http://example.com/theme/pharma/combinationOf> ?root .
    OPTIONAL {
      ?combo <http://example.com/theme/pharma/synergyScore> ?optSynergy .
    }
  }
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "root"
║     ProjectionElem "optName"
║     ProjectionElem "optLabel"
║     ProjectionElem "optTarget"
║     ProjectionElem "optPathway"
║     ProjectionElem "optIndication"
║     ProjectionElem "optContraindication"
║     ProjectionElem "optArm"
║     ProjectionElem "trial"
║     ProjectionElem "optResult"
║     ProjectionElem "optPValue"
║     ProjectionElem "optEffectSize"
║     ProjectionElem "combo"
║     ProjectionElem "optSynergy"
╚══ LeftJoin (LeftJoinIterator)
   ├── LeftJoin (LeftJoinIterator) [left]
   │  ╠══ LeftJoin (LeftJoinIterator) [left]
   │  ║  ├── LeftJoin (LeftJoinIterator) [left]
   │  ║  │  ╠══ LeftJoin (LeftJoinIterator) [left]
   │  ║  │  ║  ├── Union [left]
   │  ║  │  ║  │  ╠══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=5.0K, indexName=ospc)
   │  ║  │  ║  │  ║     s: Var (name=root) (bindingState=unbound)
   │  ║  │  ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │  ║  │  ║     o: Var (name=_const_f6bbe068_uri, value=http://example.com/theme/pharma/Drug, anonymous)
   │  ║  │  ║  │  ╚══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=955, indexName=ospc)
   │  ║  │  ║  │        s: Var (name=root) (bindingState=unbound)
   │  ║  │  ║  │        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │  ║  │        o: Var (name=_const_4795bbfb_uri, value=http://example.com/theme/pharma/ClinicalTrial, anonymous)
   │  ║  │  ║  └── Extension [right]
   │  ║  │  ║     ╠══ StatementPattern (resultSizeEstimate=13.2K)
   │  ║  │  ║     ║     s: Var (name=root) (bindingState=bound)
   │  ║  │  ║     ║     p: Var (name=_const_f6ceb733_uri, value=http://example.com/theme/pharma/name, anonymous)
   │  ║  │  ║     ║     o: Var (name=optName) (bindingState=unbound)
   │  ║  │  ║     ╚══ ExtensionElem (optLabel)
   │  ║  │  ║           Var (name=optName) (bindingState=bound)
   │  ║  │  ╚══ LeftJoin [right]
   │  ║  │     ├── StatementPattern (resultSizeEstimate=20.0K) [left]
   │  ║  │     │     s: Var (name=root) (bindingState=bound)
   │  ║  │     │     p: Var (name=_const_7f67635a_uri, value=http://example.com/theme/pharma/targets, anonymous)
   │  ║  │     │     o: Var (name=optTarget) (bindingState=unbound)
   │  ║  │     └── StatementPattern (resultSizeEstimate=666) [right]
   │  ║  │           s: Var (name=optTarget) (bindingState=bound)
   │  ║  │           p: Var (name=_const_1a978c1d_uri, value=http://example.com/theme/pharma/inPathway, anonymous)
   │  ║  │           o: Var (name=optPathway) (bindingState=unbound)
   │  ║  └── LeftJoin [right]
   │  ║     ╠══ StatementPattern (resultSizeEstimate=9.9K) [left]
   │  ║     ║     s: Var (name=root) (bindingState=bound)
   │  ║     ║     p: Var (name=_const_e46c34a6_uri, value=http://example.com/theme/pharma/indicatedFor, anonymous)
   │  ║     ║     o: Var (name=optIndication) (bindingState=unbound)
   │  ║     ╚══ StatementPattern (resultSizeEstimate=5.0K) [right]
   │  ║           s: Var (name=root) (bindingState=bound)
   │  ║           p: Var (name=_const_28b88607_uri, value=http://example.com/theme/pharma/contraindicatedFor, anonymous)
   │  ║           o: Var (name=optContraindication) (bindingState=unbound)
   │  ╚══ LeftJoin [right]
   │     ├── LeftJoin [left]
   │     │  ╠══ LeftJoin [left]
   │     │  ║  ├── Union [left]
   │     │  ║  │  ╠══ StatementPattern (new scope) (resultSizeEstimate=2.9K)
   │     │  ║  │  ║     s: Var (name=root) (bindingState=bound)
   │     │  ║  │  ║     p: Var (name=_const_73c2e40a_uri, value=http://example.com/theme/pharma/hasArm, anonymous)
   │     │  ║  │  ║     o: Var (name=optArm) (bindingState=unbound)
   │     │  ║  │  ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=2.9K)
   │     │  ║  │     ├── StatementPattern (costEstimate=27, resultSizeEstimate=2.9K) [left]
   │     │  ║  │     │     s: Var (name=optArm) (bindingState=unbound)
   │     │  ║  │     │     p: Var (name=_const_aefd3274_uri, value=http://example.com/theme/pharma/armDrug, anonymous)
   │     │  ║  │     │     o: Var (name=root) (bindingState=bound)
   │     │  ║  │     └── StatementPattern (costEstimate=54, resultSizeEstimate=2.9K) [right]
   │     │  ║  │           s: Var (name=trial) (bindingState=unbound)
   │     │  ║  │           p: Var (name=_const_73c2e40a_uri, value=http://example.com/theme/pharma/hasArm, anonymous)
   │     │  ║  │           o: Var (name=optArm) (bindingState=bound)
   │     │  ║  └── StatementPattern (resultSizeEstimate=2.9K) [right]
   │     │  ║        s: Var (name=optArm) (bindingState=bound)
   │     │  ║        p: Var (name=_const_60f6d7af_uri, value=http://example.com/theme/pharma/hasResult, anonymous)
   │     │  ║        o: Var (name=optResult) (bindingState=unbound)
   │     │  ╚══ StatementPattern (resultSizeEstimate=2.9K) [right]
   │     │        s: Var (name=optResult) (bindingState=bound)
   │     │        p: Var (name=_const_80c71989_uri, value=http://example.com/theme/pharma/pValue, anonymous)
   │     │        o: Var (name=optPValue) (bindingState=unbound)
   │     └── StatementPattern (resultSizeEstimate=2.9K) [right]
   │           s: Var (name=optResult) (bindingState=bound)
   │           p: Var (name=_const_6999fbda_uri, value=http://example.com/theme/pharma/effectSize, anonymous)
   │           o: Var (name=optEffectSize) (bindingState=unbound)
   └── LeftJoin [right]
      ╠══ StatementPattern (resultSizeEstimate=949) [left]
      ║     s: Var (name=combo) (bindingState=unbound)
      ║     p: Var (name=_const_94a74d5e_uri, value=http://example.com/theme/pharma/combinationOf, anonymous)
      ║     o: Var (name=root) (bindingState=bound)
      ╚══ StatementPattern (resultSizeEstimate=477) [right]
            s: Var (name=combo) (bindingState=bound)
            p: Var (name=_const_2c1ec653_uri, value=http://example.com/theme/pharma/synergyScore, anonymous)
            o: Var (name=optSynergy) (bindingState=unbound)
```

## SOCIAL_MEDIA

### Query 0

- Fastest observed: `0.040 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:14)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 1

- Fastest observed: `4.785 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:15)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 2

- Fastest observed: `0.049 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:16)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 3

- Fastest observed: `0.049 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:17)
- Optimized block: not present in fastest-run source file

Optimized query:

```sparql
SELECT ?u (COUNT(DISTINCT ?v) AS ?degree) WHERE {
  VALUES (?u ?v) {
    (<http://example.com/theme/social/user/3> <http://example.com/theme/social/user/3>)
    (<http://example.com/theme/social/user/3> <http://example.com/theme/social/user/4>)
    (<http://example.com/theme/social/user/3> <http://example.com/theme/social/user/5>)
    (<http://example.com/theme/social/user/3> <http://example.com/theme/social/user/6>)
    (<http://example.com/theme/social/user/4> <http://example.com/theme/social/user/3>)
    (<http://example.com/theme/social/user/4> <http://example.com/theme/social/user/4>)
    (<http://example.com/theme/social/user/4> <http://example.com/theme/social/user/5>)
    (<http://example.com/theme/social/user/4> <http://example.com/theme/social/user/6>)
    (<http://example.com/theme/social/user/5> <http://example.com/theme/social/user/3>)
    (<http://example.com/theme/social/user/5> <http://example.com/theme/social/user/4>)
    (<http://example.com/theme/social/user/5> <http://example.com/theme/social/user/5>)
    (<http://example.com/theme/social/user/5> <http://example.com/theme/social/user/6>)
    (<http://example.com/theme/social/user/6> <http://example.com/theme/social/user/3>)
    (<http://example.com/theme/social/user/6> <http://example.com/theme/social/user/4>)
    (<http://example.com/theme/social/user/6> <http://example.com/theme/social/user/5>)
    (<http://example.com/theme/social/user/6> <http://example.com/theme/social/user/6>)
  }
  FILTER (?u != ?v)
  ?u <http://example.com/theme/social/follows> ?v .
  OPTIONAL {
    ?u <http://example.com/theme/social/name> ?optName .
    BIND(?optName AS ?optAlias)
  }
  FILTER (?optAlias IN ("user3", "user4", "user5", "user6"))
}
GROUP BY ?u
HAVING (COUNT(DISTINCT ?v) >= 3)
```

Query plan:

_Not present in fastest-run source file._

### Query 4

- Fastest observed: `0.056 ms/op`
- Source: [results-2026-04-15.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-15.md:18)
- Optimized block: not present in fastest-run source file

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?u) AS ?count) WHERE {
VALUES ?u { <http://example.com/theme/social/user/7> <http://example.com/theme/social/user/8> <http://example.com/theme/social/user/9> <http://example.com/theme/social/user/10> <http://example.com/theme/social/user/11> }
FILTER NOT EXISTS {
?u <http://example.com/theme/social/follows> ?u .
BIND(?u AS ?_anon_path_3afc84691eb614a7286df1a644e93f4ae012)
}
VALUES ?v { <http://example.com/theme/social/user/7> <http://example.com/theme/social/user/8> <http://example.com/theme/social/user/9> <http://example.com/theme/social/user/10> <http://example.com/theme/social/user/11> }
FILTER (?u != ?v)
?u <http://example.com/theme/social/follows> ?v .
OPTIONAL {
?v <http://example.com/theme/social/name> ?optName .
}
FILTER (?optName != "")
}
```

Query plan:

_Not present in fastest-run source file._

### Query 5

- Fastest observed: `642.956 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:19)
- Optimized block: [2142](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:2142)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?activity) AS ?count) WHERE {
VALUES ?u { <http://example.com/theme/social/user/7> <http://example.com/theme/social/user/8> <http://example.com/theme/social/user/9> <http://example.com/theme/social/user/10> <http://example.com/theme/social/user/11> }
{
?u <http://example.com/theme/social/follows> ?v .
?v <http://example.com/theme/social/follows> ?u .
BIND(?v AS ?activity)
}
UNION
{
?post <http://example.com/theme/social/authored> ?u .
BIND(?post AS ?activity)
}
OPTIONAL {
?u <http://example.com/theme/social/name> ?optName .
}
FILTER (?optName IN ("user7", "user8", "user9", "user10", "user11"))
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Filter
│  ║  ├── ListMemberOperator
│  ║  │     Var (name=optName)
│  ║  │     ValueConstant (value="user7")
│  ║  │     ValueConstant (value="user8")
│  ║  │     ValueConstant (value="user9")
│  ║  │     ValueConstant (value="user10")
│  ║  │     ValueConstant (value="user11")
│  ║  └── LeftJoin
│  ║     ╠══ Join (HashJoinIteration) [left]
│  ║     ║  ├── BindingSetAssignment ([[u=http://example.com/theme/social/user/7], [u=http://example.com/theme/social/user/8], [u=http://example.com/theme/social/user/9], [u=http://example.com/theme/social/user/10], [u=http://example.com/theme/social/user/11]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║     ║  └── Union (new scope) [right]
│  ║     ║     ╠══ Extension (new scope)
│  ║     ║     ║  ├── Join (JoinIterator) (resultSizeEstimate=20663.5M)
│  ║     ║     ║  │  ╠══ StatementPattern (costEstimate=190, resultSizeEstimate=143.7K) [left]
│  ║     ║     ║  │  ║     s: Var (name=u)
│  ║     ║     ║  │  ║     p: Var (name=_const_9c68e12a_uri, value=http://example.com/theme/social/follows, anonymous)
│  ║     ║     ║  │  ║     o: Var (name=v)
│  ║     ║     ║  │  ╚══ StatementPattern (costEstimate=0.50, resultSizeEstimate=143.7K) [right]
│  ║     ║     ║  │        s: Var (name=v)
│  ║     ║     ║  │        p: Var (name=_const_9c68e12a_uri, value=http://example.com/theme/social/follows, anonymous)
│  ║     ║     ║  │        o: Var (name=u)
│  ║     ║     ║  └── ExtensionElem (activity)
│  ║     ║     ║        Var (name=v)
│  ║     ║     ╚══ Extension (new scope)
│  ║     ║        ├── StatementPattern (resultSizeEstimate=1.4M)
│  ║     ║        │     s: Var (name=post)
│  ║     ║        │     p: Var (name=_const_34211a22_uri, value=http://example.com/theme/social/authored, anonymous)
│  ║     ║        │     o: Var (name=u)
│  ║     ║        └── ExtensionElem (activity)
│  ║     ║              Var (name=post)
│  ║     ╚══ StatementPattern (resultSizeEstimate=16.0K) [right]
│  ║           s: Var (name=u)
│  ║           p: Var (name=_const_7d17b943_uri, value=http://example.com/theme/social/name, anonymous)
│  ║           o: Var (name=optName)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=activity)
└── ExtensionElem (count)
Count (Distinct)
Var (name=activity)
```

### Query 6

- Fastest observed: `0.069 ms/op`
- Source: [results-2026-04-09-2-full.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:5663)
- Optimized block: [1228](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:1228)

Optimized query:

```sparql
SELECT ?u (COUNT(DISTINCT ?v) AS ?connections) WHERE {
VALUES (?u ?v) {
(<http://example.com/theme/social/user/12> <http://example.com/theme/social/user/12>)
(<http://example.com/theme/social/user/12> <http://example.com/theme/social/user/13>)
(<http://example.com/theme/social/user/12> <http://example.com/theme/social/user/14>)
(<http://example.com/theme/social/user/12> <http://example.com/theme/social/user/15>)
(<http://example.com/theme/social/user/12> <http://example.com/theme/social/user/16>)
(<http://example.com/theme/social/user/12> <http://example.com/theme/social/user/17>)
(<http://example.com/theme/social/user/13> <http://example.com/theme/social/user/12>)
(<http://example.com/theme/social/user/13> <http://example.com/theme/social/user/13>)
(<http://example.com/theme/social/user/13> <http://example.com/theme/social/user/14>)
(<http://example.com/theme/social/user/13> <http://example.com/theme/social/user/15>)
(<http://example.com/theme/social/user/13> <http://example.com/theme/social/user/16>)
(<http://example.com/theme/social/user/13> <http://example.com/theme/social/user/17>)
(<http://example.com/theme/social/user/14> <http://example.com/theme/social/user/12>)
(<http://example.com/theme/social/user/14> <http://example.com/theme/social/user/13>)
(<http://example.com/theme/social/user/14> <http://example.com/theme/social/user/14>)
(<http://example.com/theme/social/user/14> <http://example.com/theme/social/user/15>)
(<http://example.com/theme/social/user/14> <http://example.com/theme/social/user/16>)
(<http://example.com/theme/social/user/14> <http://example.com/theme/social/user/17>)
(<http://example.com/theme/social/user/15> <http://example.com/theme/social/user/12>)
(<http://example.com/theme/social/user/15> <http://example.com/theme/social/user/13>)
(<http://example.com/theme/social/user/15> <http://example.com/theme/social/user/14>)
(<http://example.com/theme/social/user/15> <http://example.com/theme/social/user/15>)
(<http://example.com/theme/social/user/15> <http://example.com/theme/social/user/16>)
(<http://example.com/theme/social/user/15> <http://example.com/theme/social/user/17>)
(<http://example.com/theme/social/user/16> <http://example.com/theme/social/user/12>)
(<http://example.com/theme/social/user/16> <http://example.com/theme/social/user/13>)
(<http://example.com/theme/social/user/16> <http://example.com/theme/social/user/14>)
(<http://example.com/theme/social/user/16> <http://example.com/theme/social/user/15>)
(<http://example.com/theme/social/user/16> <http://example.com/theme/social/user/16>)
(<http://example.com/theme/social/user/16> <http://example.com/theme/social/user/17>)
(<http://example.com/theme/social/user/17> <http://example.com/theme/social/user/12>)
(<http://example.com/theme/social/user/17> <http://example.com/theme/social/user/13>)
(<http://example.com/theme/social/user/17> <http://example.com/theme/social/user/14>)
(<http://example.com/theme/social/user/17> <http://example.com/theme/social/user/15>)
(<http://example.com/theme/social/user/17> <http://example.com/theme/social/user/16>)
(<http://example.com/theme/social/user/17> <http://example.com/theme/social/user/17>)
}
FILTER (?u != ?v)
?u <http://example.com/theme/social/follows> ?v .
OPTIONAL {
?u <http://example.com/theme/social/name> ?optName .
}
FILTER (?optName != "")
}
GROUP BY ?u
HAVING (COUNT(DISTINCT ?v) >= 5)
```

Query plan:

_Not present in fastest-run source file._

### Query 7

- Fastest observed: `4.841 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:21)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 8

- Fastest observed: `667.115 ms/op`
- Source: [results-2026-04-16-5.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-5.md:22)
- Optimized block: [2442](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-5.md:2442)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?a) AS ?count) WHERE {
  ?a <http://example.com/theme/social/follows> ?b .
  ?b <http://example.com/theme/social/follows> ?c .
  ?c <http://example.com/theme/social/follows> ?a .
  BIND(?a AS ?cycleStart)
  OPTIONAL {
    ?a <http://example.com/theme/social/name> ?optName .
  }
  FILTER (?optName IN ("user0", "user1", "user2"))
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
   ├── Group ()
   │  ╠══ Filter
   │  ║  ├── ListMemberOperator
   │  ║  │     Var (name=optName)
   │  ║  │     ValueConstant (value="user0")
   │  ║  │     ValueConstant (value="user1")
   │  ║  │     ValueConstant (value="user2")
   │  ║  └── LeftJoin
   │  ║     ╠══ Extension [left]
   │  ║     ║  ├── Join (JoinIterator) (resultSizeEstimate=188738.0M)
   │  ║     ║  │  ╠══ Join (JoinIterator) (resultSizeEstimate=1.3M) [left]
   │  ║     ║  │  ║  ├── StatementPattern (costEstimate=47.9K, resultSizeEstimate=143.7K) [left]
   │  ║     ║  │  ║  │     s: Var (name=a)
   │  ║     ║  │  ║  │     p: Var (name=_const_9c68e12a_uri, value=http://example.com/theme/social/follows, anonymous)
   │  ║     ║  │  ║  │     o: Var (name=b)
   │  ║     ║  │  ║  └── StatementPattern (costEstimate=190, resultSizeEstimate=143.7K) [right]
   │  ║     ║  │  ║        s: Var (name=b)
   │  ║     ║  │  ║        p: Var (name=_const_9c68e12a_uri, value=http://example.com/theme/social/follows, anonymous)
   │  ║     ║  │  ║        o: Var (name=c)
   │  ║     ║  │  ╚══ StatementPattern (costEstimate=0.50, resultSizeEstimate=143.7K) [right]
   │  ║     ║  │        s: Var (name=c)
   │  ║     ║  │        p: Var (name=_const_9c68e12a_uri, value=http://example.com/theme/social/follows, anonymous)
   │  ║     ║  │        o: Var (name=a)
   │  ║     ║  └── ExtensionElem (cycleStart)
   │  ║     ║        Var (name=a)
   │  ║     ╚══ StatementPattern (resultSizeEstimate=16.0K) [right]
   │  ║           s: Var (name=a)
   │  ║           p: Var (name=_const_7d17b943_uri, value=http://example.com/theme/social/name, anonymous)
   │  ║           o: Var (name=optName)
   │  ╚══ GroupElem (count)
   │        Count (Distinct)
   │           Var (name=a)
   └── ExtensionElem (count)
         Count (Distinct)
            Var (name=a)
```

### Query 9

- Fastest observed: `5.792 ms/op`
- Source: [results-2026-04-09-2-full.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:5666)
- Optimized block: [1494](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:1494)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?a) AS ?count) WHERE {
VALUES ?b { <http://example.com/theme/social/user/3> <http://example.com/theme/social/user/4> <http://example.com/theme/social/user/5> <http://example.com/theme/social/user/6> }
?a <http://example.com/theme/social/follows> ?b .
FILTER (?a != ?b)
?b <http://example.com/theme/social/follows> ?c .
FILTER (?b != ?c)
?c <http://example.com/theme/social/follows> ?d .
FILTER (?c != ?d)
FILTER (?d != ?a)
?d <http://example.com/theme/social/follows> ?a .
OPTIONAL {
?b <http://example.com/theme/social/name> ?optName .
BIND(?optName AS ?optAlias)
}
FILTER (?optAlias != "")
}
```

Query plan:

_Not present in fastest-run source file._

### Query 10

- Fastest observed: `0.243 ms/op`
- Source: [results-2026-04-09-2-full.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:5667)
- Optimized block: [1586](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-09-2-full.md:1586)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?a) AS ?count) WHERE {
VALUES (?a ?b) {
(<http://example.com/theme/social/user/7> <http://example.com/theme/social/user/7>)
(<http://example.com/theme/social/user/7> <http://example.com/theme/social/user/8>)
(<http://example.com/theme/social/user/7> <http://example.com/theme/social/user/9>)
(<http://example.com/theme/social/user/7> <http://example.com/theme/social/user/10>)
(<http://example.com/theme/social/user/7> <http://example.com/theme/social/user/11>)
(<http://example.com/theme/social/user/8> <http://example.com/theme/social/user/7>)
(<http://example.com/theme/social/user/8> <http://example.com/theme/social/user/8>)
(<http://example.com/theme/social/user/8> <http://example.com/theme/social/user/9>)
(<http://example.com/theme/social/user/8> <http://example.com/theme/social/user/10>)
(<http://example.com/theme/social/user/8> <http://example.com/theme/social/user/11>)
(<http://example.com/theme/social/user/9> <http://example.com/theme/social/user/7>)
(<http://example.com/theme/social/user/9> <http://example.com/theme/social/user/8>)
(<http://example.com/theme/social/user/9> <http://example.com/theme/social/user/9>)
(<http://example.com/theme/social/user/9> <http://example.com/theme/social/user/10>)
(<http://example.com/theme/social/user/9> <http://example.com/theme/social/user/11>)
(<http://example.com/theme/social/user/10> <http://example.com/theme/social/user/7>)
(<http://example.com/theme/social/user/10> <http://example.com/theme/social/user/8>)
(<http://example.com/theme/social/user/10> <http://example.com/theme/social/user/9>)
(<http://example.com/theme/social/user/10> <http://example.com/theme/social/user/10>)
(<http://example.com/theme/social/user/10> <http://example.com/theme/social/user/11>)
(<http://example.com/theme/social/user/11> <http://example.com/theme/social/user/7>)
(<http://example.com/theme/social/user/11> <http://example.com/theme/social/user/8>)
(<http://example.com/theme/social/user/11> <http://example.com/theme/social/user/9>)
(<http://example.com/theme/social/user/11> <http://example.com/theme/social/user/10>)
(<http://example.com/theme/social/user/11> <http://example.com/theme/social/user/11>)
}
FILTER (?a != ?b)
VALUES ?c { <http://example.com/theme/social/user/7> <http://example.com/theme/social/user/8> <http://example.com/theme/social/user/9> <http://example.com/theme/social/user/10> <http://example.com/theme/social/user/11> }
FILTER (?b != ?c)
FILTER (?a != ?c)
VALUES ?d { <http://example.com/theme/social/user/7> <http://example.com/theme/social/user/8> <http://example.com/theme/social/user/9> <http://example.com/theme/social/user/10> <http://example.com/theme/social/user/11> }
FILTER (?c != ?d)
VALUES ?e { <http://example.com/theme/social/user/7> <http://example.com/theme/social/user/8> <http://example.com/theme/social/user/9> <http://example.com/theme/social/user/10> <http://example.com/theme/social/user/11> }
FILTER (?d != ?e)
?a <http://example.com/theme/social/follows> ?b .
?b <http://example.com/theme/social/follows> ?c .
?c <http://example.com/theme/social/follows> ?d .
?d <http://example.com/theme/social/follows> ?e .
?e <http://example.com/theme/social/follows> ?a .
OPTIONAL {
?e <http://example.com/theme/social/name> ?optName .
}
FILTER ((?optName IN ("user7", "user8", "user9", "user10", "user11")) && EXISTS { ?a <http://example.com/theme/social/name> ?name . FILTER ((?name = "user7") || (?name = "user8")) })
}
```

Query plan:

_Not present in fastest-run source file._

## TRAIN

### Query 0

- Fastest observed: `31.827 ms/op`
- Source: [results-main-branch.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-main-branch.md:58)
- Optimized block: not present in fastest-run source file

Optimized query:

_Not present in fastest-run source file._

Query plan:

_Not present in fastest-run source file._

### Query 1

- Fastest observed: `39.073 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:59)
- Optimized block: [6597](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:6597)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE {
{
VALUES ?target { "OP 1" "OP 2" }
{
{
?entity a <http://example.com/theme/train/OperationalPoint> .
?entity <http://example.com/theme/train/name> ?name .
}
}
FILTER ((?name = ?target) || (?name = "OP 3"))
}
UNION
{
VALUES ?target { "OP 1" "OP 2" }
{
{
?entity a <http://example.com/theme/train/Line> .
?entity <http://example.com/theme/train/name> ?name .
}
}
FILTER ((?name = ?target) || (?name = "OP 3"))
}
OPTIONAL {
?entity <http://example.com/theme/train/connectsOperationalPoint> ?op .
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ LeftJoin
│  ║  ├── Union [left]
│  ║  │  ╠══ Filter
│  ║  │  ║  ├── Or
│  ║  │  ║  │  ╠══ Compare (=)
│  ║  │  ║  │  ║     Var (name=name)
│  ║  │  ║  │  ║     Var (name=target)
│  ║  │  ║  │  ╚══ Compare (=)
│  ║  │  ║  │        Var (name=name)
│  ║  │  ║  │        ValueConstant (value="OP 3")
│  ║  │  ║  └── Join (HashJoinIteration) (resultSizeEstimate=30.7K)
│  ║  │  ║     ╠══ BindingSetAssignment ([[target="OP 1"], [target="OP 2"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │  ║     ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=30.7K) [right]
│  ║  │  ║        ├── StatementPattern (costEstimate=89.7K, resultSizeEstimate=29.9K) [left]
│  ║  │  ║        │     s: Var (name=entity)
│  ║  │  ║        │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │  ║        │     o: Var (name=_const_9807bf0f_uri, value=http://example.com/theme/train/OperationalPoint, anonymous)
│  ║  │  ║        └── StatementPattern (costEstimate=217, resultSizeEstimate=47.0K) [right]
│  ║  │  ║              s: Var (name=entity)
│  ║  │  ║              p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
│  ║  │  ║              o: Var (name=name)
│  ║  │  ╚══ Filter
│  ║  │     ├── Or
│  ║  │     │  ╠══ Compare (=)
│  ║  │     │  ║     Var (name=name)
│  ║  │     │  ║     Var (name=target)
│  ║  │     │  ╚══ Compare (=)
│  ║  │     │        Var (name=name)
│  ║  │     │        ValueConstant (value="OP 3")
│  ║  │     └── Join (HashJoinIteration) (resultSizeEstimate=8.9K)
│  ║  │        ╠══ BindingSetAssignment ([[target="OP 1"], [target="OP 2"]]) (costEstimate=6.00, resultSizeEstimate=1.00) [left]
│  ║  │        ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=8.9K) [right]
│  ║  │           ├── StatementPattern (costEstimate=379.7M, resultSizeEstimate=8.5K) [left]
│  ║  │           │     s: Var (name=entity)
│  ║  │           │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │           │     o: Var (name=_const_cef39ba5_uri, value=http://example.com/theme/train/Line, anonymous)
│  ║  │           └── StatementPattern (costEstimate=217, resultSizeEstimate=47.0K) [right]
│  ║  │                 s: Var (name=entity)
│  ║  │                 p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
│  ║  │                 o: Var (name=name)
│  ║  └── StatementPattern (resultSizeEstimate=134.8K) [right]
│  ║        s: Var (name=entity)
│  ║        p: Var (name=_const_26ff10d8_uri, value=http://example.com/theme/train/connectsOperationalPoint, anonymous)
│  ║        o: Var (name=op)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=entity)
└── ExtensionElem (count)
Count (Distinct)
Var (name=entity)
```

### Query 2

- Fastest observed: `0.111 ms/op`
- Source: [results-2026-04-22.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-22.md:61)
- Optimized block: [6050](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-22.md:6050)

Optimized query:

```sparql
SELECT ?line (COUNT(DISTINCT ?section) AS ?sectionCount) WHERE {
  VALUES ?lineName { "Line 0" "Line 1" "Line 2" }
  ?line <http://example.com/theme/train/name> ?lineName .
  FILTER (?lineName IN ("Line 0", "Line 1", "Line 2"))
  ?line a <http://example.com/theme/train/Line> .
  OPTIONAL {
    ?section <http://example.com/theme/train/partOfLine> ?line .
  }
}
GROUP BY ?line
HAVING (COUNT(?section) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 3

- Fastest observed: `143.416 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:61)
- Optimized block: [6824](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:6824)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?section) AS ?count) WHERE {
?section a <http://example.com/theme/train/SectionOfLine> .
?section <http://example.com/theme/train/partOfLine> ?line .
OPTIONAL {
?section <http://example.com/theme/train/hasTrackSection> ?track .
BIND(?track AS ?optTrack)
}
FILTER (?optTrack != ?section)
MINUS {
?line <http://example.com/theme/train/name> ?name .
FILTER (CONTAINS(STR(?name), "Line 0"))
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Difference
│  ║  ├── Filter
│  ║  │  ╠══ Compare (!=)
│  ║  │  ║     Var (name=optTrack)
│  ║  │  ║     Var (name=section)
│  ║  │  ╚══ LeftJoin
│  ║  │     ├── Join (JoinIterator) (resultSizeEstimate=67.5K) [left]
│  ║  │     │  ╠══ StatementPattern (costEstimate=33.7K, resultSizeEstimate=67.4K) [left]
│  ║  │     │  ║     s: Var (name=section)
│  ║  │     │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║  │     │  ║     o: Var (name=_const_b0bb051f_uri, value=http://example.com/theme/train/SectionOfLine, anonymous)
│  ║  │     │  ╚══ StatementPattern (costEstimate=260, resultSizeEstimate=67.4K) [right]
│  ║  │     │        s: Var (name=section)
│  ║  │     │        p: Var (name=_const_8ba830f_uri, value=http://example.com/theme/train/partOfLine, anonymous)
│  ║  │     │        o: Var (name=line)
│  ║  │     └── Extension [right]
│  ║  │        ╠══ StatementPattern (resultSizeEstimate=67.4K)
│  ║  │        ║     s: Var (name=section)
│  ║  │        ║     p: Var (name=_const_5289cea3_uri, value=http://example.com/theme/train/hasTrackSection, anonymous)
│  ║  │        ║     o: Var (name=track)
│  ║  │        ╚══ ExtensionElem (optTrack)
│  ║  │              Var (name=track)
│  ║  └── Filter (new scope)
│  ║     ╠══ FunctionCall (http://www.w3.org/2005/xpath-functions#contains)
│  ║     ║  ├── Str
│  ║     ║  │     Var (name=name)
│  ║     ║  └── ValueConstant (value="Line 0")
│  ║     ╚══ StatementPattern (resultSizeEstimate=47.0K)
│  ║           s: Var (name=line)
│  ║           p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
│  ║           o: Var (name=name)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=section)
└── ExtensionElem (count)
Count (Distinct)
Var (name=section)
```

### Query 4

- Fastest observed: `130.920 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:62)
- Optimized block: [6925](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:6925)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?line) AS ?count) WHERE {
?line a <http://example.com/theme/train/Line> .
?line <http://example.com/theme/train/name> ?name .
FILTER ((?name = "Line 1") || (?name = "Line 2"))
OPTIONAL {
?section <http://example.com/theme/train/connectsOperationalPoint> ?op .
}
FILTER EXISTS {
?section <http://example.com/theme/train/partOfLine> ?line .
}
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Filter
│  ║  ├── Exists
│  ║  │     StatementPattern (resultSizeEstimate=67.4K)
│  ║  │        s: Var (name=section)
│  ║  │        p: Var (name=_const_8ba830f_uri, value=http://example.com/theme/train/partOfLine, anonymous)
│  ║  │        o: Var (name=line)
│  ║  └── LeftJoin
│  ║     ╠══ Join (JoinIterator) (resultSizeEstimate=8.9K) [left]
│  ║     ║  ├── StatementPattern (costEstimate=4.2K, resultSizeEstimate=8.5K) [left]
│  ║     ║  │     s: Var (name=line)
│  ║     ║  │     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║     ║  │     o: Var (name=_const_cef39ba5_uri, value=http://example.com/theme/train/Line, anonymous)
│  ║     ║  └── Filter (costEstimate=216, resultSizeEstimate=46.6K) [right]
│  ║     ║     ╠══ Or
│  ║     ║     ║  ├── Compare (=)
│  ║     ║     ║  │     Var (name=name)
│  ║     ║     ║  │     ValueConstant (value="Line 1")
│  ║     ║     ║  └── Compare (=)
│  ║     ║     ║        Var (name=name)
│  ║     ║     ║        ValueConstant (value="Line 2")
│  ║     ║     ╚══ StatementPattern (resultSizeEstimate=47.0K)
│  ║     ║           s: Var (name=line)
│  ║     ║           p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
│  ║     ║           o: Var (name=name)
│  ║     ╚══ StatementPattern (resultSizeEstimate=134.8K) [right]
│  ║           s: Var (name=section)
│  ║           p: Var (name=_const_26ff10d8_uri, value=http://example.com/theme/train/connectsOperationalPoint, anonymous)
│  ║           o: Var (name=op)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=line)
└── ExtensionElem (count)
Count (Distinct)
Var (name=line)
```

### Query 5

- Fastest observed: `13.063 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:63)
- Optimized block: [3988](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:3988)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?service) AS ?count) WHERE {
  ?service <http://example.com/theme/train/scheduledTime> ?time .
  FILTER (?time IN ("08:00:00"^^<http://www.w3.org/2001/XMLSchema#time>, "09:00:00"^^<http://www.w3.org/2001/XMLSchema#time>))
  ?service a <http://example.com/theme/train/TrainService> .
  VALUES ?threshold { "10:00:00"^^<http://www.w3.org/2001/XMLSchema#time> }
  FILTER NOT EXISTS {
    ?service <http://example.com/theme/train/scheduledTime> ?late .
    FILTER (?late > ?threshold)
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 6

- Fastest observed: `81.059 ms/op`
- Source: [results-2026-04-16-3.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:64)
- Optimized block: [4046](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-3.md:4046)

Optimized query:

```sparql
SELECT ?line (COUNT(DISTINCT ?service) AS ?serviceCount) WHERE {
  {
    ?service a <http://example.com/theme/train/TrainService> .
    ?service <http://example.com/theme/train/runsOnSection> ?section .
    ?section <http://example.com/theme/train/partOfLine> ?line .
  }
  UNION
  {
    ?line a <http://example.com/theme/train/Line> .
  }
  OPTIONAL {
    ?line <http://example.com/theme/train/name> ?optName .
  }
  FILTER (?optName != "")
}
GROUP BY ?line
HAVING (COUNT(?service) > 0)
```

Query plan:

_Not present in fastest-run source file._

### Query 7

- Fastest observed: `36.313 ms/op`
- Source: [results-2026-04-16-4.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:65)
- Optimized block: [4109](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-4.md:4109)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?op) AS ?count) WHERE {
  ?op <http://example.com/theme/train/name> ?name .
  FILTER ((?name = "OP 1") || (?name = "OP 2"))
  ?op a <http://example.com/theme/train/OperationalPoint> .
  FILTER EXISTS {
    ?service <http://example.com/theme/train/passesThrough> ?op .
  }
  MINUS {
    ?op <http://example.com/theme/train/name> ?name2 .
    FILTER (CONTAINS(LCASE(STR(?name2)), "op 0"))
  }
}
```

Query plan:

_Not present in fastest-run source file._

### Query 8

- Fastest observed: `127.294 ms/op`
- Source: [results-2026-04-17.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:66)
- Optimized block: [7339](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-17.md:7339)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?service) AS ?count) WHERE {
?service <http://example.com/theme/train/runsOnSection> ?s1 .
?s1 <http://example.com/theme/train/partOfLine> ?line .
?service a <http://example.com/theme/train/TrainService> .
?service <http://example.com/theme/train/runsOnSection> ?s2 .
?s2 <http://example.com/theme/train/partOfLine> ?line .
OPTIONAL {
?line <http://example.com/theme/train/name> ?optName .
}
FILTER ((?optName IN ("Line 0", "Line 1")) && EXISTS { ?s1 <http://example.com/theme/train/connectsOperationalPoint> ?op . ?s2 <http://example.com/theme/train/connectsOperationalPoint> ?op . })
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "count"
╚══ Extension
├── Group ()
│  ╠══ Filter
│  ║  ├── And
│  ║  │  ╠══ ListMemberOperator
│  ║  │  ║     Var (name=optName)
│  ║  │  ║     ValueConstant (value="Line 0")
│  ║  │  ║     ValueConstant (value="Line 1")
│  ║  │  ╚══ Exists
│  ║  │        Join (JoinIterator) (resultSizeEstimate=595.5K)
│  ║  │        ╠══ StatementPattern (costEstimate=184, resultSizeEstimate=134.8K) [left]
│  ║  │        ║     s: Var (name=s1)
│  ║  │        ║     p: Var (name=_const_26ff10d8_uri, value=http://example.com/theme/train/connectsOperationalPoint, anonymous)
│  ║  │        ║     o: Var (name=op)
│  ║  │        ╚══ StatementPattern (costEstimate=0.50, resultSizeEstimate=134.8K) [right]
│  ║  │              s: Var (name=s2)
│  ║  │              p: Var (name=_const_26ff10d8_uri, value=http://example.com/theme/train/connectsOperationalPoint, anonymous)
│  ║  │              o: Var (name=op)
│  ║  └── LeftJoin
│  ║     ╠══ Join (JoinIterator) (resultSizeEstimate=5565.7M) [left]
│  ║     ║  ├── Join (JoinIterator) (resultSizeEstimate=82.6K) [left]
│  ║     ║  │  ╠══ Join (JoinIterator) (resultSizeEstimate=26.9K) [left]
│  ║     ║  │  ║  ├── Join (JoinIterator) (resultSizeEstimate=26.5K) [left]
│  ║     ║  │  ║  │  ╠══ StatementPattern (costEstimate=80, resultSizeEstimate=25.9K) [left]
│  ║     ║  │  ║  │  ║     s: Var (name=service)
│  ║     ║  │  ║  │  ║     p: Var (name=_const_9993352d_uri, value=http://example.com/theme/train/runsOnSection, anonymous)
│  ║     ║  │  ║  │  ║     o: Var (name=s1)
│  ║     ║  │  ║  │  ╚══ StatementPattern (costEstimate=130, resultSizeEstimate=67.4K) [right]
│  ║     ║  │  ║  │        s: Var (name=s1)
│  ║     ║  │  ║  │        p: Var (name=_const_8ba830f_uri, value=http://example.com/theme/train/partOfLine, anonymous)
│  ║     ║  │  ║  │        o: Var (name=line)
│  ║     ║  │  ║  └── StatementPattern (costEstimate=2.9K, resultSizeEstimate=8.6K) [right]
│  ║     ║  │  ║        s: Var (name=service)
│  ║     ║  │  ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
│  ║     ║  │  ║        o: Var (name=_const_a703e3e_uri, value=http://example.com/theme/train/TrainService, anonymous)
│  ║     ║  │  ╚══ StatementPattern (costEstimate=80, resultSizeEstimate=25.9K) [right]
│  ║     ║  │        s: Var (name=service)
│  ║     ║  │        p: Var (name=_const_9993352d_uri, value=http://example.com/theme/train/runsOnSection, anonymous)
│  ║     ║  │        o: Var (name=s2)
│  ║     ║  └── StatementPattern (costEstimate=0.50, resultSizeEstimate=67.4K) [right]
│  ║     ║        s: Var (name=s2)
│  ║     ║        p: Var (name=_const_8ba830f_uri, value=http://example.com/theme/train/partOfLine, anonymous)
│  ║     ║        o: Var (name=line)
│  ║     ╚══ StatementPattern (resultSizeEstimate=47.0K) [right]
│  ║           s: Var (name=line)
│  ║           p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
│  ║           o: Var (name=optName)
│  ╚══ GroupElem (count)
│        Count (Distinct)
│           Var (name=service)
└── ExtensionElem (count)
Count (Distinct)
Var (name=service)
```

### Query 9

- Fastest observed: `223.739 ms/op`
- Source: [results-2026-04-16-5.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-5.md:67)
- Optimized block: [7311](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16-5.md:7311)

Optimized query:

```sparql
SELECT ?section (COUNT(DISTINCT ?track) AS ?trackCount) WHERE {
  ?section <http://example.com/theme/train/hasTrackSection> ?track .
  FILTER EXISTS {
    ?track a <http://example.com/theme/train/TrackSection> .
  }
  ?section a <http://example.com/theme/train/SectionOfLine> .
  OPTIONAL {
    ?section <http://example.com/theme/train/connectsOperationalPoint> ?op .
    BIND(?op AS ?optOp)
  }
  FILTER (?optOp != ?section)
}
GROUP BY ?section
HAVING (COUNT(?track) > 0)
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "section"
║     ProjectionElem "trackCount"
╚══ Extension
   ├── Extension
   │  ╠══ Filter
   │  ║  ├── Compare (>)
   │  ║  │     Var (name=_anon_having_501ca9f329e637c40eeb518c2c2d3f6593601234, anonymous)
   │  ║  │     ValueConstant (value="0"^^<http://www.w3.org/2001/XMLSchema#integer>)
   │  ║  └── Group (section)
   │  ║        Filter
   │  ║        ├── Compare (!=)
   │  ║        │     Var (name=optOp)
   │  ║        │     Var (name=section)
   │  ║        └── LeftJoin
   │  ║           ╠══ Join (JoinIterator) (resultSizeEstimate=67.5K) [left]
   │  ║           ║  ├── Filter (costEstimate=33.1K, resultSizeEstimate=66.3K) [left]
   │  ║           ║  │  ╠══ Exists
   │  ║           ║  │  ║     StatementPattern (resultSizeEstimate=67.4K)
   │  ║           ║  │  ║        s: Var (name=track)
   │  ║           ║  │  ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║           ║  │  ║        o: Var (name=_const_585dd5cb_uri, value=http://example.com/theme/train/TrackSection, anonymous)
   │  ║           ║  │  ╚══ StatementPattern (costEstimate=260, resultSizeEstimate=67.4K)
   │  ║           ║  │        s: Var (name=section)
   │  ║           ║  │        p: Var (name=_const_5289cea3_uri, value=http://example.com/theme/train/hasTrackSection, anonymous)
   │  ║           ║  │        o: Var (name=track)
   │  ║           ║  └── StatementPattern (costEstimate=33.7K, resultSizeEstimate=67.4K) [right]
   │  ║           ║        s: Var (name=section)
   │  ║           ║        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║           ║        o: Var (name=_const_b0bb051f_uri, value=http://example.com/theme/train/SectionOfLine, anonymous)
   │  ║           ╚══ Extension [right]
   │  ║              ├── StatementPattern (resultSizeEstimate=134.8K)
   │  ║              │     s: Var (name=section)
   │  ║              │     p: Var (name=_const_26ff10d8_uri, value=http://example.com/theme/train/connectsOperationalPoint, anonymous)
   │  ║              │     o: Var (name=op)
   │  ║              └── ExtensionElem (optOp)
   │  ║                    Var (name=op)
   │  ║        GroupElem (_anon_having_501ca9f329e637c40eeb518c2c2d3f6593601234)
   │  ║           Count
   │  ║              Var (name=track)
   │  ║        GroupElem (trackCount)
   │  ║           Count (Distinct)
   │  ║              Var (name=track)
   │  ╚══ ExtensionElem (_anon_having_501ca9f329e637c40eeb518c2c2d3f6593601234)
   │        Count
   │           Var (name=track)
   └── ExtensionElem (trackCount)
         Count (Distinct)
            Var (name=track)
```

### Query 10

- Fastest observed: `176.286 ms/op`
- Source: [results-2026-04-16.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:68)
- Optimized block: [4289](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-16.md:4289)

Optimized query:

```sparql
SELECT (COUNT(DISTINCT ?op) AS ?count) WHERE {
{
?op a <http://example.com/theme/train/OperationalPoint> .
}
UNION
{
?op a <http://example.com/theme/train/OperationalPoint> .
?op <http://example.com/theme/train/name> ?name .
}
OPTIONAL {
?section <http://example.com/theme/train/connectsOperationalPoint> ?op .
BIND(?section AS ?optSection)
}
FILTER (?optSection != ?op)
MINUS {
?op <http://example.com/theme/train/name> ?name2 .
FILTER (CONTAINS(LCASE(STR(?name2)), "op 1"))
}
}
```

Query plan:

_Not present in fastest-run source file._

### Query 12

- Fastest observed: `1935.462 ms/op`
- Source: [results-2026-04-18.md](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:82)
- Optimized block: [8883](/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-04-18.md:8883)

Optimized query:

```sparql
SELECT ?root ?optName ?optLabel ?optLine ?optLineName ?optOperationalPoint ?optOperationalPointName ?service ?optOperationalPoint2 ?optScheduledTime ?optTrackSection WHERE {
  {
    ?root a <http://example.com/theme/train/Line> .
  }
  UNION
  {
    ?root a <http://example.com/theme/train/SectionOfLine> .
  }
  OPTIONAL {
    ?root <http://example.com/theme/train/name> ?optName .
    BIND(?optName AS ?optLabel)
  }
  OPTIONAL {
    ?root <http://example.com/theme/train/partOfLine> ?optLine .
    OPTIONAL {
      ?optLine <http://example.com/theme/train/name> ?optLineName .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/train/connectsOperationalPoint> ?optOperationalPoint .
    OPTIONAL {
      ?optOperationalPoint <http://example.com/theme/train/name> ?optOperationalPointName .
    }
  }
  OPTIONAL {
    {
      ?service <http://example.com/theme/train/runsOnSection> ?root .
    }
    UNION
    {
      ?root <http://example.com/theme/train/connectsOperationalPoint> ?optOperationalPoint2 .
      ?service <http://example.com/theme/train/passesThrough> ?optOperationalPoint2 .
    }
    OPTIONAL {
      ?service <http://example.com/theme/train/scheduledTime> ?optScheduledTime .
    }
  }
  OPTIONAL {
    ?root <http://example.com/theme/train/hasTrackSection> ?optTrackSection .
  }
}
```

Query plan:

```text
Projection
╠══ ProjectionElemList
║     ProjectionElem "root"
║     ProjectionElem "optName"
║     ProjectionElem "optLabel"
║     ProjectionElem "optLine"
║     ProjectionElem "optLineName"
║     ProjectionElem "optOperationalPoint"
║     ProjectionElem "optOperationalPointName"
║     ProjectionElem "service"
║     ProjectionElem "optOperationalPoint2"
║     ProjectionElem "optScheduledTime"
║     ProjectionElem "optTrackSection"
╚══ LeftJoin (LeftJoinIterator)
   ├── LeftJoin (LeftJoinIterator) [left]
   │  ╠══ LeftJoin (LeftJoinIterator) [left]
   │  ║  ├── LeftJoin (LeftJoinIterator) [left]
   │  ║  │  ╠══ LeftJoin (LeftJoinIterator) [left]
   │  ║  │  ║  ├── Union [left]
   │  ║  │  ║  │  ╠══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=8.5K, indexName=ospc)
   │  ║  │  ║  │  ║     s: Var (name=root) (bindingState=unbound)
   │  ║  │  ║  │  ║     p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │  ║  │  ║     o: Var (name=_const_cef39ba5_uri, value=http://example.com/theme/train/Line, anonymous)
   │  ║  │  ║  │  ╚══ StatementPattern [index: ospc]  (new scope) (resultSizeEstimate=67.4K, indexName=ospc)
   │  ║  │  ║  │        s: Var (name=root) (bindingState=unbound)
   │  ║  │  ║  │        p: Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)
   │  ║  │  ║  │        o: Var (name=_const_b0bb051f_uri, value=http://example.com/theme/train/SectionOfLine, anonymous)
   │  ║  │  ║  └── Extension [right]
   │  ║  │  ║     ╠══ StatementPattern (resultSizeEstimate=47.0K)
   │  ║  │  ║     ║     s: Var (name=root) (bindingState=bound)
   │  ║  │  ║     ║     p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
   │  ║  │  ║     ║     o: Var (name=optName) (bindingState=unbound)
   │  ║  │  ║     ╚══ ExtensionElem (optLabel)
   │  ║  │  ║           Var (name=optName) (bindingState=bound)
   │  ║  │  ╚══ LeftJoin [right]
   │  ║  │     ├── StatementPattern (resultSizeEstimate=67.4K) [left]
   │  ║  │     │     s: Var (name=root) (bindingState=bound)
   │  ║  │     │     p: Var (name=_const_8ba830f_uri, value=http://example.com/theme/train/partOfLine, anonymous)
   │  ║  │     │     o: Var (name=optLine) (bindingState=unbound)
   │  ║  │     └── StatementPattern (resultSizeEstimate=47.0K) [right]
   │  ║  │           s: Var (name=optLine) (bindingState=bound)
   │  ║  │           p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
   │  ║  │           o: Var (name=optLineName) (bindingState=unbound)
   │  ║  └── LeftJoin [right]
   │  ║     ╠══ StatementPattern (resultSizeEstimate=134.8K) [left]
   │  ║     ║     s: Var (name=root) (bindingState=bound)
   │  ║     ║     p: Var (name=_const_26ff10d8_uri, value=http://example.com/theme/train/connectsOperationalPoint, anonymous)
   │  ║     ║     o: Var (name=optOperationalPoint) (bindingState=unbound)
   │  ║     ╚══ StatementPattern (resultSizeEstimate=47.0K) [right]
   │  ║           s: Var (name=optOperationalPoint) (bindingState=bound)
   │  ║           p: Var (name=_const_cf02f21c_uri, value=http://example.com/theme/train/name, anonymous)
   │  ║           o: Var (name=optOperationalPointName) (bindingState=unbound)
   │  ╚══ LeftJoin [right]
   │     ├── Union [left]
   │     │  ╠══ StatementPattern (new scope) (resultSizeEstimate=25.9K)
   │     │  ║     s: Var (name=service) (bindingState=unbound)
   │     │  ║     p: Var (name=_const_9993352d_uri, value=http://example.com/theme/train/runsOnSection, anonymous)
   │     │  ║     o: Var (name=root) (bindingState=bound)
   │     │  ╚══ Join (new scope) (JoinIterator) (resultSizeEstimate=117.0K)
   │     │     ├── StatementPattern (costEstimate=184, resultSizeEstimate=134.8K) [left]
   │     │     │     s: Var (name=root) (bindingState=bound)
   │     │     │     p: Var (name=_const_26ff10d8_uri, value=http://example.com/theme/train/connectsOperationalPoint, anonymous)
   │     │     │     o: Var (name=optOperationalPoint2) (bindingState=unbound)
   │     │     └── StatementPattern (costEstimate=161, resultSizeEstimate=25.9K) [right]
   │     │           s: Var (name=service) (bindingState=unbound)
   │     │           p: Var (name=_const_b4130d5_uri, value=http://example.com/theme/train/passesThrough, anonymous)
   │     │           o: Var (name=optOperationalPoint2) (bindingState=bound)
   │     └── StatementPattern (resultSizeEstimate=25.8K) [right]
   │           s: Var (name=service) (bindingState=bound)
   │           p: Var (name=_const_4f78e4a9_uri, value=http://example.com/theme/train/scheduledTime, anonymous)
   │           o: Var (name=optScheduledTime) (bindingState=unbound)
   └── StatementPattern (resultSizeEstimate=67.4K) [right]
         s: Var (name=root) (bindingState=bound)
         p: Var (name=_const_5289cea3_uri, value=http://example.com/theme/train/hasTrackSection, anonymous)
         o: Var (name=optTrackSection) (bindingState=unbound)
```
