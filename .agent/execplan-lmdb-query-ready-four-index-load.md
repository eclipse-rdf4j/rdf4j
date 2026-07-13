# Make LMDB bulk loads query-ready in under 400 milliseconds

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must remain current as work proceeds. This document follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

Loading the `datagovbe-valid.ttl.gz` benchmark file must finish with the normal LMDB value dictionary and all four configured triple indexes (`spoc`, `ospc`, `psoc`, and `posc`) ready before the first SPARQL query is evaluated. The current branch stores a fast packed representation and converts it to ordinary indexes only when query planning asks for statistics. That makes the benchmark's first query perform the postponed load and raises total first-result latency to roughly 1.8 seconds.

After this work, both `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` and `loadDatagovFileInBatches` will return their first real SPARQL row in less than 400 milliseconds for `NONE` and `READ_COMMITTED` when `automaticEvaluationStrategy=false`. Parsing remains trial setup, cleanup remains invocation teardown, the triple indexes remain in one LMDB environment, literal inlining remains enabled with bit-identical IDs, and the existing query engine and `LmdbRecordIterator` continue to read the ordinary indexes. No `MDB_APPEND` or `MDB_APPENDDUP` flag is permitted.

## Progress

- [x] (2026-07-11 17:39Z) Read the repository, performance, test, comparison, Docker JFR, and ExecPlan instructions and confirmed the existing goal and clean tracked worktree.
- [x] (2026-07-11 17:39Z) Reused the successful required root offline clean install captured earlier in this task at the current commit.
- [x] (2026-07-11 17:45Z) Added and ran the four-case query-readiness regression; all four cases fail because commits publish packed staging rather than ordinary storage. Preserved the Surefire red in `initial-evidence.txt`.
- [x] (2026-07-11 19:45Z) Removed packed databases, codecs, iterators, and every query/mutation materialization trigger without migration code.
- [x] (2026-07-11 20:05Z) Replaced the collector with transaction-owned `PreparedStatementBatch`, including scalar coalescing, semantic deduplication, and triple dependencies.
- [x] (2026-07-11 21:48Z) Added the fresh value session and prepared four-index path, then covered isolation fallback, collisions, inline IDs, rollback, reopen, map growth, and worker cleanup.
- [x] (2026-07-12 16:29Z) Kept predicate IDs in a frequency-ordered 64-ID URI window after an exact varint sweep showed that reserving 1,024 keys enlarges every benchmark batch; covered cross-commit allocation and overflow.
- [x] (2026-07-12 17:29Z) Removed empty LMDB transaction starts from namespace mutations so READ_COMMITTED bulk replay retains concurrent value/index persistence; captured the focused red/green threading contract.
- [x] (2026-07-12 19:26Z) Passed the complete LMDB module: 1,484 tests, zero failures/errors, three skips, including all Surefire and Failsafe theme regressions.
- [ ] Resolve the batched-load performance floor without violating the fixed write constraints.

## Surprises & Discoveries

- Observation: The first-query regression is almost entirely deferred write work, not SPARQL iteration.
  Evidence: the JDK 25 diagnostic score is about 1,810 ms/op; 60.22% of CPU samples include `TripleStore.materializePackedIfNeeded`, 41.07% include native `mdb_put`, and only 0.53% include `LmdbRecordIterator`.

- Observation: A control configuration that writes ordinary indexes directly is much faster than materializing packed data but still above target.
  Evidence: a four-index diagnostic with automatic evaluation measured about 990 ms/op while including sketch-estimator work. Historical sketch-disabled ordinary ingestion measured roughly 684-720 ms/op.

- Observation: The earlier synthetic 1.36-second four-index native probe is not an impossibility bound for this workload.
  Evidence: competition judges reported valid single-environment, four-index submissions at 580 ms and 490 ms.

- Observation: Literal inlining is part of the required storage contract.
  Evidence: the user explicitly requires inlining and permits only backward-compatible optimizations. This plan therefore keeps the existing inline ID bits and computes each distinct literal once instead of disabling inlining.

- Observation: The packed persistence defect is identical for single and batched loading under both required isolation levels.
  Evidence: `LmdbQueryReadyBulkLoadTest` ran four parameter rows and reported four failures at the pre-query `hasPackedValueDictionary()` assertion; the compact report is appended to `initial-evidence.txt` and the full log is `logs/mvnf/20260711-174511-verify.log`.

- Observation: Prepared map-growth fallback originally leaked all three secondary direct-memory arenas.
  Evidence: the focused regression observed an arena count of three after commit; unconditional task draining reduced it to zero in `logs/mvnf/20260711-214453-verify.log`.

- Observation: Fresh-session qualification must happen before the asynchronous writer starts.
  Evidence: the old ordering intermittently crashed in native `mdb_txn_begin` while `TripleStore.isEmpty()` opened a read transaction concurrently with writer startup. Moving the emptiness check ahead of writer launch made the focused tiny-map case deterministic.

- Observation: Reserving the first 1,024 URI ID values for predicates is counterproductive for this workload.
  Evidence: the file has 55 distinct predicates. Exact key simulation for reservation sizes 64, 128, 512, and 1,024 found 64 smallest in every batch; 1,024 pushes ordinary URI IDs across an additional varint-width boundary. Frequency-ordering the predicates within a 64-ID window keeps all benchmark predicates compact and lets overflow continue collision-free in the ordinary URI sequence.

- Observation: Namespace replay was disabling concurrent prepared writes for READ_COMMITTED.
  Evidence: `LmdbSailStore.LmdbSailSink.setNamespace` started the shared LMDB transaction with `preferThreading=false` before the prepared statement batch arrived. The focused test observed `storePreparedTriples` on the caller thread before the fix and on `LmdbTripleStore-Ingest-*` afterward. A second exact single-load run then measured 258.901–286.606 ms, mean 269.202 ms.

- Observation: The remaining batched target conflicts with the required ordinary per-key LMDB write API on this machine.
  Evidence: phase instrumentation measured about 396.5 ms in the seven four-index cursor-put phases alone, excluding value persistence, Sail scalar ingestion, commits, and the first query. The cleaned exact JDK 25 results remain 546.939–605.483 ms for NONE (mean 576.265 ms) and 587.488–646.190 ms for READ_COMMITTED (mean 612.747 ms). Both keep every index query-ready and perform no query-side writes.

## Decision Log

- Decision: Delete persisted packed segments without migration while retaining their collision-safe in-memory value/quad collector under a new name.
  Rationale: Packed storage has not shipped to users, but the collector already gives `READ_COMMITTED` a useful single-pass primitive representation.
  Date/Author: 2026-07-11 / Codex.

- Decision: Keep all four triple indexes in the existing single triple-store LMDB environment and use exactly one LMDB writer.
  Rationale: The query engine must continue to read the current ordinary index databases. CPU-only sorting and encoding may run in parallel, but LMDB transaction and cursor ownership remains on one writer thread.
  Date/Author: 2026-07-11 / Codex.

- Decision: Use a narrow fresh-import session for the benchmark path and preserve the current generic path as a fallback.
  Rationale: A fresh store can assign and remember value IDs across consecutive file batches without repeatedly probing LMDB. Any removal, inferred write, incompatible context override, rollback state, or non-session mutation falls back to the general implementation.
  Date/Author: 2026-07-11 / Codex.

- Decision: Sort the main `spoc` order completely once, then derive the configured `ospc`, `posc`, and `psoc` orders with stable leading-field passes.
  Rationale: The earlier rejected experiment fully sorted every secondary index independently and paid more sorting cost than it saved. Sorting the main order once makes the existing stable-transition algorithm produce complete secondary orders with much less work and allows preparation to overlap the main-index native write.
  Date/Author: 2026-07-11 / Codex.

- Decision: Reserve 64 low URI ID values for fresh-import predicates, not 1,024, and assign the most frequent predicates first.
  Rationale: All 55 benchmark predicates fit, overflow remains compatible, and the bounded window avoids widening ordinary URI IDs. The 1,024-key proposal was retained as a measured alternative and rejected by exact encoded-key size rather than intuition.
  Date/Author: 2026-07-12 / Codex.

- Decision: Namespace changes do not open empty triple/value LMDB transactions.
  Rationale: `NamespaceStore` is protected by the existing sink lock and is persisted by `LmdbSailSink.flush()` regardless of whether a triple/value transaction is active. Avoiding the empty transaction lets a following prepared statement batch start the shared transaction with asynchronous writing enabled.
  Date/Author: 2026-07-12 / Codex.

## Outcomes & Retrospective

The ordinary query-ready pipeline and focused correctness contracts are implemented. Local JDK 25 single-transaction NONE and READ_COMMITTED runs can satisfy the 400 ms gate (the latest stable READ_COMMITTED run was 258.901–286.606 ms), but both batched methods remain above it. The measured cursor-put-only floor is already approximately the full 400 ms budget, so completion remains blocked unless the ordinary per-entry write constraint can be made materially faster without `MDB_APPEND`, multiple environments, or a query-format change. A faster load-only result is not completion if the first query writes or any configured index is missing.

## Context and Orientation

The relevant module is `core/sail/lmdb`. `LmdbSailStore` bridges Sail transactions to `ValueStore` and `TripleStore`. `ValueStore` assigns persistent or inline long IDs to RDF values in its own LMDB environment. `TripleStore` stores each quad as the key of every configured `TripleIndex` database in a separate, single LMDB environment. `LmdbRecordIterator` is the ordinary read path and must not change.

`PackedStatementList` currently holds distinct RDF values in a collision-safe object-to-int map and stores statements as four local integer IDs. `TripleStore` also owns packed databases, packed readers, and `materializePackedIfNeeded`, which decodes packed records and populates the ordinary databases. `LmdbSailStore.getEvaluationStatistics` and `LmdbStore.getEvaluationStrategyFactory` call that method during the first query. Those persisted packed facilities are removed; the in-memory representation becomes `PreparedStatementBatch` and feeds normal value/index writes before commit returns.

`DatagovLoadIsolationBenchmark` parses 613,157 statements once in `@Setup(Level.Trial)`. The single-transaction method calls `connection.add(model)` once. The batched method adds individual statements and commits every 100,000 statements. Both create a fresh repository, use four indexes through `ConfigUtil.createConfig()`, commit, prepare the two-pattern SPARQL query, and request its first row. The benchmark currently falls back to `hasStatement` when no row is produced; that fallback must be replaced by a failure so the timing cannot pass without a query result.

## Plan of Work

First add a parameterized integration regression in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb`. Its fixture must exceed the packed threshold, include two statements that guarantee a result for `SELECT * WHERE { ?s a ?type. ?type ?b ?c. }`, and run the Cartesian product of single versus batched loading and `NONE` versus `READ_COMMITTED`. Package-private diagnostics in `TripleStore` and `ValueStore` may expose index entry counts and the LMDB environment's last committed transaction ID. Immediately after every commit, the test must assert that all four explicit index databases contain every distinct fixture statement. It must then capture the value/triple transaction IDs, prepare and consume the first query row, assert both IDs are unchanged, repeat the query, reopen the store, and verify the same row again. The current code must fail before production edits because only packed databases are populated before query planning.

Next remove the persisted packed representation. Delete `PackedSegmentCodec`, `PackedValueCodec`, `PackedRecordIterator`, `PackedSegmentIteration`, and `PackedStatementIteration`; remove packed database handles, counters, readers, writers, and materialization code from `TripleStore`; remove packed checks from `LmdbSailStore` and `LmdbStore`; and replace packed-specific tests with ordinary-index readiness, duplicate, rollback, and reopen assertions. Adjust the LMDB maximum-database count to exclude packed databases. Do not edit evaluation-package classes or `LmdbRecordIterator`.

Rename `PackedStatementList` to `PreparedStatementBatch` and keep it strictly transaction-owned and in memory. It retains the `Value[]` dictionary, primitive `int[]` quads, identity front cache, semantic collision fallback, triple-term dependency ordering, and list view needed by `Changeset`. `LmdbSailSource.bufferStatementsForBranch` always uses this representation for large explicit bulk additions when no sketch estimator needs per-statement callbacks. When the sink receives an ordinary collection, including the scalar batched benchmark's transaction model, it creates the prepared batch exactly once. A preparation budget of the smaller of 512 MiB and one quarter of maximum heap bounds arrays and direct-key arenas. The 613,157-statement benchmark must fit in one prepared operation with a 2 GiB heap; larger imports split into internal operations within the same transaction.

Add a package-private fresh-import session owned by `LmdbSailStore`. It starts only when explicit and inferred triple stores and the user value dictionary are empty, the transaction contains explicit additions only, no contexts are overridden, and isolation is `NONE` or `READ_COMMITTED`. The session remembers semantic RDF value to global ID mappings across successful consecutive batch commits. It uses the existing inline literal codecs exactly once per distinct literal and uses the existing normal ValueStore bytes for non-inline values, namespaces, and triple terms. It maintains a transaction checkpoint so rollback removes provisional mappings while retaining mappings from earlier successful batch commits. An incompatible mutation invalidates the session and uses the normal code path.

Refactor `ValueStore.storeValues` internally into preparation and persistence phases without changing its public or package-external behavior. Preparation resolves existing IDs, allocates IDs for absent values, handles short and hash-keyed values and CRC collisions, records namespace and triple-term dependencies, and produces normal pending LMDB records. Persistence writes those records, reference counts, and pending hashes in the active ValueStore transaction. Transaction-visible caches publish only after successful persistence and clear on rollback. Once all IDs are available, submit triple-index construction to the existing triple writer while the caller persists ValueStore records; the two stores already use separate LMDB environments and can progress concurrently. The flush waits for both and preserves existing rollback/commit ordering.

Add a prepared ordinary-index operation to `TripleStore`. Use primitive index arrays and the existing stable radix utilities to establish complete numeric key order: stable-sort the initial statement indices by context, object, predicate, and subject to create `spoc`; derive `ospc` then `posc` on one CPU task and `psoc` on another task. Pre-encode unchanged `TripleIndex.toKey` varints into bounded direct-memory arenas. Use at most three fixed CPU workers and run inline on a single-core runtime; workers never call LMDB. The single writer inserts the main keys using the retained cursor and `MDB_NOOVERWRITE`, records successful additions, and writes secondary keys with flags zero. It skips implicit-database deletion when `LmdbSailStore.mayHaveInferred` is false. If inferred data, estimator callbacks, map resizing, removals, or another unsupported condition is present, use the existing generic aligned path. No append flag is introduced.

Finally make the benchmark require a query row, run focused and full correctness verification, capture paired local results, profile any remaining hotspot, and run the exact Docker Java 26 matrix. Retain only optimizations whose correctness tests pass and whose repeated benchmark movement is consistent. Continue the profile-guided loop until every measurement satisfies the hard threshold; do not declare success from a mean below 400 ms when an individual iteration exceeds it.

## Concrete Steps

Run commands from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`. Tests use `mvnf`, never `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbQueryReadyBulkLoadTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py TripleStoreTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py ValueStoreTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSailStoreTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Persist the initial red report by appending a clearly labeled section to the existing `initial-evidence.txt`; never overwrite its earlier evidence. Before formatting, run `(cd scripts && ./checkCopyrightPresent.sh)`. Formatting may use the repository's approved non-test quiet command:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Build the benchmark jar and run the two exact methods with `automaticEvaluationStrategy=false`, both isolation values, five one-second warmups, five one-second measurements, one fork, a 2 GiB G1 heap, and JSON output. Repeat each matrix twice on local JDK 25. Use end-anchored selectors so the six-index method cannot match. Run the same selectors in Docker Linux Java 26 and keep JFR captures for hotspot attribution.

Use `jmh-benchmark-compare` for mean and allocation comparisons, but parse JMH JSON `primaryMetric.rawData` for acceptance because every raw measurement must be under 400 ms.

## Validation and Acceptance

The new regression must fail on the pre-change code and pass after implementation. After each commit in all four load/isolation combinations, `spoc`, `ospc`, `psoc`, and `posc` must each contain the complete committed statement set. Query preparation and first-row evaluation must leave both LMDB last-transaction IDs unchanged. Duplicate adds, explicit promotion from inferred data, rollback, non-fresh fallback, map growth, reopen, namespace handling, triple terms, and inline literal golden IDs must remain correct.

The benchmark passes only when both exact methods, both isolation modes, both independent repetitions, and both runtimes satisfy the threshold. Each repetition has five measured values and every one must be less than 400 ms. `automaticEvaluationStrategy=true` is correctness-only. Parsing, teardown, append flags, missing indexes, query-side materialization, and a fallback existence check cannot contribute to a passing result.

## Idempotence and Recovery

Focused tests and benchmarks create fresh temporary stores and may be repeated. Initial and candidate benchmark artifacts use separate directories. Never delete untracked plans or profiles. If Maven fails offline because an artifact is missing, retry the exact command once without `-o`, then return offline. If a performance experiment regresses, reverse only that experiment with a narrow patch; do not use destructive Git commands. Worker failures cancel pending CPU tasks, release reservations, and roll back both LMDB transactions before the exception escapes.

## Artifacts and Notes

The pre-change JDK 25 profile is `/tmp/lmdb-first-query-profile-none-inprocess/java-command-cpu-78317.jfr`; its durable facts are copied into `Surprises & Discoveries`. Existing historical evidence under `profiles/lmdb-load-10x` remains useful for rejected full-secondary sorting, transaction caches, and ordinary direct ingestion, but packed load-only scores are not acceptance evidence because they omit query-ready indexes.

## Interfaces and Dependencies

No public API, configuration field, external dependency, query-engine class, or ordinary on-disk format changes. New internal types are `PreparedStatementBatch`, a fresh-import session owned by `LmdbSailStore`, prepared value records owned by `ValueStore`, and prepared index-key arenas owned by a single `TripleStore` operation. Literal IDs and `TripleIndex` key bytes remain bit-identical. Packed-format stores are intentionally unsupported because that format has not shipped.

Revision note (2026-07-11 17:39Z): Created the self-contained query-ready loading plan, superseding packed persistence as an accepted endpoint while preserving its useful in-memory collector. Recorded the hard four-index, single-environment, no-append, inline-compatible, first-query acceptance contract.

Revision note (2026-07-11 17:45Z): Recorded the focused four-case pre-fix failure and its durable evidence before any production edit.
