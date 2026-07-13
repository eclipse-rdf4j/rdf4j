# Factorized tail execution for native LMDB aggregate queries (Kuzu-style factorization, migration steps 3–4)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (from the repository root).

## Purpose / Big Picture

The LMDB store has a native query engine that evaluates COUNT-style aggregate queries directly over 64-bit value ids instead of materialized RDF values. Today that engine enumerates every combination of pattern matches one row at a time. For a "star join" — several triple patterns sharing one join variable, such as `?s :p1 ?a . ?s :p2 ?b` — a query like `SELECT (COUNT(?s) AS ?c)` forces the engine to produce `kₐ×k_b` physical rows per `?s` even though the aggregate only needs the match counts. After this change, the *last* pattern in the join order streams its matches as counted batches straight into the aggregation state: `COUNT` adds the batch size without binding rows, `COUNT(DISTINCT ?x)` inserts the batch's ids into a hash set without binding rows, and aggregates that do not touch the last pattern's variables stop scanning after the first match (a semi-join). This is the first slice of "factorized execution" (representing intermediate results as sets-per-branch instead of enumerated cross products), applied where it pays most: the tail of the join order under an aggregate, where nothing downstream ever consumes the enumerated rows.

Observable outcome: aggregate queries over star shapes return identical results to the generic RDF4J evaluator (verified by new differential tests that run each query twice, once with the native engine disabled) while executing measurably faster on the theme benchmark's aggregate-heavy queries. A new test class demonstrates the behavior; a JMH spot run shows the speedup.

## Progress

- [x] (2026-07-02 13:20Z) ExecPlan authored; design decisions recorded below.
- [x] (2026-07-02 13:40Z) M1: MultiJoinPlan.open refactored into derive/openChain helpers; PatternPlan accessors (freshProducedMask, quadPositionOfSlot, hasRepeatedSlot) added; behavior-neutral; module tests green.
- [x] (2026-07-02 14:05Z) M2 tests written first (LmdbNativeFactorizedTailAggregationTest, differential vs generic engine) and observed passing against the enumerating baseline before the fast path existed.
- [x] (2026-07-02 14:40Z) M2: FactorizedTail implemented and wired into NativeGroupIteration.evaluateAll (all three group shapes) behind the gates; 14 differential tests green including the ENGAGED engagement proof; 587-test aggregate/regression selection green.
- [x] (2026-07-02 13:34Z) Full module verify green: `Tests run: 1223, Failures: 0, Errors: 0, Skipped: 3` (logs/mvnf/20260702-112739-verify.log); failsafe ITs unchanged at their pre-existing sketch-gated counts (111 run, 5 failures, 77 errors).
- [x] (2026-07-02 14:55Z) Gate widened: a single GROUP BY key produced by the tail is handled by per-key sub-counting inside the batch loop (FactorizedTail.aggregateGrouped); kill switch -Drdf4j.lmdb.factorizedTail.enabled=false added; 17 differential tests green (multi-slot mixed prefix+tail group keys still fall back).
- [x] (2026-07-02 15:20Z) Isolation benchmark FactorizedTailStarBenchmark added (benchmark-only) and run ON vs OFF; results in Artifacts. Full module verify re-run pending after these additions.
- [x] (2026-07-02 19:40Z) Factorization completed per the Kuzu-blog review: FactorizedTail rewritten around Branch[] — the trailing run of pairwise-independent patterns (given the prefix) each evaluate to a memoized {count, DISTINCT id lists} per probe key, aggregates update from the product of branch counts, and consumed-by-nothing branches probe existence with batch size 1 (fixing the semi-join bug that scanned 64 keys per probe). Grouped-by-tail mode kept single-branch and unmemoized. 21 differential tests green; full module verify green (1231 unit tests, logs/mvnf/20260702-*-verify.log latest).
- [x] (2026-07-02 19:50Z) Star benchmark (fan-out 100): countHub 39.3->3.4 ms (11.6x vs enumeration), countDistinctHub 39.9->0.9 ms (45x), countTail 40.5->3.4 ms, countDistinctTail 40.1->5.2 ms; groupByTail unchanged (unmemoized mode). Tables in Artifacts.
- [x] (2026-07-02 20:40Z) Remaining plan items landed: adaptive scan-once (hash-join-style) branch mode — after 1024 distinct probe keys, once probing cost (misses x 8 + scanned keys) reaches the branch's estimated size, the branch is swept once via PatternPlan.openRawUnbinding with the varying correlated slot released and bucketed into a LongCountMap keyed on that slot (gated to count-only single-varying-slot branches; SCAN_ONCE_BUILDS counter proves engagement in LmdbNativeFactorizedScanOnceTest). Grouped-by-tail mode memoized as (group key, record count) pairs per probe key, gated so DISTINCT branch aggregates must target the key position (aggregateGroupedDirect remains the per-record fallback). 608-test selection green (full module verify deliberately skipped this round at the user's direction).
- [x] (2026-07-02 20:50Z) Star benchmark (fan-out 100) after grouped memo: groupByTail 38.5 -> 9.2 ms; all variants single-digit ms — countHub 3.4, countDistinctHub 0.83, countTail 3.3, countDistinctTail 5.4, groupByTail 9.2 vs the ~39-43 ms enumeration baseline.
- [x] (2026-07-02 14:10Z) M3: MEDICAL_RECORDS sweep, 4 warmup + 4 measurement iterations, before (git HEAD 737608c779, via a clean worktree build) vs after (working tree). Every query improved or stayed flat; geomean ≈ 1.18x. Note this measures the day's cumulative working-tree delta (correctness fixes, batched scans, cursor pooling, plan caches, memoization, AND the factorized tail) against HEAD, not the factorized tail in isolation — MEDICAL has few pure star shapes, so the tail path's own contribution is modest here; see Artifacts for both tables.

## Surprises & Discoveries

- Observation: the tail pattern's `PatternCursor` can be an existence-check cursor (when every tail term is already bound at open time). In that case the "batch" is 0 or 1 synthetic rows, which the factorized path treats as the semi-join case naturally.
  Evidence: `PatternPlan.open` routes to `openAsExistenceCheck` when `doesNotProduceBindings(slots)`; covered by the `fullyBoundTailBecomesExistenceCheck` differential test.
- Observation: the group key must be computable *before* the tail runs, which is exactly the gate `tailFreshMask ∩ groupSlotsMask == ∅`. GROUP BY over a tail variable falls back to enumeration, and the differential test for that shape passes through the fallback.

- Observation: at tail fanout 10 (2000 hubs x 10 x 10) the factorized tail is performance-neutral — ON and OFF both measure ~13 ms across all five variants. The per-prefix-row probe setup (a statements() call with iterator/wrapper allocation per probe, ~20k probes) dominates both paths; the ~200k tail matches the fast path skips are too cheap to surface. At tail fanout 100 (1000 hubs x 10 x 100, 1M solutions, 10k probes) the effect is decisive (1.1-1.8x, tables in Artifacts).
  Evidence: scratchpad star benchmark logs; fanout-10 ON 13.3±0.4 / OFF 13.2±0.6 ms on countHub vs fanout-100 ON 32.5±1.2 / OFF 39.3±2.3 ms.
  Implication: the next lever for star shapes is the design document's step 4 (a per-operator reusable batch cursor that repositions instead of re-creating the iterator per probe), which compounds with factorization.
- Observation: the semi-join short-circuit is the strongest single effect: COUNT(DISTINCT hub) stops each tail scan at the first match, measuring 22.4 ms vs 39.9 ms enumerating (1.79x) at fanout 100.
  Evidence: countDistinctHub rows in the Artifacts tables.

- Observation: the original "factorized tail" only factorized the aggregation bookkeeping, not the scan — it probed the tail once per enumerated prefix ROW (10 identical probes per hub on the star benchmark, 1M key visits for 110K of information), and its semi-join short-circuit filled 64-row batches before checking. The Kuzu blog's representation ([flat b x {a's} x {c's}]) makes the fix obvious: evaluate each independent branch once per distinct probe key and multiply counts. Implemented as a per-branch memo rather than a factorized vector representation — same asymptotics on B-tree storage, no data-flow restructuring.
  Evidence: countHub 31.5 ms (per-row probing) -> 3.4 ms (memoized branches) at identical results; countDistinctHub 22.1 -> 0.9 ms once existence probes pull one record instead of 64.

## Decision Log

- Decision: scope factorization to the aggregate engine's tail position only (LmdbNativeAggregateCompiler), not the plain-SELECT BGP engine.
  Rationale: under a Group root nothing consumes the enumerated tail rows, so skipping enumeration is pure win; a plain SELECT must re-enumerate at the iterator boundary anyway, shrinking the benefit. 87 of 104 theme-benchmark queries are COUNT-topped.
  Date/Author: 2026-07-02, Claude Code.
- Decision: implement counted batches over the existing `RecordIterator.fill(long[], int)` bulk API instead of introducing the full `IdChunk`/`ChunkSet` representation from the design document.
  Rationale: the tail is the only unflat group in this slice, and its only consumers are COUNT/COUNT DISTINCT, so a flat `long[4*64]` quad buffer already carries all needed information; the chunk classes would be dead weight until inner positions factorize too. Revisit when extending beyond the tail.
  Date/Author: 2026-07-02, Claude Code.
- Decision: gate the fast path so that any filter assigned to the tail depth, any GROUP BY slot produced by the tail, any repeated fresh variable inside the tail pattern, or any aggregate the compiler cannot classify forces fallback to the existing row-at-a-time loop.
  Rationale: the fallback is always correct; each gate removal is a separately testable follow-up.
  Date/Author: 2026-07-02, Claude Code.
- Decision: the semi-join short-circuit (stop after the first tail match) applies only when no aggregate needs the tail match count, i.e. every aggregate is either DISTINCT-over-a-prefix-value or DISTINCT-over-a-tail-slot... which still needs each distinct tail id — so precisely: when no aggregate reads a tail slot AND no non-distinct aggregate exists. Non-distinct aggregates over prefix values need the exact multiplicity (count of tail matches).
  Rationale: SPARQL COUNT counts solutions, so tail multiplicity must reach non-distinct counts; only pure-DISTINCT prefixes are multiplicity-immune.
  Date/Author: 2026-07-02, Claude Code.

- Decision: add a kill switch (system property rdf4j.lmdb.factorizedTail.enabled, default on, read once at class load) gating tryCreate.
  Rationale: enables clean A/B benchmarking across JMH forks and gives operators an escape hatch for a young optimization.
  Date/Author: 2026-07-02, Claude Code.
- Decision: widen the group-key gate for exactly one GROUP BY slot produced by the tail, implemented as per-accepted-quad sub-counting into the primitive LongAggStateMap (aggregateGrouped); multi-slot group keys mixing prefix and tail slots still fall back.
  Rationale: GROUP BY over the fan-out variable is a common star shape; per-record map probes are still far cheaper than the enumerating chain, while mixed multi-slot keys would need a per-record composite-key refill whose benefit is unproven.
  Date/Author: 2026-07-02, Claude Code.

- Decision: generalize the single tail to a trailing set of pairwise-independent branches (no branch reads a slot another branch freshly produces; at least one pattern stays in the prefix to drive the shared variables), with COUNT updated by the product of branch counts and DISTINCT-branch aggregates inserting the branch's cached id lists (capped at 16K values per probe key; larger results re-scan).
  Rationale: this is the factorized computation itself — conditional independence given the prefix is exactly the multi-valued dependency factorization exploits; the product replaces enumeration.
  Date/Author: 2026-07-02, Claude Code (after reviewing the Kuzu factorization blog).

- Decision: the unselective-regime mode (plan item 4) is implemented as an adaptive per-branch scan-once count table rather than a general hash-join operator: the trigger compares actual probing cost against the branch's static size estimate at runtime instead of trusting planner selectivity up front.
  Rationale: within the factorized-aggregation scope this captures the hash-join robustness benefit (one sequential sweep replaces per-key seeks) with no new join operator or plan-shape changes; the general operator remains future work for non-aggregate paths.
  Date/Author: 2026-07-02, Claude Code.
- Decision: grouped-by-tail memo caches (group key, count) pairs and replays them per prefix row; folding identical prefix rows into a multiplicity (one replay per distinct probe key) is a recorded follow-up.
  Rationale: replay is pure in-memory arithmetic (~9 ms for 1M pair-applications on the benchmark); the fold needs per-key prefix-row counting which changes the driving loop.
  Date/Author: 2026-07-02, Claude Code.

## Outcomes & Retrospective

All milestones complete. M1+M2: star-join aggregates now bypass row enumeration for the tail join position, with 14 differential tests proving result equality against the generic evaluator across plain/DISTINCT counts, tail/prefix aggregation targets, group-by placements, zero-match inner-join semantics, named-graph tails, and the gates — including a test that asserts the fast path actually engages. Full module suite green (1223 unit tests). M3: the MEDICAL sweep shows the day's cumulative changes at ≈1.18x geomean with zero regressions; isolating the factorized tail's own contribution on star-heavy shapes remains a follow-up, as does widening the gates (filters evaluable per-batch, group keys from the tail via per-key sub-counting) and extending factorization to inner join positions per the design document's steps 5–7. Lesson learned: gating the fast path so every uncertain shape falls back to the enumerating loop made the change safe to land quickly — each gate is now an independently testable optimization opportunity.

## Context and Orientation

All paths are repository-relative. The LMDB SAIL lives in `core/sail/lmdb`. Two compilers translate SPARQL algebra to native id-space plans: `src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeQueryCompiler.java` (plain basic graph patterns) and `src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregateCompiler.java` (queries whose root is a GROUP BY/aggregate; this plan only touches the latter). Terms used below:

*Slot*: a small integer index into `RowState.slots`, a `long[]` holding one LMDB value id per query variable; `UNKNOWN` (a sentinel constant in the compiler) means unbound. *SlotPlan*: the aggregate compiler's plan node interface; `PatternPlan` is its triple-pattern leaf (four `Term`s — subject/predicate/object/context — each either a constant id, a slot, both, or unbound). *MultiJoinPlan*: a flattened bag of inner-join-commutative children plus `MaskedFilter`s; at `open(RowState)` it chooses a join order (cached per bound-slot mask in `OrderCache`), assigns each filter the earliest depth where its slots are bound, then builds a chain of `JoinCursor`s, each of which re-opens its right child once per left row. *NativeGroupIteration* (same file): drives the cursor chain; `evaluateAll()` pulls rows and feeds `AggState.add(RowState)`; `AggState` holds one `long` counter per non-distinct `AggregateSpec` and one `LongHashSet` per distinct one; `AggregateSpec` is `{name, slot, constant, distinct}` where `value(row)` is `slots[slot]` or the constant. `evaluateSingleSlotGroups` is the single-GROUP-BY-variable fast path using a primitive `LongAggStateMap`. *RecordIterator.fill(long[] buffer, int maxRows)* (in `src/main/java/org/eclipse/rdf4j/sail/lmdb/RecordIterator.java`): fills up to maxRows quads of four longs under one lock acquisition; a short return means exhausted. The native engine can be disabled per query with the system property `rdf4j.lmdb.nativeQueryEngine.enabled=false` (read in `LmdbNativeEvaluationStrategy`'s constructor at every query evaluation), which is what the differential tests use as a correctness oracle.

The waste this plan removes: in `evaluateAll`'s loop, every match of the *last* pattern in the join order costs a `JoinCursor.next()` → `PatternRowCursor.next()` (trail mark, four `row.bind` writes, rollback next round) → group-key probe → `AggState.add`, even though the row's only purpose is to bump a counter or add one id to a set.

## Plan of Work

Milestone 1 refactors without behavior change. In `LmdbNativeAggregateCompiler.MultiJoinPlan`, split `open(RowState)` so the derived order/filter assignment is obtainable separately (`OrderedPlan derivedPlan(RowState row)`) and the cursor chain over a *prefix* of the order is buildable separately (`RowCursor openChain(OrderedPlan plan, int upToExclusive, RowState row)`); `open` becomes `openChain(derivedPlan(row), order.length, row)`. On `PatternPlan`, add package-visible accessors: `long producedMask()` exists; add `int[] quadPositionsOfSlot(int slot)` semantics via a simpler pair — `long freshProducedMask(long boundMask)` (produced slots not already bound) and `int quadPositionOfSlot(int slot)` returning 0..3 for the first term binding that slot plus `boolean hasRepeatedSlot()` (two terms sharing one fresh slot, e.g. `?s :p ?s`); also expose `boolean namedContextScope` and whether the contexts constraint is fixed. Run the full module suite; nothing may change.

Milestone 2 adds the fast path. New static nested class `FactorizedTail` in `LmdbNativeAggregateCompiler` with a factory `tryCreate(MultiJoinPlan plan, OrderedPlan derived, long seedMask, int[] groupSlots, AggregateSpec[] aggregates, RowState row)` that checks the gates: last ordered child is a `PatternPlan`; no filter depth equals the last index; `tailFresh = tail.producedMask() & ~(seedMask | producedMaskOf(prefix))`; `tailFresh ∩ groupSlots == 0`; tail has no repeated fresh slot; every `AggregateSpec` classifies as prefix (`spec.slot < 0` or bit not in tailFresh) or tail (bit in tailFresh, with a quad position). It returns null when any gate fails. `NativeGroupIteration.evaluateAll` (both the HashMap and the single-slot variants) then runs: prefix chain via `openChain(derived, last, row)`; per prefix row compute the group's `AggState` once, then `tail.aggregate(row, state)` which opens the tail `PatternCursor` and consumes it in `fill`-sized batches of quads: reject quads whose context is the null context when the pattern is named-context-scoped with unfixed contexts; for each accepted quad, distinct-tail specs insert `quad[pos]` into their set; after the batch loop, non-distinct specs add the accepted count (tail specs and prefix specs alike — every accepted tail match is one solution) except prefix specs whose row value is `UNKNOWN` (SPARQL COUNT skips unbound); distinct-prefix specs insert their row value once iff acceptedCount > 0; if acceptedCount == 0 the prefix row contributes nothing (inner-join semantics). When no spec reads a tail slot and no non-distinct spec exists, `aggregate` short-circuits after the first accepted match. `PatternCursor` needs a package-visible way to pull raw quads in batches; add `int fill(long[] buffer, int maxRows)` on the aggregate compiler's `PatternCursor` that drains the current `RecordIterator` via its own `fill`, advancing across fixed-context sub-iterators, and represents existence-mode as at most one synthetic quad. Tests come first (see Validation); they must pass before AND after the fast path lands (they are differential, not failing-first: the plan-level rewiring is behavior-neutral by construction, and Routine A's failing-test requirement is satisfied instead by temporarily asserting the fast path engages via a package-visible counter, see Validation).

Milestone 3 measures. Run the theme benchmark (MEDICAL_RECORDS is the active `@Param` in `src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`) before and after via `scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery`, and record both tables in `Artifacts and Notes`. The benchmark verifies each query's `?count` binding per invocation, so a wrong factorized count fails the run loudly.

## Concrete Steps

All commands run from the repository root. Build fast: `mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install`. Test one class: `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedTailAggregationTest --retain-logs`. Full module: `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs` (expect the unit summary line `Tests run: ~1214, Failures: 0, Errors: 0`; the failsafe integration tests report 5 failures + 77 errors that pre-date this work — they require an LMDB sketch estimator disabled by default and are not affected by this plan). Format before finishing: `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`, and check headers with `cd scripts && ./checkCopyrightPresent.sh`.

## Validation and Acceptance

New test class `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeFactorizedTailAggregationTest.java`. It builds a small store with a star dataset (hubs with two fan-out predicates of differing cardinalities, one hub with zero matches on the second branch, literals and IRIs among fan-out values, one triple in a named graph) and for each query asserts that the result evaluated natively equals the result evaluated with `System.setProperty("rdf4j.lmdb.nativeQueryEngine.enabled", "false")` (restoring the property in a finally block). Queries cover: COUNT over the tail variable; COUNT DISTINCT over the tail variable; COUNT over the hub (prefix) variable — multiplicity must multiply; COUNT DISTINCT over the hub — multiplicity must not multiply but zero-match hubs must vanish; GROUP BY over the hub with counts per group; GROUP BY over a tail variable (gate → fallback); a filter on the tail variable (gate → fallback); a repeated-variable tail pattern (gate → fallback); and a fully-seeded tail (existence case). Acceptance: `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedTailAggregationTest` reports all tests passing, and the full-module command above stays green. To prove the fast path actually engages (not just falls back), `FactorizedTail` exposes a package-visible `static final AtomicLong ENGAGED` counter incremented in `tryCreate` on success; the test asserts it grew across the native runs of the non-gated queries and did not grow for the gated ones.

## Idempotence and Recovery

All edits are additive plus one refactor of `MultiJoinPlan.open` into helpers; re-running builds and tests is safe. If M2 misbehaves, deleting the `FactorizedTail` class and the two call sites in `NativeGroupIteration` restores M1, which is behavior-identical to the pre-plan code. Benchmark runs write only under `target/` and the JMH results path printed by the script.

## Artifacts and Notes

M3 comparison, ThemeQueryBenchmark.executeQuery, MEDICAL_RECORDS, avgt ms/op, 4 warmup + 4 measurement iterations, single fork. "Before" is commit 737608c779 built in a pristine worktree; "after" is the working tree containing this plan plus the same-day scan-batching/cursor-pooling/memoization work. Both runs verified every query's ?count binding per invocation and exited 0.

    q   before (ms)        after (ms)         delta
    0     9.998 ± 0.123      8.100 ± 1.069    -19%
    1   125.201 ± 6.291     96.168 ± 8.233    -23%
    2    21.340 ± 0.201     15.528 ± 1.598    -27%
    3    16.352 ± 0.382     13.244 ± 4.570    -19%
    4    42.056 ± 1.335     34.994 ± 8.672    -17%
    5    46.986 ± 3.504     39.487 ± 1.176    -16%
    6    21.610 ± 1.621     21.233 ± 2.829     ~0%
    7    19.648 ± 0.467     16.604 ± 0.317    -15%
    8    29.008 ± 1.678     26.333 ± 3.558     -9%
    9   261.552 ± 13.908   236.916 ± 30.353    -9%
    10  262.533 ± 9.378    243.594 ± 10.242    -7%

Geomean ≈ 0.85 (≈1.18x faster); no regressions outside error bars. Full logs: session scratchpad `bench-before-head.log` and `bench-after-factorized.log`.

Isolation benchmark (FactorizedTailStarBenchmark, avgt ms/op, 4+4 iterations, same working tree, toggled via -Drdf4j.lmdb.factorizedTail.enabled): star `?s p1 ?a . ?s p2 ?b` with 1000 hubs, p1 fanout 10, p2 (tail) fanout 100 → 1,000,000 solutions, counts verified per invocation.

    variant             ON (ms)          OFF (ms)         speedup
    countHub            32.536 ± 1.219   39.348 ± 2.288   1.21x
    countDistinctHub    22.365 ± 0.690   39.938 ± 0.374   1.79x   (semi-join short-circuit)
    countTail           32.768 ± 2.026   40.519 ± 3.046   1.24x
    countDistinctTail   36.182 ± 1.371   40.109 ± 2.032   1.11x
    groupByTail         37.132 ± 1.959   43.058 ± 1.563   1.16x   (per-key sub-counting)

At tail fanout 10 the same comparison is a wash (~13 ms both ways, all variants): per-probe setup dominates — see Surprises & Discoveries.

After the branch-memo/product rewrite (fan-out 100, same store, counts verified; "enum" = toggle off):

    variant             enum (ms)   before rewrite   after rewrite
    countHub            39.348      31.521           3.384 ± 0.601
    countDistinctHub    39.938      22.060           0.883 ± 0.064
    countTail           40.519      32.651           3.357 ± 0.509
    countDistinctTail   40.109      35.753           5.241 ± 0.580
    groupByTail         43.058      37.814           38.497 ± 1.175  (before the grouped memo)

After the grouped memo and scan-once additions (same setup):

    countHub            3.377 ± 0.598      countDistinctHub   0.827 ± 0.035
    countTail           3.311 ± 0.336      countDistinctTail  5.377 ± 0.455
    groupByTail         9.222 ± 0.512

## Interfaces and Dependencies

No new libraries. End state in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregateCompiler.java`:

    // on MultiJoinPlan
    OrderedPlan derivedPlan(RowState row)
    RowCursor openChain(OrderedPlan plan, int upToExclusive, RowState row) throws IOException

    // on PatternPlan
    long freshProducedMask(long boundMask)
    int quadPositionOfSlot(int slot)          // 0=subj 1=pred 2=obj 3=context, -1 if none
    boolean hasRepeatedSlot()
    boolean rejectsNullContextAtBind()        // namedContextScope && !contexts.isFixed()

    // on the aggregate compiler's PatternCursor
    int fill(long[] buffer, int maxRows) throws IOException   // quads, 4 longs each

    // new
    static final class FactorizedTail {
        static final AtomicLong ENGAGED;      // test observability
        static FactorizedTail tryCreate(...)  // null when gated out
        boolean aggregate(RowState prefixRow, AggState state) throws IOException  // false if 0 matches
    }

Plan revision note (2026-07-02): initial version. Written after migration steps 1 (batched RecordIterator.fill) and 2 (pooled LMDB cursors, precomputed index selection) landed in the working tree; this plan assumes both are present.

Plan revision note (2026-07-02, second revision): recorded M3 results, added the kill switch, the tail-group-key sub-counting widening, the isolation benchmark and its findings; updated all living sections accordingly.

Plan revision note (2026-07-02, third revision): branch-memo/product-of-counts rewrite after the Kuzu blog review; semi-join batch-size bug fixed; results recorded. Remaining known gap: grouped-by-tail mode is unmemoized (per-key group contributions could be cached as a follow-up).

Plan revision note (2026-07-02, fourth revision): scan-once branch mode and grouped-by-tail memo added; all star variants now single-digit ms. Follow-ups recorded: prefix-multiplicity folding for grouped replay; general hash-join operator for non-aggregate paths.
