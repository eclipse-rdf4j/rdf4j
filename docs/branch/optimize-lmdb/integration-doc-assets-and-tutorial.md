# Published documentation and the adjacency page tutorial

This guide maps the changed operator documentation and teaching assets to
their implementation sources. The comparison is merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` through pinned `HEAD`
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. Documentation figures and
embedded tutorial examples explain a format; they do not define or mutate the
production storage format.

## Interactive single-file tutorial

[`site/static/lmdb-adjacency-tutorial/index.html`](../../../site/static/lmdb-adjacency-tutorial/index.html)
is a standalone HTML teaching page with inline styling, scripts, fixtures,
and accessible controls. Its lessons move from a small RDF statement sample
through partition choice and logical row/fiber/context arrays into compact
columns, packed vectors, page bytes, and a fixture-derived delta overlay/read
trace. The page includes a built-in decoder and uses deliberately small
payload IDs for legibility. Its eight hand-authored statements and scale
picture are explanatory fixtures; those IDs and counts are not constraints or
promises about production assignments, page capacities, cardinality, or
lookup complexity. Consult the source-backed
[CSF format guide](storage-csf-format.md) and
[adjacency lifecycle guide](storage-adjacency-lifecycle.md) for implementation
contracts.

The tutorial's current embedded pages identify format version 4 and a
format-hash value, which its tests pin against the fixture payload. This
checks that the teaching artifact is internally consistent; it is not a
second encoder or an independent persistent-format specification. When the
format changes, the page's fixture and format labels must be reviewed against
the production encoder and reader.

Its source-level unit suite
[`lmdb-adjacency-tutorial.test.js`](../../../e2e/tests-unit/lmdb-adjacency-tutorial.test.js)
reads the embedded script and fixtures, then checks raw ID tags and role
validity, plane/orientation routing, CSR reconstruction, first/tail columns,
page header and packed block invariants, and logical-row reads. The Playwright
spec
[`lmdb-adjacency-tutorial.spec.js`](../../../e2e/tests/lmdb-adjacency-tutorial.spec.js)
loads the local file URL and exercises the statement selector, direction,
lesson navigation, term/number formatting, logical/physical view, vector
inspector, and byte highlighting. These tests were inspected as source; no
Node test, browser, page, or rendering was executed for this documentation
work.

## Operator-facing site pages

The LMDB Store page
[`lmdb-store.md`](../../../site/content/documentation/programming/lmdb-store.md)
adds background adjacency initialization, read routing during startup,
revision catch-up and publication, backlog-byte admission, synchronous versus
asynchronous publication control, memory/failure behavior, and readiness
diagnostics. The full contract, precise failure boundaries, and current-source
limitations are in [storage guides](storage-overview.md) and
[storage transactions/async writes](storage-transactions-and-async-writes.md).
The page is operator guidance, so it should remain linked to current config
defaults and not be used as evidence that a stored page is atomically swapped
with other LMDB environments.

The Server/Workbench page
[`server-workbench.md`](../../../site/content/documentation/tools/server-workbench.md)
adds the runtime-property authorization contract: the process-wide property
GET, the `rdf4j-admin` role plus exact `X-RDF4J-Admin-Request: true` header
for POST, and remote Workbench authentication requirements. The complete
wire/error contract and the distinction between a JVM property and one query's
forced strategy are in [runtime controls and Workbench](integration-runtime-controls-and-workbench.md).

## Maintenance rules for the tutorial and docs

Keep the source of truth explicit when copying examples: tutorial fixture
bytes are checked examples, storage readers/writers establish the format, and
the operator site page summarizes lifecycle/configuration. If a byte-level
lesson changes, update the embedded fixture labels and the source-level
consistency assertions together. Avoid turning a teaching scale diagram into
a performance chart or a capacity promise. The static page is a manual
developer aid; its included browser tests provide a maintenance target, not
evidence from this documentation-only pass.
