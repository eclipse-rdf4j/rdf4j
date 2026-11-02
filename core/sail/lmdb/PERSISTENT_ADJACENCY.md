# Persistent adjacency storage

The statement authority is stored in three named databases in the existing `triples` LMDB environment:

| Database | Contents |
| --- | --- |
| `adjacency-pages` | Bounded, compressed CSF pages for outgoing and incoming adjacency. |
| `adjacency-deltas` | Insertions and tombstones relative to the visible base pages. |
| `adjacency-metadata` | The format marker, exact counts, row directories, value references, and compaction work. |

The existing `namespaces` database remains auxiliary metadata in that environment. The old statement permutation
databases and `contexts` database are removed during migration. Value dictionary records and triple-term indexes
remain in the existing `values` environment.

The implementation is `LmdbPersistentAdjacency`. `TripleStore` owns its transactions and routes statement reads,
mutations, context enumeration, counts, bulk import, and value-reference checks through it.

## Edge keys and page values

Page and delta keys are 33 bytes: a one-byte plane followed by four big-endian 64-bit identifiers in the order
`predicate, row, neighbor, context`. LMDB compares the encoded bytes lexicographically, giving unsigned identifier
order. Context `0` denotes the default graph. The query wildcard `-1` is never a persisted edge component.

| Plane | Statement kind | Row | Neighbor | Scan order |
| --- | --- | --- | --- | --- |
| `0` | Explicit | Subject | Object | Predicate, subject, object, context |
| `1` | Explicit | Object | Subject | Predicate, object, subject, context |
| `2` | Inferred | Subject | Object | Predicate, subject, object, context |
| `3` | Inferred | Object | Subject | Predicate, object, subject, context |

A partition is one `(plane, predicate)` pair. A page contains at most 1,024 quads from one partition, in unsigned
`row, neighbor, context` order. Small rows share pages; a large row can span pages. The page's key is its first full
edge key. A predecessor seek locates the page that could contain a requested edge, followed by a bounded search of
the decoded tuples. Page ranges must be ordered and must not overlap.

`PersistentCsfPageCodec` wraps the existing compact CSF page image in a 32-byte envelope. The envelope records its
magic and version, payload length, compact-format hash and version, and CRC32C checksum. Envelope and CSF payload
fields use their defined little-endian encoding; LMDB keys and metadata counters use big-endian encoding. The
decoder validates bounds, checksum, vector structure, row boundaries, and tuple ordering. Its returned arrays own
their data. Native pointers and process-local page directories are never serialized.

### Sequential decoding and membership probes

Both `decode` and `probe` first copy the bounded payload into one owned native allocation with the packed-vector
reader's required tail padding. Checksum verification and structural validation operate on that same copy. The
native allocation is released before either method returns, including when validation fails. `decode` returns
detached tuple columns; `probe` checks membership without materializing those columns. A probe still validates the
complete page after finding a match, and returns the first tuple so the caller can verify the physical page fence.

After vector validation, `PackedLongVector.NativeSequentialReader` reads each column forward. It loads the block
directory entry, header, and base once per block of up to 256 values. Each lane uses the scalar packed-bit reader;
width-zero blocks use their base directly. Delta blocks retain the accumulated raw payload value and add each
successive delta once, rather than repeatedly decoding the preceding lanes. Typed identifiers are restored after
reconstruction. The block state resets at every block boundary, including partial final blocks.
This reader owns no memory and remains inside the validated native copy's lifetime. These access choices do not
change the compact page format, checksum, vector cardinality checks, or tuple-order validation.

## Metadata format

The adjacency format version is `1`. A metadata key begins with a one-byte type. A `plane` below is one byte; every
identifier is an eight-byte big-endian value. Except for type `0`, values are positive eight-byte big-endian counts.
A zero count is represented by an absent key.

| Type | Key after the type byte | Value and purpose |
| --- | --- | --- |
| `0` | None | Four-byte big-endian format version. Written after initialization or migration completes. |
| `1` | `plane` | Statement total. Present only for outgoing planes `0` and `2`. |
| `2` | `plane, predicate` | Exact predicate count, separately for all four planes. |
| `3` | `plane, predicate, row` | Exact row count and the directory for predicate-first scans. |
| `4` | `plane, row, predicate` | The same row count, ordered for a bound node with an unbound predicate. |
| `5` | `plane, context` | Context count for outgoing plane `0` or `2`. |
| `6` | `context` | Context count across explicit and inferred statements, including context `0`. |
| `7` | `identifier` | Number of references in subject, predicate, object, and nonzero context positions. |
| `8` | `plane, predicate, row` | Number of stored delta records for this row, including tombstones. |
| `9` | `plane, predicate` | Number of stored delta records for the partition. |
| `10` | `plane, predicate` | Compaction candidate flag; its value is exactly `1`. |

Counts describe quads, so two contexts for the same subject/predicate/object contribute two. Reference counts
include each occupied position: an identifier used as both subject and object contributes twice. Types `3` and `4`
are directories and counts, rather than additional copies of the statement data.

A compaction candidate is required when a partition has pending deltas and no base page, or when its delta count
reaches 128. Once set, the candidate may remain set if later mutations reduce the count below 128. A partition with
no pending deltas has no candidate. These records make compaction work discoverable after reopening the store.

## Mutations and persisted deltas

A delta value is exactly one byte: `1` makes the edge present and `0` makes it absent. Each delta represents the
edge's final state relative to the base pages visible in the same LMDB transaction. Restoring the base state removes
the delta. For example, adding and then removing an edge absent from the base leaves no delta; removing and then
restoring a base edge also leaves no delta.

Every logical insertion or removal updates both directions, all exact counts, context metadata, value references,
and the compaction directories in the caller's write transaction. Explicit/inferred promotion remains a
`TripleStore` operation and changes the two statement kinds in that same transaction. Duplicate insertion and
removal of an absent quad do not change counts.

Reads merge the base page stream with its ordered delta stream. Tombstones suppress base edges; insertion deltas
add edges. Reopening uses these persisted deltas directly. Rebuilding an in-memory adjacency index is not a
prerequisite for reading committed data.

## Compaction and transaction boundaries

Ordinary commit visits the persistent type `10` work queue. A new partition gets its initial pages; existing
partitions can retain small committed delta sets. An explicit forced compaction visits type `9`, including
partitions below the normal threshold.

Compaction merges one affected base page interval with at most one page-sized delta batch at a time. Each merge
tuple column holds at most 2,048 values; a decoded input page and up to 1,024 copied delta records are additional
bounded state. It preserves unrelated page values and can append after a full unchanged page.
Compaction updates page values and dirty counts and deletes absorbed deltas inside the existing write transaction.
It does not publish a separate generation or require an independent compaction commit.

### Compaction scratch and positioned deletion

One invocation-owned compaction workspace is allocated lazily after the first partition candidate is validated.
Its three 2,048-element tuple arrays are reused across every partition and merge in that invocation. Each merge
overwrites the populated prefix, and encoding reads only that prefix; stale values beyond it are never consumed.

The same workspace owns one `PersistentCsfPageCodec.EncodingWorkspace` for page construction, encoder layout, and
checksum state. This encoder scratch is not thread safe, retains no caller arrays or native memory, and resets its
logical state for each page. Encoded output is a fresh independent byte array. Each encode still owns and releases
its native scratch within that call; a failed encode does not prevent subsequent reuse of the encoder workspace
by its owner.

Delta collection copies and validates a record's key and value before deleting that record through the positioned
LMDB cursor. The existing partition and exclusive upper-page fence, together with the 1,024-record limit, bound
the collection. Each deletion is followed by one cursor advance to its successor. The collection cursor closes
before tuple merging, page encoding, or dirty-count updates; those later steps use only the detached delta batch.
There is no second per-key search to remove the collected deltas.

Deletion and replacement remain part of the same uncommitted writer. A failure propagates to the enclosing
transaction's abort handling; callers of the raw compaction API must not commit a failed attempt. `TripleStore`
supplies the existing abort, eligible map-growth replay, and migration retry paths. Neither the workspace nor a
collection cursor crosses those transaction boundaries.

### Publication and recovery

The LMDB commit therefore publishes outgoing pages, incoming pages, deltas, counts, contexts, and namespace changes
atomically. Abort discards all these changes. Map-full failures during mutation or compaction enter the existing
transaction ownership, abort, growth, and replay paths; an aborted attempt cannot publish only one adjacency
direction or its metadata.

`forceSync` retains its existing meaning. The triple environment uses the existing LMDB synchronization flags;
turning synchronization off retains the same durability tradeoff as the former statement indexes. There is no
separate adjacency journal with a different synchronization policy.

The dictionary continues to use a separate LMDB environment. `LmdbSailStore` preserves its existing order:
dictionary records are committed before the authoritative triple transaction, and the existing reference-count,
generation, ownership, and recovery protocol completes that publication. This change does not introduce a new
cross-environment transaction protocol or change the dictionary's durability policy.

## Readers and snapshot lifetime

LMDB provides versioned pages for readers. A retained reader observes its original base pages, deltas, and metadata
even when a newer writer compacts or removes them. The reader must use the same native transaction for all three
databases and for both adjacency directions.

The `Txn` iterator adapter uses the existing map-resize barrier, transaction wrapper lease, and snapshot validity
checks for iteration, batch reads, and seeking. A valid pinned view keeps its original version. An invalid pinned
view fails through the existing snapshot checks. An ordinary renewable reader can reopen its cursors after a
transaction renewal and resume after the last returned tuple, or at a pending seek target.

Page decoding copies the bounded page data before exposing it. No returned tuple or decoded page keeps a pointer
into LMDB's mapped values across mutation or remapping. A read-only iterator may retain a bounded working set of
decoded pages for its current transaction; renewal discards that iterator-local cache. Managed read-only scans
and exact point reads can also reuse private columns from the separate snapshot-keyed cache described below.
Managed writer-side scans use neither cache.

Planner statistics, direct-adjacency accelerators, decoded pages, revision notifications, and other derived caches
remain process-local. Their availability does not establish the durable statement authority. Exact persisted
metadata and base-plus-delta reads remain available independently of these caches.

### Reuse within one operation

`ReadPageCache` retains at most 16 decoded pages, keyed by their complete physical fence keys. Entries contain
detached heap arrays and copied keys. The cache and its interval certificates belong to one iterator or one
synchronous mutation batch. Managed readers can obtain the columns from the shared snapshot cache, but keep their
entry and interval state private. Eviction removes the local entry and any interval certificate attached to it.
Closing the owner clears its local entry references; shared retention follows the separate global bound below.

The allowed reuse depends on the caller's transaction contract:

| Caller | Allowed reuse | Lifetime boundary |
| --- | --- | --- |
| `scan(long txn, ...)` | Decoded pages, certified page intervals, and stable read cursors. | One caller-owned stable operation. The transaction must stay valid and must not be mutated until the iterator closes, even if it is a native writer transaction. |
| `scan(Txn txn, ...)` with a read-only transaction | Stable read state and snapshot-specific shared columns, under the existing transaction guard. | One native transaction version. Renewal closes the old iterator and creates fresh state and shared-cache identity before resuming. Pinned-view validity checks still apply. |
| `scan(Txn txn, ...)` with a mutable writer transaction | Detached decoding for the current row, with independently owned row cursors. | No immutable-reader page cache, interval certificate, or empty-plane proof is retained across row reads. |
| `MutationBatch` | Decoded base pages found by an exact native predecessor seek, plus batch-owned page and metadata cursors. | One synchronous physical mutation batch. It must close before compaction, replay, transaction completion, or external callbacks. |

A mutation batch can reuse base pages because its operations update deltas and metadata without rewriting the
base. Each base-page membership probe still performs a native predecessor seek and checks the complete fence key
before using a cached page. It does not use interval certificates or an empty-delta-plane proof. Delta reads and
counter updates remain immediate in the supplied writer, so reuse does not buffer logical mutations or change
their read-your-writes behavior.

Stable row scans retain and reseek one page cursor and one delta cursor instead of opening a pair for each
node/predicate row. The row directory retains its own cursor where enumeration is needed. The predicate-first
streaming path advances directly through packed pages and deltas. These choices change cursor and decoding work;
they do not change the requested tuple order, residual filters, or which transaction supplies the data.

### Certified page intervals

A cached page's last decoded tuple does not establish where the next physical page starts. Pages can contain gaps,
and one row can span multiple pages. Before using a cached page to replace a predecessor seek, the stable reader
lazily obtains the next physical page fence from LMDB, copies it, checks that the cached page's last tuple precedes
it, and restores the cursor to the cached page's exact fence. End of the database is recorded explicitly when no
next fence exists.

For a requested full edge key, reuse requires the same plane and predicate, a cached fence at or before the key,
and a key strictly before the copied next fence, unless the certificate records end of the database. The
certificate proves which base page is the predecessor; it does not claim that the requested edge is present.
The bounded tuple search and delta merge still determine the result.

A `BaseStream` using detached cached data treats its native cursor as unpositioned. Only if traversal crosses the
cached page does it restore the exact fence before issuing the next-page operation. This remains necessary even
when looking for a cached interval has moved the shared cursor. Certificates are created only for stable scan
paths, stay with their cache entries, and are discarded on eviction, close, or transaction renewal. Failures while
obtaining or restoring a certificate propagate through the scan's cursor cleanup.

### Empty delta planes and consistency

A stable row scan that can visit multiple rows can seek once to the selected plane's delta prefix. If no key in
that plane exists, it retains that proof and skips later per-row delta seeks. The proof covers only that one
explicit/inferred and outgoing/incoming plane; deltas in another plane do not invalidate it. A row scan with both
its node and predicate bound keeps its ordinary row delta seek. When the selected plane contains deltas, rows
continue to merge their visible delta streams normally.

Both interval certificates and empty-plane proofs rely on an unchanged native transaction version. The managed
snapshot adapter checks the wrapper lease and snapshot validity under the existing map-resize barrier before
reads and seeks. An ordinary renewal discards the old iterator's cursors, decoded pages, and proofs before resuming
strictly after the last delivered tuple, or inclusively at a pending seek target. Mutable writer scans never retain
these proofs, so a later same-transaction mutation cannot be hidden by a previous absence check or cached page
interval.

### Delayed node-first merging

An unbound `spoc` or `opsc` scan begins with the type `4` node/predicate directory. After completing 128 logical
directory rows, it can switch to a bounded merge of predicate streams. Admission requires a stable read and
unbound node, predicate, and neighbor fields. A context filter remains a residual filter. The scan discovers the
selected plane's live predicates from type `2` using its existing metadata cursor. It admits at most 64 predicates,
with at most 128 stored delta records per admitted partition, including tombstones. A previously established empty
delta-plane proof supplies a zero dirty count without additional metadata reads; otherwise admission reads the
type `9` count for each candidate predicate.

For each predicate, admission reserves space for `min(1024, live count + dirty count)` tuples in its current base
page. This is conservative because a partition's base cardinality is its live cardinality minus insertion deltas
plus tombstones. The addition saturates at the page limit without overflowing. The sum of those reservations,
charged at three eight-byte tuple columns per tuple, must fit within 512 KiB. Both the head limit and the tuple
payload limit apply independently. Every initial or replacement page, including a page transferred from the warm
cache, must fit its predicate's reservation before the stream retains it. An undersized persisted count therefore
fails validation instead of permitting a larger retained working set.

Encountering a sixty-fifth predicate, a dirty count above 128, or a reservation beyond the payload budget restores
the exact next unconsumed directory key and continues ordinary row traversal. Catalog discovery examines at most
65 predicate entries. A failed admission is not retried until an explicit seek resets the traversal.

Promotion happens only after closing the previous, fully consumed row. The next unconsumed `(node, predicate)`
directory entry defines the lower frontier. A stream for a lower predicate starts at the next unsigned node;
other streams start inclusively at the frontier node. Carrying past the unsigned maximum produces no successor.
The existing full-tuple inclusive/exclusive minimum checks and residual context filter still apply to merged
results.

Each admitted predicate has one visible stream head and at most one current detached base page. An empty delta
plane uses a base-only stream; otherwise `OrderedMerge` combines that predicate's base and delta streams before
offering a visible tuple to the heap. Insertions and tombstones follow the same merge rules as predicate-first
reads. A heap compares `(node, predicate)` heads. The winner remains first while it emits neighbors and contexts
from that same row; heap ordering is updated when the winner changes rows or finishes.

Warm pages transfer out of the iterator's separate 16-entry `ReadPageCache`. Before a page load during priming,
unused history is trimmed so the early retained heads and history fit within 16 full pages, or 384 KiB of tuple
payload. Once at least 16 heads are retained, the next page load clears any remaining history first. Heads that
have only delta data may leave history untouched until such a load because they add no decoded page. Initialization
always clears unused history when it finishes or fails. The initialized heads retain only their current pages,
bounded by the admitted 512 KiB reservation; one replacement page can be transiently present during decoding and
cross-page validation. These tuple payload bounds exclude object headers, copied keys, scalar stream state, and
the codec's separately bounded native scratch.

All heads share the existing page cursor and, when needed, the existing delta cursor. A head copies its page and
delta fences; before advancing either shared cursor, it restores that fence and requires an exact key match. It
retains detached tuples and scalar delta state, never a mapped value or a native cursor position. Page decoding,
delta validation, and adjacent-page ordering checks remain unchanged. Accepted admission closes the directory
cursor; no per-predicate native cursor is opened.

Explicit seeks close the heads and restart the 128-row admission budget. Snapshot renewal constructs a fresh
iterator and admission state. Short scans, scans with bound node/predicate/neighbor fields, mutable writers, and
catalogs exceeding any admission limit retain their existing traversal. Failures and cancellation release the
detached heads before the raw iterator closes its shared cursors.

### Shared managed page cache

`PointPageCache` supplies both exact membership checks and managed scans, and is separate from `ReadPageCache` and
the node-merge heads. Across managed read-only snapshots, each backend instance retains at most 128 ready decoded
pages and at most 512 KiB of their tuple payload. Point and scan entries consume the same budget. This shared
retention is additional to each active iterator's bounded local or head state; it is not a total reader-memory
limit. A shared page contributes one set of arrays even when several readers hold references to it. The payload
charge is three eight-byte columns per tuple; copied keys, catalog entries, and object headers are additional
bounded state. An access-ordered map evicts entries until both limits permit a new page.

The map uses exact keys consisting of the transaction manager's dedicated owner token, the active native
transaction ID, and the complete physical page fence. Equality checks the owner's identity, native ID, and every
fence byte; a fingerprint supplies only the hash code. This permits sharing between reader leases that see the
same native snapshot, while keeping managers, backend instances, snapshots, planes, and predicates distinct. The
cache holds no transaction wrapper, native cursor, mapped buffer, or reader lease.

A separate, fully associative array of 64 primitive fingerprints records admission hints. These hints retain no
owner token, page key, or decoded data. A fingerprint match can request validation and decoding, but cannot answer
membership. Collisions can therefore cause an earlier decode without returning another page's contents or
weakening the exact ready-entry key comparison. The hint count and ready-page limits are independent.

Every eligible exact membership request still checks the visible delta first and performs a native predecessor
seek when base data is needed. A cached page does not replace those checks. On an unhinted miss, the page is fully
probed and its first tuple is checked against its fence before a hint is published. A hint match decodes and
validates the requested page, including its physical fence, before publishing detached tuple columns. Subsequent
accesses to that exact ready entry use bounded tuple lookup. Eviction can restart this admission sequence.

For a managed scan, `SnapshotPages` captures only the shared cache and its owner/native-ID identity under the
existing transaction guard. A local page-cache miss consults the shared map after the native cursor has located
the exact physical page. A ready hit supplies the private decoded columns. Otherwise the scan decodes the page
and validates its fence; an unhinted load publishes only a primitive hint, while a hinted load can publish the
already decoded page. One-shot scans therefore follow the same bounded admission policy as point reads.

Predicate-first streams use this loader through their local cache. Promoted node-merge streams retain the loader
after dropping local cache participation, so later pages can reuse shared columns without retaining historical
pages. Cursor positions, next-page certificates, tuple offsets, and delta state remain private to each iterator.
Shared reuse does not replace page seeks or advances, cross-page ordering checks, or the visible delta merge.
Backend readers use ordinal column access and copy values into their own reusable quad or caller-supplied batch;
cached arrays never escape those private readers. Exact managed point iterators allocate neither a local page
cache nor a scan loader.

Lookup and publication synchronize the small shared catalog; validation and decoding occur outside that monitor
while the caller holds the native read barrier and transaction guard. Publication rechecks the complete identity,
and a late hint cannot replace an already decoded page. Concurrent misses may perform duplicate validation, but
only guarded, fully validated data can be published. Cancellation is checked before publication.

Raw native-transaction calls, foreign transactions, and mutable writers do not receive a shared point-cache
identity. Managed readers must pass the existing lease and pinned-snapshot checks before access. An ordinary
renewal rebuilds the scan loader with its newly active native snapshot identity; entries for an older snapshot
cannot answer a newer one.
Detached entries for inactive snapshots may remain until bounded eviction, without retaining those readers or
their mapped pages. This cache does not use interval certificates or cached delta-absence proofs.

### Work and consistency checks

The storage schema, page envelope validation, base-plus-delta authority, and atomic publication of both directions
remain unchanged. `LmdbPersistentAdjacencyReadWorkTest`, `LmdbPersistentAdjacencyReadCacheTest`, and
`LmdbPersistentAdjacencySeekWorkTest` check actual native work alongside exact results, ordering, residual filters,
overlay behavior, bounded eviction, renewal, writer visibility, and failure cleanup. Those work assertions are
separate from benchmark timing; this description makes no throughput or latency claim.

The delayed merge has focused work and behavior cases in `LmdbPersistentAdjacencyNodeMergeWorkTest`,
`LmdbPersistentAdjacencyNodeOverlayWorkTest`, and `LmdbPersistentAdjacencyNodeMergeCapacityTest`. The managed point
cache has corresponding checks in `LmdbPersistentAdjacencyManagedPointCacheTest` and
`LmdbPersistentAdjacencyAdaptivePointCacheTest`, including separate capacity limits, concurrent publication, and
fingerprint collisions. `LmdbPersistentAdjacencyManagedScanCacheTest` checks repeated complete scans, shared point
and scan capacity, post-promotion page crossings, raw/writer exclusion, returned-value isolation, malformed
replacements, and reader retirement and renewal. `PackedLongVectorSequentialReaderTest` and the codec compatibility
tests cover decoded values, block boundaries, malformed data, and native lifetime. `PersistentCsfPageCodecEncodingWorkspaceTest` covers
workspace reuse and independent output. `LmdbPersistentAdjacencyCompactionWorkTest`,
`LmdbPersistentAdjacencyCompactionAllocationTest`, and `LmdbPersistentAdjacencyCompactionFailureTest` cover actual
native work, bounded allocation, and failure recovery. Their existence does not establish a completed verification
run or a measured performance gain; exact reports and benchmark results belong with the change's evidence.

## Migration and configuration compatibility

Opening an existing permutation-index generation imports its committed explicit and inferred quads into adjacency
storage. The native index manifest takes precedence; legacy properties and native discovery provide the fallback.
Unmanifested discovery chooses populated authority and rejects incompatible populated alternatives.

The migration write transaction creates adjacency data and counts, compacts its initial pages, removes all legacy
statement permutation databases and the old context database, writes the adjacency format marker, and publishes
the virtual scan-order manifest. Failure before commit leaves the legacy authority intact for a later retry.

Before migration starts, a larger explicitly configured `tripleDBSize` is page-aligned and applied without shrinking
the existing map. With `autoGrow` enabled, a map-full attempt aborts and retries the complete migration after
successful native map growth; overflow or failure to increase capacity fails cleanly. With `autoGrow` disabled,
migration does not grow beyond the resulting native map capacity, and a map-full migration leaves the legacy
statement authority intact. Migration uses this complete retry loop rather than ordinary mutation-journal replay.

Migration is not compatible with older writers that recreate missing permutation databases. After successful
read-only admission, a dictionary-format-2 store receives a forced `lmdbrdf.ver` file containing exactly
`rdf4j-lmdb-persistent-adjacency-v1` followed by a newline, before the writable backing store opens. Older writers
reject that file before opening native storage. Modern dictionary-format-7 stores retain their existing core
literal-reference rejection marker. Dictionary versions and literal encodings are unchanged.

The adjacency marker may remain after an interrupted migration, including before an unversioned legacy store's
first properties save. The current reader recognizes only its exact contents and runs normal native admission
before retrying. The marker does not authorize missing environments, malformed catalogs, or reinterpretation of
modern dictionary data as legacy. Unknown marker contents are rejected without repair. Marker file and parent
directory forcing use the same platform behavior as the existing core literal-reference marker.

Virtual `TripleIndex` descriptors provide four streaming orders: predicate-first `psoc` and `posc`, and node-first
`spoc` and `opsc`. Unbound node-first scans begin with the type `4` directory and can admit the bounded predicate
merge described above; both paths read the same persisted outgoing or incoming pages and deltas. None owns a native
statement-index handle. The native manifest names `psoc,posc`, the two adjacency directions. The `triple-indexes`
property retains the caller's logical scan-order preferences so a
later reopen without explicit configuration preserves those capabilities.

The legacy `tripleIndexes` configuration is still checked for valid syntax and retains its logical order
preferences. Configured orders outside the four streaming orders, such as `cspo` and `spco`, read matching
adjacency rows and use a bounded external sort. These views preserve the requested order without creating
physical permutation databases or changing the adjacency format. Their cost includes the complete matching input
and sorting, even when the caller requests a narrow output range. Prepared bulk input may supply multiple sorted
tuple orders; repeated quads are inserted idempotently and context counts are validated against the resulting
adjacency metadata.

Opening an incomplete adjacency database set, an unknown adjacency version, or a mixture of adjacency and legacy
statement authority is rejected. Ordinary admission checks the bounded schema and native generation metadata.
`LmdbStore.verifyStoreOffline` performs exhaustive verification: it decodes every physical page, applies every
physical delta independently of row directories, checks direction parity and dictionary references, and derives
the expected metadata. Large verification sets spill to disk. This also detects orphan data hidden by missing row
directories, malformed pages, incorrect counts, and inconsistent compaction work.

## Validation commands

Use the repository's required JDK and workspace-local Maven repository. Replace `<jdk-25-or-newer>` and any local
Maven setup with the toolchain available in the checkout. Run the root prerequisite build before focused tests.

```sh
export JAVA_HOME=<jdk-25-or-newer>
export PATH="$JAVA_HOME/bin:$PATH"
mvn -o -B -ntp -Dmaven.repo.local=.m2_repo -Pquick -DskipFrontend=true install
```

The focused storage, codec, iterator, migration, and admission checks are:

```sh
mvn -o -B -ntp -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb \
  -Dtest=LmdbPersistentAdjacencyContractTest,LmdbPersistentAdjacencyTest,LmdbPersistentAdjacencyAdmissionTest,LmdbPersistentAdjacencyFenceTest,LmdbPersistentAdjacencyIteratorTest,PersistentCsfPageCodecTest \
  -DskipITs -DforkCount=1 test
```

Existing snapshot, mixed-plane, map-growth, crash-recovery, and prepared-store tests should also be selected for the
affected integration paths. Record exact commands and their reports with the change; commands in this document do
not by themselves assert that a verification run completed successfully.

The optional `LmdbDevelopBaselineCompatibilityTest` additionally runs a separate writer built from pinned commit
`a7b7cedd1c954dc55fa8265f7e6fc4d215fdb1aa`. It verifies real legacy fixture migration, rejects later old-writer
attempts without changing the migrated files, and reopens them with the current reader. Running it requires both
`rdf4j.lmdb.develop.baseline.classpath` for that external build and `rdf4j.lmdb.develop.fixture.root` for its evidence
directory. A normal module test run skips those process tests when the external classpath is absent.
