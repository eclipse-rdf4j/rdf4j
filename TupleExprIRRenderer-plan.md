# TupleExprIRRenderer / IR Nodes – Plan

Owner: Codex Agent
Date: 2025-08-24

Goal: finalize the IR transformation pipeline by (a) adding `transformChildren` to IR nodes, (b) refactoring transforms to function style using these helpers, (c) removing residual print-time optimizations in `TupleExprIRRenderer`, and (d) unfreezing child fields (remove `final`) and add setters where needed.

## Context

- The textual IR lives in `core/queryrender/.../sparql/ir/*` and is rendered by `TupleExprIRRenderer.IRTextPrinter`.
- Current transforms (in `ir/util/IrTransforms`) recurse via type checks and new-instance construction. We will switch to a uniform `transformChildren(UnaryOperator<IrNode>)` across nodes.
- `IRTextPrinter` still performs several print-time fusions (paths, property lists, union-as-path, collection overrides). These should be handled in `IrTransforms` and removed from printing.
- Several IR nodes with children have `final` fields, which blocks functional rewrites. We’ll remove `final` for child fields and add setters.

## Tasks

1) Inventory IR nodes and identify children/final fields [DONE]
- Nodes with children: `IrWhere`, `IrGraph`, `IrOptional`, `IrUnion`, `IrMinus`, `IrService`, `IrSubSelect`, `IrSelect`.
- Child fields frozen by `final`: `IrWhere.lines`, `IrUnion.branches`, `IrGraph.where`, `IrOptional.where`, `IrMinus.where`, `IrService.where`, `IrSubSelect.select`.

2) Add transformChildren API [DONE]
- Add default `transformChildren(UnaryOperator<IrNode>)` in `IrNode` (no-op for leaves).
- Override in container nodes to rebuild with transformed children.
- Provide setters for child fields to align with mutable updates if ever needed by downstream code.

3) Refactor IrTransforms to use transformChildren [DONE]
- Replace custom recursion helpers with calls to `node.transformChildren(child -> ...)`.
- Keep top-level pattern logic (e.g., sibling fusion in a WHERE) as-is; only recursion switches to the function form.

4) Remove residual print-time optimizations [DONE]
- Simplify `IRTextPrinter.printLines()` to just delegate to `IrNode#print()` for each line.
- Remove collection override detection, SP/path fusions, union-as-path and property-list aggregation logic from printing.
- Keep basic indentation and block handling.

5) Ensure fields and setters [DONE]
- Remove `final` from child fields and add setters:
  - `IrWhere.lines` → add `setLines(List<IrNode>)`.
  - `IrUnion.branches` → add `setBranches(List<IrWhere>)`.
  - `IrGraph.where` + `setWhere`, `IrGraph.graph` + `setGraph`.
  - `IrOptional.where` + `setWhere`.
  - `IrMinus.where` + `setWhere`.
  - `IrService.where` + `setWhere`.
  - `IrSubSelect.select` + `setSelect`.
  - `IrSelect.where` already mutable; others are lists (left mutable).

6) Build & format [DONE]

7) BGP shorthand transform [DONE]
- Implemented `IrPropertyList` and `applyPropertyLists` to compact contiguous triples with the same subject, using `;` and commas for repeated predicates/objects. Applied recursively to BGPs (including inside GRAPH/OPTIONAL/MINUS/SERVICE/UNION).

8) Improve path fusion (chain + joins) [IN PROGRESS]
- Chain fusion (SP..SP via _anon_path_ → IrPathTriple) in `applyPaths`.
- Adjacent and non-adjacent joins for `PT+SP` and `SP+PT` inside BGPs, including nested containers.
- Special-case forward→inverse tail fusion: `?s p ?mid . ?y q ?mid` → `?s p/^q ?y`.
- New normalization pass for inner GRAPH bodies after alternation creation.
- Current status: `deep_path_in_minus` passes. Remaining GRAPH cases (`morePathInGraph`, `testMoreGraph1/2`) still show unfused `(alt)` + inverse tail. Next: dedicated in-graph alternation-tail fuser.

9) Replace deprecated applyAll with transformChildren [DONE]
- Added `IrTransforms.transformUsingChildren(IrSelect, Renderer)` and switched `TupleExprIRRenderer` to use it; only `WHERE` is copied back to avoid re-allocating the `IrSelect` header/meta.

10) Add in-graph alternation + inverse-tail fuser [IN PROGRESS]
- Added `fuseAltInverseTailBGP` to fuse `(?x (alt) ?mid) + (?y p ?mid)` into `?x (alt)/^p ?y` inside BGPs (incl. GRAPH). Still iterating to ensure it triggers for the remaining tests.
- Run formatter.
- Build `core/queryrender` offline to validate compilation.

## Decisions

- Keep transforms conservative and deterministic; do not reintroduce print-time structural changes.
- The `mergeAdjacentGraphBlocks` string post-process remains for now (low risk). If tests expect raw adjacency, we can drop it later behind a flag.

## Work log

- 2025-08-24: Scanned IR nodes and renderer. Prepared plan. Implemented `transformChildren` in `IrWhere`, `IrGraph`, `IrOptional`, `IrMinus`, `IrService`, `IrSubSelect`, `IrUnion`, `IrSelect`. Removed `final` from child fields and added setters. Refactored recursion in `IrTransforms` to use function-style child mapping. Simplified `IRTextPrinter.printLines()` to delegate to `IrNode#print` and removed path/collection/union print-time fusions. Fixed malformed methods after edits and verified `core/queryrender` compiles offline. Ran module-level formatting.
- 2025-08-24: Added IrPropertyList and `applyPropertyLists` transform for `;` and `,` shorthand. Improved path fusion in `applyPaths` (multi-step chain + `{PT,SP}` joins). Updated node printers (OPTIONAL/MINUS/GRAPH/SERVICE/VALUES/Subselect) to brace style. Targeted tests indicate remaining failures in deep path fusion (MINUS) and graph-internal alternation chain; will address next iteration.
- 2025-08-24: Replaced deprecated `applyAll` with function-style `transformUsingChildren`. Cleaned IrTransforms parse issues. Implemented non-adjacent join and a forward→inverse tail fuser; added `normalizeGraphInnerPaths`. `deep_path_in_minus` now green. Added `fuseAltInverseTailBGP`; next iteration will refine to ensure alternation + inverse tail inside GRAPH collapses into `(...)/^...` in `morePathInGraph` and `testMoreGraph1/2`.
