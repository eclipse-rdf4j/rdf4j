# RDF4J query algebra equivalence

This experimental module compares two RDF4J `TupleExpr` trees without adding an optimizer to RDF4J's standard pipeline. It is designed as a conservative gate for proposed rewrites: an optimizer may proceed only when the checker returns `EQUIVALENT`.

The checker has three outcomes:

- `EQUIVALENT` means the small proof kernel replayed structural equality or a normalization proof.
- `NOT_EQUIVALENT` means exact, non-optimized evaluation reproduced a finite counterexample twice.
- `UNKNOWN` means neither conclusion was established. This is expected for unsupported laws and opaque algebra nodes.

`CheckOptions.defaults()` selects RDF4J runtime semantics, bag observation, arbitrary incoming bindings, and proof-only checking. Generated finite-model search is deliberately disabled by default because it creates a fresh MemoryStore for each side of every attempted case. Enable it explicitly with `boundedCounterexampleSearch(true)`, or provide selected immutable `EvaluationCase` instances.

The available semantic targets are RDF4J runtime behavior, SPARQL 1.1, the current [SPARQL 1.2 W3C Working Draft](https://www.w3.org/TR/sparql12-query/), and the fragment shared by SPARQL 1.1 and draft SPARQL 1.2. Runtime Join normalization accounts for a right operand reading bindings produced by the left operand. Specification targets retain W3C Join commutativity because their operands are evaluated under the same incoming solution mapping.

Counterexamples use cloned algebra trees and a MemoryStore configured with an empty optimizer pipeline, so RDF4J cannot rewrite away the difference being tested. RDF 1.2 triple terms and directional language literals are accepted only by targets that permit them; generated models obey the selected profile.

`VerifiedRewriteEngine.apply` invokes an `AlgebraRewrite` on a detached clone, checks the result, and snapshots the accepted before/after plans. Its accessors return further clones. A custom `FunctionSafetyPolicy` must authorize purity, relocation, and duplication; deterministic return values alone are insufficient.

The proof calculus is intentionally incomplete. Operators without a checked semantic rule remain structurally opaque, and `NativeAlgebraCoverageTest` requires every concrete RDF4J tuple/value expression to be explicitly classified as supported or conservatively opaque.
