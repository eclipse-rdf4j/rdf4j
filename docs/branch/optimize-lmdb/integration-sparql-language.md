# SPARQL syntax, parsing, and model construction

This guide describes the parser-facing parts of `optimize-lmdb`. The reviewed
range is merge base `4aec7e9a2223d873b1c1a7703aad4c87bf8354df` through pinned
`HEAD` `a869fe298dc4700ce956bcaf9fece3745c57fe05`. Parser output changes are
externally observable; the notes below state the exact supported forms rather
than treating generated Java diffs as syntax changes. For query-model behavior
after parsing, see [shared structures](integration-shared-query-structures.md)
and the [query guides](README.md#native-query-feature-catalog).

## Parser stages

`SPARQLParser.parseQuery` obtains a JavaCC syntax tree, processes string
escapes/base declarations/prefixes/wildcards/blank-node labels, builds the
query algebra, applies the branch-specific `STABLE_INDEX` validation, roots
the result, and then records query form and dataset declarations. Query updates
follow a related but distinct path through `parseUpdate` and the update
sequence. This distinction is why `STABLE_INDEX` is admitted in query syntax
but rejected in updates, including a query subselect embedded inside an update.

The grammar keyword is case-insensitive and accepted with ordinary whitespace
or comments between the token and `(`. It maps only the unprefixed reserved
token to the internal URI `urn:rdf4j:stable:index-order`; prefixed local names
such as `ex:STABLE_INDEX` remain ordinary extension-function IRIs, and string
literals, IRI references, and comments are unaffected. This is enforced in
`sparql.jjt`, not by searching query text. After algebra construction the
parser accepts this special call only when it is the complete expression of
one `ORDER BY` element and it has exactly one `Var` argument. Nested uses,
non-order uses, zero or multiple arguments, and a constant argument throw
`MalformedQueryException`. `ASC(...)` and `DESC(...)` wrappers parse, but the
semantic rule is the standalone order element check in the parser visitor;
where orientation matters, the physical-order guide describes the consumer.

The syntax is an RDF4J/LMDB-specific query extension, not portable SPARQL.
Examples below are illustrative and were not executed in this documentation
task:

```sparql
SELECT ?s ?o WHERE { ?s ?p ?o }
ORDER BY STABLE_INDEX(?s)
```

The following are rejected or retain ordinary function meaning as shown:

```sparql
ORDER BY COALESCE(STABLE_INDEX(?s), 0) # nested: rejected
BIND(ex:STABLE_INDEX(?s) AS ?rank)     # ordinary prefixed extension IRI
```

Sources: [SPARQL grammar](../../../core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast/sparql.jjt),
[parser validation](../../../core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/SPARQLParser.java),
[internal algebra marker](../../../core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/LmdbIndexOrder.java).
Coverage includes new cases in
[`SPARQLParserTest`](../../../core/queryparser/sparql/src/test/java/org/eclipse/rdf4j/query/parser/sparql/SPARQLParserTest.java).

## Prefix escape expansion

`PrefixDeclProcessor` no longer creates a regular-expression `Pattern` and
`Matcher` for every QName local part. It first searches for a backslash; if
there is none, it returns the namespace plus the unchanged local substring. If
escapes exist, it appends the pieces and the character after each backslash in
a single pass. That is safe under the grammar's `PN_LOCAL_ESC` rule, which
requires each local-name backslash to introduce one of the escaped punctuation
characters. The result preserves the existing SPARQL escape expansion
contract while removing per-QName regex machinery; no throughput or allocation
measurement is claimed here. `PrefixDeclProcessorTest` covers escaped and
unescaped local names.

This optimization must remain paired with the lexer grammar. If the accepted
escape token set changes, update the parser tests and confirm the direct scanner
still handles exactly that set; a malformed backslash cannot be skipped under
the current proof.

## Expression-valued triple components

The tuple-expression builder's `toVar(ValueExpr)` used to accept variables,
constants, and unary operators. It now handles any non-null value expression:
it creates an anonymous variable, wraps the required and optional portions of
the current graph pattern in an `Extension`, adds the expression binding, then
replaces the graph-pattern required operands with that extension. The purpose
of wrapping the already-built graph pattern is to evaluate the expression in
the current pattern scope, rather than as an isolated expression over a
singleton input. Null is still rejected explicitly.

`TRIPLE(...)` component expressions are visited and mapped to variables in
subject, predicate, object order. That ordering is visible when components
contain expressions; do not reorder them as a code-cleanup. Language/direction
value forms (`LANGDIR`, `STRLANGDIR`, `HASLANG`, `HASLANGDIR`) now keep their
argument as an expression instead of forcing it through `mapValueExprToVar`.
The branch tests include `TupleExprBuilderTest`, triple-term parser/evaluation
contracts, and the shared parser suite. RDF-star algebra and execution details
are in the storage/query guides.

Source: [TupleExprBuilder](../../../core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/TupleExprBuilder.java),
[builder tests](../../../core/queryparser/sparql/src/test/java/org/eclipse/rdf4j/query/parser/sparql/TupleExprBuilderTest.java),
[triple-term parser tests](../../../core/queryparser/sparql/src/test/java/org/eclipse/rdf4j/query/parser/sparql/TestSparqlTripleTermParser.java).

## Generated JavaCC sources

`sparql.jjt` remains the grammar source of truth. The generated tree builder,
constants, and token manager are checked into source control and customized
through two ordered patches. The changed JavaCC README defines the current
workflow and CI check; read [AST generation and patch maintenance](integration-developer-tooling.md#sparql-ast-patch-maintenance)
before changing generated files. Do not edit a generated parser class as if it
were an independent source of truth, and do not attribute every generated-file
diff to a runtime grammar behavior change.

## Compatibility notes and FAQ

**Does `STABLE_INDEX` work in `INSERT`/`DELETE`?** No. The special token is
disabled while parsing update sequences. A query subselect inside an update is
still part of that update parse and remains rejected.

**Does an unknown prefixed `STABLE_INDEX` become the LMDB marker?** No. Only
the unprefixed keyword maps to the marker URI; prefixed names are expanded by
the normal prefix processor and remain ordinary function calls.

**Can a new expression be passed to `TRIPLE`?** The builder now preserves any
`ValueExpr` by binding it in the current graph-pattern extension. When changing
this path, preserve component visit order and scope, and cover expressions
inside each of the three positions rather than only variable arguments.
