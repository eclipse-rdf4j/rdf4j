> Historical implementation roadmap. Descriptions of retired synopsis, query-index, materialization, and migration paths below are historical evidence, not supported configuration or current behavior. See [the current LMDB estimation guide](../../core/sail/lmdb/README.md) for the surviving V2, quad, and cold-filter systems.

> Active implementation (updated 2026-11-07): [unify Cascades, native strategies, and LMDB/adjacency physical costing](#2026-11-06-unified-physical-costing-implementation-plan). Upstream main is merged at `2007a9fdbb83eed79047d4cec0696c49ec1c20f8`; the user's uncommitted implementation and this plan are restored. Native-local selector improvements are reusable inputs to the roadmap, not completion of its shared planning/execution contracts. Earlier measurements remain historical evidence, not validation of the merged implementation.

# Build the complete Frontier and Cascades query optimizer

This is the only optimizer ExecPlan. It is a living document and must be maintained according to `.agent/PLANS.md`.
Do not create a child, follow-on, rollout, closeout, benchmark, or component ExecPlan. Record every implementation
slice, discovery, decision, result, and next action here so a contributor can resume the complete program from this
file alone. Research notes, audit reports, benchmark output, and generated catalogs may support this plan, but they
are evidence rather than competing instructions.

## Purpose / Big Picture

The optimizer must make decisions from the richest evidence it has. A Frontier state is not merely a row-count
producer: it can retain weighted tuples, exact finite relations, correlated term identities, multiplicity,
distribution sketches, confidence bounds, provenance, and learned calibration. Collapsing that state to one scalar
inside a join transition destroys information needed by later joins, risk-aware ranking, cache validation, runtime
feedback, and safe reasoning about zeros and ranges.

After this roadmap is complete, RDF4J will encode a query once into a compact typed representation, saturate all
semantically legal rewrites, enumerate legal bushy join partitions with DPhyp inside one Cascades memo, and retain
every nondominated continuation needed by later operators. Cardinality evidence and physical cost remain distinct.
The planner carries Frontier state through candidate generation and costing, compares a Pareto set of physical cost
vectors, and converts to a scalar preference only when a declared policy selects the final plan. Execution feedback
updates scoped logical estimates and physical residuals using a LEO-derived but stricter validated lifecycle.

An exact-complete explanation certifies that rewrite, fact, hypergraph, DPhyp, Cascades, and dependent-parent work
reached a fixpoint. Because exact continuation and Pareto sets can grow exponentially, a bounded request may instead
return an explicitly incomplete status and its best complete executable incumbent. It never silently caps a frontier,
claims exactness, or writes an exact cache entry after a work, deadline, resource, or unsupported-semantics stop.

Stored predicate ranges and other proven facts travel with the same plan state. They may prove a rewrite legal or
impossible; statistical estimates may rank alternatives but never authorize a semantic rewrite. A complete catalog
links every rewrite and physical optimization to its preconditions, proof, implementation, tests, and negative
cases.

The observable result is a planner whose explanations can answer: which alternatives were legal, which evidence
state priced each candidate, which alternatives survived Pareto dominance, which learned observation changed a
dimension, which range fact enabled a rewrite, and why the selected plan won.

The end-to-end proof is the complete current 182-cell Theme catalog plus exhaustive and generated small-query oracles. Every
query must retain authoritative SPARQL result-bag semantics, every rewrite must expose its proof and rejection reason,
and every selected physical plan must be traceable to the Frontier state and multidimensional costs used during
search. Planning and execution measurements must use matched stores, JVMs, evidence modes, and fixed-plan controls;
performance may trigger an explicit incomplete status but never disguises candidate suppression or excuses an unsafe
rewrite.

## Progress

- [x] (2026-11-06) Inspect current physical costing boundaries and design the unified implementation phases.
  The dated section below is the active plan for this scope; historical unchecked milestones remain obligations
  where applicable, rather than instructions to restore retired systems.
- [x] (2026-11-06) Finish the dated implementation plan and inspect its document diff. At that checkpoint its phases
  had not started; subsequent implementation and validation are recorded below.
- [ ] **Unified phase 0, partially qualified:** the pre-merge cold-planning benchmark contains a snapshot-scope
  fallback and is not a valid performance baseline. The admitted-view lifetime and affected restored work have focused
  post-merge qualification; a frozen forward source, empirical shared-host A/A, catalog gap inventory and real
  integration reds now exist. Full catalog and full-scale outcomes, qualified timing controls, cached replay, and the
  selected-to-executed native handoff remain open; phase 0 is not accepted as complete. Its unresolved gates carry
  forward to the final campaign while the behavior-preserving phase 1 contract adapter begins.
- [x] (2026-11-07) Merge upstream `855a77e816602007a0dec9af6a811d00af44acf1` into
  `d69e9dd27a7411830ab497aff728301b4889211f` as `2007a9fdbb83eed79047d4cec0696c49ec1c20f8`.
  Git reapplied its autostash; all 5,288 original untracked files and 38 non-overlapping tracked modifications match
  their preserved SHA-256 values, and the two overlapping LMDB files retain the same local patch modulo line offsets.
  The index is clean. The independent backup and safety ref remain under
  `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/` and `refs/codex/merge-safety/upstream-20261106`.
  On the exact committed merge tree, focused ownership/cancellation suites passed 165 and 25 cases, the combined
  selector/quote/pipeline suite passed 98, and affected model binding/tuple tests passed 31. A prior run of the same
  98-case selection had one unreproduced DISTINCT quote-comparison failure; preserve its report and investigate it
  rather than attributing a cause or declaring it fixed. The clean pre-merge LMDB pipeline suite independently had
  the same three plan-shape failures as the merge candidate. Reports and diagnostics are in the backup's
  `retry-regressions/` directory and `initial-evidence.upstream-merge-20261106.txt`.
- [x] (2026-11-07) Audit the reapplied working tree before any further edits: all original untracked artifacts and
  non-overlapping tracked files match the preservation manifest; both overlapping local patches match the saved
  autostash changes after ignoring hunk positions and blob IDs. The safety ref and independent backup are retained.
- [ ] Reconcile the restored-source LMDB qualification failures before resuming implementation. The nine-class
  packed evaluation selection passes 176/176. The six-class LMDB selection runs 80 tests with 6 failures and 8
  errors: named-index, range, and provider-admission classes pass 25/25; the admitted-view no-op publication
  regression passes all three explicit/inferred/combined cases, while one packed-search population assertion,
  two real read-ownership cases, and eleven repeated-variable cases fail. The reports and exact selectors are in
  `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/validated-live-source/` and
  `initial-evidence.upstream-merged-wip.txt`. A preserved pre-merge-WIP reconstruction is being tested against the
  same selection. All 80 test identities and all 14 failure/error identities and messages match the merged work;
  only pipeline stack line offsets differ in three cases. These failures therefore predate the merge. Two test
  fixtures have since been updated to model admitted-view and same-session VALUES evidence, without weakening their
  existing assertions; repeated-variable coverage now passes 14/14. The finite-VALUES case reaches a distinct
  inclusive-plane discrepancy (expected 3, observed 4). Its prefix and append contexts have different assured-input
  frames, so their population proof correctly refuses composition and the surviving four-row estimate is isolated,
  not a contextual result. A preserved same-frame diagnostic passes 1/1: mapped finite-domain evidence produces the
  expected two and three contextual rows and visits for explicit and combined planes, while the mismatched frame
  remains refused. The correction and refusal control are in the live test and pass; the diagnostic evidence is in
  `validated-live-source/55-finite-same-frame-diagnostic/`.
  A real-store alias diagnostic confirms that a predicate-only prefix may retain a nonzero candidate population while
  actual work stays unknown, whereas a context-leading alias remains unavailable; the permanent regression checks
  both index shapes and passes. Its reports are in `validated-live-source/51-live-alias-test-green/`.
  A publication trace confirms a separate admitted-view defect: the explicit mapped leaf remains at two rows while
  a delegated projection uses a now-unqualified fresh scope and changes the root estimate from two to three. Preserve
  the original assertions and fix the reader/plane capability through decorated costing before closing phase 0.
  The in-repository `LmdbMappedPlanningPublicationRaceTest#admittedDecoratedFactorsKeepTheirPlaneAcrossPublication`
  fails all three reader-plane cases across projection, extension, filter, and nested wrappers (112 distinct failed
  assertions). For example, explicit projection changes from two rows/eight work to three rows/eleven work; an
  inclusive filter also changes provenance. The red report is preserved at
  `validated-live-source/52-publication-decorated-plane-red/` and in `initial-evidence.upstream-merged-wip.txt`.
  Separate pre-fix controls for nested, foreign, closed and escaped session ownership, and for a committed write
  invalidating the admitted view, pass 1/1 each in `validated-live-source/53-*` and `54-*` respectively. The
  intended correction is a session-owned borrowed view and reader capability threaded through delegated estimates,
  without relaxing fresh admission, shared evidence, finite-scan budgets, or invalidation.
  Its optional query-scope estimator caches can retain variable-size keys, finite relations, evidence profiles and
  opaque sketches. A fixed per-entry reservation cannot bound those graphs. Until phase 1 supplies defensible
  retained-payload accounting, admitted-session estimates must not be inserted into those optional scope caches;
  call-local memoization remains, with no claim that existing temporary computation is fully memory-accounted.
  An initial capability patch made the original three-plane matrix pass 3/3 in
  `validated-live-source/56-capability-patch-frozen-matrix/`; it was then revised to avoid unaccounted retention.
  Before the no-retention revision, the in-repository
  `LmdbMappedPlanningPublicationRaceTest#admittedDelegatedFactorDoesNotRetainUnbudgetedQueryResults` test failed
  1/1: two delegated calls charged only one mapped probe instead of two because a query-scope result was retained.
  Its report is in `validated-live-source/57-retention-hit-proof-red/`. Additional direct tests cover pre-probe work
  refusal, foreign ownership, nonempty delegated prefix context, and isolation from scalar scope-cache entries.
  The reviewed no-retention candidate and these test additions are preserved as
  `publication-capability-no-retention-review.patch` (SHA-256 `ab23b73c7243717e6f272b1ff1a1a0daf1910e57ca91e05215327cd5f98a5f07`)
  in the merge backup. The preceding retention regression now passes 1/1 in
  `validated-live-source/58-no-retention-fix-focused/run-01-retention-green/`; five direct controls pass 7/7
  including all three prefix planes in `run-02-budget-owner-prefix-cache-binding-controls/`. They exercise pre-probe
  refusal, foreign/closed owners, connected prefixes, scalar-cache isolation and finite-binding retention. The
  original publication matrix passes 3/3 and the corrected finite-VALUES reader-plane case passes 1/1 in `run-03-*`
  and `run-04-*` respectively. Scoped Spotless formatting and check pass for exactly the 15 touched Java files;
  none of the 12 pre-existing repository-wide header findings concerns those files. The formatted source snapshot
  SHA-256 is `1c45631b79540829b93ffa296b53438b0bb5383e01a6e3a672a69b4d755a9b0e`, under
  `run-05-targeted-live-formatting/final-formatted-source-snapshot/`. The original six restored-WIP LMDB classes now
  pass 93/93 on that formatted source. Eight additional related estimator classes bring the run to 241 tests, with
  13 failures and 24 errors concentrated in four classes; the reports are in `run-06-broader-restored-wip/` and
  the 97 matching identities from those four classes were checked against preserved pre-capability WIP in `run-07-*`.
  The V2 suite had the same 23 null-owner errors in both trees; its mocks did not supply the current admitted-read
  contract. Its migrated ownership-aware fixture preserves the assertions and now passes 30/30 in `run-09-*`.
  Both source-size architecture assertions already fail
  at merged HEAD and pre-merge d69: engine 701/700 and evaluation facade 577/500; the capability adds five engine
  lines. Do not raise the limits. One publication-admission case passed before the capability and failed afterward,
  while three others changed from failing to passing. A single run cannot classify that drift; compare the exact
  selector and class on immutable source snapshots before attributing cause. That target passes both standalone and
  within its class on each preserved snapshot; the one broader-run failure timed out waiting for the initial native
  guard before the borrowed-admission assertions and captured no child stack. The direct guard code and test hashes
  match. This is unresolved evidence, not proof of flakiness or of a capability regression; see `run-08-*`.
- [ ] Complete the forward-baseline prerequisites before any timed comparison. The current uncached planning
  benchmark checks plan shape only during trial setup and combines preparation stages, so it cannot prove that every
  measured invocation followed the intended path. Add validated invocation-level selection, execution, completion
  and fallback receipts; separate planning, selected preparation, fixed-plan execution and end-to-end lanes; and
  provide the independent small-plan oracle and deterministic fixtures specified below. Enumerate the complete
  current Theme catalog (14 themes by 13 query indices) and record unsupported, missing and timed-out cells. Verify
  exact or isomorphic result bags with an independent oracle; a probabilistic fingerprint alone is not equality.
  Current coverage is narrower: the Theme planning benchmark defaults to 7 themes by 11 indices; the existing Jena
  oracle defaults to 145 cells and validates counts; the bounded exact-bag LMDB comparison supplies fixtures for
  nine themes. The catalog includes nondeterministic SAMPLE/tie-sensitive cases and cells with billions or trillions
  of full-size rows, so choose deterministic bounded inputs or explicitly validate permitted results rather than
  pretending hashes or impractical enumeration prove all full-size bags. A proposed first independent core oracle
  enumerates a small connected join's proven legal orientations, scores literal typed-work vectors under independent
  synthetic prices, and checks exact duplicate-preserving bags; it is a characterization proposal, not yet a red or
  evidence of native recipe integration. Its first two-price run has one pass and one failing estimate assertion.
  The same-invocation diagnostic quotes both dependent orders at 33 and 26 for EXPENSIVE_SEEKS, but the exact-complete
  selected original join reports cost 6 and estimated rows 1; both independently materialized orientations had
  already produced the correct two-row duplicate bag. The selected bag was not reached in that case. Determine
  whether the fixture omitted a separately valid root logical event or the planner skipped a required physical quote
  before proposing production changes. Source tracing identifies a physically costed written-root incumbent with
  isolated child winners; the fixture omitted that valid costing route and left its independently known join rows
  unrefined. Revise the test-only independent model to enumerate that route and both contextual routes, distinguish
  route from final orientation, and preserve exact bags and both price schedules. No production bug follows from the
  underspecified fixture. Evidence is in `run-37-oracle-same-invocation-complete-source/`. The corrected test-only
  characterization now passes both price schedules 2/2 in `run-39-three-route-oracle/`; it is limited to the current
  admitted generic dependent-join space and makes no native-recipe or global physical-optimality claim. Its scoped
  formatting/check passes; final test SHA-256 `b7e61310d45c85f45b89a25f5a1439a05b7553f177eaca58159335dd66a13448`
  and the unchanged-source proof are in `run-41-oracle-exact-format/`.
  Freeze a complete source/input manifest and isolated dependency caches, pin and record the container/JDK, data and
  flags, and use isolated sources for every benchmark run. Docker repository isolation was verified with the pinned
  image: the inner helper's CLI `.m2_repo` overrides the wrapper's `MAVEN_OPTS` `.m2_repo_linux_j26`; therefore use a
  private full source copy with its own copied `.m2_repo`, never the live checkout. Dry-run evidence is in
  `validated-live-source/run-22-phase0-maven-docker-repo-isolation/`. Establish repeated
  matched baseline-versus-baseline variability and fix lane tolerances before candidate comparisons; collect JFR
  separately from ordinary matched timing. Preserve all earlier invalid benchmark artifacts as diagnostics.
  The first implementation slice is a test-first audit of reused-root Cascades outcome metrics: exercise real
  success, opaque-node fallback and success again on the same root, including pipeline-cache reuse. Preserve unrelated
  semantic and physical metrics while ensuring applied/fallback, completeness and reason describe the current
  invocation. Only after its in-repository red may the optimizer-owned outcome fields be corrected. A later benchmark
  receipt must read that same optimized invocation and distinguish cache reuse from fresh search without invoking the
  optimizer a second time or using a process-global counter for per-invocation attribution.
  The first red is now captured in `run-10-cascades-outcome-freshness-red/`: after actual successful planning and
  fallback on the same root, `optimizer.cascadesApplied` incorrectly remains `true` (1 test, 1 failure, 0 errors).
  An initial prefix-wide reset was also disproven: it removed the retained selected child's rule provenance during
  fallback. Its 1/1 red is preserved in `run-11-cascades-child-provenance-red/`. Replace it with an explicit
  invocation-owned field inventory; a separate real pipeline-cache hit needs its own red because it bypasses the
  optimizer and may copy cached metrics over an incoming root's stale fallback. That pipeline-hit red is now captured
  1/1 in `run-12-cascades-pipeline-cache-red/`: the genuine cache hit retained an earlier fallback flag. Preserve
  selected-plan provenance while correcting both invocation boundaries. The fallback-specific guarantee sentinels
  also survived a later successful replan or pipeline hit: `run-13-cascades-sentinel-pre-fix-matrix/` records those
  two failures and a passing retained-provenance control. Reset only those exact owned sentinel values; other guarantee
  annotations remain plan provenance. A reviewed explicit `InvocationMetric` inventory and both boundary resets are
  now frozen for focused qualification. Cached plans may still carry the original construction's search times and
  counters; the upcoming receipt must classify those as cached-plan provenance, not fresh work on the current hit,
  and prove that distinction while retaining cached completeness, cost and selected-plan provenance.
  The corrected invocation-field reset passes its original smallest selector 1/1 and the complete
  `LmdbCascadesPlanningFailSafeTest` 7/7, including real pipeline and packed cache hits, cancellation, inverse
  success/fallback, and retained rule/guarantee provenance. Reports are in
  `validated-live-source/run-17-deterministic-cache-fixture/`. The earlier store-readiness packed-cache miss and its
  diagnostic remain preserved without a claimed cause; the final test uses an independently proven stable cache
  fixture and requires genuine hit/no-estimator-call evidence.
  After targeted formatting, the same fail-safe class passes 7/7, the ownership-aware V2 fixture passes 30/30, four
  directly affected pipeline tests pass, and core `PackedPlanCacheTest` passes 52/52. The related LMDB packed-cache
  class has 8 failures in 13 cases; compare those identities with preserved pre-capability WIP before attributing them
  to this work. Seven failing methods also failed on both clean merged HEAD and pre-capability WIP. For the remaining
  semantic-zero hit assertion, matched two-method runs fail on both baselines and pass on the current overlay, while
  earlier full-class runs showed the reverse; the result is selection-sensitive and its cause remains unproved.
  The learned-filter test's numeric-only snapshot cannot authorize feedback publication, so its later invalidation
  assertion requires an owner-qualified fixture and an independently observed revision change. Exact reports are in
  `validated-live-source/run-18-targeted-format-final-verify/` and `run-20-cache-drift-matched/`.
  The next approved slice captures an immutable receipt from the same `LmdbBenchmarkQueryPlan.prepare` optimize
  result, before precompilation, with fresh search, packed-cache hit, pipeline-cache hit, fallback and unobserved
  outcomes distinguished. Only the qualified planning benchmark lane imposes COMPLETE/no-fallback requirements, and
  its uncached lane also requires fresh search. A separate explicit expectation must permit intentionally bounded
  incomplete plans for phase 6. Receipt access must not repeat optimization, attribute inherited pipeline counters to
  current work, or leave resources open when validation rejects a call. The first required red is the real
  `LmdbFrontierPlanningBenchmarkTest#measuredPlanningRejectsBudgetExhaustion`. Its first run instead failed in
  setup because no eight-factor Frontier state was observed; it did not reach the budget assertion and is not a
  behavior red. Its explanation shows an applied COMPLETE packed plan with 17 candidates, but no Frontier evidence;
  do not weaken the eight-factor setup invariant or call that lane qualified. Evidence is in
  `validated-live-source/run-19-frontier-budget-receipt-red/`. The approved smaller red uses the existing real Theme
  planning fixture and must prove BUDGET_EXHAUSTED on the same measured invocation, without another optimize call.
  That red is captured: `ThemeQueryPlanRunBenchmarkTest#measuredPlanningRejectsBudgetExhaustion` confirms the same
  invocation is BUDGET_EXHAUSTED but fails because the measured method does not reject it (1 test, 1 failure,
  0 errors), in `validated-live-source/run-21-theme-budget-receipt-red/`. An initial receipt candidate passes that
  selector 1/1, direct receipt tests 5/5 and the full Theme benchmark test 19/19 in
  `validated-live-source/run-23-receipt-candidate-qualification/`. Its Frontier benchmark class still fails the
  unchanged eight-factor setup invariant before receipt checks; the plan in that run reported BUDGET_EXHAUSTED, so
  that lane remains unqualified. Review found a missing outer-cache marker was incorrectly treated as false; the
  in-repository marker/pipeline red now records 8 cases, 4 intended failures and 0 errors in
  `validated-live-source/run-24-marker-pipeline-red/`: absent outer markers were called FRESH/PACKED and the real
  uncached pipeline left null or stale true instead of false. The explicit-marker correction now passes that
  selection 8/8 in `validated-live-source/run-25-receipt-marker-fix-qualification/01-marker-four-methods-green/`;
  the full direct receipt class passes 12/12 and the full Theme benchmark class passes 19/19 in the same bundle.
  Scoped formatting/check and post-format reruns also pass 8/8, 12/12 and 19/19 respectively in
  `validated-live-source/run-26-format-marker-final-qualification/`; these selections overlap and are not additive.
  The frozen selected diff SHA-256 is `0bff2db5760f6cada9370a2cfd54b5436e4123f6f6319426581f562e08d7f735`.
  The uncached lane's OBSERVE expectation still requires an explicitly observed fresh path; it relaxes completion
  expectations, not cache identity.
  Next, diagnose the actual Frontier admission using same-invocation receipts and effective mode, work, deadline and
  memory settings; do not broaden arbitrary limits or weaken its eight-factor invariant. Design genuinely separate
  planning and selected-preparation timing boundaries, including native preparation that remains lazy after
  precompilation. Return a concrete next slice and its validation before changing that implementation.
  Same-invocation native receipts must distinguish considered, selected, opened, executed-completed, fallback and
  abort for each native subregion. `DispatchLoop`'s process-global decision records/observer are not sufficient for
  attribution: ranking precedes open and the observer fires after open, not completed consumption. Inspect same-tree
  `LmdbNativeExplain`/attempt metrics and evaluation-identity phase events instead; detached receipts must not retain
  a source or lease. WRITTEN/EXACT algebra controls alone do not prove a fixed physical plan.
  Existing runtime-plan summaries merge openings by rendered shape, OR completion and overwrite strategy, so they
  cannot identify one evaluation of a reused prepared step. The approved next design introduces an opt-in, bounded,
  detached per-evaluation typed contract with an outer benchmark invocation handle distinct from native region tokens.
  Begin with native row-owner full/partial/reuse/concurrency and explicit UNOBSERVED/UNLINKED status; GROUP/nesting
  follow. Add only unused behavior-neutral value/interface scaffolding first, then capture real prepared-step missing
  event reds through that contract before connecting runtime owners. Do not manufacture a red via a test facade that
  always returns UNOBSERVED, or treat cumulative explain telemetry as a defect.
  The initial contract is opt-in and bounded by event and label limits; overflow reports TRUNCATED rather than
  overwriting prior evidence as complete. Detached facts carry only primitive/enums/immutable labels, no per-row
  events or live sources, contexts, leases or throwables. Scoped handles restore on every exception/iterator call,
  and workers inherit explicitly; any global ID generator is for uniqueness, not attribution. A real compiled-step
  test must cover full and partial consumption in both orders, concurrent openings, disabled capture and overflow
  isolation before lifecycle wiring. Uninstrumented group/retry/nested regions stay UNOBSERVED. Publish terminal
  completion only after the actual owner's successful cleanup; selected-to-executed linkage remains UNLINKED. Distinguish
  FAILED and CANCELLED from a discarded speculative attempt, preserve root-complete versus child-partial under LIMIT,
  and do not render full plans or exception messages merely to discover that the observation budget is exhausted.
  The unused bounded scaffold passes 8/8. Real compiled-step full/partial tests in both orders fail 2/2 and concurrent
  openings fail 1/1 at the intended OBSERVED-versus-UNOBSERVED assertion, after result bags/filter settlement and
  distinct invocation IDs pass. Reports are in `native-observation-qualification-20261107-h0718btq/`. No native
  lifecycle wiring was present for those reds.
  A real owner-transfer test must leave capture A's entry scope before consuming its iterator, then verify later pulls
  under no scope or capture B still publish only to A. Row EOF precedes final context settlement, and a context close
  can defer work until the last lease releases, so worker-handoff and cleanup-failure tests are required before any
  terminal event can claim successful completion. The six transfer variants now reach their intended missing-event
  reds after bag, scope-restoration and full/partial filter-settlement checks pass; reports are in
  `native-row-settlement-qualification-20261107-iv16n1qj/`. Corrected real context-resource/producer cleanup cases
  now fail 6/6 and held-child/worker settlement cases 2/2 only at the intended missing-event assertions, after bags,
  cleanup identity, lease and release gates pass; see `native-row-settlement-fixture-qualification-20261107-rodi9bwa/`.
  Real cancellation cases also fail 2/2 only at the missing-event assertion after cancellation, cleanup and identity
  checks pass. The 0/2-cap real tests currently see UNOBSERVED because no owner event exists, so they do not yet prove
  overflow behavior; retain them as post-wiring gates alongside the passing standalone bounds controls. Evidence is in
  `native-row-cancel-import-qualification-20261107-f3b8253d/`.
  An initial owner-wiring candidate passes 11 focused selections, 40 tests, in
  `native-row-owner-wiring-qualification-20261107-7006b5de/`, including real overflow and lease/worker controls.
  Review found the new latch must not infer PARTIAL when producer outcome is still absent, and a reentrant producer
  close may deliver a nested finish before the outer cleanup returns. Both counterexamples are now recorded: the
  outcome-after-settlement unit failed 1/1, and real reentrant close failed 2/2 while preserving original throwable
  identity and close count; all saw a premature PARTIAL. Reports are in
  `native-row-reentrant-red-qualification-20261107-dbf98c64/`. Correct only the observation latch without altering
  engine scheduling/exception precedence. Review also found a specific promotion-order risk: an occurrence-local
  cancellation can carry a real suppressed producer cleanup failure, but the new finally block could publish CANCELLED
  before existing promotion rethrows that failure. Add a real reentrant cancellation+cleanup red, then bracket the
  existing promotion boundary without duplicating throwable analysis. The red is now captured: both EOF cases pass;
  both cancellation cases preserve cleanup primary, cancellation observer/origin, one close and no terminal during
  cleanup, then lack the required FAILED event. The full actual phase was not printed, so do not assert it was
  CANCELLED. Reports are in `native-row-cancel-promotion-red-qualification-20261107-27c7f87a/`. The 40-test result is
  pre-correction, not final.
  The final owner change brackets the existing cleanup-failure promotion before publishing a terminal event. The
  original promotion and outcome-ordering reds pass 4/4 and 1/1; full observation and filter-completion classes pass
  17/17 and 196/196. A post-format combined selection then had one incomplete-feedback assertion error: its worker
  gate did not establish that the full filter input remained unconsumed. The same method passed once on each of the
  formatted candidate and a constructed candidate-minus-observation counterfactual; neither proves the failing run's
  cause. Preserve that unresolved report and the counterfactual manifests in
  `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/validated-live-source/native-row-promotion-fix-qualification-20261107-28097232/`.
  A test-only fixture now holds both distinct workers at first acceptance and derives its source population from the
  bounded morsel, batch and queue capacities, preserving the reentrant-close and incomplete-feedback assertions. The
  focused method passes 1/1, its class 196/196, and the combined lifecycle selection 280/280 both before and after
  formatting. The final fixture patch and reports are in
  `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/validated-live-source/worker-reentry-fixture-review-oqtihPXv/`.
  This qualifies only fresh owned row-region observations; group, ordered, nested, retry and physical strategy events
  remain unobserved, and selected-to-executed linkage remains unlinked. Next bind an invocation-owned benchmark handle
  to the same capture across lazy evaluation and cancellation, beginning with a real delayed-step regression.
  That real delayed-final-result regression failed 3/3 at opening capture A's missing ROW event after exact bags,
  same-tree one-optimize/precompile, scope transfer and lazy-open checks passed. The unchanged assertions now pass
  3/3: an opt-in capture follows the returned iterator's operations and timeout cancellation, and a per-evaluation
  handle owns its detached snapshot. No-capture/disabled openings preserve the original iterator; cooperative
  cancellation is advertised only when the wrapped result supports it. The native preparation class passes 19/19 and
  the combined helper/preparation/receipt/observation/Theme selection passes 84/84 before and after scoped formatting.
  Preserve the red, final patch and reports under
  `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/validated-live-source/benchmark-observation-FMCuV4IH/`.
  Per-invocation row observations do not yet certify the outer query, a native strategy or fallback; next separate
  optimization, final-result precompile and lazy evaluation/first-pull/drain/close timing boundaries on the same
  owned invocation, and build the bounded independent semantic catalog before matched A/A timing.
  The catalog oracle now has a typed exact solution-bag codec and one global blank-node bijection; exact duplicate
  multiplicities, unbound slots and nested RDF terms are retained. The prior cross-store oracle compared raw blank-node
  identifiers and failed an isomorphic-input regression. Subsequent review exposed malformed numeric/literal records,
  multiplicity-sum overflow and row-recursive stack overflow on bounded-stack 4,096-row ground and blank-node bags;
  all were recorded as reds before correction. Ground rows now match directly and blank-node rows use an iterative,
  reversible bijection search, with exhaustive small-graph controls and no arbitrary size cutoff. A separate typed
  adapter regression caught 24 ways to silently normalize inconsistent literal metadata; reconstruction now verifies
  the captured metadata. The reviewed final patch passes codec 19/19 and cross-store oracle 9/9 after formatting. Reds,
  source identities and qualification reports are under
  `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/codec-canonical-final-review-k6vdezp0/` and its linked bundles.
  This codec is infrastructure, not catalog parity: generate and independently verify a bounded REAL_ESTATE bundle
  for 13 cells first, then account for all 182 cells and SPARQL nondeterminism before claiming semantic coverage.
  The bounded REAL_ESTATE fixture now has independently checked q0--q12 witnesses, and Jena generated and separately
  verified 13 raw typed bags from its pinned 522-statement dataset. The first hermetic cross-engine comparison failed
  at q0: Jena emitted the computed decimal AVG `950000.0`, while RDF4J MemoryStore emitted `950000` with the same
  datatype and numeric value; the other row and multiplicities matched. Preserve both raw bags and the exact failure.
  A separate, conservative expression-provenance policy may compare supported computed integer/decimal results by
  equal datatype and exact numeric value, while source terms and unproved or term-sensitive origins remain exact;
  this is value equivalence, not exact Jena RDF-term equality. RDF4J MemoryStore, generic LMDB and native LMDB must
  still match each other as exact bags. The approved design and its limitations are in
  `/private/tmp/rdf4j-computed-policy-design-20261107-mffQAV/design.md`. Its separate, fail-closed policy now preserves
  raw rows, multiplicities and global term identity while relaxing only proven computed decimal/integer outputs with
  the same datatype and exact numeric value. The manifest records and independently recomputes the proof. All 13
  bounded REAL_ESTATE cells pass the Jena comparison under that stated policy and match exactly among RDF4J modes;
  policy, bundle and codec tests pass 15/9/19, and the LMDB fixture, independent oracle and observation classes pass
  3/9/17 after formatting. All 13 native-enabled execution receipts remain UNOBSERVED, so this establishes mode parity
  only and does not prove native execution. The 16 raw dataset, result-bag and order files remain byte-identical to the
  independently verified Jena bundle. Preserve red, commands, observations and final qualification at
  `/private/tmp/rdf4j-computed-policy-final-review-20261107-Pa6K0h/qualification-summary.md`. Jena fixture conversion
  explicitly rejects unsupported named graphs, directional literals and triple terms; all other catalog cells remain
  open, as do native selection/execution identity and a valid matched timing baseline.
  The timing slice preserves legacy `planQuery` and `planUncachedQuery` as combined measurements and adds separately
  named optimize, final-tuple-precompile, reused-prepared-step and end-to-end lanes. An owned preparation lifecycle
  must carry the same selected tree and receipt across stages, preserve eager `prepare` behavior, and retire dataset
  and branch on all failures or transfer them once. An untimed exact-bag qualification does not prove every later
  measured invocation's bag; counts and fingerprints do not fill that gap. Preparation in invocation teardown may
  warm code or feedback, so cache, feedback and storage regimes must be pinned before A/A timing. A reused prepared
  step is not a fixed physical plan: lazy native selection, binding and code generation can remain in evaluation.
  The final seven-file timing/fixture patch is
  `/private/tmp/rdf4j-timing-lanes-20261107-3z24z8pv/final-review/timing-and-fixture.patch` (SHA-256
  `e46891743f14df559a8d2c600423a070aff5d9173c0b6cae52fb90d35080dcf6`). After scoped formatting, the related
  selection passed 132/132 and the unchanged fixture/lane selector passed 25/25 before and after; the legacy helper
  baseline selector passed 15 cases before and 30 after its class gained 15 cases. Preserved reds prove cleanup
  failure identity, timeout restoration, explicit-input use and malformed fixture rejection; exact commands and
  reports are in `final-review/qualification-summary.md` beside the patch. All four actual generated JMH lane bodies
  completed a one-fork, one-iteration lifecycle smoke on a fresh, isolated REAL_ESTATE q2 store. Its same-helper
  preflight reported FRESH/COMPLETE and matched the 25-row Jena bag exactly; the three measured planning invocations
  reported PIPELINE_CACHE/COMPLETE, while the reused-step lane reported that receipt once at its initial preparation.
  The smoke used native execution disabled, discarded timing scores, and did not establish selected-to-executed
  identity or per-invocation bag equality. Preflight executed first; generated-code and OS page-cache state remain
  UNKNOWN. All 20,174 copied inputs stayed byte-identical; 1,600 additional isolated build outputs were retained and
  enumerated. Command arrays, fixture hashes, observation limits and logs are in
  `/private/tmp/rdf4j-timing-lanes-20261107-3z24z8pv/isolated-jmh-snapshot/`.
  During an attempted diagnostic, `debug-surefire.sh --skip-install` unexpectedly formatted 210 Java files before
  failing compilation against stale local artifacts; no test JVM started. The reviewed recovery patch restored all
  210 verified preimages, including six WIP paths and the new consumer, with no other changes across 23,136 source
  and configuration paths and no change to HEAD, branch or index. Preserve the pre/post copies, logs and audit at
  `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/formatter-live-recovery-20261107T182000Z/`. Future manual
  Maven and debugger invocations must use current installed artifacts and explicitly skip formatter/import sorting.
  In the preserved same-flow AUTO diagnostic, the actual receipt is FRESH/BUDGET_EXHAUSTED with
  CANDIDATE_EVALUATIONS, 17 charged work/candidate units and no selected Frontier state. The paired explicit-only
  dataset reports epoch 5, owner/transaction/dictionary present, plane 1 and a current matching preparer. Selected
  fallback/admission annotations are absent, so this does not identify the session that was admitted. The separate
  EXACT control is FRESH/COMPLETE with the same 17 work/candidate units and zero selected Frontier state; it also has
  no selected admission metadata. Increasing a limit alone therefore does not explain the observed EXACT failure.
  Evidence is under `run-34-frontier-same-flow-diagnostic-w7i55469/`. A remaining hypothesis is that the optimizer
  pipeline captures an empty qualified epoch during construction and substitutes generic evaluation statistics,
  bypassing the Frontier provider; a later live owner does not prove the constructor saw that owner. Capture the
  actual constructor-time decision and inspect isolation/lazy-pin timing before treating this as a cause. A corrected
  sealed-source EXACT debugger run observed epoch 5 and an actual admitted `LmdbFrontierStatisticsCostSession` with
  generation 5 and plane 1, so that no-session hypothesis did not occur in this invocation. Its first inspected
  Frontier leaf had 4,096 estimated rows but unknown physical work/seeks and a max-double objective; determine whether
  that uncertainty is justified and trace evidence through attachment, detachment and selected materialization. Its
  first leaf did attach `proof=1` and `evidenceStateId=1`; residual components outside the selected native prefix
  deliberately leave actual skip-scan visits/seeks unknown rather than deriving them from candidate population.
  The paired uninstrumented control still fails the eight-factor assertion. Debugger pauses invalidate timing and
  completion comparisons; exact source/control/debug evidence is in `run-40-frontier-exact-debugger-sealed/`.
  A separate source audit found that `CascadesPlanner` derives public completeness and provenance reason only from
  legacy deadline/work booleans, so packed resource-limit or unsupported-semantics outcomes can be labeled COMPLETE.
  A later sealed single-invocation EXACT trace links that defect to the observed Frontier setup failure: provider
  admission exhausted retained memory at 66,673,230 of 67,108,864 bytes, then scalar fallback ran without provider
  evidence. The packed result was `INCOMPLETE_RESOURCE_LIMIT`, but its public receipt said COMPLETE. The earlier
  extraction/detachment branches were not reached. The paired unpaused TRACE run also had no detached events; this
  identifies the observed route, not the cause or legitimacy of retained-memory consumption. Full evidence and the
  no-debug source-identical control are in `run-43-frontier-provider-trace-jdb/`; debugger timings are diagnostic.
  A subsequent sealed first-latch trace identifies a 12,582,912-byte `PackedPhysicalMetadataArena` planned-double
  metric array growth (1,048,576 to 2,097,152 slots), requested while 435,634 bytes remained. It arose through
  `PackedMemo.addPhysicalMetadata` and `PackedJoinEnumerator.retainDenseCandidate`, not an optional provider probe;
  no duplicate charge was observed. These are logical cumulative reservations, not a measured live heap. Examine
  arena/candidate lifetimes before declaring over-accounting; do not increase the production default or weaken the
  eight-factor invariant. The unpaused EXACT control and trace are in `run-45-frontier-first-resource-latch/`.
  For phase 0 this eight-factor lane is explicitly resource-incomplete and excluded from COMPLETE-planning latency
  comparisons until the actual invariant is met. Continue baseline qualification using lanes whose eligibility and
  completion are observed; a general configurable retained-budget contract belongs to phase 1, not a benchmark-only
  increase of the production default.
  Add an in-repository five-status boundary matrix and a real
  resource-limited public optimize red before any exhaustive mapping/API or receipt-policy correction. Both are now
  captured in `run-42-completeness-reds/`: resource/unsupported are mislabeled COMPLETE in the five-status matrix,
  and a real zero-retention executable plan is likewise mislabeled. Two receipt cases also fail to propagate those
  statuses. The approved exhaustive adapter/completeness/receipt correction may proceed; preserve selected plans,
  metrics, decision summaries and distinct causes. The reviewed fix now passes all focused reds and full affected
  classes: boundary 8/8, resource budget 18/18, receipt 14/14 and Theme 19/19 in `run-44`; these counts are distinct
  full classes, not added to their overlapping focused selections. Scoped formatting/check and post-format reruns
  pass core 26/26 and LMDB 33/33 in `completeness-format-20261107-bsoj8nl2/`; selected diff SHA-256 is
  `bd1032ff08c113c9e156b00d8ee5b7d79610494640be8226c50880b2e249ea2d`, with no nonselected source changes.
  Read-only review found that Frontier setup uses a separate default-inferred explain, whereas measured preparation
  forks an explicit-only source. The benchmark helper also calls the three-argument strategy factory (thereby using
  the default policy) and `precompile`, while the repository passes its actual execution policy and calls
  `precompileFinalTupleResult`. The next approved test-first slice will exercise the real native tuple-result entry,
  non-default policy and exact root/nested bags before aligning the benchmark helper. A separate Error-path cleanup
  red must prove primary/suppressed exception behavior before any ownership correction. The later Frontier diagnostic
  must use the same prepared invocation/receipt and declared population as measurement. Label stages optimization
  and final-result precompile; native opening, fallback compilation and binding can remain lazy through first pull.
  Native-entry tests now reproduce both helper mismatches (3 cases, 2 intended failures): final tuple-result entry was
  not called and the non-default store policy was not passed; nested duplicate/aggregate bags pass. Reports are in
  `validated-live-source/run-28-native-entry-error-reds/01-native-entry-red/`. Separate Error-path tests also fail
  4/4 as intended: the dataset is not closed at three preparation stages and cleanup failures are not suppressed
  onto the original error. The reviewed helper candidate passes the native-entry/policy/bag class 3/3 and the
  preparation-failure class 5/5, including an added self-suppression/branch-Error case, in `run-29-*`. Dependent
  helper test run has 11/12 passing; its plan-quality audit failure (`Filter at root.requiredInputFrame`) reproduces
  identically on the preserved pre-entry source in `run-30-preentry-query-audit-ixmh6vlz/`. Receipt/Theme tests remain
  to be rerun after this classification. The distinct successful-close Error red is now captured 4/4 in
  `run-31-close-control-prefx-o0pac16r/01-close-error-red/`: branch closure may be skipped, the primary may be
  replaced, or self-suppression may throw. Separate real native WRITTEN/EXACT controls pass 2/2 for their declared
  selected-algebra order and duplicate-preserving result bags; they do not establish a fixed physical plan. The scoped
  close fix now passes the focused red 4/4, full failure-cleanup class 9/9, native preparation class 5/5, receipt
  class 12/12 and Theme class 19/19 in `run-32-close-fix-qualification-u2ba722b/`; the focused four overlap the nine.
  Targeted formatting/check changed only the selected native-preparation test, and a post-format combined run passes
  all four full classes 45/45 in `run-33-close-helper-format-and-verify-efu6t87g/`. The pre-existing plan-quality
  audit and Frontier setup failures remain open.
  Matched A/A timing may begin only after freezing the forward source and all measurement identities and declaring
  which observed, COMPLETE lane and cache/feedback/storage regime it measures. A bounded q2 result is partial baseline
  evidence, not the full-scale or fixed-physical-plan acceptance comparison.
  Audit follow-up: direct mapped callbacks currently check their owning scope at entry. A writer can overlap a pinned
  read, but overlap alone does not show a defect: the older-snapshot estimate may linearize before that commit. Before
  changing this separate lane, establish the authority/feedback/selected-recipe publication contract and reproduce an
  invalid escape beyond permitted snapshot semantics; the delegated-cost red does not establish one.
- [x] (2026-11-06 12:58Z) Run the mandatory initial offline root quick clean install. All reactor modules succeeded in 01:45; tests were skipped. Full output is `maven-build.log`; the prior log was preserved as `/tmp/maven-build.log.pre-initial-root-quick-install-20261106`.
- [x] (2026-11-06 13:40Z) Observe the smallest physical-quantity regression before production changes. `PackedDeclaredQuantityReplayTest#composingAChildDoesNotInventAnUnknownSeekCount` ran one test with one expected failure because an unknown seek count became known during composition. Offline Maven first lacked the Surefire JUnit provider; an online retry reached the test. Retained report and exact commands are in `initial-evidence.unified-costing-20261106.txt`, with full logs under `/private/tmp/rdf4j-unified-costing-regression-20261106T133509Z/`.
- [x] (2026-11-06 13:49Z) Observe the independent dependent-subquery pricing regression. `PackedPhysicalCostContractTest#dependentSubqueryObjectiveIsNotRepricedAsSourceRows` ran one test with one expected failure: objective 1110 instead of 210 when a tenfold source price was applied again to an already-priced subquery. Evidence is appended to `initial-evidence.unified-costing-20261106.txt`, with retained reports and logs under `/private/tmp/rdf4j-unified-objective-red-20261106T134457Z/`.
- [x] (2026-11-06) Observe a selected-prefix access counterexample after updating its mocked read-selection fixture to model the current session contract. `LmdbPackedRepeatedVariableAccessCostTest#selectiveConstantOutsideThePrefixDoesNotShrinkTheScannedPopulation` ran one test with one failure and no errors: the prefix population is 97 while the complete-pattern logical result is 1. Later source inspection showed `LmdbRecordIterator` can skip residual-mismatch runs, so the test's original expectation that exactly 97 entries are visited is not established; it must be corrected against real traversal and seek evidence. The original fixture setup error and diagnostic red are preserved under `/private/tmp/rdf4j-lmdb-access-regression-20261106T135557Z/` and `/private/tmp/rdf4j-unified-costing-final-regressions-20261106T141102Z/01-access/` respectively. The candidate LMDB correction is provisional until it represents that distinction honestly.
- [ ] **[in_progress] Unified phase 1:** add real structural region-coverage/work-once, typed-outcome, shared-budget
  reservation/cleanup and zero-optional executable-fallback contract tests, then a behavior-preserving adapter. Keep
  existing scalar models and selection behavior compatible; review each substantive slice before expanding it.
  Current review follow-up: qualify the scoped invocation and VALUES corrections against the affected selections.
  The real-store scalar red retained an isolated output of 100 but priced a per-invocation candidate of 50, 100 scans
  and 100 result rows for two unrelated inputs; the corrected focused selection passes with candidate 100 and 200
  scans and result rows. An actual enumerator red priced two isolated reopen variants at 134 rather than 150; both
  now pass, as does the disconnected-component control. An isolated factor with possible input bindings retains
  unknown candidate, visit, seek and result work when no conditional model is available. Finite and component
  quantities retain their own measured domains. The estimate-zero invocation clamp is unchanged.
  Before the correction a nested VALUES row-only name was absent from both leaf and parent possible-output masks;
  two variants failed despite projection-barrier and bag controls passing. Cached binding analysis now marks the
  header domain incomplete and propagates that uncertainty while preserving the declared header. The affected
  finite-filter rule control passes; this does not establish blanket safety for every pre-memo rule. Exact red and
  focused green evidence is retained under `/private/tmp/rdf4j-quantity-invocations-20261108-ty_kmnh_/`.
  Quote graphs remain fixed transient per admitted callback; no new reusable scratch was added. The new input proof
  walks up to four statement terms for each prefix relation inside an admitted callback, but has no distinct
  per-relation work charge. Existing reusable `PackedCostEstimate` metric-array growth likewise has no separate
  reserve at allocation. Neither total JVM heap nor complete callback work is claimed as budget-accounted.
  The interim formatted 192-test selection passed 203 cases and matched all 192 earlier identities, including the
  original 76 and dependent 38. Subsequent fully bound scope reds showed an isolated all-constant factor changing
  from 1 to 2 rows and 2 to 4 scans/results, and a prefix-bound isolated factor changing from 100 to 2; both were
  spuriously marked contextual by the scalar compatibility adapter. A further opaque-prefix constant red produced
  unknown work even though constants cannot observe input bindings. The focused corrected selection passes both
  tests, including the opaque-prefix variant. Preserve the exact red reports and rerun qualification on the final
  formatted source; no new claim about the previously excluded fixture failures follows from these tests.
  A downstream audit found that dense append and retained-context paths can keep a finite isolated winner price when
  a contextual factor explicitly reports unknown work. Reproduce this through the real enumerator before changing
  propagation; an unpriced dependent candidate must not become a finite winner through its invocation stamp. The
  real two-factor planner red selected dependent iteration and certified a lower bound despite unknown contextual
  work; its actual result bag and a fully priced dependent control passed. A separate single-access red has no priced
  alternative but returned a finite total and lower bound; its executable result bag remains correct. A third red shows
  an unopened provider on the zero-budget fallback still received a finite total; that test preserves its separate
  INCOMPLETE_WORK_LIMIT search reason. Preserve search completion independently from typed comparison and selected-price
  availability, and propagate unknown through the memo, public plan/provenance, decisions, caches, learning and
  telemetry without a numeric proxy. Preserve a priced independent alternative and an executable ordinary fallback.
  Full TDD red evidence for all three is in `evidence/red-21-price-comparison-budget/` under the invocation artifact.
  One exploratory build overlapped live source edits; `evidence/iterate-core-unpriced-01/INVALID-SOURCE-OVERLAP.txt`
  marks its reports invalid even though the start/end hash check happened to report stable. All later source edits and
  builds are serialized. A stable, pre-adapter public-boundary test then failed because an unavailable packed price
  became an accessible finite `CostVector`. A separate real LMDB cache red failed for both unpriced and priced-but-
  incomparable winners: the former threw on an invalid interval, and the latter was reused as a cache hit. The source,
  Surefire reports and initial-evidence entries are retained in `evidence/red-23-public-boundary/` and
  `evidence/red-24-lmdb-cache/`. Boundary and cache fixes are in progress and have not yet passed verification.
  A subsequent real `EXISTS` parent red exposed unknown work being converted to a saturated finite number; its
  priced control and executable bag are retained. A further real memoized-optional callback observed the same
  saturation, while a dependent-only admitted search assigned a finite 427 to an unpriced fallback. The latter's
  small input is below the existing hash admission threshold; a larger input separately exercises the priced
  independent alternative. Reports and exact sources are in `evidence/red-25-unpriced-parents/` and
  `evidence/red-26-memo-and-fallback/`. Corrections remain subject to focused and cross-path verification.
  The scalar `EXISTS` variant separately failed in `rebaseObjective` while the explicit-physical variant passed;
  `evidence/red-27-scalar-parent/` retains that in-repo red. A read-only JDWP breakpoint on the fully priced
  control identified an unpriced written-order fallback retained alongside the costed ordinary join. An interim
  candidate-equivalence check produced a focused green but was withdrawn: runtime join guards and LMDB native
  selection can consume per-node estimates, so the two recipes' different estimates do not prove identical physical
  execution. Preserve that focused run and exact withdrawn source as investigation evidence, not final acceptance.
  A priced winner remains usable but its comparison stays unbounded in this case until actual execution identity can
  be proved. The written-order safety check also walks prefix relations and at most
  six existing binding masks per relation without allocating new masks; it has no distinct per-mask work charge beyond
  its existing owning planner operation. The breakpoint evidence is in `evidence/debug-known-comparison/`; no
  complete JVM-heap accounting is claimed.
  Further production-path reds found that a nested UNION could compose a context-free child into a purportedly
  contextual price of 321, and that an ordinary OPTIONAL could price unavailable bound work from an isolated child.
  A condition-bearing OPTIONAL retained a finite fallback of 226; a competing unconditioned OPTIONAL suppressed its
  independently priced hash alternative. Scalar and explicit-physical providers reproduced both. A separate bound
  isolated-estimate red showed unsupported prices 122 and 226; a disconnected explicit access required 427 rather
  than 226 for two inputs, and a mixed VALUES input was assigned 223 without contextual pricing. The initially
  asserted scalar value of 222 was corrected before the fix to the established startup-once recurrence of 221; the
  exact original and corrected reports are retained. The first inherited-prefix investigative red is not itself proof
  of selected mispricing: its returned 30-cost winner had context zero and the direct dependent caller rejects it.
  `red-29` and `red-32` through `red-36` preserve the actual reports and source snapshots under the invocation artifact.
  Contextual composition now requires the requested input identity; OPTIONAL retains unavailable work separately from
  an executable fallback, and can select a finite independent implementation without certifying a complete comparison.
  A proven disconnected prefix repeats the source event's isolated work without treating composed row metadata as an
  invocation-work certificate. A prefix with possible, but not assured, bindings still reaches the provider. The
  source-stable `green-36-optional-scope` focused selection passed 281/281 including bags, known prices, mixed inputs,
  independent alternatives, and zero-budget controls. Search exhaustion remains distinct from comparison completeness.
  The new prefix-domain check traverses the already retained prefix masks without allocating reusable scratch, but has
  no separate deterministic charge for each mask operation. A broad formatted-source qualification is still required.
  A further 214-case LMDB fallback selection reported
  182 null-read-selection fixture errors and 11 binding-contract assertion failures; all 193 matched by test identity,
  kind and message in the isolated 12,661-source-hash-matched immediately pre-follow-up counterfactual, with the
  current authorized index precomputation. This comparison does not establish their origin on an earlier base.
  The inactive provider contract's three-blocker review follow-up has matching planner-backed evidence: before the
  correction, 10 of 14 tests failed for cancellation precedence, an attempt resumed after raw admission refusal,
  or a provider getter reentering the shared vector builder. The focused corrected selection passes 37 of 37,
  including the original 23 provider-contract controls. Reports, scoped formatting and source identities are in
  `/private/tmp/rdf4j-provider-review-fixes-20261108-m5ys9pbp/`. The corrected candidate awaits independent review;
  fresh broad affected and LMDB qualification must follow review. No runtime selection or backend was activated.
  The subsequent signed-zero review correction uses the arena's numerical equality when rejecting UNKNOWN
  degenerate intervals. Real planner/provider tests covered quote and boundary for all signed-zero point/endpoint
  permutations, both before and after actual interval-builder materialization, and exact-zero and invalid-input
  controls. The two tests failed before the correction and passed afterward; the direct two-class selection passed
  39/39. Evidence is in `/private/tmp/rdf4j-provider-signed-zero-20261108-8b_m1ywq/`; this candidate awaits review
  before fresh broad qualification.
  The next review correction rejects non-finite, negative and unordered quantity intervals before reading or
  interpreting lineage, while checking cancellation around the provider getters. Real quote and boundary tests
  exposed masked invalid UNKNOWN declarations before the change; the matching five tests and the prior two-class
  direct selection now pass 5/5 and 44/44. The new source remains inactive and awaits review before broad
  qualification; evidence is in `/private/tmp/rdf4j-provider-interval-validation-20261108-_99a26rw/`.
  Sol's review of the corrected finite provider follow-up found no blocker, as relayed by the coordinating agent.
  Fresh qualification on the exact reviewed source passed 455/455 affected core tests and the same three original
  scalar, inclusive-cost and zero-optional nested-bag controls. The affected LMDB selection passed 49/50: its sole
  pipeline hot-hit assertion matches the preserved pre-repair counterfactual by method, message and line 359; its
  deeper cause remains unestablished. Source identities were stable across each selection; the full commands and
  reports are in `/private/tmp/rdf4j-provider-qualified-20261108-k28hrwt6/`. No runtime strategy was activated.
  A phase-2 access foundation now shares the configured-index contiguous-prefix calculation used by the native
  profile and packed access description. Their existing pricing, selection and source ownership remain separate.
  The same 74 real-index, native, packed and edge-case controls passed before and after, and after scoped formatting;
  the original three scalar/inclusive/zero-optional controls passed again. Seven tests in the existing mocked
  fully-bound class failed before and after with the same null read-selection error, before index costing; the
  separate pipeline hot-hit assertion still matches the preserved pre-repair counterfactual. Evidence and scoped
  source audits are in `/private/tmp/rdf4j-shared-index-shape-20261108-8aw_l3n_/`. No source-root LEAF provider,
  observer, selection, executable binding, or complete unified physical quote is claimed; phases 1 and 2 remain open.
  The following access-feature slice uses one allocation-free bound/prefix description and common covered/residual
  derivation in both native quote and packed/factor access paths. The native absent-ordered-index case stays distinct
  from an available empty prefix, and each consumer retains its existing interval, unknown and scalar pricing.
  The prior 74 controls plus one real native all-mask control passed 75/75 before and after and after formatting;
  38 dependent fan-out and native quote tests also passed. Reports and audits are in
  `/private/tmp/rdf4j-shared-access-features-20261108-j85qts42/`. Numeric work, consumer/snapshot authority,
  executable recipes, discovery and strategy selection remain open; neither phase 1 nor phase 2 is complete.
  The subsequent shared quantity candidate was independently reviewed and required two corrections. The partial
  packed residual path had already calculated a candidate population, but replaced `output.accessRows` with unknown
  before the same production quote read it. Pass that candidate into the existing quote separately; retain unknown
  visited entries and skip-scan seeks, and project the modeled per-invocation candidate as a named planned metric.
  Each quote quantity now carries an enforced source-population, per-invocation, total-invocation-work, isolated-factor,
  connected-component or complete-context scope. A per-invocation estimate can be an average, not a guarantee about
  each binding. These scopes do not certify physical bounds, a shared snapshot, an execution receipt or cross-consumer
  authority. Existing native and packed pricing remain the compatibility targets; allocation/planning overhead and
  performance have not been measured for this change.

  The real-store residual regression first checked a 1,000-entry prefix, unknown visited entries and seeks, and one
  open. Its newly requested planned-candidate metric was absent before the repair, so the single-method report failed
  with expected 1,000 and actual NaN; the matching method passed afterward. This red observes the missing metric, not
  the private quote. Source tracing separately establishes that the old quote received unknown access rows. A separate
  ordinary optimized and executed query showed a covered candidate metric survive selected-recipe materialization;
  this does not claim that the residual quote was selected. The final scoped LMDB selection passed 192/192, including
  exact testcase matches for the previous 76/76 and dependent 38/38, seven scope tests, the real-store and selected-plan
  cases, and 16/16 `TripleIndexPrecomputationTest` tests on the user-authorized current index baseline. The three
  original scalar/inclusive-cost/zero-work core controls also passed. Reports, source identities and scoped formatting
  are in `/private/tmp/rdf4j-quantity-scope-repair-20261108-XLRf96ca/`.

  A broader 195-test run had three null mock-estimate errors in `LmdbAccessQuoteEvidenceQualificationTest` and one
  annotated-filter assertion failure. All four matched by method, kind and message in a private source copy matching
  every one of the 12,642 saved immediately pre-repair Java/XML/plan hashes and including the current external index
  changes. This is pre-repair-candidate attribution, not proof against the earlier extraction or HEAD; the four were
  excluded from the final 192-test selection and remain open. The earlier seven fully-bound mock failures and the
  separately reproduced pipeline hot-hit failure were not rerun. The repository-wide copyright check also reported
  twelve unrelated pre-existing invalid headers; the six changed Java files passed the scoped header and formatter
  checks. Independent review of this candidate is still required before declaring the slice accepted.

  The subsequent invocation-scope review found that the non-contextual scalar path divided an isolated 100-row
  factor by two unrelated VALUES invocations, then stamped the result as per invocation. Real cost-session reds
  established that source visits and emitted source work also lost the second invocation. The revised quote keeps
  isolated, connected-component and complete-context domains distinct, scales the unchanged disconnected input,
  and retains unknown work for bound or mixed inputs without conditional evidence. It leaves modeled zero-prefix
  clamping separate from a proof that no execution occurs. Native one-invocation intervals and packed point-only
  multi-invocation quotes now have separate supported contracts. A separate nested VALUES red established that
  row-only binding names were missing before parent-mask derivation; declared header and materialization identity are
  preserved while possible names are conservatively carried through the existing binding analysis and scope barriers.

  Unknown physical objectives now remain unavailable through winner selection, parent composition, detached
  metadata, experimental plan provenance and telemetry. Search exhaustion and cost-comparison completeness are
  separate: an executable fallback or priced winner remains valid, but an unpriced relevant alternative prevents a
  cheapest-plan certificate. Exact-key cache replay can retain a valid unpriced recipe and its original owner; family,
  stale, reranking and learning paths require their separate proofs. Actual detached checkpoint and stale-generation
  tests first failed when valid rows/work with an unavailable current objective threw an invalid-estimate exception.
  Replay now emits a distinct unavailable-price refusal; resume and stale validation decline reuse and replan without
  fabricating a certificate. Existing provider failure and cancellation identity controls remain in the selection.

  The plan-quality audit retains an unavailable executable root candidate separately, in trace order, without
  comparing it numerically or changing the selected plan. In the original bound bushy fixture, the actual candidate
  was materialized with unavailable estimated and comparison costs; finite per-node annotations did not prove its
  complete bound-input work. The same test identity retains the original per-reopen inequality for a disconnected
  prefix, whose input is unchanged on every invocation; an additional bound-prefix test checks the explicit unpriced
  candidate. No extra estimator pass is made by the audit. The focused core selection passed 363/363 before final
  formatting, including these controls, exact-owner replay, provider exception handling and zero-budget fallback.
  The corresponding reds and reports are in `/private/tmp/rdf4j-quantity-invocations-20261108-ty_kmnh_/evidence/`.

  A 20-case counterfactual against the exact immediately pre-follow-up Java/XML/plan source matched five broader core
  failures; it does not establish behavior against an older base. Three LMDB feedback-stat failures likewise matched
  that source. A separate allocation assertion passed on the counterfactual but failed once in the current combined
  run and remains to be rechecked; it is not attributed to the baseline. The seven fully-bound mock errors, four
  prior scope-slice failures and prior pipeline hot-hit failure remain separately excluded and unresolved. One
  source/build overlap run was invalidated and preserved; subsequent reported runs used stable source snapshots.

  Quote graphs remain bounded transient callback objects, not cached quotes. No new reusable audit scratch array was
  added, but retaining extra unranked audit alternatives can materialize more detached plans, and the existing opt-in
  audit does not separately reserve all temporary or returned plan-object heap. Existing reusable
  `PackedCostEstimate` metric-array growth also has no separate allocation-point reserve. Neither total JVM heap nor
  all callback work is claimed as budget-accounted. Final cross-module selection, scoped formatting and independent
  review remain pending; unified phase 1 and all provider/execution and performance gates remain open.
  Slice A is an inert, source-backed join/statement-pattern occurrence and coverage contract; it does not activate
  provider discovery, selection, cache binding or execution. Its valid-region red, work-admission red, matching
  legacy pre/post controls and 106-test post-format core selection are preserved in
  `/private/tmp/rdf4j-phase1-region-slice-20261108-3f894nxg/`. Unsupported region operators remain unclaimed, and
  root has reviewed and accepted this limited foundation. Before any runtime activation or expansion, replace its
  independently supplied authorities with one authenticated planner-owned budget, cost arena and session-generation
  boundary. A coverage proof must retain its actual root and covered-region identity so another subregion with the same
  external boundaries cannot reuse it; quote vectors must also authenticate the arena that owns them. Slice A alone
  establishes none of those later ownership contracts.

  A separate same-store SOCIAL_MEDIA q11/q12 regression fully drained the expected exact RDF4J bags and then
  reproduced `PackedMemoInvariantException` warnings on native-enabled cache replay; its in-repository red and
  command are preserved under `/private/tmp/rdf4j-social-cache-repro-20261108-f4yoaddv/actual-red/`. The private
  same-invocation ownership trace in that bundle's `private-diagnostic-pass2/ownership-extract.txt` proves that
  deterministic fallback remained `INCOMPLETE_WORK_LIMIT` and produced a recipe over a separately encoded fallback
  query, while publication paired that recipe with the original planning query. The exact cache hit later consumed
  that mismatched pair. Before runtime provider or descriptor wiring, design and review a focused ownership repair
  that preserves the actual recipe owner separately from the planning/query template, keeps incomplete status and
  eligible incomplete caching, and covers exact replay and affected family/structural rebinding. Root approved that
  repair design. The new core exact-replay regression reproduced payload 39 before the repair, and a same-invocation
  witness showed one statement-pattern opcode selecting different actual RDF patterns and duplicate bags in the two
  arenas. The candidate repair now carries each recipe's actual query owner separately from the planning template,
  gates family/checkpoint use on owner and concrete-invocation compatibility, and retains exact incomplete replay.
  Raced-hit and completed-flight paths are covered with latches. The original SOCIAL q11/q12 warning-and-bag test is
  green, and the post-format targeted core selection passes 316 tests. Two existing provider-checkpoint controls now
  use the previously measured 55-unit provider-backed stage, assert seven provider calls and detached evidence, and
  retain their changed-evidence/corruption assertions. Evidence is under
  `/private/tmp/rdf4j-cache-owner-repair-20261108-2f604rfl/`; root reviewed and accepted that cache-owner repair.

  The broader LMDB selection passed 45 of 46 tests. Its pipeline aggregate-budget hot-hit failure also reproduced at
  the same assertion in an isolated, hash-audited copy with the pre-repair packed sources. The deeper cause is not
  established; the failure is retained under that bundle's `final-lmdb-dependent-red/` and
  `pipeline-counterfactual/evidence/`. This repair does not claim phase 0 or phase 6 completion or native execution
  merely from an enabled flag.
  The phase-1 B/C review candidate now binds each region factory to the exact planner work/memory session,
  packed query, memo vector arena, and a revocable generation. Proofs retain the covered root and occurrence edges;
  quotes, boundary vectors and nested results require the issuing proof and actual boundary edge. The same-budget
  foreign-memory red and the real provider cleanup-lifetime red are retained under /private/tmp/rdf4j-phase1-bc-20261108-qleCkGY3/evidence/.
  A real positive provider created and composed a quote under the shared owner, and the existing zero-optional
  nested query bag control stayed green. Work, deadline and memory refusals are separately classified; the factory
  does not open an independent quota or refund retained-byte high water. The full focused core selection passed
  382 tests after formatting, and the exact three original scalar/zero-work controls passed before and after.
  Real detached-checkpoint replay also exposed a provider exception being swallowed as corrupt state; explicit
  structural rejection and a protected provider boundary now preserve the original error, cancellation, and close
  suppression identities without retrying the provider. The matching reds and greens are in the same evidence tree.
  The focused LMDB selection passed 49 of 50; its single hot-hit failure matches the frozen pre-repair counterfactual. No physical strategy discovery, selector, cache/binder activation, execution handoff or
  candidate timing is claimed; phases 0 and 6 and the broader phase-1 obligations remain open.

  The phase-1 review follow-up validates cached producer mappings, population and state references, context and
  input-event identities, and replay phases before provider callbacks, and structural child counts before interning.
  Both direct resume and cache refresh now reject the tested corruptions through the typed fresh-memo path and
  replace the revoked region generation. Composition admits its bounded boundary traversal before inspecting
  boundary handles. Callback, binding and cleanup failures preserve the exact original throwable, including when
  close throws that same object; a checked throwable also closes its session and releases the cache flight.
  Thirteen malformed-input variants failed through each entry point before the repair, while valid replay and
  current-provider error controls remained intact. The final broad affected selection passed 397 tests; the same
  three scalar/zero-work controls passed again. The LMDB selection passed 49 of 50 with the same previously
  reproduced hot-hit assertion. Evidence is preserved under
  `/private/tmp/rdf4j-phase1-bc-review-followup-20261108-5NU2jjK8/` for independent review; phase 1 remains open.

  The next phase-1 review candidate admits actual inspected dimensions, hash probes, array copies and rehash work
  through the same session owner in bounded steps. Row growth and rehash are staged before vector publication;
  partial work and admitted modeled memory remain charged on refusal. The retained-byte owner accounts modeled
  retained-capacity and reusable-scratch high water; growth reserves a capacity delta, not the full transient
  coexistence of old and new arrays, so it does not prove a bound on JVM heap or total live scratch peak. Legacy
  `PackedCostEstimate` scope is now retained:
  a one-occurrence LOCAL quote can be composed with complete boundary subtrees, while an INCLUSIVE quote requires
  no external boundaries. LOCAL boundary costs are accepted only at leaves. Coherent incompatible scopes report
  `UNSUPPORTED`; missing or inconsistent scope evidence reports `EVIDENCE_UNAVAILABLE`. A real nested control
  charges 7 + 20 + 2 = 29 once. A fused quote covering several internal occurrences while excluding external
  boundaries still needs a distinct coverage-scoped quote contract. Focused post-format qualification passed 57
  tests; this candidate is frozen for independent review before the broader regressions are rerun. Evidence is at
  `/private/tmp/rdf4j-phase1-bc-actual-scope-20261108-AB2zcC4K/`; no phase is declared complete.

  Root accepted the actual-work/scope review and relayed that independent Sol found no concrete blocker. Fresh
  qualification of the exact frozen 18-file identity passed 411 affected core tests and the three matched scalar,
  inclusive-cost and nested zero-work bag controls. The affected LMDB selection passed 49 of 50; its sole failure
  repeats the pre-repair counterfactual's same hot-hit assertion and line 359, whose deeper cause remains unproven.
  Source hashes matched before and after each selection. Reports, commands and source-identity audits are retained at
  `/private/tmp/rdf4j-phase1-bc-final-20261108-ZOCQh3KD/qualification/`. Fused coverage-scoped quotes and the
  remaining phase-1 obligations are still open; phases 0 and 6 are not declared complete.

  The inactive provider-contract review candidate adds a distinct covered-exclusive quantity declaration, bound to
  the actual source root, current root-operator callback, query/session generation, covered occurrence proof and
  boundary edges. Request, quote, boundary and vector handles are request/context-owned and non-reentrant; the first
  authenticated request refusal stops that attempt. A real provider callback composes covered work 9 with a complete
  boundary 20 for the true 7 + 20 + 2 = 29 control. Unknown correlation is MIXED or unavailable; unproved peak,
  retained-memory and latency composition remains unknown. Nested reuse is unavailable without owner proof that
  the exact child edge receives an equivalent input context. Consumer demand and snapshot facts remain unknown.
  The versioned detached value is a schema-validated declaration, not a selected or executable backend binding.

  The initial capability red incorrectly used a leaf callback for the outer root. After preserving the candidate,
  exact source hashes and all artifacts, individually guarded patches returned only this slice's behavior to its
  saved optional-UNSUPPORTED scaffold. The corrected real root-refinement test then failed as expected, and the
  preserved implementation was replayed afterward; this is recovered test-first evidence, not a claim that the
  valid red preceded the first implementation. A separate real reentrancy red preceded its guard. Recovery artifacts
  are in `/private/tmp/rdf4j-provider-recovery-inventory-20261108-xkdair_l/` and
  `/private/tmp/rdf4j-provider-recovered-red-20261108-jawv59f0/evidence/`.
  After scoped formatting, 434 affected core tests and the exact three pre-change scalar/zero-work controls passed.
  The affected LMDB selection passed 49 of 50; its sole failure is the same pre-repair counterfactual hot-hit
  assertion at `LmdbPipelinePlanCacheTest.java:359`, with the deeper cause still unestablished. Source hashes matched
  before and after each final selection. The modeled retained-capacity/scratch high water is not an actual JVM heap
  or complete live-scratch bound. Provider/backend conformance, derived-context nesting, discovery, selection,
  cache/binder activation, execution handoff, full-corpus/fixed-plan and performance gates remain open; phase 1 stays
  in progress and phases 0 and 6 are not declared complete.

  2026-11-09/10 continuation (Claude). Evidence root for this block:
  `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/`; specs and reviews are also copied to `.agent/designs/` and
  `.agent/reviews/`. The previous session's last run (`red-55`) had died on a full disk before Surefire ran; its test
  was re-run, observed red and fixed (`PackedSearchBudget.permitsUnrankedAuditMaterialization` also refuses once its
  owner has stopped). The pending scoped formatting was applied, and for the first time in this phase the whole
  `core/queryalgebra/evaluation` module was run rather than selections: 7,671 tests, 210 red. Committed HEAD
  `4ab0c09438` does not compile for `core/sail/lmdb` (it needs untracked files), so only the core module has a HEAD
  counterfactual: 177 of the 210 were also red at HEAD and 33 were caused by the uncommitted phase-1 work
  (`evidence/qualify-01-core-module/`, `evidence/counterfactual-head-core-4/`).

  Root causes of the 33 (`rca-seed-search.md`, `rca-unknown-lifecycle.md`): every provider callback was charged one
  unit on the search budget and a refused charge threw `PackedProviderWork.Exhausted`, which no search code caught,
  so a stop discarded already priced complete plans and returned the unpriced written-order fallback, and a latched
  retained-byte stop acted as a work refusal; the rest were tests pinning the old approach (isolated pricing of a
  bound input, NaN totals rejected by the winner table, a closed memory handle returning false) and were rewritten
  around their surviving invariants (`impl-core-tests/`). On the theme workload in LMDB's default mode (`auto`, 512
  units, constant unchanged) the unified currency made 148 of 176 plannable queries stop on the work limit, 138
  return an unpriced plan and 114 plans differ from exact mode (`measure-auto/per-query.csv`).

  The owner decided that provider work is a separate limit of the same budget owner and that each failing test is
  triaged as real bug or outdated test. Implemented in four independently reviewed rounds
  (`spec-budget-stop-and-provider-limit.md`; `impl-core-budget/`, `-r2/`, `-r3/`, `-r4/`; `review-core-budget.md`,
  `review-core-budget-r2.md`): `PackedPlannerLimits.providerWorkLimit` (unbounded at every existing call site) and
  `providerWorkUnits` published next to the search count; kernel drivers absorb the planner's own refusal at the
  phase boundary and publish the best retained state, including the sparse and scalar kernels; a retained-byte stop
  no longer refuses provider work; default-implementation pricing of a winner-less group runs in a
  mandatory-completion scope that optional work steps out of; an offer records its inputs as current only when it
  was fully priced, and is otherwise retried; a correlated-lattice transition is marked explored, and a lattice
  certified, only if no stop was absorbed while it ran, and a transition whose nested search absorbed a refusal
  publishes no state (it had published a child at the context-free price, 7.0 against 1,006.0 in the fixture);
  checkpoint replay charges the provider dimension and a refused replay restarts an ordinary search; unknown work
  stays unknown instead of throwing; a re-entrant scratch-estimate clobbering in
  `contextualizeEquivalentAccessPaths` (present at HEAD, exposed once the path became reachable;
  `rca-startup-once-q10.md`) is fixed with per-activation save frames and covered by `LmdbSocialQ10AutoPlanningIT`.
  The ten budget-pinned tests went green unedited. Final requalification: core module 7,718 tests with 176 red, all
  176 also red at HEAD; `core/queryalgebra/model` 249 tests green (`evidence/requal5-*`).

  After round 1 the theme measurement gave: searches complete 28 → 135, unpriced selected plans 139 → 11, plans
  differing from exact mode 114 → 51, and all 155 completed exact-mode searches keep their plan
  (`measure-auto/after1-per-query.csv`). Default-mode cold planning time rose from 13.9 s to 37.5 s over 182 queries
  (measured under heavy machine load) because the planner now plans instead of giving up; per query it costs about
  what exact mode costs, almost all of it provider callbacks. No measurement has been repeated after rounds 2–4.

  LMDB selections: 214 reds were triaged (`rca-lmdb-model.md`, `rca-lmdb-cache-feedback.md`); 204 were outdated
  fixtures (mock runtimes predating read ownership, stubs of superseded overloads) and were rewritten (`impl-lmdb/`).
  Two real pre-existing bugs were fixed test-first: a failed reset never made learned state unavailable (the tests had
  been committed without the feature) and a capturing lambda allocated per feedback publication. A new real-store
  test shows the planner still selects and prices the direct S,P,O lookup behind an opaque filter.

  Plan-decision cache. 155 of 176 theme queries end with comparison completeness UNAVAILABLE_COST (145 in exact mode;
  sources classified in `spec-written-fallback-pricing.md`), and this phase had gated publication, refresh and
  observation recording on a complete comparison, so none of them was cached. Implemented and reviewed
  (`spec-decision-cache-replay-and-capacity.md`, `impl-lmdb-cache/`, `-r2/`, `review-lmdb-cache.md`): replay-only
  admission (an authority on the LMDB certificate; served only on an exact hit; never refreshed, promoted, routed or
  aliased onto a ranked entry; refused for a search under a finite deadline or with a foreground-sampling wall-clock
  cut) and a capacity policy that cannot get stuck (admission may evict least-recently-used families for a key that
  has missed before, tracked by a bounded doorkeeper keyed on a revision-independent digest of the exact key's
  identity; dead replay-only families are reclaimed first). Cached entries were shrunk
  (`spec-shrink-cached-plan-entries.md`, `impl-lmdb-cache-r3/`): cache-owned templates are frozen so their charge is
  exact, and three explanation diagnostics are replaced by a placeholder on a cache hit unless the new store setting
  `retainCachedPlanDiagnostics` is on; EXPLAIN text and JSON of a cached plan are pinned byte-identical. On the theme
  run at the default 4 MiB cache: 25 families resident (was 6), 175 of 176 queries served by the third planning, no
  plan differences, real heap for 175 families 56 MB → 27 MB.

  Cache rounds 4–6 (2026-11-10; `impl-lmdb-cache-r4/`, `-r5/`, `-r6/`; reviews
  `.agent/reviews/GH-0000-decision-cache-review-r3-2026-11-10.md` and `-r4-`) are merged into the working tree.
  (1) A planning whose reads were cut by a wall-clock limit is no longer faked as a provider-work refusal: the core
  contract `PackedCostSession.reproducible()` (default true) is read last by the planner, and an unreproducible
  planning keeps its result for its own request but is neither published nor handed to waiters
  (`PackedPlanCache.decline`). LMDB wraps its session in `LmdbPlanningCutAwareCostSession` (cut count at the start
  of the planning scope against the count now); the refresh path and the uncached path open the same scope. Those
  two guards (review items E3, E6a) have no failing test because a cut cannot be produced deterministically
  in-repo. (2) Retired decisions leave a bounded tombstone (data generation plus a 64-bit digest of the build
  evidence). A build replaces a retired or unservable variant only if its data generation is not lower and its
  evidence snapshot differs; a build from an older pinned snapshot never displaces a servable replay-only family.
  The cache's evidence epoch is not an evidence clock and takes no part in these comparisons. (3) A ranked variant
  can serve only if its evidence is current or its champion has refresh work; a hard-invalid ranked family is dead;
  admission never evicts for a key retired under the request's evidence. (4) The identity digest mixes 64 bits per
  code unit instead of `String.hashCode`. (5) Charges are itemised and never below what is retained (version 352,
  variant 680, challenger posterior 120, appended alias 168 bytes). (6) `retainCachedPlanDiagnostics` is opted into
  only where plans are captured or explained; timing benchmarks keep the store default. Theme harness after round
  6 (plan-only, default 4 MiB): hits by attempt {2: 47, 3: 175}, 26 first-attempt capacity bypasses, 0 wall-clock
  cuts in 546 plannings, 0 plan differences (`impl-lmdb-cache-r6/harness-r6-default.summary.txt`). Round 6 had no
  independent full review; only its replacement rule (`mayReplace`, `newerThan`) was read by the coordinator.

  Requalification of the merged tree (`evidence/requal6-*`): core module 7,720 tests, 177 red — the 176 of
  `requal5-01` plus one method of `MaterializedReplayJoinIteratorCancellationTest`
  (`experimentalCancellationBeforeRowsArePublishedPreventsLeftCreation`), which is intermittent (reruns FAIL, PASS,
  FAIL) in files this work did not touch. Triage: outdated test. `TimeLimitIteration.interrupt()` has delivered
  the cancellation on a virtual thread since merge a9b477c32a, so the replay sees it at its next poll point, not
  when `interrupt()` returns; commit 4b2a72bc4c added delivery waits to three sibling tests and missed this one
  and four others. The five tests now wait for delivery before asserting (test file only; 18 of 20 red before,
  0 of 30 after; a mutated publication gate that ignores cancellation turns the corrected test red 10 of 10;
  `impl-matflake/`). Verified in a private copy with identical sources; not yet re-run in the working tree. `core/queryalgebra/model` 249 green. LMDB consumer selection 480 tests, 7 red
  (the same 7 as before, one former red now green). Decision-cache selection: LMDB 420 tests, 20 red (19 as in the
  implementer's final run plus one intermittent), benchmark 71/11 and benchmark-common 16/0 unchanged, evaluation
  165/0. `LmdbSocialQ10AutoPlanningIT` green. Two `LmdbPackedPlanCacheTest` methods
  (`multipleFreshAnonymousPathVariablesReuseExecutablePlan`, `boundedCorrelatedAnonymousQueriesReuseTheHonestIncumbent`)
  flip between runs of the same source.

  Final measurement on the merged build (`measure-final/summary.txt`; machine load 14–76 throughout). Planner:
  181 of 182 default-mode plans are identical to the build after budget round 1 and cold planning time at equal
  load is 1.02× (default) and 0.99× (exact); default mode has 135 complete searches, 12 unpriced selected plans and
  54 plans that differ from exact mode. Plan-only cache harness unchanged ({2: 49, 3: 175}). With query execution
  under the store's default configuration (native evaluation off, 2 ms sampling cap, adaptive evidence, 4 MiB) the
  decision cache is close to useless: (a) a completed evaluation moves the global LEO revision in 119 of 182 cases
  and every replay-only entry then fails the exact-evidence check — after an evaluation that moved LEO, 56 of 56
  resident replay-only entries missed, 33 of 33 hit when it did not move; LEO moved in every one of seven rounds
  for 121 queries, so it does not converge; (b) at 4 MiB a cycle over 182 queries evicts every family before its
  next turn (0 hits in every round; about 25 families of about 300 KB fit); with a 256 MiB pipeline cache the same
  schedule gives 30 hits per plan round (all 24 ranked queries, 6 of 151 replay-only); (c) ranked entries survive
  only because they are served stale with a detached refresh (166 stale serves per evaluation round), so the
  planning CPU is moved off the request, not saved; (d) under that load 60 of 181 first plannings were cut by the
  2 ms sampling cap and published nothing. The re-planning an unconditional hit would have saved is about 810–860
  s over seven rounds. Not yet established: whether the learned values change materially or only the epoch ticks,
  and whether the public `TupleQuery.evaluate()` path ever hits (a five-query check found a different key from
  the benchmark preparation path). One evaluation error is unexplained and under investigation: a cache-served
  plan failed native precompilation with `slot layout is already frozen` (PHARMA q9, native evaluation on).

  Follow-up measurement (`measure-final/followup/summary.txt`, copy in
  `.agent/research/GH-0000-phase1-continuation-2026-11-09/`): the feedback epoch ticks once per accepted
  publication (9,807 of 9,807); 44–54 % of them change no stored value; where a model value changes, the
  planner-visible value moves by a median of 6 % early and 2 % by round five and no key grows; 2,100 of 4,278
  value-changing writes are read by no later planning. Through the public API a replay-only lookup after a
  completed evaluation of the same query missed 142 of 142 times, and in rotation 0 of 116 hit. The benchmark
  preparation path keys plans as evaluation mode STANDARD with `read.planes=1`; the public API uses STRICT and
  `read.planes=3` (default `includeInferred`). A three-step proposal (revision advances only on real change; an
  entry depends on the learned facts its planning read; a tolerance for small movements) is in
  `.agent/designs/GH-0000-cache-validity-under-execution-feedback-2026-11-10.md` and awaits the owner.

  Defects found by these runs and their state (all uncommitted):
  - Read planes read more than once per request, and an idle value-cleanup pass registering a publication it then
    abandons: fixed test-first and merged (`.agent/reviews/GH-0000-read-plane-observation-2026-11-10.md`,
    `impl-planemask/`). The warm-hit test is green 10 of 10 in the working tree (3 of 10 red before).
  - `slot layout is already frozen`: pre-existing native-compiler defect (a root attempt declines after freezing
    its planner's layout and the row fallback reuses that planner); needs no cache hit; reproduces on an 8-triple
    store. Fixed test-first (`LmdbNativeDeclinedAggregateRootTest`, `impl-q9-frozen-layout/`) and merged; verified
    in a private copy only. The query shape now runs as a generic island instead of throwing; running it natively
    is a performance follow-up.
  - A timed-out evaluation is published to learning as completed (the interrupted iterator answers "no more
    rows"), and a node opened with outer bindings is recorded under its unbound key (exact fact 1 instead of 45,337
    for `?x lib:name ?y`, which changes LIBRARY q10's plan). RCA:
    `.agent/research/GH-0000-phase1-continuation-2026-11-09/rca-exact-fact-context-and-timeout-completion.md`.
    Fixed test-first over three rounds with an independent review between rounds one and two
    (`.agent/reviews/GH-0000-feedback-cut-and-bound-open-review-2026-11-10.md`, `impl-exact-fact/`) and merged: the
    outermost feedback wrapper records a cancellation request (returns false, forwards nothing) and an end of
    stream observed after a request or under an interrupt is published as cut; a time-tracking root now publishes
    completion at all; an exact fact, the logical-row observation, physical residuals and the lifecycle are written
    only when no open of the node received a binding for one of its own variables.
  Requalification after the read-plane merge (`evidence/requal7-*`): decision-cache selection 421 tests, 20 red
  (unchanged); consumers 481, 9 red (differences are the intermittent `LmdbPackedPlanCacheTest` methods); 46
  cleanup, GC and retirement classes 462 tests, 43 red against 44 red on the pre-change tree, 35 with identical
  messages and the rest intermittent on both sides, except
  `LmdbRetirementForeignWriterWindowTest#committedRetirementReleasesNativeOwnersBeforeWaitingForDictionaryReaders`
  (2 red of 4 after, 0 of 4 before); sampled again interleaved it is red 1 of 12 before and 0 of 12 after and
  green in three class runs on each side, so it is intermittent on both sides and the cleanup pre-check stands.
  `LmdbPackedPlanCacheTest#semanticExactZeroCertificateSurvivesPlanCacheHit` is red 5 of 5 alone on the pre-change
  tree and green inside its class: pre-existing order dependence.

  `LmdbPackedPlanCacheTest` triage (`impl-packedtests/`): all seven stable reds and the order-dependent method were
  outdated tests, none a product defect; rewritten test-only and merged (40 of 40 single-method and 4 of 4 class
  runs green in a private copy, each rewritten test red under a one-line product mutation; not re-run in the
  working tree). Causes: planning on a store whose startup maintenance was still moving the revisions (a planning
  that overlaps it is refused; six methods, plus a ninth currently-green one that got the same wait); feedback
  recorded under a bare execution-snapshot binding, which no longer authorises publication (two methods; now
  recorded under a live paired read source); binding values taken from the store's own value factory, which have
  no binding identity, so the request never uses the pipeline cache (one method; now external values); and the
  expectation that a decision survives a commit by stale-serve, which holds only for a ranked decision (one method;
  now a single-pattern query whose comparison is COMPLETE). Two methods
  (`freshlyParsedCorrelatedAnonymousVariablesReuseCompletePlan`,
  `boundedCorrelatedAnonymousQueriesReuseTheHonestIncumbent`) use a query with no anonymous variables, so they test
  reuse of a ranked decision, not what their names say. Product question raised by the triage: a request whose
  bindings come from `connection.getValueFactory()` bypasses the pipeline plan cache entirely
  (`LmdbQueryValueIdentity.Capture` refuses API-native values since 89e3e9c0e7).

  Requalification after the frozen-layout, `LmdbPackedPlanCacheTest` and feedback merges (`evidence/requal8-*`,
  working tree, first compile of every merged file): evaluation module 7,731 tests, 176 red (7,720 and 177 before;
  no new red, the cancellation test is green, 11 new feedback tests green); model module 249/0; decision-cache
  selection 421 tests, 13 red (20 before) and consumers 481, 2 red (9 before), the difference in both being seven
  `LmdbPackedPlanCacheTest` methods now green; `LmdbSocialQ10AutoPlanningIT` green. A feedback, learning and
  native selection of 102 classes (no earlier reference): 1,168 tests, 71 red, of which 61 match the implementers'
  pre-change reports (50 verbatim, 11 after masking numbers), 4 are red before with a different message, 1 was
  green in one pre-change run (`LmdbNativeAggregateDemandTest#physicalLengthConsumerCannotSilentlyIgnoreItsForcedRoute[2]`)
  and 5 have no pre-change evidence (four in `LmdbNativeDomainRootHashQuoteTest`,
  `LmdbNativePackedRootFactorTest#boundNativeRunExportsBorrowedRoot`); all ten are red 3 of 3 alone. Counterfactual
  on the snapshot from before the three merges (`evidence/requal8-cf-*`; the snapshot differs from the working
  tree in exactly the 16 merged files): the five uncovered tests and the differing-message ones are red 3 of 3
  there with the working tree's messages (the message differences are between a large selection and a
  single-class run, on both sides), so they are pre-existing. `physicalLengthConsumerCannotSilentlyIgnoreItsForcedRoute[2]`
  is red 2 of 6 on the snapshot against 4 of 4 on the working tree; sampled again in 12 strictly interleaved
  single-method pairs (`evidence/requal8-cf2-*`) it is red 10 of 12 on the snapshot and 12 of 12 on the working
  tree, the recorded route is `aggState` in every red run and no log line gives a reason: intermittent on both
  sides, not attributable to the merges (the snapshot's greens cluster at high machine load). Added to the
  pre-existing backlog. `LmdbPackedPlanCacheTest` is green in three class runs;
  `cacheIsOwnedByStoreAndInvalidatedByDataRevision` (body untouched by the rewrite) is red 4 of 4 alone and 3 of 3
  alone on the snapshot. RCA (`rca-cache-owned-alone/NOTES.md`): outdated test. It plans twice right after init;
  in a cold JVM the first planning (about 240 ms, reproducible, cached) overlaps the post-ready publication of the
  estimator providers and of the Frontier statistics service, which moves `leoRevision` and `frontierRevision`, so
  the second lookup finds the entry and correctly refuses it; in a warm JVM both plannings finish before the
  publication, and the later "invalidated by data revision" assertion then passes for the wrong reason. Fixed
  test-only by waiting for startup maintenance (`settle(store)`); in a private copy green 3 of 3 alone and red
  under each of two product mutations (data revision dropped from the key; a private cache per optimizer); in the
  working tree green 3 of 3 alone and 13 of 13 in two class runs (`evidence/requal9-settle-*`). Side
  observation, open: on an empty store the same query's comparison is COMPLETE before the Frontier statistics
  service is published and UNAVAILABLE_COST after it is published with no generation.

  Cache validity, step 1 (merged 2026-11-11, uncommitted). `LmdbOperatorFeedbackStats.planningRevision()` is now
  a version of the learned state planning reads: the sum of mutation counters, each advanced in the critical
  section of the mutation it versions (model posterior/calibration/censored-bound/decay/eviction changes; exact
  fact rows changed, a fact becoming current, any fact-bearing eviction; lifecycle store revision deltas across
  observations, commits and the publication-path decision, including its bookkeeping; legacy clock ticks one for
  one; typed ticks only while a clock-decayed legacy store holds state). A typed publication only ticks the
  observation clock. Spec `.agent/designs/GH-0000-cache-validity-step1-truthful-planning-revision-2026-11-10.md`;
  two independent review rounds, six findings, five fixed test-first (publication-time lifecycle bookkeeping
  unversioned; live-stamp read under the monitor; reset after a failed reset; a bump after its mutation; eviction
  of a fact readable only at the live stamp), one kept as a documented exclusion until step 2 (an eviction made by
  a planning's own lifecycle admission changes a sibling's regression limit). Tests:
  `LmdbPlanningRevisionVersionTest` (29) and store-level `LmdbNoOpFeedbackPlanCacheTest` (a second evaluation that
  only rewrites its exact fact leaves the LEO revision unchanged and the next lookup is `USE`; red before).
  Requalification in the working tree (`evidence/requal10-*`): no new red against requal8 and against the
  pre-merge copy; `LmdbPlanCacheCommitInvalidationTest#warmSpocOnlyGuaranteeFilterDropMustNotSurviveCommit` turned
  green (3 of 3 alone, red 3 of 3 before the merge).

  Constant-time "other applicability known" in `FrontierLearningModel` (merged 2026-11-11, uncommitted; Routine
  B). The two posterior maps are wrapped in `PosteriorCells`, which keeps a key count per applicability group
  beside the insertion-ordered map; the three whole-map scans per decision become one hash lookup, asked only when
  the exact key is absent. Same 251 tests before and after; a probe over gate outcomes, estimates, sidecar bytes
  and eviction order gave identical digests on both copies; six one-line index mutations all turn the new
  `FrontierLearningModelSiblingApplicabilityTest` red. On a full model (4,096 physical keys) a decision visited
  about 2,348 entries before and none after (17.4 µs to 0.34 µs, synthetic keys, loaded machine). Requalified
  with stable keys (requal11, below); a later workload measurement on the read log shows replay at about 1.2 % of
  planning time, against 21.7 % before.

  Direct measurement of validation by re-read (`measure-validity/summary-replay.txt`; caches off, theme workload,
  replay of the previous planning's reads before the next planning, then plan comparison). No wrong serve in any
  schedule. Served, with lifecycle `regressionLimit` excluded from the comparison: rotation 281 of 728 (39 %),
  same query repeated 299 of 544 (55 %), two plannings with no feedback in between 1,134 of 1,456 (78 %); with
  `regressionLimit` compared 141, 157 and 450, because that value depends on the order of lifecycle decisions
  inside one planning (a planning's own log replays to a different limit in 681 of 910 plannings). Tolerance
  adds 6 (rotation) and 34 (repeat) serves at ln 1.25 and 26 and 38 at ln 1.5, none wrong; ignoring gate-label
  differences without an estimate is also free of wrong serves up to ln 1.5 (rotation 345) and produces 2 at
  ln 2. Every divergence between consecutive plannings is preceded by a changed recorded read (logical value or
  presence, an exact fact becoming current, a learned filter surface, a cached sample that the previous planning
  itself wrote), by a 2 ms sampling cut on one side, or by a parser-random name; none has an identical prefix.
  3.5 % of plannings are not reproducible (all from the 2 ms live-sample cap, almost all in the first round).
  Replay costs 21.7 % of a planning, 82–86 % of it the whole-map scans removed above.

  Stable learning keys (merged 2026-11-11, uncommitted; Routine A; spec
  `.agent/designs/GH-0000-stable-physical-learning-keys-2026-11-10.md`, evidence `impl-stable-keys/`). Names the
  parser generates differently on every parse no longer reach learning identities: the planning `PackedQuery`
  carries, beside its object table, the plan identity's canonical ordinal of each such name
  (`canonicalNameOrdinals`, never read by execution, costing or the cache identity); the physical source identity
  writes it under its own token tag, the binding-layout fingerprint folds ordinals under a domain constant, and
  join factors order bindings of equal role and break ties by ordinal (`SemanticBinding.compareNames`: exact names
  first, in their old order). `Filter.getBindingNames()` now returns the argument's names followed by an EXISTS
  sub-query's in insertion order, because the hash order of that set reached the logical key. Names that label
  constants stay exact, so queries without generated names keep their bytes, with one accepted exception: every
  `FILTER EXISTS` relation gets a new logical key once (its numbering followed string hashes before); blank-node
  `[ ]` and unprojected-BIND physical and event keys also change once. Two independent review rounds: no wrong
  sharing; 25 query shapes × 15 parses and a renaming sweep over 14 shapes give 0 key mismatches (before: every
  shape with a path, HAVING or EXISTS differed on every parse). requal11
  (`evidence/requal11-summary.txt`): new tests 56/56; `core/queryalgebra/model` 251/0, `core/queryparser/sparql`
  503/0, `core/queryalgebra/evaluation` 7731 with the same 176 reds as requal8, `compliance/sparql` 3612/0 in both
  trees, the requal10 LMDB selections and 11 further classes with no case red only after the merges (pre-merge
  base `trees/base-flm-index`). The byte-compatibility pins cover a plain query, a three-pattern join, OPTIONAL,
  GROUP BY (golden values captured on the pre-change copy and identical after) and `FILTER EXISTS` (the changed
  value, pinned as the accepted deviation); five one-line mutations each turn the matching pin red; 17/17 in the
  working tree. Open: none of the pinned shapes produces an event key; the store-level rebind route is covered
  only by the core unit test.

  Cache validity, step 2 phase A — record and replay (merged into the working tree 2026-11-11, uncommitted, 25
  files; spec `.agent/designs/GH-0000-cache-validity-step2-validation-by-re-read-2026-11-11.md`, evidence
  `impl-readlog/`). Learned reads are recorded at their owners into a planning-scoped log and replayed without
  side effects; nothing consumes the log yet. On the theme workload 179–180 of 182 logs replay as identical right
  after their own planning and a replay costs about 1.2 % of planning time. The independent review's nine fixes
  (R1–R9 in the spec) are merged. Requalification (`evidence/requal12-summary.txt`) was wound down early for the
  upstream merge below and is incomplete: the baselined selections and the 68-class owner selection show no new
  red against a pre-merge copy; one new test is intermittently red
  (`LmdbPlanningReadLogStoreTest#aLearnedLogicalEstimateRecordsTheMappedBoundsCheckAndReplaysIt`, a coverage
  prerequisite, also red on the pre-A2 sources; cause and fix in the Frontier rebuild paragraph below); in the
  whole-module comparison 79 red cases in
  29 classes are unclassified (no reruns), and a root cost-estimate difference printed by
  `LmdbPackedPredicateRangePlanningTest` (20.0K in the working-tree chunk run, 12 in the base runs) is unexplained.
  These stay open until classified; they are not a baseline.
  Classification on frozen copies before and after phase A (`evidence/classify14-summary.txt`, 54 runs, hashes
  stable): the 20.0K estimate is intermittent on both sides (4 of 8 runs before, 2 of 8 after), so it is not from
  phase A; `LmdbSailStoreTest` has the same 11 red cases on both sides; of 168 red cases in the 27 other classes,
  109 are the same in both, 37 intermittent in both, 13 red only after and 8 red only before (all but two of the
  one-sided cases at 1–2 of 3 runs in timing-sensitive concurrency tests on a loaded machine). The two
  deterministic differences were read: `LmdbSailStoreTest#defaultApproveAllSubmitsOneSmallIngestBatch` (fast
  "intervening native writer" error before, 15-minute hang after) swaps the store's value store for a Mockito
  spy, a field-by-field copy, while post-ready maintenance still works on the original; and
  `LmdbEstimatorPublicationAdmissionTest#delegateConstructionFailuresReleaseAdmissionAndGuard` (failing parameter
  moved from [2] to [1]) samples free reader slots without waiting for background maintenance. Working
  hypothesis for both: tests racing post-ready background maintenance, symptom decided by timing; root cause,
  production-deadlock check and test fixes in flight (`impl-bgrace/`). No evidence of a phase A regression.

  Cache validity, step 2 phase A2 — compact log (merged into the working tree 2026-11-11, uncommitted, 17 files;
  evidence `impl-readlog/`). A read that found nothing is stored as a 128-bit digest; with enforcement off the
  lifecycle reads collapse into one guard; pass ratios and learned surfaces compare exactly. Incremental log bytes
  on the theme workload: 201.8 KB → 3.4 KB median (p90 9.2 KB); as charged by the cache, median 9.2 KB, max
  105.5 KB. Independent review: no wrong verdict in about 2,700 serves compared with fresh plannings; the compact
  form disagrees with full keys only towards "changed". Requalification (`evidence/requal13-summary.txt`, 67 runs
  against the pre-merge copy, hashes stable): no case red 3/3 in one tree and never in the other; 13 mismatching
  cases in 8 classes are intermittent or red in both. The within-tolerance verdict is restated in the spec and
  confirmed by the owner ("keep, measure it"): it may differ from a fresh planning at an unguarded discrete
  planner rule (found: the hash-join 256-row floor); the audit and the B measurement report that rate.

  Cache validity, step 2 phase B — the pipeline cache serves an entry whose log still replays (merged into the
  working tree 2026-11-11 evening, uncommitted, 21 files; evidence `impl-phaseB/`, switches
  `rdf4j.lmdb.planCache.readLog.validation` default on, `.audit` default off, `.maxBytes` default 256 KiB). Three
  implementation rounds and two independent reviews: a log never costs a cached family (logs live in spare room
  and are dropped first), an in-flight refresh or publication survives a concurrent log drop, serve without
  re-stamp, the audit plans against a private empty packed cache. Capacity is identical with the switch on and off
  (16-query rotation 16 families / 48 hits / 0 evictions; 24-query 23 / 23 / 49). Measured limit: 6 of 16 and 16
  of 23 resident plans have no log because their planning was answered by the packed plan cache; such a plan
  takes the old path at its first learned-state change. Open: the uncached packed entry point chose a different
  plan than a cache-miss search for 8 of 47 audited requests under the experimental-full rollout; the B
  measurement (hit rates on/off, verdicts, audit mismatches, within-tolerance rate, plans without a log by
  reason) is not run yet. Requalification of the merge with the Frontier fix (`evidence/requal14-summary.txt`,
  completed 2026-11-12 01:23, all counted runs after the semaphore limit was raised, source hashes stable): 14
  selections against the exact pre-merge snapshot, 27 mismatching cases rerun standalone 3 times per tree, none
  red in one tree only (6 red in every observation of both with different numbers or first lines, 6 red in both
  with one green on one side, 5 intermittent in both, 10 red once in a selection run and 0/3 standalone in both).
  One-sided: the two outdated plan-cache tests above (WT-only reds, since rewritten) and green new tests. With
  `readLog.validation=false` no case present in both trees differs; 55 new revalidation tests rely on the default
  and are red with the switch forced off from outside (expected for tests of the feature; 1 sets it itself). The
  formerly flaky read-log fixture: 0 red in 10 standalone runs. Whole-module comparison
  (`evidence/module15-summary.txt`) still to run, in class batches.

  Redundant Frontier rebuild after acknowledgement (merged with phase B, uncommitted, 5 files; note
  `.agent/designs/GH-0000-frontier-redundant-rebuild-after-acknowledgement-2026-11-11.md`, evidence
  `impl-frontier-ack/`). A publication's own journal acknowledgement advanced the snapshot epoch, so an already
  requested rebuild built and published again (second planning-revision move, re-keyed learning, and the
  intermittent `LmdbPlanningReadLogStoreTest#aLearnedLogicalEstimateRecordsTheMappedBoundsCheckAndReplaysIt`).
  The service now records the acknowledgement's own commit and a snapshot is covered when only such commits,
  without a gap, follow the generation. Equal journal sequence is not used as proof: the initial NONE run import
  writes statements without journaling them. Open, not reproduced: the delta path and lease acquisition do treat
  equal sequences as "no change", so a base built for an empty store may be served after such an import.

  Upstream merge (2026-11-11): `origin/main` at `1acc280c48` merged as `31fade148e` (12 commits, 35 files, no
  conflicts; `LmdbSailStore.java` combined with its uncommitted local change by a clean three-way merge); root
  quick build green; 13 of 14 touched classes green, `LmdbSailStoreTest` red in 7 cases that were already red
  before the merge (classification on frozen copies in flight, `evidence/classify14-summary.txt`).

  Interaction of the two merges (2026-11-11 night, verified in private copy `trees/f10fix`, not merged while the
  requalification hash-checks the tree; evidence `impl-f10fix/`): two `LmdbPlanCacheRevalidationStoreTest` cases
  relied on `rebuildFrontierStatistics()` publishing on an unchanged store, i.e. on the removed defect. Rewritten
  test-side (a held scheduled refresh and a real commit); red before, green after, three mutations caught. Product
  observation recorded as open design item in the step-2 design note: between a commit and the Frontier
  publication that covers it, planning uses the conventional cost session, which creates no feedback contracts,
  and the Frontier planning revision moves twice per commit (by reading and red-then-green, not traced).

  Tests racing post-ready maintenance (2026-11-12, private copy `trees/bgrace`, test-only, not merged; evidence
  `impl-bgrace/report.txt`, backup `tmp-evidence-backup-2026-11-12-part5-bgrace.tgz`). Post-ready maintenance has
  run asynchronously since 52992a2ee8; a Mockito `spy()` swapped into `LmdbSailStore` after `init()` is a field
  copy whose writer-admission and trusted-transaction state diverges from the original while maintenance still
  writes (three outcomes reproduced deterministically: green, "intervening native writer", hang). 12 red cases in
  `LmdbSailStoreTest` and `LmdbEstimatorPublicationAdmissionTest` fixed without production changes (factory-seam
  subclasses instead of spies, fixture awaits maintenance, outdated pause points moved, two tests that pinned the
  estimator-discard behaviour abandoned in 8475bbcd75 rewritten to the current contract); 0 red in 10 class runs
  each. No product deadlock found. Open product bug: `testOrderByLmdbIndexPreservesJoinOrder` returns rows out of
  `STABLE_INDEX(?type)` order (4 of 10 class runs) when a query overlaps post-ready startup; one such execution
  ran `factorizedRows` with the label pattern first; working hypothesis: the order survives the removal of the
  Order node only by accident of costs because the factorized proposal passes `requiredPrefixMask = 0`
  (root cause and deterministic reproducer in flight, `impl-bgrace/ordbug/`). Not triaged: 7 other admission-test
  reds (5 at 10/10). Semaphore incident: the POSIX semaphore pool ran out on 2026-11-11 ~20:50 (leaked by killed
  LMDB JVMs); limit raised by the user to 20000; all agents now probe before each run and never SIGKILL a JVM;
  whole-module runs are split into class batches under the 1800 s fork timeout.

  Merged into the working tree 2026-11-12 01:30 (uncommitted): the rewritten plan-cache test and the two
  test-race files. Post-merge in the real tree (`impl-pomheap/`): `LmdbPlanCacheRevalidationStoreTest` 15/15;
  `LmdbSailStoreTest` 71 cases ×3, only `testOrderByLmdbIndexPreservesJoinOrder` red (3 of 3); admission test
  fixed cases green, one more red of the untriaged reader-slot family seen once
  (`observedIterationFailureRestoresItsReadResourceContext`, 126 vs 127). Build change by owner decision (no
  `-Xms`, `-Xmx` 8 GB): the root Surefire and Failsafe argLines and the lmdb `frontier-vector-tests` argLine went
  from `-Xmx16G` to `-Xmx8G`; pre/post green with identical counts through each, fork command lines show the
  flag, no tracked pom sets `-Xms`; lmdb unit forks stay at 4G. Agents run Maven itself at 4G. Seen once in a new
  workspace, not investigated: `core/sail/lmdb` verify ends with a japicmp "Class not found:
  ...FederatedServiceResolver" after green tests.

  Ordering bug root cause (2026-11-12 01:50, `impl-bgrace/ordbug/report.txt`): deterministic reproducer on a
  settled store (`LmdbStableIndexOrderFactorizedJoinTest`, red 10/10; private copy). `LmdbOrderByOptimizer`
  removes the Order node and leaves only `setOrder` on the anchor pattern and a left-operand/merge-join-off
  directive; `factorizedRows` (`MultiJoinPlan.sinkUnconsumedPatterns` keeps the smaller static estimate first)
  then reorders. The startup-window flip was the rdf:type leaf priced at the Cascades default 10000 because
  `LmdbPackedCostModel` returned NaN (why is open). Same shape on upstream main (be1f4ad254, by reading).
  Decision: follow the existing convention — a pattern with a requested order is an order contract and
  strategies that cannot preserve encounter order decline (factorizedRows, and parallelPipelines / batch hash
  join if a red test demonstrates the break); pinned-marker option deferred. Implementation in flight
  (`impl-bgrace/ordfix/`). Mutation checks of the rewritten tests: the estimator one is caught; the
  snapshot-coordinate one survived (its park point holds the barrier in both shapes) and is being strengthened.

  Ordering fix merged into the working tree 2026-11-12 05:57 (uncommitted; `impl-bgrace/ordfix/report.txt`,
  `changes.diff`, `me-test.diff`). `MultiJoinPlan.containsOrderedScan()` (moved from adaptive filter placement)
  now makes five strategies decline with reason `ordered-scan`: `factorizedRows`, `parallelPipelines`, the batch
  hash join (`LmdbNativeHashJoin.tryPlan`), and — found by the implementer, each shown returning rows in the
  wrong order first — `orderedDistinct` and `prefixRun` (DISTINCT over a join / over one ordered pattern).
  `LmdbStableIndexOrderFactorizedJoinTest`: 11 of 13 red on the pre-fix copy, 13/13 after; five mutations (one
  per decline) all caught; `LmdbSailStoreTest` 10 class runs 71/71 with `testOrderByLmdbIndexPreservesJoinOrder`
  0/10 red (4/10 before). Two-copy comparison (84 classes, 1,301 cases; plus 41 order-related classes, 708
  cases): no status change caused by the fix other than that test red→green. The hash-join break needs the
  ordered pattern as build side (settled 100 vs 600 rows, `rdf4j.lmdb.nativeHashJoin.minRows=0`). STABLE_INDEX is
  the only production source of a requested order (`QueryJoinOptimizer` never sets one under LMDB: the standard
  pipeline wraps the source in `OrderBlindTripleSource`, the Cascades pipeline has no `QueryJoinOptimizer`), so
  only STABLE_INDEX queries lose these strategies; that cost is unmeasured and the declines are unconditional
  (an order-safe hash join whose probe is the ordered scan is declined too). Open, by reading only: wildcard
  existence round owner, left-join batch/factor paths under OPTIONAL (unforced test green), the bushy runtime
  hash build (not a `MultiJoinPlan`). The strengthened snapshot-coordinate test (second pause point in
  `latestFrontierMutationSequence(Txn)`) is red 3/3 under its mutation and green 13/13 without.
  Pre-existing, untriaged: 149 cases red on both the pre-fix and fixed copies in 24 `LmdbNative*` /
  `LmdbFiniteValues*` classes (`impl-bgrace/ordfix/s6-red-both.txt`); the whole-module comparison in flight
  (`evidence/module15-r2/`) produces the complete list for triage. Hit-rate measurement: the revalidation-on
  theme run hit OutOfMemoryError at the 4 GB fork heap (03:45); the matrix was restarted at 8 GB. Not a
  validation effect: heap after GC peaks at 6.5 GB in the theme rotation with validation off and on alike.

  Phase B measurement result (2026-11-12 10:05; `impl-phaseB/measure-b/b-measurement.txt`, `-tables.txt`,
  `-within-tolerance.txt`; 12 clean runs at 8 GB fork heap, shared machine at load 4–7, peaks 19, plus 4
  diagnostic runs). Rotations: T = theme by theme, C = all themes cycled; two runs each of off / on / audit.
  - Audit: 0 mismatches in 471 identical-verdict serves (criterion 1 met).
  - Hit rate after a learned-only change: 0.6–1.2 % off, 18.7–23.8 % on; on the explain right after a query's
    own evaluation 1.3–1.8 % → 40–43 % (criterion 3 met). All lookups: T 0.37–0.40 → 0.48–0.49, C 0.37 → 0.46.
    Explain wall time per run: C 162/209 s off, 130/137 s on; T 177/172 s off, 126/172 s on. Replay median
    0.07 ms, p99 < 0.6 ms (one 152.7 ms outlier) against ~105 ms full planning.
  - Verdicts per on run: identical 109–135, within tolerance 73–86, changed 220–236 (first differing read:
    logical ~90, learned surface ~57, exact fact ~35, event ~29). Replay failures, capture mismatches and lost
    re-stamps: 0.
  - Within-tolerance serves that differ from a fresh plan: 11 of 314 (3.5 %), five distinct queries; two differ
    only in the physical fingerprint, three in join order / FILTER-BIND placement. Not the hash-join row floor.
    Whether the served plans run slower was not measured (the fresh planning did not price them).
  - Criterion 2 (no query consistently slower with validation on): not met as literally written. LIBRARY q0/q1/
    q7/q8 round 1 were 2–3× slower in both T-on runs, but those evaluations missed the cache and planned afresh
    in every configuration, both T-audit runs (validation also on) and 3 diagnostic on-runs did not show it, and
    recorded plans were identical on and off. Of the 21–27 evaluations per T run that executed a revalidated
    plan, all used the off runs' plan structure and one was 12 % slower. Cause of the LIBRARY case not
    identified; validation not shown to be it.
  - Capacity is the binding limit: the 4 MiB pipeline cache holds 6–13 of a theme's 13 families (24–25 small
    ones), evicts 474–669 families per run, and most read logs are dropped for slack (T: 325–379 of ~680
    variants), so 141–181 learned changes per T run end in a miss for want of a log.
  - Pre-existing, all configurations: 72 WARN "Packed cascades planning failed … Unsupported Cascades operator"
    per run.
  Owner assessment: revalidation stays default on (correctness criterion met, benefit shown, no slowdown
  attributable to it). The within-tolerance rule is put to the user with the 3.5 % figure. Cache capacity is
  now the main lever and needs a decision.

  Whole-module comparison after the merges (2026-11-12 16:30; `evidence/module15-r2/summary.txt`; WT snapshot
  `72703332…`, 7872 files, vs BASE `b6fc983a…`; forks at 4 GB): no stable green→red in WT. Eight cases are stably
  red→green (six in `LmdbSailStoreTest`, `LmdbPlanningNullValueRegressionTest#valuesWithUndefRows…`,
  `LmdbStaleAnswerAfterInsertTest#insertIsVisibleAfterCrossQueryWarmup`). Two WT-only intermittents on thin
  evidence (2 of 4 red in WT, 0 of 4 in BASE): `LmdbNativeParallelPipelinesTest#externalRootChunkKillSwitch
  KeepsScalarWorkerFallback`, `LmdbRetirementPairedReaderAdmissionTest#dictionaryPressureReleasesStatementReader
  BeforeWaiting[1]` — 10 runs per tree in flight. The ten WT-only classes (114 tests) are green. Post-merge check:
  `LmdbStableIndexOrderFactorizedJoinTest` 13/13, `LmdbSailStoreTest` 71/71 ×3. Not run: `QueryBenchmarkTest`
  (exceeds the 1800 s fork timeout), `LmdbStoreMultiRepoTest` (OutOfMemoryError at 4 GB and intermittently at
  8 GB, both trees).
  The module is far from zero red: WT 19859 tests, 1024 failures, 347 errors; 1349 cases in 273 classes are red in
  both trees. 266 of those classes are tracked, unmodified test files, nearly all added in October–November
  2026. Largest message clusters: 121 AssertJ "Expecting actual:" (30 classes), 80 "the ordinary compiler must
  admit this statement shape" (`LmdbNativeStatementTermRoleTest`), 47 NPE on `watchedNamespaceHashes`, 43
  "Missing store.properties for an existing ValueStore dictionary", 70 "Unsupported LmdbStore version", 37 NPE
  on `LmdbFrontierReadSelection.matchesSource`. Next discriminator in flight (`evidence/module15-r3/`): the same
  273 classes on a clean archive of HEAD 31fade148e, to split committed reds from reds caused by uncommitted
  work, clustered by message. Cause-level triage (real bug vs outdated test) follows per cluster.

  Follow-up (2026-11-12 18:20; `evidence/module15-r3/summary.txt`): the two WT-only intermittents are not
  WT-specific at 10 runs per tree (kill-switch case WT 2/10, BASE 5/10, "offered and declined
  ('snapshot-unavailable')"; paired-reader case WT 0/10, BASE 1/10). The committed HEAD does not compile on its
  own: commit 4ab0c09438 ("optimizations", 2026-11-08, on the remote branch) committed `TripleStore.java`
  referencing the untracked `LmdbIndexAccessFeatures` and an uncommitted `LmdbRecordIterator` constructor; the
  merge 31fade148e inherited that. The committed-vs-uncommitted split is therefore being taken at the previous
  first parent 2007a9fdbb (`evidence/module15-r4/`). Cause triage started for the store-format / direct
  `ValueStore` family (~200 cases; `triage-storefmt/`).

  Store-format family triaged (2026-11-12 19:00; `triage-storefmt/report.txt`, `diffs/ALL-proposed-fixes.diff`;
  private copy, not merged): 183 cases + 2 adjacent, six root causes, none from the 11-11 merge (onsets 10-23 to
  10-31, by reading code at those commits). 150 + 2 proven red→green in the copy.
  - R1 (67, wrong fixture): `ValueStoreIdCacheTest` subclass hooks run during constructor-time core-datatype
    seeding before its field initialisers; fixture now inert until constructed and stamps format 7 properly.
  - R2 (real product regression, performance only; was hidden behind R1): the per-batch absent-hash memo was
    lost in merge 17f5004874 — `findIdsCaptured` probes every absent namespace prefix per IRI instead of per
    batch on format-2 stores (12400 probes for 400 IRIs, bound 1600). Fix accepted with an owner addition: the
    memo is cleared whenever `nativeCommitGeneration` moves inside the batch.
  - R3 (44), R4 (36+1), R6 (3), R7 (1): fixtures open stores with unstamped `StoreProperties`; the refusal is
    deliberate (165bba16a8, 87ac925b4a, 89e3e9c0e7). Fixtures now stamp through
    `LmdbOwningTestStoreMetadata.open` / `initializeProperties`.
  - R8 (unmasked by R4): `LmdbValueGcRecoveryTest` injected its fault in a method the recovery path no longer
    calls (seam moved); `LmdbConstructorCleanupTest` pinned an exception type in the status text that the
    product's `"FAILED: " + getMessage()` convention (three sites) does not produce.
  - R5 (33, open): `LmdbHistoricalBulkDomainCompatibilityTest` (32) and `LmdbLegacyEstimatorV2CompatibilityTest`
    (1) use fixtures in format 6, which the product rejects on purpose (75753ac897). Retire or re-capture is a
    user decision; analysis of what they still protect is in flight.
  Merged into the working tree 2026-11-12 20:10 (uncommitted; `triage-storefmt/merge/`, `merge-item6/`; verified
  in the private copy on identical formatted files: family + 18 blast-radius classes = 946 tests, 33 errors (R5),
  3 failures listed below; `ValueStoreIdCacheTest` 67/67 ×3). 18 test files and `ValueStore.java`:
  - R2 as described, plus a re-read of the writer handle after the in-row `resizeForAllocation` (the old code
    would probe the consumed transaction after a resize checkpoint). The commit-generation guard has no test:
    the batch reserves its whole footprint first and no seam forces a mid-batch commit.
  - `ValueStore.getId`: the own-identity shortcut is skipped under a borrowed read context (merge a9b477c32a
    dropped upstream's handling; a lookup under a borrowed older snapshot returned a current-revision ID that
    names a retired value there). Existing failing test `testLazyBorrowDoesNotReadReusedIdFromOlderDictionary`
    [1,2] red→green. Same condition the reverse-ID cache path already uses.
  - Outdated tests: three `ValueStoreTest` `…HashCodeDoesNotInitializeAfterRestart` duplicates and one stale
    assertion removed (upstream rewrote them as `…WarmsTheRuntimeCacheAfterRestart` in 52992a2ee8; the merge
    kept both); `testDatatypeCacheKeepsOldAndFreshMeaningsOfAReusedIdSeparate` sets
    `setAsyncValueCleanup(false)`; `LmdbSynchronousIngestTest#rebuildSnapshotCountsPriorCommittedMutationsOnce`
    [1–6] rewritten to the quad-synopsis contract (an overtaken rebuild publishes its pinned snapshot, serves no
    usable evidence, requests a refresh; the next rebuild counts the commit exactly once);
    `LmdbSailStoreOverlayStartupTest#durablePostReady…` performs the owner's load-and-discover step.
  Still red in these classes:
  - `LmdbBoundedStartupTest[3]` LEGACY — real product bug: `LmdbLanguageTagVariantMerge.mergeIfRequired` plans
    (scans all language literals) on every format-2 open, ~4.4k cursor reads + ~400 seeding probes against a
    budget of 1,000. Needs a completion proof tied to the trusted native transaction chain; owner design pending.
  - `ValueStoreTest#testLegacyRebuildMapFullAbortsAndRetriesAfterGrowth` — hybrid from a merge (map exhausted by
    the startup commit before the recovery under test); fixture reshape in flight.
  - `LmdbBoundedStartupTest[7]` — intermittent, unexplained; attribution probe in flight.
  - R5 — being converted to fixtures generated at test time (domain-less format-7 bulk store; format-7 store
    with the authentic v2 estimator files grafted), with a mutation check each. The captured format-6 resources
    become unreferenced; their removal is for the user.

  R5 converted and merged 2026-11-13 00:40 (`triage-storefmt/round3/merge-r5/`): both tests now generate their
  stores at test time (domain-less format-7 bulk generation via `LmdbBulkLoader` with guarantees DISABLED, with
  setup assertions that it really is domain-less; format-7 store with the authentic v2 estimator files grafted);
  32/32 and 1/1 green, mutation checks red (16 of 32; 1 of 1). The historical test's domain reads now run inside
  a pinned planning view (required since a9b477c32a). Eight `lmdb-compat/v6-pre-domain-*` resources are no
  longer referenced (kept; removal is the user's call); the legacy-estimator zip's store entries are unused.
  `testLegacyRebuildMapFull…`: no fixture can pass all three attempts — the second attempt fails in the
  reconstruction's own first commit, whose MDB_MAP_FULL is not reported like the batch path's. Decision: product
  change (uniform "auto-grow is disabled" failure with `RefCountRecoveryMapFullException` cause), in flight.
  `LmdbBoundedStartupTest[7]`: reads are stable (385 of 1,000); the fixture's 1,100-ID queue is smaller than the
  4,096 bounded pass and races the async coordinator; fixture fix in flight. `LmdbBoundedStartupTest[3]`: design
  written (`.agent/designs/GH-0000-legacy-language-reconcile-bounded-startup-2026-11-13.md`: a clean-close
  witness on the value environment's transaction id lets a format-2 open skip the reconcile scan, conditional
  on proving that this build's own writes never need the next open's repair); implementation in flight in a
  private copy (`impl-langwitness/`).

  2026-11-13 02:00: `LmdbBoundedStartupTest[7]` fixed test-only and merged (`triage-storefmt/round4/
  merge-retireq/`): the case pins the synchronous post-ready pass (added 52992a2ee8, before async cleanup
  existed), so it now disables async cleanup, queues 4,096 + 1,100 IDs and asserts exactly 1,100 remain; 10 of 10
  green, unbounded-pass mutation red, startup reads unchanged at 388. Map-full test: even with the product
  wrapping, a third reopen fails in the store's own bootstrap commit (a failed capacity-bound reconstruction
  consumes the headroom), so the reopen-three-times shape is unsatisfiable. Decision: three attempts on one open
  maintenance store (Option A) plus the wrapping of the two reconstruction metadata commits, pinned by a second
  test (fail, reopen, fail structurally, grow, succeed); in flight (`round5/`). Three further reds seen in the
  blast radius (`LmdbCrashRecoveryTest.nativeIndexRewriteSurvivesCrashBeforePropertiesSave`,
  `LmdbValueGcOwnershipTest.legacyCloseRetainsUnusedTermsWhileObjectIndexBuildIsDelayed[1,2]`) are with the same
  agent for root cause.

  2026-11-13 03:30: legacy language-reconcile witness merged (uncommitted; design file above has the hand-back):
  a clean-close sidecar file `.rdf4j-language-tag-reconciliation` lets a format-2 open skip the reconcile scan;
  18 new tests, six mutations caught, nothing stable green→red in a 92-class comparison. It deviates from the
  design (file, not `store.properties` key) to keep the legacy sidecar's bytes unchanged.
  `LmdbBoundedStartupTest[3]` is still red (5,649 → 1,207 reads, budget 1,000): 973 reads are alternative-IRI
  probes in `ValueStore.seedCoreDatatypes`; follow-up in flight.

  2026-11-13 05:20: seeding attribution (`impl-langwitness/seeding/report.txt`, `attribution-table.txt`). On a
  default legacy store (reference counts untrusted) `seedCoreDatatypes` only looks up: 46 of 48 core datatypes
  are absent, nothing is created, so every open repeats the same misses — 17 reads per absent datatype, of which
  6 are bookkeeping for two read admissions (lookup, miss publication). Totals of the 973: 466 `mdb_txn_id`
  bookkeeping, 314 namespace-candidate probes (each probed once; the names arrive sorted), 46 primary re-probes,
  46 namespace re-reads. Merged: `getCapturedId` skips the second alternative search when the namespace is known
  (the primary miss already searched every other split in the same read transaction) — seeding 973 → 835,
  `[3]` 1,207 → 1,069, ids byte-identical on four store images against an independent cursor-scan oracle
  (`ValueStoreLegacyDatatypeSeedingTest`), three mutations caught. Still red by 69 reads. Decision: one read
  admission for the whole lookup pass (no persisted seeding state, no moved test seam); in flight
  (`impl-langwitness/seeding2/`).

  2026-11-13 06:10, merged (uncommitted; `triage-storefmt/round5/merge-mapfull3/`, `merge-extra/`):
  - `ValueStore.reconstructReferenceCountsOffline`: the UNKNOWN-marker and completion-proof commits report
    MDB_MAP_FULL as the reconstruction's structured capacity failure when auto-grow is off
    (`commitRefCountRecoveryMetadata`). `ValueStoreTest`: the map-full test makes its three attempts on one open
    maintenance store; new `testLegacyRebuildMapFullAfterReopenReportsMarkerCommitCapacity` pins the wrapping
    (red without it, 10 of 10 green with it, mutation red). Headroom measured on 16 KiB pages only.
  - `TripleStore.commitReindexTransaction`: a rebuild batch that wrote nothing publishes no LMDB transaction, but
    its id was recorded as the store's own last transaction; every later ownership check then failed
    ("intervening native writer for predicate domains", "unowned native transaction" at close). The id is now
    taken only if the environment advanced to it. `LmdbValueGcOwnershipTest.legacyCloseRetainsUnusedTermsWhile
    ObjectIndexBuildIsDelayed[1,2]` red 3/3 → green 5/5. Open doubt: `LmdbFrozenReplayTest
    #mutationReplayPreservesDefaultAndNamedContextClears` red 2 of 10 with the fix, 0 of 8 without; 20+20 runs
    in flight.
  - `LmdbCrashRecoveryTest.nativeIndexRewriteSurvivesCrashBeforePropertiesSave`: outdated premise (every writer
    open now saves `store.properties`, already naming the requested index set, de7d35f97b). Decision: test-only,
    cover both directions of sidecar/native disagreement and verify that the native manifest is authoritative;
    in flight (`round6/`).

  2026-11-13 10:30, merged (uncommitted; `impl-langwitness/seeding2/report.txt`): `seedCoreDatatypes` on a store
  under construction with untrusted counts runs all 48 lookups inside one read admission and publishes the
  misses once after the pass (same latest-view and revision check as each per-lookup publication made). The
  `findIdInTransaction` seam is still called once per datatype. Creation for trusted-count stores stays on the
  existing path inside the startup writer (a separate lookup pass first would double the lookups); pinned by
  `aFailureWhileCreatingTheMissingDatatypesCommitsNoneOfThem`. Seeding 835 → 606 reads; `LmdbBoundedStartupTest
  [3]` 1,069 → 840 and `LmdbLegacyAliasWitnessRegressionTest#unchangedCleanLegacyGeneration…` 1,067 → 838
  against 1,000, green 5 of 5 each; ids byte-identical on four images and a trusted-counts store; three mutations
  caught; 102-class comparison: nothing stable green→red. The blast ran before a three-way merge onto the
  map-full hunks (0 conflicts; 136-case focused selection rerun after). Composition check of all merges since
  the r2 snapshot is in flight on a fresh snapshot (`evidence/module15-r6/`).

  2026-11-13 11:10 (`triage-storefmt/round6/`): the reindex fix's doubt is cleared — `LmdbFrozenReplayTest
  #mutationReplayPreservesDefaultAndNamedContextClears` red 3 of 20 with the hunk, 2 of 20 without, same message
  (a pre-existing flake). `LmdbCrashRecoveryTest` reworked test-only and merged: the crash after the reindex
  commit is checked with the sidecar as saved at open and with the pre-open sidecar bytes restored; both recover
  to `{posc=128}`, READY, no ownership error, old tree emptied, sidecar and manifest agreeing after close; a
  cleanup mutation is caught by both, "trust the sidecar over the manifest" only by the restored-sidecar
  scenario and only when both `LegacyStoreDiscovery` and `TripleStore` are mutated (the manifest is protected
  twice). Not covered yet: a crash after the open-time sidecar save but before the native reindex commit
  (sidecar `posc`, manifest `{spoc}`); a third scenario is in flight (`round7/`).

  2026-11-13 12:40 (`triage-storefmt/round7/`): third crash scenario merged (test-only), using the existing
  `TripleStore.afterIndexRebuildBatch()` seam: crash with the `posc` tree staged, manifest still `{spoc}`,
  sidecar already `posc`. A reopen without an index override keeps the manifest's set, empties the staged tree
  and writes `spoc` back to the sidecar; store consistent, READY, no ownership error. The interrupted request is
  not completed — pinned, with the three deciding places named in the test (`LegacyStoreDiscovery
  .validateDescriptors`, the `TripleStore` constructor, `TripleStore.initializeIndexBuild`). "Trust the sidecar"
  is caught only when planted in both discovery and the constructor (either half alone is redundant).

  Red split at the last buildable commit (2026-11-12 23:55; `evidence/module15-r4/summary.txt`; C = 2007a9fdbb,
  2026-11-07, builds; 269 of the 273 red classes exist there). Of the 1349 cases red in both trees: 1254 are red
  at C with the same first line, 46 red with a different first line (14 of them differ only in digits), 39 green
  at C (32 green in all three runs there), 9 in four untracked test classes, 1 not run. So ~96 % of the red set
  is committed history, not this session's or other uncommitted work. The 39 post-C cases sit in 14 classes whose
  test files are byte-identical at C and in the working tree, so code changed under them:
  `LmdbPropertyPathCorrelatedBudgetTest` 17, `LmdbEstimateAuditHarnessTest` 5, `LmdbFrozenReplayTest` 3,
  `LmdbPlanCacheCommitInvalidationTest` 2, `LmdbNativeParallelPipelinesTest` 2,
  `LmdbNativeScanBackedHashBuildParityTest` 2, and eight classes with one each. Onset search across the dated
  working-tree snapshots (`trees/base` 11-09 21:46 onwards) is in flight (`evidence/module15-r5/`): it separates
  reds that predate this session from reds caused by its merges.

  Onset of the 39 post-2007a9fdbb reds (2026-11-13 05:40; `evidence/module15-r5/summary.txt`): 34 are already
  red in the working-tree snapshot of 11-09 21:46, i.e. they arrived with commit 4ab0c09438 or with uncommitted
  work before this session's phase-1 continuation. One changed during this session's first interval
  (`LmdbFrozenReplayTest#asyncProducerReportsFrozenFailureWithoutPrematureErrors[1]`: red 1 of 8 at 11-09
  21:46, 6 of 8 at 11-10 10:29); `[2]` is intermittent throughout; `LmdbNativeParallelFilterCleanupTest
  #actualParallelOwner…[2]`, `LmdbNativeParallelAggregationTest#singlePatternPlainCountUsesParallelAggregation`
  and `LmdbNativeParallelPipelinesTest#parallelSelectUsesExternalRootChunkByDefault` are green alone at every
  snapshot and red only in batch context. Root-cause triage in flight in two private copies:
  planner/estimate side, 27 cases (`triage-planner39/`); native engine and frozen replay, 12 cases
  (`triage-native39/`).

  Planner-side root causes of the post-2007a9fdbb reds (2026-11-13 09:00; `triage-planner39/report.txt`): none is
  in commit 4ab0c09438; all five come from uncommitted phase-0/1 work already present on 11-09.
  - RC-1 (17, `LmdbPropertyPathCorrelatedBudgetTest`; outdated test): the mapped view is opened only through
    `readSelection.openAdmittedView`, which refuses a statistics service that is not the runtime's own, and
    delegated scalar estimates read through the admitted view without a cached result (one extra `estimateJoin`
    per path costing). Test updated to spy the entry's real read selection; merged; a budget mutation is caught.
  - RC-2 (3, `LmdbPlanCacheCommitInvalidationTest` ×2, `LmdbAdjacencyPlannerEvidenceTest`): every two-relation
    join keeps its unpriced written-order fallback as a root candidate, so the comparison is UNAVAILABLE_COST and
    the decision is replay-only: never refreshed, refused once learned evidence moves — and each execution's
    feedback moves it, so a repeatedly executed query re-plans every time; adjacency becoming ready forces a
    synchronous re-plan. The tier-dominance prototype makes the original tests green with the same plans. This
    is the "measured in use" evidence the 2026-11-09 hold asked for; the decision goes back to the user. The
    replay-only test updates are packaged but not merged.
  - RC-3a (2, real regression, slower plans; test notes say about 2× on the AAS benchmark): a statement pattern
    whose bound parts are not a full index prefix now has unknown work, and a priced plan always outranks an
    unpriced one, so the anchor-first plan cannot be chosen in any search mode.
  - RC-3b (4, real regression): dependent-iteration candidates whose right input has only a standalone estimate
    are refused (`unavailableFactorWork`); q14 switches to hash/Cartesian joins over full-scan estimates
    (join estimated 200,000 rows, actual 432); q25 throws `PackedMemoInvariantException` "correlated DPhyp
    target was unreachable" and falls back to an unestimated plan.
  - RC-3c (1, real regression, cause not found): in auto mode the search stops at 590 work units and returns the
    unpriced written-order plan (a 5.5e9-row Cartesian start) although the anytime invariant requires the
    cheapest complete priced plan retained so far.
  Measured candidates for RC-3 (upper-bound price for residual scans; lattice declines instead of throwing;
  standalone price when no variable is shared; the RC-3c incumbent trace) and the corpus-wide effect of each,
  with tier dominance as a fourth row, are in flight (`triage-planner39/round2/`). Nothing is merged for RC-3.

  Native-engine and frozen-replay root causes (2026-11-13 12:00; `triage-native39/report.txt`). Only 3 of the 12
  cases broke after 2007a9fdbb; the other 9 fail there too when their class runs alone (their "green" was one
  two-fork batch run).
  - R1 (7 cases + about 20 other flaky cases in the same classes; racy fixture): on a fresh store the post-ready
    thread publishes the deferred RDF-term domains in a native write transaction; a fixture commit landing
    before `finishDeferredRdfTermDomains` makes it publish again after that commit (trace: post-ready txn 3,
    fixture txn 4, post-ready txn 5 → `parallel-decline snapshot expected=4 sibling=5`; in `LmdbFrozenReplayTest`
    one extra writer start / commit generation). Fixtures now await background maintenance; all 0 of 20. Merged.
  - R2 (`connectionShutdownInterruptAfterResultCloseDrainsBackpressuredWorkers`; product race, metric only):
    `requestShutdown` cancels the signal and interrupts operation threads before `onShutdownRequested` marks the
    read registrations, so a back-pressured worker saw an unowned interrupt and was counted as a worker failure.
    `consumeInterruptionOwner` now attributes an interrupt on an active read to the connection once its own
    cancellation signal is cancelled; 6/6 (20/20 earlier) → 0/20. Merged.
  - R6 (`LmdbNativeAliasJoinEncounterOrderTest#weightedCustomReducers…`; outdated test): pinned one join-order
    dependent sequence; the generic and automatic routes now assert the exact bag, DISTINCT and the 16
    callbacks. A double-call mutation is still caught. Merged.
  - R3 (`LmdbNativeScanBackedHashBuildParityTest` filteredEmpty[1,2]; regression): residual scans priced NaN
    seed unpriced singleton states, `costDenseCorrelatedTarget` throws `PackedMemoInvariantException`, the
    optimizer logs a WARN with stack trace and falls back to the generic nested loop on every planning. An
    8-line containment (return "cannot certify" instead of throwing when a retained state is unpriced) is
    proven on the case; held for the planner-side corpus measurement of the same change.
  - R4 (`LmdbNativeDeepOptionalStrategyTest`; plan-quality regression): the root plan is unpriced, so the BGP
    stays in text order and the OPTIONALs run as generic left joins around eight native islands; NaN source not
    isolated; goes to the planner-side work.
  - R5 (`LmdbNativeGenericBridgeScopeTest#scalarSubquerySharesTheOuterNowScope`; wrong results, also at
    2007a9fdbb): the scalar subquery's generic island folds `NOW()` at compile time from the compile-time
    context while the outer `NOW()` comes from the per-evaluation scope, so two `NOW()`s in one execution
    differ. Fix in flight (test-first, `triage-native39/round2/`).
  Unattributed: `LmdbNativeParallelPipelinesTest#closingResultCancelsWorkersDeniedBeforeNativeDelegateClose
  [3,4]` time out in 12 of 20 runs after the fixes against 4 of 12 in the agent's baseline and 10 of 10 in an
  earlier round; 20+20 alternating runs in flight.

  Measured candidates for the plan-quality regressions (2026-11-13 14:10; `triage-planner39/round2/report.txt`;
  plan-only probe over 182 theme queries: 25 COMPLETE comparisons, 12 unpriced roots, 6 Cascades fallbacks;
  about 7 % run-to-run noise on planning time).
  - Dependent iteration priced when the right input shares no variable with the prefix: all four RC-3b cases
    green, q14 gets the 2007a9fdbb plan back (join 624 rows, not 200,000), q25 no longer throws; corpus: one query
    goes from unpriced to priced with a new plan; no neighbouring test changes.
  - Lattice declines instead of throwing: one RC-3b case alone; never fires in the corpus; with the rule above
    its decline fires 0 times in the audit cases (the lattice becomes reachable).
  - Residual scan priced at its upper bound (visits ≤ P, seeks ≤ max(1, P−2), from `LmdbRecordIterator` and
    `TripleIndex.skipTarget`): both RC-3a cases and the audit alternative green, but a sampled population of
    zero then prices the scan and `LmdbAccessQuoteEvidenceQualificationTest#coincidentStatisticalZeroBounds
    CannotProveAnEmptyPrefix` goes red; restricting to exact/certified populations leaves both AAS tests red
    (their populations are sampled). The four-index corpus never makes a residual lookup, so it cannot show
    this rule's effect. Round 1's "E4 = the commit's price" was wrong: the commit priced from output rows.
  - RC-3c cause found: both trees make the same 77 offers and find the same priced improvement at 512 work
    units; today 55 of 77 offers are unpriced (`refineWrittenJoinCandidate` returns unknown when a connected
    prefix may change an isolated right winner's input; `PackedIncumbentSearch` keeps a two-child JOIN's total
    unknown), and an unpriced re-offer cannot replace an unpriced incumbent (`PackedWinnerTable
    .candidateIsBetter`, `refreshesSameExecutableFallback`), so the root keeps the Cartesian subtree.
  - Tier dominance: 58 of 182 comparisons become COMPLETE with no plan or price change; it breaks
    `LmdbPipelinePlanCacheTest#realStoreHitFeeds…` (the second execution becomes USE_AND_REFRESH,
    "outside-stability-envelope", where the test expects a plain hit) and six core cases that pin
    UNAVAILABLE_COST. Still the user's decision.
  Owner decisions (implementation in flight, test-first, `triage-planner39/round3/`): (1) price disconnected
  dependent iteration as left rows × standalone price; (2) the correlated lattice returns "cannot certify" only
  when a retained state is unpriced and otherwise still throws; (3) residual scans are priced at the scan's
  upper bound at the population's own guarantee level (exact, certified, statistical upper bound), never from a
  non-certified zero, and stay unknown without a population; (4) an unpriced incumbent may be refreshed by the
  same physical expression over child winners that are at least as good and one strictly better. These are
  bounds and dominance rules, not invented point estimates; the proper residual-scan quote remains phase 2.

  Composition check (2026-11-13 15:00; `evidence/module15-r6/summary.txt`; snapshot `trees/verify-1113`, hash
  `6bd715f5…`, taken after the store-format, witness, map-full, reindex and seeding merges and before the
  native-engine and crash-recovery merges): the tree compiles and installs; 363 classes (every changed or new
  test class plus every class referencing `ValueStore`, `StoreProperties`, the language merge, reindexing or the
  domain qualification) ran 5,125 tests. Against the r2 observations: 226 cases red→green, 28 new cases all
  green, 0 stable green→red, and no red case left in the 22 changed or new test classes. Five cases are
  intermittent on this snapshot and were 0 of 4 before: three `LmdbFrozenReplayTest#mutationReplay…` cases
  (2–3 of 4, "expected 2 native attempts, was 3"), `LmdbRetirementForeignWriterWindowTest
  #overlayInvalidationFailure…` (3 of 4), `LmdbRetirementTermOverlapTest#independentTermWriter…` (2 of 4).
  Hypothesis: the post-ready publication race (R1) with timing shifted by the startup changes; mechanism capture
  and fix in flight with the native-engine agent — not yet cleared. 262 cases remain red in the 363 classes, all
  in test classes whose files did not change.

  Four planner rules merged 2026-11-13 16:30 (uncommitted; `triage-planner39/round3/report.txt`, per-rule diffs in
  `round3/package/`): `PackedJoinEnumerator` (rules 1, 2), `LmdbPackedCostModel` (rule 3), `PackedWinnerTable`
  (rule 4), four new test classes.
  - Rule 1: a dependent-iteration candidate or continuation whose right input is unrelated to the prefix
    (`prefixMayChangeFactorInput` false for every right relation: outputs, the whole input contract including
    nested FILTER/EXTENSION reads, unknown domains counted as shared) is priced as left work + right work ×
    max(1, left rows) + probe work; metric `optimizer.dependentRightPrice=unrelated-to-prefix`. RC-3b 4/4 green.
  - Rule 2: `declineUnreachableCorrelatedTarget` returns false and marks the comparison unavailable only when a
    pareto-active retained state is unpriced; otherwise it still throws. ScanBacked filteredEmpty[1,2] green.
    In the audit corpus the 280 "target was unreachable" fallbacks had every state priced (dependent transitions
    refused); rule 1 removes them, rule 2 alone does not.
  - Rule 3: a residual lookup with a non-empty native prefix is priced at the scan's upper bound (visits P,
    seeks max(1, P−2), one open) with P at the population's guarantee level; `optimizer.residualAccessPricing`
    = exact/certified/statistical-upper-bound; unknown for an unavailable population, a statistical zero, no
    finite upper, or `unknownFactorAccess`. Deviation accepted: full-index scans (empty native prefix) stay
    unknown — pricing them turned eight `LmdbNativeScanBackedHashBuildParityTest` ledger assertions red by a
    path not yet traced. RC-3a 2/2 and the audit alternative green.
  - Rule 4: an unpriced offer replaces an unpriced incumbent of the same tier and physical expression when
    every child winner ranks at least as well and one strictly better (priced beats unpriced; priced by the
    table's ordering; unpriced children recursively, depth 64). No extra bytes per offer. RC-3c green (root
    rows 216,792, not 3.85e14; still BUDGET_EXHAUSTED / UNAVAILABLE_COST) and `LmdbNativeDeepOptionalStrategyTest`
    green.
  Verified in the private copy: each rule alone turns only its own cases green; with all four, LMDB selection
  237 cases and core packed selection 1,862 cases have nothing green→red; four mutations caught. Corpus
  (182 queries, plan-only): two plans change (HIGHLY_CONNECTED:8 becomes priced; SOCIAL_MEDIA:5 swaps two
  patterns), unpriced roots 18 → 17, planning time within noise; rule 3 is not exercised by the four-index
  corpus. Not verified: runtime of the two changed plans (execution in flight, `round4/`); the whole-module
  effect (both modules in flight on a fresh snapshot, `evidence/module15-r7/`).

  Execution of the changed plans (2026-11-13 18:00; `triage-planner39/round4/report.txt`; shared machine, load
  4–7, interleaved runs):
  - HIGHLY_CONNECTED:8 (rule 1) is slower: pooled median 1,749 ms against 1,493 ms (+17 %, ranges do not
    overlap, 15 evaluations per side; same single row). The newly priced edge-first plan (root work 26.0M)
    outranks the unpriced type-first plan because a priced candidate always outranks an unpriced one. Rule 1 is
    kept (it fixes the four audit cases and removes 280 planner fallbacks in the audit tests); this query is a
    recorded regression until the type-first plan's connected dependent iteration can be priced.
  - SOCIAL_MEDIA:5 (rule 4): no measurable change (12.5 against 12.3 ms).
  - AAS fixtures (rule 3): about 2× faster with the same result — specificAsset ~350 ms against ~760 ms, query2
    ~760 ms against ~1,790 ms.
  - `query2UsesConnectedEndpointBoundPathWithBoundedWork` is green only when another test has warmed the planner:
    alone in a cold JVM the 500 ms Cascades deadline cuts the budget-256 search at 85 work units
    (DEADLINE_EXPIRED). Test to be made work-bounded; cold planning cost (about 483 ms for 85 work units) is an
    open performance item.
  - One all-four query2 evaluation in a diagnostic sequence ran for more than six minutes (generic
    `JoinIterator`; plan not captured; did not recur in five later runs). Not explained. Whether a
    deadline-cut search returns a worse plan under the new rules is being tested first (`round5/`); until that
    is answered the four rules are merged but not cleared.
  - The eight ScanBacked reds of the as-specified rule 3 are not ledger numbers but term ids: the assertions
    check which pattern the native kernel builds its hash table from (`LmdbNativeHashJoin.tryPlanKernel`: the
    smaller estimate, the right child on a tie). Pricing both full-index scans equally changed the join order
    and produced a 256/256 tie. The two older `scanBackedHashBuildsSupportSubjectAndObjectConstants` reds are
    the same tie at 512/512.

  2026-11-13 20:00 (`triage-planner39/round5/report.txt`): the four rules are cleared of the six-minute
  evaluation. A search stopped before about 65 candidate evaluations returns the written-order plan (Cartesian
  pair first, unpriced) on both builds; it does not finish in 120 s, while a stop at 69–91 evaluations gives a
  priced plan running in 1.7–3.0 s. This predates the rules. The stall recurred once in 9 repeats of the
  diagnostic sequence with the rules and 0 in 8 without (not significant); a fresh planning during the stall
  chose a good plan, so the stalled evaluation ran its own early-cut plan. Why the cut happens: the search
  costs ~3.6 ms per evaluation warm (88–91 % Frontier statistics evaluation), planning takes 800 ms cold against
  a 500 ms deadline, and 8 of 30 warm plannings lost 0.3–3.9 s off-CPU under load; with a 500 ms deadline this
  query never reaches its 256-unit budget. Owner decision: the wall-clock deadline stops the search only once a
  complete priced root plan is retained (`.agent/designs/GH-0000-planning-deadline-first-priced-plan-2026-11-13
  .md`); implementation in flight (`round6/`), with a corpus measurement of never-priced queries before merge.
  Open performance item: Frontier statistics evaluation dominates planning.
  Merged test-only: `LmdbAASQuery2CascadesHypergraphPlanningTest` query2 and query3 cases widen the wall-clock
  deadline so the work budget is their only limit (green alone in a cold JVM 10 of 10; query2 still fails on the
  build without the rules for the original reason).
  HIGHLY_CONNECTED:8: the type-first plan is the written-order fallback, unpriced because VALUES →
  Join(cycle, weight) has a composite right input with no contextual certificate that reads `?optWeight`;
  the provider would need a per-invocation price for that composite. Executed rows: 4.39M type-first against
  5.15M edge-first; the 26.0M price is about five times the actual work.
  ScanBacked remaining reds (red at 2007a9fdbb as well): `scanBackedHashBuildsSupportSubjectAndObjectConstants`
  [1,2] — the inner join input carries the join-conditioned estimate, giving a 512/512 tie that builds the right
  child (proposal: rank build sides by a plan-time standalone estimate; product), and its object half expects a
  hash that is now correctly declined (47,150 against 32,279; fixture); `guardedContext…`[1,2] — the planner
  emits an algebra-level `HashJoinIteration` over two native scans, the forced native route then declines
  (test premise outdated, or a product decision on that join). Not fixed.

  2026-11-13 21:30 (`triage-planner39/round6/report.txt`; decisions in the deadline design file): the deadline
  hazard is narrower than recorded above — the default auto mode has no wall-clock deadline on the main LMDB
  planning path; `timeoutMillis` applies only in budgeted mode. First implementation: query2 with a forced early
  deadline returns a priced plan (1.2–1.3 s) where today it returns the Cartesian written-order plan; costs
  measured in the budgeted corpus: the never-priced SOCIAL_MEDIA:10 plans for ~950 ms, up from ~522 ms, and three
  priceable queries overrun needlessly because the probe sees only the memo's root winner. Not merged. Owner
  decisions: the probe asks whether the plan the deadline stop would assemble is priced; the overrun is capped
  at the deadline's own length (hard stop at twice the timeout). Refinement in flight (`round7/`). Open: a
  plan-cache flight waiter may start planning with an already expired deadline (by reading only).

  2026-11-13 22:30, merged (uncommitted; `triage-native39/round2/report.txt`, `report-R5.txt`,
  `addon-intermittent/report.txt`; after the `verify-1113b` snapshot, so not in the r7 run):
  - R5 fixed in production (`NativeRootPipeline`, `NativeExecutionContext.queryScopedGenericStep`): the semantic
    projection boundary prepared its argument once against the compile-time context, so `NOW()` was folded at
    compile time there (two values 48 ms apart in one execution, a stale `NOW()` on retained steps; ORDER/TopK
    keys alike) and `BNODE()` shared one allocation scope across all evaluations. A share-safe argument keeps
    its single preparation; any other is prepared on first use per evaluation against that evaluation's context
    and cached on the query-scope owner (never per outer row). `RAND`, `UUID`, `STRUUID` were never folded.
    `LmdbNativeGenericBridgeScopeTest`: 27 tests, 9 red before, 27 green; five mutations caught; compile plus
    evaluate time unchanged (62.1 against 62.8 µs); 44 classes compared, nothing red only on the fixed side,
    three more cases green. `NativeOrderOwnerLifecycleAcceptanceTest#genericTupleFunctionNowArgument…[1–4]`
    asserted that an explicit compile-time context controls the native `NOW()`; changed to assert the opposite
    (production never passes an explicit `NOW()`; honouring a caller-pinned one would need a new API).
    `genericFirstUnion…` pinned a pre-11-04 plan shape and was restructured.
  - `closingResultCancelsWorkersDeniedBeforeNativeDelegateClose[3,4]` and `closingResultHandlesReuse…[1,2]`: the
    post-ready race in the one fixture the first await missed (a forced parallel decline at the first `hasNext`
    runs the test's gated close on the main thread, which waits on a latch only the main thread releases, until
    the 60 s timeout). Await added after the copy into the private store; 0 of 20.
  - The five newly intermittent cases: none caused by 2026-11-13's product changes (all 24 changed files restored
    to their earlier versions: same rates). `LmdbFrozenReplayTest`: the post-ready publication is the third
    native attempt; `CountingLmdbStore.init()` now awaits maintenance — the class is 99/99 green in 20 of 20
    runs (52 other cases there were red in every run for the same reason). `LmdbRetirementForeignWriterWindow
    Test` and `LmdbRetirementTermOverlapTest`: the asynchronous value-cleanup worker retires the receipt the test
    recorded or holds the writer lease; both now set `setAsyncValueCleanup(false)` as 51 other classes do; eight
    more cases green; three older reds remain and one failure that was masked now shows at a later line.

  2026-11-13 23:50: refined deadline rule merged (uncommitted; four core planner files, one new core test class,
  an LMDB test support class and one new query2 test; hand-back in the deadline design file). In budgeted mode
  the search continues past the wall-clock deadline to the first priced plan for at most the deadline's own
  length; auto mode is unaffected. Plan-cache flight waiter defect reproduced (waiter plans with the owner's
  expired deadline); candidate B (wait for the owner until its hard stop) in flight (`round8/`).

  2026-11-14 01:00: core plan-cache flight waiter fix merged (uncommitted; `triage-planner39/round8/report.txt`;
  `PackedSearchBudget.hardStopNanos()/clockNanos()`, `PackedPlanCache` flight carries the owner's hard stop and
  `FlightClaim.await(budget)` is interruptible, `PackedCascadesPlanner`; new `PackedPlanCacheFlightWaiterTest`,
  7 cases, 5 red before; three mutations caught; core 1,898 and LMDB 428 cases pass→pass). A waiter waits for the
  owner's result until the owner's hard stop, then plans alone with the fallback marked
  `optimizer.planCacheFlightWaiterFallback=owner-hard-stop`; a no-deadline waiter used an uninterruptible
  `join()` and now ends on interrupt. In LMDB the core flight is practically never shared in budgeted mode (the
  limits include each request's own absolute deadline; flights join only on equal limits); identical concurrent
  requests are deduplicated in `LmdbPipelinePlanCache`, whose waiter also blocks on an uninterruptible `join()`
  (a cancelled waiter stays blocked until its owner ends) — fix in flight (`round9/`). Left as is: the
  flight-capacity wait in `PackedPlanCache.beginFlight` is interruptible but not bounded by a deadline.

  2026-11-14 02:00: `LmdbPipelinePlanCache` waiter made cancellable and merged (uncommitted;
  `triage-planner39/round9/report.txt`): `shared.get()` replaces `shared.join()`; an interrupted waiter restores
  its interrupt status and throws `QueryInterruptedException`, handled at `SailSourceConnection` :342–344 like any
  cancelled planning; a waiter holds no reservation, so nothing is released. New
  `LmdbPipelinePlanCacheCancelledWaiterTest` (red before: the waiter stays parked 10 s; green after; mutation
  red) compares cache counters with and without the cancelled waiter. LMDB selection 428 pass→pass. Not
  verified end to end with a real connection close or query timeout (the test interrupts the thread). New
  behaviour: a request already interrupted when it reaches an unfinished flight now fails at once.

  Whole-module run on the tree with the four planner rules (2026-11-14 03:00; `evidence/module15-r7/summary.txt`;
  snapshot `trees/verify-1113b`, hash `b7d0e1ca…`; it predates the `NOW()` fix, the deadline rule, the waiter
  fixes and the later fixture fixes).
  - `core/queryalgebra/evaluation`, whole module on both snapshots: no per-case difference (7,766 tests; 176
    cases red on both with the same first line — a red set in this module that was not on record; the three new
    `Packed*` classes green).
  - `core/sail/lmdb`, whole module (19,892 tests, 970 failures, 159 errors): against r2, 266 red→green, 37 new
    cases all green, and 11 cases STABLY green→red (3/3 alone on the new snapshot, 0/3 alone on the earlier one),
    outside the selection the rules were verified on: four `LmdbPackedRepeatedVariableAccessCostTest` cases,
    `LmdbAccessQuoteExecutionRegressionTest#residualSkipScanCannotPresentPrefixPopulationAsKnownVisitedWork`
    and `#selectedCoveredAccessCarriesItsModeledCandidateToTheExecutablePlan`, `LmdbFrontierReadOwnershipContract
    Test#rawContextAliasesCannotBecomeMissingGraphWorkZeros`, `LmdbNativeRawEqualityDispatchTest[3,4]`,
    `LmdbFrontierPlanningBenchmarkTest#constructsEightFactorFrontierPlanningState` (BUDGET_EXHAUSTED),
    `LmdbNativeLeftJoinSweepTest#selectiveOuterDeclinesTheSweep`. At least three pin principles rule 3 must
    respect (a prefix population is not known visited work; an absent alias IRI is not a zero-work certificate;
    the scanned population is not narrowed). The rules stay in the working tree but are NOT cleared; attribution
    per rule and the rule changes are in flight (`triage-planner39/round10/`), with a blast radius widened to
    every LMDB test class that touches the cost model, quotes, forced strategies or plan structure.
  - New triage baseline: 1,129 cases still red in 255 `core/sail/lmdb` classes (`evidence/module15-r7/compare/
    still-red.txt`, clusters in `still-red-clusters.txt`; 40 clusters of five or more cover 568). Largest:
    `LmdbNativeStatementTermRoleTest` 80, `LmdbFrozenReplayTest` 53 (fixed after this snapshot),
    `LmdbNativeServiceQueryCompletenessTest` 44, `LmdbMappedWholeOperatorSurfaceTest` 37,
    `LmdbNativeValuesFilterFoldShapesTest` 34.

  2026-11-14 03:45: the user approved triaging all remaining reds and then said to stop using subagents. The
  one running agent was stopped mid-work (`triage-planner39/round10/`: per-rule attribution done, its fix
  incomplete — 17 green→red candidates in its own wide run). Attribution of the 11 regressions (single-rule
  builds, `round10/runs/a-*`): rule 3 causes eight (it presents the prefix population as visited work and prices
  an absent alias IRI as zero, both against principles those tests state; it also exhausts the benchmark
  fixture's budget), rule 2 causes the two `LmdbNativeRawEqualityDispatchTest` cases, rule 1 causes
  `LmdbNativeLeftJoinSweepTest#selectiveOuterDeclinesTheSweep`, rule 4 none.
  Owner decision: rules 1, 2 and 3 are reverted from the working tree (reverse-applied; their diffs and three
  test classes kept under `.agent/research/GH-0000-phase1-continuation-2026-11-09/reverted-planner-rules-
  2026-11-14/`); rule 4 stays. Consequence: the RC-3a (2 + the audit alternative), RC-3b (4) and ScanBacked
  filteredEmpty (2) cases are red again as before 2026-11-13, HIGHLY_CONNECTED:8 returns to its earlier plan, and
  the AAS speed-up is gone. The measured candidates remain the starting point for doing this properly: residual
  scans need a quote that carries a bound without a point estimate (phase 2), and any planner rule is merged
  only after a whole-module per-case comparison. Verification run of the affected classes on the working tree
  in progress (main thread).

  Revert verified on the working tree (2026-11-14 04:30; main thread, workspace `revertverify`, reports under
  the session scratchpad `revert-verify/`): the 11 regression cases are green again
  (`LmdbPackedRepeatedVariableAccessCostTest` 28/28, `LmdbAccessQuoteExecutionRegressionTest` 7/7,
  `LmdbFrontierReadOwnershipContractTest` 32/32, `LmdbNativeRawEqualityDispatchTest` 4/4,
  `LmdbFrontierPlanningBenchmarkTest` 2/2, `LmdbNativeLeftJoinSweepTest` 7/7); rule 4's cases stay green
  (`LmdbNativeDeepOptionalStrategyTest` 11/11, `LmdbSubSelectDirectLookupEstimateTest` 3 pass, 1 skip); red again
  as expected: `LmdbAASQuery2CascadesHypergraphPlanningTest` 2, `LmdbEstimateAuditHarnessTest` 13 (8 older + 5),
  `LmdbNativeScanBackedHashBuildParityTest` 6 (4 older + 2). Core: `PackedWinnerTableChildRefreshTest` 28/28,
  `PackedPlanCacheFlightWaiterTest` 7/7, `PackedPlanCacheTest` 65/65, `PackedSearchBudgetTest` 12/12.
  One deadline-rule test depended on the reverted rule 2: `PackedPlanningDeadlineOverrunTest
  #overrunWithoutAPricedPlanEndsWhenTheGraceExpires` ran a never-priced search to completion as a fixture
  check, which without rule 2 ends in "correlated DPhyp target was unreachable". The check is now bounded by
  work (deadline + grace + 1 units); the class is 8/8. Consequence to keep in mind: in budgeted mode a
  never-priced correlated-filter search that used to be cut by the deadline can now run on during the grace to
  that invariant failure, after which LMDB falls back to the generic plan with a warning (what auto mode does
  for such a query in any case). Not measured.
  Next (main thread, no subagents): rerun all 255 formerly red LMDB classes on the current tree to get the real
  remaining list (in progress, scratchpad `redbase/`), then triage family by family
  (`triage-families/*.classes.txt`).

  2026-11-16: the user allowed Sonnet subagents again, closely supervised (diagnosis-only tasks, 40-minute time
  box, `STATUS.md`, no real-tree access; brief in `triage-families/COMMON.md`, copy in
  `.agent/research/GH-0000-phase1-continuation-2026-11-09/scripts/`). Evidence under /private/tmp is being purged
  by age (the semaphore probe and older run directories are gone); reports and diffs of 2026-11-12/13 are backed
  up in `.agent/research/GH-0000-phase1-continuation-2026-11-09/tmp-evidence-backup-2026-11-16-part6.tgz`.
  Current red list on the working tree after the revert: 1,044 cases in 240 classes (scratchpad
  `redbase/red-now.txt`, backed up as `scratchpad-redbase-2026-11-16.tgz`).
  - `LmdbNativeStatementTermRoleTest` (80; wrong fixture, test committed red in cce41a89a5): it called the
    native compiler with a null dataset and an unvalued graph variable, which `NativeContextSelection.supports`
    declines on purpose (only a selected set of ordinary named graphs proves the variable's values are
    unchanged resource ids). The fixture now supplies a dataset with the quad's graph as named graph, and a new
    case pins the decline for the null dataset. 98/98 on the working tree (main thread run).

  - Step-shape classes (`LmdbNativeBoundaryFirstAssociationTest` 14, `LmdbNativeTrailingRegionDependencyTest`
    22, `LmdbNativeInteriorIslandTest` 10; Sonnet diagnosis, `triage-families/a3/`): an archive of 4b2a72bc4c^
    runs them 0 / 6 / 2 red, the working tree 14 / 22 / 10. Cause for 35 of the 38 new reds: commit 4b2a72bc4c
    (2026-11-04) calls `NativeRootPipeline.projectionBoundary` in `compilePhysical` before any native root
    compiler; for a projection with a non-guaranteed output (CONCAT, VALUES … UNDEF) it returns a boundary whose
    argument is prepared by the generic evaluator (`NativeProjection(arg=JoinQueryEvaluationStep)`), so the
    native row root with its interior alternatives, trailing-region movement and boundary hoist is never built.
    Results stay correct; the optimisation is lost (real regression). Skipping the boundary at the root in the
    agent's copy brings the association class from 14 to 1 red. That contradicts the comment a few lines below
    ("the semantic pipeline is the total floor, not a higher-priority replacement for optimized row plans").
    Candidate being measured across all native-engine test classes before any decision: use the boundary only
    after the ordinary root compilers decline (`a3/round2/`). Six older reds are a fixture whose
    `Bound(?n) = Bound(?c)` filter the simplifier folds to `true = true` (fixture fix in flight, `a1/round2/`);
    two `minusLexicalScope…` cases lose a recorded cardinality (cause unproven).

  - `LmdbNativeValuesFilterFoldShapesTest` (34; Sonnet diagnosis, `triage-families/a2/`): rows are correct in
    every case; only plan assertions fail. 30 are an outdated test: since 4b2a72bc4c the fold is wrapped in a
    `ValuesInputGuard(folded=…, retained=…)` that keeps the unfolded plan as a runtime fallback, and the test's
    `doesNotContain("Values(rows=")` trips on the `retained=` branch (stripping it in the agent's copy turns the
    30 green). Case [14] is a real plan-shape regression: `TupleExprs.tupleBindingObservationDomain` reports
    `known=false` for a pattern with `LmdbValueVar` constants (the exact-class table does not list the `Var`
    subclass), and `references()` then aborts the fold. [5] and [7] undetermined; [34] is a single-row VALUES
    that is not inlined (planner side). Test update and two candidate fixes for [14] being prepared in the
    agent's copy (`a2/round2/`).
  - `LmdbNativeLexicalOptionalConditionTest` (13): same cause as the step-shape classes — the projection
    boundary claims the projection (`needsSemanticProjection` is true for a projected OPTIONAL subselect
    operand), the argument runs as a generic `LeftJoin`, and the native lexical OPTIONAL lowering is never
    reached. With the boundary disabled for a `LeftJoin` argument in the agent's copy all 14 pass with native
    rows equal to the control. Covered by the candidate measurement.

  - 2026-11-16 12:50: the `LmdbNativeValuesFilterFoldShapesTest` update is merged and verified on the working
    tree (main thread: 66 tests, 4 failures — [5], [7], [14], [34]; a fold-disabling mutation turned the 30 red
    again in the agent's copy). The fold expectations now read the `folded=` branch of each
    `ValuesInputGuard` (balanced-parenthesis parse) and assert the `retained=` branch exists. [7] has the same
    cause as [14]. Owner decision for [14]/[7]: decide locally in the LMDB values compiler (a constant `Var`
    with a value does not make a pattern's observation domain unknown) and leave the model's exact-class rule
    alone; merge only after a broad per-case comparison (in flight, `a2/round3/`). [5]: the shared optimizer
    turns the FILTER into `BindingSetAssignment[code=…]` without the exact-values mark, so neither fold entry
    applies — open, planner side. The fixture fix for the six folded-`Bound` cases is ready but they then meet
    the projection-boundary assertion; held (`.agent/research/…/triage-families/a1-round2-fixture.diff`).

  - 2026-11-16 13:30, row-root-first candidate measured and rejected (`.agent/research/…/triage-families/
    a3-round2-*`; 3,214 cases in 224 classes, 4 of 8 batches): trying the native root compilers before the
    projection boundary turns 72–76 cases green but 326–341 red, stably. Six are wrong results (a hidden LATERAL
    input renamed across VALUES/BIND/nested projection: counts 1 where 3 are expected; an API parameter binding
    dropped from the row, three cases; a path identity row missing, two cases); about 320 are authority-fact
    assertions (row values plus tracked/request/local provenance) in the `Native…Boundary` and `NativeSealed…`
    classes. So the boundary is a correctness mechanism and cannot simply be reordered. Whether a narrower
    condition exists (the optimisation-pinning shapes leaving `needsSemanticProjection` through a different exit
    than the correctness cases, or the boundary's argument compiled natively with origin facts) is being
    diagnosed (`a3/round3/`). Until then the 35 step-shape cases, the 13 lexical-OPTIONAL cases and the six
    held fixture cases stay red as a known optimisation regression of 4b2a72bc4c.

  - 2026-11-16 14:20, the boundary's exits do not separate the groups (`.agent/research/…/triage-families/
    a3-round3-*`). Every optimisation-pinning case leaves `needsSemanticProjection` through the same exit as most
    wrong-result and provenance cases: the projection's argument has neither a `NativeBGPOrigin` nor a
    `NativeProjectionAuthorityProof`. What differs is the argument: a Join of BGP patterns, VALUES and nested
    scope-change Projections for the optimisation tests; Lateral, ArbitraryLengthPath, LeftJoin, Union, Order,
    Graph for the correctness cases (the dropped API parameter uses the "projected name not guaranteed" exit).
    The boundary prepares its argument with `exactOriginView`, which offers the native compiler only leaves
    that carry an origin proof and builds every composite from generic operators — 95 Joins, 74 Projections and
    70 Extensions refused, 231 leaves compiled natively in the three classes. The per-row facts at stake are
    `BindingSetOrigin` (tracked, request names, local names, retained parameters) and the GRAPH frame; the
    native side can carry them (`NativeRowProvenance`, `NativeProjectedBindingSet`) but only derives them where a
    static proof exists. Two ways forward, an owner/user decision: extend the origin proofs and the native row
    step to a Join over subquery-boundary Projections, or accept generic execution for those shapes and
    re-target the tests at shapes that still reach the native row root. A corpus count of how many theme
    queries take the boundary with a generic argument is in flight (`a3/round4/`).

  - 2026-11-16 16:10: corpus footprint of the projection boundary (`.agent/research/…/triage-families/
    a3-round4-table.tsv`): 17 of 117 theme queries compile through it, all with a generic argument; 16 are the
    long OPTIONAL-chain queries at index 11/12 (not in the harness's default 77), through the "projected name
    not guaranteed" exit; 151 left joins run as generic operators. Of the ten largest, five finish (0.7–22 s,
    counts match the catalog) and five time out at 60 s. No baseline yet; the same ten are being timed on
    4b2a72bc4c^ (`a3/round5/`).
    The local fix chosen for FoldShapes [14]/[7] does not work on a clean build (the earlier result ran with the
    other candidate's model jar still installed; three more unknown-observation gates in the values compiler
    decline). Withdrawn before any merge. The model-level candidate (a constant `Var` subclass with a value is
    a known node) is being evaluated with clean-build proof and whole-module runs of the model and evaluation
    modules (`a2/round4/`).

  - 2026-11-16 17:00, before-boundary timing is inconclusive (`.agent/research/…/triage-families/a3-round5-*`):
    the archive of 4b2a72bc4c^ refuses the current theme store ("Unrecognized namespace database metadata key:
    backup-logical-boundary"), so eight of the ten queries have no baseline. On an older medical-records-only
    store both trees open: MEDICAL_RECORDS:11 runs 1.2–1.3 s before and 2.1–2.3 s now (same counts);
    MEDICAL_RECORDS:12 timed out at 60 s before and runs 1.4–1.6 s now. Row counts match the catalog wherever a
    query finishes, on both trees. Correction to the round-4 reading: under the boundary several of these
    queries do get a native left-join chain through `NativeSealedProjection` (PHARMA:12, LIBRARY:12,
    MEDICAL_RECORDS:11/12), three are native multi-joins, and three are generic iterators (PHARMA:3,
    ELECTRICAL_GRID:12, TRAIN:12). So there is no evidence of a broad slowdown from the boundary; the cost is
    specific to shapes `NativeSealedProjection` does not admit, such as the tests' join over nested subselects.
    Decision put to the user: re-target the ~54 optimisation-pinning cases at shapes that still reach the native
    row root and record the sealed-projection extension as a design item, or extend it first.

  - 2026-11-16 19:00, the constant-`Var` rule in the model's observation-domain analysis (candidate (b)) is
    still not merged (`.agent/research/…/triage-families/a2-round5/compare.txt`). Compared on batches b03–b09
    in both states (7,006 further cases, clean builds, compile lines recorded): four stable green→red, all
    forced kernel tiers that now decline with `no-fusion-opportunity` and none a row mismatch
    (`LmdbNativeRawEqualityDispatchTest#equalityProbePreservesRowsAndCountAcrossRawExecutionRoutes[3],[4]`,
    `LmdbNativeServiceQueryCompletenessTest#queryRunsWithRealKernelAndFederation[73],[74]`); five stable
    red→green, among them rows [23],[24] of the same service class. Temporary counters show the new rule was
    reached at all LMDB callers (BGP origin 435 times, sealed projection 46, primitive input 96). The rule
    therefore moves which native route a constant-bearing pattern takes; whether the four cases are outdated
    forced-tier expectations or a lost fusion route is being traced with a per-call-site switch
    (`a2/round6/`). Diagnosis of family C path classes and family B boundary classes started in parallel.

  - 2026-11-16 20:00, the four cases are explained and the rule is applied to the working tree, provisionally
    until the comparison on the classes outside the 715-class selection is back (`a2/round7/`). With one call
    site at a time switched back to the old rule, only `NativeBGPOrigin.appendPattern` restores the four
    cases: a single statement pattern whose constants are the store's own `Var` subclass now has a known
    observation domain, so `NativeExactOriginStep` admits it as a native leaf under a generic parent. Before,
    such a leaf was never native (since the exact-class table of 2026-10-31) and a forced kernel tier was
    silently not consulted; now the tier is offered at a one-scan leaf and declines because one scan has
    nothing to fuse. Rows and counts are identical in both states. The two tests therefore force a preference
    list (`<kernel tier>,batch`) for exactly those shapes (GRAPH bodies; `rows-limit-zero`); a single tag stays
    a hard force everywhere else. Rows [23]/[24] turn green through `NativePrimitiveInputSemantics`: the BIND
    over a known pattern is primitive, and the aggregate kernel the test demands now binds. Red-first evidence
    for the model test: `.agent/research/…/triage-families/a2-round6/TupleExprsTest-pre-fix-red.txt`.

  - 2026-11-16 20:00, two more families are mostly the projection boundary
    (`.agent/research/…/triage-families/a1-c-paths/`, `a3-b-boundary/`). Paths: 33 of the 40 reds in
    `LmdbNativePropertyPathTest`, `LmdbNativeGeneralPathTest` and `LmdbNativeZeroLengthNodeIdsTest` fail
    because a projection over a path has neither a BGP origin nor a projection authority, so the path body is
    prepared by the ordinary evaluator and the native path plans never run (boundary disabled in a copy: 40 →
    7 red). Boundary classes: 42 of 54 reds in `NativePureUnionVisibilityBoundaryTest`,
    `NativeCompositeOriginBoundaryTest` and `NativeNestedSelectInvocationScopeTest` are exit E2b/E3b with a
    LeftJoin, Union, Order or Lateral argument; none is a wrong result. These tests were committed one minute
    after the boundary (cce41a89a5 after 4b2a72bc4c) and describe themselves as "feature-admission red …
    the actual composite owner must execute": they specify unfinished work, a native OPTIONAL/UNION owner
    under the boundary, and are not outdated. That weighs the open decision towards extending native
    admission rather than re-targeting tests. Independent of the boundary: ten cases where the input binds an
    output name of the nested SELECT and `NativeSubSelectScopeBoundaryStep` deliberately runs the ordinary
    fallback on a detached clone (no execution-path metric on the test's Group), and two where
    `NativeContextSelection.supports` refuses a lowered private graph.

  - 2026-11-16 21:00, the boundary is a measured performance regression for property paths
    (`.agent/research/…/triage-families/a1-c-paths-cost/results.tsv`; 200,000-edge tree, 7 timed runs after 3
    warm-ups, two alternating rounds per state, identical row counts). Median with the boundary against the
    boundary disabled in a copy: `<root> :p+ ?o` 1,675 ms against 306 ms; `<root> :p* ?o . ?o :q ?v` 2,706 ms
    against 47 ms; `<root> (:p|:q)+ ?o` 3,074 ms against 301 ms. A path with a bound object, `:p?` and a
    COUNT over a path are unaffected. So the 33 path reds are real: since 2026-11-04 a SELECT over an
    unbounded path runs the path in the ordinary evaluator. Disabling the boundary is not the fix (trying the
    native row root first was measured and gives wrong results in six cases); the open design question is
    where a path body may be admitted as a native leaf under the boundary and what it must prove
    (`a1/c-paths-design/`, diagnosis in flight).

  - 2026-11-16 23:40, the constant-`Var` rule is no longer provisional
    (`.agent/research/…/triage-families/a2-round7/compare.txt`): the 829 LMDB test classes outside the earlier
    selection were compared case by case in both states (8,939 cases): no stable green→red and no stable
    red→green; twelve unstable flips, each rerun alone twice per state. Together with rounds 4 and 5 every
    `*Test` class of the module except `LmdbStoreMultiRepoTest` and `QueryBenchmarkTest` has been compared, plus
    the whole model and evaluation modules. On the working tree the deciding classes give: `TupleExprsTest`
    26/26, `LmdbNativeValuesFilterFoldShapesTest` 64/66 (cases [5] and [34] remain), `LmdbNativeRawEqualityDispatchTest`
    4/4, `LmdbNativeServiceQueryCompletenessTest` 42 red (was 44). `LmdbNativeRangePartitionedScanTest` was red
    alone in both states (a fixture race with the post-ready maintenance commit: the sibling transaction id no
    longer matches the source's and `openParallelSources` returns null); it now waits for background
    maintenance in `setUp` and is 5/5 in three alone-runs. Unstable cases seen in the comparison and still to
    triage: `LmdbOperatorFeedbackPlanningTest#secondPlanUsesLeftJoinOperatorFeedbackForBoundOptionalFanout`
    (correction 2026-11-17: it passes alone 5 of 5; it fails only in one 11-class composition under load, in
    both states), `LmdbRdfTermDomainStartupCapacityTest#startupRebuildRetriesAnUnjournaledCommitAsOneCompleteOperation[2]`,
    `LmdbCommitAtomicityTest#autogrowCommitFailureBeforeTheFirstPhysicalCommitLeavesThePreexistingModel`,
    `LmdbSailStoreCloseTest#closeDrainsRunningScheduledPersistBeforeClosingEstimator`.

  - 2026-11-16 23:40, design for the path regression written:
    `.agent/designs/GH-0000-native-path-under-sealed-projection-2026-11-16.md` (admit an `ArbitraryLengthPath` in
    `NativeSealedProjection`, empty untracked frame and default graph only; slice 1 `+` over one statement
    pattern, slice 2 `*` with a constant IRI endpoint). Disabling the boundary for paths is unsafe: 33 of 56
    cases in `NativePathProjectionProvenanceContractTest` and `LmdbNativeContextSelectionContractTest` go red,
    two with wrong bags. Slice 1 is being prototyped in a private copy (`a1/c-paths-proto/`). The packed forced
    declines are the boundary too for 20 of 27 cases (exit E3 on a BIND result; the packed planner is offered
    only the leaf and cannot produce the constant slot); the six `LmdbNativePackedFtreeWitnessTest` cases are
    independent (a `FILTER … IN` rewritten to a VALUES child, which the packed planner does not accept). A
    census of all red classes with the boundary disabled is running (`a3/boundary-census/`).

  - 2026-11-17 01:30, boundary census and path prototype
    (`.agent/research/…/triage-families/a3-boundary-census/`, `a1-c-paths-proto/`). All 240 red LMDB classes
    (4,276 cases) were run with the boundary as it is and with it disabled by a property in a copy: 928 red
    with the boundary, 1,069 without; 213 cases are stably red only because of the boundary
    (`boundary-caused.txt`) and 356 cases are stably green only because of it (`boundary-protects.txt`), 18 of
    those with wrong rows (extra alias binding in `NativeProjectionOrderTieBoundaryTest`, missing request
    parameters in `LmdbNativeApiParameterParityTest`, a lateral rename). So the boundary stays, and the 213 are
    the backlog of shapes that need a native route under it; the largest groups are
    `LmdbNativeServiceQueryCompletenessTest` 34, `LmdbNativePropertyPathTest` 22,
    `LmdbNativeLexicalOptionalConditionTest` 13, `LmdbNativePackedMorselsTest` 13, `LmdbKeyedSubqueryJoinTest`
    11, `LmdbNativeWildcardPredicateBatchTest` 10. The first slice of the path design works in a copy: only
    `NativeSealedProjection` changes; over the six path and four sealed-projection classes 41 reds become 28
    (13 `LmdbNativePropertyPathTest` cases green, none green→red, both contract classes unchanged), and
    `<root> :p+ ?o` on the 200,000-edge tree drops from about 1,670 ms to about 345 ms (306 ms with the
    boundary disabled). `:p*` and alternatives are unchanged (later slices). Not merged: the test is being
    strengthened (row provenance, random differential, composites) and a whole-module comparison is running
    (`a1/c-paths-proto2/`).

  - 2026-11-17, owner decision on the projection-boundary reds: extend native admission first. The
    boundary-pinned tests are not re-targeted; the 213 boundary-caused cases are worked off by admitting more
    shapes natively under the boundary, one slice at a time in order of payoff (refusal histogram in
    `a3/sealed-refusals/`), each with a design note, a prototype in a private copy, a differential test with
    mutations, and a whole-module per-case comparison before it is merged.

  - 2026-11-17 06:30, ordering of the admission slices by measured cost
    (`.agent/research/…/triage-families/a3-cluster-cost/summary.tsv`, `a3-filter-facts/`, `a3-service-facts/`).
    Counting refusals misranks the work: the sealed projection's most frequent first refusal is a Filter, but
    admitting filters turns only 6 cases green, because a value-only filter over patterns never reaches the
    boundary (`NativeBGPOrigin` admits it). Counting red cases misranks it too: the largest cluster, 34 service
    queries, has one local pattern before a remote call, so there is nothing for a kernel to fuse and the row
    provenance through SERVICE differs from what the sealed projection emits. So the boundary's run-time cost
    was measured for 17 query shapes on a 100,000-subject store (same compiled source, boundary on and off by
    a property in a copy, identical row counts everywhere). Stable costs: ORDER BY with LIMIT over a BIND 170 ms
    against 26 ms (the top-k route is lost), OPTIONAL with a condition 1,490 ms against 313 ms. BIND, MINUS,
    NOT EXISTS, nested SELECT, UNION with BIND and GRAPH are within 1.2–1.8×, not separable from noise on the
    loaded machine; a nested grouped subselect and `GRAPH ?g` are faster with the boundary. Order of work:
    property paths (4.6–58×; slice 1 in comparison), then OPTIONAL with a condition, then ORDER BY with LIMIT
    over a BIND; the service cluster last, as a separate owner decision.

  - 2026-11-17 15:00, first admission slice merged (uncommitted): property paths of length one or more over a
    single statement pattern run through the sealed projection
    (`.agent/designs/GH-0000-native-path-under-sealed-projection-2026-11-16.md`, hand-back section). Only
    `NativeSealedProjection` changes. Test first: `NativeSealedPathProjectionTest` red 9 of 41 on the working
    tree, green after. Whole-module per-case comparison in private copies: 1,542 of 1,542 LMDB test classes,
    19,638 cases, no stable green→red, 29 stable red→green. On the working tree `LmdbNativePropertyPathTest`
    goes from 23 red to 10 and `<root> :p+ ?o` on the 200,000-edge tree from about 1,670 ms to about 345 ms.
    Second slice (OPTIONAL with a condition,
    `.agent/designs/GH-0000-optional-condition-under-sealed-projection-2026-11-17.md`) works in a copy: the
    measured query goes from 1,490 ms to 328 ms, 21 classes unchanged; its whole-module comparison is running.

  - 2026-11-17 15:00, fixture races fixed and one real bug found while triaging unstable cases. Five tests
    raced the store's background work and now exclude it explicitly: the post-ready maintenance publication
    (`LmdbNativeRangePartitionedScanTest`, `LmdbOperatorFeedbackPlanningTest`,
    `LmdbFilterReadContextIdentityTest`: a query or capture inside an open publication has no current read
    authority, so a forced parallel strategy declines, operator feedback is dropped, or a read context is not
    captured) and the asynchronous value cleanup (`LmdbExplicitRetirementFailureTest`, two cases of
    `LmdbRdfTermDomainStartupCapacityTest`: the cleanup thread drains the retirement queue or holds the
    dictionary write transaction that the test drives directly). `LmdbResourceDatasetCompositionTest`: one case
    pinned visit counting through a value subclass that the plan cache has refused since 2026-11-06; it is
    rewritten as two cases and the visit bound is dropped as unobservable. Real bug, not fixed yet
    (`triage-families/a2/round15/`): when the post-ready thread hits map-full committing the deferred
    RDF-term-domain work while a user write is admitted, the growth coordinator refuses
    ("capacity recovery cannot replace another admitted writer's continuation"), the startup code attaches the
    refusal as suppressed and rethrows, and the domain work fails for the rest of the process although
    auto-grow is on (`LmdbRdfTermDomainStartupCapacityTest#domainEnabledEmptyStartupAcceptsSmallAutoGrowCapacity`,
    7 of 8 warm iterations). Reproducer, after-failure state and fix options in progress (`a2/round16/`).
    Also found, not yet diagnosed: for an OPTIONAL whose right-side-only filter is relocated into the right arm
    (`regex(str(?w), …)`, `strlen(str(?w)) > 1`), the native route returns the same bag but rows that do not
    carry `?w` as a local name (`a3-optional-condition-proto/scratch-reproducer.diff`).

  - 2026-11-17 21:30, second admission slice and the startup fix merged (uncommitted). OPTIONAL with a
    condition runs through the sealed projection
    (`.agent/designs/GH-0000-optional-condition-under-sealed-projection-2026-11-17.md`, sections "Prototype
    hand-back" and "Merge"): test first (`NativeSealedOptionalConditionProjectionTest`, 14 of 50 red, then
    green), whole-module comparison 1,543 of 1,544 classes with no stable green→red; the measured query goes
    from about 1,490 ms to about 328 ms. The one apparent regression in that comparison was a race that fails
    about half the time in every file-and-copy combination — two reruns per state are not enough to call a
    flip stable in a class that is already unstable; eight reruns or a swap test are. Startup bug fixed
    (`triage-families/a2-round16/`, `a2-round17/`, `a2-round18/`): in
    `LmdbSailStore.initializeDeferredRdfTermDomains`, when emergency map growth is refused because another
    writer is admitted, or the map cannot be grown because a write transaction is open, the thread waits for
    that writer's lease and retries the complete publication instead of failing. The maintenance pattern
    (phased growth) was tried first and rejected because it changes the pinned interrupt behaviour of this
    branch. Test first: `LmdbDeferredDomainStartupWriterOverlapTest`, two latch-ordered cases, both red before;
    with the fix the new test and `LmdbRdfTermDomainStartupCapacityTest` are 33 of 33 in three runs, and
    `domainEnabledEmptyStartupAcceptsSmallAutoGrowCapacity` failed 0 of 80 in the agent's loop (5 of 40 before).
    Unproven: two mutants survive (always retry when growth returns false; retry on every refusal reason)
    because no test can produce those states, and the refusal is recognised by its message text. Still
    untriaged in that class: `publicationCloseFailureRetainsOperationCapacityAndNeverInventsRetry` (about one
    run in five) and `startupRebuildRetriesAnUnjournaledCommitAsOneCompleteOperation`. Next slice in
    progress: zero-length paths with a constant endpoint and alternatives inside a path
    (`triage-families/a1-c-paths-slice23-facts/`: probe with no mismatch; 2,700 ms to 53–67 ms and 3,000 ms to
    322–377 ms).
  - 2026-11-17 23:40, path slice 2 merged (uncommitted) and bulk fixtures corrected. Zero-length closures with
    a constant endpoint and same-direction alternatives of single patterns now run through the sealed projection
    (`.agent/designs/GH-0000-native-path-under-sealed-projection-2026-11-16.md`, "Slice 2 merge"): whole-module
    comparison 1,541 of 1,541 classes, no stable green→red, 14 stable red→green; test first on the working tree
    (`NativeSealedPathProjectionTest` 11 of 69 red, then green); ten related classes 39 reds before, 25 after,
    none new. The three bulk-load test classes used a 2 MiB loader memory budget, below the loader's fixed
    overhead of about 2.6 MiB, which makes the external sort write one run file per record (124 s against about
    1 s for 10,000 statements); the fixtures now use 64 MiB and are 159 of 159 green
    (`triage-families/a3-bulk-hang/`, `a3-bulk-budget/`). What the loader itself should do with such a budget
    is an open owner question. Next admission slice: ORDER BY with LIMIT over a BIND (measured 6.7×).
  - 2026-11-17 (later), third admission slice designed, prototype in an agent's private copy
    (`.agent/designs/GH-0000-computed-bind-under-sealed-projection-2026-11-18.md`; facts in
    `triage-families/a3-orderby-bind-facts/`). The measured query (154–168 ms against 20–21 ms) is refused
    because its BIND is computed, not because of ORDER BY: Order and Slice are the boundary's own stages and stay
    outside the sealed body, so admitting the BIND alone reached 70–93 ms in a crude probe. Every Extension
    evaluator marks a BIND target as locally produced even when the expression fails; the sealed projection
    derives local names from bound cells, so the design adds an always-local rule for a computed target and
    restricts it to the mandatory spine of the body. Whether an order push-down slice follows depends on the
    prototype's time breakdown.
  - 2026-11-17 (evening), computed-BIND slice merged into the working tree (uncommitted; design note "Merge"
    section; evidence `triage-families/a3-computed-bind-proto2/`). Review of the prototype found that a computed
    IRI could become a native join or OPTIONAL key (it carries a synthetic id, not a store id); it gave equal
    rows on the test data, and is now refused: a computed target may not be a shared name of a Join or LeftJoin,
    nor a LeftJoin condition name. Whole-module comparison 1,545 of 1,545 classes, 19,896 cases, no stable
    green→red, 73 stable red→green (the new test). On the working tree `NativeSealedComputedBindProjectionTest`
    went from 74 of 183 red to green and 13 related classes kept the same 61 red cases. The slice fixes no
    previously red test; its effect is cost (157–169 ms to 81–91 ms on the measured query in the prototype,
    17–23 ms with the boundary disabled). The remainder is the per-row projection and the boundary's own top-K
    stage; pushing Order and Slice under the boundary is undecided (owner question open).

  Not done in this block, still required for the phase-1 exit: recognition purity, the remaining budget-charging
  gaps, dimension-mapping and algebra-law tests, inactive LMDB provider conformance, and the nested-context design.
  Open items and untriaged pre-existing reds are listed in the 2026-11-10 revision note at the end of this file.
  Phase 1 stays in progress; phases 0 and 6 are not declared complete.

- [ ] Unified phase 2: quote LMDB and adjacency physical access.
- [ ] Unified phase 3: preserve consumer requirements and comparable costs.
- [ ] Unified phase 4: deliver the first selected-to-executed physical recipe.
- [ ] Unified phase 5: integrate remaining native strategy families.
- [ ] Unified phase 6: qualify bounded search, refinement, and cache reuse.
- [ ] Unified phase 7: attribute runtime feedback to executed recipes.
- [ ] Unified phase 8: qualify end-to-end performance and retire duplicate costing.

- [x] (2026-08-18 12:04Z through 15:50Z) Eliminate the typed semi/anti telemetry hot-path cliff and reduce general runtime
  observation overhead. The scoped redesign replaces the saturated exact distinct table with a bounded exact/HLL
  collector, carries explicit exact/approximate quality through typed feedback and LMDB persistence version 22,
  batches primitive counters until root close, samples high-frequency timing calls, and removes avoidable
  materialized-key probe allocation. The estimator mean, structural cache upper, selected materialized q9 algorithm,
  and SPARQL semantics remain fixed. Test-first evidence is retained in
  `initial-evidence.telemetry-runtime-redesign.txt`; qualification compares telemetry-disabled and sampled execution
  of the identical plan before the supported JDK 26 q9/JFR run. The final rebuilt 3x3 q9 qualification is now
  123.052 ms/op with full telemetry disabled and 126.095 ms/op with sampled-full telemetry, a 2.47-percent delta.
  The final sampled-full ten-by-ten-second JFR run is 129.915 ms/op; an earlier disabled control recorded 126.788
  ms/op. In the final sampled-full profile, core telemetry is 49 of 3,615 measurement-window Java execution samples
  (1.36 percent), or 242 samples (6.69 percent) including the combined instrumented iterators; the largest individual
  instrumentation method is 2.49 percent. Allocation samples attribute zero bytes to the tracker, accumulator,
  registry, feedback accumulator, deferred join telemetry, or materialized index. The old capacity-length tracker,
  character-hashing loop, and per-probe join telemetry allocation are absent. The final complete query-evaluation
  suite passes 1,446 tests. The fresh all-theme result-bag audit passes all 117 cells, and the complete LMDB run passes
  every telemetry-related test among 2,230 tests. Its sole failure is the pre-existing dirty-worktree assertion that
  `SPARSE` must be a default benchmark parameter even though the current benchmark default omits it; that unrelated
  benchmark change remains untouched.
- [x] (2026-08-18 09:39Z through 11:03Z) Repair projected-distinct replica aggregation and mapped semi/anti cache
  costing. The retained reds in `initial-evidence.medical-q9-projected-distinct.txt` cover sparse lanes, unavailable
  lanes, all-positive skew, exact projection, conservative cache rejection, and the complete-store MEDICAL q9. The
  implementation now uses the overflow-safe all-lane mean, preserves unavailable samples, proves exact one-degree
  projections, carries point/lower/upper profiles, and uses the structural upper for memo eligibility and misses.
  The eight-test medical class and focused estimator/costing suites pass; the complete module run exposed and then
  passed the related cache-version assertion, with only the pre-existing dirty `SPARSE` benchmark-parameter mismatch
  remaining. JDK 26 completes q9 with one materialized RHS at 359.656 ms/op; the ten-iteration JFR qualification is
  363.781 ms/op and identifies a separate saturated runtime distinct-telemetry tracker as the remaining gap from the
  older 136.542 ms/op generic-Difference plan.
- [x] (2026-07-19 through 2026-07-25) Cut production planning over to the packed integer-ID Cascades representation,
  complete the then-supported tuple/scalar codec cutover, install primitive arenas and caches, and remove the legacy
  object memo. Milestone 1 still has to prove lossless coverage for the complete supported algebra surface.
- [x] (2026-07-22 through 2026-07-25) Introduce backend-neutral predicate-range facts and complete the first
  contradiction, tautology, finite-anchor, propagation, diagnostic, and cache-versioning slice.
- [x] (2026-07-26) Complete Frontier sketch phases 0 through 4: bounded query-index materialization, single-pass
  expansion, complete-center mapped probes, and conditionally unbiased multinomial resampling.
- [x] (2026-07-31 through 2026-08-04) Preserve Frontier state through supported transforms, contextual search,
  selected recipes, detached cache evidence, immutable costing events, stale validation, and state-specific LEO
  calibration; establish the exact continuation counterexample and 117-query audit harness.
- [x] (2026-08-05 through 2026-08-06) Repair learned-feedback commensurability, logical/physical identity,
  invocation-aware dependent costing, persistence, LastGood/quarantine/rollback, and allocation-free pre-bound
  runtime feedback. Keep production rollout in Monitoring until the gates below explicitly promote it.
- [x] (2026-08-06) Delete completed, superseded, and contradictory tracked ExecPlans and identify the surviving
  workstreams.
- [x] (2026-08-06) Merge every surviving workstream and research obligation into this self-contained plan; retire
  all other optimizer ExecPlans.
- [x] (2026-08-06) Research the canonical `papers2` corpus with a focused Thomas Neumann, Altan Birler, and Umbra
  reading set; resolve DPhyp/CD-E completeness limits, Indexed Algebra working-plan structure, state-preserving
  sketch composition, GroupJoin/mark-join alternatives, execution-aware cost dimensions, and LEO-plus safeguards
  into this plan and `papers2/papers/docs/neumann-birler-umbra-query-optimizer-research.md`.
- [x] (2026-08-06) Review this sole ExecPlan against its source APIs and evidence; repair the rewrite/search dependency,
  define wide DPhyp and resource-limit contracts, specify the cost algebra and value-flow merge semantics, couple
  catalog safety to runtime proofs, select the LEO-plus baseline/OOD policy, and replace stale commands.
- [x] (2026-08-06) Milestone 0: freeze the complete interface, scalar-boundary, rule, physical-alternative, evidence,
  learning, and test inventory; preserve the continuation contract and capture bushy DPhyp, MINUS, self-JOIN,
  self-LEFT_JOIN, global GROUP, and joint-NDV GROUP red contracts in `initial-evidence*.txt`; install the immutable
  closed-world descriptor/surface ledgers and pass all three `OptimizerRuleCatalogTest` architecture checks.
- [x] (2026-08-06) Milestone 1: finish the lossless packed representation with a typed, child-visible unsupported
  boundary; add the primitive-array `PackedBindingFlowArena` and `PackedPathFactIndex`; model UNION phi, OPTIONAL,
  compatible JOIN, projection, extension, GROUP, subquery, and MINUS occurrences; pass the five value-flow contracts,
  all 15 codec tests, and all three closed-world catalog checks. Preserve the original red reports in
  `initial-evidence.m1-binding-flow.txt`.
- [x] (2026-08-06) Milestone 2: couple runtime alternative installation to reusable applicability/proof results;
  generate and verify the closed-world production/research catalog; repair MINUS filter movement and heterogeneous
  self-JOIN/LEFT_JOIN idempotence; complete predicate-range DATATYPE/LANG/LANGMATCHES, finite no-filter anchor,
  calendar/numeric/boolean, OPTIONAL, and SERVICE-boundary contracts. Preserve the red reports in
  `initial-evidence.m2-*.txt`; the final range and catalog classes pass 39 and 6 tests respectively.
- [x] (2026-08-06) Milestone 3: standardize state-preserving estimation and implement the normative multidimensional
  cost algebra. The 41-dimension registry and primitive interval-vector arena keep physical resources separate from
  cardinality state and pass all six composition-mode contracts. Frontier payload/record v2 deduplicates exact rows
  across logical lanes, expands them losslessly during bounded reads, preserves mixed exact-heavy/sample records,
  retains bidirectional and paired design/audit behavior, and reads v1 block/record generations. Preserve the red
  reports in `initial-evidence.m3-cost-algebra.txt` and `initial-evidence.m3-payload.txt`.
- [x] (2026-08-06) Milestone 4: install exact continuation-equivalence classes, resource-accounted Pareto frontiers,
  and explicit incomplete-search statuses everywhere a winner is retained. Dense and sparse-long memo states now
  retain immutable continuation alternatives, bushy DPhyp candidates are recursively materialized from their exact
  costing events, Pareto sidecars preserve resource-distinct plans, and every byte-accounted allocation degrades to
  an executable incumbent with an explicit incomplete status instead of publishing a false optimum. The focused
  memo, continuation, cache, and resource suite passes 45 tests; the dense kernel contract passes all 9 tests.
- [x] (2026-08-07) Milestone 5: run proof-checked rewrite, hypergraph construction, wide DPhyp, and Cascades
  reactivation to one certified fixpoint across dense, sparse-long, and multiword widths. The arbitrary-width
  `PackedNodeSetArena` and node-set-ID receiver match independent DPsub oracles and the one-word kernel at 16/17,
  63/64/65, and 127/128/129 boundaries; all three subset kernels consume bushy CSG/CMP pairs and retain exact
  continuation evidence. The codec now jointly drains monotone fact revisions and idempotent proof rules, so an
  alias rewrite that exposes a ranged source value reactivates range anchoring before graph construction. Cascades'
  existing dependency worklist re-costs changed parents to a DAG fixpoint. Preserve the new red in
  `initial-evidence.m5-fixpoint.txt`; the range/DPhyp closure suite passes 46 tests and the focused kernel/resource
  suite passes 23. A profile-driven collision-checked equivalence index reduced the exact 17-factor continuation
  contract from 117.4 seconds to 1.934 seconds without pruning a state.
- [x] (2026-08-07) Milestone 6: integrate LEO-plus logical and physical residual decisions with the stateful Pareto
  cost path. Primitive five-dimensional feature envelopes, exact applicability, 3/32 support gates, robust empirical
  p99 OOD rejection, predictive intervals, interval-only censoring, exact-fact priority, pinned decisions, telemetry,
  and persistence format 21 now preserve identity across planning, execution, and restart. Preserve the OOD red in
  `initial-evidence.m6-ood.txt`; the six focused learning/lifecycle classes pass 124 tests. The frozen chronological
  replay under `profiles/lmdb-opt/leo-plus-prototype/` passes synthetic q-error, regret, coverage, OOD, restart, and
  period-two gates. Production remains in Monitoring because the matched full-corpus planning-time/allocation gate
  has not yet been evaluated; no threshold was weakened.
- [x] (2026-08-07) Milestone 7: make cache replay, plan explanations, decision certificates, and selected recipes
  lossless under the unified state/cost/frontier contracts. Selected decision certificates now detach every
  candidate's 41-dimension interval vector, complete continuation identity, child composition, rule proof, Frontier
  state, and alternatives. Current-generation replay shifts candidate dimensions from immutable event deltas,
  refreshes state ordinals, reports stable changed-dimension IDs, and fails scalar-only or incomplete certification
  closed to one fresh plan. Store-owned segmented admission accounts detached certificate bytes; strict hits remain
  provider-free, collision-checked, bounded, and single-flight. Cold and cached materialization publish matching human
  and JSON explanations covering catalog decisions, CSG/CMP identities, Pareto outcomes, Frontier/learning state,
  validation, and final root policy without recosting. Preserve the three M7 reds in
  `initial-evidence.m7-cache-vector-replay.txt`, `initial-evidence.m7-cache-failclosed.txt`, and
  `initial-evidence.m7-explanation.txt`; the focused cache/recipe/memo suite passes 41 tests.
- [ ] **[paused] Milestone 8:** close all 117 semantic and plan-quality cells, then meet the matched planning and execution
  performance gates without heuristic suppression. The authoritative 117-cell result-bag campaign is closed. The
  retained dense-seed, bijective exact-projection, and segmented emission-buffer slices reduce matched SPARSE q6
  pooled steady planning from 106.170 to 93.425 ms/op and allocation from 70,724,161 to 52,625,495 B/op; the two
  tenfold aggregate gates and remaining p95, synthetic, regret, and fixed-plan execution gates remain open.
  The latest exact q0 slice keeps pure variable-alias Extension chains primitive and removes the mapped-index
  callback allocation exposed by that faster path. Matched ENGINEERING q0 steady planning is 26.721 ms/op with
  10,509,647 B/op, 2.66 percent faster and 0.74 percent smaller than the preceding cumulative baseline; all 106
  Frontier planning integrations pass. A scope-aware scalar FILTER binding projection was implemented behind direct
  and nested-correlation contracts, then rejected and reverted after matched measurement: allocation-only profiling
  suggested a 0.92 percent reduction, but the more robust 100-iteration CPU profile regressed latency by 19.84 percent
  and allocation by 0.20 percent. The failed construction, red/green, and benchmark artifacts remain preserved.
  Exact conjunctive EXISTS/NOT EXISTS predicates now build one complete primitive correlation domain when every RHS
  leaf and join is resident `DATABASE_EXACT`; sampled large domains remain honest and reuse one lazy exact-outcome
  memo per contextual logical group and correlation surface across ordinary, streaming, memoized, and materialized
  framings. Matched TRAIN q8 steady planning falls from 102.936 to 71.536 ms/op (1.439x) and allocation from
  72,561,669 to 61,842,669 B/op (14.77 percent), while evaluator-fallback CPU samples fall 51.00 percent; all 107
  Frontier planning integrations and all 16 packed-codec tests pass. The subsequent 117-cell matched campaign has no
  failed supervised process. Across the 111 nonempty baseline/candidate pairs, saturated fixed-corpus speedup rises
  from 6.407x to 6.808x and equal-query geometric speedup from 3.492x to 3.681x. This confirms the slice at corpus
  scale, while both tenfold gates remain open; the next profile target is the completed cell with the lowest matched
  speedup, HIGHLY_CONNECTED q8 (1.309x, 87.317 ms/op saturated mean). That profile exposed sampled
  single-statement EXISTS as exact cursor work hidden behind RDF-value decoding and a fresh iterator per key. A
  primitive term-ID kernel now reuses one pinned best-index cursor batch, stops at the first legal match, preserves
  named-context/repeated-variable constraints, and keeps the evaluator fallback for ineligible datasets or inferred
  statements. Matched q8 steady planning falls from 51.195 to 44.177 ms/op (13.71 percent) and allocation from
  35,288,568 to 27,187,281 B/op (22.96 percent); EXISTS CPU samples fall 59.43 percent. The focused three-contract
  suite and all 108 Frontier planning integrations pass. A follow-up attempt to answer exact conditional probes from
  any deterministically selected design/audit lane was rejected and reverted: it shifted 1.2K q8 probes away from the
  pinned snapshot cursor, but repeated mapped range lookup raised steady latency by 3.95 percent and allocation by
  0.50 percent. The red/green and benchmark artifacts remain as negative evidence.
  A subsequent ENGINEERING q9 profile exposed checked per-component access while replaying exact memoized quads.
  Keeping the memo's owned packed storage borrowed for the replay loop removes that duplicate indexing without
  changing ownership or output order. Two candidate profiles reduce inclusive cached-replay samples from 11,982 to
  11,547 and 11,061, total planning samples by 2.1 and 3.0 percent, and median planning from 64.56 to 63.14 and
  61.86 ms/op; the matching exact-probe contract passes before and after the change. HotSpot evidence then showed
  that q9 still made about 87,000 inlined proposal decisions for roughly 4,000 selected draws. Exact memo runs now
  use a fused inverse CDF and visit selected row groups rather than every equal-weight row. ENGINEERING q9 mean and
  median planning fall a further 24.1 and 26.0 percent, planning CPU samples fall 30.8 percent, and cached replay
  samples fall 66.2 percent; both complete affected integration classes remain green. Once every retained context
  stratum is resolved, the survey now bulk-counts the remaining equal-weight tail instead of revisiting each row;
  q9 mean and median fall another 10.50 and 9.76 percent with flat allocation. A structurally complete 117-cell v7
  campaign then finished without failures, but concurrent WindowServer/network-location work inflated concentrated
  early cells and reduced the diagnostic aggregate to 6.056x fixed-corpus and 3.396x geometric speedup. It is retained
  as non-authoritative host-contamination evidence, not as an acceptance result. Deterministic scalar FILTERs
  initially memoized exact true/false outcomes by contextual logical group and their sorted direct-variable term-ID
  tuple while excluding EXISTS/subquery correlation. The later mixed-conjunction repair below expands that identity
  to the full deterministic condition surface. Two matched q9 profiles improve mean by 3.73 percent, median by 6.62 percent, and steady last-20 by
  9.81 percent with effectively flat allocation; the focused multi-variable/unbound contracts and all 108 Frontier
  integrations pass. Finally, exact primitive correlation domains retain their lazily decoded
  `FiniteRelationEstimate` in the existing collision-safe `(stateId, bindingNames)` domain map. Independent q9
  profiles improve mean by 3.11/4.73 percent, median by 3.12/5.09 percent, steady last-20 by 4.62/5.17 percent, and
  allocation by 4.24 percent; materialization/value-decoding samples fall 32.41 percent. The explicit repeated-cost
  contract, all 31 finite-surface tests, and all 108 Frontier integrations pass. Four subsequent q9 proposals were
  measured and rejected without weakening candidate enumeration: extensional exact-relation equality produced only
  one useful hit and regressed mean latency 2.19 percent; rebasing calibrated appends onto their raw payload regressed
  mean latency 3.40 percent; an ID-native bounded correlated surface regressed mean latency 1.17 percent; and a
  memory-accounted lineage-derived payload index passed its red/green contracts but regressed mean latency 1.92
  percent with flat allocation and no reduction in `extendInner` samples. All four implementations and temporary
  contracts were reverted; their initial evidence and v12--v15 profiles remain as negative evidence. Allocation
  profiling then identified LWJGL's per-match `MDBVal.mv_data()` wrapper as 2,709 allocation samples at the hot LMDB
  cursor decode line. The retained bounded native-address quad decoder preserves all nine varint widths and index
  permutations while avoiding that wrapper. A same-host A/B/A q9 comparison improves mean, p95, and steady last-20
  planning latency by 1.53, 2.13, and 1.79 percent respectively while reducing steady allocation 16.49 percent;
  `DirectByteBuffer` samples fall from 2,741 to 24 and iterator-owned allocation samples from 2,723 to 13. The focused
  native bounds contract, 26 cursor/storage tests, and all 108 Frontier integrations pass.
  The subsequent idle-host v8 matrix completed all 117 cells. Against the 111 valid baseline pairs it improves the
  saturated fixed-corpus aggregate to 6.955x and the equal-query geometric aggregate to 3.743x; both tenfold gates
  therefore remain open. The next q0 CPU profile showed that mapped Frontier query-index reads still paid
  `DirectByteBuffer.get*` checks for every primitive field even though the immutable segment already retained its
  native address. Reading the owned address with explicit segment bounds removes those wrappers without changing the
  file format. Same-host A/B/A improves ENGINEERING q0 steady mean/median/last-20 by 14.04/14.22/21.13 percent and q9
  by 2.64/2.99/4.13 percent; the 10 query-index contracts pass before and after. A follow-up hash index for identical
  lineage-derived transforms passed its focused red/green and payload suites but was rejected and reverted because
  q0 regressed 0.18 percent by steady mean and 3.02 percent over the last 20 despite a 2.15 percent q9 gain.
  Finally, JDWP tracing proved that assured trivial-alias commutation exposed a second exact dense lattice which
  strictly covered the opaque original lattice. The logical rule now records that proof edge, the scheduler orders
  covering regions first, retains the original as a fallback until cover completion, and omits only the exactly
  covered search. The alias-only red now observes one lattice instead of two; all 76 packed search tests pass. Matched
  q0 A/B/A improves steady mean/median/last-20 by 6.34/6.51/6.21 percent and allocation by 16.88 percent, while q9 is
  within +0.36 percent with flat allocation. The matched idle-host v9 campaign then completed all 117 supervised
  cells without a timeout or failure. Across the same 111 valid baseline pairs, saturated fixed-corpus speedup rises
  from 6.955x to 7.148x and equal-query geometric speedup from 3.743x to 3.849x; candidate steady time falls 2.70
  percent versus v8. Both tenfold gates remain open: the candidate sum is 5,166.530 ms against the 3,693.072 ms
  threshold. TRAIN q8 is now the slowest completed cell at 104.552 ms saturated mean, so it is the next profile
  target. The process-cold saturated-sample p95 matrix also confirms that none of the 111 comparable cells yet meets
  the 5 ms goal, so those absolute gates remain explicitly open rather than being inferred from the aggregate gain.
  The TRAIN q8 profile attributed 41.86 percent of planning samples to the generic RDF evaluator's `JoinIterator`
  while answering a sampled two-statement EXISTS. A retained arbitrary pure-conjunction kernel now joins primitive
  LMDB term IDs over one pinned multi-lane cursor batch, supports EXISTS/NOT EXISTS, deep and disconnected conjunctions,
  repeated variables, named-graph joins, and store-default scope, and declines effectful or consumed-alias extensions.
  The focused A/B/A improves steady mean/median/last-20 by 21.93/23.54/24.02 percent, p95 by 10.20 percent, and
  allocation by 38.83 percent; the candidate profile contains no `JoinIterator.getNextElement` samples. Both focused
  integration contracts, all 11 snapshot-source tests, and all 110 Frontier planning integrations pass. The idle-host
  v10 matrix then completed all 117 supervised cells. Against the same 111 valid baseline pairs, TRAIN q8 improves
  15.91 percent, candidate steady sum falls from 5,166.530 to 5,125.077 ms, fixed-corpus speedup reaches 7.206x, and
  equal-query geometric speedup reaches 3.865x. Both tenfold gates remain open; the sum still needs a 27.9 percent
  reduction, and no comparable saturated p95 is below 5 ms. SOCIAL_MEDIA q9 is now the slowest cell at 91.811 ms and
  is the next profile target for a mechanism shared across the corpus. Its profile exposed payload canonicalization
  as the largest removable non-enumeration stack. An allocation-free in-place MSD radix preserves the exact unsigned
  tuple/raw-weight comparator while reducing inclusive canonical-sort samples 44.39 percent and matched q9 steady
  mean 3.81 percent. A follow-up SPARSE q10 profile identified an immediate defensive copy of freshly built exact
  primitive correlation arrays; transferring those exact-sized query-local arrays into the immutable relation
  reduces allocation 3.30 percent and matched mean 2.63 percent. An occupied-slot side index for the scratch hash
  table was measured and rejected after it increased allocation 1.66 percent and worsened endpoint median/last-20.
  These slices preserve the exhaustive candidate set. A fresh q6 profile confirms that the remaining shared stack is
  exhaustive dense append costing and scalar finite-surface estimation; q10 additionally spent 1,847 CPU samples
  sorting exact leaf payloads after coalescing. The retained exact-leaf accumulator now radix-sorts compact row
  indexes in its otherwise-dead hash table and writes each full tuple once in canonical order, improving matched q10
  mean 3.19 percent and p95 13.07 percent with flat allocation. The next q10 profile exposed branch-and-scatter
  materialization from a half-empty primitive correlation hash table. Keeping rows dense behind encoded open-address
  buckets eliminates the `PrimitiveCorrelationKeyTable.relation` leaf (2,268 exclusive samples to zero), improves
  matched mean/median/p95/last-20 by 9.97/9.89/11.37/11.70 percent, and reduces allocation 4.14 percent. All 110
  Frontier planning integrations remain green. The next action is the full v11 fixed-corpus campaign containing all
  four retained post-v10 representation repairs; aggregate and absolute tenfold gates remain open until measured.
  The first v11 attempt completed 117/117 cells but is rejected as acceptance evidence: a separate checkout's LMDB
  Surefire JVM consumed 97.2 percent CPU throughout the matrix, its Maven parent remained active, and two macOS
  storage-analysis processes consumed 80.6 and 58.0 percent CPU. The contaminated run reports 6.761x/3.623x even
  while corpus allocation falls 2.86 percent and the targeted q10 cell improves 11.92 percent, confirming that the
  broad latency shift is environmental. Its immutable artifacts remain under
  `candidate-planning-cells-v11-representation-repairs-20260810`; the exact jar must be rerun after the host is idle.
  While the host remains contested, the q6 planning profile exposed another exact lattice-wide duplication:
  `LmdbFiniteSurfaceCache` keyed completed factor multisets order-independently but every different last-factor
  orientation still rescanned LMDB and decoded a new, extensionally equal RDF relation. A focused contract first
  failed on distinct relation identity and then on the repeated three-row scan. The cache now reuses the immutable
  completed relation and reconstructs exact matched-prefix multiplicity from its projection over the cached exact
  prefix, while preserving connectivity eligibility, bag frequencies, reordered variable columns, unbound prefix
  cells that the final factor legally binds, and the first transition's real scan telemetry. The focused contracts,
  54 finite-surface/memo contracts, all 110 Frontier planning
  integrations, and the all-117-query Theme bag/evidence audit pass. Timing evidence is deliberately pending: the
  unrelated Surefire JVM remains near 99 percent CPU and Spotlight separately reached 90 percent, so the immutable
  relation-only and completed-surface candidate jars are retained for an idle-host bracket rather than measured under
  known contamination. The generated 300-query estimator corpus then exposed two independent defects rather than one
  scalar estimation heuristic. Selected decision certificates first collapsed distinct exact continuation occurrences
  sharing the same scalar memo cell; the certificate now assigns a deterministic duplicate decision occurrence while
  retaining the shared candidate set. The remaining q44 zero defect came from childless `EMPTY_SET` alternatives:
  global structural interning kept the first proved-empty filter group and discarded the equally valid empty terminal
  for a commuted group. Empty terminals now use the interner's collision-safe group-scoped identity while canonical
  logical identity remains global. The focused memo continuation contract, both disconnected commutation directions,
  all 41 predicate-range contracts, warm-cache q44, positive q74, and the complete generated corpus pass. Preserve the
  red/green chain in `initial-evidence.m8-qerror-corpus.txt`. Deterministic plan-quality/search checks also pass 76
  packed-search, 11 exhaustive DPhyp, 17 wide Frontier-kernel, and 6 fixed-snapshot alternative-execution contracts.
  Synthetic JMH, full-corpus regret, fixed-plan execution timing, and the v11 rerun remain pending while an unrelated
  checkout's Surefire JVM continues to consume roughly one full CPU core; contaminated timing is not acceptance.
  The frozen regret protocol now has a repository-owned supervised and resumable campaign runner. It inventories each
  Theme cell, executes every stable alternative in its own hard-deadline process, rejects stale resume identities,
  compares solution-bag fingerprints, and withholds median/p95 regret until every inventoried alternative completes.
  Its three red-to-green contracts are preserved in `initial-evidence.m8-plan-quality-campaign.txt`; a real
  SOCIAL_MEDIA q4 inventory is `EXACT_COMPLETE` with four stable Pareto alternatives, and a second invocation resumes
  without opening the store. A synthetic 4/8 diagnostic was also rejected after the unrelated JVM returned to 97.4
  percent CPU during the check, so no timing gate was closed from that sample. The remaining frozen campaign
  surfaces now have repository-owned fail-closed runners. `run-packed-synthetic-campaign.py` pairs each JMH factor
  cell with a source-launched completion-status audit that works against both the exact baseline jar and the current
  completion API, keeps exact and incomplete searches separate, and reports both the process-cold sample and the
  manifest's saturated samples. `run-theme-execution-campaign.py` enforces the six exact `FIXED`-lifecycle cells,
  retains raw time/allocation samples, applies the strict no-higher-median rule, and reports one-sided exact
  permutation classifications separately. Their five red-to-green contracts are retained in
  `initial-evidence.m8-synthetic-campaign.txt` and `initial-evidence.m8-fixed-execution-campaign.txt`; a factor-four
  audit reports `EXACT_COMPLETE`, rows 240, and cost 420 for both revisions. The synthetic and fixed-plan timing
  gates remain open until an uncontested host run.
  The planning matrix now also has an identity-safe summary layer. It rejects stale jar/method/theme/query/settings
  results and cross-jar resume, derives exact request-cold and saturated samples, and retains partial timeout samples
  only as mathematical lower bounds with unbounded upper bounds. Its three contracts and frozen-v10 cross-check are
  in `initial-evidence.m8-planning-summary.txt`: the exact 111-pair projection reproduces 7.205886199x fixed-sum and
  3.865096218x geometric speedup, while the all-corpus fixed-sum interval is
  [7.431297761x, unbounded) and the geometric interval is [0, unbounded). The deliberately incomplete 117-cell v10
  directory is rejected as a final 234-cell result because it contains no validated-cache lane. The original frozen
  baseline artifacts remain available at `/private/tmp/rdf4j-frontier-baseline.jKNc2y`; immutable copies pin LMDB
  hash `aa71ee49c2c04f39b4bc99d2bd1da989616f77b74970c42b11a3e6cde35ab9c2` and query-evaluation hash
  `bdc323f88d21291fd688c224e4408a4d302a3b2295c7c20be8d193372f64b698` under `/tmp`. A profile-backed, strictly
  query-local last-hit cache for repeated mapped-index center-completeness lookups has matching pre/post greens and
  1,142 q6 planning-stack hit-proof samples in `initial-evidence.m8-center-completeness-last-hit.txt`; retention is
  conditional on the pending idle-host A/B/A bracket.
  The backend-neutral packed planner has also completed a profile-guided allocation/locality pass without removing
  any candidate or lattice transition. Retained slices allocate optional physical metadata, decision traces,
  cost-vector points, logical-rule proof scratch, JOIN cost scratch, and binding-flow columns only when their exact
  source shape requires them; canonical groups are prepared directly; logical and physical expressions intern in one
  collision-checked pass; immutable implementations are not replayed; and winner-table lookup is deferred until a
  Pareto candidate actually requires it. The complete 77-test packed-search suite and focused binding-flow contracts
  remain green. The final exact binding-flow A/B/A bracket is latency-neutral (-1.4/+1.8 percent around baseline) and
  deterministically removes 1,192 B/op. Preserve the slice evidence in the `initial-evidence.m8-*` files and the
  factor-four profiles under `profiles/lmdb-opt/final-campaign/performance/`.
  A fresh supervised synthetic campaign on jar SHA-256
  `7f7c2148fc007dab11d72abd9464bdf0b552df7f259dab61ea9f01f72ac0404f` reports `EXACT_COMPLETE` for factors four
  and eight. Factor eight passes at 1.569334 ms saturated p95 and 109,032 B; factor four allocation passes at 60,816 B
  but its contended-host saturated p95 is 0.721625 ms, so the 0.5 ms timing gate remains open. Process-cold startup is
  reported separately. The exact current LMDB campaign jar is frozen at
  `/tmp/rdf4j-lmdb-jmh-current-a9801f34.jar` (SHA-256
  `a9801f34fe657a2afbe5ad87044dd6127a19adfb8ed654a2510092b58c5e2a64`); its resumable 117-cell stable-alternative
  inventory is in progress under `performance/plan-quality-v2`. Q-error is closed by the green generated 300-query
  corpus; regret execution, fixed-plan execution comparison, factor-four timing, and both tenfold planning aggregates
  remain open.
  The synthetic timing surface is now scientifically separated into a one-invocation process-cold JVM and a distinct
  20-warmup/20-measurement saturated JVM. A JDK-25 compilation trace proved that the former zero-warmup tail was still
  compiling optimizer and materializer methods through its nominal saturated samples. The version-two runner rejects
  cross-lane warmup mismatches, retains successful audit/cold evidence when a saturated process is censored, and keeps
  the stricter compatibility gate as the maximum of cold and saturated p95 rather than letting a larger sample count
  hide cold startup. Its six contracts and red/green evidence are in `initial-evidence.m8-synthetic-lanes.txt`.
  On candidate query-evaluation jar SHA-256
  `b8eb57fa04dcf560179bd032a6faa9c80411710ddea13fbc10439b9befcd4b27`, all 4/8/16/17/32/64/65/128 audits and timing
  cells complete `EXACT_COMPLETE`. Factor four passes the saturated gate at 0.369750 ms p95 and 45,680 B/op; factor
  eight passes at 0.447333 ms and 83,640 B/op. Process-cold startup remains separately reported at 92.914 and 93.683
  ms. The first complete candidate exposed a true 64-to-65 multiword cliff and censored factor 128. Async-profiler
  attributed 70.45 percent of factor-65 CPU to reconstructing both physical prefixes inside scalar costing, where
  neither prefix is consumed. Deferring that exact reconstruction to provider-backed costing preserves the matching
  65-factor scalar and provider contracts, reduces factor-65 saturated p95 from 1,049.903 to 79.550 ms, and converts
  factor 128 from a 75-second censored lane into an exact 1,011.710 ms p95 cell. The complete version-two baseline is
  retained beside the candidate; wider cells have no absolute five-millisecond acceptance gate and remain reported
  rather than heuristically suppressed. Full-corpus planning, regret execution, and fixed-plan execution remain the
  acceptance blockers.
  The next idle-host campaign exposed a concentrated regression rather than a broad planner slowdown: PHARMA q2 rose
  from the pre-alias 26.078 ms and 15,295,656 B/op to 358 ms and 742.8 MB/op after primitive alias factors made a
  scalar predicate and correlated EXISTS one top-level AND. The first exact whole-condition memo reduced that to
  139.695 ms and 241.3 MB/op, but allocation JFR still showed one RDF iterator reopening per distinct correlation
  key. The retained general repair flattens only repeatable top-level conjunctions in original order, evaluates scalar
  conjuncts with ordinary SPARQL filter-error rejection, and routes each eligible EXISTS/NOT EXISTS term through its
  existing primitive statement/conjunction/domain kernel. Probe memos now include the canonical RHS relation identity,
  preventing two different subqueries with the same correlation surface from aliasing. The matched 5-warmup/20-sample
  PHARMA q2 profile is 23.168 ms mean, 37.764 ms p95, and 14,021,941 B/op: 15.5x faster and 98.1 percent less allocated
  than the regressed implementation, while slightly improving the pre-regression allocation. The red/green is in
  `initial-evidence.m8-conjunctive-mixed-exists.txt`; duplicate bags, nested AND, multiple distinct RHS identities,
  NOT EXISTS, optional/unbound masks, and scalar errors pass, and the complete 114-test Frontier planning integration
  class is green. The full corrected fixed-corpus rerun remains the active acceptance action.
  That rerun first exposed a false cache-miss classification for freshly parsed property paths. Canonical cache
  identity now assigns deterministic ordinals to parser-generated anonymous variables and safe internal Extension
  targets while retaining exact packed-query collision checks and preserving root-visible binding names. A second
  failure showed that admitting the plan's observational lifecycle envelope advanced the LEO planning revision and
  immediately invalidated the plan being compiled. Planning identity now advances only with learned feedback that can
  change a later decision; persistence dirtiness remains independently versioned. The exact SOCIAL_MEDIA q4
  freshly-parsed cache contract, all 33 packed cache contracts, all 9 LMDB cache contracts, and all 88 feedback
  lifecycle contracts pass. Candidate jar SHA-256
  `c826f0fd0aa226f33480c3ff14fb9aafa398f353690163c1240bc5fe54c22994` reduces q4's seventh nominal cache sample
  from 18.505 ms to 4.676 ms, but a 15-by-1-second AverageTime profile proved that both seven-shot tails were still
  compiling: the independently warmed cache path is stable at 0.393179 ms/op.
  The same diagnosis invalidates the original Theme matrix's classification of zero-warmup samples two through seven
  as saturated. MEDICAL_RECORDS q6 moves from the nominal 15.793 ms tail mean to 1.258 ms/op after ten time-based
  warmups, while the matched legacy jar measures 11.575 ms/op in the same diagnostic shape. The original frozen
  manifest remains byte-for-byte intact. `planning-manifest-v2.json` supersedes only its planning protocol with one
  independently supervised cold single shot and a distinct 10-warmup/7-measurement AverageTime JVM. The runner and
  summarizer reject stale jar, lane, mode, duration, and warmup identities; all eight harness contracts pass,
  including preservation of valid cold evidence when a saturated lane is censored and rejection of mismatched timing
  identity. The first real q4 cell records 285.374 ms cold versus 0.391845 ms/op saturated. The complete matched v2
  baseline and candidate planning matrices are now the active acceptance action.
  A provenance audit of the queued regret campaign found that stable alternative IDs alone did not authenticate a
  resumed inventory: an older jar could retain the same stable IDs and be relabeled by a newly written campaign
  index. Resume and aggregation now compare the producing classpath artifact by SHA-256 and require the exact store,
  sampling, warmup, measurement, timeout, theme, query, alternative, and output command identity. Successful status
  from another artifact is reported as stale. Preserve the red/green chain in
  `initial-evidence.m8-plan-quality-provenance.txt`; all five plan-quality harness contracts pass. Existing v3
  inventory remains historical evidence and will not be silently reused by the current jar.
  The first complete dual-lane v2 candidate matrix then closed all 468 supervised processes. Its 109 exact uncached
  pairs pass both relative gates at 16.655x fixed-corpus sum speedup and 10.539x equal-query geometric speedup. The
  frozen baseline remains incomplete because seven cells are honestly censored and six old JMH-internal timeouts were
  recorded by their process supervisor as successful exits; the summarizer therefore withholds an overall acceptance
  claim instead of relabeling missing baseline evidence. More importantly, only 23.93 percent of candidate uncached
  cells were below five milliseconds and the validated-cache p95 was 30.672 ms. Diagnostics proved that deterministic
  deadline-free `INCOMPLETE_WORK_LIMIT` results were discarded after every AUTO request. A separate approximate lane
  now reuses only an identical work/memory budget, never satisfies an exact lookup or stale validation, and keeps
  deadline/resource/unsupported outcomes transient. Preserve its red in
  `initial-evidence.m7-approximate-plan-cache.txt`; all 35 generic cache contracts and all nine resource-budget
  contracts pass. The SOCIAL_MEDIA q9 validated-cache cell fell from roughly 45.48 ms to 0.574182 ms mean and
  1,705,045 B/op.
  Allocation JFR on that new hot path attributed 54.31 percent of `LmdbCascadesOptimizer` allocation to rerunning the
  unified semantic root estimate after every strict plan hit. The exact-zero safeguard is now an explicit opt-in cold
  `PackedRootCardinalityCertifier`: LMDB evaluates it once, the collision-checked plan entry retains the certified
  rows under data/LEO/Frontier/provider revisions, and ordinary/proxy cost models receive no new call. The provider
  version advances to 29. Preserve the red in `initial-evidence.semantic-root-cache.txt`; all 36 generic cache and 11
  LMDB cache contracts pass. Candidate jar SHA-256
  `d914808b062f825702429894dd60423725c9b72326f9df5ad35b85ac5070af73` measures q9 at 0.434426 ms mean,
  0.445874 ms p95, and 1,184,367 B/op, clearing the strict validated-cache timing gate for that cell with 24 percent
  lower mean latency and 31 percent less allocation than the approximate-cache-only jar. Its complete 468-cell
  dual-lane v2 campaign is now running under
  `performance/candidate-planning-dual-lane-v2-v17-root-certificate-20260811`.
  Six frozen baseline lanes had also been misclassified as invalid because JMH's own 60-second operation timeout
  exits the outer process successfully and leaves an empty result array. `planning-manifest-v3.json` supersedes only
  that accounting rule: the summarizer now requires exit zero, no supervisor timeout, the exact frozen `-to 60s`
  command, and JMH's explicit timeout marker before recording a censored lane. It never invents a sample; malformed
  JSON without that complete identity remains invalid. Preserve the red in
  `initial-evidence.m8-jmh-operation-timeout.txt`; all nine planning-harness contracts pass. Re-summarizing the v15
  candidate makes the baseline complete-with-censoring and proves the fixed-sum lower-bound gate, while geometric
  and absolute p95 gates remain explicitly open.
  The completed v17 root-certificate matrix supplies all 468 candidate cells and all 117 semantic pairs. Its
  fixed-sum speedup lower bound is 18.330x and the 109 exactly matched pairs have a 10.788x geometric speedup, but the
  formally censored all-query geometric interval remains `[0, +inf]`; only 23.08 percent of uncached cells are below
  five milliseconds, none are below 0.5 milliseconds, and validated-cache p95 is 0.515134 ms. The summarizer
  therefore correctly leaves acceptance false. Two subsequent general repairs retain the exact search surface while
  reducing its dominant q10 work. First, the canonical complete incumbent seed now owns the single full fallback and
  every access-boundary improvement schedules only its required prefix plus one continuation. A matched A/B/A run
  measures 36.561 ms and 45.49 MB/op against 43.507 ms and 65.97 MB/op for the frozen predecessor. Second, the
  collision-safe store-owned exact-transform cache now represents deterministic binary finite joins with the
  complete second payload, structural output mapping, and detached result; search and candidate enumeration still
  run, while alpha-equivalent sessions avoid rebuilding the same immutable join payload. Its matched A/B/A q10 means
  are 32.168 and 31.935 ms with about 41.55 MB/op, versus 36.336 ms and 45.48 MB/op for the immediately bracketed
  predecessor. The full `LmdbFrontierPlanningIntegrationTest` remains green at 117/117. Candidate jar SHA-256
  `d0ac60409af9a4d3f51cab8b1b91e6735288b2ad5f7604e1f2c60135d831da7c` is the next complete planning and regret
  campaign artifact.
  The ensuing profile-driven exact-surface campaign retained query-local lazy binding universes, DPhyp reachability
  scratch, recyclable packed costing/physical arenas, lazy tuple growth, primitive unary exact-transform reuse,
  lazy hash-correlation materialization, a revision-safe bounded leaf-payload cache, joined OPTIONAL reuse, trace
  restore fast paths, bulk candidate-metric copies, and identity-fast canonical object pools. Rejected variants
  remain recorded in `initial-evidence.txt`; notably, copying every sparse metric slot and rebuilding immutable
  evidence-profile maps reduced neither measured latency nor the complete search surface. The retained v42 artifact
  `/private/tmp/rdf4j-lmdb-jmh-candidate-pool-fastpaths-v42-20260812.jar` (SHA-256
  `3510c672bdcb71f23340ab2694121b9ffc83c46a8e2a7637af5260d524593fe3`) completes a quiet-host 117/117 uncached
  saturated matrix with no timeout. The sum of cell means is 701.715377 ms/op; 56/117 cell p95 values are below
  five milliseconds, none is below 0.5 milliseconds, and the corpus p95 of cell p95 values is 15.190793 ms. This
  materially improves the earlier exact-surface matrix but leaves both absolute gates open. The next general slice
  begins component-level reuse for every snapshot-pure relation transform without bypassing enumeration or costing.
  Projection is the first TDD member: an alpha-equivalent session now reuses the collision-checked detached payload,
  while lineage-calibrated inputs continue to bypass shared reuse. Its focused red/green and all 11 exact-transform
  cache invariants pass. A same-host candidate/control/candidate q6/q11 bracket reports combined mean improvements
  of 1.03 and 1.90 percent at the two candidate endpoints with essentially flat-to-lower allocation; the isolated
  slice is retained provisionally and will be judged as part of the generalized transform-cache bundle. The completed
  bundle now gives projection, DISTINCT, heterogeneous UNION alignment/composition, INTERSECTION, exact JOIN,
  MultiProjection, pure EXISTS, and MINUS collision-checked detached reuse while keeping lineage calibration and
  physical costing session-local. Every member has focused red/green evidence, the binary descriptor rejects missing
  or different second payloads and topology, and all 130 Frontier planning integrations pass. Candidate v46
  `/private/tmp/rdf4j-lmdb-jmh-generalized-transform-cache-v46-20260812.jar` (SHA-256
  `8aff6140b4d62431d4c078f40531ef62ddd1c0209bc0d86d12ca7b4f95b05c77`) beats the frozen v42 control in a same-host
  q6/q11 candidate/control/candidate bracket: endpoint-average q6 mean/allocation improve 5.126/1.415 percent, q11
  improves 0.469/0.452 percent, and the combined endpoint-average improves 0.892/0.555 percent. The bundle is retained;
  the complete corpus still must establish its acceptance impact. A subsequent query-local binding-fact cache keeps
  planner-mask and relation-output identity distinct, while sharing immutable binding-name facts across canonical
  provider subtrees. All 130 LMDB planning integrations pass in 114.805 seconds. Candidate v47
  `/private/tmp/rdf4j-lmdb-jmh-binding-name-cache-v47-20260812.jar` (SHA-256
  `17ddae4cf3f23800999ec938b0241c68642804043e7a2538423a60e31679bf31`) beats v46 at both endpoints of the matched
  q6/q11 bracket: combined endpoint-average latency improves 0.438 percent and allocation improves 1.496 percent.
  Its q11 CPU trace reduces measured recursive `getBindingNames` samples from 404 to 261, confirms the intended path,
  and selects eager immutable costing-event digest work as the next general duplicate-work target. Costing-event
  digests are now realized lazily for only the selected root and its transitive provider-input dependency closure;
  full snapshots and direct event descriptions still realize the complete requested identity, and an explicit
  readiness byte preserves legitimate all-zero digests. All 130 LMDB planning integrations pass in 114.550 seconds.
  Candidate v48 `/private/tmp/rdf4j-lmdb-jmh-selected-event-digests-v48-20260812.jar` (SHA-256
  `324bd29901dd027341a3ad94cf65bb5d1250d2b1e827678d080105c53df519be`) improves matched q6/q11 endpoint-average
  mean and p95 by 4.916 and 5.268 percent respectively, with allocation lower by 0.428 percent. Its retained q11 CPU
  profile cuts measured `PackedCostingTraceArena.eventHash` samples from 677 to 301 (55.5 percent) while moving only
  the selected work beneath `ensureDigest`; it identifies the remaining duplicate retained-metric scan/copy as the
  next general costing-trace target. The trace arena now marks the uncommon case where stored provider metrics use
  the reserved costing-event namespace, takes the unfiltered exact-count copy path otherwise, and preserves the
  original filter for that uncommon case. The same explicit reserved-metric contract passes before and after, all
  packed lifecycle/resource/cache/physical contracts pass, and all 130 LMDB planning integrations pass in 109.517
  seconds. Frozen candidate v49 `/private/tmp/rdf4j-lmdb-jmh-retained-metric-fastpath-v49-20260812.jar` (SHA-256
  `f5741f05905813c82c2f6cc8a968344d6c2cf919dbc6ddc25ddc9a6a828f5efc`) awaits a clean timing decision. Its first
  q6/q11 bracket was stopped after candidate endpoint one and the control because an unrelated checkout started a
  JMH `executeQuery` process during the run; those partial artifacts are preserved but rejected, and the complete
  candidate/control/candidate bracket will restart only on an idle host. The clean restart retains v49: endpoint-
  average aggregate mean and p95 improve 0.285 and 0.849 percent with aggregate allocation within +0.007 percent.
  The q6 p95 endpoints are noisy, so profile hit proof is also required: with essentially identical total CPU samples,
  `appendMetrics` falls from 659 to 490 samples, the two retained-count scans fall from 87 to one, and reserved-prefix
  checks fall from 149 to 106. This is a small measured general gain, not evidence for any unrun corpus gate. The
  active work now moves to the fresh 117-cell planning matrix, exact stable-alternative regret execution, and the six
  fixed-plan execution cells. The fresh v49 uncached saturated matrix completes all 117 cells without a timeout. Its
  summed means are 568.183538 ms/op and allocation is 771,467,523 B/op, improving the frozen v42 totals by 19.03 and
  3.67 percent respectively; sub-five-millisecond coverage rises from 56 to 67 cells. Absolute acceptance remains
  open: 67/117 is below the required 106/117, no cell is yet below 0.5 milliseconds, and the corpus p95 of cell p95
  values is 12.390474 milliseconds. `SOCIAL_MEDIA q10` is the largest remaining steady cell at 24.530926 ms mean and
  is the next general-mechanism profile target. Its retained v49 CPU profile measures 21.823721 ms mean,
  22.222228 ms p95, and 24,375,860 B/op. Of 22,601 benchmark-worker samples,
  `PackedIncumbentSearch.build` owns 14,042; ordinary dense search owns 7,088, correlated-region scheduling owns
  6,653, and correlated dense search owns 6,414. The hottest exclusive graph primitives are singleton-edge lookup
  (854 samples), first-left-deep reachability (559), and connected-subset testing (501), but the inclusive evidence
  shows the required scale: eliminate exactly covered/repeated search and provider work under the AUTO budget rather
  than special-case one lookup or query. Regret and fixed-execution timing remain queued behind a candidate that
  first closes the planning gates. The first retained repair moves deterministic-work admission to the join-search
  boundary: once no candidate-costing unit remains, the planner records the same incomplete limit and returns before
  building another hypergraph, dense index, or candidate arena. Its red observed 528 bytes retained under an already
  exhausted zero-work budget; the green retains zero, and all 20 budget/seed contracts pass. Frozen candidate v50
  `/private/tmp/rdf4j-lmdb-jmh-stopped-search-fastpath-v50-20260812.jar` (SHA-256
  `a3b47b32689d3f6091ed560a352cdfd5ee7f987908d9165c3d9e2f2f08756667`) beats v49 decisively in the clean q10
  candidate/control/candidate bracket: endpoint-average mean, median, and p95 improve 23.67, 23.56, and 23.86
  percent, while allocation falls 4.24 percent. This preserves the executable incumbent and explicit incomplete
  status. The complete packed-search, correlated-row, frontier-kernel, and DPhyp regression classes add 119 passing
  tests (79 + 12 + 17 + 11). The fresh v50 q10 CPU profile reproduces the timing at 16.526689 milliseconds mean and
  16.645033 milliseconds p95 with 23,213,206 bytes/op. The post-budget ordinary join/DPhyp stack is absent, which
  proves the fast path removed the intended work. The largest remaining phase is the admissible correlated-filter
  seed/lattice path (`optimizeDenseCorrelatedFilters` 8,250 of 22,848 worker samples); that general mechanism is the
  next optimization target before any corpus rerun. An initial scalar-adapter probe appeared to show duplicate
  provider contexts, but the corrected arena-equivalence check passes on the original loops: no parent/node/physical-
  implementation transition is duplicated. Production event sourcing also keys the complete provider input, so that
  hypothesis is rejected and no transition-cache patch is retained.
  The next candidate is behavior-neutral and directly oracle-covered: replace `firstLeftDeepOrder`'s full forward
  scan of every dense subset with memoized backward reachability from the required full state. It must return the
  identical first DPhyp order for every graph/prefix/first-node constraint; the existing randomized 2-9 node oracle
  and deterministic cache contracts are the matching pre/post selection. That selection is green before and after
  (11 tests), and all 10 ordered-prefix/bounded correlated-seed contracts pass. Frozen v51
  `/private/tmp/rdf4j-lmdb-jmh-lazy-left-deep-v51-20260812.jar` (SHA-256
  `e6772f2398f3e47ec97673dd8d3a8bc72c7fc5a43d341208fb33d53b68d910e1`) wins the clean q10 bracket:
  endpoint-average mean, median, and p95 improve 5.35, 5.70, and 4.52 percent versus v50, with allocation down 0.09
  percent. The matched profile validates the mechanism with equal worker sample volume: `firstLeftDeepOrder` falls
  from 848 to 19 samples and cache resolution from 864 to 30. v51 is retained. The exposed dominant phase is now
  provider/event costing inside correlated append transitions, not graph schedule construction. Filtering the v51
  profile to measured `planUncachedQuery` stacks also exposes `isMinimalPredicatePrefix`: it repeatedly enumerates
  every proper subset and runs a connectivity walk. The next exact, behavior-neutral candidate tests the graph
  theorem that a qualifying connected proper subset exists iff a qualifying connected one-vertex deletion exists.
  A direct randomized 1-10 node exhaustive oracle is green against the current implementation before and after the
  reduction, and all 12 DPhyp contracts pass. The v52 matched q10 timing decision is next.
  Frozen v52 `/private/tmp/rdf4j-lmdb-jmh-minimal-prefix-v52-20260812.jar` (SHA-256
  `9901012cabdcb483f5f87f014a5740549b373a1f022ad33ef178bc91c4882d59`) wins both endpoints of the clean q10
  bracket: endpoint-average mean, median, and p95 improve 2.11, 1.78, and 1.62 percent versus v51, while allocation
  falls 0.056 percent. At matched measured sample volume, the retained profile reduces `isMinimalPredicatePrefix`
  from 878 to 751 inclusive samples and 73 to 48 exclusive samples; connectivity below it falls from 648 to 625.
  v52 is retained. Provider/event work inside correlated append transitions remains the dominant target. Before
  changing provider semantics, the same profile exposes 625 exclusive samples in repeated simple-subset BFS over the
  same dense factor graph. A mutation-invalidated dense-only connectivity memo now traverses each queried subset at
  most once; the matching randomized/mutation oracle passes before and after, and all 12 DPhyp contracts remain green.
  Frozen v53 `/private/tmp/rdf4j-lmdb-jmh-connectivity-memo-v53-20260812.jar` (SHA-256
  `7fba6238c40e055601c94184f18a40e9769cc70fdd1008e53b28c2a4baec81bc`) wins both endpoints of the q10 bracket:
  endpoint-average mean, median, and p95 improve 1.41, 1.53, and 2.03 percent. Its byte-per-subset representation,
  however, increases allocation by 2.52 percent when the planner constructs repeated graphs. The algorithm is sound;
  v53's representation is provisional. A complete one-bit connected-subset set will remove the tri-state byte cost
  before the retained timing decision. Frozen v54 `/private/tmp/rdf4j-lmdb-jmh-connectivity-bitset-v54-20260812.jar`
  (SHA-256 `6e59dcf8726cfc7a19186b91c507aa5208722376231873993c47b749e79defdb`) rejects that eager representation: both
  q10 candidate endpoints are approximately 19.6 percent slower in mean than v52 because computing every connected
  subset costs more than the demanded lookups. v54 is not retained. The next representation restores lazy lookup and
  packs unknown/disconnected/connected into two bits, one quarter of v53's cache bytes. Frozen v55
  `/private/tmp/rdf4j-lmdb-jmh-connectivity-packed-v55-20260812.jar` (SHA-256
  `cfeef1ccbb5e043b0dae8dd8065aa4f3ccfe993436cd69df03edb180b5874f7b`) wins both q10 endpoints: endpoint-average
  mean, median, and p95 improve 3.16, 3.26, and 3.53 percent against v52. Allocation is +0.77 percent, far below
  v53's +2.52 percent but still material; v55 is retained provisionally while a matched CPU profile and immutable
  query-local factor/correlated-graph reuse target the remaining repeated construction. At essentially identical
  measured sample volume, the v55 profile reduces simple-subset connectivity from 625 to 284 samples, with only 237
  reaching the BFS. `correlatedHypergraph` still owns 450 samples; the next lifecycle slice retains an immutable
  graph and its legal-extension table with the seeded correlated lattice across canonical, boundary, and exact phases.
  That lifecycle reuse is now complete: the same 10 ordered-prefix/seed-resume contracts pass before and after, and
  all 12 neighboring DPhyp contracts pass. Frozen v56
  `/private/tmp/rdf4j-lmdb-jmh-correlated-graph-reuse-v56-20260812.jar` (SHA-256
  `61069773728e59e7bb5d428c0ac55bd250403ba5ca2fbe4bde3fa7d3dfe34a11`) wins both endpoints of the clean q10
  bracket against v55. Endpoint-average mean, median, and p95 improve 3.53, 2.82, and 4.36 percent, and allocation
  falls 1.52 percent. v56 is retained; its matched CPU profile will verify removal of the graph-construction work and
  choose the next general correlated-costing target. The profile succeeds under its 120-second supervisor at
  17.718685 milliseconds mean, 18.014555 milliseconds p95, and 22,994,427 bytes/op. With effectively identical total
  worker samples, `correlatedHypergraph` falls from 450 to 273 samples and singleton-extension construction from 427
  to 209. Correlated append/provider costing remains flat and dominant. Within measured benchmark iterations,
  `PackedObjectPool.intern` is the hottest Java leaf at 586 samples: 282 below costing-trace metrics and 216 below
  physical-metadata metrics. The next behavior-neutral slice adds an exact identity-hit accelerator ahead of the
  existing equality interner, retaining the equality table as the collision-safe fallback. The first representation,
  frozen as v57, is rejected: its two q10 endpoints average 14.28 percent slower than the interleaved v56 control.
  It computes `System.identityHashCode` on every call. The next representation uses the equality hash already required
  by every miss to index the exact-identity accelerator, so a hit bypasses only the expensive mix/probe/equality tail.
  That v58 representation is also rejected: its endpoints average 7.32 percent slower than v56 and allocation is
  effectively flat. Repeated metrics are not stable identities often enough to pay for another lookup. The production
  identity accelerator is removed; any further work must reuse explicit arena-local metric IDs with a proved source
  ownership contract. The next general provider target registers canonical packed relation roots under opaque
  query-local identities in the active LMDB optimization scope. Detached and unregistered algebra keeps its
  collision-safe structural fingerprint, including structurally equivalent clones. All matching pre/post contracts
  and the complete 130-test Frontier planning integration suite pass; the latter finishes in 110.304 seconds. Frozen
  v59 `/private/tmp/rdf4j-lmdb-jmh-packed-factor-identity-v59-20260812.jar` (SHA-256
  `d05e0a908dbf4e837208766de20a77bc681b9384607355bccdefae6f7701089e`) wins both q10 endpoints against v56:
  endpoint-average mean, median, p95, and allocation improve 1.69, 2.01, 1.79, and 1.38 percent. Its matched measured
  CPU stacks reduce factor fingerprints from 422 to 160 samples, uncached structural construction from 402 to 146,
  and structural query-model traversal from 107 to 45 while `LmdbEstimationEngine.Session.estimate` remains flat at
  2,066 versus 2,065. Registration itself costs only 22 factor and 12 prefix samples. v59 is retained; the next slice
  must follow the newly exposed measured provider or search bottleneck. Two increasingly strict source-owned
  metric-name ID caches are rejected rather than retained on allocation alone. v60 validates every cached read and
  regresses endpoint-average q10 mean/p95 by 1.48/0.93 percent. v61 invalidates only when a metric name or object-pool
  generation changes, yet regresses mean/median/p95 by 2.89/2.82/3.64 percent with allocation effectively flat at
  +0.004 percent. Both experimental layers are removed; matching lifecycle and physical-metadata contracts pass on
  the restored v59 production path. The next measured correlated-transition slice canonicalizes the six immutable
  logical/physical candidate/left/right telemetry-key families and caches lowercase enum labels once per enum class.
  Frozen v62 `/private/tmp/rdf4j-lmdb-jmh-canonical-telemetry-names-v62-20260812.jar` (SHA-256
  `28ae7e5df7c61ffc9cbd7155d7535819069bf1bb4d0f81caf3fc58c05e50fb0b`) wins both q10 endpoints against v59:
  endpoint-average mean, median, p95, and allocation improve 5.40, 5.25, 5.02, and 6.01 percent. The retained CPU
  profile reduces `String.toLowerCase` from 301 to 4 samples, `annotateIntermediateJoinState` from 362 to 145, and
  `statusName` from 142 to 29 at matched total sample volume. A follow-up collision-safe binary-transform topology
  memo is rejected: v63 regresses mean 0.258 percent for only 0.068 percent less allocation, so it is removed. The
  complete 117-cell corpus must now decide aggregate acceptance impact before another query-specific profile. The
  first fail-closed v66 stable-alternative inventory completed MEDICAL_RECORDS q0--q4 with 1,875/1,875 costing events
  retaining nonzero Frontier outputs and no scalar fallback, then stopped at q5. Exact search itself completed, but
  all 431 recorded events came from a whole-session scalar restart after the exact binary-transform cache attempted
  to materialize optimizer-created relation ID zero as a source expression. A generic finite-relation TDD contract
  reproduces the same `unknown expression 0` failure without Theme data. Synthetic joins now derive transform
  identity from the sorted alpha-normalized topology of every logical factor in their output state, while real memo
  relations retain source-expression identity and the descriptor still proves slot mapping, operation, state keys,
  and both exact payloads. The focused red/green, all 12 exact-transform cache contracts, and the star-versus-path
  topology-separation contract pass. Frozen v67
  `/private/tmp/rdf4j-lmdb-jmh-synthetic-join-topology-v67-20260812.jar` (SHA-256
  `4653a91430418386d66f8bb4ac2a3ec2900febcf6f4451c3c84916f355cb4d0a`) closes MEDICAL_RECORDS q5 at
  `EXACT_COMPLETE`: all five stable alternatives execute with the same authoritative result bag, and all 789
  serialized costing-event occurrences retain nonzero Frontier output states with zero scalar or whole-session
  fallback. The initial five-warmup/twenty-measurement median made alternative four look 11.77 percent faster, but
  the time series is nonstationary in the same direction as the campaign's previously traced tiered compilation:
  over the last ten measurements the selected plan is 2.34 percent faster, and over the last five it is 1.95 percent
  faster. Both candidates reach the final differing continuation with identical
  cost-vector and Frontier-state digests; only the exact continuation identity differs. This is warmup/protocol
  evidence, not permission to tune a policy constant. The frozen one-warmup/three-measurement manifest remains the
  historical comparison boundary, while any acceptance rerun must preserve it and separately report a genuinely
  JIT-saturated lane rather than classify early compilation as plan regret. Exact canonical executable identity now
  excludes memo row IDs, detached properties, and DAG sharing while retaining materialized semantics and physical
  controls. Plan-quality audits use canonical equality after the digest check, keep the selected representative and
  its complete trace, and never deduplicate on a hash alone. Frozen v70
  `/private/tmp/rdf4j-lmdb-jmh-exact-plan-dedup-v70-20260812.jar` (SHA-256
  `e1a7a0a1b1268ddd1922f8c55a29ac47ec0984d9d2f6d398072ec47f8edad7b7`) reduces q5's five raw Pareto candidates
  to four unique executable plans. All 631 retained event states are nonzero with no scalar/session fallback; all
  four executions return the same bag, and the selected plan is fastest at 9.086 ms median versus 20.211, 71.491,
  and 191.543 ms, closing q5 at zero regret without a heuristic. The resumed fail-closed 117-cell inventory is next.
  That inventory exposed a general state-continuity defect at TRAIN q8: a relocated FILTER imported factors from its
  original unscheduled subtree, so a later real append appeared idempotent and the database-exact derivation failed
  closed. The repair keeps the operator's semantic scope separate from the input state's actually costed factors and
  uses the canonical operation recipe, rather than fictitious factors, to certify operator learning identity. The
  direct factor-membership red, exact-fact interaction, all 132 Frontier planning integrations, and all nine benchmark
  plan tests pass. Frozen v71 `/private/tmp/rdf4j-lmdb-jmh-truthful-filter-state-v71-20260812.jar` (SHA-256
  `0e8c6eacb1dd2c2985a810b3dfc3311e54db42244dd908c78ab92c33c5ddaa87`) closes the original full-scale q8 case at
  `EXACT_COMPLETE`: all 316 serialized costing-event occurrences have nonzero output state, with zero scalar or
  whole-session fallback. All three exact executable Pareto alternatives return the same bag; the selected plan is
  fastest at 0.477 ms median versus 426.781 and 593.651 ms, so measured regret is zero without a policy heuristic.
  The corpus-wide v71 inventory completed 116 of 117 cells at `EXACT_COMPLETE`: 366 executable alternatives and all
  45,509 serialized costing events retain nonzero Frontier output state with zero scalar or whole-session fallback.
  SPARSE q5 alone was externally killed at 120 seconds without an inventory. A bounded async-profiler diagnostic
  attributes 75.45 percent of its samples to exact `costDenseCorrelatedTarget` traversal; inspection then found that
  `auditPreparedInput` passed an unbounded root deadline directly to `PackedPlannerLimits`, ignoring the explicit
  benchmark timeout property. A generic 14-factor red proves the defect. Prepared-input exact planning and audits now
  compose the caller deadline with an explicitly configured deadline while preserving search mode, task budget,
  physical requirements, semantic scope, cost bound, row goal, estimation tier, and binding context. No default
  deadline is invented when the property is absent. The focused red/green and all ten benchmark-plan harness tests
  pass. Frozen v72 `/private/tmp/rdf4j-lmdb-jmh-exact-audit-deadline-v72-20260812.jar` (SHA-256
  `8ae6dec080bb1157a24ec41b0b8f9dbe444ae8f5042c41305815c7fe68ba70eb`) returns SPARSE q5 itself after 68.915
  seconds as `INCOMPLETE_DEADLINE`, with three executable Pareto alternatives, 2,384,636,276 retained bytes, 392,424
  work units, and all 315 costing events retaining nonzero Frontier outputs with no scalar/session fallback. The
  policy-selected plan completes its five-warmup/twenty-measurement lane at a 1.310-second median; the other two
  legal continuations both hit the independent 120-second ceiling under that long protocol and retain honest
  censored outcomes. One-shot process-isolated executions then close the semantic and regret comparison: every plan
  returns the same one-row bag, while the two non-selected plans take 17.536 and 13.575 seconds. The selected plan is
  13.39 and 10.36 times faster respectively, so measured regret across every retained executable continuation is
  zero.
  Exact closure for this sole cell is therefore limited by the declared combinatorial-search deadline, not a missing
  rewrite, erased sketch/Frontier continuation, scalar replacement, benchmark-specific heuristic, or hidden restart.
  Allocation profiling of the saturated MEDICAL_RECORDS q1 uncached lane then exposed a second, general evidence
  boundary defect: after selection had detached the root Frontier state, the planner recursively ran the legacy
  scalar estimator again and allowed its exact-zero result to replace a positive Frontier certificate. Two focused
  reds now require the selected recipe's detached database-exact point or positive certified lower bound to remain
  authoritative, including the SPARQL invariant that a global aggregate emits exactly one row. Both focused tests,
  all 37 packed plan-cache tests, and all 133 LMDB Frontier planning integrations pass. Frozen v73
  `/private/tmp/rdf4j-lmdb-jmh-certified-root-v73-20260813.jar` (SHA-256
  `dfa313e7e6f1464c5c359ad8a2c22a98d5c49cfd147aeeab6d0ab6f064f0179e`) removes the duplicate scalar traversal
  without changing search, candidates, costs, dominance, or policy. In the same-store 10x1-second warmup,
  7x1-second A/B/A bracket, normalized allocation falls from the two-control mean 3,857,749.936 to 3,653,246.176
  bytes/op (5.30 percent). Candidate latency is 2.515 ms/op versus the endpoint mean 2.800 ms/op, while its advantage
  over the closing control alone is 1.84 percent; allocation, not that noisier latency delta, is the durable claim.
  The next saturated allocation profile found that direct distinct-cardinality probes cloned and annotated every
  statement solely to communicate an already-known variable set to the exact LMDB cursor-skip selector. The retained
  behavior-neutral repair makes that proof an explicit argument while keeping the telemetry-based public path and
  the common exact plan selector unchanged. Matching pre/post characterization, all five cardinality-source tests,
  the 9/2/1 cursor-skip scope suites, and all 133 Frontier planning integrations pass. Frozen v74
  `/private/tmp/rdf4j-lmdb-jmh-direct-distinct-v74-20260813.jar` (SHA-256
  `4bf30a6afee9a7757c7d7ca04b5ac9bffb5b3d164a9b7157572032fb452b4c09`) lowers same-store normalized allocation
  from the two-control mean 3,651,332.767 to 3,526,535.418 bytes/op (3.42 percent); latency confidence intervals
  overlap, so allocation is again the durable claim. No query identity, shape test, threshold, cost constant, search
  budget, rewrite, continuation, dominance, access selection, or final policy changed. The following v75 repair moves
  a proved direct correlated Frontier access surface ahead of the legacy scalar compatibility boundary. Declined
  surfaces still invoke scalar costing. The focused red/green, all 133 Frontier integrations, and all 31 then-current
  finite-surface tests pass. Frozen v75 `/private/tmp/rdf4j-lmdb-jmh-direct-correlated-v75-20260813.jar` (SHA-256
  `2ac98f0c204b983c6764de53a5d6a79bb87983bf6ae77dc9dc5e88c0ca4727`) reduces normalized allocation by 0.81
  percent in a three-control/three-candidate alternating bracket; latency moves +0.95 percent by mean and +2.02
  percent by p95 with overlapping confidence intervals, so only allocation is retained as a durable claim.

  A separate state-continuity red then proved that an exact finite measure joined to an already composable sampled
  statement payload was unnecessarily reread through an ordered LMDB prefix. The v76 continuation is admitted only
  when one operand is `DATABASE_EXACT`, because join is then linear in the other random measure; random-by-random
  inputs keep the existing conditional probe, and zero work budget keeps the explicit non-composable boundary. The
  existing bounded, conditionally unbiased payload resampler performs the transform without a query ID, shape test,
  cost threshold, search limit, or policy constant. The focused red/green, all 33 finite-surface tests, and all 133
  Frontier integrations pass. Frozen v76 `/private/tmp/rdf4j-lmdb-jmh-linear-payload-v76-20260813.jar` (SHA-256
  `50ee54668d9ff428c4e3946bc6d48a951fe1025d1f38de472a4ce2d5ab6ab1e8`) keeps MEDICAL_RECORDS q1
  `EXACT_COMPLETE`, with the same three physical root fingerprints, selected root, memo/search cardinalities, and
  zero scalar/session fallbacks. Its relation-3 prefix advances from `UNRESOLVED / BOUND_ONLY` to
  `MEASURE_UNBIASED / COMPOSABLE_PAYLOAD`; retained state rises by 196,608 bytes within the 512 MiB query budget,
  work rises by four units, and the selected estimated cost changes by -0.18 percent from the newly retained
  evidence. An initial idle-start/idle-end v76 timing is 1.856 ms/op and 2,241,459 bytes/op, but two neighboring v75
  controls were explicitly excluded after active monitoring caught concurrent Maven work from another checkout.
  The post-v76 broad class then exposed four general continuity defects. Sampled non-observation could publish a
  speculative inexact derivation before a later exact access surface claimed the same identity; physical join
  refinement could accept a candidate unrelated to both child lineages; memo-only intermediate groups were routed
  through the immutable source-expression consumer array; and equivalent uncorrelated inner-join trees recorded an
  internal child cardinality as an external learning feature. Exact recovery now runs only after ordinary surface
  refinement and only when its complete input fits the declared exact scan contract; physical candidates must cover
  the union of their child lineage; memo-only groups have no source-expression consumers; and only genuinely
  parameterized/inherited input contributes bound-input cardinality. The fixes add no query, shape, threshold, cost,
  budget, dominance, or policy condition. The five original failures pass together, the paired inner-join/semi-anti
  learning contracts pass, and all 134 Frontier planning integrations pass in 126.649 seconds with a 16 GiB Maven
  heap. The v77 jar and timing bracket remain intentionally unfrozen while another checkout's JMH worker and macOS
  storage analysis consume the host.

  Exact continuation identity is now congruent with the collision-checked payload contract. Two commuted
  `DATABASE_EXACT` finite joins that produce the same complete relation publish one canonical continuation state,
  while a neighboring sampled/exact reproduction proves `MEASURE_UNBIASED` evidence remains distinct. The first
  valid LMDB contract failed with state IDs 3 and 4; the paired exact/sampled green and the 62-test core
  arena/memo/Bellman suite are preserved in `initial-evidence.txt`. A subsequent red exposed the practical source of
  the v77 decision-certificate explosion: `PackedMemo` retained a dominated exact derivation correctly for audit but
  also appended it as a searchable continuation winner. Search now admits that row only when the exact
  multidimensional Pareto arena retains it; if audit admission is absent or resource-limited, the exact continuation
  remains searchable because no dominance proof exists. This changes no cost dimension, epsilon, cap, work budget,
  query identity, shape rule, or final policy. Diagnostic v79
  `/private/tmp/rdf4j-lmdb-frontier-v79-20260813-1822.jar` (SHA-256
  `6c8a0c52f924a756cda9b813e4b21798457aa6e61a36c662d2868a53bd8594fe`) still produced 1,127,879 winners,
  2,255,798 work units, and 6,013,884,228 retained bytes before a 37.154-second serialization OOM. Diagnostic v80
  `/private/tmp/rdf4j-lmdb-frontier-v80-20260813-1829.jar` (SHA-256
  `56059c64523e70b2353ac82dae7a233fae8405ff650ee14a6ba7d32bc701dfbb`) completes the identical
  MEDICAL_RECORDS q1 inventory `EXACT_COMPLETE` in 9.816 seconds with the same three root alternatives, 49 winners,
  140 work units, and 1,031,012 retained bytes. The first broad search rerun then proved that a parent recipe must
  still address the exact dominated derivation it had already costed. The final split retains a materialization-only
  row with the candidate's immutable children and event, but never links that row into continuation iteration. The
  focused materialization red/green and the formerly failing 137-test dense/sparse/wide/correlated search selection
  are preserved in `initial-evidence.txt`. A full corpus rerun, final broad verification, formatting, and handoff
  remain the active acceptance action.

  The v81 through v93 correctness campaign then closed the remaining state-loss paths rather than tuning a Theme
  cell. Contextual OPTIONAL and correlated RHS planning now receive the chosen physical prefix instead of an opaque
  source JOIN; exact-by-exact payload algebra spends its typed arena memory rather than a stochastic scan budget;
  local parent cost vectors compose every retained child vector exactly once; equal exact continuations share one
  active semantic transition while incomparable CPU/memory vectors remain distinct; identical immutable metric
  bundles share retained storage; and a provider `FrontierMemoryLimitException` becomes
  `INCOMPLETE_RESOURCE_LIMIT` only after preserving an already-complete executable incumbent, with no scalar
  restart. Aggregate-only Extension elements follow RDF4J evaluator semantics and keep their Group payload, while an
  independently proved exact zero replaces a weaker sampled zero and remains `DATABASE_EXACT` through aliases. The
  broad packed matrix passes 250 contracts and the LMDB Frontier integration class passes all 136 tests in 118.523
  seconds with the 16 GiB Maven heap.

  Frozen v93 exposed one remaining algorithmic, not semantic, obstruction: PHARMA q11 spent 87.73 percent of its
  sampled CPU in quadratic insertion ordering of exact OPTIONAL binding-mask rows and exceeded the unchanged
  120-second process limit. The exact unsigned lexicographic order now uses one in-place worst-case O(n log n)
  heapsort for every input; there is no size threshold, query identity, shape detector, cost rule, or cardinality
  heuristic. Frozen v94 `/private/tmp/rdf4j-lmdb-frontier-v94-20260813-2352.jar` (SHA-256
  `5ed1ceeb432defd80da05462c175523fee049774ab6902f4df4af156dbbbe922`) completes PHARMA q11
  `EXACT_COMPLETE` in 14.424 seconds with the same three tied minimum-cost roots and 402 intact Frontier event
  states. Its full inventory closes all 117 supervised processes: 114 are `EXACT_COMPLETE`, while SOCIAL_MEDIA q1,
  SOCIAL_MEDIA q10, and SPARSE q5 honestly report the typed 512 MiB Frontier
  `INCOMPLETE_RESOURCE_LIMIT`. Across 190,107 serialized event states there are zero missing output state IDs, zero
  scalar fallbacks, and zero whole-session fallbacks; every selected comparison cost is the minimum emitted root
  cost. The first authenticated v94 execution attempt then exposed a campaign-identity defect rather than a search
  defect: independently parsed aggregate/HAVING algebra kept the same candidate fingerprint but embedded a parser
  nonce in the executable physical fingerprint. Physical identity now serializes the same lossless packed algebra
  used by collision-proof cache equality, alpha-normalizing only names proved query-internal and retaining exact
  names at observable or unsafe boundaries. A neighboring replay failure also proved that an ID-only demand
  realization could relabel typed evidence without atomically updating its guarantee and disposition; the typed
  realization contract now rejects that state mutation and preserves exact continuation explicitly. Frozen v95
  `/private/tmp/rdf4j-lmdb-frontier-v95-20260814-0108.jar` (SHA-256
  `450776ccb4fb44e703376ce7c4875dd48c663b7f71056835eaaad7dee9717c40`) authenticates all 13 independent
  MEDICAL_RECORDS q2 executions against its fresh inventory, with one common 135-row result bag and zero measured
  regret. Its fresh full inventory independently reproduces the complete v94 search accounting: all 117 supervisor
  processes succeed without timeout, 114 searches are `EXACT_COMPLETE`, and the same three cells report only their
  typed 512 MiB Frontier resource limit. All 657 executable alternatives have unique stable IDs and contiguous
  indexes; every selected comparison cost is the emitted minimum; and all 190,107 serialized event states preserve
  positive Frontier outputs without scalar or whole-session fallback. The authenticated full v95 alternative
  execution campaign authenticated q0-q2, then exposed a second identity defect at MEDICAL_RECORDS q3: two fresh
  JVMs read byte-identical manifest, 284 MB payload, and mapped query-index files, but parser-generated property-path
  variable UUIDs entered `FrontierStateKey` resampling seeds and changed sampled costs and executable controls. Seed
  schedule v2 now hashes ordered binding slots and mask shape while exact names remain in state equality, interning,
  and runtime tuple lookup. This is alpha-equivalent structural identity, not a query, size, cost, or benchmark
  heuristic. Frozen v96 `/private/tmp/rdf4j-lmdb-frontier-v96-20260814-0233.jar` (SHA-256
  `cfdd01d1adce8592df9b3641ffd0c676bdd4e7eb1baf4003867b2a06745b3051`) emits an identical ordered set of 16
  MEDICAL_RECORDS q3 physical fingerprints, pqa2 IDs, and costs from two independent inventory JVMs; all 16 separately
  supervised execution JVMs authenticate and return the same one-row bag. The zero-warmup/one-measurement diagnostic
  is not timing evidence. The frozen 5-warmup/20-measurement regret campaign, matched fixed-plan execution, absolute
  planning targets, and both tenfold planning aggregates remain open until their prescribed campaigns finish; no
  result is inferred from inventory completeness or unsaturated timing.
- [ ] Milestone 9 (previously in progress; paused while unified costing is implemented): replace the raw Frontier payload, derived query index, and query-time exact-replay
  fallback with one disk-resident, directly queryable statistics generation that remains bounded at 20 billion
  triples. M9.0 through the bounded online-maintenance core of M9.4 are implemented test-first. Missing V2 evidence cannot
  enter statement count/iteration or exact connected-surface paths; `FrontierLinearTransforms.join` no longer
  narrows or allocates Cartesian pair counts. One purpose-aware heap governor, capability-complete READY validation,
  leases, atomic publication/rollback, and structured status now own revision-3 query shards. Manifest revision 5
  pins capability, hash/bucket schema, Omni layout, term width, and tuple-ordinal width while reading legacy
  unversioned manifests and revision-2 shards for migration. The two-pass builder writes exact/heavy/projected
  summaries, coordinated Omni cells with deduplicated tuples/postings, Fast-AGMS support, and two-stage bounded center
  samples. Mapped leaf, projected-distinct, all 16 binary role pairs, star, bridge, path/tree, and retained-endpoint
  cycle programs are wired into storage and packed planning; all 139 planner integration tests pass with the V2
  no-statement-I/O route. Transactional LMDB journal rows, exact committed tails, signed count/AGMS delta layers,
  query-ready Omni insertion/tombstone layers, KL-sized random-delete reserves, reserve-driven cell de-authority,
  restart replay, 60-second scheduling, 10-percent delete-debt rebuild, and mapped size-tiered layer compaction are
  implemented. Compaction replaces CountSketch, AGMS, summary, tuple, posting, and sparse-directory shards as one
  sequence-range family without statement replay or mutation-sized heap arrays. Preserve all reds in
  `initial-evidence.frontier-omni-v2.txt`, including block-local mapping, high-degree hub overflow, and missing
  manifest algorithm identity. Mandatory all-context and named-context HLL matrices plus predicate-heavy projected
  distinct scalars now keep cold distinct costing query-ready. Bounded query-owned VALUES domains and safe finite
  filters use mapped leaf/subgraph probes rather than the old exact connected-surface cache. Immutable shard handles
  are reference-counted across adjacent generations, so atomic delta publication charges reused mappings once while
  old readers retain a valid lease. Mapped logical/physical learning now derives stable identities directly from
  packed algebra, preserves calibrated state when detached, and rejects append-event logical corrections whose
  origin does not describe the complete contextual prefix. Mapped semi/anti costing prices streaming, memoized, and
  materialized EXISTS/NOT EXISTS/MINUS alternatives from retained rows, mapped RHS cardinality, and Omni
  projected-distinct evidence without reopening V1. Legacy-sized heap settings are translated into a feasible V2
  build profile, and serializable planning retains its pinned statistics isolation. The 68-test
  builder/service/session cluster and the focused property-path, cursor-skip, finite-domain, filter, and
  opaque-operator suites are green. The 20-billion profile's compressed deterministic dimensions remove duplicate
  predicate/context HLL and AGMS state and stream AGMS publication directly from accumulator storage. Its admitted
  builder workspace is 1,269,897,643 bytes, about 72 MiB below the 1.25 GiB background envelope, while retaining
  query-ready heavy-predicate projected-distinct scalars. The later Java 21 quad-composable reference has now been compared against the
  mapped design. V2 adopts explicit replica-dispersion/effective-support quality and quality-guided progressive
  bottom-K prefixes without adopting its heap-loaded catalog or quadratic object join. Common mapped leaves stop at
  a 64-row priority tier per design lane; weak support doubles until strong, exact, or exhausted. Zero witnesses no
  longer receive a pseudo-count confidence, and sampled star/path/cycle/bridge intervals consume actual shared
  witness support instead of counting available lanes. Query-local primitive sampling accumulators eliminate the
  per-cell sampling-lineage allocation found by JFR, and immutable shards build a bounded open-addressed column-ID
  directory once at open instead of binary-searching on every decoded value. All 43
  `FrontierStatisticsBuilderTest` cases and all 139 planner integrations are green for this slice. The warm mapped
  three-pattern star measures 3.827 +/- 0.068 ms/op; the path improved from 11.833 +/- 0.085 to
  10.614 +/- 0.149 ms/op. A final 101-second JFR measures 10.771 +/- 0.063 ms/op, reduces sampled allocations from
  895 to 150, removes `SamplingReference.bottomK`, and reduces `FrontierStatisticsShard.column(int)` from 22.97 to
  0.04 percent of execution samples. Mapped logical refinement now also precedes physical independent-hash costing:
  when the assured lookup mask covers the complete compatibility mask, candidate work is the mapped logical match
  multiplicity rather than the stale Cartesian input product. The exact READY-V2 sparse-prefix reproduction, the
  61-test hash/finite compatibility group, the 166-test changed planner/storage sweep, and the 226-test V2 cluster
  are green after this repair. Typed finite predicates now expand at most 16 evaluator-validated numeric/calendar
  aliases per RDF value and 1,024 combined bindings, so common Java `LocalTime` and numeric lexical forms can recover
  positive mapped support without dictionary or statement enumeration; absence remains a `[0,1]` non-exact result.
  The five-test typed-filter class and TRAIN q5 integration are green. A complete 10-second exhaustive search of the
  highly-connected q10 memo independently selected the same four-value mapped weight anchor as budgeted search; its
  NOT EXISTS RHS is materialized once. The regression now asserts the underlying bounded-work contract—four mapped
  Omni prefix probes plus materialized/memoized anti work—instead of the obsolete textual order required by the old
  streaming-correlated implementation. Bound repeated-variable pairs fixed by a finite domain remain leaf equality
  constraints and are excluded from the unbound join-variable class domain; the focused constructor regression and
  Social Q7 self-loop integration pass. The final LMDB module gate passes 2,247 tests with zero failures or errors
  (114 skipped). The shard-reader follow-up is complete: bounded metadata reads no longer retain header or directory
  mappings; failed checksum mappings are explicitly cleaned; close seals and releases the mapping registry; and
  contiguous columns promote after four successful dedicated touches to one shared mapping. The packed hot path now
  uses one unaligned little-endian word load plus at most one spill byte, regular blocks use arithmetic lookup, and
  large irregular layouts use bounded fences before binary search. Portable mapped-buffer regressions preserve the
  existing logical `mappedDataBlockCount`; legacy/revision-3, widths 1 through 64, all codecs, irregular/tail, and
  concurrent-first-touch verification pass on Temurin 26. Matched cached-read timing improves every measured width
  and codec, while isolated cold first-touch is 43 to 46 percent slower and therefore remains an explicit cold-p95
  qualification obligation rather than being hidden by the hot result. Still open: held-out audit/adaptive promotion,
  24-hour asynchronous migration cleanup, cold-NVMe
  qualification, and physical 100M/250M/500M/1B scale runs. Milestone 8's immutable v96 artifacts
  remain valid and untouched; its timing campaign resumes only after V2 shadow-mode parity establishes a comparable
  estimator source.

## Non-negotiable architecture invariants

### Evidence is state, not a scalar

The canonical semantic estimate is a versioned evidence state. It may contain database-exact relations, bounded
weighted particles, sketch summaries, distinct/value distributions, predicate ranges, uncertainty, lineage, and
learned calibration. Every supported algebra or physical transition consumes a state and returns a state plus a
physical cost event. It must not accept only `double prefixRows` when a richer state exists.

Scalar rows are permitted only at explicit boundaries:

- compatibility with a public or legacy API that cannot carry state;
- an honest typed fallback when no supported state transform exists;
- final human-readable telemetry; or
- a final policy projection used to select one root plan after Pareto retention.

Every scalar boundary records its source, guarantee, uncertainty, state digest when one exists, and degradation
reason. A scalar adapter must never erase the state from sibling planner paths or make the scalar the new source of
truth.

### Semantic facts, estimates, observations, and costs stay separate

- Proven facts authorize rewrites and exact-zero conclusions.
- Estimated facts predict cardinality and distributions and carry uncertainty.
- Completed execution observations update future estimates only through validated, versioned learning contracts.
- Physical costs describe resource use. They do not masquerade as semantic cardinality or proof.

No confidence score, q-error threshold, learned posterior, benchmark outcome, or preferred plan shape may make an
otherwise unsafe rewrite legal.

### DPhyp enumerates; Cascades retains and chooses

DPhyp is the topology enumerator for legal connected partitions. It emits arbitrary legal CSG/CMP pairs, including
bushy pairs, rather than serving merely as an expensive way to derive singleton extensions. Typed total-eligibility
sets, conflict rules, required bindings, correlation direction, semantic barriers, and physical properties define
legality.

The Cascades memo owns equivalence, candidate installation, costing, reactivation, Pareto retention, and final
selection. DPhyp, rewrite rules, access-path providers, and store-specific implementations contribute alternatives;
none may preselect a winner or suppress a generic legal alternative.

### One subset may require many continuations

A factor subset is not a Bellman state when later costing can observe prefix order, evidence lineage, binding shape,
correlation, scope, predicate state, physical properties, or child cost composition. Candidates may share one cell
only when their continuation keys are provably congruent for every legal suffix. Without a monotonicity proof,
equality is the only safe dominance relation.

Inside one continuation-equivalence class, retain a Pareto frontier rather than one scalar winner. Dominance compares
compatible properties and all policy-relevant dimensions. At minimum the design must be able to represent:

- startup and first-result work;
- steady-state CPU/work rows;
- storage reads, page walks, seeks, and remote calls;
- rescan/reopen and materialization-once cost;
- peak and retained memory;
- output and intermediate row distributions without treating them as physical cost;
- lower/point/upper or distributional uncertainty and regression risk;
- ordering, distinctness, parameterization, cacheability, and other delivered properties; and
- optional future dimensions such as network, parallelism, energy, or monetary cost without redesigning the memo.

Traversal priority may use a scalar score. Pruning may not, unless an explicit policy proves that the score preserves
all future choices.

Exactness is a completion state, not a promise to consume unbounded memory. Query-local search accounts every
retained primitive-array capacity before growth against one `maxRetainedBytes` limit. The production default is 64
MiB per planning request; changing that default requires a Decision Log entry and matched corpus evidence. Reaching
the byte, deterministic-work, or deadline limit stops before publishing a partial alternative and returns
`INCOMPLETE_RESOURCE_LIMIT`, `INCOMPLETE_WORK_LIMIT`, or `INCOMPLETE_DEADLINE` with an executable incumbent and a
search-completeness certificate. Such a result is legal to execute but is never labeled, explained, or cached as an
exact result. An approximate candidate-suppression policy is a separately named opt-in mode and cannot reuse an exact
status.

### Cost dimensions use one declared algebra

Every cost dimension has one registry entry that defines its unit, whether it is local or inclusive, its uncertainty
representation, and its legal composition modes. Nonnegative work counters such as source rows, seeks, opens,
expression evaluations, hash work, path expansions, remote calls, network bytes, spill work, and monetary or energy
units compose by saturated addition. Repeated-open work composes as once-only build work plus invocation count times
per-open work. First-result latency follows the critical path to the first result and uses a blocking child's final
latency; final latency sums sequential phases and takes the maximum of explicitly concurrent phases before adding
dispatch and merge work. Peak memory is the maximum of child peaks and every simultaneously live local-plus-retained
set; retained memory sums only storage whose lifetimes overlap. A physical operator must name its composition mode;
it may not open-code a different formula.

The required first registry version contains startup work and steady-state CPU work in calibrated work units;
source/result/hash-build/hash-probe/path row events, expression evaluations, iterator opens, page walks, random seeks,
remote calls, cache hits/misses/evictions, and materialization lookups as nonnegative counts; sequential storage,
network, spill, value-copy, peak-memory, retained-memory, and generated-code size in bytes; first-result, final-result,
planning, compilation, dispatch, and merge latency in nanoseconds; and materialization build, first-match, exhaustion,
rebind, and rescan work in calibrated work units. Page residency, ordering, distinctness, parameterization, pipeline,
breaker, cacheability, and execution mode are delivered properties or categorical compatibility fields, not numbers
silently coerced into work. A future money or energy dimension declares micro-units and composition before use. Row
cardinality and q-error are estimator state and diagnostics respectively, never physical cost dimensions.

Every uncertain value is represented as lower, point, and upper values plus an uncertainty-lineage ID. Monotone sums,
products by nonnegative invocation counts, and maxima propagate interval endpoints with the same operation. The
default exact dominance relation uses ordinary comparison for deterministic dimensions and requires one candidate's
upper bound to be no greater than the other's lower bound in every uncertain dimension, with one strict comparison.
Candidates with overlapping intervals remain incomparable unless they share a lineage and a recorded pointwise
monotonicity proof. An unknown dimension is never zero and prevents dominance against a known value. A final declared
policy may rank incomparable root candidates by a robust scalar projection, but that projection does not alter the
retained frontier.

### The working plan representation is compact and lossless

Keep `TupleExpr` at the public import, selected-plan export, and execution boundaries. Planning uses stable integer
IDs, immutable interned expressions, primitive structure-of-arrays arenas, compact masks, direct child-group links,
and side arenas for payloads, facts, properties, evidence states, cost vectors, proofs, and learning contracts.
Structural equality resolves hash collisions. IDs never escape their owning versioned arena without a detached,
resource-free representation.

The representation must be complete for tuple operators, scalar subqueries, paths, aggregates, RDF-star, SERVICE,
datasets, binding scopes, ordering, duplicate semantics, and native boundaries. Unsupported syntax remains a
lossless typed boundary with visible children; it is never encoded as an opaque executable leaf.

Binding flow uses static-single-assignment-like value IDs, where each produced occurrence has one producer. A SPARQL
variable name can resolve to several value IDs and therefore is not itself a producer identity. UNION, OPTIONAL,
compatible JOIN mappings, projection aliases, extension, and subquery export create explicit primitive merge nodes.
UNION makes a value assured only when every reachable branch assures it; OPTIONAL preserves assured left values and
marks right-only values conditional; JOIN records both compatible producers and their equality/compatibility proof;
MINUS exports only left values; EXISTS and NOT EXISTS export none. Every merge records possible, assured, conditional,
unbound, and expression-error state. Predicate placement and range propagation operate on value IDs and merge facts,
not on variable names alone.

### Learning improves on LEO without repeating its failure modes

The learned-cost layer separates:

- canonical logical cardinality truth shared by equivalent physical alternatives;
- applicability context, including bound inputs, dataset, data/statistics epoch, and semantic scope;
- algorithm/access-specific physical residuals;
- uncertainty and observation quality; and
- plan lifecycle state such as Monitoring, LastGood, Applying, Quarantined, and rollback.

Predictions and actuals must be commensurate across repeated opens, short-circuiting, censoring, partial execution,
cache hits, and materialization. Only completed admissible observations train logical truth. Exact current facts beat
learned state. Learned corrections update the originating evidence transform or cost dimension; they do not blindly
multiply the whole plan. State is bounded, versioned, restart-safe, and invalidated by its real dependencies.

### Predicate ranges remain proof-carrying facts

Predicate-object kind, datatype, language, canonical-value bounds, and finite domains are versioned facts obtained
from the store. Algebra transforms preserve, intersect, widen, conditionalize, remap, or stop those facts according
to operator semantics. A range may prove contradiction, tautology, finite anchoring, a lookup shape, or a tighter
cardinality state only when the exact preconditions hold. Unknown, stale, disabled, excluded, or unreadable ranges
produce no proof.

### Every rewrite and optimization is cataloged

The catalog is generated from both implementation and research inventories and then reviewed. Each entry contains:

- stable rule/optimization ID and algebraic before/after form;
- whether it is semantic normalization, logical equivalence, physical implementation, access-path choice, or
  planner/search optimization;
- exact bag/set/existence regime and duplicate/order implications;
- possible, assured, nullable, local, and required binding conditions;
- SPARQL error, `UNDEF`, three-valued, scope, correlation, dataset/graph, SERVICE, volatility, and side-effect
  conditions;
- predicate-range or other proof requirements, explicitly distinguished from estimates;
- proof/source and known non-rules or counterexamples;
- implementation classes, registration/routing status, and feature/version dependencies;
- positive, negative, nested, interaction, metamorphic, and result-bag tests; and
- completeness status, rejection telemetry, and any unsupported boundary.

The catalog must include implemented and proposed rewrites, physical alternatives, join enumeration, predicate
placement, property enforcement, cache/materialization, runtime filtering, WCOJ/multiway alternatives, and search
degradation. A literature-only list or a code-only list is not complete.

The catalog is closed-world for the production optimizer. Every production rule, implementation provider, enforcer,
access path, and search optimization is registered through the catalog; everything in the research inventory is
classified as implemented, proposed, deliberately unsupported, unsafe, superseded, or a deliberate non-rule. An
executable rule evaluates to a typed `RuleApplicabilityResult` whose status and proof IDs are the same values recorded
by its descriptor. The memo installation API rejects an alternative unless the result is applicable and carries all
descriptor-required proofs. Registration completeness alone is insufficient: tests must also prove that no rule can
install through an unguarded path and that the generated human catalog matches the runtime guard IDs.

## Consolidated baseline and outstanding obligations

The packed cutover is complete. The active planner is under
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed`.
`PackedQueryCodec` encodes `TupleExpr` into immutable integer IDs; `PackedMemo` holds alternatives;
`PackedPlanRecipe` and `PackedPlanMaterializer` detach and materialize the selected plan. The hot path already uses
primitive arrays and structural interning. The remaining representation work is to prove lossless coverage at every
scope and unsupported boundary, replace variable-name source assumptions with occurrence value IDs and explicit
merge nodes, keep all facts and evidence occurrence-specific, and replace one-winner storage with
continuation/Pareto storage without reintroducing object graphs.

The Frontier estimator is also real, not prospective. `FrontierStateArena`, `EvidenceStateRef`,
`EvidenceStateSummary`, `FrontierLinearTransforms`, and `FrontierEvidenceBundle` retain exact or weighted finite
relations, tuple pairing, guarantees, dispositions, lineage, and detached replay information. LMDB supplies snapshots,
the mapped query index, physical probes, and learning through `LmdbFrontierPackedCostSession`. Query-index,
single-pass expansion, complete-center mapped probes, and unbiased bounded resampling are implemented. The remaining
sketch work is sidecar v2 record exactness, object-to-subject coordination, exact-mode deduplication, variance/audit
lanes, research-gated sampled bridge mutation, and eliminating every avoidable scalar-only transition.

The cost carrier is partly multidimensional. `PackedCostEstimate` records output/work scope, source rows, seeks,
opens, expressions, hash build/probe rows, paths, results, remote calls, peak memory, dependent invocation
components, cache/materialization behavior, and an objective interval. However, `PackedWinnerTable` still replaces
one row per goal using scalar startup/total/comparison costs. Dense, sparse, correlated, and memo-local search tables
also retain one continuation. There is no shared registry defining sequential, parallel, reopen, first-result,
uncertainty, or live-memory composition, and `PackedSearchBudget` accounts work/deadline but not retained bytes. Those
are central correctness and liveness defects this plan fixes before installing Pareto storage.

DPhyp topology exists in `PackedDphypEnumerator`, but the ordinary dense consumer in `PackedJoinEnumerator` reduces
its CSG/CMP output to `dphypSingletonExtensions`: only pairs with a singleton side feed the subset transition table.
`PackedDphypEnumerator.Receiver` and `PackedJoinHypergraph` use raw `long` masks, and the hypergraph rejects more than
64 nodes; the separate 65+ join path is therefore not wide DPhyp. The 17--64-factor sparse path and 65+-factor path
also have different completeness behavior. The final design keeps the one-word fast path, adds an interned multiword
DPhyp kernel, and uses one semantic transition contract across widths, with DPhyp enumerating topology and Cascades
owning alternatives, costing, properties, continuation identity, Pareto retention, and final selection.

Stored predicate ranges already enter the packed planner through `PackedPredicateRangeProvider`,
`PackedPredicateRangeArena`, and `PackedDomainFacts`. The first contradiction, tautology, finite query-anchor,
join/union/optional propagation, explanation, and cache-versioning slice is implemented. Still open are stored finite
anchors without a query filter, calendar/datetime lexical equivalence, complete `datatype`, `lang`, and `langMatches`
reasoning, per-candidate explanation, and the complete negative/interaction matrix.

The learned layer has repaired the four observed plan-flip causes: completed observations are invocation-aware and
commensurate; logical truth is keyed separately from physical residuals; dependent reopen/materialization/cache work
is priced deterministically; and persisted Monitoring/LastGood/Applying/Quarantined state can contain regressions.
`FrontierLearningModel` is already a hierarchical Normal-Inverse-Gamma learner over logical absolute values and
physical log residuals; that is the retained LEO-plus statistical baseline.
`LmdbOperatorFeedbackStats` currently writes internal persistence format 20 and reads the supported legacy formats.
The default rollout remains Monitoring. The unfinished work is to add exact applicability, conformal OOD,
interval-only censoring, invariants and hysteresis; make learned logical state and every physical dimension
first-class inputs to the same uncertainty-aware Pareto comparison; finish held-out/corpus validation; and promote
lifecycle enforcement only if all gates pass.

The 117-query audit harness, snapshot-only evidence policy, result-bag fingerprints, isolated worker deadlines, and
matched planning benchmark methodology exist. The corpus itself is not closed: all 117 cells still need authoritative
results, plan snapshots, rewrite/codec/search classifications, fixed-plan execution controls, and a final performance
campaign. Correctness defects found by that campaign belong to the shared operator, rule, estimator, or search
contract; query text and catalog identity may never enter a fix.

The post-packed LMDB module lifecycle has also reached a `japicmp` compatibility gate for classes removed by the
hard cutover, including the former LMDB cardinality calculator. Milestone 0 must classify each reported API removal
against the intended compatibility surface and either supply the correct compatible API or record the deliberate
experimental break. Do not hide the gate, restore the deleted planner, or treat it as an ordinary test failure.

## Milestones and dependency order

### Milestone 0: freeze the complete optimizer contract

Start by producing an executable inventory from the current tree. Enumerate every packed relational and scalar
operator, logical-rule registration, physical implementation provider, property enforcer, DPhyp/search entry point,
estimator transition, scalar conversion, predicate fact, cost dimension, cache dependency, explanation field,
runtime-feedback hook, and test oracle. Give every rewrite and optimization a stable catalog ID immediately, even
when its status is proposed, rejected, unsupported, or known-unsafe. The inventory must fail a test if production
registration lacks a catalog descriptor or if the catalog claims an implementation that is not registered.

The inventory also records the regime in which each search-completeness claim holds: predicate decomposition,
predicate-rewrite closure, Cartesian-product policy, non-inner legality construction, width kernel, and budget mode.
Record every runtime observation target as completed/exhausted, early-out/censored, cancelled, failed, sampled,
cache-served, or otherwise inadmissible. This prevents a DPhyp/CD-E pair set from being called complete when its
input hypergraph omitted a legal predicate rewrite, and prevents a partial operator count from becoming logical
cardinality truth.

Preserve the already-captured continuation red contract in `initial-evidence.continuation-dp.txt`: the three-factor
example requires retaining `B -> A` for cost 3, while the one-slot subset kernel returns cost 102. Before any related
production edit, add and capture the remaining known red contracts: a bushy CSG/CMP pair that the current
singleton-only receiver cannot consume; FILTER distribution into the right operand of MINUS; false DISTINCT
idempotence for self-JOIN and self-LEFT_JOIN under heterogeneous compatible mappings; global GROUP over zero rows;
and keyed GROUP where joint distinct support differs from minimum marginal NDV. Record each new failure in the
repository's initial-evidence format and attach its test to the catalog entry it protects; do not recreate or
overwrite the preserved continuation evidence.

Map every scalar boundary. Classify it as a public compatibility boundary, final telemetry, explicit unsupported
state transform, or bug. A test must reject any new unclassified conversion and any second join-order owner. At the
end of this milestone the catalog and architecture tests describe the exact current surface, the known red contracts
are reproducible, and later milestones can change one typed boundary at a time.

### Milestone 1: finish the lossless packed optimizer representation

Keep `TupleExpr` only at import, final export, and execution. Extend the current immutable `PackedQuery` and primitive
side arenas so logical expression identity is separate from occurrence, scope, binding facts, proof facts, estimator
state, physical properties, learning applicability, and cost. Every tuple operator, scalar subquery, property path,
aggregate, RDF-star expression, SERVICE/dataset scope, binding-set row, required input, duplicate/order contract, and
native execution boundary must round-trip without losing semantics. An unsupported family remains a typed node with
visible children and an explicit planning capability; it is never an opaque pre-executed leaf.

All rewrite rules, DPhyp alternatives, physical implementations, and property enforcers install through the same
idempotent memo API. Structural equality resolves hash collisions. Permanent group IDs are retained; no group merge,
rename, or rehash protocol is introduced. Query-local search remains single-threaded and bulk-lifetime. Cross-query
caches contain only immutable normalized templates, detached evidence, and selected recipes. Architecture tests
reject complete `TupleExpr` fields and boxed per-candidate/per-edge state from classes marked `PackedHotPath`.
Collection and stream use is rejected only in annotated steady-state fields and methods that allocation/JFR evidence
shows are hot; cold construction, diagnostics, catalog generation, and codec/materializer boundaries stay simple
unless measurement justifies specialization.

Add an Indexed-Algebra-style binding-flow index to the working representation. Assign a `PackedValueId` to every
produced occurrence and store compact producer, consumer, and merge adjacency; a variable-name index only locates the
value IDs visible in one scope. Scalar expressions remain a separate interned DAG. Add primitive merge records for
UNION phi, OPTIONAL left-preserving merge, compatible JOIN coalescing, projection/extension, and subquery export.
Each record carries possible, assured, conditional, unbound, expression-error, and compatibility-proof facts. MINUS
copies only left value IDs and EXISTS/NOT EXISTS export no value. This prevents a fact from one UNION or OPTIONAL
branch from being treated as assured on another branch.

A static path index answers producer, root, lowest-common-ancestor, predicate-placement, assured/possible-binding,
unbound/nullability-barrier, merge-path, and minimum-cardinality-path queries without materializing a growing binding
set at every operator. Measure static rebuilding under rule application before adopting a dynamic link/cut tree; add
the latter only if mutation evidence shows that it wins. The working algebra index, Cascades equivalence memo, and
selected physical execution plan remain three distinct structures with explicit conversions.

Acceptance requires structural round trips and result-bag parity for all supported syntax, collision tests, more
than 64/128 symbols, more than 64 factors, nested scope and correlation, and fail-closed behavior for unsupported
physical execution. Generated tests cover nested UNION/OPTIONAL/JOIN/MINUS/subquery merges, aliases, compatible and
incompatible shared mappings, unbound and expression-error branches, and prove that range/pushdown facts do not cross
an unsafe merge. Encoding, binding-index rebuild, rule saturation, search, extraction, materialization, and cache-hit
time/allocation are measured separately before subsequent representation changes.

### Milestone 2: close the proof-coupled rewrite catalog and semantic closure

Make the catalog the closed-world production registration surface rather than a literature-only document. Each
descriptor records a stable ID, before and after algebra, category, implementation registration, bag/set/existence
regime, duplicate and ordering effects, possible/assured/conditional/unbound/required values, error and `UNDEF`
behavior, scope/correlation/dataset/SERVICE/volatility conditions, proof facts, known counterexamples, cache/version
dependencies, positive/negative/nested/interaction tests, telemetry, and status. The finite manifest includes every
normalization, equivalence rewrite, predicate movement, subquery/decorrelation rule, physical implementation, access
path, enforcer, DPhyp/search optimization, runtime filter, WCOJ/multiway candidate, materialization/cache decision,
research proposal, deliberate unsupported case, and deliberate non-rule.

The closed universe is the union of every production registration reachable from `PackedLogicalRuleProgram`,
`PackedFilterRules`, physical/access/enforcer providers, and DPhyp/Cascades search configuration; every entry in the
dated `papers2/papers/docs/sparql_query_rewrite_catalog.md` seed; and every additional rule, physical alternative, or
non-rule named by `papers2/papers/docs/neumann-birler-umbra-query-optimizer-research.md`. The generated catalog header
records the Git revision and SHA-256 digest of both research inputs plus the catalog schema epoch. Adding a production
registration or changing either research input leaves the catalog test red until each new source entry is classified.

Route executable rules through `OptimizerRuleCatalog` and require each guard to fill a reusable
`RuleApplicabilityResult`. Its status is one of `APPLICABLE`, `NOT_APPLICABLE`, `UNSAFE`, `UNSUPPORTED`, or
`BUDGET_EXHAUSTED`; applicable results carry the descriptor ID and exact proof-set ID. `PackedMemo` accepts a
rule-produced alternative only with that result and verifies that the emitted proof IDs cover the descriptor's
required conditions. Generated documentation at `docs/query-optimizer/rewrite-and-optimization-catalog.md` uses the
same descriptors and proof IDs. Tests fail on missing research classification, production registration outside the
catalog, a descriptor without a guard, a guard/descriptor proof mismatch, installation without proof, or generated
documentation drift.

Complete semantic rewrite and predicate-range closure before join enumeration. Finish propagation and consumers
using `PackedPredicateRangeProvider`, `PackedPredicateRangeArena`, `PackedDomainFacts`, and the value-flow merge
records from Milestone 1. Add finite stored-domain anchors without a query filter; calendar/datetime canonical
equivalence; complete `datatype`, `lang`, and `langMatches` tautology/contradiction handling; safe boolean/numeric
lexical expansion; per-candidate costs and rejection reasons; and cache invalidation when data or range revisions
change. A disabled, excluded, unknown, stale, or unreadable range yields no fact. Right-only OPTIONAL facts remain
conditional, UNION widens, JOIN intersects only assured compatible facts, MINUS retains left facts,
projection/aliases remap, and SERVICE/unsupported boundaries stop propagation.

Catalog GroupJoin/preaggregation introduction separately from its eager, memoizing, separate, and indexed physical
implementations. Its logical guard includes join/group-key equivalence, functional dependency or superkey proof,
aggregate decomposability, bag multiplicity, empty-group, error, and unbound behavior. Catalog value-comparison,
existence, semi/anti, and mark-join forms separately. SQL `NOT IN`/`NOT EXISTS` folklore remains a deliberate non-rule
unless the exact SPARQL algebra, unbound/error regime, and comparison semantics prove equivalence; build-side variants
and multiple nullable/unbound-key complexity are physical applicability and cost facts.

Every semantic rule gets generated-bag metamorphic tests with duplicates, unbound values, errors, blank nodes,
named/default graphs, nested subqueries, OPTIONAL, MINUS, EXISTS/NOT EXISTS, UNION, paths, and aggregation. A fact
may authorize a rewrite; an estimate, confidence interval, learned posterior, or benchmark never may. Explanations
distinguish `not-applicable`, `unsafe`, `unsupported`, `budget-exhausted`, and `candidate-dominated`. At milestone end,
local semantic saturation is idempotent and every connectivity-changing alternative is proof-carrying and ready for
the rewrite/hypergraph fixed point in Milestone 5.

### Milestone 3: standardize state-preserving estimation and the cost algebra

Use the existing `FrontierStateArena` as the query-local owner, `EvidenceStateRef`/state IDs as cheap references,
`PackedEvidenceContext` as the planner carrier, and `FrontierEvidenceBundle` as the resource-free detached form.
Every supported transition consumes an evidence context and produces an immutable child state plus a separate
physical cost event. The operator matrix covers leaves, FILTER, projection/extension, UNION, JOIN, OPTIONAL, MINUS,
EXISTS/NOT EXISTS, DISTINCT/REDUCED, GROUP, SLICE, ORDER, zero/arbitrary paths, SERVICE, tuple functions, nested
subqueries, and correlation. Unsupported nonlinear transforms preserve honest bounds, parentage, binding layout,
and a degradation reason instead of manufacturing a scalar point estimate.

Complete the deferred sketch work in dependency order. Frontier payload format v2 adds per-record exactness so heavy
centers can remain database-exact inside sampled generations, adds an object-to-subject coordination lane, and
deduplicates exact-mode records that are currently repeated across design/audit lanes. Migration tests cover v1
read/rebuild behavior, publication atomicity, checksums, insert/delete generations, and crash recovery. Then use the
second design lane as a paired replicate to populate uncertainty and use audit lanes for independent selected-state
validation. Sampled bridge mutation is last and research-gated: it is permitted only as an alternative to unresolved
state after an exhaustive expectation oracle proves conditional unbiasedness and bounds its variance. Affordable
exact expansion always wins, and two same-lane sampled measures are never multiplied naively.

Add characteristic-set evidence with outgoing and optional incoming correlation scopes, per-predicate bag
multiplicity, and distinct-object information. Add updateable distinct sketches, heavy hitters, joint sample
frequency profiles, and moment/distribution payloads for supported aggregate and arithmetic transforms. Every
component declares merge, update/delete, remap, widen, correlation-scope, and invalidation semantics. Distinct-key
overlap is never treated as duplicate-preserving join cardinality, and a merged-away characteristic set cannot
manufacture an exact zero.

Implement the invariant's cost algebra in `PackedCostDimensionRegistry` and `PackedCostAlgebra`. Every physical-
resource `PackedCostEstimate` field maps to one stable dimension ID, unit, local/inclusive scope, uncertainty kind,
and legal composition mode; output cardinality, distributions, and q-error stay in estimator state or diagnostics and
are not imported as physical dimensions. Operators choose from `SEQUENTIAL_SUM`, `PARALLEL_MAX`, `FIRST_RESULT_PIPELINE`,
`BLOCKING_CHILD`, `ONCE_PLUS_PER_OPEN`, and `PEAK_LIVE_SET`; a new mode requires a registry entry, algebra tests, and
a catalog version bump. Lower/point/upper endpoints and uncertainty-lineage IDs survive every composition. Unknown
values remain unknown. The Pareto layer in Milestone 4 consumes only vectors produced by this algebra and never
reimplements formulas.

Keep interpreted primitive operators as the required cold-query execution path. Vectorized/batched kernels are
physical alternatives at operator or pipeline boundaries where bulk work and cache behavior justify them. Runtime
Java compilation is permitted only for a repeatedly executed, bounded pipeline after measurements show interpreter
dispatch rather than algorithm or layout dominates; it must use normalized shape/type/nullability/algorithm cache
keys, bounded generated methods and classes, explicit classloader ownership and eviction, and the interpreted path
on cold input, compile failure, or size overflow. No new compiler dependency is introduced without a dependency
health check and explicit plan revision. Benchmark cold compile-plus-first-execute, warm cache hits, and forced
fallback separately.

Acceptance uses exhaustive finite-bag expectation tests, differential mapped-index versus store probes, exact and
sampled zeros, heavy centers, repeated variables, multi-column correlations, outer/semi/anti kernels, resource
budgets, restart, and disabled/OFF modes. Cost acceptance proves each algebra mode's identity, monotonicity, endpoint
propagation, unknown handling, and associativity where claimed; exhaustive operator trees compare registry
composition with an independent oracle for sequential, parallel, blocking, rescanned, cached, materialized, and
memory-overlap cases. Theme telemetry must show increased authoritative Frontier coverage without worse q-error;
mapped/store probe counts and planning p95 are recorded before and after each format or estimator change.

### Milestone 4: install resource-accounted continuation and Pareto retention

Define a continuation key from every property a legal suffix can observe: logical group/subset, delivered physical
properties, required inputs, semantic and correlation scope, binding shape, prefix/order identity when relevant,
evidence state lineage/disposition/guarantee, estimator and learning applicability, and child cost-composition mode.
Two candidates share a cell only when tests or a proof show every legal suffix prices them congruently. Equality is
the default. A narrower key or non-equality dominance requires a recorded monotonicity proof.

Store physical resource use in a primitive cost-vector arena. It represents startup/first-result work, steady-state
source and result work, seeks, opens, expressions, hash build/probe work, path expansion, remote calls, reopen/rescan
and materialization-once behavior, cache hits/misses/evictions, peak and retained memory, spill work, network or
future dimensions, and lower/point/upper or distributional uncertainty. Cardinality distributions and binding facts
remain estimator state, not physical dimensions. Every vector is produced by the Milestone 3 algebra.

Represent planning/compilation work, latency to first and final result, execution mode, pipeline and breaker state,
parallel setup/dispatch/merge work, value-copy lifetime, page residency, and generated-code memory where a backend
can distinguish them. Umbra-style interpreted/fast-compiled/optimized execution modes and GroupJoin-style eager,
memoizing, separate, and indexed implementations are physical alternatives rather than hard-coded scalar
coefficients. Estimate uncertainty contributes the registered lower/point/upper fields and lineage; q-error is
available only after truth is observed and remains an offline diagnostic rather than a planning dimension or
physical objective.

Within one exact continuation class, retain every nondominated vector with compatible delivered properties. A
candidate dominates another only under the invariant's deterministic/interval/lineage-aware relation. Replace the
one-slot state in `PackedWinnerTable`, dense subset arrays, `PackedLongSubsetTable`, multiword search,
correlated-filter lattices, completed-lattice reuse, and cache certificates with one shared primitive frontier
abstraction. Preserve an allocation-free one-entry fast path and promote storage only when a second inequivalent or
nondominated entry arrives. Never use a beam, cap, epsilon, deadline, weighted sum, or query-specific preference as a
correctness prune.

Extend `PackedSearchBudget` with deterministic byte reservation and expose all limits through immutable
`PackedPlannerLimits`. The default `maxRetainedBytes` is 64 MiB. Candidate, frontier, continuation-key, cost-vector,
proof, node-set, hypergraph, and query-local evidence-reference arenas compute the bytes of their next primitive
capacity before allocation and reserve them atomically. Integer overflow or a rejected reservation publishes no
partial row or memo alternative. Search returns `EXACT_COMPLETE`, `INCOMPLETE_WORK_LIMIT`, `INCOMPLETE_DEADLINE`,
`INCOMPLETE_RESOURCE_LIMIT`, or `UNSUPPORTED_SEMANTICS`. Every incomplete result retains the best complete executable
incumbent found so far, records which work remains, cannot satisfy an exact-cache lookup, and cannot be promoted to
exact by later materialization. Provider-owned sketch payload limits remain separately accounted and their failure
maps to the same explicit incomplete certificate rather than scalar substitution.

Acceptance compares every small connected/disconnected graph against an exhaustive oracle, including the known
cost-3/cost-102 counterexample, adversarial uncertainty and memory tradeoffs, property enforcement, rescans, and late
suffixes that reverse prefix ranking. Tiny deterministic byte limits exercise every failed-growth boundary and prove
that no partial alternative is visible, the incumbent executes with the correct result bag, and exact cache entries
are never written. Stress tests report frontier width and retained logical bytes for 4, 8, 16, 17, 32, 64, 65, and
128 factors. A scalar score may order work and choose one root after enumeration, but cannot delete a potentially
optimal continuation.

### Milestone 5: reach a proof-checked rewrite, DPhyp, and Cascades fixpoint

Evolve `PackedDphypEnumerator` into one semantic dispatcher with two primitive kernels. The one-word kernel retains
the current `long` fast path for at most 64 factors. The wide kernel represents every node set by an integer ID in
`PackedNodeSetArena`, whose contiguous `long[]` words and parallel offset/length/hash arrays implement collision-safe
interning, union, difference, intersection, subset, minimum-member, and set-bit iteration without per-set objects.
`PackedDphypReceiver` receives node-set IDs rather than Java objects or raw one-word masks. `PackedJoinHypergraph`
stores hyperedge endpoint IDs and supplies a one-word specialized view when legal. The receiver passes every legal
connected-subgraph/complement pair to Cascades; the consumer combines complete continuation/Pareto frontiers from
both sides rather than recording only singleton extensions. Cost and cardinality never participate in topology
generation.

Run proof-checked rewrites, binding/range fact propagation, hypergraph construction, DPhyp enumeration, Cascades
candidate installation, and dependent-parent reactivation as one idempotent worklist. A work item is keyed by memo
expression ID, catalog rule ID, fact revision, and hypergraph revision. Installing a new proof-carrying logical
alternative recomputes only affected value-flow facts and join edges, then re-enumerates newly reachable CSG/CMP
pairs. Installing a new physical or costed alternative requeues every parent whose continuation/Pareto result can
change. Structural interning suppresses duplicate expressions and node sets; every semantic rule declares either
idempotence or a well-founded decreasing measure, so an exact run terminates when no rule, edge, pair, candidate, or
parent remains queued. The completion certificate records catalog, fact, rewrite-saturation, hypergraph, node-set,
cost-policy, and budget revisions.

Use CD-E/CD-D-style hiding and connectivity reasoning for typed non-inner legality, but state its proved boundary:
the 2025 Birler/Neumann CD-E result is complete for non-decomposable predicates, not for arbitrary decomposable
conjunctions. The worklist consumes safe predicate decomposition and equality/range alternatives from Milestone 2
and reopens connectivity when a later equivalence exposes an edge. Explanations distinguish CSG/CMP enumeration
completeness from rewritten-input completeness and record any bottom-up restriction, connectedness approximation,
excluded rule, or incomplete budget status.

Support legal bushy alternatives for inner joins and typed non-inner constraints: LEFT/OPTIONAL, SEMI/ANTI, MINUS,
EXISTS/NOT EXISTS, LATERAL/dependent joins, subqueries, paths, filters, and Cartesian components. Dense subset storage
for 0--16 factors, sparse one-word storage for 17--64, and interned multiword storage for 65+ share the same receiver,
memo-installation, continuation, Pareto, resource, and completion-status contracts. Any approximate graph
simplification is a separately named opt-in mode whose explanation lists excluded alternatives and never claims
exact completion.

Acceptance uses exhaustive small-hypergraph CSG/CMP and winner oracles, randomized conflict-rule graphs, rewrite
sequences that expose connectivity only after equality/range propagation, correlated/noncommutative interactions,
and end-to-end result bags. Differential tests run identical graphs through the one-word and padded multiword kernels
at 16/17, 63/64/65, 127/128/129, and compare normalized pair sets, alternatives, Pareto survivors, and selected root.
The explanation reports rule/proof decisions, rewrite and graph revisions, generated pairs, legal rejections,
continuation classes, Pareto survivors, retained bytes, completion status, and final policy choice.

### Milestone 6: integrate LEO-plus with the unified state and Pareto model

Keep the implemented hierarchical Normal-Inverse-Gamma model as the selected statistical baseline. A logical cell
models absolute `log1p(cardinality)` for canonical logical expression plus exact `LearningApplicability`; equivalent
physical alternatives share it symmetrically. A physical cell models
`log1p(actual work) - log1p(conventional predicted work)` for `PhysicalResidualKey`. Each cell exposes posterior
location, predictive variance, effective evidence weight, and a lower/point/upper predictive interval. Exact current
facts override the posterior. Bounded family shrinkage may inform a cold exact key only through the existing capped
contribution contract and never changes applicability identity.

Price deterministic dependent work before learning: startup once, invocation count, first-match and exhaustion work,
rebind/close, output per execution partition, distinct cache misses, cache hits/evictions, materialization builds and
lookups, and spill. Learning corrects only the originating logical transform or physical residual dimension and
propagates its predictive interval and lineage to the Milestone 3 cost algebra. It never authorizes rewrites, invents
a missing Frontier payload, or uses the executed physical shape as logical identity.

Use hard applicability followed by a calibrated conformal out-of-distribution gate. Categorical identity must match
dataset and statistics epochs, semantic and correlation scope, bound-input/range-shape bucket, evidence guarantee,
and, for physical residuals, algorithm, access path, cache, materialization, and execution mode. At plan creation,
pin a primitive numeric feature vector containing `log1p` conventional rows, `log1p` expected invocations, bound-input
cardinality, normalized proven-range width when known, and operator fan-in. Maintain robust center/scale and recent
nonconformity scores per applicability family. After at least 32 admissible calibration observations, reject a feature
vector whose normalized distance exceeds the empirical 99th percentile; before that support exists, or with fewer
than three exact-cell observations, return `INSUFFICIENT_SUPPORT` and use the conventional vector. Epoch/category
mismatch returns `INAPPLICABLE`; distance rejection returns `OUT_OF_DISTRIBUTION`. All three are explanation-visible
and make no learned change to the cost vector.

Classify observations before update. Completed and exhausted executions may supply point observations when their
runtime contract proves commensurability. A monotone early-out count may supply only a lower or upper censoring bound
recorded in `CensoredObservationBounds`; it may narrow a compatible predictive interval but never update the NIG point
statistics. Cancellation, failure, stale epoch, dynamic-lateral shape mismatch, ambiguous cache exposure, or an
unproved partial count remains diagnostic only. Repeated opens use saved per-open predictions and invocation sums;
logical truth is reconstructed from the canonical saved prediction rather than treating the invocation sum as one
cardinality.

Enforce predicate-narrowing monotonicity, partition consistency, stability, full-domain fidelity, and impossible-
range zero before a learned vector can rank plans. Use asymmetric regression control: compare the candidate's robust
upper objective with LastGood's robust lower objective plus the measurement-noise margin derived from the matched
baseline; do not switch when the intervals overlap. Require three consecutive admissible completed observations
before a new exact cell can influence Applying, record at most one sample per completed execution, quarantine the
cell after a catastrophic regression, and detect a repeated A-B-A-B plan sequence as period two. Quarantine restores
LastGood and requires a new data/model epoch or a successful held-out recalibration before release. `snapshot-only`
and deterministic modes neither read adaptive rankings nor mutate evidence. Full-query pilot/shadow executions remain
prohibited.

Begin this milestone with an additive replay prototype, not production ranking. Replay observations chronologically
and compare conventional estimates, the current NIG baseline, and NIG plus applicability, conformal OOD, censoring
bounds, invariants, and hysteresis. Freeze training and held-out query/dataset digests and write per-observation
predictions, intervals, OOD decisions, q-error, regret, plan switches, and lifecycle transitions under
`profiles/lmdb-opt/leo-plus-prototype/`. Promote the selected combined policy into production only when held-out
median q-error does not worsen, p95 plan regret is at most 10 percent, no completed query regresses more than 25
percent against LastGood, interval coverage meets its declared level, period-two fixtures stop oscillating, restart
replays the same decision, and planning time/allocation add no more than five percent. Otherwise retain Monitoring,
record the failed criterion here, and revise the mechanism rather than weakening a gate.

The cited comparison matrix for LEO, progressive optimization, validity ranges, consistency repair, Bao, LEON,
SkinnerDB, plan-management/query-store systems, AQO, Kepler/Lero, plan bouquets/SpillBound, and penalty-aware robust
optimization remains supporting evidence. The production choice for this plan is the scoped NIG baseline plus exact
applicability, conformal OOD fallback, interval-only censoring, invariants, asymmetric hysteresis, LastGood,
quarantine, rollback, and period-two detection described above; the implementer does not select a different model
without revising the Decision Log and rerunning the prototype gates.

### Milestone 7: unify cache replay, provenance, and final selection

Extend detached evidence and decision certificates so a selected recipe and every cache candidate retain the exact
state, continuation key, cost vector, child composition, proof, learning applicability, and alternatives needed to
revalidate the decision. Strict cache hits make zero estimator/store calls. Structural stale hits replay exact
dependencies or paired sampled evidence under current data, predicate-range, estimator, catalog, cost-policy, and
learning revisions. Missing states, invalid pruning proofs, incompatible versions, or insufficient confidence force
one full replan; they never merge old scalar telemetry into new costs.

The plan cache remains store-owned, bounded by both entries and detached evidence bytes, segmented, collision-safe,
and single-flight. No arena ID, cursor, LMDB snapshot, mapped-index lease, or evaluator object crosses the query
lifetime. Materialization follows selected primitive links once and creates only the winning `TupleExpr` tree.

Every explanation must answer which rules were considered, why they were legal or rejected, which CSG/CMP pairs
were enumerated, which continuation/Pareto alternatives survived or were dominated, which Frontier and learned
states priced them, whether cache validation changed a dimension, and which final policy selected the root. Explain
serialization may project scalars for humans but retains stable IDs/digests linking back to immutable events.

### Milestone 8: close the complete corpus and performance campaign

Capture all 117 Theme queries under the immutable `snapshot-only` policy with authoritative result-bag verification,
optimized structure-plus-estimate snapshots, and one atomic audit ledger. Classify semantic parity, rewrite legality,
packed codec coverage, search completeness/fallback, physical alternatives, Frontier coverage/degradation, estimate
quality, cost quality, cache behavior, and root cause. A cell is not closed while it has a wrong result, exception,
timeout, unsupported boundary, silent fallback, unsafe rewrite, missing legal alternative, unexplained scalar loss,
uncalibrated cost defect, or unclassified plan change.

After correctness is closed, capture matched process-cold and saturated-cache-miss planning, validated cache hits,
and fixed-plan execution matrices. First write the immutable baseline manifest required by Validation and refuse to
compare runs whose hardware, JDK, dataset/store digest, evidence mode, flags, forks, warmups, query digest, or planner
completion mode differs. Report exhaustive two-through-nine-factor correctness separately from synthetic 4, 8, 16,
17, 32, 64, 65, and 128-factor planning and from the 117-query application corpus. Require at least a tenfold
improvement in both equal-query geometric mean and fixed-corpus summed uncached planning time from that baseline;
report cached planning, incomplete searches, and execution separately. Retain the packed goals: at least 90 percent
of Theme cells below 5 ms p95, at least 50 percent below 0.5 ms, validated cache hits below 0.5 ms, four-factor
planning below 0.5 ms/512 KiB, and eight-factor planning below 5 ms/2 MiB. Report rather than hide statistically
inconclusive cells.

Plan quality must have median regret at most 1 percent and p95 at most 10 percent against the manifest's measured
legal-alternative set. For large queries that set contains every retained root Pareto survivor plus a deterministic
stratified sample of dominated alternatives; only exhaustive small queries claim global measured optimality.
Cardinality q-error, interval coverage, exactness, Frontier authority, result multiplicity, memory, and execution
time must be unchanged or improved. SOCIAL_MEDIA q9 retains the measured
`VALUES -> name -> ab -> da -> cd -> bc` order unless new matched evidence proves a better legal plan;
SOCIAL_MEDIA q4 remains the five-probe streaming anti case; HIGHLY_CONNECTED q10 and LIBRARY q10 retain beneficial
bounded materialization; MEDICAL_RECORDS q9 remains in the safe performance class across feedback, interleaving, and
restart. Profile the slowest cells with the supported JFR loop and optimize only profile-proven algorithm, layout,
locality, or duplicate-work costs. For any vectorized or compiled execution alternative, record interpreter baseline,
cold compile/first execute, warm cache hit, generated shape/size, allocation, cache/classloader behavior, and forced
fallback. Do not attribute a gain to HotSpot inlining, scalar replacement, an intrinsic, or vectorization without
JDK-25-or-newer JFR/compilation evidence for the measured method.

### Milestone 9: make Frontier Statistics V2 query-ready at 20 billion triples

This milestone supersedes the persistent and query-time implementation of the current Quad/Omni synopsis plus raw
Frontier payload plus derived `FrontierQueryIndex` pair. It does not replace the generic `FrontierStateArena`,
operator transforms, Pareto planner, or evidence contracts. One store-owned `LmdbStatisticsService` publishes
immutable, checksummed, directly queryable generations. `LmdbQuadSynopsisService` and
`LmdbFrontierSynopsisService` remain temporary delegating facades while configuration and stored generations migrate.

The resource contract is arithmetic, not a suggestion. For a JVM with `-Xmx8g`, all statistics services share one
2,147,483,648-byte governor:

    mapped-generation metadata + mutation tail       268,435,456 bytes
    guaranteed query scratch                         268,435,456 bytes
    builder/compactor maximum                      1,342,177,280 bytes
    permanently uncommitted safety margin            268,435,456 bytes
                                                     -----------------
    governed heap total                            2,147,483,648 bytes

One session may hold at most 16 MiB. Ordinary sessions target at most 4 MiB. Query leases may borrow builder capacity
up to 512 MiB aggregate; the governor revokes or denies background leases before denying query scratch. A denied
query lease yields a typed wider interval or the conventional estimator, never an LMDB replay, allocation failure,
or planning exception. Mapped file pages are outside this heap accounting and are never prefaulted.

The steady statistics cap is 137,438,953,472 bytes and the initial envelopes sum exactly to that value:

    exact totals, heavy keys, distincts, linear sketches     16 GiB
    Omni leaf cells, witness tuples, postings                32 GiB
    coordinated center/edge/path/cycle samples               32 GiB
    workload-driven predicate/role/shape refinements         40 GiB
    active deltas, manifests, checksums, compaction reserve   8 GiB
                                                               ------
                                                               128 GiB

Compaction may use at most 34,359,738,368 additional bytes, and initial migration requires 160 GiB free. Unused tier
capacity is reassigned only by measured held-out q-error reduction per retained byte. A one-row-per-statement version
of today's 72-byte derived index would require 1,440,000,000,000 bytes at 20 billion statements before filesystem
overhead, so V2 may not preserve that representation under a different allocator.

Each immutable data shard is at most 1 GiB. Shard-local row and witness ordinals may be unsigned 32-bit values, but
global row counts, epochs, sequence IDs, byte offsets, products, and cardinalities are `long`. Estimates multiply in
saturated nonnegative `double` space. Term columns use the minimum generation-wide width
`ceil(log2(maxTermId + 1))`; a domain of 20,000,000,000 IDs needs 35 bits, while the format supports one through eight
bytes. Locally implemented frame-of-reference and delta-coded blocks, bit-packed columns, deduplicated witness
tuples, and sorted postings avoid a new dependency. Headers and directories map lazily; no production code calls
`MappedByteBuffer.load()`. Structural lengths and directory checksums validate on open, and data-block checksums
validate on first access.

The builder holds one pinned LMDB read snapshot and makes at most two sequential snapshot scans. Pass one computes
exact scalar totals, cell populations, heavy-key candidates, degree moments, HLL/KMV summaries, retained-size
models, and sampling thresholds. Pass two writes only selected witnesses and coordinated center/edge/path/cycle
samples to partitioned sequential event files, externally sorts one bounded shard at a time, and emits query-ready
shards. It never retains a dataset-sized Java array. Four design lanes and two independent audit lanes use `2^14`
cells per attribute/lane and initially target 8,192 alpha-threshold witnesses per populated cell; pass-one counts
reduce thresholds before emission when the measured encoding would exceed its tier. High-degree centers use
two-stage priority sampling with bounded neighbor reservoirs rather than exact edge expansion.

At 20 billion triples, two scans are 40 billion tuple visits. At 300,000 visits/second the nominal duration is
133,333 seconds, or 37.04 hours. Qualification accepts the projected upper 95-percent confidence bound only when it
remains below 48 hours including 25-percent contingency. The builder is `O(N)` plus external sorting of retained
samples, uses at most governed heap, and emits at most configured disk. It pauses when foreground query p95 or write
throughput regresses by more than five percent.

Every published generation contains exact global and bound-mask totals, heavy-key directories, mandatory all-context
and named-context projected-distinct summaries, Omni-style witness cells, coordinated center and edge samples,
signed Fast-AGMS/CountSketch projections, confidence calibration, and its own directories/postings. Predicate-heavy
projected-distinct scalar rows are an optional query-ready accelerator. Leaf estimation intersects witnesses and
combines exact totals, heavy keys, membership/distinct summaries, and calibrated intervals.
`addBoundVariableEvidence` consumes `estimateProjectedDistinct(probe, component)` and never enumerates LMDB terms.
Shared-center stars use coordinated
center samples and degree vectors; supported alpha-acyclic paths/trees use multi-join witness intersections; cycles,
sparse foreign-key joins, and weak-witness cases combine independent Fast-AGMS lanes with coordinated edge samples.
Estimator selection minimizes a calibrated confidence interval against held-out audit lanes, not a fixed preference
order. Unsupported algebra receives a typed conservative interval.

`READY` means every mandatory query shard is present, structurally valid, mapped, and leaseable. Publication writes
and fsyncs shards, writes and fsyncs the manifest candidate, atomically renames the manifest last, then fsyncs the
directory. Reference-counted generation leases keep replaced shards alive. Immutable shard handles are shared across
adjacent manifests and charged once; generation-local directories retain their own smaller lease. Rollback atomically
points at the prior valid manifest. A failed optional refinement widens an interval; a failed mandatory shard leaves
the preceding generation authoritative.

No optimizer route may call LMDB statement count, statement iteration, or distinct enumeration. Statement-pattern
`EvidenceStateRef` values refer to mapped sketch slices plus scalar summaries; query-owned VALUES and bindings may
remain exact without entering a statement payload writer. Remove `prepareReplayableSources` and
`materializeSnapshotExactLeaf` for statement leaves. Rewrite exact joins as bounded merge/intersection or sketch
contraction. Never compute an allocatable `leftEntries * rightEntries` pair count, narrow it to `int`, or construct a
Cartesian tuple buffer. Query work is bounded by patterns, lanes, and retained K, approximately
`O(patterns * lanes * K)`, independent of store cardinality.

Effective statement changes append a compact journal row in the same LMDB write transaction: operation, explicit or
inferred scope, epoch/sequence, and four term IDs. Commit merges the transaction into a fixed-size signed in-memory
delta, and an immutable delta shard publishes within 60 seconds. Restart replays journal sequences strictly after
the last covered sequence and is idempotent. CountSketch/Fast-AGMS deltas support insertions and deletions. Alpha-
minwise reserves target 25-percent base-layer deletion churn with per-shard exhaustion probability below `10^-6`.
Rebuild begins at 10-percent deletion debt; a witness cell ceases to be authoritative before valid support falls
below K, while signed linear estimates remain safe. Compaction replaces one shard or one layer no larger than 32 GiB
at a time, so a manifest can combine independently versioned base layers and deltas.

Implement in six test-first slices:

1. **M9.0 safety invariant.** Reproduce `READY` raw evidence with an unavailable query view and three statement
   leaves. A counting source makes the test fail on any planning count/iteration. Remove exact replay and retain
   query-owned finite relations. Add large-cardinality contracts above `2^31`, `2^32`, and 20 billion proving that
   joins neither narrow nor allocate Cartesian pairs.
2. **M9.1 governed query-ready substrate.** Add the global purpose-aware heap governor, structured fallback/status
   values, shard/manifest codecs, lazy segmented mapping, checksums, atomic publication, and generation leases.
   Round-trip, truncation, corruption, partial-publication, crash-recovery, and lease-safe replacement tests precede
   their production implementations.
3. **M9.2 bounded two-pass build.** Add pass-one summaries and sizing, sequential partitions, bounded external sort,
   compressed columns/postings, and mandatory leaf plus linear-sketch shards. Tiny-governor tests force spill/pause;
   no test or production builder may allocate proportional to snapshot cardinality.
4. **M9.3 estimator ensemble.** Wire bound-mask leaves, all/named/heavy projected distincts, bounded finite domains,
   safe finite-filter anchors, center/star, path/tree, cyclic, sparse, and conditional estimators into both
   storage-estimator and packed-state paths. Exact-oracle/property tests cover all bound masks, all 16 join-role
   pairs, named contexts, explicit/inferred planes, skew, hubs, sparse foreign keys, paths, stars, cycles, and nested
   SPARQL algebra.
5. **M9.4 online lifecycle.** Add transactional journal rows, signed deltas, delete reserves, restart replay, rolling
   layer compaction, facades, asynchronous startup, V1 beside V2 migration, shadow validation, atomic promotion, and
   delayed cleanup. Insert/delete/rollback/duplicate-replay/restart/lag/reserve tests precede each behavior.
6. **M9.5 qualification.** Run JMH and CPU/allocation JFR for warm and cold planning, concurrent build plus 64
   planners, and scale points 100M, 250M, 500M, and 1B. Fit build time, retained bytes, update cost, and planner
   latency; require linear build scaling with `R^2 >= 0.99` and project the upper 95-percent bound to 20B with
   25-percent contingency.

The public mode remains `off|shadow|authoritative`. `frontierSynopsisBudgetBytes` becomes the complete steady disk
cap. Add `frontierHeapBudgetBytes` (default one quarter of `Runtime.maxMemory()`),
`frontierStatisticsMaxLagMillis` (default 60,000), and `frontierDeleteReserveFraction` (default 0.25). Parse but
deprecate `frontierQueryIndexBudgetBytes`, `frontierQueryMemoryBudgetBytes`, and
`frontierInitialMaterializationWorkUnits`; V2 ignores index/materialization settings and maps the legacy query-memory
value only when the global heap setting is absent.

Acceptance at scale requires zero statement-index planning I/O; warm p95 at most 20 ms and cold local-NVMe p95 at
most 100 ms for ordinary three-to-ten-pattern queries; leaf p95 q-error at most 2, join p95 at most 4, and complete-
plan p99 at most 10 on the frozen qualification corpus; mutation lag at most 60 seconds; ordinary query scratch at
most 4 MiB and per-session scratch at most 16 MiB; total statistics heap at most 2 GiB; steady disk at most 128 GiB;
temporary disk at most 32 GiB; and foreground p95/write-throughput regression at most five percent during build.
Accuracy authority remains provisional until the shadow-mode 1B qualification passes.

## Surprises & Discoveries

- Observation (2026-11-08): the partial residual quote reused the physical-access field after that field had been
  deliberately cleared to unknown. The modeled prefix candidate therefore vanished even while the original local
  variable still held it. A real iterator showed that the candidate population is not a count of visited keys. The
  missing new metric failed before the repair, while the quote-input loss is established separately by source trace.

- Observation (2026-11-08): invalid UNKNOWN intervals with equal endpoints, including infinity, negative values and
  out-of-range points, returned unavailable evidence before numeric validation. The actual provider quote and
  boundary tests also showed that a lineage getter could run and fail before the invalid numeric interval was rejected.

- Observation (2026-11-08): `Double.compare` distinguished signed-zero endpoints that the cost arena treats as
  equal, allowing an UNKNOWN degenerate declaration to become READY. Both quote and boundary reproduced the mismatch
  using the actual owner with fresh and materialized interval-builder storage. Exact zeros remained valid.

- Observation (2026-11-08): a raw coverage quote, boundary quote or composition invoked from a strategy quantity
  getter could reset the shared arena builder after earlier outer quantities were written. The real provider red
  changed expected source interval `[27, 29, 31]` to `[120, 120, 120]` or `[40, 40, 40]`. An inactive or reopened
  request could also bypass attribution of a raw work/memory refusal, while interrupted stopped requests returned
  normal outcomes. The complete red and post-correction control selection are preserved in the review-fix evidence.

- Observation (2026-11-08): a different borrowed memory handle for the same budget was accepted by the first
  factory candidate, and replay treated a provider `IllegalStateException` as checkpoint corruption. A six-callback
  run continued after the first failure and surfaced the close error as primary. Evidence:
  `/private/tmp/rdf4j-phase1-bc-20261108-qleCkGY3/evidence/same-budget-foreign-memory-red/` and
  `/private/tmp/rdf4j-phase1-bc-20261108-qleCkGY3/evidence/checkpoint-identity-red-stack/`.

- Observation (2026-11-07, upstream reconciliation): the upstream selector supplies native-local typed feature
  quotes, static priors, candidate offers, bind-time decline and reranking, DISTINCT/row-demand handling, decision
  diagnostics, and an optional advisory learner. `LmdbNativeAccessFacts` improves prefix/output estimates, but
  native still chooses after packed planning, uses its own time pricing, and does not consume a packed executable
  strategy descriptor or the shared packed search/provider budget. These advances partially supply phases 2, 3, 5,
  and 7; they do not close those phases. The learner's coarse native key is not a substitute for recipe-, consumer-,
  snapshot-, and pricing-version-qualified feedback.
  Evidence: merged `LmdbNativeDispatchLoop`, `LmdbNativeSelector`, `LmdbNativeCostQuote`, `LmdbNativeCostVectors`,
  `LmdbNativeStaticPrior`, `LmdbNativeOffers`, `LmdbNativeAdvisoryLearner`, `LmdbNativeAccessFacts`,
  `PackedPlanRecipe`, and `LmdbPhysicalCostObjective`; independent review of `d69e9dd27a..855a77e816`.

- Observation (2026-11-07, static review): native ordinary-versus-wildcard kernel and some grouping/prefix
  dominance declarations rely on measured cases and can suppress alternatives before price or advice comparison.
  The source does not yet establish a substitutability and work bound for every relevant input and continuation.
  This is an open phase-3/5 strategy-retention obligation, not a demonstrated wrong result or measured regression.
  Evidence: `LmdbNativeKernelExecution.ordinaryKernelDominates`, grouping candidate declarations in
  `LmdbNativeGroupStep`, and elimination in `LmdbNativeSelector`.

- Observation (2026-11-07, merge qualification): one 98-test run reported a parsed DISTINCT/OFFSET quote mismatch
  (full 3.2742523189196753 versus offset 165.3575540779186 interpreted work). The same untouched selection later
  passed 98/98, and two fresh-store repetitions of the unchanged assertions also passed. A low-interference
  observer saw ordinary q-then-p prefix scans and the same 165.3575540779186 quote for full and offset in passing
  runs. The failing run did not retain its selected algebra or snapshot identity; its saved logs contain no mapped
  lifetime exception or planning-fallback warning. Preserve the one failure as unresolved, non-reproduced evidence;
  do not infer a cause, change the assertion, or call the issue fixed. A possible mismatch between a packed
  DISTINCT-skip leaf estimate and an ordinary native scan remains a separate hypothesis to test on the actual
  selected input before relying on it.
  Evidence: `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/retry-regressions/37-*`, `40-*` through `43-*`.

- Observation (2026-11-06, source inspection): the actual planning chain is `LmdbQueryOptimizerPipeline` request-bound rewrites → `LmdbCascadesOptimizer` / packed selection → native row/group compilation and native arbitration; runtime statement access independently chooses LMDB or adjacency. `LmdbPackedCostModel` currently starts ordinary access rows from returned rows, except for an additional repeated-component population check. The existing `LmdbNativeStrategyPriorityTest` has ten `@Test` methods and all ten are disabled, so it is not active strategy coverage. `LmdbFrontierPlanningBenchmark` already supplies an eight-pattern, two-index planning fixture; its warm and renamed-query methods are controls for planning only, not a substitute for the required full workload and fixed-plan execution lanes.
  Evidence: `LmdbQueryOptimizerPipeline.java:303-309`, `LmdbPackedCostModel.java:455-508`, `LmdbNativeGroupStep.java:1458`, `LmdbNativeRowStep.java:1708-1716`, `LmdbSailStore.java:17924-17979`, and `LmdbNativeStrategyPriorityTest.java:117-279`.

- Observation (2026-11-06, source inspection): `PackedSearchBudget` deliberately records a refused optional block without latching it; a smaller block can still fit. Its current mandatory-completion operation still draws from that same deterministic allowance, while the new design requires a separately accounted minimal valid fallback. Existing declared-quantity tests already check unknown axes through copies, metadata, and trace, so extensions must preserve those routes as well as selection and recipe detachment.
  Evidence: `PackedSearchBudget.java:66-125` and `PackedDeclaredQuantityReplayTest.java:38-81`.

- Observation (2026-11-06, independent source review): selected packed metadata is lowered into planned metrics and existing join hints, then native compilation reconstructs slot plans and independently arbitrates native strategy and statement access. `PatternPlan`'s stored index name alone is not enforced when its iterator opens. `NativePhysicalInputRecipe` captures several leaf/unary inputs and semantic correspondence but not arbitrary join/filter/union regions; its match deliberately ignores pattern order. A selected-to-executed contract must connect the materializer, native compiler, group/row arbitration, and statement-access binding without relying on test-only forced strategy selection.
  Evidence: `PackedPlanMaterializer.java:848`, `LmdbNativeEvaluationStrategy.java:558`, `LmdbNativeAggregateCompiler.java:339`, `LmdbNativeGroupStep.java:1458`, `LmdbSailStore.java:17905`, and `NativePhysicalInputRecipe.java:63-113`.

- Observation (2026-11-06, independent source review): `QueryModelNode` metadata can carry an immutable detached descriptor through ordinary clones, but is transient and shallow-copied; node replacement and the LMDB cache's explicit value-copy preflight, detached-copy, and retained-accounting paths require deliberate handling. Planned string/double metrics cannot carry the typed descriptor. Existing unforced indexed-count and adjacency-observer fixtures can provide actual strategy/source evidence for an end-to-end handoff test.
  Evidence: `QueryModelNode.java:462-563`, `AbstractQueryModelNode.java:194`, `LmdbCacheValueCopies.java:386-395,468-480,622-628`, `NativeAggregateMetadataCostTest.java:228`, and `LmdbDirectAdjacencyQueryTest.java:299`.

- Observation (2026-11-06, initial control run): the existing uncached planning benchmark emitted two `Packed cascades planning failed; falling back to the pipeline-normalized plan: Mapped planning session is outside its live owning read scope` warnings in one JMH invocation (the second fork's warmup iteration 2 and measurement iteration 3). The run completed, but its measurements combine intended planning and fallback and therefore cannot establish an acceptance baseline. Its trial-setup assertion alone does not check each measured invocation. Further matched runs were paused pending diagnosis; preserve the original run and establish a per-invocation check and correct read-scope behavior before treating this lane as evidence.
  Evidence: `/private/tmp/rdf4j-unified-costing-baseline-20261106T130209Z/benchmark-output.after-run-01-estimateUncachedMultiBridge.log` and `LmdbFrontierStatisticsCostSession.java:103-105`.

- Observation (2026-11-06, debugger diagnostic): an in-process run against the identical frozen JMH jar completed 3 warmup and 15 measurement iterations with a breakpoint on the authority failure, but did not hit it. An initial launch inherited suspended JDWP into JMH's helper JVM and, after debugger detach, reproduced one warning without capturing the failed predicate. The exact invalidating event remains unknown; do not weaken snapshot-currentness checks or infer that a writer caused the warning.
  Evidence: `/private/tmp/rdf4j-frontier-runtime-diagnosis-20261106T131653Z/` and `/private/tmp/rdf4j-frontier-runtime-diagnosis-20261106T133046Z/`.

- Observation (2026-11-06, follow-up diagnostic): the next attempt could not acquire JMH's machine-wide lock because a separate active `ThemeQueryBenchmark.executeQuery` process from the user's `rdf4j7` checkout held it. Do not bypass the lock, stop that process, or run resource-heavy builds alongside its measurements. Resume diagnostics and baseline controls when it exits; the pending access regression may then run.
  Evidence: `/private/tmp/rdf4j-frontier-lifetime-diagnosis-20261106T135151Z/` and the running process command line observed during the attempt.

- Observation (2026-11-06, test-first): adding child physical quantities uses `finiteOrZero` for each existing axis and then recomputes scalar work from those axes. This turns an unknown existing seek into the child's known seek and can also replace an independent producer price with a raw count sum. Dependent FILTER/subquery callers additionally feed an already-priced dependent winner back into the source-row axis; a non-unit-price test now reproduces the second conversion as 1110 rather than 210.
  Evidence: `PackedCostEstimate.java:522-549`, `PackedIncumbentSearch.java:922-931`, `PackedFilterRules.java:159-167`, `PackedJoinEnumerator.java:5281-5294`, and `initial-evidence.unified-costing-20261106.txt`.

- Observation (2026-11-06, build side effect): the test lifecycle's Spotless apply modified 13 previously clean, unrelated Java files in `core/queryalgebra/evaluation`. The exact tool-generated diff was retained in `/private/tmp/rdf4j-unified-spotless-recovery-ap6isf6_/formatting.patch` and then reversed without `git restore` or `git reset`; only the roadmap and intended test files remain modified. Future targeted verify commands use the repository's `-Dformatter.skip=true -Dimpsort.skip=true` properties to avoid reformatting unrelated files, with touched files formatted separately at the appropriate serialized gate.

- Observation (2026-11-06, static source inspection): the current branch already has 41 stable physical cost
  dimensions, explicit composition rules, packed physical properties, work/deadline/memory limits, provider memory
  accounting, and native proposals with deferred openers. The missing work is continuity across these boundaries,
  not a new object-based optimizer or a second cost-vector schema. Evidence: `PackedCostDimensionRegistry`,
  `PackedCostAlgebra`, `PackedPlannerLimits`, `PackedProviderMemory`, and `LmdbNativeStrategyProposal`.
- Observation (2026-11-06, static source inspection): `PackedCostSession.objectiveScore` defaults to `workRows`,
  while LMDB/native paths also have their own physical pricing and arbitration. Some packed statement-access
  estimates derive scan work from output estimates; native lowering separately recognizes whole-region strategies.
  The dated plan therefore separates access work, semantic cardinality, consumer demand, and execution identity.
  These are source findings and design priorities, not measured regressions or newly observed test failures.

- Observation: the Temurin 26 cached-read improvement is much larger than the OpenJ9 diagnostic suggested, but the
  cold first touch remains slower. In alternating baseline/candidate runs, single-block width-31 random access moves
  from 10.82/10.67 to 2.15/2.28 ns, regular 1,024-block width-31 access from 62.57/62.62 to 4.99/4.92 ns, and irregular
  width-31 access from 73.54/70.33 to 14.53/13.61 ns. Widths 1 through 64 and codecs 1 through 4 all improve with
  identical sinks. Open/close moves from 80.06/76.87 to 74.45/68.36 microseconds but per-open allocation grows from
  120,842 to about 133,295 bytes. Isolated irregular-block first touch moves from 13.58/14.92 to 20.54/21.13
  microseconds at width 31 and from 14.79/13.50 to 21.17/19.42 microseconds at width 63.
- Observation: the supplied adaptive-layout verifier uses Linux `/proc/self/maps` and cannot run unchanged on macOS.
  The repository regression exercises the same five-versus-eight physical-mapping contract through
  `BufferPoolMXBean`, while the supplied cross-format semantic verifier runs unchanged and passes on Temurin 26.
- Observation: the supplied shard correctness verifier passes unchanged against the pre-refactor reader, including
  widths 1 through 64, all four codecs, legacy and revision-3 layouts, irregular blocks, tail reads, and concurrent
  first touch. The implementation gap is native mapping topology and hot-path cost, not decoded-value semantics.
  Evidence: compiling `FrontierStatisticsShardVerifier.java` against `core/sail/lmdb/target/classes` prints
  `PASS widths=1..64, codecs=1..4, legacy+v3, irregular blocks, tail read-ahead, concurrent mapping`.
- Observation: complete-store MEDICAL q9 estimates the exact 24,971-row `rdf:type med:Encounter` leaf as one
  projected subject because four independent projected-distinct lanes are approximately `[0, 0, 106808, 0]`. The
  median is zero and `FrontierMappedStatistics` clamps it to one. Mapped semi/anti costing then copies that point into
  the correlation-domain upper bound, predicts one miss and 281,300 cache hits for a 4,096-entry cache, and executes
  thousands of correlated RHS scans instead of one materialization.
  Evidence: the all-theme q9 reproduction reports `actualSemiAntiDistinctCorrelationKeys=4.0K`, 6.6K iterator opens,
  52.6M source rows scanned, and a 60-second abort. Direct lane probes report three zero lanes and one
  `106807.741...` lane; their arithmetic mean is `26701.935...`, capped by the exact leaf at 24,971.
- Observation: switching to the mean only when a replica equals zero would remain downward biased for sparse
  all-positive samples such as `[1, 1, 1, 100]`. The observed zero is a symptom of inverse-probability sampling, not a
  statistically valid branch condition. Four replicas are also insufficient to fit a normal, log-normal, or robust
  contamination model without suppressing legitimate rare-key mass.
  Evidence: projected-distinct lanes use inverse inclusion weights in `FrontierOmniIndex.distinctWeight`; the equal
  weight design average of `[1, 1, 1, 100]` is 25.75 while its median remains one.
- Observation: restoring one-pass materialization removes the 52.6-million-row repeated-source regression but does
  not recover the older generic `Difference` runtime. The corrected typed semi/anti plan is stable near 360 ms/op,
  versus 136.542 ms/op for the older plan, because its runtime telemetry tracker is sized from the 4,096-entry memo
  cache even when the selected algorithm is materialized. Once all slots are occupied, every previously unseen key
  linearly probes the entire table before returning, making telemetry work proportional to outer rows times tracker
  capacity instead of outer rows.
  Evidence: the corrected plan reports a 25.0K point, 25.0K structural upper, 25.0K memoized-costing key count, and
  `materialized-minus-compatibility`. The JDK 26 JFR attributes 4,783 of 6,753 execution samples (70.83 percent) to
  `MaterializedExistsFilterIteration$PrimitiveDistinctBindingTracker.record`; GC pauses total only 311 ms over the
  roughly 102-second recording and no LMDB data-file reads are recorded. The older plan consumed about 99.6K left
  rows and 9.8K right rows without this typed-operator tracker.
- Observation: the retained historical comparison has two relevant latency anchors. The July generic `Difference`
  result is 149.444 ms/op; the earlier 136.542 ms/op number came from a different supplied comparison artifact and
  is not the acceptance baseline for this slice. The corrected typed materialized plan is 359.656 ms/op without JFR
  and 363.781 ms/op in the JFR qualification. Its algorithmic I/O shape is already correct, so the active work keeps
  that plan fixed while removing observation and key-probe overhead.
  Evidence: the corrected q9 JFR attributes 4,783 of 6,753 Java execution samples, or 70.83 percent, to
  `MaterializedExistsFilterIteration$PrimitiveDistinctBindingTracker.record`. The tracker is provisioned from the
  4,096-entry memo capacity even for materialized execution and performs a capacity-length probe for each new key
  after saturation.
- Observation: bounded exact/HLL distinct feedback and execution-local telemetry batching eliminate the residual q9
  gap without restoring generic `Difference`. After replacing lexical character hashing with type-aware semantic
  component hashes, the corrected typed materialized plan runs faster than the retained July anchor: 123.052 ms/op
  with telemetry disabled and 126.095 ms/op with sampled-full telemetry, versus 149.444 ms/op for the older plan.
  The final sampled-full JFR qualification is 129.915 ms/op. The hybrid tracker, HLL estimation, sampled timing,
  close-time metric publication, and feedback accumulation total 49 of 3,615 measurement-window Java top-frame
  samples (1.36 percent), or 242 samples (6.69 percent) including the combined iterator instrumentation. The largest
  individual instrumentation method is 2.49 percent, and sampled allocation records attribute zero bytes to the
  tracker, node accumulators, runtime registry, feedback wrappers, deferred join telemetry, or materialized key
  index. The exact-small/post-transition microbenchmarks are 11.154/7.260 ns per one-column update and
  20.609/11.615 ns per four-column update with 0.001 to 0.002 B/op profiler noise, confirming that update cost drops
  rather than grows after the exact prefix retires.
  Evidence: JDK 26 `ThemeQueryPlanRunBenchmark.runQuery`,
  `core/sail/lmdb/target/ThemeQueryPlanRunBenchmark.runQuery.jfr`, and `RuntimeTelemetryBenchmark.record`.
- Observation: benchmark-only sampled-full instrumentation preserves the prepared plan and result bag while staying
  below the general overhead gate. The disabled and sampled-full fixed-plan runs share structural fingerprint
  `b41b851fb423da34c309628f6c9034748963c960ca50f1330e4369cba2915945`; their final 3x3 results are 123.052 and
  126.095 ms/op, a 2.47-percent delta. The final sampled-full JFR result is 129.915 ms/op. Core telemetry methods
  account for 1.36 percent of measurement-window Java top frames, and the complete instrumentation surface including
  combined iterator wrappers accounts for 6.69 percent. The largest individual instrumentation method is
  `ResultSizeCountingIterator.hasNext` or `.next` at 2.49 percent; `SampledTiming.finish` is 0.75 percent and
  `HybridDistinctBindingTracker.record` is 0.53 percent. Sampled allocations attribute zero bytes to the tracker,
  node accumulator, runtime registry, feedback accumulator, deferred join telemetry, and materialized key index.
  Evidence: `LmdbBenchmarkQueryPlanTest#sampledTelemetryPreservesPreparedPlanAndResults`,
  `ThemeQueryPlanRunBenchmarkTest#runtimeTelemetryModeIsAnExplicitJmhParameter`, and
  `core/sail/lmdb/target/ThemeQueryPlanRunBenchmark.runQuery-sampled-full.jfr`.
- Observation: `FrontierSynopsisStatus.READY` currently certifies the raw payload, while planning needs a separately
  built `FrontierQueryIndex`. `LmdbFrontierSynopsisService.refreshQueryIndex` catches index build failures and records
  `query_index_build_failed` without invalidating the raw generation. An unavailable lease then enters
  `LmdbFrontierPackedCostSession.prepareReplayableSources`, calls statement counts, iterates exact LMDB leaves, and
  writes query-local payloads. `replayable_source_materialization_failed` therefore describes query-time statement
  replay, not Omni reconstruction.
  Evidence: `LmdbFrontierSynopsisService.refreshQueryIndex`, `LmdbFrontierPackedCostSession.prepare`,
  `prepareReplayableSources`, `snapshotCandidateRangeRows`, and `materializeSnapshotExactLeaf` form the complete call
  chain. `FrontierQueryIndex` charges 72 persisted bytes per retained row and builds five `long` columns plus four
  permutations before mapping the result.
- Observation: increasing query memory exposes a second independent defect rather than curing the first.
  `FrontierLinearTransforms.join` computes exact Cartesian candidate pairs as `long`, narrows them with
  `Math.toIntExact`, and allocates an `ExactTupleBuffer` of that size. Any representation that enumerates Cartesian
  pairs is asymptotically invalid at 20 billion statements even if the narrowing is changed.
- Observation: accurate mapped logical join rows do not automatically repair a physical alternative that was costed
  before the mapped state was restored. READY-V2 sparse planning produced accurate 120,000- and 32,000-row joins but
  retained 960,000,000 and 256,000,000 hash candidates from the delegate's Cartesian product. In both cases the
  assured lookup mask equaled the complete compatibility mask, so every bucket candidate was already a logical
  match. Repricing the hash alternative after mapped refinement removes the false work without assuming correlation
  when masks differ.
  Evidence: `LmdbPackedCostModelV2SessionTest.mappedV2JoinIsAttachedBeforeAssuredHashCandidatesAreCosted` failed with
  10,000 candidates for an exact 25-row mapped join and now reports 25; all three `LmdbSparsePrefixCostTest` cases and
  the combined 61-, 166-, and 226-test suites pass.
- Observation: sampling complete selected centers is not bounded under RDF hub skew. A single selected 60,000-edge
  subject exceeded the domain writer, causing all four subject-center shards to publish zero rows; increasing the
  buffer only moves the same failure to a larger hub. Independent second-stage row sampling is required in addition
  to coordinated center sampling, and the query estimator must scale both probabilities.
  Evidence: `initial-evidence.frontier-omni-v2.txt` records the four zero-row shards in
  `highDegreeCenterUsesBoundedSecondStageNeighborSamples`; the green contract retains bounded nonzero rows in every
  lane and reconstructs the exact 900-million-row join within its interval.
- Observation: a shard version alone does not version a generation's estimator. Without format, capability,
  hash/bucket, layout, term-width, and ordinal-width identity in the manifest, a future reader can map structurally
  valid bytes produced by an incompatible selection function and return plausible but wrong estimates.
  Evidence: the manifest-identity red failed because `FrontierStatisticsManifest.formatRevision()` did not exist;
  manifest revision 5 now round-trips every field and rejects a hash-schema mismatch before publication.
- Observation: the repository already contains the right component ideas, but historical plans often made local migration
  compromises that became contradictory when treated as permanent architecture: one canonical scalar row count per
  subset, one scalar winner, and Pareto/feedback deferred as optional work.
- Observation: the 2026-08-04 continuation-frontier and DPhyp audits provide direct counterexamples to one-winner subset DP and
  show that the packed DPhyp consumer currently reduces bushy CSG/CMP enumeration to singleton extension.
  Evidence: `initial-evidence.continuation-dp.txt` records the cost-3 exhaustive winner versus the cost-102 one-slot
  result; `PackedJoinEnumerator.dphypSingletonExtensions` discards every pair that has no singleton side.
- Observation: `PackedCostEstimate` already carries many independent resource counters, but the decisive storage is
  still scalar.
  Evidence: `PackedWinnerTable.offerWithMetadata` accepts startup, total, comparison, and peak-memory values and
  `appendIfBetter` replaces the row; there is no list of nondominated vectors per continuation key.
- Observation: current width kernels do not merely vary in representation. The dense path, sparse 17--64 path, and
  65+ path have different search semantics, so the boundary can change the legal plan space.
  Evidence: the exactness audit is retained at
  `profiles/lmdb-opt/theme-audit-2026-08-04/dphyp-subset-kernel-exactness-and-performance-design.md`.
- Observation: the first 37 authoritative Theme snapshots exposed state loss across cycles, UNION/MINUS,
  disconnected components, projection, extension, GROUP, and OPTIONAL, while built-in estimate/actual summaries
  compared zero nodes.
  Evidence: `profiles/lmdb-opt/theme-audit-2026-08-04/snapshots/first-37-root-cause-report.md` and its TSV ledger.
- Observation: two generic packed rewrites have known SPARQL counterexamples independent of any Theme query:
  distributing a FILTER into the right side of MINUS, and treating DISTINCT self-JOIN/self-LEFT_JOIN as idempotent
  over heterogeneous compatible mappings.
  Evidence: `profiles/lmdb-opt/theme-audit-2026-08-04/rewrite-semantics-and-coverage-audit.md`.
- Observation: the pre-Milestone-3 source had already grown record-level exact flags, both O2S permutations, a
  second independent design lane, and audit-lane diagnostics while retaining v1 names; the remaining persistence
  defect was physical duplication of every exact row across every lane and direction.
  Evidence: payload/record v2 now writes one shared exact record per direction, expands it at the validated reader
  boundary, accepts v1 envelopes and records, and passes `LmdbFrontierSynopsisServiceTest`,
  `FrontierPayloadBlockFormatTest`, and `FrontierQueryIndexTest`.
- Observation: learned q9 instability was not one defect. Cumulative/per-open mismatch, logical/physical key
  asymmetry, deterministic reopen/materialization underpricing, and delayed lifecycle containment all had to be
  repaired together. A per-open divisor alone is wrong when predictions vary.
  Evidence: `LmdbMedicalQ9PlanStabilityIT`, `ProductionLearningKeyTest`,
  `FrontierLearningModelLogicalEstimateClampTest`, `LmdbRuntimeFeedbackTargetTest`, and internal feedback persistence
  format 20 now cover the corrected contracts; rollout still reports Monitoring by default.
- Observation: the literature rewrite catalog is broad but deliberately scoped to a 2026-05-21 paper snapshot and excludes pure
  join ordering, costing, and implementation inventory; it is therefore a seed, not the required complete catalog.
- Observation: Birler and Neumann's 2025 CD-E algorithm is complete for the paper's non-decomposable predicate regime,
  but not for decomposable conjunctions: the reported results fall from 100 percent complete queries/plans to 85.5
  percent complete queries and 96.3 percent of plans. The paper also exhibits a legal plan that requires rewriting an
  outer-join predicate through an equality established by an earlier reorder.
  Evidence: `papers2/papers/docs/neumann-birler-umbra-query-optimizer-research.md`, Sections 1 and 5, with the
  canonical paper's Tables 4 and 5 and limitations on PDF pages 10--11.
- Observation: DPhyp's CSG/CMP enumeration theorem is separable from the paper pseudocode's scalar `dpTable` winner.
  The enumerator can feed a continuation-classed Pareto memo without changing its duplicate-free topology logic.
  Evidence: the focused research note's DPhyp section and the existing cost-3/cost-102 continuation oracle.
- Observation: Umbra keeps an online reservoir sample and updateable HyperLogLog sketches while representing physical
  plans as pipeline/step state machines. GroupJoin's winner changes with selectivity and memory, and its cost
  coefficients are explicitly system-dependent.
  Evidence: the research note's estimation and costing sections; Umbra PDF page 5 and GroupJoin PDF page 8 were
  rendered and visually checked under `papers2/papers/tmp/pdfs/optimizer-research/rendered/`.
- Observation: LEO already warns that early termination can censor operator counts and keeps adjustments separate from
  base statistics. Later drift and learned-estimator studies support residual correction, invariant checks, and OOD
  fallback rather than replacing current evidence with an unconstrained learned scalar.
  Evidence: the research note's LEO-plus section and the canonical LEO, robust-drift, learned-readiness, and Delta
  papers listed there.
- Observation: Indexed Algebra's producer/consumer links and path-centric analysis avoid the quadratic space and
  invalidation cost of storing expanding binding/column sets per operator. That working index answers different
  questions from Cascades memo equivalence and must not be conflated with it.
  Evidence: the research note's working-representation section; Indexed Algebra PDF page 3 was visually checked.
- Observation: the standalone repository `process-resources` formatter can remain silent for several minutes in this
  checkout even though the full `-Pquick clean install` completes in roughly 35--40 seconds.
  Evidence: during the 2026-08-06 planning cleanup both parallel and serial standalone runs were bounded and stopped;
  the subsequent full reactor install completed successfully without unrelated tracked edits.
- Observation: the current DPhyp API cannot implement the plan's former 65+ exactness claim.
  Evidence: `PackedDphypEnumerator.Receiver` accepts raw `long` sets and `PackedJoinHypergraph.MAX_NODES` is
  `Long.SIZE`; the separate 65+ join kernel is not a multiword DPhyp enumerator.
- Observation: exact continuation/Pareto width is unbounded in the worst case, while the current planner budget owns
  only work and deadline state.
  Evidence: `packed-continuation-equivalence-frontier-design.md` records factorial prefix and exponential frontier
  growth; `PackedSearchBudget` has no retained-byte field or allocation admission protocol.
- Observation: the former milestone order asked DPhyp hypergraph construction to consume rewrite-derived edges before
  the rewrite/range milestone created them.
  Evidence: the reviewed order placed DPhyp in Milestone 4 and semantic/range saturation in Milestone 5, while the
  Birler/Neumann research note requires resaturation or connectivity rebuild after relevant rewrites.
- Observation: `PackedCostEstimate` contains many dimensions, but listing fields does not define how sequential,
  concurrent, reopened, materialized, uncertain, or overlapping-memory plans compose or dominate.
  Evidence: existing costing uses local scalar formulas such as saturated addition and `Math.max`; no shared
  dimension registry or algebra currently enforces one interpretation.
- Observation: the implemented learned baseline is already a hierarchical Normal-Inverse-Gamma model over logical
  absolute values and physical log residuals, so LEO-plus can add calibrated applicability/OOD and lifecycle safety
  without replacing the useful statistical core.
  Evidence: `FrontierLearningModel` declares that model and exposes posterior mean, variance, precision, evidence
  counts, logical absolute estimation, and physical residual estimation.
- Observation: shared GROUP estimation had two independent semantic defects: a global aggregate over empty input was
  projected to zero rows, and keyed grouping used minimum marginal NDV instead of joint distinct support.
  Evidence: `initial-evidence.m0-group-global.txt` records expected/actual `1/0` and `100/10` failures.
- Observation: the generic FILTER optimizer clones a predicate into both MINUS operands even though only left-side
  distribution is equivalent; changing the exclusion operand changes the result bag.
  Evidence: `initial-evidence.m0-group-global.txt` records the exact algebra mismatch in
  `FilterOptimizerTest#distributesFilterOnlyIntoMinusLeftOperand`.
- Observation: DISTINCT does not make self-JOIN or self-LEFT_JOIN idempotent when one relation contains compatible
  heterogeneous binding domains, because joining can synthesize a merged mapping absent from the input.
  Evidence: `initial-evidence.m0-lmdb.txt` preserves both plan-shape counterexamples.
- Observation: Milestone 0 needed two linked inventories, not a rewrite list alone: immutable rule descriptors for
  runtime proof coupling and a typed surface ledger for packed opcodes, estimator/cost/cache/feedback boundaries,
  scalar degradation, and test oracles.
  Evidence: `OptimizerRuleCatalogTest` passes three tests against `OptimizerRuleCatalog`, while
  `initial-evidence.m0-catalog.txt` preserves the pre-implementation failures.
- Observation: a complete finite predicate-object domain could authorize FILTER-derived anchors, but the same proven
  store domain was invisible when no query FILTER named the object. Installing an anchor inside the pattern's own
  permanent group would create a memo cycle.
  Evidence: `initial-evidence.m2-stored-anchor.txt`; the repaired rule introduces a costed binding-side JOIN in the
  nearest observer group and the full 39-test range suite passes.
- Observation: range propagation stopped at SERVICE, but local facts were still seeded while recursively encoding the
  remote child, so the barrier arrived too late to prevent an unsafe remote EmptySet proof.
  Evidence: `initial-evidence.m2-service-range.txt`; SERVICE-depth-aware encoding now suppresses local provider calls.
- Observation: the literature manifest classified general rewrites but did not separately identify GroupJoin
  introduction and implementations, mark/semi/anti forms, runtime filtering, materialization, cache, enforcers, and
  the deliberate SQL `NOT IN`/`NOT EXISTS` non-rule.
  Evidence: `initial-evidence.m2-catalog-research.txt`; `OptimizerRuleCatalogTest` now passes six closed-world checks.
- Observation: the legacy runtime-feedback observation bridge rebuilt every feature vector from predicted rows alone,
  discarding invocation, bound-input, range-width, and fan-in coordinates. Repeated observations from one production
  context could therefore train a full envelope and query a different envelope, producing a false OOD fallback.
  Evidence: compatibility and semi/anti observation paths now reuse `LmdbRuntimeFeedbackDescriptor.featureEnvelope()`;
  the focused integration scenarios and the 124-test Milestone 6 suite pass.
- Observation: treating a censor-bound update as a posterior-point update advanced the learning revision and feature
  calibration even though NIG sufficient statistics were unchanged. The resulting floating-point posterior drift
  violated interval-only censoring.
  Evidence: `LmdbRuntimeFeedbackTargetTest#partialCountsOnlyCensorWhileCancellationAndFailureRemainDiagnostic`
  initially observed `4.6079194242363215` instead of `4.607920640839949`; point and bound change tracking are now
  separate and the focused test passes.
- Observation: raw NIG extrapolation is catastrophically unsafe outside its calibrated feature support even when the
  in-distribution residual is stable.
  Evidence: the frozen synthetic replay records raw-NIG q-error above 2.5 million for both extreme held-out points;
  the empirical OOD gate returns the conventional estimate with q-error 1.0 and replays identically after restart.
- Observation: stale validation refreshed scalar candidate totals but left the detached Pareto vectors at the prior
  generation, and the public plan exposed neither its complete decision alternatives nor a cache-stable explanation.
  Evidence: `initial-evidence.m7-cache-vector-replay.txt` observed a current expected work dimension of `7.0` while
  the certificate retained `1.0`; `initial-evidence.m7-explanation.txt` observed a null human explanation. Exact
  event-delta replay plus detached human/JSON rendering now passes the 41-test cache/recipe/memo suite.
- Observation: bounded dense planning ran two complete-seed campaigns before exact continuation expansion. The
  standalone campaign emitted every static greedy start into the memo, then `seedDenseCostedIncumbent` costed the same
  ordered prefixes again in the retained dense arena. This consumed the 256-work AUTO budget without adding a legal
  candidate.
  Evidence: `initial-evidence.m8-dense-seed-replay.txt` records twelve prefixes costed exactly twice; the pre-change
  Q6 CPU profile attributed 40.85 percent inclusive samples to greedy seed emission. Structural ownership reduced the
  matched SPARSE q6 steady mean from 106.170 to 93.357 ms/op and allocation from 70,724,161 to 59,334,807 B/op across
  two forks; the comparison is retained under
  `profiles/lmdb-opt/final-campaign/performance/sparse-q6-dense-exact-seed-owner-v2-20260809/`.
- Observation: exact projection still used a full `ExactTupleBuffer` even when the slot mapping was bijective. Such a
  mapping is injective over complete exact tuples and therefore cannot coalesce two distinct input rows.
  Evidence: `initial-evidence.m8-bijective-projection-red.txt` records a 640-row permutation exhausting a 64 KiB
  arena in `PAYLOAD_WRITER`; direct count-and-write projection passes that contract, the 20-test transform suite, and
  the 105-test LMDB integration suite. The matched Q6 pair measures 90.848 ms/op but no reproducible allocation
  change, so this slice is retained for bounded-memory correctness rather than an allocation claim.
- Observation: after exact projection stopped dominating, geometric `EmissionBuffer.grow` copied every tuple,
  weight, and stratum array while retaining the prior arrays. It accounted for 298 of 1,641 measured planning
  allocation samples (18.16 percent), while inclusive `Arrays.copyOf` accounted for 32.36 percent.
  Evidence: linked geometric segments remove the grow stack, reduce matched Q6 allocation from 59,522,298 to
  52,625,495 B/op (11.59 percent) across two forks, and reduce measured planning allocation samples from 1,641 to
  1,448. The comparison and before/after profiles are retained under
  `profiles/lmdb-opt/final-campaign/performance/sparse-q6-segmented-emission-buffer-v2-20260809/` and its referenced
  allocation-profile directories.
- Observation: a speculative 48 KiB integration threshold did not isolate emission growth: it remained red after
  segmentation because exact coalescing and retained emissions form a larger live set. The report is deliberately
  retained as `initial-evidence.m8-emission-segments-red.txt`, but it is not cited as proof for segmentation; the
  behavior-neutral refactor instead uses matching pre/post integration greens plus allocator hit proof.
- Observation: retained FILTER, extension, and OPTIONAL-condition mappings invoked the legacy one-shot scalar
  evaluation boundary once per tuple. Each invocation rebuilt a `QueryEvaluationContext.Minimal` and its compiled
  lambdas even though the expression, dataset, comparator, and stable query-time value were unchanged.
  Evidence: `initial-evidence.m8-scalar-precompile.txt` records the missing reusable interface contract; the new
  compile-once test and all 105 LMDB Frontier integration tests pass. Across matched ENGINEERING q0 replicates,
  allocation falls from 16,401,820 to 14,447,817 B/op (11.91 percent), while allocation-profile samples fall from
  6,784 to 6,075. Context samples fall from 256 to zero and the two compiled-lambda families from 579 to 16. Pooled
  latency remains neutral at 42.116 versus 41.952 ms/op. The comparison is retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q0-scalar-precompile-v2-20260809/`.
- Observation: stratified outer expansion allocated a capturing tuple-emitter callback and a one-element `long[]`
  counter for every source mapping, although output validation, reservoir state, and the exact tuple buffer all belong
  to one synchronous expansion campaign.
  Evidence: matching pre/post 20-test transform contracts and all 105 LMDB Frontier integration tests pass with one
  reconfigurable campaign-local emitter. On the matched ENGINEERING q0 allocation recording, all 217 callback samples
  disappear, measured planning samples fall from 6,075 to 5,693 (6.29 percent), and normalized allocation falls from
  14,262,635 to 13,152,870 B/op (7.78 percent). Latency remains neutral. The comparison is retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q0-reusable-expansion-emitter-alloc-v1-20260809/`.
- Observation: `PreparedProbeMemo` duplicated every selector and scope into eight primitive key columns even though
  its retained `PreparedProbe` already owned the immutable selector. Every growth generation allocated and rehashed
  the redundant columns.
  Evidence: the prepared probe now owns direction, lane role, and lane index as well as its selector; the memo retains
  only hashes and probe references. A 42-key characterization crosses three growth boundaries and proves selector,
  direction, and lane identity; all ten mapped-index tests and all 105 LMDB Frontier integration tests pass. On the
  matched ENGINEERING q0 recording, prepared-probe long-array growth falls from 275 samples to zero, normalized
  allocation falls from 13,152,870 to 11,984,253 B/op (8.88 percent), and time falls from 28.339 to 27.661 ms/op.
  The comparison is retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q0-prepared-probe-key-dedup-alloc-v1-20260809/`.
- Observation: the exact statement-probe memo duplicated each result's four-term selector and flags into five table
  columns, then copied those columns through every growth. The retained result was the natural exact-entry owner but
  did not carry its key.
  Evidence: keyed results let the open-addressed table retain only hashes and result references; accounting reserves
  the larger result object explicitly. A 40-result characterization crosses three growth boundaries, all three memo
  tests pass, and all 105 LMDB Frontier integration tests pass. On matched ENGINEERING q0, statement-memo long-array
  growth falls from 273 samples to zero and normalized allocation falls from 11,984,253 to 10,600,646 B/op
  (11.54 percent) while latency remains neutral. The comparison is retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q0-statement-probe-key-dedup-alloc-v1-20260809/`.
- Observation: the fresh q0 CPU campaign attributes 74.02 percent of exact planning samples to incumbent
  construction, but the apparent 23.84-percent `propagateChangedInputs` stack is dominated by the refinements it
  invokes rather than by rebuilding its small consumer graph. Mapped probing is the actionable leaf: 4,800 planned
  mapped probes perform repeated range preparation and center-completeness checks. Each statement row also rebuilt
  the same primitive selector in the snapshot fallback, and an exact statement-memo hit was tested only after center
  eligibility.
  Evidence: one canonical selector now serves the memo, mapped index, and snapshot paths; memo hits return before
  center eligibility; and `StatementProbePlan` no longer allocates a second `long[4]`. Matching pre/post bridge-
  mutation tests and all 105 integration tests pass. The retained 100-iteration q0 CPU pair reduces exact planning
  samples from 10,399 to 10,221 and steady time from 19.801 to 19.607 ms/op, while the allocation pair remains neutral
  at 10,600,646 versus 10,587,554 B/op. This is a small structural cleanup, not the missing tenfold gain. Profiles and
  the decision are retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q0-statement-probe-precheck-cpu-v1-20260809/`.
- Observation: component-level query-index range memoization looks attractive from the aggregate mapped-probe count,
  but the real q0 selectors do not repeat exact component/scope keys often enough. A four-bank direct-mapped exact
  cache reused 39 predicate searches in its focused contract yet left profiled binary-search work almost unchanged.
  Evidence: the spike passed all ten mapped-index and all 105 LMDB Frontier integration tests, but matched steady q0
  time regressed from 19.607 to 23.510 ms/op and exact planning samples from 10,221 to 13,923; `equalRange` samples
  moved only from 1,376 to 1,331 while cache bookkeeping added 1,354 samples. The production cache and test-only
  metrics were reverted. Red/green evidence remains in `initial-evidence.m8-component-range-memo.txt`; the rejected
  profile remains under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q0-component-range-memo-cpu-v1-20260809/`.
- Observation: a pure variable-alias `Extension` does not need to decode retained LMDB term IDs into RDF values and
  encode them again. The first primitive implementation removed that entire local stack but changed HotSpot escape
  analysis at a nearby per-probe mapped-index lambda, masking the win with 231 KiB/op of callback allocation.
  Evidence: `initial-evidence.m8-primitive-extension-alias.txt` preserves the missing-kernel red and sequential
  mixed-mask coverage; `initial-evidence.m8-mapped-probe-sink.txt` preserves matching pre/post mapped-scan greens and
  allocator Hit Proof. A reusable exact-expansion adapter and the existing statement-probe sink now carry transient
  inputs synchronously and clear them in `finally`. The final matched q0 allocation profile has no sampled
  `LmdbFrontierPackedCostSession` lambda allocation, reduces steady allocation from 10,587,554 to 10,509,647 B/op,
  and reduces steady latency from 27.452 to 26.721 ms/op. The 100-iteration CPU pair reduces inclusive
  `refineExtension` samples from 1,562 to 238 while preserving 19.597 ms/op steady planning; all 106 integration tests
  pass. The controlled profiles and decision are retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q0-primitive-extension-final-cpu-v10-20260809/`.
- Observation: exact scalar FILTER evaluation decoded every variable retained by the input Frontier layout even when
  the expression referenced only a strict projection of that layout. This multiplied value-store lookups across
  every exact or sampled tuple and made scratch accounting depend on irrelevant columns. A layout-ordered projection
  derived through the scope-aware `ScalarDependencyAnalyzer` correctly retained direct scalar dependencies and
  correlated outer names while excluding unrelated and subquery-local names, but its setup cost dominated the saved
  tuple decodes. Evidence: `initial-evidence.m8-filter-binding-projection.txt` records the red, the complete temporary
  implementation passed 108 Frontier planning integrations in `logs/mvnf/20260809-171307-verify.log`, and the matched
  profiles are `engineering-q0-filter-binding-projection-alloc-v12-20260809` and
  `engineering-q0-filter-binding-projection-cpu-v13-20260809`. Against the preserved pre-slice CPU profile, the latter
  moved steady latency from 19.597 to 23.485 ms/op (+19.84 percent) and allocation from 10,456,462 to 10,477,032 B/op
  (+0.20 percent), so the implementation and its temporary tests were reverted. Failed `IN` and unsupported-function
  reproductions remain separately preserved as test-construction evidence.
- Observation: correlated EXISTS costing repeated the same exact RDF evaluation for each physical semi/anti
  alternative and used every RHS variable as its memo key, including subquery-local variables that the outer mapping
  can never bind. A complete RHS correlation domain can answer positive, negative, partially bound, empty, and
  uncorrelated probes without evaluation, but only when the contributing Frontier states are resident
  `DATABASE_EXACT`; treating a sampled leaf as complete would manufacture false negatives.
  Evidence: `initial-evidence.m8-primitive-conjunctive-exists.txt`,
  `initial-evidence.m8-partial-exists-domain.txt`, `initial-evidence.m8-ordinary-exists-domain.txt`, and
  `initial-evidence.m8-shared-exists-memo.txt` preserve the four red contracts. Small exact BGPs now seal one
  primitive domain for EXISTS and NOT EXISTS; the full packed-codec class passes 16 tests and the full LMDB Frontier
  integration class passes 107 tests in `logs/mvnf/20260809-181437-verify.log` and
  `logs/mvnf/20260809-181517-verify.log`. A profiling-only diagnostic, removed immediately after capture, showed that
  TRAIN q8 correctly declines complete-domain sealing because its 133,849-row RHS leaves are
  `MEASURE_UNBIASED`, not exact. Correlation-only keys plus a query-local `(contextual logical group, correlation
  surface)` memo nevertheless reduce matched steady planning from 102.936 to 71.536 ms/op (1.439x), normalized
  allocation from 72,561,669 to 61,842,669 B/op (14.77 percent), and evaluator-fallback CPU samples from 17,626 to
  8,636 (51.00 percent). The retained matched profiles are
  `profiles/lmdb-opt/final-campaign/performance/train-q8-crosscell-cpu-v1-20260809/` and
  `profiles/lmdb-opt/final-campaign/performance/train-q8-shared-memo-cpu-v5-20260809/`; intermediate v2--v4 profiles
  isolate complete-domain, partial-key, and ordinary-filter effects.
- Observation: the full matched campaign confirms that group-scoped correlation memoization is a broad retained win,
  but not the missing aggregate mechanism. Every one of the 117 independently supervised candidate cells completed;
  the six baseline q11 artifacts that predate this candidate remain empty and are excluded symmetrically, leaving
  111 numeric pairs. Saturated candidate sum falls from 5,763.985 to 5,424.494 ms and the two aggregate speedups rise
  from 6.407x/3.492x to 6.808x/3.681x (fixed-sum/equal-query geometric).
  Evidence: the immutable candidate index and raw seven-sample results are retained under
  `profiles/lmdb-opt/final-campaign/performance/candidate-planning-cells-v5-exists-memo-20260809/`. TRAIN q8 improves
  to 105.362 ms/op under the process-cold campaign protocol, while HIGHLY_CONNECTED q8 is the lowest matched speedup
  at 1.309x and therefore the next profile-selected target. Neither tenfold threshold is weakened or rounded up.
- Observation: sampled single-statement EXISTS/NOT EXISTS paid for a general RDF evaluator step and opened a native
  statement iterator for every distinct outer correlation key even though the planning snapshot already exposes
  exact primitive term IDs. This was not estimator sampling: every individual bound lookup is exact, and one pinned
  cursor per selector shape can answer it without asserting that the sampled RHS synopsis is complete.
  Evidence: `initial-evidence.m8-primitive-exists-statement.txt` preserves the sampled-kernel red. The focused
  positive/negative, repeated-key, named-scope, complete-domain, and logical-group contracts pass three tests in
  `logs/mvnf/20260809-185425-verify.log`; all 108 Frontier planning integrations pass in
  `logs/mvnf/20260809-185654-verify.log`. On the matched 100-iteration HIGHLY_CONNECTED q8 profiles, steady latency
  falls from 51.195 to 44.177 ms/op, allocation from 35,288,568 to 27,187,281 B/op, and inclusive EXISTS samples from
  6,589 to 2,673. The retained pair is
  `profiles/lmdb-opt/final-campaign/performance/highly-connected-q8-low-speedup-cpu-v1-20260809/` versus
  `profiles/lmdb-opt/final-campaign/performance/highly-connected-q8-primitive-exists-cpu-v2-20260809/`.
- Observation: every center selected into an independent design or audit lane has a complete exact adjacency, but
  using those alternate mapped copies for conditional probes is slower than the already-batched snapshot cursor on
  HIGHLY_CONNECTED q8. The candidate moved 1.2K of 13.6K primitive snapshot selectors into mapped probes and reduced
  pinned cursors from 11 to 10, yet steady time rose from 44.177 to 45.920 ms/op (+3.95 percent) and normalized
  allocation from 27,187,281 to 27,321,887 B/op (+0.50 percent). The implementation and temporary contracts were
  reverted; `initial-evidence.m8-any-complete-lane.txt`, the green logs, and the controlled CPU/telemetry artifacts
  under `profiles/lmdb-opt/final-campaign/performance/highly-connected-q8-any-complete-lane-*` are retained. This
  rules out broader lane coverage as the next optimization unless range preparation itself is made materially
  cheaper.
- Observation: preferring the primitive snapshot for every eligible sampled conditional join also fails the matched
  CPU gate. It removed the remaining forward/reverse mapped conditional probes and reduced normalized allocation
  from 27,150,468 to 26,389,202 B/op (-2.80 percent), but the stable 100-operation profile consumed 25,382
  planning-thread CPU samples versus 23,217 for its immediately preceding control (+9.33 percent). A later wall-time
  run was invalidated by concurrent macOS storage analysis and likewise did not establish a win. The routing and its
  temporary preference contract were reverted; the original complete-mapped-selector memo test passes in
  `logs/mvnf/20260809-200041-verify.log`. The red contract remains in
  `initial-evidence.m8-primitive-snapshot-preference.txt`, and the controlled artifacts are retained under
  `profiles/lmdb-opt/final-campaign/performance/highly-connected-q8-primitive-snapshot-preference-*` alongside the
  `highly-connected-q8-any-complete-lane-reverted-cpu-v4-20260809` control. This experiment did expose a separate
  provenance defect: once factor-surface refinement installs a retained state and explicit source, generic sampled
  annotation must not overwrite that source. The root fix retains any applied factor-surface provenance and passes
  the existing over-budget resampling contract plus the 109-test integration class.
- Observation: widening a primitive selector's transaction read lease from one cursor row to the complete selector
  is correct but not the planning bottleneck. `initial-evidence.m8-selector-snapshot-lease.txt` proves the former
  per-row lease boundary, and the candidate passes all 11 pinned-snapshot concurrency contracts, but the matched q8
  allocation remains effectively unchanged at 86.82 MB/op. A planning-only CPU decomposition shows that the large
  `LmdbFrontierSnapshotSource.scan` stack belongs to benchmark synopsis setup; within planning, native selector work
  dominates the smaller batch stack, not the LongAdder lease pair. The longer writer-blocking implementation and
  temporary contract were reverted. Its invalid wall-clock run and CPU recording remain under
  `profiles/lmdb-opt/final-campaign/performance/highly-connected-q8-selector-lease-cpu-v7-20260809/` and are not used
  as acceptance evidence because concurrent Spotlight/storage analysis saturated the host.
- Observation: repeated calls to `averageIndependentDesignState` in q8 are not repeated lookups for the same primary
  state. A memory-accounted primitive state-ID memo preserved the sampled EXISTS contract but produced no cacheable
  common subproblem: inclusive samples stayed flat at 6,148 versus 6,211 under the same loaded host, median planning
  stayed at about 50 ms/op, and normalized allocation changed by less than 5 KiB/op. The memo was reverted; its
  profile is retained at
  `profiles/lmdb-opt/final-campaign/performance/highly-connected-q8-averaged-design-memo-cpu-v8-20260809/`. Distinct
  correlated candidates genuinely own distinct evidence states, so a future optimization must reduce transition
  work without conflating their identities.
- Observation: caching the most recent bridge-importance proposal by the exact raw bits of its target weight makes
  the ENGINEERING q9 hot loop slower. The control and candidate profiles contain comparable
  `LmdbFrontierPackedCostSession.appendFactor` planning samples (29,643 versus 30,088), while direct
  `BridgeImportanceSelector.selectRow` samples rise from 5,984 to 7,242; normalized allocation remains effectively
  unchanged at 127.60 MB/op. The extra cache branch/state traffic outweighs repeated division, so the candidate was
  reverted and the matching memoized-probe contract passes again in `logs/mvnf/20260809-202758-verify.log`.
  The failed profile is retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-repeated-weight-proposal-cpu-v2-20260809/`; its wall
  time is diagnostic only because WindowServer remained busy, but the within-profile hotspot increase independently
  rejects the intended mechanism.
- Observation: exact statement-probe memo replay was paying four independently checked `Result.quad(row, component)`
  accesses for every selected cached row. Borrowing the result's query-owned packed quad array for the duration of
  the replay loop preserves lifetime, order, and values while traversing one offset. Across two ENGINEERING q9 CPU
  replications, `emitCachedProbeRows` inclusive samples fall from 11,982 to 11,547 and 11,061 and complete planning
  samples from 29,643 to 29,023 and 28,756. Median planning falls from 64.557 to 63.136 and 61.864 ms/op; normalized
  allocation is slightly lower (127.602 to 127.408/127.407 MB/op). The Routine B pre/post selector passes in
  `logs/mvnf/20260809-202758-verify.log` and `logs/mvnf/20260809-203003-verify.log`; profiles are retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-borrowed-quads-cpu-v3-20260809/` and
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-borrowed-quads-cpu-v4-20260809/`. Because concurrent
  WindowServer work invalidates acceptance timing, only the repeated within-planning CPU reduction is used to retain
  this behavior-neutral layout improvement; the complete quiet-host matrix remains required.
- Observation: merely splitting equal-weight proposal preparation from a still-per-row selector does not improve q9:
  its 28,817 planning samples are indistinguishable from the 29,023/28,756 borrowed-array controls. That candidate
  was reverted and its profile remains under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-equal-weight-run-cpu-v5-20260809/`. Method-scoped
  HotSpot evidence in `profiles/lmdb-opt/final-campaign/performance/engineering-q9-select-row-jit-v2-20260809/`
  explains why: C2 already inlines the 232-byte selector into `emitCachedProbeRows`, while one representative
  compilation records about 87,197 selector invocations and only 8,192 threshold-loop backedges. The asymptotic
  waste is visiting nonselected proposal rows, not call overhead or repeated division alone.
- Observation: grouping a memoized equal-weight run by inverse CDF removes that asymptotic waste. The survey sums a
  repeated mass with `Math.fma`; the selection pass advances the fused CDF once per run, maps sorted thresholds to
  row ordinals, groups repeated draws for one row, and preserves full-support importance weights plus final residual
  mass. `initial-evidence.m8-batched-bridge-cdf.txt` preserves the red that proved four repeated proposal rows still
  used the scalar path. The focused green is `logs/mvnf/20260809-205439-verify.log`; all 29 finite-surface and all 108
  Frontier planning integrations pass in the `bridge-memo` and `bridge-cdf` workspaces. Against the borrowed-array
  q9 control, mean planning falls from 71.103 to 53.958 ms/op (-24.11 percent), median from 61.864 to 45.784
  (-25.99 percent), planning samples from 28,756 to 19,906 (-30.78 percent), and inclusive cached replay samples from
  11,061 to 3,741 (-66.18 percent). Allocation is intentionally flat because the transformation removes visits, not
  retained state. The retained profile is
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-batched-bridge-cdf-cpu-v6-20260809/`.
- Observation: after the retained prefix is full and every required context stratum has been resolved, the rest of an
  equal-weight exact-memo survey contributes only repeated mass; visiting each tail row cannot change strata,
  support, or selection state.
  Evidence: bulk tail observation reduces `observeWeight` exclusive samples from 1,088 to 16 and cached replay
  samples from 2,080 to 66. Against the batched-CDF control, q9 mean falls from 53.958 to 48.291 ms/op, median from
  45.784 to 41.317, and last-20 from 43.534 to 39.734 with flat allocation. The focused contract is green in
  `logs/mvnf/20260809-211454-verify.log`; the retained profile is
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-bulk-survey-tail-cpu-v7-20260809/`.
- Observation: successful supervision is necessary but not sufficient for a timing campaign. The v7 matrix completed
  all 117 cells, yet concentrated host work inflated MEDICAL q0/q2/q9 and HIGHLY_CONNECTED q2 while the targeted q9
  and q8 cells improved.
  Evidence: `profiles/lmdb-opt/final-campaign/performance/candidate-planning-cells-v7-batched-cdf-bulk-survey-20260809/`
  has 117 successful status files and 111 matched numeric pairs, but only 6.056x fixed-corpus and 3.396x geometric
  speedup; process evidence showed concurrent WindowServer, airportd, and location work. The quiet v5 6.808x/3.681x
  campaign remains the latest authoritative aggregate.
- Observation: deterministic scalar FILTER evaluation repeats identical direct-variable inputs across duplicate bag
  rows and equivalent physical/scheduled copies, but the earlier projected-binding experiment regressed because it
  rebuilt bindings even on every miss.
  Evidence: `initial-evidence.m8-filter-outcome-memo.txt` preserves the red. The retained memo keys sorted direct
  variable term IDs (including zero for unbound), evaluates the original full binding once per miss, and is disabled
  for effectful expressions and any EXISTS/subquery surface. The multi-variable/unbound contracts and the 31-test
  filter class plus all 108 integrations pass. Against the bulk-survey control, the replicated q9 profile improves
  mean 3.73 percent, median 6.62 percent, and last-20 9.81 percent while allocation changes by +0.04 percent; filter
  evaluation samples fall about 14.5 percent. Profiles are retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-filter-outcome-memo-cpu-v8-20260809/` and
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-filter-outcome-memo-cpu-v9-20260809/`.
- Observation: `exactCorrelationRelation` decoded the same query-local primitive term-ID domain into RDF values and
  rebuilt the same `FiniteRelationEstimate` before the finite-surface cache could recognize its stable value tuple.
  The canonical correlation-domain map already had the exact collision-safe identity and an unused
  `withExactRelation` transition.
  Evidence: `initial-evidence.m8-exact-correlation-relation-factor-memo.txt` preserves the red; the focused repeated
  factor contract is green in `logs/mvnf/20260809-221614-verify.log`, all 31 finite-surface tests pass in
  `logs/mvnf/20260809-221947-verify.log`, and all 108 Frontier integrations pass in
  `logs/mvnf/20260809-221714-verify.log`. Against the retained v9 control, independent q9 profiles reduce mean by
  3.11/4.73 percent, median by 3.12/5.09 percent, last-20 by 4.62/5.17 percent, allocation by 4.24 percent, and matched
  materialization/value-decoding samples from 3,200 to 2,146/2,163. Profiles are retained under
  `profiles/lmdb-opt/final-campaign/performance/engineering-q9-exact-correlation-relation-memo-cpu-v10-20260810/`
  and `profiles/lmdb-opt/final-campaign/performance/engineering-q9-exact-correlation-relation-memo-cpu-v11-20260810/`.
- Observation: state identity, raw calibration ancestry, and relation-value equality do not expose enough duplicate
  correlated-append work to justify another result cache. Extensional relation memoization found one equality hit;
  raw calibrated rebasing and an ID-native bounded surface changed the cost path but not its dominant scan work; and
  an arena-owned lineage transform index found no repeated derived transform on the q9 workload.
  Evidence: `initial-evidence.m8-extensional-correlation-relation-memo.txt`,
  `initial-evidence.m8-calibrated-append-rebase.txt`, `initial-evidence.m8-primitive-correlated-surface.txt`, and
  `initial-evidence.m8-lineage-reuse.txt` preserve the four red contracts. The final lineage implementation passed
  its focused contract, 30 arena tests, 31 payload-state tests, and the affected integration contract before the
  matched v15 profile measured mean +1.92 percent, median +0.21 percent, last-20 -0.12 percent, and flat allocation
  versus retained v11; `extendInner` samples moved from 8,092 to 8,152. The rejected profiles are retained under the
  corresponding `engineering-q9-*-v12` through `*-v15-20260810` directories, and source/tests were reverted with a
  clean marker search and `git diff --check`.
- Observation: the hottest remaining allocation site was not a Cascades object at all: every matched LMDB cursor row
  called LWJGL `MDBVal.mv_data()`, which materialized a new `DirectByteBuffer` solely to decode four bounded varints.
  Evidence: `initial-evidence.m8-native-cursor-decode.txt` preserves the missing-native-decoder red. The retained
  decoder reads the LMDB-owned address with explicit key-length bounds, covers encoded widths one through nine and a
  non-identity index map, and leaves ByteBuffer-based APIs intact for other callers. The finalized focused contract
  passes in `.mvnf/workspaces/m8-native-cursor/logs/20260810T004054.924396Z-38259-b6bcca91/verify.log`; all 108 Frontier
  integrations pass in `.mvnf/workspaces/m8-native-cursor/logs/20260810T004431.982372Z-38905-64c534f2/verify.log`.
  Against a preserved pre-change jar on the same idle host, the A/B/A q9 CPU profiles improve mean by 1.53 percent,
  p95 by 2.13 percent, and last-20 by 1.79 percent while reducing steady allocation by 16.49 percent. Matched allocation
  JFRs reduce total sampled allocations from 5,788 to 3,042, `DirectByteBuffer` samples from 2,741 to 24, and
  `LmdbRecordIterator`-owned samples from 2,723 to 13. Evidence is retained under the
  `engineering-q9-native-cursor-{alloc-v17,cpu-v18,ab-control-cpu-v19,ab-candidate2-cpu-v20}-20260810` profile
  directories.
- Observation: immutable mapped query-index segments retained their native base address but decoded every long and
  int through a fresh checked `ByteBuffer` access path.
  Evidence: `initial-evidence.m8-native-query-index.txt` preserves the matching pre/post 10-test contract. The
  retained address decoder keeps the existing segment bounds and endian conversion; q0 A/B/A improves steady mean
  14.04 percent and last-20 21.13 percent, while q9 improves 2.64 and 4.13 percent. The CPU profile under
  `engineering-q0-native-query-index-cpu-v24-20260810` no longer contains the mapped `ByteBuffer.get*` overhead.
- Observation: identical lineage-specific transform construction is not a repeated hot subproblem on the measured
  q0/q9 pair; indexing it adds lookup work without reducing allocation.
  Evidence: `initial-evidence.m8-derived-transform.txt` preserves the red/green contract and explicit rejection. The
  q0 candidate average regressed steady mean 0.18 percent and last-20 3.02 percent with flat allocation, so the
  implementation and test were reverted; the v25--v27 A/B/A artifacts remain as negative evidence.
- Observation: the safe trivial-alias rule is stronger than bag equivalence for dense planning: after substituting an
  assured source for an unconsumed alias, every original physical placement has a corresponding rewritten placement,
  and deferring the trivial extension processes no more rows. The rewritten exact lattice therefore covers the
  original opaque-factor lattice.
  Evidence: `initial-evidence.m8-alias-dominance.txt` records the alias-only two-lattice red. The proof edge makes that
  contract green at one lattice, leaves the nested independent-lattice contract at two, and all 76 packed search
  tests pass. A/B/A artifacts under `engineering-q{0,9}-alias-dominance-{candidate1,control,candidate2}-v28--v30-20260810`
  show q0 steady mean -6.34 percent and allocation -16.88 percent, with q9 mean +0.36 percent and allocation +0.003
  percent.
- Observation: sampled multi-statement EXISTS spent most of its exact-refinement time reopening RDF iterators even
  though every selector, join key, and result condition was already available as a primitive LMDB term ID.
  Evidence: `initial-evidence.m8-conjunctive-probe.txt` preserves the missing-kernel, nested-cursor, and consumed-alias
  reds plus the focused greens. Independent cursor lanes preserve a parent seek across recursive same-shape child
  seeks; transparent parser aliases are ignored only when no factor or outer correlation observes their target. TRAIN
  q8 A/B/A reduces steady mean 21.93 percent and allocation 38.83 percent, and the candidate CPU profile under
  `train-q8-conjunctive-candidate-cpu-v36-20260810` removes all 15,554 prior inclusive `JoinIterator.getNextElement`
  samples. The v10 campaign under `candidate-planning-cells-v10-conjunctive-probe-20260810` completes 117/117 cells
  and advances the two aggregates to 7.206x/3.865x without suppressing a candidate.
- Observation: canonical payload construction spent substantial CPU swapping complete term tuples even though the
  comparator is a primitive lexicographic order and the payload is immutable after sealing.
  Evidence: `initial-evidence.m8-payload-sort.txt` preserves matching pre/post direct canonicalization coverage and
  all 31 payload-state tests. SOCIAL_MEDIA q9 A/B/A improves steady mean 3.81 percent and last-20 4.66 percent; the
  candidate profile reduces `FrontierPayloadBlock.sortEntries` from 3,591 to 1,997 inclusive samples. The retained
  in-place binary MSD radix has bounded logarithmic recursion, uses insertion sort for small partitions, and adds no
  scratch allocation.
- Observation: `PrimitiveCorrelationKeyTable.relation` allocated final exact-sized tuple/mass arrays and the
  immutable relation constructor immediately cloned both arrays, even though the query-local builder could no longer
  mutate them.
  Evidence: `initial-evidence.m8-primitive-relation-ownership.txt` preserves matching pre/post integration evidence.
  SPARSE q10 A/B/A improves steady mean 2.63 percent and allocation 3.30 percent. Only the private builder uses the
  owned-array factory; the ordinary package constructor remains defensive. A separate occupied-slot scratch index is
  rejected and fully reverted in `initial-evidence.m8-sparse-correlation-table.txt` because endpoint median/last-20
  worsened and allocation rose 1.66 percent.
- Observation: exact leaf coalescing comparison-sorted complete tuples and then payload construction comparison-sorted
  the same tuples again, despite the accumulator's hash bucket array becoming dead once accumulation seals.
  Evidence: `initial-evidence.m8-exact-indirect-sort.txt` preserves matching pre/post integration evidence and the
  bracketed q10 measurements. Reusing the dead bucket array as compact row indexes lets an allocation-free indirect
  MSD radix establish the exact payload comparator order, after which each tuple is written once. Matched q10 mean
  improves 3.19 percent and p95 improves 13.07 percent with allocation within 0.06 percent of control.
- Observation: after ownership transfer, `PrimitiveCorrelationKeyTable.relation` remained the hottest exclusive q10
  Java leaf because tuple and mass rows were stored at half-empty hash slots and sealing branched across capacity.
  Evidence: `initial-evidence.m8-dense-correlation-table.txt` preserves matching focused greens, all 110 integration
  greens, bracketed A/B/A measurements, and CPU profiles. Encoded open-address buckets now point to insertion-dense
  rows while retaining hash-plus-tuple collision checks and the same load factor. Matched q10 mean improves 9.97
  percent, p95 11.37 percent, allocation 4.14 percent, and the prior 2,268 exclusive relation samples fall to zero.
- Observation: a complete fixed-corpus matrix can be numerically valid yet unsuitable for acceptance when unrelated
  sustained host work overlaps every independently supervised process.
  Evidence: `initial-evidence.m8-campaign-v11-contaminated.txt` records all 117 successful cells, the diagnostic
  6.761x/3.623x aggregates, a 2.86 percent corpus allocation reduction, and the immediate process snapshot. A separate
  LMDB Surefire JVM used 97.2 percent CPU for 38 minutes and began before the approximately 20-minute campaign; two
  storage-analysis processes concurrently used 80.6 and 58.0 percent CPU. The matrix is retained but not accepted.
- Observation: exact finite-surface prefixes were cached by a commutative factor-multiset key, but a different
  last-factor orientation rebuilt the same completed relation and consumed the shared scan budget again.
  Evidence: `initial-evidence.m8-completed-relation-reuse.txt` preserves both focused reds and their greens. The
  retained exact projection distinguishes prefix mass from matched-prefix mass, handles duplicate VALUES
  multiplicity, different variable-column orders, and unbound prefix cells as asymmetric extension wildcards, while
  leaving disconnected/unsupported transitions on the ordinary estimator path. The 54 direct surface contracts, 110
  Frontier planning integrations, and all 117 Theme result-bag
  comparisons pass. Performance retention remains conditional on an idle-host A/B/A measurement.
- Observation: a proof-carrying childless logical terminal can be structurally identical while belonging to more than
  one independently proved equivalent memo group.
  Evidence: `initial-evidence.m8-qerror-corpus.txt` preserves the q44 red and green. Global structural interning kept
  the first `EMPTY_SET` and silently deprived a commuted filter group of its own zero proof. Group-scoped terminal
  identity preserves append-only memo ownership while retaining collision-safe canonical identity for ordinary
  logical expressions; both commutation directions and the complete generated q-error corpus pass.
- Observation: the frozen plan-quality protocol specified process-isolated inventory and alternative execution but
  had no campaign-level completeness, resume, result-equality, or regret aggregation mechanism.
  Evidence: `initial-evidence.m8-plan-quality-campaign.txt` preserves three missing-runner reds and their greens. The
  new runner retains a result only when status plus Theme/query/index/stable-ID identity match, exposes every missing,
  failed, timed-out, stale, or fingerprint-mismatched alternative, and computes regret only from completed execution
  medians. The first real SOCIAL_MEDIA q4 inventory is exact-complete and resumes without repeated work.
- Observation: the frozen synthetic benchmark returned only a materialized expression, so JMH output alone could not
  distinguish exact completion from a resource/work/deadline-incomplete search.
  Evidence: `initial-evidence.m8-synthetic-campaign.txt` preserves the missing-runner reds and greens. The paired
  source-launched audit reads the current completion status and retained bytes when available, falls back to the
  legacy baseline work/deadline indicators without modifying that jar, and keeps audit and timing identities joined
  by factor and artifact hash.
- Observation: the fixed execution and planning protocols named result files but lacked campaign-level validation,
  censoring arithmetic, stale-resume rejection, or an executable acceptance decision.
  Evidence: `initial-evidence.m8-fixed-execution-campaign.txt` and `initial-evidence.m8-planning-summary.txt` retain
  eight red/green contracts plus the exact reproduction of the independently computed v10 aggregates. A 117-cell
  uncached-only directory is now visibly incomplete against the manifest's 234 required cells, and timeout samples
  remain bounds rather than invented measurements.
- Observation: rebuilding the frozen baseline source can reproduce behavior but not the original fat-jar SHA because
  ZIP entry timestamps are part of the artifact bytes.
  Evidence: the original build directory still contains the exact manifest-pinned LMDB jar with SHA-256
  `aa71ee49c2c04f39b4bc99d2bd1da989616f77b74970c42b11a3e6cde35ab9c2`; it and the exact query-evaluation jar were
  copied to immutable `/tmp` names. A successful reconstructed build is retained only as provenance diagnostics and
  is never substituted for the pinned artifact.
- Observation: selected Frontier evidence was detached correctly but then ignored at the final root-cardinality
  boundary, causing a second recursive scalar estimation pass and permitting scalar exact zero to overwrite richer
  positive state.
  Evidence: the MEDICAL_RECORDS q1 allocation profile attributes 15,369 of 279,611 samples (5.50 percent) to
  `LmdbPackedCostModel.certifyRootRows`; the focused red observes Frontier rows 1.0 becoming scalar rows 0.0. The
  invariant-based repair removes 5.30 percent normalized allocation in a same-store A/B/A bracket while leaving
  enumeration and final policy untouched.
- Observation: an exact direct distinct-cardinality probe encoded its variable requirement by cloning and annotating
  a statement, even though the caller already owned that proof and the downstream selector immediately decoded it.
  Evidence: the MEDICAL_RECORDS q1 profile attributes 150 of 4,355 allocation samples (3.44 percent) to
  `LmdbStatementPatternCardinalitySource.estimateDistinct`; an explicit-set path through the same exact selector
  removes 3.42 percent normalized allocation in a v73/v74/v73 same-store bracket. Matching characterization and all
  cursor-skip/Frontier integration suites preserve the selected plans and semantics.
- Observation: a sampled zero can be an observation from one proposal surface without proving that an exact bound
  continuation is empty, and publishing a speculative fallback derivation before normal access refinement can also
  occupy the exact transform identity needed by the real surface.
  Evidence: `initial-evidence.txt` records the transparent-alias red (`COMPOSABLE_PAYLOAD` expected,
  `OPAQUE_BOUNDARY` observed), the fork identity-collision red, the bounded-leaf redundant-expansion regression, and
  the combined three-test green. Moving exact recovery after surface refinement and requiring a database-exact input
  whose complete probe work fits the declared scan contract preserves the sampled measure and publishes no competing
  inexact cache entry.
- Observation: learning identity and learning workload features need related but distinct normalization rules.
  Evidence: the same uncorrelated inner join retained its logical key and applicability yet changed
  `boundInputCardinality` from 0 to 103.349 across equivalent enumeration routes. Normalizing its internal child
  prefix repairs that contract, while the neighboring semi/anti OOD test proves that a parameterized operator must
  still retain outer-domain cardinality as a physical workload feature. Both tests pass together.
- Observation: immutable audit retention and searchable continuation retention are different obligations.
  Evidence: v79 retained only three root Pareto alternatives but promoted 1,127,879 dominated intermediate
  derivations into the winner table, consuming 6.014 GB and producing billion-character traces. The focused red
  proves the second exact derivation is dominated while still audit-visible. After searchable admission follows the
  exact Pareto result, v80 completes the same cell with 49 winners, 140 work units, and 1.031 MB retained, without
  changing enumeration, costs, resource limits, or final policy.
- Observation: exact OPTIONAL mask canonicalization was asymptotically dominating PHARMA q11 even though its
  comparator and output contract were correct.
  Evidence: the v93 CPU profile records 6,290 samples in `sortMaskStrata` and 5,226 in `compareMaskStrata`, or 87.73
  percent of all 13,126 samples. The same 120-second inventory timed out under insertion sort; frozen v94 completes
  it in 14.424 seconds after replacing only the sorting algorithm with exact in-place heapsort.
- Observation: a full campaign can be structurally successful while some searches are correctly incomplete at a
  typed provider resource boundary.
  Evidence: all 117 v94 supervisor statuses say `succeeded`; 114 inventories say `EXACT_COMPLETE`, and the three
  512 MiB Frontier-limit cells say `INCOMPLETE_RESOURCE_LIMIT`. Their retained incumbents participate in the same
  root-minimum audit, and none of 190,107 event states reports scalar or whole-session fallback.
- Observation: plan-quality audit inclusion and multidimensional Pareto membership are independent labels.
  Evidence: SOCIAL_MEDIA q6's policy-selected exact root is not a final Pareto survivor, but it is the audit model's
  required selected-inclusion row and has the minimum comparison cost, 450.015 versus 482.064 and 18.8 billion. Its
  selected event remains `DATABASE_EXACT`, `COMPOSABLE_PAYLOAD`, and `frontier_authoritative`; changing policy from
  the diagnostic label would therefore be an unsupported heuristic rather than a repair.
- Observation: candidate identity can remain stable while parser-local anonymous binding names make a physical plan
  fingerprint unstable across processes.
  Evidence: v94 MEDICAL_RECORDS q2 kept all 13 candidate-fingerprint prefixes but changed every physical suffix. The
  focused reparse red reproduced all five suffix changes in one JVM. The lossless packed v95 identity and its
  alpha-topology/visible-name tests are green; all 13 independent q2 executions authenticate their inventory IDs.
- Observation: realizing an evidence descriptor is a typed state transition, not an integer substitution.
  Evidence: the replay red registered canonical state 2 first as `BOUND_ONLY` and later as
  `DATABASE_EXACT/COMPOSABLE_PAYLOAD`. Atomic typed realization and the explicit negative ID-only contract pass with
  the combined 157-test search/cache/recipe/session matrix.
- Observation: charging a complete mapped-metadata envelope to every manifest makes an atomic delta publication fail
  even when almost every descriptor names the same immutable base shard.
  Evidence: `FrontierStatisticsBuilderTest.chainedOmniMutationLayersResolveTransitionsAgainstTheBaseSet` published a
  base plus two bounded mutation layers under a 64 MiB test governor; the second candidate was refused while the
  preceding generation still owned duplicate accounting. Splitting generation-local directory bytes from
  reference-counted shard-handle bytes makes the focused reproduction and the combined 68-test
  builder/service/session cluster pass without increasing the governor.
- Observation: RDF-term identity is insufficient for costing a SPARQL typed equality, but enumerating the value
  dictionary would recreate store-size-dependent planning.
  Evidence: the focused `LocalTime` regression stored the Java factory form and queried the value-equal lexical form
  `08:00:00`; exact term probing estimated five rows instead of the actual two. A bounded set of candidates admitted
  only by `QueryEvaluationUtility` recovers two positive mapped rows, while the full five-test temporal class and
  TRAIN q5 integration pass without statement enumeration or an exact-zero claim.
- Observation: a historical join-order assertion can become false after the physical anti-join implementation it
  protected is replaced.
  Evidence: highly-connected q10 originally forbade a finite weight anchor because streaming correlation reopened
  the multi-pattern RHS per candidate row. Both a 512-work budget and a complete 1,400-candidate/10-second search now
  choose the four-value anchor, while telemetry shows `materialized-hash`, four mapped `[P,O]` prefix probes, and one
  bounded RHS build. The strengthened regression checks those physical costs and retains anti-before-weight only for
  the no-anchor case.
- Observation: leaf repeated-equality masks and join variable classes describe different domains after a finite
  binding fixes a self-loop variable.
  Evidence: Social Q7 bound both components of `?v social:follows ?v`; the leaf retained subject-object equality,
  while the join program correctly omitted bound components from its variable classes. Comparing the full masks
  rejected the program. The focused constructor regression and original Social Q7 integration now pass after the
  invariant compares only repeated pairs whose two components remain unbound.
- Observation: narrow test selections hid 33 regressions; unified phase 1 had never run the whole core module.
  Evidence: `evidence/qualify-01-core-module/` (7,671 tests, 210 red) against
  `evidence/counterfactual-head-core-4/` under `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/`. Committed
  HEAD `4ab0c09438` does not compile for `core/sail/lmdb`, so LMDB has no HEAD counterfactual.
- Observation: LMDB's default planner mode is `auto` with a 512-unit work limit, and provider callbacks were a
  median 95 percent of the units a search consumed under the unified currency (5–122× inflation, median 19×).
  Evidence: `measure-auto/per-query.csv`; exact-mode method breakdown on MEDICAL, SOCIAL and PHARMA:
  `realizeEvidenceState(int)` 46 percent, `objectiveScore` 17 percent, costing callbacks about 16 percent.
- Observation: a plan-decision-cache family was modeled at about 3.25 times its real heap size, the default cache
  held about seven theme plans, and once full it refused every admission forever.
  Evidence: `spec-decision-cache-replay-and-capacity.md` section 2 and `spec-shrink-cached-plan-entries.md`; the
  admission rule dates from 89e3e9c0e7 (2026-10-23), before this phase.
- Observation: foreground filter sampling during planning has a 2 ms wall-clock limit; a timed-out sample changes
  the planner's input without changing any revision.
  Evidence: `review-lmdb-cache.md` finding C7 and the round-2 fix (`LmdbPlanningWallClockCuts`).
- Observation: `PackedDependentSubqueryCosting.contextualCost` falls back to the context-free subquery winner when
  no contextual winner exists for a non-zero input context, so a bound subquery can be published at a context-free
  price; it also happens without any refusal when the nested search is cut by its search budget.
  Evidence: fourth review of the budget change (7.0 against 1,006.0 in the core fixture). Measured afterwards
  (`spec-contextual-subquery-price.md`, data under `measure-auto/pdsc/`): on the LMDB theme corpus the
  `contextualCost` fallback never runs (LMDB's semi/anti providers cost the subquery themselves); the sibling
  fallbacks in `selectedWinner` and `recordScalarPlans` occur in about ten queries and are almost all structural (the
  subquery is not safe to cost with outer bindings, so no contextual search can exist). Returning unknown for every
  missing contextual winner changed no theme plan but broke three correlated core tests ("correlated DPhyp target was
  unreachable") and unpriced the deadline-expired canonical candidate, so that fix was not adopted.
- Observation: `PackedSelectedPlanContextualizer` ignores its cost model, so
  `selectedContextualizerRejectsComponentOnlyOperatorRefinement` cannot fail for the reason in its name.
- Observation: exact mode stops at the 64 MiB retained-state limit for 21 theme queries (12 in SPARSE); six theme
  queries cannot be planned by Cascades because the codec rejects an operator; the explain path throws "slot layout
  is already frozen" on PHARMA q9; `LmdbThemeQueryRegressionIT` has been `@Disabled` as a class since 3b019db10c;
  `testsuites/benchmark` holds about 2,900 generated `dependency-reduced-pom-*.xml` files from mvnf quick installs.
- Observation: the LEO revision behaves as a clock, not as an evidence version. With the store's default
  configuration a completed evaluation advances it in about two of three cases, for the same query in every one of
  seven repetitions, and it is global, so one query's execution invalidates every other query's replay-only cache
  entry. All plan-only cache hit rates recorded in this plan overstate what an executing workload gets.
  Evidence: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/measure-final/summary.txt`.
- Observation: the benchmark configuration (native evaluation on) publishes runtime feedback for 10 of 182
  evaluations; the store default (native evaluation off) for 119 of 182. Benchmarks therefore do not exercise the
  feedback-driven invalidation that a default store sees.
- Observation: the request's cache key reads the Frontier read-plane mask more than once per request, so the gate,
  the key and the planning can disagree; this is the cause of the intermittent warm-hit test
  (`LmdbDetachedPlanningOwnershipTest`, 3 of 10 red before and after the cache rounds).
- Observation (2026-11-10): physical residual learning and lifecycle verification are wired only for filters and
  EXISTS. A physical residual is written, and a lifecycle entry verified, only when the executed iterator reports
  an implementation id equal to the planned one; only `FilterIterator` (which echoes the planned id) and
  `MaterializedExistsFilterIteration` report one. Statement-pattern leaves, joins, hash joins and paths never do,
  so their physical cells are leased and read (a median of 258 reads per planning) but never written: 0 of about
  1.58 million physical decisions in the logical population returned an estimate. The rejection rule is the
  design; the missing reporters are incomplete wiring (the learned-feedback execplan asks for them). Not fixed;
  owner decision pending. Evidence: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/rca-physical-and-keys/NOTES.md`.
- Observation (2026-11-10): parser-generated variable names (`_anon_having_…`, `_anon_path_…`, random per parse)
  are embedded raw in the physical residual key's layout hash and in the event key's source and frames, so 33 of
  182 theme queries never reuse their physical or event cells and the shared 4,096-key map fills by round 4.
  Fix specified in `.agent/designs/GH-0000-stable-physical-learning-keys-2026-11-10.md`. Same evidence.
- Observation (2026-11-10): every model decision scans a whole key map for a boolean that only selects a gate
  label (a median of 1.64 million entries per planning, about 14 % of planning time under load). Fix specified in
  `.agent/designs/GH-0000-flm-applicability-index-2026-11-10.md`.

## Decision Log

- Decision: the semantic projection boundary stays first in `compilePhysical`, and the tests that pin native
  execution of shapes it currently hands to the ordinary evaluator are not re-targeted. Native admission under
  the boundary is extended instead (through the sealed projection), slice by slice in order of payoff. Basis:
  the census of 2026-11-17 (213 cases red only because of the boundary; 356 green only because of it, at least
  18 with wrong rows without it), the measured 4.6–58× slowdown of property-path queries, and the
  "feature-admission red" tests committed with the boundary. Date/Author: 2026-11-17 / owner (Håvard), recorded
  by Claude.
- Decision: accept that making learning keys independent of parser-generated names re-keys every `FILTER EXISTS`
  relation's logical key once, although the query contains no generated name. The old key followed the hash order
  of `Filter.getBindingNames()`; keeping those bytes would mean keeping a key that changes with any generated name
  in the sub-query. Learned cells under the old keys are orphaned and age out; no plan or result depends on them.
  Date/Author: 2026-11-11 / Claude.
- Decision: the planning revision keeps excluding lifecycle bookkeeping that a planning causes itself (admission,
  `planned`, leases); the replay of a read log captures a separate lifecycle version before and after instead.
  Including it in the planning revision would move the revision in every planning. Known limitation recorded:
  with lifecycle enforcement on, exact-revision caches can serve a decision whose capacity-protected answer a
  lease release has since changed (reviewer scratch test
  `aLeaseReleaseFlipsAnEnforcedCapacityDecisionWithoutMovingAnyVersion`). Enforcement is off by default.
  Date/Author: 2026-11-11 / Claude.
- Decision: a read whose value feeds a test that switches between two estimates (the mapped-bounds intersection)
  is validated by the test's result, recomputed at replay, not by the tolerance; with lifecycle enforcement on
  the tolerance is zero. Date/Author: 2026-11-11 / Claude.
- Decision: retain the candidate and prefix as per-invocation quantities, give emitted rows their actual factor,
  component or context domain, and give physical work its total-invocation domain. Reject scope-incompatible quote
  construction and do not convert a candidate into a visit or seek bound. Publish the candidate from the same packed
  quote used for physical projection; do not create an observer quote or issue an unowned source-root provider.
  Date/Author: 2026-11-08 / Håvard and Codex.

- Decision: check cancellation and validate finite, nonnegative, ordered quantity values before consulting lineage;
  check cancellation again after that getter. Preserve exact provider and cleanup failures for valid input, and keep
  the numerical equality rule for valid signed-zero UNKNOWN declarations. Date/Author: 2026-11-08 / Håvard and Codex.

- Decision: use the arena's numerical endpoint equality in the request's UNKNOWN degenerate-interval guard. This
  treats positive and negative zero as equal without changing the arena or adding an independent budget. Existing
  finite and ordered input validation stays in place. Date/Author: 2026-11-08 / Håvard and Codex.

- Decision: a request has at most one owner-authenticated activation; closing it revokes its borrowed values and
  cannot be done during an operation. Check cancellation before normal outcomes, and snapshot and validate each
  admitted provider quantity before resetting the shared builder. Keep all work and scratch-memory admission on the
  existing planner owner, with no session-close ownership transfer or extra quota. This is an inactive contract and
  awaits independent review and broad qualification. Date/Author: 2026-11-08 / Håvard and Codex.

- Decision: pair the planner work and memory bindings to one session before issuing an immutable generation handle;
  authenticate the query and memo arena, retain proof occurrence identities, and reissue the handle only for a new
  memo generation. Treat only explicit structural/evidence mismatches as checkpoint rejection; guard provider
  exceptions by their origin so even the same exception type is propagated unchanged.
  Rationale: shared budget identity, equal numeric vector IDs, or equal boundary sets alone do not establish session
  or covered-region identity, and arbitrary provider errors must never authorize fallback.
  Date/Author: 2026-11-08 / Håvard and Codex.

- Decision: integrate the upstream native selector and retire the old adaptive/machine/store models and native
  arbiter instead of restoring that hierarchy. Keep upstream unbounded upper-axis arithmetic and missing-work
  evidence, and carry the branch's reusable-filter ownership, terminal cancellation, failed-open cleanup,
  selected-result lifetime, and worker handoff contracts through the new dispatch. Obsolete probe/hedge-specific
  tests retire with those removed features; the general lifecycle invariants have active replacement coverage.
  Keep serializable observation ordering and native group-pushdown as independently named pipeline settings.
  Rationale: the upstream execution and quote contracts supersede the old native-local owners, while the branch's
  ownership and transaction guarantees remain necessary across retries and nested/parallel execution.
  Date/Author: 2026-11-07 / Håvard and Codex.

- Decision: keep the packed physical provider, shared objective/budget, selected executable descriptor and binder,
  consumer/cache identity, and executed-recipe feedback attribution as explicit unfinished roadmap work. Existing
  native-local selector intervals or advisory learning do not certify packed feasibility or cross-family dominance.
  Rationale: native reranking still occurs after packed selection, and the two systems do not yet share one
  comparable work or executable-recipe contract.
  Date/Author: 2026-11-07 / Codex.

- Decision: execute the entire unified costing plan using the existing roadmap, with initial build, test, and
  measurement evidence retained separately and no implementation commit or push. The later user-authorized upstream
  merge is a separate merge commit and does not stage this plan or the local implementation. The earlier planning-only
  restriction was specific to the earlier request and is superseded by the user's implementation request.
  Rationale: the current request expressly authorizes implementation and validation; maintaining one living plan avoids conflicting progress and preserves the existing delivery boundaries.
  Date/Author: 2026-11-06 / Codex.

- Decision: while a separate user-owned benchmark holds JMH's global lock, prepare the narrow test-first quantity-correctness correction and defer builds, verification, and measurements. Preserve the pre-change benchmark jar and do not treat this unvalidated change as the phase-0 performance candidate or as a completed integration phase.
  Rationale: the retained in-repository failure proves the existing cost contract already loses unknown information, and the correction can be developed without disturbing the other benchmark. Complete baseline controls and freeze their per-lane tolerances before evaluating the later unified optimizer candidate; the original baseline remains invalid where it mixed fallback paths.
  Date/Author: 2026-11-06 / Codex.

- Decision: extend this sole roadmap with the 2026-11-06 phased implementation plan; do not create another ExecPlan.
  Rationale: the repository's optimizer-plan inventory explicitly requires one governing roadmap. Preserve historical
  evidence while making the current scope, source baseline, dependencies, and unexecuted validation steps explicit.
  Date/Author: 2026-11-06 / Codex.
- Decision: let Cascades select executable physical recipes using shared typed work estimates; let native and
  storage providers recognize, quote, and bind their implementations. Install budgeting with the contracts, then
  prove one complete access-to-native integration before expanding strategy coverage.
  Rationale: improving scalar cardinalities cannot account for lost factorization, metadata aggregates, ordering,
  compilation, or index access. Adding every strategy before controlling recognition and estimation work risks
  moving execution latency into optimization. Existing packed arenas and resource contracts provide the foundation.
  Date/Author: 2026-11-06 / Codex.
- Decision: use structural proofs for semantic rewrites and sound pruning, estimates for ranking, and explicit
  incomplete status for resource-limited search. Cost-model confidence does not establish semantic or hard work bounds.
  Rationale: a fast bounded planner is useful; an unjustified exactness claim or sampled empty-result rewrite is not.
  Date/Author: 2026-11-06 / Codex.

- Decision: report cached lookup, open/close, and cold first-touch as separate shard-reader performance lanes and
  leave cold-p95 qualification open despite the large steady-state win.
  Rationale: one-time checksum validation and registry publication are intentionally paid on first block access, so
  a hot-read aggregate cannot establish cold latency. The implementation is retained for its mapping-lifetime
  correctness and repeatable 4.4x to 14.9x representative cached-read gains, but the 43 to 46 percent isolated
  first-touch regression remains explicit evidence for the later cold-NVMe gate.
  Date/Author: 2026-08-20 / Håvard and Codex.
- Decision: preserve `mappedDataBlockCount` as a logical first-touch count and test native mapping topology through
  the JDK mapped-buffer management bean rather than changing the public shard API or relying on Linux `/proc`.
  Rationale: adaptive promotion deliberately maps eight logical blocks through four dedicated mappings and one
  shared contiguous mapping. The supplied Linux verifier observes five file mappings, but RDF4J tests must remain
  portable to macOS and Windows; `BufferPoolMXBean` exposes the same process-local mapping delta without a
  platform-specific filesystem. Failed-checksum and metadata tests compare deltas around one isolated shard so
  unrelated pre-existing mappings do not affect the assertion.
  Date/Author: 2026-08-20 / Håvard and Codex.
- Decision: Use the overflow-safe arithmetic mean for every complete projected-distinct design-lane ensemble, and
  keep its point estimate separate from the structural upper bound used for bounded-cache decisions.
  Rationale: each lane is an equal-weight independent inverse-probability replica. Zero is valid sampled evidence and
  the high replica carries rare-key mass, so median, trimming, Winsorization, Huber/Catoni weighting, or a zero-only
  branch changes the estimator target. Physical memoization is irreversible at runtime and must therefore use an
  upper bound derived from row intervals, relevant leaf bounds, and exact finite assignments rather than the random
  point. Existing robust join-lane medians are outside this decision.
  Date/Author: 2026-08-18 / Håvard and Codex.
- Decision: Keep the saturated `PrimitiveDistinctBindingTracker` performance defect separate from the
  projected-distinct estimator and structural-bound repair.
  Rationale: the corrected evidence now selects the safe materialized algorithm and bounds LMDB scanning as intended;
  changing the point estimator, weakening the cache upper, or enlarging the memo would hide the independent runtime
  telemetry complexity. A follow-up behavior-neutral or separately test-first runtime slice should make the tracker
  saturate in O(1), resize under an explicit memory contract, or disable distinct telemetry once exact counting is no
  longer representable, while preserving its reported semantics.
  Date/Author: 2026-08-18 / Håvard and Codex.
- Decision: Repair runtime observation with a bounded hybrid exact/HLL collector and execution-local primitive
  batching, while retaining approximate distinctness only as explicitly qualified learning evidence.
  Rationale: exact open addressing is valuable for the existing two-key and five-key domains, but an exact bounded
  table cannot both retain arbitrary new keys and guarantee constant update cost after its memory budget is reached.
  Three p=12 byte-register HLLs keep total/matched/unmatched updates allocation-free and constant-time after a
  75-percent exact-prefix transition. Approximate points may refine expected work, but structural uppers remain the
  only authority for memo-cache admission. Counts and lifecycle state remain exact; only high-frequency call timing
  is deterministically sampled after an exact 32-call prefix.
  Date/Author: 2026-08-18 / Håvard and Codex.
- Decision: Treat generated value-equal RDF terms as bounded lower-support probes, never as a complete semantic
  rewrite domain.
  Rationale: SPARQL numeric and calendar equality can cross lexical forms and datatypes, but no finite generator can
  prove that it has enumerated every stored equivalent. At most 16 deterministic candidates per value are validated
  by the runtime equality evaluator and the combined domain is rejected above 1,024 rows. Positive mapped support may
  guide costing; a miss retains `[0,1]`, confidence at most 0.5, and a non-exact guarantee. This preserves bounded
  planning and avoids both false zeroes and dictionary scans.
  Date/Author: 2026-08-16 / Codex.
- Decision: Specify anti-join regressions in terms of bounded physical work rather than one permanent textual order.
  Rationale: anti-before-fanout was necessary for streaming correlation, but a materialized hash RHS is built once
  and can make four mapped finite-domain probes cheaper than tens of thousands of subject-bound lookups. Exhaustive
  memo search confirms the selection is cost-driven. Tests therefore require mapped finite-domain probe accounting
  and materialized or memoized anti work when the anchor wins; without an anchor they retain the selective
  anti-before-weight order.
  Date/Author: 2026-08-16 / Codex.
- Decision: Keep equality constraints on bound repeated components in the leaf and exclude them from join-variable
  classes.
  Rationale: leaf verification owns constant and repeated-component semantics, including contradictory bindings;
  join variable classes model only unbound equivalence classes used across patterns. Requiring a bound self-loop to
  appear in both domains is redundant and violates the constructor's prohibition on labeling bound components.
  Date/Author: 2026-08-16 / Codex.
- Decision: Complete mapped logical refinement before deriving physical independent-hash candidate work.
  Rationale: physical candidate multiplicity depends on the final logical match estimate and the proved relation
  between lookup and compatibility masks. Reusing a pre-refinement Cartesian product both misprices valid plans and
  breaks scale independence; replacing it when masks differ would instead undercount residual compatibility checks.
  Date/Author: 2026-08-16 / Håvard and Codex.
- Decision: Share immutable shard handles across adjacent manifests and account the handle independently from each
  generation directory.
  Rationale: atomic publication and rollback must keep old readers valid, but reused shard mappings are one physical
  resource. Reopening and double-charging them wastes file descriptors and can reject a bounded delta under the hard
  heap contract; releasing the old generation before candidate validation would sacrifice rollback safety.
  Date/Author: 2026-08-15 / Håvard and Codex.
- Decision: Frontier evidence state is the canonical estimation currency; scalar rows are adapters.
  Rationale: later transitions, uncertainty, learning, cache validation, and Pareto dominance need information that
  a scalar cannot reconstruct.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Replace subset-level scalar winners with exact continuation-equivalence classes and Pareto frontiers.
  Rationale: future costing observes more than a subset, so scalar winner replacement violates Bellman's principle.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Treat DPhyp as the canonical topology enumerator inside Cascades, not as an independent winner owner or
  singleton-extension generator.
  Rationale: one memo must see every legal bushy alternative and select it under the same state and cost contract.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Preserve a multidimensional cost vector through Pareto retention and apply scalar policy only at final
  selection or traversal priority.
  Rationale: memory, startup, rescans, ordering, uncertainty, and steady-state work are not safely interchangeable
  under one permanent weighted sum.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Improve on LEO through commensurate observations, logical/physical separation, exact applicability,
  uncertainty, validation, LastGood, quarantine, and rollback.
  Rationale: an unscoped multiplicative residual can oscillate between alternatives and learn deterministic reopen
  cost only after paying for it.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Proven predicate ranges may authorize rewrites; estimates and learned beliefs may only price them.
  Rationale: semantic correctness cannot depend on statistical confidence.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Maintain exactly one optimizer ExecPlan: this file.
  Rationale: separate component, rollout, research, benchmark, and closeout plans duplicated progress and allowed
  local compromises to look like competing architecture. One self-contained plan makes dependency order and current
  truth unambiguous.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Fold unresolved work into this plan and use Git history plus retained audit artifacts for completed detail.
  Rationale: copying thousands of lines of completed transcripts would obscure the executable future work, while
  dropping unresolved obligations would make consolidation destructive.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Keep the existing packed ID/structure-of-arrays substrate and evolve its winner storage in place.
  Rationale: the packed cutover already delivered complete codec coverage and large allocation reductions; the defect
  is information loss and one-winner semantics, not the use of primitive arenas.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Treat current learned-feedback work as an implemented baseline held in Monitoring, not as a finished
  substitute for LEO-plus Pareto integration.
  Rationale: commensurate observations and lifecycle containment prevent known oscillations, but learned uncertainty
  must still participate symmetrically in continuation/Pareto comparison and pass held-out corpus gates.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Make the rewrite catalog code-linked and machine-checkable, with generated human documentation.
  Rationale: a prose-only catalog cannot detect a newly registered rule, missing safety precondition, or stale test
  link; a code-only registry cannot document proposed non-rules and literature proofs for humans.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Certify join-search completeness over hypergraph construction, predicate decomposition, and safe rewrite
  closure as well as DPhyp pair enumeration.
  Rationale: CD-E is complete for non-decomposable predicates in the 2025 Birler/Neumann model but misses legal plans
  with decomposable or equivalence-rewritten predicates; an exhaustive callback cannot recover an omitted edge.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Add an IU-like binding-flow/path index to the compact working algebra while keeping it distinct from the
  Cascades memo and selected execution plan.
  Rationale: producer/consumer paths support binding, range, equality, pushdown, and barrier analysis without
  quadratic per-node binding sets; memo equivalence and execution state have different identity and lifetime rules.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Treat compilation mode, pipeline/breaker state, parallel setup, page residency, first-result latency, and
  value lifetime as physical properties or cost dimensions when a backend exposes them.
  Rationale: Umbra and GroupJoin show that the same logical expression changes cost with startup, materialization,
  memory, cache, execution mode, and selectivity; one permanent row-count-weighted scalar cannot preserve the tradeoff.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Learn scoped residuals around conventional current evidence and require observation qualification,
  learned-cardinality invariants, drift/OOD gates, and conservative fallback.
  Rationale: LEO's separate-adjustment design remains sound, while censored counts, stale adjustments, workload drift,
  and learned invariant violations make unconstrained replacement models unsafe.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Catalog GroupJoin/preaggregation and mark/semi/anti implementations as guarded logical and physical
  alternatives rather than universal rewrites or heuristics.
  Rationale: functional dependencies, bag multiplicity, unbound/error semantics, aggregate decomposition, key shape,
  memory, and side selectivity determine both legality and cost; SQL `IN`/`EXISTS` folklore is not a SPARQL proof.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Represent produced binding occurrences with SSA-like value IDs and explicit branch/compatibility merges.
  Rationale: a SPARQL variable name may have different producers across UNION, OPTIONAL, JOIN, aliases, and subquery
  scope; treating the name as one source can leak assured or range facts across an unsafe path.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Complete the proof-coupled semantic/range registry before join enumeration, then run rewrite,
  fact propagation, hypergraph construction, DPhyp, candidate installation, and parent reactivation to a fixpoint.
  Rationale: a DPhyp pair set can be complete only for the hypergraph it sees, and a later equality or predicate
  decomposition can expose a legal edge and plan that an earlier graph omitted.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Couple every executable catalog descriptor to the `RuleApplicabilityResult` consumed by memo installation.
  Rationale: registration drift checks alone cannot prove that runtime code enforced the descriptor's bag, binding,
  error, scope, or proof preconditions.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Preserve the one-word DPhyp fast path and add an exact interned multiword node-set kernel for 65+ factors.
  Rationale: the current raw-`long` receiver and 64-node hypergraph cannot satisfy the promised 64/65 parity; an
  integer-ID word arena supplies the same semantics without per-set object graphs.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Define exact search by certified completion under deterministic work, deadline, and retained-byte limits;
  resource exhaustion returns an explicit incomplete result rather than pruning or pretending exactness.
  Rationale: continuation and Pareto width can be exponential, so liveness requires allocation admission while
  correctness requires preserving the distinction between a complete frontier and an executable incumbent.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Use one registry-driven cost algebra and robust interval dominance before implementing Pareto retention.
  Rationale: work, latency, repeated opens, peak memory, concurrency, and uncertainty have different composition
  operators; field names alone permit inconsistent frontiers across operators and implementers.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Keep the current hierarchical NIG learner and strengthen it with exact applicability, calibrated
  conformal OOD fallback, interval-only censoring, invariants, asymmetric hysteresis, LastGood, and quarantine.
  Rationale: the existing model already separates logical absolute values from physical residuals; the missing safety
  is trustworthy reuse and lifecycle control, not an unconstrained replacement model.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Keep interpreted primitive execution as the mandatory cold/fallback path; admit vectorized or compiled
  physical alternatives only with workload-specific cold, warm, allocation, cache, and failure evidence.
  Rationale: Umbra motivates execution-mode alternatives, but compilation latency and code/cache lifetime can exceed
  any per-tuple gain for one-shot RDF queries.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Keep runtime rule descriptors and the wider optimizer surface inventory as separate immutable projections
  of one closed-world Java catalog.
  Rationale: runtime proof drift must fail independently from packed opcode, scalar-boundary, cost, cache, feedback,
  and oracle drift; neither a prose list nor runtime reflection alone supplies reviewed classification.
  Date/Author: 2026-08-06 / Håvard and Codex.
- Decision: Retain learned plan selection in Monitoring after the additive Milestone 6 replay.
  Rationale: the combined NIG/applicability/OOD policy passes every evaluated synthetic safety gate, but this replay
  cannot establish the required no-more-than-five-percent full-corpus planning-time/allocation overhead. Promotion
  remains contingent on the matched Milestone 8 campaign; the acceptance gate is not weakened or inferred.
  Date/Author: 2026-08-07 / Håvard and Codex.
- Decision: Treat a stale-cache certificate as a new immutable generation, never as scalar telemetry patched onto an
  old recipe.
  Rationale: current candidate totals alone cannot reconstruct multidimensional Pareto dominance, continuation
  identity, Frontier state, or learned applicability. Certification must replay aligned events and produce a new
  detached certificate; missing replay state fails closed to one fresh plan.
  Date/Author: 2026-08-07 / Håvard and Codex.
- Decision: Let each subset kernel own exactly one complete-incumbent seed when it can prove that seed fits.
  Rationale: the dense exact arena's context-costed seed is stronger than the standalone static-order campaign and
  retains the same immutable events for continuation search. Replaying both is exact duplicate work, while sparse,
  wide, scalar, and insufficient-work paths still require the standalone fallback before partial exploration.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Write exact bijective projections directly into their counted payload strata.
  Rationale: a permutation that contains every input slot exactly once preserves tuple identity, so a second exact
  coalescer can neither change multiplicity nor discover duplicates; non-bijective, partial, repeated, sampled, and
  residual mappings retain the general coalescing path.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Stage growable bridge emissions in linked geometric segments rather than replacement arrays.
  Rationale: each segment is reserved before allocation, append order and maximum capacity remain exact, `clear()`
  reuses retained storage, and sequential reads cache their segment. This removes whole-buffer copies while keeping
  every byte and reservation query-scoped and explicitly released.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Route repeatable variable-to-variable Extension chains over primitive Frontier term IDs and reuse
  synchronous expansion/probe adapters instead of capturing callbacks.
  Rationale: alias evaluation preserves the source term identity, sequential assignment, and unbound state exactly;
  decoding values cannot add semantics. Query-session adapters remove allocation without retaining payloads or
  emitters beyond the synchronous call, and exact full-key checks continue to own all cache semantics.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Seal conjunctive EXISTS/NOT EXISTS correlation domains only from resident database-exact Frontier states,
  and otherwise share lazy exact probe outcomes by contextual logical group plus correlation surface.
  Rationale: a complete exact domain safely answers absent and partially bound keys, while a sampled RHS cannot prove
  absence. Physical streaming/memoized/materialized framings are equivalent members of one logical group, so their
  exact boolean probe outcomes are reusable; including the sorted correlation surface prevents incompatible key
  layouts from aliasing. This removes duplicate evaluator work without suppressing a candidate or weakening an
  evidence guarantee.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Answer eligible sampled single-statement EXISTS probes from the pinned primitive snapshot, not from the
  sampled synopsis or decoded evaluator.
  Rationale: the synopsis may not prove global absence, but an exact bound selector on the same immutable LMDB
  snapshot does. Reusing one best-index cursor per selector shape and stopping at the first legal row preserves
  EXISTS/NOT EXISTS, named-context, and repeated-variable semantics while eliminating value decoding and per-key
  iterator opens. Non-default datasets, inferred statements, and unsupported RHS algebra retain the evaluator path.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Replay contiguous equal-weight proposal rows as one fused inverse-CDF batch.
  Rationale: sorted with-replacement thresholds define selected row groups directly; walking every nonselected row is
  linear in exact expansion size rather than particle budget. A fused run CDF is more accurate than accumulated
  addition, preserves deterministic seed/full-support/final-mass semantics, and reduces selection work to retained
  draws without suppressing a candidate or changing source observations.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Bulk-observe an equal-weight survey tail only after the retained prefix is full and every context stratum
  has been resolved.
  Rationale: after those exact stopping conditions, each remaining row changes only total repeated mass; preserving
  the survey sum while avoiding per-row callbacks leaves selection, support, strata, and candidate enumeration
  unchanged.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Memoize repeatable scalar FILTER outcomes by contextual logical group and exact direct-variable term IDs.
  Rationale: equivalent physical copies evaluate the same deterministic predicate over the same direct inputs. The
  memo excludes nested correlation/effects, represents unbound explicitly, evaluates the original full binding on a
  miss, and caches both true and expression-error false outcomes without suppressing any candidate.
  Date/Author: 2026-08-09 / Håvard and Codex.
- Decision: Store a materialized exact correlation relation on its existing canonical domain entry.
  Rationale: `(stateId, sorted binding names)` already defines the immutable query-local primitive domain. Replacing
  that map value with `withExactRelation` is collision-safe, avoids a parallel cache and identity, preserves decline
  behavior, and lets every stale profile reference discover the canonical decoded relation.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Decode immutable Frontier query-index primitives from the segment's retained native address.
  Rationale: the segment already owns the mapped lifetime and exact bounds. Native endian-aware reads avoid repeated
  `ByteBuffer` wrapper checks while preserving the on-disk format and keeping bounded heap-buffer APIs for callers
  that do not own a native segment.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Let a proof-backed alias-commuted filter lattice exactly cover its opaque original lattice.
  Rationale: assured source binding, absent/unused alias, deterministic scalar relocation, and the trivial-bind proof
  establish both bag equivalence and a no-greater-work candidate mapping. The scheduler keeps the original prepared
  as fallback until the covering lattice completes, follows cover dependencies topologically, and never infers cover
  from estimates, selectivity, or a budget heuristic.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Evaluate eligible pure conjunctive EXISTS/NOT EXISTS refinement as one primitive pinned-snapshot join.
  Rationale: constants, outer correlations, internal join variables, repeated-variable equalities, graph scope, and
  first-match termination are exactly decidable over LMDB term IDs. One cursor lane per statement depth prevents
  recursive seeks from moving parent cursors. Non-statement operators, effectful extensions, consumed aliases,
  unavailable constants, inferred planes, and non-store-default datasets retain the general evaluator fallback.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Canonicalize payload strata with an allocation-free in-place primitive MSD radix.
  Rationale: canonical order is exact unsigned term-ID lexicographic order followed by unsigned raw weight bits.
  Partitioning on the highest differing bit preserves that total order, moves no objects, bounds recursion by sorting
  the smaller partition first, and avoids the tuple-swap work of comparison sorting without changing payload bytes.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Let private query-local builders transfer exact-sized terminal arrays into immutable primitive relations.
  Rationale: arrays allocated during relation sealing have no remaining mutable owner. A private owned-array factory
  removes the immediate second copy while the package constructor retains its defensive public-within-package
  contract for every non-builder caller.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Establish exact-leaf canonical order indirectly before constructing the immutable payload.
  Rationale: the accumulator's hash bucket array is dead after sealing and can hold compact row indexes. Sorting those
  indexes by the same exact unsigned tuple/raw-weight order avoids repeatedly swapping full tuples and lets payload
  construction take its linear already-canonical path without a scratch allocation or semantic approximation.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Separate primitive correlation hash buckets from insertion-dense row storage.
  Rationale: open-addressed buckets still provide collision-safe aggregation at the same load factor, while encoded
  dense indexes make immutable relation construction a bounded bulk copy rather than a scan across half-empty slots.
  The representation is query-local and does not alter candidate enumeration or correlation content.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Give independently proved childless empty terminals group-scoped memo identity.
  Rationale: structural equality proves expression equivalence but does not transfer membership into another
  append-only memo group. Installing the terminal in each owning group preserves zero propagation across commuted
  alternatives without merging groups or weakening ordinary logical interning.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Treat plan-quality regret as complete only after exact stable-identity execution of every inventoried
  alternative and solution-bag agreement within each Theme cell.
  Rationale: partial survivors, stale resume files, planner estimates, or a row-count-only comparison can all make a
  selected plan look artificially good. Per-alternative hard supervision plus median execution and bag fingerprints
  keeps failures explicit and makes the frozen regret thresholds reproducible.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Pair every synthetic timing cell with an untimed completion-status audit and report process-cold and
  saturated interpretations separately.
  Rationale: one zero-warmup fork cannot supply both measurements: JDK-25 compilation evidence shows tiered optimizer
  compilation continuing through its nominal saturated tail. A one-invocation cold cell and an independently warmed
  saturated cell preserve both observations without reclassifying compilation as planning. Reporting both prevents
  a convenient reinterpretation and the audit prevents an incomplete fast search from satisfying an exact-search
  timing gate.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Reconstruct wide candidate prefixes only for costing providers that consume them.
  Rationale: scalar costing depends only on retained child rows/costs and never reads the flattened relation or
  contribution arrays. Rebuilding both trees for every wide CSG/CMP orientation added an unnecessary factor of work;
  preserving the reconstruction unchanged inside provider-backed costing removes no state, candidate, or proof.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Preserve censored planning results as intervals and require full JMH identity before resume or aggregation.
  Rationale: a timeout is evidence of completed samples plus unknown nonnegative work, not a 75-second synthetic
  operation. Lower-bound sum/geometric arithmetic, unbounded upper endpoints, and exact-pair projections make every
  conclusion reproducible without hiding failed cells or mixing jars.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Pin the original frozen artifact bytes, not a semantically rebuilt substitute.
  Rationale: the manifest names a SHA-256 artifact identity and fat-jar timestamps make a clean rebuild bytewise
  different. The surviving exact jars preserve the comparison boundary; reconstructed jars are diagnostics only.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Allocate packed-planner optional columns and scratch from immutable source-shape facts, and defer work
  whose first legitimate consumer may never exist.
  Rationale: exact relation/output-mask counts and feature-use predicates are semantic properties already computed by
  the codec and binding-fact pass. Using them to size arenas, while retaining defensive growth, removes geometric
  copies and unused columns without changing candidate enumeration, winner identity, proof coverage, or telemetry
  when a feature is exercised. Collision-checked one-pass interning and deferred winner lookup apply the same rule to
  duplicate computation rather than storage.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Compile repeatable top-level FILTER conjunctions as acceptance-equivalent scalar and exact-EXISTS terms.
  Rationale: a SPARQL FILTER accepts an AND mapping exactly when every conjunct has true EBV; false and expression
  error both reject it. Preserving source order and declining volatile or compound non-conjunctive expressions keeps
  the result bag unchanged while allowing pure EXISTS/NOT EXISTS terms to retain primitive term-ID probes. Each
  exact-outcome memo includes both contextual filter identity and canonical RHS relation identity, so multiple
  subqueries sharing a correlation layout cannot exchange truth values.
  Date/Author: 2026-08-10 / Håvard and Codex.
- Decision: Canonicalize only provably internal parser-generated binding identities before plan-cache lookup.
  Rationale: property-path anonymous variables and non-root-visible single-definition Extension targets are alpha
  identities, while projected names, VALUES names, input collisions, and SERVICE boundaries are observable. Stable
  ordinals for the former let freshly parsed equivalent algebra reuse a plan; retaining exact packed-query equality
  as the final check prevents a routing-fingerprint collision from becoming a false hit.
  Date/Author: 2026-08-11 / Håvard and Codex.
- Decision: Derive cross-process physical-plan identity from lossless packed algebra plus explicit execution-control
  annotations.
  Rationale: raw query-model signatures contain parser nonces and omit fields on some typed operators. Reusing the
  complete packed identity gives exact structural equality, safe alpha-equivalence, and collision-proof canonical
  comparison without recognizing name patterns or changing plan selection.
  Date/Author: 2026-08-14 / Håvard and Codex.
- Decision: Require evidence-state realization to publish ID, guarantee, and disposition atomically.
  Rationale: an immutable state ID cannot be bound-only in one event and exact/composable in another. Failing an
  incomplete typed alias is safer than silently replacing Frontier continuation with unresolved scalar evidence.
  Date/Author: 2026-08-14 / Håvard and Codex.
- Decision: Measure Theme process-cold and JIT-saturated planning in independent JVM lanes.
  Rationale: zero-warmup single-shot samples continued compiling for hundreds of fast planner invocations and made
  both cache and uncached latency appear one to two orders of magnitude slower. A one-shot cold process plus a
  time-warmed AverageTime process preserves both facts, supports normalized allocation, and prevents early
  compilation from being relabeled as saturated planning. The original manifest remains immutable and the v2
  manifest supersedes only this invalid timing classification.
  Date/Author: 2026-08-11 / Håvard and Codex.
- Decision: Authenticate every resumed plan-quality artifact against its producing jar and complete command identity.
  Rationale: a stable alternative ID proves identity within one inventory, not that an inventory came from the
  currently selected implementation or protocol. SHA-256 jar equality plus exact sampling, timing, store, query, and
  output arguments prevents a stale successful process from being relabeled by a fresh campaign index.
  Date/Author: 2026-08-11 / Håvard and Codex.
- Decision: Cache deterministic work-limited incumbents in a lane distinct from complete plans.
  Rationale: a deadline-free search stopped by an explicit work limit is reproducible for the same complete limit
  tuple and otherwise paid its full planning cost on every AUTO request. Exact lookup, stale-plan validation, larger
  budgets, and deadline/resource/unsupported outcomes remain fail-closed and cannot consume that approximate lane.
  Date/Author: 2026-08-11 / Håvard and Codex.
- Decision: Make stronger materialized-root cardinality a cold-plan opt-in certificate, not a hot-cache callback.
  Rationale: LMDB's semantic exact-zero check depends only on the versioned planning context, so recomputing it after
  every collision-checked cache hit allocated more than half of the optimizer's hot replay without adding evidence.
  A separate capability keeps ordinary cost-model contracts unchanged and stores only finite non-negative certified
  rows with the immutable plan generation.
  Date/Author: 2026-08-11 / Håvard and Codex.
- Decision: Classify JMH's authenticated operation timeout as censoring even when its process exits zero.
  Rationale: the outer supervisor observes a successful JVM, but JMH can interrupt an overlong benchmark invocation
  and emit an empty result array. Requiring the exact frozen operation-timeout command and marker distinguishes that
  right-censored measurement from stale or corrupt output without converting the cutoff into a point estimate.
  Date/Author: 2026-08-11 / Håvard and Codex.
- Decision: Generalize the store-owned collision-safe exact-transform cache to deterministic binary finite joins.
  Rationale: exhaustive search repeatedly composes the same immutable finite payloads across alpha-equivalent
  uncached sessions. Retaining and collision-checking the complete second payload plus structural mapping lets a
  verified transform replay avoid duplicate payload construction while search and candidate enumeration remain
  unchanged; learned calibration and non-deterministic surfaces continue to decline the cache.
  Date/Author: 2026-08-11 / Håvard and Codex.
- Decision: Reject benchmark-specific plan-selection heuristics and require a general invariant plus saturated
  evidence before changing rewrite, continuation, cost, dominance, or final-policy behavior.
  Rationale: an apparent Theme-cell regret can arise from tiered compilation or noise, while a missing rewrite,
  scalar restart, lost Frontier state, incomplete continuation, or omitted cost dimension is an architectural defect.
  Stable alternative identity, authoritative result bags, full state-event traces, algebra-tree comparison, and a
  separately warmed timing lane distinguish those cases without query IDs, shape recognizers, magic weights, search
  thresholds, or benchmark-budget tuning.
  Date/Author: 2026-08-12 / Håvard and Codex.
- Decision: Treat a detached selected-root Frontier certificate as authoritative over an optional legacy scalar
  cardinality callback.
  Rationale: a database-exact Frontier point and a certified positive lower bound are strictly stronger evidence than
  a second scalar tree walk; replacing either loses information and can reverse a proven nonempty root to zero.
  Unresolved or zero-inclusive bounds still consult the callback, so fail-closed exact-zero detection remains
  available without inventing a cost, threshold, shape rule, or benchmark exception.
  Date/Author: 2026-08-13 / Håvard and Codex.
- Decision: Pass already-proved distinct-variable requirements directly to the LMDB cursor-skip planner.
  Rationale: cloning an algebra node and attaching telemetry merely to recover a caller-owned set adds allocation and
  couples an exact physical-access proof to mutable diagnostics. Both the existing annotated entry and the direct
  entry delegate to one structural selector, so the change removes representation churn without changing eligibility,
  access paths, cost, search, continuation, or policy.
  Date/Author: 2026-08-13 / Håvard and Codex.
- Decision: Recover from sampled non-observation only through an exact continuation admitted by the declared scan
  contract, and only after the normal access surface has had the first claim on derivation identity.
  Rationale: absence from a bounded sample is not database absence. An exact retained input can still prove the next
  transition, but speculative inexact recovery both spends unreserved work and can collide with a later exact
  surface. Ordering by evidence authority and checking complete work against the existing resource contract preserve
  all candidates without a query-shape heuristic or a new threshold.
  Date/Author: 2026-08-13 / Håvard and Codex.
- Decision: Admit an exact derivation to continuation search only when the exact Pareto arena retains it, while
  preserving every admitted derivation in immutable audit history.
  Rationale: a Pareto `DUPLICATE` or `DOMINATED` result is a proof that the derivation cannot improve any legal suffix
  in its complete continuation-equivalence class; replaying it as a winner violates Bellman retention and causes
  combinatorial duplicate work. Candidate ID zero denotes absent or resource-limited Pareto evidence, not dominance,
  so that case remains searchable to preserve completeness. A Pareto-rejected derivation selected while materializing
  an already-costed parent keeps a directly addressable immutable recipe row, but that row is not linked into the
  continuation iterator. This preserves exact parent reconstruction without making dominated work searchable.
  This is proof-directed pruning, not a heuristic.
  Date/Author: 2026-08-13 / Håvard and Codex.
- Decision: Accept an intermediate physical join's candidate state only when it covers the union of both available
  child lineages; otherwise derive or retain the child-composed state explicitly.
  Rationale: an evidence-state ID is query-local, not proof that it belongs to the current join. Typed factor lineage
  supplies the collision-safe ownership certificate and prevents an unrelated scalar boundary from replacing a
  composable Frontier, without changing join eligibility, cost, dominance, or final policy.
  Date/Author: 2026-08-13 / Håvard and Codex.
- Decision: Order exact binding-mask strata with one unconditional in-place heapsort and the existing unsigned
  lexicographic comparator.
  Rationale: heapsort preserves the exact total order and deduplication input while changing worst-case complexity
  from O(n squared) to O(n log n). Applying it uniformly avoids a size crossover, workload classifier, query branch,
  or benchmark-tuned threshold; identical rows need no stable order because every compared word is equal.
  Date/Author: 2026-08-13 / Håvard and Codex.
- Decision: Treat a provider-owned Frontier memory ceiling as a typed incomplete-search result, never as authority to
  restart the query with scalar costing.
  Rationale: the complete incumbent and all admitted state/cost events remain valid, but the rejected arena growth
  means exact fixpoint certification is impossible. `INCOMPLETE_RESOURCE_LIMIT` exposes that fact and permits an
  explicit larger-budget retry without suppressing a continuation or falsely claiming exactness.
  Date/Author: 2026-08-13 / Håvard and Codex.
- Decision: Make persisted statistics generations directly queryable and prohibit exact statement replay during
  optimization.
  Rationale: raw-payload readiness plus a query-local derivative creates two incompatible availability states and
  makes planning memory and I/O proportional to dataset cardinality. A mandatory query-ready shard is validated
  before `READY`, while missing, stale, corrupt, or memory-constrained evidence returns a conservative stored
  interval or the conventional estimator. This makes the safety fallback independent of the V2 estimator's eventual
  accuracy and removes both the payload-writer exhaustion and Cartesian `int` overflow failure classes.
  Date/Author: 2026-08-14 / Håvard and Codex.
- Decision: Keep V2 in this sole roadmap as Milestone 9 instead of creating
  `.agent/execplans/frontier-statistics-v2.md`.
  Rationale: `.agent/execplans/README.md` declares this file the only optimizer ExecPlan and explicitly forbids child
  or follow-on plans. Consolidating the approved V2 contract here preserves one dependency order, progress marker,
  decision log, and recovery source while still recording every requested resource equation and acceptance gate.
  Date/Author: 2026-08-14 / Codex.
- Decision: Sample high-degree center adjacency in a second independent stage and persist both admission thresholds.
  Rationale: coordinated center inclusion preserves join-key correlation, while an independent per-domain edge
  priority bounds retained adjacency under arbitrary hub degree. Horvitz-Thompson degree scaling and the same-domain
  overlap correction preserve bag multiplicity without retaining the complete center or reporting sampled rows as
  exact.
  Date/Author: 2026-08-15 / Codex.
- Decision: Pin persisted estimator identity in the generation manifest and keep the current pointer backward
  readable.
  Rationale: READY must certify an algorithm, not just files. Manifest revision 5 declares shard revision,
  capability mask, hash/bucket schema, Omni dimensions, term width, and tuple-ordinal width; legacy revision-4
  manifests load as explicitly unversioned migration state instead of being silently relabeled.
  Date/Author: 2026-08-15 / Codex.
- Decision: Persist every mutation range as one aligned scalar-and-Omni shard family and merge witnesses by exact
  statement identity.
  Rationale: population counters alone cannot update projected distincts or correlated joins. Sparse insertion
  postings and deletion tombstones admitted under the immutable base cutoff preserve coordinated witnesses; the
  query merge requires base presence plus all signed transitions to equal zero or one. A signed-only migration
  prefix deliberately disables later witness overlays and retains the complete scalar history rather than applying
  a biased suffix.
  Date/Author: 2026-08-15 / Codex.
- Decision: Size deletion reserves with a per-shard binomial KL union bound, not a fixed multiplicative factor.
  Rationale: retaining `K / (1-r)` witnesses only makes the expected survivor count equal K and leaves exhaustion
  probability near one half. The smallest retained n satisfying the `10^-6 / populatedCells` upper-tail target
  gives the declared 25-percent random-churn guarantee; disk fitting scales authoritative K and recomputes n rather
  than truncating reserve rows.
  Date/Author: 2026-08-15 / Codex.
- Decision: Compact adjacent equal-size delta ranges as a binary counter using mapped sequential column streams.
  Rationale: a fixed layer count otherwise turns frequent small commits into a full 20-billion-row base rebuild and
  grows mapped metadata linearly. Size-tiered compaction gives logarithmic query fan-in and amortized rewrite work;
  all six shard kinds move together under a 4 MiB compactor lease, while old published files remain lease/rollback
  safe.
  Date/Author: 2026-08-15 / Codex.
- Decision: Derive sampled-estimator confidence from independent-lane dispersion, shared witnesses, effective sample
  size, represented mass, and composition depth.
  Rationale: the former leaf pseudo-count assigned confidence to zero observed support, while star/path/bridge
  estimators treated four available lanes as strong evidence even if their estimates disagreed. One primitive
  quality vector now drives conservative intervals across the ensemble; exact complete-cell answers bypass the
  statistical path. This adopts the useful quality contract from the Java 21 quad-composable reference without its
  heap object model.
  Date/Author: 2026-08-16 / Codex.
- Decision: Query immutable base Omni cells through progressively doubled unsigned-priority prefixes.
  Rationale: the prefix of a sorted bottom-K or threshold sample is itself a valid, more restrictive bottom-K sample.
  Starting at 64 rows per design lane bounds common-probe mapped I/O, while quality-triggered doubling preserves
  sparse accuracy and reaches complete-cell proofs. Signed mutation overlays remain on the full bounded merge until
  their multi-source prefix lineage is proved, preventing a latency optimization from weakening update correctness.
  Date/Author: 2026-08-16 / Codex.
- Decision: Do not store sampled state for predicate and context dimensions that are deterministic inside a
  predicate-local/default-context shard.
  Rationale: predicate projected distinct is exactly one, and default-context distinct is exactly one. Persisting
  HLL registers or a predicate-component AGMS plane for those dimensions duplicated information and pushed the 20B
  build workspace 184 MiB over its 1.25 GiB envelope. Exact scalar columns plus streamed S/O/C AGMS publication save
  256 MiB and admit the complete builder at 1,269,897,643 bytes without weakening a query answer.
  Date/Author: 2026-08-16 / Codex.
- Decision: Snapshot sampling lineage into primitive lane estimates and index immutable shard columns once on open.
  Rationale: JDK 25 JFR attributed 60.72 percent of allocation pressure to per-cell `SamplingReference.bottomK`
  objects and 22.97 percent of execution samples to repeated binary searches in `column(int)`. Two scratch-owned
  primitive accumulators preserve the exact same minimum-inclusion lineage, while a bounded open-addressed table
  supports arbitrary sparse nonnegative column IDs with memory proportional to column count. The final profile has
  no bottom-K reference allocation and only 0.04 percent column-lookup samples.
  Date/Author: 2026-08-16 / Codex.
- Decision: Provider callbacks and provider-internal batches are charged to a separate `providerWorkLimit` of the
  one `PackedSearchBudget`; `workLimit` and `workUnits()` count search steps only, and the provider limit is
  unbounded by default.
  Rationale: the unified currency made the unchanged default budget (512) return unpriced plans for 138 of 176 theme
  queries; rescaling the constant would encode internal callback multiplicity. Consequence to revisit in phase 6:
  only the deadline bounds provider work, and detached refresh runs without a deadline.
  Date/Author: 2026-11-09 / owner decision, recorded by Claude.
- Decision: Anytime invariant. When any limit stops planning, the planner returns the cheapest complete priced plan
  retained so far; a refused callback abandons only the candidate being costed; a parent that cannot be costed is
  offered unpriced over its priced child; an absorbed stop never yields a completeness certificate.
  Rationale: the roadmap already required a preserved incumbent; the abort path violated it.
  Date/Author: 2026-11-09 / Claude.
- Decision: Comparison completeness keeps its meaning (every executable candidate priced). The tier-dominance rule
  (ignore an unpriced fallback-tier candidate that the ranking policy can never select over a priced one; it would
  make 49 theme queries COMPLETE with no plan change) is on hold until replay-only caching is measured in use.
  Rationale: pricing the written-order fallback from the enumerator's result was prototyped and rejected (the
  incumbent and the enumerator build different context keys, 0 of 2,717 lookups matched, and pricing it afresh
  changed 17 selected plans); the tier rule would also switch stale validation, family reuse and feedback back on.
  Date/Author: 2026-11-09 / owner decision.
- Decision: A decision with an unavailable comparison may be cached for exact replay only and never acts as a
  ranking certificate.
  Rationale: re-planning an identical request on unchanged inputs is deterministic, and gating all caching on a
  complete comparison left most real queries uncached until phases 2–3 supply contextual evidence.
  Date/Author: 2026-11-09 / Claude, owner informed.
- Decision: Decision-cache admission evicts only for a key with proven reuse; there is no fixed in-flight reserve.
  Rationale: a fixed reserve removes capacity permanently and refuses an entry that fits under a small budget.
  Date/Author: 2026-11-09 / Claude.
- Decision: Shrink cached plan entries by making the accounting exact for frozen cache-owned templates and by
  dropping explanation diagnostics from them, with the opt-in store setting `retainCachedPlanDiagnostics` for
  tooling. The cache's 1/16 share of the evidence budget is unchanged until the shrink has been measured in use.
  Date/Author: 2026-11-10 / owner decision.
- Decision: `isBounded()` does not include the provider limit, and a retained-byte stop latched during session open
  still forces the unpriced fallback; both are recorded as open, not changed.
  Rationale: including the provider limit would make a provider-limited run seed and cost prefixes twice, so an
  exact fit would not be reproducible, and the production default leaves that limit unbounded.
  Date/Author: 2026-11-10 / Claude.
- Decision: Whether a planning may be shared is an explicit session property (`PackedCostSession.reproducible()`),
  not a simulated budget refusal.
  Rationale: a faked provider-work refusal degraded the cut planning itself and reported it as
  INCOMPLETE_WORK_LIMIT; the planning is valid for its own request and only its publication is unsafe.
  Date/Author: 2026-11-10 / Claude.
- Decision: "Newer evidence" for replacing a retired or unservable cached decision means: data generation not
  lower, and an evidence snapshot different from the one the retired decision was built under.
  Rationale: the same snapshot would reproduce the retired decision; a lower data generation is an older pinned
  reader; requiring a strictly higher data generation left a retired key uncached on a store without commits.
  Date/Author: 2026-11-10 / Claude.
- Decision (owner, 2026-11-10): implement all three steps of
  `.agent/designs/GH-0000-cache-validity-under-execution-feedback-2026-11-10.md`: the planning revision advances
  only when planner-visible learned state changed; a cached decision depends on the learned cells its planning
  read; small movements of a read value do not invalidate, with the tolerance chosen by measurement.
  Rationale: with execution, the feedback epoch is a clock, so every completed query invalidated every
  evidence-exact entry (replay-only hits 0 of 116 in rotation, 0 of 142 after an own completed evaluation).
  Date/Author: 2026-11-10 / owner, recorded by Claude.
- Decision: step 2 validates a cached decision by re-executing the learned reads its planning recorded and
  comparing outcomes, not by per-cell versions.
  Rationale: what a planning reads is f(cell statistics, the model's decay clock, the calibration shared by the
  applicability, existence of sibling keys, the planner's own inputs); a write to another cell moves the decay
  clock and the shared calibration, so a version on the cell read is unsound
  (`.agent/research/GH-0000-phase1-continuation-2026-11-09/learned-state-read-write-inventory.md`, H.1, H.2, H.6).
  Re-reading is exact for a reproducible planning and needs no model of those couplings. Step 1 is specified as a
  sum of component mutation counters (`.agent/designs/GH-0000-cache-validity-step1-truthful-planning-revision-2026-11-10.md`).
  Date/Author: 2026-11-10 / Claude.
- Decision: before the step-2 spec, (a) make the model's "other applicability known" answer constant-time
  (`.agent/designs/GH-0000-flm-applicability-index-2026-11-10.md`), (b) put filter-selectivity reads in the read
  log, and (c) measure the validation decision directly (replay planning N's reads before planning N+1, compare
  plans, with the planner's reproducibility flag and a no-feedback control).
  Rationale (sizing measurement, `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/measure-validity/summary.txt`):
  step 1 alone leaves the feedback component moving after 94–98 % of completed evaluations; the filter component
  moves in about 18 % of evaluations; re-reading costs a median 14 % of a planning because each decision scans a
  whole key map for a gate label; 875 of 916,396 model decisions return an estimate; consecutive plannings of one
  query have identical reads in 27–54 % (rotation) and 40–69 % (repeat) of cases with no plan change among them,
  while all 27 plan changes fall where the read set differs, 20 of them without an attributed cause.
  Two defects seen and sent to root-cause analysis, not yet fixed: parser-generated variable names inside physical
  and event learning keys (33 of 182 queries never reuse those cells; the physical map reaches its 4,096 cap), and
  physical learning in the logical population never yielding an estimate (0 of about 1.58 million decisions).
  Date/Author: 2026-11-10 / Claude.

## Outcomes & Retrospective

The phase-1 B/C contract and checkpoint exception boundary are a source-stable, test-first review candidate. The
remaining phase-1 provider accounting, strategy integration, executable binding, cache/feedback and performance
qualification are not complete; the positive test exercises a caller-supplied provider without selecting a new
physical strategy. The immutable packet and limitations are at /private/tmp/rdf4j-phase1-bc-20261108-qleCkGY3/review-packet/.

The upstream merge now incorporates its native-local selector and retains the branch's uncommitted costing and
mapped-view work. The merge-specific source and regression checks are recorded in Progress; qualification of the
restored combined source is in progress. The shared physical strategy provider and selected-to-executed native
descriptor are not implemented, and no post-merge end-to-end performance improvement is established. The original
planning benchmark cannot serve as a valid matched baseline because it contains snapshot-scope fallback.

The 2026-11-06 addition maps current source owners to nine dependent implementation phases and defines correctness,
planning-overhead, execution, cache, and feedback acceptance gates. Implementation and validation are now authorized
and have started with the required baseline build; the production integration and performance qualifications remain
outstanding. No earlier benchmark result establishes a performance claim for this design.

The 2026-08-18 MEDICAL q9 estimator and costing repair is complete. Projected-distinct sampling now averages every
configured equal-weight lane without sorting or overflowing, treats zero as a valid replica, rejects incomplete and
unobserved all-zero ensembles, and caps only the completed mean. Exact single-degree projections bypass sampling when
one effective statement plane is proved nonempty. The mapped cost model preserves an optional point separately from
structural lower/upper bounds, tightens uppers with mapped leaves and complete finite assignments, and makes
memoized semi/anti costing consume the upper while retaining point telemetry. The complete-store q9 returns 16,352,
plans and executes materialized hash, opens its RHS once, and scans RHS rows proportionally rather than repeating
52.6 million source rows. The public scalar APIs and Frontier shard format remain unchanged, and join-cardinality
lane medians remain untouched.

The first JDK 26 qualification disproved the premise that restoring the older algorithmic I/O shape would by itself
restore the older latency. The ordinary 3x3 run was 359.656 ms/op and the ten-iteration JFR run was 363.781 ms/op,
compared with the retained July 149.444 ms/op generic-`Difference` anchor. That residual was not LMDB I/O, GC,
normality, or the projected-distinct center: 70.83 percent of JFR execution samples were in the newer typed
semi/anti operator's saturated `PrimitiveDistinctBindingTracker.record`.

The 2026-08-18 runtime redesign closes that separate defect without weakening estimator or cache-safety rules. A
collision-safe exact prefix retires at 75-percent occupancy into three p=12 HLL register sets, distinct quality is
explicit through runtime feedback and LMDB sidecar version 22, node counters are execution-local until root close,
and high-frequency timing keeps exact counts while sampling after 32 calls. Materialized probes use a flat retained
value index and share their semantic fingerprint with distinct telemetry when the projections match. The final q9
qualification is 123.052 ms/op with telemetry disabled, 126.095 ms/op with sampled-full telemetry, and 129.915 ms/op
for the sampled-full JFR run, all faster than the July anchor. Telemetry accounts for 49 of 3,615 measurement-window
Java top-frame samples (1.36 percent), or 242 samples (6.69 percent) including combined iterator instrumentation; no
individual instrumentation method exceeds 2.49 percent. The sampled allocation profile assigns zero bytes to the
hybrid tracker, node accumulator, registry, feedback wrappers, deferred join telemetry, or materialized index. The
final complete query-evaluation suite passes 1,446 tests. The fresh 117-query result-bag audit passes, and all
telemetry-related tests pass in the 2,230-test LMDB run; only the unrelated pre-existing `SPARSE` benchmark-default
assertion remains red.

The benchmark-only sampled-full control independently verifies the broader instrumentation path. It preserves the
prepared structural fingerprint and ordered result bag, records 126.095 ms/op against the 123.052 ms/op disabled
baseline (2.47 percent overhead), and records 129.915 ms/op under JFR. In that sampled-full measurement window, core
telemetry accounts for 1.36 percent of Java top frames and the complete combined-wrapper instrumentation surface for
6.69 percent; the largest individual instrumentation method is 2.49 percent and the telemetry/materialized-index
allocation sites receive zero sampled bytes.

The 2026-08-06 consolidation leaves one optimizer ExecPlan containing the architecture, completed baseline,
unresolved implementation work, dependency order, test workflow, acceptance gates, and durable decisions. The
subordinate packed, Frontier-sketch, state-continuity/cache, learned-feedback rollout, and research hand-off plans
have been absorbed and can be recovered from Git history. This planning change alters no runtime behavior.

The most important lesson from the retired plans is that migration shortcuts must be labeled and removed, not
promoted into permanent contracts. Scalar adapters, one-entry winners, singleton-only DPhyp consumption, incomplete
rewrite coverage, and Monitoring-only learning were useful intermediate states. They are explicitly unfinished here.
Future contributors update this section after every milestone with what changed, what evidence passed, what remains,
and whether any premise in the plan was disproved.

Milestone 9 is an urgent architecture correction discovered while Milestone 8 performance qualification was in
progress. The existing v96 corpus artifacts remain immutable evidence, but their persistent estimator source cannot
meet the 20-billion-triple resource contract. The first deliverable is deliberately smaller than the new file format:
planning must become safe and store-size-independent when a query view is unavailable. No V2 accuracy or scale claim
is made until the later shadow-mode gates pass.

The focused `papers2` review tightened rather than replaced that architecture. It showed that DPhyp topology can be
retained while scalar subset winners are removed; CD-E completeness must be qualified by predicate decomposition and
rewrite closure; rich sketches, samples, characteristic sets, and moments are composable state; and Umbra/GroupJoin
make startup, pipelines, memory, residency, and execution mode part of physical costing. LEO's adjustment layer
remains useful only with qualified observations, scoped residuals, invariants, drift/OOD fallback, and lifecycle
containment. Indexed Algebra supplies the missing compact binding-flow/path index beside, not inside, memo identity.

The subsequent plan review converted those directions into executable contracts. Semantic closure now precedes and
reactivates join enumeration; produced values have explicit merge semantics; catalog guards and memo proofs are one
runtime path; the cost algebra, multiword DPhyp representation, retained-byte accounting, and incomplete statuses are
normative; and LEO-plus has a selected NIG/conformal policy with promotion gates. This revision also changes no
runtime behavior. Its purpose is to prevent implementation from claiming exactness, safety, or learned readiness
without the representation and evidence needed to prove it.

Milestone 0 then made that review executable. The catalog now maps every live packed proof bit to an immutable
descriptor and loadable implementation, inventories all relational/scalar/payload opcodes and planner boundary
categories, classifies current scalar degradation points, and asserts one production join-order owner. The preserved
red evidence files separate known semantic and search defects from the green catalog architecture baseline, so later
milestones can repair one typed boundary without losing the original reproduction.

Milestones 1 and 2 then removed the semantic blind spots ahead of join search: packed value-flow/path records make
branch-local binding guarantees explicit, and every executable rewrite must present a cataloged applicability result
whose proof set covers its descriptor before memo mutation. Predicate facts remain proof-only, survive supported
value-flow transforms, stop at remote and unsupported boundaries, and can introduce a finite binding access
alternative without relying on a query FILTER. The generated catalog names production, research, proposed,
unsupported, unsafe, and deliberate non-rules from one manifest and is verified against both source digests.

Milestone 3 made physical costing a versioned primitive algebra instead of another scalar estimate. Forty-one stable
dimensions declare unit, scope, uncertainty, aggregation, and legal composition; lower/point/upper endpoints and
lineage survive sequential, parallel, pipelined, blocking, repeated-open, and live-set composition while unknowns
remain unknown. Cardinality and q-error remain estimator state/diagnostics. Frontier payload and record v2 remove
exact-mode lane duplication without changing logical lanes, preserve record-level exact-heavy rows inside sampled
generations, retain bidirectional paired/audit views, and read legacy v1 generations for deterministic rebuild.

Milestone 8 has closed authoritative result-bag semantics for all 117 Theme cells and established the immutable
matched planning harness. The first retained duplicate-work repair makes bounded dense exact search own its complete
seed, passes the new red-to-green contract, all 17 subset-kernel contracts, and all 76 packed search tests, and reduces
matched SPARSE q6 steady planning time by 12.07 percent with 16.10 percent less allocation. A bijective exact-
projection path then removes an unnecessary full coalescing copy and closes the 64 KiB/640-row memory regression.
Finally, query-accounted linked emission segments remove the remaining geometric buffer copies: the latest matched
pair remains latency-neutral against the seed-owner result (93.425 versus 93.357 ms/op) while reducing allocation to
52,625,495 B/op, 25.59 percent below retained control. These are retained local gains, not completion of the campaign:
both tenfold full-corpus aggregates and the remaining p95, synthetic, regret, q-error, and fixed-plan execution gates
stay explicitly open. A reusable scalar-expression compilation boundary subsequently removes per-mapping evaluation
contexts and lambdas from FILTER, extension, and OPTIONAL-condition transitions. The matched ENGINEERING q0 pair is
latency-neutral and reduces allocation by 11.91 percent; its core contract and all 105 LMDB Frontier integration
tests pass. Reusing one stratified-expansion emitter per transform then removes all 217 measured per-mapping callback
allocations and another 7.78 percent of matched q0 allocation without changing the 20-case transform contract or the
105-case integration suite. Making each prepared mapped-index probe own its complete lookup key removes eight
duplicated memo columns, eliminates all 275 measured prepared-probe long-array growth samples, and reduces matched q0
allocation by another 8.88 percent while the mapped-index and integration suites stay green. Exact statement-probe
results now own their complete memo keys as well, eliminating all 273 statement-table long-array growth samples and
another 11.54 percent of matched q0 allocation with explicit retained-byte accounting and all 105 integrations green.
A subsequent canonical-selector precheck removes duplicate binding resolution and serves exact memo hits before
mapped-center eligibility. Its complete 105-test integration pass and matched q0 profiles show a small, non-regressive
CPU improvement (10,399 to 10,221 exact samples; 19.801 to 19.607 ms/op), while confirming that repeated mapped range
preparation—not dependency-graph construction—is the next material bottleneck. Pure variable-alias Extension chains
now preserve primitive term IDs across sequential aliases and mixed bound-mask strata. Removing the decoded
binding/evaluation path cuts inclusive extension samples by 84.76 percent; reusing the statement-probe callback fixes
the adjacent escape-analysis regression that initially hid the gain. The final matched q0 slice improves steady
planning from 27.452 to 26.721 ms/op and allocation from 10,587,554 to 10,509,647 B/op, with all 106 Frontier
planning integrations green. This remains a local gain: the full-corpus tenfold gates still require a broader exact
reuse or batching improvement across incumbent construction.

Conjunctive EXISTS/NOT EXISTS refinement now has a two-tier exact path. Small resident exact RHS relations build one
complete primitive correlation domain, including exact partial-key and empty-domain answers. Larger sampled RHS
relations retain the evaluator fallback but key it only by true outer correlation variables and share its exact
outcomes across every equivalent physical alternative. On matched TRAIN q8 this cuts steady planning from 102.936 to
71.536 ms/op, normalized allocation from 72,561,669 to 61,842,669 B/op, and evaluator-fallback CPU samples by 51.00
percent. The 16-test packed codec and 107-test LMDB Frontier integration suites are green. This is retained as a
general duplicate-work elimination; it does not relabel sampled evidence or claim the still-open aggregate gate.

The next complete matched run preserves that conclusion across the application corpus: all 117 supervised cells
finish successfully, and the 111 numeric baseline pairs improve to 6.808x fixed-corpus and 3.681x equal-query
geometric speedup. Those figures are authoritative non-completion evidence as well as progress: both remain below
10x, so Milestone 8 continues from a fresh profile of HIGHLY_CONNECTED q8 rather than suppressing candidates or
relaxing the acceptance contract.

That profile-driven slice now gives sampled single-statement EXISTS/NOT EXISTS its own exact primitive probe. It
shares one pinned LMDB best-index cursor batch across distinct outer keys, retains the logical-group outcome memo,
and exits on the first row satisfying selector, repeated-variable, and named-context constraints. HIGHLY_CONNECTED
q8 improves by 13.71 percent in steady time and 22.96 percent in allocation while the complete 108-test Frontier
integration class remains green. Corpus-level acceptance remains open until this and subsequent retained slices are
rerun under the full frozen matrix. A subsequent all-lanes exact-probe experiment was correctly rejected after the
matched q8 control regressed 3.95 percent despite reducing snapshot selectors: the mapped range-preparation cost
outweighed one fewer pinned cursor. Its production and temporary test changes were reverted, preserving the faster
design-lane-zero plus snapshot-batch path. A second attempt to prefer the snapshot even for complete mapped centers
was also rejected: allocation fell 2.80 percent, but planning-thread CPU samples rose 9.33 percent. Its transient
routing/test were reverted; only the independently correct factor-surface provenance preservation discovered by the
experiment remains.

The next ENGINEERING q9 campaign removes a larger repeated-work source without suppressing any candidate. Exact
statement-probe memo rows arrive as contiguous equal-weight proposal runs, so the bridge selector now advances their
cumulative mass once and maps every sorted systematic-sampling threshold through one fused inverse CDF. The focused
test first failed on the missing batched-proposal metric, then the 29-test finite-values planning suite and all 108
Frontier integrations passed. Against the retained borrowed-quad control, steady q9 planning falls from 71.103 to
53.958 ms/op, median planning from 61.864 to 45.784 ms/op, planning-thread samples from 28,756 to 19,906, and cached
replay samples from 11,061 to 3,741 while normalized allocation is effectively flat. This profile-backed batching
improvement is retained; the complete frozen campaign remains the authority for the still-open tenfold aggregate
gate.

The equal-weight survey now also stops iterating the exact tail once all retained context strata are resolved. That
follow-on cuts another 10.50 percent from matched q9 mean planning with flat allocation. The ensuing v7 full matrix is
valuable negative operational evidence: all 117 processes succeeded, but obvious macOS host contention concentrated
large regressions in early cells and produced only 6.056x/3.396x aggregates. Those timings are not promoted over the
quiet v5 authority; the next acceptance rerun must begin from an idle-host check.

Two exact query-local memos then remove work shared across Cascades alternatives without narrowing the search space.
The scalar FILTER memo keys the complete deterministic condition surface and evaluates full bindings on misses;
eligible top-level EXISTS conjunctions additionally use RHS-identity-safe primitive outcome memos. Its replicated q9
result improves steady last-20 latency 9.81 percent with flat allocation. The exact-correlation
relation memo reuses the canonical domain identity to retain RDF-value decoding once; two independent profiles
improve last-20 latency 4.62/5.17 percent and allocation 4.24 percent while matched materialization samples fall more
than 32 percent. Their red-to-green contracts, 31 finite-surface tests, and all 108 Frontier integrations are green.
They are retained local gains; the quiet full-corpus aggregate and the remaining p95, synthetic, regret, q-error, and
fixed-plan execution gates remain open.

Two native-boundary repairs and one proof-level planner repair follow. LMDB cursor quads now decode directly from
their bounded native address, reducing q9 allocation 16.49 percent; immutable Frontier query-index primitives use
the same owned-address principle, improving q0 steady mean 14.04 percent and q9 2.64 percent. The quiet v8 campaign
then completes all 117 cells at 6.955x fixed-corpus and 3.743x geometric speedup, still short of both tenfold gates.
An attempted lineage-derived transform index is rejected after a q0 regression. JDWP tracing then identifies two
alias-related exact lattices. Recording the already-proved trivial-bind cover relation lets the scheduler omit only
the dominated lattice after its covering seed/exact search exists. The alias-only contract falls from two builds to
one, all 76 packed search tests remain green, and matched q0 improves 6.34 percent with 16.88 percent less allocation;
q9 remains within 0.36 percent with flat allocation. These are retained exact reductions, not heuristic candidate
suppression, and corpus acceptance remains open pending the next frozen matrix.

The next profile-backed exact kernel removes RDF evaluation from sampled multi-statement EXISTS/NOT EXISTS. It
compiles arbitrary pure statement conjunctions into a deterministic primitive factor order, retains internal term-ID
bindings in fixed scratch, and gives each recursive depth an independent cursor lane in one pinned snapshot batch.
The semantic boundary covers deep joins, repeated variables, named graph variables, store-default scope, and
negation, while consumed aliases and effectful expressions decline to the evaluator. TRAIN q8 steady A/B/A improves
21.93 percent with 38.83 percent less allocation and eliminates the prior `JoinIterator` stack. The complete v10
matrix advances the 111-pair aggregates to 7.206x fixed-corpus and 3.865x geometric speedup. That is a retained
corpus gain, not completion: the 5,125.077 ms candidate sum remains 27.9 percent above the tenfold threshold, and the
absolute p95, synthetic, regret, q-error, and fixed-plan execution gates remain open.

The next two retained profile repairs improve general payload and correlation construction rather than changing
search. Allocation-free primitive radix canonicalization reduces SOCIAL_MEDIA q9 steady mean 3.81 percent and the
canonical-sort stack 44.39 percent. Exact-sized primitive correlation arrays now transfer once into their immutable
domain relation, reducing SPARSE q10 mean 2.63 percent and allocation 3.30 percent. A scratch occupied-slot index is
rejected after endpoint and allocation regressions. Exact leaf coalescing now sorts compact row indexes and emits
each tuple once, improving q10 mean 3.19 percent; insertion-dense correlation storage then removes the profiled
relation materialization leaf, improving q10 mean 9.97 percent and allocation 4.14 percent. All four post-v10 slices
are retained without candidate suppression. The first v11 matrix completes all 117 cells but is explicitly rejected
as acceptance evidence because a separate 97-percent-CPU LMDB Surefire run and two high-CPU storage-analysis
processes overlap it; its artifacts remain diagnostic. The aggregate campaign remains open pending an idle-host rerun
of the same immutable jar.

The next q6 profile-backed candidate attacks exact finite-surface work shared across exhaustive last-factor choices.
The completed factor-set cache now returns one immutable exact relation and, on a legal connected transition, derives
matched-prefix bag mass from the cached prefix/completed projection instead of repeating the equivalent LMDB scan.
Its red/green contracts include unequal prefix masses, an unmatched row, duplicate VALUES multiplicity, reordered
relation columns, and a prefix column legally bound by the last factor; 54 surface contracts, 110 planning
integrations, and all 117 Theme semantics/evidence checks are
green. This candidate is not yet counted as a retained performance win because two unrelated sustained CPU consumers
still prevent an authoritative bracket. The relation-only and scan-reuse jars remain immutable for that comparison.

The packed-planner cold-path pass then removes optional allocation and duplicate lookup work while preserving the
full exact search. Lazy metadata/trace/rule/JOIN scratch, exact arena sizing, direct canonical-group preparation,
single-pass expression interning, immutable-implementation reuse, and deferred winner-table lookup all retain their
matching focused greens. Exact binding-flow sizing removes 1,192 B/op in a bracket whose two candidate timings
straddle baseline. The current supervised synthetic campaign closes factor eight at 1.569334 ms/109,032 B and keeps
factor four explicit at 0.721625 ms/60,816 B rather than hiding the failed 0.5 ms timing gate. The generated 300-query
q-error corpus is green. A fresh exact LMDB jar now drives the resumable full-corpus stable-alternative inventory;
regret execution and matched fixed-plan timing follow only after inventory completeness.

The v77 continuity audit closes the broad regressions introduced while making sampled payload continuation linear.
Sampled misses no longer erase exact bound continuations or preempt exact access-surface identities; selected
physical joins retain only evidence certified by both child lineages; memo-only Cascades groups remain outside the
immutable source-expression consumer domain; and equivalent uncorrelated inner joins keep one learning feature cell
without erasing the real outer-domain feature of semi/anti algorithms. The five broad failures pass as one set, the
learning interaction pair passes, and the complete 134-test Frontier planning class is green in 126.649 seconds.
These are correctness and evidence-continuity results. Performance claims remain deferred until the already-observed
external JMH/storage contention clears and a clean immutable-jar bracket can be captured.

The v94 inventory is the first complete post-continuation audit of all 117 Theme cells under the final 16 GiB JVM,
8 GiB audit-retention ceiling, 512 MiB Frontier request budget, and 120-second process/query limits. It closes the
former PHARMA q11 timeout and proves every emitted plan uses intact event-state identities with no scalar or session
fallback. Three exponential cells remain explicitly resource-incomplete, which is the designed bounded-search
outcome rather than a correctness waiver. Milestone 8 is still in progress: the resumable stable-alternative
execution campaign initially rejected aggregate alternatives because their raw physical identity included a parser
nonce. The v95 focused MEDICAL_RECORDS q2 campaign closes that cross-process defect for all 13 alternatives with
identical result bags and zero regret. The fresh full v95 inventory has now reproduced all 117 v94 cell outcomes,
root counts, minimum selections, and intact event-state continuity without a timeout. Its authenticated execution
phase must compute complete regret for all exact inventories, followed by the six fixed-plan cells and
the still-open matched planning-time/allocation gates. No acceptance threshold has been weakened and no policy or
rewrite was changed to improve a benchmark.

Milestone 9 now has a complete query-ready base/delta substrate and a store-size-independent packed integration
boundary. The latest integration pass exposed two useful semantic/physical distinctions. First, positive mapped
term support can cost SPARQL typed equality only when generated aliases are evaluator-validated and remain explicitly
incomplete. Second, a finite anchor that precedes `NOT EXISTS` is safe when the selected materialized or memoized
algorithm bounds RHS work; the old textual-order rule belonged to the retired streaming implementation. Focused
tests cover both distinctions. Physical 100M-1B scale, held-out calibration, cold p95, background interference, and
the final post-repair module gate remain open, so no 20B authority claim follows from these results.

The shard-reader slice now closes its mapping-lifetime and cached-decode contracts on Temurin 26. The focused
`FrontierStatisticsShardTest` passes all 12 tests after formatting, and the supplied semantic verifier passes every
width, codec, legacy/revision-3 layout, irregular boundary, tail read-ahead, and concurrent first-touch case. The
complete LMDB run executes 2,237 tests and keeps the changed shard class green, but is not a module green: five
tests fail `A join telemetry side cannot have overlapping evaluations` in `JoinMetricsTracking` and the dirty
benchmark-default test still expects `SPARSE`. All five telemetry failures reproduce together in a focused rerun and
have no shard-reader stack frame, so they remain a separate branch-level gate rather than being called a suite-order
flake. The matched attached harnesses show repeatable cached-read gains
across all measured shapes and a roughly nine-percent open/close gain; they also expose a 43-to-46-percent isolated
cold-first-touch regression and about ten-percent more per-open heap allocation. Cold-NVMe/p95 acceptance therefore
remains open even though the mapping cleanup and hot-reader implementation are complete.

## Context and Orientation

Work from the repository root. RDF4J's public query algebra is `TupleExpr`, defined in the query-algebra model module.
The optimizer core is in `core/queryalgebra/evaluation`. LMDB's store-specific implementation is in
`core/sail/lmdb`. Core code must not import LMDB classes; LMDB contributes typed implementations at planner
extension boundaries.

The packed planner package is
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed`.
`PackedQueryCodec` imports an object algebra tree. `PackedQuery` owns immutable encoded arrays. `PackedMemo` interns
logical and physical alternatives. `PackedLogicalRuleProgram` and `PackedFilterRules` add logical alternatives.
`PackedJoinHypergraph` and `PackedDphypEnumerator` describe join topology. `PackedJoinEnumerator` currently owns the
dense, sparse, correlated, inherited-prefix, and fallback search paths. `PackedCostSession` fills the reusable
`PackedCostEstimate`; `PackedPhysicalMetadataArena` and `PackedCostingTraceArena` retain candidate metadata and
events. `PackedWinnerTable` currently chooses one scalar winner. `PackedPlanRecipe` detaches selected integer links,
and `PackedPlanMaterializer` creates the final `TupleExpr`.

The generic estimator-state package is
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cost`.
`FrontierStateArena` owns one query's immutable evidence states. `EvidenceStateRef` and `EvidenceStateSummary`
describe a state; `FrontierLinearTransforms` compose it; `FrontierEvidenceBundle` detaches it. `BagEstimate` is a
compatibility surface and must not become the planner's canonical state. `DistributionSketch`,
`ProductDistributionSketch`, `FiniteRelationEstimate`, and the Frontier payload types are state components, not
independent scalar estimators.

LMDB's planner boundary is `LmdbPackedCostModel` plus `LmdbFrontierPackedCostSession` under
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`. Frontier storage and the mapped query index are in that
module's `frontier` package. `LmdbPackedPredicateRangeProvider` translates store-owned predicate facts into the core
range interface. `LmdbOperatorFeedbackStats`, `FrontierLearningModel`, `LmdbLeoSurfaceStats`, and
`LmdbLeoFeedbackStore` own learned observations and persistence. The store owns `PackedPlanCache`; query-local arenas
never become shared mutable state.

At Milestone 9 entry, LMDB owns two independent synopsis services. `LmdbQuadSynopsisService` maintains the older
in-heap Quad/Omni-style bound-mask sketches used by `LmdbStorageEstimatorEvidence`.
`LmdbFrontierSynopsisService` persists raw 12-`long` records and then builds a separate 72-byte-per-row
`FrontierQueryIndex` with five columns and four sorted permutations. Store startup can synchronously refresh or build
these structures. If the raw manifest is `READY` but the derivative is absent, stale, over budget, or failed,
`LmdbFrontierPackedCostSession` scans exact statement leaves into a query-local `FrontierStateArena`. These are the
specific moving parts Milestone 9 replaces. Generic Frontier state composition remains the consumer of the new
mapped statistics references.

The following terms have exact meanings in this plan:

- A **Frontier state** is a versioned semantic estimate containing as much available correlation, multiplicity,
  distribution, uncertainty, guarantee, and lineage information as the next supported operator can use.
- A **fact** is proven from query syntax, store metadata, exact data, or algebra semantics and may authorize a
  rewrite. An estimate or learned belief is not a fact.
- A **cost event** is one immutable measurement of a logical transform or physical implementation, with its input
  state, output state, local and child cost vector, properties, and learning applicability.
- A **Cascades memo** is the equivalence structure that stores logical expressions and their physical alternatives.
  It owns retention and final choice; an estimator or DPhyp callback never owns a winner.
- **DPhyp** is a dynamic-programming hypergraph algorithm that enumerates connected subgraph/complement pairs,
  abbreviated CSG/CMP. It generates legal topology and does not rank it.
- A **value ID** names one produced binding occurrence. A **binding-flow index** links value IDs to producers,
  consumers, and explicit UNION/OPTIONAL/JOIN/projection/subquery merge nodes, then maintains compact assured,
  possible, conditional, unbound, error, equality, and range facts along source-to-use paths. A variable name may map
  to several value IDs and is never assumed to have one producer.
- A **rule applicability result** is the runtime guard outcome for one catalog descriptor. Only an applicable result
  carrying every required proof ID may authorize memo installation; an estimate or confidence value is never a proof.
- A **search-completeness certificate** identifies predicate decomposition and rewrite closure, non-inner legality
  rules, Cartesian-product policy, width kernel, catalog/fact/hypergraph revisions, and completion status in addition
  to the generated CSG/CMP pairs.
- A **continuation key** contains every state/property a later operator can observe. Two prefixes are equivalent only
  if every legal suffix sees them as interchangeable.
- A **Pareto frontier** is the set of candidates not dominated across all relevant cost dimensions. It is unrelated
  to the Frontier estimator despite the shared English word "frontier".
- A **cost algebra** is the registry of units, local/inclusive scope, uncertainty, and sequential, parallel,
  first-result, repeated-open, and live-memory composition rules used to build every physical cost vector.
- An **exact search result** means the proof/rewrite/hypergraph/Cascades worklist reached a fixpoint. A result stopped
  by work, deadline, retained-byte, or unsupported-semantics limits is explicitly incomplete even when its incumbent
  is executable and semantically correct.
- **LEO-plus** is the learned layer derived from DB2 LEO ideas but strengthened with commensurate observations,
  logical/physical residual separation, observation completeness and censoring, uncertainty, exact applicability,
  conformal out-of-distribution fallback, learned-cardinality invariants, deterministic dependent costing,
  asymmetric hysteresis, and safe lifecycle control. Its selected baseline is the existing hierarchical
  Normal-Inverse-Gamma model, abbreviated NIG.

The Theme catalog is implemented in the LMDB benchmark/test sources and contains nine themes with thirteen queries
each. `ThemeQueryPlanRunBenchmark` exposes cached, uncached, and execution lanes. The query-plan snapshot CLI under
`.codex/skills/query-plan-snapshot-cli` captures structure and estimates. Audit evidence under
`profiles/lmdb-opt/theme-audit-2026-08-04/` contains the continuation, DPhyp, GROUP, rewrite, semantic-validation,
and performance designs already summarized here. These artifacts are evidence only; this file is the sole plan.

## Plan of Work

Execute milestones in order because their interfaces are dependencies. Milestone 0 freezes the inventory and red
oracles. Milestone 1 makes the working representation lossless and gives every produced occurrence and merge a safe
value identity. Milestone 2 makes the catalog closed-world, couples guards to proofs, and produces semantic/range
rewrite closure before graph construction. Milestone 3 preserves rich estimator state and gives every physical
dimension one cost algebra. Milestone 4 installs continuation/Pareto retention with deterministic resource admission
and explicit incomplete statuses. Milestone 5 repeatedly feeds proof-checked rewrites and facts into hypergraph
construction, exact one-word or multiword DPhyp, Cascades alternatives, and dependent-parent reactivation until the
certificate reaches a fixpoint. Milestone 6 adds the selected NIG-plus-conformal learned policy to those vectors.
Milestone 7 makes decisions durable and explainable. Milestone 8 closes correctness before tuning. Milestone 9
preempts only the remaining Milestone 8 timing campaign because the current persistent estimator cannot safely run at
the required scale. It first closes the no-replay and no-Cartesian-allocation safety invariants, then delivers the
query-ready format, bounded builder, estimator ensemble, online lifecycle, and scale qualification. Resume the
remaining Milestone 8 comparisons only against a frozen V2 shadow/authoritative manifest.

For the Milestone 9 shard-reader slice, first add focused tests in
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/frontier/FrontierStatisticsShardTest.java`. Construct a
revision-3 shard with eight contiguous blocks and prove that touching all eight adds five physical mappings while
retaining eight logical mapped blocks; construct the equivalent gapped layout and prove that it retains eight
physical mappings. Corrupt one data block and prove repeated failed first touches do not retain mappings. Open a
valid shard without touching data and prove header and directory validation leave no metadata mapping behind. After
capturing the pre-change failure, reshape
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/frontier/FrontierStatisticsShard.java` so metadata regions
are heap-read when small and explicitly unmapped otherwise, data blocks decode from a padded one-word window, regular
block layouts use direct arithmetic, irregular layouts use bounded fences, and contiguous blocks promote to one
shared mapping only after four successful dedicated touches. A close/first-touch race must not publish a mapping
after the registry is sealed. Keep the revision-2 and revision-3 on-disk formats unchanged.

Do not create another ExecPlan for a milestone. Before beginning a slice, update `Progress` so exactly one entry is
marked as the active milestone, add the smallest failing in-repository test required by the repository's Routine A or D, and
persist its Surefire/Failsafe evidence. Then implement the general contract, rerun the exact selector, broaden to its
class and affected modules, and update this plan's progress, discoveries, decisions, outcomes, catalog entries,
interface descriptions, and evidence paths before moving the marker.

Keep behavior-changing slices attributable. Do not combine a codec expansion, rewrite-semantic change, dominance
change, learning-policy promotion, cache-format migration, and performance optimization in one patch. Additive
parallel representations are permitted temporarily when a differential oracle compares them, but production has one
planner and one semantic source of truth. Delete the old representation immediately after its parity and performance
gates pass; never leave a shadow or fallback route that silently chooses semantics.

Correctness order is fixed: semantic result bags and algebra legality; complete candidate enumeration; estimator and
cost accuracy; plan quality; then planning/execution performance. Under identical limits and exact mode, changing a
formerly exact result into an incomplete one is a regression even when it is faster; an explicitly requested bounded
run may still return its typed incomplete status. A different plan is acceptable only when its legality,
estimate/cost evidence, and matched execution result are recorded. Tune algorithms, primitive layout, locality, and duplicate work after profiling; never tune query IDs,
predicate names, preferred orders, fixed selectivity cutoffs, or unproved frontier caps.

## Concrete Steps

Use JDK 25 or newer. At the start of a working conversation, publish the complete checkout into `.m2_repo` with the
repository-required quick install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
      | tee maven-build.log \
      | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } \
             /Reactor Summary/ { summary=1 } summary { print }'

Never pass `-am` or `-q` to a test command. For each behavior change, select the smallest method first with the
repository runner and retain logs. The continuation contract already has preserved failing evidence; rerunning it
before Milestone 4 should still report one failure with expected cost 3 and actual cost 102. Exact existing and
prescribed selectors are:

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      LmdbFrontierPlanningIntegrationTest#missingQueryIndexNeverReadsStatementSourceDuringPlanning \
      --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      FrontierLinearTransformContractTest#exactJoinProductsAboveIntegerRangeDoNotNarrowOrAllocatePairs \
      --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      FrontierStatisticsHeapGovernorTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      FrontierStatisticsShardTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      LmdbStatisticsServiceTest --retain-logs

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      PackedFrontierSubsetKernelContractTest#denseKernelRetainsTheGloballyOptimalOrderedContinuationState \
      --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      OptimizerRuleCatalogTest#productionRegistrationRequiresRuntimeProofDescriptor --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      PackedPredicateRangePlanningTest#mayUnboundOptionalIntegerKeepsOriginalFilter --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      PackedCostAlgebraTest#sequentialParallelAndRescanCompositionObeyDeclaredLaws --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      PackedPlannerResourceBudgetTest#byteLimitReturnsIncompleteResourceStatusWithoutPartialPublication \
      --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      PackedDphypEnumeratorTest#multiwordHypergraphsMatchIndependentDpsubOracleAtSixtyFiveNodes \
      --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      ProductionLearningKeyTest#differingApplicabilityNeverSharesLogicalPosterior --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py \
      LeoPlusOutOfDistributionTest#outOfDistributionContextFallsBackToConventionalVector --retain-logs

`OptimizerRuleCatalogTest`, `PackedCostAlgebraTest`, `PackedPlannerResourceBudgetTest`, and
`LeoPlusOutOfDistributionTest` are prescribed new test classes. Create the named method before touching the
corresponding production code, run it, and expect a Surefire summary with `Tests run: 1, Failures: 1, Errors: 0`.
`PackedDphypEnumeratorTest` exists; add its prescribed multiword method there. Existing selector names must not be
replaced with placeholders.

Immediately preserve the first failure in `initial-evidence.<milestone-or-workspace>.txt` using the exact log and
report paths printed by `mvnf`; do not rely on reports that a later run may overwrite. After the fix, rerun the same
method and class, then the modules without `-am`:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

For semantic rewrites, run or add direct bag-result tests before plan-shape tests. Generate small RDF datasets that
exercise duplicates, unbound values, expression errors, named/default graphs, correlation, and nested algebra. For
search changes, compare candidate sets and Pareto survivors against an exhaustive oracle before checking one chosen
plan. For estimator changes, run exact-expectation and interval-coverage oracles before Theme q-error measurements.

Capture representative structure-plus-estimate plans with the query-plan snapshot CLI and retain its logs. Always
include SOCIAL_MEDIA q4/q9, MEDICAL_RECORDS q9/q10, HIGHLY_CONNECTED q10, and LIBRARY q10 when their affected
contracts move. Run the isolated 117-query harness only after its preflight store manifest and `snapshot-only` policy
prove the store and adaptive sidecar cannot mutate.

Use the supported benchmark wrapper for focused measurements. Examples are:

    scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:9
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query MEDICAL_RECORDS:9
    scripts/run-single-benchmark.sh \
      --module core/queryalgebra/evaluation \
      --class org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.packed.PackedCascadesSearchBenchmark \
      --method plan --enable-jfr

Run and then resume the authenticated v95 plan-quality campaign with the exact frozen jar and budgets below. The
runner rejects stale jar digests, command settings, stable IDs, or result fingerprints before it retains prior work,
and each child process remains externally capped at 120 seconds:

    python3 scripts/run-theme-plan-quality-campaign.py \
      --jar /private/tmp/rdf4j-lmdb-frontier-v95-20260814-0108.jar \
      --store-root profiles/lmdb-opt/final-campaign/performance/store-baseline \
      --results profiles/lmdb-opt/final-campaign/performance/plan-quality-v95-all-p512m-c8g-20260814 \
      --summary profiles/lmdb-opt/final-campaign/performance/plan-quality-v95-all-p512m-c8g-20260814-summary.json \
      --timeout-seconds 120 --query-timeout-seconds 120 --dominated-samples 2 \
      --warmups 5 --measurements 20 --max-retained-bytes 8589934592 \
      --frontier-query-memory-bytes 536870912 --resume

Record JDK, hardware, store/dataset digest, evidence mode, JVM flags, forks, warmups, measurements, benchmark JSON,
allocation profile, and JFR path. Compare fixed-plan execution separately from preparation. Profile before changing a
hot path and retain only changes supported by both the semantic contract and matched evidence.

Before final verification, run the copyright check from `scripts`, then the repository formatter from the root:

    ./checkCopyrightPresent.sh

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter is the repository's explicit `-q` exception. If it remains silent beyond a bounded diagnostic run,
stop it, record that fact here, ensure no unrelated files changed, and use the full quick install plus
`git diff --check` as the conclusive build/whitespace gates. Finish every milestone with the root quick install,
focused and module tests, snapshots, catalog consistency tests, and a scoped working-tree audit.

## Validation and Acceptance

The complete plan is accepted only when every category below passes together.

Semantic acceptance requires authoritative result-bag equality for all 117 Theme queries and generated nested
compositions. Every rewrite and physical optimization has a catalog entry, proof regime, implementation link,
positive and negative tests, and stable rejection telemetry. The closed-world manifest classifies every research and
production entry; production registration cannot bypass the catalog; and memo installation rejects a rule result
whose runtime proof IDs do not cover its descriptor. No rule uses an estimate or learned belief as semantic authority.
Unsupported syntax and failure paths preserve semantics or fail explicitly; there is no silent fallback.

Representation acceptance requires lossless import/export of every supported operator and scalar form, immutable
structural identity, occurrence-specific metadata/facts, collision safety, bounded primitive arenas, and winner-only
`TupleExpr` materialization. UNION, OPTIONAL, compatible JOIN, aliases, extension, MINUS, EXISTS, and subquery scope
have explicit value-ID merge behavior, and generated tests prove that assured, unbound/error, equality, and range
facts do not cross an unsafe branch. Packed hot-state architecture tests reject object-tree and boxed steady-state
state in measured annotated hot methods; they do not forbid simple collections in unmeasured cold diagnostics. Cache
entries contain no live query/session resources.

Estimator acceptance requires every supported transition to preserve the richest usable Frontier state, tuple
pairing, multiplicity, guarantee, disposition, lineage, uncertainty, and exact zero status. Every scalar projection
is typed and explained. Exact and sampled oracle expectations, interval coverage, heavy-center exactness,
insert/delete generations, budgets, v1/v2 migration, restart, and OFF/snapshot-only modes pass.

Milestone 9 estimator acceptance additionally requires a counting source that fails if planning calls statement
count, statement iteration, or bound-variable distinct enumeration; products above `2^31`, `2^32`, and 20 billion
without proportional allocation; forced spill/pause with tiny governor leases; aggregate governed heap at most 2 GiB
under a builder and at least 64 planners; shard round trips plus truncation, checksum, partial-publication, crash,
stale-manifest, and lease replacement cases; mutation journal insert/delete/rollback/idempotent replay/restart/lag and
reserve-depletion cases; and exact-oracle properties for all 16 leaf bound masks and all 16 join-role pairs. Nested
FILTER, VALUES, UNION, OPTIONAL, MINUS, EXISTS/NOT EXISTS, subquery, grouping, context, and explicit/inferred cases
must remain semantically correct when statistics are absent or uncertain.

Milestone 9 shard-reader acceptance additionally requires zero retained metadata mappings after `open`, zero mapping
growth across repeated checksum failures, exactly five physical mappings for eight contiguous logical blocks after
adaptive promotion, one physical mapping per touched block for a gapped layout, and correct decoding for legacy and
revision-3 shards across widths 1 through 64, all four codecs, irregular block boundaries, the final packed value,
and concurrent first touch. The focused `FrontierStatisticsShardTest` must pass before the complete LMDB module gate.

Cost acceptance requires every physical dimension to have one unit, scope, uncertainty, and composition registry
entry. Independent oracles agree for sequential, concurrent, first-result, blocking, repeated-open, materialized,
cached, spill, peak-memory, and retained-memory trees. Algebra tests prove identity, monotonicity, interval endpoint
propagation, unknown handling, and associativity wherever the registry claims it. No operator or backend duplicates a
different composition formula outside `PackedCostAlgebra`.

Search acceptance requires exhaustive oracles to agree with DPhyp CSG/CMP pairs, legality, continuation classes,
Pareto survivors, property delivery, and final policy choice after the rewrite/fact/hypergraph/Cascades worklist reaches
a fixpoint. Dense, sparse-long, and multiword DPhyp paths agree at 16/17, 63/64/65, and 127/128/129. Every enabled
dominance has a monotonicity proof and adversarial suffix tests. No cap, beam, epsilon, deadline, weighted sum, or
resource limit is reported as an exact prune. Tiny work/time/byte budgets prove atomic failed growth, executable
incumbents, explicit incomplete statuses, no exact-cache publication, and explanations that identify unfinished work.

Learning acceptance requires commensurate completed observations, logical symmetry across equivalent physical
alternatives, isolation across incompatible applicability and algorithms, deterministic dependent work, independent
dimension NIG posteriors, interval-only censoring, exact-fact priority, bounded persistence, restart, snapshot-only
immutability, LastGood, quarantine, rollback, and period-two containment. Hard applicability and the empirical
99-percent conformal gate deterministically return conventional vectors for mismatch, insufficient support, and OOD
input. Held-out median q-error does not worsen, p95 plan regret is at most 10 percent, no completed query regresses
more than 25 percent against LastGood, interval coverage meets its declared level, and planning time/allocation add no
more than five percent. Promotion from Monitoring is optional; weakening a gate is not.

Cache and explanation acceptance requires zero provider work on strict hits, exact dependency replay for stale hits,
fail-closed invalidation, byte and entry bounds, collision safety, concurrent single-flight behavior, and complete
decision certificates. Human and structured explanation formats identify legal/rejected alternatives, state and cost
event digests, Pareto decisions, learned changes, cache validation, and final policy without recosting.

Corpus and performance acceptance uses a frozen manifest at
`profiles/lmdb-opt/final-campaign/baseline-manifest.json` containing the Git revision, JDK, JVM flags, hardware,
dataset/store digests, evidence mode, query digests, forks, warmups, measurements, and exact/incomplete planner mode.
Correctness oracles exhaustively enumerate graphs of two through nine factors. Synthetic planning tiers use 4, 8,
16, 17, 32, 64, 65, and 128 factors; the 117 Theme cells form the application workload. Exact and incomplete results
are reported separately. Large-query regret measures every final Pareto survivor plus a deterministic stratified
sample of rejected legal alternatives recorded in the manifest; it never claims an unmeasured global optimum.
Against that frozen baseline, both full-corpus uncached planning aggregates improve by at least tenfold, per-cell
p95/allocation targets pass, fixed-plan execution does not regress, plan-quality regret remains bounded, and
q-error/coverage/exactness are unchanged or better. Every statistically inconclusive or failing cell remains
explicitly open in `Progress` and the ledger.

## Idempotence and Recovery

Tests, snapshot capture, store preflight, benchmarks, and generated catalogs are repeatable. Preserve every untracked
artifact, initial evidence file, retained Maven log, prepared store, snapshot, benchmark result, and JFR recording.
Never use reset, restore, clean, stash, destructive checkout, or broad deletion. Reverse an incorrect edit with a
small `apply_patch` that touches only that edit.

All caches, indexes, learned sidecars, range facts, payloads, detached bundles, catalog schemas, continuation keys,
and cost vectors are versioned and fail closed. A stale or incompatible artifact causes deterministic rebuild or
conservative full replanning, never semantic guessing. The former migration-only whole-session scalar restart has
been removed: a non-resource Frontier session failure closes query-local state and propagates its typed failure,
while a deterministic resource ceiling may return only an already-complete executable incumbent with an explicit
incomplete status. No local or whole-session path may silently discard a supported state.

Resource-limit recovery is also idempotent. Every primitive arena reserves deterministic logical bytes before
capacity growth, so retrying with the same limits yields the same completion status and no partially published memo
row. A caller may retry an incomplete result with larger explicit limits, but the new run starts from a validated
detached checkpoint or from a fresh query-local arena; it never relabels the old certificate. Incomplete work,
deadline, resource, unsupported, and explicit-approximation recipes use distinct cache keys and cannot satisfy an
exact lookup.

If a behavior-changing production edit occurs before its failing test was observed, stop, revert only that edit with
`apply_patch`, restore the plan's sole in-progress item to the red-test step, and resume test-first. If offline Maven
resolution fails, rerun the exact command once without `-o`, then return offline. For other root-build failures, retry
without `-T 1C` and diagnose rather than weakening tests. An interrupted sidecar or cache migration leaves the old
version intact or rebuildable from the manifest.

A V2 publication is recoverable at every boundary. Temporary shard names are generation-scoped and ignored by
readers. The old manifest remains authoritative until every mandatory shard is fsynced and the new manifest is
atomically renamed and its directory fsynced. Startup validates the selected manifest structurally without
prefaulting blocks; a failure switches to the preceding valid manifest or conventional estimation and schedules a
background rebuild. Reference-counted leases delay physical cleanup. Journal replay starts after the manifest's
covered sequence, so retrying publication or restart cannot double-apply a mutation. No recovery path synchronously
scans statements from query construction.

Git history is the archive for removed ExecPlans. If a historical plan contains evidence needed to implement a
current milestone, summarize the relevant fact and path in this file; never restore the old plan as a second source
of instructions.

## Artifacts and Notes

`.agent/execplans/README.md` is a non-plan pointer to this sole ExecPlan and records the planning consolidation. The
tracked subordinate plans deleted on 2026-08-06 remain recoverable from Git. The repository-root
`research-task-learned-feedback-plan-flip.zip` is supporting research evidence, not an execution plan.

`papers2/papers/docs/neumann-birler-umbra-query-optimizer-research.md` is the focused research synthesis for join
enumeration, rich estimation, multidimensional costing, learned feedback, rewrite safety, and working-plan
representation. It cites canonical PDFs resolved through `papers2/papers/library/catalog/papers.jsonl`, records the
legacy full-text index caveat, and distinguishes reported paper results from RDF4J design inferences. Text and
rendered-page working evidence is retained under `papers2/papers/tmp/pdfs/optimizer-research/`.

The most important retained design evidence is under `profiles/lmdb-opt/theme-audit-2026-08-04/`:

- `packed-continuation-equivalence-frontier-design.md` defines the exact continuation counterexample and storage
  requirements;
- `dphyp-subset-kernel-exactness-and-performance-design.md` audits width-specific completeness and DPhyp waste;
- `planner-algorithm-invariant-audit.md` inventories single-winner and numeric heuristic gates;
- `rewrite-semantics-and-coverage-audit.md` records unsafe rewrites and the interaction matrix;
- `group-cardinality-root-cause-and-design.md` specifies exact/projected GROUP cardinality;
- `authoritative-theme-semantic-validation-workflow.md` defines result-bag authority; and
- `all-query-planning-performance-methodology.md` defines matched 117-cell measurement.

The 2026-08-06 pre-consolidation and post-cleanup root quick installs completed with `BUILD SUCCESS` in 40.321 and
34.947 seconds. The latest build output is retained in `maven-build.log`. Initial failures and later milestone
evidence use top-level `initial-evidence*.txt`, `logs/mvnf`, named `.mvnf/workspaces`, and the exact report paths
printed by the runner.

`initial-evidence.continuation-dp.txt` is the authoritative Milestone 4 red baseline. It records one test, one failure,
expected order `B -> A -> C` at cost 3, and actual order `A -> B -> C` at cost 102. Do not overwrite it. Milestone 2
generates `docs/query-optimizer/rewrite-and-optimization-catalog.md`; Milestone 6 writes chronological prototype
evidence under `profiles/lmdb-opt/leo-plus-prototype/`; Milestone 8 freezes its comparison inputs in
`profiles/lmdb-opt/final-campaign/baseline-manifest.json` before claiming speedup or regret.

The current Milestone 8 candidate is frozen as
`/private/tmp/rdf4j-lmdb-frontier-v95-20260814-0108.jar` with SHA-256
`450776ccb4fb44e703376ce7c4875dd48c663b7f71056835eaaad7dee9717c40`. Its focused MEDICAL_RECORDS q2 inventory
and all 13 authenticated fixed-alternative executions live under
`profiles/lmdb-opt/final-campaign/performance/plan-quality-v95-medical-q2-p512m-c8g-20260814`; the exact command,
stable IDs, common result-bag fingerprint, zero regret, focused tests, and no-fallback evidence are appended to
`initial-evidence.txt`. The v94 full inventory remains immutable historical evidence for all 117 post-continuation
searches; v95 supersedes it for cross-process execution identity and final regret.

`optimizer-rca-and-roadmap-2026-07-07.md`, `hypergraph-plan.md`, and
`papers2/papers/docs/sparql_query_rewrite_catalog.md` are historical/reference inputs with explicit non-governing
banners. They may supply diagnosis or literature, but this file decides architecture and execution order.

## Interfaces and Dependencies

Preserve these existing backend-neutral boundaries and evolve them rather than introducing a second planner:

- `PackedQueryCodec`, `PackedQuery`, `PackedMemo`, and the primitive side arenas own the working representation.
- `PackedCostSession` consumes `PackedEvidenceContext` and writes one reusable `PackedCostEstimate` plus immutable
  event/state IDs. LMDB implements the session; optimizer core knows no LMDB types.
- `FrontierStateArena` owns live state, `EvidenceStateRef` names it, and `FrontierEvidenceBundle` detaches it.
- `PackedPredicateRangeProvider` writes into reusable `PackedPredicateRange`; `PackedDomainFacts` propagates facts.
- `PackedDphypEnumerator` emits topology to a receiver; it does not store costs or winners.
- `PackedPlanCache`, `PackedPlanRecipe`, and `PackedPlanMaterializer` remain the store cache, detached plan, and public
  boundary respectively.

Milestone 9 adds one LMDB-owned service and keeps mapped storage types out of optimizer core. The normative shape is:

    interface LmdbStatisticsService extends AutoCloseable {
        FrontierStatisticsLease acquire(long requiredEpoch);
        FrontierStatisticsStatus status();
        void recordEffectiveMutation(FrontierMutation mutation);
        void requestBackgroundBuild();
    }

    interface FrontierStatisticsLease extends AutoCloseable {
        boolean ready();
        FrontierFallbackReason fallbackReason();
        FrontierLeafEstimate estimateLeaf(FrontierLeafProbe probe, FrontierQueryScratch scratch);
        FrontierDistinctEstimate estimateProjectedDistinct(
                FrontierLeafProbe probe, int component, FrontierQueryScratch scratch);
        FrontierJoinEstimate estimateJoin(FrontierJoinProbe probe, FrontierQueryScratch scratch);
    }

    enum FrontierFallbackReason {
        NONE, NO_GENERATION, GENERATION_TOO_STALE, SHARD_CORRUPT,
        UNSUPPORTED_QUERY_SHAPE, CONFIDENCE_TOO_WIDE, MEMORY_PRESSURE
    }

`FrontierStatisticsStatus` reports availability; build phase and pass; base and covered epochs; lag; disk bytes by
tier; heap leases by purpose; scan rate and ETA; delete debt; audit q-error; and one structured last failure.
`FrontierStatisticsHeapGovernor` reserves bytes before allocation and returns closeable leases for metadata, query,
builder, compactor, delta, and safety purposes. `FrontierStatisticsManifest`, `FrontierStatisticsShardDescriptor`,
`FrontierStatisticsShard`, and `FrontierStatisticsGenerationLease` own the versioned disk format and lifetime.
Mapped readers expose bounded bulk operations into caller-owned primitive scratch; they never return a row object or
an iterator whose lifetime outlives the generation lease.

The initial mandatory shard tiers are exact/heavy/distinct summaries, Omni leaf witnesses/postings, coordinated join
samples, and signed linear sketches. Conditional refinements are optional. Every estimate carries point, lower and
upper rows, guarantee, support/effective sample size, base/covered epoch, estimator kind, and typed fallback reason.
Compatibility facades translate that typed estimate into existing `EvidenceStateSummary` and `BagEstimate` surfaces
without restoring query-local statement payloads.

Milestone 1 adds or evolves `PackedBindingFlowArena` and `PackedPathFactIndex`. A value ID is a primitive `int` owned
by the first arena. Its mutation surface is equivalent to:

    int addProducedValue(int expressionId, int variableId, int valueFlags);
    int addMerge(byte mergeKind, int variableId, int inputOffset, int inputCount, int factFlags, int proofSetId);
    void addConsumer(int valueId, int expressionId, byte useKind);

The arena stores producers, consumers, merge kind, merge inputs, and possible/assured/conditional/unbound/error flags
in primitive arrays. `PackedPathFactIndex` answers producer/root/LCA, merge-path, predicate placement,
equality/range, scope/OPTIONAL barrier, and minimum-cardinality queries by value ID. Start with a static query-local
index and measured rebuilds; a dynamic link/cut-tree implementation is permitted only after a benchmark and
allocation profile justify its complexity. These names are normative unless Milestone 0 finds an existing type with
the complete contract and records the substitution.

Milestone 2 adds `OptimizerRuleCatalog`, immutable `OptimizerRuleDescriptor`, reusable `RuleApplicabilityResult`, and
one production rule interface equivalent to:

    interface PackedOptimizerRule {
        int descriptorId();
        void evaluate(PackedRuleContext context, int expressionId, RuleApplicabilityResult output);
    }

`RuleApplicabilityResult` stores status, descriptor ID, proof-set ID, reason ID, and affected-fact mask without
free-form parsing. The memo installation route is equivalent to
`installRuleAlternative(int sourceExpressionId, int alternativeExpressionId, RuleApplicabilityResult result)` and
rejects non-applicable, mismatched, or under-proved results. Every logical rule, physical implementation, access path,
enforcer, search optimization, research proposal, unsupported case, and deliberate non-rule has one descriptor.
Safety conditions use enums/bitsets and stable IDs. A generator emits
`docs/query-optimizer/rewrite-and-optimization-catalog.md`; tests compare the manifest, production registrations,
runtime proof IDs, and generated output. Do not add a YAML/JSON parsing dependency merely for the catalog.

Milestone 3 adds or evolves `PackedCostDimensionRegistry`, `PackedCostAlgebra`, and `PackedCostVectorArena`.
`PackedCostDimensionRegistry` is the sole mapping from stable dimension ID to unit, local/inclusive scope,
uncertainty kind, and legal composition modes. `PackedCostAlgebra` composes local and child vector IDs for a declared
mode and invocation/lifetime descriptor. `PackedCostVectorArena` stores immutable lower/point/upper values,
unknownness, and lineage IDs in parallel primitive arrays. It imports only physical-resource fields from
`PackedCostEstimate`, leaves semantic rows/distributions in the evidence state, and never converts the vector to one
objective.

Milestone 4 adds or evolves the following package-private primitive abstractions:

- `PackedContinuationKeyArena` interns collision-safe continuation identity: goal/properties, input and semantic
  scopes, binding layout, evidence identity/disposition/guarantee, learning applicability, and composition mode.
- `PackedParetoFrontierArena` maps a planning cell and continuation key to one or more candidate IDs. Its `offer`
  operation returns inserted, duplicate, dominated, or replaced-by-proof; it never accepts an arbitrary size cap.
- `PackedCandidateArena` (or an exact extension of `PackedPhysicalMetadataArena`) stores expression, children, state,
  cost-vector, continuation-key, proof, trace, and lifecycle IDs for every retained candidate.
- `PackedPlannerLimits` stores work, deadline, and retained-byte limits. `PackedSearchBudget` exposes atomic
  `tryReserveWork(long)` and `tryReserveBytes(long)` operations plus a `PackedSearchCompletionStatus` of
  `EXACT_COMPLETE`, `INCOMPLETE_WORK_LIMIT`, `INCOMPLETE_DEADLINE`, `INCOMPLETE_RESOURCE_LIMIT`, or
  `UNSUPPORTED_SEMANTICS`.

`PackedWinnerTable` becomes a final-policy/root-selection index over retained candidate IDs. It may cache one chosen
candidate per explicit `costPolicyId`, but it is no longer the only owner of a subproblem alternative. Dense,
`PackedLongSubsetTable`, multiword, correlated, and completed-lattice search all reference the same candidate/frontier
arenas.

Milestone 5 adds `PackedNodeSetArena` and `PackedDphypReceiver`. The arena interns arbitrary-width node sets in
contiguous words and exposes primitive set operations by integer ID. The receiver contract is equivalent to:

    boolean hasSeen(int nodeSetId);
    boolean foundSingleNode(int node);
    boolean foundSubgraphPair(int leftNodeSetId, int rightNodeSetId, int edgeId);

`PackedDphypEnumerator.enumerate(PackedJoinHypergraph, PackedNodeSetArena, PackedDphypReceiver)` dispatches to the
one-word or wide kernel but exposes identical pair semantics. `PackedJoinHypergraph` stores node-set IDs for edge
endpoints and may cache raw one-word masks only as an internal at-most-64 fast path.

The learned boundary retains `LogicalLearningKey`, `LearningApplicability`, `PhysicalResidualKey`,
`RuntimeFeedbackDescriptor`, `RuntimeFeedbackContract`, `RuntimeFeedbackTarget`, and `InvocationAggregateView`.
Milestone 6 adds primitive `LearningFeatureEnvelope`, `CensoredObservationBounds`, and `LearningGateDecision` values
with `APPLICABLE`, `INAPPLICABLE`, `INSUFFICIENT_SUPPORT`, and `OUT_OF_DISTRIBUTION` outcomes. `PackedCostEstimate`,
metadata, recipes, and materialization carry the opaque runtime contract without string conversion. Per-result
execution remains allocation-free. Persistence continues to read supported older versions and writes one current
format; any semantic change bumps the appropriate wire version and tests migration/quarantine.

The fixed dependency and reactivation flow is:

    packed working algebra + value-flow/path index
        -> proven facts + proof-coupled local rewrite closure
        -> typed join hypergraph revision + one-word/wide DPhyp topology
        -> Cascades logical/physical candidate installation
        -> raw evidence + scoped learned residuals
        -> versioned semantic estimate state + registered cost algebra
        -> immutable multidimensional cost event
        -> continuation-equivalence class + resource-accounted Pareto frontier
        -> dependent-parent and affected-rewrite reactivation
        -> repeat until the worklist certificate is EXACT_COMPLETE
        -> final policy selection
        -> detached recipe + selected TupleExpr

The optimizer core must not depend on LMDB classes. LMDB supplies implementations for state materialization, range
facts, access costs, cache validation, and learned persistence through backend-neutral typed interfaces. Use only the
JDK and dependencies already present unless a later plan revision records the required dependency health check and
explicit approval.

Plan revision note (2026-08-06 / Håvard and Codex): consolidated the packed planner, Frontier full-potential,
state-continuity/cache/LEO, learned-feedback rollout, and learned-plan-flip research plans into this sole ExecPlan.
The revision preserves every unresolved obligation, reconciles obsolete intermediate versions with the current
source tree, defines one dependency order and one validation contract, and deletes the competing plan surfaces.

Plan revision note (2026-08-06 / Håvard and Codex): incorporated the canonical `papers2` research focused on Thomas
Neumann, Altan Birler, and Umbra. The revision narrows CD-E completeness to its proved predicate regime, makes
predicate-rewrite closure part of search certification, adds an Indexed-Algebra-style binding-flow index, expands
Frontier payload and physical cost obligations, and strengthens LEO-plus with censoring, invariants, drift/OOD
fallback, and guarded GroupJoin/mark-join alternatives.

Plan revision note (2026-08-06 / Håvard and Codex): repaired the post-consolidation review findings so this file is
executable without hidden design choices. The revision moves proof-coupled semantic/range closure ahead of join
enumeration and defines the rewrite/hypergraph/Cascades fixpoint; replaces variable-name source identity with
SSA-like value IDs and merge nodes; specifies one-word and multiword DPhyp; adds retained-byte admission and explicit
incomplete statuses; defines the cost algebra and interval dominance; couples catalog descriptors to runtime proof
results; selects the hierarchical-NIG plus conformal-OOD LEO policy; freezes workload/performance evidence contracts;
and replaces placeholder or stale commands with exact current or prescribed selectors.

Plan revision note (2026-08-13 / Håvard and Codex): recorded the v77 sampled-miss, exact-recovery, physical-join
lineage, memo-only worklist, and learning-feature continuity repairs, together with their focused and complete
134-test evidence. The revision explicitly keeps the timing and corpus gates open while unrelated JMH and storage
work contaminate the host; it does not reinterpret that external constraint as permission to suppress a candidate or
replace Frontier state with scalar costing.

Plan revision note (2026-08-13 / Håvard and Codex): recorded the v81-v94 exact-continuation, complete-vector,
metric-sharing, typed-resource, aggregate-wrapper, exact-zero, and mask-ordering repairs; froze the v94 jar and its
117-cell inventory audit; and advanced Milestone 8 to authenticated alternative execution. The revision keeps the
regret, fixed-execution, absolute planning, and tenfold aggregate gates open and explicitly rejects size-, shape-,
query-, budget-, or benchmark-specific heuristics.

Plan revision note (2026-08-14 / Håvard and Codex): repaired cross-process executable identity with the existing
lossless alpha-equivalent packed representation, made typed evidence realization atomic, froze v95, and proved all
13 MEDICAL_RECORDS q2 alternatives authenticate and return one result bag with zero measured regret. The full v95
inventory then reproduced 114 exact completions, three typed resource limits, global-minimum policy selections, and
190,107 intact Frontier event states across all 117 cells. The revision adds no query-shape, size, cost, or benchmark
heuristic and keeps the full execution-regret and performance gates open.

Plan revision note (2026-08-14 / Håvard and Codex): the authenticated v95 execution campaign isolated q3 drift to
literal UUID-backed anonymous binding names in otherwise deterministic Frontier resampling seeds, despite identical
durable synopsis bytes. Seed schedule v2 alpha-normalizes only the sampling identity to ordered binding slots and
mask shape; exact names remain in runtime state equality and lookup. Frozen v96 reproduces every q3 inventory field
across fresh JVMs and authenticates all 16 execution alternatives with one common result bag. The revision introduces
no plan-selection heuristic and keeps saturated regret and the remaining performance gates open.

Plan revision note (2026-08-14 / Håvard and Codex): added Milestone 9, Frontier Statistics V2, to eliminate the
split raw-payload/query-index readiness model, query-time exact LMDB replay, and Cartesian payload materialization.
The revision records the 20-billion-triple heap, disk, build, latency, freshness, and accuracy equations; selects a
query-ready sharded generation, two-pass bounded build, Omni/coordinated-sample/Fast-AGMS ensemble, transactional
signed deltas, and online migration; and defines six test-first slices. Milestone 8's frozen evidence is preserved
while its remaining timing campaign pauses for a safe, comparable V2 estimator source.

Plan revision note (2026-08-15 / Codex): completed the bounded M9.4 witness-maintenance path. Every new journal range
now persists aligned Omni insertions/tombstones beside signed CountSketch, AGMS, and mutation summaries; mapped
queries merge exact tuple transitions without statement replay. The builder derives deletion reserve rows from a
per-shard KL union bound, and adjacent equal-size range families compact through mapped sequential streams with
logarithmic fan-in. Regressions cover 65 independent commits, restart, layers larger than one raw 16,384-mutation
batch, cancelling delete/reinsert histories, reserve exhaustion, and signed-only migration prefixes. Held-out audit
promotion, asynchronous retention cleanup, and physical M9.5 scale/JMH/JFR qualification remain open.

Plan revision note (2026-08-15 / Codex): completed the V2 planner-continuity boundary. A query-local mapped learning
session now owns exact-fact and calibrated logical/physical residual application without constructing the legacy
payload or derivative index; contextual append events may train their access cost but cannot rewrite a complete
prefix with a mismatched logical origin. A mapped semi/anti coster now prices raw MINUS and typed streaming,
memoized, and materialized EXISTS/NOT EXISTS alternatives from Omni projected-distinct and mapped relation evidence.
Detached learned state remains typed, serializable planning retains the pinned generation, legacy heap settings map
to a feasible bounded build profile, and all products stay outside 32-bit candidate-count arithmetic. Physical scale,
cold-NVMe, held-out accuracy, and adaptive-promotion gates remain open.

Plan revision note (2026-08-16 / Codex): compared the attached Java 21 quad-composable Omni reference with the
disk-resident V2 implementation. The revision adopts independent-lane quality metrics and progressive mapped
bottom-K prefixes, records why heap-loaded catalogs and quadratic object joins are rejected, and propagates actual
shared-witness support through leaf, star, path, cycle, and bridge intervals. The 20-billion-statement resource and
qualification gates are unchanged; physical scale and held-out shadow accuracy remain open.

Plan revision note (2026-08-16 / Codex): closed the in-repository warm M9.5 microqualification and its profile-driven
hot-path repairs. Deterministic heavy dimensions and direct AGMS streaming put the 20B builder under its 1.25 GiB
lease. Primitive sampling lineage removes the dominant allocation site, and immutable open-addressed column
directories remove the dominant non-decoder CPU site. Warm three-pattern star/path average time is below 20 ms; the
final path recording is 10.771 +/- 0.063 ms/op with 150 sampled allocations. This does not substitute for the p95
gate. Cold NVMe, held-out accuracy, background interference, and physical 100M through 1B scale remain explicitly
open.

Plan revision note (2026-08-16 / Codex): repaired the logical-to-physical V2 cost dependency exposed by sparse-prefix
qualification. Mapped Omni evidence was already returning the correct logical join multiplicity, but the
independent-hash alternative retained Cartesian candidate work computed by the scalar delegate before mapped state
restoration. The V2 session now reprices candidate, orientation, probe, peak-memory, and telemetry fields together;
an assured lookup key covering the complete compatibility mask uses logical match multiplicity, while incomplete
masks remain conservative. The focused red/green and the 61-, 166-, and 226-test suites pass. Physical scale,
cold-NVMe, held-out accuracy, background interference, and migration-retention gates remain open.

Plan revision note (2026-08-16 / Codex): completed bounded typed-value support probes and replaced a stale
streaming-era highly-connected q10 order assertion with the physical invariant it intended to protect. Numeric and
calendar aliases are evaluator-validated, capped at 16 candidates per value and 1,024 combined bindings, and remain
non-exact with a `[0,1]` semantic interval. Exact exhaustive memo search confirms the selected four-value anchor uses
one bounded materialized anti RHS and four mapped Omni prefix probes. Focused temporal, TRAIN, and flagged-q10 tests
pass. The first complete module rerun exposed two independent contract edges: bounded AUTO may legitimately finish
before exhausting its 512-unit ceiling, and finite-bound self-loop equality belongs to the leaf rather than the
unbound join-variable classes. Focused reds and greens cover both. The final LMDB module gate passes 2,247 tests with
zero failures or errors (114 skipped). Physical scale, p95/cold, held-out accuracy, background interference, and
migration-retention gates remain open.

Plan revision note (2026-08-18 / Håvard and Codex): completed the complete-store MEDICAL q9 projected-distinct repair.
The revision selects the equal-weight arithmetic mean for complete inverse-probability lane ensembles, rejects
normal-distribution fitting and zero-triggered center switching, and requires an independent structural
correlation-domain upper bound to govern memoized semi/anti cache eligibility. It records the sparse and all-positive
lane contracts, exact-projection rules, uncertain-domain costing coverage, all-theme q9 execution gate, JDK 26
benchmark/JFR evidence, and the newly isolated saturated distinct-telemetry follow-up without changing public APIs,
statistics formats, cache capacity, or query semantics.

Plan revision note (2026-08-18 / Håvard and Codex): completed the saturated distinct-telemetry follow-up. The revision
replaces the capacity-length exact tracker with a collision-safe bounded exact/HLL collector, qualifies approximate
feedback through the typed runtime path and version-22 sidecar, batches node telemetry until root close, samples only
per-call timing, removes materialized probe-key allocation, and caches normalized registry keys. JDK 26 q9 improves
from 359.656 to 123.052 ms/op with telemetry disabled, records 126.095 ms/op with sampled-full telemetry, and records
129.915 ms/op in the final sampled-full JFR run. Core telemetry totals 1.36 percent of measurement-window Java
top-frame samples, or 6.69 percent including combined iterator instrumentation, with no attributed per-row
allocation. Focused tests, the final 1,446-test query-evaluation suite, the fresh 117-query result-bag audit, and every
telemetry-related test in the 2,230-test LMDB run pass. The benchmark-only sampled-full comparison preserves plan
fingerprint and result and adds 2.47 percent over the disabled fixed plan. The sole LMDB module failure remains the
unrelated dirty-worktree `SPARSE` benchmark-default assertion.

Plan revision note (2026-08-20 / Håvard and Codex): started the Milestone 9 shard-reader mapping and decode slice from
the supplied candidate, correctness verifier, adaptive-layout verifier, and three focused probes. The revision makes
physical mapping lifetime the test-first contract, keeps logical first-touch accounting and both disk formats stable,
uses portable JDK mapped-buffer evidence in repository tests, and records the metadata, checksum-failure, contiguous,
gapped, tail, codec, concurrency, and hot-path acceptance obligations before production changes.

Plan revision note (2026-08-20 / Håvard and Codex): completed the shard-reader slice on Temurin 26. Metadata uses
bounded heap reads or explicitly cleaned temporary mappings, checksum failures clean dedicated mappings, a sealed
registry makes close/first-touch publication race-safe, and contiguous layouts promote from four dedicated mappings
to one shared window. Regular and fenced irregular lookup plus one-word little-endian decode provide repeatable
cached gains across widths 1 through 64 and all codecs. The final 12-test shard class and supplied semantic verifier
pass. The 2,237-test module run leaves the shard green but exposes five separate telemetry-overlap errors and the
known dirty `SPARSE` default assertion. The five telemetry methods reproduce in a focused five-test rerun at
`JoinMetricsTracking$DeferredSideIteration.bind` with no shard-reader frame, so the broad gate remains independently
red rather than being classified as an ordering flake. Attached-harness evidence records a roughly nine-percent open
improvement, about ten-percent higher open allocation, and a 43-to-46-percent isolated first-touch regression;
cold-p95 remains open. The root formatter was stopped after nine CPU-active minutes because preserved `.mvnf` trees make the
repository-wide invocation pathological; the scoped LMDB formatter, repeated root quick installs, and
`git diff --check` pass without unrelated tracked edits.

## 2026-11-06 unified physical costing implementation plan


### Purpose, scope, and current status


Make RDF4J choose the execution that is expected to finish the requested query fastest, including optimization and
preparation costs. A rewrite that reduces estimated intermediate rows must compete with an equivalent expression
that enables an adjacency count, a factorized aggregate, an ordered stream, or a fused native kernel. The selected
implementation must survive lowering into execution; a planner explanation must identify what was selected, what
actually ran, and why any substitution occurred.

Optimization must remain economical. Small queries and valid cached plans should need little planning work. Large
queries may justify deeper search, but every optional recognition, estimation, refinement, and cache-validation
operation must participate in the same resource accounting. A bounded search must return a complete executable plan
and an honest completion status even when it cannot finish exploring alternatives.

This is an addition to the sole optimizer roadmap, not a replacement implementation or a separate project. Its
source baseline is `GH-0000-lmdb-predicate-guarantees` at `ae211fc3869a3a5a7388ff9e0a4793e0bf78396a` in
`/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Before future implementation, recheck the branch,
working tree, and named source owners; extend changed abstractions rather than restoring a historical design.
Implementation and execution are authorized. The implementation began at `d69e9dd27a`; current merged HEAD is
`2007a9fdbb83eed79047d4cec0696c49ec1c20f8`, with the partially implemented work restored uncommitted. The source
owners and evidence below are updated for the merge. All phase exit gates remain open until their full contracts and
evidence are recorded here. Preserve the one active progress item and existing untracked evidence throughout work.

The scope includes Cascades search, native strategy recognition and execution, ordinary LMDB indexes, adjacency
access, consumer-sensitive costing, plan caching, and feedback attribution. It preserves SPARQL semantics and the
existing query-ready Frontier Statistics V2, quad, and cold-filter evidence sources. It does not require a new
storage format, new statistics engine, new external dependency, or a second optimizer. Any later discovery requiring
such a change must be justified here before broadening the implementation.

### Reconciliation of upstream work and remaining contracts

Upstream already supplies native strategy-local feature counts, incomplete-work evidence, infinite upper bounds,
static price priors, candidate offers, selector explanations, distinct-demand handling, and bind-time decline/retry.
Preserve `LmdbNativeStrategyProposal.finishQuote`'s unknown ceiling and non-double-counted fallback expectation, and
the native code-generation contract in which a cached shape is independent of store IDs and compiled binding may
still decline to an interpreter. `LmdbNativeAccessFacts` supplies useful prefix and output evidence, but it does not
enumerate every packed index/adjacency alternative or prove that a packed choice is what execution opens.

The restored local work already carries unknown/risk-aware packed costs, dependent composition, shared planning
budget/session admission, bounded fallback and incomplete-cache status, typed LMDB access evidence, named-index/range
execution work, and a candidate admitted mapped-view lifetime repair. Those changes are partial and uncommitted.
Current native selection still reranks in row/group execution; `PackedPlanRecipe` has no native executable strategy
descriptor, native prices are separate from `LmdbPhysicalCostObjective`, and native admission is not charged through
`PackedSearchBudget`. Cache reuse, generated/interpreted fallback identity, and feedback attribution must therefore
remain explicit integration work. Do not infer an integrated recipe or a physical proof from the selected packed
logical plan, a native-local learned interval, or a best-prefix estimate.

The merge preserves filter ownership across declined, replayed, selected, failed, and parallel worker paths; it also
keeps native group-pushdown distinct from serializable observation ordering and includes native enablement in the
pipeline cache signature. Future strategy integration must preserve these contracts under nested execution,
cancellation, and cleanup failure. Native dominance declarations that rest on measured cases require real
substitutability/work proofs or must remain candidates for ordinary price comparison.

### Source orientation and existing foundations


The core planner package is
[`optimizer.cascades.packed`](/Users/havardottestad/Documents/Programming/rdf4j-small-things/core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed).
Throughout this section, a `Packed...` class belongs to that directory unless stated otherwise. `PackedCascadesPlanner`
searches equivalent expressions in a memo, a collection that shares logically equivalent subexpressions. It uses
integer identifiers and primitive arenas rather than allocating a separate object tree for every candidate.
`PackedPhysicalPropertyInterner` and `PackedPhysicalMetadataArena` hold physical distinctions. `PackedPlanRecipe`,
`PackedPlanMaterializer`, `PackedCostingReplay`, `PackedPlanContinuationSeed`, and `PackedPlanCache` already provide
selection, detachment, replay, and reuse machinery. Extend these mechanisms.

[`PackedCostDimensionRegistry.java`](/Users/havardottestad/Documents/Programming/rdf4j-small-things/core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed/PackedCostDimensionRegistry.java)
already declares 41 stable dimensions, including source and result row events, seeks, page walks, storage bytes,
memory, startup, first-result and final-result latency, compilation, dispatch, first-match, exhaustion, and rebind
work. `PackedCostAlgebra` already distinguishes sequential, blocking, parallel, and repeated invocation composition.
`PackedCostVectorArena` records which quantities are known. `PackedCostSession` owns query-local estimator state;
its default objective is `workRows`, and exact continuation identity is an equivalence proof for future costing,
not merely a hash. `PackedPlannerLimits`, `PackedProviderMemory`, and provider identity/retention accounting already
exist. Build on them instead of adding disconnected cost vectors, caches, or budgets.

The storage and LMDB planner package is
[`org.eclipse.rdf4j.sail.lmdb`](/Users/havardottestad/Documents/Programming/rdf4j-small-things/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb).
`LmdbCascadesOptimizer`, `LmdbQueryOptimizerPipeline`, `LmdbPackedCostModel`, and
`LmdbFrontierStatisticsCostSession` connect search to logical and physical estimates. `TripleStore.describePackedAccessPath`
describes a configured LMDB index path. `LmdbSailStore` contains statement access and work estimates;
`LmdbStatementAccessArbiter` chooses between statement-access implementations. `LmdbDirectAdjacencyStore`,
`LmdbAdjacencyReadView`, `LmdbAdjacencyPlannerEvidence`, and `LmdbFrontierReadSelection` provide adjacency access and
snapshot-qualified evidence. Adjacency here is a derived index with an immutable base and transactional overlays;
its existence alone does not prove coverage for a particular query snapshot.

The native package is
[`org.eclipse.rdf4j.sail.lmdb.evaluation`](/Users/havardottestad/Documents/Programming/rdf4j-small-things/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation).
`LmdbNativeAggregatePatternCompiler` and `LmdbNativePatternPlan` translate query expressions and describe access.
`LmdbNativeRowStep` and `LmdbNativeGroupStep` assemble strategy choices. `LmdbNativeStrategyProposal` already defers
opening resources, while `NativeAggregatePhysicalPlan` separates producer quoting from consumption. Reuse their
ownership rules. `NativePhysicalInputRecipe` records semantic input correspondence. Native-local pricing now uses
`LmdbNativeCostVector`, `LmdbNativeCostQuote`, `LmdbNativeCostVectors`, `NativePhysicalWorkEvidence`, and
`LmdbNativeStaticPrior`; `LmdbNativeOffers`, `LmdbNativeSelector`, and `LmdbNativeDispatchLoop` admit, rank, and open
the choices. `LmdbNativeAdvisoryLearner` is optional and uses a coarse native strategy/variant/access key. The prior
`LmdbNativePhysicalVariantKey`, `LmdbNativeCostEstimate`, adaptive/machine/store model, and native arbiter hierarchy
were retired upstream; do not restore them. `LmdbPhysicalCostObjective` in the LMDB package supplies another pricing
boundary that must become consistent with the shared policy.

These sources show integration gaps, not an absence of physical costing. Some packed access estimates still derive
rows visited from estimated rows returned; describing a B-tree prefix does not fix that estimate. Native strategy
recognition can happen after logical selection, and native and packed decisions do not universally use one objective
or one selected executable recipe. Source inspection alone does not establish the size of any resulting slowdown.

### Current implementation inventory (reconciled after upstream merge)


The request-bound optimizers in `LmdbQueryOptimizerPipeline` run before `LmdbCascadesOptimizer` selects and installs a
packed plan. `PackedPlanMaterializer` emits existing physical metadata and join hints, after which
`LmdbNativeEvaluationStrategy` and `LmdbNativeAggregatePlanner` compile the selected algebra anew. This separation
also applies when a generic enclosing operator contains native children; a root-only descriptor would not cover them.

For a statement pattern, `TripleStore.describePackedAccessPath` records the preferred snapshot-local index, its
covered/missing field masks, and access mode. `LmdbPackedCostModel` supplies the current rows and
invocation estimates and turns them into source, seek, open, and result events. Native pattern compilation has its
own index/order and work evidence, while `LmdbSailStore.statementsArbitrated` independently chooses LMDB or a
snapshot-covered adjacency source and reports a runtime source/variant key through the observer. Index availability,
contexts, inferred/explicit planes, ranges, and the actual reader snapshot constrain all three routes. This inventory
does not yet establish equivalent quotes or an enforceable selected index.

For aggregate producers, `NativeGroupStep` and `NativeGroupIteration.offerCandidates` recognize indexed count,
adjacency aggregate, type matrix, existence intersection, ordered/prefix-run grouping, wildcard reduction, factorized
tail, packed factor tree, leapfrog, kernels, and parallel implementations under their own capability checks.
`NativeAggregateCostQuotes`, family feature quotes, `LmdbNativeStaticPrior`, and `LmdbNativeSelector` price and rank
them; selected proposals bind or open execution-time resources and can decline. `NativePhysicalInputRecipe` captures
only currently supported input forms. `LmdbNativeAdvisoryLearner` can optionally refine native-local prices using its
coarse strategy/variant/index/access key, which is not a packed recipe identity. The native aggregate binder and
quote are not yet the packed planner's selected descriptor.

For row producers, `NativeRowsStep.offerUnorderedStrategies` and the corresponding ordered path admit prefix/run,
ordered distinct, leapfrog, packed/factorized, batch, interpreted/generated kernels, parallel execution, adaptive
filter placement, and nested execution. Ordering, duplicate handling, retained slots, binding scope, and replay-safe
evaluation constrain the offers. `LmdbNativeInteriorPlans` selects native children for compatible consumers. The
current dispatch can rerank these offers and attribute native-local feedback to the chosen variant without any packed
candidate naming that variant first. Hash/merge and path opportunities also have their own implementation-specific
eligibility and estimation; their complete recognition-to-bind mapping remains to be audited before phase 5 closes.

This is a working inventory, not a claim that each family's eligibility or every fallback path is fully cataloged.
Finish the family-specific restrictions, physical properties, quote provenance, binder ownership, and feedback keys
as each integration slice is exercised; the disabled priority test does not close that gap.

### The target decision and execution contract


A physical alternative consists of a logical expression, an executable implementation recipe, required input
properties, delivered output properties, evidence assumptions, resource requirements, and a typed cost estimate.
The recipe names the algorithm and its access paths. It is not a scalar discount attached to a logical node.
A region implementation may cover several logical operators at once, such as a factorized join followed by COUNT.
Its internal work must not also be charged as separately executed child operators.

Use a three-stage provider lifecycle. **Recognize** inspects the packed expression, binding information, and requested
output to report eligible strategies. **Quote** estimates the physical work for each strategy using already available
or explicitly budgeted evidence. **Bind** validates and prepares only the selected recipe against the execution
snapshot. Recognition must not open data cursors, enumerate rows, generate code, build hash tables, or reserve
workers. Quoting may perform bounded metadata reads with explicit charges, but must not run the candidate query to
discover its price. Existing methods named `prepare` must be audited before reuse because their names do not prove
that they are cheap or free of storage access.

Keep logical cardinality separate from physical work. A query can produce one result after reading a million index
entries, or compute a million-result multiplicity from a small number of factor records. Represent access rows,
emitted rows, distinct probe keys, invocations, range entries skipped, decoded values, bytes, and consumer work
separately when they differ. Preserve exact facts, statistical estimates with uncertainty, and unavailable evidence
as distinct states. Unknown work is not zero. A sampled zero cannot prove an empty relation. A statistical confidence
interval cannot authorize semantic pruning or be presented as a deterministic work bound.

The final objective is expected latency for the requested consumption under the declared resource policy, with
first-result and complete-result objectives available explicitly. Total user latency includes planning, selected
preparation, execution, and result conversion. Already-spent planning time is common to all remaining alternatives
and must not be added repeatedly to candidate scores. The decision to spend more time optimizing compares the
expected benefit of additional information with its additional planning cost. Compilation or reusable preparation
can be amortized only over reuse justified by an actual cache contract; do not invent future executions to make a
plan appear cheap.

Translate physical features into comparable estimated time through a shared versioned pricing policy. Keep memory
as a feasibility and composition property rather than adding raw bytes to nanoseconds. Charge materialization and
value decoding at the boundary where they occur. Sum actual work, compose parallel elapsed time according to the
critical path and available capacity, and compose memory according to overlapping lifetimes. Preserve startup for
blocking operators under LIMIT. Existence requires separate estimates for finding a match and exhausting an empty
probe; reducing all work by a result fraction is not sufficient.

Preserve evidence lineage when composing uncertainty. Estimates derived from the same samples or correlated binding
domains are not independent observations. Summing individual percentile predictions does not establish a percentile
for total query latency; measured end-to-end distributions remain a separate validation obligation.

Keep a valid incumbent, meaning the best complete executable plan currently available. Begin with the existing safe
execution route, then improve it. Exhausting an optimization budget must preserve that route. Exact search may claim
completion only after all work in its declared supported rewrite and physical-provider space is complete. Completion
does not mean estimates equal actual runtime or that every conceivable algorithm has been searched.

### Interfaces and dependency direction


The query-evaluation module owns backend-neutral request, property, budget, recipe, and cost contracts. It must not
depend on LMDB classes. The LMDB module supplies the storage/native provider and recipe binder. Preserve the
functional-interface compatibility of `PackedCostModel`; use optional defaults on its session or an internal
extension rather than breaking scalar providers.

Keep packed arena internals encapsulated. `PackedCostDimensionRegistry` is currently package-private; expose the
necessary typed mapping through the backend-neutral provider/cost sink contract, rather than making LMDB reach into
private arena implementation or duplicating stable IDs and units in an unrelated schema.

Introduce `PackedPhysicalStrategyProvider`, `PackedPhysicalStrategyRequest`, and `PackedPhysicalCandidateSink` in
the core packed package only where the current physical refinement interfaces cannot express region implementations.
The request is a borrowed query-local view: logical relation/region identity, known and possible binding masks,
consumer requirement identifier, evidence-state identifier, required properties, and the current budget owner.
The sink interns compact candidates in existing arenas. The intended provider operations are:

    void recognize(PackedPhysicalStrategyRequest request, PackedPhysicalCandidateSink sink);
    void quote(int candidateId, PackedPhysicalStrategyRequest request, PackedCostEstimate output);

Provide the optional session entry point `physicalStrategies()` with a no-strategy default. Recognition and quotation
report unsupported, unavailable, and budget-exhausted outcomes explicitly through the request/sink contract; these
must not all become an infinite or zero scalar price. The names above are proposed additions, not claims that these
interfaces already exist. If the same contract fits an existing interface cleanly, extend that interface and update
this section rather than maintaining two competing provider APIs.

The selected `PackedPlanRecipe` must retain a versioned physical descriptor identifying provider, implementation,
access choices, required/delivered properties, semantic correspondence, and validity assumptions. Store candidate and
property IDs in the working memo. Detach only selected or explicitly retained alternatives into immutable descriptors
before the session closes. Descriptors must not retain a transaction, cursor, adjacency view, estimator session,
filter lease, mutable scratch, or worker reservation. A recipe's assumptions must be checked when reused.

Add the LMDB implementations under the storage package, provisionally `LmdbPhysicalStrategyProvider` and
`LmdbAccessCostEstimator`, and a `LmdbPhysicalRecipeBinder` under the native evaluation package. The provider exposes
existing strategies, the estimator quotes access work for both planners and runtime access arbitration, and the
binder realizes the selected descriptor using the execution context. Avoid a permanent provider hierarchy for each
minor strategy variation; dispatch compact family IDs to existing implementations.

Every quote records its evidence quality and provenance, local versus inclusive scope, consumption assumptions,
pricing version, and feasibility conditions. Version the mapping between native features and stable packed
dimensions. Add dimensions only when an existing dimension cannot express genuinely distinct work; do not renumber
stable IDs or silently reinterpret persisted values. Preserve saturation behavior and known/unknown masks.

### Phase 0 — Establish current evidence and regression oracles


**Dependency and purpose.** This is the first executable phase of the authorized implementation. Establish what the
current branch does before changing its decisions, and make later claims about
planning speed and execution improvement reproducible. The present document is not its benchmark baseline.

**Baseline decision (2026-11-07).** After the restored working tree and its reader-lifetime prerequisites pass their
remaining qualification, freeze that complete merged, partially implemented source state as a new forward baseline.
Inventory every included partial costing change and record source, dependency, JDK, configuration, dataset and store
identities. Establish repeated baseline-versus-baseline variation and freeze per-lane tolerances before changing later
unified-provider decisions. The preserved pre-merge planning run includes a fallback during measurement and is not a
valid baseline. Its lifetime patch and the delegated capability also depend on WIP-only provider interfaces, so do
not backport or copy partial implementations merely to manufacture a historical comparison. This forward baseline
can assess subsequent work against the state actually frozen; it cannot establish a performance change attributable to
the already-present WIP relative to `d69e9dd2`, upstream main or the pre-merge tree. Make no such claim without an
independently valid matched comparison. Retain all old measurements as diagnostics, not acceptance evidence.

**Work.** Inventory actual call paths from `LmdbQueryOptimizerPipeline` through packed selection, native strategy
assembly, statement access arbitration, and execution. Record each strategy's recognizer, quote source, binder,
output properties, coverage restrictions, and feedback identity in this roadmap's implementation catalog. Include
native subregions inside a generic outer plan. Inspect active test annotations: an existing test that is disabled is
not coverage. In particular, do not use the name of `LmdbNativeStrategyPriorityTest` as evidence that every former
priority assertion still runs.

Extend existing plan-quality and native decision diagnostics with a lightweight invocation record: logical region,
required properties, considered strategy/access IDs, quoted physical dimensions, chosen recipe ID, actual recipe ID,
and fallback reason. Add counters for candidates recognized and quoted, metadata reads, estimator calls, refinements,
cache validation, provider scratch high-water, retained memo bytes, and each planning stage. Store primitive counters
and generate detailed strings only when an explanation or diagnostic is requested. Instrument phase durations at
boundaries rather than timing every emitted row.

Prepare small deterministic stores with deliberately different access work and result counts, independent-branch
stars, cyclic joins, skewed hubs, correlated semi/anti queries, and count/distinct/limit consumers. Create an
exhaustive test oracle for small supported query regions that enumerates legal physical alternatives and compares
typed costs and result bags. It must independently check the minimum under supplied synthetic prices rather than
repeat the production ranking expression. Include a case in which a logically smaller alternative has greater
physical execution cost. Do not assert a real-world latency winner from synthetic prices.

Capture planning-only, selected-preparation-only, fixed-plan execution, and full end-to-end baselines separately.
Cover cold plan cache, warm plan cache, cold/warm generated-code cache, and repeatable storage-cache regimes. Record
hardware, JDK, data snapshot, index configuration, concurrency, flags, and result checksum. Use the complete current
Theme query catalog, not only a remembered count of its historical cells. Keep a fixed-plan control to distinguish
optimizer overhead from a changed execution plan.

The native fixed-physical control depends on the selected-descriptor-to-binder handoff delivered in phase 4; requiring
that handoff as a phase-0 prerequisite would make the roadmap circular. During phase 0, investigate a constrained
generic-only control (for example, immutable data and a single usable access) and call it fixed only if its actual
execution identity and applicability are proved. Record an unavailable control honestly if that proof is not
possible. The forward baseline and phase-0 exit inventory can document this specific integration gap; a native
fixed-physical execution comparison and selected-to-executed proof remain mandatory for phase-4 acceptance rather
than being fabricated by diagnostics or asserted complete before the descriptor exists. The current q2 fixture has
two indexes and permits adjacency; its receipt does not describe every generic access. A single-index, native-off,
adjacency-disabled fixture is configurable, but generic empty/count/cache shortcuts and the mutable per-pattern index
field prevent that configuration alone from proving the complete execution route. Existing lower-level iterator/index
tests are useful controls, not an invocation-level q2 receipt. Treat this generic fixed-access control as UNPROVEN
until its same-evaluation bag, snapshot and actual accesses are established.

**Proof and exit.** Add the proposed `LmdbUnifiedPhysicalPlanningTest` in the LMDB test package and an independent
small-plan oracle in core packed tests. Capture the smallest failing behavior test before each later production
change. Phase 0 itself ends with a reproducible current baseline, an inventory of coverage gaps, and regression
fixtures that can demonstrate the missing integration. No new strategy is enabled merely because diagnostics exist.
Set performance tolerances from repeated baseline-versus-baseline variation before measuring a candidate; record
them here so the acceptance threshold cannot be widened after a disappointing result.

**Dependency refinement (2026-11-08).** The frozen forward source and dependency inventory, observed q2 shared-host
A/A variation, 182-cell gap ledger, and current integration reds are sufficient to begin a behavior-preserving phase 1
contract adapter without waiting for every phase-0 acceptance campaign. Phase 0 itself remains incomplete: carry the
remaining bounded/full-scale corpus, cached replay, quiet-host/matched execution controls and final timing obligations
explicitly into the later qualification campaign. Do not enable a decision-changing physical family or claim a
performance acceptance until the evidence required for that decision exists. The phase-4 native descriptor and
selected-to-executed proof remain phase-4 deliverables, so phase 0 must not depend circularly on their completion.

**Current exit gaps and operational continuation (2026-11-07).** The following catalog status refers to bounded
independent semantic qualification on the current WIP, not historical count checks or full-scale timings. Each row
covers the named theme's q0--q12, thirteen distinct cells. REAL_ESTATE uses the pinned 522-statement bundle: all
thirteen cells have an independent Jena comparison under the documented conservative computed-numeric value policy
and exact RDF4J MemoryStore/generic-LMDB/native-enabled bags. SOCIAL_MEDIA q0--q12 have the same bounded comparison
on a separate 75-statement fixture, including q11's ordered key groups. ADAPTIVE_FILTER_PLACEMENT and
DATA_TRANSFORMATION q0--q12 also passed these comparisons on separate deterministic default graphs. The other 130
cells are NOT_RUN under this qualification, not known unsupported and not evidence of semantic failure:

| Bounded catalog cells | Current outcome |
| --- | --- |
| REAL_ESTATE q0--q12 | 13 qualified with the stated Jena value policy; native execution UNOBSERVED |
| SOCIAL_MEDIA q0--q12 | 13 qualified with the stated Jena value policy; native execution UNOBSERVED |
| ADAPTIVE_FILTER_PLACEMENT q0--q12 | 13 qualified; q0, q8 and q9 observed a native row region with no selected-to-executed link; the other ten UNOBSERVED |
| DATA_TRANSFORMATION q0--q12 | 13 qualified; native execution UNOBSERVED |
| MEDICAL_RECORDS, LIBRARY, ENGINEERING, HIGHLY_CONNECTED, TRAIN q0--q12 | 65 NOT_RUN |
| ELECTRICAL_GRID, PHARMA, SPARSE q0--q12 | 39 NOT_RUN |
| ANALYTICS, EXPLORATION q0--q12 | 26 NOT_RUN |

Maintain a per-cell ledger with exact query, fixture, dataset and configuration identities, individual bounded
result and any unsupported, resource-incomplete, semantically unqualified, failed or not-run outcome. Do not silently
drop cells, infer unsupported status from missing observations, or call a cell COMPLETE from a count or fingerprint.
The SOCIAL_MEDIA fixture is a deterministic default graph with dataset SHA-256
`b366d1370a138ebe7ed53724f6af8754fc18b16f30cefbf0b4d0a65980e873cd` and manifest SHA-256
`0b7ca00c7f6de9c63f01e74508b3a46188e7b7a4a3381d677b8f91f303ee435c`. Its independent witness tests check positive
aggregate values, both q5 UNION branches, distinct degree thresholds, cycles including permitted repeated positions,
self-follow exclusion, OPTIONAL fanout and missing bindings, q11 ties, and q12 DISTINCT duplicate elimination.
Isolated edge-removal controls rule out alternative cycles in the checked negative cases. Jena's unmodified q11 and
q12 bags contain 54 and 237 rows respectively. The 39 bounded per-invocation captures reported UNOBSERVED with no
truncation; native-enabled mode alone is not evidence of native execution. The updated 182-cell inventory and
preserved red/green reports are under `/private/tmp/rdf4j-social-oracle-20261107-6vecgbot/qualified-v2/`.
The 156-cell query inventory and proposed batches, prepared before the latest qualification, are sealed under
`/private/tmp/rdf4j-remaining-catalog-batches-20261108-863bbxfv/`. The adaptive fixture has 29 statements and dataset
SHA-256 `92d7317c8c3bd2fb29167b4063f9271d8be1638303a8b41dad96e26d0831a9d4`; the transformation fixture has 89
statements and dataset SHA-256 `468224f5abab150b430778f4f6dee94b40a6dbbb732866cf86083212163ce253`. The former's
tests compare explicit full bags including duplicate projected rows; the latter's tests verify positive aggregate
counts, expression boundaries, fallback choices and DISTINCT fanout. Existing REAL_ESTATE and SOCIAL_MEDIA resources
regenerate byte for byte. The shared profile checks supported order and slice shapes and rejects SAMPLE, GROUP_CONCAT
and REDUCED until a separate permitted-result contract exists. Key-group sidecars currently accept only IRI or
unbound keys; other key terms fail closed. The 52-qualified ledger, raw observations and reports are under
`/private/tmp/rdf4j-expression-oracle-batch-20261108-h3zv5rm_/qualified/`. All 156 observations for these four themes
were within the 512-event/128-character limits; three adaptive native-enabled invocations observed a started and
completed row region, with no selected-to-executed link. An enabled flag alone remains insufficient evidence of native
execution. Full result bags were drained; these small fixture checks do not impose a general result-row resource cap.
Create representative bounded fixtures and independent permitted-result checks for nondeterministic SAMPLE,
unordered GROUP_CONCAT and tied slices. ANALYTICS and EXPLORATION need their actual cross-theme and graph populations.
Full-scale resource and timing outcomes are separate ledger axes and have not yet been qualified for this forward
baseline. The independently diagnosed eight-factor Frontier planning fixture is RESOURCE_LIMIT_REACHED at the current
retained-memory budget; it is not a COMPLETE-planning latency sample, and that outcome must not be assigned to a
particular catalog cell without an observed matching invocation.

The validated merged-plus-WIP state is frozen in `/private/tmp/rdf4j-phase0-freeze-20261107-7z22bikf/`, with an
inventory of partial changes and source, dependency, JDK, hardware, query, dataset, store/index and configuration
identities. Its original 182-row ledger pinned every query but predates SOCIAL_MEDIA qualification; the separately
sealed SOCIAL_MEDIA ledger above has 26 qualified cells, and the later adaptive/transformation ledger has 52. The
freeze's 25,783 source files and 8,986 dependency files matched before/after live audits; evidence-only updates to
this plan after that freeze are not included in the sealed source. Use private complete source copies and private
`.m2_repo` clones: the tested Docker wrapper's command-line local-repository setting overrides its
environment setting, so running the wrapper against the live checkout would write live build state. The isolation
audit is `/private/tmp/rdf4j-upstream-merge-preserve-ycf154_j/validated-live-source/run-22-phase0-maven-docker-repo-isolation/report.md`.
The review-ready `PROTOCOL.md` in that freeze specifies the qualified q2 lane/fixture seam, identities, alternating
paired A/A schedule, same-invocation receipt gates and a variation-derived per-lane tolerance method to freeze before
any decision-changing candidate. Its pinned Linux/JDK 26 jar passes a same-helper FRESH/COMPLETE 25-row exact-bag
preflight. The shared-host Linux A/A then stopped at its predeclared retry cap with zero valid pairs: the first two
optimize-only pair attempts encountered FRESH during warmup; the third attempt's A fork reported seven
PIPELINE_CACHE/COMPLETE iteration receipts, but its paired fork encountered FRESH during measurement. JMH returned
zero despite the benchmark exception, and the independent validator correctly rejected the incomplete run. There is
no tolerance or performance conclusion. Preserve all attempts, host-load evidence and the sealed blocked-study
handoff at `AA-BLOCKED.md` in the freeze. A later read-only same-invocation JDI diagnostic reproduced FRESH/COMPLETE
and traced the conservative pipeline admission bypass to differing captured/current evidence revisions: the former
included adjacency-statistics generation 3 while the latter had no available generation. This establishes a changing
availability observation, not its deeper lifecycle cause or an eviction defect; retain the latter as a phase 6 audit
question. The trace is `/private/tmp/rdf4j-cache-diagnostic-20261107-enbdblex/jdi-run-v4/jdi-events.log`. Do not
relabel these runs or widen the retry cap. Independently prepare a separately versioned FRESH/COMPLETE protocol only
if both cache layers can be explicitly disabled and the observed
receipt confirms the regime. A same-helper non-timed q2 control using the existing zero cache-evidence budget produced
30/30 FRESH/COMPLETE receipts; the frozen explicit-fixture JMH setup cannot yet specify that store setting. Expose the
existing nonnegative budget as an optional, default-preserving fixture input only after a real setup/receipt red,
validate its representation, and verify zero plus positive/default controls before sealing a new protocol. Keep the
earlier frozen experiment intact. The in-repository red observed the zero value ignored and a repeated PIPELINE_CACHE
receipt; the candidate applies the optional budget and the real zero-budget setup and repeat report FRESH/COMPLETE.
The full Theme benchmark test class passed 52/52 before and after scoped formatting. A newly added positive-budget
control now allows any observed planning path because retention is permitted rather than guaranteed; after that
correction, scoped formatting and the complete Theme class again passed 52/52. The adjacent helper class retains its
known pre-entry `Filter at root.requiredInputFrame` failure (14/15). Evidence is retained at
`/private/tmp/rdf4j-fixture-budget-20261107.2MOZh0/` and
`/private/tmp/rdf4j-fixture-budget-correction-20261107.O1j5wx/`. The separately frozen fresh protocol passed all
four generated-harness lifecycle smokes and a same-fixture zero-retention diagnostic, then completed 80/80 valid A/A
pairs without replacements. Its empirical ratio envelopes are 0.67445--1.48268 for optimization,
0.73613--1.35846 for final-tuple precompilation, 0.90656--1.10307 for reused-step evaluation, and
0.89923--1.11206 end to end. Each sample's preflight matched the exact q2 bag and each planning receipt was
FRESH/COMPLETE; reused-step had one initial receipt per fork. The shared host ranged from 4.54% to 90.29% busy and
selected-to-executed identity remained UNOBSERVED. These envelopes describe that shared-host experiment only; the
rank interpretation assumes exchangeability, and they do not establish quiet-host acceptance, native/fixed-physical
execution, speedup or non-regression. Frozen inputs and all 3,445 run artifacts were independently hashed; the
complete protocol, measurements and limitations are in
`/private/tmp/rdf4j-fresh-phase0-freeze-20261107-yk_3d53i/postrun/RESULTS.md` (results-seal SHA-256
`bee806bdf462b3036b1b0373b2d3f4029580b3667156994a063e9863efb2dcc0`). The cached-path study remains blocked. Run JFR
separately as diagnostic instrumentation, not as the A/A timing control.

The independent core oracle currently covers three admitted dependent-join costing routes under synthetic prices;
it does not enumerate native alternatives or prove a global physical optimum. The unchanged
`LmdbUnifiedPhysicalPlanningTest#selectedStatementAccessMatchesTheExecutedSource` was rerun on an isolated clone of
the frozen source and dependency seed: one test failed with the same six missing candidate/quote/recipe assertions as
the preserved pre-merge red, while a real `pocs` cursor scanned and matched 32 rows and the separate evaluation's
result bag passed. The selected-equals-actual assertion passed because both IDs were null. The test exposes neither
same-invocation packed completion/fallback nor admitted-view identity, so this repeat does not yet distinguish absent
provider/recipe/binder integration from incomplete planning or a scope fallback. Preserve its exact command, reports,
two setup failures and source/dependency audits in `/private/tmp/rdf4j-selected-access-20261107.BHQHVv/`. A subsequent
private test-only diagnostic of that same explanation invocation reported COMPLETE, approximate=false, empty limit
causes and no packed cache hit; its statement carried mapped-statistics status, no mapped fallback reason, and matching
generation/base/covered epoch 4. The same runtime receipt reported a completed `pocs` cursor with 32 scanned/matched
rows, but selected/executed recipe fields remained absent. The separate root fallback key was UNOBSERVED, and the
exact bag still came from another evaluation. Thus this invocation was not resource-incomplete or a scalar statement
estimate, but matching planned and runtime index names do not establish a binder handoff. Its retained diagnostic
patch, control and reports are in `/private/tmp/rdf4j-selected-access-telemetry-20261107.XXL9Ek/`. General provider
quote, selected recipe/binder, budget/refusal, cache/feedback and execution
identity oracles must then demonstrate the missing phase 1--4 contracts. Existing observations cover fresh owned ROW
regions only; GROUP, ordered, borrowed, nested, retry and native strategy-attempt paths remain UNOBSERVED, and the
selected-to-executed link remains UNLINKED. A packed native executable descriptor does not yet exist, so neither
written algebra order nor reusing a compiled step supplies the required fixed-physical-plan control.

### Phase 1 — Connect shared contracts and budget every provider


**Dependency and purpose.** Begins after phase 0 has frozen its forward source, evidence inventory and integration
reds, without declaring phase 0's remaining acceptance gates complete. Make physical alternatives representable and
optional estimation work accountable before adding more alternatives to search. This phase begins with an adapter
preserving existing selection behavior; subsequent behavioral changes still require their own failing tests and
applicable baseline/control evidence.

**Work.** Extend `PackedCostSession`, `PackedPhysicalMetadataArena`, `PackedPhysicalPropertyInterner`,
`PackedCostEstimate`, and `PackedPlanRecipe` using the provider contract above. Map native physical features and
evidence into `PackedCostDimensionRegistry` and `PackedCostVectorArena`; preserve startup, unknown quantities,
structural bounds, and native consumption information. Use `PackedCostAlgebra` for composition. Add explicit tests
that a region quote replaces its covered internal work rather than summing it twice. Define coverage with verified
structural handles and edges from the actual region, not an unaudited count asserted by a provider. At bind time,
distinguish a legitimate capability/resource refusal that permits a declared fallback from cancellation and real
execution failures; preserve the original failure and cleanup identities rather than hiding them to run fallback.

Extend `PackedPlannerLimits` accounting so recognition, costing, optional metadata access, evidence realization,
refinement, detachment, and cache validation all charge the same query-level budget. Keep work, deadline, retained
memory, and scratch high-water separately visible. Reserve memory through `PackedProviderMemory` before allocating
provider storage. Define charges by actual operations, such as metadata directory entries visited or estimator
transitions, rather than treating an arbitrarily expensive callback as one cheap unit. Check monotonic deadlines at
bounded batch boundaries and before beginning expensive work. A synchronous page fault or native call can overrun a
deadline before control returns; document and measure this limit rather than promising hard real-time termination.

Use explicit statuses for inapplicable strategy, unsupported semantics, missing evidence, incomplete exploration,
and resource rejection. Distinguish optional optimization resources from resources necessary to execute a valid
fallback. Record the minimum fallback construction/validation work separately; a zero optional-search budget should
skip optional exploration without promising zero total planning work.

**Proof and exit.** Extend `PackedPhysicalCostContractTest` and `PackedCostAlgebraTest`; add the proposed
`PackedPhysicalStrategyContractTest` and `PackedPlanningBudgetTest` in the core packed test directory. Tests must
prove consistent dimension mapping, unknown propagation, no double counting, deterministic budget charging,
reservation-before-allocation, cleanup on provider failure, and a valid fallback when optional budgets are zero or
exhausted. Fake providers that attempt cursor work, compilation, or resource acquisition during recognition must be
rejected by their contract tests. Scalar cost-model implementations remain source-compatible and keep their old
behavior until deliberately integrated.

### Phase 2 — Make LMDB and adjacency access quotes physically meaningful


**Dependency and purpose.** Depends on phase 1. Correct the foundational distinction between entries visited and rows
returned before tuning strategy preferences. Every later native strategy depends on these access estimates.

**Work.** Extract shared access description and work estimation from `LmdbPackedCostModel`,
`TripleStore.describePackedAccessPath`, `LmdbSailStore`, `LmdbStatementAccessArbiter`, and `LmdbNativePatternPlan` into
the LMDB access-cost boundary. Enumerate configured, usable B-tree indexes rather than pricing only the longest-prefix
choice. For each candidate retain fixed prefix, range interval, residual predicates, repeated-variable checks,
context handling, delivered order, and binding requirements. Estimate cursor opens, seeks/page walks, visited
entries, residual checks, decoded values, emitted rows, and bytes independently. If the visited-entry estimate is
unknown, retain that uncertainty instead of copying output cardinality into the physical field.

Add adjacency alternatives for covered snapshots and supported shapes: forward/reverse runs, membership tests,
ordered distinct frontiers, range filtering, and aggregate metadata. Describe what the implementation actually does.
For example, a range implemented by gathering or sorting a broader run must pay for that broader access and sort;
the existence of a range predicate does not make it a native range seek. Metadata counts can avoid triple iteration
but may still visit root, predicate, context, or run directories. Charge those visits; do not label all metadata
aggregates constant-time.

Use `LmdbFrontierReadSelection` and the existing adjacency read-view lifecycle to bind all evidence to one valid
query snapshot. Distinguish exact empty, no coverage, not yet available, and stale metadata. Handle explicit/inferred
planes, default/named graphs, multiple contexts, read-your-writes, and concurrent commits according to existing store
semantics. No cardinality estimate may turn missing adjacency coverage into an empty-result proof.

For parameterized access, quote the current known/possible binding masks, value-domain evidence, outer invocation
count, distinct key count, and skew. Charge reuse only when the selected implementation really caches or groups those
keys. A nested loop with ten thousand equal bindings still performs ten thousand probes unless its recipe includes
reuse. Keep per-probe expected work and conservative feasibility bounds distinct, especially for hubs and all-miss
semi/anti probes. Reuse already available Frontier/quad/adjacency evidence; prohibit full scans for costing.

Make packed scans, native pattern access, and statement arbitration consume this quote. During migration, a
compatibility adapter may translate to old fields, but it must preserve provenance and unknown state and must not
become a second independently maintained formula.

**Proof and exit.** Add the proposed `LmdbUnifiedAccessCostTest`. A broad prefix with a selective residual must quote
more source events than output events. Equal-result alternatives using different indexes must have different
physical work when their actual accesses differ. Exercise ranges, repeated variables, object/subject anchors,
context filtering, unavailable adjacency, overlay updates, duplicate outer keys, and skew. Cross-check the three
consumers of the shared quote under the same snapshot and properties. Measure estimator work by metadata operations,
not by assuming a method returning a count is cheap. No production decision may rely on a fabricated exact bound.

### Phase 3 — Carry consumer requirements and preserve physical alternatives


**Dependency and purpose.** Depends on phases 1 and 2. Make the memo retain distinctions that affect what the parent
can do, so a rewrite cannot win merely by reporting fewer logical rows.

**Work.** Represent consumer requirements in `PackedPhysicalPropertyInterner` and propagate them through memo search,
physical refinement, recipes, continuations, and cache identity. Required information includes live bindings,
duplicate/multiplicity semantics, full enumeration versus weighted/factorized consumption, group or distinct keys,
ordering, row goal and offset, existence versus full exhaustion, assured/possible binding masks, and correlation.
Reuse `LmdbWildcardPhysicalDemand` and native slot/aggregate demand concepts through backend-neutral equivalents;
do not let their native class types leak into core.

A factorized representation stores independent alternatives and multiplicities without expanding their Cartesian
product. Retain it when a compatible COUNT, EXISTS, DISTINCT, or grouping consumer can use it. Add and price explicit
conversion alternatives when a parent needs flat binding rows, a different order, materialized values, or a hash
table. Full row enumeration must still pay for every emitted row. COUNT of a nullable expression, COUNT(*), DISTINCT,
empty aggregates, and duplicate-preserving projections have different requirements; sharing a consumer label must
not erase those semantics. Internal ID order is not automatically SPARQL term order.

Audit `LmdbQueryOptimizerPipeline` and `LmdbCascadesOptimizer` for destructive choices before physical alternatives
enter the memo. Preserve equivalent shapes whose implementation opportunities differ, while sharing semantic facts.
First teach recognizers to see through harmless aliases, constant propagation, and normalized syntax when that is
semantically valid. When equivalent shapes still enable different implementations, retain both. Do not assign a
permanent cost penalty to a rewrite or a permanent bonus to a strategy name.

Extend dominance and exact-continuation comparisons to include all relevant requirements, delivered properties,
evidence state, and feasibility assumptions. Remove an alternative only when another can substitute for it for
every relevant continuation under the declared dominance rule. A cheaper flat scan does not dominate a slightly more
expensive ordered or factorized producer needed by its parent. A hash match on state is not proof of continuation
equivalence. Re-cost dependent parents when a useful child alternative appears or a quote is refined.

Route packed final ranking, `LmdbPhysicalCostObjective`, and native arbitration through the same physical-feature
pricing policy and consumption objective for integrated candidates. Preserve conservative bounds separately from
expected prices. Existing native cold priors may remain explicit compatibility estimates for an unintegrated family,
but an arbitrary preferred-strategy order cannot decide among fully integrated comparable alternatives. Do not mix
raw work rows, time, and memory in a dimensionless sum without a declared conversion.

**Proof and exit.** Extend core physical-property/cost tests and add the proposed `PackedConsumerDemandTest` and
`PackedStrategyRetentionTest`. Supply deterministic prices where an alternative with fewer logical rows loses to
one using fewer seeks or a factorized consumer; verify that both remain available until physical selection. Reverse
the prices and verify that selection reverses. Test late child alternatives, nested consumers, blocking startup under
LIMIT, all-miss existence, conversion costs, and exact cache-key separation. Compare exact small searches against
phase 0's independent oracle. These are model-contract tests, not claims about measured wall-clock rankings.

### Phase 4 — Prove one complete Cascades-to-native execution path


**Dependency and purpose.** Depends on phases 1–3. Deliver the first observable end-to-end improvement before
integrating the entire native strategy inventory. The unit of success is a physical recipe that is considered,
costed, selected, preserved through lowering, executed, and explained consistently.

**Work.** Integrate ordinary statement/B-tree access, covered adjacency access, native indexed count, and a supported
factorized count/aggregate region. Use `LmdbNativeIndexedCount`, `NativeAggregatePhysicalPlan`,
`LmdbNativeFactorizedRows`, and the existing aggregate/row steps through the shared provider. Include ordered access
when it provides a property needed by these consumers. A generic aggregate over ordinary access remains a valid
competitor, not an emergency route removed by the mere existence of a native candidate.

Connect the selected descriptor in `PackedPlanRecipe` to `NativePhysicalInputRecipe`, pattern compilation, and the
native binder. Preserve logical slot correspondence, filters, contexts, binding scope, duplicate semantics, and
physical choices. Native evaluation must not discard the descriptor and independently choose a different algorithm
using an unrelated scalar estimate. Statement arbitration must honor the access choice or a declared equivalent
adaptive choice within the recipe. If the descriptor deliberately allows runtime adaptation, quote that policy and
record the concrete variant that ran.

Validate snapshot assumptions and execution resources at bind time. Worker availability, hash-memory admission,
code-cache state, or adjacency coverage can change between quote and bind. Use a previously validated equivalent
fallback with bounded preparation, record the reason and actual identity, and release rejected attempt resources.
Do not restart an unbounded optimizer in an execution callback. Data or proof assumptions that affect correctness
must be revalidated; performance assumptions may justify a different feasible implementation without changing results.

Construct a pair of equivalent query shapes where one supported representation allows a metadata/factorized
aggregate and the other requires row enumeration under current capabilities. Establish the actual capability
difference in a test rather than assuming every rewrite destroys native support. The integrated planner must choose
using complete physical cost. A normalization that exposes the same native implementation to both forms is better
than retaining an artificial syntax-dependent preference.

**Proof and exit.** Extend the proposed `LmdbUnifiedPhysicalPlanningTest` and add `LmdbPhysicalRecipeBindingTest`.
Assert quoted/selected/executed recipe correspondence, exact result-bag parity, metadata versus row-iteration
counters, and fallback cleanup. Exercise both a known-cost winner and an unavailable-adjacency snapshot. Test cold
and cached plans and repeat after a commit that changes relevant assumptions. A real benchmark must then show the
expected work reduction and whether it improves total latency. Do not proceed to broad strategy integration if
lowering still silently substitutes an independently ranked plan.

### Phase 5 — Integrate native strategy families in verifiable slices


**Dependency and purpose.** Depends on phase 4. Expand physical coverage through the same contracts. Each family must
complete recognition, quotation, properties, binding, fallback, explanation, and tests before the next family is
declared integrated. Merely registering its name is not completion. Continue applying phase 1 budgets in every slice.

**Slice 5A: metadata and aggregate producers.** Extend the aggregate path to `LmdbAdjacencyAggregatePlan`,
`LmdbNativeCensusAggregate`, `LmdbNativeRunCountHistogram`, `LmdbNativeDatatypeHistogram`,
`LmdbNativePredicatePlaneGroups`, `LmdbTypeMatrixPlan`, and `LmdbNativeExistsIntersection` where supported.
Price directory visits, distinct values classified, predicate planes visited, ordered intersections, group state,
and final emitted groups. These can make COUNT, grouped counts, datatype counts, and existence-backed distinct
counts much cheaper than enumerating their contributing triples. Preserve each implementation's current semantic
preconditions; no type-matrix claim is valid merely because a query contains `rdf:type`. Test filters, repeated
variables, nullable expressions, empty groups, named contexts, and unavailable metadata against generic evaluation.

**Slice 5B: factorized and multiway joins.** Integrate `LmdbNativeFactorizedRows`, `LmdbNativePackedFtree`, and
`LmdbNativeLeapfrogJoin` as region alternatives. Independent branches of a star can remain factorized; cyclic joins
may benefit from intersecting sorted domains before producing large pairwise intermediates. Estimate run visits,
intersections, factor records, surviving domains, and expansion requested by the consumer, with degree/skew evidence
when available. Do not describe all stars or cycles as automatic wins. Keep current recognizer restrictions as
explicit implementation capabilities; they are not permission to impose arbitrary global search cutoffs. Verify
consumer changes from COUNT to full row output and correlated branch dependencies that invalidate factorization.

**Slice 5C: binary and ordered joins.** Integrate `LmdbNativeHashJoin`, `LmdbNativeMergeJoin`, ordered prefix/run and
ordered-distinct strategies assembled by `LmdbNativeRowStep` and `LmdbNativeGroupStep`, and batch alternatives.
Hash joins pay build input, key construction, probes, memory, and applicable spill/rejection behavior. Merge joins
must account for available order, duplicate runs, rescans, and any sorting; do not advertise broader merge eligibility
than the implementation supports. Preserve useful delivered order through `LmdbNativeSlotOrder` so downstream
grouping or DISTINCT can avoid another materialization. Include correlated nested-loop and memoized semi/anti
alternatives: a small outer input can favor probing, whereas repeated or wide outer input can favor one build.

**Slice 5D: native kernels, filtering, and parallel execution.** Integrate interpreted and generated kernels through
`LmdbNativeKernelLowering` and `LmdbNativeKernelExecution`; integrate `LmdbNativeAdaptiveFilterPlacement` and
`LmdbNativeParallelPipelines`. Quote fusion benefits, actual filter evaluation work, residual callbacks, value
materialization, code generation/compilation, cached-code reuse, dispatch, partitioning, worker capacity, merge,
and close work. Pure lowering and eligibility inspection may be cached; compilation is selected-plan preparation.
Do not assume a filter, UNION, or OPTIONAL always breaks native execution: the current lowering supports multiple
such compositions, and unsupported regions may still contain native children. Test expensive selective predicates
both before and after cheaper joins rather than applying a blanket pushdown preference. Parallel candidates must
use non-reserving capacity estimates and tolerate a smaller actual grant without charging impossible ideal speedup.

**Slice 5E: paths, wildcard access, and mixed execution.** Integrate `LmdbNativePathPlan`, wildcard demand handling,
and `LmdbNativeInteriorPlans` into the same region/property model. Bound-endpoint traversal, reverse traversal,
existence, and all-pairs path evaluation need different frontier, edge-visit, deduplication, and termination estimates.
Preserve supported path semantics rather than treating paths as a row-count multiplier. Wildcard/variable-predicate
queries must pay for the fields and multiplicities the consumer needs; projection can enable a narrower producer.
Allow a generic outer operator with native children and charge both transitions. Preserve effect-sensitive lexical,
filter, and expression boundaries; an opaque operation must not accidentally disappear into a quoted native region.

**Proof and exit for each slice.** Add focused cases to active family tests and to the shared end-to-end fixture.
Force each supported alternative in a controlled test, compare its result bag to the reference, then verify normal
planning chooses it only when the supplied costs and properties justify it. Include a losing case and an unsupported
case with a valid fallback. Extend `LmdbPhysicalWorkPricingRegressionTest`, `LmdbNativeSelectorTest`,
`LmdbNativeDispatchLoopEdgeCaseTest`, `LmdbNativeStrategyProposalTest`, native decision diagnostics, and relevant
family parity tests. Measure recognition
and quote counts as well as execution work. Every previously supported family must be either integrated or explicitly
listed as a remaining compatibility path; no silent loss of native coverage is acceptable.

### Phase 6 — Qualify fast search, selective refinement, and reuse


**Dependency and purpose.** Basic controls already exist from phase 1. This phase qualifies and tunes them with real
strategy diversity from phases 4 and 5, so search does not repeatedly pay for the same information or grow without a
declared limit. Individual improvements should land earlier when their dependencies are ready.

**Work: avoid repeated work.** Precompute compact shape, binding, predicate, and consumer summaries once per immutable
query/memo expression version. Recognize each strategy family once per relevant expression/requirement/evidence key,
including negative eligibility results. Invalidate that result when a late equivalent expression changes the facts
on which recognition depends. Cache leaf and parameterized access quotes by canonical pattern, binding mask/domain,
access path, snapshot assumptions, consumer requirement, and pricing version. Reuse primitive scratch arrays and
interned IDs; avoid algebra cloning, repeated tree walks, per-candidate collections, and formatting explanations in
the search loop. Do not merge parameter values or binding domains just because their masks match.

**Work: spend evidence selectively.** Start with exact metadata and available summaries, then existing statistical
evidence, then explicitly budgeted refinement for candidates whose ranking is uncertain and consequential. Reuse
one refined evidence state across the transitions it legitimately covers. Refine an estimate only if its possible
decision benefit can justify additional planning effort; unknown value-of-information remains unknown and the hard
budget still applies. Statistical intervals can prioritize work but cannot soundly discard a feasible alternative.
Proven physical lower bounds may prune against an executable incumbent only under compatible properties, resource
policy, and estimator continuation. The policy for optional refinement must not silently change the meaning of
exact search completion.

**Work: retain useful alternatives within honest limits.** Keep nondominated alternatives with different order,
representation, resource, consumption, and continuation properties. Use existing Pareto and continuation machinery,
where a Pareto set contains alternatives for which no substitutable plan is at least as good in every relevant
dimension. Avoid multiplying every index, join algorithm, worker count, filter placement, and output representation
before compatibility is checked. Generate applicable alternatives lazily and re-cost dependent parents incrementally.
If bounded search stops before exploring or retaining the required frontier, report incomplete search and return the
incumbent. A top-K cap, beam width, confidence interval, or cheapest-row-count rule must not masquerade as an exact
pruning proof.

**Work: reuse valid plans safely.** Extend `PackedPlanCache`, `PackedCostingReplay`, `LmdbPlanDecisionCache`,
`LmdbPipelinePlanCache`, and `LmdbFrontierPlannerSettings` rather than adding another cache or
background scheduler. Retain the detached physical descriptor and compact validation assumptions. Separate a
correctness invalidation, such as a changed fact that justified a rewrite, from a ranking refresh, such as changed
prices or selectivities. A harmless calibration update should not force every cached plan through full search.
Use bounded validation and refresh with the existing single-flight/refresh mechanisms. Cache hits must avoid opening
a full estimator session or rebuilding native candidates when descriptor assumptions can be validated cheaply.

Include dataset/context, inferred/explicit semantics, parameter identity/domain, binding layout, consumer demand,
index/capability version, snapshot-dependent facts, recipe schema, and price-model applicability in validation as
needed. Do not make a query-local transaction pointer the identity of a reusable plan. Do not retain an old adjacency
view or mapped-generation lease in the descriptor. A cache entry with incomplete search remains marked incomplete;
replay cannot promote it to exact merely because its execution is valid.

**Proof and exit.** Extend `PackedPlanningBudgetTest`, `PackedPlanCacheTest`,
`PackedPlanCacheOperatorParameterIdentityTest`, `PackedPlanCacheFamilyFlightIsolationTest`, and the LMDB packed and
pipeline cache tests, including commit invalidation, dataset identity, UNDEF rows, and failed-flight cases. Prove
bounded callback counts and allocation reservations with deterministic fakes, then measure cold/warm latency and
allocation. Stress concurrent queries and cancellations: per-query compliance must not hide an unbounded shared
cache, refresh queue, or cumulative memory footprint. Simple queries and cache hits must remain within the phase 0
performance envelope; expensive queries must justify any additional planning time through better total latency.

### Phase 7 — Learn from the physical work that actually ran


**Dependency and purpose.** Recipe identity and quote dimensions must already be stable. Connect feedback to those
same dimensions without conflating cardinality errors, missing work, and machine-price errors. This work follows a
correct unlearned model; learning must not conceal an inconsistent implementation contract.

**Work.** Integrate `LmdbMappedFrontierLearningSession`, the packed cost session/replay path,
the opt-in `LmdbNativeAdvisoryLearner`, native execution diagnostics, and statement-access observations through a
versioned descriptor linking query region, physical recipe, access choice, consumer requirement, and evidence scope. Attribute
actual work to the variant that ran, including runtime fallback, rather than the preferred variant that was quoted.
Separate operator-local from inclusive totals so an aggregate region, its pattern scans, and an enclosing pipeline
do not all train on the same elapsed time as independent work.

Observe source events, seeks, relevant metadata visits, key misses/hits, value decoding, materialization, generated
code preparation, memory admission, worker grant, first-result time, and completion/close time where inexpensive
counters exist. Share counters with existing instrumentation; batch feedback at execution close and sample costly
timing. Do not introduce per-row maps, strings, object allocations, or clock reads to collect a feature whose
measurement costs more than the expected benefit.

Treat full completion, LIMIT termination, successful existence, exhausted miss, user cancellation, timeout, and
failed execution as different observation classes. A partial observation can describe a censored prefix, meaning
that the unobserved remainder is unknown; it must not train a complete cardinality or total-cost target. Keep
cardinality calibration separate from physical feature correction and feature-to-time pricing. Avoid training a
coefficient on time already embedded in a feature or applying both global and native residual corrections twice.

Preserve existing safety around model uncertainty, cold priors, applicability, and adaptive experiments. Any
exploration performed during normal execution remains bounded and must not remove the only valid fallback. Scope
learning to store/data applicability and relevant machine/concurrency regimes. Version or invalidate incompatible
persisted evidence when dimensions or recipe identities change; never reinterpret old serialized fields under new
units. Retain reusable observations when their meaning is unchanged.

**Proof and exit.** Extend native diagnostics/isolation, adaptive-cost, physical-pricing, and Frontier feedback tests.
Add the proposed `LmdbPhysicalFeedbackAttributionTest` to prove fallback attribution, censored-observation handling,
snapshot isolation, one-time correction, version compatibility, and no parent/child double counting. Under controlled
synthetic observations the model should adjust the intended dimension and change a ranking when justified. Under
real matched fixed-plan measurements, telemetry overhead must stay within the baseline envelope or an explicitly
measured, pre-agreed observation budget. No measured speedup may rely on disabling correctness checks.

### Phase 8 — Qualify the whole system and remove competing formulas


**Dependency and purpose.** Depends on the integration, efficiency, and feedback gates above. Establish improved
end-to-end behavior across the supported workload and leave one maintained costing path for each integrated family.

**Work.** Run semantics, plan-choice, model-accuracy, planning-overhead, and end-to-end performance campaigns as
separate gates. Check complete result bags and required ordering, not just result counts. Audit the current Theme
catalog and generated small-query compositions, plus deliberate adverse distributions and different index layouts.
Measure time to first requested result and full drain separately, including parse/plan/prepare cost in the relevant
end-to-end lane. Keep matched fixed-plan controls and serial-versus-concurrent lanes. Explain per-query regressions;
an aggregate speedup must not hide a severe regression in a fast or frequently cached query.

During development, compare old and new quotes without executing the query twice. Limit any shadow costing to an
explicit diagnostic experiment with its own budget; do not double expensive estimation on every production query.
Move families to the unified contract incrementally after their acceptance gates pass. Keep compatibility adapters
only while a family still needs them, with a recorded owner and removal gate. Remove duplicated formulas, unrelated
ranking overrides, stale recipe reconstructions, and legacy learning adjustments only after their replacements are
validated. Update the current LMDB estimation guide and explanation documentation to describe the actual selected
and executed plan, evidence quality, and search completion scope.

**Proof and exit.** Core and LMDB relevant suites pass, active family coverage is complete for the declared scope,
the small exact oracle agrees with search, snapshots and caches remain correct, and planning resource limits are
observable. Baseline and candidate measurements meet the previously recorded per-lane tolerances, with improved
total latency on the targeted physical-strategy cases. If added planning work exceeds saved execution work, the
phase fails even when the selected plan has lower estimated execution cost. If a family remains a compatibility
path, list it explicitly and keep the overall unification work open; do not claim universal physical optimization.

### Validation matrix and measurable acceptance


**Semantic coverage.** Combine BGP joins with UNION, OPTIONAL, MINUS, EXISTS/NOT EXISTS, nested subqueries, filters,
BIND, VALUES including UNDEF, DISTINCT/REDUCED where supported, aggregates, ORDER BY, LIMIT/OFFSET, and property
paths. Include nested and mixed forms rather than testing each operator only at the root. Cover duplicate rows,
empty inputs, repeated variables, nullable values, expression errors, datatype/language distinctions, and typed
value aliases. Preserve context and inference semantics, snapshot isolation, and cancellation/resource closure.
An unsupported native composition must still produce the correct generic or mixed plan.

**Physical behavior.** Cover broad prefix plus selective residual, useful non-longest-prefix indexes, forward/reverse
adjacency, ordered distinct, run counts, membership, high-degree hubs, skewed predicate distributions, star branches,
cycles, selective and unselective filters, expensive expression evaluation, large/small outer probes, all-miss
existence, full enumeration, factorized consumption, conversion boundaries, and resource-limited parallel/hash
execution. Vary configured indexes and adjacency availability/coverage without changing logical answers.

**Deterministic planning gates.** Recognition performs zero data-row iterations, zero code generation, and zero
execution resource reservations. Metadata quotation charges every visited batch and has a cancellation point.
An unchanged expression/requirement/evidence key is not repeatedly recognized. Optional work never exceeds its
accounted limit except a documented already-started indivisible operation. Provider reservations include scratch
high-water and retained payload. Zero optional budget and mid-search exhaustion return a valid plan with truthful
status. Quote, selected recipe, installed implementation, and executed variant are traceable. Exact-mode tests have
no hidden frontier caps.

**Measured performance gates.** Record planning p50/p95/p99, selected preparation, first result, full drain,
end-to-end p50/p95/p99, allocation bytes, peak/retained planning memory, estimator/metadata work, cache hit/validation
cost, strategy distribution, and actual-to-estimated work by dimension. Tail metrics require an appropriate latency
harness and sufficient samples; an average-time JMH score does not establish p95 or p99. Compare both query-weighted
workload totals and individual important cells. Freeze thresholds from control variability and workload requirements
in phase 0 rather than inventing a universal millisecond cap or accepting whatever regression the new design causes.

**Optimization effort policy.** Use configurable work, deadline, and memory limits already exposed by the planner,
with deterministic defaults justified by the measured baseline. Prioritize incremental evidence by expected decision
benefit and reuse, but retain hard resource bounds and honest incomplete status. Distinguish one-off and genuinely
reused prepared queries when considering amortization. A cheap query should not incur an expensive statistical probe
merely to prove that its obvious plan is cheapest. A costly uncertain query can justify deeper search without
requiring exhaustive exploration for every query. Do not encode benchmark names, query IDs, arbitrary shape sizes,
or a permanently preferred algorithm as production policy.

### Concrete steps for the authorized implementation session


Commands below are the reproducible workflow; their actual outcomes must be recorded in the living sections. Work from
`/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Use the repository's required JDK 25 or newer and
record the exact JVM used for every comparison. Inspect the current checkout and existing evidence before edits:

    git status --short --untracked-files=no
    git log -1 --oneline
    git diff -- .agent/execplans/GH-0000-frontier-estimator-cost-model-roadmap.md

Use the repository's required initial installation before tests:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
      | tee maven-build.log \
      | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Give that installation at least a 60-second tool timeout and follow the existing offline-dependency recovery rules.
For each behavior change, add the smallest relevant failing in-repository test, run it, retain its report snippet,
then change production code. Use the supported test runner, which handles the required dependency installation.
Examples for current tests are:

    python3 .codex/skills/mvnf/scripts/mvnf.py PackedCostAlgebraTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedPhysicalCostContractTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPhysicalWorkPricingRegressionTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeSelectorTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDispatchLoopEdgeCaseTest --retain-logs

The new test names in the phases are proposed files, not existing coverage. Create them in the corresponding core
packed or LMDB test package, with the same package convention as adjacent tests. For example, phase 2's prospective
file is
`/Users/havardottestad/Documents/Programming/rdf4j-small-things/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbUnifiedAccessCostTest.java`.
Select its smallest failing method first, then the class, then relevant cross-module suites. Future selectors include:

    python3 .codex/skills/mvnf/scripts/mvnf.py PackedPlanningBudgetTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbUnifiedAccessCostTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbUnifiedPhysicalPlanningTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPhysicalRecipeBindingTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPhysicalFeedbackAttributionTest --retain-logs

Pass means the specific behavioral assertions described in each phase succeed with zero test failures/errors; a
skipped test does not satisfy a gate. Do not prescribe a fabricated future test count. Retain compact initial
failure evidence and later passing evidence with exact report/log paths in a new, non-overwriting local evidence
artifact named for this work. Preserve any existing `initial-evidence.txt` and all untracked artifacts. Do not use
`-am` or `-q` when running tests. Inspect existing failures before attributing them to this work.

After focused validation, the affected module selectors are:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Add a proposed `LmdbPhysicalPlanningBenchmark` under the LMDB benchmark test package with separate `coldPlan`,
`cachedPlan`, and `endToEnd` methods. Each must consume a verifiable plan/result and control cache state explicitly.
Use fixed-plan execution controls and a separate latency harness when tail distributions are needed. The supported
single-method benchmark invocation, after that class exists, is:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbPhysicalPlanningBenchmark --method coldPlan

Use separate invocations with `--method cachedPlan` and `--method endToEnd`. The existing Theme entry point provides
an end-to-end workload lane, with theme/query parameters recorded explicitly for each run:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery

Run ordinary matched measurements before profiling. If allocation or CPU regressions need investigation, use the
same helper with `--enable-jfr` and its documented fixed profiling settings, keep the JFR path, and do not compare
that profiling run directly to a differently configured timing control. Reuse the existing plan-quality audit
support for alternative identity/result checks; update its recorded invocation if its interface has changed.

### Idempotence, recovery, and delivery boundaries


Keep implementation slices small and source-compatible while migrating a family. Re-running a test or benchmark must
not overwrite the only baseline or initial failure evidence. Name retained outputs by immutable revision and run
identity; never replace tracked source files with recovered artifacts. This plan and working evidence remain local
and uncommitted unless separately authorized for inclusion. Do not change branches, stage, commit, or push merely
because an implementation phase completes.

On a provider failure or budget stop, close the query-local cost session and release borrowed views and scratch
reservations while preserving the valid incumbent. On a bind failure, release unopened/rejected native proposals
and use the recipe's valid equivalent fallback. If correctness assumptions cannot be revalidated, rebuild a valid
plan through the existing safe path within its declared policy; do not reuse a stale proof. Distinguish cancellation
from an invitation to keep optimizing or to run fallback work after the query has been cancelled.

On incompatible recipe or learning-schema versions, invalidate the affected cache/evidence entry through existing
version handling. Do not delete the store, rebuild adjacency unconditionally, or erase unrelated learning/history.
For a failed integration slice, preserve the known-correct compatibility path and narrow the cause before retrying.
No rollback may silence a semantic test or label unavailable evidence exact. Document remaining blockers and their
evidence here instead of marking a family complete.

### Artifacts, priority, and definition of completion


Artifacts must include this roadmap's living sections, the strategy/access catalog, initial failing and subsequent
passing test reports, small-oracle results, quote-to-execution traces, fixed-plan controls, benchmark metadata/results,
and profiling captures when needed. Earlier planning produced source inspection and document checks only; it did not
produce executable evidence for this implementation.

The first implementation priorities are the budgeted shared contract, correct physical access work, and the
selected-to-executed recipe handoff. Consumer properties and retention are part of that first complete integration,
not optional improvements after all algorithms are wired in. Broad strategy expansion and learned coefficient
tuning follow the working vertical slice. Planning efficiency is implemented from phase 1 and measured at every
phase; phase 6 qualifies and improves it rather than introducing it late.

Completion means supported equivalent query shapes compete using the physical implementations actually available;
consumer-compatible representations and ordering survive search; LMDB and adjacency work use the same evidence
contract; the chosen recipe reaches execution or produces an explicit, correctly attributed fallback; and total
user latency improves within validated planning, memory, and telemetry budgets. Correctness and search-completion
claims remain independent of estimation confidence. An estimate that looks logically cheaper, a preferred strategy
name, or a benchmark improvement obtained by ignoring planning time does not satisfy this contract.

Plan revision note (2026-11-06 / Codex): added a source-grounded nine-phase implementation sequence for unified
Cascades, native strategy, and LMDB/adjacency costing, including executable recipe ownership, shared physical work,
consumer requirements, bounded planning, cache validation, and feedback. Updated the required living sections and
explicitly separated this documentation-only task from future implementation and executable validation. The addition
preserves the single-roadmap rule and does not treat historical timings as evidence for the proposed design.

Plan revision note (2026-11-06 / Codex): started the newly authorized implementation, replaced the stale planning-only
status, and moved the sole active progress marker to phase 0. Historical milestones and their evidence are retained.
Recorded the successful initial build and source-grounded baseline inventory; neither establishes a performance or
integration result.

Plan revision note (2026-11-06 / Codex): after preserving the two focused reds above, corrected unknown-axis addition
and implemented a candidate repair for the separately priced dependent-subquery objective. The latter composes the
winner's scalar objective once, preserves the actual local versus inclusive scope, and marks composite physical
quantities unavailable when the subquery supplied only a price. Scheduled filters retain the chosen complete price
instead of asking the provider to price the same partial event again. These changes are not accepted until the
focused and cross-path tests pass; neither a source-prefix fixture setup error nor an unreproduced baseline is a
behavioral red or a performance control.

Plan revision note (2026-11-06 / Codex): an independent reviewer found that the in-progress composite correction
must also preserve the source lifecycle risk premium, and that an explicit vector with unavailable result or peak
memory can reach a strict finite-only reopening calculation. Added targeted regression tests before changing either
path. Independent LMDB source tracing also identified named-context residual and actual-index-selection gaps; their
own in-repository reds are still required. Earlier ordinary benchmark data is invalid because it included fallback;
the unrelated JMH lock owner exited before the subsequently retained access and quantity tests began.

Plan revision note (2026-11-06 / Codex): the first lifecycle and unknown-resource reopening fixes pass their two
focused tests. A subsequent typed-child-plus-scalar-child regression still loses the lifecycle envelope and has
been retained red, as have corrected unknown-visit and real wildcard-index counterexamples; see
`/private/tmp/rdf4j-unified-costing-realstore-validation-20261106T143133Z/`. A real skip-scan fixture independently
showed 1,000 prefix candidates and fewer visited keys, so an exact prefix population cannot stand in for exact
sequential work. The named-graph fixture initially failed before its intended assertion and has been corrected to
represent the provider's lowered graph domain; its actual behavioral outcome is not yet established. Interim
changes are still being validated and no unified phase is marked complete.

Plan revision note (2026-11-06 / Codex): the next focused run passed the independent-subquery lifecycle comparison,
price-only quantity-transcript replay, real-store access/index/context class (5 tests), and repeated-variable access
class (14 tests). It separately captured a source-only reopen-profile red, unavailable-base promotion, and additional
cancellation and cleanup reds in `/private/tmp/rdf4j-unified-costing-final-verification-20261106T155420Z/`.
Candidate corrections now retain lifecycle provenance, decline a partial reopen profile when a child has no supplied
quantities, carry typed Frontier leaf evidence rather than interpreting a diagnostic string, preserve unavailable
coverage, and propagate cancellation while closing both owners. Focused and broader module validation of those
latest changes is in progress; baseline fallback ownership, shared provider budgets, physical selection handoff,
remaining strategy families, cache/feedback, and performance qualification remain open.

Plan revision note (2026-11-06 / Codex): all seven focused classes passed (102, 25, 5, 6, 4, 11, and 14 tests),
but the subsequent complete query-evaluation module reported 125 failures and 53 errors across 7,480 tests. The
full report and symptom grouping are retained under
`/private/tmp/rdf4j-unified-costing-final-suite-20261106T162026Z/08-evaluation-module/`. One failure directly
identified an introduced static collection in packed hot state; its correction is awaiting a targeted rerun.
The other failures span tuple-function bindings, EXISTS, filter rewrites, join ordering/representatives, SERVICE,
deep binding-origin chains and cancellation; their baseline status is unknown. A private immutable HEAD archive
with a separate Maven repository is being used to compare the same selectors and full module without disturbing the
working tree. The LMDB module gate remains unrun; no failure is assumed pre-existing from its name alone.

Plan revision note (2026-11-06 / Codex): the isolated, tracked-only HEAD archive reproduced 177 of the current
module failures/errors with the same class, exact testcase/parameter name, and failure/error status. The only
current-only failure was the introduced packed hot-state collection; its smallest selector subsequently passed after
that collection was removed. The baseline-only documentation error came from a required research file omitted by
the tracked-only archive, so it is an archive-input mismatch rather than a proven branch failure. Both complete
report sets, the 35 additional passing current test invocations, the immutable archive identity, and the exact
comparison are retained at `/private/tmp/rdf4j-unified-costing-baseline-audit-20261106Tn5zeKu/baseline/03-comparison/`.
This comparison qualifies the unrelated evaluation failures by identity; it is not a green module result. The
reviewer's typed-child-profile regression is retained red, and its focused rerun passed after clearing the incomplete
profile on typed-child composition. The packed hot-state layout selector passed after removing the static collection.

Plan revision note (2026-11-06 / Codex): a zero-work request currently opens its optional cost session before checking
the search allowance; the smallest failure is preserved under
`/private/tmp/rdf4j-unified-costing-budget-focused-20261106TCmRl0v/run/`. A separate interrupted-thread fallback
regression expected `QueryInterruptedException` but returned normally, with report and command under
`/private/tmp/rdf4j-unified-costing-budget-cancel-20261106Ta3WdoO/run/`. No budget implementation has been changed
yet. Follow-up cases cover the expired-deadline and zero-retained-memory paths, optional range callbacks, and result
bags for nested UNION, OPTIONAL, MINUS, EXISTS, subquery, and VALUES/UNDEF. The current codec also performs rule
saturation and calls the range provider before the search budget exists; cache identity/validation and continuation
must be accounted for when the fallback boundary is repaired, without misreporting an unexamined provider space as
complete. The first native handoff is concurrently being designed and must carry a detached selected descriptor to
actual index/adjacency execution, with independently observed selection and explicit fallback.

Plan revision note (2026-11-06 / Codex): the six fallback regressions now pass alongside the adjacent search-budget,
deadline, retained-memory, and plan-cache classes (86 tests total); the reports and commands are retained at
`/private/tmp/rdf4j-unified-costing-budget-green-20261106TBYNdpT/`. The tested uncached stop paths check cancellation
and the applicable limit before optional session/range work, encode the executable canonical source without optional
rule saturation, preserve the nested three-row result bag, and expose the number of logical candidates actually
constructed for the fallback. That counter is separate from the optional search allowance; it does not claim to count
every codec visit. This is not phase 1 completion: positive-budget provider internals, cache validation and replay,
all optional codec work, and actual-operation/scratch accounting still require one shared budget owner. A separate
end-to-end LMDB red is being run for natural physical candidate selection and execution observation.

Plan revision note (2026-11-06 / Codex): the new real-store physical-handoff selector compiled and failed on the
missing candidate/quote counts and selected/executed receipts. Its independently constructed 32-row RDF bag passed,
and telemetry observed the actual LMDB `pocs` cursor, variant 2887, scanning and returning 32 rows. The report is
`/private/tmp/rdf4j-unified-physical-planning-20261106Tchtqbp/run/`; it does not yet prove why the candidate count
was absent, and the known snapshot-scope fallback remains a possible contributing condition. Descriptor transport
must include generic and native binding, per-node witness/identity, retention through all LMDB cache ownership gates,
and single-flight lifetime accounting; an explanation string alone cannot enforce execution.

Plan revision note (2026-11-06 / Codex): further regressions showed that a one-unit request previously opened one
provider and made six uncharged estimate/refinement calls, that an expired cached request accessed optional range
evidence while creating its cache identity, and that a zero-budget plan-quality audit dereferenced an absent decision
trace. Red reports are in `/private/tmp/rdf4j-unified-costing-budget-extended-20261106TjuYORW/run/`. A first shared
borrowed-work owner now admits the session before opening and charges opaque compatibility callbacks, while allowing
cooperative providers to reserve internal batches; nested operation charging and all cache/codec paths remain open.
The new focused budget, memory, lifecycle, and search-budget classes passed, but two existing cache continuation
assertions fail because a one-unit request now performs no provider estimation. More importantly, the current
candidate abort path can discard a complete provider incumbent after later refusal; preserve a detached, validated
incumbent before adjusting those fixture budgets. The combined reports are in
`/private/tmp/rdf4j-unified-costing-physical-binding-workflow-20261106TSOeJj6/02-core-qualification/`.

Plan revision note (2026-11-06 / Codex): a further independent control confirmed that a complete provider root can
already exist when optional work runs out, yet the current recovery drops its provider event; the failing assertion
and typed provider-open memory-refusal error are retained under
`/private/tmp/rdf4j-unified-costing-provider-admission-retry-20261106T2015Z/01-core-provider-refusal/`. Four LMDB
provider-work forwarding and pre-open admission failures are retained in the sibling `02-lmdb-provider-admission/`.
The separately exposed portfolio-cancellation and suppressed-close failures have focused greens in
`/private/tmp/rdf4j-unified-costing-shared-cleanup-check-20261106T1940Z/`. A later refusal after the champion was
detached also revealed an exact decision summary attached to an incomplete result; that status correction now passes,
and the regression requires no exact cache hit or stale-validation authority for the incomplete entry. The five
direct named-index tests pass in `/private/tmp/rdf4j-unified-costing-source-stable-20261106T1930Z/01-lmdb-selected-index/`;
the indexed descriptor and range-plus-ID-filter binder remain unfinished.

Plan revision note (2026-11-06 / Codex): an independent Sol review identified two remaining extraction boundaries:
typed provider memory refusal while detaching the selected winner and work refusal while capturing plan-quality
audit challengers must retain the executable winner and an honest incomplete status. A direct/cache/audit regression
matrix is being run before those corrections. The preceding source-stable runs passed 25 LMDB tests and 129 packed
evaluation tests at `/private/tmp/rdf4j-unified-costing-stable-multiclass-validation-20261106T2120Z/`. These are
focused gates, not full-module or performance acceptance. The measured one-open/one-reserved-detach continuation
fixture reached a real provider-backed incomplete checkpoint at work allowance 55 with seven provider callbacks;
the existing continuation assertions were updated to that observed stage and retain their original checks.

The original planning-baseline failures occurred during live pipeline costing, 5.023 seconds apart on the same JMH
worker. A new independent lifetime investigation has identified a plausible no-op asynchronous-maintenance
publication overlap, but no test yet proves it is the failing predicate. Preserve the frozen baseline evidence and
current read-scope guards while establishing a real snapshot/read-view reproduction; performance baselines and
thresholds remain open.

Plan revision note (2026-11-06 / Codex): the Sol review matrix now passes for direct, cache and plan-quality audit
entrypoints under work refusal, provider memory refusal and cancellation, including a later refused audit challenger;
the focused budget/cache/lifecycle classes passed 138 tests under
`/private/tmp/rdf4j-unified-costing-publication-race-core-validation-20261106T2230Z/02-core-classes/`. On a typed
detachment stop, the same complete executable winner is retained without provider runtime state, trace or certificate;
that result is not admitted to the plan cache. A later stop after successful detachment preserves the already detached
champion and synchronizes completion status across the result and summary. This does not authorize stripping an
execution-critical descriptor from a future native candidate.

The real publication/read-view regression now reproduces the exact baseline lifetime exception in explicit,
inferred and combined planes after a no-op publication begins inside an already admitted mapped session. Its fresh
physical-admission refusal assertions pass first, and the retained report is
`/private/tmp/rdf4j-unified-costing-publication-race-core-validation-20261106T2230Z/01-lmdb-race/`. This proves a
triggering condition, not the historical JMH cause. The session-only repair must preserve paired reader/lease,
native growth invalidation, real commit, nested and closed scope, and cancellation checks. The core physical
strategy/descriptor implementation is starting separately; its candidates must possess any execution-critical
detached identity before becoming complete incumbents.

Plan revision note (2026-11-08 / Codex): recorded the phase-1 B/C ownership and exception-boundary review
candidate, its real red/green evidence and unresolved qualification without marking phases 0, 1 or 6 complete.

Plan revision note (2026-11-08 / Codex): recorded the bounded phase-2 index-shape foundation and its matched
compatibility evidence while keeping the remaining phase-1 provider and phase-2 physical-quote dependencies open.

Plan revision note (2026-11-08 / Codex): recorded the shared access-feature boundary and its matched/dependent
evidence without claiming the remaining numeric quote, provider, execution, full-corpus or performance gates.

Plan revision note (2026-11-08 / Codex): recorded the shared quantity review corrections, real red/green projection
evidence, dimensional scopes, current-index qualification and isolated pre-repair comparison. The sole phase-1 progress
entry remains active; this does not establish the remaining phase-1 or phase-2 provider and execution contracts.

Plan revision note (2026-11-10 / Claude): recorded the phase-1 continuation (whole-module qualification, the
separate provider-work limit and anytime invariant, the LMDB fixture triage, replay-only decision caching, the
capacity policy and the entry shrink) in Progress, Surprises & Discoveries and the Decision Log. Nothing is
committed. Phase 1 remains the sole active item. Open items carried forward:

- The written-order emission's access-path expansion draws from the shared bounded budget (503 of 512 search units
  for SOCIAL_MEDIA q10). Options: a two-phase pass (expansions after every region has an incumbent), a
  per-expansion sub-budget, or documenting the current behaviour. Belongs with phase 6.
- A finite default provider limit or time budget for cold planning (phase 6), with JMH numbers on a quiet machine.
- The context-free fallback in `PackedDependentSubqueryCosting` (see Surprises & Discoveries). Not changed. The
  narrow rule that the measurement supports: keep the context-free price for structural cases (that plan is what
  runs once per outer row); return unknown only when the subquery could have been costed contextually but no
  contextual winner exists, in `contextualCost`, `selectedWinner` and `recordScalarPlans` alike; run the missing
  contextual search on demand as mandatory work for the canonical candidate; and make the correlated lattice
  tolerate unpriced transitions. Tests to write first are listed in `spec-contextual-subquery-price.md`. Few real
  queries are affected, so this belongs with the phase-3 consumer-sensitive costing work.
- The tier-dominance rule, and contextual evidence from the LMDB provider for OPTIONAL right sides and dependent
  joins (phases 2–3). Suggested exit measure: the share of theme queries with a COMPLETE comparison (31 of 176 in
  exact mode when measured).
- The decision cache's share of the evidence budget and frequency-aware eviction: a workload that cycles through
  more distinct queries than fit gets no hits under least-recently-used eviction.
- LMDB does not publish `providerWorkUnits` on the installed plan; refreshed ranked cache versions are still charged
  by the old model; the retry after an absorbed refusal re-prices pending offers but does not re-search the regions
  that contain them.
- Pre-existing reds not triaged: 176 in the core module (all red at HEAD: TupleFunction* 113,
  ExistsFilteredProbePreparationTest 20, filter/join optimizer and SERVICE tests); in LMDB the post-commit
  receipt-settlement tests (need per-service `Error` handling in `LmdbSailStore` post-commit maintenance), two
  `LmdbPostCommitLearnedStateFailureTest` cases (test-only wait), `LmdbFilterSamplingSafetyTest` ×3,
  `LmdbDeferredProviderRealQueryRegressionTest` ×2, `LmdbProviderPublicationCacheTest`, the plan-quality audit
  rejecting a `Filter` operator, about 21 stable and a few intermittent reds in `LmdbPackedPlanCacheTest`,
  `LmdbPlanCacheCommitInvalidationTest`, `LmdbForegroundFilterSamplingEpochTest`,
  `LmdbGenericFilterFactorReadContextTest` and `LmdbCurrentAliasCacheIdentityTest` (all red before this work;
  `evidence/counterfactual-precache-lmdb-cache-*`), and the phase-4 handoff tests
  (`LmdbUnifiedPhysicalPlanningTest`, `LmdbPhysicalRecipeBindingTest`).

Plan revision note (2026-11-11 / Claude): recorded the merge and requalification (requal11) of the constant-time
applicability index and of stable learning keys, the accepted one-time re-keying of `FILTER EXISTS` logical keys,
the state of cache-validity step 2 (phase A built and reviewed in a private copy, nine review fixes and the memory
design open, phase B not specified in detail yet), and three decisions. Unified phase 1 stays the only item in
progress; the original phase-1 exit slices (recognition purity, budget-charging gaps, dimension mapping and algebra
laws, inactive LMDB provider conformance, nested-context design) are still not started.
