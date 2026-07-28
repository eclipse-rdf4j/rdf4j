# ExecPlan 14: COUNT(DISTINCT x) + single-pattern EXISTS as a prefix-run intersection

This plan makes the LMDB native query engine answer queries of the shape "count the distinct values
that appear in one triple position AND also appear in another triple position" by intersecting two
sorted distinct-value streams read directly from LMDB indexes, instead of scanning every statement
and probing a hash table per row. The concrete target is the ANALYTICS theme benchmark query with
`z_queryIndex = 10` ("subjects that are also objects"), which must get about 10x faster.


## Why this matters (user-visible outcome)

The SPARQL query below (ThemeQueryCatalog, theme ANALYTICS, index 10, file
`testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java`
lines 1964-1970) currently takes ~2976 ms/op on the LMDB theme benchmark store:

    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT (COUNT(DISTINCT ?node) AS ?count) WHERE {
      ?node ?p ?o .
      FILTER EXISTS { ?s ?q ?node . }
    }

Baseline (captured 2026-07-21 with the docker-jfr-benchmark-loop skill, Linux Java 26 in Docker,
JFR CPU-time profiling; JFR copy kept in the session scratchpad as `baseline-analytics-q10.jfr`):

    Benchmark                         (themeName)  (z_queryIndex)  Mode  Cnt     Score     Error  Units
    ThemeQueryBenchmark.executeQuery    ANALYTICS              10  avgt   10  2975.712 ± 132.369  ms/op

    Top CPU: 20.0% LMDB.nmdb_cursor_get, 15.9% KeyedMatches.memoGet, 9.2% KeyedMatches.find,
             5.3% nmdb_cursor_put, 3.5% LongHashSet.add, 3.4% RowState.bind, 3.3% LmdbRecordIterator.fill

After this plan, that query runs as "intersect distinct subjects (SPOC prefix runs) with distinct
objects (OSPC prefix runs)". Success = the benchmark reports about 300 ms/op or less for that
selector, with identical query results, and the whole LMDB test suite still green.


## Background a novice needs

The LMDB sail (`core/sail/lmdb`) has a native query engine in package
`org.eclipse.rdf4j.sail.lmdb.evaluation`. `LmdbNativeEvaluationStrategy.precompile` hands whole
query trees to `LmdbNativeAggregateCompiler.tryCompile`; aggregate queries compile through
`LmdbNativeAggregatePlanner.compileGroup` (file `LmdbNativeAggregatePlanner.java`, ~line 79) into a
`NativeGroupStep` (file `LmdbNativeGroupStep.java`) that executes physical `SlotPlan` operators over
primitive `long[]` slot rows carrying internal value ids (no RDF Value objects on the hot path).

For our query, `compileGroup` produces: `NativeGroupStep(aggregates=[COUNT DISTINCT slot(?node)],
arg = FilterPlan(PatternPlan{?node ?p ?o}, StatementPatternExistsFilter{o = slot(?node), rest
unbound}))`. At run time (`NativeGroupStep.evaluateSpeculative`, ~line 290) this full-scans the SPOC
index, and the EXISTS filter (class `StatementPatternExistsFilter` in `LmdbNativeFilters.java`)
memoizes per-row probes and, past 64 distinct keys, builds a `PatternMembershipProbe` hash set of
every distinct object id in the store. That is the memoGet/find/LongHashSet CPU in the baseline.

A "prefix run" is a maximal run of consecutive index keys that share a leading key segment.
`TripleStore.prefixRunPlan(prefixFields, subj, pred, obj, context)` (file `TripleStore.java`,
~line 1495) picks the triple index (SPOC/OSPC/PSOC/POSC) where the requested fields, together with
any bound constants, form the shortest index-leading prefix; `LmdbPrefixRunIterator` (file
`LmdbPrefixRunCursor.java`) then emits ONE representative quad per distinct prefix and seeks past
the rest of each run with `LmdbRecordIterator.seekForward` (an MDB_SET_RANGE reposition), i.e. a
sub-linear distinct-value skip-scan. Because all fields before the requested field in the chosen
index are constant-bound, the requested field's values come out strictly ascending and unique.
This machinery already powers `SELECT DISTINCT ?x { pattern }` and single-pattern
`COUNT(DISTINCT ?x)` (see `tryPrefixRunGroupPlan`, `LmdbNativeAggregatePlanner.java` ~line 317) but
today refuses anything whose group argument is not a bare `PatternPlan` — the EXISTS `FilterPlan`
blocks it.

Key insight: for this query shape the answer is exactly

    | distinct values of the outer pattern's ?node position |  INTERSECT  | distinct values of the EXISTS pattern's correlated position |

computed on internal value ids (both streams come from the same value dictionary), so two
prefix-run cursors merged leapfrog-style (advance the smaller, seek it forward to the other's
current value) produce the count without ever materializing rows, hash sets, or Values.


## What to build

### 1. `seekTo` on the prefix-run cursor

File `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbPrefixRunCursor.java`.
Add to the `LmdbPrefixRunCursor` interface:

    /** Positions the cursor so the next call to next() returns the first run whose requested
     *  (last prefix) field value is >= value. Returns false when the cursor is already exhausted. */
    boolean seekTo(long value) throws IOException;

Implementations: `EMPTY` returns false. `LmdbPrefixRunIterator.seekTo(value)` builds a target quad
the same way `prepareNextSeekTarget` does — for every index field before the target field use the
bound constant, for the target field use `value`, for later fields use the bound value or 0 — then
clears `pending` and calls `delegate.seekForward(...)`; returns false if `exhausted`.
`LmdbSailStore.NativeSourceReadLockedPrefixRunCursor` (file `LmdbSailStore.java`, ~line 3921)
delegates with the same open/closed guard as `next()`. The target field index is
`plan.prefixFields()[plan.prefixFields().length - 1]`... careful: prefixFields are quad-position
constants (TripleIndex.SUBJ_IDX etc.), and the iterator already maps index field order via
`fieldIndex(fieldSeq[i])`; reuse that helper.

### 2. Recognizer in the planner

File `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregatePlanner.java`.
In `compileGroup`, after `tryPrefixRunGroupPlan` returns null, try a new
`tryExistsIntersectionPlan(stepSource, arg, groupSlots, aggregates)` that returns a candidate when
ALL of:

- `groupSlots.length == 0`, `aggregates.length == 1`, `isCountDistinctSlot(aggregates[0])`.
- `arg` is `FilterPlan` whose `arg` is a `PatternPlan` (call it outer) and whose `filter` is a
  `StatementPatternExistsFilter` (class in `LmdbNativeFilters.java`, same package).
- The filter's `varyingSlots` is exactly `[aggregates[0].slot]` (one correlated slot, and it is the
  distinct slot; repeated-position correlation gives length 2 and is excluded automatically).
- Filter context safety: `!filter.contexts.isFixed() && !filter.namedContextScope`, and the
  filter's `c` term is unbound (no slot, no constant).
- Exactly one of the filter's `s`/`p`/`o` terms is `slot(distinctSlot)`; the remaining terms are
  each unbound or constant.
- Outer side plans: `tryPrefixRunPlan(stepSource, outer, {distinctSlot})` returns non-null (this
  already applies `prefixSafePattern` — no fixed contexts, no named-graph scope, no repeated slots).
- EXISTS side plans: `stepSource.prefixRunPlan(new int[]{existsField}, sConst, pConst, oConst,
  UNKNOWN)` returns non-null, where existsField is the quad position of the correlated term and the
  consts are the constants of the other terms (UNKNOWN when unbound).

Package the result in a new class `LmdbNativeExistsIntersection` (new file in the evaluation
package) holding: outer `PatternPlan`, outer `LmdbPrefixRunPlan`, exists `LmdbPrefixRunPlan`,
exists constant ids (s, p, o, c as long, UNKNOWN when unbound), and the aggregate name. Pass it to
the `NativeGroupStep` constructor alongside the existing prefix-run candidate parameters.

### 3. Execution in `NativeGroupStep`

File `LmdbNativeGroupStep.java`. In `evaluateSpeculative`, right after the `evaluatePrefixRuns`
attempt, call `evaluateExistsIntersection(row)`; non-null short-circuits. That method:

- Bails (returns null) when the candidate is null or `outerPattern.hasRuntimeBoundSlot(row)`.
- Opens cursor A = `source.prefixRuns(outerPlan, outer constants via term.lookup(row.slots), false)`
  and cursor B = `source.prefixRuns(existsPlan, exists constants, false)` (try-with-resources).
- Leapfrog merge on the requested field value of each side (read from `cursor.quad()` at the
  respective quad position): advance both with `next()` when equal (count++), otherwise `seekTo`
  the lagging cursor to the leading cursor's value. Values on each side are strictly ascending and
  unique (see Background), so the merge is exact.
- Returns `List.of(bindingSet)` where the binding set carries
  `createLiteral(BigInteger.valueOf(count))` under the aggregate name — the same literal the COUNT
  branch of `toBindingSet` produces (see `LmdbNativeGroupStep.toBindingSet`, ~line 866), so HAVING
  filters and projections above behave identically.
- Records the strategy for EXPLAIN/metrics: add `PATH_EXISTS_INTERSECTION` to
  `LmdbNativeAttemptMetrics` (and its EXECUTION_PATH_VOCABULARY) and call
  `metrics.deferStrategy(explainTarget, PATH_EXISTS_INTERSECTION)` like the prefix-run path does.
- Static counters on `LmdbNativeExistsIntersection` (`PLANNED`, `OPENED`, `MATCHED` AtomicLongs,
  mirroring `LmdbPrefixRunPlan`) so tests can assert engagement.

Correctness notes a novice must respect: COUNT DISTINCT over zero rows still yields one solution
with count 0 (return the binding set unconditionally). The intersection ignores contexts because
runs are keyed only on the requested field (a subject present in two named graphs is one run).
Both sides must come from the same `NativeLmdbQuerySource`; when the store has both explicit and
inferred branches active, `CompositeNativeLmdbQuerySource.prefixRunPlan` returns null and the
recognizer simply never fires (generic paths take over) — do not try to merge across branches.

### 4. Optional milestone: parallel partitioned merge

Only if the sequential merge misses ~300 ms/op: split the id domain into K ranges and run one
leapfrog merge per range on the parallel aggregation pool, summing the partial counts. Partition
boundaries can come from `LmdbNativeExchange.tryPlanRootPartitions` over the outer pattern (raw
key ranges on the outer index; take the outer field value at each boundary and reuse those values
as `seekTo` bounds on both sides), or from uniform splits of [first value, Long.MAX_VALUE] since
ids are densely assigned. Workers each open their own cursors via the per-worker source family
used by `LmdbNativeParallelAggregation`. This milestone is SKIPPED if unnecessary — decided by the
benchmark, not by taste.


## Validation

TDD order is mandatory (repo Routine A): the query-level test lands and fails BEFORE any
production edit.

New test `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbExistsIntersectionQueryTest.java`
modeled on `LmdbPrefixRunQueryTest` (SailRepository over LmdbStore, index config "spoc,ospc,psoc,posc"):

- subjects-also-objects query returns the right count on a small dataset containing: IRIs that are
  both subject and object, subject-only IRIs, object-only IRIs, literals, bnodes on both sides,
  and duplicate memberships across contexts — and `LmdbNativeExistsIntersection.OPENED` increases.
- mirror query (distinct objects that are also subjects: outer `?s ?p ?node`, EXISTS
  `{ ?node ?q ?x }`) engages and is correct.
- bound-predicate EXISTS variant `FILTER EXISTS { ?s <p> ?node }` engages (POSC side) and is correct.
- non-engagement cases stay correct and do NOT bump OPENED: extra FILTER conjunct, GROUP BY
  present, EXISTS with a second pattern, correlated slot used twice (`{ ?node ?q ?node }`),
  named-graph-scoped query (`GRAPH ?g { ... }` around the EXISTS).
- empty store / empty intersection returns count 0.
- results equal to the memory-store answer for a randomized-ish fixed dataset (belt and braces).

Cursor-level `seekTo` tests go in `LmdbPrefixRunIteratorTest` style next to the existing ones.

Then: `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbExistsIntersectionQueryTest` red → implement →
green; `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` for the module; benchmark rerun
with the exact baseline selector via
`.codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh --module core/sail/lmdb
--class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery
--param themeName=ANALYTICS --param z_queryIndex=10`; also re-run neighboring ANALYTICS indexes 0
and 12 plus ELECTRICAL_GRID:10 to check for regressions (>5% per-query gate from plan 07).


## Progress

- [done] Baseline benchmark + JFR captured (2975.712 ms/op; hotspots recorded above)
- [done] Execution-path exploration and design (this document)
- [done] Failing query-level test observed (9 tests: 3 engagement failures, 6 correctness passes; initial-evidence.txt)
- [done] seekTo on prefix-run cursor + 2 unit tests (LmdbPrefixRunIteratorTest 6/6 green)
- [done] Recognizer + LmdbNativeExistsIntersection + NativeGroupStep wiring (LmdbExistsIntersectionQueryTest 9/9 green)
- [done] Sequential merge benchmark: 1504.156 ms/op (~2x; 52% CPU in per-run MDB_SET_RANGE seeks) — parallel milestone required
- [done] Parallel partitioned merge (prefixRunSplitValues via interpolated split keys, ParallelSource workers, TaskReservation admission; forced-parallel tests green, 11/11)
- [done] Parallel merge benchmark 913.5 ms; JFR showed 34.8% CPU in per-run AtomicLong metric
  increments contended across workers — prefix-run metrics now accumulate locally and flush once on
  close; final ANALYTICS:10 = 162.8 ms/op (18x vs the 2975.7 baseline; user's sweep showed 230.7)
- [done] Module test suite green (13 pre-existing failures only); cursor later reworked onto bulk
  fill() batches by ExecPlan 15 with all suites green
- [done] COMPLETE — see plans/lmdb-native-engine/15-parallel-prefix-run-groups.md for the follow-on
  work that generalized this machinery to the whole ANALYTICS theme


## Decision log

- 2026-07-21: Chose prefix-run intersection over (a) parallelizing the existing scan+membership
  plan (still O(all statements), memory-heavy) and (b) disabling the per-row memo in
  StatementPatternExistsFilter (saves ~16% CPU at best, nowhere near 10x). The intersection is
  bounded by the two distinct-value counts, touches no Values, and reuses proven skip-scan
  machinery.
- 2026-07-21: Merge equality is on internal ids only — sound because both cursors read the same
  store snapshot/dictionary; the plan never fires on composite (explicit+inferred) sources.
- 2026-07-21 (implementation discovery): EXISTS conditions get `placeableFilterMask == -1`
  (`LmdbNativeAggregateFilterCompiler.placeableFilterMask` rejects `containsExists`), so the
  FilterPlan is not encounter-order replay-safe and `NativeGroupIteration.evaluateAll` short-circuits
  to `evaluateSequential` without ever reaching `evaluateSpeculative`. The intersection hook
  therefore lives at the top of `evaluateAll` (after row initialization), not in
  `evaluateSpeculative` as originally sketched. Also, `recordFilterOutcomes` wraps the EXISTS filter
  in `RecordingNativeBooleanFilter` whenever evaluation statistics exist, so the recognizer unwraps
  one recording layer before matching `StatementPatternExistsFilter`. The distinct slot is refused
  at the context quad position (context id 0 means "unbound" there, which the id merge cannot model).
