# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → auto | 92.474 | 94.860 | 0.975× | 0.975–0.975× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → auto | 105.356 | 36.914 | 2.854× | 2.854–2.854× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → auto | 23.683 | 19.248 | 1.230× | 1.230–1.230× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → auto | 52.725 | 33.405 | 1.578× | 1.578–1.578× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → sparseJoinAggregate | 92.474 | 29.856 | 3.097× | 3.097–3.097× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | auto → sparseJoinAggregate | 94.860 | 29.856 | 3.177× | 3.177–3.177× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → sparseJoinAggregate | 105.356 | 34.279 | 3.073× | 3.073–3.073× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | auto → sparseJoinAggregate | 36.914 | 34.279 | 1.077× | 1.077–1.077× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → sparseJoinAggregate | 23.683 | 16.674 | 1.420× | 1.420–1.420× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | auto → sparseJoinAggregate | 19.248 | 16.674 | 1.154× | 1.154–1.154× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → sparseJoinAggregate | 52.725 | 15.901 | 3.316× | 3.316–3.316× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | auto → sparseJoinAggregate | 33.405 | 15.901 | 2.101× | 2.101–2.101× |

