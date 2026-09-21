# Restore binding-aware nested join order for LMDB aggregate kernels

This ExecPlan is a living document and follows `.agent/PLANS.md`. The goal is to remove a severe LMDB native aggregate regression where a nested OPTIONAL join starts from an unbound high-cardinality pattern even though the enclosing pipeline has already bound its join key. The user-visible result is that Medical Records q8, and equivalent nested OPTIONAL/EXISTS/UNION shapes, retain their correct results while returning to a bounded correlated probe workload.

## Purpose / Big Picture

Medical Records q8 has 8,335 patients and about 24,900 encounters. At the affected revision the native kernel lowers the OPTIONAL arm as `handledBy(enc, practitioner)` followed by `hasEncounter(patient, enc)`. The first pattern has no bound endpoint at that depth, so the generated kernel repeatedly enumerates the full encounter domain for every patient. The same structural mistake can affect any nested inner join whose first child needs an endpoint produced by an enclosing producer. The fix must choose a child using the builder's actual assured bindings at the lowering boundary, preserve filter scope and encounter-order barriers, and leave nullable OPTIONAL/UNION bindings conservative.

Acceptance is a focused regression test that fails before the production change because the generated nested arm begins with the unbound producer, then passes with the correlated producer first. The Medical Records q6–q9 correctness set must remain green, and a warmed q8 benchmark on the protected complete store must show the domain-driven counter returning to zero or the historical correlated scale rather than 208M.

## Progress

- [x] (2026-09-21) Captured current branch, clean status, and HEAD `67e299904e`.
- [x] (2026-09-21) Cloned the 6.4G benchmark store before builds under `/tmp/rdf4j-theme-medical-baseline`.
- [x] (2026-09-21) Measured warmed q6/q7/q8: 8.400/1.722/1990.062 ms/op; q8 has 208,141,620 domain-driven rows and zero actual roots.
- [x] (2026-09-21) Confirmed historical q8 order and 9.578 ms/op in `results-2026-09-19.md`; forced serial retains the bad order.
- [x] Add and observe smallest failing binding-order test.
- [x] Implement costed lowering order with binding mask.
- [x] Run focused and LMDB module verification gates.
- [x] Re-run warmed q6–q9 and forced strategies.
- [in_progress] Review, commit, and push `GH-0000`.

## Surprises & Discoveries

- Current warmed q8 selects `irAggregateParallel` but reports `nativeIrParallelRootsVisitedActual=0`, `nativeIrParallelNeighborValuesCopiedActual=0`, `domainDrivenRows=208141620`, and `nativeIrParallelWorkerOverlapNanosActual=251787.9M`.
- The current native plan's nested arm is `handledBy` then `hasEncounter`; the 2026-09-19 artifact has `hasEncounter` then `handledBy` and 24.9K roots/neighbors.
- Forcing the pre-fix `irAggregate` path retained the reversed order and measured about 4.2 seconds, proving the primary defect was plan order rather than only parallel worker contention.
- The original complete store was never run or rebuilt directly; all benchmark runs use its CoW clone.

## Decision Log

- Decision: Fix the general nested lowering boundary instead of adding a q8-specific query or benchmark condition. Rationale: the failure is caused by a reusable inner-join shape and can affect OPTIONAL, EXISTS, UNION, and nested composition. Date/Author: 2026-09-21, Codex.
- Decision: Use only bindings assured on every row at the current builder depth to select a correlated child. Rationale: nullable or branch-local bindings must not be treated as available; unsafe or encounter-order-sensitive children retain the existing lowering/fallback. Date/Author: 2026-09-21, Codex.
- Decision: Accept only strict full-chain work improvements. Rationale: local adjacent swaps use the existing interval model, preserve filter depth, avoid rank heuristics, and keep planning polynomial. Date/Author: 2026-09-21, Codex.
- Decision: Preserve the existing `MultiJoinPlan` compiler order when no child is safely ready. Rationale: correctness and observable order take precedence over an unproven reorder. Date/Author: 2026-09-21, Codex.

## Outcomes & Retrospective

The focused reproduction failed before the production edit with the unbound encounter enumeration first; it passes in `logs/mvnf/20260921-212018-verify.log`, and the expanded lowering class is 126/126 green in `logs/mvnf/20260921-215238-verify.log`. The LMDB gate is 6,215/6,215 green with 119 skipped in `logs/mvnf/20260921-213120-verify.log`; its Medical Records q0–q12 smoke evidence is retained in `/tmp/rdf4j-theme-medical-baseline/module-medical-q0-q12.txt`. The final isolated warmed runs are `final-warmed-q6.log` through `final-warmed-q9.log`; q8 is 3.504 ms/op with `domainDrivenRows: 0`. Forced q8 serial and parallel runs are `final-forced-serial-q8.log` (3.135 ms/op) and `final-forced-parallel-q8.log` (8.759 ms/op). The final JFR is `final-q8.jfr`; its query-thread samples are led by projection/adjoining access rather than domain enumeration. Commit and push remain outstanding.

## Context and Orientation

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelLowering.java` translates `SlotPlan` trees into generated LMDB kernel nodes. Its nested `Builder` tracks `assuredMask`, the slots bound on every row at the current lowering depth. `MultiJoinPlan` represents a replay-safe inner-join bag. `LmdbNativeJoinPlans.derivedPlan(row)` preserves the compiler child order and only computes filter placement; it cannot by itself repair a child that is unbound at the current nested depth. `ThemeQueryCatalog` defines Medical Records q6–q9, and `LmdbThemeQueryRegressionIT` is the integration correctness coverage. The JMH entry point is `ThemeQueryBenchmark`.

## Plan of Work

First add a unit regression in `LmdbNativeKernelLoweringTest` that constructs an outer patient producer and a nested OPTIONAL `MultiJoinPlan` whose children are deliberately ordered as an unbound `enc -> practitioner` pattern followed by a `patient -> enc` pattern. The assertion will inspect the generated nested IR/shape and require the patient-correlated producer to run first. Run only that test and save the failing report before editing production code.

Then add a small, general ordering helper to the lowering builder. It will inspect only replay-safe inner-join children and use the current `assuredMask` plus the entry mask to identify children with at least one assured endpoint. It must not reorder filters across their legal depth, must not reorder opaque plans, and must retain the original order when no safe correlated child exists. Nested `MultiJoinPlan`, OPTIONAL, UNION, MINUS, EXISTS, and nullable paths will remain on their existing boundary logic unless their children satisfy the same proof.

After the focused test passes, run the related lowering class, the Medical q6–q9 correctness integration selection, and the LMDB module gate through `mvnf`, retaining logs. Rebuild the benchmark artifact without deleting the original store, run the warmed q6–q9 selection against the protected clone, and capture q8 plan order, domain-driven rows, roots, neighbors, and result time. Review the diff and status, stage only the plan, production, and regression-test files belonging to this fix, commit with the required `GH-0000` prefix, and push the current `optimize-lmdb` branch.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j` unless stated otherwise. Preserve `/tmp/rdf4j-theme-medical-baseline` and never invoke the benchmark against the original `core/sail/lmdb/target/lmdb-theme-query-benchmark` directory.

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelLoweringTest#<new-test> --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeKernelLoweringTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --it LmdbThemeQueryRegressionIT --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

The focused test must fail before the production edit with an assertion showing the reversed nested producer order, and pass afterward. Maven test runs must not use `-am` or `-q`.

## Validation and Acceptance

The focused unit test proves the structural invariant. Medical q6–q9 integration tests prove the expected result bindings/counts and related composition shapes. The final benchmark is accepted only when q8 no longer reports the 208M domain-driven-row pathology and its plan begins with the patient-bound encounter lookup; q6, q7, and q9 remain valid. Runtime numbers are evidence, not the correctness assertion.

## Idempotence and Recovery

The store clone and logs under `/tmp` are disposable research artifacts and can be reused. Maven test runners may rebuild their own ignored workspaces; they must not delete the original benchmark store. If a build requires a clean target, verify the CoW clone exists first and restore only the ignored generated benchmark artifact as needed. Never reset, clean, restore, or overwrite unexpected tracked changes.

## Artifacts and Notes

    /tmp/rdf4j-theme-medical-baseline/warmed-q6-q8.log
    /tmp/rdf4j-theme-medical-baseline/final-warmed-q6.log
    /tmp/rdf4j-theme-medical-baseline/final-warmed-q7.log
    /tmp/rdf4j-theme-medical-baseline/final-warmed-q8.log
    /tmp/rdf4j-theme-medical-baseline/final-warmed-q9.log
    /tmp/rdf4j-theme-medical-baseline/final-forced-serial-q8.log
    /tmp/rdf4j-theme-medical-baseline/final-forced-parallel-q8.log
    /tmp/rdf4j-theme-medical-baseline/final-q8.jfr
    /tmp/rdf4j-theme-medical-baseline/module-medical-q0-q12.txt
    logs/mvnf/20260921-215238-verify.log
    logs/mvnf/20260921-213120-verify.log
    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/results-2026-09-19.md

## Interfaces and Dependencies

The implementation remains inside the package-private native lowering model and uses existing `SlotPlan`, `PatternPlan`, `Term`, `assuredMask`, `slotOperand`, and IR nodes. No public API, persisted data format, external dependency, or benchmark-only switch is added. The test uses existing JUnit 5 helpers and the existing in-memory lowering stubs.
