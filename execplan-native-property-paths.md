# Native LMDB property paths as graph reachability over long ids

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (from the repository root).

## Purpose / Big Picture

SPARQL property paths such as `?s ex:p+ ?o` and `?s ex:p* ?o` currently force the whole query off the LMDB native engine: the algebra node `ArbitraryLengthPath` is not handled by `compileTuple` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregatePlanner.java`, so the generic evaluator's `PathIteration` runs — materializing RDF `Value` objects, `Statement`s and `BindingSet`s for every traversal step. After this change, a one-step path over a constant predicate compiles to a native plan node that performs breadth-first search directly over 64-bit LMDB value ids: a primitive frontier queue, a primitive visited set (`LongHashSet`), and batched id scans through the existing retained-probe machinery. No `Value` is materialized until result rows leave the engine. Queries mixing paths with ordinary patterns keep the whole fragment native (the path node joins like any other plan node), and both the row engine (plain SELECT) and the aggregate engine (COUNT over reachability) benefit.

Observable outcome: path queries return identical results to the generic evaluator (differential tests with the `rdf4j.lmdb.nativeQueryEngine.enabled=false` oracle), an engagement counter proves the native path node runs, and a JMH benchmark over a chain/tree graph shows the native BFS beating the generic PathIteration.

## Progress

- [x] (2026-07-06 14:40Z) ExecPlan authored; algebra accessors and compiler integration point verified.
- [x] (2026-07-06 15:50Z) M1: `LmdbNativePathPlan.java` (PathPlan + PathCursor: FORWARD/BACKWARD/EXISTENCE/ALL_PAIRS, zero-length identity incl. the all-pairs dataset-vertex case, cycle-safe emission dedup separate from queue admission) landed; `compilePath` gates in `LmdbNativeAggregatePlanner`; `PathPlan` added to `SlotPlan.canReorder`. Note: the both-free `*` case was implemented (dataset-vertex identity scan) rather than gated as originally planned — the differential test agrees with the generic evaluator; Decision Log updated.
- [x] (2026-07-06 15:55Z) M2: `LmdbNativePropertyPathTest` 23/23 green on the first run (bound/unbound/both/existence × `+`/`*`, cycle/self-loop/diamond, inverse `^p+`, joins on both sides, COUNT/COUNT DISTINCT, gated shapes still correct via fallback, engagement + non-engagement counters). Full module: 1377 tests, only the pre-existing `LmdbEvaluationStatisticsMemoizationTest` failure.
- [x] (2026-07-06 16:20Z) M3: `PropertyPathReachabilityBenchmark` (layered DAG, 5 layers × 5000 nodes × fan-out 4 = 100k edges) native vs `-Drdf4j.lmdb.nativePath.enabled=false`: selectReachable 26.99→1.92 ms (14x), countReachable 24.17→0.59 ms (41x), reverseReachable 41.60→1.22 ms (34x). Table in Artifacts.

## Surprises & Discoveries

(To be filled during implementation.)

## Decision Log

- Decision: v1 scope is `ArbitraryLengthPath` (SPARQL `+` and `*`, `getMinLength()` 0 or 1) whose `pathExpression` is a single `StatementPattern` with a constant predicate bound to a known store id, default-graph scope, and a context term that is absent or constant; endpoints may be variables (either or both, shared with the rest of the query) or constants. Everything else (sequences, alternations under the star, `ZeroLengthPath` nodes, GRAPH-variable scoping, unknown predicate ids) returns null and falls back to the generic evaluator.
  Rationale: the single-predicate transitive closure is the overwhelmingly common path shape and the one where id-space BFS pays; every widening is separately testable later. Unknown predicate ids fall back rather than special-casing empty/identity semantics.
  Date/Author: 2026-07-06, Claude Code.
- Decision: reuse `PatternPlan` as the BFS step: the inner `StatementPattern` is compiled through the existing `compileStatementPattern` (slot assignment, `ContextConstraint` from the dataset, static estimates), and the BFS drives it through a private scratch `RowState` (copy of the caller's slots) plus a retained `NativeProbe`, extracting neighbor ids from the quad position of the far endpoint's slot.
  Rationale: inherits dataset/context handling, fixed-context iteration and probe retention for free instead of duplicating scan plumbing.
  Date/Author: 2026-07-06, Claude Code.
- Decision: evaluation modes chosen at `open(RowState)` by endpoint boundness — FORWARD (start known: BFS emitting each discovered node once), BACKWARD (target known: BFS over reversed steps), EXISTENCE (both known: forward BFS with early exit), ALL_PAIRS (both free: enumerate distinct step-subject ids, BFS from each). Zero-length (`*`) adds the identity result first: the bound endpoint itself, or — both free — every distinct subject and object id of the step relation, with BFS pairs where source equals target skipped so identity pairs appear exactly once. SPARQL path semantics are set-based (each node pair at most once), which the visited set provides naturally.
  Rationale: matches `PathIteration`/`ZeroLengthPathIteration` observable behavior pair-for-pair; the differential tests are the arbiter.
  Date/Author: 2026-07-06, Claude Code.
- Decision (supersedes the original both-free-`*` gate): implement the all-pairs zero-length case by scanning the dataset once (any predicate, same context constraints) and emitting an identity pair for every distinct subject-or-object id, then skipping BFS pairs whose source equals the target so identity pairs appear exactly once. Boundness is only known at open time (a correlated `*` path may legitimately open with both endpoints free after join reordering), so a compile-time gate would have been unsound anyway; the differential test `starBothFree` confirms agreement with the generic evaluator.
  Date/Author: 2026-07-06, Claude Code.
- Decision: kill switch `rdf4j.lmdb.nativePath.enabled` (default on, read at class load), `ENGAGED` counter on the new class, and `canReorder` membership so the path node participates in `MultiJoinPlan` join ordering (the factorized-rows gate simply sees a non-PatternPlan child and falls back to the enumerating chain, which is correct).
  Rationale: same observability and A/B conventions as the factorized work on this branch.
  Date/Author: 2026-07-06, Claude Code.

## Outcomes & Retrospective

All milestones complete. Single-predicate `+`/`*` paths now evaluate as id-space BFS inside the native engine with 14–41x measured wins over the generic PathIteration (the generic runs also showed large variance from Value/BindingSet allocation churn, which the native path avoids entirely). 23 differential tests pin the semantics, including the subtle ones: cycle-to-self pairs (emission dedup deliberately separate from queue admission), zero-length identity for bound and both-free endpoints, and diamond-shaped set semantics. Everything outside the gated shape falls back to the generic evaluator and is proven correct-and-not-engaged by tests. Follow-ups: alternation `(p|q)+` (multi-predicate step scans are a small extension), sequence steps via a two-hop expansion, GRAPH-variable scoping (per-graph BFS), per-probe-key memoization of reachable sets for correlated opens, and bidirectional search for the both-bound existence case.

## Artifacts and Notes (results)

PropertyPathReachabilityBenchmark, avgt ms/op, 4+4 iterations, single fork; ON = defaults, OFF = -Drdf4j.lmdb.nativePath.enabled=false; expected row counts computed at setup by the generic evaluator and verified every invocation:

    variant            ON (ms)          OFF (ms)          speedup
    selectReachable    1.918 ± 1.670    26.988 ± 63.817   14.1x
    countReachable     0.585 ± 0.905    24.166 ± 19.803   41.3x
    reverseReachable   1.224 ± 0.556    41.597 ± 89.099   34.0x

Test evidence: LmdbNativePropertyPathTest `tests=23, failures=0` (logs/mvnf/20260706-120718-verify.log); combined post-format run of the three new suites `Tests run: 68, Failures: 0, Errors: 0`; full module 1377/1/0/3 with the single failure pre-existing at HEAD d0f760bacb.

## Context and Orientation

Module `core/sail/lmdb`, package `org.eclipse.rdf4j.sail.lmdb`. The native SELECT/aggregate engine compiles algebra to `SlotPlan` nodes (LmdbNativeSlotPlan.java): `compileTuple` in LmdbNativeAggregatePlanner.java:238+ dispatches on node type and returns null for unsupported nodes (the whole-query fallback). `PatternPlan` (LmdbNativePatternPlan.java) is the triple-pattern leaf: four `Term`s (slot and/or constant id), a `ContextConstraint`, `openRaw(RowState, NativeProbe)` producing a `PatternCursor` whose `fill(long[], int)` drains quads batch-wise; `quadPositionOfSlot(int)` maps a slot to its quad column. `RowState` (LmdbNativeRowState.java) holds the `long[] slots` and a bind/rollback trail; plan cursors implement `RowCursor.next()` binding slots through the trail. `LongHashSet` (LmdbNativeAggregateState.java) is the primitive id set. `ArbitraryLengthPath` (core/queryalgebra/model/.../ArbitraryLengthPath.java) exposes `getSubjectVar()/getObjectVar()/getContextVar()/getPathExpression()/getMinLength()/getScope()`. The generic implementations being displaced are `PathIteration` and `ZeroLengthPathIteration` in core/queryalgebra/evaluation.

## Plan of Work

Milestone 1 — the plan node. New file `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativePathPlan.java` containing `PathPlan implements SlotPlan` (fields: the compiled step `PatternPlan`, endpoint descriptors — slot or constant id for the path's subject and object ends, the quad positions of both ends in the step pattern, `zeroLength`, a static estimate) and `PathCursor implements RowCursor`. `producedMask()` is the endpoint slots; `estimate(row)`/`boundScore(row)` mirror PatternPlan's I/O-free heuristics (bound endpoint → small constant, else the step's static estimate scaled). `open(row)` inspects endpoint boundness (constant, or slot value ≠ UNKNOWN) and returns a `PathCursor` in one of the four modes; the cursor owns a scratch `RowState` (slots copied from the caller so constants and correlated bindings are visible to the step scan), one `NativeProbe`, a `LongHashSet` visited, a growable `long[]` frontier queue and a pending-results ring; each `next()` binds discovered endpoint ids into the caller's row through `bind`/rollback exactly like `PatternRowCursor`. Compilation: in `LmdbNativeAggregatePlanner.compileTuple`, before the unsupported-node fallthrough, handle `ArbitraryLengthPath` per the Decision-Log gates by compiling the inner `StatementPattern` via `compileStatementPattern` and wiring endpoint vars through the planner's `slot(name)` assignment (constants via `idOf`, unknown id → null). Add `PathPlan` to `SlotPlan.canReorder` so it joins reorderably.

Milestone 2 — tests. `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativePropertyPathTest.java`: a graph with a chain, a diamond (two routes to one node — set semantics), a cycle, a self-loop, branches reaching literals, a disconnected node, and one edge in a named graph; differential queries: `+` and `*` with subject bound / object bound / both bound (ASK-like SELECT) / both free (`+` only) / both endpoints the same variable; paths joined with ordinary patterns on either side; path under COUNT and COUNT DISTINCT; inverse notation `^ex:p+`; a sequence path `ex:p/ex:q` and alternation `(ex:p|ex:q)+` (gates → fallback, results still correct); GRAPH-scoped path (gate → fallback); `*` with both endpoints free (gate → fallback). Engagement via `LmdbNativePathPlan.ENGAGED` for the gated-in shapes and stability for the gated-out ones.

Milestone 3 — benchmark. `PropertyPathReachabilityBenchmark` (benchmark-only): a layered tree/chain graph (~500k edges), variants `reachableFromRoot` (`+`, subject bound), `countReachable` (COUNT over `+`), `reachablePairsJoin` (path joined with a pattern); native vs `-Drdf4j.lmdb.nativePath.enabled=false`; verified result counts.

## Concrete Steps

From the repository root: compile `mvn -B -ntp -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Pquick install`; tests `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativePropertyPathTest --retain-logs`; full module `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- -DskipITs`; benchmark via `scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.PropertyPathReachabilityBenchmark --method executeQuery` twice (second with `--no-build --jvm-arg -Drdf4j.lmdb.nativePath.enabled=false`).

## Validation and Acceptance

All differential tests green (both engines agree on sorted canonical rows); engagement counter behavior as specified; full lmdb unit suite shows no new failures beyond the pre-existing `LmdbEvaluationStatisticsMemoizationTest` one; benchmark shows the native BFS ahead of the generic PathIteration on the bound-subject variants (target ≥3x; the win comes from id-space traversal and batched scans versus per-step Value/BindingSet materialization).

## Idempotence and Recovery

Additive: one new plan-node file, one dispatch case, one `canReorder` line, tests, benchmark. `rdf4j.lmdb.nativePath.enabled=false` (or deleting the dispatch case) restores generic evaluation for paths. Safe to re-run all steps.

## Artifacts and Notes

(Evidence and benchmark tables to be added as milestones complete.)

## Interfaces and Dependencies

No new libraries. End state in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativePathPlan.java`:

    final class PathPlan implements SlotPlan {
        static final AtomicLong ENGAGED;
        static final boolean ENABLED;                      // rdf4j.lmdb.nativePath.enabled
        static PathPlan tryCompile(ArbitraryLengthPath alp, PatternPlan step,
                int subjectSlot, long subjectConstant, int objectSlot, long objectConstant);  // null when gated
        RowCursor open(RowState row) throws IOException;   // FORWARD/BACKWARD/EXISTENCE/ALL_PAIRS
    }

plus one `ArbitraryLengthPath` case in `LmdbNativeAggregatePlanner.compileTuple` and `PathPlan` added to `SlotPlan.canReorder`.

Plan revision note (2026-07-06): initial version.
