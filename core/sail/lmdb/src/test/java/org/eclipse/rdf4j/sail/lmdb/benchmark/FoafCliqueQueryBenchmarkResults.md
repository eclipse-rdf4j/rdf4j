```
Benchmark                        (cliquePercentage)  (lftjCodegenEnabled)  (lftjEnabled)  (maxCliqueSize)  (minCliqueSize)  (peopleCount)  (randomKnowsEdges)  (seed)  Mode  Cnt     Score     Error  Units
FoafCliqueQueryBenchmark.cycle3                  30                 false          false                8                3           5000               15000   12345  avgt    3    89.718 ±   6.997  ms/op
FoafCliqueQueryBenchmark.cycle4                  30                 false          false                8                3           5000               15000   12345  avgt    3   569.446 ±  38.531  ms/op
FoafCliqueQueryBenchmark.cycle5                  30                 false          false                8                3           5000               15000   12345  avgt    3  3814.985 ± 530.638  ms/op
```

```
Benchmark                         (benchmarkMode)  (cliquePercentage)  (maxCliqueSize)  (minCliqueSize)  (peopleCount)  (randomKnowsEdges)  (seed)  Mode  Cnt    Score     Error  Units
FoafCliqueQueryBenchmark.cycle3       interpreted                  30                8                3           5000               15000   12345  avgt    3   24.902 ±   5.810  ms/op
FoafCliqueQueryBenchmark.cycle3  executor_codegen                  30                8                3           5000               15000   12345  avgt    3   23.965 ±   4.651  ms/op
FoafCliqueQueryBenchmark.cycle3      full_codegen                  30                8                3           5000               15000   12345  avgt    3   16.673 ±   3.116  ms/op
FoafCliqueQueryBenchmark.cycle4       interpreted                  30                8                3           5000               15000   12345  avgt    3  121.399 ±  61.014  ms/op
FoafCliqueQueryBenchmark.cycle4  executor_codegen                  30                8                3           5000               15000   12345  avgt    3  108.525 ±  27.221  ms/op
FoafCliqueQueryBenchmark.cycle4      full_codegen                  30                8                3           5000               15000   12345  avgt    3   82.306 ±  30.173  ms/op
FoafCliqueQueryBenchmark.cycle5       interpreted                  30                8                3           5000               15000   12345  avgt    3  702.982 ± 103.059  ms/op
FoafCliqueQueryBenchmark.cycle5  executor_codegen                  30                8                3           5000               15000   12345  avgt    3  663.095 ± 236.201  ms/op
FoafCliqueQueryBenchmark.cycle5      full_codegen                  30                8                3           5000               15000   12345  avgt    3  520.210 ± 120.747  ms/op
```

```
Benchmark                                                     (benchmarkMode)  (cliquePercentage)  (maxCliqueSize)  (minCliqueSize)  (peopleCount)  (randomKnowsEdges)  (seed)  Mode  Cnt     Score     Error  Units
FoafCliqueQueryBenchmark.cycle3                                   interpreted                  30                8                3           5000               15000   12345  avgt    5    21.555 ±   3.110  ms/op
FoafCliqueQueryBenchmark.cycle3                              executor_codegen                  30                8                3           5000               15000   12345  avgt    5    18.401 ±   1.086  ms/op
FoafCliqueQueryBenchmark.cycle3                                  full_codegen                  30                8                3           5000               15000   12345  avgt    5    14.880 ±   1.150  ms/op
FoafCliqueQueryBenchmark.cycle3                                      disabled                  30                8                3           5000               15000   12345  avgt    5   101.933 ±   4.997  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered                interpreted                  30                8                3           5000               15000   12345  avgt    5   146.240 ±   9.376  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered           executor_codegen                  30                8                3           5000               15000   12345  avgt    5   144.603 ±  27.824  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered               full_codegen                  30                8                3           5000               15000   12345  avgt    5   110.024 ±  13.207  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered                   disabled                  30                8                3           5000               15000   12345  avgt    5   102.194 ±   7.423  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest                    interpreted                  30                8                3           5000               15000   12345  avgt    5    54.924 ±   6.028  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest               executor_codegen                  30                8                3           5000               15000   12345  avgt    5    55.310 ±   5.366  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest                   full_codegen                  30                8                3           5000               15000   12345  avgt    5    43.784 ±   0.896  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest                       disabled                  30                8                3           5000               15000   12345  avgt    5    75.099 ±   1.030  ms/op
FoafCliqueQueryBenchmark.cycle4                                   interpreted                  30                8                3           5000               15000   12345  avgt    5    88.792 ±   4.729  ms/op
FoafCliqueQueryBenchmark.cycle4                              executor_codegen                  30                8                3           5000               15000   12345  avgt    5    64.010 ±   1.059  ms/op
FoafCliqueQueryBenchmark.cycle4                                  full_codegen                  30                8                3           5000               15000   12345  avgt    5    60.457 ±   2.042  ms/op
FoafCliqueQueryBenchmark.cycle4                                      disabled                  30                8                3           5000               15000   12345  avgt    5   670.650 ±   7.958  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered              interpreted                  30                8                3           5000               15000   12345  avgt    5   247.547 ±  20.613  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered         executor_codegen                  30                8                3           5000               15000   12345  avgt    5   224.290 ±  31.904  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered             full_codegen                  30                8                3           5000               15000   12345  avgt    5   188.459 ±  29.016  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered                 disabled                  30                8                3           5000               15000   12345  avgt    5   298.370 ±  15.842  ms/op
FoafCliqueQueryBenchmark.cycle5                                   interpreted                  30                8                3           5000               15000   12345  avgt    5   481.559 ±  33.314  ms/op
FoafCliqueQueryBenchmark.cycle5                              executor_codegen                  30                8                3           5000               15000   12345  avgt    5   324.419 ±  51.822  ms/op
FoafCliqueQueryBenchmark.cycle5                                  full_codegen                  30                8                3           5000               15000   12345  avgt    5   268.049 ±   4.831  ms/op
FoafCliqueQueryBenchmark.cycle5                                      disabled                  30                8                3           5000               15000   12345  avgt    5  4189.973 ±  33.778  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered       interpreted                  30                8                3           5000               15000   12345  avgt    5  3545.949 ± 193.672  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered  executor_codegen                  30                8                3           5000               15000   12345  avgt    5  2919.335 ±  32.839  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered      full_codegen                  30                8                3           5000               15000   12345  avgt    5  2135.299 ±  45.432  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered          disabled                  30                8                3           5000               15000   12345  avgt    5  2163.481 ±  82.503  ms/op
```
