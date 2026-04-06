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

``` 
Benchmark                                                     (benchmarkMode)  (cliquePercentage)  (maxCliqueSize)  (minCliqueSize)  (peopleCount)  (randomKnowsEdges)  (seed)  Mode  Cnt     Score     Error  Units
FoafCliqueQueryBenchmark.cycle3                                   interpreted                  30                8                3           5000               15000   12345  avgt    5    19.139 ±   0.249  ms/op
FoafCliqueQueryBenchmark.cycle3                              executor_codegen                  30                8                3           5000               15000   12345  avgt    5    18.083 ±   1.050  ms/op
FoafCliqueQueryBenchmark.cycle3                                  full_codegen                  30                8                3           5000               15000   12345  avgt    5    14.350 ±   0.234  ms/op
FoafCliqueQueryBenchmark.cycle3                                      disabled                  30                8                3           5000               15000   12345  avgt    5   101.270 ±   2.698  ms/op

FoafCliqueQueryBenchmark.cycle3CountCityInterest                  interpreted                  30                8                3           5000               15000   12345  avgt    5    63.494 ±   5.929  ms/op
FoafCliqueQueryBenchmark.cycle3CountCityInterest             executor_codegen                  30                8                3           5000               15000   12345  avgt    5    56.843 ±   4.770  ms/op
FoafCliqueQueryBenchmark.cycle3CountCityInterest                 full_codegen                  30                8                3           5000               15000   12345  avgt    5    49.831 ±   1.542  ms/op
FoafCliqueQueryBenchmark.cycle3CountCityInterest                     disabled                  30                8                3           5000               15000   12345  avgt    5    76.904 ±   1.780  ms/op

FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered                interpreted                  30                8                3           5000               15000   12345  avgt    5   150.576 ±   5.361  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered           executor_codegen                  30                8                3           5000               15000   12345  avgt    5   128.802 ±   5.240  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered               full_codegen                  30                8                3           5000               15000   12345  avgt    5    96.154 ±   4.793  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered                   disabled                  30                8                3           5000               15000   12345  avgt    5   100.160 ±   2.329  ms/op

FoafCliqueQueryBenchmark.cycle3GroupedInterest                    interpreted                  30                8                3           5000               15000   12345  avgt    5    51.695 ±   3.045  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest               executor_codegen                  30                8                3           5000               15000   12345  avgt    5    50.501 ±   2.451  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest                   full_codegen                  30                8                3           5000               15000   12345  avgt    5    40.283 ±   2.956  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest                       disabled                  30                8                3           5000               15000   12345  avgt    5    75.723 ±   2.881  ms/op

FoafCliqueQueryBenchmark.cycle4                                   interpreted                  30                8                3           5000               15000   12345  avgt    5    83.653 ±   3.626  ms/op
FoafCliqueQueryBenchmark.cycle4                              executor_codegen                  30                8                3           5000               15000   12345  avgt    5    76.264 ±  13.538  ms/op
FoafCliqueQueryBenchmark.cycle4                                  full_codegen                  30                8                3           5000               15000   12345  avgt    5    60.321 ±   3.252  ms/op
FoafCliqueQueryBenchmark.cycle4                                      disabled                  30                8                3           5000               15000   12345  avgt    5   685.196 ±  26.388  ms/op

FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered              interpreted                  30                8                3           5000               15000   12345  avgt    5   242.550 ±   8.657  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered         executor_codegen                  30                8                3           5000               15000   12345  avgt    5   209.008 ±  12.205  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered             full_codegen                  30                8                3           5000               15000   12345  avgt    5   159.240 ±  15.824  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered                 disabled                  30                8                3           5000               15000   12345  avgt    5   294.365 ±   5.560  ms/op

FoafCliqueQueryBenchmark.cycle5                                   interpreted                  30                8                3           5000               15000   12345  avgt    5   419.527 ±  33.089  ms/op
FoafCliqueQueryBenchmark.cycle5                              executor_codegen                  30                8                3           5000               15000   12345  avgt    5   286.429 ±   8.196  ms/op
FoafCliqueQueryBenchmark.cycle5                                  full_codegen                  30                8                3           5000               15000   12345  avgt    5   277.975 ±  17.327  ms/op
FoafCliqueQueryBenchmark.cycle5                                      disabled                  30                8                3           5000               15000   12345  avgt    5  4272.863 ± 439.501  ms/op

FoafCliqueQueryBenchmark.cycle5ValuesCountMailboxHomepage         interpreted                  30                8                3           5000               15000   12345  avgt    5  1875.367 ±  77.732  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesCountMailboxHomepage    executor_codegen                  30                8                3           5000               15000   12345  avgt    5  1450.226 ±  82.264  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesCountMailboxHomepage        full_codegen                  30                8                3           5000               15000   12345  avgt    5  1129.560 ±  18.134  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesCountMailboxHomepage            disabled                  30                8                3           5000               15000   12345  avgt    5  1808.042 ±  27.328  ms/op

FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered       interpreted                  30                8                3           5000               15000   12345  avgt    5   703.544 ±  35.325  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered  executor_codegen                  30                8                3           5000               15000   12345  avgt    5   556.799 ±  29.910  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered      full_codegen                  30                8                3           5000               15000   12345  avgt    5   455.503 ±  20.033  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered          disabled                  30                8                3           5000               15000   12345  avgt    5  2165.313 ±  74.562  ms/op
```

``` 
Benchmark                                                    (benchmarkMode)  (cliquePercentage)  (maxCliqueSize)  (minCliqueSize)  (peopleCount)  (randomKnowsEdges)  (seed)  Mode  Cnt     Score    Error  Units
FoafCliqueQueryBenchmark.cycle3                                 full_codegen                  30                8                3           5000               15000   12345  avgt    5    12.145 ±  0.697  ms/op
FoafCliqueQueryBenchmark.cycle3CountCityInterest                full_codegen                  30                8                3           5000               15000   12345  avgt    5    36.130 ±  2.730  ms/op
FoafCliqueQueryBenchmark.cycle3DistinctCityOrdered              full_codegen                  30                8                3           5000               15000   12345  avgt    5    90.575 ±  8.376  ms/op
FoafCliqueQueryBenchmark.cycle3GroupedInterest                  full_codegen                  30                8                3           5000               15000   12345  avgt    5    33.409 ±  2.409  ms/op
FoafCliqueQueryBenchmark.cycle4                                 full_codegen                  30                8                3           5000               15000   12345  avgt    5    54.210 ±  3.619  ms/op
FoafCliqueQueryBenchmark.cycle4ValuesFilteredOrdered            full_codegen                  30                8                3           5000               15000   12345  avgt    5   145.352 ± 11.220  ms/op
FoafCliqueQueryBenchmark.cycle5                                 full_codegen                  30                8                3           5000               15000   12345  avgt    5   255.649 ± 11.363  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesCountMailboxHomepage       full_codegen                  30                8                3           5000               15000   12345  avgt    5  1088.888 ± 24.557  ms/op
FoafCliqueQueryBenchmark.cycle5ValuesDistinctMailboxOrdered     full_codegen                  30                8                3           5000               15000   12345  avgt    5   437.191 ± 53.469  ms/op
```
