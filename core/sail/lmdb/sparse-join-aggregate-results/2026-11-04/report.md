# Sparse join aggregate: implementation and measured results

Measured on 2026-11-04 UTC. Base: `HASMAC-AS/rdf4j` main at `ec7e48248eb20c4f7681d8ea5cbed3100718527a`.

## Decision

Keep `sparseJoinAggregate` opt-in. This implementation gives consistent high-fanout gains on the measured synthetic joins, while low fanout does not reliably benefit. The preselected high-fanout JMH case is 2.039× faster than the existing serial AUTO planner and 3.017× faster than the current packed factorized aggregate. The low-fanout JMH case is slower by the reported means and has substantial timing drift and overlapping error intervals. The bounded sweep also contains two low-fanout cases where packed beats sparse in all three forks.

These are measurements of RDF4J's Java implementation, including per-query remapping and setup. They are not reproductions of Tessera's C++ speedups. No AMX path or cross-query matrix cache is involved. See the [implementation notes](../../sparse-join-aggregate.md) for algorithm, eligibility, resource ownership, and source references.

## Validation

All **153 selected tests pass**, with zero failures, errors, or skips: 31 new tests and 122 existing tests covering neighboring dispatch, adjacency, aggregation, and memory behavior. The kernel's randomized test independently checks 120 weighted matrices at six thresholds, or 720 exact comparisons. The public SPARQL tests cover all four endpoint orientations, fixed graphs and datasets, context multiplicities, both self-join orders and diagonals, supported COUNT channels, unsupported/error/unbound fallback, and HAVING boundaries. Resource tests cover cancellation within a large context run, refusal after private groups have been buffered, complete memory release, and cleanup failure preservation.

[verification.txt](verification.txt) records the passing selection and commands. [admission-before.txt](admission-before.txt) preserves the pre-implementation failure. The project formatter and `git diff --check` pass. The full repository test suite was not run.

## Measurement design

The main sweep uses 256 configured endpoints on each side, 512 shared values, fanouts 1/8/32, one/eight endpoint blocks, maximum context multiplicities one/four, and no/median/empty-result HAVING thresholds. This gives 36 workloads, each measured by three routes in three fresh JVMs. Each route/workload records one first-use query, three verified warmups, and seven verified warmed samples. There are **2,592 retained raw query records**, including first-use samples.

The routes are AUTO with sparse disabled, forced `packedFtreeAggregate`, and forced `sparseJoinAggregate`. Native execution is enabled and parallel execution is disabled for every route. All routes execute the same `COUNT(?z)` query. Both mandatory patterns bind `?z`, so this is equivalent to `COUNT(*)`. The existing `factorizedTail` cannot group this shape by both endpoints. The packed serial path declines COUNT(*)'s visible-mapping reducer; the bound-variable form permits a valid comparison without changing baseline production code. A separate star-count sweep checks that form.

Timing starts before SPARQL preparation and ends after evaluation, complete result draining, and result-iterator closure. It includes optimization, sparse right-plane remapping, bounds and counter allocation, arithmetic, and output collection. Exact weighted-oracle comparison runs after the timer stops for every measured and warmup query. Output endpoint bindings, counts, cardinality, and duplicate groups are checked. Forced-route assertions fail if the intended strategy does not execute. AUTO explanation witnesses run separately around the samples and are not a per-invocation trace of adaptive plan choice.

Fixture insertion, oracle construction, commit, and the explicit persistent-adjacency build are outside query timing and recorded separately as `load_ns` and `adjacency_ns`. Store initialization is also excluded. No filesystem-cache eviction is performed. `phase=first` means first use of a route/threshold on an already loaded, adjacency-built fixture; earlier cases may have warmed code and filesystem state. This is not cold-start or cold-disk latency.

The machine exposes an AMD EPYC 9V74 CPU, nine vCPUs with an eight-CPU cgroup quota, and 8 GiB memory. Runs use Eclipse Temurin Java 25.0.5 and a 3 GiB maximum heap. Builds, tests, and other benchmark processes were stopped during measurements. Complete machine/JVM/cgroup information and the measured jar SHA-256 are in each run's environment file. This is one virtualized machine, with no CPU-affinity or hardware-portability claim.

## Main sweep: no HAVING

The table below fixes unit weights, then varies fanout and blocks. Density uses the configured 256×256 endpoint universe. The summary also reports density over observed endpoint domains. At low fanout both block settings remain similarly sparse; meaningful density comparisons are the fanout-8 and fanout-32 rows.

Sparse latency is the median of per-fork warmed medians. Speedup is the median of the three within-fork baseline/sparse ratios; values above one favor sparse. These speedups need not equal a quotient of separately displayed median latencies. All raw samples are retained, with no outlier removal.

| Fanout | Blocks | Max weight | Nominal density | HAVING | Retained groups | Sparse ms | AUTO/sparse, paired | Packed/sparse, paired |
| ---: | ---: | ---: | ---: | --- | ---: | ---: | ---: | ---: |
| 1 | 1 | 1 | 0.78% | none | 510 | 8.317 | 1.050× | 1.349× |
| 1 | 8 | 1 | 0.77% | none | 502 | 6.671 | 1.039× | 0.915× |
| 8 | 1 | 1 | 39.51% | none | 25,895 | 38.957 | 1.121× | 1.130× |
| 8 | 8 | 1 | 12.38% | none | 8,111 | 14.650 | 1.328× | 1.375× |
| 32 | 1 | 1 | 100.00% | none | 65,536 | 96.553 | 2.047× | 3.194× |
| 32 | 8 | 1 | 12.50% | none | 8,192 | 18.195 | 4.754× | 5.944× |

Across all four density/weight configurations without HAVING, fanout 8 gives 1.121–1.328× versus AUTO and 1.125–1.375× versus packed. Fanout 32 gives 2.008–4.754× versus AUTO and 3.015–5.944× versus packed. Lower output density at high fanout increases the number of contributions accumulated into each emitted group; the larger benefit is consistent with direct counters saving more work relative to output materialization, but this experiment does not isolate that cost.

At fanout 1, eight blocks, unit weights, and no HAVING, packed/sparse is 0.915×, with a three-fork range of 0.877–0.971×: sparse is about 9.3% slower by the paired median. The same fixture with median threshold one retains every group and has a 0.950× ratio, range 0.759–0.954×. The third packed loss (weighted, median threshold) has mixed fork outcomes.

Sparse wins 36/36 paired workload medians versus AUTO and 33/36 versus packed, but those counts mix very different output selectivities. They do not establish a universal improvement. An offline diagnostic that chooses the faster existing route inside each fork yields only 6/12 low-fanout wins and a median across those 12 workloads of 0.999×. That diagnostic is not an executable planner or a proposed cost model.

## Weighted HAVING cases

For the same fanout-32, eight-block fixture, the maximum weight of four produces nonuniform input weights from one through four. The threshold is derived from the exact generated counts; median counts can tie, so actual retention is reported.

| Fanout | Blocks | Max weight | Nominal density | HAVING | Retained groups | Sparse ms | AUTO/sparse, paired | Packed/sparse, paired |
| ---: | ---: | ---: | ---: | --- | ---: | ---: | ---: | ---: |
| 32 | 8 | 4 | 12.50% | none | 8,192 | 20.261 | 4.652× | 5.492× |
| 32 | 8 | 4 | 12.50% | ≥ 507 | 4,096 | 13.916 | 8.271× | 8.212× |
| 32 | 8 | 4 | 12.50% | ≥ 769 | 0 | 3.051 | 35.739× | 36.168× |

The sparse work counters distinguish multiplication pruning from other benefits:

| Fanout / blocks / max weight | Threshold | Multiply-adds | Pruned left rows | Pruned products | Output groups |
| --- | ---: | ---: | ---: | ---: | ---: |
| 32 / 1 / 1 | 21 | 524,288 | 0 | 0 | 0 |
| 32 / 8 / 1 | 65 | 0 | 256 | 0 | 0 |
| 32 / 8 / 4 | 769 | 0 | 256 | 0 | 0 |
| 32 / 8 / 4 | 507 | 262,144 | 128 | 0 | 4,096 |

The first row returns no groups but performs every multiply-add. Its 42.760× paired advantage over packed cannot be attributed to multiplication pruning. Avoiding materialization and subsequent filtering of rejected groups is a plausible contributor; no isolated time breakdown was measured. The following rows demonstrate complete and partial row pruning. Column-bound correctness is independently exercised by kernel tests, but these particular examples exhibit row pruning.

The largest main-sweep ratios, 47.279× versus AUTO and 47.650× versus packed, occur in empty-output cases. They are not representative no-HAVING speedups and should not be used as the feature's general performance claim.

## Preselected JMH checks

The supported `scripts/run-single-benchmark.sh` helper runs `completeQuery` for fanouts one and 32, one block, unit weights, no HAVING, and the same 256/512 domain sizes. Each strategy gets two fresh JVM forks, three one-second warmup iterations, and three one-second measurement iterations per fork. The exact oracle runs in invocation teardown outside JMH's measured interval. These are 12 independent JVM processes across six parameter cases.

| Fanout | AUTO, ms/op | Packed, ms/op | Sparse, ms/op | AUTO/sparse | Packed/sparse |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 3.159 ± 1.648 | 2.858 ± 1.744 | 3.353 ± 2.234 | 0.942× | 0.852× |
| 32 | 213.331 ± 29.214 | 315.696 ± 70.564 | 104.627 ± 5.323 | 2.039× | 3.017× |

The ± values are JMH-reported score errors. JMH ratios above are ratios of overall means from independently forked route runs, not paired-fork ratios. The high-fanout score intervals are well separated, and every sparse fork mean is below every baseline fork mean; this corroborates the bounded sweep's high-fanout gain.

The low-fanout means show sparse about 6.1% slower than AUTO and 17.3% slower than packed, with widely overlapping score intervals. Every low-fanout route/fork has a faster final measured iteration than its first, showing continuing timing drift. These short JMH checks establish no reliable low-fanout gain, rather than a precise stabilized penalty. The seven-sample bounded sweep likewise measures prior-execution behavior, not demonstrated JVM steady state.

## COUNT(*) supplement

The separate star-count sweep uses 64 endpoints, 128 shared values, fanouts one/eight, one/eight blocks, maximum weights one/four, and the same three HAVING categories. AUTO and sparse each run in two fresh JVMs, with three warmups and seven samples. All exact-oracle and forced-route checks pass: 24 workloads, 768 retained raw records. Sparse wins 23/24 paired workload medians versus AUTO, with ratios 0.944–2.191×. This supplement checks star-count execution and setup costs; its smaller fixtures must not be compared directly to the main sweep's absolute timings.

## Limits and reproduction

The inputs have uniform per-key fanout and deterministic neighbor intervals. Weight assignment correlates with block assignment; fanout, density, and multiplicity are experimental controls, not statistically independent random variables. All comparisons within a workload use exactly the same store, contexts, query, and answer oracle. Graph weights arise from distinct contexts; repeated insertion of an identical quad would be deduplicated and would not exercise multiplicity.

The adapter still scans raw represented incidences before folding weights. The kernel rebuilds its full right-plane representation per query, and result replay consumes memory proportional to emitted groups. Neither data-generation/index-build costs nor a cold storage lifecycle are included in query latency. Larger domains, skewed fanout, tighter memory budgets, other machines, parallel execution, and real RDF workloads can change the result. The new route remains disabled by default and has no fabricated cardinality-only cost estimate.

The [reproduction guide](../../../../../scripts/sparse-join-aggregate-benchmarks.md) describes every parameter and the supported JMH invocation. Exact executed commands are in [main/commands.sh](main/commands.sh), [jmh/command.sh](jmh/command.sh), and [star/commands.sh](star/commands.sh). Replace the recorded environment-specific Java executable path with your Java 25 installation.

- [Main paired comparison](main/comparison.csv), [main route summary](main/summary.csv), and [full readable table](main/summary.md).
- Main raw records: [fork 0](main/fork-0.csv), [fork 1](main/fork-1.csv), [fork 2](main/fork-2.csv).
- [Main environment](main/environment.txt) and [source snapshot at measurement](main/working-tree.patch.gz).
- [JMH JSON including all raw measurements](jmh/results.json) and [JMH console/route evidence](jmh/console.log).
- [Star-count paired comparison](star/comparison.csv), [star-count summary](star/summary.csv), and its [environment](star/environment.txt).

All correctness checks finished successfully before these artifacts were copied. No samples were removed or replaced. Archived text uses LF line endings with trailing whitespace normalized; parsed CSV/JSON records are unchanged. Source snapshots are losslessly gzip-compressed. Original raw files remain in the run directories.
