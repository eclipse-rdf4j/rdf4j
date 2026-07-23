# Harden Cascades and hypergraph search to reference-grade behavior

> **SUPERSEDED (2026-07-22)** by `.agent/execplans/GH-0000-packed-idempotent-cascades.md`.
> The 2026-07-20 packed hard cutover deleted the legacy memo/IR this plan was hardening; every class this
> plan introduced (`MemoLogicalExpressionKey`, `JoinRegionMemo`, `JoinCardinalityModel`, `CostOrdering`,
> `JoinEdge`, `ConflictRule`, `CostingReceiver`, `PlanHypergraph`, `JoinStateEnumerator`) no longer exists in
> Java source. Item-by-item disposition of the open M4–M8 work is recorded in
> `.agent/execplans/GH-0000-unified-next-steps.md` (Workstream D): typed identity, scoped/bounded enumeration,
> deterministic AUTO, and opaque-factor handling are satisfied by the packed implementation; log-domain
> cardinality and typed non-inner join edges were dropped as mechanisms (goals met differently or deferred);
> the durable residue (EXACT completeness reporting, dead `RESOURCE_LIMIT_EXCEEDED`, non-inner rewrite-legality
> parity tests, medical baseline re-baseline, JMH/JFR measurement) is folded into that plan's Workstreams C/D/E.
> This document remains historical evidence only.

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current as implementation proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

This plan supersedes the search-mode, DPhyp-export, cost-risk, and legacy-cutover decisions in
`.agent/execplans/GH-0000-unified-self-reactivating-query-optimizer.md`. That earlier plan remains useful historical
evidence, but this document is authoritative for every change described below.

## Purpose / Big Picture

RDF4J's new Cascades optimizer already wakes rules after dependent memo changes and can accept both generic and LMDB
join contributors. Its join path still has three correctness and scale risks. Logical equivalence is represented by a
diagnostic string assembled partly from mutable `TupleExpr` objects. Join topology and predicate states are expanded
into ordinary memo expressions before the useful root plans are known. Cardinality calculations multiply and divide
ordinary `double` values in an order that can underflow, overflow, or make the same logical subset receive a different
estimate under a different join association.

After this work, a query has one typed semantic identity, one canonical cardinality for each logical join state, and
one property-aware cost comparison. DPhyp and generic exhaustive enumeration feed a bounded, core
`JoinRegionMemo`; only the derivation directed acyclic graphs reachable from retained root candidates enter the global
Cascades memo. `AUTO` always finishes according to deterministic work counters and applies a legal fallback even for
arbitrarily large regions. Explicit `EXACT` either proves the selected plan exhaustive or reports
`RESOURCE_LIMIT_EXCEEDED` without changing the input algebra. Specialized inner, OPTIONAL, EXISTS, NOT EXISTS, and
MINUS logic can add legal alternatives, but no specialized route can disable generic optimization.

A user can observe the result by repeating an `AUTO` optimization and seeing the same winner and exact counters,
setting `timeoutMillis=1` without changing that result, and requesting `EXACT` for a 16-factor clique and seeing an
unchanged algebra plus `RESOURCE_LIMIT_EXCEEDED`. The medical theme query must retain its finite VALUES relation before
the broad `med:value` access and must not allocate search state proportional to every CSG-CMP pair.

## Progress

- [x] (2026-07-16 07:37Z) Read `.agent/PLANS.md`, the high-performance Java guidance, and the isolated Maven runner
  contract for this revised implementation.
- [x] (2026-07-16 07:37Z) Ran the required clean root `-Pquick` install from the current branch; the complete reactor
  reported `BUILD SUCCESS` in 32.594 seconds.
- [x] (2026-07-16 07:37Z) Created this governing ExecPlan and recorded the three decisions superseded by the review.
- [x] (2026-07-16 07:44Z) Inventoried the complete public property surface, current mode routing, memo equivalence
  paths, generated-record equality use, and legacy/core cardinality formulas without changing source.
- [x] (2026-07-16 08:30Z) Locked eight-factor CSG-CMP pair counts in `SubgraphEnumeratorTest`: chain 84, star 448,
  cycle 196, and clique 3,025. The focused workspace selector passes; LMDB verification explicitly skips the known
  unrelated current `japicmp` incompatibility.
- [x] (2026-07-16 11:16Z) Captured the current medical-theme planner outcomes under EXACT, ROBUST, and
  FEEDBACK_AWARE. Thirty query/policy combinations produced stable rows: queries 0, 1, 6, and 8 complete under all
  three policies; queries 2, 3, 4, 5, 7, and 10 expose explicit unsupported atomic boundaries with policy-invariant
  pending counts. Query 9 failed to return under every policy, ignored the 180-second JUnit interrupt, and was
  externally terminated with Surefire exit 143 after remaining CPU-active beyond the guard.
- [x] (2026-07-16 08:34Z) Added the first typed-identity regression and preserved its failing workspace report before
  production edits. Opposite Projection source mappings incorrectly returned the same memo group ID `1`; an exact
  clone still deduplicated as required.
- [x] (2026-07-16 08:46Z) Locked the LMDB property surface in five focused characterization tests. The public input
  alias remains `shadow-budgeted`, while the existing plan annotation intentionally renders `shadow_budgeted`.
- [x] (2026-07-16 08:46Z) Expanded the typed-identity red to the full memo class: 57 tests ran, with only the four
  intended collisions (Projection mapping, context, subquery state, and opaque occurrence). Captured a separate red
  proving Projection IR drops `projectionContext`; explicit memo scope identity was already green.
- [x] (2026-07-16 09:10Z) Made `MemoLogicalExpressionKey` authoritative in every logical insertion path, completed
  Projection/scope IR round-tripping, retained a bounded assertion-only legacy discrepancy recorder, and removed the
  extractor's operator-name fallback. Focused results are green: memo 57/57, IR round-trip 18/18, and join extractor
  10/10.
- [x] (2026-07-16 09:35Z) Closed Milestone 1 against a clean `HEAD` archive. The complete queryalgebra module has the
  same five failures and three errors as `HEAD`, with no additional failures from typed identity. Logical equivalence
  and execution-route identity are now separate: physical child requirements and semantic-barrier routing do not
  split logical groups, while unregistered native subclasses receive distinct opaque occurrences.
- [x] (2026-07-16 10:08Z) Implemented canonical log-space cardinality in core and routed both exhaustive DPhyp and
  greedy legacy costing through direct logical-state estimates. The shared model owns same-label maximum-spanning
  forests; positive underflow/overflow saturate, exact zero remains distinct, directional/geometric evidence stays in
  logs, invalid primary/fallback evidence fails closed, and positive seed estimates are no longer floored. Focused
  results are green: shared model 5/5, hypergraph costing 17/17, and LMDB DPhyp planner 25/25.
- [x] (2026-07-16 10:40Z) Closed the independent cardinality audit. Proven-zero equality evidence now dominates an
  estimated forest; diagnostic labels no longer define predicate semantics; unavailable subset states fail closed;
  conditional fallback uses prefix-plus-factor row units; physical lookup evidence cannot overwrite canonical rows;
  and tiny positive nested prefixes are not floored. The broadened shared-model, hypergraph-costing, and LMDB planner
  classes pass 6/6, 20/20, and 28/28 tests respectively.
- [x] (2026-07-16 11:16Z) Closed the medical cost-policy baseline before changing cost policy. All 30 outcomes that
  reach extraction have policy-invariant semantic and physical fingerprints and root rule
  `generic-physical-implementation`; 12 are `COMPLETE` and 18 are `UNSUPPORTED_ATOMIC_BOUNDARY`. The remaining three
  combinations are the independently reproduced query-9 non-completions described above, not missing observations.
- [x] (2026-07-16 12:03Z) Added the typed required/provided property foundation: scalar-expression `SortKey`, query-local
  binding masks for distinctness and parameterization, stable `PhysicalOperatorId`, explicit compatible/conflict
  intersection, ordering-prefix satisfaction, and fail-closed parameterization direction. The two focused classes pass
  5/5 each.
- [x] (2026-07-16 12:03Z) Hardened the three DSL enforcers before cost migration. Each preserves unrelated child
  requirements; DISTINCT explicitly delivers duplicate elimination; Sort is goal-driven; Materialize is a zero-local-
  work memo marker whose selected extraction detaches the returned root and transfers winner metrics. Enforcer
  applications now carry authoritative child group IDs. The former ordering-plus-materialization heap exhaustion now
  completes with no pending work or opaque Materialize group.
- [x] (2026-07-16 12:31Z) Split raw `ExecutionCost`, `CardinalityEstimate`, and `EstimateRisk`, and made
  policy-bound `CostOrdering` the memo frontier's only risk-transform owner. EXACT/BOUNDED rank raw resources,
  ROBUST adjusts raw work once, FEEDBACK_AWARE gates that same adjustment at confidence 0.55, evidence no longer
  forks exact states, stable `PhysicalOperatorId` resolves equal-resource ties, and work/memory tradeoffs remain in
  the frontier. The planner no longer invokes the store cost model's legacy policy hook. Focused suites are green:
  cost ordering 6/6, memo model 60/60, rule specs 30/30, cost model 92/92, selected materialization 2/2, and enforcer
  composition 1/1.
- [x] (2026-07-16 12:51Z) Added the first scoped join-region search foundation without changing production routing.
  `JoinSubset` remains collision-free above 64 factors; typed subset/predicate/outer-input states own dense local
  groups; property-aware frontiers retain exact executable-resource tradeoffs, forbid a more-parameterized winner
  from dominating an independent one, and permit a later cheaper AUTO candidate to evict a retained state after
  truncation. Root export follows only the selected candidate DAG. Deterministic work and retention ledgers charge
  before work, atomically account DPhyp and total partition probes, never overshoot, and preserve multiple causes.
  Focused suites pass 6/6 and 4/4 respectively.
- [x] (2026-07-16 13:26Z) Added structured search outcomes and deterministic scheduler limits. AUTO, EXACT, BUDGETED,
  SHADOW, and SHADOW_BUDGETED now resolve separate core modes; rule work charges before execution with no overshoot;
  multiple limit causes, exact counters, retention occupancy, and pending work survive the plan boundary. The original
  topology and one executable implementation are reserved outside exploration. LMDB AUTO ignores timeout/budget
  overrides, and incomplete EXACT is transactional while AUTO may apply its best legal winner. Focused core and LMDB
  contract suites pass.
- [x] (2026-07-16 13:34Z) Repaired selected-plan/provenance alignment exposed by the deterministic-mode cutover.
  Removing a memo-only Materialize marker now collapses its provenance layer at every depth while retaining selected
  cost/estimate annotations; the full LMDB Cascades contract passes 13/13, including Difference and all supported
  composite shapes.
- [x] (2026-07-16 14:14Z) Wired one persistent deterministic work ledger through core join enumeration. Raw generic
  partition callbacks charge before memo deduplication, exact retries preserve the same counter state, and a hard
  join limit stops before overshoot without consuming the rule-work budget on futile retries. The scoped frontier now
  checks projected occupancy transactionally, permits zero-growth late replacement at the group cap, preserves
  operator-specific alternatives, and assigns IDs only to accepted states. The combined five-class selection passes
  27/27 tests.
- [x] (2026-07-16 14:13Z) Closed the remaining transactional EXACT leak at the LMDB boundary. An incomplete exact
  winner no longer annotates unchanged input nodes with unapplied selected-plan provenance; root completeness,
  causes, and counters remain observable. The focused LMDB contract is green.
- [x] (2026-07-16 14:46Z) Replaced predicate-free global join-state materialization with a persistent scoped
  property-aware search. Generic partitions remain local, are implemented through the core physical join rules and
  cost model, and only selected root-reachable derivations are exported. The first five-factor AUTO regression moved
  from 180 globally materialized route joins to a bounded selected closure and passes. Base factors are seeded from
  compatible global winner frontiers rather than requiring a synthetic `ANY` frontier.
- [x] (2026-07-16 15:20Z) Hardened scoped-search lifecycle and executable contracts. Exact retention exhaustion now
  reports `RETAINED_STATE`; superseded and partially seeded sessions release occupancy; compatible access-path
  factors preserve their actual `PhysicalOperatorId`; local candidates reactivate when new subset states appear; and
  physical joins reject child winners that do not satisfy the rule application's declared requirements. Rejected
  implementations still consume exactly one candidate-evaluation unit. The combined six-class contract selection
  passes 31/31 tests.
- [x] (2026-07-16 15:30Z) Closed two scoped completeness gaps. Provider implementation IDs are validated as a
  complete batch before any candidate work is charged, so duplicate IDs cannot silently suppress alternatives.
  Property-constrained searches export a bounded relaxed root DAG when a registered enforcer must supply the final
  property, and topology saturation now requires at least one exportable root. Strengthened frontier tests use one
  physical operator identity and prove retention independently on resource and parameterization dimensions.
- [x] (2026-07-16 15:59Z) Added the core primitive DPhyp seam and connected the real LMDB contributor. The scoped
  route now charges each raw CSG-CMP callback through the shared deterministic ledger before evaluating both physical
  orientations in `JoinRegionMemo`; LMDB graph legality and canonical cardinality preparation are shared with the
  retained legacy winner adapter. A three-factor clique
  streams all six pairs and the full contributor class passes 6/6 tests. Regions above the configured DPhyp
  eligibility or 64-bit capacity fail before graph construction; generic exhaustive search remains available.
- [x] (2026-07-16 16:55Z) Corrected the scoped DPhyp boundary after a probe-rejecting regression showed the first
  adapter still entered legacy `prepareGraph`. The primitive route now builds a payload-free simple connectivity
  graph from typed factor bindings, consults no private factor/cardinality/selectivity evidence, and preflights every
  singleton before the first charged callback. Dependency-bearing or unresolved-input regions fail closed to the
  generic legal enumerator until directional typed edges exist. The focused red was preserved; the identical three
  tests now pass 3/3 and the full contributor class passes 9/9.
- [x] (2026-07-16 16:55Z) Closed scoped-search class triage. A scheduler fixture that demanded a tenant-bound lookup
  while supplying scan-only `EvaluationStatistics` now provides a real physical lookup model; selected root export
  is asserted against the reachable DAG rather than obsolete global subset materialization. The complete
  `CascadesJoinRegionEnumerationTest` passes 12/12.
- [x] (2026-07-16 17:29Z) Reproduced the real 14-factor LMDB delete/insert query as a focused repository test. The
  scoped provider no longer embeds complete child plan trees in every intermediate candidate and releases detached
  physical cost caches after evaluation. Compact scoped input references reduced a 90-second 4 GiB heap exhaustion
  to a 25.372-second passing test; provider regressions prove three-node intermediate templates and zero retained
  transient physical cache entries.
- [x] (2026-07-16 17:56Z) Centralized the OPTIONAL plus negated-BOUND anti-join proof on immutable IR facts. Incoming
  tested bindings now reject the rewrite, VALUES assurance is the intersection across rows rather than RDF4J's union,
  total Extension expressions propagate assurance, and aggregate outputs remain unassured without a typed proof.
  The MINUS prefix rule now reuses the shared null-safe child-alternative helper. The focused regressions pass 4/4 and
  the broadened stream/rule/IR/provider cross-section passes 243/243.
- [x] (2026-07-16 17:56Z) Repaired scoped base-factor goal identity. External bindings are projected through each
  factor's possible-binding mask instead of being erased by a synthetic `ANY` goal, and the same per-factor key now
  drives optimization, scoped seeding, and dependency revisions. A physical subject-bound `enc` lookup remains an
  external root requirement while the scope-local `obs` join key does not leak.
- [x] (2026-07-16 18:47Z) Made the arbitrary-size deterministic fallback physically executable before exploratory
  limits without exempting the whole topology from accounting. Exactly one implementation per left-deep link is
  reserved; later implementations are charged normally and stop before a zero candidate limit. External goal inputs
  are propagated into both child contracts while only a declared selected-prefix RHS may consume assured LHS
  bindings. The combined scoped-search selection passes 38/38 tests.
- [x] (2026-07-16 19:08Z) Replaced the fallback selector's repeated requirement-mask clones, full-array rescans, and
  memo-expression prefix construction with a structured primitive `SeedOrder`. Dense remaining-factor state,
  dependency counters, required-binding consumers, and sparse incident adjacency give exact deterministic operation
  counters; a 128-factor chain performs 8,256 selection visits, 254 adjacency-arc visits, and one row/work lower-bound
  read per factor. Unschedulable seeds report a heuristic `UNSUPPORTED` attempt and never suppress a completed
  exhaustive contributor. Production installs the order directly and retains only dense group-ID triples for later
  charged exploration. The broadened scoped-search selection passes 41/41 tests.
- [x] (2026-07-16 19:13Z) Established the first opaque-factor parity contract without changing routing. Core now has
  an immutable `OpaqueFactorDescriptor` and `OpaqueJoinFactorPolicy`; LMDB classifies a supported correlated
  `Extension` by reusing the legacy required-input, unsafe-name, and stream-schema derivation. The descriptor records
  `seed` as required, `xs` as unsafe, `person/x/xs` as possible, and `person/x` as assured. The complete legacy opaque
  planning class remains green at 22/22 tests.
- [x] (2026-07-16 19:45Z) Completed the descriptor-level opaque parity matrix without changing route ownership.
  Extension, ordinary Filter, EXISTS/NOT EXISTS Filter, LeftJoin, Difference, Union, scope-changing Projection,
  Service, Group, and Lateral now share the legacy possible/assured binding, required-input, unsafe-name, and semantic
  barrier facts. Scalar-subquery locals remain outside the tuple schema and are carried as occurrence hazards;
  correlated names supplied by the Filter input are not. The broadened legacy characterization passes 33/33 tests.
- [x] (2026-07-16 20:30Z) Moved scalar-free predicate closure into `JoinRegionMemo`. Predicate state is part of each
  scoped group, eligibility and candidate counters charge once at the correct boundaries, unary predicate recipes
  remain root-DAG reachable, and each logical `(subset, predicates)` state receives one cached canonical log-space
  cardinality. The planner now exports scalar-free Filter-over-Join regions from the scoped route while scalar
  subqueries remain behind the legacy semantic barrier. Focused provider and planner classes pass 7/7 and 13/13.
- [x] (2026-07-16 20:50Z) Removed the profiled quadratic `Set<String>` binding-schema walk from scalar-scope
  barriers. A focused 192-factor call allocated 77,731,648 bytes before the change. `BindingShapeAnalyzer` now derives
  possible, assured, nullable, and local-output facts directly as query-local masks with an invocation-local identity
  cache; scalar scope analyzes each factor once and checks external subquery dependencies by mask subtraction and
  intersection. The Set schema is only a compatibility materialization boundary, the UNION factoring rule preserves
  its existing memo universe, the focused allocation ceiling is green below 2 MB, and the broadened schema/shape and
  exact UNION tests pass 12/12.
- [x] (2026-07-16 21:28Z) Closed bitmap-analysis parity gaps exposed by independent review. Projection aliases and
  local outputs now match canonical IR; constant-valued path and exact RDF-star terms stay input constraints; VALUES
  assurance is restricted to the declared schema without constructing a name set for every row; empty masks cannot
  prove the negated-BOUND anti-optional form; and BIND scalar references enter the dense universe directly without a
  Set-to-mask round trip. Focused red reports are preserved, and the broadened shape/schema classes pass 16/16.
- [x] (2026-07-16 22:05Z) Removed the remaining repeated string-schema construction from join-search preparation.
  Scoped predicate closure precomputes required and factor-assured masks and caches subset assurance; the arbitrary-
  size greedy seed consumes dense symbol IDs without a temporary `TreeSet<String>` or `Map<String,Integer>`; extractor
  fragment identity, filter transparency, and scalar-producer intersections use memo-owned masks; and the retained
  non-pure state route caches assured masks per occurrence subset. Focused pre/post behavior checks remain green above
  bit 63, names are now materialized only at compatibility or diagnostic boundaries, and the broadened five-class
  bitmap/join-search selection passes 50/50 tests.
- [x] (2026-07-16 22:27Z) Closed and profiled the reported hash/rewrite allocation chain. Scoped hash eligibility and
  selected-prefix propagation now consume shared `BindingShape` masks, compact memo references lazily materialize name
  sets only through the `TupleExpr` compatibility API, and the ordinary hash implementation intersects memo-group
  masks. A JDK 26 allocation trace for medical query 5 contains neither the reported
  `Set.copyOf -> StreamBindingSchema -> barrier` chain nor a `StreamBindingSchema` call below hash-rule matching. The
  matching unprofiled 3x2-second JMH run improved from 95.569 to 67.018 ms/op (-29.875%).
- [x] (2026-07-16 22:46Z) Fixed the invocation-mass parity failure exposed while broadening bitmap coverage. A
  context-total ten-row VALUES winner was correctly costed at 496,000 rows but then stored a contradictory ten-row
  `selectedOutputRows`; downstream prefix composition therefore dropped the inherited 49,600 invocations. Winner cost
  and selected-output cardinality now retain one scope, the exact ten-row relation remains profile evidence, and both
  the direct constructor regression and original nested-prefix regression pass. A conservative cross-topology
  retention experiment was removed after this root cause was proven, preserving canonical cost dominance.
- [x] (2026-07-16 23:05Z) Removed three residual bitmap-path ownership defects. Scoped physical evaluation no longer
  retains `Winner -> Shape` entries for rejected candidates: possible and assured masks now belong to factor/subset
  state and flow through `JoinImplementationProvider.Input`. Scalar correlation discovery recursively inspects
  transparent wrappers, with subquery Projection isolation and same-factor alias guards. Memo schema validation uses
  an ID-preserving private universe overlay, compares masks directly, and materializes names only for a mismatch while
  preserving rejection atomicity. The combined focused suite passes 103/103 and the full rule engine passes 172/172.
- [x] (2026-07-16 23:30Z) Removed the final `StreamBindingSchema` boundary from repeated cost application. Each memo
  expression now owns one query-local `BindingShape`; selected-expression and canonical-group output masks remain
  distinct through property restriction and finite-relation row selection; the compatibility name set is cached once
  per expression. Immutable physical-property sets and complete `BindingProfile` fact surfaces are cached as universe-
  local masks. The new 193-symbol allocation regression initially measured 1,280,000 bytes per 10,000 restrictions
  and then exposed 4,320,000 bytes from empty map views in the broad suite; it is now green together with the affected
  seven-class selection at 368/368 tests. The valid in-process JDK 26 allocation flamegraph contains no reported
  `Set.copyOf -> StreamBindingSchema -> barrier` chain, and `StreamBindingSchema` accounts for only 0.07% of sampled
  allocations before this final boundary removal.
- [x] (2026-07-16 23:49Z) Restored dependency-driven scheduler ordering after broad module verification exposed four
  physical-frontier wake regressions and one self-recost. Exact-like search now drains the initial logical revision
  before reserving a fallback, newly awakened expression work outranks costing, and a sibling frontier event no longer
  impersonates an expression-added event. Goal-driven enforcers declare the required property dimension they supply,
  so a transparent Materialize implementation is not rematched under its own relaxed child goal. The six focused
  dependency-revision tests and the dependent-parent cost-ledger regression are green. A completed dependent traversal
  also seals the current parent fact revision, preserving later external reactivation without replaying facts it
  synchronously published itself.
- [x] (2026-07-16 23:56Z) Closed scoped candidate-retention ownership. Monotonic candidate IDs now index tombstoneable
  payload slots; primitive frontier and live-parent reference counts retain an evicted child only while a selected
  derivation still reaches it, then reclaim the complete dead sub-DAG iteratively. Admission projects that cascade
  transactionally, so the group-wide limit and shared retention ledger measure live candidates rather than only
  frontier entries. Released candidate callbacks null the matching indexed `Winner`, predicate closure skips stale
  queued IDs, and close clears all payload tables. The focused red is preserved in
  `initial-evidence.optimizer-retention.txt`; the identical selector passes, and the final memo/provider/work-limit
  selection passes 29/29 in workspace run `20260716T235516.084769Z-81645-0b171c81`.
- [x] (2026-07-17 00:06Z) Unified scoped JOIN implementation discovery with the active Cascades rule registry. Every
  matching registered implementation application now competes in `JoinRegionMemo` under the shared cost model;
  applications are given stable rule/ordinal identities and fail closed unless they preserve a transparent ordered
  pair of optimizer-local child references and two explicit child contracts. Predicate closure retains its existing
  generic implementation path. The scoped-only regression was preserved in
  `initial-evidence.optimizer-provider.txt`; its identical selector passes 1/1, the full join-region class passes
  14/14, and the provider class passes 9/9 in workspace `optimizer-provider`.
- [x] (2026-07-17 00:31Z) Made the core extractor consume LMDB's immutable opaque-factor policy without changing the
  policy-free compatibility route. A classified composite becomes an unpinned atomic factor only when it has no
  semantic barrier or required outer input, its possible-binding mask exactly matches the memo surface, its assured
  mask is a subset of memo assurance, and its unsafe mask is disjoint from every sibling. Unknown or unsafe
  composites fail the whole candidate closed as `NON_JOIN_COMPOSITE`; the factor retains its transparent memo child.
  The runtime red is preserved in `initial-evidence.optimizer-opaque.txt`; the identical selector passes 1/1, the
  complete opaque parity class passes 34/34, and the core extractor/service classes pass 25/25 in workspace
  `optimizer-opaque`.
- [x] (2026-07-17 00:45Z) Completed a Routine B mask-bridge allocation slice in workspace
  `optimizer-mask-bridge`.
  `CascadesRuleEngineTest#projectionFilterPushdownKeepsConditionVars` directly reaches
  `CascadesRewriteSupport.mask` through the Projection filter-pushdown match predicate. The identical selector is
  green before and after the edit (1/1 each), and the full rule-engine class passes 172/172. The bridge now delegates
  directly to `BindingUniverse.maskOf`, removing its redundant planner-name Set materialization while preserving the
  universe's null, blank, and `_const_` filtering. Exact reports and call-chain hit proof are preserved in
  `initial-evidence.optimizer-mask-bridge.txt`.
- [x] (2026-07-17 00:53Z) Completed the mask-native EXISTS import slice in workspace `optimizer-exists-mask`.
  `TupleExprToIr.exists` now collects tuple-subquery references directly in the import's query-local universe and
  derives correlation by intersecting that mask with the visible-binding mask; it no longer constructs referenced,
  visible, and correlated string sets only to convert them back to masks. The matching multiword selector passes 1/1
  before and after, and the complete IR round-trip class passes 19/19. Exact Routine B evidence and hit proof are in
  `initial-evidence.optimizer-exists-mask.txt`.
- [x] (2026-07-17 00:44Z) Resolved the apparent budgeted OPTIONAL scheduler regression as a rolling-telemetry test
  defect, not a missing physical alternative. The selected winner already delivered `optionalAnchoredLookup` and
  carried the `lmdb-optional-rhs-anchored-lookup` proof; 4,096 later trace entries had evicted its earlier acceptance
  line. The regression now asserts the selected access path and proof directly while retaining the trace only as a
  diagnostic. No budget, timeout, scheduler, scoped-search, or opaque-policy code changed. The identical selector
  passes 1/1, its LMDB class passes 56/56, and the core scheduler selection passes 182/182 in `optimizer-budget`.
- [x] (2026-07-17 00:45Z) Replaced scoped duplicate-suppression `HashSet` records with one lifecycle-aware primitive
  evaluation ledger. A bounded string-to-ordinal map interns each distinct implementation identity once; two-long
  open-addressed tables store join and predicate keys, while a candidate-ID bitmap makes release O(1). Stale keys
  become tombstones lazily and are compacted at the current capacity before any growth, so released candidate
  generations reuse storage. The release red is preserved in `initial-evidence.optimizer-ledger.txt`; the focused
  ledger class passes 2/2, the initial related selection passes 31/31, and the expanded seven-class join-search
  selection passes 64/64 in workspace run `20260717T004408.294420Z-82211-d03cd5d9`.
- [x] (2026-07-17 00:53Z) Completed allocation-audit slice P0(9) in workspace `optimizer-branch-mask`.
  `branchLocalBindOrValuesMask` now visits Extension/BIND and BindingSetAssignment/VALUES outputs directly into one
  mutable query-local symbol bitmap, then transfers its words to `BindingMask`; it no longer builds a `HashSet` and
  immutable string Set on the mask path. Subquery Projection and scope-changing Union retain their exact no-descent
  barriers, while `branchLocalBindOrValuesNames` remains the diagnostic/compatibility path. The matching scoped-UNION
  selector passes 2/2 before and after the edit, and the full rule-engine class passes 172/172. Exact logs and hit
  proof are preserved in `initial-evidence.optimizer-branch-mask.txt`.
- [x] (2026-07-17 01:07Z) Completed the Routine B cost-bound-variable allocation slice in workspace
  `optimizer-cost-boundvars`. `PhysicalProperties` and `InputBindingContext` already supply canonical immutable
  binding-name sets, and recursive cost estimation propagates those sets or `Set.of()`. The cost model now resolves a
  goal's required/contextual union once, retains that stable set identity for the query-local model lifetime, and
  reuses `BindingUniverse.maskOfStableSet` rather than defensively copying names for every `BoundVarsKey`. The matching
  contextual refinement selector passes 1/1 before and after, and `CascadesCostModelTest` passes 93/93. Exact evidence
  and hit proof are preserved in `initial-evidence.optimizer-cost-boundvars.txt`.
- [x] (2026-07-17 01:20Z) Kept descriptor-backed LMDB DPhyp topology and dependency eligibility mask-native in
  workspace `optimizer-dphyp-masks`. The Set-based compatibility context now canonicalizes its descriptors once into
  one query-local universe; the typed constructor retains its shared universe and masks without rebasing. Factor
  overlap, required-input supply, path endpoint eligibility, finite-anchor bridges, and nested-prefix accumulation use
  `BindingMask`; names are materialized only at legacy estimator and PlanHypergraph label boundaries. The new 70-factor
  compatibility regression failed by decoding `binding1` as `binding0`, then passed above bit 63. The matching LMDB
  descriptor selector passes 1/1 before and after, the context and planner classes pass 3/3 and 28/28, and the scoped
  triangle still streams exactly six CSG-CMP pairs. Exact logs and hit proof are preserved in
  `initial-evidence.optimizer-dphyp-masks.txt`.
- [x] (2026-07-17 01:23Z) Added the first typed non-inner search slice without changing legacy ownership: an assured
  root two-factor `Difference` with atomic children becomes one directed `ANTI` edge plus an explicit left-before-
  right dependency. The extractor reuses the registered MINUS assured-shared-domain proof; scoped search invokes the
  registered Difference physical implementation and exports a transparent ordered `Difference`, never a commuted or
  substituted operator. Declined optional discovery is `NOT_APPLICABLE`, and `UNSUPPORTED_ROOT` is likewise
  non-owning; only established semantic barriers and cycles can block completeness. The focused TDD red is preserved
  in `initial-evidence.optimizer-anti.txt`; the late-alternative regressions pass 2/2, the seven-class core selection
  passes 234/234, and seven focused LMDB MINUS compatibility tests pass with the unrelated japicmp check skipped.
- [x] (2026-07-17 01:42Z) Added the planning-only Milestone 8 JMH harness without production changes. The synthetic
  matrix covers 4/8/11/12/16 factors, chain/star/cycle/clique/disconnected factor graphs, 0/4/8 scalar-free
  predicates, and AUTO/EXACT modes; ordinary verification constructs all 150 cases but never plans a dense 16-factor
  clique. Invocation setup clones the immutable template before timing, `CascadesPlanner.optimize` is the only timed
  call, and invocation teardown publishes deterministic search, retention, global memo, and root-DAG export metrics
  through JMH auxiliary counters. The fixture passes 2/2, the benchmarks profile packages and lists the method, and a
  4-factor CHAIN/AUTO diagnostic smoke reports one complete plan, 64 partition probes, 115 candidate evaluations,
  retained high-water 26, 32 memo expressions, and 5 exported join recipes. The non-forked cold timing is explicitly
  not performance evidence and supports no regression claim.
- [x] (2026-07-17 01:46Z) Added the first typed `LEFT` search slice without changing broad OPTIONAL ownership. An
  unconditioned, non-scope-changing root `LeftJoin` with exactly two atomic transparent children becomes one directed
  `LEFT` edge with a right-after-left dependency. Scoped costing consumes the registered LeftJoin output estimate
  directly, and the shared implementation provider exports the original transparent ordered `LeftJoin`; no reverse
  orientation or OPTIONAL swap is legal. Conditioned and composite-child forms decline as non-owning
  `NOT_APPLICABLE` boundaries with explicit reasons. The focused TDD red and green are preserved in
  `initial-evidence.optimizer-left.txt`; the typed semantics class passes 7/7, the affected seven-class core selection
  passes 238/238, and four broad LMDB optimizer/OPTIONAL classes pass 132/132 with only the unrelated japicmp check
  skipped.
- [x] (2026-07-17 01:47Z) Removed repeated legacy name-set reconstruction from candidate delivered-property
  restriction in workspace `optimizer-provided-masks`. Candidate output masks now clamp propagated child facts before
  `PhysicalProperties.mergedWith`; the child statistical profile is not projected because the candidate reconstructs
  its authoritative profile from declared facts and input winners immediately afterward. Query-local normalization
  caches retain restricted immutable property sets and profiles by identity plus `BindingMask`, immutable property
  transformers reuse no-op results, and `BoundVarsKey` retains only its mask while materializing provider names lazily.
  The 193-symbol regression fell from 61,295,752 to 13,371,304 allocated bytes (-78.2%) with its 16 MB assertion
  unchanged. The clean selector passes 1/1, `CascadesCostModelTest` passes 94/94, and the required/provided property
  classes pass 10/10; red, green, and diagnostic logs are recorded in
  `initial-evidence.optimizer-provided-masks.txt`.
- [x] (2026-07-17 02:02Z) Replaced DPhyp's per-pair full hyperedge scan with primitive incident adjacency and one
  reusable epoch-mark lookup per enumeration. Each lookup scans the lower-volume incident side, deduplicates
  multi-node hyperedges through epoch marks, and still selects the smallest qualifying edge ID, preserving the
  legacy deterministic callback order and representative. Epoch rollover clears the marks before reuse. The
  matching exact-order selector passes 1/1, the DPhyp enumerator/planner/contributor cross-section passes 60/60, and
  the legacy optimizer/costing consumers pass 28/28 in workspace `optimizer-dphyp-adjacency`; complete pre/post
  evidence and the code-shape audit are recorded in `initial-evidence.optimizer-dphyp-adjacency.txt`. This milestone
  establishes behavior and an allocation-free lookup body; it makes no unmeasured planning-time or allocation-rate
  claim.
- [x] (2026-07-17 02:16Z) Added the first typed root EXISTS/NOT EXISTS search slice without changing broad scalar
  ownership. A non-scope-changing root Filter whose authoritative typed condition is exactly one correlated
  `ScalarExists` now becomes a directed `SEMI`/`ANTI` edge. Immutable edge origin distinguishes EXISTS, NOT EXISTS,
  MINUS, LEFT, and INNER semantics through region identity, scoped recipes, implementation validation, proof, and
  export. The outer input must assure every typed correlation bit; both tuple bodies remain atomic transparent memo
  factors, and the scalar RHS is independently optimized under `EXISTENCE`. Empty/unassured correlations, residual
  scalar conditions, and composite children decline as non-owning `NOT_APPLICABLE` boundaries. The focused TDD red is
  preserved in `initial-evidence.optimizer-semi-exists.txt`; the identical two-method selector passes 2/2, the full
  typed class passes 12/12, and the five-class core cross-section passes 39/39 in workspace `optimizer-semi-exists`.
- [x] (2026-07-17 02:22Z) Added a test-only Milestone 7 shadow harness without changing production routing. Chain,
  star, cycle, finite VALUES, and disconnected pure-inner fixtures compare exact result bags, legal typed topology,
  required inputs, plan fingerprints, canonical output/access work, completeness, deterministic AUTO counters, and
  retention snapshots between scoped Cascades and the directly reachable legacy planner. A deletion gate proves the
  legacy route must remain while an owned opaque UNION still fails canonical extraction as `NON_JOIN_COMPOSITE`.
  The first finite-VALUES red traced to the fixture multiplying prefix invocations while labeling its estimate
  `PER_INVOCATION`; correcting only that test contract to `TOTAL_FOR_CONTEXT` makes scoped and legacy both choose
  `join(join(VALUES, B), C)` at canonical work 6. The focused selector passes 1/1; after the independent OPTIONAL
  slice stabilized, the complete shadow/deletion-gate class passes 2/2. Evidence is in
  `initial-evidence.optimizer-shadow-parity.txt`.
- [x] (2026-07-17 02:31Z) Extended typed LEFT search to independently commutable adjacent OPTIONAL branches. One
  immutable bitmap proof requires an assured common base, disjoint introduced bindings, no hidden input outside the
  base, safe structure, and no semantic barrier; both scoped Cascades and the legacy LMDB normalizer consume it. The
  directed LEFT-star region costs either branch order but rejects reverse, branch-only, and compound-RHS transitions.
  Focused, core cross-section, and LMDB selections pass 6/6, 45/45, and 95/95 in `optimizer-optional-swap`.
- [x] (2026-07-17 03:05Z) Locked the dense-search acceptance boundaries without changing production code. A real
  12-factor clique now proves repeated AUTO searches produce the same legal winner, counters, retention state, and
  factor bag while stopping at the 100,000-partition limit. A real 16-factor clique with an injected 64-partition
  exact limit proves no counter overshoot, `RESOURCE_LIMIT_EXCEEDED`, a preserved reserved topology, and byte-for-byte
  unchanged input algebra. A formula-only DPhyp regression records 261,625 CSG-CMP pairs for 12 factors and
  21,457,825 for 16 factors, proving that the configured AUTO and EXACT limits are crossed without enumerating the
  16-clique in a unit test. All three focused selectors pass 1/1 and compact evidence is preserved in
  `initial-evidence.optimizer-dense-limits.txt`.
- [x] (2026-07-17 03:07Z) Unified null-rejecting OPTIONAL-to-INNER legality behind one immutable-IR proof shared by
  the standard DSL rule and the LMDB compatibility simplifier. The proof is query-local-mask native, requires a
  genuinely unbound RHS-only witness, threads incoming bindings, rejects scope/SERVICE/condition barriers, and models
  AND/OR, BOUND, comparisons, and IN conservatively. In particular, an RHS-only value that appears only as an IN
  candidate cannot prove rejection because an earlier constant may match, and an incoming prebound RHS name is not an
  unbound witness. The duplicate LMDB Cascades rule and provider registration are removed; scoped-fanout metadata and
  hoisting remain, while legality classification no longer constructs and discards a complete hoisted replacement.
  Core proof and DSL classes pass 9/9 and 32/32; LMDB simplifier and optimizer classes pass 42/42 (10 skipped) and
  56/56. The directly relevant shared-registry selector passes 1/1. A broad registry class still exposes the unrelated
  pre-existing absence of `lmdb-inner-join-bound-lookup`; it is not changed by this slice. Red and green evidence is
  preserved in `initial-evidence.optimizer-null-optional.txt`.
- [x] (2026-07-17 03:29Z) Made output binding shape a candidate-local, group-validated scoped-memo fact. Factor seeds
  capture their query-local possible/assured masks; INNER unions both dimensions, LEFT unions possible bindings while
  retaining only left assurance, SEMI/ANTI expose only the left masks, and predicates preserve their input shape.
  Later physical costing and predicate eligibility consume those candidate masks rather than reconstructing the
  union of every factor in the subset. Eligibility is charged and cached by candidate ID plus predicate ordinal, and
  one scoped group rejects physical alternatives whose logical binding shapes disagree. The primary red showed a
  SEMI probe-only RHS leaking into the second edge; the identical selector and LEFT/INNER companion pass 1/1, the
  implementation-provider class passes 12/12, and the memo class passes 11/11. Red and green evidence is preserved in
  `initial-evidence.optimizer-candidate-shape.txt`.
- [x] (2026-07-17 03:35Z) Made exact observation order a conservative no-reorder contract until sequence provenance
  becomes typed. Join discovery now rejects `EXACT_SEQUENCE` both before enqueue and defensively while draining, so it
  cannot fall through to either scoped or global join enumeration. The imported topology remains available to
  ordinary physical implementation, search charges zero partition probes, the result remains `COMPLETE`, and no
  unsupported or pending route is reported. The primary red captured a COMPLETE join-search request for the exact-
  sequence two-factor input; the identical selector passes 1/1, the routing class passes 4/4, and the existing
  executable-original-winner selector passes 1/1. Evidence is preserved in
  `initial-evidence.optimizer-exact-sequence.txt`.
- [x] (2026-07-17 03:54Z) Added the first opaque required-input parity slice without weakening semantic barriers. A
  classified atomic Extension may now retain a query-local required-input mask when every required symbol has exactly
  one assured sibling producer. The extractor records producer-before-consumer occurrence dependencies and includes
  the mask in region identity; missing or ambiguous producers fail closed until disjunctive producer state exists.
  `joinSearchContext` unions the region fact into the occurrence descriptor, so every contributor sees the same typed
  requirement. The primary red rejected `[seed]` as `NON_JOIN_COMPOSITE`; the identical selector passes 1/1, the
  extractor class passes 15/15, planner propagation 1/1, region identity 4/4, request context 3/3, and the LMDB
  descriptor-parity selector 1/1. Evidence is preserved in `initial-evidence.optimizer-opaque-required.txt`.
- [x] (2026-07-17 04:10Z) Added independently commutable exact EXISTS/NOT EXISTS stars. One immutable query-local
  bitmap proof requires every branch to have a non-empty correlation fully assured by the common atomic base, rejects
  sibling-only and hidden inputs, and fails closed across residual scalars, SERVICE/native boundaries, scope changes,
  unsafe structures, and MINUS. The extractor retains the written exact-Filter chain but contributes both legal
  SEMI/ANTI branch orders to one common-base directed star; the scoped planner gate accepts that existence family
  without accepting `ANTI/MINUS` or mixing it with LEFT. Strict evaluation proves identical bags and duplicate
  multiplicity across written, reversed, and selected plans. The focused red is preserved in
  `initial-evidence.optimizer-semi-anti-star.txt`; the identical selector passes 1/1, the bitmap proof class passes
  3/3, the route/rejection/bag class passes 6/6, and the pre-existing typed non-inner class passes 12/12 in workspace
  `optimizer-semi-anti-star`.
- [x] (2026-07-17 04:34Z) Made ordinary external-Filter inputs occurrence-sensitive without broadening scalar
  EXISTS ownership. Every required symbol still needs exactly one assured sibling producer: an earlier producer is an
  effective Filter predecessor/input, while a later producer depends on the Filter and is excluded from that
  occurrence's effective input mask. Missing or ambiguous producers remain fail-closed. Fragment identity now retains
  the written order of these Filters, and strict evaluation proves that moving a later producer first changes an
  unbound/error EBV from zero rows to one. The preserved red failed all three later-producer assertions; the identical
  selector and fragment selector pass 1/1, the LMDB opaque class passes 36/36, and the core extractor class passes
  15/15 in workspace `optimizer-external-filter`.
- [x] (2026-07-17 05:22Z) Restored the production legacy compatibility owner only for an exact source expression whose
  canonical extraction reports `NON_JOIN_COMPOSITE`; pure-inner joins remain inapplicable and use scoped search. The
  candidate is additive and competes under canonical cost rather than being forced to win. The AUTO regression exposed
  an unsafe UNION-to-VALUES rewrite of valued nonconstant variables, which now fails closed while true constants remain
  eligible. The omitted bound-lookup implementation is registered again, and the full shadow and unified-routing
  classes pass 3/3 and 4/4. This does not convert the canonical unsupported boundary into complete EXACT search.
- [x] (2026-07-17 05:34Z) Recharacterized the dependent-parent cost-ledger regression after typed EXISTS search made
  its scoped and global cost domains both observable. Revision tracing proved one stable global
  `CostApplicationClaim`, one scoped SEMI-edge evaluation, and no dependent-traversal invalidation. The test model now
  classifies the detached domain structurally through `ScopedMemoInputReference` and independently requires exactly
  one application in each domain, preserving the original global self-publication assertion. The exact selector,
  complete ledger class, and dependency-revision cross-section pass 1/1, 11/11, and 11/11.
- [x] (2026-07-17 05:45Z) Made typed EXISTS/NOT EXISTS extraction additive to the generic mobile-predicate route.
  When the specialized two-atomic-body route reports `NON_JOIN_COMPOSITE`, the extractor now retries the complete
  Filter-over-Join predicate in isolated state instead of suppressing generic predicate closure; residual conjunctions
  remain intact. Exact enumeration consequently restores the disconnected assured `{arm,tail}` prefix as well as the
  specialized `{arm}` and `{arm,guard}` alternatives. The focused enumerator class passes 3/3 and the exact planner
  selector passes 1/1 in workspace logs ending `53084af0` and `246a7b57`.
- [x] (2026-07-17 05:46Z) Kept unsupported alternative routes topology-local while preserving exact-route dependency
  merging. Unsupported-result coalescing now keys on status, typed boundaries, and the route's leaf-group set; it no
  longer unions different leaf topologies that happen to share one boundary. The focused three-route regression
  passes 1/1 and the full extractor class passes 16/16 in workspace logs ending `2221e455` and `53ceae19`.
- [x] (2026-07-17 06:14Z) Made a safe GROUP BY subtree an atomic but outer-reorderable factor. Opaque descriptors now
  distinguish factor-internal barriers from outer-placement restrictions; the core extractor remains operator-
  agnostic and still enforces exact possible/assured surfaces, required inputs, and unsafe shared outputs. A grouped
  duplicate-producing shadow query is `COMPLETE`, has no pending tasks, carries the scoped proof, preserves its bag,
  retains one Group, and makes the legacy compatibility rule inapplicable. The focused red is preserved in
  `initial-evidence.optimizer-group-atomic.txt`; final opaque, shadow, extractor, and exact-enumeration classes pass
  37/37, 4/4, 16/16, and 15/15 in workspace `optimizer-group-atomic`.
- [x] (2026-07-17 06:36Z) Finished the safe non-scope-changing UNION atomic-factor slice. A recursive closed-graph-
  pattern proof now separates UNION's factor-internal barrier from outer placement while rejecting scope change,
  external Extension/Filter inputs, VALUES/BIND, and other composite branches. Focused safe, external-input, and
  branch-only-output selectors pass 3/3 in workspace `optimizer-union-atomic`; duplicate-bag shadow plus the moved
  SERVICE deletion gate pass 2/2, and the complete opaque, shadow, and core extractor classes pass 40/40, 4/4, and
  16/16. The broad exact-enumeration class currently passes 13/15: its two unrelated Filter/NOT EXISTS completeness
  regressions belong to the concurrent typed memo-derivation slice and are recorded below for that owner.
- [x] (2026-07-17 07:02Z) Finished the safe unconditioned LeftJoin atomic-factor slice. LEFT_JOIN remains an internal
  barrier, but a non-scope-changing OPTIONAL with two recursively closed graph-pattern children may move as one outer
  factor. Conditions, scope changes, external Filter/Extension/Service children, and optional-only sibling use remain
  pinned or fail closed. A duplicate-producing matched row and an unmatched row with `nick` unbound survive the
  selective scoped reorder; `EXACT_SEQUENCE` instead preserves the written OPTIONAL-first order. The focused red is
  in `initial-evidence.optimizer-leftjoin-atomic.txt`; the safety, parity, complete opaque, and complete shadow
  selections pass 4/4, 2/2, 43/43, and 6/6. Broad core exact verification remains deliberately deferred until the
  concurrent typed-derivation slice is stable.
- [x] (2026-07-17 07:45Z) Completed the typed-derivation and proof-provenance compatibility slice. Logical alternatives
  now deduplicate by proof-specific derivation while join discovery keys and extractor resolution compare the full
  proof-free typed route. Exact physical-route duplicates retain the deterministic union of their route-local
  certificates, and a physical `PROOFS_CHANGED` event no longer restarts proof-insensitive join discovery. The compiled
  finite-filter rule now rewrites only its local Filter memo expression, so an unselected finite VALUES alternative
  cannot attach its proof to a selected QueryRoot clone. Final focused classes pass 8/8 semantic-scope, 15/15 exact
  enumeration, 17/17 extractor, and 19/19 LMDB rewrite-coverage tests in workspace `optimizer-legacy-gap`.
- [x] (2026-07-17 08:07Z) Removed legacy proof-rule IDs from physical frontier identity. Complete versus incomplete
  admissibility is now carried only by the typed `Winner.approximate()` state; `baseline-existing-algebra` and
  `existing-algebra-emergency-fallback` are ordinary provenance and cannot fork an otherwise identical complete
  winner. The focused red proved both legacy IDs retained a second state in either registration order; the identical
  selector and full memo-model class pass 1/1 and 61/61 in workspace `optimizer-winner-role`.
- [x] (2026-07-17 08:07Z) Made the OR-to-VALUES compiled rule Filter-local. The DSL no longer wildcard-matches and
  recursively clones Projection/QueryRoot ancestors; scalar-subquery Filters remain independently discoverable memo
  inputs. Selected scoped join recipes are verified as independent derivations through selected VALUES shape plus
  direct memo-local proof carriers, rather than inheriting the source Filter certificate. The preserved red reported
  proof sources `[Filter, Projection, QueryRoot]`; the identical selector and complete rewrite-coverage class pass
  1/1 and 22/22 in workspace `optimizer-or-values-local`.
- [x] (2026-07-17 08:07Z) Restored fail-closed nested EXISTS representation after broad queryalgebra verification.
  Generic mobile-predicate fallback remains additive for representable typed routes but may no longer erase an
  explicit `NON_JOIN_COMPOSITE` boundary from composite EXISTS tuple bodies. The identical focused selector passes
  1/1 and the complete typed non-inner class passes 12/12 in workspace `optimizer-nested-exists`; top-level assured
  atomic EXISTS/NOT EXISTS routes are unchanged.
- [x] (2026-07-17 10:15Z) Made compiled structural rewrite composition retain authoritative memo lineage below late
  child and grandchild alternatives. Nested matching is lazy and typed, direct captures retain memo binding shapes,
  copied EXISTS inputs retain scalar group identity, ambiguous child routes fail closed, and recipe cycles report
  unsupported representation without attaching the parent proof or leaving the directly tested helper orphan. The
  former safe-UNION and late-Difference regressions pass 1/1 after replacing an invalid recursive test fixture with a
  valid commuted-join alternative and explicit producer proof.
- [x] (2026-07-17 10:15Z) Replaced the logically recursive complete-graph-universe rewrite with a transparent
  parameterized physical route. Its explicit inputs are the finite VALUES group and the original pattern group; the
  RHS contract requires the graph binding and selected-prefix consumption. A same-`WinnerKey` self input is rejected
  as non-progressing rather than deferred, arbitrary physical self edges fail memo insertion, and incoming binding
  context makes the route inapplicable after descent. The metadata, strict-descent, and insertion selectors each pass
  1/1 in workspace `optimizer-filter-minus-local`.
- [x] (2026-07-17 11:34Z) Made compiled-rule wakeups and matching dependencies explicit. Bitmap shape guards declare
  possible, assured, nullable, and child-profile facts; direct child fact changes reactivate structural rules; and
  nested frontier candidates consume work before inspection. Compiled matching scans indexed expressions without
  allocating `MemoGroup.expressions()` snapshots. The focused selectors pass 2/2 and the complete rule-engine class
  passes 181/181 in workspace `optimizer-compiled-facts`.
- [x] (2026-07-17 11:34Z) Made emitted compiled-rule helper DAGs transactionally memo-owned. Rule application now
  carries immutable existing/fresh input recipes rather than eagerly interning helpers; typed aliases resolve exactly
  or fail closed, and cycle validation traverses every coalesced group's complete logical dependency closure. The
  second red proved that a later helper property failure stranded the first prepared group; helper and root groups,
  expressions, properties, binding shapes, feedback, templates, typed keys, and dense IDs are now all constructed
  before publication. Both transaction regressions and the combined rule-engine/memo/non-inner cross-section pass
  259/259 in workspace `optimizer-transaction`.
- [x] (2026-07-17 11:44Z) Moved the first opaque required-input inner join off the global join-state fallback. The
  production gate is deliberately narrow: exactly two predicate-free factors, one nonempty required-input dependency,
  and one in-region producer. The scoped memo exports only the root producer-to-dependent recipe, charges the single
  legal partition once, and emits no `joinState` or `predicateState` proof. The focused red and 1/1 green are preserved
  in `initial-evidence.optimizer-scoped-dependency.txt`; the neighboring join-search selection passes 70/72, with the
  two remaining failures isolated to concurrently changed scalar-provenance and now-invalid cycle fixtures.
- [x] (2026-07-17 11:47Z) Closed the compiled scalar-provenance escape hatch. Exact nested scalar memo inputs retain
  their captured `PlanIr` identity; the compatibility fallback now accepts a captured structural root only when every
  reachable IR node is the identical captured node in the same query-local universe. A forged scalar plan that reused
  the captured root while replacing one descendant previously borrowed the wrong memo group; the focused red is in
  `initial-evidence.optimizer-scalar-provenance.txt`, and the identical selector plus all five transaction tests pass
  1/1 and 5/5 in workspace `optimizer-scalar-provenance`.
- [x] (2026-07-17 12:22Z) Removed the dominant residual cost-model string-schema allocation. Fresh logical candidates
  now cache query-local `BindingShape` values and share one immutable name snapshot per equal `BindingMask` only at
  the existing logical-properties and feedback boundary. The 512-candidate, 192-symbol regression fell from
  73,281,824 allocated bytes to below its 20 MB ceiling while retaining symbol 191 above bit 63. The focused selector
  and complete cost-model class pass 1/1 and 96/96 in workspaces `optimizer-bitmap-cost` and
  `optimizer-bitmap-bound-context`; the reds are preserved in `initial-evidence.optimizer-bitmap-cost.txt` and
  `initial-evidence.optimizer-bitmap-bound-context.txt`. An independent review also found and closed the provider-
  boundary escape hatch: 512 equal 192-symbol bound contexts previously materialized 512 name sets and now share one,
  with allocation within 1 MB of an otherwise identical empty-context control.
- [x] (2026-07-17 12:46Z) Closed three typed-composition verification gaps without reopening opaque route selection.
  Compiled matching no longer descends an unexpanded wildcard capture merely to choose one of several logical child
  routes; copied nodes with authoritative provenance reuse that exact group, while unmapped descendants become
  transactionally validated typed helper recipes. Exact written route-only children can be resolved without exposing
  those routes to recursive join expansion. A debugger audit also proved the apparently missing `{arm,tail}`
  correlated-predicate prefix was searched, retained, and locally nondominated; it was intentionally absent only from
  the selected root's exported DAG. Direct scoped coverage now asserts all four legal predicate subsets, while the
  planner test asserts only legal root-reachable export. The transaction, route-safety, and exact six-class selections
  pass 6/6, 7/7, and 72/72 in workspace `optimizer-core-fixture`.
- [x] (2026-07-17 13:26Z) Corrected OPTIONAL liveness for nullable shared names. An RHS may bind a name that is only
  possible, not assured, on a particular left row; subtracting all left-possible names therefore removed an observable
  fallback binding. The typed rule now computes RHS-introduced names as `right.possible - left.assured`. The focused
  nullable red, identical green, and positive pruning companion are preserved in workspace
  `optimizer-filter-minus-local` at runs `20260717T132421.742753Z-88078-da59c47a`,
  `20260717T132619.937116Z-1284-2fa26057`, and `20260717T132807.199480Z-24806-af73cf4b`.
- [x] (2026-07-17 16:35Z) Replaced recursive exact-like group planning with persistent goal-bearing EXPLORE,
  IMPLEMENT, COST, JOIN_DISCOVERY, and JOIN lanes. Deterministic fair selection drains to a fixed point for
  AUTO/EXACT/SHADOW while timed BUDGETED variants retain their compatibility recursion; charge-before-work never
  overshoots, one original executable topology is reserved outside exploratory limits, goal-scoped physical rule
  applications can legitimately rerun, and dependent traversal resumes only from a real terminal prefix. A former
  hardcoded 46-work fixture was disproven by measurement: Explore finishes at 136, the cheaper late helper first wins
  at the complete 138-work fixed point, and repeated runs have identical counters and costs. The complete
  `CascadesRuleEngineTest` passes 184/184 in workspace run
  `20260717T163359.190425Z-34694-2b2363ac`.
- [x] (2026-07-17 16:35Z) Made logical-output estimate invalidation goal-scoped through retained winner dependencies.
  Each stored winner carries its authoritative `WinnerKey` outside equality and dominance; matching estimate changes
  remove only frontier entries whose selected child used the changed key, then propagate those exact keys through
  reverse parent edges. Unrelated contextual RHS frontiers are no longer rescanned. Candidate-local scoped join export
  now likewise reconstructs each retained derivation from its own detached candidate template instead of substituting
  the equivalence group's first logical representative. The scheduler/evidence/export regression selection passes
  3/3 in workspace run `20260717T161907.327750Z-48904-2000fd25`; a follow-up identity regression caught and fixed
  keyed-copy winner telemetry before the 184-test broad green.
- [x] (2026-07-17 17:43Z) Made estimate publication transactional with bounded candidate admission. Logical and
  selected-output evidence is previewed for costing, rejected candidates publish nothing, and surviving evidence is
  committed before winner insertion so a dominated implementation may still improve canonical group evidence. Timed
  BUDGETED planning now reserves the original executable topology outside both task and cost bounds. The focused
  transaction selector and complete `CascadesRuleEngineTest` pass 1/1 and 185/185 in workspace
  `optimizer-estimate-transaction` runs `20260717T173952.155507Z-72995-3a06bade` and
  `20260717T174328.897235Z-86628-ecf44d60`.
- [x] (2026-07-17 18:16Z) Replaced broad estimate and winner-frontier invalidation with exact goal/state propagation.
  Each logical-output key has its own revision; selected evidence removes only matching child winner states, and
  reverse dependencies carry the exact `(WinnerKey, WinnerStateKey)` pair through every parent. A same-goal
  two-state regression proves refining `{o}` preserves the independent `{s}` parent state. The complete ledger and
  physical-fact classes pass 12/12 and 19/19 in workspace runs
  `20260717T175744.978286Z-27466-db1fe27b` and `20260717T181501.624083Z-80130-6d76c343`.
- [x] (2026-07-17 18:33Z) Closed the residual string-schema allocation seams used by scoped hash eligibility and
  shared-variable costing. Same-universe references intersect cached query-local masks directly, and the cost model
  materializes names only at a compatibility boundary. The provider and complete 96-test cost-model classes pass in
  workspace runs `20260717T164754.939826Z-27499-a279bd60` and
  `20260717T164511.661473Z-17753-182777a8`.
- [x] (2026-07-17 18:33Z) Added deep compiled-rule route capture. `captureRoute` reconstructs all non-scalar memo
  structure while retaining authoritative scalar subquery groups; route-local ambiguity and cycles fail closed. The
  LMDB unused-OPTIONAL observer now uses this provenance-preserving capture and refuses pruning across any retained
  nondeterministic node. The core transaction class passes 7/7, the nondeterministic LMDB regression passes 1/1, and
  three positive route controls pass 3/3 in workspace `optimizer-route-provenance` runs
  `20260717T183033.415160Z-43677-a92b9a78`, `20260717T182918.402341Z-39783-b6b99bee`, and
  `20260717T183248.306246Z-53974-ccd64cbd`.
- [x] (2026-07-17 19:27Z) Closed the broad deterministic-scheduler integration regressions. Original-topology
  reservation is deferred until the ordinary fixed point, memo changes coalesce until the current rule agenda
  completes, and late fallback publication reopens deterministic work only when it creates a real factor winner and
  no hard join limit has fired. Test-only physical-frontier consumers now declare their actual dependencies, keyed
  winner assertions compare value plus optimization state rather than object identity, and refinement-order fixtures
  accept the single canonical parent costing. The affected scheduler selection passes 80/80 in workspace run
  `20260717T192633.985536Z-48863-c4f941e1`.
- [x] (2026-07-17 19:27Z) Made completed goal-scoped join topology reusable across winner-only revisions without
  replaying global partition objects. DPhyp transfers and replays raw masks through primitive storage, stale bridge
  routes that contain only missing logical expressions fail closed locally without poisoning completeness, supported
  root/goal coverage clears obsolete diagnostics, and readiness consumes the same compatible factor frontier used to
  construct the scoped session. The focused join-integration selection passes 5/5 and the owning classes pass 30/30
  in workspace `optimizer-join-integration` runs `20260717T190414.452762Z-67104-871b9614` and
  `20260717T192407.376920Z-41519-1bcf0935`.
- [x] (2026-07-17 19:27Z) Replaced a known opaque operator's blanket `NON_JOIN_COMPOSITE` boundary with explicit
  written-order placement. A three-factor Difference retains both transparent child groups, admits every association
  that preserves the barrier occurrence, and rejects moving that occurrence across a sibling. The focused red and
  green plus the complete 18-test extractor class are preserved in workspace `optimizer-ordered-opaque`, whose class
  run is `20260717T191813.651244Z-18403-736a3339`.
- [x] (2026-07-17 19:38Z) Added canonical non-scope Projection classification without treating specialization as
  ownership. Ordered source-to-target mappings remain semantic identity; a nontransparent Projection is one ordered
  opaque occurrence with an independently optimizable child, while the existing identity-projection proof is the
  only mobile form and scope-changing Projection retains its separate scope barrier. The two focused reds are
  preserved in `initial-evidence.optimizer-projection-opaque.txt`; both identical selectors pass, and the full opaque
  parity class passes 45/45 in workspace run `20260717T193659.721480Z-236-f26c82fb`.
- [x] (2026-07-17 20:12Z) Moved correlated scalar-subquery predicate closure into the scoped join memo without
  recursive exact planning. Predicate applications carry ordered executable scalar contracts and independently
  costed scalar winners; missing winners defer on their exact `WinnerKey`, while later revisions of that same frontier
  reopen a saturated join and unrelated winner states remain quiet. Correlation-required masks remain visible even
  when they are not scalar outputs, and selected export retains tuple plus scalar child contracts. The original global
  `joinState` red and the late-winner red are preserved in
  `initial-evidence.optimizer-scoped-scalar-predicate.txt`; the two focused selectors and four owning classes pass
  2/2 and 50/50 in workspace runs `20260717T200923.212625Z-35761-3101340f` and
  `20260717T201031.217503Z-40121-7da0d5a6`.
- [x] (2026-07-17 20:12Z) Closed ordered SERVICE and scope-changing LATERAL canonical extraction. Ordered-factor
  required inputs are occurrence-relative to earlier assured producers, so a remote SERVICE output is not falsely
  demanded from a later consumer; classified scope factors retain descriptor masks and transparent children; unsafe
  overlap is accepted only under exact written placement. Scope child resolution remains authoritative and fails
  closed without a unique typed memo group. Both focused reds are preserved in
  `initial-evidence.optimizer-service-lateral-opaque.txt`, both identical selectors pass, and the complete opaque
  parity class passes 47/47 in workspace run `20260717T201052.152035Z-40605-80798184`.
- [x] (2026-07-17 20:12Z) Reconciled compiled physical marker IR with executable tuple input layouts. Input resolution
  peels only known unary LMDB marker nodes that IR export erases, permits a zero-child IMPLEMENTATION only when the
  executable tuple is genuinely a leaf with no fresh logical recipe, and continues rejecting unsupported helpers.
  Statement access paths now compile as physical leaves while composite Difference markers retain both child groups.
  The two focused reds are preserved in `initial-evidence.optimizer-access-path-dsl.txt`; both selectors pass in
  workspace `optimizer-access-path-dsl-final` runs `20260717T200757.458188Z-28240-bfb20130` and
  `20260717T200924.616597Z-35891-1ea2e359`.
- [x] (2026-07-17 20:39Z) Made `RequiredOuterInputs` operational rather than a constant placeholder. Base groups seed
  query-local required masks, directional composition is `leftOuter union (rightOuter - leftAssured)`, later RHS
  factors cannot retroactively discharge a left requirement, and only initially bound context can admit a
  parameterized root. Independent and parameterized states retain separate groups/frontiers, expose the required mask
  through typed physical parameterization, and every topology path skips empty frontier groups. The focused and
  broadened reds are preserved in `initial-evidence.optimizer-required-outer-inputs.txt`; the focused selector,
  `JoinRegionMemoTest`, and provider class pass 1/1, 11/11, and 15/15 in workspace
  `optimizer-required-outer-inputs`, with final run `20260717T203648.477192Z-70579-7e7f6efd`.
- [x] (2026-07-17 20:39Z) Closed the remaining scalar-subquery opaque transparency hole. Atomic EXISTS/NOT EXISTS
  Filters are policy-classified before the generic local-Filter shortcut, so their ordered `SCALAR_SUBQUERY`
  restriction and child group survive canonical extraction; ordinary scalar-free local Filters remain transparent.
  Scope-changing Projection remains a green ordered-factor control. The focused red is preserved in
  `initial-evidence.optimizer-opaque-deletion-gate.txt`, and the final full opaque parity class passes 49/49 in
  workspace run `20260717T203714.937638Z-74148-0e67c795`.
- [x] (2026-07-17 20:47Z) Made exact terminal failure propagation include goals whose last physical applications were
  waiting on deferred child costs. Once every deterministic work lane is empty, those deferrals can no longer become
  runnable without a winner revision, so publishing the missing child frontier is what reawakens its parents; the
  finalization pass no longer clears that state silently. A mutually dependent pair plus an independent executable
  root proves only terminal child goals fail and the legal root remains available. The focused red is preserved in
  `initial-evidence.optimizer-deferred-failure.txt`, and the identical selector passes 1/1 in workspace run
  `20260717T204703.917139Z-27608-ebd92a17`.
- [x] (2026-07-17 20:54Z) Corrected the selected-prefix execution contract for Projection roots. RDF4J Projection and
  MultiProjection iterators intentionally emit only declared columns, so an optimized projection alternative cannot
  preserve arbitrary caller bindings and must use an isolated/hash route. Parameterized Join execution now requires
  both children to preserve that prefix, and scoped placeholder templates are validated against their materialized
  LEFT and RIGHT winners before costing. Specialized UNION factoring remains in the same memo group; canonical cost
  may select either the original UNION or a safely isolated projected alternative. The focused reds are preserved in
  `initial-evidence.optimizer-opaque-deletion-gate.txt`; final core selected-prefix, shadow-parity, and complete opaque
  selections pass 2/2, 6/6, and 49/49 in workspace runs `20260717T211510.792496Z-51830-6d1460d4`,
  `20260717T211222.068602Z-37398-f02f5f53`, and `20260717T211342.552744Z-42043-690d0781`.
- [x] (2026-07-17 21:34Z) Made directional memo-partition identity preserve its left-to-right availability context.
  `JoinMemoExpressionKey` is the dense bitmap tuple `(availableBefore, left, right)`; service and scoped maps no
  longer deduplicate on a diagnostic string, and exhaustive partition state includes the same context. Raw callbacks
  are still charged before typed deduplication, exact duplicate contributors consume no candidate work, and DPhyp's
  primitive raw-mask tables remain unchanged. The three-factor dependency red is preserved in
  `initial-evidence.optimizer-partition-context-key.txt`; the identical selector, `JoinSearchServiceTest`, and the
  owning provider class pass 1/1, 15/15, and 15/15 in workspace runs
  `20260717T213038.950415Z-26791-772a8f1a`, `20260717T213213.892665Z-36545-37746f65`, and
  `20260717T213313.881067Z-47056-6e5d15da`.
- [x] (2026-07-17 22:00Z) Made parent cost-application identity react to targeted child-frontier invalidation without
  replaying combinations for ordinary frontier growth. Each exact `WinnerKey` now carries an invalidation epoch that
  advances only when one of its selected states is removed; the parent's sealed application revision includes that
  epoch alongside the child's fact and broad-clear revisions. The focused red is preserved in
  `initial-evidence.optimizer-cost-invalidation-epoch.txt`; the identical selector and complete ledger class pass
  1/1 and 13/13 in workspace runs `20260717T215330.781835Z-21344-d60d96f6` and
  `20260717T215504.985360Z-27078-ad0425dd`. Strict medical query 0 now returns the correct aggregate `7571` with a
  `COMPLETE` exact winner, no pending work, and no hidden-parent fallback both without and with DPhyp. Those runs are
  `20260717T215606.556234Z-30819-fe9b9529` and `20260717T215754.850097Z-35050-efd8f23d`; DPhyp contributes 0 and 10
  probes respectively while execution results remain identical.
- [x] (2026-07-17 22:25Z) Replaced ambiguous assured-supplier rejection with exact producer clauses. Mandatory
  occurrence predecessors now represent only unconditional scope/order hazards; each required binding is a
  conjunctive clause whose typed `ProducerState` alternatives are disjunctive dense `JoinSubset` values, and an
  initially bound mask may satisfy the clause without forcing a sibling first. Exact full-tree and memo enumeration
  retain all eight legal trees and 13 availability-sensitive expressions for the three-factor A-or-B case; the
  deterministic seed uses the same outer-aware readiness, and primitive DPhyp fails closed before touching any
  dependency region. Clause-only regions use scoped `JoinRegionMemo`, while hard scope barriers remain on their
  existing parity route. The focused red and identical green are preserved in
  `initial-evidence.optimizer-disjunctive-producers.txt`; final extractor, search, scoped planner, provider, and typed
  identity selections pass 18/18, 17/17, 16/16, 15/15, and 5/5 in workspace
  `optimizer-disjunctive-producers` runs `20260717T221757.337283Z-15868-9499cce8`,
  `20260717T221652.578081Z-11097-93090115`, `20260717T222057.683698Z-28043-41b6f942`,
  `20260717T222156.408788Z-30554-b29de9b2`, and `20260717T222401.441385Z-39735-a1aace73`.
- [x] (2026-07-17 23:15Z) Made mixed typed transitions finite, stateful, and association-independent. A dense
  `JoinTransitionClassifier` now permits ordinary independent prefixes without swallowing an unapplied directed RHS,
  rejects ambiguous directed transitions without invoking a physical implementation, and carries each consumed edge
  in immutable bitmap `AppliedEdgeState`. Scoped groups, canonical log-cardinality cache keys, predicate closure,
  root qualification, and reconstruction recipes preserve edge application exactly once. Direct partition admission
  uses the region's producer-readiness and `availableBefore` legality before entering the scoped memo. Directed-edge
  cardinality now comes from a provider-level logical estimate over stable factor-role templates, so physical
  implementation order and the current association cannot define the logical result; unavailable evidence retains
  the existing legal fallback and invalid evidence fails closed. The production ownership gate was deliberately not
  broadened. The two reds are preserved in `initial-evidence.optimizer-typed-transitions.txt`; the final six-class
  core matrix passes 60/60 in run `20260717T231136.420527Z-20138-13c53432`, and LMDB scoped/legacy shadow parity,
  including the safe OPTIONAL route, passes 6/6 with the unrelated `japicmp` check skipped in run
  `20260717T231436.326066Z-31937-b7c73075`.
- [x] (2026-07-17 23:25Z) Closed the first production cutover seam for primitive DPhyp. Global memo enumeration now
  records the primitive contributor as `UNSUPPORTED` with zero offered candidates instead of inheriting the full-tree
  adapter and requesting LMDB's privately selected winner; the generic exhaustive contributor still completes the
  route. Scoped raw-mask delivery and the deliberately retained scalar compatibility APIs are unchanged. The focused
  red is preserved in `initial-evidence.optimizer-primitive-dphyp-seam.txt`; the identical selector, complete
  `JoinSearchServiceTest`, and LMDB contributor class pass 1/1, 18/18, and 9/9 in workspace runs
  `20260717T232218.348095Z-67350-0fde3c7c`, `20260717T232321.305275Z-71542-9ef44654`, and
  `20260717T232420.704952Z-75563-9ad69a0c`.
- [x] (2026-07-18 00:33Z) Added one conservative scalar-evaluation effect contract and applied it to tuple rewrites,
  compiled DSL guards, the standard Filter optimizer, opaque-factor placement, legacy connected-join ordering, and
  MINUS-to-NOT-EXISTS eligibility. `REPEATABLE` and `QUERY_STABLE` expressions may move; `VOLATILE` and `UNKNOWN`
  expressions retain their written evaluation boundary. Native EXISTS is recursively classified only through audited
  model tuple operators; SERVICE, tuple functions, foreign tuple operators, missing functions, and row-dependent
  plugin functions fail closed. The red slices are preserved in `initial-evidence.optimizer-scalar-effects.txt`.
  The final core scalar/rewrite matrix passes 72/72 in workspace run
  `20260718T003024.657362Z-63106-c250eee5`, and the LMDB opaque-factor class passes 52/52 in
  `20260718T003210.311634Z-69594-e75b3786`. The full 189-test Cascades rule class retains only the two independently
  reproduced scheduler baseline failures (`autoCostsLateScopedUnionHelperAtDeterministicFixedPoint` and
  `currentDeferredGroupIsNotReenteredForEquivalentDependentPrefixes`).
- [x] (2026-07-18 00:55Z) Closed deterministic Extension external-input parity without weakening sibling-producer
  safety. External-only names are optional evaluation context, possible-but-not-assured sibling producers still fail
  closed, mixed dependencies publish only their assured-produced hard mask, and multiple assured suppliers remain
  exact disjunctive singleton producer states. Assured goal input context now seeds scoped join legality, while the
  shared scalar-effect policy keeps volatile Extension expressions at their written boundary. The focused red and
  post-fix evidence are preserved in `initial-evidence.optimizer-extension-external-input.txt`; the complete core
  extractor class passes 21/21 and LMDB scoped/legacy parity passes 8/8 in workspace
  `optimizer-extension-external-input`. A subsequently isolated, unrelated selected-root-DAG assertion still expects
  one exported two-factor subset but observes two; this slice does not alter that fixture's input context.
- [x] (2026-07-18 05:10Z) Closed the remaining deterministic scheduler baseline failures. Exact blocker subscriptions
  now survive unrelated physical wake-ups; terminally failed dependent prefixes retain a completed traversal stamp;
  and duplicate memo notifications no longer cancel a legitimately queued refreshed cost task. The late scoped UNION
  fixture exposes the true fixed point: task 140 remains bounded-incomplete, task 141 is complete, selects the cheaper
  helper, and repeats with identical counters. The focused scheduler/application-ledger matrix passes 18/18 and the
  full rule-engine class passes 190/190. Red and green evidence is preserved in
  `initial-evidence.optimizer-scheduler-blockers.txt`.
- [x] (2026-07-18 06:15Z) Normalized dependency-free pure-inner exact enumeration to one availability context, so
  disconnected eight-factor coverage is finite at 6,050 expressions while dependency-bearing regions retain their
  directional context. The corrected root-DAG assertion accounts for the independently optimized RHS leaf; the
  75-case exact matrix and 17-case scoped export class are green. A direct uniform-statistics regression also proves
  medical query 7 reaches a `COMPLETE`, no-pending EXACT winner. The complete queryalgebra module passed 1,456/1,456
  before the subsequent route-provenance hardening.
- [x] (2026-07-18 06:40Z) Scoped join-state ownership by semantic transformation-proof obligations while continuing
  to share complete legal topology enumeration. The route key retains rule, semantic scope, facts, rewrite safety,
  and assumptions, but excludes occurrence-specific certificate node IDs, implementation proofs, and the generic
  join-search proof. This prevents an unproved Filter route from reusing an OR-to-UNION-only intermediate without
  multiplying equivalent clone routes by source-expression ID. Source proofs are attached only to complete exported
  roots, including the retained legacy materializer. The new route-isolation test and selected LMDB provenance test
  pass 1/1 each; the full rule-engine class passes 190/190. The additional legitimate proof route moves the locked
  AUTO fixed point from 141 to 144 operations, with 143 bounded-incomplete and repeated 144-operation runs identical.
- [x] (2026-07-18 18:55Z) Closed the remaining scheduler and typed-join gate regressions after cost-lane batching.
  Exact cost applications are ledgered by the precise child winner state; reserved generic implementations are
  bounded-mode fallbacks only; and scoped join work now subscribes to unavailable factor frontiers instead of being
  dropped. Implementation-scope canonicalization removes fact-free invocation cardinality and selected-prefix
  consumption from rule-application identity while retaining those dimensions in winner costing. Typed LEFT,
  SEMI, and ANTI proofs are required in the saturated memo rather than forcing a specialized alternative to win an
  equal-resource physical-identity tie. The consolidated reserved-fallback, cost-ledger, work-limit, versioned-fact,
  OPTIONAL, and typed non-inner selection passes 52/52 in workspace run
  `20260718T185449.457175Z-11151-9705b394`. The complete queryalgebra/evaluation module then passes 1,472/1,472 in
  workspace run `20260718T185626.420397Z-17007-7bcb5fe8`.
- [x] (2026-07-19 01:48Z) Corrected the surrounding-join reactivation gate to assert typed memo semantics rather than
  duplicate tree syntax. A finite child rewrite remains in the referenced child group, root discovery exposes the
  expanded three-factor region, and the single canonical parent expression carries the unified-search proof. The
  focused selector passes 1/1 in workspace run `20260719T014753.979621Z-32942-dc9ff427`; temporary join telemetry was
  removed.
- [x] (2026-07-19 02:27Z) Closed the remaining scheduler/frontier and selected-prefix route gates. Winner insertion
  now reports every displaced same-state, dominated, or trimmed winner; removal invalidates only parent DAGs that
  selected that state, and provenance records the displaced alternative. A winner separately caches whether its
  selected DAG is safe as a parameterized join route; that fact is part of frontier state and is consumed by both
  global and scoped join implementations without rebuilding a tuple tree. Root input-consumption capability remains
  independent, so a transparent Difference can accept its caller's prefix while its nested-subquery form is still
  rejected as an outer Join RHS. The full rule-engine, join-region, and cost-model classes pass 195/195, 18/18, and
  98/98; the complete queryalgebra module passes 1,483/1,483 in run
  `20260719T022138.860385Z-30587-a10c08dd`. Reusing cached memo inputs removes the route-cache's temporary per-winner
  slot-list allocation; the four focused global/scoped routing gates remain green in run
  `20260719T022644.275012Z-42104-b1b2ef79`.
- [x] (2026-07-19 04:23Z) Made pure inner-join discovery own only maximal reorderable regions. Before the fix, a
  four-factor tree opened overlapping scoped searches for memo groups `[6, 2, 5, 5, 5]`; the scheduler now suppresses
  a nested route only when every executable logical consumer is a local, admissible, non-barrier INNER parent. Shared
  or independently consumed subplans remain eligible. The focused red is preserved in
  `initial-evidence.optimizer-hardening.txt`; the identical selector and complete 19-test enumeration class pass in
  workspace runs `20260719T042013.007842Z-43566-58476f1d` and
  `20260719T042237.672640Z-48469-16ffe3dc`. This closes the observed LMDB heap amplification: the same 4 GiB
  `LmdbDeleteInsertTest#test` that previously failed with `OutOfMemoryError` after 35.63 seconds now passes in 15.629
  seconds in run `20260719T042118.121135Z-45991-4d21201c`.
- [x] (2026-07-19 05:27Z) Closed the medical Q3 AUTO/EXACT plan divergence at the scoped-root export boundary.
  Finite-domain and exact-relation canonicalization now reuse collision-safe immutable facts without repeated tuple
  hashing, reducing the observed Q3 preparation path from quadratic work to linear membership. More importantly,
  bounded root export now ranks every eligible outer-input group in one canonical cost order before applying the
  sixteen-root cap; a primitive top-K keeps the later-cheaper replacement property without allocating all roots.
  The focused export red is preserved in `initial-evidence.optimizer-hardening.txt`; its identical selector, the
  complete 14-test work-limit class, Q3 plus Q7 AUTO regressions, and the affected scheduler/context selectors pass in
  workspace runs `20260719T052046.017519Z-17941-144bff0d`,
  `20260719T052507.708139Z-29920-b8786c4c`, `20260719T052647.720002Z-32688-a62c1c1d`, and
  `20260719T052413.089929Z-27514-2cf0728f`. Uncharged original-topology reservation is now deferred until ordinary
  exploration quiesces or a hard stop requires the guaranteed fallback, so a provisional physical winner cannot
  change join discovery or scoped search context.
- [x] (2026-07-19 12:10Z) Closed finite-filter evidence and mobile-predicate extraction gaps exposed by the theme
  regressions. LMDB now publishes an exact finite relation for safely enumerable literal filters while retaining the
  underlying scan work, selected finite-prefix lineage survives transparent composite costing, and an ordinary
  repeatable scalar Filter over a Join becomes a scoped mobile predicate even when it has no external scalar inputs.
  The finite-surface class passes 11/11, the extractor plus standard-rule selection passes 59/59, and the compact
  AUTO semi/anti query retains bounded finite-first work.
- [x] (2026-07-19 12:28Z) Made selected-prefix route capability an authoritative typed scoped-frontier property.
  Before the fix, a cheaper route-unsafe candidate could dominate the only intermediate candidate capable of a later
  bound lookup because recursive route safety existed only on `Winner`. Scoped seeding and implementation retention
  now resolve delivered `inputConsumption` to `SELECTED_PREFIX` or `ISOLATED`, and implementation legality reads that
  typed candidate property. The focused red is preserved in
  `initial-evidence.optimizer-hardening-route.txt`; the provider class passes 24/24 and full library query 7 passes
  direct, set-rewritten, optimized-pipeline, and result assertions in 63.583 seconds.
- [ ] Complete pure-inner scoped parity and shadow it against the legacy route, including goal-enforcer coverage,
  primitive DPhyp contribution, and predicate closure.
- [ ] Establish opaque-factor parity and typed LEFT, SEMI, and ANTI semantics before any legacy-route deletion.
- [ ] Burn in, cut over by covered region class, remove obsolete routes, optimize hot paths, and capture JMH/JFR
  evidence.

### Medical cost-policy baseline

The direct-planner harness produced the following policy-invariant identity and completeness states before the cost
policy migration. Every extracted row uses root rule `generic-physical-implementation`; policy-specific hexadecimal
cost components remain in the workspace logs named in `Surprises & Discoveries`.

| Query | Semantic fingerprint | Physical fingerprint | Completeness | Pending |
| ---: | --- | --- | --- | ---: |
| 0 | `be9637030f798137528a01f784d221fdfb973b41809610123afc817d840b1e2b` | `c6df77d94b790b233b800a4b2eb945a3e3f9a61756f07f74bb6e49f8dbcf2c64` | `COMPLETE` | 0 |
| 1 | `50bd6c9ee7652e2f74b09a3e338dacd914702142005d0b24db441dcbbcb9d2d6` | `d41ac1b48c54bb00e0834bf98d0f93e9f3ba638790c5b2d34341f16a4e252514` | `COMPLETE` | 0 |
| 2 | `eabf0fc716d1e921b9dff4cae0bac6740a1caed02edc88ff9229679d2f014162` | `586785ed2cdde0ee2e81ef60357d8f3250dda5a8629ac92ad5b2b0e4291c1cff` | `UNSUPPORTED_ATOMIC_BOUNDARY` | 2 |
| 3 | `cdb7d1b682361f6c8cd7c30f2326417287c4857c2e2ab4acc30a5722260508eb` | `d5ad659b46626533b2a3ae4da459b165889ee0854b36ea6900362bc6c54c6918` | `UNSUPPORTED_ATOMIC_BOUNDARY` | 41 |
| 4 | `c245f41c58f7e775a8058fee3d5b8e332b493c8c06bd463cd209d0ebec2ca0f1` | `a3bb4086e939393499c01ce28ef4a812d12228fa14d70022ffcad55cda835a3f` | `UNSUPPORTED_ATOMIC_BOUNDARY` | 13 |
| 5 | `2456e26c197ce1a553e6cdc1e5b7631c36ed90d5288f563c327a24cf8d89d78e` | `a2d44ef9d957e803866c667f52d17f0284a7da1b5ad5cbebeb2077826ff17bd3` | `UNSUPPORTED_ATOMIC_BOUNDARY` | 304 |
| 6 | `50a9d5d8b35e0b456c2caf42bb7b2a50faa36433b105f762de0b3c47a20f5bb4` | `ae7e61f267d50e181900ec31b8855d3fa8973ad7b1615af6c2fc7dae39f3f8ea` | `COMPLETE` | 0 |
| 7 | `d8e341b7f4be3c0e79f0c979e5354b7a19d0f136f6e965f9f89dc2dde6849956` | `50aebf5afa25cb8f42adbbaae4095b3b012be4e3d078717d4956b1ae8c3bb14d` | `UNSUPPORTED_ATOMIC_BOUNDARY` | 2 |
| 8 | `c088872c0f6b3277af048ffc1caebf0eba108cbdc6d897ac97a892bae649e0e7` | `19d6124670125ef5749c627f4a54294554f9a7ea940d7a5901ee506eb69ef725` | `COMPLETE` | 0 |
| 9 | _no winner emitted under any policy_ | _no winner emitted under any policy_ | _non-completion_ | _not emitted_ |
| 10 | `b971510a45007f9195c0f387265185438aff12da2422b51717990aad800bfc7b` | `942bf7d6d15067c5138c069f3df8a97e210732039072a6032636c5d8d9c60c0f` | `UNSUPPORTED_ATOMIC_BOUNDARY` | 17 |

## Surprises & Discoveries

- Observation: an unchanged dependency key can mean either “nothing new to schedule” or “the refreshed task is
  already queued.” Removing the queue marker in the former branch canceled the latter branch's legitimate work,
  allowing AUTO to report false quiescence at 129 tasks while a cheaper scoped UNION helper remained uncosted. Queue
  membership is ownership of pending work, not a deduplication scratch bit; an unchanged notification must leave it
  untouched. With that invariant restored, the deterministic fixed point is 141 tasks.
  Evidence: `initial-evidence.optimizer-scheduler-blockers.txt` records the false-complete red, the 140/141 boundary,
  the 18-test scheduler matrix, and the 190-test rule-engine green.

- Observation: an operator can require an atomic semantic interior without restricting placement of that whole
  factor in an outer inner-join region. Treating every non-empty semantic-barrier set as an outer prohibition froze a
  safe Group even though its group key was assured, it had no required outer input, and its aggregate-only output did
  not overlap a sibling. The two dimensions must remain typed rather than inferred from an operator name.
  Evidence: `LmdbOpaqueFactorJoinPlanningTest#coreExtractorTreatsSafeGroupAsAtomicOuterFactor` failed with
  `Opaque Group still has semantic barriers [GROUP]` in the workspace log ending `99197b3b`; the identical selector
  passes in the log ending `d82215e9`.

- Observation: broadening the opaque-factor class after valued nonconstant variables became observable exposed test
  fixtures that supplied fixed predicate IRIs through `new Var(name, value)`. Their assertions consistently omitted
  the predicate names, proving fixed-predicate intent; explicit `Var.of(name, value, false, true)` repairs the fixtures
  without changing production behavior or weakening assertions.
  Evidence: the first broad run ended `9fbcf543` with ten failures and one error caused by extra predicate bindings;
  the audited 37-test class passes in the log ending `f0292862`.

- Observation: a specialized typed rewrite must not become an ownership gate for a broader legal rewrite. A root
  EXISTS/NOT EXISTS Filter over a composite Join can fail the specialized atomic-body contract while still being a
  valid generic mobile predicate. Returning that specialized boundary directly suppressed predicate closure over a
  disconnected but fully assured prefix; isolated additive routing restores both alternative families without
  weakening either proof contract.
  Evidence: `JoinStateEnumeratorTest#extractedCorrelatedRegionClosesPredicateOverDisconnectedPrefix` and
  `CascadesJoinRegionEnumerationTest#exactJoinSearchEnumeratesCorrelatedNotExistsAtEveryAssuredPrefix`; the failing
  workspace log ends `5da6c899`, and the green logs end `53084af0` and `246a7b57`.

- Observation: an unsupported extraction result's leaf groups describe the route that produced its boundary, not a
  mergeable dependency summary. Coalescing only by status and boundary can combine unrelated alternatives and violate
  the extractor invariant that region factors equal topology-local leaves. Expanded dependency groups may merge only
  after the exact route identity, including its leaf set, matches.
  Evidence: `MemoJoinRegionExtractorTest#unsupportedAlternativeRoutesRetainTopologyLocalLeaves`; the failing and
  passing workspace logs end `a61f66b9` and `2221e455`, and the complete extractor-class log ends `53ceae19`.

- Observation: an ordinary Filter's external condition name is not an order-independent parameter. A sibling written
  later is unavailable when the Filter computes EBV; treating that sibling as a predecessor converts an unbound/error
  EBV into a value and can admit rows. Fragment coalescing must therefore retain this written occurrence boundary
  until dependencies are derived, even though ordinary inner-join leaves around it remain reorderable.
  Evidence: `LmdbOpaqueFactorJoinPlanningTest#coreExtractorPreservesExternalFilterOccurrenceOrder` evaluates the
  written form to zero rows and the deliberately illegal producer-first form to one; the initial three-assertion red
  is preserved in `initial-evidence.optimizer-external-filter.txt`.

- Observation: a `BindingSetAssignment` used as an EXISTS body can share a binding name with the outer input while
  still producing an empty authoritative `ScalarExists.correlatedVariables` mask. Reorder legality must consume the
  typed correlation fact rather than infer correlation from overlapping output names; the focused fixture therefore
  uses a shared-subject `StatementPattern` and the proof deliberately rejects the VALUES form as
  `MISSING_CORRELATION`.
  Evidence: the first post-change focused run in workspace log
  `.mvnf/workspaces/optimizer-semi-anti-star/logs/20260717T040219.119165Z-73772-91eba3f7/verify.log` stopped at
  `UNPROVEN_EXISTS_INDEPENDENCE: MISSING_CORRELATION` before the fixture was corrected.

- Observation: the current `MemoExpr` is a Java record whose components include mutable `TupleExpr`, cost, estimate,
  proofs, physical properties, and diagnostic string identity. The memo itself is keyed by `Map<String, Integer>`.
  Typed identity therefore has to become authoritative before changing the record shape; changing only
  `equals/hashCode` would mix semantic and physical state and would not repair the memo lookup path.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/MemoExpr.java`
  and `Memo.java`.

- Observation: the global join route currently deduplicates contributor output in a `LinkedHashMap<String, ...>` and
  `JoinStateEnumerator` materializes maps of boxed occurrence sets and every reachable predicate state. This is the
  allocation slope that the scoped join-region memo must replace, not merely tune.
  Evidence: `JoinSearchService.enumerateMemoExpressions` and `JoinStateEnumerator.Enumeration` in
  `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/join/`.

- Observation: global factor winners are keyed by the physical contract requested by their current parent. A valid
  executable leaf may therefore have only a `SELECTED_PREFIX` winner frontier and no separately materialized `ANY`
  frontier. Scoped join seeding must snapshot compatible winner frontiers and preserve their delivered properties;
  requiring an exact synthetic `ANY` key falsely leaves otherwise executable factors unavailable.
  Evidence: the preserved five-factor export red and green workspace logs ending in `98aa0f15` and `6db4d077`.

- Observation: materializing a fallback as `JoinMemoExpression.decompose(leftDeepTree)` retained growing boxed
  occurrence sets, deep representative trees, and canonical strings for every prefix. Even though selection itself
  was bounded, a 128-factor seed therefore created quadratic retained state before scoped costing. The production
  route now carries one primitive occurrence order and compact group-ID partitions; the expression adapter remains
  only on compatibility targets while they migrate.
  Evidence: `DeterministicGreedyJoinSeed`, `JoinSearchService.ScopedMemoTarget`, and
  `CascadesJoinSearchWorkLimitTest#deterministicGreedyOrderUsesPrimitiveStateAndExactWorkCounters`.

- Observation: cross-group insertion of an already-interned typed logical key is possible through ordinary BAG
  simplifications, but `aliasedGroupsByLogicalExpression` is write-only. Expressions, winners, facts, revisions, and
  wake-ups therefore remain split even after equivalence is proven. A safe repair cannot blindly merge SET or
  EXISTENCE routes: canonical expression identity must be separated from scope-qualified target-group membership,
  with deterministic BAG union/rebuild as the incremental migration path.
  Evidence: `Memo.addLogicalAlternativeWithInputs`, `StructuralCascadesRules.RedundantProjectionRemovalRule`, and the
  read-only typed-identity alias audit recorded on 2026-07-16.

- Observation: `JoinRegionMemo` originally retained every accepted candidate object indefinitely, because an evicted
  child could still be referenced by a live parent recipe. Counting frontier entries therefore understated the real
  retained payload. Monotonic IDs now index primitive reference counts and tombstoneable payload slots; transactional
  projection preserves a child through its final live parent and reclaims the dead derivation cascade without ID
  reuse. A memo-owned removal buffer also avoids replacing the leak with one temporary set per candidate offer.
  Evidence: the preserved red in `initial-evidence.optimizer-retention.txt`,
  `JoinRegionMemoTest#evictedCandidateIsReclaimedOnlyAfterLastLiveParentDisappears`, and green workspace run
  `20260716T235516.084769Z-81645-0b171c81`.

- Observation: an isolated allocation test can hide immutable collection-view allocation after C2 scalar replacement.
  The bitmap restriction test passed alone after set-mask caching but allocated 4.32 MB when executed after the full
  cost-model class because `EvidenceProfile` accessors recreated empty `Map.keySet()` views. Caching the complete fact
  mask per immutable `BindingProfile` in the query-local universe makes the contract independent of compilation order
  and collection implementation.
  Evidence: workspace runs `20260716T232337.288945Z-52719-9a1c25bc` and
  `20260716T232451.707167Z-56434-cc4ec183`; the final seven-class run is
  `20260716T232654.595802Z-62615-6b3a6ee5`.

- Observation: explicit enforcer child groups introduce a second, deliberately relaxed goal for the same memo group.
  Scheduling every enforcer under that child goal performs a full immutable-expression match even though the property
  it supplies is absent. Declaring the supplied goal dimension on the rule descriptor lets the indexed registry reject
  that impossible match without a rule-ID special case and retains one match per expression for the applicable goal.
  Evidence: `CascadesRuleDependencyRevisionTest#expressionLocalEnforcerMatchesEachImmutableExpressionOnce` and
  workspace run `20260716T234028.052105Z-24758-98ce881c`.

- Observation: the medical query-9 exact characterization previously allocated roughly 69 GB in a ten-second sampled
  run, dominated by `HashMap`, `LinkedHashMap`, and fragment-state construction. Coalescing discovery reached join
  enumeration but exposed an owner-group invariant where the same logical join state was registered under competing
  groups. Incremental fragment composition and canonical-first registration are present in the current branch and
  must be preserved while the scoped memo removes the root allocation cause.
  Evidence: `/tmp/lmdb-medical-q9-exact-after-fragment-alloc/` and the focused
  `CascadesJoinRegionEnumerationTest` / `MemoJoinRegionExtractorTest` regressions.

- Observation: `AUTO` is parsed separately today, but only `BUDGETED` and `SHADOW_BUDGETED` take the bounded scheduler
  path. The prior plan deliberately made `auto` drain like exact. This conflicts with the reviewed deterministic AUTO
  contract and is an intentional behavior change, not a rename.
  Evidence: `LmdbCascadesOptimizer.modeFromProperty` and `usesBudgetedSearch`.

- Observation: production memo lookup does not appear to rely on record-generated `MemoExpr.equals/hashCode`. Memo
  lookup uses diagnostic string keys and dense IDs, expression replacement compares IDs, and the cost model uses an
  `IdentityHashMap`. One test performs `assertEquals` on the same object and can safely become `assertSame` during the
  final record-to-class conversion.
  Evidence: `Memo.java`, `MemoGroup.java`, `CascadesCostModel.java`, and
  `CascadesMemoModelTest#expressionCanBeRetrievedByDenseId`.

- Observation: the authoritative `logicalFingerprint` specially encodes StatementPattern, Filter, and VALUES but
  reduces most other operators to class plus sorted possible/assured bindings. Two Projections that map `a,b` to
  `x,y` in opposite source order therefore coalesce even though they evaluate differently. Projection IR already
  carries ordered source/target pairs and subquery state, but drops `projectionContext` and explicit scope-change
  state.
  Evidence: `Memo.intern`, `MemoExpr.logicalKey`, `CascadesCostModel.logicalFingerprint`, `IrAttr.Projection`,
  `TupleExprToIr`, and `IrToTupleExpr`.

- Observation: join extraction currently tries node identity, then mutable `TupleExpr.equals`, then returns the first
  expression with the same operator name. The last fallback is fail-open and can select the wrong logical alternative
  after a false merge.
  Evidence: `MemoJoinRegionExtractor.findLogicalExpression`.

- Observation: the public configuration accepts `shadow-budgeted`, but the current planned annotation uses the enum
  spelling `shadow_budgeted`. Compatibility characterization must lock both sides rather than assuming they are the
  same serialized form.
  Evidence: `LmdbCascadesPropertySurfaceTest#modeAliasesDefaultsAndSerializableRoutingRemainStable`.

- Observation: LMDB's same-label maximum-spanning-forest selection is conceptually conservative, but its incremental
  ratio can legitimately exceed one when a stronger edge replaces an earlier weak edge. The current clamp forbids
  that correction: a `.01`, `.9`, `.9` triangle can retain `.01` instead of the canonical `.81` depending on join
  association.
  Evidence: `PlanHypergraph.newlySatisfiedSelectivity`, `sameLabelSelectivity`, and `CostingReceiver`.

- Observation: raw product caches erase the difference between exact zero and positive underflow. An invalid or
  underflowed denominator returns neutral selectivity `1.0`; join costing can overflow `leftRows * rightRows` before a
  selective predicate is applied; and `sqrt(forward * backward)` can underflow even when both directional ratios are
  representable. Exact-zero base factors are also floored during search and repaired only at export.
  Evidence: `PlanHypergraph.selectivityProduct`, `LmdbHypergraphJoinPlanner.pairSelectivity`,
  `CostingReceiver`, and `HypergraphOptimizer`.

- Observation: the ordinary statistics fallback itself converted invalid cardinality to `1.0`, and a present primary
  `FactorCostEstimate` with `NaN` rows bypassed fallback and normalized to `Double.MAX_VALUE`. Typed unavailable
  selectivity alone was therefore insufficient: estimate validation and fallback behavior had to fail closed before
  DPhyp could surface the unavailable edge and decline.
  Evidence: the preserved failures for
  `invalidDirectionalEvidenceDeclinesInsteadOfBecomingNeutralSelectivity` and
  `invalidPresentDirectionalEstimateUsesFallbackInsteadOfMaximumRows` in
  `initial-evidence.optimizer-hardening.txt`.

- Observation: keeping the maximum-spanning-forest selection in LMDB would still leave two canonical-cardinality
  policies once the scoped core memo arrives. `JoinCardinalityModel` now owns ordinary-predicate composition and
  equality-class forest selection; `PlanHypergraph` only adapts eligible long-mask predicates into typed terms.
  Evidence: `JoinCardinalityModel#estimate`, `PlanHypergraph#uncachedCardinality`, and
  `JoinCardinalityModelTest#sameEqualityClassUsesCanonicalMaximumSpanningForest`.

- Observation: the first canonical model still ranked an exact-zero equality below positive estimates. It also let
  LMDB infer equality semantics from a repeated diagnostic label, discard unavailable subset evidence, reinterpret a
  factor fallback as joined rows, overwrite logical rows during physical export, and floor tiny nested prefixes.
  These are separate contract violations that a full-state log formula alone does not prevent.
  Evidence: the five preserved audit regressions added after the first Milestone 2 green in
  `initial-evidence.optimizer-hardening.txt`; the broadened classes now pass 6/6, 20/20, and 28/28.

- Observation: a test-only direct-planner seam can characterize one query and policy without routing through the
  mutable production optimizer or requiring reflection. Query 0 under EXACT currently completes with no pending work
  and a stable semantic/physical fingerprint pair; each remaining query/policy pair still needs its own JVM so a
  resource-heavy row cannot hide later outcomes.
  Evidence: `LmdbMedicalCascadesPolicyCharacterizationIT` and workspace run
  `20260716T101749.278844Z-70456-e373ed46`.

- Observation: the medical baseline does not merely contain slow winners. Across EXACT, ROBUST, and FEEDBACK_AWARE,
  queries 0, 1, 6, and 8 are complete; queries 2, 3, 4, 5, 7, and 10 report respectively 2, 41, 13, 304, 2, and 17
  pending unsupported-boundary tasks. Their semantic and physical fingerprints are invariant across policies. Query
  9 reaches the global state-materialization failure before a policy-specific winner can be emitted: each isolated
  fork remained CPU-active beyond the declared 180-second guard, ignored the JUnit interrupt, and required external
  termination. This is a characterization result and a scoped-memo acceptance gate, not a benchmark-specific branch.
  Evidence: workspace logs `20260716T104212.467261Z-70511-ebb455cb` through
  `20260716T110134.641585Z-70499-e8efcaf8` for the 30 rows, plus query-9 logs
  `20260716T110200.886500Z-49983-4cc51544` (FEEDBACK_AWARE),
  `20260716T110725.605521Z-67727-080a1785` (ROBUST), and
  `20260716T111145.545210Z-85323-07ae3979` (EXACT).

- Observation: the legacy LMDB planner still owns opaque composite classification and Explain repair behavior that the
  core extractor does not yet model with typed LEFT, SEMI, and ANTI edges. Removing it before parity would trade an
  architectural split for SPARQL bag-semantics regressions.
  Evidence: `LmdbJoinIslandConnectivity`, `LmdbCascadesConnectedJoinPlanner`,
  `LmdbCascadesExplainFinalizer`, and `LmdbCascadesOptimizer`.

- Observation: copying an enforcer's captured physical tree is not a safe way to recover its memo child. Sort over a
  Materialize physical expression re-imported the marker as a new opaque logical occurrence and expanded the exact
  search until the Surefire fork exhausted heap. Supplying the source group ID explicitly makes the same combined goal
  complete in 0.215 seconds and keeps the marker out of logical groups.
  Evidence: `CascadesEnforcerCompositionTest#orderingAndMaterializationReuseTheAuthoritativeGroup`, red workspace log
  `20260716T115812.917637Z-73564-e5094c61`, and green log
  `20260716T120145.432868Z-83710-62986943`.

- Observation: erasing a property-only Materialize marker after building the selected plan has two independent
  obligations. The marker contributes zero local execution work, and the unwrapped child must receive the selected
  root metrics and have a null parent. Failing either contract respectively invented five work rows, left an invisible
  parent, or threw because a one-node marker snapshot was applied to a four-node StatementPattern tree.
  Evidence: `CascadesCostModelTest#materializationMarkerAddsNoLocalWork` and `SelectedPlanMaterializerTest`.

- Observation: a proven rewrite can intentionally place a typed logical expression in a different owner group even
  when the same expression already has a canonical import group. The existing route-only topology regression exercises
  this case. Typed identity therefore needs explicit alias membership for rule-proven group placement; ordinary import
  lookup still resolves only the canonical typed-key map.
  Evidence: `MemoJoinRegionExtractorTest#routeOnlyTopologiesDoNotRecursivelyExposeTheSameRegion`,
  `Memo.registerLogicalAlias`, and the green ten-test extractor class.

- Observation: the first subquery-state red accidentally compared two default-subquery Projections because RDF4J's
  `Projection` constructor defaults that flag to true. Correcting the fixture to compare false with true left the
  intended production collision exposed and avoided encoding a false premise into the regression.
  Evidence: `CascadesMemoModelTest#projectionSubqueryStateIsPartOfLogicalIdentity` and the preserved initial class
  failure report.

- Observation: child physical requirements and proof-cleared semantic barriers are route attributes, not logical
  operator identity. Collapsing them into `MemoLogicalExpressionKey` either falsely splits an equivalent logical group
  or loses the distinct reconstruction/context-propagation route. A separate `MemoLogicalRouteKey` preserves both
  contracts without reintroducing string equivalence.
  Evidence: `CascadesPhysicalInputRequirementsTest`,
  `CascadesSemanticScopeAdmissibilityTest#selectedBarrierFreeChildRetainsOuterInvocationMassAcrossFinitePrefix`, and
  the authoritative maps in `Memo`.

- Observation: exact-class registration is required for typed operator encoding. A native subclass of a known RDF4J
  operator can add evaluation behavior that the base operator attributes do not describe; treating it as the base
  operator recreates false coalescing. Unregistered subclasses are therefore opaque occurrences until a semantic
  codec explicitly registers their complete state.
  Evidence: `CascadesMemoModelTest#unsupportedNativeOccurrencesNeverCoalesce` and the former custom
  `SetOnlyStatementPattern` / `ProofDerivedStatementPattern` regressions.

- Observation: the complete queryalgebra verification is already red on clean `HEAD`. Running the same four failing
  classes from an archived `HEAD` checkout produced exactly five failures and three errors; the working tree produces
  the identical set after the typed-identity cut. This makes the comparison baseline explicit instead of incorrectly
  attributing inherited failures to the migration.
  Evidence: `/tmp/rdf4j-typed-identity-baseline`, the clean-HEAD report, and workspace log
  `.mvnf/workspaces/optimizer-hardening/logs/20260716T093348.470642Z-25438-69bcf683/verify.log`.

- Observation: enforcer composition exposed a structural problem before cost-policy work: Sort wrapped a cloned
  Materialize marker, tree-shape child inference interned it as an opaque occurrence, and the memo expanded until the
  test fork exhausted its heap. Explicit source-group IDs make enforcers transparent and converge in 0.259 seconds.
  The marker also needed zero local work and selected-metric transfer before erasure; otherwise a non-executed node
  was charged and left an invisible parent.
  Evidence: the preserved failures in `initial-evidence.optimizer-hardening.txt`, `RuleApplication`,
  `SelectedPlanMaterializer`, and `CascadesEnforcerCompositionTest`.

- Observation: `CostVector` priced uncertainty during construction and its context-free `objectiveScore()` was used
  even for exact search. The planner then called `applyCostPolicy` before insertion, while the new frontier also had
  to rank by policy. Storing raw costs and deriving a policy-ranked `ExecutionCost` only at comparison removes both
  the exact-policy contamination and the robust double-adjustment path.
  Evidence: `CostOrderingTest#robustTransformsRawResourcesExactlyOnce`,
  `CascadesRuleEngineTest#plannerLeavesCostPolicyAdjustmentToTheWinnerFrontier`, and the 60-test memo-model pass.

- Observation: adding three search modes exposed nineteen raw `BUDGETED`/`EXACT` comparisons in the scheduler.
  SHADOW was incorrectly requesting bounded join search; SHADOW_BUDGETED missed timed-budget scheduling; and AUTO
  received unbounded winner frontiers and join candidate budgets. These branches must be expressed through three
  explicit predicates: exact-like (EXACT/SHADOW), timed-budgeted (BUDGETED/SHADOW_BUDGETED), and bounded-search
  (AUTO/BUDGETED/SHADOW_BUDGETED).
  Evidence: the line-by-line `CascadesPlanner` mode audit completed at 2026-07-16 13:23Z.

- Observation: selected-plan extraction removed `MaterializeTupleExpr` from executable algebra but retained the
  marker in nested `PlanProvenance`. A parent such as Difference then paired a leaf StatementPattern with unary
  provenance and failed Explain annotation with `slots=0, inputs=1`.
  Evidence: `SelectedPlanMaterializerTest#removedMaterializationMarkerAlsoRemovesItsProvenanceLayer`, red workspace
  log `20260716T132911.491268Z-33758-175e562e`, and green log
  `20260716T133104.396645Z-49615-4171e184`.

- Observation: the first primitive LMDB adapter reused legacy `prepareGraph`, so topology discovery still called
  private factor-cost, cardinality, conditional-selectivity, path-anchor, and finite-anchor heuristics even though
  the resulting winner tree was discarded. A cost model that throws on every probe exposed this immediately.
  Evidence: red workspace log `20260716T164526.097883Z-18666-69cf61ea`, green log
  `20260716T165055.781580Z-44229-5bc5b26b`, and
  `LmdbDphypJoinSearchContributorTest#scopedTopologyDoesNotConsultPrivateCardinalityEvidence`.

- Observation: DPhyp announces singleton nodes in descending seed order and may emit valid pairs before reaching a
  later missing singleton. Returning `UNSUPPORTED` after those emissions violates the contributor contract and
  caused the unified service to throw. All singleton candidates must therefore be preflighted transactionally.
  Evidence: `LmdbDphypJoinSearchContributorTest#missingLateSingletonEmitsNoPrimitivePairs` in the same red/green logs.

- Observation: one scoped scheduler test requested a tenant-parameterized winner from plain
  `EvaluationStatistics`, which intentionally exposes only independent scan access. The route correctly propagated
  the tenant requirement but no executable implementation could satisfy it; the fixture, not the scheduler, lacked
  the promised physical lookup evidence.
  Evidence: red log `20260716T162638.999314Z-38311-3f887642`, green log
  `20260716T163944.647398Z-98520-e526f52a`.

- Observation: candidate-local physical costing originally cloned the complete selected child trees into every
  intermediate scoped Join and retained those detached nodes in identity caches. A 14-factor update therefore
  exhausted a 4 GiB heap in `MemoExpr.structuralKey` even though the deterministic search limits were obeyed. Scoped
  candidate identity needs compact child references; executable trees belong only to selected root-DAG export.
  Evidence: `LmdbDeleteInsertTest#test`, red log `20260716T171600.013358Z-37914-f18491d7`, green log
  `20260716T172943.130960Z-85319-70043df1`, and `ScopedMemoInputReference`.

- Observation: RDF4J's mutable algebra binding getters are deliberately conservative or structurally approximate:
  Extension omits total derived outputs, Group assures every aggregate output, and BindingSetAssignment reports the
  union of row names as assured. Those values cannot authorize OPTIONAL-to-ANTI rewrites. One immutable IR proof now
  owns the rewrite, stream schema, and DSL guard, including incoming bindings.
  Evidence: the four-test red log `20260716T173448.191948Z-12119-40e571f5`, green log
  `20260716T174228.775506Z-43007-95be5126`, and the 243-test broadened pass ending `ce67c78b`.

- Observation: one synthetic `ANY` goal cannot seed every scoped factor. `PhysicalProperties.satisfies` uses the
  requested bound-variable set as the whitelist for delivered external parameterization, so erasing that set rejects
  a valid bound lookup before local search begins. The root whitelist must be projected through each factor's
  possible bindings and reused verbatim in winner and dependency keys.
  Evidence: `CascadesRuleEngineTest#scopedJoinRightDoesNotLeakAnInternalBindingRequirementToItsParent`, red log
  `20260716T175125.989706Z-92165-40cb6984`, and green log
  `20260716T175445.842121Z-9221-01e2fe1e`.

- Observation: scalar-scope barrier validation flattened `N` factors but recomputed a recursive Set-backed stream
  schema for every Filter/factor pair. The exact UNION factoring benchmark stack therefore normalized and copied
  string sets in `O(F x N)` schema traversals even though the planner already owned dense query-local binding IDs. A
  192-factor repository regression measured 77,731,648 allocated bytes in one analyzer call.
  Evidence: `StreamBindingSchemaTest#scalarScopeAnalysisUsesQueryLocalBindingMasks`, red log
  `20260716T203806.176933Z-28579-c4459b52`, and green log
  `20260716T204342.114484Z-46534-ac35148c`.

- Observation: replacing string sets with bitmaps requires explicit empty-mask discipline. `containsAll(EMPTY)` is
  mathematically true but cannot serve as semantic evidence that a reserved/invalid binding exists. The first mask
  implementation falsely proved a negated-BOUND OPTIONAL to be an anti join until the tested mask was required to be
  nonempty. Likewise, VALUES assurance must be intersected inside its authoritative declared schema rather than over
  arbitrary row names.
  Evidence: `StreamBindingSchemaTest#reservedNegatedBoundNameCannotProveAntiOptional` and
  `BindingShapeTest#maskNativeFiniteRelationHonorsItsAuthoritativeSchema`, with red logs
  `20260716T212239.839165Z-32366-a9c72f8a` and `20260716T211809.347614Z-16499-26157022`.

- Observation: removing the profiled schema walk exposed three secondary name-allocation routes in join preparation:
  predicate eligibility rebuilt assured-name sets per state, the deterministic seed sorted every binding name into a
  private ordinal map, and extractor fragment comparison converted sorted sets to strings. All three already shared a
  stable memo `BindingUniverse`; retaining a second name-based identity was unnecessary and obscured correctness above
  bit 63. The mask migration preserves exact seed counters and scalar correlation decisions while deferring name
  materialization until a user-facing boundary diagnostic is actually emitted.

- Observation: after the mask conversion, allocation sampling no longer contains the user-reported
  `Set.copyOf -> StreamBindingSchema.immutablePlannerNames -> StreamBindingSchema.from ->
  ScalarDependencyAnalyzer.reorderingPreservesScalarScope -> StandardCascadesRules.barrier` stack, nor the secondary
  `StreamBindingSchema.from -> HashJoinImplementationRule.matches` route. Remaining sampled name materialization is at
  separate physical-property and cost-reporting compatibility boundaries. The profile is
  `/tmp/rdf4j-async-profiler/medical-q5-bitmap/post-bitmap-alloc-traces.txt`; the reproducible JMH comparison is
  `/tmp/rdf4j-async-profiler/medical-q5-bitmap/medical-q5-bitmap-compare.md`.

- Observation: a read-only follow-up audit confirmed that the exact reported barrier stack is structurally
  unreachable, but detached scoped candidate costing still crosses the Set schema boundary in delivered-property,
  finite-conditioned-output, and parameterized-join estimation. The valid final in-process allocation profile
  attributes 0.07% of sampled allocation to `StreamBindingSchema`; an earlier call-tree profile ties those samples to
  `CascadesJoinImplementationProvider -> ScopedJoinSearch`, not rule matching. If a future profile makes that residual
  material, the fix is to carry the query-local possible/assured `BindingShape` through the typed cost context, not to
  add another name-set cache. Legacy connected-hypergraph match/apply and finite-membership reactivation also invoke
  the compatibility schema repeatedly, but they are bounded, transitional, and unsampled: delete them at cutover or
  feed them memo-owned shapes rather than independently optimizing their mutable-tuple caches. No speculative LMDB
  churn is justified by the current evidence.

- Observation: the dependent-parent application counter originally treated a detached scoped physical Filter and its
  global memo implementation as one domain. Typed EXISTS search legitimately invokes the shared local estimator once
  while evaluating the scoped SEMI edge, then global memo costing invokes it once for the exported logical recipe.
  Instrumented stacks and stable revision snapshots showed no second global claim and no
  `DependentTraversalStamp` invalidation. Tests must distinguish these domains structurally by the scoped input
  reference while continuing to require exactly one application per canonical input state within each domain.

- Observation: scoped join alternatives can falsely satisfy `EXACT_SEQUENCE`. Candidate-local costing builds a
  detached logical expression without transformation provenance; the generic physical rule interprets an empty proof
  list as the original written topology and declares exact observation order. The sequence property is only a flag,
  not an identity, so local property dominance cannot distinguish two different orders. Until sequence provenance is
  typed, the sound behavior is to skip join discovery entirely for an exact-sequence goal, preserve the imported
  topology, charge zero partition work, and remain complete through ordinary implementation.
  Evidence: read-only audit of `CascadesPlanner`, `CascadesJoinImplementationProvider`,
  `StandardCascadesRules.GenericImplementationRule`, and `TypedPhysicalPropertyBridge` at 2026-07-17 03:10Z.

- Observation: the opaque-factor policy computes legacy-compatible descriptors, but the canonical extractor rejects
  every descriptor with a semantic barrier or nonempty required-input mask. Only self-contained Extension currently
  reaches scoped search. Moreover, the legacy deletion-gate fixture calls the old planner directly while
  `LmdbConnectedHypergraphJoinImplementationRule` has no production registration, so it proves code reachability but
  not the locked production-route claim. Remaining legacy hypergraph APIs are still consumed by the DPhyp contributor,
  therefore deletion is unsafe even though whole-tree production registration appears to have already disappeared.
  The next parity slice must carry one correlated Extension's required input and unique assured producer dependency
  rather than weakening barrier checks wholesale.
  Evidence: read-only opaque parity and rule-registration audit at 2026-07-17 03:12Z.

- Observation: broadening the bitmap selection exposed
  `selectedBarrierFreeChildRetainsOuterInvocationMassAcrossFinitePrefix` failing because no cost context combines the
  49,600-row outer invocation mass with ten exact VALUES rows. Restoring the original set-based global hash matcher
  produced the identical failure, so it is independent of this allocation fix. The exact cause was a unit mismatch:
  finite-relation evidence overwrote a context-total winner's selected rows with the ten per-invocation relation rows.
  Preserving the winner's context-total unit fixes the downstream 496,000-row lookup context without weakening scoped
  dominance. Both reds and the direct unit regression are preserved in the workspace logs and compact evidence file.

- Observation: the first scoped mask migration cached shapes by physical `Winner` before the memo decided whether the
  candidate survived dominance or retention. A one-entry frontier therefore retained three shape entries. Logical
  possible/assured bindings are subset facts, not physical-candidate facts; moving them into the provider input
  contract makes the provider stateless and prevents rejected implementations from extending object lifetime.

- Observation: reclaiming scoped candidate payloads did not reclaim the boxed duplicate-suppression records that
  named those candidates. Candidate IDs are monotonic and their liveness transitions are exact, so a bitmap can
  invalidate every dependent join and predicate key in O(1), without scanning either evaluation table. Lazy stale-key
  removal plus same-capacity compaction prevents released generations from forcing unbounded table growth. The
  focused pre-extraction and post-extraction selector stayed green 1/1, the lifecycle regression failed with three
  assertions before the primitive ledger, and the preserved failure is in
  `initial-evidence.optimizer-ledger.txt`.

- Observation: the branch-local safety caller already consumed `BindingMask`, but its helper routed Extension and
  VALUES names through the diagnostic Set collector and then converted that Set back into query-local symbols. This
  created a mutable `HashSet`, an immutable Set copy, and the bitmap on every scoped-UNION safety check. A mask-native
  visitor removes both transient string-set layers without changing which algebra boundaries are traversed.

- Observation: the cost model's bound-variable inputs were already immutable at every root: `PhysicalProperties`
  snapshots required bindings, `InputBindingContext` canonicalizes assured bindings, and recursive estimate/factor
  calls propagate a prior key's names or `Set.of()`. The later `immutableBoundVars` call therefore copied an immutable
  contextual set on every cost application and prevented the existing identity-based stable-mask cache from hitting.
  Caching the resolved set by immutable goal identity makes a genuinely combined required/contextual union a one-time
  query-local snapshot as well.

- Observation: candidate delivered-property composition merged a child's complete `Set<String>` bound surface and
  `BindingProfile` before immediately replacing both with the candidate output schema and reconstructed output
  profile. A 193-symbol projection therefore rebuilt wide hash-backed sets for every physical alternative even though
  only one symbol survived. Expanding the output mask to names on the restriction slow path repeated the same work.
  Restricting against the query-local mask before merge, caching only immutable fact-boundary normalizations, and
  reusing immutable no-op transformations removed 47,924,448 measured bytes without a parent-expression cache that
  would miss cloned rule outputs.

- Observation: a mask-based analysis is not complete merely because it avoids sets. The original direct-Filter walk
  missed the same scalar correlation below a transparent Projection. Recursive discovery plus per-symbol supplier
  counts catches dependencies above bit 63 without counting an alias created by the same factor as an external
  supplier, while subquery Projection remains an explicit isolation boundary.

- Observation: EXISTS import retained a separate name-materialization loop after surrounding scalar analysis moved
  to query-local masks. The old path collected all tuple-subquery variables into an immutable string set, materialized
  visible names, intersected another string set, and interned both results back into the same universe. A 72-visible
  plus three-local binding characterization proves direct reference collection and mask intersection preserve both
  multiword correlation and exclusion of subquery-local names.

- Observation: backend opaque classification is only safe when the core verifies the descriptor against memo-owned
  binding masks. Trusting name sets directly would let a stale or incomplete backend descriptor widen the legal
  reorder space. The success path therefore converts each descriptor surface once and performs only mask equality,
  containment, and sibling intersection; names are rematerialized only for a rejection diagnostic. The policy-free
  extractor and `JoinSearchService` constructors retain their previous ordered-barrier behavior.
  Evidence: `LmdbOpaqueFactorJoinPlanningTest#coreExtractorConsumesOpaquePolicyAndFailsClosed`, red log
  `20260717T001511.658590Z-52366-007abdc4`, green log
  `20260717T002249.503092Z-78062-36d56bac`, and compact red evidence
  `initial-evidence.optimizer-opaque.txt`.

- Observation: the affected LMDB breadth run's budgeted OPTIONAL failure was trace rollover, not semantic scheduler
  loss. The 4,096-entry `CascadesTelemetry.Recording` ring evicted the early accepted-alternative line while retaining
  the final winner, which delivered `optionalAnchoredLookup` with the expected implementation proof. Exact stop facts
  were `DEADLINE_EXPIRED`, rule work 1,427, partition probes 30,301, candidate evaluations 65,242, predicate probes
  zero, and retained-candidate high-water 1,278: neither the 4,096 rule-work limit nor a missing child winner caused
  the failure. The original red is preserved in `initial-evidence.optimizer-budget.txt`; the counter-trace run is
  `20260717T003729.406778Z-44222-4a0d7976` and the green run is
  `20260717T003934.539213Z-53558-4a8f7537`.

- Observation: the first shared OPTIONAL fact record retained mutable `TupleExpr` and re-walked it during analysis,
  despite promising immutable facts. A later algebra rewrite could therefore change an already-captured decision.
  Structural safety is now snapshotted beside the immutable binding masks and barrier value.
  Evidence: `CascadesIndependentOptionalJoinRegionTest#optionalSafetyFactsSnapshotMutableAlgebra`, red log
  `20260717T022620.507060Z-76259-9b0096e5`, green log `20260717T022814.770969Z-88962-cda6a672`, and
  `initial-evidence.optimizer-optional-swap.txt`.

- Observation: classifying a canonical legacy gap across all memo alternatives made later generic rewrites withdraw
  the compatibility candidate. In the opaque UNION regression, `join-union-distribution` created a supported
  alternative after the source expression had already exposed a legacy-owned gap. Exact-source classification keeps
  the candidate available without suppressing the new alternative. The resulting cost competition selected an
  independent UNION-to-VALUES plan and exposed a separate semantic bug: valued nonconstant StatementPattern variables
  are observable bindings, so projecting their names without recreating the bindings changed the result bag.
  Evidence: red logs `20260717T044620.380034Z-38177-595c17b6` and
  `20260717T045606.458260Z-88244-bb3f5e48`; green log
  `20260717T050749.155718Z-33752-b8a43da8`; compact evidence
  `initial-evidence.optimizer-legacy-gap.txt`.

- Observation: a false `variableScopeChange` flag is necessary but not sufficient evidence that UNION can move as an
  atomic outer factor. A branch can still contain an Extension or Filter with an external scalar input, and a binding
  produced by only one branch remains unsafe when a sibling consumes it. A recursive whitelist of closed graph-pattern
  leaves, Joins, and nested safe UNIONs supplies a finite structural proof; every other branch form stays behind the
  existing outer-placement barrier. The focused extractor selection proves the safe, external-input, and branch-only
  cases together at 3/3 in log `20260717T063200.560553Z-60845-1268d494`.

- Observation: LeftJoin needs two independent legality channels. Its right-only bindings remain nullable occurrence
  hazards even when the subtree is structurally closed, while a condition or non-graph child can carry dependencies
  that the outer factor descriptor must not guess safe. Reusing the closed graph-pattern proof establishes structural
  mobility; the existing possible/assured and unsafe-sibling masks then reject optional-only sharing. Physical
  `EXACT_SEQUENCE` remains the separate execution-order contract and selects the written OPTIONAL-first topology.
  Evidence is the 4/4 safety log `20260717T065748.816324Z-80872-b06deef5` and 2/2 parity/order log
  `20260717T065906.543890Z-86992-03900734`.

- Observation: the post-UNION broad core run found two `UNSUPPORTED_ATOMIC_BOUNDARY` completeness regressions in
  `equivalentFilterJoinTopologiesReuseCanonicalUnfilteredState` and
  `exactJoinSearchEnumeratesCorrelatedNotExistsAtEveryAssuredPrefix` after the concurrent typed memo-derivation cut.
  LMDB's UNION policy is not active in either core-only fixture; the same run keeps `MemoJoinRegionExtractorTest`
  green at 16/16, while the UNION-focused, complete opaque, and complete shadow selections remain green. The typed-
  derivation owner accepted these failures for its provenance canonicalization slice. Evidence is in workspace log
  `20260717T064407.141080Z-32837-e5c84e50`.

- Observation: proof-distinct logical alternatives exposed three apparent unsupported join boundaries, but all three
  were `MISSING_LOGICAL_EXPRESSION` aliases rather than semantic barriers: a Join using group 8, a Filter using group
  6 through group 7, and a Join using group 6 through group 9. Resolving one unique proof-free typed route per group
  restored exact extraction while still failing closed when the mutable reconstruction bridge no longer matched the
  written typed operator. Evidence is in logs ending `573141da`, `77259bc2`, `1844e18a`, and `c73eb02b`.

- Observation: the recursive compiled finite-filter helper cloned the enclosing QueryRoot and attached a child-only
  rewrite certificate to that root route. Migrating this rule to a local Filter pattern fixed the provenance leak.
  The remaining recursive tuple rewrites (`removeUnusedOptional`, `orFilterValues`, `orFilterUnion`, and
  `filterMinusLeftPushdown`) must likewise move to local memo expressions before provenance cutover can claim that
  route-local physical proof union is globally safe.

- Observation: deterministic fair lanes did not by themselves remove the legacy recursive planner. AUTO still called
  `optimizeGroup`, whose `seedPhysicalWinners` applied every specialized implementation without charging work,
  `exploreGroup` ran every rule for one group inline, and the physical snapshot cost every alternative recursively.
  The expression queues also lacked an `OptimizationGoal`, so simply deleting recursion would optimize child groups
  under the root requirements and strand parameterized parents. A two-unit AUTO regression reached its limit after a
  typed root rewrite, then incorrectly implemented and selected the late finite relation for free instead of retaining
  the reserved original StatementPattern. This red is preserved in workspace `optimizer-core-fallback`, run
  `20260717T133210.693126Z-40135-07f4251d`. The next cut therefore makes expression work goal-bearing, registers every
  requested `(group, goal)`, requests missing child work and defers the parent, and moves AUTO/EXACT/SHADOW to the fair
  queues. Timed BUDGETED remains an explicitly isolated compatibility route until equivalent priority semantics are
  characterized. The original executable reservation remains the only uncharged implementation path.

- Observation: `PrimitiveDphypJoinSearchContributor` inherited `JoinSearchContributor.contributeMemoExpressions`,
  whose compatibility default calls `contribute()` and decomposes the returned complete tree. Scoped production
  search bypassed that method, but any region still using global memo enumeration could therefore request LMDB's
  privately ranked DPhyp winner before generic exhaustive enumeration. The focused red failed directly through that
  inherited default; the interface override removes the call without changing contributor registration or coverage.
  Evidence: `JoinSearchServiceTest#primitiveDphypMemoEnumerationNeverRequestsPrivateWinnerTree`, red workspace run
  `20260717T232006.413075Z-56162-0b770487`, and
  `initial-evidence.optimizer-primitive-dphyp-seam.txt`.

- Observation: `Function.mustReturnDifferentResult()` is sufficient evidence of volatility but its default `false`
  is not sufficient evidence that a plugin function is repeatable. Constant-argument registered functions can be
  query-stable, while row-dependent plugin functions and missing/dynamic calls must remain unknown. Conversely,
  treating every scalar subquery as unknown disabled established, safe EXISTS filter ordering. Recursive EXISTS
  classification therefore needs a positive structural proof and explicit SERVICE/tuple-function/foreign boundaries.
  Evidence: the five existing FilterOptimizer regressions in run
  `20260718T002013.368960Z-25951-dcaae676`, the focused external-boundary red in
  `20260718T002616.522088Z-49274-0eecc529`, and their 5/5 and 1/1 greens in runs
  `20260718T002238.743979Z-34842-d1c08534` and `20260718T002806.258056Z-55969-38d40630`.

- Observation: RDF4J result fingerprints retain an Extension target whose expression evaluates with an unbound input
  as the binding-name entry `derived=null`. Treating absence of the binding name as the expected error-row shape made
  the first LMDB parity assertion fail even though exact bag comparison was already equal. The corrected regression
  asserts the established fingerprint representation and continues to compare complete result bags.
  Evidence: LMDB workspace run `20260718T004355.195951Z-14272-c9fb9ca5` and the final 8/8 parity run
  `20260718T005017.987008Z-38429-aec71479`.

- Observation: expanding a child group from `Filter(code)` to `Join(VALUES, code)` makes surrounding discovery expose
  both the original two-factor view and the expanded three-factor view, but exporting the expanded root legitimately
  resolves back to the existing typed `Join(hasCondition, child-group)` key. Requiring a second parent tuple tree
  would duplicate one logical memo identity; the durable gate therefore checks the referenced child alternative,
  expanded extractor result, and parent search proof together.

- Observation: selected-prefix input consumption has two different scopes. A Difference root can preserve bindings
  supplied by its caller even when a nested materialized subquery makes the complete selected subtree unsafe as the
  parameterized RHS of another Join. Reusing one recursive syntax helper for both claims first hid the valid root and
  then, after a Difference special-case, admitted the unsafe parent route. The selected winner DAG is the authoritative
  place to cache the stronger route fact; delivered properties continue to describe only the root contract.

- Observation: accepting a cheaper winner can be a removal as well as an addition. Same-state replacement,
  executable-resource dominance, and bounded-frontier trimming silently evicted selected child states, so dependent
  parent applications could remain sealed and provenance could omit the rejected alternative. Returning displaced
  winners from the frontier insertion makes targeted invalidation and discard accounting one atomic memo mutation.

- Observation: a bounded scoped search can drain every registered task and still export the wrong global alternative
  when its root cap is applied independently in insertion order. Q3 therefore reported `COMPLETE` after 381 rule
  operations while selecting an 88-work VALUES rewrite; EXACT exported the later six-work retained-filter root. Each
  local frontier was correctly ordered, but `rootCandidateIds(16)` returned as soon as earlier outer-input groups
  filled the cap. Completeness of local exploration does not imply completeness of a capped, cross-group export.
  Evidence: the direct AUTO/EXACT diagnostic is in workspace run
  `20260719T050857.701379Z-82253-1789e32a`; the focused cross-group red is
  `20260719T051820.352310Z-12685-1b510b16`, and the corrected Q3 run is
  `20260719T052213.812989Z-22464-ccff510c`.

## Decision Log

- Decision: supersede the previous decision that production `AUTO` is unbounded `EXACT`. `AUTO` is deterministic and
  work-bounded, never deadline-bounded, and always retains a legal executable fallback.
  Rationale: unbounded exact search is not a production-safe default for dense or large regions, while deterministic
  counters make approximation reproducible and observable.
  Date/Author: 2026-07-16 / Håvard and Codex.

- Decision: supersede the previous DPhyp integration in which DPhyp contributes a privately ranked tree or global
  memo partitions. DPhyp streams raw CSG-CMP masks into the core `JoinRegionMemo` shared with generic enumeration and
  ordinary physical implementations.
  Rationale: a private cost currency cannot compare properties or implementations consistently, and global pair
  materialization grows with explored partitions rather than exported winner derivations.
  Date/Author: 2026-07-16 / Håvard and Codex.

- Decision: supersede raw Pareto retention by q-error, confidence, or evidence provenance. Exact and bounded policies
  dominate only on executable resources; ROBUST and FEEDBACK_AWARE transform those resources exactly once before
  comparison.
  Rationale: estimate evidence should trigger refinement and telemetry, but it is not an executable property and must
  not preserve otherwise dominated physical states.
  Date/Author: 2026-07-16 / Håvard and Codex.

- Decision: explicit `EXACT` is transactional. If any hard deterministic resource limit prevents proof, the result is
  `RESOURCE_LIMIT_EXCEEDED`, no partial or greedy candidate replaces the caller's algebra, and all exact counter
  snapshots remain observable.
  Rationale: applying an incomplete result while labelling the request exact makes the mode contract untrustworthy.
  Date/Author: 2026-07-16 / Håvard and Codex.

- Decision: a missing or otherwise incomplete EXACT root winner cannot activate the legacy standard-plan fallback or
  post-search subtree repair. The candidate may remain observable in diagnostics, but `cascadesApplied=false` and the
  caller's algebra topology remains unchanged.
  Rationale: a fallback is still a partial result and would violate transactional EXACT just as surely as applying a
  resource-limited Cascades candidate.
  Date/Author: 2026-07-16 / Codex.

- Decision: typed semantic identity becomes authoritative in one compiling migration. A bounded legacy-key comparison
  table is diagnostic-only and never a fallback lookup or correctness oracle.
  Rationale: dual authoritative maps can disagree based on insertion order. A shadow comparison is useful only if it
  cannot affect equivalence.
  Date/Author: 2026-07-16 / Håvard and Codex.

- Decision: a surrounding join search that consumes a newly expanded child route may coalesce its exported root with
  the existing typed parent expression. Reactivation completeness is proven by the expanded region and route proof,
  not by retaining a duplicate nested `TupleExpr` template in the parent group.
  Rationale: memo groups own equivalence while child group IDs own alternative topology; duplicating the parent would
  make representation count, rather than legal-plan coverage, the scheduler contract.
  Date/Author: 2026-07-19 / Codex.

- Decision: rule-proven cross-group placement is represented as explicit typed-key alias membership, not as a second
  authoritative lookup entry. Canonical imports continue to resolve through one `MemoLogicalExpressionKey -> group`
  map; aliases are visible only to the groups established by the proof-producing rule.
  Rationale: collapsing those groups would erase owner/route semantics, while publishing two authoritative mappings
  would reintroduce insertion-order-dependent equivalence.
  Date/Author: 2026-07-16 / Codex.

- Decision: memo logical identity and memo route identity are distinct typed contracts. The former contains only
  evaluation semantics and ordered logical children; the latter adds physical child requirements and per-input
  semantic-barrier routing used to reconstruct and propagate context.
  Rationale: physical implementation contracts must not split semantic equivalence, but route-specific proof state
  must remain observable when applying or extracting an alternative.
  Date/Author: 2026-07-16 / Codex.

- Decision: a rewrite that embeds its source equivalence group below a new logical operator is not a legal logical
  alternative. A genuinely parameterized implementation may reference its own group only through an enforcer goal
  descent or a right-side selected-prefix contract that introduces a non-empty required input; an unchanged
  `WinnerKey` makes that route statically non-progressing.
  Rationale: detached duplicate groups would violate typed clone identity, while allowing a logical self edge or
  deferring a same-goal physical self edge makes saturation and cost recursion cyclic. The complete graph-universe
  route is therefore physical and parameterized, not a logical expansion.
  Date/Author: 2026-07-17 / Codex.

- Decision: canonical join-state rows are computed once from base logs plus the shared model's selected predicate
  logs. Physical join costing consumes that result directly; no contributor may reconstruct parent rows from child
  rows and an incremental ratio.
  Rationale: a same-label forest can replace weaker evidence as a subset grows, so incremental selectivity is not
  monotone and may legitimately require a ratio above one. Direct state cardinality is association invariant and
  avoids underflow/overflow before cancellation.
  Date/Author: 2026-07-16 / Håvard and Codex.

- Decision: equality-class membership is explicit typed predicate metadata; labels are diagnostic-only and neutral
  dependency/finite-anchor edges carry connectivity without a synthetic selectivity-one predicate.
  Rationale: semantic transitivity cannot be inferred from text, and connectivity evidence must not silently become
  cardinality evidence.
  Date/Author: 2026-07-16 / Codex.

- Decision: canonical logical result rows survive physical materialization unchanged. Conditional access rows and
  filter pass ratios remain local implementation evidence and work inputs only.
  Rationale: allowing a chosen nested lookup to replace result rows makes cardinality association- and
  implementation-dependent after the search has already selected a canonical logical state.

- Decision: a directed edge's logical cardinality is estimated once through the shared implementation provider over
  its canonical typed edge state and stable occurrence-order factor templates. The first retained physical operator,
  its association, and implementation enumeration order never supply that logical evidence. An unavailable estimate
  leaves the reserved legal fallback intact; malformed evidence aborts the scoped search.
  Rationale: learning edge selectivity from the first physical candidate makes the same `(subset, predicates, edges)`
  state depend on enumeration order and can silently change every parent cost. A provider-level logical boundary lets
  backend statistics contribute evidence while keeping execution resources and physical identity implementation-
  specific.
  Date/Author: 2026-07-17 / Håvard and Codex.

- Decision: unary property enforcers carry their authoritative source group ID in `RuleApplication`; the planner does
  not infer that relationship by comparing cloned mutable child trees. Their child goal clears only the property the
  enforcer supplies.
  Rationale: goal recursion is the semantic contract. Tree-shape inference fails as soon as two enforcers compose and
  can create opaque groups or an unbounded self-expanding search.
  Date/Author: 2026-07-16 / Codex.

- Decision: memo winners retain raw `CostVector` as a compatibility carrier, but frontier identity, dominance, and
  ordering use typed raw `ExecutionCost`, separate cardinality, and `EstimateRisk`. Only `CostOrdering` may transform
  raw resources for ROBUST or FEEDBACK_AWARE; cost models and the planner may refine evidence but may not price a
  whole subtree by policy.
  Rationale: one policy owner prevents exact search from inheriting robust penalties and makes a second robust
  adjustment impossible in normal planner flow. Cardinality may guard parent-safe dominance during migration, but
  q-error, confidence, evidence count, and provenance cannot preserve or dominate exact alternatives.
  Date/Author: 2026-07-16 / Codex.

- Decision: one-shot interpreted planning remains the execution model. Use dense IDs, primitive masks and arrays, and
  scoped dynamic programming; add no runtime code generation, vector API path, primitive-collection dependency, or
  third-party optimizer library.
  Rationale: the dominant problem is asymptotic materialization and boxed state, while compilation latency cannot be
  amortized for irregular one-shot query planning.
  Date/Author: 2026-07-16 / Codex.

- Decision: a scoped join-region factor snapshot is built from every compatible global winner frontier for the same
  semantic scope, cost policy, row goal, and input context. The frontier's requested legacy physical contract is not
  itself a reason to discard a winner; the candidate's delivered typed properties determine scoped legality and
  dominance.
  Rationale: ordinary parent optimization can request `SELECTED_PREFIX` without ever producing a duplicate `ANY`
  frontier. Requiring the latter invents a missing-factor condition even though a legal implementation exists.
  Date/Author: 2026-07-16 / Codex.

- Decision: a medical characterization row is recorded only after the direct planner emits a winner and completeness
  state. A query that remains in global state expansion beyond the harness guard is recorded as policy-specific
  non-completion, never assigned a fabricated fingerprint, cost, or completeness status.
  Rationale: query 9 fails before cost-policy winner selection. Inventing a row would hide the exact allocation defect
  that the scoped join-region memo must remove.
  Date/Author: 2026-07-16 / Codex.

- Decision: the legacy LMDB route stays production-active until opaque-factor parity and each supported non-inner edge
  class pass their deletion gates. Cutover proceeds by region class, with pure-inner regions first.
  Rationale: the existing route encodes required-input and unsafe-shared-name behavior not yet represented by the core
  extractor. Shadow comparison lowers migration risk without hiding missing support.
  Date/Author: 2026-07-16 / Håvard and Codex.

- Decision: an opaque-factor policy contributes facts but never overrides core legality. Core accepts a classified
  composite as an unpinned factor only after exact possible-surface parity, assured-subset validation, empty outer-
  placement restrictions, satisfiable required inputs, and sibling-disjoint unsafe bindings. Factor-internal barriers
  keep the composite atomic but do not by themselves pin its whole occurrence. Any failed check rejects the complete
  candidate region; it does not export a partial region or silently restore an LMDB-private reordering.
  Rationale: backend knowledge can extend supported factor kinds, but semantic ownership and fail-closed behavior
  must remain uniform in the shared optimizer. The typed distinction lets safe Group factors participate without an
  extractor-side Group exception, while mask-native validation keeps repeated discovery off the prior `Set<String>`
  allocation path.
  Date/Author: 2026-07-17 / Codex.

- Decision: non-scope-changing UNION is outer-reorderable only when both complete branches recursively consist of
  StatementPattern, EmptySet, SingletonSet, Join, or another UNION satisfying the same proof. UNION remains a factor-
  internal barrier, and any scope change or unrecognized composite retains an outer-placement restriction. Core
  possible/assured, required-input, and unsafe-sibling checks remain authoritative after this structural gate.
  Rationale: treating `!variableScopeChange` as a blanket permission would admit external scalar dependencies and
  branch-local binding hazards. A narrow finite whitelist establishes the first safe cutover slice without inventing
  typed semantics for operators that still belong to later parity milestones.
  Date/Author: 2026-07-17 / Codex.

- Decision: an unconditioned, non-scope-changing LeftJoin is outer-reorderable only when both children satisfy the
  same finite closed graph-pattern proof used by safe UNION. LEFT_JOIN remains factor-internal; conditioned, scoped,
  or composite-child forms retain their outer restriction. Required inputs must remain empty, optional-only outputs
  remain unsafe sibling bindings, and exact observation order is enforced by physical properties rather than by
  globally pinning every safe OPTIONAL.
  Rationale: this lets generic cost-based search move a selective sibling before an atomic OPTIONAL without exposing
  or re-associating the OPTIONAL's children, while preserving SPARQL multiplicity, unbound-row, dependency, and
  observation-order semantics.
  Date/Author: 2026-07-17 / Codex.

- Decision: rolling telemetry text is a diagnostic window, not a selected-plan correctness oracle. Tests that claim
  a physical alternative survived must assert the selected winner's typed properties and rule provenance; trace text
  may supplement the failure message but may not be the sole evidence for acceptance or rejection.
  Rationale: verbose later rewrites legitimately evict earlier events from a bounded recorder even though the memo
  retains and selects the physical candidate. Increasing the recorder or optimizer budget would only move this false
  boundary and would not strengthen the semantic contract.
  Date/Author: 2026-07-17 / Codex.

- Decision: primitive DPhyp accepts only dependency-free inner topology until directional typed edge legality is
  enforced by the scoped memo. Declared `requiredBefore` state and unresolved factor inputs make that contributor
  return `UNSUPPORTED`; the generic legal enumerator remains active.
  Rationale: an undirected hyperedge can establish connectivity but cannot enforce which side executes first, and
  combining all possible binding suppliers into one mask incorrectly turns alternatives into conjunctive producers.
  Date/Author: 2026-07-16 / Codex.

- Decision: binding-supplier alternatives are typed producer clauses, not mandatory occurrence predecessors. Clauses
  are conjunctive; exact producer states inside one clause are disjunctive, and an initially bound mask may discharge
  a clause without scheduling any sibling. `requiredOccurrenceIds` is reserved for unconditional scope/order hazards.
  Rationale: flattening A-or-B suppliers into one predecessor set either rejects a legal region or requires A and B;
  treating a singleton supplier as unconditional also overconstrains parameterized evaluation when the binding is
  already present outside the region.
  Date/Author: 2026-07-17 / Codex.

- Decision: `CascadesRewriteSupport.mask` delegates planner-name filtering directly to
  `BindingUniverse.maskOf(Collection)` rather than first copying a filtered `Set<String>`.
  Rationale: `BindingUniverse.intern` already rejects null, blank, and `_const_` names. Delegating once preserves the
  mask contract while avoiding a `HashSet` plus immutable Set copy at every bridge call.
  Date/Author: 2026-07-17 / Codex.

- Decision: immutable IR import represents EXISTS referenced bindings once as a query-local `BindingMask`; correlated
  bindings are the direct intersection of that reference mask with the inherited visible mask.
  Rationale: both masks share the same `BindingUniverse`, so converting through planner-name sets adds allocation and
  no semantic information. Direct intersection also preserves arbitrary-size masks without a 64-binding boundary.
  Date/Author: 2026-07-17 / Codex.

- Decision: scoped physical duplicate suppression uses primitive, open-addressed join and predicate tables keyed by
  group IDs, candidate IDs, predicate ordinals, and one query-local ordinal per stable implementation identity.
  Candidate release only clears a liveness bit; lookups remove stale entries lazily, and insertion compacts stale
  entries at the existing capacity before growing. Evaluation is still charged before its key is recorded.
  Rationale: this removes one record object, boxed hashing, and retained implementation string per evaluated
  combination while preserving deterministic no-overshoot accounting and avoiding an O(table size) release path.
  Date/Author: 2026-07-17 / Codex.

- Decision: `branchLocalBindOrValuesMask` owns a mask-native visitor that interns Extension and VALUES output names
  directly into a single mutable query-local bitmap. It keeps the same no-descent rules for subquery Projection and
  variable-scope-changing Union as `branchLocalBindOrValuesNames`; the latter remains available only for diagnostic
  and compatibility callers.
  Rationale: routing a mask consumer through the name-returning method necessarily materializes two transient string
  sets. Keeping the cold name boundary explicit avoids reintroducing those allocations into scoped rewrite matching.
  Date/Author: 2026-07-17 / Codex.

- Decision: `DefaultCascadesCostModel` treats bound-variable sets obtained from `OptimizationGoal`,
  `InputBindingContext`, `PhysicalProperties`, and an existing `BoundVarsKey` as canonical immutable values. It caches
  the resolved required/contextual set per goal identity and derives its mask through
  `BindingUniverse.maskOfStableSet`; arbitrary mutable sets are not accepted at this private boundary.
  Rationale: ownership is established before the cost loop, so another `Set.copyOf` adds allocation but no safety.
  Stable query-local identities permit mask reuse without introducing a global cache or changing cost semantics.
  Date/Author: 2026-07-17 / Codex.

- Decision: candidate delivered-property restriction is keyed by query-local `BindingMask`; string names remain a
  compatibility boundary. `BindingUniverse` may cache `(immutable property-set identity, mask)` and `(immutable
  BindingProfile identity, mask)` normalization results for its own lifetime, but it does not cache a parent
  `TupleExpr` identity or any mutable collection. `BoundVarsKey` retains only the mask and reconstructs names lazily
  when a legacy factor/statistics provider requires them.
  Rationale: rule applications may clone equivalent parents, so a parent-identity cache would optimize the regression
  fixture rather than the semantic boundary. Physical-property and profile records already canonicalize their
  collection components, making the query-local identity keys stable while avoiding a global mask-to-name cache.
  Date/Author: 2026-07-17 / Codex.

- Decision: the first typed non-inner contributor owns only an assured, atomic root `Difference`. It represents MINUS
  as a directed `ANTI` edge with the written left/right roles and exports the same transparent `Difference` through a
  registered physical implementation. A declined optional contributor reports `NOT_APPLICABLE`; discovery that
  cannot establish a join-owned root reports non-owning `UNSUPPORTED_ROOT`. Only an established semantic barrier or
  cycle is completeness-blocking.
  Rationale: specialized discovery must add a legal alternative without suppressing generic Difference optimization,
  creating a sticky incomplete result from a transient missing expression, commuting MINUS, or changing its bag,
  unbound-variable, or expression-error semantics. Nested and outer forms remain opaque on the legacy route.
  Date/Author: 2026-07-17 / Codex.

- Decision: the first typed OPTIONAL contributor owns only an unconditioned, non-scope-changing root `LeftJoin` whose
  two ordered inputs are exact atomic transparent memo children. It represents that operator as one directed `LEFT`
  edge, requires the written left input before the right, and exports a registered transparent physical `LeftJoin`.
  Conditioned or composite-child forms report explicit non-owning boundaries; they do not make completeness sticky,
  commute the inputs, or enter this narrow route.
  Rationale: a specialized LEFT implementation may add a legal costed alternative only after the scoped memo can
  preserve RDF4J OPTIONAL bag semantics and the written outer/inner roles. Keeping the first proof surface narrow
  avoids turning partial non-inner support into an ownership guard or query-specific rewrite while leaving generic
  rules and the legacy opaque route available for every unsupported form.
  Date/Author: 2026-07-17 / Codex.

- Decision: the first typed scalar-edge contributor owns only a non-scope-changing root Filter whose authoritative
  `LogicalOperatorKey.ConditionAttributes` value is exactly one `ScalarExists`, with ordered executable ARGUMENT and
  SCALAR_SUBQUERY inputs and two atomic transparent tuple bodies. A non-empty typed correlation mask fully assured by
  the outer group produces an EXISTS-origin `SEMI` edge or NOT_EXISTS-origin `ANTI` edge. Edge origin is part of
  region and recipe identity, and export reconstructs the original Filter operator by origin rather than treating
  every ANTI edge as MINUS.
  Rationale: polarity and correlation must come from immutable typed IR, while independently optimized existence-
  scoped RHS plans and direct registered Filter estimates must remain available to the shared cost model. The narrow
  eligibility seam adds a generic proof-backed alternative without claiming residual, nullable, or composite scalar
  forms and without blocking the generic rules or legacy route.
  Date/Author: 2026-07-17 / Codex.

- Decision: adjacent unconditioned OPTIONAL branches contribute multiple typed LEFT alternatives only when one
  immutable bitmap proof establishes a common assured base, base-assured hidden inputs, pairwise-disjoint introduced
  bindings, and safe barrier-free structure. Extraction creates one directed LEFT edge from the base to each branch;
  the legacy LMDB stable-order normalizer consumes the same proof.
  Rationale: this permits every proven legal branch order while excluding reverse, branch-only, cross-consuming,
  conditioned, nullable-base, and barrier cases without a planner-specific ownership gate or a second legality model.
  Date/Author: 2026-07-17 / Codex.

- Decision: ordinary external-Filter required inputs are resolved per written occurrence, not as an unordered region
  property. A unique assured earlier producer is an effective predecessor; a unique assured later producer must stay
  after the Filter and does not enter that occurrence's effective input mask. Zero or multiple assured producers still
  fail closed.
  Rationale: later sibling bindings do not exist during Filter EBV evaluation. Initial outer bindings remain available
  independently through `JoinSearchContext.initiallyBoundBindings`, so excluding a later sibling does not make an
  actually parameterized Filter unexecutable.
  Date/Author: 2026-07-17 / Codex.

- Decision: retain the legacy connected-hypergraph implementation as an additive compatibility candidate only when
  the exact logical source is admissible and canonical extraction reports a `NON_JOIN_COMPOSITE` boundary. Do not
  apply it to pure-inner regions, do not withdraw it merely because another rewrite later becomes supported, and do
  not give it a dominance or winner exception. A rewrite that would turn a valued nonconstant StatementPattern
  variable into an unbound projection must fail closed; only `isConstant()` terms are rewrite constants.
  Rationale: migration coverage is a property of the source semantic boundary, while winner selection belongs to the
  unified cost contract. RDF4J evaluation binds valued nonconstant variables, so `hasValue()` alone is insufficient
  evidence for constant folding. Registration preserves a legal fallback candidate but does not prove canonical
  extraction complete or permit deletion of the legacy route.
  Date/Author: 2026-07-17 / Codex.

- Decision: represent proof-bearing logical derivations separately while using a proof-free `MemoLogicalRouteKey` for
  topology discovery, unsupported-route ownership, and extraction ambiguity checks. A newly added logical derivation
  remains a topology wake-up; a proof-only physical metadata merge does not wake join discovery.
  Rationale: consumers that read proofs must see every derivation and certificate, but join topology and typed operator
  identity must not be multiplied or re-enumerated solely because an exact physical route gained another certificate.
  Date/Author: 2026-07-17 / Codex.

- Decision: exact physical structural duplicates merge the normalized union of route-local proofs independent of rule
  registration order. Logical rewrites keep proof-specific derivations and never union their certificates globally.
  Rationale: identical executable routes may be certified by several local implementation rules, whereas merging
  proofs across distinct logical derivations can authorize a rewrite that no single derivation proved. Recursive
  whole-subtree rules remain a migration blocker for the final provenance cutover.
  Date/Author: 2026-07-17 / Codex.

- Decision: primitive DPhyp returns an explicit `UNSUPPORTED` outcome only from global memo-expression enumeration,
  with detail that scoped partition delivery is required and generic contributors own the global route. Keep
  `contribute()`, `contributeExpandedTrees`, and LMDB's scalar compatibility tests intact during this cut.
  Rationale: the production invariant is that DPhyp contributes primitive topology into `JoinRegionMemo`, never a
  privately costed winner. Blocking only the inherited memo adapter enforces that invariant immediately while generic
  exhaustive enumeration preserves complete coverage and later migration can retire scalar compatibility APIs in a
  separately gated slice.
  Date/Author: 2026-07-17 / Codex.

- Decision: use one four-state scalar evaluation-effect classifier across generic and LMDB planning. Reordering and
  evaluation-count-changing rewrites require `REPEATABLE` or `QUERY_STABLE`; `VOLATILE` and `UNKNOWN` create explicit
  placement restrictions. Native EXISTS may inherit the effects of an audited structural tuple subtree, but SERVICE,
  tuple-function, foreign, and unknown tuple boundaries never receive a positive repeatability proof.
  Rationale: binding dependency alone does not preserve UUID, RAND, STRUUID, or BNODE evaluation multiplicity, while
  blanket scalar-subquery rejection suppresses safe standard EXISTS rewrites. A shared positive-proof contract keeps
  specialized rewrites composable without giving any planner route a weaker determinism policy.
  Date/Author: 2026-07-18 / Codex.

- Decision: classify deterministic Extension inputs occurrence-sensitively. A required name with no possible sibling
  producer is optional external evaluation context and is excluded from `RequiredOuterInputs`; an assured sibling
  producer contributes a hard mask and an exact producer clause; a merely possible sibling producer remains an
  explicit `NON_JOIN_COMPOSITE` boundary. For mixed inputs, only the assured-produced names enter the hard mask.
  Assured names supplied by `OptimizationGoal.inputBindingContext()` seed the region's initially bound mask. Volatile
  and unknown Extension expressions remain ordered through the shared `VOLATILE_SCALAR` placement restriction.
  Rationale: external bindings may be present or absent at evaluation time without creating a join dependency, while
  admitting a possible-but-unassured internal producer would make association determine whether the scalar sees a
  value. Exact disjunctive producer clauses preserve alternative suppliers without requiring all suppliers at once.
  Date/Author: 2026-07-18 / Codex.

- Decision: keep root input-consumption capability and whole-route selected-prefix safety as separate parent-visible
  winner facts. Derive the stronger fact once from the immutable selected DAG, include it in `WinnerStateKey` and its
  frontier context, and consult it in both global and scoped Join implementation admission. Compatibility winners may
  conservatively inspect their already materialized plan once; selected-DAG winners reuse cached child facts and memo
  inputs. Never materialize a selected plan during candidate admission merely to rediscover route safety.
  Rationale: physical-property delivery describes what the operator root can accept, while parameterized join
  execution must also know whether nested routing boundaries are legal. Conflating them either suppresses valid roots
  or admits illegal Join alternatives and also adds avoidable allocation to the hottest candidate loop.
  Date/Author: 2026-07-19 / Codex.

- Decision: make winner-frontier insertion return every displaced winner and treat displacement as targeted state
  invalidation before dependent parent costing may remain sealed.
  Rationale: addition-only change notification is incomplete when dominance or replacement removes a state selected
  by a retained parent DAG. Atomic displacement reporting preserves fixed-point reactivity and complete provenance
  without replaying unrelated frontier growth.
  Date/Author: 2026-07-19 / Codex.

- Decision: defer AUTO's uncharged original executable reservation until ordinary exploration quiesces or a hard
  limit requires the fallback, and globally cost-rank eligible scoped roots before applying AUTO's export cap. Use a
  primitive fixed-size top-K for bounded export and a full canonical sort only for unbounded exact export.
  Rationale: the reserved fallback must guarantee executability without becoming provisional optimizer knowledge that
  changes join discovery, while a per-group insertion-order cap can hide a later cheaper candidate even after a
  locally complete search. Global ranking preserves deterministic bounded memory and allows later candidates to
  evict earlier roots under the same `CostOrdering` used by the scoped frontiers.
  Date/Author: 2026-07-19 / Codex.

## Outcomes & Retrospective

The revised implementation has started from a clean compiling branch and now has an authoritative governing plan.
Milestone 1 has replaced string-based memo lookup with typed semantic identity and made Projection import/export
lossless for ordered mappings, context, subquery state, and explicit scope change. Diagnostic feedback fingerprints
remain available but no longer define equivalence. The legacy discrepancy recorder confirms the known Projection false
merge without participating in lookup. A separate typed route key preserves child implementation requirements and
barrier-clearing proofs without contaminating logical equivalence. Exact-class semantic registration prevents unknown
native subclasses from coalescing. Focused identity, IR, extraction, physical-input, and route tests pass; the complete
queryalgebra suite matches the clean `HEAD` baseline exactly. The prior q9 work demonstrates that discovery coalescing
can move the failure point but does not remove the global materialization slope; that evidence supports the later
scoped-memo milestone. Milestone 2 now provides one shared log-domain cardinality contract for the scoped memo and
legacy LMDB adapter. The required `.01/.9/.9` triangle is `0.81` for every association; `1e400` base rows cancel with
`1e-336` predicate evidence to approximately `1e64`; positive underflow and overflow saturate; and invalid evidence
causes DPhyp to decline after fallback rather than pricing it as neutral. Update this section after every milestone
with measured behavior, deviations, and remaining parity gates. The audit added explicit predicate kinds and closed
five adapter/export gaps; the final focused counts are shared model 6, legacy hypergraph costing 20, and LMDB DPhyp
planner 28, all with zero failures. The medical direct-planner baseline is closed: 30 extracted rows have stable
fingerprints, with 12 complete and 18 explicitly unsupported outcomes; query 9 independently fails to finish under
all three policies before emitting a winner. These pending-boundary and non-completion outcomes are now explicit
parity gates for the typed-property and scoped-memo milestones. The bitmap hardening slice removed the concrete
rewrite/hash allocation stacks reported from medical query 5 while retaining query-local IDs through scoped search.
The matching JDK 26 benchmark first improved from 95.569 to 67.018 ms/op and the later comparable run measured
64.186 ms/op (-32.838%); this is measured evidence, not an
inference from the allocation profile. The invocation-mass assertion exposed by broadening is now green after making
`selectedOutputRows` use the same context-total unit as its winner cost while retaining finite relation evidence as a
per-invocation fact. Repeated candidate costing now keeps output schemas as masks and caches the one compatibility
name set per expression; the >64-bit allocation contract and 368-test affected suite are green.
The follow-up schema audit leaves that outcome intact: the reported barrier route cannot construct a
`StreamBindingSchema`. The only measured residual is the detached scoped-cost compatibility boundary at 0.07% of
sampled allocation. It does not justify an isolated LMDB rewrite; a later profile-driven change should extend the typed
cost context with memo-owned possible and assured masks, while transitional legacy callers are removed or supplied
those masks during cutover.
The first milestone-5 routing slice now carries LMDB's immutable opaque descriptors through `JoinSearchService` into
the core extractor. Safe self-contained Extension factors participate without pinning while retaining transparent
children; unsafe shared outputs and unknown composites reject the whole region as `NON_JOIN_COMPOSITE`. Legacy
policy-free callers remain unchanged, and the other opaque/non-inner classes remain behind their parity gates.
The isolated budgeted OPTIONAL follow-up proved that its selected native winner and provenance were intact despite
trace rollover. Its test now reads those authoritative facts directly; all scheduler limits and production ordering
remain unchanged.
The EXISTS IR-import follow-up removes its remaining subquery-to-name-set-to-mask bridge. A 75-symbol characterization
retains all 72 visible correlations across multiple mask words and excludes the three subquery-local bindings; the
matching selector and all 19 IR round-trip tests remain green.
The cost-bound-variable follow-up removes the defensive immutable-Set copy from each `BoundVarsKey` construction.
Required/contextual name resolution now happens once per immutable goal and its query-local mask is reused; the
matching contextual refinement selector and all 93 cost-model tests remain green.
The provided-property follow-up makes that bitmap ownership authoritative while child properties are propagated and
restricted for physical candidates. Immutable fact-boundary normalization is query-local, provider names are lazy,
and the 193-symbol allocation regression is 78.2% below its recorded red at 13,371,304 bytes. The cost-model suite is
green at 94/94 and both typed property suites are green at 10/10 combined.
The first typed non-inner slice now carries a proven two-factor root MINUS through extraction, scoped costing, and
root-DAG export as one directed ANTI edge while preserving the original `Difference` operator and ordered transparent
children. Exact bag comparison covers duplicate left rows and an unbound left variable. Empty, merely possible, and
nested shared domains decline without claiming optimizer completeness, so generic rules and the legacy opaque route
remain authoritative outside the narrow proof-backed case. The affected core selection is green at 234/234 and the
focused LMDB MINUS compatibility selection is green at 7/7.
The first typed LEFT slice now carries an atomic, unconditioned root OPTIONAL through the same scoped lifecycle as a
strictly directed edge and reconstructs the original transparent `LeftJoin`. A bag-semantics regression covers two
matching RHS duplicates plus an unmatched LHS row whose RHS binding remains unbound; a separate empty-RHS regression
proves the selected winner retains the registered LeftJoin estimate of two outer rows rather than an inner-product
zero. Reverse eligibility is false by contract. Conditioned and composite inputs remain explicit non-owning legacy
boundaries. The affected core selection is green at 238/238 and the broad LMDB OPTIONAL/optimizer selection is green
at 132/132.
The first typed scalar-edge slice now carries exact root EXISTS and NOT EXISTS Filters through the scoped memo as
directed SEMI and ANTI edges without conflating NOT EXISTS with MINUS. The edge origin survives identity, physical
recipe validation, proofs, and transparent export; the RHS winner retains its authoritative existence scope. Bag
regressions prove duplicate outer rows survive EXISTS, RHS bindings never leak, and NOT EXISTS preserves an unbound
outer variable. Unsupported disjoint, unassured, residual, and nested forms remain non-owning legacy boundaries. The
affected five-class core selection is green at 39/39.
The adjacent OPTIONAL slice generalizes typed LEFT search to a proven independent LEFT star. Both branch orders are
costed and exported as transparent nested `LeftJoin` nodes; exact evaluation preserves duplicate rows and leaves
unmatched branch bindings unbound. Unsafe overlap, cross-consumption, conditions, nullable base bindings, barriers,
and hidden external inputs fail closed. Shared proof facts are mask-native and snapshot-immutable, and the affected
focused, core, and LMDB selections are green at 6/6, 45/45, and 95/95.
The ordinary external-Filter parity slice now preserves the EBV-visible written boundary while still retaining the
Filter's optimizable memo child. Earlier and later assured suppliers produce distinct dependency identities and
effective input masks; opposite same-memo fragments no longer coalesce before analysis. Missing/ambiguous inputs and
all scalar EXISTS barriers retain their prior fail-closed behavior. Focused semantic and fragment selectors pass 1/1,
the LMDB opaque class passes 36/36, and the core extractor class passes 15/15.
The safe-Group parity slice separates an atomic operator interior from outer-placement legality. Group retains its
independently optimizable child and aggregate semantics while its whole factor competes in scoped join search whenever
required-input and unsafe-shared-binding validation succeeds. Duplicate-producing shadow data proves bag equality,
one selected Group, COMPLETE/no-pending status, scoped provenance, and legacy-rule inapplicability. The audited opaque,
shadow, extractor, and exact-enumeration classes pass 37/37, 4/4, 16/16, and 15/15.
The safe-UNION parity slice applies the same internal-versus-outer distinction only after a finite recursive proof of
closed graph-pattern branches. Scope changes, external scalar inputs, branch-only sibling hazards, and unrecognized
composites remain fail-closed. Duplicate-producing shadow data proves bag equality, one selected Union, COMPLETE/no-
pending status, scoped provenance, and legacy-rule inapplicability; SERVICE now carries the legacy deletion gate. The
focused safety, opaque, shadow, and extractor selections pass 3/3, 40/40, 4/4, and 16/16. The concurrently modified
exact-enumeration class remains 13/15 until its typed-derivation owner restores two unrelated Filter/NOT EXISTS
completeness expectations.
The safe-LeftJoin parity slice extends that finite proof to an unconditioned, non-scope-changing OPTIONAL without
making its children part of the surrounding join region. Conditions, scope changes, composite children, required
inputs, and optional-only sibling use remain blocked. Scoped search moves a selective sibling ahead of the atomic
OPTIONAL under ordinary properties while preserving duplicate matched rows and unmatched unbound rows; exact
observation order retains the written topology. The focused safety/parity and complete opaque/shadow selections pass
4/4, 2/2, 43/43, and 6/6. Broad core exact verification is deferred behind the active typed-derivation repair.
The deterministic Extension external-input slice now distinguishes optional evaluation context from hard region
dependencies. External-only and assured-context cases enter scoped search; mixed expressions depend only on names
that an assured sibling can produce; possible-only sibling supply fails closed; and alternative assured suppliers
remain disjunctive. Volatile expressions reuse the shared ordered barrier rather than an extractor-specific allowlist.
The complete extractor and LMDB scoped/legacy parity classes pass 21/21 and 8/8. The legacy registry remains available
for other opaque and non-inner parity gates, but it no longer claims the possible-only Extension case on either route.

## Context and Orientation

The generic optimizer lives in `core/queryalgebra/evaluation`. A Cascades memo groups logically equivalent
expressions; a group can have multiple logical rewrites and physical implementations. `Memo.java` imports an RDF4J
`TupleExpr` tree, stores `MemoExpr` alternatives, records reverse child dependencies, and emits revisioned changes.
`CascadesPlanner.java` drains rule, costing, and join-discovery work and extracts one selected winner. The immutable
translation layer is under the sibling `cascades/ir` package. Here, “IR” means the local immutable vocabulary used to
describe tuple and scalar operators without relying on the mutable execution tree.

Join-specific core code lives in
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/join`.
`MemoJoinRegionExtractor` identifies factors and predicates. `JoinSearchService` invokes contributors.
`ExhaustiveLegalJoinSearchContributor` currently emits generic subset partitions. `JoinStateEnumerator` closes those
partitions over predicate placements. The target `JoinRegionMemo` is a scoped dynamic-programming table: it exists only
while optimizing one maximal reorderable region, groups candidates by factor subset and predicate state, and exports
only recipes reachable from selected full-region roots.

The LMDB adopter lives in `core/sail/lmdb`. `LmdbCascadesOptimizer` parses the public mode and scheduler properties,
builds the generic planner with LMDB rules, applies or shadows the result, and preserves the standard route when needed.
`LmdbHypergraphJoinPlanner` and classes under `core/sail/lmdb/.../hypergraph` contain the current long-mask DPhyp and
legacy costing implementation. `LmdbJoinIslandConnectivity` classifies opaque factors and unsafe reorderings.
`LmdbCascadesExplainFinalizer` decorates Explain output. These legacy classes remain until the core policy and typed
non-inner model prove parity.

The current public property names are compatibility contracts and must not be renamed:

- `rdf4j.optimizer.lmdb.cascades.mode`
- `rdf4j.optimizer.lmdb.cascades.budget`
- `rdf4j.optimizer.lmdb.cascades.timeoutMillis`
- `rdf4j.optimizer.lmdb.cascades.trace`
- `rdf4j.optimizer.lmdb.cascades.traceLimit`
- `rdf4j.optimizer.lmdb.cascades.standardPlanPolicy`
- `rdf4j.optimizer.lmdb.cascades.standardPlanFullAnnotations`
- `rdf4j.optimizer.lmdb.cascades.connectedJoin.trace`
- `rdf4j.optimizer.lmdb.cascades.connectedJoin.traceLimit`
- `rdf4j.optimizer.lmdb.cascades.connectedJoin.dphyp`
- `rdf4j.optimizer.lmdb.cascades.connectedJoin.dphyp.maxFactors`
- `rdf4j.optimizer.lmdb.cascades.connectedJoin.dphyp.pairBudget`
- `rdf4j.optimizer.lmdb.cascades.opaqueFactors`
- `rdf4j.optimizer.lmdb.cascades.explainFallbackAnnotations`
- `rdf4j.optimizer.cascades.costTrace`

Their current defaults and parsing are part of the characterization. Mode defaults to `auto`; blank, `false`, and
`off` mean OFF; `true` aliases EXACT; unknown values currently mean OFF. Budget defaults to 4,096 and is clamped to at
least one, but currently affects only BUDGETED and SHADOW_BUDGETED. Timeout defaults to 500 ms, a non-positive value
disables it, and it currently affects only those budgeted modes. Trace defaults false and trace limit defaults 512.
Standard-plan policy defaults to `fallback`; full annotations default false but are also enabled by trace or result-size
tracking. Connected-join trace defaults false with a 24,000-character limit clamped to at least 1,024. DPhyp defaults
enabled, `maxFactors` defaults 14 and clamps to 2 through 20, and pair budget defaults 100,000 with zero permitted.
Opaque factors default enabled. Explain fallback annotations and core cost trace default disabled.

Core `OptimizationGoal.SearchMode` currently contains only EXACT, BUDGETED, and SHADOW. The LMDB facade separately
defines AUTO and SHADOW_BUDGETED. Serializable observation mode converts AUTO/BUDGETED to SHADOW_BUDGETED and EXACT
to SHADOW. LMDB starts every optimization with exact cost policy; ROBUST and FEEDBACK_AWARE characterization therefore
uses the direct core API until a typed configuration seam exists.

The workspace is intentionally JDK 25+ compatible; JDK 26 is used for final JMH and JFR evidence. Maven tests use the
named workspace `optimizer-hardening`. Its private repository and full-GAV build roots live below
`.mvnf/workspaces/optimizer-hardening/`, and its first failing report is copied into
`initial-evidence.optimizer-hardening.txt` before a later run can overwrite it.

## Plan of Work

Milestone 0 establishes characterization without changing behavior. Finish the exact property inventory above and add
tests or static assertions that prevent accidental property renames. Capture the current medical-theme winner and
canonical cost under EXACT, ROBUST, and FEEDBACK_AWARE. Prove by code search and a focused characterization that
production memo lookup does not depend on record-generated `MemoExpr.equals/hashCode`. Record theoretical and observed
CSG-CMP pair counts for chain, star, cycle, and clique shapes at tractable sizes; use the formula
`(3^16 + 1) / 2 - 2^16 = 21,457,825` for the 16-clique rather than enumerating it in a unit test. The milestone is
complete when this document can identify the baseline winner, property defaults, pair counts, and every current
semantic lookup path.

Milestone 1 makes logical identity typed and lossless. First add failing tests for ordered Projection source-to-target
mapping, subquery state, `projectionContext`, explicit variable-scope change, clone deduplication, and two unsupported
native occurrences that must not coalesce. Add `LogicalOperatorKey` for execution domain, typed IR operator, all
evaluation-affecting local attributes, and explicit scope-change state. Add `MemoLogicalExpressionKey` for that operator
plus ordered child role, ordinal, group ID, use, and semantic scope. Unsupported/native nodes receive memo-local opaque
occurrence IDs; identical supported typed clones still deduplicate. Extend Projection IR as required and centralize
encoding in one semantic codec used by import, rule output, and join extraction. Replace `Map<String, Integer>` lookup
with typed keys in one compiling cut. Keep only a bounded test/debug discrepancy recorder between legacy strings and
typed keys. Rename the diagnostic `logicalFingerprint` concept to feedback fingerprint and remove it from every
equivalence path. Fail join extraction closed if typed equivalence cannot be established. Keep the `MemoExpr` record
shape temporarily; conversion to an immutable identity class and removal of compatibility constructors occurs only
after cutover.

Milestone 2 makes cardinality canonical and numerically stable. Add `JoinCardinalityModel` and a log-domain
selectivity value. For `(subset, appliedPredicates)`, sum the logarithms of factor cardinalities and the canonical log
selectivity of predicates, then exponentiate once. Exact zero remains zero, positive underflow becomes
`Double.MIN_VALUE`, and overflow becomes `Double.MAX_VALUE`. Ordinary and hyperpredicates contribute once. Same-label
binary equalities preserve the conservative maximum-spanning-forest policy, summing selected log weights. Remove
`newlySatisfiedSelectivity` and every tree-local reconstruction of union rows as
`leftRows * rightRows * incrementalRatio`. Directional ratios subtract logs, and bidirectional geometric mean is
`exp((logForward + logBackward) / 2)` with explicit zero and invalid-input rules. Route both scoped and legacy LMDB
costing through this model until the latter is retired. Focused tests must include the eight-node `1e50`/`1e-12`
clique near `1e64`, the large same-label forest, the `.01/.9/.9` triangle at `.81` independent of association, and a
representable geometric mean whose ordinary product would underflow.

Milestone 3 separates semantic properties, executable resources, cardinality evidence, and risk. Introduce
`RequiredPhysicalProperties`, `ProvidedPhysicalProperties`, `PhysicalOperatorId`, `ExecutionCost`,
`CardinalityEstimate`, `EstimateRisk`, and `CostOrdering`. Required ordering uses ordered `SortKey` values over a scalar
expression or binding symbol plus direction. Parameterization and distinctness use query-local binding masks. Access
path identity belongs only to `PhysicalOperatorId`; graph context belongs to logical operator attributes. Required
property intersection returns a compatible requirement or an explicit conflict. Exact and bounded policies rank and
dominate only `ExecutionCost`. ROBUST transforms it once with risk. FEEDBACK_AWARE performs the same one-time transform
only after its confidence threshold. Remove context-free `CostVector.objectiveScore` behavior that implicitly applies
risk. Keep evidence for refinement and telemetry without splitting exact memo state, and break true frontier ties by
stable physical identity. Harden the registered Distinct and Materialize enforcers so their child goal clears only the
property they supply. Add a goal-driven Sort enforcer with explicit child requirements and one concrete local cost;
remove generic surcharges that would double-charge it. Tests must show both sides of the natural-order versus Sort
choice, compatible prefixes, composition with distinct/materialized goals, property conflicts, exact dominance, and
that robust adjustment is never applied twice.

Milestone 4 introduces deterministic `AUTO` and a pure-inner scoped `JoinRegionMemo`. Add search results with one
primary completeness value, an orthogonal set of limit causes, exact counter snapshots, and retention high-water
marks. Limit causes are rule work, partition probes, candidate evaluations, predicate probes, retained state,
frontier truncation, deadline, and unsupported representation. Stop before an operation would exceed a limit; counters
never overshoot. `AUTO` uses 4,096 Cascades tasks, 100,000 DPhyp pair probes, 100,000 generic partition probes,
1,000,000 candidate evaluations, 1,000,000 predicate probes, 100,000 retained candidates, and 16 candidates per local
frontier, with no deadline. Explicit EXACT uses hard limits of 10,000,000 rule/partition operations, 50,000,000
candidate and predicate probes, and 1,000,000 retained candidates, with dominance-only local frontiers and no deadline.
`BUDGETED` and `SHADOW_BUDGETED` retain the existing task configuration and optional 500 ms default deadline. SHADOW
uses exact policy/hard limits and never applies. `timeoutMillis` never affects AUTO.

In this milestone add `JoinSubset`, using one `long` through 64 factors and compact words above 64, `PredicateState`,
`RequiredOuterInputs`, `JoinRegionGroupKey`, and `JoinRegionCandidate`. Each group owns property-aware frontiers.
Dominance requires identical predicate state, compatible outer inputs, no greater parameterization, properties that
satisfy the dominated candidate, and policy-ranked executable-resource dominance. DPhyp streams raw masks into the
table without allocating a `JoinPartition`; generic exhaustive enumeration feeds the same table and performs bounded
predicate closure there. A shared `JoinImplementationProvider` emits hash, nested-loop, bound-lookup, and backend
implementations. Base factors come from a revisioned snapshot of global memo winners. Cache identity includes those
revisions, statistics epoch, complete goal, dependency stamp, and resolved DPhyp configuration. A full AUTO frontier
may evict a retained candidate when a cheaper one arrives; truncation marks approximation but does not end search.
EXACT terminates incomplete if it cannot retain a nondominated state. Export at most 16 AUTO roots, trace their
reachable derivation DAGs, and materialize only that closure into the global memo.

Always reserve the original topology and one executable implementation outside exploratory limits. Add a deterministic
left-deep greedy seed that holds scheduled factors in a `BitSet`, connection scores and lower-bound incremental costs
in arrays, updates scores through incident-edge adjacency, prefers connected extensions, and uses stable occurrence ID
as the final tie break. Its required complexity is `O(n^2 + E)` time and `O(n + E)` memory. Reject more than 64
factors before constructing DPhyp's long-backed graph; AUTO uses original and greedy seeds, while generic exact may use
arbitrary-size `JoinSubset`. Preserve DPhyp properties and units: enabled by default, `maxFactors` default 14 clamped
to 20, and `pairBudget` default 100,000 callbacks. DPhyp reports exhaustive connected coverage only when both
eligibility and pair limits complete. In EXACT, a stopped DPhyp seed does not prevent generic exact enumeration.

Milestone 5 establishes opaque atomic-factor parity while leaving legacy production routing intact. Define the
LMDB-backed `OpaqueJoinFactorPolicy.classify(MemoExpr)` contract returning an optional descriptor with required input
bindings, unsafe shared bindings, semantic barriers, and relevant properties. Compare core extraction with the legacy
boundaries for LeftJoin, Difference, Union, Service, Group, Lateral, Filter/Extension with external dependencies, and
atomic EXISTS/NOT EXISTS forms. Require stable factor order and unsafe-name rejection. The milestone is complete only
when the canonical extractor matches the legacy factor and dependency surface for every matrix row.

Milestone 6 adds typed non-inner semantics. Define `JoinEdge` kinds INNER, LEFT, SEMI, and ANTI, plus `ConflictRule`
activation/required subsets and explicit total-eligibility sets. Represent alternative producers as exact disjunctive
states instead of requiring every possible supplier. INNER is associative/commutative only inside a barrier-free
region. Adjacent unconditioned OPTIONAL branches may swap only with assured shared base bindings, disjoint introduced
bindings, and no cross-dependency or barrier. The existing typed null-rejection proof alone may turn OPTIONAL into
INNER. EXISTS/NOT EXISTS become directional SEMI/ANTI only with assured correlation. Independent deterministic
SEMI/ANTI filters may reorder over assured base bindings. MINUS becomes ANTI only with a proven non-empty assured
shared domain. Conditioned OPTIONAL, optional-introduced references, Union, Group, Service, and Lateral remain barriers
without a separate proof. Tests compare result bags, unbound variables, multiplicity, and expression-error behavior
against unoptimized evaluation for every permitted and prohibited rewrite.

Milestone 7 burns in and cuts over safely. Shadow scoped and legacy routes on representative LMDB and generated
regions, comparing result semantics, legal topology sets, required inputs, properties, winner fingerprints,
completeness, and exact counters. Add a registry/deletion-gate test that fails if a legacy route is removed while the
canonical extractor reports `NON_JOIN_COMPOSITE` for a region it owns. Switch pure-inner regions first and each opaque
or non-inner class only after its parity gate. When all classes pass, delete `CostingReceiver`, production
`PlanHypergraph`, the private winner adapter, raw global partition routing, and unbounded `JoinStateEnumerator`.
Convert `MemoExpr` to an immutable identity entity with accessor-compatible methods; retain only ID and typed key as
authoritative identity, exclude reconstruction templates from equality, and remove compatibility constructors and the
legacy discrepancy recorder.

Milestone 8 optimizes only measured hot paths and completes verification. Keep DPhyp masks and pair callbacks
allocation-free. Store candidate IDs, subsets, child IDs, and counters in primitive arrays; allocate objects only for
retained candidates and typed property boundaries. Replace `findConnectingEdge` all-edge scans with incident adjacency
and epoch-mark arrays. Add planning-only `CascadesJoinSearchBenchmark` cases for 4, 8, 11, 12, and 16 factors; chain,
star, cycle, clique, and disconnected shapes; zero, four, and eight predicates; and AUTO/EXACT modes. Measure probes,
evaluations, retained high-water, exported recipes, time, and allocation. Capture JMH first, then JFR or async-profiler
for meaningful regressions, and inspect method-scoped JIT evidence only if code shape is implicated. Close this plan
with measured outcomes and deviations.

## Concrete Steps

Run all commands from the repository root
`/Users/havardottestad/Documents/Programming/rdf4j-small-things`. The required initial sanity command is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
      | tee maven-build.log \
      | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

For each behavior-changing slice, first add only the smallest failing test. Run it in the isolated workspace and retain
the log:

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      --workspace optimizer-hardening \
      TestClass#testMethod --retain-logs

The runner prints the exact full-GAV Surefire report and run log. Immediately preserve the compact failing evidence:

    python3 scripts/agent-evidence.py \
      --command "python3 .codex/skills/mvnf/scripts/mvnf.py --workspace optimizer-hardening TestClass#testMethod --retain-logs" \
      --log .mvnf/workspaces/optimizer-hardening/logs/<run-id>/verify.log \
      .mvnf/workspaces/optimizer-hardening/build/org.eclipse.rdf4j/<artifact>/<version>/surefire-reports \
      > initial-evidence.optimizer-hardening.txt

Do not edit production code until that failure is recorded. Implement the root cause, rerun the identical selector,
then its test class and module. Never add `-am` or `-q` to a test command. Use `--retain-logs` for evidence-bearing runs.
If offline dependency resolution fails, rerun the exact runner once with `--no-offline`, then return offline.

Before formatting, run `scripts/checkCopyrightPresent.sh` from the `scripts` directory and add
`// Some portions generated by Codex` immediately below the license header of every touched Java file that does not
already have it. Formatting is serialized outside workspace mode:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The repository permits `-q` for formatting but never for tests. Audit the final diff after formatting and keep changes
scoped to this implementation.

For the final planning benchmark, add a single JMH method at a time and use the repository wrapper:

    scripts/run-single-benchmark.sh \
      --module core/queryalgebra/evaluation \
      --class <fully-qualified-CascadesJoinSearchBenchmark> \
      --method <benchmark-method>

Use JDK 26 for final LMDB theme and JFR evidence. Record the exact JVM, benchmark parameters, git revision, plan/counter
output, and result file in this document before making performance claims.

## Validation and Acceptance

Milestone 1 passes when Projection mapping, context, subquery state, and explicit scope state survive import/export and
change typed identity; unknown native occurrences never merge; identical typed clones do merge; and no production
equivalence lookup uses a feedback fingerprint or record-generated `MemoExpr.equals/hashCode`.

Milestone 2 passes when canonical result cardinality is invariant across associations for an identical subset and
predicate state, every positive underflow remains positive, exact zero remains zero, overflow saturates, same-label
equality uses one maximum spanning forest, and all four required numerical regressions pass in both core and legacy
LMDB paths.

Milestone 3 passes when exact dominance ignores evidence, ROBUST can reverse one raw-cost ranking through exactly one
risk transform, FEEDBACK_AWARE applies its threshold once, property conflicts are explicit, and Sort/natural-order,
Distinct, and Materialize choices satisfy all relative-plan tests without double charging.

Milestone 4 passes when repeated AUTO runs under different machine load produce byte-for-byte identical winners and
counters; `timeoutMillis=1` has no effect on AUTO; no counter exceeds its limit; frontier truncation permits later
cheaper replacement; 65- and 128-factor AUTO regions never construct DPhyp's long graph and use the specified greedy
fallback; small unbounded scoped-DP winners equal an exhaustive oracle; and exported global recipes respect the
root-DAG bound. A 12-factor clique exceeds the default DPhyp/AUTO pair limit but still returns a legal AUTO plan. A
16-factor clique makes explicit EXACT report `RESOURCE_LIMIT_EXCEEDED` and leaves the input algebra unchanged.

Milestones 5 through 7 pass only with SPARQL bag-semantic parity. Opaque and non-inner tests compare results,
multiplicity, unbound variables, expression errors, required inputs, and legal topology sets. The deletion gate must
prevent removal of a still-owned legacy class. DPhyp enabled and disabled must produce equivalent results because
algorithm selection never changes legality or generic routing.

Final acceptance runs the focused Cascades suite, complete `core/queryalgebra/evaluation` verification, affected LMDB
tests, SPARQL 1.1 compliance, estimate-audit corpora, and the complete medical/theme regression corpus. The JDK 26
11-query by two-algorithm matrix must produce 22 rows, no failures, correct aggregates, and explicit completeness or
approximation causes. Exact 4- and 8-factor planning may regress no more than 10% median time or allocation. AUTO 12-
and 16-factor cases must stay inside every deterministic limit. Small-query runtime may regress no more than 5% and
planning time no more than 10%; wall-clock latency is benchmark evidence, not a flaky CI assertion. Any meaningful
regression requires a captured allocation/CPU profile before further tuning.

## Idempotence and Recovery

The Maven workspace is reusable and never deletes source or another workspace's artifacts. Repeating a selector
creates a new run log and replaces only that selected GAV's stale test reports. Preserve
`initial-evidence.optimizer-hardening.txt` before rerunning the initial failing selector. Do not delete workspace
repositories, build roots, logs, profiles, benchmark stores, or any untracked artifact.

All migrations are additive until their parity gate passes. If a typed key or scoped-memo slice breaks compilation,
repair that slice rather than switching back to a string lookup or hidden fallback. If explicit EXACT hits a hard
limit, return the structured incomplete result and retain the original algebra; do not raise a limit silently. If a
legacy deletion gate fails, restore no file destructively: leave the legacy route registered and complete the missing
extractor/policy support first.

The current branch may contain optimizer work from earlier milestones. Treat it as owned work, preserve it, and use
targeted diffs. Never use `git reset --hard`, `git clean`, `git restore`, or broad checkout commands. New commits should
be small, conventional, and must not include benchmark stores, workspace output, logs, or profiles.

## Artifacts and Notes

Initial build evidence:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
             -Dmaven.repo.local=.m2_repo -Pquick clean install
    Log: maven-build.log
    Result: BUILD SUCCESS; 32.594 s wall clock; 2026-07-16 07:37Z

Pair-count characterization evidence:

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py --workspace optimizer-hardening \
             SubgraphEnumeratorTest#referenceShapePairCountsMatchTheoryAtEightFactors \
             --retain-logs -- -Djapicmp.skip=true
    Log: .mvnf/workspaces/optimizer-hardening/logs/
         20260716T082947.593636Z-4097-d946328a/verify.log
    Report: .mvnf/workspaces/optimizer-hardening/build/org.eclipse.rdf4j/
            rdf4j-sail-lmdb/6.1.0-SNAPSHOT/surefire-reports/
            org.eclipse.rdf4j.sail.lmdb.hypergraph.SubgraphEnumeratorTest.txt
    Result: tests=1, failures=0, errors=0, skipped=0

Earlier q9 diagnostic artifacts retained for architectural evidence:

    /tmp/lmdb-medical-q9-exact-after-fragment-alloc/
    /tmp/lmdb-medical-q9-exact-after-fragment-correct/

First typed-identity red evidence:

    Evidence file: initial-evidence.optimizer-hardening.txt
    Command: python3 .codex/skills/mvnf/scripts/mvnf.py --workspace optimizer-hardening \
             CascadesMemoModelTest#projectionSourceMappingIsPartOfLogicalIdentity \
             --retain-logs -- -Djapicmp.skip=true
    Log: .mvnf/workspaces/optimizer-hardening/logs/
         20260716T083326.349379Z-32667-6f454105/verify.log
    Result: tests=1, failures=1, errors=0, skipped=0; both projections returned group 1.

DPhyp mask-native topology evidence:

    Evidence file: initial-evidence.optimizer-dphyp-masks.txt
    Legacy-universe red log: .mvnf/workspaces/optimizer-dphyp-masks/logs/
         20260717T010921.331682Z-33587-6cfb76d5/verify.log
    Post-change context class log: .mvnf/workspaces/optimizer-dphyp-masks/logs/
         20260717T012209.047966Z-16482-a52aa0e5/verify.log
    Post-change LMDB planner class log: .mvnf/workspaces/optimizer-dphyp-masks/logs/
         20260717T012343.832712Z-24751-9800eb08/verify.log
    Pair-count audit log: .mvnf/workspaces/optimizer-dphyp-masks/logs/
         20260717T012448.008143Z-31610-a5fa9bea/verify.log
    Result: context 3/3, planner 28/28, and triangle pair audit 1/1 with exactly six streamed pairs.

DPhyp incident-adjacency evidence:

    Evidence file: initial-evidence.optimizer-dphyp-adjacency.txt
    Pre-change class log: .mvnf/workspaces/optimizer-dphyp-adjacency/logs/
         20260717T014749.858115Z-71499-75b6369d/verify.log
    Pre-change exact-order log: .mvnf/workspaces/optimizer-dphyp-adjacency/logs/
         20260717T015002.863775Z-88240-d919cb7e/verify.log
    Identical post-change exact-order log: .mvnf/workspaces/optimizer-dphyp-adjacency/logs/
         20260717T015347.053447Z-5814-2d23208b/verify.log
    Post-change DPhyp cross-section log: .mvnf/workspaces/optimizer-dphyp-adjacency/logs/
         20260717T015613.464683Z-16897-5b3d7174/verify.log
    Post-change legacy-consumer log: .mvnf/workspaces/optimizer-dphyp-adjacency/logs/
         20260717T015734.195598Z-23606-4d9f6aa7/verify.log
    Result: exact callback order 1/1, DPhyp cross-section 60/60, and legacy consumers 28/28 passed. Structural audit
            confirms one reusable primitive lookup state and no allocation, collection, boxing, stream, or full
            edge-list scan in the per-pair lookup; no timing or allocation-rate claim is inferred from code shape.

Planning-only scoped-search benchmark evidence:

    Evidence file: initial-evidence.optimizer-join-benchmark.txt
    Fixture log: .mvnf/workspaces/optimizer-join-benchmark/logs/
         20260717T014001.687122Z-7673-6f229f18/verify.log
    Benchmark-profile package log: .mvnf/workspaces/optimizer-join-benchmark/logs/
         20260717T014114.497985Z-14736-40fcb735/maven.log
    Benchmark jar: .mvnf/workspaces/optimizer-join-benchmark/build/org.eclipse.rdf4j/
         rdf4j-queryalgebra-evaluation/6.1.0-SNAPSHOT/jmh-benchmarks.jar
    Result: fixture 2/2; JMH listing contains CascadesJoinSearchBenchmark.plan; 4-factor CHAIN/AUTO smoke is complete
            and emits all search, retention, memo, and exported-recipe auxiliary columns. The JDK 25 non-forked
            one-iteration score is a harness diagnostic only, not a performance result.

First typed ANTI-edge evidence:

    Evidence file: initial-evidence.optimizer-anti.txt
    Initial focused red log: .mvnf/workspaces/optimizer-anti/logs/
         20260717T005343.264848Z-41567-3f8bffd6/verify.log
    Identical focused green log: .mvnf/workspaces/optimizer-anti/logs/
         20260717T010123.597615Z-90724-df3327e1/verify.log
    Late-alternative ownership log: .mvnf/workspaces/optimizer-anti/logs/
         20260717T011744.093187Z-73685-3a17ff5f/verify.log
    Broad core log: .mvnf/workspaces/optimizer-anti/logs/
         20260717T011853.976664Z-79194-2f67995a/verify.log
    Focused LMDB MINUS log: .mvnf/workspaces/optimizer-anti/logs/
         20260717T012115.416563Z-97081-72a4dd5c/verify.log
    Result: focused red 1/1 failed at NOT_LOGICAL_JOIN; identical green 1/1; ownership 2/2, core 234/234,
            and LMDB MINUS 7/7 passed. The LMDB verify skips only the unrelated current japicmp incompatibility.

First typed LEFT-edge evidence:

    Evidence file: initial-evidence.optimizer-left.txt
    Initial focused red log: .mvnf/workspaces/optimizer-left/logs/
         20260717T012755.186612Z-46009-d89ee780/verify.log
    Identical focused green log: .mvnf/workspaces/optimizer-left/logs/
         20260717T014118.765207Z-15022-02c731a7/verify.log
    Full typed-semantics class log: .mvnf/workspaces/optimizer-left/logs/
         20260717T014236.016743Z-29777-8c2a684c/verify.log
    Broad core log: .mvnf/workspaces/optimizer-left/logs/
         20260717T014354.581820Z-39926-4501958c/verify.log
    Broad LMDB OPTIONAL log: .mvnf/workspaces/optimizer-left/logs/
         20260717T014514.851615Z-47341-b4430da7/verify.log
    Result: focused red 1/1 failed at NOT_LOGICAL_JOIN; identical green 1/1; typed semantics 7/7, core 238/238,
            and LMDB OPTIONAL/optimizer 132/132 passed. The LMDB verify skips only the unrelated current japicmp
            incompatibility.

First typed root EXISTS/NOT EXISTS edge evidence:

    Evidence file: initial-evidence.optimizer-semi-exists.txt
    Initial focused red log: .mvnf/workspaces/optimizer-semi-exists/logs/
         20260717T015641.298198Z-18941-4a16b993/verify.log
    Identical focused green log: .mvnf/workspaces/optimizer-semi-exists/logs/
         20260717T021143.236856Z-93850-4c826442/verify.log
    Full typed-semantics class log: .mvnf/workspaces/optimizer-semi-exists/logs/
         20260717T021404.023612Z-5543-2b16fec3/verify.log
    Five-class core log: .mvnf/workspaces/optimizer-semi-exists/logs/
         20260717T021542.560983Z-12639-e9f79302/verify.log
    Result: focused red 2/2 failed at NOT_LOGICAL_JOIN; identical green 2/2, typed semantics 12/12, and the
            extractor/edge/provider/partition core cross-section 39/39 passed.

Independent adjacent OPTIONAL evidence:

    Evidence file: initial-evidence.optimizer-optional-swap.txt
    Initial extraction red log: .mvnf/workspaces/optimizer-optional-swap/logs/
         20260717T021345.670525Z-1775-1b5804c6/verify.log
    Immutable-facts red/green logs: .mvnf/workspaces/optimizer-optional-swap/logs/
         20260717T022620.507060Z-76259-9b0096e5/verify.log
         20260717T022814.770969Z-88962-cda6a672/verify.log
    Focused class green log: .mvnf/workspaces/optimizer-optional-swap/logs/
         20260717T022420.587428Z-62959-8babd201/verify.log
    Core cross-section green log: .mvnf/workspaces/optimizer-optional-swap/logs/
         20260717T022947.582268Z-95246-c3984e4a/maven.log
    LMDB green log: .mvnf/workspaces/optimizer-optional-swap/logs/
         20260717T023003.376862Z-98440-5d434c33/maven.log
    Result: focused class 6/6, core edge/extractor/provider/enumeration/non-inner selection 45/45, and LMDB
            optional/opaque/optimizer selection 95/95 passed. The LMDB verify skips only japicmp.

Ordinary external-Filter occurrence-order evidence:

    Evidence file: initial-evidence.optimizer-external-filter.txt
    Initial focused red log: .mvnf/workspaces/optimizer-external-filter/logs/
         20260717T041518.484738Z-26978-fcbc7ef3/verify.log
    Identical semantic/EBV green log: .mvnf/workspaces/optimizer-external-filter/logs/
         20260717T043101.074868Z-84364-c2a30126/verify.log
    Fragment-identity green log: .mvnf/workspaces/optimizer-external-filter/logs/
         20260717T042835.923128Z-74088-8cf85c11/verify.log
    Full LMDB opaque class log: .mvnf/workspaces/optimizer-external-filter/logs/
         20260717T043204.207995Z-88735-4182177d/verify.log
    Core extractor class log: .mvnf/workspaces/optimizer-external-filter/logs/
         20260717T043300.679245Z-92552-a61db9ab/verify.log
    Result: initial 1/1 failed on all three later-producer direction/effective-input assertions; identical semantic
            selector 1/1, fragment selector 1/1, LMDB opaque class 36/36, and core extractor class 15/15 passed.

Scoped/global cost-ledger characterization evidence:

    Existing broad red logs: .mvnf/workspaces/optimizer-valued-vars/logs/
         20260717T051134.244226Z-51242-7fed20d9/verify.log
         20260717T051341.161687Z-63057-386d63fb/maven.log
    Structurally classified exact green log: .mvnf/workspaces/cost-ledger-reactivation/logs/
         20260717T052907.551720Z-39618-aac26b46/verify.log
    Complete ledger green log: .mvnf/workspaces/cost-ledger-reactivation/logs/
         20260717T053141.468973Z-51847-8abee653/verify.log
    Dependency-revision green log: .mvnf/workspaces/cost-ledger-reactivation/logs/
         20260717T053256.117773Z-62689-3a1ece90/verify.log
    Result: the original broad run observed two undifferentiated estimator calls; stack characterization proved one
            scoped and one global call. The strengthened exact selector passes 1/1, the full ledger class 11/11, and
            rule/output/join dependency-revision classes 11/11. No production scheduler or search code changed.

Add concise red/green report snippets, pair-count characterizations, theme winner fingerprints, benchmark tables, and
profile summaries here as they are produced. Detailed Maven logs stay under
`.mvnf/workspaces/optimizer-hardening/logs/`; do not paste entire logs into this document.

## Interfaces and Dependencies

`LogicalOperatorKey` is an immutable core type containing execution domain, typed IR operator and all local semantic
attributes, explicit scope-change state, and an optional memo-local opaque occurrence ID. It excludes costs, estimates,
proofs, telemetry, feedback fingerprints, physical properties, rule IDs, and mutable execution nodes.

`MemoLogicalExpressionKey` contains one `LogicalOperatorKey` and ordered child keys. Each child key contains role,
ordinal, group ID, `MemoInput.Use`, and `SemanticScope`. It is the only logical-equivalence key accepted by `Memo`.

`RequiredPhysicalProperties` and `ProvidedPhysicalProperties` are distinct immutable types. Required intersection
returns a success or explicit conflict. `SortKey` is ordered and contains a binding symbol or scalar expression plus
direction. `PhysicalOperatorId` identifies an executable operator/access path. `ExecutionCost` contains startup, total
work, rescan, memory, seeks, and page walks. `CardinalityEstimate` contains output rows and exactness. `EstimateRisk`
contains q-error, uncertainty, confidence, and evidence provenance. `CostOrdering` is the only component allowed to
apply policy-specific risk transformation or compare candidates.

`JoinSubset` supports long and multi-word representations. `PredicateState` identifies exactly the applied predicate
set. `RequiredOuterInputs` is a query-local binding mask. The scoped state keys and candidates are:

    record JoinRegionGroupKey(
        JoinSubset subset,
        PredicateState appliedPredicates,
        RequiredOuterInputs requiredOuter) {}

    record JoinRegionCandidate(
        int id,
        JoinRegionGroupKey group,
        ProvidedPhysicalProperties provided,
        PhysicalOperatorId operator,
        ExecutionCost cost,
        CardinalityEstimate cardinality,
        int leftCandidateId,
        int rightCandidateId) {}

`JoinImplementationProvider` enumerates legal physical implementations for one left/right state pair into the scoped
memo. `JoinCardinalityModel` computes the canonical result cardinality from subset and predicate state. Neither a rule
nor an LMDB contributor composes a second whole-tree cost.

`SearchMode` contains AUTO, BUDGETED, EXACT, SHADOW, and SHADOW_BUDGETED. A search result contains primary
completeness, an `EnumSet` of limit causes, exact counter snapshots, retention high-water marks, optional selected plan,
and deferred tasks. The existing mode/property strings remain aliases into this model.

`OpaqueJoinFactorPolicy` is backend-provided and core-consumed:

    interface OpaqueJoinFactorPolicy {
        Optional<OpaqueFactorDescriptor> classify(MemoExpr expression);
    }

`JoinEdge` contains INNER, LEFT, SEMI, and ANTI. `ConflictRule` contains activation and required subsets. Unsupported
forms remain explicit opaque barriers with Explain reason codes rather than being guessed safe.

No stable public Java API changes and no new dependency are allowed. Existing Cascades types are experimental/internal
and may evolve only with corresponding tests and an update to this plan.

Plan revision note (2026-07-16 07:37Z, Codex): created the governing plan from the reviewed reference-grade design,
recorded all three superseded decisions, preserved the legacy route until semantic parity, and incorporated the current
q9 allocation/invariant evidence so the implementation attacks scoped materialization rather than adding a plan-shape
exception.

Plan revision note (2026-07-16 07:44Z, Codex): incorporated the read-only property, identity, and cardinality
inventories. The revision locks current aliases/defaults, records the Projection false merge and fail-open join
extraction, and names the nonuniform same-label triangle as the first canonical-cardinality red after typed identity.

Plan revision note (2026-07-16 08:30Z, Codex): recorded the green eight-factor shape-count characterization and the
exact workspace evidence path. The 12- and 16-clique values remain theoretical acceptance data and are deliberately
not enumerated by this unit test.

Plan revision note (2026-07-16 08:34Z, Codex): recorded the first Milestone 1 red and its preserved evidence file.
The failure proves the current feedback-fingerprint string is an authoritative false-merge path, while the same test
locks supported clone deduplication for the typed-key migration.

Plan revision note (2026-07-16 13:52Z, Codex): completed mode routing and transactional-application hardening. AUTO
now has deterministic fixed limits and no deadline, SHADOW follows exact search, SHADOW_BUDGETED follows timed
bounded search, and incomplete EXACT leaves the input algebra unchanged. Structured causes, counters, and retention
high-water marks are exposed through the core plan and LMDB annotations. A broad `CascadesRuleEngineTest` comparison
against `/tmp/rdf4j-typed-identity-baseline` proved three semantic failures are inherited from clean HEAD. Two new
BUDGETED cost-observation failures were isolated to eager fallback reservation and fixed by preserving the existing
deferred BUDGETED path while retaining reserved fallback behavior for AUTO and exact-like modes. The identical
two-method selector is green in workspace run `20260716T135108.705516Z-61564-f80ebef7`.

Plan revision note (2026-07-17 00:42Z, Codex): recorded the allocation-neutral mask-bridge slice, its matching
pre-change green selector, and the decision to rely on `BindingUniverse` as the single planner-name filtering owner.

Plan revision note (2026-07-17 00:44Z, Codex): recorded the budgeted OPTIONAL trace-rollover diagnosis, exact deadline
counter snapshot, stronger winner-provenance assertion, and unchanged production scheduler contract.

Plan revision note (2026-07-17 00:45Z, Codex): recorded the primitive scoped evaluation ledger, its candidate-liveness
contract, bounded implementation-ordinal ownership, preserved release red, and the 64-test expanded join-search
verification result.

Plan revision note (2026-07-17 00:45Z, Codex): closed the mask-bridge slice with matching 1/1 pre/post selectors and
the 172/172 rule-engine class result; no scoped search, scheduler, LMDB, or other allocation-audit slice changed.

Plan revision note (2026-07-17 00:53Z, Codex): recorded the allocation-neutral EXISTS import slice, its matching 1/1
pre/post selector, 19/19 IR round-trip class, and multiword semantic invariant. The change is confined to mask-native
subquery reference collection and visible-mask intersection; planner routing, scoped search, and LMDB remain
unchanged.

Plan revision note (2026-07-17 00:53Z, Codex): closed allocation-audit slice P0(9) with matching 2/2 pre/post scoped-
UNION selectors, the 172/172 rule-engine result, exact traversal-barrier preservation, and a separate cold
name-diagnostic path.

Plan revision note (2026-07-17 01:07Z, Codex): recorded the allocation-neutral cost-bound-variable slice, its
canonical-immutability proof, matching 1/1 contextual refinement selector, and 93/93 cost-model class. The change is
confined to `CascadesCostModel` goal-name and `BoundVarsKey` caching; scheduler, join search, IR import, and LMDB remain
unchanged.

Plan revision note (2026-07-17 01:20Z, Codex): recorded the mask-native LMDB DPhyp factor/topology slice. Legacy
Set-based contexts rebase once into their context universe, while typed production contexts retain zero-copy shared
masks. Pair enumeration, factor order, public property names/defaults, routing, legality, and LMDB cost formulas are
unchanged; only set algebra inside topology/dependency preparation and prefix binding accumulation became bitmap
algebra. The >64-ID red, matching descriptor pre/post checks, full classes, and six-pair triangle audit are preserved.

Plan revision note (2026-07-17 01:46Z, Codex): recorded the first typed LEFT-edge slice, its deliberately narrow
atomic/unconditioned ownership contract, direct registered LeftJoin estimate, preserved bag and unbound-RHS semantics,
non-owning conditioned/composite boundaries, red/green evidence, and 238-core/132-LMDB broad verification results.

Plan revision note (2026-07-17 02:22Z, Codex): recorded the additive LMDB scoped-versus-legacy shadow harness and
deletion gate. The cost-scope trace showed no production finite-VALUES defect: the fixture had already multiplied its
context estimate by outer-prefix rows but mislabeled the result per invocation. With the test model's scope corrected,
the scoped and legacy routes choose the same lower-work VALUES-first topology. Production routing remains unchanged;
opaque and non-inner parity gates still block legacy deletion.

Plan revision note (2026-07-17 02:31Z, Codex): recorded independent adjacent OPTIONAL extraction, shared mask-native
legality, directed LEFT-star routing, exact bag/unbound behavior, immutable fact snapshots, and the 6/45/95-test
verification results. Unsupported OPTIONAL forms remain non-owning legacy boundaries; no broad route was deleted.

Plan revision note (2026-07-17 04:34Z, Codex): recorded the ordinary external-Filter occurrence-order slice, its
per-symbol earlier/later producer contract, preserved zero/ambiguous fail-closed behavior, distinct same-memo fragment
identity, exact EBV counterexample, and 1/1 focused plus 36/36 LMDB and 15/15 core verification. Scalar EXISTS
barriers and broad opaque ownership remain unchanged.

Plan revision note (2026-07-17 05:22Z, Codex): recorded the exact-source canonical-gap compatibility gate, additive
cost competition, pure-inner exclusion, observable-valued UNION fail-closed rule, restored bound-lookup registration,
and 3/3 shadow plus 4/4 unified-routing verification. Canonical unsupported boundaries still prevent an end-to-end
EXACT completeness claim; compatibility registration is not a completeness repair.

Plan revision note (2026-07-17 05:34Z, Codex): recorded the scoped/global cost-ledger characterization correction.
Typed EXISTS search adds one legitimate detached scoped pricing domain; the global memo still seals exactly one
canonical dependent-parent application across its own fact publication. The test distinguishes those domains by
`ScopedMemoInputReference`, all debug instrumentation was removed, and the 1/11/11-test verification is preserved.

Plan revision note (2026-07-17 05:46Z, Codex): recorded additive typed/generic EXISTS routing and topology-local
unsupported-route identity. Specialized atomic extraction remains available but cannot suppress a broader legal
predicate-closure route; unsupported results merge dependencies only within an identical leaf topology. Focused red
and green logs plus the complete 16-test extractor verification remain preserved in workspace
`optimizer-valued-vars`.

Plan revision note (2026-07-17 06:14Z, Codex): recorded typed factor-internal versus outer-placement barrier facts,
safe Group scoped routing, duplicate-bag shadow parity, legacy compatibility-rule exclusion, the fixed-predicate test-
fixture audit, and the final 37/4/16/15-test verification results in workspace `optimizer-group-atomic`.

Plan revision note (2026-07-17 06:36Z, Codex): recorded the narrow recursive safe-UNION proof, full descriptor and
extractor safety gates, duplicate-bag scoped parity, and the moved SERVICE deletion gate. Workspace
`optimizer-union-atomic` preserves the red plus focused/full 3/40/4/16-test greens; its exact-enumeration run documents
two separately owned typed-derivation completeness regressions without attributing them to the LMDB UNION slice.

Plan revision note (2026-07-17 07:02Z, Codex): recorded narrow safe-LeftJoin atomic mobility, the conditioned/scope/
composite/optional-output fail-closed matrix, duplicate and unbound bag parity, selective sibling reordering, and
`EXACT_SEQUENCE` preservation. Workspace `optimizer-leftjoin-atomic` preserves the red and final 4/2/43/6-test greens;
broad core exact verification remains held until the typed-derivation owner releases it.

Plan revision note (2026-07-17 07:45Z, Codex): recorded proof-specific logical derivations, proof-free typed route
canonicalization, route-local physical certificate union, proof-insensitive join wake-up removal, typed extractor
fail-closed behavior, and local finite-filter rewriting. Workspace `optimizer-legacy-gap` preserves the focused reds
and final 8/15/17/19-test greens. Recursive whole-subtree LMDB rules remain explicit provenance-cutover work; no
legacy route was deleted.

Plan revision note (2026-07-17 08:07Z, Codex): recorded proof-independent winner admissibility, Filter-local
OR-to-VALUES exploration, selected scoped-recipe provenance separation, and the restored nested-EXISTS fail-closed
boundary. Workspaces `optimizer-winner-role`, `optimizer-or-values-local`, and `optimizer-nested-exists` preserve the
focused reds and final 61/22/12-test class greens. Filter/MINUS and the observer-dependent OR-to-UNION/unused-OPTIONAL
rules remain explicit memo-local migration work; the broad queryalgebra rerun is held on its independently isolated
scoped cost-ledger regression.

Plan revision note (2026-07-17 08:20Z, Codex): closed the proof-only physical-metadata cost-ledger regression without
reopening executable winner frontiers. Canonical expression proof updates now refresh retained winner DAG objects and
nested provenance in place while preserving the executable `WinnerStateKey`; parent proofs remain route-local and
child certificates remain in selected-input provenance. Workspace `optimizer-cost-ledger` preserves the original
2/2 failure, the corrected focused 2/2 result, and the final 11/11 class result. Parent dependency wake-ups remain
available to proof-sensitive rules but no longer clear frontiers or duplicate global/scoped pricing.

Plan revision note (2026-07-17 08:30Z, Codex): reconciled typed EXISTS fail-closed behavior with generic correlated-
predicate closure. A composite outer Join with an atomic assured EXISTS/NOT EXISTS body now declines the binary typed
edge and falls through to generic prefix closure; a composite scalar-subquery body still reports an explicit
`NON_JOIN_COMPOSITE` boundary. Both previously failing exact selectors, the 12-test typed non-inner class, and the full
1,388-test queryalgebra/evaluation module are green in workspaces `optimizer-correlated-exists` and
`optimizer-queryalgebra-final`. The Filter/MINUS trace separately exposed a real composition gap: its memo-local
rewrite is explored and costed, but rebuilding from a raw tuple snapshot drops the already optimized/materialized
MINUS RHS; the performance assertions remain red until a generic child-alternative composition fix lands.

Plan revision note (2026-07-17 23:15Z, Codex): recorded finite mixed-transition classification, bitmap applied-edge
identity, producer-readiness admission, exact-once edge/predicate propagation, and provider-level canonical logical
edge cardinality. The source route remains gated exactly as before. Workspace `optimizer-typed-transitions` preserves
both red selectors and the final 60-test core plus 6-test LMDB shadow-parity greens; the first LMDB run's tests also
passed before the known unrelated `japicmp` incompatibility failed the lifecycle.

Plan revision note (2026-07-17 23:25Z, Codex): recorded the primitive-DPhyp global memo cutover seam. Global memo
enumeration now reports the primitive contributor unsupported without requesting its private winner tree; generic
exhaustive coverage remains complete, scoped primitive delivery is unchanged, and scalar compatibility APIs remain
intentionally present. Workspace `optimizer-primitive-dphyp-seam` preserves the focused red and final 1/18/9-test
greens.

Plan revision note (2026-07-18 00:33Z, Codex): recorded the shared scalar-effect prerequisite. Volatile and unknown
scalars now block movement consistently in the core rule engine, compiled rule DSL, standard Filter optimizer, and
LMDB legacy/scoped placement paths, while query-stable NOW and positively audited standard expressions retain legal
optimization. Runtime evaluation proves reopening UUID/BNODE changes observed values but NOW remains query-stable;
the final focused matrices pass 72/72 and 52/52 with all first reds preserved in
`initial-evidence.optimizer-scalar-effects.txt`.

Plan revision note (2026-07-19 00:09Z, Codex): closed the medical q7 cross-boundary predicate-placement gap in the
scoped join lifecycle. A repeatable Filter over a nested Join now contributes a typed mobile predicate and expands its
argument factors into the enclosing region when its required bindings are already assured; scalar memo inputs,
semantic barriers, backend outer-placement restrictions, route-only provenance, and volatile/unknown effect barriers
remain authoritative. Predicate occurrences participate in fragment identity and receive deterministic dense IDs, so
association/commutation deduplication cannot drop them. The original red expected two factors instead of three; the
focused extractor is green, the complete 22-test extractor class is green, and
`medicalQ7FullThemeFilterInPairsValuesLookupBeforePatientExists` is green with the anti-filter before patient EXISTS.
