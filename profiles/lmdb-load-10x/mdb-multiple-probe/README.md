# LMDB `MDB_MULTIPLE` write probe

Date: 2026-07-10

This scratch microbenchmark tests whether changing the triple-index representation can materially reduce native LMDB write overhead without using `MDB_APPEND` or `MDB_APPENDDUP`. It performs 613,157 statement writes into two indexes in one transaction using the same JDK 26 and 2 GiB map-size assumptions as the loading benchmark.

The ordinary case writes one 32-byte key at a time with `mdb_cursor_put(..., 0)` into two ordinary databases. The structural case opens both databases with `MDB_DUPSORT | MDB_DUPFIXED`, groups records by the leading index field, encodes each remaining tuple as a fixed 24-byte duplicate value, and submits each group with `mdb_cursor_put(..., MDB_MULTIPLE)`. The main-index probe uses 66,452 groups and the secondary-index probe uses 55 groups. Neither case uses an append flag.

The probe was compiled and run against `core/sail/lmdb/target/jmh-benchmarks.jar` with Zulu OpenJDK 26 on macOS arm64:

```text
round 1 ordinary: 693.926 ms
round 1 multiple: 221.165 ms
round 2 ordinary: 673.447 ms
round 2 multiple: 209.926 ms
round 3 ordinary: 736.935 ms
round 3 multiple: 210.526 ms

ordinary mean: 701.436 ms
multiple mean: 213.872 ms
speedup: 3.280x
```

This is a diagnostic lower-level result, not an end-to-end benchmark claim. It excludes RDF value resolution, repository lifecycle, and query/read compatibility costs. It does establish that reducing approximately 1.23 million ordinary native cursor calls to grouped fixed-width multi-value puts has much greater upside than further Java hot-loop tuning. The next probe varies the grouping granularity to distinguish LMDB per-call overhead from B-tree insertion and comparison cost.

The LMDB API contract used by the probe is documented at <https://www.lmdb.tech/doc/group__mdb.html>: `MDB_MULTIPLE` is valid for `MDB_DUPFIXED` databases and accepts an array of two `MDB_val` structures describing one fixed-size item and the item count.

## Bucket-shape and four-index sweep

A follow-up sweep held the record count constant while varying the duplicate bucket count. For two indexes, balanced 16,384/16,384 buckets were the best tested shape:

```text
groups per index       mean of three rounds
2,048                  157.341 ms
4,096                  101.346 ms
8,192                   79.451 ms
16,384                  72.859 ms
32,768                  85.323 ms
65,536                 114.520 ms
```

The U-shaped result shows that the optimum is near a page-sized duplicate run rather than at the smallest number of API calls. Very large duplicate sets make LMDB's internal duplicate-tree work more expensive.

Repeating the synthetic probe with all four indexes used by `ConfigUtil.createConfig()` produced a 1,361.895 ms ordinary-write mean and a 139.411 ms mean at 16,384 duplicate buckets per index, a 9.77x low-level gain. Adding `MDB_WRITEMAP` did not materially improve the bulk layout and made ordinary writes slower, so it was rejected.

## Packed immutable-block probe

The next structural probe stored each sorted bucket as one ordinary LMDB value containing fixed-width records. This eliminates per-record B-tree insertion: LMDB sees one put per immutable block, while the block remains binary-searchable by the read path. All record encoding and four-index writes were inside the timed region. The three-round means were:

```text
blocks per index       four-index mean
1,024                   43.446 ms
2,048                   39.476 ms
4,096                   43.216 ms
8,192                   42.295 ms
16,384                  53.005 ms
32,768                  70.594 ms
65,536                 105.284 ms
```

The best pooled result is 39.476 ms for all four indexes, with the last two 1,024-block rounds at 31.337 and 31.909 ms after the first-round cold cost. This remains a structural feasibility result rather than an end-to-end claim. A production design would need immutable transaction segments, duplicate suppression, deletion tombstones or compaction, query-side block search, and an on-disk version transition. Its absolute native-write floor is low enough to keep pursuing against the 78-83 ms acceptance target; the fixed-duplicate design's 139 ms floor is not.
