# Rejected: canonical terms in map-backed DynamicModel

Date: 2026-07-10

This Routine A experiment made equal RDF terms share one object identity while retaining `DynamicModel`'s insertion-order statement map. The focused test failed before the change and passed afterward; all 27 `DynamicModelTest` tests then passed.

The exact paired JDK 26, G1, 2 GiB benchmark with `automaticEvaluationStrategy=false` measured:

```text
NONE             771.802 ms/op   237,200,401.6 B/op
READ_COMMITTED   912.047 ms/op   289,339,206.4 B/op
```

Relative to the retained main-index-cursor checkpoint (705.864/712.789 ms/op), this is 9.34% slower for `NONE` and 27.95% slower for `READ_COMMITTED`. Allocation is also flat to slightly worse than the transaction-cache checkpoint. Canonical input identities do not remove the LMDB sink and value-store equality dictionaries; the extra model dictionary therefore adds construction and retained-memory cost without eliminating the hot work.

The production and test changes were removed. `run-1-jdk26.json` is retained as negative evidence.
