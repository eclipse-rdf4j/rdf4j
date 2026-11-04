# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | autoDisabled → auto | 81.413 | 10.078 | 8.162× | 7.497–8.828× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → auto | 101.801 | 116.193 | 0.885× | 0.777–0.992× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → auto | 126.603 | 84.477 | 2.166× | 0.866–3.465× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | autoDisabled → auto | 19.147 | 8.135 | 2.355× | 2.118–2.593× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → auto | 23.425 | 24.891 | 1.008× | 0.652–1.364× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → auto | 38.355 | 34.296 | 1.139× | 0.922–1.355× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | autoDisabled → sparseJoinAggregate | 81.413 | 8.893 | 9.192× | 8.367–10.016× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | auto → sparseJoinAggregate | 10.078 | 8.893 | 1.142× | 0.948–1.336× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → sparseJoinAggregate | 101.801 | 25.646 | 3.968× | 3.859–4.077× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | auto → sparseJoinAggregate | 116.193 | 25.646 | 4.536× | 4.108–4.963× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → sparseJoinAggregate | 126.603 | 42.128 | 3.073× | 2.391–3.755× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | auto → sparseJoinAggregate | 84.477 | 42.128 | 1.922× | 1.084–2.760× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | autoDisabled → sparseJoinAggregate | 19.147 | 6.863 | 2.784× | 2.613–2.955× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | auto → sparseJoinAggregate | 8.135 | 6.863 | 1.187× | 1.140–1.234× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → sparseJoinAggregate | 23.425 | 19.989 | 1.170× | 0.972–1.369× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | auto → sparseJoinAggregate | 24.891 | 19.989 | 1.247× | 1.003–1.491× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → sparseJoinAggregate | 38.355 | 24.966 | 1.570× | 1.254–1.885× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | auto → sparseJoinAggregate | 34.296 | 24.966 | 1.375× | 1.359–1.392× |

