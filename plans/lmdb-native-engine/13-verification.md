# 13 — Verification, benchmarks, and rollout

Goal: every change in plans 01–12 lands against a fixed measurement and correctness harness, so
"production ready on the performance side" is a set of green gates, not an assertion. This plan is
built FIRST (Phase I) and never stops running.


## 1. Correctness harness

1. **Equivalence corpora** (the workhorse): for each subsystem, a generator producing (query, dataset)
   pairs where the native engine's results are compared as BAGS (and as sequences under ORDER BY,
   modulo equal-key ties) against the generic RDF4J evaluator — the module's established semantics
   oracle discipline (`LmdbNativeAggregatePlanner.java:404-408` javadoc). Corpora: filters (three-valued
   logic, unbound, NaN, mixed numerics, type errors), joins (duplicates, context/named-graph scoping,
   RDF-star terms), OPTIONAL/MINUS/EXISTS (scoping, disjoint domains), aggregation (distinct-but-equal
   literals, DISTINCT channels, float encounter-order), paths (cycles, diamonds, zero-length,
   duplicates), ORDER BY (error-ordering, DESC, expression keys). Seeded-random with fixed seeds in CI;
   nightly widened seeds.
2. **Dispatch-contract suite** (plan 01 §1): `LmdbNativeStrategyPriorityTest` grown into per-shape
   assertions of winning strategy + decline reasons. Every work item that changes dispatch lands with
   its before/after case. This is the regression tripwire for accidental strategy shifts.
3. **Plan-snapshot corpus**: the query-plan snapshot CLI (`testsuites/benchmark` tooling) over a fixed
   query set spanning every shape named in plans 01–12. Gate: a change may alter snapshots ONLY for its
   declared target shapes; any drift outside them fails the gate. Snapshots are committed alongside the
   change that legitimately moves them.
4. **Concurrency & lifecycle**: elastic-admission tests (plan 04 §1), cancellation propagation into
   every NEW loop (radix passes, frontier BFS, batched probes, accumulate, spill merges — each must
   observe close; test via early-close fuzzing), snapshot isolation under concurrent writers (CSR
   revision guards, memo scoping, hash-file revision guard), and the memory-ledger leak/ceiling
   harness (plan 12 §1).
5. **Baseline discipline**: the 24 pre-existing LMDB-only compliance failures are recorded as the
   frozen baseline before Phase I; the list must never grow. Full-module verify
   (`core/sail/lmdb`) green modulo that baseline is a merge condition for every item.


## 2. Benchmark suite

Existing: `ThemeQueryBenchmark` (join/filter shapes). Add, under
`core/sail/lmdb/src/test/java/.../benchmark/`, one benchmark class per axis — each with a small number
of `@Benchmark` methods so `scripts/run-single-benchmark.sh` (the supported harness: single method,
`benchmarks` profile, JFR option with fixed measurement discipline) targets them individually:

    OrderByBenchmark        full sort / top-K / radix-eligible / expression keys / order-eliminated
    ParallelismBenchmark    C∩D shape, concurrent-queries (2 and 4 simultaneous), slice+parallel
    AggregationBenchmark    many-groups, few-groups, DISTINCT aggregates, wide keys, spill-forced
    PathBenchmark           deep hierarchy, hub fan-out, alternation, cycle, VALUES-bounded targets
    RangeFilterBenchmark    numeric window shapes at 0.1%/1%/10% selectivity, date windows (v2 store)
    HashJoinBenchmark       probe-bound (build ≫ L2), build-side asymmetries, mark-join shapes
    CsrBenchmark            degree/count shapes, ordered CSR-served scans, SIP-from-CSR
    ScanBenchmark           full-scan decode throughput (varint wide-load), skip-scan density sweep
    MaterializationBenchmark  100k-row SELECT resolution (batched dictionary), RDF-star terms
    CorrelatedBenchmark     1000-outer × heavy-inner accumulate shape, path-under-join memoization

Datasets: synthetic generators with controlled skew (uniform / zipf hubs / all-integer predicates /
mixed-type predicates for the pushdown soundness gate), sized so the working set exceeds L2 but fits
the CI machine (plus a nightly XL size). Store variants: ordered-v1 and (after plan 08 §2) v2.

Methodology: paired before/after runs via `run-single-benchmark.sh`; comparisons through the
jmh-benchmark-compare tooling with a ±3% noise floor; JFR profiles attached for any item whose
acceptance cites a profile claim (allocation-free filters, batched-probe MLP, sort key-stride).
Regression gate: no benchmark outside a change's declared target set may regress >3%; target-set
improvements must meet the acceptance number named in the owning plan.


## 3. Acceptance ledger (program-level targets)

Aggregated from plans 01–12; each row is verified by the named benchmark and closed only with a paired
run linked in the ledger file (`plans/lmdb-native-engine/ACCEPTANCE.md`, created at Phase I start,
updated per landing):

    Two concurrent large BGPs each ≤0.6× sequential time     ParallelismBenchmark   (04§1)
    Filtered 2-pattern join ≥2× at 1M triples                ThemeQueryBenchmark    (02§2)
    C∩D shape ≥2× on 8 cores over batch-join baseline        ParallelismBenchmark   (01§3, 04§5)
    ORDER BY producer phase scales with cores                OrderByBenchmark       (05§1)
    Sort moves live columns only (3-4× fewer bytes)          OrderByBenchmark       (05§2-3)
    Radix-eligible ORDER BY ≥2.5×                            OrderByBenchmark       (05§4)
    Order-eliminated shape: zero sort                        OrderByBenchmark       (05§5)
    Range filter 0.1% selectivity ≥10×                       RangeFilterBenchmark   (08§1)
    Scan decode ≥15%                                         ScanBenchmark          (08§3)
    Probe-bound hash join ≥1.5-2×                            HashJoinBenchmark      (03§2, 09§1)
    NOT EXISTS 1M-outer ≥3×                                  HashJoinBenchmark      (09§2)
    Triangle: store-probe count at Σ-min-run scaling         CsrBenchmark/custom    (09§4)
    Correlated accumulate: 1000 executions → 1               CorrelatedBenchmark    (09§5)
    Path repeat-start memoization: N → D traversals          PathBenchmark          (11§1)
    COUNT(*) single pattern in µs                            CsrBenchmark           (07§1, 10§5)
    50M-group GROUP BY bounded-memory completion             AggregationBenchmark   (10§1)
    Ledger returns to zero after every corpus query          (leak harness)         (12§1)


## 4. Rollout switches and the flag ledger

Every behavior-risk item ships behind a system property defaulting ON with a documented kill switch
(pattern: `rdf4j.lmdb.native.<feature>=false`), EXCEPT items proven bit-identical (02§2's filter
wrap, 08§3's decode) which ship unguarded. The flag ledger (`ROLLOUT.md` beside the acceptance
ledger) records for every flag: default, measured justification, and removal date — a flag lives at
most two releases; then it is removed or its non-default state is justified in the ledger. The ledger
also closes out the INHERITED flags this program touches: `externalRootCandidateEnabled`
(plan 04 §3 — flip or delete), `valueHashCacheEnabled` (plan 12 §4 — flip or justify),
the specialization generator (plan 03 §5 — keep or delete by measurement), and the CSR/parallel/sort
tunables (retained, defaults revalidated on the benchmark corpus).

Telemetry in production builds: the execution-path tags, decline reasons, admission costs, cache hit
rates, ledger high-water marks — all through `LmdbNativeAttemptMetrics`' existing gated pattern, so a
production incident is diagnosable from counters without a profiler.


## 5. Program exit review

When all plans report acceptance met: one final adversarial pass re-running the composition analysis
(the 12×12 matrix) against the finished code — every cell that was `X`/`D` by ACCIDENT must now be
`++`/`+-` or carry a recorded cost-based justification; every cell that was correctness-required must
be UNCHANGED. That matrix diff, the acceptance ledger, and the flag ledger together are the
"production ready, nothing further needed" evidence — the review's output is the program's closing
document.
