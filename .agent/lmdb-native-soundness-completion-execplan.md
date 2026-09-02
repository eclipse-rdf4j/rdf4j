# Make the LMDB native query engine sound and semantically complete

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current while the work proceeds. This document follows `.agent/PLANS.md` and
is self-contained. It supersedes, but does not reopen, the historically completed native-engine plan.

## Purpose / Big Picture

The LMDB store has a native query engine that keeps RDF values in compact LMDB identifiers, plans joins and
aggregates directly over native cursors, and can specialize replay-safe fragments with an interpreter or Janino. The
engine is currently opportunistic: several optimizer proofs are unsound, some lifecycle paths can leak or wedge, and
many legal query shapes fall back to RDF4J's generic evaluator. The goal is first to make every known native/LMDB
optimization semantically correct, then to give every legal algebra and expression shape a correct native row or group
implementation.

After completion, a query using no more than 60 simultaneously live native slots runs through a native semantic plan.
Only `Intersection`, `Service`, and the explicit `CAPACITY_MAX_NATIVE_SLOTS` outcome may use generic evaluation by
default. A user can see the result by running the dynamic route census and differential suites: answers match the
independent MemoryStore and standard-pipeline oracles, while route telemetry reports no other generic host, island, or
expression bridge.

## Progress

- [x] (2026-08-23 19:04Z) Confirmed branch `optimize-lmdb` at
  `6e62d9976abc2b6e433ec19324cff562063bf11d`, preserved the user-owned benchmark edit and untracked artifacts, and ran
  the mandatory root quick install successfully.
- [x] (2026-08-23 19:21Z) Established the independent four-arm oracle and evidence helpers; all six focused self-tests
  pass in isolated workspace `oracle-foundation`, with retained evidence in `initial-evidence.oracle-foundation.txt`.
- [x] (2026-08-23 21:37Z) Reproduced and resolved every known soundness-ledger defect, or strengthened and recorded it
  as unreproduced without a production change. Exact red/green evidence covers aggregate liveness, strategy views,
  optimizer proofs, binding metadata, group scope, projection, slice arithmetic, ordered laziness, lifecycle cleanup,
  native-source locking, parser/model composition, and cooperative cancellation through native roots, kernels,
  exchange queues, aggregation workers, timeout wrappers, and cursor ownership.
- [x] (2026-08-23 19:54Z) Replaced the three unsound optimizer proof branches: null rejection now uses possible SPARQL
  EBV outcomes, OPTIONAL-union exclusion uses `EqualityDomainProof`, and fully bound probe uniqueness uses only an
  explicit/assured context or one fixed dataset context. The three owning classes pass 19, 19, and 47 tests in
  workspace `optimizer-fixes`; retained reports are under the optimizer evidence tree.
- [x] (2026-08-23 20:02Z) Corrected the query-model binding contracts and audited their consumers. Possible and assured
  VALUES names are now distinct and cache-safe, standalone `ReifiedTripleRef` no longer claims a binding it does not
  produce, and the focused model, evaluator, optimizer, LMDB placer, and parser/evaluation companion classes are green;
  retained reports are under `model-contracts`.
- [x] (2026-08-23 20:08Z) Strengthened the payload-free hash-chain fixture and classified the historical row-loss
  report as unreproduced on the pinned tree. Untouched defaults and a forced semantic-hash route both return the exact
  8,400-row four-arm multiset; the forced route records 4,200 counted-chain probes. No production hash change was made.
- [x] (2026-08-23 20:15Z) Ran both deterministic soundness fuzz classes. `LmdbNativeDifferentialFuzzTest` passed 26
  tests and `LmdbNativePromotionFuzzTest` passed five tests in isolated workspace `soundness-corpora`.
- [x] (2026-08-23 20:52Z) Minimized and resolved all four initial SPARQL 1.2 corpus divergences. Three independent
  parser/model red/fix/green cycles corrected nested `TRIPLE(...)`, quoted-triple value expressions, and nested
  directional-language function arguments. The remaining empty join was a fixture gap: after adding one identical RDF
  triple term beneath distinct subjects, both joins return exactly two rows in every independent oracle arm with no
  production change. The complete nine-test LMDB corpus is green.
- [x] (2026-08-23 20:27Z) Fixed nested `TRIPLE(Expression, Expression, Expression)` construction after independent
  parser-model and LMDB reds. Arbitrary component expressions are lifted into ordered Extensions; the parser selector
  and identical LMDB selector are green.
- [x] (2026-08-23 20:52Z) Made quoted triple terms legal general value expressions and retained arbitrary nested
  expressions in `STRLANGDIR`, `LANGDIR`, `hasLANG`, and `hasLANGDIR`. The generated parser was refreshed through the
  deterministic record/check workflow. The owning parser classes pass 28 and 18 tests; retained evidence is under
  `corpora`.
- [x] (2026-08-23 20:30Z) Completed the three reproduced lifecycle slices. Ordered row materialization now begins on
  first demand and performs no work when closed beforehand; failed dataset construction closes provisional adjacency
  and transaction resources in reverse order with suppressed cleanup failures; and all dataset planner-statistics
  reads hold the native-source read lock across the open check and access. The five former-red lifecycle selectors and
  the 27-test row-step class are green in workspace `lifecycle-reds`; retained red/green reports are under `lifecycle`.
- [x] (2026-08-23 20:50Z) Isolated the legacy predicate-analytics owning-class red as a stale route-counter
  expectation, not a production defect. Pinned and unpinned legacy sources both plan adjacency predicate-prefix runs,
  and the direct cursor emits exact counts `p1=3`, `p2=1`, `p3=1`; after the first prefix-run COUNT calibrates the
  store, adaptive dispatch intentionally serves the grouped query through its unmeasured IR-aggregate must-try arm.
  The strengthened selector and all 59 owning-class tests are green; evidence is under
  `lifecycle/legacy-base-predicate-analytics`.
- [x] (2026-08-23 21:37Z) Completed cooperative cancellation propagation. Bare, bulk, ordered, and Group roots share an
  evaluation token with kernels and derived worker rows; query cancellation has a terminal kernel signal distinct from
  replayable probe expiry; idle morsel workers stop at their next bounded queue poll; producer and worker loops suppress
  post-cancel output. The row, Group, kernel-poll, timeout, and 36-test parallel-aggregation owners are green. The
  broader IR-parallel owner retains exact answers but has a pre-existing adaptive route-engagement assertion red,
  recorded separately from cancellation correctness.
- [x] (2026-08-23 21:53Z) Added `NativeTermRef` as the authoritative RDF-term representation with an optional
  authority-scoped canonical LMDB id, and completed the first semantic row-floor slice. Projectionless legal tuple
  roots now execute as identity projections, while `Label`, `LocalName`, and `Namespace` BINDs remain native with
  exact RDF type/error behavior. The term-authority owner passes 9 tests, the computed-BIND owner passes four, and
  the focused no-host/no-island selector plus expression census are green; retained evidence is under
  `semantic-floor` and `expressions`.
- [x] (2026-08-23 22:13Z) Implemented evaluation/logical-solution BNODE identity and the first semantic BindingSet
  root stage. Labeled BNODEs are stable only within one logical solution, unlabeled calls are always fresh, separate
  evaluations cannot collide, and semantic `sameTerm` no longer requires canonical LMDB ids. The canonical parser
  stack `Projection -> aggregate Extension -> Group` now stays native when the extension is provably redundant; the
  three-test BNODE owner is green with no generic host or island. Red/green evidence is under `expressions`.
- [x] (2026-08-23 22:27Z) Added native BindingSet stages for Projection, MultiProjection, Slice, Distinct, Reduced, and
  variable Order. MultiProjection preserves projection-list order and its per-list adjacent duplicate rule; blocking
  Order remains demand-lazy; every stage delegates cooperative cancellation. `NativeRootPipeline` no longer rebuilds
  algebra over a `PrecompiledStub` or calls generic preparation. Its eight-test owner is green, with evidence under
  `tuple-roots`.
- [x] (2026-08-23 22:57Z) Replaced the remaining generic boolean bridge with total semantic value outcomes for
  computed BIND, FILTER, HAVING, comparison, IF, dynamic REGEX, and arbitrary Function SPI calls. Volatile expressions
  are prepared once per evaluation, and bound/unbound/error outcomes flow through one native EBV adapter. Focused
  expression owners are green with retained evidence under `expressions`.
- [x] (2026-08-23 23:09Z) Added a streaming semantic-native `TupleFunctionPlan`. Registry lookup occurs at preparation,
  arguments evaluate once left-to-right per input, RDF-term compatibility enforces constants/prebindings/repeated
  variables, and LIMIT, cancellation, arity errors, and SPI failures close the cursor. Its 16-test owner and algebra
  census are green with evidence under `tuple-functions`.
- [x] (2026-08-23 23:19Z) Promoted standalone `ReifiedTripleRef` and `AnnotationTripleRef` to the native triple-term
  scan without exposing `reifVar`. A composite explicit/inferred source now scans the shared value dictionary exactly
  once rather than duplicating global triple terms. The identical differential selector is green with retained
  red/green evidence under `triple-terms`.
- [x] (2026-08-23 23:27Z) Removed the hosted graph-variable property-path route. The general path plan now evaluates an
  independent BFS per named graph, carries graph identity through the frontier and result tuple, honors dataset and
  prebound-graph constraints, and cannot cross graph boundaries. The original selector and 12-test path owner are
  green with retained red/green evidence under `paths`.
- [x] (2026-08-23 23:33Z) Promoted legacy `CompareAny`, `CompareAll`, and `In` expressions from the standard value step
  to a correlated semantic-native scalar-subquery plan. It evaluates the left operand once, supplies the current
  solution mapping to a native tuple step, short-circuits ANY/ALL/IN, and closes on every terminal path. The focused
  selector is green with three observed native evaluator calls and evidence under `expressions`.
- [x] (2026-08-23 23:46Z) Added a semantic-native `NativeDescribeStep`. It compiles the DESCRIBE source natively,
  performs the same outgoing/incoming symmetric-CBD traversal with blank-node breadth-first expansion, honors default
  dataset contexts and parent-binding conflicts, and owns native cursor cancellation/closure. The identical focused
  selector and all 17 root-pipeline tests are green with evidence under `tuple-roots`.
- [x] (2026-08-24 19:34Z) Completed the semantic-native floor and authoritative-term boundary. Constants, VALUES,
  source-independent rows, entry bindings, unknown/noncanonical RDF values, and 60-slot plans retain semantic native
  execution; width 61 reports only `CAPACITY_MAX_NATIVE_SLOTS`.
- [x] (2026-08-24 19:34Z) Completed LMDB source decorators, lexical OPTIONAL/LATERAL/scalar-subquery scope frames,
  native root and tuple operators, total value outcomes, aggregate argument forms, custom aggregates, dynamic
  separators, empty groups, and native Function SPI invocation. Focused differential owners and the generated-route
  corpus are green.
- [x] (2026-08-24 19:34Z) Completed tuple functions, triple/reification/annotation terms, graph-aware path families,
  fragment result kinds, interpreter/emitter parity, typed compile outcomes, and specialist-tier engagement. The
  algebra census, expression census, generated-route coverage, kernel-decline census, island posture, 60/61 capacity
  boundary, deterministic fuzz, parser/model corpus, and three-tier engagement ledgers are green.
- [x] (2026-08-25 08:59Z) Passed the final route census, differential fuzzing, three-arm compliance matrix, matched
  performance acceptance, JFR/HotSpot evidence, and a fresh full LMDB module run. The final module log contains 4,119
  tests with zero failures/errors; all 143 theme/query kernel-census pairs report zero capability declines.

## Surprises & Discoveries

- Observation: the checkout already contains a 548 KB untracked `maven-build.log` and many unrelated evidence
  artifacts.
  Evidence: the baseline install appended to the existing log rather than truncating it; tracked status still reports
  only the pre-existing `ThemeQueryBenchmark.java` edit.
- Observation: the root quick install is green but this is not LMDB test evidence.
  Evidence: Maven reported `RDF4J: LmdbStore SUCCESS [ 10.844 s]` and `BUILD SUCCESS` in 39.410 seconds with tests
  skipped by the quick profile.
- Observation: census labels currently mean that at least one planner channel exists, not that every legal shape takes
  a native route. Dynamic route coverage is therefore a mandatory completion gate.
  Evidence: the current planner can still create `GenericEvalPlan` for scope changes, unsupported expression shapes,
  source wrappers, and unusual root stacks even when their census class is `NATIVE`.
- Observation: graph-variable paths returned exact answers only because the compiler opened three generic islands.
  Evidence: the focused red observed island delta three for a compound path whose second graph contained only half of
  the sequence. The native general path state now includes graph identity, and focused plus dataset/prebound variants
  pass without islands.
- Observation: the row-root planner rejected every projectionless legal tuple expression before considering its
  native tuple plan, even when the expression itself was fully supported.
  Evidence: the strengthened `Label`/`LocalName`/`Namespace` selector observed one hosted generic root after scalar
  compilation was added. Treating a missing Projection as an identity projection over declared bindings made the
  identical selector green without a host or island, and preserved all four computed-BIND owner cases.
- Observation: aggregate required-name analysis drops both BIND inputs and all N-ary custom aggregate arguments.
  Evidence: focused native tests returned COUNT 0 instead of 2 and weighted sum 0 instead of 5; retained reports are
  under `.agent/evidence/lmdb-native-soundness-completion/aggregate-liveness`.
- Observation: the claim-suppressed generic sibling silently resets the configured query evaluation mode.
  Evidence: its direct configuration test observed `[STRICT, STRICT]` instead of `[STANDARD, STANDARD]`, and a forced
  generic root reversed STANDARD calendar ordering; both reports are persisted under the optimizer evidence tree.
- Observation: boolean null-rejection recursion cannot soundly pass its proof unchanged through SPARQL `NOT`.
  Evidence: with the OPTIONAL binding absent, `BOUND` has EBV `FALSE` but `!BOUND` has EBV `TRUE`; the old proof
  rewrote that filter to an inner join. The focused red/green and 19-test class green are retained under
  `optimizer-reds/negated-bound-optional`.
- Observation: RDF term inequality is not a mutual-exclusion proof for SPARQL value equality.
  Evidence: `"1"^^xsd:integer` and `"1.0"^^xsd:decimal` are distinct terms but compare equal. `EqualityDomainProof`
  now retains exact `sameTerm` exclusion and only proves `=` exclusion for resource, boolean, string, and
  language/directional-string domains; the 19-test overlap class is green.
- Observation: the optimizer discarded the active `Dataset` and substituted estimated output cardinality for a
  context uniqueness proof.
  Evidence: estimates claiming one output removed contextless OPTIONAL and EXISTS, while genuinely structural
  single-default and single-named dataset cases were not recognized. All four focused selectors and the 47-test
  owning class are green under `optimizer-fixes`.
- Observation: `BindingSetAssignment` currently conflates possible and assured names.
  Evidence: a three-row assignment reported `[shared, firstOnly, secondOnly]` as assured rather than only `[shared]`;
  the focused report is persisted under the model-contract evidence tree. A single repeatable snapshot now computes
  declared/possible and all-row/assured names separately, and audited collision/layout consumers use possible names
  while readiness/must-bind consumers use assured names.
- Observation: standalone `ReifiedTripleRef` traversal metadata was incorrectly exposed as a produced binding.
  Evidence: model and evaluator companion tests show that the standalone node produces no `reifVar`; parser-generated
  `rdf:reifies` statement patterns remain responsible for that binding and preserve context multiplicity.
- Observation: RDF4J's standard evaluator scopes labeled `BNODE` identity to one solution mapping, while separate
  expression nodes and separate solutions receive distinct blank nodes.
  Evidence: the oracle self-test first observed distinct `stable_1_0` and `stable_2_1` identities; the retained green
  invariant now checks identity reuse through a copied binding plus distinct labeled and unlabeled invocations per
  solution, without comparing generated labels across arms.
- Observation: a direct aggregate SELECT is not presented to the compiler as a Group root.
  Evidence: the parser emits `Projection -> Extension -> Group`; the row compiler consequently created two generic
  islands per attempt even though both the BNODE input expression and COUNT(DISTINCT) aggregate were native. A proof
  that every Extension element exactly matches a named Group element now removes only that redundant wrapper, and a
  cancellation-delegating native BindingSet projection stage preserves parent-binding and duplicate-target semantics.
- Observation: `NativeRootPipeline` was classified as native while every peeled solution modifier was recompiled by
  the generic evaluator over a private `PrecompiledStub`.
  Evidence: the focused structural red found that stub as a declared pipeline class. Native stages now execute wrapper
  stacks in literal algebra order, and the complete owner retains exact generic-equal answers for repeated slices,
  ordering, Distinct, nested roots, ASK, and MultiProjection without the stub or generic preparation seam.
- Observation: the row-kernel parity class is not currently a clean broad gate even though the new overflow selector
  passes in both compiled and interpreted tiers.
  Evidence: its class run passed seven of nine methods but two unrelated route-engagement assertions observed zero
  interpreter openings; those exact methods must be isolated before this is classified as a regression or baseline
  failure.
- Observation: native Group prepared volatile interior filters and the authoritative HAVING filter against distinct
  `EvaluationScopedQueryEvaluationContext` instances.
  Evidence: the focused red recorded two NOW literals across three interior and three HAVING calls; routing HAVING
  through the group's `NativeExecutionContext` made the identical selector pass with all six calls sharing one value.
- Observation: the historical payload-free hash result of 2,060 native rows versus 8,400 expected rows is not present
  on the pinned tree after fixture strengthening.
  Evidence: the untouched default route first returned the exact four-arm 8,400-row multiset; an independently forced
  semantic-hash phase then built 1,000 payload-free rows over 500 degree-two keys, probed exactly 4,200 rows, and
  returned the same multiset. The focused result is retained as `hash-join-chain-stats/unreproduced.txt` and production
  was left unchanged.
- Observation: deterministic algebra/promotion fuzzing found no further evaluator divergence, but the SPARQL 1.2
  parser/model corpus fails before evaluation on nested `TRIPLE` component expressions, quoted-triple constructor
  syntax in value-expression position, and nested `STRLANGDIR` arguments.
  Evidence: the two fuzz classes pass 26/0/0 and 5/0/0; `LmdbNativeSparql12DifferentialTest` reports one failure and
  three errors, with the first minimized red retained as `corpora/sparql12-triple-functions-red.txt`.
- Observation: the existing parser helper for `TRIPLE(...)` lifted only unary component expressions and threw for a
  legal `TripleComponent` expression.
  Evidence: `TestSparqlTripleTermParser#testTripleFunctionAcceptsNestedComponentExpressionsLeftToRight` and the LMDB
  corpus selector failed before evaluation, then passed after arbitrary value expressions were lifted left-to-right
  without rebuilding the graph pattern repeatedly. Red/green reports are under `corpora`.
- Observation: the triple-term cross-pattern corpus fixture does not currently contain one RDF triple term reachable
  from both subjects; its numeric components are value-equal but RDF-term-distinct.
  Evidence: all generic oracle arms initially returned the same empty result. After the shared term was added, the
  identity and component joins each returned exactly two rows through MemoryStore standard evaluation, LMDB standard
  factory evaluation, LMDB optimizer/native-disabled evaluation, and production LMDB. This was a test-fixture gap,
  retained as `corpora/sparql12-triple-term-join-four-arm-green.txt`, and required no production change.
- Observation: directional-language parser visitors imposed a Var-only restriction that the SPARQL expression grammar
  does not impose.
  Evidence: focused parser and LMDB reds rejected nested `STR(...)` through `mapValueExprToVar`; retaining each child
  as its original `ValueExpr` made the identical selectors and the 18-test `TupleExprBuilderTest` class green.
- Observation: the generated parser grammar omitted quoted triple constructors from general `PrimaryExpression`
  positions even though SPARQL 1.2 permits them in BIND and comparison expressions.
  Evidence: focused parser and LMDB reds failed before evaluation. Adding the existing labeled-triple-term production
  to `PrimaryExpression`, regenerating and recording the optimizer patch deterministically, made the exact selectors
  and 28-test `TestSparqlTripleTermParser` class green.
- Observation: the process-wide `TimeLimitIteration` timer can be monopolized by synchronous delegate close, so one
  blocked timeout prevents later timeout tasks from running.
  Evidence: the cooperative signal, wrapper-delegation, global-timer, post-`next`, and timer-failure-survival reds now
  pass; the non-cooperative synchronous-close selector remains green. Reports are retained under
  `cancellation/time-limit` and `cancellation/wrappers`.
- Observation: native Group has an in-flight close flag but its sequential aggregation loop does not observe it at the
  next row poll.
  Evidence: `LmdbNativeGroupCancellationTest#closeStopsInFlightAggregationAtNextPoll` originally advanced the
  controlled cursor after close. An outcome-only evaluation token now stops after the in-flight call returns, accepts
  repeated requests while open, and declines after close; both exact selectors and the two-test class pass. The
  existing isolated kernel cancellation poll test is already green and remains recorded as unreproduced.
- Observation: making ordered evaluation genuinely lazy also defers ordered execution telemetry until first demand,
  and three older unordered row fixtures passed a null source despite adaptive arbitration requiring source identity.
  Evidence: all four owning-class failures reproduced independently; demanding the zero-limit iteration and supplying
  `RecordingNativeSource` to only the three invalid fixtures made the identical selectors and the 27-test class green.
  Red/green fixture reports are retained under `lifecycle/ordered-laziness`.
- Observation: `RUN_ROWS_COUNTED` is a physical-route counter and cannot be added across consecutive adaptive queries
  as if one prefix strategy were guaranteed to win both.
  Evidence: the legacy source advertises and executes the exact five-row predicate-prefix cursor, but after a
  fixed-predicate prefix COUNT records three rows, the adaptive model's documented must-try policy opens the previously
  unmeasured IR aggregate for the grouped predicate query. The semantic answers remain `p1=3`, `p2=1`, `p3=1`, the IR
  route reports three result groups, and no additional prefix rows are counted. Focused red/green and 59-test class
  evidence is retained under `lifecycle/legacy-base-predicate-analytics`.
- Observation: the aggregate MIN admission paths treated the native unbound marker as a real dictionary identifier.
  Evidence: the computed-expression selector admitted `NULL_CONTEXT_ID`/unknown values before MIN comparison. All
  aggregate admission paths now ignore both markers, and the identical selector plus promotion fuzz are green with
  retained evidence under `aggregate`.
- Observation: a witness-local BIND failure is represented by an occupied `NULL_CONTEXT_ID` slot, but correlated
  EXISTS substitution must treat that marker as absent rather than as an RDF identifier.
  Evidence: the strengthened witness-correlation red dropped the row whose OPTIONAL BIND failed. Scalar and prepared
  EXISTS probes now bypass id shortcuts for marker-bearing rows and run exact direct evaluation with the marker
  normalized to unknown; the EXISTS/NOT EXISTS owner and full kernel-decline census are green.
- Observation: aggregate witness filters use mask `-1` as a lexical-position sentinel, not as a literal native slot
  mask.
  Evidence: the strengthened four-slot lowering red failed until the kernel used the compiled predicate's exact
  `batchReadMask()` while preserving the sentinel's lexical placement. The identical selector and all eight
  kernel-decline census tests are green.

## Decision Log

- Decision: fix soundness before expanding default native coverage.
  Rationale: expanding an incorrect semantic surface makes failures harder to localize and increases user impact.
  Date/Author: 2026-08-23 / Codex.
- Decision: retain optimizer rewrites only behind structural or semantic proof objects; remove only the unsound proof
  branch.
  Rationale: the correct cases remain valuable, but estimates and approximate metadata are not semantic evidence.
  Date/Author: 2026-08-23 / Codex.
- Decision: add no optimizer safe-mode property and no master completion property.
  Rationale: soundness fixes must be unconditional. The existing global native-engine switch is sufficient for an
  emergency evaluator rollback.
  Date/Author: 2026-08-23 / Codex.
- Decision: keep the 60-slot primitive-mask architecture and classify wider plans explicitly as capacity fallback.
  Rationale: the requested boundary includes every semantic shape, but explicitly excludes a wide-row architecture
  rewrite.
  Date/Author: 2026-08-23 / Codex.
- Decision: the interpreted native row/group plan is the semantic authority; specialized, parallel, IR, and Janino
  paths are derived implementations.
  Rationale: correctness must not depend on code generation, and stateful or effectful operations must not be replayed.
  Date/Author: 2026-08-23 / Codex.
- Decision: characterize adaptive routing with source-capability and actual-winning-route assertions rather than
  assuming that a global counter belongs to a particular query in a multi-query test.
  Rationale: must-try exploration is intentional and semantic parity is preserved; forcing one physical winner in a
  fixture would hide the route that production actually executes.
  Date/Author: 2026-08-23 / Codex.
- Decision: cooperative cancellation is an accepted/not-accepted outcome, not a native timeout exception.
  Rationale: the evaluation thread must unwind its own cursors and leases while `TimeLimitIteration` remains the sole
  owner of timeout exceptions; unsupported or declined targets retain synchronous close, and timer-internal request
  failures fall back to close without killing the process-wide timer.
  Date/Author: 2026-08-23 / Codex.

## Outcomes & Retrospective

The soundness ledger and semantic-completion milestones are implemented. Every reproduced defect has persisted
focused red/green evidence, while strengthened cases that did not reproduce caused no speculative production change.
The semantic native row/group tier now owns legal algebra and expression execution up to 60 live slots; specialized,
parallel, interpreter, and Janino tiers decline to that semantic tier rather than creating generic islands. Dynamic
closure gates allow generic evaluation only for `Intersection`, `Service`, and the explicit capacity outcome.

Correctness gates are green: the algebra and expression censuses, generated-route corpus, kernel-decline census,
island posture, 60/61 capacity boundary, deterministic differential fuzzing, parser/model composition corpus,
specialist engagement ledgers, all three isolated 504-test compliance arms, and the final 4,119-test LMDB module run.
The final kernel census executed 143 theme/query pairs with zero capability declines. Matched JMH acceptance has no
cell with both a regression greater than five percent and disjoint confidence intervals; retained JFR identifies the
optimized aggregate value-cache path, and HotSpot evidence records its C2 compilation and hot inlining. Two broad-only
transient failures were green at identical method/class scope and did not recur in the final full run, so they caused
no speculative code or assertion change.

## Context and Orientation

The main module is `core/sail/lmdb`. `LmdbNativeEvaluationStrategy` chooses between native and generic compilation.
`LmdbNativeAggregateCompiler` and `LmdbNativeAggregatePlanner` construct native row/group plans. `SlotPlan` and its
implementations operate on compact row slots; a slot is one variable position represented by an LMDB value identifier.
`GenericEvalPlan` is an island that converts a native row to a generic RDF4J `BindingSet`, invokes the standard
evaluator, and converts results back. Such islands are the semantic escape hatch that this plan removes except for the
two allowed algebra nodes and the width limit.

The optimizer pipeline lives primarily in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`. It performs
LMDB-specific join ordering, filter placement, OPTIONAL rewrites, and cardinality-driven physical planning. A rewrite
is sound only when it preserves the SPARQL multiset of solution mappings, including duplicate counts, errors, unbound
variables, query evaluation mode, datasets, and named-graph multiplicity.

The query-algebra contracts live in `core/queryalgebra/model`. Iteration timeout and wrapper behavior spans
`core/common/iterator`, `core/sail/base`, and `core/repository/sail`. These cross-module fixes must be installed to the
workspace-local Maven repository before downstream LMDB tests run; the `mvnf` helper performs that install.

The independent oracle is test-only. It loads the same statements into MemoryStore and LMDB and evaluates through four
arms: MemoryStore standard evaluation, LMDB with `DefaultEvaluationStrategyFactory`, LMDB with native evaluation
disabled but the LMDB optimizer retained, and production LMDB. Unordered answers are compared as multisets; ordered
answers remain sequences. RDF terms are compared recursively with datatype, language, base direction, and triple-term
components. Blank nodes and volatile functions are checked through identity and invocation invariants rather than
unstable lexical labels.

## Plan of Work

### Milestone 0: Oracle and evidence foundation

Add a test-only `IndependentSparqlOracle` under `core/sail/lmdb/src/test/java/.../evaluation`. Give it explicit builders
for the four arms, one data loader, result canonicalization, exception-category comparison, and a route assertion. Add
the existing MemoryStore module as a test dependency if it is not already available. Do not add a production optimizer
property. Add evidence directories under `.agent/evidence/lmdb-native-soundness-completion` only as each focused red is
observed.

For every later soundness item, first add the smallest named test and run the exact method. Persist its Surefire report
and retained Maven log before editing production. If the test unexpectedly passes, strengthen the fixture while leaving
production untouched. If the intended counterexample remains unreproducible, record that outcome here and do not make a
speculative fix.

### Milestone 1: Soundness ledger

Fix `genericOnlyView` configuration propagation first so generic comparison paths preserve `QueryEvaluationMode`.
Then address the optimizer proofs. Replace OPTIONAL null-rejection's boolean recursion with an abstract SPARQL EBV
outcome set containing possible `TRUE`, `FALSE`, and `ERROR` results when right-side variables are unbound. Replace
OPTIONAL overlap's RDF `Value.equals` shortcut with an explicit equality-domain proof. `sameTerm` constants are exact;
SPARQL `=` is accepted only for disjoint comparison families or transitive classes that the core evaluator defines
exactly. Remove estimate-as-uniqueness entirely and retain only fixed/assured context SPOC proofs.

Correct `BindingSetAssignment`: possible names are the declared/union names and assured names are the intersection of
names bound in every row. Compute both from a repeatable snapshot and audit all model/evaluation/LMDB consumers.
Layout and collision checks use possible names; filter readiness and missing-variable proofs use assured names.

Fix aggregate liveness by walking Extension elements backwards from every live output and collecting every argument of
custom aggregates. Make group interior and HAVING share one evaluation-scoped context. Canonicalize duplicate projection
targets according to generic projection behavior. Centralize saturating limit/offset arithmetic for row, interpreter,
emitter, and parallel paths.

Reproduce the documented payload-free hash case end to end: 4,200 probe statements joined to 500 build keys duplicated
in two named graphs must emit 8,400 rows. Assert each boundary—raw source, chosen build/probe, hash build counts, probe
distribution, counted-chain output, and row adapter—and fix the earliest divergence rather than guessing.

Make ordered native evaluation lazy so sorting starts on first demand after timeout wrapping. Introduce an internal
`CooperativeCancellation` contract that wrapper iterations delegate. Native evaluations own one cancellation token;
all row/group/kernel/probe/parallel loops poll it. A timeout signals the token and returns from the timer thread, while
the evaluation thread unwinds native cursors and read leases. Keep query cancellation distinct from a bounded native
probe decline.

Make dataset construction exception-safe and protect every planner-statistics read with the native-source read lock.
Use latch-driven tests and bounded futures, never sleeps. Finally align `ReifiedTripleRef` binding-name metadata with the
existing evaluator: its `reifVar` remains traversable metadata but is bound by the parser-generated `rdf:reifies`
statement pattern, not by the standalone triple-term scan.

After the named ledger is green, run deterministic differential fuzzing. Every new mismatch becomes a focused
reproduce-first slice and is added to this milestone before completion work continues.

### Milestone 2: Semantic native substrate and sources

Replace nullable compilation with a `CompileResult` whose outcomes are `NATIVE_FAST`, `NATIVE_GENERAL`,
`ALLOWED_INTERSECTION`, `ALLOWED_SERVICE`, and `CAPACITY_MAX_NATIVE_SLOTS`. A kernel-specialization failure returns to
the semantic native row plan, not to generic evaluation. Once a native cursor emits a row, runtime errors propagate and
never trigger a generic restart.

Add `NativeTermRef`, which contains an optional canonical LMDB identifier and the authoritative RDF `Value`. The fast
path compares ids only when both refs share a canonical id space; all other compatibility uses RDF term equality. Add a
logical solution identity, lexical `NativeScopeFrame`, exact-once/effect metadata, and a general native row cursor below
the optimized slot tier. Use these types to make VALUES, missing-store constants, noncanonical language/direction forms,
base bindings, SingletonSet, EmptySet, BIND, and ASK native on empty and non-empty sources.

Build semantic native-source decorators for transaction changesets, removals and clears, serializable observation,
explicit/inferred composites, legacy id authority, and triple-term overlays. Preserve read-your-writes, statement
multiplicity, access notifications, open/next/close failure suppression, cancellation, and cursor ownership.

### Milestone 3: Total scopes, tuple/root operators, expressions, and aggregates

Use `NativeScopeFrame` to implement nested Projection, MultiProjection, Group, Order, Distinct, Reduced, Slice, ASK,
Describe, VALUES, Extension, Filter, UNION, MINUS, EXISTS/NOT EXISTS, subqueries, arbitrary root stacks, and lexical
LeftJoin semantics. LATERAL receives only `rightInputBindingNames`; hidden left bindings are masked and restored, and the
plan is a non-reorderable correlation barrier. Retire generic root-modifier rebuilding.

Add a total native value evaluator with outcomes bound value, unbound, and error. Cover every legal Compare, IF error
branch, dynamic REGEX, truth/type form, and Function SPI call. External calls evaluate arguments left-to-right exactly
once and use the original TripleSource. Implement BNODE with evaluation and logical-solution identity. Implement legacy
Label, LocalName, Namespace, CompareAny, CompareAll, and In with a closing correlated scalar-subquery plan.

Create a total `NativeAggregateSpec` for all built-ins, direct and bound constants, unknown ids, DISTINCT and star,
dynamic parent-scoped GROUP_CONCAT separators, empty groups, and unary/n-ary custom aggregates. General native Group is
the semantic floor at root and nested positions; specialized aggregate tiers may only decline to it.

### Milestone 4: Tuple functions, triple terms, paths, and specialization

Implement `TupleFunctionPlan` as a per-input streaming native plan. Resolve the registry at preparation, evaluate
arguments once in order, verify tuple arity and compatible constant/prebound variables, bind unbound outputs, and close
on LIMIT, cancellation, errors, and SPI failures.

Handle TripleRef, ReifiedTripleRef, and AnnotationTripleRef explicitly with accurate produced masks and repeated-slot
checks. Composite triple-term scans delegate once to the shared dictionary source. Add a direct `ScanTripleTerms` IR
node to both interpreter and Janino emitter when replay-safe.

Complete zero-length, zero-or-one, arbitrary-length, inverse, alternative, sequence, and negated paths. Bind graph
variables exactly like the generic evaluator, support same endpoint names through hidden positional slots, preserve
dataset/context behavior and BFS deduplication, and support model-only minimum lengths.

Finish the fragment result kinds PREDICATE, VALUE, SORT_KEY, AGG_UPDATE, and ACCESS. Every pure replay-safe fragment up
to 60 slots has interpreter/emitter differential tests. Stateful streaming, volatile, and external-SPI work stays on
the semantic row tier with an explicit explain reason.

### Milestone 5: Closure and default-on proof

Create an engagement ledger mapping every concrete tuple and value-expression census entry to an executed differential
case. The parser corpus covers SPARQL 1.0, 1.1, and 1.2 shapes; hand-built algebra covers model-only classes. Compose
each legal node below JOIN, OPTIONAL, UNION, MINUS, subquery, EXISTS/NOT EXISTS, LATERAL, GRAPH, entry bindings, and
nested scopes. With defaults enabled, dynamic telemetry permits generic routes only for Intersection, Service, and
CAPACITY_MAX_NATIVE_SLOTS. A boundary test proves 60 live slots stay native and 61 decline only for capacity.

Run the five LMDB compliance suites in isolated production, native-disabled, and standard-factory arms. Run the full
LMDB module last. After correctness, benchmark cold compile/first execute and warm cached execution, cache/fallback
behavior, generated source size, and representative OPTIONAL, aggregate, and ordered queries. Collect JFR and HotSpot
compilation evidence before making a performance claim. A greater-than-five-percent default-path regression with
disjoint confidence intervals blocks default enablement, but no soundness fix is reverted for speed.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`. The mandatory baseline command is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

The preferred focused test form is:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#methodName \
      --module <module> --retain-logs

Never use `-am` or `-q` for a test. After each focused red, use `scripts/agent-evidence.py` and the retained log to
persist a compact report under `.agent/evidence/lmdb-native-soundness-completion/<issue>/red.txt`; preserve and append
to the existing root `initial-evidence.txt`. Re-run the identical selector after the fix and persist `green.txt`, then
run its class and module.

The recurring LMDB closure gates are:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAlgebraCensusTest \
      --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeExpressionCensusTest \
      --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeGeneratedQueryCoverageTest \
      --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest \
      --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Before finalizing Java changes, run `scripts/checkCopyrightPresent.sh` from `scripts`, then run the repository formatter
with the workspace-local Maven repository. Formatting is not a test and may use the repository-prescribed `-q` form.

## Validation and Acceptance

Each soundness test must visibly fail before its production fix and pass after it. The independent oracle must show the
same multiset or ordered sequence in all applicable arms, including duplicate counts and exact binding names. Lifecycle
tests use workers, latches, bounded `Future.get`, and unconditional latch release in `finally`; no test uses sleep as a
synchronization mechanism.

Completion is accepted only when all focused soundness evidence is green, deterministic fuzzing reports no divergence,
the dynamic route census reports no forbidden generic path, the five query/update compliance suites have no new query
failures in production, and `core/sail/lmdb` is genuinely green. An update failure is separately classified unless a
shared model change caused it. A hang, OOM, aborted command, missing provider, or overwritten/stale report is not green.

Performance is reported only from matched benchmark inputs and JVM settings. Report baseline/candidate score,
allocation evidence or `unknown`, JIT tier/inlining evidence or the missing prerequisite, cold Janino compile cost, warm
cache behavior, fallback exercise, and confidence.

## Idempotence and Recovery

All tests and builds are repeatable. Never delete or rename the user-owned benchmark edit or untracked evidence. If an
offline build misses a dependency, rerun the identical command once without `-o`, then return offline. If a focused test
hangs, inspect process/thread state and native leases; do not use a same-thread timeout that cannot unwind the close
loop. If a proposed reproducer remains green, document it and leave production unchanged. If a milestone introduces a
new mismatch, return to a focused red before continuing rather than weakening an assertion or routing the case generic.

## Artifacts and Notes

The baseline quick-install transcript is appended to `maven-build.log`. Per-issue red and green evidence belongs under
`.agent/evidence/lmdb-native-soundness-completion`. The pre-existing `initial-evidence.txt` must be appended to, never
truncated. The tracked benchmark edit at
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java` is outside this work.

## Interfaces and Dependencies

`BindingSetAssignment.getBindingNames()` returns possible names; `getAssuredBindingNames()` returns names bound in every
row. `ReifiedTripleRef` no longer advertises `reifVar` as an output binding. The internal common-iterator
`CooperativeCancellation` interface reports whether nonblocking cancellation was accepted; wrappers delegate it,
`TimeLimitIteration` falls back to synchronous close only when unsupported or declined. Native row and Group roots,
generated/interpreted kernels, exchange queues, and worker-confined row states share one outcome-only evaluation token;
kernel query cancellation is distinct from the arbiter's replayable probe-deadline signal.

Within LMDB, define typed compile outcomes, `NativeTermRef`, `NativeScopeFrame`, logical solution identity, effect
metadata, and total semantic native plan/value/aggregate interfaces. These remain internal experimental APIs. Add no
new external library: MemoryStore is an existing reactor module used only in tests. Preserve existing native feature
switches and the global `rdf4j.lmdb.nativeQueryEngine.enabled`; soundness fixes have no switch.

Revision note (2026-08-23 19:11Z): recorded the first two focused aggregate-liveness reds and their persisted evidence;
no aggregate production code had been changed yet.

Revision note (2026-08-23 19:28Z): closed the first root-fix cycle. Aggregate required-name analysis now includes every
custom-aggregate argument and transitively closes live Extension targets over their source expressions. Both exact
selectors and both owning classes are green; red/green evidence is persisted under `aggregate-liveness`.

Revision note (2026-08-23 20:59Z): closed the common timeout/wrapper gates and the first native Group cancellation
poll-point cycle. Exact red/green and owning-class evidence is persisted under `cancellation`; broader native token
propagation remains explicit follow-up work.

Revision note (2026-08-23 21:37Z): completed native-root, kernel, exchange, and worker cancellation propagation. The
idle-morsel reproducer timed out before the fix and now exits in 75 ms; row, Group, kernel, timeout, and parallel
aggregation owners are green. A separate IR-parallel route-counter fixture is red despite exact result parity and is
tracked as route-closure work rather than cancellation evidence.
