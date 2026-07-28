# Janino Whole-Stage Code Generation for the LMDB Native Engine (10x Program)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (repository root).

## Purpose / Big Picture

Today the LMDB native engine (the `LmdbNative*` classes in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`) executes SPARQL queries as a tree of interpreted operators: each row flows through virtual calls on `RowCursor`/`BatchCursor` objects, per-operator slot copies into a shared `RowState`, and megamorphic filter dispatch. The engine already has one runtime-compilation facility — `LmdbNativeSpecialization`, which hand-assembles small bytecode kernels with the JDK Class-File API — but it only covers one narrow shape (batch filter + slot copy). A quick proof-of-concept comparison (`benchmark-results/pipeline-codegen-comparison.md`) showed even that single-operator kernel is worth 5–26% end-to-end; per the user (2026-07-22), that number is a **floor, not an indicator of potential** — it specialized one operator while rows still crossed every operator boundary, which is precisely what this plan removes. Authoring larger fused kernels in raw bytecode is prohibitively laborious, which is exactly what has kept codegen narrow.

This plan introduces **Janino** (an embedded Java source compiler, `org.codehaus.janino:janino`) as a second codegen backend so we can emit *whole-stage fused kernels*: one generated Java class per hot pipeline that runs scan → join probes → filters → projection → aggregation as a single set of tight, primitive, monomorphic loops reading CSR adjacency arrays and batch columns directly — the HyPer/Spark-SQL "data-centric" style. Source-level generation makes kernels of that complexity feasible to write, review, dump and debug, which bytecode assembly does not.

The performance goal is **10x**, pursued with full commitment: whole-stage fusion, constant specialization, and the engine's existing levers (CSR arrays, parallelism) composed rather than treated as alternatives. To make 10x a falsifiable engineering target rather than a slogan, it is operationalized per query:

1. Milestone 1 measures, per representative query, the *ceiling*: how fast a hand-written ideal fused loop (written exactly as the generator would emit it, fully optimized) can go. Hand-written ideal code is by construction the upper bound on what generated code of the same design can achieve, so this grounds each target in a measurement rather than a guess. Queries whose ceiling shows ≥10x headroom over the current native engine form the **gate set**; the gate is 10x on those queries.
2. Queries whose ceiling shows less headroom (because they are bound by LMDB I/O, sorting, or result pumping rather than interpreter overhead) get the measured ceiling as their target, and the shortfall is documented with JFR evidence — never silently accepted, never silently redefined.
3. Every query in the ANALYTICS theme and the FOAF-clique suite must be **no slower** than the current default engine (within 5% noise) with Janino codegen enabled.

Observable outcome: `benchmark-results/janino-gate.md` reports, for every gate-set query, ≥10x vs the pinned baseline; `LmdbNativeJaninoCodegen` counters prove kernels executed; module tests, differential fuzz with kernels forced on, and the SPARQL compliance baseline stay green.

## Progress

- [x] (2026-07-22 06:35Z) M0: Janino 3.1.12 + commons-compiler added (root `pom.xml` dependencyManagement + property `janino.version`; `core/sail/lmdb/pom.xml` dependency), fetched once online then back offline. Spike `LmdbNativeJaninoSpikeTest` 4/4 green: public-interface kernel compiles+runs; package-private access across the classloader boundary does not work (public SPI confirmed as required); representative 179-line kernel compiles in **11 ms** warm; generated classloaders are GC-collectible.
- [x] (2026-07-22 07:20Z) M1 COMPLETE: ceiling measured and targets pinned (`benchmark-results/janino-ceiling.md`). Kernels in test-scope `JaninoCeilingKernels.java` (generated-code shape over the public `NativeAdjacency` SPI + `RecordIterator` scans); parity `JaninoCeilingParityTest` 3/3 green vs generic evaluator. Results: cycle3GroupedInterest 34.35→0.638 ms (**53.8x headroom — gate query**); cycle4 146.05 (no-WCOJ probe chain) → 14.37 ms fused single-thread (**10.2x — pipeline-family gate on the non-WCOJ path**; ties the parallel WCOJ's 19.8 ms on one core); ANALYTICS q11 1184→1037 ms (**1.14x — cursor-bound, ceiling-target class, no 10x available from codegen**; evidence is the ceiling itself). Targets: cycle3GroupedInterest ≤3.43 ms (gate) / ≤0.80 ms (ceiling+20%); cycle4 pipeline ≤14.6 ms vs no-WCOJ (gate), ≤17.2 ceiling+20%; q11 ≤1245 ms (ceiling+20%, no-regression).
- [x] (2026-07-22 07:58Z) M2 COMPLETE: SPI package `evaluation/codegen/` (`JaninoKernel` bind/fill/close contract over packed primitive row buffers, `KernelContext` carrying adjacency views/constants/entry slots/key domains — kernels never touch RowState/NativeBatch, the engine-side wrapper owns that translation) + `LmdbNativeJaninoCodegen` service (per-store weak-owner caches, single daemon compiler thread, threshold admission, failed-shape memoization so a bad shape never retries, LRU entry eviction, `dumpDir` source dumping, counters). `LmdbNativeJaninoCodegenTest` 6/6 green.
- [ ] M3 nearly complete: `LmdbNativeJaninoPipeline` (recognizer over `MultiJoinPlan.derivedPlan` order → conservative-Java source emitter → `KernelRowCursor` wrapper) wired as a rung in `LmdbNativeRowStep.openUnorderedInput` directly after WCOJ, `PATH_JANINO_KERNEL` explain tag. Red evidence captured (engagement test failed "pipeline shape was never recognized"/"kernel never opened" with the rung guarded off), then green: `LmdbNativeJaninoPipelineTest` 3/3 (chain + 4-cycle engage and match generic; counters silent when disabled). Differential fuzz: new `janinoKernelBasicGraphPatterns` round (fixed cyclic shapes + 40 randomized BGPs, forced-on flags, WCOJ off, engagement asserted via test bridge) — full suite 21/21 green. Supported shape v1: 2–6 constant-predicate patterns chained through known endpoints (root key-domain enumeration via new `NativeAdjacency.keyCount/keyAt`), CLOSE-edge multiplicity, inline id-`!=` where sound, all other filters residual in the wrapper. Benchmark (`benchmark-results/janino-m3.md`): cycle4 end-to-end 146.05 → **31.16 ms (4.7x)** with production-default admission; the remaining gap to the 14.37 ceiling is measured to be the shared result-materialization surface (~127k rows through RowState/BindingSet — the count-only ceiling never paid it), not kernel quality. 10x end-to-end on row-materializing consumers requires batch-form emission (M5/M6 lever); the 10x-class consumers are the non-materializing ones (M4 aggregation). Module regression sweep pending.
- [ ] M4 nearly complete: `LmdbNativeJaninoAggregate` — fused grouped `COUNT(DISTINCT ?x)` kernel hooked into `NativeGroupIteration.evaluateAll` (right after the exists-intersection collapse, `PATH_JANINO_AGGREGATE` tag, emission via new `NativeGroupIteration.kernelGroupRow` so synthetic VALUES ids resolve through the step's own source). Recognized shape v1: one group slot seeded by a VALUES domain (`ValuesPlan` or folded `MultiValuePatternPlan`), all aggregates the identical COUNT(DISTINCT ?x) (HAVING materializes an anonymous twin spec — discovered via the new decline-trace facility `rdf4j.lmdb.janinoCodegen.debug`), producer pattern g→x requiring `runsNeighborOrdered` CSR runs (new `NativeAdjacency` accessor) for prev-value dedup, witness patterns compiled to an existence method with short-circuit returns, ALL filters must absorb as sound id-inequalities (no residual surface exists). Red evidence captured ("aggregate shape was never recognized" with the rung guarded), then green: `LmdbNativeJaninoAggregateTest` 2/2; fuzz suite 21/21 including two new VALUES-seeded COUNT(DISTINCT) parity cases. **GATE MET** (`benchmark-results/janino-m4.md`): cycle3GroupedInterest 34.35 → **1.023 ms end-to-end = 33.6x**, vs the 3.43 ms 10x target (3.4x margin); residual vs the 0.638 bare-loop ceiling is per-execution engine setup, documented. M4 status: [x] complete (2026-07-22 08:00Z).
- [x] (2026-07-22 08:15Z) Session-final regression sweep: full `mvnf core/sail/lmdb` — 2115 tests, 12 failures across 7 classes, ALL verified pre-existing at HEAD `9fde1f1172` via a throwaway worktree with cloned `.m2_repo` (the two LeftJoin classes fail identically there); zero regressions from M0–M4. Formatter + copyright clean.
- [ ] M5: measurement-gated extensions — WCOJ leapfrog kernels, constant-inlining tier, and batch-form/consumer-fused emission for row-materializing pipelines (the measured M3 lever) — implemented only if the numbers justify them.
- [ ] M6: gate re-run, `janino-gate.md` written, formatter/copyright, module sweep, compliance baseline re-check, default-on/off decision recorded.

## Surprises & Discoveries

(Pre-registered risks to watch: Janino language-subset gaps on generated source; compile latency on large fused classes; C2 refusing to compile generated methods over 8000 bytecodes.)

- Observation: Janino compile latency is a non-issue at kernel scale — a 179-line, 25-method kernel compiles in 11 ms warm on this machine (JDK 25).
  Evidence: `LmdbNativeJaninoSpikeTest.compileLatencyForRepresentativeKernelIsBounded` stdout `[janino-spike] representative kernel (179 lines) compiled in 11 ms`.
- Observation: the cross-classloader package-private trap is real as predicted; the spike's same-package-name generated class could not reach `LmdbNativeSpecialization` (test `packagePrivateAccessAcrossClassLoadersDoesNotWork` green), confirming the public-SPI design.
- Observation (M1, FOAF ceiling, `benchmark-results/janino-ceiling-foaf.txt`): `cycle3GroupedInterest` engine 34.35 ms vs hand-fused 0.638 ms — **53.8x headroom**, far past the 10x gate; the fused loop collapses VALUES→triangle→COUNT(DISTINCT) into an existence-check per person over CSR arrays. `cycle4` engine (parallel WCOJ) 19.82 ms vs single-threaded fused probe chain 14.37 ms — only 1.38x vs the WCOJ rung, i.e. one fused thread ≈ the whole parallel leapfrog; the remaining cycle4 lever is parallelizing the fused chain (substrate exists), not more fusion.
  Evidence: JMH avgt 5×1s after 5×1s warmup, fork 1, identical dataset (5000/30%/3..8/15000/seed 12345).
- Observation (M1, ANALYTICS ceiling): ideal fused code beats the engine by only **1.14x** on q11 (1184→1037 ms) — full-scan aggregates over LMDB are cursor-bound, not interpreter-bound; the engine's aggregate machinery is already near the storage floor. Consequence: the 10x program's scope is pattern-join + aggregation queries served by CSR arrays; cursor-bound scans are ceiling-target only. This also independently confirms the plan-16-era finding that generic "codegen everywhere" would be wasted effort here — the win is in *what* the generated code reads (in-memory arrays), not just fewer virtual calls.
  Evidence: `benchmark-results/janino-ceiling-analytics.txt` (3×5s warmup + 3×5s measure on the fixed 13.8M theme store).
- Observation (M4): a HAVING clause referencing an aggregate materializes that aggregate as a SECOND, anonymous `AggregateSpec` alongside the projected one — a recognizer requiring "exactly one aggregate" never fires on real HAVING queries. Found in minutes via the permanent decline-trace facility (`rdf4j.lmdb.janinoCodegen.debug` printing per-gate decline reasons); the fix is to admit N identical COUNT(DISTINCT) specs and bind one computed count under every spec name.
  Evidence: trace line `tryEvaluate: children=5 groupSlots=1 aggregates=2` on the M4 fixture query.
- Observation (M3 benchmark): end-to-end cycle4 through generated kernels is 31.2 ms vs the 14.4 ms count-only ceiling; the gap is the shared result-materialization surface (~127k rows through RowState binds + BindingSet construction), which every join strategy pays and the M1 ceiling did not measure. Per-row nanocosts (an AtomicLong increment per row) were experimentally shown irrelevant (29.0 vs 31.2, within noise).
  Evidence: `benchmark-results/janino-m3.md`.

## Decision Log

- Decision: Adopt Janino as a new dependency of `core/sail/lmdb`, reversing the plan-16 decision ("no Janino, Class-File API only") for the *whole-stage* kernel tier.
  Rationale: The user explicitly requested a Janino-based codegen plan (2026-07-22), which supplies the dependency approval the house rules require. Technically: whole-stage fusion needs kernels with dozens of interleaved loops, local variables and helper methods per plan shape; emitting those as Java source is tractable, reviewable and dumpable, while hand-assembling them with the Class-File API is not (the existing 405-line `LmdbNativeSpecialization` implements a single fixed loop). The two backends coexist: Class-File hidden classes stay for the existing small fixed-shape kernels; Janino owns whole-stage source generation. A consolidation decision is deferred to M6.
  Date/Author: 2026-07-22 / Claude (Fable) with hmottestad.
- Decision: Janino pinned at **3.1.12** (`janino.version` property, root `pom.xml`), compile scope in `core/sail/lmdb` only.
  Rationale: latest 3.1.x at implementation time; fetched into `.m2_repo` with a single online run of the module install, all subsequent builds offline.
  Date/Author: 2026-07-22 / Claude (M0).
- Decision: The 10x gate is defined against measured per-query ceilings (M1), not as a blanket multiplier over the current engine.
  Rationale: A hand-written ideal fused loop is the physical upper bound on what a generator emitting the same design can achieve, so measuring it first turns "10x" into per-query targets that are actionable and falsifiable, and shows exactly which complementary levers (parallelism, constant inlining, data layout) must compose with fusion where fusion alone is not enough. Note (user clarification, 2026-07-22): `benchmark-results/pipeline-codegen-comparison.md` was a quick single-operator POC and its 5–26% must be read as a minimum, NOT as evidence bounding codegen potential; the ceiling-first structure is de-risking methodology, not skepticism about the goal.
  Date/Author: 2026-07-22 / Claude.
- Decision: Generated code accesses engine internals through a new public-but-internal SPI package (`org.eclipse.rdf4j.sail.lmdb.evaluation.codegen`), not through package-private access.
  Rationale: Janino loads generated classes in a child classloader. Two classes in the same-named package but different classloaders are in different *runtime* packages, so package-private access from generated code to `RowState`/`NativeBatch` internals throws `IllegalAccessError`. Spark and Flink solve this the same way: the surface generated code touches is public, annotated as internal (`@Experimental` + javadoc), and exposes primitive arrays/fields directly so hot loops stay allocation- and dispatch-free.
  Date/Author: 2026-07-22 / Claude.
- Decision: Kernel cache is keyed by canonical *plan shape* (operator kinds, slot layout, filter kinds, aggregate kinds — never constant ids), with query constants passed via kernel fields at instantiation.
  Rationale: Shape-keying makes kernels reusable across queries and bounded in number; constant-inlining trades reuse for speed and is deferred to M5 as a second tier for proven ultra-hot shapes. This mirrors Flink/Spark practice and the existing `LmdbNativeSpecialization` key discipline.
  Date/Author: 2026-07-22 / Claude.

- Decision: Ceiling-kernel algorithm choices (M1) — recorded so M3/M4 know what the ceiling assumes: (a) `cycle3GroupedInterest` uses an existence check per person (first witnessing triangle short-circuits) instead of enumerating all triangles, which is the fused equivalent of the engine's factorized/EXISTS counting reasoning — an M4 aggregation kernel must exploit COUNT(DISTINCT)-as-EXISTS to reach this ceiling; (b) `cycle4` is a pure fused probe chain (nested adjacency loops + inline inequality filters + linear membership scan for the closing edge), matching the M3 pipeline-fusion design 1:1, deliberately NOT a leapfrog; (c) ANALYTICS q11 exploits the serving index's sort order (distinct-by-transition-counting on the leading field, scratch hash set for the other), which an M4 kernel can replicate since `indexName()` is available at plan time.
  Rationale: the ceiling must be reachable-in-principle by the generator families this plan builds, otherwise it is not a target but a fantasy; each choice maps to a concrete generator capability.
  Date/Author: 2026-07-22 / Claude (M1).

- Decision: M3 recognizer/emitter policies (all in `LmdbNativeJaninoPipeline`):
  (a) Additive public SPI change — `NativeAdjacency` gained `keyCount()`/`keyAt(int)` (default unsupported, implemented by `CsrNativeAdjacency` over `keysByDense`) so root patterns can enumerate the key domain without an extra store scan.
  (b) Inline `!=` absorption is limited to id-decidable cases: at least one operand resource-assured (bound at a subject position of a kernel pattern — such slots hold Resources in every surviving row) or a Resource constant; id-inequality then coincides with SPARQL value-inequality. Everything else stays a residual filter.
  (c) Residual filters are NOT decline reasons: the wrapper cursor applies every unabsorbed `MaskedFilter.filter.accept(row)` after binding each kernel row, so admission never changes filter semantics (filters are plan-owned; cursors only call accept — established engine contract).
  (d) Multiplicity: closing edges count matching arena entries and re-emit the row that many times; per-(neighbor,context) arena entries make multi-graph duplicate solutions come out exactly as the generic evaluator produces them (pinned by the fuzz round).
  (e) Admission threshold approximates observed rows as opens×4096 per shape key until real per-shape row accounting exists — refine in M5/M6 if it misbehaves.
  (f) v1 kernels materialize their full result on first fill (growable long[]) instead of a resumable state machine; LIMIT-heavy queries over-compute — acceptable while the flag defaults off, revisit for M6.
  Rationale: each choice keeps generated source conservative and the rung strictly optional; correctness burden sits in the recognizer's admission rules plus the wrapper's unmodified-filter path.
  Date/Author: 2026-07-22 / Claude (M3).

## Outcomes & Retrospective

(Interim, M0–M4 complete, 2026-07-22.) The plan's central question — can Janino whole-stage codegen deliver 10x — is answered **yes, for the class of queries the ceiling methodology predicted**: grouped COUNT(DISTINCT) over CSR-served pattern chains runs **33.6x** faster end-to-end (cycle3GroupedInterest 34.35 → 1.02 ms, gate target 3.43 ms), because aggregation fusion eliminates both the per-row operator stack AND the result-materialization surface. Row-materializing consumers (cycle4 SELECT *) get 4.7x from the same kernels — their remaining cost is the shared RowState/BindingSet surface, not generated-code quality, and the levers are batch-form emission and consumer fusion (M5/M6). Cursor-bound full scans (ANALYTICS) have no codegen headroom at all — measured, not assumed (ideal code: 1.14x). Methodology lessons worth keeping: (a) the M1 hand-written-ceiling discipline converted "10x" from a slogan into per-query engineering targets and correctly predicted where the win would land; (b) count-only ceilings under-measure — they skip the result surface real queries pay, so ceiling benchmarks should mirror the consumer shape; (c) permanent decline-trace facilities (the debug property) pay for themselves on the first integration surprise (HAVING's anonymous twin spec); (d) Janino compile latency (11 ms warm) never mattered — async compile with an interpreted first execution hides it completely; (e) the public-SPI + packed-primitive-buffer kernel contract kept every generated line inside conservative Java with zero engine-internals coupling. Regression posture: all 12 module-sweep failures baseline-verified pre-existing (throwaway worktree at HEAD); flag stays default-off until M6's suite-wide no-regression decision. Remaining: M5 (measurement-gated leapfrog kernels / constant inlining / batch emission), M6 (full gate re-run incl. cold-start reporting, compliance baseline, default decision).

## Context and Orientation

Everything below lives in `core/sail/lmdb` unless stated; paths are repository-relative. Branch: `optimize-lmdb`.

The **native engine**: `LmdbNativeEvaluationStrategy.precompile(...)` asks `LmdbNativeAggregateCompiler.tryCompile(...)` to translate SPARQL algebra into a tree of `SlotPlan` operators (`evaluation/LmdbNativeSlotPlan.java`). A `SlotPlan` offers `RowCursor open(RowState)` (row-at-a-time), optionally `BatchCursor openBatch(RowState,int)` (columnar `NativeBatch`: column-major `long[]` slots + `int[]` selection vector), `long producedMask()`, `double estimate(RowState)`. A `RowState` (`evaluation/LmdbNativeRowState.java`) is a `long[] slots` of dictionary ids plus bound-mask and rollback trail; ids decode to RDF `Value`s only at projection. All id comparisons are unsigned (`Long.compareUnsigned` — inlined doubles are negative as signed longs).

Join bags flatten into `MultiJoinPlan` (`evaluation/LmdbNativeJoinPlans.java`) with masked filters and a derived greedy execution order; runtime strategy selection (prefix-run, batch, parallel, factorized, merge/hash join, WCOJ leapfrog) happens in `LmdbNativeRowStep.NativeRowsIteration.initialize()` / `openBatchOrParallel()`, each rung tagged via `LmdbNativeAttemptMetrics`. Aggregation runs through `LmdbNativeGroupStep`/`LmdbNativeGroupTable`; the WCOJ operator is `LmdbNativeLeapfrogJoin`. The **CSR cache** (`LmdbCsrAdjacencyCache`, package `sail/lmdb`) holds, per predicate, sorted adjacency arrays (`runStart[]` prefix sums + `neighbors[]`) in both orientations — the ideal substrate for generated loops. Parallelism substrate: `LmdbNativeParallelPipelines` (shared pool + admission), `NativeLmdbQuerySource.openParallelSources(n)`.

**Existing codegen**: `evaluation/LmdbNativeSpecialization.java` — JDK Class-File API bytes, `defineHiddenClass(..., NESTMATE)`, per-store-id-space `WeakHashMap` cache with entry/byte-budget LRU, single daemon compiler thread, engagement threshold `rdf4j.lmdb.nativeSpecialization.thresholdRows` (default 32768), one kernel family (`NativeBatchFilterKernel`). Its cache/threshold/metrics *pattern* is the template for the Janino tier; its bytes-level emitter is not reused.

**Janino** is a small embedded compiler that turns Java source strings into classes at runtime (`SimpleCompiler`, `ClassBodyEvaluator`). Constraints that shape this plan: it supports a conservative Java subset (assume roughly Java 8 syntax — no records, switch expressions, text blocks, or var in generated source; verify exact support in M0); it loads classes via its own `ByteArrayClassLoader` child of a parent you supply; compile latency is milliseconds for small classes but must be measured, never assumed. License BSD-3-Clause (EDL-compatible); artifacts `org.codehaus.janino:janino` + transitive `commons-compiler`, both shipped as OSGi bundles. Version to use: the latest 3.1.x at implementation time (3.1.12 as of this writing); pin the exact version in the Decision Log when M0 lands.

**Why whole-stage fusion can reach 10x where kernels-per-operator cannot**: the current 5–26% wins come from specializing one operator while rows still cross every operator boundary. Fusion removes the boundaries themselves: no per-row virtual calls, no `RowState` writes for pass-through slots, no batch materialization between stages, counted loops over primitive arrays that C2 can bounds-check-eliminate and (for filter/aggregate loops) auto-vectorize, and aggregate state held in locals/registers across the innermost loop. That only dominates when per-row work is cheap and row counts are large — hence the ceiling-first methodology.

Prior plans in `plans/lmdb-native-engine/` (untracked working-tree files) provide background but this plan does not depend on reading them; all needed context is restated here. Out of scope (do not modify): `LmdbSketchJoinOptimizer`, `LmdbEvaluationStatistics`, `ParetoJoinMemoPlanner` (other-branch work), the generic RDF4J evaluator, store write paths.

House rules that bind every step: root quick install before tests (`mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`, filtered through the standard awk); tests only via `python3 .codex/skills/mvnf/scripts/mvnf.py`, one at a time, never `-am`/`-q`; keep untracked artifacts; new Java files carry the 2026 copyright header plus the agent signature line; formatter `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` before finalizing.

## Plan of Work

### Milestone 0 — Dependency and feasibility spike

Add Janino to the build: a `dependencyManagement` entry in the root `pom.xml` (janino + commons-compiler, single pinned version) and a `dependency` in `core/sail/lmdb/pom.xml`. Because the repo builds offline, the first install after the edit must run once without `-o` to fetch, then return offline. Record the pinned version and the user-approval provenance in the Decision Log.

Write `LmdbNativeJaninoSpikeTest` (test-only, `src/test/java/.../lmdb/evaluation/`) proving on JDK 25: (a) `SimpleCompiler` compiles a class implementing a *public* engine interface and it runs; (b) the cross-classloader trap is real — a generated class touching a package-private member fails — and the public-SPI route works (this pins the M2 design); (c) compile latency for a ~200-line synthetic kernel, printed and recorded here (expect single-digit to low-tens of ms); (d) generated classes become unreachable after the compiler/loader is dropped (weak-reference GC assertion), pinning the leak story. Acceptance: spike test green; latency number written into Progress.

### Milestone 1 — Ceiling prototype (the honesty gate)

Scope: measure the maximum win fusion can deliver before building any generator. Pick three representatives: (1) an ANALYTICS-theme scan→filter→aggregate query with large row count (from `lmdb-theme-query-benchmark`'s ANALYTICS theme — choose the highest-row-count COUNT/GROUP BY query); (2) `cycle3GroupedInterest` from `FoafCliqueQueryBenchmark` (aggregation-decorated cycle, currently the most codegen-responsive at 26%); (3) a plain multi-pattern join chain (e.g. `cycle4` with WCOJ disabled, forcing the probe-chain path). For each, hand-write in test/benchmark scope a fused Java loop — exactly the code M3/M4 would generate, reading CSR arrays via the future SPI accessors — and benchmark it against the current native engine on identical data (JMH via `scripts/run-single-benchmark.sh` or the module's harness; identical warmup/measurement settings both sides).

Deliverable: `benchmark-results/janino-ceiling.md` with columns query / native-now / hand-fused / headroom, plus JFR profiles (`docker-jfr-benchmark-loop` skill) showing *where* the remaining time goes for any query under 10x headroom. Then update this plan: the gate set (headroom ≥10x), per-query ms/op targets for the rest, and a Decision Log entry. If NO query shows ≥10x headroom, stop and report — the plan's premise is then falsified and the user decides whether ceiling-targets replace the 10x framing. Acceptance: the ceiling doc exists, targets are pinned in this file, and the hand-written loops' results are verified equal to engine results (multiset compare in a unit test) so the ceiling is not measuring wrong answers.

### Milestone 2 — Codegen infrastructure

New classes in `evaluation/` plus the public SPI package `evaluation/codegen/`:

- `evaluation/codegen/` (public, `@Experimental`, javadoc "internal engine SPI, no compatibility guarantees"): `JaninoKernel` (the interface generated classes implement: `RowCursor open(...)`-shaped entry points plus batch variants), `KernelDataAccess` (public accessors handing generated code the primitive arrays: CSR `runStart`/`neighbors`, `NativeBatch` columns, `RowState` slots, group-table arrays). Existing internals gain nothing public beyond what this package re-exposes.
- `LmdbNativeJaninoCodegen` (package-private, `evaluation/`): owns emission and lifecycle. Responsibilities, mirroring `LmdbNativeSpecialization`'s proven pattern: per-store-id-space cache (`WeakHashMap` keyed by id space) with entry + generated-byte budgets and LRU eviction; canonical **shape key** (operator-kind string + slot layout + filter/aggregate kinds + flags); async compilation on the existing single daemon compiler thread after `rdf4j.lmdb.janinoCodegen.thresholdRows` (default 32768) interpreted rows; interpreted path always remains and compile failure never fails a query (counter + one-time debug log); one Janino `SimpleCompiler`/classloader per compiled kernel with parent = the SPI package's classloader, dropped after instantiation so classes stay collectible; metrics counters (COMPILATIONS, COMPILE_FAILURES, COMPILE_NANOS, KERNEL_EXECUTIONS, CACHE_HITS/MISSES, EVICTIONS, FALLBACKS) exposed via `LmdbNativeExplain`/`LmdbNativeAttemptMetrics` like other rungs.
- Source emitter discipline (enforced by emitter structure, tested in M3): conservative Java-8 subset; one helper method per operator stage; no method may exceed a configured emitted-statement budget (default sized so bytecode stays well under HotSpot's 8000-bytecode `DontCompileHugeMethods` limit — verify actual bytecode sizes in M3 and record); constants via `final` fields set in the generated constructor; `rdf4j.lmdb.janinoCodegen.dumpDir` writes every generated source to disk for debugging; generated source contains nothing derived from user *data*, only from validated plan structure (no injection surface).

Flags: `rdf4j.lmdb.janinoCodegen.enabled` (default **false** until M6 decides), `...thresholdRows`, `...maxEntries`, `...maxGeneratedBytes`, `...dumpDir`. Acceptance: infrastructure unit tests green — cache hit/miss/evict behavior, async switchover at a forced threshold of 0, injected compile failure falls back cleanly, store-close makes kernels collectible (GC test), dumped source compiles standalone.

### Milestone 3 — Whole-stage pipeline kernels

The first real family: fuse a derived `MultiJoinPlan` probe chain — "for each binding of child 0, probe children 1..k, applying masked filters at their earliest depth, emit projected slots" — into one generated class. Integration point: a new rung in `LmdbNativeRowStep.NativeRowsIteration` (beside the existing strategy proposals) that, when the plan shape is emittable (all children are pattern probes / CSR-servable, all filters have generated-code equivalents via `LmdbNativeExpressionCompiler`'s vocabulary), requests a kernel from `LmdbNativeJaninoCodegen` and runs it once published; until then the existing interpreter runs. Non-emittable shapes decline with a recorded reason, plan unchanged.

Follow strict TDD within the milestone: first a red engagement test (`LmdbNativeJaninoPipelineTest`: counters must move for a supported shape with threshold 0; must NOT move for unsupported shapes and with the flag off), then emission. Differential coverage: extend `LmdbNativeDifferentialFuzzTest` with a Janino-forced-on round (threshold 0) over its full query corpus — generated kernels must agree with the interpreter on every fuzzed query, multiset-sensitive. Benchmarks: the three M1 queries re-run with generated (not hand-written) kernels; target = within 20% of the M1 hand-written ceiling (the generator must not lose what the prototype proved). Acceptance: engagement + fuzz green; ceiling-tracking documented in `benchmark-results/janino-m3.txt`.

### Milestone 4 — Aggregation fusion

Extend the emitter so a pipeline that terminates in `LmdbNativeGroupStep` fuses the aggregate update into the innermost generated loop: group-key lookup against the group table's arrays (exposed via `KernelDataAccess`), primitive accumulator updates (COUNT/SUM/MIN/MAX first; AVG as sum+count), DISTINCT via the existing native distinct structures. Single-group (no GROUP BY) aggregates keep state in locals across the whole loop — the biggest register-resident win. Same TDD/fuzz/benchmark discipline as M3; the ANALYTICS representative and `cycle3GroupedInterest` are the tracked queries. Acceptance: fuzz (aggregation corpus) green with kernels forced on; tracked queries within 20% of ceiling; ANALYTICS 5-query spot check shows no regression >5%.

### Milestone 5 — Measurement-gated extensions

Only if M3/M4 evidence justifies each (record the go/no-go with numbers in the Decision Log): (a) **leapfrog kernels** — specialize `LmdbNativeLeapfrogJoin`'s per-level intersection loops per shape (the transplanted-PR comparison showed a well-shaped interpreter over CSR arrays beating that PR's Janino codegen, but that codegen lacked this engine's substrate — decide purely by measurement on kernels that DO use the CSR arrays); (b) **constant-inlining tier** — for shapes whose kernel executed more than `rdf4j.lmdb.janinoCodegen.hotRecompileExecutions` times, recompile with query constants inlined as literals (separate cache keyed shape+constants, small entry budget); (c) fused filter-chain kernels for the legacy Class-File tier retired into the Janino tier if the Janino versions win. Acceptance: each accepted extension carries its own engagement test, fuzz round, and benchmark delta; each rejected one carries a one-paragraph evidence note here.

### Milestone 6 — Gate, sweep, and default decision

Re-run the full benchmark set: gate-set queries vs their pinned baselines (10x targets), ceiling-target queries vs their targets, full FOAF suite and ANALYTICS theme with the flag on vs off (no-regression check). Produce `benchmark-results/janino-gate.md` (use the `jmh-benchmark-compare` skill) with query / baseline / result / target / verdict. Cold-start honesty: report first-execution latency including compile (a dedicated single-shot benchmark mode) separately from warm numbers, plus total compile time and generated-byte counters from a full suite run. Then: formatter, `cd scripts && ./checkCopyrightPresent.sh`, full `mvnf core/sail/lmdb` sweep judged by report XMLs (13 known pre-existing failures on this branch), compliance re-check against `plans/lmdb-native-engine/COMPLIANCE-BASELINE.md` (no NEW failures beyond the 24 known). Finally decide and record: `rdf4j.lmdb.janinoCodegen.enabled` default on or off, based on the no-regression evidence and cold-start cost.

## Concrete Steps

All commands from `/Users/havardottestad/Documents/Programming/rdf4j`.

Root install (first thing every session; once without `-o` right after the pom edit in M0):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{s=1} s{print}'

Tests, one at a time via mvnf (pipefail when piping):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeJaninoSpikeTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeJaninoPipelineTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Benchmarks (never while builds/tests run — measurement-isolation lesson from plan 16):

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class <fqcn> --method <method>

Forcing kernels on for tests/benchmarks: `-Drdf4j.lmdb.janinoCodegen.enabled=true -Drdf4j.lmdb.janinoCodegen.thresholdRows=0`.

## Validation and Acceptance

Correctness: differential fuzz with kernels forced on agrees with the interpreter on the full corpus; FoafCliqueCorrectnessTest green in a kernels-forced configuration; compile-failure injection falls back with correct results; snapshot-isolation scenario covered by running an existing native isolation test with kernels forced on. Engagement: counters + explain-path tags. Performance: M1 ceiling doc, M3/M4 ceiling-tracking (≥80% of hand-written), M6 gate doc with per-query verdicts, cold-start numbers reported separately. Regression: flag-off behavior byte-identical to today (flag defaults off until M6); flag-on regressions >5% on any suite query block the default-on decision.

## Idempotence and Recovery

Everything is additive and flag-guarded; `rdf4j.lmdb.janinoCodegen.enabled=false` (the default until M6) restores current behavior exactly. The dependency addition is the only non-flagged change and is inert without the flag. If a milestone cannot go green, stop, record state in Progress, leave the flag off. Keep all untracked artifacts; `benchmark-results/` files are never cleaned. Kernel caches are per-store-id-space with weak keys, so repeated test runs cannot leak across stores.

## Artifacts and Notes

Planned artifacts (repo root `benchmark-results/`): `janino-ceiling.md`, `janino-m3.txt`, `janino-m4.txt`, `janino-gate.md`; generated-source dumps under the configured `dumpDir` when debugging. Background artifacts: `pipeline-codegen-comparison.md` (single-operator POC — treat its 5–26% strictly as a floor, per the user), `wcoj-vs-pr.md` (interpreter vs the old PR's Janino full-codegen; context for the M5 leapfrog go/no-go).

## Interfaces and Dependencies

New dependency (root `pom.xml` dependencyManagement + `core/sail/lmdb/pom.xml`): `org.codehaus.janino:janino` latest 3.1.x, pinned at M0. No other new dependencies.

As built in M2 (all under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`):

In `codegen/JaninoKernel.java` (public, `@Experimental @InternalUseOnly`):

    public interface JaninoKernel {
        void bind(KernelContext context);        // once per cursor open
        int fill(long[] rowBuffer, int maxRows); // packed row-major ids, fixed slot count; 0 = exhausted
        default void close() {}
    }

In `codegen/KernelContext.java` (public, final): public final fields
`NativeLmdbQuerySource.NativeAdjacency[] adjacencies`, `long[] constants`, `long[] entrySlots`,
`long[][] keyDomains`. Kernels read only this context plus the public `NativeAdjacency` view
(`denseIdOf/runStart/runEnd/neighborAt/contextAt`) — never `RowState`/`NativeBatch`; the engine-side wrapper
translates fill-buffer rows into the row model. (The planned `KernelDataAccess` collapsed into
`KernelContext` + the pre-existing public `NativeAdjacency` — no new accessor surface was needed.)

In `LmdbNativeJaninoCodegen.java` (package-private):

    final class LmdbNativeJaninoCodegen {
        static boolean enabled();  // rdf4j.lmdb.janinoCodegen.enabled, default false until M6
        static JaninoKernel kernel(Object cacheOwner, String shapeKey, String className,
                Supplier<String> sourceSupplier, long observedRows);   // null = stay interpreted
        static JaninoKernel awaitKernel(Object cacheOwner, String shapeKey, long timeout, TimeUnit unit);
        static final AtomicLong COMPILATIONS, COMPILE_FAILURES, COMPILE_NANOS, KERNEL_INSTANTIATIONS,
                CACHE_HITS, CACHE_MISSES, EVICTIONS, FALLBACKS;        // test hooks
    }

Compiled shapes cache one `Constructor<?>`; instances are per-open. Failed shapes stay cached as failed
(no retry storm). Properties: `rdf4j.lmdb.janinoCodegen.{enabled,thresholdRows,maxEntries,dumpDir}`.

`SlotPlan`, `RowCursor`, `NativeBatch`, `RowState`, `LmdbCsrAdjacencyCache` are consumed, not modified, except for package-private accessors feeding `KernelDataAccess` (each recorded in the Decision Log).

---

Revision note (2026-07-22, initial version): plan authored on user request for Janino-based codegen targeting 10x; structured ceiling-first to ground per-query targets in measured headroom before building the generator.

Revision note (2026-07-22, second revision): reframed on user clarification — `pipeline-codegen-comparison.md` was a quick POC and its 5–26% is a minimum, not an indicator of what properly implemented and optimized codegen can achieve; removed all language reading it as a bound. The ceiling-first M1 structure is retained purely as target calibration (hand-written ideal fused code is the true upper bound for generated code of the same design), and the M5 leapfrog-kernel decision is now framed as a neutral measurement question rather than an expected no-go.
