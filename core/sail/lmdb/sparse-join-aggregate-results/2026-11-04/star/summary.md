# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/sparse ratios within each fork. Values above 1 favor sparse. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

| Fanout | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control | Control ms | Sparse ms | Paired speedup | Fork range |
| ---: | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1 | 1 | 1 | star | empty ≥ 2 | 3.12% / 3.80% | 0/128 | auto | 8.369 | 7.602 | 1.134× | 0.814–1.454× |
| 1 | 1 | 1 | star | median ≥ 1 | 3.12% / 3.80% | 128/128 | auto | 12.437 | 8.991 | 1.382× | 1.334–1.430× |
| 1 | 1 | 1 | star | none | 3.12% / 3.80% | 128/128 | auto | 12.528 | 10.450 | 1.215× | 0.952–1.479× |
| 1 | 1 | 4 | star | empty ≥ 17 | 3.12% / 3.80% | 0/128 | auto | 7.390 | 6.136 | 1.208× | 1.172–1.244× |
| 1 | 1 | 4 | star | median ≥ 4 | 3.12% / 3.80% | 74/128 | auto | 8.616 | 7.304 | 1.180× | 1.176–1.184× |
| 1 | 1 | 4 | star | none | 3.12% / 3.80% | 128/128 | auto | 6.989 | 5.471 | 1.272× | 1.193–1.351× |
| 1 | 8 | 1 | star | empty ≥ 3 | 2.81% / 3.42% | 0/115 | auto | 5.826 | 5.177 | 1.119× | 1.041–1.197× |
| 1 | 8 | 1 | star | median ≥ 1 | 2.81% / 3.42% | 115/115 | auto | 6.435 | 6.840 | 0.944× | 0.901–0.988× |
| 1 | 8 | 1 | star | none | 2.81% / 3.42% | 115/115 | auto | 5.459 | 4.979 | 1.102× | 0.964–1.239× |
| 1 | 8 | 4 | star | empty ≥ 29 | 2.81% / 3.42% | 0/115 | auto | 6.522 | 4.548 | 1.434× | 1.425–1.443× |
| 1 | 8 | 4 | star | median ≥ 4 | 2.81% / 3.42% | 66/115 | auto | 6.535 | 5.346 | 1.225× | 1.103–1.348× |
| 1 | 8 | 4 | star | none | 2.81% / 3.42% | 115/115 | auto | 5.202 | 3.355 | 1.566× | 1.265–1.867× |
| 8 | 1 | 1 | star | empty ≥ 10 | 87.96% / 87.96% | 0/3603 | auto | 6.583 | 4.309 | 1.527× | 1.317–1.737× |
| 8 | 1 | 1 | star | median ≥ 2 | 87.96% / 87.96% | 2403/3603 | auto | 12.506 | 9.123 | 1.370× | 1.326–1.413× |
| 8 | 1 | 1 | star | none | 87.96% / 87.96% | 3603/3603 | auto | 16.484 | 10.836 | 1.657× | 0.894–2.419× |
| 8 | 1 | 4 | star | empty ≥ 59 | 87.96% / 87.96% | 0/3603 | auto | 7.691 | 3.496 | 2.191× | 2.009–2.373× |
| 8 | 1 | 4 | star | median ≥ 13 | 87.96% / 87.96% | 1899/3603 | auto | 10.413 | 8.709 | 1.199× | 1.178–1.221× |
| 8 | 1 | 4 | star | none | 87.96% / 87.96% | 3603/3603 | auto | 13.720 | 11.494 | 1.259× | 1.049–1.468× |
| 8 | 8 | 1 | star | empty ≥ 17 | 12.50% / 12.50% | 0/512 | auto | 4.179 | 2.290 | 1.838× | 1.751–1.924× |
| 8 | 8 | 1 | star | median ≥ 16 | 12.50% / 12.50% | 512/512 | auto | 7.909 | 4.171 | 1.939× | 1.701–2.177× |
| 8 | 8 | 1 | star | none | 12.50% / 12.50% | 512/512 | auto | 6.038 | 3.984 | 1.515× | 1.491–1.540× |
| 8 | 8 | 4 | star | empty ≥ 193 | 12.50% / 12.50% | 0/512 | auto | 4.384 | 2.305 | 1.961× | 1.393–2.528× |
| 8 | 8 | 4 | star | median ≥ 123 | 12.50% / 12.50% | 256/512 | auto | 6.589 | 3.790 | 1.758× | 1.564–1.953× |
| 8 | 8 | 4 | star | none | 12.50% / 12.50% | 512/512 | auto | 5.048 | 2.906 | 1.743× | 1.660–1.825× |
