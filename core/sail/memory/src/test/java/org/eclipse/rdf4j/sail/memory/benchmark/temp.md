## With sketches enabled

```
Benchmark                                                     Mode  Cnt    Score    Error  Units
QueryBenchmark.complexQuery                                   avgt    5   18.410 ±  0.513  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5    0.953 ±  0.016  ms/op
QueryBenchmark.groupByQuery                                   avgt    5    0.565 ±  0.012  ms/op
QueryBenchmark.long_chain                                     avgt    5  123.316 ±  8.546  ms/op
QueryBenchmark.lots_of_optional                               avgt    5   39.419 ±  3.083  ms/op
QueryBenchmark.minus                                          avgt    5  778.570 ± 44.976  ms/op
QueryBenchmark.multipleSubSelect                              avgt    5  125.835 ±  0.958  ms/op
QueryBenchmark.nested_optionals                               avgt    5   46.466 ±  1.133  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5    9.946 ±  0.735  ms/op
QueryBenchmark.optional_rhs_filter                            avgt    5   16.468 ±  2.377  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5    3.986 ±  0.150  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    0.488 ±  0.013  ms/op
QueryBenchmark.query10                                        avgt    5  238.342 ±  9.302  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5   35.472 ±  2.948  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    1.866 ±  0.215  ms/op
QueryBenchmark.subSelect                                      avgt    5  141.902 ±  0.408  ms/op
```

## Sketeches disabled
```
Benchmark                                                     Mode  Cnt     Score    Error  Units
QueryBenchmark.complexQuery                                   avgt    5    13.971 ±  0.762  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5     0.459 ±  0.016  ms/op
QueryBenchmark.groupByQuery                                   avgt    5     0.549 ±  0.032  ms/op
QueryBenchmark.long_chain                                     avgt    5   115.460 ±  8.114  ms/op
QueryBenchmark.lots_of_optional                               avgt    5    38.796 ±  0.833  ms/op
QueryBenchmark.minus                                          avgt    5   768.421 ± 22.720  ms/op
QueryBenchmark.multipleSubSelect                              avgt    5   197.285 ±  7.302  ms/op
QueryBenchmark.nested_optionals                               avgt    5    47.261 ±  0.539  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5    12.443 ±  2.394  ms/op
QueryBenchmark.optional_rhs_filter                            avgt    5    18.858 ±  3.640  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5     4.673 ±  1.086  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5     0.483 ±  0.016  ms/op
QueryBenchmark.query10                                        avgt    5  1170.793 ± 39.531  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5    49.513 ±  8.388  ms/op
QueryBenchmark.simple_filter_not                              avgt    5     1.664 ±  0.171  ms/op
QueryBenchmark.subSelect                                      avgt    5   229.672 ±  7.602  ms/op

```
