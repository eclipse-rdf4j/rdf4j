# Harden the LMDB factorized execution paths: overflow, truncation, memo budgets, estimate validation, and engagement observability

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It must be maintained in accordance with `.agent/PLANS.md` (repository-relative path from the rdf4j repo root).

## Purpose / Big Picture

The LMDB triple store (module `core/sail/lmdb`) has a native query engine that evaluates SPARQL directly over 64-bit dictionary ids instead of materialized RDF values. Two of its execution strategies use "factorization": instead of enumerating every join solution row-by-row, they represent the trailing patterns of a join as per-key results of the form `{count, list-of-values}` and combine them arithmetically (a COUNT aggregate adds `product of branch counts` instead of iterating). The two strategies are the factorized SELECT sink (`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeFactorizedRows.java`) and the factorized aggregation tail (`.../LmdbNativeFactorizedTail.java` plus `.../LmdbNativeFactorizedTailBranch.java`).

This plan fixes correctness and robustness defects found in a source review of those paths, and makes their engagement observable. After this change:

1. A COUNT query whose true result overflows a signed 64-bit long fails with a clear `QueryEvaluationException` instead of silently returning a wrapped (wrong) number. Today `LmdbNativeFactorizedTail.aggregate` computes `product *= result.count` and `state.counts[k] += product` with no overflow guard, so a star join with four independent branches of 65,536 matches each returns COUNT = 0 (2^64 wraps to zero).
2. The factorized SELECT path uses the same overflow policy (hard error) instead of saturating multiplicity at `Long.MAX_VALUE`, which would make the row emitter repeat a row ~9.2e18 times — an effective hang.
3. Branch value collection cannot silently truncate through `int` casts on counts above 2^31.
4. All factorized memo caches share explicit budgets (entries and stored values), so pathological key cardinality degrades to re-scanning instead of unbounded heap growth.
5. The adaptive "scan-once" flip validates its cardinality estimate before trusting it.
6. Native explain output reports whether factorization engaged and how the memos behaved, so a regression that silently disqualifies factorization is visible.

## Progress

- [x] (2026-07-13) Source review completed; defects verified at the line level (see Context).
- [x] (2026-07-13) ExecPlan authored.
- [x] (2026-07-13 15:37Z) Milestone 1: failing test `LmdbNativeFactorizedOverflowTest#countProductOverflowFailsInsteadOfWrapping` captured red (query returned instead of throwing), fix applied (`multiplyCounts`/`addCounts` helpers in `FactorizedTail`, applied in `aggregate`, `applyGroupedPairs`, and `AggState.mergeCountsFrom`), test green; broader regression run pending confirmation.
- [x] (2026-07-13 16:00Z) Milestone 2 (revised, see Decision Log): saturation soundness comment added at `LmdbNativeFactorizedRows.Cursor.next`, LIMIT-bounded differential test `saturatedMultiplicityUnderLimitMatchesGeneric` added; post-green run pending.
- [x] (2026-07-13 16:15Z) Milestone 3 (revised, see Decision Log): array-cap guards with clear `QueryEvaluationException` in both scan loops (`LmdbNativeFactorizedRows.TailBranch.scan`, `Branch.scan`), long-safe index math, growth clamped to `MAX_COLLECTED_VALUES`; post-green run pending.
- [x] (2026-07-13 16:10Z) Milestone 4: `FactorizedTail.MemoBudget` (entries + cumulative stored values, `MEMO_BYPASSES` counter) shared across all branch memos and the grouped memo; rows-path memos already bounded, unchanged. Post-green run pending.
- [x] (2026-07-13 16:10Z) Milestone 5: scan-once flip now requires a finite positive `staticEstimate` (mirrors `PatternPlan.estimate` guard). Post-green run pending.
- [x] (2026-07-13 17:35Z) Milestone 6: `nativeExecutionStrategy` actual explain metric (LmdbNativeExplain.recordStrategy) recorded at every strategy-choice point in `NativeRowsIteration.initialize()` (prefixRun / orderedDistinct / batch / parallelPipelines / factorizedRows(...) / nestedLoop), the ORDER BY paths (orderedTopK / orderedSpillSort), and `NativeGroupIteration.evaluateAll()` (prefixRunGroups / parallelAggregation / factorizedTail(...) / orderedDistinctGroups / orderedSinglePatternGroups / aggState / singleSlotGroups / primitiveTupleGroups / hashGroups). Tests (red→green): `LmdbNativeQueryExplanationTest#telemetryExplanationShowsFactorizedRowsEngagement` and `...TailEngagement`. Metric surfaces at `Explanation.Level.Telemetry` (the framework copies generic actual string metrics only at that level; the `optimizer.` whitelist is not appropriate for a runtime metric).
- [x] (2026-07-13 17:50Z) Milestone 7: four fuzz suites added to `LmdbNativeDifferentialFuzzTest`: `factorizedStarProjections` (unprojected legs → multiplicity; in-branch filters), `factorizedStarAggregates` (count products, grouped-by-prefix and grouped-by-tail, HAVING), `slicesOverFactorizedRowsStayWithinFullResult` (size + sub-multiset properties — row equality is unsound for unordered slices), `reducedProjectionsAgreeOnDistinctSet` (REDUCED permits any dedup degree; compare distinct sets). Immediately found a real bug — see Surprises — fixed same session; pinned catalog regressions added. Final verification run pending.

## Surprises & Discoveries

- Observation: the two factorized implementations have asymmetric protections — the SELECT path saturates multiplicity overflow while the aggregation path has no guard at all, and the SELECT path caps memo entries while the aggregation path caps nothing.
  Evidence: `LmdbNativeFactorizedRows.java:262-266` (saturation) vs `LmdbNativeFactorizedTail.java:334,353` (unguarded); `LmdbNativeFactorizedRows.java:396-401` (entry cap) vs `LmdbNativeFactorizedTailBranch.java` `Branch.memo` (no cap).
- Observation: the new `factorizedStarProjections` fuzz suite found a real filter-semantics bug on its first run: the fully-compiled native filter path treated `?v != <numeric constant>` as a type error (row dropped) when `?v` was an IRI or blank node, while SPARQL `=`/`!=` fall back to RDF term equality whenever either operand is a non-literal — only literal pairs may error (`QueryEvaluationUtil.compareEQ/NE` lines 157-167/183-193). The `CachedCompareFilter` variant was unaffected because it falls back to the generic predicate on ERROR; only `LmdbNativeExpressionCompiler.compileCompare`'s pure-native truth path (used e.g. by factorized in-branch filters) dropped rows.
  Evidence: fuzz failure for `SELECT ?a ?v0 ?v1 { ?a p4 ?v0 . ... FILTER(?v0 != 3) }` — native missing all rows with `v0=<iri>` / `v0=_:bnode`; green after the fix across all 10 fuzz suites (≥400 generated queries).
  Fix: `LmdbNativeExpressionOps.compareValues` gained EQ/NE fallbacks — term equality when either side is a resource, delegation to `QueryEvaluationUtil.compareLiteralsEQ/NE` for natively-incomparable literal pairs (exact parity by construction, cost confined to the rare slow path) — and a `strict` flag threaded from the compilers' `strategy.getQueryEvaluationMode()`. Pinned regressions in the hot catalog (`FILTER(?o != 3)`, `FILTER(!(?o = 3))`, `FILTER(?o != "text1")`).

## Decision Log

- Decision: overflow of a COUNT product or accumulation raises `QueryEvaluationException` rather than falling back to the enumerating path or saturating.
  Rationale: a true count above 2^63 cannot be produced by any enumerating engine in practical time either (the generic evaluator would need 9.2e18 iterations), so a fallback is an unbounded hang, and saturation is a silent wrong answer. A clear error is the only honest outcome; the message names the aggregate and the overflow point.
  Date/Author: 2026-07-13 / Claude Code.
- Decision: the failing test loads 4 × 65,536 = 262,144 branch triples plus one prefix triple so the true COUNT is exactly 2^64 and the wrapped result is exactly 0.
  Rationale: this is the smallest data volume that makes the product overflow deterministic and the wrong answer unmistakable; loading ~262k triples in an LMDB test store takes seconds.
  Date/Author: 2026-07-13 / Claude Code.
- Decision: the SELECT path keeps multiplicity saturation instead of adopting the hard-error policy (Milestone 2 revised).
  Rationale: a saturated multiplicity only controls how many times an identical row is emitted. Any prefix of the emission stream an observer can actually consume (at most 2^63-1 rows) is identical under the saturated and the true multiplicity, so saturation is observationally sound; and under LIMIT the results are exactly correct. Making it throw would break LIMIT queries that work correctly today. The aggregation path is different in kind: it folds the product into a final scalar the user reads directly, so wrapping there is a silently wrong answer and must be an error. The asymmetry is now documented at the saturation site.
  Date/Author: 2026-07-13 / Claude Code.
- Decision: the int-truncation fix (Milestone 3) guards at the maximum array size and throws a clear `QueryEvaluationException`, instead of implementing a per-prefix-row streaming degrade.
  Rationale: below the array cap the current arrays work; at the cap the current code crashes with `ArrayIndexOutOfBoundsException`/`NegativeArraySizeException` (or OOMs first), never silently truncates — so a clean error is strictly better than today with zero regression risk, while a streaming degrade is real new machinery that the planned unified-pipeline rework (Phase 2 of the umbrella plan) replaces anyway.
  Date/Author: 2026-07-13 / Claude Code.
- Decision: disable `rdf4j.lmdb.parallel.enabled` inside the overflow test.
  Rationale: the parallel aggregation strategy would otherwise claim the query first (root estimate far above the 50,000 threshold) and the test targets the sequential factorized tail; the parallel path shares the same aggregate state code and is covered by the same guard.
  Date/Author: 2026-07-13 / Claude Code.

## Outcomes & Retrospective

2026-07-13, ALL MILESTONES COMPLETE. The factorized aggregation tail can no longer return silently wrong
COUNTs on overflow (it throws with a clear message; verified by a red-then-green test whose star join has a
true count of exactly 2^64), the SELECT path's saturation is documented as sound and locked by a LIMIT-bounded
differential test, both scan loops fail cleanly at the maximum array size instead of wrapping int indexes,
every memo on the aggregation side now draws from one bounded budget, and the scan-once flip no longer trusts
non-finite estimates. The executed strategy is now visible: `explain(Telemetry)` reports
`nativeExecutionStrategy` (e.g. `factorizedRows(flatPrefix=1, enumBranches=0, countBranches=1,
existsBranches=0)`) at every dispatch point of both the row and the group iterations, so silent
disqualification regressions are observable and assertable. Four new differential fuzz suites target the
factorized shapes specifically — and the first run of `factorizedStarProjections` immediately found and led to
the fix of a real filter-semantics bug (`?v != <constant>` dropped IRI/bnode rows in the fully-compiled native
filter path; see Surprises). Final verification: 132 surefire tests across 12 classes plus the module failsafe
ITs (theme regression 65/65) all green.

Lessons: (1) the two factorized implementations had drifted apart in exactly the ways the umbrella unification
plan predicts — every protection existed on one side and not the other; the unified-branch refactor (umbrella
plan Phase 1/2) will make this class of asymmetry impossible. (2) The differential fuzz harness paid for
itself within minutes of being extended; every future phase should extend it before touching the engine. (3)
Native fast paths that approximate SPARQL operator semantics need either exact-parity delegation on their slow
edge (as `compareValues` now does for incomparable literals) or a per-row generic fallback (as
`CachedCompareFilter` already had) — silent ERROR is not a safe default for EQ/NE.

Follow-up for the umbrella plan Phase 1: the fuzz slices suite covers LIMIT/OFFSET properties; the engagement
metric makes the planned sink-priority-inversion fix (factorized before batch) directly assertable.

## Context and Orientation

The LMDB sail lives in `core/sail/lmdb`. Its native engine compiles a SPARQL algebra tree into `SlotPlan` objects (`evaluation/LmdbNativeSlotPlan.java`) whose variables are integer "slots" (indexes into a `long[] slots` array of dictionary ids, class `RowState` in `evaluation/LmdbNativeRowState.java`). A `PatternPlan` is a triple-pattern scan; a `MultiJoinPlan` is a bag of inner-joined children evaluated as an index nested-loop join in a compiler-chosen order.

Factorized aggregation tail: `NativeGroupIteration.evaluateAll` (`evaluation/LmdbNativeGroupStep.java`, around line 257) may hand a GROUP BY/aggregate query to `FactorizedTail` (`evaluation/LmdbNativeFactorizedTail.java`). `FactorizedTail.tryCreate` (line ~147) splits the join order into a flat prefix and trailing "branches" — patterns whose fresh variables no later pattern reads. For each prefix row, `aggregate(RowState, AggState)` (line ~325) asks each branch for a `BranchResult {long count; long[][] values}` (memoized per probe key in `evaluation/LmdbNativeFactorizedTailBranch.java`), multiplies the counts into `product`, and adds `product` into the matching `AggState.counts[k]` slots. The defect: `product *= result.count` (line 334) and `state.counts[k] += product` (lines 353, 357) use unchecked long arithmetic.

Factorized SELECT: `LmdbNativeFactorizedRows` (same package) does the analogous thing for non-aggregate SELECT; COUNT-role branches multiply into a `multiplicity` field which the row emitter (`NativeRowsIteration.getNextElement`, `evaluation/LmdbNativeRowStep.java` line ~455) uses to repeat the projected row. Overflow currently saturates to `Long.MAX_VALUE` (lines 262-266).

"Memo" means a `HashMap<GroupKey, ...>` keyed on the ids of the slots a branch's result depends on. `GroupKey` (in `evaluation/LmdbNativeAggregateState.java`) wraps a `long[]` with a cached hash; callers keep one mutable probe key (`refill`) and store immutable copies (`storedCopy`).

Tests for these paths already exist and show the house style: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeFactorizedTailAggregationTest.java` (differential: native vs `rdf4j.lmdb.nativeQueryEngine.enabled=false`), `LmdbNativeFactorizedRowsTest.java`, `LmdbNativeFactorizedScanOnceTest.java`, `LmdbNativeDifferentialFuzzTest.java`. New tests go in the same package. Test commands use the repo's runner: from the repo root, `python3 .codex/skills/mvnf/scripts/mvnf.py <ClassName>#<method>`; a root quick install (`mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`) must have run first.

## Plan of Work

Milestone 1 (overflow, aggregation tail). Add a test class `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeFactorizedOverflowTest.java` with the repository's standard copyright header and the agent signature comment. The test builds an LMDB store with one prefix triple `ex:hub ex:p ex:a` and, for each of four predicates `ex:q1..ex:q4`, 65,536 triples `ex:hub ex:qi ex:o<j>` (object IRIs `ex:o0..ex:o65535`, shared across predicates). It evaluates `SELECT (COUNT(?s) AS ?c) WHERE { ?s ex:p ?a . ?s ex:q1 ?v1 . ?s ex:q2 ?v2 . ?s ex:q3 ?v3 . ?s ex:q4 ?v4 }` with `rdf4j.lmdb.parallel.enabled=false` and asserts the evaluation throws `QueryEvaluationException` whose message mentions overflow. Run it, observe it fail (the query currently returns `"0"^^xsd:integer`), capture the Surefire snippet. Then change `FactorizedTail.aggregate` (and its grouped variants `aggregateGrouped`/`aggregateGroupedDirect`/`applyGroupedPairs` where counts multiply or accumulate) to use `Math.multiplyExact`/`Math.addExact` wrapped so that `ArithmeticException` is rethrown as `QueryEvaluationException("COUNT overflow in factorized aggregation ...")`. Re-run; capture the passing snippet. Also re-run `LmdbNativeFactorizedTailAggregationTest` to show no regression.

Milestone 2 (overflow, SELECT multiplicity). Extend the same test class with a SELECT variant that would overflow multiplicity (same data, `SELECT ?s WHERE { ... }` with the four q-legs unprojected — each becomes a COUNT-role branch; multiplicity product 2^64) asserting `QueryEvaluationException` on iteration. Replace the saturation branch in `LmdbNativeFactorizedRows.Cursor.next` with the same exact-arithmetic policy.

Milestone 3 (int truncation). In `LmdbNativeFactorizedRows.TailBranch.scan` and `LmdbNativeFactorizedTailBranch.Branch.scan`, introduce a shared constant `MAX_COLLECTED_VALUES` (1 << 27 longs) checked before any `int` cast or array growth; on breach return the path's existing "too large" degrade (rows: bypass memo and enumerate directly for that prefix row; tail: the existing `TOO_LARGE` sentinel machinery). This is not reachable with test-scale data, so it is covered by a unit-level assertion test on the guard condition where feasible and otherwise documented as defensive; the behavior-visible part (no silent truncation) is asserted by code inspection in review plus the cap constant test.

Milestone 4 (memo budgets). Give `FactorizedTail`/its branches the same budget discipline `LmdbNativeFactorizedRows` has, and make both cumulative: a per-sink `MemoBudget {int maxEntries; long maxStoredValues; long storedValues; int entries}` consulted before every memo insert (rows `TailBranch.memo`, `PrefixDepth` quad memo, tail `Branch.memo`, `groupedMemo`). On exhaustion, skip insertion and increment the existing bypass counters. Defaults preserve current row-path limits (`MEMO_MAX_ENTRIES = 1<<16`, `MEMO_MAX_VALUES = 1<<20`).

Milestone 5 (estimate validation). In `LmdbNativeFactorizedTailBranch.Branch.result`, clamp `pattern.staticEstimate` to a finite non-negative value before the scan-once comparison and skip the flip when the pattern has runtime-bound slots (mirroring `PatternPlan.estimate(RowState)` semantics).

Milestone 6 (observability). The native engine exposes plan explain text (locate via `LmdbNativeEvaluationStrategy`/`NativeRowsStep.toString`, which already appends offset/limit around `LmdbNativeRowStep.java:250`). Append factorization engagement fields (engaged yes/no, branch count, memo entries/bypasses at close) to the step's explain/diagnostic string, and keep the existing static counters. The acceptance is that a test can assert the explain text mentions factorized engagement for a star query.

Milestone 7 (differential coverage). Extend `LmdbNativeDifferentialFuzzTest` generators with shapes that exercise the factorized paths specifically: star joins with unprojected legs (COUNT-role), DISTINCT and REDUCED projections (EXISTS-role), OFFSET/LIMIT with multiplicities, filters assigned to branch depths. Keep runtimes bounded (seed-stable, small stores).

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{summary=1} summary{print}'
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedOverflowTest#countProductOverflowFailsInsteadOfWrapping
    # expect FAIL before the fix: evaluation returns 0 instead of throwing
    # after the fix:
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedOverflowTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedTailAggregationTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedRowsTest
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources   # formatter before finalizing

## Validation and Acceptance

Milestone 1 acceptance: `LmdbNativeFactorizedOverflowTest#countProductOverflowFailsInsteadOfWrapping` fails before the production change with an assertion error showing the query returned `0` (Surefire report under `core/sail/lmdb/target/surefire-reports/`), and passes after, with the thrown `QueryEvaluationException` message containing "overflow". `LmdbNativeFactorizedTailAggregationTest` (18 tests) stays green. Subsequent milestones follow the same pattern: each behavior change lands with a test that fails before and passes after; budget/estimate milestones may use Routine B (behavior-neutral, hit-proof) where results cannot differ on test-scale data.

## Idempotence and Recovery

All steps are additive and re-runnable. Test data is built per-test in JUnit `@TempDir` stores. If a run leaves stale artifacts, `mvnf` deletes module test artifacts before running. If the offline install fails on a missing dependency, rerun the same command once without `-o`, then return offline.

## Artifacts and Notes

Milestone 1 pre-fix failing evidence (also persisted as repo-root `initial-evidence.txt`):

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedOverflowTest#countProductOverflowFailsInsteadOfWrapping
    Summary: tests=1, failures=1, errors=0, skipped=0, time=2.013s
    Failure: countProductOverflowFailsInsteadOfWrapping: Expecting code to raise a throwable.
    (the COUNT silently returned 0 — the wrapped value of 2^64)

Milestone 1 post-fix passing evidence:

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeFactorizedOverflowTest#countProductOverflowFailsInsteadOfWrapping
    Summary: tests=1, failures=0, errors=0, skipped=0, time=1.692s

Regression selection (same eight classes, run before and after each production change; all green):

    -Dtest=LmdbNativeFactorizedTailAggregationTest,LmdbNativeGroupByTest,LmdbNativeParallelAggregationTest,
           LmdbNativeCountStarTest,LmdbNativeFactorizedOverflowTest,LmdbNativeFactorizedRowsTest,
           LmdbNativeFactorizedScanOnceTest,LmdbNativeDifferentialFuzzTest
    Run 1 (post Milestone 1, pre Milestones 4-5): total tests: 97 failures: 0 errors: 0
    Run 2 (post Milestones 2, 4, 5): total tests: 98 failures: 0 errors: 0
    Run 3 (post Milestone 3 + formatter): surefire 98 tests, 0 failures, 0 errors; the module's
    failsafe ITs also ran green in the same verify, including LmdbThemeQueryRegressionIT (65 tests, 582s)
    and the theme/pharma snapshot ITs — broad end-to-end coverage over the factorized paths.
    Run 4 (final; post Milestones 6-7 + compare-semantics fix): 12 test classes, surefire 132 tests,
    0 failures, 0 errors, plus the module failsafe ITs green (exit 0). Selection added
    LmdbNativeQueryExplanationTest, LmdbNativeExpressionFilterTest, LmdbNativeLeftJoinFilterRewriteTest,
    LmdbNativeAggregateFilterSemanticsTest to the standing eight classes.

Milestone 7 pre-fix failing evidence (the fuzz-found compare bug):

    factorizedStarProjections -- FAILURE: native vs generic for
    SELECT ?a ?v0 ?v1 WHERE { ?a ex:p4 ?v0 . ?a ex:p1 ?v1 . ?a ex:p2 ?v2 . ?a ex:p3 ?v3 . FILTER(?v0 != 3) }
    but could not find the following elements:
      {a=ex:s3, v0=ex:s5, v1=ex:s1}, {a=ex:s3, v0=_:b14, v1=ex:s1}, ...
    (native dropped every row whose ?v0 was an IRI or blank node)

Hit Proof for the Routine-B changes (budget, estimate guard): `LmdbNativeFactorizedScanOnceTest` drives
`Branch.result` through memo misses into the scan-once flip; `LmdbNativeFactorizedTailAggregationTest`
inserts into branch memos and the grouped memo on every star query; both are in the selection above.

## Interfaces and Dependencies

No new libraries. New test class `org.eclipse.rdf4j.sail.lmdb.LmdbNativeFactorizedOverflowTest`. Production edits confined to `evaluation/LmdbNativeFactorizedTail.java`, `evaluation/LmdbNativeFactorizedTailBranch.java`, `evaluation/LmdbNativeFactorizedRows.java`, and the explain surface in `evaluation/LmdbNativeRowStep.java`/`LmdbNativeGroupStep.java`. Exception type: `org.eclipse.rdf4j.query.QueryEvaluationException` (already used throughout the package).
