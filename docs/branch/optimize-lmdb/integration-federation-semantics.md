# Federated `SERVICE` semantics and batching

This branch changes shared repository-side federation in addition to LMDB
evaluation. The historical source boundary is merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` to inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. This is a semantic compatibility
guide: it explains when a remote request may be split, and how failure and
bag multiplicity are preserved. The LMDB-specific correlated-provider path is
documented separately in [query strategy guides](README.md#native-query-feature-catalog).

## One logical invocation by default

For constant-IRI `SERVICE`, the evaluator treats the service pattern as one
logical invocation whose result is a multiset. Splitting a block of incoming
bindings across several remote requests can change observable results: remote
state may change between requests, fresh values such as UUIDs or blank nodes
may differ, failures can occur after only some blocks completed, and a
`SERVICE SILENT` failure cannot legally expose a prefix of the successful
rows. Therefore `RepositoryFederatedService` defaults
`partitionToleranceDeclared` to false.

When the whole input fits in one generated request, bindings can be pushed as a
single `VALUES` table. Otherwise the evaluator sends the original service
pattern once and joins those remote rows locally with the input mappings. That
join uses compatible-mapping merge: two mappings that bind a shared variable
to different values do not combine; an unbound variable is compatible; and
duplicate input/remote rows preserve multiplicity. This can use more client
memory and network bytes than partitioned calls because one response is
buffered/combined against the input set, but it avoids inventing a multi-request
behavior for one service invocation.

Subqueries in the service pattern also disable injected VALUES by default.
Pushing outer bindings across the subquery boundary may affect aggregate scope,
and injecting VALUES before a subselect can constrain solutions before its
`ORDER` or `LIMIT`. The evaluator therefore makes one request for the original
service expression and joins locally when it detects a subquery.

## Explicit row-correlated extension

The documented opt-in convention is to project `?__rowIdx` in the service
query to request row-correlated vectored evaluation. In that case the
evaluator attaches a correlation index to the pushed values and matches
returned rows to their input mapping. The current detector is a string
heuristic—`service.getServiceExpressionString().contains("?" + ROW_IDX_VAR)`—
not a projection or AST check. A textual occurrence in another part of the
service expression can therefore activate this path even when the variable is
not projected. Treat this as an implementation convention, not enforced
projection syntax. It is an RDF4J federation extension, not a claim that the
behavior is equivalent to one SPARQL constant-IRI `SERVICE` invocation.
The implementation chooses a fresh synthetic row-index variable unless the
user-selected variable is the actual opt-in; it checks service variables,
input bindings, and the serialized service pattern text for collisions.
Malformed generated-query recovery does not silently turn an ordinary
`SERVICE` into several per-binding calls: it falls back to the one-invocation
local join. Under explicit row-index opt-in, per-binding correlated
evaluation is the documented fallback.

Partitioned requests are also available to an endpoint whose operator has
explicitly declared it partition tolerant through
`setPartitionToleranceDeclared(true)`. This is an affirmative deployment
promise: state/results must be stable during the query, blank-node and volatile
function behavior must be equivalent across requests, and failure and SILENT
handling must be all-or-nothing-equivalent. A deterministic query string alone
does not prove those endpoint properties. Enabling the declaration can reduce
the size of each request and allow chunked remote work, while increasing
request count and changing the operational contract. Do not turn it on by
default for an arbitrary SPARQL endpoint.

## `SILENT` is atomic at the invocation boundary

For a `SERVICE SILENT` call, the evaluator drains and buffers the complete
remote response before exposing any mapping. If draining or closing throws a
`RuntimeException`, it discards all buffered partial rows and substitutes the
input mappings (the singleton empty mapping for the single-binding path); the
outer join then passes each input mapping through unchanged. `Error` is not
caught by this buffering boundary and is not converted to the SILENT result.
Non-silent SERVICE keeps normal error propagation. This buffering raises peak
memory in proportion to the response multiset, but is necessary because
streaming a prefix and then recovering from a later failure would produce a
result no single failed SILENT invocation can have. Close paths release the
remote iteration whether the buffer completes or fails.

Sources: [repository federation evaluator](../../../core/repository/sparql/src/main/java/org/eclipse/rdf4j/repository/sparql/federation/RepositoryFederatedService.java),
[service join conversion](../../../core/repository/sparql/src/main/java/org/eclipse/rdf4j/repository/sparql/federation/ServiceJoinConversionIteration.java),
[evaluation service iterator](../../../core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/federation/ServiceJoinIterator.java),
and the [W3C SPARQL 1.1 Federated Query specification](https://www.w3.org/TR/sparql11-federated-query/).
`RepositoryFederatedServiceTest` is the direct test entry point, supplemented
by compliance/service cases and query parser/evaluator tests. These links
identify source coverage; they do not imply a passing test result.

## Review checklist for a federation change

Ask whether the transformation still represents one remote invocation, whether
the endpoint can change during it, where volatile values are generated, how
partial failure is handled, whether `SILENT` must discard rows, and whether
subquery limits/aggregates are crossed by injected bindings. Test duplicate
input mappings and duplicate remote rows. For query cancellation while remote
work is active, follow the ownership path in [query lifecycle](integration-query-lifecycle.md).
