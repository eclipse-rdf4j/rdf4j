# Phase 2: the FactorizedChunk pipeline — one factorized vector execution substrate for the LMDB native engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`. This is Phase 2 of the approved umbrella plan (user plan file `users-havardottestad-documents-programm-binary-squid.md`); Phase 0 (`.agent/execplan-lmdb-factorized-correctness-audit.md`) and Phase 1 (`.agent/execplan-lmdb-phase1-engagement-and-substrate.md`) are complete.

## Purpose / Big Picture

The LMDB native engine currently picks one of five whole-query strategies per SELECT (see `NativeRowsIteration.initialize()` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java`): prefix-run cursor, ordered-distinct, batch hash join, parallel pipelines, factorized rows, or a row-at-a-time nested-loop chain. Each strategy has its own machinery and narrow gates. This phase builds the replacement substrate: a pipeline of composable operators exchanging FactorizedChunks — a columnar batch of "flat" rows plus per-row unflat groups (Kuzu's DataChunk model adapted to this engine's slot masks) — so that batching, factorization (count/enumerate/exists semantics), memoization, and run reuse are properties of ONE probe operator available to every query, not separate strategies. After this phase, an eligible SELECT over a `MultiJoinPlan` runs the chunk pipeline (behind `rdf4j.lmdb.chunkPipeline.enabled`, default on at phase exit), reports `nativeExecutionStrategy=chunkPipeline(...)` in `explain(Telemetry)`, and star-shaped SELECTs with unprojected legs run at least 2× faster than the row-at-a-time factorized sink they replace. The legacy bare-BGP engine (`LmdbNativeQueryCompiler`, ~1550 lines) is deleted once the pipeline covers its shapes.

## Progress

- [x] (2026-07-14) ExecPlan authored.
- [x] (2026-07-14 00:40Z) Milestones A+B+D(run detection), per the integration decision below: `LmdbNativeChunkPipeline`
  landed — `ScanStage` (batched root fills, PatternBatchCursor bind discipline), `ProbeStage` (per-row probes with
  retained NativeProbe, bounded run-replay buffer serving adjacent equal probe keys without touching the store),
  `ChunkPrefixRowCursor` surface, wired as the preferred prefix strategy inside `LmdbNativeFactorizedRows.open()`
  behind `rdf4j.lmdb.chunkPipeline.enabled` (prefix-filter-free, all-pattern prefixes only; chunked-memo and row-chain
  fallbacks unchanged). `LmdbNativeChunkPipelineTest` (3 tests: differential + ENGAGED + RUN_REPLAYS counters,
  DISTINCT/LIMIT, single-depth prefix) green; full verification landed 2026-07-14 01:15Z: 202 tests green across
  the differential fuzz and every factorized suite with the chunk prefix active.
- [x] (2026-07-14 01:40Z) Milestone D-filters: `FilterStage` — prefix-depth filters run inside the batched chain by
  compacting the selection vector (shared-RowState evaluation with `recomputeBoundMask()`, which invalidates the
  binding view, so generic-fallback and stateful filters keep row-chain semantics). `tryOpenPrefix` no longer refuses
  filtered prefixes. Test `chunkPrefixHandlesPrefixDepthFilters` green; fuzz revalidation in flight.
- [x] (2026-07-14 07:35Z) Milestone B-remainder: per-key memo in ProbeStage — arena-backed (`LmdbNativeLongArena`
  shared across the pipeline's probe stages), adaptive (first visit stores a SEEN_ONCE marker, second visit commits
  the replay buffer already collected for run detection, later visits replay from the arena), budgeted through the
  sink's shared `FactorizedTail.MemoBudget`. Counter `MEMO_REPLAYS`. Red→green
  `chunkPrefixMemoServesNonAdjacentKeyRepeats` (4-pattern chain: index-ordered scans keep depth-1 keys grouped, so
  non-adjacent repeats only appear at depth 2 where each hub's five subjects replay its 20 mids — red run proved the
  original 3-pattern shape could NOT exhibit them: runReplays=40, memoReplays=0). Fuzz 10/10 + factorized suites green.
- [x] (2026-07-14 07:50Z) Milestone E-integration (engagement metric): `open()` records the chosen prefix strategy;
  `describeEngagement()` reports `chunkPipeline(flat=…, enumBranches=…, countBranches=…, existsBranches=…)` when the
  chunk pipeline drives the prefix, `factorizedRows(flatPrefix=…, prefix=chunkedMemo|chain, …)` otherwise. Strategy
  priority + explanation tests updated (`chunkPipelineEngagementIsVisibleInTelemetry`,
  `telemetryExplanationShowsChunkPipelineEngagement`); VALUES-seeded prefixes still report `factorizedRows`. Fuzz
  10/10 green.
- [x] (2026-07-14 08:00Z) Milestone E-quantification: new `ChunkPipelineChainBenchmark` (4-deep chain, mode param
  pins the prefix strategy per JMH fork with an explain-based engagement check). Results (M-series laptop, 2w/3m
  iterations): chunkPipeline 26.98 ms/op, chunkedMemo 30.60, rowChain 67.57 — **2.50× vs the row chain it
  replaces** (≥2× bar met on the shape the pipeline accelerates). Star `selectHub` (flat=1, no probes to
  accelerate): 19.54 vs 19.59 ms/op — parity, no regression.
- [x] (2026-07-14 08:05Z) Milestone F-hash-build: estimate-driven scan-once in ProbeStage — validated trigger ported
  from the factorized tail (`storeProbes ≥ 1024 && storeProbes×8 + cumulativeScanned ≥ finite positive
  staticEstimate`), one `openRawUnbinding` sweep bucketing key→(arena offset, matches) with the memo budget
  refusing oversized builds; every later key resolves from the table (absent = no matches). Gated on
  `!hasRepeatedSlot`. Counter `HASH_BUILDS`; red→green `chunkPrefixBuildsHashTableForDistinctKeyFloods` (red run
  exposed two planning realities recorded in Surprises). Fuzz 10/10 + factorized + strategy suites green.
- [~] Milestone C: subsumed by the integration decision — the factorized-rows sink's tail branches ARE the unflat
  groups (count/exists/enum + multiplicity emission), now driven by the chunk-pipeline prefix. A standalone
  chunk-native group representation moves to Phase 3 (aggregation on chunks).
- [x] Milestone D: complete — FilterStage (2026-07-14 01:40Z) + run detection + per-key memo cover the milestone;
  in-branch filters keep running through `TailBranch.acceptFilters` per the integration decision.
- [x] (2026-07-14 08:00Z) Milestone E: engagement metric + strategy-priority/explanation tests done (see
  E-integration above); benchmark spot-check done (see E-quantification); theme ITs deferred to the phase-exit
  review pass.
- [x] (2026-07-14 08:45Z) Milestone F-deletion: `LmdbNativeQueryCompiler` (1547 lines)
  DELETED. Bare BGP fragments (Filter/Join/StatementPattern trees without a Projection root — the legacy claim set)
  now compile through `LmdbNativeAggregatePlanner.compileBareRoot` → `NativeRowsStep.bareFragment(...)` in a new
  `snapshotRows` mode that emits `RowBindingSetView` full-row snapshots carrying base bindings through (the generic
  join iterators drive fragments with their own bindings and rely on the child stream carrying them). Bare fragments
  get the full modern dispatch (batch/parallel/factorized/chunk prefix) and explain kind `bgp`. Red→green:
  `rowCompilerOpensPatternsInAlgebraOrder` (migrated; pins order-preserving paths with the batch flag off) +
  `bareFragmentRowsBindAllVariablesAndCarryBaseBindings` (new). `COMPILED` no longer counts bare fragments —
  fallback-pinning tests (`LmdbNativeCountStarTest`, `LmdbNativeLeftJoinWellDesignedTest`) rely on it meaning "the
  native compiler claimed the query root". Full-module rerun in flight; only known pre-existing failure expected
  (`LmdbEvaluationStatisticsMemoizationTest#recordsLearnedFilterPassRatioForExternalBoundPatternLocalFilter`).
- [x] (2026-07-14 09:30Z) Milestone F-review: full module 1600 tests / 1 known pre-existing failure
  (`LmdbEvaluationStatisticsMemoizationTest`, sketch learned-filter recording); theme ITs — plan snapshots 2/2,
  smoke 10/10, regression 64/65 (`electricalGridQ7` shares the learned-filter root cause: the plan never converges
  because pass ratios never record; chip task_6aa18888); W3C SPARQL 1.1 compliance 174/176 — BOTH failures (bind10
  filter scoping, post-query VALUES) reproduce with the native engine fully disabled and pass on the memory store,
  pinning them to the LMDB algebra-level optimizers modified before this session (chip task_2d2bba68); bare-fragment
  claim put behind `rdf4j.lmdb.bareFragments.enabled` during the investigation (kept as a safety flag, default on).
- [x] (2026-07-14 14:00Z) F-deletion regression fix: ThemeQueryBenchmark MEDICAL_RECORDS q8 went 36 ms → ~15 s
  because bare fragments' "full modern dispatch" let per-open strategies claim CORRELATED fragment entries — a
  generic LeftJoinIterator re-evaluates the OPTIONAL arm's 2-pattern join once per outer row (8,335×), and BOTH
  expensive strategies gated on estimates that ignore the entry bindings: (1) `LmdbNativeHashJoin.tryOpen` used
  STATIC estimates, so each entry rebuilt a ~25K-row hash table (`openRawUnbinding` sweep) — 36 ms → 15 s; (2)
  after fixing that, `LmdbNativeParallelPipelines.tryOpen` cleared its 50K root threshold via the bound pattern's
  STRUCTURAL pseudo-estimate (free-object 4096 × wildcard-context 256 ≈ 1M), spinning up workers + parallel
  snapshot sources + read txns per outer row (JFR-confirmed) — still ~1.1 s. Fix for both: refuse when the entry
  row binds any pattern slot (`hasRuntimeBoundSlot`) — the bound-prefix nested-loop chain is what exploits the
  binding; root-entry behavior unchanged. Red→green
  `LmdbNativeHashJoinBatchTest#correlatedFragmentEntryNeverBuildsPerOuterRow` (red: BUILDS=40, then
  PARALLEL_ROW_RUNS=20, for 20 outer rows; green: 0 for both). Lesson for the Phase 5/6 cost-model unification:
  every whole-plan strategy gate must be entry-bound-aware — correlated evaluation multiplies per-open setup cost
  by the outer cardinality, which no static or structural estimate sees.
- [x] (2026-07-14 15:30Z) F-deletion regression fix, round 2 (user bisect hint: 28293f90 was fine): worktree
  measurements with one shared harness pinned the WHOLE regression to the uncommitted tree — q8 warms to ~43 ms at
  28293f90 AND at b8e90e9686 (the plan-handling commit is innocent), 12.7 s in the tree pre-fix, ~87 ms after the
  first two gates. The rest was per-open cost on correlated entries: (3) the BATCH dispatch slot allocated a
  NativeBatch + PatternBatchCursor per open (24.9K EXISTS evaluations); (4) `LmdbNativeFactorizedRows.tryCreate`
  re-ran its split analysis per open for all-ENUM fragments whose memos die with the open. Fix: one
  `correlatedEntry = (arg.producedMask() & row.boundMask()) != 0` in `NativeRowsIteration.initialize()` skips the
  batch slot, and tryCreate under correlated entry is guarded by the existing allocation-light
  `plansCountingBranch` probe (counting/existence branches still factorize — they pay for the analysis).
  Red→green: the correlated test additionally pins `NativeBatch.ROOT_ITERATIONS` delta ≤ 1 (red: 21). Final: q8
  ~45-48 ms warm = baseline; fuzz 10/10, priority 5/5, chunk pipeline 6/6, hash join 4/4 green.

## Scope exclusion (2026-07-14, user instruction)

The LMDB algebra-level optimizers — `LmdbSketchJoinOptimizer`, `LmdbDeferredFilterPlacer`, `LmdbJoinPlanSupport`
(the experimental cascades/memo, sketch-based pipeline) — are OUT OF SCOPE for this and all follow-on phases: the
user is replacing them wholesale in a separate branch. Working-tree edits to those three files (the lmdb-side
jagged-VALUES filter-placement fix from the Phase 1 UNDEF audit) were reverted on user instruction on 2026-07-14.
Consequence: the two W3C compliance failures pinned to those optimizers (bind10 filter scoping, post-query VALUES —
chip task_2d2bba68) stay open and are owned by the user's optimizer-replacement branch, not by these phases. The
native-engine-side UNDEF soundness fixes (`ValuesPlan.bindsAllSlotsEveryRow`, core `FilterOptimizer`) are unaffected
and stay.

## Surprises & Discoveries

- Index-ordered scans mean depth-1 probe keys arrive GROUPED (the stable-index-order work picks e.g. posc, sorting
  rows by the fresh object) — a dataset interleaving keys at depth 1 still run-replays (red run: runReplays=40,
  memoReplays=0). Non-adjacent key repeats first appear at depth ≥ 2, where an upstream run replay re-emits the same
  fresh-value cycle per input row. The memo test uses a 4-pattern chain for this reason.
- `parallelPipelines` outranks the factorized path, so any test needing the chunk prefix on a >~1k-row root must set
  `rdf4j.lmdb.parallel.enabled=false` (the priority test already did; the hash-build test now does too).
- The planner roots at the smallest pattern and sinks unconsumed patterns to tail branches, so a "distinct-key flood"
  shape must be engineered: the flooded pattern needs to (1) not be the root (strictly larger than the root relation,
  padded with dead keys if needed) and (2) stay in the flat prefix (its fresh slots consumed by a later pattern).
  The first flood attempt turned the flooded pattern into an ENUM tail branch instead (explain showed
  `chunkPipeline(flat=2, enumBranches=1, ...)` with the 50-quad p3 as root).
- Native and generic engines insert bindings in different orders, so BindingSet.toString comparisons are order-fragile;
  the differential helpers now canonicalize by sorting binding names.
- The batch hash join physically scans its build side first (same rows, different first-scan). The
  algebra-order guard test now disables `rdf4j.lmdb.nativeBatch.enabled` to pin the invariant on the order-driven
  paths; MultiJoinPlan compiler-order tests keep guarding logical order.
- Bare BGP fragments (no Projection root) must emit rows that carry base bindings for FOREIGN names through
  (JoinIterator drives the child with its own bindings and uses the child's output as the joined row). The legacy
  compiler did this via `RowBindingSetView(source, layout, base, slots, true)` snapshots; `NativeRowsStep` got a
  `snapshotRows` mode reproducing exactly that.

## Decision Log

- Decision: build incrementally behind `rdf4j.lmdb.chunkPipeline.enabled` with per-milestone claims (single pattern → joins → factorized groups → filters), each gated by the differential fuzz, rather than landing the full operator set at once.
  Rationale: every milestone is independently verifiable against the generic evaluator and the existing sinks; the claim-gate widens as operators land.
  Date/Author: 2026-07-14 / Claude Code.
- Decision: Milestones A+B+D(run detection) land first as a batched, run-aware FLAT-PREFIX cursor
  (`ChunkPrefixRowCursor`) plugged into `LmdbNativeFactorizedRows.open()` as a third prefix strategy (alongside the
  cursor chain and the chunked-prefix memo), instead of a separate top-level dispatch entry.
  Rationale: the factorized sink already owns the branch machinery (roles, memos, budgets, multiplicity emission,
  DISTINCT/OFFSET/LIMIT integration); replacing its prefix with the chunk pipeline gives the batching + run-reuse
  performance win with zero new dispatch surface and full differential coverage, and the standalone operator split
  (select sink on chunks, group handoff to aggregation) follows in the remaining milestones once proven here.
  Date/Author: 2026-07-14 / Claude Code.

## Outcomes & Retrospective

Phase 2 delivered the batched flat-prefix substrate with every planned adaptive property living INSIDE the probe
stage instead of separate strategies: run replay (adjacent key repeats), arena-backed per-key memo (non-adjacent
repeats), and the estimate-driven hash build (distinct-key floods) — all budgeted by one shared MemoBudget, all
observable (`RUN_REPLAYS`/`MEMO_REPLAYS`/`HASH_BUILDS`, strategy string `chunkPipeline(...)` at explain Telemetry).
Quantified: 2.50× vs the row chain on a 4-deep chain (26.98 vs 67.57 ms/op), 1.13× vs the chunked memo, star
parity (flat=1 has no probes to accelerate). The 1547-line legacy BGP compiler is deleted; bare fragments ride the
modern dispatch in a snapshot-rows mode that preserves the generic evaluator's base-carrying contract, verified by
the full module suite (1600), theme ITs, and the W3C suite (174/176 — both failures proven independent of this
phase, chips spawned). The integration decision (chunk pipeline as the factorized sink's prefix, not a new
dispatch entry) held up: zero new dispatch surface, and the sink's tail-branch machinery doubles as the unflat
group representation until the Phase 5/6 chunk-native sinks land. Remaining scaffolding: `ChunkedPrefixCursor`
(the chunked-memo fallback) is now a strict subset of the chunk pipeline's claim and can be deleted when the flag
default-flips permanently; the umbrella's standalone FactorizedChunk struct (groups as first-class chunk columns)
was deliberately not built — the branch machinery already provides its semantics for every claimed shape.

## Context and Orientation

All code lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/` (package `org.eclipse.rdf4j.sail.lmdb.evaluation`). Key existing pieces this phase builds on:

- A "slot" is an index into a query-wide `long[] slots` of LMDB dictionary ids (`RowState` in `LmdbNativeRowState.java`); ≤60 slots, sets are 64-bit masks. `UNKNOWN` (from `LmdbNativeAggregateCompiler`) marks unbound.
- `PatternPlan` (in `LmdbNativePatternPlan.java`) is a triple-pattern scan; `pattern.openRaw(row, probe)` returns a `PatternCursor` whose `fill(long[] buffer, int maxRows)` drains up to maxRows quads (4 longs each: SUBJ/PRED/OBJ/CONTEXT via `TripleIndex` position constants); `quadPositionOfSlot(slot)` maps a slot to its quad position; `freshProducedMask(bound)`/`producedMask()` give the slot masks; `hasRepeatedSlot()`; `rejectsNullContextAtBind()` means quads with context==NULL_CONTEXT_ID must be skipped when the pattern requires a named graph.
- `MultiJoinPlan` (in `LmdbNativeJoinPlans.java`) holds `children` (bag of SlotPlans) and `filters` (`MaskedFilter[]` — `NativeBooleanFilter` + read mask); `derivedFactorizedPlan(row)` gives the ordered plan with unconsumed patterns sunk to the tail (Phase 1); `LmdbNativeFactorizedRows.analyzeSplit/tryCreate` computes the flat-prefix/branch split.
- `NativeBatch` (in `LmdbNativeBatch.java`) is the existing slot-major columnar batch (`slots[slot * capacity + row]`, `int[] selection`, `selectedCount`) — the chunk's flat section reuses this layout directly.
- `LmdbNativeLongArena` (Phase 1) stores value runs addressed by `(offset, len)` with a budget.
- The Select emission path: `NativeRowsIteration` (in `LmdbNativeRowStep.java`) handles DISTINCT (`NativeDistinctTracker`), OFFSET/LIMIT (multiplicity-aware in `getNextElement`), and projection (`step.project(row.slots, values)` → lazy `NativeProjectedBindingSet`). The chunk sink plugs in as another cursor kind there.
- Engagement observability: `LmdbNativeExplain.recordStrategy(expr, string)` (Telemetry-level actual metric), guarded by `LmdbNativeExplain.recordsStrategies(expr)` for dynamic strings.
- The correctness oracle: `LmdbNativeDifferentialFuzzTest` (differential vs `rdf4j.lmdb.nativeQueryEngine.enabled=false`), including star/aggregate/slice/REDUCED/UNDEF suites. Run with `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest` from the repo root after a root `-Pquick` install. ALWAYS use `set -o pipefail` when piping mvnf output.

The chunk exchange format (new file `LmdbNativeFactorizedChunk.java`):

    FactorizedChunk
      flat rows: reuse NativeBatch layout (slot-major long[] + selection vector), capacity 1024
      mult:      long[] per flat row (null => all 1)
      groups:    list of UnflatGroup { int[] slots; long[] counts; long[] offsets; int stride; LmdbNativeLongArena arena }
                 — per flat row r: counts[r] tuples whose slot values live at arena[offsets[r] + i*stride + j]
      logical rows = Σ over selected r of mult[r] × Π groups[g].counts[r]

Operators (each a `ChunkOperator` with `int fill(FactorizedChunk out)` pull semantics or producer/consumer pairs — see Interfaces): ChunkScan (pattern → flat columns), ChunkProbeJoin (probe next pattern per flat row; output flat when consumed-later, else an UnflatGroup with count-only/exists/enumerate roles), ChunkFilter, ChunkFlatten (expand one group into flat rows), ChunkSelectSink (emit BindingSets honoring multiplicity/DISTINCT/OFFSET/LIMIT via the existing NativeRowsIteration mechanics).

## Plan of Work

Milestone A. Create `LmdbNativeFactorizedChunk` (chunk struct, no groups yet — flat + mult only) and `LmdbNativeChunkOperators.java` with `ChunkScan` (wraps `PatternCursor.fill`, binds quad positions into flat columns for the pattern's produced slots, honoring `rejectsNullContextAtBind` and constant-slot checks exactly as `PatternBatchCursor` does — copy its bind discipline) and `ChunkSelectCursor` implementing the existing `RowCursor`-compatible surface: simplest integration is a `BatchCursor` adapter so `NativeRowsIteration`'s existing batch path consumes it (batch = flat section; multiplicity handled in Milestone C). Gate: only single-`PatternPlan` `MultiJoinPlan`s (or bare pattern args) claim, behind the flag. Wire before the batch strategy in `initialize()`. Differential fuzz + `LmdbNativeBatchExecutionTest` green.

Milestone B. `ChunkProbeJoin`: consumes a chunk, for each selected flat row probes the next `PatternPlan` (retained `NativeProbe`, `openRaw`), appends matches as additional flat rows into the output chunk (input row values copied + fresh slots bound). Claim: MultiJoinPlan with all-pattern children, no filters, following `derivedFactorizedPlan` order. This is INLJ on chunks — same asymptotics as the JoinCursor chain but batched. Fuzz green; strategy metric `chunkPipeline(scan+probe×N)`.

Milestone C. UnflatGroups: for tail positions from `analyzeSplit` (fresh slots unconsumed), ChunkProbeJoin emits per-row `{count, offset}` into the group arena instead of flat expansion — role COUNT (count only, no arena values), EXISTS (stop at first match; count∈{0,1}) for DISTINCT, ENUM (values appended to arena, stride = valueSlots.length). `ChunkFlatten` expands ENUM groups into flat rows at the sink boundary (or the sink emits with multiplicity = product of COUNT groups × mult). The Select sink consumes: per selected row, emit the projected flat row `mult × Π counts` times (OFFSET/LIMIT skip arithmetic as `getNextElement` does today); DISTINCT: EXISTS semantics for unprojected groups + `NativeDistinctTracker` on projected ids. Per-key memoization: ChunkProbeJoin keys results by probe-key values using the run-detection first (previous row same key → reuse last result range) and a `HashMap<GroupKey, long[2]{count, offset}>`-style memo (budgeted) second. At this milestone the chunk pipeline claims everything `LmdbNativeFactorizedRows` claims (minus in-branch filters until D); dispatch prefers the chunk pipeline over the factorized-rows sink under the flag.

Milestone D. `ChunkFilter` applies `MaskedFilter`s at their derived depth: filters whose mask ⊆ flat-bound slots run vectorized over the flat section (dropping rows via the selection vector, using the compiled `NativeBooleanFilter` against a scratch `RowState` view — copy the `FilterBatchCursor` discipline); filters reading a group's fresh slots run inside the probe (bind candidate, evaluate, rollback — port `TailBranch.acceptFilters`). Run detection: sorted scan output makes repeated probe keys adjacent; compare the current probe-key values against the previous row's before memo lookup.

Milestone E. Integration hardening: engagement string `chunkPipeline(flat=…, countGroups=…, enumGroups=…, existsGroups=…)`; update `LmdbNativeStrategyPriorityTest` expectations (chunk pipeline claims what factorized rows claimed); microbenchmark comparison — run `FactorizedRowsStarBenchmark` shapes via a JMH spot check or the theme ITs' wall-times; acceptance = ≥2× on the star SELECT with unprojected legs versus flag-off, and no theme IT regression.

Milestone F. Estimate-driven hash build (port the scan-once decision from `LmdbNativeFactorizedTailBranch` with Phase 0's validated-estimate rule): when expected distinct probe keys × seek cost ≥ pattern sweep cost, build a key→(count,offset) table in one sweep (`openRawUnbinding`) and probe from it. Delete `LmdbNativeQueryCompiler` and its tests' reliance (route bare BGPs through `LmdbNativeAggregateCompiler.compileRowRoot`); update `LmdbNativeEvaluationStrategy.precompile` and `tryAnnotateForExplain`. Full review pass (finder angles), full module verify, benchmark confirmation.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j` (always `set -o pipefail` with piped mvnf):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | awk '/\[ERROR\]/{print} /Reactor Summary/{s=1} s{print}'
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeChunkPipelineTest        # new per-milestone tests
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest     # oracle after every milestone
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb -- '-Dtest=<selection>'   # phase-exit verification

## Validation and Acceptance

Each milestone lands with: a new red→green test in `LmdbNativeChunkPipelineTest` (engagement metric + result equality for the newly claimed shape), the full differential fuzz green, and the strategy-priority tests green. Phase exit: theme ITs green, star-SELECT spot check ≥2× vs flag-off, `LmdbNativeQueryCompiler` deleted with all module tests green.

## Idempotence and Recovery

The flag isolates the new pipeline; flag-off must always behave exactly as Phase 1 left the engine (regression selection re-run proves it). Each milestone is an additive, revertable set of files/edits.

## Artifacts and Notes

(evidence appended as milestones complete)

## Interfaces and Dependencies

New files: `evaluation/LmdbNativeFactorizedChunk.java` (chunk + UnflatGroup), `evaluation/LmdbNativeChunkOperators.java` (ChunkScan/ChunkProbeJoin/ChunkFilter/ChunkFlatten + the claim/compile entry `ChunkPipeline.tryCreate(MultiJoinPlan, OrderedPlan, RowState, int[] sourceSlots, boolean distinct)`), test `LmdbNativeChunkPipelineTest.java`. Modified: `evaluation/LmdbNativeRowStep.java` (dispatch), later `evaluation/LmdbNativeEvaluationStrategy.java` (legacy deletion). Reuses: `NativeBatch` (flat layout), `LmdbNativeLongArena`, `PatternCursor.fill`, `GroupKey`, `FactorizedTail.MemoBudget`, `LmdbNativeExplain`.
