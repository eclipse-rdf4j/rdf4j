# Delete Legacy LMDB Sketch Planner

This ExecPlan is a living document. It follows `.agent/PLANS.md` in this repository and must be kept up to date as work proceeds.

## Purpose / Big Picture

LMDB now uses the Cascades optimizer path by default. The older sketch join optimizer is still present behind default-off flags, but the branch does not use it in the normal optimizer pipeline. Removing this code lowers planner maintenance cost, removes legacy tests that only validate the dead path, and prevents future fixes from being split across two optimizer implementations. A reviewer can see the change working by compiling the LMDB module and by running the optimizer pipeline tests that prove the live Cascades path remains registered while the legacy sketch optimizer is absent.

## Progress

- [x] 2026-07-08: Confirmed `/tmp/rdf4j-cascades-fix` is the branch/worktree containing the requested Cascades and legacy sketch files.
- [x] 2026-07-08: Created this local ExecPlan before production edits.
- [ ] Run required root quick install baseline.
- [ ] Delete the legacy sketch optimizer classes and their direct legacy-only tests.
- [ ] Remove legacy flag registration and SKETCH_INPUT baseline capture from the live pipeline.
- [ ] Remove legacy opaque Cascades rules and references to the deleted planner helper.
- [ ] Compile and run focused LMDB optimizer tests.
- [ ] Format, inspect diff, commit, and push.

## Surprises & Discoveries

- Observation: The main checkout at `/Users/havardottestad/Documents/Programming/rdf4j` is on `optimize-lmdb` and does not contain the Cascades/sketch files named in the request.
  Evidence: `rg --files core/sail/lmdb/src/main/java | rg "LmdbCascadesOptimizer|LmdbCascadesRuleProvider"` returned no hits there.
- Observation: The already-pushed worktree `/tmp/rdf4j-cascades-fix` is clean and contains the requested files.
  Evidence: `git status --short --branch --untracked-files=no` reported only `## GH-0000-lmdb-predicate-guarantees...origin/GH-0000-lmdb-predicate-guarantees`.

## Decision Log

- Decision: Apply this deletion in `/tmp/rdf4j-cascades-fix`, not the main checkout.
  Rationale: The user cited files that exist in that worktree and do not exist in the main checkout. Editing the main checkout would only produce a partial, wrong-branch cleanup.
  Date/Author: 2026-07-08 Codex.
- Decision: Treat this as a significant refactor with compile/test proof rather than adding new behavioral tests.
  Rationale: The requested change deletes unreachable flag-gated behavior and its legacy-only tests. Acceptance is absence of dead registrations and preservation of the live Cascades optimizer pipeline.
  Date/Author: 2026-07-08 Codex.

## Outcomes & Retrospective

Not completed yet.

## Context and Orientation

The LMDB module lives under `core/sail/lmdb`. The query optimizer pipeline is assembled in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java`. The live planner is the Cascades optimizer, implemented across `LmdbCascadesOptimizer.java`, `LmdbCascadesRuleProvider.java`, and related cost/model classes. The legacy sketch optimizer is a separate older optimizer in `LmdbSketchJoinOptimizer.java`; it uses helper classes such as `LmdbSmallLiteralFilterAnchors.java` and `LmdbUnionFilterDistributor.java`. The standard-plan baseline capture helper `LmdbStandardPlanBaselineOptimizer.java` currently has a `SKETCH_INPUT` mode used only to feed the legacy sketch plan into a later Cascades fallback path.

## Plan of Work

First run the repository-required root quick install in `/tmp/rdf4j-cascades-fix` so tests resolve against fresh local artifacts. Then remove the legacy sketch optimizer registration in `LmdbQueryOptimizerPipeline.java`, including the `rdf4j.optimizer.lmdb.legacySketchOptimizer` property. Delete `LmdbSketchJoinOptimizer.java`, `LmdbSmallLiteralFilterAnchors.java`, and `LmdbUnionFilterDistributor.java` with their direct legacy-only test classes. Next, remove the `SKETCH_INPUT` baseline capture from `LmdbStandardPlanBaselineOptimizer.java` and the matching stage in `LmdbCascadesOptimizer.java`. Finally, remove the default-off legacy opaque rule path from `LmdbCascadesRuleProvider.java` and replace any use of `LmdbSketchJoinOptimizer.plannerAlgorithmForSegment(...)` with a small live helper or inline threshold logic owned by the Cascades class.

Tests that directly construct deleted classes will be removed. Pipeline tests will be updated to assert the legacy optimizer is absent and the Cascades optimizer remains present. Any production references to deleted symbols must be eliminated before compile.

## Concrete Steps

Run from `/tmp/rdf4j-cascades-fix`:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Expected result: reactor summary ends with `BUILD SUCCESS`.

After edits, run focused verification:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbOptimizerPipelineTest --module core/sail/lmdb --retain-logs

Expected result: `Tests passed` with zero failures and zero errors.

Run a compile-focused module verify or targeted test set as needed when deleted references are found. Do not use `-am` or `-q` for test runs.

## Validation and Acceptance

Acceptance is met when `rg -n "LmdbSketchJoinOptimizer|legacySketchOptimizer|legacyOpaqueJoinProviders|SKETCH_INPUT" core/sail/lmdb/src/main/java` returns no live production references, the LMDB optimizer pipeline tests pass, and `git diff --check` reports no whitespace errors. If the full LMDB module test run exposes unrelated existing branch failures, record the failing selector and keep the deletion patch scoped.

## Idempotence and Recovery

The deletion is ordinary Git state. If a compile failure names a deleted symbol, find the caller with `rg`, either remove the legacy-only caller or replace it with a live Cascades-owned helper. Do not revert unrelated work. If the wrong checkout is detected, stop and return to `/tmp/rdf4j-cascades-fix`.

## Artifacts and Notes

The initial reference scan showed tests and benchmark result markdown still mention the legacy classes. Benchmark result markdown is historical output and should not drive production compile. Direct legacy-only tests should be deleted with the production classes because they would only preserve the dead path.

## Interfaces and Dependencies

No new dependencies. No public RDF4J API changes. The live optimizer pipeline remains an implementation of `org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerPipeline` and continues to include the Cascades optimizer for LMDB planning.
