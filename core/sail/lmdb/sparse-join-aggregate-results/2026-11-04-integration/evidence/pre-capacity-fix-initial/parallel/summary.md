# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | true | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 16.158 | 15.694 | 1.036× | 0.822–1.250× |
| 32×32 | uniform | 512/512 | true | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → auto | 200.307 | 172.537 | 1.315× | 0.905–1.725× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | true | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 16.158 | 12.471 | 1.294× | 1.074–1.513× |
| 1×1 | uniform | 512/512 | true | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 15.694 | 12.471 | 1.259× | 1.211–1.307× |
| 32×32 | uniform | 512/512 | true | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → sparseJoinAggregate | 200.307 | 118.998 | 1.691× | 1.499–1.884× |
| 32×32 | uniform | 512/512 | true | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | auto → sparseJoinAggregate | 172.537 | 118.998 | 1.475× | 0.869–2.081× |

