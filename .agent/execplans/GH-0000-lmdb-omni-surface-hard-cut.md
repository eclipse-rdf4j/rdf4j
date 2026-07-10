# LMDB Omni Surface Hard-Cut

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

LMDB planning currently computes rich Omni sketch witness evidence, then repeatedly narrows it into `JoinFrequencyEstimate` rows, confidence, and source strings. After this change, LMDB planning, costing, and annotations retain the complete Omni sketch surface: selected rows, row bounds, witness counts, sampling probability, fallback reason, source kind, exact-zero status, and step/binding evidence. Shared Cascades vector APIs stay scalar; LMDB keeps the full evidence in LMDB-owned records and a scoped evidence store, then derives scalar cost inputs from that evidence only at ranking boundaries.

The visible proof is that `JoinFrequencyEstimate` is gone from LMDB planning/costing, `BoundJoinProductEstimate` and finite surface records expose `omniSurface()`, and focused tests can assert fallback reason, sampling probability, witness counts, source kind, and scalar cost metrics from the same estimate.

## Progress

- [x] (2026-07-09T10:55Z) Created this ExecPlan from the accepted hard-cut plan.
- [ ] Run the required root quick install before production edits.
- [ ] Add failing tests for the new Omni surface contract.
- [ ] Add Omni surface evidence records and conversion helpers.
- [ ] Migrate `SketchBasedJoinEstimator` from `JoinFrequencyEstimate` to Omni surfaces.
- [ ] Migrate LMDB statistics, bound-product, finite-branch, and optional-bridge records.
- [ ] Add LMDB scoped Omni evidence store and planner-service accessors.
- [ ] Remove count-min join-surface planning/costing code.
- [ ] Run focused selectors, cleanup search, formatter, diff check, and module verification.

## Surprises & Discoveries

- Observation: Current worktree has unrelated modified files in LMDB config and Omni sketch classes. Implementation must preserve those edits and work with them rather than reverting them.
  Evidence: `git status --short --untracked-files=no` before this plan showed modified LMDB config files, `SketchBasedJoinEstimator.java`, `OmniJoinEstimator.java`, `OmniWitnessSet.java`, and related tests.

## Decision Log

- Decision: Use a full hard cut, not a parallel migration.
  Rationale: The user explicitly selected a hard removal of `JoinFrequencyEstimate`; keeping it as a compatibility path would preserve the scalarization bug this work is meant to remove.
  Date/Author: 2026-07-09 / Codex.

- Decision: Keep shared Cascades/core optimizer APIs unchanged and use an LMDB-owned sidecar for complete Omni evidence.
  Rationale: The user explicitly chose an LMDB sidecar. `CostVector` and `EstimateVector` remain scalar ranking primitives; LMDB surfaces carry the complete evidence separately and derive scalar inputs at the boundary.
  Date/Author: 2026-07-09 / Codex.

- Decision: Low-level count-min sketch classes may remain only for legacy storage/config tests, but planning/costing code must not call count-min join-surface APIs.
  Rationale: The requested hard cut targets planning, optimizing, and costing. Removing every low-level count-min implementation in the same change is unnecessary and risks widening the migration beyond the requested behavior.
  Date/Author: 2026-07-09 / Codex.

## Outcomes & Retrospective

No implementation outcomes yet.

## Context and Orientation

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/SketchBasedJoinEstimator.java` owns LMDB sketch ingestion and most Omni estimate construction. It currently returns `JoinFrequencyEstimate` for join surfaces and bridge chains even when it has complete `OmniWitnessSet` evidence.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java` consumes sketch estimates and converts them into `StatisticsEstimate`, `FactorCostEstimate`, and LMDB value records. It currently stores `JoinFrequencyEstimate` in records such as `BoundJoinProductEstimate`, `FiniteDerivedSurfaceEstimate`, and `FiniteBranchSurfaceEstimate`.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CostVector.java` and `EstimateVector.java` are scalar ranking types. This plan does not alter their public shape.

An Omni surface means the evidence LMDB needs to understand a query sub-plan: row estimate, bounds, work rows, confidence, sampling probability, exact-zero status, fallback reason, minimum detectable rows, binding-level witnesses, and step-level probe evidence. The evidence must be available to LMDB planners and annotators even when only scalar metrics are stamped onto query algebra nodes.

## Plan of Work

First add tests that reference the new Omni surface API and fail to compile or fail assertions. These tests define the contract before production changes. The smallest contract test is `OmniSketchSurfaceEstimateTest`; broader tests cover estimator retention, bound-product API, LMDB statistics metrics, and cleanup.

Then create the Omni evidence records in the LMDB sketch package. These records must be immutable, normalize invalid numeric input, expose string/double metric conversion helpers, and keep `OmniWitnessSet` objects available for LMDB-side consumers.

Next migrate `SketchBasedJoinEstimator`. Public surface entrypoints become Omni-named and return `OmniSketchSurfaceEstimate`. Internally, every place that currently constructs `JoinFrequencyEstimate` from Omni witnesses should instead build an Omni surface with bindings and steps. Count-min join-surface methods are removed from planning code.

Next migrate LMDB statistics and records. Any record field named `countMinEvidence` becomes `omniSurface`. Any telemetry helper named count-min surface becomes Omni surface telemetry and reads from `OmniSketchSurfaceEstimate`. Calibration and trust checks for count-min surfaces are deleted.

Next add `LmdbOmniEvidenceStore` scoped to `OptimizationCostScope`. Store evidence by tuple identity when a concrete algebra node is known and by stable fingerprint for join-order candidates. Expose read/write through `LmdbPlannerServices` so annotators, rule providers, connected planner, hypergraph planner, runtime metrics, and explain finalizer can recover complete evidence before falling back to scalar metrics.

Finally remove `JoinFrequencyEstimate.java`, update tests, run cleanup searches, format, and verify.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

1. Run the required initial quick install:

       mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

2. Add red tests:

       python3 .codex/skills/mvnf/scripts/mvnf.py OmniSketchSurfaceEstimateTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorOmniSurfaceRetentionTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBoundJoinProductBlendTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEvaluationStatisticsMemoizationTest --retain-logs

   Expected before production changes: at least the new tests fail to compile or fail because the new Omni surface types and accessors do not exist.

3. Implement the production migration described in `Plan of Work`.

4. Run the same focused selectors and expect success:

       python3 .codex/skills/mvnf/scripts/mvnf.py OmniSketchSurfaceEstimateTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorOmniSurfaceRetentionTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBoundJoinProductBlendTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEvaluationStatisticsMemoizationTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCorrelatedFilterRuleAdmissibilityTest --retain-logs

5. Run cleanup gates:

       rg -n "JoinFrequencyEstimate|countMinEvidence|estimateCountMinJoinSurface|CountMinSurface" core/sail/lmdb/src/main/java core/sail/lmdb/src/test/java
       git diff --check
       mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

6. Broaden:

       python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

## Validation and Acceptance

The migration is accepted when all focused selectors pass, `JoinFrequencyEstimate` is deleted, no planning/costing code references count-min join-surface APIs, `BoundJoinProductEstimate` and finite surface records expose `omniSurface()`, and LMDB statistics still emits scalar cost metrics such as `plannedSketchConfidence`, `plannedOmniJoinSurfaceRows`, `plannedBoundJoinProductRows`, and witness-count metrics derived from the full Omni surface.

If broad `core/sail/lmdb` verification reports unrelated baseline failures, capture the focused green selectors and the failing report snippets, then document why the failure is outside the changed surface before stopping.

## Idempotence and Recovery

The implementation is additive until `JoinFrequencyEstimate` and count-min planning APIs are removed. If a step fails, inspect the focused test report, update this ExecPlan with the discovery, and continue from the smallest failing selector. Do not revert unrelated user changes in the dirty worktree.

If the offline quick install fails due to missing dependencies, rerun the same install command once without `-o`, then return to offline commands.

## Artifacts and Notes

Initial dirty files before implementation included LMDB config and Omni sketch files. This plan intentionally does not claim ownership of those pre-existing modifications.

## Interfaces and Dependencies

Create these package-visible or public records in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/`:

- `OmniSketchBindingEvidence(String bindingName, double rows, double distinctRows, int witnessCount, OmniWitnessSet witnesses, OmniWitnessSet.SourceKind sourceKind, String relation, String attribute)`
- `OmniSketchStepEvidence(String operatorKind, String inputBindingName, String outputBindingName, double inputRows, double probeRows, double outputRows, int inputWitnesses, int probeWitnesses, int outputWitnesses, double samplingProbability, String fallbackReason, OmniWitnessSet.SourceKind sourceKind, boolean exactZero, String relation, String predicateKeyKind)`
- `OmniSketchSurfaceEstimate(double rows, double lowerBoundRows, double upperBoundRows, double workRows, double confidence, double samplingProbability, boolean exactZero, String fallbackReason, double minimumDetectableRows, String source, String surfaceKind, String predicateKeyKind, Map<String, String> stringMetrics, Map<String, Double> doubleMetrics, Map<String, OmniSketchBindingEvidence> bindings, List<OmniSketchStepEvidence> steps)`

`OmniSketchSurfaceEstimate` must provide helper methods named `selectedRows()`, `calibratedRows()` as an alias for migration readability, `toDoubleMetrics()`, `toStringMetrics()`, `toBagEstimate(Set<String> bindingNames)`, and `toCostInputs()`.

Create `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOmniEvidenceStore.java` with identity and fingerprint maps for `OmniSketchSurfaceEstimate`.

No new third-party dependencies are allowed.
