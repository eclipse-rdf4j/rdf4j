# Exact sketch-disabled LMDB TPS result

Candidate measurements use the same settings as the immutable `b2cc8fadd7` baseline: Zulu JDK 25, G1, a 2 GiB
heap, one fork, two 10-second warmup iterations, five 10-second measurement iterations, and sketches disabled.

| Benchmark | Baseline ops/s | Candidate ops/s | Ratio |
| --- | ---: | ---: | ---: |
| `TransactionsPerSecondBenchmark.transactions` | 34,279.010 | 82,428.444 | 2.405x |
| `TransactionsPerSecondBenchmark.transactionsLevelNone` | 28,203.265 | 73,599.338 | 2.610x |
| `TransactionsPerSecondBenchmark.mediumTransactionsLevelNone` | 7,552.965 | 48,626.538 | 6.438x |
| `TransactionsPerSecondBenchmark.largerTransaction` | 23.438 | 66.599 | 2.841x |
| `TransactionsPerSecondBenchmark.largerTransactionLevelNone` | 22.669 | 182.395 | 8.046x |
| `TransactionsPerSecondBenchmark.veryLargerTransactionLevelNone` | 0.446 | 1.367 | 3.065x |
| `TransactionsPerSecondBenchmarkFoaf.transaction1x` | 19,709.163 | 66,322.006 | 3.365x |
| `TransactionsPerSecondBenchmarkFoaf.transaction1xLevelNone` | 18,021.349 | 71,922.164 | 3.991x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10x` | 8,444.172 | 33,012.611 | 3.910x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10xLevelNone` | 7,926.912 | 54,242.688 | 6.843x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10kx` | 35.400 | 231.351 | 6.535x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10kxLevelNone` | 39.271 | 236.367 | 6.019x |
| `TransactionsPerSecondBenchmarkFoaf.transaction100kx` | 5.409 | 21.536 | 3.982x |
| `TransactionsPerSecondBenchmarkFoaf.transaction100kxLevelNone` | 5.706 | 23.220 | 4.069x |
| `TransactionsPerSecondForceSyncBenchmark.transactions` | 2,535.312 | 6,642.403 | 2.620x |
| `TransactionsPerSecondForceSyncBenchmark.transactionsLevelNone` | 3,308.962 | 6,792.604 | 2.053x |
| `TransactionsPerSecondForceSyncBenchmark.mediumTransactionsLevelNone` | 770.601 | 6,511.092 | 8.449x |
| `TransactionsPerSecondForceSyncBenchmark.largerTransaction` | 11.103 | 165.001 | 14.861x |
| `TransactionsPerSecondForceSyncBenchmark.largerTransactionLevelNone` | 11.931 | 182.890 | 15.329x |
| `TransactionsPerSecondForceSyncBenchmark.veryLargerTransactionLevelNone` | 0.356 | 0.794 | 2.230x |

All twenty point estimates exceed 2.00x. The narrowest margin is force-sync `transactionsLevelNone` at 2.053x.
Raw JMH tables are the three sibling `.txt` files.
