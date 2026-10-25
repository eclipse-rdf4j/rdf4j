# Restore fast LMDB loading for NONE imports and cheap alternative-IRI checks

This ExecPlan is a living document maintained according to `.agent/PLANS.md` at the repository root. Keep `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` current as work proceeds. Label: GH-6205 (the current branch `GH-6205-lmdb-sparqloscope-native-execution`).

## Purpose / Big Picture

The LMDB theme benchmark (`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`) rebuilds its store when the store is deleted. It does this by adding 14,561,110 generated statements through `RDFInserter`, in a single connection transaction started with `IsolationLevels.NONE`. On 2026-10-13 this load took about 10 seconds. At HEAD `a9e2b3ed86` it takes 61 to 66 seconds, against a target of under 15 seconds. After this work the same load should take well under 30 seconds, ideally close to the old 10. Loads into stores that already hold data should also do far fewer dictionary lookups per new IRI.

The user observes the change by deleting `core/sail/lmdb/target/lmdb-theme-query-benchmark` and running the benchmark, or by running the standalone harness described under Concrete Steps. The harness prints `LOAD TOTAL <seconds>`.

Milestones 1 to 3 brought the load to about 30 seconds. Milestones 4 to 7 add the "initial run import", which the user asked for after scratch spikes reached 13.0 to 13.5 seconds. When the first write transaction ever made against an empty store is an explicit NONE transaction, the store holds a store-wide lock for the whole import. While it holds that lock, it writes no triple index entries and no value lookup records. Instead it collects them in memory and sorts and spills them as run files on background threads; a run file is a sorted batch of rows on disk. When the import ends, it merges the runs and writes every index in one pass. Users see a much faster first load. Other connections wait until that load is complete, instead of watching it grow. The importing connection can still read its own data: any read, flush, removal or rollback by it first completes the import.

## Progress

- [x] (2026-10-25 14:30Z) Measured and bisected the regression (Routine C spike, no production changes). Results are in Surprises & Discoveries.
- [x] (2026-10-25 16:10Z) Milestone 1. Red: `ValueStoreIdCacheTest#bulkCreationProbesEachAbsentNamespacePrefixHashOncePerBatch` counted 12,400 alternative probes for 400 sequential IRIs. Green after `BatchIdStorer.absentRecordHashes`. `ValueStoreIdCacheTest` 45, `ValueStoreTest` 147 and `ValueStoreNamespaceCacheTest` 1 pass. The harness NONE load went from 61.1 s to 39.0 s.
- [x] (2026-10-25 16:50Z) Milestone 2, implemented as fresh value assignment inside GH-6070's NONE chunks rather than buffering (see Decision Log). Red: `LmdbInitialNoneFreshImportTest` (2 of 3 failed: the session was never active). Green: 3 of 3. Spoil guard red/green: `ValueStoreIdCacheTest#ordinaryInsertSpoilsAnActiveFreshValueSession` fails with the `nextId` hook removed.
- [x] (2026-10-25 17:20Z) Milestone 2 regression suites green: `LmdbNoneStreamingIngestionTest` 89, `LmdbInitialNoneImportAccelerationTest` 38, `LmdbPreparedBulkStoreTest` 38, `FreshValueReservationTest` 8, `LmdbBatchedIngestionTest` 17, `LmdbInitialNoneFreshImportTest` 4 (including a fresh-chunk MAP_FULL replay). Two `LmdbBatchedIngestionTest` methods inject failures through `ValueStore.storeValues` on an initial NONE import. They now seed the store first, so they keep exercising the lookup path they were written for.
- [x] (2026-10-25 17:25Z) Harness NONE load with both fixes: 30.0 s at `-Xmx8g` and 29.5 s at `-Xmx16g` (was 61–66 s). `values/data.mdb` is 608 MB.
- [x] (2026-10-25 16:20Z) Committed and pushed as `cca0caf831`. At the user's request, CI replaces the local full-module run. The previous merge commit's CI had two environmental LMDB failures: a parallel strategy declined with `range-partition-disabled`, and an overlay warm-up assumption reported inside `assertAll`.
- [x] (2026-10-25 16:45Z) Remaining-cost experiments (scratch worktree, not committed), all at `-Xmx16g` with all four indexes:
  - NONE chunk limits from 262,144 rows / 64 MiB up to a single chunk: 29.5 s down to 26.6 s.
  - NONE batch size (`bulkOperationSize`, default 1,024, which sizes each `PendingNoneBatch`) of 16 k to 262 k: 26.3–28.0 s.
  - Index count: `spoc` alone 12.9 s (645 MB), `spoc,posc` 17.3 s, all four about 29.5 s (3.0 GB).
  - READ_COMMITTED's fresh path takes 17.2 s, against 10 s on 2026-10-13. Its extra time is `Changeset`/`DynamicModel` buffering and write-preflight estimates, which NONE does not use.
- [ ] Milestone 3: re-measure the theme load, run the full `core/sail/lmdb` suite, format, commit and push. (Superseded in part: the user asked for Milestones 4 to 7 first. The full-suite run moves to Milestone 7.)
- [x] (2026-10-25 20:30Z) Mapped the code that the run import touches, with four read-only investigations. The results are in Context and Orientation under "Initial run import".
- [x] (2026-10-25 20:45Z) Milestone 4 code written: `LmdbSortedRuns`, `LmdbRunCollector` and `LmdbDeferredValueLookups`, with `LmdbSortedRunsTest` and `LmdbRunCollectorTest`. Not yet run.
- [x] (2026-10-25 21:10Z) Milestone 4: `LmdbSortedRunsTest` 2/2 and `LmdbRunCollectorTest` 2/2 pass.
- [x] (2026-10-25 21:25Z) Milestone 5. `LmdbInitialRunImportTest` was written first. It failed to compile until the classes existed; after implementation, 7 of 9 passed. The two failures were:
  - The forfeit rule missed a buffered READ_COMMITTED first writer that never started a native transaction. Fixed with `LmdbStoreConnection.markStatementWriteAttempted` → `LmdbSailStore.noteStatementWriteAttempt`.
  - A reader test hit a limitation that predates this work (see Surprises).
  Now 9/9.
- [x] (2026-10-25 21:35Z) Milestone 6 (marker + `LmdbStore.initializeInternal` reset), covered by `aLeftoverMarkerResetsTheStoreToEmptyOnStartup`.
- [x] (2026-10-25 21:50Z) Theme load with the real implementation: 13.7–14.1 s (import about 10.5 s, commit 3.2–3.4 s). `triples/data.mdb` is 1.75 GB and `values/data.mdb` 649 MB, and verification in a fresh JVM passes. Two fixes were found by tracing:
  - namespace reads and writes no longer complete the import;
  - the final merge's value half now runs while the triple half appends.
- [x] (2026-10-25 21:55Z) Committed and pushed the run import as `fcebf87b1c`.
- [x] (2026-10-25 22:40Z) Edge-case coverage: `LmdbInitialRunImportTest` grew to 22 tests, and `LmdbDeferredValueLookupsTest` was added. New cases:
  - inferred rows in the import transaction;
  - a full fresh session, through the new `freshNoneValueLimit()` seam;
  - many small runs with duplicates across runs, through `LmdbInitialRunImport.chunkBytesForTest`;
  - mixed value kinds (triple terms, long and short IRIs, language and typed literals, empty strings, embedded NUL);
  - rollback without a checkpoint, closing the connection mid-import, and a failed completion;
  - listener notifications compared with the switch off, and empty NONE transactions;
  - clearing a context mid-import, namespaces, and a second connection on the importer's thread;
  - adjacency and overlay rebuilds after the merge.
  Two defects were found and fixed:
  - listener connections now never claim the import;
  - the collector allocates its chunk buffers lazily.
  All 28 run-import tests pass.
- [x] (2026-10-25 22:50Z) User request: the bulk loader's default statement indexes are now `spoc,posc,ospc`, in both `StatementIndexBulkRecords.DEFAULT_INDEXES` and `LmdbNativeBulkStore.DEFAULT_TRIPLE_INDEXES`. `LmdbBulkLoaderDefaultIndexesTest` failed before the change and passes after; the bulk package passes 500 tests with 1 skipped. The store's own default (`TripleStore.DEFAULT_TRIPLE_INDEXES`) is unchanged.
- [ ] Milestone 7: full `core/sail/lmdb` suite. The first attempt was stopped: IntelliJ's build wrote into the same `target/` directories, so test classes vanished mid-run. It needs a rerun with nothing else building this checkout.

## Surprises & Discoveries

- Observation: the regression first appeared at `60e509b4ef` (2026-10-14, "Merge develop into optimize-lmdb"). That merge brought in GH-6070 (#6073) `eec180ddc6`, "Stream NONE ingestion through bounded native batches".
  Evidence: `git bisect --first-parent` over the same load harness. `99d9adec6c` took 10.0 s (7.2 s of it in commit); `60e509b4ef` took 598 s; `3bae6620c2` (2026-10-19) took 67 s; HEAD takes 61–66 s.
- Observation: a NONE write from a connection sets `LmdbSailSink.nativeCheckpoints` (`LmdbSailStore.java`, the `LmdbSailSink` constructor). That flag routes `approve()` to `addStatement()` and makes `shouldBufferPreparedApprovals()` return false. The fresh value session (`ValueStore.startFreshValueSessionIfEmpty`) is likewise skipped when `nativeCheckpoints` is set. A NONE import into an empty store therefore never takes the fast path. That path assigns new value IDs without any dictionary lookup (`ValueStore.persistPreparedValues`) and writes all four indexes in one globally sorted pass at commit (`TripleStore.storeGloballyOrderedFreshTriples`). The same HEAD code started with `IsolationLevels.READ_COMMITTED` loads the data in 18.1 s, and its `triples/data.mdb` is 1.6 GB instead of 3.15 GB.
- Observation: half of the streaming NONE time is spent in `ValueStore.AlternativeIriLookup.find`. This lookup runs for every new IRI and checks whether the same IRI text is already stored under a different namespace/local-name split. `MAX_KEY_SIZE` is `ValueStoreRecordCodec.MAX_INLINE_KEY_BYTES` (16), so every IRI longer than 15 bytes probes one hashed namespace key (`HASH_KEY` plus the CRC32 of the prefix) for each length from its full length down to 16. The namespace-proof cache that could shorten this is disabled during writes: `beginValueLookupMutation()` sets `valueLookupGeneration = null`, so `AlternativeIriLookup.scope` is null. In the theme data, 3,221,528 distinct IRIs average 46 bytes, which is about 31 probes each. Skipping the lookup entirely (a scratch switch, not a fix) cut the load from 61.1 s to 31.3 s.
- Observation: why 2026-10-13 was fast. Its NONE sink buffered the whole transaction in `pendingPreparedApprovals`; the adds took about 3 s. At commit it assigned every value fresh and wrote each index in one globally sorted pass (`storeGloballyOrderedFreshTriples`, plain `mdb_cursor_put` with a reused cursor and no append flag), while three `LmdbIndexPrepare` threads sorted and encoded the keys. That writer thread was busy only 1.4 s: 0.7 s of puts for four indexes of 14.6 M rows, plus one 0.6 s commit. Today's streaming writer is busy 20.3 s: 13.0 s of puts and 6.5 s across about 56 chunk commits. Its rows are sorted only within 1,024-statement `PendingNoneBatch`es, each scattered across the existing B-trees, so pages are cache-cold, split half-full (3.0 GB instead of 1.6 GB), and copied again in every chunk transaction. The price of the old path was memory: peak heap before GC was 3.95 GB, against 1.88 GB now (both `-Xmx8g`), and it grows with the import. The fast path was switched off by the 2026-10-14 merge resolution (`60e509b4ef`), which added `|| nativeCheckpoints` gates to `shouldBufferPreparedApprovals()`, `approve()` and the fresh-session start in `startTransaction`.
  Evidence: `good-load.jfr` versus `fix12-load.jfr`, weighting Java samples at 10 ms and native samples at 20 ms.
- Observation: the LMDB bulk loader is slower on this data. For a like-for-like run, the generator streamed statements straight into `CanonicalStatementStager.writeStatement` through a scratch-only `LmdbBulkLoaderEngine.loadStatements` entry point, with no RDF file and not committed. The load took 72.0 s, and the reopened store matched the 14,561,110 distinct generated statements (digest and per-context counts). The files are dense (`triples/data.mdb` 1.63 GB), peak RSS was 4.1 GB, and CPU time was 173 s user. Phases: `STAGE_INPUTS` 20.1 s (single-threaded for statement input), `BUILD_MAPPED_DICTIONARY` 5.5 s, `RESOLVE_IDS` 16.4 s, `BUILD_NATIVE_RUNS` 5.0 s, `WRITE_GENERATION` 20.9 s. Uncompressed staging (71.6 s) and an 8 GiB memory budget (76.9 s) made no material difference. The time goes to staging to disk, LZ4 and XXHash, and external merges, not to LMDB puts (about 2 s). From a 2.7 GB N-Quads file with parallel staging it took 57.5 s.
- Observation: deferred secondary-index spike (scratch-only `TripleStore.buildSortedFromMainForSpike`, single-threaded, raw native transactions). A `spoc`-only NONE load takes 13.3–15.3 s. Building `ospc`, `psoc` and `posc` from it takes:
  - in 128 MB chunks (4 chunks): 10.3 s (read 0.5, sort 2.6, write 7.1 s), with `triples/data.mdb` 3.07 GB, because later chunks insert into B-trees that already span the key range;
  - in one chunk: 8.9 s (read 1.7, sort 2.5, write 4.4 s), 1.96 GB.
  The existing background `StatementIndexBuild` rebuild (256-row batches, one transaction each) took 57.5 s. User constraints for the real design: hold a store-global lock for the whole initial load including the index build, and take it only if the store's first-ever write transaction uses NONE; any other first writer invalidates the option for the rest of the store's life.
- Observation: parallel variants of the deferred build, building three indexes from `spoc`. Each index is sorted on its own thread with a stable four-field LSD radix sort, and one thread writes:
  - serial, 128 MB chunks: 10.3 s;
  - parallel sort with off-heap pre-encoded keys: 7.7 s at 128 MB (write 4.9 s), 5.3 s in one chunk (write 2.3 s);
  - parallel sort producing `int[]` permutations over four `long[]` columns, keys encoded by the writer: 9.3 s / 6.4 s;
  - the user's layout, one interleaved `long[]` of up to 128 MB plus one `int[]` permutation per index, keys encoded by the writer: 8.8 s at 128 MB (read 1.1, sort 2.5 thread-seconds, writer waited 0.9, write 6.8 s) and 6.1 s in one chunk (write 4.0 s).
  The single writer limits every variant. Encoding keys there costs about 1.5–2 s, and inserting later 128 MB chunks into populated B-trees about 2.5 s more than one append-order pass. `triples/data.mdb` is 3.07 GB with 128 MB chunks and 1.96 GB with one chunk.
- Observation: the textbook solution wins. That is an external merge sort with a bottom-up append load (`MDB_APPEND`, as `mdb_load -a` does). The scratch spike `ExternalIndexBuildSpike`:
  - reads the main index in parallel ranges, each with its own read transaction and 1/P of the 128 MB budget;
  - sorts and spills each (range chunk, index) run with a stable four-field LSD radix sort;
  - runs one heap k-way merge thread per index, encoding keys into 4 MB blocks;
  - has one writer that appends blocks round-robin with `MDB_APPEND`.
  It builds `ospc`, `psoc` and `posc` (14,561,110 entries each, verified) in 2.81 s with 12 scanner threads: runs 0.92 s, merge and write 1.84 s, of which writer puts are 1.66 s. Each index gets 26,658 leaf pages against 39,246 for normally written `spoc`, and `triples/data.mdb` is 1.96 GB. Other results:
  - single-threaded scan: 3.55 s (12 slices), 3.75 s (4 slices), 4.29 s (1 slice);
  - without `MDB_APPEND`: 4.26 s;
  - one transaction instead of six: no change.
  The single LMDB writer (about 38 ns per append) is the floor.
- Observation: the `spoc`-only import (14.5 s) is bound by the main thread (12.7 s busy): `persistPreparedValues` 3.2 s, write-lock spinning in `Thread.yield` 2.3 s (the value-overlay warm-up thread was busy 3 s meanwhile), fresh assignment about 1.1 s, value commits 0.8 s, size estimates 0.4 s, and about 3 s of per-statement intake. The `spoc` writer thread is busy only 1.4 s.
- Observation: "all indexes from runs" spike (scratch worktree only: `RunBuilderSpike`, a hook in `flushNoneBatch`, `ExternalIndexBuildSpike.finishFromRuns`). During the NONE import, no triple index is written. Resolved quads fill two recycled 128 MB interleaved `long[]` buffers, and background threads sort and spill one run per (slice, index) for all four indexes. After commit, each index is k-way merged with duplicates skipped (the generator emits 14,772,659 statements, 14,561,110 distinct), context counts come from the `spoc` merge, and everything is appended with `MDB_APPEND` through `TripleStore`'s own transactions. Those transactions must be marked as native mutations, or `endTransaction` aborts them.
  Results:
  - verification in a fresh JVM passes: `spoc` digest, `size()`, 116 predicate-bound counts, 4,407 sampled object-bound counts, every index at 14,561,110 entries, 11 contexts;
  - `triples/data.mdb` is 1.75 GB;
  - time: import 13.5–14.8 s plus final merge and write 2.5–2.9 s (writer puts 2.2–2.3 s), 16.1–17.7 s in total;
  - sort and spill (3.2 thread-seconds) never made the import wait.
  The import's main thread is now the limit: value persistence 3.7 s, fresh assignment 1.7 s, value commits 1.2 s, size estimates 0.7 s, batch dedupe 0.6 s. Correction, found while timing the next spike: the 0.9 s of write-lock spinning and the 2.9 s value-overlay warm-up were not inside the import. `beginInitialNoneImport` already defers overlay maintenance. The rebuild started at commit (`rebuildAfterInitialNoneImport`) and overlapped the final merge, and the spinning was `size()` and shutdown waiting for it.
- Observation: "value lookups from runs" spike (scratch worktree only: `ValueRunSpike`, a hook in `ValueStore.persistPreparedValues`, `ValueStore.writeDeferredLookupsForSpike`). While a fresh session is active, `persistPreparedValues` writes only the ID-keyed records (prefix 5). Data-keyed records (key = value data of 16 bytes or less) and hash-keyed records (`HASH_KEY` / `HASHID_KEY`) go into 128 MB interleaved `long[]` chunks instead. Each record is four longs whose top bit is zero (7 + 7 + 2 key bytes plus key length, then the ID), so signed order is LMDB's memcmp order. Background threads sort and spill the chunks. After commit, a heap merge writes them in one value transaction, in parallel with the triple merge (separate environments, separate writers). The post-import adjacency and overlay rebuild is held until both merges finish.
  Results:
  - 5,138,290 records deferred (data 3,416,870, hash 1,721,420); dictionary lookups during the deferred window: 0, counted by tripwires in `findIdInTransaction`, `AlternativeIriLookup.findRecord`, `BatchIdStorer.findId` and the snapshot `findId`;
  - the main value database is byte-identical to the baseline for every key prefix (order-sensitive CRC per prefix). In `ref_counts`, 72 of 73 records match; the 73rd is the reference-count proof record, which stamps the native transaction ID, and the spike adds one value transaction;
  - verification in a fresh JVM passes, with the same checks as the previous spike;
  - time, 3 alternating runs each: baseline (triple runs only) import 13.7–14.4 s and total 16.3–17.1 s; spike import 10.2–10.6 s, value merge 1.12–1.24 s (hidden behind the 2.6–2.9 s triple merge), total 13.0–13.5 s;
  - cost: `values/data.mdb` 649 MB against 608 MB, and 39,505 leaf pages against 31,482.
  `MDB_APPEND` is impossible for any value record in an existing store. The value store's `dbi` is LMDB's unnamed main database, which also holds the named sub-database records (`gc_meta`, `ref_counts`, term index databases). Their keys are ASCII names that sort above every value key. So the deferred records go in as sorted plain puts, which split pages in the middle. Fresh IDs are also not one appendable stream: they come from per-type ordinals (`ordinal << 7 | type << 1`), so ID records arrive as interleaved ascending streams, one per value type.
  Main thread of the import afterwards (about 6.2 s of samples): fresh assignment 1.2 s, ID-record puts 1.0 s, size estimates 0.7 s, dedupe 0.7 s, `ThreadLocal` 0.6 s, value commits 0.06 s (was 1.2 s). GC pauses: 0.13 s.
- Observation: a limitation that predates this work. While any NONE transaction is open, an auto-commit read on another connection fails with `UnsupportedOperationException: This store does not support multiple datasets`. Reads inside a NONE transaction work.
  Cause: `LmdbStore`'s `SnapshotSailStore` hands out the backing `LmdbSailSource` while isolation is disabled. `SnapshotSailStore.datasetForRead` then calls `fork()` on it, and `LmdbSailSource.fork()` is unsupported.
  Evidence: a scratch probe with the run import switched off printed `PROBE failed: java.lang.UnsupportedOperationException: This store does not support multiple datasets`. This is not fixed here; the reader in `otherConnectionsWaitUntilTheImportIsComplete` reads inside a NONE transaction.
- Observation: `RDFInserter.endRDF` reads each namespace back (`getNamespace`) after every theme, and the generator sets namespaces. A namespace read went through `beginDatasetAcquisition`, and a namespace write's `LmdbSailSink.flush()` called `flushNonePending`. Both completed the import after the first theme, and the load took 31 s.
  Fix: completion moved out of `flushNonePending` into `LmdbStoreConnection.endUpdateInternal` (commit and explicit flush), reads (`beginDatasetAcquisition`, skipped inside namespace reads), removals and rollback.
  A second trace showed the NONE state being discarded after routine flushes. Completion then found no controller sink and the import failed at commit. While an import is active, `retainsInitialRunImportState` now keeps that state.
- Observation: a connection with `SailConnectionListener`s opens one NONE mutation probe dataset, `NoneMutationDataset`, and keeps it across many adds. That dataset reads the pending batch and the open write transaction, so rows moved into runs after the claim were invisible to it. Duplicate adds were then notified: 2,000 events with the run import against 1,000 without.
  Fix: `claimInitialRunImport` skips connections for which `notifiesStatementListeners()` is true. If listeners are added later, the next probe acquisition (`enterNoneMutationRead`) completes the import first.
- Observation: a limitation that predates this work. With two NONE connections on one thread, the second one's `commit()` waits forever for the writer lease that the first connection's sink keeps while it holds pending rows. It also does not see those rows: its `size()` returns 0. A scratch probe with the run import switched off timed out the same way. With the run import on, the second connection's read completes the import, and then sees all rows. The test commits the importer first.
- Observation: single-statement NONE transactions (`TransactionsPerSecondSyntheticBenchmark.transactions`) were 2.9× slower than 6.1.0: 13,744 against 40,397 ops/s in Docker with JFR, and 17,818 against 30,751 locally. The benchmark was copied into a 6.1.0 worktree, with only an unused import removed. The three causes, measured locally against about 31,000–32,000 ops/s for 6.1.0:
  1. `AsyncSignal` parked immediately on every caller and writer hand-off. 6.1.0 spun with `Thread.yield()`. A three-tier wait fixes it (4,096 `Thread.onSpinWait()`, then 256 `Thread.yield()`, then park): 18,170 → 24,628 ops/s. The tier-size grid is in the `AsyncSignal` class comment.
  2. A final batch that opens its own chunk still handed off to the ingest worker three times. It now runs on the caller thread (`flushNoneBatch(true)` from `flushNonePending`): about 28,600–30,000 ops/s.
  3. The value store committed every transaction: 1.00 commits per transaction, against 0.22 in 6.1.0, counted with `mdb_env_info().me_last_txnid`. Each commit also rewrote the next-ID, reference-count and proof records. `ValueStore.commitIfChanged()` now aborts an unchanged writer, and `writerChangedNothing()` lists the conditions: 30,049 → 37,352 ops/s.
  With these and the `sortedStoreOrder` scratch fix, the result is 38,769 ops/s locally against 30,986 for 6.1.0, and 55,548 ± 3,144 against 40,397 ± 6,559 in Docker. The remaining time is 51% LMDB commits (mostly `pwrite` of the triple commit's dirty pages) and 28% thinly spread Java overhead. `MDB_WRITEMAP` measured 77,543 against 40,550 ops/s locally. It is not enabled, because with `forceSync` off an OS crash could then corrupt the store rather than only lose recent commits.
- Observation: the run import's completion took `sinkStoreAccessLock` before the writer lease. A peer sink of the same connection held the lease and waited for the lock, so `LmdbNoneStreamingIngestionTest#connectionOwnedNoneAliasCannotDisturbParkedPreparedGeneration` deadlocked. `completeInitialRunImportBeforeRead` and `rollbackInitialRunImport` now acquire the owner's writer lease first; leases are re-entrant per owner. With that fix, and with that class's streaming configs switched to `setInitialRunImportEnabled(false)`, all 89 of its tests pass. Its other 31 failures asserted GH-6070 streaming behaviour that the store-wide lock replaces.
- Observation: the first version of the dictionary-commit skip lost values. `QueryBenchmarkTest` failed with "A repository result value could not be resolved". Two writes were invisible to `writerChangedNothing()`. First, `persistPreparedValues` wrote value records without setting `nativeMutation`. Second, the fresh value session assigns per-type ordinals, so a chunk of only new literals can leave `nextId` unchanged. The skip now requires `nativeMutation` to be false and no active fresh session. `LmdbSmallNoneTransactionTest#freshValuesWithLowOrdinalsAreCommittedEvenWhenTheNextIdDoesNotMove` covers the second case.
- Observation: inferred rows, and batches too large for the fresh session's value limit, reached the run import and failed with "Values cannot be created outside the initial import's fresh session". `flushNoneBatch` now completes the import before such a batch, and `acceptingInitialRunImport()` requires explicit rows.
- Observation: running the final batch on the caller thread is only correct when that batch is the last one. Namespace flushes and other mid-transaction flushes still use the ingest worker. `flushNonePending(owner, true)` is called only from `LmdbStoreConnection.endUpdateInternal`.
  The latest local A/B, with all fixes: 6.1.0 31,325 ± 7,643 against 41,918 ± 1,906 ops/s. The theme load still takes 13.5 s and verifies.
- Observation: within one sorted value batch, adjacent IRIs share most of their prefix; on average 5.7 bytes differ from the previous IRI. The 3.2 million IRIs come from only 69 namespaces, and none uses a non-canonical split.

## Decision Log

- Decision: Fix the alternative-IRI cost with a cache that lives for one `ValueStore.BatchIdStorer` run (one `storeValues` call) and remembers the CRC values for which `HASH_KEY + crc` returned `MDB_NOTFOUND`. No persisted marker and no startup census.
  Rationale: a missing hash key means no dictionary record of any kind has that CRC, so the cached answer is exact, with no ambiguity when two prefixes share a CRC. The batch holds the native write transaction exclusively, so the only inserts that can invalidate an entry are the batch's own value inserts (which know their CRC) and namespace creation (rare, and handled by clearing the cache). A persisted "canonical splits only" marker would remove the probes entirely, but it needs a format/downgrade review (`ref_counts` marker conventions, legacy validation sweeps). A namespace census needs a full dictionary scan, because IDs carry their type in the low bits and legacy stores use global ordinals.
  Date/Author: 2026-10-25 / Claude.
- Decision: Keep GH-6070's bounded-memory guarantee for NONE. An initial NONE import into an empty store may buffer statements for the fresh-import path only up to a memory budget derived from the maximum heap. Past the budget it flushes what it has through the fresh path and continues with the existing bounded streaming.
  Rationale: GH-6070's stated purpose is that an explicit NONE transaction can ingest arbitrarily long streams without holding the whole stream in memory. The user asked for the fast path back for NONE loads into an empty store.
  Date/Author: 2026-10-25 / user via Claude.

- Decision: Fix 1 does not buffer NONE statements. It keeps GH-6070's pending batches, chunk checkpoints and capacity replay unchanged, and replaces only `LmdbSailSink.resolveBatchValues`' dictionary lookups (`ValueStore.storeValues`) with `ValueStore.assignFreshValues` plus `persistPreparedValues` (`storeFreshNoneValues`). The session starts in `startTransaction` when a NONE sink is the only NONE owner and the store is empty (`freshNoneImportOwner`). It ends (`invalidateFreshValueSession`) at removals, for inferred sinks, when another NONE owner appears, when `ValueStore.isFreshValueSessionUsable` reports an ordinary insert (`nextId` sets `freshValueSessionSpoiled`), past `FRESH_NONE_VALUE_LIMIT` (`maxMemory / 1024` values), and in `finishInitialNoneImport`.
  Rationale: about 70 GH-6070 tests assert paired checkpoints while streaming into empty stores, and buffering would also break the bounded-memory goal. Value lookups are the part of the streaming path the fresh session removes. The triple side keeps per-chunk aligned writes, and rollback already resets the session's provisional IDs (`LmdbSailStore.rollback()` calls `rollbackFreshValueTransaction`), so capacity replay re-assigns cleanly.
  Date/Author: 2026-10-25 / Claude.

- Decision: Build the "initial run import" into the store, enabled by default, behind a new experimental `LmdbStoreConfig.setInitialRunImportEnabled(boolean)` (RDF config predicate `lmdb:initialRunImportEnabled`).
  Rationale: the user asked for the complete design built into the store. Its main visible change is that other connections wait instead of watching the import grow. Some users, and the GH-6070 tests that assert progressive chunk visibility, need the old streaming behaviour, so a switch is necessary. Existing tests that are about streaming from an empty store turn the switch off rather than being rewritten.
  Date/Author: 2026-10-25 / user via Claude.
- Decision: The import may claim the lock only at the start of the store's first native write transaction. That transaction must be an explicit NONE transaction from a connection with no inferred data, it must be the only NONE writer, and it must start a fresh value session on an empty store. Any other first write transaction clears `initialRunImportAvailable` for the rest of the store's open life. After a restart, eligibility is re-derived from emptiness: `TripleStore.isEmpty()` plus `ValueStore.canStartFreshValueSession()`, which is false once any ID record exists. Namespace-only transactions are not "writers" for this rule, because they write neither statements nor values.
  Rationale: this is the user's rule ("only for the first transaction write if that transaction uses NONE; if any other writer happens first then the lock is invalidated"). Emptiness checks make the rule hold across restarts without a new persisted flag.
  Date/Author: 2026-10-25 / user via Claude.
- Decision: The store-wide lock is two gates, plus completion on any read by the importer. The writer gate sits at the top of `LmdbSailStore.acquireWriterLease`: any owner other than the importer waits there, and `tryAcquireWriterLease` returns null. The reader gate sits at the top of `LmdbStoreConnection.beginDatasetAcquisition`: other connections wait there. When the importing connection reads, flushes (`flushNonePending`), probes through `beginMutationDatasetAcquisition`, removes (`removeNoneStatements`), or rolls back, the import is completed first. Internal background readers (estimator sampling, statistics) are not gated. They see the committed ID records and empty indexes, which is a consistent "empty" view, and the sketch estimator is marked for rebuild when the import completes.
  Rationale: these are the choke points the code mapping found. Gating `createReadView` itself would also block the importer's own ingest worker and internal maintenance and risk deadlocks. Completing on the importer's own reads keeps read-your-writes intact.
  Date/Author: 2026-10-25 / Claude.
- Decision: The final merge runs as one last NONE chunk through the importer's own sink, using the existing machinery. It is a new `Operation`, `InitialRunImportFinish`, kept in `state.work`. Its value half writes the merged lookup records into the chunk's value transaction on the caller thread. Its triple half runs on the ingest worker and appends all indexes into the chunk's triple transaction with `TripleStore.appendInitialImportRuns`. `checkpointNoneChunk` then commits it with the normal dictionary-before-triple order. `MAP_FULL` in either environment goes through the existing `recoverNoneCapacity` replay, which re-runs the finish from the run files. They are deleted only after both environments committed and were synced.
  Rationale: this reuses the writer-ownership, replay, revision and publication bookkeeping instead of inventing a parallel commit path. The value and triple halves run in parallel because they are different LMDB environments with their own writer threads.
  Date/Author: 2026-10-25 / Claude.
- Decision: Value lookup records are written with sorted plain cursor puts, not `MDB_APPEND`.
  Rationale: the value store's main database is LMDB's unnamed database, which also stores the records of every named sub-database (`ref_counts`, `gc_meta`, …). Those keys sort above every value key, so an append is never legal. Triple indexes are empty, dedicated databases, so they do use `MDB_APPEND`.
  Date/Author: 2026-10-25 / Claude.
- Decision: A crash or failed completion resets the store to empty at the next start. Before the first chunk commits, the import writes the marker file `.lmdb-initial-import.incomplete` in the data directory with `CREATE_NEW`, then forces it and the directory to disk. After both environments have committed the final chunk and been synced with `mdb_env_sync`, it deletes the run files and then the marker. `LmdbStore.initializeInternal`, right after taking the directory lock, finds a leftover marker and deletes everything in the data directory except `lock/` and `config.ttl`, removing the marker last. If completion fails at runtime, the store refuses further reads and writes with a `SailException` until it is restarted.
  Rationale: an interrupted import leaves value ID records without lookup records. A later import would then miss every lookup and duplicate values. NONE gives no guarantee that a partial prefix survives, and the store was empty before the import, so empty is the correct recovery point. A file marker can be read before either environment opens and before any startup writer runs.
  Date/Author: 2026-10-25 / Claude.
- Decision: A rollback of the initial run import first aborts the open chunk and discards the unflushed batch. It then completes the import from the runs of the chunks already checkpointed.
  Rationale: GH-6070 tests assert that a NONE rollback keeps its already-durable prefix, and this keeps that contract.
  Date/Author: 2026-10-25 / Claude.

## Outcomes & Retrospective

The theme benchmark's NONE load drops from 61–66 s to about 30 s. The per-batch absent-hash cache accounts for 22 s of that, and fresh value assignment inside NONE chunks for another 9 s. The remaining limit is the triple-ingest thread, which is about 21 s busy: aligned index puts take 14 s and about 56 chunk commits take 6.5 s, while the main thread waits 5 s on it. Reaching the old 10 s would need fewer, larger NONE chunks during an initial import, or the globally sorted whole-import write. GH-6070's bounded streaming rules out the latter for NONE.

Lessons: the regression came in through a merge's conflict resolution (the `!nativeCheckpoints` gates), not through a single feature commit. A first-parent bisect over a standalone load harness found it in about 20 minutes, which a profile alone would not have.

What still separates NONE from the 15 s target is the three secondary indexes. Fresh subject IDs arrive nearly in order, so `spoc` alone loads in 12.9 s; each secondary index adds 4–8 s of scattered B-tree inserts and doubles the page count. Chunk and batch sizes do not fix that. The candidate design: during an initial NONE import, write only the main index, keep the secondary indexes out of the query manifest (the `StatementIndexBuild` manifest rule already keeps incomplete indexes out), and build them in sorted append order when the attempt completes.

Follow-ups noticed:
- The READ_COMMITTED fresh path still trusts a fresh session that persists across transactions without checking `isFreshValueSessionUsable`. An ordinary insert by another connection between two such transactions could make its map stale.
- `ThemeQueryBenchmarkSmokeIT` intermittently fails to validate a freshly reopened store ("intervening native LMDB writer"); this looks like post-ready maintenance racing the validation read.

## Context and Orientation

The LMDB store is `core/sail/lmdb`. `LmdbStore` owns an `LmdbSailStore` (`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`). That in turn owns a `ValueStore` (the dictionary: RDF terms mapped to numeric IDs, `ValueStore.java`) and a `TripleStore` (four LMDB indexes over ID quads, `TripleStore.java`). Writes reach the store through `LmdbSailStore.LmdbSailSink`.

There are two relevant write paths. The streaming path resolves each batch's values with `ValueStore.storeValues`, which runs a `BatchIdStorer`. That class looks every value up and stores new ones, calling `findAlternativeIriId` for new IRIs. The batch's triples are then queued to an asynchronous writer thread named `LmdbTripleStore-Ingest-0`. For NONE writes, native LMDB transactions are committed in chunks (`flushNoneBatch` and `checkpointNoneChunk`, bounded by `NoneWriteState.MAX_CHUNK_ROWS` = 262,144 and `MAX_CHUNK_BYTES` = 64 MiB).

The fresh-import path is used when the store is empty and the sink buffers approvals (`approvePreparedBuffered` into `pendingPreparedApprovals`). It starts a `ValueStore.FreshValueSession` and stores buffered batches through `storeFreshPreparedStatements`. Large batches go through `storeGlobalFreshPreparedStatements`, which assigns IDs from an in-memory map without lookups and writes globally sorted index blocks. Index entries go in with `MDB_NOOVERWRITE`, so a second batch into a now non-empty store is still correct.

An "initial NONE import" (`LmdbSailStore.beginInitialNoneImport` and `finishInitialNoneImport`) is already recognised when a NONE attempt starts on an empty store. It defers adjacency and value-overlay maintenance until the attempt ends.

The alternative-IRI lookup is `ValueStore.findAlternativeIriId` and its inner class `AlternativeIriLookup`. `findRecord(prefix, offset, length, hash)` probes `HASH_KEY + varint(hash)` with `probeIriAlternative` (package-private and counted in tests). If the key exists, it compares the record and walks the `HASHID_KEY` collision chain.

Initial run import, orientation. A NONE write streams through `LmdbSailStore.LmdbSailSink`.
- `appendNoneStatement` fills a `PendingNoneBatch`.
- `flushNoneBatch` turns it into a `BulkAddQuadsOperation`, adds it to `NoneWriteState.work`, starts the chunk's native transactions with `startTransaction(true)` and resolves values with `resolveBatchValues`. For an empty store, `storeFreshNoneValues` assigns IDs from the fresh session and calls `ValueStore.persistPreparedValues`. The batch is then queued to the single ingest worker thread with `submitOperation`, which ends up in `TripleStore.storeTriplesAligned`.
- `checkpointNoneChunk` commits a chunk through `commitPendingPublication`:
  - `drainAsyncOperations`;
  - `commitDictionaryBeforeTriple`, which runs `valueStore.commit()`, then `commitFreshValueTransaction` and `publishPreparedValues`;
  - `commitTripleTransaction`;
  - `finishCommittedDerivedState`;
  - `noneCheckpoints++` and `recordNoneCheckpoint`;
  - `noneWriteState.published()`.
- `MAP_FULL` during a chunk raises `LmdbNativeChunkFullException`. `recoverNoneCapacity` then rolls the attempt back through `rollback()`, which calls `rollbackFreshValueTransaction`, grows the maps (`growNoneMaps`), restarts the transactions, and re-runs every operation in `state.work`.

On commit, `LmdbStoreConnection.endUpdateInternal` calls `flushNonePending` (final flush and checkpoint). `closeResources` then runs `finishInitialNoneImport`, releases the NONE `txnLock`, and calls `completePostCommitValueMaintenance` and `rebuildAfterInitialNoneImport`. On rollback, `rollbackInternal` runs `discardNonePending`, `rollback(this)`, `finishInitialNoneImport` and the rebuild, and emits an event for the durable prefix.

Every added statement also needs its bookkeeping, as `TripleStore.storeGloballyOrderedFreshTriplesDirect` shows:
- one per-context count in the `contexts` database (`incrementAlignedContext`);
- `recordFanOutAdded` (in-memory fan-out statistics; cleared on abort);
- `synchronizeStagingQuad`, a no-op without a background index build;
- the `nativeMutation` and `tripleMutation` flags (a transaction without `nativeMutation` is aborted, not committed);
- the sketch estimator's `addStatement`, or `discardAndMarkForRebuild()`.

## Plan of Work

Milestone 1. First, in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreIdCacheTest.java`, add a test that stores a few hundred sequential IRIs in one namespace with one bulk `storeValues` call. It asserts that the number of `probeIriAlternative` calls is at most a small constant per IRI (four), where today it is about the IRI length minus 15. Then give `AlternativeIriLookup` an optional absent-hash set. `findRecord` consults it before the `HASH_KEY` probe and records CRC values that came back `MDB_NOTFOUND`. `BatchIdStorer` owns the set for one run, removes a CRC whenever it inserts a record under that `HASH_KEY`, and clears the set when a namespace record is created (a counter in `getNamespaceID`'s creation branch). The existing alternative-split and collision tests in the same class must stay green.

Milestone 2. First add a test in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbInitialNoneImportAccelerationTest.java` (or a new class) showing that a NONE connection import into an empty store goes through the fresh path. That is observable through `ValueStore` statistics, or through a package-visible counter of fresh prepared values. Then allow `shouldBufferPreparedApprovals()` and the fresh value session for `nativeCheckpoints` sinks during an initial NONE import, bounded by a heap-derived statement budget. When the budget is reached, flush the buffer through `storePreparedStatements`, end the fresh session and fall back to streaming. The GH-6070 streaming tests (`LmdbNoneStreamingIngestionTest`, `LmdbBatchedIngestionTest`) must stay green. Where they rely on streaming from an empty store, keep that behaviour above the budget, or seed the store so the import is not initial.

Milestone 3. Run the harness, then the full LMDB module.

Milestone 4: run files. Three new package-private classes live in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`.
- `LmdbSortedRuns` sorts rows of four longs by a field order, using `LeadingFieldSorters.lsdRadixSort` (stable, unsigned) once per field from the least significant up. It writes 32-byte rows to a run file, and `LmdbSortedRuns.Merge` k-way merges runs with a binary heap, optionally skipping duplicates.
- `LmdbRunCollector` fills a `long[]` chunk buffer, with two buffers recycled. It separates provisional rows (the open native chunk) from committed ones: `commit()` and `rollback()` mirror chunk commit and abort. When a buffer is full it hands the committed prefix to background sort tasks, one run per (slice, field order), and moves the provisional tail into the free buffer.
- `LmdbDeferredValueLookups` packs a value lookup record into four longs whose top bit is zero: 7 key bytes, 7 key bytes, then the last 2 key bytes plus the key length, then the ID. Its `Source` decodes the merged records in key order.
Acceptance: `LmdbSortedRunsTest` and `LmdbRunCollectorTest` pass.

Milestone 5: the import.
- In `LmdbSailStore`, keep `initialRunImportAvailable`. It is set at construction from the config switch and store emptiness, and cleared by any store transaction start that does not claim the import.
- The claim happens in `LmdbSailSink.startTransaction`, right after the fresh session starts, under the conditions in the Decision Log. It creates an `LmdbInitialRunImport`, which holds the owner, the triple `LmdbRunCollector` (one field order per `TripleStore` index), the value `LmdbDeferredValueLookups`, a sort executor, the run directory `.lmdb-initial-import-runs/` and the marker file. The claim then calls `valueStore.deferLookupRecords(lookups)`.
- During the import, `flushNoneBatch` skips the triple size estimate. After `resolveBatchValues`, it adds the batch's quads to the triple collector instead of calling `submitOperation`, and sets `state.statementsAdded`.
- `ValueStore.persistPreparedValues` hands its data-keyed and hash-keyed records to the deferral instead of writing them.
- `commitFreshValueTransaction` and `rollbackFreshValueTransaction` commit or roll back the deferral. `commitPendingPublication` commits the triple collector after the triple commit, and `rollback()` rolls it back.
- `ValueStore.storeValues` and `findId(..., create=true)` throw if called while lookups are deferred. That path would duplicate values.
- `LmdbSailSink.completeInitialRunImport()` flushes and checkpoints the open chunk. It then pre-grows the maps for the projected size, adds `InitialRunImportFinish` to `state.work`, starts the transactions, writes the value half, submits the triple half and checkpoints. Finally it syncs both environments, deletes the runs and the marker, stops the deferral, marks the sketch estimator for rebuild, and wakes the waiters. The replay loop in `recoverNoneCapacity` re-runs the value half before submitting the operation again.
- Completion is triggered:
  - from `flushNonePending`, which covers commit, explicit flush and the importer's own reads;
  - from `removeNoneStatements`;
  - from `beginMutationDatasetAcquisition` and the importer's `beginDatasetAcquisition`;
  - from `flushNoneBatch`, when the fresh session would reach `FRESH_NONE_VALUE_LIMIT`;
  - from a new first step of `LmdbStoreConnection.rollbackInternal`, `rollbackInitialRunImport(this)`.
- The gates are as in the Decision Log.
- `TripleStore.appendInitialImportRuns(List<List<Path>> runsByIndex, int[][] fieldOrders, …)`:
  - checks that every index and the staging list are empty, then calls `prepareForMutation(estimate)`;
  - starts one merge thread per index, each encoding keys into 4 MiB blocks; the `spoc` blocks also carry their quads;
  - appends round-robin with `MDB_APPEND`;
  - for each new quad, counts its context and calls `recordFanOutAdded`;
  - writes the context counts, then sets `nativeMutation` and `tripleMutation`;
  - maps `MDB_MAP_FULL` to `growForTransactionRetry`, as `writeLogicalAlignedBatch` does.
- `ValueStore.writeDeferredLookups(LmdbDeferredValueLookups.Source)` writes the records with sorted cursor puts in the open value transaction.
Acceptance: the new tests in `LmdbInitialRunImportTest`:
- an initial NONE import commits and reopens with every statement and lookup present;
- a second connection's read and write wait until the import commits;
- a read by the importing connection mid-import sees its own statements;
- a rollback keeps the checkpointed prefix and discards the open chunk;
- a non-NONE first writer disables the import for the rest of the store's life;
- a forced `MAP_FULL` during the final merge replays correctly;
- duplicates are counted once.

Milestone 6: crash safety. `LmdbStore.initializeInternal` resets a directory that contains the marker. Acceptance: a test plants the marker beside a populated store and finds the store empty after `init()`. A test that kills an import halfway, by closing the store without committing, finds it empty or complete after reopening, never half-written.

Milestone 7: run `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs`. For GH-6070 tests that assert progressive visibility from an empty store, turn the switch off in their store config, and record each one in Surprises & Discoveries. Measure the theme load with the harness, then format.

## Concrete Steps

Harness: the scratch sources are `ThemeLoadHarness.java` (src2) and `IriShapes.java` (src3), under the session scratchpad. Compile against `core/sail/lmdb/target/classes` plus the test classpath from:

    mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=cp.txt

Then run:

    java -Xmx8g --enable-native-access=ALL-UNNAMED -cp "out:core/sail/lmdb/target/classes:$(cat cp.txt)" ThemeLoadHarness /tmp/store

The harness uses `ThemeQueryBenchmark.ConfigUtil.createConfig()` settings plus `iterationCacheSyncThreshold(0)` and one `begin(NONE)` transaction. The `scratch.isolation` system property selects another isolation level.

Tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py ValueStoreIdCacheTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

## Validation and Acceptance

Milestone 1 holds when the new probe-count test fails before the change and passes after, every existing `ValueStoreIdCacheTest` test stays green, and the HEAD harness NONE load drops by roughly the alternative-lookup share (61 s towards about 35 s). Milestone 2 holds when the new fresh-path test fails before and passes after, and the harness NONE load approaches the READ_COMMITTED time (about 18 s). Milestone 3 holds when the full `core/sail/lmdb` suite is green apart from the known workspace-only spill tests.

## Idempotence and Recovery

All changes are additive code paths that can be reverted alone. The harness deletes and recreates its store directory on each run.

## Artifacts and Notes

The bisect log is `bisect.log` in the session scratchpad. Profiles: `head-load.jfr` (HEAD NONE), `skip-load.jfr` (alternative lookup skipped) and `good-load.jfr` (`99d9adec6c`).

## Interfaces and Dependencies

No new dependencies. `ValueStore.probeIriAlternative(long, MDBVal, MDBVal)` stays the single native probe seam that tests count.
