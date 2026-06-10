# Cascades SPARQL Rewrite Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the currently identified Cascades rewrite correctness bugs and complete the useful SPARQL algebra rewrites that belong in the Cascades rule engine, while explicitly documenting report items that are parser-level, pipeline-level, or intentionally out of Cascades scope.

**Architecture:** Treat Cascades as a rule engine over RDF4J tuple algebra alternatives. Every logical rewrite added to the Cascades rules package must be bag-semantics equivalent, scope-safe, and covered by a focused failing test before production edits. Existing pipeline optimizers remain the home for parser/query-form desugaring and expression simplification that already happens before Cascades; the plan adds only rewrites that still matter inside Cascades search or LMDB physical planning.

**Tech Stack:** Java 25, Maven offline with workspace-local `.m2_repo`, RDF4J query algebra, Cascades optimizer package, LMDB Sail optimizer pipeline, JUnit 5, Maven Surefire/Failsafe, existing `.codex/skills/mvnf/scripts/mvnf.py` runner.

---

This ExecPlan is a living document. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current as implementation proceeds. Maintain this document according to `.agent/PLANS.md`.

## Purpose / Big Picture

The LMDB Cascades optimizer currently contains several important SPARQL algebra rewrites, but static inspection found both correctness bugs and missing rewrite families compared with the four research reports in `/Users/havardottestad/Downloads/deep-research-report1.md` through `/Users/havardottestad/Downloads/deep-research-report4.md`. After this work, Cascades should no longer produce non-equivalent logical alternatives for projection or `OPTIONAL + !BOUND`, large greedy connected joins should not be corrupted by 32-bit masks, and the rule registry should contain the safe logical rewrites that improve join search, finite anchors, union handling, optional handling, projection pruning, and structural simplification.

The observable outcome is not a user-facing API. It is demonstrated by focused optimizer unit tests that fail before each change and pass after, plus LMDB module verification showing the default optimizer pipeline still builds equivalent plans without parent-reference breakage.

## Progress

- [x] (2026-05-31) Static review identified three existing bugs: unsafe projection pushdown, unsafe no-shared-var optional anti-join, and greedy planner 32-bit mask overflow.
- [x] (2026-05-31) Static review mapped existing Cascades rule registry and LMDB pre-Cascades pipeline.
- [x] (2026-05-31) Static review classified missing report rewrite families into Cascades candidates, already-covered pipeline rewrites, and non-Cascades items.
- [x] (2026-06-02) Create initial evidence file before any implementation test loop.
- [x] (2026-06-02) Fix existing Cascades correctness bugs with regression tests.
- [x] (2026-06-02) Add shared rule support utilities for safe finite-value anchors and projection pruning.
- [x] (2026-06-02) Add structural simplification rules.
- [x] (2026-06-02) Add finite anchor and equality rewrites as Cascades rules.
- [x] (2026-06-02) Add projection/filter placement rules for identity projections, filters, MINUS, OPTIONAL, and
  Extension.
- [x] (2026-06-02) Add guarded optional/union distribution rules.
- [ ] Add remaining minus distribution coverage and order/distinct disposition notes.
- [ ] Add order/distinct dispositions and coverage doc.
- [ ] Register rules and add pipeline/registry coverage tests.
- [ ] Migrate hot Cascades planner decisions to query-local binding masks. Child input-goal derivation and
  physical-property compatibility now use `BindingMask` through the memo `BindingUniverse`, and
  `DefaultCascadesCostModel` uses `BindingMask` for estimate, provider, decision-refinement, and physical-input cache
  keys. Projection, set-operation, and filter-placement rule guards now use memo-local masks for their binding
  membership/intersection checks. Remaining work is to move the remaining rule guards, logical-property checks, and
  non-boundary cost-model internals off `Set<String>`.
- [x] (2026-06-06) Preserve supporting sketch evidence in `BindingProfile` satisfaction checks so parent-visible
  combined sketch evidence cannot be hidden by a cheaper empty/current-only profile.
- [ ] Run focused, module, format, and final verification.

## Surprises & Discoveries

- Observation: Current representative `ThemeQueryPlanRunBenchmark.runQuery` results after the connected-planner
  access-path tie breaker are competitive with the May `ThemeQueryBenchmark.executeQuery` comparison set. SPARSE q4
  and q7 remain slower than May but within the accepted 10-20% band; no missing rewrite was obvious in the printed
  plan telemetry because connected hypergraph and optional RHS anchored lookup rules were active.
  Evidence: `/tmp/rdf4j-lmdb-planrun-SPARSE-q5-q7-after-tie.txt`,
  `/tmp/rdf4j-lmdb-planrun-SOCIAL_MEDIA-q5-after-tie.txt`,
  `/tmp/rdf4j-lmdb-planrun-MEDICAL_RECORDS-q7-after-tie.txt`,
  `/tmp/rdf4j-lmdb-planrun-PHARMA-q5-after-tie.txt`,
  `/tmp/rdf4j-lmdb-planrun-TRAIN-q2-after-tie.txt`, and
  `/tmp/rdf4j-lmdb-planrun-LIBRARY-q7-after-tie.txt`.
- Observation: Milestone 1 correctness fixes are present in the current worktree. Projection pushdown retains the
  outer projection and shared join variables, optional `!BOUND` anti-join requires shared variables, and greedy
  connected planning uses set membership for prefix indices instead of overflowing `1 << factorIndex` beyond 31.
  Evidence: `CascadesRuleEngineTest` passed with 40 tests on 2026-06-02, and
  `LmdbCascadesConnectedRuleAdmissibilityTest` passed with 10 tests after the connected-planner changes.
- Observation: Shared support extraction is present in the current worktree. `FilterValuesAnchorSupport` now owns
  the safe finite-values anchor recognition used by `FilterInValuesOptimizer`, and `CascadesRewriteSupport` owns the
  reusable planner-name, projection, conjunct, and scoped branch-local helpers used through compatibility wrappers in
  `StandardCascadesRules`.
  Evidence: `FilterInValuesRewriteRegressionTest` passed with 24 tests on 2026-06-02, and `CascadesRuleEngineTest`
  passed with 40 tests after wiring the helper wrappers.
- Observation: Structural simplification rules are present in the current worktree. Cascades now has focused rules for
  nested filter merge, RDF boolean constant filters, join empty/singleton identities, non-scoped UNION empty/flatten
  simplification, and identity projection merge, registered in both the generic registry and LMDB rule provider.
  Evidence: `CascadesRuleEngineTest#nestedFilterMergeCombinesConditionsWithoutCrossingScope` failed red before the
  production rule file existed, then `CascadesRuleEngineTest` passed with 45 tests and
  `LmdbCascadesConnectedRuleAdmissibilityTest` passed with 10 tests after implementation.
- Observation: The structural rules did not regress the original poor-plan benchmark case. `SOCIAL_MEDIA q5`
  `ThemeQueryPlanRunBenchmark.runQuery` measured 2.595 ms/op after implementation, compared with the prior post-tie
  run at 3.311 ms/op and the May `ThemeQueryBenchmark.executeQuery` comparison point at 262.443 ms/op.
  Evidence: `./scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:5` on 2026-06-02.
- Observation: Finite-anchor Cascades rules are present in the current worktree. `FilterCascadesRules` converts safe
  RDF-term equality/IN filters into finite `BindingSetAssignment` join anchors, keeps unsafe numeric equality as a
  filter, preserves residual conjuncts, requires assured argument bindings before dropping filters, and avoids
  duplicating existing finite anchors.
  Evidence: `CascadesRuleEngineTest#filterValuesAnchorTurnsSafeInIntoValuesJoin` failed red before the production rule
  file existed, then `CascadesRuleEngineTest` passed with 50 tests and
  `LmdbCascadesConnectedRuleAdmissibilityTest` passed with 10 tests after implementation.
- Observation: The PHARMA q5 guard failure after finite-anchor rules was a guard false positive, not a runtime
  regression. The plan had one connected component and low chosen `plannedCostWorkRows`, while a diagnostic
  `plannedCostCartesianWorkRows` on the outer filter exceeded the guard threshold. The guard now ignores that connected
  low-work diagnostic shape but still fails truly high chosen Cartesian work.
  Evidence: `python3 scripts/query-plan-risk-guard.py --self-test` passed after adding the regression sample; guarded
  `./scripts/run-single-benchmark.sh --theme-plan-run --theme-query PHARMA:5 --no-build` then completed at 11.309
  ms/op.
- Observation: Representative finite-anchor benchmark loop remains competitive with the May comparison set:
  `SOCIAL_MEDIA q5` 2.817 ms/op vs May 262.443, `LIBRARY q7` 178.834 vs May 1380.326, `PHARMA q5` 11.309 vs May
  16.929, `MEDICAL_RECORDS q7` 12.043 vs May 56.793, and `TRAIN q2` 0.414 vs May 21.068.
  Evidence: `ThemeQueryPlanRunBenchmark.runQuery` runs on 2026-06-02 using the rebuilt LMDB benchmark jar.
- Observation: Projection/filter placement Cascades rules are present in the current worktree. Cascades now has
  focused rules for projection pruning through filters while retaining condition variables, projection pruning through
  MINUS while retaining shared compatibility variables, projection pruning through OPTIONAL while retaining shared and
  optional-condition variables, filter pushdown through non-subquery identity projections, filter pushdown into the
  left side of OPTIONAL only when the filter uses left-assured variables and the optional condition does not depend on
  nullable-right variables, and filter pushdown below Extension only when the filter is independent of Extension
  outputs.
  Evidence: `CascadesRuleEngineTest#projectionFilterPushdownKeepsConditionVars` failed red before
  `ProjectionCascadesRules` and the new `FilterCascadesRules` placement rules existed, then `CascadesRuleEngineTest`
  passed with 56 tests and `LmdbCascadesConnectedRuleAdmissibilityTest` passed with 10 tests after implementation and
  formatting.
- Observation: Representative projection/filter placement benchmark loop remains competitive with the May comparison
  set: `SOCIAL_MEDIA q5` 2.971 ms/op vs May 262.443, `LIBRARY q7` 184.780 vs May 1380.326, `PHARMA q5` 11.327 vs
  May 16.929, `MEDICAL_RECORDS q7` 25.490 vs May 56.793, and `TRAIN q2` 0.436 vs May 21.068. These are all faster
  than the May `ThemeQueryBenchmark.executeQuery` comparison points; no timing in this loop shows an obvious
  estimation or rewrite-selection issue under the accepted 10-20% threshold.
  Evidence: `./scripts/run-single-benchmark.sh --theme-plan-run --theme-query <THEME:INDEX>` on 2026-06-02, first
  run rebuilding the LMDB benchmark jar and later runs using `--no-build`.
- Observation: `ThemeQueryPlanRunBenchmark.runQuery` does generate optimized-plan output before the benchmark method
  executes, but the console capture can show only the heading because the rendered plan is tens to hundreds of KB and
  JMH output interleaves/truncates stdout. The sidecar optimized-plan files are populated:
  `SOCIAL_MEDIA-q5-runQuery-optimized.txt` 83 KB, `LIBRARY-q7-runQuery-optimized.txt` 29 KB,
  `PHARMA-q5-runQuery-optimized.txt` 33 KB, `MEDICAL_RECORDS-q7-runQuery-optimized.txt` 163 KB, and
  `TRAIN-q2-runQuery-optimized.txt` 26 KB.
  Evidence: files under `core/sail/lmdb/target/lmdb-theme-query-benchmark/plans/`.
- Observation: Guarded optional/union Cascades rules are present in the current worktree. `SetCascadesRules` adds
  left-optional-over-union distribution only for equal binding sets and safe branch shapes, and a narrow right-optional
  union distribution only when finite discriminator rows exactly cover mutually exclusive branch filters. Unsupported
  branch shapes, nested scope changes, and unsafe conditions are rejected.
  Evidence: `CascadesRuleEngineTest#optionalLeftUnionDistributionPreservesLeftBranchMultiplicity`,
  `#optionalLeftUnionDistributionRejectsUnsupportedBranchShapes`,
  `#optionalRightUnionDistributionIsNotAppliedWithoutMutualExclusion`, and
  `#optionalRightUnionDistributionAppliesForFiniteDiscriminatorBranches` passed on 2026-06-02; full
  `CascadesRuleEngineTest` passed with 60 tests and `LmdbCascadesConnectedRuleAdmissibilityTest` passed with 10 tests.
- Observation: The fresh post-optional-union representative benchmark loop remains competitive with the May
  comparison set and does not show an obvious rewrite/estimation issue under the accepted 10-20% threshold:
  `SOCIAL_MEDIA q5` 3.229 ms/op vs May 262.443, `LIBRARY q7` 172.798 vs May 1380.326, `PHARMA q5` 10.396 vs May
  16.929, `MEDICAL_RECORDS q7` 12.646 vs May 56.793, and `TRAIN q2` 0.382 vs May 21.068. Sidecar diagnostics show
  `optimizer.cascadesWinner=cascades`; `optimizer.cascadesStandardPlanPolicy=fallback` is the retained non-Cascades
  comparison candidate, not emergency fallback selection.
  Evidence: JSON result files under `core/sail/lmdb/target/benchmark-results/theme-plan-run-*.json` and plan sidecars
  under `core/sail/lmdb/target/lmdb-theme-query-benchmark/plans/`.
- Observation: `LIBRARY q7` now plans the finite anchor before the branch-name lookup and estimates the downstream
  `locatedAt` fanout rather than treating it as a singleton direct lookup. The sidecar shows
  `optimizer.guaranteeOption=finite-anchor:branchName`, `plannedDistinctLookupBindings=2.00`, and
  `plannedLookupDomainAverageOutputRows=154.4K` for the derived `locatedAt` access.
  Evidence: `core/sail/lmdb/target/lmdb-theme-query-benchmark/plans/LIBRARY-q7-runQuery-optimized.txt`.
- Observation: The Cascades package already contains the intended query-local symbol vocabulary:
  `BindingSymbol`, `BindingUniverse`, `BindingMask`, and `BindingShape`. The remaining migration is not to invent a
  new abstraction, but to stop converting back to `Set<String>` in hot planner decisions, rule guards, logical
  property checks, and physical-property satisfaction. Names should remain only at RDF4J tuple-algebra and telemetry
  boundaries.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/BindingUniverse.java`,
  `BindingMask.java`, `BindingShape.java`, `LogicalProperties.java`, and `PhysicalProperties.java`.
- Observation: The first mask-core migration slice is now in the planner. Child input goals derive required bound
  variables against memo `BindingShape.possible()` masks, and physical-property satisfaction / missing-property
  rejection checks use query-local `BindingMask` when the planner has a memo universe. This keeps the external
  `Set<String>` API intact while removing two hot internal string-set compatibility checks.
  Evidence: `CascadesRuleEngineTest` passed with 74 tests, `BindingShapeTest` passed with 4 tests,
  `CascadesMemoModelTest` passed with 24 tests, and
  `LmdbCascadesContextPropagationTest#scopeChangingUnionHidesOnlyBranchLocalBindOutputs` passed on 2026-06-06.
  `ThemeQueryPlanRunBenchmark.runQuery` for `MEDICAL_RECORDS q7` measured 47.760 ms/op after the property-mask slice,
  versus the May comparable `ThemeQueryBenchmark.executeQuery` value of 56.793 ms/op.
- Observation: The second mask-core migration slice moved `DefaultCascadesCostModel` cache keys from `Set<String>` to
  `BindingMask` for logical estimates, provider estimates, decision refinements, and physical input combinations. The
  cost model still converts masks back to names at estimator/provider boundaries, keeping public and LMDB estimator
  APIs unchanged.
  Evidence: `CascadesCostModelTest` passed with 39 tests, `CascadesRuleEngineTest` passed with 74 tests,
  `BindingShapeTest` passed with 4 tests, and
  `LmdbCascadesContextPropagationTest#scopeChangingUnionHidesOnlyBranchLocalBindOutputs` passed on 2026-06-06.
  `ThemeQueryPlanRunBenchmark.runQuery` for `MEDICAL_RECORDS q7` measured 47.320 ms/op after the cost-key mask slice,
  versus the May comparable `ThemeQueryBenchmark.executeQuery` value of 56.793 ms/op.
- Observation: The third mask-core migration slice moved projection, set-operation, and filter-placement rule guard
  arithmetic from name-set operations to memo-local `BindingMask` operations. Public binding names remain at rewrite
  construction and telemetry boundaries, but hot guard checks now use ordinal masks for contains/intersects/minus.
  Evidence: `CascadesRuleEngineTest` passed with 74 tests after the projection guard slice, again after the set-rule
  guard slice, and again after the filter-rule guard slice on 2026-06-06. `BindingShapeTest` passed with 4 tests, and
  `LmdbCascadesContextPropagationTest#scopeChangingUnionHidesOnlyBranchLocalBindOutputs` passed. The representative
  `ThemeQueryPlanRunBenchmark.runQuery` run for `MEDICAL_RECORDS q7` measured 45.709 ms/op after the filter-rule
  mask slice, versus the May comparable `ThemeQueryBenchmark.executeQuery` value of 56.793 ms/op.
- Observation: `BindingProfile.satisfies` saw embedded current sketch keys but ignored embedded supporting sketch
  keys. That could let property satisfaction treat a profile without parent-visible support evidence as satisfying one
  that has supporting evidence, allowing later dominance/rejection to hide useful sketch information from parent join
  costing. The guard now requires both current and supporting evidence-profile sketch key sets.
  Evidence:
  `CascadesMemoModelTest#bindingProfileSatisfiesSeesSupportingProfileSketchKeys` failed red with
  `expected: <false> but was: <true>`, then `CascadesMemoModelTest` passed with 25 tests and `BagEstimateMathTest`
  passed with 36 tests after the fix on 2026-06-06.
- Observation: `AASQueriesBenchmark.runQuery` now has covered run-only measurement via `PreparedQueryState`; plan
  preparation and optimized-plan printing happen in iteration setup, while the benchmark method only evaluates the
  prepared `LmdbBenchmarkQueryPlan`. No stored AAS May history was present, so the local comparison used the same
  run-only method with `useCascades=true` and `useCascades=false`.
  Evidence: `AASQueriesBenchmarkTest` passed with 6 tests. Run-only samples:
  `query1PropertyProjection` 0.020 ms/op vs 0.018 non-Cascades, `query2ThresholdCount` 110.968 vs 104.015, and
  `query3LineAggregates` 177.578 vs 172.156.
- Observation: The latest representative theme run-only loop remains competitive with the May comparison set:
  `LIBRARY q7` 181.828 ms/op vs May 1380.326, `SOCIAL_MEDIA q5` 1.115 vs May 262.443, `MEDICAL_RECORDS q7` 44.863
  vs May 56.793, `PHARMA q5` 1.245 vs May 16.929, and `TRAIN q2` 0.282 vs May 21.068. Sidecar diagnostics had no
  selected `plannedEstimateSource=sampled` and no emergency fallback markers. `SOCIAL_MEDIA q5` selected the standard
  comparison winner, but that result was far faster than May and not a current regression target under the accepted
  10-20% threshold.
  Evidence: JSON result files under `core/sail/lmdb/target/benchmark-results/aas-runQuery-*.json` and
  `core/sail/lmdb/target/benchmark-results/theme-plan-run-*.json`; theme plan sidecars under
  `core/sail/lmdb/target/lmdb-theme-query-benchmark/plans/`.
- Observation: Alias projection was still dropping tuple-level finite and sketch evidence. `EstimateMath.extendAlias`
  copied scalar variable estimates but left finite relations and tuple sketches keyed by the source binding name, so a
  later projection from `?code AS ?alias` discarded `{?enc, ?code}` evidence instead of carrying it as
  `{?enc, ?alias}`. `EvidenceProfile.alias` now duplicates finite relations plus current/supporting sketch evidence
  under the alias key while keeping source-keyed evidence for Extension semantics.
  Evidence: `BagEstimateMathTest#aliasProjectionRenamesTupleEvidence` failed red with
  `Projection alias should rename exact tuple evidence`, then `BagEstimateMathTest` passed with 37 tests,
  `CascadesMemoModelTest` passed with 25 tests, and `CascadesCostModelTest` passed with 39 tests on 2026-06-06.
- Observation: After the alias-evidence fix, the representative theme run-only loop remains competitive with the May
  comparison set: `MEDICAL_RECORDS q7` 54.907 ms/op vs May 56.793, `LIBRARY q7` 244.770 vs May 1380.326,
  `SOCIAL_MEDIA q5` 1.484 vs May 262.443, `PHARMA q5` 1.341 vs May 16.929, and `TRAIN q2` 0.327 vs May 21.068.
  Sidecar diagnostics again had no selected `plannedEstimateSource=sampled` and no emergency fallback markers.
  `SOCIAL_MEDIA q5` still selected the standard comparison winner, but the measured runtime is far below May and not a
  current poor-plan target under the accepted 10-20% threshold.
  Evidence: JSON result files
  `core/sail/lmdb/target/benchmark-results/theme-plan-run-*-after-alias-evidence.json` and sidecar diagnostics under
  `core/sail/lmdb/target/lmdb-theme-query-benchmark/plans/`.
- Observation: Multi-hop evidence propagation still had a blind spot after alias preservation. A prefix join could
  retain a tuple sketch only as supporting evidence, while a later fresh factor exposed the same tuple sketch as
  current evidence. `EvidenceProfile.innerJoinScalar` only compared current/current and supporting/supporting sketch
  pairs, so the later join fell back to generic tuple distinct math instead of the available sketch inner product. It
  now accepts current/supporting sketch pairs in both directions before falling back.
  Evidence: `BagEstimateMathTest#innerJoinUsesMixedCurrentAndSupportingSketchEvidence` failed red with
  `expected: <13.0> but was: <40.0>`, then `BagEstimateMathTest` passed with 38 tests,
  `CascadesCostModelTest` passed with 39 tests, and `CascadesMemoModelTest` passed with 25 tests on 2026-06-06.
- Observation: The scalar mixed-sketch fix still did not deliver a composed current tuple sketch to parent costing.
  `joinedSketches` only built a current `ProductDistributionSketch` from current/current inputs, so a join costed from
  supporting/current evidence retained the tuple sketch only as supporting carry-over. `joinedSketches` now builds the
  current composed sketch from current evidence first and non-stale supporting evidence as fallback.
  Evidence: `BagEstimateMathTest#innerJoinPromotesMixedSketchJoinEvidenceToCurrentRelationSketch` failed red with
  `expected: <true> but was: <false>`, then the method passed. Full `BagEstimateMathTest` passed with 39 tests,
  `CascadesCostModelTest` passed with 39 tests, and `CascadesMemoModelTest` passed with 25 tests on 2026-06-06.
- Observation: The planner-core identity direction is already present in this branch, not a `Set<Var>` migration.
  `BindingUniverse`, `BindingSymbol`, `BindingMask`, and `BindingShape` already provide query-local ordinal binding
  identity, including outputs from `Projection`, `Extension`, `Group`, and `BindingSetAssignment`. The remaining
  string-heavy work is staged migration of `PhysicalProperties`, `LogicalProperties`, rule helper APIs, and
  `EvidenceProfile` keys so names stay at RDF4J/telemetry boundaries.
  Evidence: inspection of `BindingUniverse.java`, `BindingShape.java`, `PhysicalProperties.java`,
  `LogicalProperties.java`, and `EvidenceProfile.java` on 2026-06-06.

- Observation: `StandardCascadesRules.ProjectionPushdownRule` currently rewrites `Projection(Join(left,right))` into `Join(Projection(left), right)` or `Join(left, Projection(right))` without retaining the outer projection. This can expose bindings from the unprojected side.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StandardCascadesRules.java`, method `ProjectionPushdownRule.apply`.
- Observation: `OptionalNegatedBoundAntiJoinRule` checks that the RHS assures the `!BOUND` variable, but it does not require any shared variable between left and right. SPARQL `MINUS` has special no-shared-variable behavior and is not equivalent in that case.
  Evidence: same file, method `OptionalNegatedBoundAntiJoinRule.apply`.
- Observation: `LmdbCascadesConnectedJoinPlanner.greedyPlan` is used for large join islands, but its disconnected finite-anchor bridge uses `int` masks and `1 << factorIndex`. Java masks int shift counts modulo 32.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedJoinPlanner.java`, methods `greedyPlan`, `connectedExtensionRejection`, `disconnectedFiniteAnchorEnablesBridge`, and `maskFor`.
- Observation: Several report rewrites already happen before Cascades in `LmdbQueryOptimizerPipeline`, including binding assignment, constants, compare/sameTerm, conjunctive and disjunctive filters, union scope changes, RDF4J normalization, projection removal, filter optimization, iterative evaluation, bound simplification, projection pushdown, set semantics, semantic dependencies, and LMDB filter simplification.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java`.

## Decision Log

- Decision: Fix equivalence bugs before adding new rewrites.
  Rationale: New alternatives increase the search space. Existing unsound alternatives must be removed first so later tests do not hide incorrect winners behind better cost choices.
  Date/Author: 2026-05-31 / Codex.
- Decision: Keep parser/query-form rewrites out of Cascades.
  Rationale: ASK, DESCRIBE, CONSTRUCT, wildcard projection expansion, and SPARQL surface syntax lowering are not tuple-algebra search alternatives. They belong in parser/desugar stages or existing pre-Cascades optimizers.
  Date/Author: 2026-05-31 / Codex.
- Decision: Do not implement Jena-style `DISTINCT` to `REDUCED` as a logical Cascades rewrite.
  Rationale: `REDUCED` can return duplicates and is not bag-equivalent to exact `DISTINCT`. Cascades may implement physical duplicate elimination alternatives, but not an exact-to-weaker logical rewrite.
  Date/Author: 2026-05-31 / Codex.
- Decision: Reuse existing support logic where possible instead of duplicating optimizer-specific parsing of safe equality and values filters.
  Rationale: `FilterInValuesOptimizer`, `LmdbFilterSimplifierOptimizer`, and `LmdbProjectionPushdownOptimizer` already encode subtle SPARQL-safety checks. Shared helpers reduce drift.
  Date/Author: 2026-05-31 / Codex.
- Decision: Do not add more new rule classes to the already-large `StandardCascadesRules` file.
  Rationale: The repository asks agents to keep files under roughly 500 lines and split/refactor as needed. `StandardCascadesRules` is already over that size. Existing nested rules can stay for compatibility, but new rule families should live in focused files in the same package and be registered from `LmdbCascadesRuleProvider`.
  Date/Author: 2026-05-31 / Codex.
- Decision: Port optional-union rewrites only with the existing LMDB proof guards.
  Rationale: Optional distribution is easy to make non-equivalent. The older LMDB sketch optimizer already contains guarded proofs for left-union optional distribution and a narrow mutually-exclusive right-union optional distribution. Cascades should reuse those conditions rather than applying a broad algebra pattern.
  Date/Author: 2026-05-31 / Codex.
- Decision: Treat `BindingUniverse` ordinals and `BindingMask` sets as the planner-core binding identity, not `Var`
  instances and not mutable `Set<String>` intersections. `Var` objects are only algebra syntax nodes; projections,
  extensions, groups, and VALUES also create visible bindings. Public APIs may keep returning names for now, but
  Cascades memo properties, rule guards, join enumeration, and physical-property checks should operate on masks and
  convert to names only at boundaries.
  Rationale: The same logical binding can appear as many `Var` instances after cloning and rewrites, while many
  binding outputs are not represented by `Var` at all. Query-local dense ordinals make membership, intersection,
  union, nullable, and assured-binding checks cheap and structurally correct.
  Date/Author: 2026-06-06 / Codex.

## Outcomes & Retrospective

Milestone 1 implementation is present in the current worktree and covered by focused rule/connected-planner tests.
Milestone 2 shared support extraction is present and covered by the focused filter and Cascades rule tests.
Milestone 3 structural simplification rules are present and covered by focused Cascades rule tests plus LMDB provider
registration coverage.
Milestone 4 finite-anchor filter rules are present and covered by focused Cascades tests, LMDB registration coverage,
and representative benchmark spot checks.
Projection/filter placement rules are present and covered by focused Cascades tests, LMDB registration coverage, and
representative benchmark spot checks.
Guarded optional/union distribution rules are present and covered by focused Cascades tests, LMDB registration
coverage, and representative benchmark spot checks.
Representative runQuery benchmarks are competitive with the May comparison set. Remaining plan work starts at
remaining minus distribution coverage and order/distinct coverage items.

## Context and Orientation

Cascades is the optimizer package under `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades`. A Cascades rule is a Java class implementing `CascadesRule`; existing standard logical rules live as nested classes in `StandardCascadesRules`, while new rule families in this plan live in focused sibling files such as `StructuralCascadesRules`, `FilterCascadesRules`, `ProjectionCascadesRules`, and `SetCascadesRules`. A logical transformation returns a `RuleApplication.transformation(...)` and must create an equivalent `TupleExpr` alternative in the same memo group.

LMDB registers Cascades rules in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`. The LMDB optimizer pipeline in `LmdbQueryOptimizerPipeline` runs several classic RDF4J optimizers before `LmdbCascadesOptimizer`. Rewrites already guaranteed by that pre-Cascades pipeline do not need duplicate Cascades rules unless they create useful alternatives during memo search after other rules have changed shape.

The main rule tests live in `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`. LMDB rule-admissibility and connected planner tests live in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedRuleAdmissibilityTest.java`. Existing helper methods in these tests create `StatementPattern`, `Projection`, `BindingSetAssignment`, and rule applications directly. Add focused tests there when the behavior is generic Cascades; add LMDB tests when the behavior depends on LMDB rule registration, connected join planning, or LMDB-only cost model support.

Important SPARQL terms used here:

Projection means `SELECT ?x ...` choosing which variables remain visible in the result. A projection rewrite is unsafe if it exposes variables not selected by the original projection.

LeftJoin is RDF4J algebra for SPARQL `OPTIONAL`. If the optional right side has no match, left rows survive with right-side variables unbound.

Difference is RDF4J algebra for SPARQL `MINUS`. `MINUS` only removes a left row when the right row is compatible and shares at least one variable with it.

BindingSetAssignment is RDF4J algebra for finite `VALUES`. It is useful as a small finite anchor because LMDB can often plan a direct lookup from it.

Scope-changing nodes are tuple algebra nodes that introduce a SPARQL subquery or branch-local binding scope. Do not push filters, projections, or finite bindings across them unless the rule proves it preserves variable visibility.

## Rewrite Coverage Matrix

Implement these Cascades rule families:

1. Correctness fixes: safe projection-over-join pruning, shared-variable guard for optional anti-join, and large greedy connected-planner masks.
2. Structural simplification: nested filter merge, filter true/false, join with empty/singleton, union flattening, union empty elimination, projection merge, distinct/reduced no-op over empty or already distinct where detectable.
3. Finite anchors: safe equality, `sameTerm`, and `IN` filters to `BindingSetAssignment` joins as costed alternatives.
4. Projection pruning: projection through filter, join, difference, left join, and union while retaining the original outer projection and preserving join keys, minus shared keys, and optional condition variables.
5. Filter pushdown expansion: filter through projection when identity and condition variables are preserved, filter through left side of left join when condition variables are left-assured, and filter through extension only when it does not depend on extension output names.
6. Union distribution: join over union is already present; add optional-left-union distribution and minus-left-union coverage tests; add union flattening.
7. Optional and minus: fix optional anti-join, add guarded optional-left-union distribution, add guarded mutually-exclusive optional-right-union distribution, keep null-rejecting optional behavior in LMDB rule, and extend tests for no-shared RHS and branch-local bindings.
8. Order, slice, distinct, and aggregate dispositions: implement only bag-equivalent/no-op/safe physical-property rules. Record non-equivalent or already-pipeline-covered items in tests or documentation so the rewrite audit is complete.
9. Binding-mask core migration: keep `BindingUniverse` query-local and immutable after the memo is seeded, keep
   `BindingShape` as the canonical possible/assured/nullable/local-output model, and migrate hot Cascades internals
   away from repeated `HashSet<String>` construction. Retain compatibility name sets only for existing RDF4J public
   APIs, algebra rendering, telemetry, and statistics-provider calls that still require names.

Do not implement these as Cascades rules in this plan:

ASK, DESCRIBE, CONSTRUCT, wildcard projection expansion, GRAPH group lowering, blank-node-to-variable parser desugaring, and SPARQL subquery lifting. These occur before tuple-algebra Cascades or are query-form transformations rather than memo alternatives.

Do not implement SERVICE/text/geo magic predicate rewrites unless a later LMDB SERVICE/text/geo physical implementation is explicitly introduced. Cascades should not invent SERVICE calls as logical alternatives in this module.

Do not implement `DISTINCT` to `REDUCED` as a logical rewrite because it weakens exact result semantics.

## Plan of Work

### Milestone 1: Baseline Evidence and Existing Bug Fixes

This milestone fixes known unsound behavior before new rules increase the search space. It produces three focused failing tests first, then minimal production changes.

Files:

- Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`.
- Modify `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StandardCascadesRules.java`.
- Modify `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedRuleAdmissibilityTest.java`.
- Modify `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedJoinPlanner.java`.
- Artifact `initial-evidence.txt`.

Concrete test additions:

Add `projectionPushdownKeepsOuterProjectionWhenOtherSideBindsExtraNames` to `CascadesRuleEngineTest`. Build `Projection(Join(pattern("s","p1","leftOnly"), pattern("s","p2","rightOnly")), projection("leftOnly"))`, apply `ProjectionPushdownRule`, and assert every returned alternative has root type `Projection` with binding names equal to `Set.of("leftOnly")`. Then inspect the projection argument and assert it is the rewritten `Join`. Before the fix this test fails because the alternative root is a bare `Join`.

Add `optionalNegatedBoundDoesNotRewriteWithoutSharedVariables` to `CascadesRuleEngineTest`. Build `Filter(LeftJoin(pattern("s","p1","o1"), pattern("x","p2","rhsOnly")), Not(Bound(rhsOnly)))`, apply `OptionalNegatedBoundAntiJoinRule`, and assert no `Difference` alternative is returned. Before the fix this test fails because the rule creates a `Difference`.

Add `greedyPlannerFiniteAnchorBridgeHandlesMoreThanThirtyOneFactors` to `LmdbCascadesConnectedRuleAdmissibilityTest`. Build at least 34 runtime factors so greedy planning is forced beyond the DP threshold. Use one already-bound prefix factor, a candidate small `BindingSetAssignment` at index 33, and a later bridge statement pattern that shares variables with both the prefix and candidate. The test should assert the plan is present and the finite anchor is selected before the bridge lookup. Before the fix this can fail or choose an order inconsistent with the bridge because `1 << 33` collides with bit 1.

Implementation:

In `ProjectionPushdownRule.apply`, return `new Projection(new Join(pushedLeft, projectedRightOrOriginalRight), projection.getProjectionElemList().clone(), projection.isSubquery())` or the equivalent constructor available in this tree. The inner child projections must keep variables needed for join compatibility. Follow `LmdbProjectionPushdownOptimizer.pushThroughJoin`: each side keeps projected names present on that side plus shared join variables. The outer projection remains the root alternative so result variables match the original query.

In `OptionalNegatedBoundAntiJoinRule.apply`, add a shared-variable check after existing RHS assured-binding checks. Compute planner names for left and right binding names, intersect them, and return no applications when the intersection is empty. Keep the existing checks that the negated variable is RHS-only and RHS-assured.

In `LmdbCascadesConnectedJoinPlanner`, replace greedy-only prefix mask plumbing with a `Set<Integer>` or `BitSet`. The DP path can keep `int` masks because it is threshold-limited, but `greedyPlan`, `connectedExtensionRejection`, and `disconnectedFiniteAnchorEnablesBridge` must not use `1 << factorIndex` for large islands. Prefer `Set<Integer> prefixIndices = Set.copyOf(order)` for clarity and low risk.

Validation:

Run, in repo root:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Then persist initial evidence:

    python3 scripts/agent-evidence.py --command "mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install" --log maven-build.log core/queryalgebra/evaluation/target/surefire-reports core/sail/lmdb/target/surefire-reports > initial-evidence.txt

Run focused failing tests before production edits:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#projectionPushdownKeepsOuterProjectionWhenOtherSideBindsExtraNames --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#optionalNegatedBoundDoesNotRewriteWithoutSharedVariables --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesConnectedRuleAdmissibilityTest#greedyPlannerFiniteAnchorBridgeHandlesMoreThanThirtyOneFactors --retain-logs

Expected before fixes: each new test fails for the reason described above. Capture the Surefire report snippets before editing production code.

Expected after fixes: the same three commands pass.

Commit after the milestone with message `GH-0000 fix cascades rewrite correctness`.

### Milestone 2: Shared Support for Safe Rewrites

This milestone creates helper code so later rules share exact safety logic with existing optimizers. It should be behavior-neutral until rules call the helpers, so use Routine B if pre/post green and hit proof pass; otherwise add the failing tests described here before helper changes.

Files:

- Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRewriteSupport.java`.
- Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/FilterValuesAnchorSupport.java`.
- Modify `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/FilterInValuesOptimizer.java`.
- Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`.

`CascadesRewriteSupport` responsibilities:

- `plannerNames(Set<String>)`: remove null names and names beginning with `_const_`.
- `intersects(Set<String>, Set<String>)` and `intersection(Set<String>, Set<String>)`.
- `splitConjuncts(ValueExpr)` and `combineConjuncts(List<ValueExpr>)`.
- `isIdentityProjection(Projection)`, `projectionNames(Projection)`, and `projectionElemList(List<String>)`.
- `project(TupleExpr, Set<String>)`: create transparent optimizer projection with `subquery=false` when the constructor is available.
- `branchLocalBindOrValuesNames(TupleExpr)`: preserve the scoped-union protection currently embedded in `StandardCascadesRules`.

Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/FilterValuesAnchorSupport.java` for logic shared with the existing `FilterInValuesOptimizer`. This class must be `public final`, because Cascades is in the subpackage `optimizer.cascades`.

`FilterValuesAnchorSupport` responsibilities:

- `safeValuesAnchor(ValueExpr)`: return a `BindingSetAssignment` for safe RDF-term equality, `sameTerm`, or `ListMemberOperator` filters.
- Preserve the safety restrictions from `FilterInValuesOptimizer`: allow IRIs and non-numeric, non-calendar, non-duration, non-boolean literals; reject unsafe value equality datatypes; cap finite values at 64.
- Deduplicate repeated constants while preserving first-seen order.

Update `FilterInValuesOptimizer` to delegate to `FilterValuesAnchorSupport.safeValuesAnchor(...)`. This is a behavior-neutral extraction; it needs pre/post green evidence from existing and new focused tests.

Both new Java files must start with the exact 2026 RDF4J header and `// Some portions generated by Codex` immediately below it. Immediately after creating them, run:

    cd scripts && ./checkCopyrightPresent.sh

Concrete tests:

Add or extend focused tests for `FilterInValuesOptimizer` so safe IRI `IN` creates a `BindingSetAssignment`, numeric equality is rejected, and duplicate constants are deduplicated without duplicate binding rows. Add `CascadesRuleEngineTest` coverage for `CascadesRewriteSupport` only through rule behavior in later milestones, not by testing private helper details directly.

Validation:

Run:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs

Expected: pre-change and post-change green if this milestone is pure extraction. If any helper changes behavior, switch to Routine A and capture the failing test first.

Commit after the milestone with message `GH-0000 refactor cascades rewrite support`.

### Milestone 3: Structural Simplification Rules

This milestone adds bag-equivalent simplification alternatives that reduce memo noise and give later rules cleaner shapes to match.

Files:

- Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StructuralCascadesRules.java`.
- Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`.
- Modify `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`.

Rules to add:

- `NestedFilterMergeRule`: `Filter(Filter(arg, c1), c2)` becomes `Filter(arg, And(c1, c2))` when neither filter is a variable-scope change.
- `FilterConstantRule`: `Filter(arg, true)` becomes `arg`; `Filter(arg, false)` becomes `EmptySet`. Use RDF4J boolean literal constants only; do not try to evaluate arbitrary expressions here because `ConstantOptimizer` already handles that before Cascades.
- `JoinEmptySetRule`: `Join(EmptySet, x)` and `Join(x, EmptySet)` become `EmptySet`.
- `JoinSingletonRule`: `Join(SingletonSet, x)` and `Join(x, SingletonSet)` become `x`, preserving the non-singleton side.
- `UnionSimplificationRule`: flatten nested non-scope-changing `Union`, remove `EmptySet` branches, and preserve `variableScopeChange` when the original union had it. Do not flatten scope-changing unions across branch-local binding boundaries.
- `ProjectionMergeRule`: `Projection(Projection(arg, inner), outer)` becomes `Projection(arg, outer)` only for identity projections where the inner projection contains all names required by the outer projection.

Concrete tests:

Add one test method per rule in `CascadesRuleEngineTest`:

- `nestedFilterMergeCombinesConditionsWithoutCrossingScope`.
- `constantFilterTrueAndFalseSimplify`.
- `joinEmptyAndSingletonSimplify`.
- `unionFlatteningDoesNotCrossScopeChangingUnion`.
- `projectionMergeRequiresIdentityProjection`.

Each test should apply the specific rule with the existing `apply(...)` helper and assert the returned alternative type and key fields. Add one negative case for scope-changing union and one negative case for projection aliases or aggregate projection elements.

Validation:

Run each new test first and capture failures. Then implement and rerun:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#nestedFilterMergeCombinesConditionsWithoutCrossingScope --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#constantFilterTrueAndFalseSimplify --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#joinEmptyAndSingletonSimplify --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#unionFlatteningDoesNotCrossScopeChangingUnion --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#projectionMergeRequiresIdentityProjection --retain-logs

Add the 2026 RDF4J header and `// Some portions generated by Codex` to the new source file, then run `cd scripts && ./checkCopyrightPresent.sh`.

Register these rules in `LmdbCascadesRuleProvider.rules(...)` before implementation rules and before cost-sensitive distribution rules. Rerun:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs

Commit after the milestone with message `GH-0000 feat cascades structural rewrite rules`.

### Milestone 4: Finite Anchor and Equality Rules

This milestone brings the useful part of Jena/Oxigraph filter equality rewrites into Cascades without weakening RDF term semantics.

Files:

- Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/FilterCascadesRules.java`.
- Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`.
- Modify `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`.
- Use `FilterValuesAnchorSupport.safeValuesAnchor(...)` from Milestone 2 for finite anchor recognition.

Rules to add:

- `FilterValuesAnchorRule`: for `Filter(arg, condition)` where `condition` is safe `?v IN (...)`, `?v = constant`, `constant = ?v`, or `sameTerm(?v, constant)`, and `arg.getAssuredBindingNames()` contains `v`, create a costed logical alternative `Join(valuesAssignment, arg.clone())`. Drop the filter only for the same safe cases accepted by `FilterInValuesOptimizer`; reject numeric, calendar, duration, and boolean literal equality because SPARQL value equality can differ from RDF term equality.
- `FilterConjunctValuesAnchorRule`: for `Filter(arg, And(...))`, extract one or more safe finite anchor conjuncts, prepend their `BindingSetAssignment` joins, and keep a residual filter for conjuncts not converted. Do not duplicate anchors already present in the argument.

Concrete tests:

Add:

- `filterValuesAnchorTurnsSafeInIntoValuesJoin`.
- `filterValuesAnchorRejectsNumericValueEquality`.
- `filterConjunctValuesAnchorKeepsResidualCondition`.
- `filterValuesAnchorRequiresAssuredArgumentBinding`.
- `filterValuesAnchorDeduplicatesRepeatedConstants`.

Each test should inspect the `TupleExpr` alternative, not rely on string rendering. Assert that `BindingSetAssignment` has the expected binding name and row count.

Validation:

Run the five tests red, implement, then rerun them green:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#filterValuesAnchorTurnsSafeInIntoValuesJoin --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#filterValuesAnchorRejectsNumericValueEquality --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#filterConjunctValuesAnchorKeepsResidualCondition --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#filterValuesAnchorRequiresAssuredArgumentBinding --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#filterValuesAnchorDeduplicatesRepeatedConstants --retain-logs

Then run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs

Add the 2026 RDF4J header and `// Some portions generated by Codex` to the new source file, then run `cd scripts && ./checkCopyrightPresent.sh`.

Commit after the milestone with message `GH-0000 feat cascades finite filter anchors`.

### Milestone 5: Projection and Filter Placement Expansion

This milestone makes Cascades capable of producing the safe pruning and placement alternatives currently found in classic optimizers, while preserving the outer logical result shape.

Files:

- Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/ProjectionCascadesRules.java`.
- Modify `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/FilterCascadesRules.java`.
- Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`.
- Modify `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`.

Projection rules:

- Replace or rename the fixed `ProjectionPushdownRule` so its behavior is explicitly projection pruning through join.
- Add `ProjectionFilterPushdownRule`: `Projection(Filter(arg, condition))` becomes `Projection(Filter(Projection(arg, projectedNames plus condition variable names), condition), originalProjectionElems)` for identity projections only.
- Add `ProjectionDifferencePushdownRule`: prune the left side to projected names plus shared MINUS names; prune the right side to shared MINUS names. Retain the outer projection.
- Add `ProjectionLeftJoinPushdownRule`: prune left and right using projected names, shared names, and left-join condition names; retain the outer projection. For a conditioned `LeftJoin`, keep each condition variable on every side that can bind it. If a condition variable is not visible from either side, do not create an alternative.
- Keep existing `ProjectionUnionDistributionRule`, but add tests for scope-changing union and branch-local missing bindings.

Filter rules:

- Add `FilterProjectionPushdownRule`: `Filter(Projection(arg, elems), condition)` can become `Projection(Filter(arg, condition), elems)` only when projection is identity and contains every condition variable.
- Add `FilterLeftJoinLeftPushdownRule`: `Filter(LeftJoin(left,right), condition)` can push into the left input only when all condition variables are left-assured and the left join has no condition or the condition does not depend on nullable right variables.
- Add `FilterExtensionPushdownRule`: `Filter(Extension(arg, elems), condition)` can push into `arg` only when the filter condition does not reference any extension output name.

Concrete tests:

Add:

- `projectionJoinPruningRetainsOuterProjectionAndSharedJoinVars`.
- `projectionFilterPushdownKeepsConditionVars`.
- `projectionDifferencePushdownKeepsMinusSharedVars`.
- `projectionLeftJoinPushdownKeepsOptionalSharedAndConditionVars`.
- `filterProjectionPushdownRequiresIdentityProjectionAndVisibleVars`.
- `filterLeftJoinPushdownUsesOnlyLeftAssuredVars`.
- `filterExtensionPushdownRejectsExtensionOutputVars`.

Validation:

Run each new test red before production edits, then green after implementation. Then run:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesConnectedRuleAdmissibilityTest --retain-logs

Add the 2026 RDF4J header and `// Some portions generated by Codex` to the new projection rules file, then run `cd scripts && ./checkCopyrightPresent.sh`.

Commit after the milestone with message `GH-0000 feat cascades projection filter placement`.

### Milestone 6: Union, Optional, and Minus Distribution

This milestone covers the useful report rewrites around union flattening, optional left-union distribution, and minus alternatives while avoiding known non-equivalences.

Files:

- Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/SetCascadesRules.java`.
- Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`.
- Modify `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java`.

Rules to add or extend:

- `OptionalLeftUnionDistributionRule`: `LeftJoin(Union(a,b), right)` becomes `Union(LeftJoin(a,right), LeftJoin(b,right))` only when all of the existing LMDB sketch proof guards pass. The left union must not be scope-changing. Neither branch nor the optional RHS may contain `Extension`, `Group`, `Projection`, `LeftJoin`, `Difference`, nested `Union`, `Service`, or nested `EXISTS` filters. The optional RHS must share a non-empty identical binding-name set with both left branches. If the `LeftJoin` has a condition, that condition's variables must be visible from each distributed branch plus the optional RHS.
- `OptionalRightUnionMutuallyExclusiveDistributionRule`: port only the guarded existing pattern from `LmdbSketchJoinOptimizer.rewriteMutuallyExclusiveRightUnionOptional`. `LeftJoin(base, Union(branch1, branch2, ...))` may become a union of branch-specific optionals only when each branch has a local discriminator condition on a base binding, the base is a finite `BindingSetAssignment` covering exactly those discriminator values, every branch produces the same right-only binding names, and every branch condition is local to base plus branch variables. Do not add a broad `LeftJoin(left, Union(a,b))` distribution rule.
- Extend `MinusAlternativeRule` tests for left-union distribution and chained-minus alternatives. Keep right-union MINUS distribution out unless a proof is added, because `A MINUS (B UNION C)` is equivalent to `(A MINUS B) MINUS C`, not to unioning alternatives.
- Add explicit `JoinUnionDistributionRule` negative tests for scope-changing branch binding overlap and positive tests for finite binding extraction.

Concrete tests:

Add:

- `optionalLeftUnionDistributionPreservesLeftBranchMultiplicity`.
- `optionalLeftUnionDistributionRejectsUnsupportedBranchShapes`.
- `optionalRightUnionDistributionIsNotAppliedWithoutMutualExclusion`.
- `optionalRightUnionDistributionAppliesForFiniteDiscriminatorBranches`.
- `minusLeftUnionDistributionPreservesScope`.
- `minusRightUnionUsesChainedDifferenceNotUnion`.
- `joinUnionDistributionRejectsScopedBranchOverlap`.

Validation:

Run focused red/green tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#optionalLeftUnionDistributionPreservesLeftBranchMultiplicity --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#optionalLeftUnionDistributionRejectsUnsupportedBranchShapes --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#optionalRightUnionDistributionIsNotAppliedWithoutMutualExclusion --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#optionalRightUnionDistributionAppliesForFiniteDiscriminatorBranches --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#minusLeftUnionDistributionPreservesScope --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#minusRightUnionUsesChainedDifferenceNotUnion --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#joinUnionDistributionRejectsScopedBranchOverlap --retain-logs

Then run:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs

Add the 2026 RDF4J header and `// Some portions generated by Codex` to the new set rules file, then run `cd scripts && ./checkCopyrightPresent.sh`.

Commit after the milestone with message `GH-0000 feat cascades union optional minus rewrites`.

### Milestone 7: Order, Slice, Distinct, Aggregate, and Report Disposition

This milestone prevents a false sense of completeness. It implements only safe Cascades rules and records why other report rewrites are not Cascades rules.

Files:

- Modify `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StructuralCascadesRules.java` only for safe rules.
- Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`.
- Create `docs/cascades-sparql-rewrite-coverage.md`.

Safe rules to implement:

- `SliceOverEmptyRule`: `Slice(EmptySet, offset, limit)` becomes `EmptySet`.
- `OrderOverEmptyRule`: `Order(EmptySet)` becomes `EmptySet`.
- `DistinctOverEmptyRule`: `Distinct(EmptySet)` becomes `EmptySet`.
- `ReducedOverEmptyRule`: `Reduced(EmptySet)` becomes `EmptySet`.
- `DistinctOverDistinctRule`: nested `Distinct` collapses to one `Distinct`.

Do not add an order-limit physical-property rule in this plan. Leave `OrderLimitOptimizer` after Cascades as the owner because exact ordering and limit semantics are not represented in current `PhysicalProperties`.

Coverage document content:

Create `docs/cascades-sparql-rewrite-coverage.md` with sections for Blazegraph, Jena, Oxigraph, and QLever. For each report family, record one of:

- Cascades rule implemented, with rule id and test name.
- Covered by pre-Cascades pipeline, with optimizer class and pipeline position.
- Existing LMDB physical alternative, with rule id and test name.
- Not a Cascades rewrite, with rationale.
- Intentionally excluded because not semantics-preserving for exact RDF4J SPARQL.

Concrete tests:

Add:

- `emptyOrderSliceDistinctReducedSimplify`.
- `nestedDistinctSimplifies`.
- `distinctToReducedIsNotRegisteredAsLogicalRewrite`.

Validation:

Run:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#emptyOrderSliceDistinctReducedSimplify --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#nestedDistinctSimplifies --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#distinctToReducedIsNotRegisteredAsLogicalRewrite --retain-logs

Commit after the milestone with message `GH-0000 docs cascades rewrite coverage`.

### Milestone 8: Registry, Pipeline, and End-to-End Verification

This milestone proves the rules are registered in the LMDB Cascades path and do not break the existing optimizer suite.

Files:

- Modify `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOptimizerPipelineTest.java`.
- Modify `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedRuleAdmissibilityTest.java`.
- Modify `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java` only to adjust rule order.

Concrete tests:

Add a registry test that builds a `RuleRegistry` through `LmdbCascadesRuleProvider.rules(...)` and asserts the presence of every new standard rule id. Also assert intentionally excluded rule ids are absent, especially a made-up `distinct-to-reduced` rule.

Add a pipeline test showing `FilterInValuesOptimizer` is not inserted into the LMDB pipeline by this plan. Finite anchors are intentionally provided by the new Cascades finite-anchor rules and existing `LmdbFilterSimplifierOptimizer`; record that disposition in `docs/cascades-sparql-rewrite-coverage.md`.

Validation:

Run:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptimizerPipelineTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesConnectedRuleAdmissibilityTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs

Run module verification:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run copyright check before formatting:

    cd scripts && ./checkCopyrightPresent.sh

Run formatter/resources:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Run final quick install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Commit after the milestone with message `GH-0000 test cascades rewrite registry`.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

1. Check worktree status and avoid overwriting unrelated current changes:

    git status --short --untracked-files=no

2. Run the mandatory quick install and persist `initial-evidence.txt` before implementation testing:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'
    python3 scripts/agent-evidence.py --command "mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install" --log maven-build.log core/queryalgebra/evaluation/target/surefire-reports core/sail/lmdb/target/surefire-reports > initial-evidence.txt

3. For each milestone, add the smallest failing test first, run it with `mvnf`, capture the Surefire snippet, then edit production code. Do not run Maven tests with `-am` or `-q`.

4. After each production patch, rerun the same focused test selection and capture the passing Surefire snippet.

5. Broaden only after focused tests pass: first `CascadesRuleEngineTest`, then `LmdbCascadesConnectedRuleAdmissibilityTest`, then the relevant modules.

6. Keep `docs/cascades-sparql-rewrite-coverage.md` current whenever a report item is implemented or dispositioned.

## Validation and Acceptance

Acceptance requires all of the following:

- The three known bugs have focused regression tests that fail before production edits and pass after.
- Every new logical rewrite has at least one focused rule test and at least one negative test for scope or semantic safety when relevant.
- `LmdbCascadesRuleProvider.rules(...)` registers every intended Cascades rule and no intentionally excluded non-equivalent rule.
- `docs/cascades-sparql-rewrite-coverage.md` maps all rewrite families from the four research reports to implemented, already-covered, physical-only, out-of-scope, or intentionally-excluded status.
- `python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs` passes.
- `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesConnectedRuleAdmissibilityTest --retain-logs` passes.
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs` passes.
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` passes, or any unrelated pre-existing failures are documented with exact report snippets.
- `cd scripts && ./checkCopyrightPresent.sh` passes.
- `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` completes.
- Final handoff includes Routine A evidence blocks for failing and passing tests, or Routine B pre/post green plus hit proof for purely behavior-neutral helper extraction.

## Idempotence and Recovery

All changes should be additive or local replacements. Running the same focused tests repeatedly is safe. Running the quick install repeatedly is safe and should update `.m2_repo` with the current workspace artifacts.

If an offline Maven command fails because of missing dependencies or plugins, rerun the exact command once without `-o`, then return to offline commands.

If a new rule produces an explosion of alternatives or planner timeout, disable only that new rule registration while keeping its test and implementation in the worktree, then add a focused budget/duplicate-suppression test before re-enabling it. Do not mute existing tests or weaken assertions.

If a logical rewrite cannot be proven bag-equivalent, do not implement it as a `RuleKind.TRANSFORMATION`. Record it in `docs/cascades-sparql-rewrite-coverage.md` as out of scope or physical-only, with the reason.

If unrelated local modifications conflict with the implementation, stop and inspect `git diff` for the affected files. Preserve user/other-agent changes and adjust the patch around them.

## Artifacts and Notes

Current dirty files observed during plan creation:

    core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/MemoGroup.java
    core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesPlanner.java
    core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/StandardCascadesRules.java
    core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesMemoModelTest.java
    core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedJoinPlanner.java
    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java
    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesConnectedRuleAdmissibilityTest.java
    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesContextPropagationTest.java

Treat these as existing user or other-agent changes. Do not revert them unless Håvard explicitly asks.

Untracked artifacts observed during plan creation must also be kept unless Håvard explicitly asks for cleanup:

    core/queryalgebra.zip
    core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java.orig
    core/sail/lmdb.zip
    core/sail/lmdb/hs_err_pid92106.log
    docs/superpowers/plans/2026-05-27-pipeline-wide-no-cartesian-join-planning.md
    focused-lmdb-it.log
    frontier-red-evidence.txt
    htmlReport.zip
    trace.txt

Research report source files:

    /Users/havardottestad/Downloads/deep-research-report1.md
    /Users/havardottestad/Downloads/deep-research-report2.md
    /Users/havardottestad/Downloads/deep-research-report3.md
    /Users/havardottestad/Downloads/deep-research-report4.md

## Interfaces and Dependencies

New generic helper class:

    package org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades;

    final class CascadesRewriteSupport {
        static Set<String> plannerNames(Set<String> names);
        static boolean intersects(Set<String> left, Set<String> right);
        static Set<String> intersection(Set<String> left, Set<String> right);
        static List<ValueExpr> splitConjuncts(ValueExpr expression);
        static ValueExpr combineConjuncts(List<ValueExpr> conjuncts);
        static boolean isIdentityProjection(Projection projection);
        static List<String> projectionNames(Projection projection);
        static ProjectionElemList projectionElemList(List<String> names);
        static TupleExpr project(TupleExpr arg, Set<String> neededNames);
        static Set<String> branchLocalBindOrValuesNames(TupleExpr tupleExpr);
    }

New shared finite-anchor helper:

    package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

    public final class FilterValuesAnchorSupport {
        public static BindingSetAssignment safeValuesAnchor(ValueExpr condition);
    }

New or fixed standard rule classes in `StandardCascadesRules`:

    ProjectionPushdownRule
    OptionalNegatedBoundAntiJoinRule

New structural rule classes in `StructuralCascadesRules`:

    NestedFilterMergeRule
    FilterConstantRule
    JoinEmptySetRule
    JoinSingletonRule
    UnionSimplificationRule
    ProjectionMergeRule
    SliceOverEmptyRule
    OrderOverEmptyRule
    DistinctOverEmptyRule
    ReducedOverEmptyRule
    DistinctOverDistinctRule

New filter rule classes in `FilterCascadesRules`:

    FilterValuesAnchorRule
    FilterConjunctValuesAnchorRule
    FilterProjectionPushdownRule
    FilterLeftJoinLeftPushdownRule
    FilterExtensionPushdownRule

New projection rule classes in `ProjectionCascadesRules`:

    ProjectionFilterPushdownRule
    ProjectionDifferencePushdownRule
    ProjectionLeftJoinPushdownRule

New set/optional/minus rule classes in `SetCascadesRules`:

    OptionalLeftUnionDistributionRule
    OptionalRightUnionMutuallyExclusiveDistributionRule

Rule ids should be stable kebab-case strings, for example `filter-values-anchor`, `optional-left-union-distribution`, and `projection-leftjoin-pushdown`. Tests should assert rule ids through `LmdbCascadesRuleProvider.rules(...)` after registration.

Revision note: Initial plan created from static review on 2026-05-31. The plan intentionally separates semantically safe Cascades transformations from parser-level and pipeline-level rewrites so implementation can be complete without adding unsound or misplaced rules.

Revision note: Reviewed and tightened on 2026-05-31. The revision splits new rules into focused files, adds immediate copyright checks for new Java files, replaces broad optional-union guidance with existing LMDB proof guards, and adds the narrow mutually-exclusive right-union optional rewrite that was missing from the first version.
