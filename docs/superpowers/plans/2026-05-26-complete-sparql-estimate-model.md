# Complete SPARQL Cardinality And Cost Model

This ExecPlan is a living document. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current while implementing.

## Purpose

Implement one shared multiset cardinality and cost model for RDF4J tuple algebra. Sketch join ordering, Cascades costing, LMDB page-walk estimates, filter-to-VALUES rewrites, OPTIONAL, MINUS, GROUP, DISTINCT, UNION, projection, extension, slice, and order must use the same math rather than separate heuristics.

The observable outcome is that generated or explicit VALUES clauses are costed as finite bag relations, complex BGPs no longer pick exploding Cartesian or disconnected placements when a connected alternative has lower modeled cost, and operators outside BGPs propagate cardinality, work, uncertainty, and variable coverage coherently.

## Progress

- [x] Root quick install baseline captured.
- [x] `initial-evidence.txt` written from a green root quick install.
- [x] Add red pure math tests.
- [x] Add red BGP/VALUES planner regressions.
- [x] Add red LMDB medical Q9 regression.
- [x] Implement shared estimate types.
- [x] Implement shared operator formulas.
- [x] Wire SketchBasedJoinEstimator.
- [x] Wire SketchJoinOrderPlanner through the existing sketch provider path.
- [x] Wire CascadesCostModel and RdfStatisticsProvider.
- [x] Wire LMDB page-walk/sketch fusion.
- [x] Run focused and module verification.
- [x] Preserve projected finite tuple frequencies through projection.
- [x] Add a 300-query SPARQL estimate audit corpus.
- [x] Add a first LMDB estimate audit harness.
- [ ] Commit logical groups and push.

## Surprises & Discoveries

- Current worktree already contains unrelated modified LMDB benchmark files and many untracked artifacts. Do not revert or overwrite them.
- Current branch is `GH-0000-lmdb-predicate-guarantees`, so implementation can proceed off main without creating another branch.
- The medical/engineering VALUES failures were not caused by join ordering itself. Exact finite expansion produced the right full-prefix surface, but selection trusted a lower-confidence bridge estimate over the exact full-prefix relation.
- `mvnf` runs Maven `verify`; for LMDB method-level Surefire checks it can continue into unrelated Failsafe ITs. Use `mvn ... -DskipITs test` for tight unit evidence when needed.
- The Q9 blow-up was a double-counting problem. Exact finite expansion joined the generated `condCode` relation into the statement surface, then the later `BindingSetAssignment` guard applied selectivity again. The guard now contributes only bag multiplicity once a finite binding has been absorbed.
- Full `core/sail/lmdb` verification still has two residual failures outside this implementation: `LmdbSubSelectDirectLookupEstimateTest#subSelectPlanStaysBoundedAfterStoreMutations` times out waiting for sketch readiness before plan assertions, and `ThemeQueryBenchmarkSparseParamTest` reflects the pre-existing dirty benchmark parameter file.
- Projection dropped exact finite tuple relations whenever any column was removed. That destroyed projected tuple frequency and correlation before later DISTINCT, GROUP, or JOIN costing. Projection now keeps a projected finite bag relation with merged tuple frequencies.
- The first estimate audit harness parses and evaluates full queries plus statement patterns. It is enough to preserve broad query coverage and expose leaf-level access estimates, but it does not yet split nested joins, filters, groups, paths, or optional/minus subtrees into independently audited pieces.
- The current AAS path problem appears to favor a high-work Cartesian shape because `rdf:type` class statement patterns look artificially cheap as anchors while connected property-path joins carry fallback/no-winner or inflated work estimates. The next regression should prove the connected alternative beats the type-first Cartesian alternative by modeled work, not by a class-specific placement rule.

## Decision Log

- Use Routine D because this is a cross-cutting optimizer model change.
- Keep all new APIs internal to query evaluation packages; no external RDF4J API changes.
- Preserve VALUES tuple multiplicity and multi-column correlation with a finite bag relation, not per-variable sets.
- A generated finite range has a memory safety cap. Within the cap, choosing the rewrite is cost-based.
- No join-placement heuristics. Generate alternatives broadly and let the shared model choose by modeled work, rows, uncertainty, and confidence.
- Exact full-prefix finite relation estimates dominate modeled bridge products because they are stronger mathematical evidence, not because of a hard-coded VALUES position rule.
- Audit corpus queries are generated deterministically from templates instead of stored as static fixtures so future failures can be expanded systematically without maintaining 300 hand-written query strings.

## Implementation Tasks

### 1. Red Tests

Add tests before production changes.

Pure math tests should cover:

- Inner join with shared key and with Cartesian key set empty.
- Inner join using exact finite frequencies: `rows = sum(freqL(k) * freqR(k))`.
- LeftJoin with full match, partial match, no match, one-to-many RHS, and nullable RHS variables.
- Difference/MINUS with shared compatible variables and no shared variables.
- Filter exact finite evaluation, `IN`, equality, and bounded integer range.
- Group with no keys and grouped tuple NDV.
- Distinct/Reduced tuple NDV.
- Union bag semantics.
- Projection variable dropping.
- Extension constants and aliases.
- Slice offset/limit.
- Order work rows.

Planner tests should cover:

- Single-column VALUES connected after a non-selective prefix.
- Disconnected VALUES before and after connected statement alternatives.
- Duplicate VALUES rows preserving bag multiplicity.
- Multi-column VALUES preserving tuple correlation.
- VALUES under OPTIONAL.
- VALUES under MINUS.
- VALUES before and after inner joins.
- Filter-to-VALUES alternative competing against raw filter by cost.

LMDB regression should cover the medical Q9 query shape and assert:

- No generated `?condCode` VALUES placement causes the `med:code` statement to execute as an exploding lookup over the wrong prefix.
- Plan annotations expose finite relation costs.
- Query completes within the existing test timeout.

Commands:

- `python3 .codex/skills/mvnf/scripts/mvnf.py BagEstimateMathTest --retain-logs --stream`
- `python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorJoinOrderPlannerTest --retain-logs --stream`
- `python3 .codex/skills/mvnf/scripts/mvnf.py CascadesCostModelTest --retain-logs --stream`
- LMDB focused regression through `mvnf`.

Capture failing Surefire snippets before production edits.

### 2. Shared Estimate Types

Create `org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cost`.

Add:

- `BagEstimate`: rows, workRows, memoryRows, uncertainty, confidence, variable estimates, finite relations, source metrics.
- `VariableEstimate`: distinct bound values, bound row coverage, nullable row coverage, optional distribution sketch.
- `FiniteRelationEstimate`: exact finite bag relation over ordered variable names with tuple frequencies and per-variable summaries.
- `VariableSetKey`: stable sorted key for variable sets.
- `DistributionSketch`: adapter interface for sketch overlap, distinct estimates, union, and restrictions.
- `EstimateMath`: static formulas for algebra operators.

Keep files under roughly 500 LOC. Split helper types rather than growing one large utility class.

### 3. Operator Formulas

Implement formulas in `EstimateMath`.

Inner join:

- `K = sharedVars(left, right)`.
- If `K` empty: `rows = left.rows * right.rows`.
- Else if exact finite relation or sketch frequencies exist: `rows = sum(freqLeft(k) * freqRight(k))`.
- Else: `rows = left.rows * right.rows / max(ndvLeft(K), ndvRight(K), 1)`.
- Work includes left work, right work/access work, repeated lookup work, and intermediate rows.

LeftJoin:

- `joinRows = innerJoinRows(left, right)`.
- `matchedLeftRows = estimateMatchedLeftRows(left, right)`.
- `unmatchedLeftRows = max(0, left.rows - matchedLeftRows)`.
- `rows = joinRows + unmatchedLeftRows`.
- Left vars remain bound across all output rows.
- Right-only vars have `boundRows = joinRows` and nullable coverage for unmatched rows.

Difference/MINUS:

- If no shared compatible variables: `rows = left.rows`.
- Else: `rows = max(0, left.rows - matchedLeftRows(left, right))`.
- Preserve only left variable estimates.

Filter:

- Exact finite evaluation when all referenced vars are finite.
- Equality, `IN`, and safe bounded integer ranges produce finite restrictions.
- Otherwise use learned/sketch selectivity or a conservative interval.
- `rows = input.rows * passRatio`.

Group:

- No group vars: `rows = input.rows > 0 ? 1 : 0`.
- With group vars: `rows = ndv(tuple(groupVars))`.
- `workRows = input.workRows + input.rows`.
- `memoryRows = groupRows`.

Distinct/Reduced:

- `rows = ndv(tuple(projectedVars))`.
- `workRows = input.workRows + input.rows`.
- `memoryRows = distinctRows`.

Union:

- Bag rows add.
- Variable bound rows add per branch.
- Distinct estimates union when sketches or finite relations are available.

Projection:

- Rows unchanged.
- Drop unprojected variable estimates and finite relations referencing dropped vars.

Extension:

- Rows unchanged.
- Constants get `distinctRows = 1`.
- Aliases reuse source variable stats.
- General expressions get conservative stats bounded by input rows.

Slice:

- `rows = min(limit, max(0, input.rows - offset))`.
- Work reflects rows consumed to reach `offset + limit`.

Order:

- Rows unchanged.
- Add `n * log2(max(n, 2))` work.
- Memory is `n` unless a physical property proves streaming order.

### 4. Planner Wiring

Make `SketchBasedJoinEstimator.TuplePlanEstimate` delegate to `BagEstimate` while preserving existing callers.

Replace finite VALUES extraction in `JoinFactorCostModel.CostContext` and `SketchJoinOrderPlanner` with `FiniteRelationEstimate`. Remove per-variable set logic after tests prove tuple and duplicate behavior.

Use `EstimateMath` in:

- `SketchBasedJoinEstimator` for tuple algebra subtree estimates.
- `SketchJoinOrderPlanner` for join alternative scoring.
- `CascadesCostModel` for operator estimates and fallbacks.
- `RdfStatisticsProvider` via optional `BagEstimate` exposure.
- LMDB statistics/page-walk provider, preserving access rows and lookup work as physical evidence.

Add comments near `EstimateMath` and join-order scoring explaining that hard-coded placement heuristics are forbidden because they optimize one current query while making another query worse.

### 5. Verification

Run:

- Focused red tests after adding tests.
- Same focused tests after implementation.
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs --stream`
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --stream`
- Format/checkCopyright before final commit.
- Medical Q9 benchmark through `scripts/run-single-benchmark.sh`.

Acceptance:

- New tests fail before production edits and pass after.
- Existing focused tests remain green.
- LMDB medical Q9 no longer produces the 424M intermediate `med:code` lookup.
- Plan annotations show finite relation and shared model cost source.
- No query-specific join-order heuristic is introduced.
- Projected finite relations survive projection with bag multiplicity preserved.

## Outcomes & Retrospective

Implemented a shared internal estimate model in
`org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cost` with bag rows, work rows, memory rows,
uncertainty, variable coverage, finite bag relations, and one `EstimateMath` formula surface for
joins, optional joins, minus, filters, grouping, distinct, union, projection, extension, slice, and
order.

Wired the shared model through `StatisticsEstimate`, `RdfStatisticsProvider`, `CascadesCostModel`,
`SketchBasedJoinEstimator`, and the LMDB statistics/page-walk provider. `BindingSetAssignment` now
creates exact finite bag estimates, and LMDB exact finite expansion preserves tuple correlation and
duplicate multiplicity instead of reducing VALUES to per-variable sets.

Validation commands:

- `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`
- `python3 .codex/skills/mvnf/scripts/mvnf.py BagEstimateMathTest --retain-logs --stream`
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs --stream`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=BagEstimateMathTest,CascadesCostModelTest -DskipITs test`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbFiniteValuesJoinSurfacePlanningTest,LmdbThemeQ9EstimateRegressionTest,LmdbEvaluationStatisticsMemoizationTest,LmdbOperatorFeedbackPlanningTest -DskipITs test`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbEstimateAuditHarnessTest,LmdbEstimateAuditQueryCorpusTest -DskipITs test`
- `mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources`
- `./scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery --warmup-iterations 1 --measurement-iterations 1 --forks 1 --param sketchEstimatorEnabled=true --param themeName=MEDICAL_RECORDS --param z_queryIndex=9 --jvm-arg -Xms1G --jvm-arg -Xmx16G`
- `bash testsuites/benchmark/test-run-single-benchmark.sh`
- `./scripts/run-single-benchmark.sh --no-build --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery --warmup-iterations 1 --measurement-iterations 1 --forks 1 --param sketchEstimatorEnabled=true --param themeName=MEDICAL_RECORDS --param z_queryIndex=9`

The focused core and LMDB selections pass. The broader `core/queryalgebra/evaluation` module passes.
The Q9 benchmark completes at `592.628 ms/op`, with no timeout and no 424M `med:code` intermediate.
The selected Q9 plan exposes the generated finite anchor as `costing=dynamic`, keeps `finiteValues`
on `condCode`, and limits actual BGP intermediates to thousands/hundreds-of-thousands rather than
hundreds of millions.

`scripts/run-single-benchmark.sh` now preflights the same local `ServerSocket` capability JMH
uses for fork communication before Maven packaging and before benchmark jar lookup. In the restricted
path that produced `BinaryLinkServer/SocketException`, the script now fails immediately with
`JMH fork socket preflight failed` and an escalation hint.

Remaining risk: the full LMDB module verify is not fully green because of the two residual failures
listed in Surprises & Discoveries. They are documented and were not introduced by the estimate model
changes.

### 2026-05-27 AAS Path / Type Cartesian Follow-Up

User-provided AAS path plans showed Cascades favoring a disconnected `rdf:type` branch because type
patterns and provider/fallback estimates were sometimes published as row-only scalars with no bag
variable coverage. After a Cartesian product, a later connected join on `?b`/path variables then fell
back to `tupleDistinct = branch.rows`, dividing by the whole Cartesian cardinality and collapsing the
estimate.

Red evidence:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesCostModelTest#providerRowsStillCarryBindingStatsThroughCartesianBranches -DskipITs test`
- Failure: `expected: <100.0> but was: <5.0>`.

Fix:

- `CascadesCostModel` now normalizes provider/fallback estimates into `BagEstimate` values carrying
  conservative binding stats for every output binding.
- Provider filter estimates preserve the input bag through pass-ratio scaling.
- Pass-through unary wrappers preserve bag variables instead of converting through a scalar vector.
- Fallback estimates now use `bagWithBindings(...)`.

Validation:

- `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesCostModelTest,CascadesMemoModelTest -DskipITs test`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbEstimateAuditHarnessTest#noKeyAggregateUsesAggregateOutputRows,LmdbEstimateAuditHarnessTest#auditsGeneratedCorpusTemplatesAcrossNestedPieces -DskipITs test`
- `git diff --check`

Result:

- Focused core estimator tests pass: `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`.
- LMDB audit tests pass: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.
- The generated corpus audit now catches this class of under-estimation before long benchmark runs.
