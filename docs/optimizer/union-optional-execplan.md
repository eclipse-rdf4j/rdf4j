# UNION + OPTIONAL Optimizer Harness and Safe Rewrite Plan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `PLANS.md` at the repository root.

## Purpose / Big Picture

The goal is to make UNION and OPTIONAL (LeftJoin) queries faster without changing SPARQL semantics. After this change, a developer can run a deterministic harness that compares baseline vs candidate optimizers, emits per-node estimates and actuals to CSV, and can toggle new rewrite rules at runtime. Success is visible by running the harness and seeing identical results with measurable improvements in at least one skewed UNION or selective OPTIONAL case.

## Progress

- [x] (2025-12-27 21:20+01:00) Pin pipeline and evaluation entrypoints
- [x] (2025-12-27 21:27+01:00) Build harness module and run baseline
- [x] (2025-12-27 21:28+01:00) Write baseline optimizer overview doc
- [x] (2025-12-27 21:40+01:00) Rewrite ExecPlan to PLANS format
- [x] (2025-12-27 21:42+01:00) Add rewrites and rollout docs
- [x] (2025-12-27 21:39+01:00) Add Union flattening rule tests
- [x] (2025-12-27 21:39+01:00) Implement Union flattening optimizer rule
- [x] (2025-12-27 21:44+01:00) Add Union reorder rule and tests
- [ ] (2025-12-27 21:40+01:00) Add Optional safe rules and tests
- [ ] (2025-12-27 21:40+01:00) Refine estimator and expand harness

## Surprises & Discoveries

RDF4J already applies UNION and OPTIONAL related rewrites in the standard pipeline. `QueryModelNormalizerOptimizer` distributes Join over Union and lifts well-designed LeftJoin over Join, and `FilterOptimizer` pushes filters into Union arms. Any new rules must avoid duplicating or conflicting with these. Evidence is in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryModelNormalizerOptimizer.java` and `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/FilterOptimizer.java`.

## Decision Log

Decision: Place the harness in a new tools sub-module `tools/optimizer-harness` with a CLI main class. Rationale: A tools module isolates harness concerns from core and keeps CSV artifacts local and optional. Date/Author: 2025-12-27 / Codex.

Decision: Do not auto-commit during implementation despite PLANS.md guidance. Rationale: `AGENTS.md` forbids commits unless explicitly requested, which is higher priority. Date/Author: 2025-12-27 / Codex.

## Outcomes & Retrospective

Milestone A is complete. The harness module builds and runs on a small profile, writing baseline and candidate CSV plus summary files under `tools/optimizer-harness/target/harness/run-<timestamp>/`. No optimizer behavior changes yet; candidate equals baseline.

The baseline doc captures the current pipeline, UNION/OPTIONAL evaluation classes, and statistics hooks.
The rewrites and rollout docs describe safe transformations and toggles.
Milestone B is complete with Union flattening behind flags and tests covering enabled and disabled paths. Milestone C is complete with Union arm reordering and tests for enabled and disabled paths.

## Context and Orientation

RDF4J parses SPARQL into a tuple expression (algebra tree). UNION is represented by `Union` nodes and OPTIONAL by `LeftJoin` nodes. The optimizer pipeline runs in `StandardQueryOptimizerPipeline` and is executed by `DefaultEvaluationStrategy.optimize`. Evaluation occurs in `DefaultEvaluationStrategy`, which delegates UNION to `UnionQueryEvaluationStep` and OPTIONAL to `LeftJoinQueryEvaluationStep`. Query plan metrics (result size estimate/actual, cost estimate, total time) are stored on `AbstractQueryModelNode` and exposed in `GenericPlanNode` via `QueryModelTreeToGenericPlanNode`. The harness uses `Explanation.Level.Timed` to populate these metrics without custom iterator wrappers.

Key files are `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java`, `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`, `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/UnionQueryEvaluationStep.java`, and `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/LeftJoinQueryEvaluationStep.java`.

## Plan of Work

Milestone A delivers the harness and baseline documentation without changing optimizer behavior. Milestone B introduces UNION flattening behind a flag and adds tests that prove structure changes without altering results. Milestone C adds UNION arm reordering with conservative estimates and skewed harness cases. Milestone D adds OPTIONAL-safe improvements that only reorder the left-hand side joins and apply conservative filter handling. Milestone E refines estimates and expands adversarial harness cases.

## Concrete Steps

For Milestone B, add a failing test that expects nested UNIONs to be flattened when the feature flag is enabled, then add a passing test that keeps nested UNIONs when the flag is disabled. After the failing test is observed, implement `UnionFlatteningOptimizer` and wire it into the standard pipeline when `rdf4j.optimizer.unionOptional.enabled` and `rdf4j.optimizer.unionOptional.flatten.enabled` are true. Re-run the same focused tests and then the module tests.

For Milestone C, add a failing test that expects UNION arms to reorder by estimated cost when `rdf4j.optimizer.unionOptional.unionReorder.enabled` is true, then implement the reorder logic with conservative estimate guards. Add a harness case with skewed UNION arms and confirm a measurable improvement.

For Milestone D, add tests that guard OPTIONAL semantics: one with a FILTER outside OPTIONAL referencing optional variables (must not be moved inside) and one with a correlated optional. Implement LHS-only join reordering and safe filter handling that never crosses the OPTIONAL boundary unless the filter is already scoped inside it.

For Milestone E, use harness summaries to identify estimates with >100x error and refine heuristics while keeping bounds and confidence checks intact.

Command examples are run from repository root and use the project conventions. For a single test method, run:

  python3 .codex/skills/mvnf/scripts/mvnf.py UnionOptionalOptimizerTest#testUnionFlatteningEnabled

For module verification after changes, run:

  python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation

## Validation and Acceptance

Milestone A is accepted when the harness produces baseline and candidate CSVs with non-zero per-node metrics and identical results, and a summary file of worst estimation errors. Milestones B through D are accepted when each rule has a positive and a negative test, the harness shows no mismatches, and at least one skewed case improves without exceeding the regression threshold. Overall acceptance requires harness runtime under one minute for the small profile, toggleable optimizers, and no correctness mismatches.

## Idempotence and Recovery

All steps are additive and can be re-run safely. If a harness run fails or produces mismatches, delete the run output directory and re-run with the same seed. If a rewrite causes regressions, disable its flag and re-run the harness to confirm baseline behavior.

## Artifacts and Notes

Expected harness outputs use a run directory under `tools/optimizer-harness/target/harness/`. Example paths are:

  tools/optimizer-harness/target/harness/run-20251227-202559/baseline.csv
  tools/optimizer-harness/target/harness/run-20251227-202559/candidate.csv
  tools/optimizer-harness/target/harness/run-20251227-202559/plans/query-union-skew-baseline.txt

## Interfaces and Dependencies

The harness depends on `rdf4j-repository-sail`, `rdf4j-sail-memory`, `rdf4j-queryparser-sparql`, and `rdf4j-query`. The CLI entry point remains `org.eclipse.rdf4j.tools.optimizer.harness.HarnessRunner` and accepts flags for seed, dataset shape, and output. Optimizer rules are guarded by `rdf4j.optimizer.unionOptional.enabled` and per-rule flags.

Revision Note (2025-12-27): Updated progress for Milestone C and kept the plan in PLANS.md prose format with harness notes and supporting docs.
