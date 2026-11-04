# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | true | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 15.028 | 16.803 | 0.923× | 0.610–1.237× |
| 32×32 | uniform | 512/512 | true | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → auto | 205.906 | 170.517 | 1.245× | 1.072–1.418× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | true | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 15.028 | 12.114 | 1.328× | 0.794–1.863× |
| 1×1 | uniform | 512/512 | true | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 16.803 | 12.114 | 1.404× | 1.301–1.506× |
| 32×32 | uniform | 512/512 | true | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → sparseJoinAggregate | 205.906 | 110.590 | 1.865× | 1.686–2.044× |
| 32×32 | uniform | 512/512 | true | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | auto → sparseJoinAggregate | 170.517 | 110.590 | 1.548× | 1.189–1.907× |

