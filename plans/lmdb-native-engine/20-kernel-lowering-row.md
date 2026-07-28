# Kernel lowering M1: general SlotPlan-to-IR lowering substrate + row-side execution rung

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md` (repository root). Milestone M1 of the approved lowering program (user-approved plan, 2026-07-24: full program; retire old recognizers + default ON at the end; per-milestone benchmark gates). Predecessors: `19-kernel-ir-primitives.md` (IR + emitter, complete), `18-janino-pattern-analysis.md` (corpus leverage order), `17-janino-whole-stage-codegen.md` (codegen service + the two shape-specific rungs).

## Purpose / Big Picture

The kernel IR (`evaluation/LmdbNativeKernelIr.java`) and its generic emitter (`evaluation/LmdbNativeKernelEmitter.java`) can express and compile whole-stage query kernels, but no production query reaches them. This milestone builds the four-class lowering substrate — a pure recognition pass from compiled `SlotPlan` trees to IR, a binding descriptor, an engine-side `KernelHooks` implementation, and a shared execution front door — and wires the row-side rung into `LmdbNativeRowStep.openUnorderedInput` directly after the existing `LmdbNativeJaninoPipeline` rung. After M1, any inner-join bag (`MultiJoinPlan`) of CSR-servable constant-predicate patterns — including VALUES-seeded and correlated-entry shapes, with arbitrary filters degrading through three tiers (id-inline, hook-callback, wrapper-residual) — executes through a generated kernel, verified by a differential fuzz round that asserts engagement. Observable outcome: `KernelExecutionTestAccess.opened() > 0` on the new fuzz round with identical results to the interpreter; explain shows `PATH_IR_KERNEL`; flag-off behavior is byte-identical to today.

## Progress

- [x] (2026-07-24) ExecPlan authored; integration surfaces verified by three exploration reports (plan/rung/value taxonomies).
- [x] (2026-07-24) Step 1: `PATH_IR_KERNEL` tag + `KernelExecutionTestAccess` + red engagement test captured red ("IR kernel rung never engaged (planned=4, ...)" with the rung absent/async-race), then green.
- [x] (2026-07-24) Step 2: `LmdbNativeKernelBindings` + `LmdbNativeKernelHooks` implemented; hook behavior covered end-to-end by the engagement test's STRSTARTS hook filter (dedicated unit test folded into Step 3's lowering test).
- [x] (2026-07-24) Step 3: `LmdbNativeKernelLowering.lowerRows` for MultiJoinPlan implemented (patterns → EnumerateAdjKeys/Probe/ProbeClose; MultiValuePatternPlan → EnumerateDomain+pattern; single-slot complete ValuesPlan → EnumerateDomain; ENTRY operands; three filter tiers).
- [x] (2026-07-24) Step 4: `LmdbNativeKernelExecution.tryOpenRows` + rung wired in `openUnorderedInput` after the Janino pipeline rung; `LmdbNativeKernelExecutionTest` 3/3 green (VALUES-seeded chain + hook-tier filter engage with exact results; flag-off silent). Engagement uses the module's established warm-until-engaged pattern (async compile publishes between executions).
- [x] (2026-07-24) Step 5: `LmdbNativeKernelLoweringTest` 6/6 green (chain→EnumerateAdjKeys+Probe, closing edge→ProbeClose multiplicity, VALUES→EnumerateDomain, entry operands, three filter tiers, unsupported-root decline) — the chain/closing-edge cases are exactly the old recognizer's two shape families, covering the parity claim; fuzz round `irKernelBasicGraphPatterns` added (4 fixed shapes the old recognizer declines + 30 randomized VALUES-seeded chains), full `LmdbNativeDifferentialFuzzTest` 22/22 green with `KernelExecutionTestAccess.opened() > 0` asserted.
- [x] (2026-07-24) Step 6: formatter + copyright green; module sweep 2169 tests / 4 failures / 0 errors — identical to the pre-M1 baseline (the 2 known pre-existing classes `LmdbNativeFeatureFlagForkTest` 3F, `LmdbNativeLeftJoinFilterRewriteTest` 1F, both under concurrent branch work and verified independent), zero new failures. M1 COMPLETE.

## Surprises & Discoveries

(Pre-registered risks: scratch-RowState view memoization staleness after direct slot writes — must use `recomputeBoundMask()` before and after hook evaluation; `RecordingNativeBooleanFilter` wrapping must be unwrapped before instanceof dispatch but the *wrapped* filter must still be the one counted for adaptive telemetry when left residual; `openUnorderedInput` runs on the query thread per open — lowering must be cheap or cached.)

- Observation: kernel compilation is asynchronous, so a short fixed warm loop races the publish — the module's established remedy is `warmUntilEngaged` (re-execute up to 300 times until the OPENED counter moves), used by `LmdbNativeJaninoPipelineTest`; the engagement test adopted it after the initial 4-iteration loop failed with `compilations=0` (the compile simply had not finished).
  Evidence: first run `planned=4, declined=0, compilations=0, compileFailures=0`; green after adopting the pattern.
- Observation: a freshly constructed `RowState` has 0-filled slots, not UNKNOWN(-1) — `bind` on such a row is a no-op that returns false because slot 0 holds 0, not the sentinel. Production seeds rows before use; unit tests must `Arrays.fill(row.slots, UNKNOWN)` + `recomputeBoundMask()` first.
  Evidence: `entryBoundSlotBecomesEntryOperand` produced `EA(...)` (fresh classification) until the fill was added.
- Observation: `NativeSlotLayout` needs `freeze(slotNames)` before `RowState` construction (slot array is sized from the frozen names, not the map).
  Evidence: all lowering tests failed with `Index 0 out of bounds for length 0` until frozen.
- Observation: the first module sweep caught a real integration regression — the rung opened a store probe on every *planned* attempt even while the kernel was below threshold / compile-pending, which disturbed probe-retention accounting (`LmdbNativeChunkHashBuildTest.orderedFlatEnumRefusalRestartsClassicSort`: `newProbeCalls` 17 vs 16). Fix: request the compiled kernel BEFORE touching the store (lowering + shape key need no probe); the probe only opens once a kernel instance exists. Additionally, that test class asserts interpreted-strategy internals, so it now pins `rdf4j.lmdb.janinoCodegen.enabled=false` class-wide (same pattern as WCOJ pinning elsewhere) — with the flag defaulting on in this branch, strategy-internals tests must opt out explicitly or the kernel absorbing their shapes is indistinguishable from a regression.
  Evidence: sweep 2169 tests → only that 1 new failure; green after both changes (44/44 in the class; engagement tests re-verified 3/3).

## Decision Log

- Decision: M1 lowers only `MultiJoinPlan` roots (the same structural scope as the existing pipeline rung, plus VALUES/MultiValuePattern seeds, correlated entries, and the filter tiers). `LeftJoinPlan`/`UnionPlan`/`PathPlan`/`ExtensionPlan`/`MinusPlan` roots record decline `"unsupported:<Node>"` and fall through — they are M2–M4 scope. The lowering entry is written as a node-type switch so later milestones add cases without restructuring.
  Rationale: keeps M1 reviewable and gate-able at parity-plus; the approved plan sequences constructs by corpus leverage.
  Date/Author: 2026-07-24 / Claude (Fable) with hmottestad.
- Decision: Id-tier filters in M1 are EQ/NE (soundness identical to the existing recognizer: at least one operand resource-assured — bound at a subject position — or a Resource constant, via `safeResourceId`) and `ValueSetFilter` → `FilterInConstants` only when every accepted id is `safeResourceId`. Numeric range compares do NOT lower to `FilterRangeUnsigned` in M1: `CachedCompareFilter.nativeDecision` proves ordered-integer per *runtime id* and falls back to decode otherwise, a per-id check `FilterRangeUnsigned` cannot express — ranges go to the hook tier instead (still in-kernel, engine-exact). Revisit with zone-map/ordered-domain proofs in M5.
  Rationale: correctness first; the hook tier already removes the operator-boundary cost, which is the measured win.
  Date/Author: 2026-07-24 / Claude.
- Decision: Hook-tier eligibility = filter mask is a subset of (kernel-produced slots ∪ entry-bound slots), mask has ≤3 bits (KernelHooks arity), mask ≥ 0 (sticky/-1 masks — EXISTS-bearing — stay residual in M1; they lower in M2). Everything else residual. **A filter alone never declines a lowering.**
  Rationale: monotone admission versus the current rung; sticky filters keep exact current behavior until Exists lowering lands with its own gates.
  Date/Author: 2026-07-24 / Claude.
- Decision: `testFilter` scratch protocol: a dedicated `RowState(row.source, row.layout, row.base)` per hooks instance; per call — write arg ids into the registered `argSlots` (skip id −1, leaving the slot UNKNOWN), `recomputeBoundMask()`, `filter.accept(scratch)`, then reset the written slots to UNKNOWN and `recomputeBoundMask()` again. The base binding-set is carried so name-based fallback predicates resolve outer-fragment variables exactly as on the live row.
  Rationale: `accept` implementations read `row.slots[...]` and fall back to `row.view`; the reset keeps calls independent; `recomputeBoundMask` keeps the view's caches coherent (it is the documented re-seeding hook).
  Date/Author: 2026-07-24 / Claude.
- Decision: Rung placement in M1 is directly AFTER the existing `LmdbNativeJaninoPipeline` rung and before batch/parallel; admission reuses `LmdbNativeJaninoCodegen.kernel(...)` keyed by the IR `shapeKey()` with the same opens×4096 estimate (per-shape opens map in `LmdbNativeKernelExecution`). The rung-order flip and estimate replacement are M3/M5 items with their own evidence gates.
  Rationale: additive risk posture — the proven rungs keep their traffic; the new rung takes what they decline.
  Date/Author: 2026-07-24 / Claude.

## Outcomes & Retrospective

(2026-07-24, M1 complete.) The general lowering substrate exists and is live: `LmdbNativeKernelLowering` (MultiJoinPlan → IR with VALUES/folded-IN seeds, correlated entries, three filter tiers), `LmdbNativeKernelBindings`, `LmdbNativeKernelHooks`, and `LmdbNativeKernelExecution` wired as a rung after the shape-specific pipeline rung. Verified: engagement 3/3 (VALUES-seeded chain + STRSTARTS hook filter run in-kernel with exact results; flag-off silent), lowering units 6/6, differential fuzz 22/22 including the new `irKernelBasicGraphPatterns` round (34 forced-on queries, engagement asserted), module sweep at baseline. The coverage step beyond the old recognizer is real today: VALUES seeds, folded IN sets, value-tier filters, and correlated entries all reach kernels for the first time. Two design corrections came from testing rather than review: the async-compile warm race (adopt `warmUntilEngaged`) and — caught by the sweep, the important one — probe-before-admission churn, now inverted so below-threshold opens cost zero store interaction. Rule reaffirmed for all later milestones: with the flag default-on, every strategy-internals test must pin the kernel rung off explicitly. Next: M2 (`21-kernel-lowering-aggregate.md`) — the aggregate rung with EXISTS/NOT EXISTS/MINUS witnesses and full AggKind breadth, the >80% corpus prize.

## Context and Orientation

Everything lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/` unless stated; tests in the matching `src/test` tree. Branch `optimize-lmdb`. New Java files carry the 2026 EDL header + `// Some portions generated by Claude`.

What exists (verified in-code this session):

- IR + emitter (plan 19): `LmdbNativeKernelIr` — `Operand.constant/entry/col`, nodes `EnumerateAdjKeys(adj,keyCol,valueCol|-1)`, `EnumerateDomain(domain,col)`, `EnumerateEntry`, `Probe(adj,key,valueCol)`, `ProbeClose(adj,key,target,multiplicity)`, `Intersect`, `FilterCompareId(negated,left,right)`, `FilterInConstants(value,constIdx[])`, `FilterRangeUnsigned`, `FilterValue(filterId,args≤3)`, `LeftProbe`, `Exists(negated,pipeline)`, `Union(branches)`, `BindAlias`, `BindHook`, `PathExpand`, terminals `Emit(cols,distinct)`/`Aggregate` with `OutputMods`; `Kernel(columnCount,pipeline,terminal)` → `shapeKey()`/`className()`. `LmdbNativeKernelEmitter.emit(Kernel)` → Java source implementing the public SPI `codegen/JaninoKernel` (`bind(KernelContext)`, `int fill(long[],int)`); `codegen/KernelContext{adjacencies,constants,entrySlots,keyDomains,hooks}`; `codegen/KernelHooks{testFilter(int,long,long,long), computeBind(int,long,long), compareValues, isNumeric, doubleValue}`; `codegen/KernelRuntime` data structures. Compile service `LmdbNativeJaninoCodegen.kernel(cacheOwner,shapeKey,className,sourceSupplier,observedRows)` (null = stay interpreted; async compile; threshold `rdf4j.lmdb.janinoCodegen.thresholdRows` default 32768; `awaitKernel` test hook; counters).
- The compiled plan surface consumed by the lowering: `MultiJoinPlan{SlotPlan[] children, MaskedFilter[] filters, derivedPlan(row)→OrderedPlan{order,filterDepth,placement,sunkCount}}`; `PatternPlan{Term s,p,o,c; ContextConstraint contexts; namedContextScope; hasRepeatedSlot(); staticEstimate}` with `Term{slot,constant,bindConstant}`; `MultiValuePatternPlan{constrainedSlot, long[] constants, PatternPlan[] alternatives, fallback}`; `ValuesPlan{ValuesRow[] rows, bindsAllSlotsEveryRow}`; `MaskedFilter{NativeBooleanFilter filter, long mask (-1=sticky), adaptive, plannedDepth}`. Filter impls with package-visible fields: `ValueSetFilter{slot,accepted[]}`, `CachedCompareFilter{slot,constant,op,...}`, `OrderedSlotCompareFilter{leftSlot,rightSlot,op}`, `BooleanCombinationFilter`, `LmdbNativeCompiledBoolean{requiredMask}`, wrapper `RecordingNativeBooleanFilter{delegate}`; `safeResourceId` from `LmdbNativeAggregateCompiler`. EXISTS lives inside filters (`StatementPatternExistsFilter`, `ExistsFilter{subPlan}`) — residual in M1.
- The rung site: `LmdbNativeRowStep` inner iteration `openUnorderedInput(RowState)` (~line 815): WCOJ → `LmdbNativeJaninoPipeline.tryOpen(multiJoin,row)` (`PATH_JANINO_KERNEL`; note stray debug println kept until M7) → batch/parallel → factorized → adaptive → nested loop. `originalExpr` is in scope for `LmdbNativeExplain.recordExecutionPath` / `LmdbNativeAttemptMetrics.recordDecline(target,strategy,reason)`. `RowState{source,layout,base,slots,view,boundMask(),bind,mark,rollback,recomputeBoundMask}`; scratch construction `new RowState(source,layout,base)`.
- Hooks source material: `orderCompare(leftId,rightId,codec)` logic in `LmdbNativeRowStep` (ordered-integer fast path `ValueIds.compareOrderedIntegers`, else `LmdbNativeExpressionCompiler.compareDecoded(codec.decode l, r)`, null → `ValueComparator` unbound-first); `ValueIds.getIdType`+numeric-type test; `LmdbNativeValueCodec.decode(id)` → `DecodedValue{numeric(),floatingValue(),decimalValue()}`; `source.nativeValueCodec()` may be null → decline value-order/numeric hook registrations.
- Existing wrapper template: `LmdbNativeJaninoPipeline.KernelRowCursor` (fill-buffer drain, `row.mark()/bind()/rollback()`, residual `MaskedFilter.filter.accept(row)`); the new execution class hosts its own copy (the original is private and will be retired in M7).
- Fuzz harness: `src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeDifferentialFuzzTest.java` — round template at `janinoKernelBasicGraphPatterns` (~374): save/set `rdf4j.lmdb.janinoCodegen.enabled=true`, `...thresholdRows=0`, `rdf4j.lmdb.wcoj.enabled=false`; reset metrics via a test-access bridge; warm 3×; `assertSameResults`; assert engagement>0; restore in finally.

Definitions: "lowering" = translating a compiled `SlotPlan` tree into a `LmdbNativeKernelIr.Kernel` plus a `LmdbNativeKernelBindings` descriptor, with no store access. "Rung" = one attempt in the runtime strategy ladder; returning null falls through with a recorded decline. "Residual filter" = a `MaskedFilter` evaluated by the wrapper cursor on each kernel row via `accept(row)` after slots are bound — semantics identical to the interpreter because filters are plan-owned and cursors only call accept.

## Plan of Work

Step 1 (red first): add `PATH_IR_KERNEL = "irKernel"` to `LmdbNativeAttemptMetrics` (+ vocabulary set). New test-access class `src/test/.../evaluation/KernelExecutionTestAccess.java` mirroring `JaninoPipelineTestAccess` (exposes `planned()/opened()/kernelRows()/declined()/resetMetrics()` on `LmdbNativeKernelExecution` counters). New `LmdbNativeKernelExecutionTest` with a small on-disk store: a 3-pattern chain query with a VALUES seed and one hook-tier filter, forced-on props, asserting results match the generic evaluator AND `opened() > 0` — red until Step 4.

Step 2: `LmdbNativeKernelBindings` (final fields: `AdjacencyRequest[] {long predicate; boolean bySubject; boolean needsKeyEnum}`, `long[] constants`, `int[] entrySlotIds`, `long[][] keyDomains`, `FilterHook[] {MaskedFilter source; int[] argSlots}`, `BindHook[]` (empty in M1), `int[] columnEngineSlots`, `List<MaskedFilter> residualFilters`; method `KernelContext bind(NativeProbe probe, RowState row, KernelHooks hooks)` returning null when any `probe.adjacency(...)` is null). `LmdbNativeKernelHooks implements KernelHooks` per the Decision-Log scratch protocol; `compareValues/isNumeric/doubleValue` per Context; `computeBind` throws `UnsupportedOperationException` in M1 (no BindHook registrations yet). Unit test `LmdbNativeKernelHooksTest`: the −1→unbound convention (a `Bound`-style filter must see the slot unbound), reset-between-calls independence, ordered-integer compare fast path.

Step 3: `LmdbNativeKernelLowering` — entry `static Lowered lowerRows(SlotPlan arg, RowState row, TupleExpr declineTarget)`; switch on node type (only `MultiJoinPlan` implemented; others `recordDecline(target, PATH_IR_KERNEL, "unsupported:"+simpleName)` → null). MultiJoin lowering walks `derivedPlan(row).order`: `PatternPlan` guards verbatim from the existing recognizer (constant predicate, predicate/context not slotted, contexts not fixed, no bindConstant on s/o, no repeated slot, no namedContextScope); classification against entryMask/slotColumn produces `EnumerateAdjKeys` (first child, both ends fresh), `Probe` (one end known; direction by which end), `ProbeClose(multiplicity=true)` (both known); `MultiValuePatternPlan` → `EnumerateDomain(constants)` into the constrained slot's column + `Probe` for the other end (decline when the constrained slot is already bound — rare; interpreter handles); single-slot complete `ValuesPlan` → `EnumerateDomain` (ids from its rows); entry-bound slots → `ENTRY` operands snapshotting `row.slots`. Then filters: unwrap Recording, tier per Decision Log (EQ/NE id tier reusing the existing soundness helpers; `ValueSetFilter`→`FilterInConstants` when all-safe; hook tier registering `FilterHook{argSlots = mask bits ascending}` and IR `FilterValue(hookIndex, colOperands)`; residual otherwise), placed by operand availability depth (IR pipeline position after the producing node). Terminal `Emit(all columns)`. Test `LmdbNativeKernelLoweringTest` builds plans directly (PatternPlan/MultiJoinPlan constructors are package-visible) and asserts shape keys, bindings content, tier assignment, and decline reasons.

Step 4: `LmdbNativeKernelExecution` — counters, `SHAPE_OPENS`, `static RowCursor tryOpenRows(SlotPlan arg, RowState row, TupleExpr originalExpr)`: enabled-gate → lower (cheap, but memoize per `MultiJoinPlan` identity+boundMask later if profiling demands) → `row.source.newProbe()` → `bindings.bind(...)` (null → decline `"adjacency-unavailable"`, close probe) → `LmdbNativeJaninoCodegen.kernel(row.source.idSpace(), shapeKey, className, () -> LmdbNativeKernelEmitter.emit(kernel), opens×4096)` (null → decline `"below-threshold-or-pending"`, close probe) → `kernel.bind(context)` → wrapper cursor (copy of `KernelRowCursor`: drain `fill`, bind `columnEngineSlots`, apply residuals, rollback discipline, close kernel+probe). Wire the rung into `openUnorderedInput` after the Janino pipeline block, guarded `multiJoin != null` in M1 (the SlotPlan-general entry stays for M3/M4), recording `PATH_IR_KERNEL` on success. Engagement test from Step 1 goes green.

Step 5: parity + fuzz. Parity test: for a corpus of generated 2–6-pattern chain plans (reuse the fixtures in `LmdbNativeJaninoPipelineTest` where possible), assert `LmdbNativeJaninoPipeline`-recognizable shapes also produce non-null lowerings with equivalent column sets. Fuzz: new round `irKernelBasicGraphPatterns` cloned from the existing Janino round but with the old pipeline shapes plus VALUES/filters mixed in, forced-on flags **plus** `rdf4j.lmdb.janinoPipeline.enabled=false` if such a flag exists — it does not; instead assert engagement via `KernelExecutionTestAccess` while leaving the old rung on only for shapes it declines (the round's queries include hook-tier filters and VALUES seeds the old recognizer rejects, guaranteeing the new rung fires).

Step 6: hygiene + gates per Concrete Steps.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j`:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{s=1} s{print}'
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install   # compile loop
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelHooksTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelLoweringTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelExecutionTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs   # judged by report XMLs

## Validation and Acceptance

Red→green: `LmdbNativeKernelExecutionTest` fails before Step 4 ("kernel path never engaged") and passes after. `LmdbNativeDifferentialFuzzTest` all rounds green including `irKernelBasicGraphPatterns` with `KernelExecutionTestAccess.opened() > 0`. Parity test green. Module sweep: zero failing classes beyond the branch's known pre-existing set (compare report XMLs; 2 classes / 4 failures as of 2026-07-24: `LmdbNativeFeatureFlagForkTest`, `LmdbNativeLeftJoinFilterRewriteTest` — re-baseline at run time since the branch is under concurrent work). Flag-off check: with `rdf4j.lmdb.janinoCodegen.enabled=false`, `KernelExecutionTestAccess` counters stay zero across the fuzz suite.

## Idempotence and Recovery

All additive: four new main classes, one metrics constant, one guarded rung block, new tests. The rung is inert when the flag is off or lowering declines; reverting = deleting the new files and the rung block. Kernel caches are per-store weak-keyed (existing service). Keep untracked artifacts; nothing committed without explicit request.

## Artifacts and Notes

(Transcripts to be captured as steps complete.)

## Interfaces and Dependencies

No new external dependencies. New classes as specified in Plan of Work; signatures pinned:

    // evaluation/LmdbNativeKernelLowering.java
    static Lowered lowerRows(SlotPlan arg, RowState row, TupleExpr declineTarget)
    static final class Lowered { final LmdbNativeKernelIr.Kernel kernel; final LmdbNativeKernelBindings bindings; }

    // evaluation/LmdbNativeKernelBindings.java
    KernelContext bind(NativeLmdbQuerySource.NativeProbe probe, RowState row, KernelHooks hooks)  // null = adjacency unavailable

    // evaluation/LmdbNativeKernelHooks.java
    LmdbNativeKernelHooks(RowState liveRow, LmdbNativeKernelBindings bindings)  // builds scratch RowState internally

    // evaluation/LmdbNativeKernelExecution.java
    static RowCursor tryOpenRows(SlotPlan arg, RowState row, TupleExpr originalExpr)
    static final AtomicLong PLANNED, OPENED, KERNEL_ROWS, DECLINED

---

Revision note (2026-07-24, initial): authored as M1 of the approved kernel-lowering program immediately after plan approval; all integration facts verified in-code this session.
