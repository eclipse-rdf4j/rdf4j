# Make SPARQL query optimization and evaluation preserve observable algebra semantics in every evaluation mode

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It must be maintained in accordance with `.agent/PLANS.md` at the repository root.


## Purpose / Big Picture

RDF4J is a Java framework for storing and querying RDF data with SPARQL. A SPARQL query is parsed into an algebra tree (nodes like `Join`, `LeftJoin`, `Filter`, `Extend`), rewritten by optimizers, and executed by iterators. The SPARQL 1.1 Recommendation defines the *correct result* of a query through that algebra: for example `eval(Join(P1,P2)) = Join(eval(P1), eval(P2))` — both join operands denote independently evaluated multisets of solutions. An implementation may execute differently (the "as-if" rule) only when the observable result is one the algebra permits.

Today RDF4J violates that in several ways. After this change, all of the following hold and can be demonstrated by running the named tests:

    1. In BOTH evaluation modes (STRICT and STANDARD), the optimizer refuses rewrites that change results
       involving volatile expressions (RAND, UUID, STRUUID, BNODE, unknown functions).
    2. SELECT (COUNT(DISTINCT ?u) AS ?c) WHERE { VALUES ?x {1 2} OPTIONAL { BIND(UUID() AS ?u) } }
       returns c=1 (today it returns 2, because the engine re-evaluates the OPTIONAL per row).
    3. SELECT ?x WHERE { BIND(<urn:a> AS ?x) FILTER(?x IN (<urn:a>, <urn:b>)) } returns exactly one row
       (an optimizer rewrite can currently duplicate it).
    4. A query containing a failing non-silent SERVICE fails, even when another part of the query is empty
       or a filter is constant-false (today optimizers can silently discard the SERVICE).
    5. FILTER(sameTerm(?o,<urn:a>) || BOUND(?o)) does not duplicate rows (today a rewrite can).
    6. ORDER BY RAND() sorts without violating Java comparator contracts.

"Evaluation mode" is a per-store setting (`QueryEvaluationMode`, values `STRICT` and `STANDARD`) that is only supposed to relax *value* semantics (extended comparisons/arithmetic), never result multiplicity or identity semantics.

The authoritative, review-approved design is in `/Users/havardottestad/.claude/plans/make-a-plan-to-cuddly-lighthouse.md` (five maintainer review rounds). This ExecPlan restates everything needed to execute it; where detail conflicts, that plan file wins.


## Context and Orientation

Repository: multi-module Maven build. Modules relevant here:

    core/queryalgebra/evaluation   — optimizers, evaluation strategy, iterators, function SPI
    core/queryalgebra/model        — algebra node classes
    core/sail/memory, core/sail/lmdb — stores used by tests
    core/repository/sparql, federation paths — SERVICE (federated query) implementation

Key files (paths relative to repo root, all under core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation unless noted):

    util/QueryEvaluationUtility.java        — repeatability classification ("is it safe to re-evaluate /
                                              move this subtree?"), function whitelist, function pinning
    impl/DefaultEvaluationStrategy.java     — optimize() entry point (~line 338), FunctionCall preparation
                                              and constant folding (~line 1301)
    impl/evaluationsteps/JoinQueryEvaluationStep.java, LeftJoinQueryEvaluationStep.java
                                            — physical join algorithm selection
    iterator/JoinIterator.java, LeftJoinIterator.java, BadlyDesignedLeftJoinIterator.java
                                            — nested-loop joins; they re-evaluate the right operand once per
                                              left row WITH the left row's bindings injected
    iterator/HashJoinIteration.java         — hash join; materializes inputs; no condition parameter
    iterator/OrderIterator.java (+ OrderComparator) — sorting; evaluates order expressions inside compare()
    optimizer/QueryModelNormalizerOptimizer.java, SameTermFilterOptimizer.java,
    optimizer/DisjunctiveConstraintOptimizer.java, FilterInValuesOptimizer.java, FilterOptimizer.java,
    optimizer/OrderLimitOptimizer.java, QueryJoinOptimizer.java — rewrites guarded (or not) by repeatability
    function/Function.java                  — public function SPI (has mustReturnDifferentResult())

Test infrastructure precedents:

    core/queryalgebra/evaluation/src/test/.../impl/StrictEvaluationStrategyTest.java — runs real optimize()
    core/sail/lmdb/src/test/.../LmdbFunctionRepeatabilityTest.java — call-counting canary Function
    core/sail/lmdb/src/test/.../LmdbNativeDifferentialFuzzTest.java — differential result comparison

Build and test commands (house rules — mandatory):

    Root install (run before any test session; ~30 s):
        mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    Run tests (never -am, never -q):
        python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method
        python3 .codex/skills/mvnf/scripts/mvnf.py <module-path>
    Format before finalizing:
        mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    Copyright check: cd scripts && ./checkCopyrightPresent.sh

New Java files need the exact Eclipse RDF4J header (year 2026) plus, directly below it, the line
`// Some portions generated by Claude Code`. Commits are prefixed `GH-0000` (no issue number assigned; noted
in the final handoff). Evidence protocol: before each production change, run the newly-written failing test and
persist a compact report via `python3 scripts/agent-evidence.py ... > initial-evidence.txt` (first run) and
Evidence blocks in the conversation.

IMPORTANT: the working tree already contains UNRELATED uncommitted work — a merge-join change in
`core/queryalgebra/evaluation/.../optimizer/QueryJoinOptimizer.java` and its new test
`QueryJoinOptimizerMergeJoinBoundVarsTest.java`. Do not modify or revert that work; do not include it in
commits for this effort unless it is committed separately first.


## The five defect classes being fixed

A. STANDARD-mode bypass. Commit `fd78b03dd7` made `DefaultEvaluationStrategy.optimize()` set
   `ignoreFunctionRepeatability = getQueryEvaluationMode() != QueryEvaluationMode.STRICT`, wrapping the
   optimizer pipeline in `QueryEvaluationUtility.withFunctionRepeatabilityPolicy(...)`. In STANDARD mode this
   lets optimizers merge, clone, and move volatile expressions (`Or(RAND(),RAND()) -> RAND()` etc.), which
   changes observable results. STANDARD is documented as still SPARQL-compliant, so this is a bug to remove.

B. Independent-operand violations (pre-existing, both modes). The algebra evaluates each Join/LeftJoin operand
   once as an independent multiset. RDF4J's nested-loop iterators instead re-evaluate the right operand per
   left row with the left bindings injected. Consequences: fresh UUID per outer row inside OPTIONAL (wrong);
   `FilterInValuesOptimizer` builds `Join(VALUES, arg.clone())` whose bind join injects the VALUES binding
   into the cloned arg — since `Extend` (the algebra node for BIND) is undefined for a pre-bound target
   variable and RDF4J's Extension overwrites it, a deterministic query can return duplicated rows; the
   federated SERVICE implementation splits one logical Invocation into per-block HTTP requests (block size
   15), so >15 outer rows over a remote BIND(UUID()) observe two UUIDs where the spec allows one Invocation
   only; `SilentIteration` can swallow a failure after rows already streamed (partial result impossible for
   one Invocation); the fixed helper variable name `__rowIdx` can collide with a user variable.

C. Volatile ORDER BY. `OrderComparator` evaluates non-variable order expressions inside `compare()` — a
   volatile key gives the same row different keys during one sort (comparator contract violation) — and its
   outer catch turns any `QueryEvaluationException` into "equal", swallowing fatal errors.

D. `DisjunctiveConstraintOptimizer` splits `Filter(L||R, arg)` into `Union(Filter(L,clone), Filter(R,clone))`
   without proving L and R disjoint. `FILTER(sameTerm(?o,<urn:a>) || BOUND(?o))` over a matching row emits the
   row from BOTH branches — a deterministic multiset bug, independent of volatiles.

E. Error-suppressing eliminations. Query-failure-versus-empty-result is observable. Optimizer simplifications
   (`Join(EmptySet,R)->EmptySet`, `LeftJoin` annihilation, `Filter(false)->EmptySet`, Intersection
   annihilation, generic removal of unary operators over an EmptySet child, `SameTermFilterOptimizer`'s
   whole-subtree replacement, `FilterInValuesOptimizer`'s merge path) and physical short-circuits (hash join's
   `leftIter.hasNext() && rightIter.hasNext()`, LIMIT-style early completion, ASK) can discard a subtree
   containing a failing non-silent SERVICE, converting a required query error into an empty result.


## Core contracts (the design's invariants)

1. Algebra-result contract: every physical strategy must produce an outcome permitted by the SPARQL algebra
   and applicable RDF4J extension contracts. No hidden invocation-count guarantee — where SPARQL permits
   several outcomes (REDUCED multiplicity, unspecified orderings, RAND repeats, volatile expressions over
   duplicate multiset occurrences) any permitted outcome is acceptable and tests must not over-constrain.
2. Recompute contract: a tuple operand may be re-evaluated only when repetition under the same legitimate
   input is guaranteed observationally equivalent, or the variation is proven unobservable.
3. Binding-injection contract: bindings from another operand may be pushed into a tuple operand only when that
   is proven equivalent to independent evaluation plus compatible-mapping join. Injection safety is JOINT over
   the injected variable set, not per-variable (`FILTER(!BOUND(?x) || !BOUND(?y))` is safe for {?x}, safe for
   {?y}, unsafe for {?x,?y}).
4. Error contract: three-valued `ErrorEffect { NONE, ROW_LOCAL, QUERY_FATAL }`. BIND/FILTER/ORDER expression
   errors are row-local. Non-silent SERVICE (and declared fatal-capable extensions) are query-fatal. A subtree
   may be discarded unevaluated only when `canDiscardWithoutEvaluation(subtree, ctx)` holds. Applies when a
   query completes and reports a result; explicit caller cancellation is exempt.
5. LeftJoin semantics: implemented/tested as `Filter(condition, Join(Ω1,Ω2)) ∪ Diff(Ω1,Ω2,condition)` (the
   errata-reliable formulation).

Performance principle (supersedes any implication of extra strictness): best possible performance while
following the spec. Every barrier is a default mode-independent barrier removable by a local proof of
observational equivalence; guards survive only by citing the observable violation they prevent; conservative
classification is a staging state with tracked refinement, never the advertised end state.


## Plan of Work (milestones)

Milestone S1a — Remove the STANDARD bypass. Test-first: rewrite the six STANDARD-relaxation tests in
`StrictEvaluationStrategyTest` (lines ~130-235) into `standardEvaluationKeeps...Barrier` forms mirroring the
STRICT keeper at line ~180; run them, record failing evidence; then surgically delete the ignore-policy from
`DefaultEvaluationStrategy.optimize()` and `QueryEvaluationUtility` (two fields, `withFunctionRepeatabilityPolicy`,
the boolean `pinFunctions` overloads, metadata-key reads/writes, the collector's `ignoreFunctionRepeatabilityFor`
paths, `FunctionPinningVisitor` extras); delete the three tests that exercise the removed mechanism
(`StrictEvaluationStrategyTest.queryModeRetaggingOverridesClonedFunctionMetadata`,
`QueryEvaluationUtilityTest.outerStrictConfigurationOverridesStaleStandardFunctionMetadata`,
`LmdbSketchJoinOptimizerTest.standardModeLetsLmdbPlanThroughUnmarkedFunctionFilter`). Keep: one-arg
`pinFunctions`, `isRepeatableWithinPreparation`, `PinnedFunctionCall`, `QuerySafetySnapshot`, the whitelist,
the QueryModelNode metadata API (other consumers exist). LMDB needs no production edit (it inherits
`optimize()`). Acceptance: the rewritten tests pass; `mvnf.py StrictEvaluationStrategyTest`,
`QueryEvaluationUtilityTest`, and `--module core/sail/lmdb LmdbSketchJoinOptimizerTest` green; a mode-parity
test asserts per-case that the forbidden transformation did NOT occur in either mode while MD5/NOW positive
controls DID collapse in both (no whole-tree cross-mode equality).

Milestone S1b — Disable `DisjunctiveConstraintOptimizer`. Test-first: a multiset test showing
`FILTER(sameTerm(?o,<urn:a>) || BOUND(?o))` duplication through the optimizer. Then disable the rewrite
entirely (the current `isRepeatable(arg)` guard is NOT accepted as duplication safety). Record whether the bug
reproduces on `develop` for an upstream report. Re-enabling (restricted, proof-carrying) is Stage 2+ work.

Milestone S1c — Global fatal-error gates. Introduce `ErrorEffect` and `canDiscardWithoutEvaluation` in the
evaluation module; gate every subtree-discarding site (normalizer annihilations incl. Intersection and generic
unary-over-EmptySet, `SameTermFilterOptimizer`, FilterInValues merge path, hash-join empty-left short-circuit,
early completion). Witness queries listed in "The five defect classes" item E, plus ASK and UNION+LIMIT 1
variants; SILENT variants recover.

Milestone S2 — Independent operands. (a) `InjectionSafety` joint predicate + `BindingFacts`; (b)
`TupleSafetyProfile` (replayStableWithSameInput, sequenceStable, injectionSafety, safeToDuplicate — defined by
observable equivalence, not call counts —, errorEffect) computed as phase-local snapshots, never carried
through clone(); (c) Join/LeftJoin fix with test-first path discovery (UUID trio, STRUUID, BNODE forms, the
IRI FilterInValues witness, LeftJoin Diff cases incl. condition-error rows, BadlyDesignedLeftJoinIterator,
external initial bindings), physical strategy order = bind join (when replay-stable ∧ injection-safe ∧ fatal-
handled) > side reversal > hash/merge > materialize-and-replay (spillable `createBindingSetQueue`, snapshot
every stored mapping, close resources on every exit path) > exact proofs; (d) SERVICE: pushdown ≠ partitioning
predicates, ALL fallback paths (incl. `evaluateInternalFallback`) under the partition contract, SILENT
failure-atomicity via buffered exposure, fresh generated row-correlation variable.

Milestone S3 — ORDER BY stabilization. Per-occurrence lazy single-flight key slots (atomic state machine),
row-local sentinel for `ValueExprEvaluationException` only, `QueryEvaluationException` propagates; key state
survives spill/merge (serialize occurrence id + keys + sentinels, never evaluators); deterministic path
untouched; forced-spill and parallel-sort-sized tests.

Milestone S4 — Guard re-audit for performance. Per-guard local proof + tests + benchmark before/after:
FilterInValues (needs injection-safety authorization), join-reordering barriers (independent-operand +
multiplicity + fatal exposure preserved), filter sibling/optional guards, OrderLimitOptimizer (after S3),
aggregate/REDUCED/Slice refinement on benchmark evidence.

Milestone S5 — Function determinism SPI. `Function.Determinism` with DETERMINISTIC (immutable w.r.t.
query/tx/clock/registry/TripleSource state) and VOLATILE (default); QUERY_STABLE deferred pending a
context-aware SPI decision; consumer predicates `isStableWithinQuery` vs `isSafeForPlanConstantFolding`
(QUERY_STABLE must never plan-fold); `mustReturnDifferentResult()` dominates; pin implementation + declared
classification at prepare; probe harness is test tooling only ("passing does not establish determinism").

Deferred (recorded, not built): `SharedTupleExpr` materialize-once/consume-N node — preconditions: canonical
frozen producer, query-context-scoped cache lifetime, known consumer count, cancellation+spill design,
native-compiler fallback, a proven-correct consumer with benchmark value.


## Concrete Steps (current milestone: S1a)

1. Edit `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/StrictEvaluationStrategyTest.java`:
   rewrite the six `nonStrictEvaluationOptimizes*` tests to `standardEvaluationKeeps*Barrier` (STANDARD mode
   set, same tree construction, assertion flipped to "barrier held" — mirror lines 180-189).
2. Run `python3 .codex/skills/mvnf/scripts/mvnf.py StrictEvaluationStrategyTest --retain-logs`; expect exactly
   those six to fail; persist `initial-evidence.txt` via `python3 scripts/agent-evidence.py ...`.
3. Edit `DefaultEvaluationStrategy.optimize()` to unconditional `pinFunctions(expr)` + plain loop; strip the
   ignore-policy plumbing from `QueryEvaluationUtility` as listed in Milestone S1a; delete the three
   stale tests.
4. Re-run the same selection green; then `mvnf.py QueryEvaluationUtilityTest`,
   `mvnf.py --module core/sail/lmdb LmdbSketchJoinOptimizerTest`, `mvnf.py LmdbFunctionRepeatabilityTest`,
   `mvnf.py LmdbNativeExpressionRepeatabilityTest`.
5. Add `QueryEvaluationModeOptimizerParityTest` (same package): parameterized volatile-construct × guarded-
   rewrite matrix, per-case forbidden-shape + positive-control assertions in both modes.
6. Format + copyright check; commit `GH-0000 Enforce function repeatability barriers in all query evaluation modes`.


## Validation and Acceptance

After S1a: `SELECT` over `Filter(Or(RAND,RAND))` optimized under STANDARD keeps the `Or` (run
`StrictEvaluationStrategyTest#standardEvaluationKeepsNonRepeatableFunctionBarrier`); after S1b the
sameTerm||BOUND query returns one row; after S1c the SERVICE witnesses throw `QueryEvaluationException`; after
S2 the UUID-trio counts are 1/1/2 on memory and LMDB stores in both modes; after S3 `ORDER BY RAND()` over
five VALUES rows returns all five exactly once with no comparator exception. Module sweeps
(`core/queryalgebra/evaluation`, `core/sail/memory`, `core/sail/lmdb`) green per stage; W3C compliance
spot-runs after S1c/S2/S3; benchmarks (scripts/run-single-benchmark.sh, jmh-benchmark-compare) after S1a, S2,
S3 and each S4 removal — requirement: no regression for volatile-free queries.


## Idempotence and Recovery

Every step is re-runnable: tests are additive; production edits are deletions/replacements checked by the same
test selections. If a step fails midway, re-run the failing `mvnf.py` selection; reports live under
`<module>/target/surefire-reports`. If offline resolution fails, re-run the exact Maven command once without
`-o`, then return offline. Commits happen only at green milestones, so `git status`/`git diff` always shows a
coherent in-progress state; the unrelated `QueryJoinOptimizer.java` diff must stay untouched.


## Interfaces and Dependencies

No new external dependencies. New internal APIs (later stages): `ErrorEffect`, `canDiscardWithoutEvaluation`,
`InjectionSafety`, `TupleSafetyProfile`, `MaterializedReplayJoinIterator`, service pushdown/partition
predicates, `Function.Determinism` (Stage 5, public SPI, default-method additive). Existing consumers that must
keep working unchanged: `FilterSelectivityKeys` / `SketchBasedJoinEstimator` / `LmdbNativeExplain` (all use the
QueryModelNode metadata API being retained), the LMDB native compilers (`isRepeatableWithinPreparation`).


## Progress

- [x] 2026-07-18T07:21Z Root `-Pquick` clean install green (29.7 s).
- [x] 2026-07-18T07:25Z ExecPlan authored (this file).
- [x] 2026-07-18T07:25Z S1a: six STANDARD tests rewritten to barrier form; failing run recorded
      (22 tests, 6 failures — exactly the six new tests; `initial-evidence.txt`).
- [x] 2026-07-18T07:27Z S1a: production removal applied (reverse-apply of `fd78b03dd7` limited to the two
      production files; zero residual references); three stale tests + three orphaned imports deleted.
      Post-change green: StrictEvaluationStrategyTest 21/21, QueryEvaluationUtilityTest 24/24,
      LmdbSketchJoinOptimizerTest 43/43, LmdbFunctionRepeatabilityTest 1/1,
      LmdbNativeExpressionRepeatabilityTest 3/3.
- [x] 2026-07-18T07:33Z S1a: `QueryEvaluationModeOptimizerParityTest` added — 13 scenarios × 2 modes = 26
      green (barriers for RAND/unknown/BNODE/Sample/custom-aggregate/TupleFunctionCall/Service across
      Or/And-collapse, join-over-union, union factor-out, SameTerm split, order hoist; MD5 + NOW positive
      controls fire in both modes).
- [ ] S1a: module sweep green; committed.
- [x] 2026-07-18T07:45Z S1b: witnesses failing pre-fix — unit level (`doesNotSplitNonDisjointDisjunction`
      showed the Union split) and end-to-end (`MemoryDisjunctiveFilterMultisetTest`: expected 1 row, got 2
      through the full memory-store pipeline). Fix: `DisjunctiveConstraintOptimizer` made a documented no-op
      and excluded from `StandardQueryOptimizerPipeline` + `LmdbQueryOptimizerPipeline`. Post-fix green:
      DisjunctiveConstraintOptimizerTest 5/5, MemoryDisjunctiveFilterMultisetTest 2/2.
- [x] 2026-07-18T08:05Z S1c: nine witnesses in `MemoryFatalServiceErrorPreservationTest` (throwing
      FederatedService mock modeling real vectored behavior — no request for an empty binding stream, SILENT
      handled in evaluate per the vectored contract). Pre-fix: 5 failures (constant-false filter, empty join
      operand, impossible sameTerm, OPTIONAL constant-false, hash-join empty left); UNION+LIMIT and ASK
      witnesses already passed (UnionQueryEvaluationStep constructs both branch iterations eagerly — kept as
      regression pins against future lazy unions). Fix: `mayRaiseQueryFatalError`/`canDiscardWithoutEvaluation`
      classifier in QueryEvaluationUtility (Service !silent fatal; unknown TupleExpr conservative-fatal;
      expression errors row-local); gates in QueryModelNormalizerOptimizer (Join/LeftJoin/Difference/
      Intersection annihilation, LeftJoin+Filter constant-false, non-silent Service(EmptySet) unary collapse),
      SameTermFilterOptimizer, FilterInValuesOptimizer merge path; eval-time: LeftJoin PreFilter only for
      discardable rights, subquery-hash + inner hash/nested-loop wrapped with `withGuaranteedRightEvaluation`
      (drains the right operand once when the join completes without opening it; caller close() exempt),
      specialized join fast paths gated to discardable rights, ServiceJoinIterator performs one non-silent
      Invocation on empty left. Post-fix 9/9 green. Module sweeps green: evaluation 891, memory 804,
      lmdb 1905, model 48. W3C SPARQL 1.1 compliance (memory) 176/176 after the NAryValueOperator parent fix
      (see Surprises).
- [x] 2026-07-18T09:10Z S2 (part 1): witnesses in `MemoryIndependentOperandSemanticsTest` — pre-fix the
      UUID/STRUUID/BNODE-in-OPTIONAL trio failed (2 distinct values instead of 1: per-left-row re-evaluation);
      nested-group UUID passed already (scope change → hash join; kept as pin); bare-BIND controls passed.
      Fix: (2a) `QueryEvaluationUtility.permitsBindingInjection` — JOINT injection-safety analyzer
      (conservative: positive pattern shapes only; rejects expression reads of injected names, Extend-target
      collisions, unknown operators; `BindingInjectionSafetyTest` 5/5 incl. the two-var BOUND joint
      counterexample); (2c) `LeftJoinQueryEvaluationStep.supply` routes rights that are not
      (replay-stable ∧ injection-safe) to the new `MaterializedReplayLeftJoinIterator` — right operand
      evaluated exactly once with join-entry bindings, snapshotted, replayed per left row with
      compatible-mapping merge and `Filter(Join) ∪ Diff` condition semantics (condition error = not satisfied,
      row-local); right materialized BEFORE left is consumed (fatal errors surface on empty left). All 7
      witnesses green. NOTE: buffer is in-memory ArrayList — parity with HashJoinIteration's default cache;
      genuine spill = follow-up via the same hook pattern.
- [ ] S2 (part 1): module sweeps + compliance green; committed.
- [ ] S2 (part 2): inner-Join routing for unsafe non-scope-change rights; B2 FilterInValues execution-level
      witness under forced nested loop; SERVICE pushdown≠partitioning + SILENT failure-atomicity + fresh
      row-correlation variable (2d).
- [ ] S3: ORDER BY stabilization.
- [ ] S4: guard re-audit items (individually benchmarked).
- [ ] S5: determinism SPI.


## Surprises & Discoveries

- The nested-loop right-operand re-evaluation (defect B) predates this branch and exists upstream; the UUID
  OPTIONAL witness is expected to fail on `develop` too. To be verified when writing S2 tests.
- `MapDb3CollectionFactory.createList()` does not spill (delegates to in-memory); only the queue-based
  binding-set structures spill. Discovered during plan review; S2c uses `createBindingSetQueue`.
- The `DisjunctiveConstraintOptimizer` disjointness bug (defect D) exists verbatim on `develop`
  (`git show develop:...` shows the same unguarded `containsSameTerm` + clone rewrite) — candidate for an
  upstream issue/report. End-to-end proof on this branch: `SELECT ?o WHERE { VALUES ?o { <urn:test:a> }
  FILTER(sameTerm(?o, <urn:test:a>) || BOUND(?o)) }` returned 2 rows instead of 1.
- Two additional consumers invoked the disjunctive rewrite outside the standard pipelines and are silently
  fixed by the no-op: `LuceneSailConnection` (direct `new DisjunctiveConstraintOptimizer().optimize(...)` call,
  line ~454) and `FederationEvaluationStrategy` (references the
  `StandardQueryOptimizerPipeline.DISJUNCTIVE_CONSTRAINT_OPTIMIZER` constant in its own optimizer list) —
  which is why the public constant was retained rather than deleted.
- Stage-1 benchmark gate (SOCIAL_MEDIA q8, local, ThemeQueryBenchmark with the user's shortened 2s
  iterations): my tree ≈609–641 ms; worktree at `bfc4cad70d` (before ANY of this effort's commits)
  ≈534–541 ms — overlapping error bars, so **no demonstrated Stage-1 regression**, but BOTH are ~8× the
  2026-07-16 local baseline (67–72 ms). The regression window is `e8c3227158..bfc4cad70d` (the user's
  "merge join and seeking" + the STANDARD-relaxation commit; the benchmark store runs default STRICT, so the
  relaxation commit is an unlikely cause). RESOLVED attribution: `e8c3227158` (before BOTH 07-17 commits) also
  measures ≈603 ms — the slowdown predates the merge-join commit too and sits in the user's own
  `7b837882bb..e8c3227158` window (includes the develop merge) or is benchmark-methodology drift (the
  uncommitted ThemeQueryBenchmark tweak shortens warmups 6s→2s; 07-16 numbers used the longer warmups). The
  native merge-join sysprops do NOT restore performance. **Stage-1 gate satisfied: this effort's commits
  measure within noise of all three pre-work points.** Chip task_3b20e1e6 spawned for the user to bisect with
  original warmup settings.
- The post-S1c W3C compliance spot-run (MemorySPARQL11QueryComplianceTest) surfaced a pre-existing model bug,
  unrelated to this effort's changes and identical on `develop`: `NAryValueOperator.replaceChildNode` (used by
  `Coalesce`) swapped the child in its argument list WITHOUT setting the replacement's parent reference —
  every sibling implementation delegates to parent-setting setters. Function pinning replaces a whitelisted
  cast FunctionCall under COALESCE via `replaceWith`, leaving the pinned node with a null parent and tripping
  the assertions-only ParentReferenceChecker. One-line root fix + `NAryValueOperatorTest`; compliance then
  176/176. Bonus finding recorded: `NAryValueOperator.setArguments` stores the caller's list without copying,
  so an immutable argument list would make `replaceChildNode` throw — not fixed (aliasing change out of scope).
- BNODE("label") locality is keyed on BindingSet OBJECT IDENTITY in
  `QueryEvaluationContext.Minimal.getOrCreateBNode`. Chained `BIND(BNODE("k"))... BIND(BNODE("k"))...`
  therefore yields distinct bnodes (each Extend produces a new extended mapping object) — but that is
  DEFENSIBLE: successive Extends produce successive solution mappings, and SPARQL does not settle whether they
  count as "one solution mapping" for BNODE's label scope. The original witness over-claimed and was reshaped
  to the unambiguous form (two SELECT expressions over one row), which RDF4J already satisfies. Recorded per
  the algebra-result contract: do not test outcomes the spec does not distinguish.


## Decision Log

- 2026-07-17 (maintainer): repeatability barriers must apply in STANDARD as well as STRICT; recovery of
  optimization for unknown functions goes through a declared determinism SPI and proof-carrying guard
  removals, not through mode relaxation. Rationale: STANDARD is documented as spec-compliant.
- 2026-07-17 (maintainer): runtime determinism probing rejected as optimizer input (unsound, side-effecting);
  retained only as CI validation of declarations.
- 2026-07-17 (maintainer): value-level replay of function outputs rejected — invocation-order identity cannot
  be guaranteed (runtime-adaptive join algorithm selection, early termination, parallel consumption);
  tuple-level materialization dominates. SharedTupleExpr itself deferred until a proven consumer exists.
- 2026-07-18 (maintainer): observable-behavior contract replaces any evaluation-count invariant; SPARQL does
  not expose call counts. Tests must not require independent draws for duplicate multiset occurrences.
- 2026-07-18 (maintainer): injection safety is joint over the injected set (BOUND counterexample); fatal-error
  preservation is a first-class contract; SERVICE pushdown is distinct from partitioning (partitioning default
  false, all fallback paths included); SERVICE SILENT is failure-atomic (buffer before exposure); ORDER keys
  are single-flight and spill-persistent; DisjunctiveConstraintOptimizer is disabled outright in Stage 1.
- 2026-07-18 (implementation): ExecPlan stored at repo root as `EXECPLAN-sparql-correctness.md`; approved plan
  file remains authoritative for design detail.
- 2026-07-18 (implementation, S1b): disabled the disjunctive rewrite by making the class a documented no-op AND
  excluding it from both pipelines, rather than deleting the class: `FederationEvaluationStrategy` references
  the public pipeline constant and `LuceneSailConnection` instantiates the optimizer directly, so the no-op
  fixes those callers without any API break; the pipeline exclusion avoids a wasted per-query tree walk.
- 2026-07-18 (implementation, S1c): shipped the boolean `mayRaiseQueryFatalError`/`canDiscardWithoutEvaluation`
  pair instead of the full `ErrorEffect` enum — the discard gates only need the fatal/not-fatal distinction;
  the ROW_LOCAL tier is introduced in Stage 3 where the ORDER sentinel logic actually consumes it. Specialized
  join fast paths (merge, bound-statement-pattern lefts) are made ineligible for fatal-capable rights (falling
  through to the guarded default) rather than individually wrapped — simpler and those paths are BGP-shaped in
  practice. The empty-left Service invocation uses `fs.select` with an empty binding set (one logical
  Invocation, result drained for its error effect only).


## Outcomes & Retrospective

(To be written at milestone completions.)


## Artifacts and Notes

- Approved design: `/Users/havardottestad/.claude/plans/make-a-plan-to-cuddly-lighthouse.md`
- Build log: `maven-build.log` (repo root); evidence: `initial-evidence.txt` (repo root, produced in S1a).
- Unrelated in-flight work to preserve: `QueryJoinOptimizer.java` diff + `QueryJoinOptimizerMergeJoinBoundVarsTest.java`.
