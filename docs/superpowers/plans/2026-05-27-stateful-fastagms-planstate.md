# Carry FastAGMS Sketches In PlanState

This ExecPlan is a living document. It follows `.agent/PLANS.md` and must be kept current while the implementation
proceeds.

## Purpose / Big Picture

LMDB join planning currently computes FastAGMS sketch intersections, then collapses the result into scalar row and work
numbers before the physical transition state is updated. After this change, a planning prefix will carry the sketch
state itself, so later joins can combine the current prefix with candidate factors using FastAGMS math instead of
reconstructing evidence from factor lists or repairing scalar estimates. A user can see the change through focused
unit tests that prove `PlanState` carries sketch-backed tuple estimates, and through the sparse-prefix LMDB regression
showing cardinality estimates are produced from stateful transitions.

## Progress

- [x] (2026-05-27T13:56Z) Identified scalar loss point in `ScalarFactorTransitionEstimator`.
- [x] (2026-05-27T14:08Z) Added the red `PlanState` tuple-sketch carrier test.
- [x] (2026-05-27T14:23Z) Added sketch-bearing state fields and conversion helpers.
- [x] (2026-05-27T14:31Z) Routed sketch transition estimates through the planner.
- [x] (2026-05-27T14:47Z) Found sparse-prefix scalar overwrite and projection-island gaps.
- [x] (2026-05-27T15:02Z) Verified focused queryalgebra and LMDB sparse-prefix tests.
- [ ] Commit and push the slice.

## Surprises & Discoveries

- Observation: FastAGMS intersection math already exists in `SketchBasedJoinEstimator`, including shared-var
  intersections and prefix/factor surface merging.
  Evidence: `SketchBasedJoinEstimator.estimateSharedVarJoin` calls `intersectJoinOrderingSketches`, and
  `estimateJoinSurfaceRows` merges prefix stats before joining a candidate.
- Observation: The physical state path still erases sketch evidence.
  Evidence: `ScalarFactorTransitionEstimator.nextEstimate` creates a `BagEstimate` from scalar rows/work and passes
  `Map.of()` for variables.
- Observation: The sparse-prefix regression still failed after state was preserved because LMDB enrichment overwrote the
  stateful step rows with scalar factor re-estimates.
  Evidence: `LmdbSparsePrefixCostTest` reported prefix 4 actual rows `480` but planned rows `1052` while the selected
  DP vector had `finalRows=560`.
- Observation: The final sparse prefix failed after transparent projection flattening because a complete nested direct
  lookup was harmonic-blended with a loose scalar bridge product.
  Evidence: the red plan showed `plannedBoundJoinProductRawRows=3.1K`,
  `plannedBoundJoinProductPageWalkRows=545`, `plannedIndexAccessMode=directLookup`, and
  `plannedLookupComponents=[S, P]`.

## Decision Log

- Decision: Preserve `SketchBasedJoinEstimator.TuplePlanEstimate` inside `PlanState` rather than trying to encode
  FastAGMS sketches into `BagEstimate`.
  Rationale: `BagEstimate` is a generic optimizer-cost DTO; the FastAGMS summaries are package-local sketch planner
  evidence. Keeping them in `PlanState` avoids widening generic cost interfaces prematurely.
  Date/Author: 2026-05-27 / Codex.
- Decision: Keep the scalar adapter as a fallback path while adding a sketch-aware transition path.
  Rationale: The current planner still needs a safe path for tests and non-sketch cost models while the architecture is
  migrated incrementally.
  Date/Author: 2026-05-27 / Codex.
- Decision: For complete nested direct lookup transitions, use the access-path transition envelope instead of blending it
  with the scalar bound-product estimate.
  Rationale: the access path is the chosen physical transition under the current prefix; blending it with an
  independently reconstructed scalar bridge product reintroduces the old architecture loss.
  Date/Author: 2026-05-27 / Codex.

## Outcomes & Retrospective

Focused verification is green:

- `PlanStateTransitionAdapterTest`: 11 tests, 0 failures.
- `LmdbSparsePrefixCostTest`: 1 test, 0 failures with the tightened 1.5x sparse-prefix cardinality guard.
- `LmdbCascadesConnectedRuleAdmissibilityTest`: 4 tests, 0 failures.
- `LmdbCascadesContextPropagationTest`: 4 tests, 0 failures.

## Context and Orientation

The relevant module is `core/queryalgebra/evaluation`. `SketchBasedJoinEstimator` builds `TuplePlanEstimate` objects
for tuple expressions. A `TuplePlanEstimate` contains row estimates and per-variable `VarPlanStats`; each
`VarPlanStats` can hold a `FastAgmsBindingSummary`, which is the actual sketch evidence used for join intersections.

`SketchJoinOrderPlanner` performs dynamic-programming join ordering. It already computes `TuplePlanEstimate` values for
logical prefixes, but it also maintains a `PlanState` object intended to become the stateful physical-planning carrier.
`PlanState` currently stores `BagEstimate`, finite binding values, bound variables, access path history, and
diagnostics, but not the sketch-backed tuple estimate. `ScalarFactorTransitionEstimator` turns a factor-cost estimate
into a `BagEstimate`, losing the sketch summaries.

The term "FastAGMS" here means a frequency sketch that can estimate an inner product of two binding-frequency vectors.
When two factors join on `?x`, the inner product of the left and right sketches for `?x` estimates the joined row count.
For bridge joins, the prefix state must keep the sketch for the bridge variable after each transition so the next factor
can be combined against the already-conditioned prefix.

## Plan of Work

First add a red unit test in `PlanStateTransitionAdapterTest` that uses reflection to require an internal
`PlanState` tuple-estimate carrier. Reflection keeps the test compiling before the internal API exists. The test will
build a small sketch estimator from `StubSketchStatementSource`, create a `TuplePlanEstimate` for a statement pattern,
construct a `PlanState` with that tuple estimate, and assert that the state exposes sketch evidence for the runtime
join variable.

Then update `PlanState` to hold an optional `SketchBasedJoinEstimator.TuplePlanEstimate`. Add package-private accessors
that expose the tuple estimate and whether a variable has sketch evidence. Add an overload for `initial` and
`advance`/`withEstimateAndCost` paths that can carry a tuple estimate forward.

Next update `SketchBasedJoinEstimator.TuplePlanEstimate` with package-private helpers that convert its variable stats
to `VariableEstimate` and report sketch evidence for a variable. These helpers stay in the sketch package and do not
change RDF4J public APIs.

Finally route `SketchJoinOrderPlanner.factorTransitionState` through the sketch-bearing state by passing the
already-computed `TuplePlanEstimate` for the transition into `PlanState`. Keep `ScalarFactorTransitionEstimator` usable
for fallback transitions, but make planner-owned transitions preserve the sketch state.

## Concrete Steps

Run commands from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Add and verify the red test:

    python3 .codex/skills/mvnf/scripts/mvnf.py PlanStateTransitionAdapterTest#planStateCarriesTupleSketchEstimateForStatefulTransitions --retain-logs

Expected before implementation: one failing Surefire test stating that `PlanState` does not expose the sketch state
constructor or sketch evidence.

After implementation, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py PlanStateTransitionAdapterTest#planStateCarriesTupleSketchEstimateForStatefulTransitions --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PlanStateTransitionAdapterTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSparsePrefixCostTest --retain-logs

## Validation and Acceptance

The new unit test must fail before the implementation and pass after it. `PlanStateTransitionAdapterTest` must pass as a
class. `LmdbSparsePrefixCostTest` must remain green and continue to show finite cardinality and work estimates for all
sparse prefixes.

## Idempotence and Recovery

The implementation is additive around package-private planner state. If a test run fails, keep the failing reports and
rerun the same `mvnf` selector after the fix. Do not remove untracked logs or benchmark artifacts.

## Artifacts and Notes

Initial sparse containment evidence already exists in the thread: `LmdbSparsePrefixCostTest` passes but still shows
overestimates around 2.5x. This plan targets the remaining architectural loss of sketch evidence in physical state.

## Interfaces and Dependencies

No public RDF4J API changes. Internal package-private APIs to add:

- `PlanState.initial(BagEstimate, Set<String>, Map<String, Set<Value>>, SketchBasedJoinEstimator.TuplePlanEstimate)`.
- `PlanState.tupleEstimate()`.
- `PlanState.hasSketchEvidence(String)`.
- `SketchBasedJoinEstimator.TuplePlanEstimate.hasSketchEvidence(String)`.
- `SketchBasedJoinEstimator.TuplePlanEstimate.variableEstimates()`.
