# Connectedness optimiser adversarial audit

This is a Routine C investigation. It adds unit tests and evidence only; no production source was changed.

## Confirmed current behaviours

| Attempt | Unit coverage | Runtime result | Classification |
| --- | --- | --- | --- |
| More than six constant statement patterns with a cheap connected pair outside the three primary candidates | `startingSelectionChecksConnectedPairsOutsideTheThreeCheapestCandidates`; its `StartingFixtureStatistics` enables join estimation and asserts that the join-estimation branch was called | The optimized and legal rare-pair evaluations return the same complete binding bag, while the counted statement work is `20271` versus `371` | Real physical cost harm. The primary candidate truncation is at `QueryJoinOptimizer.java:608-612`. `git blame` attributes those lines to `1fd83f729d0`, before `8f9da0e0545`; the base source was not executed, so historical runtime attribution is left open. |
| A statement depending on a computed BIND result is missed as a connected cost opportunity | `computedBindOutputCanBeMissedAsAConnectedCostOpportunity` and `standardPipelineCanMissAStatementProducedTotalBindCostOpportunity`, parameterized for both `STR(?a)` with a string object and `isIRI(?a)` with a boolean object | Direct optimizer work is `2000` versus legal `1001`; full-pipeline work is `2001` versus `1002`. Both variants preserve the complete binding bag | Cost opportunity only, not a semantic false-connection claim. `isGuaranteedValueExpr` accepts constants and variables only at `QueryJoinOptimizer.java:1160-1167`. The fixtures use a bound IRI and total expressions, while the estimation statistics deliberately isolate the connectedness decision. The guarantee logic is from `8f9da0e0545`. |
| Reordering a join ahead of MINUS exposes an incoming-binding scope error | `differenceRightVariablesMustNotChangeResultsWhenAJoinIsReordered`, `parsedMinusRightVariablesMustNotChangeResultsWhenAJoinIsReordered`, `standardPipelineKeepsMinusRightVariablesOutOfOuterBindings`, and the parsed disjoint-variable case `parsedMinusWithDisjointChildrenRejectsAnUnrelatedIncomingBinding` | Each raw/public evaluation returns one row; the reordered evaluation returns an empty bag. The disjoint case has no hidden consumer and uses separate variables in the MINUS children, so it isolates the incoming-binding trigger | Confirmed semantic correctness failure. `MinusQueryEvaluationStep.evaluate` passes the same incoming bindings to both children at `MinusQueryEvaluationStep.java:30-32`; `SPARQLMinusIteration` then derives the right-side binding-name domain and shared-name decision at `SPARQLMinusIteration.java:80-147`. The optimizer's recursive variable collector (`QueryJoinOptimizer.java:726-735`) and left-only `Difference` binding info (`1050-1052`) can trigger the reorder, but the evaluator scope behavior is the direct cause observed in the isolated case. Historical introduction attribution is unverified. |

The MINUS plan-only test `differenceRightVariablesAreIncorrectlyExposedAsOuterCostConnections` remains as supporting coverage for the optimizer trigger. It is not treated as an independent root cause or as proof that changing only the collector would fix the semantic failure.

## Retained controls and closed attempts

* `positiveLengthPathExportsItsIntermediateBindingWhenTheEvaluatorDoes` and `parsedPositiveLengthSequencePathKeepsItsConsumerSemantics` pass, including positive-length path intermediate bindings and a parsed sequence-path consumer.
* `directBindOutputRemainsConnectedAsTheControlCase` passes for the direct variable BIND control.
* `statementRunUsesTheCostOfTheWholeBoundPrefixInsteadOfAnOldPair` is a public finite fixture with equal measured work (`602` current versus `602` legal) and equal complete binding bags. The earlier exact-order hypothesis is therefore closed and the test now asserts only semantic and work invariants.
* The scope matrix passes 9 cases covering UNION complete/partial/UNDEF, OPTIONAL, nested EXISTS/NOT EXISTS, subquery projection/alias, grouping, multiple connected choices, and mixed nesting.
* The dependency matrix passes 13 cases covering SERVICE readiness, incoming bindings, named graphs, paths, LATERAL, nullable OPTIONAL inputs, independent siblings, and connected physical-chain ordering. Earlier failures were invalid fixture/projection attempts; their artifacts remain, while the final unit cases are corrected.
* Earlier temporary STR/isIRI and prefix probes are retained as evidence artifacts. The final source keeps the corrected STR and `isIRI` parameterized attempts rather than relying on old logs.

## Evidence and verification

* Initial root `-Pquick clean install`: `initial-evidence-before-adversarial-20260920.txt`, full log `maven-build-before-adversarial-20260920.log`.
* Copyright validation: `scripts/checkCopyrightPresent.sh` completed with `All files have valid copyright headers and SPDX lines.`
* Formatter: `adversarial-formatter-pass-20260920-1605.txt`, full log `formatter-adversarial-20260920-1605.log`.
* Existing optimizer baseline: 64 tests passed in `adversarial-baseline-final-20260920-160828.txt`.
* Scope matrix: 9 tests passed in `adversarial-scope-matrix-final-pass-20260920-160718.txt`.
* Dependency matrix: 13 tests passed in `adversarial-dependency-matrix-final-pass-20260920-160752.txt`.
* Adversarial controls: 4 tests passed in `adversarial-final-controls-pass-20260920-160643.txt`.
* The final adversarial class contains 14 invocations: 10 intentional failures and 4 passing controls, with zero errors, in `adversarial-corrected-class-failures-20260920-160420.txt`. The failures remain enabled because they assert semantic or measured-work invariants.
* The final `core/queryalgebra/evaluation` gate reports 890 tests, 10 failures, 0 errors, and 1 skipped test. All 10 failures are the intentional assertions in the adversarial class; no unrelated module test failed. Evidence: `adversarial-module-final-20260920-160905.txt`, full log `logs/mvnf/20260920-160905-verify.log`.

Evidence:
Command: `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs`
Report: `adversarial-module-final-20260920-160905.txt`
Snippet: `Summary: tests=890, failures=10, errors=0, skipped=1, time=9.249s`; the first failure is the expected BIND work assertion (`optimized=2000`, `ideal=1001`).
