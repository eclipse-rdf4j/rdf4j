# Kernel lowering M3+M4: synthetic-VALUES unlock, OPTIONAL, rung-order flip, UNION/BIND/paths

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained per `.agent/PLANS.md`. Milestones M3+M4 of the approved lowering program. Predecessors (both complete, uncommitted): `20-kernel-lowering-row.md` (M1 — general lowering substrate, row rung `PATH_IR_KERNEL`), `21-kernel-lowering-aggregate.md` (M2 — aggregate rung `PATH_IR_AGGREGATE` with EXISTS/NOT-EXISTS/MINUS witnesses; HC-q10 shape ~125x on the real store; CSR kernel-demand recording + constant-key bind-time domains).

## Purpose / Big Picture

Three coverage steps in corpus-leverage order. Step 0 (carried from M2): a single-row `VALUES ?t { v }` variable referenced inside FILTER NOT EXISTS poisons the whole condition into an unintrospectable generic lambda at `LmdbNativeAggregateFilterCompiler.compileBoolean`'s first guard (`syntheticVarNames` intersection) — relaxing that guard for `Exists`/`Not(Exists)` lets the native ExistsFilter compile (its leaves apply their own synthetic protection), unlocking the CATALOG form of HIGHLY_CONNECTED q10 for the M2 kernel machinery. M3: OPTIONAL — `LeftJoinPlan` lowers to the IR's `LeftProbe` (bare single-`PatternPlan` right arms, chains as sequential LeftProbes; condition-bearing or multi-pattern arms decline until a measured need for a `LeftGroup` IR container), plus the rung-order flip (IR rungs ahead of the old shape-specific rungs) once shadowing is proven. M4: UNION (`UnionPlan` → IR `Union` with shared-slot column alignment), BIND (`ExtensionPlan` → `BindAlias`/`BindHook`), and native paths (`PathPlan` → `PathExpand` under set-semantics consumers). Gates per milestone: fuzz rounds (forced-on, engagement-asserted), sweep + compliance at baseline, benchmarks — catalog HC q10 (Step 0), SOCIAL q8 (M3), LIBRARY q6 + PHARMA q10 (M4), ANALYTICS no-regression.

## Progress

- [x] (2026-07-24) ExecPlan authored.
- [x] (2026-07-24) Step 0 core: red engagement test (`syntheticValuesWitnessEngagesAndMatchesGeneric`, red "never engaged (declined=300)") → guard relaxation in `compileBoolean` (Exists/Not(Exists) roots exempt from the synthetic short-circuit) → green 7/7 with three more fixes found en route: (a) witness sub-plans peel their own `FilterPlan` wrappers (sticky generic conditions over synthetic vars lower as hook-tier witness filters); (b) `EnumerateAdjKeys` allowed at any depth (a 1-row VALUES seed before a both-fresh pattern is a legitimate cartesian; the old first-producer-only guard declined it); (c) **kernel cache owner switched from `row.source.idSpace()` to a stable static token** — plans with synthetic values wrap the source in a per-plan `SyntheticValueSource`, so idSpace-keyed caching missed every time (evidence: 302 compilations / 0 hits / 0 instantiations with an IDENTICAL printed shape key; IR kernels are pure shape — every store-specific input arrives via the bind-time context — so cross-store class sharing is sound). Also learned: single-pattern NOT EXISTS shapes are now claimed by the exists-intersection engine at plan time (a better native path — the test uses a two-pattern witness, like catalog HC q10). Fuzz 23/23 + execution + expression-filter green.
- [x] (2026-07-24) Step 0 benchmark verdict: **catalog HC q10 (its real VALUES-threshold form) 5436 → 30.6 ms/op on the official JMH harness (~178x)**, same store and day as the kernel-off baseline; the benchmark's built-in expected-count assertion (59) validates correctness. `ScratchHcQ10ExplainTest` deleted.
- [ ] Step 0 close: sweep + compliance verdicts.
- [x] (2026-07-24) Step 1 (M3) core: `lowerRows` peels left-deep `LeftJoinPlan` chains (arms in application order) AND `FilterPlan` wrappers (group-level filters over optional vars — tier-degraded as usual); `lowerOptionalArm` v1 (bare single pattern, one available endpoint, fresh slot → `LeftProbe`; declines: condition-bearing/multi-pattern arms, ordered-scan hints, keys that are themselves optional — non-well-designed scan semantics); `optionalColMask` soundness guards (id-tier filters and witness correlation decline on maybe-null columns — NE-vs-unbound diverges, and a null witness key would need scan semantics; hook tier handles both exactly via the −1→unbound scratch convention); wrapper cursor skips binding NULL columns (binding −1 would falsely mark the slot bound). Row rung moved OUTSIDE the MultiJoinPlan guard in `openUnorderedInput` (LeftJoin roots never reached it). Tests: `LmdbNativeKernelExecutionTest` 5/5 (plain OPTIONAL null-extension + `!BOUND`-filter-over-optional exact), aggregate 7/7, fuzz **24/24** with new round `irKernelOptionalShapes` (29 differential queries: chained/sibling OPTIONALs, BOUND filters, grouped COUNT over optional columns; engagement asserted).
- [x] (2026-07-24) Step 1 sweep: 2182 tests / 4 failures = pre-existing baseline, zero new — the wider OPTIONAL absorption is regression-free.
- [x] (2026-07-24) SOCIAL q8 A/B: 772 → 682 ms/op (within noise) — the query's integral `BIND(?a AS ?cycleStart)` compiles to an `ExtensionPlan` root the lowering declines, so its gate is blocked on Step 4's BIND lowering and moves there (recorded; expected interlock, not a regression).
- [ ] Step 2 (M3): rung-order flip behind shadowing evidence (theme-corpus explain sweep showing PATH_JANINO_* fully covered by PATH_IR_*).
- [ ] Step 3 (M4): `UnionPlan` lowering + fuzz round; LIBRARY q6 + PHARMA q10 benchmarks.
- [ ] Step 4 (M4): `ExtensionPlan` → BindAlias/BindHook (hook implementation in `LmdbNativeKernelHooks.computeBind` via `CopyBinding`/generic step); `PathPlan` → `PathExpand`; fuzz rounds; ANALYTICS no-regression check.
- [ ] Step 5: hygiene + sweep + compliance; living sections updated.

## Surprises & Discoveries

- Observation (Step 0 sweep): widening kernel absorption surfaced a REAL correctness regression, not just assertion drift — `LmdbSailStoreTest.testOrderByLmdbIndexPreservesJoinOrder` got rows in the wrong order because ORDER-BY-satisfied-by-index plans rely on the producer cursor's encounter order (no sort layer), and the kernel enumerates CSR order instead. Soundness gate added: any `PatternPlan.statementOrder != null` declines the lowering (`pattern-ordered-scan`) — an ordered-scan hint is a promise the consumer may rely on (index-order ORDER BY, merge-join inputs). Three more strategy/explain assertion classes pinned (`LmdbCsrCacheQueryTest`, `LmdbNativeQueryExplanationTest`, `LmdbNativeStrategyPriorityTest` — now seven pinned classes total).
  Evidence: expected [urn:s3, urn:s2, urn:s1] got [urn:s3, urn:s1, urn:s2]; green (42/42) after the gate with zero test edits in that class.

(Pre-registered: LeftJoin condition semantics — a condition must be part of the join decision (null-extend on failure), never a post-filter; `LeftJoinPlan.right` arriving as `FilterPlan` = condition-bearing → decline in v1. Union branch column alignment: a slot produced by both branches must map to ONE column; the emitter resets all branch-produced columns to NULL before each branch. Optional slots downstream: filters reading maybe-null columns must go hook tier (the −1→unbound scratch convention makes `!BOUND` exact). The rung-order flip changes which strategy-internals tests see kernel absorption — expect more pin-the-flag fixes.)

## Decision Log

- Decision: Step 0's guard relaxation is scoped to exactly `Exists` and `Not(Exists)` condition roots: the synthetic-var short-circuit is skipped for those, letting the recursive compile proceed; every raw-id-shortcut leaf (compare/IN/sameTerm compilers) keeps its own synthetic protection, and expression leaves referencing synthetic vars still fall back to per-row generic evaluation inside the ExistsFilter sub-plan — which is precisely the pre-relaxation behavior of the whole condition, now confined to the leaf.
  Rationale: the guard's purpose (never raw-id-compare a plan-local synthetic id against store ids) is enforced at the leaves; poisoning the entire EXISTS wrapper was coarser than its own justification. TDD: a failing engagement test on the catalog HC q10 fixture shape first; differential fuzz + compliance guard semantics.
  Date/Author: 2026-07-24 / Claude (Fable) with hmottestad.
- Decision: M3 v1 LeftProbe scope — right arm must be a bare single `PatternPlan` with a constant predicate, one endpoint reading an already-available operand and the other a fresh slot; anything else (`FilterPlan`-wrapped = condition-bearing, multi-pattern, nested LeftJoin) declines with a distinct reason so the M5 audit can size a `LeftGroup` container. Row side: optional columns bind through the existing wrapper (NULL_ID −1 = engine UNKNOWN, so `row.bind` is simply skipped for −1 values — extend the wrapper cursor to skip NULL columns instead of binding them). Aggregate side: COUNT(x) over an optional column already skips NULL (emitter's COUNT gate), COUNT(*) counts the null-extended row — matching SPARQL.
  Date/Author: 2026-07-24 / Claude.
- Decision: The rung-order flip is evidence-gated exactly as the approved plan states: run the theme corpus + FOAF suites with both rung families enabled and explain recording on; flip only when every `PATH_JANINO_KERNEL`/`PATH_JANINO_AGGREGATE` engagement in that sweep is also lowerable by the IR path (decline-reason report empty for those shapes). Until then the old rungs stay first.
  Date/Author: 2026-07-24 / Claude.

## Outcomes & Retrospective

(To be written as milestones complete.)

## Context and Orientation

All paths in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/` unless noted; branch `optimize-lmdb`; house rules as in plans 20/21 (mvnf one at a time, no -am/-q, root install first, keep untracked artifacts, report-XML sweep judgment, strategy-internals tests pin `rdf4j.lmdb.janinoCodegen.enabled=false` — now four such classes: `LmdbNativeChunkHashBuildTest`, `LmdbNativeChunkPipelineTest`, `LmdbNativeParallelPipelinesTest` (package `sail/lmdb`), `benchmark/ParallelismBenchmarkTest`).

Current substrate (as-built in M1/M2):

- `evaluation/LmdbNativeKernelLowering` — `lowerRows` (MultiJoinPlan roots; three filter tiers; VALUES/folded-IN/constant-key domains; ENTRY operands) and `lowerAggregate` (peels FilterPlan/MinusPlan chains; cores MultiJoinPlan|PatternPlan|MultiValuePatternPlan|ValuesPlan; witness lowering via `lowerWitnessJoin` with greedy connectivity ordering, hook-tier witness sub-filters, self-loop semi ProbeClose, Extension peel on MINUS arms; Aggregate terminal: COUNT kinds exact, SUM double+`sumsExact` guard with discard-and-rerun, MIN/MAX id-preserving via `AGG_MIN_ID`/`AGG_MAX_ID`; AVG/SUM-DISTINCT/constant-input decline). Decline tracing: `rdf4j.lmdb.janinoCodegen.debug=true` prints `[ir-lowering]`/`[ir-aggregate]`/`[filter-compile]`/`[exists-compile]`/`[csr-kernel-demand]` traces.
- `evaluation/LmdbNativeKernelBindings` — adjacency requests; `DomainRequest` (literal ids or pattern-run slices bound by `bindDomains(probe, row, route)`); filter hooks (`argSlots`); `KernelGroupLayout` (`ENC_LONG_COUNT`/`ENC_SUM_DOUBLE_BITS`/`ENC_VALUE_ID`); `hooksRequired`/`sumGuard` flags.
- `evaluation/LmdbNativeKernelHooks` — scratch-RowState `testFilter` (−1 arg = slot left UNKNOWN — load-bearing for BOUND semantics), `compareValues` (ordered-int → decoded → ValueComparator unbound-first), `isNumeric`/`doubleValue` with the armed SUM guard (integer-datatype-family + count×maxAbs < 2^53 + non-numeric invalidates), `computeBind` currently throws (M4 fills it).
- `evaluation/LmdbNativeKernelExecution` — `tryOpenRows` (rung after `LmdbNativeJaninoPipeline` in `LmdbNativeRowStep.openUnorderedInput`, kernel-before-probe ordering mandatory) and `tryEvaluateAggregate` (rung after `LmdbNativeJaninoAggregate` in `NativeGroupIteration.evaluateAll`, OUTSIDE the MultiJoinPlan guard); counters + `KernelExecutionTestAccess` (test scope).
- CSR: `LmdbCsrAdjacencyCache.recordKernelDemand` (immediate build on kernel adjacency miss; probe-amortization skipped, budget/admission/backoff kept) called from `LmdbSailStore` CsrProbeSupport.adjacency miss with same-open re-lookup.
- IR/emitter (plan 19): `LeftProbe(adj, key, valueCol)` (null arm sets −1, NULL key → null arm), `Union(branches)` (per-branch reset of all branch-produced columns), `BindAlias`/`BindHook(bindId, args≤2, dstCol)`, `PathExpand(adj, src, dstCol, minHops 0|1)` — all compile-and-run tested at IR level (`LmdbNativeKernelIrEmitterTest`); the lowering just has to produce them.
- Step-0 target evidence: catalog HC q10 = `VALUES ?threshold { 3 } … FILTER NOT EXISTS { ?node conn:connectsTo ?n2 . ?n2 conn:weight ?w2 . FILTER(?w2 < ?threshold) } MINUS { … self loop … }`; with the threshold inlined as a literal the same machinery already delivers **5436 ms → ~43 ms warm, count=59 exact** on `target/lmdb-theme-query-benchmark/complete` (scratch loop `benchmark/ScratchHcQ10ExplainTest`, `-Dscratch.hcq10=true`). The guard to relax: `LmdbNativeAggregateFilterCompiler.compileBoolean`'s first block (`syntheticVarNames` ∩ condition vars → `compileGenericBoolean`).
- Plan-node shapes for M3/M4 (verified in plan 20's exploration): `LeftJoinPlan{left, right}` (condition compiled INTO the right arm as a `FilterPlan` wrapper); `UnionPlan` (factory `SlotPlan.union`); `ExtensionPlan{arg, CopyBinding[] copies}`; `PathPlan` (arbitrary-length, min ≤ 1, constant-pred alternatives, endpoint slots/consts, `zeroLength` flag). Benchmarks via `scripts/run-single-benchmark.sh --theme-query THEME:INDEX` (+ `--jvm-arg -D...`); baselines from `results-2026-07-23.md`: SOCIAL q8 719 ms, LIBRARY q6 2780 ms, PHARMA q10 3514 ms.

## Plan of Work

Step 0: relax the synthetic guard for `Exists`/`Not(Exists)` roots in `compileBoolean` (structure: hoist the guard below a new early branch that dispatches those two shapes to the existing Not/Exists paths). Red first: `LmdbNativeKernelAggregateTest` gains a VALUES-threshold witness query (small-store form of catalog HC q10) asserting engagement — fails with today's lambda. Green, then fuzz + compliance + sweep, then catalog HC q10 A/B on the benchmark store via the scratch loop and `run-single-benchmark.sh --theme-query HIGHLY_CONNECTED:10`; record numbers; delete `ScratchHcQ10ExplainTest`.

Step 1 (M3): in `lowerRows`/`lowerAggregate` child dispatch, accept `LeftJoinPlan` children/cores: left lowers as usual; right arm per Decision Log → `LeftProbe` node; optional slots register in a new `optionalMask` so later filter tiers treat them as maybe-null (id tier declines on optional operands; hook tier fine; witness correlation on an optional slot declines v1). Wrapper cursor: skip binding NULL_ID columns. Fuzz round `irKernelOptional` (well-designed OPTIONAL generator incl. `!BOUND` filters above). Aggregate side: COUNT over optional columns verified against generic in the round.

Step 2 (M3): explain-shadowing sweep (scratch harness over the theme catalog with both rung families + explain recording; report = old-rung engagements not lowerable by IR), flip the rung order in `openUnorderedInput` + `evaluateAll` when the report is empty (else fix gaps first); expect and fix new strategy-test pins; SOCIAL q8 A/B benchmark.

Step 3 (M4): `UnionPlan` lowering — branches lower into IR `Union` with a shared column map (each branch a sub-lowering against the same builder state, produced-column reconciliation: same engine slot → same column in every branch); branch-unsupported → decline whole union. Fuzz round `irKernelUnion`; LIBRARY q6 + PHARMA q10 A/B.

Step 4 (M4): `ExtensionPlan` → `BindAlias` for copy bindings, `BindHook` for computed ones (hook impl: `CopyBinding` evaluation against the scratch row; register args from its read slots ≤2); `PathPlan` → `PathExpand` when single alternative + min≤1 + endpoints map (set-semantics consumers only: DISTINCT/aggregate terminals); fuzz rounds `irKernelBind`, `irKernelPath`; ANALYTICS theme spot-check (expected ~1.0x, no regression >5%).

Step 5: formatter/copyright, module sweep, compliance, plan updates.

## Concrete Steps

As plans 20/21 (root install; module clean-install loop; `python3 .codex/skills/mvnf/scripts/mvnf.py <Class>`; sweep `core/sail/lmdb --retain-logs`; compliance `compliance/sparql --retain-logs` judged vs `lmdb-compliance-baseline.json`; benchmarks isolated from test runs).

## Validation and Acceptance

Each step: red engagement test → green; full fuzz suite green with new rounds' engagement asserted; sweep and compliance at their frozen baselines; benchmark A/B recorded in Artifacts. Flag-off behavior stays byte-identical throughout.

## Idempotence and Recovery

All additive except the Step-0 guard relaxation (small, revertible, fuzz+compliance-guarded) and the Step-2 rung-order swap (two-line, revertible). Keep untracked artifacts; nothing committed without explicit request.

## Artifacts and Notes

(To be captured as steps complete.)

## Interfaces and Dependencies

No new dependencies. New lowering cases only; possible `LeftGroup` IR container deliberately deferred to measured demand. `LmdbNativeKernelHooks.computeBind` gets its first real implementation in Step 4.

---

Revision note (2026-07-24, initial): authored at M2 completion; Step 0 carries the fully-diagnosed synthetic-VALUES unlock forward with its evidence.
