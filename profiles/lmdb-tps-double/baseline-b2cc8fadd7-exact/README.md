# LMDB TPS baseline at `b2cc8fadd7`

These results are the immutable pre-optimization baseline for the 2x TPS goal. Every benchmark uses sketches disabled, Zulu JDK 25, G1, a 2 GiB heap, one fork, two 10-second warmup iterations, and five 10-second measurement iterations. Method selectors were end-anchored because JMH interprets selectors as regular expressions.

| Benchmark | Baseline ops/s | 2x target ops/s | 99.9% error |
| --- | ---: | ---: | ---: |
| `TransactionsPerSecondBenchmark.transactions` | 34,279.010 | 68,558.020 | 2,022.764 |
| `TransactionsPerSecondBenchmark.transactionsLevelNone` | 28,203.265 | 56,406.530 | 2,506.596 |
| `TransactionsPerSecondBenchmark.mediumTransactionsLevelNone` | 7,552.965 | 15,105.930 | 1,145.996 |
| `TransactionsPerSecondBenchmark.largerTransaction` | 23.438 | 46.876 | 0.774 |
| `TransactionsPerSecondBenchmark.largerTransactionLevelNone` | 22.669 | 45.338 | 6.416 |
| `TransactionsPerSecondBenchmark.veryLargerTransactionLevelNone` | 0.446 | 0.892 | 0.033 |
| `TransactionsPerSecondBenchmarkFoaf.transaction1x` | 19,709.163 | 39,418.326 | 2,184.559 |
| `TransactionsPerSecondBenchmarkFoaf.transaction1xLevelNone` | 18,021.349 | 36,042.698 | 5,504.951 |
| `TransactionsPerSecondBenchmarkFoaf.transaction10x` | 8,444.172 | 16,888.344 | 831.336 |
| `TransactionsPerSecondBenchmarkFoaf.transaction10xLevelNone` | 7,926.912 | 15,853.824 | 4,051.966 |
| `TransactionsPerSecondBenchmarkFoaf.transaction10kx` | 35.400 | 70.800 | 8.154 |
| `TransactionsPerSecondBenchmarkFoaf.transaction10kxLevelNone` | 39.271 | 78.542 | 0.960 |
| `TransactionsPerSecondBenchmarkFoaf.transaction100kx` | 5.409 | 10.818 | 0.108 |
| `TransactionsPerSecondBenchmarkFoaf.transaction100kxLevelNone` | 5.706 | 11.412 | 0.186 |
| `TransactionsPerSecondForceSyncBenchmark.transactions` | 2,535.312 | 5,070.624 | 2,062.466 |
| `TransactionsPerSecondForceSyncBenchmark.transactionsLevelNone` | 3,308.962 | 6,617.924 | 370.337 |
| `TransactionsPerSecondForceSyncBenchmark.mediumTransactionsLevelNone` | 770.601 | 1,541.202 | 375.804 |
| `TransactionsPerSecondForceSyncBenchmark.largerTransaction` | 11.103 | 22.206 | 2.356 |
| `TransactionsPerSecondForceSyncBenchmark.largerTransactionLevelNone` | 11.931 | 23.862 | 1.567 |
| `TransactionsPerSecondForceSyncBenchmark.veryLargerTransactionLevelNone` | 0.356 | 0.712 | 0.091 |

The fourteen non-force-sync rows are the primary contract represented by the tracked `tps.md`. The force-sync rows are a conservative durability matrix and will be repeated where variance is too wide for a credible final ratio.
