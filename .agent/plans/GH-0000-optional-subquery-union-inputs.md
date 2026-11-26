# Preserve UNION inputs inside OPTIONAL subqueries

This ExecPlan follows `.agent/PLANS.md`. It records the follow-up to published commit `e4ef2b06ff4df5c6505ceac9b42088e41f592001` on `HASMAC-AS/rdf4j:GH-0000-union-scope-inputs`. The initial upstream base was `eb9b76be580eed238009c7a359e9a928148bd368`. Upstream subsequently advanced to `40f45d96faa300bc8f2432b72d02363ca5ace853`; its only content changes add test annotations in three unrelated modules. That main revision is incorporated in the follow-up.

## Purpose / Big Picture

Make inner UNION join ordering use the bindings that the actual enclosing evaluator supplies. The reported query contains a subselect inside OPTIONAL, arbitrary-length paths, and inner UNIONs. The user's larger query is private, so reproduce these shapes with finite parsed queries and explicit result bags, runtime UNION inputs, and triple-source requests.

A path endpoint produced inside a subselect can be a valid input to an inner UNION. An outer-only binding is not automatically a correlation into an independent SELECT. The change must preserve that distinction, including when a UNION remains scope-marked.

## Progress

- [x] Inspect the published branch and optimizer pipeline.
- [x] Reproduce missing inputs before production changes.
- [x] Correct scope proof and operand planning.
- [x] Protect expression inputs and assignment targets.
- [x] Verify complete evaluation module and current-main integration.
- [ ] [in_progress] User PR creation and repository review.

The implementation and local verification are complete. This document is the review checkpoint included with the branch; the final handoff supplies the published commit, PR creation link, complete changed files, and verified patch.

## Context and Orientation

Repository workspace: `/workspace/scratch/5c2cf927d55d/rdf4j-review`.

Production changes are confined to these files beneath `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/`:

- `UnionScopeChangeOptimizer.java` collects possible incoming names and proves whether scope removal preserves expressions and result multiplicities.
- `QueryJoinOptimizer.java` chooses and rebuilds join hierarchies, then recursively plans their operands.

`StandardQueryOptimizerPipeline` runs UNION scope optimization before normalization, projection removal, and join ordering. `QueryAlgebraBindingAnalysis` already models evaluator input transitions, including the entry mappings retained by independently evaluated operands. It is unchanged by this follow-up.

Four regression classes under `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/` provide the focused acceptance suite: `UnionScopeOptionalSubqueryInputTest`, `QueryJoinOptimizerOptionalEntryPrefixTest`, `QueryJoinOptimizerUnionExtensionPriorityTest`, and the expanded `UnionScopeOptionalContextTest`.

## Surprises & Discoveries

1. The general UNION scope proof rejected DISTINCT, REDUCED, ORDER, SLICE, and GROUP ancestors even when crossing only their tuple-input edges. Pure branches bypassed this rejection, which hid the gap in the earlier tests.
2. Normalization could copy a marked containing Join's scope flag onto a replacement UNION after the inner UNION had been cleared. The first grouped fixture allowed later factoring to mask the problem; a broad common statement made the missing runtime input and unreachable lookup observable.
3. QJO recursively optimized operands while they still occupied their original positions. Its physical selected prefix and QABA's source-tree input context could disagree. After rebuilding `Join(A, Join(B, scopedUnion))`, the UNION legitimately receives A's invocation input but not B's newly produced row. A flat prefix cannot represent that distinction reliably.
4. Branch-local BINDs caused the entire UNION to be classified as a priority extension operand. It consequently ran before its path supplier, including when the UNION retained its scope or was wrapped in FILTER.
5. Declared names are not guaranteed bindings. A nullable FILTER read, nested OPTIONAL fallback, or VALUES declaration inside EXISTS can observe a newly introduced invocation mapping. The complete-read and target guards prevent these changes.
6. The first full suite caught an overly conservative guard for a safe two-factor marked EXISTS filter. With only two factors an independent operand receives the same entry input in either order. The guard was narrowed, and the existing planning assertion was retained unchanged.

## Decision Log

- Use test-first reproduction with preserved reports. Sixteen cases fail on the published branch; two further retained/wrapped assignment cases fail on the preliminary correction. Date: 2026-11-11.
- Traverse modifier ancestors only through their tuple argument. Add no aggregate targets, ordering-expression names, or modifier outputs as input suppliers. Do not admit result modifiers inside UNION branches on this basis. Date: 2026-11-11.
- Expand a certified pure region through containing exact Joins only while the highest cleared root itself has an exact ordinary Join parent. Keep direct OPTIONAL-boundary proofs separate. Date: 2026-11-11.
- Defer ordinary operand recursion until the final enclosing join hierarchy is installed and QABA invalidated. Reuse actual algebra contexts instead of duplicating evaluator rules or promoting physical cost names to semantic guarantees. Date: 2026-11-11.
- Recognize branch-local assignments at a bare UNION, including through exact FILTER wrappers. Other wrappers retain their previous extension-priority classification. Date: 2026-11-11.
- Treat complete expression reads, evaluated BIND targets, nullable FILTER reads, and nested OPTIONAL names as input-sensitive. Unknown expression input contracts veto reordering. Retain the independent two-factor exception and all three-or-more-factor safeguards. Date: 2026-11-11.
- Keep global ArbitraryLengthPath/ZeroLengthPath purity and repeatability classifications unchanged. This work corrects planning and proven scope removal; it does not assume unrestricted path join-distributivity. Date: 2026-11-11.

## Plan of Work Implemented

In `UnionScopeChangeOptimizer.candidateInputs`, add exact Distinct, Reduced, Order, Slice, and Group tuple-argument transitions. In the pure-relational path, find the largest eligible containing Join region, clear its certified scope flags once, invalidate derived analysis, and retry the nearest retained ancestor UNION when needed. Existing identity-memoized structural proof remains in use.

In `QueryJoinOptimizer.meet(Join)`, save ordinary operands after final ordering/VALUES placement, rebuild and install their hierarchy, invalidate QABA, and only then visit each saved operand. Each visit starts with the enclosing Join's entry cost prefix. New semantic guarantees come from the actual rebuilt tree. This adds one operand list and does not introduce a second whole-query optimization pass.

The priority classifier strips only exact Filter wrappers before checking for a Union boundary. The input-sensitivity collector uses `FilterBindingScope.conditionBindingReads` for executed BIND expressions and FILTER conditions, including implicit VALUES correlations in value subqueries. FILTER removes only QABA-proven guaranteed argument outputs from its sensitive reads. Nested OPTIONALs conservatively contribute declared and referenced names. The generic guard does not block a two-factor independent candidate whose input is unchanged in either position.

## Concrete Steps

Use the task-local JDK 25/Maven wrapper by setting:

    PATH=/workspace/scratch/5c2cf927d55d/build_support/bin:$PATH

Focused selection:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs -- -DskipITs -Dtest=UnionScopeOptionalSubqueryInputTest,UnionScopeOptionalContextTest,QueryJoinOptimizerOptionalEntryPrefixTest,QueryJoinOptimizerUnionExtensionPriorityTest

Complete module verification:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs

The mvnf skill installs the root quick profile before verification. All Maven invocations use the workspace `.m2_repo` and start offline. No test invocation uses `-am` or `-q`. Builds are serialized.

Before publication, run the copyright checker from `scripts`, run the evaluation-module formatter, check whitespace, verify all tested Java bytes are unchanged, and inspect the remote branch lease. Publish the follow-up with the previous feature head and incorporated main head as parents; do not overwrite intervening remote work.

## Validation and Acceptance

The final complete evaluation-module run reports:

    Tests run: 4220, Failures: 0, Errors: 0, Skipped: 1

The skip is the pre-existing disabled sketch accuracy test `SketchBasedJoinEstimatorTupleSketchJoinAccuracyTest.randomTombstoneDeletesKeepJoinEstimatesAlignedWithTruthDataJoin`.

All 35 focused invocations pass; 30 are new in this follow-up. They cover six subquery modifier forms in both path/UNION source orders, pure controls, extra grouping, two three-factor UNION positions, retained/wrapped local assignments, duplicate and unbound result bags, independent subqueries, OPTIONAL left-only inputs, volatile conditions, assignment overwrite/removal, nullable FILTER reads, nested OPTIONAL fallback, and implicit EXISTS/VALUES correlation. Positive planning cases use default EvaluationStatistics and record actual incoming mappings and storage requests.

The earlier broader optimizer selection passed 390 invocations before the final priority refinement. The first full run exposed one safe-reordering assertion; the final full run resolves it without weakening the test. Exact unsafe alternative trees are evaluated in two negative controls to prove the counterexamples rather than merely assuming an input contract.

## Outcomes & Retrospective

The remaining issue required both scope-proof and scheduling fixes. A scope marker alone does not determine all incoming bindings, and clearing a marker does not help if a priority rule schedules the UNION before its supplier. Planning from the rebuilt tree resolves the actual input transition while preserving independent scopes.

The complete evaluation source and tests were hashed before incorporating upstream's three unrelated annotation-only test changes. Those 721 Java files remain byte-identical after that merge. No production dependency or build configuration changed upstream, so the completed module verification remains applicable to the integrated change.

The unavailable private query has not been executed. The finite reproductions establish the reported nesting and binding-loss mechanisms, with explicit bag-preservation controls. Unsupported expression contracts retain conservative ordering.

## Idempotence and Recovery

All baseline and final reports are preserved outside Maven's overwritten target directories. Failed proofs do not clear candidate scope flags. Unrelated and untracked files are retained. Remote updates use the expected previous feature SHA; a mismatch requires inspecting new work before retrying. User authorization to create and push this branch persists from the preceding request; PR creation is left to the supplied user link.

## Artifacts and Notes

Evidence directories under `../evidence` include `optional-subquery-refined-before`, `optional-subquery-final-controls-before`, `optional-subquery-priority-before`, `optional-subquery-first-full`, `optional-subquery-final-focused`, and `optional-subquery-final-full`. `initial-evidence.txt` contains concise first-failure excerpts. `optional-subquery-final-summary.txt` contains per-class final totals without environment properties.

The deliverable archive contains complete changed files and a patch against the incorporated main revision. Verify the patch in a temporary Git index and compare the reconstructed tree with the published tree. Verify every archive member and its checksums before handing off download links.

## Interfaces and Dependencies

No public API, dependency, evaluator input contract, or shared path-safety classification is added or changed. Existing QABA contexts, structural safety proofs, and complete expression-read analysis are reused.

Revision: consolidated the living investigation into the final review checkpoint, including executed before/after evidence, the corrected grouped fixture, the two-factor guard refinement, and the unrelated current-main integration. PR creation and repository review are the remaining user-owned step.
