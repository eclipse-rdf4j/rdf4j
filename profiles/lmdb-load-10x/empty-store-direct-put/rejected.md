# Rejected empty-store direct-put experiment

LMDB documents that `mdb_put` with `MDB_NOOVERWRITE` returns `MDB_KEYEXIST` and points the data argument at the
existing item. The experiment used that contract to skip value-dictionary preflight gets only when a write transaction
started with an unused dictionary. It also deduplicated adjacent equal values inside a sorted batch. It never used
`MDB_APPEND` or `MDB_APPENDDUP`.

The implementation passed its focused red/green test and all 22 `ValueStoreTest` tests. It was then removed because
two exact paired JDK 26 benchmark runs did not show a repeatable improvement over the transaction-cache checkpoint.

| Isolation | Transaction-cache checkpoint (ms/op) | Direct-put run 1 | Direct-put run 2 | Direct-put pooled | Pooled change |
| --- | ---: | ---: | ---: | ---: | ---: |
| NONE | 747.595 | 726.580 | 766.491 | 746.536 | -0.14% |
| READ_COMMITTED | 770.796 | 762.972 | 792.778 | 777.875 | +0.92% |

The first run's apparent gain disappeared in the confirmation run, while allocation stayed essentially unchanged.
The production and test experiment is not present in the commit that preserves these results.

Primary LMDB API reference: <https://www.lmdb.tech/doc/group__mdb.html>.
