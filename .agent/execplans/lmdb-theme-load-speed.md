# Restore fast LMDB loading for NONE imports and cheap alternative-IRI checks

This ExecPlan is a living document maintained according to `.agent/PLANS.md` at the repository root. Keep `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` current as work proceeds. Label: GH-6205 (the current branch `GH-6205-lmdb-sparqloscope-native-execution`).

## Purpose / Big Picture

The LMDB theme benchmark (`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`) rebuilds its store when the store is deleted. It does this by adding 14,561,110 generated statements through `RDFInserter`, in a single connection transaction started with `IsolationLevels.NONE`. On 2026-10-13 this load took about 10 seconds. At HEAD `a9e2b3ed86` it takes 61 to 66 seconds, against a target of under 15 seconds. After this work the same load should take well under 30 seconds, ideally close to the old 10. Loads into stores that already hold data should also do far fewer dictionary lookups per new IRI.

The user observes the change by deleting `core/sail/lmdb/target/lmdb-theme-query-benchmark` and running the benchmark, or by running the standalone harness described under Concrete Steps. The harness prints `LOAD TOTAL <seconds>`.

## Progress

- [x] (2026-10-25 14:30Z) Measured and bisected the regression (Routine C spike, no production changes). Results are in Surprises & Discoveries.
- [x] (2026-10-25 16:10Z) Milestone 1. Red: `ValueStoreIdCacheTest#bulkCreationProbesEachAbsentNamespacePrefixHashOncePerBatch` counted 12,400 alternative probes for 400 sequential IRIs. Green after `BatchIdStorer.absentRecordHashes`. `ValueStoreIdCacheTest` 45, `ValueStoreTest` 147 and `ValueStoreNamespaceCacheTest` 1 pass. The harness NONE load went from 61.1 s to 39.0 s.
- [x] (2026-10-25 16:50Z) Milestone 2, implemented as fresh value assignment inside GH-6070's NONE chunks rather than buffering (see Decision Log). Red: `LmdbInitialNoneFreshImportTest` (2 of 3 failed: the session was never active). Green: 3 of 3. Spoil guard red/green: `ValueStoreIdCacheTest#ordinaryInsertSpoilsAnActiveFreshValueSession` fails with the `nextId` hook removed.
- [x] (2026-10-25 17:20Z) Milestone 2 regression suites green: `LmdbNoneStreamingIngestionTest` 89, `LmdbInitialNoneImportAccelerationTest` 38, `LmdbPreparedBulkStoreTest` 38, `FreshValueReservationTest` 8, `LmdbBatchedIngestionTest` 17, `LmdbInitialNoneFreshImportTest` 4 (including a fresh-chunk MAP_FULL replay). Two `LmdbBatchedIngestionTest` methods inject failures through `ValueStore.storeValues` on an initial NONE import. They now seed the store first, so they keep exercising the lookup path they were written for.
- [x] (2026-10-25 17:25Z) Harness NONE load with both fixes: 30.0 s at `-Xmx8g` and 29.5 s at `-Xmx16g` (was 61–66 s). `values/data.mdb` is 608 MB.
- [ ] Full `core/sail/lmdb` suite.
- [ ] Milestone 3: re-measure the theme load, run the full `core/sail/lmdb` suite, format, commit and push.

## Surprises & Discoveries

- Observation: the regression first appeared at `60e509b4ef` (2026-10-14, "Merge develop into optimize-lmdb"). That merge brought in GH-6070 (#6073) `eec180ddc6`, "Stream NONE ingestion through bounded native batches".
  Evidence: `git bisect --first-parent` over the same load harness. `99d9adec6c` took 10.0 s (7.2 s of it in commit); `60e509b4ef` took 598 s; `3bae6620c2` (2026-10-19) took 67 s; HEAD takes 61–66 s.
- Observation: a NONE write from a connection sets `LmdbSailSink.nativeCheckpoints` (`LmdbSailStore.java`, the `LmdbSailSink` constructor). That flag routes `approve()` to `addStatement()` and makes `shouldBufferPreparedApprovals()` return false. The fresh value session (`ValueStore.startFreshValueSessionIfEmpty`) is likewise skipped when `nativeCheckpoints` is set. A NONE import into an empty store therefore never takes the fast path. That path assigns new value IDs without any dictionary lookup (`ValueStore.persistPreparedValues`) and writes all four indexes in one globally sorted pass at commit (`TripleStore.storeGloballyOrderedFreshTriples`). The same HEAD code started with `IsolationLevels.READ_COMMITTED` loads the data in 18.1 s, and its `triples/data.mdb` is 1.6 GB instead of 3.15 GB.
- Observation: half of the streaming NONE time is spent in `ValueStore.AlternativeIriLookup.find`. This lookup runs for every new IRI and checks whether the same IRI text is already stored under a different namespace/local-name split. `MAX_KEY_SIZE` is `ValueStoreRecordCodec.MAX_INLINE_KEY_BYTES` (16), so every IRI longer than 15 bytes probes one hashed namespace key (`HASH_KEY` plus the CRC32 of the prefix) for each length from its full length down to 16. The namespace-proof cache that could shorten this is disabled during writes: `beginValueLookupMutation()` sets `valueLookupGeneration = null`, so `AlternativeIriLookup.scope` is null. In the theme data, 3,221,528 distinct IRIs average 46 bytes, which is about 31 probes each. Skipping the lookup entirely (a scratch switch, not a fix) cut the load from 61.1 s to 31.3 s.
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

## Outcomes & Retrospective

The theme benchmark's NONE load drops from 61–66 s to about 30 s. The per-batch absent-hash cache accounts for 22 s of that, and fresh value assignment inside NONE chunks for another 9 s. The remaining limit is the triple-ingest thread, which is about 21 s busy: aligned index puts take 14 s and about 56 chunk commits take 6.5 s, while the main thread waits 5 s on it. Reaching the old 10 s would need fewer, larger NONE chunks during an initial import, or the globally sorted whole-import write. GH-6070's bounded streaming rules out the latter for NONE.

Lessons: the regression came in through a merge's conflict resolution (the `!nativeCheckpoints` gates), not through a single feature commit. A first-parent bisect over a standalone load harness found it in about 20 minutes, which a profile alone would not have.

Follow-ups noticed:
- The READ_COMMITTED fresh path still trusts a fresh session that persists across transactions without checking `isFreshValueSessionUsable`. An ordinary insert by another connection between two such transactions could make its map stale.
- `ThemeQueryBenchmarkSmokeIT` intermittently fails to validate a freshly reopened store ("intervening native LMDB writer"); this looks like post-ready maintenance racing the validation read.

## Context and Orientation

The LMDB store is `core/sail/lmdb`. `LmdbStore` owns an `LmdbSailStore` (`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`). That in turn owns a `ValueStore` (the dictionary: RDF terms mapped to numeric IDs, `ValueStore.java`) and a `TripleStore` (four LMDB indexes over ID quads, `TripleStore.java`). Writes reach the store through `LmdbSailStore.LmdbSailSink`.

There are two relevant write paths. The streaming path resolves each batch's values with `ValueStore.storeValues`, which runs a `BatchIdStorer`. That class looks every value up and stores new ones, calling `findAlternativeIriId` for new IRIs. The batch's triples are then queued to an asynchronous writer thread named `LmdbTripleStore-Ingest-0`. For NONE writes, native LMDB transactions are committed in chunks (`flushNoneBatch` and `checkpointNoneChunk`, bounded by `NoneWriteState.MAX_CHUNK_ROWS` = 262,144 and `MAX_CHUNK_BYTES` = 64 MiB).

The fresh-import path is used when the store is empty and the sink buffers approvals (`approvePreparedBuffered` into `pendingPreparedApprovals`). It starts a `ValueStore.FreshValueSession` and stores buffered batches through `storeFreshPreparedStatements`. Large batches go through `storeGlobalFreshPreparedStatements`, which assigns IDs from an in-memory map without lookups and writes globally sorted index blocks. Index entries go in with `MDB_NOOVERWRITE`, so a second batch into a now non-empty store is still correct.

An "initial NONE import" (`LmdbSailStore.beginInitialNoneImport` and `finishInitialNoneImport`) is already recognised when a NONE attempt starts on an empty store. It defers adjacency and value-overlay maintenance until the attempt ends.

The alternative-IRI lookup is `ValueStore.findAlternativeIriId` and its inner class `AlternativeIriLookup`. `findRecord(prefix, offset, length, hash)` probes `HASH_KEY + varint(hash)` with `probeIriAlternative` (package-private and counted in tests). If the key exists, it compares the record and walks the `HASHID_KEY` collision chain.

## Plan of Work

Milestone 1. First, in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreIdCacheTest.java`, add a test that stores a few hundred sequential IRIs in one namespace with one bulk `storeValues` call. It asserts that the number of `probeIriAlternative` calls is at most a small constant per IRI (four), where today it is about the IRI length minus 15. Then give `AlternativeIriLookup` an optional absent-hash set. `findRecord` consults it before the `HASH_KEY` probe and records CRC values that came back `MDB_NOTFOUND`. `BatchIdStorer` owns the set for one run, removes a CRC whenever it inserts a record under that `HASH_KEY`, and clears the set when a namespace record is created (a counter in `getNamespaceID`'s creation branch). The existing alternative-split and collision tests in the same class must stay green.

Milestone 2. First add a test in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbInitialNoneImportAccelerationTest.java` (or a new class) showing that a NONE connection import into an empty store goes through the fresh path. That is observable through `ValueStore` statistics, or through a package-visible counter of fresh prepared values. Then allow `shouldBufferPreparedApprovals()` and the fresh value session for `nativeCheckpoints` sinks during an initial NONE import, bounded by a heap-derived statement budget. When the budget is reached, flush the buffer through `storePreparedStatements`, end the fresh session and fall back to streaming. The GH-6070 streaming tests (`LmdbNoneStreamingIngestionTest`, `LmdbBatchedIngestionTest`) must stay green. Where they rely on streaming from an empty store, keep that behaviour above the budget, or seed the store so the import is not initial.

Milestone 3. Run the harness, then the full LMDB module.

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
