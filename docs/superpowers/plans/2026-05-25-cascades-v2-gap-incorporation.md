# Cascades V2 Gap Incorporation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Incorporate the useful parts of `big-refactor-v2/rdf4j-cascades-optimizer-v2.patch` and `big-refactor-v2/rdf4j-cascades-optimizer-v2-source/` that are not already present, without regressing the current default-on LMDB Cascades pipeline or exact LMDB estimator usage.

**Architecture:** Treat v2 as a reference patch, not an authoritative overwrite. Add a Cascades-owned estimate-vector layer for algebra-wide transformations, extend the RDF statistics SPI and LMDB overrides around that layer, then add v2 logical/physical alternatives behind focused tests. Keep the existing `JoinFactorCostModel.EstimateVector` for LMDB factor estimates and bridge it into Cascades vectors rather than replacing it.

**Tech Stack:** Java 25, Maven offline with `.m2_repo`, RDF4J query algebra, LMDB Sail, JUnit 5, Maven Surefire/Failsafe, existing `mvnf` runner.

---

## Current Gap Map

Source-tree audit:

- `big-refactor-v2/rdf4j-cascades-optimizer-v2-source/` is a broad 349-file source snapshot. It includes many full `queryalgebra/evaluation` and `sail/lmdb` files unrelated to the Cascades delta.
- Use the source tree as reference for complete file context only. Do not copy broad files into the Maven modules.
- Relevant unique source additions are narrow: `EstimateVector.java`, `LmdbStarJoinScanSupport.java`, and `CascadesEstimateVectorTest.java`.
- Existing v2 source versions of `CascadesMemoModelTest` and `CascadesRuleEngineTest` differ from this workspace mostly by formatting, not new behavior.
- The source tree is older than this workspace in important LMDB areas. It does not contain current q6/q7 defensive work in `LmdbEvaluationStatistics`, `LmdbCascadesOptimizer`, `JoinFactorCostModel`, or `JoinCostVector`.
- Therefore every v2 production change must be cherry-picked by concept and reconciled with current files. Whole-file replacement is explicitly out of scope.

Already implemented in this workspace:

- Cascades package and LMDB optimizer registration.
- Default LMDB Cascades mode is `auto`.
- `CostVector` carries rows, work rows, memory rows, seeks, page-walk rows, row/work q-error, uncertainty, confidence, evidence.
- `JoinFactorCostModel.EstimateVector` extracts q-error conservatively and maps LMDB page-walk metrics.
- LMDB Cascades factor costing uses LMDB estimators and exact estimator tier.
- `RdfStatisticsProvider` has statement-pattern, characteristic-set star, property-path, and feedback correction hooks.
- Memo frontier exact-mode no-trim is partially present for `SearchMode.EXACT`.

Not yet implemented from v2:

- Cascades-owned `EstimateVector` with row intervals, source, metrics, vector operations, feedback gating, and conversion to the current extended `CostVector`.
- `StatisticsEstimate.fromVector(...)` and `StatisticsEstimate.vector()`.
- Algebra-wide `CascadesCostModel` estimates for `Filter`, `Join`, `LeftJoin`, `Difference`, `Union`, `Projection`, `Distinct`, `Reduced`, `BindingSetAssignment`, `ArbitraryLengthPath`, and `ZeroLengthPath`.
- Extended `RdfStatisticsProvider` methods: `multiPatternJoin`, `filter`, `union`, `leftJoin`, `minus`, `distinct`, and `starMultiPredicateScan`.
- LMDB overrides for those provider methods.
- V2 logical rules: conjunct filter pushdown, filter-over-UNION, projection-over-UNION, join-over-UNION.
- Planner-visible `lmdb-star-multi-predicate-scan` physical alternative.
- `RecordIterator.skipTo(...)` and skip-ahead telemetry.
- Budgeted frontier approximation marker and no frontier trimming for non-budgeted shadow/exact modes.
- Dedicated `CascadesEstimateVectorTest`.

Guardrails:

- Do not apply `rdf4j-cascades-optimizer-v2.patch` wholesale.
- Do not copy whole files from `rdf4j-cascades-optimizer-v2-source/`.
- Keep current workspace improvements when source conflicts:
  - `JoinFactorCostModel.EstimateVector` q-error extraction and page-walk mapping.
  - `JoinCostVector` primitive robust dominance and q7-safe ranking.
  - LMDB q6 scoped-UNION distribution fix.
  - LMDB q7 optional-source/domain fallback fixes.
  - `LmdbCascadesOptimizer` default `auto`, fast-path annotations, and safe root replacement.
- Map source paths into Maven modules exactly:
  - `org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/*` -> `core/queryalgebra/evaluation/src/main/java/...`
  - `src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/*` -> `core/queryalgebra/evaluation/src/test/java/...`
  - `org/eclipse/rdf4j/sail/lmdb/*` -> `core/sail/lmdb/src/main/java/...`
- Keep LMDB Cascades cost contexts on `JoinFactorCostModel.EstimationTier.EXACT`, including budgeted search. Budget may cap exploration, not downgrade costing.
- Keep protected untracked artifacts untouched: `big-refactor/`, `big-refactor-v2/`, `initial-evidence.txt`, `.initial-quick-install.log`, `logs/`, `prompt-exports/`.
- New Java files need the 2026 RDF4J header plus `// Some portions generated by Codex`.

### Task 1: Capture V2 Baseline Failures

**Files:**
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmarkSmokeIT.java`
- Test: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesEstimateVectorTest.java`
- Artifact: `logs/cascades-v2-initial-evidence.txt`

- [ ] **Step 1: Run quick install**

Run:

```bash
mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
```

Expected: build success. If offline resolution fails, rerun once without `-o`, then return to offline commands.

- [ ] **Step 2: Capture existing q7 failure**

Run:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py --it ThemeQueryBenchmarkSmokeIT#sparseQ7BenchmarkKeepsOfferPathBeforePersonFanout --retain-logs --stream
```

Expected before fixes: fail like the current broad run, where q7 reports `plannedWorkRows` around `2.4K` for actual `480000` rows on the `https://schema.org/about` bridge.

- [ ] **Step 3: Preserve failure evidence without touching protected root evidence**

Run:

```bash
tail -120 logs/mvnf/$(ls -t logs/mvnf | head -1) > logs/cascades-v2-initial-evidence.txt
```

Expected: `logs/cascades-v2-initial-evidence.txt` contains the q7 failure snippet. Do not overwrite `initial-evidence.txt`.

- [ ] **Step 4: Add failing Cascades vector tests**

Create `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesEstimateVectorTest.java` with tests covering:

```java
@Test
void feedbackIsIgnoredBelowConfidenceThreshold() {
	EstimateVector base = EstimateVector.heuristic(100.0d, 4.0d, "base");
	FeedbackCorrection weak = new FeedbackCorrection(10.0d, 12.0d, 0.25d, 1.5d, 1.5d, "weak-feedback");

	assertEquals(base, base.applyFeedback(weak, 0.55d));
}

@Test
void trustedFeedbackCarriesRowsWorkAndQErrorIntoTheVector() {
	EstimateVector base = EstimateVector.heuristic(100.0d, 8.0d, "base");
	FeedbackCorrection feedback = new FeedbackCorrection(20.0d, 25.0d, 0.90d, 2.0d, 3.0d,
			"operator-feedback");

	EstimateVector adjusted = base.applyFeedback(feedback, 0.55d);

	assertEquals(20.0d, adjusted.rows());
	assertEquals(25.0d, adjusted.workRows());
	assertEquals(3.0d, adjusted.rowQErrorMax());
	assertEquals(3.0d, adjusted.workQErrorMax());
	assertEquals(0.90d, adjusted.confidence());
	assertTrue(adjusted.metrics().containsKey("plannedFeedbackConfidence"));
}

@Test
void filterKeepsQErrorIntervalInsteadOfCollapsingToScalarRows() {
	EstimateVector base = EstimateVector.heuristic(1_000.0d, 5.0d, "base");
	QErrorInterval pass = QErrorInterval.fromBounds(0.10d, 0.25d, 0.50d, 0.70d, "learned-filter");

	EstimateVector filtered = base.filter(0.25d, pass, "filter");

	assertEquals(250.0d, filtered.rows());
	assertTrue(filtered.lowerRows() < filtered.rows());
	assertTrue(filtered.upperRows() > filtered.rows());
	assertEquals(0.70d, filtered.confidence());
}
```

- [ ] **Step 5: Run vector tests and confirm red**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesEstimateVectorTest verify
```

Expected: compile failure because `EstimateVector` does not exist yet.

### Task 2: Add Cascades EstimateVector

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/EstimateVector.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StatisticsEstimate.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CostVector.java`

- [ ] **Step 1: Implement `EstimateVector`**

Create a Cascades-local record with this shape:

```java
public record EstimateVector(double rows, double lowerRows, double upperRows, double workRows, double memoryRows,
		double seeks, double pageWalkRows, double rowQErrorMean, double rowQErrorMax, double workQErrorMean,
		double workQErrorMax, double uncertaintyRows, double confidence, double evidenceCount, String source,
		Map<String, Double> metrics) {
}
```

Implement constructors/helpers:

- `exact(double rows, String source)`
- `heuristic(double rows, double qError, String source)` with confidence `0.25d` and evidence `0.0d`
- `fromStatistics(StatisticsEstimate estimate)`
- `fromFactorVector(JoinFactorCostModel.EstimateVector estimate)`
- `toStatistics(String method)`
- `toCostVector()`
- `plus(...)`, `union(...)`, `join(...)`, `filter(...)`, `withRows(...)`, `withWorkRows(...)`, `withMetric(...)`
- `applyFeedback(FeedbackCorrection feedback, double confidenceThreshold)`

Preserve the current robust-vector semantics:

- q-error clamped to `>= 1.0d`
- confidence clamped to `[0.0d, 1.0d]`
- evidence non-negative
- uncertainty from row interval width, not silently zero when the estimate is heuristic
- exact q-error `1.0d` only from exact constructors or exact metrics

- [ ] **Step 2: Add StatisticsEstimate conversions**

Add:

```java
public static StatisticsEstimate fromVector(EstimateVector vector, String method) {
	EstimateVector safe = vector == null ? EstimateVector.heuristic(1.0d, 16.0d, method) : vector;
	String effectiveMethod = method == null || method.isBlank() ? safe.source() : method;
	return new StatisticsEstimate(safe.rows(), safe.interval(effectiveMethod), safe.workRows(), effectiveMethod,
			safe.metrics());
}

public EstimateVector vector() {
	return EstimateVector.fromStatistics(this);
}
```

- [ ] **Step 3: Keep CostVector ranking explicit**

Add `objectiveScore()` to the current extended `CostVector`, but keep current primitive robust dimensions in `dominates(...)`:

```java
public double objectiveScore() {
	double robustWork = robust().workRows();
	double ioPenalty = seeks * 0.25d + pageWalkRows * 0.05d + memoryRows * 0.01d;
	double score = robustWork + ioPenalty;
	return Double.isFinite(score) && score >= 0.0d ? score : Double.MAX_VALUE;
}
```

Update `compareTo(...)` to compare `objectiveScore()` first, then current tie-breakers.

- [ ] **Step 4: Run vector tests**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesEstimateVectorTest verify
```

Expected: `CascadesEstimateVectorTest` passes.

### Task 3: Extend Cascades Algebra Costing

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesCostModel.java`
- Test: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesCostModelTest.java`

- [ ] **Step 1: Add failing cost-model tests**

Add tests asserting:

- `Filter` calls provider filter when available and preserves interval confidence.
- `Union` adds lower/upper intervals from both inputs.
- `LeftJoin` output rows are at least left rows.
- `Difference` output rows are no greater than left rows.
- `Distinct` marks duplicate behavior as eliminating in delivered properties.
- `BindingSetAssignment` is exact and uses q-error `1.0d`.
- `Join` with statement-pattern factors asks provider for `multiPatternJoin` before generic fallback.

- [ ] **Step 2: Replace scalar `estimateRows(...)` path**

Add a private `StatisticsEstimate estimate(TupleExpr tupleExpr, Set<String> boundVars)` and route:

- `StatementPattern` -> provider `statementPattern`
- `Filter` -> provider `filter` then `EvaluationStatistics.estimateFilterPass(...)`
- `Join` -> star scan / characteristic set / multi-pattern / join-frequency / bridge path / vector join fallback
- `LeftJoin` -> provider `leftJoin` then vector fallback with left-row minimum
- `Difference` -> provider `minus` then vector anti-probe fallback
- `Union` -> provider `union` then vector union fallback
- `Projection` -> pass-through with small projection work
- `Distinct` and `Reduced` -> provider `distinct` then vector distinct fallback
- `BindingSetAssignment` -> exact row count
- `ArbitraryLengthPath` and `ZeroLengthPath` -> provider `propertyPathNodePairs`

- [ ] **Step 3: Wire feedback gating**

Use:

```java
private static final double FEEDBACK_CONFIDENCE_THRESHOLD = 0.55d;
```

Apply `rdfStatisticsProvider.feedbackCorrection(...)` only when `feedback.trusted(FEEDBACK_CONFIDENCE_THRESHOLD)` is true.

- [ ] **Step 4: Preserve exact LMDB factor tier**

Keep:

```java
.withEstimationTier(JoinFactorCostModel.EstimationTier.EXACT)
```

Do not copy v2's budgeted `STANDARD` downgrade.

- [ ] **Step 5: Avoid double-counting physical rule cost**

In `localCost(...)`, if `expression.physical()` and `expression.ruleCost()` is not `CostVector.ZERO`, return the input cost after applying policy; then the caller's existing `.plus(expression.ruleCost())` adds the physical rule cost exactly once.

- [ ] **Step 6: Rebuild plans from input winners**

Override `buildPlan(...)` so unary and binary algebra nodes get child plans from `inputWinners`. This prevents selected alternatives from losing optimized children.

- [ ] **Step 7: Run cost-model tests**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesCostModelTest,CascadesEstimateVectorTest verify
```

Expected: both classes pass.

### Task 4: Extend Statistics Provider And LMDB Overrides

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/RdfStatisticsProvider.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizerTest.java`

- [ ] **Step 1: Add SPI defaults**

Add defaults:

- `multiPatternJoin(List<TupleExpr> orderedFactors, Set<String> boundVars)`
- `filter(TupleExpr input, ValueExpr condition, StatisticsEstimate inputEstimate, Set<String> boundVars)`
- `union(StatisticsEstimate left, StatisticsEstimate right)`
- `leftJoin(StatisticsEstimate left, StatisticsEstimate right, Set<String> joinVars)`
- `minus(StatisticsEstimate left, StatisticsEstimate right, Set<String> joinVars)`
- `distinct(StatisticsEstimate input, Set<String> distinctVars)`
- `starMultiPredicateScan(List<StatementPattern> starPatterns, Set<String> boundVars)`

Keep defaults conservative and vector-preserving. Empty is better than fake certainty.

- [ ] **Step 2: Add LMDB override tests**

Extend `LmdbCascadesOptimizerTest` with checks that:

- `multiPatternJoin(...)` uses sketch planner estimates and emits q-error > 1 unless exact evidence exists.
- `filter(...)` uses LMDB learned filter selectivity.
- `starMultiPredicateScan(...)` reports independent and batched work metrics.
- feedback with confidence below `0.55` does not replace base estimate.
- feedback with confidence above `0.55` replaces rows/work and carries row/work q-error.

- [ ] **Step 3: Implement LMDB overrides**

Use existing LMDB estimators:

- `sketchBasedJoinEstimator.cardinality(...)`
- `estimateFactorCost(...)`
- `estimateFilterPass(...)`
- `estimateSubjectStar(...)`
- `estimatePropertyPath(...)`
- `estimateOperatorFeedback(...)`

Populate metrics:

- `plannedJoinFactorCount`
- `plannedJoinVarDistinctLeft`
- `plannedJoinVarDistinctRight`
- `plannedBridgeVariableDistinctRows`
- `optimizer.starMultiPredicateScanPredicates`
- `optimizer.starMultiPredicateIndependentWorkRows`
- `optimizer.starMultiPredicateBatchedWorkRows`
- `plannedWorkRows`

- [ ] **Step 4: Run LMDB focused tests**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbCascadesOptimizerTest -DskipITs verify
```

Expected: `LmdbCascadesOptimizerTest` passes.

### Task 5: Add V2 Logical Rewrite Rules

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StandardCascadesRules.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/RuleRegistry.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`
- Test: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`

- [ ] **Step 1: Add failing rule tests**

Add tests for:

- `Filter(?a && ?b)` over `Join(left, right)` splits conjuncts to visible sides and leaves residual conjuncts above the join.
- `Filter(Union(left, right))` distributes only when both branches bind condition variables.
- `Projection(Union(left, right))` distributes only when both branches bind projected variables.
- `Join(Union(a, b), c)` and `Join(a, Union(b, c))` produce union-of-joins alternatives.
- All rules handle a null goal by using `OptimizationGoal.BAG_SEMANTICS`.

- [ ] **Step 2: Implement rules**

Add classes:

- `FilterConjunctPushdownRule`
- `FilterUnionDistributionRule`
- `ProjectionUnionDistributionRule`
- `JoinUnionDistributionRule`

Add private helpers:

- `splitConjuncts(ValueExpr expression)`
- `combineConjuncts(List<ValueExpr> conjuncts)`

- [ ] **Step 3: Register rules**

Register in both generic and LMDB registries after the existing simple pushdown rules and before implementation rules.

- [ ] **Step 4: Run rule tests**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesRuleEngineTest verify
```

Expected: `CascadesRuleEngineTest` passes.

### Task 6: Add LMDB Star Multi-Predicate Scan Alternative

**Files:**
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStarJoinScanSupport.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRewriteProof.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizerTest.java`

- [ ] **Step 1: Add recognition tests**

Add tests that:

- two or more statement patterns with the same variable subject and constant distinct predicates are recognized.
- different subjects are rejected.
- duplicate predicate constants are rejected.
- non-constant predicates are rejected.

- [ ] **Step 2: Implement support class**

Implement:

- `plan(TupleExpr tupleExpr)`
- `annotate(TupleExpr tupleExpr, Plan plan, StatisticsEstimate estimate)`
- `Plan(String subjectName, List<StatementPattern> patterns, Set<String> predicateValues)`

Use metrics:

- `optimizer.starMultiPredicateScanProof`
- `optimizer.starMultiPredicateScanPredicates`
- `optimizer.starMultiPredicateScanSubject`
- `optimizer.starMultiPredicateIndependentWorkRows`
- `optimizer.starMultiPredicateBatchedWorkRows`

- [ ] **Step 3: Add rule provider implementation**

Add `LmdbStarMultiPredicateScanRule` before ordinary access-path implementation. It must:

- require `RdfStatisticsProvider`
- call `starMultiPredicateScan(...)`
- annotate the cloned join tree
- deliver access path `starMultiPredicateScan`
- cost from `estimate.vector().toCostVector()`

- [ ] **Step 4: Add rewrite proof enum values**

Add:

- `STAR_MULTI_PREDICATE_SCAN`
- `RECORD_ITERATOR_SKIP_AHEAD`

- [ ] **Step 5: Run LMDB Cascades tests**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbCascadesOptimizerTest -DskipITs verify
```

Expected: tests pass and star-scan alternatives are planner-visible. Do not claim specialized runtime execution unless a later adapter consumes the annotation.

### Task 7: Add RecordIterator Skip-Ahead Hook

**Files:**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RecordIterator.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIterator.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStatementIterator.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIteratorSkipAheadTest.java`

- [ ] **Step 1: Add failing skip-ahead test**

Create a package-local test that inserts ordered quads, opens a `RecordIterator`, calls `skipTo(subject, predicate, object, context)`, then asserts:

- the next record is at or after the target key.
- `getSkipAheadSeekCountActual()` increments.
- calling `skipTo(...)` on a closed iterator returns `false`.

- [ ] **Step 2: Add default API**

Add to `RecordIterator`:

```java
default boolean skipTo(long subject, long predicate, long object, long context) {
	return false;
}

default long getSkipAheadSeekCountActual() {
	return -1;
}
```

- [ ] **Step 3: Implement LMDB cursor seek**

Implement `LmdbRecordIterator.skipTo(...)` by preparing the next minimum key and forcing the next cursor operation to position with `MDB_SET_RANGE`. Validate against the failing test; do not rely solely on the v2 snippet if current iterator state requires a different `fetchNext` value.

- [ ] **Step 4: Expose statement iterator telemetry**

Add:

```java
public long getSkipAheadSeekCountActual() {
	return recordIt.getSkipAheadSeekCountActual();
}
```

- [ ] **Step 5: Run skip tests**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbRecordIteratorSkipAheadTest -DskipITs verify
```

Expected: skip-ahead tests pass.

### Task 8: Finish Planner Exact And Budgeted Semantics

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesPlanner.java`
- Test: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesMemoModelTest.java`

- [ ] **Step 1: Add failing planner tests**

Add tests that:

- `SearchMode.SHADOW` does not trim non-dominated frontier winners.
- `SearchMode.BUDGETED` marks the search approximate when the bounded winner frontier is reached.

- [ ] **Step 2: Apply frontier semantics**

Before adding a winner, compute:

```java
WinnerKey winnerKey = goal.key(expression.groupId());
if (goal.searchMode() == OptimizationGoal.SearchMode.BUDGETED
		&& memo.winners(winnerKey).size() >= boundedFrontierLimit) {
	state.markApproximate("bounded winner frontier reached for group " + expression.groupId());
}
```

Then call:

```java
memo.addWinner(winnerKey, winner, boundedFrontierLimit,
		goal.searchMode() != OptimizationGoal.SearchMode.BUDGETED);
```

- [ ] **Step 3: Run memo tests**

Run:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesMemoModelTest verify
```

Expected: memo tests pass.

### Task 9: Re-Verify Q7 And Broaden

**Files:**
- Format/check all touched source files.

- [ ] **Step 1: Run focused q7 IT**

Run:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py --it ThemeQueryBenchmarkSmokeIT#sparseQ7BenchmarkKeepsOfferPathBeforePersonFanout --retain-logs --stream
```

Expected: q7 no longer underestimates the `https://schema.org/about` bridge by more than 10x.

- [ ] **Step 2: Run focused unit suites**

Run:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs --stream
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptimizerPipelineTest --retain-logs --stream
python3 .codex/skills/mvnf/scripts/mvnf.py CascadesMemoModelTest --retain-logs --stream
python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs --stream
python3 .codex/skills/mvnf/scripts/mvnf.py CascadesEstimateVectorTest --retain-logs --stream
```

Expected: all pass.

- [ ] **Step 3: Run broadened modules**

Run:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs --stream
python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --stream
```

Expected: both module verifies pass.

- [ ] **Step 4: Header and formatting checks**

Run:

```bash
cd scripts && ./checkCopyrightPresent.sh
mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
```

Expected: copyright check and formatting pass.

- [ ] **Step 5: Final audit**

Run:

```bash
git status --short
git diff --stat
```

Expected: only planned files changed, protected untracked artifacts preserved, no broad source snapshot overwrite.

## Self-Review

Spec coverage:

- Robust vector signal: Tasks 1-4 cover vector shape, row intervals, q-error, confidence, evidence, feedback gating, and provider extraction.
- Broader provider coverage: Task 4 covers all v2 SPI additions and LMDB overrides.
- Logical rules: Task 5 covers all v2 logical rules.
- Star scan: Task 6 covers planner-visible LMDB star-scan support and telemetry.
- Skip-ahead: Task 7 covers iterator API and telemetry.
- Planner correctness: Task 8 covers exact/shadow no-trim and budgeted approximation marking.
- Verification: Task 9 covers focused and broadened commands.

Known deliberate differences from v2:

- Do not copy v2's budgeted `STANDARD` cost tier; keep exact LMDB estimator tier.
- Do not reduce the current extended `CostVector` to v2's smaller scalar q-error shape.
- Do not claim runtime star-scan execution until an evaluator adapter consumes the physical annotation.
