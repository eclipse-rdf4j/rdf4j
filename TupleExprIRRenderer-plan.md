Goal: Fix remaining TupleExprIRRendererTest failures by keeping the main path — TupleExpr → textual IR → IR transforms → SPARQL — and moving any printing-time heuristics into well-scoped IR transforms when possible.

- Module: core/queryrender
- Test class: org.eclipse.rdf4j.queryrender.TupleExprIRRendererTest

Read the following files before you start:
 - [IrTransforms.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir/util/IrTransforms.java)
 - [TupleExprIRRenderer.java](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/TupleExprIRRenderer.java)
 - All the files in [ir](core/queryrender/src/main/java/org/eclipse/rdf4j/queryrender/sparql/ir)

Keep these in your context.

Nice to know:
 - Variables generated during SPARQL parsing typically have a prefix that tells you why they were generated. Such as the prefixes "_anon_path_" or "_anon_collection_" or "_anon_having_".
 - When a UNION is created because of a SPARQL path, the union does not have a new scope.

DO NOT CHANGE ANYTHING ABOVE THIS LINE.
-----------------------------------------------------------

Add your plan here:

1) Triage & Scope
- Run `mvn -o -pl core/queryrender test -Dtest=TupleExprIRRendererTest` once to list failing cases and group by pattern (paths, GRAPH/OPTIONAL placement, NPS, property lists, collections, subselect zero-or-one, filter ordering).
- For each group, confirm if the gap is (a) missing/insufficient IR transform or (b) printing-time heuristic still influencing output.

2) Guardrails (keep throughout)
- Keep main path intact: TupleExpr → textual IR (IrSelect/IrBGP/…) → IR transforms → render.
- Do not add any new printing-time fusions. If unavoidable for parity, add a temporary transform and remove the string-level helper.
- Respect parser hints: only fuse across `_anon_path_*`, `_anon_collection_*`, `_anon_having_*` bridge vars; never guess beyond hints.
- When a UNION is created because of a SPARQL path, the union does not have a new scope, so path fusions must cross UNIONs when safe.
- Never rewrite inside SERVICE or nested subselects unless the logic is purely local and semantics-preserving.
- Make every transform idempotent and side-effect free (functional style via `transformChildren`).

3) Transform pipeline adjustments (IrTransforms)
- Order: collections → NPS → simple path fusions → union-based alternation extension → GRAPH coalesce → OPTIONAL graph-merge → filter reordering → property lists → zero-or-one subselect normalize → union flatten.
- Ensure every step is conservative, tests for graph-ref equality, and short-circuits where not safe.

4) Concrete fixes to address typical remaining failures
- GRAPH coalesce vs. string merge: remove reliance on `TupleExprIRRenderer.mergeAdjacentGraphBlocks(String)`. Ensure `coalesceAdjacentGraphs(IrBGP)` runs before OPTIONAL merges so the printer sees a single GRAPH body. Then drop the string helper and adjust tests if needed.
- OPTIONAL inside GRAPH: keep `mergeOptionalIntoPrecedingGraph(IrBGP)` but restrict it to a preceding GRAPH with a single simple line, and OPTIONAL bodies that are (a) simple SP/path lines or (b) a single `GRAPH ?g { simple }` optionally followed by FILTER lines. Preserve FILTER order inside the OPTIONAL (current implementation already does so; verify on failures).
- Path tail alternation from UNION: verify `fusePathPlusTailAlternationUnion` also works when the union branches are wrapped in `GRAPH ?g { … }` with identical refs (already covered; add tests if missing). Confirm it emits parenthesized alternation exactly as expected by idempotence tests.
- Path extension by constant tail triple: prefer inverse tail (object-join) when both subject- and object-joins exist; ensure no fusion when the non-bridge end is `_anon_path_*`.
- Negated Property Set (NPS): keep the transform local to GRAPH blocks when the filter and triple share the same predicate-var. Support chaining to an immediately following GRAPH with the same ref and a constant tail triple (`/(^)?p`). Do not apply global NOT IN → NPS conversions.
- Property lists: only build property lists when grouping contiguous SPs with same subject yields either multiple distinct predicates (`;`) or repeated objects for a single predicate (`,`). Preserve variable/IRI rendering and `a` for rdf:type consistently.
- Collections (RDF lists): rewrite contiguous `_anon_collection_*` chains into `( … )` in the nearest safe container; don’t attempt cross-container rewrites.
- Zero-or-one path subselect: keep `normalizeZeroOrOneSubselect` strict: match `UNION` of `FILTER(sameTerm(?s, ?o))` and a chain of constant-predicate SPs between `_anon_path_*` vars from `?s` to `?o`. Reject anything else.
- Filter reordering in OPTIONAL: move filters ahead of nested OPTIONALs only when the filter’s free vars are available from the preceding lines (already implemented via textual var extraction). Never pull filters out of OPTIONAL blocks.
- UNION/GRAPH/OPTIONAL recursion: ensure transforms recurse using `transformChildren` into BGPs of GRAPH/OPTIONAL/UNION/MINUS/SUBSELECT, but avoid SERVICE.

5) Printing layer cleanups (TupleExprIRRenderer)
- Remove or no-op `mergeAdjacentGraphBlocks(String)` once `coalesceAdjacentGraphs` covers all cases.
- Keep `IRTextPrinter` dumb: no fusions; only apply text overrides and pretty-printing.
- Keep consistent formatting: canonical whitespace, `a` for `rdf:type`, idempotent parentheses.

6) Verification strategy
- Always run formatter before tests: `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`.
- Fast loop: `mvn -o -pl core/queryrender -Dtest=TupleExprIRRendererTest#<name> test` for a single failing test.
- After each transform change, assert: (a) fixed-point rendering holds; (b) algebra string of re-parsed output equals original algebra string modulo var-renaming (`VarNameNormalizer`).
- Spot-check tricky cases by enabling `cfg.debugIR = true` in the failing test branch to print IR before/after transforms.

7) Milestones
- M1: Coalesce GRAPH in IR; delete string-level GRAPH merge; zero regressions.
- M2: Stabilize path union/tail fusion exact-parentheses to satisfy idempotence.
- M3: Tighten OPTIONAL-into-GRAPH merge to only safe/simple bodies; reorder filters within OPTIONAL bodies.
- M4: NPS chaining across adjacent GRAPHs; confirm no global NOT IN rewrites.
- M5: Collections + property lists stabilized; confirm no cross-container rewrites.

8) Non-goals / caution
- Don’t attempt semantic rewrites that change evaluation order or scope (e.g., moving FILTERs across OPTIONAL boundaries, or rewriting into SERVICE bodies).
- Don’t invent alternations or repeat steps if not explicitly derivable from local IR.
- Don’t de-duplicate lines unless a transform explicitly consumes them as part of a fusion.

9) Open questions for review
- Should we make SERVICE recursion opt-in via a config flag once core tests are green?
- Do we prefer path alternation text as `(^p)|q` vs `( ^p )|(q)` in edge cases? Confirm against tests and standardize in one place.
- Values column order: keep `valuesPreserveOrder=true` as default in tests; expose default in renderer?

Expected outcome: All TupleExprIRRendererTest cases pass with idempotent rendering, no string-level merges, and all heuristics encoded as explicit IR transforms that are conservative and repeatable.
