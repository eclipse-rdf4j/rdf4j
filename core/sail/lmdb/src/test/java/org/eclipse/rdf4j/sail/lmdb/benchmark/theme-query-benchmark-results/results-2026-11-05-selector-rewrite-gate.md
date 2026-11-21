# Selector rewrite benchmark gate, 2026-11-05

Baseline: `main` at `6e51368d6a`. Branch: `GH-0000-lmdb-native-selector-rewrite-m3` with M6 applied. Both built with `scripts/run-single-benchmark.sh`,
3 warmup and 5 measurement iterations, one fork, run alternately on the same machine, which was not idle. Scores are ms/op, lower is better; FOAF rows use 5000 people, the native engine and the default store.

| Benchmark | main | branch | branch / main |
| :--- | ---: | ---: | ---: |
| cycle3 | 26.492 | 23.576 | 0.89 |
| cycle3CountCityInterest | 8.530 | 9.881 | 1.16 |
| cycle3DistinctCityOrdered | 52.697 | 58.990 | 1.12 |
| cycle3GroupedInterest | 16.550 | 12.420 | 0.75 |
| cycle4 | 111.153 | 92.668 | 0.83 |
| cycle4ValuesFilteredOrdered | 77.776 | 48.136 | 0.62 |
| cycle5 | 352.908 | 330.302 | 0.94 |
| cycle5ValuesCountMailboxHomepage | 201.673 | 121.776 | 0.60 |
| cycle5ValuesDistinctMailboxOrdered | 485.966 | 355.547 | 0.73 |
| distinctAggregates | 8.758 | 6.165 | 0.70 |
| fewGroups | 0.120 | 0.129 | 1.08 |
| manyGroups | 59.912 | 61.063 | 1.02 |
| spillForced | 101.858 | 99.745 | 0.98 |
| wideKeys | 150.354 | 134.925 | 0.90 |

Repeat of the two rows the first pass flagged, three rounds, 4 warmup and 8 measurement iterations:

| Benchmark | main, by round | branch, by round |
| :--- | ---: | ---: |
| cycle3CountCityInterest | 9.706, 9.153, 8.329 | 11.574, 10.331, 8.425 |
| cycle3DistinctCityOrdered | 56.928, 53.134, 66.435 | 58.040, 66.513, 58.788 |

`cycle3DistinctCityOrdered` is noise: the rounds disagree on which side is faster. `cycle3CountCityInterest` is slower in all five comparisons, by about 1 ms on 9 ms, and is left open.
`AggregationBenchmark.fewGroups` crashed once on `main` and passed on a rerun, which is the number shown.
