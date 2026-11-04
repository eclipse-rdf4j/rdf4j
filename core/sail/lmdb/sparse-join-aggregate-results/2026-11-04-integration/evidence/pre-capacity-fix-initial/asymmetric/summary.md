# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | autoDisabled → auto | 41.277 | 46.633 | 0.885× | 0.882–0.888× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | autoDisabled → auto | 57.671 | 59.919 | 1.357× | 0.506–2.207× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | autoDisabled → auto | 45.139 | 43.267 | 1.043× | 1.038–1.047× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | autoDisabled → auto | 37.024 | 35.886 | 1.034× | 0.958–1.110× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | autoDisabled → sparseJoinAggregate | 41.277 | 47.067 | 0.874× | 0.819–0.929× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | auto → sparseJoinAggregate | 46.633 | 47.067 | 0.987× | 0.929–1.046× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | autoDisabled → sparseJoinAggregate | 57.671 | 37.517 | 1.724× | 0.953–2.496× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | auto → sparseJoinAggregate | 59.919 | 37.517 | 1.506× | 1.131–1.881× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | autoDisabled → sparseJoinAggregate | 45.139 | 36.187 | 1.245× | 1.141–1.350× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | auto → sparseJoinAggregate | 43.267 | 36.187 | 1.194× | 1.099–1.289× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | autoDisabled → sparseJoinAggregate | 37.024 | 32.320 | 1.159× | 1.055–1.264× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | auto → sparseJoinAggregate | 35.886 | 32.320 | 1.135× | 0.951–1.319× |

