# Rich estimates through the memo and cheaper omni witness probes

This ExecPlan is a living document maintained per `.agent/PLANS.md`. It is Phase 4 of the roadmap (Phase 1: `GH-0000-opaque-factor-join-enumeration.md`, Phase 2: `GH-0000-lmdb-standard-rule-parity.md`; Phase 3/LATERAL is blocked on merging `develop` — see the pending task chip).

## Purpose / Big Picture

Two goals with one root cause. Accuracy: when the cascades planner costs a join of two already-costed subplans, the subplans' rich estimates (per-variable distinct counts, sketch evidence, q-error) should flow into the parent estimate instead of collapsing to scalar row counts — bad star/bridged-join estimates beyond the first hop come from this collapse. Speed: the planner burns measured CPU re-deriving what it already computed — omni witness probes were ~14 % of planning CPU (JFR, 2026-07-02) and `CascadesCostModel.buildPlan` clones subtrees for every accepted winner. After this change, parent costing composes cached rich estimates and the probe/clone hot paths shrink, measured by a ThemeQueryPlanRunBenchmark planQuery A/B.

## Progress

- [x] (2026-07-04) Research complete; findings below.
- [x] (2026-07-04) Milestone 1 committed as 327771e45e. Red: `CascadesCostModelTest#parentJoinReusesCachedRichChildEstimateWithoutBindingProfile` (cold=50.0 == warm=50.0, pure scalar collapse); green via `cachedWinnerBag` identity lookup in `inputWinnerEstimate`. The estimate change surfaced TWO latent generic bugs, both fixed at the root in the same commit:
  1. Lossy IR round-trip: `TupleExprToIr.scalar()` degraded unsupported ValueExprs (IRIFunction, empty ListMemberOperator) to placeholder `urn:rdf4j:native:` function calls with dropped arguments, which `IrToTupleExpr` emitted as REAL FunctionCall algebra → "Unknown function" at evaluation. Fixed with `ScalarExpr.FunctionCall.opaque(iri, original)` carrying the original expression; round-trip restores a clone. Red/green: `TupleExprIrRoundTripTest#roundTripPreservesValueExprsTheIrCannotRepresent`.
  2. Unsound join-commute over BIND outputs: commuting `Join(Extension, consumer-of-output)` puts the Extension on the lookup side with its output pre-bound; `ExtensionIterator` overwrites instead of join-filtering, losing BIND error semantics (W3C SES-2250). Fixed with the new `noCrossBindOutputs` guard (RuleDsl + intrinsic registry + join-commute yaml + code spec); associativity preserves operand order so only commute needs it, and the subtree-level mask check also blocks commute-after-associate compositions. Red/green: `StandardRuleSpecTest#joinCommuteDoesNotFireAcrossBindOutputDependency` (+nested +still-fires-when-local variants).
  Verification: queryalgebra module green; `core/sail/lmdb` exact known-red baseline; LMDB W3C compliance IMPROVED beyond baseline — LmdbSPARQLComplianceTest 10→9 (bind()[3] fixed, bind()[6] restored) and LmdbSPARQL11QueryComplianceTest 6→5 (tests()[25] fixed): those pre-existing failures were earlier victims of the same unsound commute. New compliance baseline: 9 + 5.
- [x] (2026-07-04) Milestone 2 baseline profile — PREMISE INVALIDATED, milestone retargeted. Fresh async-profiler run (planQuery, q7, MEDICAL_RECORDS, omni, post develop-merge; collapsed profile kept under core/sail/lmdb/…z_queryIndex-7/collapsed-cpu.csv; ~329 ms/op on 3 short iterations): `probeOmniBridgeWitnesses` is now 0.2 % and `estimateOmniPatternWitnesses` 1.2 % of JVM CPU — the earlier witness-retention bounds and connected-join caches already absorbed the old 14 % figure. Do NOT build the probe-result cache. The real profile: planning path 27.5 % of JVM CPU, background estimator persistence 26.1 %, JVM/GC/other 46.4 %. Top planning leaves are cost-model bookkeeping churn: `overlayBindingProfile` 1.3 %, `EvidenceProfile.<init>`/`immutableVariables`/`equals` ≈ 2.7 %, `EstimateSnapshot.<init>` 0.6 %, `BindingProfile.toBagEstimate` 0.5 %, plus `AbstractQueryModelNode.clone` 1.2 % and `Join.getBindingNames` 0.7 %.
- [ ] Milestone 2 (retargeted): reduce evidence/profile object churn in `DefaultCascadesCostModel` — memoize `overlayBindingProfile` per (bag identity, profile identity), intern `EvidenceProfile.immutableVariables` copies, and skip the overlay when `cachedWinnerBag` already recovered the estimate the profile was derived from. Verify with the full-length A/B protocol (variance on short runs is ±150 %: 3×5 s minimum per side, back-to-back).
- [ ] Milestone 2b (benchmark hygiene): the planQuery measurement overlaps the estimator's background snapshot persistence (26 % of CPU, `LmdbSailStore.scheduleEstimatorPersist` → `persistIfDirty` writing the freshly built store's snapshot). The harness should await persistence quiescence before measurement iterations, or A/B comparisons are polluted.
- [ ] Milestone 3: buildPlan clone reduction — clone is 1.2 %; low priority.
- [ ] Final: planQuery/runQuery A/B (≥2×5 s warmup, ≥3×5 s measurement, back-to-back; see memory note lmdb-theme-benchmark-harness), retrospective.

## Surprises & Discoveries

- Observation: the scalar collapse is narrower than the roadmap assumed. The collapse point is `DefaultCascadesCostModel.inputWinnerEstimate` (core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesCostModel.java:1556): it discards the input winner's computed estimate and synthesizes `bagWithBindings(plan, winner.cost().rows(), …)` — a bag whose per-variable distinct counts default to the scalar row count. BUT when the winner's `deliveredProperties().bindingProfile()` is present, `overlayBindingProfile` (:1594) re-attaches per-variable estimates, so headline join rows often survive. What is irrecoverably lost today: winners without binding profiles, and estimate *evidence* (q-error interval, overlap/finite-relation metrics beyond the profile's double-metrics copy).
- Observation: the rich estimates already exist in a cache keyed exactly right. `physicalEstimateCache` (`IdentityHashMap<MemoExpr, Map<PhysicalInputKey, StatisticsEstimate>>`, CascadesCostModel.java:137-ish) stores the full `StatisticsEstimate` (with `BagEstimate`) per (memo expression, bound-vars mask, input winners). An input winner's own rich estimate can be recovered by `winner.expression()` identity lookup — no API change to `Winner`/`EstimateSnapshot` needed. `EstimateSnapshot` is scalar-only (rows/workRows/cost/metrics) and extending it would change `Winner`→`PlanProvenance` record equality, which `PhysicalInputKey` hashing depends on — avoid.
- Observation: `EstimateMath.innerJoin(left.bag(), right.bag(), sharedVars)` (join composition, CascadesCostModel.java:1091) is the consumer — richer input bags immediately improve multi-way composition; no changes needed downstream.
- Observation: `buildPlan` is invoked once per accepted winner (CascadesPlanner.java:591) after `canAddWinner` passes, so its cost is proportional to accepted alternatives (frontier limit 16 per goal), not to all explored combinations. Lazy plan construction would need `Winner.plan` consumers (telemetry `alternativeAccepted`, metric stamping, plan extraction) to accept suppliers — medium refactor, defer until re-profiled.

## Decision Log

- Decision: recover input-winner richness via an identity-keyed lookup into the cost model's own estimate caches rather than adding a `BagEstimate` to `EstimateSnapshot` or a field to `Winner`.
  Rationale: record equality of `Winner`/`PlanProvenance` participates in `PhysicalInputKey` cache keys; changing those components risks silent cache-behavior changes and deep-equality costs on hot paths. The identity lookup is contained in `DefaultCascadesCostModel`.
  Date/Author: 2026-07-04 / Claude
- Decision: Milestone 2 (witness probes) follows the docker-jfr-benchmark-loop evidence workflow (baseline JFR → change → rerun), Routine B with pre/post benchmark green plus hit proof, because it is behavior-preserving caching.
  Date/Author: 2026-07-04 / Claude

## Context and Orientation

The cascades cost model (`CascadesCostModel.DefaultCascadesCostModel`, core/queryalgebra/evaluation/.../optimizer/cascades/CascadesCostModel.java) costs each memo expression given the winners chosen for its inputs (`localCost`, :341). For a physical Join/LeftJoin/Difference/unary parent it composes input estimates (`estimateConcretePhysicalJoin` etc., :1081+) from `inputWinnerEstimate` (:1556). A `StatisticsEstimate` (…/optimizer/cascades/StatisticsEstimate.java) carries a `BagEstimate` (…/optimizer/cost/BagEstimate.java) with per-variable estimates and evidence; `EstimateMath` (…/optimizer/cost/EstimateMath.java) implements bag composition. "Winner" (…/cascades/Winner.java) is a costed physical alternative; its `provenance().estimate()` is a scalar `EstimateSnapshot`. The omni witness probes live in `SketchBasedJoinEstimator.probeOmniBridgeWitnesses`/`estimateOmniPatternWitnesses` (core/queryalgebra/evaluation/.../sketch/SketchBasedJoinEstimator.java, ~:4780+) and are invoked per candidate join prefix from `LmdbEvaluationStatistics`; witness retention bounds landed in commit 48d9323f11, but probe results are not yet cached per (relation, key set, retention) across prefixes.

Benchmark harness: `ThemeQueryPlanRunBenchmark` (core/sail/lmdb, JMH test scope) `planQuery`; run from `core/sail/lmdb/` with the classpath recipe and store-directory mapping documented in the memory note `lmdb-theme-benchmark-harness` (symlink `complete-omni -> complete`; `-Drdf4j.benchmark.profiling=true`; high variance — ≥2×5 s warmup, ≥3×5 s measurement, back-to-back A/B). JFR loop: `.claude/skills/docker-jfr-benchmark-loop`.

Known-red baseline: see the Phase 1 ExecPlan section "Pre-existing known-red"; nothing new may fail.

## Plan of Work

Milestone 1 (accuracy, Routine A). Failing test first in `core/queryalgebra/evaluation/src/test/java/.../cascades/CascadesCostModelTest.java` (44 existing tests show the harness idioms): build a physical join memo expression whose left input winner was previously costed by the same cost model instance with a rich estimate (per-variable distinct count ≪ rows, no binding profile on deliveredProperties), and assert the composed join estimate uses the cached distinct count rather than the scalar fallback (expected rows differ by construction). Then change `inputWinnerEstimate` to first consult a new `cachedWinnerEstimate(Winner)` helper: identity lookup of `winner.expression()` in `physicalEstimateCache` (and a new sibling cache for leaf expressions, populated where `computeEstimateForLocalCost` returns leaf estimates), validated by `estimate.rows() == winner.cost().rows()` before reuse; fall back to today's synthesis when absent. Keep the binding-profile overlay as a second layer on top of the recovered bag.

Milestone 2 (speed, Routine B). Baseline JFR via the docker-jfr-benchmark-loop skill on one witness-heavy theme query; add a probe-result cache in `SketchBasedJoinEstimator` keyed by (omni relation, canonical key material, retention bound, estimator revision) — the probes are deterministic per snapshot; verify identical estimates (existing estimate tests as hit proof + pre/post green) and rerun JFR + planQuery A/B for the payoff evidence.

Milestone 3 (speed, conditional). If post-M2 JFR still ranks `CascadesCostModel.buildPlan`/clone high, prototype deferring plan materialization to winner acceptance (or memoizing cloned subtrees per (expression, input winners) identity), gated by the same A/B.

## Validation and Acceptance

Milestone 1: the new CascadesCostModelTest case fails before and passes after; `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation` and `... core/sail/lmdb` show no new failures vs the known-red baseline; LMDB compliance classes unchanged (6 + 10). Milestones 2–3: identical plan snapshots on the theme corpus (query-plan-snapshot-cli), planQuery A/B improvement reported with run configuration, JFR before/after flame data retained under logs/.

## Idempotence and Recovery

Milestone 1's lookup degrades to today's behavior when the cache misses; a system property is not needed (pure containment in DefaultCascadesCostModel). Milestone 2's cache must be keyed by estimator revision so mutations invalidate naturally; if verification finds any estimate drift, drop the cache (single revert).

## Interfaces and Dependencies

No public API changes. New private members in `DefaultCascadesCostModel` (leaf estimate cache + `cachedWinnerEstimate`); new private cache in `SketchBasedJoinEstimator`.
