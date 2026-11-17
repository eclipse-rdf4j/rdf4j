# A computed BIND under the semantic projection boundary: admit it through the sealed projection (2026-11-18)

Status: merged into the working tree 2026-11-17 (uncommitted; see "Prototype hand-back" and "Merge"). Routine A (behaviour change: which route executes a SELECT whose
body has `BIND(<expression> AS ?x)`). Third admission slice after property paths and OPTIONAL with a condition;
owner decision "extend native first" (ExecPlan Decision Log, 2026-11-17).

## Observation

`SELECT ?s ?x { ?s :p ?v BIND(?v * 2 AS ?x) } ORDER BY ?x LIMIT 100` on 100,000 subjects: 154–168 ms with the
boundary, 20–21 ms with it disabled, same rows
(`.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a3-orderby-bind-facts/`).

- The algebra is `[Slice] → Order → Projection → Extension → StatementPattern`. `?x` is not a guaranteed output, so
  the boundary is taken (exit E3b). An alias `BIND(?v AS ?x)` takes no boundary and is unaffected.
- `NativeSealedProjection.plainBody` refuses the Extension because the element's expression is not a plain `Var`.
  The BIND is then computed by the ordinary Extension evaluator over native single-pattern leaves, and the
  boundary's own `TopK` stage sorts the resulting binding sets.
- With the boundary disabled the plan is one `NativeRows` with a native computed copy and a packed top-K
  (`orderSlots=[?x], limit=100`).
- Lifted crudely, three refusals are met in order: `plainBody` (non-`Var` element);
  `NativePrimitiveInputSemantics.supportsSealedTargets` (the proof reports no assignments, so the inputs are
  `NONE`); `PhysicalProof.create` / `completeRows` (an `ExtensionPlan` copy with `computedValue != null`). Order
  and Slice stay outside the sealed body. Bags and key order agreed in 16 shapes; the query ran in 70–93 ms.
- One provenance difference under the crude lift, always the same: on a row where the expression fails, the
  ordinary route's row has the target among its local names and the sealed row does not.

That difference is a rule, not an accident: every evaluator of an evaluated extension element marks the target as
locally produced whether or not a value was bound (`ExtensionIterator.buildLambdaToEvaluateTheExpressions`,
`NativeBindingSetExtensionStep` both call `producing(Set.of(name))` unconditionally). `RowProjection.project`
today derives local names from bound cells only, which is exact for patterns, paths and assured aliases and wrong
for a computed target.

## Scope of this slice

Admit the computed element into the sealed body. Do not move Order or Slice: they remain the boundary's stages
over sealed rows. Expected cost after this slice is therefore about the crude-lift figure, not the
boundary-disabled one; where the remaining time goes is measured as part of the prototype and decides whether an
order push-down slice follows.

## Required behaviour

Extend the sealed projection, and nothing else. Everything not named is refused as today, and a refusal is never
an exception.

1. `plainBody`, Extension branch. An element whose expression is an exact `Var` keeps today's alias rule
   unchanged. Any other element is admitted as *computed* when all of the following hold; otherwise null.
   - Exact class `ExtensionElem`, `TupleExprs.isEvaluatedExtensionElement`, non-null target name.
   - The expression tree consists only of the classes of `CONDITION_CLASSES` (the OPTIONAL-condition whitelist:
     `Var`, `ValueConstant`, `Compare`, `SameTerm`, `And`, `Or`, `Not`, `Bound`, `Str`, `Lang`, `Datatype`, `IsURI`,
     `IsLiteral`, `IsBNode`, `IsNumeric`, `MathExpr`), walked with the existing condition walker. No `If`, no
     `Coalesce`, no `FunctionCall`, no `Exists`, no aggregate.
   - Every non-constant `Var` in it is exact class `Var`, has no value, and its name is in the Extension's own
     argument's `possible` set or is an earlier target of the same Extension. A constant `Var` has a value and an
     unobserved name.
   - The target is not in `possible` so far. It becomes possible; it is **not** assured and **not** a resource.
     (An alias whose source is a computed target is therefore refused by the existing assured-source rule.)
   - The Extension is on the *mandatory spine* of the body: starting at the body root, the spine continues through
     an Extension to its argument, through a Join to both arguments, and through a LeftJoin to its left argument
     only. An Extension with a computed element anywhere else (a LeftJoin right arm, a Union arm, below any other
     node) is refused.
2. `BodyProof` records the computed target names. They are part of `targets` and make `hasAssignments()` true, so
   every existing assignment precondition of `tryCompile` applies unchanged: not `shared`, `plainObservers`, the
   real dictionary with canonical IRI encoding, `NativePrimitiveInputSemantics.prepare` certifying exactly the
   written target set, `preserveLexicalScopes`. In addition a body with a computed element requires
   `source.nativeValueCodec() != null`, checked before compiling like the condition rule.
3. `PhysicalProof.create` and `completeRows` admit an `ExtensionPlan` copy with a computed value when: no source
   slot, `assignment`, not `setNullOnError`, not `termChecked`, `encounterOrderReplaySafe`, `computed == null`,
   `semanticValue == null`, no constant, target not yet possible. The target becomes possible, not assured. The
   slots the computed value reads must be provably within the possible mask of the plan's argument (plus earlier
   targets of the same plan node); if the plan object does not expose what it reads, the proof refuses. The
   number and targets of computed copies must equal the logical proof's (the same recipe-count discipline as for
   aliases); any other physical form is refused.
4. `RowProjection.project`: an output whose name is a computed spine target is a local name on every row, bound
   or not. All other outputs keep the bound-cell rule.
5. Run-time proof obligation: the sealed branch must actually execute (`RowProjection.supportsSource` requires an
   empty synthetic catalog). If computed values that are not stored terms make the dispatch fall back to the
   semantic step per invocation, this slice has no effect and is stopped; that is to be established first.

## Invariant to prove before anything is merged

"For an admitted body on an empty untracked frame, the sealed rows equal the semantic projection's rows as a bag
and row by row in origin facts (tracked, requests, locals, parameters, graph frame)." Evidence:

- A new differential test, red first for the sealed-route assertions: native disabled against native enabled, bag,
  per-row origin facts, and for ordered queries the key-column sequence. Data: integers, decimals, doubles
  (including values whose product or sum changes numeric type), plain and language-tagged strings, IRIs and blank
  nodes, so that each expression fails on some rows and succeeds on others.
  Admitted shapes: `?v * 2`, `?v + 1`, `?v / 3`, `?v - ?u` over two patterns, `str(?v)`, `lang(?v)`,
  `datatype(?v)`, `?v > 5`, `isNumeric(?v)`, `!bound(?v) || ?v = ?u`, two BINDs in one group where the second
  reads the first, a BIND followed by another pattern on the spine, a BIND on the left of an OPTIONAL, each with
  and without `ORDER BY ?x`, `ORDER BY DESC(?x) LIMIT … OFFSET …`, `ORDER BY ?x ?s`, `DISTINCT`, and a projection
  that keeps both `?v` and `?x`. A failed-expression row and a successful row must both occur and be asserted
  for origin facts in each family.
- Refusals, asserted "not sealed, same bag, same origin facts": `IF`, `COALESCE`, a function call
  (`regex`, `concat`, `abs`), `EXISTS`; a BIND inside an OPTIONAL right arm; a BIND inside a UNION arm; a BIND
  reading a name from a sibling group; an alias of a computed target; a query evaluated with a non-empty input
  binding for the target or for a read name; a source without a value codec (the composite-origin fixture stays
  green); GRAPH and FROM datasets.
- No case green before and red after in: the four `NativeSealedProjection*Test` classes,
  `NativeSealedPathProjectionTest`, `NativeSealedOptionalConditionProjectionTest`,
  `NativeCompositeOriginBoundaryTest`, `LmdbNativePackedMorselsTest`, `LmdbNativePackedAlgebraTest`,
  `LmdbNativeQueryExplanationTest`, `GeneratedValueKeysIntegrationTest`, `LmdbNativeWildcardPredicateBatchTest`,
  `LmdbNativeLateralDifferentialTest`.
- One mutation per rule, each caught by a test: admit a computed element off the spine; treat the target as
  assured; drop the always-local rule; admit a class outside the whitelist; admit a read name outside the
  argument's domain; drop the value-codec precondition; admit a physical computed copy with `setNullOnError`.

## Acceptance

- The measured query: boundary on, prototype, boundary disabled (medians of five), and a breakdown of where the
  prototype's remaining time goes (projection per row, the boundary's top-K, the native body).
- The list of previously red cases that turn green, by name.
- Whole-module per-case comparison with no stable green→red (eight reruns per state for a candidate in an
  unstable class); merged by the owner only then.

## Not in this change

Order or Slice inside the sealed body; `IF`, `COALESCE` and function calls; computed elements off the mandatory
spine; aggregates; BIND under GRAPH.

## Prototype hand-back (2026-11-17; `.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a3-computed-bind-proto/`)

Not in the working tree. `computedbind.diff` (sha256 d9d91aaa…) changes only `NativeSealedProjection.java`.

- Run-time proof (item 5): for the measured query the sealed dispatch is taken once per evaluation and the
  semantic fallback never; `RowProjection.project` runs once per input row. Computed values are interned into the
  evaluation's synthetic id space (`SyntheticValueSource.internComputedValue`), not the compile-time catalog,
  which stays empty.
- The physical computed copy exposes its read mask (`CopyBinding.requiredMask()`, -1 for a semantic-value copy).
- New test `NativeSealedComputedBindProjectionTest`, 171 cases: 73 red before (all sealed-route assertions), all
  green after. Thirteen related classes, 534 common cases: no case changes state, so no previously red test turns
  green.
- Cost of the measured query: 157–169 ms before, 81–91 ms with the prototype, 17–23 ms with the boundary
  disabled. Of the prototype's time about 26 ms is the per-row projection, about 24 ms the native body and
  31–41 ms the boundary's own top-K stage.
- Corrections to this note. `?v = ?u` and `!bound(?v) || ?v = ?u` are planned as a semantic-value copy without a
  read mask and are refused physically; they belong in the refusal list. A FROM dataset is not refused and runs
  sealed with equal bag and origin facts. A caller-bound input cannot be seen in the explain text; the test
  detects the route from the row class instead. A source without a value codec is already excluded by the
  real-dictionary precondition on every assignment-bearing body.
- Mutations: off-spine, always-local and whitelist are each caught alone. "Target assured" and "read name outside
  the domain" are each shadowed by the physical guard and caught only as paired mutants. Dropping the codec
  precondition and admitting `setNullOnError` survive (the first is subsumed, the second has no planner path).
- Hazard found in review, being settled before the whole-module comparison: the join rule admits a shared name
  that is a resource on one side, so a computed target (for example `datatype(?v)`, an IRI with a synthetic id)
  could become a native join key against a stored subject. Rule added to the design: a computed target may not
  be a shared name of a Join or LeftJoin, nor a LeftJoin condition name.

## Merge (2026-11-17; working tree, uncommitted)

Evidence: `.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a3-computed-bind-proto2/`.

- Join-key facts on the prototype without the extra rule (same data now in the test: datatype IRIs stored as
  subjects, the computed numbers and strings stored as objects, `"02"` and `"2"` integers): a computed IRI as a
  join subject and as an OPTIONAL key was admitted and sealed, with equal bag and origin facts on that data. A
  computed number or string as a join object was already refused (the shared name is a resource on neither
  side). A LeftJoin condition that reads a computed target is relocated by the optimizer into the right arm as a
  `Filter`, and a BIND beside a UNION is distributed into both arms; both were already refused.
- Rule merged (`computedbind2.diff`, sha256 22b760c2…): the slots that are computed targets are carried per node
  beside `Domain`; a Join or LeftJoin whose two sides share a computed slot is refused, and so is a LeftJoin
  condition that names one. Union is unaffected (a computed element in a Union arm is off the spine). Dropping
  the rule turns exactly the two join-key cases red, on the route assertion only. The condition-name check is
  defensive: no query shape reaches it.
- Whole-module per-case comparison (path slices and OPTIONAL-condition slice as the base): 1,545 of 1,545 LMDB
  test classes, 19,896 cases; no stable green→red (11 candidates, each flaky or red in both states over eight
  reruns per state); 73 stable red→green, all the new test's sealed-route cases.
- Test first on the working tree: `NativeSealedComputedBindProjectionTest` 74 of 183 red (the sealed-route
  cases; one more than the agent's 73 because the FROM-dataset case now asserts the sealed route instead of
  printing it), 183 of 183 green after. The 13 related classes show the same 61 red cases before and after
  (`merge-pre.reds`, `merge-post.reds`): `LmdbNativePackedMorselsTest` 12, `NativeCompositeOriginBoundaryTest` 12,
  `LmdbNativePackedAlgebraTest` 11, `LmdbNativeWildcardPredicateBatchTest` 11, `LmdbNativeQueryExplanationTest` 10,
  `GeneratedValueKeysIntegrationTest` 3, `LmdbNativeLateralDifferentialTest` 2.
- Resulting `NativeSealedProjection.java` sha256 c8cc37a4…; test file sha256 d9359c25….
- Not proven: computed ids against differently encoded stored terms beyond that one dataset (the rule removes
  the admitted shapes); the cost of the measured query was not re-measured on the merged tree (prototype
  81–91 ms). The remaining gap to the boundary-disabled figure is the per-row projection and the boundary's
  top-K stage, the subject of a possible order push-down slice (owner decision open).
