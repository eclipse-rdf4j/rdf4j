# OPTIONAL with a condition under the semantic projection boundary: admit it through the sealed projection (2026-11-17)

Status: merged into the working tree 2026-11-17 (uncommitted; see "Prototype hand-back" and "Merge"). Routine A (behaviour change: which route executes a SELECT
whose body has `OPTIONAL { … FILTER(cond) }`). Second admission slice after property paths
(`GH-0000-native-path-under-sealed-projection-2026-11-16.md`); owner decision "extend native first" (ExecPlan
Decision Log, 2026-11-17).

## Observation

`SELECT ?s ?v ?w { ?s :p ?v OPTIONAL { ?s :q ?w FILTER(?w > ?v) } }` on 100,000 subjects: 1,490 ms with the
boundary, 313 ms with it disabled, identical rows
(`.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a3-cluster-cost/summary.tsv`). The
projected name `?w` is not guaranteed, so the boundary is taken (exit E3b); the sealed projection then refuses the
`LeftJoin` because it has a condition (`NativeSealedProjection.plainBody`, the `optional.hasCondition()` check),
and the whole OPTIONAL runs in the ordinary evaluator over native single-pattern leaves.

Facts established by probe (`…/triage-families/a3-optional-condition-facts/`):

- Let through, `compileRowRoot` returns a `NativeRowsStep` whose tree is a plain
  `LeftJoinPlan(left, FilterPlan(right))` with all `lexical*` fields null; the first physical refusal is
  `PhysicalProof.create`, which does not allow a `FilterPlan` node.
- With condition and `FilterPlan` admitted crudely, query 04 runs in 342–456 ms and an 18-query differential has no
  bag mismatch; row origin facts on query 04 equal the semantic route's (tracked, no requests, locals = the bound
  outputs: `[s,v,w]` when the right side matched and the condition held, `[s,v]` for no match, condition false and
  condition error).
- A condition over right-side names only, and an EXISTS, are relocated by the optimizer into the right arm as a
  `Filter` node: they are not LeftJoin conditions and are out of this slice. `FILTER(BOUND(?w))` is removed by the
  optimizer and already seals.
- Hazard found: a source without a native value codec (`NativeLmdbQuerySource.nativeValueCodec()` null, the
  composite-origin fixture's in-memory source) makes the filter compiler throw, and the exception escapes
  `tryCompile` (8 cases of `NativeCompositeOriginBoundaryTest` red under the crude probe).
- The `lexicalSharedSlots` / `lexicalCondition` / `lexicalProblemSlots` forms of `LeftJoinPlan` (subquery or
  non-replay-stable right side; non-well-designed OPTIONAL) were never exercised by the probe and stay refused.
  The 13 boundary-caused cases of `LmdbNativeLexicalOptionalConditionTest` need VALUES and a subselect on the right
  as well and are not reached by this slice.

Why native filter semantics can be trusted here: value-only filters over patterns already run natively outside the
boundary (`NativeBGPOrigin.of` admits them through `valueOnlyCondition`), with the same filter compiler.

## Required behaviour

Extend the sealed projection, and nothing else. Everything not named is refused as today, and a refusal is never
an exception.

1. `plainBody` admits a `LeftJoin` with a condition when all of the following hold.
   - The condition tree consists only of these exact classes: `Var`, `ValueConstant`, `Compare`, `SameTerm`, `And`,
     `Or`, `Not`, `Bound`, `Str`, `Lang`, `Datatype`, `IsURI`, `IsLiteral`, `IsBNode`, `IsNumeric`, `MathExpr`,
     `Regex`. No `Exists`, no `FunctionCall`, no aggregate, no subquery, nothing else.
   - Every `Var` is exact class `Var`; a non-constant one has no value and its name is in
     `left.possible ∪ right.possible`; a constant one has a value and its name is not observed.
   - The condition has no required input binding names (the existing observation-domain helpers decide; unknown
     domain means refusal).
   - The LeftJoin's Domain is computed exactly as for a LeftJoin without a condition today (assured = the left's
     assured names; the existing shared-name resource rule still applies). The condition adds no name.
2. `tryCompile` refuses (returns the semantic step) before compiling when the body proof contains a condition and
   `source.nativeValueCodec()` is null. No try/catch around `compileRowRoot`.
3. `PhysicalProof.create` admits a `FilterPlan` only as the direct right child of a `LeftJoinPlan`; its `possible`
   is its argument's. `completeRows` walks into it with the frame input the right arm gets today
   (`input | possible(left)`) and requires the filter's slot mask to be within
   `input ∪ possible(left) ∪ possible(right arm)`. A `FilterPlan` anywhere else, `MultiJoinPlan.filters`, a pattern
   key range, and any `LeftJoinPlan` with a non-null `lexicalSharedSlots`, `lexicalCondition` or (for a bound
   problem slot) `lexicalProblemSlots` stay refused exactly as today.
4. If the physical tree for an admitted logical body is anything else (the planner pushed the condition into a
   range, a bound constant, or chose a lexical form), the proof refuses; it never assumes.

## Invariant to prove before anything is merged

"For an admitted body on an empty untracked frame, the sealed rows equal the semantic projection's rows as a bag
and row by row in origin facts (tracked, requests, locals, parameters, graph frame)." Evidence:

- A new differential test, red first for the sealed-route assertions: native disabled against native enabled, bag
  and per-row origin facts, on data with integers, decimals, doubles, plain and language-tagged strings, IRIs and
  blank nodes as right-side objects, subjects with zero, one and several right matches. Admitted shapes:
  `?w > ?v`, `?w = ?v`, `?w != ?v`, `?v = <const>` (left names only), `?w > 5 && ?v < 9` when it stays a LeftJoin
  condition, `sameTerm(?v, ?w)`, `isNumeric(?w) && ?w + 1 > ?v`, `!isLiteral(?w) || ?w > ?v`,
  `str(?w) = str(?v)`, `lang(?w) = "en"`, `datatype(?w) = xsd:integer`, `regex(str(?w), "a")`, a type error in the
  comparison (string against integer), two chained conditioned OPTIONALs, projection of a subset of the names.
  Each of the four row kinds (matched and true, no right match, matched and false, matched and error) must occur
  and be asserted for origin facts.
- Refusal shapes, asserted as "not sealed, same bag, same origin facts": a condition variable produced by neither
  side; EXISTS in the condition; a function call; a right-side-only condition (relocated Filter); an OPTIONAL whose
  right side is a subselect; a non-well-designed OPTIONAL (right-only name bound by the outer frame); a query
  evaluated with a non-empty input binding; a source without a value codec (the composite-origin fixture, which
  must stay green).
- `NativeCompositeOriginBoundaryTest` cases [17]–[32], `NativeLexicalOptionalNullDomainTest`,
  `LmdbNativeLeftJoinWellDesignedTest`, `LmdbNativeFilteredOptionalEntryBindingTest`,
  `LmdbNativeFilteredOptionalAccelerationTest`, `OptionalNullRejectionContractTest`: no case green before and red
  after.
- One mutation per rule, each caught by a test: admit a condition variable outside the domain; drop the value-codec
  precondition; admit a `FilterPlan` outside a LeftJoin right arm; admit a lexical LeftJoin form; treat the
  condition's names as assured.

## Acceptance

- Query 04 and its constant-condition variant within 1.5× of the boundary-disabled medians.
- Whole-module per-case comparison with no stable green→red, on top of the path slice; merged by the owner only
  then.

## Not in this change

Right-side-only and EXISTS conditions (a Filter in the right arm); VALUES and subselects on the right (the 13
`LmdbNativeLexicalOptionalConditionTest` cases); the lexical LeftJoin forms; filters in a multi-join or as key
ranges; ORDER BY with LIMIT over a BIND.

## Prototype hand-back (2026-11-17; `.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a3-optional-condition-proto/`)

Not in the working tree. `slice2.diff` (on top of the path slice) changes only `NativeSealedProjection.java`.

- Results over 21 classes (617 existing cases) plus the new 50-case `NativeSealedOptionalConditionProjectionTest`:
  nothing green→red; no wrong bag and no origin-fact mismatch for the shapes that seal; the four row kinds occur
  with locals `[s,v,w]` (matched and true) and `[s,v]` (the other three). Query 04: 328 ms against 279–285 ms
  with the boundary disabled (1,374–1,720 ms before); the constant-condition variant 297–313 ms against 246–253.
- Corrections to the shape lists above. The optimizer relocates a right-side-only condition into the right arm
  as a `Filter` (`lang(?w) = "en"`, `datatype(?w) = xsd:integer`, `regex(str(?w), "a")`) and splits a
  conjunction with a right-side-only conjunct (`?w > 5 && ?v < 9`, `isNumeric(?w) && ?w + 1 > ?v`,
  `datatype(?w) = xsd:integer && ?w != ?v`); none of those is a LeftJoin condition and none seals. They stay
  LeftJoin conditions when a left name is mixed in with `||`. `regex(...)` is a `FunctionCall` in the algebra, so
  it is always refused; `Regex` is removed from the whitelist. A projection that omits the optional name takes
  no boundary at all.
- Physical refusal found: a right arm with two patterns makes the planner fold the condition into
  `MultiJoinPlan.filters`; the proof counts `LeftJoinPlan`-right `FilterPlan` nodes against logical conditions
  and refuses on a mismatch. Also sealed (reported, not asserted): a union or a path on the right, a
  multi-pattern left.
- Addition beyond the text above: `completeRows` also refuses a sticky filter mask.
- Mutations: dropping the value-codec precondition is caught (8 `NativeCompositeOriginBoundaryTest` cases).
  Admitting a variable outside the domain and treating condition names as assured are each shadowed by a
  second guard (the physical mask check; the physical assured check) and are caught only as paired mutants;
  cases for both pairs are in the test. Admitting a `FilterPlan` elsewhere and admitting a lexical LeftJoin form
  could not be reached from any of about 14 diagnosed query shapes; those two guards are defensive and have no
  query-level test.
- Found on the way, pre-existing and independent of this slice: for a relocated right-arm filter such as
  `OPTIONAL { ?s :q ?w FILTER(regex(str(?w), "a")) }` the native route returns the same bag but rows on which
  `?w` is bound do not carry `?w` as a local name, unlike the ordinary evaluator. Reproducer:
  `scratch-reproducer.diff` (three shapes, all red). To be diagnosed separately.

## Merge (2026-11-17; working tree, uncommitted)

- `slice2b.diff` (`.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a3-optional-condition-proto2/`):
  the prototype with `Regex` removed from the condition classes and the inline class name replaced by an import.
- Whole-module per-case comparison on top of the path slice: 1,543 of 1,544 LMDB test classes, 19,609 cases; 14
  stable red→green (the new test's sealed-route cases); no stable green→red. One case looked like a regression
  (`LmdbNativeKernelRowsPrimaryContractTest#actualLiveProbeDeadlineKeepsItsControlIdentityWhenCleanupSucceeds`,
  6 passes without against 3 failures with); a swap of the changed file between the two build copies showed it
  fails 4–5 times in 8 in all four combinations, so it is an unrelated race. The uncompared class,
  `LmdbPreparedBulkStoreTest`, exceeds the fork timeout in both states (a 2 MiB bulk-load memory budget makes its
  external sort degenerate; not a deadlock).
- Test first on the working tree: `NativeSealedOptionalConditionProjectionTest` 14 of 50 red (the sealed-route
  cases), green after. The 21 related classes afterwards show exactly the reds they had before (86, none new):
  `NativePureUnionVisibilityBoundaryTest` 28, `LmdbNativeLexicalOptionalConditionTest` 13,
  `NativeCompositeOriginBoundaryTest` 12, `LmdbNativeKernelRangeTest` 11, `NativeProjectionOrderTieBoundaryTest` 8,
  `NativeLexicalOptionalNullDomainTest` 6, `LmdbNativeLeftJoinFilterRewriteTest` 4, and four single cases.
