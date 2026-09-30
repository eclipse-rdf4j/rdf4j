# Fix seven further query and lifecycle regressions

This living ExecPlan follows `.agent/PLANS.md`. Every bounded behavior change follows Routine A: add and run a failing in-repository regression, preserve its report, then change production and rerun the same selection. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current.

## Purpose / Big Picture

Restore query results when optimizer transformations cross grouping or variable scopes, when MINUS encounters expression errors, when legal variable names resemble internal constants, and when UNION branches use different graphs. Ensure query cancellation and store shutdown do not block behind their own lock dependencies. After these seven repairs, inspect another bounded portion of the same branch diff and repair any further high-confidence defect that can be reproduced.

## Progress

- [done] Reproduce and fix seven confirmed findings
- [done] Verify root fixes with related coverage
- [done] Review remaining paths for concrete defects
- [done] Fix and verify newly confirmed defects
- [done] Format changes and complete final checks

## Context and Orientation

The checkout is `/Users/havardottestad/Documents/Programming/rdf4j-small-things`, branch `GH-0000-lmdb-predicate-guarantees`, with review base `2169c9bc62c3d5ace654844497b846e34abb850c`. Preserve existing estimator cleanup, staged deletions, untracked artifacts, and eight earlier completed fixes. The start patch for this round is `/tmp/rdf4j-round2-start.patch`; hashes of existing modified and untracked source files are in `/tmp/rdf4j-round2-source-start.json`. Do not stage, commit, push, change branch, or remove unrelated files.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed/PackedLogicalRuleProgram.java` creates equivalent query alternatives. Its eligibility UNION rewrite can lift EXISTS above GROUP without preserving the correlation bindings in group keys. Its finite code/type rewrite retains branch zero's statement patterns without comparing graph scope and context against branch one. Root owns these fixes and evaluation tests in the packed package.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbFilterSimplifierOptimizer.java` attaches executable filters to raw value IDs using name-based statement origins that can cross subqueries and BIND boundaries. It also rewrites MINUS into a negated filter while mistaking bound STR arguments for error-free expressions. The optimizer agent owns these two changes and LMDB regression coverage.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnManager.java` closes transactions under the active-set monitor even though individual close takes the opposite lock order. The storage agent owns a lifecycle fix and deterministic concurrency tests that preserve pooled-reader cleanup.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/BindingUniverse.java` excludes all `_const_` names, including legal user variables, from schemas now used for executable hash joins. `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/MaterializedExistsFilterIteration.java` drains RHS scans under a cache monitor needed by close-time telemetry. The evaluation agent owns these fixes and their behavioral regressions.

## Plan of Work

Use independent named Maven workspaces to isolate writable build outputs: root uses `review-set`, optimizer uses `review-bound`, storage uses `review-overlay`, evaluation uses `review-equality`. Source is shared, so do not edit another owner's production or test file without coordinating. Save new red evidence under distinct top-level `initial-evidence.<workspace>-round2*.txt` names; preserve all existing evidence. Serialize source formatting after agents finish.

First reproduce each reviewed trigger with observable results or deterministic lifecycle assertions and related variants. Capture the failing Surefire snippet before touching its production path. Fix the general invariant in existing abstractions, retain positive optimization cases, and rerun the identical selection. Then run directly related suites. Once the seven fixes pass, independently inspect remaining diff paths; suspicious code that cannot be reproduced does not justify a speculative patch. Any additional defect uses the same test-first loop.

## Concrete Steps

Run commands at the checkout root. The initial root clean install was completed earlier in this conversation and all current workspaces already contain successful quick installs. Each mvnf invocation performs another root quick install before testing. Example:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace review-set --retain-logs PackedGroupGraphSafetyTest -- -Djapicmp.skip=true

Do not use `-am` or `-q` with tests. Do not run raw Maven concurrently with workspaces. Use the runner's printed full-GAV report and run-log paths when persisting evidence, never another workspace's or stale legacy reports. Full logs are retained under `.mvnf/workspaces/<workspace>/logs/<run-id>/`; reports under `.mvnf/workspaces/<workspace>/build/org.eclipse.rdf4j/<artifact>/6.1.0-SNAPSHOT/surefire-reports/`.

## Validation and Acceptance

Eligibility on a medication must filter the medications before patient-level DISTINCT aggregation; two medications with only one eligible produce count one. Correlation entirely within group keys must retain the safe lifted EXISTS optimization. Finite code/type rewriting must preserve differing graph scopes and context terms in supported direct algebra, and retain safe equal-graph alternatives.

An outer isIRI filter on a binding recreated by BIND must not constrain a hidden same-named literal in a projected subquery. MINUS must retain a row when its RHS filter errors on a blank node. Hash joins must reject incompatible mappings of legal `_const_` variables. Cancellation must close an active EXISTS scan without waiting for the scan to relinquish the cache monitor. Concurrent transaction close and manager shutdown must both finish and release pooled readers.

Run focused new and related suites, then an appropriate broader check if new cross-module changes or unresolved concerns warrant it. Known pre-existing compatibility failures caused by estimator API removals are separately documented; use `-Djapicmp.skip=true` for behavioral gates. The earlier full evaluation suite had two unchanged MINUS temp-directory tests fail under the workspace/JDK temporary-directory arrangement. Do not label those failures as green or disable their assertions.

## Idempotence and Recovery

Preserve all starting changes and evidence. Compilation/environment failures are not behavioral red evidence. If a production change accidentally precedes red evidence, undo only that new patch and reproduce first. Follow the repository offline-first dependency retry rule. Keep concurrency tests deterministic with observable public/package lifecycle behavior rather than sleeps or reflection to bypass interfaces.

## Surprises & Discoveries

The graph-context review finding is reachable through supported programmatic UNION algebra. Ordinary parsed UNIONs retaining the variable-scope-change flag are blocked earlier; the regression must exercise the actual admitted planner path and also preserve that existing guard.

PackedGroupGraphSafetyTest reproduced both wrong-result findings: medication-level eligibility yielded patient1/count2 plus an extra patient2/count1 instead of patient1/count1; different graph contexts yielded count1 instead of count2. The corrected five-case fixture had exactly two failures and zero errors in 0.284 seconds. Red evidence is retained in `initial-evidence.review-set-round2-confirmed.txt`, with the initial fixture report separately retained in `initial-evidence.review-set-round2.txt`. Safe single/composite group-key and identical-context controls passed before the fix.

TxnManager shutdown reproduced an actual monitor cycle in four tracked/untracked RESET/ABORT variants in isolated child JVMs. The identical four-case selection now passes after restoring lock order; red and green evidence are in `initial-evidence.review-overlay-round2-shutdown.txt` and `post-evidence.review-overlay-round2-shutdown.txt`.

The next review produced five further packed regression failures across three causes: global-group HAVING removal emits count-zero rows on empty input/eligibility; volatile aggregate operands or extensions observe multiplicities removed by eligibility/optional rewrites; nullable left bindings can be supplied by OPTIONAL and are not actually redundant. The ten-case class had five failures, zero errors in 0.307 seconds; exact evidence is in `initial-evidence.review-set-round2-followup.txt` from run `20260907T220923.073494Z-2839-4c7b7249`. These tests failed before the follow-up production changes. A separate MINUS case without shared variables also failed before its fix and is included in the 74-case LMDB filter gate.

The ten-case packed class subsequently passed. Four additional interaction cases failed before their production fixes: LIMIT, OFFSET, and combined slicing observe OPTIONAL multiplicity, and a retained global HAVING operand can belong only to eliminated eligibility bindings. The fourteen-case class had four failures, zero errors in 0.870 seconds, preserved in `initial-evidence.review-set-round2-interactions.txt` from run `20260907T221503.992660Z-3244-ce3ce665`. The independent evaluation review also reproduced cancellation during adaptive OPTIONAL initialization in two cases; its evidence is in `initial-evidence.review-equality-round2-adaptive-optional.txt`.

## Decision Log

Use four warmed isolated workspaces for independent fixes; this retains dependency correctness while avoiding repeated serial builds. Treat root causes as bounded Routine A fixes even though this document coordinates the larger task. Keep compatibility and broad-suite caveats separate from focused behavioral success.

When eligibility correlation is not a subset of the group keys, keep the optimized EXISTS below GROUP rather than rejecting the whole semi-join rewrite. Only emit the group-key-lift proof for the lifted form. Require the finite code/type branches to agree on each retained pattern's graph scope and context term.

Retain positive HAVING and its aggregate/extension for a global group, since an empty global group still yields an aggregate row. Duplicate-insensitive rewrites require repeatable aggregate operands and input evaluation. Optional bindings are redundant only when every observed right-side value is assured by the left, not merely present in its possible output schema.

Express the retained hidden global HAVING positivity check with COUNT(*): the original operand was proved assured on the original input, but EXISTS can remove its binding. Stop optional pruning at Slice, which observes row multiplicity even when its enclosing aggregate ignores duplicates. Track adaptive OPTIONAL inputs throughout delegate initialization and fallback so cancellation can close active or late-returned scans.

## Outcomes & Retrospective

Implementation and validation of the reviewed and follow-up fixes are complete. All fourteen packed regression cases pass their identical post-fix selection (run `20260907T221950.896421Z-3553-83ee7ab3`, 1.177 seconds), saved in `post-evidence.review-set-round2.txt`. Adaptive OPTIONAL's original two cancellation cases also pass.

Final related gates pass 448 distinct cases with zero failures, errors, or skips: packed planner/group/graph/binding safety 115, evaluation/hash/EXISTS/OPTIONAL lifecycle 141, LMDB optimizer/filter pipeline 137, and transaction/storage 55. Evidence is retained in `post-evidence.review-set-round2-final.txt`, `evidence.review-equality-round2-final-green.txt`, `post-evidence.review-bound-final-integration.txt`, and `post-evidence.review-overlay-round2-shutdown.txt`. All four cooperative runners have finished. The final independent read found no further actionable defect in the repaired paths. Full-module suites were not repeated; earlier broad-suite limitations and the known compatibility skip still apply.

The preservation audit confirms all 15 original tracked diff blocks unchanged, 35 of 37 round-start source hashes identical (only the two known shared owned files differ), and the original three staged deletions untouched. Branch, HEAD and merge base match the starting values. Scoped header validation using an unmodified copy of the repository copyright script passes all 14 Java files; log: `review-round2-copyright.log`.

Final serialized scoped Spotless formatting completed successfully in `review-round2-format.log`. Java tokens and import sets are unchanged across formatting for all seven production files. The final audit again confirms all 15 original tracked changes and 35 unrelated/earlier-fix source hashes unchanged; `git diff --check` passes. Full tracked/untracked status is retained in `/tmp/rdf4j-round2-final-status.txt`. All task-owned Maven runs are finished. No staging, commits, pushes, branch changes, or removal of untracked artifacts occurred.

## Interfaces and Dependencies

Prefer existing packed masks, variable metadata, iterator lifecycle interfaces, and transaction ownership. No new dependency or public configuration is planned. Keep tests in the modules that own the production behavior.

Plan created for the user's request to repair the seven new findings, then review and repair further high-confidence defects.

Updated after captured packed and transaction failures to record evidence, fixture correction, and the general grouping remedy.

Updated after seven focused fixes passed to begin the requested follow-up review while related gates finish.
