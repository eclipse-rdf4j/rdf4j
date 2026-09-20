# Repair optimizer scope, binding guarantees, and evaluation resources

This ExecPlan is a living document and follows `/Users/havardottestad/Documents/Programming/rdf4j7/PLANS.md`. It records the work needed to make the RDF4J query optimizer and the shared evaluation path preserve SPARQL results while still using connected join and cost decisions. The plan must be updated when evidence changes the design.

## Purpose / Big Picture

Queries containing `MINUS` (the algebra operator represented by `Difference`), optional values, nested filters, and computed bindings must produce the same rows before and after optimizer reordering. The user-visible result is that the standard evaluator and the LMDB evaluator retain correct rows for these constructs, while the optimizer may still choose a cheaper connected order when the choice is semantically safe. Failures in this area are especially costly because a plan can look connected and fast while silently dropping rows.

The change is demonstrated by the adversarial optimizer tests in `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/QueryJoinOptimizerAdversarialTest.java`, the model guarantees in `core/queryalgebra/model/src/test/java/org/eclipse/rdf4j/query/algebra/BindingSetAssignmentTest.java`, and the LMDB regression test in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbSPARQL11ComplianceRegressionTest.java`. A valid repair must pass focused tests, the affected modules, and the relevant backend and iterator variants.

## Progress

- [done] (2026-09-20) Read repository instructions, `PLANS.md`, and the `mvnf` skill; completed the mandatory root clean install and formatter/header checks.
- [done] (2026-09-20) Published the intended adversarial test and audit scope in commit `a7eb18ba25` and pushed it before rerunning the failing class, as requested.
- [done] (2026-09-20) Captured the initial class report: 14 invocations, 10 intentional failures, and no errors in `initial-evidence.txt`.
- [done] (2026-09-20) Repaired connected candidate selection, `Difference` output-domain accounting, filter relocation, type-fact inference, and the standard evaluator's independent MINUS-containing join path.
- [done] (2026-09-20) Added lazy RHS evaluation and failure cleanup to `IndependentJoinIteration`; focused close and no-left-row tests pass.
- [done] (2026-09-20) Added non-null binding-set guarantee and extension-overwrite regressions; red evidence is in `logs/mvnf/20260920-184703-verify.log` and `logs/mvnf/20260920-184732-verify.log`, and focused greens are in `184855`, `184933`, and `185009`.
- [done] (2026-09-20) Validate every optimizer and evaluation pipeline variant, update the audit with confirmed and refuted findings, and prepare the review-ready fix commit.
- [done] (2026-09-20) Run module and backend gates, including the standard evaluation, query algebra model, MemoryStore, LMDB, lateral, and binding inliner suites.
- [in_progress] Format, inspect the exact staged scope, commit, push, and report final evidence.

## Surprises & Discoveries

- `Difference` comparison uses the domains of the actual left and right results. Incoming bindings can be required by a RHS `SERVICE`, `EXISTS`, or `LATERAL` context, but they must not manufacture a comparison variable that the left result never binds. Global RHS input filtering therefore broke a valid service case and was removed.
- A right-only variable under `Difference` must not become an outer cost connection. The standard optimizer flattens ordinary joins before final reordering, so a late barrier alone cannot enforce this rule; candidate collection and scope-aware evaluation both need coverage.
- `FilterOptimizer.FilterRelocator.meet(Difference)` copied an outer filter into both operands. A filter on a left-only variable can then reject every RHS row because that variable is unbound there. Relocation now keeps the filter on the left side.
- `BindingSetAssignment.getBindingNames()` is a schema union, while `getAssuredBindingNames()` must be the intersection of names with non-null values in every row. `ListBindingSet` deliberately reports null slots in its schema, so using only `getBindingNames()` falsely authorizes hash joins.
- `Extension` inherits child guarantees even when an `ExtensionElem` writes the same name. An error in that expression installs a null binding in the ordinary evaluator, so the overwritten name must be removed from the conservative guarantee set. The runtime regression proves that the wrong guarantee selected a hash join and dropped a compatible unbound row.
- A hash join cannot represent an unbound join key as a wildcard. The independent fallback materializes the RHS once after the first left row and checks compatibility, which is correct but has an O(left rows times right rows) comparison cost for nullable keys. This plan records that tradeoff and does not claim an unmeasured performance improvement.
- `BindingSetAssignmentInlinerTest.testOptimize_Minus` already proves that the parser's scope-change nodes protect ordinary `VALUES` from leaking into a MINUS RHS. The broad inliner-leak hypothesis is therefore refuted and receives no production workaround.
- `STR` accepts IRIs, literals, and triple terms, but a statement pattern can also bind a blank node or another unsupported value. The original universal statement-produced `STR` work assertion is reclassified as an invalid premise; the direct typed STR and statement-produced `isIRI` measured-work assertions remain required.

## Decision Log

- Decision: Keep the two-argument `MinusQueryEvaluationStep` and `SPARQLMinusIteration` constructors for compatibility, and use explicit output-domain comparison names in the prepared Difference path. Rationale: callers that supply an evaluation context still need their incoming bindings, while ordinary MINUS comparison must ignore unrelated input names. Date/Author: 2026-09-20, Codex.
- Decision: Isolate MINUS-containing ordinary joins at the join boundary with `IndependentJoinIteration`, but stop isolation at `Service` and `Lateral` scopes. Rationale: ordinary join results must not inject left rows into a right MINUS evaluation, while declared correlated contexts must retain their inputs. Date/Author: 2026-09-20, Codex.
- Decision: Use conservative guarantee facts for type predicates and computed values. Rationale: `BOUND` is total, while predicates such as `isIRI` are safe only when their operands are guaranteed; arbitrary unary and binary functions cannot be marked total without proof. Date/Author: 2026-09-20, Codex.
- Decision: Correct assurance metadata at the query-algebra model boundary instead of teaching each runtime join selector to guess through wrappers. Rationale: `BindingSetAssignment` and `Extension` are shared by the standard and LMDB planning paths, so conservative model facts prevent the same false hash assurance in every backend. Date/Author: 2026-09-20, Codex.
- Decision: Do not synchronize the standard optimizer stage list into LMDB. Rationale: LMDB has its own `LmdbSketchJoinOptimizer` and rewrite ordering; equivalent semantics must be tested through its public pipeline rather than copied mechanically. Date/Author: 2026-09-20, Codex.

## Outcomes & Retrospective

The focused, module, and backend gates are green. The final adversarial class has 33 passing tests. The query-algebra model module has 50 passing tests and one existing skip; the evaluation module has 909 passing tests and one existing skip; the full LMDB module has 1199 passing tests and 102 existing skips. The audit records every original invocation, the refuted inliner hypothesis, the reclassified untyped STR cost premise, and the O(left rows times right rows) nullable-key fallback tradeoff. The exact fix commit and pushed SHA remain to be recorded after final formatting and staged-scope review.

## Context and Orientation

`QueryJoinOptimizer` in `core/queryalgebra/evaluation` estimates and reorders joins. Its connectedness logic must treat the left result of `Difference` as the only outer output and must account for computed values only when their inputs and value kinds are guaranteed. `DefaultEvaluationStrategy` compiles algebra nodes to `QueryEvaluationStep` objects. `MinusQueryEvaluationStep` and `SPARQLMinusIteration` implement MINUS comparison. `JoinQueryEvaluationStep` chooses hash, merge, service, or nested-loop evaluation. `IndependentJoinIteration` is the nested-loop implementation used when a normal join contains a same-scope `Difference` and nullable keys make hash lookup unsafe.

The query algebra model in `core/queryalgebra/model` supplies `getBindingNames()` (all names that can occur) and `getAssuredBindingNames()` (names present in every output row). `BindingSetAssignment` represents VALUES or caller-provided rows; `Extension` represents BIND-like computed assignments. `FilterOptimizer` moves filters only when their variable scope is valid. The LMDB module has a separate planning path and must be checked through `LmdbSPARQL11ComplianceRegressionTest` and its existing compliance tests.

## Plan of Work

The first implementation phase adds or preserves tests for each observed semantic failure, runs them red, and stores the report paths in `initial-evidence.txt`. The repair phase keeps the Difference comparison domain separate from evaluator input bindings, prevents outer filter copies from entering the RHS, and uses a scope-aware independent join for ordinary joins whose RHS contains Difference. The independent iterator evaluates the left first, delays RHS materialization until a left row exists, and closes the left iteration if RHS evaluation or materialization fails.

The model phase computes BindingSetAssignment assurance by intersecting only non-null row values while retaining the complete binding-name schema. It preserves explicit schemas and one-shot/dynamic iterables by caching both name sets from the same traversal. Extension assurance starts with the child assurance and removes every name written by an extension element because the expression may error or overwrite the prior value. The optimizer's type analysis remains conservative across joins, unions, projections, optionals, and nested extensions.

The verification phase reruns the adversarial class and its related optimizer, iterator, inliner, lateral, model, evaluation, MemoryStore, and LMDB suites. It then runs formatting, header checks, `git diff --check`, and a final full-scope status review. Logs and audit evidence remain untracked unless the final review explicitly chooses the substantive audit and plan documents for the fix commit.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j7`. Use Java 25 or newer and always pass `-Dmaven.repo.local=.m2_repo`. The `mvnf` runner performs the required root quick install before each test selection and must be used without `-am` or `-q` for tests.

The smallest regressions are run as follows:

    python3 .codex/skills/mvnf/scripts/mvnf.py BindingSetAssignmentTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerAdversarialTest#extensionOverwriteCannotAuthorizeHashJoinForNullableOutput --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerAdversarialTest#independentJoinClosesLeftWhenRightEvaluationFails --retain-logs

Broader selections are then run with the same runner, followed by module gates. If an offline dependency is missing, rerun only the exact install once without `-o`, then return to offline tests. Never delete the repository's untracked logs, reports, or evidence files.

## Validation and Acceptance

Acceptance requires the red regressions above to fail on the pre-fix implementation and pass after the model and evaluation changes. The final adversarial class must have zero failures and zero errors. The model assurance test must show the complete schema while excluding null slots from its assured set. The Extension runtime regression must return the compatible row that hash evaluation previously dropped. The iterator failure test must throw during consumption and must report that the left resource closed.

The final gate list includes `BindingSetAssignmentTest`, `QueryJoinOptimizerAdversarialTest`, `QueryJoinOptimizerTest`, `FilterOptimizerTest`, `BindingSetAssignmentInlinerTest`, `SPARQLMinusIterationTest` and its fuzz variants, `LateralQueryEvaluationStepTest`, the full query-algebra model module, the full query-algebra evaluation module, the MemoryStore MINUS scoping regression, the LMDB compliance regression, and the connected dependency/scope matrix classes. Each report must be retained or summarized in `initial-evidence.txt`; a failure is triaged against the specific semantic contract before any further edit.

## Idempotence and Recovery

All edits are additive or localized and can be rerun without changing branch selection. A failed test run may overwrite module reports, so append a compact summary to `initial-evidence.txt` immediately and retain the full `logs/mvnf/*-verify.log`. If formatting changes a touched file, inspect the diff before staging. Do not use `git reset --hard`, `git clean`, or manual stash operations. Before committing, stage only the intended source, test, audit, and this plan files; leave all diagnostic logs and evidence artifacts untouched.

## Artifacts and Notes

Initial publication: commit `a7eb18ba25` (`GH-5906 add adversarial optimizer coverage`) was pushed before the failing rerun. Initial red evidence is in `initial-evidence.txt` and `logs/mvnf/20260920-165043-verify.log`. Current assurance red evidence is in `logs/mvnf/20260920-184703-verify.log` and `logs/mvnf/20260920-184732-verify.log`; focused post-fix reports are `logs/mvnf/20260920-184855-verify.log`, `logs/mvnf/20260920-184933-verify.log`, and `logs/mvnf/20260920-185009-verify.log`.

## Interfaces and Dependencies

The public algebra interface remains `TupleExpr.getBindingNames()` and `TupleExpr.getAssuredBindingNames()`. `BindingSetAssignment` must retain its existing setters and iterable input contract. `MinusQueryEvaluationStep` retains its legacy constructor and adds only internal metadata for the prepared Difference path. `IndependentJoinIteration` consumes two `QueryEvaluationStep` instances and one incoming `BindingSet`, returns a `CloseableIteration<BindingSet>`, evaluates the RHS at most once after the first left row, and closes resources on normal close and RHS failure. No new external dependency is required.

Plan update (2026-09-20): added the non-null ListBindingSet and Extension overwrite findings after their red regressions, documented the service/correlation boundary and O(left times right) fallback cost, and recorded the refuted BindingSetAssignment inliner hypothesis.
