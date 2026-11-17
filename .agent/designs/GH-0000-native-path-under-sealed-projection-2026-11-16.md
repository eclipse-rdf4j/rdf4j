# Property paths under the semantic projection boundary: admit them through the sealed projection (2026-11-16)

Status: slices 1 and 2 merged into the working tree 2026-11-17 (uncommitted; see "Slice 1 hand-back" and "Slice 2
merge"). Slice 2 covers a zero-length closure with a constant endpoint and alternatives of single patterns in one
direction. Not started: `:p?`, sequences and mixed-direction alternatives inside a closure, `?s :p* ?o`, paths
under GRAPH. Routine A (behaviour change: which route executes a SELECT over a property path).

## Observation

Since the semantic projection boundary (4b2a72bc4c, 2026-11-04) is taken first in
`LmdbNativeAggregateCompiler.compilePhysical`, a `Projection` whose argument contains an `ArbitraryLengthPath`
never reaches a native path plan:

- `NativeBGPOrigin.of` (`NativeBGPOrigin.java:111`) and `NativeProjectionAuthorityProof.of` (`:117`) return null
  for every node that is not a statement pattern (or VALUES/singleton/empty for the authority), so
  `NativeRootPipeline.needsSemanticProjection` (`:197-201`) is true;
- the argument is prepared through `exactOriginView(...).precompileRuntimeSubtree`, which hands the path to the
  ordinary evaluator (`LmdbNativeEvaluationStrategy.java:586-588`); the ordinary path evaluation then drives one
  native statement-pattern leaf per frontier node;
- `NativeSealedProjection.plainBody` (`:152`), the one place under the boundary that compiles a whole body
  natively, returns null for a path node.

Measured (`.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a1-c-paths-cost/results.tsv`,
200,000-edge tree, identical row counts): `<root> :p+ ?o` 1,675 ms against 306 ms with the boundary disabled;
`<root> :p* ?o . ?o :q ?v` 2,706 ms against 47 ms; `<root> (:p|:q)+ ?o` 3,074 ms against 301 ms. 33 of the 40 red
cases in `LmdbNativePropertyPathTest`, `LmdbNativeGeneralPathTest` and `LmdbNativeZeroLengthNodeIdsTest` are this.

## Why the boundary cannot simply be skipped for paths

With the boundary disabled, 33 of 56 cases in `NativePathProjectionProvenanceContractTest` and
`LmdbNativeContextSelectionContractTest` go red: rows of the native row root carry no origin metadata for a path
(`tracked=false, locals=[]`), and two cases return wrong bags
(`pathStepScopeGraphAndAlternativesRetainTheirOwnDomains[2]`,
`pathIdentityDomainBelongsToTheEnclosingSelector[2]`: zero-length identity rows under an enclosing GRAPH
selector). The boundary is what keeps these correct. Trying the native row root first was measured on
2026-11-16 and rejected for the same reason.

## Required behaviour

The sealed projection already owns exactly the case that matters: an empty, untracked invocation frame
(`RowProjection.supportsInput` → `emptyFrame`), no active graph, no substitution names, no enclosing projection, a
body of plain positive statement algebra, every output cell a completed store term; it builds the row's
provenance itself (`RowProjection.project`: tracked, locals = the bound outputs, empty graph frame) and falls
back to the prepared semantic step on every refusal (`Choice`). Extend that unit, and nothing else:

1. `plainBody` admits an `ArbitraryLengthPath` node as a producer when all of the following hold; otherwise it
   returns null as today.
   - Exact class `ArbitraryLengthPath`, own logical origin, not an independent input scope, not a variable-scope
     change (the existing per-node checks apply unchanged).
   - Scope `DEFAULT_CONTEXTS` and no context variable (no GRAPH around the path, neither constant nor variable).
   - The path expression subtree consists only of nodes the native path planner compiles today AND that pass the
     same per-pattern checks `plainBody` applies to a top-level statement pattern (exact `Var` class, constants
     are IRIs with a value, no observed constant name, known observation domain, no inherited context origin).
     Start with: one statement pattern; then alternatives/sequences only if the path planner compiles them and
     the contract tests stay green. Anything else: null.
   - Each endpoint is a non-constant exact `Var` without a value, or a constant `Var` whose value is an IRI. A
     constant endpoint contributes no slot.
   - `minLength >= 1` (slice 1). `minLength == 0` is admitted only when at least one endpoint is a constant IRI
     (slice 2): the identity row is then that constant, never a graph-node enumeration. `?s :p* ?o` with two
     variable endpoints stays semantic.
   - Domain: `possible` = `assured` = the variable endpoints. `resource` contains the subject endpoint only when
     `minLength >= 1` (it is then the subject of a stored statement); the object endpoint is never claimed as a
     resource. The existing join rule (a shared name must be a resource on one side) then decides joins.
2. `PhysicalProof.create` and `completeRows` admit the physical path plan node(s) the planner produces for such
   a body as a leaf, with `possible` = its produced mask, under the conditions that mirror the pattern checks:
   no key range, no entry-dependent field, no bind-constant / read-only / preserve-existing term, default-graph
   scope. A path plan with any feature the proof does not name is refused (return false / null), never assumed.
   The assured mask comes from the same `SlotPlan.assuredMask` machinery; if that does not already cover the
   path plan class, the proof refuses rather than adding a general claim to `SlotPlan`.
3. Everything else is unchanged: the `tryCompile` preconditions, the `carrier.catalog.isEmpty()` requirement (a
   constant endpoint that is not a stored term therefore stays semantic), `supportsSealedDispatch`, the semantic
   fallback, and the boundary for every other shape.

## Invariant to prove before anything is merged

"For an admitted body, on an empty untracked frame, the sealed rows equal the semantic projection's rows as a
bag, and each row's provenance equals what the semantic projection produces." Evidence required:

- `NativePathProjectionProvenanceContractTest` and `LmdbNativeContextSelectionContractTest`: no case green before
  and red after (the one pre-existing red, `explicitProjectionRestoresAnUnproducedHiddenRequest`, is out of
  scope).
- A new differential test (red first where it asserts the native route): for a generated graph with cycles,
  self-loops, multi-parent nodes, literals and blank nodes as path objects, and for the query set below, the
  bag from the native-disabled evaluator equals the bag with native enabled, and for the admitted shapes the
  executed plan is a `SealedProjection(...)` containing a path plan. Queries: `<c> :p+ ?o`, `?s :p+ <c>`,
  `?s :p+ ?o`, `<c> :p* ?o`, `?s :p* <c>`, `<c> :p+ ?o . ?o :q ?v`, `<c> :p* ?o . ?o :q ?v`,
  `?s :q ?v . ?s :p+ ?o`, `<c> (:p|:q)+ ?o`, `<c> :p+ <d>`; and the refusals, asserted as "not sealed, same
  bag": `?s :p* ?o`, a path inside `GRAPH <g>` and `GRAPH ?g`, a constant endpoint absent from the store, a
  literal constant endpoint, a query evaluated with a non-empty input binding for an endpoint, FROM / FROM NAMED
  dataset clauses.
- One mutation per rule (admit `minLength == 0` with two variable endpoints; claim the object endpoint as a
  resource; admit a GRAPH-scoped path): each must turn a test red.

## Acceptance

- The 33 path reds: count how many turn green; any that stay red are listed with their first failure line and
  not "fixed" by editing the test.
- The cost test (`A1PathCostTest`): queries a, d, f within 1.5× of the boundary-disabled medians.
- Whole-module per-case comparison (both LMDB selections and the evaluation module) with no stable green→red;
  only then is the change applied to the working tree, by the owner.

## Not in this change

Paths under GRAPH; `?s :p* ?o` with two variable endpoints; `:p?`; tracked or non-empty invocation frames;
giving path rows origin metadata at the native row root (the leaf hook `NativeExactOriginStep`); the composite
OPTIONAL/UNION owner cases of `NativePureUnionVisibilityBoundaryTest` and `NativeCompositeOriginBoundaryTest`;
BIND/Extension shapes that lose packed execution under exit E3.

## Slice 1 hand-back (2026-11-17; merged into the working tree, uncommitted)

Evidence: `.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a1-c-paths-proto/`,
`a1-c-paths-proto2/`, `a1-c-paths-proto3/` (`slice1.diff`, `slice1-tests.diff`, `reconciliation.txt`,
`NativeSealedPathProjectionTest-pre-fix-red.txt`).

- Change: `NativeSealedProjection` only — `pathDomain` (minimum length one, one default-graph statement pattern
  with a constant IRI predicate, endpoints a plain variable or a constant IRI, the step subject claimed as a
  resource), `PathPlan` admitted as a leaf in `PhysicalProof.create`, and a `PathPlan` branch in `completeRows`
  that refuses more than one step, zero length, a named-graph scope, a context slot or flag, and a fixed context
  set (a FROM dataset). The `PathPlan` keeps only predicate, positions, context term, context constraint and
  endpoint slots, so those checks cover every retained field.
- `SlotPlan.assuredMask` has no `PathPlan` branch and returns 0, so an alias over a path endpoint is refused
  physically (conservative; `SlotPlan` not widened). A path under GRAPH never reaches `plainBody` as a path node.
- Test `NativeSealedPathProjectionTest` (41 cases, about 20 s): bag and row origin facts, native disabled against
  native enabled, for the admitted shapes, inverse steps, a path inside OPTIONAL, two joined paths, DISTINCT and
  ORDER BY over a path, the closed pair `<c> :p+ <d>` reachable and unreachable, the refusals, and a seeded random
  differential (3 seeds, 150 nodes). Red first on the working tree: 9 of 41, all sealed-route assertions.
  Mutations (admit minimum length zero; claim the object endpoint as a resource; admit a context or dataset) each
  turn a case red.
- Whole-module per-case comparison: 1,542 of 1,542 LMDB test classes, 19,638 cases, no stable green→red, 29
  stable red→green (`LmdbNativePropertyPathTest` 13, the new test 9, `LmdbNativeStreamingReadContextTest` 3,
  `LmdbNativeQueryExplanationTest` 2, `LmdbNativePackedMorselsTest` 1, `LmdbNativeWorkerLifecycleTest` 1).
- Cost: `<root> :p+ ?o` on the 200,000-edge tree about 345 ms (1,670 ms before, 306 ms with the boundary
  disabled). `:p*` with a join and `(:p|:q)+` unchanged — slices 2 and 3.
- On the working tree after the merge: new test 41/41; `LmdbNativePropertyPathTest` 10 red (was 23),
  `LmdbNativeGeneralPathTest` 9, `LmdbNativeZeroLengthNodeIdsTest` 8 (shapes outside slice 1),
  `NativePathProjectionProvenanceContractTest` 1 (pre-existing, out of scope), `LmdbNativeContextSelectionContractTest`
  and the four sealed-projection classes green.

## Slice 2 merge (2026-11-17; working tree, uncommitted)

Evidence: `.agent/research/GH-0000-phase1-continuation-2026-11-09/triage-families/a1-c-paths-slice2/` (prototype)
and `a1-c-paths-slice2b/` (`pathslice2.diff`, `pathslice2-tests.diff`, `STATUS.md`, `compare-L1..L4.txt`,
`reconciliation.tsv`, `NativeSealedPathProjectionTest-pre-fix-red.txt`, `slice2-pre.reds`, `slice2-post.reds`).

- Change: `NativeSealedProjection` only. `pathDomain` now admits (a) minimum length zero when at least one
  endpoint is a constant IRI, and (b) a path expression that is a tree of exact `Union` nodes (no variable-scope
  change) over single statement patterns that each pass the slice-1 pattern checks and all run in the same
  direction. A zero-length path claims no endpoint as a resource (the identity row can be any node); otherwise
  the endpoint that is the stored subject of every alternative is claimed. `completeRows` accepts one or more
  steps with identical subject and object positions, and refuses a zero-length plan whose two endpoint constants
  are both unknown. A constant endpoint that is not a stored term still fails the empty-catalog requirement and
  stays semantic.
- Refused and asserted as "not sealed, same bag, same row provenance": `?s :p* ?o`; an absent constant endpoint
  on either side of `:p*`; a constant stored only as an object (`<n9> :p* ?o`); mixed-direction alternatives
  `(:p|^:q)+`; a sequence inside the closure `(:p/:q)+`; `?s (:p|:q)+ <d>`; `<c> :p* <c>`; `:p?`; GRAPH, FROM,
  FROM NAMED; a zero-length path object joined on a pattern object only.
- Test `NativeSealedPathProjectionTest`, 69 cases (about 20–30 s): the refusal cases and the random differential
  now also compare row provenance; the random differential runs one query per shape per seed. A scratch report
  write to an absolute temporary path, left in the slice-1 version of the test, is removed.
- Whole-module per-case comparison (isolated copies, before the OPTIONAL-condition slice and the startup fix
  were in the base): 1,541 of 1,541 LMDB test classes (19,618 cases; `LmdbPreparedBulkStoreTest` excluded in
  both states), no stable green→red under the eight-rerun rule, 14 stable red→green.
- Test first on the working tree (with the OPTIONAL-condition slice and the startup fix present): the updated
  test 11 of 69 red, all sealed-route assertions; after the change 69 of 69. Ten related classes, 329 cases,
  compared case by case: 39 red before, 25 after, no case green→red; red→green are the 11 new-test cases and
  `LmdbNativePropertyPathTest` `allPairsAlternationSeedsFromTheUnionOfAdjacencyKeyDomains`,
  `alternationPathRunsNatively`, `inverseAlternationAndZeroLengthRunNatively`. Remaining reds, all present
  before: `LmdbNativePropertyPathTest` 7, `LmdbNativeGeneralPathTest` 9, `LmdbNativeZeroLengthNodeIdsTest` 8
  (`:p?` with two variable endpoints, same-variable endpoints, general path forms),
  `NativePathProjectionProvenanceContractTest` 1.
- Cost (agent's probe before the prototype, `a1-c-paths-slice23-facts/`): `<root> :p* ?o . ?o :q ?v` about
  2,700 ms to 53–67 ms; `<root> (:p|:q)+ ?o` about 3,000 ms to 322–377 ms. Not re-measured on the merged tree.
