# Compose optimizer sketch evidence profiles

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`,
`Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.
This document follows `.agent/PLANS.md`.

## Purpose / Big Picture

RDF4J's optimizer chooses query plans from row and work estimates. The current sketch propagation
work keeps some sketch objects alive, but scalar rows, variable surfaces, finite relations, and
tuple/set sketches are still stored in separate places. That lets a later join consume stale sketch
row mass, miss tuple evidence across planner bridges, or treat scalar fallback as high-quality
sketch evidence. This plan changes optimizer evidence into one composable profile that owns the
current cardinality and all distribution evidence. After the change, Cascades and the sketch join
planner can keep using the complete evidence view when choosing lower-work plans.

The behavior is visible through focused optimizer tests. The red tests must fail before production
changes because the current branch either drops tuple/set evidence, blindly copies stale sketches,
or has no way to tag scalar fallback quality. The same tests must pass after implementation.

## Progress

- [x] (2026-06-05 23:21+02:00) Ran root quick clean install successfully before edits.
- [x] (2026-06-05 23:24+02:00) Replaced prior completed ExecPlan with this evidence-profile composition plan.
- [x] (2026-06-05 23:57+02:00) Added red tests for stale sketch row mass, blind join sketch merging, bridge evidence loss, fallback quality, and cyclic bridge evidence.
- [x] (2026-06-06 00:27+02:00) Implemented evidence wrapper types and `BagEstimate.evidenceProfile()`.
- [x] (2026-06-06 00:35+02:00) Added `DistributionSketch.highQualityInnerProduct` so wrapper composition can tag FAST_AGMS scalar fallback as `SCALAR_FALLBACK`.
- [x] (2026-06-06 00:38+02:00) Added profile operator APIs and migrated `EstimateMath.innerJoin`, `filter`, and `project`.
- [x] (2026-06-06 00:40+02:00) Migrated Cascades `BindingProfile`, winner overlay, feedback, normalization, and provider filter paths to rebase through profiles.
- [x] (2026-06-06 00:41+02:00) Migrated `TuplePlanEstimate`, `PlanState`, filter transitions, and scalar factor adapter bridge paths to carry evidence profiles.
- [x] (2026-06-06 00:45+02:00) Ran copyright check, formatter, focused tests, and `core/queryalgebra/evaluation` module verification successfully.
- [x] (2026-06-06 01:20+02:00) Started follow-up implementation pass after review found remaining profile flattening and bridge propagation gaps.
- [x] (2026-06-06 01:20+02:00) Ran root quick clean install successfully before follow-up edits and saved compact evidence to `initial-evidence.txt`.
- [x] (2026-06-06 01:48+02:00) Converted `BagEstimate` from profile view to durable profile owner.
- [x] (2026-06-06 01:48+02:00) Split current and supporting sketch evidence in `EvidenceProfile`.
- [x] (2026-06-06 01:48+02:00) Tightened red tests for tuple/set evidence and stale current evidence.
- [x] (2026-06-06 01:48+02:00) Migrated operator transforms and Cascades/sketch-planner bridges to the durable profile.

## Surprises & Discoveries

- Observation: Root quick clean install passed before this implementation pass.
  Evidence: `maven-build.log` ended with `BUILD SUCCESS` and total time `54.432 s`.

- Observation: Product sketch scalar fallback and mixed tuple/FAST_AGMS fallback are different from
  high-quality sketch composition.
  Evidence: `PlanStateTransitionAdapterTest#mixedFastAgmsScalarFallbackIsQualityTagged` failed
  before the `highQualityInnerProduct` hook with `expected: <SCALAR_FALLBACK> but was:
  <VARIABLE_SKETCH>`.

- Observation: The final module verify passed after the profile and bridge migration.
  Evidence: `mvnf` reported `tests=1008, failures=0, errors=0, skipped=0`.

- Observation: A follow-up review showed that profile ownership is still incomplete.
  Evidence: `BagEstimate.evidenceProfile()` reconstructs a fresh profile from lossy maps, `TuplePlanEstimate`
  stores only per-variable stats, `BindingProfile.mergedWith()` selects one embedded profile, and several
  non-inner-join operators still round-trip through old `BagEstimate` constructors.

- Observation: The follow-up module verify passed after durable profile ownership and bridge migration.
  Evidence: `mvnf` reported `tests=1012, failures=0, errors=0, skipped=0`.

## Decision Log

- Decision: Treat this as Routine D / ExecPlan work.
  Rationale: This changes optimizer cost math, Cascades physical winner properties, and sketch
  join-order state. It changes externally observable plan/cost behavior and needs red tests first.
  Date/Author: 2026-06-05 / Codex.

- Decision: Introduce an `EvidenceProfile` wrapper instead of adding more raw sketch maps.
  Rationale: The root bug is split ownership. A valid estimate needs current scalar rows, variables,
  finite relations, sketches, provenance, and confidence to move together.
  Date/Author: 2026-06-05 / Codex.

- Decision: Keep raw `DistributionSketch` as a primitive and put planner quality decisions in wrapper code.
  Rationale: A sketch can answer math, but it cannot know whether its row mass is current after
  feedback, filter selection, or physical winner normalization.
  Date/Author: 2026-06-05 / Codex.

- Decision: Default `FAST_AGMS` remains supported; scalar fallback stays usable but must be tagged
  `SCALAR_FALLBACK`.
  Rationale: Pairwise `FAST_AGMS` cannot always produce exact higher-order product moments, but the
  planner must know when it is using fallback rather than real tuple/set composition.
  Date/Author: 2026-06-05 / Codex.

- Decision: Add `DistributionSketch.highQualityInnerProduct` while keeping `innerProduct` as the
  compatibility API.
  Rationale: Existing direct sketch callers still need a usable scalar answer, but evidence-profile
  costing needs to know when a result came from real sketch math versus wrapper-owned scalar
  fallback.
  Date/Author: 2026-06-06 / Codex.

## Outcomes & Retrospective

Implemented the evidence-profile wrapper layer and migrated the main optimizer bridges that were
dropping or stale-retaining sketches. `EstimateMath.innerJoin` now composes profiles and keeps only
transformed join evidence instead of blindly merging relation sketch maps. Filters and projections
use profile transformations. `BindingProfile` carries an `EvidenceProfile` through Cascades winner
boundaries, and row feedback/provider normalization rebases evidence with explicit finite-relation
preservation policy. The sketch planner now exposes tuple estimates as profiles and uses those
profiles when crossing `TuplePlanEstimate -> PlanState` and scalar transition adapter bridges.

No runtime speedup is claimed here. Verification is deterministic cost/plan evidence and module
tests only.

## Context and Orientation

The relevant module is `core/queryalgebra/evaluation`. `BagEstimate` represents estimated rows and
the variables carried by an intermediate query result. `VariableEstimate` represents one binding
name and may contain a `DistributionSketch`, a compact distribution summary used to estimate joins.
`FiniteRelationEstimate` is exact evidence for small binding sets. `EstimateMath` performs generic
bag math for joins, filters, projection, distinct, union, and other operators. `CascadesCostModel`
converts between logical groups, physical winners, and cost estimates. `BindingProfile` is the
physical property that lets a Cascades parent reuse a child winner's evidence. The sketch join
planner uses `TuplePlanEstimate`, `PlanState`, `StatePlan`, and `JoinCostVector` to compare join
orders. All of those row surfaces must describe the same post-transition estimate.

The known weak paths are:

1. Row normalization updates `BagEstimate.rows()` while retaining sketch objects whose
   `totalRows()` still describes the old row mass.
2. `EstimateMath.innerJoin()` merges relation sketch maps rather than composing a new evidence
   profile for the joined bag.
3. `TuplePlanEstimate` exposes only per-variable estimates, so tuple/set evidence cannot cross the
   sketch-planner bridge into `PlanState`.
4. Product sketch scalar fallback is returned as a plain `OptionalDouble`, so the planner cannot
   distinguish fallback from real sketch composition.
5. Non-inner-join operators still drop or stale-retain sketch evidence without a shared policy.

## Plan of Work

First add focused red tests in existing test classes. In `BagEstimateMathTest`, add tests for
rebased sketch row mass, relation sketch composition quality, stale side-relation removal, and
operator transformations. In `CascadesCostModelTest`, add tests proving feedback and physical winner
normalization keep one current evidence profile through chained physical joins. In
`PlanStateTransitionAdapterTest` and `SketchBasedJoinEstimatorJoinOrderPlannerTest`, add tests for
tuple/set evidence crossing `TuplePlanEstimate -> PlanState` and for cost-vector row consistency.

Then add the evidence wrapper layer in
`org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cost`. Create `EvidenceQuality`,
`EvidenceScalar`, `EvidenceComposition`, `RebaseMode`, `SketchEvidence`, and `EvidenceProfile`.
`EvidenceProfile` owns current rows, work rows, memory rows, confidence, source, variables, finite
relations, sketch evidence, and metrics. `SketchEvidence` owns the binding set, wrapped sketch,
current rows, current distinct rows, quality, and provenance. `SketchEvidence.estimateInnerProduct`
returns `EvidenceScalar`, never a bare `OptionalDouble`.

Next migrate `BagEstimate` so existing constructors remain source-compatible but delegate to
`EvidenceProfile`. Its accessors keep returning variables, finite relations, sketch maps, rows,
work rows, memory rows, confidence, source, and metrics. New code should prefer
`BagEstimate.evidenceProfile()` and profile transformation methods. `withRowsPreservingEvidence`
becomes a compatibility wrapper around `EvidenceProfile.rebaseRows`.

Then migrate `EstimateMath`. `innerJoin` calls `EvidenceProfile.composeInnerJoin` and uses its
chosen scalar. The priority order is exact finite relation, exact tuple/set sketch, valid composed
sketch, single-variable sketch, scalar fallback, and product-distinct heuristic. Joins must stop
blindly copying left/right relation sketches. Keep only transformed evidence, exact output
relations, current join-key evidence, and lower-quality composed output-set evidence when valid.
Filter, project, distinct, union, left join, difference, slice, and order call profile
transformation methods so evidence is either current, downgraded, or dropped with one policy.

Then migrate bridges. `BindingProfile` carries `EvidenceProfile` across Cascades winner
boundaries. `CascadesCostModel.inputWinnerEstimate` rebuilds physical inputs from the delivered
profile rather than rows plus binding names. `withFilteredInputBag`, `adjustJoinBagRows`,
`normalizeBagRows`, and `applyFeedback` call `EvidenceProfile.rebaseRows`. Provider-selected bags
keep provider evidence as current; generic scalar filters drop or downgrade sketches unless a
valid selected profile exists.

Finally migrate the sketch planner. Extend `TuplePlanEstimate` with `EvidenceProfile
evidenceProfile()`. `ScalarFactorTransitionEstimator.factorEstimate` builds factor bags from that
profile, including tuple/set sketches when available. `PlanState`, `StatePlan`, and
`JoinCostVector` use the same post-composition `EvidenceScalar.rows()`. Add assertions that those
surfaces remain aligned.

## Concrete Steps

Run commands from the repository root
`/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

Start from the clean quick install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Add red tests, then run focused selections:

    python3 .codex/skills/mvnf/scripts/mvnf.py BagEstimateMathTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesCostModelTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PlanStateTransitionAdapterTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorJoinOrderPlannerTest --retain-logs

After implementation, rerun the same focused tests, then format and verify:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs

Do not run Maven tests with `-am` or `-q`.

## Validation and Acceptance

Acceptance requires red test evidence before production edits and green evidence after production
edits. The new tests must show:

1. Feedback rebases bag rows and downstream joins use wrapper-owned current rows, not raw sketch
   `totalRows()`.
2. Physical winner boundaries carry one `EvidenceProfile` through chained physical joins.
3. Multi-variable joins consume tuple/set sketch evidence before product-distinct fallback.
4. Composed sketch evidence is lower quality than exact tuple sketch evidence, and scalar fallback
   is tagged `SCALAR_FALLBACK`.
5. Generic filters drop or downgrade stale sketch evidence; provider-selected filters keep current
   selected evidence.
6. A bridge/cycle query keeps tuple evidence through `TuplePlanEstimate`, `PlanState`, Cascades, and
   the next join.
7. `PlanState.estimate().rows()`, `StatePlan`, and `JoinCostVector` stay row-consistent.

The final module verification must pass for `core/queryalgebra/evaluation`. Do not claim runtime
speedup unless a benchmark is run; deterministic plan-shape and work-row regressions are sufficient
for this implementation.

## Idempotence and Recovery

All changes are additive until the red tests exist. If a test fails for a typo or setup error
instead of the intended missing behavior, fix the test and rerun before production edits. If a
production change is made before a red test proves the intended behavior gap, revert that production
change and restart at the red test step. The Maven commands are safe to rerun and use the
workspace-local `.m2_repo`.

## Artifacts and Notes

Initial evidence for this implementation pass:

    [INFO] BUILD SUCCESS
    [INFO] Total time:  54.432 s (Wall Clock)

Final verification for this implementation pass:

    ./checkCopyrightPresent.sh
    All files have valid copyright headers and SPDX lines.

    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    [INFO] BUILD SUCCESS

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    [mvnf] Summary: tests=1008, failures=0, errors=0, skipped=0, time=14.195s

## Interfaces and Dependencies

No new dependency will be added. Production changes stay in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cost`,
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades`,
and `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch`.

At completion these public package-private/internal types must exist in the cost package:

- `EvidenceQuality`
- `EvidenceScalar`
- `EvidenceComposition`
- `RebaseMode`
- `SketchEvidence`
- `EvidenceProfile`

Existing callers of `BagEstimate` and `BindingProfile` remain source-compatible through constructors
and accessors, but new optimizer code uses `EvidenceProfile` as the source of truth.
