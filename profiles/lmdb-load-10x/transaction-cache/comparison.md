# Transaction value-ID cache checkpoint

The acceptance workload is `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` with
`automaticEvaluationStrategy=false`, JDK 26, G1, `-Xms2G -Xmx2G`, the default two indexes, one fork, five one-second
warmups, five one-second measurements, and the JMH GC profiler.

| Isolation | Pooled baseline (ms/op) | Transaction cache (ms/op) | Time change | Baseline allocation (B/op) | Transaction cache allocation (B/op) | Allocation change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| NONE | 780.972 | 747.595 | -4.27% | 358,449,635 | 236,969,122 | -33.89% |
| READ_COMMITTED | 831.080 | 770.796 | -7.25% | 472,907,788 | 285,498,796 | -39.63% |

Measured iteration times were:

- `NONE`: 751.357, 752.754, 742.626, 748.149, and 743.087 ms/op.
- `READ_COMMITTED`: 773.606, 779.341, 784.229, 739.861, and 776.945 ms/op.

The exact result is `run-2-jdk26.json`. `run-1.json` is a JDK 25 diagnostic produced while rebuilding the jar; it is
not used in the acceptance comparison.
