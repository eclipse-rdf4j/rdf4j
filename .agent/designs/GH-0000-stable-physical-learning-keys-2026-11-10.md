# Physical and event learning keys that survive a re-parse (spec, 2026-11-10)

Root cause analysis: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/rca-physical-and-keys/NOTES.md` (question 2)
and the red scratch test `RcaPhysicalAndKeysScratchTest#parserGeneratedNamesDoNotChangePhysicalLearningKeys` beside it.
Measurement: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/measure-validity/summary.txt`, D.3(a).
Packed = `core/queryalgebra/evaluation/.../optimizer/cascades/packed/`; MFLS = `LmdbMappedFrontierLearningSession`.

## Defect
The SPARQL parser names HAVING aggregates, path intermediates and a few other internals with a JVM-wide counter plus
a per-JVM random UUID (`_anon_having_…`, `_anon_path_…`, `_anon_path_inverse_…`, `_anon_…`, and the builder's
`_anon_collection_…`/`_anon_bnode_…`). Two learning identities embed those raw names:
- the physical residual key's layout field: `MFLS.layoutFingerprint` hashes the sorted raw
  `query.outputBindingNames(relation)`;
- the event key: its source is `PackedQueryView.tryPhysicalSourceIdentity` → `appendPackedIdentity(query, sink)` over
  the planning query, which carries raw names, and its frames write raw names through `appendPhysicalMask`.
So every parse of such a query (33 of 182 in the theme workload) creates physical and event cells that no later
planning reads, what was learned is never reused, and the cells crowd the shared 4,096-key map (cap reached in round
4, evicting cells that held observations). Logical keys are alpha-normalised and unaffected.

## Required behaviour
Two preparations of the same query text produce equal logical keys, equal (logical, physical, applicability) keys and
equal event keys, in the same JVM and across JVMs, whenever the query's identity surface is safe to canonicalise
(the rule the plan caches already use: `PackedQueryCacheIdentity.IdentitySurface.safeToCanonicalize`). Where it is not
safe (a SERVICE boundary, an anonymous name visible at the root or in VALUES) names stay exact, as they do in the
plan identity.

## Design
One canonicalisation, the existing one. Do not invent a second.
1. **Carry the canonical ordinals with the planning query.** The cache-identity encoding
   (`PackedQueryCodec.encodeForCacheIdentity(source, canonicalNames, …)`) replaces each canonicalisable name object by
   `CanonicalAnonymousName(ordinal)`. The planning encoding keeps raw names. At planning-encode time record, for every
   object of the planning query that the identity encoding would have replaced, its ordinal (a side array indexed by
   object id, −1 otherwise; no change to the objects themselves, to relation/scalar/mask ids or to anything execution
   reads). It must survive every way a planning query is produced in `PackedCascadesPlanner` (fresh encode,
   `familyIdentity.concreteQuery()`, `PackedQueryCodec.rebindOriginalBindingSets` of a cached older parse) and it must
   describe the names that query object actually holds. Compute the identity surface once per source; reuse the one
   `createPlanIdentity` already collects if that avoids a second tree visit.
2. **Physical source and frames write the canonical token.** In the stream `appendPhysicalSource` produces and in
   `appendPhysicalMask`, a name with an ordinal is written as the same token the identity encoding writes for
   `CanonicalAnonymousName` (`"anonymous-name"`, ordinal); every other name and every other object is written exactly
   as today. Prove injectivity from the sink's token encoding: a canonical token must not be producible by any
   sequence of string and number tokens at the same position (if the existing tagging does not give that, add a
   dedicated token kind to the physical sink).
3. **Layout fingerprint.** `MFLS.layoutFingerprint` folds exact names exactly as today (same sort, same mixing, so a
   layout without canonical names keeps its value) and then folds canonical names in ordinal order under a
   domain-separating constant. Expose what it needs from `PackedQueryView` without materialising name lists per call
   beyond what `outputBindingNames` already does.
4. **Join-basis tie order** (`MFLS.joinBasis`, about lines 985–994, orders factors by raw name): determine whether a
   parser-generated name can decide the order and thereby change a logical key between parses. If it can, break the
   tie on the canonical token instead and pin it with a test; if it cannot, say why in one sentence in the report.

## Compatibility
- Keys that involve no canonicalisable name are byte-identical before and after (physical key strings, event sources,
  frames, lifecycle keys, sidecar bytes). Pin this with a test on a query without internal names: the three key sets
  and the persisted sidecar section are equal on the pristine base and the changed copy (golden values captured from
  the base, or a two-copy comparison in the evidence).
- Keys that do involve one change value. The old ones were orphans after any re-parse or restart already; no sidecar
  version bump, no migration.
- The plan identity, the plan caches and execution are untouched.

## Tests (Routine A: red first, Surefire snippets in the evidence)
New class `LmdbPhysicalLearningKeyStabilityTest` in `core/sail/lmdb`, store-level through the public API, plan caches
disabled where a test needs two fresh plannings (`setFrontierCacheEvidenceBudgetBytes(0L)`, as the scratch test does):
1. HAVING query, two parses: precondition the `_anon_having_` names differ; equal logical keys (green today), equal
   physical key sets, equal event key sets (red today).
2. Property-path sequence (`?s :p/:q ?x`), two parses: equal physical key sets including the layout field (red today).
3. One of the other unstable families that reaches a planning (`_anon_` from an ORDER BY aggregate, or a collection):
   equal key sets. If none reaches a key, say which you tried.
4. Cache-on path: with the default caches, the second preparation is served through the cached older parse; its
   contracts' keys equal the first preparation's.
5. Unsafe surface: a query whose anonymous name is visible at the root (or a SERVICE query if constructible without a
   network) keeps exact names in its keys: key sets differ between parses exactly as today (characterisation).
6. Injectivity: two queries that differ only in which internal variable a relation outputs produce different layout
   fields and different event keys; a user variable literally named `anonymous-name` does not collide with a
   canonical token.
7. Learning actually accumulates: evaluate the HAVING query to completion in two separate preparations; a physical or
   event cell written by the first holds its observation when the second resolves its target (same cell object or
   same observation count through an existing package-private posterior accessor).
8. Compatibility pin from the section above.
Core-level unit tests beside the packed classes for the side array (survives rebind and concretisation; −1 for exact
names; equal across two parses) and for the sink token.

Each red-today test must also go red under a one-line mutation of its part of the fix.

## Constraints
- Do not raise architecture size limits; if `PackedQueryView` or another class is at its limit, put the new code in
  the class that owns the concern (the identity class) and say so.
- No new public API beyond what the LMDB module needs from the packed package (keep additions package-private or
  on the existing internal view).
- Private copy only; scoped formatting; new Java files get the exact RDF4J header and
  `// Some portions generated by Claude`; nothing committed.

## Deliverables
`changed-files.txt`, cumulative `changes.diff`, `evidence.txt` (red and green snippets, mutation outcomes); three
consecutive green runs of the new and touched test classes; one run of `core/queryalgebra/evaluation` packed tests
(`rg -l "PackedQueryCacheIdentity|PackedQueryView|tryPhysicalSourceIdentity|PhysicalEventKey" --glob '*Test.java'`
in both modules) compared against the same selection on the pristine base copy; a list of anything in this spec the
code contradicted.
