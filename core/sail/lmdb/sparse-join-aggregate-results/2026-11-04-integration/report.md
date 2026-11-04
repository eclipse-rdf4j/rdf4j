# Sparse join aggregate integration: verification and measurements

**The local integration is complete within the configured native LMDB evaluator.** Sparse aggregation is available to normal planning, has physical cost and adaptive-selection support, retains the explicit opt-out, and handles the verified spillable-replay probe case. The final clean module build passed, followed by **447 focused and adjacent tests with zero failures, errors, or skips**. This is a targeted verification selection, not the complete RDF4J suite. The native evaluator and direct-adjacency store settings retain their existing defaults. See the [final build log](evidence/final-verification/sparse-integration-complete-install.log), [test log](evidence/final-verification/sparse-integration-complete-tests.log), and [feature contract](../../sparse-join-aggregate.md).

Performance is selective. In the strongest isolated case, hot weighted 32×32 input with median HAVING, automatic execution averaged **29.779 ms/op versus 134.816 ms/op** with sparse disabled: an observed **4.527×** ratio, close to forced sparse's 28.903 ms/op. Uniform 32×32 without HAVING showed an observed 1.362× ratio, with overlapping reported intervals. Costs remain visible in adverse cases: asymmetric 1×32 median HAVING had an observed 3.121-ms increase, and the longer-warmup disjoint-input control recorded 0.8991 versus 0.7205 ms/op, an increase of **0.1786 ms, or 24.8%**. The latter intervals overlap and fork means differ substantially. Normal integration therefore exposes useful selective acceleration while planning, learning, and route-selection overhead remain; it does not establish an optimal choice for every query. The [primary comparisons](evidence/final-jmh-primary-comparisons.csv) and [longer-warmup calculation](evidence/final-jmh-long-warmup-comparison.json) retain the unrounded figures.

All final measurements used the corrected implementation. Both the earlier `sparse-default-*` sweep and the later `sparse-final-*` sweep were run **before the replay/probe capacity defect was fixed**. The latter directory names reflected the intention at the time; they do not identify the completed feature. Their copies are explicitly labeled `pre-capacity-fix-provisional-final` and retained in the historical sections below.

Meeting the requested normal-integration requirement required changes beyond the original opt-in PR. This report covers the local completion on top of PR head `03a16b126371ff8cc523c7b68ee1f875d867f298`; that is the completion patch's base. The full feature patch starts from `ec7e48248eb20c4f7681d8ea5cbed3100718527a`. Final source and artifact identities accompany the delivery manifest. No commit, push, deployment, or publication is part of this work.

## Implementation and configuration boundary

Within the configured native LMDB evaluator, `sparseJoinAggregate` is available to ordinary aggregate planning without a sparse-specific enable property or a forced strategy. It has a finite physical cost quote when the required snapshot metadata is available, participates in adaptive selection and bounded specialist trials, publishes executed explanations, and retains an explicit `rdf4j.lmdb.sparseJoinAggregate.enabled=false` opt-out. Availability does not mean that every eligible query selects sparse. The public forced strategy remains a diagnostic control. See the [feature description](../../sparse-join-aggregate.md), [plan adapter](../../src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbSparseJoinAggregatePlan.java), and [automatic-query tests](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/SparseJoinAggregateAutomaticTest.java).

**A plain `LmdbStoreConfig` still defaults to the generic evaluator with direct adjacency disabled.** This integration does not change those store defaults or build a persistent adjacency index on demand. The benchmark configures native evaluation and direct adjacency, loads the fixture, and builds the index before running queries. The distinction matters when applying the feature to an existing repository. See [store configuration](../../src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java) and [benchmark fixture setup](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/SparseJoinAggregateBenchmark.java).

The implemented computation contracts weighted adjacency fibers directly into grouped counts. It supports the eligible two-pattern mandatory fragment, all four endpoint directions, predicate self-joins, `COUNT(*)`, `COUNT(1)`, proven-bound count variables and aliases, fixed graphs, and compiled dataset restrictions. It retains the complete outer HAVING expression. Unsupported algebra remains with existing alternatives. Context filtering occurs before multiplicities are formed. The [semantic tests](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/SparseJoinAggregateSemanticsTest.java), [admission tests](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/SparseJoinAggregateAdmissionTest.java), and [kernel tests](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/SparseJoinAggregateKernelTest.java) exercise these boundaries.

The physical recipe chooses a retained orientation from conservative memory bounds and uses that same recipe for pricing and execution. Scoped-context work and fixed context buffers are charged. Expected product and group estimates are separate from conservative upper bounds; unknown degree skew and overlap remain uncertainty, not cardinality facts. Sparse execution is blocking, so a downstream LIMIT does not discount aggregation and private replay setup. Adaptive keys distinguish meaningful size and fanout bands, while common family evidence remains available. See the [cost tests](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/SparseJoinAggregateCostTest.java) and [cost quote implementation](../../src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeAggregateCostQuotes.java).

Bounded physical-fiber copying avoids expanding stored multiplicities into quads where the positioned cursor certifies the operation. Context-filtered, merged, and multi-page cases retain the bounded quad path. The kernel charges workspace before allocation, checks cancellation during input and arithmetic, uses checked count arithmetic, and closes private output on a declined attempt. Kernel allocation refusal can decline a candidate; replay workspace denial retains the existing `QueryEvaluationException` behavior. This report does not claim that every resource failure causes transparent fallback. See the [adapter tests](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/SparseJoinAggregateAdapterTest.java) and [native query source](../../src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java).

### Replay/probe defect found after the provisional sweeps

The adapter's `SparseOutput` checked the **cumulative number of emitted groups** against the adaptive probe's private materialization row ceiling (`rdf4j.lmdb.adaptiveProbe.bufferRows`, default 4096). Its output is a separately accounted, spillable replay. Applying the private row-array ceiling to total replay output can stop a bounded sparse trial even when replay resident memory is within its own budget. That can prevent a trial from completing and contributing usable selection evidence. Ordinary forced execution of larger outputs was already observed; this was not a universal 4096-group limit on every sparse query.

The focused regression is `SparseJoinAggregateAdapterTest#probeCanCompleteSpillingReplayBeyondPrivateRowBuffer`. It requests 512 exact endpoint-pair groups with typed integer count one, a probe row ceiling of 8, and an 8192-byte resident replay budget. It checks all output groups, the memory bound, the untripped capacity flag, and resource release. Before the correction, that test failed with `KernelCancelledException` while materializing the replay: 1 test, 1 failure, 0 errors or skips. The [red report](evidence/capacity-fix-verification/sparse-integration-probe-capacity-red-report.txt) and [complete red log](evidence/capacity-fix-verification/sparse-integration-probe-capacity-red.log) are preserved.

The correction removes the unrelated cumulative-output cap while retaining cancellation/deadline checks, replay accounting, spilling, and cleanup. The [regression](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/SparseJoinAggregateAdapterTest.java) passed after the correction, with a recorded method duration of 0.054 seconds and no failure, error, or skip. It ran within the adapter class's 13 passing tests and the final selection's 447 passing tests. The [final Surefire summary](evidence/final-verification/surefire-summary.json) and [adapter report](evidence/final-verification/surefire-reports/org.eclipse.rdf4j.sail.lmdb.evaluation.SparseJoinAggregateAdapterTest.txt) provide that post-correction evidence; the earlier 446-test run does not.

## Final verification

The post-capacity-fix clean LMDB module install with the benchmark profile completed successfully in 1 minute 5 seconds. The subsequent focused and adjacent selection completed in 1 minute 44 seconds: **447 tests, 0 failures, 0 errors, 0 skips**. The preserved reports cover 20 test classes, including all six sparse classes, physical adjacency, aggregate explanations, adaptive arbitration, forced execution and fallback, cost estimates, expression handling, and output memory. This is a targeted selection, not a complete repository test run. The build skipped tests through its profile; the separate test command supplied the passing verification.

The recorded test command requested one fork and `-DreuseForks=false`. The effective configuration reused the fork: Surefire 3.6.1's module configuration explicitly enables reuse, and examined class XML files share the same booter command. No assertions were weakened. The original failed descriptor-baseline run and the independently invoked 69-test class remain available in the historical verification section. Exact executed argument vectors and log destinations are in the [verification command manifest](evidence/final-verification/commands.json); its `mvn-review.py` wrapper is local execution infrastructure outside the RDF4J source bundle.

| Check | Exact command / evidence | Result |
| --- | --- | --- |
| Replay/probe regression before correction | [Red log](evidence/capacity-fix-verification/sparse-integration-probe-capacity-red.log), [Surefire report](evidence/capacity-fix-verification/sparse-integration-probe-capacity-red-report.txt), and [command](evidence/final-verification/commands.json) | 1 test; 1 failure; 0 errors or skips |
| Same regression after correction, within the broader selection | [Method and class summary](evidence/final-verification/surefire-summary.json), [adapter report](evidence/final-verification/surefire-reports/org.eclipse.rdf4j.sail.lmdb.evaluation.SparseJoinAggregateAdapterTest.txt) | Regression passed; adapter class 13 tests, 0 failures/errors/skips |
| Final focused and adjacent selection, with fork reuse | [Complete log](evidence/final-verification/sparse-integration-complete-tests.log), [class summaries](evidence/final-verification/surefire-summary.json), [command](evidence/final-verification/commands.json) | 447 tests; 0 failures/errors/skips; BUILD SUCCESS |
| Final clean module benchmark-profile install | [Build log](evidence/final-verification/sparse-integration-complete-install.log), [command](evidence/final-verification/commands.json) | BUILD SUCCESS; tests verified separately |
| Summary/parser checks for the unchanged benchmark scripts | [Earlier check log](evidence/pre-capacity-fix-verification/sparse-benchmark-summary-tests.log) | 4 tests OK; no additional parser rerun claimed |
| Copyright and SPDX validation | [Final copyright check log](evidence/final-verification/copyright-check-final.log) | All checked files have valid copyright headers and SPDX identifiers |
| Final benchmark JAR identity | [Artifact record](evidence/final-verification/benchmark-artifact.json) | SHA-256 `2de560b99e7fd9cfdb5e71dfd02579e17935f488cc6d5d152298148bc7a0963f` |

The benchmark JAR is identified for the final measurements; its binary is not included in the source/evidence bundle. The accompanying delivery manifest records the final source bundle and patch identities. The [verification copy manifest](evidence/final-verification/copy-manifest.json) covers the completed build/test logs and all 20 plain-text Surefire reports. The structured summary records the relevant XML attributes and hashes without copying the XML system-property inventories.

## Definitive post-capacity-fix performance

The five bounded suites completed against the corrected source and the verified benchmark JAR identified above. Their original output directories are `target/sparse-integrated-{symmetric,asymmetric,hot-weighted,overlap,parallel}`. The [command manifest](evidence/final-macro/suite-commands.json), [build identity](evidence/final-macro/build-identity.json), and [copy manifest](evidence/final-macro/copy-manifest.json) accompany the preserved raw data. Runs were serial; `JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=4` limited the JVM's processor visibility. That option does not describe the number of physical host cores.

The five primary route-isolated JMH cases completed using independent route/fork processes, two forks, three one-second warmup iterations and five one-second measurement iterations. Final JSON files use `target/sparse-integrated-jmh-*.json`. Together they cover 15 route configurations and 150 measured JMH iterations. A longer-warmup disjoint follow-up adds two route configurations and 20 measured iterations. Each iteration executes repeated queries; these are not query counts. Exact arguments for all cases are in the [JMH command manifest](evidence/final-jmh/commands.json). The [post-measurement artifact check](evidence/final-measurement-artifact-check.json) confirms the benchmark JAR hash remained unchanged.

### Primary route-isolated JMH comparisons

Times are mean milliseconds per operation, followed by JMH's reported 99.9% score-error estimate. The two ratio columns divide the corresponding means; they are unpaired point estimates, not the bounded sweep's paired median ratios or confidence intervals for a speedup.

| Case | Disabled ms/op | Automatic ms/op | Forced sparse ms/op | Disabled / automatic | Automatic / forced | Raw evidence |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Uniform 1×1, multiplicity 1, no HAVING | 4.890 ± 1.219 | 4.402 ± 1.218 | 3.725 ± 1.287 | 1.111 | 1.182 | [Uniform JSON](evidence/final-jmh/uniform/results.json) |
| Uniform 32×32, multiplicity 1, no HAVING | 239.609 ± 39.444 | 175.974 ± 45.333 | 141.617 ± 24.218 | 1.362 | 1.243 | [Uniform JSON](evidence/final-jmh/uniform/results.json) |
| Hot 32×32, multiplicity 4, median HAVING | 134.816 ± 18.150 | 29.779 ± 3.566 | 28.903 ± 4.827 | 4.527 | 1.030 | [Hot JSON](evidence/final-jmh/hot/results.json) |
| Uniform 1×32, multiplicity 1, median HAVING | 37.186 ± 3.721 | 40.307 ± 7.204 | 40.153 ± 7.714 | 0.923 | 1.004 | [Asymmetric JSON](evidence/final-jmh/asymmetric/results.json) |
| Disjoint 8×8, multiplicity 1, empty HAVING | 1.595 ± 0.695 | 2.369 ± 1.243 | 3.405 ± 1.360 | 0.673 | 0.696 | [Disjoint JSON](evidence/final-jmh/disjoint-empty/results.json) |

The strongest isolated result is the hot, weighted 32×32 median-HAVING case: default automatic execution's mean was 105.037 ms lower than disabled automatic, an observed 4.527× ratio, and within 0.876 ms of forced sparse. Uniform 32×32 without HAVING had an observed 1.362× ratio and a 63.635-ms lower automatic mean, while its reported 99.9% intervals overlap. The low-fanout 1×1 case also has broad overlapping intervals and visible drift; this run does not establish a reliable low-fanout benefit. The asymmetric 1×32 case showed a 3.121-ms, 8.393% higher automatic mean, with substantially overlapping score intervals. Its observed penalty is retained alongside the gains.

The short disjoint-input run showed a 0.775-ms higher automatic mean, but all three routes became faster during the measured iterations. Disabled fork 0 moved from 2.117 to 1.071 ms/op between its first and last measured iterations; automatic fork 0 moved from 3.573 to 1.542 ms/op. The second forks also declined. The corresponding [per-iteration CSV](evidence/final-jmh-primary-iterations.csv) retains all 150 iteration averages, while the [score CSV](evidence/final-jmh-primary-scores.csv) and [comparison CSV](evidence/final-jmh-primary-comparisons.csv) retain unrounded values. This drift prevents interpreting the short-run difference as an established steady-state overhead.

The uniform, hot, and asymmetric console logs are the benchmark helper's compact risk-guard output: they retain parameters and timings, but not the printed trial-setup route strings. Their full underlying output file was overwritten between suites. The short disjoint case and its longer-warmup follow-up additionally retain [full short-run output](evidence/final-jmh/disjoint-empty/full-console.log) and [full longer-warmup output](evidence/final-jmh/disjoint-long-warmup/full-console.log). In the short run, both automatic trial-setup explanations executed sparse with zero multiply-adds and zero output groups, streaming `x` and retaining `y`; disabled explanations executed interpreted IR. These are separate setup explanations. Actual paths during every timed JMH invocation were not recorded, so the broader automatic-route claims rely on the preserved bounded-sweep witnesses and the automatic integration tests.

### Longer-warmup disjoint-input control

This completed follow-up compares `autoDisabled` and `auto` in two fresh forks per route, with ten two-second warmup iterations and five two-second measurement iterations. It addresses the remaining overhead question using a longer warmup and is retained alongside the short-run observations.

| Route | Mean ms/op | JMH 99.9% score error | Raw evidence |
| --- | --- | --- | --- |
| Disabled automatic | 0.7205 | ±0.0963 | [Longer-warmup JSON](evidence/final-jmh/disjoint-long-warmup/results.json) |
| Default automatic | 0.8991 | ±0.2438 | [Longer-warmup JSON](evidence/final-jmh/disjoint-long-warmup/results.json) |

The exact difference is 0.178572 ms/op, a 24.783% higher automatic mean; the disabled/automatic ratio is 0.801389. This residual observed cost remains in the result. The reported 99.9% intervals overlap: 0.6242–0.8169 ms/op for disabled and 0.6553–1.1429 ms/op for automatic. The disabled fork means were 0.6901 and 0.7510 ms/op; automatic's were 1.0410 and 0.7572 ms/op. Those heterogeneous independent forks do not establish one uniform steady-state overhead.

The recorded absolute difference was smaller with the longer warmup than in the short JMH run (0.774502 ms) or the bounded sweep (1.482–1.748 ms), but it was not eliminated. The [unrounded scores](evidence/final-jmh-long-warmup-scores.csv), [all 20 iteration averages](evidence/final-jmh-long-warmup-iterations.csv), and [comparison calculation](evidence/final-jmh-long-warmup-comparison.json) preserve the complete follow-up. The short-run evidence and its drift remain available; no favorable subset replaced it.

### Complete bounded diagnostic grid

The final grid contains all 27 cases, 810 warmed timing rows, 162 first-use timing rows, and 162 final witness rows. Times below are medians of the two per-fork warmed medians. Ratio ranges are calculated from matching forks, so they need not equal a ratio of the displayed median times. `I` means `irAggregate`, `Ii` means `irAggregateInterpreted`, and `S` means `sparseJoinAggregate`. A plus sign retains multiple observations around a warmed batch; it does not assign a route to every timed invocation. All cases use the dimensions in the methodology table.

| Case | Disabled ms | Automatic ms | Forced sparse ms | Disabled / automatic, fork range | Automatic / forced, fork range | Warmed automatic witnesses, fork 0 / 1 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Uniform 1×1, empty | 7.877 | 8.251 | 6.856 | 0.831–1.081 | 1.066–1.385 | I / Ii+S |
| Uniform 1×1, median | 10.317 | 12.020 | 10.111 | 0.663–1.157 | 1.119–1.240 | I / I+S |
| Uniform 1×1, none | 15.871 | 19.039 | 11.653 | 0.641–1.088 | 1.540–1.778 | I / I |
| Uniform 32×32, empty | 97.431 | 9.890 | 6.602 | 9.304–10.535 | 1.323–1.675 | S / S |
| Uniform 32×32, median | 179.572 | 108.909 | 63.187 | 1.212–2.504 | 1.166–2.280 | S / I+S |
| Uniform 32×32, none | 216.483 | 165.966 | 101.196 | 1.069–1.651 | 1.246–2.089 | S / I |
| Uniform 8×8, empty | 14.068 | 8.553 | 5.205 | 1.140–2.562 | 0.995–2.564 | I / S |
| Uniform 8×8, median | 59.342 | 63.009 | 55.215 | 0.838–1.074 | 0.903–1.438 | I / S |
| Uniform 8×8, none | 58.554 | 53.719 | 53.353 | 0.948–1.299 | 0.635–1.672 | S / S |
| Asymmetric 1×32, median | 38.527 | 36.401 | 34.052 | 0.984–1.141 | 1.050–1.087 | I / I |
| Asymmetric 1×32, none | 48.008 | 46.042 | 36.676 | 0.495–1.987 | 1.116–1.353 | I / I |
| Asymmetric 32×1, median | 44.719 | 41.152 | 29.443 | 0.867–1.338 | 1.379–1.415 | I / S |
| Asymmetric 32×1, none | 35.977 | 35.276 | 28.879 | 0.880–1.155 | 1.144–1.308 | I / S |
| Hot m4 32×32, empty | 115.723 | 10.463 | 9.207 | 9.690–12.681 | 1.067–1.231 | S / S |
| Hot m4 32×32, median | 103.002 | 67.740 | 31.601 | 1.142–2.348 | 1.169–3.469 | I+Ii / S |
| Hot m4 32×32, none | 130.372 | 48.510 | 43.556 | 2.289–3.129 | 0.855–1.530 | S / S |
| Hot m4 8×8, empty | 17.200 | 8.527 | 8.210 | 1.910–2.122 | 0.963–1.129 | S / S |
| Hot m4 8×8, median | 22.660 | 21.426 | 17.850 | 1.026–1.083 | 1.027–1.385 | I / I |
| Hot m4 8×8, none | 39.283 | 33.872 | 20.133 | 0.878–1.570 | 1.677–1.686 | I / S |
| Overlap 0% 8×8, empty | 4.858 | 6.473 | 6.295 | 0.734–0.763 | 0.955–1.091 | I+Ii / S |
| Overlap 0% 8×8, median | 6.763 | 7.067 | 6.904 | 0.917–1.013 | 0.934–1.099 | I / S |
| Overlap 0% 8×8, none | 5.616 | 4.891 | 6.025 | 1.129–1.177 | 0.715–1.030 | S / I+S |
| Overlap 25% 8×8, empty | 12.099 | 12.031 | 7.068 | 0.914–1.078 | 1.600–1.792 | I / I |
| Overlap 25% 8×8, median | 39.102 | 29.889 | 28.694 | 1.123–1.515 | 0.973–1.112 | I / I |
| Overlap 25% 8×8, none | 36.063 | 40.358 | 21.945 | 0.449–1.543 | 1.533–2.131 | I / S |
| Parallel enabled 1×1, none | 12.492 | 16.498 | 13.103 | 0.519–1.300 | 0.836–1.619 | I / I |
| Parallel enabled 32×32, none | 240.340 | 125.960 | 159.589 | 1.823–1.993 | 0.718–0.876 | S / S |

The [per-fork diagnostic CSV](evidence/final-macro-case-forks.csv) preserves exact warmed samples, first-use values, absolute differences, staged route witnesses, and orientation strings. The [case-summary CSV](evidence/final-macro-case-summary.csv) contains the unrounded summary values. These are derived views of the preserved raw data, with no sample removal.

Automatic execution had lower medians in both forks for 12 cases, higher medians in both for one case, and mixed signs for the other 14. This is a descriptive count of these selected fixtures, not an estimate of how often an arbitrary RDF query will benefit. Dense 32×32 inputs improved in both forks for all serial thresholds; automatic still sometimes remained on another witnessed route or changed route during the sequence. In particular, uniform 32×32 without HAVING showed sparse in fork 0 and IR in fork 1, leaving an automatic/forced gap of 1.246× and 2.089× respectively. Hot 32×32 median HAVING retained a 3.469× gap in the fork whose warmed witnesses were IR and interpreted IR, versus 1.169× in the fork with sparse witnesses. Automatic selection is therefore exercised but is not equivalent to forcing the sparse producer.

The one consistent adverse case was zero overlap, 8×8, empty HAVING. Automatic added 1.748 ms in fork 0 and 1.482 ms in fork 1, with paired disabled/automatic ratios of 0.763 and 0.734. Its warmed witnesses were IR/interpreted IR in one fork and sparse in the other. At zero overlap, `median` and `empty` both use threshold 1 and generate identical SPARQL, so their different results in this shared-history sweep cannot be attributed to different HAVING semantics. The isolated disjoint-input comparisons above test that exact query without the preceding sweep history and retain the residual observed cost. The historical asymmetric 1×32 median regression was not consistent in the final grid: automatic was 4.875 ms faster in one fork and 0.624 ms slower in the other. The isolated asymmetric result above records the additional observed penalty.

First-use results show why process history remains material. For uniform 1×1 without HAVING, disabled/automatic first-use times were 593.441/21.629 ms in fork 0 and 23.164/679.911 ms in fork 1: the large startup cost followed the automatic control run first in the process. In hot 32×32 median HAVING, automatic first-use times were 28.031/39.594 ms, while its warmed medians were 92.919/42.560 ms; the first fork's explanation changed from sparse to IR. These observations combine startup, adaptive decisions, and short-run variability. Fresh-process JMH measurements provide an additional comparison, and none of these first-use samples represents a filesystem-cache-cleared query.

The parallel-enabled high-fanout case favored automatic in both forks, but the sampled execution path was sparse in each. The data establishes behavior with competing parallel routes available, not parallel speedup. Physical orientation continues to follow the retained-state decision: the asymmetric 1×32 sparse control streams `y` and retains `x`, while 32×1 streams `x` and retains `y`; the full plan strings are preserved in the diagnostic CSV.

The replay/probe regression is verified fixed, and automatic execution can realize substantial sparse-aggregation gains without a forced strategy. The full bounded grid, primary isolated comparisons, and longer-warmup adverse control also show the limits: sparse is not best for every shape, default planning can retain a slower route, and candidate selection and learning have a cost. These measurements support normal selective integration, not a claim of universal speedup or regression-free timing.

## Measurement method

### Controls, timing, and validation

The three default controls run the same SPARQL text against the same loaded fixture:

| Benchmark route | Sparse-specific property | Selection |
| --- | --- | --- |
| `autoDisabled` | Explicit `false` | Ordinary automatic planning |
| `auto` | Cleared, using the production default | Ordinary automatic planning |
| `sparseJoinAggregate` | Explicit `true` | Public forced sparse strategy |

The normal automatic route is not implemented by disabling the new feature. No competing aggregate strategy is disabled to make that route choose sparse. The `parallel` setting applies equally to every route. Fresh query preparation and a disabled result cache prevent the preceding route's prepared result or compiled aggregate object from being reused as the next route's plan. Adaptive timing history can still influence subsequent automatic choices within a process. See the [harness](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/SparseJoinAggregateBenchmark.java) and [reproduction guide](../../../../../scripts/sparse-join-aggregate-benchmarks.md).

Every timed operation includes SPARQL preparation, optimization, per-execution sparse remapping and retained-plane construction when that route executes, aggregation, and full result draining. Fixture insertion, oracle construction, commit, and the explicit persistent adjacency build are recorded separately and excluded for every route. The reported cost is end-to-end query cost, not kernel-only time. Every query result is checked after the timing interval against an independent weighted oracle: each endpoint pair and integer count must match, with no duplicate or missing groups. Warmups and first-use queries also undergo result validation. JMH uses invocation teardown for that validation.

Executed explanations are separate queries after first use, before the warmed sample batch, and after that batch. They record actual route names and available sparse work and orientation telemetry. The last explanation has `phase=witness` and zero timings and is excluded from latency statistics. These observations establish what those explanation queries executed; **they do not instrument every timed invocation**. JMH's setup witness is also a separate execution, not a per-invocation route guarantee.

`sparseJoinAggregatePlan` identifies `streamBinding` and `retainedBinding` as well as the physical slots and directions. After transposition, kernel `leftRows` and `rightEndpoints` refer to streamed and retained roles. The summary's `observed_streamed_endpoints` and `observed_retained_endpoints` use those roles; its historical `observed_left_endpoints` and `observed_right_endpoints` fields remain deprecated aliases. A forced sparse reference supplies those physical dimensions even on control summary rows. The reference route and all witnessed dimension pairs are recorded, rather than assigning a written-query left/right meaning to a transposed plan.

### Bounded suites and statistical interpretation

Each historical sweep used 256 endpoints per nominal side, 512 join keys per relation, one endpoint block, two fresh JVM forks, two untimed warmups, and five timed warmed queries per route and case. The five suites covered 27 query cases:

| Suite | Degree / distribution / multiplicity | HAVING and overlap | Cases |
| --- | --- | --- | ---: |
| Symmetric | 1×1, 8×8, 32×32; uniform; multiplicity 1 | none, median, empty; full overlap | 9 |
| Asymmetric | 1×32, 32×1; uniform; multiplicity 1 | none, median; full overlap | 4 |
| Hot weighted | 8×8, 32×32; hot endpoints; maximum multiplicity 4 | none, median, empty; full overlap | 6 |
| Overlap | 8×8; uniform; multiplicity 1 | none, median, empty; 25% and 0% overlap | 6 |
| Parallel supplement | 1×1, 32×32; uniform; multiplicity 1; parallel enabled | none; full overlap | 2 |

Each complete sweep has 810 warmed timing rows, 162 first-use timing rows, and 162 separate final witness rows. The summaries report the median of the per-fork warmed medians. `automatic-comparison.csv` calculates disabled/automatic ratios within each fork before summarizing them. `comparison.csv` reports controls against forced sparse. Ratios above one favor the named candidate. The two-fork range is descriptive and is not a confidence interval. All recorded samples are retained.

Each loaded fixture is reused across routes and thresholds, with route order rotated between cases and forks. That controls fixture identity but does not erase JIT state or adaptive history. A first-use sample means the first query for that route/threshold on an already loaded, adjacency-built store. It is not cold-disk latency, and the JVM can be warm from earlier cases. Two warmups do not by themselves establish steady state. Fresh-process, route-isolated JMH comparisons provide a complementary control for persistent gaps.

The fixtures are deterministic synthetic graphs. Hot endpoints concentrate most keys in a restricted endpoint range while preserving the selected degree. Reduced overlap leaves unmatched input in both stored relations. Multiplicities arise from distinct named contexts, not duplicate insertion of an identical quad. Median HAVING uses the median positive exact count, so ties need not leave exactly half the groups. Empty HAVING uses one above the maximum exact count; an empty result does not establish that all multiplication was pruned. The raw CSV retains exact thresholds, expanded join cardinality, output groups, and density.

Parallel-enabled cases make the normal parallel competitors available with the same setting on all controls. They do not constitute a scaling study, and the existence of a parallel setting does not prove that a particular query used a parallel execution path. Exact commands, JVM information, CPU/container limits, source diff, route logs, and raw CSVs are retained for each suite.

## Historical verification evidence

These results precede the replay/probe correction. Counts describe the recorded test selections, not the complete RDF4J test suite.

| Selection or stage | Recorded result | Evidence |
| --- | --- | --- |
| Original PR focused and adjacent tests | 153 tests; 0 failures, errors, or skips | [Original review log](evidence/pre-capacity-fix-verification/review-tests.log) |
| Earlier integrated selection | 408 tests; 0 failures, errors, or skips | [Integration pass](evidence/pre-capacity-fix-verification/sparse-integration-green.log) |
| Physical grouping class in isolation | 69 tests; 0 failures, errors, or skips | [Grouping pass](evidence/pre-capacity-fix-verification/sparse-integration-grouping-isolated.log) |
| Later shared-process selection | 411 tests; 1 failure, 0 errors or skips | [Failed run, despite its original `final-green` filename](evidence/pre-capacity-fix-verification/sparse-integration-final-green.log) |
| Subsequent selection requesting `-DreuseForks=false`; per-class fresh forks not established | 446 tests; 0 failures, errors, or skips | [Pre-capacity-fix pass](evidence/pre-capacity-fix-verification/sparse-integration-final-isolated.log) |
| Python summary/parser checks | 4 tests; OK | [Summary check log](evidence/pre-capacity-fix-verification/sparse-benchmark-summary-tests.log) |

The shared-process failure was `NativePhysicalGroupingCostTest#idleSpilledOwnersDoNotRetainOneFileDescriptorPerGroup`: its exact process descriptor baseline was 140, while later observations were 139. The log preserves the failed assertion; a smaller descriptor count is not evidence of a leaked per-group descriptor. The subsequent 446-test run retained the assertions and passed. Its requested `-DreuseForks=false` must not be described as proven per-class isolation: the [module POM](../../pom.xml) explicitly sets `<reuseForks>true</reuseForks>`. Inspection during the later post-correction run confirmed identical Surefire booter commands for the direct-adjacency, sparse-adapter, and sparse-semantics classes despite a `reuseForks=false` system property. The [fork-identity observation](evidence/capacity-fix-verification/fork-identity-observation.json) preserves those values and source XML hashes. No per-class fresh-fork claim is made for either broad run. The separate invocation of the 69-test grouping class is the actual isolated-class result.

The development failures are preserved as well. They established missing behavior before the corresponding changes rather than being discarded once later runs passed:

| Regression area | First captured relevant failure |
| --- | --- |
| Admission without a sparse enable override | [1 error: strategy unavailable](evidence/pre-capacity-fix-verification/sparse-integration-admission-red.log) |
| Constant and bound-alias counts | [1 error: strategy unavailable](evidence/pre-capacity-fix-verification/sparse-integration-constants-red.log) |
| Finite physical quotes | [2 failures in 5 tests](evidence/pre-capacity-fix-verification/sparse-integration-cost-red.log) |
| Physical weights without quad expansion | [1 failure](evidence/pre-capacity-fix-verification/sparse-integration-fibers-red.log) |
| COUNT(1), orientation, blocking LIMIT, one-sided fanout | [4 failures in 17 tests](evidence/pre-capacity-fix-verification/sparse-integration-cost-contract-red.log) |
| Scoped-context quote, recipe, and scratch bounds | [3 failures in 4 tests](evidence/pre-capacity-fix-verification/sparse-integration-scoped-red.log) |
| Central estimates distinct from upper bounds | [2 failures](evidence/pre-capacity-fix-verification/sparse-integration-estimate-red.log) |
| Adaptive size and expansion keys | [2 failures in 22 tests](evidence/pre-capacity-fix-verification/sparse-integration-key-red.log) |
| Bounded specialist exploration | [1 failure in 23 tests](evidence/pre-capacity-fix-verification/sparse-integration-exploration-red.log) |

The [earlier broad contract run](evidence/pre-capacity-fix-verification/sparse-integration-contract-tests.log), [initial evidence snapshot](evidence/pre-capacity-fix-verification/initial-evidence.txt), and dependency/setup attempt logs are also retained. These failing runs are development history, not a substitute for the final pass.

## Historical diagnostics before the capacity correction

The values below are per-fork medians of five warmed samples, in milliseconds. A pair is fork 0 / fork 1. Both stages are historical. These comparisons motivated investigation; they must not be relabeled as final post-fix results.

| Case and stage | Disabled ms | Automatic ms | Forced sparse ms | Automatic / forced, per fork | Warmed explanation witnesses for automatic |
| --- | ---: | ---: | ---: | ---: | --- |
| Uniform 32×32, no HAVING — initial | 362.526 / 305.721 | 229.785 / 210.819 | 93.474 / 140.961 | 2.458 / 1.496 | `irAggregate` |
| Uniform 32×32, no HAVING — provisional final | 247.469 / 210.235 | 179.402 / 253.957 | 101.583 / 102.098 | 1.766 / 2.487 | `irAggregate` |
| Hot 32×32, multiplicity 4, median — initial | 102.764 / 121.001 | 106.828 / 118.431 | 26.474 / 30.611 | 4.035 / 3.869 | `irAggregate` |
| Hot 32×32, multiplicity 4, median — provisional final | 97.758 / 105.843 | 125.741 / 106.645 | 25.334 / 25.959 | 4.963 / 4.108 | `irAggregate` |
| Uniform 1×32, median — initial | 36.160 / 46.395 | 41.014 / 52.253 | 44.169 / 49.965 | 0.929 / 1.046 | `irAggregate` |
| Uniform 1×32, median — provisional final | 40.231 / 35.613 | 42.370 / 39.699 | 44.322 / 32.419 | 0.956 / 1.225 | `irAggregate` |
| Uniform 32×32, empty HAVING — initial | 97.711 / 103.719 | 10.952 / 11.978 | 9.089 / 8.090 | 1.205 / 1.481 | `sparseJoinAggregate` |
| Uniform 32×32, empty HAVING — provisional final | 126.150 / 121.691 | 9.588 / 8.501 | 7.450 / 7.132 | 1.287 / 1.192 | `sparseJoinAggregate` |

All 108 stage/case/fork comparisons, including adverse cases, appear in the [derived historical diagnostic CSV](evidence/pre-capacity-fix-case-diagnostics.csv). It was calculated directly from the copied `phase=warm` rows and includes disabled/automatic ratios, absolute millisecond differences, route-witness unions, and forced sparse orientation witnesses. The unchanged suite summaries and raw samples are linked in the evidence index below.

The 1×32 median case was the clearest repeatable small regression across the two historical sweeps: automatic added 4.854 and 5.858 ms initially, then 2.139 and 4.086 ms in the provisional sweep. The latter pair corresponds to approximately 5.3% and 11.5% overhead. Both warmed witness sets remained `irAggregate`; forced sparse was not uniformly better. That result motivated the isolated post-fix control reported above. The forced orientation streamed `y` and retained `x` for 1×32, while 32×1 streamed `x` and retained `y`.

Conversely, provisional uniform 32×32 empty HAVING had paired disabled/automatic ratios of 13.157 and 14.314, with sparse witnessed around the warmed samples. The broader dense-output cases still had large automatic/forced gaps. This contrast motivated investigation of probe completion and selection. The capacity defect supplies a concrete mechanism affecting bounded trials; the completed post-fix measurements quantify the subsequent behavior without attributing every historical timing difference to that one correction.

Some adverse samples were not persistent. In the provisional zero-overlap/no-HAVING case, one automatic fork recorded 68.174, 52.176, 27.792, 5.614, and 5.937 ms in order; the other automatic fork's median was 5.364 ms. The whole sequence is retained. Reporting only its 27.792-ms first-fork median as a steady-state regression would hide the warmup behavior. Low-fanout serial and parallel comparisons also changed sign between forks. See the [provisional overlap raw samples](evidence/pre-capacity-fix-provisional-final/overlap/fork-0.csv) and [complete diagnostics](evidence/pre-capacity-fix-case-diagnostics.csv).

### Historical route-isolated JMH control

A separate pre-capacity-fix JMH run used the hot 32×32, multiplicity-4, median-HAVING case with full overlap and parallel disabled. Each route received two fresh JVM forks, three one-second warmup iterations, and three one-second measurement iterations. JMH 1.37 reported average time with JDK 25.0.5+8-LTS, one benchmark thread, and `-Xms1G -Xmx3G`. The following are means and JMH's reported 99.9% score-error estimates, not the bounded sweep's median statistic:

| Route | Mean ms/op | JMH score error, ms/op |
| --- | ---: | ---: |
| Disabled automatic | 112.523 | ±37.699 |
| Default automatic | 27.471 | ±3.808 |
| Forced sparse | 30.357 | ±7.839 |

This isolated control showed that fresh-route execution could realize a sparse-sized improvement even while the shared-history sweep stayed on another witnessed route. It does not eliminate the capacity defect or establish the final performance. Preserve the [raw JMH JSON](evidence/pre-capacity-fix-isolated-jmh/results.json) and [console log](evidence/pre-capacity-fix-isolated-jmh/console.log) separately from subsequent measurements.

## Evidence index and reproduction

The copied evidence is byte-for-byte preserved. The [historical copy manifest](evidence/pre-capacity-fix-copy-manifest.json) records each original repository-relative path, copied path, byte count, and SHA-256. It covers 163 copied files totaling 6,400,170 bytes. The capacity regression's red evidence has a [separate copy manifest](evidence/capacity-fix-verification/red-copy-manifest.json). The derived case-diagnostic CSV is a separately labeled analysis of the historical copies; it is not an original raw file. Original `target` results and root logs remain in place. No database directories or benchmark JARs were copied.

| Evidence set | Entry points |
| --- | --- |
| Final post-capacity-fix verification | [Build log](evidence/final-verification/sparse-integration-complete-install.log), [447-test log](evidence/final-verification/sparse-integration-complete-tests.log), [exact commands](evidence/final-verification/commands.json), [Surefire summaries](evidence/final-verification/surefire-summary.json), [artifact identity](evidence/final-verification/benchmark-artifact.json), [copy manifest](evidence/final-verification/copy-manifest.json) |
| Final post-capacity-fix bounded grid | [Exact command arrays](evidence/final-macro/suite-commands.json), [build identity](evidence/final-macro/build-identity.json), [copy manifest](evidence/final-macro/copy-manifest.json); [symmetric](evidence/final-macro/symmetric/summary.md), [asymmetric](evidence/final-macro/asymmetric/summary.md), [hot weighted](evidence/final-macro/hot-weighted/summary.md), [overlap](evidence/final-macro/overlap/summary.md), [parallel](evidence/final-macro/parallel/summary.md) |
| Final primary JMH cases | JSON and compact logs: [uniform](evidence/final-jmh/uniform/results.json) / [log](evidence/final-jmh/uniform/console.log), [hot weighted](evidence/final-jmh/hot/results.json) / [log](evidence/final-jmh/hot/console.log), [asymmetric](evidence/final-jmh/asymmetric/results.json) / [log](evidence/final-jmh/asymmetric/console.log), [disjoint empty](evidence/final-jmh/disjoint-empty/results.json) / [log](evidence/final-jmh/disjoint-empty/console.log) / [full log](evidence/final-jmh/disjoint-empty/full-console.log) |
| Longer-warmup disjoint control and JMH provenance | [JSON](evidence/final-jmh/disjoint-long-warmup/results.json), [compact log](evidence/final-jmh/disjoint-long-warmup/console.log), [full log](evidence/final-jmh/disjoint-long-warmup/full-console.log), [all JMH commands](evidence/final-jmh/commands.json), [JMH copy manifest](evidence/final-jmh/copy-manifest.json), [final artifact hash check](evidence/final-measurement-artifact-check.json) |
| Initial pre-capacity sweep | [Exact command arrays](evidence/pre-capacity-fix-initial/suite-commands.json); [symmetric](evidence/pre-capacity-fix-initial/symmetric/summary.md), [asymmetric](evidence/pre-capacity-fix-initial/asymmetric/summary.md), [hot weighted](evidence/pre-capacity-fix-initial/hot-weighted/summary.md), [overlap](evidence/pre-capacity-fix-initial/overlap/summary.md), [parallel](evidence/pre-capacity-fix-initial/parallel/summary.md) |
| Provisional-final pre-capacity sweep | [Exact command arrays](evidence/pre-capacity-fix-provisional-final/suite-commands.json); [symmetric](evidence/pre-capacity-fix-provisional-final/symmetric/summary.md), [asymmetric](evidence/pre-capacity-fix-provisional-final/asymmetric/summary.md), [hot weighted](evidence/pre-capacity-fix-provisional-final/hot-weighted/summary.md), [overlap](evidence/pre-capacity-fix-provisional-final/overlap/summary.md), [parallel](evidence/pre-capacity-fix-provisional-final/parallel/summary.md) |
| Generator/oracle smoke check | [Commands](evidence/pre-capacity-fix-generator-smoke/commands.sh), [raw samples](evidence/pre-capacity-fix-generator-smoke/fork-0.csv), [log](evidence/pre-capacity-fix-generator-smoke/fork-0.log); validation only, not performance evidence |
| Additional traced diagnostic | [Commands](evidence/pre-capacity-fix-trace/commands.sh), [raw samples](evidence/pre-capacity-fix-trace/fork-0.csv), [trace log](evidence/pre-capacity-fix-trace/fork-0.log); diagnostic instrumentation, not primary performance evidence |
| Original opt-in PR measurements | [Original report](../2026-11-04/report.md), retained with its original route meaning |

Every suite directory also contains raw fork CSVs and logs, `automatic-comparison.csv`, `comparison.csv`, `summary.csv`, `commands.sh`, `environment.txt`, and the captured working-tree patch. Historical reports where `auto` disabled sparse retain that meaning; the current summarizer identifies their property setting as `legacy_false` rather than treating them as current default-enabled results.

Use the [benchmark guide](../../../../../scripts/sparse-join-aggregate-benchmarks.md), [runner](../../../../../scripts/run-sparse-join-aggregate-benchmarks.sh), and [summarizer](../../../../../scripts/summarize-sparse-join-aggregate.py) for reproduction. Keep builds, tests, and benchmark processes serial. When overriding both JMH `leftFanout` and `rightFanout`, pin the inherited `fanout` parameter to one value to avoid redundant Cartesian trials. A final source bundle is a partial repository overlay containing complete changed files; it still depends on the exact RDF4J checkout specified by its patch base.

These synthetic two-pattern measurements do not establish performance on a real RDF workload corpus, hardware portability, parallel scaling, cold storage, or AMX acceleration. They do not compare RDF4J against the external research implementation that inspired the sparse accumulation algorithm. The final interpretation must remain bounded by the exact fixtures, process history, and source identity recorded here.
