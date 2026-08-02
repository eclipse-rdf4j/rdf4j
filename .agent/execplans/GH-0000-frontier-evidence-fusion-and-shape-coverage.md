# Complete Frontier evidence fusion and query-shape coverage

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md`.

## Purpose / Big Picture

LMDB's Frontier estimator already carries query-local weighted tuple samples through several relational operators,
but it currently works beside rather than with RDF4J's learned evidence. In the default configuration Frontier is
authoritative, while the learned filter and LEO services are initialized through the disabled legacy-sketch path.
When LEO does produce a scalar correction, the authoritative Frontier wrapper subsequently overwrites it. Filters
learn only when their input reduces to one statement pattern. As a result, repeated executions cannot repair a bad
Frontier mass estimate and learned filter selectivity cannot improve a multi-star or multi-bridge query.

After this work, a supported LMDB plan first constructs a raw Frontier state, restricts that state with any
deterministic filter, and then applies qualified learned evidence inside the state lineage. The calibrated mass
continues through later bridges. Exact database evidence is never changed. A user can observe the result in an
explain plan: the plan reports the raw Frontier rows, the learned source and correction, and the final rows. A
completed query trains later unseen queries with the same canonical subgraph or filter surface. Operators without a
proved state transform form a local typed boundary instead of disabling Frontier for the entire query.

## Progress

- [x] (2026-07-29 12:24Z) Read repository and performance instructions, inspect the dirty branch, and complete the
  required JDK 25 root clean install.
- [x] (2026-07-29 12:24Z) Reproduce and preserve the two relevant baseline failures: repeated-predicate transition
  provenance and the missing SPARSE default benchmark parameter.
- [x] (2026-07-29 12:32Z) Add and preserve the first failing contracts for learned-calibration state types,
  safe-correction default rollout, default learned-service lifecycle, and root evidence-state propagation.
- [x] (2026-07-29 12:52Z) Restore the packed hash-join implementation path and clear both relevant baseline
  failures without weakening their assertions.
- [x] (2026-07-29 12:59Z) Decouple default learned-service construction from the legacy sketch, make safe
  correction the default, preserve root evidence IDs, and implement the O(1) calibrated-payload overlay contract.
- [x] (2026-07-29 13:34Z) Apply qualified LEO corrections to raw root and prefix Frontier states, propagate their
  effective mass through a later bridge, and preserve calibrated lineage through projection, filter, UNION,
  OPTIONAL, and MINUS transforms.
- [x] (2026-07-29 13:41Z) Replace the plan cache's LEO-only discriminator with a unified revision containing
  snapshot, LEO, filter-learning, and Frontier-availability state.
- [x] (2026-07-29 14:08Z) Make filter observations completion-aware across generic, EXISTS, and binding-assignment
  iterators; reject sliced, partial, SERVICE, and volatile executions while allowing complete deterministic nested
  filters.
- [x] (2026-07-29 14:08Z) Persist exact and generalized topology-aware filter surfaces with legacy sidecar
  compatibility, and apply confidence-blended learned filter calibration to the actual explored Frontier prefix.
- [x] (2026-07-29 14:25Z) Replace whole-query property-path rejection with local typed boundaries, preserve ORDER
  as an identity transform, and make state/output keys follow the actual selected-plan input.
- [x] (2026-07-29 14:25Z) Remove canonical powerset preallocation and the one-word/four-column composition ceilings;
  a 65-factor connected star now creates only explored keys and publishes its complete Frontier state.
- [x] (2026-07-29 14:39Z) Materialize exact VALUES, singleton, and empty leaves with nullable masks and query-local
  value IDs; compose them in either join order, including disconnected exact products.
- [x] (2026-07-29 14:55Z) Apply LEO after raw UNION, OPTIONAL, MINUS, and FILTER transforms, publish one consistent
  Frontier physical mode, activate unary multiplier evidence, and prove learned-filter evidence prevents duplicate
  unary correction.
- [x] (2026-07-29 15:05Z) Carry repeatable deterministic extensions through later bridges and expand
  MultiProjection alternatives as one linear Frontier transform beneath exact-only REDUCED boundaries.
- [x] (2026-07-29 15:13Z) Prove MultiProjection duplicate suppression before carrying state: exact inputs are
  scanned for projected collisions, sampled inputs require a structurally injective mapping, and unsafe cases form
  the stable `multi_projection_duplicate_suppression_unresolved` boundary.
- [x] (2026-07-29 15:32Z) Add a theorem-safe finite binary join transform, compose arbitrary selected child states
  across hash joins, preserve mixed OPTIONAL mask strata, and pass both raw child state IDs through every root
  enumeration kernel so Frontier and LEO affect winner ranking.
- [x] (2026-07-29 15:52Z) Retain both mapped orientations and every independent lane, use direction-scoped
  exact-heavy metadata for O2S bridge probes, and report design-lane disagreement plus audit drift without adding
  those lanes to execution mass.
- [x] (2026-07-29 16:13Z) Materialize a second query-local design-lane state and use it for bounded conditionally
  unbiased Cartesian products when exact disconnected expansion exceeds the refinement budget; keep lane zero as
  the ordinary execution measure.
- [x] (2026-07-29 16:29Z) Bound sampled bridge expansion with deterministic importance-with-replacement mutation,
  retain every OPTIONAL mask stratum, use a full-support proposal epsilon of 0.1, and expose mutation provenance,
  proposal, exact-atom, residual-particle, and variance telemetry.
- [x] (2026-07-29 16:38Z) Repair LMDB reader teardown exposed by the expanded integration suite: TripleStore now
  drains active and pooled transactions before environment close, and concurrent cleaner removal cannot race native
  reset/abort against teardown.
- [x] (2026-07-29 16:41Z) Add an end-to-end sampled shape oracle combining a two-branch star, diamond closure, and
  third-star continuation; the six-factor state remains composable through two bounded mutations and estimates the
  98-row execution bag with eight particles.
- [x] (2026-07-29 16:53Z) Replay zero-support learned filters through the independent design lane, keep the learned
  cardinality target based on the primary raw lane, and carry the recovered real-particle payload through later
  projections, restrictions, bridges, and compatible unions.
- [x] (2026-07-29 17:09Z) Make query-index permutations lane-scope-first and prepare leaves in ascending candidate
  cost, so exact snapshots scan one logical copy, sampled snapshots collect all independent lanes once, and a broad
  syntax-first leaf cannot starve a later selective leaf under the materialization budget.
- [x] (2026-07-29 17:18Z) Replace the retained-input budget cliff in projected and tuple-expanding outer kernels with
  deterministic effective-weight resampling, and route correlated EXISTS/NOT EXISTS filters through that linear
  kernel instead of rejecting them before state refinement.
- [x] (2026-07-29 17:26Z) Expose lineage-aware raw, learned-filter, LEO, and final Frontier rows in optimized plans
  and audit rows, using `NaN` for learned stages that did not occur instead of conflating absence with zero mass.
- [x] (2026-07-29 17:37Z) Add audit-only Fast-AGMS and JoinSketch inner-product references with identical total
  byte budgets and seeds, and publish their numerical estimates and actual allocations for eligible binary joins
  without routing legacy strategy aliases through the unified estimator.
- [x] (2026-07-29 23:55Z) Make independent design-lane materialization demand-driven and bypass redundant scalar
  leaf/append cardinality probes for positive sampled Frontier states; the uncached eight-factor benchmark fell
  from 102.640 ms/op to 12.000 ms/op without changing the held-out q-error distribution.
- [x] (2026-07-30 00:04Z) Preserve composable evidence across enumerator-created physical joins with no source
  relation, allowing exact finite surfaces to feed a later UNION and FILTER without replacing database-exact rows
  or applying LEO under an unstable physical key.
- [x] (2026-07-30 00:04Z) Enforce the configured synopsis indexing budget up to one quarter of the live JVM maximum
  heap, and retain the lower explicit budget when the user configures one.
- [x] (2026-07-30 00:04Z) Make accuracy attribution stop at local typed boundaries instead of borrowing a descendant
  Frontier label; the honest supported-piece audit now reports p95 q-error 1.523191, worst 2.148600, and zero false
  authoritative zeros.
- [x] (2026-07-30 00:40Z) Average compatible independent design measures before sampled MINUS probes, preserving
  composability and improving the held-out q18 q-error from 2.148600 to 1.342875 and corpus worst from 2.148600 to
  2.030921 without changing p95 or producing a false authoritative zero.
- [x] (2026-07-30 01:05Z) Verify 1,029 query-evaluation tests, 57 Frontier integration tests, all 117 Theme queries,
  the held-out audit, and the full LMDB module on JDK 25; stabilize the uncached eight-factor planning benchmark at
  12.294 +/- 1.457 ms/op.
- [x] (2026-07-30 01:36Z) Replace four comparator query-index sorts with stable value radix plus a reusable lane-scope
  partition and a sparse-scope fallback. The focused million-row JMH improved from 235.043 +/- 18.825 ms/op to
  24.928 +/- 5.311 ms/op while binary output and sparse configured lanes remained correct.
- [x] (2026-07-30 01:56Z) Complete final formatted verification: 1,029 query-evaluation tests, 1,722 LMDB
  unit/integration tests, the 57-test Frontier class, and all 117 Theme queries pass on JDK 25.
- [x] Add focused failing contracts immediately before Frontier-after-LEO ordering, general learned filters,
  completion-aware observation, unified cache revision, and learned-filter state fusion.
- [x] Repair the two baseline failures without weakening their assertions.
- [x] Publish one learned-evidence planning revision.
- [x] Preserve evidence state IDs through root refinement and selected-plan contextualization.
- [x] Connect the verified composable calibration overlay to direct Frontier-to-LEO fusion.
- [x] Generalize learned filter surfaces and completion-safe runtime observations.
- [x] Add exact finite leaves and ORDER identity.
- [x] Add deterministic extension and multi-projection transforms.
- [x] Remove canonical-subset and 64-factor cliffs with on-demand interned state/factor keys.
- [x] Complete coordinated star, bounded resampling, and sampled bridge mutation work required by the accuracy gates.
- [x] Run focused, module, Theme, audit, and benchmark verification; update calibration records with measured results.
- [x] Format, inspect the complete diff, and record final outcomes and remaining research boundaries.

## Surprises & Discoveries

- Observation: the checkout starts with substantial staged and unstaged Frontier work, including a new mapped query
  index, multinomial resampler, and edits in the packed optimizer. These changes are user-owned and must be extended
  rather than replaced.
  Evidence: `git status --short` on 2026-07-29 listed eight staged additions and twenty-four unstaged tracked files.

- Observation: the mandatory root quick build is green on Azul JDK 25, so compile failures are not masking the
  estimator behavior.
  Evidence: `maven-build.log` ends with `BUILD SUCCESS` and total wall time 38.498 seconds.

- Observation: the repeated-predicate test has numerically consistent 6.2K rows and transition provenance on the
  child statement pattern, but the dirty branch had removed the packed hash alternative, its contextualization,
  materialized hint, and execution selection. Restoring that existing end-to-end path preserves independent scan
  evidence and clears the failure.
  Evidence: `initial-evidence.txt`, selector
  `LmdbIndependentFiniteAnchorJoinPlanningTest#repeatedPredicateChainUsesMultiRelationTransitionEvidence`.

- Observation: an immutable calibration can share the raw payload block safely when payload ownership and effective
  weight scale are separate primitive columns. Eviction and rematerialization must resolve the shared owner rather
  than charge or copy the overlay.
  Evidence: `FrontierPayloadStateTest` is green with 23 tests, including exact precedence, certified-bound clamping,
  duplicate-key rejection, and positive summary-only degradation for zero support.

- Observation: the current strategy names `fastagms` and `joinsketch` parse to the same `UNIFIED` implementation.
  They are not valid independent baselines. Any competitive claim needs audit-only reference implementations or
  must remain unmade.

- Observation: canonical raw factor-set slots cannot represent both raw and calibrated descendants. A calibrated
  state needs a lineage-specific derived state while retaining the raw canonical state as its counterfactual parent.
  Evidence: the prefix-plus-bridge and wrapper integration tests now keep `LEARNED_CALIBRATED` through later
  transforms without overwriting the raw state.

- Observation: a filter observation can change plan costing without changing either LMDB data or LEO's epoch. The
  former cache key therefore returned a stale hit after completed filter evidence.
  Evidence: `initial-evidence.txt` and `LmdbPackedPlanCacheTest#learnedFilterRevisionInvalidatesCachedPlan`; the
  complete cache test class is green after introducing the unified revision.

- Observation: selected-plan contextualization can apply a syntactically local filter to a larger physical prefix.
  A filter output key predeclared from only its syntax child then has the wrong factor set and frontier layout.
  Evidence: the learned-filter integration initially failed with
  `restriction must preserve frontier layout and mask strata`. Operator keys are now derived from the actual input
  state and declared lazily in the query-local arena.

- Observation: the unified learned-evidence hash is an opaque cache discriminator and can naturally set the sign
  bit, while calibration persistence requires a nonnegative revision. The public revision now masks the sign bit
  once at its source.
  Evidence: the focused filter-fusion test reached `EvidenceCalibrationSummary` only after dynamic state-key
  construction, then exposed `evidenceRevision must be nonnegative`.

- Observation: removing the one-word factor mask exposed an independent fixed-width assumption in the exact tuple
  coalescer. Its hash table already handled arbitrary tuple widths; only its constructor rejected layouts wider than
  four columns.
  Evidence: the 65-factor regression first failed with
  `IllegalArgumentException: invalid exact Frontier coalescer dimensions`, then passed after removing that stale
  statement-width guard.

- Observation: direct LEO surfaces are physical-mode specific, but non-join Frontier operators did not publish a
  physical implementation. Completed UNION observations were therefore stored under `frontier_authoritative` while
  calibration requested `frontier`.
  Evidence: `logs/mvnf/20260729-144523-verify.log` shows the exact record and miss keys; the unchanged UNION contract
  passes after all composable Frontier operators publish `plannedPhysicalImplementation=frontier`.

- Observation: LEO already persisted FILTER/projection multiplier observations but never exposed them to planning.
  A confidence-gated multiplier estimate is now available to Frontier. The filter transform applies specialized
  learned selectivity first and skips the generic multiplier when that operation already calibrated its lineage.
  Evidence: the focused multiplier and conflicting-evidence tests pass in
  `logs/mvnf/20260729-145439-verify.log` and `logs/mvnf/20260729-145543-verify.log`.

- Observation: the prior semantic-scope guard blocked every transformed state from a later bridge even when the
  exact probe depended only on retained bindings. Joined keys now preserve or mix semantic scope, preventing raw and
  transformed lineages from colliding without disabling valid continuations.
  Evidence: `LmdbFrontierPlanningIntegrationTest#deterministicExtensionCarriesComputedBridgeBinding` passes in
  `logs/mvnf/20260729-150209-verify.log`.

- Observation: MultiProjection has observable consecutive-duplicate suppression per projection lane. Treating it
  as unconditional bag union over-counted an exact collision fixture from two rows to four.
  Evidence: `logs/mvnf/20260729-151133-verify.log`; exact collision scanning and a structural sampled proof now
  preserve state only when duplicate suppression cannot change the measure.

- Observation: final selected-plan contextualization already supplied both hash-join child state IDs, but the LMDB
  wrapper ignored them and republished one partial child. Root enumeration separately passed neither child ID.
  Evidence: `logs/mvnf/20260729-151810-verify.log` reported a three-factor root as state 6 with factor count two;
  `logs/mvnf/20260729-153033-verify.log` exposed zero child IDs in `PackedJoinEnumerator.refineRootJoin`.

- Observation: a binary relation over two same-lane sampled measures is not a valid product estimator. The new
  general transform therefore joins exact/exact or sampled/exact measures and emits
  `correlated-random-product-unresolved` for same-lane random products instead of using an independence heuristic.
  Evidence: both mathematical contracts in `FrontierLinearTransformContractTest` are green, and the mixed OPTIONAL
  plus later bridge integration passes in `logs/mvnf/20260729-152924-verify.log`.

- Observation: row presence is not proof that a sampled center is complete. The old mapped view treated any retained
  S2O row as exact-heavy and had no direction-scoped completeness API, while the query index had already persisted
  the per-record exact-heavy flag.
  Evidence: `logs/mvnf/20260729-154112-verify.log`; direction-aware completeness and the query-level O2S exact-heavy
  bridge pass in `logs/mvnf/20260729-154225-verify.log` and `logs/mvnf/20260729-154646-verify.log`.

- Observation: mapped O2S, second-design, and audit payloads were all discarded by the v1 query index. Retaining
  them is safe only when every scan is scope-filtered and design lane zero remains the sole execution measure.
  Evidence: the format-v2 retention contract and audit-only telemetry are green in
  `logs/mvnf/20260729-154934-verify.log` and `logs/mvnf/20260729-155159-verify.log`.

- Observation: selected-plan contextualization represents a nested-loop Cartesian join as one direct child state
  plus one already-expanded prefix state. Choosing independent evidence from only those two handles can therefore
  multiply overlapping factor sets. The safe source is the root factor set: for the supported two-component case,
  resolve its two direct leaf measures and replace one with the corresponding second design lane.
  Evidence: `logs/mvnf/20260729-160932-verify.log` retained 512 same-lane expansion particles; the unchanged query
  passes with an eight-particle independent Cartesian state in `logs/mvnf/20260729-161200-verify.log`.

- Observation: once bridge mutation bounds an over-budget same-lane Cartesian expansion, its retained particle
  count is no longer evidence that the original expansion fit the budget. Independent-lane replacement must consult
  the `BRIDGE_MUTATION` operation provenance, while the disjoint-frontier proof prevents connected bridges from
  being mistaken for products.
  Evidence: `logs/mvnf/20260729-162444-verify.log` exposed the regression; the connected mutation and disconnected
  replacement contracts both pass in `logs/mvnf/20260729-162744-verify.log`.

- Observation: the full Frontier class completed all 52 assertions and then crashed in
  `Cleaner-3 -> TxnManager.Txn.close -> mdb_txn_reset`. TripleStore, unlike ValueStore, closed its LMDB environment
  without first closing its transaction manager; transaction removal also became visible before native free
  completed. Draining readers and making removal/free atomic fixes both the deterministic handle leak and the
  cleaner race.
  Evidence: `core/sail/lmdb/hs_err_pid14360.log`, the focused red in
  `logs/mvnf/20260729-163252-verify.log`, and the clean 52-test exit in
  `logs/mvnf/20260729-163623-verify.log`.

- Observation: a qualified learned filter can predict positive mass while the primary sampled payload contains no
  accepted tuple, even though the already-materialized second design lane contains the exact target. Scaling the
  empty primary payload would invent support, while publishing a summary immediately discards useful real support.
  The safe repair replays the raw transform in lane one, uses that payload only as support, and calibrates it to the
  primary-lane/learned blended cardinality.
  Evidence: the focused red in `logs/mvnf/20260729-164756-verify.log` published
  `learned-positive-without-supporting-particles`; the unchanged oracle is green in
  `logs/mvnf/20260729-165138-verify.log`, and the four-filter interaction selector is green in
  `logs/mvnf/20260729-165232-verify.log`.

- Observation: query-index component permutations were sorted only by component value, so even a lane-scoped query
  had to walk copies for both directions and every design/audit lane. A six-row database-exact fixture therefore
  predicted 72 physical candidates; the broad first relation consumed the entire work budget before a three-row
  selective relation was considered.
  Evidence: the focused red in `logs/mvnf/20260729-165542-verify.log`, the intermediate physical-copy failure in
  `logs/mvnf/20260729-170049-verify.log`, and the green order-invariance contract in
  `logs/mvnf/20260729-170543-verify.log`.

- Observation: the backend-neutral outer kernels already had exactly the linear semantics needed for bounded input
  resampling, but LMDB's correlated EXISTS path checked the probe budget before invoking them and then evaluated the
  condition as an ordinary restriction. Removing that precheck alone was insufficient; the filter had to select
  EXISTS or NOT EXISTS as the kernel and probe the positive subquery per sampled outer mapping.
  Evidence: the two focused transform reds in `logs/mvnf/20260729-171239-verify.log`, their green run in
  `logs/mvnf/20260729-171445-verify.log`, and the end-to-end one-probe/two-row EXISTS run in
  `logs/mvnf/20260729-171658-verify.log`.

- Observation: a single nearest-calibration record is insufficient for audit attribution after calibrated states
  pass through additional unary or binary transforms. The audit must fold filter and LEO factors separately across
  the immutable state lineage and decline stage attribution when two parents carry different calibration histories.
  Evidence: the telemetry red in `logs/mvnf/20260729-171903-verify.log`, its green run in
  `logs/mvnf/20260729-172229-verify.log`, and the audit-row red/green pair in
  `logs/mvnf/20260729-172405-verify.log` and `logs/mvnf/20260729-172522-verify.log`.

- Observation: Fast-AGMS and JoinSketch are independently meaningful for pushed-down binary inner products, while
  the existing configuration aliases are not. Multiway sketch composition needs its own proved reference transform;
  returning `NaN` is preferable to silently evaluating an exact intermediate or relabeling the unified synopsis.
  Evidence: the absent-reference red in `logs/mvnf/20260729-173003-verify.log`, the direct-reference green in
  `logs/mvnf/20260729-173306-verify.log`, and the audit-integration red/green pair in
  `logs/mvnf/20260729-173433-verify.log` and `logs/mvnf/20260729-173609-verify.log`.

- Observation: selected-plan contextualization can introduce physical join nodes whose generated logical group has
  no source query relation. Those nodes retained scalar rows but could not call the ordinary relation refinement
  hook, so an exact finite-surface branch lost its query-local state and made the enclosing UNION degrade.
  Evidence: `logs/mvnf/20260729-234950-verify.log` reported `union_child_state_unavailable`; the unchanged q16
  filter contract passes in `logs/mvnf/20260729-235405-verify.log` after adding a raw-state-only intermediate-join
  refinement hook.

- Observation: the audit harness recursively borrowed the first specific descendant source for every non-full
  algebra piece. This attributed GROUP's one-row aggregate estimate to a two-row Frontier leaf and also skipped
  across GROUP from an enclosing projection or extension.
  Evidence: focused failures in `logs/mvnf/20260729-235909-verify.log` and
  `logs/mvnf/20260730-000221-verify.log`; the direct and nested boundary contract passes in
  `logs/mvnf/20260730-000331-verify.log`. The corrected 30-query audit in
  `logs/mvnf/20260730-000419-verify.log` reports 236 honestly attributed Frontier pieces, p95 q-error 1.523191,
  worst q-error 2.148600, and zero false authoritative zeros.

- Observation: exact MINUS evaluation over a sampled primary state can retain no removable particle even when an
  independent design lane observes the removed stratum. Averaging the two compatible measures before the exact
  boolean kernel reduces this variance without inventing support or recursively training on corrected rows.
  Evidence: `logs/mvnf/20260730-003921-verify.log` and `logs/mvnf/20260730-004013-verify.log`; q18 improved from
  q-error 2.148600 to 1.342875 and the held-out worst improved to 2.030921.

- Observation: a full LMDB run spent sustained CPU in the four comparator sorts that publish mapped query-index
  permutations. End-to-end Theme loading hid the change behind LMDB ingestion, but an isolated million-row JMH
  showed 235.043 ms/op for comparator quicksort, 44.475 ms/op for two-key stable radix, and 24.928 ms/op when one
  scope partition is reused after a stable value radix pass.
  Evidence: `FrontierQueryIndexSortBenchmark`, `logs/mvnf/20260730-013127-verify.log`, and
  `logs/mvnf/20260730-013239-verify.log`.

## Decision Log

- Decision: use Routine D with test-first behavior slices.
  Rationale: the work crosses query evaluation, packed planning, LMDB persistence, learning, and statistical state
  semantics. An ExecPlan is required, while the repository's proportional test-first rule still requires a focused
  failing contract before each behavior change.
  Date/Author: 2026-07-29 / Codex

- Decision: learned correction is a composable mass calibration, not an unbiased estimator.
  Rationale: uniformly scaling a Frontier measure lets the repair influence later bridges, which point-only
  correction cannot do. Learning changes the statistical guarantee, so the state must say so explicitly.
  Date/Author: 2026-07-29 / Håvard and Codex

- Decision: automatic safe correction is the default.
  Rationale: the feature must be used without hidden system properties. Corrections still require support and
  confidence gates and retain OFF, observe-only, and shadow profiles.
  Date/Author: 2026-07-29 / Håvard and Codex

- Decision: keep the planning engine interpreted with primitive structure-of-arrays state.
  Rationale: each query is planned once, so runtime compilation cost would dominate. The important asymptotic change
  is lazy state creation rather than code generation.
  Date/Author: 2026-07-29 / Codex

- Decision: an alternate design lane may supply particle support but not the learned target cardinality.
  Rationale: lane one is an independent sample, not execution truth. The raw primary pass ratio and learned surface
  determine final rows; replayed lane-one tuples merely make that positive estimate composable without fabricating
  bindings.
  Date/Author: 2026-07-29 / Codex

- Decision: only completed execution observations train correction targets.
  Rationale: a LIMIT, early close, abort, SERVICE call, or partial subquery can produce selection-biased counts.
  Audit lanes may measure drift and coverage but cannot pretend to be actual runtime rows.
  Date/Author: 2026-07-29 / Codex

- Decision: sort each mapped query-index permutation by direction, lane role, lane index, component value, and row
  ID, and budget preparation against only the scopes it actually consumes.
  Rationale: scope-first ranges make lane selection logarithmic, eliminate unrelated physical copies from the work
  estimate, and let one combined leaf scan emit design lanes while accumulating audit diagnostics. An exact view
  scans design lane zero only; a sampled view still includes every independent lane, including exact additive
  insert generations.
  Date/Author: 2026-07-29 / Codex

- Decision: sample over-budget outer inputs proportional to their effective particle weight, give each draw
  `totalMass / drawCount`, and then run the deterministic exact RHS kernel.
  Rationale: EXISTS, NOT EXISTS, MINUS, and OPTIONAL transforms are linear in the outer measure. This preserves
  conditional unbiasedness, consumes calibration scales correctly, changes exact inputs to sampled evidence
  explicitly, and keeps a zero draw budget as a typed boundary.
  Date/Author: 2026-07-29 / Codex

- Decision: report learned evidence as four canonical cardinality stages and use `NaN` for an inapplicable learned
  stage in audit rows.
  Rationale: zero is a valid cardinality and must never mean “this stage did not run”; separate filter and LEO
  lineage factors also make ordering and double-correction failures observable after downstream transforms.
  Date/Author: 2026-07-29 / Codex

- Decision: keep independent sketch references audit-only and report unsupported multiway composition explicitly.
  Rationale: a numerical baseline is valid only when it follows the published estimator. Exact intermediate
  execution or the production `UNIFIED` alias would make a larger-query comparison look complete while invalidating
  it statistically.
  Date/Author: 2026-07-29 / Codex

- Decision: generated physical joins may compose raw child evidence but may not apply learned correction.
  Rationale: the physical node has no stable source-algebra key suitable for LEO training. Retaining its raw state
  restores downstream composability, while the next source-backed JOIN, UNION, or FILTER remains the sole place
  that applies a learned correction.
  Date/Author: 2026-07-30 / Codex

- Decision: accuracy attribution follows transparent wrappers only until the first semantic operator.
  Rationale: projection, deterministic extension, and order can inherit their direct input's estimate source, but
  skipping across GROUP, FILTER, JOIN, SLICE, or another boundary would score rows Frontier did not estimate.
  Date/Author: 2026-07-30 / Codex

- Decision: compatible independent design measures may be averaged as a first-class state operation before a
  linear boolean kernel.
  Rationale: retaining both payloads at half effective weight preserves measure unbiasedness and downstream
  support, while treating lane one as execution truth or using it only after observing the primary miss would bias
  the estimate.
  Date/Author: 2026-07-30 / Codex

- Decision: publish query-index permutations with stable value radix followed by a shared scope partition.
  Rationale: stable value ordering preserves the row-ID tiebreaker, stable scope distribution preserves value
  ordering within each direction/role/lane, and the sparse fallback bounds workspace memory for unusual configured
  lane indexes. The isolated measured speedup is 9.43x over the prior comparator.
  Date/Author: 2026-07-30 / Codex

## Outcomes & Retrospective

The implemented outcome includes a green JDK 25 quick reactor build; both known branch failures repaired; default
safe LEO/service lifecycle enabled without the legacy sketch; root Frontier state propagation; direct
raw-Frontier-to-LEO calibration at roots and prefixes; propagation through later bridges and linear wrappers; and
unified cache invalidation for snapshot, LEO, learned-filter, and Frontier availability changes. Completed
deterministic filter observations feed persisted topology surfaces, and sampled Frontier prefixes apply
confidence-blended learned-filter calibration after particle restriction. Shape coverage, factor-set scalability,
exact finite binary composition with nullable masks, root child-state propagation, direction-aware O2S probing,
independent-lane diagnostics, independent sampled Cartesian products, and bounded bridge mutation are implemented.
LMDB teardown reliably drains Frontier-planning readers before native environment close. Sampled fork, diamond,
cycle, and three-star continuation coverage is green. Optimized plans and audit rows report raw Frontier,
post-filter, post-LEO, and final rows without assigning learned-stage values to ambiguous two-parent lineages.
Independent Fast-AGMS and JoinSketch references cover eligible pushed-down binary joins; a proved multiway sketch
reference and the plan-regret/interval-coverage promotion measurements remain. Demand-driven independent lanes plus
scalar-probe bypass reduced the focused uncached eight-factor planning benchmark from 102.640 ms/op to a stabilized
12.294 +/- 1.457 ms/op. Independent-lane averaging reduced the held-out corpus worst q-error to 2.030921 while p95
remains 1.523191, with zero false authoritative zeros, preparation p95 0.158 ms, preparation maximum 0.169 ms, and
377,896 bytes peak query-local memory. Reusing a primitive scope partition reduced the million-row query-index
ordering benchmark from 235.043 to 24.928 ms/op without changing persisted ordering.

## Context and Orientation

The relevant modules are `core/queryalgebra/evaluation` and `core/sail/lmdb`. The query-algebra module owns
backend-neutral packed planning and query-local evidence types. `PackedCostEstimate.evidenceStateId` is an integer
handle valid only for one planning session. `FrontierStateArena` stores immutable weighted tuple states in primitive
parallel arrays. `EvidenceStateSummary` is the scalar audit view of one such state.

The LMDB module owns the persistent Frontier synopsis, exact store probes, runtime learned statistics, and the
`LmdbFrontierPackedCostSession` wrapper. A Frontier state contains exact LMDB term IDs and nonnegative particle
weights. A bridge is a statement pattern that moves the retained tuple frontier from one subject-centered star to
another. LEO means RDF4J's runtime operator-feedback learner. It records completed operator rows and work, then
returns a confidence-blended correction for a later matching operator. A learned filter surface is the combination
of a filter condition, the topology of its input, and its binding/nullability shape.

Today `LmdbSailStore` constructs `LmdbFilterSelectivityStats` and `LmdbOperatorFeedbackStats` only when the legacy
`SketchBasedJoinEstimator` exists. The default disables that legacy estimator while enabling authoritative Frontier.
`LmdbPackedCostModel` may apply LEO first, but `LmdbFrontierPackedCostSession` then installs raw Frontier rows.
`LmdbEvaluationStatistics.estimateFilterPass` and `recordFilterOutcome` require the filter input to reduce to a
single statement pattern. `PackedJoinEnumerator.refineRootJoin` resets `PackedCostContext` without the winning
evidence state. `PackedSelectedPlanContextualizer` also clears evidence at several binary wrappers.

The current Frontier query preparation predeclares at most 4,096 canonical subsets when there are at most twenty
supported relations and uses a compact `long` relation mask. The persisted `FrontierStateKey` already supports a
`long[]`; therefore a query-local factor-set interner can remove this artificial limit without putting boxed sets in
the cost loop.

## Plan of Work

First add focused red contracts in the smallest owning module. Query-algebra tests will specify the new guarantee,
calibration metadata, arena overlay, root state propagation, and completion-aware filter observation API. LMDB tests
will specify default service construction, correction ordering, exact-state immunity, filter topology lookup,
cache invalidation, and a correction that survives an additional bridge. Preserve all failing Surefire reports
before production edits.

Repair the current baseline reds next. Restore SPARSE in `ThemeQueryBenchmark`'s default parameter list. Trace the
repeated-predicate failure through `PackedJoinEnumerator` and `PackedSelectedPlanContextualizer`; preserve the winning
transition state/provenance without mislabeling an independently scanned input.

Introduce `EvidenceGuarantee.LEARNED_CALIBRATED`, `FrontierStateOperation.CALIBRATE`, and an experimental immutable
`EvidenceCalibrationSummary`. The guarantee is composable only for positive supported mass. It cannot claim an
exact or statistical interval and cannot assert an exact or estimated zero. Store calibration metadata and a
cumulative primitive weight scale beside each state. A calibrated state references the same payload as its parent;
downstream cursors multiply by the cumulative scale. Materialization and resampling fold the scale once.

Add a package-private `LearnedCardinalityCorrection` to the LMDB runtime. It carries raw and corrected rows/work,
factor, source, evidence count, correction confidence, key, revision, uncertainty, and the application decision.
Centralize qualification: adaptive evidence must be allowed, the rollout profile must permit correction, direct
evidence must have at least three observations, confidence must be at least 0.55, generalized evidence must contain
at least two distinct signatures, and the state must not be database-exact. Retain existing finite ratio clamps and
clamp to a certified upper bound. A correction from zero to positive triggers bounded alternate-lane/refinement
replay; without retained support it becomes summary-only with a stable reason.

Build a canonical LEO surface key from the sorted factor signatures, variable-incidence graph, operator kind,
binding/nullability shape, predicate/context constants, and physical mode. It must be independent of join-tree order
and the larger parent query. Record both exact and predicate/context-generalized forms while continuing to read
legacy persisted keys. Runtime observations compare actual rows to raw Frontier rows; final corrected rows are used
only for safety and q-error telemetry.

Decouple service construction in `LmdbSailStore`. Default authoritative Frontier constructs filter and feedback
services even when the legacy sketch is null. Explicit Frontier OFF plus LEO OFF constructs none. Publish one
`learnedEvidenceRevision()` from `LmdbEstimatorRuntime` that mixes Frontier snapshot, LEO planning, and filter
planning revisions. Use it in the packed plan-cache context so a newly eligible correction cannot reuse a stale
recipe.

Generalize filter learning. Add a persisted `FilterSurfaceKey` with exact and template variants and keep legacy
single-pattern maps as fallback. Bump the sidecar format and read the previous version. Add
`FilterOutcomeObservation(passedCount, filteredCount, completed, poisonReason)` to `EvaluationStatistics` while
retaining the old overload for binary compatibility. Every filter iterator records only after normal input
exhaustion. Deterministic subquery filters may train; partial, sliced, aborted, SERVICE, and nondeterministic
observations do not.

In Frontier filter refinement, evaluate the predicate on retained particles first. Exact input stays exact. For
sampled positive support, obtain the learned pass ratio and compute
`finalPass = (1 - correctionConfidence) * rawPass + correctionConfidence * learnedPass`. Apply the ratio between
final and raw mass as a calibration overlay. The specialized learned-filter result takes precedence over LEO's
generic unary multiplier. Extend required-binding analysis so later filters, bridge endpoints, cycles, OPTIONAL
continuations, EXISTS, NOT EXISTS, and MINUS retain needed values through projections.

Replace whole-query property-path rejection with a centralized operator capability policy. Statement patterns,
VALUES, singleton/empty sets, deterministic filters, joins, disconnected products, projections, deterministic
extensions, multi-projection, UNION, OPTIONAL, MINUS, EXISTS/NOT EXISTS, QueryRoot, and ORDER carry state.
DISTINCT, REDUCED, GROUP/aggregates, SLICE, and INTERSECTION transform only database-exact affordable states or form
a local typed boundary. Arbitrary-length paths, SERVICE, tuple functions, external sets, and nondeterministic
expressions are opaque boundaries. Fixed property paths that lower to joins remain supported. Descendant Frontier
states remain visible below every boundary.

Replace canonical subset preallocation with an on-demand `FrontierFactorSetArena`. It interns immutable factor
bitsets during cold state creation and exposes primitive integer IDs in the packed cost loop. Query preparation is
O(F + V + E + S * ceil(F/64)), where F is factor count, V variables, E variable incidences, and S states actually
visited, instead of powerset predeclaration. Memory remains charged to the existing 64 MiB query budget.

Complete the statistical composition needed by large shapes. Use mapped pathwise-exact adjacency whenever the
retained center is complete, consume subject-to-object and object-to-subject lanes, preserve per-record exact-heavy
mass, and use the existing multinomial resampler when an affordable exact expansion exceeds the payload cap.
Exact bridge expansion remains first choice. Beyond the deterministic work budget, use importance-weighted
with-replacement RHS draws with proposal
`q = 0.9 * informedProposal + 0.1 * uniformFullSupport`. Never substitute draw probabilities for without-replacement
inclusion probabilities and never multiply same-lane sampled measures. Use independent lanes or an explicit
`correlated-random-product-unresolved` boundary for a disconnected sampled product.

Finally extend the audit harness to report raw Frontier, learned-filter, LEO, and final rows. Add audit-only faithful
Fast-AGMS and JoinSketch references under the same memory budget and seed; do not use the existing alias parser as
evidence. Train on completed queries and evaluate held-out constants and larger supergraphs. Update the calibration
record only with measured results.

## Concrete Steps

Run all commands from the repository root.

The initial installation command is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

For each behavior slice, run the smallest selector first:

    python3 .codex/skills/mvnf/scripts/mvnf.py <ClassName>#<methodName> \
      --module <module> --retain-logs

After focused greens, run the owning classes and then:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run the bounded calibration and Theme coverage selectors defined by
`LmdbEstimateAuditHarnessTest` and the generated Theme coverage test. Benchmark the focused multi-bridge method:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbFrontierPlanningBenchmark \
      --method estimateMultiBridge

Run representative whole-query planning/execution:

    scripts/run-single-benchmark.sh --theme-plan-run --theme-query PHARMA:11
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query SPARSE:12

Before final verification, check source headers, run the repository formatter, and rerun the two modules. Tests must
never use Maven `-am` or `-q`.

## Validation and Acceptance

The new default-lifecycle test must open a default LMDB store with the legacy sketch disabled and show non-null
filter and feedback services. Explicit Frontier OFF plus LEO OFF must show both absent.

The root-state test must pass a nonzero state ID into root join refinement and observe the same winning state or a
derived calibrated state afterward. Cached recipes must contain no query-local state ID.

The correction integration test must train at least four completed observations, plan an unseen query containing
the learned subgraph plus another bridge, and report `lmdb-frontier+leo` or
`lmdb-frontier+learned-filter`. Its downstream bridge rows must differ from raw Frontier in the correction
direction. Exact-state variants must remain unchanged.

Filter tests must cover a multi-relation child, template generalization across constants, OPTIONAL nullable masks,
a required variable that would otherwise be projected away, sampled zero with a positive Wilson upper bound,
restart persistence, and rejection of partial or nondeterministic observations.

Shape tests must cover two- and three-star chains and forks, reverse O2S, a diamond, exact cycle closure,
disconnected products, mixed OPTIONAL/UNION/EXISTS/NOT EXISTS/MINUS, VALUES, more than sixty-four factors, and an
arbitrary path beside a supported local subtree.

All 117 Theme queries must preserve semantic results. Every audited algebra piece must have a composable state or a
stable nonblank degradation reason. Promotion requires zero false authoritative zeros; p95 q-error below 5 for
supported acyclic shapes through eight edges; p95 plan regret below 2x and no plan above 10x the bounded oracle;
approximately 92--98 percent empirical coverage for intervals labeled 95 percent; preparation p95 at most 5 ms and
maximum at most 10 ms; query memory at most 64 MiB; cached insertion maintenance below 1 ms p95; and complete green
query-evaluation and LMDB modules on JDK 25. A superiority claim requires the corrected estimator to meet the
accuracy gate on a held-out corpus where both independent baselines fail it.

## Idempotence and Recovery

All tests and builds are repeatable. Preserve untracked artifacts and existing dirty changes. Do not reset, restore,
clean, or stash the worktree. New sidecar readers must accept the previous format, and a failed migration must leave
the old sidecar untouched and rebuild derived state safely. Query-local arenas and index leases close on all
success, fallback, and exception paths. OFF and zero-budget configurations allocate no Frontier or learned state.

If a focused test exposes a conflict with pre-existing work, inspect the three-way intent using the current diff and
record the discovery here before changing course. If a statistical transform cannot meet its exact expectation
oracle, keep the old exact path and typed fallback rather than weakening the guarantee.

## Artifacts and Notes

The initial evidence is in `initial-evidence.txt`. Relevant logs are:

    maven-build.log
    logs/mvnf/20260729-122204-verify.log
    logs/mvnf/20260729-122321-verify.log
    logs/mvnf/20260730-003740-verify.log
    logs/mvnf/20260730-004013-verify.log
    logs/mvnf/20260730-004457-verify.log
    logs/mvnf/20260730-004630-verify.log
    logs/mvnf/20260730-005615-verify.log
    logs/mvnf/20260730-005653-verify.log
    logs/mvnf/20260730-013239-verify.log
    logs/mvnf/20260730-013408-verify.log
    logs/mvnf/20260730-013534-verify.log
    logs/mvnf/20260730-013815-verify.log

The active JDK is Azul JDK 25. Focused JMH covers uncached eight-factor planning and query-index ordering. The
remaining performance uncertainty is whole-query plan regret, interval coverage, and cached insert-maintenance p95
under the complete promotion corpus.

## Interfaces and Dependencies

In `core/queryalgebra/evaluation`, add:

    EvidenceGuarantee.LEARNED_CALIBRATED
    FrontierStateOperation.CALIBRATE
    record EvidenceCalibrationSummary(
        double rawRows,
        double finalRows,
        double factor,
        String source,
        long evidenceCount,
        double correctionConfidence,
        String evidenceKey,
        long evidenceRevision)

`FrontierStateArena` must expose calibration metadata for a state and create a calibrated overlay without copying
the payload. `EvidenceGuarantee.isComposablePointEstimate()` includes positive learned-calibrated states.

`EvaluationStatistics` must retain its current `recordFilterOutcome(Filter,long,long)` method and add a
completion-aware overload using an immutable observation type. Old subclasses continue to work.

In `core/sail/lmdb`, add package-private immutable correction and filter-surface types. `LmdbEstimatorRuntime`
provides qualified LEO and learned-filter correction methods plus `learnedEvidenceRevision()`.
`LmdbFrontierPackedCostSession` is the only component that applies learned correction to Frontier state. The scalar
cost model may still use learning when Frontier is unavailable, but it must not apply the same correction twice.

No new production dependency is required. Test-only Fast-AGMS and JoinSketch references use JDK arrays and seeded
hash functions. The execution model remains interpreted and primitive-array based on JDK 25.

Revision note (2026-07-29): created this self-contained ExecPlan from the accepted implementation plan and the
verified state of the dirty branch. It records the initial build and failing-test evidence so work can resume from
this file alone.

Revision note (2026-07-29 12:32Z): recorded the first four test-first integration contracts and split the remaining
contract work by behavior slice so each production milestone retains a focused pre-change failure.
