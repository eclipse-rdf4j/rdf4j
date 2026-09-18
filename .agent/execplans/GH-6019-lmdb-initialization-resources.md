# Repair LMDB initialization resources and fallback scratch storage

This ExecPlan is a living document and follows `.agent/PLANS.md`. It covers only approved findings 3, 6, and 7 on branch `GH-6019-lmdb-native-page-mapping`; it must not change the existing native mapping generation guard, public APIs, store format, estimator ownership, or unrelated CI jobs.

## Purpose / Big Picture

An LMDB store must finish the read transaction that discovers page metadata before changing the environment map. If the native resize then fails, construction must stop without touching a possibly invalid mapping and must release every resource already acquired while retaining the original exception. The fallback data-file header read must use short-lived `MemoryStack` storage so repeated worker reads do not retain one direct buffer per reader and thread. The mapped-page workflow must run the new regressions on macOS and Windows Java 25 while Linux remains covered by the broad pull-request verification job.

The result is observable through two new regression classes. The initialization class runs a bounded child JVM for an oversized map request on non-WRITEMAP 64-bit Linux/macOS and then reopens the directory normally; the fallback class measures JVM-managed direct-buffer counts before and after concurrent header reads and reader close. Both classes must fail against the current implementation and pass after the narrow production changes.

## Progress

- [x] (2026-09-18) Confirmed clean tracked tree and preserved untracked artifacts.
- [x] (2026-09-18) Read `AGENTS.md`, `.agent/PLANS.md`, and mvnf instructions.
- [x] (2026-09-18) Ran the mandated offline root install on Java 26 (Java 25+).
- [x] (2026-09-18) Implemented transaction-safe initialization and cleanup.
- [x] (2026-09-18) Replaced fallback ThreadLocal with MemoryStack storage.
- [x] (2026-09-18) Updated mapped-pages matrix and test selectors.
- [x] (2026-09-18) Ran focused green selectors and full LMDB.
- [x] (2026-09-18) Applied parent-requested regression refinements and reran final gates.
- [x] (2026-09-18) Completed parent review and final handoff.

## Surprises & Discoveries

The fallback child probe observed 7 direct buffers at baseline and 55 after two workers read 24 measured readers, proving the old ThreadLocal retention. The oversized initialization probe crashed both estimator modes in `liblwjgl_lmdb.dylib` with SIGSEGV before producing an IOException; `-XX:-CreateCoredumpOnCrash` suppressed full core files and two `hs_err` logs were retained under the task red profile. Evidence is in `profiles/GH-6019/initialization-resources-20260918/red/`.

After the fix, the two child/normal regression classes and the seven requested existing classes passed together: 85 tests, zero failures/errors/skips. The invalid-header direct-buffer assertion required prewarming the JVM's own MemoryStack pool before measuring; the corrected test passed alongside the 24-reader worker probe. Post-review refinements add native page-size/minimum-map assertions, loaded populated oversized probes, stack-frame restoration checks, and complete child cleanup/error propagation.

## Decision Log

- Decision: Use a separate initialization-failure cleanup path that attempts transaction, estimator, index, transaction-manager, and environment cleanup independently and suppresses each cleanup failure on the original throwable. Rationale: construction can fail before normal `close()` ownership is transferred, and one cleanup failure must not prevent releasing later resources. Date/Author: 2026-09-18 / Codex.
- Decision: Keep page size and emptiness discovery inside the existing read transaction, end that transaction, then perform map-size operations and obtain the final size from `mdb_env_info`. Rationale: LMDB map resizing cannot safely occur while the discovery transaction is active, and the native environment may clamp the requested size. Date/Author: 2026-09-18 / Codex.
- Decision: Test oversized initialization only in a bounded child JVM on non-WRITEMAP 64-bit Linux/macOS; Windows runs normal initialization cases only. Rationale: an invalid native map request can crash or hang the process, and Windows may extend a real file even without WRITEMAP. Date/Author: 2026-09-18 / Codex.
- Decision: Use method-scoped `MemoryStack` allocation for fallback page headers and leave the native mapped-page fast path unchanged. Rationale: the header buffer cannot escape `readLeafEntryCount`, while the native path has separate lifetime and pointer contracts. Date/Author: 2026-09-18 / Codex.

## Outcomes & Retrospective

Post-review implementation and verification are complete on macOS ARM64 with Temurin JDK 26; this host has no JDK 25 installation. The two refined regression classes passed 10 and 2 tests respectively. The focused LMDB selection passed 88 tests with zero failures/errors/skips. The final `core/sail/lmdb` mvnf verify passed 1,264 tests with zero failures/errors and 102 skips: Surefire reported 1,161 tests with 3 skips across 79 report files, and Failsafe reported 103 tests with 99 skips across 13 report files. Raw reports and logs are retained under `profiles/GH-6019/post-review-20260918/`; the earlier red/full artifacts remain under `profiles/GH-6019/initialization-resources-20260918/`. The oversized child probe remains explicitly limited to 64-bit Linux/macOS and was not run on Windows; the mapped-pages workflow uses Java 25 on macOS/Windows and broad Ubuntu coverage remains in `pr-verify`. No commits or pushes were made.

## Context and Orientation

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` owns the LMDB environment, transaction manager, indexes, page estimator, and Java `mapSize`/`pageSize` state. Its `initializePageAndMapSize` method now gathers metadata inside a read transaction, ends that transaction, and then performs map sizing and final native-size reporting. `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/estimate/LmdbDataFile.java` reads a page header either through the LMDB native mapping or through positional file I/O; only the fallback header scratch buffer was changed. `.github/workflows/lmdb-mapped-pages.yml` is the focused platform matrix, while `.github/workflows/pr-verify.yml` already supplies broad Ubuntu coverage.

The module is `core/sail/lmdb`. Use JDK 25 or newer, the repository-local Maven repository `.m2_repo`, and the `python3 .codex/skills/mvnf/scripts/mvnf.py` runner. Tests must not use `-am` or `-q`. Existing untracked `profiles/GH-6019` artifacts and the root `initial-evidence.txt` are historical evidence and must remain intact.

## Completed Work

The required offline root install ran on Java 26 (Java 25+), and its complete `maven-build.log` was retained. `TripleStoreInitializationTest` was added in package `org.eclipse.rdf4j.sail.lmdb` with normal empty/populated initialization, alignment/minimum-size, reopen/retained-data/auto-growth, and bounded oversized child-JVM scenarios. `LmdbDataFileHeaderBufferTest` was added in package `org.eclipse.rdf4j.sail.lmdb.estimate` with synthetic files, both byte orders, fixed worker threads, strongly retained readers, latches, and `BufferPoolMXBean` direct-buffer counts. The pre-fix red selectors, green selectors, and all raw reports were retained under task-specific `profiles/GH-6019` directories while the original evidence was preserved.

After both red tests were recorded, `TripleStore.initializePageAndMapSize` was changed so the read transaction only gathers checked `mdb_stat` metadata, then closes before checked `setMapSize`/`mdb_env_info` calls. Zero/aligned requests, native minimum clamping, and empty/populated behavior were preserved; Java `mapSize` now comes from the successful final native info. Constructor-failure cleanup releases already-created resources while suppressing cleanup exceptions on the primary failure. Only the fallback `LmdbDataFile` header buffer changed to `MemoryStack`, with its obsolete ThreadLocal initialization/removal removed. Ubuntu was removed from the focused workflow matrix and both new classes were added to its existing `-Dtest` selection.

The identical selectors were rerun after the edits. The two new classes plus `LmdbDataFileNativeMapTest`, `LmdbMappingAnchorTest`, `LmdbPageMappingLifecycleTest`, `LmdbPageCardinalityEstimatorDupSortNativeTest`, `TripleStoreAutoGrowTest`, and `TripleStoreTest` passed together. The repository formatter, copyright check, and diff check passed, followed by the full LMDB module through mvnf with retained logs and raw Surefire/Failsafe totals and skip counts. No commit or push was made.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j7`, run the required baseline command and wait for completion:

    set -euo pipefail
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

If offline resolution fails only because an artifact/plugin is missing, rerun the exact command once without `-o`, then return to offline commands. For another root failure, retry without `-T 1C` before diagnosing. Archive the existing `initial-evidence.txt` into the new task profile before writing any new compact evidence. Use mvnf selectors without `-am` or `-q` and retain logs.

The bounded child probes must use the test runtime classpath, `-ea`, a normal-completion marker, a finite timeout, forced cleanup after timeout, and a per-probe output file. They may assert `IOException` for oversized non-WRITEMAP initialization, but must never run that case on Windows. The header-buffer child must keep worker failures, readers, and warmup readers strongly reachable, join workers in `finally`, and measure only the JVM-managed direct-buffer pool.

## Validation and Acceptance

Before production edits, both new selectors must fail for a reason tied to the reported behavior, and their raw reports must be copied to `profiles/GH-6019/initialization-resources-20260918/red`. After production edits, the same selectors must pass and their reports must be copied to the matching green directory. The initialization tests must observe successful normal reopen/read/write and a bounded oversized failure/recovery on supported non-WRITEMAP platforms. The header test must observe worker reads for both byte orders and equal direct-buffer counts before and after measured readers close. The focused existing classes and then `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` must pass with failures/errors reported separately from skips. Windows must be reported as not running the oversized native probe; CI matrix changes must leave Linux to `pr-verify`.

## Idempotence and Recovery

The tests and plan are additive and may be rerun. Never delete or reset untracked artifacts. If a child JVM crashes or hangs, preserve its output/core-dump suppression result, force only that child process, and mark the platform limitation precisely. If a Maven run overwrites module reports, use the task-specific copies already made. If formatting changes unrelated files, stop and restore only newly introduced unrelated edits after inspection; do not touch historical artifacts.

## Artifacts and Notes

Keep `maven-build.log`, mvnf verify logs under `logs/mvnf`, raw report directories, compact evidence, child probe logs, and the task-specific profile directory. The root `initial-evidence.txt` already exists and must not be overwritten. The final handoff must link exact source/test/workflow paths and report the red and green snippets.

## Interfaces and Dependencies

Use existing LWJGL LMDB bindings (`mdb_stat`, `mdb_env_info`, and `setMapSize`), existing `TripleStore` transaction/resource ownership, `StoreProperties` persistence helpers, `LmdbDataFile(File, ByteOrder, pageSize)`, JUnit 6, and the Java `BufferPoolMXBean`. Do not add dependencies, reflection-based production hooks, native private-structure access, test-only production APIs, or store-format changes.

## Revision Note

Created 2026-09-18 to execute the approved initialization/resource and CI changes after the prior native page-mapping plan. The checklist and evidence locations are intentionally task-specific so historical generation-cache and mapping artifacts remain untouched.

Updated 2026-09-18 after the required red selectors: the root install passed, both regressions failed against unchanged production, and native crash artifacts were copied into the task profile. Implementation is now the sole active step.

Updated 2026-09-18 after the focused gate: production and workflow edits are complete, copyright/format checks passed, and the grouped 85-test selector is green. Full module verification remains active.

Updated 2026-09-18 after full verification: the complete LMDB module is green, all red/green/focused/full evidence is retained under the task profile, and the remaining active item is this parent handoff. No commits or pushes were made.

Updated 2026-09-18 for parent review follow-up: the active step is to strengthen the two regression classes with runtime page-size/minimum-map assertions, loaded populated oversized initialization, public MemoryStack frame restoration checks, complete worker/reader cleanup aggregation, and scoped child crash logs before rerunning focused and full verification.

Updated 2026-09-18 after post-review verification: dynamic native-size/minimum-map checks, four populated/empty oversized probe variants, stack-frame restoration assertions, child error-file scoping, and cleanup aggregation are green. Formatting, copyright, and diff checks passed. The final active step is parent review and handoff.

Updated 2026-09-18 after parent review completion: the plan is closed with no remaining active steps. All requested source, test, workflow, and evidence work is complete; artifacts and native red crash logs remain preserved, with no staged changes, commits, or pushes.
