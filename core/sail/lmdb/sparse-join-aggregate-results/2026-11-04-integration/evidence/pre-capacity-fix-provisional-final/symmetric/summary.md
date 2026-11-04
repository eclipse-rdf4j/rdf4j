# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | autoDisabled → auto | 4.845 | 5.856 | 0.894× | 0.622–1.167× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 10.149 | 11.324 | 0.967× | 0.565–1.369× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 13.584 | 15.620 | 0.948× | 0.560–1.337× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | autoDisabled → auto | 123.921 | 9.045 | 13.735× | 13.157–14.314× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | autoDisabled → auto | 173.099 | 145.948 | 1.185× | 1.155–1.216× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → auto | 228.852 | 216.679 | 1.104× | 0.828–1.379× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | autoDisabled → auto | 14.692 | 12.571 | 1.468× | 0.713–2.224× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | autoDisabled → auto | 67.406 | 60.611 | 1.117× | 1.011–1.223× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | autoDisabled → auto | 55.139 | 59.157 | 0.935× | 0.849–1.022× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | autoDisabled → sparseJoinAggregate | 4.845 | 4.577 | 1.058× | 1.005–1.111× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | auto → sparseJoinAggregate | 5.856 | 4.577 | 1.284× | 0.952–1.615× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 10.149 | 8.290 | 1.207× | 0.986–1.427× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 11.324 | 8.290 | 1.394× | 1.042–1.746× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 13.584 | 10.986 | 1.257× | 0.906–1.608× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 15.620 | 10.986 | 1.410× | 1.203–1.617× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | autoDisabled → sparseJoinAggregate | 123.921 | 7.291 | 16.998× | 16.932–17.064× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | auto → sparseJoinAggregate | 9.045 | 7.291 | 1.240× | 1.192–1.287× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | autoDisabled → sparseJoinAggregate | 173.099 | 79.827 | 2.278× | 1.727–2.830× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | auto → sparseJoinAggregate | 145.948 | 79.827 | 1.911× | 1.495–2.327× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → sparseJoinAggregate | 228.852 | 101.840 | 2.248× | 2.059–2.436× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | auto → sparseJoinAggregate | 216.679 | 101.840 | 2.127× | 1.766–2.487× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | autoDisabled → sparseJoinAggregate | 14.692 | 6.015 | 2.453× | 2.395–2.510× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | auto → sparseJoinAggregate | 12.571 | 6.015 | 2.298× | 1.077–3.519× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | autoDisabled → sparseJoinAggregate | 67.406 | 57.277 | 1.189× | 1.095–1.283× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | auto → sparseJoinAggregate | 60.611 | 57.277 | 1.082× | 0.895–1.270× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | autoDisabled → sparseJoinAggregate | 55.139 | 53.909 | 1.035× | 0.948–1.122× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | auto → sparseJoinAggregate | 59.157 | 53.909 | 1.124× | 0.928–1.321× |

