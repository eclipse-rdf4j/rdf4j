Original benchmark results 

```
Benchmark                                                     Mode  Cnt     Score    Error  Units
QueryBenchmark.complexQuery                                   avgt    5     6.786 ±  0.968  ms/op
QueryBenchmark.different_datasets_with_similar_distributions  avgt    5     4.056 ±  0.040  ms/op
QueryBenchmark.groupByQuery                                   avgt    5     1.425 ±  0.005  ms/op
QueryBenchmark.long_chain                                     avgt    5  1180.336 ± 46.383  ms/op
QueryBenchmark.lots_of_optional                               avgt    5   428.926 ±  7.985  ms/op
QueryBenchmark.minus                                          avgt    5  1042.468 ± 46.901  ms/op
QueryBenchmark.nested_optionals                               avgt    5   254.052 ±  4.293  ms/op
QueryBenchmark.pathExpressionQuery1                           avgt    5    44.147 ±  1.200  ms/op
QueryBenchmark.pathExpressionQuery2                           avgt    5    10.732 ±  0.176  ms/op
QueryBenchmark.query_distinct_predicates                      avgt    5    70.255 ±  3.541  ms/op
QueryBenchmark.simple_filter_not                              avgt    5    11.890 ±  0.761  ms/op

```
