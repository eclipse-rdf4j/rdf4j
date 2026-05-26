# LMDB Cascades Optimizer North Star Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make RDF4J/LMDB planning a Cascades-first optimizer where proven SPARQL algebra facts define the search space, bounded duplicate-aware estimates rank alternatives, LEO-style feedback repairs costs before winner selection, and execution operators implement the physical plans the optimizer chooses.

**Architecture:** Cascades becomes the contract: logical alternatives, physical alternatives, proof annotations, required/delivered properties, estimates, feedback, and pruning decisions all live in or are attached to the memo. Existing LMDB rewrite and sketch-join behavior is migrated behind Cascades rules and physical implementations, then removed or left only as compatibility instrumentation.

**Tech Stack:** Java 25, Maven offline with `.m2_repo`, RDF4J query algebra, LMDB Sail, Cascades memo classes under `core/queryalgebra/evaluation`, LMDB adapter classes under `core/sail/lmdb`, JUnit 5, Maven Surefire/Failsafe, JMH/theme benchmarks, local paper index under `papers/.paper-index`.

---

This ExecPlan is a living document. Maintain it according to `.agent/PLANS.md`: keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current whenever work proceeds.

## Purpose / Big Picture

After this plan is implemented, LMDB query planning is no longer a chain of independent eager rewrites followed by a separate join-order picker. A query with joins, filters, `UNION`, `OPTIONAL`, `MINUS`, `EXISTS`, paths, grouping, projection, and distinctness will expose its meaningful alternatives in one memo. The user can verify this with `EXPLAIN` output that shows which rule fired or was rejected, which proof authorized it, which estimate and feedback correction ranked it, and which runtime operator implements it.

The practical outcome is measurable: fewer `fallback_no_winner` plans, lower planning overhead at the same or better plan quality, lower q-error by operator class, faster theme benchmark execution, bounded statistics memory/disk usage, and regression protection through plan snapshots plus metamorphic rewrite tests.

## Progress

- [x] (2026-05-25T22:08Z) Captured the user North Star as this living implementation plan.
- [x] (2026-05-25T22:08Z) Ran root quick install before planning; build passed.
- [x] (2026-05-25T22:08Z) Searched local paper index for Cascades, LEO, SPARQL `OPTIONAL`/`UNION`, join sketches, and runtime filtering.
- [x] (2026-05-25T22:08Z) Audited current Cascades and LMDB optimizer surfaces enough to name concrete files and gaps.
- [ ] Milestone 1: Stabilize Cascades/memo/property model.
- [ ] Milestone 2: Move missing rewrites into memo alternatives with proofs.
- [ ] Milestone 3: Introduce boundness-aware plan summaries under the existing sketch budget.
- [ ] Milestone 4: Wire LEO/operator feedback into candidate costing before winner selection.
- [ ] Milestone 5: Add physical semijoin, anti-semijoin, star, and dynamic-filter operators.
- [ ] Milestone 6: Retire or absorb legacy optimizer behavior.
- [ ] Milestone 7: Continuous paper-guided benchmark hardening.

## Surprises & Discoveries

- Observation: The repository already contains a local paper workflow and a reading order aligned with this North Star.
  Evidence: `papers/README.md` says the implementation principle is to keep proven facts, estimated facts, and observed facts separate. `papers/implementation_reading_order.md` orders Cascades, Pareto, sketches, feedback, semantic rewrites, execution-aware costing, WCOJ, learned components, and correctness testing.

- Observation: The current Cascades layer already has useful primitives but still lacks the full contract.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/PhysicalProperties.java` models ordering, distinct vars, access path, bound vars, materialization, graph context, and duplicate behavior. `LogicalProperties.java` models binding names, assured binding names, nullable vars, estimated rows, and q-error. The model does not yet carry boundness-mask distributions or plan summaries.

- Observation: Some rewrite work has already moved toward proofs, but several rules still exist as LMDB eager optimizers.
  Evidence: `StandardCascadesRules.java` contains proof-carrying join/filter/union/projection rules. LMDB still has `LmdbSetSemanticsOptimizer`, `LmdbOptionalNormalFormOptimizer`, `LmdbUnionFilterDistributor`, `LmdbSemanticDependencyOptimizer`, `LmdbFilterSimplifierOptimizer`, `LmdbProjectionPushdownOptimizer`, and `LmdbValueLookupOptimizer`.

- Observation: LEO-style feedback is partly available but not yet the single first-class costing path for every memo candidate.
  Evidence: `LmdbEvaluationStatistics.feedbackCorrection(...)` and `LmdbOperatorFeedbackStats` exist; tests such as `LmdbOperatorFeedbackPlanningTest` assert that selected plans show `plannedEstimateFusion=operator_feedback`.

- Observation: The paper index search exposes exact research anchors for the next implementation tasks.
  Evidence: local search returned Graefe 1995 Cascades chunks on required physical properties, Ding/Narasayya/Chaudhuri 2024 on extensible optimizers and enforcers, Stillger et al. 2001 LEO chunks on feedback, Pang/Zou/Ozsu/Chen on SPARQL `OPTIONAL` and `UNION`, Rusu/Dobra on Fast-AGMS join-size estimation, Wang et al. 2023 JoinSketch on unbiased inner-product estimation, and Zhu et al. 2017 on lookahead information passing.

## Decision Log

- Decision: Treat this plan as a program roadmap, not one giant patch.
  Rationale: The North Star spans algebra semantics, memo search, statistics, feedback, execution operators, benchmarks, and retirement of legacy behavior. Each milestone must be independently testable and reversible.
  Date/Author: 2026-05-25 / Codex

- Decision: Start with Cascades/memo/property stabilization before adding more rewrite rules.
  Rationale: The user explicitly wants Cascades to be the optimizer contract. Adding more eager rewrites would deepen the migration debt and make correctness harder to prove.
  Date/Author: 2026-05-25 / Codex

- Decision: Keep one selected sketch backend per estimator instance.
  Rationale: This matches the existing pluggable `SketchBasedJoinEstimator.SketchStrategy` direction and prevents unbounded sketch proliferation.
  Date/Author: 2026-05-25 / Codex

- Decision: Use estimates only for ranking, never for rewrite validity.
  Rationale: The paper-pack principle and the user North Star both separate proven facts from estimated facts. Rewrite correctness must come from SPARQL algebra/proofs.
  Date/Author: 2026-05-25 / Codex

## Outcomes & Retrospective

No implementation milestone is complete yet. This document turns the North Star into a repo-local, executable roadmap. The first code milestone is to make Cascades produce fewer fallback plans with explicit property/proof rejection reasons, while preserving current query results.

## Context and Orientation

The relevant generic Cascades code lives in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/`.

Important classes:

- `Memo`, `MemoGroup`, and `MemoExpr` hold equivalent expressions.
- `CascadesPlanner` explores groups, applies rules, optimizes physical expressions, and records winners.
- `StandardCascadesRules` contains generic proof-carrying transformations and implementations.
- `RuleProof`, `PlanProvenance`, `RejectedAlternative`, and `CascadesTelemetry` explain decisions.
- `LogicalProperties` and `PhysicalProperties` are the current property model.
- `EstimateVector`, `CostVector`, `QErrorInterval`, `StatisticsEstimate`, and `FeedbackCorrection` carry estimate/cost information.
- `RdfStatisticsProvider` is the generic statistics SPI used by LMDB.

The LMDB adapter code lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`.

Important classes:

- `LmdbCascadesOptimizer` integrates generic Cascades into the LMDB optimizer pipeline.
- `LmdbCascadesRuleProvider` registers LMDB-specific rules and physical alternatives.
- `LmdbEvaluationStatistics` supplies LMDB cardinalities, feedback, characteristic-set estimates, property-path estimates, and sketch estimates.
- `LmdbSketchJoinOptimizer` is the legacy/sketch join planner path that must be absorbed or retired.
- `LmdbOperatorFeedbackStats`, `LmdbFilterSelectivityStats`, and `LmdbTupleExprEstimateAnnotator` provide observed feedback and telemetry.
- `LmdbStarJoinScanSupport`, `LmdbStatementIterator`, `RecordIterator`, and `TripleStore` are relevant for physical runtime operators.

Sketch/statistics code lives in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/`.

Important classes:

- `SketchBasedJoinEstimator` now supports the selected backend strategy `FAST_AGMS`, `TUPLE`, or `JOIN_SKETCH`.
- `FrequencySummaryOps`, `FastAgmsBindingSummary`, `FastAgmsFrequencySketch`, `JoinFrequencySketch`, and `TupleSketchOps` are the current duplicate-aware sketch components.
- `SketchJoinOrderPlanner`, `ParetoJoinMemoPlanner`, `JoinCostVector`, and `JoinPlanningState` are legacy/current join-order infrastructure that must be bridged into Cascades or retired.

Definitions used in this plan:

A memo is a set of groups, where each group contains algebra expressions that are known to be equivalent under a stated semantic scope. A physical property is a runtime obligation such as ordering, streaming/materialized behavior, access path, or duplicate behavior. A logical property is an algebra property such as visible variables, assured variables, nullable variables, and estimated rows. A proof is metadata explaining why a rewrite is valid. An estimate is a cost/cardinality guess with uncertainty and provenance; it never proves semantic validity. LEO means learning optimizer feedback: actual runtime observations stored under canonical keys and used to repair future estimates.

## Research Anchors

Every optimizer implementation task must start by searching `papers/.paper-index` and reading the relevant local sections. The minimum commands are:

    cd /Users/havardottestad/Documents/Programming/rdf4j-small-things/papers
    .venv/bin/python -m paper_index search "Cascades memo physical properties" --limit 8
    .venv/bin/python -m paper_index search "LEO learning optimizer feedback" --limit 8
    .venv/bin/python -m paper_index search "SPARQL OPTIONAL UNION efficient execution" --limit 8
    .venv/bin/python -m paper_index search "Rusu Dobra sketches join estimation" --limit 8
    .venv/bin/python -m paper_index search "JoinSketch unbiased join size estimation" --limit 8
    .venv/bin/python -m paper_index search "sideways information passing runtime filters lookahead" --limit 8

Read these local sources before changing code:

- `papers/implementation_reading_order.md`, phases 1, 3, 4, 5, 7, and 10.
- `papers/01_core_architecture/01-graefe-1995-cascades-framework.pdf`, especially the chunks returned for required and excluded physical properties.
- `papers/01_core_architecture/05-ding-narasayya-chaudhuri-2024-extensible-query-optimizers-in-practice.pdf`, especially enforcer/required-property discussion.
- `papers/papers/LEO – DB2’s LEarning Optimizer.pdf` or `papers/04_feedback_robust_adaptive/11-stillger-et-al-2001-leo-db2-learning-optimizer.pdf`.
- `papers/papers/efficient_execution_sparql_optional_union_expressions_pang_2025.pdf` and the duplicate copy under `graph_triple_store_optimizer_papers_supplement/01_sparql_semantics_and_rewrites/`.
- `papers/03_cardinality_selectivity_sketches/07-rusu-dobra-2008-sketches-for-size-of-join-estimation.pdf`.
- `papers/03_cardinality_selectivity_sketches/wang-et-al-2023-joinsketch-accurate-unbiased-join-size-estimation.pdf`.
- `papers/08_dynamic_filtering_runtime_pruning/zhu-et-al-2017-looking-ahead-makes-query-plans-robust.pdf`.
- `papers/11_testing_correctness/` papers before adding metamorphic rewrite tests.

Record the paper section used in the new test name, Java comment, or proof reason when it directly justifies a rule.

## Plan of Work

Milestone 1 stabilizes the Cascades contract before new capabilities. Add tests that currently expose `fallback_no_winner` and property/proof gaps, then refine memo winner selection, logical properties, physical properties, rejection telemetry, and LMDB adapter behavior until the fallback count drops without changing query results.

Milestone 2 migrates missing logical rewrites into memo alternatives. Each rule gets a metamorphic test proving bag-semantics equivalence and a `RuleProof` with explicit facts. Eager LMDB rewrites remain only when they are canonical, cheap, and semantics-preserving independent of cost.

Milestone 3 replaces scalar-only plan estimates with boundness-aware summaries. The optimizer carries distributions over bound variable masks and relevant keys under the current sketch budget. The selected sketch backend stays pluggable; extra summaries are workload-promoted only when q-error improvement justifies storage.

Milestone 4 makes feedback a first-class cost input. Feedback corrections are keyed by canonical operator surfaces and applied to candidate costs before winner selection. Feedback tracks confidence, age, q-error history, dataset version, and sketch payload version.

Milestone 5 makes physical execution match planning. Add runtime semijoin and anti-semijoin probes, star/multi-predicate scans, dynamic filters/SIP, batched lookups, ID-only joins, late value materialization, and memory-aware materialization choices. Planner credit is granted only for implemented operators.

Milestone 6 retires or absorbs legacy behavior. Any remaining legacy optimizer rule must either become a Cascades rule/physical alternative with proofs or be removed. Compatibility flags may remain temporarily only with tests showing the new default path is complete.

Milestone 7 keeps pressure on benchmarks and correctness. Add plan snapshots, metamorphic rewrite tests, q-error dashboards by operator class, and theme benchmark comparisons against legacy/current behavior.

## Concrete Steps

### Milestone 1: Stabilize Cascades/memo/property model

Start with tests. Add or extend `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizerTest.java` with a method named `defaultCascadesDoesNotFallbackForCoveredAlgebraShapes`. The test should build representative queries using `UNION`, `OPTIONAL`, `MINUS`, `FILTER EXISTS`, `FILTER NOT EXISTS`, projection, grouping, and property paths, run `new LmdbCascadesOptimizer(statistics, false).optimize(...)`, finalize with `LmdbCascadesExplainFinalizer`, and assert that planned metrics do not contain `plannedEstimateUsage=fallback_no_winner` for shapes expected to be covered by this milestone. Keep the first version small: one query per algebra family.

Add a second test in `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesMemoModelTest.java` named `memoWinnerRequiresMatchingLogicalAndPhysicalProperties`. It should construct two physical alternatives in the same memo group with different delivered `PhysicalProperties`, require one property, and assert the incompatible alternative is rejected with reason `missing-physical-property`.

Run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesMemoModelTest verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbCascadesOptimizerTest verify

Implement only enough Cascades/property changes for these tests. Expected touched files are `LogicalProperties.java`, `PhysicalProperties.java`, `CascadesPlanner.java`, `Memo.java`, `MemoExpr.java`, `PlanProvenance.java`, `RejectedAlternative.java`, `CascadesPlanProvenanceAnnotator.java`, `LmdbCascadesOptimizer.java`, and `LmdbCascadesRuleProvider.java`.

Acceptance: the tests pass, no query result changes, and `EXPLAIN` output for covered shapes shows the selected Cascades rule/proof or a concrete rejection reason instead of silent fallback.

### Milestone 2: Move missing rewrites into memo alternatives with proofs

For each rewrite, write a metamorphic test first. Use `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRewriteProofTest.java` for proof annotation checks and create `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRewriteMetamorphicTest.java` if no suitable class exists.

The first rewrite batch is:

- Filter over `UNION` when the filter variables are visible in both branches.
- Projection over `UNION` when bag semantics are preserved.
- Join over `UNION` when variable scope and duplicate behavior are safe.
- `OPTIONAL` negated-bound anti-join alternative when the RHS has a fresh assured binding.
- `EXISTS` and `NOT EXISTS` decorrelation to semijoin/anti-semijoin when the correlation surface is explicit.
- `MINUS` to anti-semijoin when the shared variable set is non-empty.

Implement rules in `StandardCascadesRules.java` for generic algebra cases and in `LmdbCascadesRuleProvider.java` for LMDB physical alternatives. Every rule application must attach a `RuleProof` whose facts include the semantic condition, for example `bagUnion`, `conditionVisibleInBothBranches`, `rhsFreshAssuredBinding=<var>`, `correlationSurface=<vars>`, or `minusSharedVars=<vars>`.

Run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesRuleEngineTest,CascadesMemoModelTest verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbRewriteProofTest,LmdbCascadesRewriteMetamorphicTest verify

Acceptance: optimized and unoptimized query results match for duplicate-sensitive tests, each new rule emits a proof, and estimates only affect whether the proven alternative wins.

### Milestone 3: Add boundness-aware plan summaries

Create plan-summary types in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/`:

- `PlanSummary`
- `MaskSummary`
- `KeySummary`
- `VarMask`
- `VarSet`
- `SummaryEvidence`

These classes should model visible variables, rows by boundness mask, key rows, distinct/effective distinct, uncertainty, confidence, and evidence source. Keep them immutable and small. Do not introduce a global composite-key cache.

Extend `StatisticsEstimate` or add a sibling wrapper so `RdfStatisticsProvider` can return both a scalar estimate and an optional `PlanSummary`. Add tests in `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/PlanSummaryTest.java` for:

- `UNION` adds rows by mask and does not turn absent variables into fake nulls.
- `OPTIONAL` preserves unmatched left rows under the left mask.
- `MINUS` keeps left-side masks and subtracts anti-semijoin probability.
- Projection intersects masks with projected variables and preserves bag rows.
- Inner join estimates by mask pairs using the selected sketch backend or effective-distinct fallback.

Bridge to `SketchBasedJoinEstimator` through the existing `SketchStrategy`. FastAGMS remains the default. Tuple and JoinSketch stay selectable for compatibility and accuracy comparisons.

Run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=PlanSummaryTest,SketchBasedJoinEstimatorConfigTest,SparseWholeBgpSketchAccuracyTest verify

Acceptance: plan summaries are available to Cascades costing, sparse whole-BGP duplicate-aware accuracy logs still run, and storage remains bounded by the chosen sketch backend plus promoted summaries.

### Milestone 4: Apply LEO before winner selection

Define canonical feedback keys in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/FeedbackCorrection.java` or a new `FeedbackKey` class. Keys must identify the operator class and semantic surface, not the raw query string. Examples: statement pattern predicate/object shape, join key variables, optional bridge variables, filter expression canonical form, property path predicate and direction, group/distinct key.

Modify `CascadesCostModel` and `LmdbEvaluationStatistics` so feedback correction is applied while computing candidate `EstimateVector`/`CostVector`, before `Memo.addWinner(...)` compares alternatives. Preserve telemetry showing both base estimate and applied feedback.

Add tests:

- In `LmdbOperatorFeedbackPlanningTest`, assert the second planning run changes the selected winner because operator feedback is used before winner selection.
- In `CascadesEstimateVectorTest`, assert low-confidence feedback is ignored and trusted feedback updates rows, work rows, q-error, uncertainty, confidence, and source.
- In a new `LmdbFeedbackInvalidationTest`, assert feedback is ignored after dataset/sketch version mismatch.

Run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CascadesEstimateVectorTest verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbOperatorFeedbackPlanningTest,LmdbFeedbackInvalidationTest verify

Acceptance: `EXPLAIN` shows `plannedEstimateFusion=operator_feedback` only when the feedback affected candidate ranking, not merely final annotation.

### Milestone 5: Add runtime physical operators

Add physical alternatives only when runtime support exists.

Implement semijoin/anti-semijoin probes in LMDB execution classes around `LmdbEvaluationStrategy`, `LmdbStatementIterator`, `RecordIterator`, and `TripleStore`. They must operate on value IDs where possible and avoid materializing full RDF values until needed. Add telemetry for probe count, hit count, miss count, and skipped RHS rows.

Implement star/multi-predicate scan execution using `LmdbStarJoinScanSupport` and LMDB indexes. The operator should bind a shared subject once and probe multiple predicates without building a binary join tree when the chosen physical alternative demands a star scan.

Implement dynamic filters/SIP as a planner-visible physical property. The producing side should create a compact value-ID filter; the consuming side should apply it during LMDB cursor scans or batched lookups.

Tests go in:

- `LmdbSemijoinOperatorTest`
- `LmdbAntiSemijoinOperatorTest`
- `LmdbStarJoinScanSupportTest`
- `LmdbDynamicFilterPlanningTest`

Run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbSemijoinOperatorTest,LmdbAntiSemijoinOperatorTest,LmdbStarJoinScanSupportTest,LmdbDynamicFilterPlanningTest verify

Acceptance: plan output names the physical operator, runtime telemetry shows the operator actually executed, and benchmark traces show fewer scanned rows or fewer materialized values for target workloads.

### Milestone 6: Retire or absorb legacy optimizer behavior

Inventory legacy LMDB optimizers in `LmdbQueryOptimizerPipeline.java`. For each legacy optimizer, decide one of:

- Keep as canonical pre-normalization because it is cheap and obviously semantics-preserving.
- Convert to a Cascades logical transformation with proof.
- Convert to a Cascades physical implementation/enforcer.
- Remove.

Add pipeline tests in `LmdbOptimizerPipelineTest` proving default planning is Cascades-first and that legacy flags are temporary compatibility paths. Add plan snapshot tests for representative theme queries proving no `fallback_no_winner` on supported shapes.

Run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbOptimizerPipelineTest,LmdbIndexAwareJoinOrderPlanningTest verify

Acceptance: the default pipeline no longer depends on legacy join-order planning for supported algebra; any remaining legacy optimizer has a documented reason and test.

### Milestone 7: Continuous benchmark and correctness hardening

Add or extend benchmark/support tests so every change can be compared against current and legacy behavior.

Use:

    ./scripts/run-single-benchmark.sh --module core/sail/lmdb --benchmark ThemeQueryBenchmark.executeQuery --jmh-args '<target parameters>'

When a test or benchmark is slow, print the optimized query plan before execution. If the plan is wrong, stop the run and inspect the plan instead of waiting for query execution. If execution is unexpectedly slow despite a good plan, attach async-profiler with the approved profiler scripts and inspect obvious overhead first.

Add correctness tests:

- Metamorphic rewrite tests for every new rule.
- Plan snapshots for benchmark themes.
- q-error reports by operator class.
- Memory/disk budget checks for sketch and feedback stores.
- `EXPLAIN WHY` tests for rule fired, rule rejected, estimate used, feedback applied, and plan pruned.

Run the focused tests first, then:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py --module core/sail/lmdb LmdbOptimizerPipelineTest --retain-logs --stream

For LMDB integration tests that would trigger long Failsafe suites unintentionally, use manual Maven with `-DskipITs` for unit-only checks.

Acceptance: benchmark reports show lower or equal planning overhead at equal or better execution time, fewer fallbacks, bounded statistics storage, and no semantic regressions.

## Validation and Acceptance

The full plan is accepted only when these observable checks hold:

- `EXPLAIN` for representative theme queries shows `plannerId=lmdb-cascades` and no `fallback_no_winner` for supported algebra.
- Every rewrite alternative has a proof and a duplicate-sensitive metamorphic test.
- Sparse whole-BGP sketch accuracy reports still show duplicate-aware estimates for FastAGMS, Tuple, and JoinSketch.
- Operator-feedback tests show feedback changes candidate costing before winner selection.
- Semijoin, anti-semijoin, star scan, and dynamic-filter plans have runtime telemetry proving the selected operator executed.
- Theme benchmark comparisons show faster or equal execution time versus the legacy path for targeted queries, without worse planning overhead.
- Statistics memory/disk checks show bounded sketch and feedback storage.

Minimum verification at each milestone:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources | tail -200

Do not use `-am` for test runs. Do not use `-q`.

## Idempotence and Recovery

All milestones are additive until tests prove parity. Do not remove legacy behavior in the same patch that introduces a new replacement unless the replacement has focused unit tests, plan snapshots, and a benchmark comparison. Do not overwrite tracked files from `big-refactor/` or `big-refactor-v2/`; when a reference file is needed, stage a copy under `/tmp/` and cherry-pick the idea manually.

If a benchmark or test hangs, first print the optimized plan. If the plan is visibly wrong, kill the run and fix planning. If the plan is plausible but execution is slow, use async-profiler or JFR to identify runtime overhead. Record the finding in `Surprises & Discoveries`.

If offline Maven resolution fails, rerun the exact command once without `-o`, then return to offline commands.

## Artifacts and Notes

Initial planning evidence:

    Command: mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    Result: BUILD SUCCESS
    Key lines:
      RDF4J: Query algebra - evaluation .................. SUCCESS [  2.260 s]
      RDF4J: LmdbStore ................................... SUCCESS [  4.055 s]
      BUILD SUCCESS

Paper-index searches performed:

    .venv/bin/python -m paper_index search "Cascades memo physical properties" --limit 8
    .venv/bin/python -m paper_index search "LEO learning optimizer feedback" --limit 8
    .venv/bin/python -m paper_index search "SPARQL OPTIONAL UNION efficient execution" --limit 8
    .venv/bin/python -m paper_index search "Rusu Dobra sketches join estimation" --limit 8
    .venv/bin/python -m paper_index search "JoinSketch unbiased join size estimation" --limit 8
    .venv/bin/python -m paper_index search "sideways information passing runtime filters lookahead" --limit 8

One paper-index query with a hyphenated phrase failed with `paper-index: error: no such column: aware`; split hyphenated searches into simpler terms instead of changing the paper-index tool.

## Interfaces and Dependencies

New or stabilized interfaces expected by this plan:

- `PhysicalProperties` must represent runtime obligations that the executor can actually satisfy.
- `LogicalProperties` must grow toward mask-aware logical summaries, not remain only scalar rows plus visible variables.
- `PlanSummary`, `MaskSummary`, and `KeySummary` must be immutable, bounded, and estimator-strategy independent.
- `RdfStatisticsProvider` must expose operator-class estimates for joins, filters, unions, left joins, minus, exists, paths, stars, group, distinct, and projection.
- `FeedbackCorrection` or a new `FeedbackKey` must carry canonical feedback keys and versioning fields.
- LMDB physical operators must expose telemetry that proves runtime behavior.

No new external runtime dependency is assumed by this plan.

## Change Notes

2026-05-25 / Codex: Created this plan from the user North Star. The plan intentionally starts with the Cascades contract and only then moves rewrites, summaries, LEO, physical operators, legacy retirement, and benchmarking into separate verifiable milestones.
