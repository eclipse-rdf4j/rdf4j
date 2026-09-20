# Connectedness and optimizer-pipeline audit

This document records the complete test-first review of the optimizer and evaluation changes on branch `GH-5906-query-join-connected-order`. The initial adversarial tests were published in `a7eb18ba25` before the failing rerun. The initial class run had 14 invocations, 10 failures, and 0 errors. The final class has 33 tests, 0 failures, and 0 errors. Diagnostic logs and evidence files remain untracked; this document is the substantive audit that belongs with the fix.

## User-visible purpose

The optimizer may reorder joins to reduce physical statement work, but a reordered plan must return the same SPARQL binding rows as the legal evaluation. This audit covers connectedness through computed `BIND` values, the left-only output scope of `MINUS`, nullable values, filters, `EXISTS`, `LATERAL`, `SERVICE`, and the separate LMDB planning path. It also checks that cost facts used by the optimizer are conservative and that evaluation resources close on failure.

## Review of every original adversarial invocation

The initial report is `logs/mvnf/20260920-165043-verify.log` and is preserved in `initial-evidence.txt`.

1. `startingSelectionChecksConnectedPairsOutsideTheThreeCheapestCandidates` failed before the fix with counted work `20271` versus legal `371`. This was a confirmed physical cost bug: the starting selection considered only the first three cheap candidates and could miss a connected pair later in the list. Candidate selection now evaluates connected alternatives outside that bounded prefix. The test passes in the final 33-test class.

2. `positiveLengthPathExportsItsIntermediateBindingWhenTheEvaluatorDoes` passed before the fix and remains a control. Positive-length path intermediate bindings are retained and the final result is correct.

3. `parsedPositiveLengthSequencePathKeepsItsConsumerSemantics` passed before the fix and remains a parsed path control. A consumer of the intermediate path binding still receives the correct row.

4. `differenceRightVariablesAreIncorrectlyExposedAsOuterCostConnections` failed as a plan-only assertion. The right-only MINUS variable was exposed to outer cost connectedness and made its consumer appear connected. The Difference variable collector now visits the left output for outer cost facts; the test passes.

5. `differenceRightVariablesMustNotChangeResultsWhenAJoinIsReordered` failed with an empty optimized result where the legal result had one row. The optimizer reordered an outer consumer using a variable that exists only in the MINUS RHS. The left-only collector plus scope-aware evaluation repair this case; the test passes.

6. `parsedMinusRightVariablesMustNotChangeResultsWhenAJoinIsReordered` failed with the same empty-result shape through the parser. The standard optimizer now preserves the parsed scope and the test passes.

7. `parsedMinusWithDisjointChildrenRejectsAnUnrelatedIncomingBinding` failed because an unrelated sibling binding restricted a MINUS RHS whose children have disjoint domains. The Difference comparison domain now comes from actual left and right outputs rather than incoming bindings, and ordinary joins evaluate both sides independently. The test passes.

8. `standardPipelineKeepsMinusRightVariablesOutOfOuterBindings` failed through `DefaultEvaluationStrategy.optimize`, proving that a local optimizer-only repair was insufficient. The shared prepared Difference and join evaluation path now preserves the standard pipeline result; the test passes.

9. `computedBindOutputCanBeMissedAsAConnectedCostOpportunity`, parameterized for `STR(?a)` and `isIRI(?a)`, failed with direct optimized work `2000` versus legal `1001`. This was a confirmed cost opportunity. The optimizer now propagates guaranteed value kinds through sequential extensions, projections, unions, joins, filters, and optional boundaries. `STR` is guaranteed only for supported statically known input kinds; `isIRI` is guaranteed only when its operand is guaranteed. Both parameterizations pass their complete result and work assertions.

10. `directBindOutputRemainsConnectedAsTheControlCase` passed before the fix and remains a control for a direct variable BIND connection.

11. `standardPipelineCanMissAStatementProducedTotalBindCostOpportunity`, parameterized for `STR(?a)` and `isIRI(?a)`, failed with full-pipeline work `2001` versus legal `1002`. The original STR work premise was too broad: a statement pattern can bind a blank node or another value unsupported by `STR`, so no universal guarantee is valid. The final source preserves the statement-produced STR result bag and documents this conservative cost classification in `standardPipelinePreservesStatementProducedBindResults`. The statement-produced `isIRI` workload remains a valid total-function case in `standardPipelineUsesStatementProducedIsIriAsTotalBindCostOpportunity`, and its work assertion passes. This is a rejected premise for the untyped STR performance claim, not a production workaround for a semantic failure.

12. `statementRunUsesTheCostOfTheWholeBoundPrefixInsteadOfAnOldPair` passed with equal measured work (`602` current and legal) and equal binding bags. The exact-order hypothesis is closed; the test retains semantic and work invariants without claiming a different tie-break.

The ten initial failures are therefore classified as seven confirmed semantic/physical repairs (starting selection, the Difference plan trigger, three reordered MINUS cases, the parsed disjoint case, and the standard MINUS pipeline), two direct computed-value cost repairs, and one reclassified untyped STR cost premise. The four original controls remain green. No initial failure was muted or weakened to make the class pass.

## Additional regressions and controls

The later tests cover the interactions that determine whether the repairs generalize:

* `minusRightEvaluationUsesOnlySharedOutputBindings` and `directMinusInitialBindingsDoNotCreateComparisonDomain` prove that incoming bindings do not manufacture a MINUS comparison name.
* `directMinusPreservesIncomingBindingRequiredByRhsService`, `correlatedExistsPreservesOuterBindingsNeededByMinusRight`, and `lateralMinusKeepsDeclaredRightInputBindings` prove that ordinary nested-loop isolation does not erase declared SERVICE, EXISTS, or LATERAL inputs.
* `nullableMinusLeftDoesNotTreatAnOuterBindingAsItsOutput`, `nullableValuesRemainJoinCompatibleWhenDifferenceIsNested`, and `nullableValuesDoNotGuaranteeNestedExtensionResults` cover OPTIONAL and UNDEF rows.
* `minusScopeBarrierSurvivesTransparentFilterWrapper`, `outerFilterMustNotBeRelocatedIntoMinusRightArg`, and `valuesInMinusLeftMustNotBeInlinedIntoMinusRightScope` cover transparent wrappers, filter relocation, and parser/inliner scope changes.
* `extensionOverwriteCannotAuthorizeHashJoinForNullableOutput` is the runtime assurance regression: a same-name extension expression errors and clears a join key, so inherited assurance would choose a hash join and drop a compatible unbound row. The model now removes every extension target from conservative assured names.
* `independentJoinClosesLeftWhenRightEvaluationFails` observes the RHS exception during consumption and verifies that the left iteration closes. `independentJoinDefersRightEvaluationUntilItHasALeftRow` verifies that an empty left does not evaluate the RHS.
* `typePredicatesRequireGuaranteedOperandsAndKnownStrInputs`, `guaranteedValueKindsSurviveSequentialBindsAndProjection`, `unionOfKnownAndUnsupportedKindsDoesNotGuaranteeStr`, and `optionalValueKindsDoNotBecomeGuaranteed` cover total predicates, supported STR inputs, UNION, projection aliases, and OPTIONAL.

The existing `BindingSetAssignmentInlinerTest.testOptimize_Minus` and its full 13-test class pass. The broad hypothesis that the inliner leaks same-group VALUES into a parsed MINUS RHS is refuted by the existing scope-change behavior and receives no production change.

## Root-cause repairs

`QueryJoinOptimizer` now scans connected candidates beyond the bounded primary list and derives Difference output and cost variables from the left result. Its guaranteed-value analysis is recursive and conservative; it does not treat arbitrary unary or binary functions as total.

`FilterOptimizer.FilterRelocator` relocates an outer filter into the left Difference operand only. Copying it into the RHS was unsound when the condition referenced a left-only variable.

`DefaultEvaluationStrategy`, `MinusQueryEvaluationStep`, and `SPARQLMinusIteration` carry an explicit output comparison domain. The public legacy constructor remains available for callers that supply an established context. The prepared Difference path compares only the intersection of actual left and right output names, while preserving incoming bindings for RHS expressions that explicitly require them.

`JoinQueryEvaluationStep` identifies Difference below transparent same-scope wrappers and selects `IndependentJoinIteration` when nullable keys make a hash join unsafe. SERVICE and LATERAL are scope boundaries. The independent iterator evaluates the left first, materializes the RHS once only after a left row exists, performs SPARQL-compatible binding checks, and closes the left resource when RHS evaluation or materialization fails. This fallback is deliberately correctness-first and can perform O(left rows times right rows) compatibility checks for nullable keys; no benchmark claim is made here.

`BindingSetAssignment` now separates the complete schema union from the guaranteed intersection. The intersection considers only names whose row values are non-null, including `ListBindingSet` null slots, while explicit schemas and dynamic iterables retain their existing name behavior. `Extension.getAssuredBindingNames()` conservatively removes names overwritten by extension elements.

LMDB retains its separate `LmdbSketchJoinOptimizer` and rewrite ordering. No standard stage list was copied into LMDB. The LMDB public regression and full module gates exercise equivalent MINUS semantics through the backend pipeline.

## Evidence and verification

The mandatory initial root quick clean install passed before test work. Copyright/header validation and formatter checks passed before publication; the final checks are rerun before staging. The initial adversarial red, all later red regressions, and every focused/module result are appended to `initial-evidence.txt`; full Maven output remains in the referenced `logs/mvnf/*-verify.log` files.

Focused and module results after the final fixes:

* `QueryJoinOptimizerAdversarialTest`: 33 passed, 0 failures, 0 errors, `logs/mvnf/20260920-185349-verify.log`.
* `QueryJoinOptimizerTest`: 64 passed, 0 failures, 0 errors, `185459`.
* `FilterOptimizerTest`: 27 passed, 0 failures, 0 errors, `185541`.
* `BindingSetAssignmentInlinerTest`: 13 passed, 0 failures, 0 errors, `185613`.
* `SPARQLMinusIterationTest`: 10 passed, 0 failures, 0 errors, `185654`; fuzz test: 1 passed, `185729`.
* `LateralQueryEvaluationStepTest`: 2 passed, `185757`.
* Connected dependency matrix: 13 passed, `185836`; connected scope matrix: 9 passed, `185908`.
* Query algebra model module: 50 passed, 1 skipped, `185940`.
* Query algebra evaluation module: 909 passed, 1 skipped, `190012`.
* MemoryStore MINUS scoping regression: 16 passed, `190118`.
* LMDB regression class: 3 passed, `190157`.
* Full LMDB module: 1199 passed, 0 failures, 0 errors, 102 skipped, `190247`.

The 102 LMDB skips are existing opt-outs, not failures introduced by this change. Three unit skips are the intentionally long concurrency case, the disabled concurrent model mutation case, and the regression-capture test whose assumption requires `-Drdf4j.lmdb.regressionCapture.enabled=true`. The 99 integration skips are disabled dataset-scale theme, sketch-estimator, filter-placement, and recorded-plan/benchmark snapshot cases; they require external theme data or deliberate benchmark capture and were not rerun with those opt-ins.

Evidence:
Command: `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs`
Report: `core/queryalgebra/evaluation/target/surefire-reports/`
Snippet: `Tests run: 909, Failures: 0, Errors: 0, Skipped: 1`.

Evidence:
Command: `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs`
Report: `core/sail/lmdb/target/surefire-reports/` and `core/sail/lmdb/target/failsafe-reports/`
Snippet: `Tests run: 1199, Failures: 0, Errors: 0, Skipped: 102`.

Audit update (2026-09-20): replaced the initial Routine C-only report with the completed test-first repair audit, recorded every original invocation and its disposition, added the model assurance and iterator failure regressions, and documented the LMDB skip categories and unmeasured nullable-key fallback cost.
