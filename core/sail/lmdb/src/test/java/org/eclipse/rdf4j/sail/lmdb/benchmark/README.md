## pool cursors
```
Benchmark                                                     Mode  Cnt    Score    Error  Units
QueryBenchmark.complexQuery                                   avgt    5    3.724 ±  0.083  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5    2.118 ±  0.089  ms/op
QueryBenchmark.groupByQuery                                   avgt    5    0.916 ±  0.019  ms/op
QueryBenchmark.long_chain                                     avgt    5  641.809 ±  6.881  ms/op
QueryBenchmark.minus                                          avgt    5   10.472 ±  0.556  ms/op
QueryBenchmark.multiple_sub_select                            avgt    5   55.949 ±  3.581  ms/op
QueryBenchmark.nested_optionals                               avgt    5  171.423 ± 39.898  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5   36.728 ±  2.244  ms/op
QueryBenchmark.optional_rhs_filter                            avgt    5   51.788 ±  2.241  ms/op
QueryBenchmark.ordered_union_limit                            avgt    5   73.121 ±  4.403  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5   20.738 ±  0.385  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    4.546 ±  0.338  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5   47.712 ±  1.803  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    5.641 ±  0.151  ms/op
QueryBenchmark.sub_select                                     avgt    5   72.550 ± 10.050  ms/op
QueryBenchmarkFoaf.groupByCount                               avgt    5  721.870 ± 16.816  ms/op
QueryBenchmarkFoaf.groupByCountSorted                         avgt    5  651.351 ± 19.324  ms/op
QueryBenchmarkFoaf.personsAndFriends                          avgt    5  213.945 ±  7.394  ms/op
```

### dup key
```
Benchmark                                                     Mode  Cnt    Score    Error  Units
QueryBenchmark.complexQuery                                   avgt    5    3.451 ±  0.026  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5    2.523 ±  0.018  ms/op
QueryBenchmark.groupByQuery                                   avgt    5    0.875 ±  0.011  ms/op
QueryBenchmark.long_chain                                     avgt    5  674.397 ±  6.014  ms/op
QueryBenchmark.lots_of_optional                               avgt    5  295.217 ±  2.850  ms/op
QueryBenchmark.minus                                          avgt    5   10.704 ±  0.640  ms/op
QueryBenchmark.multiple_sub_select                            avgt    5   57.411 ±  1.345  ms/op
QueryBenchmark.nested_optionals                               avgt    5  218.365 ±  0.449  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5   34.577 ±  0.341  ms/op
QueryBenchmark.optional_rhs_filter                            avgt    5   65.118 ±  0.355  ms/op
QueryBenchmark.ordered_union_limit                            avgt    5   68.742 ±  1.475  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5   20.378 ±  0.232  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    5.127 ±  0.031  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5   44.293 ±  0.293  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    5.509 ±  0.061  ms/op
QueryBenchmark.sub_select                                     avgt    5   76.451 ±  0.970  ms/op
QueryBenchmarkFoaf.groupByCount                               avgt    5  742.100 ± 11.235  ms/op
QueryBenchmarkFoaf.groupByCountSorted                         avgt    5  602.247 ± 31.896  ms/op
QueryBenchmarkFoaf.personsAndFriends                          avgt    5  218.583 ±  1.921  ms/op

```


### Instance of LmdbIRI
```text
Benchmark                                                     Mode  Cnt    Score     Error  Units
QueryBenchmark.complexQuery                                   avgt    5    3.431 ±   0.027  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5    2.504 ±   0.008  ms/op
QueryBenchmark.groupByQuery                                   avgt    5    0.865 ±   0.006  ms/op
QueryBenchmark.long_chain                                     avgt    5  670.361 ±   5.933  ms/op
QueryBenchmark.lots_of_optional                               avgt    5  294.593 ±   3.254  ms/op
QueryBenchmark.minus                                          avgt    5   10.788 ±   0.241  ms/op
QueryBenchmark.multiple_sub_select                            avgt    5   56.797 ±   0.608  ms/op
QueryBenchmark.nested_optionals                               avgt    5  217.519 ±   2.129  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5   34.818 ±   0.603  ms/op
QueryBenchmark.optional_rhs_filter                            avgt    5   64.181 ±   0.366  ms/op
QueryBenchmark.ordered_union_limit                            avgt    5   68.970 ±   0.575  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5   20.395 ±   0.229  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    4.756 ±   0.041  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5   49.811 ±   1.072  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    5.559 ±   0.049  ms/op
QueryBenchmark.sub_select                                     avgt    5   74.768 ±   1.344  ms/op
QueryBenchmarkFoaf.groupByCount                               avgt    5  707.383 ±  13.236  ms/op
QueryBenchmarkFoaf.groupByCountSorted                         avgt    5  633.781 ± 159.621  ms/op
QueryBenchmarkFoaf.personsAndFriends                          avgt    5  214.109 ±   3.490  ms/op
```
