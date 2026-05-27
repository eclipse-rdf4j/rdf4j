# Connected-Only SPARQL Join Planning

This ExecPlan is a living document. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` current while implementing. This document follows `.agent/PLANS.md`.

## Purpose / Big Picture

The LMDB/Cascades SPARQL planner currently enumerates disconnected inner-join prefixes and tries to make
them unattractive with Cartesian work penalties. That leaves room for a cheap-looking `rdf:type` class lookup
to be selected before it is connected to the rest of the query, producing exploding plans. After this work, a
basic graph pattern or other reorderable inner-join island that has a connected ordering will only enumerate
connected join plans. Cartesian joins remain visible and explicit only when the query is truly disconnected.

The observable outcome is that the AAS relationship/power query and the medical VALUES query no longer pick
plans with avoidable Cartesian prefixes, while deliberately disconnected queries still run through a clearly
annotated Cartesian fallback.

## Progress

- [x] (2026-05-27 07:12 +02:00) Root quick install passed before connected-only edits.
- [x] (2026-05-27 07:20 +02:00) ExecPlan created for connected-only join planning.
- [x] (2026-05-27 07:15 +02:00) Added a failing connected-only planner telemetry test.
- [x] (2026-05-27 07:35 +02:00) Added failing LMDB regressions for AAS, property paths, medical VALUES,
  disconnected fallback, and audit breadcrumb phase reporting.
- [x] (2026-05-27 07:42 +02:00) Implemented structural connectedness helper and candidate pruning.
- [x] (2026-05-27 07:48 +02:00) Wired connected-only outcomes into plan telemetry and Cascades/LMDB selection.
- [x] (2026-05-27 07:54 +02:00) Preserved FILTER-generated finite anchors as movable VALUES rewrites.
- [x] (2026-05-27 07:56 +02:00) Updated audit harness row/time-cap breadcrumbs with evaluation phase.
- [x] (2026-05-27 07:58 +02:00) Ran focused verification and root quick install.

## Surprises & Discoveries

- The current branch is `GH-0000-lmdb-predicate-guarantees`, so implementation can proceed without creating a
  new branch.
- The working tree already contains modified estimator and LMDB benchmark/test files from earlier work. Preserve
  these changes and do not revert unrelated dirty files.
- `SketchJoinOrderPlanner` already computes `runtimeVarMasks`, `connectivityVarMasks`, `bindingVarMasks`, and
  `factorRequiredBeforeVarMasks`. The first implementation target is `candidatesMask(StatePlan)`, because that
  is where all remaining legal factors are currently admitted before later heuristics and Cartesian penalties.
- Existing `cartesianWorkRows(...)` is a diagnostic and scoring input, not a structural guard. Connected-only
  planning must prune disconnected candidates before cost ranking.
- The first focused red test failed because selected plans do not expose the connected-only enumeration contract:
  `expected: <phase1_connected_only> but was: <null>`. The tiny three-factor shape already selected a connected
  order, so a stronger LMDB regression is still needed to guard against the AAS generic/cartesian winner.
- The medical Q9 red test showed the `?condCode IN (...)` rewrite was semantically valid but not selected:
  `finite-anchor:condCode[valid ... costing=dynamic]` lost to `original[baseline ...]`. That left the FILTER in
  place and prevented the generated VALUES relation from entering the connected join-order search space.
- The fix is to select semantically safe finite-domain rewrites before access-path costing, then let the
  connected planner place the generated `BindingSetAssignment` by normal cost ranking. Rejecting the rewrite
  because one local estimate looks worse is too early: it prevents later reorderability.

## Decision Log

- Decision: Use Routine D and this ExecPlan because the change affects optimizer search-space semantics across
  query algebra, Cascades, and LMDB tests.
  Rationale: A behavior-changing planner refactor needs restartable context and focused red tests before
  production code edits.
  Date/Author: 2026-05-27 / Codex.
- Decision: Implement connected-only planning in the existing left-deep dynamic-programming planner rather than
  replacing it with a full bushy DPhyp enumerator.
  Rationale: The current planner already enumerates left-deep append actions, and the user-provided rule allows
  either connected csg/cmp pairs or connected left-deep appends. This is the smallest root-cause change.
  Date/Author: 2026-05-27 / Codex.
- Decision: Runtime variable sharing is the only Phase 1 connectivity source. Deferred filters do not make two
  factors connected unless they are represented by a real shared runtime variable.
  Rationale: The requested definition is based on shared SPARQL runtime variables, not general predicates or
  cost heuristics.
  Date/Author: 2026-05-27 / Codex.
- Decision: Zero-variable single-row scalar factors may be applied in connected Phase 1, but zero-variable
  multi-row factors are explicit Cartesian multipliers and belong in fallback handling.
  Rationale: A multi-row no-variable relation changes bag multiplicity without any join key and should not be
  hidden as a connected join.
  Date/Author: 2026-05-27 / Codex.
- Decision: FILTER `IN` finite-domain rewrites that are semantically safe should be generated and selected as
  movable VALUES-style anchors before comparing local access-path costs.
  Rationale: The generated relation is a logical factor. Its best placement is a join-order decision, not a
  preselection decision at the original FILTER location.
  Date/Author: 2026-05-27 / Codex.

## Outcomes & Retrospective

Connected-only enumeration is implemented for the sketch join planner and exposed through planner telemetry.
The LMDB/Cascades path now keeps generic original-tree joins from winning connected reorderable islands, records
explicit disconnected fallback metadata, and keeps FILTER-generated finite-domain VALUES relations available for
later connected join-order placement.

The important bug for the latest medical Q9 case was not that VALUES cardinality itself was wrong; it was that
the finite anchor was rejected too early as a local guarantee option, so the generated VALUES relation never
became a movable factor. The regression now asserts that `finite-anchor:condCode` is selected, the rendered
optimized query contains `VALUES ?condCode { "DX-200" "DX-201" "DX-202" }`, and the original FILTER is gone.

## Context and Orientation

The main planner is `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchJoinOrderPlanner.java`.
It receives a list of reorderable tuple-expression factors for one inner-join island. A factor is a statement
pattern, VALUES assignment, path expression, or similar tuple expression that can be placed in a left-deep join
order inside that island. The planner uses bit masks for factors and variables. `runtimeVarMasks[i]` should
represent real variables that factor `i` can bind or consume at runtime. Constant-valued internal variables must
not create connectivity. Generated property-path variables count only when they carry runtime bindings.

`SketchJoinOrderPlanner.candidatesMask(StatePlan)` currently starts from every remaining factor, applies
factor-action legality, and leaves disconnected candidates available for later scoring. `cartesianWorkRows(...)`
then adds work for disconnected choices. The new invariant is stricter: when a connected completion exists,
disconnected candidates are not candidates at all.

Cascades physical selection and LMDB statistics must consume this planner result rather than letting the generic
original-tree Join implementation win for the same connected island. The relevant integration points are
`CascadesCostModel` and `LmdbEvaluationStatistics`.

## Plan of Work

First add red tests in query algebra evaluation around `SketchBasedJoinEstimatorJoinOrderPlannerTest` or a new
neighbor test class in the same package. These tests should build small join islands directly and inspect the
planner summary/metrics so they fail because disconnected candidates are currently accepted. Add LMDB regressions
for the AAS relationship/power query, property-path query, medical Q9 VALUES shape, and a deliberately
disconnected query that must take explicit fallback.

Then add a package-private connectivity helper in the sketch package. It should compute connected candidate
masks from the existing factor masks and initially definite bound vars. A candidate is connected when its runtime
var mask intersects the current definite bound prefix. At an empty standalone prefix, any non-zero-var factor may
seed a connected component. With definite outer vars, prefer seeded starts that can complete the island; only use
an unseeded start when no seeded complete connected plan exists. Single-row zero-var scalar factors are always
eligible; multi-row zero-var factors are not Phase 1 connected factors.

Modify `candidatesMask(StatePlan)` and any greedy candidate path to call the helper before cost ranking. Track
how many otherwise-legal disconnected candidates were rejected. Phase 1 final plans should have
`cartesianWorkRows == 0` except for explicit scalar cases that do not multiply rows. If a complete connected
ordering is impossible, split factors into connected components, optimize each component connected-only, combine
the component plans with explicit Cartesian joins, and annotate the fallback reason.

Finally wire telemetry through the produced plan. Add `optimizer.connectedEnumeration`,
`optimizer.connectedComponentCount`, `optimizer.disconnectedCandidateRejectedCount`, and
`optimizer.cartesianFallbackReason` to plan attributes where the join planner already records logical exploration
metadata. Adjust Cascades/LMDB selection so a connected sketch-provider winner cannot be beaten by a generic
original Join implementation that preserves an avoidable Cartesian order.

Update `LmdbEstimateAuditHarness` to emit query id, piece id, algebra kind, and phase before evaluation starts.
Keep existing row/time caps, but make the first failure identify the exact query/piece even if evaluation blocks
before yielding a row.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

1. Root install baseline:

       mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

   Expected outcome: `BUILD SUCCESS`.

2. Add red tests and run them before production changes:

       mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -DskipITs -Dtest=SketchBasedJoinEstimatorJoinOrderPlannerTest test
       mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbEstimateAuditHarnessTest,LmdbPropertyPathEstimateTest test

   Expected red outcome before implementation: at least one new test fails because avoidable Cartesian work or
   disconnected candidate enumeration is still present.

3. Implement connected-only candidate enumeration and telemetry.

4. Re-run the same focused tests. Expected outcome: all new focused tests pass.

5. Broaden verification:

       mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -DskipITs -Dtest=SketchBasedJoinEstimatorJoinOrderPlannerTest test
       mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=LmdbEstimateAuditHarnessTest,LmdbPropertyPathEstimateTest,LmdbCascadesContextPropagationTest test
       mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

6. Run formatting and copyright checks before final handoff:

       cd scripts && ./checkCopyrightPresent.sh
       mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources

   Do not use `-q` because repository instructions forbid quiet test/build reporting for this workflow.

## Validation and Acceptance

The change is accepted when:

- Connected planner tests prove disconnected appends are rejected when a connected complete ordering exists.
- AAS LMDB regression shows no avoidable positive `plannedCostCartesianWorkRows`, no generic original-tree
  Cartesian Join winner for the connected island, and no unanchored `?rel a RelationshipElement` prefix chosen
  before a connected anchor.
- Medical Q9 regression shows the generated `?condCode` VALUES relation does not cause the `med:code` lookup to
  explode.
- A deliberately disconnected query reports explicit Phase 2 Cartesian fallback with component telemetry.
- Focused query algebra evaluation and LMDB tests pass, followed by a root quick install.

## Idempotence and Recovery

The test commands are safe to repeat. If a test run overwrites Surefire reports, preserve the important failure
snippet in the chat handoff and in this plan's discoveries before re-running. Do not revert unrelated dirty files.
If connected-only pruning blocks all plans for a valid connected island, inspect the runtime variable masks first;
the most likely cause is a missing generated path var or an internal constant var being counted incorrectly.

## Artifacts and Notes

Root quick install before connected-only edits:

    BUILD SUCCESS
    Total time: 27.480 s (Wall Clock)

Focused red evidence before the finite-anchor fix:

    Command: mvn -Dmaven.repo.local=.m2_repo -o -pl core/sail/lmdb -DskipITs -Dtest=ThemeQueryBenchmarkSmokeIT#medicalRecordsQueryNineBenchmarkLifecycleKeepsConditionCodeValuesAnchorMovable test
    Result: Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    Snippet: optimizer.guaranteeOptionCandidates=original[baseline ... selected=original];
             finite-anchor:condCode[valid ... costing=dynamic]

Focused green evidence after the finite-anchor fix:

    Command: mvn -Dmaven.repo.local=.m2_repo -o -pl core/sail/lmdb -DskipITs -Dtest=ThemeQueryBenchmarkSmokeIT#medicalRecordsQueryNineBenchmarkLifecycleKeepsConditionCodeValuesAnchorMovable,LmdbEstimateAuditHarnessTest#rowCapFailureCarriesQueryPieceKindAndPhase test
    Result: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

Connected planner focused green:

    Command: mvn -Dmaven.repo.local=.m2_repo -o -pl core/queryalgebra/evaluation -DskipITs -Dtest=SketchBasedJoinEstimatorJoinOrderPlannerTest#dynamicProgrammingPrunesDisconnectedAppendWhenConnectedCompletionExists test
    Result: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

Root quick install after focused verification:

    BUILD SUCCESS
    Total time: 26.858 s (Wall Clock)

## Interfaces and Dependencies

No public RDF4J API changes. Add only package-private helper types or private methods in the sketch planner
package. Do not add dependencies. New telemetry keys are internal string annotations on existing plan metadata.
