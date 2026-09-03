# Restore complete LMDB Theme query performance

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

The LMDB Theme benchmark is a 130-query matrix covering aggregation, joins, optional branches, wildcard predicates, filters, and high-cardinality graph shapes. The newest run produces only 119 scores: six configurations time out and five report an unexpected row count. Some successful queries are also much slower than their earlier executions. The goal is not to whitelist these benchmark cases. The goal is to repair the shared planning, adaptive strategy selection, and native execution mechanisms so every query returns the catalogued result without timing out and no query has a statistically credible regression against its best comparable historical execution.

Completion is observable by running the complete `ThemeQueryBenchmark` matrix with exact direct adjacency ready before measurement. All 130 configurations must produce result rows. The comparison report must show no non-overlapping error-interval regressions against the comparable history, and any remaining noisy point deltas must retain an equal-or-better structural route and work count. Correctness and focused LMDB tests must pass before benchmark claims are accepted.

## Progress

- [x] (2026-09-03 13:43Z) Compared the September 2, September 3, and ready-state September 3 reports and classified the eleven remaining failures.
- [x] (2026-09-03 13:43Z) Confirmed the prior NPE, readiness, and authoritative adjacency-aggregate fixes with focused tests and a 119-score rerun.
- [ ] Establish the historical per-query baseline, current route, and structural-work inventory for all 130 configurations.
- [x] (2026-09-03 17:12Z) Excluded a simple repetition leak by executing ANALYTICS Q7 512 times in one test-owned repository; every execution stayed bounded on `irAggregateTypeMatrixParallel`.
- [x] (2026-09-03 19:02Z) Reproduced ANALYTICS Q7's unsafe adaptive transition at the arbitration-policy boundary, added deterministic coverage, and removed the unbounded under-confirmed trial escape.
- [x] (2026-09-03 19:02Z) Re-profiled ANALYTICS Q7 after repair; it completes in 478.126 ms instead of reaching the 50-second benchmark limit.
- [x] (2026-09-03 19:04Z) Profiled SOCIAL_MEDIA Q12 and ENGINEERING Q12 and isolated shared empty-interner classification plus per-row projection-layout work.
- [ ] Add focused invariants for the empty-context and projection-layout fast paths.
- [ ] Reproduce and classify the other five timeouts and five row-count failures by shared physical-plan mechanism.
- [ ] Implement general planner or execution fixes for each remaining shared mechanism, with focused tests before behavior-changing production edits.
- [ ] Run focused, neighboring, and complete LMDB verification without modifying unrelated worktree state.
- [ ] Run the matched 130-query Docker/JDK 26 matrix, compare it with history, and iterate until the acceptance bar is met.

## Surprises & Discoveries

- Observation: enabling exact direct-adjacency readiness and repairing the structural aggregate preference reduced the common-query geometric mean to 0.532 times the first September 3 run and produced eleven error-interval-separated improvements with no separated regressions.
  Evidence: `results-2026-09-03-2.md` contains 119 scores, including ANALYTICS Q11 at 0.105 ms/op on `adjacencyAggregate`, versus 2779.677 ms/op on `orderedDistinctGroups` in `results-2026-09-03.md`.

- Observation: ANALYTICS Q7 is not a startup-rebuild problem. It times out after direct adjacency is reported exact and after two successful warmup iterations.
  Evidence: the ready-state run records 254.272 ms/op, then 15.679 ms/op, then a 50-second timeout during warmup iteration three. The first September 3 run completes all five warmups and times out on measurement iteration one. This discontinuity is consistent with a stateful adaptive trial choosing a catastrophic strategy, but the failing iteration emits no final telemetry, so the selected arm remains to be proven.

- Observation: the eleven remaining failures fall into two visible classes. Six are query timeouts and five are deterministic result-count mismatches.
  Evidence: timeouts occur for SOCIAL_MEDIA Q11/Q12, LIBRARY Q12, HIGHLY_CONNECTED Q11/Q12, and ANALYTICS Q7. Row-count mismatches occur for LIBRARY, ENGINEERING, TRAIN, ELECTRICAL_GRID, and PHARMA Q11.

- Observation: ANALYTICS Q7 remains fast through 512 consecutive executions when its repository and cost model contain only that query's observations.
  Evidence: `ThemeQueryBenchmarkSmokeIT#analyticsTypeMatrixRemainsBoundedAcrossAdaptiveRepetitions` passed in 10.443 seconds; final telemetry selected `irAggregateTypeMatrixParallel` with 15 workers and 368 partitions. The benchmark timeout therefore requires cross-query model state, fork sequencing, or both.

- Observation: the adaptive policy has a deterministic unbounded escape hatch at exactly the boundary implicated by Q7. If a known winner is faster than the 500 microsecond minimum probe deadline, `maybeProbe` declines, `probeStructurallyImpossible` reports true, and `dispatchWithExploration` may dispatch an under-confirmed IR arm as an ordinary unbounded execution.
  Evidence: `LmdbNativeAdaptiveArbitration.dispatchWithExploration`, `probeStructurallyImpossible`, and `LmdbNativeStrategyPreference.allowsUnboundedMandatoryTrial` compose this path. A focused policy regression is being added before changing production code.

- Observation: the adaptive repair removes the latest ANALYTICS Q7 timeout in a matched Docker/JFR run.
  Evidence: `profiles/lmdb/latest-analytics-q7.jfr` records 478.126 ms/op with exact synchronous adjacency ready; the latest matrix run had reached the 50-second query limit.

- Observation: OPTIONAL-heavy Q12 execution repeatedly classifies ordinary store ids through empty concurrent runtime maps. ENGINEERING Q12 spends 523 of 1,324 query-thread CPU samples in `NativeExecutionContext.contains`, reached from `LexicalLeftJoinCursor.sharedKeyMatches`; SOCIAL_MEDIA Q12 spends 1,176 of 4,897 samples in `NativeExecutionContext.valueOf` while detaching projected rows.
  Evidence: `profiles/lmdb/latest-engineering-q12.jfr` and `profiles/lmdb/latest-social-q12.jfr`. Both paths have a per-evaluation context even when the query has not interned a runtime or query-scoped value, so the current implementation boxes and probes `ConcurrentHashMap` for every ordinary store id.

- Observation: SOCIAL_MEDIA Q12 also spends 476 of 4,897 query-thread samples re-running duplicate projection-name discovery in every `NativeProjectedBindingSet` constructor.
  Evidence: the JFR parent stack is `String.equals -> NativeProjectedBindingSet.indexOf -> hasDuplicateNames -> <init> -> NativeRowsStep.project`; the target names and source slots are immutable compiled-step metadata.

- Observation: after compiled projection layouts and batched value resolution removed the original projection scan, SOCIAL_MEDIA Q12 still reached the 50-second limit while its query worker averaged only 5.97% user CPU and the paged-CSF build pool consumed most available CPU.
  Evidence: `profiles/lmdb/latest-social-q12-batched.jfr` contains concurrent `lmdb-paged-csf-build-*` activity throughout the measured query even though the benchmark readiness check had returned in 0.126 seconds.

- Observation: the synchronous-maintenance option defaulted to true but did not activate for a populated store until its first exact cutover, so `triggerBuild()` returned immediately and let benchmark queries race the initial adjacency build.
  Evidence: the pre-fix focused test `LmdbDirectAdjacencyCommitTest#defaultMaintenanceWaitsForPopulatedStartupBuild` failed because `synchronousUpdatesActivatedForTest()` was false while the build was paused.

## Decision Log

- Decision: use Routine D and treat the work as a query-engine recovery program rather than independent benchmark patches.
  Rationale: the failures span adaptive selection, aggregation, optional-heavy joins, and result-shape validation. A general solution may change planner or execution architecture and must be validated across composition boundaries.
  Date/Author: 2026-09-03 / Codex.

- Decision: accept a performance result only when result count, plan/route telemetry, structural work, and repeated timing agree.
  Rationale: three-iteration JMH error intervals are wide, and a fast point estimate can hide an invalid result or a lucky adaptive arm. Structural counters make the result portable beyond one host run.
  Date/Author: 2026-09-03 / Codex.

- Decision: preserve the dirty checkout and make no branch, staging, commit, cleanup, reset, or stash operation unless explicitly requested.
  Rationale: many tracked and untracked LMDB files are user-owned work that overlaps the engine under investigation. Each edit must be path-scoped and reconciled with the existing implementation.
  Date/Author: 2026-09-03 / Codex.

- Decision: use bounded exploration at the execution boundary rather than query-name checks, elapsed-time sleeps, or benchmark-only exclusions.
  Rationale: a generally safe adaptive engine must not let a speculative strategy consume an unbounded amount of work relative to a known-valid incumbent. Query-specific exceptions would leave the same failure available to arbitrary user queries.
  Date/Author: 2026-09-03 / Codex.

- Decision: treat "incumbent too fast for a useful probe" as a reason to retain the incumbent, never as proof that an unbounded trial is safe.
  Rationale: a below-floor deadline says the expected value of exploration cannot justify even the probe mechanism's minimum overhead. Removing the deadline reverses that safety conclusion and exposes every fast query to arbitrary speculative latency.
  Date/Author: 2026-09-03 / Codex.

- Decision: after the user's scope correction, treat deterministic Q11 row-count mismatches as outside this performance pass; retain them as known correctness work but do not use an external oracle or mix them into the performance acceptance loop.
  Rationale: the current request is specifically the latest Theme benchmark's performance differences. The active work covers completed-query regressions and timeout failures, including Q11 only where it times out.
  Date/Author: 2026-09-03 / Codex.

- Decision: make default synchronous direct-adjacency maintenance operational at startup, not merely configured true.
  Rationale: `LmdbDirectAdjacencyOptions.fromSystemProperties` defaults the option to true, but the activation gate kept populated initial builds asynchronous. Activation now occurs under the transaction fence before build submission and `triggerBuild()` waits for that build; asynchronous startup remains available only through an explicit false setting.
  Date/Author: 2026-09-03 / Codex.

## Outcomes & Retrospective

The first milestone retained the previous grouped-query NPE repair and removed asynchronous direct-adjacency construction from steady-state measurements. The ready-state rerun recovered four formerly failed configurations and all statistically separated score regressions. The remaining work starts with ANALYTICS Q7 because it supplies the clearest evidence of a catastrophic stateful strategy transition. This section must be updated after each shared failure class is repaired and again after the complete matrix passes.

## Context and Orientation

The Maven module is `core/sail/lmdb`. `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java` loads generated datasets for ten themes and runs catalogued queries selected by `themeName` and `z_queryIndex`. `ThemeQueryCatalog.java` owns query text and expected result counts. `ThemeQueryBenchmark.executeQuery()` applies a 50-second execution limit, evaluates the query, and rejects an unexpected row count.

Native row and aggregate execution is lowered under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`. `LmdbNativeStrategyArbiter` and `LmdbNativeAdaptiveArbitration` choose among physical strategies using persisted cost observations. A physical strategy is an implementation such as a direct adjacency aggregate, a parallel type-matrix aggregate, generated or interpreted native IR, or the general nested-loop fallback. An incumbent is the currently preferred measured strategy. Exploration means deliberately trying an insufficiently measured alternative so the model can learn whether it is better.

The key danger is unbounded exploration: a candidate whose applicability is known but whose runtime is not bounded can replace a successful millisecond-scale incumbent and run until the outer 50-second query timeout. A safe design needs a work or time budget derived from the incumbent and must be able to decline or cancel the speculative attempt without corrupting query state. Bounded probes already exist in parts of the arbiter; the investigation must determine why Q7 bypasses them or why the chosen operator does not cooperate with their budget.

Historical reports are under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results`. `results-2026-09-02.md` contains thirteen ANALYTICS results. `results-2026-09-03.md` attempted the full matrix without an exact-adjacency wait. `results-2026-09-03-2.md` attempted the full matrix with exact adjacency and contains 119 successful scores. The previous focused work is documented in `.agent/execplans/lmdb-2026-09-03-query-regression-forensics.md`; this plan incorporates its relevant outcomes rather than depending on undocumented thread context.

## Plan of Work

First, build a machine-readable inventory of every attempted theme/query pair. For each pair record success or failure, score and uncertainty, logical query shape, optimized physical plan, selected native execution path, adaptive decision reason, rows scanned or decoded, materialized rows, worker count, and the fastest comparable historical result. Group failures and regressions by physical mechanism rather than by theme name. Repair the history parser if its current parameter grammar cannot read the Janino-enabled report format, with focused parser tests because this tooling is part of the acceptance loop.

Second, isolate ANALYTICS Q7 in a deterministic regression test that reuses the same store and executes enough repetitions to cross the failing adaptive transition. Add diagnostic capture at the strategy boundary if necessary so a cancelled or failed evaluation records the selected family and consumed work before propagating the exception. The initial test must retain the existing correctness assertion and use a bounded execution limit; it must fail before production repair. Determine whether the failure comes from mandatory cold-start rescue, evidence re-arming, persisted observations, loss of a type-matrix specialist, or failure to propagate an existing probe budget into a blocking aggregate.

Third, design and implement bounded adaptive exploration as a shared contract. An alternative may execute without a probe only when it is structurally bounded, answers the complete query from exact metadata, or has a credible upper cost close to the incumbent. Otherwise it must run under a cooperative row/work/time budget and fall back to a fresh exact incumbent state if the budget is exhausted. The implementation must preserve semantic results, cancellation, filter ownership, runtime telemetry, and learned cost accounting. Tests must cover joins, grouped aggregation, nested optional branches, generated and interpreted IR, parallel operators, candidates that decline, and candidates that exceed the probe budget.

Fourth, reproduce the other five timeouts one at a time with the smallest reusable Theme regression selectors. Compare their plans with the closest successful historical run and profile representative mechanisms in Docker/JFR. Optimize shared asymptotic or execution-model bottlenecks before constant factors: eliminate repeated full scans, prefer exact metadata aggregates, preserve type-matrix and predicate-plane specialists, partition only when work is sufficiently large, and avoid materializing explosive optional products where factorized or existence-based execution can preserve semantics.

Fifth, classify the five Q11 row-count mismatches. Verify the SPARQL result shape and catalogued expectation independently of the benchmark counter. If the catalog expects one row for a non-aggregate projection, fix the catalog and its tests. If the query is intended to aggregate but lowering dropped a grouping or projection boundary, add a semantic regression test and repair the shared lowering rule. Never make the benchmark accept a wrong engine result merely to produce a score.

Finally, run focused selectors, neighboring evaluation suites, and the complete LMDB module. Then run the full 130-query benchmark with exact adjacency ready, identical JDK, heap, warmup, measurement, and fork settings. Compare every result against its fastest semantically and operationally comparable history. Re-profile any remaining separated regression and repeat the test-first loop until all configurations pass the acceptance criteria.

## Concrete Steps

Run from `/Users/havardottestad/Documents/Programming/rdf4j`.

Use the repository-local Maven runner for focused tests. A typical selector is:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbThemeQueryRegressionTest#<method> --module core/sail/lmdb --retain-logs -- -Drdf4j.lmdb.themeRegression.persistentStore.enabled=true

Persist the first failing report for each behavior-changing mechanism in a task-specific evidence file so later runs cannot overwrite it. Do not pass `-am` or `-q` to test runs. The Maven runner performs the required root quick install before verification.

Capture a representative Docker/JFR profile through the benchmark main method:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh --module core/sail/lmdb --main-class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --main-arg --theme=ANALYTICS --main-arg --query-index=7 --jfr-output /tmp/theme-analytics-q7-2026-09-03.jfr

Read recordings with `jfr summary`, `jfr view cpu-time-hot-methods`, and focused stack/event queries described by the Docker/JFR skill. Use profiled runs for causal attribution and matched unprofiled runs for headline timings.

After each candidate change, rerun the identical focused selector. Before final handoff, run copyright checks, repository formatting, a root quick install, the complete LMDB suite, and the complete Theme matrix. Retain all logs and generated comparison reports.

## Validation and Acceptance

Correctness acceptance requires 130 attempted configurations, 130 successful result rows, no `QueryInterruptedException`, no `NullPointerException`, and no unexpected row-count failure. Each repaired mechanism must have a focused test that fails before its production change and passes afterward without weakened assertions.

Performance acceptance requires no query whose candidate confidence interval is wholly slower than its fastest comparable historical confidence interval. The geometric mean over the full comparable set must be equal to or lower than the strongest full-matrix baseline. A noisy point regression is acceptable only when intervals overlap, the candidate uses an equal-or-better structural path, and scanned/decoded/materialized work does not regress materially. Repeat or lengthen measurements when those conditions cannot be established from three iterations.

Architecture acceptance requires adaptive exploration to be bounded independently of the query text. A deliberately slow candidate must be cancelled or declined within its assigned budget, the valid incumbent must restart from fresh state, and the final result must remain exact. The same rules must hold for sequential, parallel, interpreted, and generated execution.

Verification claims must be separated into focused green, neighboring-suite green, complete-module green, benchmark complete, and unrun or blocked. A build with skipped tests is never described as test verification.

## Idempotence and Recovery

The focused tests, report parsing, query-plan extraction, and benchmark comparisons are repeatable. Persistent Theme stores may be reused only when their expected LMDB file-size markers match. JMH runs must be sequential because they share `/tmp/jmh.lock`.

Do not clean, reset, restore, stash, delete, or broadly stage this checkout. If another process modifies a touched file, inspect the overlap before editing. Apply all source changes through small path-scoped patches. If a behavior-changing production edit is accidentally made before a failing test is captured, revert only that exact patch and restart from the reproducer.

## Artifacts and Notes

The initial three-run comparison outputs are `/tmp/sep3-to-sep3-2.md` and `/tmp/sep2-to-sep3-2.md`. The earlier Q11, SOCIAL_MEDIA Q1, and MEDICAL_RECORDS Q5 JFR recordings remain under `/tmp/theme-*-2026-09-03.jfr`. New evidence files should use `.agent/evidence/lmdb-full-query-performance-recovery/` or a task-specific `initial-evidence.<name>.txt` without overwriting user artifacts.

## Interfaces and Dependencies

No new dependency is expected. Reuse `LmdbNativeStrategyArbiter`, `LmdbNativeAdaptiveArbitration`, `LmdbNativeAdaptiveCostModel`, `LmdbNativeAttemptMetrics`, the cooperative cancellation interfaces used by native operators, and the existing Theme query/catalog test utilities. A bounded exploration design should expose a small internal budget contract that can be consumed by row, group, and parallel operators without embedding benchmark knowledge.

Revision note (2026-09-03 17:12Z): 512 isolated Q7 executions stayed bounded, shifting the reproducer from invocation count to the cross-query adaptive-policy boundary; identified the below-floor-probe to unbounded-trial transition for test-first repair.

Revision note (2026-09-03 20:16Z): a post-materialization SOCIAL_MEDIA Q12 profile exposed the supposedly synchronous populated-store startup build running through the query window. Added a failing contract test, activated synchronous maintenance under the transaction fence before submission, and made explicit asynchronous tests opt out through configuration.
