# TupleExprIRRenderer: Union Scope, Path-Generated Unions, and What To Fix

This report summarizes what I found by:
- Running org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest and inspecting failures/diffs and IR dumps
- Building an exploration test suite (TupleExprUnionPathScopeShapeTest) that enumerates explicit vs. path‑generated unions across GRAPH/SERVICE/OPTIONAL/MINUS and nested combinations
- Reading TupleExprToIrConverter, IrTransforms (esp. ApplyPathsTransform), and the IR node classes

It explains how explicit vs. path‑generated unions differ, why certain unions end up with a “new scope” that blocks path fusions, and what precise code changes will make the renderer produce the expected canonical SPARQL.

## Current Behavior (Observed)

- Explicit UNION (from surface `... } UNION { ...`) is created with `Union#setVariableScopeChange(true)` by the parser. In IR, this becomes `IrUnion.newScope=true`.
- Path‑generated unions (alternation `a|b`, NPS `!(a|^b)`, and `?` zero‑or‑one) are built by the parser with `setVariableScopeChange(false)` (or default false), and IR sets `IrUnion.newScope=false`.
- However, when a path‑generated union is the root of a branch inside an explicit UNION (or inside a container like SERVICE/GRAPH/OPTIONAL/MINUS), the algebra frequently marks the nested UNION as “(new scope)”. This happens due to subsequent normalizers/optimizers and grouping semantics. In IR, that nested `IrUnion` ends up with `newScope=true` even though it originates from path syntax.
- IrTransforms (ApplyPathsTransform, FusePrePathThenUnionAlternationTransform, ApplyNegatedPropertySetTransform, etc.) are fairly conservative: they refuse to merge a `newScope` union unless they can prove it came from parser path decoding (look for shared `_anon_path_*` variables across branches).

Effect: in several scenarios, the transformer declines to fuse simple, safe alternations into a property path because the nested union carries `newScope=true` and there are no `_anon_path_*` bridge variables (for example, `{ ?s foaf:knows ?o } UNION { ?s ex:knows ?o }`).

## Evidence From Failing Tests

Failures in TupleExprIRRendererTest (abridged) show the desired canonical result is a fused path expression rather than explicit `UNION` blocks:

- service_with_graph_and_path
  - Expected: `SERVICE ?svc { GRAPH ?g { ?s (foaf:knows|ex:knows) ?o . } }`
  - Actual: nested braces with an explicit `UNION` inside SERVICE/GRAPH.

- values_then_graph_then_minus_with_path
  - Expected: `MINUS { ?s (ex:knows|foaf:knows) ?o . }`
  - Actual: `MINUS { { ?s ex:knows ?o } UNION { ?s foaf:knows ?o } }`

- testValuesGraphUnion6 and related
  - Expected: one path with alternation/NPS inside a `GRAPH`, optionally combined with VALUES outside.
  - Actual: explicit `UNION` branches inside GRAPH.

IR dumps confirm that in these scenarios the nested `IrUnion` typically has `newScope=true`, and branch BGPs often have no `_anon_path_*` vars (endpoints are user vars, e.g., `?s`, `?o`). The transforms gate on `newScope` + “no shared anon path” → no fusion occurs.

## What My Tests Show (Scope and Shape)

In TupleExprUnionPathScopeShapeTest I recorded algebra and raw/transformed IR in many cases. Key findings:

- Plain alternation `(ex:a|ex:b)` outside containers → `Union` with `variableScopeChange=false` (IR: `newScope=false`), transforms fuse into `IrPathTriple` as expected.
- NPS `!(ex:p1|^ex:p2)` outside containers → `Union` with `newScope=false` (two filtered SPs merged into NPS), transforms fuse into `IrPathTriple` with NPS.
- Containers with path alternations:
  - GRAPH { ?s (a|b) ?o } → Algebra shows union of SPs in FROM NAMED; raw IR often has `IrUnion.newScope=false`; transforms fuse into `IrPathTriple` inside `IrGraph` (OK).
  - OPTIONAL { ?s (a|b) ?o } and MINUS { ?s (a|b) ?o } → raw IR shows `IrUnion.newScope=false`; transforms fuse to a single `IrPathTriple` under OPTIONAL/MINUS (OK).
  - SERVICE { ?s (a|b) ?o } → raw/transformed IR show `IrUnion.newScope=true` in many inputs; because there is no `_anon_path_*` bridge var when endpoints are `?s` and `?o`, transforms decline to merge. This directly explains `service_with_graph_and_path` and similar failures.
- Branch root path unions in explicit UNIONs also pick up `newScope` and are not fused unless they share a parser bridge variable. This blocks canonicalization in several Values+Graph+Union tests.

Conclusion: even when a nested union is marked `newScope=true`, there are common safe cases where fusing into a property path alternation does not alter semantics (e.g., `{ ?s pA ?o } UNION { ?s pB ?o }`). The current transforms don’t allow this because they rely on `_anon_path_*`-based safety for new-scope unions.

## Root Cause

Two interacting issues:

1) New-scope marking leaks onto path‑generated unions when they are placed as branch roots inside explicit unions or inside containers (SERVICE/GRAPH/OPTIONAL/MINUS). This is correct for grouping semantics but does not necessarily indicate a user-authored explicit union — it can be an artifact of parsing and grouping.

2) Transform policy forbids fusing unions that carry `newScope=true` unless branches share `_anon_path_*` vars (proof of path-decoding origin). This excludes valid, safe alternation fusions where each branch is a single constant‑IRI step with identical endpoints (or a simple NPS member), which is exactly what the tests expect to be canonicalized.

## Proposed Fix (Precise Changes)

We should expand the “allowed to fuse even when `u.isNewScope()`” rule to include another conservative, verifiable case: both branches reduce to a single triple-like with identical endpoints (optionally inside the same GRAPH), and each predicate/path is atomic (constant IRI or a simple canonical NPS member), with no extra user-visible bindings introduced.

Concretely:

1) ApplyPathsTransform — general UNION alternation rewrite
   - Location: `core/queryrender/.../ApplyPathsTransform.java` in the block `if (n instanceof IrUnion) { ... }` around the `permitNewScope` calculation.
   - Today: `permitNewScope = !u.isNewScope() || unionBranchesShareAnonPathVarWithAllowedRoleMapping(u)`, then if not permitted, bail out.
   - Change: add an additional allowance `branchesFormSafeAlternation(u)` and use it when `u.isNewScope()` is true. That predicate should return true iff:
     - Every branch is exactly one `IrTripleLike` (either `IrStatementPattern` or `IrPathTriple`), optionally wrapped in a single `IrGraph` with the same graph ref on all branches.
     - Endpoints (subject/object) align across branches (allow inverting a simple SP by prefixing `^` as already supported) so we can produce `?s (pA|pB|...) ?o`.
     - Each piece (predicate/path text) is atomic (no top‑level `|` or `/`, and no quantifiers), or is a simple canonical NPS `!(...)` member.
   - When fusing under `newScope=true`, preserve grouping semantics by wrapping the fused `IrPathTriple` in an `IrBGP` marked `newScope=true` (ApplyPathsTransform already contains code to wrap when needed for the GRAPH + SP + UNION fusion path; mirror that behavior in the general alternation rewrite).

2) ApplyPathsTransform — “GRAPH/SP followed by UNION over bridge var” rewrite
   - Same idea: the preconditions already allow new-scope union fusing if `unionBranchesShareAnonPathVarWithAllowedRoleMapping(u)` is true. Extend to also allow `branchesFormSafeAlternation(u)` when the branch pieces are trivial triple-like elements under a single GRAPH ref (exactly the case in `service_with_graph_and_path` and `testValuesGraphUnion6`). The code already builds the fused `IrPathTriple` and reorders any remaining inner lines; just relax the gate.

3) Optional (but helpful) — IR builder hint for path-generated unions
   - Location: `TupleExprToIrConverter.meet(Union)`.
   - The IR builder currently sets `IrUnion.newScope = u.isVariableScopeChange()`. For unions that are clearly path-generated (both branches are a single SP/Filter+SP pair over identical endpoints, or recognized NPS piece), we could set `IrUnion.newScope=false` even if `u.isVariableScopeChange()` is true. The transforms can then proceed without the extra new‑scope gate. This is a quality-of-implementation improvement; not strictly necessary if we implement (1) and (2) correctly.

## Why This Is Safe

The proposed `branchesFormSafeAlternation(u)` is conservative:
- It demands each branch be a single triple-like with identical endpoints (or a verified invertible pair), optionally under the same graph reference.
- It rejects cases with additional user-visible bindings or complex path expressions where alternation could reorder or change precedence.
- It preserves explicit grouping: when fusing under `newScope=true`, the fused result is wrapped in a brace group (`IrBGP.newScope=true`).

This aligns with the test oracle’s expectations while retaining all safety constraints around `_anon_path_*` variables for more complex merges.

## Examples (Before → After)

1) SERVICE with GRAPH alternation
- Before (actual):
  ```
  SERVICE ?svc {
    GRAPH ?g {
      { ?s foaf:knows ?o } UNION { ?s ex:knows ?o }
    }
  }
  ```
- After (expected):
  ```
  SERVICE ?svc { GRAPH ?g { ?s (foaf:knows|ex:knows) ?o . } }
  ```

2) MINUS with alternation
- Before:
  ```
  MINUS { { ?s ex:knows ?o } UNION { ?s foaf:knows ?o } }
  ```
- After:
  ```
  MINUS { ?s (ex:knows|foaf:knows) ?o . }
  ```

3) GRAPH with alternation + NPS
- Before:
  ```
  GRAPH ?g0 {
    { ?s ex:pA ?o } UNION { ?s !(foaf:knows|^foaf:name) ?o } UNION { ?s ex:pB ?o }
  }
  ```
- After:
  ```
  GRAPH ?g0 { ?s (ex:pA|!(foaf:knows|^foaf:name)|ex:pB) ?o . }
  ```

4) VALUES + GRAPH + UNION
- Before:
  ```
  { VALUES ?s { ex:s1 ex:s2 } { GRAPH ?g0 { { ?s ex:pA ?o } UNION { ?s ^foaf:name ?o } } } } UNION { ?u2 ex:pD ?v2 }
  ```
- After:
  ```
  { VALUES ?s { ex:s1 ex:s2 } { GRAPH ?g0 { ?s (ex:pA|^foaf:name) ?o . } } } UNION { ?u2 ex:pD ?v2 }
  ```

## How This Relates To Explicit vs Path-Generated Union Scope

- Explicit unions are real surface `UNION`s and should remain as such — unless their branches reduce to a safe single alternation over the same endpoints. In such a case we can preserve grouping (brace pair) but collapse to a single `IrPathTriple` with an alternation.
- Path-generated unions arise from `a|b`, NPS, and `?`. They should not be marked as scope changes. When they pick up `newScope` because of surrounding structure (branch root or container rules), the transforms should still be allowed to compact them using the conservative checks above.

## Step‑By‑Step Code Changes

1) In ApplyPathsTransform general union rewrite (around the `permitNewScope` logic):
   - Add a helper `branchesFormSafeAlternation(IrUnion u, TupleExprIRRenderer r)` that implements the check listed above (single `IrTripleLike` per branch, same endpoints, identical graph ref, atomic predicate/path or simple NPS).
   - Replace:
     ```java
     boolean permitNewScope = !u.isNewScope() || unionBranchesShareAnonPathVarWithAllowedRoleMapping(u);
     if (!permitNewScope) { out.add(n); continue; }
     ```
     with:
     ```java
     boolean permitNewScope = !u.isNewScope()
         || unionBranchesShareAnonPathVarWithAllowedRoleMapping(u)
         || branchesFormSafeAlternation(u, r);
     if (!permitNewScope) { out.add(n); continue; }
     ```
   - When `u.isNewScope()` and we fuse, wrap the fused `IrPathTriple` in an `IrBGP` with `newScope=true` (there’s already precedent around line ~1069 to preserve scope by wrapping; reuse that pattern).

2) In the “GRAPH/SP followed by UNION over bridge var” block:
   - Extend the existing `if (u.isNewScope() && !unionBranchesShareAnonPathVarWithAllowedRoleMapping(u))` gate to also permit
     `branchesFormSafeAlternation(u, r)`.
   - This handles `GRAPH { ?s pA ?o } UNION { GRAPH { ?s pB ?o } }` patterns and the SERVICE‑contained variant.

3) Optional: In TupleExprToIrConverter.meet(Union)
   - Detect trivially path‑generated unions (two single SPs with identical endpoints or two bare NPS members) and set `IrUnion.newScope=false` even if `u.isVariableScopeChange()` is true. This helps transforms but is not strictly required if (1) and (2) are applied.

## Closing Notes

The changes above are narrowly targeted, preserve safety guarantees (no user variables are removed or merged), and match the shape expected by TupleExprIRRendererTest in all the failing scenarios I’ve observed:
- `SERVICE` with GRAPH + alternation
- `MINUS` + alternation
- `VALUES` + `GRAPH` + `UNION` → alternation (including NPS)
- Mixed explicit unions whose branches reduce to a simple alternation over identical endpoints

The transforms already contain most of the machinery; the main gap is the overly strict `newScope` gate. Relaxing it for the “safe alternation” case and wrapping the fused result to preserve grouping fixes the canonicalization while keeping semantics intact.


