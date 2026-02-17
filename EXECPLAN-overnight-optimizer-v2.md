# Deliver Adaptive Runtime Re-Optimization, Bushy Search, Persistent Calibration, and Plan-Decision Explainability

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

`PLANS.md` exists at repository root and this plan is maintained according to `PLANS.md`.

This plan builds on `EXECPLAN-hybrid-join-optimizer.md` for existing hybrid optimizer context. This document is self-contained for the four new features requested here.

## Purpose / Big Picture

After this change, RDF4J will do four new things by default: runtime adaptive join re-optimization, bushy join-plan search, persistent cardinality calibration across process restarts, and explain output that states why specific plan decisions were made. A user can disable each feature independently with system properties if a deployment needs conservative rollback.

User-visible outcomes:

1. Query execution adapts join algorithm choice at runtime based on sampled left-input behavior.
2. Planner can build non-left-deep join trees (bushy trees) when beneficial.
3. Calibration q-error data survives restart and continues improving estimates.
4. `EXPLAIN` shows decision rationale and rejected alternatives summary.

## Progress

- [x] (2026-02-16 00:47Z) Created ExecPlan for requested features 1-4.
- [x] (2026-02-16 00:47Z) Incorporated rollout policy: default-on + per-feature disable switches.
- [x] (2026-02-16 21:23Z) Implemented feature 1 runtime adaptive re-optimization and verified with `JoinQueryEvaluationStepTest`.
- [x] (2026-02-16 21:29Z) Implemented feature 2 bushy-plan optimizer with default-on and disable property.
- [x] (2026-02-16 21:32Z) Implemented feature 3 persistent calibration storage/loading with default-on and disable property.
- [x] (2026-02-16 21:36Z) Implemented feature 4 explain decision diagnostics with default-on and disable property.
- [x] (2026-02-16 21:41Z) Ran module verifies for touched modules (`core/queryalgebra/evaluation`, `core/queryalgebra/model`, `core/query`).
- [x] (2026-02-16 21:42Z) Captured implementation evidence and rollout notes in this ExecPlan.
- [x] (2026-02-16 21:44Z) Added required Codex signature comments on touched Java files and reran focused verification.
- [x] (2026-02-16 21:49Z) Added optimization hardening wave tasks (5-8).
- [x] (2026-02-16 21:50Z) Implemented task 5 buffered calibration persistence flush controls with focused tests.
- [x] (2026-02-16 21:51Z) Implemented task 6 bounded calibration state size with deterministic eviction.
- [x] (2026-02-16 21:54Z) Implemented task 7 adaptive runtime nested-loop cacheability hints.
- [x] (2026-02-16 23:03Z) Implemented task 8 runtime-sampling candidate pruning controls with default-on + disable switch.
- [x] (2026-02-16 23:03Z) Added next-wave critical optimization tasks (9-12) to this ExecPlan.
- [x] (2026-02-16 23:08Z) Implemented task 9 runtime-sampling candidate diversity pruning with default-on + disable switch.
- [x] (2026-02-16 23:18Z) Implemented task 10 shared per-query join-estimate memoization across planner phases.
- [x] (2026-02-16 23:18Z) Updated diversity-pruning test isolation to disable memoization while validating diversity-specific impact.
- [x] (2026-02-16 23:19Z) Added next-wave critical optimization tasks (13-16) to this ExecPlan.
- [x] (2026-02-16 23:33Z) Implemented task 11 adaptive planning-budget scaling by join-group width + recent planner latency.
- [x] (2026-02-16 23:33Z) Implemented task 12 calibration persistence compaction/version checks with corruption-safe unsupported-version fallback.
- [x] (2026-02-16 23:39Z) Implemented task 13 bounded join-estimate memoization cache cardinality with deterministic eviction.
- [x] (2026-02-16 23:44Z) Implemented task 14 planner telemetry counters in join decision diagnostics (estimate calls, memoization hit-rate, runtime-sampling ms).
- [x] (2026-02-16 23:49Z) Implemented task 15 connected-component join-group partitioning with default-on + disable switch.
- [x] (2026-02-16 23:49Z) Added next-wave critical optimization tasks (25-28).
- [x] (2026-02-16 23:55Z) Implemented task 16 staged runtime-sampling budgets (`candidateGenerationBudgetMs`, `prefixSamplingBudgetMs`) with hard caps and disable switch.
- [x] (2026-02-16 23:55Z) Added next-wave critical optimization tasks (29-32).
- [x] (2026-02-17 00:03Z) Implemented task 17 per-query join-plan reuse cache with normalized join-group signature + binding-profile key and default-on disable switch.
- [x] (2026-02-17 00:03Z) Added next-wave critical optimization tasks (33-36).
- [x] (2026-02-17 00:12Z) Implemented task 18 adaptive beam-width control with default-on + disable switch and focused verification.
- [x] (2026-02-17 00:17Z) Implemented task 19 online algorithm-threshold tuning from observed runtime costs with default-on + disable switch.
- [x] (2026-02-17 00:18Z) Added next-wave critical optimization tasks (37-40).
- [x] (2026-02-17 00:26Z) Implemented task 20 bushy-subtree structural memoization with default-on + disable switch.
- [x] (2026-02-17 00:32Z) Implemented task 21 runtime-sampling join-estimate call cap (hard per-join-group budget) with default-on + disable switch.
- [x] (2026-02-17 00:33Z) Added next-wave critical optimization tasks (41-44).
- [x] (2026-02-17 00:36Z) Implemented task 22 join-output estimate reuse for algorithm-hint assignment with default-on + disable switch.
- [x] (2026-02-17 00:36Z) Added next-wave critical optimization tasks (45-48).
- [x] (2026-02-17 00:43Z) Implemented task 23 adaptive runtime-sampling auto-throttle with default-on + disable switch and focused verification.
- [x] (2026-02-17 00:50Z) Implemented task 24 cross-optimize warm join-plan cache with calibration-drift invalidation guardrails and default-on + disable switches.
- [x] (2026-02-17 00:50Z) Added next-wave critical optimization tasks (49-52).
- [x] (2026-02-17 01:00Z) Implemented task 25 join-estimate call-stage metering (`ordering`, `reranking`, `bushy`) in diagnostics with focused verification.
- [x] (2026-02-17 01:05Z) Implemented task 26 deterministic disconnected-component ordering heuristics (bound-vars first, then estimated output) with default-on + disable switch.
- [x] (2026-02-17 01:05Z) Added next-wave critical optimization tasks (53-56).
- [x] (2026-02-17 01:10Z) Implemented task 27 runtime-sampling memoization-hit-rate guardrails (default-on + disable switch) with focused verification.
- [x] (2026-02-17 01:10Z) Added next-wave critical optimization tasks (57-60).
- [x] (2026-02-17 01:16Z) Implemented task 58 memoization-guard diagnostics counters in explain output with focused verification.
- [x] (2026-02-17 01:16Z) Implemented task 59 high-q-error memoization-guard override for runtime-sampling reranking with focused verification.
- [x] (2026-02-17 01:16Z) Added next-wave critical optimization tasks (61-64).
- [x] (2026-02-17 01:22Z) Implemented task 60 disconnected-component ordering reuse cache (within optimize call) with default-on + disable switch and focused verification.
- [x] (2026-02-17 01:22Z) Added next-wave critical optimization tasks (65-68).
- [x] (2026-02-17 01:28Z) Implemented task 61 memoization-guard hysteresis (engage/disengage thresholds) with default-on + disable switch and focused verification.
- [x] (2026-02-17 01:28Z) Added next-wave critical optimization tasks (69-72).
- [x] (2026-02-17 01:35Z) Implemented task 62 source-weighted q-error override scoring for memoization-guard high-q-error bypass with default-on + disable switch.
- [x] (2026-02-17 01:35Z) Added next-wave critical optimization tasks (73-76).
- [x] (2026-02-17 01:40Z) Implemented task 63 runtime-sampling candidate-source attribution diagnostics (`baselineOnly`, `topKEnumerated`, `promotedPair`, `guardSkipped`) with focused verification.
- [x] (2026-02-17 01:40Z) Added next-wave critical optimization tasks (77-80).
- [x] (2026-02-17 01:46Z) Implemented task 64 bounded disconnected-component ordering reuse cache with deterministic eviction and max-entry property.
- [x] (2026-02-17 01:46Z) Implemented task 65 component-ordering reuse hit/miss diagnostics in explain decision details.
- [x] (2026-02-17 01:46Z) Added next-wave critical optimization tasks (81-84).

## Surprises & Discoveries

- Observation: The runtime join execution switchpoint is centralized in `JoinQueryEvaluationStep`, which allows adaptive behavior without changing parser or query model semantics.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/JoinQueryEvaluationStep.java` selects hash/merge/nested execution.

- Observation: Existing calibration storage is process-local static memory in `EvaluationStatistics`; persistence requires explicit serialization logic.
  Evidence: `CALIBRATION_STATES` in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/EvaluationStatistics.java` is a `ConcurrentHashMap` with no file I/O.

- Observation: Bushy rewrite can clash with legacy hint-preservation tests when existing join nodes already carry explicit hints.
  Evidence: `CostBasedJoinOptimizerTest.preservesLegacyHintForReassociatedThreeLeafSubtree` required skipping bushy-shape rewriting when preexisting explicit hints are detected.

- Observation: Calibration persistence can add avoidable overhead if every observation performs file I/O.
  Evidence: before task 5, `recordGlobalCardinalityObservation` called `persistCalibrationStates()` directly on every observation.

- Observation: Runtime sampling candidate generation can overwork join-estimation on larger join groups when `runtimeSampling.topK` is set high.
  Evidence: task-8 pre-fix failure in `CostBasedJoinOptimizerTest.runtimeSamplingCandidatePruningIsEnabledByDefaultAndCanBeDisabled` showed no reduction in join-estimate requests without explicit pruning controls.

- Observation: Diversity pruning must run even when the candidate list is already under cap; otherwise the diversity switch has no effect.
  Evidence: task-9 first implementation still failed `CostBasedJoinOptimizerTest.runtimeSamplingCandidateDiversityPruningIsEnabledByDefaultAndCanBeDisabled` until under-cap path was fixed.

- Observation: New join-estimate memoization can reduce the measured deltas in runtime-sampling pruning tests by collapsing duplicate estimate calls globally.
  Evidence: after task 10, class-level `CostBasedJoinOptimizerTest` initially failed diversity-pruning assertion until memoization was disabled inside that specific diversity measurement test.

- Observation: Calibration persistence without format metadata is brittle across payload drift and can silently accept incompatible data.
  Evidence: task-12 pre-fix failures in `EvaluationStatisticsTest.testCalibrationPersistenceWritesFormatVersionMetadata` and `EvaluationStatisticsTest.testCalibrationPersistenceSkipsUnsupportedFormatVersion`.

- Observation: Unbounded join-estimate memoization can flatten into unbounded memory growth during wide-join planning/reranking loops.
  Evidence: task-13 change from unbounded `HashMap` to bounded insertion-ordered cache in `JoinEstimateMemoizingStatistics`.

- Observation: explain diagnostics telemetry cannot be sourced from memoization wrapper internals unless planner always uses a telemetry-aware statistics delegate.
  Evidence: task-14 telemetry assertions in `CostBasedJoinOptimizerTest.annotatesJoinDecisionDiagnosticsByDefault` stayed false until `JoinEstimateMemoizingStatistics` became unconditional (memoization behavior still property-gated).

- Observation: disconnected join groups still triggered unnecessary join-estimation work because DP/runtime-sampling treated the whole flattened group as one search space.
  Evidence: task-15 pre-fix failure in `CostBasedJoinOptimizerTest.joinGroupPartitioningIsEnabledByDefaultAndCanBeDisabled`.

- Observation: stage budget properties do not constrain candidate-generation cost unless top-K enumeration is explicitly skipped for ultra-small candidate-generation windows.
  Evidence: task-16 implementation adds a hard guard that bypasses top-K enumeration when `candidateGenerationBudgetMs` is set to a 1ms micro-budget.

- Observation: join-plan reuse across equivalent UNION branches needs normalized term fingerprints with connectivity + scope-binding features; predicate-only signatures are too collision-prone for safe plan replay.
  Evidence: task-17 implementation in `CostBasedJoinOptimizer` adds `TermFingerprint`/`JoinGroupReuseKey` and validated reuse via `CostBasedJoinOptimizerTest.joinPlanReuseIsEnabledByDefaultAndCanBeDisabled`.

- Observation: adaptive beam-width expansion is effective under q-error pressure but needs conservative lower/upper bounds to avoid unnecessary planner blowup on stable workloads.
  Evidence: task-18 measurement in `CostBasedJoinOptimizerTest.adaptiveBeamWidthControlIsEnabledByDefaultAndCanBeDisabled` showed estimate-call expansion only when q-error is elevated.

- Observation: online threshold tuning can destabilize deterministic planning tests if adaptation kicks in with too few observations.
  Evidence: task-19 design moved default `rdf4j.optimizer.joinAlgorithmTuning.minSamples` to `32` to keep adaptation default-on but conservative.

- Observation: bushy-shape planning repeatedly cloned/re-estimated structurally equivalent UNION branch subtrees, inflating join-estimation pressure even with plan-reuse enabled.
  Evidence: task-20 pre-fix failure in `CostBasedJoinOptimizerTest.bushyStructuralMemoizationIsEnabledByDefaultAndCanBeDisabled` showed equal join-estimate counts with memoization toggled.

- Observation: runtime-sampling caps must apply inside top-K enumeration itself; outer-loop checks alone are insufficient because beam expansion can issue many estimates per iteration.
  Evidence: task-21 implementation added budget-aware join-estimate fallback in `JoinPlanEnumerator.estimateJoinOutput(...)` and routed runtime-sampling top-K enumeration through budget callback from `CostBasedJoinOptimizer`.

- Observation: even with join-estimate memoization available, algorithm-hint assignment can still trigger duplicate join-estimate requests when memoization is disabled or cold; per-query join-output reuse removes those duplicate calls deterministically.
  Evidence: task-22 pre-fix failure in `CostBasedJoinOptimizerTest.algorithmHintEstimateReuseIsEnabledByDefaultAndCanBeDisabled` (equal request counts), then post-fix pass after adding `estimateJoinOutput(...)` reuse cache.

- Observation: low-confidence gating alone can still over-spend runtime-sampling work on stable sources with low calibration q-error; auto-throttle cuts this planner overhead without disabling runtime-sampling globally.
  Evidence: task-23 test `CostBasedJoinOptimizerTest.runtimeSamplingAutoThrottleIsEnabledByDefaultAndCanBeDisabled` passed after introducing q-error/sample-count based throttle controls.

- Observation: per-query join-plan reuse does not reduce repeated planning work across separate optimize invocations; a cross-optimize warm cache is required to get that win.
  Evidence: task-24 pre-fix failure in `CostBasedJoinOptimizerTest.joinPlanWarmCacheIsEnabledByDefaultAndCanBeDisabled`, then post-fix pass after adding warm-cache snapshot/load + drift invalidation checks.

- Observation: aggregate join-estimate counters in diagnostics can hide where planner cost is spent; stage-level counters are needed to isolate ordering vs reranking vs bushy hotspots.
  Evidence: task-25 extension in `CostBasedJoinOptimizerTest.annotatesJoinDecisionDiagnosticsByDefault` required explicit assertions for `plannerJoinEstimateCallsOrdering`, `plannerJoinEstimateCallsReranking`, and `plannerJoinEstimateCallsBushy`.

- Observation: disconnected-component planning still followed traversal order even when initial bindings strongly favored a later component.
  Evidence: task-26 pre-fix failure in `CostBasedJoinOptimizerTest.joinGroupPartitionComponentOrderingIsEnabledByDefaultAndCanBeDisabled` until component-ordering heuristic was added.

- Observation: runtime-sampling candidate enumeration can waste work when planner memoization hit-rate is already high and candidate marginal value is low.
  Evidence: task-27 pre-fix failure in `CostBasedJoinOptimizerTest.runtimeSamplingMemoizationGuardIsEnabledByDefaultAndCanBeDisabled`, then post-fix pass after adding memoization-hit-rate guardrail.

- Observation: memoization-guard behavior is difficult to tune blindly without explicit diagnostics on guard checks/skips and decision-time hit-rate.
  Evidence: task-58 pre-fix failure in `CostBasedJoinOptimizerTest.annotatesJoinDecisionDiagnosticsByDefault` until guard counters were emitted.

- Observation: guarding rerank work purely by memoization hit-rate can suppress needed exploration under calibration drift unless q-error explicitly overrides the guard.
  Evidence: task-59 pre-fix failure in `CostBasedJoinOptimizerTest.runtimeSamplingMemoizationGuardYieldsForHighQError` until high-q-error override bypassed guard short-circuit.

- Observation: repeated disconnected-component ordering work across equivalent UNION branches can consume avoidable join-estimation calls unless component ordering decisions are reused within the optimize pass.
  Evidence: task-60 pre-fix failure in `CostBasedJoinOptimizerTest.joinGroupPartitionComponentOrderingReuseIsEnabledByDefaultAndCanBeDisabled` until per-optimize ordering reuse cache was added.

- Observation: a single memoization-guard threshold can thrash rerank decisions around borderline hit-rates; hysteresis stabilizes behavior and avoids alternating skip/enable patterns.
  Evidence: task-61 pre-fix failure in `CostBasedJoinOptimizerTest.runtimeSamplingMemoizationGuardHysteresisIsEnabledByDefaultAndCanBeDisabled` until engage/disengage thresholds were introduced.

- Observation: naïve source-share amplification in weighted q-error override can over-trigger on stable (`qError ~= 1.0`) sources and collapse rerank discrimination.
  Evidence: task-62 first regression run failed `CostBasedJoinOptimizerTest.runtimeSamplingMemoizationGuardYieldsForHighQError` until weighted override was re-based around `qError - 1.0` contribution.

- Observation: candidate-source diagnostics are easiest to regress silently because some join groups never enter runtime sampling, so missing fields can go unnoticed without explicit explain assertions.
  Evidence: task-63 pre-fix failure in `CostBasedJoinOptimizerTest.annotatesJoinDecisionDiagnosticsByDefault` after adding required candidate-source diagnostics assertions.

- Observation: disconnected-component ordering reuse benefits collapse when equivalent signatures are separated by many distinct signatures; bounded deterministic eviction is required to keep memory predictable while preserving repeatability.
  Evidence: task-64 pre-fix failure in `CostBasedJoinOptimizerTest.joinGroupPartitionComponentOrderingReuseCacheSizeIsBoundedAndDeterministic` showed no request delta between tight and wide caps until bounded eviction was implemented.

## Decision Log

- Decision: Ship all four features default-on and add explicit disable flags for each.
  Rationale: Matches user requirement for immediate broad rollout with targeted rollback controls.
  Date/Author: 2026-02-16 / Codex

- Decision: Implement features in strict order 1 -> 2 -> 3 -> 4 and validate after each feature.
  Rationale: Matches user request and minimizes debugging ambiguity across interacting planner/runtime changes.
  Date/Author: 2026-02-16 / Codex

- Decision: Keep bushy planning default-on but suppress bushy rewrite when the input join group already has explicit join hints.
  Rationale: Preserves legacy non-ordering optimizer work while still enabling bushy optimization for non-hinted groups.
  Date/Author: 2026-02-16 / Codex

- Decision: Default to buffered calibration persistence with threshold + interval flush triggers, plus shutdown flush.
  Rationale: Avoids per-observation persistence overhead while preserving eventual durability and explicit override knobs.
  Date/Author: 2026-02-16 / Codex

- Decision: Bound calibration source cardinality by evicting least-observed sources first (stable lexical tie-break).
  Rationale: Prevent unbounded calibration map growth and keep persisted footprint predictable.
  Date/Author: 2026-02-16 / Codex

- Decision: In adaptive runtime mode, nested-loop fallbacks default to cacheable when sampled left-input size is small.
  Rationale: Low-risk, high-impact repeated-right-side evaluation savings; still fully disable-able.
  Date/Author: 2026-02-16 / Codex

- Decision: Runtime sampling candidate pruning is default-on, with hard candidate caps and stricter caps for wide join groups.
  Rationale: Keep planning latency stable while preserving sampled alternatives; deployments can fully disable pruning by property.
  Date/Author: 2026-02-16 / Codex

- Decision: Runtime sampling diversity pruning is default-on and keyed by configurable prefix length.
  Rationale: Remove near-duplicate candidate orders from runtime sampling to spend budget on materially different prefixes.
  Date/Author: 2026-02-16 / Codex

- Decision: Implement join-estimate memoization as a per-optimizer `EvaluationStatistics` delegate wrapper keyed by structural `Join` expressions.
  Rationale: Share cached join-estimation results across ordering, reranking, and bushy-shape planning without invasive planner API churn.
  Date/Author: 2026-02-16 / Codex

- Decision: Keep diversity-pruning effectiveness tests independent by disabling memoization in that targeted test path.
  Rationale: Preserve signal for diversity-pruning behavior while leaving memoization default-on in production.
  Date/Author: 2026-02-16 / Codex

- Decision: Store calibration persistence metadata (`version`, `entryCount`) and reject unsupported future versions during load.
  Rationale: Preserve forward compatibility safety and support deterministic compaction/rewrite of legacy or malformed payloads.
  Date/Author: 2026-02-16 / Codex

- Decision: Bound join-estimate memoization by default using deterministic insertion-order eviction and expose max size as property.
  Rationale: Cap planner memory growth while preserving stable behavior and easy rollout tuning.
  Date/Author: 2026-02-16 / Codex

- Decision: Keep telemetry collection always-on inside planner statistics wrapper while retaining independent memoization enable/disable controls.
  Rationale: Explain diagnostics and runtime tuning need stable counters regardless of memoization mode.
  Date/Author: 2026-02-16 / Codex

- Decision: Partition disconnected join groups into connected components before ordering/reranking, default-on with explicit disable property.
  Rationale: Reduces combinatorics and join-estimation pressure without changing query semantics; preserves rollback path.
  Date/Author: 2026-02-16 / Codex

- Decision: Apply runtime-sampling budgets in two explicit phases (candidate generation, prefix scoring), each hard-capped by effective planning budget and overridable per property.
  Rationale: Stabilizes planning latency and avoids one stage starving the other while maintaining backward compatibility via default stage budgets and a disable switch.
  Date/Author: 2026-02-16 / Codex

- Decision: Enable join-plan reuse by default within each optimize invocation and key entries by normalized join-group fingerprint plus scope-binding profile, with property `rdf4j.optimizer.joinPlanReuse.enabled` to disable.
  Rationale: Cuts repeated planning/join-estimation work across equivalent join groups while preserving rollout safety.
  Date/Author: 2026-02-17 / Codex

- Decision: Keep adaptive beam-width control default-on and q-error-driven, but clamp with configurable min/max width bounds and latency backpressure.
  Rationale: Preserves upside on uncertain workloads while preventing planner over-expansion under latency pressure.
  Date/Author: 2026-02-17 / Codex

- Decision: Implement online algorithm-threshold tuning via runtime join-cost observations, default-on with conservative sample floor and explicit disable switch.
  Rationale: Allows planner thresholds to adapt from real runtime behavior while minimizing risk of oscillation on low-sample noise.
  Date/Author: 2026-02-17 / Codex

- Decision: Add bushy-shape structural memoization cache keyed by ordered normalized term fingerprints and scoped binding context, default-on with property disable.
  Rationale: Eliminates repeated bushy clone/estimate work for equivalent wide join groups without changing selected order semantics.
  Date/Author: 2026-02-17 / Codex

- Decision: Enforce runtime-sampling join-estimate call budget at both orchestration and enumerator step-estimation levels, with heuristic fallback after cap.
  Rationale: Keeps per-join-group planner latency bounded while preserving candidate generation/prefix scoring continuity under tight budgets.
  Date/Author: 2026-02-17 / Codex

- Decision: Add per-query join-output estimate reuse cache in `CostBasedJoinOptimizer` and gate it by property `rdf4j.optimizer.algorithmHintEstimateReuse.enabled` (default-on).
  Rationale: Reuses join-output estimates computed during ordering/reranking when applying algorithm hints, reducing duplicate join-estimation calls without changing plan semantics.
  Date/Author: 2026-02-17 / Codex

- Decision: Add runtime-sampling auto-throttle driven by calibration q-error/sample-count stability and gate it with explicit properties.
  Rationale: Prevents unnecessary runtime-sampling reranks for stable, well-calibrated sources while preserving rollout safety and explicit opt-out.
  Date/Author: 2026-02-17 / Codex

- Decision: Add cross-optimize warm join-plan cache in `CostBasedJoinOptimizer`, with q-error drift invalidation and explicit disable switches.
  Rationale: Captures repeated query-shape planning wins across optimize invocations while preventing stale-plan reuse after calibration drift spikes.
  Date/Author: 2026-02-17 / Codex

- Decision: Split planner join-estimate telemetry by stage (`ORDERING`, `RERANKING`, `BUSHY`) and emit all stage counters in join decision diagnostics.
  Rationale: Makes planner hot-path attribution observable and improves tuning/debugging precision without changing optimization behavior.
  Date/Author: 2026-02-17 / Codex

- Decision: Enable disconnected-component ordering heuristic by default and rank components by (1) initial-bound-variable connectivity, then (2) estimated component output, with explicit disable property.
  Rationale: Anchors disconnected planning around externally constrained components first and reduces avoidable intermediate-size blowups while preserving rollback path.
  Date/Author: 2026-02-17 / Codex

- Decision: Add default-on runtime-sampling memoization guard that skips reranking candidate generation when planner memoization hit-rate exceeds a configurable threshold and minimum sample floor.
  Rationale: Avoids low-yield planning work under stable high-cache-hit conditions while preserving explicit disable/tuning controls.
  Date/Author: 2026-02-17 / Codex

- Decision: Emit memoization-guard diagnostics (`memoizationGuardChecks`, `memoizationGuardSkips`, `memoizationGuardHitRateAtDecision`) in join decision details by default.
  Rationale: Makes guard behavior observable/tunable during explain analysis without requiring profiler-level instrumentation.
  Date/Author: 2026-02-17 / Codex

- Decision: Override memoization-guard short-circuit when runtime-sampling confidence assessment detects high calibration q-error drift.
  Rationale: Preserve reranking exploration under known estimate drift, preventing guard-induced planner blind spots.
  Date/Author: 2026-02-17 / Codex

- Decision: Add default-on per-optimize disconnected-component ordering reuse keyed by normalized component signatures + scope bindings, with explicit disable switch.
  Rationale: Removes repeated component-output estimation cost across equivalent disconnected groups while keeping rollout safety and fast rollback.
  Date/Author: 2026-02-17 / Codex

- Decision: Add default-on memoization-guard hysteresis with separate engage/disengage hit-rate thresholds and explicit disable switch.
  Rationale: Reduces guard oscillation near threshold boundaries and keeps reranking behavior stable under noisy memoization hit-rate measurements.
  Date/Author: 2026-02-17 / Codex

- Decision: Add default-on source-weighted q-error override scoring (`qError + sourceShare * (qError - 1.0)`) and gate it with an explicit disable property.
  Rationale: Lets dominant drifted sources push memoization-guard bypass decisions without falsely promoting stable (`qError ~= 1.0`) sources.
  Date/Author: 2026-02-17 / Codex

- Decision: Emit runtime-sampling candidate-source diagnostics as aggregated planner counters (`baselineOnly`, `topKEnumerated`, `promotedPair`, `guardSkipped`) on every join-group decision detail.
  Rationale: Makes reranking path attribution observable in explain output and gives a stable signal for next-step adaptive runtime-sampling tuning.
  Date/Author: 2026-02-17 / Codex

- Decision: Bound disconnected-component ordering reuse cache by default (`maxEntries=256`) with deterministic insertion-order eviction, and emit reuse hit/miss counters in decision diagnostics.
  Rationale: Caps per-optimize memory overhead while preserving deterministic planning and making cache value visible in explain output.
  Date/Author: 2026-02-17 / Codex

## Outcomes & Retrospective

Implementation complete for features 1-4.

Optimization hardening wave tasks 5-8 complete.

Delivered behavior:

1. Runtime adaptive re-optimization implemented in `JoinQueryEvaluationStep` with default-on property `rdf4j.optimizer.adaptiveRuntimeReopt.enabled`.
2. Bushy planning implemented in `CostBasedJoinOptimizer` with default-on property `rdf4j.optimizer.bushy.enabled` and guard for preexisting explicit join hints.
3. Persistent calibration implemented in `EvaluationStatistics` with default-on property `rdf4j.optimizer.calibration.persistence.enabled`.
4. Explain decision diagnostics implemented from query model (`setPlanDecisionDetails`) through explain node rendering with default-on property `rdf4j.optimizer.explain.decisionDiagnostics.enabled`.
5. Calibration persistence flush is now buffered via:
   - `rdf4j.optimizer.calibration.persistence.flushEveryObservations` (default `32`)
   - `rdf4j.optimizer.calibration.persistence.flushIntervalMs` (default `10000`)
   with forced flush on shutdown hook and explicit force paths.
6. Calibration source cardinality is now bounded via:
   - `rdf4j.optimizer.calibration.maxSources` (default `5000`), with least-observed source eviction.
7. Adaptive runtime nested-loop cacheability hints are now controlled via:
   - `rdf4j.optimizer.adaptiveRuntimeReopt.nestedCache.enabled` (default `true`)
   - `rdf4j.optimizer.adaptiveRuntimeReopt.nestedCache.maxLeftSampledRows` (default `64`)
8. Runtime sampling candidate pruning is now controlled via:
   - `rdf4j.optimizer.runtimeSampling.candidatePruning.enabled` (default `true`)
   - `rdf4j.optimizer.runtimeSampling.candidatePruning.maxCandidates` (default `6`)
   - `rdf4j.optimizer.runtimeSampling.candidatePruning.wideJoinTermThreshold` (default `8`)
   - `rdf4j.optimizer.runtimeSampling.candidatePruning.wideJoinMaxCandidates` (default `3`)
   with opt-out by setting `rdf4j.optimizer.runtimeSampling.candidatePruning.enabled=false`.
9. Runtime sampling candidate diversity pruning is now controlled via:
   - `rdf4j.optimizer.runtimeSampling.candidatePruning.diversity.enabled` (default `true`)
   - `rdf4j.optimizer.runtimeSampling.candidatePruning.diversityPrefixTerms` (default `2`)
   with opt-out by setting `rdf4j.optimizer.runtimeSampling.candidatePruning.diversity.enabled=false`.
10. Shared per-optimizer join-estimate memoization is now controlled via:
   - `rdf4j.optimizer.joinEstimateMemoization.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.joinEstimateMemoization.enabled=false`.
11. Adaptive planner budget scaling is now controlled via:
   - `rdf4j.optimizer.planningBudget.adaptive.enabled` (default `true`)
   - `rdf4j.optimizer.planningBudget.adaptive.maxMs` (default `50`)
   - `rdf4j.optimizer.planningBudget.adaptive.widthStepMs` (default `2`)
   - `rdf4j.optimizer.planningBudget.adaptive.latencyPenaltyFactor` (default `0.5`)
   - `rdf4j.optimizer.planningBudget.adaptive.latencyAlpha` (default `0.2`)
12. Calibration persistence format now includes metadata keys:
   - `rdf4j.optimizer.calibration.persistence.version`
   - `rdf4j.optimizer.calibration.persistence.entryCount`
   with unsupported-format payloads ignored during load (no unsafe import).
13. Join-estimate memoization cache is now bounded via:
   - `rdf4j.optimizer.joinEstimateMemoization.maxEntries` (default `4096`)
   with deterministic insertion-order eviction once the bound is reached.
14. Join-decision diagnostics now include planner telemetry fields:
   - `plannerJoinEstimateCalls`
   - `plannerMemoizationHitRate`
   - `runtimeSamplingMs`
   and are emitted by default when `rdf4j.optimizer.explain.decisionDiagnostics.enabled=true`.
15. Connected-component join-group partitioning is now controlled via:
   - `rdf4j.optimizer.joinGroupPartitioning.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.joinGroupPartitioning.enabled=false`.
16. Runtime-sampling stage budgets are now controlled via:
   - `rdf4j.optimizer.runtimeSampling.stageBudgets.enabled` (default `true`)
   - `rdf4j.optimizer.runtimeSampling.candidateGenerationBudgetMs` (default `0`, auto/uncapped within effective planning budget)
   - `rdf4j.optimizer.runtimeSampling.prefixSamplingBudgetMs` (default `0`, auto/uncapped within effective planning budget)
   with hard per-stage caps at effective planning budget and opt-out by setting `rdf4j.optimizer.runtimeSampling.stageBudgets.enabled=false`.
17. Per-query join-plan reuse is now controlled via:
   - `rdf4j.optimizer.joinPlanReuse.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.joinPlanReuse.enabled=false`.
18. Adaptive beam-width control is now controlled via:
   - `rdf4j.optimizer.beam.adaptive.enabled` (default `true`)
   - `rdf4j.optimizer.beam.adaptive.minWidth` (default `8`)
   - `rdf4j.optimizer.beam.adaptive.maxWidth` (default `64`)
   - `rdf4j.optimizer.beam.adaptive.qErrorBoost` (default `8`)
   - `rdf4j.optimizer.beam.adaptive.latencyThresholdMs` (default `8.0`)
   with opt-out by setting `rdf4j.optimizer.beam.adaptive.enabled=false`.
19. Online join algorithm-threshold tuning is now controlled via:
   - `rdf4j.optimizer.joinAlgorithmTuning.enabled` (default `true`)
   - `rdf4j.optimizer.joinAlgorithmTuning.alpha` (default `0.2`)
   - `rdf4j.optimizer.joinAlgorithmTuning.minSamples` (default `32`)
   with opt-out by setting `rdf4j.optimizer.joinAlgorithmTuning.enabled=false`.
20. Bushy-subtree structural memoization is now controlled via:
   - `rdf4j.optimizer.bushy.structuralMemoization.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.bushy.structuralMemoization.enabled=false`.
21. Runtime-sampling join-estimate call cap is now controlled via:
   - `rdf4j.optimizer.runtimeSampling.estimateCallCap.enabled` (default `true`)
   - `rdf4j.optimizer.runtimeSampling.estimateCallCap.maxCalls` (default `64`)
   with opt-out by setting `rdf4j.optimizer.runtimeSampling.estimateCallCap.enabled=false`.
22. Join-output estimate reuse for algorithm-hint assignment is now controlled via:
   - `rdf4j.optimizer.algorithmHintEstimateReuse.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.algorithmHintEstimateReuse.enabled=false`.
23. Runtime-sampling auto-throttle is now controlled via:
   - `rdf4j.optimizer.runtimeSampling.autoThrottle.enabled` (default `true`)
   - `rdf4j.optimizer.runtimeSampling.autoThrottle.qErrorThreshold` (default `1.25`)
   - `rdf4j.optimizer.runtimeSampling.autoThrottle.minSamples` (default `32`)
   with opt-out by setting `rdf4j.optimizer.runtimeSampling.autoThrottle.enabled=false`.
24. Cross-optimize warm join-plan cache and drift invalidation are now controlled via:
   - `rdf4j.optimizer.joinPlanWarmCache.enabled` (default `true`)
   - `rdf4j.optimizer.joinPlanWarmCache.driftInvalidation.enabled` (default `true`)
   - `rdf4j.optimizer.joinPlanWarmCache.driftInvalidation.qErrorDeltaThreshold` (default `1.0`)
   with opt-out by setting `rdf4j.optimizer.joinPlanWarmCache.enabled=false` or `rdf4j.optimizer.joinPlanWarmCache.driftInvalidation.enabled=false`.
25. Join-decision diagnostics now include join-estimate stage counters:
   - `plannerJoinEstimateCallsOrdering`
   - `plannerJoinEstimateCallsReranking`
   - `plannerJoinEstimateCallsBushy`
   (alongside `plannerJoinEstimateCalls`) by default when explain diagnostics are enabled.
26. Disconnected-component ordering heuristic is now controlled via:
   - `rdf4j.optimizer.joinGroupPartitioning.componentOrdering.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.joinGroupPartitioning.componentOrdering.enabled=false`.
27. Runtime-sampling memoization guard is now controlled via:
   - `rdf4j.optimizer.runtimeSampling.memoizationGuard.enabled` (default `true`)
   - `rdf4j.optimizer.runtimeSampling.memoizationGuard.hitRateThreshold` (default `0.85`)
   - `rdf4j.optimizer.runtimeSampling.memoizationGuard.minEstimateCalls` (default `32`)
   with opt-out by setting `rdf4j.optimizer.runtimeSampling.memoizationGuard.enabled=false`.
28. Explain diagnostics now include memoization-guard telemetry:
   - `memoizationGuardChecks`
   - `memoizationGuardSkips`
   - `memoizationGuardHitRateAtDecision`
   emitted by default when explain diagnostics are enabled.
29. Runtime-sampling memoization guard now yields automatically when confidence assessment detects high calibration q-error, so drifted sources still trigger reranking exploration.
30. Disconnected-component ordering reuse cache is now controlled via:
   - `rdf4j.optimizer.joinGroupPartitioning.componentOrderingReuse.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.joinGroupPartitioning.componentOrderingReuse.enabled=false`.
31. Runtime-sampling memoization-guard hysteresis is now controlled via:
   - `rdf4j.optimizer.runtimeSampling.memoizationGuard.hysteresis.enabled` (default `true`)
   - `rdf4j.optimizer.runtimeSampling.memoizationGuard.hysteresis.engageHitRateThreshold` (default `0.85`)
   - `rdf4j.optimizer.runtimeSampling.memoizationGuard.hysteresis.disengageHitRateThreshold` (default `0.75`)
   with opt-out by setting `rdf4j.optimizer.runtimeSampling.memoizationGuard.hysteresis.enabled=false`.
32. Source-weighted q-error override scoring for memoization-guard bypass is now controlled via:
   - `rdf4j.optimizer.runtimeSampling.memoizationGuard.qErrorWeighting.enabled` (default `true`)
   with opt-out by setting `rdf4j.optimizer.runtimeSampling.memoizationGuard.qErrorWeighting.enabled=false`.
33. Join-decision diagnostics now include runtime-sampling candidate-source counters:
   - `runtimeSamplingCandidateSourceBaselineOnly`
   - `runtimeSamplingCandidateSourceTopKEnumerated`
   - `runtimeSamplingCandidateSourcePromotedPair`
   - `runtimeSamplingCandidateSourceGuardSkipped`
   emitted by default when explain diagnostics are enabled.
34. Disconnected-component ordering reuse cache is now bounded by default via:
   - `rdf4j.optimizer.joinGroupPartitioning.componentOrderingReuse.maxEntries` (default `256`)
   with deterministic insertion-order eviction when the cache reaches the configured max-entry bound.
35. Join-decision diagnostics now include disconnected-component reuse counters:
   - `componentOrderingReuseHits`
   - `componentOrderingReuseMisses`
   emitted by default when explain diagnostics are enabled.

Next-wave critical tasks queued in this plan:

17. Add per-query join-plan reuse cache keyed by normalized join-group signature and binding profile. (completed)
18. Add adaptive beam-width control driven by planner-latency EWMA and calibration q-error pressure. (completed)
19. Add online join algorithm threshold tuning (hash/merge/nested/cacheability) from observed runtime costs. (completed)
20. Add bushy-subtree structural memoization to reduce repeated clone/estimate overhead in wide groups. (completed)
21. Add hard cap on runtime-sampling join-estimate calls per join-group to prevent pathological planning latency spikes. (completed)
22. Reuse join-output estimates from ordering/reranking during algorithm-hint assignment to remove duplicate estimate calls. (completed)
23. Add adaptive runtime-sampling auto-throttling when calibration q-error remains stably low over recent observations. (completed)
24. Add query-shape plan warm cache (signature + initial bindings) with invalidation on calibration drift. (completed)
25. Add join-estimate call-stage metering (`ordering`, `reranking`, `bushy`) in diagnostics to pinpoint hot paths. (completed)
26. Add deterministic component ordering heuristics (bound-vars first, then estimated output) for disconnected groups. (completed)
27. Add runtime-sampling guardrails that skip candidate generation when memoization hit-rate indicates low marginal value. (completed)
28. Add planner memory budget guard (soft cap for temporary join clones/estimates) with safe fallback to beam-only mode.
29. Add runtime-sampling diagnostics for candidate-source selection (`baseline`, `topK`, `promotedPair`) and candidate counts.
30. Add adaptive runtime-sampling top-K scaling based on planner-latency EWMA to reduce runaway planning cost.
31. Add initial-binding-aware component ordering to anchor disconnected components that join external bindings first.
32. Add unsupported-join-estimation selectivity sketching (predicate co-occurrence fallback) to improve heuristic join-output estimates.
33. Add join-plan-reuse hit/miss telemetry in explain diagnostics and planner counters.
34. Add join-plan-reuse drift invalidation when calibration q-error spikes for reused source profiles.
35. Add bounded join-plan-reuse cache size (per optimize) with deterministic eviction policy.
36. Add baseline-score reuse for algorithm-hint assignment to eliminate repeated join-output estimate calls.
37. Add persistence support for online algorithm-tuning state (versioned file + corruption-safe fallback).
38. Add statistically significant-win guardrail before applying online threshold adjustments.
39. Add runtime-sampling q-error trend detector to auto-reenable broader candidate search after drift spikes.
40. Add disconnected-component ordering cache keyed by initial bindings to reduce repeated component-sort work.
41. Add cross-query join-estimate warm cache keyed by canonical join fingerprint + calibration epoch to reduce repeated estimate work across optimizer instances.
42. Add adaptive DP max-term threshold scaling from planner-latency EWMA and q-error pressure to reduce DP blowups on wide groups.
43. Add runtime algorithm-feedback weighting (hash/merge/nested empirical win rates) into `JoinCostModel.chooseJoinAlgorithm(...)`.
44. Add explain diagnostics for runtime-sampling truncation causes (`budgetExceeded`, `deadlineExceeded`, `estimateCallCapExceeded`) and candidate counts at each stage.
45. Add adaptive runtime-sampling prefix-term depth (`runtimeSampling.prefixTerms`) scaling by observed selectivity variance to improve sample quality under fixed budgets.
46. Add join-output estimate reuse telemetry counters (`joinOutputEstimateReuseHits`, `joinOutputEstimateReuseMisses`) to explain diagnostics.
47. Add guardrail to skip algorithm-hint estimate recomputation when explicit join hints are already preserved from legacy subtree templates.
48. Add per-query planner cache budget partitioning (plan reuse vs join-output reuse vs bushy-shape reuse) with deterministic eviction priorities.
49. Add warm-cache max-entry bound + deterministic LRU eviction to cap optimizer-instance memory growth under diverse query shapes.
50. Add warm-cache entry calibration-epoch tagging so global drift/refresh events can invalidate stale plans in one step.
51. Add warm-cache telemetry counters (`warmPlanCacheHits`, `warmPlanCacheMisses`, `warmPlanCacheInvalidations`) to explain diagnostics.
52. Add dataset-aware warm-cache keying (dataset fingerprint + source binding profile) to avoid cross-dataset false plan reuse.
53. Add warm-cache admission control (require repeated shape hits before promotion) to prevent low-value cache churn.
54. Add component-ordering telemetry in diagnostics (`componentReorders`, `boundFirstSelections`, `estimatedOutputDelta`) for tuning feedback.
55. Add runtime-sampling variance gate that skips sampling for components with persistently low prefix-score variance.
56. Add memory-pressure backoff for planner caches (join-output reuse, bushy-shape cache, warm plan cache) using free-heap watermarks.
57. Add adaptive memoization-guard threshold tuning from observed rerank win-rate (auto tighten/relax).
58. Add explain diagnostics counters for memoization guard (`memoizationGuardSkips`, `memoizationGuardChecks`, hit-rate at decision time). (completed)
59. Add high-q-error override path so memoization guard does not suppress reranking when calibration drift spikes. (completed)
60. Add component-ordering reuse cache across equivalent disconnected groups within an optimize call. (completed)
61. Add memoization-guard hysteresis (separate engage/disengage thresholds) to reduce on/off oscillation on borderline hit-rates. (completed)
62. Add per-source q-error weighting in memoization-guard override so drifted sources dominate rerank enablement decisions. (completed)
63. Add runtime-sampling candidate attribution diagnostics by component (`baselineOnly`, `topKEnumerated`, `promotedPair`, `guardSkipped`). (completed)
64. Add bounded disconnected-component ordering reuse cache with deterministic eviction + hit/miss diagnostics. (completed)
65. Add disconnected-component ordering reuse telemetry counters in explain diagnostics (`componentOrderingReuseHits`, `componentOrderingReuseMisses`). (completed)
66. Add component-ordering reuse key versioning so future key-shape changes can invalidate stale cached decisions safely.
67. Add admission threshold for component-ordering reuse (minimum repeated signature hits before cache insert) to avoid low-value entries.
68. Add optional max-entry cap for component-ordering reuse cache with deterministic eviction strategy.
69. Add explain diagnostics for memoization-guard hysteresis state (`memoizationGuardHysteresisEngaged`) and thresholds used at decision time.
70. Add adaptive hysteresis widening/narrowing based on observed guard flip rate to stabilize under noisy workloads.
71. Add minimum join-group width gate for memoization guard/hysteresis to avoid overhead on tiny groups.
72. Add memoization-guard decision-source diagnostics (`highQErrorOverride`, `hysteresisEngage`, `hysteresisDisengage`, `fixedThreshold`).
73. Add explain diagnostics for source-weighted q-error override (`weightedDominantQError`, `weightedDominantSourceShare`, `qErrorWeightingEnabled`).
74. Add source-level calibration-snapshot memoization inside confidence assessment to avoid repeated static snapshot lookups for repeated source keys.
75. Add weighted-override activation floor (`minDominantSourceShare`) to prevent noisy low-coverage sources from driving rerank bypass.
76. Add explicit guard decision telemetry counters for weighted-override path (`weightedOverrideChecks`, `weightedOverrideTriggers`).
77. Add candidate-source saturation guardrail to force periodic top-K exploration after long `baselineOnly`/`guardSkipped` streaks.
78. Add candidate-source diagnostics split by disconnected component index to isolate hot components in explain output.
79. Add adaptive runtime-sampling stage-budget rebalancing driven by observed candidate-source win rates.
80. Add promoted-pair fallback backoff when promoted-pair wins stay below threshold over recent samples.
81. Add component-ordering reuse eviction telemetry (`componentOrderingReuseEvictions`) in explain diagnostics.
82. Add component-ordering reuse cache utilization diagnostics (`entries`, `maxEntries`) at decision time.
83. Add component-ordering reuse adaptive max-entry tuning from observed hit-rate and eviction pressure.
84. Add component-ordering reuse stale-entry aging policy (TTL in optimize pass) with explicit disable switch.

Verification evidence (all offline, `-Dmaven.repo.local=.m2_repo`):

- `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install` -> `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify` -> `Tests run: 579, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model verify` -> `Tests run: 20, Failures: 0, Errors: 0, Skipped: 1`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/query verify` -> `Tests run: 200, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=EvaluationStatisticsTest verify` ->
  pre-fix: `Tests run: 11, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinQueryEvaluationStepTest verify` ->
  pre-fix: `Tests run: 6, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingCandidatePruningIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingCandidateDiversityPruningIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinEstimateMemoizationIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#adaptivePlanningBudgetScalingIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=EvaluationStatisticsTest#testCalibrationPersistenceWritesFormatVersionMetadata+testCalibrationPersistenceSkipsUnsupportedFormatVersion verify` ->
  pre-fix: `Tests run: 2, Failures: 2, Errors: 0, Skipped: 0`, post-fix: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinEstimateMemoizationCacheSizeIsBoundedAndDeterministic verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#annotatesJoinDecisionDiagnosticsByDefault verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinGroupPartitionComponentOrderingIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardYieldsForHighQError+annotatesJoinDecisionDiagnosticsByDefault verify` ->
  pre-fix: `Tests run: 2, Failures: 2, Errors: 0, Skipped: 0`, post-fix: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardIsEnabledByDefaultAndCanBeDisabled+runtimeSamplingMemoizationGuardYieldsForHighQError+annotatesJoinDecisionDiagnosticsByDefault verify` ->
  post-fix: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinGroupPartitionComponentOrderingReuseIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardIsEnabledByDefaultAndCanBeDisabled+runtimeSamplingMemoizationGuardYieldsForHighQError+joinGroupPartitionComponentOrderingReuseIsEnabledByDefaultAndCanBeDisabled+annotatesJoinDecisionDiagnosticsByDefault verify` ->
  post-fix: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardHysteresisIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardIsEnabledByDefaultAndCanBeDisabled+runtimeSamplingMemoizationGuardYieldsForHighQError+runtimeSamplingMemoizationGuardHysteresisIsEnabledByDefaultAndCanBeDisabled+joinGroupPartitionComponentOrderingReuseIsEnabledByDefaultAndCanBeDisabled+annotatesJoinDecisionDiagnosticsByDefault verify` ->
  post-fix: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardQErrorWeightingIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingMemoizationGuardIsEnabledByDefaultAndCanBeDisabled+runtimeSamplingMemoizationGuardYieldsForHighQError+runtimeSamplingMemoizationGuardQErrorWeightingIsEnabledByDefaultAndCanBeDisabled+runtimeSamplingMemoizationGuardHysteresisIsEnabledByDefaultAndCanBeDisabled+joinGroupPartitionComponentOrderingReuseIsEnabledByDefaultAndCanBeDisabled+annotatesJoinDecisionDiagnosticsByDefault verify` ->
  post-fix: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#annotatesJoinDecisionDiagnosticsByDefault verify` ->
  pre-fix (task-63 assertions): `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinGroupPartitionComponentOrderingReuseCacheSizeIsBoundedAndDeterministic+annotatesJoinDecisionDiagnosticsByDefault verify` ->
  pre-fix (task-64/65 assertions): `Tests run: 2, Failures: 2, Errors: 0, Skipped: 0`, post-fix: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinGroupPartitionComponentOrderingReuseIsEnabledByDefaultAndCanBeDisabled+joinGroupPartitionComponentOrderingReuseCacheSizeIsBoundedAndDeterministic+annotatesJoinDecisionDiagnosticsByDefault verify` ->
  post-fix: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinGroupPartitioningIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingStageBudgetsAreEnabledByDefaultAndCanBeDisabled verify` ->
  post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinPlanReuseIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#adaptiveBeamWidthControlIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install` ->
  pre-fix: compilation failure in `JoinCostModelTest` (missing task-19 tuning API symbols), post-fix: `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinCostModelTest#onlineAlgorithmThresholdTuningIsEnabledByDefaultAndCanBeDisabled verify` ->
  post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#bushyStructuralMemoizationIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingEstimateCallCapIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#algorithmHintEstimateReuseIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#runtimeSamplingAutoThrottleIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinPlanWarmCacheIsEnabledByDefaultAndCanBeDisabled verify` ->
  pre-fix: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`, post-fix validated with focused 3-test run below.
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest#joinPlanReuseIsEnabledByDefaultAndCanBeDisabled+joinPlanWarmCacheIsEnabledByDefaultAndCanBeDisabled+joinPlanWarmCacheDriftInvalidationIsEnabledByDefaultAndCanBeDisabled verify` ->
  post-fix: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest,JoinPlanEnumeratorTest verify` ->
  `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest verify` ->
  `Tests run: 44, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify` ->
  `Tests run: 585, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`

## Context and Orientation

The join planner and runtime in this repository are split across three areas:

- `core/queryalgebra/evaluation/.../optimizer/`: logical planning and cost model (`CostBasedJoinOptimizer`, `JoinPlanEnumerator`, `JoinCostModel`, `StandardQueryOptimizerPipeline`).
- `core/queryalgebra/evaluation/.../impl/evaluationsteps/`: runtime execution-step selection (`JoinQueryEvaluationStep`).
- `core/queryalgebra/model` and `core/query`: explain conversion and explain node model (`QueryModelTreeToGenericPlanNode`, `GenericPlanNode`).

Existing system already supports hybrid mode, runtime sampling during planning, calibration feedback in-memory, and join algorithm hinting. The new work extends those systems without changing SPARQL semantics.

## Plan of Work

Feature 1 will extend `JoinQueryEvaluationStep` with a runtime adaptive path that samples left input for each join invocation and chooses hash or nested loop at execution time. This path will be controlled by a new property:

- `rdf4j.optimizer.adaptiveRuntimeReopt.enabled` (default `true`, disable with `false`).

Feature 2 will add bushy-plan dynamic programming in `JoinPlanEnumerator` and optional use in `CostBasedJoinOptimizer` for eligible join groups. It will be controlled by:

- `rdf4j.optimizer.bushy.enabled` (default `true`).
- `rdf4j.optimizer.bushy.maxTerms` (default `10`).

Feature 3 will persist calibration state to disk in `EvaluationStatistics` and reload it at startup. It will be controlled by:

- `rdf4j.optimizer.calibration.persistence.enabled` (default `true`).
- `rdf4j.optimizer.calibration.persistence.file` (default `${java.io.tmpdir}/rdf4j-calibration-state.properties`).

Feature 4 will attach planner decision rationale to query model nodes in `CostBasedJoinOptimizer` and expose that rationale through `QueryModelTreeToGenericPlanNode` and `GenericPlanNode`. It will be controlled by:

- `rdf4j.optimizer.explain.decisionDiagnostics.enabled` (default `true`).

Follow-up optimization hardening wave (new critical tasks):

5. Reduce calibration persistence overhead by buffering flushes instead of writing on every observation.
6. Bound calibration state cardinality (max tracked sources) to avoid unbounded in-memory/file growth.
7. Improve runtime adaptive join execution by setting cacheability hints for nested-loop fallback paths.
8. Add candidate-pruning controls for top-K runtime-sampled plans to keep planning latency stable on wider join groups.

Next-wave critical tasks (new):

9. Add runtime-sampling candidate diversity pruning to avoid sampling near-duplicate candidate orders.
10. Add shared per-query join-estimate memoization across planner phases.
11. Add adaptive planner budget scaling tied to join-group width and recent planner runtime.
12. Add calibration persistence compaction/version checks and corruption-safe load fallback.
13. Add bounded memoization cache cardinality + eviction policy for long/complex planning sessions.
14. Add optimizer telemetry counters (estimate calls, memoization hits, runtime-sampling phase time) and expose in explain. (completed)
15. Add connected-component join-group partitioning before DP/beam to reduce combinatorics and avoid accidental cartesian exploration. (completed)
16. Add staged runtime-sampling budget controls (`candidateGenerationBudgetMs`, `prefixSamplingBudgetMs`) with hard caps. (completed)
17. Add per-query join-plan reuse cache keyed by normalized join-group signature and initial binding profile. (completed)
18. Add adaptive beam-width control tied to planner-latency EWMA and calibration q-error pressure. (completed)
19. Add online algorithm-threshold tuning from observed runtime join costs. (completed)
20. Add bushy subtree structural memoization to cap clone/estimate overhead. (completed)
21. Add hard cap on runtime-sampling join-estimate calls per join-group to prevent pathological planner latency spikes. (completed)
22. Reuse join-output estimates from ordering/reranking during algorithm-hint assignment to remove duplicate estimate calls. (completed)
23. Add adaptive runtime-sampling auto-throttling when calibration q-error stays low over recent observations. (completed)
24. Add query-shape warm plan cache with invalidation when calibration drift exceeds threshold. (completed)
25. Add join-estimate call-stage metering (`ordering`, `reranking`, `bushy`) in explain diagnostics. (completed)
26. Add deterministic disconnected-component ordering heuristics (bound-vars first, then estimated output). (completed)
27. Add runtime-sampling short-circuit when memoization hit-rate signals low incremental benefit. (completed)
28. Add planner temporary-memory guardrail with beam fallback when clone/estimate pressure is high.
29. Add runtime-sampling diagnostics for candidate-source attribution and candidate counts.
30. Add adaptive runtime-sampling top-K scaling driven by planner-latency EWMA.
31. Add initial-binding-aware disconnected-component ordering.
32. Add predicate co-occurrence selectivity sketch fallback when join-estimation is unsupported.
33. Add join-plan-reuse hit/miss telemetry in explain diagnostics.
34. Add join-plan-reuse drift invalidation driven by calibration q-error spikes.
35. Add bounded join-plan-reuse cache size with deterministic eviction.
36. Add score/estimate reuse path for algorithm-hint assignment to remove duplicate estimate calls.
37. Add persistence support for online algorithm-tuning state (versioned file + corruption-safe fallback).
38. Add statistically significant-win guardrail before applying online threshold adjustments.
39. Add runtime-sampling q-error trend detector to auto-reenable broader candidate search after drift spikes.
40. Add disconnected-component ordering cache keyed by initial bindings to reduce repeated component-sort work.
41. Add cross-query join-estimate warm cache keyed by canonical join fingerprint + calibration epoch.
42. Add adaptive DP max-term threshold scaling from planner-latency EWMA and q-error pressure.
43. Add runtime algorithm-feedback weighting into `JoinCostModel.chooseJoinAlgorithm(...)`.
44. Add explain diagnostics for runtime-sampling truncation causes and per-stage candidate counts.
45. Add adaptive runtime-sampling prefix-term depth scaling from observed selectivity variance.
46. Add join-output estimate reuse hit/miss telemetry to explain diagnostics.
47. Add algorithm-hint recomputation guardrail for explicit legacy hints.
48. Add planner cache budget partitioning across reuse caches with deterministic eviction priorities.
49. Add warm-cache max-entry bound + deterministic LRU eviction.
50. Add warm-cache calibration-epoch tagging for bulk stale-entry invalidation.
51. Add warm-cache hit/miss/invalidation telemetry in planner diagnostics.
52. Add dataset-aware warm-cache keying using dataset + source-binding fingerprints.
53. Add warm-cache admission control (minimum repeated hits before warm-cache promotion).
54. Add disconnected-component ordering telemetry in explain diagnostics (reorders, bound-first picks, estimated-output deltas).
55. Add runtime-sampling variance gate to skip candidate sampling for stable low-variance components.
56. Add memory-pressure-driven backoff for planner caches using free-heap watermarks.
57. Add adaptive memoization-guard threshold tuning from observed rerank win-rate.
58. Add memoization-guard diagnostics counters to explain output. (completed)
59. Add high-q-error override so memoization guard yields under drift spikes. (completed)
60. Add disconnected-component ordering reuse cache within optimize call. (completed)
61. Add memoization-guard hysteresis (engage/disengage thresholds) to prevent oscillation around threshold boundaries. (completed)
62. Add source-weighted q-error override scoring so one drifted source cannot be masked by many stable sources. (completed)
63. Add runtime-sampling candidate-source diagnostics counters (`baselineOnly`, `topKEnumerated`, `promotedPair`, `guardSkipped`). (completed)
64. Add bounded disconnected-component ordering reuse cache with deterministic eviction and hit/miss diagnostics. (completed)
65. Add disconnected-component ordering reuse hit/miss counters in explain diagnostics for tuning visibility. (completed)
66. Add component-ordering reuse cache key-version marker for safe compatibility evolution.
67. Add component-ordering reuse admission policy (minimum repeat count before insert).
68. Add configurable max-entry bound for component-ordering reuse cache with deterministic eviction.
69. Add memoization-guard hysteresis state/threshold diagnostics to explain output.
70. Add adaptive hysteresis spread tuning based on observed guard flip rates.
71. Add join-group width gating for memoization-guard evaluation overhead control.
72. Add memoization-guard decision-path diagnostics (`override`, `engage`, `disengage`, `fixed`).
73. Add explain diagnostics for source-weighted q-error override (`weightedDominantQError`, `weightedDominantSourceShare`, `qErrorWeightingEnabled`).
74. Add source-level calibration-snapshot memoization inside confidence assessment to reduce repeated lookup overhead.
75. Add weighted-override activation floor (`minDominantSourceShare`) to suppress low-coverage noise triggers.
76. Add weighted-override telemetry counters (`weightedOverrideChecks`, `weightedOverrideTriggers`) in planner diagnostics.
77. Add candidate-source saturation guardrail to force periodic top-K exploration after long `baselineOnly`/`guardSkipped` streaks.
78. Add candidate-source diagnostics split by disconnected component index to isolate hot components in explain output.
79. Add adaptive runtime-sampling stage-budget rebalancing driven by observed candidate-source win rates.
80. Add promoted-pair fallback backoff when promoted-pair wins stay below threshold over recent samples.
81. Add component-ordering reuse eviction telemetry (`componentOrderingReuseEvictions`) to explain diagnostics.
82. Add component-ordering reuse cache utilization diagnostics (`entries`, `maxEntries`) to decision details.
83. Add adaptive component-ordering reuse max-entry tuning from observed hit/miss/eviction rates.
84. Add stale-entry aging policy for component-ordering reuse (TTL in optimize pass) with disable switch.

## Concrete Steps

Work directory for commands: repository root.

1. Implement feature 1 and run focused tests.

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest,JoinPlanEnumeratorTest verify

2. Implement feature 2 and run focused tests.

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=JoinPlanEnumeratorTest,CostBasedJoinOptimizerTest verify

3. Implement feature 3 and run focused tests.

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=EvaluationStatisticsTest,CostBasedJoinOptimizerTest verify

4. Implement feature 4 and run focused tests across model/evaluation/query.

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model -Dtest=QueryModelTreeToGenericPlanNodeTest verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/query -Dtest=GenericPlanNodeTest verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest=CostBasedJoinOptimizerTest,StandardQueryOptimizerPipelineTest verify

5. Run module-level verify for regression safety.

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/model verify
    mvn -o -Dmaven.repo.local=.m2_repo -pl core/query verify

6. Implement task 5 (buffered calibration persistence) first:

    - Add failing tests in `EvaluationStatisticsTest` for buffered flush behavior.
    - Add properties:
      - `rdf4j.optimizer.calibration.persistence.flushEveryObservations` (default buffered, disable buffering via `1`).
      - `rdf4j.optimizer.calibration.persistence.flushIntervalMs` (time-based safety flush).
    - Re-run focused verify for `EvaluationStatisticsTest`.

7. Implement tasks 6-8 in sequence with focused tests after each.
8. Implement tasks 9-52 in sequence with focused tests after each.

Expected output signature for successful test commands: `Tests run: <n>, Failures: 0, Errors: 0, Skipped: 0` and Maven `BUILD SUCCESS`.

## Validation and Acceptance

Acceptance is met when all the following are true:

1. Runtime adaptive join behavior is enabled by default and can be disabled with `-Drdf4j.optimizer.adaptiveRuntimeReopt.enabled=false`.
2. Bushy planning is enabled by default and can be disabled with `-Drdf4j.optimizer.bushy.enabled=false`.
3. Calibration persistence is enabled by default and can be disabled with `-Drdf4j.optimizer.calibration.persistence.enabled=false`.
4. Explain decision diagnostics are enabled by default and can be disabled with `-Drdf4j.optimizer.explain.decisionDiagnostics.enabled=false`.
5. New tests verify default-on behavior and disable behavior for each feature.
6. Existing optimizer and explain tests remain green.

## Idempotence and Recovery

All edits are additive and property-gated. Re-running commands is safe. If a feature causes regressions, disable only that feature with its property while keeping the rest active. If a test fails mid-step, fix and rerun the same command; no destructive reset is needed.

## Artifacts and Notes

Evidence will be appended as implementation progresses:

- Surefire/Failsafe report snippets per feature.
- Property names and defaults list.
- Brief diff excerpts for key classes.
- `initial-evidence.txt` captured at repository root.

## Interfaces and Dependencies

At completion, these interfaces and properties must exist:

- Runtime adaptive switch:
  - class: `org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.JoinQueryEvaluationStep`
  - property: `rdf4j.optimizer.adaptiveRuntimeReopt.enabled` default `true`

- Bushy planner:
  - class: `org.eclipse.rdf4j.query.algebra.evaluation.optimizer.JoinPlanEnumerator`
  - integration: `org.eclipse.rdf4j.query.algebra.evaluation.optimizer.CostBasedJoinOptimizer`
  - properties: `rdf4j.optimizer.bushy.enabled`, `rdf4j.optimizer.bushy.maxTerms`

- Persistent calibration:
  - class: `org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics`
  - properties: `rdf4j.optimizer.calibration.persistence.enabled`, `rdf4j.optimizer.calibration.persistence.file`

- Explain diagnostics:
  - classes: `org.eclipse.rdf4j.query.algebra.helpers.QueryModelTreeToGenericPlanNode`, `org.eclipse.rdf4j.query.explanation.GenericPlanNode`
  - property: `rdf4j.optimizer.explain.decisionDiagnostics.enabled`

## Revision Notes

- 2026-02-16 00:47Z: Initial plan creation for user-requested overnight feature set (1-4) with default-on + disable controls.
- 2026-02-17 00:03Z: Task 17 completed (join-plan reuse) and next-wave tasks extended to 33-36.
- 2026-02-17 00:18Z: Tasks 18-19 completed (adaptive beam width + online algorithm-threshold tuning); next-wave tasks extended to 37-40.
- 2026-02-17 00:33Z: Tasks 20-21 completed (bushy structural memoization + runtime-sampling estimate-call cap); next-wave tasks extended to 41-44.
- 2026-02-17 00:36Z: Task 22 completed (algorithm-hint estimate reuse); next-wave tasks extended to 45-48.
- 2026-02-17 00:43Z: Task 23 completed (runtime-sampling auto-throttle).
- 2026-02-17 00:50Z: Task 24 completed (cross-optimize warm join-plan cache + drift invalidation); next-wave tasks extended to 49-52.
- 2026-02-17 01:35Z: Task 62 completed (source-weighted q-error override scoring for memoization-guard bypass); next-wave tasks extended to 73-76.
- 2026-02-17 01:40Z: Task 63 completed (runtime-sampling candidate-source attribution diagnostics); next-wave tasks extended to 77-80.
- 2026-02-17 01:46Z: Tasks 64-65 completed (bounded component-ordering reuse cache + explain hit/miss diagnostics); next-wave tasks extended to 81-84.
