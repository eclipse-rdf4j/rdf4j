# Borrowed adjacency root factors

Enabled by default for eligible regions within Packed F-trees. Opt out with `-Drdf4j.lmdb.packedFtree.rootFactor.enabled=false`. Default execution routing and IR selection are unchanged; this switch does not force Packed F-trees.

A correlated single-pattern region with a fixed predicate/key and one unbound endpoint can retain the existing source-owned adjacency descriptor instead of constructing packed scalar root lanes. Logical singleton runs copy the value inline. Multi-value runs retain a codec-aware `BorrowedFactorBatch` until demanded by a consumer; a source lease protects the immutable snapshot. Context multiplicities are preserved. No raw unowned address or new storage format is introduced.

This narrow path requires revision-valid adjacency entry metadata. Unknown metadata declines before creating a speculative probe. Filtered, context-constrained, ordered/range, repeated-slot and general multi-pattern shapes retain existing execution. Capability decline is never logical absence. Each cursor/probe/source and decoder remains worker-local; this change does not add parallel execution.

Runtime explain telemetry reports root-run descriptor exports separately from generic factorization: logical rows represented, descriptor kinds, singleton handling and capability declines. These are export/representation counters, not decoded or consumed rows, and are accumulated without publishing the physical plan per run.

Focused tests cover exact bags, OPTIONAL absence, both directions, singleton handling, deferred reads, metadata/export fallback, cancellation, early close, snapshot changes and telemetry. Run the repository's required root quick install first, then the LMDB tests `LmdbNativeRootRunFactorTest`, `LmdbNativeRootRunSnapshotTest`, `LmdbNativePackedFtreeTest` and `LmdbNativeExplainTest` (no `-am` for tests).

## Measurement scope

The original prototype was measured on `5c18f7534ee9f53af72aa8f47e04947f52fb139f`, not this rebased publication tree. It combined deferred payloads with avoiding planner/runtime/chunk setup. Forced Packed F-tree comparisons found lower allocation and latency for eligible single-pattern OPTIONAL and a ten-layer OPTIONAL/UNION workload. Default controls stayed on IR; deep fixed-predicate OPTIONAL did not show a consistent latency improvement. The original unsupported-adjacency fallback incurred extra allocation; this publication adds the conservative metadata guard above.

The current-base guard and observability changes require their own performance measurements before any new speedup claim or cost-model promotion. Packed/IR routing and the cost model are unchanged; only eligible root-run borrowing within Packed is default-enabled.
