# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 4.858 | 6.473 | 0.748× | 0.734–0.763× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 6.763 | 7.067 | 0.965× | 0.917–1.013× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.616 | 4.891 | 1.153× | 1.129–1.177× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | autoDisabled → auto | 12.099 | 12.031 | 0.996× | 0.914–1.078× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | autoDisabled → auto | 39.102 | 29.889 | 1.319× | 1.123–1.515× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | autoDisabled → auto | 36.063 | 40.358 | 0.996× | 0.449–1.543× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 4.858 | 6.295 | 0.767× | 0.701–0.833× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 6.473 | 6.295 | 1.023× | 0.955–1.091× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 6.763 | 6.904 | 0.977× | 0.946–1.007× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 7.067 | 6.904 | 1.016× | 0.934–1.099× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.616 | 6.025 | 1.011× | 0.808–1.213× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 4.891 | 6.025 | 0.873× | 0.715–1.030× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | autoDisabled → sparseJoinAggregate | 12.099 | 7.068 | 1.698× | 1.463–1.932× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | auto → sparseJoinAggregate | 12.031 | 7.068 | 1.696× | 1.600–1.792× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | autoDisabled → sparseJoinAggregate | 39.102 | 28.694 | 1.361× | 1.249–1.473× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | auto → sparseJoinAggregate | 29.889 | 28.694 | 1.043× | 0.973–1.112× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | autoDisabled → sparseJoinAggregate | 36.063 | 21.945 | 1.661× | 0.957–2.364× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | auto → sparseJoinAggregate | 40.358 | 21.945 | 1.832× | 1.533–2.131× |

