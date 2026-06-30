# Plan: Fix ID-only join to use ID-binding on RHS

## Description

- Problem: In the two-pattern ID join path, `LmdbIdJoinQueryEvaluationStep` materializes the left record back into a `MutableBindingSet` and uses the BindingSet-based iterator for the right-hand side, instead of staying in ID space.
- Evidence in code:
  - `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/join/LmdbIdJoinQueryEvaluationStep.java:74` creates the left iterator via `dataset.getRecordIterator(leftPattern, bindings)`.
  - `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/join/LmdbIdJoinQueryEvaluationStep.java:76-84` builds a `MutableBindingSet` from the left record and calls `dataset.getRecordIterator(rightPattern, bs)`.
  - Contrast with BGP path: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/join/LmdbIdBGPQueryEvaluationStep.java:70` uses the ID-binding overload: `dataset.getRecordIterator(rightPat, varName -> leftInfo.getId(leftRecord, varName))`.
- Impact: Allocates `Value` objects and performs value-store lookups on the right-hand side, negating expected performance gains and increasing GC pressure for ordinary binary joins.

## Reproduce & Test Plan

Goal: Prove the RHS iterator is created via the BindingSet overload today and enforce the change to the ID-binding overload with focused tests.

1) Unit test (overload call verification)
- Add `LmdbIdJoinRightUsesIdBindingTest` under `core/sail/lmdb/src/test/java/.../lmdb/`.
- Approach A (Mockito):
  - Spy/mock `LmdbEvaluationDataset` to record invocations of `getRecordIterator(StatementPattern, BindingSet)` vs. `getRecordIterator(StatementPattern, LmdbIdVarBinding)`.
  - Build a simple join `?s p1 ?x . ?s p2 ?y` with shared var `?s` and create `LmdbIdJoinQueryEvaluationStep` using a valid `LmdbQueryEvaluationContext`.
  - Evaluate with `EmptyBindingSet` and assert the RHS overload used is the ID-binding variant.
- Approach B (test double):
  - Implement a small `RecordingDataset` test double implementing `LmdbEvaluationDataset` that throws if `getRecordIterator(…, BindingSet)` is called for RHS and flips a flag if `getRecordIterator(…, LmdbIdVarBinding)` is called.
  - Assert the flag after evaluation.

Suggested targeted command during iteration:
`mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbIdJoinRightUsesIdBindingTest verify | tail -500`

2) Behavioral sanity (algorithm remains ID join)
- Reuse `LmdbIdJoinEvaluationTest.simpleJoinUsesIdIterator` pattern to assert the join algorithm name remains `LmdbIdJoinIterator` after the change.

3) Optional micro-observability (no perf assert)
- Wrap `ValueStore` in a counting decorator in a dedicated test to assert that the RHS path does not call `getValue(id)` or materialize `Value` objects during the join (optional and only if trivial to wire without touching production code).

## Fix Plan

- Change `LmdbIdJoinQueryEvaluationStep.evaluate` right-factory to use ID binding:
  - Replace the construction of a `MutableBindingSet` for RHS with an `LmdbIdVarBinding` lambda that provides IDs from the left record.
  - Model the code after the BGP path in `LmdbIdBGPQueryEvaluationStep`.
- Keep materialization to `BindingSet` in the final stage only (e.g., `LmdbIdFinalBindingSetIteration`), ensuring the pipeline stays in ID space end-to-end.
- Update/add tests from the Reproduce plan; ensure they fail before and pass after the change.

High-level pseudo-diff for clarity (not exact code):
```java
// Before (simplified)
MutableBindingSet bs = context.createBindingSet();
leftInfo.applyRecord(leftRecord, bs, valueStore);
return dataset.getRecordIterator(rightPattern, bs);

// After (RHS stays in ID space)
return dataset.getRecordIterator(rightPattern,
    varName -> leftInfo.getId(leftRecord, varName));
```

## Why This Needs To Be Fixed

- Performance: Avoids unnecessary `Value` creation and value-store ID resolution on the RHS, preserving the intended speedup of the ID-only join.
- Consistency: Aligns the 2-pattern join path with the BGP optimization, ensuring uniform behavior across join forms.
- Resource usage: Reduces GC pressure and heap churn for common joins.

## Validation / Success Criteria

- New unit test confirms the RHS path uses `getRecordIterator(…, LmdbIdVarBinding)`.
- Existing `LmdbIdJoinEvaluationTest` continues to pass and still reports `LmdbIdJoinIterator` as the chosen algorithm.
- No regressions in `core/sail/lmdb` module tests.

