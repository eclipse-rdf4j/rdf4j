# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | autoDisabled → auto | 7.806 | 7.065 | 1.137× | 0.901–1.372× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 10.847 | 13.252 | 0.820× | 0.779–0.861× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → auto | 14.059 | 14.858 | 0.953× | 0.849–1.057× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | autoDisabled → auto | 100.715 | 11.465 | 8.791× | 8.659–8.922× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | autoDisabled → auto | 197.573 | 192.217 | 1.030× | 0.948–1.113× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → auto | 334.123 | 220.302 | 1.514× | 1.450–1.578× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | autoDisabled → auto | 12.980 | 11.947 | 1.487× | 0.722–2.252× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | autoDisabled → auto | 75.464 | 57.964 | 1.298× | 1.245–1.351× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | autoDisabled → auto | 63.469 | 56.261 | 1.157× | 0.926–1.388× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | autoDisabled → sparseJoinAggregate | 7.806 | 7.439 | 1.053× | 0.941–1.165× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | auto → sparseJoinAggregate | 7.065 | 7.439 | 0.947× | 0.849–1.044× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 10.847 | 10.837 | 1.001× | 0.987–1.014× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 13.252 | 10.837 | 1.223× | 1.178–1.268× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | autoDisabled → sparseJoinAggregate | 14.059 | 13.002 | 1.130× | 0.869–1.392× |
| 1×1 | uniform | 512/512 | false | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | auto → sparseJoinAggregate | 14.858 | 13.002 | 1.170× | 1.023–1.317× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | autoDisabled → sparseJoinAggregate | 100.715 | 8.590 | 11.785× | 10.750–12.821× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | auto → sparseJoinAggregate | 11.465 | 8.590 | 1.343× | 1.205–1.481× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | autoDisabled → sparseJoinAggregate | 197.573 | 75.005 | 2.649× | 2.504–2.795× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | auto → sparseJoinAggregate | 192.217 | 75.005 | 2.599× | 2.251–2.948× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | autoDisabled → sparseJoinAggregate | 334.123 | 117.217 | 3.024× | 2.169–3.878× |
| 32×32 | uniform | 512/512 | false | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | auto → sparseJoinAggregate | 220.302 | 117.217 | 1.977× | 1.496–2.458× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | autoDisabled → sparseJoinAggregate | 12.980 | 5.425 | 2.410× | 2.219–2.602× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | auto → sparseJoinAggregate | 11.947 | 5.425 | 2.114× | 1.155–3.073× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | autoDisabled → sparseJoinAggregate | 75.464 | 47.281 | 1.593× | 1.447–1.739× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | auto → sparseJoinAggregate | 57.964 | 47.281 | 1.225× | 1.163–1.287× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | autoDisabled → sparseJoinAggregate | 63.469 | 54.261 | 1.277× | 0.860–1.693× |
| 8×8 | uniform | 512/512 | false | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | auto → sparseJoinAggregate | 56.261 | 54.261 | 1.074× | 0.929–1.220× |

