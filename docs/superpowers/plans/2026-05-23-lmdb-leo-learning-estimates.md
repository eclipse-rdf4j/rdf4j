# LMDB LEO-Style Learning Estimates

This ExecPlan is a living document. Keep `Progress`, `Discoveries`, `Decision Log`, and `Outcomes`
current as implementation proceeds.

## Purpose

Extend LMDB's existing operator-feedback cache into a more useful LEO-style learning layer for optimizer
diagnostics and cost selection. The immediate goal is not to replace sketches or page walking. The goal is to
make every learned correction explainable, measure q-error, carry uncertainty into planner metrics, and expose
bridge/star/path evidence so benchmark plans can show why the optimizer missed the cheapest plan.

## Progress

- [x] Ran required root quick install. Single-thread quick install passes; the earlier parallel clean/test-compile
  failure looks like a build artifact race.
- [x] Read `LmdbOperatorFeedbackStats`, `LmdbEvaluationStatistics`, and planner-feedback call sites.
- [x] Checked local LEO/q-error/JoinSketch papers for the formulas to encode.
- [ ] Add failing unit tests for q-error learning and persistence.
- [ ] Add failing planner test proving q-error/uncertainty is visible in selected estimates.
- [ ] Implement q-error summaries in operator feedback.
- [ ] Propagate q-error metrics through fused cost estimates and estimate trace.
- [ ] Add bridge/star shape telemetry hooks for future generalized statistics repair.
- [ ] Run focused tests, hygiene, and the split planning/run benchmark.

## Discoveries

- Existing LMDB feedback already records actual operator outcomes and applies learned estimates to `Join`,
  `LeftJoin`, `Union`, and `Difference`.
- Feedback is currently keyed by a structural operator key and canonical join-surface factors, not by reusable
  q-error/shape summaries.
- `LmdbEvaluationStatistics` already has estimate tracing via `rdf4j.optimizer.lmdb.estimateTrace`; this is the
  right place to include q-error and source details instead of adding a separate logger.
- Connected page-walk blending, optional bridge products, and duplicate-corrected bound join rows already exist.
  The missing part is observability and robust use of learned uncertainty.
- The q-error paper defines multiplicative symmetric error and motivates robust planning because bounded q-error
  bounds bad-plan risk.
- JoinSketch's relevant math is the frequency-vector inner product for duplicate-preserving joins:
  `|R join S| = sum_x freq_R(x) * freq_S(x)`. LMDB bridge estimates should surface which bridge variable and
  correction surface were used so we can compare alternatives.

## Decision Log

- Use Routine D because this is a significant optimizer feature set.
- Use Routine A inside the plan for behavior-changing code: failing focused tests before production edits.
- Implement the first production slice as q-error telemetry and uncertainty propagation, because it is needed to
  diagnose whether future bridge/star formulas improve plan selection.
- Keep mid-query reoptimization, characteristic-set storage, and new persistent RDF star/path sketches out of this
  first slice unless tests force them; they need broader storage design.
- Keep learned corrections bounded by the existing clamp rules, but report q-error separately so clamps do not
  hide estimate failures.

## Outcomes

To be filled after implementation and benchmark analysis.

## Context

Primary files:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStats.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizer.java`
- `core/query/src/main/java/org/eclipse/rdf4j/query/explanation/TelemetryMetricNames.java`

Existing test anchors:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStatsTest.java`
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackPlanningTest.java`
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatisticsMemoizationTest.java`

## Plan of Work

1. Add feedback-stat tests.
   Record bad planned-vs-actual outcomes and require learned estimates to expose row/work q-error, sample count,
   q-error max, and q-error mean. Persist and reload the summaries.

2. Add planner-surface tests.
   Train a query once, re-plan it, and require selected operator-feedback estimates to show q-error/uncertainty
   metrics in optimized explanation output.

3. Implement q-error summaries.
   Store row/work q-error sums and maxima in `LearnedOperatorCounts`. Compute q-error as
   `max(actual / estimate, estimate / actual)` with stable zero handling: equal zero is `1`, one zero side is the
   clamp ceiling. Expose mean and max q-error on `OperatorEstimate`.

4. Propagate uncertainty.
   In `LmdbEvaluationStatistics.fusedFeedbackEstimate`, add planned metrics for base rows/work, learned rows/work,
   q-error mean/max, confidence, and uncertainty rows. Make the objective score reflect work plus bounded learned
   uncertainty only for low-confidence/high-error feedback.

5. Add estimate trace coverage.
   Extend estimate tracing to print planned operator-feedback q-error and uncertainty metrics.

6. Add bridge/star telemetry hooks.
   Where optional bridge and bound-join product estimates are selected, stamp variable/source metrics consistently
   so benchmark plans show the bridge candidate and correction surface used.

7. Verify and benchmark.
   Run focused unit tests, quick install, hygiene, then the planning/run benchmark for the affected theme query.

## Concrete Steps

Required baseline before test groups:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
```

Focused red/green:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbOperatorFeedbackStatsTest#operatorFeedbackRecordsQErrorSummary test
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbOperatorFeedbackPlanningTest#operatorFeedbackPlanExposesLearnedQError test
```

Focused gate:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbOperatorFeedbackStatsTest,LmdbOperatorFeedbackPlanningTest,LmdbEvaluationStatisticsMemoizationTest test
```

Hygiene:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
./checkCopyrightPresent.sh
git diff --check
```

Benchmark:

```bash
scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryPlanRunBenchmark --method planQuery --no-build --warmup-iterations 1 --measurement-iterations 3 --measurement-time 3s --forks 1
scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryPlanRunBenchmark --method runQuery --no-build --warmup-iterations 1 --measurement-iterations 3 --measurement-time 3s --forks 1
```

## Validation

Each production change must have failing test evidence first. Passing evidence must come from the same focused
selection, then the broader focused gate. Benchmark results are diagnostic, not a correctness gate.

## Idempotence and Recovery

The sidecar format must reject older versions cleanly. New q-error fields must survive reload and reset with the
estimator revision. If persistence format changes break old sidecars, version bump and ignore old files rather
than attempting partial reads.

## Artifacts

- Initial root install evidence: `initial-evidence.txt`
- This plan: `docs/superpowers/plans/2026-05-23-lmdb-leo-learning-estimates.md`
- Estimate trace toggle: `-Drdf4j.optimizer.lmdb.estimateTrace=true`

## Interfaces

Expected new or extended metrics:

- `plannedOperatorFeedbackRowQErrorMean`
- `plannedOperatorFeedbackRowQErrorMax`
- `plannedOperatorFeedbackWorkQErrorMean`
- `plannedOperatorFeedbackWorkQErrorMax`
- `plannedOperatorFeedbackUncertaintyRows`
- `plannedBridgeCorrectionSource`
- `plannedBridgeCorrectionJoinVar`

Expected API extension:

- `LmdbOperatorFeedbackStats.OperatorEstimate` gains q-error and uncertainty fields.
