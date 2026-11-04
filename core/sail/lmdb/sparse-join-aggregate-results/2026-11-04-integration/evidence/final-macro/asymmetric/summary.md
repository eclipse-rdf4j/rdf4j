# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | autoDisabled → auto | 38.527 | 36.401 | 1.062× | 0.984–1.141× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | autoDisabled → auto | 48.008 | 46.042 | 1.241× | 0.495–1.987× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | autoDisabled → auto | 44.719 | 41.152 | 1.102× | 0.867–1.338× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | autoDisabled → auto | 35.977 | 35.276 | 1.018× | 0.880–1.155× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | autoDisabled → sparseJoinAggregate | 38.527 | 34.052 | 1.133× | 1.069–1.198× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 21.97% / 24.56% | 14400/14400 | auto → sparseJoinAggregate | 36.401 | 34.052 | 1.068× | 1.050–1.087× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | autoDisabled → sparseJoinAggregate | 48.008 | 36.676 | 1.444× | 0.670–2.218× |
| 1×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 21.97% / 24.56% | 14400/14400 | auto → sparseJoinAggregate | 46.042 | 36.676 | 1.235× | 1.116–1.353× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | autoDisabled → sparseJoinAggregate | 44.719 | 29.443 | 1.536× | 1.227–1.845× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 22.13% / 25.51% | 14500/14500 | auto → sparseJoinAggregate | 41.152 | 29.443 | 1.397× | 1.379–1.415× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | autoDisabled → sparseJoinAggregate | 35.977 | 28.879 | 1.258× | 1.006–1.511× |
| 32×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 22.13% / 25.51% | 14500/14500 | auto → sparseJoinAggregate | 35.276 | 28.879 | 1.226× | 1.144–1.308× |

