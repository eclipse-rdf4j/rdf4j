
```
Benchmark                                                     Mode  Cnt     Score     Error  Units
QueryBenchmark.complexQuery                                   avgt    3   973.922 ± 221.832  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    3     4.560 ±   0.686  ms/op
QueryBenchmark.groupByQuery                                   avgt    3     1.550 ±   0.082  ms/op
QueryBenchmark.long_chain                                     avgt    3  1272.403 ± 252.444  ms/op
QueryBenchmark.lots_of_optional                               avgt    3   444.513 ±  27.674  ms/op
QueryBenchmark.minus                                          avgt    3   970.190 ±  32.938  ms/op
QueryBenchmark.nested_optionals                               avgt    3   271.831 ±  43.975  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    3    47.796 ±   3.139  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    3    10.934 ±   0.755  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    3    77.214 ±   1.614  ms/op
QueryBenchmark.simple_filter_not                              avgt    3    12.707 ±   0.842  ms/op
QueryBenchmarkFoaf.groupByCount                               avgt    3  1061.455 ±  23.814  ms/op
QueryBenchmarkFoaf.groupByCountSorted                         avgt    3   981.977 ± 278.497  ms/op
QueryBenchmarkFoaf.personsAndFriends                          avgt    3   497.006 ±  21.121  ms/op
```


# Sketch disabled
```
Benchmark                                                     Mode  Cnt     Score     Error  Units
QueryBenchmark.complexQuery                                   avgt    3  1359.329 ±  61.359  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    3     4.432 ±   1.614  ms/op
QueryBenchmark.groupByQuery                                   avgt    3     1.532 ±   0.018  ms/op
QueryBenchmark.long_chain                                     avgt    3  1274.135 ± 108.420  ms/op
QueryBenchmark.lots_of_optional                               avgt    3   447.965 ±   4.143  ms/op
QueryBenchmark.minus                                          avgt    3   996.523 ± 362.187  ms/op
QueryBenchmark.nested_optionals                               avgt    3   269.161 ±  61.094  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    3    47.786 ±  30.660  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    3    11.222 ±   3.980  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    3    71.709 ±   3.867  ms/op
QueryBenchmark.simple_filter_not                              avgt    3    12.333 ±   0.370  ms/op
QueryBenchmarkFoaf.groupByCount                               avgt       1292.244            ms/op
QueryBenchmarkFoaf.groupByCountSorted                         avgt       1185.806            ms/op
QueryBenchmarkFoaf.personsAndFriends                          avgt        500.712            ms/op
```
