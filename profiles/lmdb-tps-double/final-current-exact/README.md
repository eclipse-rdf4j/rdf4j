# Final sketch-disabled LMDB TPS result

Measurements use the same settings as the immutable `b2cc8fadd7` baseline: Zulu JDK 25, G1, a 2 GiB heap,
one fork, two 10-second warmup iterations, five 10-second measurement iterations, and sketches disabled.

| Benchmark | Baseline ops/s | Final ops/s | Ratio |
| --- | ---: | ---: | ---: |
| `TransactionsPerSecondBenchmark.transactions` | 34,279.010 | 81,494.384 | 2.377x |
| `TransactionsPerSecondBenchmark.transactionsLevelNone` | 28,203.265 | 88,071.848 | 3.123x |
| `TransactionsPerSecondBenchmark.mediumTransactionsLevelNone` | 7,552.965 | 47,365.850 | 6.271x |
| `TransactionsPerSecondBenchmark.largerTransaction` | 23.438 | 166.899 | 7.121x |
| `TransactionsPerSecondBenchmark.largerTransactionLevelNone` | 22.669 | 181.216 | 7.994x |
| `TransactionsPerSecondBenchmark.veryLargerTransactionLevelNone` | 0.446 | 1.744 | 3.910x |
| `TransactionsPerSecondBenchmarkFoaf.transaction1x` | 19,709.163 | 68,401.358 | 3.471x |
| `TransactionsPerSecondBenchmarkFoaf.transaction1xLevelNone` | 18,021.349 | 72,229.534 | 4.008x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10x` | 8,444.172 | 48,899.575 | 5.791x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10xLevelNone` | 7,926.912 | 52,386.741 | 6.609x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10kx` | 35.400 | 231.628 | 6.543x |
| `TransactionsPerSecondBenchmarkFoaf.transaction10kxLevelNone` | 39.271 | 231.025 | 5.883x |
| `TransactionsPerSecondBenchmarkFoaf.transaction100kx` | 5.409 | 21.989 | 4.065x |
| `TransactionsPerSecondBenchmarkFoaf.transaction100kxLevelNone` | 5.706 | 21.351 | 3.742x |
| `TransactionsPerSecondForceSyncBenchmark.transactions` | 2,535.312 | 11,987.248 | 4.728x |
| `TransactionsPerSecondForceSyncBenchmark.transactionsLevelNone` | 3,308.962 | 12,072.245 | 3.648x |
| `TransactionsPerSecondForceSyncBenchmark.mediumTransactionsLevelNone` | 770.601 | 7,428.544 | 9.640x |
| `TransactionsPerSecondForceSyncBenchmark.largerTransaction` | 11.103 | 163.655 | 14.740x |
| `TransactionsPerSecondForceSyncBenchmark.largerTransactionLevelNone` | 11.931 | 176.551 | 14.798x |
| `TransactionsPerSecondForceSyncBenchmark.veryLargerTransactionLevelNone` | 0.356 | 1.688 | 4.742x |

All twenty point estimates exceed 2.00x. The narrowest final margin is
`TransactionsPerSecondBenchmark.transactions` at 2.377x. The three sibling benchmark result files are the raw
JMH CSV outputs.
