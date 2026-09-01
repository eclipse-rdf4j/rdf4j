# Frontier OmniSketch theme coverage inventory

This note records a static inventory of the RDF4J theme benchmark corpus for
Frontier OmniSketch coverage planning. It is a design and test-planning
artifact, not evidence that the estimator supports or accurately estimates any
listed shape.

## Provenance and method

The inventory was derived on 2026-07-25 from:

- `ThemeQueryCatalog` and its factored `SparseThemeQueries`;
- the nine values of `ThemeDataSetGenerator.Theme`; and
- RDF4J's parsed tuple algebra for every catalog query.

The catalog contains 13 queries for each of nine themes, or 117 queries total.
Counts below describe query presence, not operator occurrences. A query can
contribute to several columns.

- **Multi-star** means at least two subject-centred stars.
- **Multi-bridge** means at least two written subject-to-object bridges whose
  object is also a subject centre.
- **Reciprocal** means an explicit bridge pair in both directions.
- **Discarded filter** is a conservative textual candidate: a filter mentions a
  variable absent from the final projection. Required-frontier analysis must
  decide whether the implementation actually discards it too early.
- **Fixed path** means a finite property-path sequence. It lowers to ordinary
  joins and is not an arbitrary-length path.

## Corpus summary

| Theme | Multi-star | Multi-bridge | Reciprocal | Fork | Cycle | Repeated variable | OPTIONAL | UNION | EXISTS | NOT EXISTS | MINUS | Discarded filter | DISTINCT | Aggregate | Fixed path |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Medical records | 4 | 6 | 0 | 9 | 0 | 0 | 11 | 4 | 3 | 2 | 3 | 11 | 13 | 11 | 1 |
| Social media | 4 | 9 | 5 | 8 | 9 | 3 | 12 | 2 | 4 | 1 | 2 | 11 | 13 | 11 | 0 |
| Library | 4 | 5 | 0 | 6 | 0 | 0 | 11 | 3 | 2 | 2 | 3 | 11 | 13 | 11 | 0 |
| Engineering | 3 | 4 | 0 | 5 | 0 | 0 | 11 | 3 | 3 | 1 | 3 | 11 | 13 | 11 | 0 |
| Highly connected | 2 | 8 | 6 | 4 | 8 | 1 | 10 | 4 | 3 | 2 | 3 | 11 | 12 | 11 | 0 |
| Train | 5 | 4 | 0 | 5 | 0 | 0 | 11 | 4 | 4 | 1 | 3 | 11 | 12 | 11 | 0 |
| Electrical grid | 4 | 5 | 0 | 6 | 0 | 0 | 11 | 4 | 3 | 2 | 3 | 11 | 12 | 11 | 0 |
| Pharma | 10 | 12 | 1 | 11 | 1 | 0 | 13 | 3 | 4 | 2 | 2 | 11 | 11 | 10 | 0 |
| Sparse | 11 | 12 | 0 | 12 | 0 | 0 | 12 | 5 | 0 | 1 | 0 | 3 | 4 | 13 | 0 |
| **Total of 117** | **47** | **65** | **12** | **66** | **18** | **4** | **102** | **32** | **26** | **14** | **22** | **91** | **103** | **100** | **1** |

The largest catalog queries contain:

- 20 statement patterns (`PHARMA` query 11);
- six subject-centred stars (`ELECTRICAL_GRID` query 11);
- nine bridges (`SPARSE` query 12); and
- 15 `OPTIONAL` operators (`PHARMA` query 11).

The catalog contains no arbitrary-length property path. `MEDICAL_RECORDS`
query 3 contains the sole fixed sequence and lowers to two statement patterns.

## Root degradation implication

Of the 117 queries, 113 contain `DISTINCT`, an aggregate, or both:

| Root feature class | Query count |
| --- | ---: |
| `DISTINCT` and aggregate | 90 |
| `DISTINCT` only | 13 |
| Aggregate only | 10 |
| Neither | 4 |

The theorem-safe first phase therefore cannot treat full-query support as a
proxy for relational-frontier support. Generic projected-key `DISTINCT`,
`COUNT DISTINCT`, and nonlinear aggregation remain explicitly degraded while
supported descendant stars, bridges, filters, and SPARQL operators retain
their evidence states.

The four candidates without those root blockers are:

- `HIGHLY_CONNECTED` query 12;
- `TRAIN` query 12;
- `PHARMA` query 3; and
- `PHARMA` query 12.

They still exercise combinations of `UNION`, `OPTIONAL`, `NOT EXISTS`,
discarded-filter candidates, reciprocal bridges, and cycles. Support remains
conditional on mask handling, exact probes, and retaining required cycle
endpoints.

## Proposed generated coverage harness

Add a generated `LmdbThemeFrontierCoverageIT` rather than reviving the disabled
catalog-specific `LmdbThemeQueryRegressionIT`.

1. Group dynamic tests by theme so one prepared store serves all 13 queries.
2. Obtain the query and expected result from
   `ThemeQueryCatalog.benchmarkQueryFor(theme, index)`.
3. Reuse `BenchmarkJoinEstimatorSupport.prepareThemeRegressionStore(...)`.
4. Execute the query for semantic correctness.
5. Extend `LmdbEstimateAuditHarness`, which already parses, explains, executes,
   and audits the full query and every tuple-algebra piece, to carry Frontier
   summary fields in `PlanEstimate` and `AuditRow`.
6. Add typed planned telemetry for state presence, required frontier,
   guarantee, zero status, interval kind, lower/upper rows, confidence, and
   degradation reason. A query-local state ID may be exposed only ephemerally;
   it must never enter a cached physical plan recipe.
7. Distinguish `EXISTS` and `NOT EXISTS` conditions inside `Filter`;
   `LeftJoin` already identifies `OPTIONAL`, and `Difference` identifies
   `MINUS`.

For every full query and every algebra piece, require exactly one outcome:

- a state carrying `DATABASE_EXACT` or `MEASURE_UNBIASED`, with internally
  consistent interval and zero metadata; or
- `CERTIFIED_BOUND_ONLY`, `SCALAR_FALLBACK`, or `UNRESOLVED`, with a nonblank
  stable degradation reason.

Only `DATABASE_EXACT` may assert `EXACT_ZERO`. Descendants must be checked
independently so a degraded `DISTINCT` or aggregate root cannot hide missing
star or bridge evidence.

## Missing minimal synthetic shapes

The theme corpus is broad but usually wraps relational shapes in projection,
aggregation, or optional structure. Add the following smallest isolating
tests:

1. A bare two-star, one-bridge BGP with no blocking wrapper.
2. Bare three-star, two-bridge chain and fork variants, including a reverse
   object-to-subject start.
3. A diamond fork/merge, `a -> b`, `a -> c`, `b -> d`, `c -> d`, that requires
   an exact probe, independent lanes, or explicit degradation.
4. A width-two tuple frontier across a bridge, including a shared leaf
   variable that cannot be replaced by independent degree products.
5. An unmatched `OPTIONAL` outer row whose live RHS variable feeds a later
   factor, plus nested `OPTIONAL` with mixed masks.
6. Separate minimal `EXISTS`, `NOT EXISTS`, and `MINUS` cases with unbound RHS
   variables; include empty-domain overlap and duplicate-producing RHS cases.
7. A filter whose required value would otherwise be projected away, plus a
   filter over mixed optional masks.
8. Exact cycle closing with both endpoints retained, paired with the same
   cycle after one endpoint is discarded.
9. Inverse, alternative, optional, transitive, and negated property paths:
   `^p`, `p|q`, `p?`, `p*`, `p+`, and a negated property set.
10. Projected-key `DISTINCT`, `COUNT DISTINCT`, a nonlinear aggregate, and a
    nested `AVG` subquery as separate degradation contracts.
11. A disconnected Cartesian BGP and an unavoidable random-random bushy join.
12. Named `GRAPH` and explicit-versus-inferred context behavior.
13. A bound predicate, a repeated predicate-position variable, and a positive
    repeated subject/object variable.
14. `UNION` branches with incompatible bound masks feeding a later bridge.
15. Existence-probe and refinement exhaustion, including sampled zero with a
    positive certified upper bound.

## Stable degradation reason taxonomy

Use named constants or an enum rather than ad hoc prose. The initial taxonomy
needed by the corpus and synthetic tests is:

| Stable reason | Meaning |
| --- | --- |
| `distinct-not-composable` | Generic projected-key `DISTINCT` lacks a proved frontier transform. |
| `count-distinct-not-supported` | `COUNT DISTINCT` cannot be obtained by Booleanizing particle weights. |
| `nonlinear-aggregate-not-supported` | The aggregate is not a supported linear measure transform. |
| `filter-value-not-retained` | A required filter value is absent from the retained frontier. |
| `cycle-endpoint-not-retained` | Exact cycle closing cannot probe both endpoint terms. |
| `optional-live-binding-unavailable` | A matched RHS binding needed downstream was not retained. |
| `existence-probe-budget-exhausted` | Exact correlated existence or absence could not be resolved within budget. |
| `refinement-budget-exhausted` | Diagnostics required refinement beyond the deterministic work cap. |
| `correlated-random-product-unresolved` | Composition would multiply correlated uncertain messages without correction. |
| `unsupported-frontier-width` | The required ordered tuple frontier exceeds the supported representation or budget. |
| `arbitrary-property-path-not-supported` | The path cannot be reduced to supported finite bridge transfers. |
| `snapshot-mismatch` | Evidence and exact term IDs belong to a different LMDB snapshot epoch. |
| `deletion-generation-invalidated` | A deletion made the nonnegative Frontier synopsis unavailable pending rebuild. |

This taxonomy classifies why evidence is not composable. It must not silently
select an estimate or imply that a fallback is accurate.
