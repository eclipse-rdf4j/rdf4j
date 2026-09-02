# Restore ANALYTICS q8 and q10 with native IR kernels

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while work proceeds. Maintain this document according to
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The ANALYTICS Theme benchmark has two material performance regressions. Query 8 moved from 44.555 ms/op on
2026-08-30 to 408.151 ms/op on 2026-09-02 even though both runs used the same type-matrix traversal and reported
nearly the same logical work. Query 10 moved from 30.009 ms/op to 19,398.886 ms/op because the strategy arbiter
selected a wildcard intermediate-representation (IR) aggregate that scans 13.8 million statements instead of the
exact subject/object node-domain intersection. An IR is the small physical operator language in
`LmdbNativeKernelIr.java`; Janino turns an admitted IR shape into Java bytecode at runtime.

After this work, both queries must execute through first-class IR operators without losing their asymptotically
better algorithms. Query 8 must complete in at most 49.01 ms/op and query 10 in at most 33.01 ms/op on matched JDK
26 runs. Parallel IR must be available and its workers must demonstrably overlap, but the adaptive strategy arbiter
may choose serial IR when serial execution is faster.

## Progress

- [x] (2026-09-02 06:15Z) Inspected benchmark results, executed plans, current source routing, and the user-owned
  ANALYTICS-only benchmark diff.
- [x] (2026-09-02 06:15Z) Completed the mandatory root `-Pquick clean install`; all reactor modules succeeded.
- [x] (2026-09-02 06:22Z) Added and ran the smallest failing IR-route tests for q10 and q8; preserved both compact red
  reports in the existing `initial-evidence.txt` before touching production code.
- [x] (2026-09-02 08:04Z) Added the node-domain intersection runtime view, IR producer, interpreter/emitter support,
  physical-work costing, exact fallback, and query-scoped parallel overlap telemetry. Both q10 execution tiers and the
  seven-test shared parallel aggregate suite pass.
- [x] (2026-09-02 09:00Z) Moved both type-matrix shapes into a structural `TypeMatrixAggregate`, added direct
  generated/interpreted loops, distinct compiled/interpreted serial/parallel routes, worker-local predicate/root
  morsels, exact specialist fallback, and overlap telemetry. Both tiers pass the serial usage/linkage and forced
  parallel linkage selectors.
- [x] (2026-09-02 08:04Z) Added coordinated worker startup, initial disjoint ownership, and query-scoped partition,
  worker, overlap, rows, steals, and decline telemetry. The q10 test proves peak overlap and two non-zero workers.
- [x] (2026-09-02 10:10Z) Bisected q8 to `2d54a3e3d2`, removed repeated stable-source lookup from the adjacency hot
  path, specialized the type-matrix pair counts and predicate-plane pruning, and reran calibration. Normal dispatch
  now selects parallel type-matrix IR; the focused JFR run measured 47.709 ms/op with 15 overlapping workers.
- [ ] (in progress) Enable the outgoing node-predicate projection end to end, log its build progress, and audit runtime
  defaults for latency/throughput before returning to q10 binding.
- [ ] Finish q10 full-data binding, focused/parity verification, full LMDB, ANALYTICS benchmark, JFR, and JIT gates.

## Surprises & Discoveries

- Observation: q8 is not currently an IR kernel. `LmdbNativeAggregatePlanner.tryTypeMatrixStep` returns
  `LmdbNativeTypeMatrix` before `NativeGroupStep` can offer any serial or parallel IR candidate.
  Evidence: the 2026-08-30 and 2026-09-02 result files both report `nativeExecutionPath=typeMatrix`, 368 morsels, and
  about 4.04 million roots plus 7.82 million fibers, despite the 9.16x time regression.
- Observation: q10's compiled wildcard IR is algorithmically weaker than an already available exact set kernel.
  Evidence: the 2026-09-02 plan reports 13,808,610 candidate rows and `filter-not-forkable`; the older
  `LmdbNativeExistsIntersection.tryDomainSynopsisCount` calls
  `NodeDomainSynopsis.intersectionCardinality(NodeDomainPresence)` and completed near 30 ms.
- Observation: existing type-matrix parallel tests prove only that morsels were counted, not that workers overlapped.
  Evidence: `LmdbTypeMatrixTest.projectionFreeLinkageUsesRootOrdinalMorselsInParallel` checks only
  `PARALLEL_ADJACENCY_MORSELS`.
- Observation: on the small q10 regression dataset, both Janino arms already choose the row-enumerating wildcard IR over
  the planned exists-intersection candidate.
  Evidence: the captured red plans report `nativeExecutionPath=irAggregateWildcardInterpreted` and
  `nativeExecutionPath=irAggregateWildcard` while the physical plan still carries `existsIntersection`.
- Observation: the current dirty worktree contains one tracked user edit that comments out every benchmark theme except
  ANALYTICS. This edit must remain intact.
  Evidence: `git status --short --untracked-files=no` reports only `ThemeQueryBenchmark.java`.
- Observation: q8's only type-matrix implementation change between the supplied baseline and current revision is
  commit `2b8f1496c1` (`bug fixes`), which removed the cost-arbitrated LMDB fallback and made exact adjacency structural.
  Evidence: the two benchmark plans retain essentially identical work, making this the first-bad candidate to verify.
- Observation: q10 initially failed to bind through three independent capability gaps: the synthetic source wrapper did
  not forward intersection requests, synopsis/synopsis intersections were unsupported, and reversed orientation was
  rejected even though the set operation is symmetric. All three now bind without materializing IDs.
- Observation: deterministic overlap requires coordinated worker entry and initial ownership, not just a shared queue.
  Evidence: the forced q10 telemetry now reports multiple simultaneously active workers and multiple workers with
  positive disjoint work; the pre-change plan had only a parallel route label.
- Observation: q8 parallel IR initially declined with `task-budget` even though its kernel and result were correct.
  The generic row-materializing parallel aggregate was being offered for the same algebra and could reserve the two
  available tasks during adaptive probing. Suppressing that redundant candidate for the structurally owned matrix
  shape lets both the compiled and interpreted IR start two workers and report peak overlap of two.
- Observation: the repository runner's non-clean root install can leave package-private LMDB classes absent from
  `target/classes` after incremental recompilation, causing test compilation failures for unrelated tests. A clean
  quick install followed directly by manual focused verify restores the complete main output; no Surefire test ran in
  the failed runner attempt.
- Observation: q8's first bad commit is `2d54a3e3d2` (`faster adjacency building`), not the earlier source-only
  candidate. It added a stable-source lookup on every adjacency resolve; caching stable source IDs in the arena
  catalog removes that repeated lookup for every adjacency consumer.
- Observation: after the shared hot-path repair and structural-kernel pruning, fresh q8 calibration selects
  `irAggregateTypeMatrixParallel` normally. A short JFR run measured 47.709 +/- 3.849 ms/op and query telemetry reports
  15 workers started, peak active 15, 15 non-zero workers, 368 disjoint partitions, and work stealing.
- Observation: q10's structural IR exists but the full-data composite binding still declines. The dedicated
  `nodeDomainIntersection` view bypasses the optional retained-synopsis gate, but composite membership still asks the
  generic, default-off `nodeDomainPresence` API. The composite must use the same intersection view for membership.
- Observation: the node-predicate feature had three contradictory defaults: construction, runtime serving, and actual
  IR lowering were off, while the runtime registry advertised serving and lowering as on. Building the projection
  therefore did not imply that either normal traversal or generated kernels could use it.
- Observation: six other runtime-registry entries advertised default-on even though their actual execution gates and
  source comments deliberately keep them opt-in pending matched performance evidence: distinct numeric aggregate
  codegen, plan bridge, wildcard-predicate codegen, accumulate join, hash byte admission, and streaming WCOJ.

## Decision Log

- Decision: Use Routine D and this ExecPlan, while still creating a failing in-repository test before every
  behavior-changing production change.
  Rationale: this is a cross-cutting physical-plan, code-generation, concurrency, and performance refactor; the
  repository's proportional test-first rule still forbids production edits before a captured failing test.
  Date/Author: 2026-09-02 / Codex.
- Decision: Do not make `StatementPatternExistsFilter` merely forkable for q10.
  Rationale: parallelizing 13.8 million membership tests preserves the wrong asymptotic algorithm. The IR must express
  the compressed node-domain intersection itself.
  Date/Author: 2026-09-02 / Codex.
- Decision: Do not wrap `LmdbNativeTypeMatrix` in an opaque `KernelPlan` callback.
  Rationale: that would change telemetry without putting the hot traversal and aggregation loops under the IR emitter.
  The generated kernel must own the primitive loops and use engine views only as runtime data sources.
  Date/Author: 2026-09-02 / Codex.
- Decision: Keep runtime views outside canonical cache keys and retain exact interpreted/specialist fallbacks.
  Rationale: kernel classes are reusable by structural shape, while snapshot-owned cursors and run handles are neither
  reusable nor cross-thread safe.
  Date/Author: 2026-09-02 / Codex.
- Decision: Give type-matrix compiled/interpreted and serial/parallel executions distinct strategy tags, while using
  `nativeIrSelectedOperatorActual=TypeMatrixAggregate` as the operator identity shared across tiers.
  Rationale: this prevents four different cost distributions from contaminating one adaptive variant while keeping
  query-level execution evidence stable and directly comparable.
  Date/Author: 2026-09-02 / Codex.
- Decision: Enable only the outgoing node-predicate projection by default, and enable its serving and structural IR
  lowering defaults in the same change. Keep incoming projection opt-in.
  Rationale: outgoing enumeration is the compact subject-domain side and accelerates bound-subject wildcard-predicate
  queries; incoming includes the materially larger referenced-object/literal domain. A built sidecar that remains
  unreachable by both consumers pays startup and memory cost without query benefit.
  Date/Author: 2026-09-02 / Codex.
- Decision: Log node-predicate build start, exact sizing completion, and materialization completion at info level.
  Rationale: three bounded lifecycle records expose progress, memory demand, row/incidence size, and elapsed time
  without putting logging in the row merge loop.
  Date/Author: 2026-09-02 / Codex.
- Decision: Keep synchronous Janino compilation, retained general synopses, detailed semijoin probe tracking, OPTIONAL
  sweep, and mark join default-off; also report the six opt-in experimental tiers as off in the runtime registry.
  Rationale: the first three impose request-tail, memory/cache, or hot-path instrumentation cost; the latter operators
  explicitly lack matched latency/throughput acceptance in their own source contracts. Enabling them from a registry
  label would be speculative and makes operational state misleading.
  Date/Author: 2026-09-02 / Codex.
- Decision: Retain calibration, sample recording, bounded probes/hedges, native IR, factorization, and guarded parallel
  execution default-on. Leave hot counters on for this change but preserve the live opt-out.
  Rationale: q8/q10 demonstrate that bounded adaptive evidence prevents catastrophic static misrouting, while the
  engine's work thresholds and arbiter keep parallel startup away from small queries. Hot counters do add atomics in
  decode/lookup paths, but changing their observability contract requires a separate matched measurement and broader
  test audit; high-throughput deployments can already disable them live.
  Date/Author: 2026-09-02 / Codex.

## Outcomes & Retrospective

The q10 algorithmic regression is structurally repaired but full-data activation still needs its composite membership
binding: its exact subject/object set intersection is now structural IR, has both
interpreter and Janino emitter support, uses physical bitmap/sparse work for arbitration, retains the specialist as a
capability fallback, and can execute over disjoint worker-local partitions with measured overlap. q8 now likewise has
a structural terminal whose generated class owns the edge/type traversal and primitive accumulation loops, plus an
exact retained evaluator when a required view is unavailable. Focused compiled/interpreted serial and parallel q8
tests are green. q8 meets the target in the short JFR run and has proven parallel overlap; five-fork unprofiled
acceptance, q10 activation/timing, and broad verification remain pending.

## Context and Orientation

The work is in Maven module `core/sail/lmdb`. `LmdbNativeAggregatePlanner.java` converts RDF4J algebra into internal
slot plans and currently recognizes both the q10 exists-intersection and q8 type-matrix shapes.
`LmdbNativeGroupStep.java` offers physical candidates to `LmdbNativeStrategyArbiter`; this is where the slower q10
wildcard IR was allowed to outrank the exact intersection. `LmdbNativeKernelLowering.java` converts slot plans into
nodes from `LmdbNativeKernelIr.java`. `LmdbNativeKernelInterpreter.java` executes those nodes without generated code,
while the code under `evaluation/codegen` emits and compiles Java with Janino.

`NativeLmdbQuerySource.java` is the internal snapshot-view service-provider interface. Its `NativeProbe` can currently
borrow an exact all-predicate `NodeDomainSynopsis` or membership-only `NodeDomainPresence`. The concrete compressed
representations are `LmdbNodeDomainSynopsis.java` and `LmdbNodeDomainPresence.java`. q10 needs a snapshot-bound
intersection view constructed from those representations. For dense IDs it must count intersecting bitmap words with
`Long.bitCount`; for sparse IDs it must merge unsigned sorted primitive arrays.

`LmdbNativeTypeMatrix.java` is the current q8 specialist. Its fastest path splits the source rdf:type root domain into
bounded ordinal morsels, merge-probes those roots against accepted predicate planes, decodes target fibers, accumulates
primitive `(sourceType,targetType)` counts per worker, and merges those maps on the coordinator. This algorithm must be
represented by a structural IR aggregate rather than replaced by a generic three-pattern join.

Tests belong in `LmdbKernelExistsIntersectTest.java`, `evaluation/LmdbTypeMatrixTest.java`, the existing IR lowering,
interpreter, and emitter test classes, and a Theme regression integration test when the full ANALYTICS store is needed.
Benchmark history is under
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results`.

## Plan of Work

First change tests only. Update the q10 regression test so a full immutable-adjacency snapshot must report an IR
node-domain-intersection execution path for both Janino arms, while an overlay or unavailable synopsis still produces
the exact count through fallback. Add a type-matrix test requiring the linkage shape to report an IR type-matrix route.
Add an instrumented two-worker source whose workers block on a latch until both enter useful work, then assert exact
results, disjoint partitions, at least two workers with non-zero work, and peak overlap of two. Run the narrow selectors
and save their failure reports to top-level `initial-evidence.txt`.

For q10, add an internal `NodeDomainIntersection` view to `NativeLmdbQuerySource`. It exposes exact total work,
partition planning, exact `countPartition`, and a unique-root cursor for each disjoint partition. The concrete LMDB view
partitions by RDF ID type and then by dense bitmap word or sparse array range; it never allocates per root. Add
`NodeDomainIntersectionRequest` to `LmdbNativeKernelBindings` and a fluent binding on `KernelContext` so existing
constructors remain source-compatible. Add ROOT-grain `EnumerateNodeDomainIntersection` to the IR, including canonical
shape requirements. The interpreter and emitter both enumerate unique root IDs. When its only consumer is
`COUNT(DISTINCT)` on that produced column, fold the aggregate into exact partition counts and avoid a distinct table.
The parallel aggregate path gives one or more disjoint partitions to each worker and sums worker counts.

Teach lowering to recognize the existing q10 algebra shape and produce this IR node. Estimate it from bitmap words and
sparse entries. Do not offer the row-enumerating wildcard IR when the intersection binding succeeds. If the view cannot
cover the visible snapshot because of overlays, context restrictions, incomplete adjacency, or memory refusal, decline
before output and run the current `LmdbNativeExistsIntersection` path.

For q8, add a structural `TypeMatrixAggregate` IR node holding only roles and structural flags: source type predicate,
optional target type predicate, predicate-filter site, group-column order, and aggregate layout. Add runtime bindings
for the predicate catalog and the existing `NativeAdjacency`, `WildcardAdjacency`, `LabelSynopsis`, and root-domain
views needed by the traversal. Extract primitive windows, fiber batches, pair-count maps, and morsel claims from
`LmdbNativeTypeMatrix` into package-private helpers shared by the interpreter and emitter; no BindingSet or boxed map is
allowed in the hot loop. Generate conservative Java loops with small helpers so Janino and HotSpot do not receive one
oversized method. Keep `LmdbNativeTypeMatrix` as the exact fallback until IR parity and performance are proven.

Replace the planner's early q8 return with a `NativeGroupStep` carrying the type-matrix descriptor. The arbiter offers
compiled parallel, compiled serial, interpreted parallel, interpreted serial, and specialist fallback candidates. The
IR work estimate uses type-root count, accepted predicate-plane roots, and fiber upper bounds. Parallel workers open
worker-local probes and adjacencies, claim over-decomposed root morsels dynamically, keep local primitive counters, and
merge on the coordinator. A worker may not expose a run handle or mutable view to another thread, and an outer worker
must not start a nested worker group.

Add query-scoped metrics for IR operator name, planned partitions, workers started, peak active workers, completed work
per worker, steals, and decline reason. Metrics must be attached to the executed explanation rather than inferred from
global atomics. The deterministic test must prove overlap; the final JFR must also show CPU samples on at least two
kernel worker threads inside the same query interval.

Before tuning q8, benchmark and profile commit `35d085a074` and `fe4b945525` in isolated worktrees. If the same
algorithm is still 9x apart, bisect to the first commit over 150 ms/op. Profile that commit and its parent, then fix the
newly dominant shared hot path. Inspect bytecode/inlining only after JFR identifies the method. Do not change thread
counts or thresholds as a substitute for removing measured CPU, allocation, contention, or deoptimization.

## Concrete Steps

Run commands from the repository root. Tests use the repository runner, never Maven `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      LmdbKernelExistsIntersectTest#existsIntersectionAnswersTheDistinctNodeCount --retain-logs

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      LmdbTypeMatrixTest#projectionFreeLinkageUsesRootOrdinalMorselsInParallel --retain-logs

After the failing test run, preserve compact evidence before another run overwrites reports:

    python3 scripts/agent-evidence.py --command "<failed mvnf command>" \
      core/sail/lmdb/target/surefire-reports > initial-evidence.txt

After implementation, rerun the identical selectors, then the relevant classes, then the module:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbKernelExistsIntersectTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbTypeMatrixTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Use `scripts/run-single-benchmark.sh` for unprofiled headline timings. Use the benchmark class main for JFR after it
accepts focused arguments:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
      --module core/sail/lmdb \
      --main-class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark \
      --main-arg=--theme=ANALYTICS --main-arg=--query-index=8 \
      --jfr-output profiles/lmdb/analytics-q8.jfr

Repeat with query index 10. Keep profiled timings out of the headline comparison. Use the JMH comparison script on raw
baseline/candidate outputs and retain its Markdown/CSV reports under `profiles/lmdb/`.

## Validation and Acceptance

The first q10 and q8 route tests must fail before production edits and pass unchanged afterwards. Semantic tests must
cover Janino enabled/disabled, subject/subject and subject/object intersections, dense and sparse layouts, empty sets,
duplicate context multiplicity, reversed group keys, predicate filters, multiple types, overlays, incomplete/disabled
adjacency, cancellation, memory refusal, and one-worker fallback. Generic, interpreted IR, compiled IR, and retained
specialist answers must match.

The executed full-data q10 explanation must contain the node-domain-intersection IR path, report no per-statement
EXISTS filter tests, and omit `filter-not-forkable`. The executed q8 explanation must contain the type-matrix IR path.
A forced-parallel run must report at least two started workers, peak active workers at least two, at least two workers
with positive work, and exact disjoint-partition accounting.

Final unprofiled JDK 26 acceptance uses five forks with the supplied heap and synchronous-adjacency flags. At least four
fork means must be at or below 49.01 ms/op for q8 and 33.01 ms/op for q10; the fifth may not exceed 53.466 ms/op and
36.011 ms/op respectively. Run all thirteen ANALYTICS queries. Promote another query only when at least four of five
matched forks are at least 10 percent and 1 ms slower than the 2026-08-30 baseline.

## Idempotence and Recovery

All tests and benchmark captures are repeatable. Isolated worktrees protect the dirty primary checkout. Do not delete
or clean user artifacts; request permission before reclaiming benchmark stores or Maven workspaces. If an IR binding
fails after partial setup, close every opened view and decline before output. If a worker fails, cancel siblings, close
worker sources, release task/memory reservations, and rethrow without publishing partial aggregates.

If production code is edited before the failing route tests are observed, revert only the new task-owned production
hunks with `apply_patch`, preserve all user changes, and restart at the failing tests.

## Artifacts and Notes

The mandatory pre-change root build is in `maven-build.log`; it ended with `BUILD SUCCESS` after 36.074 seconds. The
2026-08-30 result file belongs to commit `35d085a074`; the current checkout is `fe4b945525`. Keep failing test evidence
in `initial-evidence.txt`, retained Maven logs under `logs/mvnf`, plan snapshots and profiles under `profiles/lmdb`, and
record exact paths here as they are produced.

## Interfaces and Dependencies

No supported public RDF4J API, configuration key, dependency, or persisted LMDB format changes. New types remain in
the experimental/internal LMDB evaluation API. `NativeLmdbQuerySource.NodeDomainIntersection` owns snapshot-valid
partition cursors; `LmdbNativeKernelBindings.NodeDomainIntersectionRequest` describes only subject/object directions;
`KernelContext` carries bound views; and IR nodes carry structural indexes, columns, and flags only. Existing Janino,
primitive collections, native adjacency views, the parallel task reservation system, and exact fallbacks are reused.

Revision note (2026-09-02): Created the initial executable plan after source/result triage and the mandatory successful
root build. The design deliberately preserves algorithmic specialists as fallbacks while moving their hot algorithms
into structural IR nodes.

Revision note (2026-09-02 06:22Z): Recorded the two intentionally failing route tests and their retained evidence. The
q10 red run also proved that the arbitration defect is reproducible on the small in-repository dataset, not only in the
13.8-million-row Theme benchmark.

Revision note (2026-09-02 08:04Z): Completed the q10 structural IR milestone and deterministic concurrency telemetry.
Focused evidence is retained in `logs/mvnf/20260902-080307-verify.log` and the shared parallel parity evidence in
`logs/mvnf/20260902-080412-verify.log`. Per user direction, subsequent q8 implementation edits will be batched before
the next Maven test cycle.

Revision note (2026-09-02 09:00Z): Completed the q8 structural IR milestone. Parallel compiled/interpreted evidence is
retained in `logs/mvnf/20260902-105930-type-matrix-parallel-verify.log`; serial usage/linkage evidence is in
`logs/mvnf/20260902-110005-type-matrix-serial-verify.log`. The next active milestone is matched q8 profiling and the
first-bad-commit repair.
