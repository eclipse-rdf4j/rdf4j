Goal: Fix remaining TupleExprIRRendererTest failures by keeping the main path — TupleExpr → textual IR → IR transforms → SPARQL — and moving any printing-time heuristics into well-scoped IR transforms when possible.

Summary of current state (local run):
- Module: core/queryrender
- Test class: org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest
- Status: 128 run, 3 failures, 18 skipped
- Failing tests: deep_optional_path_2, deep_optional_path_3, deep_optional_path_5

Root causes and intended fixes
- Filter ordering inside OPTIONAL bodies (deep_optional_path_2, deep_optional_path_3)
  - Current behavior: IRBuilder emits the LeftJoin’s condition as an IrFilter appended to the end of the IrOptional body (IRBuilder#meet(LeftJoin)). IRTextPrinter preserves order, so the filter ends up after any nested OPTIONALs. The tests expect the filter to appear right after the first path/triple inside the OPTIONAL, and before nested OPTIONALs.
  - Fix strategy: keep IRBuilder simple (still append the filter into the optional’s body) but add a dedicated IR transform that reorders filters within an OPTIONAL body when it’s semantically safe:
    - Inside an IrOptional’s inner IrBGP, move IrFilter lines so that they appear before any IrOptional lines, provided the filter variables are already bound by the lines that precede it (conservative safety check).
    - Heuristic to detect safety: extract var names from filter text (?name tokens) and ensure all such vars also appear in the preceding head (collected from IrStatementPattern subjects/objects, IrPathTriple subject/object text, and IrPropertyList subject/objects). If not safe, don’t move.
    - Implement as IrTransforms.reorderFiltersInOptionalBodies and invoke it in the main transform pipeline.

- Path followed by UNION of opposite-direction tail triples (deep_optional_path_5)
  - Current behavior: we produce a path triple to an intermediate var followed by a UNION with two branches each containing a single triple that connects the intermediate to the final end var in opposite directions (e.g., mid foaf:name ?n vs ?n foaf:name mid). We print this as a UNION of two blocks.
  - Expected: a single fused path with an alternation tail on the last step: …/(foaf:name|^foaf:name) ?n.
  - Fix strategy: add an IR transform that detects the local pattern “IrPathTriple pt; IrUnion u” where u has two branches, each a single triple (optionally wrapped in a one-line GRAPH) that joins the path’s object to the same end var either forward or inverse with the same constant IRI.
    - Replace the [pt, u] pair with a single IrPathTriple whose pathText extends with “/(p|^p)” and whose objectText is the common end var.
    - Preserve surrounding lines and any following IrFilter on the same level (the test’s STRLEN filter stays outside of the UNION and unaffected by this rewrite).
    - Implement as IrTransforms.fusePathPlusTailAlternationUnion and call it after applyPaths, before property-list compaction.

Detailed plan (iterative)
1) Add IR transform: filter ordering in OPTIONAL bodies
   - Add IrTransforms.reorderFiltersInOptionalBodies(IrBGP, renderer)
   - For each IrOptional, recurse into its inner BGP, then reorder filters before nested IrOptional lines when safe by variable availability.
   - Also recurse through IrGraph, IrUnion, IrMinus, IrService, IrSubSelect conservatively using transformChildren.
   - Insert this step into transformUsingChildren() after applyPaths/coalesce/mergeOptionalIntoPrecedingGraph and before property list compaction (ordering neutrality).

2) Add IR transform: path + UNION alternating tail
   - Add IrTransforms.fusePathPlusTailAlternationUnion(IrBGP, renderer)
   - Scan a BGP sequence: if IrPathTriple is followed by IrUnion with exactly two branches each with one IrStatementPattern (or IrGraph containing one IrStatementPattern), whose predicate is the same constant IRI, and one branch connects pt.object → end forward while the other connects end → pt.object (inverse), then fuse.
   - Build new IrPathTriple with pathText “pt.path/(p|^p)” and object “?end”.
   - Recurse into containers; keep non-matching unions intact.
   - Insert this step after applyPaths (so earlier fusions/alternations have already run) and before property list compaction.

3) Keep IRBuilder minimal
   - Do not move filter-placement policy into IRBuilder; maintain a single policy place (IrTransforms). This keeps TupleExpr → IR predictable and delegates shape normalization to the transform layer.

4) Verify and adjust
   - Re-run core/queryrender tests offline.
   - If ordering issues persist in different nestings, extend reorderFiltersInOptionalBodies to:
     - consider IrGraph wrappers when identifying the “first nested OPTIONAL line”, and
     - handle multiple IrFilter lines, preserving their relative order and moving only the safe subset.
   - If alternation fusion misses GRAPH-wrapped union branches, allow the branch to be a single IrGraph containing a single IrStatementPattern and verify both branches have compatible graph refs.

5) Formatting and pre-commit
   - Run: mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format
   - Quick build without tests: mvn -o -Pquick verify -DskipTests | tail -1000
   - Run the specific tests: mvn -o -pl core/queryrender -Dtest=TupleExprIRRendererTest test

Progress log
- Baseline (now): 3 failures — deep_optional_path_2/3/5. Root causes identified as above.
- Next checkpoints:
  - After (1): deep_optional_path_2 and _3 should pass (filter ordering).
  - After (2): deep_optional_path_5 should pass (path+UNION alternation tail).

Update 1 (implemented):
- Added transform reorderFiltersInOptionalBodies() and integrated into pipeline.
- Added transform fusePathPlusTailAlternationUnion() and integrated into pipeline.
- Result: TupleExprIRRendererTest now passes fully (128 run, 0 failures, 18 skipped) locally for core/queryrender.

Notes / constraints
- Keep transforms conservative: only rewrite when structural preconditions match and variable-safety checks succeed.
- Do not rewrite inside SERVICE or subselects unless explicitly needed by tests (current failures don’t involve these).
- Maintain GRAPH scoping: when fusing, ensure branches agree on graph ref or skip the fusion.

If anything else fails after these fixes, iterate similarly: inspect shape at IR level (IrDebug.dump), add narrowly-scoped transforms, and avoid ad-hoc printing-time reordering.
