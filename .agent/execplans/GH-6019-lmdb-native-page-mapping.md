# Reuse LMDB mapping in the page estimator

This ExecPlan follows `.agent/PLANS.md`.

Completion status (2026-09-18 18:25 CEST): the parent reviewed the final production changes, regression coverage, and benchmark setup. Independent aggregation of the final JDK 25.0.1 reports confirmed 1,252 tests, 102 skipped, and zero failures/errors; the required JDK 25 offline root clean install also passed. All planned local implementation and validation work is complete. Linux and Windows execution remains unrun for this follow-up; the existing CI matrix covers both. Publication is authorized for the reviewed changes; commit, push, and the develop comparison remain operational steps in this handoff and are not preclaimed here.

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
- [x] Review portability evidence and final changes.
- [x] (2026-09-17 16:00 Europe/Oslo) Inventory current focused tests, benchmark wrappers, and CI matrix without changing the worktree.
- [x] (2026-09-17 21:35 Europe/Oslo) Root clean install and five focused baseline selectors passed: 42 tests, zero failures/errors/skips; prior root evidence archived.
- [x] Add generation-aware managed snapshot reuse and resize helper.
- [x] Add real-LMDB cache, ownership, mismatch, and recovery regressions.
- [x] Format, verify focused tests, module suite, and benchmark.
- [x] (2026-09-17 22:29 CEST) Captured the zero-transaction native-map red: the old guard order called `mdb_txn_id(0)` and produced `NullPointerException` instead of `IOException`.
- [x] (2026-09-17 22:30 CEST) Captured the file-only zero-transaction red for the same premature native lookup; repaired both fallback and native guard paths.
- [x] (2026-09-17 22:34 CEST) Re-ran the repaired native-map class: 9 tests, zero failures/errors/skips.
- [x] (2026-09-17 22:34 CEST) Re-ran the stale pinned snapshot regression after three unobserved commits: 1 test, zero failures/errors/skips.
- [x] (2026-09-17 22:43 CEST) Re-ran the lifecycle matrix: 22 tests, zero failures/errors/skips, including default and `WRITEMAP` remaps and managed independent threads.
- [x] (2026-09-17 22:48 CEST) Re-ran the full LMDB module: 1,250 tests with 102 skipped, zero failures/errors.
- [x] (2026-09-17 22:42 CEST) Measured the two-fork JMH wrapper on JDK 26 with three warmups, five measurements, and `-prof gc`; raw output is archived under `profiles/GH-6019/generation-cache-repair-20260917-2224/benchmark-output.log`.
- [x] (2026-09-18 00:25 CEST) Removed the unused cached-map anchor validation argument and parameterized concurrent and active-close probes over both read APIs.
- [x] (2026-09-18 00:25 CEST) Final macOS JDK 25 mapped-pages selection passed 55 tests with zero failures/errors/skips; reports are archived under `profiles/GH-6019/final-jdk25-20260917/`.
- [x] (2026-09-18 00:25 CEST) Final macOS JDK 25 LMDB module passed 1,252 tests with 102 skipped and zero failures/errors; the 1,150 executed tests passed.
- [x] (2026-09-18 00:29 CEST) Complete parent review and evidence handoff.

## Surprises & Discoveries

`mdb_env_info` returns the persisted fixed-map address, which is zero for ordinary RDF4J stores. LMDB has no public getter for the live mapping base. `mdb_cursor_get(MDB_FIRST)` returns a mapped key pointer belonging to the pinned transaction, including when values use duplicate or overflow pages. The existing decoder can locate that same first key's file offset. Subtracting this verified offset recovers the existing mapping without another mmap or private MDB_env structure offsets.

## Decision Log

- Decision: Use LMDB's existing mapping and remove the separate JDK mapping implementation.
  Rationale: The user's portability and allocation constraints require zero-copy reads from the mapping LMDB already owns; the superseded implementation and reports remain under `profiles/GH-6019/superseded-separate-mapping` as history.
  Date/Author: 2026-09-09 / historical design decision.

- Decision: Pass the already pinned read-only transaction through a scoped estimator reader while retaining transaction-ID-only positional reads.
  Rationale: A borrowed scope can validate LMDB's public cursor pointer without opening or closing another transaction. The caller keeps the transaction pinned and excludes resize/close until the scope closes.
  Date/Author: 2026-09-09 / historical design decision.

- Decision: Add `readTransaction(long readTxn, long mappingGeneration)` for managed scopes, and retain the one-argument overload as the conservative path.
  Rationale: Cache entries are keyed by transaction ID, generation, borrowed versus file-backed mode, and managed versus conservative mode. The environment is validated on every borrowed scope, so equal transaction IDs from distinct environments cannot reuse a snapshot merely because their IDs match. The generation is supplied by `TripleStore` while its existing priority read lock is held; the resize helper adds no lock.
  Date/Author: 2026-09-17 / Codex.

- Decision: Centralize all five `mdb_env_set_mapsize` calls in `TripleStore` behind one private helper with a monotonic, non-wrapping generation.
  Rationale: Incrementing before every native attempt, including failures and same-size attempts, invalidates managed page caches before a mapping can change while preserving each caller's existing return and error handling. Initialization remains serialized and runtime resize remains under its existing write-lock/deactivation protocol.
  Date/Author: 2026-09-17 / Codex.

- Decision: Keep `ReadView` locking, close waiting, creating-thread ownership, nested scopes, and failure-release behavior unchanged.
  Rationale: Every borrowed estimator count/estimate path uses a try-with-resources `ReadView`; failed discovery publishes no cache entry. The main DBI continues through `openDatabaseWithTxn(txn, null, 0)`, `headerBuffer.remove()` remains, and the scratch-buffer comment describes the calling thread.
  Date/Author: 2026-09-17 / Codex.

- Decision: Reuse matching conservative and file-backed snapshot state while preserving per-scope native verification.
  Rationale: Conservative borrowed scopes must call `withNativeMap` each time, but metadata, heap anchors, page caches, and descriptor caches remain valid when the transaction/mode key matches and the native address/size identity is unchanged. File-backed matches return directly without rereading metadata. A cache-key mismatch is local; failed discovery must not evict another reader's valid snapshot.
  Date/Author: 2026-09-17 / Codex.

- Decision: Let a managed cache hit validate only the live environment and transaction identity.
  Rationale: Discovery already checks mapping bounds, and the exact transaction ID plus unchanged generation certify those immutable bounds. Repeating `mdb_env_info` and cursor/bounds checks on every managed hit would rebuild validation work without adding safety under the documented caller exclusion.
  Date/Author: 2026-09-17 / Codex.

## Plan of Work (historical execution design)

The following plan text records the completed design and execution history; the current checklist above is authoritative for remaining work.

First strengthen the existing four-case native activation regression to assert that the page's value address equals the address returned by mdb_get. Observe and retain this failure against the separate mapping implementation before changing production again.

Replace the owned mapping with a cursor anchor from the same pinned transaction and exact metadata. Validate the first key against the file decoder before computing the base; bound access by both committed pages and current LMDB map size. Re-resolve the anchor for each borrowed transaction scope before any cached native page is used, covering remapping even without a transaction-ID change. Keep concurrent readers safe and never store native views in legacy file-only snapshots.

Adapt tests for ordinary TLS and NOTLS environments, read-only reopen, NOSUBDIR, WRITEMAP, duplicate and overflow layouts, map growth/remapping, overlapping snapshots, close/concurrency, empty databases, fallback and pointer identity. Keep real LMDB tests platform-neutral and run them on available macOS and Linux runtimes. Add Windows/macOS/Linux CI coverage. Preserve all reports and unrelated working-tree artifacts.

The current implementation keeps the existing native pointer verification and adds a generation-aware managed path. In `LmdbPageCardinalityEstimator`, the public two-argument `readTransaction` overload uses managed cache reuse and the one-argument overload uses conservative per-scope native verification. Each borrowed entry computes `mdb_txn_id` once and passes it into snapshot discovery. Cache entries distinguish the transaction ID, mapping generation, borrowed versus file-backed mode, and managed versus conservative discovery; borrowed hits validate the live environment and exact transaction metadata, while discovery performs all mapping-bound checks. An environment or transaction mismatch, stale generation, or failed validation prevents stale pages from being used, and only successfully verified snapshots are published. The existing read-lock release on acquisition failure remains required.

In `TripleStore`, add a monotonic, non-wrapping mapping generation and one private helper around every `mdb_env_set_mapsize` call: the two initialization calls near lines 315/322, reindex near 387, update-from-cache near 1557, and end-transaction near 1620. Increment before each native call even if the call returns an error, then preserve the caller-specific return/error path. Pass the generation into the estimator scope while the existing `doWithPriority` read lock is held. Do not introduce a lock in the helper, because initialization and runtime resize have different established lock ordering.

Extend the real-LMDB regression matrix to exercise warmed managed reuse, unchanged-transaction growth, same-size remapping through both APIs, equal transaction IDs from another environment, switching between managed/conservative/file-backed reads, concurrent snapshots and commits without resizing, nested scopes, wrong-thread access, exception-driven release, discovery failure followed by a valid read, cached-write auto-growth, and the existing close guarantees. Use only public LMDB APIs and observable behavior; do not use reflection, private state, sleeps, OS-specific mmap enumeration, a second mapping, or weakened assertions.

For measurement, use the existing `scripts/run-single-benchmark.sh` wrapper and a real LMDB workload with `Scope.Thread` transaction ownership. Verify that both paths return the same count and that native mapping is active during setup. Compare warmed conservative and managed scopes with at least three one-second warmups, five one-second measurements, and two forks; enable `-prof gc` to expose allocation differences. If a pre-change baseline cannot be captured without overwriting existing artifacts, retain the superseded output and label any old-API versus managed result as discovery-savings evidence rather than a promised speedup.

## Concrete Steps (historical execution record)

The commands below document the intended and completed workflow; the current JDK 25 evidence is listed in the final update below.

Run the mvnf runner from the repository root, with --retain-logs and the required workspace-local .m2_repo. Start with LmdbDataFileNativeMapTest#environmentConstructorActivatesNativeMap, then focused mapping classes, then core/sail/lmdb. Never use -am or -q with tests enabled. Run copyright checks before the repository formatter.

Before any test selection, run the repository-required root clean install with JDK 25 or newer and keep the complete output in `maven-build.log`. Archive any existing root `initial-evidence.txt` and report directories before writing fresh compact evidence. The baseline selectors are `LmdbPageMappingLifecycleTest`, `LmdbDataFileNativeMapTest`, `LmdbMappingAnchorTest`, `LmdbPageCardinalityEstimatorDupSortNativeTest`, and `TripleStoreAutoGrowTest#testPageEstimatorAcrossAutoGrowth`, each through `python3 .codex/skills/mvnf/scripts/mvnf.py ... --module core/sail/lmdb --retain-logs`. After implementation, rerun the identical selectors, then the LMDB module with integration tests as appropriate, and retain all logs. The cache-repair red selectors and their reports live under `profiles/GH-6019/generation-cache-repair-20260917-2224`; the native-map class now passes nine tests and the stale pinned snapshot regression passes one test.

For the benchmark, run from the repository root: `./scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.estimate.LmdbPageMappingBenchmark --method '.*Scope' --warmup-iterations 3 --measurement-iterations 5 --forks 2 --jmh-arg -prof --jmh-arg gc`. The wrapper builds the benchmark jar before launching JMH; retain its complete output and report the score and allocation intervals for both methods. The local sandbox may reject JMH's fork socket, in which case rerun the same command with the approved external execution permission.

## Validation and Acceptance

The original activation assertion and the new pointer identity assertion must pass using LMDB's mapping. No FileChannel.map or OS-specific map enumeration remains in production. Correct counts must survive commit, reopen and LMDB-controlled resize. Legacy file-only access continues working. Preserve pre-fix and post-fix Surefire reports in profiles/GH-6019 and initial-evidence.txt. Report actual platform runs separately from unrun CI jobs.

## Idempotence and Recovery

Issue https://github.com/eclipse-rdf4j/rdf4j/issues/6019 and branch GH-6019-lmdb-native-page-mapping already exist. Do not recreate them or discard unrelated changes. Do not commit or push without user instruction. Archive superseded work before replacing it.

## Artifacts and Notes

Original red: profiles/GH-6019/red, four failures. Superseded implementation: profiles/GH-6019/superseded-separate-mapping. Cache-repair red and green evidence is under `profiles/GH-6019/generation-cache-repair-20260917-2224/`, including `red-stale-old-snapshot-evidence.txt`, `red-zero-handle-evidence.txt`, `red-zero-handle-file-fallback-evidence.txt`, `post-stale-old-snapshot-evidence.txt`, `focused-native-map-post-evidence.txt`, `full-module-evidence.txt`, and `benchmark-output.log`. The full module summary is `tests=1250, failures=0, errors=0, skipped=102`; report wording must keep the 102 skipped separate from the 1,148 executed tests.

The corrected `post-stale-old-snapshot-evidence.txt` reports the one archived test run; the earlier duplicate-count summary is preserved as `post-stale-old-snapshot-evidence-superseded-duplicate-summary.txt`. Final macOS JDK 25 reports and compact evidence are under `profiles/GH-6019/final-jdk25-20260917/focused-mapped-pages-evidence.txt` (`tests=55, failures=0, errors=0, skipped=0`) and `profiles/GH-6019/final-jdk25-20260917/full-lmdb-evidence.txt` (`tests=1252, failures=0, errors=0, skipped=102`). These final evidence directories are kept separate from historical reports.

## Interfaces and Dependencies

Use existing LWJGL LMDB cursor and transaction APIs and existing LMDB page-format decoding. No new dependencies or changes to persisted data, flags, or public configuration. Add an explicitly scoped API for borrowing a pinned transaction; retain legacy transaction-ID methods.

## Outcomes & Retrospective

The entries below are retained historical execution notes. The final update at the end records the current verification state.

Implemented on GH-6019-lmdb-native-page-mapping. The fix borrows LMDB mapping pointers, validates their page identity, and passes pinned transaction scopes from TripleStore. Full LMDB suites passed on macOS JDK 25 and JDK 26 (1,215 tests each, zero failures/errors, 102 skipped). Linux JDK 25 passed 33 focused tests. Windows CI coverage is added but unrun. The initial JDK 26 parallel-sort stall, isolated passing checks, and successful full retry are documented separately. No commit or push has been made.

Current repair outcome 2026-09-17: matching conservative scopes now retain metadata, anchors, page caches, and descriptors while rechecking the native address and size; file-backed matches avoid metadata rereads; managed hits validate the environment and transaction against the bounds certified at discovery. Zero-transaction native and file-only paths pass their focused regressions after the guard-order repair. The local macOS JDK 26 full module run passed 1,250 tests with 102 skipped and zero failures/errors. The repaired two-fork benchmark measured conservative scopes at 69.108 +/- 0.740 ns/op and 72.004 +/- 0.010 B/op, managed scopes at 10.465 +/- 0.242 ns/op and 0.001 +/- 0.002 B/op; these are workload-specific observations, not a general speedup claim.

Review note: 32 focused cases pass. Add a stale-offset rejection regression before any computed mapping base can be used, since identical first-key bytes do not prove the page number is still the same.

Full-suite note: JDK 26 stalled after 744 tests in QueryBenchmarkTest.ordered_union_limit, inside Arrays.parallelSort, with no common-pool workers in the dump. Reports and diagnostic stacks are retained in profiles/GH-6019/module-jdk26-stall. The mapped-pointer focused matrix passed 33 tests on both macOS JDK 26 and Linux JDK 25. Investigate the isolated test and rerun the full suite with JDK 25; do not claim the stalled run passed.

Validation update: macOS ARM64 JDK 25.0.1 full module passed 1,215 tests with zero failures/errors and 102 skips; QueryBenchmarkTest passed all 13 tests. Linux ARM64 JDK 25.0.1 passed all 33 focused cases. The exact changed Java files match the Linux source archive. A standalone original-HEAD QueryBenchmarkTest.ordered_union_limit probe passed on JDK 26; the same focused Surefire check with the fix is pending.

Runtime isolation: the isolated QueryBenchmarkTest#ordered_union_limit passed in Surefire on JDK 26 (one test, zero failures/errors, 12.943 seconds). Original-HEAD standalone query probe also passed. Repeat the unmodified JDK 26 full selector once; retain the first stalled run separately regardless of the outcome.

Final verification: the unchanged JDK 26 full retry passed. All initial reds and final greens are archived under profiles/GH-6019. The supported-platform CI matrix is ready for a future PR. Final handoff is the only active plan step.

Follow-up scope opened 2026-09-17: parent review identified the need to distinguish conservative versus managed cache generations and to characterize ownership, mismatch, and release behavior before final handoff. The earlier implementation and its historical evidence remain preserved; this plan now tracks the additive generation-aware cache work and its fresh baseline/post-change evidence.

Plan revision 2026-09-17: replaced the former final-handoff active step with the authorized generation-aware implementation sequence, recorded the cache-key and resize-generation decisions, and added the required regression and benchmark acceptance criteria. Existing historical evidence and platform notes were retained.

Implementation update 2026-09-17: added the managed `ReadView` overload, generation and mode-aware snapshot cache, borrowed-environment validation on managed hits, conservative rediscovery, and scoped transaction-ID-only reads. Centralized all five map-size calls behind a non-wrapping pre-increment helper and passed the generation under `doWithPriority`'s existing read lock. Added six real-LMDB lifecycle regressions and the `LmdbPageMappingBenchmark` wrapper target. Focused post-change selectors passed 48 tests with zero failures/errors/skips; the full LMDB module passed 1,245 tests with zero failures/errors and 102 skips. The supported benchmark completed on JDK 26; its wide three-iteration confidence intervals are retained under `profiles/GH-6019/post-generation-cache-20260917-2159/benchmark/`.

Final follow-up update 2026-09-17: added a public-API regression for an aligned write that naturally falls back to the record cache, grows the LMDB map during cached replay, and then uses the page estimator for the committed count. Its focused selector passed; the complete LMDB module then passed 1,246 tests with zero failures/errors and 102 skips. The final module reports are retained under `profiles/GH-6019/post-generation-cache-20260917-2159/full-module-final/`. The existing Ubuntu/macOS/Windows Temurin 25 mapped-pages job now selects this regression as well.

Repair update 2026-09-17: the smallest conservative cache regression failed against the intermediate implementation after three unobserved commits because it discarded the matching snapshot and reread metadata for an old pinned transaction. The native and file-only zero-transaction regressions also failed before the guard because `mdb_txn_id(0)` raised `NullPointerException`. Both reds are archived under `profiles/GH-6019/generation-cache-repair-20260917-2224`; guard order and cache reuse were repaired, and the focused post-change native-map and stale-snapshot selectors pass.

Repair design update 2026-09-17: matching conservative snapshots now reuse their verified metadata, heap anchor, page cache, and named-database descriptors after `withNativeMap` confirms the current address and size; changed mapping identity creates a new cache. Matching file-backed snapshots return directly. Managed hits validate the environment and exact transaction metadata only because generation and transaction identity preserve the discovered bounds. Cache-key mismatches leave the shared previous cache intact until a successfully discovered replacement is published.

Coverage update 2026-09-17: lifecycle tests now exercise managed scopes on independent threads, default and `WRITEMAP` remaps through both managed and conservative APIs, native page estimates, separate warmed caches, file-mode separation, and managed close rejection. The cached-write growth regression warms a committed estimate before the aligned cached flush and asserts the combined count. The benchmark setup verifies active native mapping and both returned counts; its source uses two forks, three one-second warmups, and five one-second measurements.

Final verification update 2026-09-18: removed the unused `MappingAnchor` parameter from cached native validation and parameterized the concurrent and active close probes over conservative and managed scopes. On macOS ARM64 with Temurin 25.0.1, the mapped-pages CI selector passed 55 tests with zero failures/errors/skips, and the full `core/sail/lmdb` module passed 1,252 tests with 102 skipped (1,150 executed and passing) and zero failures/errors. Final reports and retained logs are under `profiles/GH-6019/final-jdk25-20260917/`; no additional benchmark run was needed for this test-only cleanup. The only remaining active plan step is this parent-review handoff.
