# Refactor LMDB Native Aggregate Compiler

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document follows `.agent/PLANS.md`.

## Purpose / Big Picture

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregateCompiler.java` currently combines query-shape recognition, slot-plan construction, native row cursors, OPTIONAL/LEFT JOIN execution, factorized tail aggregation, native boolean filters, synthetic value handling, and aggregate state tables in one 6,555-line file. The refactor keeps the same observable query behavior but moves these responsibilities into focused package-private implementation files. After the change, maintainers should be able to extend aggregation, joins, filters, or row plans without reopening one giant class.

## Progress

- [x] (2026-07-05T18:24:17Z) Read `.agent/PLANS.md`, high-performance Java guidance, and local memory for the native LMDB engine.
- [x] (2026-07-05T18:24:17Z) Ran the root quick install before production edits.
- [x] (2026-07-05T18:24:17Z) Captured pre-change focused test evidence in `initial-evidence.txt`.
- [x] (2026-07-05T19:12:00Z) Split the monolithic compiler into focused package-private source files.
- [x] (2026-07-05T19:13:00Z) Ran the same focused test selection after the split.
- [x] (2026-07-05T19:17:00Z) Ran final formatting, copyright, compile, and diff hygiene checks.

## Surprises & Discoveries

- Observation: the worktree already had an unrelated modified benchmark file before this refactor.
  Evidence: `git status --short --branch --untracked-files=no` showed `M core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`.
- Observation: the current aggregate/left-join/factorized behavior is green before refactoring.
  Evidence: `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs -Dtest=LmdbNativeGroupByTest,LmdbNativeNonCountAggregateTest,LmdbNativeFactorizedTailAggregationTest,LmdbNativeLeftJoinHashTest,LmdbNativeLeftJoinCursorTest` reported `tests=63, failures=0, errors=0, skipped=0`.
- Observation: the first post-split focused verification is green.
  Evidence: the same `mvnf` selection reported `tests=63, failures=0, errors=0, skipped=0, time=1.585s`.

## Decision Log

- Decision: Treat this as Routine D because the task is a significant production refactor, but keep the proof surface behavior-neutral with matching pre/post focused tests.
  Rationale: no query behavior is intentionally changing, yet the implementation split is large enough to need a restartable plan.
  Date/Author: 2026-07-05 / Codex.
- Decision: Keep `LmdbNativeAggregateCompiler` as the small facade and preserve its existing observability fields such as `COMPILED`, `LEFTJOIN_*`, and `FactorizedTail.*` compatibility counters.
  Rationale: tests and adjacent code read those fields directly; moving behavior must not break the branch-local test API.
  Date/Author: 2026-07-05 / Codex.

## Outcomes & Retrospective

`LmdbNativeAggregateCompiler.java` is now a 211-line facade that preserves the existing
`tryCompile(...)` entry point and observability counters. The native aggregate engine moved into
focused package-private implementation files for planner state, statement-pattern planning, VALUES
folding, filters, slot plans, row/group steps, join plans, left-join payload/memo handling, aggregate
state, factorized-tail execution, and synthetic value ids. The refactor intentionally preserves query
behavior: the same focused LMDB aggregate/left-join/factorized test selection passed before and after
the split.

Final evidence:

    Root/format: mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    Focused verify: logs/mvnf/20260705-191623-verify.log
    Summary: tests=63, failures=0, errors=0, skipped=0, time=1.872s
    Hygiene: git diff --check

## Context and Orientation

The LMDB native query engine lives under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`. `LmdbNativeEvaluationStrategy` asks `LmdbNativeAggregateCompiler.tryCompile(...)` to compile supported query fragments into native slot-based execution. A slot is an integer position in a row array; row cursors fill primitive LMDB value ids into those slots and delay RDF `Value` materialization until result rows or generic expression fallback need it.

The current compiler file contains these separable areas:

- A facade and planning entry point: `tryCompile(...)`, query-wide slot layout, synthetic values, and tuple-shape compilation.
- Row execution steps: `NativeRowsStep`, `NativeGroupStep`, and `NativeGroupIteration`.
- Slot plans and cursors: `SlotPlan`, `RowCursor`, statement-pattern plans, joins, unions, filters, extensions, MINUS, VALUES, and left joins.
- Native filters and probes: `NativeBooleanFilter`, `StatementPatternExistsFilter`, `ExistsFilter`, `ValueSetFilter`, `CachedCompareFilter`, and membership probes.
- Aggregate state: `AggregateSpec`, `AggState`, `AggContext`, primitive maps, group keys, and long hash sets.
- Factorized tail aggregation: the scan-once and grouped-tail optimization used by native aggregate tests.
- Synthetic value source: a plan-local wrapper for VALUES constants that do not map safely to stored LMDB ids.

## Plan of Work

First, keep `LmdbNativeAggregateCompiler.java` as a facade that owns public package-level observability counters and delegates to a new planner class. Then move coherent runtime groups into package-private files in the same Java package so package visibility replaces the old private nestmate access. The implementation should favor mechanical movement over behavior edits: constructors, fields, and helper methods may become package-private when another moved class needs them, but query semantics, fallback paths, system property names, and counter increments must stay unchanged.

The expected target structure is:

- `LmdbNativeAggregateCompiler.java`: facade, constants that tests reflect on, and compatibility counter aliases.
- `LmdbNativeAggregatePlannerBase.java`: shared planner state, slots, terms, aggregate specs, synthetic values, and dataset context helpers.
- `LmdbNativeAggregatePatternCompiler.java`: statement-pattern planning and constant filter folding.
- `LmdbNativeAggregateValuesCompiler.java`: VALUES folding and VALUES-row plan construction.
- `LmdbNativeAggregateFilterCompiler.java`: native boolean filters and EXISTS/list-member/compare compilation.
- `LmdbNativeAggregatePlanner.java`: top-level algebra-to-slot-plan orchestration and query-shape recognition.
- `LmdbNativeSlotPlan.java`: common `SlotPlan`, `RowCursor`, and small plans.
- `LmdbNativePatternTerms.java`: statement-pattern terms, context constraints, and raw pattern cursor contract.
- `LmdbNativePatternPlan.java`: statement-pattern cursor implementation.
- `LmdbNativeMultiValuePatternPlan.java`: multi-value statement-pattern plan.
- `LmdbNativeJoinPlans.java`: inner join, multi-join ordering, and ordinary join cursor code.
- `LmdbNativeLeftJoinPlans.java`, `LmdbNativeLeftJoinPayloadProbe.java`, and `LmdbNativeLeftJoinMemo.java`: native OPTIONAL/LEFT JOIN replay, hash, memo, and payload support.
- `LmdbNativeFilters.java`: native boolean filters, compare/list-member operands, and EXISTS helpers.
- `LmdbNativeRowPlans.java`, `LmdbNativeRowStep.java`, `LmdbNativeGroupStep.java`, and `LmdbNativeRowState.java`: row-stream plans, group steps, VALUES rows, primitive maps, and row state.
- `LmdbNativeAggregateState.java`: aggregate specs, group keys, primitive hash tables, and accumulator state.
- `LmdbNativeFactorizedTail.java` and `LmdbNativeFactorizedTailBranch.java`: factorized tail optimization and per-branch memo/scan-once execution.
- `LmdbSyntheticValueSource.java`: synthetic VALUES id wrapper.

## Concrete Steps

Work from repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

The pre-change evidence command was:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs -Dtest=LmdbNativeGroupByTest,LmdbNativeNonCountAggregateTest,LmdbNativeFactorizedTailAggregationTest,LmdbNativeLeftJoinHashTest,LmdbNativeLeftJoinCursorTest

After each structural split, compile the LMDB module before going deeper:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Pquick install

Do not use `-am` for test runs. The post-change focused test command must match the pre-change command exactly.

## Validation and Acceptance

The refactor is accepted when the same focused test selection that passed before the edit also passes after the edit, with report evidence retained under `logs/mvnf`. The file `LmdbNativeAggregateCompiler.java` should be small enough to read as a facade instead of a runtime implementation. New implementation files should have the standard RDF4J header and `// Some portions generated by Codex` immediately under it.

## Idempotence and Recovery

The refactor is behavior-neutral and should be restartable from Git plus this plan. If a compile error appears after moving a class, treat it as a visibility or name-boundary issue first: prefer package-private constructors/fields over semantic rewrites. If focused tests fail after compilation is restored, compare the moved class against the original block before changing logic.

## Artifacts and Notes

Pre-change focused evidence is stored in `initial-evidence.txt`:

    Summary: tests=63, failures=0, errors=0, skipped=0, time=1.337s
    Log: logs/mvnf/20260705-182222-verify.log

## Interfaces and Dependencies

All new classes remain package-private in `org.eclipse.rdf4j.sail.lmdb`. No new external dependencies are introduced. The only externally observed entry point remains:

    static QueryEvaluationStep LmdbNativeAggregateCompiler.tryCompile(
        TupleExpr expr,
        QueryEvaluationContext context,
        LmdbNativeEvaluationStrategy strategy,
        NativeLmdbQuerySource source)

Compatibility aliases for existing tests remain under `LmdbNativeAggregateCompiler`, including `COMPILED`, `LEFTJOIN_REPLAY_MATERIALIZATIONS`, `LEFTJOIN_HASH_BUILDS`, `LEFTJOIN_MEMO_MATERIALIZATIONS`, and `LmdbNativeAggregateCompiler.FactorizedTail`.
