# Iteratively profile and optimize the 13 REAL_ESTATE theme queries on the LMDB native engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (checked into this repository).

## Purpose / Big Picture

The REAL_ESTATE benchmark theme (added by `execplan-sparse-realestate-theme.md`, checked in at the repository root) contains 13 SPARQL queries that were deliberately designed to hit constructs the LMDB native query engine cannot compile today — non-COUNT aggregates, ORDER BY/LIMIT, DISTINCT at the root, computed BINDs, property paths, subqueries, and non-aggregate OPTIONAL/UNION/MINUS roots — or compiles onto a slow path (per-row generic filters, disabled factorized tail). Today those queries run mostly on RDF4J's generic iterator evaluator and are far slower than they need to be.

After this work, each of the 13 queries has been (a) benchmarked one at a time under JFR CPU-time profiling, (b) analyzed to identify the dominant cost, and (c) improved — either by extending the native engine (new plan shapes, new aggregate kinds, cheaper filters) or, where a fix is out of proportion, documented with profiling evidence explaining exactly what future engine feature is needed. "Significant improvement" means a clearly-measurable drop in average query time on the JMH benchmark (`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`, `-p themeName=REAL_ESTATE -p z_queryIndex=N`) for every query where an engine change landed, with unchanged query results proven by differential tests.

## Progress

- [x] (2026-07-03) Research: benchmark harness, JFR runner, native-engine entry points, correctness-test pattern identified.
- [x] (2026-07-03) ExecPlan written.
- [x] (2026-07-03) Milestone 0: correctness gates in place and green pre-change — `LmdbRealEstateNativeParityTest` (13/13, 1.5 s) and `RealEstateLmdbQueryCountIT` (full-size, 5.4 s).
- [ ] Milestone 1: baseline table — per-query timing with native engine on vs off, plus native/fallback classification.
- [x] (2026-07-03) Milestone 2 (cluster C1): native non-COUNT aggregates (SUM/MIN/MAX/AVG, HAVING over them) → queries 0, 1, 10. AggKind enum, value-typed AggState accumulators mirroring GroupIterator semantics, strict-compare wiring, factorized-tail COUNT-only gate, synthetic ids for VALUES constants absent/unsafe in the store. 19/19 new differential tests green, 13/13 parity green, 33 existing native tests green. JMH (same instrument as baseline): q0 67.3->53.4 ms/op (-21%), q1 51.6->39.3 (-24%), q10 48.3->30.1 (-38%).
- [ ] Milestone 3 (cluster C2): native row-stream roots — Projection/Distinct/Reduced/Order/Slice wrappers and LeftJoin/Union/Minus outside aggregation → queries 2, 3, 11, 12. (Implementation complete: compileRowRoot peels Slice/Distinct/Reduced/Projection/Order, inner tree runs as native slot cursor, full-row snapshots sorted on materialized keys (order keys may be unprojected), id-tuple dedup, offset/limit, bounded top-k under ORDER+LIMIT; DISTINCT enables OPTIONAL-branch pruning via the duplicate-insensitive path. 16/16 row-stream tests, 13/13 parity, 52 regression tests, full-size IT green. Light harness native ms vs M1 baseline: q2 73->43, q3 79->56, q11 950->479, q12 675->325. Remaining: JMH confirmation sweep.)
- [x] (2026-07-03) Milestone 3.5 verdict — materialization wins, Roaring itself does not: light-harness native ms for the eligible queries, off vs hash vs roaring — q3: 60.1 / 47.1 / 49.0, q4: 97.3 / 79.4 / 74.8, q7: 52.1 / 38.6 / 39.8. The one-time-scan membership set beats per-row probes by 18-26%, but Roaring64Bitmap is statistically indistinguishable from the in-repo LongHashSet at these cardinalities (9k-18k ids); Roaring's strengths (compression, bitwise set algebra, very large sets) are not exercised. Plan: keep the materialization with LongHashSet (no external dependency), remove the RoaringBitmap dependency and impl branch after JMH confirmation. 6/6 membership differential tests and all 81 gates green in every mode.
- [former] Milestone 3.5 (prototype, user-requested): RoaringBitmap experiment — materialized membership sets for MINUS / EXISTS / NOT EXISTS whose right side is a single statement pattern correlated on exactly one bound variable (REAL_ESTATE queries 3, 4, 7). Three modes via -Drdf4j.lmdb.membership.impl: off (per-row LMDB probes, today's behavior), hash (LongHashSet), roaring (Roaring64Bitmap). The A/B/C design separates the algorithmic win (materialize once instead of probing per row — the memo is useless when the correlation key is unique per row, as in q4/q7) from Roaring's data-structure contribution. Built lazily after a miss threshold (default 64, tests force 0) with a size cap; RoaringBitmap 1.6.14 from the offline repo, dependency flagged as prototype pending keep/revert decision.
- [ ] Milestone 4 (cluster C3): cheaper filters on the native path (COALESCE, arithmetic BIND, numeric/date ranges) → queries 4, 5, 7, 9.
- [ ] Milestone 5 (cluster C4): query 6 (property path) and query 8 (subquery root) — analyze actual algebra shape, then either extend the engine or document the gap with JFR evidence.
- [ ] Milestone 6: final sweep — re-run all 13 JMH benchmarks, before/after table, full-size correctness IT, formatter/headers, retrospective.

## Surprises & Discoveries

- Observation: `LmdbNativeEvaluationStrategy.precompile` disables the native engine whenever result-size or time tracking is on, so `explain()` output can never show whether a query ran natively. Native/fallback classification must be done differentially: run with `-Drdf4j.lmdb.nativeQueryEngine.enabled=false` vs `true` and compare timings, or assert compilation directly from an in-package test.
  Evidence: `LmdbNativeEvaluationStrategy.java:52-55` — `if (nativeEnabled && !isTrackResultSize() && !isTrackTime() ...)`.
- Observation: `ThemeQueryBenchmark` persists its store under `core/sail/lmdb/target/lmdb-theme-query-benchmark` and validates it against recorded LMDB file sizes (`expected-db-file-sizes.properties`); adding the REAL_ESTATE theme changes the dataset, so the first benchmark run after this branch rebuilds the store from scratch (all 9 themes) — a one-time multi-minute cost.
- Observation: LMDB inlines small numeric literals into value ids, so `idOf` on a VALUES constant like `8` returns a real id even when the term is nowhere in the store — but `safeResourceId` rejects it (raw-id equality is not term equality for inlined encodings), which used to abort native compilation of any VALUES over numerics. Solved with plan-local synthetic ids plus a source decorator (`SyntheticValueSource`): probes bound to synthetic ids return empty, materialization returns the original Value, filters over synthetic variables are forced onto the generic path.
  Evidence: `LmdbNativeNonCountAggregateTest#valuesConstantMissingFromStore` failed with the compiler rejecting until the unsafe-inlined case was covered; REAL_ESTATE q1 then flipped from fallback to AGG-NATIVE (60.7 -> 47.5 ms).
- Observation: With the native engine ENABLED, queries 11 and 12 (non-aggregate denormalized views) are ~1.6x SLOWER than with it disabled (interleaved measurement, so not warmup bias). Hypothesis: the generic evaluator's recursive precompile hands inner join fragments (or EXISTS probes) to the native compilers, and per-outer-row re-execution of those native fragments (plan setup, cursor open, BindingSet bridging) costs more than the generic iterators. Queries 2, 3, 8, 10 show the same effect weakly (ratios 0.90-0.98). Must profile before fixing.
  Evidence: baseline table in Artifacts and Notes; `LmdbRealEstateQueryTimingTest` output 2026-07-03.

## Decision Log

- Decision: Use the repo's Docker JFR loop (`.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery --param themeName=REAL_ESTATE --param z_queryIndex=N`) as the profiling instrument of record: Linux Java 26, JFR CPU-time sampling, fixed settings (no warmup, 10x10s, 1 fork).
  Rationale: The repo mandates `scripts/run-single-benchmark.sh` derivatives for spot-checking performance; JFR CPU-time sampling (`jdk.CPUTimeSample`) is Linux-only, hence Docker. Docker is available on this machine (verified).
  Date/Author: 2026-07-03 / Claude Code.
- Decision: Keep a second, much faster measurement harness for the inner loop: a JUnit test in `core/sail/lmdb` that loads a REAL_ESTATE-only store once into `target/` and times all 13 queries native-on vs native-off with a handful of repetitions. JMH+JFR (minutes per query) is used at cluster boundaries and for hotspot analysis; the light harness (seconds) drives day-to-day iteration.
  Rationale: 13 queries x multiple iterations x ~3 min per JMH run is hours of wall-clock per loop; the light harness gives the same relative signal for coarse decisions.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: Correctness gate is differential, not absolute: every query is evaluated with the native engine on and off and the sorted rows must be identical (pattern copied from `LmdbNativeFactorizedTailAggregationTest.assertSameAsGeneric`). A scaled-down `RealEstateConfig` (fewer properties) keeps the per-change gate fast; a full-size IT additionally asserts the catalog counts through LMDB at milestone boundaries.
  Rationale: Differential testing catches wrong-result bugs in new native plans without needing hand-computed expectations for every intermediate dataset; CLAUDE.md requires tests before/alongside any behavior-affecting engine change.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: Docker JFR runs and local Maven runs share `core/sail/lmdb/target/`, and each Docker run rebuilds the benchmark jar from the working tree inside the container. Therefore: (1) never run local Maven while a Docker run is in its build phase, and (2) never edit `src/main` while a baseline sweep is queued — the next run would bake in-progress code into a "baseline". Baselines for a cluster are taken immediately before that cluster's implementation starts (incremental baselines: cluster N's "before" equals cluster N-1's "after").
  Rationale: observed collision surface; incremental baselines are the honest comparison for per-cluster deltas anyway.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: The native ORDER BY path uses a bounded top-k (PriorityQueue of the worst-of-the-best) when a LIMIT caps output at <= 100k rows and no DISTINCT intervenes; DISTINCT+LIMIT stays on full sort because de-duplication happens after projection and a pre-projection top-k could underfill.
  Rationale: the generic OrderIterator already does limit-aware ordering, so the naive full sort made q2 SLOWER than fallback (99 vs 87 ms); top-k brought it to 43 ms.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: Work is clustered by root cause (C1 aggregates, C2 row-stream roots, C3 filters, C4 paths/subqueries) rather than strictly query-by-query, but profiling and final measurement remain per-query.
  Rationale: Several queries share one missing engine feature; implementing it once moves the whole cluster. Profiling stays per-query because each query can hide a second bottleneck.
  Date/Author: 2026-07-03 / Claude Code.

## Outcomes & Retrospective

(To be written as milestones complete.)

## Context and Orientation

Everything lives in this repository. Key pieces:

- Native engine: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeEvaluationStrategy.java` tries `LmdbNativeAggregateCompiler.tryCompile` (root must be `Group` or `Filter(Group)`; joins/optionals/unions/minus/values/exists supported underneath; only COUNT aggregates), then `LmdbNativeQueryCompiler.tryCompile` (only Filter/Join/StatementPattern/Singleton roots; ID-level filters), else falls back to the generic `StrictEvaluationStrategy`. The system property `rdf4j.lmdb.nativeQueryEngine.enabled=false` disables both compilers; `rdf4j.lmdb.factorizedTail.enabled=false` disables the factorized tail aggregation fast path (see `execplan-factorized-tail-aggregation.md`).
- Queries under optimization: `ThemeQueryCatalog.benchmarkQueriesFor(Theme.REAL_ESTATE)` in `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java`, indices 0-12. Their per-index gap targets are tabulated in `execplan-sparse-realestate-theme.md` ("Context and Orientation"). Ground-truth counts (MemoryStore) are baked into the catalog and re-verified by `core/sail/memory/src/test/java/org/eclipse/rdf4j/sail/memory/benchmark/RealEstateThemeQueryCountTest.java`.
- Benchmark of record: `ThemeQueryBenchmark` (JMH, `@Param themeName`, `@Param z_queryIndex`, method `executeQuery`), which loads ALL themes into one persistent LMDB store under `core/sail/lmdb/target/lmdb-theme-query-benchmark` and verifies expected row counts / `?count` values on every invocation — so a wrong-result native plan fails the benchmark itself.
- Profiling instrument: `.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh` (wraps `scripts/run-single-benchmark-docker.sh`; Linux Java 26 in Docker, JFR CPU-time sampling, stackdepth 1024, DebugNonSafepoints). Read recordings per `.claude/skills/docker-jfr-benchmark-loop/references/jfr-reading.md`.
- Correctness pattern to copy: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeFactorizedTailAggregationTest.java` — evaluates each query twice (native on/off via the system property) and compares sorted row strings.
- House rules: root `-Pquick` install before tests; run tests via `python3 .codex/skills/mvnf/scripts/mvnf.py`; never `-am` or `-q` with tests; formatter `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`; new files need the standard header plus `// Some portions generated by Claude Code`. This branch has ~28 pre-existing failing LMDB tests unrelated to this work (see memory `optimize-lmdb-branch-status.md`); do not chase failures that reproduce without these changes.

## Plan of Work

Milestone 0 — correctness gates. Create `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRealEstateNativeParityTest.java` (same package as the engine, standard header + Claude Code signature). It loads a scaled-down REAL_ESTATE dataset (`ThemeDataSetGenerator.realEstateConfig().withPropertyCount(4000).withAgentCount(400).withDistrictCount(40).withAmenityCount(60)` — small enough to load in seconds, big enough that every query returns rows) into a `@TempDir` `LmdbStore("spoc,posc,ospc")`, then for each catalog query index 0-12 asserts native rows == generic rows (sorted string comparison, the factorized-tail test pattern). Also create `RealEstateLmdbQueryCountIT` (full-size dataset, asserts the exact catalog counts through LMDB with the native engine on) for milestone-boundary verification. Run both once to establish they are green before any engine edits: this is the pre-change baseline evidence.

Milestone 1 — baseline table. Create a JMH-independent timing harness `LmdbRealEstateQueryTimingTest` (JUnit, `@Disabled` by default or guarded by a system property so it never runs in CI) that reuses the parity test's store at full default scale, warms each query 2x, then times 5 repetitions native-on and native-off and prints `index | native ms | generic ms | ratio`. Queries where native==generic within noise are pure fallbacks. Record the table here. In parallel, launch the Docker JFR baseline for each query one at a time (`--param themeName=REAL_ESTATE --param z_queryIndex=N`), starting with the slowest ones from the light harness; archive `.jfr` paths and top-5 CPU hotspots per query in "Artifacts and Notes".

Milestone 2 (C1 aggregates) — extend `LmdbNativeAggregateCompiler`: support Sum/Min/Max/Avg aggregate operators (over a single Var argument) in `compileAggregates`, including HAVING comparisons over them, DISTINCT variants, and the existing group-key machinery. Numeric accumulation must match SPARQL semantics (type promotion int->decimal->double, type errors ignore the row per SPARQL Aggregates; verify semantics against the generic evaluator via the parity test — that is what the differential gate is for). Factorized-tail interactions: non-COUNT aggregates that read the tail slot must disable the factorized path (extend the existing gates). TDD: start each sub-step by adding queries to `LmdbNativeFactorizedTailAggregationTest`-style differential tests plus a compilation assertion that fails while the feature is missing (e.g. a package-private test hook or a timing-differential check); the failing-first evidence is the differential test exercising SUM before support exists only if results diverge — otherwise use a small unit test asserting the compiler returns a non-null step for a SUM query (fails today).

Milestone 3 (C2 row-stream roots) — teach the engine to serve non-aggregate queries: accept `Projection`, `Distinct`/`Reduced`, `Order`, `Slice` wrappers at the root by compiling the inner tuple tree with the aggregate compiler's plan machinery (which already handles LeftJoin/Union/Difference/Values) into a row stream, then applying native or thin generic wrappers (native DISTINCT on ID rows; ORDER BY via materialized sort keys; Slice as a simple limit on the iterator). This unlocks queries 2, 3, 11, 12 (and generally every "denormalized view" theme query in the other 8 themes). Scope control: if full ORDER BY support balloons, land Projection/Distinct/Slice first (queries 3, 12), then Order (2, 11) as a separate step.

Milestone 4 (C3 filters) — profile-first: JFR on queries 4, 5, 7, 9 to see whether the generic per-row filter (COALESCE/arith/range) or something else (e.g. BindingSet view materialization) dominates; then either add ID-level/materialized-value fast paths for numeric range comparison and COALESCE-over-optional-slot, or optimize the per-row evaluation bridge (`SlotBindingSetView`). Decide based on hotspots, not assumptions.

Milestone 5 (C4) — query 6: inspect the optimized algebra (fixed-length sequence paths are usually expanded to joins by the SPARQL parser — if so, the query may already be aggregate-compiler-eligible and its cost lies elsewhere); query 8: subquery `Group` under a non-aggregate root — potentially compilable by allowing the aggregate compiler to be invoked for inner `Group` nodes reached through the generic evaluator's recursive precompile (verify whether that already happens; if it does, the gap is only the outer join/filter). Implement what profiling justifies; document the rest.

Milestone 6 — final sweep: for each query index 0-12 run the JMH benchmark (same command as baseline), build the before/after table, re-run the full-size IT + parity test + `testsuites/benchmark-common` + targeted LMDB test classes touched, formatter + copyright check, update this plan's Outcomes & Retrospective, and update the memory file `optimize-lmdb-branch-status.md`.

## Concrete Steps

All commands from the repository root.

    # once per session
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

    # correctness gates (fast, run after every engine change)
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRealEstateNativeParityTest --module core/sail/lmdb

    # full-size catalog-count check through LMDB (milestone boundaries)
    python3 .codex/skills/mvnf/scripts/mvnf.py --it RealEstateLmdbQueryCountIT --module core/sail/lmdb

    # light timing sweep (inner loop)
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRealEstateQueryTimingTest --module core/sail/lmdb -- -Drdf4j.realestate.timing=true

    # profiling run of record, one query at a time (N = 0..12)
    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
      --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark \
      --method executeQuery \
      --param themeName=REAL_ESTATE \
      --param z_queryIndex=N

The JFR output path is printed by the runner; read it with `jfr print --events jdk.CPUTimeSample` style tooling per `references/jfr-reading.md` in the skill directory.

## Validation and Acceptance

- Every engine change: `LmdbRealEstateNativeParityTest` green (native rows identical to generic rows for all 13 queries) plus the existing LMDB native-engine test classes for the touched area (e.g. `LmdbNativeFactorizedTailAggregationTest`, `LmdbNativeAggregateFilterSemanticsTest`).
- Cluster completion: JMH `executeQuery` for the cluster's queries shows a clear average-time improvement vs the recorded baseline under identical settings, and the benchmark's built-in count verification passes (it throws on wrong results).
- Plan completion: before/after table for all 13 queries in this document; queries without an engine change have a documented profiling analysis naming the dominant cost and the engine feature required.

## Idempotence and Recovery

Benchmark stores live under `core/sail/lmdb/target/` and are rebuilt automatically when deleted or when their recorded file sizes mismatch. JFR recordings and JMH logs are plain files; re-running a step overwrites nothing that matters (recordings are timestamped). Engine changes are ordinary git-tracked edits; revert per file to roll back. If a native plan produces wrong results, the parity test pinpoints the query index; disable the new path via its gate (each new feature must keep a clean "return null → fallback" bail-out) while diagnosing.

## Artifacts and Notes

Baseline (2026-07-03, light harness `LmdbRealEstateQueryTimingTest`, full-size REAL_ESTATE-only store, median of 5 interleaved reps, macOS arm64 local JVM — relative signal only; JMH numbers are the record):

    query  native-ms  generic-ms  ratio(gen/nat)
    0          81.08       92.53   1.14
    1          60.70       68.98   1.14
    2          72.68       71.53   0.98
    3          79.01       74.92   0.95
    4          99.09      116.46   1.18
    5         235.71      259.41   1.10
    6          42.68       60.60   1.42
    7          54.54       69.11   1.27
    8          37.56       34.96   0.93
    9          43.26       46.27   1.07
    10         59.18       53.54   0.90
    11        950.17      583.71   0.61   <-- native ON is 1.6x SLOWER
    12        674.56      414.54   0.61   <-- native ON is 1.6x SLOWER

Reading: no query gets a large native win today (max 1.42x on q6); q11/q12 actively regress with the native engine enabled. Priority order by absolute cost: q11, q12, q5, q4, q0, q3, q2.

JMH baselines of record (Docker, Linux Java 26, JFR CPU-time on, avgt 10x10s):

    q11 executeQuery REAL_ESTATE: 698.966 +/- 48.947 ms/op   (logs/jfr/docker-q11-baseline.log; recording logs/jfr/q11-baseline.jfr)
    q0  executeQuery REAL_ESTATE:  67.260 +/-  3.288 ms/op   (logs/jfr/docker-q0-baseline.log)
    q1  executeQuery REAL_ESTATE:  51.622 +/-  1.651 ms/op   (logs/jfr/docker-q1-baseline.log)
    q10 executeQuery REAL_ESTATE:  48.343 +/-  4.822 ms/op   (logs/jfr/docker-q10-baseline.log)

After Milestone 2 (same instrument):

    q0  53.373 +/- 2.545 ms/op  (-21%)   logs/jfr/docker-q0-after-m2.log,  recording logs/jfr/q0-after-m2.jfr
    q1  39.263 +/- 1.512 ms/op  (-24%)   logs/jfr/docker-q1-after-m2.log,  recording logs/jfr/q1-after-m2.jfr
    q10 30.099 +/- 3.028 ms/op  (-38%)   logs/jfr/docker-q10-after-m2.log, recording logs/jfr/q10-after-m2.jfr

After Milestone 3 (same instrument; q2/q3/q12 had no per-query JMH baseline — the M1 light-harness native column is
their reference; q11's JMH baseline was 698.966):

    q2  41.187 +/- 1.449 ms/op            logs/jfr/docker-q2-after-m3.log, recording logs/jfr/q2-after-m3.jfr
    q3  55.080 +/- 2.620 ms/op            logs/jfr/docker-q3-after-m3.log
    q12 362.443 +/- 16.191 ms/op          logs/jfr/docker-q12-after-m3.log
    q11 (rerun in flight after a JMH lock clash between overlapping sweeps)

q2 post-M3 hotspots (logs/jfr/q2-after-m3.jfr): RowBindingSetView bridge is gone; remaining: 34.5% LMDB cursor
work, 15.9% CursorPool acquire/release churn, ~6% AggContext value cache (boxed HashMap) — both M4 targets.

q11 CPU-time hot methods (native engine enabled — explains the native-on regression):

    16.72%  LMDB.nmdb_cursor_get                      <- real index work
     8.67%  RowBindingSetView.slot(String)            <- bridge: string-keyed slot lookup
     7.91%  RowBindingSetView.size()
     7.07%  String.equals                             <- from slot-name scans
     5.80%  TripleStore$CursorPool.release
     3.53%  TripleStore$CursorPool.acquire            <- per-outer-row native plan reopen
     2.98%  ValueStore.getStoredHash
     2.21%  RowBindingSetView.hasBinding(String)
     1.95%  RowBindingSetView.<init>
     1.56%  NativePlanIteration.<init>
     1.26%  LeftJoinIterator.getNextElement           <- generic OPTIONAL machinery driving it all

Conclusion: ~30% of q11 CPU is the generic-evaluator/native-plan bridge (RowBindingSetView string lookups + per-row plan/cursor setup), not data access. Fix strategy confirmed for M3: compile the whole non-aggregate tree natively (no per-row bridging); the bridge itself is a secondary target (M4) for queries that must keep falling back.

## Interfaces and Dependencies

No new Maven dependencies. Engine work happens in `org.eclipse.rdf4j.sail.lmdb` (`LmdbNativeAggregateCompiler`, `LmdbNativeQueryCompiler`, possibly new plan classes in the same package). New aggregate support must keep the existing contract: `tryCompile(...)` returns `null` whenever any part of the tree is unsupported, so the generic evaluator remains the universal fallback. Tests live in the same package under `core/sail/lmdb/src/test/java`.

Revision note (2026-07-03, Claude Code): initial version, written after research and before Milestone 0.
