# Recover LMDB theme-query correctness and performance without sacrificing existing gains

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

The LMDB native evaluator currently returns the wrong result for TRAIN query 5 and is materially slower than the July 13 implementation on nine theme queries. At the same time, the current branch is faster for most comparable queries and contains valuable ordered aggregation, finite VALUES, batch, chunk, and factorized execution work. This change repairs the shared compiler, ordering, and dependent-execution contracts rather than recognizing individual benchmark queries.

Completion is observable in three ways. TRAIN query 5 returns 24 results and agrees with generic RDF4J evaluation. The nine confirmed regressions no longer reproduce in matched JDK 26 measurements. Finally, the full 88-query theme suite shows no statistically confirmed slowdown relative to clean commit `f3cc68dfcc48e825a6e050475687cbc767e4f9c0`, while the established LIBRARY query 6 and HIGHLY_CONNECTED query 6 limits remain satisfied.

## Progress

- [x] (2026-07-16 17:28Z) Read `.agent/PLANS.md`, the supplied regression report, and the complexity, high-performance Java, Maven, query-plan, and JMH comparison workflows.
- [x] (2026-07-16 17:28Z) Confirmed a clean `optimize-lmdb` worktree at `f3cc68dfcc48e825a6e050475687cbc767e4f9c0`.
- [x] (2026-07-16 17:31Z) Ran the required clean root install; all reactor modules passed in 34.804 seconds.
- [x] (2026-07-16 17:39Z) Froze the f3 JDK 26 benchmark jar, 19 native plan snapshots, and fresh point timings for every regression, target gain, and normalized control before production edits.
- [x] (2026-07-16 17:43Z) Added and observed two independent correlated synthetic-threshold failures: native count 2 versus generic count 1.
- [x] (2026-07-16 17:48Z) Corrected expression slot ownership; focused, differential, full filter, and TRAIN 5 integration selections pass, including the exact result count 24.
- [x] (2026-07-16 17:51Z) Added the ordered-planner structural safety cases and observed the smallest VALUES-barrier failure before production edits.
- [x] (2026-07-16 18:01Z) Constrained ordered promotion with barriers, connectivity, earliest-filter, and saturating cumulative-work gates; all focused and gain-shape tests pass.
- [x] (2026-07-16 18:06Z) Added dependent bare-fragment route/lifecycle/concurrency tests and observed the focused foreign-base bulk-setup failure before production edits.
- [x] (2026-07-16 18:10Z) Implemented direct-or-bulk bare dispatch over the unified SlotPlan; focused, lifecycle, cleanup, and concurrent isolation tests pass.
- [x] (2026-07-16 18:38Z) Added and observed a structural failure: ten dependent probe flushes repeated the same revision-stable predicate-cardinality scan ten times.
- [x] (2026-07-16 18:40Z) Memoized the CSR predicate cardinality per slot/revision; the focused test now observes one read across ten under-amortized flushes.
- [x] (2026-07-16 18:42Z) Verified all eleven CSR cache tests and the exact HIGHLY_CONNECTED 10 structural theme path, which completed in 2.346 seconds after the pre-fix broad run remained compute-bound for more than nine minutes.
- [x] (2026-07-16 19:24Z) Profiled PHARMA 10 on candidate and July 13 artifacts and observed the failing dedicated-existence route test before any existence-path production edit.
- [x] (2026-07-16 19:38Z) Added the existence-specialized compact cursor over the existing SlotPlan; focused route, multiplicity, cleanup, and concurrency tests pass, and PHARMA 10 measured 3584.650 ms/op in the first matched repetition.
- [x] (2026-07-16 19:44Z) Added and observed the filtered-BGP EXISTS fallback: the focused test failed because `existsStep()` returned null before opening any cursor.
- [x] (2026-07-16 19:48Z) Added and observed the single-pattern filtered EXISTS fallback; its `FilterPlan` wrapper was rejected before cursor creation.
- [x] (2026-07-16 19:54Z) Added and observed the internal native `ExistsFilter` row-route failure: two range scans instead of one range scan and one boolean membership lookup.
- [x] (2026-07-16 20:02Z) Extended the shared boolean cursor across placeable and trailing filters plus internal native `ExistsFilter`; false, barrier, nesting-order, route, and cleanup contracts pass.
- [x] (2026-07-16 20:43Z) Formatted and verified the complete LMDB module: 1,888 tests passed, zero failures/errors, three skips.
- [x] (2026-07-16 20:43Z) Ran focused, differential, theme, module, formatting, copyright, and build verification.
- [x] (2026-07-16 20:55Z) Completed three alternating matched PHARMA 10 pairs on JDK 26; the candidate was 8.1-9.8% faster in every pair.
- [ ] Run the remaining targeted and full-suite performance acceptance and record the results.

## Surprises & Discoveries

- Observation: The clean starting commit includes the permanent native explanation work described by the supplied report.
  Evidence: `git rev-parse HEAD` returned `f3cc68dfcc48e825a6e050475687cbc767e4f9c0`, and `git status --short --branch` contained no changed files before this ExecPlan was created.

- Observation: The nine timing regressions split into an ordering family and a repeated dependent-execution family; the generic optimized algebra remained stable at the confirmed bad boundaries.
  Evidence: The supplied investigation records identical generic `structure+estimates` signatures but reports broad scans before finite VALUES for PHARMA 0/5 and SOCIAL_MEDIA 3/9, ordered-DISTINCT cursor churn for PHARMA 1, and unchanged semantic row/probe populations with much greater NativeRows setup for ELECTRICAL_GRID 8, PHARMA 10, HIGHLY_CONNECTED 10, and LIBRARY 2.

- Observation: TRAIN query 5's foreign threshold is expression input, not output of the recursively compiled bare fragment.
  Evidence: The query binds a synthetic `?threshold` outside `FILTER NOT EXISTS`; the child fragment produces `?service` and `?late`. Allocating a child-native threshold slot causes `NativeRowSeeder` to translate a foreign synthetic value through the child's native value source and reject it as unknown.

- Observation: The required pre-change reactor build is green on the active workspace and local Maven repository.
  Evidence: `maven-build.log` ends with `BUILD SUCCESS`, `RDF4J: LmdbStore ... SUCCESS [  4.594 s]`, and total wall-clock time `34.804 s`.

- Observation: The frozen f3 endpoint reproduces the intended correctness and performance state.
  Evidence: `/tmp/lmdb-theme-regression-recovery/f3cc68dfcc/benchmark/jmh-benchmarks.jar` has SHA-256 `abbd58ed5ef8608ba14b9c5fa5ea43619d5e23be480d2cff1d9c87dfeb18c807`. Fresh PHARMA point timings include query 5 at `45.464 ms/op` and query 10 at `7207.799 ms/op`. The TRAIN 5 capture fails with `expected 24 but got 94` after writing its native plan snapshot.

- Observation: The failure did not require changing seeding or translating foreign values into native ids.
  Evidence: Removing eager expression-slot allocation and passing a nonallocating existing-slot resolver to native expression shortcuts changes the focused native result from 2 to 1. The unchanged `NativeRowSeeder` continues to reject genuinely local bound pattern terms absent from the native dictionary.

- Observation: Ordered aggregate planning currently promotes a later pattern across a finite VALUES relation.
  Evidence: `LmdbNativeJoinOrderCrossProductTest#orderedDistinctDoesNotPromotePatternAcrossValues` failed with physical children `[PatternPlan, ValuesPlan]` where compiler order starts `[ValuesPlan, PatternPlan]`; Surefire reported one failure in 0.050 seconds before any ordered-planner production edit.

- Observation: The safe gain-producing promotions do not require crossing non-pattern operators or accepting greater cumulative work.
  Evidence: The nine-test structural planner class passes after the new gates, including its connected lower-work positive control. All nineteen `LmdbNativeOrderedDistinctTest` methods also pass, including `libraryUnionShapeOrdersMemberThenLoan` and `highlyConnectedUnionShapeOrdersNodeThenNeighbor`.

- Observation: A nonempty base binding outside the fragment's slot layout is still sent through the bulk dispatcher.
  Evidence: `LmdbNativeRowStepIterationTest#foreignBoundBareFragmentUsesDirectCursorWithoutBulkSetup` failed before production edits because the plan's direct `open` count remained zero and `openBatch` was used. Surefire reported one failure in 0.051 seconds.

- Observation: The dependent path needs no aggregate, DISTINCT, ordering, batching, factorization, chunk, or parallel state.
  Evidence: The direct iteration constructs only one `RowState`, opens the shared immutable `SlotPlan`, and snapshots each result. The focused structural test observes one `open`, zero `openBatch` calls, and unchanged batch/factorized/chunk/parallel counters. All eleven row-step tests pass, including failure cleanup and two concurrent evaluations with independent cursors.

- Observation: Closing many small dependent cursors can repeatedly trigger an O(predicate-range) cardinality scan in the CSR adaptive-build gate.
  Evidence: The broad LMDB verification spent more than nine compute-bound minutes inside `TripleStore.cardinality` from `LmdbCsrAdjacencyCache.recordProbes`, reached through `RetainedNativeProbe.close` for HIGHLY_CONNECTED query 10. Two thread dumps 132 seconds apart showed the same call chain while probe totals were still below the cache-build amortization threshold.

- Observation: Revision-scoped predicate-cardinality reuse removes the runaway close cost without changing cache behavior.
  Evidence: The focused counter test changed from ten cardinality calls to one; all eleven CSR content/order/invalidation/budget/concurrency tests pass; and `highlyConnectedQ10RunsAntiExistsBeforeWeightFanout` passes end to end in 2.346 seconds.

- Observation: PHARMA 10's remaining time is almost entirely its correlated three-pattern `EXISTS`, not dependent-step setup.
  Evidence: The matched candidate CPU run measured `6609.924 ms/op`; telemetry charged `6662.8M` nanoseconds of `6673.4M` expression nanoseconds to `Exists`, with `22.6K` native invocations on `bareDirect`. The CPU profile is dominated by LMDB range traversal, key comparison, `LmdbRecordIterator.reset`, and pattern bind/rollback work.

- Observation: Ordinary row enumeration must preserve cross-context multiplicity, but `EXISTS` only needs a boolean and may use direct membership probes for suffix patterns whose terms are already bound.
  Evidence: The July 13 artifact measured `3928.522 ms/op` under the same profiler run. Its monolithic native iterator calls `source.has` whenever a pattern produces no new binding; the current unified ordinary-row path intentionally scans when an unknown context could yield several quads. The new end-to-end test fails because PHARMA 10 reports `nativeExecutionPath=bareDirect` instead of the required existence-only route.

- Observation: The dedicated existence route recovers and slightly exceeds the July 13 PHARMA 10 result without changing its algebra or ordinary BGP routes.
  Evidence: The first matched JDK 26 repetition measured `3584.650 ± 137.768 ms/op`, versus the reported candidate `6578.413 ± 847.878 ms/op` and July 13 matched median `3863.048 ms/op`. Telemetry records `nativeExecutionPath=bareExists` on all 22.6K correlated invocations. Structural tests observe one `has` lookup for the fully bound suffix, retain three ordinary matching rows, close early cursors, and isolate concurrent evaluations.

- Observation: The final frozen artifact sustains the PHARMA 10 recovery across alternating matched repetitions on the requested JVM and harness settings.
  Evidence: Candidate repetition means were `3712.730`, `3594.029`, and `3701.103 ms/op`; the paired July 13 means were `4052.954`, `3983.922`, and `4027.983 ms/op`. Every pair favors the candidate by 8.1-9.8%. The frozen candidate jar has SHA-256 `937ff6b2f3dcca4e411fc1868b0531ca49370e6ee9bc343206840d8b83a5f9d1`.

- Observation: `NOT EXISTS` already receives the boolean route through its nested `Exists`, but a BGP carrying an internal filter is compiled to a `MultiJoinPlan` with filters and is currently rejected by the specialized cursor.
  Evidence: `NativeExistsPatternCursor.patterns` accepts only a single `PatternPlan` or a `MultiJoinPlan` whose `filters` array is empty. Filtered BGPs still have boolean-only result semantics and the immutable `MultiJoinPlan` already records each filter's earliest legal evaluation depth.

- Observation: Native aggregates have a second EXISTS boundary inside `ExistsFilter`, independent of `LmdbNativeEvaluationStrategy.prepare(Exists)`.
  Evidence: `LmdbNativeRowStepIterationTest#nativeExistsFilterUsesBooleanCursorForPatternJoin` failed with two statement-range scans and zero membership lookups. Its pattern-only subplan has the same boolean semantics and shape proof as the dedicated outer EXISTS step.

## Decision Log

- Decision: Treat this as Routine D because it changes three coupled performance-sensitive subsystems, while using a separate smallest failing test before each behavioral production milestone.
  Rationale: The work needs a restartable cross-cutting design, but test-first reproduction remains the safest way to prevent correctness or route regressions.
  Date/Author: 2026-07-16 / Codex.

- Decision: A native slot belongs only to a value produced or intentionally materialized by the compiled fragment.
  Rationale: Correlated expression variables already exist in the incoming `BindingSet`; assigning them a fragment-local numeric identifier crosses value-source identity domains and makes synthetic values appear absent.
  Date/Author: 2026-07-16 / Codex.

- Decision: Preserve the compiler's incoming child order as the fallback and allow ordered promotion only across a contiguous connected pattern prefix that is no more expensive under an order-sensitive work model.
  Rationale: A product of child cardinalities is order invariant and cannot distinguish a selective VALUES/filter prefix from a broad scan. Semantic barriers, connectivity, and cumulative probe work express the general safety condition without query names or tuned thresholds.
  Date/Author: 2026-07-16 / Codex.

- Decision: Split bare-fragment dispatch by whether the incoming `BindingSet` is empty.
  Rationale: Empty root-like evaluation can amortize bulk setup and retain batch/chunk/factorized gains. A nonempty dependent invocation already supplies correlation context and must not reconstruct batch or aggregate dispatch state on every call.
  Date/Author: 2026-07-16 / Codex.

- Decision: Do not pool mutable probes or cursors across `QueryEvaluationStep.evaluate` calls.
  Rationale: Query steps can be evaluated concurrently and do not own a lifecycle suitable for thread-local or shared mutable native cursors. Each evaluation owns and closes its cursor state; immutable compiled plans remain shared.
  Date/Author: 2026-07-16 / Codex.

- Decision: Make the CSR amortization gate remember predicate cardinality per slot and data revision.
  Rationale: Cardinality is invariant for a store revision and is needed only to decide when a two-pass cache sweep is paid back. Repeating its range scan for every evaluation-local probe close is pure duplicated work; revision-scoped reuse preserves the existing build threshold and cache semantics.
  Date/Author: 2026-07-16 / Codex.

- Decision: Compile native BGPs reached through `EXISTS` to an existence-specialized evaluator over the same immutable SlotPlan.
  Rationale: The boolean context makes duplicate result rows irrelevant, allowing fully bound suffix patterns to use exact membership probes and allowing a tight evaluation-local depth-first cursor. Ordinary bare-row evaluation remains unchanged and continues to preserve graph multiplicity. This is a general expression-context optimization, not a query or benchmark special case.
  Date/Author: 2026-07-16 / Codex.

- Decision: Extend the existence cursor to filtered pattern-only multi-joins, while retaining the existing fallback for VALUES, OPTIONAL, MINUS, extensions, projections, and other row-observing or unsupported plan shapes.
  Rationale: A filter changes which complete pattern solutions exist but does not make their multiplicity observable. Applying each existing compiled `MaskedFilter` at the `MultiJoinPlan`'s normal earliest legal depth preserves semantics and broadens the optimization to FILTER EXISTS/NOT EXISTS without expanding the compiler's algebra claim.
  Date/Author: 2026-07-16 / Codex.

- Decision: Reuse the same proven boolean plan inside native aggregate `ExistsFilter` instances.
  Rationale: Internal and strategy-level EXISTS have the identical truth-only contract. Caching the immutable extracted plan once lets both routes share the evaluation-local cursor, while unsupported child operators retain `ExistsFilter`'s ordinary row-cursor fallback.
  Date/Author: 2026-07-16 / Codex.

- Decision: Use statistical parity as the default acceptance rule, with a three-percent median slowdown triggering a rerun even when uncertainty intervals overlap.
  Rationale: The user did not select a stricter alternative. This rule rejects confirmed losses while avoiding a single noisy point estimate, and structural route/work tests prevent noise from hiding a known bad execution path.
  Date/Author: 2026-07-16 / Codex.

## Outcomes & Retrospective

All implementation milestones are complete. Foreign correlated expression variables remain in the incoming `BindingSet` instead of becoming child-native slots. The exact focused failure now passes, the independent differential case passes, all eight aggregate-filter semantics tests pass, and TRAIN query 5 passes its integration contract with 24 results. Ordered aggregation now preserves compiler fallback order and rejects promotions across barriers, delayed filters, Cartesian prefixes, or increased cumulative work, while the LIBRARY 6 and HIGHLY_CONNECTED 6 ordered gains remain structurally available. Dependent nonempty bare fragments now open one evaluation-local direct cursor; empty inputs retain the existing bulk dispatcher. Revision-stable CSR cardinality reuse removes repeated close-time scans. Pattern-only `EXISTS` and `NOT EXISTS`, including safely placeable filters and aggregate-internal existence tests, share one evaluation-local boolean cursor while row-observing operators retain the ordinary fallback. The complete LMDB module is green, and three matched PHARMA 10 pairs favor the candidate. Remaining broad performance acceptance is intentionally not claimed.

## Context and Orientation

The affected Maven module is `core/sail/lmdb`. Its package `org.eclipse.rdf4j.sail.lmdb.evaluation` compiles selected RDF4J tuple algebra into a native slot plan. A slot is an integer index in `RowState`, whose value is an LMDB-native numeric RDF identifier. A `SlotPlan` is immutable compiled execution structure; opening it creates mutable `RowCursor` objects that fill a row. The incoming RDF4J `BindingSet` is called the base binding set and can contain correlated values created by an outer query fragment.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregatePlanner.java` compiles aggregate roots and projection-less bare roots. Its `compileBareRoot` method currently builds a `NativeRowsStep`. `LmdbNativeAggregateFilterCompiler.java` compiles native and generic boolean expressions. Its current slot resolver allocates slots for expression variables, including foreign correlated inputs. `LmdbNativeSlotOrder.java` chooses native orders for grouping and DISTINCT. Its multi-join estimate is effectively order invariant, so the ranking can promote a broad scan ahead of a finite relation. `LmdbNativeRowStep.java` owns `NativeRowsStep` and `NativeRowsIteration`; the latter evaluates batch, parallel, factorized, chunk, DISTINCT, and ordered routes even for small recursively invoked bare fragments.

The key focused tests are `LmdbNativeAggregateFilterSemanticsTest`, `LmdbNativeDifferentialFuzzTest`, `LmdbNativeOrderedDistinctTest`, `LmdbNativeJoinOrderCrossProductTest`, `LmdbNativeStrategyPriorityTest`, `LmdbNativeRowStepIterationTest`, `LmdbNativeChunkPipelineTest`, and `benchmark/LmdbThemeQueryRegressionIT`. Query-plan snapshots and the theme JMH harness provide end-to-end evidence beyond unit tests.

The historical July 13 endpoint is `d8f0026bc0850f32ccbfe639e6863b22d263e5e1`. The current pre-change endpoint is `f3cc68dfcc48e825a6e050475687cbc767e4f9c0`. The supplied regression report and harness artifacts are under `/tmp/lmdb-theme-regression-2026-07-13-vs-16-first`; preserve them and create new timestamped result directories rather than overwriting historical evidence.

## Plan of Work

First capture the clean pre-change state. Run the required root quick clean install. Build an immutable benchmark artifact or isolated temporary Git worktree for `f3cc68dfcc`; do not rely on a later rebuild of the changing workspace. Capture native OPTIMIZED/EXECUTED/TELEMETRY plans for TRAIN 5, all nine timing regressions, LIBRARY 6, HIGHLY_CONNECTED 6, LIBRARY 8, and the normalized controls. Run the same JDK 26 flags and system properties as the supplied report. Save command lines, JVM identity, source/store checksums, raw JSON, and normalized comparison tables in `/tmp/lmdb-theme-regression-recovery`.

For correlated variable ownership, add the smallest failing test to `LmdbNativeAggregateFilterSemanticsTest`. It must bind an `xsd:time` threshold outside a correlated `NOT EXISTS`, compare an inner `?late` value to that threshold, and assert native and generic results match. Add an equivalent differential scenario and the exact TRAIN 5 ground truth of 24. Run the smallest method and retain its Surefire failure before editing production code. Then add a nonallocating existing-slot resolver in the aggregate compiler base/filter compiler. Pattern terms, VALUES outputs, extension targets, and aggregate slots continue to use the allocating resolver. Native expressions, generic booleans, list membership, ID operands, and comparison shortcuts use existing slots only; when a referenced variable is foreign they fall back through `RowBindingSetView` and the base binding set. Do not weaken `NativeRowSeeder`.

For ordered planning, add focused failures that model a finite VALUES seed followed by its consumer, a selective filter before an ordered DISTINCT anchor, a disconnected pattern that would create a Cartesian prefix, and a connected pure-pattern prefix that can safely expose ordered grouping. Tests must inspect the native physical child/index/filter order, not only the generic algebra or final rows. Implement a fail-closed order-sensitive work helper in `LmdbNativeSlotOrder`. Starting from the original child sequence and the currently bound slots, accumulate saturated estimated probe work and output multiplicity for each prefix. A promoted candidate is eligible only within a contiguous prefix containing plain pattern plans, when each displaced pattern joins the already bound/produced slots, when no placeable filter moves later, and when candidate cumulative work is finite and no greater than the original. VALUES, multi-value patterns, filters, extensions, and other operators are barriers. Keep existing deterministic candidate ranking after this eligibility check, and preserve existing LIBRARY 6/HIGHLY_CONNECTED 6 ordered-plan tests.

For dependent bare fragments, first add failing route tests. A nonempty base binding set containing a foreign correlated value must choose `bareDirect`, avoid `PatternBatchCursor`, `NativeBatch`, parallel, chunk, and factorized setup counters, and match generic results. An empty base must remain eligible for `bareBulk`. Cover lazy opening, early close, exception cleanup, repeat evaluation, and concurrent evaluations. Then introduce one package-private step alongside `NativeRowsStep` that shares the immutable compiled `SlotPlan`. For an empty base it delegates to the existing bulk step and reports `bareBulk`. For a nonempty base it creates one `RowState`, seeds local slots, opens the current `SlotPlan` cursor directly, and lazily returns snapshot binding-set views while reporting `bareDirect`. The direct path must not construct aggregate, DISTINCT, ordering, batch, parallel, factorized, or chunk state. All mutable row/cursor state belongs to that single returned iteration and is closed exactly once.

For boolean-context generalization, first add a filtered multi-pattern EXISTS test that compiles successfully but reports the ordinary row route before production edits. It must compare native and generic truth values for passing and rejecting bindings and prove the filter runs as soon as its inputs are bound. Then let the existence cursor retain the `MultiJoinPlan.OrderedPlan` and its `filterDepth` mapping, evaluate the immutable `MaskedFilter` objects after a pattern row is bound at each depth, and backtrack immediately on rejection. Keep every frame, row, and cursor evaluation-local. Do not broaden `compileBareRoot`'s algebra acceptance: VALUES, extensions, OPTIONAL, MINUS, projections, and service expressions continue through existing evaluators until a separate proof shows that their row semantics can be erased safely.

After each milestone, rerun the exact focused failing selection and preserve the passing report. Then run neighboring classes, native/generic differential cases, the relevant persistent-store theme methods, and the complete LMDB module. Run copyright validation before formatting; add `// Some portions generated by Codex` below the existing header in every edited Java file, without changing existing copyright years. Format and import-sort through the repository resource-processing command, then rerun focused and module verification.

Finally capture candidate plan snapshots and diff them against the immutable f3 baseline. Run targeted matched JMH comparisons first. If those pass, run all 88 queries against both the immutable f3 artifact and the candidate under alternating three-pair order. Parse raw JMH tables with the repository comparison tool. If a query fails the performance gate, preserve the run, use TELEMETRY and JFR/async-profiler evidence to identify the shared cursor/planner cost, update this ExecPlan, add the smallest structural regression test, and refine the shared implementation. Do not add query identifiers, benchmark detection, global feature switches, or special numeric cutoffs.

## Concrete Steps

All repository commands run from `/Users/havardottestad/Documents/Programming/rdf4j`.

The required initial build is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Store its full output in `maven-build.log` and show only errors plus the reactor summary. If offline dependency resolution fails, repeat the exact command once without `-o`, then return to offline operation. Do not use `-am` or `-q` on a test command.

Focused tests use the repository runner with persistent theme stores when applicable:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeAggregateFilterSemanticsTest#correlatedSyntheticThresholdUsesBaseBinding --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeOrderedDistinctTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeStrategyPriorityTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbThemeQueryRegressionIT#trainFiveRetainsCorrelatedThreshold --module core/sail/lmdb --retain-logs -- -Drdf4j.lmdb.themeRegression.persistentStore.enabled=true

Immediately after the first failing test run, persist compact evidence at repository root:

    python3 scripts/agent-evidence.py --command "<failed mvnf command>" core/sail/lmdb/target/surefire-reports core/sail/lmdb/target/failsafe-reports > initial-evidence.txt

Subsequent evidence may be appended, never replacing the first failure. Every implementation status update must state a description, quote a compact Surefire/Failsafe report snippet, and keep exactly one living-plan step in progress.

The query-plan wrapper is:

    ./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/lmdb-theme-regression-recovery/<name>.log -- --store lmdb --theme <THEME> --query-index <INDEX> --query-id <id>

Use the same query id for baseline and candidate, then compare structure and estimates. The native physical plan and execution-path metrics are additional acceptance evidence because the semantic comparator intentionally ignores native metric strings.

The matched JMH configuration is Zulu JDK 26+35, JMH 1.37, one thread, one fork, `-Xms1G -Xmx16G`, four three-second warmups, five three-second measurements, and three paired repetitions with endpoint order alternated. Set `rdf4j.lmdb.csrCache.enabled=false` and `rdf4j.lmdb.parallel.rangePartition.enabled=false` for comparability with the supplied report. Reuse `/tmp/lmdb-theme-regression-2026-07-13-vs-16-first/run-matched-endpoints.sh` only after inspecting its arguments and output paths; copy or parameterize it in `/tmp` rather than editing repository-tracked benchmark behavior.

After code changes, validate source headers and format:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is not a test command; test commands remain free of `-q` and `-am`. Finish with a clean root install using the initial command.

## Validation and Acceptance

TRAIN 5 returns exactly 24. The focused synthetic-threshold test and differential test agree between native and generic evaluation for present, absent, unknown-to-LMDB, and unbound outer values. Local bound values that are absent from the LMDB dictionary still reject native pattern matches, proving `NativeRowSeeder` was not weakened.

PHARMA 0/5 and SOCIAL_MEDIA 3/9 retain finite VALUES placement before the corresponding broad consumer scan. PHARMA 1 does not select an ordered-DISTINCT route that increases cumulative work or delays its selective filter. A disconnected promotion is rejected; connected LIBRARY 6 and HIGHLY_CONNECTED 6 order proofs remain selected. Every native result matches generic evaluation.

A dependent nonempty bare-fragment call reports `bareDirect`, opens one direct slot-plan cursor, and does not engage batch, parallel, chunk, factorized, DISTINCT, or ordered setup. An empty base remains eligible for `bareBulk`. Repeated and concurrent evaluations have isolated mutable state, early close releases native cursors, and exceptions do not leak resources.

All focused tests, differential native tests, theme regression selections, and the complete `core/sail/lmdb` module pass. Formatting, copyright validation, `git diff --check`, and the final root quick clean install pass.

For timing, all result counts must validate before comparing latency. None of the nine confirmed regressions may have a candidate confidence interval separated in the slower direction from July 13. A median slowdown of at least three percent with overlapping intervals is inconclusive and is rerun. The full candidate suite must contain no statistically confirmed slowdown against immutable f3. Existing gains remain, including LIBRARY 6 at or below 3635 ms/op and HIGHLY_CONNECTED 6 at or below 502 ms/op; LIBRARY 8 and the normalized controls must not show a confirmed loss. If any gate fails, completion is not claimed.

## Idempotence and Recovery

Inspection, builds, snapshots, focused tests, and benchmarks are safe to repeat when new timestamped output directories are used. Preserve all untracked artifacts and historical `/tmp` evidence. Do not use destructive Git commands, overwrite tracked files from an old commit, manually stash, or change branches. Historical source needed for comparison must be built in `/tmp` or an isolated worktree.

If a focused test unexpectedly passes before production changes, strengthen the structural assertion or select the actual compiled native path; do not manufacture a failure unrelated to the reported contract. If a production edit is made before its behavioral failure is observed, stop, revert only that new edit with an explicit patch, and restart the milestone from its failing test. If the module cannot resolve an offline dependency, retry once online as described above. If a benchmark is noisy, retain it and rerun the identical pair in alternating order.

## Artifacts and Notes

The supplied report identifies historical July 13 medians in ms/op: PHARMA 5 `0.335`, PHARMA 0 `0.263`, SOCIAL_MEDIA 3 `0.166`, ELECTRICAL_GRID 8 `11.325`, HIGHLY_CONNECTED 10 `6650.385`, SOCIAL_MEDIA 9 `5.160`, PHARMA 10 `3863.048`, LIBRARY 2 `38.299`, and PHARMA 1 `0.822`. These values orient the investigation but do not replace matched candidate measurements.

The report attributes ELECTRICAL_GRID 8 allocation pressure primarily to `long[]` allocated by `PatternBatchCursor`; HIGHLY_CONNECTED 10 also allocates record iterators, pattern cursors, NativeBatch, ScanStage, and factorized rows. PHARMA 5 and PHARMA 1 spend more time acquiring/releasing LMDB cursors after ordered promotion. Use these profiles to validate mechanism, not as permission to specialize by theme or query number.

## Interfaces and Dependencies

No public RDF4J API, SPARQL behavior, storage format, serialized form, or external dependency changes. New helpers and steps remain package-private in `org.eclipse.rdf4j.sail.lmdb.evaluation`.

The filter compiler gains a nonallocating existing-slot lookup for expression inputs. The ordered planner gains a package-private saturated cumulative-work calculation and eligibility predicate used only to reject unsafe ordered promotions. Bare-root compilation returns an immutable package-private direct-or-bulk step whose `evaluate(BindingSet)` method owns all mutable direct-iteration state. Diagnostic `nativeExecutionPath` labels add `bareDirect` and `bareBulk` while preserving the existing bounded explanation contract.

No branch, commit, push, or pull-request operation is part of this work unless separately requested. If commits are later requested without an issue number, use the repository's `GH-0000` prefix.

Plan revision note (2026-07-16 17:28Z): created the initial self-contained execution plan from the approved design, clean f3 starting point, supplied regression report, and repository execution/testing requirements.

Plan revision note (2026-07-16 17:31Z): recorded the successful mandatory clean root install before tests or production edits.

Plan revision note (2026-07-16 17:39Z): recorded the immutable Zulu 26 f3 benchmark artifact, complete target/control plan set, fresh point timings, and reproduced TRAIN 5 failure. Full paired statistical runs remain a final acceptance milestone using the frozen jar.

Plan revision note (2026-07-16 17:48Z): recorded the failing-then-passing correlated-variable evidence, the nonallocating slot-ownership fix, and restored TRAIN 5 correctness.

Plan revision note (2026-07-16 17:51Z): recorded the pre-production ordered-planner tests and the focused proof that current ordered promotion crosses a VALUES barrier.

Plan revision note (2026-07-16 18:01Z): recorded the ordered-planner root fix, complete structural test pass, and retained LIBRARY 6/HIGHLY_CONNECTED 6 gain-shape contracts.

Plan revision note (2026-07-16 18:06Z): recorded the pre-production dependent bare-fragment route, lifecycle, and concurrency tests plus the focused proof of bulk setup for a foreign nonempty base.

Plan revision note (2026-07-16 18:10Z): recorded the unified direct-or-bulk bare dispatcher and passing focused, lifecycle, cleanup, and concurrency evidence.

Plan revision note (2026-07-16 19:54Z): broadened the boolean-context milestone to the native aggregate's internal `ExistsFilter` after a structural failing test proved it still enumerated an existence-only suffix through the ordinary row route.

Plan revision note (2026-07-16 20:02Z): completed boolean-context generalization for pattern-only plans. Filters retain earliest legal evaluation and ordinary nesting-order cleanup; VALUES and other row-producing operators remain explicit fallbacks.

Plan revision note (2026-07-16 20:43Z): recorded the complete LMDB module pass after formatting: 1,888 tests, zero failures/errors, three skips, including the full theme/snapshot integration set.

Plan revision note (2026-07-16 20:55Z): recorded the final frozen artifact and three alternating JDK 26 PHARMA 10 pairs. Every pair favored the candidate by 8.1-9.8%; remaining target and full-suite timing gates were left open rather than overclaimed.
