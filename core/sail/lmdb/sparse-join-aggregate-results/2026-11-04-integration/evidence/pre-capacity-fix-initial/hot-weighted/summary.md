# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | autoDisabled → auto | 100.043 | 10.481 | 9.679× | 7.979–11.379× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → auto | 111.883 | 112.630 | 0.992× | 0.962–1.022× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → auto | 136.103 | 85.788 | 1.997× | 1.120–2.875× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | autoDisabled → auto | 17.556 | 9.086 | 1.930× | 1.906–1.955× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → auto | 25.825 | 25.930 | 0.998× | 0.878–1.119× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → auto | 46.451 | 40.761 | 1.115× | 0.896–1.333× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | autoDisabled → sparseJoinAggregate | 100.043 | 7.760 | 12.904× | 11.518–14.291× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 2795 | 29.29% / 29.29% | 0/19195 | auto → sparseJoinAggregate | 10.481 | 7.760 | 1.350× | 1.256–1.444× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | autoDisabled → sparseJoinAggregate | 111.883 | 28.543 | 3.917× | 3.882–3.953× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 43 | 29.29% / 29.29% | 10064/19195 | auto → sparseJoinAggregate | 112.630 | 28.543 | 3.952× | 3.869–4.035× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | autoDisabled → sparseJoinAggregate | 136.103 | 41.204 | 3.302× | 3.208–3.397× |
| 32×32 | hot | 512/512 | false | 1 | 4 | bound | none | 29.29% / 29.29% | 19195/19195 | auto → sparseJoinAggregate | 85.788 | 41.204 | 2.074× | 1.116–3.033× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | autoDisabled → sparseJoinAggregate | 17.556 | 8.793 | 1.994× | 1.952–2.036× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | empty ≥ 380 | 9.18% / 11.79% | 0/6019 | auto → sparseJoinAggregate | 9.086 | 8.793 | 1.033× | 1.024–1.042× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | autoDisabled → sparseJoinAggregate | 25.825 | 20.522 | 1.260× | 1.244–1.276× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | median ≥ 9 | 9.18% / 11.79% | 3222/6019 | auto → sparseJoinAggregate | 25.930 | 20.522 | 1.283× | 1.112–1.453× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | autoDisabled → sparseJoinAggregate | 46.451 | 26.390 | 1.785× | 1.177–2.394× |
| 8×8 | hot | 512/512 | false | 1 | 4 | bound | none | 9.18% / 11.79% | 6019/6019 | auto → sparseJoinAggregate | 40.761 | 26.390 | 1.555× | 1.313–1.796× |

