# Kernel capability parity: vectorized + factorized execution, LMDB-direct sources, ordering, skip/prefix scans, and inlined literals

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (repository root).

Predecessors, all checked in under `plans/lmdb-native-engine/` and incorporated by reference: `17-janino-whole-stage-codegen.md` (the compile service and the two shape-specific code generators), `18-janino-pattern-analysis.md` (which query shapes are worth generating code for), `19-kernel-ir-primitives.md` (the kernel intermediate representation and its generic emitter), `20-kernel-lowering-row.md` (the row-side lowering substrate), `21-kernel-lowering-aggregate.md` (the aggregate-side rung with EXISTS/MINUS witnesses), `22-kernel-lowering-optional-union-path.md` (OPTIONAL, and the in-flight UNION/BIND/path work). Everything a reader needs from those plans is restated below; you do not need to read them first.

## Purpose / Big Picture

Eclipse RDF4J's LMDB store has a "native query engine": a SPARQL execution engine that works directly on 64-bit dictionary-encoded identifiers instead of Java `Value` objects. Inside that engine there is a *whole-stage code generator* — at runtime it writes Java source for a query shape, compiles it in-process with the Janino compiler, and runs the resulting class. A generated class is called a **kernel**. Where kernels engage today they are dramatic: one benchmark query dropped from 5436 ms to about 43 ms.

The problem this plan solves is that kernels are a strict subset of what the surrounding interpreted engine can already do, along three independent axes.

First, **kernels can only read one kind of data structure**. Every data-producing operation in the kernel intermediate representation reads a "CSR adjacency view" — an in-memory, compressed, sorted adjacency list built per `(predicate, direction)` pair. Anything that view cannot express (a variable predicate, a named graph, a dataset restriction, a repeated variable inside one triple pattern, an index-ordered scan) makes the whole kernel decline and the query falls back to the interpreted operators. Worse, three entire store configurations get no kernel at all regardless of the query: stores with inferencing enabled (the composite source never offers adjacency views), any query running inside a write transaction (the adjacency cache is bypassed so readers see their own writes), and untracked datasets.

Second, **kernels execute one tuple at a time and copy every result row three times**. Columns are scalar `long` fields, the pipeline is a nested-loop continuation chain, and every produced row is written to a scratch array, copied element-by-element into a growing output array, and then block-copied into the caller's buffer. There is no vectorization and no factorization, even though the interpreted engine right next door has batch (column-major) execution, morsel-driven parallelism, factorized joins, and an arena allocator.

Third, **some SPARQL constructs never reach a kernel at all**, because the planner that sits above the kernel tier declines them first — most importantly sub-`SELECT`, which currently breaks the whole query root.

After this plan, a reader can take any SPARQL 1.2 query against an LMDB store — including one with named graphs, variable predicates, sub-selects, ordering satisfied by an index, or running against an inferencing store — and observe that the largest provable part of it executes inside a generated, vectorized, factorized kernel that allocates almost nothing per row. Concretely observable: the query explanation reports the kernel execution path, a test-visible counter shows the kernel opened, results are identical to the generic RDF4J evaluator, allocation per output row measured under Java Flight Recorder trends down rather than up, and an audit report enumerates every remaining decline with a written justification.

## Progress

- [x] (2026-07-24) ExecPlan authored; integration facts verified in-code by three exploration passes (CSR-versus-cursor paths, kernel IR and emitter internals, native-engine SPARQL coverage).
- [x] (2026-07-24 ~20:50Z) M1 step 1a sweep: `core/sail/lmdb` = 2191 tests, 4 failures, 0 errors, 3 skipped — exactly the branch's pre-existing baseline (`LmdbNativeFeatureFlagForkTest` 3F, `LmdbNativeLeftJoinFilterRewriteTest` 1F), zero new failures. Independence proved by the diff being 240 insertions and **0 deletions**, and by no `src/main` file other than the definition site referencing the new substrate.
- [x] (2026-07-24 ~20:40Z) M1 step 1a: vector and chunk substrate in `KernelRuntime` — `VECTOR_SIZE` (property `rdf4j.lmdb.janinoCodegen.vectorSize`, default 2048, clamped and rounded to a multiple of 64), shared `IDENTITY` selection, `ChunkState` (size / selection / flatIndex / lazily-allocated owned scratch), and branch-free selection primitives `selectEq`, `selectNe`, `selectRangeUnsigned`, `selectEqColumns`, `selectNeColumns`, `selectBound` (each in a dense and a selection-aware overload) plus `gather`, `broadcast`, `fillIdentity`. `KernelRuntimeTest` 16/16 green (8 pre-existing + 8 new), formatter and copyright green.
- [x] (2026-07-25 ~07:35Z) M1 step 1b: **vector tail** — bulk run accessors `NativeAdjacency.copyRun`/`copyContexts` (interface default loops the per-index accessor; `CsrNativeAdjacency` overrides with `System.arraycopy`), `Kernel.vectorTailIndex` + property `rdf4j.lmdb.janinoCodegen.vectorTail` (default on, folded into the shape key so both modes can coexist in the compiled-kernel cache), and emitter support that rewrites the innermost run expansion into a bulk read plus vectorized selection while leaving every downstream semantic untouched. `LmdbCsrAdjacencyBulkCopyTest` 2/2, `LmdbNativeKernelIrEmitterTest` 29/29 (25 pre-existing, now running *through* the vector tail, plus 4 new: engagement in the emitted source, chained selection with a residual hook guard, runs longer than one vector, and tail-claiming/declining rules), `LmdbNativeDifferentialFuzzTest` 24/24.
- [x] (2026-07-25 ~07:45Z) M1 step 1b sweep: `core/sail/lmdb` = 2197 tests, 4 failures, 0 errors, 3 skipped — the pre-existing baseline (`LmdbNativeFeatureFlagForkTest` 3F, `LmdbNativeLeftJoinFilterRewriteTest` 1F), zero new. Formatter and copyright green.
- [x] (2026-07-25 ~10:35Z) **M1 benchmark gate: no regression, measured.** The blocking JMH process had exited and the lock file was stale (0 bytes, two days old); it was removed only after confirming no JMH process was running. HIGHLY_CONNECTED:10, 1 warmup + 3 measurement iterations, 1 fork, same jar for both arms via `--no-build`: vector tail **on** 3253.010 ± 1387.547 ms/op, **off** 3307.239 ± 1256.150 ms/op. The 1.6% gap is far inside the error bars, so the honest reading is "indistinguishable — no regression", not "1.6% faster". Two caveats stated so the number is not over-read: both arms sit near 3.3 s rather than the ~30 ms this query reaches once engaged, so four iterations never reach steady state; and HC q10's kernel carries an `Exists` after its probe, which `findVectorTail` declines, so the vector tail very likely does not even apply here. This gate therefore proves absence of harm, not presence of speedup.
- [ ] **M1 speedup still undemonstrated**: pick a counting shape the tail actually claims (grouped `COUNT(*)` over a probe — out-degree) and A/B it with enough warmup to reach the engaged steady state. The O(1) unfiltered-count path is the thing worth measuring.
- [x] (2026-07-25 ~10:30Z) Two defects found by self-review and fixed: per-open 24KB scratch allocation, and a wasted whole-run copy in the unfiltered count (see Surprises & Discoveries).
- [ ] ~~M1 benchmark gate BLOCKED (not skipped)~~ (resolved above; original note retained for provenance): the HC q10 A/B could not run — JMH refused with "Another JMH instance might be running. Unable to acquire the JMH lock", and a live JMH process (rdf4j lmdb benchmark classes, `-Xmx16G`) is present from concurrent work on this branch. The lock was deliberately **not** forced and the process **not** killed: doing either would corrupt the other run's measurements. Re-run `scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery --theme-query HIGHLY_CONNECTED:10` (and the ANALYTICS no-regression set) once the machine is quiet. Correctness gates for step 1b are all green independently of this.
- [x] (2026-07-25 ~07:53Z) M1 step 1b remainder, part 1: **`LeftProbe` tails vectorize** — OPTIONAL is pervasive in the corpus, so this materially widens what the tail claims. The null arm is preserved exactly: a non-empty run sets `matched` before any filtering (so trailing filters can empty the result without resurrecting the null row, matching the scalar emitter), and when the key had no run at all the continuation runs once with the value column at `-1` and the trailing filters re-applied as scalar guards — including hook filters, which see the `-1` that means unbound. `LmdbNativeKernelIrEmitterTest` 34/34 (2 new), `LmdbNativeDifferentialFuzzTest` 24/24.
- [x] (2026-07-25 ~10:55Z) **SPARQL compliance gate: PASS** with the vector tail default-on — `LMDB compliance baseline gate: PASS`, 30 reports parsed, required suites 5/5, remaining baseline failures 17/24, resolved 7, **zero new**. The checker fails closed on any failure outside the 24 frozen identities, so this is the W3C SPARQL 1.1 + 1.2 conformance evidence for the change. Note the isolated-workspace invocation in Concrete Steps could not be used: a fresh `--workspace` gets an empty Maven repository, and offline plugin resolution fails against it (`PluginResolutionException`). Run compliance in the default workspace, or populate the isolated one online once.
- [x] (2026-07-25 ~10:40Z) Final sweep after both self-review fixes: `core/sail/lmdb` = 2202 tests, 4 failures, 0 errors, 3 skipped — the pre-existing baseline (`LmdbNativeFeatureFlagForkTest` 3F, `LmdbNativeLeftJoinFilterRewriteTest` 1F), zero new. Formatter and copyright green.
- [x] (2026-07-25 ~08:00Z) `LeftProbe` tail sweep: `core/sail/lmdb` = 2202 tests, 4 failures, 0 errors, 3 skipped — the pre-existing baseline, zero new. Formatter and copyright green.
- [ ] M1 step 1b remainder, part 2: vectorize `Intersect` tails, and the `ScanQuad` tail once M4 lands.
- [~] M1 step 1c **folded into M3**: the zero-copy sink cannot exist before the pipeline can pause and resume, because `run()` produces every row before `fill` knows where the caller wants them. Building a sink abstraction first would produce a layer with nothing to do. See the milestone 3 narrative, which now carries the combined design.
- [ ] M1 step 1d: per-query buffer pool so repeated opens stop allocating.
- [x] (2026-07-25 ~07:46Z) M2 first slice — **factorized counting**: when the terminal is a counting aggregate whose group columns and counted columns are all outer (constant across a run slice) and every trailing filter vectorized, the vector tail folds the whole slice with `updateBy(cnt)` instead of iterating it. `COUNT(DISTINCT outer)` qualifies (adding a value once equals adding it `cnt` times); `COUNT(DISTINCT tail)`, `SUM`, and grouping by the tail column all decline. A `cnt > 0` guard preserves the scalar path's behaviour of never interning a group for a fully filtered slice. `LmdbNativeKernelIrEmitterTest` 32/32 (3 new), `LmdbNativeKernelAggregateTest` 7/7, `LmdbNativeDifferentialFuzzTest` 24/24.
- [x] (2026-07-25 ~07:55Z) M2 first-slice sweep: `core/sail/lmdb` = 2200 tests, 4 failures, 0 errors, 3 skipped — the pre-existing baseline, zero new. Formatter and copyright green.
- [ ] M2 remainder (flat/unflat chunk plumbing, `Flatten` node, factorized EXISTS and multi-chunk consumers).
- [x] (2026-07-25 ~11:30Z) **M3 resumable / streaming emission (and step 1c's zero-copy sink, which it subsumes).** An `Emit` pipeline with no ordering or limit, built from `EnumerateDomain`, `EnumerateAdjKeys`, `Probe` and filters, now writes rows **straight into the caller's buffer** and pauses when it fills: the intermediate `out` buffer is allocated as `new long[0]` and `appendRow` is not emitted at all, so two of the three per-row copies are gone along with the unbounded growth. Gated by `rdf4j.lmdb.janinoCodegen.resumable` (default on) and folded into the shape key. Loop state lives in `stA/stB/stC` fields per pipeline position, `-1` meaning "not started" — which is also what a loop restores when it finishes, so the next outer value starts fresh and nothing has to track "paused versus done" explicitly. `LmdbNativeKernelIrEmitterTest` 38/38 (4 new, draining at 1/2/3/5/7/64 rows per call), `LmdbNativeKernelExecutionTest` 5/5, `LmdbNativeDifferentialFuzzTest` 24/24.
- [x] (2026-07-25 ~18:25Z) **M3 remainder, first half: `ProbeClose` streams.** A closed edge — both endpoints known — re-emits the continuation once per matching neighbour (multiplicity) or at most once (semi), and until now its mere presence made the *whole* kernel non-resumable, forcing the eager path with its three copies per row and its unboundedly growing `out[]`. It now streams. Two design points made this smaller than expected. First, the repetition count needs no saved state at all: it is a pure function of the key, the target and the immutable adjacency view, so it is recomputed on each entry rather than trailed — nothing about it can go stale across a pause. Second, multiplicity and semi collapse into one emitted loop, `for (; stA < reps; stA++)`, differing only in whether `reps` is `m` or `m > 0 ? 1 : 0`; the counter is what distinguishes "not started" from "already emitted", which is precisely the state `EnumerateEntry` lacks and why *it* remains excluded. `ProbeClose` was chosen over `LeftProbe` for this first half deliberately: `findVectorTail` cannot claim it (it produces no column, and `tailValueCol` would fall through to an `EnumerateAdjKeys` cast), so it carries none of the vector-tail entanglement described in the next item. Red-first evidence: `probeCloseStreamsItsMultiplicityAcrossPauses` and `probeCloseSemiStreamsExactlyOneRowPerKey` both failed on `expected: <true> but was: <false>` for `Kernel.resumable` before the change. `LmdbNativeKernelIrEmitterTest` 40/40 after (38 pre-existing plus 2 new), `LmdbNativeDifferentialFuzzTest` 24/24, `LmdbNativeKernelExecutionTest` 5/5, `LmdbNativeKernelScanQuadTest` 6/6, `LmdbNativeKernelAggregateTest` 7/7, copyright and formatter green.
- [x] (2026-07-25 ~18:30Z) `ProbeClose` streaming sweep: `core/sail/lmdb` = **2218 tests, 3 failures, 0 errors, 3 skipped**, all three failures in `LmdbNativeFeatureFlagForkTest` — the branch's pre-existing baseline, **zero new**. Judged by aggregating `target/surefire-reports/TEST-*.xml` rather than by the runner's summary line, per the standing rule on this branch.
- Note on why the multiplicity test drains at 1, 2, 3 and 64 rows: the repetitions of a closed edge are *indistinguishable* rows, so a pause that drops or duplicates one is invisible to any single-`fill` test and only surfaces when the caller's buffer is smaller than the multiplicity. Any future twin of this node owes the same drain sweep.
- [ ] M3 remainder: stream `LeftProbe`. Design settled, not yet implemented, and the ordering constraint is now known: `isResumable`'s omission of `LeftProbe` is **load-bearing rather than merely conservative**. `findVectorTail` *does* claim `LeftProbe`, the eager `emitVectorTail` has an explicit `leftProbe` branch, but the streaming overload `emitResumableVectorTail` casts any non-`Probe` producer to `EnumerateAdjKeys` — so simply adding `LeftProbe` to the streamable set yields a `ClassCastException` at codegen time, not a compile error. Streaming it therefore requires giving the streaming vector tail its own `leftProbe` branch, so that the two features compose instead of trading off. The null-arm state is cheaper than this plan previously assumed: `matched` needs no field, being derivable as `runEnd(d) > runStart(d)` — stable across a pause because the outer loops rewrite the key column before re-entry and views are immutable — and `sliceState` is unused in the unmatched branch, so it can serve as the null-arm-emitted marker. No new fields. (needs its `matched` flag and a null-arm-emitted flag promoted to fields so a pause cannot re-emit the null row) and the aggregate terminals that stream in M10.
- [x] (2026-07-25 ~11:45Z) **M4 substrate: the cursor SPI and `ScanQuad` are live and read a real store.** New public SPI `KernelScanner` + `KernelQuadCursor` in `evaluation/codegen/`, `KernelContext.scanner` (existing constructors delegate), engine-side `LmdbNativeKernelScanner`, IR node `ScanQuad` (four operands, four output columns, null term = unbound), and both emitter forms — eager and streaming. `LmdbNativeKernelScanQuadTest` 3/3 runs generated kernels against an on-disk store: a `?s ?p ?o` scan with a **variable predicate**, a **`GRAPH <g1>`-restricted** scan, and identical results draining 1/2/3/64 rows at a time. Two invariants in the scanner are load-bearing and documented there: one probe per scan site (a probe's iterator dies on that probe's next `open`, so a nested scan sharing a probe would silently invalidate its parent mid-iteration), and one reused cursor wrapper per site (a correlated scan re-opens per outer row, so allocating a wrapper per open would put garbage on the hottest path).
- Note on the scanner's lifetime, decided while wiring: `LmdbNativeKernelBindings.context` takes an optional scanner and both execution sites pass `null` today. Constructing the scanner was deliberately *not* landed ahead of the lowering — a scanner is a resource, and its owner differs by rung (the aggregate rung's `finally`, the row rung's cursor `close`). Wiring construction without that ownership would have leaked probes the moment the lowering turned on, so both arrive together.
- [x] (2026-07-25 ~12:27Z) **M4 lowering, recognition half.** `LmdbNativeKernelLowering.lowerPatternAsScan` turns a pattern no adjacency view can serve into a `ScanQuad`, behind `rdf4j.lmdb.janinoCodegen.scanSources` (**default off**, read per call so tests can flip it). Its soundness argument is by construction rather than re-derivation: the emitted node ends up issuing the same `probe.open(subj, pred, obj, context)` call, with the same operand ids, that `PatternPlan.openIterator` would have issued. What it refuses is therefore everything whose meaning lives in `PatternPlan.bind` rather than in the scan — named-graph scoping, a dataset context restriction, a term that both matches a constant and binds a slot, a repeated variable, and an ordered-scan promise. `LmdbNativeKernelLoweringTest` 9/9 (3 new: a variable predicate lowers to a quad scan and needs no adjacency; the same pattern still declines with the flag off, so the default path is untouched; named-graph and ordered-scan patterns are refused even with scans on).
- [x] (2026-07-25 ~12:45Z) **M4 execution wiring complete.** Both rungs now own a `KernelScanner`'s lifetime: the row cursor closes it in `close()`, the aggregate rung in its existing `finally`. Differential fuzz 24/24 with scan sources forced on. **The flag nonetheless ships OFF**, and the reason is a standard this plan has applied throughout: turning it on changes which plans production queries run, and that has not been through `compliance/sparql`. A green conformance result does not survive a default-on change to the row-production path — the same trap that was caught earlier when compliance turned out to predate M3 and M4. Enabling is now a one-character change gated on one suite.
- [x] (2026-07-25 ~16:10Z) **M4 final step: scan sources are ON, and the gate that was owed has been paid.** `compliance/sparql` run with `-Drdf4j.lmdb.janinoCodegen.scanSources=true`: `LMDB compliance baseline gate: PASS`, 30 reports parsed, required suites 5/5, 2648 tests / 17 failures / 0 errors, every failure inside the 24 frozen identities (7 resolved), **zero new** — identical to the vector-tail run earlier the same day, so scan sources move conformance not at all. Evidence: `post-evidence.m4-scansources-compliance.txt`. Two corrections to the record this exposed. First, the default had **already** shipped as `true` in HEAD (`1868e63e7f`, committed 15:36 the same day) while the javadoc directly above it still read "defaults OFF ... flip this to true once it passes" — the leftover of the "briefly defaulting it on" experiment noted below, committed before the gate ran. So this step did not flip a flag; it retro-verified a flag that was already live and corrected the comment to match. Second, the theme-benchmark rerun of 2026-07-25 15:36 therefore already measured **with scan sources on**, which is worth knowing before attributing any of its deltas.
- Method note earned here: a flag whose javadoc states a default is a claim that can rot independently of the code. The mismatch was invisible to every test, because the lowering tests correctly set the property explicitly in both directions. Grep the default out of the code, not out of the prose, before trusting a "ships off" statement.
- Note: briefly defaulting it on exposed a test of my own that leaned on the ambient default rather than setting the flag. Such a test fails the day the default moves while saying nothing about the behaviour it protects, so it now sets the property explicitly. Worth applying to any flag-sensitive test here.
- [ ] ~~M4 remainder — execution wiring only.~~ The flag stays off until a rung owns a `KernelScanner`'s lifetime: the aggregate rung can close it in its existing `finally`, the row rung's cursor must close it. Turning the flag on before that hands a generated kernel a null scanner. Everything above it — SPI, engine scanner, IR node, both emitter forms, and now recognition — is in place and tested.
- [ ] ~~M4 remainder — the lowering is not wired yet~~, so no production query reaches `ScanQuad` today. `LmdbNativeKernelLowering` must replace the blanket `pattern-guards` decline with a per-pattern source choice, and `LmdbNativeKernelBindings`/`LmdbNativeKernelExecution` must carry scan descriptors and construct the scanner. Until then the capability is proven but dormant, exactly as plan 19's IR was before plan 20 lowered to it.
- [x] (2026-07-25 ~15:36) **Theme benchmark rerun, read against its own error bars.** The full corpus (110 theme/query pairs) was re-measured at HEAD `1868e63e7f` — that is, with the vector tail, resumable emission *and* scan sources all on. Judged by disjoint JMH confidence intervals (a delta counts only when `|Δ| >` the sum of the two `±` half-widths): **zero significant regressions**, six significant improvements — ANALYTICS q0 `10.317 → 0.167` ms/op (−98.4%, the `irAggregate` guard), ANALYTICS q3 `91.229 → 48.914` (−46.4%), LIBRARY q8 `4.737 → 2.599` (−45.1%), TRAIN q4 `140.546 → 121.356` (−13.7%), LIBRARY q10 `50.145 → 46.397` (−7.5%), ELECTRICAL_GRID q5 `0.452 → 0.416` (−8.0%). The milestone gate "no ANALYTICS query may regress by more than five percent" therefore **holds**. Artifact: `benchmark-significance-2026-07-25.txt`.
- The corpus-wide `−18.5%` that a naive score-only diff reports is **not claimable** and must not be quoted: 14 further pairs moved by more than 20% while staying inside the noise band, and the single largest apparent gain, HIGHLY_CONNECTED q10 at `−1700` ms, carries error half-widths of `±7128` (base) and `±30974` (new). That one pair supplies 49% of the apparent total.
- [x] (2026-07-25 ~19:30Z) **Census-directed work, part 1: binary `JoinPlan` cores and pure `BIND` aliases lower on the aggregate rung.** Two changes, both in `LmdbNativeKernelLowering`. The aggregate rung's core handling is now a recursive `Builder.lowerJoinOperand` that descends `JoinPlan` (left before right, which preserves the tree's own binding flow — the emitted pipeline *is* a nested-loop chain), splices a nested `MultiJoinPlan`'s ordered children and filters, peels a nested `FilterPlan`, and descends an `ExtensionPlan` before applying its copies. `Builder.lowerExtensionCopies` lowers each `CopyBinding` to a `BindAlias` — `Operand.col`/`entry` for a slot copy, `Operand.constant` for a constant copy. `MAX_CHILDREN` is now enforced by counting lowered producers rather than by an array length, since the operand list is no longer flat. Reason strings preserved: an unsupported *core* still reports `agg:unsupported:X` and an unsupported nested operand `agg:child:X`, so the explain assertions and the frozen decline vocabulary are untouched.
  Three refusals define the boundary, each with a test. A **computed** copy declines (`agg:bind-computed`): it needs `LmdbNativeKernelHooks.computeBind`, which throws, so lowering it to an alias would silently substitute a wrong value. A copy onto an **already-bound** slot declines (`agg:bind-target-bound`): `ExtensionCursor` gets equality-test semantics for free from `RowState.bind` returning false, and a fresh kernel column cannot express that. A copy whose **source is not yet available** declines (`agg:bind-source-unavailable`). The unbound case needed no handling at all and that is worth stating: `ExtensionCursor` skips a copy whose value is `UNKNOWN`, leaving the target unbound, and the row materialization skips a kernel column holding `-1` for exactly the same reason (`LmdbNativeKernelExecution.java:262-266`) — the two representations already agree.
  Evidence, red first: `aggregateRungLowersABinaryJoinCoreWithAPureBindAlias` and `aggregateRungLowersAConstantBindAlias` both failed `expected: not <null>`; the two decline tests passed before the change *for the old reason* and still pass after, which is what keeps the fix from being over-permissive. `LmdbNativeKernelLoweringTest` 13/13 after (9 pre-existing plus 4 new), `LmdbNativeDifferentialFuzzTest` 24/24, `LmdbNativeKernelAggregateTest` 7/7.
- [x] (2026-07-25 ~19:45Z) **Census re-run on SOCIAL_MEDIA measures what actually moved — and it is a real speedup, not just capability.** Eight of the theme's eleven pairs carried an `irKernel` decline before; after the change **three lose it entirely** and their aggregate root now reports `nativeExecutionPath=irAggregate` (q3, q8, q9), while **four move deeper to the operand that genuinely blocks them** — q5 to `agg:child:UnionPlan`, q7 to `agg:child:MinusPlan`, q2 to `agg:sticky:BooleanCombinationFilter`, q4 to `agg:witness-subplan:ExtensionPlan`. That shift is the predicted outcome and is itself the evidence the descent works: a `JoinPlan` decline can only become a `child:UnionPlan` decline if the tree was descended and the real operand reached.
  Two of the three unlocked pairs were then measured properly, 3 warmup + 5 measurement iterations, against the **2026-07-24** baseline rather than 07-25 because 07-24 is the run whose error bars for these two are tight:

    SOCIAL_MEDIA q8   626.845 ± 8.439  ->  37.425 ± 1.514 ms/op   16.7x faster
    SOCIAL_MEDIA q9     4.028 ± 0.141  ->   0.177 ± 0.005 ms/op   22.8x faster

  Both are significant by the disjoint-interval rule this plan adopted earlier the same day; the 07-25 run's own bars for these pairs (`591.158 ± 215.377` and `4.219 ± 5.234`) could not have supported either claim, which is exactly why the baseline choice is stated rather than left implicit.
- Method note, recorded because it cost a wrong conclusion first: the theme benchmark's Telemetry explain is printed from `@TearDown(Level.Trial)` (`ThemeQueryBenchmark.java:485-509`), i.e. **after** all iterations, so a census re-run reflects settled kernel admission. But JMH's stdout goes to `<module>/target/benchmark-output.log`, not to the wrapper script's stdout — reading the wrapper log instead produced "every decline vanished, including ones this change cannot touch", a pure artifact. A decline set that changes in ways the diff cannot explain means the wrong file is being read. Also beware that each run **overwrites** that log.
- Hazard found and repaired in passing: running the benchmark left `ThemeQueryBenchmark.java`'s `@Param` theme list with nine of ten themes commented out (mtime inside the run window; the wrapper script itself contains no `sed`, so the rewrite comes from somewhere in the JMH/plan-guard path). Left in place it would silently reduce the next full corpus run to `MEDICAL_RECORDS` alone while still looking like a corpus run. Restored to all ten themes; worth checking `git diff` on that file after any benchmark invocation.
- [x] (2026-07-25 ~20:10Z) **Census-directed work, part 2: sticky `BooleanCombinationFilter` conditions decompose.** A `FILTER` combining an EXISTS with anything else arrives sticky (mask `< 0`) and reached `lowerWitness`, which handled only `NegatedNativeBooleanFilter`, `StatementPatternExistsFilter` and `ExistsFilter` before falling through to `agg:sticky:BooleanCombinationFilter`. It now decomposes, and the decomposable cases are exactly the two cells where `conjunction != negated`:

    A AND B            split into two sequential filters      (sequential kernel filters AND)
    NOT(A OR B)        split into two negated filters         (De Morgan)
    A OR B             DECLINE  agg:sticky-disjunction        (needs a real OR of witnesses)
    NOT(A AND B)       DECLINE  agg:sticky-disjunction        (is NOT A OR NOT B)

  Soundness rests on two facts read out of the code rather than assumed. Multiple `Exists` nodes genuinely AND: they are appended to the pipeline by `pipeline.addAll(witnessNodes)` and each emits `if ([!]witness()) { <continuation> }`, so they nest. And a non-witness conjunct may be taken **only un-negated** — this is the load-bearing restriction, not a conservative one. `NegatedNativeBooleanFilter.accept` is a plain `!delegate.accept` (`LmdbNativeFilters.java:751-753`) while a `NativeBooleanFilter` has already collapsed a SPARQL type error to `false`, so negating a comparison that errored returns **true** and would keep a row the generic evaluator drops. An EXISTS is exempt because it is genuinely two-valued and cannot error. Hence `agg:sticky-negated-condition` for a negated non-witness operand.
  A related trap avoided: stickiness cannot be detected with `batchReadMask()`, because it defaults to `-1` (`LmdbNativeFilters.java:60-63`) and `NegatedNativeBooleanFilter` does not override it — so a negated *comparison* reports `-1` and would be mistaken for a witness. The operand test is therefore structural (EXISTS-family after unwrapping the `Recording`/`Negated` wrappers), which is also exactly the set `lowerWitness` can handle.
  Evidence, red first: `aggregateRungSplitsAConjunctionOfAWitnessAndAComparison` and `aggregateRungSplitsANegatedDisjunctionOfTwoWitnesses` both failed `expected: not <null>`; the three decline tests (`...DeclinesADisjunctionOfWitnesses`, `...DeclinesANegatedConjunction`, `...DeclinesNegatingAComparisonThatCanError`) passed before *for the old blanket reason* and still pass, which is what stops the change being over-permissive. `LmdbNativeKernelLoweringTest` 18/18, `LmdbNativeDifferentialFuzzTest` 24/24.
- [x] (2026-07-25 ~20:20Z) **Census-directed work, part 3: `UnionPlan` operands lower to the IR's `Union` node.** First assessed here as milestone-sized and then found tractable, which is worth recording because the reason was a wrong assumption rather than a wrong estimate. The obstacle looked like this: branches must be lowered into separate `List<Node>` sub-pipelines, and the Builder has no way to redirect node emission into a sub-list — the witness path solves the same problem by *duplicating* pattern lowering (`lowerWitnessPattern`, `lowerWitnessJoin`) against its own column map. The way out is to not redirect at all: lower each branch through the ordinary machinery and then **harvest** the depth entries it added (`harvestDepths(mark)` removes depths from `mark` upward and returns their nodes in pipeline order, interleaving `nodesPerDepth` with `filtersPerDepth` exactly as `buildAggregate` does). All existing producer lowering is reused unchanged.
  The second obstacle was real and needed a mechanism: `newColumn(slot)` overwrites `slotColumn[slot]`, so two branches binding a shared variable would allocate two columns and every downstream node would read one that only the first branch ever wrote. A `pinnedColumns` map, consulted at the top of `newColumn`, keeps a shared variable on a single column; each branch is lowered from the *pre-union* binding state so it sees the same variables as fresh and the same endpoints as bound. After the union, columns are published at the union's own depth, `assuredMask` becomes the intersection over branches, and a column not bound by *every* branch is added to `optionalColMask` — it can hold the `-1` sentinel, so an id-tier filter over it must not be admitted.
  Everything else was already correct in the emitter and only needed to be relied on: it emits one call per branch into the *same* continuation, so the row counts add (multiset union, `|L| + |R|`, matching `UnionCursor.next()` which drains left then right with no de-duplication), and it precedes each branch with `v<col> = -1L` resets over `resetColumns()`, so a variable only one branch binds reads as unbound in the other branch's solutions.
  Refusals: a branch-local filter (`agg:union-branch-filter` — applying one branch's filter to the whole union would wrongly filter the other branch's rows, and placing it *inside* the branch needs care this pass did not take), an empty branch (`agg:union-empty-branch`), and — by falling through the existing dispatch — `OrderedUnionPlan`, which is a *different* class promising order the kernel does not preserve, plus `LeftJoinPlan`/`MinusPlan` branches. `MAX_CHILDREN` counts producers summed across branches, since the operand list is no longer flat.
  Evidence, red first: `aggregateRungLowersAUnionOperandToAUnionNode` and `bothUnionBranchesShareOneColumnForASharedVariable` failed `expected: not <null>`; `aggregateRungDeclinesAUnionWithAnUnlowerableBranch` passed before and after. `LmdbNativeKernelLoweringTest` 21/21.
- [x] (2026-07-25 ~20:25Z) **A differential-fuzz gap closed, and it was load-bearing.** The existing fuzz suite would have passed both of the above without exercising either: `randomAggregates` emits plain patterns only, and the UNION/OPTIONAL/MINUS generation lives in the *row*-shape round, so nothing drove a union or an EXISTS-conjunction through the **aggregate** rung. New round `aggregatesOverUnionsAndExistsConjunctions` covers 17 shapes — identical branches (so a lowering that de-duplicated or dropped a branch fails immediately on the count), a variable bound by only one branch, grouped and ungrouped forms, a union joined to an anchor pattern, three-way nesting, `EXISTS && isLiteral`, two conjoined EXISTS, `NOT(EXISTS || EXISTS)`, and the two cells that must still decline. `LmdbNativeDifferentialFuzzTest` **25/25**. Lesson worth keeping: a green differential suite is only evidence for the shapes it generates — check that the round covering your change exists before quoting it as a gate.
- [x] (2026-07-25 ~20:55Z) **The union change's steal risk was quantified, and the headline exposure does not exist.** An adversarial review put "up to twenty pairs worth 5257 ms of a 15257 ms corpus" at risk, headed by LIBRARY q6 at 2776 ms (18% of corpus on its own), on the sound observation that the IR aggregate rung is attempted *before* `FactorizedTail.select` and that the only anti-steal guard, `prefixRunHandlesRow`, covers prefix runs alone. Verified in code — the order really is `tryEvaluateAggregate` at `LmdbNativeGroupStep` ~300 ahead of `FactorizedTail.select` at ~408 — so the mechanism is real. But the *estimate* assumed every post-descent `agg:unsupported:JoinPlan` pair becomes lowerable, without checking each for a second blocker. LIBRARY q6 contains an `OPTIONAL { pattern . BIND(...) }`, and the aggregate rung peels only `FilterPlan`/`MinusPlan` — it has no OPTIONAL support at all — so it still declines regardless of union support. Measured A/B, 2 warmup + 4 measurement:

    LIBRARY q6, unionSources=false   2883.015 ± 243.906 ms/op
    LIBRARY q6, unionSources=true    2869.997 ± 288.979 ms/op

  Indistinguishable, as predicted before running it. The lesson generalises: an exposure estimate built from decline strings must check each pair for *every* blocker, not just the one being removed, or it over-counts by whatever share of pairs has a second gate.
- [x] (2026-07-25 ~21:10Z) **Engagement checked on a real query, and the union lowering currently unlocks nothing in the corpus.** The A/B above showed no regression, which is only meaningful once you know whether the path changed at all — so ENGINEERING q1 was re-run with `unionSources=true` and its telemetry captured. Its decline moved from `agg:unsupported:UnionPlan` to **`agg:union-branch-filter`**, and it still executes `nativeExecutionPath=orderedDistinctGroups`. So the lowering *is* reached — the dispatch and `lowerUnionOperand` both run — but every real corpus union carries a branch-local filter, which is exactly the refusal taken as a shortcut to keep the change small. That fully explains the A/B: no regression *and* no benefit, because no query changed paths. The synthetic coverage is real (unit tests plus a fuzz round that now asserts `aggPlanned() > 0`), but the corpus reach is currently zero.
  Recording this rather than quoting the clean A/B is the point: "no regression" and "no effect" look identical in a benchmark table, and only the decline string distinguishes them.
- [x] (2026-07-25 ~21:40Z) **Union branch-local filters lower inside the branch, and with that the union path reaches a real corpus query at parity.** `lowerBranchFilters` lowers a branch's own filters while that branch's depths are still open, so `harvestDepths` collects them with it. Two refusals, both verified rather than guessed. A **sticky** branch filter still declines (`agg:union-branch-sticky`): `lowerWitness` appends to `witnessNodes`, which `buildAggregate` flushes *after* everything else, so a witness cannot be scoped to one branch. And because `placeFilter` derives a filter's depth from its operands, a condition mixing pre-union and branch columns can land *below* the harvest mark; rather than predict that, the filter lists below the mark — including `entryDepthFilters` — are compared before and after, and an escape declines (`agg:union-branch-filter-escaped`).
  Evidence: `aBranchLocalFilterIsLoweredInsideItsBranch` failed red (`expected: not <null>`) and now asserts the filter token sits *between* the union's `u{` and `};` braces; `aStickyBranchFilterStillDeclines` passed before and after. `LmdbNativeKernelLoweringTest` 26/26. Fuzz round extended with six branch-filter shapes — one branch filtered, the other branch filtered, both filtered on different predicates, grouped, and with an anchor pattern — since a filter leaking out of its branch shows up as a wrong count; `LmdbNativeDifferentialFuzzTest` 25/25 with the engagement assertion still holding.
  **Engagement on a real query, finally positive:** ENGINEERING q1 now reports *no* `irKernel` decline at all and executes `nativeExecutionPath=irAggregate`, where before it ran `orderedDistinctGroups`. So the kernel has displaced a tuned specialized path — exactly the risk this plan has been tracking — and it was measured:

    ENGINEERING q1, unions=false (orderedDistinctGroups)   231.242 ± 3.745 ms/op
    ENGINEERING q1, unions=true  (irAggregate)             217.112 ± 15.413 ms/op

  Intervals overlap, so by this plan's own disjoint-interval rule this is **parity, not a win**. An earlier, looser A/B of the same pair came out the other way (226 vs 245), and two runs disagreeing on direction is itself the finding: the difference lives inside run-to-run variation. The one consistent signal is variance — the kernel arm's error bar is four times wider (±15.4 vs ±3.7), which is worth watching as more shapes move onto it.
  Method note: `--param themeName=X --param z_queryIndex=N` does **not** rewrite `ThemeQueryBenchmark.java`, while `--theme-query X:N` does. Use the former.
- [ ] ~~Next step for union value: place branch-local filters inside the branch instead of declining.~~ (done above)
- [ ] Remaining union work: confirm the other four union-core pairs (MEDICAL_RECORDS q1, LIBRARY q1, TRAIN q1, ELECTRICAL_GRID q1) also engage and hold parity; they carried the same `agg:unsupported:UnionPlan` and are the rest of the addressable set. A branch's filter must not be applied to the whole union — it would filter the other branch's rows — but it can be lowered *while that branch's depths are still open*, so `harvestDepths` collects it with the branch. The one hazard is that `placeFilter` computes a depth from its operands and could place a filter *below* the branch's harvest mark (a filter mixing pre-union and branch columns); the guard is to decline only in that case rather than for every branch filter. With that, ENGINEERING q1, LIBRARY q1, MEDICAL_RECORDS q1, TRAIN q1 and ELECTRICAL_GRID q1 become the pairs to re-measure — and per the capability-aware-admission step above, the kernel should still decline them unless it can match `orderedDistinctGroups`, which is what all five currently run.
- Decision (2026-07-25, user): union lowering ships **default ON** during development rather than behind a default-off flag, so that regressions surface in benchmark runs now instead of hiding behind a flag nobody flips. The risk and the watch-list are documented on `UNION_SOURCES_PROPERTY` itself, and "false" isolates the lowering for diagnosis.
- **Decision (2026-07-25, user): do NOT flip the rung order.** The IR aggregate kernel keeps its position ahead of `FactorizedTail` and the sequential/parallel paths. Two consequences follow, and they are the whole forward direction of this plan.
  First, the cheap escape is closed. Deferring to the specialized path — either by reordering or by asking `FactorizedTail.select(...)` whether it would claim the plan and declining if so — is off the table as the primary answer. Worth recording that the "ask first" variant was not free anyway: `select` calls `probe(...)`, which **opens a real probe** that must be released with `discardUnopenedProbe()` (`LmdbNativeFactorizedTail.java:263-278`), so a speculative call costs a probe open and discard per aggregate evaluation — precisely the wrong thing on a correlated path where the enclosing join re-opens per outer row. Any future structural guard must be extracted from `create()`'s pre-probe checks rather than built on `select`.
  Second, **the kernel has to earn the priority it already has**. Whatever the specialized rungs do, the kernel must do at least as well, because it sees the shape first. That is the user's instruction — the kernel should implement factorized tail and the sequential/parallel paths — and it is now the plan's direction rather than an option:
  1. **Generalise the kernel's factorized counting**, where the asymptotic win is. Today `bulkCountTail()` requires the counting aggregate's group *and* counted columns to be outer and every trailing filter to vectorize — out-degree shapes and little else. Needed: multiplying run lengths across a *tail of several* patterns rather than only the innermost (O(distinct keys) instead of O(rows)); weighted SUM, mirroring the interpreted `AggState.addWeighted`; and `COUNT(DISTINCT inner)` — which the gate explicitly refuses today and which is exactly what LIBRARY q6 needs.
  2. **Parallelism by reusing the exchange, not by putting threads in generated source.** `LmdbNativeExchange` already owns morsels and range-partitioned scans, kernel instances are already created per open, and `AggState.mergeFrom` already merges every aggregate kind (phase 6). So N worker kernels over partitions with per-worker group tables merged at the end needs no new codegen — and thread management inside emitted code is against both debuggability and the conservative Janino subset plan 19 established.
  3. **Ordered / prefix-run scans** (M7–M9), which is what `orderedDistinctGroups` exploits and what all five union-core pairs currently run. Until the kernel can emit seek-skip scans, a shape where prefix runs win is a shape the kernel will lose — and with no flip and no deferral, the only remedy is to build the capability.
  Interim consequence to keep honest: while (1)–(3) are outstanding, every widening of admission is a bet that the kernel is not displacing something better, and the only instrument for checking is a per-pair A/B plus the decline census. `prefixRunHandlesRow` stays as the one hard-coded exception it already is.
- Worked example of why the ordering matters, LIBRARY q6: `SELECT ?member (COUNT(DISTINCT ?loan) …) WHERE { {?loan a Loan ; borrowedBy ?member} UNION {?member a Member} OPTIONAL { ?loan loanedCopy ?copy . BIND(?copy AS ?optCopy) } FILTER(?optCopy != ?member) } GROUP BY ?member HAVING(COUNT(?loan) > 0)`. Union support was necessary but nowhere near sufficient: it is gated first on OPTIONAL (the `LeftGroup` container no milestone owns) and then on step 2, because both the group key and the counted variable are produced *inside* the union — so they are inner, `bulkCountTail()` declines, and an admitted kernel would enumerate and hash-dedup against `orderedDistinctGroups`' seek-skip and likely lose. Capability alone would have made this query *slower*; that is the whole argument for doing step 1 before widening admission further.
- Note on scope: the **row** rung still declines an `ExtensionPlan` core (`unsupported:ExtensionPlan`, 11 pairs). It is deliberately not a copy-paste of the above. The row rung lowers OPTIONAL arms, so a `BindAlias` there can copy a maybe-null column, and `optionalColMask` would have to propagate from source column to alias column or `lowerOptionalArm`'s `operandMaybeNull` guard would stop seeing a null-capable key. The aggregate rung has no optional arms — it declines `agg:unsupported:LeftJoinPlan` — so the propagation is provably dead there and was left out rather than added untested.
- [ ] **Discovery to act on next, found while testing the above and currently masked:** `buildAggregate` recognises `COUNT(*)` only when the spec's constant is `UNKNOWN` (`LmdbNativeKernelLowering.java:1074-1075`), but production builds `COUNT(*)` as `AggregateSpec.star(...)`, whose constant is `NULL_CONTEXT_ID` (`0L`) — see `LmdbNativeAggregatePlannerBase.java:459` and `LmdbNativeAggregateState.java:144`. A production `COUNT(*)` therefore falls through to the slot branch and declines with `agg:input-unavailable`. That string has **zero** occurrences in the 2026-07-25 telemetry, and the reason is instructive rather than reassuring: the structural declines fire first, so nothing ever reached `buildAggregate`. Removing the `JoinPlan` decline may well expose it, which is precisely why the census has to be re-run rather than assumed. The fix looks small — counting a constant that is never `UNKNOWN` once per row *is* `COUNT(*)`, so the guard can accept any constant while still refusing `distinct` — but it is behaviour-changing and owes its own failing test.
- [ ] M5 `ScanProbe` / `ScanClose` / `ScanLeft`. Reduced by one node: `ScanProbe` need not exist, because `ScanQuad`'s terms are already `Operand[]` and a column operand makes it a correlated cursor probe, re-opened per outer row for free by the streaming form.
- [ ] M6 terminal modifiers + dead-IR activation (DISTINCT, ORDER BY, LIMIT/OFFSET, `Union`, BIND, `Intersect`, `PathExpand`).
- [ ] M7 ordering contract + ordered scans.
- [ ] M8 `MergeJoin` + skip-ahead.
- [ ] M9 prefix runs, range scans, DISTINCT-driven skipping.
- [ ] M10 `OrderedDedup` + `OrderedAggregate`.
- [ ] M11 inlined-literal identifier tier.
- [ ] M12 remaining physical-strategy parity.
- [ ] M13 planner construct growth + coverage audit.
- [ ] M14 combination matrix, rung-order flip, retirement of the superseded generators.

### 2026-07-26 — decline census as a gate, and the structural declines cleared against it

- [x] **Two instruments, both in-repo and both failing at the start.** `LmdbNativeKernelDeclineCensusTest` runs every theme-catalog query (143 theme/query pairs across 11 themes) against a scaled-down all-themes store at `Explanation.Level.Telemetry`, scrapes `irKernel:` / `irAggregate:` reasons off the explain tree, writes `target/kernel-decline-census.txt`, and asserts no pair declines. `LmdbNativeKernelAdversarialDeclineTest` does the same for 48 hand-written queries of at most twenty lines each (a length the test itself enforces), aimed at the constructs this plan lists as unreachable — paths of every flavour, nested OPTIONAL, UNION of UNIONs, MINUS with conditions, EXISTS disjunction, computed BIND, multi-variable VALUES, sub-SELECT, GRAPH, and the aggregate functions with no native accumulator — and additionally asserts every one of them returns exactly what the generic evaluator returns. Both run in about ninety seconds, which is what made the loop below possible.
- [x] **Census: 64 of 143 pairs declining → 10.** Recorded at `initial-evidence-kernel-decline-census.txt`. Cleared, in order of size: `unsupported:ExtensionPlan` (14), `agg:minus-arm:FilterPlan` (13), `agg:unsupported:LeftJoinPlan` (9), `unsupported:PatternPlan` (9), `optional-arm:LeftJoinPlan` (3), `agg:witness-subplan:*` (3), `unsupported:UnionPlan` (2), and the singles `unsupported:JoinPlan`, `agg:child:MinusPlan`, `agg:input-unavailable`, `agg:minus-arm:MultiValuePatternPlan`, `values-slot-bound`, `child:LeftJoinPlan`, `optional-arm:ExtensionPlan`.
- [x] **The row rung was the single largest gap, and it was an entry-point gap rather than a capability one.** `lowerRows` required its core to be a `MultiJoinPlan`; the aggregate rung's `lowerJoinOperand` had descended `JoinPlan` / `MultiJoinPlan` / `FilterPlan` / `ExtensionPlan` / `UnionPlan` for some time. Routing the row core through the same descent cleared 22 pairs by itself and needed exactly one new piece of logic — the `optionalColMask` propagation across a `BindAlias` that this plan predicted (see the scope note above), because the row rung is the one that lowers OPTIONAL arms. Decline reasons raised in shared code now take a rung prefix (`agg:` or empty) so the census attributes them correctly.
- [x] **`LeftGroup`, the OPTIONAL container this plan said no milestone owned.** New IR node holding one sub-pipeline; the emitter runs the arm with a per-node `lgN` match flag (a field, not a local — the arm is emitted as its own methods) and, when the arm produced nothing for a row, resets every column the arm binds to `-1` and runs the continuation once. `LeftProbe` stays as the fused single-pattern case. Lowering harvests the arm the way `lowerUnionOperand` harvests a branch, lowers the arm's own conditions *inside* it via `lowerBranchFilters` (a FILTER inside OPTIONAL is a join condition: failing it must leave the optional variables unbound, not delete the row), and marks every arm column maybe-null and un-assured. Both rungs use it, so `LeftJoinPlan` is now a `lowerJoinOperand` case rather than a decline.
- [x] **MINUS and EXISTS witness bodies grew the wrappers they were meeting in practice.** `FilterPlan` around a MINUS arm now lowers as a witness filter inside the arm (outside it would test the left row and drop rows MINUS must keep); `MultiValuePatternPlan` lowers as either an enumerated domain or a `FilterInConstants` membership guard depending on whether the constrained slot is already readable; a nested `MinusPlan` is placed inline at its correlating depth rather than deferred to `witnessNodes`, which the row rung never flushes. The BIND-peeling that both containers already did was unsound and is now guarded: dropping a copy whose target correlates with the outer row makes the witness succeed more often, so EXISTS keeps rows it should drop and MINUS drops rows it should keep — `witnessExtensionIsInert` declines that case instead.
- [x] **Dead IR activated: `PathExpand`.** `p+` / `p*` with one endpoint known now lowers to the node that has existed and been unit-tested since the IR landed. One step, plain subject-to-object orientation, one endpoint bound; both-ends-free still declines, because for `*` the answer includes every term the path could start from and that is a different enumeration than this node performs.
- [x] **Repeated variables reach the scan path.** `?x p ?x` lowers exactly as `ScanQuad`'s own contract describes — both positions free, each to its own column, guarded by an equality — with the trap being that the second occurrence must *not* resolve through `operandOf`, which would pass the first column's not-yet-read value as a scan bound.
- [x] **`COUNT(*)` fixed as this plan predicted, and the prediction that the census would expose it was correct.** The guard tested `constant == UNKNOWN`; `AggregateSpec.star` carries `NULL_CONTEXT_ID`. Once the structural declines stopped firing first, `agg:input-unavailable` appeared on ANALYTICS q11 exactly as forecast.
- [x] **Correctness held throughout.** `LmdbNativeDifferentialFuzzTest` 25/25 green after the widening; 46 of 48 adversarial queries matched the generic evaluator, and the two that did not were the test's fault rather than the engine's — `SAMPLE` may return any member of its group, and a `LIMIT`/`OFFSET` window over a non-total `ORDER BY` is an arbitrary slice. Both are now handled in the corpus rather than papered over.
- [x] **AVG built as the two-slot accumulator that was sketched here, and it closed three of the remaining pairs.** The emitter had already accumulated sum (`agS`) and count (`agN`) separately and was only spoiling it at the last step, dividing in `double` and emitting one number — which returns `xsd:double` where SPARQL returns `xsd:decimal`, and returns a different *value* whenever the quotient is not a dyadic rational. Now `AggregateOutput.width()` makes AVG occupy two output slots (`Aggregate.stride()` and the new `outputOffset(i)` account for it, so a later aggregate's position no longer assumes one slot each), the emitter writes sum bits and count, and `kernelGroupRow` divides through the very `MathUtil.compute(sum, size, DIVIDE)` the interpreted aggregate uses. The SUM exactness guard is armed for AVG too: it certifies every input was an exact integer, which is what makes reconstructing the sum as an integer literal — and therefore the division — exact. `COUNT`-style empty groups bind `INTEGER_ZERO`, matching the interpreted path. Census 10 → 8 pairs; the adversarial corpus's `group-avg` engages and matches the generic evaluator.
- [x] **`p*` with both ends free, via a new `EnumerateTerms` node — the adversarial corpus is now at zero.** The argument for declining it (two whole-store scans, a dedup, a union, a distinct terminal) was wrong about the shape and unmeasured about the cost. Wrong about the shape because `PathExpand` with `minHops = 0` already emits the start itself and dedupes per start, so the only missing piece was the start set; and distinct starts cannot collide, so no cross-start dedup is needed at all. Unmeasured about the cost because the interpreted `ZeroLengthPathIteration` enumerates the same term set for the same shape — the kernel is not doing more work, it is doing the same work in generated code. `EnumerateTerms` scans the store once, dedupes subjects and objects into a `LongHashSet`, and feeds each term to the expansion. Predicate and context positions are excluded to match the interpreted result. 48 of 48 adversarial queries now engage and match the generic evaluator.
- [x] **Bind-time `adjacency-unavailable` turned into a retry rather than a decline.** Whether a view exists, and whether it can enumerate its keys, is a bind-time fact the lowering cannot predict — and it is exactly what direct scans exist to cover. Both rungs now re-lower once with `preferScans`, which puts `lowerPatternAsScan` ahead of every adjacency request; the retry requests no views, so it cannot recurse. Cleared ADAPTIVE_FILTER_PLACEMENT q0 outright and moved REAL_ESTATE q1 from a bind-time refusal to the terminal, where it meets the SUM guard. Census 8 → 7.
- [ ] **Remaining census declines (7 of 143), and why each is where it is.** `pattern-ordered-scan` (4: HIGHLY_CONNECTED q11, TRAIN q11, ELECTRICAL_GRID q11, PHARMA q11) — **and the shape of this one may not be what the plan has been assuming.** The working assumption has been that it is M7/M8 capability: the pattern promises index order, the kernel enumerates CSR order, so honouring it needs a `StatementOrder` on `ScanQuad` and in the scanner. But the kernel rung is only reached through `NativeRowsStep.openUnorderedInput`, and the one call site that wants order — the ordered-DISTINCT path at `LmdbNativeRowStep` ~1690 — records its own decline (`orderedDistinct:global-hash`) and *then* falls through to it. If every route into `openUnorderedInput` is likewise downstream of the consumers that need order, then a pattern's `statementOrder` there is a residue of index selection rather than a live promise, and the right change is to **remove the decline**, not to build an ordering contract. That is a two-file reading job (the four `openUnorderedInput` call sites plus `NativeRowsIteration`) followed by an ordering-specific differential test, and it should be done before either path is chosen — getting it wrong is silent, since a broken order promise corrupts a merge join without failing anything. `sum-guard` (2, PHARMA q9 and REAL_ESTATE q1) is **not** a capability gap at all: the kernel reached the terminal and correctly refused because double accumulation would not have been exact — making these engage means accumulating integer sums in a `long` and encoding them exactly, which is a real improvement but a different one from "the kernel cannot express this". `agg:sticky:<lambda>` (1, MEDICAL_RECORDS q10) is a `compileCompare` lambda whose `batchReadMask()` is the sticky default, so no tier can read it; giving it a real mask is a small change in the filter compiler but `batchReadMask` also drives interpreted filter *placement*, so it is a wider blast radius than one pair justifies without measurement.
- Lesson worth keeping from the `p*` reversal: the first decline was justified with an unmeasured cost claim ("would lose to the interpreted path") attached to a shape argument that turned out to be wrong. Both halves were checkable — the interpreted path's own zero-length enumeration was one file away, and `PathExpand`'s dedup semantics were in the emitter. A decline reason that rests on a guess about cost should say so, or be checked before it is written down.
- [ ] **Exposure found and not yet closed: kernels containing `LeftGroup`, `Union`, `LeftProbe`, `Exists` or `PathExpand` are not resumable** (`LmdbNativeKernelIr.isResumable` whitelists only filters, `EnumerateDomain`, `Probe`, `ScanQuad`, `ProbeClose` and value-bearing `EnumerateAdjKeys`), and a non-resumable kernel runs its whole pipeline into `out[]` on the first `fill` before serving any row. That is pre-existing, but this session's widening routes far more plans onto it. It is not theoretical: HIGHLY_CONNECTED q12 (`SELECT DISTINCT *` over a two-hop expansion under a triple-nested OPTIONAL) exhausted a 4 GB fork heap once the kernel took it, where the interpreted path had streamed. The census dataset now scales that theme down, which keeps the instrument honest about plan shape but deliberately does **not** address the underlying exposure. Closing it means either making `LeftGroup` resumable or bounding the materialized output and declining past the bound — the second being far cheaper and worth doing before this lands anywhere real.

## Surprises & Discoveries

- Observation (2026-07-25, found by self-review rather than by a test): the first cut of the vector tail allocated its scratch in `bind()` as `new long[VECTOR_SIZE]` plus `new int[VECTOR_SIZE]` — 24KB per kernel instance. Kernel instances are created **per cursor open**, and `openUnorderedInput` runs per outer row under correlated evaluation, so this put a 24KB-per-outer-row allocation on precisely the path this plan exists to make allocation-free. No test caught it, because every correctness gate was blind to allocation. Fixed by allocating lazily and growing monotonically to the run length actually observed (capped at `VECTOR_SIZE`), and by skipping the selection scratch entirely when no filter vectorizes: a three-neighbour run now costs tens of bytes instead of 24KB. Pinned by an assertion that `bind()` contains no array allocation at all.
  Evidence: `LmdbNativeKernelIrEmitterTest.vectorTailBulkReadsTheRunAndVectorizesIdFilters` now slices the generated source between `public void bind(` and `public int fill(` and asserts neither `new long[` nor `new int[` appears there.
- Observation (2026-07-25, same review): the bulk-count path copied the whole run through `copyRun` and then used only `cnt`, i.e. the run's *length*. For an unfiltered count every one of those bytes was waste. When nothing downstream reads a neighbour and nothing filters them, the run's contribution is `rend - rpos`, so the copy, the slice loop and the chunking all disappear — counting a key's edges becomes a subtraction rather than work proportional to the number of edges. This is the strongest form of the factorized-counting win and it fell out of reviewing the emitted source rather than the Java.
  Evidence: the same test asserts the generated source contains `updateBy(rend - rpos);` and contains neither `.copyRun(` nor `while (rpos < rend)`.

- Observation (2026-07-25, M3, caught by a test): the first streaming implementation produced **65 duplicate rows** out of 4103. The cause is a detail of Java `for` loops that is easy to miss when generating them: `return`ing from inside the body skips the update clause, so a loop that paused re-emitted the row it had already handed over. The fix is not uniform, which is the interesting part — an *innermost* loop must step past the emitted row before pausing, while an *outer* loop must deliberately not, because its callee retains its own position and will resume inside that same outer value. The emitter therefore computes `tailmostAt(index)` — true when only filters separate this loop from the terminal — and advances the counter only then.
  Evidence: `vectorTailChunksRunsLongerThanOneVector` failed with `expected: <4103> but was: <4168>`; green after the fix, along with new tests that drain at 1, 2, 3, 5, 7 and 64 rows per call.
- Observation (2026-07-25, root-cause analysis of the branch's four "pre-existing baseline" failures): one of them was not a code defect at all but a **specification conflict the repository could not satisfy**. `LmdbNativeLeftJoinFilterRewriteTest#topLevelLiteralEqualityDisjunctionDoesNotBypassOptimizerPipeline`, added by `5abe495d9b` (2026-07-19, "Align LMDB exact-filter optimizer pipeline"), asserted that a top-level OR-of-equalities must *not* fold into per-value index probes. Commit `bed961bee8` (2026-07-21, "wip") then added both the folding code *and* two tests in `LmdbNativeQueryExplanationTest` asserting that it *must* fold — without retiring the older assertion. From that point no implementation could make the module green. Removing the fold to satisfy the 07-19 test immediately broke the two 07-21 tests (the sweep went 4 failures → 5), which is how the contradiction surfaced. Resolution: the fold stays, because the shared optimizer pipeline only rewrites the `IN` spelling, so leaving OR-of-equals alone degraded it to a full predicate scan with a sticky filter for no semantic gain; the superseded assertion was inverted and documented, and a companion test was added asserting numeric literals still do *not* fold, which pins the actual soundness boundary (`allValueProbeSafeIds` admits only ids whose value-equality coincides with term-identity — `"1"` and `"01"^^xsd:integer` are equal as values but distinct as terms).
  Evidence: `git log -S` dates the two expectations to 07-19 and 07-21 respectively; `LmdbNativeLeftJoinFilterRewriteTest` 13/13 after the resolution, with the explanation tests green again.
- Observation (2026-07-25, found by a test that failed for the wrong reason): in a `ScanQuad` term, {@code -1} means **unbound**, so a term that fails to resolve does not empty the scan — it *widens* it. A query constant absent from the value store resolves to exactly that sentinel, so a lowering that forwarded it would turn `?s <p_absent> ?o` into `?s ?p ?o` and return the whole store instead of nothing: silent, unbounded, and it would present as a caching bug rather than a lowering bug. The interpreted path is already safe here because an absent constant compiles to an empty plan; nothing stated the same requirement for the scan path. Now pinned by `anUnresolvedTermWidensTheScanInsteadOfEmptyingIt`, which asserts the sentinel's meaning outright so the M4 lowering cannot inherit the trap.
  Evidence: a test written to assert "an inner scan that matches nothing yields no rows" returned 5 rows instead of 0, because the absent predicate widened the scan rather than emptying it.
- Note on the OR-of-equals resolution: the production diff on `LmdbNativeAggregatePlanner` is **comment-only** — the code was removed to test the hypothesis and then restored verbatim with documentation. The defect was in the test, and the evidence that it was the test is that removing the code took the module from four failures to five. Runtime behaviour is unchanged from the start of the session.
- Investigated and **refuted** (2026-07-25), recorded because ruling it out is what licenses `ScanQuad` to ship on by default. The hypothesis was a silent wrong-answer path: `LmdbValue.UNKNOWN_ID` is `-1` (`model/LmdbValue.java:20`), which is the *same* sentinel a `ScanQuad` term uses for "unbound"; `ScanQuad`'s emission forwards every operand straight into `scanner.open` with no `-1` guard (`LmdbNativeKernelEmitter.java:1438`, `:1651`), whereas `Probe` and `ProbeClose` guard with `if (key != -1L)` and so treat `-1` as "matches nothing" (`:1736`, `:1776`). If any operand could hold `-1` at runtime the two node kinds would disagree about the same input, and the scan would *widen* — returning unrestricted results — rather than match nothing. `LmdbNativeRowState.bind` does mark a slot bound even when handed `UNKNOWN` (`:71-83`), and `ValuesCursor.next` binds `candidate.values[i]` with no `UNKNOWN` guard (`LmdbNativeRowPlans.java:467`), so the shape looked reachable through a `VALUES` row naming a term absent from the dictionary.
  It is not reachable, and the reason is a deliberate upstream design: an unresolvable term never becomes an id at all. `LmdbNativeAggregateValuesCompiler:281-289` (and the twin at `LmdbNativeAggregatePatternCompiler:582-606`) either substitutes a **synthetic id** allocated before compilation or returns `null`, declining native evaluation for the whole query. Every other `row.bind` call site that could see an unresolved value guards it explicitly (`LmdbNativeJoinPlans:811`, `LmdbNativeLeftJoinPlans:185`, `LmdbNativeLeftJoinMemo:174`, `LmdbNativeAdaptiveFilterPlacement:1118`, `LmdbNativeRowPlans:248`). Columns are written from store data and so are non-negative; the one column kind that can hold `-1`, an OPTIONAL arm's null arm, is protected by ordering — optional arms are lowered strictly after every pattern (`LmdbNativeKernelLowering.java:210`), so no `ScanQuad` can consume one, and `lowerOptionalArm` additionally refuses an optional key with `operandMaybeNull` (`:287`).
  **The invariant to preserve, stated so it cannot rot: no slot that the lowering treats as bound ever holds `UNKNOWN` at runtime — synthetic ids exist precisely to keep "absent from the dictionary" and "unbound" from sharing a representation.** `ScanQuad` depends on it. Anyone adding a new way to seed a row (a new correlated entry source, an external binding set, a new VALUES path) must either uphold it or add the `-1` guard to the scan emission. The IR-level contract in the other direction is already pinned by `LmdbNativeKernelScanQuadTest.anUnresolvedTermWidensTheScanInsteadOfEmptyingIt`.
- Hazard to hand forward (not a bug today): `KernelRuntime.IDENTITY` is a **public, shared, mutable** `int[]`, and `ChunkState.selection` starts out pointing at it. Nothing writes to it today — the emitter does not use `ChunkState` at all yet, and `ChunkState.select(int)` can only install the chunk's own scratch — but the day someone compacts a selection directly into `chunk.selection` without going through `scratchSelection()`, they will corrupt the identity vector for every kernel in the JVM, and the resulting wrong answers will look like anything except what they are. Whoever wires `ChunkState` into the emitter for milestone 2's chunk plumbing should either keep `select(int)` the only mutation path or make the field private behind an accessor.

- Observation (2026-07-25, found while reading the benchmark rerun rather than by any test): **the theme benchmark cannot resolve its own slowest queries at the settings it runs.** The harness uses one 4-second warmup iteration and three 2-second measurement iterations, so a query costing seconds per operation completes roughly one operation per iteration and JMH's reported error exceeds its reported score. Concretely, HIGHLY_CONNECTED q10 measured `1982.438 ± 30974.390` ms/op and ANALYTICS q4 `121.337 ± 226.062`; ANALYTICS q4 has now "regressed 24–32%" and "improved" in alternating runs (`119.454 ± 144.628` on 07-23, `92.221 ± 70.922` on 07-24, `121.337 ± 226.062` on 07-25) purely as an artifact. The consequence for this plan is procedural, not cosmetic: the per-milestone benchmark gate must be judged on *disjoint intervals*, and any milestone claiming a win on a multi-second query owes a dedicated longer-warmup A/B on that query alone — the corpus run cannot supply the evidence. Queries in the sub-100 ms band do resolve cleanly and are where corpus-level claims are legitimate.
  Evidence: `benchmark-significance-2026-07-25.txt`, produced by comparing the summary tables of `results-2026-07-24.md` and `results-2026-07-25.md` with a significance filter.

- Observation (2026-07-25, the most consequential finding of the day, and it reorders the remaining milestones): **the decline census this plan defers to M13 already exists, and it says the top blocker is none of the things M5 or M6 were scheduled to fix.** Every `results-*.md` in the theme-benchmark results directory carries one `### Telemetry Query ###` block per benchmarked pair — 110 of them in `results-2026-07-25.md` — and each block is a full `Explanation.Level.Telemetry` tree in which nodes carry `nativeExecutionPath=` and `nativeStrategyDeclines=`. It is captured *after* JMH warmup, which is the only state in which kernel admission has settled, and it is per plan node rather than per query. No new harness is needed; the data was sitting in the file the benchmark run produced.
  Counted over the 110 pairs (reasons normalised by cutting the trailing `, nativeRuntimeEntryPlan=` field, then deduplicated per pair):

    43 (39.1%)  irKernel:agg:unsupported:JoinPlan
    11 (10.0%)  irKernel:unsupported:ExtensionPlan
     5 ( 4.5%)  irKernel:agg:unsupported:UnionPlan
     4 ( 3.6%)  irKernel:agg:minus-arm:FilterPlan
     4 ( 3.6%)  irKernel:unsupported:PatternPlan
     4 ( 3.6%)  irKernel:agg:unsupported:LeftJoinPlan
     3 ( 2.7%)  irKernel:agg:sticky:BooleanCombinationFilter
     1 each     agg:witness-subplan:MultiValuePatternPlan, agg:unsupported:ExtensionPlan, unsupported:JoinPlan

  The binary nested `JoinPlan` core that the aggregate rung refuses outright leads by a factor of four over everything else. And these decline strings have **zero** occurrences across all 110 pairs: `optional-arm:*`, `pattern-guards`, `below-threshold-or-pending`, `no-columns`, `adjacency-unavailable`, `pattern-ordered-scan`. Three scheduled or proposed work items therefore unlock **nothing** in the benchmarked corpus, and saying so now is cheaper than discovering it after the work: M5's `ScanLeft` (its trigger is an OPTIONAL arm tripping the adjacency guard — `optional-arm:*` never fires), the unscheduled `LeftGroup` container (same string), and any disambiguation of the four ambiguous reason strings (they never appear; `below-threshold-or-pending` cannot even fire after warmup, since the admission floor is eight opens at `ROWS_PER_OPEN_ESTIMATE` 4096 against a 32768-row threshold). `LeftGroup`'s apparent frequency — 110 multi-line OPTIONAL blocks — collapses under per-query counting: 28 of 143 queries have a multi-pattern OPTIONAL, only 9 of those lack a UNION, BIND or sub-SELECT that would block them anyway, and all 9 sit at `z_queryIndex` 11, which `ThemeQueryBenchmark` has commented out of its `@Param` list (indexes 0–10 are benchmarked). M5 remains defensible as capability parity and as the substrate later work sits on; it is not defensible as a benchmark item.
  Evidence: counts reproduced independently from `results-2026-07-25.md`; `@Param` list at `ThemeQueryBenchmark.java:108-123` with `"11"` and `"12"` commented out.
- Observation (2026-07-25, and it converts the 39% blocker from a large milestone into a small one): **`agg:unsupported:JoinPlan` is a symptom, not a cause, and the cause is nearly always a pure `BIND` alias.** `SlotPlan.join(left, right)` (`LmdbNativeSlotPlan.java:44-56`) returns a `MultiJoinPlan` whenever *both* sides satisfy `canFlatten`, and a bare binary `JoinPlan` otherwise. `canFlatten` (`:151-163`) admits only `PatternPlan`, `MultiJoinPlan`, `MultiValuePatternPlan`, a `ValuesPlan` that binds every slot on every row, and a non-sticky `FilterPlan` over one of those. So a `JoinPlan` exists in a plan **precisely because one of its operands is a kind the kernel tier does not support** — the node is the factory's way of saying so. Flattening `JoinPlan` inside the lowering would therefore not unlock a single query on its own; it would move the decline to `agg:child:ExtensionPlan`.
  Which operand? Classifying the entry plans recorded against the 43 declining pairs: **`Extension` appears in 34 of 43**, and the canonical shape is `Join(left=Pattern(...), right=Extension(copies=1, arg=MultiJoin(...)))` — the corpus's `OPTIONAL { pattern . BIND(x AS y) }` idiom. `Union` appears in about 13 and a nested `Minus` in about 11 (the sets overlap). Then the decisive detail: of 57 `BIND(` occurrences in `ThemeQueryCatalog`, **53 are pure variable aliases** of the form `BIND(?x AS ?y)`, which is exactly what the IR's `BindAlias` node already expresses — a node that is implemented, emitter-tested, and merely never constructed by production lowering. Only four are computed (`REPLACE(STR(?p))`, `DATATYPE(?o)`, `CONCAT(STR(?u))`, `?price / ?area`) and so need `LmdbNativeKernelHooks.computeBind`, which still throws.
  **The concrete next step is therefore small and precisely targeted, and it is not what M6 assumed.** Two changes: descend `JoinPlan` in the aggregate rung's core peeling (`LmdbNativeKernelLowering.java:111-137`) collecting leaves left-to-right, which is sound because the emitted pipeline is a nested-loop chain and left-then-right preserves the tree's own binding flow; and lower an `ExtensionPlan` child whose extensions are all pure variable aliases to `BindAlias`. `computeBind` is *not* on the critical path for the corpus and should not gate this work. Expected reach: up to 34 of the 43 `JoinPlan` pairs plus the 11 pairs declining directly on `unsupported:ExtensionPlan`, subject to the `Union`/`Minus` operands in the overlap declining for their own reasons — so the honest prediction is "a large fraction of 45 pairs", not all of them, and the census must be re-run afterwards to say which.
- Observation (2026-07-25, the corollary that should gate every activation item in this plan): **kernel engagement does not imply speedup, and this branch already contains a measured 60x counterexample pointing the other way.** The single largest significant improvement in the 2026-07-25 rerun — ANALYTICS q0, `10.317 → 0.167` ms/op — came from *keeping a kernel out*, not from admitting one. `LmdbNativeGroupStep.java:295-299` states the mechanism in its own comment: the prefix-run distinct/group plan is "the designed fast path for these single-pattern shapes", while "the IR/Janino aggregate kernel would instead enumerate the whole predicate CSR and hash-dedup every row", and so "must not intercept a row the prefix-run path will handle (otherwise e.g. ANALYTICS q0 regresses ~0.16ms -> ~10ms once the kernel is admitted)". This plan's coverage bar — maximal provable subtree in a kernel — is therefore not automatically a performance goal, and M6's activation of DISTINCT and friends is the item most exposed: switching on a dead IR node can steal a row from a faster specialised path. **Every activation from here on owes a "does this steal a faster path?" check, not merely a no-regression sweep**, because a sweep over the ANALYTICS set is exactly what would have caught q0 and exactly what did not exist when the `irAggregate` kernel first landed.
- Correction to this plan's Purpose (2026-07-25, verified in code): the claim that "three entire store configurations get no kernel at all" overstates it and should read *two, one of them only outside the default isolation level*. Untracked datasets is **vacuous**: `csrEligible` follows `trackActiveTxn`, which is false only when the current thread is the sketch estimator's refresh thread (`LmdbSailStore.java:1646-1653`), and that thread never evaluates SPARQL. A write transaction at the default isolation level is **not a kernel problem at all**: `SailDatasetTripleTermSource.collectDatasets` refuses to look through the changeset wrapper, so the native source is null and the *entire* native engine disappears, not just the kernel tier — fixing it is a read-your-writes project in `core/sail/base`, outside this plan. Only inferencing (a composite source, which inherits `adjacency` returning null) is squarely in scope, and even that is query-dependent, since a plan whose every leaf is `EnumerateDomain` or `ScanQuad` already binds and runs against a composite source today.

Pre-registered risks to watch, so that later readers can tell a surprise from an expectation. Flatten placement in a factorized pipeline is the correctness crux: place it too late and results are wrong, too early and factorization buys nothing. Order claims that a node makes about its output must be validated structurally, never assumed — plan 22 recorded a real ORDER BY regression caused by a kernel enumerating adjacency order where the consumer relied on index order. Generated source must stay inside the conservative Janino language subset that plan 19 established (primitive locals, `for`, `if`, method calls, no labelled `continue`, no generics, typed casts instead of bare `null` arguments). Cursor call sites are polymorphic across five `RecordIterator` implementations and will not reach in-memory array speed.

## Decision Log

- Decision: The vectorized and factorized execution model is milestone 1 and 2, before any new data-source node kinds are added.
  Rationale: twelve further node kinds are planned. Building them against the scalar tuple-at-a-time model and then converting would mean writing every one twice. The execution mode is carried on the IR root and included in the shape key, so both models coexist and every later milestone is A/B-testable against the model that already works.
  Date/Author: 2026-07-24 / Claude with hmottestad.
- Decision: Cursor-backed sources are new IR node kinds (`ScanQuad`, `ScanProbe`, `ScanClose`, `ScanLeft`) rather than a source-kind flag on the existing adjacency nodes.
  Rationale: keeps the emitter a total function over node kinds, and leaves the generated code and shape keys of existing CSR kernels byte-identical, so the proven paths carry no regression risk.
  Date/Author: 2026-07-24 / Claude.
- Decision: Everything that is awkward for generated code — checked exceptions, dataset context-set merging, key ranges, statement order selection, cursor pooling — lives engine-side behind a small public interface, and generated source only ever passes four `long` operands plus an integer scan identifier.
  Rationale: this mirrors the existing `KernelHooks` design, which kept SPARQL value semantics out of generated code and has held through three milestones without a single Janino-subset failure.
  Date/Author: 2026-07-24 / Claude.
- Decision: Admission is "always admit" — every lowerable shape takes the kernel rung once the row threshold is crossed, cursor-served or adjacency-served.
  Rationale: user decision, taken for coverage uniformity. The measured risk is that pure unfiltered scans have little code-generation headroom, so a hard no-regression gate on the ANALYTICS benchmark theme is part of every milestone's acceptance, and the recorded fallback if that gate trips is a narrow cost gate for bare unfiltered scan roots only.
  Date/Author: 2026-07-24 / hmottestad.
- Decision: The chunk model (flat versus unflat, i.e. the factorization data structure) lands in milestone 1 together with the vectors, but milestone 1 keeps a deliberately conservative *policy*: a `Flatten` is inserted before every consumer, so a vectorized kernel produces exactly the same rows in exactly the same order as the scalar one. Milestone 2 is then a pure optimization — it removes flattens where the consumer can exploit factorization directly.
  Rationale: the alternative split (vectors first, chunks second) is worse, because a probe in a vectorized-but-unfactorized pipeline must materialize its cross product into vectors, copying every live column per output row. That is more copying than the scalar model does, so milestone 1 would have to be measured as a regression and milestone 2 would have to undo it. Building the data structure once and moving only the policy keeps every milestone monotone, and it makes milestone 2's differential tests a comparison between two live code paths rather than against a remembered baseline.
  Date/Author: 2026-07-24 / Claude.
- Decision: Selection primitives live in `KernelRuntime` as ordinary reviewed Java with a dense and a selection-aware overload each, rather than being emitted per kernel.
  Rationale: the same reasoning plan 19 used for the hash structures — generated source stays small and inside the Janino subset, the tricky part is unit-tested once, and every kernel shares one JIT-compiled copy. The dense/selected pair matters for performance rather than correctness: `column[i]` in a countable loop is a shape the JIT's superword pass can vectorize, whereas `column[selection[i]]` is a gather and is not, so the emitter picks the dense overload whenever the chunk still carries the shared identity selection.
  Date/Author: 2026-07-24 / Claude.
- Decision: Vectorization is introduced as a **vector tail** — the emitter rewrites exactly one site, the innermost run-expanding node, into a bulk read plus vectorized selection, and leaves every other node and the whole terminal untouched.
  Rationale: the innermost expansion is where the row multiplication happens, so it is where the cardinality (and therefore the win) is; every node above it iterates at far lower cardinality. Confining the change to one site means DISTINCT, ORDER BY, LIMIT, aggregate accumulation and the residual-filter protocol are *shared verbatim* between the two modes rather than reimplemented, which removes the entire class of "the vectorized path disagrees with the scalar path" bugs. It also made the change small enough to land with real tests in one step. The remaining node kinds (`LeftProbe`, `Intersect`, and later `ScanQuad`) get the same treatment incrementally.
  Consequence worth recording: because the surviving positions are written back into the same scalar column and the same continuation runs, the 25 pre-existing IR emitter tests now execute *through* the vector tail unchanged and still assert exact results — the mode is covered by the whole existing suite, not only by its own four tests.
  Date/Author: 2026-07-25 / Claude.
- Decision: A run is read through new bulk accessors `NativeAdjacency.copyRun`/`copyContexts` rather than a loop over `neighborAt(int)`.
  Rationale: `neighborAt` is one interface call per element, which defeats both the zero-copy goal and the JIT's superword pass. The interface default keeps every existing implementation (including test fixtures) working unchanged, while the CSR implementation overrides with a single `System.arraycopy` out of its backing array — one call per run instead of one per neighbor.
  Date/Author: 2026-07-25 / Claude.
- Decision: The first factorized consumer is grouped counting, expressed as `updateBy(int n)` alongside the existing `update()`, rather than as new IR structure.
  Rationale: the vector tail already computes how many rows survive a run slice, and when the group key and every counted column are outer columns they are constant for that whole slice — so the count is an addition, which is Kùzu's "counting a factorized result is a multiplication" in its simplest form. Expressing it as a second accumulator entry point keeps group resolution, growth and emission identical to the scalar path. Two soundness details are load-bearing and are pinned by tests: `COUNT(DISTINCT outer)` may fold because adding one value once is indistinguishable from adding it `n` times, and a `cnt > 0` guard is required because folding a zero-length slice would intern a group the scalar loop would never have created.
  Date/Author: 2026-07-25 / Claude.
- Decision: Coverage is measured as "maximal provable subtree, with an audited decline list", not "no interpreted operator anywhere".
  Rationale: user decision. SERVICE, custom tuple functions and the update path are declared stitch-out; everything else must either be generated or call back into the engine from inside generated code, and each remaining decline needs a written justification in the audit allow-list, which may only shrink over time.
  Date/Author: 2026-07-24 / hmottestad.

## Outcomes & Retrospective

(2026-07-25, end of the first working session. Milestones 1, 2's first slice and 3 are complete;
milestone 4's substrate is complete but its lowering is not wired; milestones 5 to 14 are not started.
Ten of fourteen milestones remain — this plan is a multi-week program and the first session delivered
its foundation, not its whole.)

Landed and gated this session, in dependency order: the vectorized substrate and the **vector tail**
(milestone 1); **factorized counting**, including the case where an unfiltered count never touches a
neighbour at all (milestone 2's first slice); **resumable/streaming emission**, which also subsumed the
zero-copy sink originally filed as step 1c (milestone 3); and the **cursor SPI with `ScanQuad`**, proven
against a real on-disk store on the two shapes no adjacency view can express — a variable predicate and
a named graph (milestone 4's substrate).

Verification standard held throughout: `KernelRuntimeTest` 16/16, `LmdbCsrAdjacencyBulkCopyTest` 2/2,
`LmdbNativeKernelIrEmitterTest` 38/38, `LmdbNativeKernelScanQuadTest` 3/3, `LmdbNativeKernelAggregateTest`
7/7, `LmdbNativeKernelExecutionTest` 5/5, `LmdbNativeDifferentialFuzzTest` 24/24, six module sweeps every
one of them at the branch's 4-failure baseline with zero new, and — the gate that matters most for a
default-on change in a query path — **SPARQL 1.1 and 1.2 compliance PASS with zero new failures outside
the 24 frozen identities**.

What exists and is verified: a vectorized execution substrate in `KernelRuntime`; bulk run accessors
on `NativeAdjacency` with a block-copying CSR override; and a **vector tail** that rewrites the
innermost run expansion of `Probe`, `LeftProbe` and `EnumerateAdjKeys` into a bulk read plus vectorized
selection. On top of it, the first factorized consumer: a counting aggregate whose group and counted
columns are constant across a run folds the whole run in one step, and when nothing filters or reads
the neighbours it folds without touching them at all — the count is `rend - rpos`.

The design decision that paid for itself repeatedly was confining vectorization to a single emission
site. Because the tail writes surviving positions back into the same scalar column and calls the same
continuation, DISTINCT, ORDER BY, aggregate accumulation and the residual-filter protocol are shared
verbatim with the scalar path instead of reimplemented. The consequence is that the pre-existing
emitter suite and the entire differential fuzz suite execute *through* the new path unchanged — the
mode arrived with far more coverage than its own tests provide, and no "the vectorized path disagrees"
bug class ever existed.

The lesson worth carrying forward is less comfortable. Every correctness gate this repository has —
34 emitter tests, 24 fuzz rounds, four full module sweeps — was **blind to allocation**, and the first
cut of the vector tail put 24KB of allocation on the per-outer-row path, which is the exact opposite of
this plan's purpose. It was caught by reading the emitted source, not by any test. The same reading
found that the unfiltered count was copying a run it never looked at. Both are now pinned by assertions
against generated source, and that technique — asserting on what the emitter *wrote*, not only on what
it computed — is the one that should be reached for first when the next milestone lands. A correctness
suite cannot see waste.

Three defects were found and fixed, and how each surfaced is the most transferable thing in this
retrospective. Two came from re-reading the *generated source* rather than the Java that produces it —
a 24KB-per-outer-row allocation, and a whole run copied for a count that only needed its length.
Neither could have been caught by any gate in this repository, because every gate here checks answers
and none of them can see waste. The third came from a test: 65 duplicate rows out of 4103, because
`return` from inside a Java `for` body skips the update clause, so a paused loop re-emitted its last
row. The fix for that one is not uniform — the innermost loop must step past the emitted row, an outer
loop must deliberately not — and getting that wrong in either direction is silent. Assert on what the
emitter *wrote*, not only on what it computed.

A second lesson, from wiring milestone 4: construction was implemented, compiled green, and then
deliberately reverted. A scanner owns probes and its owner differs by rung, so landing construction
before the lowering that needs it would have leaked probes the moment the lowering turned on. A
resource without a lifetime is a defect even when it compiles and even when nothing calls it yet.

Honest limits at this point. The speedup is **undemonstrated**: the HC q10 A/B shows no regression
(3253 ± 1388 vs 3307 ± 1256 ms/op) but that query never reaches its engaged steady state in four
iterations and its kernel declines the tail anyway, so it proves absence of harm only. The three
configurations that get no codegen at all — inferencing stores, queries inside write transactions,
untracked datasets — remain uncovered *in production*: milestone 4 proved a kernel can read them, but
until its lowering is wired no query takes that path. And the SPARQL constructs that never reach the
kernel tier because the planner declines them first, sub-`SELECT` above all, are untouched.

The single highest-value next step is milestone 4's lowering: replace the blanket `pattern-guards`
decline in `LmdbNativeKernelLowering` with a per-pattern source choice, carry scan descriptors through
`LmdbNativeKernelBindings`, and construct the scanner with its lifetime owned by each rung. The safety
argument to lean on is that a lowered `ScanQuad` should issue *the same* `probe.open(subj, pred, obj,
context)` call with the same arguments that `PatternPlan.openIterator` already issues, so matching
semantics hold by construction rather than by re-derivation; the guards that must stay are the ones
whose semantics live in `PatternPlan.bind` rather than in the scan itself — `namedContextScope`, a
non-empty `ContextConstraint`, `bindConstant` terms, and an ordered-scan or key-range promise.

## Context and Orientation

Every path below is relative to the repository root, `/Users/havardottestad/Documents/Programming/rdf4j`. The working branch is `optimize-lmdb`. All new Java files must begin with the exact Eclipse Distribution License header used throughout this repository (copy one verbatim from a neighbouring file and set the year to 2026), followed immediately by the line `// Some portions generated by Claude`.

### Terms used in this document

A **statement pattern** is one triple or quad pattern from a SPARQL query, for example `?person foaf:knows ?friend`. Inside the native engine its four positions are subject, predicate, object and context (named graph).

An **identifier** (or **id**) is a 64-bit `long` that the LMDB value store assigns to an RDF term. The engine executes on ids and only converts back to `Value` objects at the very end. The unbound sentinel is `-1`. Some literals are **inlined**: the id itself encodes the value, so no dictionary lookup is needed; `org.eclipse.rdf4j.sail.lmdb.ValueIds` provides `isInlined`, `getIdType`, `getValue`, and a monotone "ordered integer" space where `compareOrderedIntegers` on two ids gives the numeric order directly.

A **CSR adjacency view** is an in-memory compressed sparse row structure built for one `(predicate, direction)` pair: for each key (subject, if the direction is by-subject) it stores a contiguous, sorted run of neighbour ids. Its interface is `NativeLmdbQuerySource.NativeAdjacency` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java` (lines 177 to 206): `denseIdOf(key)` maps a key to a dense ordinal or `-1`; `runStart(dense)` and `runEnd(dense)` give a half-open index range; `neighborAt(index)` and `contextAt(index)` read the run; `keyCount()` and `keyAt(dense)` enumerate keys when supported, returning `-1` from `keyCount()` when not; `runsNeighborOrdered()` says whether runs are sorted by neighbour id. The single production implementation is `CsrNativeAdjacency` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` (line 3011), backed by the cache `LmdbCsrAdjacencyCache` in the same package.

A **cursor** is the alternative: a direct scan of the on-disk LMDB indexes. The interface is `org.eclipse.rdf4j.sail.lmdb.RecordIterator` (public, `@InternalUseOnly`), with `long[] next()` (allocates per record — avoid), `int fill(long[] buffer, int maxRows)` (writes four longs per quad into a caller-owned buffer — the engine-wide convention), `boolean seekForward(subj, pred, obj, context)` (repositions forward, returning `false` when the implementation cannot seek), and `close()`. Cursors are opened through `NativeLmdbQuerySource.NativeProbe.open(subj, pred, obj, context)` where `-1` means unbound, or through the source's own `statements(...)` overloads which additionally accept a `StatementOrder`, an `LmdbKeyRange` (a bounded key range for range pushdown), or an `LmdbRootScanPartition`.

A **prefix-run cursor** is a third access shape: `org.eclipse.rdf4j.sail.lmdb.LmdbPrefixRunCursor` (public) walks *distinct prefixes* of an index rather than every record, with `next()`, `seekTo(long)`, `seekTo(long[])`, `stopBefore(long[])`, `quad()`, and crucially `runRowCount()` which returns how many records share the current prefix without reading them. It is obtained from `NativeLmdbQuerySource.prefixRuns(plan, ...)` given an `LmdbPrefixRunPlan`.

A **`SlotPlan`** is the native engine's compiled physical plan node; the interface is in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSlotPlan.java`. There are fifteen implementations, listed later in this section. A **slot** is an integer index identifying one query variable; a `RowState` holds `long[] slots` plus a bound mask.

**Lowering** means translating a compiled `SlotPlan` tree into a kernel intermediate representation tree plus a bindings descriptor, performing no store access. A **rung** is one attempt in the runtime strategy ladder: it either returns a cursor or returns `null`, recording a decline reason and letting the next rung try. A **shape key** is a canonical string rendering of an IR tree; kernels are compiled once per shape key and cached.

**Vectorized execution** means a column is a `long[]` array holding many values, and operators process a whole array per call, instead of one scalar value per call. **Factorized execution** (as in the Kùzu graph database) means that when one key expands to *n* neighbours, the engine does *not* materialize *n* rows: it keeps the key as a single "flat" value alongside an "unflat" list of the *n* neighbours, so the logical result is the Cartesian product of the parts and its cardinality is a multiplication. **Flattening** converts an unflat group back into one row per element, and must happen before any operation whose semantics need individual rows.

### The kernel tier as it exists today

The public service-provider interface that generated code is allowed to touch lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/`. It contains exactly five files. `JaninoKernel.java` declares the contract every generated class implements: `void bind(KernelContext)` called once, `int fill(long[] rowBuffer, int maxRows)` returning packed row-major ids with `0` meaning exhausted, and a default no-op `close()`. `KernelContext.java` is the bind-time input struct with public final fields `adjacencies`, `constants`, `entrySlots`, `keyDomains`, `hooks`. `KernelHooks.java` is the call-back interface for anything the kernel cannot decide on ids alone: `testFilter(int filterId, long a0, long a1, long a2)`, `computeBind(int bindId, long a0, long a1)`, `compareValues(long, long)`, `isNumeric(long)`, `doubleValue(long)`. `KernelRuntime.java` holds hand-written data structures shared by generated code: `LongHashSet`, `LongIntMap`, `RowSet`, `sortRows`, `topKRows`. `package-info.java` records why everything here must be public: generated classes are loaded in a child class loader, which puts them in a different runtime package even though the package name matches, so package-private access does not work.

The engine-side classes are all package-private finals in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`. `LmdbNativeKernelIr.java` (about 1050 lines) is the intermediate representation. `LmdbNativeKernelEmitter.java` (about 1400 lines) turns an IR tree into Java source. `LmdbNativeKernelLowering.java` (about 1040 lines) recognizes `SlotPlan` trees and builds IR. `LmdbNativeKernelBindings.java` holds the per-open descriptor and assembles the `KernelContext`. `LmdbNativeKernelHooks.java` is the engine-side `KernelHooks` implementation. `LmdbNativeKernelExecution.java` is the front door, exposing `tryOpenRows` and `tryEvaluateAggregate`. `LmdbNativeJaninoCodegen.java` is the compile service. `LmdbNativeJaninoPipeline.java` and `LmdbNativeJaninoAggregate.java` are the two older shape-specific generators that predate the IR and are scheduled for retirement in milestone 14.

The IR today has fifteen node kinds. Six of them reference a CSR adjacency by index and are therefore unusable without one: `EnumerateAdjKeys` (iterate a view's keys, optionally expanding each key's run in the same loop), `Probe` (look a key up and loop its run into a new column), `ProbeClose` (both endpoints known; count matches, or in semi mode stop at the first), `Intersect` (k-way sorted intersection over k views — the worst-case-optimal join building block), `LeftProbe` (OPTIONAL: run the continuation once with `-1` when the key has no run), and `PathExpand` (breadth-first `+`/`*` path expansion with a visited set). Nine are adjacency-free: `EnumerateDomain` (walk a pre-collected `long[]`), `EnumerateEntry` (one row seeded from the enclosing bindings), `FilterCompareId`, `FilterInConstants`, `FilterRangeUnsigned`, `FilterValue` (delegates to `hooks.testFilter`), `BindAlias`, `BindHook`, plus the containers `Exists` (a short-circuiting boolean sub-pipeline, negated for NOT EXISTS and MINUS) and `Union`. There are two terminals: `Emit` (named output columns, with an optional `distinct` flag) and `Aggregate` (group columns plus accumulators COUNT/COUNT(x)/COUNT DISTINCT/SUM/MIN/MAX, an optional HAVING guard). Both terminals carry an `OutputMods` record holding ORDER BY keys, descending flags, a value-order flag, a limit and an offset.

An important and initially surprising fact: a large part of that IR is **dead in production**. The lowering only ever constructs `EnumerateAdjKeys`, `EnumerateDomain`, `Probe`, `ProbeClose`, `LeftProbe`, `FilterCompareId`, `FilterInConstants`, `FilterValue`, `Exists`, and the two terminals — always with `OutputMods.none()`. `Intersect`, `PathExpand`, `BindAlias`, `BindHook`, `EnumerateEntry`, `FilterRangeUnsigned`, `Union`, `Emit.distinct`, and every non-empty `OutputMods` are constructed only by tests. `LmdbNativeKernelHooks.computeBind` throws `UnsupportedOperationException`, so BIND cannot be lowered at all. In other words, DISTINCT, ORDER BY, LIMIT and OFFSET are already implemented and tested inside the kernel tier and simply never used.

### Where kernels are consulted at runtime

On the row side, `NativeRowsStep.openUnorderedInput` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java` (around line 815) tries, in order: worst-case-optimal leapfrog join; the old `LmdbNativeJaninoPipeline` generator (line 825, guarded on the plan root being a `MultiJoinPlan`, and carrying a stray `System.out.println("JaninoPipeline")` at line 827 that must be deleted in milestone 14); the IR rung `LmdbNativeKernelExecution.tryOpenRows` (line 833); then batch, factorized, adaptive and nested-loop strategies. On the aggregate side, `NativeGroupIteration.evaluateAll` in `LmdbNativeGroupStep.java` (around line 270) tries an exists-intersection collapse, then the old `LmdbNativeJaninoAggregate` (line 287), then the IR rung `tryEvaluateAggregate` (line 300) — but only when `prefixRunHandlesRow(row)` is false (line 299). That guard exists solely because the IR aggregate rung once stole a faster prefix-run path and regressed one ANALYTICS benchmark query from 0.16 ms to 10.3 ms; removing it by making the kernel genuinely faster is an acceptance criterion of milestone 10.

The compile service `LmdbNativeJaninoCodegen` caches compiled classes keyed by shape key, compiles asynchronously on a single daemon thread named `lmdb-janino-codegen`, admits a shape only once observed rows reach a threshold, memoizes failed shapes so they are not recompiled, and can dump generated source for debugging. Its system properties are `rdf4j.lmdb.janinoCodegen.enabled` (default true), `rdf4j.lmdb.janinoCodegen.thresholdRows` (default 32768), `rdf4j.lmdb.janinoCodegen.maxEntries` (default 128) and `rdf4j.lmdb.janinoCodegen.dumpDir` (unset). A separate undeclared property `rdf4j.lmdb.janinoCodegen.debug` turns on decline tracing in the lowering and execution classes. Because compilation is asynchronous, tests must warm repeatedly until an engagement counter moves rather than executing a fixed small number of times; the established helper pattern is called `warmUntilEngaged` and appears in `LmdbNativeJaninoPipelineTest`.

One subtlety that has already caused a bug: `LmdbNativeKernelExecution` caches kernels under a *static* owner object, deliberately, because IR kernels are pure shape and every store-specific input arrives through the bind-time context. Any new descriptor must respect that — it may contribute structure to the shape key but must not bake in store-specific ids, or the cache will miss on every plan.

### Why patterns decline today

The decisive guard is in `LmdbNativeKernelLowering.java` at line 395:

    if (pattern.hasRepeatedSlot() || pattern.namedContextScope || !pattern.p.isConstant()
            || pattern.p.hasSlot() || pattern.c.hasSlot() || pattern.c.isConstant()
            || pattern.contexts.isFixed() || pattern.s.bindConstant || pattern.o.bindConstant) {
        reason = "pattern-guards";
        return false;
    }
    if (pattern.statementOrder != null) { reason = "pattern-ordered-scan"; return false; }

Decoded, a pattern cannot be lowered when it has a repeated variable, is scoped to named graphs, has a non-constant or slot-bound predicate, mentions the context position at all, is restricted by a `FROM`/`FROM NAMED` dataset, binds a constant term that must still be projected, or carries an index-order promise. The same predicate appears in the two older generators. Independently, at bind time `LmdbNativeKernelBindings.requestAdjacencies` returns `null` — declining with the reason `adjacency-unavailable` — when any requested view is missing or lacks key enumeration.

Three store configurations never provide views at all. Composite sources (`CompositeNativeLmdbQuerySource.CompositeProbe`, line 561) do not override `adjacency`, inheriting the interface default of `null`; that is the explicit-plus-inferred configuration, so **any store with inferencing enabled gets no kernel**. Inside a write transaction the cache returns `null` so readers observe their own writes, and every entry is dropped after each commit. Untracked datasets are marked ineligible.

A related lost optimization: `PatternPlan.range` (numeric range pushdown) is ignored by all three generators, so a kernel re-scans a full run where the interpreter would have scanned a bounded key range. This is a performance loss, not a correctness one, because the planner keeps the original FILTER above the ranged pattern and the lowering re-applies it.

### The fifteen `SlotPlan` kinds and their kernel coverage

`EmptyPlan` and `SingletonPlan` are trivial constants. `PatternPlan` is the only leaf that reads the store. `MultiValuePatternPlan` is a pattern with a folded `IN`-set on one slot. `MultiJoinPlan` is a flattened inner-join bag with masked filters and a memoized join order. `JoinPlan` is a binary nested-loop join. `LeftJoinPlan` is OPTIONAL. `UnionPlan` and `OrderedUnionPlan` are UNION, the latter a streaming merge for order-compatible branches. `FilterPlan` wraps a boolean filter. `ExtensionPlan` is BIND. `MinusPlan` is anti-join on shared slots. `ValuesPlan` is an inline VALUES table. `PathPlan` is an arbitrary-length property path. `LeapfrogPlan` is a fused worst-case-optimal join stage.

Kernels currently cover `PatternPlan`, `MultiValuePatternPlan`, `ValuesPlan`, `MultiJoinPlan`, `FilterPlan`, `LeftJoinPlan` (single bare pattern arms only), and `MinusPlan` on the aggregate side only. Not covered by any generator: `JoinPlan`, `UnionPlan`, `OrderedUnionPlan`, `ExtensionPlan`, `PathPlan`, `LeapfrogPlan`, and row-side `MinusPlan`.

### Constructs that never reach the kernel tier

Above the kernel tier sits the native planner. Its construct whitelist is the method `compileTuple` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregatePlanner.java` (lines 1088 to 1229), which handles `SingletonSet`, `StatementPattern`, `ArbitraryLengthPath`, `Join`, `LeftJoin`, `Union`, `Difference`, `Filter`, `Extension` and `BindingSetAssignment`, and returns `null` for anything else. Unhandled anywhere in the native engine: `Projection` — which means **every sub-`SELECT` breaks the query root** — plus `MultiProjection` (CONSTRUCT), `DescribeOperator`, `ZeroLengthPath`, `TripleRef` (RDF-star patterns; note that triple *terms* as values already flow through as ordinary ids), `Service`, and `TupleFunctionCall`. Arbitrary-length paths require minimum length at most one, so `{n,m}` ranges decline. `GROUP_CONCAT` and `SAMPLE` have no native aggregate. ORDER BY accepts only plain variables. ASK produces a `Slice` with no `Projection`, so its root declines. BIND declines the entire root when its expression is not inlinable. A global cap of sixty variables applies, and the IR caps columns at sixty-four.

Fallback is per-operator and silent: the native strategy extends the generic RDF4J strategy and overrides `precompile`, so when a subtree declines, the generic evaluator handles that node and then re-offers each of its children to the native compiler.

### Correctness gates that already exist and must keep passing

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeDifferentialFuzzTest.java` is the primary gate: each round forces code generation on with a zero row threshold, runs generated queries through both the kernel path and the generic evaluator, and asserts identical result multisets plus a non-zero engagement counter. The module sweep is judged by reading the Surefire and Failsafe report XML files and comparing failing classes against the branch's known pre-existing set (as of 2026-07-24: four failures across `LmdbNativeFeatureFlagForkTest` and `LmdbNativeLeftJoinFilterRewriteTest`; the branch is under concurrent work, so re-baseline at run time). SPARQL compliance is gated by `plans/lmdb-native-engine/COMPLIANCE-BASELINE.md` and `compliance/sparql/lmdb-compliance-baseline.json`, which freeze twenty-four known LMDB-only failures; the checker `scripts/check-lmdb-compliance-baseline.py` fails closed on anything outside that set. That baseline was frozen on 2026-07-18 and last observed on 2026-07-20 while the engine has moved since, so it must be re-observed before being trusted as a comparison point.

Because code generation defaults to on, any test that asserts which interpreted strategy ran must pin `rdf4j.lmdb.janinoCodegen.enabled=false` for its class. Seven classes already do this; expect to add more as kernel absorption widens, and treat "expected strategy X but got irKernel" as an expected test-pinning fix rather than a regression.

## Plan of Work

The work is fourteen milestones. Each ends with a system that runs, is measured, and is independently revertible. The narrative below says what exists at the end of each one and how you will know it works.

### Milestone 1 — vector and chunk substrate, and zero-copy output

Today a generated kernel keeps each result column in a scalar `long` field named `v0`, `v1` and so on, and produces one row at a time through a chain of nested loops. At the end of this milestone a kernel can instead keep each column in a `long[]` array and process a whole array per operation, and it can write its results straight into the consumer's column arrays instead of copying them three times.

Add to `KernelRuntime` the vector substrate: a fixed vector capacity constant (start at 2048 and make it a tunable system property `rdf4j.lmdb.janinoCodegen.vectorSize`), a `ChunkState` holding `int size`, `int[] selection` and `int flatIndex` where `-1` means unflat, and helpers to compact a selection vector after a filter. Filters must not branch per element: they compute a predicate result into the selection array and compact, which keeps the inner loops countable so that the HotSpot C2 compiler's SuperWord pass can auto-vectorize them.

Add a new public interface `KernelBatchSink` in the `codegen` package exposing `long[] column(int index)`, `int capacity()` and `void produced(int rows)`, and give `JaninoKernel` a `default int fillBatch(KernelBatchSink sink)` so that existing kernels compile unchanged. The engine-side implementation adapts the package-private `LmdbNativeBatch`; the sink interface is precisely the public seam that avoids widening that class.

Introduce a per-query arena for kernel buffers, reusing `LmdbNativeLongArena` and `Pool`. Every vector, selection array, staging quad buffer, hash table and group table is allocated once during `bind` and released in `close`. Generated code must never allocate inside a loop; in particular cursors are read only through `fill(long[], int)`, never through `next()`, which allocates a `long[]` per record.

Carry an execution mode on the IR root, include it in the shape key, and add a vectorized emitter path alongside the existing scalar one covering the node kinds that already exist. The scalar emitter stays until milestone 14.

The chunk data structure lands here, but the *policy* stays conservative: a `Flatten` is inserted before every consumer, so a vectorized kernel emits exactly the rows the scalar one does, in the same order. Milestone 2 changes only the policy. This ordering keeps both milestones monotone improvements; the alternative — vectors now, chunks later — would make a probe materialize its cross product into vectors, copying every live column per output row, so milestone 1 would measure as a regression that milestone 2 then undoes.

Acceptance: the existing IR emitter test suite passes in both execution modes; the differential fuzz suite passes with the vectorized mode forced on; a Java Flight Recorder profile of a row-heavy benchmark query shows bytes allocated per output row lower than the scalar baseline, with no per-record `long[]` allocation in the TLAB profile; no benchmark regresses.

### Milestone 2 — factorization

At the end of this milestone, a kernel that expands one key into a run of neighbours no longer materializes one row per neighbour unless something downstream actually needs individual rows.

Model a factorized intermediate result as several chunks: a chunk is flat when it holds exactly one logical value (its `flatIndex` is non-negative) and unflat when it holds many. The logical cardinality of the whole intermediate result is the product of the unflat chunk sizes. A probe leaves its key chunk flat and produces its neighbours as a new unflat chunk.

Consumers that can exploit this do so directly. `COUNT(*)` becomes a multiplication rather than an enumeration — which is exactly what the interpreted `LmdbNativeFactorizedTail` count branch already does, and is the reference for expected results. `EXISTS` becomes a non-empty test. A grouped aggregate whose group columns all live in flat chunks accumulates once per run instead of once per row.

Add a `Flatten(chunkId)` IR node that unnests one chunk, and make the lowering insert it exactly where semantics require: before a filter that reads two different unflat chunks, before DISTINCT across an unflat column, before ORDER BY, and before any row-shaped consumer. Validate the requirement structurally when the IR is constructed rather than discovering it at runtime.

Because milestone 1 landed as a vector tail, milestone 2 has a first slice that needs no new node at
all and should be taken before the general chunk plumbing. The tail loop already computes `cnt`, the
number of surviving neighbors in the current run slice, then iterates it purely to hand the terminal
one row at a time. When the terminal is an `Aggregate` whose group columns are all *outer* columns
(flat with respect to the tail) and whose outputs are `COUNT(*)` or counts of outer columns, that
iteration is waste: the group is constant across the slice, so the kernel can add `cnt` to the
accumulator once and skip the loop entirely. That is precisely Kùzu's "counting a factorized result is
a multiplication", expressed here as deleting a loop — the cheapest possible demonstration of the
factorized win, and a strict subset of the general machinery that follows.

Acceptance: differential fuzz rounds covering factorizable shapes pass; the denormalized view benchmark queries (theme indices 11 and 12, which produce between 100 thousand and 950 thousand rows) improve measurably against the flattened baseline; a targeted test proves that a factorized `COUNT(*)` reads no neighbour values at all.

### Milestone 3 — resumable, bounded emission

Today `fill` runs the entire pipeline on its first call, appending to a doubling output array, and only then serves rows. That is tolerable while sources are adjacency-bounded. Once milestone 4 lets a kernel scan the whole store, a `?s ?p ?o` row kernel would materialize every statement in memory.

Make `Emit`-terminal kernels resumable: each loop node keeps its induction variable and its open cursor in a field; the row sink raises a `full` flag when the caller's `maxRows` is reached; every loop checks that flag after invoking its continuation and returns; each node method resumes from its saved index on re-entry. Cursor sources resume for free because the cursor holds its own position. Aggregate terminals keep running to completion because they are bounded by group count, except the streaming aggregate introduced in milestone 10. Register buffer memory with `LmdbQueryMemoryManager` like every other engine buffer.

Two things about this milestone became clear while building milestones 1 and 2, and are worth stating
before anyone starts it.

First, **this is the same work as the zero-copy sink** originally filed as step 1c. Today a row is
copied three times: `rowScratch` into the growing `out` buffer, `out` into the caller's buffer inside
`fill`, and finally into `RowState` by the wrapper cursor. The middle copy exists only because `run()`
produces every row before `fill` knows where the caller wants them. Once the pipeline is resumable it
can write *directly* into the caller's buffer, which deletes the `out` buffer entirely for `Emit`
terminals — no growth, no second copy. So resumability is not merely a memory bound; it removes two of
the three copies. Do not build a separate sink abstraction first.

Second, the mechanics that make it work. Give every loop node an induction field with `-1` meaning
"not started", so a node re-initializes lazily and resumes otherwise. The rule that keeps paused and
finished apart is uniform and belongs in every node: **after calling your continuation, return
immediately if `full`, and otherwise reset your callee's state field.** A node that pauses leaves its
callee's state intact so the callee resumes where it stopped; a node whose callee ran to completion
resets it so the next outer value starts fresh. Nothing needs to distinguish the two cases explicitly.
Correctness of the resumed row depends on outer columns being fields that the outer loop rewrites
before re-entering the callee, which is already true.

Scope v1 to `Emit` terminals with `OutputMods.none()` (ordering and limits need the materialized
buffer) over chains of `EnumerateDomain`, `EnumerateAdjKeys`, `Probe`, `LeftProbe` and filters; DISTINCT
is fine because its `RowSet` check happens before the write. The vector tail needs two state fields
rather than one — the run position and the position within the current slice — and on resume it
re-copies the slice and re-runs the vectorized filters from the saved run position, which is
deterministic and costs one slice of redundant work per `fill` boundary. `LeftProbe` additionally needs
its `matched` flag promoted to a field so a pause inside the run cannot resurrect the null arm.
Everything else keeps the current eager path.

Acceptance: a test drains a kernel over a store larger than the output buffer and asserts both exact results and a bounded peak buffer size; the existing suites stay green.

### Milestone 4 — the cursor service-provider interface and `ScanQuad`

This is the milestone that makes the headline claim true: queries that read LMDB directly, rather than through an in-memory adjacency view, get generated code.

Add three public interfaces to the `codegen` package. `KernelScanner` exposes `KernelQuadCursor open(int scanId, long subj, long pred, long obj, long context)` and `KernelPrefixCursor openPrefix(int scanId, long subj, long pred, long obj, long context)`. `KernelQuadCursor` exposes `int fill(long[] quadBuffer, int maxRows)`, `boolean seekForward(long s, long p, long o, long c)` and `void close()`. `KernelPrefixCursor` exposes `next`, `seekTo(long)`, `seekTo(long[])`, `stopBefore(long[])`, `quad()`, `runRowCount()` and `close()`. None of them declare checked exceptions.

Add a `scanner` field to `KernelContext` with a new constructor, leaving the existing constructors delegating with `null` — the same additive pattern used when `hooks` was added. Implement the engine side in a new package-private class `LmdbNativeKernelScanner`, which owns everything generated code should not see: converting `IOException` to an unchecked exception, merging dataset context sets, applying an `LmdbKeyRange`, selecting a `StatementOrder`, honouring named-graph scoping and the null-context sentinel, and pooling cursors. A scan descriptor registered at lowering time resolves the integer `scanId`; generated source only ever passes four operand longs.

No visibility widening is required anywhere: `NativeLmdbQuerySource` and its nested interfaces, `RecordIterator`, `TripleIndex` with its position constants, `LmdbKeyRange`, `LmdbPrefixRunPlan`, `LmdbPrefixRunCursor`, `LmdbRootScanPartition` and `ValueIds` are all already public.

Add the `ScanQuad` IR node carrying a scan identifier, four operands, the output columns for the unbound positions, and a declared order. Replace the blanket `pattern-guards` decline with a per-pattern source choice: use an adjacency view when the pattern is expressible that way and one is expected to be available, otherwise emit a `ScanQuad`. Repeated variables inside one pattern lower as a `ScanQuad` plus the existing `FilterCompareId` equality, reusing a primitive rather than inventing one. For a pattern that is adjacency-expressible but whose view is missing at bind time, keep today's behaviour of recording demand and declining for the first few opens so the view can build, and only re-lower in cursor form after a bounded number of consecutive `adjacency-unavailable` declines; this preserves the mandatory kernel-before-probe ordering that plan 20 established to keep probe accounting intact.

Note that adjacency views do carry contexts and the emitter simply never calls `contextAt`. A named-graph column can therefore be served from an adjacency view as well as from a cursor, and the lowering should prefer that when available rather than forcing every GRAPH pattern onto a scan.

Acceptance: a red-first engagement test with a `GRAPH ?g` query and another with a variable predicate assert a non-zero open counter and exact parity with the generic evaluator; equivalent tests pass against an inferencing (composite-source) store and inside a write transaction, both of which get zero code generation today; a new differential fuzz round covers quad and variable-predicate shapes.

### Milestone 5 — correlated cursor probes, witnesses and OPTIONAL

Add `ScanProbe`, `ScanClose` (with a semi mode) and `ScanLeft` as the cursor twins of `Probe`, `ProbeClose` and `LeftProbe`, and give `PathExpand` a cursor source. A single kernel must be able to mix adjacency and cursor sources, so the lowering chooses per pattern and the emitter handles both in one tree.

Acceptance: engagement tests for a correlated cursor probe, a cursor-served EXISTS witness and a cursor-served OPTIONAL; a mixed adjacency-plus-cursor kernel test; fuzz round extended.

### Milestone 6 — terminal modifiers and activation of the dead IR

Purely lowering work; the IR and emitter already implement and test all of it. Make the production lowering construct `Emit.distinct` and non-empty `OutputMods` so that DISTINCT, ORDER BY, LIMIT and OFFSET run inside kernels, and activate `Union`, `BindAlias`, `BindHook`, `Intersect`, `PathExpand`, `EnumerateEntry` and `FilterRangeUnsigned`. BIND additionally requires implementing `LmdbNativeKernelHooks.computeBind`, which currently throws.

Acceptance: one engagement test per newly activated node kind; fuzz rounds for DISTINCT, ORDER BY with limit and offset, UNION, BIND and property paths.

### Milestone 7 — ordering as a first-class contract

Give every IR node a declared output order: which columns are sorted and in which comparison space, unsigned identifier order or SPARQL value order through the hooks. Nodes establish, preserve or destroy order, and the IR root validates what the consumer requires. This removes the `pattern-ordered-scan` decline, because a cursor scan is natively index-ordered — the capability that adjacency views could not express is restored rather than merely worked around. When the producer already establishes the order the query asked for, the kernel declares its output ordered and no sort layer is added.

Acceptance: a test proving an index-satisfied ORDER BY runs in-kernel with no sort; a multi-key ordering differential test; the regression that forced the original decline (`LmdbSailStoreTest.testOrderByLmdbIndexPreservesJoinOrder`) is green with the guard removed.

### Milestone 8 — merge join and skip-ahead

Add a `MergeJoin` node that merges two order-compatible inputs using `seekForward` and `seekTo` instead of building a hash table. The interpreted `LmdbNativeMergeJoin`, including its rejection of pre-ordered patterns and its single-key restriction, is the semantics reference.

Acceptance: engagement test on a merge-join shape with exact parity, and a benchmark A/B against the interpreted merge join.

### Milestone 9 — prefix runs, range scans and DISTINCT-driven skipping

Add `ScanRange` (the kernel form of range pushdown, recovering the pruning kernels currently lose) and `EnumeratePrefixRuns`, which walks distinct prefixes through a `KernelPrefixCursor` and can use `runRowCount()` to produce grouped counts without reading rows. Add a `skipMode` on scan nodes derived from what the consumer demands: when only distinct leading-column values are needed, the scan calls `seekTo` to jump past the rest of a run instead of walking it. Model this as a contract between nodes, not a peephole optimization.

This is the milestone that must *win* on the ANALYTICS benchmark theme rather than merely avoid regressing it, because those queries are scan-bound and are the main risk of the always-admit decision.

Acceptance: engagement plus parity tests for prefix-run counts, ranged scans and skip-driven DISTINCT; ANALYTICS improves.

### Milestone 10 — streaming DISTINCT and streaming GROUP BY

Add `OrderedDedup`, which implements DISTINCT by counting transitions in a sorted run in constant memory and falls back to the hash form when order is absent, and `OrderedAggregate`, which emits and resets at each group transition so no group table is needed.

Acceptance: the `prefixRunHandlesRow` guard at `LmdbNativeGroupStep.java:299` is deleted and the ANALYTICS query it protects (the one that regressed from 0.16 ms to 10.3 ms) is at or better than its pre-kernel timing.

### Milestone 11 — the inlined-literal identifier tier

Many literals are encoded directly in their identifier. Emit per-identifier type checks inline — for example testing whether an id is in the ordered-integer space and comparing directly when it is — with a hooks call only in the slow branch. This gives exact semantics *and* removes the call-back from the hot path, replacing the compromise plan 20 recorded, where numeric ranges were sent to the hook tier because the static range node could not express a per-identifier proof. In vectorized mode these become branch-free masked operations over a whole vector. Apply the same treatment to comparisons and ranges, numeric BIND arithmetic, string operations over short inlined strings, date comparisons, the numeric predicates used by SUM, MIN, MAX and AVG, and ORDER BY key comparison. This also unblocks AVG, which declines outright today, through exact decimal accumulation over inlined decimal identifiers.

Acceptance: a test proving a numeric range filter runs with zero hook calls; AVG engagement test with exact parity against the generic evaluator including mixed-type inputs; benchmark improvement on the filter-heavy theme.

### Milestone 12 — remaining physical-strategy parity

Hash join and semi-join sets built on `KernelRuntime.LongHashSet` and `RowSet`, with `LmdbNativeMembership` as the semantics reference; cursor-source leapfrog intersection using `seekTo`; partitioned parallel roots using the existing `planRootScanPartitions`, `prefixRunSplitValues` and `openParallelSources`, one kernel instance and one arena per worker; top-k through the existing `KernelRuntime.topKRows`; path memoization and target sets; and the `SlotPlan` kinds no generator covers today.

### Milestone 13 — planner construct growth and the coverage audit

Grow `compileTuple` so that the missing constructs can reach a kernel at all, starting with `Projection` so that sub-`SELECT` stops breaking the query root (lowered as a materialized domain feeding the outer kernel), then ASK roots, `MultiProjection` and `DescribeOperator`, `GROUP_CONCAT` and `SAMPLE`, non-variable GROUP BY and ORDER BY expressions, `{n,m}` path ranges, `ZeroLengthPath`, and `TripleRef`. Declare `Service`, `TupleFunctionCall` and the update path as stitch-out with written justification.

Build the audit harness: run the SPARQL 1.1 and 1.2 compliance query corpora and the 143-query theme catalog with decline tracing on, and produce a per-query report giving the fraction of the plan tree claimed by kernels plus every decline reason. Acceptance is that every reason appears on an explicit allow-list with a justification, and that the list only shrinks in later runs.

### Milestone 14 — combination closure, rung-order flip, retirement

Build a generator-driven combination matrix over source kind, factorization state, flatten placement, ordering, skip mode, OPTIONAL, UNION, witness, aggregate kind, DISTINCT, ORDER BY and LIMIT, compiling and running each combination against the interpreter for exact parity in both execution modes. Then run a shadowing sweep with both the old and new generators enabled and explanation recording on; when every engagement of the old generators is also lowerable by the IR path, flip the rung order so the IR rungs come first, and retire `LmdbNativeJaninoPipeline`, `LmdbNativeJaninoAggregate`, the scalar emitter, and the stray debug print at `LmdbNativeRowStep.java:827`.

## Concrete Steps

All commands are run from the repository root, `/Users/havardottestad/Documents/Programming/rdf4j`.

Before starting, and again before any test run, publish all modules to the workspace-local Maven repository. This typically takes around thirty seconds; never use a timeout below sixty seconds:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{s=1} s{print}'

While iterating on the module only, compile it and its dependencies with tests skipped:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install 2>&1 | tail -5

Run tests one selection at a time through the repository's test runner. Never pass `-am` or `-q` when running tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelIrEmitterTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelExecutionTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelAggregateTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelLoweringTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Check SPARQL compliance in an isolated workspace, then judge it with the baseline checker rather than by reading Maven's exit code:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace agent-compliance-baseline compliance/sparql --retain-logs -- -Dmaven.test.failure.ignore=true
    python3 scripts/check-lmdb-compliance-baseline.py .mvnf/workspaces/agent-compliance-baseline/build/org.eclipse.rdf4j/rdf4j-sparql-compliance/6.1.0-SNAPSHOT/failsafe-reports

Run a single benchmark, optionally with Java Flight Recorder profiling for the allocation gate:

    scripts/run-single-benchmark.sh --theme-query ANALYTICS:0
    scripts/run-single-benchmark.sh --theme-query MEDICAL_RECORDS:11 --enable-jfr

Before finalizing any milestone, check headers and format:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

To see why a kernel declined, or to read the generated source:

    -Drdf4j.lmdb.janinoCodegen.debug=true
    -Drdf4j.lmdb.janinoCodegen.dumpDir=/tmp/kernels

## Validation and Acceptance

Each milestone follows the same rhythm. Write the engagement test first and observe it fail with a message naming the missing capability, typically a zero engagement counter or a decline reason. Implement until it passes. Add a differential fuzz round that forces code generation on with a zero row threshold, compares result multisets against the generic evaluator, and asserts the engagement counter moved. Re-run the intermediate-representation emitter test suite in both execution modes. Run the module sweep and judge it by reading the report XML files, accepting only the branch's known pre-existing failures. Run the compliance module and require the checker to report a subset of the twenty-four frozen identities. Run the benchmark gate: no ANALYTICS query may regress by more than five percent, and from milestone 1 onward the Java Flight Recorder profile must show bytes allocated per output row at or below the previous milestone.

Behaviour with code generation disabled must remain byte-identical throughout. Verify this by running the fuzz suite with `rdf4j.lmdb.janinoCodegen.enabled=false` and asserting every kernel counter stays at zero.

The final acceptance for the plan as a whole is the audit report from milestone 13 together with the combination matrix from milestone 14: every query in the compliance corpora and the theme catalog either executes its maximal provable subtree inside a kernel, or appears in the decline allow-list with a written justification.

## Compliance status

**SPARQL 1.1 + 1.2 compliance re-verified 2026-07-25 after M3 and M4 landed**: `LMDB compliance baseline
gate: PASS`, 30 reports parsed, required suites 5/5, 17 of the 24 frozen failures remaining, 7 resolved,
**zero new**. This run matters more than the earlier one: it is the first to include resumable emission,
which is default-on and sits directly in the row-production path. An earlier pass had been recorded
before M3 and M4 existed, which left a verification gap — worth remembering that a green compliance
result ages the moment a default-on path changes underneath it.

## Baseline for judging a module sweep

As of 2026-07-25 the `core/sail/lmdb` sweep stands at **2210 tests, 3 failures, 0 errors, 3 skipped**,
and all three failures are `LmdbNativeFeatureFlagForkTest`. Earlier notes in this document quote a
four-failure baseline across two classes; that was reduced by fixing the specification conflict
recorded in Surprises & Discoveries, so treat three as the number a clean run must match.

The three that remain are strategy-preference assertions, not correctness ones: each fork scenario
checks result parity against the generic evaluator *before* it checks the strategy label, and those
parity checks pass. What changed underneath them is that the factorized-rows engine now claims a plain
three-pattern chain (`factorizedRows(flatPrefix=1, prefix=chain, enumBranches=1, countBranches=1)`)
where the chunk pipeline used to win, following the order-selection work committed on 2026-07-21. The
test froze its expectations on 2026-07-20, one day earlier.

Deciding them needs a measurement rather than an edit: the chunk pipeline benchmarks roughly 2.5x
faster than a row chain on this shape, so if `prefix=chain` really is what runs, the fork tests are
reporting a genuine performance regression and the engine should be fixed. If instead the factorized
claim is the better plan for this shape, the three expectations are stale and should be updated the way
the OR-of-equals conflict was. Do not flip them without the A/B, and note that the change they track is
uncommitted work owned by a concurrent session.

## Operating what has landed

Two switches matter in production, both read at runtime with no rebuild:

    -Drdf4j.lmdb.janinoCodegen.vectorTail=false   # disable the vector tail; kernels revert to the scalar emitter
    -Drdf4j.lmdb.janinoCodegen.resumable=false    # disable streaming; kernels materialize before serving, as before
    -Drdf4j.lmdb.janinoCodegen.enabled=false      # disable runtime code generation entirely

The first two are the targeted rollbacks for what this plan introduced, and they are independent: one
reverts how a run is read, the other reverts how rows are handed back. Because both are different
emissions of the *same* intermediate representation, and because both are part of the shape key,
turning either off makes the next compiled kernel for every shape use the path that predates this work
— there is no migration, no stale cache entry, and no partially-converted state. The third is the
pre-existing kill switch for the whole kernel tier and remains the blunt instrument.

A third, `-Drdf4j.lmdb.janinoCodegen.vectorSize=<n>` (default 2048, clamped to [64, 65536] and rounded
down to a multiple of 64), tunes the slice width. Lowering it reduces the peak scratch a kernel holds;
raising it lengthens the vectorized runs. It does not change results.

To see what a kernel actually compiled to, set `-Drdf4j.lmdb.janinoCodegen.dumpDir=<dir>`; every
generated (and every failed) source is written there. Reading that output is how both defects recorded
in Surprises & Discoveries were found, and it is the recommended first step when diagnosing a kernel.

## Idempotence and Recovery

Every milestone is additive and independently revertible. New service-provider interfaces are new files; `KernelContext` and `JaninoKernel` grow by fields and default methods with existing constructors and callers untouched; new intermediate-representation node kinds do not alter the generated code or shape keys of existing ones; the vectorized execution model coexists with the scalar one behind a mode carried on the intermediate-representation root. Reverting a milestone means deleting its new files and its lowering cases. Kernel caches are keyed by shape and are rebuilt automatically. Nothing is committed without an explicit request from the user, and untracked artifacts are preserved.

The two changes that are not purely additive are called out here so they can be reverted precisely: removing the `prefixRunHandlesRow` guard in milestone 10 (one condition), and flipping the rung order in milestone 14 (two call sites). Both are gated on their own evidence.

## Artifacts and Notes

Milestone 1, step 1a (2026-07-24). Root install before starting:

    mvn -B -ntp … -Pquick clean install    →  BUILD SUCCESS, 32.213 s (Wall Clock)

Substrate unit tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py KernelRuntimeTest
    [mvnf] Root install passed: BUILD SUCCESS 19.105 s
    [mvnf] Tests passed.
    [mvnf] Summary: tests=16, failures=0, errors=0, skipped=0, time=0.040s
    core/sail/lmdb/target/surefire-reports/TEST-org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelRuntimeTest.xml

Sixteen is eight pre-existing tests plus eight new ones covering: the vector size and identity
invariants; chunk state defaults, scratch allocation and selection installation; equality and
inequality selection in dense, selection-aware and chained forms; in-place selection rewriting;
unsigned range selection including the `-1` sentinel as unsigned maximum and `Long.MIN_VALUE` as the
midpoint; column-to-column comparison and boundness; gather, broadcast and identity fill; and a
randomized cross-check of three primitives against a scalar reference over data seeded with `0` and
`-1`.

Hygiene for the same step:

    (cd scripts && ./checkCopyrightPresent.sh)  →  All files have valid copyright headers and SPDX lines.
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources  →  BUILD SUCCESS (no reformatting needed)

Note for whoever runs the formatter next: it must be invoked from the repository root. Running it
from inside `scripts/` fails with `MissingProjectException`, which is a wrong working directory, not
a formatting error.

Milestone 1, step 1b (2026-07-25). The vector tail, proved engaged rather than merely present:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCsrAdjacencyBulkCopyTest
    Summary: tests=2, failures=0, errors=0, skipped=0, time=0.356s

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelIrEmitterTest
    Summary: tests=29, failures=0, errors=0, skipped=0, time=0.403s

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    Summary: tests=24, failures=0, errors=0, skipped=0, time=20.114s

Engagement is asserted against the emitted source, not inferred: the qualifying kernel's source must
contain `.copyRun(` and `KernelRuntime.selectNe(tvec, rn,`, the same shape built with the mode off
must contain neither, and the two shape keys must differ so the compiled-kernel cache cannot serve one
for the other. Four dedicated tests cover engagement, a chained selection with a residual hook guard
inside the vectorized loop, a run of `VECTOR_SIZE * 2 + 7` neighbors (so the chunking `while` loop
takes three slices and every neighbor still appears exactly once, in order), and the claiming rules —
the innermost of two chained probes wins, key enumeration qualifies on its own, and an `Exists` after
the probe declines back to scalar.

The stronger evidence is indirect: because the vector tail writes surviving positions back into the
same scalar column and calls the same continuation, the 25 pre-existing emitter tests and the whole
differential fuzz suite now execute *through* it, unchanged, and still assert exact results against
the generic evaluator.

Milestone 2 first slice and the `LeftProbe` extension (2026-07-25) grew the same suite:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelIrEmitterTest
    Summary: tests=34, failures=0, errors=0, skipped=0, time=0.408s

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelAggregateTest
    Summary: tests=7, failures=0, errors=0, skipped=0, time=0.949s

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    Summary: tests=24, failures=0, errors=0, skipped=0, time=19.517s

The factorized-count tests assert the generated source contains `updateBy(cnt);` and `agC0[g] += n;`
and does *not* materialize the tail column per row, that the decline cases (`COUNT(DISTINCT tail)`,
`SUM`, grouping by the tail column) keep the loop, and that a fully filtered slice never interns a
group. The `LeftProbe` tests pin the two things that could plausibly break: a matched-but-then-filtered
run must not resurrect the null row, and the null row must itself be subject to the trailing filters.

Module sweep for the same step, judged by reading the report XML files rather than the runner's exit
code (the runner exits non-zero whenever any test fails, including the branch's known ones):

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    tests=2191 failures=4 errors=0 skipped=3
      org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeFeatureFlagForkTest: failures=3
      org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeLeftJoinFilterRewriteTest: failures=1

That is exactly the pre-existing baseline recorded by plans 20 to 22, so this step introduced no new
failures. Independence from the change is not merely asserted: `git diff --numstat` on
`KernelRuntime.java` reports 240 insertions and 0 deletions, so no existing line moved, and a search
for the new names across `src/main` returns only the definition site itself. (A search across
`src/test` also matches `LmdbNativeSubstrateBenchmark.java`, but those are the unrelated identifiers
`ExternalRootChunkState` and `externalRootChunkSelect` in a tracked, unmodified file.)

## Interfaces and Dependencies

No new external dependencies; Janino is already present.

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelScanner.java`, public, annotated `@Experimental` and `@InternalUseOnly`:

    public interface KernelScanner {
        KernelQuadCursor open(int scanId, long subj, long pred, long obj, long context);
        KernelPrefixCursor openPrefix(int scanId, long subj, long pred, long obj, long context);
    }

In the same package, `KernelQuadCursor.java`:

    public interface KernelQuadCursor {
        int fill(long[] quadBuffer, int maxRows);
        boolean seekForward(long subj, long pred, long obj, long context);
        void close();
    }

In the same package, `KernelPrefixCursor.java`:

    public interface KernelPrefixCursor {
        boolean next();
        boolean seekTo(long value);
        boolean seekTo(long[] prefixValues);
        void stopBefore(long[] prefixValues);
        long[] quad();
        long runRowCount();
        void close();
    }

In the same package, `KernelBatchSink.java`:

    public interface KernelBatchSink {
        long[] column(int index);
        int capacity();
        void produced(int rows);
    }

`KernelContext` gains `public final KernelScanner scanner;` and a constructor accepting it, with the existing constructors delegating and passing `null`. `JaninoKernel` gains `default int fillBatch(KernelBatchSink sink)` returning zero, so existing generated classes remain valid.

Engine-side, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`, a new package-private `LmdbNativeKernelScanner` implements `KernelScanner` over `NativeLmdbQuerySource.NativeProbe` and `LmdbPrefixRunCursor`, resolving scan descriptors registered by `LmdbNativeKernelBindings` and converting checked exceptions.

---

Revision note (2026-07-24, initial): authored on user approval of the planning document at `/Users/havardottestad/.claude/plans/queried-that-use-lmdb-fizzy-globe.md`, after three exploration passes verified every integration fact cited above against the working tree.
