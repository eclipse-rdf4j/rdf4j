# Post-direct-ingestion profiling

Revision profiled: `c6e45c9842 GH-0000 Route iterable loads through LMDB bulk writes`.

Workload: exact `DatagovLoadIsolationBenchmark.loadDatagovFileSingleTransaction$`, `automaticEvaluationStrategy=false`, default two indexes, JDK 26, G1, and a 2 GiB heap. No LMDB append flags were used.

## macOS async-profiler 4.4

CPU JFR captures used two one-second warmups, four one-second measurements, one fork, and a 5 ms interval. The profiled scores were 682.572 ms/op for `NONE` and 719.670 ms/op for `READ_COMMITTED`.

Inclusive call-stack attribution over all samples:

| Path | NONE | READ_COMMITTED |
| --- | ---: | ---: |
| `TripleStore.storeTriplesAligned` | 29.56% | 28.48% |
| `ValueStore` | 21.49% | 19.93% |
| `LmdbSailSink.approveAllBulk` | 21.49% | 20.34% |
| `Changeset.approveAll` | 0.00% | 2.30% |
| native LMDB library | 36.20% | 34.78% |
| `ObjectLongHashMap` | 4.90% | 4.21% |
| `String.equals` | 2.26% | 2.26% |
| leading-field radix sort | 1.58% | 1.38% |
| sink flush | 6.73% | 6.12% |

The profiler starts before JMH trial setup, so Turtle parsing contributes 7.30%/6.97% of total samples even though parsing is outside the measured benchmark operation. That setup cost does not alter the ordering of ingestion hotspots. Raw JFR and collapsed stacks are retained under `async-none` and `async-read-committed`.

## Linux Java 26 CPU-time JFR

The Docker loop used no warmup, ten ten-second measurements, one fork, 1,024-frame stacks, and debug non-safepoints.

| Isolation | Score | CPU-time samples | Lost-sample events |
| --- | ---: | ---: | ---: |
| NONE | 584.626 ± 13.624 ms/op | 18,694 | 1,347 |
| READ_COMMITTED | 609.773 ± 13.904 ms/op | 18,171 | 1,165 |

Top self CPU methods:

| Method | NONE | READ_COMMITTED |
| --- | ---: | ---: |
| `nmdb_cursor_put` | 27.67% | 27.90% |
| `nmdb_put` | 8.01% | 7.93% |
| `nmdb_get` | 7.03% | 7.09% |
| sink flush | 7.69% | 8.00% |
| `nmdb_txn_commit` | 5.08% | 5.06% |
| `String.equals` | 4.04% | 3.73% |
| primitive-map equality | 2.27% | 2.03% |
| `ValueStore.getId` | 2.18% | 1.82% |
| leading-field radix sort | 2.67% | 2.73% |

The profiles show that the iterable conduit removed the old scalar Sail dispatch but did not change the two structural costs: equality-based value resolution and one native cursor put per statement per secondary index. READ_COMMITTED changeset materialization is now too small to be the next target.

## Next experiment

Resolve each adaptive operation as one hash-sorted component batch. Equal RDF values have equal public hashes, so sorting primitive component positions by the 32-bit hash makes equal terms contiguous. Each hash run can be collision-checked against a tiny representative list, preserving equality semantics without a random-access map probe for every statement component. Only distinct representatives then cross `ValueStore.storeValues`; resolved IDs are scattered back into the aligned primitive arrays. This leaves the on-disk format unchanged and directly targets the combined 8–10% Java equality/self cost plus repeated value-store work.

If that experiment is retained, the remaining dominant native cursor-put cost still requires the planned immutable packed-index segments; ordinary per-statement B-tree writes cannot reach the 78–83 ms acceptance thresholds.
