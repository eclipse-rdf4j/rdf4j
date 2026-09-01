# Vectorize projection-free wildcard-predicate adjacency

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with
`.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

When the optional node-to-predicate projection is disabled, a wildcard-predicate statement pattern currently sweeps
the predicate domain with scalar, predicate-local point probes. That makes a query such as `SELECT DISTINCT ?p WHERE
{ ?a a ex:SomeClass . ?a ?p ?c }` repeat control flow for every input node and predicate and may decode neighbors that
the consumer never observes.

After this change the same adjacency-backed query consumes sorted node IDs in batches, intersects those IDs with the
sorted root IDs of predicate planes, and does only the work required by its physical demand. Terminal distinct
predicate queries mark and emit each predicate once, then permanently retire it. Node existence queries compact
unresolved node lanes after every predicate. Pair, multiplicity, and payload consumers share the same primitive merge
kernel but request progressively more information. The node-to-predicate projection remains disabled, no public API or
persisted format changes, and Java Vector API work remains deferred.

The result is observable through parity and routing tests with projection explicitly disabled: adjacency must answer
the query, wildcard batching telemetry must increase, distinct-predicate retirement must occur, and payload-decoded
rows must remain zero for a pure `DISTINCT ?p` query.

## Progress

- [x] (2026-08-31 10:02Z) Read repository, performance, test-runner, and ExecPlan instructions.
- [x] (2026-08-31 10:02Z) Capture clean tracked state and successful root quick-install baseline.
- [x] (2026-08-31 10:10Z) Trace fixed adjacency, wildcard fallback, native batching, memory, and parallel contracts.
- [ ] Add failing low-level batch and SPARQL route tests (completed: terminal `DISTINCT ?p` route failure captured;
  remaining: primitive cursor and other demand milestones).
- [x] (2026-08-31) Replace the rejected terminal-shape prototype with a physical-demand contract carrying live slots,
  duplicate policy, scope, and order; route projection, boolean membership, weighted aggregation, and expanded payload
  through that contract.
- [x] (2026-08-31) Implement reusable wildcard predicate-plane cursor and initial serial primitive merge kernels.
- [x] (2026-08-31) Replace byte-per-lane existence marks with dense one-dimensional long bitmaps and add bounded-run
  cancellation checks before any partially processed batch or OPTIONAL null-extension can escape.
- [x] (2026-08-31) Sort `(rootID,inputRow)`, group duplicate root IDs for one plane lookup, and expand run handles back
  to every physical input lane across existence, multiplicity, payload, and correlated stages; preserve an upstream
  factor weight on every expanded PAYLOAD row.
- [x] (2026-08-31) Generalize grouped wildcard execution from downstream observability rather than query text: carry
  only grouping, aggregate, OPTIONAL, and expression inputs; fold dead statement dimensions into checked weights;
  preserve those weights through JOIN/OPTIONAL/LATERAL/FILTER/BIND; and re-factorize before the primitive group table.
- [ ] Lower physical demands and integrate serial wildcard execution (completed: terminal set demands, weighted
  aggregates, all-unbound predicate-major root batches for pair presence and multiplicity, bounded payload copying,
  a memory-accounted batch-to-row bridge beneath FILTER/BIND/UNION/standard OPTIONAL/MINUS/bushy-left wrappers,
  payload admission directly at `MultiJoinPlan` plus set-demand pushdown from interior `RowDistinctPlan`,
  exact weight transfer across UNION, inner/lateral joins, left-side MINUS, and entry-binding boundaries,
  arbitrary-prefix wildcard payload stages for correlated inner-join right arms, single-pattern EXISTS/NOT EXISTS,
  compatible MINUS, and factorized fixed/wildcard single-pattern OPTIONAL right arms with batched null-extension;
  wildcard JOIN, OPTIONAL, and LATERAL right arms now share the same weighted correlated stage, with lexical input and
  preserved-left masks for LATERAL; the cross-operator static audit is complete; remaining: verified route evidence).
- [x] (2026-08-31) Add bounded predicate tiles, node partitions, and shared-budget admission: ledger-sized serial
  tiles; same-snapshot worker groups for `NODE_ANY`, `PREDICATE_ANY`, `PAIR_PRESENCE`, unrestricted and constrained
  `PAIR_MULTIPLICITY`, and `PAYLOAD`; coordinator barriers; worker-first then tile-width memory reduction; and
  worker-owned bounded primitive payload pages that never transfer view-local run handles.
- [ ] Add semantic corpus, telemetry, benchmark shapes, and route assertions.
- [ ] Run focused, module, profile, and paired benchmark verification.
- [ ] Audit final diff and record measured outcomes.

## Surprises & Discoveries

- Observation: `NativeBatch.configuredRows()` already supplies the required 1,024-row default and selection-vector
  representation, so wildcard execution does not need another row-size property.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/NativeBatch.java` owns the frozen row setting.

- Observation: the fixed-predicate adjacency path already has a sorted batch lookup that merges direct immutable roots
  and overlays, including tombstones and historical snapshots. The new wildcard view should select a predicate plane
  and reuse this resolution logic, not implement a second visibility model.
  Evidence: `LmdbDirectNativeAdjacency.findBatch(...)` and
  `ImmutablePagedQuadCsfIndex.PartitionCursor.findBatch(...)`.

- Observation: wildcard lowering currently depends on the node-to-predicate projection when one endpoint is bound;
  the exact-full projection-free candidate therefore falls back to a predicate sweep even though every predicate plane
  has sorted root IDs and batch lookup support.
  Evidence: `LmdbNativeKernelLowering.lowerVariablePredicatePattern(...)` and
  `LmdbDirectAdjacencyPredicateSweepIterator`.

- Observation: the factorized runtime already owns a shared 8 MiB query ledger whose claims are shared by sibling
  sources, and parallel pipelines already provide same-snapshot sources, reservation, cancellation, and worker-failure
  propagation. A second memory or task system would violate existing execution contracts.
  Evidence: `LmdbFusedSipFactorizedRuntime.DEFAULT_QUERY_BUDGET`, its `Session.claim/release`, and
  `LmdbNativeParallelPipelines`.

- Observation: the root quick install refreshed the initially reported tracked modifications; subsequent porcelain-v2
  and file diffs were empty. Existing untracked `.agent/evidence` and final-evidence artifacts remain user-owned and
  must be preserved.
  Evidence: root `maven-build.log`, `git status --porcelain=v2 --untracked-files=no`, and `git diff-files --raw`.

- Observation: the motivating projection-free query does not reach variable-predicate lowering at all; generic/native
  result parity holds, then the route assertion fails with the lowering counter unchanged at zero.
  Evidence: `initial-evidence.txt`, retained log `logs/mvnf/20260831-101541-verify.log`, and Surefire summary
  1 test / 1 failure / 0 errors / 0 skipped.

- Observation: the compiled plan is already a two-pattern `MultiJoinPlan`, but routing a terminal-only recognizer from
  `NativeRowsStep` still makes the optimization depend on one consumer/query shape and leaves wildcard access inside
  filters, OPTIONAL, MINUS, EXISTS, subqueries, aggregates, and payload plans untouched.
  Evidence: the focused failure renders
  `NativeRows(arg=MultiJoin(...), sourceSlots=[?p], distinct=true)`; implementation review showed that the recognizer
  received projection/distinct state directly rather than a general physical-demand contract.

- Observation: the existing aggregate path can already consume a `FactorizedRowCursor` weight, but weights were lost
  at FILTER, BIND, and OPTIONAL boundaries and dead dimensions were retained until grouping. Exact multiplicity
  transfer plus boundary re-factorization lets an object remain live only through its OPTIONAL type lookup, then folds
  rows by `(predicate,type_fixed)` before the group table.
  Evidence: `LmdbNativeGroupStep.NativeGroupTable.add(row, weight)`, `FactorizedRowCursor`, and the weighted wildcard
  cursors in `LmdbWildcardPredicateBatch`.

- Observation: predicate boundness is a per-lane runtime property, not a query-shape property. A batch may contain only
  bound predicate ids, only unbound lanes, or both. Sorting and deduplicating visible predicate ordinals avoids a full
  sweep for the first case; the presence of any unbound lane makes the same schedule enumerate the exact full domain.
  Evidence: `preparePredicateSchedule(...)` is shared by boolean, multiplicity, and payload kernels.

- Observation: all-unbound access does not require manufacturing a singleton node batch or decoding statement rows.
  The existing `KeyRunCursor.fillRoots(...)` contract already supplies bounded root ids, predicate ordinals, run
  handles, and exact multiplicities. Pair presence consumes only root coordinates; weighted consumers use the run
  multiplicity; payload consumers bulk-copy bounded neighbor/context columns from the retained cursor.
  Evidence: `PredicateMajorPresenceCursor`, `PredicateMajorMultiplicityCursor`, and
  `PredicateMajorPayloadCursor` in `LmdbWildcardPredicateBatch`.

- Observation: a row-to-batch adapter must restore its last active row after every non-empty fill, including a short
  final batch. Restoring only full batches leaks the final left-arm bindings into a following UNION arm or wrapper.
  Evidence: the composable batch bridge made the partial-batch boundary reachable in `RowBatchCursor`; its active-row
  marker now covers every non-empty fill.

- Observation: aggregate liveness alone does not describe duplicate sensitivity. An aggregate family whose members
  are all DISTINCT over the same value slot is a set consumer over `(group keys,value)`: global
  `COUNT(DISTINCT ?p)` requests `PREDICATE_ANY`, while `GROUP BY ?s` requests `PAIR_PRESENCE`. Mixed distinct and
  non-distinct aggregates retain the weighted path because bag semantics remain observable.
  Evidence: `distinctAggregateValueSlot(...)`, `distinctProjection(...)`, and `openDistinctSet(...)` derive this from
  `AggregateSpec`, not from query text.

- Observation: a statically produced endpoint is not necessarily bound in every lane. A UNION or OPTIONAL prefix can
  advertise a root slot in `producedMask()` while emitting rows where it is absent; treating that as a sorted root
  silently drops the all-unbound lanes. Wildcard merge admission must therefore use `SlotPlan.assuredMask(...)` and
  decline to the exact path when neither endpoint is assured.
  Evidence: `tryOpenPayload(...)`, `Shape.match(...)`, and the existing `SlotPlan.assuredMask(...)` algebra rules.

- Observation: OPTIONAL requires a verdict per left factor only after the complete predicate domain has been checked.
  A one-dimensional dense match bitmap supplies exact null-extension. When the opposite endpoint and context are dead
  and unconstrained, the same cursor emits exact run-size weights per left-factor/predicate pair without decoding the
  fiber; otherwise it uses bounded neighbor/context copies before re-factorizing the surviving live dimensions.
  Evidence: `WeightedWildcardOptionalPatternCursor` and `WeightedOptionalPatternCursor` in
  `LmdbWildcardPredicateBatch`.

- Observation: wrapper-level `openBatch()` delegation is only useful when its multi-join child exposes wildcard
  batching as a normal physical candidate. Restricting demand selection to terminal native rows and aggregate entry
  points causes an interior DISTINCT, FILTER, UNION arm, or subquery to fall back to unrelated merge/hash candidates.
  Evidence: `MultiJoinPlan.openBatch()`, `RowDistinctPlan.openBatch()`, `tryOpenPayload(MultiJoinPlan,...)`, and
  `tryOpenDistinct(...)`.

- Observation: cancellation must be polled inside bounded run copies, not only between predicates. A restrictive
  context/opposite filter can otherwise consume a supernode run without emitting, starving the outer row-loop poll;
  OPTIONAL could then mistake an interrupted sweep for an empty right side and null-extend it.
  Evidence: the payload, presence, multiplicity, and fixed/wildcard OPTIONAL run loops now stop and close before
  exposing a partial batch or fallback row.

- Observation: deduplicating a sorted root vector is only a storage optimization; duplicate upstream rows still carry
  distinct bindings and/or factor weights. The batch kernel therefore needs group offsets that map each unique-root
  lookup back to all original physical rows. PAYLOAD is itself a weighted cursor because a later expansion cannot
  reset a factor inherited from an already-reduced prefix.
  Evidence: `groupSortedRoots(...)`, `expandRunHandles(...)`, `PayloadCursor`, `PairMultiplicityCursor`, and the
  fixed/wildcard OPTIONAL cursors in `LmdbWildcardPredicateBatch`.

- Observation: correlated wildcard JOIN, OPTIONAL, and LATERAL differ in output policy, not in their predicate-plane
  merge. Inner joins discard unmatched factors, OPTIONAL null-extends them after the full predicate sweep, and LATERAL
  additionally masks hidden left bindings while probing and preserves those bindings while merging the right row.
  Evidence: `WeightedWildcardPatternCursor`, `tryOpenOptional(...)`, `tryOpenLateral(...)`, and the weighted algebra
  routing in `LmdbWildcardPredicateBatch`.

- Observation: an all-unbound distinct-node consumer has no input node vector to prune, but it still does not need
  fibers. Predicate-major `fillRoots` can emit root coordinates and let the ordinary DISTINCT layer union the same node
  across predicate planes. Pair DISTINCT remains unique within each plane and may keep the same conservative wrapper.
  Evidence: all-unbound node-only set demand selects `PredicateMajorPresenceCursor`; only predicate-domain cursors claim
  that they have fully discharged a global DISTINCT.

- Observation: an uncorrelated wildcard EXISTS/NOT EXISTS has no node axis at all. With unrestricted contexts and no
  repeated variables, one non-empty predicate root domain is a complete global witness; the verdict is snapshot-local
  and can be applied to every current and later input batch without reading a run.
  Evidence: `ExistenceBatch.selectGlobalIfUncorrelated(...)` caches the first-witness verdict and the route test covers
  both positive and complemented consumers.

- Observation: a run handle is local to the mutable adjacency view that resolved it and therefore cannot cross from a
  same-snapshot worker to the coordinator. Presence may transfer bits and unrestricted multiplicity may transfer exact
  run sizes, but PAYLOAD workers must publish bounded primitive neighbor/context pages and retain their own handles
  until those pages have been consumed.
  Evidence: `ParallelPredicateTiles` merges worker-local presence bitmaps and multiplicity matrices, while payload
  rounds expose only bounded `(inputRow,predicate,neighbor,context)` primitive pages after the coordinator barrier.

- Observation: when both endpoints are available, choosing outgoing unconditionally can select the denser plane. A
  snapshot-local prepass can compare exact run sizes for entry-bound roots and exact visible root counts otherwise,
  while preserving outgoing as the stable tie-break and declining to whichever direction is actually available.
  Evidence: `Shape.directionWithLowerRootCount(...)` uses both wildcard plane views before opening the chosen cursor.

- Observation: the both-endpoint prepass needs outgoing and incoming wildcard views alive concurrently. Probe
  ownership therefore has to retain an intrusive list, just like fixed-predicate native adjacency, rather than closing
  the previously returned wildcard view when another direction is requested.
  Evidence: `LmdbDirectWildcardAdjacency.attachToProbe(...)` and both native-probe implementations close every owned
  wildcard view at probe shutdown.

## Decision Log

- Decision: implement one scalar batched primitive merge kernel first; do not use `jdk.incubator.vector` or introduce
  Vector API feature selection in this plan.
  Rationale: ordering, deduplication, visibility, demand, and bounded-memory behavior must be correct and measured
  before SIMD code shape has value.
  Date/Author: 2026-08-31 / Codex.

- Decision: treat wildcard evaluation as five explicit internal demands: `NODE_ANY`, `PREDICATE_ANY`,
  `PAIR_PRESENCE`, `PAIR_MULTIPLICITY`, and `PAYLOAD`.
  Rationale: existence, distinctness, counting, and row materialization have different stopping rules. An explicit
  demand prevents accidental object/context decoding and makes optimizer pushdown auditable.
  Date/Author: 2026-08-31 / Codex.

- Decision: make the predicate plane the outer storage unit and use predicate-major bitmap tiles for pair-producing
  demands; only `NODE_ANY` partitions nodes.
  Rationale: one worker then owns every write for a predicate in a batch round, while node existence workers own
  disjoint lane ranges. Both avoid per-match atomics and permit coordinator merges between bounded rounds.
  Date/Author: 2026-08-31 / Codex.

- Decision: persist maximum-root exhaustion only when the input stream proves global unsigned ascending order.
  Rationale: sorting an individual unordered batch makes merge probing safe but does not prove that a later batch cannot
  contain smaller IDs. A matched `PREDICATE_ANY` predicate is independently permanent because its global result is
  already known.
  Date/Author: 2026-08-31 / Codex.

- Decision: claim all new arrays and pages from the existing shared query ledger, decreasing workers before tile width
  and declining before emitting if one serial predicate cannot fit.
  Rationale: this preserves exact fallback and makes memory refusal atomic; partial optimized output can never be
  followed by fallback output.
  Date/Author: 2026-08-31 / Codex.

- Decision: retain the old exact wildcard evaluator behind a default-on typed batch-candidate switch.
  Rationale: the new path remains a costed physical candidate, has an exact rollback path for unsupported demand/order
  combinations or memory refusal, and can be disabled without changing results or persisted state.
  Date/Author: 2026-08-31 / Codex.

- Decision: reject the terminal `DISTINCT ?p` query-shape recognizer. Introduce a consumer-derived physical demand
  carrying live slots, cardinality sensitivity (boolean, set, bag), required ordering, and global/local scope. Walk the
  physical algebra to refine that demand at filters, joins, OPTIONAL, MINUS, EXISTS, subqueries, and aggregates; select
  a wildcard mode from properties, never from SPARQL text or a fixed number of sibling patterns.
  Rationale: every projection-free wildcard access must enter the same batched stage. `DISTINCT ?p` is one legal
  `PREDICATE_ANY` consequence, not the recognition rule.
  Date/Author: 2026-08-31 / Codex.

- Decision: make wildcard evaluation a composable `BatchCursor` stage over an arbitrary upstream batch. The stage
  accepts mixed boundness, groups already-bound runtime predicates, enumerates unbound predicates by tiles, and uses
  predicate-major root cursors when neither endpoint is bound. Multi-join planning may reorder inner-join children but
  must preserve residual filter masks; nested operators request the same stage through their own boolean/set/bag
  demand rather than installing special-case evaluators.
  Rationale: this makes PAYLOAD the exact general fallback while allowing NODE_ANY, PREDICATE_ANY, PAIR_PRESENCE, and
  PAIR_MULTIPLICITY to remove work wherever downstream observability proves that legal.
  Date/Author: 2026-08-31 / Codex.

- Decision: represent downstream observability as required slot values plus duplicate policy (`ANY`, `DISTINCT`,
  `WEIGHTED`, or `EXPANDED`), rather than treating `PAYLOAD` as the only choice whenever an opposite endpoint is live.
  A weighted stage emits one primitive key tuple and an exact checked multiplicity, and later joins, OPTIONAL,
  extensions, and aggregates preserve or multiply that weight. The original five wildcard modes remain useful storage
  kernels, but are selected underneath this algebraic contract rather than exposed as a closed set of query shapes.
  Rationale: in `GROUP BY ?p ?type_fixed` with `COUNT(*)`, the wildcard stage needs object IDs but not one physical row
  per `(?s,?p,?o)` statement. It can factorize dead subjects into weighted `(?p,?o,count)` tuples, batch distinct object
  IDs through the OPTIONAL type lookup, propagate the weight to every matching type (or the null-extended default row),
  evaluate the BIND, and call the existing weighted primitive group table once per factor.
  Date/Author: 2026-08-31 / Codex.

- Decision: every optimized boolean/set stage must retain an exact pre-emission decline boundary. A wildcard batch may
  fail admission or discover mixed boundness before consuming input; once a prepared stage has buffered rows it either
  finishes those rows exactly or uses the ordinary scalar evaluator against that same buffer. Query-local state and
  memoization are reused by both paths.
  Rationale: adjacency is an optional physical source. Memory pressure, unsupported direction, or a non-direct source
  must change only the selected implementation, never the result or SPARQL bag semantics.
  Date/Author: 2026-08-31 / Codex.

- Decision: keep the wildcard kill switch internal and separate from the Workbench live-control allowlist. The
  execution code samples the typed, default-on `rdf4j.lmdb.wildcardAdjacencyBatch.enabled` system property at
  candidate boundaries, while the public runtime-properties API, persisted configuration, and adjacency projection
  defaults remain unchanged.
  Rationale: a query-engine rollback switch does not imply that remote live mutation should be exposed as an
  administrative control.
  Date/Author: 2026-08-31 / Codex.

- Decision: choose the predicate-major scan direction from the live endpoint for all-unbound patterns. Subject is the
  stable tie-break; an object-only or object-predicate demand uses the incoming planes so a live object remains a root
  coordinate rather than forcing subject payload expansion.
  Rationale: direction is a physical property derived from downstream liveness, not a syntactic query shape. This also
  lets weighted `(predicate,object)` consumers use exact run multiplicity without decoding dead subjects.
  Date/Author: 2026-08-31 / Codex.

- Decision: admit a correlated wildcard merge only when its chosen root is constant, bound on operator entry, or
  assured by every row of the prefix. Do not infer root availability from `producedMask()` alone.
  Rationale: per-lane missing roots require a different all-unbound scan and cannot be discarded by a root-vector
  kernel. Conservative admission preserves exact fallback while still batching the common assured class-domain and
  join-key cases.
  Date/Author: 2026-08-31 / Codex.

- Decision: sort root IDs together with their physical input-row ordinals, probe each unique root once per predicate
  plane, then expand the returned handle over the duplicate group before applying lane-local constraints. Keep factor
  weight as a separate primitive column through PAYLOAD expansion.
  Rationale: storage lookups can be deduplicated without changing SPARQL bags. Collapsing the physical rows or losing
  a prefix weight would undercount grouped consumers and could merge rows whose other bindings differ.
  Date/Author: 2026-08-31 / Codex.

- Decision: use one correlated wildcard cursor for inner, outer, and lexical joins. Carry an explicit outer flag plus
  lexical constraint and preserved-left masks instead of creating query-shape-specific evaluators.
  Rationale: predicate scheduling, root grouping, bounded payload copying, run-size weighting, and cancellation are
  identical. Only unmatched-row emission and which left bindings may constrain or be replaced by the right arm differ.
  Date/Author: 2026-08-31 / Codex.

## Outcomes & Retrospective

The first SPARQL route test and its pre-change failure are complete. A first terminal-shape prototype was compiled but
rejected during review before acceptance because it would not generalize to other wildcard consumers; its reusable
predicate-plane cursor work remains applicable, while its recognizer will be replaced. The
baseline root `Pquick` clean install passed in 36.360 seconds, including the LmdbStore module in 10.713 seconds. No
performance or throughput claim has been made. Update this section at every
milestone with the failing-before/passing-after evidence, supported demands, fallback boundaries, and measured serial
and parallel results.

## Context and Orientation

The LMDB Sail module is under `core/sail/lmdb`. LMDB remains the authoritative persisted statement store. Direct
adjacency is a derived in-memory index split into fixed-predicate planes. An outgoing plane is ordered by unsigned
subject ID, then object and context IDs; an incoming plane is ordered by unsigned object ID, then subject and context
IDs. A root means the subject of an outgoing plane or the object of an incoming plane. A run is every neighbor/context
entry for one root in one predicate plane. A run handle is an internal stable reference that lets later code obtain the
run size or copy bounded neighbors without first allocating RDF values.

`NativeLmdbQuerySource.NativeProbe` is the internal query-facing source of adjacency views.
`NativeLmdbQuerySource.NativeAdjacency` is the fixed-predicate interface. Its direct implementation in
`LmdbDirectAdjacencyStore.java` resolves the immutable base, retained historical generations, replacements, overlays,
and tombstones. `findBatch` accepts sorted primitive root IDs and writes run handles. The immutable base implementation
in `ImmutablePagedQuadCsfIndex.java` already chooses a merge intersection for sufficiently dense batches.

`LmdbNativeKernelLowering.java` turns supported statement patterns into native intermediate representation. A physical
demand describes what a downstream operator can observe. `NODE_ANY` needs one yes/no bit per node. `PREDICATE_ANY`
needs one persistent yes/no bit per predicate. `PAIR_PRESENCE` needs every distinct node-predicate pair but not the
number of statements in a run. `PAIR_MULTIPLICITY` needs the exact qualifying statement count for each pair. `PAYLOAD`
needs neighbor and context IDs. Demand may be pushed below a statement pattern only when every intervening operator is
insensitive to omitted node, object, context, and bag multiplicity.

`NativeBatch` supplies the standard batch size and selection vector. `LmdbFusedSipFactorizedRuntime` owns the shared
8 MiB per-query scratch ledger. `LmdbNativeParallelPipelines` reserves tasks and creates sibling sources pinned to the
same LMDB/adjacency snapshot. `LmdbDirectAdjacencyPredicateSweepIterator` is the scalar projection-free fallback and
must remain exact while the new candidate is staged and whenever admission declines.

Unsigned ordering means comparing encoded long IDs with `Long.compareUnsigned`, not Java signed order. A predicate
domain is the exact visible list of predicate IDs for the pinned query snapshot. Historical reads and uncommitted
overlays must see exactly the same domain and runs as fixed-predicate adjacency. Duplicate upstream rows carry SPARQL
bag multiplicity; deduplicating root IDs for lookup is legal only when group offsets preserve how many and which input
rows produced each root.

## Plan of Work

Milestone one establishes tests and the low-level interface. Add a package-private wildcard adjacency view to
`NativeLmdbQuerySource.NativeProbe`, with a no-support default so non-direct sources compile and decline. The direct
view owns a reusable mutable plane cursor per worker. It exposes the visible predicate count, predicate ordinal and ID,
plane binding, exact root count, maximum root, `findBatch`, short-circuiting `anyBatch`, run size, exact run filtering,
and bounded neighbor/context copying. Refactor fixed and wildcard views onto one internal plane resolver so base,
overlay, tombstone, retained-generation, and handle-resolution rules have one implementation. Tests must first fail on
unsigned sorted input, unsorted duplicate input, empty planes, historical views, and replacement/tombstone visibility,
then pass without RDF value allocation.

Milestone two first adds the demand contract and a composable serial stage in new package-private production classes
near the native runtime. A demand contains the downstream live-slot mask, duplicate policy (`ANY`, `DISTINCT`,
`WEIGHTED`, or `EXPANDED`), global versus per-input scope, and requested physical order. `WEIGHTED` is legal only when
every intervening operator has an exact transfer: inner joins multiply compatible weights, OPTIONAL multiplies a left
weight into every matching right factor and retains it once for null extension, extension/BIND and filters preserve or
discard it, and supported aggregates consume it with checked arithmetic. Demand analysis walks every `SlotPlan` kind:
joins
add shared and residual-filter slots; OPTIONAL retains right multiplicity unless an enclosing set/boolean consumer
proves it dead; MINUS and EXISTS request per-left boolean membership while retaining their compatibility domains;
subqueries establish a new scope; group and distinct operators expose their grouping/aggregate slots. The wildcard
mode is derived from those properties, not from child count, source query text, or one projection list.

The stage is a `BatchCursor` transform over arbitrary upstream rows. It handles a batch containing unbound predicates,
already-bound runtime predicates, or both: unbound lanes traverse predicate tiles, while bound lanes are grouped by
predicate ID and merged against that one plane. If a root endpoint is not yet bound, predicate-major
`KeyRunCursor.fillRoots/fillFibers` generates roots directly; if both endpoints are bound, plane statistics select the
cheaper direction. Multiple wildcard patterns compose as multiple stages, so no wildcard access falls back merely
because the BGP contains another wildcard. Filters are placed at the first stage whose bound mask covers their read
mask, using the existing `FilterBatchCursor`.

Normalize
input through a scratch structure containing unsigned-sorted unique node IDs, original row ordinals, and group offsets;
borrow the input array only when the physical order property proves global ascending uniqueness. Implement
`NODE_ANY` with an unresolved-lane vector compacted after each predicate. Implement `PREDICATE_ANY` with a persistent
predicate bitmap, `anyBatch`, first-match emission, and permanent retirement. Implement pair presence with a flattened
predicate-major bitmap tile whose index is `predicateWithinTile * wordsPerNodeBatch + word`. Implement multiplicity by
reading run sizes when context is unrestricted and invoking an exact run-level filter for named/default/fixed context,
bound opposite endpoint, or repeated-variable constraints. Generalize multiplicity into a weighted projection: retain
the downstream-live primitive fields as its factor key, fold dead fields into an exact long weight, and coalesce equal
keys before leaving the bounded tile. Implement expanded payload by retaining primitive
`(nodeOrdinal,predicateOrdinal,runHandle)` triples only until that predicate has expanded them into bounded output
pages. Every mode checks cancellation at batch/predicate boundaries.

Milestone three integrates lowering and execution. Consumer properties derive safe terminal `DISTINCT ?p` and
`COUNT(DISTINCT ?p)` as `PREDICATE_ANY`, `EXISTS`, `NOT EXISTS`, and compatible `MINUS` as `NODE_ANY`, and
`DISTINCT ?node ?p` as `PAIR_PRESENCE`. Keep `PAIR_MULTIPLICITY` or `PAYLOAD` whenever a filter,
projection, grouping expression, context, alias/repeated variable, or downstream operator can observe counts or row
contents. Where values are observed but individual duplicate copies are not, choose weighted projection instead of
expanded payload. Propagate grouping-key, aggregate-input, OPTIONAL-compatibility, and expression-input liveness
backward through arbitrary nesting; the concrete `?p`/`?type_fixed` query is a corpus member, not a recognizer. Make
`FactorizedRowCursor` multiplicity composable through filters, extensions, inner joins, LATERAL, and OPTIONAL cursors;
permit bounded re-factorization after an operator drops a formerly live dimension; and make every group-table entry
point consume surfaced weights. Both-bound endpoints choose the
direction with the smaller exact visible root/run estimate, with outgoing as
tie-break. All-unbound patterns scan predicate-major roots and fibers directly rather than constructing a synthetic
node batch. Requested result order remains a declared physical property; use existing sorting, merging, and residual
filters when the demand kernel cannot prove it.

Milestone four adds monotonic exhaustion and parallel rounds. Empty planes retire immediately. For a globally monotonic
input stream, compare each plane maximum root with batch minimum/maximum to skip or retire it; keep the cursor when its
next root exceeds the current maximum. Never retain this exhaustion across independently sorted unordered batches.
Partition node ranges for `NODE_ANY`; partition predicate tiles for all other demands. Reserve existing sibling tasks,
give each worker private bitmaps/handles/output pages, and merge only at the coordinator between rounds. Claim the full
minimum scratch before any result emission. On refusal reduce workers, then tile width toward one; if serial minimum
fails, close all temporary views and take the old exact path. Admit parallelism only for two or more independent ranges
and enough estimated root/payload work to exceed the existing measured scheduling intercept.

Milestone five completes observability and acceptance. Add a startup-frozen, typed, default-true internal property for
the wildcard batch candidate alongside existing `LmdbRuntimeProperties` consumers; it must not become a Workbench live
control unless explicitly allowlisted. Add counters for batches, predicates tested/matched/exhausted, lanes pruned,
bitmap tiles, payload rows decoded, workers, memory refusals, and fallback reason. Extend native routing diagnostics so
tests can prove adjacency and the demand selected. Add low-level, SPARQL, cross-tier, worker-count, and benchmark cases.
Keep scalar, serial batch, and parallel batch selectable in benchmarks, but publish no speedup claim until paired JMH
and profiles show less scalar probing and payload decoding with no credible regression in affected shapes.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j`. Use `apply_patch` for edits and do not clean,
stash, restore, or delete untracked artifacts.

The required root baseline command has completed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

Before the first production edit, add the smallest focused route/parity test and run it through the repository runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py \
      'LmdbNativeWildcardPredicateBatchTest#terminalDistinctPredicateUsesBatchAdjacencyWithoutPayload' \
      --retain-logs

Expect the new assertion to fail because the existing projection-free route is a scalar predicate sweep. Immediately
preserve compact Surefire evidence in `initial-evidence.txt` with `scripts/agent-evidence.py`, leaving the retained log
and reports in place. After the minimal production milestone, rerun the identical selector and expect it to pass.

Repeat failing-before/passing-after at each behavior milestone with focused methods for node existence, pair presence,
multiplicity/context, payload, historical/overlay visibility, memory refusal, cancellation, and one/many-worker parity.
Then run whole relevant classes and the module without `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeWildcardPredicateBatchTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeVariablePredicateKernelTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbUniversalAdjacencyCandidateTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencySemijoinTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run the strict adjacency/native profiles through runner passthrough where available. Before final tests, run the
copyright checker, add `// Some portions generated by Codex` beneath the existing license header in every new Java
file, and format with the repository command:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is behavior-neutral and is the only documented `-q` exception; no test command may contain
`-q`. Benchmark one method at a time through the supported wrapper, first scalar, then serial batch, then parallel
batch, with JFR enabled for representative dense and sparse shapes:

    ./scripts/run-single-benchmark.sh --enable-jfr <fully-qualified-benchmark-method>

Record exact revisions, JDK, worker setting, data seed, JMH score/error, and JFR destination in this plan. Do not infer
performance from correctness tests or telemetry alone.

## Validation and Acceptance

Low-level acceptance covers unsigned root IDs around the signed boundary, already sorted and borrowed input, unsorted
input, duplicate roots with recovered input-row multiplicity, empty predicate domains, predicate and node counts that
cross tile/batch boundaries, supernode runs, bound neighbors, default/named/fixed context filters, explicit/inferred
planes, historical snapshots, replacements/tombstones, memory refusal before emission, and prompt cancellation. Each
case compares the wildcard cursor/kernel with the fixed-predicate exact view over the same pinned snapshot.

SPARQL acceptance runs outgoing and incoming wildcard patterns with projection disabled through generic evaluation,
native rows, interpreted IR, generated kernels, factorized execution, and serial/parallel native execution. It covers
aliases and repeated variables, both-bound and all-unbound endpoints, OPTIONAL, MINUS, EXISTS, NOT EXISTS, subqueries,
DISTINCT, ungrouped and grouped distinct count, full bag consumers, and context datasets. Every result multiset must
match generic evaluation exactly.

The grouped weighted corpus includes `?s a ex:CommonClass; ?s ?p ?o`, followed by
`OPTIONAL { ?o a ?type }`, `BIND(COALESCE(STR(?type), "DefaultClass") AS ?type_fixed)`, and
`GROUP BY ?p ?type_fixed` with `COUNT(*)` and matching order keys. Its first wildcard stage is a weighted projection:
the OPTIONAL observes `?o`, while individual subject rows are dead and can be folded into exact `(?p,?o)` weights.
The OPTIONAL batches distinct object IDs, multiplies that weight into each matching type, or preserves it once on the
null-extended path; BIND preserves the weight and the primitive group table consumes it directly. Grouping on `?p`
must neither misclassify the access as predicate existence nor force expanded payload.

The corpus covers repeated `(?p,?o)` factors across subjects, objects with zero, one, and multiple types, wildcard
edges whose objects also participate in the type relation, duplicate upstream rows, contexts, and a real type whose
string equals the default label. Route assertions require wildcard weighted projection, batched OPTIONAL probing, and
weighted group accumulation; expanded wildcard payload rows must remain below the logical statement multiplicity.

For the motivating terminal `DISTINCT ?p` query, routing diagnostics must report adjacency, `PREDICATE_ANY`, at least
one wildcard batch, exactly one emission per matching predicate, permanent matched-predicate retirement, and zero
payload rows decoded. A late/no-match case must test every non-exhausted predicate; an early-match case must show fewer
predicate-batch probes than the scalar node-by-predicate product. For `NODE_ANY`, lane-pruning telemetry must increase
and NOT EXISTS must equal the complement over the original selected lanes.

The full LMDB module gate must have no new Surefire or Failsafe failures. Paired benchmarks must compare identical
revision, JVM, data, forks, warmup, and measurement settings. Retain the parallel route only for workload regions where
matched results beat the identical serial kernel after scheduling startup. A performance claim requires profile
evidence of reduced fixed-predicate point-probe control flow and zero/less payload decoding where demand permits, plus
no statistically credible regression for affected wildcard shapes.

## Idempotence and Recovery

All source changes are ordinary Git edits. Test and benchmark products stay in `.m2_repo`, `target`, `logs`, JFR, and
top-level evidence files; always retain them. Focused tests and installs are safe to repeat. Never use Git cleanup,
reset, broad restore, or stash because existing untracked evidence belongs to the user.

Every optimized operator claims its complete minimum scratch and establishes its route before emitting. Therefore a
memory, unsupported-demand, visibility, order, or task-reservation refusal can close its private views and invoke the
existing exact scalar evaluator without duplicate rows. Worker failure cancels siblings and surfaces the original
failure; it does not fall back after partial output. The default-on kill switch returns planning to the prior exact
candidate without changing store contents. Historical or overlay disagreement is a correctness failure: stop and fix
shared plane resolution rather than bypassing the case.

## Artifacts and Notes

Initial baseline:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install
    Result: BUILD SUCCESS; total wall-clock 36.360 s; LmdbStore SUCCESS in 10.713 s.

The original tracked status entries had no content diff after the mandatory install. User-owned untracked evidence
under `.agent/evidence` and top-level `final-evidence*.txt` remains untouched.

## Interfaces and Dependencies

No new external dependency, public RDF4J API, persisted adjacency format, or node-projection default is permitted.
Use primitive `long[]`, `int[]`, and `long[]` bitmap words in hot paths. Do not use boxed maps, per-match records,
streams, lambdas, or Java Vector API code in the merge kernel.

In `NativeLmdbQuerySource.NativeProbe`, define a package-private/internal view equivalent to:

    WildcardAdjacency wildcardAdjacency(boolean bySubject);

The returned closeable view exposes one exact snapshot predicate domain and creates one reusable worker cursor. The
cursor contract must include predicate count, predicate ID by ordinal, binding an ordinal, exact visible root count,
maximum visible root, `findBatch`, `anyBatch`, run size, exact qualifying count/any with neighbor/context constraints,
and bounded neighbor/context copy. Unsupported sources return null or an explicit unsupported result according to the
existing native probe convention.

Define storage-kernel modes `NODE_ANY`, `PREDICATE_ANY`, `PAIR_PRESENCE`, `PAIR_MULTIPLICITY`, and `PAYLOAD`, selected
from an algebraic demand containing required slots and duplicate policy (`ANY`, `DISTINCT`, `WEIGHTED`, or `EXPANDED`).
Define one batch-kernel entry point that receives the pinned wildcard view, direction, normalized primitive input,
demand, context/opposite-endpoint constraints, order proof, shared query session, cancellation signal, and a bounded
primitive result sink. Serial and parallel code must call the same kernel logic; parallelism changes ownership and
coordination, not semantics.

Use `NativeBatch.configuredRows()` for row capacity, `Long.compareUnsigned` for ID order,
`LmdbFusedSipFactorizedRuntime.Session.claim/release` for all scratch, and `LmdbNativeParallelPipelines` for task
reservation, same-snapshot sibling sources, cancellation, and failure propagation. Use the existing runtime-properties
typed source for the default-on switch and existing native route/metrics conventions for telemetry.

Revision note (2026-08-31 / Codex): created the initial self-contained execution specification after source-path and
baseline inspection. It resolves demand semantics, merge layout, exhaustion, memory/fallback atomicity, parallel
ownership, test-first milestones, and the explicit exclusion of Java Vector API work.

Revision note (2026-08-31 / Codex): recorded the first failing SPARQL route test. Its answers match generic evaluation,
but the projection-free shape never reaches native variable-predicate lowering, which establishes the pre-change route
defect without attributing a correctness failure to the current fallback.

Revision note (2026-08-31 / Codex): generalized downstream demand after the grouped OPTIONAL/COUNT example. Live
payload no longer automatically means expanded rows: exact multiplicity may flow as a factor weight through compatible
operators and directly into aggregation.
