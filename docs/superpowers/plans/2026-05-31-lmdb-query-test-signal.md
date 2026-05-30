# LMDB and Query Evaluation Test Signal

This ExecPlan is a living document. It follows `.agent/PLANS.md` and must be kept current while work proceeds.

## Purpose / Big Picture

The LMDB and query-evaluation optimizer tests should protect durable behavior: correct results, bounded work, reliable estimator invariants, and rewrites that improve a whole class of queries. They should not lock the optimizer into a single incidental plan for one query shape. After this work, the selected test set will make planner regressions visible without blocking legitimate optimizer improvements.

## Progress

- [x] (2026-05-31 00:23+02:00) Confirmed branch and worktree: `GH-0000-lmdb-predicate-guarantees`, tracked files clean, only pre-existing untracked artifacts.
- [ ] Capture current failing/pass matrix for `core/queryalgebra/evaluation`.
- [ ] Capture current failing/pass matrix for `core/sail/lmdb`.
- [x] (2026-05-31 00:27+02:00) Added parent Surefire/Failsafe fork timeout and JUnit Jupiter default timeout; verified one query-evaluation class and one LMDB class.
- [ ] Classify planner/estimator/optimizer tests into signal categories and disable brittle tests with explicit reasons.
- [ ] Add generator-style estimator tests that create data distributions and assert monotonicity, upper-bound, and selectivity invariants.
- [ ] Add rewrite tests that assert safe performance invariants for pattern classes rather than exact query-plan strings.
- [ ] Fix production code only after a focused failing test or existing failing report identifies the root cause.
- [ ] Commit and push each green increment.

## Surprises & Discoveries

- Observation: Current tracked tree is clean even though previous patch work exists in branch history.
  Evidence: `git status -sb` reported only untracked artifacts and `ahead 29`.

- Observation: Parent Surefire/Failsafe configuration is inherited by both target modules, and focused tests still start and pass with the timeout properties active.
  Evidence: `CascadesCostModelTest` passed 21/21; `LmdbCascadesOptimizerTest` passed 21/21.

## Decision Log

- Decision: Treat exact plan-shape tests as suspect unless they assert a semantic or broad performance invariant.
  Rationale: The user explicitly wants tests to prevent monkey patches and avoid pinning one query to one fragile plan.
  Date/Author: 2026-05-31 / Codex.

- Decision: Add timeout protection before running broad suites that may deadlock.
  Rationale: The user explicitly requested that tests never deadlock, and a broad LMDB run can otherwise waste hours.
  Date/Author: 2026-05-31 / Codex.

## Outcomes & Retrospective

No completed milestone yet.

## Context and Orientation

The work spans two Maven modules. `core/queryalgebra/evaluation` contains generic query algebra evaluation, sketch estimation, cascades planner infrastructure, and optimizer tests. `core/sail/lmdb` contains the LMDB-backed store, LMDB estimator, LMDB-specific optimizer rules, and many regression/benchmark-style tests. A planner test has high signal when it checks a property like "a selective anchor is chosen before an unbounded path" or "a rewrite preserves results and lowers estimated work for a known pattern class." A planner test has low signal when it asserts a full plan string or a single exact cost for one fixture without explaining why that exact shape is always better.

Important existing commands:

- `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs`
- `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs`
- `python3 scripts/agent-evidence.py <module>/target/surefire-reports <module>/target/failsafe-reports`

Maven test commands must use `-Dmaven.repo.local=.m2_repo`, must not use `-q`, and must not use `-am` with tests enabled.

## Plan of Work

First, add timeout protection in Maven/JUnit configuration so broad runs terminate rather than deadlock. Then run the query-evaluation and LMDB module tests with compact retained logs. Use Surefire/Failsafe reports, not console noise, as the matrix source.

Second, classify failing and planner-sensitive tests. Keep tests that assert semantic correctness, monotonic estimator behavior, bounded work, absence of Cartesian explosions for a pattern class, direct lookup when constants/bindings make it universally better, and rewrite safety. Disable tests that only pin exact plan text or exact estimator internals for a single query unless they can be rewritten into an invariant.

Third, add focused generator tests. For estimators, generate small deterministic RDF-like datasets with skew, zero intersections, optional-only bindings, finite value sets, duplicate-heavy predicates, and property-path endpoints. Assert invariants such as adding a constant does not increase estimated rows, exact finite sets do not exceed their cardinality, disjoint joins estimate near zero, and a bound endpoint path is cheaper than a full path scan. For rewrites, generate matching/non-matching patterns and assert result equality plus lower or bounded planned work for the matching class.

Fourth, fix production code one root cause at a time. Each fix starts from an existing failing report or a new failing invariant test, then reruns the narrow class/method and the relevant module selection.

## Concrete Steps

1. Inspect existing timeout setup in `pom.xml`, `core/queryalgebra/evaluation/pom.xml`, and `core/sail/lmdb/pom.xml`.
2. Add or adjust timeout configuration with minimal scope.
3. Run focused compile/test selections and preserve compact reports.
4. Build a failure matrix from Surefire/Failsafe reports.
5. Edit low-signal tests using `@Disabled` with a clear reason, or rewrite them into invariant tests.
6. Add new deterministic generator helpers only when existing fixtures cannot express the invariant.
7. Fix production code for each remaining high-signal failure.
8. Commit after each green increment and push the branch.

## Validation and Acceptance

Acceptance for each increment is a green targeted test run plus no formatting/header regressions. Final acceptance is:

- Query-evaluation selected tests pass.
- LMDB selected tests pass.
- Tests that remain disabled have explicit, reviewable reasons tied to low signal or known non-goals.
- New estimator tests generate data and queries rather than pinning one fixture-only plan.
- New rewrite tests prove result preservation and broad performance invariants.
- Timeout protection prevents hung tests from deadlocking the suite.
- Commits are pushed to `origin/GH-0000-lmdb-predicate-guarantees`.

## Idempotence and Recovery

All test runs are safe to repeat. Generated Maven logs may be overwritten, but retained `logs/mvnf` reports and Surefire/Failsafe summaries remain enough to rebuild the matrix. Do not remove pre-existing untracked artifacts. If a broad test run hangs despite timeout configuration, stop it with an approved `kill` and record the class in `Surprises & Discoveries`.

## Artifacts and Notes

Current branch state before edits:

    ## GH-0000-lmdb-predicate-guarantees...origin/GH-0000-lmdb-predicate-guarantees [ahead 29]
    ?? core/queryalgebra.zip
    ?? core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java.orig
    ?? core/sail/lmdb.zip
    ?? docs/superpowers/plans/2026-05-27-pipeline-wide-no-cartesian-join-planning.md
    ?? htmlReport.zip
    ?? trace.txt

## Interfaces and Dependencies

Use JUnit 5 `@Timeout` or JUnit platform timeout properties where available. Use Maven Surefire/Failsafe fork timeouts when a module can contain JUnit 4, JUnit 5, or mixed tests. Avoid new dependencies.
