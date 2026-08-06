# Repair learned feedback and contain plan regressions

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with `.agent/PLANS.md` from the repository root.

The separate research brief at `.agent/execplans/GH-0000-research-learned-feedback-plan-flip.md` and the repository-root `research-task-learned-feedback-plan-flip.zip` are supporting evidence only. They must remain untouched. This plan embeds every implementation decision needed to finish the production work without relying on those artifacts.

## Purpose / Big Picture

The LMDB query optimizer currently learns feedback from completed executions, but a repeatedly opened operator can report a cumulative row count against a one-open prediction. The resulting false correction can make the plan that just ran appear expensive while an equivalent unobserved implementation remains optimistic. Medical-records query q9 exposes the combined failure: a safe set-difference plan can be replaced by a correlated `NOT EXISTS` probe that is called tens of thousands of times but priced approximately once, creating a persistent plan flip and a large runtime regression.

After this work, logical cardinality observations are commensurate, censored execution is not mistaken for a full scan, logically equivalent physical alternatives share one canonical cardinality truth, and physical startup/reopen/cache/materialization effects remain isolated to the implementation that exhibited them. Dependent operators are priced from explicit components, so invocation multiplication is deterministic cost-model machinery rather than something feedback must discover. A persisted lifecycle then keeps a verified LastGood alternative across planning passes, quarantines catastrophic regressions after their single completed execution, and rolls back only on the next planning pass. The default rollout records lifecycle state without changing winners; `SAFE_PLAN_LIFECYCLE` enforces it.

The observable proof is a set of focused D1–D4 tests plus repeated clean-state, interleaved q9/q10, and restart-persistence tests. Query-plan snapshots must retain identical result counts, and a same-run benchmark must keep learned q9 within 25 percent of the forced-safe baseline without a catastrophic outlier or more than five percent planning-time regression.

## Progress

- [x] (2026-08-05 07:54Z) Read repository rules, required skills, current research brief, and optimizer architecture.
- [x] (2026-08-05 07:54Z) Ran the mandated offline root clean install; all reactor modules passed in 34.653 seconds.
- [x] (2026-08-05 08:24Z) Established D1-01–06 typed observations, removed divisor learning, and kept compatibility telemetry safe.
- [x] (2026-08-05 11:15Z) Completed exact logical/applicability/physical keys, including alpha-normalized bound-input applicability.
- [x] (2026-08-05 12:51Z) Completed invocation-aware ordinary-join costing and absolute logical-cardinality posteriors; repeated q9 is green without crushed outer rows or heap exhaustion.
- [x] (2026-08-05 13:02Z) Verified persisted lifecycle, objective intervals, LastGood selection, quarantine, rollback, period-two containment, and Monitoring-only default rollout.
- [x] (2026-08-05 13:59Z) Preserved typed semi/anti events across exact cache hits and scheduled recosting.
- [x] (2026-08-05 14:00Z) Verified evaluation module at clean-`HEAD` failure parity.
- [x] (2026-08-05 14:33Z) Classified the complete LMDB module against an isolated clean-`HEAD` control.
- [x] (2026-08-05 16:05Z) Repaired every working-tree-only LMDB integration regression; the 89-test Frontier planning class is at exact clean-`HEAD` failure parity.
- [x] (2026-08-06 00:27Z) Pre-bound runtime feedback targets and proved the zero-map evaluation path within both q0 performance gates.
- [x] (2026-08-06 00:54Z) Preserved typed feedback contracts through costing-event reuse, winner composition, and materialization; evaluation remains at clean-`HEAD` failure parity.
- [x] (2026-08-06 04:32Z) Certified logical factor origins and prevented contextual child posteriors from calibrating Join cardinality.
- [x] (2026-08-06 05:41Z) Completed broad verification, q9/q10 snapshots, restart integration, and q0/q9 performance acceptance.
- [x] (2026-08-06 06:12Z) Completed formatting, final install/test, signature, staging, worktree, and artifact audit.
- [x] (2026-08-06 06:45Z) Hid the logical learning key from human-readable telemetry while preserving JSON.
- [ ] [in_progress] Hold rollout in Monitoring pending baseline-suite cleanup and explicit promotion.

## Surprises & Discoveries

- Observation: The working branch already contains the earlier q9 optimizer work as commits, while the tracked worktree is clean. Existing research plans, output, papers, profiles, and packaged evidence are untracked user artifacts.
  Evidence: `git status --short --branch` reported branch `GH-0000-lmdb-predicate-guarantees` and only pre-existing untracked paths.

- Observation: `SailSourceConnection` clones the tuple tree for a normal execution, so a fixed-size accumulator attached to each cloned feedback-enabled node naturally has execution-local ownership. Reopens of an inner iterator see the same clone; the completed-root traversal can publish one immutable snapshot.
  Evidence: existing query preparation follows clone, optimize, precompile, evaluate; `LmdbOperatorFeedbackStats.recordCompletedQuery` already performs one completed-tree traversal.

- Observation: The packed cost representation already stores primitive work, startup-like open, seek, hash, expression, memory, invocation, and planned-metric dimensions. Additive primitive component fields preserve the no-allocation planning shape and let old providers retain exact point intervals.
  Evidence: `PackedCostEstimate` is a mutable primitive carrier consumed by packed winner tables and LMDB cost sessions.

- Observation: Dividing cumulative rows by opens is observably wrong when the open-time predictions vary. With actual and intended predicted sums both 111, the current final-stamp/divisor path corrected a base of 111 down to 64.77681746006242.
  Evidence: `LmdbOperatorFeedbackStatsTest.reinvokedFrontierObservationUsesSummedPerOpenPredictions` failed before production edits; retained log `logs/mvnf/20260805-075851-verify.log` and report are summarized in `initial-evidence.txt`.

- Observation: The old output-cardinality hierarchy erased binding and correlation identity at the family tier, so one observation immediately corrected an incompatible cold alternative.
  Evidence: `FrontierLearningKeyGroupFamilyTest.outputRowsEvidenceDoesNotCrossIncompatibleApplicability` failed 1/1 before D2 with `expected: <true> but was: <false>`; retained log `logs/mvnf/20260805-082646-verify.log`.

- Observation: q9 still admitted a statement-pattern observation collected under bound input and reused it for the same logical expression when planned unbound. This crushed a raw outer estimate of 27 rows to 0.8 and selected streaming execution on the second learned run.
  Evidence: `LmdbMedicalQ9PlanStabilityIT.q9StaysInOnePerformanceClassAcrossLearningRuns` failed after 99.166 seconds; retained log `logs/mvnf/20260805-110330-verify.log` and Failsafe report `core/sail/lmdb/target/failsafe-reports/org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbMedicalQ9PlanStabilityIT.txt`.

- Observation: After bound-input applicability was isolated, q9 retained one materialized-hash logical/applicability/physical identity and never crushed its outer estimate, but four learned runs still took 19.8–21.8 seconds before a fifth learned observation found the 342 ms plan. The slow nested `VALUES ?condCode` join records three opens/seeks yet produces roughly 49,100 contextual rows; the inherited 49,800-row outer execution domain is absent from ordinary dependent-join child costing. Learning is therefore still discovering the deterministic fact that a reopened child repeats.
  Evidence: `LmdbMedicalQ9PlanStabilityIT.q9StaysInOnePerformanceClassAcrossLearningRuns` failed after 248.958 seconds; retained log `logs/mvnf/20260805-111645-verify.log`. Runs 0–5 kept the same materialized-hash identity, while elapsed times were 7.8 s, 21.8 s, 20.2 s, 19.8 s, 20.8 s, and 342 ms.

- Observation: Fixing the inherited-prefix emission shortened q9's slow phase by one execution but did not remove it. The selected slow main plan contains an ordinary dependent `JoinIterator`: its left prefix has 25,000 rows while a disconnected two-row `BindingSetAssignment` records one priced open and five work units. The parent join still adds those five units once. The adjacent conditional statement factor already records 25,000 priced opens, proving that dependent costing must compare required left-row invocations with the openings already represented in the child's physical vector rather than blindly multiplying every contextual estimate.
  Evidence: retained log `logs/mvnf/20260805-113803-verify.log`; q9 runs 1–3 took 18.4–21.3 seconds, then runs 4–5 took 339 and 279 ms. The slow plan's VALUES event has `optimizer.costEventPrefixRows=25.0K`, `plannedRepeatedInvocations=1.00`, and `plannedWorkRows=5.00`, while the preceding conditional statement event has 25,000 for both prefix rows and repeated invocations.

- Observation: Scaling only scan/open dimensions while treating every child result-row dimension as already contextual is also unsound. q9 selected a disconnected `JoinIterator` whose 49,800-row right component was reopened for 49,900 left bindings. Its cost event recorded the correct invocation exposure and 2.489-billion cartesian-work diagnostic, but charged all 49,800 result rows once as fixed output; the fork exhausted Java heap while materializing the resulting cartesian intermediate. Component-scoped output rows must repeat across disconnected execution partitions, while truly contextual output rows remain fixed.
  Evidence: retained log `logs/mvnf/20260805-115047-verify.log`, `core/sail/lmdb/target/failsafe-reports/failsafe-summary.xml`, and the Surefire dump files. The selected join recorded `optimizer.dependentInvocationCount=49.9K`, `optimizer.dependentFirstMatchWork=0`, `optimizer.dependentExhaustionWork=0`, `optimizer.dependentOutputWork=49.8K`, and `plannedCostCartesianWorkRows=2489.0M`, followed by `java.lang.OutOfMemoryError: Java heap space` before any test completed.

- Observation: Once deterministic dependent work stopped the heap failure, q9 still shrank the Difference outer estimate from 51,900 to 690 and then 65. The logical posterior was stored as a raw-relative log residual, so each change in the underlying raw estimate reapplied the same correction at a new scale. Logical cardinality must be an absolute posterior; physical dimensions remain residuals.
  Evidence: retained q9 log `logs/mvnf/20260805-123249-verify.log` and the focused D2-04 red log `logs/mvnf/20260805-123900-verify.log`, where one observation yielded 259.6987917117054 from raw 100 but 517.8164072678492 from raw 200.

- Observation: Changing logical-posterior meaning requires a wire-version boundary even though the serialized fields are byte-compatible. A v19 sidecar loaded under absolute semantics produced a live 300.429-row correction; v20 now consumes v18/v19 typed payloads only to validate and quarantine them.
  Evidence: focused red/green logs `logs/mvnf/20260805-124709-verify.log` and `logs/mvnf/20260805-124855-verify.log`; the complete 84-test feedback-store class is green in `logs/mvnf/20260805-124946-verify.log`.

- Observation: With component-scoped reopen work, preserved full-prefix logical row scope, and absolute logical truth, six clean-state q9 executions retain non-crushed outer estimates and complete without heap exhaustion or runtime probe-budget rescue.
  Evidence: `logs/mvnf/20260805-125036-verify.log`; outer estimates were 30,700, 49,900, 56,800, 58,400, 25,000, and 56,200, and the Failsafe report passed 1/1 in 103.540 seconds.

- Observation: The default `SAFE_CARDINALITY_CORRECTION` profile left point-cost ranking unchanged but exposed persisted counterfactual states such as `Applying` and `Quarantined` directly in plan telemetry. The rollout contract requires an effective Monitoring decision until lifecycle enforcement is enabled.
  Evidence: focused D4 rollout test failed with `expected: <MONITORING> but was: <QUARANTINED>` in `logs/mvnf/20260805-125602-verify.log`, then passed in `logs/mvnf/20260805-125716-verify.log`. Complete lifecycle and packed-winner classes passed 9/9 each in `logs/mvnf/20260805-125812-verify.log` and `logs/mvnf/20260805-125908-verify.log`.

- Observation: Interleaving catalog q9 and q10 preserves exact logical/applicability isolation: q10 consumed no learned exact identity outside q9's observed set, and the subsequent q9 execution retained its identity, safe plan, and result count. The catalog pair does not happen to select a shared exact key, so D2-03's focused key/model test remains the proof of compatible sharing.
  Evidence: Q9-02 passed 1/1 in `logs/mvnf/20260805-130816-verify.log` after the deliberately over-strong cross-query-overlap assertion was corrected; the rejected assumption is retained in `initial-evidence.txt`.

- Observation: A restarted q9 store restores v20 exact logical/applicability evidence and lifecycle records. Its safe persisted winner may be materialized, in which case correlated-only `plannedCorrelationOuterRows` telemetry is intentionally absent; every present correlated estimate remained 30,700 or 49,900 rows.
  Evidence: Q9-03 passed 1/1 in 68.719 seconds in `logs/mvnf/20260805-131358-verify.log`; the initial assertion-design failure and disposition are retained in `initial-evidence.txt`.

- Observation: Q9-01, Q9-02, and Q9-03 compose cleanly in one fresh Failsafe fork rather than passing only as isolated methods.
  Evidence: the complete `LmdbMedicalQ9PlanStabilityIT` class passed 3/3 in 312.448 seconds in `logs/mvnf/20260805-131620-verify.log`.

- Observation: Canonical logical-key normalization exposed a deterministic materialization-lifetime defect in Q9-01. The selected learned candidate is labelled `materialized-hash` and prices one build per materialization-parameter partition, but `FilterIterator` shares only its probe budget and memo cache across the precompiled step; every reopened `MaterializedExistsFilterIteration` owns a fresh `materializedPartitions` map and rebuilds the same partition. The learned plan raised correlated outer exposure to approximately 122,500 rows and timed out at 60 seconds while producing only about 2,000 rows. This is D3 deterministic work, not evidence that evaluation must generate a new key.
- Observation: After the materialization partition was correctly shared for the precompiled step, q9 still timed out because typed logical publication stored the complete execution's `actual_rows_sum` as one-open absolute cardinality. A child producing about 49,800 rows per exact exhausted open can be reopened by an enclosing iterator, turning approximately 75.4 million execution rows into the canonical logical posterior even though `raw_predicted_rows_sum` carries the same invocation exposure. Logical truth must therefore apply the exact aggregate ratio `actual_rows_sum / raw_predicted_rows_sum` to the contract's saved raw cardinality; this uses D1's commensurate sums without dividing by invocation count or generating identity during evaluation.
- Observation: Aggregate-ratio normalization removed the immediate q9 timeout: four early learned runs completed in 4.1-5.1 seconds and the next plan completed in 141 ms. The fast observation then produced a sixth plan that exhausted Java heap. The OOM classifier identified managed heap exhaustion, and the test now persists each completed run's full optimized plan so the post-fast winner can be inspected without inferring identity from evaluation-time values.
  Evidence: combined Q9-01–03 log `logs/mvnf/20260806-021807-verify.log`; Failsafe reports 3 tests, 0 failures, 1 error, with `q9StaysInOnePerformanceClassAcrossLearningRuns` timing out at line 82. The timed plan carries logical digest `4d0d...`, applicability `603a...`, physical digest `a741...`, and LastGood physical digest `ebb6...`.
- Observation: The captured sixth-run winner contains a disconnected intermediate join with approximately 986.2 million planned Cartesian rows but only 44,800 objective rows. Both that join and its non-equivalent child carried the enclosing filter's logical key and applicability, so exact child feedback corrected the Cartesian parent. Physical-event stamping treated any inherited contract as the current memo-group identity instead of using the planner's canonical factor state. A focused direct costing-event reproduction fails with `expected: <join> but was: <filter>` before the repair.
  Evidence: q9 plan `core/sail/lmdb/target/medical-q9-plan-stability-run-4.txt`; focused red log `logs/mvnf/20260806-030546-verify.log` and Surefire report `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbFiniteValuesJoinSurfacePlanningTest.txt`.
- Observation: Once physical join identity stopped crossing logical groups, q9 remained bounded but its materialized Difference input fell from 25,000 to 13,100, 888, and 230 rows. A correlated statement access planned 3,700 identical one-row invocations, while its immutable runtime contract stored the 3,700-row contextual total as the prediction for every open. The accumulator therefore compared approximately 13.7 million predicted rows with 3,700 actual rows. The costing event already publishes `plannedRowsPerInvocation=1`; contract construction must carry that explicit per-open primitive and per-open physical components rather than reconstructing or dividing cumulative execution observations.
  Evidence: q9 red log `logs/mvnf/20260806-030915-verify.log`; focused contract red log `logs/mvnf/20260806-031423-verify.log`, expected per-open `1.0` but contract carried `1001.5`.

- Observation: Decomposing the immutable open vector fixed D1 commensurability but exposed a second typed-scope collision: `FrontierLearningModel` also used that per-open row as the canonical logical cardinality baseline. The traced q9 target published exact `99,670/99,670` and `49,835/49,835` predicted/actual sums, yet two samples of per-open `1` drove the contextual plan through the fourfold safety caps `13,100`, `888`, and `230`. The contract must preserve both planner quantities: contextual raw/applied logical cardinality for D2 and immutable per-open primitive predictions for D1/physical residuals.
  Evidence: traced q9 log `logs/mvnf/20260806-032352-verify.log`; focused contextual-cardinality red log `logs/mvnf/20260806-032754-verify.log`.

- Observation: Splitting contextual logical cardinality from per-open exposure restored all six q9 safety invariants and eliminated the heap failure: outer estimates were 25,000, 57,600, 28,500, 75,200, 82,500, and 86,400 with identical results. The run still failed its fastest-run timing statistic because runs 0–4 took 4.2–4.7 seconds and run 5 took 480 ms. Runs 4 and 5 have identical algebra structure, physical algorithms, index modes, hash build sides, and materialized semi/anti identity, with only small estimate changes; JVM-tier evidence is required before treating that one-way speedup as optimizer oscillation.
  Evidence: q9 log `logs/mvnf/20260806-033200-verify.log`; plans `core/sail/lmdb/target/medical-q9-plan-stability-run-4.txt` and `core/sail/lmdb/target/medical-q9-plan-stability-run-5.txt`.
- Observation: Capping the entire Failsafe fork at C1 (`-XX:TieredStopAtLevel=1`) preserved the same one-way sixth-run speedup: runs 0–4 took 11.1–11.9 seconds and run 5 took 918 ms. C2 tier transition is therefore not the cause. Every admitted feedback sample advances the LEO revision, forcing exact plan-cache misses; the sixth-run shape is consistent with the stale-plan validator first certifying structural reuse. The q9 test now records query preparation/open separately from result consumption before changing its performance statistic.
  Evidence: Failsafe report `core/sail/lmdb/target/failsafe-reports/org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbMedicalQ9PlanStabilityIT.txt`; C1 run completed with 1 test, 1 failure, 0 errors.
- Observation: The q9 timing split proved the one-way speedup is entirely in optimizer/open work. Runs 0–4 opened in 3.85–4.40 seconds and consumed in 458–478 ms; run 5 opened from the certified cache in 12.7 ms and consumed in 474.9 ms. All six executions therefore remained in one physical runtime class. The stability invariant now compares each learned execution with the fastest previously verified learned execution, while the dedicated planning benchmark retains the five-percent planning gate.
  Evidence: retained log `logs/mvnf/20260806-035234-verify.log`; Failsafe report `core/sail/lmdb/target/failsafe-reports/org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbMedicalQ9PlanStabilityIT.txt`.
- Observation: Q9-01 passes after separating optimizer/open time from result consumption and using a prior-only execution baseline. Six clean-state executions retained identical results, safe outer estimates, and materialized semi/anti identity without a later runtime regression.
  Evidence: retained log `logs/mvnf/20260806-035516-verify.log`; Failsafe summary reports 1 test, 0 failures, 0 errors.
- Observation: The broad focused LMDB selection passes 297 of 305 tests and resolves two of the three formerly known `LmdbEstimateAuditHarnessTest` failures. Comparison with the retained clean-head controls shows that six failures are established baseline contracts: the factor-specific distinct-probe metric and five canonical Frontier-state tests. Two working-tree regressions remain: a calibrated canonical join prefix is overwritten by its physical enumeration-route key, and the independent-lane MINUS estimate no longer reduces its sampled left input.
  Evidence: retained aggregate log `logs/mvnf/20260806-035839-verify.log`; the factor-specific baseline reproduces alone in `logs/mvnf/20260806-040607-verify.log`; the working-tree canonical-prefix regression reproduces alone in `logs/mvnf/20260806-040843-verify.log`; clean controls are `logs/mvnf/20260806-020233-verify.log` and `logs/mvnf/20260806-013223-verify.log`.

- Observation: Correlated scheduling could reintroduce the algorithm-less source `FILTER` as a normal-cost root candidate after typed semi/anti alternatives were installed. At equal cost that compatibility form displaced the selected streaming/memoized/materialized event, so a cold plan and its exact cache hit lost physical identity. The source form must remain executable but compare in the fallback tier whenever its exact root group owns typed alternatives; relocated helper groups without typed alternatives remain normally costed.
  Evidence: the focused test failed 1/1 in `logs/mvnf/20260805-135250-verify.log`; the immutable decision trace in `logs/mvnf/20260805-135634-verify.log` showed relation 5 (`FILTER`) selected over relation 6 (`ANTI_JOIN`) at the same cost; the focused test and complete 17-test class passed in `logs/mvnf/20260805-135830-verify.log` and `logs/mvnf/20260805-135923-verify.log`.

- Observation: The complete evaluation module has one failure which is not introduced by this work. An isolated archive of clean `HEAD` fails the same `PackedFrontierSubsetKernelContractTest.denseKernelRetainsTheGloballyOptimalOrderedContinuationState` contract with the same expected B-A-C cost 3 versus actual A-B-C cost 102. The production tree runs 21 additional tests and has no other failure.
  Evidence: clean-`HEAD` control `/tmp/rdf4j-head-baseline.AdM0wY` ran 1,147 tests with one failure; the production tree ran 1,168 tests with the same sole failure in `logs/mvnf/20260805-140023-verify.log`.

- Observation: The first complete LMDB module gate ran 1,873 tests in 14:32 and reported 28 failures plus one error across seven classes. The set mixes clearly stale or pre-existing contracts (including an untouched 722-line architecture limit, a deadline-expired AAS test, and sidecar assertions still expecting v17) with potentially in-scope invocation, finite-filter, and feedback-key regressions, so assertions must not be changed until the same module selection is controlled against clean `HEAD`.
  Evidence: `logs/mvnf/20260805-140143-verify.log` and `core/sail/lmdb/target/surefire-reports/`.

- Observation: The isolated clean-`HEAD` LMDB control ran 1,848 tests in 13:56 and failed ten methods: the AAS query-2 deadline, both estimator architecture size limits, the finite-prefix invocation contract, five frontier-state contracts, and the generated-corpus q-error audit. The working tree adds 25 D1–D4 tests and has those same ten baseline failures plus nineteen working-tree-only failures. The additional set is confined to the new finite-filter contract, thirteen frontier contracts, two legacy sidecar-version contracts, one optimizer-pipeline source contract, and two estimate-audit contracts.
  Evidence: clean control `/tmp/rdf4j-head-baseline.AdM0wY/logs/mvnf/20260805-141744-verify.log` reports `Tests run: 1848, Failures: 10, Errors: 0`; production log `logs/mvnf/20260805-140143-verify.log` reports `Tests run: 1873, Failures: 28, Errors: 1`.

- Observation: Scheduled typed semi/anti recosting stamped the implementation relation but refined the filter relation, violating immutable costing-event identity. Refining the same implementation relation removed the planner fallback and preserved the physical contract.
  Evidence: reproduction log `logs/mvnf/20260805-154314-verify.log`; repaired focused log `logs/mvnf/20260805-155143-verify.log`; packed physical contract log `logs/mvnf/20260805-155240-verify.log`.

- Observation: A typed semi/anti operator's own left-input prefix was being encoded as an external conditional binding shape, so identical logical work received a different `LearningApplicability` after feedback. Treating an exact encoded-input prefix as internal stabilized the key while retaining genuinely inherited partial prefixes.
  Evidence: diagnostic log `logs/mvnf/20260805-155836-verify.log`; repaired four-method integration log `logs/mvnf/20260805-160219-verify.log`.

- Observation: After the relation-identity and applicability repairs, the complete 89-test Frontier planning class has exactly the same five failing methods as the isolated clean-`HEAD` control; no working-tree-only integration failure remains.
  Evidence: production log `logs/mvnf/20260805-160315-verify.log` and clean control `/tmp/rdf4j-head-baseline.AdM0wY/logs/mvnf/20260805-141744-verify.log`.

- Observation: Medical Records query 0 exposes unacceptable execution overhead from routing learned feedback through generic query-model metric maps. With otherwise identical profiling runs, feedback enabled measured 83.921 ms/op and feedback disabled measured 65.569 ms/op, approximately 28 percent overhead.
  Evidence: preserved async-profiler inputs under `/tmp/medical-q0-learning-profile/`; the feedback-on profile is `manual-cpu-39555.txt` and the feedback-off profile is `manual-cpu-feedback-off-39724.txt`.

- Observation: The feedback-on CPU profile attributes 5.47 percent of samples to generic metric-map methods, 3.78 percent to `HashMap`, and 1.08 percent to invocation-accumulator machinery. The current reopen path repeatedly calls `getDoubleMetricPlanned`, `getLongMetricActual`, and query-model metadata accessors; root publication reparses persisted-key strings and performs another series of map lookups.
  Evidence: grouped samples from the Medical Records query 0 profiles, plus the hot paths in `EvaluationStatistics.beginInvocationObservation`, `DefaultEvaluationStrategy.ResultSizeCountingIterator`, and `LmdbOperatorFeedbackStats.recordFrontierOutcome`.

- Observation: Allocation profiling confirms that generic metric communication boxes primitive feedback values. The focused feedback-on run sampled 440 `Double` allocations versus three with feedback disabled; it also sampled 40 `ResultSizeCountingIterator` allocations versus none with feedback disabled.
  Evidence: `/tmp/medical-q0-learning-profile/manual-alloc-feedback-on-focused-45110.txt` and `/tmp/medical-q0-learning-profile/manual-alloc-feedback-off-44796.txt`, collected with a two-megabyte allocation-sampling interval.

- Observation: `LateralQueryEvaluationStep` can clone, rename, and precompile its right subtree from inside evaluation. That runtime recompilation is outside the normal immutable planning identity and therefore cannot safely resolve or train learned feedback.
  Evidence: `LateralQueryEvaluationStep.prepareRight` invokes `DefaultEvaluationStrategy.precompile` after dynamically scoping the subtree.

- Observation: Resolving typed cells before evaluation did not by itself make publication allocation-free. The first direct-cell measurement allocated 3,803,312 bytes over 4,096 publications because physical digests were recomputed, fixed posterior slots were lazily materialized, lifecycle envelopes were replaced, and the trace system property was reread on the hot path. Precomputing the physical digest, materializing fixed slots during resolution, mutating lifecycle envelope primitives, and caching trace enablement reduced the stable-state direct target path to zero bytes.
  Evidence: failing log `logs/mvnf/20260805-215349-verify.log`, intermediate log `logs/mvnf/20260805-215610-verify.log`, and passing log `logs/mvnf/20260805-215843-verify.log`.

- Observation: A completed successful EXISTS probe is intentionally censored for output cardinality and full-scan work, but is exact evidence for the hit outcome and first-match work. Dependent physical predictions must be expressed per hit and per miss; multiplying either component by all opens makes planned hit-rate error contaminate the physical residual.
  Evidence: failing logs `logs/mvnf/20260805-220054-verify.log` and `logs/mvnf/20260805-220227-verify.log`; the repaired combined test passed in `logs/mvnf/20260805-220447-verify.log`.

- Observation: After direct publication became allocation-free, ordinary compiled evaluation still allocated one counting wrapper and one root wrapper per execution. Across 4,096 identical one-row evaluations feedback added exactly 262,144 bytes. Both views can be created once at precompile and rebound because a compiled algebra node has one active iterator at a time within its execution-local tree; repeated inner opens reuse the same view while accumulating into the same target.
  Evidence: failing log `logs/mvnf/20260805-221725-verify.log`; passing zero-delta log `logs/mvnf/20260805-222031-verify.log`; 100,000 compiled reopens and the complete 11-test contract passed in `logs/mvnf/20260805-222210-verify.log` and `logs/mvnf/20260805-222333-verify.log`.

- Observation: Applicability is fully determined during planning. An exact prefix equal to a unary or semi/anti relation's encoded logical input is enumeration topology rather than an outer binding condition and must normalize to the standalone applicability; genuinely inherited or partial prefixes remain conditional. Evaluation contributes neither correlated values nor identity material.
  Evidence: `LmdbFrontierPackedCostSession.productionLearningIdentity` normalizes `prefixCoversRelationInput`, while the guarded compiled-evaluation test throws on every generic metadata accessor after precompile and still passes.

- Observation: The first alternating q0 result appeared to satisfy both performance gates, but it was not a valid `OBSERVE_ONLY` comparison. `ThemeQueryPlanRunBenchmark.BaseState.leoRolloutProfile` had its JMH `@Param` annotation commented out, so setup replaced the requested value with the field fallback `SAFE_CARDINALITY_CORRECTION`.
  Evidence: `ThemeQueryPlanRunBenchmarkTest.leoRolloutProfileIsAnExplicitJmhParameter` failed in `logs/mvnf/20260805-232450-verify.log` and passed after restoring the annotation in `logs/mvnf/20260805-232540-verify.log`. The earlier `pair-a-comparison.md`, `pair-b-comparison.md`, and `planning-comparison.md` remain retained as rejected benchmark evidence.

- Observation: A live-attach CPU profile taken only after q0 planning and store setup removed the old result-size metric-map hot path, but exposed one remaining ordinary-learning violation: `JoinMetricsTracking` still allocates a per-probe wrapper and writes string-keyed generic join metrics whenever a child merely has cost feedback enabled. This compatibility telemetry is not a typed observation source and must remain exclusive to explicit Timed/Telemetry evaluation.
  Evidence: `output/learned-feedback-plan-flip/profiles/medical-q0/live-cpu-observe/attach-69840-cpu.txt` sampled `JoinMetricsTracking$1.<init>`, `HashMap`, and `String.hashCode` during the steady execution window.

- Observation: Guarding both inner and left join compatibility metric wrappers behind explicit Timed/Telemetry evaluation removes their generic-map transport from ordinary learned-feedback execution without changing diagnostic sessions. A precompiled guarded query now executes after every generic metric and metadata accessor is armed to throw.
  Evidence: the focused guard failed before the repair in `logs/mvnf/20260805-225410-verify.log`, then passed in `logs/mvnf/20260805-225518-verify.log`; the complete 12-test runtime-feedback class and five existing join-metric tests passed in `logs/mvnf/20260805-225611-verify.log` and `logs/mvnf/20260805-225644-verify.log`.

- Observation: Directly costing one canonical `Difference` once standalone and once with its exact left subtree exposed as the enumeration prefix reproduced the reported key split: the logical key matched while `LearningApplicability` changed from the unbound input bucket to a bound-prefix bucket. `prefixCoversRelationInput` recognized two-child typed semi/anti relations but omitted the canonical two-child `Difference` itself.
  Evidence: failing log `logs/mvnf/20260805-230552-verify.log`; after recognizing the logical `Difference` as a left-input operator, the same focused test passed in `logs/mvnf/20260805-230713-verify.log`.

- Observation: Exact facts are deliberately keyed by the typed logical identity plus applicability, not by the explanatory logical-key string alone. The derived-MINUS integration assertion still queried the old standalone string slot after production keys became valid; probing the planned typed applicability verifies the fact without weakening conditional isolation. The combined invariants retain exact-prefix normalization, genuine bound/unbound separation, logical-alternative symmetry, and fact reuse.
  Evidence: the stale assertion failed in `logs/mvnf/20260805-230927-verify.log`; the five-method planning cluster passed 5/5 in `logs/mvnf/20260805-231313-verify.log`. `LmdbRuntimeFeedbackTargetTest.conditionalExactFactIsNotVisibleAsStandaloneLogicalTruth` remains the focused guard against leaking a conditional fact into standalone truth.

- Observation: With an explicit JMH rollout parameter, the matched pre-refactor q0 CPU runs measured 44.630 ms/op in `OBSERVE_ONLY` versus 41.734 ms/op with feedback off, a 6.94 percent regression. The feedback-only flat profile attributed 1.92 percent exclusively to `RuntimeFeedbackCountingIterator.bind` and 0.47 percent to `RuntimeFeedbackAccumulator.addPrediction`: every immutable reopen reread and summed all prediction dimensions instead of incrementing one exposure counter.
  Evidence: `output/learned-feedback-plan-flip/profiles/medical-q0/valid-cpu-observe/` and `valid-cpu-off/`; the direct 100,000-open contract was green before the neutral refactor in `logs/mvnf/20260805-234100-verify.log`.

- Observation: Deferring immutable prediction multiplication to publication reduces an ordinary open to two primitive saturating increments while preserving direct varying-prediction accumulation. The post-change 100,000-open test verifies exact row, work, and iterator-open sums; the complete evaluation and LMDB runtime-target suites remain green.
  Evidence: focused post-change log `logs/mvnf/20260805-234159-verify.log`, 12-test evaluation log `logs/mvnf/20260805-234243-verify.log`, and 9-test LMDB target log `logs/mvnf/20260805-234323-verify.log`.

- Observation: Two post-refactor alternating q0 pairs with isolated stores and identical plan fingerprint `378dc5a77a8c68da98e02efc532a0450415bdc0bb56d7530498b09ce3194be44` now meet the execution gate in aggregate. Across 20 measurements per profile, feedback off had a 42.1235 ms/op median and `OBSERVE_ONLY` had a 43.8825 ms/op median, a 4.18 percent overhead. Pair medians were +2.98 and +5.84 percent, so the combined alternating median is the acceptance statistic and profile evidence remains required before closing the milestone.
  Evidence: retained JMH logs under `output/learned-feedback-plan-flip/benchmarks/medical-q0/deferred-sum-{off,observe}-{a,b}/`.

- Observation: The first post-refactor allocation profile still found `LearningKeyCodec.encode` below `resolveRuntimeFeedbackTarget` after iterator evaluation had begun. `GroupIterator` compiled `group.getArg()` in its constructor, so the outer precompile resolved zero child targets and ordinary evaluation performed typed-cell resolution, allocation, and string encoding. Precompiling the group argument in `DefaultEvaluationStrategy.prepare(Group)` and injecting that immutable step into `GroupIterator` closes the boundary without changing group semantics.
  Evidence: focused precompile-boundary test failed with `expected: 1L but was: 0L` in `logs/mvnf/20260806-000058-verify.log` and passed in `logs/mvnf/20260806-000209-verify.log`; the complete 13-test runtime-feedback class and 24-test `GroupIteratorTest` passed in `logs/mvnf/20260806-000255-verify.log` and `logs/mvnf/20260806-000329-verify.log`. The triggering allocation trace is retained under `output/learned-feedback-plan-flip/profiles/medical-q0/deferred-alloc-observe/`.

- Observation: After the Group precompile repair, the final isolated-store q0 execution pair retains structural fingerprint `378dc5a77a8c68da98e02efc532a0450415bdc0bb56d7530498b09ce3194be44` and passes its result assertions. Feedback off has a 42.2940 ms/op measurement median and `OBSERVE_ONLY` has a 43.5940 ms/op median, a 3.07 percent overhead. Matched CPU profiles measure 42.280 versus 43.724 ms/op, a 3.42 percent overhead, with no sampled feedback/key/metric/string/boxing self-time.
  Evidence: `output/learned-feedback-plan-flip/benchmarks/medical-q0/precompiled-group-comparison.md` and matched profiles under `output/learned-feedback-plan-flip/profiles/medical-q0/precompiled-group-cpu-{off,observe}/`.

- Observation: A final allocation profile with the benchmark's explicit telemetry teardown disabled contains no frames for `LearningKeyCodec`, `resolveRuntimeFeedbackTarget`, feedback accumulator or iterator construction, feedback binding/publication, generic metric access, string hashing, or primitive boxing. The only `HashMap` CPU samples in the clean observe-only profile are from q0's `COUNT(DISTINCT ...)` implementation, not learning. The remaining inactive `JoinMetricsTracking` delegate frame performs no metric operation and allocates nothing during evaluation.
  Evidence: clean profiles under `output/learned-feedback-plan-flip/profiles/medical-q0/clean-{cpu,alloc}-observe/`; allocation sampling used a two-megabyte interval, and the CPU profile used a five-millisecond interval.

- Observation: Explicit uncached-planning runs satisfy the five-percent planning gate. Feedback off has a 17.1855 ms/op measurement median and `OBSERVE_ONLY` has a 12.0945 ms/op median, a 29.62 percent improvement rather than a regression.
  Evidence: `output/learned-feedback-plan-flip/benchmarks/medical-q0/precompiled-group-planning-comparison.md` and the retained benchmark logs in its sibling `precompiled-group-plan-{off,observe}/` directories.

- Observation: Reusing one precompiled counting iterator is safe for ordinary reopens only if exhaustion releases it even when its parent omits an explicit close. `LeftJoinIterator` returns an unmatched left binding after an empty right probe by dropping that right iterator; the next probe then re-enters the still-active feedback view. Finalizing on terminal `hasNext() == false` makes exhaustion exact and idempotent, matching the existing root wrapper, without allocating another view.
  Evidence: the focused evaluator reproduction failed in `logs/mvnf/20260806-003321-verify.log` and passed in `logs/mvnf/20260806-003412-verify.log`; the complete 14-test runtime-feedback class passed in `logs/mvnf/20260806-003457-verify.log`. The original audit method passed in `logs/mvnf/20260806-003533-verify.log`, and all 48 `LmdbEstimateAuditHarnessTest` tests passed in `logs/mvnf/20260806-003652-verify.log`.

- Observation: The packed winner contract test still supplied only explanatory key strings, masking a real object-propagation gap. Once the fixture supplied a typed contract, costing-event restore and composed winner metadata both dropped it. The costing journal now retains the immutable contract, includes input contract identity in invocation reuse, and winner composition/restoration carries the object to the recipe and materialized node. Generic `costEvent*` metrics remain explanatory and are not runtime prediction inputs.
  Evidence: the typed fixture failed with `expected RuntimeFeedbackContract but was null` in `logs/mvnf/20260806-004535-verify.log`; the materialization method passed in `logs/mvnf/20260806-005127-verify.log`, the 14-test costing-session lifecycle class passed in `logs/mvnf/20260806-005202-verify.log`, and all 69 `PackedSearchTest` tests passed in `logs/mvnf/20260806-005241-verify.log`. The complete 1,184-test evaluation module in `logs/mvnf/20260806-005315-verify.log` has only the established clean-`HEAD` dense-kernel continuation-state failure.

- Observation: The first post-prebinding LMDB module run executed 1,889 tests and reported twelve failures. Nine are shared with the isolated clean-`HEAD` control; the three working-tree-only Frontier failures were confined to q10 key stability, posterior layering under exact-fact precedence, and learned intermediate-join prefix propagation.
  Evidence: production log `logs/mvnf/20260806-005445-verify.log` reports twelve failures; clean control `/tmp/rdf4j-head-baseline.AdM0wY/logs/mvnf/20260805-141744-verify.log` reports the corresponding nine surviving baseline failures after the generated audit repair. The focused working-tree-only cluster failed 2/3 in `logs/mvnf/20260806-011209-verify.log`, while q10 passed in isolation.

- Observation: The filter regression was an assertion setup defect rather than a production regression. A current-stamp exact fact correctly supersedes a posterior and intentionally omits the LEO-row diagnostic; disabling exact facts in the posterior-layering test isolates the contract it intends to verify.
  Evidence: `sampledFilterAppliesLeoMultiplierAfterRawRestriction` expected `plannedFrontierLeoRows=30` but observed its absent sentinel in `logs/mvnf/20260806-011209-verify.log`; the revised exact-fact-disabled cluster passes in `logs/mvnf/20260806-012958-verify.log`.

- Observation: Intermediate join applicability depended on the enumeration prefix and mask-strata hash instead of the canonical joined factor set and alpha-normalized layout. More seriously, physical-event refinement recomputed the complete production identity and overwrote an inherited logical contract. A raw physical candidate could therefore be relabelled with the learned candidate's logical key without receiving its logical correction, recreating executed-plan-versus-unknown asymmetry inside one memo group.
  Evidence: instrumentation showed an exact posterior match and a logical correction from 71.3146 to 84.1104 before the selected physical winner reverted to the raw state. Preserving the inherited logical key/applicability while replacing only the physical key makes the single join-prefix reproduction pass in `logs/mvnf/20260806-012906-verify.log`; the three-test regression cluster passes 3/3 in `logs/mvnf/20260806-012958-verify.log`.

- Observation: After preserving the inherited logical contract, the complete 90-test Frontier planning class returned to exact clean-`HEAD` parity: only the five established canonical-state tests fail. A subsequent 1,889-test LMDB run had those five plus the four other baseline failures and one q10 candidate-selection failure, for ten failures total.
  Evidence: class log `logs/mvnf/20260806-013223-verify.log` reports 90 tests and the exact five clean-control methods. Module log `logs/mvnf/20260806-013452-verify.log` reports 1,889 tests, ten failures, and no errors; `libraryQ10ShapeKeepsFiveKeysThroughUnionOptionalAndBoundedAntiJoin` is its sole delta from the nine surviving clean-control failures.

- Observation: The q10 module delta is not reproducible as JVM-state contamination or as a stable key mismatch. The method passes alone, after the two immediately preceding module classes, after all 115 inherited optimistic-isolation tests followed by the complete Frontier class, and in five additional fresh-JVM/store repetitions. The one failing plan retained learned filter evidence but selected the generic materializing `Difference` after a bounded exact-expansion fallback instead of a typed semi/anti candidate.
  Evidence: focused log `logs/mvnf/20260806-015152-verify.log` passes 1/1; the two-predecessor run passes 7/7; optimistic-isolation plus Frontier runs 205 tests with only the five baseline Frontier failures; five subsequent focused repetitions all pass. The second complete LMDB run `logs/mvnf/20260806-020233-verify.log` runs 1,889 tests with exactly the nine clean-`HEAD` failures and no q10 failure, so no working-tree-only module regression remains.

- Observation: A contextual `appendFactor` event carried the appended StatementPattern's conditional logical key while its Frontier state and scalar rows described the complete joined prefix. After a genuine Join posterior had calibrated that lineage, the child posterior could recalibrate it to approximately one row; duplicate-evidence protection then correctly declined a second Join calibration but left the child scalar in place. The MINUS audit consequently reported an exact 11-row Difference over a 0.966-row sampled left Join.
  Evidence: failing audit log `logs/mvnf/20260806-041824-verify.log` and trace logs through `logs/mvnf/20260806-042350-verify.log`; the repaired audit passes in `logs/mvnf/20260806-043029-verify.log`, while canonical learned-prefix propagation and the non-equivalent Filter guard pass in `logs/mvnf/20260806-043120-verify.log` and `logs/mvnf/20260806-043204-verify.log`.

- Observation: Final module verification contains no working-tree-only regression. The evaluation selection ran 1,185 tests with only the established clean-`HEAD` dense-kernel continuation-state failure. The LMDB selection ran 1,893 tests with the exact nine established clean-`HEAD` failures; all 48 estimate-audit tests and every pre-bound runtime-target test pass.
  Evidence: retained module logs `logs/mvnf/20260806-050713-verify.log` and `logs/mvnf/20260806-050807-verify.log`.

- Observation: The complete q9/q10/restart integration class passes in one fresh Failsafe fork. Repeated clean-state q9 remains semantically stable, q9 and q10 retain exact-identity isolation when interleaved, and v20 feedback and lifecycle state survive restart.
  Evidence: `logs/mvnf/20260806-052445-verify.log` reports 3 tests, 0 failures, 0 errors, and 0 skipped.

- Observation: Final isolated-store snapshots preserve exact result semantics. q9 baseline and learned snapshots both return one result with fingerprint `f5dd5c92aa9869f02158d63c5323127eafe44e0abf2ee08fc7581b47c7eaa37b`; q10 both return one result with fingerprint `1266010ed839a9940871fa2f04c9a022b42ff10247e149853162edb782d69270`. Structure-and-estimate diffs are retained. Adaptive index/evidence sidecars change the store-size input fingerprint during a clean-to-learned run, so these artifacts prove identical query/data results rather than byte-identical optimizer input state.
  Evidence: `output/learned-feedback-plan-flip/plan-snapshots/` contains the four JSON snapshots and baseline/candidate logs.

- Observation: The final q9 benchmark passes the 25-percent safety gate without a catastrophic outlier. `SAFE_CARDINALITY_CORRECTION` has a 214.852 ms/op median and 206.173–238.185 ms/op range; the same-jar feedback-off control has a 199.679 ms/op median and 198.327–203.493 ms/op range. The learned profile is 7.60 percent slower, within the allowed envelope.
  Evidence: `output/learned-feedback-plan-flip/benchmarks/medical-q9-final.txt`, `output/learned-feedback-plan-flip/benchmarks/medical-q9-off-baseline.txt`, and `output/learned-feedback-plan-flip/benchmarks/medical-q9-comparison.md`.

- Observation: Repository formatting, copyright inspection, and the mandatory final offline root clean install completed successfully. The copyright script reports only pre-existing or untouched POMs and workspace build copies, with no edited Java source finding; the clean root reactor completed in 34.382 seconds. The final audit caught one missing Codex signature in `LeftJoinQueryEvaluationStep`, added the required header-only comment, reran formatting, and passed the directly exercising runtime-feedback class 14/14.
  Evidence: final install output, `logs/mvnf/20260806-061120-verify.log`, formatter exit status, and the retained copyright output from the final verification pass.

- Observation: The opaque `optimizer.frontierLearningKey` is useful in JSON diagnostics but overwhelms the human-readable plan. Filtering it only while `GenericPlanNode` assembles text keeps the underlying planned-string map unchanged, so JSON retains the complete value without copying or mutating telemetry state.
  Evidence: the focused test failed before the renderer filter in `logs/mvnf/20260806-061615-verify.log`, passed afterward in `logs/mvnf/20260806-061743-verify.log`, and the complete 234-test `core/query` module passed in `logs/mvnf/20260806-061904-verify.log`.

## Decision Log

- Decision: Keep the interpreted iterator and Cascades memo execution model; use primitive accumulators on execution clones and bounded primitive/object maps off the per-result hot path.
  Rationale: The requested runtime contract is O(1) per open/close with no new allocation per result. Execution-local ownership avoids synchronization and preserves the existing architecture.
  Date/Author: 2026-08-05 / Codex

- Decision: Represent one completed root execution as one posterior sample regardless of opens or probes.
  Rationale: Open and probe counts are exposure. Treating them as independent samples would falsely collapse uncertainty and let one catastrophic execution masquerade as thousands of observations.
  Date/Author: 2026-08-05 / Codex

- Decision: Save prediction values at each open and aggregate sums directly; remove the invocation divisor.
  Rationale: Summing correctly handles both constant predictions such as 49,800 opens of two rows and varying predictions such as 1, 10, and 100. Division can only reconstruct the constant case and loses censoring information.
  Date/Author: 2026-08-05 / Codex

- Decision: Represent immutable-prediction opens as a primitive exposure count and form their exact sums once when the aggregate is read; continue accumulating explicit varying predictions at each open.
  Rationale: The costing vector is immutable for ordinary reopens, so adding all dimensions repeatedly is avoidable deterministic work. Separating constant exposure from varying sums preserves D1 commensurability while removing the dominant feedback-only q0 reopen cost.
  Date/Author: 2026-08-05 / Codex

- Decision: Use exact canonical logical-expression plus applicability matching for production v1. Keep broader family summaries diagnostic-only.
  Rationale: Exact matching prevents incompatible predicates, binding contracts, datasets, or epochs from contaminating winner selection while still calibrating all physical implementations of the same memo group symmetrically.
  Date/Author: 2026-08-05 / Codex

- Decision: Encode actual input-bound variables in applicability with an alpha-normalized expression fingerprint, while keeping lookup masks, algorithms, and layout identifiers exclusively physical.
  Rationale: A statement pattern's logical output is identical across access paths, but its conditional cardinality given a bound subject is not commensurate with its unbound relation cardinality. Alpha normalization permits safe reuse across renamed equivalent expressions without leaking query-local identifiers.
  Date/Author: 2026-08-05 / Codex

- Decision: Normalize an exact encoded-input prefix out of `LearningApplicability` for unary and typed semi/anti operators.
  Rationale: The operator's own physical input is not an external correlation condition. Keeping it in applicability made the key depend on plan state and prevented a completed observation from applying on the next planning pass; genuinely partial or additional inherited prefixes remain conditional.
  Date/Author: 2026-08-05 / Codex

- Decision: Store logical truth and physical residuals independently, and train logical error against the saved raw prediction rather than an already corrected estimate.
  Rationale: This prevents double correction and stops algorithm-specific behavior from becoming logical cardinality truth.
  Date/Author: 2026-08-05 / Codex

- Decision: Store logical output cardinality and hit probability as absolute log-domain truths, while keeping startup, reopen, first-match, exhaustion, cache, seek, hash, and materialization dimensions as raw-relative physical residuals.
  Rationale: A logical expression has one cardinality for matching applicability even when child estimates change; multiplying a learned residual by a newly corrected raw estimate compounds feedback and recreates the q9 plan flip.
  Date/Author: 2026-08-05 / Codex

- Decision: Carry contextual raw/applied logical-cardinality predictions separately from the immutable per-open primitive prediction vectors in `RuntimeFeedbackContract`.
  Rationale: A correlated access can have logical cardinality `N` and physical invocation exposure `N × 1` simultaneously. Reusing either quantity for the other recreates q9 poisoning: a contextual vector is multiplied once per open, while a per-open logical baseline teaches the posterior that the whole applicability has cardinality one. Both values are planner-certified and pre-bound; evaluation only accumulates open exposure and actual counters.

- Decision: Diagnose q9's one-way final speedup by separating optimizer/open time from iterator consumption before altering the stability assertion.
  Rationale: A future certified plan-cache hit is not evidence that earlier semantically safe executions regressed. The acceptance test must continue rejecting a later slow learned plan, but it must not compare full replanning runs against a future cache hit as though both paid the same planning work.

- Decision: Define Q9-01's performance class as result-consumption time and compare only against already completed learned executions.
  Rationale: This preserves the original period-flip detector—a later catastrophic plan still fails against a previously verified fast plan—while making optimizer cache hits, JVM improvements, and future faster plans one-way-safe. Planning-time regression remains an independent benchmark gate rather than being conflated with iterator execution.
  Date/Author: 2026-08-06 / Codex

- Decision: Enforce rollback on the next planning pass only and never restart, shadow, duplicate, or cancel an executing query.
  Rationale: A completed execution supplies the safety evidence. Revision increments invalidate cached recipes so the following planning pass can exclude an unsafe implementation without changing query semantics mid-flight.
  Date/Author: 2026-08-05 / Codex

- Decision: Keep `SAFE_CARDINALITY_CORRECTION` as Monitoring-only and add `SAFE_PLAN_LIFECYCLE` as the first profile that changes winner selection.
  Rationale: This separates statistical observation from deployment enforcement and provides a safe staged rollout.
  Date/Author: 2026-08-05 / Codex

- Decision: Carry an opaque immutable `RuntimeFeedbackContract` through the packed cost estimate, physical metadata arena, cached recipe, and materializer, then resolve its mutable storage cells exactly once in `DefaultEvaluationStrategy.precompile`.
  Rationale: Logical and physical identities belong to planning, while posterior/lifecycle cells belong to the current store generation. Separating the contract from the resolved target lets cached recipes retain typed identity without retaining stale receivers and removes all key parsing and map lookup from evaluation.
  Date/Author: 2026-08-05 / Codex

- Decision: Treat the typed runtime feedback contract as part of query-local costing-event identity and preserve it across every estimate-copy boundary; never reconstruct it from explanatory cost-event or key metrics.
  Rationale: Two candidates with equal scalar costs can target different logical, physical, applicability, epoch, or lifecycle cells. Reusing one provider event across those typed identities would violate D2 isolation, while parsing strings during materialization or evaluation would violate pre-binding.
  Date/Author: 2026-08-06 / Codex

- Decision: A physical implementation event uses the canonical logical key, applicability, and physical key generated from its current planner-certified memo/factor state. It may reuse an inherited contract only as proof that typed planning identity exists and for legacy diagnostics; it must not substitute that contract's logical group for the current event. If no typed contract is present, reject physical learning rather than synthesizing or reparsing evaluation identity.
  Rationale: Estimate copies can cross nested logical boundaries before physical costing. Treating any inherited contract as the current group allowed a Cartesian join to inherit an enclosing filter posterior. The current factor state is planner information, preserves physical-alternative symmetry through canonical topology, and never requires evaluation-time key generation or lookup.
  Date/Author: 2026-08-06 / Codex

- Decision: Certify a nonzero costing relation from its own memo group's transitive factor set, not from a larger contextual Frontier state, and apply its logical row posterior only when that certificate matches the output state's factor set. Keep the relation's conditional runtime contract and physical residual path intact.
  Rationale: An appended access relation and the joined prefix are different logical cardinality objects even when one packed estimate carries both access work and contextual rows. Typed origin matching prevents a child posterior from mutating Join truth without moving key construction into evaluation or discarding valid per-open physical observations.
  Date/Author: 2026-08-06 / Codex

- Decision: Treat the zero-map guarantee as applying to ordinary learned-feedback evaluation only. Explicit Timed or full Telemetry sessions may continue to use generic metric maps for explanations.
  Rationale: The performance defect comes from enabling generic telemetry as the learning transport. Keeping diagnostic sessions explicit preserves plan-explanation compatibility without charging every normal query for maps, strings, boxing, or allocation.
  Date/Author: 2026-08-05 / Codex

- Decision: Compile dynamic lateral subtrees with feedback resolution suppressed and a no-op target.
  Rationale: Runtime-renamed expressions have no normal precompiled contract, and resolving them during evaluation would violate both the zero-map contract and canonical identity safety. They remain executable and can learn once represented by an ordinary planned contract.
  Date/Author: 2026-08-05 / Codex

- Decision: Precompile ordinary `Group` inputs in the enclosing strategy pass and inject the resulting step into each `GroupIterator`; retain the direct public iterator constructors as compatibility adapters.
  Rationale: Group input identity is already certified before evaluation and is not a dynamic rewrite. Deferring its compilation to iterator construction repeated cell resolution and key encoding inside evaluation, whereas an injected immutable step shares the enclosing query-level feedback compilation and preserves direct-constructor compatibility.
  Date/Author: 2026-08-06 / Codex

- Decision: Finalize and release a pre-bound counting view as soon as its delegate reports terminal exhaustion; retain explicit close as an idempotent fallback for partial, cancelled, or failed termination.
  Rationale: Iterator parents are not uniformly required to close an already exhausted child. Terminal false is exact exhaustion evidence and is the earliest allocation-free point at which the reusable compiled view can be safely rebound.
  Date/Author: 2026-08-06 / Codex

- Decision: Treat an exact left-subtree prefix of a canonical `Difference` as internal enumeration topology, just like its typed semi/anti implementations; retain conditional applicability for every partial, inherited, or value-bound external prefix.
  Rationale: The logical operation and binding contract do not change merely because packed enumeration reaches the same memo group through its already-costed left input. Correlated values remain invocation observations, never identity material.
  Date/Author: 2026-08-05 / Codex

- Decision: Accept the pre-binding milestone only when Medical Records query 0 in `OBSERVE_ONLY` mode is no more than five percent slower than feedback-off, with identical results and no learning-attributed post-precompile map, string, boxing, or allocation frames.
  Rationale: The existing 28 percent regression is large enough that structural correctness alone is insufficient. Alternating isolated-store runs and repeated CPU/allocation profiles make the performance contract observable and guard against benchmark drift.
  Date/Author: 2026-08-05 / Codex

## Outcomes & Retrospective

D1 through D4 and pre-bound evaluation routing are implemented and verified at working-tree parity. Open-time raw and applied predictions now accumulate on the cloned node, constant 49,800×2 and varying 1+10+100 cases are exact sums, partial/cancelled/failed observations are diagnostic-only, semi/anti hit and miss work is typed, and 100,000 probes retain statistical weight one. Versioned logical, applicability, and physical identities feed separate bounded stores; logical cardinality is absolute, topology families cannot influence output-cardinality winners, bound and unbound inputs cannot share conditional truth, and physical residuals cannot cross algorithms. The v20 sidecar quarantines v17 topology state and v18/v19 residual-logical state. Explicit semi/anti, cache, materialization, inherited-prefix reopen, and component-output formulas are green, and repeated q9 no longer crushes its outer estimate or exhausts heap. The persisted lifecycle covers robust near ties, decisive verified switches, LastGood rollback, catastrophic quarantine, period-two freeze, stale epochs, protected eviction, and a Monitoring-only default that preserves counterfactual safety state without changing winners.

Typed contracts now resolve current direct cells once during normal precompile; planner-generated logical/applicability/physical identity is preserved as an object through cached recipes and materialization, while evaluation contributes only primitive predictions, actual work, termination, and physical-ID verification. Group inputs no longer compile during evaluation, dynamic uncertified lateral recompilation receives a no-op target, and q0 meets both five-percent gates: 3.07 percent execution overhead, no planning regression, and no learning-attributed post-precompile allocation or sampled map/string/boxing work. q9/q10/restart passes 3/3, snapshot result fingerprints are identical, and q9 is 7.60 percent above its same-jar feedback-off median with no catastrophic outlier. The two complete module selections have exact clean-`HEAD` failure parity and no working-tree-only regression. The final dirty-worktree and artifact audit is complete; no pre-existing user artifact has been removed, reset, committed, or pushed. `SAFE_PLAN_LIFECYCLE` is implemented and tested, but the deployment default remains Monitoring until the inherited module-suite baseline failures are cleaned up and rollout is explicitly promoted.

## Context and Orientation

The project is Eclipse RDF4J, built with Maven from the repository root. `core/queryalgebra/evaluation` owns the general query algebra evaluator and packed Cascades optimizer. `core/sail/lmdb` owns LMDB-specific cardinality, feedback persistence, and physical costing.

`EvaluationStatistics` is the compatibility boundary through which execution reports feedback. `DefaultEvaluationStrategy.ResultSizeCountingIterator` wraps result iterators and currently updates cumulative primitive counters on `QueryModelNode`. `MaterializedExistsFilterIteration` executes streaming, memoized, materialized, or adaptive semi/anti probes and exposes hit/miss and algorithm telemetry. `LmdbOperatorFeedbackStats` traverses a completed execution tree, admits observations, updates persisted state, and owns the sidecar format. A posterior is the bounded statistical summary that estimates a value and uncertainty from admitted query executions.

The packed optimizer stores alternatives in memo groups. A memo group means a set of expressions that produce the same logical result. `PackedMemo` interns groups and merges equivalences. `PackedCostEstimate` is the allocation-free primitive cost carrier, `PackedDependentSubqueryCosting` handles repeated dependent work, `PackedWinnerTable` compares alternatives, and `PackedPlanCache` caches selected recipes against feedback revisions. `LmdbFrontierPackedCostSession` supplies LMDB estimates and enumerates streaming, memoized, and materialized semi/anti choices. `FrontierSemiAntiProfile` contains the logical outer/key domain information used to price those choices.

Three new versioned identities separate concerns. `LogicalLearningKey` identifies the canonical logical memo-group expression: normalized operator and child groups, exact predicates and VALUES domains, alpha-normalized visible and required outer variables, bag/set and duplicate semantics, unbound semantics, and dataset scope. `LearningApplicability` restricts reuse to a binding shape, correlation contract, conditional feature bucket, and matching data/catalog/model epochs. `PhysicalResidualKey` identifies only implementation behavior: algorithm, access kernel or index mode, reopen/rescan mode, cache mode and capacity class, materialization parameter domain, and binding-layout class. It never stores logical cardinality.

An invocation aggregate is the fixed-size execution-local snapshot containing raw and applied predicted-row sums, actual-row and physical-work sums, opens, completion classifications, semi/anti outcomes, cache activity, and distinct-binding exposure. A censored open ended before full exhaustion, so it can train first-match work or diagnostic counters but cannot establish full logical cardinality. A root-complete exact logical observation requires the query root and every relevant contributing open to exhaust normally. Failures, cancellations, and partial closes remain diagnostic only.

A lifecycle record is keyed by logical key, physical key, and applicability. Its states are `Monitoring`, `Applying`, `Blocked`, `Quarantined`, and `Stale`. LastGood separately identifies the most recently verified safe physical alternative for the same logical group and applicability, with a verified objective envelope. A robust lower/point/upper objective interval expresses uncertainty. Winner selection keeps LastGood while intervals overlap and switches only when the challenger upper bound is strictly below LastGood's lower bound.

## Plan of Work

Milestone 1 implements D1. Add an `EvaluationStatistics.InvocationAggregateObservation` immutable compatibility type and an execution-local mutable accumulator stored on feedback-enabled cloned nodes. Stamp raw and applied row predictions at every open from the costed node, update primitive counts without allocating per result, classify close as exhausted, early-success, partial, cancelled, or failed, and publish one snapshot per completed root. Expand `SemiAntiOutcomeObservation` additively with prediction sums, first-match and exhaustion work, cache counters, termination classification, and root completion. Existing hooks delegate to the new overload. Change LMDB admission to consume snapshots only; derive legacy node counters from the snapshot for telemetry, remove cumulative-row division, and weight each completed query execution once. Tests D1-01 through D1-06 must fail against the old behavior and prove constant and varying sums, hit/miss censoring, rejection of incomplete work, and one sample for 100,000 probes.

Milestone 2 implements D2. Add versioned `LogicalLearningKey`, `LearningApplicability`, and `PhysicalResidualKey` types in the LMDB module. Generate a deterministic logical digest at memo-group interning from canonical logical structure and preserve it during memo merges and contextual rewrite bridges. Stamp every Difference, streaming, memoized, and materialized implementation from one group with the same logical key and a distinct physical key. Split `FrontierLearningModel` persistence into logical output/hit-rate posteriors, physical component residuals, and diagnostic-only family summaries. Estimate in the required order: raw logical estimate; current exact fact or exact-key posterior; structural-bound reconciliation; deterministic physical formula; matching physical residual; risk policy. Persist raw and applied predictions separately and train only from raw. Bump the v17 sidecar to the next version. Older topology-keyed learned state is read only far enough to discard or quarantine deterministically; it is never reinterpreted. Tests D2-01 through D2-06 prove symmetry, physical isolation, compatibility rules, no double correction, exact precedence, and legacy quarantine.

Milestone 3 implements D3. Extend `PackedCostEstimate` with primitive startup-once, invocation, hit probability, first-match, exhaustion, rebind, close, output, distinct-key miss, cache-hit, eviction, materialization build/lookup, memory/spill, and lower/point/upper objective fields. Defaults reconstruct current point behavior for providers without components. Replace winner-total multiplication in `PackedDependentSubqueryCosting`: streaming is outer plus startup once plus invocation count times probability-weighted first-match/exhaustion and rebind/close, plus output. Memoized cost uses distinct-binding misses, cache hits, and evictions. Materialized cost uses one build per materialization-parameter partition plus membership lookups and memory/spill. Extend `FrontierSemiAntiProfile` and the existing LMDB enumeration to compare all three crossover curves. Unknown providers use the conservative decomposition `startup once + N × non-startup child work`; outer and startup are never multiplied. Runtime adaptive materialization remains an unresolved-input fallback, and observations after an algorithm change cannot train the originally planned physical key. Tests D3-01 through D3-06 prove scaling, first-row/all-row separation, perfect and bounded caches, planner materialization crossover, and decorrelation safety for OPTIONAL, unbound values, duplicates, and DISTINCT.

Milestone 4 implements D4. Add bounded persisted lifecycle and LastGood stores keyed by exact logical/physical/applicability identity. Extend packed decisions and traces with objective intervals, lifecycle state, LastGood identity, and switch or rollback reason. `SAFE_CARDINALITY_CORRECTION` computes and persists Monitoring state but preserves existing winner order. `SAFE_PLAN_LIFECYCLE` excludes Blocked and Quarantined candidates when an eligible alternative exists, keeps LastGood across overlapping intervals, requires a challenger's upper bound below LastGood's lower bound to switch, and otherwise chooses the smallest upper bound followed by point cost and existing deterministic ties. If every alternative is blocked, choose the least-regret executable alternative without clearing state.

After a semantically valid completed execution, compare the observed objective with its 99.9 percent predictive upper bound and the stored regression limit, defined as LastGood's verified upper bound or the best eligible alternative's bound. Exceeding both transitions immediately to Quarantined but adds only one statistical sample; lesser verified regressions transition to Blocked. A safe completed challenger becomes LastGood only after verification, preserving the prior LastGood until then. Epoch or objective-version mismatch marks records Stale and returns them to Monitoring. Track the last four selected physical identities per applicability; an A-B-A-B sequence freezes LastGood and blocks the challenger. Every actionable transition increments the feedback/safety revision so `PackedPlanCache` cannot return an unsafe recipe. Bounded-store eviction removes oldest Stale then Monitoring entries and never evicts LastGood or Quarantined merely to admit ordinary monitoring data. Tests D4-01 through D4-05 prove catastrophic quarantine, invalid-event rejection, near-tie retention, decisive switching, and next-pass rollback.

Milestone 5 pre-binds learned-feedback targets. Add an immutable typed `RuntimeFeedbackContract` to `PackedCostEstimate` and preserve it as an object through the physical metadata arena, cached recipe, and materializer. During `DefaultEvaluationStrategy.precompile`, call `EvaluationStatistics.resolveRuntimeFeedbackTarget` once per contracted node and capture the returned direct target in that node's compiled evaluation step. A target owns one execution-local fixed-size primitive accumulator and direct leased references to the current logical, physical, filter/semi-anti, exact-fact, and lifecycle cells. Opens stamp raw and applied primitive predictions directly; result production increments primitive counters; the outermost close publishes the complete recorder array once and releases leases idempotently. Root completion, partial termination, cancellation, and failure are explicit primitive classifications.

No compiled LMDB feedback path may call generic planned/actual metric accessors, query-model metadata maps, string parsers, compatibility observation hooks, or destination maps during open, result production, close, or publication. Add `InvocationAggregateView` so both the mutable accumulator and immutable compatibility snapshot expose one allocation-free observation contract, and add `FeedbackWorkReportingIterator` for direct source-row, seek, expression, hash, path, remote, memory, cache, materialization, and compact implementation-ID counters. Unknown physical IDs reject only physical learning. Missing, invalid, capacity-rejected, generation-stale, and dynamically lateral contracts compile to no-op targets; there is no runtime fallback to parsing keys. Store cells are leased for the query lifetime, bounded eviction skips active, LastGood, and Quarantined cells, and batched publication updates all direct cells under one synchronization boundary. The persisted v20 representation remains unchanged because this milestone changes in-memory routing only.

Tests for milestone 5 prove one target resolution per compiled node and zero more across 100,000 reopens; successful evaluation after generic metric and metadata accessors are armed to throw after precompile; exact constant and varying prediction sums; one root publication per recorder; diagnostic-only incomplete executions; accumulator isolation with safely shared cells; typed cached contracts with next-precompile generation resolution; shared logical but isolated physical cells; direct filter, semi/anti, cache, materialization, algorithm-drift, and distinct-exposure counters; no-op dynamic lateral recompilation; and active/protected eviction, generation invalidation, LastGood, Quarantined, and restart behavior.

Milestone 6 supplies end-to-end proof. Add Q9-01 repeated clean-state stability, Q9-02 q9/q10 compatible sharing and isolation, and Q9-03 sidecar restart persistence. Run focused suites, complete `core/queryalgebra/evaluation` and `core/sail/lmdb` module selections, semantic query-plan snapshots for Medical Records q9 and q10, and the requested q9 benchmark. Resolve all working-tree-only `LmdbEstimateAuditHarnessTest` failures before declaring broad verification green. The result counts must not change. q9 must retain a Difference/materialized alternative without relying on a runtime probe-budget rescue or crushing the outer-row estimate. Benchmark Medical Records query 0 in alternating feedback-off and `OBSERVE_ONLY` runs with isolated stores, three warmup iterations, ten measurement iterations, and one fork. Feedback-on must remain within five percent of feedback-off with no more than five percent planning-time regression. Repeat CPU and allocation profiling and require no learning-attributed generic maps, `HashMap`, string hashing/parsing, boxing, or allocation after precompile. Promote the test deployment profile to `SAFE_PLAN_LIFECYCLE` only after every P0/P1 invariant and both performance gates hold.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Never use `-am` or `-q` for tests, never delete untracked artifacts, and do not reset, commit, push, or change branches.

The initial required build is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

For each new behavior, add the smallest focused test before production code and run it with retained logs. Typical selectors are:

    python3 .codex/skills/mvnf/scripts/mvnf.py InvocationAggregateObservationTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py FrontierLearningModelTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedDependentSubqueryCostingTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedWinnerTableTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbMedicalQ9PlanStabilityIT --it --retain-logs -- -Drdf4j.lmdb.themeRegression.persistentStore.enabled=true

Immediately persist the first failing report in repository-root `initial-evidence.txt` with `scripts/agent-evidence.py`, and preserve every retained `logs/mvnf` report. Re-run each same selector after its production change, then broaden to:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Capture baseline and candidate q9/q10 plans using `.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh` with identical store, theme, selector, query id, and JVM flags. Compare with `--diff-mode structure+estimates`; retain logs under `output/learned-feedback-plan-flip/`. Run the benchmark exactly as requested:

    ./scripts/run-single-benchmark.sh --theme-plan-run --theme-query MEDICAL_RECORDS:9 --warmup-iterations 3 --measurement-iterations 10 --forks 1

If performance exceeds the acceptance limit, rerun with JFR and inspect planning and execution separately before changing code. Do not claim a JVM cause without profile evidence.

Before final verification, ensure all edited Java sources retain their copyright header and the `// Some portions generated by Codex` signature directly below it. Run:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is intentionally the repository-standard exception; test commands remain free of `-q`. Then rerun the root install, focused tests, both module suites, q9/q10/restart integration tests, final snapshots, and the benchmark.

## Validation and Acceptance

D1 is accepted when a constant prediction of two across 49,800 opens records predicted and actual sums of 99,600; predictions 1, 10, and 100 record 111; a successful EXISTS hit trains only first-match work; an exhausted miss records hit=false, exhaustion work, and exact zero for that binding; partial, cancelled, failed, or root-incomplete observations cannot update a logical posterior; and 100,000 probes increase posterior sample count by exactly one.

D2 is accepted when equivalent Difference, streaming, memoized, and materialized alternatives share one logical digest and cardinality correction, while their physical residuals cannot cross algorithms or applicability. Compatible q9/q10 expressions may share exact logical state only when their canonical expression and applicability match. Applied predictions do not train another correction. Current exact facts win over posteriors. Loading v17 state deterministically quarantines or discards unsafe topology-keyed cardinality instead of applying it.

D3 is accepted when doubling uncached invocations doubles only repeated child work; first-match and exhaustion costs diverge correctly; a perfect cache converts repeat probes to lookup cost; bounded-cache cost changes smoothly with evictions; materialization wins at the explicit crossover before a runtime bailout would occur; and decorrelation never changes OPTIONAL, unbound, duplicate, or DISTINCT semantics.

D4 is accepted when a valid catastrophic execution immediately quarantines its physical alternative but supplies one sample, invalid/incomplete execution cannot transition safety state, an interval-overlapping risky challenger does not displace LastGood, a decisively better verified challenger does, the prior LastGood survives until verification, and the next planning pass rolls back after a regression. A-B-A-B freezes LastGood and blocks the challenger. Plan-cache revision changes on every actionable state transition.

End-to-end acceptance requires all focused and module tests green, q9 stable over repeated clean state, q9/q10 isolation under interleaving, lifecycle survival across restart, equal snapshot result counts, and no invalid observation admission or incompatible-key sharing. The learned steady-state q9 median must be within 25 percent of the same-run clean or forced-safe baseline, contain no catastrophic outlier, and add at most five percent planning time. If any gate fails, leave rollout in Monitoring and record the gap here rather than weakening the test.

Pre-binding acceptance additionally requires exactly one resolution per contracted compiled node, no resolution during reopens, one query-level publication, and no ordinary-feedback evaluation access to generic metric/metadata maps or key strings. Medical Records query 0 in `OBSERVE_ONLY` mode must produce identical results and a median no more than five percent above feedback-off in alternating isolated-store runs. Its repeated CPU and allocation profiles must contain no learning-attributed generic metric, `HashMap`, string hashing/parsing, primitive boxing, or post-precompile allocation frames. If the time gate fails after those frames disappear, inline the remaining feedback wrapper state into the affected operator iterators and profile again before broad q9 verification.

## Idempotence and Recovery

All tests, installs, snapshots, and benchmarks are repeatable. The sidecar tests must use isolated temporary stores or explicit test roots so reruns cannot alter user data. Keep old-format fixtures immutable and copy them into test directories before loading. Bounded stores must make repeated load/save deterministic.

Do not use Git reset, restore, clean, checkout, stash, branch changes, commits, or pushes. If an edit is wrong, reverse only the lines introduced by this plan with a reviewable patch. Never remove or rename an unexpected file. If Maven fails offline because an artifact is missing, rerun that exact build once without `-o`, then return offline. For any other build failure, retry the same root install without `-T 1C`. Preserve `maven-build.log`, `initial-evidence.txt`, retained mvnf logs, reports, snapshot logs, benchmark output, and every pre-existing untracked artifact.

## Artifacts and Notes

Initial build evidence:

    [INFO] Reactor Summary for Eclipse RDF4J 6.1.0-SNAPSHOT:
    ...
    [INFO] RDF4J: Query algebra - evaluation ........ SUCCESS [  4.102 s]
    [INFO] RDF4J: LmdbStore ......................... SUCCESS [  6.021 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time: 34.653 s (Wall Clock)

Initial D1-02 failing evidence:

    Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    expected: <110.99999999999996> but was: <64.77681746006242>

The research workbook Design Tests sheet contains the source matrix D1-01 through Q9-03 at `/tmp/codex-learned-feedback-workbook-019fd06a/learned-feedback-plan-flip-comparison-matrix.xlsx`. That path is read-only source material; executable tests belong in the repository modules and must be understandable without the workbook.

## Interfaces and Dependencies

No new dependency is permitted or needed. Use existing JDK primitives, RDF4J model types, and bounded maps.

In `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/EvaluationStatistics.java`, add an immutable `InvocationAggregateObservation` and a compatibility overload such as `recordOperatorOutcome(QueryModelNode node, InvocationAggregateObservation observation)`. Preserve `recordOperatorOutcome(QueryModelNode)` by delegating with a snapshot derived from legacy telemetry. Expand `SemiAntiOutcomeObservation` additively and keep existing constructors or factories delegating to safe defaults.

In the evaluator, use a fixed-size mutable accumulator with primitive fields. It must expose explicit methods for open-time raw/applied predictions, result/work increments, termination classification, semi/anti outcomes, cache activity, and an immutable close/root snapshot. No method invoked once per result may allocate.

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`, define versioned immutable `LogicalLearningKey`, `LearningApplicability`, and `PhysicalResidualKey` value types with deterministic equality, hashing, serialization, and stable digest telemetry. Extend or reshape `FrontierLearningModel` and `LmdbOperatorFeedbackStats` into bounded logical, physical, lifecycle, and diagnostic stores. Increase the sidecar persistence version from 17 and make legacy handling explicit.

In `PackedCostEstimate`, add primitive setters/getters for dependent components and lower/point/upper objectives. Existing construction must produce lower=point=upper and preserve behavior when no new component profile is present. Extend winner/certificate telemetry additively; do not break existing providers.

Add experimental `RuntimeFeedbackDescriptor`, `RuntimeFeedbackContract`, `RuntimeFeedbackTarget`, and `InvocationAggregateView` interfaces in the evaluation module. The descriptor is opaque to the generic evaluator. The contract holds the descriptor, raw/applied primitive prediction vectors, objective envelope, compact typed algorithm/access identifiers, applicability epochs, and admission flags. The target exposes only primitive recording, one batched publication, and idempotent lease release. `EvaluationStatistics.resolveRuntimeFeedbackTarget(QueryModelNode, RuntimeFeedbackContract)` defaults to a precompiled no-op, and the LMDB implementation resolves current logical, physical, filter/semi-anti, exact-fact, and lifecycle cells from its typed descriptor during precompile. Preserve compatibility observation methods through an explicit legacy adapter, but do not invoke that adapter from compiled LMDB execution.

Add an opaque contract setter/getter to `PackedCostEstimate`; carry the same object through `PackedPhysicalMetadataArena`, `PackedPlanRecipe`, and `PackedPlanMaterializer` without external-form conversion. Add primitive `FeedbackWorkReportingIterator` methods additively so source iterators can expose work without generic metrics. A bounded primitive distinct-binding tracker may be allocated once per streaming recorder; result production must allocate nothing. Keep sidecar v20 unchanged.

In `LeoRolloutProfile`, add `SAFE_PLAN_LIFECYCLE` after the Monitoring-compatible default and before broader experimental modes. Fine-grained kill switches may suppress typed learning, logical application, component refinement, or lifecycle enforcement, but disabling one must fall back to conservative costing or no learning and must never reactivate cumulative-row division, physical-to-logical reinterpretation, or whole-subplan multiplication.

Plan revision note (2026-08-05 07:54Z): Created the production implementation plan after the clean tracked-worktree audit and mandatory root build. It separates the untouched research brief from executable D1–D4 milestones and records the test-first, persistence, performance, and rollout contracts requested by Håvard.

Plan revision note (2026-08-05 08:24Z): Marked D1 complete after the failing varying-prediction reproduction changed from 64.77681746006242 to the exact summed correction and all focused accumulator/LMDB compatibility tests passed. Advanced the sole active milestone to D2 key and store separation.

Plan revision note (2026-08-05 08:45Z): Marked D2 complete after exact logical/applicability matching, physical residual isolation, v18 persistence, and v17 quarantine passed focused model, sidecar, and planning tests. Advanced the sole active milestone to D3 component costing.

Plan revision note (2026-08-05 09:16Z): Marked D3 complete after explicit dependent formulas, startup-once fallback, bounded-cache telemetry, reopen scaling, and semi/anti planner crossover passed focused tests. Advanced the sole active milestone to D4 persisted lifecycle integration.

Plan revision note (2026-08-05 11:20Z): Marked D2 complete after alpha-normalized input-binding applicability passed focused key and planning tests. Reopened D3 as the sole active milestone because the full q9 reproduction proved that ordinary nested joins under inherited prefixes still price contextual child work once instead of by execution partition.

Plan revision note (2026-08-05 12:06Z): Kept D3 as the sole active milestone after postmortem OOM classification showed a selected 2.489-billion-row cartesian intermediate. The invocation multiplier is present, but component-scoped child result work was incorrectly classified as fixed contextual output; the next focused test separates component output from contextual output before changing production costing.

Plan revision note (2026-08-05 12:51Z): Marked D3 complete after focused inherited-prefix and component-output tests, full-prefix logical-row preservation, absolute logical posterior D2-04, v20/v19 quarantine D2-06, the 84-test persistence class, and six-run q9 stability all passed. Advanced the sole active milestone to D4 lifecycle acceptance.

Plan revision note (2026-08-05 13:02Z): Marked D4 complete after D4-01–05, period-two, stale/reset, protected-eviction, robust-winner, all-blocked fallback, v20 restart persistence, and default Monitoring/enforced-state separation passed focused tests. Advanced the sole active milestone to end-to-end verification.

Plan revision note (2026-08-05 16:05Z): Closed working-tree-only LMDB integration repair after relation-identity and applicability normalization fixes brought the complete 89-test Frontier planner class to exact clean-`HEAD` failure parity. Advanced the sole active milestone to broad module, snapshot, and benchmark verification.

Plan revision note (2026-08-05): Inserted pre-bound runtime feedback as the sole active milestone after Medical Records query 0 measured 83.921 ms/op with feedback versus 65.569 ms/op without it. The profile identified generic metric maps, `HashMap`, boxed metric values, and close/root key reconstruction as the dominant learning overhead. Broad verification now follows typed contract propagation, one-time precompile resolution, direct primitive accumulation/publication, dynamic-lateral suppression, lease/generation safety, and the five-percent query-0 performance gate.

Plan revision note (2026-08-06 00:27Z): Marked pre-bound runtime feedback complete after the Group child boundary test, complete focused evaluator suites, final q0 median overhead of 3.07 percent, matched CPU overhead of 3.42 percent, a clean allocation profile with no learning-attributed post-precompile allocation, and a 29.62 percent uncached-planning improvement. Advanced the sole active milestone to broad verification, snapshots, and benchmarks.

Plan revision note (2026-08-06 05:41Z): Closed broad verification after the mandatory formatter/install pass, exact clean-`HEAD` module failure parity, all 48 estimate-audit tests, the 3/3 q9/q10/restart integration class, exact snapshot result fingerprints, and the q9 7.60-percent same-jar safety comparison. Advanced the sole active milestone to the final dirty-worktree and artifact audit.

Plan revision note (2026-08-06 06:12Z): Closed the final audit after the required signature correction, a second successful repository formatter, the 34.382-second offline root clean install, the 14/14 pre-bound evaluator test, empty whitespace/staging/signature checks, and a complete tracked/untracked scope review. The implementation is complete; the sole living operational milestone keeps rollout in Monitoring until inherited baseline-suite failures are resolved and `SAFE_PLAN_LIFECYCLE` is explicitly promoted.

Plan revision note (2026-08-06 06:45Z): Hid `optimizer.frontierLearningKey` exclusively from the human-readable `GenericPlanNode` renderer while preserving the planned metric and JSON serialization. The focused red/green test, 28-test renderer class, 234-test query module, repository formatter, and 34.108-second offline root clean install all passed. Restored Monitoring rollout containment as the sole living operational milestone.
