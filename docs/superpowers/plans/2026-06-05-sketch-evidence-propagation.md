# Preserve sketch evidence through query planning

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`,
`Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.
This document follows `.agent/PLANS.md`.

## Purpose / Big Picture

RDF4J's query optimizer estimates how many rows a query operator will produce. This work keeps
distribution evidence, meaning sketches and exact finite binding relations, attached to those row
estimates as they move through joins, filters, feedback corrections, Cascades winner boundaries,
and physical/logical planner bridges. After the change, downstream operators can keep using
sketches for costing instead of falling back to scalar independence estimates after an intermediate
operator.

The behavior is visible through focused optimizer tests. The red tests must fail before production
changes because sketch evidence is currently stored in some paths but erased or made unusable in
others. The same tests must pass after the implementation.

## Progress

- [x] (2026-06-05 16:51+02:00) Created this ExecPlan.
- [x] (2026-06-05 16:50+02:00) Ran root quick clean install successfully.
- [x] (2026-06-05 18:18+02:00) Added red tests for chained sketch consumption and row normalization.
- [x] (2026-06-05 18:23+02:00) Implemented evidence-preserving row normalization.
- [x] (2026-06-05 18:29+02:00) Implemented consumable composed and variable-set sketch evidence.
- [x] (2026-06-05 18:30+02:00) Extended Cascades binding profiles across winner boundaries.
- [x] (2026-06-05 18:34+02:00) Aligned PlanState, StatePlan, and JoinCostVector estimates.
- [x] (2026-06-05 18:40+02:00) Ran focused tests, formatting, and module verification.

## Surprises & Discoveries

- Observation: Root quick clean install passed before edits.
  Evidence: `maven-build.log` ended with `BUILD SUCCESS` and total time `28.657 s`.

- Observation: A joined default `FAST_AGMS` product sketch was not consumable by a later join.
  Evidence: `PlanStateTransitionAdapterTest#fastAgmsProductSketchRemainsConsumableAfterJoin`
  failed because the product sketch returned `OptionalDouble.empty()`.

- Observation: Feedback-corrected estimates kept stale variable rows after the bag rows changed.
  Evidence: `CascadesCostModelTest#feedbackCorrectionPreservesJoinedSketchProfile` failed with
  expected bound rows `12.0` but actual bound rows `17.0`.

- Observation: Provider-selected filter bag sketches were overwritten by filtered input math.
  Evidence: `CascadesCostModelTest#providerFilterKeepsSelectedBagSketchProfile` failed because the
  delivered sketch was `null`.

## Decision Log

- Decision: Treat this as Routine D / ExecPlan work.
  Rationale: The change touches optimizer cost math, Cascades physical winners, and sketch join
  planning. It changes externally observable plan/cost behavior and needs red tests first.
  Date/Author: 2026-06-05 / Codex.

- Decision: Keep default `FAST_AGMS` support and add no dependency.
  Rationale: The planner currently defaults to `FAST_AGMS`; a fix that only works for tuple
  sketches would leave the production path weak.
  Date/Author: 2026-06-05 / Codex.

- Decision: Use exact delegate math first for product sketches, then row-mass scalar fallback.
  Rationale: Pairwise `FAST_AGMS` summaries do not contain exact third-order moments. The fallback
  keeps the evidence consumable and explicit without claiming exactness.
  Date/Author: 2026-06-05 / Codex.

## Outcomes & Retrospective

Implemented the sketch evidence propagation path. `BagEstimate` now carries variable-set sketch
relations alongside per-variable sketches and finite relations. `BindingProfile` carries those
relations over Cascades winner boundaries. `EstimateMath.innerJoin()` now checks exact finite
relations, variable-set sketches, composed sketches, single-variable sketches, then scalar fallback.
Provider-selected filter bags keep their supplied sketches, and feedback-corrected bags normalize
variable row surfaces without erasing valid sketches.

Validation passed with the focused optimizer/sketch suites and full `core/queryalgebra/evaluation`
module verify. The remaining caveat is mathematical: default pairwise `FAST_AGMS` cannot produce
exact third-order product moments, so composed product sketches use exact delegate math where
available and a row-mass scalar fallback otherwise.

## Context and Orientation

The relevant module is `core/queryalgebra/evaluation`. `BagEstimate` represents estimated rows and
the variables carried by an intermediate query result. `VariableEstimate` represents one binding
name inside the bag and may contain a `DistributionSketch`, which is compact distribution evidence
used to estimate joins. `FiniteRelationEstimate` is exact evidence for a small set of bindings.
`EstimateMath` performs generic bag math, including joins and filters. `CascadesCostModel` converts
between logical groups, physical winner plans, and cost estimates. `BindingProfile` is the profile
carried by physical properties so a Cascades parent can reuse a child winner's estimate. The sketch
join planner in `org.eclipse.rdf4j.query.algebra.evaluation.sketch` has `PlanState`, `StatePlan`,
and `JoinCostVector` values that must stay row-consistent.

The known weak paths are:

1. `EstimateMath.innerJoin()` can create composed/product sketches, but default `FAST_AGMS`
   composition must remain consumable by later joins.
2. Generic join math only uses sketch evidence for single-variable joins unless exact finite
   relations are available.
3. Filtering, feedback corrections, and row normalization can scale rows while dropping sketches or
   leaving variable rows inconsistent with the bag rows.
4. Cascades physical winner boundaries must preserve full binding evidence, not reconstruct only
   scalar variables.

## Plan of Work

First add focused tests in the existing test classes. The tests must cover an `A join B join C`
case where the sketch produced by the first join changes the row estimate for the second join under
default `FAST_AGMS`, a multi-variable bridge/cycle case where tuple or variable-set evidence wins
over product-distinct fallback, a provider-filter case where the selected bag's sketch survives, and
a feedback-correction case where bag rows and variable rows are normalized without erasing valid
sketches.

Then implement a small evidence-preserving helper around `BagEstimate` and `BindingProfile`.
The helper must preserve exact finite relation evidence when it remains exact, preserve provider-
supplied selected-bag sketches, preserve sketches across row-only feedback corrections with updated
scalar row bounds, and mark unusable composed evidence as fallback rather than silently using it.

Next update `EstimateMath.innerJoin()` so the join row-estimation priority is exact finite relation,
then tuple or variable-set sketch evidence, then consumable composed sketch evidence, then existing
single-variable sketch evidence, and finally current product-distinct math. Product sketch support
must work for default `FAST_AGMS`; if exact product composition cannot be answered for a downstream
join, tuple/set evidence must be preferred and the fallback must be explicit in tests.

Finally update `CascadesCostModel` and the sketch join planner bridge paths so child winner
profiles, filter/provider estimates, feedback-corrected estimates, `PlanState`, `StatePlan`, and
`JoinCostVector` all expose the same post-transition row estimate and usable evidence.

## Concrete Steps

Run commands from the repository root
`/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Start from the clean quick install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Add and run red tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py BagEstimateMathTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesCostModelTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorJoinOrderPlannerTest --retain-logs

After implementation, rerun the same focused tests, then format and verify:

    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs

Do not run Maven tests with `-am` or `-q`.

## Validation and Acceptance

Acceptance requires red test evidence before production edits and green evidence after production
edits. The new tests must show:

1. A sketch produced by `A join B` is consumed by the later join with `C`.
2. Multi-variable bridge evidence is used before scalar product-distinct fallback.
3. Provider-supplied filtered bags retain their sketches across Cascades costing.
4. Feedback corrections keep variable row estimates consistent with corrected bag rows.
5. `PlanState.estimate().rows()`, `StatePlan`, and `JoinCostVector` remain aligned.

The final module verification must pass for `core/queryalgebra/evaluation`.

## Idempotence and Recovery

All changes are additive until the red tests exist. If a test fails for a typo or setup error instead
of the intended missing behavior, fix the test and rerun before production edits. If a production
change is made before a red test proves the intended behavior gap, revert that production change and
restart at the red test step. The Maven commands are safe to rerun and use the workspace-local
`.m2_repo`.

## Artifacts and Notes

Initial evidence:

    [INFO] RDF4J: Query algebra - evaluation .................. SUCCESS [  2.674 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  28.657 s (Wall Clock)

Final evidence:

    [mvnf] Summary: tests=1000, failures=0, errors=0, skipped=0, time=27.618s

## Interfaces and Dependencies

No new dependency will be added. Production changes should stay in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cost`,
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades`,
and `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch`.

Expected interface-level additions are a small evidence-preserving row-normalization helper for
`BagEstimate` or `EstimateMath`, a binding-profile representation capable of carrying variable-set
sketch evidence, and composed sketch behavior that remains consumable by downstream join costing
under default `FAST_AGMS`.
