# Phase 1: engage LMDB factorization more often and lay the arena substrate

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`. It is Phase 1 of the approved umbrella plan (user plan file `users-havardottestad-documents-programm-binary-squid.md`; Phase 0 was `.agent/execplan-lmdb-factorized-correctness-audit.md`, complete 2026-07-13).

## Purpose / Big Picture

The LMDB native engine's factorized SELECT path (`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedRows.java`) avoids enumerating join results whose variables the query never consumes — but three gates keep it from firing on queries it would speed up: (1) the columnar batch/hash-join path is tried first in `NativeRowsIteration.initialize()` (`evaluation/LmdbNativeRowStep.java`, around line 551) and enumerates the full join even when factorization would collapse it to counts; (2) `MultiJoinPlan.derive()` (`evaluation/LmdbNativeJoinPlans.java`) keeps the compiler-provided join order verbatim, so an unconsumed "hub leg" pattern sitting mid-order blocks the trailing-suffix factorization split; (3) `LmdbNativeFactorizedRows.tryCreate` rejects the whole plan unless every ordered child is a triple-pattern scan, even though only the tail branches need to be patterns. After this phase, all three gates are opened, each verified by the `nativeExecutionStrategy` explain metric introduced in Phase 0 plus the differential fuzz harness; and a `LongArena` class (chunked long[] storage, the Kuzu DataBlock analog) exists with unit tests, ready for the Phase 2 chunk pipeline.

## Progress

- [x] (2026-07-13) ExecPlan authored.
- [x] (2026-07-13 17:40Z) Milestone 1: sink-priority inversion — red→green via the engagement metric
  (`LmdbNativeStrategyPriorityTest#countingFactorizationOutranksBatchHashJoin`); review-hardened into the
  allocation-light `plansCountingBranch` probe sharing `analyzeSplit` with `tryCreate`.
- [x] (2026-07-13 20:30Z) Milestone 2: sinking in `derivedFactorizedPlan` (separate order cache; plain `open()`
  untouched), unit-spec'd in `LmdbNativeFactorizedSinkTest` (5 cases incl. filter-exclusion, seed-consumption,
  pruning guard, keep-smallest driver); e2e guard `unconsumedMidPlanPatternSinksToBranch` (countBranches=2).
- [x] (2026-07-13 20:50Z) Milestone 3: prefix gate relaxed to branches-only; `canFlatten` admits `ValuesPlan`
  (full-binding rows only) and `MultiValuePatternPlan`; red→green `valuesSeededStarStillFactorizes`.
- [x] (2026-07-13 19:20Z) Milestone 4: `LmdbNativeLongArena` + unit tests; simplified per review (reset releases
  blocks; no reuse bookkeeping).
- [x] (2026-07-13 23:50Z) Milestone 5: phase review complete — 8 finder angles, verified findings fixed
  (see Surprises): VALUES-UNDEF soundness class (incl. an upstream FilterOptimizer bug, fixed at the root with a
  red→green pin in `FilterOptimizerTest`), eager-planning waste, ENGAGED semantics, rows memo cumulative budget,
  budget release on scan-once flip, bypass-counter contention, per-branch strategy stamps, strictCompare hoist,
  GenericPlanNode-based test parsing, fuzz PER_CLASS lifecycle + UNDEF arm + pinned queries. Final verification:
  queryalgebra-evaluation 769/769; lmdb selection green including all module ITs and the two theme snapshot ITs
  that caught the over-conservative first fix (SOCIAL_MEDIA:0 canonical shape restored by the refined
  sibling-rebind-only relocation predicate).

## Surprises & Discoveries

- Observation: the mid-plan-sinking end-to-end test shape (chain + leg) turned out to already factorize without
  sinking — the optimizer happened to order the chain link before the leg. The trailing-suffix walk is more capable
  than assumed; sinking's value is for orders where an unconsumed pattern precedes a consumed-producer. The sinking
  spec therefore lives in a direct unit test (`LmdbNativeFactorizedSinkTest`), with the e2e test kept as a regression
  guard.
- Observation: widening `canFlatten` to admit `ValuesPlan` broke a deliberately pinned design test
  (`slotPlanJoinKeepsValuesBeforeBroadPattern` asserted the JoinPlan shape as a proxy for order preservation) — and
  the review's cross-file tracer found the REAL hazard behind that pin: `ValuesPlan.producedMask` is a union over
  rows, so VALUES with UNDEF rows leaves slots unbound at runtime while bag planners (filter earliest-cover placement,
  the factorized prefix split) treat produced slots as bound → silently wrong results. Fixed by gating flattening on
  `ValuesPlan.bindsAllSlotsEveryRow`; the pinned test now asserts the order contract inside the bag (strictly stronger
  on the semantics that matter); pinned UNDEF differential queries + a random UNDEF arm added to the fuzz.
- Observation (process): piping mvnf output through `tail` masked the exit code — a test failure surfaced as exit 0.
  All earlier green conclusions had been cross-checked against report contents, but from now on verification commands
  use `set -o pipefail` and check the `[mvnf] Tests passed` marker.
- Observation: the review's altitude/efficiency angles found that the first-cut inversion fix ran the full
  `tryCreate` planning pass eagerly per `evaluate()` call (per OUTER ROW under correlated evaluation) and made the
  `ENGAGED` counter count planning instead of engagement. Replaced with an allocation-light `plansCountingBranch`
  probe that shares `analyzeSplit` with `tryCreate` so the two can never disagree.
- Observation (major, pre-existing upstream bug found by the new UNDEF fuzz coverage): the standard RDF4J
  `FilterOptimizer.FilterRelocator` pushes a filter below a join into an argument whose `getBindingNames()` covers the
  filter variables — the UNION, not a guarantee. For `VALUES ?b { :x UNDEF } ?s :p ?b . FILTER(?b != :y)` the filter
  lands on the `BindingSetAssignment`, evaluates on unbound ?b for UNDEF rows (error → drop), and silently loses
  solutions the join still produces — on EVERY store using the standard pipeline, both engines alike (which is why
  the engine-differential harness alone could not catch it). Two extra layers made this hard to pin: (1) parser-built
  VALUES rows are ListBindingSets whose `getBindingNames()` reports the DECLARED names even for UNDEF entries, so row
  jaggedness is only visible through `getValue(name) == null`; (2) an attempted principled fix — making
  `BindingSetAssignment.getAssuredBindingNames()` the row intersection — regressed an exact-plan pipeline test through
  an opaque optimizer interaction and was reverted in favor of a self-contained `assuredlyBinds` check inside
  `FilterOptimizer` (jagged-aware, value-based), pinned by
  `FilterOptimizerTest#doesNotPushFilterIntoValuesWithUndef` (red→green) and the lmdb pinned differential queries.
  Evidence: fuzz mismatch `native []  vs generic [16 subjects]`; post-fix both engines return identical, correct
  rows and all 769 queryalgebra-evaluation tests stay green. Same value-based row check applied to the lmdb-side
  `LmdbJoinPlanSupport.everyRowBindingNames` (used by the sketch optimizer's filter placement) and the `ValuesPlan`
  bag gate.
  UPDATE (2026-07-14): the lmdb-side part of this fix (edits to `LmdbJoinPlanSupport`, `LmdbDeferredFilterPlacer`,
  `LmdbSketchJoinOptimizer`) was REVERTED on user instruction — those cascades/sketch algebra optimizers are being
  replaced wholesale in a separate branch and are out of scope for this plan family. The core `FilterOptimizer`
  fix (+ its pinned test) and the native-side `ValuesPlan.bindsAllSlotsEveryRow` gate stay.

## Decision Log

- Decision: the inversion fix engages factorized rows ahead of the batch path only when at least one branch has a non-ENUM role (COUNT or EXISTS).
  Rationale: non-ENUM branches provably replace per-row enumeration with per-distinct-key counting — strictly less work than the hash join's full enumeration. When every branch is ENUM (all outputs projected), factorization degenerates to memoized enumeration whose relative cost against a hash join depends on key cardinality; the existing batch priority is kept there until the Phase 2 pipeline unifies the decision under one cost model.
  Date/Author: 2026-07-13 / Claude Code.
- Decision: primitive map-kit consolidation (7 near-identical open-addressed maps) is deferred out of Phase 1; only `LongArena` is built now.
  Rationale: consolidating the maps is churn without behavior change until the Phase 2/3 rewires replace their call sites anyway; the arena is the only substrate piece Phase 2 consumes on day one.
  Date/Author: 2026-07-13 / Claude Code.
- Decision (review cycle): flattening admits only bag members that bind every produced slot on every row —
  `ValuesPlan` gains `bindsAllSlotsEveryRow` and UNDEF-carrying VALUES stays outside the bag as a `JoinPlan`.
  Rationale: bag planners (filter placement, factorized splits) treat produced slots as bound; a union mask is not a
  binding guarantee. The alternative (a `SlotPlan.guaranteedMask()` distinct from `producedMask()`) is the right
  Phase 2 shape but touches every plan type; the gate is exact and minimal now.
  Date/Author: 2026-07-13 / Claude Code.
- Decision (review cycle): sinking excludes patterns whose exclusive slots any filter reads.
  Rationale: the re-placed filter would land at the sunk trailing depth, where `FactorizedTail.tryCreate`'s
  `maxFilterDepth >= last` gate disqualifies factorization outright — the sunk order must never claim fewer branches
  than the plain order (the derivedFactorizedPlan contract).
  Date/Author: 2026-07-13 / Claude Code.
- Decision (review cycle): the batch-yield check is a shared-analysis probe (`plansCountingBranch` /
  `analyzeSplit`), not an eager `tryCreate`.
  Rationale: `initialize()` runs once per `evaluate(bindings)` call — per OUTER ROW under correlated evaluation — so
  eager construction was allocation churn, and it made `ENGAGED` count planning instead of engagement. Sharing the
  split analysis keeps the probe and the builder incapable of disagreeing. This dispatch rule remains a documented
  stopgap until the Phase 2 chunk pipeline unifies strategy choice under one cost model.
  Date/Author: 2026-07-13 / Claude Code.
- Decision (review cycle): `LmdbNativeLongArena.reset()` releases all blocks instead of retaining them for reuse.
  Rationale: the reuse path needed offset-collision compensation (a real bug caught during design) and duplicated
  fit conditions; one 256 KiB allocation per reset is noise next to the copies the arena serves.
  Date/Author: 2026-07-13 / Claude Code.
- Decision (review cycle): deferred findings, tracked for Phase 2 — (a) `asLiteral`/`rdfTermCompare` reconstructs
  literals from decoded parts instead of materializing through the source value cache; exact-parity risk is bounded
  by the differential fuzz but the EQ/NE slow path should materialize via value ids and delegate wholesale to
  `QueryEvaluationUtil.compare` when the compiled filter has the id in hand (it also allocates per row on
  incomparable-literal-heavy filters); (b) explain's physical plan prints the unsunk order while factorized
  strategies run the sunk one — the describe surface should show both; (c) `OrderCache` stops inserting at capacity 8
  without eviction (pre-existing); (d) `LmdbNativeLongArena` has no production consumer until Phase 2 lands its first
  arena-backed memo (intentional substrate-first sequencing).
  Date/Author: 2026-07-13 / Claude Code.

## Outcomes & Retrospective

2026-07-13, PHASE 1 COMPLETE AND REVIEWED. All three engagement gates opened: the batch hash join now yields to
counting factorizations (probe-based, allocation-free, provably consistent with the builder), unconsumed patterns
sink to claimable tail positions in a factorized-only derived order (cost-guarded, filter-aware, unit-spec'd), and
VALUES/multi-value leaves join the reorderable bag so VALUES-seeded stars factorize. The `LongArena` substrate is
ready for Phase 2. The review cycle (8 finder angles + verification) found and fixed one **critical pre-existing,
engine-independent upstream bug**: the standard `FilterOptimizer` relocates filters below joins based on union
binding names, silently dropping solutions for VALUES-with-UNDEF joins on every RDF4J store; fixed at the root with
the minimal sound criterion (block relocation only for variables the argument does not always bind AND the join
sibling can rebind), pinned upstream and in the lmdb differential fuzz. Also fixed from review: eager per-outer-row
planning waste, ENGAGED counter semantics, the rows-side missing cumulative memo budget, budget release on the
scan-once flip, bypass-counter contention, and several test-robustness items.

Lessons: (1) engine-differential testing cannot catch both-engines-wrong algebra bugs — the UNDEF pin now lives
UPSTREAM at the optimizer level where the semantics are decided; (2) "conservative" fixes to optimizer placement
are not automatically safe — the first assured-names cut broke a canonical fast-plan shape (SOCIAL_MEDIA:0) that
legitimately relies on union-based placement for optional-only variables; the exact unsoundness condition
(sibling-rebindable, non-assured variables) had to be identified, not approximated; (3) exact-plan snapshot ITs
earn their keep — they caught both the over- and under-conservative versions within one session.

Remaining (tracked in the umbrella plan): Phase 2 chunk pipeline consumes `LongArena` and the probe/builder split;
the upstream `BindingSetAssignment.getAssuredBindingNames()` API fix is spawned as its own task (chip
`task_f45e354c`).

## Context and Orientation

Everything lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`. A "slot" is an index into a query-wide `long[] slots` array of dictionary ids (`RowState`); masks are 64-bit slot sets. `MultiJoinPlan` is a bag of inner-joined children (mostly `PatternPlan` scans) evaluated as index nested-loop joins in a fixed order derived once per bound-slot mask (`derive`, with filter depths assigned by an earliest-cover rule). `LmdbNativeFactorizedRows.tryCreate(plan, derived, row, seedMask, sourceSlots, distinct)` splits that order into a flat prefix and trailing "tail branches" — patterns whose fresh slots nothing later reads — each evaluated once per distinct probe key with roles ENUM (values enumerated by an odometer), COUNT (match count multiplies the emitted row's multiplicity), or EXISTS (semi-join under DISTINCT). Phase 0 added `LmdbNativeExplain.recordStrategy`, which stamps a `nativeExecutionStrategy` string metric (visible via `query.explain(Explanation.Level.Telemetry)`) naming the strategy that actually ran, including `factorizedRows(flatPrefix=…, enumBranches=…, countBranches=…, existsBranches=…)`.

Dispatch order today in `NativeRowsIteration.initialize()`: prefix-run cursor → ordered-distinct → NativeBatch (`MultiJoinPlan.openBatch` → `LmdbNativeHashJoin.tryOpen`: exactly 2 pattern children, no filters, 1–4 shared unbound key slots, both `staticEstimate >= rdf4j.lmdb.nativeHashJoin.minRows` default 4096) → parallel pipelines (root estimate ≥ 50000, ≥2 threads) → factorized rows → plain nested loop.

Tests: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeQueryExplanationTest.java` (explain assertions), `LmdbNativeDifferentialFuzzTest.java` (differential harness incl. the Phase 0 factorized suites), `evaluation/LmdbNativeHashJoinBatchTest.java`, `evaluation/LmdbNativeFactorizedRowsTest.java`. Runner: `python3 .codex/skills/mvnf/scripts/mvnf.py <Class#method>` from the repo root after the root quick install (`mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`).

## Plan of Work

Milestone 1 (inversion). Add a test to `LmdbNativeQueryExplanationTest` (or a sibling class) that loads ~4200 subjects each carrying `ex:p1` and `ex:p2` triples (so both patterns' static estimates clear the 4096 hash-join floor), disables `rdf4j.lmdb.parallel.enabled`, runs `SELECT ?s WHERE { ?s ex:p1 ?a . ?s ex:p2 ?b }` at `Explanation.Level.Telemetry`, and asserts the strategy metric starts with `factorizedRows`. It fails today (metric says `batch`). Then in `initialize()` insert, before the `NativeBatch` block, a factorized attempt that engages only when `tryCreate` succeeds and at least one branch role is not ENUM; the existing post-parallel factorized attempt stays for the all-ENUM case. Re-run the explanation test class, the hash-join batch test, and the fuzz class.

Milestone 2 (sinking). In `MultiJoinPlan.derive(long initialBoundMask)`, after cloning the compiler order, compute for each child the set of slots only that child produces (its "exclusive" slots, ignoring the seed mask). A child is sinkable when its exclusive slots are read by no other child (they may appear in filter masks — filter depths are recomputed after the reorder by the existing earliest-cover rule, which reassigns any filter reading the sunk pattern's slots to the pattern's new depth; filters are pure so placement does not change results). To avoid losing the early-pruning effect of selective patterns, guard: only sink a child whose `staticEstimate` is finite and at least as large as the smallest `staticEstimate` among the non-sinkable children (fan-out legs, not needles). Move sinkable children to a trailing suffix preserving relative order. Correctness: reordering an inner-join bag is multiset-neutral; sinking never unbinds a slot another child reads (exclusive slots by construction). Verification: full differential fuzz class (the star suites specifically exercise interleaved legs), `LmdbNativeJoinOrderCrossProductTest`, and the module IT suite (theme regression) to catch cost regressions.

Milestone 3 (gate relaxation). In `tryCreate`, replace the every-child-is-a-pattern check with per-position handling: positions are eligible branch candidates only if they are `PatternPlan`s without repeated slots; the `laterNeeds` walk uses `producedMask()` generically (it already does); `flatCount` shrinks only across eligible trailing candidates; the chunked prefix cursor is built only when all prefix depths are patterns (the `plan.openChain` fallback at the existing line ~200 handles mixed prefixes). Fuzz suites already generate VALUES/OPTIONAL/UNION prefixes through `randomOptionalUnionMinusValues`; add one targeted fuzz variant with a VALUES-seeded star to prove engagement (assert via the strategy metric in a deterministic companion test).

Milestone 4 (arena). New file `evaluation/LmdbNativeLongArena.java`: append-only chunked `long[]` blocks (32768 longs per block = 256 KiB), `long append(long[] src, int from, int len)` returning the arena offset, `void copyTo(long offset, long[] dst, int dstFrom, int len)`, `long get(long offset)`, `long size()`, `void reset()`, and a `budget` hook (max total longs; append returns −1 when exhausted). No thread safety (instances are pipeline-confined). Unit test `evaluation/LmdbNativeLongArenaTest.java` covering block-boundary appends, reset reuse, and budget refusal.

Milestone 5 (review). Run the code-review skill over the working diff, address findings, re-run: fuzz class, explanation class, factorized suites, module verify (units + ITs). Benchmark spot-check with `scripts/run-single-benchmark.sh` on `FactorizedRowsStarBenchmark` if time permits; theme IT wall-times serve as the coarse regression guard otherwise.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j`:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[ERROR\]/{print} /Reactor Summary/{s=1} s{print}'
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeQueryExplanationTest   # M1 red, then green
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLongArenaTest          # M4
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb -- '-Dtest=<final selection>'   # M5

## Validation and Acceptance

M1: the new explanation test fails before the dispatch change with the metric reporting `batch` and passes after reporting `factorizedRows(...)`; `LmdbNativeHashJoinBatchTest` stays green (all-ENUM cases still take batch). M2/M3: differential fuzz green (any mismatch prints the query + seed); theme ITs green with no wall-time blowup. M4: arena unit tests green. M5: review findings addressed; full selection green.

## Idempotence and Recovery

All steps re-runnable; tests use @TempDir stores. Each milestone is an independent, revertable edit; if a milestone's verification fails, fix or revert that milestone only.

## Artifacts and Notes

(evidence appended as milestones complete)

## Interfaces and Dependencies

No new libraries. New files: `evaluation/LmdbNativeLongArena.java`, test `evaluation/LmdbNativeLongArenaTest.java`. Modified: `evaluation/LmdbNativeRowStep.java` (dispatch), `evaluation/LmdbNativeJoinPlans.java` (derive sinking), `evaluation/LmdbNativeFactorizedRows.java` (gate), tests.
