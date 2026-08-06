# Parallel IR kernel parity: fork filters, dedup DISTINCT, merge mods — everything the interpreted parallel engine already does with threads

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (repository root: `/`, plan conventions file: `.agent/PLANS.md`).

## Purpose / Big Picture

The LMDB "native engine" in `core/sail/lmdb` evaluates SPARQL queries in tiers. The newest tier lowers a query fragment to a small intermediate representation (IR), generates Java source for it, and compiles that source at runtime with the Janino compiler into a single tight loop (a "kernel"). Kernels are the fastest tier. They also have a morsel-parallel rung: the root enumeration is split into key windows and one kernel instance per window runs on a worker thread (`LmdbNativeParallelKernelRows` for row output, `LmdbNativeParallelKernelAggregate` for GROUP BY output).

Today that parallel rung is admitted very narrowly. A query as simple as

    SELECT ?s ?a ?b WHERE { ?s :p ?a ; :q ?b . FILTER(?a != ?b) }

is declined by the parallel kernel rung with reason `filter-hooks` — even though the exact same query runs multi-threaded in the older tiers: the interpreted parallel pipeline engine (`LmdbNativeParallelPipelines`) forks the filter per worker and scans in parallel. So turning on the fastest tier currently *removes* parallelism for filtered queries.

After this plan is implemented, the parallel kernel rung accepts everything the interpreted parallel engine accepts: hook-invoked filters and residual filters (when each filter can fork a worker-private copy), in-kernel DISTINCT, in-kernel ORDER/LIMIT/OFFSET, aggregate HAVING and output modifiers, MIN/MAX aggregates over general values, and kernels rooted at a raw quad scan. Every remaining decline reason will name a condition under which the interpreted engine *also* stays sequential, so "kernel parallel declined" never again means "the older tier would have been faster".

Observable outcome: with `-Drdf4j.lmdb.janinoCodegen.debug=true`, the query above stops printing `[ir-kernel-parallel] decline: filter-hooks` and instead runs through the parallel path (`LmdbNativeParallelKernelRows.PARALLEL_RUNS` increments), returning identical results to the sequential kernel.

## Progress

- [x] (2026-08-06 09:10Z) Milestone 0/1: forkable filter hooks in the rows rung. New end-to-end test `LmdbNativeIrKernelParallelTest#parallelIrKernelRunsForkableFilterHooks` (BGP + `FILTER(?v != ?v2)` + OPTIONAL); red/green captured by temporarily restoring the blanket decline (failed: `Expecting actual: 0L to be greater than: 0L`) then reverting (passed). Admission is now `LmdbNativeKernelPartitions.filterHooksForkable`; workers fork via `forkFilterHooks` and close via `closeForkedHooks`.
- [x] (2026-08-06 09:10Z) Milestone 2: forkable residual filters forked per worker and evaluated worker-side (`applyResiduals` compacts the packed buffer, replicating the consumer's bind-then-filter semantics on an entry-seeded scratch row). No query-path lowering currently emits residual filters together with a partitionable root, so coverage is via the admission gate only — first live shape will exercise it.
- [x] (2026-08-06 09:10Z) Milestone 3: aggregate rung filter-hook parity; end-to-end test `LmdbNativeIrAggregateParallelTest#parallelIrAggregateRunsForkableFilterHooks` green. Worker hooks objects stay PER RANGE (the numeric accumulator sidecar is indexed by each kernel instance's group ordinals) but share the per-worker forked filter array.
- [x] (2026-08-06 09:10Z) Milestone 4: consumer-side global dedup for in-kernel DISTINCT (streaming and ordered paths). DORMANT on the query path: lowering constructs every Emit with `distinct=false` today (see Surprises), so validation is limited to code review + the emitter-level distinct tests staying green.
- [x] (2026-08-06 09:10Z) Milestone 5: kind survey done — SPARQL MIN/MAX already lower to `AGG_MIN_ID`/`AGG_MAX_ID`, which the rung already merges via `compareValues`; the double-tier `AGG_MIN`/`AGG_MAX` kinds are never produced by lowering. Remaining unmergeable kinds are exactly the DISTINCT-input ones; see Outcomes for why they stay declined.
- [x] (2026-08-06 09:10Z) Milestone 6: aggregate HAVING + output mods post-merge, via a worker kernel variant (`having=null`, `OutputMods.none()`) compiled under its own shape key; consumer applies the count-compare HAVING and the `KernelRuntime` sort/slice. DORMANT on the query path (lowering never sinks HAVING/mods yet).
- [x] (2026-08-06 09:10Z) Milestone 7: row output mods — worker variant folds OFFSET into LIMIT; unordered LIMIT/OFFSET counted on packed rows in the streaming cursor; ORDER BY drains all pages pre-handoff and sorts through `KernelRuntime.sortRows`/`topKRows` with the hook sidecar for value order. DORMANT on the query path for the same reason.
- [ ] Milestone 8: ScanQuad-rooted kernels partitioned by root ranges — NOT implemented; needs a windowed-scanner spike (see Outcomes).
- [x] (2026-08-06 09:45Z) Milestone 9: decline census (`LmdbNativeKernelDeclineCensusTest`, 3 tests, ~8 min) green; emitter suite (44), cleanup contract (36), both parallel kernel suites (5+5) green; formatter + copyright check run; FULL `core/sail/lmdb` module verify green — 2997 tests, 0 failures, 0 errors, 6 skipped, aggregated from the surefire/failsafe report XMLs (prior full-green baseline 2978/0/0). Benchmark gates (theme spot checks) not run this session — flags unchanged (rows rung default ON, aggregate rung default OFF), so no flip needed gating.

## Surprises & Discoveries

- Observation: the filter produced by `FILTER(?a != ?b)` (`OrderedSlotCompareFilter` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFilters.java`, around line 1132) already implements `parallelWorkerForkable()`/`forkForParallelWorker()`. The parallel kernel rung never asks; it declines on the mere presence of a hook.
  Evidence: `LmdbNativeParallelKernelRows.java:103` declines when `bindings.filterHooks.length > 0 || !bindings.residualFilters.isEmpty()` with no forkability check, while `LmdbNativeParallelPipelines.parallelWorkerForkable(MaskedFilter[])` (line 382) is the interpreted engine's per-filter gate.
- Observation: no query-path lowering ever installs DISTINCT, ORDER/LIMIT/OFFSET, or HAVING on a kernel today. All three construction sites in `LmdbNativeKernelLowering` (lines 347, 2223, 2265) pass `distinct=false`, `having=null`, `OutputMods.none()`; the only callers of `.distinct()`, `.having(...)`, `withMods(...)` are the IR emitter unit tests. So the `in-kernel-distinct`, `output-mods`, and `having-or-output-mods` declines were DEAD GATES on the query path — the sink-side machinery is IR-ready but not wired into lowering. Milestones 4/6/7 are therefore implemented ahead of the lowering: correct, compiled, but unreachable end-to-end until DISTINCT/ORDER/HAVING sinking lands.
  Evidence: `rg -n "withMods|\.distinct\(\)|\.having\(" core/sail/lmdb/src/main/java` matches only `LmdbNativeKernelIr.java` and the new parallel-rows variant construction.
- Observation: SPARQL MIN/MAX lower to `AGG_MIN_ID`/`AGG_MAX_ID` (value-id winners folded through `hooks.compareValues`), which `mergeWinner` in the aggregate rung already merges by SPARQL value order. The double-tier `AGG_MIN`/`AGG_MAX` IR kinds (numeric doubles, `Double.doubleToLongBits` in the packed row) are never produced by the aggregate lowering. Milestone 5 was therefore a no-op beyond the survey.
  Evidence: `LmdbNativeKernelLowering.java` cases `MIN:`/`MAX:` call `AggregateOutput.minId/maxId` (lines ~2185-2195); `LmdbNativeParallelKernelAggregate.mergeWinner` already calls `hooks.compareValues`.
- Observation: the interpreted parallel aggregation DOES merge DISTINCT aggregates ("merges are addition plus distinct-id-set union", `LmdbNativeParallelAggregation` class javadoc) — so `AGG_COUNT_DISTINCT`/`AGG_SUM_DISTINCT`/`AGG_AVG_DISTINCT` are a real parity gap for the kernel rung. Closing it needs the generated kernel to expose its per-group `KernelRuntime.LongHashSet` distinct channels to the drain (a new emitter surface), not just an admission change. Deliberately left declined; see Outcomes.
  Evidence: the distinct sets live only as generated-code fields (`agD<i>[g]` in `LmdbNativeKernelEmitter`); `fill` emits counted rows, never the sets.
- Observation: the red-check proves the end-to-end wiring: with the blanket decline temporarily restored, `parallelIrKernelRunsForkableFilterHooks` fails with `Expecting actual: 0L to be greater than: 0L` — i.e. the `FILTER(?v != ?v2)` query really does reach the kernel as a filter HOOK (not an IR-native compare node), and forking is what unlocks the parallel run.
  Evidence: Surefire failure snippet above; green re-run after revert (tests=1, failures=0).

## Decision Log

- Decision: reuse the existing `NativeBooleanFilter.parallelWorkerForkable()`/`forkForParallelWorker()` SPI instead of adding locks or a new kernel-specific SPI.
  Rationale: the interpreted parallel engine (`LmdbNativeParallelPipelines.forkFilters`) and the leapfrog join already fork through this SPI; every filter that is safe there is safe here, and the admission rule becomes identical by construction ("all filters forkable"), which is exactly the parity the user asked for.
  Date/Author: 2026-08-06 / Claude (planning session).
- Decision: run forked residual filters worker-side (compacting the packed row buffer before it is shipped) rather than on the consumer thread.
  Rationale: parity again — the interpreted engine evaluates filters inside the workers, so filtering cost scales with threads; consumer-side evaluation would serialize it. Correctness is unaffected because the packed emit columns fully determine the filter's slot reads once the entry row is reseedable (a gate the rung already enforces).
  Date/Author: 2026-08-06 / Claude (planning session).
- Decision: keep the `plan-producer` decline (kernels whose bindings carry `PlanRequest`s, i.e. EXISTS/NOT EXISTS subplans executed as bound plans).
  Rationale: this is not a parity gap. The interpreted parallel engine also declines EXISTS filters (`ExistsFilter` does not override `parallelWorkerForkable()`, so `stateful-filter` rejects it — memo tables and lazily created native probes are per-instance mutable state). Making EXISTS forkable would benefit both engines and belongs in its own plan.
  Date/Author: 2026-08-06 / Claude (planning session).
- Decision: implement Milestones 1-7 in a single pass and validate end-to-end only what the query path can reach (filter hooks in both rungs); accept IR-level/dormant status for the mods/distinct/having machinery rather than blocking on sink-side lowering that does not exist yet.
  Rationale: the user asked for all milestones at once; the discovery that lowering never sinks DISTINCT/ORDER/HAVING (see Surprises) means end-to-end tests for those simply cannot exist today, and building the lowering itself is a separate feature with its own plan-scale.
  Date/Author: 2026-08-06 / Claude (implementation session).
- Decision: skip Milestone 8 (ScanQuad roots) in this pass and leave DISTINCT-input aggregate kinds declined.
  Rationale: both need new machinery below the admission layer (windowed scan iterators; a generated-code drain surface for distinct sets) — high-risk exploratory work that should not ride along with an eight-file change already touching concurrency. Their decline reasons are unchanged and the census stays honest.
  Date/Author: 2026-08-06 / Claude (implementation session).
- Decision: implement DISTINCT as per-worker in-kernel dedup plus an exact global dedup on the consumer thread, instead of recompiling a distinct-free worker kernel.
  Rationale: the per-worker distinct is free (it is already in the generated code) and shrinks cross-thread traffic; the consumer dedup restores exactness because worker-local dedup can only let *cross*-partition duplicates through. The interpreted engine gets DISTINCT for free the same way — its parallel scan feeds a query-thread distinct operator.
  Date/Author: 2026-08-06 / Claude (planning session).
- Decision: ORDER/LIMIT/OFFSET and HAVING require a *worker kernel variant* — a second lowering of the same fragment with the global output modifiers removed (offset dropped, limit widened to offset+limit, HAVING dropped) — with the global modifiers applied on the consumer.
  Rationale: those modifiers are properties of the whole result; the generated code bakes them in, so running the unmodified kernel per partition silently drops rows (a per-worker OFFSET skips rows in *every* partition). A variant keyed separately in the compile cache keeps the sequential shape untouched.
  Date/Author: 2026-08-06 / Claude (planning session).

## Outcomes & Retrospective

2026-08-06, first implementation pass (all milestones attempted in one session):

Delivered and live on the query path: forkable filter hooks in both parallel kernel rungs — the headline `filter-hooks` decline for `FILTER(?a != ?b)`-style queries is gone, with end-to-end red/green tests in both rung suites. The EXISTS decline test still passes unchanged (its filter is genuinely non-forkable, matching the interpreted engine). Delivered but dormant pending sink-side lowering: worker-side residual filters, consumer DISTINCT dedup, ORDER/LIMIT/OFFSET (worker variant + `KernelRuntime` consumer sort/slice), aggregate HAVING/mods post-merge (worker variant). These paths compile and their sequential IR-level twins are test-covered, but no query can reach them until lowering learns to sink DISTINCT/ORDER/HAVING into kernels — when that lands, the parallel rungs are already parity-complete for it.

Not delivered, with reasons: (1) ScanQuad-rooted partitioning (Milestone 8) — needs a windowed variant of `LmdbNativeKernelScanner`'s per-site LMDB range iterators plus reuse of `LmdbNativeExchange.tryPlanRootPartitions`; genuinely exploratory, start with the spike described in Plan of Work. (2) DISTINCT-input aggregate kinds — a REAL parity gap vs the interpreted engine (which unions distinct-id sets), but closing it requires the generated kernel to expose its per-group distinct sets through a new drain surface; that is emitter work of the same order as a new terminal and was out of proportion for this pass. Both remain as follow-ups with their own decline reasons intact.

Lesson: the parity inventory changed shape mid-flight — two of the seven presumed gaps (MIN/MAX, and most of the mods/distinct/having surface) turned out to be either already-closed or unreachable-by-lowering. Surveying which gates are LIVE on the query path before sizing milestones would have re-ordered the work; the Surprises entries record the evidence for the next contributor.

## Context and Orientation

All paths below are relative to the repository root. Everything lives in the LMDB store module `core/sail/lmdb`, package `org.eclipse.rdf4j.sail.lmdb.evaluation` (main sources under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`, tests under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/` and its `evaluation` subpackage).

Terms used in this plan:

- *Native engine*: the LMDB-specific query evaluation stack that operates on long-encoded value ids and adjacency structures instead of materialized RDF values.
- *Slot plan*: the interpreted native operator tree (`SlotPlan`, `MultiJoinPlan`, `PatternPlan`, `FilterPlan`, … in this package). A `MaskedFilter` pairs a `NativeBooleanFilter` predicate with the bitmask of engine slots it reads.
- *IR kernel*: `LmdbNativeKernelLowering` translates an admitted slot plan into `LmdbNativeKernelIr` nodes (`EnumerateAdjKeys`, `ScanQuad`, `Probe`, `FilterValue`, `Emit`, `Aggregate`, …); `codegen.JaninoKernel` is the runtime-compiled loop. `LmdbNativeKernelBindings` carries everything the compiled code needs at bind time: adjacency requests, constants, key domains, `FilterHook[]` (filters invoked from inside the generated loop through the `KernelHooks` callback SPI), `residualFilters` (filters applied after emission), `PlanRequest[]` (subplans the kernel executes as row producers, e.g. lowered EXISTS), and the aggregate `KernelGroupLayout`.
- *Hooks object*: `LmdbNativeKernelHooks` implements the `KernelHooks` callback SPI for one execution: `testFilter(filterId, a0..a2)` copies the argument ids into the registered slots of a private scratch `RowState` and calls `bindings.filterHooks[filterId].source.filter.accept(scratch)`. The hooks object itself is already created per execution (and per worker in the parallel rung); only the `NativeBooleanFilter` inside each `FilterHook` is shared — that shared mutable filter is the whole reason for the current `filter-hooks` decline.
- *Parallel kernel rungs*: `LmdbNativeParallelKernelRows` (row output; flag `rdf4j.lmdb.irKernelParallel.enabled`, default ON) and `LmdbNativeParallelKernelAggregate` (GROUP BY output; flag `rdf4j.lmdb.irAggregateParallel.enabled`, default OFF). Both partition the root enumeration into contiguous key-ordinal windows (`LmdbNativeKernelPartitions.ranges`, `KeyWindowView`), run one kernel instance per window on the shared pool (`LmdbNativeParallelPipelines.pool()`), each worker against its own pinned-snapshot source (`row.source.openParallelSources(threads)`), and stream packed row pages (rows rung) or per-worker partial group maps (aggregate rung) back to the query thread.
- *Interpreted parallel engine*: `LmdbNativeParallelPipelines` (row scans) and `LmdbNativeParallelAggregation` (GROUP BY) — the pre-Janino multi-threaded tiers. Their filter admission is `LmdbNativeParallelPipelines.parallelWorkerForkable(plan.filters)` and their per-worker setup is `forkWorkerPlans` → `forkFilters`, which calls `NativeBooleanFilter.forkForParallelWorker()` on every filter and wraps each fork in `CloseOnceNativeBooleanFilter`. This is the parity baseline.
- *Forkable filter*: a `NativeBooleanFilter` whose `parallelWorkerForkable()` returns true and whose `forkForParallelWorker()` returns a worker-confined copy (shared immutable expression kernels allowed; no shared mutable counters, memo tables, or close ownership). Forkable today: `OrderedSlotCompareFilter` (slot-vs-slot compare — the `?a != ?b` case), `CachedCompareFilter` (slot-vs-constant), `ValueSetFilter` (IN-set), `NegatedNativeBooleanFilter` and `BooleanCombinationFilter` (when children are), `RecordingNativeBooleanFilter` (when its delegate is; the fork carries the statistics context so feedback still publishes on close). Not forkable: `GenericBooleanFilter` (wraps an arbitrary `Predicate<BindingSet>`) and `ExistsFilter` (memo + native probes).

Current decline gates, and their parity classification:

`LmdbNativeParallelKernelRows.tryOpen` declines on: `not-emit`, `zero-columns` (structural, keep); `in-kernel-distinct` (Milestone 4); `output-mods` (Milestone 7); `plan-producer` (keep — interpreted engine declines EXISTS too, see Decision Log); `filter-hooks` (Milestones 1–2); `root-not-partitionable` (Milestone 8 widens to ScanQuad roots); `single-thread`, `below-threshold`, `root-too-small`, `task-budget`, `snapshot-unavailable` (resource/cost gates shared with the interpreted engine, keep); `correlated-entry` (keep — the interpreted engine rejects correlated entries too, `LmdbNativeParallelPipelines` reason `correlated-entry`).

`LmdbNativeParallelKernelAggregate.tryEvaluate` declines additionally on: `having-or-output-mods` (Milestone 6), `unmergeable-aggregate-kind-*` (Milestone 5), and the same `filter-hooks` (Milestone 3).

Existing test anchors: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeIrKernelParallelTest.java` (rows rung; asserts `PARALLEL_RUNS` moves and results match sequential), `LmdbNativeIrAggregateParallelTest.java` (aggregate rung), `LmdbNativeParallelPipelinesTest.java` (interpreted engine, includes forked-filter coverage to crib from), and the decline censuses `LmdbNativeKernelDeclineCensusTest.java` / `LmdbNativeKernelAdversarialDeclineTest.java`.

Debugging: `-Drdf4j.lmdb.janinoCodegen.debug=true` prints `[ir-kernel-parallel] decline: <reason>` and `[ir-aggregate-parallel] fallback: <problem>` to stderr.

Build/test workflow (repository conventions): run the root install once per session, then use the mvnf skill for tests. From the repository root:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeIrKernelParallelTest

Never pass `-am` or `-q` when running tests. New Java files need the standard copyright header plus the agent signature comment, then `cd scripts && ./checkCopyrightPresent.sh`, then the formatter `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` before finalizing.

## Plan of Work

### Milestone 0 — Failing admission tests for every parity gap

Scope: encode the parity contract as tests before touching production code. In `LmdbNativeIrKernelParallelTest`, add one test per gap, each loading a small dataset (large enough to clear `root-too-small` and `below-threshold`; the existing tests in that class show how — they load repeated key fan-out and lower `rdf4j.lmdb.parallel.minWorkEstimate`/thread properties), running a query, asserting result equality against the same query with `rdf4j.lmdb.irKernelParallel.enabled=false`, and asserting `LmdbNativeParallelKernelRows.PARALLEL_RUNS` incremented. The queries: (a) BGP + `FILTER(?a != ?b)` (hook filter, the user's case); (b) BGP + a filter that lowers to a residual (one reading a slot combination the kernel does not pass through `testFilter` — inspect `LmdbNativeKernelLowering` to construct one, or take one from `LmdbNativeKernelDeclineCensusTest`); (c) `SELECT DISTINCT` over a fragment with cross-partition duplicates (two subjects sharing an object so partitions overlap in emitted tuples); (d) `ORDER BY ?x LIMIT n OFFSET m`; (e) the aggregate twins in `LmdbNativeIrAggregateParallelTest`: `MIN(?str)`/`MAX(?str)` over plain literals (value-tier compare), `HAVING`, and hook-filtered aggregate; (f) a kernel whose root is a raw scan (a single unbound-subject pattern too wide for adjacency enumeration — check `partitionableRootAdjacency` preconditions to construct one). Mark each with the milestone that will flip it, and until then assert the *current* behavior (parallel runs did NOT move, results still correct) so the suite stays green — each later milestone flips its test's assertion to the parallel expectation. This gives every milestone its red/green pair without leaving the tree red between milestones.

What exists after: a test naming every gap, self-documenting the parity contract.

### Milestone 1 — Forkable filter hooks in the rows rung

Scope: `LmdbNativeParallelKernelRows` plus small additions to `LmdbNativeKernelBindings` and `LmdbNativeKernelHooks`.

Admission change in `tryOpen` (line ~103): replace the unconditional `filter-hooks` decline for `filterHooks` with a forkability check — every `bindings.filterHooks[i].source.filter.parallelWorkerForkable()` must be true, else decline with the new distinct reason `filter-not-forkable` (so the census can tell a genuine parity-complete decline from a leftover gap). Residual filters keep declining in this milestone (reason stays `filter-hooks` until Milestone 2 renames it `residual-filter`).

Worker change in `runWorker`: once per worker (before the range loop, not per range), build a worker-private `FilterHook[]`: for each hook, `NativeBooleanFilter fork = hook.source.filter.forkForParallelWorker()`; wrap as `new FilterHook(new MaskedFilter(fork, source.mask, source.adaptive, source.plannedDepth), hook.argSlots)`. If any fork returns null despite the preflight, throw `ParallelKernelDecline` (same pattern as `worker-seed-unavailable` — pre-handoff this declines cleanly to the sequential cursor). Give `LmdbNativeKernelHooks` a constructor overload `LmdbNativeKernelHooks(RowState liveRow, LmdbNativeKernelBindings bindings, FilterHook[] hooks)` that uses the supplied array instead of `bindings.filterHooks`, keeping the existing constructor delegating to it. Hoist the hooks object out of the per-range loop (it is currently re-created per range; one per worker is correct and cheaper — the scratch row is reset after every `testFilter` call by design). In the worker's `finally`, close every forked filter (mirror `LmdbNativeFactorizedRows.closeFilters(NativeBooleanFilter[], Throwable)` for suppressed-failure chaining); this is the per-worker analogue of `LmdbNativeKernelHooks.closeFilters()`, which the sequential route calls at `LmdbNativeKernelExecution.java:211` — forked filters may lazily acquire native read stamps, and leaking one wedges dataset close (see the closeFilters javadoc for the 2026-08-05 hang this guards against). Fork-time feedback: `RecordingNativeBooleanFilter.forkForParallelWorker()` already returns a recording fork that publishes its pass/filter counts on close, matching the interpreted engine's behavior — nothing extra needed beyond closing the forks.

Also mirror the exact same admission + per-worker fork in the aggregate rung's shared helper if you extract one now (Milestone 3 does this; extracting a package-private helper `LmdbNativeKernelPartitions.forkFilterHooks(FilterHook[])` + `closeForkedHooks(...)` in this milestone avoids duplication).

Flip test (a); the debug run of the user's query must stop printing `decline: filter-hooks`.

### Milestone 2 — Forkable residual filters, worker-side

Scope: `LmdbNativeParallelKernelRows` only. Residual filters (`bindings.residualFilters`, applied by the sequential `KernelRowCursor` after binding each packed row — see `LmdbNativeKernelExecution.java:515`) become admissible when every one is forkable. Each worker forks its own copies (same helper as Milestone 1) and, after each `kernel.fill(buffer, n)`, evaluates them against a worker-private scratch `RowState`: for each packed row, install the emit columns into the scratch row's slots via `bindings.columnEngineSlots` (skipping `UNKNOWN` exactly as the consumer's bind loop does), recompute the bound mask, run each forked residual's `accept`, and compact surviving rows in place in the buffer before packing the page. This is legal because admission already guarantees the entry row is reseedable (`entryReseedable` gate) — the worker's scratch state after installing a packed row equals the consumer's live row state after binding that row, so filter decisions are identical to the sequential route. Keep the consumer (`ParallelKernelRowCursor.next`) untouched — it must not re-apply residuals. Note in a comment that `KERNEL_ROWS` telemetry now counts post-residual rows on the parallel path; keep it consistent with what the sequential cursor counts (verify which side of the residual the sequential path counts on and match it). Flip test (b); rename the leftover decline reason for non-forkable residuals to `residual-filter-not-forkable`.

### Milestone 3 — Filter-hook parity in the aggregate rung

Scope: `LmdbNativeParallelKernelAggregate`. Apply Milestones 1–2 identically: forkability-based admission at line ~114, per-worker forked `FilterHook[]` in `runWorker` (line ~326 already creates a per-worker hooks object — hand it the forked array), per-worker close in the worker's `finally`. The consumer-side `mergeHooks` (line 246) only uses `compareValues`/numeric-merge and never invokes filters, so it can keep using the shared `bindings` array — state this in a comment. The numeric accumulator arrays inside each worker's hooks object are already worker-private and already merged via `installNumericPartial`; nothing changes there. Flip the aggregate hook-filter test. Note the rung's flag is default OFF (`Boolean.getBoolean`), so tests set `rdf4j.lmdb.irAggregateParallel.enabled=true` explicitly.

### Milestone 4 — DISTINCT: per-worker dedup plus consumer-side global dedup

Scope: `LmdbNativeParallelKernelRows`. Remove the `in-kernel-distinct` decline. Workers run the unmodified kernel (its in-kernel distinct now dedups within the partition, shrinking pages). The consumer (`ParallelKernelRowCursor`) gains, only when `emit.distinct` is set, an exact global dedup over the packed emit columns before binding: a hash set keyed on the row's column longs (a simple open-addressing long-tuple set — hash the `columnSlots.length` longs of each row; `LongsKey` in `LmdbNativeParallelKernelAggregate` is an existing packed-longs key you can reuse or mirror; avoid per-row boxed allocation if a primitive layout is easy, but correctness first). Memory note for the plan reader: the set holds at most as many tuples as the distinct result itself, which the sequential in-kernel distinct also materializes, so this is not a new memory class. Flip test (c) — it must produce duplicates across partitions before dedup (assert the parallel result set equals the sequential result *as multisets*).

### Milestone 5 — Aggregate MIN/MAX value-tier merge; kind survey

Scope: `LmdbNativeParallelKernelAggregate`. First a short verification step: read `LmdbNativeParallelAggregation` (the interpreted parallel GROUP BY) and record in this plan's Decision Log exactly which `AggKind`s it merges — that is the parity target; do not exceed it here. Then admit `AGG_MIN`/`AGG_MAX` (value-tier compare, distinct from the already-admitted `AGG_MIN_ID`/`AGG_MAX_ID` ordered-id kinds): in `mergePartial`, compare the two partials' candidate ids with `mergeHooks.compareValues(left, right)` (the same SPARQL `ValueComparator` semantics the kernel itself uses via `KernelHooks.compareValues`) and keep the winner. The DISTINCT-input kinds (`AGG_COUNT_DISTINCT`, `AGG_SUM_DISTINCT`, `AGG_AVG_DISTINCT`) need cross-partition value-set union to merge; only implement them if the verification step shows the interpreted engine parallelizes them — otherwise leave them declining with their current reason and record why (parity, not capability, is this plan's bar). Flip the MIN/MAX test.

### Milestone 6 — Aggregate HAVING and output mods post-merge

Scope: `LmdbNativeParallelKernelAggregate`, `LmdbNativeKernelLowering`, and the kernel compile-cache keying in `LmdbNativeKernelExecution`. The generated aggregate kernel applies HAVING and ORDER/LIMIT/OFFSET to *final* group values at drain time; per-partition partials must not run them (they would filter/truncate fragments of a group). Introduce a worker kernel variant: from the same `Lowered` fragment, produce a second `Kernel` whose terminal `Aggregate` has `having = null` and `mods = null`, compiled through the same Janino cache under a shape key extended with a variant marker (find where the shape key is built next to the `Supplier<JaninoKernel>` the callers pass in — `LmdbNativeKernelExecution` constructs the factory; thread a second factory or a variant flag through to the parallel rung). Workers drain partials from the variant; the consumer merges partials exactly as today, then applies HAVING and mods on the merged final groups before emission — HAVING semantics are the kernel's own (re-use the same evaluation the sequential drain performs on final groups; if that logic lives only inside generated code, evaluate HAVING on the consumer through the existing hooks/filters that the lowering captured for it — verify how `Aggregate.having` is represented before coding, and record it here). ORDER/LIMIT/OFFSET on merged groups is a plain sort+slice on the query thread, mirroring how the interpreted engine's GROUP BY feeds the downstream order operator. Remove the `having-or-output-mods` decline; flip its test.

### Milestone 7 — Row output mods via worker kernel variant plus consumer merge

Scope: `LmdbNativeParallelKernelRows` plus the same variant-compilation seam as Milestone 6. Two cases. Unordered `LIMIT`/`OFFSET` (no `orderKeys`): workers run a variant with offset dropped and limit widened to `offset+limit` (a worker can never need more rows than the global slice); the consumer counts emitted rows, skips the first `offset`, stops after `limit`, and cancels the group (the existing `cancelled` flag plus `close()` already tears workers down). Ordered (`orderKeys != null`): each worker's variant keeps ORDER (per-partition sorted output) with offset dropped and limit widened to `offset+limit`; the consumer performs a k-way merge across worker streams on the order keys — this requires per-worker output streams rather than the current single multiplexed queue, so give each worker its own bounded queue and merge with a small heap; compare rows using the same comparator the kernel's order phase uses (again: verify how `mods.orderKeys` comparisons are emitted, and evaluate the same comparison on the consumer via `KernelHooks.compareValues` per key). If the ordered case proves too invasive in one go, split it: land unordered LIMIT first (flip half of test (d)) and keep `output-mods-ordered` as a distinct decline until the merge lands. Either way the end state removes the blanket `output-mods` decline.

### Milestone 8 — ScanQuad-rooted kernels

Scope: `LmdbNativeKernelPartitions` and both rungs. Kernels whose pipeline is rooted at `ScanQuad` (a raw index scan, used when no adjacency/domain enumeration fits) currently fail `root-not-partitionable`, while the interpreted engine range-partitions root pattern scans (`LmdbNativeExchange.tryPlanRootPartitions`, exercised by `LmdbNativeRangePartitionedScanTest`). Add `partitionableRootScan(pipeline)` (root is `ScanQuad`, no other node re-reads the same scan site) and a scan window: workers receive per-worker scan ranges derived the same way `LmdbNativeExchange` plans root partitions (reuse it if the seam allows — it needs the source and pattern; the kernel side knows the scan's `scanOrders[site]` and the bound constants from bindings). The partition surface is a range-restricted scanner for the root site only — inspect `LmdbNativeKernelScanner` to find the narrowest place to install a per-worker `[from, to)` bound (its constructor takes `bindings.scanOrders`; a decorator like `KeyWindowView` is the model). This milestone is the most exploratory: begin with a short spike proving a windowed scanner returns exactly a partition of the full scan on a test store, and record findings here before wiring admission. Flip test (f).

### Milestone 9 — Census, benchmarks, cleanup

Re-run the kernel decline censuses (`LmdbNativeKernelDeclineCensusTest`, `LmdbNativeKernelAdversarialDeclineTest`) and update their expected reasons (`filter-hooks` disappears; `filter-not-forkable`/`residual-filter-not-forkable` appear only where the interpreted engine also declines). Run the theme benchmark spot checks on queries that previously declined (use `scripts/run-single-benchmark.sh` with a `ThemeQueryBenchmark` query known to carry a slot-compare filter; compare against a run with `rdf4j.lmdb.irKernelParallel.enabled=false`), remembering the theme-benchmark noise floor: judge by disjoint error intervals, never by score alone. Confirm flags: rows rung stays default ON, aggregate rung stays default OFF unless the user decides otherwise (raise it in the handoff). Update `.agent/three-tier-parity-execplan.md`'s Milestone 10B notes to point at this plan. Full module verify (`python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb`) — verify sweeps via the report XMLs, not the mvnf summary line.

## Concrete Steps

Working directory is always the repository root `/Users/havardottestad/Documents/Programming/rdf4j` unless stated.

1. Once per session: root quick install (command in Context and Orientation; expect `BUILD SUCCESS` in the reactor summary within ~30–60s).
2. Per milestone: write/flip the tests first, run the narrowest selection, e.g.

       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeIrKernelParallelTest#parallelRunsHookFilterQuery

   expect the new assertion to fail before the production change (Surefire report under `core/sail/lmdb/target/surefire-reports/`), then implement, then re-run the same selection green, then broaden:

       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeIrKernelParallelTest
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeIrAggregateParallelTest
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelDeclineCensusTest

3. Manual observation of the headline fix (Milestone 1): a scratch test or main that loads a store, enables `-Drdf4j.lmdb.janinoCodegen.debug=true`, runs the `FILTER(?a != ?b)` BGP, and shows no `[ir-kernel-parallel] decline: filter-hooks` line while `LmdbNativeParallelKernelRows.PARALLEL_RUNS` increased.
4. Formatting + headers before every commit: `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`, `cd scripts && ./checkCopyrightPresent.sh`.
5. Commit per milestone on the current branch with the `GH-0000` prefix (no issue number was provided; call this out in the handoff), e.g. `GH-0000 lmdb ir-kernel parallel: forkable filter hooks (parity M1)`.

## Validation and Acceptance

Acceptance per milestone is behavioral: the milestone's Milestone-0 test flips from "sequential fallback, results correct" to "parallel run observed (`PARALLEL_RUNS` incremented), results identical to the sequential kernel and to the interpreted engine". Overall acceptance: the decline census over the parity query set shows no reason that the interpreted parallel engine would have admitted, and the whole `core/sail/lmdb` module verify is green (13 pre-existing suite failures noted in memory as of 2026-07-21 excepted — re-baseline before starting: run the module verify once before any change and store the report summary as the baseline via `python3 scripts/agent-evidence.py ... > initial-evidence.txt`).

Correctness invariants each test must assert: result multiset equality (set equality plus cardinality) against `rdf4j.lmdb.irKernelParallel.enabled=false`; for ORDER BY, exact sequence equality; for aggregates, group-key/value equality including the empty-input global-aggregate row. Concurrency risk is High by this repo's proportionality model, so every worker-side change also runs under the existing cleanup tests (`LmdbNativeParallelAggregationCleanupTest` pins the filter-close contract) — a leaked native read stamp reproduces as a dataset-close hang, which is exactly what the per-worker close in Milestone 1 guards.

## Idempotence and Recovery

Every milestone is additive and flag-guarded: the parallel rungs decline to the sequential kernel on any pre-handoff failure, and the kill switches (`rdf4j.lmdb.irKernelParallel.enabled=false`, `rdf4j.lmdb.irAggregateParallel.enabled=false`, `rdf4j.lmdb.parallel.enabled` for the shared pool) restore today's behavior at runtime without reverting code. If a milestone's worker-side change misbehaves mid-implementation, revert only that milestone's commit; earlier milestones stand alone. Keep all untracked benchmark artifacts; never clean the protected theme-benchmark store (it is guarded in the lmdb pom's clean-plugin config, but do not test fate with manual deletes).

## Artifacts and Notes

The key call sites, verified 2026-08-06 on branch `optimize-lmdb` (commit a3cbd411a3 + uncommitted M6 work):

    LmdbNativeParallelKernelRows.java:103   — blanket filter-hooks decline to replace
    LmdbNativeParallelKernelRows.java:262   — per-worker hooks object already exists (per-range; hoist)
    LmdbNativeParallelKernelAggregate.java:114 — aggregate twin of the decline
    LmdbNativeParallelPipelines.java:382,414 — parity baseline: parallelWorkerForkable + forkFilters
    LmdbNativeFilters.java:1132             — OrderedSlotCompareFilter fork (the ?a != ?b filter)
    LmdbNativeKernelHooks.java:210          — closeFilters release contract to mirror per worker
    LmdbNativeKernelExecution.java:515      — sequential residual-filter application to replicate
    LmdbNativeKernelPartitions.java         — ranges/KeyWindowView partition surface; home for new helpers

## Interfaces and Dependencies

No new external dependencies. New/changed internal surfaces at completion:

In `LmdbNativeKernelHooks` (same file), add:

    LmdbNativeKernelHooks(RowState liveRow, LmdbNativeKernelBindings bindings, LmdbNativeKernelBindings.FilterHook[] hooks)

In `LmdbNativeKernelPartitions`, add package-private helpers:

    static LmdbNativeKernelBindings.FilterHook[] forkFilterHooks(LmdbNativeKernelBindings.FilterHook[] hooks)
    static NativeBooleanFilter[] forkResidualFilters(List<MaskedFilter> residuals)
    static Throwable closeForked(NativeBooleanFilter[] forked, Throwable failure)
    static int partitionableRootScan(List<Node> pipeline)            (Milestone 8)

Both parallel rungs gain the admission predicate "all hook and residual filters `parallelWorkerForkable()`", new decline reasons `filter-not-forkable` and `residual-filter-not-forkable`, and (Milestones 6–7) a variant-kernel factory parameter alongside the existing `Supplier<JaninoKernel>`. The `NativeBooleanFilter` SPI itself is unchanged.

---

Revision note (2026-08-06, initial version): plan authored from a code survey of the current decline gates vs the interpreted parallel engine's admission; the headline gap is the blanket `filter-hooks` decline for filters that are already worker-forkable.
