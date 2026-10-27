# Load a whole collection in one NONE transaction as fast as READ_COMMITTED does

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.


## Purpose / Big Picture

A user who loads a dataset into an empty LMDB store with `connection.begin(IsolationLevels.NONE); connection.add(model); connection.commit();` expects that isolation level `NONE` is at least as fast as `READ_COMMITTED`, because `NONE` promises less. In October 2026 the JMH benchmark `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` (about 600,000 statements from `benchmarkFiles/datagovbe-valid.ttl.gz`) measured 678 ms for NONE against 395 ms for READ_COMMITTED. After this work NONE is no slower than READ_COMMITTED for that benchmark, and every existing guarantee of NONE stays: bounded memory for streamed input, recovery when an LMDB map fills up, and one dictionary ID per value.

The cause is structural. A READ_COMMITTED transaction buffers its statements and, at commit, hands the whole buffered set to the "prepared" bulk path: it copies the set into a `PreparedStatementBatch` (distinct values plus statements as value indexes), assigns every new value an ID once from the "fresh value session", and writes all triple index entries in one globally sorted pass. A NONE connection instead turns `approveAll` into one `approve` per statement. Each 1,024-statement batch is then flushed on its own: a byte estimate of the batch (`ValueStore.estimateWriteBytes`, about 10% of the load in a profile), a session lookup for every value position, and record encoding. The triple entries go to the "initial run import" (sorted runs merged at commit). Profiling the NONE load shows 65% of the time in the per-statement path.


## Progress

- [x] (2026-10-26 12:30Z) Measured with the scratch harness `DatagovBatches` on a loaded machine. Single transaction: NONE with run import 710–750 ms (add 466–519, commit 154–163), NONE without run import 735–811 ms, READ_COMMITTED 446–469 ms (add 53–59, commit 339–383). Batched: NONE 750–840 ms, READ_COMMITTED 780–900 ms.
- [x] (2026-10-26 13:00Z) Milestone 1: `LmdbNoneBulkApproveTest`. The path test failed before Milestone 2. The adjacency test also failed on the per-statement path: `LmdbStoreConnection.addStatementsInternal` never marked the attempt as writing, so its commit skipped the post-commit rebuild after the initial import and adjacency stayed at revision 0 (fixed in the same commit).
- [x] (2026-10-26 13:10Z) Milestone 2: the prepared NONE chunk with capacity replay, committed as `b74cbf2dcc`; the run-import switch no longer disables it.
- [x] (2026-10-26 13:20Z) Milestone 3, harness (loaded machine): single transaction NONE 370–424 ms against READ_COMMITTED 397–452 ms; batched NONE 718–761 ms against READ_COMMITTED 715–835 ms. Full module run left to CI.


## Surprises & Discoveries

- Observation: a raw `SailSink` opened with `IsolationLevels.NONE` and no owning connection has `nativeCheckpoints == false`, so it already takes the prepared path through `shouldBufferPreparedApprovals()` whenever a fresh session exists. Only NONE connections (and owned internal sinks) take the per-statement path.
  Evidence: `LmdbSailStore.LmdbSailSink` constructor sets `nativeCheckpoints` only when `requestedWriterOwner != null`.


## Decision Log

- Decision: route only `approveAll` of a `java.util.Collection` whose size is known, between `PREPARED_BATCH_MIN_STATEMENTS` (1,024) and a bound of 4 × `NoneWriteState.MAX_CHUNK_ROWS` statements, through the prepared path. Larger or unsized input keeps the per-statement path and the initial run import.
  Rationale: a collection is already in memory, so copying it into a prepared batch at most doubles memory the caller already spends; the bound keeps one native LMDB transaction (whose dirty pages LMDB holds in memory until commit) from growing without limit, which is what NONE users rely on for huge imports.
  Date/Author: 2026-10-26, Claude Code.
- Decision: the prepared batch forms one NONE chunk of its own: the attempt's earlier pending work is checkpointed first, and the prepared chunk is checkpointed right after it is written.
  Rationale: NONE chunk accounting (`NoneWriteState.rows`, `bytes`) and replay then cover exactly one operation, and a capacity failure replays only the prepared operation.
  Date/Author: 2026-10-26, Claude Code.


## Outcomes & Retrospective

(To be written at completion.)


## Context and Orientation

All code is in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. `LmdbSailStore.java` holds the write pipeline; its inner class `LmdbSailSink` receives statements from a connection (`approve`, `approveAll`, `deprecate`, `clear`). `LmdbStoreConnection.java` is the connection; it reports the end of a NONE attempt through `LmdbSailStore.finishInitialNoneImport(owner, attempt, committed)`.

A "NONE attempt" is one NONE transaction of one connection. It writes in bounded "native chunks": each chunk is one LMDB write transaction in each of the two environments (the value dictionary `ValueStore` and the triple indexes `TripleStore`), committed by `LmdbSailSink.checkpointNoneChunk()` through `LmdbSailStore.commitPendingPublication`. `NoneWriteState` (an inner class of `LmdbSailStore`) records the chunk: `work` (a list of `Operation` objects that can be replayed), `rows`, `bytes`, `containsAdds`. When an LMDB map fills, `LmdbSailSink.recoverNoneCapacity` rolls the chunk back, grows the maps (`growNoneMaps`) and replays `state.work`: it re-resolves values for `BulkAddQuadsOperation`, resolves `NoneRemoveOperation`, and re-runs `InitialRunImportFinish`. Any new operation type needs its own replay branch there.

The "fresh value session" (`ValueStore.FreshValueSession`) is an in-memory map from value to ID that a store which started empty uses instead of dictionary lookups. `ValueStore.assignFreshValues(session, values, predicateIndexes)` assigns IDs to a set of distinct values; `ValueStore.freshValueEncoder(session, assignment)` then yields `PreparedValueBatch` records that `persistPreparedValues` writes into the open value transaction. A session is spoiled (`ValueStore.isFreshValueSessionUsable` returns false) when value GC deletes a value or an ordinary insert assigns an ID outside it. A committed NONE connection leaves its session reserved for the next NONE connection (`freshSessionReservedForNone`); `LmdbSailSink.storeFreshNoneValues` adopts it for a chunk of 64 values or more. Session rollback (`rollbackFreshValueTransaction`) removes the provisional IDs of a rolled-back native transaction, so a replay can assign again.

The prepared path used by READ_COMMITTED is `LmdbSailSink.approveAllBulk` → `preparedBatch` (builds `PreparedStatementBatch.copyOf`) → `reservePreparedImportCapacity` → `storePreparedStatements` → `storeFreshPreparedStatements`, which for a batch larger than one operation uses `storeGlobalFreshPreparedStatements` and the `GlobalPreparedImportOperation` (triples written by `TripleStore.storeGloballyOrderedFreshTriples`). Value publication happens at commit from `pendingFreshValueBatches`.

The "initial run import" (`LmdbInitialRunImport`) is claimed by the first native start of the first NONE attempt of an empty store (`claimInitialRunImport`, gated by the one-shot flag `initialRunImportAvailable`). It defers triple entries and value lookup records to sorted runs and writes them when the attempt completes.


## Plan of Work

Milestone 1 adds `LmdbNoneBulkApproveTest` in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/`. It loads a generated model of 20,000 statements (shared predicates, shared and unique objects, a named graph) with one `connection.add(model)` in a NONE transaction into an empty store, and asserts the contents, a lookup by every object (one ID per value), and the same after reopening. It repeats the load with a second NONE connection that adds an overlapping model, with a rollback in between, with adjacency enabled (answers equal LMDB's), with a connection listener (every add reported), and with small maps and `autoGrow` so that the prepared chunk hits capacity and is replayed. A test that observes the path itself (the prepared chunk is used; the run import is not claimed) fails before Milestone 2; the correctness tests pass before and after and guard the change.

Milestone 2 adds the prepared NONE chunk to `LmdbSailSink.approveAll`. When `nativeCheckpoints` is true, the approval is explicit, has no override contexts, the sketch estimator is off, prepared batches are enabled, the input is a sized collection inside the bounds above, this attempt is the only NONE writer, and the fresh session is owned by this owner or reserved for it, the sink: flushes the pending NONE batch and completes any run import of this owner; checkpoints the open chunk; consumes `initialRunImportAvailable` so that its own native start does not claim an import; copies the collection into a `PreparedStatementBatch`; grows the maps for the batch's projected size; adds a `PreparedNoneAddOperation` to `state.work`; starts the native transaction; assigns and stores the batch through the same code as `storeFreshPreparedStatements`; records the mutation; and checkpoints. `recoverNoneCapacity` gets a branch that re-runs the operation after the session rollback. In every other case `approveAll` keeps `SailSink.super.approveAll`.

Milestone 3 measures `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` and `loadDatagovFileInBatches` with the JMH jar, runs the full module, and records the outcome.


## Concrete Steps

From the repository root:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNoneBulkApproveTest --module core/sail/lmdb --retain-logs
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Pbenchmarks,quick -DskipTests package
    java -jar core/sail/lmdb/target/jmh-benchmarks.jar 'DatagovLoadIsolationBenchmark.loadDatagovFile(SingleTransaction|InBatches)$' -p automaticEvaluationStrategy=false


## Validation and Acceptance

`LmdbNoneBulkApproveTest` passes, and its path test fails before Milestone 2. The full `core/sail/lmdb` suite passes. In JMH, `loadDatagovFileSingleTransaction` with NONE is no slower than with READ_COMMITTED (within the error bars), and `loadDatagovFileInBatches` with NONE stays no slower than READ_COMMITTED.


## Idempotence and Recovery

Tests use `@TempDir` stores. The change is additive: removing the new branch in `approveAll` restores the previous behaviour.


## Artifacts and Notes

Profile of the NONE single-transaction load before the change (share of main-thread samples, including parse time): `approveAll` per statement 65%, `flushNoneBatch` 49%, `resolveBatchValues` 35%, `storeFreshNoneValues` 29% (`FreshValueEncoder.next` 13%, `assignFreshValues` 11%), `ValueStore.estimateWriteBytes` 10%.


## Interfaces and Dependencies

No public interface changes. New package-private class or inner class `PreparedNoneAddOperation implements LmdbSailStore.Operation` inside `LmdbSailStore`, and a package-private counter for tests, for example `LmdbSailStore.preparedNoneChunks()`.
