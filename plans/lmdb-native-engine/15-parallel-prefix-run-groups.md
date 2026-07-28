# ExecPlan 15: Parallel prefix-run group aggregation for the ANALYTICS queries

Goal: every ANALYTICS theme benchmark query currently above 100 ms/op must get 2x-10x faster
(user directive 2026-07-21). Reference numbers from the user's latest sweep (ThemeQueryBenchmark,
LMDB store, ~13.8M statements):

    q2  statement count per predicate      306 ms   prefixRunGroups (sequential run-row counting)
    q3  distinct subjects per class        126 ms   orderedDistinctGroups (full scan)
    q4  distinct objects per predicate     635 ms   orderedDistinctGroups (full scan)
    q5  literal datatype histogram        1731 ms   generic aggregation over native row stream
    q6  out-degree histogram              1173 ms   inner Group prefixRunGroups (sequential walk)
    q7  class predicate usage matrix       922 ms   parallelAggregation (join, already parallel)
    q8  class linkage matrix               761 ms   parallelAggregation (join, already parallel)
    q9  high in-degree objects            1642 ms   singleSlotGroups (hash groups over SPOC scan)
    q10 subjects that are also objects     231 ms   existsIntersection (done, ExecPlan 14: 13x)

Queries 0, 1 are already sub-millisecond. Query shapes are in
`testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java`
lines 1873-1988. Execution-path telemetry above from
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-07-21.md`.

Background needed to read this plan: ExecPlan 14 (`plans/lmdb-native-engine/14-exists-distinct-intersection.md`)
introduced `LmdbPrefixRunCursor.seekTo`, `TripleStore.planPrefixRunSplitValues` (interpolated index
split keys), `NativeLmdbQuerySource.prefixRunSplitValues/openParallelSources`, and the parallel
leapfrog merge in `LmdbNativeExistsIntersection`. A "prefix run" is a maximal block of consecutive
index keys sharing a leading key segment; `LmdbPrefixRunIterator` emits one representative per run
and either seeks past the rest of the run (distinct streaming) or walks the run counting rows
(`countRunRows`, used for COUNT per group). `NativeGroupStep.evaluatePrefixRuns`
(`LmdbNativeGroupStep.java`) executes those plans; `LmdbNativeAggregatePlanner.tryPrefixRunGroupPlan`
(~line 317) recognizes them but today only accepts a bare `PatternPlan` group argument and only the
combos {group slots, no aggregates}, {group slots, COUNT(*)}, {no group slots, COUNT DISTINCT}.

Value ids embed the value type: `ValueIds.getIdType(id)` returns T_URI/T_LITERAL/T_BNODE or an
inlined-literal code (T_INTEGER..T_ORD_*, T_DOUBLE via low bit) whose xsd datatype is fixed per
code. This makes isIRI/isLiteral and (for inlined literals) DATATYPE computable from the id alone.


## Milestones

### M1 — Parallel partitioned prefix-run group execution (accelerates q2, q6; foundation for the rest)

Partition a prefix-run group scan into disjoint ranges of the streamed prefix (decoded from
interpolated split keys of the plan's own index — `planPrefixRunSplitValues` generalized to return
all prefix fields per split, not just the last), run one prefix-run aggregation per range on the
shared pool (`LmdbNativeParallelPipelines.pool()`, `TaskReservation` admission, per-worker
`openParallelSources` siblings), and merge the per-range group tables by summing counts per group
key. Boundaries are full-prefix tuples, so a run (one prefix) is always wholly owned by one worker;
only groups can span workers, and their partial COUNT/COUNT-DISTINCT-run counts sum exactly.
Seeking a worker to its range start uses a multi-field `seekTo(long[] prefixValues)` added next to
the single-value `seekTo`. Admission mirrors ExecPlan 14: engage only when the pattern's static
estimate exceeds `rdf4j.lmdb.prefixRun.parallelMinEstimate` (default 1,000,000; tests force 0).
Merged results must be produced before binding sets are emitted so consumers (e.g. q6's outer
histogram group) still see exactly one row per group.

### M2 — Recognizer extensions (unlocks q3, q4, q9)

1. group slots + one `COUNT(DISTINCT ?x)` where ?x is a pattern position: prefixFields = group
   fields followed by the distinct field; per group, the aggregate value is the number of runs.
2. `COUNT(?var)` (non-distinct) over a variable bound by the pattern itself counts exactly like
   COUNT(*) (the pattern always binds it): treat as countRunRows.
3. A `FilterPlan` between Group and PatternPlan no longer disqualifies the prefix-run plan when the
   filter's read mask is a subset of the group prefix slots (e.g. q9's `isIRI(?object)` reads only
   the group slot): evaluate the filter once per run representative; rejected runs are skipped
   (seek past) without counting.

### M3 — q5 literal datatype histogram specialization

Recognize `Group(BIND(DATATYPE(?o)) as key, COUNT(*)) over Filter(isLiteral(?o), Pattern(?s ?p ?o))`
and execute as OSPC prefix runs on [o] with countRunRows: per run, the object id's type code
decides literal-ness (non-literal runs are seek-skipped without counting) and the datatype —
directly from the id for inlined literals, via a memoized ValueStore lookup for T_LITERAL
references. Histogram keyed on datatype id, merged parallel via M1.

### M4 — q7/q8 (already parallel joins): profile-first

Rerun with JFR after M1-M3, find the dominant cost (join probing vs group hashing of 10.7M/7.6M
rows), and apply targeted fixes. Not designed up front.


## Validation

Every recognizer extension gets query-level tests in the style of
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbPrefixRunQueryTest.java` (small store,
correct results, engagement asserted via LmdbPrefixRunPlan counters / new parallel counters,
non-engagement cases stay correct), written to fail before the production change (Routine A).
Parallel paths are additionally forced in tests via the min-estimate property = 0 and verified
against hand-computed or sequentially-computed counts, including group-spanning-worker cases.
Benchmarks: `.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh --module
core/sail/lmdb --class ...ThemeQueryBenchmark --method executeQuery --param themeName=ANALYTICS
--param z_queryIndex=<n>` for each affected query, before/after, plus a regression check on q0, q1,
q10 and ELECTRICAL_GRID:10.


## Progress

- [done] Telemetry review of q2-q9 execution paths; ValueIds type-encoding confirmed
- [done] M1 v1 (prefix-tuple boundaries, per-worker binding sets): q2 306->248 (1.2x), q6 1173->793 (1.5x) — insufficient
- [done] M1 v2: recursive-bisection split keys (planBalancedSplitKeys), raw-key boundaries with partial-run
  stitching for counting mode, work-queue over 4x-oversampled ranges, raw entries + post-merge
  materialization; skew test green (5/5), exists-intersection and prefix-run suites green
- [done] q2 benchmark with balanced raw-boundary partitioning: 306 -> 124.7 ms/op (2.5x) — target met
- [done] M2 recognizers (group + COUNT DISTINCT via run counting; COUNT(boundVar) == COUNT(*);
  run-constant filters evaluated once per run, sequential-only for thread-safety) — 14/14 + 6/6 green
- [done] Full lmdb module suite: back to exactly the 13 pre-existing failures (3 white-box grouping
  tests pinned to their machinery via rdf4j.lmdb.prefixRun.enabled=false; testQuery2 / union-shape
  ordered-distinct / feature-flag-fork / value-store-cache failures confirmed pre-existing)
- [done] First q3/q4/q9 benchmarks: q4 635->302 (2.1x, met); q3 126->201 and q9 1642->2639 REGRESSED
- [done] Root causes + fixes: (a) run-length-1 runs paid one MDB_SET_RANGE each — cursor reworked onto
  bulk fill() batches with in-batch run detection and adaptive skip (seek only after 16 same-prefix
  rows); (b) per-row next() paid lock+JNI per row — same fix; (c) group-slot-only filters now applied
  per merged group on the query thread, re-enabling parallelism for q9; (d) all id comparisons made
  unsigned (inlined double ids are negative as signed longs). In-batch seekTo satisfies near targets
  without touching LMDB. All four suites green (7/14/6/11).
- [done] Re-benchmark after cursor rework: q3 51.4 (2.4x, met), q2 127 (2.4x stable), q10 163 (18x); q9
  still 2373 — its HAVING adds a second anonymous COUNT aggregate so the recognizer never fired, and
  ~8M groups materialized before HAVING dropped all but 113
- [done] q9 enablers: multi-aggregate all-COUNT combo (every whole-row COUNT binds the run count) and
  HAVING(COUNT >= n) pushdown (min-run-count pruning before materialization, sequential + parallel;
  generic HAVING above still re-checks survivors) — suites 16/6/10 green
- [done] q9: 1642 -> 398 ms (4.1x, met); q4: 635 -> 76 ms (8.4x, met); q6 837 (1.4x, still short)
- [done] q6 run-count histogram recognizer + LmdbNativeRunCountHistogram step (nested GROUP BY collapses
  into one run-counting scan, parallel run-aligned) — 3/3 green
- [done] q6: 1173 -> 61.7 ms (19x, met)
- [done] M3 q5 datatype-histogram recognizer + LmdbNativeDatatypeHistogram (per-distinct-value datatype,
  id-type dismissal of non-literals, per-type-code cache for inlined ids) — 3/3 green
- [done] q5 first cut 926 ms (1.9x); JFR showed ~45% in per-distinct-literal dictionary reads with full
  label decode — added ValueStore.literalDatatypeId header peek (datatype id read without label decode
  or value-cache traffic, exposed via NativeLmdbQuerySource) — q5: 1731 -> 678 ms (2.6x, met)
- [done] M4 design: q7/q8 joins dissolve into one subject-ordered co-scan (each SPOC subject run holds
  the instance's rdf:type rows AND its edges; q8 adds a prebuilt instance-to-types table for targets;
  predicate filters pre-evaluated per distinct predicate id on the query thread) —
  LmdbNativeTypeMatrix + MultiJoinPlan-shape recognizer, 4/4 tests green, module suite still exactly
  the 13 pre-existing failures
- [done] q7: 922 -> 91/92.6 ms (10x, met). q8 first cut 512 (HashMap boxing in the type table), second
  269.7 ms after: primitive open-addressing instance-to-types table, subject-ordered type scan (the
  unordered scan came back type-ordered and forced a 2.9M-pair quicksort per evaluation), primitive
  run buffers, reusable GroupKey probe, id-type probe skip for literal objects — 2.8x, met
- [done] Formatter + copyright green; full module suite: exactly the 13 pre-existing failures
- [done] Regression spot-check: ANALYTICS 0 = 0.162 ms, 1 = 0.326 ms (still far below the 100 ms bar;
  small absolute deltas vs the user's sweep are measurement-config noise under JFR), ELECTRICAL_GRID
  10 = 60.2 ms vs 51.2 +- 12 recorded the same day (within noise + profiling overhead)
- [done] COMPLETE. Final scores vs the user's baselines (docker JFR harness, 10x10s):
  q2 306->127 (2.4x), q3 126->51 (2.5x), q4 635->76 (8.4x), q5 1731->678 (2.6x), q6 1173->62 (19x),
  q7 922->92 (10x), q8 761->270 (2.8x), q9 1642->398 (4.1x); q10 (ExecPlan 14) 2976->163 (18x).
  Every ANALYTICS query above 100 ms improved 2.4x-19x. Module suite: exactly the 13 pre-existing
  failures before and after.
- [todo] M2 recognizers + tests + q3/q4/q9 benchmarks
- [todo] M3 q5 specialization + benchmark
- [todo] M4 q7/q8 profiling and targeted fixes
- [todo] Full ANALYTICS sweep + regression check + handoff


## Decision log

- 2026-07-21: Partition boundaries are full-prefix tuples (not raw keys): runs are never split, so
  countRunRows partials and distinct-run counts merge by plain per-group summation, no boundary
  corrections. The residual skew risk (one gigantic run owned by one worker) is accepted for now —
  for q2 (prefix=[p]) the largest predicate bounds the critical path, still ~an order better than
  the sequential walk.
