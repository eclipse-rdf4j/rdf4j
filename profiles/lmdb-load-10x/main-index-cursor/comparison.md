# Main-index cursor checkpoint

The aligned writer now retains an ordinary write cursor for the main index and calls `mdb_cursor_put` with
`MDB_NOOVERWRITE`, matching the cursor lifetime already used for secondary indexes. No append flags are used.

Both runs used the exact `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction` method with JDK 26, G1,
`-Xms2G -Xmx2G`, `automaticEvaluationStrategy=false`, both requested isolation modes, one fork, and five one-second
measurements. Run 1 used three warmups and run 2 used five.

| Isolation | Transaction-cache checkpoint (ms/op) | Cursor run 1 | Cursor run 2 | Cursor pooled | Checkpoint change | Original baseline change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| NONE | 747.595 | 707.080 | 704.648 | 705.864 | -5.58% | -9.62% |
| READ_COMMITTED | 770.796 | 711.227 | 714.352 | 712.789 | -7.53% | -14.23% |

Correctness evidence before the benchmark:

- `TripleStoreTest`: 26 tests, zero failures/errors.
- `TripleStoreAlignedSortResetTest` and `TripleStoreAutoGrowTest`: 9 tests, zero failures/errors.
