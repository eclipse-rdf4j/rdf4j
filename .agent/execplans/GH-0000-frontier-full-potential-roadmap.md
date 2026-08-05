# Frontier sketches: full-potential roadmap

This is a prioritized roadmap, not a single-feature ExecPlan. Each phase is sized to become its own
PLANS.md-conformant ExecPlan when work starts. It was produced by a code review of the
`GH-0000-lmdb-predicate-guarantees` branch (2026-07-26) covering the new mapped query index
(`core/sail/lmdb/.../frontier/FrontierQueryIndex.java` and friends), the full
`LmdbFrontierPackedCostSession`, `FrontierSynopsisBuilder`, `FrontierLinearTransforms`, the research
contract in `.agent/research/frontier-omnisketch/README.md`, and the parent plan
`GH-0000-frontier-omnisketch-rdf4j.md`.

## Progress

- [x] (2026-07-26) Phase 0 implemented: dedicated `frontierQueryIndexBudgetBytes` config property
  (LmdbStoreConfig/LmdbStoreSchema, wired in LmdbSailStore), delegating constructors reference
  `FRONTIER_INITIAL_MATERIALIZATION_WORK_UNITS`, NaN insert-design coercion documented in
  `LmdbFrontierSynopsisService.ReadAccumulator`, config test coverage extended.
- [x] (2026-07-26) Phase 1 implemented: single-pass buffered leaf materialization (per-leaf arena-
  reserved match buffer replayed into writers; work charge is one scan, not two), per-leaf work
  budgeting with per-leaf scalar fallback (deterministic relation-ID order; new
  `plannedFrontierQueryIndexExcludedLeaves` / `...MatchedRows` telemetry), verification-free
  O(log n) counting in `FrontierQueryIndex.countMatches` via `FrontierLeafSelector.verificationFree`
  with an adversarial named-context regression test. Permutation intersection DEFERRED: scanning the
  smallest equal-range with columnar verification is already O(smallest); a gallop-intersection has
  the same asymptotics here and only sharpens the work predictor, so it is not worth the complexity
  until telemetry shows over-prediction excluding leaves.
- [x] (2026-07-26) Phase 2 implemented: `extendInner` is single-pass via a growable arena-charged
  `EmissionBuffer` (store probes and Value round-trips halved for every bridge/star extension), and
  on database-exact generations bridge probes run against the mapped query index in pure ID space
  (`visitMatchingIndexRows`/`probeIndexRow`) with a 64x mapped-probe work budget
  (`MAPPED_PROBE_WORK_MULTIPLIER`); the probe-exhaustion integration test now pins its intent with a
  zero refinement budget. EXISTS/OPTIONAL/MINUS index-backed probes still TODO.
- [x] (2026-07-26) Phase 3 implemented via the refinement below: `FrontierSynopsisBuilder.selected`
  made package-visible, `FrontierQueryIndexView.centerComplete` + lease passthrough, and a
  per-retained-tuple mapped/store dispatch in `LmdbFrontierPackedCostSession.visitMatchingRow` —
  star extensions and complete-center bridge targets probe the mapped index (pathwise identical to
  the store probe), everything else keeps the exact snapshot cursor. Unit test
  `sampledViewReportsCenterCompleteness` covers survivor / retained-heavy / absent-non-survivor /
  nonpositive ids.
- [x] (2026-07-26) Verification: LMDB module verify green at the Phase 0-2 state (1,542 tests, 0
  failures, 0 errors, 17 skipped); Phase 3 focused suites green (FrontierQueryIndexTest 6,
  LmdbFrontierPlanningIntegrationTest 32); Phase 3 final state fully verified — audit harness 39/39
  and complete LMDB module verify at 1,543 tests, 0 failures, 0 errors, 17 skipped.
- [x] (2026-07-26) Phase 4 implemented: `FrontierMultinomialResampler` (stateless
  `FrontierSeedSchedule` draws in the `RESAMPLE` domain, seeded from the input state's canonical
  seed plus the operation recipe ordinal so physical exploration order cannot change particles)
  with an exhaustive-expectation oracle test —
  all 81 outcomes of a 3-category/4-draw model enumerated with exact probabilities, verifying
  conditional unbiasedness for four linear functionals plus pathwise total-mass preservation.
  `extendInner` now resamples over-budget bags down to the probe budget (entry weight becomes
  `k_i * W / m`, zero-draw entries skipped) instead of interning `unresolved`; a resampled output is
  always `MEASURE_UNBIASED` even from a database-exact input, and a zero budget still degrades.
  Integration test `overBudgetExactProbeResamplesInsteadOfDegrading` (100-entry bag, budget 64)
  proves authority is retained. Outer kernels (EXISTS/OPTIONAL/MINUS) keep their budget gates —
  resampling those is future work.
- [x] (2026-07-26) Race fix surfaced by the Phase 4 module verify:
  `LmdbFrontierSynopsisService` deleted the durable dirty/insertion markers (plus a directory
  fsync) before writing the volatile `READY` status, so an observer keying on a vanished marker —
  the audit-harness calibration test, and any planner polling the same way — could read a stale
  `DIRTY_INSERTION` status. Both publication sites now publish `READY` before deleting markers; a
  crash between the two only repeats one coalesced background rebuild.
- [x] (2026-07-26) Phase 4 final verification: audit harness 39/39 across three consecutive runs
  (race fix holds), complete LMDB module verify at 1,547 tests, 0 failures, 0 errors, 17 skipped.
- [ ] Theme benchmarks (planQuery p95 target) not yet re-measured.
- [ ] Phases 5-7 not started.

## Mission statement and current gap

The Frontier sketches exist to estimate the output size of queries with several star joins connected
by bridges (subject-to-subject and object-to-subject). Today that estimation works, but only via
"sample the first factor from the sketch, then exact-probe the store per retained particle"
(`BRIDGE_TRANSFER` in `extendInner`). The sketch machinery built specifically for composition —
center-coordinated complete adjacency (the proved `coordinatedStar` construction), the O2S
direction, design lane 1, both audit lanes, per-record heavy exactness, bounded resampling, and
sampled bridge mutation — is persisted and paid for but unread/unimplemented. Under load the only
move is degradation to the scalar estimator: probe budget 4,096 particles, leaf work budget 262,144
rows per session, all-or-nothing.

## KPI (defined before any work)

Extend `LmdbEstimateAuditHarness` to emit, across the 117 theme queries, a histogram of Frontier
status per algebra node (authoritative / degraded reason). The roadmap's success measures are:

1. fraction of star/bridge join nodes carrying `MEASURE_UNBIASED` or `DATABASE_EXACT` evidence;
2. planning-time store probe count (new telemetry counter) and preparation p95 (target from the
   query-index plan: p95 <= 5 ms, max <= 10 ms);
3. q-error on the audited nodes, unchanged or better at every phase.

## Phase 0 — Land the current branch clean (days)

The query-index ExecPlan has two unchecked boxes: focused/module verification and the
planning/complete-theme benchmarks. Run them (no-red policy: triage every red). Then small guards:

- `LmdbFrontierSynopsisService` record-sink NaN inclusion-probability coercion silently replaced a
  corrupt-record throw; either restore the throw or document the "insert payload predates base
  design" intent with a test.
- Give the sidecar byte budget its own config property; `LmdbSailStore` currently passes
  `getFrontierQueryMemoryBudgetBytes()` (a per-query RAM budget) as the on-disk index budget.
- Replace the hardcoded `262_144L` defaults in `LmdbEstimatorRuntime` / `LmdbEvaluationStatistics`
  delegating constructors with `LmdbStoreConfig.FRONTIER_INITIAL_MATERIALIZATION_WORK_UNITS`.
- Fix `queryIndexVisitedRows` telemetry (currently definitionally 2 x candidateRows).

## Phase 1 — Remove the leaf-path budget cliffs

1. Single-pass leaf materialization: in the count pass, buffer matching row positions (int per
   match, bounded by the work budget, <= 1 MiB); the write pass replays the buffer instead of
   rescanning and re-verifying. Charge `candidateRows + matches` instead of `2 x candidateRows`.
2. O(log n) count shortcut in `FrontierQueryIndex.countMatches`: when the selector constrains only
   the chosen component (no other constants, empty equality mask, no named-context restriction),
   return `range.size()` without scanning.
3. Per-leaf budgeting with per-leaf fallback: `estimateLeaf` already falls back per-leaf when
   `leaf.state == null`; stop abandoning the whole session when one unselective pattern
   (e.g. `?s ?p ?o`) blows the aggregate budget — materialize the leaves that fit.
4. Sorted-permutation intersection: equal-ranges are row-ID-sorted (builder tie-breaks by row ID),
   so two bound components can be gallop-merged instead of scanning the smaller range and verifying.

Acceptance: no theme query loses all Frontier evidence because of one unselective pattern; prep
p95 target still holds; audit KPI node coverage strictly increases.

## Phase 2 — Halve and then bypass probe I/O

1. Single-pass `extendInner`: buffer emissions (tuples + weights) during the count pass, then size
   the writer and replay. Today every DP extension executes its real LMDB probes twice.
2. Index-backed probes when the generation is `databaseExact`: the mapped index then IS the full
   explicit S2O quad set, so implement `FrontierExactMultiplicityProbe` /
   `FrontierExactTupleExpansion` against the lease (selector built from the retained tuple's IDs
   plus factor constants) — pure long-space, no `valueStore.getValue`/`getId` round-trips, no LMDB
   cursors. Account mapped probes in a separate (much larger) work-unit budget than store probes.
3. Same treatment for EXISTS/OPTIONAL/MINUS refinement when the RHS is a single statement pattern.

Risk control: a differential test proving index answers == `statementSource` answers under
`databaseExact` (contexts, quoted triples, named-graph scope) before switching.

### Phase 3 refinement (2026-07-26, from implementation analysis)

A stronger and safer formulation than a new `coordinatedStar` operation: because selection is
center-coordinated, a mapped probe with subject `s` is **pathwise identical** to the exact store
probe whenever the view is complete for `s`:

    completeFor(s) := base.databaseExact
                   || selected(laneHash(s, DESIGN, 0), base.inclusionProbability)   // s survived
                   || baseHasRowsFor(s)                                             // s is heavy

`laneHash`/`selected` are deterministic and already live in `FrontierSynopsisBuilder` (same package
as the view). If `s` survived, the base generation retains s's complete adjacency (that is the
design invariant), inserts are exact additive, and deletions invalidate the whole synopsis — so the
mapped probe enumerates exactly the store's rows (emission weight = input weight, row weight
ignored). Star extensions always satisfy the test (the center came from a retained subject
position, hence survived); object-to-subject bridge targets satisfy it with probability ~p plus the
heavy mass, and the remainder falls back to the per-tuple store cursor. No new estimator math, no
new bias risk, and the existing exact-probe integration suite doubles as the equivalence oracle.
The sampled-sampled HT-weight composition (distinct-center event pricing) remains future work and
must go through exhaustive-expectation oracle tests before production.

## Phase 3 — Implement `coordinatedStar`

The builder retains complete adjacency per surviving center (selection is by
`laneHash(center) < p`), heavy centers exact — precisely so a k-arm same-center star can be
estimated with one inclusion event per center and a single `1/pi` weight (parent plan Milestone 4).
No such composition exists in code; star arms currently pay per-particle store probes.

- Track the coordination center in the evidence-state key (the `correlationScope` slot exists).
- In `appendFactor`, when the new factor shares the input state's coordination center, expand arms
  from the mapped index (the subject permutation gives contiguous per-center runs) instead of
  probing; weight `1/pi` once per center. Otherwise fall through to `BRIDGE_TRANSFER`.
- Conservative first version emits residual mass only; exact-heavy lineage preservation waits for
  Phase 5's per-record flag.
- Oracle tests first (rational enumeration in the `FrontierExactExpectationTest` style): star
  cardinality, single-inclusion-event variance, repeated arm variables.

Acceptance: the bare two-star/one-bridge synthetic (THEME_COVERAGE missing-shape list, items 1–2)
plans with zero store probes for star arms; probe counter shows the drop; estimates match the probe
path within tolerance.

## Phase 4 — Bounded conditionally-unbiased resampling 

The theory's answer to bag growth is bounded resampling; `EvidenceStateSummary.resamplingVariance`
exists but nothing fills it, and oversized bags go `unresolved`. Implement the resampling transform
in `FrontierLinearTransforms`, trigger it when a bag exceeds the refinement/emission caps, keep
degradation for genuine effective-sample-size collapse (config `frontierTargetRelativeStandardError`
already exists). Oracle tests with exact rational expectations are mandatory before wiring.

Acceptance: hub-heavy prefixes beyond 4,096 particles retain `MEASURE_UNBIASED` on synthetic
many-to-many chains instead of `unresolved`; no bias in oracle tests.

## Phase 5 — Sidecar format v2: per-record exactness, O2S, exact-mode dedup 

- Add a per-row flags column (VERSION=2) carrying record-level `databaseExact`. Heavy centers are
  exact even in sampled generations; a leaf anchored on a heavy subject can then be certified
  `DATABASE_EXACT` (today it is capped at `MEASURE_UNBIASED` and sampled zeros trigger
  `sampled_zero_not_authoritative`). Also enables exact-heavy lineage in `coordinatedStar`.
- Add the O2S lane-0 rows as a second sidecar per generation for object-centered coordinated stars
  (only worth building after Phase 3 lands).
- Exact-mode dedup upstream: when p == 1.0 every quad is written ~8x ((2 design + 2 audit lanes)
  x 2 directions of identical records). Store one copy and derive the rest, cutting the
  complete-theme payload from ~267 MiB toward ~33 MiB and making every rebuild/verify ~8x cheaper.
  This touches the authoritative payload format — separate ExecPlan, version bump, migration test.

## Phase 6 — Cash in the lanes: variance and audit 

- Materialize the selected plan's key states from design lane 1 as a paired replicate; populate the
  currently-empty interval fields (`intervalKind=NONE`, upper `inf`, confidence 0) with empirical
  variance.
- Use the audit lanes for their stated purpose (two-lane design decision): one-shot post-planning
  validation of the selected state, feeding `LmdbOperatorFeedbackStats`.
- The packed cost model can then break ties risk-aware (relevant to the 2026-07-23 benchmark
  regression groups: anchor tie-breaks, cost saturation).

## Phase 7 — Sampled bridge mutation (research-gated)

Importance-weighted with-replacement draws (`m_i` from a full-support proposal) for large-fanout
bridges, as the middle ground between exact enumeration and `unresolved`. Needs a proposal source
(per-center run lengths from the index, or the exact predicate degrees the parent plan describes).
`frontierDefensiveProposalEpsilon` config already exists. Gate: only as an alternative to
`unresolved`, never replacing affordable exact enumeration; the impossibility theorem (variance
`chi^2/K`, exponential particle demand with bridge depth) bounds expectations. The
witness-survival bridge-anchoring ideas from the 2026-07 omni coordination analysis remain
research-only until a proof exists.

## Standing constraints

- Never compose two same-lane sampled measures by naive multiplication (covariance bias — proved).
- The manifest stays the sole authority; sidecars remain derived and disposable.
- No-red baseline policy applies at every phase boundary.
- Zero-budget and OFF modes must keep costing nothing.

## Hygiene backlog (opportunistic, any phase)

- Scalar-path assist: let `LmdbEvaluationStatistics` use `countMatches` for exact statement-pattern
  cardinalities and exact-zero certificates when the generation is `databaseExact`.
- Reuse open sidecar mappings across insert publications instead of re-opening and re-hashing every
  generation on each publish.
- Probe/coordination telemetry counters (needed by the KPI anyway).
