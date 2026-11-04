# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.481 | 4.509 | 1.246× | 1.038–1.455× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.597 | 5.660 | 1.003× | 0.907–1.099× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.243 | 16.578 | 0.644× | 0.160–1.128× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | autoDisabled → auto | 14.925 | 8.836 | 1.754× | 1.229–2.278× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | autoDisabled → auto | 39.858 | 28.449 | 1.416× | 1.313–1.518× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | autoDisabled → auto | 45.610 | 38.272 | 1.282× | 0.789–1.775× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.481 | 4.833 | 1.139× | 1.054–1.223× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 4.509 | 4.833 | 0.928× | 0.841–1.015× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.597 | 5.444 | 1.062× | 0.842–1.281× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 5.660 | 5.444 | 1.089× | 0.766–1.411× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.243 | 4.536 | 1.169× | 1.105–1.232× |
| 8×8 | uniform | 0/512 | false | 1 | 1 | bound | none | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 16.578 | 4.536 | 4.352× | 0.980–7.724× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | autoDisabled → sparseJoinAggregate | 14.925 | 9.570 | 1.577× | 1.215–1.940× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | empty ≥ 3 | 11.78% / 11.78% | 0/7717 | auto → sparseJoinAggregate | 8.836 | 9.570 | 0.920× | 0.851–0.988× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | autoDisabled → sparseJoinAggregate | 39.858 | 27.691 | 1.447× | 1.281–1.613× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | median ≥ 1 | 11.78% / 11.78% | 7717/7717 | auto → sparseJoinAggregate | 28.449 | 27.691 | 1.036× | 0.844–1.228× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | autoDisabled → sparseJoinAggregate | 45.610 | 33.130 | 1.488× | 0.905–2.072× |
| 8×8 | uniform | 128/512 | false | 1 | 1 | bound | none | 11.78% / 11.78% | 7717/7717 | auto → sparseJoinAggregate | 38.272 | 33.130 | 1.157× | 1.147–1.167× |

