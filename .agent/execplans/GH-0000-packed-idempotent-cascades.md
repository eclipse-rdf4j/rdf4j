# Replace Cascades internals with a packed idempotent planner

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept current while implementation proceeds. Maintain this document in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J's Cascades optimizer currently spends much of its planning time allocating and traversing Java maps, sets, lists, streams, record keys, and complete `TupleExpr` alternatives. The replacement described here keeps `TupleExpr` at RDF4J's public planning and execution boundary but uses dense integer identifiers and parallel primitive arrays throughout Cascades. A query is encoded once, logical and physical alternatives are interned idempotently, the winner is selected from integer links, and only that winner is materialized as a `TupleExpr`.

The observable result is unchanged query behavior with substantially lower planning latency and allocation. On the versioned planning corpus, every supported search mode participates equally in the acceptance calculation: at least 90 percent of query/mode cells must have p95 planning latency below 5 ms and at least 50 percent below 0.5 ms. The existing four-factor benchmark must fall below 0.5 ms and 512 KiB per plan, and the existing eight-factor benchmark below 5 ms and 2 MiB per plan on the recorded JDK 25 reference machine.

LMDB's stored predicate-object range guarantees must remain active after the packed cutover. When every stored object for a constant predicate is known to be an IRI, literal, blank node, particular datatype family, canonical integer range, or member of a finite value set, packed exploration must use that semantic fact to prove impossible filters empty, remove filters that are always true, generate duplicate-safe finite `VALUES` anchors, choose bound LMDB index probes, and improve join order. `EXPLAIN` must distinguish a guarantee that was unavailable, available but inapplicable, used to generate an alternative, selected, or rejected on cost, so a user can verify that the optimization is genuinely participating rather than merely seeing `optimizer.objectGuarantee` annotations.

This is a hard cutover. The completed repository contains one Cascades implementation, no runtime fallback to the old memo, no shadow planner, and no compatibility view that exposes the old memo or winner objects. Cascades selects a physical plan; evaluation remains the executor's responsibility.

## Progress

- [x] (2026-07-19 20:12Z) Read repository instructions, performance skills, the supplied packed-IR source, and the memo-idempotence survey.
- [x] (2026-07-19 20:12Z) Run the mandatory root JDK 25 quick clean install; all 122 reactor modules succeeded.
- [x] (2026-07-19 20:20Z) Freeze focused correctness, allocation, latency, and JFR baselines; existing plan assertions are the structural oracle.
- [x] (2026-07-19 22:13Z) Add packed mask, interner, memo, property, recipe, complete query-model codec, metadata, and architecture contracts.
- [x] (2026-07-19 22:13Z) Implement primitive arenas, mask kernels, interners, immutable packed query encoding, binding rows, and metadata side columns.
- [x] (2026-07-19 21:12Z) Implement packed memo groups, physical alternatives, goal-keyed winners, and linear selected-recipe extraction.
- [x] (2026-07-19 22:29Z) Build a deterministic physical incumbent, extract its selected recipe, materialize from winner links, and select mask width from canonical query symbols.
- [x] (2026-07-20 08:15Z) Derive packed binding facts, add ID-only filter placement, and implement winner-path-only connected-subset join search through sixteen factors.
- [x] (2026-07-20 18:02Z) Port normalization, property enforcement, physical implementations, query-syntactic finite-domain rewrites, OPTIONAL/MINUS rules, and eligibility/HAVING/alias rewrites to packed IDs; consuming stored LMDB predicate ranges remains open below.
- [x] (2026-07-20 11:24Z) Implement context-sensitive packed costing, selected-winner materialization, and LMDB-owned immutable plan/query-template caches.
- [x] (2026-07-20 11:24Z) Add dense, collision-safe sparse-long, and interned-multiword join-subset kernels with selected-path-only memo emission.
- [x] (2026-07-20 18:02Z) Finish the packed-only production cutover and remove the legacy memo, IR, rule DSL, shadow/fallback sources, and eager eligibility tree mutator.
- [ ] Activate stored LMDB predicate-range guarantees inside packed fact derivation, rule generation, costing, and diagnostics, with failing end-to-end tests captured before production changes.
- [ ] Run focused, module, corpus, allocation, JFR, and plan-quality acceptance gates.

## Surprises & Discoveries

- Observation: the existing four-factor `CascadesJoinSearchBenchmark` takes about 4.58 ms and allocates about 10.94 MB per plan; an eight-factor exhaustive workload takes about 4.14 seconds and allocates about 10.05 GB. JFR attributes substantial allocation to `HashMap`, `LinkedHashMap`, `IdentityHashMap`, stream construction, and their backing arrays.
  Evidence: the measurements and recording were captured during the design investigation that preceded this ExecPlan; repeat them in Milestone 1 and record the exact artifacts here.

- Observation: the supplied packed-IR prototype validates dense IDs, query-selected mask widths, primitive memo rows, and bulk-lifetime memory, but its hybrid header/body node layout failed its own sequential/full-scan target even though its 64-bit mask was about 4.6 times faster than `BitSet`.
  Evidence: `rdf4j-packed-ir-0.2.0-sources.zip` benchmark reports recorded roughly 1.016x sequential-header and 0.987x full-scan performance versus the object baseline, while `Mask64` measured roughly 1.977 ns versus 9.164 ns for `BitSet`.

- Observation: `PlanTemplateCache` is currently scoped to `LmdbEstimatorRuntime.OptimizationScope`, so it disappears after one optimization and cannot provide prepared-query hot-path reuse.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PlanTemplateCache.java` is constructed in the optimization scope and that scope is removed at completion. The replacement cache therefore belongs to `LmdbSailStore`.

- Observation: the reproducible JDK 25 baseline is worse than the earlier design-time latency sample while matching its allocation diagnosis. Four-factor AUTO/CHAIN measured 17.201 ms and 11,030,378 B/op; eight-factor AUTO/CHAIN with four predicates measured 6,830.809 ms and 10,055,015,392 B/op.
  Evidence: `/tmp/packed-cascades-baseline-4-auto.json` and `/tmp/packed-cascades-baseline-8-auto.json`. The eight-factor observation performed 144,015 candidate evaluations and 53,174 partition probes for one plan.

- Observation: the supported benchmark packaging route invokes repository formatting and changed wrapping/import order in fourteen existing Cascades files without changing logic.
  Evidence: `git diff` after packaging contains only import ordering, wrapping, and blank-line changes. Preserve these changes until the affected files are either edited or removed; do not mistake them for packed-planner behavior changes.

- Observation: the first packed foundation needs no third-party primitive library. A one-object-per-query mask layout plus primitive expression columns covers the 64/128/multiword cases and collision-safe structural interning directly.
  Evidence: `PackedMaskLayoutTest` passes 2 tests and `PackedExpressionInternerTest` passes 4 tests; logs are `logs/mvnf/20260719-202547-verify.log` and `logs/mvnf/20260719-202649-verify.log`.

- Observation: physical-property identity and winner lookup fit the same append-only primitive-table contract. Stronger ordering/masks remain distinct IDs, satisfaction is an explicit prefix/containment operation, and the dense ANY-goal slot avoids composite hashing on the default path.
  Evidence: `PackedPhysicalPropertyInternerTest` and `PackedWinnerTableTest` each pass 3 tests; logs are `logs/mvnf/20260719-205326-verify.log` and `logs/mvnf/20260719-205449-verify.log`. The forced-collision cases retain unequal rows.

- Observation: a frozen `PackedQuery` can remain a zero-copy memo base while query-local alternatives use disjoint expression handles. Read-only base lookup checks structural duplicates before the overlay, so repeated insertion reuses the original ID and a cross-group duplicate fails immediately without group merging.
  Evidence: `PackedMemoContractTest` passes 3 tests in `logs/mvnf/20260719-210311-verify.log`; it covers base reuse, helper-group permanence, separate logical/physical/winner identities, and absence of merge/alias/rename APIs.

- Observation: selected-plan extraction needs no map or per-node record. A primitive winner-ID remap preserves shared children and emits exactly one immutable physical recipe DAG; cycle detection remains an invariant check.
  Evidence: `PackedPlanRecipeTest` passes 2 tests in `logs/mvnf/20260719-211230-verify.log`.

- Observation: the boundary codec can structurally round-trip the common tuple operators, property paths, RDF-star operators, and the remaining non-aggregate scalar family while retaining subquery-valued scalar operands only as packed relational IDs.
  Evidence: focused green logs are `logs/mvnf/20260719-212106-verify.log`, `logs/mvnf/20260719-212511-verify.log`, and `logs/mvnf/20260719-213158-verify.log`. The final selection covers 24 additional scalar forms including `EXISTS`, `IN`, `ANY`, and `ALL`.

- Observation: all concrete query-model tuple and scalar families fit the packed codec without opaque nodes. `VALUES` uses canonical primitive binding rows; service configuration and tuple-function arguments/results use canonical payload rows; aggregate distinctness, custom arguments, and separators remain structural.
  Evidence: focused logs `logs/mvnf/20260719-215148-verify.log` and `logs/mvnf/20260719-215747-verify.log` are green.

- Observation: planner-relevant annotations must remain outside logical expression keys. Explicit variable scope, estimates, planned metrics, binary algorithm names, and join flags fit immutable metadata side columns, while the architecture marker rejects collection, stream, boxed-number, queue, optional, and `TupleExpr` fields from every packed hot-state class.
  Evidence: `logs/mvnf/20260719-220640-verify.log` and `logs/mvnf/20260719-221059-verify.log` are green. The combined packed selection passes 26 tests with zero failures or errors.

- Observation: base relational IDs are topological because the codec interns relational children before their parents. The initial physical pass can therefore install child winners with one dense forward scan, no queue, recursion, or visited set; selected-recipe materialization uses recipe child IDs rather than the original logical children.
  Evidence: the contract changed from `ClassNotFoundException` in `logs/mvnf/20260719-221615-verify.log` to green in `logs/mvnf/20260719-222121-verify.log`. The expanded packed selection passes 28 tests.

- Observation: binding symbols can reuse canonical string object IDs as an allocation-flat direct key. A dense symbol side table discovers names while encoding terms, projection aliases, extensions, grouping, and binding rows, and supplies the exact width to `PackedMaskInterner`.
  Evidence: the 129-binding contract failed with missing `PackedQuery.symbolCount()` in `logs/mvnf/20260719-222326-verify.log` and passes in `logs/mvnf/20260719-222539-verify.log`.

- Observation: canonical output/dependency mask IDs are sufficient to drive the first semantics-safe packed rewrite. A filter whose deterministic scalar dependencies fit exactly one inner-join input is placed on that input through ID sinks and permanent helper groups; no replacement algebra tree or alternative list exists during the rule.
  Evidence: the red structural plan is in `logs/mvnf/20260720-044539-verify.log`; the green ID-only rule is in `logs/mvnf/20260720-044837-verify.log`.

- Observation: inserting every provisional subset improvement into the memo defeats the compact-forest design even with primitive rows. At sixteen connected factors this took 27.230 ms and allocated 47,389,069 B/op. Keeping parent/cost state in dense arrays and emitting only the final winner path reduced the same end-to-end plan to 0.133 ms and 1,920,393 B/op.
  Evidence: supported-runner JMH with `-prof gc` on `PackedCascadesSearchBenchmark.plan`; the four/eight-factor packed results are 0.005 ms / 36,096 B and 0.065 ms / 215,184 B. The sixteen-factor before/after measurements are recorded in the task transcript, and the selected connected order remains green in `logs/mvnf/20260720-060315-verify.log`.

- Observation: context-sensitive access costing does not require maps of bound variables. The selected prefix is an integer relation-ID slice; LMDB derives constant and prefix-bound SPOC masks directly into one reusable primitive estimate slot, and only the selected recipe copies provider strings.
  Evidence: all fourteen finite-values surface cases pass in `logs/mvnf/20260720-090400-verify.log`; nested mobile filters retain finite prefix fanout without constructing replacement tuple trees.

- Observation: join subsets need three explicit kernels rather than allowing the dense integer mask to overflow. Seventeen through sixty-four factors use a collision-safe open-addressed `long` table whose subset key never changes; larger regions intern multiword prefix masks and keep selection state in a shared `long[]`.
  Evidence: `PackedJoinSubsetKernelTest` passes the 16/17/64/65 boundaries and end-to-end 17/65-factor regions in `logs/mvnf/20260720-111551-verify.log`; `PackedLongSubsetTableTest` passes a forced-hash-collision and derived-state-improvement contract in `logs/mvnf/20260720-111243-verify.log`.

- Observation: the final eager LMDB eligibility optimizer was four dependent `TupleExpr` mutations whose success depended on wrapper shape. Encoding them as one projection-level packed alternative makes the semantic preconditions atomic: assured alias source, positive count, duplicate-insensitive aggregates, dead branch locals, and group-key-only correlation must all hold before insertion.
  Evidence: the pre-cutover regression fails 1/1 in `logs/mvnf/20260720-174909-verify.log`; the seven positive/negative packed cases pass in `logs/mvnf/20260720-180102-verify.log`, and all 40 `LmdbOptimizerPipelineTest` cases pass in `logs/mvnf/20260720-180236-verify.log`.

- Observation: the packed cutover currently preserves predicate-domain text as plan annotation but does not feed the domain into packed exploration. `LmdbTupleExprEstimateAnnotator` is the only current consumer that attaches `optimizer.objectGuarantee`; `PackedQueryCodec` derives finite values only from equality, `IN`, and `OR` syntax already present in the query; and `LmdbPackedCostModel` can cost a `BindingSetAssignment` prefix but never calls `LmdbEstimatorRuntime.rdfTermDomain(IRI)`. A stored guarantee can therefore be visible in `EXPLAIN` while generating no empty-set, tautology, range-anchor, access-path, or join-order alternative.
  Evidence: repository searches on 2026-07-21 found `getKnownRdfTermDomain` only in the tuple annotator and eager filter simplifier, while the packed planner boundary accepts only `PackedCostModel`. The current `LmdbPredicateObjectDomainIndexTest` assertions for `selected=finite-anchor:o` and generated guarantee options identify the missing observable behavior.

## Decision Log

- Decision: use structure-of-arrays primitive arenas instead of reproducing the prototype's hybrid node layout.
  Rationale: parallel arrays give selective column access, dense scans, and no per-node object or header cost; the prototype's own scan result does not justify copying its exact layout.
  Date/Author: 2026-07-19 / Codex.

- Decision: make normalization plus intern-or-return-existing the only insertion path, with immutable key columns and explicit structural comparison after every hash hit.
  Rationale: idempotence must be an insertion invariant, not a cleanup pass. Cost and derived facts are not expression identity.
  Date/Author: 2026-07-19 / Codex.

- Decision: do not support group merge, alias, rename, or rehash operations.
  Rationale: the normalizing factory creates initial groups bottom-up, while semantics-preserving rules receive their target group. Permanent group IDs avoid mutable keys and the high complexity of Calcite/ORCA-style reindexing. A cross-group structural duplicate is a rule-contract defect and must fail an invariant test.
  Date/Author: 2026-07-19 / Codex.

- Decision: use query-local single-threaded mutable search state and cache only immutable normalized query templates and extracted physical recipes.
  Rationale: locking a live memo would add overhead and metadata-lifetime risk. Store-level reuse belongs above the ephemeral memo.
  Date/Author: 2026-07-19 / Codex.

- Decision: use an interpreted, statically compiled packed rule program rather than runtime Java code generation or a new primitive-collection dependency.
  Rationale: planning is cold and latency-sensitive; code-generation startup and class-lifetime costs cannot be amortized reliably. In-repository primitive arrays provide the required layout without a dependency.
  Date/Author: 2026-07-19 / Codex.

- Decision: preserve `CascadesPlanner.optimize(TupleExpr, OptimizationGoal)` as the boundary while breaking experimental Cascades result and extension APIs that expose legacy internals.
  Rationale: the rest of RDF4J continues to use `TupleExpr`, while the user explicitly requested no backward-compatibility layer inside Cascades.
  Date/Author: 2026-07-19 / Codex.

- Decision: represent a predicate-object range guarantee as a backend-supplied, sound upper bound on the possible RDF terms produced by a constant-predicate statement pattern, and keep it separate from cardinality evidence.
  Rationale: kinds, datatypes, canonical-value facts, finite possible values, and integer bounds can prove semantic rewrites and legal concrete probes, but they do not say how frequently a value occurs. Treating a possible-value domain as an exact bag or as an execution count would undercost repeated nested-loop probes and can change bag multiplicity.
  Date/Author: 2026-07-21 / Codex.

- Decision: inject predicate ranges through a dedicated packed semantic-fact provider during encoding, not by parsing `optimizer.objectGuarantee` strings and not by hiding semantic rewrites inside the cost model.
  Rationale: string metrics are diagnostics rather than a typed proof boundary, while a cost model may rank alternatives but must not silently change logical semantics. A typed provider lets the codec intern facts into primitive side columns before rule saturation and gives non-LMDB callers an explicit no-facts implementation.
  Date/Author: 2026-07-21 / Codex.

- Decision: every applicable range optimization is an ordinary idempotent memo alternative with proof lineage; the original expression remains available unless the range proves the group empty.
  Rationale: branch-and-bound should compare the original scan/filter with guarantee-derived anchors and simplifications. This prevents a stored metadata feature from becoming an uncosted fast path and makes generated-but-not-selected behavior observable.
  Date/Author: 2026-07-21 / Codex.

## Outcomes & Retrospective

The packed planner is now the only production Cascades route. The old memo, object IR, rule DSL, join-search object model, shadow/fallback modes, and eager eligibility mutator have been removed. The query codec, packed memo, three join-subset kernels, LMDB ID-based costing, selected-recipe materialization, and store-owned cache are integrated. The complete 40-case LMDB optimizer-pipeline regression was green at the recorded checkpoint. Remaining work is to restore consumption of stored predicate-object ranges inside packed planning, then run the full module, corpus, allocation, JFR, and plan-quality closeout.

## Context and Orientation

The work is centered in Maven module `core/queryalgebra/evaluation`, package `org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades`. `CascadesPlanner` is the current search coordinator. `Memo`, `MemoGroup`, `MemoExpr`, `Winner`, and `SearchState` form an object- and collection-heavy memo/search model. The sibling `cascades.ir` package is also object-based, and `cascades.dsl.CompiledRule` repeatedly crosses between `TupleExpr` and that IR. Join enumeration under `cascades.join` contains several overlapping memo, contributor, recipe, and candidate object models.

Production construction currently occurs in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizer.java`. LMDB supplies statistics, implementation choices, and planning configuration. `LmdbSailStore` is the long-lived store object and is therefore the owner of the new cross-query caches. The evaluator consumes the selected `TupleExpr`; no executor operator or evaluation algorithm is changed by this work.

A packed arena is a group of primitive arrays whose shared index is an identifier. Identifier zero means absent; valid identifiers begin at one. Structure-of-arrays means that operator tags, child IDs, payload IDs, hashes, costs, and flags live in separate contiguous arrays rather than one Java object per row. A memo group is an equivalence class of expressions with the same logical result. A physical property describes requirements such as ordering or execution domain. A winner is the cheapest physical recipe for one group under one required-property and semantic context.

A predicate-object range guarantee is store-maintained metadata saying that every object currently stored for one predicate lies within a described RDF term domain. `RdfTermDomain` in `core/sail/lmdb` is the source representation. Its states `UNKNOWN` and `UNRESTRICTED` authorize no semantic optimization. A known domain can describe possible RDF kinds, language presence, XSD datatypes, universal facts such as canonical integer/date forms, a finite set of possible values, and an inclusive canonical-integer range. An empty domain proves that the statement pattern has no rows. These are semantic guarantees, not selectivity estimates.

Three quantities must never share a packed field. A possible-value domain lists values that may occur and is safe for restricting probes. An exact finite relation is a `BindingSetAssignment` with concrete rows and bag multiplicities. An execution count is the number of times a nested-loop child opens under its prefix. The optimizer may derive an exact one-row-per-distinct-value anchor from a finite possible-value domain because joining that anchor with the original statement pattern preserves the original statement multiplicity, but it must still cost output rows and repeated opens from LMDB cardinalities and prefix rows.

Create production classes under `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed/`. Tests belong under the matching test package. During migration, legacy code may be invoked only by tests as a golden oracle. Do not add a production flag, fallback, or shadow route.

## Plan of Work

### Milestone 1: Freeze behavior and performance

Run the existing rule-engine, memo-model, typed-semantics, property, and join-region tests before changing production code. Capture their Surefire summaries and golden plan snapshots for representative queries. Run `CascadesJoinSearchBenchmark.plan` for the known four- and eight-factor shapes with GC allocation profiling, then repeat the four-factor case with JFR. Record commands, JVM, hardware, benchmark parameters, and artifact paths in this document. Add a versioned corpus benchmark whose query and search-mode parameters are checked into the test tree; do not tune the corpus after seeing results except through an explicit Decision Log entry.

Acceptance for this milestone is reproducible baseline evidence and snapshots that can serve as the oracle after legacy classes are removed. Production code remains unchanged.

### Milestone 2: Primitive foundations and immutable query codec

Write the smallest failing tests first. `PackedMaskTest` covers one-word, two-word, and multiword masks at 0, 63, 64, 127, 128, and larger binding counts. `PackedExpressionInternerTest` forces equal keys, unequal keys with identical supplied hashes, resize boundaries, and child-slice equality. `PackedQueryCodecTest` starts with leaves, unary nodes, binary nodes, and scalar operands, then grows until every concrete built-in `TupleExpr` and `ValueExpr` subtype has a structural encoding test. An unrecognized subtype must fail with its class and operator path before search.

Implement `PackedMaskLayout`, `PackedIntVector`, `PackedObjectPool`, and one reusable open-addressing kernel. Tables use a power-of-two capacity, linear probing, no deletion, and a maximum 0.65 load factor. Store the 64-bit mixed hash with each row and compare all key fields after a hash match. Geometrically grow arrays only when the query-size estimate or work budget was insufficient.

Implement `PackedQuery` with separate relational, scalar, child-vector, payload, binding, and object-pool arenas. Common child zero and child one live in direct columns; larger arities use a vector slice. The codec performs iterative bottom-up traversal with primitive stacks and a custom object-identity table. It normalizes safe commutative fields before interning. Packed rows contain no complete `TupleExpr`; only boundary payload values needed to reconstruct the winner may enter the object pool.

Acceptance is complete structural coverage, deterministic IDs and fingerprints, collision-safe interning, and no forbidden collection-typed fields in the packed hot-path classes.

### Milestone 3: Idempotent memo, properties, and winners

Add failing `PackedMemoContractTest`, `PackedPropertyInternerTest`, and `PackedWinnerTableTest`. Reinsertions must return the same expression and group IDs without increasing counts. A structurally identical expression offered to another group must throw the invariant exception. Key columns must have no mutator. Required properties must not alter logical identity. Stronger delivered properties may satisfy weaker requirements but remain distinct property IDs. Winner updates must be monotonic under the configured cost ordering.

Implement `PackedMemo` as an immutable `PackedQuery` base plus query-local overlay arrays. Logical-expression identity consists of operator, canonical payload, child groups, semantic scope, and execution domain. Physical-expression identity consists of physical operator, canonical payload, child groups, delivered property, and implementation form. Goal/winner identity consists of group, required property, semantic scope and row goal, input context, and cost policy. Store costs and estimates in primitive columns; materialize `CostVector` only for the final result or diagnostics.

Implement separate property interning and explicit satisfaction/dominance routines. Store default-property winners in a direct `int[]` indexed by group and all other goals in one primitive composite-key table. Store child winner/requirement links as vector slices. Drop the whole query-local overlay at completion; never detach or share it.

Acceptance is idempotence under repeated and adversarial insertion, permanent group IDs, correct property-aware winners, and linear winner extraction into `PackedPlanRecipe`.

### Milestone 4: Packed rule and search engine

Characterize each rule family before porting it. Add a failing packed-rule parity case, implement that family, and run the same legacy golden selection. Compile rule descriptors once into primitive pattern, guard, proof, and substitution arrays. At query time, root-operator indexes, applied-rule masks, generation stamps, primitive ring queues, and integer heaps drive matching. Rules receive a target group and emit through a sink; they cannot allocate collections or construct `TupleExpr` trees.

Port cost and implementation providers to ID-based read views and primitive output sinks. Generate a deterministic executable incumbent before exploration. Search requested group/property goals top-down, use the incumbent as the branch-and-bound ceiling, reject candidates whose lower bound exceeds it, and examine expensive/disqualifying children first. Restrict interesting properties to root requests, operator requirements, and safe non-dominated deliveries.

Replace generic inner-join associativity/commutativity rules with a single n-ary connected-region enumerator. For up to 16 factors use arrays indexed by subset mask. For 17 through 64 factors use a sparse primitive long-mask table. Above 64 factors intern multiword masks. Enumerate connected subsets using DPhyp-style graph edges, with explicit legality masks for OPTIONAL, MINUS, EXISTS, correlation, required outer inputs, and Cartesian products. Retain only safely non-dominated recipes for each subset/property/input context.

`AUTO` and `BUDGETED` use deterministic work budgets with deadline sampling outside inner loops. `EXACT` uses identical representation and safe pruning but exhausts all enabled convergence-contracted work. Remove `SHADOW` and `SHADOW_BUDGETED`; do not emulate them.

Acceptance is deterministic parity on the frozen corpus, convergence independent of rule registration order, no complete alternative-tree construction during search, and no collection/stream allocation attributed to packed rule matching, costing, or join enumeration.

### Milestone 5: Materialization, caches, and hard cutover

Add a failing end-to-end test that optimizes representative RDF4J queries through `LmdbCascadesOptimizer`, materializes one winner, and produces the same query results as the frozen baseline. `PackedPlanMaterializer` follows winner links iteratively and constructs only the chosen `TupleExpr`, preserving values, binding names, ordering, row goals, physical annotations, and provenance.

Redefine `CascadesPlan` to hold one non-null selected `TupleExpr`, final `CostVector`, `OptimizationSearchStatus`, `PlanProvenance`, and immutable `PlanningMetrics`. Remove its live memo, optional winner, pending-task collection, compatibility constructors, `approximate()` compatibility view, and legacy completeness bridge. When no physical implementation exists or a node cannot be encoded, throw `CascadesPlanningException`; never return to another optimizer.

Implement two immutable cache entry types. A selected-plan entry stores a `PackedPlanRecipe` and required payload template. A normalized-query entry stores `PackedQuery` arrays that a fresh memo overlay can reference without copying. Key them by a 128-bit normalized fingerprint, dataset and binding shape, parameter-selectivity variant, goal, rule/catalog and implementation-provider versions, and statistics/data revision. Verify hits structurally. Use fixed-capacity segmented primitive tables, bounded frequency admission, lazy stale-entry eviction, and single-flight construction. The segment lock is the only shared-planning lock. Place ownership in `LmdbSailStore`; remove optimization-scope ownership from `LmdbEstimatorRuntime`.

After end-to-end and performance gates pass, switch `LmdbCascadesOptimizer` directly to the packed planner. Delete the legacy memo/search/IR/join-contributor implementation, shadow modes, fallback tests, and compatibility APIs in the same milestone. Update tests to assert the final API, not the deleted model.

Acceptance is exactly one production Cascades implementation, a cache hit that bypasses search, fail-fast unsupported operators, correct invalidation, concurrent single-flight behavior, and unchanged query results.

### Milestone 6: Activate predicate-object range guarantees

Start with the smallest failing end-to-end tests in `core/sail/lmdb`; do not change production code until their Surefire reports are captured in `initial-evidence.predicate-ranges.txt` using the named Maven workspace `predicate-ranges`, preserving the earlier `initial-evidence.txt`. First pin the current regression: a repository with a known canonical integer, boolean, or finite predicate range can still show `optimizer.objectGuarantee`, but packed Cascades neither reports `optimizer.guaranteeOptions=generated=1` nor selects the expected `BindingSetAssignment` direct-lookup alternative. Use the existing methods in `LmdbPredicateObjectDomainIndexTest` as the first selectors where they already fail, and add `LmdbPackedPredicateRangePlanningTest` for cases not isolated there.

Add a backend-neutral `PackedPredicateRangeProvider` and reusable `PackedPredicateRange` result slot in the packed package. The provider method is `boolean describeObjectRange(IRI predicate, PackedPredicateRange output)`; `false`, `UNKNOWN`, and `UNRESTRICTED` all mean that no proof is available. The result exposes a state, possible RDF-kind bits, possible language/datatype bits, universal canonical-value bits, optional inclusive integer bounds, and zero or more finite `Value` instances through indexed access. It owns no collection and is reset and reused by the codec. Add `long predicateRangeVersion()` so cache keys change when the encoding or semantics change. Existing `PackedCascadesPlanner` overloads delegate to a no-facts provider; add overloads that accept both `PackedCostModel` and `PackedPredicateRangeProvider` without combining the two responsibilities.

In LMDB, implement `LmdbPackedPredicateRangeProvider` as the translation boundary from `LmdbEstimatorRuntime.rdfTermDomain(IRI)` to the backend-neutral result slot. `LmdbCascadesOptimizer` passes this provider for every packed optimization. Preserve `LmdbPredicateObjectDomainSource` and `RdfTermDomain` as the store/index contract; do not move LMDB classes into the core query module. Include the predicate-range semantic version in `PackedPlanCache.Context` together with the existing data/statistics revision, and bump `LmdbPackedCostModel.VERSION`. A disabled, excluded, unreadable, stale, or unknown guarantee must produce no fact and no rewrite. A store mutation or range-index rebuild that changes the range revision must miss both normalized-query and selected-plan cache entries.

Split packed query construction into explicit encode, fact-derive/rule-saturate, and freeze phases. During encoding, intern each known range into a `PackedPredicateRangeArena` and attach its ID to the object symbol of a constant-predicate `StatementPattern`. `PackedPredicateRangeArena` uses primitive state/mask/bounds columns plus object-pool slices for datatypes and finite values; structurally equal ranges reuse one ID. `PackedDomainFacts` stores a range ID per logical group and binding symbol. A statement pattern seeds its object fact. Transparent unary operators retain facts; projection and trivial aliases remap them; `Join` and `Lateral` intersect independently valid facts for a shared symbol; `Union` unions possible values while retaining only universal facts valid in both branches; `Difference` retains only left facts; group keys retain their input facts but aggregates do not; and a `BindingSetAssignment` contributes an exact finite domain without turning its row count into a predicate frequency. Right-only `LeftJoin` facts remain conditional and cannot discharge an outer filter unless existing assured-binding analysis proves the variable present. Variable predicates, service boundaries, unsupported tuple functions, and unknown domains stop propagation rather than guessing.

Run `PackedLogicalRuleProgram` to deterministic quiescence after facts are available. Key applications by rule, target group, and fact revision so repeated discovery is idempotent. A stronger group fact increments its revision and re-enqueues only dependent parent/rule pairs; unchanged facts or duplicate alternatives do nothing. Preserve the original logical expression while adding these alternatives:

* A contradiction alternative produces `EmptySet` with zero rows when the range is empty or an assured-bound filter cannot be true for any value in the range.
* A tautology alternative drops a filter only when every possible range value makes the supported predicate true and the referenced variable is assured bound. Support `isIRI`, `isBNode`, `isLiteral`, `isNumeric`, equality/inequality of `datatype`, empty/non-empty `lang`, and `langMatches`; simplify `AND`, `OR`, and `NOT` using SPARQL effective-boolean-value and error semantics rather than Java booleans.
* A finite-anchor alternative intersects `=`, `IN`, equality-`OR`, and bounded canonical-integer filters with the stored range. Generate at most 64 distinct values. Expand boolean lexical equivalents and calendar/numeric lexical forms only when the stored canonical facts prove value equivalence safe. The `BindingSetAssignment` must be joined with the original statement pattern, not replace it. Remove the filter only when the generated values exactly encode its truth set; otherwise retain it.
* A finite stored domain may generate an anchor even without a finite query filter when the value count is within the same 64-value guard. It remains a candidate, not a forced winner.
* Cost each anchor as concrete LMDB lookups through `LmdbPackedCostModel`. A selected object anchor must expose a bound `[P, O]` lookup, or `[S, P, O]` when the prefix also binds the subject. Exact relation multiplicity describes the anchor rows; LMDB estimates describe matching output; prefix rows describe repeated opens. Never assign uniform frequency or exact confidence merely because the domain is finite.

Preserve and extend proof/diagnostic output. Every constant-predicate statement pattern with a known range retains `optimizer.objectGuarantee` and `optimizer.objectGuaranteePredicate`. Range consumers add `optimizer.guaranteeOptions=generated=<n>, selected=<name-or-original>`, stable candidate names such as `empty-domain:<binding>`, `empty-filter:<kind>`, `drop:<filter>`, and `finite-anchor:<binding>`, plus `optimizer.guaranteeOptionCandidates` entries containing generated, valid/rejected, estimated work, and a stable rejection reason. Add planning counters for ranges read, applicable ranges, alternatives generated, alternatives selected, and contradictions. If a guarantee is visible but unused, `EXPLAIN` must say one of `unassured-binding`, `unsupported-filter`, `too-many-values`, `unsafe-lexical-equivalence`, `candidate-dominated`, or `budget-exhausted`; silence is not acceptable.

Acceptance for this milestone is result-bag parity plus observable activation. Cover: IRI-only versus `isLiteral` becoming empty; IRI-only versus `isIRI` offering a filter-free alternative; canonical integer bounds producing selected finite direct lookups; boolean `true` expanding safely to `true` and `1`; mixed or non-canonical numeric datatypes retaining the original filter; disabled/excluded/unknown guarantees producing no semantic alternative; shared-variable join domains intersecting; union domains widening safely; right-only OPTIONAL facts not escaping; a generated anchor losing on cost while remaining visible; and cache invalidation after an insert broadens a range. Existing `LmdbPredicateObjectDomainIndexTest`, `LmdbOptimizerPipelineTest`, and theme-query plan gates must pass without weakening bag-result, anchor-order, access-mode, or proof assertions.

### Milestone 7: Acceptance and closeout

Run focused tests, the complete query-evaluation module, the LMDB module, the versioned JMH corpus, allocation profiling, and JFR. Add a reflection architecture test for classes marked as packed hot path: fields may not be `Map`, `Set`, `List`, `Collection`, `Queue`, `Optional`, streams, boxed numeric state, or complete `TupleExpr`. Allow boundary codec/materializer classes to reference `TupleExpr` explicitly.

Compare selected plan estimated cost and observed query execution against the golden corpus. Query results must be identical. Median plan-quality regret must be at most 1 percent and p95 at most 10 percent. Planning acceptance is the p95 distribution and per-shape latency/allocation goals from Purpose / Big Picture. Report encoding, search, extraction, materialization, and cache time separately.

Acceptance is a formatted tree, green focused and module tests, satisfied performance and plan-quality gates, no unexpected tracked files, and this document updated with final evidence and retrospective.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`.

The mandatory initial install is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Focused tests use the repository runner, which performs the required install and writes isolated reports:

    python3 .codex/skills/mvnf/scripts/mvnf.py PackedMaskTest#boundaryWidths --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedExpressionInternerTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedMemoContractTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedQueryCodecTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace predicate-ranges LmdbPredicateObjectDomainIndexTest#canonicalIntPredicateWithSingleXsdIntUsesGuaranteedDirectLookup --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace predicate-ranges LmdbPredicateObjectDomainIndexTest#booleanPredicateExpandsTrueFilterToBooleanLexicalEquivalents --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace predicate-ranges LmdbPackedPredicateRangePlanningTest --retain-logs

Broader tests are:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run the existing benchmark and retain a JFR recording with:

    scripts/run-single-benchmark.sh --module core/queryalgebra/evaluation --class org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.CascadesJoinSearchBenchmark --method plan
    scripts/run-single-benchmark.sh --module core/queryalgebra/evaluation --class org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.CascadesJoinSearchBenchmark --method plan --enable-jfr

Before final verification, check headers and format through the repository resource phase:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources

Do not pass `-am` or `-q` to a test command. Preserve untracked evidence, benchmark, and profile artifacts.

## Validation and Acceptance

Correctness acceptance requires all built-in relational and scalar operator encodings, collision-safe idempotent insertion, immutable key columns, permanent group IDs, physical-property satisfaction and dominance, deterministic rule convergence, legal join enumeration, one-winner materialization, active predicate-range consumption, and end-to-end query-result parity. Cover empty and single-node queries, duplicate subexpressions, hash collisions, more than 64 and 128 bindings, more than 64 join factors, correlated and non-commutative joins, no viable physical implementation, deadline/work exhaustion with an incumbent, cache collision, stale statistics, parameter variants, concurrent cold misses, and every positive and negative predicate-range case in Milestone 6. A known applicable range that appears only as `optimizer.objectGuarantee` without either a generated candidate or a stable inapplicability reason fails acceptance.

Architecture acceptance rejects collection-backed or `TupleExpr`-backed state in packed hot-path classes. Query-local search is single-threaded and lock-free. The cross-query cache is bounded and segmented. No new dependency, off-heap storage, runtime code generation, executor change, compatibility adapter, shadow route, or runtime fallback remains.

Performance acceptance uses JDK 25 and records hardware and flags. At least 90 percent of versioned query/mode cells must have p95 planning time below 5 ms and at least 50 percent below 0.5 ms; `EXACT` is included without exemption. The four-factor case must be below 0.5 ms and 512 KiB per plan, the eight-factor case below 5 ms and 2 MiB, and validated final-plan cache hits below 0.5 ms. JFR must attribute no collection or stream allocation to packed encoding and search. Performance claims require repeatable JMH results plus matching allocation/profile evidence; JIT explanations remain provisional unless compiler evidence is collected.

## Idempotence and Recovery

All new insertions are safe to repeat because interning returns existing IDs after structural comparison. Predicate-range facts are interned structurally, fact revisions advance only when a group becomes strictly more constrained, and rule applications are keyed by that revision. Tests and benchmarks may be rerun without deleting untracked artifacts. Cache entries are immutable and dependency-stamped; a stale or partially built entry, including one whose predicate-range or data revision changed, is ignored and rebuilt through single-flight coordination.

During migration, keep the tree compiling at each milestone. If a packed family fails parity, keep production on the old planner, fix the packed implementation, and do not add a fallback within the packed path. The hard cutover occurs only after full supported-operator and end-to-end parity. Once old files are removed, recover only by reverting the complete cutover commit; do not resurrect a dual path.

## Artifacts and Notes

The initial root install completed with `BUILD SUCCESS`, 122 successful reactor modules, and total wall time 1 minute 40 seconds at 2026-07-19T22:12:10+02:00. Full filtered output is in ignored `maven-build.log`.

The focused pre-cutover selection is persisted in `initial-evidence.txt`: 307 tests, zero failures/errors/skips, 4.372 seconds, with Maven log `logs/mvnf/20260719-201514-verify.log`.

Milestone 6 must preserve its first failing predicate-range selector in `initial-evidence.predicate-ranges.txt`; its Maven log and Surefire reports live under the exact `.mvnf/workspaces/predicate-ranges/` paths printed by `mvnf`. Do not overwrite the pre-cutover evidence.

The JDK 25 ARM64 benchmark artifacts are `/tmp/packed-cascades-baseline-4-auto.json` and `/tmp/packed-cascades-baseline-8-auto.json`. The matching six-second allocation recording is `/tmp/cascades-plan-4-auto.jfr`; its leading allocation sites are `LinkedHashMap.newNode` 6.96%, `IdentityHashMap.init` 5.92%, `HashMap.resize` 5.58%, stream construction 7.21% combined, and `HashMap.newNode` 3.07%.

Baseline artifacts to add here during Milestone 1 are the focused Surefire summaries, golden plan snapshots, JMH result files, GC allocation output, JFR recording, and a short JFR allocation table. Preserve their exact paths.

## Interfaces and Dependencies

`PackedQueryCodec.encode(TupleExpr root, OptimizationGoal goal)` returns an immutable `PackedQuery` and normalized root goal ID. Its planning overload also accepts `PackedPredicateRangeProvider`; a provider writes into the reusable `PackedPredicateRange` result described in Milestone 6. `PackedMemo` accepts that query and exposes integer read methods plus `internLogical`, `internPhysical`, `internProperty`, and winner update operations; callers never receive mutable arrays. `PackedRuleProgram.apply(int expressionId, int targetGroupId, PackedRuleSink sink)` emits alternatives. `PackedCostModel` and packed implementation providers receive read-only ID views and write into reusable primitive result slots. `PackedPlanExtractor.extract(int rootGroupId, int goalId)` returns `PackedPlanRecipe`. `PackedPlanMaterializer.materialize(PackedQuery, PackedPlanRecipe)` returns the selected `TupleExpr`.

The public experimental boundary remains `CascadesPlanner.optimize(TupleExpr, OptimizationGoal)`. The final `CascadesPlan` has one selected plan and no live memo. `LmdbSailStore` owns one bounded `PackedPlanCache`; `LmdbCascadesOptimizer` injects it together with `LmdbPackedCostModel`, `LmdbPackedPredicateRangeProvider`, statistics/data revision, and both provider versions. All implementation uses the JDK already available in the project; do not add libraries.

Revision note (2026-07-19 / Codex): initial self-contained ExecPlan created from the approved packed, idempotent Cascades design and the repository-specific performance investigation. Updated after Milestone 1 to record the reproducible JDK 25 tests, benchmark artifacts, JFR allocation sites, and formatter side effect; updated through the primitive memo, winner-recipe extraction, relational/scalar codec slices, deterministic incumbent, recipe materialization, and query-derived mask width.

Revision note (2026-07-21 18:05Z / Codex): added Milestone 6 after inspection showed that stored LMDB predicate-object ranges survive only as annotations in the packed route. The revision defines the typed fact-provider boundary, domain propagation and soundness rules, optimizer consumers, cost semantics, activation diagnostics, cache invalidation, and red/green acceptance cases so a visible guarantee cannot silently remain unused.
