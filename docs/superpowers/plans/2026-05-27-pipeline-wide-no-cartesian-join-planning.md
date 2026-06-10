# Pipeline-wide no-Cartesian join planning

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md`.

## Purpose / Big Picture

LMDB Cascades query planning must not build Cartesian product join trees inside a connected reorderable SPARQL inner
join island. A connected island is a set of inner-join factors where every factor can be reached from every other factor
through shared runtime variables. Users should be able to run the AAS threshold property-path query and see a connected
join plan, not a generic RDF4J iterator tree or recursive LMDB access-path join that bypasses the connected-only planner.

The observable outcome is an optimized plan for the AAS `query2ThresholdCount` shape with connected enumeration telemetry
on the main join island, no generic or access-path physical `Join` winner inside that island, and no positive Cartesian
work unless the query is genuinely disconnected and annotated as explicit Phase 2 fallback.

## Progress

- [x] (2026-05-27T06:38:14Z) Ran required root quick install before edits.
- [x] (2026-05-27T06:39:00Z) Persisted initial green install evidence in `initial-evidence.txt`.
- [x] (2026-05-27T06:45:54Z) Confirmed existing relationship-power path test is green and insufficient for the current
  threshold query failure.
- [x] (2026-05-27T07:08:31Z) Added exact AAS threshold red regression; it fails on the benchmark shape.
- [x] (2026-05-27T07:58:02Z) Added Cascades rule-admissibility red regressions for connected joins and property paths.
- [x] (2026-05-27T08:03:31Z) Suppressed generic/access-path join bypasses and generic property-path bypass.
- [x] (2026-05-27T08:03:31Z) Made property paths supported join-island factors for LMDB rule ownership.
- [x] (2026-05-27T08:03:31Z) Removed sketch-provider finite-input and anti-join post-plan reshuffles.
- [x] (2026-05-27T08:32:00Z) Fixed disconnected projected-island final-row propagation.
- [x] (2026-05-27T08:38:00Z) Fixed plan rendering for runtime-bound variables under
  `BoundStatementPatternJoinIteration`.
- [x] (2026-05-27T09:29:00Z) Fixed join-island outer binding visibility for connected component classification,
  LMDB delivered physical properties, and `Union` input-goal propagation.
- [x] (2026-05-27T09:29:00Z) Fixed `mvnf` selector isolation so focused Surefire selections skip unrelated Failsafe
  ITs, and focused Failsafe selections skip unrelated Surefire tests.
- [ ] Verify remaining module tests and benchmark sanity.

## Surprises & Discoveries

- Observation: The existing `relationshipPowerPathDoesNotUseGenericJoinImplementation` test passes, so it does not
  reproduce the `query2ThresholdCount` benchmark failure.
  Evidence: `org.eclipse.rdf4j.sail.lmdb.estimate.LmdbEstimateAuditHarnessTest.txt` reports one passing test in 1.564 s.
- Observation: After the focused test completed, a surefire JVM stayed alive doing background sketch estimator rebuild
  work. This is separate from the red assertion needed for the current query but confirms long-running test evidence must
  include breadcrumbs and process snapshots.
  Evidence: `jcmd` showed `RdfJoinEstimator-Refresh` in `SketchBasedJoinEstimator.rebuild`.
- Observation: The exact AAS threshold regression reproduces all three bypasses from the benchmark plan: connected
  `Join` nodes annotated as `fallback_no_winner`, `Join` winners from `lmdb-access-path`, and an `ArbitraryLengthPath`
  winner from `generic-physical-implementation`.
  Evidence: `LmdbEstimateAuditHarnessTest#specificAssetThresholdPathDoesNotUseCartesianBypass` fails with
  `plannedEstimateDecisionId=lmdb-access-path:g6:e21`, `plannedCostWorkRows=603.1M`, and
  `plannedEstimateDecisionId=generic-physical-implementation:g0:e1` on the path node.
- Observation: Rule applicability itself exposes the bug before costing. A connected two-pattern island has applicable
  rules `[lmdb-sketch-join-order-provider, lmdb-access-path, join-commute, generic-physical-implementation]`; an LMDB
  property-path factor has `[lmdb-property-path, generic-physical-implementation]`.
  Evidence: `LmdbCascadesConnectedRuleAdmissibilityTest` fails two assertions against those rule id lists.
- Observation: The rule-admissibility test now passes. The connected island is owned by
  `lmdb-sketch-join-order-provider`, and the property path no longer admits `generic-physical-implementation`.
  Evidence: `LmdbCascadesConnectedRuleAdmissibilityTest` reports `Tests run: 2, Failures: 0, Errors: 0`.
- Observation: The AAS threshold plan later exposed a display-only binding-state bug. The planner correctly passes bound
  lookup variables through `plannedBoundVars`, but the tree renderer did not treat `BoundStatementPatternJoinIteration`
  as a left-bound-right algorithm, so shared RHS variables could be printed as `bindingState=unbound`.
  Evidence: `QueryModelTreeToGenericPlanNodeTest#annotatesBoundStatementPatternJoinRightSideVarsAsBound` initially
  failed because the right-side shared variable rendered unbound.
- Observation: Disconnected projected islands could publish stale raw cardinality at `Projection` while the chosen join
  provider had a better final-row estimate. Normalizing per-step final rows after LMDB enrichment fixes the mismatch.
  Evidence: `LmdbEstimateAuditHarnessTest#disconnectedProjectedIslandsUseJoinFinalRowsForCardinality` initially failed
  with `full plannedRows=1.0 actualRows=312 qError=312.0` while the join planned rows were accurate.
- Observation: The connected-component counter could still report multiple structural components inside an island whose
  runtime execution was seeded by an already-bound outer variable. The fix treats definite outer bindings as a virtual
  connectivity seed for structural classification.
  Evidence: `SketchBasedJoinEstimatorJoinOrderPlannerTest#planJoinOrderTreatsOuterBoundVarsAsVirtualConnectivitySeed`
  passes with `Tests run: 1, Failures: 0, Errors: 0`.
- Observation: LMDB physical alternatives were costed with outer bound variables but some winners delivered
  `inputBoundVars=[]`, making later audit/rendering think runtime-bound variables were unbound. The fix publishes the
  same input-bound context in sketch join-order, guarantee-option, and star multi-predicate scan physical properties.
  Evidence: `LmdbCascadesContextPropagationTest` passes with `Tests run: 4, Failures: 0, Errors: 0`.
- Observation: `CascadesPlanner.inputGoals()` propagated required bound variables into join/left-join/difference
  children but not `Union` branches. This made union branch islands blind to variables bound outside the union.
  Evidence: `LmdbCascadesContextPropagationTest#unionBranchJoinIslandsReceiveOuterBindings` is covered by the passing
  class run.
- Observation: Focused `mvnf` unit-test selections leaked into unrelated Failsafe ITs because the verify command used
  only `-Dtest=...`, which does not disable Failsafe. The script now appends `-DskipITs` for Surefire selectors and
  `-PskipUnitTests` for `--it` selectors.
  Evidence: `.codex/skills/mvnf/test_mvnf.py` passes, and the real `LmdbCascadesContextPropagationTest` run shows
  `-DskipITs` with `failsafe:integration-test` skipped and no `target/failsafe-reports` directory.

## Decision Log

- Decision: Use the exact AAS threshold count query as the first red regression rather than relying on the existing
  relationship-power path regression.
  Rationale: The existing regression is green while the benchmark still produces Cartesian-prone fallback and generic
  plans.
  Date/Author: 2026-05-27 / Codex.
- Decision: Enforce connectedness at physical rule admissibility, not only inside `SketchJoinOrderPlanner`.
  Rationale: The observed bad plan is selected by `generic-physical-implementation` and `lmdb-access-path` `Join`
  alternatives that never enter the connected-only planner.
  Date/Author: 2026-05-27 / Codex.
- Decision: Treat definite outer bindings as island inputs, not as local factor variables.
  Rationale: Runtime connectivity depends on variables that are already bound by the enclosing operator, but those
  variables must remain lookup context rather than becoming reorderable factors.
  Date/Author: 2026-05-27 / Codex.
- Decision: Make `mvnf` focused selectors mutually exclusive between Surefire and Failsafe.
  Rationale: A focused unit regression should not launch unrelated benchmark ITs, and a focused IT should not run the
  module's unit-test suite before the selected Failsafe test.
  Date/Author: 2026-05-27 / Codex.

## Outcomes & Retrospective

Not complete yet.

## Context and Orientation

`LmdbCascadesRuleProvider` registers LMDB-specific Cascades physical rules. Its sketch join-order rule delegates to
`SketchJoinOrderPlanner`, which already has a local connected-only enumeration mode. The same provider also registers
`LmdbAccessPathImplementationRule`, which currently accepts nested `Join` trees recursively, and the generic Cascades
rule can clone any logical expression as a physical RDF4J iterator implementation. Those two rules can produce physical
join trees without connected-only enumeration.

`StandardCascadesRules.GenericImplementationRule` is the generic fallback that keeps the original RDF4J algebra node
legal as a physical plan. It must remain available for unsupported or non-reorderable algebra, but not for connected
reorderable join islands when an LMDB connected join provider can own the island.

Property paths such as `(aas:value)*` appear as `ArbitraryLengthPath` nodes. In the bad benchmark plan, the path can be
costed as a generic standalone full scan inside an otherwise connected island. Property path subject/object variables
must therefore participate in the join island connectivity model.

## Plan of Work

First add failing tests. The initial LMDB regression will load a small AAS-specific asset graph and explain the exact
threshold count query. It will fail if any connected-island `Join` line uses `fallback_no_winner`,
`generic-physical-implementation`, or `lmdb-access-path`, or if any connected-island line reports positive Cartesian
work without explicit Phase 2 telemetry. Separate Cascades unit tests will prove that the generic implementation and
access-path implementation do not match connected reorderable `Join` islands.

Then add a shared internal join-island connectivity helper. It will flatten inner `Join` trees that are safe to reorder,
collect runtime variables from statement patterns, binding assignments, and property paths, classify connected
components, and expose whether a connected complete ordering exists.

Then wire the helper into Cascades rule matching. For connected reorderable join islands, only the connected join-order
provider may produce a physical join tree. Recursive access-path `Join` implementation will be removed. Generic
implementation will be suppressed for connected join islands and for connected property paths when the LMDB property-path
rule can provide a physical alternative.

Finally remove reordering patches that fight the invariant: post-planner finite binding promotions, correlated anti-join
anchor promotions, and subject-type guard placement heuristics in connected Phase 1. Cost math can still rank connected
alternatives; disconnected Cartesian joins remain explicit Phase 2 fallback.

## Concrete Steps

Run commands from the repository root `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Initial install already passed:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

Focused red tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEstimateAuditHarnessTest#specificAssetThresholdPathDoesNotUseCartesianBypass --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesConnectedRuleAdmissibilityTest --retain-logs --stream

Focused green tests after implementation:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEstimateAuditHarnessTest#specificAssetThresholdPathDoesNotUseCartesianBypass --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesConnectedRuleAdmissibilityTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEstimateAuditHarnessTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py QueryModelTreeToGenericPlanNodeTest#annotatesBoundStatementPatternJoinRightSideVarsAsBound --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py QueryModelTreeToGenericPlanNodeTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorJoinOrderPlannerTest#planJoinOrderTreatsOuterBoundVarsAsVirtualConnectivitySeed --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesContextPropagationTest --retain-logs
    python3 .codex/skills/mvnf/test_mvnf.py
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbCascadesConnectedRuleAdmissibilityTest verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbEstimateAuditHarnessTest#disconnectedProjectedIslandsUseJoinFinalRowsForCardinality+specificAssetThresholdPathDoesNotUseCartesianBypass verify

Broader verification:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs --stream

Benchmark sanity:

    ./scripts/run-single-benchmark.sh core/sail/lmdb AASQueriesBenchmark.query

## Validation and Acceptance

The exact AAS threshold regression must fail before the production fix and pass after it. The optimized explanation must
show `optimizer.connectedEnumeration=phase1_connected_only` for the connected island, no connected-island `Join` winner
from `generic-physical-implementation`, no connected-island `Join` winner from `lmdb-access-path`, and no
`fallback_no_winner` on connected-island joins.

A deliberately disconnected query must still report `optimizer.connectedEnumeration=phase2_disconnected_components` and
`optimizer.cartesianFallbackReason=disconnected-components`.

The AAS benchmark must not time out and must not show a connected-island Cartesian product plan.

## Idempotence and Recovery

All edits are additive or localized. Tests may be rerun safely. If a focused test hangs, capture `ps` and `jcmd
Thread.print`, stop only the spawned surefire JVM, and keep the logs. Do not reset or delete untracked user artifacts.

## Artifacts and Notes

Initial install evidence is stored in `initial-evidence.txt`.

Focused evidence captured during implementation:

- `QueryModelTreeToGenericPlanNodeTest#annotatesBoundStatementPatternJoinRightSideVarsAsBound`: red before the renderer
  fix, green after adding `BoundStatementPatternJoinIteration` to left-bound-right rendering.
- `QueryModelTreeToGenericPlanNodeTest`: green, 17 tests.
- `LmdbCascadesConnectedRuleAdmissibilityTest`: green, 4 tests with integration tests skipped.
- `LmdbEstimateAuditHarnessTest#disconnectedProjectedIslandsUseJoinFinalRowsForCardinality+specificAssetThresholdPathDoesNotUseCartesianBypass`:
  green, 2 tests with integration tests skipped.
- `SketchBasedJoinEstimatorJoinOrderPlannerTest#planJoinOrderTreatsOuterBoundVarsAsVirtualConnectivitySeed`: green,
  1 test with integration tests skipped.
- `LmdbCascadesContextPropagationTest`: green, 4 tests with integration tests skipped.
- `.codex/skills/mvnf/test_mvnf.py`: green, 2 tests. Real `mvnf` command shows `-DskipITs` for Surefire selectors.
- `git diff --check`: green after normalizing trailing whitespace in `initial-evidence.txt`.

## Interfaces and Dependencies

No public RDF4J API changes. New helpers must be package-private or internal. New telemetry keys are internal optimizer
metrics: `optimizer.connectedEnumeration`, `optimizer.connectedComponentCount`,
`optimizer.disconnectedCandidateRejectedCount`, `optimizer.cartesianFallbackReason`, and
`optimizer.disallowedImplementationReason`.
