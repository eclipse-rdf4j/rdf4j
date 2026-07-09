# LMDB loading baseline profile summary

Revision profiled: `66fba92e01 GH-0000 Capture LMDB loading baseline`.

Workload: `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction`, `automaticEvaluationStrategy=false`, default two indexes, 2 GiB G1 heap, JDK 26. No LMDB append flags were used.

## macOS async-profiler 4.4

The startup-command wrapper ran the existing JMH jar in-process with three warmup iterations and five three-second measurements. These scores are diagnostic only; the forked paired baseline remains the performance reference.

For `NONE`, the CPU profile recorded 9,832 samples. The native LWJGL LMDB library accounted for 32.85%, platform `memcmp` for 11.15%, condition waits for 8.18%, sink flush for 3.78%, radix sorting for 2.40%, `memmove` for 2.18%, and Varint encoding for about 1.96%. The measured score was 757.189 ms/op.

For `READ_COMMITTED`, the CPU profile recorded 9,655 samples. The native LMDB library accounted for 29.91%, platform `memcmp` for 11.21%, condition waits for 6.63%, sink flush for 3.88%, `memmove` for 2.21%, radix sorting for 1.63%, scalar `ValueStore.getId` for 1.38%, and Varint encoding for about 2.19%. The measured score was 778.369 ms/op.

The allocation profile for `NONE` was led by `byte[]` at 25.82%, `HeapCharBuffer` at 12.79%, `long[]` at 8.78%, `HeapByteBuffer` at 6.46%, `char[]` at 5.69%, boxed `Long` at 5.15%, `int[]` at 4.41%, `DirectByteBuffer` at 4.40%, and `String` at 3.89%.

The allocation profile for `READ_COMMITTED` was led by `byte[]` at 21.00%, `DirectByteBuffer` at 12.27%, `HeapCharBuffer` at 11.20%, `HeapByteBuffer` at 9.46%, boxed `Long` at 5.96%, `char[]` at 5.42%, `MDBVal` at 5.07%, `long[]` at 4.27%, `LinkedHashMap.Entry` at 3.79%, and `GenericStatement` at 2.84%.

Flat wall profiles are dominated by idle JVM service-thread waits and are not interpreted as benchmark-thread latency. Thread-split HTML flamegraphs are retained for call-shape inspection.

## Linux Java 26 Docker CPU-time JFR

The repository Docker JFR loop used ten ten-second measurements, no warmup, a 2 GiB G1 heap, 1,024-frame stacks, debug non-safepoints, and CPU-time sampling. The method regex was end-anchored so the six-index sibling benchmark was excluded.

`NONE` measured 690.737 ± 25.961 ms/op. Its 19,702 CPU-time samples ranked `mdb_cursor_put` at 18.97%, `mdb_put` at 17.51%, `mdb_get` at 15.93%, transaction commit at 4.27%, sink flush at 5.78%, and radix sorting at 3.62%. JFR reported 1,198 `CPUTimeSamplesLost` event records, so small rankings are low-confidence; the three LMDB wrappers jointly accounting for 52.41% is still decisive.

`READ_COMMITTED` measured 765.966 ± 105.106 ms/op. Its 18,730 CPU-time samples ranked `mdb_get` at 18.51%, `mdb_put` at 16.08%, `mdb_cursor_put` at 15.89%, sink flush at 4.88%, transaction commit at 3.74%, transaction startup work at 3.24%, scalar `ValueStore.getId` at 2.48%, `HashMap.putVal` at 2.24%, and radix sorting at 2.22%. JFR reported 1,509 lost-sample event records. The three LMDB wrappers jointly account for 50.48%.

Representative stacks show two dominant callers. `ValueStore.storeValues` performs `mdb_get` and paired `mdb_put` work through `BatchIdStorer`, including scalar namespace lookup from `uri2data`. `TripleStore.storeTriplesAligned` performs one `MDB_NOOVERWRITE` `mdb_put` into the main index and ordinary `mdb_cursor_put` calls into secondary indexes. No append mode appears in either stack.

## First optimization hypothesis

The first candidate should reduce calls into LMDB rather than tune Java syntax around them. The value cache holds only 128 entries and the namespace-ID cache only 32, while this workload spans far more repeated values. A transaction-owned, bounded primitive-friendly identity/value dictionary can retain resolved IDs for the whole bulk transaction, avoid cross-batch re-encoding and `mdb_get`, and serve both isolation paths. It must be measured against its memory footprint and must preserve rollback and revision semantics. Reusable batch collectors and native carriers are a secondary allocation optimization after the operation-count change is tested.

Confidence is high that native dictionary/index operations are the dominant CPU cost because macOS async-profiler and Linux CPU-time JFR agree. Confidence is medium on exact percentages because JFR lost samples are non-zero and macOS CPU profiling reports native work at library granularity.
