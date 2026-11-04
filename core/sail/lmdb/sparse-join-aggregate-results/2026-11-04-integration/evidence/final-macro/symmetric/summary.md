# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | autoDisabled → auto | 7.877 | 8.251 | 0.956× | 0.831–1.081× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 10.317 | 12.020 | 0.910× | 0.663–1.157× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 15.871 | 19.039 | 0.865× | 0.641–1.088× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | autoDisabled → auto | 97.431 | 9.890 | 9.920× | 9.304–10.535× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | autoDisabled → auto | 179.572 | 108.909 | 1.858× | 1.212–2.504× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → auto | 216.483 | 165.966 | 1.360× | 1.069–1.651× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | autoDisabled → auto | 14.068 | 8.553 | 1.851× | 1.140–2.562× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | autoDisabled → auto | 59.342 | 63.009 | 0.956× | 0.838–1.074× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | autoDisabled → auto | 58.554 | 53.719 | 1.124× | 0.948–1.299× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | autoDisabled → sparseJoinAggregate | 7.877 | 6.856 | 1.191× | 0.886–1.497× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | auto → sparseJoinAggregate | 8.251 | 6.856 | 1.226× | 1.066–1.385× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 10.317 | 10.111 | 1.058× | 0.821–1.295× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 12.020 | 10.111 | 1.179× | 1.119–1.240× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 15.871 | 11.653 | 1.461× | 0.987–1.935× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 19.039 | 11.653 | 1.659× | 1.540–1.778× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | autoDisabled → sparseJoinAggregate | 97.431 | 6.602 | 14.763× | 13.942–15.584× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | auto → sparseJoinAggregate | 9.890 | 6.602 | 1.499× | 1.323–1.675× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | autoDisabled → sparseJoinAggregate | 179.572 | 63.187 | 2.842× | 2.765–2.919× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | auto → sparseJoinAggregate | 108.909 | 63.187 | 1.723× | 1.166–2.280× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → sparseJoinAggregate | 216.483 | 101.196 | 2.145× | 2.057–2.233× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | auto → sparseJoinAggregate | 165.966 | 101.196 | 1.667× | 1.246–2.089× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | autoDisabled → sparseJoinAggregate | 14.068 | 5.205 | 2.735× | 2.549–2.922× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | auto → sparseJoinAggregate | 8.553 | 5.205 | 1.779× | 0.995–2.564× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | autoDisabled → sparseJoinAggregate | 59.342 | 55.215 | 1.088× | 0.970–1.206× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | auto → sparseJoinAggregate | 63.009 | 55.215 | 1.170× | 0.903–1.438× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | autoDisabled → sparseJoinAggregate | 58.554 | 53.353 | 1.205× | 0.825–1.585× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | auto → sparseJoinAggregate | 53.719 | 53.353 | 1.154× | 0.635–1.672× |

