---
title: "LMDB transactions, map growth, and recovery"
toc: true
autonumbering: true
---

This page describes the transaction and storage behavior of the RDF4J LMDB Store, including the tradeoffs in its
native snapshot and recovery path. The store continues to advertise `NONE`, `READ_COMMITTED`, `SNAPSHOT_READ`,
`SNAPSHOT`, and `SERIALIZABLE`; its default remains `SNAPSHOT_READ`. The general meanings of those levels are described
in [Transaction Isolation Levels](/documentation/programming/repository/#transaction-isolation-levels).

Here, “this change” refers to the GH-6070 work in [PR #6073](https://github.com/eclipse-rdf4j/rdf4j/pull/6073),
compared with its pre-port target at `978c9a64cf4e`. No advertised isolation level was removed or redefined. The
commit-publication and whole-write recovery rows below describe repaired behavior; explicit invalidation on map growth
is the availability cost applications need to handle.

## Changes applications may notice

| Operation | Behavior before this change | Behavior now and what to do |
| --- | --- | --- |
| A query or transaction remains open while a map grows | Publication of Sail-buffered commits to LMDB, and the associated map growth, could be postponed while observers remained open; after a reader reset, the record iterator could renew its native cursor and appear to continue. That renewal did not promise one pinned native generation across resize. | A pinned result is invalidated when either LMDB map is resized. Re-run the whole query to get a new `SNAPSHOT_READ` result; restart a `SNAPSHOT` or `SERIALIZABLE` transaction. The old result cannot continue on a new generation. |
| A repository transaction commits while other reads stay open | Root changes could remain buffered until the final observer closed, making the commit path depend on reader cleanup. | `RepositoryConnection.commit()` flushes both roots at the commit boundary. Native commit work must complete before `commit()` returns; with `forceSync=true`, the required storage flush is also part of that path. |
| A namespace-only update commits | Namespace data was persisted separately in `namespaces.dat`, outside the TripleStore transaction. | The namespace snapshot is written into the TripleStore environment with a native commit. A namespace-only change therefore performs a physical TripleStore commit, but does not commit the ValueStore or advance the RDF data revision. |
| A large write reaches the TripleStore map limit | Map-full growth could commit a prefix while continuing the logical write. | The current path aborts the TripleStore attempt, grows the map, and replays the complete in-process statement and namespace mutation journal before publication. This avoids publishing a prefix, at the cost of replay work and temporary disk use for large writes. |

The first row is the main new availability cost applications need to handle. The isolation-level contracts remain the
same; the former cursor-renewal path did not make continuing the old result across resize a supported guarantee. The
underlying exception currently says:

```text
SNAPSHOT transaction invalidated: the store's memory map was resized during the transaction; retry the transaction
```

For a query-level `SNAPSHOT_READ`, abandon and re-execute the whole query so it gets a fresh result snapshot. Do not
resume from the last row: results already consumed came from the invalidated view, and downstream side effects should
be safe to repeat. For a transaction-level `SNAPSHOT` or `SERIALIZABLE`, restart the transaction; retrying only the
last statement would mix generations. Repository and query APIs may wrap the underlying `SailException` in their own
exception type, so inspect the cause chain when handling the error.

This is an invalidation boundary, not a promise that every pre-change query always survived every resize. The earlier
cursor-renewal path could make a read appear to continue after a reset; the current path fails rather than allowing an
already-pinned result to proceed on a different native view.

## Map sizing and reader lifetime

`autoGrow` remains enabled by default. When growth is needed, LMDB must remap its environment while native readers are
quiesced. Pinned read views are invalidated by that remap. Pre-size both the TripleStore and ValueStore maps when the
workload is known, using `LmdbStoreConfig.setTripleDBSize(...)` and `setValueDBSize(...)`, to reduce how often this
boundary is reached. Disabling `autoGrow` disables automatic growth; a write that reaches the configured map limit
then fails with a map-full error.

An admitted read view holds a native reader in each environment until its final owning lease is released. A
`SNAPSHOT`/`SERIALIZABLE` connection can keep that transaction view after an individual result or dataset closes. The
reader-slot limits already existed before this change; pinned views now keep both readers for the life of the view.
Close datasets, iterations, and query results promptly in high-concurrency applications. A long-lived reader also
delays reclamation of old dictionary IDs until the reader horizon has passed and committed triples have been checked.
Reclamation makes internal pages and IDs reusable; it does not promise that an `.mdb` file shrinks.

RDF values returned from a successfully closed result remain usable: the normal final close detaches the reachable lazy
and nested values from the native read snapshot. That work can add CPU and value-resolution work to close. One narrower
case remains for direct low-level `SailDataset` use: if map growth invalidates the snapshot before it is closed, values
that were already returned but are still lazy may no longer be resolved after invalidation. Resolve values while that
snapshot is valid, or reacquire a dataset and read them again. Public `RepositoryConnection` statement and binding
paths initialize their returned values. This invalidation-specific lifetime note is source-derived; it is not a
separate runtime reproduction.

## Automatic publication and low-level SailSource use

For ordinary repository transactions, use `RepositoryConnection.commit()` as the publication boundary. It explicitly
flushes the explicit and inferred roots even when read datasets remain open.

Applications that use `SailSource` or `SailSourceBranch` directly have a separate auto-flush tradeoff. When closing the
last observing dataset would need to wait for another owner's prepared writer, the observer close now returns without
blocking and skips that auto-flush. The changes remain pending; after the prepared writer completes, the direct caller
must call `SailSource.flush()` to publish them. This avoids a close/publication deadlock and does not discard the
changes. It is not the behavior of `RepositoryConnection.commit()`.

Direct backing-source users also get a retryable admission error if they try to create a read dataset under the same
owner while a ValueStore map-growth checkpoint is still pending:

```text
LMDB ValueStore map growth checkpoint is in progress; retry dataset admission after the writer completes
```

This prevents a read view from combining dictionary data already checkpointed for space with triples not yet
published. Other owners wait for the writer to finish. Complete or roll back the low-level write, then retry dataset
admission. This is a backing-`SailSource` boundary; it is not a claim that all public repository reads fail this way.

The store now also checks ownership of unfinished native writes. A backing sink that belongs to a different owner, or
an older sink from a finished writer generation, cannot publish or abort the current writer. Separate connections and
concurrent transactions remain supported; code using the low-level source API should keep each unfinished writer with
its owner and close sinks when finished.

## Store files, migration, and backup

The native metadata changes are additive: the TripleStore now stores namespace and index-generation metadata, and the
ValueStore stores its term-index manifest and value-ID retirement records. RDF value-ID encodings are unchanged. The
existing limit of 12 active triple-term indexes is also unchanged, but rebuilding indexes can temporarily require room
for both the old and staged index generations.

When the native namespace marker is absent, the new store imports `namespaces.dat` once. After the marker is written,
including for an intentionally empty namespace set, the native snapshot is authoritative. Editing a leftover
`namespaces.dat` file no longer changes namespaces in a migrated store; use the Repository namespace operations.
Likewise, once an index manifest exists, its committed index set takes precedence over stale `store.properties` index
settings. Use an explicit `LmdbStoreConfig` index setting when you intend to request a reindex.

The test suite opens and reads a pre-change store with the new code. It does not establish that older RDF4J versions
can open a store after these native metadata records have been written, or that downgrading in place is safe. Keep a
pre-upgrade backup or export, and test any rollback by restoring to a separate copy. Continue to back up the complete
store. The existing warning about separately copying the ValueStore and TripleStore during active writes still
applies because they are separate LMDB environments.

## Durability and uncertain outcomes

`forceSync` remains `false` by default. In that mode the LMDB environments use `MDB_NOSYNC` and `MDB_NOMETASYNC`, so
an acknowledged commit has weaker power-loss durability. Set `forceSync=true` when the application requires LMDB's
synchronous commit behavior, and ensure the filesystem and storage stack honor flushes. Even with `forceSync=true`,
the ValueStore and TripleStore are separate environments: dictionary additions and retirement intents are committed
before the authoritative triple/namespace transaction. An interrupted write can therefore leave unreferenced
dictionary entries for later cleanup. Retirement and recovery recheck committed triple liveness before an ID can be
reused.

If an I/O failure leaves the namespace commit outcome uncertain, the store refuses further use with:

```text
LMDB namespace commit outcome is uncertain; reopen the store before using it
```

After an exception from commit, do not blindly replay the write. Reopen or inspect the authoritative store state first,
because the native commit may have completed before the failure was reported. Make application-level retries
idempotent or reconcile their outcome before applying them again.

The checked-in crash campaign exercises `forceSync=true` on a calibrated Linux guest with a volatile NBD-backed disk.
That verifies the simulated flush and power-cut model used by the tests; it does not establish behavior for every
physical controller, filesystem, or device. No comparative latency or throughput benchmark for this branch is
published here.

## Regression and recovery evidence

The behavioral comparison is covered by concrete tests in the [LMDB test tree](https://github.com/eclipse-rdf4j/rdf4j/tree/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb):

- [Snapshot publication](https://github.com/eclipse-rdf4j/rdf4j/blob/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreFlushReproductionTest.java#L79), [transaction-wide snapshot generation](https://github.com/eclipse-rdf4j/rdf4j/blob/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreFlushReproductionTest.java#L180), and [map-growth invalidation](https://github.com/eclipse-rdf4j/rdf4j/blob/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreFlushReproductionTest.java#L749).
- [Reader-horizon retirement](https://github.com/eclipse-rdf4j/rdf4j/blob/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSnapshotValueLifetimeTest.java#L224) and [live-value preservation after failed commit and reopen](https://github.com/eclipse-rdf4j/rdf4j/blob/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbValueRetirementRecoveryTest.java#L106).
- [Legacy namespace migration](https://github.com/eclipse-rdf4j/rdf4j/blob/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCrashRecoveryTest.java#L338) and [interrupted namespace/triple publication](https://github.com/eclipse-rdf4j/rdf4j/blob/f8ce9a0e3c528e51355b023ada6678ff0e08ad88/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCrashRecoveryTest.java#L592).

The [hosted QEMU CI run](https://github.com/eclipse-rdf4j/rdf4j/actions/runs/36351546739) records the selected Java gate, device calibration, and crash campaigns. The [GH-6070 pull request](https://github.com/eclipse-rdf4j/rdf4j/pull/6073) links the review and CI evidence for this scope.
