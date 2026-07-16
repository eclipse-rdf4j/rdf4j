# Build a unified, self-reactivating query optimizer

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current as implementation proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J's LMDB query optimizer currently combines a long sequence of destructive tree rewrites, a Cascades-style memo,
specialized join planners, opaque whole-subtree physical alternatives, and post-planning repair passes. A specialized
planner can therefore prevent a generic rule from running, and a rewrite that introduces a useful finite `VALUES`
relation can arrive too late to wake join ordering again. The reported medical-record query demonstrates the result:
the three-value relation is opened once per broad observation row instead of restricting the value lookup first.

After this work, query optimization is one dependency-driven search. Every logical alternative, newly derived fact,
physical implementation, child-winner change, estimate refinement, and required-property change wakes precisely the
rules and cost tasks that depend on it. Specialized LMDB rules add alternatives without owning or closing a part of the
search space. Complete mode terminates only when the registered rule and costing queues are empty; bounded mode is an
explicit opt-in that reports all deferred work. A user can observe the change in Explain output: the medical finite
anchor precedes the broad value lookup, composite MINUS/EXISTS plans show independently selected children, no node is
tagged `covered_by_parent_winner`, and the plan reports `COMPLETE`.

This change first establishes the reusable optimizer in `core/queryalgebra/evaluation`, then makes LMDB its first full
adopter. The legacy LMDB pipeline remains only as a shadow oracle while migration is in progress and is removed from
production routing before completion. No new third-party dependency or runtime code generation is introduced.

## Progress

- [x] (2026-07-13 13:52Z) Captured the clean root `-Pquick` build baseline; all reactor modules installed successfully.
- [x] (2026-07-13 13:52Z) Created this self-contained ExecPlan and recorded the superseding architecture decisions.
- [x] (2026-07-13 13:59Z) Added and captured the red rule-reactivation regression; the consumer is attempted once
  and never retried after a producer adds its declared memo dependency.
- [x] (2026-07-13 14:01Z) Added and captured the red duplicate-rule-ID regression; registry construction currently
  accepts two distinct rules with the same scheduler identity.
- [x] (2026-07-13 14:08Z) Added and captured the red child-frontier regression; a late one-row child leaves the
  cached root at 103 work rows instead of recosting it to 4.
- [x] (2026-07-13 14:09Z) Added and captured the red registration-order saturation contract; neither ordering reaches
  the dependency-created alternative under the one-shot ledger.
- [x] (2026-07-13 14:22Z) Added dense memo expression IDs, revisioned `MemoChange` events, reverse child-parent uses,
  winner-frontier invalidation, rule descriptors and operator indexes, duplicate-ID validation, and structured
  completeness/pending-task reporting.
- [x] (2026-07-13 14:41Z) Replaced the one-shot rule ledger with revision-keyed exploration/implementation queues and
  a fixed-point search loop. The full 105-test `CascadesRuleEngineTest` class now passes, including rule reactivation,
  late child recosting, registration-order saturation, exact-cycle convergence, and bounded pruning.
- [x] (2026-07-13 15:08Z) Added typed memo input slots for tuple children, scalar EXISTS/NOT EXISTS subqueries, and
  property-path expressions, including explicit semantic barriers and executable per-input physical requirements.
- [x] (2026-07-13 15:34Z) Made `CascadesCostModel.localCost` operator-local, moved total composition into the planner,
  and migrated LMDB materialized MINUS/EXISTS and connected-hypergraph alternatives to transparent child groups.
- [x] (2026-07-13 15:34Z) Removed the DPhyp ownership guard and registered standard logical rules directly. The full
  54-test `LmdbCascadesOptimizerTest` class and 108-test core rule-engine class pass after the change.
- [x] (2026-07-13 15:34Z) Added the backend-neutral `JoinSearchService`, deterministic contributor routing, immutable
  join regions, dependency-legal exhaustive bushy enumeration, and an independent exhaustive-oracle test.
- [x] (2026-07-13 16:06Z) Migrated every LMDB composite implementation away from opaque whole-tree alternatives:
  access paths, finite VALUES, row-preserving wrappers, star scans, MINUS/EXISTS, and DPhyp now retain memo children.
- [x] (2026-07-13 16:06Z) Made per-child bound-input and access-path requirements executable, rejected composite
  atomic implementations, and deleted the finite-VALUES/ListMember winner-dominance shape exception.
- [x] (2026-07-13 16:46Z) Added occurrence-safe memo join-region extraction, a group-level `ENUMERATE_JOIN` queue,
  reverse region subscriptions, revision-keyed reactivation, subset equivalence groups, and transparent bushy-tree
  materialization. Candidate insertion cannot re-enumerate an unchanged region revision.
- [x] (2026-07-13 16:46Z) Split logical join enumeration from physical costing, raised the deterministic exact
  contributor to eight factors with subset-state enumeration, and made absent exhaustive coverage explicit pending
  work. The focused join scheduler class passes 4 tests and the join service passes 9 tests.
- [x] (2026-07-13 17:24Z) Added transparent safe-Filter join factors, immutable factor/goal request context,
  deterministic bounded candidate accounting, transitive parent invalidation, and keyed incomplete join retries that
  clear their pending state only after exhaustive coverage completes.
- [x] (2026-07-13 17:30Z) Made proof-only structural merges first-class memo mutations. A new `PROOFS_CHANGED` event
  advances dependency revisions, wakes proof-dependent rules, and refreshes transitive parent provenance at
  quiescence; all 110 scheduler tests pass.
- [x] (2026-07-13 18:25Z) Replaced widened join dependency hashes and string region keys with immutable typed values,
  retained every source-expression subscription, and installed exhaustive join alternatives as memo-local oriented
  subset partitions rather than expanded complete trees.
- [x] (2026-07-13 18:25Z) Prevented quiescence without an executable root winner from reporting `COMPLETE`; plans now
  report `NO_VIABLE_PHYSICAL_PLAN` with a root-group diagnostic, and the four-test completeness contract passes.
- [x] (2026-07-13 18:25Z) Added LMDB's occurrence-safe DPhyp candidate seam and contributor. Its private rank is
  diagnostic only; the unified service receives a `JoinTree`, retains repeated occurrences, and remains complete when
  DPhyp is disabled because the exhaustive contributor stays registered.
- [x] (2026-07-13 18:25Z) Removed legacy whole-tree join commute/association rules from production rule scheduling.
  Their DSL specs remain directly testable legality contracts, while the unified join service alone installs the same
  alternatives as shared memo partitions.
- [x] (2026-07-13 19:59Z) Indexed rule work by typed logical, physical, proof, fact, child, estimate, statistics, and
  required-property events before calling `matches`. Physical alternatives no longer rematch logical-only rules, and
  parent groups receive an explicit child-winner event without waking the winning group itself.
- [x] (2026-07-13 19:59Z) Enforced explicit `JoinRegion.requiredBefore` constraints inside the LMDB DPhyp contributor,
  removed the legacy connected-hypergraph production registration, and made generic and bound-lookup implementations
  coexist without join-island ownership gates. The focused routing and 20-test hypergraph suites pass.
- [x] (2026-07-13 19:59Z) Audited all 22 pre-Cascades and three post-Cascades LMDB passes against memo parity. The
  audit identified post-cost finite-filter materialization as the direct reason a newly created VALUES relation cannot
  wake join ordering, and established an incremental retirement order rather than a wholesale semantic cutover.
- [x] (2026-07-13 20:12Z) Injected LMDB's DPhyp and exhaustive contributors into the production `CascadesPlanner`
  through the same service, exposed `optimizer.cascadesCompleteness`, and migrated ownership-era admissibility tests.
  Enabled and disabled DPhyp routes both complete, while generic and specialized implementations remain eligible.
- [x] (2026-07-13 20:12Z) Made task/deadline bounding explicit-only. The production default plus legacy `auto` and
  `exact` values now drain to `COMPLETE` even when the configured bounded budget is one task; explicit `budgeted`
  remains visibly `BUDGET_EXHAUSTED` when it defers work.
- [x] (2026-07-13 20:20Z) Removed the eager iterative UNION-factoring pass and the logical-rule parity gate. Common
  prefix factoring is now an always-registered memo alternative, and the retired property cannot suppress it.
- [x] (2026-07-13 20:41Z) Canonicalized winner-frontier identity by delivered properties, admissibility, and the
  `CostVector.compareTo` objective dimensions. The eight-factor exact oracle now completes with 607 retained winners
  and a maximum frontier of 11 instead of exhausting a 4 GiB fork.
- [x] (2026-07-13 20:41Z) Coalesced pending expression work by kind and dense expression ID, merging the newest
  dependency revision and every wake event. The focused producer storm now observes revisions `[0, 24]` in two rule
  attempts rather than processing 25 queued historical revisions.
- [x] (2026-07-13 21:59Z) Added typed fact/property/change declarations to rule descriptors, rejected uncontracted
  rules in unbounded exact/shadow search, and narrowed LMDB's legacy implementation wake set from all ten events to
  five declared dependencies. The four-factor selector now holds 454 expressions instead of 17,535.
- [x] (2026-07-13 22:09Z) Removed the eager LMDB projection-pushdown pass after making memo projection distribution
  preserve subquery/scope/context metadata and preventing projection merge from erasing semantic boundaries. Focused
  projection-rule and pipeline ownership regressions are green.
- [x] (2026-07-13 22:46Z) Added a physical cost-application ledger keyed by expression, complete goal, statistics and
  fact revisions, and ordered canonical child states. Rejection provenance now retains one stable diagnostic class;
  the focused parent replay regression fell from six/four repeated prices to one price for each child state.
- [x] (2026-07-13 23:10Z) Separated statistical output evidence from executable physical properties, added typed
  per-fact memo revisions and changed-fact wake routing, and invalidated dependent winner frontiers on refinement.
  Duplicate physical implementations now merge exact finite-relation facts without splitting executable identity;
  the combined scheduler, memo, cost-ledger, and fact-channel selection passes all 49 tests.
- [x] (2026-07-13 23:29Z) Replaced LMDB's inherited wildcard/all-facts descriptor with constructor-required finite
  root, fact, child-property, output, and wake contracts for all 16 registered direct rules. Added distinct property-
  path root keys and proof-change reactivation; standard and specialized rules remain co-eligible, and the 79-test
  LMDB optimizer/routing/admissibility sweep passes.
- [x] (2026-07-13 23:41Z) Deleted post-Cascades finite-filter materialization and its manual left-deep join rebuild.
  A focused red/green regression proves correlated placement can no longer remove a Filter or inject an uncosted
  VALUES relation; finite-relation alternatives and proofs now originate only inside memo exploration.
- [x] (2026-07-14 00:54Z) Split finite possible-value domains from exact bag-frequency relations, propagated the
  versioned domain fact through joins and wrappers, and made domain-only refinement wake only its declared consumers.
  A disconnected prefix retains the three medical values without falsely retaining an exact composite relation.
- [x] (2026-07-14 01:05Z) Replaced arity-based provenance annotation with the memo's typed input layout. Scalar
  subqueries and path expressions now receive their independently selected provenance; the JDK 26 medical plan is
  VALUES-first and contains neither `covered_by_parent_winner` marker.
- [x] (2026-07-14 01:17Z) Separated finite-domain distinctness from nested-loop execution mass. Composite-prefix rows
  now scale concrete LMDB lookup output/work and seek counts, while inferred uniform domain frequencies are not
  reported as exact evidence. The focused estimator/access/path sweep passes 21 tests.
- [x] (2026-07-14 01:30Z) Deleted the internal opaque whole-tree emergency winner, enforced the atomic-leaf invariant
  inside `Memo`, and made LMDB's generic physical implementation unconditional. Missing implementations report
  `NO_VIABLE_PHYSICAL_PLAN`; specialized MINUS/EXISTS rewrites no longer suppress the generic route. The focused core
  sweep passes 160 tests and the LMDB optimizer/contract/admissibility sweep passes 68 tests.
- [x] (2026-07-14 01:53Z) Deleted the `covered_by_parent_winner` writer and every LMDB routing/feedback reader. Selected
  composite provenance must have one child decision per typed input or fail explicitly; generic scalar subquery
  winners are no longer submitted to a second optimizer pass. The core and LMDB sweeps pass 158 and 66 tests.
- [x] (2026-07-14 02:01Z) Split unsupported logical algebra from physical `MATERIALIZE` in the immutable IR. A shared
  tuple-input codec now keeps unary, binary, path, structural, and scalar-subquery children transparent behind a
  `NATIVE_BOUNDARY`; round-trip, DSL, and memo coverage pass 73 tests.
- [x] (2026-07-14 03:41Z) Added predicate-aware join states keyed by factor occurrences and applied predicates. Exact
  join search closes every legal topology over every newly assured predicate placement, including correlated
  EXISTS/NOT EXISTS scalar inputs, instead of relying on syntax-directed FILTER pushdown.
- [x] (2026-07-14 03:41Z) Completed the executable bound-input cost contract for this slice. Context-costed leaves
  expose incoming requirements, typed scalar inputs consume sibling-assured correlations, and parameterized
  composite RHS work is scaled per outer execution when no store estimate prices the whole composite.
- [x] (2026-07-14 03:41Z) Made the reported correlated-prefix regression cost-select the TYPE-only placement with
  `COMPLETE` status. The focused LMDB test passes without a placement preference, query fingerprint, or post-plan
  repair; the known unrelated `japicmp` check is explicitly skipped.
- [x] (2026-07-14 04:20Z) Made unsupported native-boundary binding facts follow the selected transparent children
  instead of the preserved import tree, so later rewrites cannot retain stale assured bindings.
- [x] (2026-07-14 04:20Z) Added execution-domain-aware memo identity and typed definition inputs. SERVICE bodies stay
  visible as `REMOTE_DEFINITION` groups for dependencies and lossless extraction, but receive no local rule, join,
  statistics, child-frontier, or cost work; the local SERVICE operator still reaches `COMPLETE`.
- [x] (2026-07-14 04:50Z) Centralized native-root support classification and made native parents, native children,
  scalar subqueries, definition inputs, and variable-scope transitions explicit semantic barriers on memo edges.
- [x] (2026-07-14 04:50Z) Persisted logical/transparent/atomic implementation form in memo structural identity.
  Unknown zero-input operators now import with uncertain facts and report `UNSUPPORTED_ATOMIC_BOUNDARY` until an
  explicit atomic-leaf rule supplies an executable implementation.
- [x] (2026-07-14 05:06Z) Made bounded scheduling establish an executable root frontier before draining global
  expansion, then added the missing dependency-keyed `COST_EXPRESSION` queue. Missing child frontiers defer and wake
  on child/group revisions instead of becoming false terminal rejections; the scoped-UNION OPTIONAL regression is
  green with its specialized transparent alternative accepted.
- [x] (2026-07-14 05:06Z) Deleted `LmdbCorrelatedFilterPlacementOptimizer` and its post-Cascades pipeline registration.
  Correlated predicate placement now ends inside memo saturation, so winner extraction is no longer followed by a
  manual left-deep join rebuild or an uncosted materialized anti rewrite.
- [x] (2026-07-14 05:29Z) Removed legacy pre-search standard-plan shortcut, comparison, and cost-bound routing. Every
  enabled query enters the unified scheduler; a legal Cascades winner cannot lose to a baseline, and a successful
  `COMPLETE` winner is extracted once without `optimizeSubtrees` or semantic fallback repair afterward.
- [x] (2026-07-14 05:29Z) Made typed `GROUP` IR lossless for every core aggregate shape: COUNT/AVG/SUM/MIN/MAX/SAMPLE,
  COUNT(*), GROUP_CONCAT separators, custom n-ary function IRIs, arguments, and DISTINCT now survive import/export.
  Unsupported custom aggregate classes remain explicit transparent native boundaries rather than lossy typed nodes.
- [x] (2026-07-14 05:48Z) Made zero- and arbitrary-length property paths first-class lossless IR operators. Endpoint
  terms, context scope, constant/anonymous flags, minimum length, and the transparent step-expression child survive
  import/export; the executable child remains independently selectable behind an explicit legality barrier.
- [x] (2026-07-14 06:01Z) Added a typed `BAG`/`SET`/`EXISTENCE` observer lattice and dense per-expression
  admissibility routes. `Distinct` now strengthens only its input goal to `SET`, `EXISTENCE` remains preserved,
  source routes combine conservatively with rule proofs, and structural duplicates retain the least restrictive
  route while emitting a dependency wake-up. The focused scope contract passes 3 tests; the complete scheduler and
  memo model pass 123 and 41 tests with exact search quiescent.
- [x] (2026-07-14 06:18Z) Made exact `Service` and `TripleRef` operators first-class lossless IR nodes. SERVICE keeps
  its endpoint term, source text, prefixes, base URI, SILENT flag, and synchronized transparent definition input;
  the definition remains visible in the `REMOTE_DEFINITION` memo domain without receiving local optimization work.
  TripleRef preserves all RDF-star terms and constant-aware binding facts, while subclasses with extra state remain
  explicit native boundaries. The focused red/green contracts pass, the complete IR round-trip suite passes 17
  tests, and the existing SERVICE domain and extraction contracts both pass.
- [x] (2026-07-14 07:24Z) Replaced parent-class semantic-goal special cases with immutable ordered semantic
  requirements on every memo input edge. `Distinct`, no-key all-`COUNT(DISTINCT)` Group inputs, and scalar subqueries
  now declare `SET` or `EXISTENCE` contracts in the shared input layout; logical and physical alternatives preserve
  those contracts as structural identity, and the planner combines them with the inherited observer goal.
- [x] (2026-07-14 07:24Z) Migrated finite membership under distinct aggregate observation into the self-reactivating
  memo lifecycle. `LmdbDistinctFiniteMembershipRule` inspects every child-group alternative, installs its local
  `SET`-safe membership alternative back into that same group, and installs the full aggregate as `BAG`-safe without
  creating an opaque child island. The recursive set-normalization rule is no longer registered in production.
- [x] (2026-07-14 09:28Z) Added the canonical, scope-aware `StreamBindingSchema` contract and routed memo binding
  shape, logical properties, and logical-alternative validation through it. Scalar `EXISTS`/`NOT EXISTS` locals no
  longer masquerade as stream outputs when filter conditions are merged; the schema matrix passes 3 tests and the
  complete memo-model class passes 46 tests.
- [x] (2026-07-14 09:28Z) Hardened distinct finite membership so counted correlations must be assured, scalar
  dependencies in remaining `BIND` expressions cannot consume probe-only bindings, and only the complete observed
  Group carries the bag-equivalence proof. The complete membership class passes 7 tests.
- [x] (2026-07-14 09:38Z) Migrated the late finite-code/type test injector and schema assertions from raw RDF4J
  binding names to `StreamBindingSchema`. This preserves the production rule's root-only observer alternative,
  separate normalized child group, child wake-up, and Group-only proof; the complete class passes 6 tests.
- [x] (2026-07-14 10:20Z) Closed the P0 physical/schema routing gap at the shared winner boundary. Every physical
  declaration is restricted to its memo group's canonical stream outputs before property satisfaction, memo fact
  merging, or dominance; scalar-subquery locals can no longer become bound, distinct, ordered, or statistical winner
  outputs through a specialized implementation.
- [x] (2026-07-14 10:20Z) Made scalar correlation occurrence-aware in join-region extraction and LMDB routing. A
  same-name producer written after a scalar filter carries an explicit anti-reorder dependency, while a producer
  already visible before that filter reports `CORRELATED_SCALAR_INPUT` rather than silently changing semantics.
  LMDB DPhyp now derives connectivity and required inputs from immutable factor descriptors rather than rescanning
  scalar-subquery syntax. The focused core pair and LMDB execution/descriptor trio are green.
- [x] (2026-07-14 12:14Z) Captured a generic dependent-frontier red for the medical-q1 cost explosion. A two-row
  exact outer relation never reaches the transparent RHS frontier; the parent instead recursively reprices the RHS
  syntax at 200 rows and 208 work rows. Evidence is retained in `initial-evidence.medical-q1-cost.txt`.
- [x] (2026-07-14 14:19Z) Made canonical selected-prefix binding context part of goals, winners, rule/cost ledgers,
  and statistics caches. Dependent RHS frontiers now optimize from the selected child's exact relation, parent totals
  compose selected children exactly once, and recursive selected-RHS syntax repricing is gone.
- [x] (2026-07-14 14:19Z) Finished the observer-equivalent materialized semi/anti contract. Correlated EXISTS uses
  `SET`, uncorrelated EXISTS uses `EXISTENCE`, selected children remain transparent, and exact semi-join evidence
  preserves matching left multiplicity without multiplying it by duplicate RHS rows.
- [x] (2026-07-14 14:19Z) Deleted cross-context winner-bag recovery by equal row count. Rich child evidence now comes
  only from the context-specific selected winner's delivered binding profile, preventing unrelated finite relations
  from leaking through cost caches.
- [x] (2026-07-14 14:26Z) Added adaptive MINUS overflow costing against the execution step's shared internal limit.
  When the estimated RHS exceeds the cap, local cost includes every RHS reopening and memory remains bounded by the
  cap; the focused regression moved from 8 versus 408 work rows to green.
- [x] (2026-07-14 14:33Z) Reverified the complete 69-test cost-model class, complete 55-test LMDB optimizer class,
  medical-shaped pipeline, selected-context/local-cost trio, and complete 9-test finite-membership class with zero
  failures or errors. Full logs are under workspace `medical-q1-cost` and listed in Artifacts and Notes.
- [x] (2026-07-14 16:54Z) Rebuilt `ThemeQueryPlanRunBenchmark` and ran medical query 1 on JDK 26 with unified
  statistics. The aggregate and plan guard pass, but the selected plan retains the broad two-arm UNION, reports
  `UNSUPPORTED_ATOMIC_BOUNDARY`, and measures 326.344 ms/op without warmup versus the prior warmed 86.864 ms/op.
  Added a red end-to-end plan regression covering completeness, finite code/type factoring, and access order.
- [x] (2026-07-14 17:07Z) Replaced telemetry-inferred invocation semantics with explicit `PER_INVOCATION` and
  `TOTAL_FOR_CONTEXT` cost scope. Same-context selected children are normalized to one invocation, composed once,
  and rescaled once; the focused `100 -> 10,000` and telemetry-key regressions, all 73 cost-model tests, and all 46
  memo-model tests are green. Evidence is retained in `initial-evidence.context-cost.txt`.
- [x] (2026-07-14 17:11Z) Ran the full medical query 5 plan regression against the selected-theme store. The current
  unified search already renders the exact three-value relation before `med:value`; the red assertions only assumed
  the specialized anti implementation must be `MINUS`, while the legal selected implementation is transparent
  `FILTER NOT EXISTS`. Narrowed the permanent contract to accept either transparent anti form and reject hidden
  parent-winner children. Evidence is retained in `initial-evidence.medical-q5-plan.txt`.
- [x] (2026-07-14 17:16Z) Traced medical query 1's remaining `3,315,373,045` estimate to
  `LmdbEstimationEngine.finiteBindingLookup`: global RDF-type buckets are scaled across 99,670 outer invocations even
  though the finite `(type, code)` relation has no concrete `entity` bindings. Added a general context-total
  composition regression and began a separate context-coverage regression while retaining query 5's valid finite
  object lookup as the non-regression case.
- [x] (2026-07-14 17:55Z) Ran medical query 5 through the real JDK 26 JMH harness after the context-cost changes. The
  result is correct and the optimizer reports no plan-guard failure, but runtime regressed to 204.522 ms/op from the
  prior 12.939 ms/op. Runtime telemetry shows 148,900 `med:value` probes because the three-row finite relation is
  joined to a disconnected patient/encounter/observation chain before reaching its consuming statement pattern.
- [x] (2026-07-14 17:56Z) Captured the generic context-total defect in
  `LmdbEstimationEngineTest#disconnectedStatementPatternRepeatsForEveryOuterInvocation`: a four-row statement surface
  disconnected from a three-row outer context is reported as 4 rows/work instead of 12. Red evidence is retained in
  `initial-evidence.q5-disconnected-context.txt`; no query-specific production branch is permitted by the fix.
- [x] (2026-07-15 08:53Z) Made exact finite relations an executable physical-property contract and preserved logical
  child requirements across later compatible rewrites and implementations. Added a multi-rewrite regression proving
  the contract survives memo insertion and final extraction without expression-ID pinning or planner ownership gates.
- [x] (2026-07-15 09:00Z) Removed two generic cost distortions: exact `BagEstimate` cardinality now retains its exact
  interval, and local physical cost subtracts cumulative child work, memory, seeks, and page walks without repricing
  zero local work through parent row uncertainty. The medical Q9 end-to-end structural gate is green: both exact
  finite domains win, the code filter is consumed before broad access, and the anti RHS is materialized once.
- [ ] Route remaining cost/winner and LMDB legality sites through `StreamBindingSchema`; add equivalent pre-mutation
  validation for physical alternatives and for malformed logical alternatives with invalid semantic requirements.
- [ ] Complete memo-local IR vocabulary and statistics/fact revisions, then remove the compatibility tree bridge.
- [x] Replace whole-subtree/opaque costing with transparent child requirements and one local-cost composer.
- [x] Introduce a unified join-region service and migrate DPhyp/exhaustive search as non-owning contributors.
- [ ] Move pre/post LMDB optimization passes into the memo rule lifecycle and reduce the production pipeline.
- [ ] Run shadow parity, semantic corpora, the JDK 26 matrix, planning/runtime benchmarks, formatting, and diff audit.

## Surprises & Discoveries

- Observation: the reported finite rewrite is explored, but a child estimate that already covers 66,527 outer
  invocations and returns 49,835 rows is composed as if it were per invocation. The resulting alternative carries
  exactly `66,527 * 49,835 = 3,315,373,045` rows and loses to the broad UNION scan. A diagnostic
  `plannedRepeatedInvocations` key currently also changes `FactorCostEstimate` semantics, so telemetry can alter
  winner selection. The cost contract needs an explicit invocation scope and must compose each selected child total
  exactly once.
  Evidence: `/tmp/medical-q1-cost-trace.txt` and
  `initial-evidence.medical-q1-plan.txt`.
- Observation: the exact q1 inflation is born one layer earlier than the initial parent-composition trace implied.
  For the six-row `(type, code)` relation, `finiteBindingLookup` scales each type frequency by
  `99,670 / 6`; it then multiplies global Condition/Medication bucket counts by 49,835 executions, yielding
  `66,527 * 49,835`. Assured-but-not-concrete `entity` input is invisible to that scaling. Query 5 does not take this
  invalid path: its three concrete values legitimately contribute three predicate/object buckets before
  `med:value`.
  Evidence: memo groups `g13 -> g91 -> g28 -> g27` in the medical-q1 cost trace and the focused
  `LmdbEstimationEngineTest` context-coverage regression.

- Observation: textual ordering is not a sufficient finite-anchor acceptance invariant. Query 5 renders the finite
  `VALUES` relation before `med:value`, but its selected join tree first combines that relation with a disconnected
  patient/encounter/observation chain. The broad value access is consequently opened about 148,900 times instead of
  three; its disconnected intermediate is observed at 148,900 rows while planned at about 8,300 rows. Under the
  explicit total-context contract, a disconnected leaf must repeat across the complete invocation mass, while a
  leaf sharing assured input bindings is conditioned against that context and composed once.
  Evidence: `/tmp/medical-jmh-20260714-q5-rowgoal/benchmark-output.log`, its `plans/` directory, and
  `initial-evidence.q5-disconnected-context.txt`.

- Observation: the earlier values-first JDK 26 checkpoint was not stable across the later stream-schema and
  transparent-boundary migrations. Acceptance must therefore exercise the real benchmark setup after each core
  contract change, not infer plan quality from isolated rule and cost-model tests.
  Evidence: the current rendered plan under
  `/tmp/medical-jmh-20260714-q1-post-cost-pre-fix/plans/` retains UNION and places `med:code` before the finite
  condition.

- Observation: `CascadesPlanner.exploreGroup` visits expressions appended to one group, but `SearchState.ruleFired`
  permanently suppresses a rule for an expression and goal. The key contains no child, fact, winner, or statistics
  revision, while `SearchState.exploredGroups` lets a previously visited parent return its cached winner.
  Evidence: `CascadesPlanner.java`, methods `optimizeGroup`, `exploreGroup`, and `applyRule`.

- Observation: `Memo.addPhysicalAlternative(..., opaque=true)` deliberately gives a composite expression no child
  groups. This removes child optimization, child costs, provenance, and later wake-ups rather than merely changing
  Explain output.
  Evidence: `Memo.java` chooses `List.of()` instead of `internInputs(alternative)` for opaque applications.

- Observation: materialized MINUS computes left and right subtree estimates, then declares work as roughly left rows
  plus right rows. Because the alternative is opaque, selected child work is never composed back into the total.
  Evidence: `LmdbMaterializedSemiAntiRules.java`, `LmdbMaterializedMinusAntiSemiRule`.

- Observation: winner dominance contains a benchmark-shape exception that recognizes a `BindingSetAssignment` and a
  `ListMemberOperator`. Correct finite-anchor selection must follow from exact facts and ordinary cost dominance.
  Evidence: `MemoGroup.WinnerFrontier.finiteAnchorBoundInputAlternative`.

- Observation: the existing immutable planner IR is a useful typed vocabulary but is not a viable memo unchanged.
  Compiled rules repeatedly convert complete `TupleExpr` subtrees to and from `PlanIr`, unsupported operators become
  zero-input `MATERIALIZE` nodes, and child-group alternatives are invisible to a parent's concrete tree pattern.
  Evidence: `TupleExprToIr`, `IrToTupleExpr`, `CompiledRule`, and `MemoExpr`.

- Observation: the reported medical query and the post-cost VALUES pass are independent failure modes. The post-pass
  accepts only all-IRI top-level anchors and tagged its output `lmdb-post-cost-finite-filter-values-rewrite`, while the
  medical numeric anchor is produced inside memo exploration by `lmdb-guarantee-options` and then retains append
  order because join enumeration is not reactivated for the newly shaped region.
  Evidence: the focused post-pass regression rewrote the two-IRI Filter after Cascades; the captured medical plan's
  VALUES node instead carries `optimizer.guaranteeOption=finite-anchor:value`.

- Observation: rule IDs are used as scheduler identity, but `RuleRegistry.Builder` does not reject duplicates. LMDB
  currently registers duplicate logical rule IDs, so one matching implementation can suppress another accidentally.
  Evidence: `RuleRegistry.Builder.build` and `CascadesPlanner.applyRule`.

- Observation: LMDB has two concrete duplicate-ID pairs: the compiled and Java finite-filter-values rewrites both use
  `lmdb-finite-filter-values-rewrite`, while the generic and predicate-domain VALUES anchors both use
  `filter-values-anchor`. Strict registry validation therefore requires removing the redundant first registration and
  assigning the LMDB specialization its own identity without suppressing the generic rule.
  Evidence: `LmdbCascadesRuleProvider`, `LmdbSemanticRuleSpecs`, and the Java rule implementations.

- Observation: the only tracked edits present when this work began are user-owned changes in
  `ThemeQueryPlanRunBenchmark.java` and `ThemeQueryPlanRunBenchmarkTest.java`. They fix canonical benchmark store
  paths and retain user-selected JMH warmup, measurement, fork, and parameter settings. They must not be overwritten.
  Evidence: `git status --short` and the scoped diff captured before this plan was created.

- Observation: a transformation which wraps its source group can create a legal recursive memo edge with a stronger
  child goal. The old recursive planner then allowed the same in-progress goal to consume its partially built frontier,
  producing an unbounded sequence of Pareto candidates whose evidence dimensions differed even as work increased.
  Evidence: the pre-fix `plannerPropagatesRewriteMetadataToRules` timed out; a live `jcmd Thread.print` showed repeated
  `WinnerFrontier.canAdd` calls from recursive input-winner enumeration. Rejecting only an active identical `WinnerKey`
  preserves the stronger-goal rewrite and makes the focused test pass in 0.113 seconds.

- Observation: scalar subqueries and property-path expressions are not represented by RDF4J's unary/binary tuple
  operator hierarchy, so a child layout based only on tuple operators silently hides optimizable inputs.
  Evidence: `MemoInputLayout` now discovers `SubQueryValueOperator` and `ArbitraryLengthPath` inputs, and the focused
  memo-model tests prove those groups survive import, costing, and plan rebuilding.

- Observation: the typed GROUP importer recorded aggregate output names with a placeholder function, while the
  exporter always constructed `Group(..., List.of())`. A legal distinct COUNT therefore emerged with no aggregate
  expression at all even though the node was classified as supported typed IR.
  Evidence: `AggregateIrRoundTripTest#groupRoundTripPreservesAggregateOperatorAndDistinctFlag` failed with one
  expected `Count (Distinct)` element and an empty rendered group-element list.

- Observation: LMDB's external standard-plan baseline cannot simply be deleted. The internal emergency fallback is
  created only after rule exploration, while budget and deadline exits can return earlier with no executable winner.
  Evidence: `CascadesPlanner.optimizeGroup` exits before `seedExistingPlanWinner`, and
  `LmdbStandardPlanBaselineOptimizer` currently supplies a separate ThreadLocal candidate outside the memo.

- Observation: the subject-star rule prices and annotates a composite `Join`, but no evaluator consumes its
  `starMultiPredicateScan` marker as a distinct executable operator. Treating that tree as an opaque leaf therefore
  hides independently optimizable factors and claims a physical implementation that does not exist.
  Evidence: `LmdbStarMultiPredicateScanRule`, `LmdbStarJoinScanSupport`, and the absence of an execution-strategy use
  of `LmdbStarJoinScanSupport.ACCESS_MODE`.

- Observation: expression-ID rule scheduling is not a safe place for join enumeration. Each emitted binary tree is a
  new expression and would recursively enumerate the same factor region. Region work instead needs a canonical
  factor-occurrence fingerprint, goal/statistics identity, factor dependency revisions, and its own saturation ledger.
  Evidence: `CascadesPlanner.JoinWorkKey` and `CascadesJoinRegionEnumerationTest`.

- Observation: memo group IDs cannot identify factor occurrences. A self-join may contain two occurrences of one
  equivalent group, and collapsing them loses multiplicity and dependency orientation.
  Evidence: `JoinFactor(occurrenceId, groupId)` and `MemoJoinRegionExtractorTest`.

- Observation: standard commute/associate rules can install a legal tree before the unified enumerator reaches it.
  Structural dedup must therefore merge the route proof into the existing expression without incrementing semantic
  revisions; otherwise the route ran correctly but its provenance disappeared.
  Evidence: the first post-integration `CascadesJoinRegionEnumerationTest` remained red until proof-only merging was
  added to `Memo.addLogicalAlternativeWithInputs`.

- Observation: proof attachment is itself optimizer knowledge. Replacing an expression's proof list without a memo
  event leaves proof-gated consumers and parent provenance stale once unrelated winner work has quiesced.
  Evidence: `proofOnlyLogicalMergeReactivatesConsumerAndRefreshesParentProvenance` failed with zero consumer
  applications until `MemoChange.PROOFS_CHANGED` advanced the dependency revision and invalidated reverse parents.

- Observation: a factor-only string fingerprint cannot safely identify a join region once dependency edges,
  predicates, barriers, or repeated-factor constraints exist. It can also collapse association-distinct source
  subscriptions, while the current dependency stamp omits expanded intermediate groups.
  Evidence: `MemoJoinRegionExtractor.fingerprint`, `CascadesPlanner.JoinSubscriptionKey`, and
  `joinDependencyRevision` currently encode only sorted leaf groups/revisions. The migration must replace this with an
  immutable region key and exact read-set stamp before adding those semantics.

- Observation: emitting every complete binary join tree defeats memo sharing before the memo can exploit it. Eight
  disconnected factors have 17,297,280 oriented trees, although all alternatives can be represented by a much smaller
  set of subset-partition expressions whose child groups implicitly compose the same trees.
  Evidence: `ExhaustiveLegalJoinSearchContributor.SubsetEnumerator.emit` recursively expands both child tree products,
  and `JoinSearchService.enumerate` retains every distinct complete-tree fingerprint.

- Observation: installing subset partitions is not sufficient if each partition is subsequently sent through the old
  whole-tree commute and association rules. The eight-factor regression stayed bounded at 6,050 join-service
  candidates but exhausted the heap while repeatedly converting expanded joins in `CompiledRule.trace`.
  Evidence: `logs/mvnf/20260713-181210-verify.log` and a live `jcmd Thread.print` rooted at
  `TupleExprToIr.convert -> CompiledRule.apply -> CascadesPlanner.drainRuleWork`.

- Observation: removing the duplicate whole-tree rewrites bounded the candidate space, but a physical-expression
  addition still schedules every expression in its memo group and asks every root-compatible rule to match before the
  revision ledger can reject an irrelevant application. On the eight-factor oracle this repeatedly converts logical
  joins to IR even though the triggering event cannot create a new logical match.
  Evidence: the sampled stack from the focused eight-factor run was rooted at
  `RuleRegistry.evaluateRule -> CompiledRule.sharedIr -> TupleExprToIr.convert`, after the join service had already
  stopped at its 6,050 oriented subset-partition bound.

- Observation: LMDB's new DPhyp contributor preserved occurrence identity but initially forwarded only the legacy
  planner's inferred binding dependencies. A contributor could therefore return an order that violated an explicit
  `JoinRegion.requiredBefore` edge even while the exhaustive contributor retained the legal search space.
  Evidence: `LmdbDphypJoinSearchContributorTest` selected `[12,10]` for a region requiring occurrence 10 before 12;
  the red report is `logs/mvnf/20260713-183723-verify.log`.

- Observation: production LMDB still registers the legacy connected-hypergraph implementation rule and predicates
  the generic RDF4J implementation on join-island ownership. The unified DPhyp contributor therefore exists as an
  isolated seam but is not yet the production route, and specialization can still suppress generic implementation.
  Evidence: both assertions in `LmdbUnifiedJoinRoutingTest` fail in
  `logs/mvnf/20260713-183859-verify.log`.

- Observation: event-indexed matching removes repeated whole-tree IR conversions, but exact physical costing still
  takes the Cartesian product of every child winner frontier. The eight-factor regression consumed about 4.5 GiB and
  remained in `enumerateInputWinnerCombinations -> WinnerFrontier.canAdd` after 2.5 minutes even though the logical
  join space was already bounded at 6,050 subset partitions.
  Evidence: `logs/mvnf/20260713-185920-verify.log` and a live `jcmd Thread.print` of the Surefire fork.

- Observation: `LmdbCorrelatedFilterPlacementOptimizer` calls
  `materializeFiniteFilterValuesAfterCosting` on the extracted winner. The winner is therefore priced as FILTER,
  executes as a VALUES join, carries stale provenance, and cannot trigger a new join-region search.
  Evidence: `LmdbCorrelatedFilterPlacementOptimizer.materializeFiniteFilterValues` and
  `LmdbCascadesRuleProvider.materializeFiniteFilterValuesAfterCosting`.

- Observation: only binding assignment and LMDB value-ID resolution fit the intended final pre-import contract. Of
  the eager semantic passes, union scope removal, common-prefix factoring, and projection pushdown have the strongest
  current memo parity; constant/scalar folding, OPTIONAL normal form, eligibility, set semantics, generic filter
  placement, ORDER/LIMIT, and Explain repair still need granular memo contracts before their passes can be retired.
  Evidence: the milestone-6 pipeline parity audit against `LmdbQueryOptimizerPipeline`, `RewriteRuleCatalog`, and the
  focused legacy/memo tests.

- Observation: including the complete estimate-evidence vector in winner identity retains derivations that canonical
  cost ordering considers equal. Symmetric join subsets then multiply states which cannot affect selection.
  Evidence: `compareEqualNonObjectiveEvidenceSharesCanonicalState` was red in
  `logs/mvnf/20260713-192710-verify.log`; after canonicalization the focused contract passes and the exact
  eight-factor selector completes in 10.21 seconds (`logs/mvnf/20260713-194059-verify.log`).

- Observation: a queue key containing the full dependency revision and wake-event set deduplicates only identical
  historical work. When one expression advances repeatedly before its first task is consumed, every version remains
  live and exact search can exhaust the heap merely reconstructing a dequeue key.
  Evidence: the LMDB dump `2026-07-13T21-31-35_095-jvmRun1.dump` fails in
  `SearchState.pollExpressionWork`; the focused regression observed 25 attempts instead of two in
  `logs/mvnf/20260713-193706-verify.log`.

- Observation: identity-keyed coalescing fixes queue-version growth but cannot hide expression-space growth. The
  four-factor LMDB selector remained active for more than five minutes with 17,535 memo expressions, 16,470 pending
  expression identities, and 47,380 rule-ledger keys. Its live stack was filtering rules in `RuleRegistry.rulesFor`.
  Most Java LMDB rules inherit an all-events descriptor, so physical, winner, proof, and estimate changes can rerun
  logical matching and emit new concrete child combinations even when those dependencies are not read.
  Evidence: `jcmd 20157 Thread.print`, `GC.heap_info`, and `GC.class_histogram` captured during
  `logs/mvnf/20260713-194157-verify.log`; the run was stopped after five minutes rather than accepted as quiescent.

- Observation: narrowing rule wake dependencies bounds expression work but the recursive cost path still replays the
  full Cartesian product of retained child frontiers after every parent invalidation. Each replay appends the same
  dominated rejection and copies the entire group rejection history into candidate provenance.
  Evidence: the clean `logs/mvnf/20260713-195914-verify.log` run held only 454 `MemoExpr` and 601 rule-ledger keys but
  accumulated 611,401 `RejectedAlternative`, 2,297,134 immutable maps, and 4,493 winners before it was stopped at
  149.78 seconds in `SearchState.rejectionsFor -> enumerateInputWinnerCombinations`.

- Observation: statistical evidence must be a versioned group fact, not part of executable physical identity. Keeping
  finite relations in `PhysicalProperties` produced duplicate physical expressions and winner-frontier entries, while
  composite joins also inherited a child's access-path marker as if the parent delivered it.
  Evidence: the pre-fix `CascadesPhysicalFactChannelTest` had three failures; the post-fix 49-test selection stores
  exact finite relations in `MemoGroup.outputBindingProfile`, keeps executable profiles endpoint-only, and passes.

- Observation: the current evidence lattice safely accumulates disjoint finite-relation keys, but
  `BindingProfile.mergedWith` retains the first value for the same `VariableSetKey`. A later statistics-epoch contract
  must define whether same-key evidence is replacement, refinement, or conflict before claiming complete estimate
  refinement semantics.
  Evidence: `CascadesVersionedOutputFactTest` proves the disjoint `s`/`o` case; code inspection shows
  `mergedFinite.putIfAbsent` for equal keys.

- Observation: a possible-value domain and an exact finite bag are different optimizer facts. Reusing the exact
  relation channel for a domain lost values after a disconnected prefix; synthesizing one frequency per value then
  lost prefix multiplicity and incorrectly made a uniform assumption look exact.
  Evidence: `CascadesCostModelTest#parameterizedJoinCostRetainsFiniteDomainButNotExactRelationThroughDisconnectedPrefix`
  and `LmdbEstimationEngineTest#finiteBindingContextScalesConcreteLookupsByCompositePrefixMultiplicity` failed with a
  missing domain and 900 rows instead of 9,000 respectively.

- Observation: `CascadesPlanProvenanceAnnotator` used Java unary/binary arity rather than `MemoInputLayout`. A Filter
  with an EXISTS scalar input and an arbitrary-length path therefore stamped selected descendants as
  `covered_by_parent_winner` despite both children having real memo winners.
  Evidence: the scalar-subquery provenance regression received the parent decision for its outer child; typed slot
  traversal makes both it and the path-expression regression green.

- Observation: `seedExistingPlanWinner` bypassed `RuleApplication` validation by calling `Memo` directly with
  `opaque=true`, so an arbitrary composite could erase all child groups. LMDB generic implementation gates then made
  the escape hatch necessary for exactly the shapes whose specialized rewrites claimed ownership.
  Evidence: the focused Projection regression selected a silent fallback, direct Memo insertion accepted an atomic
  Join, and both MINUS/EXISTS eligibility regressions omitted `generic-physical-implementation` before the fix.

- Observation: parent-winner coverage was not merely Explain decoration. LMDB used the synthetic marker to prune
  subtree optimization and feedback traversal, while generic scalar subqueries were deliberately excluded from the
  planned-child check and optimized a second time.
  Evidence: `compositeProvenanceCannotHideOptimizableInputs` and
  `genericConditionSubqueryDoesNotBecomeSubtreeCandidate` failed before the writer/readers were removed.

- Observation: unsupported logical algebra used the physical `MATERIALIZE` IR operator with zero inputs even though
  the memo independently retained those inputs. Compiled rules therefore saw a different topology and operator kind
  from the scheduler, and rendering could not substitute a selected child.
  Evidence: the Reduced regression imported as inputless `MATERIALIZE`; it now imports as a scope-barrier
  `NATIVE_BOUNDARY` sharing `TupleExprInputCodec` with `MemoInputLayout`.

- Observation: a syntax-local correlated FILTER rewrite can offer one earlier placement yet still omit a legal
  placement opened by a different bushy topology. Predicate mobility therefore belongs in the join-state fixed point,
  not in one rewrite's traversal order.
  Evidence: `exactJoinSearchEnumeratesCorrelatedNotExistsAtEveryAssuredPrefix` initially retained `{arm}`,
  `{arm,guard}`, and `{arm,guard,tail}` but omitted `{arm,tail}`; the predicate-state closure now retains all four.

- Observation: a transparent parameterized composite RHS was priced as one standalone child when the store factor
  model could not estimate the whole composite. This made a bushy late-predicate plan look cheaper even though the
  runtime reopens that RHS once per outer row.
  Evidence: `parameterizedCompositeRightScalesSelectedChildWorkWhenNoFactorEstimateExists` expected 1,000 local work
  rows but observed 1 before generic repeated-composite ownership was added.

- Observation: contextual bound variables influenced leaf access estimates but were absent from delivered physical
  properties. Propagating them then exposed a second omission: scalar-subquery requirements were not discharged by
  the tuple argument that supplies their correlation.
  Evidence: the leaf contract test observed `[]` instead of `[kind]`; after that fix the LMDB regression correctly
  reported `NO_VIABLE_PHYSICAL_PLAN` until the typed `SCALAR_SUBQUERY` role consumed argument-assured bindings.

- Observation: IR import already marked `Reduced` and other unsupported roots as `NATIVE_BOUNDARY`, but memo input
  legality independently treated their argument edges as ordinary rewritable inputs. Scalar subqueries were likewise
  executable inputs without an explicit semantic barrier.
  Evidence: `nativeBoundaryParentMakesArgumentEdgeSemanticBarrier` observed `semanticBarrier=false` before the shared
  `TupleExprToIr.isNativeBoundary` classifier was introduced.

- Observation: an unknown zero-input tuple operator failed inside `EvaluationStatistics` before the optimizer could
  report the unsupported execution boundary, while the generic implementation rule would otherwise have treated any
  inputless object as executable. The atomic marker on `RuleApplication` was discarded after memo insertion.
  Evidence: `unknownInputlessOperatorReportsUnsupportedAtomicBoundary` first threw `Unhandled node type`; after
  heuristic native-boundary import, a persisted implementation form and a reachable-group quiescence audit are needed
  to distinguish an explicit atomic implementation from an accidental zero-child physical expression.

- Observation: bounded search drained the global rewrite/join queue before attempting to establish a root physical
  frontier. Predicate-aware join closure therefore consumed all 4,096 tasks, reached expression ID 10,605, and left
  the reported OPTIONAL with no child winners; root-first scheduling reduced the same search to 64 expressions.
  Evidence: `budgetedScopedUnionOptionalKeepsDecomposedOptionalAlternative` changed from no winner and 366 transient
  missing-input rejections to a retained root winner with one remaining transient attempt.

- Observation: an unavailable child frontier was recorded as `RejectedAlternative` and emitted through
  `alternativeDiscarded`, even though `Memo.addWinner` already emitted `CHILD_WINNER_CHANGED` for a later retry and
  `PendingOptimizationTask.COST_EXPRESSION` had no implementation. This made a temporary dependency look like a
  semantically invalid plan.
  Evidence: after root-first scheduling the OPTIONAL was later accepted, but its trace still contained an earlier
  `missing-input-winner` discard until dependency-indexed cost deferral was added.

- Observation: the post-Cascades correlated-placement pass was not decorative. It could replace a selected Filter
  with Difference, push NOT EXISTS into a hand-built left-deep prefix, and inject planned metrics after costing.
  Evidence: the pipeline ownership regression failed until the class and registration were deleted; the direct
  Cascades correlated-prefix regression was already green without it.

- Observation: arbitrary-length paths already exposed their step expression as an executable `PATH_EXPRESSION` memo
  input, but its semantic barrier came only from classifying the parent as an unsupported native boundary. Making the
  parent typed without replacing that implicit condition would silently allow generic rewrites to cross path scope.
  Evidence: the IR red imported `ZeroLengthPath` as `NATIVE_BOUNDARY`; the memo contract now requires an executable,
  independently optimized path input whose `semanticBarrier` remains true after native classification is removed.

- Observation: semantic scope existed only as a proof annotation and a string on the root goal. Child goals forwarded
  that string unchanged, while structural duplicate insertion merged proof lists without recording which observers
  could legally consume the route. Consequently, a shared expression below both bag-sensitive and `Distinct`
  consumers could neither activate a `SET`-only rewrite for the latter nor reject that route for the former.
  Evidence: the focused regression expected the `SET` rewrite to fire once under `Distinct` but observed zero
  applications before typed input-goal propagation and memo-resident admissibility were added.

- Observation: the broadened `CascadesRuleDependencyRevisionTest` and join-region contract selection currently
  exposes four failures outside semantic-scope routing: three assertions retain the pre-frontier scheduler order, and
  one join test supplies a root bound-input requirement without a viable producer. All expressions in that join case
  carry the original `BAG` route, while the complete 123-test scheduler and 41-test memo suites remain green.
  Evidence: workspace `semantic-scope-contracts`; its completeness class passed all 6 tests, while the four named
  failures reproduce scheduler-order/root-goal drift rather than an admissibility rejection.

- Observation: semantic observation is an edge contract, not a property inferable only from a parent's Java class.
  The same child group may be consumed by both a bag-sensitive parent and a duplicate-insensitive aggregate, and a
  physical implementation introduced after import must retain the same requirement as its logical source.
  Evidence: before `MemoInput.requiredSemanticScope` became structural identity, the focused Group regression saw
  zero `SET` rule applications; the six-test semantic-scope suite now covers logical, physical, shared-consumer, and
  scalar-subquery propagation.

- Observation: a finite-membership rewrite below a no-key distinct aggregate is only locally `SET`-safe. Treating the
  replacement child as globally bag-equivalent leaks it to ordinary consumers, while placing only a rewritten whole
  tree in another child group prevents later child alternatives from waking it.
  Evidence: the focused LMDB tests require the membership child to be rejected by a `BAG` goal, the containing Group
  to remain `BAG`-safe, and a late child alternative to reactivate the rule.

- Observation: RDF4J's `Filter.getBindingNames()` includes scalar-subquery-local variables for a direct `Exists`
  condition but loses them after `nested-filter-merge` wraps that same condition in `And`. Raw algebra binding-name
  access therefore makes equivalent filter rewrites appear to change their stream schema.
  Evidence: the attributed pipeline failure named `nested-filter-merge`, expression 247, source and target group 46;
  the new scalar-subquery schema regression now passes without modifying the merge rule.

- Observation: finite-membership legality cannot be established from possible names or `Filter` scalar expressions
  alone. Nullable counted correlations must be assured, and an `Extension`/`BIND` in the remaining subtree may read a
  probe-only variable even when no filter does.
  Evidence: the three safety regressions initially failed for the nullable correlation, observed `copiedCode` BIND,
  and an incorrectly attached child proof; all pass after using assured stream facts and typed opaque-factor inputs.

- Observation: the memo currently validates canonical stream schema only for logical alternatives. A malformed
  physical alternative can still bypass the same contract, while a logical alternative with invalid semantic edge
  requirements may intern child groups before later validation rejects it. Distinct finite membership also rewrites
  one eligible membership per emitted alternative rather than saturating multiple memberships in one expression.
  Evidence: source audit of `Memo.addPhysicalAlternative`, `Memo.addLogicalAlternativeWithInputs`, and
  `LmdbDistinctFiniteMembershipRule.apply`; these are recorded follow-ons rather than widened fixes in this slice.

- Observation: after canonical schema validation removed six memo insertion errors, the fallback-policy pipeline
  selector retained one meaningful red. Both `lmdb-materialized-exists-semi` and `lmdb-distinct-exists-join` are legal
  and present in provenance, but the direct observation join wins and the selected tree has no `Exists` node.
  Evidence: `LmdbOptimizerPipelineTest.java:1077` fails `assertTrue(containsExists(tupleExpr), diagnosticPlan)`; the
  six-selector run reports 6 tests, 1 failure, 0 errors. This is a local-cost/winner-contract follow-on, not a schema
  legality failure, and must not be hidden by a rule-ID or plan-shape dominance exception.

- Observation: optimizer test rules can violate memo equivalence if they use raw `TupleExpr.getBindingNames()` to
  construct a projected group and then inject an unprojected child. Constant-valued predicate/type variables appear
  in the raw set but are not tuple-stream outputs, so the canonical guard correctly rejects that fixture.
  Evidence: `lateNestedUnionChildReactivatesExactCodeTypeObserver` reported child bindings
  `[code, entity, target]` versus raw projected bindings `[branchType, code, codePredicate, entity, target,
  typePredicate]`; migrating the fixture and its assertions to `StreamBindingSchema` restored 6/0/0 without changing
  the root-only production rewrite or moving its proof onto the child.

- Observation: canonical logical-group validation alone does not stop a specialized physical rule from advertising a
  scalar-subquery local as a delivered output. Before the shared winner clamp, a deliberately malformed Filter
  implementation won with `[probeLocal, probePredicate, probeValue]` in its output contract and could satisfy a goal
  that required `probeLocal`.
  Evidence: `CascadesRuleEngineTest#specializedPhysicalRuleCannotClaimScalarSubqueryLocalOutput` failed before the
  change and passes after `CascadesPlanner` restricts every delivered `PhysicalProperties` value to the memo group's
  canonical `BindingShape.possible` mask.

- Observation: a repeated variable name inside a scalar subquery is not intrinsically local or correlated; its
  meaning depends on whether an earlier join occurrence has already supplied that name. Treating the full syntax tree
  as one unordered set either creates false DPhyp connectivity or permits a reorder that changes query results.
  Evidence: the paired extractor regression initially produced no dependency for local-first order and accepted the
  correlated producer-first order. The real LMDB regression returned only `good` instead of `[bad, good]` for the
  local-first query. Both direct and `And`-wrapped EXISTS forms now preserve the two distinct execution results.

- Observation: the P0 legality/connectivity cut does not resolve the broader winner/provenance contract. MEDICAL q4
  and `LmdbUnifiedJoinRoutingTest` both show saturated join regions and contributed DPhyp/exhaustive alternatives, but
  a generic/access-path tree wins without the expected join-search provenance. The full core rule class also retains
  two independently tracked branch reds: one memo binding invariant and one redundant-projection winner assertion.
  Evidence: workspace `stream-schema-p0` logs `20260714T100425.410484Z-21661-1faaa9f8`,
  `20260714T101309.501785Z-61291-bcc2f7bc`, and `20260714T101512.077524Z-67908-bd4b1c78`. These failures are preserved;
  no rule-ID, query-shape, or winner-preference exception was added.

- Observation: a child estimate cache keyed only by output row count can recover a rich finite relation from an
  unrelated binding context. Equal cardinality is not evidence equivalence, so the selected winner's delivered
  binding profile is the only valid source of reusable child evidence.
  Evidence: `CascadesCostModelTest#winnerBagDoesNotRecoverEqualRowEvidenceFromAnotherInputContext` initially recovered
  context A's finite relation while costing context B; it passes after row-equality recovery was removed.

- Observation: materialized MINUS is adaptive at execution time. Once its distinct RHS set reaches the configured
  cap, it abandons the set and reopens the selected RHS for every left row. Linear build-plus-probe costing therefore
  underprices precisely the large anti-join case where the fallback activates.
  Evidence: `materializedMinusOverflowPricesRepeatedRhsReopenings` observed 8 local work rows instead of 408 before
  costing shared the runtime limit and charged selected-RHS reopening work.

## Decision Log

- Decision: production defaults to strict complete search; bounded search remains explicit and visibly incomplete.
  Rationale: the user's primary requirement is that routing and staging never silently omit a registered optimization.
  Date/Author: 2026-07-13 / Håvard and Codex.

- Decision: build the generic engine in queryalgebra evaluation and make LMDB the first complete adopter.
  Rationale: keeping the scheduler in LMDB would preserve the architectural split the change is intended to remove.
  Date/Author: 2026-07-13 / Håvard and Codex.

- Decision: retain the legacy pipeline only for temporary shadow comparison, then delete production fallback routing.
  Rationale: parallel comparison reduces migration risk without leaving two planners that can disagree after cutover.
  Date/Author: 2026-07-13 / Håvard and Codex.

- Decision: supersede the prior "DPhyp owns an island" contract. Join algorithms contribute candidates through one
  service and cannot suppress generic legal alternatives.
  Rationale: ownership is the direct reason a specialized implementation can block a rewrite or later reorder.
  Date/Author: 2026-07-13 / Codex.

- Decision: use memo-local immutable operator expressions, not whole `PlanIr` instances or mutable `TupleExpr` trees,
  as the canonical memo representation.
  Rationale: a local expression can reference child equivalence groups directly, so newly added child alternatives are
  visible to parent matching and costing without cloning an entire tree.
  Date/Author: 2026-07-13 / Codex.

- Decision: rules provide legality and physical operator descriptions; only the planner composes total costs.
  Rationale: a rule-level whole-subtree cost becomes stale as soon as a child winner changes and caused the reported
  opaque MINUS accounting failure.
  Date/Author: 2026-07-13 / Codex.

- Decision: treat phases as priorities, never barriers, and use versioned wake-ups plus deterministic queue order.
  Rationale: priorities retain efficient search order while exact mode still drains all eligible work to quiescence.
  Date/Author: 2026-07-13 / Codex.

- Decision: model semantic observation strength as the typed lattice `BAG` > `SET` > `EXISTENCE`, while storing on
  each memo expression the strongest observer for which its derivation is legal. Original imports are `BAG`-safe;
  rule output combines the source route with every proof requirement, and identical structures merge toward the
  least restrictive available route while preserving every proof.
  Rationale: observer goals and derivation admissibility are dual contracts. Keeping both typed prevents a specialized
  rewrite from leaking into a stronger consumer without suppressing a generic route usable by all consumers.
  Date/Author: 2026-07-14 / Codex.

- Decision: semantic requirements are ordered typed memo-edge data and participate in logical and physical structural
  identity. The shared input codec assigns the contract, rules must preserve or deliberately replace it, and the
  planner derives each child goal by combining that edge contract with the inherited observer.
  Rationale: central edge routing covers every implementation and specialized rule uniformly; parent-specific planner
  branches cannot be complete and lose the contract when an equivalent physical expression is introduced later.
  Date/Author: 2026-07-14 / Codex.

- Decision: finite-membership normalization under a duplicate-insensitive aggregate emits two explicit proofs: the
  local child alternative is admissible only for `SET`, while the complete no-key all-`COUNT(DISTINCT)` Group is
  admissible for `BAG`. Both are inserted into existing equivalence groups and remain transparent.
  Rationale: this preserves ordinary bag multiplicity, lets every generic child optimization coexist, and lets child
  expression/fact revisions legitimately rerun the specialized rule without recursive tree ownership.
  Date/Author: 2026-07-14 / Codex.

- Decision: canonical stream outputs are derived by the public experimental immutable
  `StreamBindingSchema(possible, assured)` contract, not by operator-local calls to raw RDF4J binding-name methods.
  Scalar subquery locals are inputs, FILTER is stream-transparent, binary operators apply explicit scope semantics,
  and compatibility helpers delegate to this one implementation during migration.
  Rationale: memo equivalence, rule legality, and costing must observe one stable schema before and after rewrites;
  otherwise a scalar condition's syntax can spuriously split or merge equivalence groups.
  Date/Author: 2026-07-14 / Codex.

- Decision: the planner restricts every physical alternative's delivered output-scoped properties and facts to the
  owning memo group's canonical possible-binding mask before admissibility, fact merging, and winner comparison.
  Input requirements, access-path mode, materialization, duplicate behavior, graph context, and observation order stay
  intact because they describe execution contracts rather than tuple outputs.
  Rationale: specialized rules may declare stronger legal properties, but no rule is allowed to enlarge an
  equivalence group's stream schema. Enforcing this once at the shared winner seam covers generic and store-specific
  implementations without adding per-rule gates.
  Date/Author: 2026-07-14 / Codex.

- Decision: scalar same-name order safety is represented by occurrence identity, not by variable-name connectivity.
  A later producer depends on the earlier scalar factor, preventing retroactive correlation; an already correlated
  scalar factor remains an explicit unsupported semantic boundary until its input contract can be represented without
  changing evaluation order. Join contributors consume descriptor binding facts and `JoinRegion.requiredBefore`
  constraints and must not infer extra edges from tuple templates.
  Rationale: the same syntax has different legal reorderings at different occurrences. Directional dependencies and
  explicit boundaries preserve that distinction while still allowing all safe generic and specialized alternatives.
  Date/Author: 2026-07-14 / Codex.

- Decision: a finite-membership child's local SET-safe rewrite carries no bag-equivalence annotation. Only the
  complete no-key all-`COUNT(DISTINCT)` Group receives the `fullGroupBagEquivalent` proof, and all counted
  correlations must be assured by the non-probe remainder before the rewrite is legal.
  Rationale: proof scope is part of route safety. Decorating the child lets an unrelated BAG observer consume a
  derivation whose duplicate changes are hidden only by its original aggregate parent.
  Date/Author: 2026-07-14 / Codex.

- Decision: physical input enumeration may recurse to the same group only under a distinct winner goal; consuming an
  in-progress identical `WinnerKey` is a cyclic physical derivation and is not a legal winner candidate.
  Rationale: memo equivalence cycles are useful for rewrites, but executable plans must be finite. This goal-keyed guard
  permits finite-domain binding to strengthen a child goal while preventing a frontier from recursively pricing itself.
  Date/Author: 2026-07-13 / Codex.

- Decision: model tuple, scalar-subquery, path-expression, and unknown structural inputs through one typed memo-input
  contract, with materialization and bound-input requirements carried per slot.
  Rationale: special expression shapes must not become invisible scheduling or costing boundaries.
  Date/Author: 2026-07-13 / Codex.

- Decision: seed the normalized existing algebra as an ordinary root memo candidate before rule work, then remove the
  LMDB ThreadLocal standard-plan owner and its policy arbitration.
  Rationale: this gives budgeted search a safe executable candidate while keeping all alternatives under one cost and
  winner contract; it is a migration baseline, not a second planner or a complete-mode shortcut.
  Date/Author: 2026-07-13 / Codex.

- Decision: supersede the existing-algebra seeding decision. Supported algebra is implemented transparently by
  registered physical rules; if no implementation satisfies the goal, the memo returns no winner and reports
  `NO_VIABLE_PHYSICAL_PLAN`. A whole-tree clone is never an atomic or emergency physical implementation.
  Rationale: silent seeding erased child groups, bypassed validation, hid missing implementations, and preserved the
  very planner ownership boundary this migration removes. Budgeted execution must expose incompleteness rather than
  invent an uncosted winner.
  Date/Author: 2026-07-14 / Codex.

- Decision: keep finite domains, exact finite relations, and execution counts as separate evidence. Domain values
  select legal concrete probes; exact relation frequencies describe bag multiplicity; the prefix row count describes
  nested-loop executions unless a physical cache contract explicitly says otherwise.
  Rationale: distinct lookup values cannot reduce output replay or RHS opens in the current iterator implementation,
  and an inferred distribution must retain uncertainty instead of being promoted to exact storage evidence.
  Date/Author: 2026-07-14 / Codex.

- Decision: selected-plan provenance arity is a planner invariant, not a telemetry fallback opportunity. A composite
  without one provenance input per typed child is rejected; no parent estimate is copied onto hidden descendants and
  no downstream optimizer may route on a coverage marker.
  Rationale: synthetic descendant costs hide incomplete memo topology and can suppress both generic optimization and
  independent runtime feedback.
  Date/Author: 2026-07-14 / Codex.

- Decision: `NATIVE_BOUNDARY` represents a lossless but currently unsupported logical shell with transparent child
  groups and an explicit semantic barrier. `MATERIALIZE` is reserved for a physical property enforcer.
  Rationale: unsupported syntax must limit rewrites across its boundary without erasing child optimization or
  masquerading as an executable leaf.
  Date/Author: 2026-07-14 / Codex.

- Decision: aggregate syntax is immutable typed IR knowledge, not a binding-name annotation. `AggregateBinding`
  carries a finite core kind, custom function IRI, ordered scalar arguments, optional GROUP_CONCAT separator, and
  DISTINCT. Unknown aggregate subclasses use the explicit native boundary until a structural kind is registered.
  Rationale: cardinality, duplicate, ordering, and aggregate rewrites cannot be complete or safely costed when import
  drops the executable aggregate expression.
  Date/Author: 2026-07-14 / Codex.

- Decision: property-path endpoints and traversal parameters are immutable typed IR attributes, while an
  arbitrary-length path's step expression is one transparent executable child. Both path operators remain explicit
  semantic barriers; specialization may optimize the child but cannot flatten it into the surrounding join region.
  Rationale: treating a path as native hides its operator/facts, while treating its step as an ordinary unary stream
  changes correlation and traversal scope. Typed attributes plus a barrier preserve both optimization and legality.
  Date/Author: 2026-07-14 / Codex.

- Decision: an exact `Service` owns immutable endpoint/query metadata and one transparent definition input whose
  structural fingerprint must stay synchronized with the stored source text. That input is dependency-visible but
  routed through `REMOTE_DEFINITION`, never local rule, join, implementation, or cost work. Exact `TripleRef` is a
  typed RDF-star leaf whose nonconstant terms determine assured bindings; subclasses such as `ReifiedTripleRef`
  remain native until every additional semantic term has an explicit IR field.
  Rationale: hiding a SERVICE body loses dependency topology, while optimizing it as a local subtree changes remote
  query semantics. Widening either class match would silently discard subclass state and produce a falsely lossless
  plan.
  Date/Author: 2026-07-14 / Codex.

- Decision: legacy standard-plan policies are temporary no-winner fallback aliases only. They cannot bypass search,
  bound the canonical cost frontier, compare against a legal winner, or trigger mutation of an extracted winner.
  Rationale: any of those routes reintroduces a second planner/cost contract and lets phase ownership hide registered
  alternatives; fallback retirement can now proceed as an explicit missing-implementation migration.
  Date/Author: 2026-07-14 / Codex.

- Decision: join enumeration is group-level work keyed by canonical occurrence region, goal, statistics epoch, and
  factor revisions; it is not an ordinary expression transformation rule.
  Rationale: this permits legitimate fact/access-path/winner wake-ups while candidate insertion itself remains
  idempotent and cannot create recursive ownership.
  Date/Author: 2026-07-13 / Codex.

- Decision: the unified join service has an enumeration-only production path. Every validated candidate is installed
  into transparent memo groups, and only the Cascades local-cost composer may select and price physical winners.
  Rationale: a pre-installation scalar join rank would be a second cost model without independently selected children.
  Date/Author: 2026-07-13 / Codex.

- Decision: region and wake-up identity will be typed structural values, never widened hashes or diagnostic strings.
  A join task reads an immutable region key plus an ordered revision read-set covering every source, factor,
  predicate, barrier, property, and statistics fact on which legality or costing depended.
  Rationale: hash collisions and omitted intermediate revisions are indistinguishable from saturation and therefore
  violate the no-missed-wake-up guarantee.
  Date/Author: 2026-07-13 / Codex.

- Decision: complete join exploration will install unique subset-partition expressions rather than retaining every
  expanded full tree. Complete binary trees remain representable implicitly through child equivalence groups.
  Rationale: this is the Cascades memo representation itself, removes the arbitrary eight-factor production cap, and
  changes exhaustive search from full-tree explosion to finite dynamic-programming states.
  Date/Author: 2026-07-13 / Codex.

- Decision: memo change kinds are part of rule routing, before `matches` is called. Logical and physical expression
  additions are distinct wake-up events; rules with explicit descriptors receive only declared events, while legacy
  rules without event metadata conservatively subscribe to all events.
  Rationale: the application ledger prevents cycles but cannot prevent expensive or side-effectful matching work, and
  a physical implementation cannot make a logical-only rewrite newly match.
  Date/Author: 2026-07-13 / Codex.

- Decision: legacy join commute/association specifications remain available as independently tested legality
  definitions but are not registered as production expression rules after `JoinSearchService` is present.
  Rationale: the unified service already installs exactly those legal partitions; re-expanding them as concrete trees
  duplicates work rather than contributing a distinct rewrite, obscures memo sharing, and caused the eight-factor OOM.
  Date/Author: 2026-07-13 / Codex.

- Decision: scheduler quiescence and optimizer completeness are distinct from plan viability. An empty root winner is
  reported as `NO_VIABLE_PHYSICAL_PLAN`, never `COMPLETE`, even when no task remains queued.
  Rationale: pending work would be fabricated in this state, while a precise terminal status exposes the actual
  optimizer outcome without misclassifying it as an unsupported atomic boundary.
  Date/Author: 2026-07-13 / Codex.

- Decision: the pending expression queue has one stable position per `(task kind, expression ID)`. Later changes merge
  component-wise revisions and union wake events into that position; the application ledger still decides whether an
  individual rule has already consumed the relevant revision.
  Rationale: fixed-point search needs the newest state and every cause, not a FIFO replay of states which the monotonic
  memo has already superseded. This preserves legitimate reruns while bounding queue occupancy.
  Date/Author: 2026-07-13 / Codex.

- Decision: physical costing has its own application ledger keyed by expression, complete goal, immutable statistics
  epoch, expression/child fact revisions, and selected child winner states. A newly retained child state creates only the new
  Cartesian tuples that contain it; unrelated memo wake-ups do not reprice old tuples. Rejection provenance stores a
  bounded canonical diagnostic per rule/reason/property class while telemetry may count every rejection.
  Rationale: cost fixed points need the same dependency-keyed idempotence as rules. Replaying an unchanged tuple adds
  no legal alternative and made exact search non-quiescent through diagnostic allocation rather than optimizer work.
  Date/Author: 2026-07-13 / Codex.

- Decision: logical join state is `(factor occurrences, applied predicates)`, and exact search closes predicate
  applications and topology transitions to a fixed point. Specialized topology contributors cannot own predicate
  placement or suppress a legal state.
  Rationale: predicate legality changes with assured bindings, and a new topology can make a previously inapplicable
  predicate legal after the syntax-local rewrite phase would already have finished.
  Date/Author: 2026-07-14 / Codex.

- Decision: any physical leaf costed with referenced contextual bindings exposes those bindings as required inputs.
  Parents discharge requirements according to typed memo-input roles; a parameterized composite that remains on a
  JOIN RHS is priced per outer execution unless an explicit store estimate or materialization contract owns that work.
  Rationale: contextual cost without an executable property contract underprices bushy plans and lets required
  correlation escape or disappear depending on tree shape.
  Date/Author: 2026-07-14 / Codex.

- Decision: IR support classification is a shared legality contract. An edge is a semantic barrier when either endpoint
  is a native boundary, when it crosses variable scope, or when its typed role is structural, scalar-subquery, or
  definition; executable children remain independently optimized behind the barrier.
  Rationale: unsupported or specialized syntax may restrict cross-boundary equivalences but must never erase generic
  child exploration, costing, or provenance.
  Date/Author: 2026-07-14 / Codex.

- Decision: physical memo expressions carry `LOGICAL`, `TRANSPARENT`, or `ATOMIC` form in structural identity. Exact
  quiescence audits every reachable local inputless boundary, and only an explicitly atomic physical alternative can
  close a non-standard leaf; a heuristic cardinality estimate never grants executability.
  Rationale: child count alone cannot prove that an evaluator implements an operator, and a rule ID or proof string is
  not a semantic execution contract.
  Date/Author: 2026-07-14 / Codex.

- Decision: costing is first-class scheduled work. A physical expression whose child frontier is not yet available
  enters a deduplicated `COST_EXPRESSION` queue keyed by expression, complete goal, statistics epoch, and relevant
  group revisions; reverse child-winner changes wake it. Exact quiescence finalizes permanently infeasible work,
  while bounded termination exposes every still-deferred cost task.
  Rationale: dependency absence is neither a legal rejection nor permission for another planner to take ownership;
  it is precisely the reactivation case the unified scheduler must represent.
  Date/Author: 2026-07-14 / Codex.

- Decision: extracted memo winners are immutable inputs to execution and Explain. Correlated anti/semi placement may
  add logical or physical alternatives inside the memo but no optimizer may rewrite the selected tree afterward.
  Rationale: a post-cost tree mutation bypasses child selection, canonical costs, completeness, and wake-up routing.
  Date/Author: 2026-07-14 / Codex.

- Decision: input binding context is canonical dependency identity, not transient recursion state. It includes exact
  finite relations and sparse correlations while ignoring source labels and column order; goals, winners, rule and
  cost ledgers, and statistics caches all use the same value.
  Rationale: a selected finite prefix can change legal access paths, cardinality, and total work of every dependent
  child. Omitting it either reuses an invalid winner or recursively reprices syntax outside the memo.
  Date/Author: 2026-07-14 / Codex.

- Decision: adaptive MINUS costing reads the execution step's `@InternalUseOnly` materialization-limit resolver.
  Overflow cost is selected child work per left reopening plus local build/probe work; memory is limited by the same
  cap. The default and property parser are not duplicated in the optimizer.
  Rationale: execution-policy drift would make canonical costs select a plan whose real runtime takes a different
  algorithmic path, while an internal resolver shares the contract without adding a stable public API.
  Date/Author: 2026-07-14 / Codex.

## Outcomes & Retrospective

The generic scheduler, local cost composer, and first exact join-region lane are operational. Memo mutations, including
proof-only knowledge changes, carry revisions and wake reverse dependants; rules are indexed and uniquely identified;
exact exploration reactivates until all expression and region queues quiesce; and plans expose structured completeness.
Join factors preserve occurrence identity, legal candidates materialize through shared subset groups, and physical
joins retain independently selected children. Typed event routing now prevents physical additions from rematching
logical-only rules, and LMDB no longer grants its legacy hypergraph rule exclusive ownership. Focused red-to-green
evidence is retained in `initial-evidence.txt`. Canonical frontier identity and identity-coalesced pending work now
bound the core exact oracle, and production LMDB routes both DPhyp and exhaustive search through the same service.
Remaining critical work is to make every LMDB rule dependency explicit (the current all-events legacy descriptors
still generate a non-quiescent physical expression space), migrate the audited semantic passes—especially post-cost
finite VALUES materialization—remove standard-plan/fallback arbitration, and run the semantic/performance acceptance
matrix.

The memo now also carries semantic observation strength on each typed input edge. The production distinct finite-
membership route is memo-local and revision-reactivated: focused core scope/model/scheduler suites pass 6, 42, and 125
tests, and the LMDB membership suite passes 4 tests. The complete 44-test pipeline leaves only three already-routed
follow-ons concerning finite type anchors and eligibility/common-prefix lifting; the migrated membership and Exists
selectors are green. Full commands, report paths, and snippets are retained in
`initial-evidence.set-membership-final.txt`.

The P0 stream-schema routing slice now protects physical winners as well as logical memo insertion. Specialized
implementations cannot manufacture scalar-subquery outputs, DPhyp connectivity uses canonical descriptor facts, and
same-name scalar references retain written occurrence semantics. Focused red-to-green evidence is retained in
`initial-evidence.stream-schema-p0.txt`; the core pair passes 2 tests and the LMDB trio passes 3 tests including real
store execution. Neighboring LMDB legality/connectivity classes pass 74 of 75 tests; the single broader routing red and
the MEDICAL q4 plan-order red both saturate join alternatives but select generic provenance, so they remain cost/winner
follow-ons rather than being hidden in this slice.

The medical-q1 cost-contract slice is now green. Selected finite prefixes are canonical memo context, materialized
EXISTS/MINUS retain independently optimized children, semi-join cardinality preserves left bag semantics, and
context-specific evidence cannot leak through a row-count cache shortcut. Adaptive MINUS overflow is priced using the
runtime's own internal cap. The complete core cost-model and LMDB optimizer classes pass 69 and 55 tests; the exact
medical pipeline, selected-context/local-cost trio, and finite-membership class pass 1, 3, and 9 tests respectively.
JDK 26 plan/runtime acceptance remains part of the final matrix and is intentionally run by the benchmark owner.

## Context and Orientation

The generic optimizer lives under
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/`.
`CascadesPlanner` currently performs recursive top-down search. A `Memo` holds `MemoGroup` equivalence classes, and a
`MemoExpr` contains a complete mutable `TupleExpr`, child-group IDs, physical properties, costs, and rule proofs.
`RuleRegistry` selects `CascadesRule` instances. `RuleApplication` adds logical or physical alternatives and contains an
`opaque` flag. `CascadesCostModel` estimates expressions, while `Winner` and `WinnerKey` store selected physical plans.

The typed planner IR lives in the `cascades/ir/` subpackage. `PlanIr` is a query-local graph of immutable `IrNode`
records using `IrOp` and `IrAttr`. Today this representation is primarily a per-rule conversion format. The target memo
expression reuses its operator and attribute vocabulary but stores child memo-group IDs rather than node IDs from a
whole query graph. `PlanIr` remains useful for import/export and codec tests during migration.

LMDB optimizer integration lives under
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. `LmdbQueryOptimizerPipeline` currently runs roughly twenty
standard and LMDB-specific optimizers before `LmdbCascadesOptimizer`, then runs correlated-filter placement,
order/limit, and Explain passes afterward. `LmdbCascadesRuleProvider` registers standard and LMDB rules but also wraps
generic join rules with `LmdbOwnedJoinEnumerationRuleGuard`. `LmdbConnectedHypergraphJoinImplementationRule` emits an
opaque complete join tree from `LmdbCascadesConnectedJoinPlanner`, whose DPhyp-enabled path lives in
`LmdbHypergraphJoinPlanner` and `org.eclipse.rdf4j.sail.lmdb.hypergraph`.

A memo group is a set of expressions proven to have the same query-result semantics. A memo fact is immutable knowledge
about that group, such as assured bindings or an exact finite domain. A memo revision is a monotonically increasing
number changed whenever expressions, facts, estimates, or winner frontiers change. A reverse dependency maps a child
group to parent expressions that consume it. A work item is a deterministic queued request to explore a rule, implement
an expression, enumerate a join region, or cost a physical alternative. Quiescence means all eligible queues are empty
for the current goal and statistics snapshot.

A join region is a maximal set of inner-join factors that may be reordered together, plus dependency edges that encode
correlation, OPTIONAL/MINUS constraints, property-path endpoint requirements, and scope barriers. A physical property
describes execution requirements such as incoming bound variables, ordering, materialization, duplicate behavior, and
row goals. An atomic physical implementation is a true executable leaf with no separable child query plan; composite
operators must always expose child memo groups.

## Plan of Work

Milestone 1 establishes failing invariants before replacing the scheduler. Add focused tests to
`CascadesRuleEngineTest` proving that a rule which initially returns no output fires again after a dependent group fact
or child expression changes, that a new child winner can replace a parent winner, and that registry order does not alter
the exact saturated expression set. Add a `RuleRegistry` test requiring unique IDs. Construct tiny test-only rules and
cost models so failures identify scheduler behavior rather than LMDB estimates. Observe and retain the current failures.

Milestone 2 introduces the canonical memo expression and change model additively. Define an immutable expression
specification containing `IrOp`, `IrAttr`, child-group IDs, expression kind, a backend-neutral physical implementation
descriptor, delivered properties, per-child required properties, proofs, and annotations. Add `MemoChange`, per-group
expression/fact/winner revisions, a statistics epoch, and reverse parent-use indexes. Every mutation returns its change
set. Keep a cached `TupleExpr` bridge for existing rules and statistics providers until migration is complete. Expand
the IR codec so unsupported structural unary/binary nodes retain children, split `NATIVE` from `MATERIALIZE`, model
EXISTS/NOT EXISTS subqueries as group inputs, and preserve aggregates, paths, SERVICE, scope, duplicates, and metadata.

Milestone 3 replaces recursive one-shot exploration with a deterministic indexed scheduler. Define rule descriptors
that list root operators, memo facts and child properties read, changes produced, wake-up events, priority, and one of
the accepted convergence classes: monotonic fact, canonical reduction, or finite equivalence expansion. Build operator-
indexed rule buckets and `ArrayDeque` queues for exploration, implementation, join enumeration, and costing. Use dense
integer IDs and compact revision keys. A work-key includes rule/expression/goal identity and only the revisions declared
by that rule. Memo changes enqueue affected rules and parent tasks. Complete mode drains every queue; bounded mode uses
a deterministic task budget and returns pending work. Add `OptimizationCompleteness` and structured pending-work
diagnostics to `CascadesPlan` while retaining deprecated forwarding accessors where compatibility requires them.

Milestone 4 establishes one physical and cost contract. Replace `RuleApplication.opaque` with transparent expression
specifications and a narrowly validated atomic-leaf form. Make required child properties executable instead of metadata.
Split cardinality/evidence estimation from local operator cost. A physical rule emits an operator and requirements but
does not precompute child work. The planner selects each child frontier, calculates local output rows and local work,
and composes total work, I/O, memory, materialization, and uncertainty exactly once. Winner changes increment a frontier
revision and requeue parents. Migrate materialized MINUS/EXISTS, OPTIONAL, row-preserving wrappers, access paths,
multi-predicate star scans, and generic operators. Remove baseline/emergency/finite-anchor shape exceptions from winner
dominance once invariant tests show ordinary costs select the right candidate.

Milestone 5 introduces a first-class join-region service. Normalize maximal legal inner joins into a memo expression
containing factor-group IDs, predicates, total-eligibility constraints, and required inputs. Define one
`JoinSearchService`; adapt exact DP, DPhyp, branch-and-bound, and greedy search as contributors. In complete mode an
exhaustive legal enumerator remains available and all applicable exact work reaches completion. Specialized algorithms
may reuse or accelerate states but never suppress the base candidate space. Enumeration emits transparent physical join
expressions and intermediate subset groups for bushy trees. Changes to factor membership, exact finite domains, assured
bindings, correlations, access paths, required properties, or estimates increment the region revision and resume
enumeration. Remove DPhyp ownership guards and make `dphyp=false` select another unified contributor.

Milestone 6 migrates the optimizer pipeline. Convert constant and filter simplification, projection and set-semantics
rewrites, eligibility rewrites, correlated filter placement, finite `VALUES` construction, ORDER/LIMIT handling,
subquery optimization, and semantic repairs into independent memo rules with explicit dependencies. Initial bindings
and LMDB value IDs enter as importer facts rather than destructive planner passes. Serializable observation order is a
required semantic/physical property. The final production pipeline contains import, the unified optimizer, one winner
extraction, and non-mutating Explain/provenance decoration. Provenance describes the actual selected child lineage and
never controls further traversal.

Milestone 7 performs shadow validation and removal. During migration, optimize cloned input with the legacy pipeline
only for tests and diagnostic shadow comparisons. Compare results, canonical costs, structure, optimizer time,
allocation, expression counts, and task counts. After semantic and performance gates pass, remove standard-plan
arbitration, subtree repair/replanning, `cascadesCoveredByWinner`, `LmdbOwnedJoinEnumerationRuleGuard`,
`standardLogicalRuleParity`, `standardPlanPolicy`, opaque composite handling, and post-Cascades mutation. Legacy
`auto`/`exact` modes map to complete; budgeted modes map to bounded; an algorithm kill switch changes contributor
selection and never routes to another planner.

## Concrete Steps

Run every command from the repository root. Never use `-am` or `-q` with tests, and always use the workspace-local
`.m2_repo`. Preserve untracked artifacts and user-owned benchmark modifications.

Before each focused test session, prefer the repository test runner, which performs the required root quick install:

    python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --retain-logs

Run the smallest new method first, then the whole core class. For LMDB-focused milestones use:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesContextPropagationTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesConnectedRuleAdmissibilityTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRuleRegistryCoverageTest --retain-logs

Run complete affected modules only after focused selectors are green:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

For persistent theme-regression iteration append:

    -- -Drdf4j.lmdb.themeRegression.persistentStore.enabled=true

Capture and compare the reported medical plan through the repository query-plan snapshot wrapper using identical query
IDs and `structure+estimates` diff mode. Keep logs under `/tmp` and record the exact commands and summaries here.

Use JDK 26 for the final plan/run matrix and JMH verification. Run the complete eleven-query by two-algorithm matrix
with unified statistics, require twenty-two result rows and no `<failure>` entries, and retain the result text. Use
`scripts/run-single-benchmark.sh` for focused repeatable measurements. If optimizer time or allocation regresses,
profile before attributing the cause to the JVM.

Before final verification, run the copyright checker, format touched source files through Maven, rerun focused and
module tests, and audit `git diff --check`, `git status --short`, and the complete diff. Do not let the formatter alter
the user's benchmark files except for changes explicitly required by this optimizer migration.

## Validation and Acceptance

The core scheduler is accepted when a rule can wake after previously producing no output; child expression, fact,
estimate, and winner changes recost every dependent parent; exact saturation is invariant under rule registration
order; duplicate IDs fail at registry construction; canonical cycles add no duplicate expressions; complete mode has
zero pending tasks; and bounded mode reports every deferred work item.

The IR/memo layer is accepted when every supported core algebra/scalar operator round-trips with identical results,
aggregates, variable scope, duplicates, bindings, paths, SERVICE behavior, and planned contracts. Rule matching and
application perform no whole-tree IR conversion. Unknown structural operators retain visible child groups, while any
truly atomic boundary reports its reason and bindings.

The physical/cost layer is accepted when every composite winner has independently selected child winners and total
cost equals selected child totals plus local operator cost. VALUES contributes exact row count, tuple multiplicity,
finite domain, and assured binding facts. No rule prices a complete child subtree, and winner dominance contains no
rule-ID or query-shape exception.

Join search is accepted against a brute-force oracle for generated regions of up to eight factors, covering bushy and
left-deep trees, disconnected components, finite anchors, required inputs, paths, OPTIONAL/MINUS constraints, and
correlation. Adding or refining any declared dependency must resume the region and produce the same complete candidate
set regardless of which specialized contributor runs first.

LMDB semantic acceptance includes the complete queryalgebra and LMDB module suites, SPARQL 1.1 compliance regressions,
the estimate-audit corpora, and metamorphic query families that permute join order and wrap equivalent expressions in
Projection, Group, OPTIONAL, MINUS/NOT EXISTS, UNION, SERVICE, subselect, and IN/OR/VALUES forms. Results and bag
multiplicity must remain equivalent.

The reported medical query is accepted when the three-row finite relation is selected before the broad `med:value`
access, VALUES is not reopened once per observation row, the anti-join shows transparent selected children, no node is
tagged `covered_by_parent_winner`, no production rule mentions the benchmark query, and Explain reports `COMPLETE`.

Final JDK 26 acceptance requires twenty-two successful plan/run matrix rows, correct aggregates, no `<failure>` entry,
and equivalent results with DPhyp enabled or disabled. Record optimizer time, allocation, memo expressions, work-item
counts, rule wake-ups, and query runtime. Complete mode has no latency cutoff by design; bounded mode must obey its
deterministic task budget and expose pending work.

## Idempotence and Recovery

All memo additions use canonical fingerprints and are safe to repeat. Revisions increase only when state actually
changes. Queue insertion is deduplicated by the dependency-aware work key. Test setup and query-plan captures may be
rerun without deleting user files. Persistent benchmark stores validate marker and LMDB file sizes before reuse.

Implement additively through the compatibility bridge, keeping both engines buildable until shadow parity is green.
If a milestone fails, fix or revert only that milestone's files; do not restore or overwrite the user-owned benchmark
changes. Do not use destructive Git commands. The old engine remains diagnostic-only during migration and must not be
reintroduced as a production fallback to mask a missing physical implementation.

## Artifacts and Notes

Initial build evidence:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
             -Dmaven.repo.local=.m2_repo -Pquick clean install
    Report:  maven-build.log
    Result:  BUILD SUCCESS, total wall-clock time 36.605 s

Initial fixed-point red evidence:

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py \
             CascadesRuleEngineTest#emptyRuleApplicationIsReactivatedAfterMemoDependencyChanges --retain-logs
    Report:  core/queryalgebra/evaluation/target/surefire-reports/
             org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.CascadesRuleEngineTest.txt
    Result:  Tests run: 1, Failures: 1; the consumer was not retried after the memo changed.
    Log:     logs/mvnf/20260713-135911-verify.log

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py RuleRegistryTest#rejectsDuplicateRuleIds --retain-logs
    Report:  core/queryalgebra/evaluation/target/surefire-reports/
             org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.RuleRegistryTest.txt
    Result:  Tests run: 1, Failures: 1; no IllegalArgumentException was thrown.
    Log:     logs/mvnf/20260713-140112-verify.log

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py \
             CascadesRuleEngineTest#lateChildAlternativeRecostsDependentParent --retain-logs
    Report:  core/queryalgebra/evaluation/target/surefire-reports/
             org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.CascadesRuleEngineTest.txt
    Result:  Tests run: 1, Failures: 1; expected 4 work rows but retained the stale 103-row parent.
    Log:     logs/mvnf/20260713-140738-verify.log

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py \
             CascadesRuleEngineTest#exactSaturationIsIndependentOfRuleRegistrationOrder --retain-logs
    Report:  core/queryalgebra/evaluation/target/surefire-reports/
             org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.CascadesRuleEngineTest.txt
    Result:  Tests run: 1, Failures: 1; exact search did not reach the dependency-created alternative.
    Log:     logs/mvnf/20260713-140848-verify.log

Semantic-edge and finite-membership migration evidence:

    Evidence file: initial-evidence.set-membership-final.txt
    Focused red:   no-key all-COUNT(DISTINCT) Group observed zero SET-rule applications.
    Focused green: semantic-scope 6/0/0, memo model 42/0/0, scheduler 125/0/0,
                   LMDB finite membership 4/0/0, migrated pipeline selectors 3/0/0.
    Full pipeline: 44 tests, 3 failures, 0 errors; the remaining tests are the separately routed finite type-anchor
                   and eligibility/common-prefix follow-ons recorded in the evidence file.

Canonical stream-schema and membership-safety evidence:

    Evidence files: initial-evidence.stream-binding-schema-p0.txt
                    initial-evidence.finite-membership-safety-p0.txt
                    initial-evidence.finite-code-type-schema-fixture.txt
    Focused green:  StreamBindingSchemaTest 3/0/0; CascadesMemoModelTest 46/0/0;
                    LmdbDistinctFiniteCodeTypeRuleTest 6/0/0;
                    LmdbDistinctFiniteMembershipRuleTest 7/0/0.
    Pipeline sweep: 6 tests, 1 failure, 0 errors. All six memo insertion errors are gone. The remaining failure is
                    LmdbOptimizerPipelineTest.java:1077, where the direct join beats a legal materialized EXISTS
                    implementation; preserve it for the local-cost contract milestone.
    Logs:           .mvnf/workspaces/finite-code-type/logs/20260714T091641.905887Z-71237-7fd9fb8e/verify.log
                    .mvnf/workspaces/finite-code-type/logs/20260714T092321.294660Z-5915-63d18709/verify.log
                    .mvnf/workspaces/finite-code-type/logs/20260714T092216.034888Z-96783-31c2bb38/verify.log
                    .mvnf/workspaces/finite-code-type/logs/20260714T092421.455563Z-13728-aaf4c7ad/verify.log
    Post-format:    StreamBindingSchemaTest 3/0/0 at
                    .mvnf/workspaces/finite-code-type/logs/20260714T093130.311040Z-69739-b0573197/verify.log
                    CascadesMemoModelTest 46/0/0 at
                    .mvnf/workspaces/finite-code-type/logs/20260714T093224.957875Z-75263-f454d3fd/verify.log
                    LmdbDistinctFiniteCodeTypeRuleTest 6/0/0 at
                    .mvnf/workspaces/finite-code-type/logs/20260714T093538.727948Z-6876-adf96f78/verify.log
                    LmdbDistinctFiniteMembershipRuleTest 7/0/0 at
                    .mvnf/workspaces/finite-code-type/logs/20260714T093642.123440Z-11459-f5174577/verify.log

Physical winner-schema and scalar-occurrence evidence:

    Evidence file: initial-evidence.stream-schema-p0.txt
    Focused red:   specialized physical output leaked scalar locals; descriptor routing invented a syntax-only edge;
                   local-first scalar execution returned `[good]` instead of `[bad, good]`; occurrence extraction
                   emitted no anti-reorder dependency.
    Focused green: core physical/extractor pair 2/0/0; LMDB schema/DPhyp/execution trio 3/0/0.
    Core log:      .mvnf/workspaces/stream-schema-p0/logs/20260714T100318.023694Z-15589-42dd0811/verify.log
    LMDB log:      .mvnf/workspaces/stream-schema-p0/logs/20260714T101034.020085Z-50596-36d7757a/verify.log
    Broader sweep: LMDB neighboring classes 75 tests, 1 broader winner/provenance failure; core classes 131 tests,
                   1 pre-winner memo invariant error and 1 redundant-projection winner failure. These are recorded as
                   follow-ons, not weakened or routed around.

Selected-prefix and local-cost evidence:

    Evidence file: initial-evidence.medical-q1-cost.txt
    Focused reds:  selected finite context was lost while recursively repricing the RHS; adaptive MINUS reported
                   8 local work rows where its execution fallback requires 408.
    Focused green: MINUS overflow 1/0/0 at
                   .mvnf/workspaces/medical-q1-cost/logs/20260714T142559.404179Z-29209-2671d402/verify.log
    Broad green:   CascadesCostModelTest 69/0/0 at
                   .mvnf/workspaces/medical-q1-cost/logs/20260714T142750.514359Z-35709-a0542b05/verify.log
                   LmdbCascadesOptimizerTest 55/0/0 at
                   .mvnf/workspaces/medical-q1-cost/logs/20260714T142849.986222Z-39394-f7b9fb28/verify.log
                   medical-shaped pipeline 1/0/0 at
                   .mvnf/workspaces/medical-q1-cost/logs/20260714T142959.475819Z-43191-f7dc2888/verify.log
                   selected-context/local-cost trio 3/0/0 at
                   .mvnf/workspaces/medical-q1-cost/logs/20260714T143108.642092Z-48889-f179975a/verify.log
                   finite-membership class 9/0/0 at
                   .mvnf/workspaces/medical-q1-cost/logs/20260714T143204.289046Z-51421-30224f9d/verify.log

Record focused red/green reports, shadow diffs, snapshot paths, benchmark outputs, and material design discoveries in
this section as they are produced. Keep evidence concise; full Maven logs remain under `logs/mvnf/`.

## Interfaces and Dependencies

The final core design defines these experimental/internal contracts in the Cascades package. Exact method signatures
may evolve only when this document's Decision Log and all dependent descriptions are updated together.

`MemoExpressionSpec` contains an `IrOp`, `IrAttr`, child group IDs, expression kind, optional physical implementation
descriptor, delivered `PhysicalProperties`, per-child `ChildRequirement` values, proofs, and immutable annotations.

`MemoChange` identifies changed group/expression/fact/frontier/statistics state and the before/after revisions.
`MemoRevision` exposes expression, logical-fact, estimate, and winner-frontier revisions. `Memo` maintains reverse
parent uses for every child-group reference.

`RuleDescriptor` contains rule ID, kind, root operators, read dependencies, produced changes, wake-up events, priority,
and convergence class. `CascadesRule` matches memo-local expressions and emits expression specifications rather than
complete `TupleExpr` trees. `RuleRegistry` indexes descriptors and rejects duplicate IDs.

`OptimizationCompleteness` contains `COMPLETE`, `BUDGET_EXHAUSTED`, `DEADLINE_EXPIRED`, and
`UNSUPPORTED_ATOMIC_BOUNDARY`. `CascadesPlan` exposes completeness, pending work, saturation counters, the memo, and the
selected winner. Existing experimental approximate accessors forward to this state during migration.

`LocalOperatorEstimate` contains output `EstimateVector` plus local work/CPU, I/O, memory, materialization, and
uncertainty contributions. `CascadesCostModel` estimates only local operator behavior from selected child estimates.
The planner owns total `CostVector` composition.

`JoinRegion` contains factor group IDs, predicates, total-eligibility constraints, correlation requirements, semantic
barriers, and a revision. `JoinSearchService` invokes registered `JoinSearchContributor` implementations and emits
transparent memo alternatives. The exhaustive contributor is mandatory in complete mode; DPhyp, branch-and-bound,
and greedy contributors cannot suppress it.

No new external dependency is permitted. Reuse JDK collections with dense integer IDs, `ArrayDeque`, compact revision
keys, and existing RDF4J estimate/cost types. Introduce no Janino or generated query code in this change.

Plan revision note (2026-07-13, Codex): initial ExecPlan created from the user-approved strict-completeness,
core-engine/LMDB-first, shadow-then-cut-over architecture and the repository inspection findings.

Plan revision note (2026-07-14 09:28Z, Codex): recorded canonical stream-schema and finite-membership safety work,
the eliminated memo insertion errors, and the deliberately preserved materialized-EXISTS cost-contract red. Added
follow-ons for physical/schema prevalidation, multiple-membership saturation, and remaining schema consumers so this
slice does not conceal known completeness gaps.

Plan revision note (2026-07-14 09:38Z, Codex): recorded the post-format schema-fixture red and green. The correction
uses canonical stream names in the test injector; production remains a root-only observer rewrite with a separate,
transparent normalized child and no child-level bag-equivalence proof.

Plan revision note (2026-07-14 10:20Z, Codex): recorded the P0 physical winner-schema clamp, descriptor-only LMDB
DPhyp connectivity, and occurrence-aware scalar correlation contract. The revision also preserves the broader
MEDICAL/routing and core winner reds as explicit cost/provenance or memo follow-ons rather than adding plan-shape
exceptions.
