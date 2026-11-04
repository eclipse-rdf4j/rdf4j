# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | autoDisabled → auto | 37.922 | 41.035 | 0.923× | 0.897–0.950× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | autoDisabled → auto | 44.308 | 43.963 | 1.127× | 0.586–1.667× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | autoDisabled → auto | 34.005 | 41.644 | 0.836× | 0.713–0.958× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | autoDisabled → auto | 29.821 | 37.960 | 0.812× | 0.673–0.952× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | autoDisabled → sparseJoinAggregate | 37.922 | 38.370 | 1.003× | 0.908–1.099× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | auto → sparseJoinAggregate | 41.035 | 38.370 | 1.090× | 0.956–1.225× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | autoDisabled → sparseJoinAggregate | 44.308 | 41.378 | 1.143× | 0.660–1.626× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | auto → sparseJoinAggregate | 43.963 | 41.378 | 1.051× | 0.975–1.127× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | autoDisabled → sparseJoinAggregate | 34.005 | 29.949 | 1.138× | 1.074–1.203× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | auto → sparseJoinAggregate | 41.644 | 29.949 | 1.403× | 1.121–1.686× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | autoDisabled → sparseJoinAggregate | 29.821 | 32.803 | 0.924× | 0.816–1.032× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | auto → sparseJoinAggregate | 37.960 | 32.803 | 1.149× | 1.084–1.213× |

