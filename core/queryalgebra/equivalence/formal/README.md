# RDF4J algebra-equivalence formal model

This Lake project is pinned by `lean-toolchain` to Lean 4.32.0 and imports only Lean's distributed standard library.

Run:

```sh
lake build
rg -n '\\b(sorry|admit|axiom)\\b' .
```

Current status: **not certified while independent-review gates remain open**. The model defines terms,
extensional executable mappings, bag/set observations, sequences, ASK, collapsed failures, datasets, incoming mappings,
pure/impure functions, the supported algebra shape, and four target relations. It proves the generic checked-certificate
composition theorem plus expression-level UNION, DISTINCT, VALUES, FILTER, JOIN, LEFT JOIN, MINUS, and REDUCED laws,
and records each exported theorem's standard Lean axiom inventory (drawn only from `propext`, `Classical.choice`, and
`Quot.sound`). The executable theorem registry deliberately narrows the sound-but-incomplete checker: unproved reorder,
distribution, projection, and error-sensitive rules return `UNKNOWN`. The 1,024-cell matrix contains 347 theorem cells
and 677 reviewed inapplicability cells, with no pending cells.

The four elision rules (`LEFT_JOIN_EMPTY_LEFT`, `LEFT_JOIN_EMPTY_RIGHT`, `LEFT_JOIN_UNIT_RIGHT`, `MINUS_EMPTY_LEFT`)
discard a condition or an operand. `RDF4J_RUNTIME` prepares a condition once, before either input is evaluated, so
the model gives that target a preparation phase (`conditionPrepares`) and the corresponding theorems carry a
`ConditionPrepares`/`RuntimeOperandTotal` premise. The executable kernel discharges those premises syntactically —
an absent condition has nothing to precompile, and a `.empty`/`.unit` operand cannot fail — so the rules stay
available at runtime exactly when the elision is observationally free.

`check-certificates` independently decodes version-2 Java JSON-lines certificates, reconstructs exact source trees,
enforces the theorem-backed root-local step boundary, evaluates both endpoints over the finite target oracle, and
rejects malformed, extra, or nested steps. It streams input batches so the full release campaigns do not have to fit in
memory.

No output from this project may be described as a semantic certification until the executable accepts the Java
certificate corpus independently and every remaining campaign/sign-off gate is closed.
