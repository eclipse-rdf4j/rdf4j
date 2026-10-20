# Sail snapshots, changesets, and source lifetimes

The shared Sail integration changes in this guide are from the historical
comparison between merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` and inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. Their source descriptions were
checked against `a678d9a369deded63520cd86b6c30152485b7f6d`; the branch README
records these revision roles. These are important prerequisites
for LMDB-specific execution: a store may use an optimized path only when the
wrapper stack still preserves its transaction, snapshot, and observation
semantics. For the LMDB value and adjacency lifetimes themselves, use
[storage overview](storage-overview.md).

## Snapshot leases across branch flushes

`SailSourceBranch` now tracks a cached snapshot with a lease object: the branch
owns the snapshot generation, and each dataset observer borrowing it is counted
under the branch semaphore. When branch changes flush to the backing source,
the branch retires that generation. It can no longer hand the retired snapshot
to new readers, but already-open observers keep it alive until they close. The
last observer to release a retired lease closes the dataset. Similarly, when
an auto-flushing long-lived branch notices that a bypass writer made its
snapshot stale, it retires that generation so new borrowers see a newly
derived snapshot. Transaction-scoped branches (`autoFlush=false`) keep their
snapshot stable for their own lifetime.

The lifetime matters because a dataset can own a native read transaction or a
snapshot-bound view. Closing it at flush time would break existing readers;
reusing it for later readers would expose stale state. The lease trades a
small amount of branch metadata and delayed release for preserving both
existing-reader stability and fresh-reader visibility. The resource remains
live as long as a borrower is open, so leaked dataset observers keep their
snapshot resources alive as well. Dataset `close()` removes the observer,
releases the lease, compresses the branch changes, and runs auto-flush while
collecting later cleanup errors as suppressed failures behind the first.

The current implementation and its concurrency/failure coverage are in
[SailSourceBranch](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSourceBranch.java),
[`SailSourceBranchTest`](../../../core/sail/base/src/test/java/org/eclipse/rdf4j/sail/base/SailSourceBranchTest.java),
and [`SnapshotSailStoreTest`](../../../core/sail/base/src/test/java/org/eclipse/rdf4j/sail/base/SnapshotSailStoreTest.java).

## Compact changeset bulk path

`Changeset.approveAll` has a narrow compact path when all of these hold:

* the incoming iterable is a `Set` with at least 1,024 statements;
* no context override was requested; and
* the changeset has no existing approved, compact-approved, or deprecated set.

That path buffers the statements into a compact ordered sequence and tracks
approved contexts without first constructing the ordinary model-backed
approved set. It exposes the sequence to a sink through a read-only
`CompactApprovedSet` internal cross-module contract. Sinks that understand the
contract can consume the compact sequence without expanding it. An operation
that needs general `Model` lookup or mutation materializes it under the write
lock. This is an optimization opportunity for set-backed bulk ingestion, not a
change to `Set` uniqueness or statement/context semantics. For smaller sets,
non-set iterables, context overrides, or changesets with existing state, the
general path remains in use. No memory or speed measurement is asserted.

`SailConnection.addStatements` provides a compatible default loop for Sails
that do not override it. `AbstractSailConnection` has a final transaction-aware
entry point, counts inserted statement/context pairs for data-import metrics,
and preserves a pending prefix if iteration throws after an optimized sink may
have buffered some input. `SailSourceConnection` uses `SailSink.approveAll`
directly when no connection listeners require per-statement notifications;
with listeners it retains the per-statement path. Context arguments replace
each statement's original context; an empty context array retains that
statement context, matching the public method contract.

The main sources are [Changeset](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/Changeset.java),
[SailConnection](../../../core/sail/api/src/main/java/org/eclipse/rdf4j/sail/SailConnection.java),
[AbstractSailConnection](../../../core/sail/api/src/main/java/org/eclipse/rdf4j/sail/helpers/AbstractSailConnection.java),
and [SailSourceConnection](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSourceConnection.java).
The expanded tests in `ChangesetTest`, `SailSourceBranchTest`,
`SnapshotSailStoreTest`, and `UnionSailDatasetTest` cover branches of the
contract. For the ingestion command and resumable workspace semantics, see
[bulk loading](integration-bulk-loading.md) and
[storage ingestion/recovery](storage-bulk-ingestion-recovery.md).

## Native-source detection and ordered access

`SailDatasetTripleTermSource` now exposes its dataset to internal store
evaluators and can walk transparent base wrappers to collect an underlying
dataset type. It traverses union datasets and statement-access-transparent
delegating wrappers, but stops when it encounters a wrapper that can change
statement semantics or impose side effects (for example, a transaction
changeset or serializable read-observation wrapper). If the full wrapper path
cannot be classified, it returns no partial set of backing datasets. This
keeps store-specific execution from bypassing transaction-local behavior just
because a native dataset appears somewhere lower in the wrapper stack.

The adapter also detects pending statement changes through dataset layers. If
changes are pending, it reports no supported statement order, returns no
comparator, and refuses an explicit ordered scan with a query-evaluation error.
Physical store order describes the committed snapshot; applying it to a
transaction view with added/removed statements would be an invalid ordering
claim. The read-only `hasStatements` method delegates directly to the dataset.
The public-facing base contracts are [SailDataset](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailDataset.java),
[DelegatingSailDataset](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/DelegatingSailDataset.java),
[SailDatasetTripleTermSource](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailDatasetTripleTermSource.java),
and [UnionSailDataset](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/UnionSailDataset.java).

## Developer guidance

If a new `DelegatingSailDataset` wrapper is added, explicitly state whether its
statement access is transparent before allowing native unwrapping to cross it.
If it adds transaction state, observation, filtering, inference, or any other
semantic side effect, it is a barrier. When changing branch flush/close code,
cover existing borrowers, newly opened readers after a commit, stale snapshots,
multiple observers, and failures in each close/cleanup stage. Keep source
ordering claims disabled when transaction-local changes invalidate their
preconditions.
