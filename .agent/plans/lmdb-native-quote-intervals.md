# Real cost intervals for the LMDB native strategy selector

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log` and `Outcomes & Retrospective` must be kept up to date as the work proceeds.

## Purpose / Big Picture

The LMDB native query engine picks an execution strategy for each decision point by pricing every admitted candidate. A price is a quote: a floor, an expected cost and a ceiling. Today those intervals say almost nothing. On the selection test corpus 58.6 % of quotes have no ceiling at all, and the finite ones are useless: the ceiling is 353 times the expected cost at the median and 1.7e21 times at the 99th percentile, because every join multiplies by the number of statements in the whole store.

After this work every candidate carries two intervals. Its cost vectors keep what can be proven, so a sampled row count can never cap a physical ceiling. The selector also reports the interval a run is estimated to land in, calculated from the expected price and from how far the statistics behind it can be off. A reader of `EXPLAIN` or of `-Drdf4j.lmdb.nativeDecisions.trace=true` sees a range the measured run actually falls in instead of `unbounded`. Success is observable with three numbers from the regret harness and the trace census described under Validation: the share of quotes with no estimated ceiling, the median ratio of ceiling to expected cost, and the share of measured runs that land inside their interval.

## Progress

- [x] (2026-11-05) Baseline census on the selection suites: 69,692 decisions, 11,574 distinct quotes, 58.6 % unbounded; finite high/expected p50 353, p90 3.3e7, p99 1.7e21.
- [x] (2026-11-05) Regret baseline at `9e1ceb0d55`, three of six configs, stopped there for time: on `spoc,posc` 11.8 % unbounded, 73.1 % of runs inside their interval, finite high/expected p50 3090, p99 1.9e37, 27 of 58 chosen within 5 % of the fastest.
- [x] (2026-11-05) First design, abandoned: estimates written into the vectors' `high`, through row bounds, access facts and route ceilings. It reached 0 % unbounded, ceiling over expected p50 6.2, p99 113 and 92 % of runs inside, and it broke the module: 40 tests that pin that a sampled count never caps a physical ceiling. See the Decision Log. The working tree of that attempt is in the session scratchpad only.
- [x] (2026-11-05) Row spreads: `LmdbNativeRowSpread` with independent errors added in quadrature, `LmdbNativeCostVectors.rowSpread`, `PatternPlan.rowSpread`.
- [x] (2026-11-05) Row counts for plans that keep algebra: `NativeGraphPlan` and `NativeSubqueryPlan` report `algebraRows`, and `LmdbNativeCostVectors.unpriced` costs them from those rows instead of a store sweep.
- [x] (2026-11-05) Estimated interval: `LmdbNativeSelector.estimatedInterval`, `Ranking.estimated`, `LmdbNativeDecisionRecord.Entry.estimated`, rendered in explain, trace and the JFR event. The vectors are as pushed in `9e1ceb0d55`.
- [x] (2026-11-05) Harness with the estimate recorded, `spoc,posc` and `spoc`: all 83 common decisions kept their winner; at ÷32 and ×3, 0 % with no ceiling, 98.2 % and 96.7 % of runs inside, ceiling over expected p50 6.7, p90 and p99 49.
- [x] (2026-11-05) Module on the two-interval design: 12,613 tests, 20 failures, all the known local `LmdbBufferedTripleTermTest`. Integration gate on the final constants: 153 tests, 0 failures, 70 skipped because they are `@Disabled`.

## Surprises & Discoveries

- Observation: every consumer of `LmdbNativeCostVectors.rowBounds` and `PatternPlan.rowBounds` in a quote only prices work; none drives memory admission or sizing. The conservative `SlotPlan.estimateWork` channel uses the separate `LmdbNativeWork.rowBounds`, so proofs and estimates can be separated without touching admission.
  Evidence: reading `LmdbNativeRowStep`, `LmdbNativeKernelHashCostQuote`, `LmdbNativeDistinctRowCostQuote`, `LmdbNativeParallelPipelines`.
- Observation: the store has exact per-predicate statement and key counts when adjacency serves the snapshot (`LmdbAdjacencyPlaneStatistics`) and an exact statement count with a 64-register HyperLogLog distinct estimate otherwise (`LmdbFanOutStats`). It has no maximum degree and no degree variance.

- Observation: after M1 to M3 only two plan kinds had no row estimate, `NativeGraphPlan` and `NativeSubqueryPlan`, because both keep an algebra tree until they open. Reading the optimizer's annotation, and falling back to the statement patterns' estimates, took the unbounded share from 13.0 % to 0.5 %.
  Evidence: temporary instrumentation in `rowBounds`, `logs-interval-spike.log`, `logs-interval-spike2.log`, and `logs-interval-trace5.log`.
- Observation: bespoke quotes such as `LmdbNativePackedFtreeQuote` count their work precisely from sampled row counts, so `packedFtree` quoted 152267..154589 ns with a row ceiling of 194 against 240 real rows. A near-point interval is as wrong as an infinite one, which is why the spread belongs to the input of a decision and not to each strategy.
- Observation: the first full-module run of the vector design failed 40 tests that the focused selection had not included. Those tests are the contract that a sample never caps a physical ceiling. Lesson: run every `*Bound*`, `*Sampled*` and `*Quote*` test before calling a quote change green.

- Observation: measured cost error barely depends on how wide the row estimate is. Grouping the first harness run by ceiling over expected, the 95th percentile of actual over expected stayed between 1.7 and 6.8 in every group, from ceilings of 1.5 times to over 1000 times expected, and the widest groups had no run above the ceiling. Multiplying spreads through a join was far too wide for deep plans, and the narrow groups had 13 to 23 % of runs above the ceiling.
  Evidence: `spread_vs_error.py` over `regret-intervals-1.tsv`.
- Observation: row spreads alone cannot reach coverage, because most misses are the price itself. `predicateHistogram`'s parallel aggregates quote 20 times their real time and `prefixRunGroups` measures 6.8 times its quote with exact counts. A band on the prices took coverage from 70 % to over 90 %.
- Observation: the prior errs high much further than low. At an upward model spread of 1 the held-out configs had no run above the ceiling, and at a downward spread of 4 coverage crossed 95 % there.

## Decision Log

- Decision: a quote has two intervals. The vectors' low and high stay what can be proven, and only those can eliminate a candidate. The selector separately reports the estimated interval, `expected / (rowDown × 32) .. expected × rowUp × 3`, limited by the proven ceiling, and that is what explain, the trace and the JFR event show.
  Rationale: the first design put estimates into `high`. That is the defect from the review of `d3d3349c1b` again, in a new form: a sample of 2 rows with 32 real rows gets a ceiling below the truth, and the module has 40 tests pinning the opposite (`LmdbNativeSampledCardinalityQuoteBoundTest`, `LmdbNativeFactorizedSampledPrefixBoundTest` and others). The harness also showed per-feature estimates were not needed: cost error barely depends on a plan's depth, so one spread around the expectation does the job with far less code. A report that can be wrong does no harm, because it decides nothing.
  Date/Author: 2026-11-05 / Claude Code
- Decision: the price-model spread is asymmetric, ÷32 below and ×3 above.
  Rationale: the prior errs high much further than low. Scaling the model spread scales the interval exactly, so these were read offline from the estimated-interval harness run: ÷4 and ×2 gave 88 % of runs inside, ÷16 and ×3 95 %, and ÷32 and ×3 98 %, on ceilings that stay at a median of 6.7 times expected. A floor at a hundredth of the expectation is weak, and it is what the measurements support.
  Date/Author: 2026-11-05 / Claude Code

## Outcomes & Retrospective

Every candidate now reports an interval calculated from the statistics, next to a proven interval that is unchanged. On the 3000-person harness, against the baseline at `9e1ceb0d55`: the share of quotes with no ceiling fell from 11.8 % to 0 %, the ceiling over expected cost from a median of 3090 to 6.7 and from a 99th percentile of 1.9e37 to 49, and the share of measured runs inside their interval rose from 73 % to 98 % on `spoc,posc` and 97 % on `spoc`. All 83 common decisions kept their winner, and every bound contract test is unchanged, because the vectors are.

The numbers are tuned on the data that reports them. The `spoc` configs were held out at first, but the final model spread was read from both sets, so nothing here is a true held-out result. The fixture is one synthetic FOAF graph, and the band will not transfer unchanged to skewed real data. The floor is weak, about a hundredth of the expectation at the median, because the prior over-quotes some parallel aggregates by that much.

The remaining misses are prices that are simply wrong, and a wider band would only hide them. `starTwoHop` on the `spoc` store is quoted at 8 ms and measured at 2.6 s, 320 times its quote, and `prefixRunGroups` measures 7 to 15 times its quote. Those belong to the prior and the quotes, not to this interval.

Reading statistics for the spread adds a little planning work per decision. It was not benchmarked.

## Context and Orientation

All paths are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/` unless stated. A plan (`SlotPlan`) is one node of the native physical plan: `PatternPlan` scans or probes one statement pattern, `MultiJoinPlan` and `JoinPlan` join, `LeftJoinPlan` is OPTIONAL, `MinusPlan` is MINUS, and `ExistsFilter` is a filter that runs a sub-plan. A bound mask says which row slots already have values; a pattern whose slot is bound by the join prefix is a probe, run once per prefix row.

`LmdbNativeCostVector` holds, per physical feature such as `INDEX_SEEK` or `SCANNED_ROW`, a low, expected and high count. `LmdbNativeCostQuote` is a startup vector and a streaming vector. `LmdbNativeStaticPrior` turns a vector into nanoseconds. `LmdbNativeSelector.rank` eliminates candidates and picks the lowest expected price among the rest.

`SlotPlan.estimateWork` and the vectors' high are the proven channel, and `LmdbNativeWork.rowBounds` and `LmdbNativeCostVectors.rowBounds` feed it. Many quotes have no proven ceiling, and that is correct: nothing the store keeps bounds the output of a join below the product of its inputs.

A q-error is the factor by which an estimate differs from the truth: `max(estimate / actual, actual / estimate)`. Errors of independent estimates multiply through a join, so a plan's spread is the product of its parts.

## Plan of Work

`LmdbNativeCostVectors.rowSpread(plan, row, mask)` returns a `LmdbNativeRowSpread`, the factors by which the plan's real row count may lie from `rows(plan)`. A pattern's spread comes from what backs its estimate: a sampled population, a mean fan-out or a structural pseudo-count. A probe's upward factor also stops at the most one key can hold, `quads − keys + 1`, so a functional predicate has none when adjacency gives an exact key count. Joins combine spreads in quadrature on a log scale, UNION takes the wider side, and filters, DISTINCT and MINUS have no floor because they can reject every row.

Each dispatch site passes its input's spread to `LmdbNativeOffers.inputSpread`, or `withInputSpread` at the ORDER site, and the quote carries it. `LmdbNativeSelector.rank` computes the estimated interval from the expected price, that spread and the prior's model spread. `NativeGraphPlan` and `NativeSubqueryPlan` keep an algebra tree until they open, so `algebraRows` reads the optimizer's annotated size, or the statement patterns' estimates, and `unpriced` costs them from those rows. Only a plan kind with no statistic at all keeps its proven interval.

## Concrete Steps

From the repository root, the focused selection suites run with `/opt/homebrew/bin/python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --workspace m5fix --threads 6 --retain-logs -- -DskipITs '-Dtest=...'`. The census adds `-Drdf4j.lmdb.nativeDecisions.trace=true -Dmaven.test.redirectTestOutputToFile=true` on `LmdbNativeGoldenDecisionsTest,LmdbNativeGeneratedQueryCoverageTest,LmdbNativeDifferentialFuzzTest`, then parses the `*-output.txt` files in the surefire report directory.

The regret harness runs with `--workspace bench --threads 1` and `-Dtest=LmdbNativeStrategyRegretHarnessTest -Drdf4j.lmdb.regret.enabled=true -Drdf4j.lmdb.regret.people=3000 -Drdf4j.lmdb.regret.measurements=5 -Drdf4j.lmdb.regret.output=<absolute path>`, and `scripts/lmdb-regret-report.py <tsv>` prints the comparison.

## Validation and Acceptance

Acceptance is measured against the baseline in `Progress`. The share of quotes with no estimated ceiling falls below 5 %, the median estimated ceiling over expected cost falls below 10 and the 99th percentile below 1000, and at least 95 % of harness measurements land inside their estimated interval. Every existing bound contract test stays green unchanged, `LmdbNativeGoldenDecisionsTest` keeps its winners, and all decisions common to the baseline keep their winner. To measure coverage the harness must record the estimate: `LmdbNativeDecisionTestAccess` captures `entry.nanos()`, so a run substitutes `entry.estimated()` there temporarily.

## Idempotence and Recovery

All steps are ordinary code edits and repeatable test runs. Harness output goes to a new absolute path each run. The strict channel is not touched, so reverting any milestone restores the previous quotes without affecting execution.

## Interfaces and Dependencies

No new dependency. New: `record LmdbNativeRowSpread(double down, double up)`, `static LmdbNativeRowSpread LmdbNativeCostVectors.rowSpread(SlotPlan, RowState, long)`, `LmdbNativeCostQuote.inputSpread()`, `LmdbNativeSelector.Ranking.estimated()` and `LmdbNativeDecisionRecord.Entry.estimated()`.
