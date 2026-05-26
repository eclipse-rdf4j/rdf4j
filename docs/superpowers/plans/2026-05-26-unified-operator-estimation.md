# Unified Operator Estimation Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace VALUES-specific ordering pressure with one mathematical estimator model for BGP joins, `VALUES`,
`FILTER`, `OPTIONAL`/`LeftJoin`, `MINUS`/`Difference`, and `GROUP`, using sketches, LMDB page walking, and LEO feedback
as cooperating evidence sources.

**Architecture:** Every operator produces or transforms the same estimate vector: duplicate-preserving rows, lookup
domain rows, physical work rows, page-walk rows, memory rows, q-error, confidence, evidence count, and provenance.
Sketches estimate logical multiplicity and correlation surfaces; page walking estimates concrete access and lookup
work; LEO feedback repairs the fused vector before candidate comparison.

**Tech Stack:** Java 25, Maven with workspace-local `.m2_repo`, RDF4J query algebra, queryalgebra sketch planner,
LMDB evaluation statistics, Cascades `EstimateVector`, LMDB operator feedback, JUnit 5, Surefire/Failsafe, theme
query regression tests.

---

This ExecPlan is a living document. Maintain `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` while the estimator work proceeds.

## Purpose / Big Picture

The optimizer must not need a rule such as "if a disconnected `VALUES` table can be connected by the next statement,
force that statement first." That is an ordering heuristic. It can improve one query while making a different BGP worse.

After this plan, a connected `VALUES` access path wins only when its estimate vector is cheaper than alternatives. A
query where the filter fires too few times can still keep the filter because the vector includes filter evaluation cost,
lookup work, row flow, confidence, and feedback. A query with `LeftJoin`, `Difference`, `Group`, or `Filter` uses the
same row/work/q-error model as a plain BGP instead of separate ad hoc corrections.

## Progress

- [x] Captured required root quick install evidence before tests in `initial-evidence.txt`.
- [x] Added red BGP unit variants showing connected `VALUES` plans are currently priced above Cartesian splits.
- [x] Added red LMDB q9 optional/VALUES evidence showing finite-VALUES continuation rows are overwritten.
- [x] Added code comments at previous heuristic pressure points saying not to add shape-specific order rules.
- [x] Refine the BGP red tests so their synthetic cost model has the same repeated-lookup contract as LMDB page walking.
- [x] Fix repeated-invocation row-flow costing in `SketchJoinOrderPlanner`.
- [x] Preserve finite-VALUES-conditioned continuation estimates in LMDB optional bridge math.
- [ ] Add focused operator-vector tests for `Filter`, `LeftJoin`, `Difference`, and `Group`.
- [ ] Apply LEO repair to the unified vectors before winner selection where the current path still repairs too late.
- [ ] Replace remaining operator-local formulas with the documented shared vector contract.

## Surprises & Discoveries

- Observation: The current BGP red tests reproduce the bad plan but their test cost model is too scalar.
  Evidence: The test cost model returns `new FactorCostEstimate(workRows, outputRows)` without physical access metadata
  or `plannedRepeatedInvocations`, so the generic planner assumes per-invocation rows and may multiply them again.

- Observation: The planner has a repeated-invocation contract, but `physicalRowFlowRows(...)` only honors it when the
  physical estimate also has exact output rows.
  Evidence: `SketchJoinOrderPlanner.physicalRowFlowRows(...)` skips row-flow multiplication only for
  `physicalEstimate.exactOutputRows() && costModelChargedRepeatedInvocations(...)`.

- Observation: LMDB already rejects duplicate-product correction when finite binding values affect a factor during
  `estimateBoundJoinProductFactorCost(...)`, but optional continuation still performs an unguarded duplicate-product
  rewrite afterwards.
  Evidence: `finiteBindingValuesAffectBoundJoinProduct(...)` guards `estimateBoundJoinProductFactorCost(...)`, while
  `bridgeContinuationRows(...)` and `multiBridgeContinuationRows(...)` call `repeatedBoundProductRows(...)` without
  passing the finite binding values.

- Observation: The generic Cascades estimate vector already carries most fields needed for the shared model.
  Evidence: `EstimateVector` contains rows, lower/upper rows, work rows, memory rows, seeks, page-walk rows, q-error,
  uncertainty, confidence, evidence count, metrics, and `applyFeedback(...)`.

## Decision Log

- Decision: Do not add factor-ordering heuristics for `VALUES`.
  Rationale: Correct plans must fall out of better estimates and a richer alternative set, not hard-coded query-shape
  choices.
  Date/Author: 2026-05-26 / Codex.

- Decision: Treat repeated invocations as an estimate contract.
  Rationale: If a physical/page-walk estimate says repeated invocations are already costed, the join-order layer must
  not derive a second row-flow multiplication from prefix rows.
  Date/Author: 2026-05-26 / Codex.

- Decision: Keep finite-VALUES conditioning stronger than unconditioned duplicate-product correction.
  Rationale: A finite lookup has already incorporated the value domain; replacing it with a product that ignores that
  domain loses information.
  Date/Author: 2026-05-26 / Codex.

- Decision: Extend the existing `EstimateVector` model rather than introducing a parallel top-level cost type first.
  Rationale: It already has q-error, confidence, feedback, and page-walk fields; the immediate defect is inconsistent
  producer semantics, not absence of a vector container.
  Date/Author: 2026-05-26 / Codex.

- Decision: Scope this patch to the finite-domain row/work contract and keep broader `Filter`/`LeftJoin`/`Difference`/
  `Group` vector implementation as explicit follow-up.
  Rationale: The q9 regression is caused by inconsistent finite-domain costing. The general math is documented here,
  but the remaining operators need focused red/green tests before changing those broader paths.
  Date/Author: 2026-05-26 / Codex.

## Mathematical Model

Every algebra node should estimate one relation under a finite binding environment, not a tree shape. A `VALUES`
clause is one source of that environment.

Each operator returns an estimate vector `E`:

`E = (rows, lowerRows, upperRows, workRows, memoryRows, seeks, pageWalkRows, qRowMean, qRowMax, qWorkMean, qWorkMax, uncertaintyRows, confidence, evidenceCount, source, metrics)`.

Each operator also carries relation summaries by variable set:

- `K[X]`: a key summary for variables `X`: exact finite tuple map, sketch, sampled histogram, or distinct count.
- `N`: duplicate-preserving row count.
- `D[X]`: effective distinct tuple count over `X`.
- `f_X(t)`: estimated multiplicity of tuple `t` on variables `X`.
- `B[X]`: finite binding relation from `VALUES` or an equivalent finite domain rewrite, with tuple multiplicities.

The current code already has most vector fields. The missing discipline is that every producer must obey the same
contract for rows, work, repeated invocation counts, finite binding domains, and feedback timing.

### VALUES as a Finite Relation

Unary `VALUES ?x { a b c }` can be treated as a finite union over one key:

`B[{x}] = { (a): 1, (b): 1, (c): 1 }`.

For a relation `R(..., x, ...)`, conditioning by the `VALUES` relation is:

`rows(R join B) = sum_v B(v) * f_R(v)`.

That is the "bind the union of values, then estimate with the union" model. It is correct for unary `VALUES` and for
safe `FILTER (?x IN (...))` rewrites when the filter expression is equality-like, deterministic, and does not alter
SPARQL error or unbound semantics.

Multi-column `VALUES` must not be decomposed into independent unary domains. This is wrong:

`VALUES (?x ?y) { (a b) (c d) } != VALUES ?x { a c } VALUES ?y { b d }`.

The independent version invents `(a,d)` and `(c,b)`. The correct model keeps a finite tuple relation:

`B[{x,y}] = { (a,b): 1, (c,d): 1 }`

and estimates:

`rows(R join B) = sum_(x,y) B(x,y) * f_R(x,y)`.

If a statement pattern can only use one component physically, the tuple relation may project to that component for
access, but the full tuple relation must remain as a residual correlation constraint for later joins and filters.

### Evidence Fusion

The estimator has three evidence sources and one repair source:

1. **Finite relation evidence.** Exact tuple maps from `VALUES`, duplicate-safe filter rewrites, and small direct lookup
   enumerations.
2. **Sketch evidence.** Duplicate-preserving join surfaces:
   `joinRows(R,S,X) = sum_t f_R_X(t) * f_S_X(t)`.
3. **Page-walk evidence.** Physical LMDB access rows, fanout, lookup count, and page-walk work for concrete access
   prefixes.
4. **LEO feedback.** Runtime repair for rows, work, q-error, confidence, and evidence count.

Fusion rule:

- If evidence is exact for a field, use the exact value and mark confidence high for that field.
- If page walking enumerates all finite lookup tuples, use page-walk rows/work as exact for that access and export the
  observed fanout back as a key summary for downstream operators.
- If sketches provide a join surface and page walking provides per-key fanout, use the finite/page-walk domain to
  restrict the sketch surface, and use the sketch distinct estimate to choose how many page-walk probes are expected.
- If both sources are inexact, combine row estimates in log space:
  `rows = exp((w_s * ln(rows_s) + w_p * ln(rows_p)) / (w_s + w_p))`
  where weights increase with confidence/evidence and decrease with q-error.
- The final q-error bound is not the blended average. It must cover disagreement:
  `qMax = max(q_s, q_p, max(rows_s, rows_p) / max(epsilon, min(rows_s, rows_p)))`.
- Apply LEO after base fusion and before candidate comparison:
  `E_final = repair(E_base, feedback)` when the feedback key matches the operator surface and confidence clears the
  threshold.

This is how sketches and page walking feed each other without a heuristic:

- Sketches predict which variables form selective lookup domains and how many distinct lookup keys exist.
- Page walking measures concrete fanout/work for finite or sampled keys.
- Page-walk fanout repairs sketch surfaces for the current candidate and records evidence for LEO.
- LEO repairs the fused vector on later planning attempts, so the optimizer learns when either sketch or page-walk
  evidence was biased.

### Shared Metrics Contract

Additional metrics describe lookup-domain semantics:

- `plannedRepeatedInvocations`: lookup invocations already included in `workRows`.
- `plannedDistinctLookupBindings`: distinct bound lookup keys.
- `plannedAccessRows`: rows per access domain or rows before a local access filter.
- `plannedAccessWorkRows`: physical work per invocation when repeated invocations are not already included.
- `plannedBoundJoinProductRows`: duplicate-preserving rows from sketch/page-walk surface products.
- `plannedFiniteBindingRows`: rows in the finite binding relation used for the estimate.
- `plannedFiniteBindingDistinctRows`: distinct finite tuples used for access.

The invariant is:

If `plannedRepeatedInvocations` is present, `workRows` already includes repeated physical access work. The join-order
layer may still use `rows` for logical cardinality, but it must not multiply the physical work or row-flow contract a
second time.

### Operator Formulas

`BindingSetAssignment` / `VALUES`:

- `rows = sum_t B(t)`.
- `D[X] = count_distinct(t in B[X])`.
- `K[X] = exact finite tuple map`.
- `workRows = rows`.
- Exports `B[X]` to the enclosing algebra scope.

`StatementPattern`:

- Unbound access:
  `rows = indexRows(pattern constants)` from exact index counts, page-walk prefix counts, then sketch/count fallback.
- Bound access by finite domain `B[X]`:
  `rows = sum_t B(t) * fanout(pattern | X=t)`.
- If page walking can enumerate all `t`, then `fanout` and access work are exact for this candidate:
  `workRows = sum_t B(t) * pageWalkWork(pattern | X=t)`.
- If only sampled, estimate:
  `rows = rows(B) * mean_sampled_fanout`, with q-error from sample confidence and sketch disagreement.
- For multi-column finite domains, use the widest index-supported projection for access and keep the remaining tuple
  columns as residual filters/correlation summaries.

`Filter`:

- Deterministic finite membership filter:
  `passRows = sum_t input.f_X(t) for t in allowed`.
- Generic filter:
  `rows = input.rows * passRatio`.
- Pass ratio source order: exact finite evaluation, learned filter stats, sampled stats, sketch-backed local
  selectivity, fallback.
- `workRows = input.workRows + input.rows * filterEvalCost`.
- A safe `FILTER IN` to `VALUES` rewrite is profitable only when:
  `work(conditioned access path) + valuesWork < work(original access path) + input.rows * filterEvalCost`
  under the same q-error/confidence comparison.

`Join`:

- With shared key set `X`:
  `rows = sum_t f_L_X(t) * f_R_X(t)`.
- With finite domain `B[X]` present:
  `rows = sum_t B(t) * f_L_X(t) * f_R_X(t)` if `B` is an external constraint,
  or `rows = sum_t f_L_X(t) * f_R_X(t)` after either side has already been conditioned by `B`.
- Without a usable surface:
  `rows = L.rows * R.rows / max(D_L[X], D_R[X], 1)`.
- Disconnected join:
  `rows = L.rows * R.rows`, but the work vector should make this lose when a connected path can condition a later
  access. No shape-specific ordering rule is needed.
- Physical work:
  `workRows = L.workRows + R.workRows + probeWork`.
  If `R.workRows` already includes repeated invocations, `probeWork` must not multiply it again.

`Union`:

- Bag semantics:
  `rows = left.rows + right.rows`.
- For key summaries:
  `f_X(t) = f_L_X(t) + f_R_X(t)`.
- `workRows = left.workRows + right.workRows`.
- Distinct variants apply the `Distinct` formula after the union.

`LeftJoin` / `OPTIONAL`:

- Estimate RHS under the left prefix domain, not independently:
  `matchedRows = rows(left join right)`.
- Estimate matched left rows over shared key `X`:
  `matchedLeftRows = sum_t f_L_X(t) * P(right has at least one match | X=t)`.
- Then:
  `rows = matchedRows + max(0, left.rows - matchedLeftRows)`.
- Work includes probing/building the RHS for the left lookup domain:
  `workRows = left.workRows + conditionedRight.workRows + optionalJoinOverhead`.
- Finite `VALUES` inside the optional RHS contributes a `B` relation to the RHS conditioning. If page walking measures
  `right | B`, later duplicate-product corrections must not replace it with an unconditioned RHS estimate.

`Difference` / `MINUS`:

- Let shared key set be `X`, excluding variables that do not participate in SPARQL `MINUS` compatibility.
- Estimate RHS existence over the left domain:
  `matchProb(t) = P(right has at least one compatible row | X=t)`.
- `antiMatchRatio = sum_t f_L_X(t) * matchProb(t) / left.rows`.
- `rows = left.rows * (1 - antiMatchRatio)`.
- Work is either RHS materialization plus left probe work, or repeated RHS page-walk probes, depending on the selected
  physical alternative:
  `workRows = left.workRows + rhsBuildOrProbeWork + left.rows * antiProbeCost`.
- Finite RHS `VALUES` constrains `matchProb`; it must not be costed as an independent Cartesian table unless no shared
  variables can exist by SPARQL semantics.

`Group`:

- Global aggregate:
  `rows = 1` when the input relation can produce a group; `rows = 0` only for operators whose SPARQL semantics truly
  produce no result row.
- Grouped aggregate over key `G`:
  `rows = D_input[G]`.
- With finite domain `B[G]`:
  `rows <= count_distinct(B[G])`, and exact when the input has been fully enumerated over `B`.
- `workRows = input.workRows + input.rows * aggregateUpdateCost`.
- `memoryRows = D_input[G]` for hash aggregation, lower for streaming aggregation when ordering proves grouped input.

`Projection` / `Extension`:

- Plain projection preserves duplicate rows:
  `rows = input.rows`.
- Key summaries for dropped variables are discarded; summaries for retained expressions are projected when exact or
  estimated when expression semantics are known.
- Extension preserves rows and adds expression evaluation work:
  `workRows = input.workRows + input.rows * expressionCost`.

`Distinct` / `Reduced`:

- `rows = D_input[projectedVars]`.
- If a finite relation exactly covers projected variables:
  `rows = count_distinct(B[projectedVars])` after all filters/joins affecting those variables.
- Work includes either hash-state memory proportional to output distinct rows or streaming cost when ordering proves
  uniqueness.

`Order` / `Slice`:

- `Order` preserves rows and adds sort work/memory unless physical ordering is delivered:
  `workRows = input.workRows + input.rows * log2(max(input.rows, 2))`.
- `Slice`:
  `rows = min(limit, max(0, input.rows - offset))`.
- If an access path can deliver order plus limit, page walking may cap access rows before the logical input rows are
  fully materialized.

## Context and Orientation

Primary files for this slice:

- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchJoinOrderPlanner.java`
- `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimatorJoinOrderPlannerTest.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/EstimateVector.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStats.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizer.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`

Primary tests for this slice:

- `SketchBasedJoinEstimatorJoinOrderPlannerTest`
- `LmdbThemeQ9EstimateRegressionTest`
- `LmdbNestedBoundLookupEstimateTest`
- `LmdbThemeQueryRegressionIT`
- new focused tests for `EstimateVector`/operator algebra if the current coverage is not enough.

## Plan of Work

### Task 1: Make BGP VALUES Tests Physical

**Files:**
- Modify: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimatorJoinOrderPlannerTest.java`

- [x] Change the synthetic cost model to implement `estimateFactorCost(TupleExpr, CostContext)`.
- [x] Return physical `FactorCostEstimate` instances with lookup masks, access rows, and `plannedRepeatedInvocations`
  for finite/bound lookups.
- [x] Keep the assertions that connected fixed-order estimates are cheaper than Cartesian splits.
- [x] Run:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=SketchBasedJoinEstimatorJoinOrderPlannerTest#planJoinOrderKeepsObjectValuesLookupInConnectedComponent+planJoinOrderKeepsObjectValuesLookupConnectedWhenValuesDeclaredLast+planJoinOrderKeepsObjectValuesLookupConnectedWithUnrelatedValues+planJoinOrderKeepsValuesAnchorConnectedAcrossTwoHopBridge verify`

Expected red before production fix: at least one selected/fixed-order plan still splits the connected `VALUES` component
or still prices connected repeated lookups above the Cartesian split.

### Task 2: Fix Repeated Row-Flow Contract

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchJoinOrderPlanner.java`

- [x] In `physicalRowFlowRows(...)`, skip derived row-flow multiplication whenever
  `costModelChargedRepeatedInvocations(physicalEstimate.factorCostEstimate())` is true.
- [x] Keep `factorStepWorkRows(...)` intact unless a focused red test proves it still double-counts work.
- [x] Rerun the Task 1 command and expect green.

### Task 3: Preserve Finite-VALUES Continuation Math

**Files:**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`

- [x] Extract a helper that checks whether finite binding values intersect a factor's binding names.
- [x] Reuse it from `finiteBindingValuesAffectBoundJoinProduct(...)`.
- [x] Pass finite binding values into `repeatedBoundProductRows(...)`.
- [x] Return the finite-aware fallback rows when finite binding values affect the continuation factor.
- [x] Run:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbThemeQ9EstimateRegressionTest#q9LikeOptionalValuesUsesRepeatedBridgeRowsWithSelectedValueFrequency verify`

Expected green after the fix: the optional bridge continuation keeps the selected finite value frequency instead of
using an unconditioned duplicate product.

Implemented in this slice:

- `VALUES`/finite-domain estimates that already include repeated physical invocations now carry that contract through
  `FactorCostEstimate`, and `SketchJoinOrderPlanner` no longer multiplies row-flow again.
- LMDB optional bridge continuation keeps finite-VALUES-conditioned branch estimates instead of replacing them with an
  unconditioned duplicate-product estimate.
- BGP tests cover VALUES declared before/after connected statements, unrelated VALUES, and a two-hop finite bridge.
- LMDB q9 tests assert that selected finite-anchor candidates do not win with positive Cartesian work.

### Task 4: Cover Filter, LeftJoin, Difference, Group

**Files:**
- Modify or create tests under `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/`
- Modify or create tests under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/`

- [ ] Add a `Filter` vector test: finite `VALUES` pass ratio beats heuristic pass ratio and preserves q-error metrics.
- [ ] Add a `LeftJoin` vector test: output rows are at least left rows and matched duplicate rows are retained.
- [ ] Add a `Difference` vector test: anti-match probability reduces left rows without changing visible variables.
- [ ] Add a `Group` vector test: global aggregate estimates one output row and grouped aggregate uses effective
  distinct group count.
- [ ] Run focused tests with no `-am` and no `-q`.

### Task 5: Apply LEO Repair in Candidate Costing

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/EstimateVector.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizer.java`

- [ ] Identify operator paths that still annotate feedback after selection rather than using feedback for candidate
  comparison.
- [ ] Convert those paths to produce base vector plus feedback-repaired vector before winner selection.
- [ ] Preserve telemetry for base rows/work, repaired rows/work, q-error, confidence, and evidence count.
- [ ] Run focused feedback tests:
  `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbOperatorFeedbackPlanningTest,LmdbOperatorFeedbackStatsTest verify`

## Validation and Acceptance

Acceptance for this slice:

- The BGP `VALUES` red tests pass without adding order heuristics.
- The LMDB q9 optional/VALUES red test passes.
- `LmdbThemeQueryRegressionIT` confirms safe `FILTER IN` rewrites can render as `VALUES` unless filter evaluation is
  cheaper.
- `Filter`, `LeftJoin`, `Difference`, and `Group` have focused tests proving they use the shared vector semantics.
- Plan telemetry shows estimate source, q-error, confidence, page-walk rows, repeated invocations, and feedback source
  where available.

## Idempotence and Recovery

All tests build in-memory datasets or local LMDB temporary stores. No external services are required. Existing untracked
logs and scratch directories must remain in place. If a focused test fails after a change, keep the red assertion and
fix the estimator contract rather than weakening the test.

## Artifacts

- Initial quick install evidence: `initial-evidence.txt`
- Red BGP evidence: `core/queryalgebra/evaluation/target/surefire-reports/org.eclipse.rdf4j.query.algebra.evaluation.sketch.SketchBasedJoinEstimatorJoinOrderPlannerTest.txt`
- Red q9 evidence: `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbThemeQ9EstimateRegressionTest.txt`
- This plan: `docs/superpowers/plans/2026-05-26-unified-operator-estimation.md`

## Interfaces

The immediate code slice uses existing interfaces:

- `JoinFactorCostModel.FactorCostEstimate`
- `JoinFactorCostModel.CostContext`
- `EstimateVector`
- `StatisticsEstimate`
- `FilterPassEstimate`

Future extraction can introduce an `OperatorEstimateMath` helper only after the focused operator tests prove duplication
or inconsistent semantics across files.
