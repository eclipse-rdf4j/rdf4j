# Rejected: operation-wide hash-sorted value resolution

This experiment replaced scalar value resolution in direct iterable ingestion with one operation-wide batch. Statement components were ordered by their unsigned 32-bit RDF value hash using primitive radix sort. Equal values therefore became contiguous; each hash run was collision-checked with exact `Value.equals`, and only distinct representatives were passed to `ValueStore.storeValues` before IDs were scattered back to aligned statement arrays.

The approach was correct in focused tests but decisively slower:

| Isolation | Retained direct-ingestion pooled mean | Hash-sorted batch | Regression |
| --- | ---: | ---: | ---: |
| NONE | 683.717 ms/op | 966.514 ms/op | 41.36% |
| READ_COMMITTED | 719.733 ms/op | 1037.307 ms/op | 44.12% |

The run used JDK 26, G1, `-Xms2G -Xmx2G`, `automaticEvaluationStrategy=false`, one fork, three one-second warmups, and three one-second measurements. Production and test changes were removed after the measurement.

The result disproves the assumption that eliminating random equality-map probes is sufficient. Large `ValueStore.storeValues` calls encode and sort a much larger live component set, add substantial primitive/reference scratch traffic, and lose the useful streaming overlap between value resolution and asynchronous triple writes. The existing scalar/transaction-cache path remains faster. Further work should reduce native write count rather than enlarge value batches.
