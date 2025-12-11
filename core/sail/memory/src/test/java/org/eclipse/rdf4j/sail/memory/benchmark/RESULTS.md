## Without sketch-based join optimization
```
Benchmark                                                     Mode  Cnt    Score     Error  Units
QueryBenchmark.complexQuery                                   avgt    5    0.844 ±   0.042  ms/op
QueryBenchmark.contactPointPathChase                          avgt    5   28.138 ±   2.220  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5    0.421 ±   0.051  ms/op
QueryBenchmark.distributionMediaContrast                      avgt    5  344.017 ± 227.775  ms/op
QueryBenchmark.groupByQuery                                   avgt    5    0.443 ±   0.012  ms/op
QueryBenchmark.joinReorderStress                              avgt    5  290.568 ±  11.586  ms/op
QueryBenchmark.languageGroupHaving                            avgt    5    6.433 ±   0.733  ms/op
QueryBenchmark.languageUnionRegex                             avgt    5    1.663 ±   0.205  ms/op
QueryBenchmark.long_chain                                     avgt    5  151.923 ±  17.753  ms/op
QueryBenchmark.lots_of_optional                               avgt    5   33.559 ±   2.906  ms/op
QueryBenchmark.minus                                          avgt    5    3.739 ±   0.366  ms/op
QueryBenchmark.multipleSubSelect                              avgt    5   24.643 ±   2.674  ms/op
QueryBenchmark.nested_optionals                               avgt    5   49.773 ±   2.232  ms/op
QueryBenchmark.optionalFilterPushdown                         avgt    5   12.306 ±   1.903  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5    9.843 ±   1.295  ms/op
QueryBenchmark.optional_rhs_filter                            avgt    5   15.941 ±   0.877  ms/op
QueryBenchmark.overlappingOptionalsFiltered                   avgt    5   28.863 ±   2.263  ms/op
QueryBenchmark.overlappingOptionalsWide                       avgt    5   73.697 ±   4.823  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5    4.697 ±   0.815  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    0.364 ±   0.007  ms/op
QueryBenchmark.publisherDistributionAggregation               avgt    5   12.209 ±   1.107  ms/op
QueryBenchmark.query10                                        avgt    5  131.643 ±  15.015  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5   45.589 ±  11.050  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    1.833 ±   0.430  ms/op
QueryBenchmark.starPathFanout                                 avgt    5  100.289 ±   3.938  ms/op
QueryBenchmark.subSelect                                      avgt    5   43.164 ±   4.365  ms/op
QueryBenchmark.topTitlesByLength                              avgt    5    0.039 ±   0.002  ms/op
QueryBenchmark.unionPublisherDedup                            avgt    5  103.038 ±  13.775  ms/op
QueryBenchmark.valuesDupUnion                                 avgt    5  262.925 ±  20.421  ms/op

```

## With sketch-based join optimization
```
Benchmark                                                     Mode  Cnt    Score    Error  Units
QueryBenchmark.complexQuery                                   avgt    5    0.838 ±  0.058  ms/op
QueryBenchmark.contactPointPathChase                          avgt    5   27.064 ±  0.767  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5    0.341 ±  0.003  ms/op
QueryBenchmark.distributionMediaContrast                      avgt    5  237.293 ± 22.346  ms/op
QueryBenchmark.groupByQuery                                   avgt    5    0.547 ±  0.135  ms/op
QueryBenchmark.joinReorderStress                              avgt    5  316.609 ± 14.024  ms/op
QueryBenchmark.languageGroupHaving                            avgt    5    5.652 ±  0.313  ms/op
QueryBenchmark.languageUnionRegex                             avgt    5    1.554 ±  0.045  ms/op
QueryBenchmark.long_chain                                     avgt    5  133.697 ±  2.341  ms/op
QueryBenchmark.lots_of_optional                               avgt    5   29.869 ±  1.568  ms/op
QueryBenchmark.minus                                          avgt    5    3.849 ±  0.351  ms/op
QueryBenchmark.multipleSubSelect                              avgt    5   20.308 ±  1.059  ms/op
QueryBenchmark.nested_optionals                               avgt    5   46.416 ±  1.155  ms/op
QueryBenchmark.optionalFilterPushdown                         avgt    5   11.506 ±  0.565  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5    9.958 ±  0.629  ms/op
QueryBenchmark.optional_rhs_filter                            avgt    5   12.389 ±  0.851  ms/op
QueryBenchmark.overlappingOptionalsFiltered                   avgt    5   25.515 ±  0.895  ms/op
QueryBenchmark.overlappingOptionalsWide                       avgt    5   70.990 ±  2.499  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5    4.386 ±  0.218  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    0.394 ±  0.005  ms/op
QueryBenchmark.publisherDistributionAggregation               avgt    5   10.730 ±  0.536  ms/op
QueryBenchmark.query10                                        avgt    5  119.851 ±  5.581  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5   49.313 ±  3.660  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    1.892 ±  0.023  ms/op
QueryBenchmark.starPathFanout                                 avgt    5   92.395 ±  5.806  ms/op
QueryBenchmark.subSelect                                      avgt    5   36.521 ±  1.389  ms/op
QueryBenchmark.topTitlesByLength                              avgt    5    0.038 ±  0.001  ms/op
QueryBenchmark.unionPublisherDedup                            avgt    5   21.263 ±  0.248  ms/op
QueryBenchmark.valuesDupUnion                                 avgt    5    5.181 ±  0.348  ms/op
```


## With sketch-based join optimization (full reorder)
```
Benchmark                                                     Mode  Cnt    Score     Error  Units
QueryBenchmark.complexQuery                                   avgt    5    1.824 ±   0.350  ms/op
QueryBenchmark.contactPointPathChase                          avgt    5   28.115 ±   0.919  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5    1.711 ±   0.155  ms/op
QueryBenchmark.distributionMediaContrast                      avgt    5  418.272 ± 363.553  ms/op
QueryBenchmark.groupByQuery                                   avgt    5    0.488 ±   0.139  ms/op
QueryBenchmark.joinReorderStress                              avgt    5  338.483 ±  23.360  ms/op
QueryBenchmark.languageGroupHaving                            avgt    5    6.393 ±   1.444  ms/op
QueryBenchmark.languageUnionRegex                             avgt    5    3.863 ±   1.175  ms/op
QueryBenchmark.long_chain                                     avgt    5  166.316 ±  10.228  ms/op
QueryBenchmark.lots_of_optional                               avgt    5   33.397 ±   1.305  ms/op
QueryBenchmark.minus                                          avgt    5    3.903 ±   0.921  ms/op
QueryBenchmark.multipleSubSelect                              avgt    5   23.398 ±   4.796  ms/op
QueryBenchmark.nested_optionals                               avgt    5   51.926 ±   5.856  ms/op
QueryBenchmark.optionalFilterPushdown                         avgt    5   12.566 ±   3.723  ms/op
QueryBenchmark.optional_lhs_filter                            avgt    5   10.358 ±   1.452  ms/op 
QueryBenchmark.optional_rhs_filter                            avgt    5   15.658 ±   4.092  ms/op <--- This one is better
QueryBenchmark.overlappingOptionalsFiltered                   avgt    5   28.566 ±   7.722  ms/op
QueryBenchmark.overlappingOptionalsWide                       avgt    5   72.869 ±   4.751  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5    4.921 ±   1.622  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    0.470 ±   0.078  ms/op
QueryBenchmark.publisherDistributionAggregation               avgt    5   13.856 ±   2.057  ms/op
QueryBenchmark.query10                                        avgt    5  135.747 ±  27.544  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5   48.686 ±   5.479  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    2.036 ±   0.665  ms/op
QueryBenchmark.starPathFanout                                 avgt    5  100.842 ±   3.290  ms/op
QueryBenchmark.subSelect                                      avgt    5   44.990 ±   7.294  ms/op
QueryBenchmark.topTitlesByLength                              avgt    5    0.103 ±   0.009  ms/op
QueryBenchmark.unionPublisherDedup                            avgt    5   22.372 ±   2.000  ms/op
QueryBenchmark.valuesDupUnion                                 avgt    5    8.829 ±   0.476  ms/op
```
