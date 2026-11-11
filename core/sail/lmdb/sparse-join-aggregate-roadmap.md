# Composable sparse aggregation roadmap

This is a design investigation, dated 2026-11-07, rather than an implementation plan or a promise of universal speedups.
The goal is to let eligible sparse regions participate in arbitrary legal query composition while preserving RDF4J's
ordinary evaluator wherever a summary cannot preserve the required bindings, errors, evaluation count or order.
Semantic coverage of all supported functions and aggregates is feasible through that fallback. Executing every one
directly on compressed counts is neither generally possible nor the right acceptance criterion.

## Starting point and measured motivation

The [current implementation](sparse-join-aggregate.md) recognizes two mandatory fixed-predicate planes or two
subject/object-to-predicate projections, with exact bag weights or independently proved DISTINCT incidences.
Every aggregate in its group must be an equivalent non-DISTINCT COUNT. Input DISTINCT, aggregate DISTINCT and
SELECT DISTINCT are different operations. Surviving UNION, OPTIONAL, MINUS and arbitrary grouped subqueries are
outside this sparse recognizer; their elimination by another optimizer is not support for the surviving operator.

The feature audit checked 800 ordered aggregate pairs: for each producer, 16 pairs of eligible COUNT forms executed
forced sparse and 384 declined; all automatic bags matched the native-disabled LMDB control. Longer COUNT lists,
aliases, folded constants and anonymous HAVING aggregates confirmed the same rule. The ten-method green complement
was representative coverage, not an exhaustive proof of all nesting or third-party behavior. Its nested COUNT and
outer UNION/OPTIONAL/SUM examples used alternatives rather than sparse. Pure post-group expressions did preserve a
sparse child in the tested aggregate-root stack.

The measured predicate-incidence candidate on 2026-11-07 completed EXPLORATION Q9 with all 408 groups checked against
an independent raw-statement oracle: automatic 1639.386 ± 39.599 ms/op and forced 1611.533 ± 40.061 ms/op, with actual
`sparseJoinAggregate`/dense telemetry. The matched disabled control timed out at 60 seconds and has no numerical score.
A generated nonempty 16-predicate fixture measured automatic 1.180 ± 0.206 versus disabled 179.095 ± 7.929 ms/op.
These are retained historical JMH results for that candidate, dataset, JVM and configuration, not measurements of this
roadmap. Q11 remained a fixed-plane sparse control; ANALYTICS Q7/Q8 actually executed `irAggregateTypeMatrixParallel`.

The normative baseline is SPARQL 1.1's [algebra](https://www.w3.org/TR/sparql11-query/#sparqlAlgebra),
[aggregate definitions](https://www.w3.org/TR/sparql11-query/#aggregateAlgebra) and
[expression evaluation](https://www.w3.org/TR/sparql11-query/#expressions).
UNION adds solution multiplicities; compatible joins multiply them. MINUS additionally tests intersecting binding
domains. DISTINCT removes duplicates at its own boundary. Aggregates consume their group's expression outcomes,
not merely its final row count. RDF4J extensions, including LATERAL and custom SPI behavior, follow repository contracts.

## Integrate with existing abstractions

The repository already has the required separation between algebra, factorized relations, reducers and semantic rows.
A factorized relation stores correlated alternatives without expanding their Cartesian product; a marginal reader
projects the bindings one consumer needs and carries the contribution of omitted independent factors.
Extend these contracts rather than building a second UNION/OPTIONAL evaluator inside `LmdbSparseJoinAggregatePlan`.

| Existing source, under `src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/` | Contract to retain or extend |
|---|---|
| [LmdbNativeFactorAlgebra](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorAlgebra.java), `open`, `Joined`, `Union` | Algebra boundaries already distinguish additive UNION, OPTIONAL's unmatched mapping and scalar effect barriers. |
| [NativeFactorProjections](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeFactorProjections.java) and [LmdbNativeFactorProjections](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorProjections.java) | One producer runs once; independent demanded readers use bounded windows, exact weights or presence. Additive partitions require global identity deduplication. |
| [NativeReducerCapabilities](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeReducerCapabilities.java) and [LmdbNativeAggregateState](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregateState.java) | Existing membership, merge, spill, weight and encounter-order rules are the authority for every aggregate channel. |
| [LmdbNativePackedAlgebraAggregate](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedAlgebraAggregate.java), `evaluate`, `collectMarginals` | Reuses one algebra input for multiple aggregate projections; already expands opaque callbacks and preserves order-sensitive reducers. |
| [NativePredicateCapabilities](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativePredicateCapabilities.java) and [NativeScalarPlan](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeScalarPlan.java) | Full reads, effects, replay, query scope and worker ownership are independent proofs. |
| [NativeResultCell](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultCell.java) and [NativeLogicalResultTransferPlan](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLogicalResultTransferPlan.java) | Bound, unbound and error cells remain distinct; a physical summary never impersonates an RDF term. |
| [NativeSubqueryPlan](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeSubqueryPlan.java), `withPhysicalPlan`, `forWorker` | Lexical frames, active graphs, input bindings, source authority and worker reconstruction belong to the boundary. |
| [NativeCountGroupStore](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeCountGroupStore.java) and [NativeAggregatePhysicalPlan](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeAggregatePhysicalPlan.java) | Bounded group/distinct events, spill merging, immutable physical recipes and honest work quotes already exist. |

The proposed extension is a capability-bearing relation descriptor shared by these existing producers, not a new
public query API. It records exposed slots, assured-bound slots, term authority, lexical parameters, snapshot identity,
ordering, legal replay, ownership, bounded cursor setup and available demand projections. A projection's contract states
whether it returns exact weighted mappings, exact distinct identities, or an aggregate-specific event stream.
PRESENCE and SUM are independent demands, not interchangeable modes attached globally to an entire query.

Perform backward demand analysis through the actual algebra. Retain group keys, aggregate arguments, expression reads,
join/compatibility keys, MINUS domains, correlation inputs, graph frames and ordering witnesses until their last observer.
Projection can discard an identity only after all these demands have crossed that boundary safely. For example,
`?x :p ?z . ?x :q ?z` cannot become independent counts by `?x`: that loses the required equality of `?z`.
Likewise, an outer expression cannot inspect a subquery's private binding just because storage can still retrieve it.
When a demanded correlation is unavailable, retain a richer relation or expand to semantic rows before discarding it.

Keep a separate group-existence channel. A real group whose aggregate arguments all error or remain unbound still
exists; its COUNT can be zero. An empty grouped relation and a global aggregate's empty-input group are distinct.
Do not infer group existence from a successful value in one selected aggregate channel.

## Operator transfer rules

The following are general operator contracts. They must apply recursively, including inside both arms of another
operator; named benchmark queries are validation workloads, never recognition conditions.

| Operator | Required transfer and fallback |
|---|---|
| Mandatory join | Multiply compatible weights and retain all demanded correlations. Equality of bound shared terms alone is insufficient when slots can be unbound; preserve mapping compatibility. A sparse leaf may contract only dimensions no later observer needs. |
| UNION | Concatenate additive relation partitions and add weights of identical mappings. Overlapping branches contribute twice to bag COUNT; presence/DISTINCT channels union their identities across branches, never add local distinct cardinalities. Preserve branch evaluation order and effects where observable. |
| OPTIONAL | Reuse the existing LeftJoin/Diff semantics, condition evaluation and lexical-frame handling. Successful matches multiply weights; the qualifying unmatched left mapping contributes its original weight once with right-only bindings absent. An empty RHS is not a zero factor. Do not use a global `max(1,rightCount)` shortcut. |
| MINUS | Keep/remove each left mapping using compatible right witnesses and nonempty intersection of actually bound domains; retained weight is unchanged. A disjoint RHS differs from NOT EXISTS. Keep mandatory error-producing evaluations even when a pure relational simplification would discard them. |
| EXISTS / NOT EXISTS | Use a presence witness in the correct correlated frame, active graph and dataset; witness multiplicity never scales the left input. Reuse/memoize only when the complete correlation key and snapshot/effect proof permit it. Expand an omitted binding before testing a witness that reads it. |
| VALUES | Preserve duplicate rows, UNDEF domains, zero rows and the one-empty-mapping identity. UNDEF is absence, not an RDF null or equality key. A bound API input and a locally assigned variable retain their distinct scope contracts. |
| FILTER / BIND | Evaluate with existing short-circuit, EBV, error and assignment rules. Grouped evaluation is legal only on a complete dependency closure with replay proof. Failed BIND leaves its target absent; failed FILTER rejects the mapping. Retain exceptions that are query-fatal. |
| Projection / DISTINCT / REDUCED | Project at the lexical boundary, then apply its declared duplicate operation. Projected bag rows coalesce by summing weights; DISTINCT sets weight to one for each visible mapping. REDUCED may use exact distinct, but cannot silently become a bag/presence assumption downstream. |
| GROUP / HAVING | Plan all aggregate channels together against one input and stable group identity. Hidden HAVING/ORDER aggregates participate too. Materialize authoritative group values before post-group expressions; residual HAVING always remains enforced. |
| Subquery | Use its projected schema and lexical entry frame. Give a completed sparse grouped result a physical result recipe, rather than assuming its original input incidence is the output relation. Group-of-group aggregation observes produced RDF values and errors. |
| GRAPH / dataset | Filter contexts before reduction; retain graph identity whenever projected, shared, grouped or correlated. Keep dataset merge/duplicate behavior and default-graph aliases authoritative. Never merge graph scopes merely because their endpoint terms match. |
| LATERAL | Parameterize by the complete left entry frame and declared inputs. Repeated equal parameter values may share replay only under the existing effect/scope contract. No descriptor resolved against one frame may be reused under another without rebinding. |
| SERVICE / external source | Preserve endpoint, SILENT, error, scope and invocation semantics through its ordinary producer. Local sparse proof does not establish remote snapshot, replay, bounded work or purity; retain the external semantic boundary. |
| Paths | Use the existing path evaluator as a physical leaf with its actual solution semantics. Fixed-length path joins and alternatives can preserve multiplicity, whereas arbitrary-length reachability deduplicates reached nodes. Do not count walks or apply blanket presence to every path form. |
| ORDER / LIMIT / OFFSET | Preserve ordering, stable representatives and evaluation placement. Blocking sparse setup cannot receive an early-LIMIT discount. Ordered concatenation and effectful rows require ordered replay even when the final SELECT has no ORDER BY. |

## DISTINCT and aggregate channels

There are three separate identity domains: a DISTINCT input projection's exported mapping; each aggregate's argument
value or argument tuple; and the final SELECT mapping after projection. COUNT(DISTINCT *) needs names, bound status and
the full visible input mapping. Distinct argument events must survive spill and worker partitions until global
deduplication; summing branch/partition distinct counts is incorrect. Use the same term/group-key authority as the
ordinary evaluator, including canonicalized language tags, literal datatypes, blank nodes and computed values.

Generalize the sparse adapter's demand/output contract before widening its aggregate admission guard. Today's final
endpoint count cannot reconstruct a discarded shared node, value or graph. COUNT(DISTINCT ?shared), for example,
needs exact `(group, sharedIdentity)` membership events; SUM over a payload needs its value and exact outside weight.
Use the common projection readers and reducer state for those demands. If the sparse representation cannot provide
the requested projection without changing semantics, retain that region's ordinary producer.

| Reducer | Feasible compressed state | Conditions that retain semantic/ordered execution |
|---|---|---|
| COUNT(*) / COUNT(expr) | Checked exact row weight, or bound/non-error expression weight. DISTINCT uses globally merged mapping/value identity events. | Evaluate non-reusable expression once per logical mapping; unbound and errors do not contribute to COUNT(expr). Preserve existing overflow/error behavior. |
| SUM | Typed exact numeric state with promotion/error state; repeated integer/decimal values may fold weights when existing reducer laws allow it. | Floating-point multiplication/reassociation changes rounding; NaN, infinities, signed zero, invalid literals and mixed promotion need existing ordered semantics. Do not infer associativity from a numeric datatype label. |
| AVG | The same accepted numeric/error state plus exact denominator, finalized with existing division/promotion rules. DISTINCT first deduplicates arguments. | Never average partial averages; preserve the generic evaluator's treatment of each error/unbound/non-numeric case and empty input. Float/double keep encounter-order safeguards. |
| MIN / MAX | Eligible extremum plus its RDF-term representative; duplicate weights are irrelevant after argument evaluation. | Comparator mode, incomparable terms, equal-valued distinct lexical/datatype representatives and encounter order must follow the existing comparator and tie policy. |
| SAMPLE | One eligible value; duplicate weights do not change membership. | Preserve eligible membership and the actual physical recipe's representative/order guarantees. First-value selection in one state implementation does not impose global encounter order: existing capabilities classify SAMPLE as reorderable. Expression calls still occur when required. |
| GROUP_CONCAT | Ordered value segments and exact copy counts; DISTINCT requires membership before concatenation. A proven lexical-length-only consumer can use the existing specialized summary. | The repository preserves encounter order even though SPARQL leaves string order unspecified. Copy folding requires contiguous equivalent copies; final RDF/string size remains output-sized. Separators, Unicode and empty/error inputs need exact semantics. |
| Custom unary/n-ary aggregate | Existing collector invoked on every logical row/argument tuple; DISTINCT uses the collector's existing membership contract. | The current SPI promises no associative merge, weighted update, serialization, purity or worker safety. Treat it as opaque. Any optional capability SPI must be explicit, implementation-identity-pinned, default-closed and validated against ordinary collectors. |

One difficult reducer must not force unrelated pure COUNT projections to expand unnecessarily. Plan mixed channels
from the same single producer, share safe identity/value materialization, and expand only the demands requiring rows.
However, this is not permission to run input BIND/functions once per aggregate or to reconstruct a different order.
If one opaque observer requires the whole logical stream, retain it and avoid independently replaying its effects.

## Functions and expression scope

The source census used in the feature audit found 195 core scalar SPI registrations, including generic and window
extensions, plus dedicated algebra expressions, tuple functions and optional modules. Maintain that finite inventory
from the [core service manifest](../../queryalgebra/evaluation/src/main/resources/META-INF/services/org.eclipse.rdf4j.query.algebra.evaluation.function.Function)
and [native library](src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFunctionLibrary.java), rather than
hard-coding a second list in the sparse recognizer. Registration, native compilation, sparse admission, automatic
selection and actual execution are separate capabilities; no finite catalogue proves arbitrary user code safe.

All functions can remain usable through the existing semantic evaluator at their original algebra position, including
casts, strings/language/directional literals, RDF triple functions, dates, hashes, optional extension libraries and UDFs.
An unsupported native function must not exclude an otherwise legal query. Compression can preserve surrounding safe
regions but must expand the complete mapping at an opaque callback boundary and invoke it the required number of times.
There is no general way to replace an arbitrary side-effecting callback with one weighted invocation.
Tuple/magic-property functions retain their multi-row schema and multiplicity contract. Window/analytic extensions
retain partition, order and frame semantics; a scalar-service census entry does not make them ordinary pure callbacks.

Native lowering must honor the actually resolved function implementation and preparation snapshot, not just its IRI.
The audit's replacement of a built-in IRI demonstrated why implementation identity is a prerequisite for purity proof.
Keep this property consistent across constant folding, Boolean/value/numeric native compilation and retained plans.
Unknown custom IRIs currently default to semantic execution, but native code generation is not a prerequisite for safe
callback reuse. The public [Function determinism contract](../../queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/Function.java)
already promises equal terms/equivalent errors and no observable invocation effects for DETERMINISTIC implementations,
independent of query, transaction and store state. Future semantic callback replay can rely on that explicit contract,
the pinned implementation and complete argument-expression dependencies; it need not require a URI whitelist or a new
scalar SPI. `mustReturnDifferentResult()` dominates determinism. Worker construction and priced work remain separate
proofs. Arbitrary user code can lie about metadata; no optimizer can prove every implementation obeys its declaration.
Keep sparse reuse conservative for UNSPECIFIED, volatile and context/store-dependent callbacks, preserving per-solution
semantic execution when unproved; this must not silently change the existing generic query-policy defaults.

Classify constant, query-stable, row-stable, volatile and effectful behavior independently. NOW shares one invocation's
query context but is not a retained-plan constant. BNODE(label) has a solution-local contract; fresh BNODE/UUID/RAND
cannot be collapsed across duplicate solutions. Pin callbacks and reconstruct worker-owned contexts explicitly.
Keep short-circuit IF/COALESCE/AND/OR and IN error behavior in the semantic evaluator; eager argument evaluation is not
an interchangeable implementation. Distinguish expression errors from cancellation, storage and query-fatal failures.
After grouping, functions consume the authoritative group BindingSet and can usually wrap sparse results directly.

## Physical execution, memory and selection

First make the existing sparse grouped producer expressible through `NativePhysicalResultPlan` and the common
GROUP_PHYSICAL candidate protocol. That path currently offers ordered/table grouping, while ordinary GROUP separately
offers sparse. An outer physical consumer can therefore lose the sparse choice. Supply output schema/domain/error
evidence and a source-valid executable recipe; do not simply reuse a strategy label or a parent evaluation closure.
Extend `NativeSubqueryPlan.forWorker` incidence rebinding: it currently deliberately drops coordinator-resolved incidence
descriptors. Rebind source/dataset IDs, lexical parameters, scalar evaluators and ownership, or decline that worker route.

Charge resident and transient arrays, relation/dedup state, expression values, replay, cursors, spill buffers and merge
fan-in to the shared query ledger before allocation. Partition/spill membership events globally and ensure progress for
one hot key; no unbounded hash set or per-predicate cursor array may escape accounting. Reserve ordered spill segments
for order-sensitive reducers. Poll cancellation during opening, metadata/catalog preparation, rejected runs, sorting,
long strings, multiplication, merge and replay. Output stays private until any fallback requiring replay is safe.
Never speculatively execute effects and then retry them after a memory or cost refusal.

A physical recipe needs separate proofs of semantic legality, bounded resources and priced work. Quote operator
reopening, graph restrictions, deduplication, expression/callback work, output bytes, spill I/O and worker reconstruction.
Missing overlap or term-width evidence stays unknown/unbounded; totals are not proofs of join overlap. A legal forced
spill recipe does not inherit a dense in-memory price. Unsupported snapshot/overlay capability continues through the
ordinary source; any future overlay sweep must establish bounded, cancellable setup before admission.

## Priorities and independently verifiable milestones

Implementation should use an ExecPlan under `.agent/PLANS.md`, with test-first evidence and one active milestone.
The order below favors shared contracts and measured controls over separate recognizers for individual query strings.

1. **Close semantic and physical boundaries.** Preserve resolved function identity, pin query scope and define relation
   demand/output evidence. Add sparse grouped output to GROUP_PHYSICAL with exact worker/source rebinding and private
   replay. Exit: root and nested consumers produce identical RDF-term bags/errors, and telemetry proves which group
   actually used sparse; unsupported worker/frame cases decline before consuming input.
2. **Expose exact projections and DISTINCT channels.** Adapt sparse leaves to the existing factor/projection protocol,
   including group existence, bound masks, richer retained identities and one-key/global groups. Reuse bounded distinct
   event merging. Exit: arbitrary mixed COUNT/DISTINCT lists, graph duplicates and aliases pass an independent oracle;
   current fixed/predicate co-occurrence controls retain correct routes and memory behavior.
3. **Compose UNION and witness operators.** Add sparse leaves to existing additive partitions; apply global membership
   merging and complete EXISTS/NOT EXISTS/MINUS keys. Exit: overlapping arms, disjoint domains and projected correlations
   pass recursive differential tests; no local distinct-cardinality addition or extra callback evaluation occurs.
4. **Compose OPTIONAL and bound-state transfer.** Reuse existing left-join scopes and unmatched-row production; support
   multi-match, empty/error RHS, condition filters and nested unions without flattening across the boundary. Exit:
   matched and unmatched rows preserve bags, COUNT(*) versus COUNT(?optional), DISTINCT and non-well-designed frames.
5. **Share typed reducer and expression events.** Enable SUM/AVG/MIN/MAX/SAMPLE and argument expressions through existing
   reducer laws; retain ordered replay for floating-point/tie-sensitive states. Exit: arbitrary channel order/repetition,
   anonymous HAVING and nested grouped values match datatype/error/representative behavior, including forced spill.
6. **Retain ordered/opaque execution locally.** Compose GROUP_CONCAT, every supported scalar/tuple UDF and custom aggregate
   through semantic row boundaries; introduce optional weighted/merge SPI only with demonstrated need and full contracts.
   Exit: callback count/order/context, DISTINCT tuples, output-sized concatenation and cancellation are correct without
   claiming that opaque work has become a sparse numeric reduction.
7. **Measure automatic admission and adverse cases.** Price the same immutable recipes actually opened. Compare automatic,
   forced and disabled modes with full-result validation, source identity and executed telemetry. Exit: targeted gains
   are repeatable, adverse/skew/small/no-overlap cases retain sensible alternatives, and incomplete quotes stay incomplete.

Candidate ranking is an architectural-benefit hypothesis based on actual query source, not a new performance result.
Query indices below are zero-based in
[ThemeQueryCatalog](../../../testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java).

| Priority / workload | Why it exercises the general solution | Evidence boundary |
|---|---|---|
| First: HIGHLY_CONNECTED Q9, degree with weighted neighbors | UNION of incoming/outgoing edges, OPTIONAL weight/BIND/filter, DISTINCT neighbor output and non-DISTINCT HAVING COUNT need simultaneous presence and exact bag channels. Reciprocal/self edges overlap arms. | Source at `Connected: degree >1 with weighted neighbors`; no new measured gain. |
| First: EXPLORATION Q6, properties by class with coverage | Mixed COUNT(*), COUNT(DISTINCT subject/object), SUM(IF(kind,...)), named graphs, nested totals and post-group decimal arithmetic test independent demands on one producer. | Multi-channel opportunity; performance unmeasured. |
| Next: EXPLORATION Q8, forward/inverse cardinality | UNION of grouped DISTINCT value counts feeding COUNT/MIN/AVG/MAX/SUM(IF); tests physical grouped-result transfer and exact numeric laws. | Architectural candidate; no sparse-route/speed claim. |
| Next: EXPLORATION Q7 and Q12 | OPTIONAL/COALESCE object-class assignment and nested degree distributions with directional UNION/three-axis DISTINCT stress grouping and graph identities. | Source-derived follow-ons; ordinary optimized routes must be measured first. |
| Later: EXPLORATION Q10, recurring signatures | Ordered nested GROUP_CONCAT, computed feature strings, DISTINCT values and SAMPLE require full expression/order semantics. | High semantic value; output work and existing routes may dominate. |
| Controls: EXPLORATION Q9/Q11; competitors: ANALYTICS Q7/Q8 | Preserve measured predicate/class co-occurrence and compare richer mixed/three-pattern matrix alternatives. | Q9/Q11 have executed sparse evidence; ANALYTICS Q7/Q8 already execute parallel TypeMatrix. Replacing it requires a controlled win. |

## Behavioral verification matrix

Each milestone adds the smallest failing regression before production changes, then focused and relevant module gates.
Generate bounded algebra trees from operator contracts with independent simple scalar/reference evaluation; vary nesting,
operand order and optimizer simplifications. Finite all-pairs coverage complements, rather than replaces, compositional
proofs. Keep the existing 800-pair audit as a baseline and extend heterogeneous lists to every standard reducer and both
DISTINCT states, plus unary/n-ary custom collectors. Avoid test-specific recognition and unconditional random-value equality.

| Axis | Required concrete cases and interactions |
|---|---|
| UNION | Same mapping in both arms, partial/disjoint schemas, zero arm, reversed/nested arms, branch graph duplicates; ordinary and DISTINCT counts together. |
| OPTIONAL | Zero/one/multiple RHS matches; bound/unbound keys; condition true/false/error; FILTER inside/outside; UNION in each arm; OPTIONAL inside MINUS/EXISTS and the converse. |
| Negative/witness operators | Shared/disjoint/UNDEF domains, correlated BIND/filter, empty mapping RHS, nested NOT EXISTS, graph frames and fatal/volatile RHS that cannot be discarded. |
| Projection/correlation | Forget a later join/witness key; same variable names in independent scopes; aliases/rebindings; prebound API inputs; SELECT DISTINCT versus aggregate DISTINCT versus DISTINCT *. |
| GROUP | Zero/one/many keys, computed/error keys, no input, all-error/unbound arguments, hidden HAVING/ORDER aggregates, multiple/repeated/reordered reducers, GROUP over GROUP/UNION/OPTIONAL. |
| RDF values | Equal-valued different lexical/datatype terms, language-tag spellings, blank nodes, IRI/literal mixtures, invalid numerics, integer/decimal/float/double promotion, NaN/infinity/signed zero, extrema ties and Unicode separators. |
| Expressions/SPI | Each finite built-in entry and arity family with dynamic inputs; lazy errors; query-stable/fresh/volatile/custom hidden reads; registry replacement; retained-plan reevaluation; tuple duplicates and custom aggregate DISTINCT tuples. |
| GRAPH/LATERAL/paths | Fixed/variable/hidden/projected/shared graph, FROM/FROM NAMED, default aliases; per-frame LATERAL inputs; cycles/zero length/inverse/alternatives/negated paths with graph restrictions. |
| Physical/resources | Both fixed/predicate families and mixed leaves; direct/decoded/overlay/legacy IDs; serial/workers; partition-boundary duplicate identities; tiny budgets, one hot group, spill/merge, allocation refusal and mid-open/mid-replay cancellation. |

For deterministic outputs compare full RDF-term multisets, binding domains, datatypes, errors and representatives with
native-disabled LMDB and an independent MemoryStore or explicit raw-statement oracle where applicable. For SAMPLE,
unordered SPARQL choices and volatile/fresh values assert permitted membership/invariants and callback observations;
also retain this repository's declared deterministic ordering where it is contractual. Distinguish query errors from
per-expression errors. Record runtime telemetry by group/operator, not just any sparse label elsewhere in the query.

Benchmarks must retain matched datasets/query checksums/JVM/configuration, complete bags, actual strategy and physical
recipe, peak ledger/spill data, and first-use versus warmed measurements. Use the supported benchmark helper; run no
competing builds/benchmarks. Keep 60-second interruptions and wrong-count runs unscored. An intended regression budget
must preserve both absolute and relative thresholds; overlapping uncertainty is not a proven win.

## Investigation boundary

This document was produced by source review, the retained feature audit and benchmark manifests, and the W3C primary
specification. No builds, tests, benchmarks, production changes or Git writes were performed for this roadmap.
The design's main unresolved choices are which sparse projections can retain useful compression, what reusable ordered
event representation is cheapest, and whether any optional custom-aggregate capability is worth its API complexity.
Resolve those with the staged behavioral proofs and measurements before expanding automatic admission.
