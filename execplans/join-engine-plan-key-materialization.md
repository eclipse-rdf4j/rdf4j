# Introduce JoinPlanKey and Late Materialization in Join Engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan follows `PLANS.md` in the repository root and must remain aligned with that guidance.

## Purpose / Big Picture

After this change, the join engine memo no longer depends on storing `TupleExpr` trees for every ranked alternative. The engine stores stable plan keys and numeric costs, then materializes the winning `TupleExpr` only at selection time. This reduces accidental coupling between memo artifacts and mutable query-model nodes while preserving existing planner/rule behavior.

## Progress

- [x] (2026-02-27 03:28+01:00) Confirmed current engine/memo/candidate architecture and detached-clone baseline.
- [x] (2026-02-27 03:29+01:00) Wrote ExecPlan scaffold and scope for JoinPlanKey + late materialization.
- [x] (2026-02-27 03:30+01:00) Implemented `JoinPlanKey` value object and variant-signature helper.
- [x] (2026-02-27 03:30+01:00) Refactored `JoinPlan` and engine add-plan path to key+cost memo entries.
- [x] (2026-02-27 03:30+01:00) Added winner late-materialization path (`JoinTreeBuilder` + rule replay for rules variant).
- [x] (2026-02-27 03:31+01:00) Ran focused verifies for joinengine + trace suites.
- [x] (2026-02-27 03:31+01:00) Ran full `core/queryalgebra/evaluation` verify via `mvnf`.
- [x] (2026-02-27 03:34+01:00) Removed remaining `JoinPlan` candidate retention; plan entries are now key+descriptor+cost only.
- [x] (2026-02-27 03:34+01:00) Added `JoinOrderCandidate.parseSignature(...)` and switched winner materialization to signature-index reconstruction.
- [x] (2026-02-27 03:35+01:00) Re-ran focused verifies (same selection pre/post) and full module verify after key-only refactor.
- [x] (2026-02-27 03:37+01:00) Cached parsed order indices in `JoinPlanKey` and switched materialization to key indices (no per-selection parse).
- [x] (2026-02-27 03:37+01:00) Added winner shape assertions in adaptive fallback tests to prove signature-based materialization fidelity.
- [x] (2026-02-27 03:38+01:00) Re-ran focused verifies and full module verify after key-index caching/assertion additions.
- [x] (2026-02-27 03:39+01:00) Updated `JoinTreeBuilder` to consume candidate signature indices against `JoinRegion` atoms, removing dependence on candidate `TupleExpr` order payload.
- [x] (2026-02-27 03:40+01:00) Re-ran focused verifies and full module verify after signature-index builder transition.
- [x] (2026-02-27 03:44+01:00) Removed `JoinOrderCandidate` `TupleExpr` order retention and exposed stable `getOrderIndices()` API.
- [x] (2026-02-27 03:44+01:00) Updated `JoinPlanKey`/`JoinTreeBuilder` to consume candidate indices directly (no signature reparse on internal paths).
- [x] (2026-02-27 03:44+01:00) Updated legacy planner ordering test to assert through signature/indices.
- [x] (2026-02-27 03:44+01:00) Re-ran root quick install, focused joinengine verifies, and full module verify after index-only candidate transition.
- [x] (2026-02-27 03:46+01:00) Added `JoinOrderCandidateTest` for invalid base-order operand rejection and index/signature stability.
- [x] (2026-02-27 03:46+01:00) Re-ran root quick install, focused joinengine verifies, and full module verify after new regression coverage.
- [x] (2026-02-27 03:50+01:00) Hardened `JoinOrderCandidate` permutation invariants (`of(...)`/`fromIndices(...)` full-size and duplicate guards).
- [x] (2026-02-27 03:50+01:00) Extended `JoinOrderCandidateTest` with incomplete-order and duplicate-index rejection regressions.
- [x] (2026-02-27 03:51+01:00) Re-ran root quick install, focused joinengine verifies, and full module verify after invariant hardening.
- [x] (2026-02-27 03:55+01:00) Re-ran root quick install and focused joinengine verify (`31` tests) while finalizing evidence handoff.
- [x] (2026-02-27 03:58+01:00) Added failing `QueryJoinOptimizerDetachedJoinConstructionTest` proving legacy `JoinVisitor` helper methods mutated parent pointers on live operands.
- [x] (2026-02-27 03:59+01:00) Updated legacy `QueryJoinOptimizer.JoinVisitor` join-tree builders to clone operands (`buildJoinHierarchy`, `buildRightJoinTree`, `initialMergeJoinOptimization`).
- [x] (2026-02-27 04:01+01:00) Updated legacy reorder tests to assert structural equivalence instead of object identity for detached join trees.
- [x] (2026-02-27 04:02+01:00) Re-ran focused legacy + joinengine suites and full `core/queryalgebra/evaluation` verify after legacy detachment hardening.
- [x] (2026-02-27 04:11+01:00) Revalidated detached-construction and legacy optimizer focused verifies (`QueryJoinOptimizerDetachedJoinConstructionTest`, `QueryJoinOptimizerTest`, `QueryJoinOptimizerEmptyStatisticsTest`) after final test stabilization.
- [x] (2026-02-27 04:13+01:00) Re-ran root quick install and full `core/queryalgebra/evaluation` verify; suite remained green (`Tests run: 778`).
- [x] (2026-02-27 04:14+01:00) Ran canonical `mvnf` workflow for `core/queryalgebra/evaluation` (clean + root install + module verify); all green (`Tests run: 778`).

## Surprises & Discoveries

- Observation: `JoinMemo` DP state is already numeric/order-based and only materializes temporary trees for estimation; the main remaining expression retention is `JoinPlan.expr` in ranking memo.
  Evidence: `JoinMemo#SubsetPlan`, `JoinMemo#appendAtom`, and `DefaultJoinOptimizationEngine#addPlan`.
- Observation: `DefaultJoinOptimizationEngineAdaptiveFallbackTest` had an object-identity-based cost discriminator that was invalid under detached/multi-materialization behavior; structural predicate matching fixed the test invariant.
  Evidence: pre-fix failure `expected legacy-greedy but was reverse`; post-fix class verify green.
- Observation: after first pass, memo still indirectly retained live atoms through `JoinPlan -> JoinOrderCandidate -> List<TupleExpr>`, so the representation goal needed one more cut.
  Evidence: `JoinPlan` constructor still accepted `JoinOrderCandidate` before second pass.
- Observation: signature parsing per selected plan was functionally correct but unnecessary churn once the key already owns a stable signature.
  Evidence: `materializeChosenPlan(...)` parsed signature on every selection before key-index caching.
- Observation: `JoinTreeBuilder` still consumed `candidate.getOrder()` despite stable signature availability, leaving a stale dependency on candidate `TupleExpr` payload.
  Evidence: pre-change `JoinTreeBuilder.build(...)` iterated `candidate.getOrder()`.
- Observation: `JoinOrderCandidate.of(...)` could encode `-1` indices if planners returned atoms outside the region base order, which is invalid for signature/index materialization.
  Evidence: pre-fix implementation appended `index` without asserting successful identity match.
- Observation: module test count increased from 773 to 776 after adding three `JoinOrderCandidateTest` rejection cases; all joinengine suites remained green.
  Evidence: `mvnf` final verify summary (`Tests run: 776, Failures: 0, Errors: 0, Skipped: 0`).
- Observation: legacy `QueryJoinOptimizer` tests encoded object-identity assumptions for innermost join operands, which no longer hold once detached cloning is enforced.
  Evidence: first post-patch module verify failed with six `isSameAs` assertion failures across `QueryJoinOptimizerTest` and `QueryJoinOptimizerEmptyStatisticsTest`.
- Observation: value-constrained filter patterns carry predicate constants in filter conditions, not in `StatementPattern` predicate values.
  Evidence: `NullPointerException` in `QueryJoinOptimizerTest#getPredicateValue` after switching to structural assertions.

## Decision Log

- Decision: Scope Gap 1 to engine-level memo representation and winner materialization, without redesigning DP enumeration APIs in this pass.
  Rationale: Highest risk reduction per line changed; keeps existing planner portfolio and trace schema stable.
  Date/Author: 2026-02-27 / Codex
- Decision: Keep telemetry signature semantics unchanged by deriving descriptor signature from `JoinPlanKey` (`baseline => [..]`, `rules => [..]+rules`).
  Rationale: avoids trace-consumer regressions and preserves plan-ID determinism expectations in existing tests.
  Date/Author: 2026-02-27 / Codex
- Decision: Re-apply rewrite rules only for final winner materialization when winner variant is `rules`.
  Rationale: avoids storing/retaining expression trees in memo while still returning the selected rewrite variant.
  Date/Author: 2026-02-27 / Codex
- Decision: Materialize winner order from key signature (`[i,j,k]`) using `JoinOrderCandidate.parseSignature(...)` + `fromIndices(...)`.
  Rationale: keeps memo entries free of `TupleExpr`/candidate references while reusing existing safe tree builder.
  Date/Author: 2026-02-27 / Codex
- Decision: cache parsed signature indices inside `JoinPlanKey` and materialize from `JoinPlanKey#getOrderIndices()`.
  Rationale: avoids repeated parse and keeps signature-derived reconstruction deterministic.
  Date/Author: 2026-02-27 / Codex
- Decision: switch `JoinTreeBuilder` operand reconstruction to signature-index lookup against `JoinRegion` atoms.
  Rationale: keeps plan materialization aligned with plan skeleton (`signature`) rather than candidate payload objects.
  Date/Author: 2026-02-27 / Codex
- Decision: remove `TupleExpr` order payload from `JoinOrderCandidate` and keep only stable order indices/signature.
  Rationale: closes remaining alternative-plan object retention and aligns candidate representation with memo key materialization.
  Date/Author: 2026-02-27 / Codex
- Decision: enforce `JoinOrderCandidate.of(...)` identity-match validity with hard failure when an operand is missing from `baseOrder`.
  Rationale: prevents latent invalid signatures (`-1`) from propagating into materialization.
  Date/Author: 2026-02-27 / Codex
- Decision: enforce detached-operand construction in legacy `QueryJoinOptimizer.JoinVisitor` join-tree helpers by cloning operands before synthetic `Join` creation.
  Rationale: closes remaining parent-pointer mutation gap outside the join-engine path and aligns legacy helper behavior with single-parent invariant.
  Date/Author: 2026-02-27 / Codex
- Decision: keep reorder-order assertions identity-based where output list order is unchanged, but assert join-tree structure semantically for innermost joins under detached-clone construction.
  Rationale: preserves selection-order coverage while removing brittle object-identity coupling to join-tree materialization internals.
  Date/Author: 2026-02-27 / Codex

## Outcomes & Retrospective

`JoinPlan` memo entries now carry only `JoinPlanKey` + descriptor + cost. `DefaultJoinOptimizationEngine` no longer returns a previously stored candidate tree and no longer depends on candidate retention in memo entries; it reconstructs order from key signature and materializes at selection boundary, replaying rewrite rules when the chosen variant is `rules`. This keeps plan ranking representation detached from mutable query-model tree instances while preserving score/risk telemetry and planner selection behavior.

Validation passed:

- focused verify (`DefaultJoinOptimizationEngineAdaptiveFallbackTest`, `DetachedJoinPlanSafetyTest`, `JoinMemoDeterminismTest`, `QueryJoinOptimizerJoinEngineTraceTest`) with `BUILD SUCCESS`.
- full module verify (`python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs --stream`) with `Tests run: 776, Failures: 0, Errors: 0, Skipped: 0`.
- adaptive-fallback tests now assert both selected planner and resulting winner left predicate (`urn:p1`/`urn:p2`) to confirm key-derived materialization shape.
- `JoinTreeBuilder` now uses signature indices + region atoms for ordered join args and rejects invalid indices early (`IllegalArgumentException`).
- Legacy `QueryJoinOptimizer.JoinVisitor` join-tree helpers now clone operands before synthetic join assembly (`buildJoinHierarchy`, `buildRightJoinTree`, `initialMergeJoinOptimization`), preventing transient parent-pointer mutation on live operands.
- `JoinOrderCandidate` now stores only planner + order indices + signature, and legacy ordering assertions are index-driven.
- `JoinOrderCandidateTest` now locks two invariants:
  - `of(...)` rejects operands not found in base order.
  - `fromIndices(...)` exposes stable indices/signature (`[1,0]`).
- `JoinOrderCandidateTest` now also rejects incomplete and duplicate index permutations, making index-signature materialization inputs strict permutations.
- Added `QueryJoinOptimizerDetachedJoinConstructionTest` to lock legacy helper detachment invariants (`buildJoinHierarchy` and `buildRightJoinTree` keep live operands parentless).
- Post-refactor validation remained green:
  - root quick install: `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200`
  - focused verify (`QueryJoinOptimizerTest`, `QueryJoinOptimizerEmptyStatisticsTest`, `QueryJoinOptimizerDetachedJoinConstructionTest`): `BUILD SUCCESS`, `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`
  - focused joinengine/trace verify (`QueryJoinOptimizerJoinEngineTraceTest`, `DefaultJoinOptimizationEngineAdaptiveFallbackTest`, `DetachedJoinPlanSafetyTest`, `JoinMemoDeterminismTest`, `LegacyGreedyPlannerOrderingTest`, `JoinOrderCandidateTest`, `QueryJoinOptimizerDetachedJoinConstructionTest`): `BUILD SUCCESS`, `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`
  - full module verify: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify`, `Tests run: 778, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`

## Context and Orientation

`DefaultJoinOptimizationEngine` currently builds candidate trees, computes cost, and stores each alternative as `JoinPlan(expr, candidate, descriptor, cost)`. `JoinPlan` is then ranked in `JoinMemo`; the winner’s stored `expr` is returned as `OptimizationResult.optimized`. The new direction is to keep memo entries as a stable identity (`planner`, `order signature`, `variant`) plus cost and candidate metadata, then re-materialize only the winning tree.

Key files:

- `core/queryalgebra/evaluation/src/main/java/.../joinengine/DefaultJoinOptimizationEngine.java`
- `core/queryalgebra/evaluation/src/main/java/.../joinengine/JoinPlan.java`
- `core/queryalgebra/evaluation/src/main/java/.../joinengine/PlanDescriptor.java`
- `core/queryalgebra/evaluation/src/main/java/.../joinengine/JoinTreeBuilder.java`

## Plan of Work

Add a small immutable `JoinPlanKey` with planner, base order signature, and variant (`baseline` or `rules`). Let `JoinPlanKey` derive descriptor signature (`[...]+rules` for rewritten variants) and deterministic plan ID hash inputs. Update `JoinPlan` to hold `JoinPlanKey` instead of a materialized `TupleExpr`.

In `DefaultJoinOptimizationEngine`, keep current costing flow but stop storing expressions in memo entries. For each candidate, compute costs from transient trees; add `JoinPlan` entries keyed by `JoinPlanKey`. After ranking and adaptive fallback, materialize only the selected plan by rebuilding from `JoinTreeBuilder` and applying rewrite rules if selected variant is `rules`.

Keep trace-event payload semantics unchanged (`planner`, `signature`, `planId`, `variant`, score/risk). `signature` continues to report descriptor signature (baseline or `+rules`) so telemetry compatibility is preserved.

## Concrete Steps

Working directory: repository root.

1. Edit engine key/model files using small patches.
2. Run mandatory quick install:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

3. Run focused joinengine test classes first.
4. Run module verify via `mvnf`.

Expected focused outcome:

- `BUILD SUCCESS`
- no failures in joinengine tests.

## Validation and Acceptance

Acceptance criteria:

- Optimizer still emits valid `PLAN_SELECTED`, `COSTED`, and pruning events with stable plan IDs/signatures.
- `core/queryalgebra/evaluation` test suite remains green.
- Detached-plan safety tests remain green.

## Idempotence and Recovery

Edits are additive/refactor-only; rerunning commands is safe. If any regression appears in trace/selection tests, revert only the affected file-level patch and rerun focused class verifies before broad verify.

## Artifacts and Notes

Will append short evidence snippets after implementation runs.

## Interfaces and Dependencies

New value type to add:

- `joinengine.JoinPlanKey`
  - fields: `planner`, `orderSignature`, `variant`
  - behavior: derive descriptor signature and deterministic ID seed

Updated interfaces/classes:

- `joinengine.JoinPlan`: replace stored `TupleExpr expr` with `JoinPlanKey key`
- `joinengine.DefaultJoinOptimizationEngine`: add winner materialization step from `JoinPlanKey` and `JoinOrderCandidate`

## Revision Notes

- 2026-02-27 03:31+01:00: Marked first-pass implementation/verification milestones complete and recorded outcomes after green focused + module verifies.
- 2026-02-27 03:35+01:00: Extended implementation to remove remaining candidate retention from memo entries and switched winner materialization to signature-index reconstruction; revalidated focused + module verifies.
- 2026-02-27 03:38+01:00: Added key-level order-index caching and explicit winner-shape assertions; revalidated focused + module verifies.
- 2026-02-27 03:40+01:00: Transitioned `JoinTreeBuilder` from candidate order payload to signature-index reconstruction and revalidated focused + module verifies.
- 2026-02-27 03:44+01:00: Completed candidate representation transition to index/signature-only (`JoinOrderCandidate`), updated dependent tests, added identity-match guard in `of(...)`, and revalidated via root quick install + focused + module verifies.
- 2026-02-27 03:46+01:00: Added targeted `JoinOrderCandidateTest` regression coverage and revalidated with root quick install + focused joinengine verify + full module verify (`mvnf`).
- 2026-02-27 03:51+01:00: Hardened `JoinOrderCandidate` to require full permutations (size parity + duplicate rejection), added three regression tests, and revalidated with root quick install + focused joinengine verify + full module verify (`mvnf`).
- 2026-02-27 03:55+01:00: Re-ran root quick install + focused joinengine verification during execplan handoff finalization (`Tests run: 31`, all green).
- 2026-02-27 04:02+01:00: Added legacy detached-construction regression test, hardened legacy `JoinVisitor` builders with operand cloning, updated legacy tests to structural assertions, and revalidated via focused suites + full module verify (`Tests run: 778`).
