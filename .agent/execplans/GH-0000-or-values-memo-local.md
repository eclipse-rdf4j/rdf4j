# Make LMDB OR-to-VALUES Rewrites Memo-Local

This ExecPlan is a living document. It must be maintained in accordance with `.agent/PLANS.md`, including the
`Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` sections.

## Purpose / Big Picture

LMDB's OR-to-VALUES Cascades rule currently matches any tuple expression, recursively searches its descendants for a
matching `Filter`, and clones every parent between the rule application and that filter. This can attribute a
child-local semantic proof to a selected parent route and creates redundant whole-subtree alternatives. After this
change, the rule matches and rewrites only the current `Filter`. Cascades memo dependencies remain responsible for
reactivating and recosting parents when that child group gains the VALUES alternative.

## Progress

- [x] (2026-07-17 07:48Z) Audited the rule registration, recursive helper, and current callers.
- [x] (2026-07-17 07:52Z) Added and ran the smallest proof-locality regression.
- [x] (2026-07-17 07:53Z) Preserved the failing workspace report as initial evidence.
- [x] (2026-07-17 07:54Z) Replaced wildcard routing with a Filter-local emitter.
- [x] (2026-07-17 08:02Z) Reran the focused method and full coverage class successfully.
- [x] (2026-07-17 08:03Z) Audited the owned-file diff and recorded the outcome.

## Surprises & Discoveries

- Observation: `LmdbCascadesRuleProvider.orFilterValuesAlternative` has no caller other than
  `LmdbSemanticRuleSpecs.orFilterValues` in production or tests.
  Evidence: `rg -n "orFilterValuesAlternative\\(|rewriteOrFilterValues\\(" core/sail/lmdb/src/main core/sail/lmdb/src/test`
  returns only the semantic spec, the provider entry point, and its recursive implementation.

- Observation: One matching Filter caused the wildcard rule to emit proof-bearing rewrites from three roots.
  Evidence: The focused test failed with `Transparent parents emitted a child-local OR-to-VALUES proof: [Filter,
  Projection, QueryRoot]` and `tests=1, failures=1, errors=0, skipped=0`.

- Observation: Scoped join materialization selected the correct VALUES shapes without copying the source Filter proof
  onto independent join recipes.
  Evidence: Three multi-factor plans contained the expected finite assignments and no OR, while direct memo inspection
  kept every OR proof in a group containing its matching Filter.

- Observation: The first post-change test was green, but Maven later failed on the repository's unrelated `japicmp`
  report for the removed `LmdbCardinalityCalculator` type.
  Evidence: Surefire reported `tests=1, failures=0, errors=0, skipped=0`; the same selector passed cleanly with
  `-Djapicmp.skip=true`.

## Decision Log

- Decision: Prove locality at the compiled-rule boundary before changing production code.
  Rationale: This directly catches the wildcard application that assigns a descendant proof to a parent expression,
  without depending on cost-based winner selection.
  Date/Author: 2026-07-17 / Codex

- Decision: Mirror the already migrated finite-filter DSL shape.
  Rationale: A `filter("root", capture("input"), scalar("condition"))` pattern keeps scalar subqueries and parent
  wrappers as independent memo inputs while preserving the existing IR import/export bridge.
  Date/Author: 2026-07-17 / Codex

- Decision: Do not propagate the source Filter proof into independently derived scoped join recipes.
  Rationale: Selected VALUES shape proves the physical route, while the memo-local proof preserves the exact semantic
  derivation. Copying it to a different recipe would recreate false provenance.
  Date/Author: 2026-07-17 / Codex

## Outcomes & Retrospective

The OR-to-VALUES rule now matches only a current non-scope-changing Filter and emits a local Join or residual Filter
alternative. The recursive parent walker and its unused entry point were removed. The focused regression changed from
red to green, correlated EXISTS/NOT EXISTS memo coverage remains active, and all 22 coverage tests pass. No public API,
dependency, formatter, or module-wide change was made.

## Context and Orientation

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSemanticRuleSpecs.java` declares LMDB semantic rules for
the generic Cascades rule compiler. Its legacy `tupleRule` helper matches any root expression, but the OR-to-VALUES
declaration now uses a Filter-specific DSL pattern.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesRuleProvider.java` contains the rewrite logic.
`cascadesOrFilterValuesAlternative` converts a safe disjunction of exact RDF terms on the current Filter into a
`BindingSetAssignment` joined to that filter's input, with a residual `Filter` when some conjuncts remain. It does not
search or clone enclosing parents.

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOrFilterRewriteCoverageTest.java` contains the
focused behavior and memo-provenance coverage. A memo is the optimizer's collection of equivalent expressions;
proof locality means that the OR-to-VALUES proof is emitted only by the group whose original operator is the matching
`Filter`, not by a transparent ancestor such as `Projection` or `QueryRoot`.

## Plan of Work

Add a regression that compiles `LmdbSemanticRuleSpecs.orFilterValues`, applies it independently to every logical memo
expression for a projected query, and requires every proof-emitting source expression to be a `Filter`. Run only that
method in Maven workspace `optimizer-or-values-local` and preserve its failure report.

Then change `orFilterValues` to use the Filter DSL pattern and add
`LmdbCascadesRuleProvider.cascadesOrFilterValuesAlternative(TupleExpr)`. The new provider method must reject null,
non-Filter, and scope-changing roots; otherwise it must reuse the existing finite-relation analysis and emit only the
current Filter's Join or residual Filter alternative. Remove the recursive entry point only after the caller audit
still proves it is unused.

Finally rerun the identical method, then the complete `LmdbCascadesOrFilterRewriteCoverageTest`. Do not run formatting
or module-wide tests in this isolated slice because other agents own concurrent source changes.

## Concrete Steps

From the repository root, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace optimizer-or-values-local \
      LmdbCascadesOrFilterRewriteCoverageTest#orValuesRewriteProofIsEmittedOnlyForMatchingFilterGroup \
      --module core/sail/lmdb --retain-logs

After the production change, rerun that exact command, followed by:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace optimizer-or-values-local \
      LmdbCascadesOrFilterRewriteCoverageTest --module core/sail/lmdb --retain-logs -- -Djapicmp.skip=true

The initial method must fail because parent expressions emit the proof. The same method and then the full class must
pass after the Filter-local migration.

## Validation and Acceptance

Acceptance requires the focused test to show one or more matching Filter expressions and zero proof-emitting
non-Filter expressions. Existing OR-to-VALUES cases must still produce the expected finite assignments, unsafe value
equalities must remain unrevised, and nested scalar-subquery coverage in the full class must remain green.

The initial failing report must be copied to `initial-evidence.optimizer-or-values-local.txt`. Exact workspace log and
Surefire report paths must be recorded here after each run.

## Idempotence and Recovery

The focused Maven commands are safe to repeat; the workspace runner creates a new run log and replaces only that
workspace's selected test reports. If compilation fails because of concurrent changes outside the owned files, record
the exact failure and retry after the owning agent restores compilation. Do not alter unrelated files.

## Artifacts and Notes

Initial red evidence:

    Summary: tests=1, failures=1, errors=0, skipped=0, time=0.346s
    Failure: Transparent parents emitted a child-local OR-to-VALUES proof: [Filter, Projection, QueryRoot]
    Report: .mvnf/workspaces/optimizer-or-values-local/build/org.eclipse.rdf4j/rdf4j-sail-lmdb/
      6.1.0-SNAPSHOT/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbCascadesOrFilterRewriteCoverageTest.txt
    Log: .mvnf/workspaces/optimizer-or-values-local/logs/
      20260717T075032.715406Z-75612-3d428f91/verify.log

Focused green evidence:

    Summary: tests=1, failures=0, errors=0, skipped=0, time=0.150s
    Log: .mvnf/workspaces/optimizer-or-values-local/logs/
      20260717T075718.961244Z-21536-6d66c4ab/verify.log

Full-class green evidence:

    Summary: tests=22, failures=0, errors=0, skipped=0, time=4.015s
    Report: .mvnf/workspaces/optimizer-or-values-local/build/org.eclipse.rdf4j/rdf4j-sail-lmdb/
      6.1.0-SNAPSHOT/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbCascadesOrFilterRewriteCoverageTest.txt
    Log: .mvnf/workspaces/optimizer-or-values-local/logs/
      20260717T080120.916189Z-48075-5ff511b3/verify.log

## Interfaces and Dependencies

No public API or dependency changes are permitted. The new package-private provider method is:

    static TupleExpr cascadesOrFilterValuesAlternative(TupleExpr tupleExpr)

The DSL rule retains its existing ID, promise, proof facts, reason, and behavior; only its match scope changes from any
tuple root to a current `Filter` root.

Revision note (2026-07-17): Created this focused execution note before the failing test so the isolated migration can
be resumed from this file alone.

Revision note (2026-07-17): Recorded the focused red result, which confirms that the wildcard rule assigns one
Filter-local rewrite proof to Filter, Projection, and QueryRoot application roots.

Revision note (2026-07-17): Closed the slice with the local implementation, scoped-join provenance decision, focused
green result, and complete 22-test coverage result.
