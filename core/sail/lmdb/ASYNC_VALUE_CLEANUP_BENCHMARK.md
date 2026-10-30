# Measuring asynchronous dictionary cleanup

`AsyncValueCleanupBenchmark` compares the default durable policy and the explicitly enabled asynchronous policy using identical synthetic RDF, indexes, sync settings, and transaction operations. It records commit latency separately from mutation and transaction latency, because moving reclamation into another part of the application call would not be a useful improvement.

The default fixture has 100,000 triples. Every subject and plain string object is unique, one predicate is shared, and every 1,000 rows share a named graph. The four indexes are `spoc,posc,ospc,cspo`; the context-leading index makes partition deletion a bounded indexed operation. The initial value and triple map sizes are 256 MiB, auto-growth is enabled, and the optional sketch estimator and direct adjacency index are disabled in both modes. These choices isolate dictionary retirement rather than query optimization. The fixture is a reproducible synthetic workload, not a reconstruction of an externally supplied dataset.

Measured writes use the raw `SailConnection` transaction API. Validation and pinned readers use managed `RepositoryConnection`/`RepositoryResult` scopes, and no returned RDF value escapes a closed result. Raw Sail result iteration can promote values into conservative legacy value protection even after its iterator closes; using it for setup validation would unintentionally force later cleanup into durable deferral. The managed reader path avoids that measurement contamination while preserving a real snapshot across each pinned-reader cycle. The harness also checks that no legacy value-protection epoch remains after validation; a violation fails the scenario instead of silently measuring that accidental retention.

## Run

Use Java 25 or later and Maven with the repository's dependencies installed. From the repository root:

```bash
scripts/run-single-benchmark.sh \
  --module core/sail/lmdb \
  --main-class org.eclipse.rdf4j.sail.lmdb.AsyncValueCleanupBenchmark \
  --jvm-arg -Xms2g \
  --jvm-arg -Xmx2g \
  --main-arg --output=/tmp/rdf4j-cleanup-comparison-1 \
  --main-arg --triples=100000 \
  --main-arg --cycles=10 \
  --main-arg --warmup-cycles=2 \
  --main-arg --force-sync=false
```

The output directory must not exist. Each invocation retains both the CSVs and all generated stores. Repeated runs must use new output directories. Run several fresh JVM invocations, and repeat with `--force-sync=true` to measure the fsync configuration separately. The default matches `LmdbStoreConfig`'s existing `forceSync=false`; it is recorded in metadata rather than silently conflated with forced-sync results.

The supported runner packages the benchmark source and its dependencies. After building once, add `--no-build` for repeated measurements. Add `--enable-jfr` and `--jfr-output /tmp/rdf4j-cleanup-comparison-1.jfr` for an additional profiling run. Profiled and unprofiled results should be reported separately.

Useful options are:

| Option | Default | Meaning |
| --- | ---: | --- |
| `--triples` | `100000` | Number of initially loaded triples. |
| `--partition-size` | `1000` | Rows per context and per pressure transaction. |
| `--cycles` | `10` | Measured delete/reinsert cycles. |
| `--warmup-cycles` | `2` | Unmeasured warmup cycles, still present in raw CSVs. |
| `--mode` | `both` | `durable`, `async`, or `both`. |
| `--scenario` | `all` | `burst`, `pressure`, `pinned`, or `all`. |
| `--force-sync` | `false` | Same LMDB forced-sync setting in both modes. |
| `--admission-timeout-ms` | `30000` | Async producer admission timeout before publication. |
| `--drain-timeout-seconds` | `60` | Timeout at explicit benchmark queue-drain barriers. |

All options use the form `--name=value`. A short fixture, for example `--triples=1000 --cycles=1 --warmup-cycles=0`, checks that the harness and output format work; it is not a performance result for the 100,000-triple workload.

## Scenarios

**Burst** removes the entire store in one transaction and reinserts the same RDF in the next transaction. With the default 100,000 triples, the candidate stream is larger than the default in-memory cleanup capacity. This exercises bounded transaction spilling and reuse of values whose previous retirement may still be queued. There are no queue-drain barriers between measured commits.

**Pressure** removes and reinserts one named-graph partition at a time. It uses a configured limit of 2,048 candidate workspace slots, 256 KiB estimated retained bytes, cleanup batches of at most 128 IDs, and four pending producers. The coordinator may derive a smaller effective batch size from its complete workspace accounting. The loop contains no per-commit queries or explicit drain barriers, so the worker has to compete with sustained writes. With default options it yields 2,000 measured commit samples per mode.

**Pinned** opens a snapshot reader before each complete-store delete/reinsert cycle. That reader stays open across both commits, and every original statement is validated before it closes. An explicit barrier then allows the asynchronous worker to finish its admitted candidate streams before the next reader pins another snapshot. Barrier duration is recorded separately and included in overall scenario throughput; it is excluded from the commit samples. This models cleanup delayed by a reader without treating eventual cleanup as free. The barrier only establishes that admitted in-memory/spilled streams are empty; it does not claim that all legacy, recursive, or protected-value durable continuations have been physically reclaimed.

Warmup uses the same transaction operations. The measured phase starts after warmup candidate streams finish and the current RDF is validated. Initial loading, that warmup barrier, and final shutdown are outside the measured phase. In a combined run, mode order alternates between scenarios to reduce a consistent first-mode advantage; independent repeated JVM runs remain necessary for stable comparisons.

## Files and interpretation

`commit-samples.csv` contains one row per transaction, including warmup. Its timings separate `begin`, mutation, `commit`, and the complete transaction. The commit interval runs until the public `SailConnection.commit()` returns, including any post-commit work. A failed transaction is recorded explicitly, with `committed=false` and no invented successful commit duration. Any scenario failure makes the process exit unsuccessfully after preserving available output.

`summary.csv` reports nearest-rank p50, p95, p99, and maximum latency for successful commits and complete transactions. Rows are provided for all mutations, deletion, and reinsertion separately. Each row includes the sample count, failed transaction count, Java allocation, overall measured duration, throughput, queue-drain duration, shutdown duration, and RDF validation result. A 20-sample burst p99 is effectively the observed maximum; it does not establish a reliable production tail-latency distribution. Increase cycles and repeat runs before drawing tail-latency conclusions.

Throughput is successful row mutations divided by total measured scenario wall time. The denominator includes CSV sampling overhead and, in the pinned scenario, reader validation and queue-drain barriers. The per-operation summary rows use the same complete scenario denominator; they partition total mutation throughput rather than assigning all wall time to one operation.

`cleanup-metrics.csv` samples cumulative coordinator counters and instantaneous bounded-memory observations after each transaction, at measurement start and end, after pinned-reader release, and around shutdown. `reserved_candidate_slots` and `reserved_bytes_estimate` include conservative worker and producer workspace reservations. They are not a count or byte size of every candidate in temporary spill files. `queued_streams` counts admitted FIFO streams, each of which can contain multiple bounded chunks. `unacknowledged_candidate_ids` counts actual pending IDs across queued streams and pending producers, including candidates in temporary spill files; it exposes cleanup backlog growth separately from bounded retained memory. `waiting_producers` records producers waiting for admission. Compare counters at the `measure/start` and `measure/end` rows to distinguish measured work from initial loading, warmup, final content validation, and shutdown. High-water counters are maintained by the coordinator and can capture peaks between sampling points; they cover the entire scenario, including warmup.

The metrics also separate application writer-owner contention wait, cleanup writer acquisition elapsed time, and successful cleanup maintenance commits. The worker acquisition counter includes acquisition overhead and must not be interpreted as pure blocked time. JFR's `jdk.ThreadPark` and `jdk.JavaMonitorEnter` events provide complementary stack-level contention evidence.

`cleanup-metrics.csv` also records exact native `mdb_del`/`mdb_cursor_del` attempts and successful returns in `ValueStore` and `RetiredValueIdStore`. The benchmark enables these optional counters in both modes. Their scope includes metadata removal, free-ID consumption, reference-count updates, and calls made in subsequently aborted transactions. They exclude `TripleStore` statement-index deletion. A successful native deletion is not necessarily a committed deletion; compare these counters with maintenance outcomes and failure counts.

`native-stats.csv` records native database entry and page counts before and after measurement. Entry-count changes are not native delete-call counts: retirement can update several indexes, deletion and reinsertion can cancel in a net count, and some cleanup is deferred. They complement the explicit call counters rather than replacing them.

`metadata.properties` captures Java/OS information, available processors, heap limit, exact options, and measurement definitions. Set the JVM property `rdf4j.benchmark.revision` through the runner's `--jvm-arg` option to include the tested Git revision.

Allocation comes from `com.sun.management.ThreadMXBean`: foreground-thread bytes and JVM-wide tracked Java allocation during each transaction are separate fields. The summary additionally records JVM allocation over the complete measured phase, the complete scenario, and shutdown. This exposes work performed between application commits and after a reader releases its snapshot, which would be missed by adding the individual transaction counters alone. Whole-scenario allocation includes initial loading, warmup, validation, and shutdown; the phase counter includes only the measured phase. These complete-scenario/phase totals are repeated on the per-operation summary rows and must not be summed across those rows.

A value of `-1` means unavailable. These counters exclude native allocation and fixture construction. The complete fixture RDF values are prepared before measurements, so the comparison measures store-operation allocation rather than RDF input generation. Heap retained by the queue is separately represented by its conservative reservation counters; allocated bytes and retained bytes measure different things.

## Correctness and failure interpretation

The harness checks every final statement using its subject ordinal, expected predicate, exact object, expected named graph, and a duplicate-detection bit set. Snapshot readers undergo the same full-content check before release. A matching cardinality alone is not considered sufficient.

Pressure can legitimately reach the admission timeout when the worker cannot retire admitted streams quickly enough. Report that result, its failed transaction count, and the latency distribution together; do not discard the failures and claim a throughput win. Likewise, a queue-drain timeout is a reported failed scenario rather than a forced synchronous reclamation pass.

Shutdown is measured separately through `SailRepository.shutDown()`, which delegates to the underlying store after the benchmark connections close. If the store reports that its worker has not quiesced, the benchmark stops measuring other scenarios and does not close native environments itself. It preserves the store's ownership rule and reports the condition; the harness never force-closes LMDB to manufacture a shutdown-time result.

## Recorded comparison: 2026-10-30

The verified build completed 6,080 measured commits on the synthetic 100,000-triple fixture, with no failed transactions,
scenario errors, admission timeouts, failed worker batches, RDF-content differences, or reservation-bound violations.
The all-scenario run used ten measured and two warmup cycles with `forceSync=false`; a second JVM ran five measured and
one warmup pressure cycle with `forceSync=true`. Both used Java 25.0.5+8, a fixed 2 GiB heap, and the same production code.

| forceSync | Scenario | Samples per mode | Commit p99, durable → async (ms) | Transaction p99, durable → async (ms) | Mutations/s, durable → async |
| --- | --- | ---: | ---: | ---: | ---: |
| false | Burst | 20 | 500.055 → 340.816 | 500.779 → 3623.903 | 272997 → 147050 |
| false | Pressure | 2000 | 27.061 → 22.978 | 28.553 → 91.690 | 70206 → 29097 |
| false | Pinned reader | 20 | 589.414 → 291.647 | 589.585 → 299.346 | 145449 → 58063 |
| true | Pressure | 1000 | 39.166 → 19.722 | 43.794 → 149.955 | 35919 → 20049 |

These results show a qualified commit-time benefit, not an overall transaction-latency or throughput improvement.
Default-sync pressure reinsertion commit p99 regressed from 17.324 to 28.391 ms even though the combined-operation tail
improved. Pressure admission waiting totaled 55.981 seconds without forced sync and 42.189 seconds with forced sync.
The default-sync pressure worker performed 36,210 maintenance commits for 4,000,000 spilled candidate entries, including
duplicates. Explicit pinned-reader drain barriers consumed 24.959 seconds and are included in throughput.

Peak reserved cleanup memory was 5,632 slots / 272,384 estimated bytes in async burst, and 2,034 slots / 118,240 bytes in
both pressure runs. These are workspace estimates, not process RSS. Default-sync pressure JVM measured-phase allocation
fell from 14,741 MiB to 2,170 MiB; pinned measured-phase allocation increased. Normal shutdown ranged from 3.182 to
32.991 ms across the eight cases. This does not bound an arbitrary native call. Lower native delete counts do not imply
equal completed reclamation: work can remain queued, spooled, durably deferred, or unnecessary after reinsertion.

Production-source verification ran 577 distinct cases across 32 selected classes: 576 passed and one existing test was
`@Disabled`. One loopback-socket fixture initially encountered a sandbox permission error and passed unchanged when
rerun with the required permission. The benchmark assembly matched all 2,772 compiled LMDB/Sail API production classes,
including inner classes, without missing, mismatched, or duplicate class entries.

The benchmark source fingerprint was `eb2159de7fca4e1dc0a1d7d797743f5fbf7bfbbfb91d1622733553ad17711791`; the benchmark
JAR SHA256 was `92606eb1dd6c7737f94f2c25f5f927c886daf7e8ae8c4aa00976896c756489a8`. These are single-invocation comparisons
on a shared Linux overlay filesystem host, not stable production tail estimates. In the 20-sample scenarios p99 is the
observed maximum.

### Unresolved disposable-stream observation

Some benchmark stores retained unreferenced UUID `.ids` files after acknowledged processing. One fresh 100,000-triple
DEBUG diagnostic reproduced a 3.2 MB leftover without logging a deletion exception. A separate unchanged-build trace
observed successful `unlink` and no residue; another wrapper observed no residue inside the JVM, immediately after
exit, or in a later filesystem read. Those controls did not establish a production ownership defect or filesystem
reappearance, and the precise cause remains unresolved.

These unreferenced files are cleanup hints and never authorize dictionary deletion. Coordinator startup retries removal
of recognized orphan streams. Temporary-file disposal is best effort; a persistent removal problem can retain disk
usage even when candidate memory and native processing have drained.
