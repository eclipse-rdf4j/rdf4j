# Restore optimizer authority in LMDB native execution

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while work proceeds. Maintain it in accordance with
`.agent/PLANS.md` at the repository root.


## Purpose / Big Picture

The LMDB native evaluator compiles already-optimized RDF4J query algebra into cursor-oriented physical plans. The
optimizer's selected join order and filter boundary must therefore be the initial authority. Today the compiler
retains child order but, when it flattens an inner-join bag, reduces a filter boundary to the slots the filter reads.
The ordinary native plan then moves the filter to the earliest legal depth, while the adaptive path begins at the
deepest legal depth and dispatches before established batch, parallel, and factorized strategies. Both choices can
silently discard optimizer work before runtime evidence exists.

After this plan is complete, ordinary native evaluation starts at the optimizer's filter boundary. Adaptive
placement is a default-on final nested-loop fallback and can move one filter only after a complete bounded observation
window proves lower work. Safe equality and `IN` filters use the same standard Filter-to-VALUES optimizer in both
LMDB optimizer pipelines instead of a native compiler gate driven by learned statistics. Local scan-once operators
share a bounded observed-work admission controller whose build attempt cannot be blocked forever by an estimate.
Broader parallel, join, top-k, and specialization thresholds are measured but not changed.

The working directory is the repository root, `/Users/havardottestad/Documents/Programming/rdf4j`. Java 25 is the
active runtime. All Maven commands use the workspace-local `.m2_repo`; tests never use `-am` or `-q`.


## Progress

- [x] (2026-07-19 08:57Z) Read repository, ExecPlan, performance, test-runner, and query-plan snapshot instructions.
- [x] (2026-07-19 08:57Z) Confirm the existing `ThemeQueryBenchmark.java` change is user-owned and non-overlapping.
- [x] (2026-07-19 08:57Z) Complete the mandatory root quick clean install successfully in 32.074 seconds.
- [x] (2026-07-19 09:02Z) Add and observe the smallest failing optimizer-placement contract test; preserve its report.
- [x] (2026-07-19 09:24Z) Preserve planned filter depths and repair adaptive sampling, selection, admission, dispatch, and opt-in rollout.
- [x] (2026-07-19 12:18Z) Prevent ordered access-path promotion across an optimizer-planned filter boundary.
- [x] (2026-07-19 12:36Z) Pass Slice 1 adaptive overhead, allocation, and target-workload performance gates; keep rollout opt-in.
- [x] (2026-07-19 18:26Z) Diagnose the adaptive-theme regression and restore default-on rollout at the user's direction.
- [ ] **In progress:** reuse the standard Filter-to-VALUES optimizer in the LMDB sketch pipeline and remove native telemetry gating.
- [ ] Add shared observed-work build admission and migrate membership, factorized, chunk, and left-join operators.
- [ ] Add measurement-only telemetry and boundary benchmarks for broader fixed thresholds.
- [ ] Run focused/module verification, query-plan snapshots, paired JMH gates, formatting, and final install.


## Surprises & Discoveries

- Observation: The closed `EXECPLAN-adaptive-filter-placement.md` says adaptive dispatch belongs immediately before
  the final nested-loop fallback and documents short-limit, work, evaluation, cost, and fan-out admission. The only
  implementation commit instead dispatches adaptive first, begins at the deepest legal depth, and has a test tripwire
  forbidding planner-estimate consultation.
  Evidence: `NativeRowsIteration.initialize()` in `LmdbNativeRowStep.java`, `AdaptiveFilterSession` in
  `LmdbNativeAdaptiveFilterPlacement.java`, and `LmdbNativeAdaptiveFilterPlacementTest`.

- Observation: `MultiJoinPlan.derive()` retains compiler-provided child order but places every flattened filter at
  the earliest depth covering its read mask. The mask proves legality but does not encode the optimizer-selected
  boundary.
  Evidence: `SlotPlan.collectFlattenable()` in `LmdbNativeSlotPlan.java` and `MultiJoinPlan.derive()` in
  `LmdbNativeJoinPlans.java`; focused test expected depth 1 but observed depth 0.

- Observation: The standard fallback LMDB optimizer already inherits `FilterInValuesOptimizer`; the LMDB sketch
  pipeline omits it. The native compiler compensates with a direct filter-to-probe path whose eligibility depends on
  `EXACT` or `LEARNED_FILTER` telemetry.
  Evidence: `StandardQueryOptimizerPipeline.java`, `LmdbStandardQueryOptimizerPipeline.java`,
  `LmdbQueryOptimizerPipeline.java`, and `LmdbNativeAggregatePatternCompiler.java`.

- Observation: The adaptive theme begins at the optimizer-planned depth one and, after exactly 1,024 observed
  filter evaluations, commits one move to depth three. The previous integration assertion assumed no movement and
  therefore failed with initial depth one versus final depth three; all catalog results remained identical.
  Evidence: `LmdbAdaptiveFilterPlacementThemeIT`, Failsafe log `logs/mvnf/20260719-091912-verify.log`, and green
  rerun `logs/mvnf/20260719-092404-verify.log`.

- Observation: Ordered DISTINCT planning could promote a later pattern across a planned filter boundary while
  retaining the same numeric depth. That replaced the optimizer's planned prefix even though the existing
  `delaysFilter` check reported no delay.
  Evidence: `orderedDistinctDoesNotPromoteAcrossPlannedFilterBoundary` failed in
  `logs/mvnf/20260719-121658-verify.log` and passed after the boundary guard in
  `logs/mvnf/20260719-121804-verify.log`.

- Observation: Module verification produced 1,842 green Surefire/Failsafe results after excluding only
  `LmdbThemeQueryRegressionIT`, but `mvnf` did not receive EOF after Maven and its JVM exited. An inherited stdout
  descriptor left the Python wrapper blocked, so it was interrupted only after reports were persisted. The excluded
  fixture had previously spent 49 minutes rebuilding full-theme estimator state; the user-owned expanded
  `ThemeQueryBenchmark` matrix must remain untouched.
  Evidence: `logs/mvnf/20260719-121949-verify.log`, report summary in `initial-evidence.txt`, and thread dumps captured
  during the two unbounded fixtures.

- Observation: The two latest adaptive-theme benchmark reports did not exercise the same execution path. The
  11:13 report admitted adaptive placement for all eleven queries and averaged 25.95 ms/op; the 19:45 report used
  nested-loop fallback for all eleven and averaged 40.92 ms/op. The intervening Slice 1 commit changed the unset
  property default to false, while `ThemeQueryBenchmark` did not set the property. The new adaptive contract also
  begins at planned depth one and observes 1,024 filter evaluations plus 100,000 work units before moving, whereas
  the old implementation began immediately at deepest depth three and evaluated the filter only 256 times.
  Evidence: `results-2026-07-19.md`, `results-2026-07-19-2.md`, commit `40e31982d9`, and the three-fork JDK 26
  query-zero checks (41.529 ms/op enabled versus 80.242 ms/op disabled).


## Decision Log

- Decision: Optimizer child order and filter boundary are the initial physical baseline. Native code may move a
  filter only after observed work, not from an open-time estimate-only arbitration.
  Rationale: This preserves the standard optimizer pipeline unless within-query evidence supplies the requested
  very good reason to depart.
  Date/Author: 2026-07-19 / Codex.

- Decision: Keep at most eight adaptive gates but sample a wide legal envelope instead of rejecting it.
  Rationale: Gate arrays and passive counters are real per-execution resources; farthest-point sampling keeps the
  envelope representative without unbounded state.
  Date/Author: 2026-07-19 / Codex.

- Decision: Adapt one filter and choose it by saturating expression-cost times legal-depth span.
  Rationale: Simultaneous filter movement creates interacting experiments. Cost times span is a deterministic proxy
  for avoidable work and is strictly better than lowest-ID selection; actual work still controls movement.
  Date/Author: 2026-07-19 / Codex.

- Decision: Restore Filter-to-VALUES parity through the shared optimizer, not through an unconditional LMDB native
  rewrite. Do not add OR-of-equalities in this work.
  Rationale: The optimized algebra should express the rewrite. Reusing the established pass preserves its semantic
  gates and avoids telemetry-driven execution policy.
  Date/Author: 2026-07-19 / Codex.

- Decision: Keep adaptive placement opt-in until exact correctness, no-op allocation/throughput, and at-least-ten-
  percent win gates pass.
  Rationale: The current default-on implementation violates the intended dispatch and baseline contracts.
  Date/Author: 2026-07-19 / Codex.

- Decision: Slice 1 passes and Slice 2 may proceed, but adaptive remains opt-in until the final rollout decision.
  Rationale: No-filter and ineligible medians meet the one-percent overhead gate, conservative confidence-ratio upper
  bounds are 2.70% and 1.71%, allocation is unchanged within profiler resolution, and committed-move workload medians
  improve by 66.1% to 86.9%.
  Date/Author: 2026-07-19 / Codex.

- Decision: Supersede the interim opt-in rollout and enable adaptive placement by default; retain the explicit
  `rdf4j.lmdb.adaptiveFilterPlacement.enabled=false` opt-out.
  Rationale: The user explicitly requires default-on behavior after the benchmark audit showed that the opt-in
  default silently turned the adaptive theme into a nested-loop benchmark. Slice 1's correctness, overhead,
  allocation, and committed-move gates had already passed. An unset-property unit contract and an end-to-end theme
  integration test now cover the default path.
  Date/Author: 2026-07-19 / Håvard and Codex.

- Decision: Measure the 4,096, 50,000, 100,000, 32,768, and 32-value thresholds without changing production
  behavior in this plan.
  Rationale: These thresholds bound startup or memory work. Actual miss evidence is required before designing a
  replacement, and parallel queue backlog cannot be observed before parallel resources exist.
  Date/Author: 2026-07-19 / Codex.


## Outcomes & Retrospective

The required root quick clean install is green. Slice 1 focused verification is green: 67 related unit tests and
the adaptive theme integration test pass. Broad report-level verification is also green for 1,842 tests with the
single documented benchmark-fixture exclusion. Ordinary plans retain early, middle, and late optimizer boundaries;
adaptive placement starts at the planned depth, samples at most eight depths, and adapts one highest-regret filter.
After the adaptive-theme regression audit, the user selected default-on rollout; explicit false remains the opt-out.
The unset-property unit contract and default-path theme integration test pass.


## Context and Orientation

`core/sail/lmdb` contains the LMDB store and its experimental native query evaluator. RDF4J first runs a query
optimizer pipeline over algebra nodes such as `Join` and `Filter`. `LmdbQueryOptimizerPipeline` is the statistics-
aware LMDB pipeline; `LmdbStandardQueryOptimizerPipeline` wraps the ordinary RDF4J pipeline when LMDB join estimates
are unavailable. `LmdbNativeAggregatePlanner` then compiles the optimized algebra into `SlotPlan` objects.

`SlotPlan` is the internal physical-plan interface in
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSlotPlan.java`. A `MultiJoinPlan` is a
flat inner-join bag containing ordered `children` plus `MaskedFilter` entries. A depth is a zero-based child position;
a filter at depth one runs after child one has joined with the prefix. A filter read mask records which native slots
its expression needs. It determines the earliest legal depth but does not say where the optimizer placed the filter.

`NativeRowsIteration.initialize()` in `LmdbNativeRowStep.java` chooses an execution strategy. Batch execution can use
merge or hash join, parallel execution can use factorized worker plans, factorized execution can replace enumeration
of unused tails with counts, and the final ordinary path is a nested-loop cursor chain. Adaptive placement belongs
only at that last seam.

`LmdbNativeAdaptiveFilterPlacement.java` installs one filter gate at each candidate depth and owns a primitive
`AdaptiveFilterSession`. Passive gates count arrivals while the active gate evaluates the target. A complete prefix
window is the safe movement boundary because descendants for the preceding prefix have drained. The existing
controller charges seeks, join rows, replays, materialization, and filter evaluations and supports trial, rollback,
cooldown, and commit.

`FilterInValuesOptimizer` in `core/queryalgebra/evaluation` changes safe direct equality, `sameTerm`, and `IN` filters
into a finite `VALUES` semijoin. It caps distinct constants at 64 and rejects unsafe equality families, scope changes,
non-repeatable arguments, missing assured bindings, and binding-injection hazards. The standard fallback already
runs this pass. The LMDB sketch pipeline must run the same pass after its join optimizer and before order/limit.

A scan-once conversion pays once to build an in-memory lookup instead of repeatedly probing and scanning LMDB. The
factorized tail, chunk pipeline, left-join payload, and membership operators currently make similar decisions with
different fixed thresholds and static-estimate comparisons. `RuntimeBuildAdmission` will be a package-private,
primitive state machine shared by these operators. Its states are `PROBING`, `BUILDING`, `COMMITTED`, and `REFUSED`.


## Plan of Work

Milestone one restores the optimizer/native contract. Add focused tests in
`LmdbNativeAdaptiveFilterPlacementTest` and a new optimizer-contract test where necessary. A filter wrapped around
the first two members of a later three-member join must remain after member two, even when its read slots exist after
member one. Add `plannedDepth` to `MaskedFilter`; set it to the last child inside the filter's algebra boundary; and
offset it when a nested `MultiJoinPlan` is appended to an existing flattened bag. Ordinary derivation uses the planned
depth, constrained only so it cannot precede its required slots. Factorized sinking remains separate and keeps its
existing tests.

Repair adaptive placement in the same milestone. Dispatch it after batch, parallel, and factorized attempts. Start
the session at `plannedDepth`. For a legal envelope wider than eight, seed earliest, planned, and deepest, then add
the legal depth with maximum distance from the selected set until eight are present, preferring shallower ties.
Choose one target using a saturating `estimatedCostUnits * (legalDepthCount - 1)` score. Require 1,024 completed
planned-prefix observations and 100,000 actual work units before the first proposal. Preserve existing measured
proposal, rollback, cooldown, blacklist, and commit behavior. Decline short finite slices, correlation, order,
non-repeatability, native `EXISTS`, and unsafe extension replay. Change the unset feature property to false until the
final performance decision.

Milestone two makes exact-filter behavior a pipeline decision. Add an `@InternalUseOnly` static accessor on
`StandardQueryOptimizerPipeline` returning the existing filter optimizer as `QueryOptimizer`, and insert it after
`LmdbSketchJoinOptimizer` in `LmdbQueryOptimizerPipeline`. Add generic optimizer tests if the accessor changes
visibility expectations and LMDB integration tests for cold and warm equality/IN queries with 1, 32, 64, and 65
values. Remove the top-level native compiler attempt that consults learned filter statistics; a remaining algebra
`Filter` stays a filter. Preserve compilation of optimizer-produced `VALUES` and separately gated left-join probes.
Do not synthesize selectivity telemetry and do not add OR-of-equalities.

Milestone three adds `RuntimeBuildAdmission` in the LMDB evaluation package. It records primitive counters for
distinct probes, seeks, scanned rows, matches, batch time, build rows, and refusal. Membership may try after 64 misses;
the other scan-once operators may try after 1,024 probes. Static estimates size a capacity that is clamped by existing
budgets but cannot veto the trial. A build aborts when an existing resource budget refuses it or when build work is
greater than twice the already-paid probe work; an aborted or failed build publishes nothing and cannot retry. Use an
injectable nano-time source in unit tests and sample time only at batch boundaries. Migrate membership, factorized
tail, chunk pipeline, and left-join payload in that order, stopping between operators for correctness and paired
benchmark evidence.

Milestone four is measurement-only. Extend telemetry-level explanation with bounded decline reasons, static estimate
classification, actual fallback work, old-threshold crossing, and budget refusal for parallel rows, parallel
aggregation, hash/merge join, top-k, specialization, and literal anchors. Add benchmark states at the requested
boundaries and with deliberately wrong estimates. Do not alter any production threshold, merge/hash precedence,
background work, or decorator eligibility. Summarize findings and rank follow-up work in this ExecPlan.


## Concrete Steps

Before any tests in a fresh conversation, run the required root install from the repository root:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
      | tee maven-build.log \
      | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Add and run the smallest failing test for each production slice with `mvnf`, retaining logs. Representative focused
commands are:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAdaptiveFilterPlacementTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeStrategyPriorityTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAggregateFilterSemanticsTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAggregateValuesCompilerTest --retain-logs

Append compact first-failure or pre-green evidence to the existing `initial-evidence.txt`; never replace its previous
adaptive evidence. After a focused failure is captured, make only the production edit required by that test, rerun
the identical selector, then broaden to the containing class.

For query-plan comparisons, use the query snapshot wrapper with identical store, theme, query index, query ID, JVM
flags, and prepared data. Store logs outside tracked source:

    ./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-lmdb-baseline.log -- \
      --store lmdb --theme MEDICAL_RECORDS --query-index 0 --query-id lmdb-native-admission-med-q0

    ./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-lmdb-candidate.log -- \
      --store lmdb --theme MEDICAL_RECORDS --query-index 0 --query-id lmdb-native-admission-med-q0 \
      --compare-latest --diff-mode structure+estimates

Use the supported single-method benchmark wrapper. Run baseline and candidate with the same JDK, store, data, and
flags and at least three independent forks:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAdaptiveFilterPlacementBenchmark \
      --method <oneBenchmarkMethod>

After each gated slice, run the relevant test class, then the complete LMDB module:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before finalization, run copyright validation, formatting, the focused and module tests, and the root clean install:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is the repository-mandated exception that uses `-q`; no test command may use it.


## Validation and Acceptance

The optimizer contract is accepted when tests demonstrate filters at early, middle, and late algebra boundaries;
prebound input; two, eight, nine, and wider legal envelopes; multiple eligible filters; safe and unsafe extensions;
and custom and fallback optimizer pipelines. Ordinary native execution must begin at the optimizer depth. Adaptive
execution must publish that planned start depth, must not move before 1,024 completed prefixes and 100,000 observed
work units, and must retain exact sequence and multiplicity against the static native and generic evaluators.

Exact-filter parity is accepted when cold and warm equality/IN tests with 1, 32, 64, and 65 constants cover
duplicates, missing IDs, external bindings, contexts, unsafe literal families, scope boundaries, injection hazards,
and filter errors. The optimized algebra may differ only by the standard Filter-to-VALUES rewrite, and result counts
must match. If a low-selectivity LMDB workload regresses by both more than three percent and more than 100
microseconds per invocation, leave this milestone unshipped and record the need for optimizer-level costing; never
restore a learned-telemetry native gate.

Runtime build admission is accepted when tests cover underestimation, overestimation, infinity, unknown estimates,
budget refusal, early close, cancellation, build failure, no retry, and immutable publication. A refused attempt must
spend no more than twice the already-paid probe work. Each migrated target must improve its paired benchmark median
by at least ten percent, while neighboring non-converted workloads regress by at most three percent.

Adaptive default-on requires exact checksums, disabled/ineligible median overhead no greater than one percent, a
confidence upper bound below three percent, no new per-row allocation, and at least ten percent median improvement
for every workload in which a move commits. Because the user selected default-on explicitly, a failed gate blocks
completion and requires a root-cause fix; it must not be hidden by silently reverting the default.

Full acceptance also requires the complete `core/sail/lmdb` verify, formatter and copyright checks, and final root
quick clean install to pass. Query-plan snapshots must use the same query, data, revision, and flags; historical theme
best results are regression sentinels, not causal baselines.


## Idempotence and Recovery

Builds, tests, snapshots, and benchmarks are safe to rerun. Never delete untracked artifacts. Preserve the existing
user modification in `ThemeQueryBenchmark.java` and stage only files belonging to this ExecPlan. If a behavior test
reveals production was edited before a failing test was observed, revert only this plan's production edit, record the
recovery in this document, and resume at the test. If a performance gate fails, keep the relevant feature disabled,
profile the exact workload, and record the result before changing direction. A failed runtime build must discard its
private partial structure and resume the original operator without restarting the query.


## Artifacts and Notes

The initial root quick clean install completed at 2026-07-19 08:57Z with `BUILD SUCCESS` in 32.074 seconds. Full
output is in `maven-build.log`.

`initial-evidence.txt` already contains five lines of green evidence from the earlier adaptive implementation. Append
new labelled sections; do not replace those lines. The first Slice 1 failure is preserved there and in
`logs/mvnf/20260719-090132-verify.log`.

The only pre-existing tracked change at plan start is:

    M core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java


## Interfaces and Dependencies

No supported public API, dependency, storage format, or configuration schema changes. Add an `@InternalUseOnly`
optimizer accessor returning `QueryOptimizer`; internal `MaskedFilter.plannedDepth` and envelope metadata; and a
package-private `RuntimeBuildAdmission`. Extend telemetry-level explanation only with bounded planned/final depth,
candidate, decline, exact-VALUES, and runtime-build metrics. Retain snapshot, replay, order, volatility, correlation,
task, hash, memo, sort, generated-code, row, and byte guards; external-root chunk default-off behavior; and CSR cache
admission.


Revision note (2026-07-19 08:57Z): Created the governing plan from the agreed planner-first remediation, recorded the
green root baseline and current dirty-tree ownership, and set the first optimizer-contract test as the only active
step.

Revision note (2026-07-19 09:02Z): Preserved the focused depth-0-versus-depth-1 contract failure and advanced the
single active step to the minimal planned-depth production fix.
