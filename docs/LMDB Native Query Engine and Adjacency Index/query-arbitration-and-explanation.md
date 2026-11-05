# LMDB strategy arbitration and explanations

An explain decision is a report about one dispatch point, not a prediction that the whole query will run in one physical operator. This guide follows the offered candidate set through selection, opening, fallback, and explanation. The branch delta catalogued as Q10 covers the LMDB proposal, arbitration, and explanation paths; stable fallback behavior and generic evaluation remain the semantic baseline for these changes.

## Candidate admission, selection and opening

Every decision point runs on [`LmdbNativeDispatchLoop`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeDispatchLoop.java). A site contributes its strategy list; each strategy admits itself with a [`LmdbNativeCandidate`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeCandidate.java) carrying a quote and an opener, or declines. A decline is a structural statement, not a loss in a cost comparison.

[`LmdbNativeSelector`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSelector.java) is the one ranking rule, applied to the admitted candidates:

1. Each candidate is priced by [`LmdbNativeStaticPrior`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeStaticPrior.java), then narrowed by learned advice if the [advisory learner](query-cost-learning-and-adaptation.md) is on.
2. A last-resort candidate competes only when nothing else was admitted, and an unpriced candidate only when no candidate has a price. A quote has a price when it has a finite ceiling or an estimate above its floor; a floor alone is not one.
3. Structural dominance edges are applied, all at once, so `A -> B -> C` does not depend on visiting order. A dominated candidate goes regardless of price.
4. A parallel candidate must cut the best serial expected price by the share its worker count demands: 30 % on four workers, 50 % on eight.
5. A candidate whose whole interval lies above a rival's whole interval is eliminated. A finite ceiling is therefore a proof: sampled row counts and the store's statement count set a quote's expectation and never its bounds, so an estimated quote renders as `low..unbounded` and eliminates nobody.
6. The lowest expected price among the survivors wins, with registry order breaking exact ties only.

A finite consumer slice prorates streaming work before pricing. Every rule operates only over the candidates the actual dispatch site offered. A strategy being forceable, passing compile-time recognition and successfully opening are three different facts.

## Open decline, retry and forced strategy

After selection, `open()` may still return `null` if bind-time views, estimates, reservations, or source capabilities no longer satisfy the offer. The dispatch loop then removes that candidate, re-ranks the remaining set, and releases resources owned by non-winners. A removed candidate takes its dominance edges with it, which is how a dominated candidate still serves when its dominator cannot open. A proposal that fails while constructing itself must release reservations it has not transferred to the dispatch loop; a returned proposal's `close()` owns them. The winner's cursor/resources live with the consuming cursor/aggregate result.

Some dispatchers register a terminal semantic fallback as a last-resort candidate, used only after ordinary candidates decline. Other sites, such as ORDER BY fusion, have an unconditional caller-level fallback and use optional forcing semantics so a forced family that is not applicable at that shortcut can still be tried at its later authoritative site.

Force controls bypass normal cost ranking only at the authoritative dispatch point. A mandatory forced family that was not offered or whose open returned `null` fails with a `QueryEvaluationException` naming the requested tag and captured decline reason; it does not quietly run `nestedLoop`. Forcing at an optional shortcut quietly declines if no candidate matches there, allowing the caller's own normal path to continue. Exact forceable families are defined by [`LmdbNativeForceableStrategies`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeForceableStrategies.java) and the current family reachability notes in [strategy families](query-strategy-families.md).

At the GROUP site a query memory refusal while a candidate opens counts as a decline when the input may be evaluated a second time, so the next candidate runs and the explanation lists the refused one in `nativeStrategyDeclines`. When no candidate is left, or the input may not be repeated, the query fails with a `QueryEvaluationException` naming the memory budget.

Candidate open decline and post-open iteration failure are different. Before the first row is committed, a decision site may try an alternative if it has one and can safely close the attempted resources. After a row has escaped, restarting would risk duplicated/missing solutions and repeated volatile or remote work. Errors, cancellation, and failures then propagate through the selected cursor, with cleanup. A blocking open may materialize work before it returns the first row; that is still pre-output and only recoverable if the route reports a safe decline rather than a semantic/query error.

## Explain and telemetry boundary

[`LmdbNativeDecisionRecord`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeDecisionRecord.java) holds everything the selector concluded about one dispatch, and explain, logs, traces and JFR all render it rather than recompute it:

- `nativeDecision.<point>.<n>` is one metric per decision on the plan node, with `n` counting that point's decisions there (the first eight, since a correlated point can dispatch per entry row). It names the winner and its reason, then every candidate with its interval, its elimination, or its decline.
- `nativeDecisionOutcome` is the last winner with its reason and expected price, in `key=value` form.
- `nativeStrategyDeclines` lists structural declines and eliminations together; an elimination reads `outranked (<reason>)`.
- `nativeStrategyProposalCosts` and `nativeStrategyProposalFeatures` list every candidate's expected price and feature counts.
- The JFR event `org.eclipse.rdf4j.lmdb.NativeStrategyDecision` ([`NativeStrategyDecisionEvent`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeStrategyDecisionEvent.java), disabled by default) is committed once per entry of a ranking.
- `rdf4j.lmdb.nativeDecisions.trace` writes each record to standard error.

Read the path actually emitted by a dispatch, not a candidate list, as evidence that a physical operator ran. Preview invokes no opener, reserves no execution resources and teaches the learner nothing; it records "execution-time binding and admission must still succeed" as the winner's condition.

Normal dispatch logging includes evaluation and decision ids, dispatch point, strategy tag, role and reason. Server and Workbench exposure and the explanation wire format are covered in [integration plan explanation and telemetry](integration-plan-explanation-and-telemetry.md).

## Illustrative control-flow example (source-only)

For a basic join with both `nestedLoop` and a batch proposal structurally available, both are admitted and priced. If batch wins but its bind declines before output because a required view could not be leased, the loop releases it and re-ranks what remains. If instead batch emits a row and then fails, the failure propagates through the selected cursor, because restarting could duplicate or lose solutions.

Representative source tests: [`LmdbNativeSelectorTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSelectorTest.java), [`NativeStrategyDecisionDiagnosticsTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeStrategyDecisionDiagnosticsTest.java), [`LmdbNativeForcedExecutionStrategyTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeForcedExecutionStrategyTest.java) and [`LmdbNativeGoldenDecisionsTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/LmdbNativeGoldenDecisionsTest.java).
