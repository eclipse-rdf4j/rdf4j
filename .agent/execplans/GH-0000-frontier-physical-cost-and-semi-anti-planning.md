# Make Frontier evidence drive physical semi/anti-join planning

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md`.

## Purpose / Big Picture

RDF4J's packed query planner already uses Frontier sketches to carry correlations across complex join trees, but its
physical cost is still collapsed into one `workRows` number. That loses the difference between scanning rows once,
opening an index iterator hundreds of thousands of times, and building then probing a hash table. It also loses
Frontier state when `UNION` branches expose different variables and optimizes dependent `EXISTS` subqueries after the
parent winner has already been chosen.

After this change, logical cardinality and physical work are separate. Frontier sketches estimate how many mappings
survive joins, optional joins, semi-joins, and anti-joins. A primitive physical vector estimates how those mappings are
produced. Pure `EXISTS`, `NOT EXISTS`, and safe `MINUS` shapes receive streaming, memoized, and materialized physical
alternatives. LIBRARY q10 demonstrates the outcome: its five correlation keys no longer cause approximately 772,600
independent right-hand-side iterator openings, while its result count and logical cardinality estimates remain
unchanged.

## Progress

- [x] (2026-07-30 10:15Z) Read the packed-planner, performance, Maven, and query-plan workflow instructions.
- [x] (2026-07-30 10:15Z) Complete the required offline root clean install; `BUILD SUCCESS` in 35.069 seconds.
- [x] (2026-07-30 10:18Z) Add and capture the first failing packed physical-cost contract in
  `initial-evidence.txt`.
- [x] (2026-07-30 10:38Z) Implement primitive physical cost storage, scoped composition, scoring, deterministic
  winner ordering, and generic dependent-subquery charging.
- [x] (2026-07-30 10:46Z) Add and capture the failing heterogeneous-`UNION` Frontier contract.
- [x] (2026-07-30 10:46Z) Preserve exact and sampled Frontier state through heterogeneous `UNION`, `OPTIONAL`,
  `NOT EXISTS`, and LEO calibration.
- [x] (2026-07-30 11:18Z) Add and capture failing semi/anti normalization, composed-RHS, execution, and q10-shaped
  planning contracts.
- [x] (2026-07-30 11:35Z) Introduce typed packed semi/anti operators, exact correlation profiles, and streaming,
  memoized, and materialized physical vectors.
- [x] (2026-07-30 11:35Z) Generalize runtime direct-`EXISTS` evaluation to positive and negative memoized,
  materialized, and adaptive execution.
- [x] (2026-07-30 12:07Z) Preserve explicit physical scope through metadata copies, complete deterministic
  winner ordering, stamp adaptive break-even thresholds, and add completed semi/anti runtime observations.
- [x] (2026-07-30 12:07Z) Apply learned-filter and LEO evidence sequentially and replace confidence interpolation
  with one Jeffreys posterior over Frontier and learned pseudo-counts.
- [x] (2026-07-30 16:20Z) Separate scalar-filter, semi/anti, and LEO evidence, version their persistence, and
  publish planned/actual physical telemetry.
- [x] (2026-07-30 16:45Z) Verify LIBRARY q10 plan, counters, results, benchmark, held-out accuracy, and affected
  module suites. The repository audit harness explicitly reports plan regret as not measured; see the validation
  note below.
- [x] (2026-07-30 18:19Z) Preserve rare exact UNION relations, compress exact high-fanout OPTIONAL expansions,
  prove exact correlation-key domains through sampled lineage, and re-cost contextualized selected MINUS plans.
- [x] (2026-07-30 18:19Z) Re-run focused contracts, query-evaluation and LMDB modules, held-out accuracy, the
  supported q10 benchmark, and cold-fallback q10 plan capture on the final source.
- [x] (2026-07-30 22:20Z) Fix the late HIGHLY_CONNECTED q10 execution fallback with parameter-partitioned
  materialization and preserve every upstream correlation binding in the shared cache key.
- [x] (2026-07-30 22:20Z) Pass the final 1,740-test LMDB gate and classify all final Theme corpus changes above
  20 percent against develop.
- [x] (2026-07-31 11:40Z) Reproduce SOCIAL_MEDIA q4 with execution logging and classify the regression as
  correlation-sensitive physical costing plus incomplete materialized-scan feedback.
- [x] (2026-07-31 11:40Z) Complete the required JDK 25 offline root clean install; `BUILD SUCCESS` in 34.275 seconds.
- [x] (2026-07-31 15:42Z) Add and capture focused failing q4 planning, source-scan telemetry, and
  feedback-migration contracts in `initial-evidence.social-media-q4.txt`.
- [x] (2026-07-31 15:42Z) Price semi/anti candidates from distinct Frontier probe, hash, and
  materialization-parameter domains, including bounded exact finite-relation refinement.
- [x] (2026-07-31 15:42Z) Record leaf-most RHS scan work, persist version-14 physical observations, and publish
  candidate-specific access telemetry without public API changes.
- [x] (2026-07-31 15:42Z) Verify focused contracts, both affected modules, q4 snapshots, and the full SOCIAL_MEDIA
  benchmark corpus.

## Surprises & Discoveries

- Observation: The current scalar composition uses `max(childCost, providerWork)` for a provider that does not replace
  child work. That can hide a dependent subquery's local opens and seeks instead of charging them.
  Evidence: `PackedIncumbentSearch` and `PackedSelectedPlanContextualizer` both contain this composition shape.

- Observation: The Frontier linear kernels already implement the statistically important part of semi/anti
  estimation: weighted matched and unmatched outer atoms. The primary gap is preserving compatible state and
  translating it into physical probe counts, not replacing Frontier.
  Evidence: `FrontierLinearTransforms.resolveProjectedOuterKernel` computes exact, semi, and anti retained mass.

- Observation: LIBRARY q10 has approximately 772,600 outer mappings but only five correlation keys. A per-row
  correlated `NOT EXISTS` therefore changes the asymptotic operation count even when its output-row estimate is
  reasonable.
  Evidence: the existing q10 benchmark/plan investigation reported approximately 772,600 iterator opens for the
  correlated plan versus a one-time right-side scan of approximately 45,300 rows on develop.

- Observation: A compound `EXISTS` subquery was optimized and retained in `PackedDependentPlans`, but its winner cost
  never reached the owning filter or its ancestors.
  Evidence: the new contract expected 111 work units from 10 outer + 1 filter + 100 dependent work, while the
  pre-change planner returned 11.

- Observation: Heterogeneous exact `UNION` inputs already carried compatible database-exact measures, but
  `combinedOperatorStateKey` required byte-identical layouts and mask strata.
  Evidence: the failing contract reported `plannedFrontierFallbackReason=union_state_unavailable` and selected
  `lmdb-finite-alternative-surface` for a two-layout, three-row union.

- Observation: The correlated-filter enumerator could install a scalar `NOT EXISTS` winner before charging its
  dependent RHS. Contextualization charged the RHS only after the physical winner had been fixed, so a 1,783-unit
  incomplete scalar appeared cheaper than the approximately 3,045-unit bounded anti-join.
  Evidence: the focused winner-selection contract failed with no `optimizer.semiAntiAlgorithm`; the q10-shaped
  explanation retained the scalar filter despite exact five-key Frontier evidence.

- Observation: LMDB's old exact-local filter gate rejected every function call, including deterministic local
  `CONTAINS(LCASE(STR(...)))`. That discarded otherwise exact q10 correlation evidence.
  Evidence: allowing scalar expressions classified as reorder-safe changed q10's anti profile from degraded fallback
  to exact outer/matched/unmatched mass with five exact keys.

- Observation: Physical vectors were correct when first produced but their scope and dependent-cost marker were lost
  while copying metadata into contextual candidates and restored winners.
  Evidence: the focused metadata-copy contract observed an explicit local vector return as a scalar-inclusive vector.

- Observation: The learned-filter path explicitly skipped LEO whenever learned evidence created a calibrated state.
  Evidence: the orthogonal-fusion contract reported `lmdb-frontier+learned-filter` and no
  `plannedFrontierLeoRows` after four completed residual observations.

- Observation: Confidence interpolation treated approximately three effective Frontier particles and 100 learned
  outcomes as comparable point estimates, producing approximately 47 rows.
  Evidence: the Jeffreys contract combines the same evidence as pseudo-counts and produces approximately 21 rows.

- Observation: Selected-plan contextualization could replace a typed anti-join's physical estimate with a refined
  estimate of its canonical logical `MINUS`, losing the marker that the selected implementation had already costed
  its dependent RHS.
  Evidence: the focused contract observed an eight-unit typed anti-join become 10,058 units after its 10.1K RHS
  recipe was charged a second time. Preserving the selected physical ownership marker restores the eight-unit cost.

- Observation: The exact q10-shaped planning contract retains five correlation keys, while a cold standalone
  snapshot can conservatively degrade its planned Frontier state before the synopsis is warm.
  Evidence: the exact integration contract reports five database-exact keys; the final cold snapshot reports
  `minus_left_state_unavailable` but still selects `materialized-hash` and executes with five actual keys and one RHS
  iterator opening.

- Observation: A bounded exact alternative relation was previously used only as a scalar cardinality, so a rare
  five-row heterogeneous UNION relation could still disappear from a sampled synopsis.
  Evidence: the failing q10 contract retained the scalar UNION cardinality but lost all branch tuples and therefore
  could not carry correlation through OPTIONAL. Promoting the finite relation into a database-exact Frontier state
  restores all five keys without special-casing q10.

- Observation: Exact outer expansion stopped when output fanout exceeded the refinement budget, even when the number
  of distinct outer mappings was small enough to probe exactly.
  Evidence: two exact outer mappings with fanout 100 and a four-particle budget degraded to `UNRESOLVED`. Stratified
  per-outer reservoir retention now preserves every outer key and the exact total mass while bounding particles.

- Observation: Reservoir compression changes the state guarantee but need not destroy an exact correlation-key proof.
  Evidence: the nearest database-exact ancestor has exactly five possible branch keys and all five survive in the
  retained strata. The correlation profile now reports lower, estimate, and upper bounds of five even though the
  expanded state is measure-unbiased.

- Observation: Contextual refinement of the canonical `Difference` updated cardinality but restored the selected
  typed anti-join's stale scalar correlation and physical metrics.
  Evidence: after learned-filter calibration the anti state was correct but its distinct estimate was absent.
  Re-costing the already-selected physical algorithm from the refined Frontier state publishes five keys and bounded
  materialized work.

- Observation: The old sampled-UNION/LEO contract became exact after bounded exact alternative promotion.
  Evidence: its 192-row fixture fit the 4,096-row exact scan bound and correctly rejected LEO cardinality correction.
  Enlarging the fixture just past the bound keeps the named test sampled and continues to verify Frontier then LEO.

- Observation: The final q10 implementation remains structurally stable and below the requested runtime significance
  threshold.
  Evidence: the supported forked JMH run is 168.334 ms/op versus the accepted 153 ms/op (approximately +10%). The
  cold snapshot consumes 772,600 outer rows, emits 618,400 rows, observes five keys, opens the materialized RHS once,
  and retains a stable optimized structure hash.

- Observation: The held-out accuracy harness remains within the requested q-error gates, but this repository has no
  bounded-oracle plan-regret implementation.
  Evidence: the full LMDB run reports `p95QError=1.523191`, `worstQError=2.030921`, `falseZeros=0`, and
  `optimizerRegret=not-measured`.

- Observation: Several old plan tests encoded a particular physical tree position rather than query semantics or
  bounded-work invariants.
  Evidence: HIGHLY_CONNECTED q10, MEDICAL q7, and the cursor-skip test remained semantically correct after typed
  anti-join planning but failed legacy implementation-order assertions. Their assertions now identify the intended
  operation structurally and check the bounded algorithm/result contract.

- Observation: Planning a materialized anti-join did not guarantee bounded execution when the RHS referenced an
  outer-only parameter.
  Evidence: HIGHLY_CONNECTED q10 selected materialized hash but the executor rejected the specialization because
  `?threshold` was not an RHS output, then rebuilt a hinted RHS hash for every outer row. Partitioning the materialized
  cache by outer-only parameters reduced the supported benchmark from approximately 15.9 seconds to 0.56 seconds.

- Observation: The final Theme corpus exposes one remaining conservative-fallback weakness rather than an unbounded
  execution bug.
  Evidence: when no usable Frontier correlation state survives, scalar costing sometimes treats a bound probe as
  global RHS work. SOCIAL_MEDIA q4 consequently materializes approximately 143,700 RHS rows for five exact outer
  keys, while HIGHLY_CONNECTED q4 materializes approximately 272,200 rows. These plans are bounded and correct, but
  slower than develop's cheap correlated lookups.

- Observation: Most percentage regressions are not semi/anti regressions.
  Evidence: 16 of the 20 greater-than-20-percent cases strengthen a null-rejected `OPTIONAL` to an inner join and
  subsequently choose a different join order. Eleven of the 20 add less than 11 milliseconds. HIGHLY_CONNECTED q6 and
  ELECTRICAL_GRID q10 retain the same categorical tree and show only local ordering/run variance.

- Observation: SOCIAL_MEDIA q4 has five exact unmatched correlation keys, but its cold streaming cost multiplies
  every miss by the global 143,700-row RHS estimate.
  Evidence: the logged run prices streaming at approximately 718,700 work rows, selects materialized hash, and scans
  the POSC predicate prefix once; the bound shape is an SPOC direct lookup over subject, predicate, and object.

- Observation: A materialized RHS that scans 143,700 source rows and emits zero rows trains the current feedback
  surface as zero sequential work.
  Evidence: q4 reports zero RHS rows examined and zero hash-build rows even though its child statement telemetry
  records the full POSC scan. The observation carrier currently has no separate source-scan counter.

- Observation: One nested access stamp cannot describe both semi/anti candidates.
  Evidence: q4's filter compares bound SPOC probes with an unbound POSC materialization, while the selected child
  stamp exposes only the materialized context.

- Observation: A freshly bulk-loaded one-off snapshot can conservatively select q4's streaming candidate before the
  persisted Frontier query index is ready, while the benchmark's persisted synopsis proves the same five-key domain
  exactly.
  Evidence: the CLI snapshot reports `query_index_unavailable`, five actual correlation keys, and five SPOC lookups;
  the supported benchmark plan reports `plannedCorrelationDistinctKeys=5`,
  `plannedCorrelationGuarantee=database_exact`, and `plannedCorrelationConfidence=1.00`.

- Observation: Correcting proof identity for assured-shared `MINUS` also removes the same materialization failure from
  SOCIAL_MEDIA q1 and q7.
  Evidence: their pre-change plans contain a selected `Difference`; their final plans contain a correlated
  `Filter(Not(Exists))`, and the full corpus moves from 7.300 to 0.899 ms/op and 7.721 to 0.371 ms/op respectively.

## Decision Log

- Decision: Keep Frontier as the source of logical rows and introduce a separate physical-cost vector.
  Rationale: Cardinality answers "how many mappings survive"; physical cost answers "which operations produce them."
  Combining those concepts in `workRows` caused the regression.
  Date/Author: 2026-07-30 / Codex

- Decision: Store physical dimensions in parallel primitive arrays and use primitive scratch carriers.
  Rationale: Packed planning is a hot, allocation-sensitive path; object-per-candidate vectors would undo the packed
  representation's locality and allocation benefits.
  Date/Author: 2026-07-30 / Codex

- Decision: Use explicit `LOCAL` and `INCLUSIVE` scopes and remove `max` composition.
  Rationale: Local work must add to child work, while a complete-subtree provider must replace it. Those are different
  semantics and cannot be inferred from magnitudes.
  Date/Author: 2026-07-30 / Codex

- Decision: Retain existing scalar APIs as compatibility adapters and keep `PackedCostModel` functional.
  Rationale: The redesign is internal and must not break existing providers or public query algebra.
  Date/Author: 2026-07-30 / Codex

- Decision: Introduce packed-only semi/anti opcodes rather than public algebra nodes.
  Rationale: They represent planner alternatives. Materialization can map them back to `Filter`, `Exists`, `Not`, and
  `Difference`, preserving RDF4J's external algebra contract.
  Date/Author: 2026-07-30 / Codex

- Decision: Cut over each completed milestone immediately rather than maintaining a dual scorer.
  Rationale: The requested rollout prioritizes accuracy and a single explainable decision path.
  Date/Author: 2026-07-30 / Codex

- Decision: Represent branch alignment as a replayable Frontier `ALIGN` operation.
  Rationale: Widening a branch is not an ordinary projection: absent variables must remain unbound and the target
  strata may include masks contributed by sibling branches. Explicit provenance also lets alternate design-lane
  replay reconstruct the same aligned union.
  Date/Author: 2026-07-30 / Codex

- Decision: Require providers that cost embedded subqueries inside an operator vector to declare that fact explicitly.
  Rationale: `LOCAL` versus `INCLUSIVE` describes composition with relational children; it cannot say whether an
  embedded scalar subquery was already included. The explicit marker prevents both missing RHS work and double charge.
  Date/Author: 2026-07-30 / Codex

- Decision: Treat LEO as a residual layer whose baseline is the state after learned-filter calibration.
  Rationale: LEO observations contain actual/planned ratios. Applying that ratio to raw Frontier evidence after a
  learned selectivity correction either suppresses LEO or double-corrects cardinality.
  Date/Author: 2026-07-30 / Codex

- Decision: Fuse sampled Frontier and learned-filter observations as pass/fail pseudo-counts under one Jeffreys prior.
  Rationale: Evidence mass, rather than independently computed confidence weights, determines how much each source
  contributes and guarantees each source is incorporated exactly once.
  Date/Author: 2026-07-30 / Codex

- Decision: Preserve a selected physical implementation's dependent-subquery ownership across contextual refinement
  of its canonical logical source.
  Rationale: Contextual refinement may improve rows and physical dimensions, but it cannot revoke the physical
  implementation's declaration that embedded RHS work is already included. Losing that declaration double-charges
  typed semi/anti plans.
  Date/Author: 2026-07-30 / Codex

- Decision: Update stale plan-shape assertions when they require a legacy physical ordering without expressing a
  semantic, cardinality, or bounded-work invariant.
  Rationale: Typed semi/anti alternatives intentionally change physical structure. Tests should protect results,
  SPARQL semantics, correlation evidence, and execution bounds rather than freeze the superseded implementation.
  Date/Author: 2026-07-30 / Codex

- Decision: Complete exact per-outer probes and compress their output with a stratified reservoir when fanout, rather
  than distinct outer keys, exceeds the particle budget.
  Rationale: Stopping mid-expansion biases complex join and semi/anti evidence toward early outer mappings. One
  stratum per exact outer key retains correlation coverage while scaled weights preserve exact total mass.
  Date/Author: 2026-07-30 / Codex

- Decision: Prove distinct-key bounds from exact unary lineage independently of the expanded state's overall
  guarantee.
  Rationale: Sampling fanout does not make a fully observed finite key domain unknown. Keeping the two guarantees
  separate preserves an exact physical memoization/materialization decision without overstating tuple-level accuracy.
  Date/Author: 2026-07-30 / Codex

- Decision: Partition an explicitly selected materialized semi/anti cache by every outer-only RHS parameter.
  Rationale: Such parameters change RHS contents and therefore cannot share one hash, but they also do not require
  falling back to one RHS execution per outer row. A parameter-keyed partition preserves semantics and bounds work by
  distinct parameter values.
  Date/Author: 2026-07-30 / Codex

- Decision: Model probe/cache keys, shared hash keys, and materialization parameters as separate Frontier domains.
  Rationale: Runtime uses these domains for different operations. Collapsing them into shared RHS output names loses
  outer-only parameters and cannot price bound probes or partitioned builds correctly.
  Date/Author: 2026-07-31 / Codex

- Decision: Use cheap conditional factor costing first and invoke bounded exact finite-surface refinement only when
  the two best candidates are within ten percent.
  Rationale: Bound index-prefix evidence removes q4's asymptotic error without eager store probes. Exact correlated
  tuple probes remain useful for close decisions and stay within the existing finite-surface budgets.
  Date/Author: 2026-07-31 / Codex

- Decision: Keep RHS result rows and underlying source rows as separate physical observations.
  Rationale: Result rows determine hash-build work; source rows determine sequential access work. Empty-result scans
  must not be learned as free.
  Date/Author: 2026-07-31 / Codex

- Decision: Preserve the public semi/anti observation carrier and version only LMDB's internal feedback format.
  Rationale: The correction is store-internal. Version 14 can add physical evidence while versions 12 and 13 remain
  readable without widening a stable query-evaluation API.
  Date/Author: 2026-07-31 / Codex

- Decision: Preserve assured-shared `MINUS` as semantic proof identity while leaving its physical algorithm to the
  LMDB candidate coster.
  Rationale: Proof-aware costing must distinguish safe `MINUS` from an arbitrary scalar `Difference`, but globally
  stamping materialization would override the candidate comparison. For example, MEDICAL q7's 868 bound SPOC probes
  are cheaper than its 16,700-row POSC materialization, so the correct physical choice is streaming.
  Date/Author: 2026-07-31 / Codex

## Outcomes & Retrospective

Milestone 1 is complete. Packed cost slots and metadata now carry nine primitive physical dimensions. Explicit local
operator work composes additively with inclusive children, peak memory uses maximum, arithmetic saturates, and a
query-local objective hook ranks the composed vector. Exact ties prefer the original lower-rule-rank implementation.
Compound dependent subqueries now contribute their selected winner cost to their owning filter. The focused physical
cost contracts pass 5/5, winner-table contracts pass 4/4, and the existing packed search suite passes 58/58.

Milestone 2's branch-alignment slice is complete. Exact and sampled `UNION` branches now align to a common sorted
layout and common canonical mask strata. Variables absent from a branch receive term id zero only in an explicitly
unbound stratum, so no value is invented. Sampling identity and estimator semantics must match before states combine.
Focused contracts show exact heterogeneous union cardinality 3, exact downstream `OPTIONAL` cardinality 3 and
`NOT EXISTS` cardinality 2, and successful sampled union calibration through LEO.

Milestones 3 and 4 have reached their first end-to-end slice. Pure safe forms generate packed-only typed alternatives
for streaming, memoized, and materialized execution, while unsafe forms retain their public algebra. Exact Frontier
profiles retain outer multiplicity while deduplicating five q10 correlation keys. Runtime evaluation supports positive
and negative memoized/materialized semantics with shared key caches. The active q10-shaped contract is green with
`UNION=10`, `OPTIONAL=10,000`, anti-output `8,000`, five exact keys, a bounded algorithm, and the correct result count.
Dependent-subquery cost now participates before winner selection, and an explicit provider marker prevents LMDB's
semi/anti vector from being charged twice.

The physical metadata and execution follow-through are also complete. Local/inclusive scope survives arena and winner
copies, winner ties use expected, worst-case, startup, peak-memory, original-plan, and stable-id ordering, and adaptive
execution consumes the planner-stamped break-even threshold. Completed positive and negative executions report a
separate per-key semi/anti observation; early-closed executions do not train it.

The ordinary-filter fusion slice is complete. Sampled Frontier effective sample size and learned pass/fail evidence
form one Jeffreys posterior with published mean and interval. LEO consumes the post-posterior estimate as its baseline,
so telemetry exposes raw Frontier, post-learned, and final residual-calibrated rows separately. Database-exact
Frontier cardinality remains protected.

All implementation milestones are complete. The current full query-evaluation module passes 1,051 tests. The final
LMDB run passes 1,729 unit tests with no failures or errors, followed by the selected integration tests with no
failures or errors. The held-out audit passes the requested p95 and worst q-error limits with no authoritative false
zeros.

LIBRARY q10 now executes its anti-join as a materialized hash operation. The measured run consumes approximately
772,600 outer rows, emits 618,400 rows, identifies five distinct correlation keys, builds the one-row RHS once, and
opens that RHS iterator once rather than once per outer mapping. Its result count and physical plan remain stable.
An earlier 128-run ready-store snapshot averaged 161 ms versus 153 ms and reported no plan difference. The final
supported forked JMH measurement is 168.334 ms/op; both remain below the 20-percent significance threshold, so JFR
was not needed.

The historical Theme report dated July 30 predates this implementation and therefore cannot be used as a post-change
full-corpus runtime result. Its greater-than-20-percent classifications remain baseline evidence only. The current
repository also emits `optimizerRegret=not-measured`; consequently the bounded-oracle regret thresholds could not be
certified without first adding a separate oracle runner. This is the only requested validation metric not available
from the repository's existing harness.

The final supported q10 JMH run reports 168.334 ms/op, approximately 10 percent above the accepted 153 ms/op baseline
and below the requested 20-percent significance threshold. The final cold snapshot necessarily records
`DIRTY_INSERTION` and scalar planned correlation bounds while the newly loaded query index is unpublished, but it
still selects materialized hash execution. Runtime telemetry records 772,600 outer rows, 618,400 output rows, five
actual distinct keys, a one-row hash build, and exactly one RHS iterator opening. The separate ready-synopsis
integration contract proves the authoritative Frontier path through heterogeneous UNION, high-fanout OPTIONAL,
learned calibration, and selected-plan contextualization.

The late HIGHLY_CONNECTED q10 audit found and removed a separate execution fallback: materialized execution now
partitions its shared cache by outer-only RHS parameters while retaining all upstream prefix correlations in its key.
The final supported spot check is 564.741 ms/op versus approximately 15.9 seconds before that fix and 162,106 ms/op on
develop. The complete final corpus reports 95 successful scores and the same four inherited timeouts as the
pre-change July 30 run: HIGHLY_CONNECTED q8, ELECTRICAL_GRID q3, and SPARSE q2/q9.

Against the 86 overlapping develop scores, 42 improve by more than 20 percent, 24 stay within 20 percent, and 20
regress by more than 20 percent. Eleven regressions add less than 11 milliseconds. The material regressions are bounded:
PHARMA q6/q7, HIGHLY_CONNECTED q4/q6, MEDICAL q3/q8, TRAIN q8/q9, and ELECTRICAL_GRID q10. Their categorical causes
are null-rejected `OPTIONAL` strengthening plus join reordering, conservative materialized semi/anti fallback, and
PHARMA q7 property-path alternation expansion. HIGHLY_CONNECTED q6 and ELECTRICAL_GRID q10 have no categorical
operator-shape change. LIBRARY q10 is 171.411 ms/op in the final corpus versus 229.521 ms/op on develop, with UNION
10, outer 772,700, anti output 618,500, five-key bounds, and one planned RHS iterator opening.

Milestone 7 closes that fallback. Probe/cache keys, shared hash keys, and outer-only materialization parameters are
independent tuple-preserving domains. Streaming and memoized candidates use bound invocation work; materialization
uses partition-bound access work plus distinct output/build rows and outer hash probes. Exact refinement is
query-local, budgeted, supported-kernel-only, and decision-sensitive. Materialized execution aggregates leaf-most
source-scan deltas, so an empty hash build cannot train a 143,700-row scan as free. LMDB feedback format 14 persists
physical evidence while retaining safe reads of versions 12 and 13.

The final q4 plan has five database-exact keys, one row of bound work per SPOC direct lookup, and a competing
143,700-row POSC prefix scan. It selects and executes `streaming-correlated`, performs five RHS lookups, returns the
unchanged result, and measures 0.154 ± 0.045 ms/op in the focused five-iteration run and 0.143 ± 0.066 ms/op in the
full SOCIAL_MEDIA corpus. The pre-change attachment measured 9.107 ms/op, so q4 improves by 98.4 percent.

No current SOCIAL_MEDIA point regresses against the attached pre-change corpus. q1 and q7 additionally replace
materialized safe-`MINUS` plans with correlated probes and improve by 87.7 and 95.2 percent. Every other query retains
the attachment's plan structure. Against `develop`, q0, q2, q3, q4, q6, and q9 are slower by more than 20 percent, but
q0, q2, q3, q6, and q9 are unchanged from the attachment and therefore predate this milestone. q4 is 0.046 ms/op
slower in absolute terms, uses the same correlated strategy class as `develop`, and remains comfortably below its
0.5 ms/op acceptance ceiling. q1, q5, q7, q8, and q10 are faster than `develop` by more than 20 percent.

## Context and Orientation

The packed planner is in `core/queryalgebra/evaluation`. `PackedRelOp` encodes logical operators as integer opcodes.
`PackedMemo` stores equivalent expressions, `PackedIncumbentSearch` creates an initial implementation, and
`PackedWinnerTable` selects the lowest comparison cost. `PackedPhysicalMetadataArena` stores the primitive metadata
for each physical expression. `PackedPlanMaterializer` converts the selected packed expression back into RDF4J query
algebra.

`PackedCostModel` is a functional extension point. It creates a query-local `PackedCostSession`, which fills a mutable
`PackedCostEstimate`. Today that estimate contains logical rows and scalar work. `PackedSelectedPlanContextualizer`
re-estimates the selected tree with parent bindings and separately optimizes dependent subqueries such as `EXISTS`.

LMDB's provider is in `core/sail/lmdb`. `LmdbFrontierPackedCostSession` maintains Frontier evidence states. A Frontier
state is a compact weighted set of retained binding tuples plus masks describing which variables are bound.
`FrontierLinearTransforms` applies joins and outer, semi, and anti kernels to those weighted tuples.

At runtime, `FilterIterator` recognizes a direct `Exists` expression and can use
`MaterializedExistsFilterIteration`. `MinusQueryEvaluationStep` separately implements SPARQL `MINUS`. This work
generalizes those execution choices without weakening SPARQL compatibility for unbound variables.

Learned filters and LEO are both local feedback mechanisms. A learned filter estimates selectivity for a repeatable
expression. LEO records the residual between a plan estimate and completed execution. They must refine different
layers: learned selectivity follows the raw Frontier transform, and LEO calibrates the remaining error, including
physical work, without overriding exact Frontier cardinalities.

## Plan of Work

Milestone 1 introduces physical-cost semantics. Add failing packed-planner tests proving that local work adds to both
children, inclusive work replaces children, peak memory uses maximum rather than sum, saturating arithmetic is safe,
dependent expression work is visible before winner selection, and exact ties prefer the original implementation.
Extend `PackedCostEstimate` with nine primitive dimensions and a `CostScope`. Extend
`PackedPhysicalMetadataArena` with parallel arrays for expected, upper-bound, startup, and memory scores plus the
dimensions. Migrate internal cost providers to explicit local or inclusive setters. Preserve `setRows` and functional
model compatibility by mapping scalar work to inclusive sequential work. Add a default scoring hook to
`PackedCostSession`; LMDB sums normalized operation counts. Replace all `max(child, provider)` composition with
saturating vector composition.

Milestone 2 preserves Frontier state through heterogeneous unions. Add failing exact and sampled tests where branches
have overlapping but non-identical variable layouts, followed by projection, `OPTIONAL`, and anti transforms. Build a
common union layout, remap branch masks and tuples, represent absent branch variables as unbound, and merge only states
with compatible sampling identities. Add a query-local correlation profile keyed by state, correlated mask, right
relation, and kernel. Exact states publish exact key counts. Sampled states publish retained keys as a lower bound and
outer rows as an upper bound, leaving the central NDV unknown. Deduplicate exact planning probes with a primitive
open-addressed table while keeping each key's outer multiplicity.

Milestone 3 adds typed semi/anti planning. Add packed-only `SEMI_JOIN` and `ANTI_JOIN` opcodes and a payload containing
semantic origin, correlated variables, and assured-binding proof. Normalize only pure `Filter(Exists(rhs))`, pure
`Filter(Not(Exists(rhs)))`, and `Difference` with non-empty shared variables assured on both sides. Leave compound,
non-deterministic, service, and unsafe-minus expressions in their original form. Produce streaming-correlated,
memoized-correlated, and materialized-hash implementations with one logical Frontier output estimate and distinct
physical vectors. Materialize the winning packed form back to existing public algebra with an internal algorithm hint.

Milestone 4 generalizes execution. Replace the direct materialized-EXISTS specialization with an adaptive iterator
supporting positive and negative results. Its compiled step owns a cache keyed by bound mask and values. The planner
stamps the break-even point computed from build, probe, open, seek, and memory cost. The legacy fixed threshold is used
only if no metadata exists. The iterator may progress from streaming to memoized or materialized work, but it must
retain current wildcard compatibility, memory caps, overflow fallback, cancellation, and exception behavior.

Milestone 5 makes evidence orthogonal. Ordinary sampled filters combine Frontier effective sample size and decayed
learned counts once under a Jeffreys `Beta(0.5, 0.5)` prior. Semi/anti evidence receives a separate key and observation
surface containing distinct-key hits, misses, repeats, probe depth, exhausted failures, builds, probes, algorithm, and
completion status. Partial, cancelled, remote, and non-deterministic observations do not train. Apply LEO after either
specialized learned layer and key physical residuals by operator, algorithm, binding shape, and estimator version.
Protect exact cardinality but allow bounded physical-cost calibration after existing confidence gates. Bump
persistence and plan-cache versions, retain legacy reads as fallback, and write only the new format.

Milestone 6 validates the design end to end. Capture a LIBRARY q10 plan snapshot, verify its result count and
intermediate Frontier estimates, and prove that right-side openings are bounded by distinct-key or one-time-build work
rather than outer rows. Run the single-query benchmark, the Theme history analyzer, held-out accuracy and plan-regret
checks, the LMDB suite, and the affected query-evaluation suites. Use JFR only if the operation counters do not explain
the remaining runtime.

Milestone 7 closes the conservative bound-probe fallback exposed by SOCIAL_MEDIA q4. Add failing contracts for an
exact five-key self-loop anti-probe, empty-result materialization scan accounting, and legacy feedback migration.
Derive separate Frontier domains for bound probes, shared hash keys, and outer-only materialization parameters. Price
streaming and memoized alternatives with a conditionally bound RHS factor and price materialization with its
partition-bound access factor plus distinct RHS output/build rows. Preserve exact correlated tuples in a
`FiniteRelationEstimate` and invoke the bounded finite-surface estimator only for close decisions. Record leaf-most
RHS source-scan deltas, bump LMDB feedback persistence to version 14, and publish candidate-specific access telemetry
without changing public query-evaluation interfaces.

## Concrete Steps

Run all commands from the repository root. Before production edits, create and run the smallest failing test with:

    python3 .codex/skills/mvnf/scripts/mvnf.py <ClassName>#<methodName> --retain-logs

The runner performs the required install and then a test selection without Maven `-am` or `-q`. Immediately preserve
the first failure:

    python3 scripts/agent-evidence.py --command "<exact mvnf command>" \
      --log logs/mvnf/<verify-log> \
      <module>/target/surefire-reports <module>/target/failsafe-reports > initial-evidence.txt

After each root-cause slice, rerun the exact same selector and expect it to pass. Broaden to its test class, then its
module. Check source headers before formatting and run the repository formatter only after the focused tests pass.

Capture q10 with:

    ./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh \
      --log /tmp/library-q10-candidate.log -- \
      --store lmdb --theme LIBRARY --query-index 10 --query-id library-q10 \
      --compare-latest --diff-mode structure+estimates

Run its benchmark with:

    scripts/run-single-benchmark.sh --theme-plan-run --theme-query LIBRARY:10

Analyze current history with:

    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/\
    analyze-theme-query-history.sh --sort-regressions

Capture and benchmark the Milestone 7 target with:

    ./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh \
      --log /tmp/social-media-q4-candidate.log -- \
      --store lmdb --theme SOCIAL_MEDIA --query-index 4 --query-id social-media-q4 \
      --compare-latest --diff-mode structure+estimates

    scripts/run-single-benchmark.sh --theme-plan-run --theme-query SOCIAL_MEDIA:4 \
      --warmup-iterations 1 --measurement-iterations 5 --forks 1

## Validation and Acceptance

Cost-vector tests must show that local work is the saturating sum of child and operator dimensions, inclusive work does
not double-count children, peak memory is the maximum live requirement, scalar providers retain their old logical row
behavior, and deterministic ties no longer prefer a transformed plan solely because it has a higher rule rank.

Frontier tests must show that exact and sampled heterogeneous unions retain valid states across projection,
`OPTIONAL`, semi, and anti transforms. Missing branch variables are unbound, not fabricated values. Incompatible
sampling states fall back conservatively and never become an authoritative zero.

Semi/anti tests must cover legal and illegal normalization, duplicate-heavy outer input, empty and large right inputs,
partially bound and wildcard keys, no shared variables, compound Boolean filters, nested unions and optionals,
materialization overflow, early close, and exceptions. Existing SPARQL `MINUS` fuzz and conformance tests must remain
green.

For the benchmark fixture, LIBRARY q10 must keep approximately 10 union rows, 772,600 optional rows, and 618,400
anti-output rows, with five exact correlation keys. It must choose memoized or materialized anti execution and must not
open the right input once for every outer row. Its result count must match develop.

Held-out cardinality accuracy must remain at p95 q-error no greater than 1.53 and worst q-error no greater than 2.05.
Plan regret must remain at p95 no greater than 2x with no query above 10x the bounded oracle. Classify every Theme
runtime regression above 20 percent. Outside q10, runtime movement is informational unless it is catastrophic,
unbounded, or accompanied by correctness or accuracy failure.

For Milestone 7, cold and warmed SOCIAL_MEDIA q4 must select and execute `streaming-correlated`, retain five exact
probe keys, expose bound SPOC direct lookup versus POSC prefix-scan candidate telemetry, and perform no more than five
RHS lookups. Its result count must remain unchanged and its supported JMH point estimate must be below 0.5 ms/op.
Structural operation counters are the primary non-flaky gate. Empty-result materialization must report the underlying
source scan, and legacy version-13 physical counters must not override cold costs. Existing beneficial materialization
for HIGHLY_CONNECTED q10 and LIBRARY q10 must remain intact.

## Idempotence and Recovery

All focused tests, snapshots, and benchmark commands are safe to repeat. Keep generated reports, logs, benchmark
results, and other untracked artifacts. Never reset or clean the user's working tree. If offline Maven resolution
fails, rerun the exact command once without `-o`, then return to offline mode. If a behavior-changing production edit
is made before its failing test is captured, revert only that edit with `apply_patch`, restore the failing-test-first
state, and rerun the selector.

## Artifacts and Notes

The clean-install baseline is in `maven-build.log`:

    [INFO] BUILD SUCCESS
    [INFO] Total time: 35.069 s (Wall Clock)

Store the first focused failure in `initial-evidence.txt`. Retain focused Maven logs under `logs/mvnf`, q10 snapshots
under `/tmp`, and any JFR recording under the benchmark wrapper's reported path.

## Interfaces and Dependencies

`PackedCostModel` remains a functional interface. `PackedCostEstimate` gains explicit primitive local/inclusive cost
accessors and a cost-scope enum. `PackedCostSession` gains a default objective-scoring method. Scalar accessors remain
available as compatibility adapters.

`PackedPhysicalMetadataArena` owns the vector columns and scalar comparison projections. `PackedWinnerTable` consumes
the expected, upper-bound, startup, and peak-memory projections; it does not allocate cost-vector objects.

`EvaluationStatistics` gains a default no-op semi/anti outcome-recording method and immutable observation carrier.
LMDB supplies the persistent implementation. Packed semi/anti opcodes and payloads remain internal; no public query
algebra node is added.

No dependency is added. The implementation targets the repository's JDK 25 environment and uses existing primitive
packed structures, LMDB statistics, Maven runners, snapshot tooling, and benchmark harness.

Revision note (2026-07-30 10:38Z): Recorded the completed physical-vector milestone, its dependent-subquery discovery,
and focused validation before starting Frontier `UNION` work.

Revision note (2026-07-30 10:46Z): Recorded heterogeneous-`UNION` alignment, replay provenance, exact downstream
correlation, and sampled LEO validation.

Revision note (2026-07-30 16:45Z): Recorded completed evidence fusion and telemetry, the selected-plan
dependent-cost ownership fix, full module verification, q10 structural/runtime evidence, held-out q-error results,
and the unavailable bounded-oracle regret metric.

Revision note (2026-07-30 18:19Z): Recorded exact alternative-state promotion, stratified high-fanout expansion,
exact key-domain lineage proof, contextual MINUS re-costing, the final module runs, and final q10 benchmark/snapshot
evidence.

Revision note (2026-07-30 22:20Z): Recorded parameter-partitioned materialized execution, the upstream-correlation
cache-key fix, final module verification, the complete post-change Theme corpus, and all greater-than-20-percent
develop comparisons.

Revision note (2026-07-31 11:40Z): Added Milestone 7 for conditional Frontier probe costing, source-scan feedback,
candidate-specific access telemetry, SOCIAL_MEDIA q4 validation, and internal feedback-format version 14.

Revision note (2026-07-31 15:42Z): Completed Milestone 7, including tuple-preserving Frontier correlation domains,
bound candidate costing, decision-sensitive exact refinement, leaf source-scan feedback, version-14 persistence,
candidate-specific telemetry, safe-`MINUS` proof identity, full module verification, q4 snapshot/benchmark evidence,
and the greater-than-20-percent SOCIAL_MEDIA comparison.

Revision note (2026-07-31 20:33Z): Linked the follow-on Frontier continuity, event-sourced costing, cross-generation
plan-cache validation, and state-specific LEO work in
`.agent/execplans/GH-0000-frontier-state-continuity-cache-and-leo.md`. This completed plan remains the prerequisite
physical-cost and semi/anti foundation; the follow-on owns all new implementation and progress.
