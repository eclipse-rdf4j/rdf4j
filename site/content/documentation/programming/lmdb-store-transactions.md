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
commit-publication and whole-write recovery rows below describe repaired behavior. A replayable read that has not
exposed a result may be restarted when TripleStore growth or dictionary triple-term membership requires it. Ordinary
statement reads preserve their existing RDF snapshot through ValueStore-only growth.

## Changes applications may notice

| Operation | Behavior before this change | Behavior now and what to do |
| --- | --- | --- |
| A query or transaction remains open while a map grows | Publication of Sail-buffered commits to LMDB, and the associated map growth, could be postponed while observers remained open; after a reader reset, the record iterator could renew its native cursor and appear to continue. That renewal did not promise one pinned native generation across resize. | Ordinary statement reads continue on their original RDF snapshot through ValueStore-only growth. TripleStore growth and dictionary triple-term membership retain the retry boundary: a replayable read-only attempt that has not exposed a result may restart automatically, up to `readOnlyReplayMaxRetries`. An observed, writing, or otherwise ineligible view instead reports a conflict: re-run the whole `SNAPSHOT_READ` query or restart the `SNAPSHOT`/`SERIALIZABLE` transaction. Replay reruns the query plan, including remote `SERVICE` calls, so external services may receive duplicate requests. |
| A repository transaction commits while other reads stay open | Root changes could remain buffered until the final observer closed, making the commit path depend on reader cleanup. | `RepositoryConnection.commit()` flushes both roots at the commit boundary. Native commit work must complete before `commit()` returns; with `forceSync=true`, the required storage flush is also part of that path. |
| A namespace-only update commits | Namespace data was persisted separately in `namespaces.dat`, outside the TripleStore transaction. | The namespace snapshot is written into the TripleStore environment with a native commit. A namespace-only change therefore performs a physical TripleStore commit, but does not commit the ValueStore or advance the RDF data revision. |
| A write reaches a native map limit | Map-full growth could commit a prefix while continuing the logical write. | Tracked TripleStore writes can abort, grow, and replay their complete statement and namespace journal before publication. Dictionary growth is proactive; unexpected native ValueStore map-full errors can still fail a tracked write. Eligible writes may omit the journal; exhaustion then requires rollback and retry of the entire transaction. Failed writes publish no partial RDF or namespace transaction. |

Automatic replay is limited to read-only work that remains unobserved and has a replay factory. It is bounded by the
configured retry count; repeated growth or ineligible work can still invalidate a pinned view. The isolation-level
contracts remain the same; neither automatic replay nor the former cursor-renewal path allows an already exposed result
to continue on a different native generation. A retried query may repeat work outside the store, including a remote
`SERVICE` request, so such endpoints should tolerate repeat invocation. The underlying exception currently says:

```text
SNAPSHOT transaction invalidated: the store's memory map was resized during the transaction; retry the transaction
```

At the Sail layer, map-growth invalidation is reported as `SailConflictException`, a typed `SailException` indicating
that the requested isolation level could not be maintained. Repository and query APIs may wrap it in their own
exception type, so inspect the cause chain when handling the error.

For a query-level `SNAPSHOT_READ`, abandon and re-execute the whole query so it gets a fresh result snapshot. Do not
resume from the last row: results already consumed came from the invalidated view, and downstream side effects should
be safe to repeat. For a transaction-level `SNAPSHOT` or `SERIALIZABLE`, restart the transaction; retrying only the
last statement would mix generations.

The earlier cursor-renewal path could make a read appear to continue after a reset. The current path preserves
generation consistency: eligible unobserved work may be replayed, while an observed or otherwise ineligible result is
invalidated rather than allowed to proceed on a different native view.

## Map sizing and reader lifetime

`autoGrow` remains enabled by default. When growth is needed, LMDB must remap its environment while native readers are
quiesced. ValueStore ID resolution preserves logical value identity and the existing TripleStore snapshot through a
dictionary-only remap. TripleStore growth still retires or invalidates the old physical statement view; eligible
unobserved reads may be replayed as described above. Dictionary triple-term enumeration is also a membership snapshot:
an active term scan retains that retry boundary, and a pre-growth view first requesting terms after dictionary growth
reports a conflict rather than silently acquiring newer terms. An eligible unobserved attempt can then replay the
whole query or transaction. When the workload is known, pre-size both the TripleStore and ValueStore maps using
`LmdbStoreConfig.setTripleDBSize(...)` and `setValueDBSize(...)` to reduce how often this boundary is reached.
Disabling `autoGrow` disables automatic growth; a write that reaches the configured map limit
then fails with a map-full error.

Replay tracking is chosen before the first mutation and stays fixed for the whole transaction. Small maps, unknown
growth history, high occupancy, or rapid page allocation retain complete tracking. Tracking may be omitted only when
both maps are at least 64MiB, both have at least 32 successful mutation observations, and their occupancy and headroom
allow a conservative forecast using recent peak growth and a minimum 512KiB reserve per transaction. Allocation rate
here means native high-water bytes per successful transaction, not bytes per second. Deleted data and free pages do
not establish reusable headroom while older readers may still pin them. Reopening or replacing a dictionary environment
requires new history.

If capacity is unexpectedly exhausted while tracking was omitted, the store attempts capacity growth for the next
attempt and recovery retains tracking. Growth or cleanup failures are retained as suppressed exceptions. The cause
chain includes `LmdbTransactionRetryException`, for example:

```text
LMDB TripleStore capacity exhausted while replay tracking was omitted; roll back and retry the entire transaction
```

The same message identifies `ValueStore` when the dictionary exhausted its capacity. Call `rollback()` and resubmit
all statements and namespace changes in a fresh transaction. Further writes or commit attempts in the failed logical
transaction retain that retry error until rollback; retrying only its last operation would discard earlier intent.

Buffered write intent can start a managed growth episode when projected allocated-page use reaches the configured soft
threshold or a buffered operation has no exact estimate. The episode closes admission to new connection transactions and
standalone queries, including new writers, and schedules phased growth rather than resizing synchronously at branch
preflight. Already active logical writers finish first; this writer drain has no growth timeout. Existing readers may
upgrade to writers during that phase. Upgrades after the transition to reader draining fail with a retryable transaction
conflict. During ordinary growth, the coordinator starts the configured reader grace after the last logical writer
finishes publication and cleanup.

Projected estimates cannot guarantee capacity: native write costs can exceed them or include work that cannot be sized
exactly. Unexpected exhaustion therefore still uses complete TripleStore replay or proactive dictionary growth when
tracked; an unexpected native ValueStore map-full error can still fail a tracked transaction. When tracking was omitted,
the whole-transaction retry above applies. If an already admitted writer exhausts capacity before it finishes, emergency
growth is attempted only when its internal continuation, journal and dictionary checkpoint can preserve all writer
observations and borrowers. It suspends that writer and starts a separate reader grace. An open reader can delay the writer
and all new admissions for that grace when the affected map supplies its membership snapshot. At its end, affected
observed or otherwise nonreplayable readonly work is cancelled; eligible unobserved work is parked for replay. Ordinary
statement readers do not need cancellation or replay for ValueStore-only growth. Native calls must quiesce safely
before the mappings change, so the grace bounds the
cancellation request rather than forced termination. The store grows the environments whose projected write needs more
capacity and reserves fresh paired epochs for eligible replays before reopening admission. Replay execution continues
lazily. Emergency recovery resumes internal mutations without rerunning application, SPARQL, SERVICE or custom-function
code. An unsafe continuation or incomplete journal requires rollback and retry of the entire exhausted transaction;
other writers remain protected.

Configure the per-store soft allocated-page fullness trigger with `LmdbStoreConfig.setMapGrowthThreshold(...)` (default
`0.75`). It also guides sizing below the trigger and measures allocated high-water pages, not live statement count.
Configure the reader grace with `LmdbStoreConfig.setMapGrowthReadDrainTimeoutMillis(...)` (default `30000`
milliseconds). Ordinary growth starts it after logical writers finish; emergency growth starts its own grace while the
exhausted writer is suspended. Native work that remains after the grace still has to quiesce safely. Queries keep
their original maximum execution time across admission, execution, lazy result consumption and replay. Waiting consumes
that same budget without restarting it. Without a query timeout, admission waits interruptibly until growth finishes or
the store shuts down. Starting a transaction has no independent transaction timeout. Setting
the reader grace to `0` skips the grace period but does not disable the early admission warning or the writer drain.
Configure eligible read retries with `LmdbStoreConfig.setReadOnlyReplayMaxRetries(...)` (default `3`); setting the retry
limit to `0` disables automatic replay.

An admitted read view retains its native TripleStore snapshot and logical dictionary value lifetime until its final
owning lease is released. Its dictionary native reader preserves original triple-term membership until close or
dictionary remapping; ordinary ID resolution can then use brief reads without replacing the TripleStore snapshot.
A `SNAPSHOT`/`SERIALIZABLE` connection can keep that transaction view after an individual result or dataset closes.
Close datasets, iterations, and query results promptly in high-concurrency applications because native reader slots
remain limited. A long-lived reader also
delays reclamation of old dictionary IDs until the reader horizon has passed and committed triples have been checked.
Reclamation makes internal pages and IDs reusable; it does not promise that an `.mdb` file shrinks.

Returned RDF values remain usable after their result, transaction, and repository close, including deferred nested
triple terms. Shared lazy value lifetimes preserve dictionary bytes while values remain unresolved, and store shutdown
resolves still-reachable lazy values before freeing the native environment. This can add value-resolution work to
shutdown; normal result closure does not materialize every lazy value. Direct dictionary-only snapshots without an
authoritative TripleStore reader retain strict native dictionary snapshots and report a retryable conflict if that
dictionary map grows.

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
