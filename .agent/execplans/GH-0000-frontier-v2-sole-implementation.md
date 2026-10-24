# Make Frontier V2 the Sole LMDB Frontier Implementation

This is a living ExecPlan for `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. It follows the repository rules in `PLANS.md` and `.agent/PLANS.md`; those documents require this plan to be updated after every milestone and to remain self-contained. No branch change, commit, push, reset, clean, broad restore, or stash is part of this work.

## Purpose / Big Picture

RDF4J’s LMDB sail currently contains an older Frontier synopsis/planning path alongside the newer Frontier Statistics V2 path. This refactor makes V2 the only Frontier implementation, reducing duplicated lifecycle and persisted-state machinery while preserving the V2 planner behavior that users rely on: Omni design and audit lanes, projected-distinct statistics, center samples, subject/object/context Fast-AGMS projections, mutation and learning integration, and the independent quad and cold-filter synopses.

After the change, an eligible store uses the mapped V2 planning session; an ineligible, disabled, unavailable, or incompatible V2 generation follows the existing conventional scalar fallback. Explicit rebuilds use `rebuildFrontierStatistics()` and return `FrontierStatisticsStatus`. New statistics manifests use version 6 only, so old derived bytes are rejected and rebuilt rather than being interpreted with changed shard ordinals or compacted signed-AGMS coordinates. Authoritative LMDB data and the retained quad synopsis format remain intact.

The result is demonstrated by focused lifecycle, persistence, Count-Min, Fast-AGMS, planner, learning, quad, and cold-filter tests, followed by the LMDB module suite and any evaluation-module tests required by shared evaluation changes. Removal completeness is demonstrated with repository searches showing no production, configuration, benchmark, or maintained-documentation references to the retired functionality.

## Progress

- [x] (2026-09-08) Confirmed the active branch and recorded the dirty-worktree preservation boundary.
- [x] (2026-09-08) Read `PLANS.md`, `.agent/PLANS.md`, the `mvnf` instructions, and the high-performance Java guidance.
- [x] (2026-09-08) Captured the baseline install and focused Frontier evidence, then completed the first source/dependency census.
- [x] (2026-09-08) Added and ran the smallest failing lifecycle/API and version-6 manifest coverage before production behavior changes.
- [x] (2026-09-08) Remove the legacy synopsis/session/caches and obsolete configuration, controls, hooks, and exclusive tests.
- [x] (2026-09-08) Compact V2 Count-Min and signed Fast-AGMS persistence while preserving hashes, masks, counters, totals, and retained projections.
- [x] (2026-09-08) Migrate rebuild/session/cache identity APIs and update callers and benchmark setup.
- [x] (2026-09-08) Reconcile maintained documentation and complete the maintained-source census.
- [x] (2026-09-08) Adapt retained V2 budget fixtures and triage broad-suite regressions; replace the obsolete planning-integration assertions with focused V2 lifecycle/fallback/nested-query coverage.
- [x] (2026-09-08) Run the LMDB module suite: 2,195 tests passed with zero failures/errors and 101 skips.
- [in_progress] (2026-09-08) Run shared evaluation-module verification for changed planner/cache code.
- [ ] Format intended changes and complete final diff audits.
- [ ] Record final outcomes, evidence paths, remaining gaps, and a clean handoff without commit or push.

## Surprises & Discoveries

- The checkout contains many unrelated tracked edits and untracked artifacts, including prior Frontier research and benchmark material. All work must remain file-scoped; no bulk staging or cleanup is safe.
- Prior repository memory records that V2 lifecycle/rebuild behavior and optional-shard recovery are active in this checkout, while legacy quad synopsis and V2 use separate persistence/lifecycle systems. The implementation must not conflate those systems.
- Disk capacity is currently ample enough to begin, but prior runs in this checkout exhausted temporary storage during broad tests. Long-running tests will be preceded by capacity checks and kept after focused evidence.
- The first census found that base heavy-predicate AGMS storage already uses three compact coordinates while the signed mutation delta still allocates four and updates the predicate coordinate. Count-Min likewise indexes masks 1..15 but allocates a 16-mask slot per plane; the zero slot is unused.
- The old service is still opened unconditionally under `frontier-synopsis`; V2 is opened separately under `frontier-statistics-v2`. The old service owns the legacy mutation, rollback, rebuild, shutdown, and revision hooks in `LmdbSailStore` and `LmdbEstimatorRuntime`.
- There is no existing V2 manifest-store test class. The current V2 manifest writer emits version 5 and its decoder accepts both version 4 and 5, so the new version-6-only contract needs a dedicated focused test.
- The first compact Count-Min comparison failure was in the new fixture: the batched API consumes pre-hashed component coordinates, while the fixture supplied raw term IDs. After correcting the fixture, the production layout passed. The signed-AGMS gate then exposed and fixed the analogous production boundary mistake: compact storage still maps original component IDs 0, 2, and 3, so the mutation path must not pass compact IDs 0, 1, and 2.

## Decision Log

- Decision: Treat this as a significant refactor and maintain this ExecPlan from source census through final audit. Rationale: the request removes multiple implementations, changes persisted layout, and crosses lifecycle, planner, configuration, and tests. Date/Author: 2026-09-08, Codex.
- Decision: Use Routine A for lifecycle and persisted-format behavior: add and run the smallest failing in-repository tests before production edits. Rationale: the requested changes alter externally observable rebuild, fallback, and persisted-byte behavior. Date/Author: 2026-09-08, Codex.
- Decision: Preserve unrelated working-tree edits and untracked artifacts, and never use broad reset/restore/clean/stash. Rationale: the checkout is shared and already contains user work. Date/Author: 2026-09-08, Codex.
- Decision: Advance the statistics manifest to version 6 and accept only version 6. Rationale: removing enum members changes shard ordinals and compacting signed Fast-AGMS changes persisted coordinates; accepting older bytes would reinterpret incompatible data. Date/Author: 2026-09-08, Codex.
- Decision: Retain V2’s active design/audit lane implementation and active cache settings, but remove only the old public controls and caches explicitly identified by the request. Rationale: the user requires Omni audit lanes and surviving planner caches while removing legacy controls and allocations. Date/Author: 2026-09-08, Codex.
- Decision: Keep the legacy sketch estimator’s enabled/throttle/evidence/memory/cold-filter controls unless the consumer census proves them exclusive to removed functionality; remove the specifically named strategy, bucket, witness-cohort, and context-pair controls. Rationale: the request preserves the cold filter and explicitly names only those sketch controls for removal. Date/Author: 2026-09-08, Codex.
- Decision: Preserve the new failing contract tests while refactoring; do not weaken their assertions to match the current implementation. Rationale: the repository protocol requires a recorded failing reproduction before lifecycle and persisted-format changes. Date/Author: 2026-09-08, Codex.
- Decision: Keep the signed Fast-AGMS coordinate mapper expressed in original RDF component IDs and compact only the stored coordinate range. Rationale: predicate-keyed grouping and the existing subject/object/context hash domains remain part of the V2 contract; only the persisted coordinate slot for object/context is renumbered. Date/Author: 2026-09-08, Codex.

## Outcomes & Retrospective

This section is intentionally updated at each major milestone and completed at the end. It must state which requested systems were removed, which V2/quad/cold-filter behavior was preserved, exact verification results, and any environment-limited validation. No claim of completion is valid until the removal audit and required tests are complete.

## Context and Orientation

The implementation is in `core/sail/lmdb`. Frontier V2 is centered in `src/main/java/org/eclipse/rdf4j/sail/lmdb/frontier`, especially `FrontierStatisticsBuilder`, `FrontierStatisticsGeneration`, `FrontierStatisticsManifest`, `FrontierStatisticsShardKind`, `FrontierCountMinMatrix`, `FrontierSignedAgmsDeltaAccumulator`, `FrontierStatisticsDeltaBuilder`, the Omni builders/indexes, projected-distinct and center builders, and `LmdbStatisticsService`. Store lifecycle and planner selection are outside that package, chiefly in `LmdbSailStore`, `LmdbEvaluationStatistics`, the Frontier planning-session classes, planner/cache identity classes, and LMDB configuration classes.

The older implementation is identified by `LmdbFrontierSynopsisService`, its builder/manifest/payload/query-index types, `LmdbFrontierPackedCostSession` and its learning wrapper, old leaf/exact-transform caches, and the `sketch` package paths that have no surviving consumer. The source census must distinguish exclusive legacy types from shared snapshot, estimator, learning, quad-synopsis, and cold-filter infrastructure before deleting anything.

Count-Min is a frequency sketch. In this repository it counts rows in 15 nonempty masks for each statement plane; the compact V2 base layout must allocate 30 tables rather than 32 without changing mask identities, hash functions, counters, or exact unbound totals. Fast-AGMS is a signed projection sketch used to estimate joins and distinctness. Its retained signed delta coordinates are subject/object/context only; predicate-keyed grouping and hash/lane metadata remain where V2 still consumes them, but predicate-component updates must disappear. A statistics manifest describes the files and shard kinds in a derived generation; version 6 is an invalidation fence, not a migration format.

The retained systems are V2 leaf estimates, projected-distinct shards, join topology, center samples, Omni design/audit lanes, learning integration, snapshot pinning, mutation handling, the quad synopsis under `estimation/`, and the cold filter synopsis. Their settings include V2 mode, persistent and heap budgets, lag controls, internal build profiles, active cache settings, quad enablement/memory/throttling/evidence/capacity, and cold-filter capacity.

## Plan of Work

First establish a baseline from the current checkout. Run the required offline root quick clean install, preserve its full log in `maven-build.log`, and capture compact baseline evidence in a new task-specific `initial-evidence.frontier-v2-removal.txt`. Run the narrowest existing Frontier V2 and lifecycle/configuration/persistence selectors that are available before edits; retain logs and report paths. Complete a source census with `rg` and call-path inspection, recording the exact legacy-only files and shared dependencies in this plan.

Before changing production behavior, add focused tests for the requested lifecycle and persistence contract. The tests must fail on the current implementation for a reason tied to the requested removal: the rebuild method name/return contract, no creation or maintenance of the old synopsis, and version-6-only manifest acceptance/rebuild. Capture the Surefire/Failsafe report snippets immediately and keep the failing test code unchanged while implementing the fix.

Remove the older synopsis service and all of its exclusive builder, manifest/payload, query-index, lifecycle, mutation, rollback, rebuild, shutdown, and revision hooks. Remove `LmdbFrontierPackedCostSession`, its learning wrapper, and exclusive sampling/materialization helpers. Move the access-kernel helper into `LmdbMappedFrontierLearningSession` and move the exact-cardinality property constant into retained feedback code with identical behavior. Delete old leaf-payload and exact-transform cache classes and allocations without reallocating their byte budgets to other caches. Delete `FrontierBottomKRows` and any other type proven exclusive by the census.

Make the retained session selector choose mapped V2 for eligible generations and the existing conventional scalar session otherwise. Remove the legacy synopsis revision from planning identities and make former stale-validation call sites request replanning while retaining V2 generation validation, active plan-cache infrastructure, and Omni audit lanes. Remove the obsolete posterior/control state only after confirming no active plan-cache path consumes it.

Change the public rebuild API to `rebuildFrontierStatistics()` returning `FrontierStatisticsStatus` directly. Preserve initialization and active-write-transaction checks, and preserve V2 disabled/unavailable status. Update all repository callers, tests, benchmarks, and maintained documentation. Remove obsolete sketch and old-Frontier controls from fields, constants, getters/setters, RDF vocabulary, parsing/export, tests, benchmark setup, and documentation. Keep all explicitly retained V2, quad, and cold-filter settings.

Advance the statistics manifest to version 6 and remove version-4 decoding. Delete retired shard kinds and registration/fallback paths for `DISTINCT_HLL`, `EDGE_SAMPLES`, and `ADAPTIVE_REFINEMENT`, while retaining the active adaptive resource tier used by heavy-object counts and projected-distinct shards generated by current builds. Compact the base Count-Min layout to 30 tables and update heap accounting. Compact signed Fast-AGMS to subject/object/context coordinates, remove predicate-component updates, and preserve predicate-keyed grouping, hashes, lanes, the three consumed projections, mutation deltas, explicit/inferred planes, compaction, and reopen behavior.

Adapt tests by deleting tests that exclusively exercise removed implementations and strengthening retained tests for nested query planning, lifecycle/fallback, version-6 persistence/rejection, Count-Min masks and totals, Fast-AGMS projections and mutations, optional-shard recovery, quad/cold-filter settings, and removal completeness. Update benchmarks only where they refer to removed APIs or systems; do not remove benchmark data or research artifacts unrelated to this request.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. Maven commands always use `.m2_repo`, offline mode when possible, no `-q`, and no `-am` for test execution. Root installs may use `-am` only when tests are skipped by the `-Pquick` install profile.

1. Record `git status --short --branch`, available disk space, Java/Maven versions, and the current baseline file census. Do not alter unrelated files.
2. Run the required root baseline command:

       mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

3. Use `python3 .codex/skills/mvnf/scripts/mvnf.py ... --retain-logs` for each baseline and post-change focused selector. Capture report snippets from `target/surefire-reports` or `target/failsafe-reports` and preserve compact task evidence separately from overwritten report directories.
4. Use `rg --files`, `rg -n`, `git diff -- <path>`, and direct source inspection to maintain the exact deletion and retention allowlists. Never use a repo-wide search/replace script.
5. After every grouped production change, run the most specific relevant test selection and update this plan’s `Progress`, `Surprises & Discoveries`, and `Decision Log` sections before moving to the next milestone.
6. Before final handoff, run the required formatter/checkCopyright workflow only over intended changed Java/XML resources, run focused tests again, run the LMDB module suite, run evaluation-module tests if shared evaluation code changed, and audit `git diff --check`, production references, configuration exports, active documentation, persisted version constants, and the complete working-tree status.

## Validation and Acceptance

The pre-change reproduction must contain a failing in-repository test report before behavior-changing production edits. The post-change focused selectors must pass with the same test names and assertions. Every evidence block must identify its exact command, retained log, report directory/file, and a one-to-thirty-line report snippet.

Lifecycle acceptance requires startup, explicit rebuild, reopen, committed insertion and deletion, rollback, disabled V2, unavailable V2, and incompatible-generation recovery. The assertions must show that no older Frontier synopsis directory, service, revision, or lifecycle hook is created or maintained and that the selected session is mapped V2 only when eligibility rules permit it.

Retained-estimation acceptance requires V2 leaf, projected-distinct, join-topology, learning, snapshot-pinning, planner, and nested-query tests. Count-Min acceptance covers all 15 nonempty masks in both statement planes, scalar and batched construction, exact unbound totals, and reduced heap accounting. Fast-AGMS acceptance covers subject/object/context estimates over base-plus-delta generations, insertion/deletion mutation signs, explicit/inferred planes, reopen, compaction, and absence of predicate projections. Persistence acceptance covers version-6 round trips, rejection/rebuild of incompatible manifests, corruption handling, and optional-shard recovery. Quad and cold-filter acceptance retains their lifecycle, persistence, selectivity, capacity, and surviving configuration round trips.

Removal acceptance is a source audit: no production references, configuration vocabulary/export/parser paths, active benchmark setup, or maintained documentation may advertise removed controls, classes, shard kinds, aliases, or old APIs. The audit must explicitly distinguish unrelated research artifacts and untracked user work, which remain untouched.

## Idempotence and Recovery

The source census, status checks, formatting checks, and read-only audits are repeatable. Focused Maven runs are repeatable after rerunning the root install when changed modules are involved. If a test run fails, preserve the report and log, classify the failure as introduced, pre-existing, environmental, or unrelated dirty-worktree impact, and update this plan before retrying. If disk pressure appears, stop broad testing, record the exact error, and do not delete user artifacts; ask before any material cleanup. If a source edit overlaps an unrelated dirty edit, stop that file’s edit and report the conflict rather than overwriting it.

The intended final state is an uncommitted working tree containing only the requested implementation/test/documentation changes plus all pre-existing unrelated changes and untracked artifacts. No migration utility, filesystem sweep, compatibility alias, deprecated API stub, commit, or push is to be added.

## Artifacts and Notes

Expected task-specific artifacts are:

    .agent/execplans/GH-0000-frontier-v2-sole-implementation.md
    initial-evidence.frontier-v2-removal.txt
    maven-build.log
    logs/mvnf/*-verify.log

Baseline evidence recorded so far:

    root quick clean install: `maven-build.log`, BUILD SUCCESS, RDF4J: LmdbStore SUCCESS
    `FrontierStatisticsBuildConfigTest`: `logs/mvnf/20260908-111231-verify.log`, 7 tests, 0 failures/errors
    compact baseline: `initial-evidence.frontier-v2-removal.txt`

Pre-fix reproduction evidence:

    `LmdbFrontierSoleImplementationContractTest`: `logs/mvnf/20260908-112259-verify.log`, 2 tests, 1 error because `LmdbStore.rebuildFrontierStatistics()` is absent.
    `FrontierStatisticsManifestVersionTest`: `logs/mvnf/20260908-112348-verify.log`, 2 failures: the writer emits version 5 and version 5 is accepted.
    The latter report remains at `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.frontier.FrontierStatisticsManifestVersionTest.txt`; the former report was overwritten by the second focused run after its output was captured in the turn log.

Post-change focused evidence:

    `FrontierCountMinMatrixTest`: raw focused verify, one test passed; report `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.frontier.FrontierCountMinMatrixTest.txt`, log `logs/mvnf/20260908-130047-verify.log`.
    `FrontierStatisticsBuildConfigTest`, `FrontierStatisticsBuilderTest`, and `FrontierSignedAgmsDeltaAccumulatorTest`: one grouped verify, 63 tests passed; reports under `core/sail/lmdb/target/surefire-reports/`, with the builder report recording 53 tests and the signed-AGMS report recording 2 tests.
    `LmdbFrontierSoleImplementationContractTest`, `LmdbStoreConfigFrontierTest`, `LmdbPackedCostModelV2SessionTest`, and `FrontierStatisticsManifestVersionTest`: one grouped verify, 27 tests passed; reports under `core/sail/lmdb/target/surefire-reports/`.

The plan must be updated with exact test counts, report paths, version-6 evidence, the final removal audit, and any blocked validation. Existing untracked evidence, benchmark, research, profile, and temporary artifacts are not task artifacts and must not be deleted or rewritten.

## Interfaces and Dependencies

At completion, the public LMDB store operation is `rebuildFrontierStatistics()` returning `FrontierStatisticsStatus` directly. The old `rebuildFrontierSynopsis()` operation, old Frontier synopsis service/session/cache APIs, removed controls, aliases, and compatibility stubs must not exist.

The retained V2 statistics persistence accepts manifest version 6 only. Its shard-kind enum and readers contain only current V2 kinds, including retained projected-distinct, center/leaf/Omni, heavy-object/adaptive resource, quad-independent, and cold-filter-independent paths as currently defined by their consumers. The base Count-Min accumulator allocates 30 tables for the 15 nonempty masks across the two statement planes. The signed delta accumulator stores and updates subject/object/context coordinates only while preserving existing hashes, lane counts, grouping, and mutation semantics.

The retained mapped learning session owns the access-kernel helper formerly held by the removed packed-cost session. Retained runtime-feedback code owns the exact-cardinality property constant formerly shared with the removed session. Planner identity contains V2 generation validity but no older synopsis revision, and stale paths request replanning instead of invoking a removed validator.

No new external dependency is expected. If compilation reveals an actually exclusive dependency, remove it from the smallest owning module after proving no retained consumer uses it; do not delete a package solely because its name contains “sketch” or “Frontier”.

## Milestones

Milestone 1 establishes a reproducible baseline and exact source/dependency census. Its acceptance is a successful root quick install plus retained focused baseline reports and an updated plan listing legacy-only versus shared files.

Milestone 2 adds and runs failing lifecycle/version-6 coverage. Its acceptance is a preserved Surefire/Failsafe failure that demonstrates the current implementation does not yet satisfy the requested API/persistence boundary.

Milestone 3 removes legacy implementation and obsolete controls while keeping V2 session selection and active planner/cache behavior. Its acceptance is focused compilation/tests with no old production references.

Milestone 4 changes V2 persistence and compact sketches. Its acceptance is focused Count-Min, Fast-AGMS, manifest, corruption, optional-shard, and retained-estimation tests passing with version-6 evidence.

Milestone 5 performs broad verification and final audit. Its acceptance is the LMDB module suite, required evaluation suite, formatter/checkCopyright, diff audit, and a complete final handoff with exact evidence classification.

## Plan Revision Notes

- 2026-09-08: Created the initial self-contained ExecPlan after reading repository planning rules, the `mvnf` workflow, the high-performance Java guidance, current branch/status, available disk space, and relevant Frontier/V2 memory. No production source was changed and no tests were run at plan creation time.
- 2026-09-08: Completed the baseline root install and the first focused V2 configuration test, persisted their evidence, and mapped the old service/session/cache ownership against the retained V2 lifecycle. No production source was changed.
- 2026-09-08: Replaced the legacy-heavy planning integration class with five focused V2 tests after the old session assertions failed en masse; the retained class now passes with default/rebuilt/disabled V2, nested EXISTS, and planner-version coverage.
