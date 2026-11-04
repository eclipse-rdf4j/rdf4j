# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/candidate ratios within each fork. Values above 1 favor the candidate named in the table. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

Route witnesses come from separate executed explanations before and after the warmed batch, not from instrumentation of each timed invocation. Historical CSVs contain only the earlier witnesses. Consult summary.csv for their scope, stages and observed path union.

Observed compact-domain dimensions use the forced sparse reference route and name its physical streamed/retained roles, which may transpose the written query sides. All witnessed dimension pairs and orientation plans are retained; density is unchanged by transposition.

## Automatic planner integration

`auto` has no sparse-property override. `autoDisabled` explicitly disables the candidate; both leave route selection to the planner. Results are also in automatic-comparison.csv.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.885 | 7.605 | 0.774× | 0.774–0.774× |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.288 | 5.467 | 0.967× | 0.967–0.967× |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 4.370 | 3.834 | 1.140× | 1.140–1.140× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | empty ≥ 48 | 7.91% / 44.02% | 0/81 | autoDisabled → auto | 5.202 | 6.632 | 0.784× | 0.784–0.784× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | median ≥ 12 | 7.91% / 44.02% | 48/81 | autoDisabled → auto | 7.670 | 6.527 | 1.175× | 1.175–1.175× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | none | 7.91% / 44.02% | 81/81 | autoDisabled → auto | 6.402 | 6.256 | 1.023× | 1.023–1.023× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | empty ≥ 23 | 3.22% / 15.87% | 0/33 | autoDisabled → auto | 7.940 | 5.863 | 1.354× | 1.354–1.354× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | median ≥ 6 | 3.22% / 15.87% | 23/33 | autoDisabled → auto | 5.260 | 6.951 | 0.757× | 0.757–0.757× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | none | 3.22% / 15.87% | 33/33 | autoDisabled → auto | 4.909 | 5.542 | 0.886× | 0.886–0.886× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 8.236 | 8.893 | 0.926× | 0.926–0.926× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 13.317 | 11.558 | 1.152× | 1.152–1.152× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 10.177 | 9.735 | 1.045× | 1.045–1.045× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | empty ≥ 19 | 22.95% / 34.97% | 0/235 | autoDisabled → auto | 6.138 | 5.538 | 1.108× | 1.108–1.108× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | median ≥ 4 | 22.95% / 34.97% | 137/235 | autoDisabled → auto | 9.692 | 7.998 | 1.212× | 1.212–1.212× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | none | 22.95% / 34.97% | 235/235 | autoDisabled → auto | 9.050 | 7.799 | 1.160× | 1.160–1.160× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | empty ≥ 13 | 6.25% / 9.52% | 0/64 | autoDisabled → auto | 28.194 | 5.811 | 4.852× | 4.852–4.852× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | median ≥ 4 | 6.25% / 9.52% | 32/64 | autoDisabled → auto | 10.933 | 9.436 | 1.159× | 1.159–1.159× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | none | 6.25% / 9.52% | 64/64 | autoDisabled → auto | 9.068 | 9.665 | 0.938× | 0.938–0.938× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 4.055 | 4.069 | 0.997× | 0.997–0.997× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 4.063 | 4.701 | 0.864× | 0.864–0.864× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 2.657 | 2.382 | 1.115× | 1.115–1.115× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | empty ≥ 65 | 8.20% / 37.50% | 0/84 | autoDisabled → auto | 4.941 | 4.878 | 1.013× | 1.013–1.013× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | median ≥ 12 | 8.20% / 37.50% | 48/84 | autoDisabled → auto | 5.451 | 5.543 | 0.983× | 0.983–0.983× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | none | 8.20% / 37.50% | 84/84 | autoDisabled → auto | 3.433 | 3.162 | 1.086× | 1.086–1.086× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | empty ≥ 15 | 3.12% / 14.29% | 0/32 | autoDisabled → auto | 4.744 | 4.546 | 1.044× | 1.044–1.044× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | median ≥ 14 | 3.12% / 14.29% | 16/32 | autoDisabled → auto | 3.632 | 3.645 | 0.997× | 0.997–0.997× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | none | 3.12% / 14.29% | 32/32 | autoDisabled → auto | 2.296 | 3.349 | 0.685× | 0.685–0.685× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.872 | 6.281 | 0.935× | 0.935–0.935× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 5.548 | 5.603 | 0.990× | 0.990–0.990× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → auto | 4.849 | 4.206 | 1.153× | 1.153–1.153× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | empty ≥ 20 | 23.73% / 34.52% | 0/243 | autoDisabled → auto | 4.064 | 3.921 | 1.036× | 1.036–1.036× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | median ≥ 4 | 23.73% / 34.52% | 144/243 | autoDisabled → auto | 5.995 | 4.255 | 1.409× | 1.409–1.409× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | none | 23.73% / 34.52% | 243/243 | autoDisabled → auto | 5.479 | 4.686 | 1.169× | 1.169–1.169× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | empty ≥ 13 | 6.25% / 11.11% | 0/64 | autoDisabled → auto | 3.918 | 4.292 | 0.913× | 0.913–0.913× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | median ≥ 4 | 6.25% / 11.11% | 32/64 | autoDisabled → auto | 4.491 | 5.103 | 0.880× | 0.880–0.880× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | none | 6.25% / 11.11% | 64/64 | autoDisabled → auto | 3.476 | 5.253 | 0.662× | 0.662–0.662× |

## Forced sparse comparisons

These measure forced sparse against each available control, separately from the automatic integration comparison. Historical `auto` rows used the disabled planner; their summary.csv sparse_setting is legacy_false.

| Left×right fanout | Distribution | Shared keys | Parallel | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control → candidate | Control ms | Candidate ms | Paired speedup | Fork range |
| ---: | --- | ---: | --- | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.885 | 13.794 | 0.427× | 0.427–0.427× |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 7.605 | 13.794 | 0.551× | 0.551–0.551× |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.288 | 4.798 | 1.102× | 1.102–1.102× |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 5.467 | 4.798 | 1.139× | 1.139–1.139× |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 4.370 | 3.782 | 1.156× | 1.156–1.156× |
| 1×8 | hot | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 3.834 | 3.782 | 1.014× | 1.014–1.014× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | empty ≥ 48 | 7.91% / 44.02% | 0/81 | autoDisabled → sparseJoinAggregate | 5.202 | 3.847 | 1.352× | 1.352–1.352× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | empty ≥ 48 | 7.91% / 44.02% | 0/81 | auto → sparseJoinAggregate | 6.632 | 3.847 | 1.724× | 1.724–1.724× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | median ≥ 12 | 7.91% / 44.02% | 48/81 | autoDisabled → sparseJoinAggregate | 7.670 | 4.606 | 1.665× | 1.665–1.665× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | median ≥ 12 | 7.91% / 44.02% | 48/81 | auto → sparseJoinAggregate | 6.527 | 4.606 | 1.417× | 1.417–1.417× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | none | 7.91% / 44.02% | 81/81 | autoDisabled → sparseJoinAggregate | 6.402 | 5.314 | 1.205× | 1.205–1.205× |
| 1×8 | hot | 32/32 | false | 1 | 4 | bound | none | 7.91% / 44.02% | 81/81 | auto → sparseJoinAggregate | 6.256 | 5.314 | 1.177× | 1.177–1.177× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | empty ≥ 23 | 3.22% / 15.87% | 0/33 | autoDisabled → sparseJoinAggregate | 7.940 | 4.322 | 1.837× | 1.837–1.837× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | empty ≥ 23 | 3.22% / 15.87% | 0/33 | auto → sparseJoinAggregate | 5.863 | 4.322 | 1.357× | 1.357–1.357× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | median ≥ 6 | 3.22% / 15.87% | 23/33 | autoDisabled → sparseJoinAggregate | 5.260 | 4.676 | 1.125× | 1.125–1.125× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | median ≥ 6 | 3.22% / 15.87% | 23/33 | auto → sparseJoinAggregate | 6.951 | 4.676 | 1.487× | 1.487–1.487× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | none | 3.22% / 15.87% | 33/33 | autoDisabled → sparseJoinAggregate | 4.909 | 4.253 | 1.154× | 1.154–1.154× |
| 1×8 | hot | 8/32 | false | 1 | 4 | bound | none | 3.22% / 15.87% | 33/33 | auto → sparseJoinAggregate | 5.542 | 4.253 | 1.303× | 1.303–1.303× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 8.236 | 6.616 | 1.245× | 1.245–1.245× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 8.893 | 6.616 | 1.344× | 1.344–1.344× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 13.317 | 8.050 | 1.654× | 1.654–1.654× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 11.558 | 8.050 | 1.436× | 1.436–1.436× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 10.177 | 7.207 | 1.412× | 1.412–1.412× |
| 1×8 | uniform | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 9.735 | 7.207 | 1.351× | 1.351–1.351× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | empty ≥ 19 | 22.95% / 34.97% | 0/235 | autoDisabled → sparseJoinAggregate | 6.138 | 5.335 | 1.151× | 1.151–1.151× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | empty ≥ 19 | 22.95% / 34.97% | 0/235 | auto → sparseJoinAggregate | 5.538 | 5.335 | 1.038× | 1.038–1.038× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | median ≥ 4 | 22.95% / 34.97% | 137/235 | autoDisabled → sparseJoinAggregate | 9.692 | 10.333 | 0.938× | 0.938–0.938× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | median ≥ 4 | 22.95% / 34.97% | 137/235 | auto → sparseJoinAggregate | 7.998 | 10.333 | 0.774× | 0.774–0.774× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | none | 22.95% / 34.97% | 235/235 | autoDisabled → sparseJoinAggregate | 9.050 | 7.832 | 1.155× | 1.155–1.155× |
| 1×8 | uniform | 32/32 | false | 1 | 4 | bound | none | 22.95% / 34.97% | 235/235 | auto → sparseJoinAggregate | 7.799 | 7.832 | 0.996× | 0.996–0.996× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | empty ≥ 13 | 6.25% / 9.52% | 0/64 | autoDisabled → sparseJoinAggregate | 28.194 | 5.488 | 5.137× | 5.137–5.137× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | empty ≥ 13 | 6.25% / 9.52% | 0/64 | auto → sparseJoinAggregate | 5.811 | 5.488 | 1.059× | 1.059–1.059× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | median ≥ 4 | 6.25% / 9.52% | 32/64 | autoDisabled → sparseJoinAggregate | 10.933 | 8.479 | 1.289× | 1.289–1.289× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | median ≥ 4 | 6.25% / 9.52% | 32/64 | auto → sparseJoinAggregate | 9.436 | 8.479 | 1.113× | 1.113–1.113× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | none | 6.25% / 9.52% | 64/64 | autoDisabled → sparseJoinAggregate | 9.068 | 9.046 | 1.002× | 1.002–1.002× |
| 1×8 | uniform | 8/32 | false | 1 | 4 | bound | none | 6.25% / 9.52% | 64/64 | auto → sparseJoinAggregate | 9.665 | 9.046 | 1.068× | 1.068–1.068× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 4.055 | 4.532 | 0.895× | 0.895–0.895× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 4.069 | 4.532 | 0.898× | 0.898–0.898× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 4.063 | 3.340 | 1.216× | 1.216–1.216× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 4.701 | 3.340 | 1.407× | 1.407–1.407× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 2.657 | 2.308 | 1.152× | 1.152–1.152× |
| 8×1 | hot | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 2.382 | 2.308 | 1.032× | 1.032–1.032× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | empty ≥ 65 | 8.20% / 37.50% | 0/84 | autoDisabled → sparseJoinAggregate | 4.941 | 4.423 | 1.117× | 1.117–1.117× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | empty ≥ 65 | 8.20% / 37.50% | 0/84 | auto → sparseJoinAggregate | 4.878 | 4.423 | 1.103× | 1.103–1.103× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | median ≥ 12 | 8.20% / 37.50% | 48/84 | autoDisabled → sparseJoinAggregate | 5.451 | 4.855 | 1.123× | 1.123–1.123× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | median ≥ 12 | 8.20% / 37.50% | 48/84 | auto → sparseJoinAggregate | 5.543 | 4.855 | 1.142× | 1.142–1.142× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | none | 8.20% / 37.50% | 84/84 | autoDisabled → sparseJoinAggregate | 3.433 | 3.039 | 1.129× | 1.129–1.129× |
| 8×1 | hot | 32/32 | false | 1 | 4 | bound | none | 8.20% / 37.50% | 84/84 | auto → sparseJoinAggregate | 3.162 | 3.039 | 1.040× | 1.040–1.040× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | empty ≥ 15 | 3.12% / 14.29% | 0/32 | autoDisabled → sparseJoinAggregate | 4.744 | 2.774 | 1.710× | 1.710–1.710× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | empty ≥ 15 | 3.12% / 14.29% | 0/32 | auto → sparseJoinAggregate | 4.546 | 2.774 | 1.638× | 1.638–1.638× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | median ≥ 14 | 3.12% / 14.29% | 16/32 | autoDisabled → sparseJoinAggregate | 3.632 | 2.975 | 1.221× | 1.221–1.221× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | median ≥ 14 | 3.12% / 14.29% | 16/32 | auto → sparseJoinAggregate | 3.645 | 2.975 | 1.225× | 1.225–1.225× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | none | 3.12% / 14.29% | 32/32 | autoDisabled → sparseJoinAggregate | 2.296 | 2.332 | 0.984× | 0.984–0.984× |
| 8×1 | hot | 8/32 | false | 1 | 4 | bound | none | 3.12% / 14.29% | 32/32 | auto → sparseJoinAggregate | 3.349 | 2.332 | 1.436× | 1.436–1.436× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.872 | 4.599 | 1.277× | 1.277–1.277× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | empty ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 6.281 | 4.599 | 1.366× | 1.366–1.366× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 5.548 | 4.869 | 1.139× | 1.139–1.139× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | median ≥ 1 | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 5.603 | 4.869 | 1.151× | 1.151–1.151× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | autoDisabled → sparseJoinAggregate | 4.849 | 4.383 | 1.106× | 1.106–1.106× |
| 8×1 | uniform | 0/32 | false | 1 | 4 | bound | none | 0.00% / 0.00% | 0/0 | auto → sparseJoinAggregate | 4.206 | 4.383 | 0.960× | 0.960–0.960× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | empty ≥ 20 | 23.73% / 34.52% | 0/243 | autoDisabled → sparseJoinAggregate | 4.064 | 4.225 | 0.962× | 0.962–0.962× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | empty ≥ 20 | 23.73% / 34.52% | 0/243 | auto → sparseJoinAggregate | 3.921 | 4.225 | 0.928× | 0.928–0.928× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | median ≥ 4 | 23.73% / 34.52% | 144/243 | autoDisabled → sparseJoinAggregate | 5.995 | 4.104 | 1.461× | 1.461–1.461× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | median ≥ 4 | 23.73% / 34.52% | 144/243 | auto → sparseJoinAggregate | 4.255 | 4.104 | 1.037× | 1.037–1.037× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | none | 23.73% / 34.52% | 243/243 | autoDisabled → sparseJoinAggregate | 5.479 | 5.033 | 1.089× | 1.089–1.089× |
| 8×1 | uniform | 32/32 | false | 1 | 4 | bound | none | 23.73% / 34.52% | 243/243 | auto → sparseJoinAggregate | 4.686 | 5.033 | 0.931× | 0.931–0.931× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | empty ≥ 13 | 6.25% / 11.11% | 0/64 | autoDisabled → sparseJoinAggregate | 3.918 | 3.273 | 1.197× | 1.197–1.197× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | empty ≥ 13 | 6.25% / 11.11% | 0/64 | auto → sparseJoinAggregate | 4.292 | 3.273 | 1.312× | 1.312–1.312× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | median ≥ 4 | 6.25% / 11.11% | 32/64 | autoDisabled → sparseJoinAggregate | 4.491 | 3.821 | 1.175× | 1.175–1.175× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | median ≥ 4 | 6.25% / 11.11% | 32/64 | auto → sparseJoinAggregate | 5.103 | 3.821 | 1.335× | 1.335–1.335× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | none | 6.25% / 11.11% | 64/64 | autoDisabled → sparseJoinAggregate | 3.476 | 3.837 | 0.906× | 0.906–0.906× |
| 8×1 | uniform | 8/32 | false | 1 | 4 | bound | none | 6.25% / 11.11% | 64/64 | auto → sparseJoinAggregate | 5.253 | 3.837 | 1.369× | 1.369–1.369× |

