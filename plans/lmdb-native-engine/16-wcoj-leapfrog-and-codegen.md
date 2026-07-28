# Worst-Case-Optimal Join (Leapfrog Triejoin) and Core Codegen for the LMDB Native Engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (repository root).

## Purpose / Big Picture

SPARQL queries whose basic graph patterns form a *cycle* — e.g. a triangle `?a knows ?b . ?b knows ?c . ?c knows ?a` — are pathological for pairwise join engines: every binary join plan first enumerates a large intermediate (all 2-paths) and then filters it down. A *worst-case-optimal join* (WCOJ), of which *leapfrog triejoin* (LFTJ) is the classic implementation, instead intersects all patterns **one variable at a time**, never materializing an intermediate larger than the final result bound. PR https://github.com/eclipse-rdf4j/rdf4j/pull/5736 (branch `GH-5735-leapfrog-triejoin`, snapshot kept in the session scratchpad and reachable via `git show GH-5735-leapfrog-triejoin:<path>`) proved the concept for the LMDB store as a bolt-on evaluation strategy and reported, on cyclic FOAF-clique benchmarks, up to 15x over standard evaluation with runtime Janino codegen (`full_codegen` mode).

After this plan is implemented, the LMDB **native engine** on branch `optimize-lmdb` (the `LmdbNative*` classes under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`) will:

1. Detect the cyclic sub-part of any supported query during native compilation and execute exactly that sub-part with a leapfrog triejoin operator, while every other part of the query (filters, OPTIONAL, DISTINCT, ORDER BY, aggregation, non-cyclic patterns) continues to use the engine's existing operators. This is per-fragment, not all-or-nothing.
2. Beat the original PR's best mode (`full_codegen`) by **at least 2x on the same FOAF-clique benchmark suite, on the same machine** (both sides re-measured locally; the PR's committed numbers are only indicative).
3. Grow the engine's existing bytecode-generation facility (`LmdbNativeSpecialization`, JDK Class-File API + hidden classes) into a core, complete engine feature: in addition to the existing batch-filter kernels it will compile leapfrog kernels for WCOJ fragments *and* pipeline kernels for ordinary (non-WCOJ) join chains, so we can measure whether general queries benefit from codegen too (evaluated on the FOAF suite's decorated queries and the ANALYTICS theme).

Observable outcome: `FoafCliqueQueryBenchmark` (transplanted to this branch, rewritten to drive the native engine) reports, for every catalog query, at most half the ms/op of the PR branch's `full_codegen` mode measured on the same machine; `LmdbNativeLeapfrogJoin.PLANNED/OPENED` counters prove engagement; all existing lmdb tests plus the SPARQL compliance baseline stay green (24 known pre-existing failures, see `plans/lmdb-native-engine/COMPLIANCE-BASELINE.md`).

## Progress

- [x] (2026-07-21 23:20Z) Fetched PR branch `GH-5735-leapfrog-triejoin`, extracted changed-file snapshot to scratchpad, studied both codebases (reports: scratchpad `lftj-pr-report.md`, `native-engine-report.md`).
- [x] (2026-07-22 00:05Z) This ExecPlan written.
- [x] (2026-07-22 00:55Z) M0: local PR-branch baseline captured in `benchmark-results/lftj-pr-baseline.txt` via a git worktree at `../rdf4j-lftj-pr` (module built with `-P benchmarks,quick package`, then `java -jar core/sail/lmdb/target/jmh-benchmarks.jar -wi 5 -i 5 -f 1 -p benchmarkMode=full_codegen,disabled 'org.eclipse.rdf4j.sail.lmdb.benchmark.FoafCliqueQueryBenchmark\..*'`). Local full_codegen numbers match the PR's committed run almost exactly (cycle3 11.5 / cycle4 53.8 / cycle5 255.5 ms/op). 2x gate targets: cycle3 ≤5.8, cycle3CountCityInterest ≤17.5, cycle3DistinctCityOrdered ≤45.2, cycle3GroupedInterest ≤16.3, cycle4 ≤26.9, cycle4ValuesFilteredOrdered ≤73.8, cycle5 ≤127.8, cycle5ValuesCountMailboxHomepage ≤542.9, cycle5ValuesDistinctMailboxOrdered ≤224.4 ms/op. A first noisy run (concurrent test build) is kept as `lftj-pr-baseline-run1-noisy.txt`; ignore it.
- [x] (2026-07-22 00:30Z) M1: benchmark suite transplanted — `FoafCliqueDataGenerator/QueryCatalog/QueryCatalogTest` copied verbatim; `FoafCliqueQueryBenchmark` rewritten for engine modes native / native_no_wcoj / generic via system properties; `FoafCliqueCorrectnessTest` (single store, per-mode property toggling, SHA-256 multiset hashes) green 27/27 BEFORE the operator existed. The separate `native-pre-wcoj.txt` file is superseded: the `native_no_wcoj` benchmark mode provides the same baseline within one run.
- [x] (2026-07-22 01:15Z) M2: red tests captured — `LmdbNativeLeapfrogJoinTest` (evaluation package) failed exactly on the engagement counters with the rung unwired (`initial-evidence.txt`); correctness portions already green via existing operators.
- [x] (2026-07-22 01:20Z) M3: interpreted WCOJ green — `LmdbNativeLeapfrogJoin` (GYO cyclic-core recognizer + frontier-intersection leapfrog cursor + JoinCursor chain with earliest-cover filter placement) wired as the first rung of `LmdbNativeRowStep.openUnorderedInput` behind `rdf4j.lmdb.wcoj.enabled`, `PATH_WCOJ` telemetry tag added. Green: LmdbNativeLeapfrogJoinTest 5/5 (`post-evidence.wcoj-leapfrog-green.txt`), FoafCliqueCorrectnessTest 27/27, LmdbNativeDifferentialFuzzTest 19/19 plus a new `cyclicBasicGraphPatterns` round (7 fixed shapes + 60 randomized cyclic BGPs, multiplicity-sensitive). Module sweep running.
- [x] (2026-07-22 01:45Z) M3 follow-up: module sweep triaged — 13 failures, 12 pre-existing (confirmed by rerunning suspects with `-Drdf4j.lmdb.wcoj.enabled=false`, plus the memory-recorded stash-verified baseline), 1 was global-counter leakage into `LmdbNativeTupleMetricsTest` from the new DISTINCT fuzz queries, fixed with a `@BeforeEach` reset. Flag propagation into surefire forks verified by positive control (engagement test fails with the kill switch off).
- [x] (2026-07-22 02:10Z) M3 performance iteration: naive interpreted leapfrog was catastrophically slow (cycle3 5593 ms — every level entry rescanned frontiers whose resolved scan terms do not depend on the prefix). Execution-scoped frontier cache keyed by (pattern, resolved s/p/o/c ids, position, counts) + per-cursor `NativeProbe` reuse brought it to PR-full_codegen parity: cycle3 12.7, cycle4 52.3, cycle5 262.1, DistinctCityOrdered 60.7 (beats PR 90.3). Results in `benchmark-results/wcoj-interpreted-m3.txt` (naive) and `wcoj-cached-m3b.txt` (cached).
- [x] (2026-07-22 02:50Z) M4 first cut: `ParallelLeapfrog` — level-0 candidate array tiled into ranges, workers on snapshot-sibling sources bind candidates on their own `RowState` and run worker-local serial leapfrogs (nested parallelism forbidden), page-based handoff to the query thread. Green incl. two parallel-engagement tests and both parity suites with `parallelMinCandidates=1`. Benchmark (`wcoj-parallel-m4.txt`): cycle3 7.8 ms (beats PR 11.5, target 5.8 close), but cycle4/cycle5 unchanged (~50/252 ms) — **the big cycles are now bound by per-row result pumping, not by join work** — and cycle4ValuesFilteredOrdered regressed to 298 ms (parallel spawn per VALUES row recomputed all constant frontiers per worker). Fixed the latter by sharing the plan's now-concurrent frontier cache across workers and spawns. End-of-run detection latency fixed (latch-before-queue check instead of 50 ms poll tails).
- [x] (2026-07-22 03:40Z) M4 continued — three iterations, each verified by the parity suites and re-measured (`wcoj-filters-m4b.txt`, `wcoj-memo-m4c.txt`, `wcoj-stageorder-m4d.txt`):
  1. Filter pushdown: covered filters run inside the leapfrog at their earliest covering level (worker-forked via `NativeBooleanFilter.forkForParallelWorker`, serial when unforkable) — cycle4 50→20 ms.
  2. Cache restructure: the shared ConcurrentHashMap had grown one entry per prefix (contended inserts, GC churn — aggregate queries regressed); now the shared map holds only prefix-independent frontiers and each level keeps a last-key memo per member, exploiting the depth-first revisit pattern with O(1) memory — aggregate regressions gone.
  3. Stage placement by derived cost order: the leapfrog fuses at the position of the FIRST core member in `derivedPlan(mask).order`, so selective non-core children the compiler ranked earlier (VALUES, bound attribute probes) pre-bind core slots — cycle5ValuesDistinctMailboxOrdered 1349→228.6 ms, cycle4ValuesFilteredOrdered 121→38.6 ms.
  State vs 2x-of-PR targets: MET cycle4 (19.4 vs 26.9), cycle4ValuesFilteredOrdered (38.6 vs 73.8), cycle3DistinctCityOrdered (28.5 vs 45.2); at-threshold cycle5ValuesDistinctMailboxOrdered (228.6 vs 224.4); remaining: cycle3 (7.2 vs 5.8), cycle3CountCityInterest (34.8 vs 17.5), cycle3GroupedInterest (35.0 vs 16.3), cycle5 (270 vs 127.8), cycle5ValuesCountMailboxHomepage (973 vs 542.9).
- [x] (2026-07-22 04:40Z) M4 finale: JFR showed cycle5 samples entirely on the query thread — the parallel leapfrog never engaged because every multi-clause FILTER compiled to unforkable And/Or lambdas; fixed with `BooleanCombinationFilter` (fork/close/read-mask compose from children). cycle5 270→84.9 ms (3.0x vs PR). Decline-tracing flag added: `rdf4j.lmdb.wcoj.debugParallel`. A `plansCountingBranch`-based decline for counting queries was attempted and reverted (false-positives on cycle closing edges; would have killed the VDMO win).
- [x] (2026-07-22 05:00Z) M5 (first deliverable): specialization-kernel on/off comparison measured across the suite — codegen helps general query machinery 5–26% (`benchmark-results/pipeline-codegen-comparison.md`); recommendation: keep on by default; follow-ups: fused filter-chain kernels, ANALYTICS-scale confirmation. WCOJ-specific kernels deliberately deferred: the interpreter already beats the PR's full Janino codegen, so they must be justified by measurement.
- [x] (2026-07-22 05:10Z) Gate scoreboard written (`benchmark-results/wcoj-vs-pr.md`): 4 of 9 MET (cycle3DCO 3.2x, cycle4 2.7x, cycle4VFO 3.7x, cycle5 3.0x), cycle5VDMO 1.90x near-miss, and three understood miss groups with named levers (CSR frontiers for cycle3; group-table for the two aggregate-bound cycle3 variants; weighted counting for VCMH).
- [ ] M5 continued: CSR-served frontier accessor on `NativeLmdbQuerySource` (cycle3 lever); group-table presize/parallel aggregation (CCI/GI lever); weighted-multiplicity aggregation (VCMH lever); fused filter-chain kernels; ANALYTICS no-regression spot-check.
- [ ] M6: final gate re-run after the above; formatter/copyright done (2026-07-22); final module sweep + compliance baseline; remove PR worktree.
- [ ] M5: codegen — leapfrog kernels + general pipeline kernels; comparison doc for non-WCOJ queries.
- [ ] M6: 2x gate measured and documented; formatter/copyright/compliance sweep; remove PR worktree.

## Surprises & Discoveries

- Observation: The PR's `LmdbLftjTieredCodegenCompiler` is not tiered — it forwards straight to the full-stack compiler.
  Evidence: class body is a two-line delegation to `LmdbLftjFullCodegenCompiler.INSTANCE` (see scratchpad `lftj-pr-report.md` §3).
- Observation: The PR's frontier caches (`LmdbPrefixFrontierProvider`) are per-iteration, so every query execution re-scans LMDB to rebuild frontiers; and every leaf recomputes `countMatches` per pattern. Both are large avoidable costs the native engine's store-lifetime CSR cache does not pay.
- Observation: The native engine already contains a 2-way leapfrog (`LmdbNativeExistsIntersection.merge()`) and the seek primitives (`PatternCursor.seekForward`, `LmdbPrefixRunCursor.seekTo`) needed for a k-ary generalization.
- Observation: This machine reproduces the PR's committed benchmark numbers almost exactly (cycle5 255.5 vs 255.6 ms/op), so the committed results were evidently produced on comparable hardware and the local baseline is trustworthy.
- Observation: `statements(StatementOrder, ...)` on `NativeLmdbQuerySource` throws `UnsupportedOperationException` by default rather than returning null, so the M3 frontier scan uses unordered collect + unsigned sort (`x ^ Long.MIN_VALUE` trick) everywhere; ordered/CSR fast paths are deferred to the performance milestones. Unsigned order matters: inlined-double ids are negative as signed longs.
- Observation: The first "pre-WCOJ" benchmark attempt was invalidated twice — once by a concurrent test build perturbing JMH (kept as `lftj-pr-baseline-run1-noisy.txt`), once because the benchmark's build raced my source edits. Lesson: never run builds or tests while a measurement is in flight, and prefer flag-based baselines (`native_no_wcoj` mode) over source-state baselines.

## Decision Log

- Decision: Do not transplant any PR main-source class. Re-implement WCOJ natively in the `evaluation/` package; transplant only benchmarks and tests (rewritten to the native surface).
  Rationale: The PR duplicates an entire parallel evaluation strategy, optimizer pipeline, txn plumbing and a Janino dependency; the native engine already has superior equivalents (long-id row model, CSR cache, parallel substrate, Class-File-API codegen). The user explicitly asked for a rewrite that fits the native engine.
  Date/Author: 2026-07-22 / Claude (Fable) with hmottestad.
- Decision: No new third-party dependency (no Janino). Codegen uses the JDK Class-File API + `defineHiddenClass`, extending `LmdbNativeSpecialization`'s existing pattern.
  Rationale: House rule (no new deps without approval); the engine already generates bytecode this way; hidden classes unload with the store and JIT-compile as well as normal classes.
  Date/Author: 2026-07-22 / Claude.
- Decision: WCOJ recognition happens on the native physical plan (`MultiJoinPlan`'s flattened inner-join bag) rather than on the TupleExpr algebra.
  Rationale: `MultiJoinPlan` already flattens the join bag and carries filters with masks; recognizing there automatically inherits every entry path (row root, bare fragments, aggregation inputs) and keeps non-cyclic children untouched — this is what "WCOJ on the part of the query that requires it" means operationally. Precedent: `tryTypeMatrixStep` and `FactorizedTail` decisions live at the same level.
  Date/Author: 2026-07-22 / Claude.
- Decision: Trie access prefers per-predicate CSR adjacency arrays (`LmdbCsrAdjacencyCache`) with galloping array intersection; falls back to streaming LMDB cursors via `PatternCursor.seekForward`; declines (leaves the plan unchanged) when neither side of a pattern can serve sorted seeks.
  Rationale: FOAF-clique cycles are single-predicate; CSR turns leapfrog into in-memory sorted-array intersection, which is the main engine of the 2x win. Streaming fallback keeps generality; declining keeps correctness.
  Date/Author: 2026-07-22 / Claude.
- Decision: Feature flags follow native-engine convention (system properties): `rdf4j.lmdb.wcoj.enabled` (default true), `rdf4j.lmdb.wcoj.codegen.enabled` (default true), `rdf4j.lmdb.pipelineCodegen.enabled` (default true, general-query kernels), plus thresholds mirroring `nativeSpecialization.*`. No `LmdbStoreConfig` schema additions.
  Rationale: Store-config booleans (the PR's approach) change the public config schema; the native engine consistently uses JVM properties for engine internals.
  Date/Author: 2026-07-22 / Claude.
- Decision: The 2x gate compares this branch against the PR branch **on this machine** using the identical JMH parameters (5000 people, 30% clique, 3..8, 15000 random edges, seed 12345), PR side run in `full_codegen` mode.
  Rationale: The committed results file was produced on unknown hardware; cross-machine comparison would be meaningless.
  Date/Author: 2026-07-22 / Claude.

- Decision: `And`/`Or` compiled boolean filters became a named `BooleanCombinationFilter` class (in `LmdbNativeFilters.java`) instead of bare lambdas.
  Rationale: the lambdas defaulted `forkForParallelWorker()` to null, so ANY conjunction of forkable comparisons was unforkable — silently disabling both the parallel leapfrog and (in principle) parallel pipelines whenever a query had a multi-clause FILTER. The class composes fork/close/read-mask from its children. Found via `-Drdf4j.lmdb.wcoj.debugParallel=true` decline tracing on cycle5.
  Date/Author: 2026-07-22 / Claude.
- Decision: WCOJ does NOT decline on `LmdbNativeFactorizedRows.plansCountingBranch` (attempted and reverted).
  Rationale: that probe reports factorized-split counting/existence branches, which a cycle's own closing edge also produces — the heuristic disabled WCOJ for VALUES-decorated cycles and would have killed the cycle5VDMO win. cycle5ValuesCountMailboxHomepage (COUNT(*) with a factorizable attribute tail) therefore remains slower under WCOJ than the factorized counting path; the proper fix is weighted-row emission (multiplicity-aware aggregation) or making the leapfrog visible to the factorized split — future work, see Outcomes.
  Date/Author: 2026-07-22 / Claude.

## Outcomes & Retrospective

(Interim, M4 complete.) The WCOJ operator with an interpreted frontier-intersection cursor plus parallel execution beats the PR's best Janino full-codegen mode substantially on most of the catalog while remaining a strictly scoped rung of the native ladder (kill switch, per-fragment, cost-ordered stage placement). Perf findings worth keeping: (a) frontier caching strategy is everything — constant-key frontiers in a small shared map, prefix-dependent frontiers in per-level last-key memos; a general map keyed by prefix explodes; (b) the parallel win only materialized after pushed-down filters became forkable; (c) evidence so far suggests a well-shaped interpreter over sorted arrays matches or beats source-generated code (we outperform the PR's full_codegen without any WCOJ codegen), so M5's codegen case must be proven by measurement, not assumed. Remaining for the 2x gate: cycle3 (1.4x), cycle3CountCityInterest and cycle3GroupedInterest (aggregation-bound: GROUP-BY table resize dominates, not the join), cycle5ValuesCountMailboxHomepage (needs weighted counting), cycle5VDMO at 1.89x. Planned levers: CSR-served frontiers (new `NativeLmdbQuerySource` accessor returning the adjacency cache's sorted arrays directly), group-table presizing/parallel aggregation, weighted-multiplicity rows for counting aggregates, leapfrog codegen kernels if measurement supports them.

## Context and Orientation

Everything below lives in `core/sail/lmdb` unless stated. Paths are repository-relative.

The **native engine** is the set of `LmdbNative*` classes in `src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`. A SPARQL query reaches it through `LmdbNativeEvaluationStrategy.precompile(TupleExpr, QueryEvaluationContext)`, which asks `LmdbNativeAggregateCompiler.tryCompile(...)` to translate the algebra into a native plan; on failure it falls back (for that node) to the generic RDF4J evaluator. Native plans are trees of **`SlotPlan`** operators (interface in `evaluation/LmdbNativeSlotPlan.java`): `RowCursor open(RowState)` for row-at-a-time pulls, optional `BatchCursor openBatch(RowState,int)` for columnar batches (`NativeBatch`: column-major `long[]` + selection vector), `long producedMask()`, `double estimate(RowState)`. A **`RowState`** is a `long[] slots` of dictionary ids plus a bound-mask and a rollback trail; ids are only decoded to RDF `Value`s at projection (`NativeLmdbQuerySource.lazyValue`). Id comparisons must use `Long.compareUnsigned` (inlined doubles are negative as signed longs).

Inner-join bags are flattened into **`MultiJoinPlan`** (in `evaluation/LmdbNativeJoinPlans.java`), which holds child `SlotPlan`s plus masked filters, derives an execution order (`derive`/`derivedPlan`, greedy order from `LmdbNativeSlotOrder`), and offers nested-loop, merge-join (`LmdbNativeMergeJoin`), and hash-join (`LmdbNativeHashJoin`) strategies arbitrated by cost proposals (`LmdbNativeStrategyProposal`). The runtime "ladder" that picks between prefix-run cursors, ordered-distinct, batch, parallel, factorized and nested-loop execution is `LmdbNativeRowStep.NativeRowsIteration.initialize()` / `openBatchOrParallel()`; every rung records an execution-path tag via `LmdbNativeAttemptMetrics`.

Storage: `TripleStore` keeps composite-key quad indexes (default `spoc,posc`; benchmarks/tests often configure `spoc,ospc,psoc,posc` via `LmdbStoreConfig("spoc,ospc,psoc,posc")`). Sorted streaming with in-order skip is available through `RecordIterator.seekForward(...)` (overridden by range iterators and `LmdbCsrScanIterator`; default returns false = unsupported) and, at the pattern level, `PatternCursor.seekForward(...)` in `evaluation/LmdbNativePatternTerms.java` (declines for multi-context concatenations and synthetic stubs; open fields must be passed as `0`). Distinct-value skip-scan cursors exist as `LmdbPrefixRunCursor.seekTo/stopBefore`. The **CSR cache** (`LmdbCsrAdjacencyCache`) materializes, per predicate, sorted adjacency (`runStart[]` prefix sums + `neighbors[]`) in both `BY_SUBJECT` and `BY_OBJECT` orientations with min/max zone data — an in-memory sorted-array view ideal for leapfrog intersection.

Parallelism: `LmdbNativeParallelPipelines` owns the shared pool and admission (`tryReserveTasks`); `NativeLmdbQuerySource.openParallelSources(n)` yields per-worker snapshot sources; `TripleStore.planBalancedSplitKeys` produces interpolated range boundaries; `LmdbNativeParallelPrefixRuns` and `LmdbNativeExistsIntersection.tryParallelCount` are existing consumers of this substrate and the pattern to copy (workers pull range ids from a queue, `seekTo(lo)`/`stopBefore(hi)`, merge in range order on the query thread).

Codegen today: `evaluation/LmdbNativeSpecialization.java` builds bytecode with the JDK Class-File API (`java.lang.classfile.ClassFile`), loads it with `MethodHandles.lookup().defineHiddenClass(bytes, true, NESTMATE)`, caches per store id-space in a `WeakHashMap` (LRU by entries and generated-byte budget), compiles asynchronously on a daemon thread after an interpreter-mode row threshold (`rdf4j.lmdb.nativeSpecialization.thresholdRows`, default 32768), and currently emits one kernel family: `NativeBatchFilterKernel` (unrolled batch→row slot copy + filter + selection compaction).

The **PR under transplant** (branch `GH-5735-leapfrog-triejoin`; study report in session scratchpad `lftj-pr-report.md`): a separate `LmdbLftjEvaluationStrategy` + optimizer pipeline fuses each maximal *cyclic* BGP (≥3 statement patterns, all six quad indexes required, no repeated variable within a pattern, no named-context scope) into an `LmdbLftjTupleExpr` leaf carrying a variable order (exhaustive permutation ≤8 vars scored by index compatibility, else greedy) and per-pattern index choices; non-pattern siblings and residual filters stay outside, so it is fragment-scoped too. Execution walks variables depth-first, intersecting per-pattern sorted frontiers (materialized `long[]` per prefix via `LmdbPrefixFrontierProvider`, cached only per iteration) with binary-search seeks; at a leaf it multiplies witness counts (`countMatches` per pattern) to honor hidden-component multiplicity. Codegen tiers: `executor_codegen` (Janino-compiled unrolled control flow over the same cursors) and `full_codegen` (Janino-compiled, inlines raw LMDB cursor stepping and varint decode; fastest). Var-`!=`-var filters become native inequality constraints. External bindings become fixed prefix slots; unknown ids fall back to standard evaluation. Benchmarks: `benchmark/FoafCliqueDataGenerator` (synthetic FOAF graph: 5000 people, 30% in cliques of 3..8, 15000 random `foaf:knows` edges, seed 12345, plus city/interest/mailbox/homepage attributes), `FoafCliqueQueryCatalog` (cycle3/4/5 pure cycles + six decorated variants adding COUNT/GROUP BY/DISTINCT/ORDER BY/VALUES/FILTER), `FoafCliqueQueryBenchmark` (JMH, modes interpreted / executor_codegen / full_codegen / disabled), `FoafCliqueLftjCorrectnessTest` (parity vs standard eval). PR-machine best (`full_codegen`, ms/op): cycle3 12.1, cycle3CountCityInterest 36.1, cycle3DistinctCityOrdered 90.6, cycle3GroupedInterest 33.4, cycle4 54.2, cycle4ValuesFilteredOrdered 145.4, cycle5 255.6, cycle5ValuesCountMailboxHomepage 1088.9, cycle5ValuesDistinctMailboxOrdered 437.2. The 2x gate uses locally re-measured equivalents of these.

Why the native rewrite should be ≥2x faster than the PR's best: (a) store-lifetime CSR adjacency replaces per-iteration frontier materialization and per-leaf `countMatches` scans; (b) the engine's long-id row model avoids per-row `MutableBindingSet` materialization until projection; (c) the parallel substrate executes the first search variable's domain across workers (the PR is single-threaded); (d) decorated queries keep DISTINCT/ORDER/GROUP inside the native engine (batch/factorized/prefix-run operators) instead of generic RDF4J iterators above a fused leaf; (e) hidden-class kernels JIT at least as well as Janino output without source-string compilation latency.

Out of scope (do not modify): `LmdbSketchJoinOptimizer`, `LmdbEvaluationStatistics`, `ParetoJoinMemoPlanner` (owned by other branch work); the standard RDF4J evaluator; store write paths.

## Plan of Work

Work proceeds benchmarks → tests → code, per milestone below. New main-source classes go in `src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`; new tests in `src/test/java/org/eclipse/rdf4j/sail/lmdb/` (benchmark classes under `.../lmdb/benchmark/`). Every new Java file carries the standard 2026 copyright header. All Maven commands use `-Dmaven.repo.local=.m2_repo`; tests run through `python3 .codex/skills/mvnf/scripts/mvnf.py` (never `-am`, never `-q`).

### Milestone 0 — Local PR baseline (benchmarks first, part 1)

Goal: fair, local numbers for the 2x gate before any transplant. Check out the PR branch in a **separate worktree** so `optimize-lmdb` stays untouched: `git worktree add ../rdf4j-lftj-pr GH-5735-leapfrog-triejoin` (remove with `git worktree remove` at the end of M6). In that worktree run the root quick install and then the JMH benchmark `FoafCliqueQueryBenchmark` for modes `full_codegen` and `disabled` with the committed default parameters. Save the JMH text output to `benchmark-results/lftj-pr-baseline.txt` on THIS branch (create the directory; it is untracked-artifact-safe). Acceptance: the file contains one avgt score per catalog query for both modes. These numbers define the gate: target(query) = full_codegen_local(query) / 2.

### Milestone 1 — Transplant the benchmark suite (benchmarks first, part 2)

Goal: the same data generator and query catalog run as a JMH benchmark against the native engine on this branch. Copy `FoafCliqueDataGenerator.java`, `FoafCliqueQueryCatalog.java`, `FoafCliqueQueryCatalogTest.java` from the PR snapshot into `src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/` mostly verbatim (keep the exact same RDF schema, parameters and seed so results are comparable). Rewrite `FoafCliqueQueryBenchmark.java`: delete `LmdbLftjBenchmarkMode`/`LmdbBenchmarkStore` usage; the store is an `LmdbStore` with `LmdbStoreConfig("spoc,ospc,psoc,posc")`; the JMH param becomes `engineMode ∈ {native, native_no_wcoj, generic}` implemented by setting the system properties `rdf4j.lmdb.nativeQueryEngine.enabled` and `rdf4j.lmdb.wcoj.enabled` in `@Setup` (mirroring how `LmdbNativeDifferentialFuzzTest` toggles the engine). Also port `FoafCliqueLftjCorrectnessTest` as `FoafCliqueCorrectnessTest`: for every catalog query, assert native results equal generic-evaluator results (multiset; ordered where ORDER BY). Run it; it must be green *before* WCOJ exists (the native engine must already answer these queries correctly today — this is the pre-change parity anchor). Then run the benchmark in `native` mode (current engine, no WCOJ) and save `benchmark-results/native-pre-wcoj.txt`. Acceptance: catalog test + correctness test green; two baseline files exist.

### Milestone 2 — Red tests (tests before code)

Goal: smallest failing tests that pin the new behavior. Add `LmdbNativeLeapfrogJoinTest` (unit-level, in `src/test/java/.../lmdb/`) asserting: (a) for the triangle query on a tiny handcrafted dataset the native engine produces correct rows AND `LmdbNativeLeapfrogJoin.PLANNED.get() > 0` / `OPENED.get() > 0` (static `AtomicLong` engagement counters, the established pattern from `LmdbNativeExistsIntersection`); (b) for an acyclic star query the counters do NOT move; (c) parity under external bindings (join variable pre-bound by a preceding VALUES). Add cyclic-query cases (triangle, 4-cycle, diamond-with-chord, cycle-plus-tail, cycle-under-OPTIONAL, cycle-with-filter) to `LmdbNativeDifferentialFuzzTest`'s query list so the fuzz harness covers WCOJ paths from now on. Capture the failing run (`mvnf LmdbNativeLeapfrogJoinTest`) — counters at zero — as the Routine-A red evidence. Acceptance: new test compiles and fails only on engagement/counters, not on correctness (correctness must already pass via existing operators).

### Milestone 3 — Interpreted WCOJ operator

Goal: the operator, engaged and correct, no codegen yet. New classes (all in `evaluation/`):

- `LmdbNativeLeapfrogJoin` — recognizer + `SlotPlan`. Static `tryPlan(MultiJoinPlan-children, filters, boundMask, source)` runs during `MultiJoinPlan.derive`-time planning (hook: where `derivedPlan` finalizes child order in `LmdbNativeJoinPlans.java`, alongside the existing strategy proposals in `LmdbNativeRowStep.openBatchOrParallel`): build the variable/pattern hypergraph of the *unbound* slots of the flattened `PatternPlan` children (only plain `PatternPlan` leaves participate; children of other types stay outside); find the maximal sub-bag (≥3 patterns) whose slot graph contains a cycle after contracting bound slots; if found, and every participating pattern can serve sorted seeks for the chosen variable order (CSR or seekable index — checked via a new capability probe, below), replace those children with one `LeapfrogPlan` node and keep everything else in the `MultiJoinPlan` unchanged. Filters whose mask lies fully inside the leapfrog's produced slots and that are var-!=-var / var-!=-const comparisons are absorbed as inline constraints; other filters stay masked filters in `MultiJoinPlan` (existing earliest-legal-depth placement). Engagement gated by `rdf4j.lmdb.wcoj.enabled` and a cost sanity check (decline when the cycle's minimum pattern estimate is tiny, where binary join wins — threshold `rdf4j.lmdb.wcoj.minEstimate`, default 0 initially, tuned in M6).
- Variable order: greedy — start from slots bound by the enclosing prefix (RowState boundMask), then repeatedly pick the unbound slot with the highest connectivity (appears in most patterns), tie-broken by the smallest sum of per-pattern frontier estimates (CSR run lengths / `estimateForBoundMask`). Exhaustive permutation only for ≤6 unbound slots (cheap, uses real CSR cardinalities, unlike the PR's structural scoring).
- `LeapfrogCursor` (RowCursor) — k-ary generalization of `LmdbNativeExistsIntersection.merge()`: per depth, an array of *level iterators*, one per pattern containing that slot; `leapfrog_search` = max-of-current-keys, seek laggards (`Long.compareUnsigned`), emit on agreement; descend/backtrack via RowState `mark()`/`rollback()`. Level iterator implementations: `CsrLevelIterator` (galloping over `LmdbCsrAdjacencyCache` neighbor arrays, both orientations) and `ScanLevelIterator` (streaming `PatternCursor.seekForward`, open fields as 0). Multiplicity: only patterns with hidden components (context wildcards over multi-context data) need witness counting; detect at plan time and use CSR run lengths / bounded probe counts only for those patterns — pure-visible patterns emit multiplicity 1 with no counting scan (fixes the PR's per-leaf `countMatches` overhead).
- Capability probe: small additions in `evaluation/` only — a static helper asking whether pattern×(variable position) can produce sorted distinct values under a given bound-prefix (CSR entry exists, or a configured index serves the needed field order and `PatternCursor.seekForward` applies). If any level of the chosen order fails, try the alternate order; if none works, decline (recognizer returns null, plan unchanged). No `TripleStore` changes expected; if one proves necessary, keep it additive and record it in the Decision Log.
- Telemetry: `PATH_WCOJ` tag in `LmdbNativeAttemptMetrics`, decline reasons recorded like other rungs, static counters PLANNED/OPENED/DECLINED_* for tests.

Acceptance: M2 tests green (counters move; parity holds); differential fuzz green; module test suite green (`mvnf core/sail/lmdb`, judged by report XMLs per branch convention); benchmark in `native` mode already faster than `native_no_wcoj` on cycle4/cycle5.

### Milestone 4 — Parallel WCOJ

Goal: multi-core leapfrog. Partition the FIRST search variable's domain into ranges using the existing substrate (`openParallelSources(n)`, `planBalancedSplitKeys` on the driving pattern's index, or CSR key-array slicing when CSR-backed — the simplest correct tiling: split the first level's candidate array into `workers × 4` chunks); admission mirrors `LmdbNativeParallelPrefixRuns` (estimate ≥ `rdf4j.lmdb.wcoj.parallelMinEstimate`, default 1e6, `configuredThreads() ≥ 2`, task reservation); each worker runs an independent `LeapfrogCursor` over its range against its snapshot source; results merge on the query thread in range order (order-preserving, so downstream ordered operators stay correct). Unordered consumers may stream via the existing parallel queue pattern. Acceptance: a test with the threshold property forced to 0 asserts a parallel-runs counter > 0 and parity; benchmark shows cycle5 scaling with cores.

### Milestone 5 — Codegen as a core engine feature

Goal: two new kernel families in `LmdbNativeSpecialization` (or a sibling `LmdbNativeKernels` sharing its cache/thread/budget machinery — decide during implementation and record):

- **Leapfrog kernels** (`rdf4j.lmdb.wcoj.codegen.enabled`): specialize `LeapfrogCursor` per execution shape (unbound-slot count, patterns-per-level fan-out, CSR vs scan level kinds, constraint set) — unrolled per-level intersection loops with monomorphic call sites reading CSR arrays directly. Key by a canonical shape string (slot count + per-level kinds + constraint kinds), NOT by constant ids, so kernels are reusable across queries; compile async after the interpreted cursor crosses the existing row threshold; interpreted path remains the always-available fallback (compile failure must never fail a query).
- **Pipeline kernels for general queries** (`rdf4j.lmdb.pipelineCodegen.enabled`): compile the hot inner loop of a derived `MultiJoinPlan` probe chain — the sequence "for each row of child 0, probe child 1..k via NativeProbe/hash table, apply masked filters at their depths" — into one generated method, eliminating per-operator virtual dispatch. Start with the two most common shapes measured on the FOAF decorated queries and ANALYTICS theme (chain of index-probes with filters; hash-probe chains); keep scope honest — this milestone is the *experiment* the user asked for ("test out codegen for other queries"), so its deliverable is: kernels implemented for the selected shapes + a written comparison (`benchmark-results/pipeline-codegen-comparison.md`) of native-with vs native-without `pipelineCodegen` on FOAF decorated queries and at least 4 ANALYTICS queries, with a recommendation (keep on by default / gate higher / revert) recorded in the Decision Log.

Acceptance: codegen unit tests (kernel == interpreted results on randomized data, cache eviction, async-compile switchover), fuzz green with kernels forced on (threshold 0), comparison document written, no ANALYTICS regression >5% (guard: `analytics` theme spot-checks via ThemeQueryBenchmark on the 5 fastest-to-run queries).

### Milestone 6 — The 2x gate and final sweep

Goal: prove the target. Re-run the transplanted benchmark (`native` mode, WCOJ + codegen on) with the same JMH settings as M0; produce `benchmark-results/wcoj-final.txt` and a comparison table (use the `jmh-benchmark-compare` skill) `benchmark-results/wcoj-vs-pr.md` with columns: query, PR-local full_codegen, native final, speedup. Gate: speedup ≥ 2.0 for every catalog query; where a decorated query is dominated by non-join work (e.g. ORDER BY on large results) and misses 2x, either optimize the dominating native operator or, if genuinely join-independent, document the exception explicitly with JFR evidence (`docker-jfr-benchmark-loop` skill) — do not silently accept it. Then: formatter (`mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` — allowed here, not a test run), copyright check (`cd scripts && ./checkCopyrightPresent.sh`), full `mvnf core/sail/lmdb` sweep judged by report XMLs, compliance baseline re-check against `plans/lmdb-native-engine/COMPLIANCE-BASELINE.md` (no NEW failures beyond the 24 known), update this plan's Progress/Outcomes, remove the PR worktree.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j` unless noted.

Root install (before any test work, and after cross-module changes):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{s=1} s{print}'

M0 baseline (in the PR worktree `../rdf4j-lftj-pr`, same install command first):

    mvn -o -Dmaven.repo.local=../rdf4j/.m2_repo -pl core/sail/lmdb -Pbenchmarks ... (exact JMH invocation to be pinned when M0 starts; use scripts/run-single-benchmark.sh if compatible, else the module's JMH main; record the exact command here once run)

Tests, always via mvnf, one at a time (pipefail when piping):

    python3 .codex/skills/mvnf/scripts/mvnf.py FoafCliqueQueryCatalogTest
    python3 .codex/skills/mvnf/scripts/mvnf.py FoafCliqueCorrectnessTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeapfrogJoinTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Expected transcript shape for the M2 red run: `LmdbNativeLeapfrogJoinTest` fails with an assertion like `expected PLANNED > 0 but was 0` in `core/sail/lmdb/target/surefire-reports/`.

## Validation and Acceptance

- Correctness: `FoafCliqueCorrectnessTest` (native == generic for every catalog query), `LmdbNativeDifferentialFuzzTest` including the new cyclic cases, `LmdbNativeLeapfrogJoinTest` engagement + bound-input parity, snapshot-isolation coverage via a cyclic-query case added to the existing native isolation test (native WCOJ must read a consistent snapshot while a writer commits — port the *scenario* of the PR's `LmdbLftjSnapshotIsolationTest`, not its code).
- Engagement: static counters + `PATH_WCOJ` execution-path tag observable via `LmdbNativeExplain`.
- Performance: the M6 gate table; every catalog query ≥2x vs local PR full_codegen, and `native` ≥ `native_no_wcoj` everywhere (no harm from the recognizer on non-cyclic queries, pinned by the decorated-query rows).
- Regression: module suite green (report XMLs), compliance baseline unchanged, ANALYTICS spot-checks within 5%.

## Idempotence and Recovery

Every milestone is additive and flag-guarded; `rdf4j.lmdb.wcoj.enabled=false` restores pre-plan behavior at any point, `rdf4j.lmdb.pipelineCodegen.enabled=false` isolates the general-codegen experiment. Benchmark transplants are test-scope only. The PR worktree is disposable (`git worktree remove ../rdf4j-lftj-pr`). If a milestone's tests cannot be made green, stop, record the state in Progress, and leave the flag default-off for the offending feature rather than reverting unrelated work. Keep all untracked artifacts (house rule): baseline files under `benchmark-results/` must not be cleaned.

## Artifacts and Notes

Study reports (session scratchpad, regenerate by re-running the exploration against `GH-5735-leapfrog-triejoin` and this branch if lost): `lftj-pr-report.md`, `native-engine-report.md`. Baselines & gate outputs land in `benchmark-results/` (repo root): `lftj-pr-baseline.txt`, `native-pre-wcoj.txt`, `wcoj-final.txt`, `wcoj-vs-pr.md`, `pipeline-codegen-comparison.md`.

## Interfaces and Dependencies

No new external dependencies. End-state (names may gain detail during implementation; keep this section current):

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeLeapfrogJoin.java`:

    final class LmdbNativeLeapfrogJoin implements SlotPlan {
        static SlotPlan tryPlan(List<SlotPlan> children, MaskedFilter[] filters,
                                long boundMask, NativeLmdbQuerySource source);   // null = decline
        public RowCursor open(RowState row);
        public BatchCursor openBatch(RowState row, int capacity);               // M4+, optional
        public long producedMask();
        public double estimate(RowState row);
        static final AtomicLong PLANNED, OPENED, PARALLEL_RUNS;                  // test hooks
    }

    interface LevelIterator {                    // one pattern × one depth
        boolean open(RowState prefix);           // position at first key ≥ 0
        boolean seek(long key);                  // leapfrog seek, unsigned; false = exhausted
        boolean next();
        long key();
        long multiplicity();                     // 1 unless hidden components
        void close();
    }

Kernels (M5) implement the same `RowCursor`/`LevelIterator` contracts; generated classes are hidden classes cached per store id-space keyed by canonical shape strings. Existing interfaces (`SlotPlan`, `RowCursor`, `BatchCursor`, `NativeLmdbQuerySource`, `LmdbCsrAdjacencyCache`) are consumed, not modified, except for possible package-private accessors recorded in the Decision Log.

---

Revision note (2026-07-22, initial version): plan authored from the two exploration reports after PR #5736 was fetched; supersedes nothing. Written before any code/benchmark transplant, per the benchmarks→tests→code ordering requested by the user.
