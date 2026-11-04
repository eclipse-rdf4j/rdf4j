# Sparse join aggregate measurements

Complete-query latency includes preparation, optimization, sparse setup/remapping, aggregation and result collection. Exact validation is outside the timer. Each latency is the median of per-fork warmed medians; speedup is the median of paired baseline/sparse ratios within each fork. Values above 1 favor sparse. The range is the minimum–maximum paired ratio across forks, not a confidence interval. No samples are removed.

First-use samples are separate in summary.csv. Stores and persistent adjacency are already built; no cold-disk claims are made. Consult environment.txt, commands.sh and raw fork CSVs for the machine, exact invocation and all samples.

| Fanout | Blocks | Max weight | Count | HAVING | Density nominal/observed | Retained/total groups | Control | Control ms | Sparse ms | Paired speedup | Fork range |
| ---: | ---: | ---: | --- | --- | ---: | ---: | --- | ---: | ---: | ---: | --- |
| 1 | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | auto | 7.357 | 6.148 | 1.090× | 1.049–1.197× |
| 1 | 1 | 1 | bound | empty ≥ 3 | 0.78% / 1.00% | 0/510 | packedFtreeAggregate | 6.390 | 6.148 | 1.008× | 0.909–1.265× |
| 1 | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | auto | 11.589 | 9.142 | 1.309× | 0.986–1.539× |
| 1 | 1 | 1 | bound | median ≥ 1 | 0.78% / 1.00% | 510/510 | packedFtreeAggregate | 9.164 | 9.142 | 1.097× | 0.976–1.217× |
| 1 | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | auto | 18.073 | 8.317 | 1.050× | 0.959–2.339× |
| 1 | 1 | 1 | bound | none | 0.78% / 1.00% | 510/510 | packedFtreeAggregate | 12.310 | 8.317 | 1.349× | 0.715–1.891× |
| 1 | 1 | 4 | bound | empty ≥ 17 | 0.78% / 1.00% | 0/510 | auto | 6.704 | 5.518 | 1.185× | 1.053–1.231× |
| 1 | 1 | 4 | bound | empty ≥ 17 | 0.78% / 1.00% | 0/510 | packedFtreeAggregate | 6.164 | 5.518 | 1.089× | 0.822–1.267× |
| 1 | 1 | 4 | bound | median ≥ 8 | 0.78% / 1.00% | 255/510 | auto | 7.694 | 6.269 | 1.159× | 1.136–1.227× |
| 1 | 1 | 4 | bound | median ≥ 8 | 0.78% / 1.00% | 255/510 | packedFtreeAggregate | 8.226 | 6.269 | 1.224× | 1.215–1.394× |
| 1 | 1 | 4 | bound | none | 0.78% / 1.00% | 510/510 | auto | 7.529 | 5.878 | 1.146× | 1.125–1.698× |
| 1 | 1 | 4 | bound | none | 0.78% / 1.00% | 510/510 | packedFtreeAggregate | 6.807 | 5.878 | 1.158× | 0.895–1.364× |
| 1 | 8 | 1 | bound | empty ≥ 3 | 0.77% / 1.01% | 0/502 | auto | 4.433 | 2.657 | 1.192× | 0.871–2.078× |
| 1 | 8 | 1 | bound | empty ≥ 3 | 0.77% / 1.01% | 0/502 | packedFtreeAggregate | 4.773 | 2.657 | 1.796× | 0.903–2.031× |
| 1 | 8 | 1 | bound | median ≥ 1 | 0.77% / 1.01% | 502/502 | auto | 7.809 | 7.260 | 1.083× | 1.031–1.346× |
| 1 | 8 | 1 | bound | median ≥ 1 | 0.77% / 1.01% | 502/502 | packedFtreeAggregate | 6.895 | 7.260 | 0.950× | 0.759–0.954× |
| 1 | 8 | 1 | bound | none | 0.77% / 1.01% | 502/502 | auto | 7.090 | 6.671 | 1.039× | 0.574–1.508× |
| 1 | 8 | 1 | bound | none | 0.77% / 1.01% | 502/502 | packedFtreeAggregate | 5.985 | 6.671 | 0.915× | 0.877–0.971× |
| 1 | 8 | 4 | bound | empty ≥ 25 | 0.77% / 1.01% | 0/502 | auto | 4.183 | 2.703 | 1.313× | 1.148–1.804× |
| 1 | 8 | 4 | bound | empty ≥ 25 | 0.77% / 1.01% | 0/502 | packedFtreeAggregate | 3.835 | 2.703 | 1.053× | 1.053–1.650× |
| 1 | 8 | 4 | bound | median ≥ 8 | 0.77% / 1.01% | 252/502 | auto | 4.906 | 4.204 | 1.179× | 1.167–1.479× |
| 1 | 8 | 4 | bound | median ≥ 8 | 0.77% / 1.01% | 252/502 | packedFtreeAggregate | 4.134 | 4.204 | 0.983× | 0.942–1.373× |
| 1 | 8 | 4 | bound | none | 0.77% / 1.01% | 502/502 | auto | 4.261 | 3.112 | 1.090× | 0.920–1.369× |
| 1 | 8 | 4 | bound | none | 0.77% / 1.01% | 502/502 | packedFtreeAggregate | 3.572 | 3.112 | 1.135× | 0.991–1.354× |
| 32 | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | auto | 254.313 | 6.067 | 41.780× | 27.213–44.586× |
| 32 | 1 | 1 | bound | empty ≥ 21 | 100.00% / 100.00% | 0/65536 | packedFtreeAggregate | 269.684 | 6.067 | 42.760× | 29.773–44.632× |
| 32 | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | auto | 307.694 | 63.690 | 4.831× | 4.535–5.085× |
| 32 | 1 | 1 | bound | median ≥ 8 | 100.00% / 100.00% | 35766/65536 | packedFtreeAggregate | 311.161 | 63.690 | 4.886× | 4.478–5.669× |
| 32 | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | auto | 192.511 | 96.553 | 2.047× | 1.994–2.821× |
| 32 | 1 | 1 | bound | none | 100.00% / 100.00% | 65536/65536 | packedFtreeAggregate | 293.573 | 96.553 | 3.194× | 2.566–3.204× |
| 32 | 1 | 4 | bound | empty ≥ 133 | 100.00% / 100.00% | 0/65536 | auto | 286.467 | 8.392 | 34.508× | 31.857–36.940× |
| 32 | 1 | 4 | bound | empty ≥ 133 | 100.00% / 100.00% | 0/65536 | packedFtreeAggregate | 272.047 | 8.392 | 32.416× | 30.178–36.138× |
| 32 | 1 | 4 | bound | median ≥ 52 | 100.00% / 100.00% | 32850/65536 | auto | 290.034 | 61.192 | 4.929× | 4.578–6.071× |
| 32 | 1 | 4 | bound | median ≥ 52 | 100.00% / 100.00% | 32850/65536 | packedFtreeAggregate | 287.324 | 61.192 | 4.841× | 4.695–8.105× |
| 32 | 1 | 4 | bound | none | 100.00% / 100.00% | 65536/65536 | auto | 211.068 | 105.136 | 2.008× | 1.961–3.209× |
| 32 | 1 | 4 | bound | none | 100.00% / 100.00% | 65536/65536 | packedFtreeAggregate | 321.068 | 105.136 | 3.015× | 3.011–3.347× |
| 32 | 8 | 1 | bound | empty ≥ 65 | 12.50% / 12.50% | 0/8192 | auto | 109.019 | 2.225 | 47.279× | 45.687–48.987× |
| 32 | 8 | 1 | bound | empty ≥ 65 | 12.50% / 12.50% | 0/8192 | packedFtreeAggregate | 107.965 | 2.225 | 47.650× | 44.540–48.711× |
| 32 | 8 | 1 | bound | median ≥ 64 | 12.50% / 12.50% | 8192/8192 | auto | 115.121 | 20.468 | 5.860× | 5.475–6.018× |
| 32 | 8 | 1 | bound | median ≥ 64 | 12.50% / 12.50% | 8192/8192 | packedFtreeAggregate | 122.872 | 20.468 | 6.247× | 5.857–6.389× |
| 32 | 8 | 1 | bound | none | 12.50% / 12.50% | 8192/8192 | auto | 100.274 | 18.195 | 4.754× | 4.706–6.249× |
| 32 | 8 | 1 | bound | none | 12.50% / 12.50% | 8192/8192 | packedFtreeAggregate | 108.147 | 18.195 | 5.944× | 5.937–6.242× |
| 32 | 8 | 4 | bound | empty ≥ 769 | 12.50% / 12.50% | 0/8192 | auto | 111.538 | 3.051 | 35.739× | 26.891–36.561× |
| 32 | 8 | 4 | bound | empty ≥ 769 | 12.50% / 12.50% | 0/8192 | packedFtreeAggregate | 112.546 | 3.051 | 36.168× | 26.795–38.197× |
| 32 | 8 | 4 | bound | median ≥ 507 | 12.50% / 12.50% | 4096/8192 | auto | 115.095 | 13.916 | 8.271× | 7.788–9.357× |
| 32 | 8 | 4 | bound | median ≥ 507 | 12.50% / 12.50% | 4096/8192 | packedFtreeAggregate | 114.282 | 13.916 | 8.212× | 7.932–9.477× |
| 32 | 8 | 4 | bound | none | 12.50% / 12.50% | 8192/8192 | auto | 94.248 | 20.261 | 4.652× | 4.495–5.120× |
| 32 | 8 | 4 | bound | none | 12.50% / 12.50% | 8192/8192 | packedFtreeAggregate | 111.282 | 20.261 | 5.492× | 5.053–5.921× |
| 8 | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | auto | 26.033 | 2.528 | 9.854× | 7.082–11.818× |
| 8 | 1 | 1 | bound | empty ≥ 5 | 39.51% / 39.51% | 0/25895 | packedFtreeAggregate | 25.791 | 2.528 | 10.201× | 6.831–11.669× |
| 8 | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | auto | 52.955 | 43.460 | 1.193× | 1.145–1.300× |
| 8 | 1 | 1 | bound | median ≥ 1 | 39.51% / 39.51% | 25895/25895 | packedFtreeAggregate | 48.573 | 43.460 | 1.145× | 1.118–1.148× |
| 8 | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | auto | 43.864 | 38.957 | 1.121× | 1.084–1.206× |
| 8 | 1 | 1 | bound | none | 39.51% / 39.51% | 25895/25895 | packedFtreeAggregate | 44.027 | 38.957 | 1.130× | 1.064–1.171× |
| 8 | 1 | 4 | bound | empty ≥ 57 | 39.51% / 39.51% | 0/25895 | auto | 27.515 | 2.591 | 10.618× | 9.802–10.952× |
| 8 | 1 | 4 | bound | empty ≥ 57 | 39.51% / 39.51% | 0/25895 | packedFtreeAggregate | 24.862 | 2.591 | 10.115× | 9.594–10.174× |
| 8 | 1 | 4 | bound | median ≥ 8 | 39.51% / 39.51% | 14427/25895 | auto | 44.712 | 27.889 | 1.603× | 1.570–1.605× |
| 8 | 1 | 4 | bound | median ≥ 8 | 39.51% / 39.51% | 14427/25895 | packedFtreeAggregate | 44.477 | 27.889 | 1.675× | 1.488–1.675× |
| 8 | 1 | 4 | bound | none | 39.51% / 39.51% | 25895/25895 | auto | 50.083 | 41.281 | 1.188× | 1.094–1.213× |
| 8 | 1 | 4 | bound | none | 39.51% / 39.51% | 25895/25895 | packedFtreeAggregate | 43.805 | 41.281 | 1.125× | 0.951–1.143× |
| 8 | 8 | 1 | bound | empty ≥ 11 | 12.38% / 12.38% | 0/8111 | auto | 14.877 | 2.626 | 5.783× | 4.303–6.844× |
| 8 | 8 | 1 | bound | empty ≥ 11 | 12.38% / 12.38% | 0/8111 | packedFtreeAggregate | 15.128 | 2.626 | 5.761× | 4.463–6.223× |
| 8 | 8 | 1 | bound | median ≥ 4 | 12.38% / 12.38% | 4853/8111 | auto | 19.451 | 11.620 | 1.674× | 1.522–1.714× |
| 8 | 8 | 1 | bound | median ≥ 4 | 12.38% / 12.38% | 4853/8111 | packedFtreeAggregate | 18.565 | 11.620 | 1.537× | 1.499–1.696× |
| 8 | 8 | 1 | bound | none | 12.38% / 12.38% | 8111/8111 | auto | 19.655 | 14.650 | 1.328× | 1.121–1.342× |
| 8 | 8 | 1 | bound | none | 12.38% / 12.38% | 8111/8111 | packedFtreeAggregate | 19.587 | 14.650 | 1.375× | 1.081–1.534× |
| 8 | 8 | 4 | bound | empty ≥ 113 | 12.38% / 12.38% | 0/8111 | auto | 14.043 | 2.209 | 6.598× | 5.940–7.179× |
| 8 | 8 | 4 | bound | empty ≥ 113 | 12.38% / 12.38% | 0/8111 | packedFtreeAggregate | 14.610 | 2.209 | 6.577× | 6.243–7.826× |
| 8 | 8 | 4 | bound | median ≥ 21 | 12.38% / 12.38% | 4093/8111 | auto | 18.892 | 11.252 | 1.679× | 1.490–1.739× |
| 8 | 8 | 4 | bound | median ≥ 21 | 12.38% / 12.38% | 4093/8111 | packedFtreeAggregate | 19.374 | 11.252 | 1.732× | 1.648–1.990× |
| 8 | 8 | 4 | bound | none | 12.38% / 12.38% | 8111/8111 | auto | 19.788 | 16.284 | 1.206× | 1.147–1.393× |
| 8 | 8 | 4 | bound | none | 12.38% / 12.38% | 8111/8111 | packedFtreeAggregate | 21.236 | 16.284 | 1.273× | 1.051–1.527× |
