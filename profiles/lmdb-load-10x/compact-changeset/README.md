# Compact bulk changeset checkpoint

Large unique bulk approvals now enter `Changeset` as an owned insertion-order array instead of immediately building a hash-backed `Model`. Reads can scan the immutable compact sequence. A later scalar addition, removal, or context-filtered clear materializes the normal model exactly once. The straight `READ_COMMITTED` commit path forwards the compact `Collection` directly to LMDB, where it remains eligible for fresh packed storage.

The retained result in `candidate.json` used JDK 26, G1, `-Xms2G -Xmx2G`, five one-second warmups, five one-second measurements, one fork, the exact end-anchored benchmark, and `automaticEvaluationStrategy=false`.

| Isolation | Previous ms/op | Compact ms/op | Change | Previous B/op | Compact B/op |
| --- | ---: | ---: | ---: | ---: | ---: |
| NONE | 102.174 | 102.831 | +0.64% | 94,965,668 | 94,965,620 |
| READ_COMMITTED | 157.360 | 144.034 | -8.47% | 119,524,052 | 97,436,683 |

`NONE` is statistically unchanged. `READ_COMMITTED` improves by 13.326 ms/op and removes 22.09 MB/op. Its five measurements span only 141.448-145.565 ms/op. The focused test was first captured red because the prior path allocated one model, then green after the compact representation; all 19 sail-base tests and three neighboring LMDB packed/materialization tests pass.
