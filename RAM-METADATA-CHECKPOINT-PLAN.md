# Implement bounded RAM metadata with native checkpoints

This is a living ExecPlan governed by PLANS.md. Maintain Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective as implementation proceeds. User authorization on 2026-11-02 is to implement with Astra ultra, validate, commit and push to the existing draft PR, then benchmark against its preceding head and current main. Do not merge.

## Purpose / Big Picture

Ordinary statement writes currently modify many LMDB metadata records per quad. This change keeps recent count and directory changes in bounded RAM, while preserving exact reads, reference-based reclamation, transaction rollback, native snapshots and crash recovery. A periodic maintenance transaction folds recent changes into the existing native metadata database. A clean shutdown checkpoints; an interrupted dirty store rebuilds from authoritative adjacency pages and deltas before queries or reclamation start. Success means fewer ordinary metadata puts without an incorrect count, invisible statement or incorrectly retired identifier. Performance remains an experimental result, not an assumption.

## Progress

- [x] (2026-11-02 20:03Z) Verified draft PR 6352 head ddb7a3c3b6fb674934b567f37c8903974213a4e2 and current main 23a0cfa42785797613ffb7e91c091f7e6f41684b.
- [x] (2026-11-02 20:25Z) Created clean worktree /tmp/rdf4j-ram-metadata-checkpoint on GH-0000-ram-metadata-checkpoint. Failed aggregation experiment remains untouched in another worktree.
- [x] (2026-11-02 20:29Z) Captured in-repo failing LmdbRamMetadataCheckpointTest: one test, one assertion failure, zero errors. Logical count/scan pass; native total was prematurely persisted.
- [x] (2026-11-02 20:55Z) Implemented and compiled bounded overlay, adapters, native lifecycle/publication, checkpoint admission, periodic maintenance and raw recovery.
- [x] (2026-11-02 21:24Z) All 91 final focused lifecycle, pressure, cursor, recovery, shutdown and planner-budget tests pass; independent review has no remaining source blocker.
- [x] (2026-11-02 21:53Z) Repaired the prepared-bulk certificate boundary; the final deduplicated 62-class gate passes 582 tests with zero failures/errors and two declared platform skips. Module installation, reciprocal review, formatting and copyright checks pass.
- [ ] [in_progress] Recheck the remote head, commit and publish the exact tested tree to the existing draft PR branch without force or merge.
- [ ] After push, run matched previous-branch/current-main benchmarks and monitor exact-commit CI to terminal status; report actual results and limits.

## Surprises & Discoveries

Pooled Txn.close in RESET mode may keep its native snapshot active. Metadata ownership follows native reset/abort/renew, not outward close. The native commit API consumes its handle even on error. A standalone prototype initially double-aborted that handle; the corrected proof tests both data and checkpoint failures.

Metadata is not a planner cache: families 1–7 determine row discovery, context enumeration and reference exclusion. The 549,994-quad fixture has 2,286,952 metadata records; compact payload alone is about 47 MiB before versions/indexes. Therefore a full resident map is not the default.

Combined synthetic native tests passed bounded primitive overlays, prefix checkpoints, old snapshots, pressure refusal, commit failures, publication fencing and reopen. In 32/64 MiB dispersed stress cases, native MAP_FULL occurred before the Java overlay cap; releasing the held reader allowed the same attempted native commit with the same map size. The Java budget does not bound native MVCC pages or RSS. These synthetic proofs are not production integration or throughput evidence.

Production verification at 20:56Z: all 11 direct engine tests pass, including regressions that first reproduced retained prepared roots after a consumed commit, unreleased cursor slots after native snapshot retirement, writer cursor invalidation and the wrong native MAP_FULL exception type. Six of seven manifest-admission tests pass; the prepared-store clean admission fixture is still under investigation. The first adapter run passed 34 of 39 tests; five failures exposed an unattached standalone test wrapper and that fixture was corrected without weakening its oracle. The complete integrated failure/crash suites remain in progress; none of these partial runs substitutes for the final regression gate.

Independent review found that skipping hidden tombstones inside merged planner cursors can exceed an outer native-read budget. A cursor-level work/cancellation hook and deterministic budget regression are being added. Compaction's families 8–10 cursor remains native-only, so ordinary commit does not inadvertently force covered metadata back to disk.

## Decision Log

Use the existing LMDB metadata database as the cold base plus immutable sorted RAM segments of absolute final values. A zero value is a deletion marker, not an absent override. Newer segments override older ones. Each native reader owns the exact overlay root compatible with its own native snapshot. This makes in-place prefix checkpoints valid: applying an oldest prefix to the base and retaining its newer suffix yields the same logical map.

Move only families 1–7 initially; keep dirty counters and historical compaction candidates (families 8–10) native. Keep dictionary datatype/nested ownership separate. Preserve unsigned encoded-key ordering and role multiplicity when one identifier occupies several quad roles.

Use a default 16 MiB owned-overlay cap including fixed writer/checkpoint reserves, primitive segment capacities, root indexes and pins. Bound private updates and segment fan-in. Before a new allocation would exceed capacity, or before a live writer metadata cursor opens, fold the committed overlay then private assignments into that same uncommitted native transaction. Continue native-exact until that physical attempt ends. This introduces no extra user-data commit and no snapshot eviction. Native exhaustion still follows ordinary abort/growth/replay policy.

Keep the manifest in the existing namespace database under an internal key, avoiding extra records in the adjacency metadata DB that would invalidate its fresh sorted MDB_APPEND path. The manifest records coverage/format, clean/dirty state, base epoch and actual native checkpoint transaction ID. It certifies the atomic state of its own native environment; it is not an integrity checksum and does not claim to detect arbitrary cross-store record copying or validly encoded corruption. Inspect it before startup housekeeping writes. The first deferred mutation stages DIRTY in the same native transaction as data; process dirty state changes only after successful native commit. Exact-native/checkpoint commits can stamp CLEAN only when their complete committed view is native.

Prepare every immutable successor and publication descriptor before native commit. Record native success before any fallible publication hook. Publish the matching root while the existing native read-admission barrier is held, before tracked-reader reset/renew or callbacks. Any post-native-success root failure fences dependent operations; it cannot be reported as rollback or optional derived-cache failure.

A metadata checkpoint obtains ordinary store writer admission and performs only a bounded oldest-prefix fold. It must not implicitly compact authoritative pages or increment RDF data revision. Idle scheduling is supplemented by threshold/age pressure and native-exact fallback; idle-only progress is insufficient. Every targeted old segment eventually drains under fair successful writer turns; continuous new writes need not reach CLEAN. Shutdown can stop admissions and finish a finite suffix or leave DIRTY safely. Its deadline is checked between bounded transactions, not a hard wall-clock limit on one native commit. The public Sail preflight must stop the worker before changing lifecycle state or releasing the directory lock; interruption must remain retryable.

No older-binary migration work is required for this unmerged feature. Existing unrelated compatibility paths are not removed opportunistically.

## Context and Orientation

The module is core/sail/lmdb. TripleStore owns the native triple environment, physical writers, replay/map growth and commit bookkeeping. TxnManager owns pooled native readers and its native-operation barrier. LmdbSailStore coordinates logical writer admission, dictionary-before-triple publication and cleanup. LmdbPersistentAdjacency encodes pages/deltas and metadata. LmdbContextIdIterator, LmdbAdjacencyRangePlanner and LmdbPersistentCardinalityEstimator also read metadata directly and require adaptation.

New LmdbMetadataCheckpoint owns primitive RAM segments, private writer metadata, snapshot pins, manifest encoding and merged metadata cursors. It never begins or commits a native transaction itself. TripleStore calls beginWriter after native begin, aborted for every native abort/replay attempt, beforeCommit before ownership transfer, and committed immediately after successful native commit. TxnManager calls bindSnapshot at owned native start/renew and releaseSnapshot only at actual reset/abort.

A segment contains a bounded number of canonical typed keys and absolute counts. A root is a bounded ordered list of references to segments; old roots retain shared arrays once in the ownership ledger. A metadata cursor merges a native sorted cursor and segment positions without materializing an entire result. Writer cursors instead force native-exact mode before opening and preserve existing mutable cursor semantics.

## Plan of Work

First implement the engine and its isolated tests, then attach all authoritative scalar/directory/reference reads and writes. Raw standalone adjacency objects remain native-exact for low-level fixtures and recovery; production TripleStore attaches the engine. Initial sorted loading explicitly uses exact native mode. Physical native statistics may remain raw, but logical counts may not bypass the adapter.

Add reader lifecycle callbacks without changing pooling behavior: an unchanged pooled snapshot keeps its existing root; renewal acquires a new root under the admission barrier. Family admission balances pins on failure. Foreign writer wrappers borrow the private writer view and own no native lifetime.

Inspect startup manifest before native setup. If missing/stale, keep admission closed, mark DIRTY, delete only families 1–7 in bounded transactions, then rebuild from raw ordered pages plus canonical deltas. Never use RowsCursor or stale directories to discover source quads. Freeze authoritative data/compaction across chunks and resume by the exact full unsigned edge key. Count outgoing explicit/inferred quads once, derive both directions, and verify reverse-plane membership. Preserve families 0 and 8–10. Finalize CLEAN only after all count contributions and validation commit.

Checkpoint and shutdown use normal ownership with a limited metadata-only transaction body. Never wait for a checkpointer while retaining the writer it needs, and never hold a metadata accounting lock while waiting for native barriers, reader permits or map growth. Native failures abort the attempt or trigger existing replay; publication after native success must fence safely on failure.

## Concrete Steps

Use /tmp/rdf4j-ram-metadata-checkpoint as working directory. Source /tmp/rdf4j-tools/env.sh for Java 25 and Maven 3.9.11. The cached Maven repository is /workspace/scratch/bfa8fbb86df5/rdf4j-persistent-adjacency-repairs/.m2_repo. One heavy JVM runs at a time. The worktree uses sparse checkout to avoid a roughly 478 MiB unrelated site directory; core, testsuites, scripts, eclipse-settings and build configuration are present.

Focused test command:

    mvn -B -ntp -f core/sail/lmdb/pom.xml -o -Dmaven.repo.local=/workspace/scratch/bfa8fbb86df5/rdf4j-persistent-adjacency-repairs/.m2_repo -Dtest=LmdbRamMetadataCheckpointTest -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=512m test

Do not use -am or -q for tests. Preserve current failure evidence under adjacency-input/ram-metadata-implementation-20261102 in the shared workspace; do not overwrite it with later green output. Expand focused suites only after integration compiles. Run the required license/copyright/Spotless tooling and inspect reports rather than assuming a successful process means all tests ran.

## Validation and Acceptance

The first test must become green while proving both exact logical reads and deferred native metadata. Add deterministic schedules for an aborted first dirty writer, rollback after prior dirty commits, old readers through data/checkpoint commits, pooled unchanged reuse and actual renewal, family admission failure, foreign writer cursors with later mutation, pressure fallback, native put failure, consumed commit error, replay, postcommit publication fencing, checkpoint prefix failure, clean reopen and dirty subprocess halt/rebuild. Include repeated identifiers across quad roles, explicit/inferred overlap, contexts and high-bit unsigned IDs.

Test metadata-only commits against expected native transaction identity and unchanged RDF data revision. Test raw rebuild with directories missing, tombstone-bearing deltas and inconsistent reverse authority. Test reference-based cleanup against old readers and current roots. Verify actual supported durability modes separately; a process halt is not a power-loss proof.

After correctness and review, re-read remote PR head, incorporate external changes rather than force overwrite, commit with GH-0000 prefix and push existing branch. Then benchmark full ordinary commit cost, 550K input-to-ready, selected directory/read paths and dirty recovery using matched durability settings and exact live/cold-reopen oracles. Compare both previous PR head and current main. Report speedups/regressions honestly; synthetic timing is not a production benchmark. Monitor exact pushed commit's CI until terminal or a genuine external blocker.

## Idempotence and Recovery

Never reset or overwrite unrelated working trees. Retain the frozen old runtime and unpublished experiment. Native commit handles are cleared before committing; failed commits are not aborted twice. Replay reconstructs private state from the committed predecessor and repeats native-exact folding if needed. Incomplete recovery/checkpoints remain DIRTY and can be restarted. Cleanup errors after a confirmed native commit must not change its recorded outcome. Do not weaken tests, durability, snapshot validity or budget enforcement to make a benchmark pass.

## Artifacts and Notes

Initial in-repo red:

    Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    family 1 total stays absent from the native base until metadata checkpoint
    expected: -30798 (MDB_NOTFOUND) but was: 0 (MDB_SUCCESS)

Source baseline and old benchmark runtime are preserved; benchmark output will be recorded only after the production commit is pushed, as requested.

## Interfaces and Dependencies

Use existing LWJGL LMDB bindings, existing native transaction/barrier ownership and existing primitive Java arrays. Add no external dependencies. LmdbMetadataCheckpoint exposes inspect/writeClean, bindSnapshot/releaseSnapshot, beginWriter/aborted, readCount/adjust, forceNativeWriter, beforeCommit/committed, stageCheckpoint and a merged cursor. LmdbPersistentAdjacency exposes attachment plus bounded clear/rebuild chunk helpers. TripleStore exposes package-private checkpoint diagnostics and deterministic test seams without reflection-based state mutation. The implementation may refine signatures only with matching tests and this plan updated.

Revision 2026-11-02 20:33Z: moved from completed design spike to authorized production implementation after a captured native-deferral red; all implementation gates and ownership are explicit.

Revision 2026-11-02 20:57Z: implementation compiles; ownership regressions reproduced and repaired, direct engine gate green, integration and independent budget review active.

Independent final-diff audit (21:05Z) identified two lifecycle defects before publication: a closed TxnManager retained the new owner callback, and public shutdown lacked metadata-worker quiescence in its existing preflight. Dedicated deterministic regressions are being captured before the narrow fixes. Prepared clean-open failures were traced to fixtures lacking lazily-created namespace metadata; the paired dictionary CLEAN marker and native triple ID already matched.

Revision 2026-11-02 21:24Z: final focused gate passes 91 tests with zero failures/errors/skips. Both concurrent native-retirement races and callback failure/permit paths were first reproduced, then fixed by retaining registry visibility through retirement, joining the transaction monitor before manager ownership transfer, recording native consumption before callbacks, and returning ended outward leases in cleanup. Public shutdown preflight and all prepared clean-admission tests pass. Broader existing regression gate is running; publication and performance remain pending.

Revision 2026-11-02 21:40Z: broader regression exposed a separate prepared-bulk completion path that retained a small RAM metadata tail after binding the dictionary/native certificate; later TripleStore.close advanced the triple transaction and correctly failed sealing. Existing LmdbPreparedBulkStoreTest reproduced 2 failures and 27 errors out of 40 cases. Prepared bulk batches now establish native-exact mode inside their existing abort/growth/replay boundary, and terminal generation completion explicitly requires native-exact metadata before binding the certificate. Sealing assertions are unchanged. Remaining broader classes continue against the pre-fix compiled source; exact revised-source verification follows.

## Outcomes and Retrospective

Final local acceptance at 2026-11-02 21:53Z: 582 tests across 62 classes, zero failures, zero errors and two platform-specific skips. This deduplicated gate includes the new focused tests and existing prepared bulk, frozen replay, growth, publication, compaction, reference/GC, startup and shutdown regressions. Prepared bulk passes all 40 cases and frozen replay all 99. The module is packaged and installed; the production jar SHA-256 is `ff7a61280475a3822a513277de6489d5c4c611bac902cb4c0d11ee321b822170`. Exact reports and source hashes are retained under `adjacency-input/ram-metadata-implementation-20261102`.

The broader gate mattered: it exposed a separate bulk-generation certificate path not exercised by ordinary Sail shutdown. Keeping prepared bulk native-exact inside its existing transaction/replay boundary fixed the ordering without relaxing certification. Existing raw native-metadata test oracles had to become full snapshot-bound merged-map oracles; native authority/page/delta checks and tombstone/reference coverage remain. Replay instrumentation now counts metadata maintenance separately from application attempts, rather than broadening exact replay expectations. Native success without a matching RAM publication deliberately requires reopen before another writer, and the revised uncertainty test verifies the original exact committed state before a successful post-recovery follow-up.

A runtime reset and a later external exit137 interrupted earlier broad attempts. Their logs are retained and are not reported as passing. The successful final gate used a 256 MiB Maven launcher, the unchanged 512 MiB test heap, unchanged maps and a shared cross-namespace JVM lock. Local correctness is not a full CI result or a performance result: exact-published-commit CI and matched post-push benchmarks against previous head/current main remain pending.
