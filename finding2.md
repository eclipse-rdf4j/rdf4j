# Plan: Disable ID-only join when transaction changes are present

## Description

- Problem: The safeguard that disables the LMDB ID-only join in the presence of uncommitted writes relies on `LmdbEvaluationDataset.hasTransactionChanges()`. In production, the dataset installed by the store is `LmdbSailStore.LmdbSailDataset`, which does not override this method (default `false`), so the optimization remains enabled.
- Evidence in code:
  - Strategy gate: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStrategy.java:62-70` checks `ds.hasTransactionChanges()` to decide whether to disable the ID-only join.
  - Production dataset: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java:1363-1467` implements both `getRecordIterator` overloads but does not override `hasTransactionChanges()`.
  - Overlay dataset (tests only): `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOverlayEvaluationDataset.java:169-171` returns `true` for `hasTransactionChanges()`, but is only referenced from tests.
- Impact: With the optimization enabled, queries evaluated while a connection has uncommitted writes can read directly from the LMDB snapshot and ignore pending changes, leading to stale/missing results. At minimum, the current safeguard is dead code and gives a false sense of safety.

## Reproduce & Test Plan

Goal: Demonstrate the optimization stays enabled with pending changes in a write transaction, then prove it’s disabled and correctness is preserved after the fix.

1) Integration test (current behavior exposes the gap)
- Add `LmdbIdJoinTxnVisibilityTest` in `core/sail/lmdb/src/test/java/.../lmdb/`.
- Scenario:
  1. Create a store and repository; add baseline triple `:alice :knows :bob`.
  2. Open a connection, begin a transaction (if needed by the harness), and add uncommitted triple `:alice :likes :pizza`.
  3. Execute `SELECT ?person ?item WHERE { ?person :knows ?other . ?person :likes ?item . }` before commit.
  4. Capture the chosen algorithm via `explain` or by precompiling the algebra (as in `LmdbIdJoinEvaluationTest`) and assert it is currently `LmdbIdJoinIterator`.
  5. Assert that results are missing/stale (0 rows) if the store does not expose the overlay to the evaluation path. If the store flushes writes on read in this setup, adapt the test by using a `SailDatasetTripleSource` overlay (see 2).

2) Deterministic overlay test (mirrors existing test but without manual dataset injection)
- Build an overlay `TripleSource` that unions the baseline with the pending (uncommitted) triple on-the-fly (similar to `LmdbIdJoinDisableOnChangesTest`).
- Construct an evaluation strategy over the baseline triple source, but do NOT install `LmdbOverlayEvaluationDataset` into the thread-local.
- Precompile or evaluate the join and assert that the ID-only join is disabled by the strategy after the fix, purely by inspecting the active triple source (no thread-local overlay dataset needed).

Targeted command examples:
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbIdJoinTxnVisibilityTest verify | tail -500`
- `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbIdJoinDisableOnChangesTest verify | tail -500`

3) Unit-level indicator (dataset signal)
- Add a focused test to assert that the production dataset used during evaluation exposes pending writes appropriately after the fix (e.g., `((LmdbEvaluationDataset) dataset).hasTransactionChanges()` becomes `true` in the same connection during a write).

## Fix Plan

Two viable approaches (pick one; Option 2 preferred for correctness):

1) Quick, conservative fix (global writer visibility)
- Override `hasTransactionChanges()` in `LmdbSailStore.LmdbSailDataset` to return `true` when the store has an active write transaction (e.g., `storeTxnStarted.get()`).
- Pros: Minimal code, immediately makes the safeguard effective.
- Cons: Conservative: disables ID-only join for all queries while any writer is active, even in other connections.

2) Connection-local detection (preferred)
- Add a connection/thread-local flag in `LmdbSailStore` that is set in `LmdbSailSink.startTransaction(...)` and cleared on commit/rollback.
- Override `hasTransactionChanges()` in `LmdbSailStore.LmdbSailDataset` to consult this connection-local flag, so only queries in the same connection/transaction see `true`.
- Alternatively or additionally, update `LmdbEvaluationStrategy.precompile(...)` to detect that the active `TripleSource` is a `SailDatasetTripleSource` with overlays and install an `LmdbOverlayEvaluationDataset` as the effective dataset in the `QueryEvaluationContext` (which returns `true` for `hasTransactionChanges()`).
- Pros: Accurate semantics, disables ID-only only where necessary.
- Cons: Slightly more wiring; ensure no cross-thread leakage if evaluation occurs on different threads.

3) Safety net (minimal surface change in strategy)
- If wiring connection-local flags is not feasible, in `LmdbEvaluationStrategy.precompile(...)`, when `tripleSource` is not the LMDB-native source (e.g., it’s a `SailDatasetTripleSource`), wrap it with `new LmdbOverlayEvaluationDataset(tripleSource, valueStore)` as the `effectiveDataset`. This makes `hasTransactionChanges()` return `true` and will disable ID-only joins in `prepare(...)`.
- Pros: No store changes; straightforward.
- Cons: May disable ID-only joins even when no writes are pending; acceptable as a stopgap to ensure correctness.

## Why This Needs To Be Fixed

- Correctness: Queries executed within a write transaction must see uncommitted changes from their own transaction. Leaving the optimization enabled can yield stale or missing results.
- Transparency: The current safeguard is effectively dead code unless tests manually install an overlay dataset; production behavior should not depend on test-only wiring.
- Predictability: Aligns optimization behavior with RDF4J’s transactional semantics and user expectations.

## Validation / Success Criteria

- The new integration test demonstrates the issue pre-fix and passes post-fix: ID-only join is disabled when pending writes exist in the evaluating connection.
- Existing `LmdbIdJoinDisableOnChangesTest` continues to pass.
- No regressions in `core/sail/lmdb` tests; ID-only join remains enabled when no transaction changes are present.

