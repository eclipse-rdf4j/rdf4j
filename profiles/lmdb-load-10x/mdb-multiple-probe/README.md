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
