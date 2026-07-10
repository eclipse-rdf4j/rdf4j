# Direct LMDB iterable ingestion

`LmdbSailSink` now specializes the generic repository-to-Sail iterable conduit. With bulk operations enabled it resolves and submits adaptive aligned operations directly, rather than routing every statement through scalar `approve`. Context overrides, estimator callbacks, duplicate handling, operation reservations, opt-out behavior, and rollback paths remain on the existing bulk implementation.

Both short paired runs used JDK 26, G1, `-Xms2G -Xmx2G`, `automaticEvaluationStrategy=false`, one fork, three one-second warmups, and three one-second measurements.

| Run | NONE (ms/op) | READ_COMMITTED (ms/op) |
| --- | ---: | ---: |
| 1 | 667.403 | 709.363 |
| 2 | 700.031 | 730.102 |
| Pooled mean | 683.717 | 719.733 |

Relative to the original durable baseline of 780.972/831.080 ms/op, pooled time improves 12.45% for `NONE` and 13.40% for `READ_COMMITTED`. This is a retained architectural increment, not the 10x result.

## TDD evidence

Before production changes, the focused test observed all five iterable elements falling through `SailSink.approveAll` into scalar `LmdbSailSink.approve`:

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSailStoreTest#iterableApproveAllUsesLmdbBulkPathDirectly --retain-logs
    Report: core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbSailStoreTest.txt
    Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    Never wanted but invoked: LmdbSailSink.approve (five calls)

After specialization, the focused test and four neighboring bulk, opt-out, adaptive-capacity, and add/remove-ordering tests pass:

    Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
    Log: logs/mvnf/20260710-004646-verify.log
