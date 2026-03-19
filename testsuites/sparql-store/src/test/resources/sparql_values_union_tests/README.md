# SPARQL VALUES/UNION regression suite

This suite contains 128 tests for optimizers that rewrite a trailing query-level
VALUES clause around a UNION.

Each test case has:
- `data.ttl` — default-graph input data
- `query.rq` — the SPARQL query
- `meta.json` — machine-readable metadata for the case

Some disputed cases also carry oracle metadata:
- `oracle_winner` — `jena` or `rdf4j`
- `oracle_basis` — spec basis for the ruling, including confirmed erratum `#215` where relevant

## Intended use

Run each query twice against the same `data.ttl`:
1. with your optimizer disabled
2. with your optimizer enabled

The results should be identical.

## Comparison advice

Compare results as multisets (bags), not just as sets.
Many tests intentionally depend on duplicate preservation.

Most non-aggregate SELECT queries include `ORDER BY` to make textual comparison
easier. Three empty-VALUES edge cases intentionally omit `ORDER BY`.

## Coverage focus

The suite is designed around the semantics that matter for pushing a trailing
VALUES clause through UNION and then potentially deeper into each union arm:

- late binding on arms that do not mention the pushed variable
- multi-column row correlation
- duplicate VALUES rows and bag semantics
- UNDEF / unbound handling
- FILTER and function/error sensitivity
- EXISTS / NOT EXISTS substitution sensitivity
- OPTIONAL / LeftJoin barriers
- MINUS compatibility traps
- BIND / Extend barriers, including target-variable collisions
- subquery scope, hidden variables, aggregation, DISTINCT, HAVING, ORDER/LIMIT
- nested UNION trees
- interaction with inner VALUES blocks
- property path arms
- positive controls where early visibility is safe because the arm already binds the variable(s)
- combined stress cases

## Directory layout

- `manifest.csv`
- `manifest.json`
- `cases/T001/...` through `cases/T128/...`

## Notes

Tests `T113`, `T114`, and `T119` use an empty VALUES table.
They are intentional edge cases.

Oracle metadata is currently recorded for the 24 known Jena/RDF4J disagreement
cases. Aggregate/modifier disputes that depend on the trailing-VALUES ordering
fix are marked with confirmed erratum `#215`.

Because each test is defined as a Turtle default-graph input plus a single query,
this suite focuses on the relevant default-graph semantics rather than named-graph,
SERVICE, or entailment-regime specific behavior.
