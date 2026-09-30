# Repair the confirmed optimizer, iterator, and LMDB regressions

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance with
`PLANS.md` at the repository root.

## Purpose / Big Picture

The current branch contains twelve independently confirmed regressions in shared query evaluation and LMDB-specific
planning or persistence code. They can return different query answers for logically identical rows, create invalid
federated joins, lose crash-consistency metadata, leak unbounded persistent statistics state, race during feedback
recording or timeout shutdown, break configuration round-trips, remove standard-store explain estimates, create an
OSGi and JPMS split package, lose bounded ORDER BY top-k execution, and turn MINUS overflow into per-row re-evaluation.

After this work, the supplied reproductions must be stable and specification-correct, every affected configuration or
bundle boundary must round-trip or resolve, and bounded execution paths must remain bounded without changing query
bag semantics. Each behavioral repair starts with an automated test that fails in this checkout before its production
change. The checkout is deliberately dirty; only the files named by a confirmed repair and its focused tests may be
changed, and no existing untracked artifacts may be removed.

## Progress

- [x] (2026-08-30 21:02Z) Inspected branch status, dirty targets, merge base, and mandatory repository instructions.
- [x] (2026-08-30 21:02Z) Ran the mandatory offline root quick clean install; all reactor modules built successfully.
- [x] (2026-08-30 21:05Z) Reproduced `QueryPlanRetrievalTest`: 28 tests, 17 failures, 0 errors, 2 skipped.
- [x] (2026-08-30 21:41Z) Added and observed the FilterIterator adaptive EXISTS and real parser-backed VALUES UNDEF
  regressions; preserved both reports in top-level evidence files.
- [x] (2026-08-30 21:18Z) Added and observed the OPTIONAL, hash compatibility, timeout close, and MINUS overflow
  regressions in isolated evaluation workspaces.
- [x] (2026-08-30 21:37Z) Added and observed the LMDB guarantee, journal lifecycle, feedback synchronization,
  confidence parsing, and ORDER/LIMIT top-k regressions.
- [x] (2026-08-30 21:12Z) Added and observed a model-package ownership test that detects the split package.
- [x] (2026-08-30 23:22Z) Implemented the root-cause repairs, including correlated EXISTS substitution, exact
  once-only MINUS spill probing, bottom-up OPTIONAL behavior in core and FedX, and LMDB journal/sidecar lifecycle
  hardening, without overwriting the pre-existing optimizer work.
- [x] (2026-08-30 23:24Z) Ran the matching focused and grouped selectors and the repository formatter. The post-fix
  root and complete LMDB module suites remain unrun because Maven workspace accumulation exhausted the volume during
  the broad attempt; retained focused gates cover 108 evaluation, 41 MemoryStore, 22 model, 15 FedX-mode, and 126
  LMDB tests with no failures or errors.
- [x] (2026-08-30 23:29Z) Completed the final read-only cross-module audit: no unresolved non-LMDB defect was found,
  old runtime-feedback imports are absent, model and evaluation jars no longer split the package, all new Java files
  have valid headers/signatures, both staged and unstaged diffs pass `git diff --check`, and unrelated state remains.

## Surprises & Discoveries

- Observation: The working tree already modifies `LmdbQueryOptimizerPipeline.java` for snapshot-sensitive predicate
  simplification. The reported ORDER/LIMIT location is otherwise untouched.
  Evidence: `git diff -- core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java`
  shows only constructor arguments around lines 132 and 251.

- Observation: The standard MemoryStore explain regression exactly matches the supplied 17-of-28 failure count.
  Evidence: workspace `plan-explain` reports `Tests run: 28, Failures: 17, Errors: 0, Skipped: 2` in
  `.mvnf/workspaces/plan-explain/build/org.eclipse.rdf4j/rdf4j-sail-memory/6.1.0-SNAPSHOT/surefire-reports/`.

- Observation: Missing estimates and structural plan changes are separate mechanisms. Commit `e55da6ed5e` gated
  estimate conversion on `plannedEstimateUsage` and removed text, DOT, and JSON estimate rendering, while later
  changes moved filter optimization ahead of the standard join optimizer and made the shared greedy join reorderer
  prefer connected factors.
  Evidence: `git show e55da6ed5e` and `git diff origin/develop...HEAD` over the converter, renderer, standard pipeline,
  and `QueryJoinOptimizer`.

- Observation: The LMDB optimizer does not use the shared `QueryJoinOptimizer`; it invokes its packed Cascades
  optimizer after an LMDB-specific normalization list. Therefore LMDB-only optimizer requirements must not silently
  reshape every default-strategy store through the standard pipeline.
  Evidence: `LmdbQueryOptimizerPipeline.requestBoundOptimizerList()` followed by `cascadesOptimizer()`.

- Observation: RDF4J parser rows represent a declared `VALUES` column containing `UNDEF` with a `ListBindingSet`
  whose `getBindingNames()` still includes that column; only `hasBinding(name)` distinguishes the unbound cell.
  Evidence: `BindingSetAssignmentTest` using `new ListBindingSet(List.of("a", "b"), Arrays.asList(null, value))`
  failed before the assured-name implementation switched to `hasBinding` and then passed in workspace
  `binding-assurance`.

- Observation: Removing the branch's globally unsafe `earlyBoundProblemVars` behavior exposed that the first 32
  correlated EXISTS probes had accidentally depended on that same shortcut. The ordinary fallback then returned
  0/60 EXISTS rows and 60/60 NOT EXISTS rows for the OPTIONAL reproduction.
  Evidence: workspace `filter-exists` run `20260830T213858.414923Z-47296-80ea351e` reports two failures after the
  LeftJoin repair and before EXISTS-boundary substitution.

- Observation: The first broad post-fix LMDB attempt reached 143 integration tests but was not a green baseline: two
  packed-costing/cache-identity failures, one unrelated `QuadValueHash` null-pointer error, and 97 skipped tests were
  reported. A later root install then failed before tests with `No space left on device` because the checkout held
  more than one thousand retained Maven workspaces.
  Evidence: the broad Failsafe reports and the retained workspace/root Maven logs; no workspace, report, or unrelated
  artifact was deleted to recover capacity.

- Observation: A final FedX audit found that `prepareLeftJoin` had the same well-designedness bypass as the shared
  evaluator: its subquery hash branch returned before the problem-variable gate, and its regular branch used a
  reversed containment check. Parser-backed tests failed in the default, native, and remote modes before the repair.
  Evidence: `initial-evidence.fedx-leftjoin-well-designed.txt` and workspace `eval-joins` logs.

## Decision Log

- Decision: Treat every supplied item as authorized repair work rather than repeat the already-completed triage.
  Rationale: The report labels each item confirmed and supplies runtime or path evidence; repository rules still
  require an in-repo failing regression before behavior-changing production edits.
  Date/Author: 2026-08-30 / Codex.

- Decision: Use Routine A test-first repair for behavioral, concurrency, I/O, configuration, and persistence changes;
  use the existing failing `QueryPlanRetrievalTest` as the red test for its explain/standard-pipeline repair.
  Rationale: These changes have high behavioral or I/O/concurrency risk and do not satisfy the neutral-refactor gates.
  Date/Author: 2026-08-30 / Codex.

- Decision: Partition work by non-overlapping production files and use named Maven workspaces.
  Rationale: Named workspaces isolate Maven repositories, build trees, reports, temporary paths, and logs while the
  shared dirty source tree requires strict file ownership between agents.
  Date/Author: 2026-08-30 / Codex.

- Decision: Repair MINUS overflow with a bounded in-memory right prefix plus a temporary spill suffix, evaluate the
  right operand exactly once, and probe fixed-size left blocks against an indexed complete right relation.
  Rationale: Exact anti-join semantics with two unbounded inputs require retained external state or repeated scans.
  The spill suffix preserves once-only RHS evaluation and bounded heap use while the binding-name/value candidate
  index avoids the reported `|L| x |R|` re-execution cliff without changing SPARQL domain-overlap compatibility.
  Date/Author: 2026-08-30 / Codex.

- Decision: Move both runtime feedback model types together into a model-owned package and update consumers atomically.
  Rationale: Moving only one type leaves a split root package or makes the model artifact depend on the evaluation
  artifact. Both types are model metadata referenced from `QueryModelNode` and belong in query-algebra-model.
  Date/Author: 2026-08-30 / Codex.

- Decision: Represent correlated EXISTS semantics at the EXISTS boundary by cloning the non-monotone subquery,
  applying `BindingAssignerOptimizer` with the current outer row, compiling that substituted algebra, and evaluating
  it from an empty input. Force these shapes onto the streaming physical path.
  Rationale: SPARQL EXISTS substitutes the current solution into the graph pattern before evaluation. Restoring
  `earlyBoundProblemVars` inside every badly-designed LeftJoin would again change ordinary bottom-up OPTIONAL
  semantics, while independent materialization cannot reconstruct rows exposed by substituted OPTIONAL or MINUS.
  Date/Author: 2026-08-30 / Codex.

- Decision: Keep declared and assured names as independently cached properties of `BindingSetAssignment`.
  Rationale: `setBindingNames` carries the declared VALUES schema even for no-row or all-UNDEF inputs, while an
  assured name must be present according to `hasBinding` in every actual row. Reusing one cache for both contracts
  fabricated correlation bindings.
  Date/Author: 2026-08-30 / Codex.

## Outcomes & Retrospective

All twelve supplied mechanisms are repaired, together with two adjacent races/bypasses discovered while tracing the
same paths: `FilterIteration` close/read coordination and FedX's own well-designed OPTIONAL gates. Every behavioral
repair has preserved pre-fix red evidence and matching post-fix focused evidence. The initial 123-module quick install
was green; post-fix grouped gates are green for 108 evaluation tests, 41 MemoryStore tests, 22 query-model tests, all
15 FedX mode runs (four intentional skips), and 126 LMDB persistence/statistics tests. A second unit-only LMDB gate
covering crash consistency, OFF-mode lifecycle, config round-trip, synchronized feedback, and ORDER/LIMIT ran 167
tests with no failures, errors, or skips. `GenericPlanNodeTest` is also green with 28 tests. The formatter completed
successfully and both staged and unstaged `git diff --check` runs are clean.

The acceptance boundary is deliberately narrower than a repository-wide green claim. A broad LMDB Failsafe attempt
remains red for separately triaged packed-costing/cache-identity failures and an unrelated `QuadValueHash` error, and
the later post-fix root/module attempts were blocked by retained-workspace disk exhaustion. No files were deleted,
staged, committed, or pushed; the unrelated staged deletion and all untracked research artifacts remain untouched.

## Context and Orientation

RDF4J represents SPARQL queries as query-algebra nodes in `core/queryalgebra/model`. The shared evaluator and standard
optimizer pipeline live in `core/queryalgebra/evaluation`; MemoryStore and most default-strategy stores consume that
pipeline. FedX, under `tools/federation`, also uses the shared hash-join iterator APIs. LMDB, under `core/sail/lmdb`, has
its own packed optimizer and native persistence layer.

An assured binding name is a variable guaranteed to be bound in every row. A declared VALUES column can contain
`UNDEF`, so declared and assured names are not interchangeable. A well-designed OPTIONAL is one whose variables do not
escape their safe scope; badly designed OPTIONAL evaluation must obey SPARQL bottom-up compatibility semantics rather
than pre-bind escaping variables. A materialized EXISTS semi-join evaluates a subquery independently and probes a
materialized relation; it is valid only when the subquery is safe under that transformation.

LMDB transactions may commit cached statement records in more than one native transaction when the record cache
overflows. Predicate-guarantee metadata that is used as a rewrite license must become durable no later than the
triples it describes. Frontier mutation rows are a persistent journal used by the statistics service; an in-memory
tail cap does not bound the LMDB database and is not a retention policy.

The two feedback types currently under
`core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/` put the model artifact in the root
package owned by query-algebra-evaluation. OSGi bundles can wire a package to only one exporter and JPMS rejects split
packages, so model-owned types need a distinct model-owned package such as
`org.eclipse.rdf4j.query.algebra.feedback`.

## Plan of Work

First, preserve one failing automated reproduction for each mechanism. FilterIterator tests will exercise more than
the 32-row adaptive probe budget and VALUES rows containing both UNDEF and a concrete value. OPTIONAL tests will cover
the memoized/hash, adaptive overflow, and early-bound badly-designed paths. Hash-join tests will share a variable that
is not assured on both sides and verify the compatibility check remains separate from lookup keys. Timeout tests will
coordinate close and read calls without sleeps. MINUS tests will use an injectable production threshold and count
right-step openings so the assertion measures algorithmic slope rather than elapsed time.

LMDB tests will inject failure between native commits and reopen the store to verify guarantee metadata, inspect or
drive the mutation-journal lifecycle in OFF and enabled modes, coordinate concurrent plan-shadow mutation/persistence,
and export then parse non-default confidence bounds. The ORDER/LIMIT test will prepare a width-preserving projection
and inspect the order evaluation limit so SELECT-star and DISTINCT variants retain bounded top-k. The package test will
inspect built bundle contents or descriptors and fail when the model and evaluation artifacts contain the same Java
package.

Then, implement the smallest general repair for each mechanism. Materialized EXISTS must decline algebra shapes whose
semantics are not monotone under independent materialization. Equality prebinding must use row-level assured bindings,
not VALUES declaration names. Every left-join fast path must be gated by the same well-designedness decision, and the
badly-designed iterator must not pre-bind escaping problem variables. Hash join APIs must carry lookup names and full
compatibility names as separate contracts into FedX. Deferred close/read methods must use a stable local snapshot and a
closed-state guard. MINUS overflow must retain useful work and scan per left block.

LMDB guarantee changes must be flushed in the same native commit as every batch of triples or leave an explicit rebuild
marker that survives a later failure. Frontier journaling must be disabled when no consumer is configured and have a
consumer-owned acknowledged watermark plus compaction/truncation lifecycle when enabled. Feedback map mutation and
epoch/dirty updates must share the existing instance monitor. Config parsing must gather all confidence values and
validate their cross-field invariant after parsing. Standard explain conversion and rendering must remain available
without LMDB provenance metrics, while planner-specific metrics stay additive. Shared optimizer behavior must remain
stable for standard stores unless a new behavior is deliberately characterized. Feedback model imports must move as
one atomic package migration. LMDB must run the standard order/limit transformation or provide an exactly equivalent
packed rewrite for all projection widths and DISTINCT/REDUCED shapes.

Finally, run each exact red selector again, then its owning module suite. Run cross-module tests where a public helper
contract crosses into FedX, the runtime OSGi packaging test for the package move, the MemoryStore explain class, and the
LMDB module suite for persisted-state and planner changes. Serialize formatter and copyright checks after all agents
finish because those operations share source state.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things`. The mandatory initial install
was:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

Focused tests use distinct workspace IDs and retain logs, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace plan-explain \
      QueryPlanRetrievalTest --retain-logs

Each initial report is summarized immediately into a top-level `initial-evidence.<workspace>.txt`. Post-fix runs use
the identical selector and workspace so test scope is comparable. Tests never use Maven `-am` or `-q`.

The planned module commands were:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace evaluation-module \
      core/queryalgebra/evaluation --retain-logs

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace lmdb-module \
      core/sail/lmdb --retain-logs

The broad copyright script was stopped because it recursively traversed retained workspace build trees; every newly
added Java file was instead checked directly for the required copyright, SPDX header, and Codex signature. Formatting
was serialized after test work:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command contains `-q` only because it does not run tests; no test command may contain `-q`.

## Validation and Acceptance

The EXISTS reproduction must return all 60 rows for EXISTS and no rows for NOT EXISTS, independent of row position.
The VALUES reproduction must return no row when UNDEF makes the equality expression an error and the concrete value is
unequal. OPTIONAL results must match bottom-up evaluation for union and non-union RHS shapes across every fast and
overflow path. FedX must reject incompatible shared unassured bindings and must not graft right-only bindings from an
incompatible row. Timeout close races must finish with the query interruption/closed contract and must close delegates
exactly once without NullPointerException. MINUS RHS openings must be bounded by the number of left blocks.

Every committed LMDB triple must be compatible with the reopened predicate-guarantee index after an injected second
commit failure. OFF mode must not append Frontier journal rows; enabled mode must demonstrate a bounded acknowledged
retention lifecycle. Concurrent feedback recording and persistence must complete without map corruption and must not
lose dirty/epoch state. An exported config with minimum 0.2, initial 0.3, maximum 0.4 must parse successfully.

`QueryPlanRetrievalTest` must return to green without deleting its legacy estimate expectations. Built query-algebra
model and evaluation artifacts must not contain the same Java package. LMDB SELECT-star ORDER BY LIMIT plans must pass
a finite limit to `OrderIterator`, including DISTINCT. The final diff must contain only intended production, focused
test, evidence, and this ExecPlan file changes, while all pre-existing tracked and untracked work remains present.

## Idempotence and Recovery

Named Maven workspaces are reusable and do not modify source snapshots. A failed selector can be rerun with the same
workspace after a code change; its first report remains in the top-level evidence file. If offline resolution fails,
rerun the identical selector once with `--no-offline`, then return to offline operation. Never restore, reset, clean,
stash, or delete unexpected files in this checkout. If a target overlaps a pre-existing edit, apply only a narrow hunk
after reviewing the exact diff.

If production code is discovered to have changed before its red test was observed, stop that repair, reverse only the
newly introduced hunk with `apply_patch`, set the living plan back to the reproduction step, and capture the failing
test before reapplying the repair.

## Artifacts and Notes

Initial failing evidence is stored in `initial-evidence.plan-explain.txt`, `initial-evidence.query-explain.txt`,
`initial-evidence.feedback-package.txt`, `initial-evidence.filter-exists.txt`,
`initial-evidence.binding-assignment-undef-row.txt`, `initial-evidence.leftjoin-nested-optional.txt`,
`initial-evidence.eval-joins.txt`, `initial-evidence.fedx-leftjoin-well-designed.txt`,
`initial-evidence.join-metrics-concurrency.txt`, `initial-evidence.join-metrics-followup.txt`,
`initial-evidence.filter-iteration-race.txt`, `initial-evidence.minus-followup.txt`,
`initial-evidence.queryjoin-values-barrier.txt`, `initial-evidence.order-limit-state.txt`,
`initial-evidence.lmdb-storage.txt`, and `initial-evidence.lmdb-followup.txt`. Retained Maven logs live under the
matching `.mvnf/workspaces/*/logs/` directories. The repository root `maven-build.log` contains the initial successful
quick build.

## Interfaces and Dependencies

No new external dependency is planned. Public hash-join helper contracts must distinguish lookup bindings, which form
the hash key, from compatibility bindings, which include every shared variable that can be bound on both rows. Any
constructor compatibility shim must preserve the old complete-compatibility meaning for external callers.

The feedback package migration must end with `RuntimeFeedbackContract` and `RuntimeFeedbackDescriptor` in
`org.eclipse.rdf4j.query.algebra.feedback`, with all model, evaluation, packed optimizer, LMDB descriptor, and test
imports updated. No class in the model artifact may remain under the evaluation artifact's root package.

The Frontier journal lifecycle must have an explicit configuration source, an owner for the consumption watermark,
and a transactionally safe compaction point. A mere in-memory cap, time-based guess, or test-only truncation is not an
acceptable interface.

Revision note (2026-08-30): Created after repository inventory and the first preserved failing test so parallel repairs
share one self-contained safety, test, and acceptance contract. Updated after all original reds were captured, the
EXISTS/OPTIONAL integration run exposed the need for substitution at the correlated subquery boundary, the final FedX
audit exposed its duplicate well-designedness bypass, and focused verification established the disk-constrained
acceptance boundary.
