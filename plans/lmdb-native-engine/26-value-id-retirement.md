# Durable two-stage value-ID retirement for LMDB pinned snapshots

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` at the repository root.

## Purpose / Big Picture

The LMDB sail (`core/sail/lmdb`) stores RDF statements in two separate LMDB environments: the TripleStore (numeric quad indexes) and the ValueStore (a dictionary mapping RDF values to numeric IDs in both directions). On the `optimize-lmdb` branch, a reader that opens a SNAPSHOT-isolation transaction gets a "pinned" LMDB read transaction over the TripleStore: it keeps seeing the exact B+tree snapshot that existed when it started, even while writers commit. A "pinned read transaction" here means an LMDB read transaction that is deliberately never reset when writers commit, created by `TxnManager.createReadTxnPinned(...)` and stamped with the store revision it observed.

The problem this plan fixes: when a writer removes statements and commits, the value dictionary garbage collector (`ValueStore.gcIds()`) eagerly deletes the value→ID mapping for IDs that no longer appear in the current triple indexes. A pinned reader whose snapshot still contains those triples then silently gets **empty query results**, because looking up the query's constant values (for example the subject IRI) in the dictionary no longer returns an ID. This is a wrong-answer bug, not an error. The test `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSnapshotValueGcTest.java` demonstrates it: with GC deferral disabled the pinned reader sees `[]` instead of the removed statement.

After this change, value-ID reclamation is scheduled through a small durable "retirement" queue inside the ValueStore LMDB environment. An ID whose statements were removed at store revision R is only handed to the existing GC once no pinned reader with a snapshot older than R exists. The observable outcomes:

1. Pinned readers keep resolving values removed after their snapshot (existing `LmdbSnapshotValueGcTest` stays green without the temporary workaround).
2. A pinned reader no longer blocks reclamation of *unrelated* IDs retired before its snapshot (new test, impossible with the temporary workaround).
3. Retirement intents survive a process restart and are reclaimed on reopen (new recovery test).

## Background you must know (self-contained orientation)

All paths are relative to the repository root. Line numbers refer to the working tree at the time of writing; verify with `rg` before editing.

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` — the dictionary. Key pieces:
  - `gcIds(Collection<Long> ids, Collection<Long> nextIds)` (~line 2794): for each ID, unless ref-counted, writes a tombstone into the `unusedDbi` LMDB database (key = `Varint(valueStoreRevisionId)|Varint(id)`), then `deleteValueToIdMappings(...)` (~2847) deletes the value→ID and hash→ID entries (the destructive half) while keeping ID→value, and cascades reference-count decrements of component IDs (datatypes, namespaces, RDF-star triple terms) into `nextIds`.
  - `freeUnusedIdsAndValues(...)` (~2997): later moves `unusedDbi` tombstones to `freeDbi` (the free list) and deletes ID→value — but only for ValueStore revisions whose `ValueStoreRevision.Lazy` object has become unreachable (a `Cleaner` registers the revision id into `unusedRevisionIds` at ~3138). This is the "lazy-value horizon": query results hand out `LmdbValue` objects that resolve lazily even after the dataset closed, so ID→value must outlive the dataset. **This whole mechanism stays unchanged.**
  - The environment's named-database budget is set at ~line 717: `E(mdb_env_set_maxdbs(env, 6 + 12));`. Auxiliary databases are opened in `openAuxiliaryDatabases()` (~770): `unused_ids`, `free_ids`, `ref_counts`.
  - Write access pattern: `gcIds` runs inside `readTransaction(env, ...)` → `resizeMap(...)` → `writeTransaction((stack, writeTxn) -> ...)`; `writeTransaction` reuses the ValueStore's active write transaction when one is open (it always is during a connection flush, because `LmdbSailStore` called `valueStore.startTransaction(true)` at transaction start).
  - `enableGC()` (~4174) returns `valueEvictionInterval > 0` (default 60000 ms) and gates all of `gcIds`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` — the quad indexes.
  - `dataRevision` (`AtomicLong`, ~line 204) counts committed write transactions. It is **process-local**: it starts at 0 on every open and is never persisted. It is bumped at ~4268 inside `commit()`, under the `TxnManager` write lock, after `mdb_txn_commit`.
  - `filterUsedIds(Collection<Long> ids)` (~2840) is the only liveness primitive: it **destructively removes from the given collection** every ID that still occurs in any committed index (it opens its own read-only transaction, so it sees committed state only). There is no single-ID `isIdUsed`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnManager.java` — transaction bookkeeping per environment.
  - `createReadTxnPinned(LongSupplier dataRevision)` (~233) creates an untracked read txn and stamps `txn.snapshotRevision` with the current data revision, under the manager's read lock, which excludes the commit critical section — so the (B+tree snapshot, revision) pair is exact.
  - `minPinnedSnapshotRevision()` (~257) returns the minimum `snapshotRevision` over all active transactions that have one (`>= 0`), or `Long.MAX_VALUE` when none do. This is the reclamation watermark. Parallel sibling transactions inherit the pin's revision (`LmdbSailStore` ~4144), so they participate.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` — the coordinator that owns both stores.
  - Removed-statement value IDs are collected into `unusedIds` (a `PersistentSet<Long>`) during `removeStatements`.
  - Commit ordering in `flush()` (~1848): `tripleStore.commit()` → `filterUsedIdsInTripleStore()` (prunes `unusedIds` to truly-unused IDs) → `handleRemovedIdsInValueStore()` (~1813, the GC trigger) → `valueStore.commit()` → CSR cache delta merge. The multi-threaded ingest path runs the first two on an executor thread; `flush()` awaits it before the rest, so `handleRemovedIdsInValueStore()` always runs on the flush thread after the TripleStore commit.
  - The temporary workaround now being replaced lives here: field `deferredUnusedIds` (~184), created at ~829 via `setFactory.createSet("deferredUnusedIds", ...)`, and logic at ~1814–1831 that parks **all** of `unusedIds` whenever `minPinnedSnapshotRevision() < getDataRevision()` and only drains when no pinned reader is behind. Kill switch `deferValueGcForPinnedReaders()` (~124), system property `rdf4j.lmdb.valueGc.deferForPinnedReaders`, default true.
  - `GuardedEstimatorStatementSource` (~892–957): the sketch-estimator refresh reader. It acquires `sinkStoreAccessLock` before opening its dataset and holds it until its iteration closes, and refuses to start while a write transaction is active. Because `flush()` (all GC activity) runs under the same lock, refresh iterations can never interleave with reclamation; they always read the latest committed snapshot. This is why these untracked readers are exempt from the watermark.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PersistentSetFactory.java` — scratch LMDB env in a temp dir (wiped per boot) backing `unusedIds` etc. The workaround bumped `mdb_env_set_maxdbs(env, 2)` to `3` at ~line 70; this plan reverts that.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/Varint.java` — order-preserving variable-length integer codec (sqlite4 style): lexicographic byte order equals numeric order, and the first byte determines the length, so concatenated varints form composite keys that compare component-wise under LMDB's default unsigned-byte-lexicographic key ordering. The existing `unusedDbi` keys rely on exactly this.

## The design

Core invariant: **while any active TripleStore snapshot can contain ID `i`, both `value → i` and `i → value` must remain valid, and `i` must not be reused.** Retirement bookkeeping only *schedules* reclamation; every drain re-checks current TripleStore liveness (`filterUsedIds`) before handing IDs to the unchanged `gcIds()` path. Correctness therefore never depends on the bookkeeping alone — losing or delaying records can only delay reclamation (a bounded dictionary-space leak), never reclaim early.

Three new named LMDB databases in the **ValueStore** environment (bump `maxdbs` at ValueStore.java:717 from `6 + 12` to `9 + 12`), opened in `openAuxiliaryDatabases()`:

- `retired_ids` — by-ID, latest-wins: key `Varint(id)` → value `Varint(bootEpoch)|Varint(retirementRevision)`. A plain `mdb_put` overwrite supersedes an older record when the same ID is removed, re-added, and removed again.
- `retired_ids_seq` — ordered drain index: key `Varint(bootEpoch)|Varint(retirementRevision)|Varint(id)` → empty value. Entries are never proactively deleted on supersede; the drain detects staleness by comparing against `retired_ids`.
- `gc_meta` — a single record: key byte `0x01` → `Varint(lastBootEpoch)`. Read, incremented, and written back once at open, followed by one `mdb_env_sync` (the env runs `MDB_NOSYNC`, and the epoch must not go backwards; one sync per boot is cheap).

Why a boot epoch: `dataRevision` restarts at 0 every boot, so revisions from different process lifetimes are incomparable. After a restart no pre-restart reader can exist, so every record with `epoch < currentBootEpoch` is automatically past the reader horizon — but must still pass the liveness re-check, because the removal that motivated it might never have committed (crash between the two environments' commits) or the value may have been re-added.

Because `(epoch, revision, id)` sorts numerically under the varint encoding, all drainable entries form a contiguous prefix of `retired_ids_seq`: cursor from `MDB_FIRST`, stop at the first key with `epoch == bootEpoch && revision > watermark`.

The drainability rule: an ID retired at revision R is reclaimable iff `R <= minPinnedSnapshotRevision()`. A reader pinned concurrently with a drain stamps its revision under the TxnManager read lock, which excludes the commit critical section, so its revision is ≥ every drained R — it never saw the removed triples; the race is benign.

The supersede race this design must handle (and Milestone 3 tests): remove `v` at R10 (record `(epoch, R10, id)`), re-add `v` at R11 (same ID, because value→ID was retained), pin a reader at R12, remove `v` again at R13 (by-ID record now says R13). When the watermark passes R10 but not R13, the drain encounters the `R10` seq entry, sees the by-ID record says R13, and treats the seq entry as stale (deletes it, does not reclaim). Without the by-ID currency check the reader at R12 would lose `v`.

### New class `RetiredValueIdStore`

New file `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RetiredValueIdStore.java`, package-private, owned by `ValueStore`. Shape (signatures, not prescriptive code):

    final class RetiredValueIdStore {
        void open(long env, long writeTxn);   // open 3 DBIs, bump+persist bootEpoch, init pendingCount
        long bootEpoch();
        boolean isEmpty();                    // O(1) via maintained count (init from mdb_stat)
        void recordRetirements(MemoryStack stack, long writeTxn, Collection<Long> ids, long retirementRevision);
        DrainBatch pollDrainable(long readTxn, long maxRevisionInclusive, int maxIds);
        void removeDrained(MemoryStack stack, long writeTxn, DrainBatch batch);
        static final class DrainBatch {
            List<Long> ids;                   // candidates whose by-id record matched the seq entry
            List<byte[]> matchedSeqKeys;      // their exact seq keys
            List<byte[]> staleSeqKeys;        // superseded entries: delete seq key only
            boolean moreRemaining;
        }
    }

`ValueStore` gets thin wrappers so map-resize sizing stays inside `ValueStore`, mirroring `gcIds`: `recordRetiredIds(Collection<Long>, long tripleDataRevision)`, `pollRetiredIds(long maxRevisionInclusive, int maxIds)`, `removeRetiredIds(DrainBatch)`, `hasRetiredIds()`. `recordRetiredIds` is gated on the same `enableGC()` check as `gcIds` — with GC disabled nothing is ever reclaimed, so the retirement DBIs must not grow. All writes go through `writeTransaction`, so they join the ValueStore's active write transaction and become durable atomically with the dictionary writes at `valueStore.commit()`.

### Commit-path rewrite in `LmdbSailStore`

Replace the body of `handleRemovedIdsInValueStore()` (called at flush, after `tripleStore.commit()` and `filterUsedIdsInTripleStore()`, before `valueStore.commit()`):

1. `revision = tripleStore.getDataRevision()` (exact post-commit value); `watermark = tripleStore.getTxnManager().minPinnedSnapshotRevision()`; `pinnedReadersBehind = deferValueGcForPinnedReaders() && watermark < revision`.
2. If `unusedIds` is non-empty: when `pinnedReadersBehind`, record them all as retirement intents at `revision` and clear; otherwise run the existing gcIds/filter cascade loop. Cascade IDs surfacing in `nextUnusedIds` (datatype/namespace/triple-term references whose refcount hit zero) get the same two-stage treatment: filter, then gcIds or record-as-intent depending on `pinnedReadersBehind`.
3. Drain: if `hasRetiredIds()`, poll a bounded batch up to the watermark, delete stale seq keys, run `filterUsedIdsInTripleStore()`-style liveness re-check over the candidates (`tripleStore.filterUsedIds`), remove the drained records (including records whose ID turned out live again — a future removal re-records them), and hand survivors to the gcIds cascade loop.

Batch bound: new system property `rdf4j.lmdb.valueGc.drainBatchSize`, default 100000. `moreRemaining` simply leaves the tail for the next commit or the next open.

Crash-safety note (documented in the method's javadoc): the TripleStore commits before the ValueStore in `flush()`. A crash between the two loses that commit's intents while the triple deletions survive — a **dictionary-space leak only**, never wrong data (value→ID mappings persist harmlessly). Both environments run `MDB_NOSYNC` anyway, so neither commit is a hard durability point. Reordering the commits (write-ahead intents) is a deliberately deferred follow-up (see Decision Log).

### Recovery on open

In the `LmdbSailStore` constructor, after `tripleStore` is constructed (so `filterUsedIds` works) and before background machinery starts: if `hasRetiredIds()`, loop bounded polls with `maxRevisionInclusive = Long.MAX_VALUE` (no readers can exist yet) → liveness re-check → remove → gcIds cascade, wrapped in `valueStore.startTransaction(true)` / `commit()`. The wrap matters: `gcIds` sets `invalidateRevisionOnCommit`, and the lazy-revision bookkeeping only runs in `endTransactionInternal`. The epoch bump already happened in `open()`, so an interrupted recovery leaves old-epoch entries that remain always-drainable later. The existing `unusedDbi` startup sweep is untouched and independent.

### Workaround revert and kill switch

Remove from `LmdbSailStore`: the `deferredUnusedIds` field, its creation, and the park/unpark block. Revert `PersistentSetFactory` `maxdbs` to 2. **Keep** `deferValueGcForPinnedReaders()` and its property name (it names the guarantee, not the mechanism). Disabled ⇒ `pinnedReadersBehind` is always false ⇒ old eager gcIds behavior and no new intents; the drain of pre-existing durable records still runs so nothing is stranded. The property is read per call, which keeps tests able to flip it with `System.setProperty`.

### What is explicitly out of scope

- Any change to `gcIds`, the `unusedDbi`→`freeDbi` drain, refcount cascades, or the `ValueStoreRevision`/Cleaner lazy-value horizon. The two horizons stay separate: the snapshot horizon protects value→ID + ID→value + non-reuse; the lazy-revision horizon protects ID→value + non-reuse afterwards.
- flush() commit reordering (ValueStore-first).
- Stamping estimator-refresh readers into the watermark (exempt via `sinkStoreAccessLock`; add explanatory comments at `GuardedEstimatorStatementSource` and at the drain site).
- The other uncommitted branch work (snapshot invalidation via `ensureSnapshotValid`, `isSnapshotCurrent`/`SailSourceBranch` retire-on-flush, CSR generation chains) — shared watermark, no interface change.

## Milestones

Commands assume the repository root as working directory. Build once per session: the root quick install (see CLAUDE.md "Always Install Before Tests"). Tests run via `python3 .codex/skills/mvnf/scripts/mvnf.py Class#method`; never pass `-am` or `-q` to test runs. Note `-DargLine` does not propagate to the forked Surefire JVM in this build — tests set system properties with `System.setProperty` in setup/teardown (both switches are read per call), or use `-DforkCount=0` for ad-hoc experiments.

### Milestone 1 — the first failing test (per-ID drain progress)

New test `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbValueGcRetirementTest.java` (same package as the internals; reach the `ValueStore` via the package-private accessors on `LmdbStore`/`LmdbSailStore`; the oracle for "dictionary entry reclaimed" is `valueStore.getId(value) == LmdbValue.UNKNOWN_ID`).

Test `pinnedReaderDoesNotBlockOlderRetirements`: commit base data (revision R1); open pinned reader A at R1; commit a removal of value `v` (R2 — intent recorded because A is behind); close A; open pinned reader B at R2; commit a removal of value `w` (R3 — this same commit's drain sees watermark R2 ≥ retirement R2 and reclaims `v`'s IDs, while `w` is parked because B is behind). Assert `v`'s value→ID is gone while B still resolves `w` in a query.

Expected to FAIL against the current workaround (all-or-nothing: B behind R3 parks everything, so `v` is still resolvable). Capture the failing Surefire snippet as evidence before touching production code. Then `git rm`-nothing — the workaround revert happens in Milestone 2 as part of making this green.

Acceptance: the test exists, runs, and fails with an assertion showing `v` still resolvable (or equivalent), recorded in `Progress` and in an evidence file at the repository root.

### Milestone 2 — implement the retirement store and rewrite the commit path

Work items, in order, keeping the build compiling throughout: (a) `RetiredValueIdStore` + three DBIs + epoch bump + `maxdbs` 9+12; (b) `ValueStore` wrapper methods; (c) `handleRemovedIdsInValueStore` rewrite including the cascade policy and the bounded drain; (d) workaround revert (field, creation, block, `PersistentSetFactory` maxdbs back to 2). New Java file needs the exact copyright header (current year) plus the agent signature comment line, and `cd scripts && ./checkCopyrightPresent.sh` must pass.

Acceptance: Milestone 1's test passes; `LmdbSnapshotValueGcTest` (the original pinned-reader-resolves-removed-values test) still passes — now via the new mechanism instead of the workaround. Both Surefire snippets captured.

### Milestone 3 — supersede race

Add test `supersededRetirementDoesNotTearNewerReader` to `LmdbValueGcRetirementTest`: remove `v` with an older pinned reader open (intent at R_a); re-add `v` (same ID via retained value→ID — assert the ID equality, it is load-bearing); open pinned reader M; remove `v` again (by-ID record superseded to R_b); close the older reader; commit something unrelated so the drain runs with watermark ≥ R_a but < R_b. Assert M still resolves `v` and (via package-private inspection or a follow-up drain) that the stale R_a seq entry was purged without reclaiming.

If the Milestone 2 implementation already handles this, the test may pass immediately — that is acceptable (it is a regression guard); if it fails, fix the by-ID currency check.

### Milestone 4 — restart recovery

New test `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbValueGcRecoveryTest.java`: (case 1) retire IDs behind a pinned reader, close the reader, shut down **without** any further commit (no drain ran), reopen the store against the same data dir, assert the retired IDs were reclaimed during open and live values are untouched. (case 2) with `rdf4j.lmdb.valueGc.drainBatchSize=1`, leave several old-epoch entries and reopen twice, exercising always-drainable old-epoch records, `moreRemaining`, and double-drain idempotency of `gcIds` (re-tombstoning is harmless; LMDB `MDB_NOTFOUND` deletes are tolerated by `E()`).

Implement the constructor recovery loop to make case 1 green.

### Milestone 5 — kill switch semantics

Test: with `rdf4j.lmdb.valueGc.deferForPinnedReaders=false`, a removal behind a pinned reader gc's eagerly (the reader observes the historical wrong-answer behavior or at least: no intents are recorded, `hasRetiredIds()` stays false). This documents the escape hatch.

### Milestone 6 — regression and polish

Run `LmdbSnapshotValueGcTest`, `LmdbCsrSnapshotTest`, `LmdbSnapshotInvalidationTest`, `TxnManagerTest`, then the whole module `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb`. The module has a known baseline of pre-existing failures on this branch (13 as of 2026-07-21, unrelated to value GC) — judge against that baseline via the report XMLs, not the mvnf summary alone. Run the formatter (`mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`) and the copyright check. Update this ExecPlan's `Outcomes & Retrospective`.

## Progress

- [x] (2026-07-28) ExecPlan authored; user decisions recorded in Decision Log.
- [x] (2026-07-28 12:00Z) M1: failing test `pinnedReaderDoesNotBlockOlderRetirements` observed failing (`expected: -1L but was: 258L`); evidence in `initial-evidence-value-id-retirement-m1.txt`.
- [x] (2026-07-28 12:04Z) M2a: `RetiredValueIdStore` + DBIs + boot epoch + maxdbs 9+12.
- [x] (2026-07-28 12:04Z) M2b: `ValueStore` wrappers (`recordRetiredIds`/`pollRetiredIds`/`removeRetiredIds`/`hasRetiredIds`).
- [x] (2026-07-28 12:04Z) M2c: `handleRemovedIdsInValueStore` rewrite + `drainRetiredIds` + `runValueGcCascades`; recovery loop `recoverRetiredValueIds` also landed here (constructor call).
- [x] (2026-07-28 12:05Z) M2d: workaround reverted; M1 test green (tests=1, failures=0) and `LmdbSnapshotValueGcTest` green — evidence in `final-evidence-value-id-retirement-m2.txt`.
- [x] (2026-07-28 12:10Z) M3: supersede-race test green (tests=2, failures=0). Mid-race protection worked immediately; the final-reclamation phase needed a fresh SNAPSHOT borrower because the store branch caches the last pinned dataset (see Surprises).
- [x] (2026-07-28 12:12Z) M4: recovery tests green on first run (tests=2, failures=0) — `retirementsRecoveredAfterRestart` + `boundedRecoveryDrainsEverything` (drainBatchSize=1 forces many bounded rounds in one recovery).
- [x] (2026-07-28 12:13Z) M5: kill-switch test green (tests=3, failures=0 across `LmdbValueGcRetirementTest`).
- [x] (2026-07-28 12:35Z) M6: full-module regression: tests=2407, failures=5, errors=0 — all 5 failures byte-identical to the pre-existing baseline in `logs/mvnf/20260727-183247-verify.log` (kernel decline census gate, strategy priority, 3× feature-flag fork parity); zero new failures. Formatter + copyright check clean. Evidence in `final-evidence-value-id-retirement-m6.txt`.

## Surprises & Discoveries

- (2026-07-28, from pre-plan investigation) The pinned-reader failure mechanism is the destruction of **value→ID** in `deleteValueToIdMappings`, not ID→value: disabling the workaround makes `LmdbSnapshotValueGcTest` fail with an *empty result* (`[]`), not a resolution error. Any fix must gate that half of `gcIds`, which the retirement design does by deferring the whole `gcIds` call.
- (2026-07-28) `-DargLine` does not propagate to the forked Surefire JVM in this build (verified with a bogus `-XX:` flag); system-property-sensitive experiments need `-DforkCount=0` or in-test `System.setProperty`.
- (2026-07-28) `SailSourceBranch` (autoFlush, store-level) caches the last pinned SNAPSHOT dataset for reuse after the borrowing connection closes; the cached dataset's read transaction stays active and correctly participates in the watermark until the next SNAPSHOT borrower retires it via `isSnapshotCurrent()`. Tests that expect a final "no readers left" drain must borrow a fresh SNAPSHOT dataset first. This is design-consistent (a cached snapshot is a real active reader), not a leak.

## Decision Log

- (2026-07-28, user) **Defer flush() commit reordering.** The design's "retirement intents durable before the TripleStore deletion publishes" would require committing the ValueStore first, a pre-commit `filterUsedIds(writeTxn)` overload (the current one opens its own read txn and sees committed state only), and a new barrier op on the multi-threaded ingest executor. Since both envs run `MDB_NOSYNC`, neither commit is a hard durability point, and the current-order crash window is leak-only (never wrong data) — strictly better than the temp-dir set it replaces. V1 keeps the current order and documents the window; reordering is an independently shippable follow-up.
- (2026-07-28, user) **Routine D**: this ExecPlan governs the work; TDD evidence discipline (failing-then-passing Surefire snippets) applies inside each milestone.
- (2026-07-28) **Varint composite keys, not fixed-width**: the repo's `Varint` is order-preserving and prefix-free across values, matching the existing `unusedDbi` key pattern; keys stay 4–12 bytes and drainable entries form a contiguous DBI prefix.
- (2026-07-28) **Estimator-refresh readers stay out of the watermark**: they hold `sinkStoreAccessLock` across their whole iteration and always read the latest committed snapshot, so reclamation cannot interleave; stamping them would let a long refresh block GC. Documented in comments instead.
- (2026-07-28) **Kill switch keeps its name** (`rdf4j.lmdb.valueGc.deferForPinnedReaders`): it names the guarantee, not the mechanism. Disabled = eager gcIds, no new intents; existing durable records still drain.
- (2026-07-28) **Records of re-added (live) IDs are dropped at drain** rather than kept: a future removal re-records them at its own revision. Keeping them would need proactive supersede-cleanup on the re-add path, touching the hot add path for no correctness gain.

## Outcomes & Retrospective

- (2026-07-28) Risk item closed: `rg` confirms `ValueStore.gcIds` is called only from `LmdbSailStore.recoverRetiredValueIds` (open-time) and the sink's `runValueGcCascades` (flush-time); `handleRemovedIdsInValueStore()` has exactly one call site in `flush()`. No bypassing GC trigger exists.

All six milestones completed in one session (2026-07-28). Delivered: `RetiredValueIdStore` (three DBIs in the ValueStore env, boot-epoch persistence with one forced sync per open), `ValueStore` wrappers, the flush-path rewrite (`handleRemovedIdsInValueStore` → record-or-gc plus bounded drain with cascade two-stage treatment), the open-time recovery loop, and the removal of the `deferredUnusedIds` workaround (`PersistentSetFactory` maxdbs back to 2). Seven feature tests across `LmdbValueGcRetirementTest` (per-ID drain progress, supersede race, kill switch), `LmdbValueGcRecoveryTest` (restart recovery, bounded recovery), and the pre-existing `LmdbSnapshotValueGcTest`/`LmdbSnapshotInvalidationTest` are green; the full module shows zero new failures against the pre-existing baseline.

What remains (deliberately out of scope, candidates for follow-up plans): the flush() commit reordering (write-ahead intents before the TripleStore commit — see Decision Log), and any proactive retiring of stale cached branch snapshots on commit (today a stale cached pin is only retired when the next SNAPSHOT borrower arrives, which delays draining accordingly; correct but lazy).

Lessons: (1) the empirical failure mode (value→ID destroyed, ID→value kept) pinned the design early — the whole `gcIds` call must be deferred, not just the free-list drain; (2) the branch-level snapshot cache is an easy-to-miss "active reader" — any watermark-based reclamation test must account for it; (3) supersede semantics genuinely trigger in practice (the M3 test exercises the exact remove→re-add→remove interleaving), validating the by-ID currency check rather than a plain append-only queue.
