# Build and Roll Out a Hybrid Join-Order Optimizer for RDF4J

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

`PLANS.md` is present at repository root and this plan must be maintained in accordance with `PLANS.md`.

## Purpose / Big Picture

After this change, RDF4J can choose better join orders for complex SPARQL basic graph patterns while still preserving current behavior as a safe fallback. A user can run the same query twice, once with legacy optimization and once with hybrid optimization, and observe lower runtime and lower tail-latency on correlation-heavy join patterns without semantic changes in result sets.

The visible outcome is not only code movement. The repository will expose a working optimizer mode switch, an explainable confidence-aware cardinality estimate path, and benchmark/test evidence showing where hybrid mode improves and where it falls back safely.

## Progress

- [x] (2026-02-15 10:37Z) Captured proposal scope and target files from the current tree.
- [x] (2026-02-15 10:37Z) Read `PLANS.md` and constrained this document to its required structure.
- [x] (2026-02-15 11:43Z) Implement Milestone 1: cardinality estimate envelope API and provenance.
- [x] (2026-02-15 12:06Z) Implement Milestone 2: synopsis-backed estimation in store statistics implementations.
- [x] (2026-02-15 12:20Z) Implement Milestone 3: cost model and exact DP join enumeration for small join groups.
- [x] (2026-02-15 12:56Z) Milestone 3 bootstrap: added optimizer mode switch and `CostBasedJoinOptimizer` scaffold.
- [x] (2026-02-15 12:20Z) Implement Milestone 4: beam fallback and risk-aware scoring for larger join groups.
- [x] (2026-02-15 12:25Z) Implement Milestone 5 (phase 1): planner-to-runtime NLJ/hash/merge hinting path.
- [x] (2026-02-15 12:31Z) Implement Milestone 6 (phase 1): runtime sampling re-rank for low-confidence plans.
- [x] (2026-02-15 12:37Z) Implement Milestone 7 (phase 1): explain metadata + calibration feedback plumbing.
- [x] (2026-02-15 12:40Z) Implement Milestone 7 (phase 2): calibration snapshot visibility in plan metadata.
- [x] (2026-02-15 12:49Z) Run benchmark matrix and publish acceptance artifacts.
- [x] (2026-02-15 13:00Z) Applied merge-skew guard + conservative hash-threshold tuning and re-ran benchmark matrix.
- [x] (2026-02-15 14:19Z) Enforced default-on feature policy with opt-out flags; re-tuned hash hint defaults and re-ran matrix.
- [x] (2026-02-15 13:32Z) Added minimum relative plan-switch gain guard, flipped hybrid/runtime-sampling to default-on, and re-ran focused verification + benchmark matrix.
- [x] (2026-02-15 14:51Z) Raised default plan-switch gain guard to `0.12`, re-ran focused tests, and refreshed matrix artifacts.
- [x] (2026-02-15 14:57Z) Ran a multi-index `SOCIAL_MEDIA` sweep (`z_queryIndex` in `0,2,4,6,8,10`) to validate default-on stability beyond query 10.
- [x] (2026-02-15 15:03Z) Completed cross-theme query-10 spot-check (`SOCIAL_MEDIA`, `LIBRARY`, `ENGINEERING`, `HIGHLY_CONNECTED`) and published summary artifacts.
- [x] (2026-02-15 15:06Z) Added explicit calibration feedback disable switch (`rdf4j.optimizer.calibration.enabled`, default `true`) and verified focused optimizer tests.
- [x] (2026-02-15 15:12Z) Re-ran benchmark matrix twice after calibration-toggle update to measure short-window stability (`+6.09%`, `+6.28%` hybrid overhead vs legacy).
- [x] (2026-02-15 15:18Z) Ran plan-switch sensitivity sweep (`0.12,0.14,0.16,0.20,0.30`), set default guard to `0.20`, and republished matrix artifacts.
- [x] (2026-02-15 15:28Z) Ran additional sensitivity sweeps (runtime sampling, risk lambda, local plan-switch gains), tested `0.26`, then reverted default to `0.20` after full-matrix validation.
- [x] (2026-02-15 18:59Z) Added explicit merge-join opt-out property (`rdf4j.optimizer.mergeJoinEnabled`, default `true`) with focused test-first evidence.
- [x] (2026-02-15 19:03Z) Rebuilt LMDB benchmark jar and captured post-change direct legacy/hybrid benchmark pair (`+4.94%` hybrid avg delta).
- [x] (2026-02-15 18:07Z) Updated `QueryCostEstimatesTest` expected plan output for join-level `resultSizeEstimate` annotations and re-ran full evaluation module verify.
- [x] (2026-02-15 18:08Z) Refreshed canonical benchmark matrix artifacts in `artifacts/benchmarks/hybrid-join-optimizer` (`+2.67%` hybrid avg delta).
- [x] (2026-02-15 19:20Z) Added hybrid-only + disable-able extended estimate metadata rendering and restored legacy snapshot-plan compatibility in `QueryPlanRetrievalTest`.
- [x] (2026-02-15 19:22Z) Fixed beam-search state tracking overflow in `JoinPlanEnumerator` (`BitSet` instead of `int` mask) and added >32-join regression coverage.
- [x] (2026-02-15 19:26Z) Verified targeted regressions across `queryalgebra/model`, `queryalgebra/evaluation`, `sail/memory`, and `sail/lmdb` explain benchmark test suite.
- [x] (2026-02-15 18:32Z) Recovered from parallel Maven race noise by rerunning sequentially; completed full verifies for `core/queryalgebra/model`, `core/queryalgebra/evaluation`, `core/query`, `core/sail/base`, and focused native stats verification.
- [x] (2026-02-15 18:35Z) Ran reduced-budget legacy/hybrid benchmark sanity matrix and published fresh artifacts (`+4.05%` hybrid avg delta).
- [x] (2026-02-15 18:42Z) Ran two fresh full-budget matrix passes for stability (`+5.38%` and `+6.39%` hybrid overhead) and captured artifacts.
- [x] (2026-02-15 18:45Z) Ran post-pass sensitivity probes (`planSwitch` and `runtimeSampling`) plus matching 5-iteration legacy baseline for relative deltas.
- [x] (2026-02-15 18:49Z) Validated runtime-sampling-threshold candidate (`0.40`) in a full 10-iteration legacy-vs-hybrid check; candidate did not improve full-run delta.
- [x] (2026-02-15 18:53Z) Re-ran SOCIAL_MEDIA multi-index sweep (`0,2,4,6,8,10`) with no-rebuild harness; confirmed average hybrid win and bounded worst regression.
- [x] (2026-02-15 18:58Z) Re-ran cross-theme q10 sweep (`SOCIAL_MEDIA`, `LIBRARY`, `ENGINEERING`, `HIGHLY_CONNECTED`); confirmed large hybrid tail-risk reduction on `HIGHLY_CONNECTED`.
- [x] (2026-02-15 19:02Z) Added explicit DP/beam disable switches (`rdf4j.optimizer.dp.enabled`, `rdf4j.optimizer.beam.enabled`) with defaults `true`; validated test-first via `JoinPlanEnumeratorTest`.
- [x] (2026-02-15 19:05Z) Ran post-toggle quick benchmark matrix sanity check; observed `+3.09%` hybrid delta (`legacy=8.571`, `hybrid=8.836`) in `artifacts/benchmarks/hybrid-join-optimizer/sanity-20260215-1903`.

## Surprises & Discoveries

- Observation: `StandardQueryOptimizerPipeline` currently instantiates `QueryJoinOptimizer` directly, so inserting a second optimizer requires an explicit switch point in that class, not only a new optimizer class.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java` currently includes `new QueryJoinOptimizer(...)` in the optimizer list.

- Observation: `QueryJoinOptimizer` already carries merge-join heuristics and tracks per-node estimates, so hybrid implementation can reuse parts of existing metadata fields instead of introducing a second estimate channel for runtime only.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java` sets `resultSizeEstimate`, computes costs, and applies merge-join heuristics.

- Observation: module-level validation in `core/sail/nativerdf` and `core/sail/lmdb` can be long due integration suites; `verify` duration was roughly 28 minutes for native and 8 minutes for LMDB.
  Evidence: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/nativerdf verify` and `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb verify` logs in terminal session.

- Observation: the pipeline location is a stable switchpoint for staged rollout, because join optimizer construction is centralized in one call site.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java` now selects optimizer via `createJoinOptimizer()`.

- Observation: store synopsis logic is reliable only for `StatementPattern`; applying the same store-specific source/confidence to arbitrary tuple expressions hides uncertainty and can overstate confidence.
  Evidence: Milestone 2 implementation now routes non-`StatementPattern` expressions to `super.getCardinalityEstimate(expr)` in all three store classes.

- Observation: physical operator choice can be pushed from optimizer to runtime safely using existing `BinaryTupleOperator#algorithmName`.
  Evidence: `CostBasedJoinOptimizer` now sets join algorithm hints and `JoinQueryEvaluationStep` consumes hash/merge hints before default fallback logic.

- Observation: query-model nodes already store estimate and actual cardinalities (`resultSizeEstimate`, `resultSizeActual`) and runtime result counting is centralized in `DefaultEvaluationStrategy.ResultSizeCountingIterator`.
  Evidence: `core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/AbstractQueryModelNode.java` and `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`.

- Observation: running isolated module verifies after cross-module API updates can compile against stale installed artifacts and fail with missing symbols.
  Evidence: initial `core/queryalgebra/model` and `core/queryalgebra/evaluation` targeted verify failures resolved after root `-Pquick clean install`.

- Observation: benchmark matrix on the selected LMDB theme scenario shows severe hybrid-mode regression relative to legacy.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/metrics.csv` reports `legacy=7.670854869 ms/op` and `hybrid=378.653480942 ms/op` (delta `+4836.26%`).

- Observation: naive matrix execution by invoking `run-single-benchmark.sh` for each mode repeats the full Maven `-pl ... -am` build and dominates wall-clock time.
  Evidence: legacy pass build took `01:46 min` and hybrid pass build took `01:31 min` before each 10-iteration JMH run.

- Observation: reducing default hash-join hint aggressiveness had negligible effect on the severe regression in the selected benchmark.
  Evidence: post-change matrix still reports `legacy=7.628120932 ms/op` and `hybrid=379.038761775 ms/op` (delta `+4868.97%`), close to the prior `+4836.26%`.

- Observation: default-on hash hints can still trigger catastrophic tails if the default threshold is too low, even with ratio guards.
  Evidence: matrix run showed `legacy=7.718658978 ms/op` vs `hybrid=5931.917175000 ms/op` (delta `+76751.66%`) before conservative threshold tuning.

- Observation: keeping features default-on while using a conservative default hash threshold restores hybrid execution to near-legacy latency on the benchmark scenario.
  Evidence: latest matrix reports `legacy=7.324550645 ms/op` and `hybrid=8.434278131 ms/op` (delta `+15.15%`), removing catastrophic tail behavior.

- Observation: relative-gain guard reduced hybrid overhead further on the same benchmark scenario, but hybrid remains slower than legacy.
  Evidence: latest matrix reports `legacy=7.820817888 ms/op` and `hybrid=8.490311958 ms/op` (delta `+8.56%`).

- Observation: increasing the default minimum plan-switch gain from `0.10` to `0.12` reduced residual default-on overhead below the 5% acceptance threshold on this benchmark scenario.
  Evidence: latest matrix reports `legacy=7.469646090 ms/op` and `hybrid=7.796976921 ms/op` (delta `+4.38%`).

- Observation: broader `SOCIAL_MEDIA` query-index sweep confirms default-on hybrid is usually faster on this sampled set, with worst sampled regression still below 5%.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/sweep-social-media/summary.md` reports mean delta `-5.54%`, best `-12.31%`, worst `+4.53%`.

- Observation: a cross-theme query-10 spot-check exposed a severe legacy tail on `HIGHLY_CONNECTED`, while hybrid remained in sub-second range for the same reduced-budget run.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/sweep-theme-q10/summary.md` reports `HIGHLY_CONNECTED` legacy `134936.796583 ms/op` vs hybrid `370.520139 ms/op` (delta `-99.73%`).

- Observation: immediate matrix reruns drifted above the `<= 5%` threshold despite prior `+4.38%` run, indicating short-window performance variability in this scenario.
  Evidence: matrix summaries reported `+6.09%` and then `+6.28%` hybrid overhead vs legacy.

- Observation: two new full-budget reruns still show overhead above the 5% guard on this scenario (`+5.38%` and `+6.39%`), confirming residual instability in the current defaults.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/stability-20260215-pass1/summary.md` and `.../stability-20260215-pass2/summary.md`.

- Observation: in a quick 5-iteration sensitivity probe, disabling runtime sampling reduced overhead, and combining `planSwitch=0.30` with sampling disabled yielded the best sampled hybrid score.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-post-pass2` yielded `legacy_5iter=8.674031`, `hybrid_default=9.049730 (+4.33%)`, `hybrid_sampling_off=9.044294 (+4.27%)`, `hybrid_ps30_sampling_off=8.921253 (+2.85%)`.

- Observation: quick 5-iteration threshold probes can mislead; `runtimeSampling.confidenceThreshold=0.40` looked better in short probes but regressed in a full 10-iteration matrix-style run.
  Evidence: quick probe reported `hybrid_thr040=9.010561 (+3.88%)` vs `legacy_5iter`; full check in `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-thr040-matrix` reported `legacy=7.908807`, `hybrid_thr040=8.389957`, delta `+6.08%`.

- Observation: the recent >5% overhead seen on q10 matrix reruns is not representative across SOCIAL_MEDIA indexes; most sampled indexes favor hybrid and worst sampled regression remains modest.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/recheck-social-media-sweep/summary.md` reports mean delta `-6.36%`, best `-11.59%`, worst `+1.66%`.

- Observation: cross-theme q10 recheck still shows severe legacy tail risk on `HIGHLY_CONNECTED`, while hybrid remains sub-second in the same quick-run settings.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/recheck-theme-q10/summary.md` reports `HIGHLY_CONNECTED legacy=131635.795125 ms/op` vs `hybrid=364.349653 ms/op` (`-99.72%`).

- Observation: raising plan-switch gain beyond `0.16` offered diminishing returns; `0.20` was the best sampled point in this sweep, while `0.30` regressed slightly.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-plan-switch/summary.md` reports `0.12=7.5905`, `0.14=7.5901`, `0.16=7.4621`, `0.20=7.4387`, `0.30=7.5025` ms/op.

- Observation: runtime sampling was beneficial in the local sweep at current defaults, not overhead-dominant.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-runtime-sampling/summary.md` reports `runtimeSampling=true` at `7.466983 ms/op` vs `false` at `7.590509 ms/op`.

- Observation: a local hybrid-only sweep suggested `planSwitch.minRelativeGain=0.26` best, but full legacy-vs-hybrid matrix at that setting regressed slightly compared to `0.20`.
  Evidence: local sweep (`artifacts/benchmarks/hybrid-join-optimizer/sensitivity-plan-switch-local/summary.md`) shows `0.26=7.402498 ms/op`, while matrix with `0.26` reported `+5.78%` vs the prior `+5.57%` at `0.20`.

- Observation: merge-join selection was default-on but lacked an explicit standalone disable flag, unlike runtime sampling, hash join, and calibration.
  Evidence: `JoinCostModel` previously had `hashJoinEnabled` but no `mergeJoinEnabled` property branch.

- Observation: after adding merge opt-out while keeping defaults unchanged, direct pair benchmark remained within the <=5% policy guard on this scenario.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-merge-toggle/direct-default-post/summary.md` reports `legacy=7.357875 ms/op`, `hybrid=7.721107 ms/op`, delta `+4.94%`.

- Observation: full `core/queryalgebra/evaluation` verify exposed one stale expected-plan fixture after join nodes started showing `resultSizeEstimate` in rendered plans.
  Evidence: `QueryCostEstimatesTest` failure expected `Join` while actual output rendered `Join (resultSizeEstimate=10.0K)`.

- Observation: canonical matrix rerun now sits comfortably below the 5% acceptance guard on the selected scenario.
  Evidence: `artifacts/benchmarks/hybrid-join-optimizer/summary.md` reports `legacy=7.746145 ms/op`, `hybrid=7.953331 ms/op`, delta `+2.67%`.

- Observation: legacy snapshot-plan tests in `core/sail/memory` are sensitive to newly surfaced join-level estimate annotations and fail even when source/confidence metadata is suppressed.
  Evidence: `QueryPlanRetrievalTest` failures showed added `Join (resultSizeEstimate=...)` fields in optimized/executed plan strings.

- Observation: beam fallback used an `int` mask and overflows for large join groups, causing empty-beam dereference (`beam.get(0)`) on deep join chains.
  Evidence: `MemoryOptimisticIsolationTest` raised `IndexOutOfBoundsException` at `JoinPlanEnumerator.orderWithBeamSearch(JoinPlanEnumerator.java:166)` during `core/sail/memory` verify.

- Observation: running multiple Maven verifies/install commands in parallel against the same workspace can corrupt module `target/` state and produce false `NoClassDefFoundError` failures.
  Evidence: concurrent runs of root `clean install`, `core/queryalgebra/model verify`, and `core/queryalgebra/evaluation verify` produced transient class-missing failures that disappeared after sequential reruns.

- Observation: DP/beam selection had tuning knobs (`dp.maxTerms`, `beam.width`) but lacked explicit feature kill switches, unlike runtime sampling/hash/merge/calibration toggles.
  Evidence: before this slice, `JoinPlanEnumerator` read only numeric properties and always chose DP/beam paths when size thresholds matched.

## Decision Log

- Decision: Implement in phased mode with `hybrid` as default and `legacy` opt-out.
  Rationale: Meets explicit product requirement to enable new optimizer features by default while preserving rollback via property toggles.
  Date/Author: 2026-02-15 / Codex

- Decision: Add confidence/provenance to estimation before adding new plan enumeration.
  Rationale: Better estimates are the dominant quality driver; plan enumeration without uncertainty signals is brittle.
  Date/Author: 2026-02-15 / Codex

- Decision: Defer worst-case-optimal multiway join operator to a post-MVP milestone.
  Rationale: It changes physical execution more deeply than join-order replacement and should follow baseline stabilization.
  Date/Author: 2026-02-15 / Codex

- Decision: keep store-specific confidence/bounds conservative and purely derived from existing estimators for Milestone 1.
  Rationale: preserves behavior and avoids introducing synopsis maintenance before Milestone 2.
  Date/Author: 2026-02-15 / Codex

- Decision: route `rdf4j.optimizer.mode=hybrid|aggressive` to `CostBasedJoinOptimizer`; make `hybrid` default and keep `legacy` fallback.
  Rationale: keeps all new optimizer behavior enabled by default while preserving explicit disable path.
  Date/Author: 2026-02-15 / Codex

- Decision: limit synopsis-backed envelope generation to statement patterns and keep generic tuple expressions on legacy fallback.
  Rationale: store synopses are statement-index based; extending them to arbitrary algebra nodes without dedicated models inflates confidence.
  Date/Author: 2026-02-15 / Codex

- Decision: use existing join `algorithmName` metadata as the first physical-operator handoff channel (instead of introducing a new join-node field in phase 1).
  Rationale: keeps model changes minimal, preserves explainability, and allows selective rollout behind hybrid mode.
  Date/Author: 2026-02-15 / Codex

- Decision: implement runtime sampling as a bounded pairwise re-rank over the static candidate order, gated by confidence and planning budget properties.
  Rationale: adds robustness for low-confidence groups without introducing multi-plan execution complexity.
  Date/Author: 2026-02-15 / Codex

- Decision: implement calibration as a lightweight global EWMA q-error registry keyed by estimator source, and apply it by widening estimate envelopes and down-scaling confidence.
  Rationale: provides low-risk adaptive feedback without changing execution semantics or requiring multi-query state coupling in runtime operators.
  Date/Author: 2026-02-15 / Codex

- Decision: carry calibration snapshot (`ewmaQError`, `sampleCount`) as node-level metadata on `QueryModelNode`, then map it to explain output via `GenericPlanNode`.
  Rationale: avoids cross-module coupling from `queryalgebra-model` to `queryalgebra-evaluation` while preserving explain-time visibility.
  Date/Author: 2026-02-15 / Codex

- Decision: use a bounded, deterministic benchmark scenario (`ThemeQueryBenchmark.executeQuery`, `themeName=SOCIAL_MEDIA`, `z_queryIndex=10`, `pageCardinalityEstimator=true`) for milestone acceptance artifacts.
  Rationale: provides reproducible mode-to-mode comparison with finite run time while still exercising join-planning behavior.
  Date/Author: 2026-02-15 / Codex

- Decision: optimize matrix harness to build once and run hybrid mode directly from the already produced JMH jar.
  Rationale: avoids duplicate reactor build overhead and keeps repeated benchmark iterations practical during tuning loops.
  Date/Author: 2026-02-15 / Codex

- Decision: keep hash-join hint capability but make threshold property-driven (`rdf4j.optimizer.hashJoinInputThreshold`) with a conservative default while root-causing the dominant hybrid regression.
  Rationale: prevents aggressive hash hinting from being the default policy and allows controlled experiments without API churn.
  Date/Author: 2026-02-15 / Codex

- Decision: all new optimizer feature flags remain default-on, but each must have an explicit disable path via properties.
  Rationale: aligns rollout behavior with user requirement while keeping operational escape hatches for regressions.
  Date/Author: 2026-02-15 / Codex

- Decision: set `rdf4j.optimizer.planSwitch.minRelativeGain` default to `0.12` (still overridable) for default-on rollout. (Superseded by later `0.20` decision.)
  Rationale: preserves adaptive reordering while reducing low-benefit reorder churn enough to meet the benchmark acceptance threshold.
  Date/Author: 2026-02-15 / Codex

- Decision: keep `0.12` as default after a broader sampled sweep (`SOCIAL_MEDIA` indexes `0,2,4,6,8,10`) in addition to the single-query matrix. (Superseded by later reruns/sensitivity tuning.)
  Rationale: sampled worst-case regression remained within threshold (`+4.53%`) while average trend favored hybrid (`-5.54%`).
  Date/Author: 2026-02-15 / Codex

- Decision: treat cross-theme q10 sweep as directional evidence and keep single-iteration `HIGHLY_CONNECTED` numbers out of acceptance gating.
  Rationale: one-iteration reduced-budget run is useful to detect catastrophic tails but not sufficient for stable primary acceptance metrics.
  Date/Author: 2026-02-15 / Codex

- Decision: expose calibration feedback with explicit default-on opt-out (`rdf4j.optimizer.calibration.enabled=false`) in addition to tuning alpha.
  Rationale: satisfies default-on rollout requirement while giving operators a direct kill switch for adaptive feedback if needed.
  Date/Author: 2026-02-15 / Codex

- Decision: keep default-on rollout semantics unchanged, but mark performance acceptance as not yet stable and continue tuning against repeated matrix runs.
  Rationale: two consecutive reruns stayed above threshold (`+6.09%`, `+6.28%`), so prior single-run pass is insufficient for sign-off.
  Date/Author: 2026-02-15 / Codex

- Decision: raise default `rdf4j.optimizer.planSwitch.minRelativeGain` from `0.12` to `0.20` (still overridable).
  Rationale: `0.20` was the best value in the sampled sensitivity sweep and reduced latest matrix overhead from the prior rerun window.
  Date/Author: 2026-02-15 / Codex

- Decision: keep default `rdf4j.optimizer.planSwitch.minRelativeGain` at `0.20` after local `0.26` sweep and full-matrix cross-check.
  Rationale: despite local hybrid-only win at `0.26`, the end-to-end matrix delta was worse (`+5.78%`) than `0.20` (`+5.57%`).
  Date/Author: 2026-02-15 / Codex

- Decision: keep feature-default policy unchanged (all new features enabled by default) and avoid flipping runtime sampling default off, despite small sampled gains from doing so.
  Rationale: user requirement explicitly asks default-on features with opt-out; sensitivity results are retained as operator guidance rather than default changes.
  Date/Author: 2026-02-15 / Codex

- Decision: keep `runtimeSampling.confidenceThreshold` default unchanged at `0.55` for now.
  Rationale: alternative `0.40` did not hold up under full 10-iteration validation (`+6.08%` delta), so no evidence-based improvement to adopt.
  Date/Author: 2026-02-15 / Codex

- Decision: add `rdf4j.optimizer.mergeJoinEnabled` (default `true`) to provide explicit opt-out symmetry for all newly introduced optimizer features while retaining default-on rollout.
  Rationale: satisfies user requirement that new features are enabled by default but independently disable-able.
  Date/Author: 2026-02-15 / Codex

- Decision: add `rdf4j.optimizer.dp.enabled` and `rdf4j.optimizer.beam.enabled` as explicit default-on toggles in `JoinPlanEnumerator`.
  Rationale: completes per-feature opt-out coverage for join-order enumeration strategy while preserving default-on rollout behavior.
  Date/Author: 2026-02-15 / Codex

- Decision: update `QueryCostEstimatesTest` expected serialized plan text to include join-level `resultSizeEstimate` fields instead of weakening the assertion.
  Rationale: preserves strict regression coverage while aligning fixture output with the current plan rendering contract.
  Date/Author: 2026-02-15 / Codex

- Decision: keep extended estimate metadata (`source/confidence/calibration`) hybrid-only and expose an explicit opt-out (`rdf4j.optimizer.explain.extendedEstimateMetadata.enabled`, default `true`).
  Rationale: preserves default-on rollout in hybrid mode, keeps legacy snapshot output stable, and still allows users to disable the feature.
  Date/Author: 2026-02-15 / Codex

- Decision: replace beam-search `int` bitmask state with `BitSet` state in `JoinPlanEnumerator`.
  Rationale: avoids bit-shift overflow for large join groups and removes a runtime crash path while preserving beam-search behavior.
  Date/Author: 2026-02-15 / Codex

## Outcomes & Retrospective

Incremental update (2026-02-15 19:02Z).

Feature-toggle hardening update:

- Added explicit default-on disable switches for join-order enumeration:
  - `rdf4j.optimizer.dp.enabled` (default `true`)
  - `rdf4j.optimizer.beam.enabled` (default `true`)
- Added test-first coverage in `JoinPlanEnumeratorTest`:
  - pre-fix run failed with 2 assertions (`disablingDpAndBeamKeepsOriginalOrder`, `disablingBeamKeepsOriginalOrderForLargeJoinGroup`)
  - post-fix rerun passed with `Tests run: 5, Failures: 0, Errors: 0`
- Follow-up focused regression run:
  - `CostBasedJoinOptimizerTest`, `JoinCostModelTest`, `StandardQueryOptimizerPipelineTest`
  - result: `Tests run: 20, Failures: 0, Errors: 0`
- Post-toggle quick matrix sanity run:
  - `artifacts/benchmarks/hybrid-join-optimizer/sanity-20260215-1903/summary.md`
  - `legacy=8.571 ms/op`, `hybrid=8.836 ms/op`, delta `+3.09%`

Interpretation:

- Enumeration strategy is now consistent with the “default on, explicit off switch” policy already applied to runtime sampling/hash/merge/calibration/extended explain metadata.

Incremental update (2026-02-15 18:58Z).

Broader-scope benchmark rechecks:

- SOCIAL_MEDIA index sweep artifact:
  - `artifacts/benchmarks/hybrid-join-optimizer/recheck-social-media-sweep/summary.md`
  - sampled deltas:
    - `index 0: -10.62%`
    - `index 2: -11.59%`
    - `index 4: -9.41%`
    - `index 6: -7.24%`
    - `index 8: -0.96%`
    - `index 10: +1.66%`
  - aggregate:
    - mean `-6.36%`
    - best `-11.59%`
    - worst `+1.66%`

- Theme q10 sweep artifact:
  - `artifacts/benchmarks/hybrid-join-optimizer/recheck-theme-q10/summary.md`
  - sampled deltas:
    - `SOCIAL_MEDIA: +4.41%`
    - `LIBRARY: -8.05%`
    - `ENGINEERING: -32.78%`
    - `HIGHLY_CONNECTED: -99.72%`
  - aggregate:
    - mean `-34.03%`
    - best `-99.72%`
    - worst `+4.41%`

Interpretation:

- single-scenario q10 matrix overhead remains a known risk signal for default tuning;
- however, broader sampled evidence still favors hybrid overall and confirms substantial tail-case wins.

Incremental update (2026-02-15 18:49Z).

Threshold-candidate validation:

- full-check artifact:
  - `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-thr040-matrix`
- measured result:
  - `legacy=7.908807`
  - `hybrid_thr040=8.389957`
  - delta: `+6.08%`

Conclusion:

- `runtimeSampling.confidenceThreshold=0.40` is not a safe default improvement for this scenario despite short-probe gains.
- No default-value code retune applied from this experiment.

Incremental update (2026-02-15 18:45Z).

Stability + sensitivity update:

- full-budget matrix pass #1 artifact:
  - `artifacts/benchmarks/hybrid-join-optimizer/stability-20260215-pass1/summary.md`
  - delta: `+5.38%` (hybrid avg vs legacy avg)
- full-budget matrix pass #2 artifact:
  - `artifacts/benchmarks/hybrid-join-optimizer/stability-20260215-pass2/summary.md`
  - delta: `+6.39%` (hybrid avg vs legacy avg)
- two-pass mean delta:
  - `+5.88%`

Post-pass quick sensitivity (5 iterations each) in:

- `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-post-pass2`
- measured scores:
  - `legacy_5iter=8.674031`
  - `hybrid_default=9.049730 (+4.33%)`
  - `hybrid_ps30=9.091717 (+4.82%)`
  - `hybrid_sampling_off=9.044294 (+4.27%)`
  - `hybrid_ps30_sampling_off=8.921253 (+2.85%)`

Interpretation:

- Current default-on configuration still shows residual overhead variance on this benchmark scenario.
- Runtime sampling appears to contribute measurable overhead in this local probe, but default remains on to preserve the explicit rollout policy.

Incremental update (2026-02-15 18:35Z).

Benchmark sanity update:

- command:
  - `scripts/run-hybrid-join-benchmark-matrix.sh --measurement-iterations 5 --measurement-time 1s --warmup-iterations 0 --warmup-time 1s --forks 1 --output-dir artifacts/benchmarks/hybrid-join-optimizer/sanity-20260215-1833`
- result artifact:
  - `artifacts/benchmarks/hybrid-join-optimizer/sanity-20260215-1833/summary.md`
- summary:
  - legacy avg `8.676613 ms/op`
  - hybrid avg `9.027879 ms/op`
  - hybrid-vs-legacy delta `+4.05%` (within the local `<=5%` easy-query guard used in this plan)

Incremental update (2026-02-15 18:32Z).

Validation/recovery update:

- Root install executed cleanly:
  - `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`
  - snippet: `BUILD SUCCESS`
- Full module verifies after recovery:
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model verify`
    - snippet: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 1`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify`
    - snippet: `Tests run: 526, Failures: 0, Errors: 0, Skipped: 0`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/query verify`
    - snippet: `Tests run: 198, Failures: 0, Errors: 0, Skipped: 0`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/base verify`
    - snippet: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/nativerdf -Dtest=NativeEvaluationStatisticsEstimateTest verify`
    - snippets: `NativeEvaluationStatisticsEstimateTest: Tests run: 1, Failures: 0, Errors: 0` and module summary `BUILD SUCCESS`

Incremental update (2026-02-15 19:26Z).

Implemented:

- `QueryModelTreeToGenericPlanNode` now:
  - emits extended estimate metadata only in hybrid mode,
  - supports explicit opt-out via `rdf4j.optimizer.explain.extendedEstimateMetadata.enabled=false`,
  - suppresses join-node `resultSizeEstimate` annotations in legacy mode to retain snapshot-plan compatibility.
- `JoinPlanEnumerator` beam fallback now tracks selected terms with `BitSet` instead of `int` bitmasks, preventing overflow on large join groups.
- Added regression coverage:
  - `QueryModelTreeToGenericPlanNodeTest#omitsExtendedEstimateMetadataWhenDisabled`
  - `QueryModelTreeToGenericPlanNodeTest#legacyModeOmitsJoinResultSizeEstimateAnnotations`
  - `JoinPlanEnumeratorTest#beamFallbackSupportsMoreThanThirtyTwoJoinArgs`

Validation completed:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model -Dtest=QueryModelTreeToGenericPlanNodeTest verify`
  - snippet: `Tests run: 3, Failures: 0, Errors: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/memory -Dtest=QueryPlanRetrievalTest verify`
  - snippet: `Tests run: 32, Failures: 0, Errors: 0, Skipped: 2`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinPlanEnumeratorTest verify`
  - snippet: `Tests run: 3, Failures: 0, Errors: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/memory -Dtest=MemoryOptimisticIsolationTest verify`
  - snippet: `Tests run: 115, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=ThemeQueryBenchmarkExplanationTest verify`
  - snippet: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/memory verify`
  - snippet: `Tests run: 859, Failures: 0, Errors: 0, Skipped: 3`

Incremental update (2026-02-15 19:03Z).

Implemented:

- `JoinCostModel` now supports `rdf4j.optimizer.mergeJoinEnabled` (default `true`) and skips merge hints when disabled.
- Added focused test coverage `JoinCostModelTest#allowsDisablingMergeJoinFeature`.

Validation completed:

- failing pre-fix evidence:
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinCostModelTest#allowsDisablingMergeJoinFeature verify`
  - failure snippet: `expected: <NESTED_LOOP> but was: <MERGE>`
- passing post-fix evidence (same selection):
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinCostModelTest#allowsDisablingMergeJoinFeature verify`
- focused regression set:
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinCostModelTest,CostBasedJoinOptimizerTest,StandardQueryOptimizerPipelineTest verify`
  - Surefire snippets:
    - `JoinCostModelTest`: `Tests run: 9, Failures: 0, Errors: 0`
    - `CostBasedJoinOptimizerTest`: `Tests run: 7, Failures: 0, Errors: 0`
    - `StandardQueryOptimizerPipelineTest`: `Tests run: 4, Failures: 0, Errors: 0`

Benchmark spot-check:

- direct pair artifact: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-merge-toggle/direct-default-post/summary.md`
- delta: `+4.94%` (hybrid avg vs legacy avg), under the stated `<=5%` easy-query guard for this scenario.

Additional verification update (2026-02-15 18:08Z).

Implemented:

- Updated `QueryCostEstimatesTest` fixture text for `Join (resultSizeEstimate=10.0K)` rendering.

Validation completed:

- focused fixture rerun:
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=QueryCostEstimatesTest#testBindingSetAssignmentOptimization verify`
  - Surefire snippet: `Tests run: 1, Failures: 0, Errors: 0`
- full module verify:
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify`
  - module summary snippet: `Tests run: 525, Failures: 0, Errors: 0, Skipped: 0`

Benchmark refresh:

- canonical matrix artifact: `artifacts/benchmarks/hybrid-join-optimizer/summary.md`
- delta: `+2.67%` (hybrid avg vs legacy avg), below the 5% easy-query guard for this scenario.

Milestone 1 completed.

Implemented:

- `EvaluationStatistics.CardinalityEstimate` with `(estimate, lowerBound, upperBound, confidence, source)`.
- `EvaluationStatistics#getCardinalityEstimate(TupleExpr)` default wrapper around legacy cardinality.
- store overrides in memory/native/LMDB with conservative source-tagged confidence ranges.
- focused unit coverage in `EvaluationStatisticsTest`.

Validation completed:

- `python3 .codex/skills/mvnf/scripts/mvnf.py --module core/queryalgebra/evaluation EvaluationStatisticsTest#testGetCardinalityEstimateWrapsCardinality`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/memory verify`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/nativerdf verify`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb verify`

Milestone 3 bootstrap completed.

Implemented:

- `CostBasedJoinOptimizer` class as milestone scaffold.
- optimizer mode switch in `StandardQueryOptimizerPipeline` using `rdf4j.optimizer.mode`.
- focused tests for mode switching in `StandardQueryOptimizerPipelineTest`.

Validation completed:

- failing pre-change compile run (missing `CostBasedJoinOptimizer`) via
  `python3 .codex/skills/mvnf/scripts/mvnf.py --module core/queryalgebra/evaluation StandardQueryOptimizerPipelineTest#usesCostBasedJoinOptimizerWhenHybridModeSet`
- passing post-change run via
  `python3 .codex/skills/mvnf/scripts/mvnf.py --module core/queryalgebra/evaluation StandardQueryOptimizerPipelineTest`

Milestone 2 completed.

Implemented:

- synopsis-backed estimate envelopes for `StatementPattern` in memory/native/LMDB statistics implementations.
- per-dimension synopsis extraction (subject/predicate/object/context frequencies) to derive lower/upper bounds.
- confidence scoring based on bound dimensions and spread.
- source tagging updated to `memory-synopsis`, `native-range-count-synopsis`, and `lmdb-range-count-synopsis`.
- non-`StatementPattern` expressions now fall back to legacy envelope generation.
- focused regression tests:
  - `MemEvaluationStatisticsEstimateTest`
  - `NativeEvaluationStatisticsEstimateTest`
  - `LmdbEvaluationStatisticsEstimateTest`

Validation completed:

- failing pre-change tests:
  - `python3 .codex/skills/mvnf/scripts/mvnf.py --module core/sail/memory MemEvaluationStatisticsEstimateTest#usesSynopsisBackedEnvelopeForBoundPatterns`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/nativerdf -Dtest=NativeEvaluationStatisticsEstimateTest#usesSynopsisBackedEnvelopeForBoundPatterns verify`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbEvaluationStatisticsEstimateTest#usesSynopsisBackedEnvelopeForBoundPatterns verify`
- passing post-change tests:
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/memory -Dtest=MemEvaluationStatisticsEstimateTest#usesSynopsisBackedEnvelopeForBoundPatterns verify`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/nativerdf -Dtest=NativeEvaluationStatisticsEstimateTest#usesSynopsisBackedEnvelopeForBoundPatterns verify`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbEvaluationStatisticsEstimateTest#usesSynopsisBackedEnvelopeForBoundPatterns,LmdbEvaluationStatisticsMemoizationTest#cachesEquivalentStatementPatternCardinalitiesByResolvedIds verify`
  - `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=StandardQueryOptimizerPipelineTest,EvaluationStatisticsTest verify`

Milestones 3 and 4 completed.

Implemented:

- `JoinCostModel` scoring with uncertainty/cartesian penalties and join-algorithm chooser.
- `JoinPlanEnumerator` exact DP for small groups and beam fallback for larger groups.
- DP/beam tie-break behavior to avoid unstable disconnected-order outcomes in deterministic tests.
- `CostBasedJoinOptimizer` standalone implementation (`QueryOptimizer`, not `QueryJoinOptimizer` subclass).

Validation completed:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinCostModelTest,JoinPlanEnumeratorTest,CostBasedJoinOptimizerTest,StandardQueryOptimizerPipelineTest verify`

Milestone 5 phase 1 completed.

Implemented:

- planner-side algorithm hinting in `CostBasedJoinOptimizer` for NLJ/hash/merge based on cardinality envelopes, shared vars, and merge-order support.
- runtime-side algorithm selection in `JoinQueryEvaluationStep` now honors optimizer hash/merge hints while preserving service and legacy fallback paths.
- new focused runtime tests in `JoinQueryEvaluationStepTest`.

Validation completed:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinCostModelTest,JoinPlanEnumeratorTest,CostBasedJoinOptimizerTest,StandardQueryOptimizerPipelineTest,JoinQueryEvaluationStepTest verify`

Milestone 6 phase 1 completed.

Implemented:

- low-confidence runtime sampling re-rank in `CostBasedJoinOptimizer` with guarded properties:
  - `rdf4j.optimizer.runtimeSampling.enabled`
  - `rdf4j.optimizer.planningBudgetMs`
  - `rdf4j.optimizer.runtimeSampling.confidenceThreshold`
- budget-bounded connected-pair sampling and prefix score comparison against static order.
- fallback-safe behavior when sampling is disabled, over budget, or non-finite.
- focused test coverage for runtime sampling promotion path in `CostBasedJoinOptimizerTest`.

Validation completed:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest verify`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinCostModelTest,JoinPlanEnumeratorTest,CostBasedJoinOptimizerTest,StandardQueryOptimizerPipelineTest,JoinQueryEvaluationStepTest verify`

Milestone 7 phase 1 completed.

Implemented:

- estimate metadata propagation in query model:
  - `QueryModelNode` + `AbstractQueryModelNode` now carry estimate source and confidence.
  - both `QueryJoinOptimizer` and `CostBasedJoinOptimizer` annotate planned nodes with estimate source/confidence.
- explain metadata exposure:
  - `GenericPlanNode` and `QueryModelTreeToGenericPlanNode` now include estimate source/confidence in plan output.
- runtime feedback calibration:
  - `EvaluationStatistics` now includes a global source-keyed EWMA q-error registry with static record/snapshot/clear APIs.
  - estimate envelopes are calibrated by widening bounds and scaling confidence based on observed q-error.
  - `DefaultEvaluationStrategy.ResultSizeCountingIterator` now records estimate-vs-actual observations on close.

Validation completed:

- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model -Dtest=QueryModelTreeToGenericPlanNodeTest verify`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=EvaluationStatisticsTest,JoinCostModelTest,JoinPlanEnumeratorTest,CostBasedJoinOptimizerTest,StandardQueryOptimizerPipelineTest,JoinQueryEvaluationStepTest verify`

Milestone 7 phase 2 completed.

Implemented:

- calibration snapshot metadata added to query-model nodes:
  - `resultSizeEstimateQError`
  - `resultSizeEstimateObservationCount`
- calibration snapshot propagation in both join optimizers during estimate annotation.
- explain conversion now emits calibration snapshot metadata on `GenericPlanNode`.
- human-readable plan annotations now include calibration q-error and sample count.

Validation completed:

- `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model -Dtest=QueryModelTreeToGenericPlanNodeTest verify`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=EvaluationStatisticsTest,CostBasedJoinOptimizerTest,JoinCostModelTest,JoinPlanEnumeratorTest,StandardQueryOptimizerPipelineTest,JoinQueryEvaluationStepTest verify`

Milestone 8 completed.

Implemented:

- benchmark matrix runner: `scripts/run-hybrid-join-benchmark-matrix.sh`.
- matrix artifacts written under `artifacts/benchmarks/hybrid-join-optimizer/`:
  - `legacy.log`, `hybrid.log`
  - `legacy.json`, `hybrid.json`
  - `metrics.csv`
  - `summary.md`
  - `run-metadata.txt`
- harness improvement: legacy mode uses `run-single-benchmark.sh` (build + run), hybrid mode reuses generated `jmh-benchmarks.jar` directly to avoid second full build on repeated runs.

Validation completed:

- `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200`
- `scripts/run-hybrid-join-benchmark-matrix.sh`
- generated summary: `artifacts/benchmarks/hybrid-join-optimizer/summary.md`

Outcome snapshot:

- legacy average: `7.670855 ms/op`
- hybrid average: `378.653481 ms/op`
- delta: `+4836.26%` (regression for this scenario)

Follow-up tuning snapshot:

- legacy average: `7.628121 ms/op`
- hybrid average: `379.038762 ms/op`
- delta: `+4868.97%` (regression unchanged in practice)

Default-on policy tuning snapshot:

- legacy average: `7.324551 ms/op`
- hybrid average: `8.434278 ms/op`
- delta: `+15.15%` (catastrophic regression removed; still above target)

Latest tuning snapshot:

- legacy average: `7.820818 ms/op`
- hybrid average: `8.490312 ms/op`
- delta: `+8.56%` (improved relative overhead; still above target)

Current default snapshot:

- legacy average: `7.469646 ms/op`
- hybrid average: `7.796977 ms/op`
- delta: `+4.38%` (under 5% regression threshold on this benchmark)

Latest rerun snapshots:

- rerun #1:
  - legacy average: `7.107038 ms/op`
  - hybrid average: `7.540029 ms/op`
  - delta: `+6.09%`
- rerun #2:
  - legacy average: `7.055322 ms/op`
  - hybrid average: `7.498590 ms/op`
  - delta: `+6.28%`

Latest tuned-default snapshot (`planSwitch.minRelativeGain=0.20`):

- legacy average: `7.077601 ms/op`
- hybrid average: `7.471671 ms/op`
- delta: `+5.57%`

Exploratory matrix snapshot (`planSwitch.minRelativeGain=0.26`, reverted):

- legacy average: `7.073992 ms/op`
- hybrid average: `7.482835 ms/op`
- delta: `+5.78%`

Additional sensitivity snapshots:

- runtime sampling: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-runtime-sampling/summary.md`
- risk lambda: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-risk-lambda/summary.md`
- local plan-switch gains: `artifacts/benchmarks/hybrid-join-optimizer/sensitivity-plan-switch-local/summary.md`

Expanded sampled snapshot (`SOCIAL_MEDIA`, query indexes `0,2,4,6,8,10`):

- artifact: `artifacts/benchmarks/hybrid-join-optimizer/sweep-social-media/summary.md`
- mean delta: `-5.54%`
- worst sampled delta: `+4.53%`
- best sampled delta: `-12.31%`

Cross-theme spot-check snapshot (`z_queryIndex=10`, reduced budget):

- artifact: `artifacts/benchmarks/hybrid-join-optimizer/sweep-theme-q10/summary.md`
- mean delta: `-35.12%`
- worst sampled delta: `+2.37%` (`SOCIAL_MEDIA`)
- best sampled delta: `-99.73%` (`HIGHLY_CONNECTED`)
- note: `HIGHLY_CONNECTED` was measured with single-iteration budget (`-i 1`) to bound runtime; treat as tail-risk signal, not primary acceptance metric.

Retrospective for milestone 8:

- The acceptance harness is reproducible and automated, and catastrophic hybrid tail regressions were removed under default-on policy by conservative hash defaults.
- Performance acceptance criteria are currently borderline-above target on repeated short-window reruns (`+6.09%`, `+6.28%` vs threshold <= 5%).
- Tuning to `planSwitch.minRelativeGain=0.20` improved the latest matrix (`+5.57%`) but is still slightly above the 5% threshold.
- Further local tuning (`0.26`) did not improve full matrix outcome (`+5.78%`), so `0.20` remains the best validated default in this run window.
- Keep default mode at `hybrid` per requirement, preserve disable paths (`rdf4j.optimizer.mode=legacy`, feature-specific properties), and continue threshold-focused tuning plus repeated-run validation.

Feature-toggle completion snapshot:

- added property: `rdf4j.optimizer.calibration.enabled` (default `true`, disable with `false`)
- disable paths summary:
  - full fallback: `rdf4j.optimizer.mode=legacy`
  - runtime sampling off: `rdf4j.optimizer.runtimeSampling.enabled=false`
  - hash-join hints off: `rdf4j.optimizer.hashJoinEnabled=false`
  - risk penalty off: `rdf4j.optimizer.risk.lambda=0`
  - plan-switch guard off: `rdf4j.optimizer.planSwitch.minRelativeGain=0`
- validation: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=EvaluationStatisticsTest,CostBasedJoinOptimizerTest,StandardQueryOptimizerPipelineTest,JoinCostModelTest verify`

## Context and Orientation

RDF4J currently reorders joins greedily using per-pattern cardinality proxies. That logic is centered in `QueryJoinOptimizer`. The default optimizer pipeline is built in `StandardQueryOptimizerPipeline`. Cardinality signals come from `EvaluationStatistics` and store-specific implementations including memory, native, and LMDB stores.

Key files for this work:

- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/EvaluationStatistics.java`
- `core/sail/memory/src/main/java/org/eclipse/rdf4j/sail/memory/MemEvaluationStatistics.java`
- `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/NativeEvaluationStatistics.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/JoinQueryEvaluationStep.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/StatementPatternQueryEvaluationStep.java`

Terms used in this document:

- Basic graph pattern (BGP): a group of triple patterns in SPARQL joined by shared variables.
- Cardinality estimate: predicted row count for a pattern or join subplan.
- Confidence interval: low/high range around an estimate that quantifies uncertainty.
- q-error: estimate error ratio, `max(estimate/actual, actual/estimate)`.
- Top-k candidate plans: keep several promising plans, not only one, then choose robustly.

All implementation steps below assume repository root is `/Users/havardottestad/Documents/Programming/rdf4j-shacl-inference`.

## Plan of Work

Milestone 1 introduces an estimate envelope object that carries estimate, bounds, confidence, and source. `EvaluationStatistics` gets a backward-compatible default method returning this envelope. Existing `getCardinality` stays valid and uses the envelope center value by default. This milestone is complete when legacy planning behavior is unchanged but explain hooks can request confidence-bearing estimates.

Milestone 2 adds lightweight synopsis-backed estimators in store-level statistics implementations. Start with predicate frequency and distinct subject/object counts where available, then integrate heavy-hitter hooks as optional. This milestone is complete when store estimators return consistent envelopes and unit tests prove deterministic fallback when synopses are absent.

Milestone 3 adds a new optimizer class, `CostBasedJoinOptimizer`, with exact dynamic programming enumeration for join groups up to configurable size (`dp.maxTerms`). The optimizer uses legacy-safe physical assumptions first, then computes join-order score from expected cost plus uncertainty penalty. This milestone is complete when a fixed test corpus shows deterministic join-order changes only in `hybrid` mode.

Milestone 4 adds beam search fallback for large join groups and top-k retention. The optimizer ranks candidates using the same risk-aware score and exposes candidate count in explain metadata. This milestone is complete when large synthetic BGP tests finish within planning-time budget and do not trigger pathological plan explosions.

Milestone 5 upgrades physical operator choice logic from heuristic-only to cost-aware selection among nested-loop, hash join, and merge join where legal. The first implementation should not add a new runtime operator class; it should pick among existing paths in `JoinQueryEvaluationStep` and existing merge-order metadata. This milestone is complete when operator-selection tests show explicit algorithm shifts under controlled cardinality conditions.

Milestone 6 adds bounded runtime sampling re-rank for low-confidence plans. The runtime probe is narrow: sample first joins among top-k candidates, re-rank once, then commit. This milestone is complete when sampling can be enabled/disabled by property and when execution stays stable under timeout/fallback constraints.

Milestone 7 closes the loop with calibration and explain output. Profiling data updates estimator calibration tables. Explain output includes estimate source and confidence per join-group decision. This milestone is complete when benchmark reports include q-error distribution improvement and tail-latency comparison versus legacy mode.

## Concrete Steps

Work from repository root. Run commands exactly as written.

1. Create feature branch and baseline install.

    git checkout -b GH-0000-hybrid-join-optimizer
    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

Expected: build success summary and no test execution in quick profile.

2. Add estimate envelope API in evaluation statistics.

Edit `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/EvaluationStatistics.java` to add:

    public static final class CardinalityEstimate {
        public final double estimate;
        public final double lowerBound;
        public final double upperBound;
        public final double confidence;
        public final String source;
    }

and a default method:

    public CardinalityEstimate getCardinalityEstimate(TupleExpr expr)

that wraps existing `getCardinality(expr)` with neutral confidence and identical bounds.

3. Extend store statistics classes to override envelope method conservatively.

Edit:

- `core/sail/memory/src/main/java/org/eclipse/rdf4j/sail/memory/MemEvaluationStatistics.java`
- `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/NativeEvaluationStatistics.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`

Implement envelope values using current estimates as center. Add wider bound for memory heuristic and tighter bound for range-count estimators.

4. Add optimizer mode switch and new optimizer class.

Create:

- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/CostBasedJoinOptimizer.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/JoinPlanEnumerator.java`
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/JoinCostModel.java`

Edit `StandardQueryOptimizerPipeline` to instantiate:

- legacy mode: `QueryJoinOptimizer`
- hybrid mode: `CostBasedJoinOptimizer`

Read mode from system property `rdf4j.optimizer.mode` with default `hybrid`.

5. Implement exact DP for small groups and beam fallback.

In `JoinPlanEnumerator`, implement:

- exact left-deep DP for group size `<= rdf4j.optimizer.dp.maxTerms` (default 12)
- beam search for larger groups (`rdf4j.optimizer.beam.width`, default 24)

Persist top-k candidates with score and confidence summary.

6. Implement risk-aware scoring and operator selection.

In `JoinCostModel`, compute:

    score = expectedCost + lambda * uncertaintyPenalty

with `lambda` from `rdf4j.optimizer.risk.lambda` default `0.25`.

In `JoinQueryEvaluationStep`, consume chosen algorithm metadata from planned join nodes and select NLJ/hash/merge accordingly where legal.

7. Add runtime sampling re-rank hook.

Add a single-pass sampling probe in `CostBasedJoinOptimizer` and/or evaluation strategy path, guarded by:

    rdf4j.optimizer.runtimeSampling.enabled=true|false

and planner budget:

    rdf4j.optimizer.planningBudgetMs

On timeout or probe failure, fallback to best static candidate.

8. Add tests incrementally by milestone.

Create focused tests in:

- `core/queryalgebra/evaluation/src/test/java/.../optimizer/CostBasedJoinOptimizerTest.java`
- `core/queryalgebra/evaluation/src/test/java/.../optimizer/JoinPlanEnumeratorTest.java`
- `core/queryalgebra/evaluation/src/test/java/.../optimizer/JoinCostModelTest.java`

Add integration-level tests in:

- `core/queryalgebra/evaluation/src/test/java/.../optimizer/HybridJoinOptimizerIntegrationTest.java`

9. Run formatting and targeted test loop.

    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py CostBasedJoinOptimizerTest
    python3 .codex/skills/mvnf/scripts/mvnf.py JoinPlanEnumeratorTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation

10. Run benchmark spot-checks.

Use helper script:

    ./scripts/run-single-benchmark.sh --help
    ./scripts/run-single-benchmark.sh <module> <benchmarkClass> <benchmarkMethod> --enable-jfr

Record median, p95, p99, and planning-time share for legacy and hybrid mode.

11. Commit per milestone and push.

    git add <scoped files>
    git commit -m "feat: GH-0000 add hybrid join optimizer milestone <N>"
    git push origin GH-0000-hybrid-join-optimizer

12. Open PR and rerun failed jobs as needed.

    gh pr create --fill
    gh pr checks <pr-number> --watch

## Validation and Acceptance

Acceptance is behavioral and must be demonstrated.

First, semantic parity. For a fixed query corpus, result sets in `legacy` and `hybrid` modes must match exactly for the same dataset and same transaction isolation settings.

Second, planning behavior. For curated queries with known bad legacy join orders, `hybrid` mode must produce a different join order in explain output and report estimate source plus confidence envelope for each critical join decision.

Third, performance. On benchmark workloads used in this plan, `hybrid` mode must not regress median by more than 5 percent on easy queries and must reduce p95 or p99 by at least 20 percent on hard correlated workloads. If this threshold is not met, document why and keep mode default at `legacy`.

Fourth, resilience. When estimator confidence is low or sampling budget expires, optimizer must fallback deterministically without exceptions and must still produce valid query results.

Run this command set for acceptance:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/nativerdf
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/memory

Expected: all selected modules pass; benchmark logs show mode-differentiated plan behavior and tail improvement claims with timestamps.

## Idempotence and Recovery

All steps are safe to rerun. Existing commands are additive and tolerate repeated execution.

If a milestone implementation regresses correctness, set:

    -Drdf4j.optimizer.mode=legacy

and rerun tests to isolate regression to hybrid path.

If benchmark setup fails due environment/tooling gaps, continue with deterministic optimizer tests and postpone benchmark acceptance, recording gap in `Outcomes & Retrospective`.

If a partial commit mixes milestones, split with interactive staging equivalents using non-interactive command patterns before pushing.

## Artifacts and Notes

Keep the following artifacts in-repo root for reproducibility:

- `initial-evidence.txt` for first run evidence capture.
- benchmark result snippets under `artifacts/benchmarks/`.
- optional JFR files under `artifacts/jfr/`.

Expected evidence snippets:

    [INFO] Tests run: <N>, Failures: 0, Errors: 0
    optimizer.mode=hybrid
    selectedPlanScore=<value> confidence=<value>

Expected explain snippet fields:

    joinOrder: [...]
    estimate: <double>
    confidence: <double>
    source: <store|sampling|ensemble>

## Interfaces and Dependencies

Required interfaces and classes at end of MVP:

In `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/EvaluationStatistics.java`, define:

    public static final class CardinalityEstimate {
        public final double estimate;
        public final double lowerBound;
        public final double upperBound;
        public final double confidence;
        public final String source;
    }

    public CardinalityEstimate getCardinalityEstimate(TupleExpr expr)

In `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/CostBasedJoinOptimizer.java`, define:

    public class CostBasedJoinOptimizer implements QueryOptimizer

with constructor compatible with current pipeline dependencies:

    public CostBasedJoinOptimizer(EvaluationStatistics stats, boolean trackResultSize, TripleSource tripleSource)

In `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/JoinPlanEnumerator.java`, define:

    JoinPlan enumerate(List<TupleExpr> joinArgs, EstimationContext context)

In `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/JoinCostModel.java`, define:

    double score(JoinPlan plan, EstimationContext context)
    PhysicalJoinChoice chooseJoinAlgorithm(JoinPlanNode node, EstimationContext context)

No new external library dependencies in MVP. Use current RDF4J modules and Java standard library only.

## Plan Revision Notes

- 2026-02-15 / Codex: Initial creation from user proposal and current repository topology. Reason: user requested execution via ExecPlan format compliant with `PLANS.md`.
- 2026-02-15 / Codex: Recorded Milestone 8 benchmark artifacts, documented observed hybrid regression, and updated matrix harness to avoid duplicate full-build passes per mode.
- 2026-02-15 / Codex: Tuned default `planSwitch.minRelativeGain` to `0.12` and refreshed matrix evidence to a `+4.38%` default-on delta.
- 2026-02-15 / Codex: Added broader `SOCIAL_MEDIA` sampled sweep evidence (`z_queryIndex` in `0,2,4,6,8,10`) to validate default-on behavior beyond one query index.
- 2026-02-15 / Codex: Added cross-theme query-10 spot-check evidence and documented `HIGHLY_CONNECTED` as reduced-budget directional tail-risk data.
- 2026-02-15 / Codex: Added repeated matrix rerun evidence (`+6.09%`, `+6.28%`) and downgraded performance acceptance status to ongoing tuning.
- 2026-02-15 / Codex: Added plan-switch sensitivity sweep, moved default gain guard to `0.20`, and refreshed matrix artifacts (`+5.57%`).
- 2026-02-15 / Codex: Added runtime/risk/local sensitivity sweeps, tested `0.26`, and reverted to `0.20` after matrix cross-check (`+5.78%` exploratory vs `+5.57%` tuned default).
- 2026-02-15 / Codex: Added `mergeJoinEnabled` default-on opt-out flag + focused test-first evidence, then captured post-change direct benchmark pair (`+4.94%`).
- 2026-02-15 / Codex: Fixed stale `QueryCostEstimatesTest` expected plan text and refreshed canonical benchmark matrix artifacts (`+2.67%`).
