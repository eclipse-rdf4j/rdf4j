# Direct-adjacency storage lifecycle

This guide describes the direct-adjacency subsystem at pinned branch `a869fe298dc4700ce956bcaf9fece3745c57fe05`. It covers how the derived in-memory index is built, updated and retained. It does not describe the query planner's routing decisions; see the sibling query guides for those consumers. The triple store remains authoritative throughout.

**Branch delta and prerequisites:** the branch adds direct-adjacency capture, paged-CSF base construction, delta/run overlays, immutable generation publication, leases, consolidation and recovery to the existing LMDB statement store. The existing statement indexes remain the source for startup/rebuild scans and the fallback authority; this lifecycle guide describes when newly added derived state may claim a complete snapshot.

## What the index represents

The current base is an immutable, paged CSF index. There is no legacy native-arena/CSR base implementation selected at runtime: the four CSF planes are the base representation, while the run codecs described below encode committed changes and composite row sources. All of these structures are derived from the statement indexes and are rebuilt from them after a gap or restart. They are not part of the durable repository format. In particular, `PERSISTENT_COMPOSITE_VERSION` is a version for an in-memory run-graph representation, not a second set of repository files. The separate page header version belongs to the CSF page codec; it is not `LmdbStore.VERSION`.

The four planes are outgoing-explicit, outgoing-inferred, incoming-explicit and incoming-inferred. Each plane groups rows by predicate and root key: subject for outgoing, object for incoming. Neighbors are objects in outgoing planes and subjects in incoming planes. Raw context IDs, with zero denoting the default context, participate in the row's `(neighbor, context)` ordering. A context catalog maps raw IDs to compact ordinals for storage but retains the raw pair order. [`LmdbAdjacencyPlane`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyPlane.java), [`LmdbAdjacencyTripleStoreScanner`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyTripleStoreScanner.java), and the [CSF layout guide](storage-csf-format.md) define the storage representation.

`FULL` coverage allows all predicates to be represented, subject to plane support. `SELECTED` coverage means the configured predicate set is represented; it requires at least one selected IRI. A predicate introduced after a selected-coverage base can be classified as selected in a later delta generation, while an unselected predicate still lacks a coverage proof. The store must not turn absent-from-base into absent-from-repository unless coverage says that predicate's full domain was captured.

## Build, catch-up, and cutover

```mermaid
sequenceDiagram
  participant W as Writers
  participant T as TripleStore snapshot
  participant B as CSF base builder
  participant Q as Captured delta queue
  participant P as PublishedState
  W->>T: commit statements at revision r
  W->>Q: capture row replacements / deletes
  B->>T: scan a pinned base revision b
  B->>P: publish BUILDING/unavailable metadata
  loop contiguous revisions b+1 ... target
    Q->>B: sealed commit deltas
    B->>B: apply in revision order
  end
  B->>W: close write admission for cutover
  W-->>B: finish already-admitted handoffs
  B->>T: fence and verify exact target revision
  B->>P: atomically publish base + generations + cleared gap
  B-->>W: reopen writes; subsequent commits update synchronously
```

`triggerBuild()` schedules local maintenance and returns without waiting for the full build. A build takes a statement snapshot, scans the required planes and captures writes that commit while scanning. It applies only contiguous sealed revisions after the scanned base. At cutover it closes new write admission, lets already-admitted writers finish handoff without holding the physical-commit fence they need, then fences commits long enough to check the final target and publish one immutable state. If the observed gap sequence or revision continuity changed, that candidate is discarded and recovery is retried instead of exposing a mixed state.

There is one startup distinction. For a truly empty store, `triggerBuild()` checks emptiness under the transaction manager's read lock and enables synchronous update publication immediately. This makes the first inserts wait for their adjacency publication. A populated store does not enable that barrier until its scan and catch-up reach the exact cutover; the cutover's first post-release commit is the first synchronous update. This avoids making imported/reopened data look ready merely because background work was scheduled. The existing [LMDB store operations page](../../../site/content/documentation/programming/lmdb-store.md) describes this behavior; source anchors are [`triggerBuild()` and cutover in `LmdbDirectAdjacencyStore`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyStore.java#L2620) and the [startup concurrency contract source](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyStartupConcurrencyTest.java).

Build failure, memory refusal, incomplete selected coverage, an unsupported requested plane, an old snapshot below the currently retained floor, a pending row touched by that snapshot, or a recorded gap prevents the direct representation from claiming a complete answer. Those are derived-index states; the corresponding persistent LMDB statement indexes remain available to the caller. `SHADOW` builds and can sample comparisons but does not answer the request. `PREFER` serves only shapes with the required coverage and snapshot proof, otherwise the query layer falls back. `DISABLED` does not schedule the index.

## Commit capture and immutable publication

The triple-store commit listener brackets the revision bump. Before the new revision becomes visible, the adjacency bridge publishes a pending row table (or a gap marker) for readers that could observe the commit revision. After authoritative commit success, it seals the per-transaction delta and queues it for maintenance application. Maintenance publishes the applied row replacement as an immutable delta generation. A no-op statement delta can still advance the applied revision. If sealing, capture, queue handoff or application cannot preserve continuity, the store marks the first affected revision as a gap and schedules a quiescent rebuild. It does not attempt to fill a missing revision by combining old and new generations.

`LmdbAdjacencyPublishedState` is the atomic publication unit: one base, one overlay set, sorted pending tables, an applied revision, gap start, serving state, and a minimum snapshot revision. Readers retain a `LmdbAdjacencyReadView` over one such object. A read view is usable only when the requested snapshot is at or above `minSnapshotRevision`, at or below the state's horizon, and has no gap at or before that snapshot. A pending table at a reader's revision causes row-specific fallback if it touches that row. Thus a state can serve unaffected rows while withholding a row changed by a not-yet-applied commit.

Publication uses reference counting for the state, base, overlay set, contexts and backing arenas. A publisher retains shared owners before swapping the atomic state; a read view retains the chosen state; the last release closes its owned objects. Consolidation may raise `minSnapshotRevision` because it retains only newest versions in the replacement, but already-retained older states continue serving their own snapshots until those leases close. A newly acquired snapshot below the new floor declines. This is why memory can remain charged after a replacement is visible: old readers may still own the old base or generation.

The current edge-run codec uses a tag byte plus one of: `SMALL_VARINT` for 1–15 incidences, `BLOCK_FOR` for ordinary runs through 1,048,575 incidences (and the 64 MiB run limit), or `CHUNK_DIRECTORY` for larger rows. Chunks target at most 65,536 edges and 64 MiB. Composite runs can refer to base-CSF slices or delta run slices; the current persistent-composite graph uses fan-out 64. The word “persistent” here names a run-graph codec version retained in the in-process arena/catalog model: the CSF pages and run bytes are not written into `values/` or `triples/`. See [`LmdbAdjacencyRunCodec`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyRunCodec.java), [`LmdbAdjacencyDeltaGeneration`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyDeltaGeneration.java), and [`LmdbAdjacencyOverlaySet`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyOverlaySet.java).

## Consolidation, gaps, and capability-local failure

Maintenance can consolidate generations or rebuild the base. Consolidation merges visible row versions into a replacement source graph, publishes the new owner atomically, and retires replaced arenas only when no published state or read lease still refers to them. Generation-count policy can trigger maintenance; it does not make a generation's rows durable. Quiescent rebuild closes admission at cutover, rescans authoritative indexes, catches up and either publishes a gap-free state or leaves the accelerator unavailable. A later query cannot heal an unprovable revision by reading from stale CSF.

The node-to-predicate projection is an optional capability attached to the base, distinct from direct adjacency's core row representation. At construction, `rdf4j.lmdb.directAdjacency.nodePredicateProjection.enabled` chooses whether to build the outgoing projection; incoming projection defaults off and is only enabled when the outgoing projection is enabled. An inconsistency disables this projection, logs the condition, and schedules a rebuild. That failure is contained to bound-node/unbound-predicate enumeration: other adjacency rows remain independently servable, and the query path falls back for the declined projection shape. A runtime kill switch, `rdf4j.lmdb.directAdjacency.nodePredicateProjection.serve.enabled`, is deliberately re-read on each serving call; setting it false makes consumers decline immediately without waiting for rebuild. It does not remove the projection from memory.

`failOnMaintenanceError=false` is the default. Ordinary derived-maintenance failures therefore leave the authoritative store open and permit its fallback behavior; setting it true causes certain unexpected maintenance failures to be surfaced by guarded paths. This is not a general durability switch and does not make adjacency part of a transaction. A capacity refusal has its own `MEMORY_REFUSED` path; it keeps the LMDB store usable and is not an OOME recovery guarantee. The current source's capability-local behavior and recovery scheduling are in [`onNodePredicateInconsistency()` and `nodePredicateDecline()`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyStore.java#L4300) and [`LmdbNodePredicateRowCursor`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNodePredicateRowCursor.java).

## Memory, admission, and runtime controls

The adjacency byte limit covers allocations charged through `LmdbAdjacencyMemoryAccount`; it is not a process-RSS, JVM-heap, or OS-resident-page limit. Reservations happen before tracked native arena growth. The ledger distinguishes base, delta, pending and retained-snapshot data, build counters/output, preparation/consolidation output, Java metadata, node-predicate native/Java memory, label synopsis, and domain synopsis. Its `highWaterBytes` and refusal counters describe that ledger only. Unmodeled heap objects, JVM overhead, temporary Java arrays, unrelated query state, LMDB mappings, and the operating system's page cache are different resources and may coexist with it.

`directAdjacencyMaxBytes=0` means AUTO: at store construction, the source asks HotSpot's extended `OperatingSystemMXBean` for the container-aware total-memory limit and resolves exactly half, rounded down to an eight-byte boundary. A positive explicit value is used verbatim and must be at least 256 MiB. If the extended bean is unavailable, AUTO resolves to zero and the non-disabled derived index is refused; the LMDB store remains usable. Positive explicit cap bypasses AUTO and is not clamped to detected memory, so operators own that headroom decision. An effective cap below 256 MiB refuses the accelerator. The source computes an 86% `steadyLimitBytes` value, but the pinned store/account allocation path does not consume it as an admission limit; do not document it as a second active cap. The actual memory-account reservation uses `effectiveMaxBytes`.

Writer admission is bounded during build/catch-up, active asynchronous apply and consolidation. `directAdjacencyBacklogMaxBytes=0` resolves to `clamp(effectiveMaxBytes / 100, 8 MiB, 2 GiB)`. The measured backlog is pending plus preparation-output charges, not total index memory. A writer may be made to wait when this threshold is reached or cutover closes admission. Each admitted connection receives a child capture budget; by default `rdf4j.lmdb.directAdjacency.commitMaxBytes` is `clamp(effectiveMaxBytes / 1000, 8 MiB, 64 MiB)`. An explicit positive value can raise or lower this default but may not exceed the absolute backlog ceiling. If capture exceeds its child budget, the import can still commit to the authoritative store, but the adjacency path records a gap and recovery is required. The [operations page](../../../site/content/documentation/programming/lmdb-store.md#background-adjacency-indexes) already gives the operator-facing admission story; the configuration and implementation are [`LmdbStoreConfig`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java#L147), [`LmdbDirectAdjacencyOptions`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyOptions.java), and [`LmdbAdjacencyMemoryAccount`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyMemoryAccount.java).

Direct-adjacency runtime options below are JVM system properties read once when `LmdbDirectAdjacencyOptions.resolve()` runs during store construction, unless called out otherwise. Byte values are bytes; `Millis` values are milliseconds; edge/count values are counts.

| Property / config | Default | Meaning and scope |
| --- | --- | --- |
| `LmdbStoreConfig.directAdjacencyMode` | `PREFER` | `DISABLED`, `SHADOW`, or `PREFER`; persisted through Sail config when explicitly set. |
| `directAdjacencyCoverage` | `FULL` | `SELECTED` requires at least one predicate IRI; `FULL` requires no selected predicate list. |
| `directAdjacencyBuildOnStart` | true unless mode is `DISABLED` | Schedules the background build at store open; it does not mean the populated store is ready synchronously. |
| `directAdjacencyMaxBytes` | `0` / AUTO | Explicit positive bytes must be at least 256 MiB. |
| `directAdjacencyBacklogMaxBytes` | `0` / AUTO | Positive bytes or derived 1% cap with 8 MiB–2 GiB bounds. |
| `rdf4j.lmdb.directAdjacency.commitMaxBytes` | 8–64 MiB formula | Per admitted connection capture budget; explicit positive bytes cannot exceed absolute ceiling. |
| `.sealWarnMillis` | 1,000 ms | Positive elapsed-time threshold for commit sealing diagnostics. |
| `.maxDeltaGenerations` | 8 (range 1–64) | Generation-count maintenance policy. |
| `.supernodeEdges` | 4,096 edges | Supernode/chunk sizing input. |
| `.supernodeChunkEdges` | 4,096 edges | Chunk sizing input for supernode construction. |
| `.supernodeTargetBytes` | 65,536 bytes | Target byte size used by supernode/chunk construction. |
| `.buildThreads` | `max(1, processors − 1)`, bounded to visible processors | Build parallelism. |
| `.buildTargetMillis` | 43,200,000 ms (12 hours) | Positive target duration input to build policy. |
| `.buildRetryMillis` | 60,000 ms | Positive retry delay after recoverable build failure. |
| `.shadowSampleEvery` | 10,000 probes | Compare cadence in `SHADOW`; source can only sample requests with a transaction. |
| `.nodePredicateProjection.enabled` | true | Build-time outgoing node-predicate capability. |
| `.nodePredicateProjection.incoming.enabled` | false | Build-time incoming capability, additionally gated by the outgoing projection switch. |
| `.synchronousMaintenance` | true | Build/cutover publication-wait behavior for admitted writes. |
| `.failOnMaintenanceError` | false | Whether selected unexpected maintenance failures are surfaced rather than remaining on fallback path. |
| `.nodePredicateProjection.serve.enabled` | true | **Read on every serving call**, so it is an immediate serving kill switch, not a memory-release control. |

The names/defaults are resolved in [`LmdbDirectAdjacencyOptions.resolve()`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyOptions.java#L115). The CSF allocator has separate class-initialization controls; see [CSF allocation and ownership](storage-csf-format.md#shards-allocation-classes-and-retained-page-ownership). Query and experiment toggles under `LmdbDirectAdjacencyStore` are documented with their query consumers, rather than treated as storage contracts here. Exact property mapping to public Sail config is in [`LmdbStoreConfig`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java#L475); adjacency tuning system properties are not themselves serialized into `store.properties`.

## Developer checklist and contract sources

When changing this subsystem, preserve the invariants that one publication is snapshot-coherent, a missing revision becomes a gap, row absence requires a coverage proof, and every native allocation has one memory-charge owner with an explicit final-release path. For new optional projections, state whether inconsistency invalidates only that projection or the whole base; do not let a projection-level mismatch leak into unrelated rows. For new codecs, validate counts, ordinals, references and lengths before native access, and keep format-version scope explicit.

Useful intended-contract sources include [`LmdbDirectAdjacencyCommitTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyCommitTest.java), [`LmdbAdjacencyStartupConcurrencyTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyStartupConcurrencyTest.java), [`LmdbDirectAdjacencySnapshotTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencySnapshotTest.java), [`LmdbDirectAdjacencyConsolidationTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyConsolidationTest.java), [`LmdbNodePredicateKernelFaultTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNodePredicateKernelFaultTest.java), [`LmdbNodePredicateRecoveryTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNodePredicateRecoveryTest.java), and [`LmdbNodePredicateLifetimeTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNodePredicateLifetimeTest.java). They are references to expected contracts, not evidence that this documentation task ran those tests.

Related detail: [overview](storage-overview.md), [CSF page layout](storage-csf-format.md), [transaction/commit bridge](storage-transactions-and-async-writes.md), and [query routing](query-routing-and-hosts.md).
