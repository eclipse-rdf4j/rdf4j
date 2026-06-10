# Omni Sketch Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the LMDB sketch estimator use the vendored OmniSketch zip implementation as the default join-cardinality strategy, then prove with tests, query-plan evidence, and `ThemeQueryPlanRunBenchmark.runQuery` results that plans use Omni multi-predicate and multi-variable join estimates and beat the May/June benchmark averages.

**Architecture:** Keep RDF4J’s existing `SketchBasedJoinEstimator` planner and persistence boundaries, vendor the zip implementation under RDF4J internals, and make `OMNI` own compact Omni side-state beside the existing FastAGMS, tuple, JoinSketch, and Count-Min paths. The migration is not complete until LMDB tests pass and theme query plans visibly route the relevant join surfaces through Omni estimates.

**Tech Stack:** Java 25+, Maven, RDF4J query algebra evaluation module, LMDB store configuration, existing DataSketches dependency, and the local OmniSketch contribution zip at `/Users/havardottestad/Downloads/OmniSketchDataSketchesContribution.zip`.

---

This ExecPlan is a living document. Maintain it according to `PLANS.md` in the repository root.

## Purpose / Big Picture

LMDB currently has a partial Omni migration: the estimator can build Omni side-state and focused queryalgebra tests pass, but full LMDB verification still has planner and estimate assertion failures. After this work, users can configure `sketchEstimatorStrategy=omni`, new LMDB stores default to that strategy, and theme query plans should use Omni side summaries for multi-predicate and multi-variable join estimates instead of falling back to older FastAGMS, JoinSketch, Count-Min, or broad page-walk estimates. The behavior is visible through focused estimator tests, full LMDB tests, query-plan telemetry containing `omni-sketch-surface`, and fresh `ThemeQueryPlanRunBenchmark.runQuery` results that beat the chosen May/June targets.

## Progress

- [x] (2026-06-09 01:42Z) Ran required root quick clean install; build succeeded.
- [x] (2026-06-09 01:46Z) Inspected current RDF4J sketch estimator and OmniSketch contribution archive.
- [x] (2026-06-09 02:08Z) Reversed the first implementation decision after the user clarified that the zip implementation must be copied into the project.
- [x] (2026-06-09 02:18Z) Added `OmniSketchCoreTest` first and captured red evidence in `initial-evidence.txt`; the failure was `ClassNotFoundException` for the RDF4J Omni package.
- [x] (2026-06-09 02:30Z) Vendored the zip sources into `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/omni` with RDF4J headers and RDF4J package names.
- [x] (2026-06-09 03:05Z) Replaced interim Omni adapter wiring with zip-backed `UpdateOmniSketch` and `CompactOmniSketch` usage through `OmniFrequencySketch`.
- [x] (2026-06-09 03:20Z) Added `SketchStrategy.OMNI`, LMDB `omni` defaults, Omni side-state persistence, and lazy snapshot restore.
- [x] (2026-06-09 03:40Z) Focused tests passed: `OmniSketchCoreTest`, `SketchBasedJoinEstimatorConfigTest`, `SketchJoinSketchAccuracyComparisonTest`, `SketchBasedJoinEstimatorPersistenceTest`, and `LmdbStoreConfigTest`.
- [x] (2026-06-09 03:43Z) Required root quick clean install passed after the current dirty Omni migration state.
- [ ] Fix the remaining LMDB red tests caused by Omni estimate/plan drift.
- [ ] Run a full `core/sail/lmdb` verify with zero new failures.
- [ ] Run `ThemeQueryPlanRunBenchmark.runQuery` and parse it against May/June targets.
- [ ] Inspect query plans for Omni telemetry and improve estimators/rules/planners until benchmark targets are beaten.

## Surprises & Discoveries

- Observation: `core/queryalgebra/evaluation/pom.xml` already depends on `org.apache.datasketches:datasketches-java:9.0.0`, and public search results show DataSketches Java 9.0.0 as the latest release, but not an accepted OmniSketch package. The zip says it is a contribution overlay, not an ASF release.
  Evidence: `/tmp/omni-sketch-contribution/extracted/OmniSketchDataSketchesContribution/README.md` says “This is a contribution-ready overlay, not an ASF-accepted release.”
- Observation: RDF4J already has `OmniSketch` and `OmniFrequencySketch` in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch`, but `SketchStrategy` does not expose `OMNI`.
  Evidence: `SketchBasedJoinEstimator.SketchStrategy` currently lists `FAST_AGMS`, `TUPLE`, `JOIN_SKETCH`, `COUNT_MIN`, and `COUNT_MIN_DUAL`.
- Observation: Existing planner tests already cover cycles and dense multi-shared VALUES cliques through the robust planner; the new tests should focus on the strategy and estimate source rather than re-proving planner admission.
  Evidence: `SketchBasedJoinEstimatorJoinOrderPlannerTest` contains `planJoinOrderSupportsCycleWithCompactPlanner` and `planJoinOrderSupportsMultiSharedValuesClique`.
- Observation: The first full `core/sail/lmdb` verify under default Omni failed mostly because existing tests assert exact old plan or estimate behavior. The broad failure summary was 17 failures and 6 errors before the aggregate fallback guard.
  Evidence: `logs/mvnf/20260609-032637-verify.log` reported `Tests run: 1647, Failures: 17, Errors: 6, Skipped: 55`.
- Observation: The `Unknown function 'urn:rdf4j:native:org.eclipse.rdf4j.query.algebra.Count'` errors came from the cascades IR scalar fallback converting aggregate `Count` expressions into synthetic function calls. A guard in `TupleExprToIr` now keeps aggregate-valued `ExtensionElem`s as native tuple boundaries.
  Evidence: After the guard, `LmdbSubSelectDirectLookupEstimateTest` changed from `Unknown function Count` errors to two plan-estimate assertion failures and zero errors.

## Decision Log

- Decision: Vendor the zip OmniSketch implementation under RDF4J internals rather than relying on the interim local Omni sketch.
  Rationale: The user explicitly required the implementation in `/Users/havardottestad/Downloads/OmniSketchDataSketchesContribution.zip`; RDF4J package names and the local `OmniSketchFamily` are the compatibility glue.
  Date/Author: 2026-06-09 / Codex.
- Decision: Add `OMNI("omni")` as a first-class `SketchStrategy`, make it the default, and keep old strategy values accepted.
  Rationale: The user requested migrating away from current sketches. Existing values remain available for rollback and comparison.
  Date/Author: 2026-06-09 / Codex.
- Decision: Route `OMNI` through side-state sketches parallel to JoinSketch and Count-Min before replacing `FastAgmsBindingSummary`.
  Rationale: `SketchBasedJoinEstimator` exposes many methods typed to `FastAgmsBindingSummary`. A smaller side-state migration produces visible join-estimation improvements without a risky whole-file rewrite.
  Date/Author: 2026-06-09 / Codex.
- Decision: Treat benchmark acceptance as a performance gate, not only a correctness gate.
  Rationale: The current user goal requires `ThemeQueryPlanRunBenchmark.runQuery` plans to use Omni and to beat May/June averages. Passing unit tests is necessary but insufficient.
  Date/Author: 2026-06-09 / Codex.

## Outcomes & Retrospective

Partial implementation exists. Queryalgebra is green with 1132 tests passing after the Omni migration and aggregate fallback guard. The next milestone is LMDB correctness: the remaining red tests must be classified into true regressions versus old exact-plan expectations, and true regressions must be fixed before benchmark claims are made.

## Context and Orientation

The sketch estimator core is `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`. It builds summaries over statement components: subject (`S`), predicate (`P`), object (`O`), and context (`C`). A “join surface” is the bag of values for one join variable after applying pattern constants or previously joined factors; estimating the intersection of two join surfaces gives a duplicate-aware join cardinality.

The existing estimator stores `FastAgmsBindingSummary` in global, single-component, and pair-component arrays. It also maintains optional side-state arrays for `JoinFrequencySketch` and `CountMinFrequencySketch`. `OmniFrequencySketch` already implements `FrequencySketch`, but is not currently selected by `SketchStrategy` or persisted. LMDB wires the estimator config in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` and validates config values in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java`.

The local contribution zip is `/Users/havardottestad/Downloads/OmniSketchDataSketchesContribution.zip`. Its sources have been copied into `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/omni`. Its `OmniSketches.intersection(...)` API demonstrates the desired semantics: build one sketch per predicate dimension, convert predicates to summaries, then estimate the intersection of the summaries.

## Plan of Work

First, complete the correctness migration. The existing failing LMDB tests are the active red suite. Start with `LmdbSubSelectDirectLookupEstimateTest`, `LmdbEvaluationStatisticsMemoizationTest`, `LmdbDistinctCursorSkipTest`, and the cascades context/pipeline tests because they expose whether Omni estimates are changing logical row-flow semantics or only changing plan costs. Preserve tests that catch real over-counting, direct-lookup double-counting, scope leaks, or wrong query results.

Second, prove Omni is actually used in plans. Add or extend tests to assert planner telemetry on relevant joins contains `omni-sketch-surface` or an equivalent Omni source string for supported one-variable and two-variable surfaces. Unsupported surfaces must continue to use existing fallbacks.

Third, run the theme query benchmark with the benchmark wrapper. Use focused single-query runs for triage and a full `ThemeQueryPlanRunBenchmark.runQuery` run for acceptance. Parse the fresh result against the May/June historical files under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results`.

Fourth, iterate on estimators, cascades rules, and physical planner cost selection. Changes must be general and data-driven: no theme-name or query-index special cases. Each behavior-changing planner fix needs a focused red test or a failing existing test before production edits.

## Concrete Steps

1. Run the focused red test for config:

    Working directory: repository root.
    Command:

        python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorConfigTest#defaultSketchStrategyIsOmni --retain-logs

    Expected before implementation: Maven test failure because the test method or expected `OMNI` enum value does not exist yet.

2. Add tests:

    Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimatorConfigTest.java`.
    Rename or replace `defaultSketchStrategyIsFastAgms` with `defaultSketchStrategyIsOmni`, asserting `SketchStrategy.OMNI`.
    Extend `builderSelectsEachSketchStrategy` unchanged so it automatically includes `OMNI`.
    Add `omniStrategyAllocatesOmniSideState`, mirroring `onlyJoinSketchStrategyAllocatesJoinSketchSideState`, and assert a reflective `omniSketchCount(estimator) > 0` for `OMNI`.

    Modify `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchJoinSketchAccuracyComparisonTest.java`.
    Add a test named `omniStrategyEstimatesCompositeKeyJoinSurface` that builds a two-variable shared join using existing `skewedCompositeKeyJoin`, configures `SketchStrategy.OMNI`, calls `estimateSketchJoinSurface(List.of(left, right), "encounter")` and `estimateSketchJoinSurface(List.of(left, right), "drug")`, and asserts finite positive estimates with source `omni-sketch-surface`.

3. Run red tests:

    Command:

        python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorConfigTest#defaultSketchStrategyIsOmni --retain-logs

    Expected: FAIL because `OMNI` is not defined or the default remains `FAST_AGMS`.

    Command:

        python3 .codex/skills/mvnf/scripts/mvnf.py SketchJoinSketchAccuracyComparisonTest#omniStrategyEstimatesCompositeKeyJoinSurface --retain-logs

    Expected: FAIL because `OMNI` is not defined or source is not `omni-sketch-surface`.

4. Implement minimal code:

    Modify `SketchBasedJoinEstimator.java`:
    Add `OMNI("omni")` to `SketchStrategy`.
    Change `Config.sketchStrategy` default to `SketchStrategy.OMNI`.
    Add `SKETCH_PAYLOAD_FORMAT_OMNI`, `OMNI_SKETCH_PAYLOAD_MAGIC`, and `OMNI_SKETCH_PAYLOAD_VERSION`.
    Add `OmniSketchByEntry` inside `State`, with `get`, `getOrCreate`, `set`, `clear`, and capacity growth.
    Add `omniSketchForRead`, `omniSketchForWrite`, and `estimateOmniSketchNetInnerProduct` helpers parallel to JoinSketch helpers.
    Update side-state ingestion to create Omni sketches when `state.sketchStrategy == SketchStrategy.OMNI`.
    Update side-state persistence to write/read Omni sketches.
    Update `joinSingles` and `joinPairs` to prefer Omni side-state for `OMNI` when tombstones are absent.
    Update `estimateSketchJoinSurface` source for Omni to `omni-sketch-surface`.

    Modify `OmniFrequencySketch.java`:
    Add package-private `byte[] toByteArray()` and `static OmniFrequencySketch fromByteArray(byte[])`.
    Persist rows, buckets, support sample count, cell sample count, seed, support hashes/counts, and cell state. If direct cell serialization is intrusive, persist replayable weighted support and total sketch config for this milestone and document that persisted Omni side-state is a lossy support-sample snapshot.

    Modify `OmniSketch.java` and `OmniSketchCell.java` only if exact cell serialization is needed. Keep each new helper small and package-private.

5. Update LMDB config:

    Modify `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java` so the default `sketchEstimatorStrategy` is `"omni"` and validation message includes `omni`.
    Modify `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfigTest.java` so accepted values include `"omni"` and default assertions expect `"omni"`.
    Modify benchmark defaults only where a test asserts `"fastagms"` as a default.

6. Run green tests:

    Commands:

        python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorConfigTest --retain-logs
        python3 .codex/skills/mvnf/scripts/mvnf.py SketchJoinSketchAccuracyComparisonTest --retain-logs
        python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStoreConfigTest --retain-logs
        python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSubSelectDirectLookupEstimateTest --retain-logs

    Expected: all selected tests pass; Surefire reports show failures 0. `LmdbSubSelectDirectLookupEstimateTest` is currently red with two plan-estimate assertion failures after the aggregate fallback guard.

7. Format and broader verify:

    Commands:

        cd scripts && ./checkCopyrightPresent.sh
        mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
        python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
        python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

    Expected: copyright check passes, formatter completes, both module verifies pass. The formatter command uses `-q` as allowed by the repository formatting rule; test commands do not use `-q` or `-am`.

8. Run benchmark acceptance:

    Command:

        ./scripts/run-single-benchmark.sh --theme-plan-run --warmup-iterations 1 --measurement-iterations 2 --forks 1 --jvm-arg -Xmx8G

    For focused triage, add `--theme-query THEME:INDEX`, for example `--theme-query SOCIAL_MEDIA:7`.

    Expected: JMH prints `ThemeQueryPlanRunBenchmark.runQuery` rows. Query plans printed or captured during the run must show supported join surfaces using Omni estimate sources. Parse the result against May/June result files and continue planner work until all target rows beat the chosen average.

## Validation and Acceptance

Acceptance requires:

The red evidence exists before production changes for behavior-changing tests. The focused green evidence then shows the same tests pass. `SketchBasedJoinEstimator.Config.defaults().sketchStrategy` is `OMNI`. `SketchBasedJoinEstimator.SketchStrategy.fromConfigValue("omni", FAST_AGMS)` returns `OMNI`. LMDB `LmdbStoreConfig` exports and parses `"omni"`. A skewed composite-key join test gets an Omni-sourced join estimate through the public estimator surface.

Full acceptance additionally requires a green `core/sail/lmdb` verify, query-plan evidence showing Omni sources for supported multi-predicate and multi-variable join surfaces, and fresh `ThemeQueryPlanRunBenchmark.runQuery` results that beat the agreed May/June average for every measured query.

## Idempotence and Recovery

All changes are ordinary source edits and can be repeated. If Omni persistence proves too broad, keep `OMNI` side-state in memory only, leave snapshot reading disabled for `OMNI`, and document the limitation in `Outcomes & Retrospective` before final handoff. If a test exposes planner regressions unrelated to Omni routing, revert only the local Omni changes and preserve user or other-agent files.

## Artifacts and Notes

Initial build evidence:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    Result: BUILD SUCCESS
    Total time: 28.928 s

Web/dependency note:

    Search result observed on 2026-06-09: apache/datasketches-java latest release listed as 9.0.0 for Java 25. The Omni package was not visible in public search results; the local zip remains the design reference.

## Interfaces and Dependencies

New or modified internal interfaces:

In `SketchBasedJoinEstimator.SketchStrategy`, add:

    OMNI("omni")

In `OmniFrequencySketch`, add package-private serialization:

    byte[] toByteArray() throws IOException
    static OmniFrequencySketch fromByteArray(byte[] payload) throws IOException

In `SketchBasedJoinEstimator`, add private side-state methods:

    private OmniFrequencySketch omniSketchForRead(State state, SketchAddress address)
    private OmniFrequencySketch omniSketchForWrite(State state, SketchAddress address)
    private double estimateOmniSketchNetInnerProduct(State state, SketchAddress leftAdd, SketchAddress rightAdd, SketchAddress leftDelete, SketchAddress rightDelete)

Expected estimate source string:

    omni-sketch-surface
