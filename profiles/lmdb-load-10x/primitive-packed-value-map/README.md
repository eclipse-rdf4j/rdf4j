# Primitive packed-value map checkpoint

The transaction-local packed value dictionary now uses the already-present Eclipse Collections `ObjectIntHashMap<Value>` instead of `HashMap<Value, Integer>`. It preserves RDF `equals`/`hashCode` collision semantics and insertion-order IDs while removing boxed integers, node objects, and chained pointer traversal. No dependency, file format, LMDB flag, or public API changes.

`candidate.json` is an exact paired JDK 26/G1/2 GiB run with five one-second warmups, five one-second measurements, one fork, `automaticEvaluationStrategy=false`, and the end-anchored benchmark method.

| Isolation | Previous ms/op | Primitive ms/op | Change | Previous B/op | Primitive B/op |
| --- | ---: | ---: | ---: | ---: | ---: |
| NONE | 102.831 | 92.354 | -10.19% | 94,965,620 | 92,631,909 |
| READ_COMMITTED | 144.034 | 145.334 | +0.90% | 97,436,683 | 95,102,946 |

`NONE` measurements span 89.284-96.037 ms/op and confirm a material shared-dictionary gain. The `READ_COMMITTED` distributions overlap and are treated as neutral. Both modes remove about 2.33 MB/op. All nine packed load, reopen, rollback, materialization, evaluator-scope, disabled-path, and value-type tests pass.
