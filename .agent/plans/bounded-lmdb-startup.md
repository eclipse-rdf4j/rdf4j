# Bound LMDB startup and preserve crash-safe ownership

This ExecPlan follows root `PLANS.md` and is a living document. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current. All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-merge` unless another working directory is stated. The user approved implementation of the complete plan on 2026-10-23 and subsequently explicitly requested an issue, scoped commit, normal push and draft pull request before continuing. Keep the current branch and exclude unrelated work, logs and datasets from publication. Do not remove untracked files or overwrite unrelated work.

## Purpose / Big Picture


An existing LMDB repository must become ready for normal reads and writes without walking its statements, dictionary, garbage-collection queues, or obsolete index trees. Foreground startup, measured from store construction through repository initialization, must perform at most 1,000 logical native reads. The performance target is readiness within ten seconds for a billion-statement repository; the user will run that timing test on their own store. The implementation must provide a reproducible acceptance runner and truthful counters instead of manufacturing a billion-statement fixture or extrapolating timing from a smaller store.

After a normal JVM crash, the repository must establish quickly whether component reference counts are exact. A missing proof must allow conservative reads and writes while retaining all values and suspending reclamation and identifier reuse. Requested index layout changes must build in the background while the committed indexes continue serving correct results. Exhaustive validation and repair become explicit maintenance operations, never an implicit startup fallback.

## Progress


- [x] (2026-10-23) Inspect startup paths and preserve baseline.
- [x] (2026-10-23) Approve bounded admission and crash authority.
- [x] (2026-10-23) Preserve startup work through test-only publication.
- [x] (2026-10-23) Implement atomic count authority with regressions.
- [x] (2026-10-23) Implement bounded admission and lazy auxiliaries.
- [x] (2026-10-23) Implement resumable background index layout changes.
- [x] (2026-10-23) Harden native errors before acceptance runs.
- [x] (2026-10-23) Merge latest main while preserving work.
- [x] (2026-10-23) Validate incoming main before startup continuation.
- [x] (2026-10-23) Resolve replay and snapshot integration regressions.
- [x] (2026-10-23) Integrate startup counters and acceptance runner.
- [in_progress] Publish scoped issue commit and draft.
- [ ] Verify focused suites and module results.
- [ ] Format and audit final cohesive changes.

Exactly one entry is `in_progress`. Other implementation work may proceed in parallel under separate file ownership; update this progress list when an integrated milestone completes.

## Surprises & Discoveries


The previous catalog implementation starts at `MDB_FIRST` and attempts to open dictionary rows as named databases. A focused regression proved this before production changes: `LmdbLegacyIndexDiscoveryTest#catalogScanSeeksPastLegacyDictionaryRecords` failed with `expected: 17 but was: 0`, where 17 is `MDB_SET_RANGE` and 0 is `MDB_FIRST`. The earlier legacy-only range patch is insufficient for the approved scope and remains a starting point rather than the final solution.

Component reference counts describe the active value-owner graph inside the values environment, not the number of statement occurrences. Literal and IRI owner mappings contribute datatype/namespace dependencies; live triple-term SPOC rows contribute their three components. Normal dictionary commits already flush count deltas in the same LMDB transaction as owner changes. A dictionary commit followed by a failed triple commit can leave orphan owners, but their component counts remain exact.

`TripleStore.filterUsedIds` may scan an entire statement index when a leading index is unavailable. Startup currently calls it for each unused value, and retirement recovery drains every batch. A batch-size limit therefore does not bound startup work. `mdb_drop` can also walk a whole tree within one native call, so call counting alone cannot justify a ten-second bound.

Optional hash-cache validation computes a checksum over the entire hash file. Optimizer snapshot loading reads whole directory indexes even when sketch payload loading is lazy. Namespace snapshots are authoritative data and currently decode eagerly. These paths require separate treatment; skipping dictionary discovery alone cannot meet the goal.

The persistent theme fixture at `core/sail/lmdb/target/lmdb-theme-query-benchmark/complete` has 14,561,110 statements, 10,276,590 main value entries and eleven namespaces according to `expected-db-file-sizes.properties`. It is useful for a local acceptance run but is not evidence for a billion statements.

The constructor-through-readiness matrix now covers clean stores, a real child-JVM crash, authentic legacy stores, absent/bad/stale count authority, 1,100 durable retirement candidates, and the largest supported configured index sets. Each successful case preserves an acknowledged statement and namespace and commits a new statement after readiness. Malformed fixed authority fails within the same bounded budget before advancing the native values transaction. Wrapper-accounting and source-inventory tests verify that overload delegation counts once and production has no direct `nmdb_*` invocation bypass. First legacy namespace mutations preserve held lazy snapshots and rollback; two real JVM cuts preserve either the original file authority or the complete committed native snapshot.

Checkpoint 11 ran 36 tests with one genuine revision-cohort failure and no errors. All 29 admission tests, three counter-accounting tests and two in-range catalog rejection tests passed. The combined JNI-wrapper plus native transaction-header totals were CLEAN 252, PROCESS_CRASH 199, LEGACY 185, absent/bad/stale proof 217 each, large retirement queue 402, maximum configured indexes 424 and maximum physical indexes 533. These are small deterministic fixtures, not billion-statement timing evidence. A child-JVM regression proved that a JVM-local revision counter could collide with a persisted unused-ID prefix and make a first writer physically drain historical backlog; the corrected reproducer now justifies a fixed-extremum revision bootstrap. Exact commands and reports are retained under `logs/bounded-startup-focused-checkpoint-11/`.

Checkpoint 12 ran 284 tests with 23 failures and two fixture setup errors. The corrected revision-cohort child cases, 38 count-authority cases, 23 legacy bootstrap cases and three counter cases passed. Remaining old tests still expected eager constructor/close GC or constructor-completed reindexing; their physical capacity, data preservation and reader-lease assertions are being exercised at explicit reconstruction and bounded post-ready maintenance instead. Two optional-job status regressions did not reach their assertions because their 64 MiB adjacency fixture was below the existing 256 MiB configuration minimum; correct the fixture and observe an actual red before changing status behavior. Reports remain under `logs/bounded-startup-focused-checkpoint-12/`.

Checkpoints 13–15 separately observed and fixed pending-work reporting for still-running optional jobs, unsupported count-certificate loss during read-only open and owner mutation in both formats, and a native map-growth diagnostic undercount. All assertions were preserved before production changes. The expanded randomized checkpoint 17 ran 403 tests; the complete original crash suite and all count, GC, value, descriptor, statement/term background and growth cases passed. Its two failures were in the coordinator's custom status fixture, which deferred auxiliary initialization accidentally. Checkpoint 18 corrected that factory to the normal owning-store flags and passed all 48 selected admission/counter/overlay/cleanup/namespace tests with randomized method seed 8675309. Test-only synchronization waits for real term cleanup before holding an overlay scan: reopen cleanup may legitimately cancel an earlier overlay generation. Exact commands and retained reports are under `logs/bounded-startup-focused-checkpoint-13/` through `logs/bounded-startup-focused-checkpoint-18/`; full-module and supplied-store acceptance evidence are still pending.

The required copyright audit and exact root formatter both completed successfully before acceptance; the formatter changed no files and the unrelated Theme benchmark diff hash remained `a05c2287bb5a20184504ce1110cca2b9f1d6198a32dbec27904262a3411a3a46`. The final read-only maintenance audit then found two unchecked native cursor results: CSPO lookup during unused-ID cleanup can treat a native error as absence after inserting a free ID, and retirement polling can return a partial batch after an error. Acceptance and full-module execution were held before starting. Add injected-error regressions and observe their reports before propagation fixes; reformat afterward. Audit artifacts are under `logs/bounded-startup-final-audit/`.

Checkpoint 19 observed six failures among ten injected-error tests before production changes: final CSPO lookup and retirement FIRST/NEXT errors were swallowed; related by-ID/unused-NEXT cases already propagated. Minimal result checks now route both through normal rollback. Checkpoint 20's related Surefire selection passed all 115 cases, including unchanged native-error tests checking payload/free-ID/queue/count/proof rollback and successful retry. That invocation also started default Failsafe integration tests; retain its terminal results before acceptance. If all default integration tests pass and final formatting changes no source bytes, run the remaining entire Surefire module with `-DskipITs` and report the complementary scopes accurately instead of repeating integration tests. Parent explicitly approved this verification split.

Checkpoint 20 is now terminal: Surefire passed 115 tests; default Failsafe ran 159 cases with two failures, no errors and 105 skips. `LmdbAutogrowIsolationIT` retained the old READ_COMMITTED rows after paired map growth, and `LmdbFrozenSpillReplayIT` failed its 300,000-row/128 MiB child with a capacity error plus namespace-state reconciliation failure. These are observed integration regressions to triage before acceptance, not a full green. No billion-statement fixture was generated. Default integration tests opened the existing theme artifact and advanced its values-file mtime while leaving its size, triples file and sidecar bytes unchanged; preserve the artifact instead of restoring it. The separately retained APFS acceptance copy remains unopened. Exact terminal evidence and preservation audit are under `logs/bounded-startup-focused-checkpoint-20/`.

The user explicitly prioritized merging latest `main` into the current branch before further startup work. Both workers reached a stable no-edit/no-Maven boundary; FrozenReplay 99 passed before the pause and its evidence is retained under `logs/bounded-startup-frozen-replay-green/`. The namespace fix also made the unchanged READ_COMMITTED integration reproduction pass; the 300,000-row/128 MiB spill child now reaches successful commit and fails on reopening with heap exhaustion. Its heap dump, GC/NMT logs and source/compiled identities remain under `logs/bounded-startup-spill-oom-diagnostic/`. No cache-production patch has been made. Snapshot all tracked/untracked WIP, merge fetched `origin/main` into the current branch without switching or pushing, verify preservation, then resume this plan on the merged head.

The merge completed as a fast-forward from `78ab8734ac1d57915fa4a9ead495ba845af7f204` to fetched `origin/main` `74a966e1612272f920458806fbd9f32e57ee2c1b` on the same branch. Incoming changes touched 37 paths and overlapped none of the 61 WIP paths. No conflict, stash, new local commit or push was needed. All 36 tracked and 25 untracked WIP files, and the full tracked patch, were byte-identical immediately after the merge; the index and unmerged set were empty and `git diff --check` passed. Backup: `/private/tmp/rdf4j-pre-main-merge-x9h3zbkm`; proof: `logs/main-merge/preservation-proof.json`. Incoming query-renderer and CSF focused verification is pending before startup continuation.

Incoming-main validation passed at `74a966e1612272f920458806fbd9f32e57ee2c1b`: `IrTraversalTest` 15 tests and the LMDB parallel/CSF selection 46 tests, all green with no skips. Commands, reports and prerequisite install logs are retained under `logs/main-merge/validation/`. Startup work resumed on this head. An initial shallow heap-root search identified a strong load-frame reference to the closed `ProbeLmdbStore`; a scalar-receipt helper removed that test-lifetime root while preserving every 300,000-row/128 MiB spill/growth/replay/ACK/content/namespace assertion. The unchanged constrained test still failed after that refactor, so it was not sufficient to explain or fix the regression.

The corrected heap-root search reached depth 40 and found both closed stores strongly retained at depth 15: `Thread.threadLocals -> cursor pool -> PooledCursor.owner -> TxnManager.readerRetirementListener -> TripleStore -> ValueStore`. The new retirement callback captured its owning store; a pooled cursor could therefore retain the closed store and its caches after shutdown. This is a real lifecycle regression in the startup implementation, not merely a fixture or pre-existing cache policy. `TxnManager` now detaches the listener after all reader/native-pool close attempts, before rethrowing aggregated close failures, preserving live retirement notifications. The exact constrained integration test then passed: one test, zero failures/errors, 128 MiB, 300,000 rows, with exact data and namespace assertions before and after independent reopen. Retained red: `logs/bounded-startup-spill-helper-lifetime-red/`; corrected strong-root evidence: `logs/bounded-startup-spill-oom-after-helper/`; green: `logs/bounded-startup-spill-oom-green/`. No heap increase, forced GC, row reduction or production cache clearing was used. Focused listener lifecycle and full map-growth integration verification follow before acceptance.

The full `TxnManagerTest` class passed 44 tests, including RESET/ABORT variants that verify live reader retirement notifications and safe late handle release after manager shutdown. The subsequent complete `LmdbAutogrowIsolationIT` run passed eleven cases but independently reproduced the READ_COMMITTED defect in `unobservedReadCommittedResultReplaysExactRowsAcrossPairedMapGrowth`: authoritative writer statement/namespace assertions passed, yet the unobserved reader returned the old three rows. The earlier narrow-method green therefore did not settle replay admission. Retained command/report: `logs/bounded-startup-autogrow-full-red/`; listener coverage: `logs/bounded-startup-txn-manager-green/`. Fix the general reserved/read epoch admission path against this actual red before the frozen final formatter, full module and acceptance pipeline.

A stable smallest integration regression now orders the problematic states explicitly: hold an unobserved READ_COMMITTED result, force both maps to grow, cross reopened admission and assert a completed replay reservation, then commit a matching row and namespace without another resize. `unobservedReadCommittedReplayAdmitsPublicationAfterItsReservedEpoch` failed with one test/one failure because the replay omitted that acknowledged later row; all reservation/growth/publication preconditions passed. Exact command and report are retained under `logs/bounded-startup-reserved-epoch-red/`, before replay production changes. An earlier fixture-only reservation-precondition failure remains separately retained and is not presented as this behavioral evidence.

Operation-scoped read leases now consume their one-use reserved view under the coordinator lock; a pair superseded before admission is abandoned and closed outside that lock, and normal paired publication/native admission captures its replacement. Transaction-snapshot leases retain their existing shared epoch. Nested captures still use the active admitted view. The unchanged smallest regression passed, followed by all thirteen `LmdbAutogrowIsolationIT` cases, including the twelve original isolation variants. The new fixture crosses actual OPEN admission after seed so unfinished seed growth cannot invalidate its reservation precondition. Retained greens: `logs/bounded-startup-reserved-epoch-green/` and `logs/bounded-startup-autogrow-full-green/`. Related replay, checkpoint, generation and lifecycle unit selections follow before the final frozen-source pipeline.

The related replay/lifecycle/generation unit selection passed all 107 cases: 25 map-growth lifecycle, one context admission, three standalone replay, eleven exposure, four checkpoint, one background generation, eighteen statement reindex and 44 transaction-manager cases. These complement the exact 128 MiB/300,000-row integration green and the thirteen isolation cases. The remaining pipeline now freezes Java sources for copyright/signature checks, the exact required root formatter, a current-source complete LMDB unit/default-integration run, and supplied-copy startup acceptance. Historical checkpoint 20 integration results do not represent the later namespace/listener/replay source identity.

The user then explicitly prioritized publication: ensure an equivalent GitHub issue exists, commit and push the scoped startup changes, open a draft PR, and continue verification afterward. The final copyright audit and exact root formatter completed successfully before this pause; the full module run had not started and the retained acceptance copy remains unopened. Preserve the unrelated Theme benchmark patch and all pre-existing untracked work; publish only this implementation, its tests/runner/docs and this plan. The draft must state that final full-module and supplied-store timing evidence are outstanding.

Issue [#6260](https://github.com/eclipse-rdf4j/rdf4j/issues/6260) now records the complete bounded-startup/count-authority/background-reindex scope and outstanding billion-statement timing. Search found related bulk-loader issue #6200 and count-correctness issue #6095, but neither covers this complete admission/recovery contract. The GitHub connector rejected issue creation with `Resource not accessible by integration`; the already-authenticated GitHub CLI created it successfully. Keep the current branch, use `GH-6260` for this scoped commit/PR, and retain all local logs and datasets without publishing them.

Checkpoint 3 ran 99 tests with four failures, three errors and one opt-in skip. The held-source planner regression, fifteen statement tests, ten real-process count-crash cases, fourteen checkpoint cases, five term-background cases and ten legacy count cases passed. New reds exposed missing/zero trusted count decrements, descriptor ordering, and three fixture/capacity issues. Checkpoint 4 ran 191 tests with forty failures and one error while broadening to existing compatibility suites. Most existing failures characterize the deliberately changed foreground validation/auxiliary timing; preserve their semantic assertions while moving exhaustive checks to explicit verification and waiting for requested background completion. Genuine new reds prove self/mutual owner cycles escaped read-only verification, retained durable queue work disappeared from status, and a late descriptor `mdb_put` capacity failure bypassed admitted replay. Reports and exact commands are retained under `logs/bounded-startup-focused-checkpoint-3/` and `logs/bounded-startup-focused-checkpoint-4/`; neither run is a full green.

## Decision Log


Decision: Startup uses bounded metadata and structural checks; deeper corruption is detected when affected records are accessed. Exhaustive verification is explicit maintenance. Unsafe authoritative state fails quickly before writable mutation. Rationale: exhaustive proof cannot be independent of data size. Date/author: 2026-10-23, user-approved.

Decision: Preserve existing durability settings and make no new power-loss guarantee. The crash authority is about process/JVM death and coherent LMDB transaction snapshots. Rationale: the user explicitly retained current synchronization choices. Date/author: 2026-10-23, user-approved.

Decision: Add a separate versioned `refcounts-exact` record in `gc_meta`. Do not conflate it with existing whole-store CLEAN metadata or legacy marker zero in `ref_counts`. Rationale: exact component counts can survive a crash without the values and triples environments having a whole-store clean-close seal, and older format-2 marker readers must remain compatible. Date/author: 2026-10-23, user-approved.

Decision: Missing, stale, unsupported, malformed or externally invalidated exact-count authority opens conservatively with safe reads and writes. Disable reclamation and free-ID reuse, bypass untrusted count payload reads/updates, retain durable candidate queues, and never promote the proof on ordinary writes or close. Rationale: preserving garbage costs space but preserves RDF data; full verification can restore exactness later. Date/author: 2026-10-23, user-approved.

Decision: Before a conservative format-2 owner mutation skips count deltas, remove the historical one-byte completion marker in the same native transaction. A read-only open/close may retain it. Rationale: an older reader cannot interpret `gc_meta/refcounts-exact`; absence of its historical marker makes it rebuild instead of trusting counts that this writer intentionally stopped maintaining. Date/author: 2026-10-23, coordinator and parent review.

Decision: Requested index layout changes build in the background. Use canonical inactive permutation names, atomic dual writes, short read batches, membership rechecks in the staging writer, durable progress and atomic manifest publication. Rationale: existing indexes can remain authoritative without a long pinned snapshot or a second recovery journal. Date/author: 2026-10-23, user-approved.

Decision: Do not generate a billion-statement fixture. Supply an acceptance runner with constructor-to-ready timing, foreground logical-read categories, count trust, pending maintenance, and separately measured first-query latency. Rationale: the user will personally verify the billion-statement timing. Date/author: 2026-10-23, user-approved.

Decision: Keep Maven and formatting serialized across workers. Initial mandatory root offline clean install already succeeded in this conversation. Rationale: the checkout and its `.m2_repo` are shared and tests must resolve the current installed artifacts. Date/author: 2026-10-23, coordinator.

Decision: Perform one bounded post-ready retirement pass and one bounded persisted-unused pass, with bounded maintenance at later publications. Preserve unresolved and unvisited candidates durably and report retained work; index/auxiliary waits do not promise queue exhaustion. Rationale: automatic reclamation must begin after readiness while preserving uncertain liveness and reader revisions; continuous background exhaustion is outside this startup requirement. Date/author: 2026-10-23, parent clarification of the approved bounded-pass design.

Decision: Owning-store close retains durable unused candidates instead of reopening a closed transaction manager or draining the whole backlog. Exercise reclamation capacity during bounded post-ready maintenance after readers are retired. A live forward owner mapping dominates an obsolete unused hint; verification and reconstruction preserve that owner and must not place its ID on the free list. Rationale: those hints are derived, while the committed owner graph and reader leases establish data safety. Date/author: 2026-10-23, coordinator and parent review.

## Outcomes & Retrospective


The initial offline root clean install passed. The earlier focused catalog regression failed before its patch and passed after; the legacy discovery class passed 77 tests. The pre-plan full LMDB module run terminated with failures, including three node-predicate expectations and a `ThreeTierEngagementCensusTest` fallback expectation. Preserve these as baseline observations, not as proof that failures are unrelated to the final implementation. The current implementation and acceptance work remain incomplete.

On 2026-10-23 the user explicitly paused startup implementation and required predicate-enumeration test fixes to be committed and pushed first. Startup sources and workers are frozen at HEAD `79b395929c9e5efc6313794858e05f95362645b4` on `GH-0000-shacl-lexer-millisecond-deadlines`, tracking `origin/GH-0000-shacl-lexer-millisecond-deadlines`. The startup changes are uncommitted and must not enter that test-only publication. A legacy-marker focused run (session `77866`) had already begun; it may finish and preserve evidence, but no further startup Maven jobs or edits may begin until the test-only commit is pushed and verified and implementation is explicitly resumed.

That prerequisite is now complete. Test-only commit `78ab8734ac1d57915fa4a9ead495ba845af7f204` was verified in an isolated exact-baseline checkout, pushed normally, and safely fast-forwarded into the original branch. Coordinator and parent independently verified the original, isolated and live remote commit identities. Its diff contains exactly the four authorized predicate/cancellation test files; original startup and benchmark WIP remained byte-identical. The final randomized broad selection passed 159 tests with one opt-in skip, and seven focused/repeat cases passed with all four projection switches initially false. Evidence, executable commands and publication proof are retained in `logs/predicate-enumeration-publication/`. Startup implementation and both workers were explicitly resumed after this verification; the new baseline is `78ab8734ac1d57915fa4a9ead495ba845af7f204`. The next startup `mvnf` run must reinstall the unpublished sources because the shared dependency cache was last populated from the isolated committed baseline.

The first integrated focused checkpoint compiled successfully and ran 26 tests: eleven count-authority, four term-background, and six coordinator startup tests passed; five statement-background tests produced one failure and one error on reopened staging DBI handles. Evidence is retained under `logs/bounded-startup-focused-checkpoint-1/` with full log `logs/mvnf/20261023-144101-verify.log`. The identified resume-handle fix is not applied. This is partial subsystem evidence, not full-module success.

## Context and Orientation


`core/sail/lmdb` contains two native LMDB environments. `ValueStore.java` owns RDF value payloads, reverse mappings, triple-term indexes, allocation metadata, component counts, free IDs, unused values and durable retirement queues. `TripleStore.java` owns explicit/inferred statement indexes, context metadata and authoritative namespace snapshots. LMDB transactions are atomic within one environment. The owner in `LmdbSailStore.java` commits dictionary records and retirement intents before the corresponding authoritative triple commit.

`LegacyStoreDiscovery.java` currently performs read-only preflight, catalog enumeration and legacy deep validation. `PreparedBulkStore.java` admits matching native CLEAN current stores cheaply. `StoreProperties.java` holds sidecar configuration and verified native transaction information. `LmdbStartupDiagnostics.java` supplies the existing calling-thread observation seam. `NamespaceStore.java`, `ValueStoreHashFile.java`, `LmdbFilterSelectivityStats.java`, and the sketch estimator under `core/queryalgebra/evaluation/.../sketch/` contain auxiliary startup paths.

A logical native read means a lookup, cursor positioning/advance, database-open probe, statistics read or native metadata read initiated on the foreground construction/initialization thread. Counting these must not hide whole-tree deletion, checksum streaming, or large snapshot decoding. Background work must not run before the readiness measurement gate opens. A reference-count proof is a fixed-size record establishing that a supported writer maintained exact owner dependencies through the specified values transaction; its checksum protects the record itself, not every count payload against arbitrary storage corruption.

An index generation is the committed immutable set of query indexes and its selection table. Staging indexes are incomplete secondary permutations excluded from query selection. Dual writing means applying each real source mutation to registered staging indexes in the same native transaction. A build descriptor records the active/target layout, state, copied continuation key and native transaction authority. It must fit a bounded metadata record.

Preserve unrelated modifications to `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`, existing `.agent/plans/*`, and `scripts/ci/test_pr_verify_partitions.py`. Do not broadly stage, restore, clean or delete files. All new Java files use the exact project copyright/SPDX header for 2026 and `// Some portions generated by Codex`; retain existing header years.

## Plan of Work


### Milestone 1: Trust exact component counts after a process crash

Add a fixed versioned proof codec, its validation and tests. The proof key is `refcounts-exact` in `gc_meta`; fields include schema and count-algorithm versions, generation identity, exact values native transaction ID, allocation high-water mark and checksum. Before any production change, add the smallest real in-repository regression demonstrating that a crash after a committed value transaction currently forces reconstruction or loses authority. Run it with `mvnf`, append compact initial evidence, and retain its actual report.

Integrate proof publication after `persistNextId` and `updateRefCounts` in every live trusted writer path, including checkpoints, metadata-only startup changes, validated bulk completion and completed reconstruction. The proof and the owner/count changes must commit atomically. Metadata-only proof advancement requires a matching owned predecessor; an external writer cannot gain authority accidentally. A matched existing CLEAN marker can seed the new proof inside the checked startup writer. Historical DIRTY state, partial recovery, unsupported markers and raw imports cannot seed it.

For unknown authority, preserve mappings and queues and make ordinary writes independent of old count payloads. `gcIds` records candidates without deleting owners; unused/retired drains and free-ID reuse remain disabled. Keep native writer ownership checks independent of count trust. Allocation uses a trusted persisted counter/proof high-water mark or mathematically safe bounded legacy extrema; missing safe authority fails quickly rather than scanning or guessing. Add explicit directory-locked verification and reconstruction entry points; reconstruction leaves durable UNKNOWN state through every partial batch and publishes exact authority only after the whole owner graph is complete.

Acceptance is deterministic: crash cuts before and after native commits, dictionary-before-triple commits, checkpoints, GC and bulk/partial reconstruction preserve acknowledged data and shared dependencies. Matching proof reopens without reconstruction; missing/stale/CRC/schema/external-writer proof reopens with safe RW, retains IDs and stays unknown through writes and close.

### Milestone 2: Bound admission and auxiliary initialization

Change `LegacyStoreDiscovery.readDatabaseCatalog` for every admitted format to seek the printable ASCII name interval `[0x20,0x7f)` and stop at its upper bound. Apply a finite schema/catalog budget, including registered staging names. Remove startup deep dictionary, context, statement, term and retirement-queue traversals. Preserve deep validators behind explicit offline verification and move tests asserting deep corruption rejection to that operation without weakening their semantic assertions. Preserve bounded rejection of unsafe format fences, generation mismatches, flags and authoritative manifest structure before writable setup. Share the bounded inspection result between discovery, prepared admission and writable initialization rather than recapturing full state.

Owner startup must not drain queues or drop inactive trees. Untrusted count mode retains all backlogs. Trusted post-ready maintenance may use bounded liveness probes only; if a component lacks a suitable leading index, retain its candidate for maintenance instead of falling back to a whole statement scan. Ordinary first writes must not unexpectedly drain the pre-existing backlog.

Start optional hash caches empty or unavailable without reading old payloads. Defer large filter/sketch sidecars with safe heuristic fallback, and preserve cache corruption safety. Namespace data cannot be reset: make authoritative snapshot decoding/import lazy, ensure reads see the committed snapshot, and force the correct snapshot before namespace-dependent mutation/publication. Background adjacency/value-overlay work starts only after readiness and must not block it.

Acceptance includes clean/current, crash, legacy, unknown proof, malformed metadata, maximum physical index sets and large queues. Guard actual native reads and forbidden whole-tree/snapshot operations, not elapsed time alone.

### Milestone 3: Rebuild layouts while committed indexes serve

Separate query index generations from active-plus-staging write targets in `TripleStore` and `ValueStore`. Canonical names bound the physical schema: 24 statement permutations with explicit/inferred databases plus contexts and namespaces fit discovery's 64 DB limit; 24 possible term permutations plus metadata fit the values limit. Store a compact descriptor in the existing metadata database; no unbounded generation-name growth.

Background-reset obsolete staging trees in bounded cursor-delete transactions. Atomically register dual writes, then keyset-copy from a committed source using short read transactions. Close the read view before resizing or writing. The stage writer rechecks each copied tuple against the current canonical source and applies only current membership, committing its continuation key with the stage updates. Fix the existing `TripleStore.copyReindexBatch` continuation bug: after `MDB_SET_RANGE`, skip the result only if its key equals the previous key. Inserts behind the continuation already reach staging through dual writes; deletions cannot be resurrected by stale copies.

Cover scalar, aligned/prepared, native chunk, explicit/inferred promotion, delete, clear, rollback, capacity replay and bulk mutation paths through mandatory transactional maintenance. Optional adjacency collectors and temporary `TxnMutationJournal` are not durable correctness authority. Term-stage copies do not create owners or increment component counts. Prepared write plans carry a generation fence and are revalidated/re-encoded when write targets change.

At source EOF, verify generation and source/stage counts in the publishing writer, commit the new manifest, and publish the immutable query generation under existing reader/transaction barriers. Retain removed handles for old snapshots. Do not drop trees in the publication transaction; clean them with bounded background deletion after readers retire. Resume matching descriptor/native authority after a crash; reset an invalid build in the background while committed indexes serve. Coalesce repeated requests to one job per environment. Expose status and an await API; failed builds leave active indexes usable.

Acceptance exercises deleted continuation keys, delete-between-copy phases, inserts behind progress, promotion, rollback, clear, MAP_FULL replay, prepared/native batches, restart at every publication boundary, coalescing, old readers, maximum layouts and nested term GC/count independence.

### Milestone 4: Integrate observability and acceptance

Expose startup/count trust and index-maintenance status from `LmdbStore` with an await API matching existing lifecycle conventions. Instrument foreground logical reads by category with a measurement scope whose lifetime covers construction through repository readiness, including database-open/stat/metadata reads. Keep diagnostics observational; exceeding test/acceptance limits must identify the operation and phase rather than silently bypassing validation.

Add a reproducible runner and documentation under `scripts/` and appropriate LMDB test/benchmark sources. It accepts an existing supplied store/copy and configuration, reports constructor-to-ready milliseconds and each read category, exact count authority, pending index maintenance, and first-query time separately. It must never build a billion-statement dataset. The user runs the provided command against their billion-statement store. Local theme-fixture results must name the actual 14.6M statement scale.

### Milestone 5: Verify and deliver

Run focused subsystem tests, the existing compatibility/startup/reference-count/GC/index suites, then full LMDB verification. Compare failures with the preserved pre-plan run; investigate change-related failures and report unresolved limits precisely. Run copyright/header audit, required repository formatter, then a final relevant verification on formatted sources. Audit the full dirty status and scoped diff; retain all evidence and unrelated work. Update this plan's outcomes to distinguish terminal results from running or failed checks.

## Concrete Steps


Read relevant skills first: `.codex/skills/high-performance-java/SKILL.md` and `.codex/skills/mvnf/SKILL.md`. Check status before edits. The initial required root clean install already passed and is retained at `logs/legacy-catalog-initial-build.log`. The old module run has now terminated; preserve its reports before any new `mvnf` cleanup.

For each behavior unit, add focused tests and run:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs

Capture the actual red report before production edits, append (never overwrite) `initial-evidence.txt`, and copy scoped evidence under `logs/`. Use:

    python3 scripts/agent-evidence.py --command '<exact command>' --log logs/mvnf/<run>-verify.log core/sail/lmdb/target/surefire-reports core/sail/lmdb/target/failsafe-reports

Then implement, rerun the same selector, and broaden to class/subsystem selections. All Maven commands use `.m2_repo`; offline is default. Never run tests with `-am` or `-q`. `mvnf` performs prerequisite root quick installs and writes install output to `maven-build.log`. Only one Maven/formatter operation may run against this shared checkout at a time. On missing offline artifacts retry the same command online once, then return offline. Root build failures unrelated to resolution retry without parallel reactor before other recovery.

Before final formatting run the copyright audit from `scripts/`, retaining its output. Then run:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    git diff --check
    git status --short

The formatter command is a non-test lifecycle operation; its `-q` does not override the prohibition on quiet test runs. Preserve unexpected concurrent edits rather than reverting them.

## Validation and Acceptance


Every production behavior change has an observed smallest-scope in-repository failing automated test before its fix. Save red and matching green summaries in evidence artifacts. A normal process crash with matching exact-count proof must reopen without owner reconstruction and support safe writes. Missing or mismatched proof must preserve statements, namespaces, IDs and nested dependencies while staying untrusted, retaining queues and avoiding reuse. Explicit reconstruction must restore trust only after successful complete validation, leaving unknown authority on injected failures.

Startup tests impose at most 1,000 foreground logical native reads over construction through ready, for clean, crashed, legacy, untrusted, large-backlog and maximum-layout stores. Tests prohibit data-size-dependent foreground cursor walks, whole-tree drops, old hash-cache checksum streaming and eager optimizer/namespace decoding. Record-access corruption and explicit offline validation retain their correctness assertions.

Concurrent reindex tests prove results and writes remain correct on old committed indexes through building, interruption and failure, then the new layout becomes visible atomically. Holding an old snapshot must remain safe through publication and cleanup. Stage copies must not alter component counts. A background failure is visible in status and does not invalidate the active layout.

The acceptance runner reports real local cardinality, constructor-to-ready milliseconds, per-category reads and total, count trust and pending work, with first query measured separately. The user alone supplies the billion-statement timing result; no local result is labeled as that measurement.

## Idempotence and Recovery


Never delete unknown or untracked work. Retain initial reports because `mvnf` clears stale report directories. New proof publication is atomic with its data and cannot turn unknown counts exact merely by reopening or closing. Partial explicit reconstruction and raw imports remain unknown. Staging descriptors allow bounded startup and background resumption/reset; cleanup never removes active query authority. A failed stage, copy, cutover or map-growth attempt leaves the last committed generation serving data. Background tasks stop cleanly on store close and do not publish into a new environment generation.

## Artifacts and Notes


Initial catalog red: `logs/legacy-catalog-initial-red.txt`, command `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbLegacyIndexDiscoveryTest#catalogScanSeeksPastLegacyDictionaryRecords --retain-logs`, log `logs/mvnf/20261023-130133-verify.log`. Summary: one test, one failure, no errors/skips; `expected: 17 but was: 0`.

Earlier matching green: `logs/legacy-catalog-focused-green.txt`, log `logs/mvnf/20261023-130500-verify.log`. Summary: one test, zero failures/errors/skips. Earlier class green: `logs/legacy-catalog-class-evidence.txt`, log `logs/mvnf/20261023-130649-verify.log`; 77 tests, zero failures/errors/skips.

Pre-plan module evidence: `logs/bounded-startup-preplan-evidence.txt`, copied reports `logs/bounded-startup-preplan-surefire-reports/`, full log `logs/mvnf/20261023-130939-verify.log`. This run failed and must not be described as full green.

Atomic-count red reports: `logs/value-count-authority-red/` and `logs/value-count-legacy-red/`. The first selection observed reconstruction after a committed dictionary crash and after untrusted external count changes; its third case initially had an incomplete fixture descriptor. After correcting that test-only setup, the legacy marker method independently failed with `expected: <null> but was: <[1]>`. Full logs are `logs/mvnf/20261023-140850-verify.log` and `logs/mvnf/20261023-141036-verify.log`.

Statement background red: `logs/bounded-startup-statement-red-evidence.txt`, reports `logs/bounded-startup-statement-red-reports/`, full log `logs/mvnf/20261023-141229-verify.log`. One test failed because construction immediately changed the native layout from `[spoc]` to `[posc]`.

Coordinator startup red: `logs/bounded-startup-coordinator-red-evidence.txt`, reports `logs/bounded-startup-coordinator-red-reports/`, full log `logs/mvnf/20261023-141907-verify.log`. Five tests produced four failures and one error. Current discovery performed 3,308 database opens, 4,435 cursor gets, nine gets, fifteen flag reads and three transaction-ID reads (7,770 logical reads). Namespace construction eagerly read its snapshot; startup invoked value reclamation; a clean hash cache reopened with old bytes. The legacy populated variant exposed a provisional allocation-tag incompatibility in the count-worker patch and is assigned back to that worker; it is not counted as the desired budget assertion failure.

## Interfaces and Dependencies


Use existing LWJGL LMDB, JDK concurrency, repository transaction/read barriers and in-repository test dependencies; add no new dependency without demonstrated need. Keep proof encoding/validation separate from whole-store CLEAN handling. Expose count trust and pending verification, startup measurement and background index status through stable types on `LmdbStore`; delegate implementation to its owner and native stores. Define internal status and lifecycle interfaces consistently between statement and term reindex implementations, preserving a single metadata authority per environment.

The coordinator owns integration, this ExecPlan, discovery/admission, namespace/auxiliary initialization, public lifecycle/status/acceptance APIs and final evidence. Dedicated workers own ValueStore/count-proof/term maintenance and TripleStore/statement reindex respectively. Workers must request the serialized Maven slot before running installs/tests; no formatter runs until all edits are quiescent. Changes to shared files require an explicit ownership handoff. Workers report red evidence, completed subsystem milestones or actionable blockers, not unchanged polling status.

Revision note (2026-10-23): Created the full implementation ExecPlan after explicit user approval. It replaces the earlier legacy-only catalog optimization as the task's complete scope and preserves all approved safety, crash, background-layout and acceptance policies.

Revision note (2026-10-23): Corrected the governing plan reference to root `PLANS.md` and assigned two complex native-store workers while the coordinator owns admission, auxiliaries, public lifecycle and verification serialization. Untrusted legacy owner mutations must remove a historical completion marker before skipping count deltas, preserving safe downgrade behavior.

Revision note (2026-10-23): Paused all startup implementation at the user's explicit request for a separate test-only predicate-enumeration commit/push. Preserved worker checkpoints, active session identity and partial integrated evidence; startup must not resume before that publication is verified.

Revision note (2026-10-23): Verified the scoped test-only publication and resumed startup implementation on baseline `78ab8734ac1d57915fa4a9ead495ba845af7f204`. The two observed startup defects to fix first are legacy read-only marker over-invalidation and resumed statement staging DBI handles opened in an aborted read transaction. Explicit read-only verification covers authoritative dictionary/index/queue/schema records; reference-count reconstruction separately checks the complete owner graph and publishes exact-count authority. Do not claim read-only count-table equality verification.
