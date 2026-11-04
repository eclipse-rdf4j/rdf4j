# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 3.399 | 3.880 | 0.902× | 0.782–1.023× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 3.538 | 3.583 | 0.997× | 0.809–1.186× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 2.779 | 3.117 | 0.892× | 0.836–0.948× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | autoDisabled → auto | 10.551 | 9.664 | 1.157× | 0.877–1.437× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | autoDisabled → auto | 24.076 | 31.553 | 0.800× | 0.573–1.027× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | autoDisabled → auto | 36.406 | 35.462 | 1.078× | 0.659–1.497× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 3.399 | 3.542 | 0.962× | 0.943–0.980× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 3.880 | 3.542 | 1.082× | 0.958–1.206× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 3.538 | 4.182 | 0.848× | 0.834–0.862× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 3.583 | 4.182 | 0.885× | 0.704–1.066× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 2.779 | 4.266 | 0.666× | 0.583–0.748× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 3.117 | 4.266 | 0.755× | 0.615–0.895× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | autoDisabled → sparseJoinAggregate | 10.551 | 6.861 | 1.574× | 1.328–1.820× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | auto → sparseJoinAggregate | 9.664 | 6.861 | 1.390× | 1.267–1.514× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | autoDisabled → sparseJoinAggregate | 24.076 | 22.456 | 1.072× | 1.072–1.072× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | auto → sparseJoinAggregate | 31.553 | 22.456 | 1.457× | 1.045–1.870× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | autoDisabled → sparseJoinAggregate | 36.406 | 25.176 | 1.527× | 0.919–2.135× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | auto → sparseJoinAggregate | 35.462 | 25.176 | 1.411× | 1.395–1.426× |

