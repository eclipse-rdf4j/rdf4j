# RDF4J query algebra equivalence

This experimental module compares two RDF4J `TupleExpr` trees without adding an optimizer to RDF4J's standard pipeline. It is designed as a conservative gate for proposed rewrites: an optimizer may proceed only when the checker returns `EQUIVALENT`.

The checker has three outcomes:

- `EQUIVALENT` means the small proof kernel accepted a versioned exact-tree certificate. Structural equality is
  encoded as a zero-step normalization certificate and passes through the same mandatory gate.
- `NOT_EQUIVALENT` means a target-tagged oracle reproduced a finite counterexample. RDF4J-runtime witnesses use exact,
  non-optimized evaluation and are replayed twice.
- `UNKNOWN` means neither conclusion was established. This is expected for unsupported laws and opaque algebra nodes.

`CheckOptions.defaults()` selects RDF4J runtime semantics, bag observation, arbitrary incoming bindings, and proof-only checking. Generated finite-model search is deliberately disabled by default because it creates a fresh MemoryStore for each side of every attempted case. Enable it explicitly with `boundedCounterexampleSearch(true)`, or provide selected immutable `EvaluationCase` instances.

The available semantic targets are RDF4J runtime behavior, the dated [SPARQL 1.1 Recommendation](https://www.w3.org/TR/2013/REC-sparql11-query-20130321/), the dated [5 June 2026 SPARQL 1.2 Working Draft](https://www.w3.org/TR/2026/WD-sparql12-query-20260605/), and the fragment shared by those two specifications. Runtime Join normalization accounts for a right operand reading bindings produced by the left operand. Specification targets retain W3C Join commutativity because their operands are evaluated under the same incoming solution mapping.

Runtime counterexamples use cloned algebra trees and a MemoryStore configured with an empty optimizer pipeline, so
RDF4J cannot rewrite away the difference being tested. The checker never treats RDF4J evaluation as a W3C
counterexample oracle: when no specification-level witness producer is available, it returns `UNKNOWN`. RDF 1.2 triple
terms and directional language literals are accepted only by targets that permit them; generated models obey the
selected profile.

`VerifiedRewriteEngine.apply` invokes an `AlgebraRewrite` on a detached clone, checks the result, and snapshots the accepted before/after plans. Its accessors return further clones. A custom `FunctionSafetyPolicy` must authorize purity, relocation, and duplication; deterministic return values alone are insufficient.

The proof calculus is intentionally incomplete. Operators without a checked semantic rule remain structurally opaque, and `NativeAlgebraCoverageTest` requires every concrete RDF4J tuple/value expression to be explicitly classified as supported or conservatively opaque.

## Scope-safety modes and their cost

The `org.eclipse.rdf4j.query.scopeSafety.mode` system property selects how the optimizer pipeline
uses this machinery: `OFF` (default, legacy pipeline only), `AUDIT` (legacy plans, fail-fast
cross-check of the scope analysis — a diagnostic/canary mode), `ENFORCE`, and `SHADOW` (legacy
results always returned; a sampled fraction of queries additionally runs a scope-safe candidate
plan and divergences are recorded as telemetry; set `shadowStrict=true` to make them fatal in CI).

`ENFORCE` runs ONLY scope-safe rewrites. Legacy optimizers without a scope-safe replacement are
skipped — notably join reordering (`QueryJoinOptimizer`) and query-model normalization — so a
query written with a poorly ordered basic graph pattern executes in written order and can be
dramatically slower. ENFORCE trades optimization for provable scope safety and is not a
general-purpose production configuration.
