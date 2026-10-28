# Native ID rows for zero-length property paths

The native LMDB query engine can enumerate zero-length path graph nodes as IDs, without eagerly creating RDF Values. This is an experimental, default-enabled execution option, not a new SPARQL feature or a COUNT-specific rewrite.

## Default and explicit disable

The producer is enabled by default. To retain the original producer, start the JVM with:

```text
-Drdf4j.lmdb.nativeZeroLengthNodeIds.enabled=false
```

The native query engine must also be enabled in the store's existing configuration. The property alone does not enable that engine or allocate a direct-adjacency index. An unset property enables the ID-row producer. Explicit `false`, an empty value and unrecognized values retain the original producer; Boolean parsing accepts case-insensitive `true`. The property is sampled when an unbound node cursor opens. Changing it later affects new cursors, not an already-open cursor's algorithm or resource ownership. Prefer setting it at JVM startup; it is process-wide rather than per-repository configuration.

Remove the property or set it to `true` to re-enable the producer. No store migration, persistent format change or data rebuild is required to change this setting.

## Physical admission and fallback

With both endpoints free, exact retained adjacency subject/object domains can produce the graph-node union. Each subject appears once, followed by object IDs absent from the subject domain. Literal objects are graph nodes; dictionary-only, predicate-only and graph-name-only terms are not.

This domain route requires canonical stored term IDs, unrestricted graph scope, no encounter-order requirement, and complete snapshot-valid subject and object domains under the same retained probe. The existing source capability decides whether its current mode, coverage, pending writes, maintenance state and retained snapshot can supply these domains. A missing domain means fallback, never an empty graph. The operator does not force an adjacency strategy or create its own query-result cache.

For scoped GRAPH/datasets, unavailable domains or order-sensitive consumers, the original statement scanner retains its scope, order and ID deduplication. If stored IDs are canonical, only its redundant RDF-term set/materialization is omitted. Legacy/noncanonical dictionaries retain the original Value-level equality fallback. Synthetic wrappers are unwrapped only to classify IDs emitted by the physical store, not arbitrary plan/runtime/bound values.

Bound endpoint identity handling is unchanged, including same-variable and dictionary-absent terms. Existing one-step edge execution, outer DISTINCT, projections, subqueries, joins, filters and aggregate strategies remain in charge. This change does not fuse operators, factorize rows or add direct batch execution.

## Lifetime, ordering and diagnostics

A successful domain cursor owns its retained probe until exhaustion or close. It rolls back its row bindings on advance/close, closes once, and polls query cancellation while skipping overlapping nodes. Failed domain acquisition closes the untransferred probe. As with other RowCursor implementations, the owning execution operator closes the cursor after an iteration exception. Snapshots, inferred visibility and pending-write admission are delegated to the existing exact source interfaces.

Unordered result enumeration may differ from the statement scan. Consequently `LIMIT` without `ORDER BY` may select a different valid subset. Consumers requiring encounter order retain the scanner.

Package-private cumulative counters on `ZeroLengthPathPlan` expose domain opens, emitted node rows and canonical scan opens for tests/diagnostics. Domain row counts are committed at exhaustion/close, avoiding an atomic operation per row. Counters are process-wide and do not themselves prove result correctness or provide per-query isolation under concurrency.

## Costs and rollout limits

This removes producer-level Value materialization and redundant ID/term sets when the domain route serves. It still emits normal rows and retains required downstream DISTINCT. SELECT output and value-sensitive filters must materialize Values. Earlier measurements found SELECT faster but allocating about 3–5% more because cold Values reach the existing repository exposure path; this option does not change that lifetime-sensitive subsystem. Retained adjacency summary memory and cold construction cost also remain relevant.

The option is enabled by default, with explicit disable retained for comparisons and rollback. Upstream review, representative workload validation and broader release checks remain necessary. Exact measurements and local test evidence belong in the accompanying implementation report, not as universal performance guarantees here. A pre-existing native binding-origin bug can produce an invalid identity row for a dictionary-absent runtime VALUES binding. Regression tests characterize the unchanged behavior with this option disabled, explicitly enabled and unset; the separate semantic fix is not part of this producer change.
