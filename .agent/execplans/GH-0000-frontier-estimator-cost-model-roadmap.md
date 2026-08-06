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
- [ ] **[in_progress] Milestone 0:** freeze the complete interface, scalar-boundary, rule, physical-alternative,
  evidence, learning, and test inventory; turn every known semantic or Bellman counterexample into a cataloged red
  contract before production changes.
- [ ] Milestone 1: finish the lossless packed representation, SSA-like value identity, and branch-merge-aware
  binding-flow index.
- [ ] Milestone 2: make the rewrite/optimization catalog closed-world and proof-coupled, then complete semantic and
  predicate-range rewrite closure before join enumeration.
- [ ] Milestone 3: standardize state-preserving estimation, finish Frontier composition, and implement the normative
  multidimensional cost algebra.
- [ ] Milestone 4: install exact continuation-equivalence classes, resource-accounted Pareto frontiers, and explicit
  incomplete-search statuses everywhere a winner is retained.
- [ ] Milestone 5: run proof-checked rewrite, hypergraph construction, wide DPhyp, and Cascades reactivation to one
  certified fixpoint across dense, sparse-long, and multiword widths.
- [ ] Milestone 6: integrate LEO-plus learned logical state, physical residuals, uncertainty, and lifecycle decisions
  with the Pareto cost model using the selected NIG-plus-conformal-OOD policy, then decide whether Monitoring can be
  promoted.
- [ ] Milestone 7: make cache replay, plan explanations, decision certificates, and selected recipes lossless under
  the unified state/cost/frontier contracts.
- [ ] Milestone 8: close all 117 semantic and plan-quality cells, then meet the matched planning and execution
  performance gates without heuristic suppression.

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

## Surprises & Discoveries

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
- Observation: Frontier phases 0--4 are implemented, but the on-disk payload block remains version 1 and record-level
  exactness, O2S coordination, paired variance, and audit-lane use are still absent.
  Evidence: `FrontierPayloadBlockWriter.VERSION` is 1; the completed resampling contracts live in
  `FrontierMultinomialResamplerTest` and the Frontier oracle package.
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

## Decision Log

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

## Outcomes & Retrospective

The 2026-08-06 consolidation leaves one optimizer ExecPlan containing the architecture, completed baseline,
unresolved implementation work, dependency order, test workflow, acceptance gates, and durable decisions. The
subordinate packed, Frontier-sketch, state-continuity/cache, learned-feedback rollout, and research hand-off plans
have been absorbed and can be recovered from Git history. This planning change alters no runtime behavior.

The most important lesson from the retired plans is that migration shortcuts must be labeled and removed, not
promoted into permanent contracts. Scalar adapters, one-entry winners, singleton-only DPhyp consumption, incomplete
rewrite coverage, and Monitoring-only learning were useful intermediate states. They are explicitly unfinished here.
Future contributors update this section after every milestone with what changed, what evidence passed, what remains,
and whether any premise in the plan was disproved.

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
Milestone 7 makes decisions durable and explainable. Milestone 8 closes correctness before tuning.

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
conservative full replanning, never semantic guessing. During migration, an explicit whole-session typed fallback may
restart exactly once with the legacy scalar cost session after a declared Frontier session failure; no local operator
may silently discard a supported state, and the final acceptance goal removes avoidable fallback boundaries.

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
