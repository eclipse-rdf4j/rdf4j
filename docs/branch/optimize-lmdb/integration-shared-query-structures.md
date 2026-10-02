# Shared query structures and contracts

This guide covers shared RDF4J APIs and model/evaluation support changed on
`optimize-lmdb`, independent of the LMDB physical plan. The historical branch
comparison is merge base `4aec7e9a2223d873b1c1a7703aad4c87bf8354df` to inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. “Changed” below means the path is
different in that range; unchanged RDF4J contracts are called out where needed
to explain compatibility. The [query guides](README.md#native-query-feature-catalog) describe
the LMDB consumers.

## Binding compatibility index

`BindingSetCompatibilityIndex` is an internal-use collection-factory contract
for finding *possible* compatible matches between mappings. It deliberately
does not promise that candidates agree on every shared variable: after looking
up a selective shared-variable posting, the join still performs the complete
compatibility check. An unbound value is compatible with either binding, while
two unequal bound values conflict. Duplicate input rows remain distinct rows
and therefore remain observable in bag-valued query results.

`DefaultBindingSetCompatibilityIndex` snapshots each inserted binding row,
assigns it a row identity, and maintains postings for its bound values and
wildcard/unbound state. A candidate lookup chooses the smallest posting for a
bound shared variable, or the all-row posting when no shared variable is bound;
it combines the exact-value and unbound postings for that variable. This is a
candidate-generation index, not a substitute for checking the rest of the
mapping. The data structure uses the `CollectionFactory` supplied by its
caller, so implementations can apply their existing memory/spill policy to
both row snapshots and posting collections. A `close()` releases those owned
collections; the factory itself remains caller-owned.

This narrows work when an input has a selective shared binding. It adds index
state proportional to rows plus row-variable postings, so an index can cost
more than a scan for small inputs or low-selectivity keys. It is intended for
operators that can amortize that index over candidate probes. Tests in
`BindingSetCompatibilityIndexTest` under the API, MapDB, and MapDB3 modules
cover equality, wildcard compatibility, duplicates, candidate filtering, and
resource closure; these links identify relevant source coverage without
reporting test execution.

Sources: [index contract](../../../core/collection-factory/api/src/main/java/org/eclipse/rdf4j/collection/factory/api/BindingSetCompatibilityIndex.java),
[default index](../../../core/collection-factory/api/src/main/java/org/eclipse/rdf4j/collection/factory/api/DefaultBindingSetCompatibilityIndex.java),
[factory contract](../../../core/collection-factory/api/src/main/java/org/eclipse/rdf4j/collection/factory/api/CollectionFactory.java),
[MapDB3 factory](../../../core/collection-factory/mapdb3/src/main/java/org/eclipse/rdf4j/collection/factory/mapdb/MapDb3CollectionFactory.java).

## VALUES rows and binding-name guarantees

The changed `BindingSetAssignment` keeps the tuple header separately from the
names that can actually appear in its rows. Its possible names are the union of
the declared names and row names; its assured names are the intersection of
names that are bound in every row. An empty explicit assignment retains its
declared header as assured names; for non-empty data, an explicitly declared
column can still be unbound in a row and is not therefore guaranteed. The
class snapshots rows lazily and publishes immutable name sets, so later
mutation of a caller-owned mutable row cannot silently alter the assignment
after its snapshot is taken. Changes to rows or declared names invalidate the
cached derived name sets.

This distinction matters to optimizer proofs: “may bind” supports candidate
planning, while “will bind” supports transformations that depend on a value
being present. Do not use `getBindingNames()` where a guarantee is required.
The model tests are in
[`BindingSetAssignmentTest`](../../../core/queryalgebra/model/src/test/java/org/eclipse/rdf4j/query/algebra/BindingSetAssignmentTest.java).
The deeper join-order and VALUES routing consequences are covered in
[joins and factorization](query-joins-and-factors.md) and
[optimizer/statistics](query-optimizer-and-statistics.md).

## Function safety and value ordering

The `Function` contract now exposes determinism information used by safe
optimizer reasoning. Its default is conservative: extension functions must not
be treated as repeatable merely because their URI looks familiar. The function
registry and query-safety analysis account for registered functions and
unknown/unmarked implementations; this is why adding a function or changing
its determinism declaration can affect whether an expression may be moved,
re-evaluated, or used for ordered optimization. GeoSPARQL implementations
participate through explicit declarations. A declaration is a semantic promise
to the optimizer, not a speed flag.

`ValueComparator` changes add transitivity coverage and compare paths for RDF
values. Ordered consumers depend on comparator consistency: an ordering that
contradicts itself can break sort, merge, seek, and order-sensitive joins even
when pairwise comparisons look plausible. The changed
`ValueComparatorTransitivityTest` and function-safety tests are the regression
map. See [expression semantics](query-expression-semantics.md) for query-visible
error and expression rules.

## Model-side memory behavior

`DynamicModel` uses a canonical-value map in its map-backed phase. Statements
added to that phase are rebuilt only when needed and repeated equal values share
one canonical object, including recursively nested RDF-star triple terms.
That trades one hash-map entry per retained distinct value plus its table
overhead for avoiding duplicate equal term objects across statements. Deletion
records weighted “removal debt”; when debt reaches the larger of 1,024 removed
value units or half the current canonical-map size, the canonical map is
rebuilt from remaining statements. Clearing the model drops the cache. When the
model has promoted to its other backing implementation, it drops this cache;
deserialization rebuilds it only for the map-backed form. This gives deleted
terms a bounded cleanup path without scanning all surviving statements after
every removal. It is an implementation trade-off; no allocation or throughput
measurement is asserted.

`CoreDatatype.from(String)` adds a URI-string lookup for built-in RDF, GEO, and
XSD datatypes. It returns `CoreDatatype.NONE` for `null` or an unrecognized
string. The helper lazily constructs an unmodifiable reverse-lookup map. The
existing IRI-based lookup remains available. See
[the model tests](../../../core/model/src/test/java/org/eclipse/rdf4j/model/impl/DynamicModelTest.java)
and [datatype API](../../../core/model-api/src/main/java/org/eclipse/rdf4j/model/base/CoreDatatype.java).

## Query-model metadata and reified triples

The branch adds the internal `LmdbIndexOrder` sentinel used by the parser and
LMDB evaluator to carry `STABLE_INDEX(?var)` as an order annotation. It is not
an extension-function API for arbitrary consumers. The parser validates that
the call is the standalone expression of an `ORDER BY` element and has exactly
one variable argument; other forms fail parsing as a malformed query. The
ordering promise is store-specific; see [SPARQL language](integration-sparql-language.md)
and [physical access](query-physical-access.md).

`ReifiedTripleRef` no longer adds its optional reification variable to the
assured-binding set merely because that slot exists. Presence in the algebra
node does not prove the store can supply that binding for each solution. The
reified-triple tests pin this distinction. Query-model nodes also gain generic
metadata hooks used to carry transient strategy-decision information into
explanations; this metadata is not an optimizer cache key. See
[explanation and telemetry](integration-plan-explanation-and-telemetry.md).

## Extending safely

When introducing a new mapping operator, state separately which names it may
bind and which it guarantees, preserve duplicate rows unless the algebra
operator removes them, and make candidate indexes return a superset that the
caller can fully verify. If an optimization will re-evaluate an operand, first
establish that its functions and sources are repeatable and that re-evaluation
does not change error visibility. These contracts are consumed jointly by
shared evaluation and LMDB-specific operators; see [shared evaluator semantics](integration-query-evaluation.md)
and [query memory/lifecycle](query-memory-and-result-lifecycle.md).
