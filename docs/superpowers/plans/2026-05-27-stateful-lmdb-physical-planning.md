# Stateful LMDB Physical Planning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace scalar factor-cost planning with a stateful physical planner where cardinality, work, access path,
bound variables, finite domains, sketches, confidence, and diagnostics advance together for each candidate transition.

**Architecture:** A reorderable LMDB join island is planned by transitions over `PlanState`. A transition is the
conditioned estimate of applying one physical access-path candidate to the current prefix state. Cascades receives
complete physical alternatives for LMDB-owned islands; it no longer ranks scalar fragments that later need repair.

**Tech Stack:** Java 25, RDF4J query algebra, queryalgebra sketch planning, LMDB evaluation statistics, Cascades cost
vectors, existing sketch/finite-domain estimate classes, JUnit 5, Surefire/Failsafe, and `mvnf`.

---

This ExecPlan is a living document. Maintain `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` while the stateful planner work proceeds.

## Purpose / Big Picture

The current optimizer asks separate components to estimate the same plan from different partial views:
`SketchBasedJoinEstimator` estimates logical row flow, `LmdbEvaluationStatistics` estimates factor/access work,
`SketchJoinOrderPlanner` combines those values with prefix masks and scalar repairs, and Cascades ranks the resulting
`CostVector`. That loses the core fact needed for physical planning: every access path is conditioned on the current
prefix state.

After this plan, the planner asks one question:

`transition(prefixState, candidateFactor, candidateAccessPath) -> nextState`

The result carries the next cardinality, work, runtime bindings, sketches/surfaces, finite domains, confidence,
evidence, diagnostics, and the selected physical access. Cost ranking still exists, but it ranks complete transitions,
not independent factor estimates.

## Progress

- [x] Mapped current scalar seams in `JoinFactorCostModel`, `SketchJoinOrderPlanner`, `JoinPlanningState`,
  `JoinCostVector`, `LmdbEvaluationStatistics`, and LMDB Cascades rules.
- [x] Confirmed existing `BagEstimate`, `VariableEstimate`, `DistributionSketch`, and `FiniteRelationEstimate` can
  carry the stateful estimate payload instead of inventing a parallel sketch model first.
- [x] Add the stateful planning contracts and an adapter over current scalar factor costs.
- [x] Add focused tests for transition state propagation and diagnostics.
- [x] Wire `SketchJoinOrderPlanner` seed/extend paths through transition estimates.
- [x] Expose selected statement-pattern access paths as transition metadata.
- [ ] Branch statement-pattern access paths as costed transition alternatives.
- [ ] Move property-path access paths into the same transition alternative model.
- [ ] Add per-transition audit output for planned/actual rows and work.
- [ ] Retire scalar repair code that becomes unreachable after transition ownership.

## Surprises & Discoveries

- Observation: `JoinPlanningState` already exists, but it only stores factor masks, bound-var masks, order lineage, and
  deferred-filter masks. It is useful topology state, not the physical planning state needed here.

- Observation: The planner already has enough extension points for an adapter layer.
  Evidence: `SketchJoinOrderPlanner.factorCostEstimate(...)` passes a `JoinFactorCostModel.CostContext` containing the
  current bound mask, outer prefix rows, distinct lookup bindings, finite binding values, prefix factors, and tier.

- Observation: Existing cost payloads can preserve more evidence than the planner currently uses.
  Evidence: `JoinFactorCostModel.FactorCostEstimate` exposes an `EstimateVector`, physical access path metadata,
  lookup masks, string metrics, double metrics, and exact/repeated-invocation flags.

- Observation: The current DP planner can continue to rank with `JoinCostVector` during migration.
  Evidence: `ParetoJoinMemoPlanner` is generic over state type and only requires a `JoinCostVector` from each state.

- Observation: Statement access-path metadata is now available on the transition candidate itself.
  Evidence: `AccessPathCandidate` carries lookup/missing lookup masks and direct-lookup state, while
  `SketchJoinOrderPlanner` copies selected `FactorPhysicalEstimate` masks into the state transition.

## Decision Log

- Decision: Introduce the stateful planner as an adapter first.
  Rationale: This changes the architecture without a big-bang rewrite. Existing statement/path estimators can be
  wrapped as transitions, then replaced operator family by operator family.
  Date/Author: 2026-05-27 / Codex.

- Decision: Reuse existing sketch estimate classes in the first `PlanState`.
  Rationale: `BagEstimate`, `VariableEstimate`, `DistributionSketch`, and `FiniteRelationEstimate` already express row
  counts, variable summaries, finite domains, confidence, and evidence. Reusing them avoids duplicating math.
  Date/Author: 2026-05-27 / Codex.

- Decision: Keep `JoinCostVector` as the compatibility ranking vector while transitions become stateful.
  Rationale: Cascades and the Pareto memo already consume this vector. Replacing it is lower priority than stopping
  scalar factor estimates from driving access-path decisions.
  Date/Author: 2026-05-27 / Codex.

- Decision: LMDB connected islands remain single-owner.
  Rationale: Cascades should not preserve or invent alternative join trees inside a connected LMDB island after the
  stateful provider has planned that island.
  Date/Author: 2026-05-27 / Codex.

- Decision: Keep the first statement-pattern migration as a selected-access adapter before changing DP branching.
  Rationale: The planner can now persist physical access metadata in `PlanState` without destabilizing the existing
  join-order tests; later slices can replace scalar selection with multiple costed transition branches.
  Date/Author: 2026-05-27 / Codex.

## PlanState Contract

`PlanState` is package-internal planner state, not a public RDF4J API. It must carry:

- Duplicate-preserving rows and physical work rows.
- Current definitely bound runtime variables.
- Variable distinct estimates and optional sketches/surfaces.
- Finite domains and tuple correlations from `VALUES` and safe filter rewrites.
- Ordering and distinctness guarantees when known.
- Bag multiplicity and row-flow semantics.
- Confidence, q-error, evidence count, and evidence source.
- Access-path history and transition diagnostics.
- Optional/dependent edge context, without treating nullable right-only variables as required anchors.

The existing `JoinPlanningState` can remain as topology lineage inside `PlanState`. It should not be the only state
used for estimating a candidate.

## Transition Contract

The old question:

`cost(factor, currentlyBoundVars)`

The new question:

`transition(prefixState, candidateFactor, candidateAccessPath)`

Each transition returns:

- `nextState`: the updated `PlanState`.
- `stepCost`: local physical work and access metadata.
- `planCost`: accumulated ranking vector.
- `runtimeShape`: access mode, lookup masks, materialization, duplicate behavior.
- `evidence`: source, confidence, q-error, exactness, and fallback tier.
- `diagnostics`: metrics for explain output and audit rows.

## Access-Path Candidates

For each factor, enumerate physical alternatives before costing:

- Full scan.
- Predicate/object/subject prefix scan.
- Direct lookup.
- Bound nested lookup.
- Finite-domain lookup.
- Property-path traversal.
- Star/grouped scan.
- Optional/dependent probe.

The planner compares these alternatives as transitions from the same prefix state. Access path is not a later annotation
on a logical factor estimate.

## Evidence Ladder

Fallback order for a transition:

1. Exact finite evidence.
2. Complete page-walk or lookup evidence.
3. Sample/page-walk evidence.
4. Sketch intersection or join surface.
5. Surface product using variable distinct estimates.
6. Global cardinality fallback.

Every fallback marks confidence and q-error. Low-confidence fallback must not rank like exact finite or page-walk
evidence.

## Implementation Steps

- [ ] Add package-internal `PlanState`, `AccessPathCandidate`, and `TransitionEstimate` contracts in the sketch planner
  package.
- [ ] Add a `ScalarFactorTransitionEstimator` adapter that wraps current `FactorCostEstimate` and updates `PlanState`.
- [ ] Add unit tests proving bound variables, finite domains, access diagnostics, confidence, evidence, and cost vector
  fields survive one transition.
- [ ] Replace `SketchJoinOrderPlanner.seedPlan(...)` local scalar assembly with the adapter transition.
- [ ] Replace `SketchJoinOrderPlanner.extendPlan(...)` local scalar assembly with the adapter transition.
- [ ] Move statement-pattern access-path enumeration behind `AccessPathCandidate`.
- [ ] Move `ArbitraryLengthPath` and `ZeroLengthPath` planning behind property-path transition candidates.
- [ ] Add per-transition audit diagnostics, initially in explain metrics.
- [ ] Remove scalar repairs once equivalent transition evidence is in place.

## Validation

Use `mvnf` for tests.

Initial narrow tests:

- `PlanState` starts with outer bound variables and finite domains.
- Scalar adapter preserves `FactorCostEstimate` physical access path metadata.
- A statement transition updates bound runtime variables and keeps constants out of connectivity state.
- A finite-domain transition carries tuple domains forward for later factors.
- Confidence/evidence in `EstimateVector` reaches `JoinCostVector` and explain diagnostics.

Planner regressions:

- Connected AAS query remains phase1 connected-only with zero Cartesian work.
- Medical finite `FILTER IN` rewrite remains a movable finite factor.
- Property-path plans use LMDB property-path transition diagnostics, not generic full-scan winners.
- Deliberately disconnected queries still report explicit phase2 fallback.

## Idempotence / Recovery

The first adapter layer is additive. If a transition test fails, the old scalar estimator remains available behind the
adapter. Each operator migration should keep old metrics in explain output until replacement tests prove equivalent or
better behavior.

Do not remove scalar helper methods until:

- The corresponding transition path is wired.
- Focused tests cover the new path.
- Explain metrics still expose the fields used by current regressions.

## Outcomes & Retrospective

To be filled after implementation.
