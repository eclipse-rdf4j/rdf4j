# Sail bulk-ingestion conduit

This checkpoint carries one `Iterable<? extends Statement>` from the repository API through the Sail connection and sink layers. The generic implementation preserves statement/context semantics, listener behavior, update ordering, metrics, and duplicate handling. `Changeset` holds its write lock for the complete iterable instead of acquiring it once per statement. The default methods retain scalar behavior for stores that do not specialize the conduit.

The LMDB benchmark uses `automaticEvaluationStrategy=false`, JDK 26, G1, `-Xms2G -Xmx2G`, one fork, three one-second warmups, and three one-second measurements. These short runs establish that the generic conduit is neutral for `NONE` and reduces current-host `READ_COMMITTED` time by roughly 30 ms when the one-lock `Changeset` path is used. Host drift makes these diagnostic rather than acceptance runs.

| Run | NONE (ms/op) | READ_COMMITTED (ms/op) |
| --- | ---: | ---: |
| Conduit before one-lock Changeset | 756.883 | 772.575 |
| Conduit with one-lock Changeset | 772.180 (755.678 minimum) | 742.816 |

## TDD evidence

The repository-level test was introduced before production changes and initially failed because `add(Iterable)` never invoked a bulk Sail method:

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py --module core/repository/sail SailRepositoryConnectionTest#iterableAddUsesSingleSailBulkInvocation --retain-logs
    Report: core/repository/sail/target/surefire-reports/org.eclipse.rdf4j.repository.sail.SailRepositoryConnectionTest.txt
    Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
    expected: 1
     but was: 0

After the conduit was implemented, all ten `SailRepositoryConnectionTest` tests and all eight `ChangesetTest` tests passed. `ChangesetTest` includes coverage for set/duplicate behavior and context replacement in a bulk approval.

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py --module core/repository/sail SailRepositoryConnectionTest --retain-logs
    Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

    Command: python3 .codex/skills/mvnf/scripts/mvnf.py ChangesetTest --retain-logs
    Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

The conduit is an architectural checkpoint, not a claim of a 10x result. Its purpose is to let LMDB specialize the bulk operation and carry a compact numeric/packed representation without reconstructing per-statement state at every layer.
