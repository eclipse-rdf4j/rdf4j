# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | autoDisabled → auto | 115.723 | 10.463 | 11.186× | 9.690–12.681× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → auto | 103.002 | 67.740 | 1.745× | 1.142–2.348× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → auto | 130.372 | 48.510 | 2.709× | 2.289–3.129× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | autoDisabled → auto | 17.200 | 8.527 | 2.016× | 1.910–2.122× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → auto | 22.660 | 21.426 | 1.054× | 1.026–1.083× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → auto | 39.283 | 33.872 | 1.224× | 0.878–1.570× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | autoDisabled → sparseJoinAggregate | 115.723 | 9.207 | 12.974× | 10.343–15.605× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | auto → sparseJoinAggregate | 10.463 | 9.207 | 1.149× | 1.067–1.231× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → sparseJoinAggregate | 103.002 | 31.601 | 3.352× | 2.744–3.960× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | auto → sparseJoinAggregate | 67.740 | 31.601 | 2.319× | 1.169–3.469× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → sparseJoinAggregate | 130.372 | 43.556 | 3.090× | 2.677–3.503× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | auto → sparseJoinAggregate | 48.510 | 43.556 | 1.193× | 0.855–1.530× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | autoDisabled → sparseJoinAggregate | 17.200 | 8.210 | 2.100× | 2.044–2.156× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | auto → sparseJoinAggregate | 8.527 | 8.210 | 1.046× | 0.963–1.129× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → sparseJoinAggregate | 22.660 | 17.850 | 1.277× | 1.054–1.499× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | auto → sparseJoinAggregate | 21.426 | 17.850 | 1.206× | 1.027–1.385× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → sparseJoinAggregate | 39.283 | 20.133 | 2.056× | 1.481–2.632× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | auto → sparseJoinAggregate | 33.872 | 20.133 | 1.682× | 1.677–1.686× |

