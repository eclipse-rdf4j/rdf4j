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

The end-to-end proof is the complete 117-query Theme catalog plus exhaustive and generated small-query oracles. Every
query must retain authoritative SPARQL result-bag semantics, every rewrite must expose its proof and rejection reason,
and every selected physical plan must be traceable to the Frontier state and multidimensional costs used during
search. Planning and execution measurements must use matched stores, JVMs, evidence modes, and fixed-plan controls;
performance may trigger an explicit incomplete status but never disguises candidate suppression or excuses an unsafe
rewrite.

## Progress

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
- [ ] **[in_progress] Milestone 9:** replace the raw Frontier payload, derived query index, and query-time exact-replay
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

## Decision Log

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

## Outcomes & Retrospective

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
