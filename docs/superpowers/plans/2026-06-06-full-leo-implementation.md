# Full LEO Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete LEO so RDF4J LMDB can learn from completed execution, feed learned surfaces into Cascades, and safely reoptimize remaining work when first-run plans go badly wrong.

**Architecture:** Keep the existing conservative operator feedback as the seed, but split it into three layers: query-local identity/surface modeling, persistent LMDB learned statistics, and Cascades/adaptive consumers. The planner core must use symbols, ordinals, and masks internally; names remain only at query boundaries and telemetry. Runtime adaptation starts with safe unopened-subtree checkpoints, then expands only when correctness and duplicate semantics are proven.

**Tech Stack:** Java 25, RDF4J query algebra, LMDB Sail, Cascades optimizer, FastAGMS sketches, Surefire/Failsafe, JMH, JaCoCo.

---

## File Structure

**Core planner identity and LEO SPI**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/BindingUniverse.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/BindingMask.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoBindingShape.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoOperatorKey.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoObservation.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoCorrection.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoSurfaceEstimate.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoFeedbackProvider.java`

**LMDB learned feedback store**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStats.java`
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLeoFeedbackConfig.java`
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLeoFeedbackStore.java`
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLeoSurfaceStats.java`
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLeoFeedbackRecorder.java`

**Planner consumers**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesCostModel.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesPlanner.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/EstimateVector.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cost/EvidenceProfile.java`

**Finite anchors and virtual sketches**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/VirtualSketchFactor.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/QueryLocalFiniteRelation.java`

**Adaptive runtime**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/ReoptimizingQueryEvaluationStep.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoReoptimizationContext.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoReoptimizationDecision.java`

**Physical alternatives needed by LEO**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/AdaptiveHashAntiJoinIteration.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/AdaptiveHashSemiJoinIteration.java`

**Tests and benchmarks**
- Modify: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStatsTest.java`
- Modify: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackPlanningTest.java`
- Modify: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizerTest.java`
- Create: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoOperatorKeyTest.java`
- Create: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoConfidenceModelTest.java`
- Create: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoReoptimizationDecisionTest.java`
- Modify: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/LmdbThemeQueryRegressionIT.java`
- Modify: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryPlanRunBenchmark.java`

---

## Task 1: Establish Baseline and Guardrails

**Files:**
- Read: all files listed above
- Write: `initial-evidence.txt`

- [ ] **Step 1: Capture current focused green**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackStatsTest --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackPlanningTest --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
```

Expected: all pass. Persist compact evidence with:
```bash
python3 scripts/agent-evidence.py --command "focused LEO baseline" \
  core/sail/lmdb/target/surefire-reports \
  core/queryalgebra/evaluation/target/surefire-reports > initial-evidence.txt
```

- [ ] **Step 2: Capture benchmark baseline**

Run:
```bash
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query MEDICAL_RECORDS:7
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query LIBRARY:7
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query TRAIN:2
```

Expected: record current `ThemeQueryPlanRunBenchmark.runQuery` numbers and optimized-plan telemetry before changing planner behavior.

---

## Task 2: Introduce Stringless LEO Keys

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoBindingShape.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoOperatorKey.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/BindingUniverse.java`
- Test: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoOperatorKeyTest.java`

- [ ] **Step 1: Write failing alpha-equivalence key tests**

Test cases:
- `?a knows ?b JOIN ?b knows ?c` and `?x knows ?y JOIN ?y knows ?z` produce the same structural key.
- Different constants produce different keys.
- `VALUES` key includes arity and duplicate row multiplicity, but not Java object identity.
- `Union` branch order does not affect key.
- `Difference` left/right order remains significant.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LeoOperatorKeyTest --retain-logs
```

Expected: compile failure for missing LEO key classes.

- [ ] **Step 2: Implement immutable key model**

Required public shapes:
```java
public record LeoBindingShape(BindingMask possible, BindingMask assured, BindingMask nullable) {}

public record LeoOperatorKey(
		String operatorType,
		String structuralFingerprint,
		BindingMask outputMask,
		BindingMask leftMask,
		BindingMask rightMask,
		BindingMask sharedMask,
		String executionMode) {}
```

Rules:
- Use `BindingUniverse` IDs and `BindingMask`.
- Use value IDs or stable RDF value lexical fingerprints only at constant boundaries.
- Do not use `Set<String>` in hot planner matching.
- Keep rendering back to names only for telemetry.

- [ ] **Step 3: Migrate key generation behind compatibility wrapper**

Modify `LmdbOperatorFeedbackStats.keyFor(...)` to delegate to `LeoOperatorKey` when a `BindingUniverse` is available, and keep old keying only as persistence migration fallback.

- [ ] **Step 4: Verify**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LeoOperatorKeyTest --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackStatsTest --retain-logs
```

Expected: both pass. Telemetry still prints human-readable binding names.

---

## Task 3: Version Feedback Store V2

**Files:**
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLeoFeedbackConfig.java`
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLeoFeedbackStore.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStats.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStatsTest.java`

- [ ] **Step 1: Add failing persistence migration tests**

Cases:
- V1 sidecar loads and rewrites to V2 without losing scalar observations.
- Estimator snapshot revision mismatch resets V2.
- Store mutation reset clears scalar and surface sections.
- Bounded eviction keeps most recent/highest-confidence entries.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackStatsTest --retain-logs
```

Expected: fail on missing V2 format behavior.

- [ ] **Step 2: Implement V2 sections**

Sections:
- header: magic, version, estimator revision, store revision
- scalar operators
- learned fanout surfaces
- confidence metadata
- workload profile key

Config properties:
```text
rdf4j.optimizer.lmdb.leo.mode=off|record|learn|adaptive
rdf4j.optimizer.lmdb.leo.profile=default
rdf4j.optimizer.lmdb.leo.maxEntries=...
rdf4j.optimizer.lmdb.leo.maxSurfaceBuckets=...
```

Default mode: `record` for completed-query passive collection only when an LMDB feedback store is configured; `learn` and `adaptive` must be explicit until benchmark and production safety gates pass.

- [ ] **Step 3: Verify**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackStatsTest --retain-logs
```

Expected: persistence and reset tests pass.

---

## Task 4: Replace Aggressive Confidence

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoCorrection.java`
- Create: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoConfidenceModelTest.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStats.java`

- [ ] **Step 1: Write failing confidence tests**

Cases:
- One sample has low confidence, not `0.75`.
- Stable repeated samples climb gradually.
- High variance caps confidence.
- Recent samples carry more weight than stale samples.
- Outliers are quarantined until repeated.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LeoConfidenceModelTest --retain-logs
```

Expected: fail until confidence model exists.

- [ ] **Step 2: Implement confidence model**

Use:
- EWMA rows/work ratios
- sample count
- variance/coefficient of variation
- q-error mean/max
- recency epoch
- outlier bucket

Rule of thumb:
- first sample confidence <= `0.35`
- stable 3 samples <= `0.65`
- stable 8 samples may reach `0.90`
- max confidence `0.95`
- any exact/protected estimate remains immune

- [ ] **Step 3: Verify existing planning telemetry**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackPlanningTest --retain-logs
```

Expected: update assertions from fixed `0.75` to model-derived confidence and explicit telemetry.

---

## Task 5: Learn Fanout Surfaces

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoSurfaceEstimate.java`
- Create: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbLeoSurfaceStats.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStats.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStatsTest.java`

- [ ] **Step 1: Write failing surface tests**

Cases:
- Join learns output per distinct left binding for shared mask.
- LeftJoin learns match rate and matched fanout separately.
- Difference learns reject rate and RHS probe work.
- Union learns branch rows independently.
- Surface is not applied when binding masks or physical access path differ.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackStatsTest --retain-logs
```

Expected: fail for missing surface estimates.

- [ ] **Step 2: Implement bounded surfaces**

Store:
- key: `LeoOperatorKey` plus shared/bound masks
- totals: left rows, right rows, output rows, work rows
- derived: fanout, match rate, reject rate, branch rows
- confidence and variance

Keep bounded:
- max buckets per key
- decay stale buckets
- collapse low-frequency buckets into default bucket

- [ ] **Step 3: Verify**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackStatsTest --retain-logs
```

Expected: scalar feedback still passes; new surface tests pass.

---

## Task 6: Feed Surfaces into EvidenceProfile

**Files:**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cost/EvidenceProfile.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesCostModel.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackPlanningTest.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizerTest.java`

- [ ] **Step 1: Write failing Cascades feedback-evidence tests**

Cases:
- Feedback adds evidence count > 0 to a join winner.
- Learned surface changes alternative ranking when base estimates are non-exact.
- Exact finite lookup and finite derived surface are not overridden.
- Physical access work floor is preserved.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py 'LmdbOperatorFeedbackPlanningTest+LmdbCascadesOptimizerTest' --retain-logs
```

Expected: fail until surface evidence reaches Cascades.

- [ ] **Step 2: Implement provider methods**

`LmdbEvaluationStatistics` must expose:
- `feedbackCorrection(...)` for scalar correction
- `surfaceCorrection(...)` for mask-aware fanout
- telemetry: `plannedEstimateSource=leo-surface`, `plannedLeoEvidenceCount`, `plannedLeoConfidence`, `plannedLeoKey`

`CascadesCostModel` must:
- treat LEO as evidence, not just scalar vector replacement
- propagate q-error/uncertainty into objective
- keep exact/protected source immunity

- [ ] **Step 3: Verify**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackPlanningTest --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
```

Expected: feedback evidence visible in optimized plans.

---

## Task 7: Query-Local Finite Relations and Virtual Sketches

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/QueryLocalFiniteRelation.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/VirtualSketchFactor.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- Test: existing sketch estimator tests plus new focused tests in the same module.

- [ ] **Step 1: Write failing finite-anchor tests**

Cases:
- Unary `VALUES ?x` joins through two statement patterns and estimates downstream fanout.
- Tuple `VALUES (?x ?y)` preserves arity and duplicate multiplicity.
- Unsupported values reject cleanly and fall back.
- Direct tiny object lookup stays exact and does not use sketch.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorTest --retain-logs
```

Expected: fail or add a new focused test class if no existing class fits.

- [ ] **Step 2: Implement query-local virtual factors**

Requirements:
- Reuse FastAGMS seeds/hash layout.
- Never write finite anchor values into persisted store sketches.
- Cache virtual sketches per optimization scope.
- Keep exact row/value representation for small anchors.
- Preserve duplicate rows.

- [ ] **Step 3: Integrate with cost selection**

Use finite sketch when:
- finite anchor derives a downstream binding through prefix factors
- target access-path estimate is non-exact
- sketch materially disagrees with singleton direct lookup

Telemetry:
```text
plannedEstimateSource=lmdb-finite-anchor-sketch
plannedFiniteAnchorRows=...
plannedFiniteAnchorSketchRows=...
```

- [ ] **Step 4: Verify benchmark smoke**

Run:
```bash
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query LIBRARY:7
```

Expected: planned rows reflect branch fanout, not singleton direct lookup.

---

## Task 8: Add Missing Physical Alternatives

**Files:**
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/AdaptiveHashAntiJoinIteration.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/AdaptiveHashSemiJoinIteration.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizerTest.java`

- [ ] **Step 1: Write failing physical-alternative tests**

Cases:
- `Difference` has correlated probe and materialized anti-semi alternatives.
- `EXISTS` has correlated probe and materialized semi-join alternatives.
- Materialized RHS is cached once per plan evaluation.
- If RHS exceeds configured materialization cap, runtime falls back without wrong results.
- Cascades chooses correlated probe only when outer rows and RHS lookup work justify it.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
```

Expected: fail until operators and rules exist.

- [ ] **Step 2: Implement adaptive iterators**

Behavior:
- Build RHS hash/materialization until cap.
- If cap exceeded before completion, close partial materialization and use existing iterator/probe implementation.
- Preserve SPARQL bag and compatibility semantics.
- Emit telemetry: `plannedLeoPhysicalAlternative`, `actualAdaptiveFallback`, `actualMaterializedRows`.

- [ ] **Step 3: Register Cascades rules**

Add rules:
- `lmdb-adaptive-materialized-minus-anti-semi`
- `lmdb-adaptive-materialized-exists-semi`

Cost:
- build RHS once
- probe left once
- include fallback risk in objective when RHS uncertainty is high

- [ ] **Step 4: Verify MEDICAL_RECORDS q7**

Run:
```bash
./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/leo-med-q7.log -- --store lmdb --theme MEDICAL_RECORDS --query-index 7 --query-id leo-med-q7 --compare-latest --diff-mode structure+estimates
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query MEDICAL_RECORDS:7
```

Expected: no correlated anti-exists regression; plan telemetry shows adaptive materialized anti-semi when costed best.

---

## Task 9: Memo-Aware Feedback Repair

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesPlanner.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesCostModel.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/EstimateVector.java`
- Test: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizerTest.java`

- [ ] **Step 1: Write failing memo repair tests**

Cases:
- Feedback can revive an alternative previously dominated by bad base estimate.
- Feedback does not create new algebra rewrites by itself.
- Feedback marks alternatives as `leo-corrected`, not `exact`.
- Missing physical alternative remains visible as `leo-missing-alternative`, not silently scalar corrected.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
```

Expected: fail until memo feedback metadata exists.

- [ ] **Step 2: Add feedback-aware alternative scoring**

Planner must:
- attach feedback source to `MemoExpr` estimates
- include feedback q-error and confidence in dominance
- avoid pruning high-uncertainty alternatives too early
- expose telemetry for winner changes caused by LEO

- [ ] **Step 3: Verify**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
```

Expected: memo-aware tests pass; no existing Cascades tests regress.

---

## Task 10: Safe Mid-Query Reoptimization

**Files:**
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoReoptimizationContext.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoReoptimizationDecision.java`
- Create: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/ReoptimizingQueryEvaluationStep.java`
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`
- Test: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/leo/LeoReoptimizationDecisionTest.java`

- [ ] **Step 1: Write failing decision tests**

Cases:
- Does not reoptimize incomplete or Slice-limited roots.
- Reoptimizes only unopened child subtree.
- Requires q-error threshold and enough remaining estimated work.
- Does not reoptimize when query has unstable SERVICE/custom function boundary.
- Produces telemetry explaining skip or replan.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LeoReoptimizationDecisionTest --retain-logs
```

Expected: fail until decision model exists.

- [ ] **Step 2: Implement decision model**

Inputs:
- actual rows/work observed at checkpoint
- planned rows/work
- remaining subtree estimate
- operator support
- confidence threshold
- time budget

Outputs:
- `SKIP_LOW_QERROR`
- `SKIP_UNSAFE_SCOPE`
- `SKIP_TOO_LITTLE_WORK_LEFT`
- `REPLAN_REMAINING_SUBTREE`

- [ ] **Step 3: Implement runtime wrapper**

Initial safe checkpoints:
- before opening RHS of `Join`
- before opening RHS of `LeftJoin`
- before opening RHS of `Difference`
- before opening path expansion queue

Correctness rule: never discard already emitted rows; only replan unopened work.

- [ ] **Step 4: Verify integration**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LeoReoptimizationDecisionTest --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOperatorFeedbackPlanningTest --retain-logs
```

Expected: reoptimization is opt-in with `rdf4j.optimizer.lmdb.leo.mode=adaptive`.

---

## Task 11: Normalize Rewrites and Diagnose Missing Alternatives

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StandardCascadesRules.java`
- Modify: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`
- Test: existing Cascades rule tests or new focused test class.

- [ ] **Step 1: Write failing rewrite tests**

Cases:
- `?x = c1 || ?x = c2` becomes safe finite relation alternative.
- `IN` becomes finite relation alternative.
- Safe new-scope removal happens only when variables cannot capture/escape.
- Deterministic filters over exact finite relations evaluate exactly.

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
```

Expected: fail until rewrite alternatives exist.

- [ ] **Step 2: Implement as alternatives, not forced rewrites**

Rules must:
- produce costed alternatives
- keep original algebra legal
- prove safety in `RuleProof`
- expose telemetry when selected

- [ ] **Step 3: Verify no monkey patches**

Run plan snapshots for:
```bash
./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/leo-library-q7.log -- --store lmdb --theme LIBRARY --query-index 7 --query-id leo-library-q7 --compare-latest --diff-mode structure+estimates
./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/leo-med-q7.log -- --store lmdb --theme MEDICAL_RECORDS --query-index 7 --query-id leo-med-q7 --compare-latest --diff-mode structure+estimates
```

Expected: telemetry explains rules and evidence; no query-specific branch.

---

## Task 12: JaCoCo Coverage Audit

**Files:**
- Modify if needed: root `pom.xml`
- Modify if needed: module `pom.xml` files for JaCoCo Java 25/26 compatibility

- [ ] **Step 1: Run coverage on focused modules**

Run:
```bash
python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation -- -Pjacoco
python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb -- -Pjacoco
```

Expected: JaCoCo runs on current JDK. If JaCoCo fails due class file version, update JaCoCo plugin version in root build config.

- [ ] **Step 2: Inspect inactive paths**

Check coverage for:
- LEO key generation
- V2 persistence migration
- surface correction
- protected exact estimates
- adaptive fallback
- reoptimization skip reasons

Add focused tests until every new branch has direct coverage.

---

## Task 13: Benchmark and Acceptance Loop

**Files:**
- Modify: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryPlanRunBenchmark.java`
- Modify: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/AASQueriesBenchmark.java`
- Write benchmark logs under existing result folders only when intentionally preserving them.

- [ ] **Step 1: Run first-run, second-run, adaptive comparisons**

For each target:
```bash
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query LIBRARY:7
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:5
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query MEDICAL_RECORDS:7
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query PHARMA:5
./scripts/run-single-benchmark.sh --theme-plan-run --theme-query TRAIN:2
```

Run each mode:
- `leo.mode=off`
- `leo.mode=learn` after one training execution
- `leo.mode=adaptive`

- [ ] **Step 2: Run AAS run-only benchmark**

Run:
```bash
java -jar core/sail/lmdb/target/jmh-benchmarks.jar AASQueriesBenchmark.runQuery
```

Expected: runQuery excludes planning/optimization; compare against existing AAS baseline.

- [ ] **Step 3: Acceptance criteria**

Accept only if:
- No result changes.
- No benchmark-specific overrides.
- Same-method May comparison is within 10-20 percent unless telemetry shows remaining missing physical operator.
- `plannedCascadesEvidenceCount` or LEO evidence is non-zero where learned evidence is used.
- MEDICAL_RECORDS q7 no longer chooses correlated anti-exists when materialized/adaptive anti-semi is cheaper.
- TRAIN q2 either reaches baseline or telemetry clearly shows the remaining missing aggregate/count pushdown.
- LIBRARY q7 finite-anchor fanout uses finite relation/sketch evidence, not singleton lookup.

---

## Final Verification

Run:
```bash
scripts/checkCopyrightPresent.sh
mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
git diff --check
python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
```

Expected:
- all tests pass
- formatter clean
- no whitespace errors
- benchmark evidence saved
- final summary lists completed LEO mode matrix: `off`, `record`, `learn`, `adaptive`

---

## Self-Review

Spec coverage:
- Adaptive reoptimization: Task 10.
- Memo-aware repair: Task 9.
- Learned distributions/fanout surfaces: Task 5 and Task 6.
- Stringless planner identity: Task 2.
- Conservative confidence, decay, outliers: Task 4.
- Tracking mode/default safety: Task 3.
- Query-local virtual sketches and finite anchors: Task 7.
- Missing rewrites/physical alternatives: Task 8 and Task 11.
- Benchmark loop and May comparison: Task 13.

Known sequencing constraint:
- Do not implement adaptive runtime before physical alternatives and confidence gates exist.
- Do not enable `learn` or `adaptive` by default until benchmark and correctness gates pass.
- Do not replace exact finite estimates with learned feedback.
