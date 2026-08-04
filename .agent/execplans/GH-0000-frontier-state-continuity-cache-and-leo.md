# Preserve Frontier evidence from costing through cache and LEO

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md`.

This work follows the completed physical-cost and semi/anti work in
`.agent/execplans/GH-0000-frontier-physical-cost-and-semi-anti-planning.md`. That plan established primitive physical
cost vectors, typed semi/anti candidates, correlation domains, source-scan feedback, and LMDB feedback format 14.
This plan starts at the next architectural boundary: the selected recipe and plan cache currently detach the Frontier
state which produced those estimates, several contextual search paths still pass rows without the state, and LEO
learns a scalar residual instead of correcting the originating Frontier transform and physical dimension.

## Purpose / Big Picture

RDF4J's LMDB planner estimates a join prefix with a Frontier state: a query-local weighted relation that preserves
which RDF term IDs occur together, their multiplicity, and the mathematical guarantee behind the estimate. Today the
memo retains that state while candidates are costed, but the selected recipe copies only scalar rows and work. The
arena is then closed, so a cached plan cannot compare its evidence with a newer synopsis. Some dependent and
correlated-filter searches also reset the prefix to a scalar row count before making the ordering decision. Finally,
selected-plan contextualization can produce telemetry by estimating an already selected tree, which makes it unclear
whether a displayed number was the number that won the search.

After this change, any supported transition which receives Frontier evidence produces another Frontier state with
honest lineage. Candidate costs are recorded as immutable events at the instant the candidate is measured. Recipe
extraction and materialization only copy the winning event. A bounded detached evidence bundle survives the arena and
can be retained by the store-owned plan cache. When data or LEO feedback changes, the cache replays or pairs that
evidence against the current generation and reuses the plan only when the winner remains statistically safe. Runtime
observations carry the originating event and stable Frontier learning key, so LEO corrects the relevant transform and
individual physical work dimensions rather than multiplying an entire plan by one residual.

The observable demonstrations are SOCIAL_MEDIA q9, q4, HIGHLY_CONNECTED q10, and LIBRARY q10. q9 must choose the
Frontier-supported `VALUES -> name -> ab -> da -> cd -> bc` order and expose only estimates recorded during candidate
search. q4 must remain a five-probe streaming anti plan. The q10 queries must retain their beneficial materialized
plans. Repeating an unchanged query must produce a zero-estimator cache hit, while changing the data or LEO revision
must either validate a newly recorded decision event or fully replan.

The closing campaign expands that focused evidence to every query in `ThemeQueryCatalog`: nine themes with thirteen
queries each, for 117 required cells. Each cell must have a semantic result check, an optimized-plan snapshot, a
planner-completeness/fallback classification, cardinality and physical-cost evidence, and a cold-planning benchmark.
Correctness and plan-quality defects are repaired before throughput tuning. The performance phase then makes the
same exact search and evidence cheaper through better algorithms, data structures, locality, and deduplication. It
must not introduce query identities, selectivity cutoffs, preferred join orders, fixed physical-algorithm thresholds,
or any other heuristic that substitutes for Frontier, DPhyp, rewrite legality, or cost-model correctness.

## Progress

- [x] (2026-07-31 20:31Z) Read the repository ExecPlan, Maven, and HotSpot performance instructions.
- [x] (2026-07-31 20:31Z) Complete the required offline root clean install; `BUILD SUCCESS` in 39.987 seconds.
- [x] (2026-07-31 20:33Z) Confirm the active recipe, inherited-context, correlated-filter, fallback, and LEO loss sites.
- [x] (2026-07-31 20:38Z) Capture the failing BagEstimate sidecar contract and preserve state through every transform.
- [x] (2026-07-31 20:50Z) Capture and pass the detached inline-payload contract after arena closure.
- [x] (2026-07-31 20:52Z) Carry detached state ordinals and guarantees from memo metadata into selected recipes.
- [x] (2026-07-31 21:06Z) Introduce typed Frontier dispositions and retain bound/opaque lineage at LMDB fallbacks.
- [x] (2026-07-31 21:10Z) Carry `PackedEvidenceContext` through inherited leaf, filter, and join costing.
- [x] (2026-07-31 21:14Z) Preserve winning Frontier states across correlated-filter DP lanes and scheduling.
- [x] (2026-07-31 21:50Z) Preserve the previously costed outer Frontier state through dependent-subquery assembly.
- [x] (2026-07-31 21:50Z) Record provider calls as immutable costing events and make selected-plan assembly estimator-free.
- [x] (2026-07-31 21:50Z) Export and import resource-free detached evidence with stable tuple pairing and digests.
- [x] (2026-07-31 22:45Z) Restart exactly once in scalar mode after an explicit whole-Frontier-session failure.
- [x] (2026-07-31 22:58Z) Retain unary, binary, unkeyed, and budget-degraded lineage without using state zero.
- [x] (2026-07-31 23:14Z) Add bounded exact `DISTINCT` and deterministic `GROUP` kernels with focused red/green evidence.
- [x] (2026-08-01 00:39Z) Finish exact/bounded set, slice, intersection, and value-bounded zero-length-path transitions.
- [x] (2026-08-01 02:58Z) Add bounded cross-generation cache lookup, validation, and single-flight replacement.
- [x] (2026-08-01 02:58Z) Add paired decision-risk inference and adaptive confidence from 0.51 through 0.999.
- [x] (2026-08-01 02:58Z) Route observations to state-specific LEO cardinality and physical-dimension posteriors.
- [x] (2026-08-01 04:55Z) Make correlated-filter winner emission copy its retained DP events without provider replay.
- [x] (2026-08-01 08:48Z) Separate operator result-row work from LMDB source scans throughout immutable cost events.
- [x] (2026-08-01 11:36Z) Cost every concrete join implementation as a physical event across all search kernels.
- [x] (2026-08-01 11:45Z) Prevent nested costing events from inheriting their parent's derived telemetry fields.
- [x] (2026-08-01 14:03Z) Pass all 1,087 query-algebra evaluation tests and preserve the first full LMDB red gate:
  1,629 tests with seven focused failures in `initial-evidence.frontier-lmdb-module-red.txt`.
- [x] (2026-08-01 14:11Z) Repair the seven focused LMDB planning regressions without weakening their structural
  contracts; the 78-test Frontier integration class and 46-test estimate-audit harness both pass.
- [x] (2026-08-01 21:26Z) Measure faithful q9 locked-order variants, repair opaque unary-prefix reconstruction, and
  correct the structural gate to the empirically faster `cd -> bc` winner without adding a preference rule.
- [x] (2026-08-01 21:41Z) Add an alpha-renamed uncached-planning harness, profile q9 in Docker, and remove
  allocation-heavy LMDB page/node decoding without changing candidate semantics or search-space bounds.
- [x] (2026-08-01 22:40Z) Assemble dense-DP and sparse-DPhyp winners from retained immutable costing events, and
  replay exact correlated factor lattices instead of re-running already completed sub-lattice searches.
- [x] (2026-08-01 23:05Z) Reuse identical primitive finite-surface cardinality measurements query-locally and reduce
  uncached SOCIAL_MEDIA q9 planning from 3,654.378 to 772.904 ms/op without changing the candidate space.
- [x] (2026-08-02 02:23Z) Separate learned statistical guarantees from payload disposition at every transform and
  bridge boundary; learned bound-only evidence remains rankable without being opened as tuple evidence.
- [x] (2026-08-02 05:40Z) Eliminate completed correlated-lattice replay and exact duplicate decision-trace records
  across dense DP and sparse DPhyp without changing the candidate space or winner comparison.
- [x] (2026-08-02 06:35Z) Profile uncached q9 planning in Docker, remove metric-snapshot point reads, reduce mapped
  query-index access checks, and canonicalize each completed Frontier payload exactly once; q9 reaches 627.240 ms/op.
- [x] (2026-08-02 19:06Z) Trace the immutable candidate lifecycle for nested subselects and re-enumerate a prioritized
  JOIN when a descendant winner changes; the focused repeated-direct-lookup regression now passes in `AUTO` mode.
- [x] (2026-08-02 22:52Z) Record and replay demand-realization aliases so a cached descriptor reproduces the canonical
  Frontier state consumed by a later immutable costing event; 65 focused cache/lifecycle/arena tests pass.
- [x] (2026-08-03 01:59Z) Restore the formal packed join hypergraph and DPhyp CSG/CMP receiver, encode exact
  predicate-readiness and Cartesian topology, and pass all 1,139 query-algebra evaluation module tests.
- [x] (2026-08-04 00:24Z) Re-establish the clean-build baseline: the required offline root clean install passed all
  modules in 38.897 seconds. The complete LMDB verify then ran 1,822 tests in 16:30 with eight failures and no
  errors: one DPhyp/filter deadline, one repeated-probe multiplicity error, five Frontier state/degradation failures,
  and the generated estimate corpus's 15.144 join q-error. The retained log is
  `logs/mvnf/20260803-220705-verify.log`.
- [x] (2026-08-04 01:10 CEST) Parse and structurally inventory all 117 Theme queries. All parse; strict
  alpha-normalized algebra/topology comparison leaves 111 distinct structures. Three queries contain genuinely
  unused VALUES variables, and q11/q12 supply 16 distinct nested-OPTIONAL/UNION stress structures rather than
  interchangeable copies.
- [x] (2026-08-04 01:10 CEST) Capture the first 37 authoritative LMDB snapshots through LIBRARY q10. MEDICAL q12 and
  SOCIAL_MEDIA q11 hit the configured execution limit; LIBRARY q11 then left the batch at 37/117 for more than four
  minutes despite `--query-timeout-seconds 120`. The interrupted batch log is
  `profiles/lmdb-opt/theme-audit-2026-08-04/baseline.log`; its 37 completed JSON snapshots are retained alongside it.
- [x] (2026-08-04 01:01 CEST) Audit every retained field in the first 37 snapshots and publish the 37-row ledger plus
  root-cause report. The built-in estimate/actual summaries compare zero nodes in every snapshot, 245 selected
  Frontier nodes degrade, Social q11/q12 consume 1.113B/850.708M modeled work units, and exact finite state is lost
  across cycles, UNION/MINUS, disconnected components, projection, extension, group, and OPTIONAL boundaries. The
  artifacts are `profiles/lmdb-opt/theme-audit-2026-08-04/snapshots/first-37-query-audit.tsv` and
  `profiles/lmdb-opt/theme-audit-2026-08-04/snapshots/first-37-root-cause-report.md`.
- [x] (2026-08-04 01:09 CEST) Turn the Bellman-correctness concern into a deterministic three-factor red test. The
  exhaustive connected optimum retains ordered/evidence state `B -> A -> C` at cost 3, while the dense one-slot
  subset kernel discards `B -> A` and returns `A -> B -> C` at cost 102. The failing report is preserved in
  `initial-evidence.continuation-dp.txt`.
- [x] (2026-08-04 01:34 CEST) Specify the exact continuation-equivalence frontier which replaces one-winner subset
  retention. The design identifies every provider-observable state dimension, equality-only and certified monotone
  dominance modes, a proved subset-Markov `k = 1` fast path, primitive arena/interning layouts, dense/sparse/
  correlated/memo migration points, and unavoidable worst-case bounds. It explicitly rejects beams, caps, epsilon
  thresholds, and deadline pruning. The design is
  `profiles/lmdb-opt/theme-audit-2026-08-04/packed-continuation-equivalence-frontier-design.md`.
- [x] (2026-08-04 03:07 CEST) Make query timeouts cover optimization, precompilation, iterator opening, execution,
  and explanation under one monotonic lexical deadline. Focused and module tests pass in the `query-deadline`
  workspace. Because arbitrary optimizer code is not cooperatively preemptible, also add an opt-in one-child-per-query
  batch runner with a parent-owned 117-row atomic audit ledger, phase sidecars, retained stdout/stderr, process-tree
  termination, and continuation after worker timeout or failure. Its focused CLI and real-process tests pass; red and
  green evidence is retained in `initial-evidence.query-deadline.txt` and `initial-evidence.batch-audit.txt`.
- [x] (2026-08-04 03:07 CEST) Correct snapshot metric extraction to read packed
  `doubleMetricsPlanned.plannedCardinalityRows` as the already invocation-scaled planned cardinality while preserving
  the legacy top-level metric contract. All 14 focused capture tests pass with evidence in
  `initial-evidence.query-plan-metrics.txt`. A complete benchmark-common run exposed one separate catalog marker
  failure for SPARSE q6, which remains an audit item rather than being hidden.
- [x] (2026-08-04 03:07 CEST) Complete the read-only GROUP root-cause audit. Global aggregation has two zero-row
  violations; keyed GROUP discards joint NDV, substitutes minimum marginal NDV, then applies the prohibited
  `sqrt(rows) * keyCount` fallback; sampled unique keys are mislabeled as point NDV; and aggregate/key boundness is
  overstated. The exact projected-key and typed-bound design plus ten-step TDD matrix is
  `profiles/lmdb-opt/theme-audit-2026-08-04/group-cardinality-root-cause-and-design.md`.
- [x] (2026-08-04 04:14 CEST) Add and fully verify an explicit `--lmdb-evidence-mode snapshot-only|adaptive`
  capture policy. The default remains adaptive, while isolated cold audits now propagate snapshot-only through
  preflight and every worker and reject invalid/non-LMDB/compare combinations. All 50 CLI tests pass; evidence is
  retained in `initial-evidence.snapshot-evidence-mode.txt`. Persist the pre-capture sidecar hashes so the 117-cell
  run can prove that snapshot-only neither reads adaptive evidence for ranking nor mutates it.
- [x] (2026-08-04 04:31 CEST) Audit the active packed/DPhyp traversal and numeric plan gates before changing planner
  behavior. Dense and sparse kernels both retain a single Bellman-invalid state per subset, ordinary dense DPhyp is
  reduced to singleton extensions, the sparse kernel has a distinct adjacency traversal, and hard numeric rules
  exclude hash joins or manufacture GROUP/OPTIONAL/access cardinalities. The source-grounded inventory and required
  replacements are `profiles/lmdb-opt/theme-audit-2026-08-04/planner-algorithm-invariant-audit.md`.
- [x] (2026-08-04 04:38 CEST) Design authoritative semantic validation for all 117 cells. Existing Jena defaults
  value-check 64 global aggregates, row-count-check 26 non-global cells, and exclude 27; 42 cells therefore need
  exact complete-bag comparison and SPARSE q2--q12 need a generic factorized exact bag-algebra oracle. Stable
  fingerprints remain the iterative guard, not independent proof. The coverage map, dataset/query identity contract,
  bounded external-sort certification, ordering and blank-node policies, artifacts, and TDD sequence are in
  `profiles/lmdb-opt/theme-audit-2026-08-04/authoritative-theme-semantic-validation-workflow.md`.
- [x] (2026-08-04 02:18 CEST) Replace row-count-only repeated execution with a streaming, schema-aware RDF-term bag
  fingerprint and authoritative catalog `?count` contract. Equal-count/different-binding runs now fail with both
  digests, incomplete and unavailable fingerprints remain explicit, and the 54-test CLI class passes. Red evidence
  is retained in `initial-evidence.result-verification.txt`; the encoding/performance contract is covered by eight
  `SolutionBagFingerprintTest` methods.
- [x] (2026-08-04 02:18 CEST) Complete the exactness/performance audit of DPhyp and every packed subset kernel. The
  16/17 boundary changes legal disconnected interleavings, the 65+ path is one greedy order, and dense K16 emits
  21,457,825 bushy CSG/CMP pairs to derive only 524,272 singleton transitions. The exact shared-transition/frontier
  design and post-baseline TDD queue are in
  `profiles/lmdb-opt/theme-audit-2026-08-04/dphyp-subset-kernel-exactness-and-performance-design.md`.
- [x] (2026-08-04 02:18 CEST) Audit every rewrite family reachable from all 117 structures. Two generic semantic
  counterexamples are now P0: unsafe FILTER distribution into a MINUS right operand, and false DISTINCT idempotence
  for self-JOIN/self-LEFT_JOIN over heterogeneous compatible mappings. The audit also maps irreversible OPTIONAL
  ordering, finite-domain admission caps, effect-safety gaps, UNION/subquery omissions, proof/test coverage, and its
  prioritized TDD queue in `profiles/lmdb-opt/theme-audit-2026-08-04/rewrite-semantics-and-coverage-audit.md`.
- [x] (2026-08-04 02:34 CEST) Complete the first audit-safe capture-hardening pass after repository formatting. Operation deadlines,
  parser/repository/sail propagation, packed metric extraction, result fingerprints, isolated batch accounting, and
  real process-tree termination pass 104 focused tests across seven module selections; the final CLI selection is
  54/54 in `capture-cli-serial`. A transient concurrent Maven/Jansi startup stall was diagnosed by thread dump and
  rerun serially before any corpus evidence was accepted. The first sandboxed corpus launch then exposed a denied
  `ProcessHandle.descendants()` syscall before preflight. A focused red/green contract now degrades only this
  non-nesting Java worker launcher to exact direct-child lifecycle control, records that reduced termination scope,
  and keeps full process-tree control where inspection is available; all three launcher tests pass with the red
  preserved in `initial-evidence.process-tree-fallback.txt`. The later real cold run reopened this gate by exposing
  adaptive-filter mutation and lifecycle boundary cases not covered by that first pass.
- [x] (2026-08-04 02:48 CEST) Define the matched 117-cell performance protocol before throughput changes. Existing
  artifacts measure execution, the default JMH annotations cover only 11 cells, and the uncached endpoint currently
  charges alpha-renaming plus an exact-mode no-op global property mutation. The accepted protocol uses separately
  reported process-cold and fixed-count saturated-miss lanes, complete fixture/evidence identity, exactness and
  semantic gates, paired block-first statistics, and requires the 95% lower confidence bound to exceed 10x for both
  equal-query geometric mean and fixed-corpus summed time. The design and benchmark-only TDD queue are in
  `profiles/lmdb-opt/theme-audit-2026-08-04/all-query-planning-performance-methodology.md`.
- [x] (2026-08-04 04:14 CEST) Stop the first isolated cold corpus run after 25/117 workers when a live hash check
  proved that `snapshot-only` still trained and persisted `join-estimator.rjes.filters`. Preserve those 25 snapshots
  as diagnostics only, clone the resulting store to
  `/private/tmp/rdf4j-lmdb-theme-audit-20260804-frozen-e2ca677b`, and add failing repository tests for foreground
  sampling, background queuing, completed-result feedback, storage-evidence routing, legacy-sidecar loading, and the
  sketch-disabled CLI path. The final policy is immutable for the store lifetime, never opens adaptive `.filters`
  in snapshot-only, and reports the physical queue truthfully. Red evidence is
  `initial-evidence.snapshot-filter-policy-red.txt`; the focused post-format selections pass 3/3, 12/12, and 10/10.
- [x] (2026-08-04 04:14 CEST) Run a two-execution SOCIAL_MEDIA q2 smoke against the frozen clone. The catalog count is
  1, the complete solution-bag digest is stable at
  `8d38fbcebe5fa351edaf472c18f68269e5cf7605a7f58ddd9de299955994e863`, and the optimized-plan hash is stable.
  Before/after manifests under
  `profiles/lmdb-opt/theme-audit-2026-08-04/snapshot-freeze-smoke-q2-post-filter-fix/` prove identical paths, sizes,
  inodes, mtimes, and SHA-256 values for all 14 persistent non-lock files and no new `.cold` or temp sidecar. LMDB's
  writable open advanced only ctime on `triples/data.mdb` and `values/data.mdb`; no persisted bytes changed.
- [x] (2026-08-04 04:45 CEST) Close the reopened process-lifecycle gate: retain already-discovered descendants after
  later inspection failure, classify deadline-boundary completion by exit status, reap and audit interrupted workers,
  make termination scope conservative/final, include launch/setup in the hard budget, and remove the lingering
  `TimeLimitIteration` scheduler seen in the q2 smoke. The timer repair is an exact lease-counted shared scheduler:
  overlapping deadlines share one generation, close and timeout race through one atomic release, the last lease
  terminates the generation, and a later deadline recreates it. Red evidence is
  `initial-evidence.time-limit-scheduler.txt`; the focused four-test contract and eight-test iterator module are green.
- [x] (2026-08-04 06:00 CEST) Close the semantic-publication gate before corpus capture: atomically publish only final
  verified snapshots, enforce catalog row counts for all 117 queries (plus authoritative numeric `?count` values),
  make one-run result/plan stability explicitly `not-assessed`, propagate verification and aggregate batch failure to
  a nonzero outcome after retaining diagnostics, audit preflight as its own row, and replace scalar-size reuse with a
  fail-closed schema-v1 manifest of every persistent non-lock path, exact size, SHA-256, and effective LMDB config.
  Preflight validates without opening LMDB; isolated workers use its header and a full postflight digest rejects any
  mutation. Red evidence is in `initial-evidence.catalog-row-count.txt`, `initial-evidence.one-run-stability.txt`,
  `initial-evidence.verification-failure-propagation.txt`, `initial-evidence.isolated-batch-summary.txt`,
  `initial-evidence.preflight-audit-row.txt`, `initial-evidence.store-manifest.txt`,
  `initial-evidence.validation-only-preflight.txt`, `initial-evidence.postflight-store-validation.txt`, and
  `initial-evidence.manifest-recording-command.txt`. The final 71-test benchmark selection and 15-test capture
  selection are green; exact commands and the frozen fixture digest are preserved in
  `profiles/lmdb-opt/theme-audit-2026-08-04/semantic-publication-gate-green.txt`.
- [ ] Capture all 117 LMDB query snapshots and execution-result checks, including q11/q12 and every query omitted
  from the current default JMH annotations or the incomplete 2026-08-03 history.
- [ ] Maintain a 117-row audit ledger classifying semantic parity, rewrite legality, packed-codec coverage, planner
  completeness/fallback, selected physical algorithms, estimate quality, cost quality, and actionable root cause.
- [ ] Repair every discovered query defect test-first at its shared optimizer boundary; add compositional variants for
  UNION, OPTIONAL, MINUS, EXISTS/NOT EXISTS, subqueries, filters, paths, aggregation, and nested combinations.
- [ ] Capture complete uncached and cached 117-cell planning JMH baselines plus fixed-plan execution baselines in a
  planning-specific history format; do not compare them with legacy prepare-plus-execute history.
- [ ] Profile the slowest planning cells and aggregate Frontier/DPhyp/costing phases, then deliver at least a tenfold
  reduction in both full-corpus geometric-mean and summed uncached planning time without an accuracy regression.
- [ ] Re-run the full query-evaluation and LMDB modules, all 117 snapshots/results, plan-quality gates, and the matched
  benchmark matrix; document every intentional plan change and every remaining statistically inconclusive delta.

## Surprises & Discoveries

- Observation: the original `snapshot-only` policy suppressed adaptive estimator reads during selection but still
  loaded `.filters`, sampled filters, recorded execution outcomes, queued background work, and persisted the adaptive
  sidecar at shutdown. Twenty-five isolated workers grew the sidecar from 191,385 to 214,211 bytes while triples,
  values, operator evidence, and Frontier content remained byte-identical.
  Evidence: the original sidecar SHA was
  `509115145226902c93c5eeed1bc7cdfbcd00dfc3c03d3bd1eb63b8d6f445c0c2`; the stopped-run/frozen-fixture SHA is
  `e2ca677bccaca20f8afca8ba0f61156f702a0347437a9d56ab4ea0c1523418bf`; the focused three-test policy contract
  failed 3/3 before the root fix and is preserved in `initial-evidence.snapshot-filter-policy-red.txt`.

- Observation: a normal LMDB query-only open is byte-stable but not metadata-no-op. The q2 smoke preserved every
  persistent file's size, inode, mtime, and SHA-256, yet advanced ctime on the triple and value data files. The same
  run exited successfully only after Maven warned that a `TimeLimitIteration` thread remained alive for 15 seconds.
  Evidence: exact before/after manifests and the retained CLI log are in
  `profiles/lmdb-opt/theme-audit-2026-08-04/snapshot-freeze-smoke-q2-post-filter-fix/`.

- Observation: `TimeLimitIteration` used one eager process-lifetime `Timer`; closing a query canceled only its task,
  so Maven waited its full 15-second thread-cleanup window even after the result and repository were closed. A lazy
  scheduler generation now exists exactly while timed iterations own leases. Normal close and timeout completion
  release once through the task itself, preserving one shared thread for overlap and terminating it at idle without
  a grace period, per-wrapper executor, or process-global shutdown hook.

- Observation: process success is not yet semantic success. All 117 catalog entries have an expected result-row
  count, but repeated execution currently enforces only the 75 queries with an authoritative `?count` binding; the
  other 42 can return a deterministic wrong bag and pass. One completed run is labeled stable vacuously, final JSON
  is written non-atomically (and once before verification), worker verification failures exit zero, the parent drops
  its failure summary, preflight has no ledger row, and store reuse trusts a scalar size with a one-mebibyte tolerance.
  These are capture-harness correctness defects and must be repaired test-first before any new corpus snapshot is
  accepted as evidence.

- Observation: the current snapshot accuracy summaries are structurally blind rather than accurate. All first 37
  snapshots report zero comparable nodes and zero maximum q-error even when selected relation estimates differ from
  executed rows by as much as `9.33e25`. Planned rows live under `doubleMetricsPlanned.plannedCardinalityRows`, while
  executed rows live under `resultSizeActual`, but no stable semantic node pairing joins them. The result verifier has
  the analogous gap: it checks only row count, so a one-row aggregate with a wrong binding value passes. The repair
  requires stable planned/executed identities, explicit unavailable/incomplete states, and a term-type-aware,
  multiplicity-sensitive result-bag fingerprint.

- Observation: the first 37 queries exhibit two independent continuation failures. Exact finite payloads are correct
  at their leaves and remain exact in positive-control queries, but disappear across repeated-variable cycles,
  UNION/MINUS, disconnected scalar components, projection/extension, GROUP, and OPTIONAL boundaries. Separately,
  learned calibration remains rankable after the structured payload it calibrated becomes non-composable. Both must
  be repaired by exact state transforms and continuation-equivalence classes; a larger exploration budget, fixed
  beam, single winner per subset, or learned scalar multiplier cannot restore the missing information.

- Observation: Social q11/q12 select catastrophic nested OPTIONAL implementations even though q12 records a
  proof-backed well-designed normalization. Their telemetry consumes 1.113B and 850.708M modeled work units,
  respectively, with q11 dominated by dependent and badly-designed left joins and q12 by DISTINCT over OPTIONAL
  fanout. Rewrite legality and physical implementability are therefore separate proof obligations. The physical
  frontier must preserve assured outer bindings, compatibility keys, and OPTIONAL scope, then cost every safe
  dependent lookup, keyed materialization, and hash-compatibility implementation without algorithm thresholds.

- Observation: GROUP cardinality loses otherwise accurate input evidence. Several selected joins are within one
  percent while their containing group is wrong by 11.6x to 51.8x, and global aggregate nodes are sometimes planned
  at zero even though SPARQL emits one row for an empty input. Exact global-aggregate semantics and key-domain/NDV
  propagation are required; a square-root or fixed-ratio group estimate would only replace one heuristic with
  another.

- Observation: GROUP has enough information for exact cardinality without evaluating aggregates. For a global group
  the answer is algebraically one; for an exact finite Frontier input the answer is the number of distinct projected
  `(bound mask, term-ID tuple)` keys. The current implementation instead expands multiplicities into `BindingSet`
  objects and replays aggregate evaluation, so payload/work limits can turn known cardinality into unresolved zero.
  Sampled unique keys supply only a certified lower bound unless a genuine joint distinct estimator supplies a point
  and interval.

- Observation: the packed join dynamic program is not Bellman-correct for its current cost-provider contract.
  Dense search retains one state per relation subset, sparse search retains one state per subset mask, and correlated
  search retains one state per `(factor subset, applied-filter set)`, but `PackedCostContext` makes future cost depend
  on ordered prefix, rows/contribution vector, evidence/calibration lineage, scopes, and delivered properties. A
  three-factor counterexample costs `A->B=1`, `B->A=2`, `AB->C=100`, and `BA->C=1`: cost-only pruning discards the
  globally optimal `B,A,C` continuation (5) and returns `A,B,C` (103). The repair must retain exact
  continuation-equivalence classes/Pareto states with a provably canonical `k=1` fast path, never a beam or cap.

- Observation: `--query-timeout-seconds` is not an operation deadline. `SailTupleQuery.evaluate` starts its
  `TimeLimitIteration` only after `SailConnection.evaluate` has optimized, precompiled, and opened the iterator;
  optimized explanation ignores the supplied timeout; and telemetry interruption depends on operators observing a
  thread interrupt while a top-level `hasNext` is still running. This allowed LIBRARY q11 to wedge the 117-query
  audit after query 37. Deadline propagation and batch failure continuation are correctness requirements for the
  audit harness, not reasons to raise the timeout.

- Observation: ten non-aggregate q11/q12 catalog entries use an expected row count of one as a placeholder, while the
  Jena oracle and default JMH matrices omit those cells. Those constants cannot serve as semantic oracles. The audit
  must compare complete result bags against an independent implementation/bounded store and record stable,
  multiplicity-sensitive hashes instead of treating placeholder row counts as truth.

- Observation: the remaining access-enabling failure is not an inaccurate candidate cost; the candidate-costing
  event is never reached. `seedAccessEnablingAlternatives` installs only finite-VALUES recipes, while the subsequent
  correlated lattice expands states from a row-cost priority queue and exhausts the shared work budget before either
  proof-ready predicate transition is visited. Its dormant `seedCorrelatedFilter` fallback would not be a valid fix:
  `preferredSeedProvider` chooses one producer from scalar row estimates and relation IDs.
  Evidence: the focused
  `seedsEveryLateAccessEnablingRecipeBeforeExhaustiveSearchInEitherInputOrder` run reports both expected correlated
  prefixes missing (`expected [urn:late-type, urn:direct-type] but was []`). The relevant production paths are
  `PackedIncumbentSearch.seedAccessEnablingAlternatives`, `PackedJoinEnumerator.optimizeDenseCorrelatedFilters`, and
  `PackedJoinEnumerator.preferredSeedProvider`.

- Observation: ordinary dense join planning uses DPhyp only to precompute a singleton-extension table, then discards
  CSG/CMP traversal order and re-expands retained states through `PackedCostOrderedStateQueue`. Correlated-filter
  planning consumes CSG/CMP transitions directly, while the 17--64-factor sparse kernel uses a separate adjacency
  traversal. `factorHypergraph` now correctly adds Cartesian edges between disconnected components, repairing the
  earlier missing-transition defect, but dense/sparse transition parity and the declared left-deep physical search
  contract still need exhaustive boundary oracles. The Theme corpus reaches 20 statement patterns, so this is an
  exercised boundary rather than theoretical cleanup.

- Observation: several active numeric rules alter the candidate space or invent cardinalities instead of expressing
  evidence. A connected hash join is assigned infinity below 256 total input rows even when its modeled cost is much
  lower than repeated lookup; an existing test explicitly codifies that outcome with 100,000 repeated-probe work.
  GROUP and OPTIONAL use square-root cardinality functions; bound-access propagation uses 2x and 10x disagreement
  gates plus geometric means. Semantic feasibility checks such as scope safety and `SERVICE` dependence remain valid,
  but every feasible physical implementation must be compared by measured cost and every estimate fusion must be
  evidence/uncertainty based. The complete provisional inventory is
  `profiles/lmdb-opt/theme-audit-2026-08-04/planner-algorithm-invariant-audit.md`.

- Observation: the catalog and current Jena test do not independently certify all 117 solution bags. Only 64 cells
  currently receive an independent numeric aggregate check; 26 receive only a row-count check and 27 are disabled by
  default. A repeated RDF4J solution-bag fingerprint detects drift but cannot prove that a stable result is correct.
  The 42 non-global bags require bounded external-sort comparison against Jena, while SPARSE q2--q12 require a generic
  exact factorized bag evaluator cross-checked against Jena on reduced datasets. Oracle records must bind exact query,
  dataset, schema, term policy, and algorithm identities; timeout or missing evidence is unverified, never success.

- Observation: a predicate seed whose readiness certificate contains every factor used to publish its completed input
  JOIN directly into the predicate's root memo group, then publish the predicate only into an internal helper group.
  Evidence: SERVICE, tuple-function, volatile-extension, volatile-OPTIONAL, and nullable-VALUES contracts all selected
  an unfiltered JOIN. Making the terminal predicate the root transition restores all seven semantic-barrier cases.

- Observation: repeated correlated enumeration can rediscover an already materialized factor in the exact same
  evidence and event context. Reinvoking a stateful provider then produces a second Frontier state for one immutable
  transition and violates event identity.
  Evidence: `correlatedDenseCarriesTheWinningFrontierStateThroughFilterScheduling` retained equal scalar rows and cost
  but rejected state 19 against the original state 12. Exact memo-context lookup now restores the originating event;
  the component-row contract proves the initial 12-row event and zero provider replays.

- Observation: full-module verification exposed three ordering contracts outside the initial focused selection. A
  context-free JOIN spine was refined before its containing correlated DPhyp lattice; a one-unit binary search spent
  that unit on a redundant greedy seed; and the direct DPhyp traversal did not reach a nine-factor reverse optimum
  within the existing 256-event bound.
  Evidence: `PackedFrontierSubsetKernelContractTest.correlatedHypergraphOwnsCostingBeforeItsWrittenJoinSpine` and
  `PackedJoinOrderedPrefixTest` reported three failures while the other 1,136 module tests passed. The first two are
  ownership defects: containing correlated regions must precede contained JOIN regions, and a two-node hypergraph has
  no separate ceiling-search problem. The third requires an anytime search correction whose candidate evidence comes
  from real cost events, not a scalar or workload-specific ordering rule.

- Observation: a canonical Frontier state can first appear as a later costing event input even though the event which
  produced its logical evidence emitted only a replayable or bound-only descriptor.
  Evidence: `costingReplayReconstructsDemandRealizedStatesFromRecordedDescriptorAliases` records a descriptor leaf,
  realizes it before a physical join, and fails stale replay with `costing event 4 uses left input Frontier state
  before its originating event`. The descriptor ordinal is already bound; the trace omitted the deterministic
  descriptor-to-canonical realization which created the later input identity.

- Observation: `PackedCostContext` already has a query-local `evidenceStateId`, and the normal dense, sparse, and
  multiword join searches use it. The loss is not a missing provider interface; it is a set of state-free contextual
  call sites.
  Evidence: `PackedJoinEnumerator` passes state IDs in its ordinary subset kernels, but
  `contextualizeLeaf`, dense correlated-filter transitions, scheduled filters, and inherited-prefix initialization
  still call `reset` without a state.

- Observation: the packed rewrite removed the repository's previously oracle-tested `Hypergraph` and
  `SubgraphEnumerator`; the current so-called sparse-DPhyp kernel is cardinality-layer subset expansion over pairwise
  output-mask adjacency, while dense and correlated search use the same adjacency closure. It does not construct
  hyperedges or emit CSG/CMP pairs.
  Evidence: `PackedJoinEnumerator.adjacency` creates only pairwise shared-binding edges, `optimizeSparseLong` scans
  retained states by cardinality, and repository history at `cef5254a05` and `577e0791cb^` contains the removed DPhyp
  implementation plus randomized DPsub-oracle tests. The bounded AAS q2 test misses the endpoint-bound path at 256
  work units, while the same estimator selects it with an unbounded budget.

- Observation: a prioritized JOIN candidate can become stale even after a later logical fallback refresh records its
  direct child winners as current. The tier-one fallback cannot replace the earlier tier-zero costed winner, and the
  old logical-input fingerprint then prevents fixed-point propagation from scheduling another costing event.
  Evidence: the logging-enabled subselect run retained event 100 with two pre-refresh child winners, rejected event
  110 with the current child winners, and selected the stale 74,477-by-74,477 type cross product. Re-enumerating only
  when the direct winner identities changed selects the bound direct-lookup alternative and passes
  `LmdbSubSelectDirectLookupEstimateTest#subSelectPlanDoesNotDoubleCountRepeatedDirectLookupRows`.

- Observation: the memo already retains the selected state ID and `EvidenceGuarantee` in primitive columns.
  Evidence: `PackedPhysicalMetadataArena` has `evidenceStateIds` and `evidenceGuarantees`, while
  `PackedPlanRecipe.Extractor.append` copies rows, work, access, and planned metrics but omits both columns.

- Observation: the existing selected-plan contextualizer is a second cost search, not a passive annotation pass.
  Evidence: `PackedCascadesPlanner.compute` selects `rootWinnerId`, invokes
  `PackedSelectedPlanContextualizer.contextualize`, and only then extracts the recipe. The contextualizer calls
  `estimate`, `refineOperator`, and `refineIntermediateJoin`, restores incumbent planned metrics, and can offer a new
  winner.

- Observation: the plan cache uses exact `Context` equality including `dataRevision`, so it has no route for finding
  a structurally matching stale-generation plan.
  Evidence: `PackedPlanCache.PlanEntry.matches` compares the complete context, and segment routing includes
  `context.queryHash`, which itself includes the data and predicate-range revisions.

- Observation: Frontier already distinguishes `CERTIFIED_BOUND_ONLY`, `SCALAR_FALLBACK`, and `UNRESOLVED` guarantees.
  The new composability/opacity distinction must therefore be orthogonal rather than another overloaded guarantee.
  Evidence: `EvidenceGuarantee.isComposablePointEstimate` accepts only exact, unbiased, and learned-calibrated states.

- Observation: `withRowsPreservingEvidence` was the most misleading latent sink: it delegated through
  `EvidenceProfile.toBagEstimate`, which necessarily created a state-free result.
  Evidence: the first focused contract failed at the ordinary `withRows` assertion, and the same implementation trace
  showed the explicit state loss in `withRowsPreservingEvidence`. Constructing the rebased profile with the existing
  sidecar makes the focused method pass 1/1.

- Observation: detaching a CALIBRATE state must not copy the raw tuple payload a second time.
  Evidence: `FrontierStateArena.calibrate` shares its parent's payload owner and records the correction as an immutable
  lineage node. The bundle therefore exports the raw parent payload once and retains only the calibration overlay on
  the child.

- Observation: arena IDs cannot be used as detached ordinals even when they often appear topological.
  Evidence: canonical keys are sorted and assigned IDs before materialization, so a parent can have a numerically
  larger canonical ID than its child. Export now performs an iterative lineage traversal and assigns new parent-first
  ordinals.

- Observation: correlated-filter DP needs two state columns per subset, matching its pending/applied FILTER lanes.
  Evidence: the focused provider rejected the scheduled FILTER because both the factor transition and operator input
  had state ID zero. Storing each successful provider output in the corresponding lane makes the same focused method
  pass while retaining the distinct prefix and child states.

- Observation: inherited-prefix costing has three independent contextual identities in addition to rows and state.
  Evidence: the focused contract distinguishes binding layout, correlation mask, and semantic-scope mask and observes
  all three unchanged in contextual leaf and FILTER refinement calls.

- Observation: an event-sourcing wrapper around the provider boundary is sufficient to make exact cache hits
  bit-for-bit stable without adding allocations to the dense memo rows.
  Evidence: a provider that deliberately changes its estimate on a second invocation is called once; the cold plan
  and exact cache hit retain the same event digest, rows, and work, while selected-plan assembly makes no provider
  call.

- Observation: selected dependent subqueries do not require a second estimator pass once contextual winners have
  already been installed in the memo.
  Evidence: the assembler now walks the selected winner graph, reconstructs only primitive input context, and links
  the matching contextual dependent winner; the focused contract passes with a provider that rejects any assembly-
  time invocation.

- Observation: detached inline Frontier payloads can be imported without remapping individual tuple columns.
  Evidence: import predeclares canonical keys parent-first, recreates each stratum as a paired tuple relation, and a
  subsequent export has the same state digest and `(x,y)` pairing after the source arena has closed.

- Observation: a non-composable state must be stopped before payload-opening join kernels, not treated as a provider
  failure.
  Evidence: the work-budget regression initially triggered the new whole-session failover because the join resolver
  attempted to open a bound-only state. Checking disposition at the transition retains its typed degradation and the
  focused LMDB methods pass 2/2.

- Observation: exact `GROUP` cannot reuse the finite BGP surface estimator because the latter deliberately accepts
  only bounded basic-graph-pattern unions and joins.
  Evidence: the new aggregate contract first failed with `exact_only_group_boundary`. Bounded replay of the exact
  Frontier input through RDF4J's aggregate evaluator now produces a database-exact child with `GROUP` lineage while
  sampled, over-budget, nondeterministic, and unsupported aggregates retain a bound-only state.

- Observation: packed INTERSECTION dispatch was not missing from the generic incumbent search; the first direct test
  instantiated the optimizer without a snapshot-backed triple source, which correctly disabled Frontier for the
  complete session.
  Evidence: the same algebra, planned with a `SailDatasetTripleTermSource`, reached the LMDB binary transition and
  retained the left bag multiplicity for mappings in the right support. Sampled/incompatible inputs now keep a
  two-parent bound-only state.

- Observation: a value-bounded zero-length path is a finite relation even when the endpoint value does not occur in
  the database.
  Evidence: RDF4J's zero-length iterator returns one compatible mapping whenever either endpoint is fixed, or exact
  zero when two fixed endpoints differ. Materializing that relation as an exact Frontier leaf avoids a store scan and
  does not claim exactness for the unbounded, snapshot-enumerating form.

- Observation: a root-only decision certificate cannot validate a physical choice made by a child operator.
  Evidence: LEO raised the cost of an exact materialized semi/anti scan above streaming, but stale validation retained
  the cached materialized winner until the certificate included the root decision's complete child-decision closure.

- Observation: replayed local cost events form a dependency DAG; substituting each event objective for the complete
  candidate cost either drops child changes or counts a prior generation's delta twice.
  Evidence: focused cache tests first observed no root movement after a leaf-event change, then observed `13` instead
  of `7` on a second unchanged generation. Retaining child event/cost edges and rebasing event baselines fixes both.

- Observation: connected Frontier component rows can be the complete intermediate-join rows.
  Evidence: SOCIAL_MEDIA-q9-shaped bridge planning retained the two-factor state only on the appended leaf. Comparing
  the component rows with the composed join output before attaching the immutable event preserves disconnected
  multiplication semantics and lets LEO update the actual intermediate transform.

- Observation: certified stale reuse needs independent shadow replans, not only audits after validation declines.
  Evidence: the first low-confidence certified-reuse contract opened one planning session and recorded zero audits.
  Spending the residual error probability `1 - confidence` on deterministic audit generations now opens a fresh
  session, assigns an independent lane, and records stable/flip regret in the adaptive posterior.

- Observation: ordinary scalar `setRows` cannot be inferred to describe a joined component.
  Evidence: interpreting an unscoped scalar leaf estimate as component rows made the same connected pair estimate 10
  rows in one orientation and 100 in the reverse orientation. The packed subset DP then selected `large -> small`
  over the lower-cost `small -> medium` path. Only `setComponentRows` and `setContextualRows` have sufficient scope to
  replace or compose a join cardinality.

- Observation: the correlated-filter DP formerly depended on a post-hoc canonical-root row stamp to make equivalent
  schedules comparable.
  Evidence: without that stamp, the state-zero scalar fallback estimated the same complete FILTER expression as 500
  or 50,000 rows depending on when the filter ran. Frontier/contextual estimates do not need this compatibility path;
  a scalar provider with no scoped estimate must instead reuse the logical group's already-costed equivalence
  cardinality during candidate costing and carry that selected DP value into emission.

- Observation: retaining only rows and state IDs in correlated-filter DP lanes is not enough for event sourcing.
  Evidence: JDWP showed the winning DP path entering `emitScheduledFilterOrder`, which reserves the seed budget a
  second time and invokes the factor and FILTER providers again. Equivalent typed semi/anti alternatives therefore
  spent four additional units apiece, starved a later finite anchor, and could stamp a different answer from a
  provider whose second invocation changes. The DP must retain the winning transition's physical metadata/event and
  assemble memo winners from those immutable records.

- Observation: a generic correlated `FILTER` seed is theorem-dominated when its logical group already contains the
  three typed semi/anti implementations.
  Evidence: JDWP showed the canonical direct `FILTER(EXISTS)` consuming five bounded-search units before the
  streaming, memoized, and materialized alternatives for the same group each received their own seed. Skipping only
  that redundant generic seed preserves all physical algorithms, while a commuted FILTER in a separate group remains
  the sole schedule capable of exposing its late correlation and must still be seeded.

- Observation: repeating the relation-ID order cannot propagate a late logical-alternative winner to a fixed point.
  Evidence: the SOCIAL_MEDIA q7 winner trace cost relations 14--19 with group 10's `144077` materialized incumbent,
  then relation 22 improved group 10 to the `343` memoized semi/anti candidate. A second linear pass repeated exactly
  that order, again refreshing relations 14--19 before relation 22, and left the selected root at `144200` work rows.
  Logical-expression IDs describe append order, not the dependency order of equivalence-group winners.

- Observation: inherited-prefix planning recognizes a safe subtree and then silently returns its unbound winner at a
  deterministic unary wrapper.
  Evidence: PHARMA q7's OPTIONAL right side is `Extension(StatementPattern(?comp, name, ?optName))`. Both UNION
  branches assure `?comp`, but `optimizeWithInheritedPrefix` stops at the Extension and retains the global 13.2K-row
  POSC scan. The materialized plan later labels `?comp` bound even though event 47 was measured with input context
  zero, inflating the LeftJoin to 731.9K work and violating event-sourced context fidelity.

- Observation: the physical vector conflates rows produced by an algebra operator with rows scanned from an LMDB
  access path.
  Evidence: after contextualizing PHARMA q7's OPTIONAL name lookup, telemetry correctly changed the child from an
  unbound POSC scan to a bound SPOC lookup, but the exact LeftJoin still retained 499.6K `sequentialRows`. That number
  is the generic pre-Frontier result-cardinality work fallback, not an LMDB source scan. The focused physical-cost
  contract fails because `PackedCostEstimate` has no independent tenth `resultRows` dimension.

- Observation: a memo-only binding-preserving wrapper has no original packed-query relation in its equivalence group.
  Evidence: LIBRARY q3's relocated FILTER and HIGHLY_CONNECTED q10's typed ANTI_JOIN were valid selected prefix
  winners, but prefix reconstruction tried to convert their synthetic logical source to a base query relation and
  aborted the complete Frontier session. Walking through binding-preserving unary winners retains the physical
  source's group identity while the selected event's Frontier state continues to carry the FILTER/semi/anti row
  distribution.

- Observation: preserving the logical join state is insufficient when the physical implementation is selected by a
  different cost surface.
  Evidence: JDWP showed a deterministic Extension bridge choosing a semantically required independent hash join at
  objective cost 26 versus 28 for dependent iteration, while the memo retained only the logical scalar. Recording a
  separate physical event across canonical, correlated-filter, scheduled-filter, inherited-prefix, greedy, and
  filter-pushdown paths retains the actual hash build/probe/result vector and an implementation-specific LEO key.

- Observation: event-derived telemetry is not provider metadata and cannot be nested in a later event's payload.
  Evidence: a physical hash event had correct primitive dimensions `2 + 2 + 2 = 6`, but its payload retained the
  logical parent event's `optimizer.costEventWorkRows=2`; materialization replayed that stale field over the child.
  Reserving the `optimizer.costEvent*` namespace and reconstructing it exclusively from primitive event columns makes
  the arena-level and LMDB explain regressions pass.

- Observation: applying cardinality LEO only to the logical join event does not correct the physical join event which
  becomes the retained prefix for downstream search.
  Evidence: `leoCalibratedPrefixChangesTheFollowingBridgeState` finds the two-factor physical event with four exact
  `result_rows` observations but an uncalibrated `MEASURE_UNBIASED` output state; the three-factor event is calibrated.

- Observation: the first full LMDB gate exposed contextual-plan failures as a coherent family rather than budget
  exhaustion.
  Evidence: the search status is `COMPLETE`, yet finite VALUES plans retain `[P]` scans instead of `[P,O]`, a
  correlated OPTIONAL right side selects an independent hash over global inputs, and trained streaming semi/anti
  feedback loses the typed physical alternative. The same gate also shows an alternative-path UNION degrading at
  `union_child_state_non_composable` and collapsing a 90-row bag estimate to one row.

- Observation: a physical join event can carry a valid bound-only or opaque Frontier state without allowing the
  state transition to replace the implementation's already-costed physical vector.
  Evidence: `degradeOperator` correctly constructed nonzero lineage, but in authoritative mode it also replaced an
  independent hash event's build/probe vector with scalar result work. The resulting zero build/probe telemetry made
  broad hash plans look cheaper than finite bound probes across the VALUES, alternative-path, and semi/anti failures.

- Observation: an independent hash nested under an inherited binding context is constructed once per outer mapping.
  Evidence: the correlated OPTIONAL plan priced one 2.1K-row RHS scan although `LeftJoinIterator` constructs the
  right-side `HashJoinIteration` 300 times. The dependent alternative already prices its bound factor across all 300
  invocations; the hash alternative must aggregate the independent child and hash dimensions over the same execution
  partitions.

- Observation: transform cardinality and physical implementation work need distinct learning identities on the same
  immutable event.
  Evidence: overwriting the logical join's learning key with `implementation=dependent-iteration` routed actual rows
  only to the physical key, so the following bridge transform could not find the four cardinality observations.

- Observation: a finite lookup derived solely from binding-assignment values is not a complete connected-component
  estimate when the appended statement pattern also bridges an already joined prefix component.
  Evidence: the candidate trace priced two exact `branchName` probes as two complete rows after a 400-row
  copy-to-branch prefix. That let the search postpone the finite anchor and collapse 400 rows to two, even though the
  two probes describe only the local assignment-to-name surface and the complete joined prefix contains 200 rows.

- Observation: the query-local exact-surface budget was charged repeatedly for the same topology and finite domain.
  Evidence: scoped tracing evaluated the identical `type -> locatedAt -> name -> type` surface twice, consuming 800
  rows each time, then exhausted the 4,096-row budget immediately before the dependent `EXISTS` probe. The existing
  factor-cost cache could not reuse it because scalar prefix-row fields differed even though the exact database
  surface did not.

- Observation: caching only complete derived requests still rescans every exact prefix for each appended factor.
  Evidence: the finite `name -> locatedAt -> type -> EXISTS(type)` candidate consumed 202, then 402, then 602 scan
  rows for nested prefixes even though each successful surface already retained the paired exact relation required
  to price the next access. These cumulative charges are neither candidate work nor independent evidence.

- Observation: learned filter evidence was keyed differently solely because a UNION branch marked the same filter as
  a variable-scope boundary.
  Evidence: the recorded key and UNION key had identical input topology, condition, assured binding shape, nullable
  mask, predicate/context identity, and determinism; only `scope=new` differed. The scalar estimator reused the
  observation while Frontier saw no calibration and UNION consequently summed two sampled-zero summaries.

- Observation: the bootstrap binary-join incumbent can be costed before join enumeration has produced a contextual
  candidate state.
  Evidence: the MINUS audit's first connected two-pattern JOIN entered `resolveRawJoinState` with two composable
  sampled leaves but candidate state zero. Multiplying those coordinated samples produced
  `correlated-random-product-unresolved`; its scalar-compatible local cost tied the later valid bridge candidate and
  allowed the opaque incumbent to reach the boolean kernel.

- Observation: an exact conditional bridge is replayable only when its operation recipe identifies the appended
  statement factor.
  Evidence: replacing the invalid sample product with `extendInner` restored Frontier source attribution, but the
  MINUS output initially remained equal to its left input. The bridge recorded the parent JOIN relation as its recipe,
  so alternate-lane replay could not resolve a `LeafState`. Recording the factor relation restored independent-lane
  averaging and the focused MINUS audit passed all variance-reduction assertions.

- Observation: a learned filter can legitimately select a different filter/join schedule from the cold plan, so its
  immutable costing event can have different input rows, raw pass ratio, and effective sample size.
  Evidence: the LEO residual integration test originally reconstructed the learned posterior from the cold plan's
  scalar mirrors and expected `0.22742987122611596`; the selected learned event recorded its own 94-row input and
  posterior `0.22504058628206663`. Computing the posterior solely from that selected event matches its recorded rows
  exactly and the complete 78-test integration class passes.

- Observation: a memo-equivalent unary implementation can be one logical join factor even when its memo-local helper
  child cannot be flattened into packed-query relation IDs.
  Evidence: unrestricted q9 planning aborted with `initial join factor winner expands to -1 factors instead of one`.
  Retaining the winner group's original factor as an opaque prefix component preserves its rows, cost, Frontier state,
  and unary semantics; the same focused IT then plans through Cascades with no fallback.

- Observation: q9's requested `bc -> cd` closure is not the fastest plan on the supported SOCIAL_MEDIA fixture.
  Evidence: a faithful `EXACT_SEQUENCE` harness ran all ordinary LMDB rewrites, excluded planning time, preserved the
  nullable-name proof by comparing against the untouched query, rotated execution order, and collected 21 samples per
  variant. All variants returned the same result; medians were 5.357 ms for the unrestricted
  `VALUES -> name -> ab -> da -> cd -> filters -> bc` winner, 5.375 ms for locked `cd -> bc`, and 8.034 ms for locked
  `bc -> cd`. The Frontier winner is therefore about 33 percent faster than the originally requested order.

- Observation: exact finite-surface cardinality access, rather than the packed DP itself, dominated the first faithful
  uncached q9 planning profile.
  Evidence: alpha-renaming only variable identifiers forced a structurally equivalent cache miss on every JMH
  invocation. The ten-iteration Docker baseline averaged 5,709.149 ms/op. JFR attributed about 93 percent of sampled
  allocation pressure to `HeapByteBuffer.slice`, its backing constructor, and one `LmdbNode` allocation per decoded
  B-tree node, beneath `LmdbFiniteSurfaceCache -> LmdbFiniteJoinSurfaceEstimator -> planningCardinality`.

- Observation: retaining one page buffer and one mutable node carrier per range walk materially reduces planning
  cost while leaving the exact range algorithm and every costed candidate unchanged.
  Evidence: the identical ten-iteration Docker run averaged 3,654.378 ms/op, a 36.0 percent reduction. Sampled
  allocations fell from 34,009 to 12,974 and garbage collections from 308 to 87; `ByteBuffer.slice` and
  `LmdbPage.node` disappeared from the allocation profile. `LmdbKeyComparator`, `LmdbPage.readNode`, and exact range
  counting remain the leading CPU path, so subsequent tuning can target per-comparison work independently of planner
  search-space changes.

- Observation: dense DP and sparse DPhyp retained winner rows and costs but reconstructed the selected path by calling
  the provider again.
  Evidence: a provider which counts complete ordered prefixes observed the selected two-, three-, and four-factor
  prefixes twice in dense search, and the same duplication at 17 factors in sparse search. Retaining the winning
  factor, transition, join-event, implementation, and physical-metadata IDs makes both kernels assemble the winner
  with one provider invocation per candidate. The full 66-test packed-search class and five-test subset-kernel class
  pass. Docker wall time remained statistically flat because exact finite-surface I/O still dominated the workload.

- Observation: different DP/DPhyp candidates repeatedly request the same exact primitive LMDB cardinality surface.
  Evidence: the focused contract observed two calls for the identical `(101,-1,-1,-1)` probe through one query-scoped
  finite-surface budget. Replaying that measured value once per complete primitive key reduced the ten-iteration q9
  Docker result from 3,654.378 to 798.521 ms/op. A subsequent exact mapped-index address fast path measured
  772.904 ms/op; its confidence interval overlaps 798.521, so only the probe replay is classified as a demonstrated
  wall-clock improvement.

- Observation: manually factoring the query-index row offset is slower than leaving the repeated expression to
  HotSpot.
  Evidence: the experiment moved `Math.multiplyExact` from 0.77 percent to 6.22 percent of execution samples and
  measured 817.706 ms/op. The experiment was removed. The preceding single-mapping fast path remains because it
  eliminated `FrontierQueryIndex.segment` and the one-element immutable-list lookup from the profile while retaining
  an exact multi-mapping branch; its wall-clock result is classified as neutral.

- Observation: `LEARNED_CALIBRATED` identifies a statistically corrected estimate, not the presence of a replayable
  tuple payload.
  Evidence: calibrating a sampled zero to a positive posterior correctly produced a `BOUND_ONLY` state, but the
  guarantee-only linear-transform guard admitted it and then failed with `frontier payload is not resident`.
  Payload-consuming transforms and the LMDB bridge now require both `COMPOSABLE_PAYLOAD` disposition and a composable
  guarantee; the focused contract fails before the change and passes after it, while q10 and both sampled-zero paths
  remain green.

- Observation: a complete correlated factor/filter lattice could be followed by a context-free filter-placement rule
  which rebuilt the same legal alternatives and displaced its contextual child winner.
  Evidence: the AAS end-bound property-path candidate was costed correctly inside the dense lattice, then the later
  rewrite reconstructed the path from context-free child winners and selected the opposite start direction. Recording
  exact lattice coverage by factor/filter multiset, scope, inherited state, and base winners lets the fallback rule run
  only when that search was absent, incomplete, or stale; the three focused path/MINUS regressions pass.

- Observation: decision-certificate assembly received byte-for-byte identical immutable alternatives more than once.
  Evidence: a focused memo contract observed three trace rows for two distinct candidates. Collision-safe primitive
  interning over the complete decision goal, context, event, Frontier evidence, physical vector, comparison tier,
  ordered children, and costs reduces that to two while retaining alternatives which differ only in state or child
  provenance.

- Observation: q9's post-replay profile spent planner CPU and allocation on generic representation overhead rather
  than additional estimator information.
  Evidence: production callers materialized complete `LinkedHashMap` snapshots to read one planned metric; mapped
  query-index rows performed repeated foreign-memory address/session checks; and one completed Frontier payload was
  heap-sorted for `pointRows`, effective sample size, maximum weight, and sealing. Allocation-free point lookup,
  absolute views over the same arena-owned mapped segments, and an idempotent canonicalization barrier reduce the
  identical Docker workload from 900.663 to 627.240 ms/op. Payload sort CPU falls from about 9.1 to 3.1 percent, while
  a bitwise floating-point contract proves canonical diagnostic order is unchanged.

## Decision Log

- Decision: validate cold snapshots under an explicit LMDB `snapshot-only` evidence policy and isolate every query in
  a child JVM with a parent-owned audit row before using the corpus to diagnose planner quality.
  Rationale: persisted LEO/operator sidecars otherwise make query order part of the planning input, and an in-process
  timeout cannot guarantee termination inside non-cooperative optimization code. Snapshot-only keeps the synopsis but
  disables adaptive evidence use/learning; process isolation makes every timeout observable and allows all remaining
  cells to continue.
  Date/Author: 2026-08-04 / Codex

- Decision: represent repeated-execution semantics with a versioned, binding-schema-aware, RDF-term-type-aware,
  order-independent and multiplicity-sensitive solution-bag fingerprint in addition to row count.
  Rationale: equal counts do not detect changed bindings, and one-row aggregates can be numerically wrong. The
  streaming fingerprint avoids materializing large result bags and is explicitly diagnostic rather than a proof;
  authoritative equality still requires an independent oracle or actual bag comparison.
  Date/Author: 2026-08-04 / Codex

- Decision: validate query semantics in two tiers tied to canonical dataset/query/schema identities: streaming
  solution-bag fingerprints on every iterative capture, and exact bounded-memory canonical-row merge comparison when
  certifying the oracle. Use Jena for the 106 tractable cells and a query-independent factorized bag-algebra evaluator
  for SPARSE q2--q12, cross-checked against Jena on reduced configurations.
  Rationale: a commutative digest is an excellent O(1)-memory regression signal but cannot be an authoritative bag
  equality proof; expanded execution of the SPARSE aggregates is infeasible even though their exact multiplicities
  are representable compactly. Neither tier may branch on theme, query index, expected answer, or plan estimates.
  Date/Author: 2026-08-04 / Codex

- Decision: derive GROUP cardinality from algebraic exactness, exact projected key support, current joint NDV with an
  honest interval, or certified key-domain bounds—in that order—and keep cardinality certainty independent of
  aggregate payload composability.
  Rationale: no function of input row count and key count identifies joint NDV. Primitive projected-key deduplication
  is exact without aggregate replay, while incomplete sampled unique keys are lower bounds rather than point
  estimates.
  Date/Author: 2026-08-04 / Codex

- Decision: retain an exact continuation frontier per logical planning cell, partitioned by a canonical identity for
  every value a legal future cost transition can observe. Default dominance is equality-only; physical-property or
  resource dominance is enabled only by an explicit transitive substitutability/monotone-composition certificate.
  Preserve one-state storage only as a representation optimization or under a proved subset-Markov additive kernel.
  Rationale: subset masks are not Bellman state for the current provider contract. Any fixed width, top-K rule,
  tolerance, elapsed-time cutoff, or scalar winner can discard the globally optimal continuation. Exact primitive
  frontiers repair the contract, while provider certificates—not observed workload behavior—recover the fast path.
  Date/Author: 2026-08-04 / Codex

- Decision: record source-to-realized state aliases on the originating immutable costing event and replay the exact
  provider realization only when the realized ordinal has not already been bound.
  Rationale: realization changes the representation required by a provider call without changing statistical
  evidence. An explicit alias preserves event chronology and makes detached replay deterministic; inferring the alias
  from rows, guarantees, state order, or payload similarity would be heuristic and could merge distinct evidence.
  Date/Author: 2026-08-02 / Codex

- Decision: after prioritized JOIN enumeration, re-enumerate that JOIN exactly when its direct child winner
  identities change; do not promote the executable-fallback tier to hide the stale certificate.
  Rationale: a costing event is immutable for its original input states. Changed winner identities define a new
  context that must be measured, whereas tier promotion would let an intentionally incomplete fallback displace a
  fully costed candidate and would conceal the event-sourcing violation.
  Date/Author: 2026-08-02 / Codex

- Decision: keep Frontier state continuity separate from statistical guarantee by adding an internal disposition.
  Rationale: an opaque or bound-only operator can retain useful tuple lineage without claiming a composable point
  estimate, while a database-exact state can still be temporarily replay-only because of a payload budget.
  Date/Author: 2026-07-31 / Codex

- Decision: use primitive parallel arrays for candidate event IDs, state IDs, dispositions, and decision certificates.
  Rationale: the packed enumerator is an allocation-sensitive dynamic program; an object per candidate would damage
  locality and create avoidable garbage.
  Date/Author: 2026-07-31 / Codex

- Decision: state ID zero is a session boundary, not a local operator fallback.
  Rationale: a supported operator which cannot produce a point estimate can still derive an unresolved or bound-only
  state retaining its parent and reason. A local zero prevents every downstream Frontier transform from recovering.
  Date/Author: 2026-07-31 / Codex

- Decision: reconstruct correlation prefixes through unary operators only when their algebraic output binding layout
  is identical to their selected child, and never repurpose a physical expression's source-logical ID as provenance.
  Rationale: physical source IDs are group-scoped memo identities. FILTER, query root, slice, reduced, distinct,
  materialize, order, semi, and anti preserve bindings; projection, extension, and group do not and therefore remain
  explicit prefix nodes or conservative boundaries.
  Date/Author: 2026-08-01 / Codex

- Decision: publish a finite binding lookup as component rows only when its participating assignments cover the
  entire prefix component connected by the appended factor; otherwise decline that partial estimate and use the
  full contextual surface/transition estimator.
  Rationale: component-row composition may multiply genuinely unrelated components, but it cannot invent join
  selectivity for connected prefix relations omitted from the measurement. Coverage is an algebraic property of the
  costing event, not a row-count heuristic.
  Date/Author: 2026-08-01 / Codex

- Decision: cache bounded exact derived surfaces by prefix topology, factor topology, and the complete finite-binding
  domain, independently of scalar prefix-row mirrors.
  Rationale: an exact surface is a database relation determined by those structural inputs. Reusing it prevents
  equivalent candidates and physical algorithms from spending the shared scan budget repeatedly while preserving
  the existing 4,096-row cap and declining genuinely distinct or over-budget refinements.
  Date/Author: 2026-08-01 / Codex

- Decision: memoize every successful exact derived relation as a completed-prefix Frontier surface and append later
  factors directly to that paired relation.
  Rationale: exact natural-join prefixes are reusable evidence, not work that must be rediscovered for each suffix.
  Incremental composition charges the shared budget only for the new access, preserves multi-column tuple pairing and
  multiplicity, and avoids increasing the cap or introducing an order-specific preference.
  Date/Author: 2026-08-01 / Codex

- Decision: omit the filter node's variable-scope marker from its learning fingerprint only when every condition
  dependency is assured by the filter input and the expression is repeatable.
  Rationale: such a filter is locally closed, so a surrounding algebra scope cannot change its selectivity. Filters
  with external, correlated, or nullable dependencies retain the scope marker and therefore cannot borrow evidence
  across semantically different binding contexts.
  Date/Author: 2026-08-01 / Codex

- Decision: when a binary logical JOIN has no previously costed contextual candidate, replace the invalid product of
  two non-exact coordinated Frontier measures with the existing exact conditional LMDB bridge whenever either
  physical child is a single statement factor. Preserve the child order and record that factor as the replay recipe.
  Rationale: exact conditional expansion is linear in the retained random measure and therefore composable; direct
  multiplication is not. This supplies a mathematically valid bootstrap estimate without a join-order preference,
  row threshold, or query-specific rule, and keeps independent design-lane replay available to downstream boolean
  kernels.
  Date/Author: 2026-08-01 / Codex

- Decision: tests and telemetry consumers must validate learned estimates from the immutable selected costing event,
  never by combining scalar fields retained from an earlier cold or competing plan.
  Rationale: learning can change both the physical winner and the contextual input surface. Mixing event generations
  creates a numerically plausible but causally false posterior, exactly the reconstruction that event-sourced costing
  is designed to prohibit.
  Date/Author: 2026-08-01 / Codex

- Decision: retain selected-plan contextualization only as a pre-finalization candidate phase until its costing is
  folded into enumeration; never use it to reconstruct telemetry after final winner commitment.
  Rationale: contextual dependencies are real planning inputs. Recording their cost events before the final recipe
  preserves correctness while allowing the migration to remove post-selection stamping incrementally.
  Date/Author: 2026-07-31 / Codex

- Decision: export a detached, immutable bundle containing selected states, decision alternatives, pruning proofs,
  and transitive ancestors, with inline, replayable-exact, and bound-only payload forms.
  Rationale: retaining the live arena would leak query-local store resources, while retaining only the selected
  scalar or state summary is insufficient to validate a changed generation safely.
  Date/Author: 2026-07-31 / Codex

- Decision: exact cache hits reuse the original immutable event; stale-generation validation always creates new cost
  events before a plan is reused.
  Rationale: telemetry must describe what was actually costed. A changed generation cannot stamp old numbers, and
  materialization must not call an estimator merely to refresh annotations.
  Date/Author: 2026-07-31 / Codex

- Decision: propagate late winner changes with an exact dependency worklist keyed by the child-winner IDs last used
  to cost each logical expression.
  Rationale: a logical candidate is stale precisely when one of its immutable child winner references changes. The
  worklist revisits only those candidates, enqueues consumers only after an actual output-group winner change, and
  terminates at the algebra DAG's fixed point without a pass count, percentage threshold, or query-specific rule.
  Date/Author: 2026-08-01 / Codex

- Decision: contextualize repeatable unary operators by recursively costing their child under the inherited evidence
  context, then recording a new operator event over that exact contextual child.
  Rationale: a deterministic Extension is part of the physical RHS invocation, not a reason to discard the outer
  bindings. Safety is proved from scalar relocation/effect facts and assured dependencies; unsupported scope or
  effect boundaries continue to return the existing unbound candidate honestly.
  Date/Author: 2026-08-01 / Codex

- Decision: add `resultRows` as a first-class physical cost dimension and reserve `sequentialRows` exclusively for
  source rows scanned.
  Rationale: result production, projection/extension work, and OPTIONAL merge work are real execution costs, but they
  must learn and replay independently from storage access. The dimension is carried through packed estimates,
  metadata, immutable events, recipes, cache validation, telemetry, and LEO. Operator kernels publish local result
  rows while their selected child access events retain the actual LMDB scan dimensions.
  Date/Author: 2026-08-01 / Codex

- Decision: statistical cache reuse compares candidate objective-cost differences directly with paired deterministic
  Frontier inclusion, not independent scalar cardinality deltas.
  Rationale: direct paired differences retain covariance across shared tuples and cost dimensions and avoid an
  arbitrary percentage-change heuristic.
  Date/Author: 2026-07-31 / Codex

- Decision: initialize validation at 0.99 confidence, bound it to [0.51, 0.999], and require posterior expected regret
  at or below one percent.
  Rationale: the user selected a one-percent loss budget. Independent audit lanes and shadow replans can safely reduce
  evidence effort for stable decisions, while flips and audit misses raise it without query-specific thresholds.
  Date/Author: 2026-07-31 / Codex

- Decision: preserve raw data evidence and LEO calibration as separate Frontier states.
  Rationale: exact database cardinalities must remain protected, learned corrections must be auditable, and a new LEO
  revision must be removable/replayable without rebuilding the raw synopsis evidence.
  Date/Author: 2026-07-31 / Codex

- Decision: keep `BagEstimate.equals` and `hashCode` scalar-compatible, preserve the evidence sidecar in every fluent
  transformation except `withoutEvidenceState`, and add an explicit Frontier fingerprint comparison.
  Rationale: changing public equality semantics risks unrelated map/set behavior. Frontier memo and cache code should
  use an explicit identity rather than silently depending on scalar equality.
  Date/Author: 2026-07-31 / Codex

- Decision: a decision certificate contains the selected root decision plus the transitive closure of every child
  decision referenced by any fully costed root alternative.
  Rationale: this retains relevant physical choices and pruning dependencies without allowing unrelated sampled memo
  groups to downgrade an otherwise exact validation.
  Date/Author: 2026-08-01 / Codex

- Decision: replay complete candidate costs as an immutable local-event residual plus recursively replayed child
  candidate costs; inclusive events remain atomic.
  Rationale: event objectives are operator-local measurements, while plan selection compares complete costs. The DAG
  preserves both meanings and supports generation-by-generation baseline rebasing.
  Date/Author: 2026-08-01 / Codex

- Decision: schedule certified shadow replans with exactly the validation's residual error probability and derive the
  audit lane from an independent avalanche of the data/LEO generation identity.
  Rationale: this supplies unbiased miss detection without a query rule or row-change threshold; exact confidence-one
  reuse remains zero-work, while lower-confidence tiers receive proportionally more independent audits.
  Date/Author: 2026-08-01 / Codex

- Decision: seed append events with the factor's isolated rows/work and treat unchanged unscoped scalar rows as
  compatibility mirrors. When a legacy scalar provider changes that seeded row value for the supplied prefix, promote
  its result to a complete-prefix contextual estimate; explicit component/contextual APIs remain authoritative. For a
  complete correlated FILTER candidate with state zero and no scoped provider rows, use the logical group's previously
  costed equivalence cardinality while the candidate is still in the DP, then carry that exact selected value into
  emission.
  Rationale: the provider's own before/after event distinguishes an actual legacy contextual refinement from the
  default isolated estimate without inspecting a query shape or applying a threshold. Logical-equivalent physical
  schedules then retain one output cardinality, while Frontier and explicitly scoped provider estimates remain
  candidate-specific and are never overwritten.
  Date/Author: 2026-08-01 / Codex

- Decision: correlated-filter DP transitions own their cost events and physical metadata; winner emission is a
  copy-only graph assembly step and consumes no search work.
  Rationale: provider work was already reserved and measured when the transition competed. Replaying it after winner
  selection violates telemetry provenance, double-charges bounded search, and permits a mutable provider response to
  alter the selected candidate. Primitive retained-event/metadata/winner columns preserve locality while making the
  selected graph bit-for-bit identical to the winning DP path.
  Date/Author: 2026-08-01 / Codex

- Decision: seed typed semi/anti alternatives instead of their equivalent generic FILTER whenever both occupy the
  same logical group.
  Rationale: typed alternatives are a complete physical refinement of the pure existence predicate. The generic
  seed contributes no new access schedule, whereas FILTER commutations and unsupported/untyped predicates remain
  distinct logical coverage obligations. This is a capability implication, not a budget threshold or query rule.
  Date/Author: 2026-08-01 / Codex

- Decision: cost logical join transforms and concrete runtime join implementations as consecutive immutable events.
  The logical event owns Frontier cardinality calibration; the physical event retains that state and applies only the
  selected implementation's dimension-specific LEO posterior.
  Rationale: dependent iteration and independent hash share result cardinality but have different access work,
  memory, runtime semantics, and learned residuals. Combining them in one scalar loses both provenance and the cost
  surface needed to compare future candidates.
  Date/Author: 2026-08-01 / Codex

- Decision: reserve every `optimizer.costEvent*` metric for reconstruction from `PackedCostingTrace` primitive
  columns; never persist those names in provider metric payloads.
  Rationale: a candidate may be the input to a later candidate event. Derived telemetry from the parent is not part of
  the provider's answer and otherwise overwrites the child's immutable rows, work, dimensions, phase, or digest during
  restoration and materialization.
  Date/Author: 2026-08-01 / Codex

- Decision: Frontier annotation of a physical event is cost-preserving, and nested independent work is multiplied by
  the measured inherited execution-partition count.
  Rationale: state disposition and statistical guarantee describe evidence, not a replacement cost model. Runtime
  iterator construction supplies an algebraic invocation count, so accounting for it is part of the physical model
  rather than a row-count threshold or join-order heuristic.
  Date/Author: 2026-08-01 / Codex

- Decision: retain one transform learning key for output cardinality and add a sibling physical learning key for
  access dimensions.
  Rationale: all implementations of one logical transform share result cardinality, while source scans, seeks, hash
  work, and memory are implementation-specific. Feedback records both observations from the originating event and
  planning applies each posterior at the layer it can actually correct.
  Date/Author: 2026-08-01 / Codex

- Decision: represent an unflattenable memo-local unary winner by its equivalent logical factor when constructing a
  later join prefix, and reconstruct component contributions conservatively instead of aborting planning.
  Rationale: logical-group equivalence proves the factor's bindings and semantics, while the immutable winner supplies
  its rows, cost, and Frontier state. Flattening the helper would erase the unary operation; state-free scalar fallback
  would erase evidence. Treating it as one opaque component is the exact abstraction already used by join enumeration.
  Date/Author: 2026-08-01 / Codex

- Decision: amend q9's structural acceptance from `bc -> cd` to the measured `cd -> bc` winner.
  Rationale: result-equivalent, pipeline-faithful locked variants show `cd -> bc` is materially faster, and Frontier's
  exact correlated surfaces predict the same ordering (`5,696` versus `8,957` intermediate rows). Forcing the slower
  order would require the query-specific heuristic explicitly prohibited by this plan.
  Date/Author: 2026-08-01 / Codex

- Decision: optimize LMDB range walks by reusing decode carriers and page buffers, while retaining the allocating
  compatibility entry points and all existing corruption checks.
  Rationale: Docker JFR proved these allocations occur inside the exact physical estimator for every DP/DPhyp
  candidate. Reuse changes neither the cardinality formula nor candidate enumeration, so it makes each legitimate
  costing event cheaper without introducing a row threshold, early-exit rule, selectivity guess, or join-order
  preference.
  Date/Author: 2026-08-01 / Codex

- Decision: retain the winning physical transition and immutable event in both dense DP and sparse DPhyp tables, then
  assemble the selected path by linking those records rather than invoking the estimator again.
  Rationale: the provider result which won is already the complete measurement. Replay removes duplicate work and
  prevents a later provider answer from changing telemetry; it does not prune, reorder, or approximate a candidate.
  Date/Author: 2026-08-01 / Codex

- Decision: memoize exact finite-surface cardinality measurements inside the existing query-scoped scan budget by the
  complete primitive `(subject, predicate, object, context)` key.
  Rationale: each key denotes the same physical lookup in the same planning scope. Candidate cost still counts every
  logical runtime probe, while the planner performs the identical database measurement once. The key contains no
  query name, row threshold, selectivity class, factor position, or preferred order.
  Date/Author: 2026-08-01 / Codex

- Decision: retain only profile-supported constant-factor changes and remove the manually factored row-offset
  experiment.
  Rationale: exact semantic equivalence is necessary but not sufficient for a performance patch. Docker JFR showed
  the manual factorization made HotSpot output worse, so preserving it would add complexity without evidence.
  Date/Author: 2026-08-01 / Codex

- Decision: make Frontier payload composability the conjunction of typed disposition and statistical guarantee.
  Rationale: LEO may validly correct a bound or opaque state's scalar distribution without inventing supporting
  tuples. Keeping those axes independent preserves the learned estimate for ranking and prevents local provider
  failures or false tuple composition. This is a representation invariant, not a candidate preference or threshold.
  Date/Author: 2026-08-02 / Codex

- Decision: deduplicate planner work only by complete immutable subproblem identity.
  Rationale: completed-lattice reuse validates structural scope, factor/filter sets, inherited Frontier context, and
  base winners; trace interning compares every decision/event/evidence/child field; finite surfaces retain all four
  primitive IDs. No estimate threshold, predicate identity, query text, candidate rank, or preferred order participates
  in reuse, so a semantically distinct alternative cannot be pruned as a duplicate.
  Date/Author: 2026-08-02 / Codex

- Decision: retain only constant-factor changes whose Docker JFR stack and exact contracts both support them.
  Rationale: direct planned-metric lookup preserves snapshot APIs; `ByteBuffer` views read the same mapped bytes under
  the same arena lifetime; and idempotent payload canonicalization preserves tuple ordering and floating-point bits.
  These changes make each candidate cheaper to measure without changing which candidates exist or how they compare.
  Date/Author: 2026-08-02 / Codex

- Decision: restore formal DPhyp as the source of join-search topology and remove the experimental cost-priority
  traversal as a substitute for it.
  Rationale: the hypergraph encodes shared-binding connectivity and semantic eligibility; the paper-faithful CSG/CMP
  enumerator emits each legal connected partition once in dynamic-programming order, and the Frontier-aware receiver
  alone compares candidate costs. This removes disconnected and duplicate subproblems by structural proof. It adds no
  estimate threshold, query identity, preferred start, row ordering, or fixed join sequence. Cost ordering may only
  break an exact objective tie after DPhyp has produced the same legal candidate set.
  Date/Author: 2026-08-02 / Codex

- Decision: model predicate readiness and Cartesian legality in the hypergraph instead of selecting an
  access-enabling seed.
  Rationale: for each relocatable predicate, every inclusion-minimal factor set which assures all required bindings
  and evaluation barriers becomes one exact complex hyperedge to a predicate node. DPhyp therefore emits the
  predicate transition only after a mathematically sufficient prefix, and emits every minimal alternative without
  consulting rows, costs, relation IDs, predicates, or workload identity. Factor components receive explicit
  Cartesian edges because INNER JOIN semantics permit every cross-component pairing; these edges represent legal
  topology, not a preference. The receiver alone costs emitted factor and predicate transitions, while completed
  greedy endpoint seeds only tighten the incumbent ceiling and cannot define enumeration order or justify pruning.
  Date/Author: 2026-08-03 / Codex

- Decision: let DPhyp enumerate the legal dense JOIN transitions, then schedule those transitions by the immutable
  measured objective of their retained prefix state.
  Rationale: CSG/CMP enumeration remains the sole topology authority, including Cartesian edges. The queue cannot add
  an edge, remove an edge, or justify pruning; it only chooses which already-proved transition to cost next when a
  finite work bound makes the search anytime. This restores the exact-cost ordering contract without a scalar
  cardinality proxy or query-specific seed. Each provider transition is still costed once per retained state revision.
  Date/Author: 2026-08-03 / Codex

- Decision: define the Theme corpus as all 117 catalog entries, not the currently uncommented JMH defaults or the 92
  rows present in the latest execution-history file.
  Rationale: `ThemeQueryCatalog.QUERY_COUNT` is thirteen and `Theme.values()` contains nine themes. q11/q12 and
  omitted/failed benchmark cells exercise supported algebra and cannot disappear from an exhaustive optimizer audit.
  Date/Author: 2026-08-03 / Codex

- Decision: treat semantic result parity, legal rewrite proofs, no unexpected packed-planner fallback, and honest
  estimate/cost provenance as gates before execution speed or planning throughput.
  Rationale: a fast query with the wrong result or a fast normalized fallback is not an optimizer success. Separating
  these gates also prevents historical execution latency from being mistaken for planner-quality evidence.
  Date/Author: 2026-08-03 / Codex

- Decision: measure the tenfold target on matched `ThemeQueryPlanRunBenchmark.planUncachedQuery` results for every one
  of the 117 cells, requiring both the geometric mean and the sum of cell latencies to improve by at least 10x.
  Measure `planQuery` and fixed-lifecycle `runQuery` separately, reject missing cells, and investigate every material
  per-cell regression. Freeze a post-correctness plan/accuracy baseline before performance-only work.
  Rationale: the geometric mean gives every query equal proportional weight, while the sum prevents large absolute
  planning outliers from being hidden by tiny queries. Separate cached planning and fixed-plan execution avoid mixing
  cache benefits or execution-plan changes into the cold-planner claim.
  Date/Author: 2026-08-03 / Codex

- Decision: remove or replace fixed selectivity assumptions and physical-algorithm cutoffs only with typed unknown
  evidence, mathematically legal alternatives, analytic cost equations, or measured/posterior evidence. Any search
  reduction must have an equivalence, dominance, or topology certificate independent of query identity and estimates.
  Rationale: the current scalar fallbacks and row-threshold candidate suppression can directly choose a plan. The
  user's constraint rules out hiding these defects behind workload-tuned constants or special cases.
  Date/Author: 2026-08-03 / Codex

## Outcomes & Retrospective

Implementation is in progress. Frontier states now survive supported transitions, recipe extraction, arena closure,
strict and structural cache lookup, and LEO calibration. Costing telemetry is copied from immutable provider events;
stale validation replays the complete decision dependency DAG and rebases it after each certified generation.
Resource-free bundles preserve tuple pairing, weights, guarantees, lineage, calibration overlays, digests, and replay
descriptors. LEO uses stable transform/access keys with independent dimension posteriors and format-15 persistence.
Logical and physical join events are separate across every packed search kernel, and nested events cannot leak parent
telemetry into their selected child event.
Dense DP, sparse DPhyp, and correlated-filter DP now assemble winners from their original immutable events. Exact
correlated sub-lattices and identical finite-surface primitive probes are replayed query-locally, reducing faithful
uncached SOCIAL_MEDIA q9 planning from 5,709.149 to 772.904 ms/op while preserving the complete candidate space.
Focused generated populations pass all five confidence tiers' anytime coverage, false-reuse, and one-percent regret
gates. Workload snapshots, broad module verification, benchmarks, and Theme classification remain outstanding.

## Context and Orientation

The packed Cascades planner lives under
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed`.
`PackedQueryCodec` encodes RDF4J's object query algebra into integer relations. `PackedMemo` stores logical and
physical alternatives. `PackedJoinEnumerator` performs join-order dynamic programming. A `PackedCostSession` is a
query-local provider which fills a reusable `PackedCostEstimate`; LMDB implements it with
`LmdbFrontierPackedCostSession`. `PackedPhysicalMetadataArena` copies a candidate estimate into primitive memo
columns. `PackedWinnerTable` identifies the winning physical expression. `PackedPlanRecipe` detaches the selected
winner graph, and `PackedPlanMaterializer` recreates ordinary RDF4J `TupleExpr` nodes.

A Frontier state lives in `core/queryalgebra/evaluation` under the optimizer `cost` package. `FrontierStateArena`
owns immutable state rows for one planning session. Each `EvidenceStateRef` names one row and carries an immutable
`EvidenceStateSummary`; payload tuples remain arena-scoped. `FrontierStateKey` records the canonical operation,
binding layout, masks, store UUID, generation, lane, and estimator seed. `FrontierLinearTransforms` derives joins,
outer joins, semi/anti joins, projections, and summary states. A state ID is meaningful only inside its arena.

LMDB's Frontier session lives in
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbFrontierPackedCostSession.java`. It opens the synopsis and
query-index snapshots, derives states, publishes physical candidate costs, and applies current LEO feedback.
`LmdbLeoFeedbackStore`, `LmdbOperatorFeedbackStats`, and `LmdbLeoSurfaceStats` persist and query learned residuals.
`LmdbStoreConfig` and `LmdbStoreSchema` already expose Frontier budgets and design/audit lane counts. The store owns a
`PackedPlanCache`, currently bounded by entry count and keyed by a context containing the data revision.

In this document, a costing event means the immutable input context, output estimate, physical vector, access path,
and Frontier state emitted by exactly one provider invocation. A decision certificate is the set of winning and
competing event IDs plus the comparisons or lower bounds which prove the winner. A detached evidence bundle is a
resource-free copy or replay recipe for every Frontier state needed by that certificate. A disposition says whether a
state can participate in an ordinary point-estimate transform, provides only certified bounds, or crosses an opaque
operator.

## Plan of Work

Milestone 1 establishes state-continuity contracts. Add a package-private `FrontierStateDisposition` and carry it in
`PackedCostEstimate` and `PackedPhysicalMetadataArena`. Add a small state-transition validator which rejects a zero
output after nonzero input unless the transition is tagged as initial-unavailable, semantic-scope-isolation, Frontier
disabled, or whole-session abort. Replace `degradeOperator` with explicit helpers for identity, transformed,
bound-only, opaque-boundary, and initial-unresolved states. Each helper derives a `FrontierStateArena` state retaining
its parent and degradation reason. Unexpected exceptions leave the local output untouched, abort the Frontier
session, and cause the planner boundary to retry once with the scalar cost session.

The operator matrix is exhaustive. Query root and ordering preserve the input. Projection, deterministic extension,
multi-projection, and deterministic filter transform retained tuples. Unsupported or nondeterministic expressions
retain available columns and a bound-only state. Union, optional, MINUS, and intersection compose child states or
derive a bound-only state with both parents. Exact distinct and group operations use bounded exact kernels; sampled
versions retain bounds until a design-valid nonlinear estimator exists. Reduced retains tuple support with bag bounds.
Slice retains a capped bound-only state. Zero-length paths use an exact transform. Arbitrary paths retain a replay or
bound-only state. Service and tuple functions create opaque states. Missing leaf annotations create an initial exact,
sampled, or unresolved state and are the only local source allowed to begin without a parent.

Milestone 2 closes planner-side context losses. Introduce `PackedEvidenceContext`, a primitive carrier for rows,
state ID, guarantee/disposition, binding layout, and correlation/scope masks. Change inherited-prefix entry points to
accept it. Seed contextual leaf costing, inherited ordering, dependent EXISTS, and selected dependent plans with the
outer state. Add evidence-state, guarantee/disposition, and cost-event arrays to dense correlated-filter DP lanes and
pass them through factor and scheduled-filter transitions. Preserve multi-column tuple identity. A semantic scope
barrier starts an explicitly tagged isolated context; it does not masquerade as accidental scalar loss.

Milestone 3 makes costing event-sourced. Add `PackedCostingTraceArena` with primitive columns for phase, relation,
physical expression, input context fingerprint, input/output state IDs, guarantee/disposition, row/work values, every
physical dimension, access metadata, and provider planned metrics. Wrap every planner-to-provider call and inject a
recorder into LMDB for internal access candidates and exact refinements. Assign an event ID only after a provider
invocation finishes successfully. Copy it into physical metadata and winners. Mark accepted, rejected, and pruned
events without mutating their measured values.

Move contextual costing before final recipe commitment. The first safe cut keeps
`PackedSelectedPlanContextualizer` as a named candidate-finalization phase but records all its provider calls and
allows it to change the winner only before `PackedPlanRecipe.extract`. Then migrate its contextual join, filter, and
dependent-subquery costing into enumeration. Once migrated, the class becomes a verification/assembly walker with no
cost-session reference. Delete incumbent planned-metric restoration and any metric blending. A missing measurement is
`unmeasured`.

Extend recipe rows with the winning event ordinal/digest, detached state ordinal, guarantee/disposition, and all ten
physical dimensions. Recipe extraction copies the event snapshot verbatim. Materialization copies recipe fields onto
the selected query nodes and never opens or calls a cost session. Candidate telemetry is retained by the decision
certificate rather than reconstructed from the selected child.

Milestone 4 detaches Frontier evidence and extends the cache. Add `FrontierEvidenceBundle` export/import APIs to
`FrontierStateArena`. Export canonical keys, summaries, operation and parent IDs, tuple/mask/weight payloads,
calibration overlays, guarantees, dispositions, and stable 128-bit digests for all certificate states and ancestors.
Deduplicate payload owners. Inline bounded exact and sampled payloads. Store large exact states as resource-free
replay recipes with snapshot identity and digest. Store unsupported/budgeted states as summary/bound-only lineage.
Never retain a cursor, snapshot, query-index lease, or arena token.

Split `PackedPlanCache.Context` matching into strict and structural forms. Strict matching retains every revision and
serves zero-work hits. Structural matching excludes only mutable data, predicate-range, and LEO revisions while
retaining query shape, value/binding variant, dataset, goal, catalog, provider semantic version, and immutable
Frontier configuration. Route stale candidates by the structural hash and verify the detached query snapshot before
use. Add immutable bundles and decision certificates to `PlanEntry`. Keep the 1,024-entry count limit and add weighted
evidence accounting, default 64 MiB, divided across cache segments. An entry above the total budget remains eligible
for strict hits but not stale-generation reuse. Zero evidence bytes disables structural reuse. Existing single-flight
ownership performs validation and atomically replaces the entry.

Milestone 5 implements statistical revalidation. Exact states replay against the current snapshot and compare content
digests. Sampled states reuse deterministic tuple/lane hashes to form paired Horvitz-Thompson objective-cost
differences, including covariance from shared tuples and multi-column domains. Reapply current LEO posteriors and
recompute every affected candidate event and pruning proof. Invalid or missing dependencies and invalidated pruning
bounds force a full replan.

Use one-sided, family-wise, anytime-valid confidence sequences over direct candidate cost differences. Exact
components have zero variance. The validation policy starts at 0.99 confidence, is bounded to [0.51, 0.999], and
requires posterior expected positive regret no greater than 0.01 of the current best expected cost. Independent audit
lanes and shadow replans update a hierarchical Beta-Binomial stability posterior with Jeffreys `Beta(0.5, 0.5)`
priors. Select evidence effort by minimizing validation work plus expected execution regret under that bound. Stable
audits may lower effort; flips and audit misses raise it. If the current Frontier work/memory budgets cannot certify a
decision, replan. Add LMDB config values for initial, minimum, and maximum confidence, expected-regret ratio, and
cache-evidence bytes, with the stated defaults and validation.

Milestone 6 makes LEO state-specific. Define `FrontierLearningKey` from the raw transform fingerprint, binding and
correlation layout, access kernel/index/mode, operator family, and estimator semantic version. Cost events retain that
key and the exact predicted dimensions. Runtime nodes carry the event digest and learning key so completion routes
directly to the prediction. Record actual result rows and each available physical counter independently; absence is
not zero.

Maintain a raw state and a derived `CALIBRATE` state. For non-exact cardinality and each physical dimension, persist
hierarchical Normal-Inverse-Gamma sufficient statistics over `log1p(actual) - log1p(predicted)`, backing off from the
exact transform/access key to its operator family by posterior precision. Exact cardinality is never changed, though
its physical costs may be calibrated. Remove the global physical multiplier and hardcoded minimum-observation and
confidence gates. Increment a distinct LEO revision when a posterior changes enough to alter its serialized
sufficient statistics; cache validation then replays raw evidence, applies the new posterior, and reruns the decision
certificate.

Bump LMDB feedback persistence to format 15 and continue reading 12 through 14. Import older scalar residuals only as
low-precision family priors. Version-13 incomplete physical counters and absent legacy dimensions cannot override
cold or state-specific costs. Write only format 15.

Milestone 7 validates correctness, quality, and cost. Run focused methods, affected classes, then the complete
query-evaluation and LMDB modules. Capture q9 and q4 structure-plus-estimate snapshots and logging-enabled runs. Run
forced q9 variants and require the cost ordering to match measured execution ordering. Run the supported q4, q9,
HIGHLY_CONNECTED q10, and LIBRARY q10 benchmarks, followed by the full Theme comparison. Investigate every movement
above 20 percent. If primitive trace/bundle work causes unexplained planning CPU or allocation growth, capture JFR or
async-profiler evidence before tuning.

Milestone 8 performs the exhaustive Theme audit and planner-throughput campaign. Capture one named LMDB snapshot run
for all 117 catalog queries with a reusable store, one bounded execution verification per cell, and structure-plus-
estimate output. Build a machine-readable ledger from those snapshots and classify every cell; a query is not closed
while it has a wrong result, exception, timeout, unsupported packed operator, planner fallback, incomplete search,
unexplained evidence degradation, unjustified rewrite, missing legal physical alternative, or estimate/cost defect
capable of changing the winner. For every behavior defect, add the smallest failing in-repository test before editing
production, then extend coverage to the related algebra compositions rather than matching the query text.

After all correctness and plan-quality classifications are closed, capture the complete 117-cell
`planUncachedQuery`, `planQuery`, and fixed-lifecycle `runQuery` matrices as JMH JSON. Add once-per-plan phase
accounting for cost-session/Frontier preparation, packed encoding and rules, DPhyp topology/enumeration, provider
estimation calls, physical costing, contextualization, extraction, materialization, cache validation, candidate
counts, and unexplained residual. Use aggregate timers or JFR events instead of a clock read around each candidate.
Profile the slowest cells with the supported Docker JFR loop and optimize in descending Amdahl share. Candidate
changes include readiness-antichain construction instead of subset rescans, component-aware exact DPhyp, proof-keyed
Pareto state retention where one-state Bellman optimality fails, demand-driven Frontier materialization, packed
relation/symbol reuse instead of repeated `TupleExpr` and boxed-map construction, primitive query-local tables, and
interned/deferred cost-event telemetry. Each change must preserve the exact legal candidate set unless an explicit
equivalence or dominance proof justifies removing work.

## Concrete Steps

Run commands from the repository root. The initial clean install is already complete and recorded in
`maven-build.log`. Before each behavior-changing production slice, add the smallest failing method and run it through
the repository wrapper, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py PackedFrontierSessionContractTest#recipeRetainsDetachedFrontierState \
      --retain-logs

Immediately persist the first failure in `initial-evidence.frontier-continuity.txt` with
`scripts/agent-evidence.py`, retaining the exact log and Surefire report path. Do not run tests with Maven `-am` or
`-q`. After the fix, rerun the identical selector, then its class. Broaden with:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run copyright checks before formatting, then format from the root:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Use the query-plan snapshot skill for SOCIAL_MEDIA q9 and q4 with `structure+estimates`, preserving logs under `/tmp`.
Use the supported benchmark wrapper:

    scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:9
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:4
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query HIGHLY_CONNECTED:10
    scripts/run-single-benchmark.sh --theme-plan-run --theme-query LIBRARY:10

Run the Theme history analyzer from
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results` and classify all
changes over 20 percent against the fresh baseline and develop.

## Validation and Acceptance

State-continuity tests must show that every supported nonzero input yields a nonzero child state and that exactness is
never retained after a sampled or unresolved transform. They must cover identity, filter, projection, extension,
multi-projection, union, optional, MINUS, intersection, distinct, reduced, group, slice, zero/arbitrary paths, service,
tuple function, unsupported expression, missing source, budget exhaustion, nested scopes, and unexpected provider
failure. Multi-column tests must prove tuple pairing survives.

Planner tests must prove inherited and dependent subqueries receive the exact outer state, dense correlated-filter
pending/applied lanes carry state, and scope isolation is explicit. SOCIAL_MEDIA q9's fixture must enumerate the legal
orders and select `VALUES -> name -> ab -> da -> cd -> bc` from measured Frontier costs without a query-specific rule.

Telemetry tests use a provider which returns a different value on its second call. The materialized plan must contain
the event from the invocation which won; extraction and materialization must make no second call. Rejected candidate
events must remain visible only as alternatives. Exact cache hits must make zero estimator/store calls. A validated
stale hit must stamp a newly recorded validation event.

Bundle tests must round-trip exact and sampled tuples, masks, weights, operation lineage, and calibration overlays
after the source arena closes. No store lease may remain. Cache tests cover strict and structural hits, byte eviction,
oversized entries, data-only and LEO-only revisions, invalidated pruning proofs, incompatible provider versions, and
concurrent single-flight validation.

Statistical tests use deterministic generated finite populations with an exhaustive cost oracle. Force 0.999, 0.99,
0.95, 0.75, and 0.51 policies and measure independent audit coverage, false reuse, and expected positive regret. A
tier is authoritative only if its observed audit coverage is compatible with the nominal level and no accepted
decision exceeds one-percent expected regret. Ambiguous and over-budget cases must replan.

LEO tests prove an observation updates the originating transform/access key, cardinality and every physical dimension
learn independently, exact rows remain unchanged, downstream Frontier states use the calibrated parent, and a LEO
revision can retain or invalidate a cached winner. Use held-out fixtures to show q-error and plan regret improve rather
than only fitting observed queries. Persistence tests cover versions 12 through 14 and a format-15 round trip.

End-to-end results are unchanged. SOCIAL_MEDIA q9 selects the measured cycle order above. SOCIAL_MEDIA q4 retains
five exact SPOC probes and `streaming-correlated`. HIGHLY_CONNECTED q10 and LIBRARY q10 retain bounded beneficial
materialization. Candidate telemetry is event-sourced, every stale reuse is statistically certified, and no
query-specific heuristic, fixed join preference, new dependency, or breaking query-evaluation API is introduced.

The exhaustive gate additionally requires 117/117 semantic result checks and snapshots, with no unexpected planner
fallback and no unclassified cell. Every intentional rewrite or physical-plan change has focused and compositional
tests plus a ledger rationale. Against matched pre-campaign measurements on the same hardware, JDK, dataset, flags,
and JMH configuration, both the geometric mean and sum of all 117 `planUncachedQuery` scores improve by at least 10x;
cached planning and fixed-plan execution are reported separately. Cardinality q-error, decision regret, exactness,
Frontier guarantees, and result multiplicities are unchanged or improved. A performance-only slice must preserve the
post-correctness selected-plan/evidence baseline bit-for-bit unless its proof and measured benefit are documented.

## Idempotence and Recovery

All tests, snapshot commands, and benchmarks are safe to repeat. Keep untracked evidence, logs, snapshots, prepared
stores, and benchmark results. Never reset, clean, or overwrite unrelated working-tree changes. If a behavior-changing
production edit is made before its failing test is observed, revert only that edit with `apply_patch`, return to the
failing-test step, and preserve its Surefire evidence. If offline dependency resolution fails, rerun the exact build
once without `-o`, then return offline. If a Frontier session throws unexpectedly, the planner may retry exactly once
with the existing scalar session; repeated failure propagates and is never swallowed.

## Artifacts and Notes

The initial install is in `maven-build.log`:

    [INFO] BUILD SUCCESS
    [INFO] Total time: 39.987 s (Wall Clock)

The initial focused failure belongs in `initial-evidence.frontier-continuity.txt`. Later evidence files should use
descriptive suffixes so the first failure is never overwritten. Query snapshots and logging runs belong under `/tmp`;
Maven logs belong under `logs/mvnf` or the exact workspace path printed by the runner.

## Interfaces and Dependencies

Add package-private `FrontierStateDisposition`, `PackedEvidenceContext`, `PackedCostingTraceArena`,
`PackedDecisionCertificate`, `FrontierEvidenceBundle`, `FrontierValidationPolicy`, and `FrontierLearningKey`. Extend
`PackedCostEstimate`, `PackedPhysicalMetadataArena`, `PackedPlanRecipe`, `PackedPlanningResult`, and
`PackedPlanCache.PlanEntry` with primitive IDs or immutable detached values. Do not expose arena-local IDs as durable
identities; cached and explain telemetry uses bundle ordinals plus stable digests.

Add backward-compatible LMDB configuration properties for the 64-MiB evidence budget, 0.99 initial confidence, 0.51
minimum, 0.999 maximum, and 0.01 expected-regret ratio. Existing constructors and public query algebra remain
compatible. LMDB feedback format becomes 15 with reads for 12, 13, and 14. No dependency is added. The hot planner
path continues to use primitive arrays, integer IDs, reusable scratch carriers, and bounded materialization on JDK 25.

Revision note (2026-07-31 20:33Z): Created the follow-on ExecPlan, recorded the verified scalar-loss architecture,
fixed implementation order, statistical reuse policy, one-percent regret choice, LEO model, and validation contract.

Revision note (2026-07-31 20:38Z): Recorded the first failing Surefire contract and completed the latent
`BagEstimate` sidecar-preservation slice without changing its scalar equality semantics.

Revision note (2026-08-01 11:45Z): Completed event-sourced physical join costing across all planner search paths,
separated logical cardinality LEO from implementation-dimension LEO, and reserved derived event telemetry so nested
events remain immutable during restoration and materialization.

Revision note (2026-08-01 19:10Z): The q9 logging run localized its planning stall to snapshot-backed bridge
expansion, where `refinementWorkUnits` bounded input probes and retained particles but not rows traversed inside one
LMDB cursor. Added one query-local row budget shared by the bridge survey and replay passes. Cursor rows rejected by
the selector still consume work; mapped query-index rows remain governed by their existing bounded kernel. Exhausting
an ordered snapshot prefix now publishes an `UNRESOLVED` bound-only state with the input lineage and operation recipe,
rather than claiming an unbiased sample or failing the whole session. A sampled-synopsis plus exact-`VALUES` fixture
proves the snapshot path, bounded degradation, nonzero lineage, and unchanged query results; the existing zero-probe
and mapped-resampling contracts remain green.

Revision note (2026-08-01 21:05Z): A bounded q9-shaped planner fixture reproduced budget starvation at 4,000 work
units: nested strict filter regions consumed the budget before the containing region could compare the complete
factor-and-predicate schedule. The observed failure was `VALUES -> ab -> bc -> cd -> da -> name`; exact search costs
41,675 candidate transitions. Relocatable regions are now scheduled in their structural containment order, with
supersets before strict subsets. This ordering is a topological dependency rule only: it reads no predicate identity,
row estimate, selectivity, access path, or workload name, and it does not alter the unbounded candidate set. The same
4,000-work fixture now selects `VALUES -> name -> ab -> da -> bc -> cd`.

Revision note (2026-08-01 21:12Z): The supported Docker JFR planning loop separated one cold SOCIAL_MEDIA q9 plan
from exact cache reuse. The first operation measured 6,764.598 ms/op; subsequent operations measured 1.137–1.268
ms/op. The recording is `profiles/lmdb-opt/social-q9-planning-baseline.jfr`; the cold-planning chunk is
`/tmp/social-q9-jfr-chunks/social-q9-planning-baseline_58.jfr`. Its dominant planner-side CPU includes LMDB page
decoding, native cursor access, range counting, Frontier heavy-candidate tracking, and primitive maps. Allocation is
dominated by `HeapByteBuffer.slice`, `HeapByteBuffer.<init>`, and `LmdbPage.node`. Because the recording contains one
cold operation plus cache hits and setup, the next profile must repeatedly force structurally equivalent uncached
plans before changing page or buffer code. Optimization work is restricted to canonical subproblem/event reuse and
profile-proven implementation costs; fixed row thresholds, predicate-name rules, workload-specific join orders, and
selectivity heuristics are prohibited.

Revision note (2026-08-01 21:18Z): With containment scheduling, the real q9 winner improved to
`VALUES -> name -> ab -> da -> cd -> bc` and planned work fell from about 104.7K to 50.7K, leaving only the final two
cycle edges reversed. At the decisive prefix, the `cd` POSC reverse expansion exhausts the exact snapshot row budget
and becomes an `UNRESOLVED` bound-only Frontier state; the competing `bc` transition is not retained in the root
decision certificate because dense-DP losers are currently discarded. The next estimator change must preserve the
query-index sampling design (including tuple covariance and inclusion weights) or retain an honest bound; it may not
replace the missing distribution with a scalar fanout or a preference rule. Dense-DP finalists will also retain their
originating immutable cost events so this comparison can be explained without recosting.

Revision note (2026-08-01 21:26Z): Exact factor-domain refinement now retains the decisive q9 alternatives as
originating events: at the shared 598-row prefix, `bc` produces 8,957 rows while `cd` produces 5,696 rows. A
pipeline-faithful locked-order experiment excluded planning time, rotated 21 measurements per variant, and verified
identical results against the untouched nullable query. The unrestricted `cd -> bc` winner measured 5.357 ms median,
locked `cd -> bc` 5.375 ms, and the originally requested `bc -> cd` 8.034 ms. The acceptance gate is corrected to the
faster `cd -> bc` order; no preference heuristic is introduced. The same experiment exposed and now covers an
unflattenable memo-local unary prefix that previously aborted Cascades and silently selected the normalized fallback.

Revision note (2026-08-01 21:41Z): Added `planUncachedQuery`, whose lexical alpha-renaming preserves comments, IRIs,
quoted strings, and variable syntax while generating a fresh structural cache key for every otherwise identical q9
plan. The baseline was 5,709.149 ms/op. Its JFR showed exact LMDB finite-surface range walks allocating a sliced key
buffer and node object for each comparison. `LmdbBtreeRangeCounter` now reuses one node carrier and page buffer,
`LmdbPage` exposes an allocation-free decoder behind its compatible allocating method, and `LmdbDataFile` supports a
caller-owned page buffer. The same Docker workload is 3,654.378 ms/op (36.0 percent faster), with sampled allocations
down 61.8 percent and GCs down 71.8 percent. This is a profile-proven constant-factor optimization only: the planner
visits and costs the same exact candidate space.

Revision note (2026-08-01 23:05Z): Dense DP and sparse DPhyp tables now retain their winning factor, transition,
physical implementation, metadata, state, and costing-event IDs; selected-plan emission links those records and makes
no provider call. Exact correlated factor lattices likewise replay complete retained sub-lattice states when their
scope, factor/filter multiset, base winners, and Frontier state all match. Below that memo layer, the shared
finite-surface scan budget now memoizes `planningCardinality` by its complete four-ID lookup key. The focused red gate
observed two identical physical measurements and the green gate observes one. Docker q9 fell from 3,654.378 to
798.521 ms/op, and the final exact single-mapping address path measured 772.904 ms/op. This is 78.9 percent below the
page-reuse baseline and 86.5 percent below the original faithful uncached baseline. A manual row-offset factoring
experiment was slower in both JFR shape and point estimate and was removed. None of these changes inspect query text,
predicate identity, estimates, selectivity, or candidate rank; candidate enumeration and objective comparison remain
unchanged.

Revision note (2026-08-02 02:23Z): Removed the legacy whole-operator LEO cardinality path so event-keyed Frontier
calibration is the sole non-scalar correction path. A positive learned posterior without supporting particles now
retains `LEARNED_CALIBRATED` guarantee and `BOUND_ONLY` or `OPAQUE_BOUNDARY` disposition. Every linear transform and
LMDB provider transition checks both axes before payload access; exact cardinalities remain untouched and physical
dimensions remain independently learned. No query fingerprint, row cutoff, fixed order, or preference rule was added.

Revision note (2026-08-02 06:35Z): Exact completed-lattice coverage now prevents the fallback filter rule from
re-enumerating an already complete correlated search, and collision-safe decision-trace interning collapses only
fully identical immutable alternatives. The supported Docker q9 loop then localized constant-factor costs: whole-map
metric snapshots, checked foreign-memory reads, and four canonical sorts per completed Frontier payload. Direct point
lookups, absolute views over the same arena-owned mapping, and one idempotent canonicalization barrier reduce uncached
q9 planning from 900.663 to 627.240 ms/op. The profile records
`profiles/lmdb-opt/social-q9-planning-uncached-canonical-once.jfr`; bitwise diagnostic-order, segmented-index, memo,
and planner regressions remain the semantic gates. None of the reuse keys or constant-factor paths inspect estimates,
selectivity, predicates, query text, workload identity, or candidate rank.

Revision note (2026-08-02 08:52Z): The post-dedup Docker recording contains 44,999 execution samples. Its remaining
general duplicate-work signal is LEO structural-key construction: join-permutation canonicalization is on 23,212
sample stacks and `learningRawTransform` on 3,827. The session already caches the complete `operation@topology`
string, but alternating Frontier operations for one packed relation evict each other and rematerialize and
canonicalize the identical logical topology. Split that cache into an immutable per-relation topology digest and a
last complete transform. This preserves the exact `LeoOperatorKey` fingerprint byte-for-byte and changes neither the
DP/DPhyp candidate set nor any cost; it only prevents repeated materialization/canonicalization of the same packed
relation. The pre-existing original-plan preference remains outside this change and is consulted only after every
objective dimension is bitwise tied. No new estimate threshold, preference, or workload key is permitted.

Revision note (2026-08-02 09:01Z): The per-relation topology-cache experiment measured
668.309 +/- 109.177 ms/op and retained 23,184 join-permutation samples, statistically indistinguishable from and
slightly above the 627.240 +/- 88.360 baseline with 23,212 samples. It demonstrated that the repeated work occurs
once in each deliberately uncached plan rather than through operation-cache eviction, so the field and code were
removed. The next optimization targets the canonicalizer algorithm itself and must reproduce the current structural
fingerprint exactly for every factor permutation; it may prune only lexicographically dominated prefixes, never
plans or cost candidates.

Revision note (2026-08-02 09:05Z): The retained LEO canonicalizer uses a reversible variable-ordinal map and a
prefix string while traversing the same complete permutation tree. Once a complete canonical string exists, a branch
is skipped only when its already-emitted prefix is lexicographically greater and therefore cannot possibly become the
minimum under any suffix. An independent test enumerates all 720 orders of a six-factor cycle and proves the exact
fingerprint for three input tree orders. Docker q9 measures 597.525 +/- 88.508 ms/op versus the 627.240 +/- 88.360
pre-change run; canonicalizer stack presence falls from 23,212 to 1,469 samples (93.7 percent). The recording is
`profiles/lmdb-opt/social-q9-planning-uncached-canonical-prefix.jfr`. This prunes only impossible representations of
one immutable LEO key: it neither skips nor reprices a DP/DPhyp plan candidate and uses no estimate, predicate,
workload identity, or cost threshold.

Revision note (2026-08-02 12:45Z): A cost-priority work-queue experiment made the synthetic descending-prefix fixture
green but did not repair bounded AAS q2 and changed its fallback plan. The experiment identified budget starvation but
is not the architectural fix: a costly prefix can unlock a cheap bound access, so partial accumulated cost is not an
admissible ordering proof. Repository history confirmed that the binary packed rewrite had removed the prior formal
DPhyp layer. The active milestone therefore restores the oracle-tested hypergraph/CSG-CMP abstraction and routes
Frontier/event-aware costing through its receiver; no cost or cardinality value participates in topology generation.

Revision note (2026-08-02 22:52Z): Demand realization is now an explicit event-sourced representation alias rather
than an implicit jump between unrelated detached state ordinals. Each event records only source IDs which differ from
the provider-consumed state, and stale replay invokes the current session's exact realization before reproducing the
candidate. The focused regression first failed at event 4 with an unbound canonical left-input ordinal and now passes;
the full cache/lifecycle/arena selection is 65/65 green, LMDB cache and semi/anti selections are 11/11 green, and the
LMDB integration class retains only its same five previously isolated composition failures. This is provenance, not a
costing rule: no estimate, threshold, candidate rank, query fingerprint, predicate, or access path controls it.

Revision note (2026-08-03 00:20Z): A logging-enabled fresh-store SOCIAL_MEDIA q9 run exposed an exploration-order
precision collision below DPhyp. The exact `VALUES -> name` event retained four tuples, and the finite `ab` surface
measured 57 exact result rows, but the output key had already been occupied by a sampled derivation from another
physical orientation. Candidate costing copied the exact surface rows while retaining the earlier
`UNRESOLVED`/`BOUND_ONLY` state, so later joins lost tuple pairing and exact refinement. An independent one-key
fixture reproduces the same contradiction: seven exact surface rows are stamped beside a sampled-zero fallback.
The repair will retain the already-measured finite relation as an immutable lineage-specific state whenever a weaker
canonical derivation exists, and cache that exact state by its complete logical key. It will not mutate prior events,
rerun the estimator, compare candidates by guarantee, or inspect relation identities; database-exact evidence is a
proof that dominates a sampled representation of the same logical state.

Revision note (2026-08-03 00:55Z): The exact-surface repair now derives the estimator input from the complete
database-exact Frontier tuple relation, while continuing to price index access from only the factor's shared binding
domain. This preserves unrelated carry-through columns and their multiplicities instead of attempting to reconstruct
them from an already projected key surface. Exact states are retained query-locally by their complete logical key; an
existing exact state must agree on row mass, while a weaker canonical state remains immutable and receives a
lineage-specific exact child. The carried-binding regression first failed with
`declined:missing-binding:tag`/`UNRESOLVED` and now passes as `DATABASE_EXACT`/`COMPOSABLE_PAYLOAD`. Fresh-store and
logging-enabled SOCIAL_MEDIA q9 runs both select `VALUES -> name -> ab -> da -> cd -> bc`; the q4 five-key anti-probe,
six semi/anti persistence tests, and 78 unaffected Frontier integration tests pass. The integration class retains its
same five previously documented failures after this repair. Docker q9 uncached planning measures
402.264 +/- 48.313 ms/op versus 472.924 +/- 61.897 before the repair, a 14.94 percent decrease (below the 20 percent
classification threshold). The post-fix recording is
`profiles/lmdb/social-q9-plan-uncached-post-exact-surface.jfr`; its planning samples are led by Frontier query-index
reads and payload operations. The older recording also contains fresh-store construction and 15.1 million StorePath
events, so its setup hotspots are not a controlled comparison of planner internals. No candidate cost, relation ID,
predicate, query fingerprint, access-path preference, or cardinality threshold controls the repair.

Revision note (2026-08-03 22:16Z): Expanded the closing milestone from four focused queries to the complete 117-query
Theme catalog and added separate semantic, rewrite, packed-coverage, fallback, estimate, physical-cost, and planning-
latency gates. Defined the matched 117-cell 10x acceptance metric, prohibited heuristic candidate suppression, and
recorded the required phase instrumentation and algorithm/data-structure optimization order.
