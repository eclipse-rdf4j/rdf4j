# LMDB scalar TPS scratch-reuse checkpoint

This checkpoint optimizes the adaptive scalar `SailSink.approve` path used by
`TransactionsPerSecondBenchmark.largerTransactionLevelNone`. It retains the existing LMDB operation sequence but
reuses transaction-owned value-resolution arrays and replaces `HashMap<Value, Integer>` with the existing primitive
`ObjectIntHashMap<Value>` implementation.

Both profiled runs used the exact selector below with Linux Java 26, G1, a 2 GiB heap, no warmup, ten 10-second
measurements, one fork, CPU-time JFR, 1,024-frame stacks, and debug non-safepoints:

    .codex/skills/docker-jfr-benchmark-loop/scripts/run-docker-jfr-loop.sh \
      --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.TransactionsPerSecondBenchmark \
      --method largerTransactionLevelNone

## Result

| Run | Score | Stable iteration range | Notes |
| --- | ---: | ---: | --- |
| Baseline | 19.041 ± 8.864 ops/s | 19.553-23.021 ops/s | One 2.564 ops/s outlier; nine-iteration mean 20.872 ops/s |
| Scratch reuse | 21.192 ± 0.437 ops/s | 20.694-21.492 ops/s | All ten iterations stable |

The candidate is 1.5% above the baseline mean after removing its single extreme outlier. The raw-score delta is not a
credible estimate because the baseline outlier depresses its ten-iteration mean. Throughput should therefore be
described as roughly flat to 1.5% higher, not as an 11% improvement.

The allocation profile confirms the intended structural change:

| Allocation class | Baseline pressure | Candidate pressure |
| --- | ---: | ---: |
| `HashMap$Node` | 6.31% | Below report cutoff |
| `Integer` | 4.23% | 0.80% |
| `Value[]` | 2.09% | Below report cutoff |
| `int[]` | 7.60% | 5.45% |

Normalizing JFR allocation-sample counts by completed operations gives an approximately 17% reduction. Allocation
sampling is statistical, so this is directional evidence rather than exact bytes per operation.

CPU hotspots remained effectively unchanged: `LmdbSailSink.flush` stayed near 32%, native LMDB transaction commit
near 26-27%, native cursor writes near 22-23%, and `Thread.yield0` near 7%. This is expected because the candidate does
not change native operation count. Larger steady-state TPS gains require a storage-layout or commit-path design that
preserves repeated-transaction duplicate, delete, query, and isolation semantics; the fresh-load packed segment cannot
be reused safely for this growing-store benchmark.

## Validation

Six focused `LmdbSailStoreTest` methods pass together, covering scalar batching, capacity growth/reset, add/remove
ordering, cross-sink transaction restart, reservation cleanup, and configuration enablement:

    Tests run: 6, Failures: 0, Errors: 0, Skipped: 0

The complete 40-method `LmdbSailStoreTest` class has one deterministic pre-existing failure in
`testExplainExecutedShowsIndexName`: the test expects a legacy `index=` explanation marker while the branch returns an
LMDB-native `NativeRows(...)` physical plan. The scalar scratch change does not touch query planning or explanation.

Artifacts are under `baseline/` and `candidate-scratch-reuse/`, including the JFR recordings, summaries, CPU-time hot
methods, allocation view for the candidate, and expanded CPU-time samples.
