# Kernel lowering M2: aggregate rung with EXISTS/NOT-EXISTS/MINUS witnesses and aggregate breadth

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained per `.agent/PLANS.md`. Milestone M2 of the approved lowering program (predecessor M1: `20-kernel-lowering-row.md`, complete — general row-side lowering substrate `LmdbNativeKernelLowering`/`Bindings`/`Hooks`/`Execution` live behind `PATH_IR_KERNEL`). Corpus rationale: `18-janino-pattern-analysis.md` — >80% of theme-corpus runtime is aggregation-consumer shapes gated on witness filters; prize query HIGHLY_CONNECTED q10 (5.8s: candidate scan → NOT EXISTS two-hop witness → COUNT DISTINCT).

## Purpose / Big Picture

After M2, native aggregations (`NativeGroupIteration.evaluateAll` in `LmdbNativeGroupStep.java`) can execute through IR-lowered kernels: grouped or global, multi-column group keys, COUNT/COUNT(x)/COUNT(DISTINCT x)/SUM/MIN/MAX outputs (AVG declines, see Decision Log), with EXISTS / NOT EXISTS witness filters and MINUS arms fused into the kernel as short-circuit sub-pipelines. HAVING keeps its current architecture (a generic FilteringIteration above the native step reading the anonymous `_anon_having_*` twin spec — the kernel simply produces that spec as another output). Observable outcomes: a new `PATH_IR_AGGREGATE` execution path with engagement tests and three forced-on differential fuzz rounds; the HC q10-shaped benchmark shows the witness fusion win; module sweep and SPARQL compliance baseline stay at their known pre-existing failures.

## Progress

- [x] (2026-07-24) ExecPlan authored; all integration facts verified in-code this session.
- [x] (2026-07-24) Step 1: `AGG_MIN_ID`/`AGG_MAX_ID` in IR + emitter; `LmdbNativeKernelIrEmitterTest` 25/25 (value-order winners; NULL on empty input).
- [x] (2026-07-24) Step 2: `NegatedNativeBooleanFilter` replacing the `Not` lambda; fuzz suite green pre/post (Routine-B evidence).
- [x] (2026-07-24) Step 3: SUM exactness guard in `LmdbNativeKernelHooks` (`armSumGuard`/`sumsExact`; integer-DATATYPE-family requirement — an integral-valued xsd:decimal still promotes the result type, so it fails the guard; bound count×maxAbs < 2^53; non-numeric inputs invalidate because generic SUM poisons to omitted binding).
- [x] (2026-07-24) Step 4: `lowerAggregate` — peels `FilterPlan` wrappers (sticky witnesses never flatten into MultiJoinPlan!) and `MinusPlan` arms off the root; cores: MultiJoinPlan | PatternPlan | MultiValuePatternPlan | ValuesPlan; witnesses: StatementPatternExistsFilter / ExistsFilter{subPlan: Pattern or filter-free MultiJoin} / NegatedNativeBooleanFilter, existential endpoints as scratch columns (slot-less anonymous vars supported); MINUS arms as `Exists(negated=true)` correlated on shared slots; Aggregate terminal (multi-col groups, COUNT kinds, SUM guarded, MIN/MAX as id-preserving; AVG + SUM-DISTINCT + constant-input specs decline).
- [x] (2026-07-24) Step 5: `tryEvaluateAggregate` (kernel-before-probe; discard-and-rerun on `!sumsExact()`) + generalized `kernelGroupRow(long[], base, KernelGroupLayout)` + rung in `evaluateAll` — placed OUTSIDE the `arg instanceof MultiJoinPlan` guard (witness roots are FilterPlan/PatternPlan). `LmdbNativeKernelAggregateTest` 6/6: NOT EXISTS COUNT DISTINCT (HC q10 shape), multi-pattern EXISTS grouped, MIN/MAX/SUM typed-exact, MINUS anti-join, SUM-guard discard above 2^53 (aggOpened stays 0, interpreted rerun exact), flag-off silent.
- [x] (2026-07-24) Step 6 (partial): fuzz round `irAggregateWitnessShapes` (7 differential queries incl. MINUS chains and non-integer SUM guard rerun) — suite 23/23 green with `aggPlanned > 0` asserted; all kernel test classes re-verified green; formatter + copyright green.
- [x] (2026-07-24) Step 6 benchmark chase — four production fixes landed while pursuing the HC q10 gate on the real 13.8M store (each verified by the scratch explain loop, then the full battery):
  1. Witness sub-filters lower via the hook tier inside witness pipelines (previously declined `agg:witness-subfilters`).
  2. Repeated-slot self-loop witnesses (`?x pred ?x`, the MINUS self-loop) lower as semi ProbeClose with both ends the same operand; MINUS arms peel `ExtensionPlan` wrappers (BIND adds bindings — existence-preserving).
  3. Witness join bags use greedy connectivity ordering (the compiler's derived order is planned for entry-mask 0 and can put an exists-local pattern before the correlated one → `agg:witness-uncorrelated`).
  4. **CSR kernel-demand recording** (`LmdbCsrAdjacencyCache.recordKernelDemand` called from the probe support's adjacency miss): kernel adjacency requests build the view immediately (budget/admission/backoff still govern; the probe-amortization gate is skipped because one kernel execution consumes the whole adjacency) — without this, witness-only predicates never built and kernels declined forever. Plus: **constant-key patterns materialize their single run as a bind-time key domain** (`DomainRequest` pattern-run form) instead of demanding a whole-predicate CSR — the rdf:type byObject view (84MB, over entry budget, refused) reduced to one `conn:Node` extent scan.
- [x] (2026-07-24) **Benchmark evidence (HC q10 shape, inlined threshold, real benchmark store)**: baseline (kernel off) 5436 ms/op (JMH avgt 3); kernel path warm ~43 ms with exact count=59 matching the generic evaluator and the catalog ground truth — **~125x**. Cold first run 3.7s (CSR demand builds + async compile). Full battery green after all changes: KernelAggregateTest 6/6, KernelExecutionTest 3/3, LoweringTest 6/6, IrEmitterTest 25/25, ExpressionFilterTest 9/9, JaninoAggregate/Pipeline/CsrCacheMetrics green, fuzz 23/23.
- [x] (2026-07-24) Step 6 complete: three more strategy-internals test classes pinned (`LmdbNativeChunkPipelineTest`, `LmdbNativeParallelPipelinesTest`, `ParallelismBenchmarkTest` — the last failed with the definitive "expected parallelPipelines but got irKernel"); final module sweep 2179 tests / 4 failures = exactly the pre-existing baseline; compliance 2648 tests / 17 failures all within the 24 frozen identities, zero new; formatter + copyright green. M2 COMPLETE. Carried forward: catalog-form HC q10 unlock (synthetic-VALUES guard relaxation for Exists/Not(Exists)) and `ScratchHcQ10ExplainTest` removal once that lands.

## Surprises & Discoveries

(Pre-registered: NOT EXISTS was an anonymous lambda — hence Step 2; SUM typing must match `AggState`'s promotion — integer inputs emit xsd:integer, which double accumulation can only honor under the exactness bound; float/double SUM accumulation order could differ from the generic evaluator's, so non-integer SUM discards rather than risking drift; `ExistsFilter` witnesses may bind scratch columns — allocate above a watermark and never let outer nodes read them.)

- Observation: sticky (EXISTS-bearing) filters never reach `MultiJoinPlan.filters` — `collectFlattenable` only hoists placeable masks, so witness shapes arrive as `FilterPlan` wrappers (often around a bare `PatternPlan`, not a MultiJoinPlan at all). The first rung placement inside the `arg instanceof MultiJoinPlan` guard therefore never fired (`planned=0`); the rung moved outside the guard and the lowering peels FilterPlan/MinusPlan chains off the root.
  Evidence: engagement test's counter message progression `planned=0` → after the peel `planned=300, declined=288`.
- Observation: EXISTS-local variables can be entirely slot-less (`Term.unbound()`) — the witness lowering initially demanded `o.hasSlot()` for the existential branch and declined everything as `agg:witness-uncorrelated` (decline-trace facility found it in one run). Existential endpoints now probe into scratch columns whether or not a slot exists.
- Observation: CSR adjacency views build from KEYED probe traffic only; predicates touched exclusively through EXISTS filters (`source.has`) or full scans never accumulate probe counts, so `probe.adjacency(...)` stays null and the rung declines `adjacency-unavailable` forever on such stores. Tests warm with VALUES-seeded keyed-probe queries; PRODUCTION GAP for M5: witness-only predicates need demand recording (e.g. the bind-failure path feeding `csrCache.recordProbes`) or HC-q10-class queries will not engage on cold stores.
  Evidence: `[ir-aggregate] decline: adjacency-unavailable` ×288 with scans warmed; engagement immediately after keyed-probe warming.

## Decision Log

- Decision: M2 aggregate function scope — COUNT_STAR/COUNT/COUNT_DISTINCT (exact, any data), MIN/MAX via new id-preserving `AGG_MIN_ID`/`AGG_MAX_ID` (winner id kept via `hooks.compareValues`; emission through `lazyValue` preserves exact typed literals and SPARQL value-order semantics for mixed types), SUM via double accumulation guarded exact (all-integer inputs, count×maxAbs < 2^53, no non-numeric input; else the kernel result is discarded and the interpreted path re-runs — cheap because `evaluateAll` materializes anyway). AVG **declines** in v1: SPARQL AVG is decimal division with specified semantics that double arithmetic cannot reproduce bit-exactly, and the corpus prizes are counts; revisit with the M5 audit.
  Rationale: never ship a result the generic evaluator would not produce; the differential fuzz gate makes anything else a permanent flake source.
  Date/Author: 2026-07-24 / Claude (Fable) with hmottestad.
- Decision: NOT EXISTS becomes recognizable via a named `NegatedNativeBooleanFilter` (delegate field) replacing the lambda in the filter compiler's `Not` case. Behavior-neutral (identical accept semantics; `selectBatch` default composes; forkability delegates); Routine-B evidence = existing fuzz + filter test suites pre/post.
  Rationale: lowering cannot introspect a lambda; the HC q10 prize is a NOT EXISTS witness.
  Date/Author: 2026-07-24 / Claude.
- Decision: HAVING stays above the native step (generic FilteringIteration); the kernel produces the anonymous twin spec as a normal output and `kernelGroupRow` binds it by name like any other. The IR-level `Having` fast-path is not used in M2 (it only understands count thresholds; the generic filter is already correct and cheap over materialized groups).
  Date/Author: 2026-07-24 / Claude.
- Decision: witness sub-pipelines only lower shapes whose own structure the M1 builder already handles (patterns chained through available endpoints); `ExistsFilter.subPlan` recursion accepts `MultiJoinPlan` and bare `PatternPlan` sub-plans in v1. Unlowerable witnesses leave the whole filter residual — but residual sticky filters on the *aggregate* side mean the kernel cannot claim the terminal (residuals need per-row wrapper application, which the aggregate consumer has no surface for), so any unlowered sticky filter declines the aggregate lowering. This asymmetry with M1 is deliberate and recorded.
  Date/Author: 2026-07-24 / Claude.

## Outcomes & Retrospective

(2026-07-24, M2 complete.) The aggregate rung delivers the program's central promise on real data: the HC q10 witness shape — candidate scan → IN filter → two-pattern NOT EXISTS with an inner comparison → self-loop MINUS → COUNT(DISTINCT) — runs **5436 ms → ~43 ms warm (~125x)** on the 13.8M-triple benchmark store with the exact catalog-ground-truth count, because the kernel replaces 89K interpreted per-row exists sub-queries with fused CSR loops. Getting there surfaced four productionizable gaps the small-store tests could not see, all now fixed and battery-verified: witness sub-filter hooks, self-loop/Extension-wrapped MINUS arms, greedy witness connectivity ordering, and — the deep one — **CSR kernel-demand recording** plus **constant-key runs as bind-time domains** (the cache only learned from keyed probe traffic, which witness-only predicates and class extents never generate; and a class extent must not require an 84MB whole-predicate view). Two honest boundaries recorded: the catalog form of HC q10 still declines because a single-row VALUES threshold poisons the whole NOT EXISTS into an unintrospectable generic lambda at the native-compile layer (diagnosed to the exact guard; relaxation queued), and AVG/non-integer SUM stay interpreted by design (typed-arithmetic semantics; the exactness guard's discard-and-rerun was proven by test). Method lessons: the explain + debug-gated decline traces turned every silent gap into a five-minute diagnosis; the differential fuzz suite held the whole time — every intermediate state produced exact results, with declines, never wrong answers.

## Context and Orientation

Paths relative to `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/` unless noted; branch `optimize-lmdb`; new files carry the 2026 EDL header + `// Some portions generated by Claude`. House rules: mvnf tests one at a time (never -am/-q), root quick install first, keep untracked artifacts, formatter + `scripts/checkCopyrightPresent.sh` before finalizing, sweeps judged by report XMLs (current pre-existing baseline: `LmdbNativeFeatureFlagForkTest` 3F + `LmdbNativeLeftJoinFilterRewriteTest` 1F). Strategy-internals tests must pin `rdf4j.lmdb.janinoCodegen.enabled=false` (flag defaults ON on this branch).

Aggregate-side integration facts (verified):

- `NativeGroupIteration.evaluateAll()` (`LmdbNativeGroupStep.java:270`): builds a `RowState`, `initialize(row)`, then rungs: exists-intersection collapse → `LmdbNativeJaninoAggregate.tryEvaluate((MultiJoinPlan) arg, row, groupSlots, aggregates, this)` when `arg instanceof MultiJoinPlan` (`PATH_JANINO_AGGREGATE`) → WCOJ counts when `AggregateSpec.allCounts` → sequential/speculative. The new rung goes directly after the JaninoAggregate block, same guard style, tag `PATH_IR_AGGREGATE` (add the constant + vocabulary entry to `LmdbNativeAttemptMetrics`).
- `AggregateSpec` (`LmdbNativeAggregateState.java:117`): `{String name, int slot, long constant, boolean distinct, AggKind kind}`, `AggKind {COUNT, SUM, MIN, MAX, AVG}`; COUNT(*) is `star(...)` (slot < 0). HAVING materializes a second anonymous spec named `_anon_having_*`.
- Emission conventions (`toBindingSet`, `LmdbNativeGroupStep.java:~1020`): group ids bind via `source.lazyValue` skipping UNKNOWN/NULL_CONTEXT_ID; COUNT → `createLiteral(BigInteger.valueOf(count))`; SUM binds the typed accumulated Literal or omits the binding on type error; MIN/MAX bind the winning Value. The existing narrow `kernelGroupRow(long groupId, long count)` (`:1006`) binds one count literal under every spec name — it stays for the old rung until M7; the generalized overload is additive.
- Witness filter representations: `StatementPatternExistsFilter` (`LmdbNativeFilters.java:216`; fields `s/p/o/c` Terms, `contexts`, `namedContextScope`, `varyingSlots`); `ExistsFilter` (`:371`; field `subPlan` SlotPlan); NOT EXISTS currently `row -> !arg.accept(row)` (`LmdbNativeAggregateFilterCompiler.java:~388`) — replaced in Step 2 by `NegatedNativeBooleanFilter`. Sticky filters carry mask −1 (`placeableFilterMask` returns −1 for EXISTS-bearing conditions).
- `MinusPlan` (`LmdbNativeRowPlans.java:277`): `{left, right, sharedMask}`; factory already degenerates to `left` when no shared variables, but a constructed `MinusPlan` in the tree still means shared vars exist — lower right as `AntiExists` correlated on the shared slots, right-arm-only slots as scratch columns.
- M1 substrate to reuse: `LmdbNativeKernelLowering.Builder` (pattern classification, operand/column allocation, filter tiers), `LmdbNativeKernelBindings` (+ needs a `BindHook[]`-style extension not required here), `LmdbNativeKernelHooks` (extend with SUM guards), `LmdbNativeKernelExecution` (add `tryEvaluateAggregate`; kernel-before-probe ordering is mandatory — probe churn on below-threshold opens breaks probe-accounting tests), `KernelExecutionTestAccess` (add aggregate counters).
- IR: `Aggregate` terminal already supports groupCols/COUNT kinds/SUM/`Having`/OutputMods; `Exists(negated, pipeline)` and `ProbeClose(semi)` exist; emitter (`LmdbNativeKernelEmitter`) needs only the two new MIN_ID/MAX_ID accumulator kinds (long[] winner-id arrays + boolean[] seen, update via `hooks.compareValues`).
- Compliance gate: `mvnf compliance/sparql` (or the module's documented invocation) compared against `plans/lmdb-native-engine/COMPLIANCE-BASELINE.md` (24 known pre-existing LMDB failures). Benchmark: HC q10 shape via `ThemeQueryBenchmark` (HIGHLY_CONNECTED index 10) with `scripts/run-single-benchmark.sh`, or the docker-jfr loop for stability.

## Plan of Work

Step 1 (IR): add `AGG_MIN_ID`/`AGG_MAX_ID` constants + `AggregateOutput.minId(col)/maxId(col)` to `LmdbNativeKernelIr` (count-kind = false; requires hooks); emitter: fields `agW<i>` (long[] winner ids) + `agB<i>` (boolean[] seen), update `if (v != -1L) { if (!agB[g] || hooks.compareValues(v, agW[g]) < 0) {...} }` (MAX with > 0), emission `agB[g] ? agW[g] : -1L` (raw id — the engine side resolves); grow in `ensure`. Red-green via two new `LmdbNativeKernelIrEmitterTest` cases (value-order via TestHooks signed compare).

Step 2 (neutral refactor): `NegatedNativeBooleanFilter implements NativeBooleanFilter` in `LmdbNativeFilters.java` (`final NativeBooleanFilter delegate`; accept negates; close/fork delegate; batchReadMask −1 conservative); compiler `Not` case constructs it. Pre/post green: `LmdbNativeDifferentialFuzzTest` (covers NOT EXISTS rounds) + `LmdbNativeAdaptiveFilterPlacementTest` selection.

Step 3 (hooks): add to `LmdbNativeKernelHooks`: `boolean sumGuardEnabled` toggle (set when the lowering registered SUM outputs), counters `sumInputs`, `sumMaxAbs`, flags `sumSawNonInteger`, `sumSawNonNumeric`; `doubleValue` updates them (integer detection: `ValueIds.isOrderedInteger` or decoded `decimalValue()` integral with `floatingValue() == null`); `boolean sumsExact()` = no non-numeric, no non-integer, `sumInputs * max(sumMaxAbs,1) < 2^53`. `isNumeric` also records non-numeric sightings when the guard is armed (the emitter's SUM gate calls isNumeric then doubleValue).

Step 4 (lowering): `lowerAggregate` entry — arg must be `MultiJoinPlan` (or a `MinusPlan` whose left is one, v1) with every child lowerable by the M1 builder; filters: placeable filters through the M1 three tiers; sticky filters must ALL lower as witnesses or the aggregate declines; witness lowering per Context; then terminal: map specs (COUNT kinds/SUM/MIN/MAX; AVG → decline `"agg:avg"`; constant-input specs (`spec.constant != UNKNOWN`) → decline `"agg:constant-input"` v1), group cols = groupSlots mapped through the column table (group slot must be a kernel column or entry — else decline), `Aggregate(groupCols, outputs, null, OutputMods.none())`. Emit nothing (Aggregate terminal). Returns `Lowered` + a new `KernelGroupLayout` on the bindings (group engine slots + per-output spec/encoding).

Step 5 (execution + emission): `tryEvaluateAggregate(SlotPlan arg, RowState row, int[] groupSlots, AggregateSpec[] aggregates, NativeGroupIteration step, TupleExpr explainTarget)` — kernel-before-probe, drain `fill` into a stride buffer, rows → `step.kernelGroupRow(buffer, base, layout)`; on `!hooks.sumsExact()` return null discarding results (decline reason `"sum-guard"`); counters `AGG_PLANNED/AGG_OPENED/AGG_ROWS` on the execution class + test access. `kernelGroupRow` generalized overload in `LmdbNativeGroupStep` binding group ids then per-output by encoding. Rung in `evaluateAll`. `LmdbNativeKernelAggregateTest` (store fixture like M1's; red engagement first): grouped COUNT with EXISTS witness; NOT EXISTS witness (the q10 shape: candidates minus those with a low-value neighbor); COUNT DISTINCT + HAVING via anon twin; MIN/MAX typed results; SUM guard discard (a >2^53 literal in data → interpreted rerun, results still exact); flag-off silence.

Step 6 (gates): fuzz rounds per Progress; formatter/copyright; module sweep; compliance sparql module re-check vs `COMPLIANCE-BASELINE.md`; benchmark the HC q10 theme query (fixed store) before/after with the kernel on, recording ms/op in Artifacts.

## Concrete Steps

From the repo root: root quick install; iterate with the module clean-install template; tests via `python3 .codex/skills/mvnf/scripts/mvnf.py <Class>`; sweep `... core/sail/lmdb --retain-logs`; compliance `python3 .codex/skills/mvnf/scripts/mvnf.py compliance/sparql --retain-logs` (judge by report XMLs vs the baseline doc); benchmark `scripts/run-single-benchmark.sh --module core/sail/lmdb --class ...ThemeQueryBenchmark --method executeQuery` with theme/index params per that script's conventions.

## Validation and Acceptance

Red→green engagement (`LmdbNativeKernelAggregateTest` fails "aggregate rung never engaged" before Step 5 wiring); fuzz suite green with the three new rounds' engagement asserted; the SUM-guard fuzz case must show identical results to the generic evaluator (proving discard-and-rerun fired, asserted via the decline counter); module sweep at baseline; compliance: no NEW failures beyond the 24 known; benchmark: HC q10 shape faster with the rung on than off (target: multiple-x, exact number recorded — the 5.8s baseline is dominated by the witness scan the kernel eliminates).

## Idempotence and Recovery

Additive except two small production edits (the `Not`-case named filter; the `evaluateAll` rung block) — both trivially revertible; flag-off restores current behavior byte-identically. Keep untracked artifacts; nothing committed without explicit request.

## Artifacts and Notes

(Transcripts and benchmark numbers to be captured as steps complete.)

## Interfaces and Dependencies

No new external dependencies. Signatures:

    // LmdbNativeKernelIr additions
    static final int AGG_MIN_ID = 7; static final int AGG_MAX_ID = 8;
    AggregateOutput.minId(int col) / maxId(int col)

    // LmdbNativeFilters addition
    final class NegatedNativeBooleanFilter implements NativeBooleanFilter { final NativeBooleanFilter delegate; }

    // LmdbNativeKernelLowering addition
    static Lowered lowerAggregate(SlotPlan arg, RowState row, int[] groupSlots, AggregateSpec[] aggregates,
            TupleExpr declineTarget)

    // LmdbNativeKernelBindings addition
    final KernelGroupLayout groupLayout; // null for row-side lowerings
    static final class KernelGroupLayout { final int[] groupEngineSlots; final AggOut[] outs; }
    static final class AggOut { final AggregateSpec spec; final int encoding; } // LONG_COUNT | SUM_DOUBLE_BITS | VALUE_ID

    // LmdbNativeGroupStep addition
    BindingSet kernelGroupRow(long[] rowBuf, int base, LmdbNativeKernelBindings.KernelGroupLayout layout)

    // LmdbNativeKernelExecution addition
    static List<BindingSet> tryEvaluateAggregate(SlotPlan arg, RowState row, int[] groupSlots,
            AggregateSpec[] aggregates, NativeGroupIteration step, TupleExpr explainTarget)

---

Revision note (2026-07-24, initial): authored as M2 immediately after M1 completion; scope decisions (AVG decline, SUM exactness guard, NOT EXISTS named wrapper, sticky-residual asymmetry) pinned before implementation.
