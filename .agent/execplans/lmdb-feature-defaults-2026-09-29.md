# Make LMDB acceleration explicitly opt-in

This ExecPlan is a living document and follows `/Users/havardottestad/Documents/Programming/rdf4j/.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current while implementing it.

## Purpose / Big Picture

After this change, an LMDB store created with ordinary settings uses RDF4J's standard `DefaultEvaluationStrategyFactory` and leaves the native query engine, direct adjacency index, and compressed value overlay off. Users can enable any combination through three independent booleans on `LmdbStoreConfig` or repository configuration. Turning on one feature must not silently activate either of the others. A regression test will demonstrate the new default, and integration tests will demonstrate that all eight combinations retain the same query results while only requested accelerators start.

The standard and strict query evaluation modes are separate from the factory choice. Preserve the mode selected by `AbstractSail` or an explicit repository setting. An explicitly supplied custom evaluation factory continues to take precedence over automatic factory selection.

## Progress

- [x] (2026-09-29 09:03 UTC) Inspect current configuration lifecycle defaults
- [x] (2026-09-29 09:04 UTC) Write factory selection regression test
- [x] Capture initial red and evidence safely
- [x] (2026-09-29 09:44 UTC) Add three parsed and exported settings
- [x] (2026-09-29 09:45 UTC) Gate evaluator adjacency overlay independently
- [x] (2026-09-29 10:06 UTC) Opt tests benchmarks templates docs
- [x] (2026-09-29 10:24 UTC) Check copyright and branch formatter
- [x] Add parser and nested-query regressions
- [x] Capture nonliteral and multiple value red
- [x] Enforce single literal per predicate
- [x] Run focused settings and factory tests
- [x] (2026-09-29 12:52 UTC) Run focused fixture migration tests
- [x] Resolve default evaluator isolation regression
- [x] (2026-09-29 14:03 UTC) Capture first full verify failure
- [x] (2026-09-29 14:40 UTC) Reproduce two Failsafe errors
- [x] Probe the adjacency reader behavior
- [x] Cover adjacency and fallback snapshot paths
- [x] Finalize snapshot docs and formatting checks
- [x] Rerun complete LMDB verification after repair
- [x] Clarify startup restart requirement
- [x] (2026-09-29 16:09 UTC) Run repository and tool integration tests
- [x] (2026-09-29 16:11 UTC) Review final diff and evidence records
- [in_progress] Deliver final evidence handoff

## Surprises & Discoveries

- `LmdbStore.getEvaluationStrategyFactory()` currently selects the native factory automatically, while `getConnectionEvaluationStrategyFactory()` wraps it in an adaptive factory. The change must preserve this wrapper and its delegation of optimizer pipeline, resolver, result tracking, collection factory, and iteration-cache threshold.
- Adjacency already has a central null gate: `LmdbSailStore` constructs the store only when the resolved mode is not `DISABLED`; commit hooks, write admission, and query readers already check for a non-null store. The new boolean should resolve to effective `DISABLED` before construction.
- The value-overlay startup budget property defaults to zero, so current automatic startup warming is off. Explicit `warmCompressedValueOverlay(...)` can still populate the registry. Therefore the new false setting must gate explicit warming and every registry consumer, not only startup scheduling.
- `rdf4j.lmdb.valueOverlay.retained.maxBytes` configures a shared retained-memory budget whose current default is 2 GiB. If `valueOverlayEnabled` is true and `rdf4j.lmdb.valueOverlay.maxBytes` is absent, use that configured shared cap; explicit zero continues to suppress automatic warm-up and a positive value remains an override.
- When direct adjacency is disabled, resolving no index options must not parse legacy runtime tuning. The resolver now returns an inert disabled configuration before checking those properties; an invalid worker count or commit budget therefore cannot affect a store that opted out.
- The required focused red test passed through Maven and failed for the intended factory mismatch. Its Surefire report is preserved at `.agent/evidence/lmdb-opt-in-20260929/default-factory-red-surefire.txt`; the same report and command were appended to `initial-evidence.txt` without changing its pre-existing prefix.
- The legacy `mvnf` runner truncates root `maven-build.log`; the first focused run replaced that pre-existing untracked log. The prescribed root clean-install log and failing test report are retained separately. Avoid further legacy-runner truncation by retaining runner logs to task-specific paths.
- The first full LMDB verify exposed stale implicit-native assumptions in the sketch-planning fixture and forced-fallback benchmark. Both test configs now opt into native evaluation because they assert or force native LMDB behavior. The fallback benchmark's fixed-predicate case also made no progress for several minutes after its runtime-property-only setup; its interrupted report snapshot and verify log are preserved under `.agent/evidence/lmdb-opt-in-20260929/lmdb-full-interrupted-20260929T105322/`.
- The second full LMDB verify completed but failed 3,159 tests with errors, most caused by `java.io.IOException: No space left on device` while opening small LMDB environments. The host filesystem reports 204 GiB free, but the isolated Maven build tree is 1.8 GiB; `quota` inspection is denied. Reports and the full verify log are preserved at `.agent/evidence/lmdb-opt-in-20260929/lmdb-full-no-space-20260929T111748/`. Retry test output under `/private/tmp` to separate a `.mvnf` workspace limit from actual product failures.
- A focused `LmdbCascadeValueExceptionTest` run with build and temp roots under `/private/tmp` reproduced the same initialization error immediately, with all 12 cases failing in `PersistentSetFactory` at `mdb_env_open`. This rules out the `.mvnf` workspace tree as the limiting path. The focused failure log is `.agent/evidence/lmdb-opt-in-20260929/external-focused-cascade-verify.log`; test Linux in the available JDK 25 Docker image to isolate the host platform.
- The Linux full-suite attempt reached `LmdbEvaluationStatisticsMemoizationTest.repeatedOptimizerNeedPromotesBackgroundSamplingRequestToFront`, whose shared readiness helper waits for a native factory while the test config omitted `nativeEvaluationEnabled=true`. A thread dump confirmed it was in the test's sleep/readiness loop; the partial reports and stack are saved at `.agent/evidence/lmdb-opt-in-20260929/docker-full-interrupted-eval-stats/`. The feature-specific statistics fixtures now explicitly enable native evaluation.
- The restarted Linux full-suite reached `LmdbNativeKernelDeclineCensusTest.everyCompiledKernelStrategyWinsTheThemeCensus` while its config enabled adjacency but omitted native evaluation. Its main thread spent over nine minutes of CPU in standard `DefaultEvaluationStrategy` LEFT JOIN evaluation on a scaled theme query. The thread dump is `.agent/evidence/lmdb-opt-in-20260929/docker-logs/lmdb-full-final-thread-dump-2.txt`; this confirms another stale native-test fixture rather than a native-resource stall. Preserve partial reports before adding the missing explicit opt-in.
- The focused fixture-migration gate ran 608 tests and failed only `LmdbSailStoreTest.orderedNativeSourceSeparatesPlannerWitnessFromSelectedIndex`: after enabling native evaluation and direct adjacency, the expected adjacency-arbitrated order name was still the LMDB fallback `posc`. The native source's adjacency view was not built before this assertion; make the test synchronously build its explicitly requested adjacency index.
- The native-source arbitration test now calls `AdjacencyEngagementTestAccess.buildNow` after opting into direct adjacency. Its exact Surefire method selection passes (1 test, zero failures/errors/skips) in Linux JDK 25; the red class report and focused log are preserved before the complete 30-class focused rerun.
- The first complete Linux LMDB run completed 6,496 tests with 0 failures, 2 errors, and 20 skips. The errors are reproducible in the default standard-evaluator path: `LmdbOptimisticIsolationTest.test_safeQuery` and `test_safeInsert` now commit-conflict because generic join planning records a broader read pattern than the earlier LMDB optimizer pipeline. The full report snapshot is `.agent/evidence/lmdb-opt-in-20260929/full-lmdb-verify-final-report/surefire-reports/`; the focused repro log and report are `.agent/evidence/lmdb-opt-in-20260929/optimistic-isolation-focused-rerun.log` and `.agent/evidence/lmdb-opt-in-20260929/optimistic-isolation-focused-red/surefire-reports/`.
- A focused causal probe injected LMDB's optimizer pipeline into a `DefaultEvaluationStrategyFactory` while leaving all accelerators, including native evaluation, off. The full optimistic-isolation suite passed 115/115 both with the original native optimizer pipeline and with the standard fallback pipeline selected by `supportsJoinEstimation()`. The standard fallback probe proves the default no-sketch case; preserved reports are under `.agent/evidence/lmdb-opt-in-20260929/optimistic-isolation-standard-pipeline-probe-green/`.
- The automatic non-native path now caches `LmdbDefaultEvaluationStrategyFactory`, which subclasses `DefaultEvaluationStrategyFactory`, preserves an explicitly configured optimizer pipeline, and installs the matching LMDB pipeline only when no override exists. Both stats branches are covered by `LmdbOptimizerPipelineTest`; the focused factory/flag tests pass 26/26, and the unchanged default-config optimistic isolation suite passes 115/115. Evidence is under `.agent/evidence/lmdb-opt-in-20260929/default-pipeline-feature-matrix-green/` and `optimistic-isolation-default-pipeline-green/`.
- The first full Linux ARM64 module gate after that repair passed 6,388 Surefire tests with zero failures/errors and 20 skips, then failed in Failsafe: 149 tests, zero assertion failures, two errors, and 99 skips. Both errors are in `LmdbAutogrowIsolationIT`; the saved XML shows `SNAPSHOT transaction invalidated` during `SNAPSHOT_READ` result iteration and an exhausted pinned iterator. The full log and report snapshot are under `.agent/evidence/lmdb-opt-in-20260929/full-lmdb-verify-post-pipeline*`. Isolate these cases before retrying the module gate.
- The focused Linux ARM64 `LmdbAutogrowIsolationIT` rerun reproduces the same two errors in 10 tests. `TxnManager.Txn.setActive` invalidates revision-pinned readers while the environment is parked for a map resize, and the next native record pull correctly refuses a renewed transaction. The suite's shared `autoGrowConfig()` used the historical implicit `PREFER` adjacency behavior; after the new false default, these growth fixtures may no longer exercise the prior in-memory read path. Check explicit adjacency opt-in before considering a production change.
- An isolated probe passed both affected methods only after adjacency was enabled in the default `PREFER` mode and its immutable base was built. A later focused run confirmed that `SHADOW` still uses authoritative mapped reads and does not retain the query snapshot. The generic mapping-backed path must retain fail-safe snapshot invalidation, and the exhausted-iterator case must prove EOF before resizing; the test migration covers both paths without changing production behavior.
- `LmdbAutogrowIsolationIT` now passes all 11 Failsafe tests: a PREFER-backed built index retains the open query's snapshot, the default-off mapped path reports retryable invalidation and a fresh transaction sees committed growth, and the exhausted-iterator case reaches EOF before resize.
- The repaired full Linux ARM64 module gate passes 6,388 Surefire tests (20 skipped) and 150 Failsafe tests (99 skipped), with zero failures or errors in either suite.

## Decision Log

- Decision: Default `nativeEvaluationEnabled`, `directAdjacencyEnabled`, and `valueOverlayEnabled` to false. Rationale: the user requested that every accelerator be independently opt-in. Date/Author: 2026-09-29, Codex.
- Decision: Use `DefaultEvaluationStrategyFactory` for the automatic non-native path. Rationale: it is the current successor to the deprecated strict factory; query evaluation mode remains an independent setting. Date/Author: 2026-09-29, Codex.
- Decision: Keep routine fixture repair and serialized validation with this Luna Max coordinator; use a Sol Max worker only if a separate complex root-cause issue appears. Rationale: the remaining authorized work is a bounded test migration, and no test or formatting operation may overlap. Date/Author: 2026-09-29, Codex.
- Decision: Let the adjacency boolean gate activation, while the legacy mode only chooses the enabled mode. Suppress `buildOnStart` when adjacency is disabled; reject an enabled setting combined with explicit `DISABLED`. Rationale: a legacy `PREFER` or `SHADOW` value alone must not activate the feature, while an explicit contradictory disable should be reported. Date/Author: 2026-09-29, Codex.
- Decision: Keep value-overlay property tuning subordinate to the boolean. Rationale: `maxBytes` alone must not activate a disabled feature; when enabled, absent budget means use the configured shared cap, explicit zero means no automatic startup warm-up, and positive means the caller-selected cap. Date/Author: 2026-09-29, Codex.
- Decision: Do not migrate persisted LMDB data. Rationale: all three features are derived runtime behavior; committed triples, value IDs, and record encodings remain authoritative and unchanged. Date/Author: 2026-09-29, Codex.
- Decision: Return an inert disabled adjacency option set before parsing any runtime tuning. Rationale: legacy adjacency tuning is dormant when `directAdjacencyEnabled` is false, and malformed dormant worker/admission values must not affect startup. Date/Author: 2026-09-29, Codex.

## Outcomes & Retrospective

The three LMDB runtime accelerators now have independent false-by-default per-store startup settings, with matching Java and RDF repository configuration, and legacy tuning values cannot activate them. The default evaluator remains the standard RDF4J evaluator while retaining LMDB's normal optimizer pipeline; explicit factories and custom pipelines keep precedence. Tests cover all eight feature combinations in both query modes, malformed configuration values, nested SPARQL behavior, adjacency lifecycle and fallback, overlay budget choices, and factory/configuration propagation. No persistent data migration is required.

The final Linux ARM64 / Temurin 25 `core/sail/lmdb verify` gate passed 6,388 unit tests (20 skipped) and 150 integration tests (99 skipped), with zero failures or errors. The bulk-load tools module passed 45 tests; the affected Repository API and Workbench selections passed 10 and 21 tests respectively. Preserved commands, logs, Surefire/Failsafe reports, and focused feature reports are under `.agent/evidence/lmdb-opt-in-20260929/`; the appended final evidence is in `initial-evidence.txt`. Copyright validation, formatting, and `git diff --check` passed. macOS native LMDB initialization continued to fail at `mdb_env_open` with `ENOSPC`; the precise host cause was not established, so the full runtime result is reported from Linux only. No commit or push was requested or performed.

Plan update (2026-09-29 09:05 UTC): `LmdbOptimizerPipelineTest#ordinaryStoreUsesDefaultEvaluationStrategyFactory` failed against the current native default; the full report and concise evidence are preserved. The only next in-progress step is implementing the three independently parsed settings and factory gate.

Plan update (2026-09-29 09:45 UTC): the three false-default RDF settings, factory selection, adjacency activation gate, and overlay lifecycle gate are implemented. The adjacent resolver now skips dormant tuning properties completely. Config, eight-combination, template, benchmark, repository-test, and documentation coverage is being completed before validation.

Plan update (2026-09-29 10:06 UTC): the repository template and focused configuration tests now cover the three false defaults and boolean round-trips. Native-only suites and benchmark variants explicitly select the native factory; adjacency-dependent cases opt in, while generic/fallback comparisons remain off. The all-eight-combination test checks factory selection, actual adjacency and overlay activity, and query results in both evaluation modes. Overlay tests also cover no per-build budget, explicit zero, positive budget, and refusal under a fully reserved shared cap. One in-progress step remains: run the copyright check and branch formatter before focused verification.

Plan update (2026-09-29 10:24 UTC): the full repository copyright/SPDX check passes after repairing 18 affected LMDB test header terminators and confirming Codex signatures on every edited Java file. The branch's JDK 25+ Spotless formatter has run on `core/sail/lmdb` and `core/repository/api`. All implementation, configuration, test opt-ins, template, and documentation edits are code-complete; the single in-progress step is focused and broad validation.

Plan update (2026-09-29 10:28 UTC): the post-change default-factory regression passes (1 test, zero failures). Independent review identified missing coverage for malformed RDF flag values, nested query semantics across all flag combinations, explicit-factory restoration/default pipeline forwarding, rendered opt-in templates, and precise overlay budget wording. The next in-progress step adds those regression tests before any corresponding parser changes.

Plan update (2026-09-29 10:39 UTC): strict malformed-config tests now fail for all three accelerator predicates when given an IRI object or conflicting true/false literals. Surefire evidence is preserved at `.agent/evidence/lmdb-opt-in-20260929/accelerator-rdf-parse-red-surefire.txt` and summary at `.agent/evidence/lmdb-opt-in-20260929/accelerator-rdf-parse-red-summary.txt`; implementation will now reject each predicate unless it has exactly one literal object.

Plan update (2026-09-29 10:45 UTC): the single-literal helper now rejects nonliteral or multiple objects while preserving absent=false and lexical boolean validation. Focused greens: `LmdbStoreConfigTest` 108/0, expanded `LmdbFeatureFlagsTest` 3/0 (eight flag combinations × both modes, compound queries, clear/reopen), and `LmdbOptimizerPipelineTest` 22/0 (standard-factory pipeline/settings forwarding and override reset included). Combined evidence is `.agent/evidence/lmdb-opt-in-20260929/focused-green-summary.txt`; the next in-progress step verifies the remaining accelerator lifecycle gates and broad module suites.

Plan update (2026-09-29 11:00 UTC): the first full LMDB verify exposed one planner-metrics failure and stalled in the forced-fallback benchmark's fixed-predicate case. The former lacked native factory opt-in after the intentional default change; the latter relied on a native runtime property that no longer selects the native factory by itself. Their shared test/benchmark setup now opts in explicitly. The interrupted report snapshot is preserved; next rerun both classes before retrying the full LMDB gate.

Plan update (2026-09-29 11:20 UTC): the targeted planner suite passes 10 tests and the forced-fallback benchmark suite passes all 12 cases after explicit native opt-in. Their Surefire reports and logs are preserved under `.agent/evidence/lmdb-opt-in-20260929/planner-opt-in-focused-green/` and `fallback-benchmark-focused-green/`. Restarting full LMDB verification is now the next step.

Plan update (2026-09-29 11:50 UTC): the macOS cascade suite still fails at `mdb_env_open` with `No space left on device` when both build and temp roots are under `/private/tmp`, while the same 12 tests pass in Linux ARM64 Temurin 25 Docker. The Linux module run then exposed a slow native-factory assertion with an unopted fixture in `LmdbEvaluationStatisticsMemoizationTest`; a thread dump captured the readiness wait. The affected tests now explicitly opt into native evaluation. Next rerun that focused class and continue full validation in Docker.

Plan update (2026-09-29 11:55 UTC): `LmdbEvaluationStatisticsMemoizationTest` now passes 32 tests, zero failures/errors/skips in Linux Docker, including the previously sleeping automatic-factory assertion. Its focused Surefire report and verify log are preserved in `.agent/evidence/lmdb-opt-in-20260929/docker-eval-memo-focused-green/`. Run the final formatter, then the full LMDB module gate again.

Plan update (2026-09-29 12:08 UTC): Linux removed the host `mdb_env_open` constraint; the full-suite retry found another test-only migration gap. A thread dump captured `LmdbNativeKernelDeclineCensusTest.everyCompiledKernelStrategyWinsTheThemeCensus` running its scaled theme workload through the standard evaluator for over nine minutes of CPU, because its config enables adjacency but not native evaluation. The current module-gate step is now a systematic fixture audit before the next full verification.

Plan update (2026-09-29 12:43 UTC): the focused 30-class fixture migration completed 608 tests with 607 passing and one failure in `LmdbSailStoreTest.orderedNativeSourceSeparatesPlannerWitnessFromSelectedIndex`. The updated test now opts into both native evaluation and adjacency, but it must synchronously finish the adjacency build before asserting the selected index. The failing report is preserved under `.agent/evidence/lmdb-opt-in-20260929/focused-fixture-migration-red/`; resolve this readiness gap before rerunning the focused gate.

Plan update (2026-09-29 12:46 UTC): the native-source readiness correction passes its exact method selection (1 test, no failures/errors/skips). `AdjacencyEngagementTestAccess.buildNow` was the required fixture contract because the assertion inspects adjacency arbitration directly. The next gate repeats the full 30-class migrated fixture selection before the LMDB module suite.

Plan update (2026-09-29 12:52 UTC): the full 30-class focused fixture selection now passes 608 tests with zero failures, errors, or skips. The native kernel decline census's scaled-theme method completes in 229.7 seconds with the explicit native and adjacency settings. The focused Surefire reports and exact command are preserved in `.agent/evidence/lmdb-opt-in-20260929/focused-feature-fixture-migration-green/`. Proceed with the full Linux `core/sail/lmdb` `verify` gate, including Failsafe.

Plan update (2026-09-29 11:30 UTC): the second full LMDB verify completed with 6,357 tests, 68 failures, 3,159 errors, and 20 skips. Most failures are `No space left on device` during LMDB environment creation despite 204 GiB reported free on the host volume. This run's full Surefire snapshot and log are preserved. Revalidate with Maven build and test-temp roots under `/private/tmp` before deciding whether remaining failures are product issues.

Plan update (2026-09-29 11:40 UTC): the focused cascade test reproduced `No space left on device` on its first environment open from `/private/tmp`, so the failure is not limited to the `.mvnf` build tree. The exact failing native operation is `mdb_env_open` in `PersistentSetFactory`; a Linux JDK 25 Docker image is available, so the next check runs there before attributing the issue to the patch.

Plan update (2026-09-29 11:45 UTC): the same 12-test cascade suite passed 12/12 in Linux ARM64 Temurin 25 Docker with separate `/private/tmp` build/temp roots. It failed 12/12 on macOS at `mdb_env_open`, confirming a host-native LMDB initialization limitation rather than repository workspace capacity. The offline Docker root quick install and focused test logs/report are preserved in `.agent/evidence/lmdb-opt-in-20260929/docker-root-install.log` and `docker-cascade-focused-green/`; run the complete LMDB module in this Linux environment.

## Context and Orientation

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java` owns the LMDB options and RDF configuration parsing. `LmdbStoreSchema.java` defines the RDF predicates. `LmdbStore.java` chooses the evaluation-strategy factory and creates the adaptive connection wrapper. `LmdbSailStore.java` creates or omits the in-memory adjacency index and installs its commit hooks. `ValueStore.java` owns the compressed value-overlay lifecycle; `OverlayMemoryBudget.java` provides a process-shared memory cap. `core/repository/api/src/main/resources/org/eclipse/rdf4j/repository/config/lmdb.ttl` is the user-facing LMDB repository template. Tests live beside those modules, and branch-level LMDB guides live under `docs/branch/optimize-lmdb/`.

“Opt-in” here means the corresponding setting must be true before a feature allocates its runtime structure or starts its worker. The standard evaluator remains fully capable of evaluating RDF queries; it simply does not route them through LMDB-specific native operators unless `nativeEvaluationEnabled` is true. The adjacency index and value overlay are in-memory accelerators that may fall back to authoritative LMDB reads.

## Plan of Work

### Milestone 1: Capture the old factory default

Add a smallest-scope test to `LmdbOptimizerPipelineTest` asserting that a default `new LmdbStore(new LmdbStoreConfig())` returns a `DefaultEvaluationStrategyFactory`. Run only that method and retain its Surefire failure before changing production. Store the concise report and exact command in `.agent/evidence/lmdb-opt-in-20260929/` and append without replacing any existing data in top-level `initial-evidence.txt`.

### Milestone 2: Add explicit independent settings

Add fluent `get`/`set` methods and false defaults for `nativeEvaluationEnabled`, `directAdjacencyEnabled`, and `valueOverlayEnabled` in `LmdbStoreConfig`. Add matching predicates to `LmdbStoreSchema`, and parse/export explicit booleans with the same validation pattern as existing boolean properties. Absent repository configuration remains false. Add the controls and false defaults to the repository template and its tests.

In `LmdbStore`, freeze `nativeEvaluationEnabled` at construction. Automatic selection returns the native factory only when true; otherwise it returns `DefaultEvaluationStrategyFactory`. Preserve explicit factory precedence and make clearing an override restore the automatic selection. Preserve the existing adaptive connection wrapper and forward all configuration/customization through both automatic factory paths. Preserve the query mode selected by `AbstractSail` or configuration.

In `LmdbDirectAdjacencyOptions` and `LmdbSailStore`, map `directAdjacencyEnabled=false` to effective `DISABLED` before allocating the adjacency index. Legacy `PREFER` and `SHADOW` settings alone do not activate it. With the new boolean true, default to `PREFER` and startup build; an explicit legacy `DISABLED` is a configuration error. A true `buildOnStart` setting is suppressed when the new boolean is false. Confirm Java construction and RDF validation reject the enabled-plus-disabled contradiction.

In `ValueStore`, capture `valueOverlayEnabled` at construction. When false, do not initialize the configured registry, schedule startup warming, acquire snapshots, capture or publish mutations, compact, recreate the registry during clear, or retain a worker for shutdown. Explicit synchronous warming throws a clear `IllegalStateException`. When true, automatic warming runs asynchronously: absent `rdf4j.lmdb.valueOverlay.maxBytes` uses the configured shared retained-memory cap, explicit zero skips automatic warming, and a positive value overrides the startup construction budget while all allocations remain subject to the shared retained-memory cap. A budget property never activates the feature when its boolean is false. Preserve LMDB fallback and make the close-during-scan path drain the worker before closing the native environment.

### Milestone 3: Characterize combinations and update consumers

Add tests for all eight boolean combinations that assert factory selection, actual accelerator activity, and query results. Reuse query suites for nested joins, `UNION`, `OPTIONAL`, `MINUS`, `EXISTS`, `NOT EXISTS`, subqueries, property paths, named graphs, and both evaluation modes. Cover explicit factory precedence, custom optimizer pipelines, long-lived connections, mixed-configuration repositories, adjacency startup and commit/rollback, read-your-writes, snapshots, clear/reopen, close during a build, and disabled worker/maintenance behavior despite legacy settings. Cover overlay absent/zero/positive budget behavior, shared-cap refusal, manual warm-up, and read/commit/rollback behavior.

Update feature-specific tests and benchmarks to opt in explicitly, without weakening assertions. Keep ordinary store tests on all-false defaults. Update the two benchmark configuration helpers, the theme benchmark JVM arguments, and the repository/API template tests. Document new defaults, setting precedence, and migration examples in the appropriate LMDB documentation, including `docs/branch/optimize-lmdb/query-configuration.md`, `storage-adjacency-lifecycle.md`, and `storage-value-overlay.md` where they remain applicable.

### Milestone 4: Format and verify the complete change

Run the repository copyright check for changed/new sources and use the project formatter. First run focused tests for configuration parsing, default factory selection, and each accelerator lifecycle. Then run the full `core/sail/lmdb` suite and affected `core/repository/api`, repository, and Workbench tests with `mvnf --retain-logs`; run LMDB bulk-loader tests if edits affect its configuration or completion path. Maven tests must not use `-am` or `-q`. Use JDK 25 or newer and the workspace-local `.m2_repo`. Preserve existing untracked files and logs; capture any new evidence without truncating `initial-evidence.txt` or `maven-build.log`.

## Concrete Steps

Working directory for all commands is `/Users/havardottestad/Documents/Programming/rdf4j`. Use the bundled JDK 25+ already selected by the environment. Before the first test, run the repository quick clean install with output retained in `maven-build.log`, appending if it already exists. The required command shape is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

For each targeted test, use the repository runner and retain logs, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptimizerPipelineTest#ordinaryStoreUsesDefaultEvaluationStrategyFactory --module core/sail/lmdb --retain-logs

Run the changed module suites through the same runner, without `-am` or `-q`. When offline dependency resolution fails, retry that exact command once online and return to offline runs. Do not start concurrent Maven or formatting operations.

## Validation and Acceptance

Before production changes, the new factory-selection method must fail because the current default is `LmdbNativeEvaluationStrategyFactory`; retain the Surefire report snippet. After the implementation, it must pass and show the default is `DefaultEvaluationStrategyFactory`. Each of the eight settings combinations must return correct query results, and the observed factory/index/overlay activity must match only the enabled settings.

Repository configuration export followed by parse must preserve true and false values. Missing predicates must resolve false. Malformed non-boolean values must raise the repository configuration error. A custom factory must still win when configured, and clearing it must restore the factory chosen by `nativeEvaluationEnabled`. `STANDARD` and `STRICT` query modes must keep their pre-change semantics.

Disabled adjacency must create no index or worker, register no commit hooks, and impose no adjacency write wait even if legacy mode is `PREFER`/`SHADOW` or `buildOnStart=true`. Enabled adjacency must build and track committed updates, while rollback and snapshots remain correct. Disabled overlay must not warm even when old tuning properties are set; explicit warm must report that it is disabled. Enabled overlay must warm asynchronously under the shared cap when no per-build limit is set, while explicit zero and positive values retain their documented behavior. Full module suites must have no new failures; report exact module totals and any environment blocker.

## Idempotence and Recovery

All changes are additive configuration and tests; no on-disk migration is required. Before editing, preserve all existing working-tree and untracked artifacts. If a test fails, keep its report and diagnose the cause rather than weakening its assertion. If any generated evidence path already exists, append under a new unique filename. If the root build log or `initial-evidence.txt` already exists, append a clearly separated section and verify its previous byte prefix is unchanged.

## Artifacts and Notes

Keep the failing and passing factory-selection evidence, root build log excerpt, focused and module test reports, and command transcripts under `.agent/evidence/lmdb-opt-in-20260929/`. The top-level `initial-evidence.txt` remains append-only for this task. The ExecPlan itself is `.agent/execplans/lmdb-feature-defaults-2026-09-29.md`.

## Interfaces and Dependencies

The final public configuration interface contains getter/setter pairs for `nativeEvaluationEnabled`, `directAdjacencyEnabled`, and `valueOverlayEnabled` on `LmdbStoreConfig`, plus matching `LmdbStoreSchema` RDF predicates. Confirm the repository's naming convention in the existing source before implementing; use consistent getter naming if it differs from these proposed names.

The automatic factory interface remains `EvaluationStrategyFactory`. `LmdbStore` must select between `DefaultEvaluationStrategyFactory` and `LmdbNativeEvaluationStrategyFactory` based on its frozen config, while preserving the existing explicit-factory override and `AdaptiveEvaluationStrategyFactory` delegation. The adjacency index remains `LmdbDirectAdjacencyStore`; the value overlay remains `ValueOverlayRegistry` and `CompressedValueOverlay`, with `OverlayMemoryBudget` providing the shared cap. No new dependency is required.

Plan update (2026-09-29 15:12 UTC): the focused adjacency-on probe passed both previously failing methods (2/2); its Failsafe reports are preserved under `.agent/evidence/lmdb-opt-in-20260929/autogrow-adjacency-on-probe-reports/`. The causal boundary is explicit adjacency-backed retention versus retryable invalidation for a remapped LMDB snapshot. Next add a default-off invalidation/retry case and establish the iterator's EOF before growth.

Plan update (2026-09-29 15:22 UTC): the revised class run passed the new default-off invalidation/retry coverage and the EOF contract, but the adjacency snapshot case still used `SHADOW`, which does not serve the query from the immutable index. Use explicit `PREFER`, synchronously build its base before opening the query, and rerun the complete IT class.

Plan update (2026-09-29 15:27 UTC): all 11 `LmdbAutogrowIsolationIT` cases pass after the retention fixture was changed to explicit `PREFER` with startup building disabled and a synchronous base build. The focused command, full Maven log, and copied Failsafe reports are under `.agent/evidence/lmdb-opt-in-20260929/autogrow-isolation-focused-prefer*`; the next step is the complete LMDB `verify` gate after copyright and formatting checks.

Plan update (2026-09-29 15:30 UTC): parent review confirmed the two test paths and requested the map-growth boundary in user-facing docs. Clarify that mapped reads may fail with retryable snapshot invalidation while a fully served immutable adjacency view can retain its snapshot; then run copyright and formatter checks before the module gate.

Plan update (2026-09-29 15:32 UTC): the public LMDB Store page now documents the mapped-reader retry boundary and limits adjacency snapshot retention to operations fully served by a built immutable view. The copyright/SPDX check passed, `process-resources` completed successfully, and `git diff --check` is clean. Restart the complete Linux ARM64 LMDB module gate, including unit and integration tests.

Plan update (2026-09-29 15:55 UTC): full Linux ARM64 `core/sail/lmdb verify` passed with 6,388 unit tests (20 skipped) and 150 ITs (99 skipped), zero failures/errors. Copied reports are under `.agent/evidence/lmdb-opt-in-20260929/full-lmdb-verify-autogrow-repaired-reports/`. Parent review requested one user-facing sentence stating that startup-option changes require repository restart; add it before the separate bulk-load-tools test gate.

Plan update (2026-09-29 15:59 UTC): the public LMDB Store page and query-configuration reference now state that changing any per-store startup option requires repository restart. This documentation-only edit does not alter runtime behavior; proceed with the outstanding bulk-load-tools module tests and final evidence audit.

Plan update (2026-09-29 16:09 UTC): bulk-load-tools verification passed 45 tests with zero failures, errors, or skips. The completed LMDB module gate includes 6,388 unit tests (20 skipped) and 150 integration tests (99 skipped); affected Repository API and Workbench tests passed 10/10 and 21/21. Finish the tracked diff and evidence audit.

Plan update (2026-09-29 16:11 UTC): final report XML totals match the saved summaries; the relevant source/test/documentation diff is consistent with the approved snapshot boundary; `git diff --check` passes; and exactly one plan step remains in progress for the final handoff.

Plan update (2026-09-29 16:14 UTC): corrected the overlay budget description to distinguish the startup construction budget from the shared retained-memory allocation cap. This is documentation-only; existing validation evidence remains unchanged.
