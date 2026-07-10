# Compact bulk changeset checkpoint

Large unique bulk approvals now enter `Changeset` as an owned insertion-order array instead of immediately building a hash-backed `Model`. Reads can scan the immutable compact sequence. A later scalar addition, removal, or context-filtered clear materializes the normal model exactly once. The straight `READ_COMMITTED` commit path forwards the compact `Collection` directly to LMDB, where it remains eligible for fresh packed storage.

The retained result in `candidate.json` used JDK 26, G1, `-Xms2G -Xmx2G`, five one-second warmups, five one-second measurements, one fork, the exact end-anchored benchmark, and `automaticEvaluationStrategy=false`.

| Isolation | Previous ms/op | Compact ms/op | Change | Previous B/op | Compact B/op |
| --- | ---: | ---: | ---: | ---: | ---: |
| NONE | 102.174 | 102.831 | +0.64% | 94,965,668 | 94,965,620 |
| READ_COMMITTED | 157.360 | 144.034 | -8.47% | 119,524,052 | 97,436,683 |

`NONE` is statistically unchanged. `READ_COMMITTED` improves by 13.326 ms/op and removes 22.09 MB/op. Its five measurements span only 141.448-145.565 ms/op. The focused test was first captured red because the prior path allocated one model, then green after the compact representation; all 19 sail-base tests and three neighboring LMDB packed/materialization tests pass.

## Post-checkpoint profiles

The macOS JDK 26 async-profiler CPU and allocation reports are under `async-rc-cpu` and `async-rc-alloc`. `Changeset.approveCompactWithoutMaterialization` is only 0.63% of CPU samples, and `LinkedHashMap.Entry` falls from 12.07% before this checkpoint to 0.62% of sampled allocation. The remaining Java hotspot is the packed local value dictionary: `packedValueId` is 9.30%, `SimpleIRI.equals` 4.28%, `String.equals` 3.83%, and `HashMap` lookup/hash another 1.47% in the macOS flat profile.

The Linux Java 26 CPU-time recording is `docker-read-committed.jfr`. It contains 9,909 CPU-time samples and 123 lost samples (1.24%). Its top ingestion methods are `packedValueId` 11.11%, `HashMap.getNode` 8.82%, `writePackedValues` 8.05%, `String.equals` 6.03%, and `HashMap.hash` 3.31%. `Changeset.approveCompactWithoutMaterialization` is 0.44%. `LinkedHashMap$LinkedHashIterator.nextNode` remains 18.10% because the repository must still consume the caller's original model once; the hash-backed Changeset copy itself is gone.
