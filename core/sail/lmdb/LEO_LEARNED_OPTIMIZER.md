# LMDB learned optimizer contract

The LMDB learned optimizer is a LEO-style feedback loop layered on top of the existing OMNI and Cascades optimizer. It is deliberately not a standalone plan generator: learning may correct cardinality/work estimates, expose learned fanout evidence, and optionally steer rule priority, but it must never rewrite query semantics.

Evidence precedence is enforced conservatively. Exact/protected estimates are not overridden. Fresh query-local evidence and OMNI/sketch evidence remain preferred where available. Persisted fanout/heavy-hitter surfaces can improve skewed statement-pattern estimates. Scalar LEO operator feedback is used only when it is applicable, calibrated, and more specific evidence is missing or untrusted.

Runtime observation is exposed through `LeoLearnedEvidenceService` and operator policy is represented by `LeoOperatorLearningPolicy`:

- `LEARN_DIRECTLY` operators persist scalar row/work corrections and can feed them back into planning.
- `LEARN_AS_CHILD_MULTIPLIER` operators are observed and persisted as calibration/multiplier samples, but they do not directly produce planning evidence yet.
- `SHADOW_ONLY` operators are observed for diagnostics and RDF-specific surfaces. Statement-pattern observations can populate live predicate/value fanout surfaces, but shadow samples do not directly override plans.
- `DO_NOT_LEARN` operators are excluded because observed rows are capped, remote, partial, or otherwise unsafe.

The live LMDB learned-evidence provider is `LmdbOperatorFeedbackStats`. It owns scalar operator feedback, multiplier observations, shadow observations, plan-level shadow fingerprints, and fanout/heavy-hitter surfaces. These sidecars are keyed by the estimator revision and are reset or decayed when data or feedback epochs move forward.

Generalized operator keys are intentionally gated. The exact operator key is always recorded first. A predicate/context-preserving generalized key is used only after enough samples and enough distinct constant-specific signatures have trained it, preventing repeated executions of a single constant from being generalized to unrelated constants.

Rule steering is shadow-first. `LmdbOperatorFeedbackStats` can emit `LeoRuleHint`s into Cascades memo groups. By default these hints carry zero priority delta and are visible only as feedback metadata. Setting `rdf4j.optimizer.lmdb.leoRuleSteering=true` allows the same hints to affect rule priority while preserving all normal semantic checks.

## Phase 12-20 additions

Optimized EXPLAIN output is now annotated with learned-evidence metadata where available. LMDB nodes may include `optimizer.leoEvidence`, `optimizer.leoPlanRankingCandidate`, `optimizer.leoPlanRankingReason`, `optimizer.leoPlanRankingConfidence`, and `optimizer.leoPlanRankingEvidenceCount`. These annotations are diagnostic: they show what learned evidence matched, not a semantic rewrite.

Operator keys now include binding-shape context where Cascades supplies a `BindingUniverse`/`BindingShape`, and physical implementation context where the planned node exposes an access mode or repeated-probe/bound-lookup hint. Context-specific keys are recorded with an unscoped fallback key so old evidence still has a conservative migration path.

Fanout surfaces are context-aware. A context-specific surface is learned for predicate/value/context combinations when a statement pattern has a bound context; a global fallback surface is trained at the same time so unseen contexts can still use low-risk aggregate evidence.

Rule steering has a graduated safety layer. `rdf4j.optimizer.lmdb.leoRuleSteering=true` enables priority deltas, while `rdf4j.optimizer.lmdb.leoRuleSteering.minConfidence`, `.maxPriorityDelta`, `.badMissBudget`, `.badMissQError`, and `.cooldownEpochs` bound when and how strongly feedback may steer Cascades. Bad misses place steering into cooldown without disabling passive observation.

Plan-level learning is currently shadow ranking. Cascades reports top-k candidate costs to `LeoLearnedEvidenceService.observePlanCandidate(...)`; completed root plans record actual row/work feedback. `LeoPlanRankingAdvice` exposes the resulting advice as shadow metadata. Applying plan-level reranking remains gated behind the separate `rdf4j.optimizer.lmdb.leoPlanRanking` property and is intentionally inert unless the advice is non-shadow and sufficiently trusted.

Learned-state administration is exposed through `LmdbSailStore` helper methods for reset, summary, export, import, and shadow benchmark snapshots. These helpers are intended for benchmark and operations tooling so bad or stale learned feedback can be inspected and cleared without deleting the store.

## Shadow benchmark helper

`scripts/leo-shadow-benchmark.sh` runs any supplied workload command with LEO observation enabled and steering/ranking in shadow mode by default. `scripts/leo-shadow-benchmark-summary.py` can scan loose telemetry or explain-plan logs for learned-evidence fields and summarize confidence/q-error samples. These scripts are intentionally thin wrappers so they can be used with Maven tests, custom workload runners, or external benchmark harnesses.

## Phase 22-30 completion notes

The explain surface now has an explicit learned-estimate diff. Nodes can expose the base rows/work rows, learned rows/work rows, reconciliation decision, source/kind, confidence, and the full debug evidence string through planned metrics. This gives optimized EXPLAIN and benchmark log parsers enough structure to show what learning changed and why.

Plan-candidate feedback is now correlated in two stages. During Cascades exploration every costed alternative can be recorded as an accepted or rejected candidate. After runtime completes, the root plan's candidate/rule fingerprint receives the actual rows/work rows. This makes the top-k ranking path shadow-first but data-bearing: the optimizer can later tell whether the physical candidate family that won actually behaved better.

Applied plan reranking remains guarded. The service exposes `shouldApplyPlanRanking(...)`; Cascades applies a learned work-row adjustment only when the rollout profile and explicit plan-reranking switch allow it and the ranking has enough confidence and improvement margin. This keeps plan-level learning opt-in and narrow.

Learned state administration now includes a human-readable detail export and a lightweight inspector script. The export still writes revision-compatible binary sidecars, but it also writes bounded summaries that can be viewed without binding operational tooling to the sidecar wire format.

Feedback-poisoning checks now explicitly reject observations affected by slice/limit ancestry, cancelled/aborted/rolled-back execution, remote SERVICE execution, expression/remote errors, rows dropped by limit/offset, and non-terminal partial iteration. Completed-root rescue is still allowed to record nested nodes, but only when the root is fully consumed and the nested node is not already marked as recorded.

Mutation handling is profile-safe and decay-oriented. A reset policy can still force full invalidation, but the default path advances the feedback epoch and decays predicate-specific fanout surfaces when predicate identifiers are available. Broad or unknown mutation sets can still trigger full reset.

OMNI/LEO reconciliation is centralized in `LeoEstimateReconciler`. Exact/protected estimates are kept, OMNI/sketch-like estimates are preferred over thin scalar feedback, and scalar LEO evidence is accepted only when it is finite, calibrated, and not obviously less specific than the base estimate.

Rollout profiles are named and centralized through `LeoRolloutProfile`:

- `off`
- `observe-only`
- `shadow-explain`
- `safe-cardinality-correction`
- `safe-rule-steering`
- `safe-plan-reranking`
- `experimental-full`

The profile property is `rdf4j.optimizer.lmdb.leo.rolloutProfile`; the legacy alias `rdf4j.optimizer.lmdb.leoProfile` is also accepted.
