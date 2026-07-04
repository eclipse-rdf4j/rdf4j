# Standard-rule parity and real SERVICE/GROUP/TripleRef estimates for the LMDB planner

This ExecPlan is a living document maintained per `.agent/PLANS.md`. It is Phase 2 of the roadmap whose Phase 1 is `.agent/execplans/GH-0000-opaque-factor-join-enumeration.md`.

## Purpose / Big Picture

The generic Cascades rule catalog (`RuleRegistry.standardLogicalRules()` in `core/queryalgebra/evaluation/.../optimizer/cascades/RuleRegistry.java`) contains seventeen logical rewrites that the LMDB store never registered: SERVICE co-planning (push VALUES/filters into the remote query, project only needed variables, expand variable endpoints), subquery inlining and cleanup, UNION common prefix/suffix factoring and subsumed-branch elimination, GROUP BY-as-DISTINCT, negation pushdowns, positive-closure decomposition and graph restriction. LMDB also costed three operators with base-class placeholders: a constant-endpoint SERVICE was estimated at ~1–100 rows (more selective than a local index lookup, pulling the remote call to the *front* of plans), GROUP BY passed through as its input size, and RDF-star triple references used per-component constants instead of store data. After this change, LMDB plans get the full standard rewrite catalog (with an escape hatch, `rdf4j.optimizer.lmdb.cascades.standardLogicalRuleParity=false`), remote SERVICE calls are never costed as selective, keyed aggregation collapses below its input, keyless aggregation is exactly one row, and triple references use the matching statement count as a store-backed proxy. Observable in `query.explain(...)` estimates and in plan shapes for queries using SERVICE/subselects/UNION.

## Progress

- [x] (2026-07-04) Red: `LmdbRuleRegistryCoverageTest` — `tests=1, failures=1`, listing all seventeen missing rule ids; `LmdbOpaqueOperatorCardinalityTest` — `tests=4, failures=4` (SERVICE estimated at 102.0; keyed GROUP 120=input; keyless GROUP 120≠1; TripleRef 10 vs statement proxy 30).
- [x] (2026-07-04) Green: seventeen rules registered in `LmdbCascadesRuleProvider.rules` behind `STANDARD_RULE_PARITY_PROPERTY` (default on); `LmdbCardinalityCalculator` gains `meet(Service)` floor (1 000 rows for constant endpoints), `meet(Group)` (keyless → 1.0; keyed → clamp(√input × keys, [1, input])), and `getCardinality(TripleRef)` statement-pattern proxy via `statementPatternCardinalitySource`. Both test classes pass.
- [x] (2026-07-04) Regression sweep: cascades suite selection green (only the two known-reds); full `core/sail/lmdb` module = exact known-red baseline per class; LMDB W3C compliance initially regressed one test (`tests()[170]`, sq08) — root-caused to the generic `hasSubqueryModifier` guard, fixed in `StructuralCascadesRules` (Extension chain walk), reproducer `LmdbSubqueryAggregateScopeTest` red→green; compliance then exactly at baseline (6 + 10) and `core/queryalgebra/evaluation` module green.
- [x] (2026-07-04) Format, header check, committed as a2df217c28.

## Outcomes & Retrospective (final)

Phase 2 shipped: seventeen standard logical rules registered for LMDB with a parity contract test preventing future drift, and three placeholder estimates replaced (SERVICE floor 1 000, GROUP BY collapse, TripleRef statement proxy). The registration surfaced and fixed a real generic-rule correctness bug (aggregate subquery inlining, W3C sq08) that no other store could have caught — evidence that compliance suites must gate every rule-catalog change on this branch. Phase 3's LATERAL work is blocked on a product decision: LATERAL already exists on `develop` (116 commits ahead); importing it means merging develop into this branch, which invalidates the current known-red baselines and needs its own session. Next roadmap phase: rich estimates through the memo (scalar-collapse fix) plus omni witness probe cost.

## Surprises & Discoveries

- Observation: the missing rules were pure registration drift — `LmdbCascadesRuleProvider.rules` re-lists standard rules by hand instead of composing `standardLogicalRules()` minus exclusions. The coverage test now pins parity so drift is impossible; composing the registries structurally is a possible follow-up refactor.
- Observation: registering the parity batch surfaced a latent correctness bug in the GENERIC rule catalog. W3C sq08 ("Subquery with aggregate") failed on LMDB (`tests()[170]`, a global `max` became a correlated per-binding `max` with duplicate rows) because `StructuralCascadesRules.hasSubqueryModifier` only inspected the direct child of the subquery projection — but aggregate subqueries compile to `Projection(Extension(Group(...)))`, so the `Extension` masked the `Group` and `InlineModifierFreeSubqueryRule`/`DropUnusedSubqueryVarsRule` treated an aggregate subquery as modifier-free and dissolved its scope. The bug was invisible before because no cascades-enabled store ran the W3C suites with these rules registered.
  Evidence: `LmdbSubqueryAggregateScopeTest` (new reproducer) red with `[x=a;max="2"], [x=a;max="2"], [x=b;max="3"]` instead of one row; green after `hasSubqueryModifier` walks the `Extension` chain. Compliance flag-off run (`standardLogicalRuleParity=false`) reproduced the baseline 6 failures, isolating the batch.

## Decision Log

- Decision: one batch kill-switch property for the whole parity block rather than seventeen per-rule switches.
  Rationale: the rules are already exercised by the generic-registry test suites; per-rule switches add surface without evidence of need. The cascades budget and promises bound search-space growth.
  Date/Author: 2026-07-04 / Claude
- Decision: GROUP BY keyed-collapse heuristic is `clamp(√input × keyCount, 1, input)`.
  Rationale: no per-key distinct-count statistics are wired into the calculator yet; square-root collapse is the classic stats-free assumption, strictly below input (fixing the pass-through), and Phase 4 (rich estimates) can replace it with sketch-backed distinct counts.
  Date/Author: 2026-07-04 / Claude
- Decision: TripleRef proxies through the statement count for the same (s, p, o).
  Rationale: LMDB has no dedicated triple-term index; asserted-statement count is the only store-backed signal and is exact when all three components are bound.
  Date/Author: 2026-07-04 / Claude

## Outcomes & Retrospective

(pending regression results)

## Context and Orientation

`LmdbCascadesRuleProvider.rules(EvaluationStatistics)` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java, ~line 125) assembles the LMDB rule registry by hand. `RuleRegistry.standardLogicalRules()` is the canonical generic catalog. Cardinality estimation lives in `LmdbEvaluationStatistics.LmdbCardinalityCalculator` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java, ~line 11530), extending the base `EvaluationStatistics.CardinalityCalculator` whose SERVICE/TripleRef heuristics carry literal TODOs (core/queryalgebra/evaluation/.../impl/EvaluationStatistics.java:497–570). Tests: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRuleRegistryCoverageTest.java` (registry parity contract with a documented-exclusions set, currently empty) and `LmdbOpaqueOperatorCardinalityTest.java` (store-backed estimate assertions using `store.getBackingStore().getEvaluationStatistics()`).

## Validation and Acceptance

Run `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRuleRegistryCoverageTest --module core/sail/lmdb` and `... LmdbOpaqueOperatorCardinalityTest ...`: both red before the change (evidence above), green after. Then the cascades suite selection, the full module run (no per-class failure count may exceed the known-red baseline recorded in the Phase 1 ExecPlan), and the LMDB W3C compliance classes in `compliance/sparql` (failsafe; `-Dit.test='LmdbSPARQLComplianceTest,LmdbSPARQL11QueryComplianceTest' -Dtest=NoSuchUnitTest`), whose failures must not exceed the pre-existing set (10 + 6).

## Idempotence and Recovery

`rdf4j.optimizer.lmdb.cascades.standardLogicalRuleParity=false` removes the newly registered rules; the estimate overrides have no flag (they replace placeholder TODO heuristics and are strictly more defensible; revert the commit if they must be undone).

## Interfaces and Dependencies

No new dependencies. New property constant `LmdbCascadesRuleProvider.STANDARD_RULE_PARITY_PROPERTY`; new overrides `LmdbCardinalityCalculator.meet(Service)`, `meet(Group)`, `getCardinality(TripleRef)`.
