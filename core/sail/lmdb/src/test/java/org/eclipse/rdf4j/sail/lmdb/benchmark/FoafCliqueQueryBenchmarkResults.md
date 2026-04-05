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
