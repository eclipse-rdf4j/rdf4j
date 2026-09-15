# Reuse LMDB mapping in the page estimator

This ExecPlan follows `.agent/PLANS.md`.

## Purpose / Big Picture

Fix issue #6019 by reusing the mapping LMDB already owns. The user explicitly rejects creating another mmap. Preserve file-I/O fallback and existing public estimator methods while making production estimates use the caller's pinned read-only transaction.

## Progress

- [x] Create issue and its working branch.
- [x] Preserve original four-case failing regression evidence.
- [x] Reproduce borrowed mapping identity requirement first.
- [x] Implement cursor-based discovery and transaction lifetime.
- [x] Reproduce stale-offset rejection before final validation.
- [x] Validate complete module on minimum JDK.
- [x] Complete full JDK 25/26 runtime gates.
- [ ] [in_progress] Hand off verified implementation and evidence.
- [x] Review portability evidence and final changes.

## Surprises & Discoveries

`mdb_env_info` returns the persisted fixed-map address, which is zero for ordinary RDF4J stores. LMDB has no public getter for the live mapping base. `mdb_cursor_get(MDB_FIRST)` returns a mapped key pointer belonging to the pinned transaction, including when values use duplicate or overflow pages. The existing decoder can locate that same first key's file offset. Subtracting this verified offset recovers the existing mapping without another mmap or private MDB_env structure offsets.

## Decision Log

2026-09-09: The user clarified that only LMDB's existing mmap should be used. The separate JDK mapping implementation and its successful test reports are archived under profiles/GH-6019/superseded-separate-mapping and do not validate the revised approach.

2026-09-09: Pass the existing read-only transaction from TripleStore through a scoped estimator reader. Keep transaction-ID-only methods compatible through positional reads. Validate mapping identity using the public cursor API before reusing cached native pages. A reader borrows the transaction and never ends it or maps/unmaps LMDB memory. The caller must keep the transaction pinned and exclude environment resizing/closure until the scoped reader closes, as TripleStore's transaction manager already does.

## Plan of Work

First strengthen the existing four-case native activation regression to assert that the page's value address equals the address returned by mdb_get. Observe and retain this failure against the separate mapping implementation before changing production again.

Replace the owned mapping with a cursor anchor from the same pinned transaction and exact metadata. Validate the first key against the file decoder before computing the base; bound access by both committed pages and current LMDB map size. Re-resolve the anchor for each borrowed transaction scope before any cached native page is used, covering remapping even without a transaction-ID change. Keep concurrent readers safe and never store native views in legacy file-only snapshots.

Adapt tests for ordinary TLS and NOTLS environments, read-only reopen, NOSUBDIR, WRITEMAP, duplicate and overflow layouts, map growth/remapping, overlapping snapshots, close/concurrency, empty databases, fallback and pointer identity. Keep real LMDB tests platform-neutral and run them on available macOS and Linux runtimes. Add Windows/macOS/Linux CI coverage. Preserve all reports and unrelated working-tree artifacts.

## Concrete Steps

Run the mvnf runner from the repository root, with --retain-logs and the required workspace-local .m2_repo. Start with LmdbDataFileNativeMapTest#environmentConstructorActivatesNativeMap, then focused mapping classes, then core/sail/lmdb. Never use -am or -q with tests enabled. Run copyright checks before the repository formatter.

## Validation and Acceptance

The original activation assertion and the new pointer identity assertion must pass using LMDB's mapping. No FileChannel.map or OS-specific map enumeration remains in production. Correct counts must survive commit, reopen and LMDB-controlled resize. Legacy file-only access continues working. Preserve pre-fix and post-fix Surefire reports in profiles/GH-6019 and initial-evidence.txt. Report actual platform runs separately from unrun CI jobs.

## Idempotence and Recovery

Issue https://github.com/eclipse-rdf4j/rdf4j/issues/6019 and branch GH-6019-lmdb-native-page-mapping already exist. Do not recreate them or discard unrelated changes. Do not commit or push without user instruction. Archive superseded work before replacing it.

## Artifacts and Notes

Original red: profiles/GH-6019/red, four failures. Superseded implementation: profiles/GH-6019/superseded-separate-mapping. Final evidence will be recorded separately.

## Interfaces and Dependencies

Use existing LWJGL LMDB cursor and transaction APIs and existing LMDB page-format decoding. No new dependencies or changes to persisted data, flags, or public configuration. Add an explicitly scoped API for borrowing a pinned transaction; retain legacy transaction-ID methods.

## Outcomes & Retrospective

Implemented on GH-6019-lmdb-native-page-mapping. The fix borrows LMDB mapping pointers, validates their page identity, and passes pinned transaction scopes from TripleStore. Full LMDB suites passed on macOS JDK 25 and JDK 26 (1,215 tests each, zero failures/errors, 102 skipped). Linux JDK 25 passed 33 focused tests. Windows CI coverage is added but unrun. The initial JDK 26 parallel-sort stall, isolated passing checks, and successful full retry are documented separately. No commit or push has been made.

Review note: 32 focused cases pass. Add a stale-offset rejection regression before any computed mapping base can be used, since identical first-key bytes do not prove the page number is still the same.

Full-suite note: JDK 26 stalled after 744 tests in QueryBenchmarkTest.ordered_union_limit, inside Arrays.parallelSort, with no common-pool workers in the dump. Reports and diagnostic stacks are retained in profiles/GH-6019/module-jdk26-stall. The mapped-pointer focused matrix passed 33 tests on both macOS JDK 26 and Linux JDK 25. Investigate the isolated test and rerun the full suite with JDK 25; do not claim the stalled run passed.

Validation update: macOS ARM64 JDK 25.0.1 full module passed 1,215 tests with zero failures/errors and 102 skips; QueryBenchmarkTest passed all 13 tests. Linux ARM64 JDK 25.0.1 passed all 33 focused cases. The exact changed Java files match the Linux source archive. A standalone original-HEAD QueryBenchmarkTest.ordered_union_limit probe passed on JDK 26; the same focused Surefire check with the fix is pending.

Runtime isolation: the isolated QueryBenchmarkTest#ordered_union_limit passed in Surefire on JDK 26 (one test, zero failures/errors, 12.943 seconds). Original-HEAD standalone query probe also passed. Repeat the unmodified JDK 26 full selector once; retain the first stalled run separately regardless of the outcome.

Final verification: the unchanged JDK 26 full retry passed. All initial reds and final greens are archived under profiles/GH-6019. The supported-platform CI matrix is ready for a future PR. Final handoff is the only active plan step.
