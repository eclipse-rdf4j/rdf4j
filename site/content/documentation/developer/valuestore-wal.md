---
title: "Designing a Write-Ahead Log for the ValueStore"
layout: "doc"
toc: true
autonumbering: true
---

## Motivation and overview

The NativeStore maps each RDF value (subject, predicate, object, and context) to an integer identifier that is stored on disk. If these ValueStore files become corrupt, the mapping between identifiers and values can be lost. RDF4J can repair corrupt indexes with the `softFailOnCorruptDataAndRepairIndexes` flag, but it cannot currently restore lost identifier-to-value mappings. A write-ahead log (WAL) records every logical change before it is applied to the main store and can be replayed to recover the mappings. For the ValueStore this means logging every minted identifier together with its RDF value, enabling recovery if the underlying files are damaged.

The design goals for a ValueStore WAL are:

- **Fast and append-only.** Log entries are written using sequential I/O and never rewritten so that appends remain inexpensive.
- **Asynchronous and non-blocking.** ID minting should not block on disk I/O; logging work is delegated to a background component that performs asynchronous writes.
- **Durable.** Written log entries are flushed to persistent storage using `fsync`/`FileChannel.force()` to survive crashes. Group commit batches multiple entries to amortize flush costs.
- **Simple serialization.** JSON Lines (NDJSON) is the baseline format thanks to its readability and mature tooling, though binary alternatives are evaluated below.

## General write-ahead logging principles

WAL is a well-established technique in systems such as PostgreSQL, RocksDB, SQLite, and LSM-tree engines. Key principles include:

1. **Log before applying changes.** Each logical change is appended to the WAL before the main data structures are updated so that recovery can replay the change after a crash.
2. **Append-only sequential I/O.** WALs rely on sequential writes, which are faster than random I/O, and are therefore kept append-only.
3. **Atomicity and durability.** Each record must be written atomically and flushed to durable storage with `fsync`/`FileChannel.force()`.
4. **Checkpointing and compaction.** Periodic checkpoints apply the WAL contents to the main store and prune or roll over log segments.

## Capturing ValueStore mint operations

ValueStore methods such as `storeValue` mint identifiers when new RDF values are inserted. WAL support requires:

1. **Intercepting the ID assignment.** Extend the ValueStore so that once an ID is minted and persisted, a WAL record is produced with the identifier and its RDF value.
2. **Defining the WAL record structure.** Each entry contains a monotonically increasing sequence number or timestamp, the minted identifier, the lexical value, value type (IRI, blank node, literal), and literal-specific datatype and language fields. A checksum is optional for integrity verification.
3. **Appending to the WAL.** The serialized record is appended to a dedicated log file (for example `valuestore.wal`) without interfering with the existing B-tree indexes used for lookups.

## Serialization format options

### JSON Lines / NDJSON

JSON Lines stores one JSON object per line and is widely used for append-only logs. Advantages include:

- Stream-friendly and easily appendable, allowing parallel or partial recovery.
- Works with standard tooling such as `grep`, `jq`, and log aggregation systems.
- Schema flexibility that tolerates additional fields.

Drawbacks are the space and CPU overhead of textual encoding, lack of enforced schema, and slower parsing compared to binary formats. Compression (for example, `.jsonl.gz`) mitigates size overhead. JSON Lines is recommended as the default because of its simplicity and debuggability.

### Binary alternatives

Binary formats such as Protocol Buffers or Apache Avro encode data compactly and provide strong schema evolution features at the cost of added dependencies and reduced human readability. A custom binary layout—length-prefixed records with checksums—achieves minimal overhead and fast sequential writes, similar to RocksDB’s WAL, but lacks standard tooling. Binary formats are suitable when disk space or extremely high throughput is critical; otherwise JSON Lines is sufficient.

## Asynchronous logging strategies

### Why asynchronous?

Synchronous logging forces the minting thread to wait for disk I/O and increases latency. Asynchronous logging decouples the application thread from disk writes so that the caller enqueues a record and proceeds immediately.

### Implementation sketch

- **Threading model.** Introduce an `AsyncWalWriter` that accepts WAL entries through a bounded thread-safe queue. Minting threads enqueue records; a dedicated writer thread drains the queue, serializes entries, and appends them to the WAL.
- **Asynchronous I/O.** Java NIO.2’s `AsynchronousFileChannel` can perform non-blocking writes, though implementations may rely on background executors. The interface allows the caller to continue without waiting for completion.
- **Back-pressure.** The queue has a configurable capacity. When full, producers either block, drop to synchronous writes, or signal pressure to callers.
- **Durability.** The writer batches pending entries and flushes them with `FileChannel.force(true)` or `fsync` at configurable intervals to guarantee durability.

### Synchronisation strategies

1. **Immediate flush.** Flush after every record for maximum durability at the expense of throughput.
2. **Frequency-based flush (group commit).** Batch multiple entries and flush periodically (for example every N entries or milliseconds) to amortize fsync costs, similar to LevelDB, RocksDB, and PostgreSQL.
3. **Asynchronous commit.** Optionally acknowledge commits before the flush completes, trading durability for throughput. Unflushed entries are lost on crashes, so this mode must be opt-in and clearly documented.

## Recovery and maintenance

### Crash recovery

1. **Locate the WAL.** On startup the ValueStore finds the latest WAL segment (for example `valuestore.wal` plus rolled segments).
2. **Replay entries.** Parse the WAL from the beginning and rebuild a map of identifiers to values. Missing or divergent values in the store are rewritten.
3. **Handle duplicates.** Sequence numbers or timestamps ensure the newest record for each identifier wins.
4. **Verify integrity.** Optional checksums protect against log corruption.

### Log rotation and compaction

The WAL must not grow indefinitely. Checkpoints flush cached state, guarantee that log entries are materialized in the primary files, and then roll the WAL to a new segment. Old segments are archived or deleted once they are safely incorporated. This mirrors checkpointing in systems like SQLite and PostgreSQL and also enables point-in-time recovery.

## Summary of approaches

- JSON Lines is a strong default thanks to its simplicity and tooling support, while binary formats should be considered when throughput and disk efficiency dominate.
- Asynchronous logging with group commit balances durability and performance for ValueStore ID minting.
- Recovery relies on replaying WAL records and periodic checkpoints to bound log size and ensure durability.

