# Make hash joins share a correct key contract and pay for their real work

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while the work proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J's packed Cascades planner can currently choose an independent hash join because it prices input rows, while
the runtime may examine the Cartesian product of those rows. This happens when the runtime lookup key contains a
synthetic constant or a variable that is not present in every row. After this change, the planner and iterator use
the same two-part contract: an assured lookup key for bucket selection and the complete set of shared variables for
SPARQL compatibility. The planner also prices bucket fan-out, candidate examinations, and peak hash-table memory.

The behavior is visible in focused iterator and planner tests and in `AASQueriesBenchmark`: queries 2 and 3 finish
without the five-second timeout, return the same results as iterator execution, and only retain hash plans whose
complete cost wins.

## Progress

- [x] (2026-08-06 10:48Z) Read repository, performance, Maven, and ExecPlan instructions.
- [x] (2026-08-06 10:48Z) Run the mandatory root quick clean install; all modules built successfully.
- [x] (2026-08-06 10:52Z) Add the immutable shared binding contract and focused schema tests; 5 tests pass.
- [x] (2026-08-06 10:57Z) Split hash lookup from compatibility filtering; 13 iterator tests pass.
- [x] (2026-08-06 11:11Z) Propagate packed lookup and compatibility masks through costing traces; 14 lifecycle tests pass.
- [x] (2026-08-06 11:48Z) Add Frontier fan-out estimation, both build orientations, and LMDB peak-memory pricing.
- [x] (2026-08-06 12:31Z) Add packed, LMDB, iterator-equivalence, and AAS integration regressions.
- [x] (2026-08-06 13:03Z) Run focused evaluation and LMDB selections; all new hash-contract tests pass.
- [x] (2026-08-06 13:24Z) Run both affected module suites and classify unrelated pre-existing failures.
- [x] (2026-08-06 15:13Z) Run three AAS trials: Q1 passes; Q2 and Q3 complete but miss their score gates.
- [x] (2026-08-06 15:21Z) Capture Q2/Q3 JFR and isolate repeated property-path AST cloning as the hot allocation path.
- [x] (2026-08-06 15:36Z) Precompile per-iteration property-path expansion shapes and verify path semantics.
- [x] (2026-08-06 16:04Z) Guarantee one complete correlated filter schedule per prepared region under bounded search.
- [x] (2026-08-06 16:13Z) Re-run focused regressions and both affected module suites; all task-specific tests pass.
- [x] (2026-08-06 16:42Z) Run nine final AAS trials and compare medians; Q1, Q2, and Q3 pass every gate.
- [x] (2026-08-06 17:02Z) Run the full formatter and final root quick clean install; the reactor is green.
- [x] (2026-08-06 17:15Z) Audit headers and agent attribution for all 97 in-scope changed Java sources.

## Surprises & Discoveries

- Observation: The existing worktree contains extensive uncommitted optimizer and telemetry work that predates this
  task, including edits to the exact planner and LMDB classes this change must extend.
  Evidence: `git status --short --untracked-files=no` listed 56 modified tracked files and `git diff --stat` reported
  7,881 insertions. All edits in this plan must preserve those changes and stay narrowly scoped.
- Observation: `StreamBindingSchema` already computes possible and assured runtime bindings while
  `BindingUniverse.plannerName` excludes synthetic `_const_` names. The packed codec separately omits constant `Var`
  names from its binding masks.
  Evidence: inspection of `StreamBindingSchema`, `BindingShapeAnalyzer`, `BindingUniverse`, and `PackedBindingFacts`.
- Observation: `HashJoinIteration` already has defensive indexes for partial build keys, but one `joinAttributes`
  array controls both bucket lookup and compatibility. Its current `hashProbeRows` counter mostly counts candidate
  rows rather than base probe operations.
  Evidence: `HashJoinIteration.matchingHashRows`, `compatibleIterator`, and `hashJoinAttributeNames`.
- Observation: Counting candidates lazily at iterator consumption produces truer runtime feedback than charging an
  entire bucket at lookup time, especially when a consumer closes after its first result.
  Evidence: the new iterator wraps both direct buckets and compatibility-filtered buckets in one counting cursor.
- Observation: Packed possible/assured masks already model UNION, projection, extension, grouping, assignments, and
  other binding-shape operators; composing prefix masks preserves those semantics without reconstructing algebra.
  Evidence: `PackedBindingFacts.deriveRelationOutputs`, `deriveRelationAssuredBindings`, and the green packed trace
  lifecycle suite after adding both mask IDs to invocation fingerprints and replay.
- Observation: All three Q2 and Q3 benchmark trials complete without a hash plan or timeout, but their medians are
  171.703 ms/op and 555.660 ms/op respectively. Q3 changes from an approximately 800 ms property-path-late plan to
  an approximately 260 ms property-path-early plan only on the fourth execution.
  Evidence: `profiles/lmdb/aas-hash-contract-acceptance-q{2,3}-trial*.txt`.
- Observation: Suppressing optional hash candidates does not change Q3's plan transition, so rejected hash
  alternatives are not consuming the bounded search opportunity that produces the regression.
  Evidence: `profiles/lmdb/aas-query3-hash-candidates-disabled-diagnostic.txt` and a repeated diagnostic score of
  805.778 ms/op followed by 244.205 ms/op.
- Observation: JFR attributes the dominant avoidable allocation/CPU path to `PathIteration.createIteration`, which
  clones a heavily annotated path expression for every frontier expansion. Each query-model clone copies up to seven
  metric/metadata maps before the strategy recompiles the clone.
  Evidence: `profiles/lmdb/aas-query{2,3}-hash-*-v24.jfr`; hot stacks contain `HashMap.putMapEntries` ->
  `AbstractQueryModelNode.clone` -> `StatementPattern.clone` -> `PathIteration.createIteration`.
- Observation: Removing the measured clone hotspot made each selected plan cheaper but did not by itself make the
  cold bounded search retain Q3's property-path-early plan. The remaining problem was search scheduling, not hash
  selection or hash execution.
  Evidence: the post-precompilation AAS plans still changed after feedback while hash-candidate suppression left the
  transition unchanged.
- Observation: A prepared correlated filter region could spend its bounded lattice budget on incomplete subsets and
  starve a later sibling region before either had published a complete filtered incumbent. Mutable dense state then
  allowed optional exploration to invalidate the evidence lineage of a previously seeded winner.
  Evidence: `initial-evidence.correlated-seed.txt` records the new one-method regression failing with expected `1.0`
  but actual `0.0`; the same selection and the full 70-test `PackedSearchTest` class pass after the scheduler fix.
- Observation: The repository copyright helper does not prune retained `.mvnf` workspaces and spends its traversal
  walking their generated POM forests before reaching project sources.
  Evidence: process inspection showed its `find` process consuming CPU inside the checkout after eight minutes. A
  direct equivalent audit over the changed/untracked implementation scope checked 97 Java sources successfully.

## Decision Log

- Decision: Define lookup bindings as the intersection of both arguments' assured bindings, and compatibility
  bindings as the intersection of both arguments' possible bindings.
  Rationale: Assured bindings are safe hash-key components; possible bindings are exactly the variables that can
  conflict under SPARQL solution-mapping compatibility.
  Date/Author: 2026-08-06 / Codex.
- Decision: Keep legacy array constructors and interpret their array as both lookup and compatibility bindings.
  Rationale: This retains source and binary compatibility for internal/federation callers while algebra-aware core
  call sites move to the richer contract.
  Date/Author: 2026-08-06 / Codex.
- Decision: Define hash probe work as probe-input rows plus candidate examinations, while publishing both components.
  Rationale: Every probe performs lookup work even for an empty bucket, and every bucket member or fallback row is
  examined before it can become an output.
  Date/Author: 2026-08-06 / Codex.
- Decision: Use exact joint-key overlap when Frontier provides primitive key relations, joint NDV otherwise, and the
  Cartesian product when key evidence is absent or no assured key exists.
  Rationale: This prices skew and multi-column keys without making an optimistic assumption when evidence is weak.
  Date/Author: 2026-08-06 / Codex.
- Decision: Add peak memory rows once to the LMDB objective and retain the existing hard materialization cap.
  Rationale: Build insertion work and retained-memory pressure are separate costs; counting peak memory once avoids
  multiplying a simultaneous resource by execution time.
  Date/Author: 2026-08-06 / Codex.
- Decision: Prepare one endpoint-specific property-path expansion shape per `PathIteration`, bind each frontier value
  through a private generated variable, and reuse its `QueryEvaluationStep`.
  Rationale: The AST shape is invariant across expansions; only one endpoint value changes. Binding that value keeps
  SPARQL semantics while eliminating repeated telemetry-map copies and repeated strategy compilation measured by
  JFR. Preparation remains instance-local so concurrent path evaluations do not share mutable feedback iterators.
  Date/Author: 2026-08-06 / Codex.
- Decision: Give every prepared correlated filter region one mandatory, topology-derived complete seed before any
  region spends budget enumerating optional alternatives, and keep that seed in an isolated dense-state snapshot.
  Rationale: A bounded optimizer must first retain one executable complete plan per region. Optional subset
  exploration may improve it, but cannot be allowed to starve siblings or mutate the seed's proof lineage.
  Date/Author: 2026-08-06 / Codex.

## Outcomes & Retrospective

The shared contract, runtime counters, packed key masks, Frontier fan-out evidence, orientation scoring, and LMDB
memory objective are implemented. The property-path allocation hotspot and the bounded correlated-search starvation
exposed by the acceptance runs are also fixed without forcing hash selection. Focused contract, iterator, packed,
LMDB, search-budget, and AAS integration selections are green. Full module runs expose only pre-existing
dirty-worktree failures outside the task-specific suites. The final lower-is-better medians are Q1 `0.046 ms/op`
(non-hash, 34.286% below baseline), Q2 `107.299 ms/op` (31.223% below baseline), and Q3 `171.180 ms/op` (26.547%
below baseline). All queries complete, preserve results, and pass their acceptance thresholds.

## Context and Orientation

The runtime hash join lives in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/HashJoinIteration.java`.
`JoinQueryEvaluationStep` and `LeftJoinQueryEvaluationStep` choose it after the packed planner materializes a hash
hint. A SPARQL solution mapping is a `BindingSet`; two mappings are compatible when every variable bound on both
sides has the same value. A variable absent from either side does not conflict.

`StreamBindingSchema` describes which binding names an algebra expression can emit (`possible`) and which names are
present in every emitted row (`assured`). This distinction is the source of truth for runtime hash keys.

Packed planning lives under
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed`.
`PackedBindingFacts` represents possible and assured names as query-local integer mask IDs. `PackedJoinEnumerator`
compares dependent iteration and independent hash candidates. `PackedPhysicalJoinCosting` emits the physical cost
vector, and the LMDB session can refine it with database evidence before `LmdbPhysicalCostObjective` turns that
vector into a scalar score.

LMDB Frontier evidence lives in `LmdbFrontierPackedCostSession`. A `FrontierCorrelationDomain` contains row mass,
joint distinct-value estimates, and sometimes a primitive multi-column key relation. That relation can compute the
weighted overlap between build and probe keys: for each shared lookup key `k`, multiply its build multiplicity by
its probe multiplicity and sum the products. This is the number of candidate pairs retrieved from hash buckets
before compatibility variables that are not part of the lookup key are checked.

## Plan of Work

First add `HashJoinBindingContract` beside `StreamBindingSchema`. It is an immutable internal type with deterministic
`lookupBindings` and `compatibilityBindings` lists. Its algebra factory obtains both schemas, intersects assured and
possible sets respectively, filters through `BindingUniverse.plannerName`, and checks that lookup is a subset of
compatibility. Add focused schema-shape tests before wiring production call sites.

Next change `HashJoinIteration` to hold separate lookup and compatibility arrays. Build exact and partial indexes
only on lookup bindings. An empty lookup array maps every build row to one bucket. Every candidate is checked using
all compatibility bindings, except the existing safe fast path where lookup and compatibility are identical and
both rows have complete keys. Keep the old constructors as adapters. Track build rows, probe-input rows, candidate
rows, total hash-probe work, and peak live build rows, and publish them on close. Update algebra-aware join and left
join steps to pass the new contract.

Then add query-local lookup and compatibility masks to physical hash candidates. Compose prefix possible/assured
masks from packed relation masks and intersect them with the factor masks. Extend `PackedCostContext`, its reset and
copy paths, `PackedCostingTraceArena`, immutable `PackedCostingTrace`, and `PackedCostingReplay`; the two masks must
participate in invocation identity so two candidates with different key shapes cannot share a cached refinement.
Materialized plans publish the human-readable bindings and the physical cost components.

Change physical hash costing to score complete work. For each legal build orientation, calculate total build
insertions, probe lookups, partition-aware candidate pairs, result rows, and per-instance peak build rows. A missing
lookup mask uses the exact pair product. The LMDB refinement replaces keyed candidate pairs with weighted primitive
key overlap when both evidence states support it, otherwise with `B*P/max(buildNDV, probeNDV)`, otherwise with the
product. Clamp and saturate all results. Select the cheaper admissible orientation and publish its build side.
`plannedCostHashProbeRows` is probe lookups plus candidates.

Finally add one unit of `peakMemoryRows` to `LmdbPhysicalCostObjective`, keep the existing hard cap, increment
`LmdbPackedCostModel.VERSION` to 24, and add regression coverage. Run targeted tests before module suites. The final
performance check uses the repository benchmark wrapper and the supplied disabled-hash scores as the baseline.

If the benchmark gates expose a non-hash hotspot, follow the required JFR evidence instead of changing hash
selection. For the observed property-path hotspot, characterize clone frequency first, then prepare an
endpoint-specific expansion expression once per iterator. Every later expansion evaluates that prepared shape with
the current frontier endpoint supplied as an internal binding; the internal binding is removed before results leave
the iterator. Preserve the existing first-hop, zero-hop, fixed-endpoint, and reverse-traversal semantics.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

The initial build command, already completed successfully, is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

Use focused tests through the repository runner, without `-am` or Maven `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py HashJoinBindingContractTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py HashJoinIterationTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedPhysicalCostContractTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbFrontierPlanningIntegrationTest --retain-logs

After focused tests pass, run the evaluation and LMDB module suites with `mvnf`. Before finalizing, run the copyright
check and repository formatter, then repeat the root quick install.

Run each AAS target three times with matching JVM and dataset parameters:

    ./scripts/run-single-benchmark.sh --aas-run --aas-query query1PropertyProjection \
      --param useCascades=true --warmup-iterations 2 --measurement-iterations 2 --forks 1
    ./scripts/run-single-benchmark.sh --aas-run --aas-query query2ThresholdCount \
      --param useCascades=true --warmup-iterations 2 --measurement-iterations 2 --forks 1
    ./scripts/run-single-benchmark.sh --aas-run --aas-query query3LineAggregates \
      --param useCascades=true --warmup-iterations 2 --measurement-iterations 2 --forks 1

Compare saved result text with:

    python3 .codex/skills/jmh-benchmark-compare/scripts/jmh_benchmark_compare.py \
      <baseline.txt> <candidate.txt> --score-direction lower --export-formats md --output /tmp/hash-join-compare.md

## Validation and Acceptance

Contract tests must prove that constants never become lookup or compatibility bindings, assured shared variables do
become lookup bindings, nullable shared variables remain compatibility-only, and ordering is deterministic through
UNION, OPTIONAL, projection, extension, grouping, binding-set assignments, and nesting.

Iterator tests must show the same solution mappings as dependent iteration for complete keys, nullable conflicts,
unbound values, no key, legacy partial keys, both pinned build sides, and left-join null extension. Telemetry must
show one probe-input unit per probe row and one candidate unit per examined build row.

Packed and LMDB tests must observe product fallback for no key or missing evidence, exact weighted overlap for
skewed and multi-column evidence, NDV fallback, correct partition accounting, both build orientations, distinct trace
identity, unit memory scoring, and hard-cap rejection.

The AAS benchmark acceptance is lower-is-better average time. The median of three query-2 runs must be below
156.009 ms/op; query 3 must be below 233.048 ms/op. Query 1 must keep a non-hash plan and remain at or below
0.0735 ms/op. No run may time out, and all queries must return the same result as the disabled-hash baseline. If a
target misses, capture JFR with the benchmark wrapper and continue from measured CPU/allocation evidence.

## Idempotence and Recovery

Builds and tests can be rerun safely. Do not reset, restore, stash, delete, or overwrite the pre-existing dirty
worktree. If an edit conflicts with an existing user change, inspect the local diff and adapt the new code around it.
Keep untracked build, report, and evidence artifacts as required by repository policy.

## Artifacts and Notes

The initial root build is stored in `maven-build.log` and ended with:

    [INFO] BUILD SUCCESS
    [INFO] Total time: 41.374 s (Wall Clock)

Prior diagnosis artifacts remain available at `/tmp/aas-query2-hash-enabled.log`,
`/tmp/aas-query3-hash-enabled.log`, and `initial-evidence.hash-join-diagnosis.txt`.

The final regression and acceptance evidence is preserved in:

    initial-evidence.correlated-seed.txt
    logs/mvnf/20260806-160156-verify.log
    logs/mvnf/20260806-160240-verify.log
    logs/mvnf/20260806-160330-verify.log
    logs/mvnf/20260806-154852-verify.log
    profiles/lmdb/aas-hash-contract-seeded-v24-q{1,2,3}-trial{1,2,3}.txt
    profiles/lmdb/aas-hash-contract-seeded-v24-comparison.md
    maven-build.log

The final root quick install ended with `BUILD SUCCESS` after `34.089 s (Wall Clock)`. The evaluation module run
reported 1 known dirty-worktree failure among 1,207 tests, and the LMDB module run reported 11 known dirty-worktree
failures among 1,906 tests; every task-specific focused selection is green.

## Interfaces and Dependencies

No new dependency is required. Add one internal immutable `HashJoinBindingContract` type with algebra/schema factory
methods and read-only lookup/compatibility accessors. Add internal hash-mask fields and accessors to the experimental
`PackedCostContext`; they must be reset, copied, traced, fingerprinted, and replayed together. Existing
`HashJoinIteration` constructors remain callable and delegate to the legacy equal-list contract. No supported public
API or persisted LMDB schema changes.

Revision note (2026-08-06): Created the initial self-contained execution plan after the mandatory root build and
before production edits. It records the supplied performance gates and the pre-existing dirty-worktree constraint.

Revision note (2026-08-06 10:52Z): Recorded the completed shared-contract milestone and its focused green test run.

Revision note (2026-08-06 10:57Z): Recorded the runtime split, exact work-counter semantics, and focused iterator
verification.

Revision note (2026-08-06 11:11Z): Recorded packed mask propagation, trace identity/replay wiring, and focused
lifecycle verification.

Revision note (2026-08-06 15:36Z): Recorded completed costing/test milestones, benchmark/JFR findings, the rejected
hash-candidate diagnostic, and the measured property-path precompilation follow-up.

Revision note (2026-08-06 17:02Z): Closed the plan after the property-path improvement, mandatory correlated-region
seed scheduling, focused and module verification, nine passing final benchmark trials, formatting, and the green
root quick install.

Revision note (2026-08-06 17:15Z): Recorded the generated-workspace limitation in the repository copyright helper
and the successful scoped header/attribution audit.
