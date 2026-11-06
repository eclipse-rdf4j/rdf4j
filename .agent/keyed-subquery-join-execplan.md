# Native keyed-lookup join for one-row-per-key subqueries (LMDB native engine)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md` (repository root relative). The repository rule "commit frequently" in PLANS.md is overridden by the user's instruction for this task: do not commit and do not push; leave all work in the working tree and keep every untracked file (`parked-untracked/`, `*.patch`, `scripts/lmdb.lmdb-bulk-load.properties`, `initial-evidence.txt`) untouched.

## Purpose / Big Picture

The RDF4J LMDB store (module `core/sail/lmdb`) has a "native engine" that evaluates SPARQL queries on dictionary ids (64-bit integers standing for RDF terms) instead of RDF4J `Value` objects. A common analytic query, the "class-linkage census", joins an instance-count subquery with the linkage patterns of every class pair:

    SELECT ?sourceClass ?instanceCount ?targetClass (COUNT(*) AS ?edgeCount) WHERE {
      { SELECT ?sourceClass (COUNT(DISTINCT ?source) AS ?instanceCount)
        WHERE { ?source rdf:type ?sourceClass } GROUP BY ?sourceClass }
      ?source rdf:type ?sourceClass .  ?source ?predicate ?target .
      FILTER(?predicate != rdf:type)   ?target rdf:type ?targetClass .
    } GROUP BY ?sourceClass ?instanceCount ?targetClass

A previous change on this branch (`LmdbSubqueryGroupPushdownOptimizer`) rewrites `Group(Join(Sub, Rest))` into `Join(Sub, Projection[subquery](Group(Rest)))`, so that the inner group runs on `LmdbNativeTypeMatrix` (a specialised native operator that computes class-pair edge counts). That leaves a join between two subquery operands. Today the native engine compiles both operands to `NativeSubqueryPlan` and combines them with `JoinPlan`, whose cursor re-opens the right operand for every left row; `NativeSubqueryCursor.next()` then replays every solution of that operand and interns each term, so the join costs roughly (number of classes) times (number of class pairs) dictionary interns: it is quadratic.

After this work, a join `Join(Sub, X)` in which `Sub` is a subquery with exactly one row per value of a single key variable `?c1` is evaluated by a new native operator, `NativeKeyedSubqueryJoinPlan`. It evaluates `Sub` once, indexes its rows by key, and attaches `Sub`'s extra columns to each row of `X` by looking the key up, remembering the answer for repeated keys. Observable effects: results are identical to the reference (non-native) evaluation; `explain(Telemetry)` shows `nativeExecutionPath=keyedSubqueryJoin(generic)` on the join; and a counter delta proves `Sub` was indexed exactly once. Later milestones (not in the scope of this document's current stopping point) add an adjacency-based fast path so that a single-pattern `COUNT(DISTINCT ?s)` subquery is answered by a metadata seek per probed key instead of a full materialisation.

Pluggable providers are the extension point: the join operator asks a provider "give me the columns for this key". The generic provider materialises any one-row-per-key subquery once and indexes it. A provider is one small class plus one recognizer line, so future providers (bag COUNT via run length, several aggregates over one pattern, per-key evaluation of a complex subquery) need no operator changes.

User decisions that bound the design: do not modify `LmdbNativeTypeMatrix` or its IR kernels; the operator must be correct whichever route produced the probe rows; the operator must extend to more complex subqueries through providers.

## Progress

- [x] (2026-11-05 17:00Z) Read the approved plan, PLANS.md, and verified the code references used below.
- [x] (2026-11-05 18:05Z) Environment: `.m2_repo` seeded from `~/.m2/repository` (missing BOM poms, jars and plugins), root install replaced by module install `-pl core/sail/lmdb -am` because the `tools/workbench` module needs a Node.js download (HTTP 403). Tests run through a manual Maven command instead of `mvnf` (see Decision Log).
- [x] (2026-11-05 18:08Z) Milestone 1: `LmdbSubqueryKeyShapes` extracted; `LmdbSubqueryGroupPushdownOptimizerTest` green before (10 tests) and after (10 tests).
- [x] (2026-11-05 18:12Z) Milestone 2, tests first: `LmdbKeyedSubqueryJoinTest` written; five positive tests fail for the right reason (no `keyedSubqueryJoin` tag, zero keyed opens); five decline tests pass (they pin the ordinary join). Evidence saved to `initial-evidence.txt`.
- [x] (2026-11-05 18:40Z) Milestone 2, implementation: `NativeSubqueryPlan` accessors and `attemptScopedState`; `NativeKeyedSubqueryProvider`; `NativeKeyedSubqueryIndex`; `NativeKeyedSubqueryJoinPlan` with `KeyedCursor`; planner hook `compileKeyedSubqueryJoin`; dispatch sites (`SlotPlan.assuredMask`/`encounterOrderReplaySafe`/`requiresPhysicalCells`, `subqueryFrameInputMask`, `NativeWorkerPlanBinder.canBind`/`bind`, `LmdbNativeKernelLowering`, minimal `LmdbNativeExplain.describe` and `LmdbNativeStrategyPreview.inspect`); vocabulary entry `keyedSubqueryJoin`.
- [x] (2026-11-05 19:00Z) Milestone 2, verification: `LmdbKeyedSubqueryJoinTest` 12 tests green; regression classes green (see Artifacts and Notes); `process-resources` formatter and `checkCopyrightPresent.sh` clean.
- [x] (2026-11-05 19:30Z) Milestone 3 (Routine B): `LmdbAdjacencyRootCounter` extracted. Pre-green on the adjacency selection (182 tests, six classes) captured first; hit proof: `LmdbAdjacencyAggregateMetadataTest` references `LmdbAdjacencyAggregatePlan` (6 hits) and drives `groupedDirectional` metadata path; post-green identical selection 182 tests. `LmdbAdjacencyRootCounterTest` (6 tests, fake `LmdbPrefixRunCursor`) green.
- [x] (2026-11-05 20:10Z) Milestone 4 (Routine A): failing tests first (8 failures, recorded in `initial-evidence.txt`), then `LmdbKeyedAdjacencyCountProvider`, its recognizer and the `keyedProvider` choice in the planner; `LmdbKeyedSubqueryJoinTest` 19 tests green.
- [x] (2026-11-05 20:40Z) Milestone 5: `NativeKeyedSubqueryJoinCancellationTest` (6 tests), richer `LmdbNativeExplain.describe`, `hasExecutionPath` guard so `open` records the tag once per distinct path; formatter and copyright clean.
- [x] (2026-11-06) Final checks: root clean install works; `mvnf` works after one non-offline run fetched `surefire-junit-platform:3.6.1`; 726 tests across the regression and adjacency selections green; full module run 12293 tests, only the 20 known `LmdbBufferedTripleTermTest` baseline failures; formatter and copyright check clean.

## Surprises & Discoveries

- Observation: the offline `.m2_repo` was missing jars and POMs that the repository build needs (Spring and Jackson BOMs, `surefire-junit-platform:3.6.1`), and the sandbox forbids deleting `*.zip`/`*.tar.gz` artefacts under `target/`.
  Evidence: `mvn -o ... clean install` failed with "Non-resolvable import POM ... spring-boot-dependencies:pom:4.1.2" and "Failed to delete .../sample-with-turtle...zip: Operation not permitted". Fixed by `rsync -a --ignore-existing ~/.m2/repository/ .m2_repo/`, deleting `_remote.repositories` markers, and running Maven outside the command sandbox.
- Observation: the surefire provider `surefire-junit-platform:3.6.1` that the default plugin version asks for is not available offline anywhere; `3.6.0` is.
  Evidence: "surefire-junit-platform:jar:3.6.1 (absent)". Tests are run with `org.apache.maven.plugins:maven-surefire-plugin:3.6.0:test` invoked explicitly after `test-compile`.
- Observation: before the change, the census plan is `Join(left=NativeSubquery(...), right=NativeSubquery(...))` executed by `strategy: nestedLoop` (a `JoinPlan`), confirming the quadratic shape.
  Evidence: the failing telemetry in `initial-evidence.txt` shows `0: Join(left=NativeSubquery(frameInputs=[], step=NativeProjection(arg=NativeGroup(...`.

- Observation: the GRAPH-body shape `GRAPH ?g { Sub X }` legitimately uses the keyed join. The graph body is its own evaluation scope run per graph, so the join inside it sees an independent Sub; only a Sub that takes a graph frame from a sibling (a `GraphFrameMapping` between the operands) or is wrapped by a GRAPH operand is declined.
  Evidence: the first draft of `subqueryInsideAGraphFrameKeepsTheOrdinaryJoin` failed because telemetry showed `nativeExecutionPath=keyedSubqueryJoin(generic)` on the Join under `Graph`, with results equal to the reference store. The test was split into `joinInsideAGraphBodyUsesTheKeyedJoinPerGraph` (positive) and `subqueryUnderAGraphOperandKeepsTheOrdinaryJoin` (decline).
- Observation: the SPARQL join optimizer places a plain subselect or pattern probe after the keyed subquery in almost every text order; only a probe written as a leading subselect keeps the keyed subquery as the right operand.
  Evidence: a temporary `System.err` print of `subIsAlgebraLeft` showed `true` for the census, the subquery-last census and the probe-first complex query, and `false` for `{ SELECT ?item ?category ... } { SELECT ?category (COUNT(...)) ... }`; the latter is now `keyedSubqueryAsTheRightOperandEqualsTheReferenceStore`.
- Observation: the kernel lowering (`LmdbNativeKernelLowering`, chain at the `NativeSubqueryPlan || NativeGraphPlan` check) reports `unsupported:<PlanClass>` for unknown plan types, so the new plan silently disabled IR-kernel lowering for a sub-select join.
  Evidence: `LmdbNativeKernelAdversarialDeclineTest.adversarialSpecializedDeclinesStayOnTheSemanticNativeFloor` failed with `irKernel:unsupported:NativeKeyedSubqueryJoinPlan sub-select`; adding the plan to the opaque-native-operator chain (`lowerNativeOperator`) made it pass again.

- Observation: a unit-test `RowState` built with `new RowState(...)` has slots initialised to 0, not `UNKNOWN`; `bind` then fails against the phantom value. Tests must seed through `NativeRowSeeder.seed` (and `recomputeBoundMask`).
  Evidence: the first run of `NativeKeyedSubqueryJoinCancellationTest` failed all three row-driven tests (probe produced 5000 rows, session never opened) until `newRow` seeded the slots.
- Observation: when a commit is still pending in the applier, the source reports no adjacency run plan (`usesAdjacency()` false), so the keyed provider serves the whole session through the generic index. The metadata-unavailable decode path only happens once the applier has folded the commit into a delta over the built base.
  Evidence: with the applier paused, telemetry showed `keyedSubqueryJoin(generic)` and zero adjacency counters; after resuming the applier (`pauseApplier(store, false)` waits until the head is applied) the same query ran `keyedSubqueryJoin(adjacencyDistinctCount)` with `ADJACENCY_DECODE_COUNTS` greater than zero.
- Observation: `logical(...)` (bag-weight replay) returns false at a cancelled tick instead of throwing, so a cancelled probe makes the keyed cursor simply stop; the cooperative poll in the keyed loop is the backstop for runs of probe rows that never emit.

## Decision Log

- Decision: `LmdbNativeStrategyPreview.inspect` keeps its minimal form (inspect the probe and the subquery plan).
  Rationale: the keyed join has no strategy alternatives of its own to preview; the provider choice is rendered by `LmdbNativeExplain.describe` and the executed tag, and previewing both children is what the nested join already did.
  Date/Author: 2026-11-06 / Claude agent.
- Decision: `AdjacencyEngagementTestAccess` (test scope) gained `pauseApplier` and `ROOT_SCAN_PROPERTY`.
  Rationale: `pauseApplierForTest` and the property are package-private in `org.eclipse.rdf4j.sail.lmdb`, while the new test lives in the evaluation package; resuming the applier is how the test waits until a commit is folded into the delta.
  Date/Author: 2026-11-06 / Claude agent.

- Decision: The sequential decode of `LmdbAdjacencyAggregatePlan.groupedDirectional` (one increment per run) stays in place; Milestone 3 extracts the shared rules (canonical backing, metadata gate, metadata read plus accounting) used by that plan, and puts the per-root decode and forward-only seek bookkeeping into `LmdbAdjacencyRootCounter`.
  Rationale: the sequential loop is not reusable per key, and moving it would change a behaviour-neutral refactor into a rewrite; the keyed decode counts the (root, neighbour) runs of one root, which is a different loop.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: The adjacency provider decides at run time whether adjacency serves a row (`servesFromAdjacency`) and reports `generic` through `NativeKeyedSubqueryProvider.executionTag(row)` when it does not; value-guarded keys go to a lazily opened generic session for that key only.
  Rationale: whether the source offers an adjacency plan is only known per evaluation (uncommitted overlay, pending applier), but the telemetry must name the provider that actually answered.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: Probe results are cached per session in a `HashMap` up to 65536 keys (not a direct-mapped cache), so `ADJACENCY_KEY_PROBES` equals the number of distinct probed keys exactly.
  Rationale: a direct-mapped cache can evict colliding keys and re-probe them, which would make the counter unequal to the distinct key count the test pins.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: `NativeKeyedSubqueryJoinPlan.open` records its path only when `LmdbNativeExplain.hasExecutionPath` says it is missing.
  Rationale: `recordExecutionPath` takes the expression monitor on every call; nested opens under an outer loop should not.
  Date/Author: 2026-11-05 / Claude agent.

- Decision: Keep Sonnet-agent delegation out of this stage; the Sonnet agent executing this plan implements directly.
  Rationale: the invoking instruction scoped this stage to Routine D setup plus Milestones 1 and 2 for a single agent.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: Run tests with a manual Maven command (`test-compile` plus explicit `maven-surefire-plugin:3.6.0:test`, no `-am`, no `-q`) instead of `mvnf`.
  Rationale: `mvnf` performs a full root install, which fails on the `tools/workbench` module (Node.js download blocked), and the default surefire provider version is not available offline. CLAUDE.md allows manual Maven when `mvnf` does not fit. Module install `-pl core/sail/lmdb -am -Pquick clean install` was run once at the start so that `.m2_repo` holds current upstream jars.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: Integrate the keyed join as a post-compile step in the existing Join branch of `LmdbNativeAggregatePlanner.compileTuple`, after both operands have been compiled exactly as the ordinary join compiles them, rather than compiling `Sub` with `compileNativeSubquery(sub, new String[0])` first.
  Rationale: the approved plan compiled Sub with no frame inputs, then reused those plans for a decline. Compiling each operand with the same function the ordinary path uses (left: `compileTupleOrIsland`; right: `compileNativeSubquery(rightArg)` when `independentSubqueryRight`, else `compileTupleOrIsland`) makes every decline identical to today's behaviour by construction. The independence of Sub from the outer row is then checked on the compiled `NativeSubqueryPlan` (`independentOfOuterRow()`).
  Date/Author: 2026-11-05 / Claude agent.
- Decision: The generic index interns the key and payload terms once at build time (store-first, through `SyntheticValueSource.internComputedValue`) and stores ids in primitive arrays; the join binds payload ids with `RowState.bindOrCheckTerm`.
  Rationale: this makes the per-probe work an id hash lookup, with `LmdbNativeTermAuthority.termHashKey` for hashing and `sameRdfTerm` to confirm, and keeps the index immutable so that it can be shared across attempts.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: Treat `NativeKeyedSubqueryJoinPlan` as an opaque native operator in `LmdbNativeKernelLowering` exactly like `NativeSubqueryPlan`.
  Rationale: the plan evaluates correctly under pre-bound inputs (it opens the probe on the entry row and reads the key slot per row), and the alternative (declining kernel lowering) regressed an existing adversarial-decline test.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: Open the provider session lazily on the first probe row that has a decidable key, and record the `keyedSubqueryJoin(<tag>)` path on open.
  Rationale: an empty probe then never evaluates the subquery, as in the ordinary join, and the tag is reported for every keyed open.
  Date/Author: 2026-11-05 / Claude agent.
- Decision: Each worker fork of the plan gets its own index (cache key is the worker's own `NativeSubqueryPlan`), not one shared across workers.
  Rationale: runtime (synthetic) ids are only guaranteed shareable between sources that prove a shared term space; building per fork avoids relying on that proof.
  Date/Author: 2026-11-05 / Claude agent.

## Outcomes & Retrospective

Milestone 1 is complete: the shared recognizer exists in a public class and `LmdbSubqueryGroupPushdownOptimizerTest` (10 tests) is green before and after.

Milestone 2 is complete: the census and any other keyed one-row-per-key subquery join now run through `NativeKeyedSubqueryJoinPlan` with the generic provider, reporting `nativeExecutionPath=keyedSubqueryJoin(generic)`, while the type matrix still runs the inner group. `LmdbKeyedSubqueryJoinTest` (12 tests) passes; the five positive tests failed before the implementation for the expected reasons. Regression classes requested for this stage pass, and the full module unit run shows 12274 tests with 20 failures, all in `LmdbBufferedTripleTermTest` (19, reproducible on an unmodified checkout of HEAD) and `LmdbValueGcRecoveryTest` (1, passes when run alone), i.e. value-id reclamation storage tests that do not touch query evaluation.

Update at the end of Milestone 5: all milestones are done; the encounter-order delegation and worker binding are covered by `NativeKeyedSubqueryJoinCancellationTest`. The sentence below describes the state at the end of Milestone 2 and is kept for history. Not done at that time: the adjacency fast path, the `LmdbAdjacencyRootCounter` extraction, richer explain rendering, cancellation and worker-binding tests. Known gap: the sub-major encounter-order delegation (`row.encounterOrderRequired` with Sub as the algebra-left operand) is implemented but no test forces `encounterOrderRequired`, so it is covered only by review; Milestone 5 should add one. Lesson: exhaustive `instanceof` chains hide regressions as silent declines (the kernel lowering), so every new plan type should be run through `LmdbNativeKernelAdversarialDeclineTest` and the lowering tests.

## Context and Orientation

All paths are relative to the repository root `/Users/hottestad1/Documents/Programming/rdf4j`. The module is `core/sail/lmdb`; main sources are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`, and the native engine lives in the `evaluation` sub-package (called "the evaluation package" below).

Vocabulary used in this document, in plain words. A "slot" is an integer index into a per-evaluation array (`RowState.slots`) that holds the current dictionary id of one query variable; unbound slots hold the constant `UNKNOWN`. A "plan" (`SlotPlan`) is an immutable operator; calling `open(RowState)` returns a "cursor" (`RowCursor`) whose `next()` binds slots for the next solution and returns true, and whose `close()` releases resources. Bindings made while a cursor advances are recorded on a "trail" so `RowState.mark()` / `RowState.rollback(mark)` can undo them. `RowState.bindOrCheckTerm(slot, id)` binds an unbound slot or checks that an existing binding denotes the same RDF term. A "probe" is the operand whose rows drive the join; a "keyed lookup" finds the (at most one) row of the other operand with the same key.

Key files, all in the evaluation package unless noted:

- `NativeSubqueryPlan.java`: the plan for an independent subquery boundary. It evaluates its inner step once per evaluation through `NativeLazyResultReplay` (a once-evaluated producer with replayable, ledger-accounted storage), cached in `NativeExecutionContext.nativeState`, and `NativeSubqueryCursor` replays solutions into the outer row. Its private `replayState` wraps `execution.nativeState` and registers the state with the current probe attempt (`LmdbNativeProbeDeadline`) so that a declined attempt discards it.
- `LmdbNativeSlotPlan.java`: the `SlotPlan` interface and the exhaustive `instanceof` chains `assuredMask`, `encounterOrderReplaySafe`, `requiresPhysicalCells` (unknown plan types are conservatively "nothing assured", "unsafe", "false").
- `LmdbNativeJoinPlans.java`: `JoinPlan`/`JoinCursor` (the ordinary nested-loop join), `JoinCleanup` (failure-safe close helper) and `LateralCursor`.
- `LmdbNativeAggregatePlanner.java`: compiles algebra to plans. The Join branch of `compileTuple` is around line 1860. `subqueryFrameInputMask` (around line 2199) unions frame-input slots over nested plans. `compileNativeSubquery` (around line 2259 and 2321) builds a `NativeSubqueryPlan`.
- `NativeWorkerPlanBinder.java`: `canBind`/`bind` rebuild a plan for a parallel worker; an unknown plan type throws "unsupported native worker producer".
- `LmdbNativeAttemptMetrics.java`: `EXECUTION_PATH_VOCABULARY` lists the allowed base names for `nativeExecutionPath` telemetry; `LmdbNativeExplain.recordExecutionPath(expr, tag)` records a tag on an algebra node.
- `LmdbNativeTermAuthority.java`: `termHashKey(id)` and `sameRdfTerm(a, b)` give term-equality hashing across id spaces.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSubqueryGroupPushdownOptimizer.java` and the new `LmdbSubqueryKeyShapes.java` (package `org.eclipse.rdf4j.sail.lmdb`): the recognizer `oneRowPerKeyVariable(TupleExpr)` returns the single grouping variable of a plain subquery (`Projection[subquery]` over optional `Extension`s over a `Group` with exactly one grouping variable, no renames, assured key) or null.

Tests: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbTypeMatrixSubqueryCensusTest.java` is the harness copied by the new `LmdbKeyedSubqueryJoinTest.java` (pinned system properties, a direct-adjacency native store plus `AdjacencyEngagementTestAccess.buildNow`, a reference store with `setNativeEvaluationEnabled(false)`, a seeded fixture, sorted row comparison, `explain(Explanation.Level.Telemetry)`).

Environment notes (found during this work): Maven must run outside the command sandbox, offline (`-o`) with `-Dmaven.repo.local=.m2_repo`; see Surprises & Discoveries.

## Plan of Work

### Milestone 1: extract the shared recognizer (refactor, behaviour-neutral)

Create `LmdbSubqueryKeyShapes` (public final, `@Experimental @InternalUseOnly`, license header 2026 plus `// Some portions generated by Claude Code`) in `org.eclipse.rdf4j.sail.lmdb`, holding `public static String oneRowPerKeyVariable(TupleExpr)` moved verbatim from the optimizer. The optimizer calls it and drops its now-unused `QueryModelNode` import. Proof: `LmdbSubqueryGroupPushdownOptimizerTest` (10 tests) passes before and after, from the same selection. This milestone is Routine B (behaviour-neutral refactor with direct existing coverage).

### Milestone 2: generic keyed join

Scope: after this milestone, every keyed one-row-per-key subquery joined with any operand uses an index built once, and the census reports `keyedSubqueryJoin(generic)` while the type matrix is still used for the inner group.

Tests first (Routine A). `LmdbKeyedSubqueryJoinTest` covers: the census (results equal the reference store, telemetry contains `nativeExecutionPath=keyedSubqueryJoin(generic)` and a type-matrix path); the census with the subquery written last; a complex subquery (join, filter, two aggregates) served by the generic provider, with `KEYED_OPENS` and `GENERIC_INDEX_BUILDS` counter deltas of exactly one; the probe operand written first; rows whose key has no subquery row dropped (category links to non-classes); and decline cases that must keep the ordinary join (no `keyedSubqueryJoin` in telemetry, equal results): both operands one-row-per-key, operands sharing more than the key, key not assured in the other operand (OPTIONAL), subquery inside a GRAPH frame, and the kill switch `rdf4j.lmdb.keyedSubqueryJoin.enabled=false`. The positive tests must fail before the implementation for the right reason (missing tag, zero opens).

Implementation, in this order.

1. `NativeSubqueryPlan`: add package-private `independentOfOuterRow()` (no frame input slots, no private graph parameters, no lateral step factory), `outNames()`, `outSlots()`, `executionContextOf(RowState)`, `internTerm(RowState, Value, int slot)`, `openSolutions(RowState)` (the frame-free half of `open`: obtains the replay state and returns `state.cursor(row.cancellation)`), and extract `replayState` into `static <T> T attemptScopedState(NativeExecutionContext, Object lexicalKey, Supplier<T> factory)` so other cached values get the same attempt-discard semantics; `replayState` becomes a thin caller.
2. `NativeKeyedSubqueryProvider` (interface): `String tag()`, `Session open(RowState row, NativeKeyedSubqueryJoinPlan plan)`, `NativeKeyedSubqueryProvider forWorker(NativeSubqueryPlan workerSub)`. `Session` (AutoCloseable): `Lookup seek(long keyId)` returning `MATCH`, `ABSENT` or `LEGACY`; `boolean nextMatch()`; `boolean bindMatch(RowState row)` (binds the payload slots of the current match, false on incompatibility); `close()`.
3. `NativeKeyedSubqueryIndex` (generic provider): builds an immutable `Index` (AutoCloseable, memory charged to the query ledger through `KernelRuntime.AggregateMemory`) once per attempt-scoped key `IndexKey(sub)`: drains `sub.openSolutions(row)`, interns the key cell and payload cells, stores them in primitive arrays with a hash table over `termHashKey`, with head/next chains so it stays a correct multimap, confirms matches with `sameRdfTerm`. Rows whose key cell is not a bound RDF term mark the index "legacy only". A session keeps a small bounded direct-mapped cache from probe key id to first match.
4. `NativeKeyedSubqueryJoinPlan` (`SlotPlan`) with nested `KeyedCursor` (`RowCursor`), static counters `KEYED_OPENS`, `GENERIC_INDEX_BUILDS`, `LEGACY_FALLBACKS`, a `forWorker` rebinding, the tag recording through `LmdbNativeExplain.recordExecutionPath`, the cooperative polls (`LmdbNativeProbeDeadline.poll`, `row.cancellation.poll`) and `JoinCleanup`-based failure cleanup. For one probe row it reads `row.slots[keySlot]`; if that slot is unbound or holds a lexical summary it falls back to `subPlan.open(row)` for that row only; otherwise it seeks and binds the payload under `row.mark()` / `row.rollback`. If `row.encounterOrderRequired` and Sub is the algebra-left operand it delegates to `new JoinPlan(subPlan, probe)` to keep Sub-major order. The probe cursor is wrapped with `LmdbNativeFactorAlgebra.logical` so bag weights of factorized probe cursors are replayed exactly.
5. `LmdbNativeAggregatePlanner`: in the Join branch, after both operands are compiled and the existing null/empty guards, call `compileKeyedSubqueryJoin(join, left, right)` which returns the keyed plan or null (null means continue with `joinWithIslandHoist`). Eligibility: kill switch on; `forceInteriorIslands` off; no GRAPH frames on either operand; the join is not a merge join; exactly one operand passes `LmdbSubqueryKeyShapes.oneRowPerKeyVariable` (both passing declines); Sub and probe share exactly the key as visible binding names; the key is in the probe's `getAssuredBindingNames()`; Sub compiled to an independent `NativeSubqueryPlan`; neither plan is `EmptyPlan`; neither plan `requiresPhysicalCells`. Extend `subqueryFrameInputMask` with the new plan.
6. Dispatch sites that would otherwise misbehave on the new plan type: `SlotPlan.assuredMask` (probe's assured mask plus the assured payload slots), `encounterOrderReplaySafe` (probe safe and Sub worker-bindable), `requiresPhysicalCells`; `NativeWorkerPlanBinder.canBind` and `bind`; `LmdbNativeAttemptMetrics.PATH_KEYED_SUBQUERY_JOIN = "keyedSubqueryJoin"` added to `EXECUTION_PATH_VOCABULARY`; `LmdbNativeExplain.describe` and `LmdbNativeStrategyPreview.inspect` get minimal cases (richer rendering is Milestone 5).

Acceptance for Milestone 2: the new test class passes (ten tests); `LmdbTypeMatrixSubqueryCensusTest`, `LmdbSubqueryGroupPushdownOptimizerTest`, `LmdbNativeExplainTest`, `LmdbNativeGeneratedQueryCoverageTest`, `LmdbNativeStrategyArbiterTest`, `NativeSubqueryWorkerScopeTest` and any other test referencing `NativeSubqueryPlan` stay green.

### Milestone 3 (not started): `LmdbAdjacencyRootCounter`

Extract from `LmdbAdjacencyAggregatePlan` the per-root distinct-neighbour counting (decode loop, metadata read, metadata gate, canonical backing) into a reusable class; the adjacency suites stay green; add `LmdbAdjacencyRootCounterTest` with a fake cursor.

### Milestone 4 (not started): adjacency fast path

`LmdbKeyedAdjacencyCountProvider` recognises `Projection{c1,cnt}` over an aggregate-only `Extension` over `Group({c1}, COUNT(DISTINCT ?n))` over one `StatementPattern` with a constant predicate, no context, and answers each probed key with a metadata seek, with tag `adjacencyDistinctCount`. Tests first: census tag, zero `GENERIC_INDEX_BUILDS`, probes equal to distinct source classes, duplicates across named graphs, count-literal id agreement, committed-delta decode fallback, uncommitted-transaction visibility, FROM dataset falls back to the generic tag.

### Milestone 5 (not started): dispatch polish and cancellation

Explain and preview rendering, `NativeKeyedSubqueryJoinCancellationTest` (stub probe, generic build cancelled mid-way, fast-path decode cancellation) and a worker-binding test.

## Concrete Steps

Working directory for every command: `/Users/hottestad1/Documents/Programming/rdf4j`.

Build upstream modules once (this mirrors the CLAUDE.md template, narrowed to the LMDB module because the full root install fails on the workbench module here):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick clean install

Run one test class (no `-am`, no `-q`):

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbKeyedSubqueryJoinTest \
      -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -DtrimStackTrace=false \
      test-compile org.apache.maven.plugins:maven-surefire-plugin:3.6.0:test

Expected after Milestone 2: `Tests run: 10, Failures: 0, Errors: 0`. Reports are in `core/sail/lmdb/target/surefire-reports/`.

Before finishing: `cd scripts && ./checkCopyrightPresent.sh`, then from the repository root `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` (the formatter).

## Validation and Acceptance

Behavioural acceptance: for the census query on the fixture, rows from the native store equal rows from the reference store; the executed telemetry contains `nativeExecutionPath=keyedSubqueryJoin(generic)` and a type-matrix path; for the complex-subquery query, exactly one keyed open and one generic index build are counted; for the missing-key query fewer rows than category links are returned and they equal the reference; every decline query returns the reference rows and its telemetry contains no `keyedSubqueryJoin`.

Before the change (observed): five tests fail with "Expecting actual ... to contain nativeExecutionPath=keyedSubqueryJoin(generic)" and "keyed join opens expected 1 but was 0"; five decline tests pass.

## Idempotence and Recovery

All steps are additive and re-runnable. The new operator is guarded by the kill switch `rdf4j.lmdb.keyedSubqueryJoin.enabled` (default true), so reverting behaviour needs only that property. Nothing is committed; to roll back, delete the new files and revert the touched files with `git diff`-guided edits (never `git checkout --` unrelated changes).

## Artifacts and Notes

`initial-evidence.txt` at the repository root holds the pre-implementation failing summary of `LmdbKeyedSubqueryJoinTest` (five positive tests failing, tests=20 failures=5 across the two selected classes).

Post-implementation evidence (regression selection, manual Maven run described in Concrete Steps, 491 tests across 18 classes before the kernel-lowering fix; after it the one failing class re-ran green):

    Tests run: 12, Failures: 0, Errors: 0 -- LmdbKeyedSubqueryJoinTest
    Tests run: 4, Failures: 0, Errors: 0 -- LmdbTypeMatrixSubqueryCensusTest
    Tests run: 10, Failures: 0, Errors: 0 -- LmdbSubqueryGroupPushdownOptimizerTest
    Tests run: 8, Failures: 0, Errors: 0 -- LmdbNativeExplainTest
    Tests run: 31, Failures: 0, Errors: 0 -- LmdbNativeGeneratedQueryCoverageTest
    Tests run: 90, Failures: 0, Errors: 0 -- LmdbNativeStrategyArbiterTest
    Tests run: 5, Failures: 0, Errors: 0 -- LmdbNativeKernelAdversarialDeclineTest (after the lowering fix)
    Tests run: 135, Failures: 0, Errors: 0 -- LmdbNativeKernelLoweringTest

## Interfaces and Dependencies

No new dependencies. At the end of Milestone 2 these exist in `org.eclipse.rdf4j.sail.lmdb.evaluation` (package-private unless noted):

    final class NativeKeyedSubqueryJoinPlan implements SlotPlan {
        static final AtomicLong KEYED_OPENS, GENERIC_INDEX_BUILDS, LEGACY_FALLBACKS;
        NativeKeyedSubqueryJoinPlan(SlotPlan probe, NativeSubqueryPlan sub, NativeKeyedSubqueryProvider provider,
            int keySlot, long assuredMask, TupleExpr explainTarget, boolean subIsAlgebraLeft);
        NativeKeyedSubqueryJoinPlan forWorker(NativeWorkerPlanBinder binder);
    }
    interface NativeKeyedSubqueryProvider {
        String tag();
        Session open(RowState row, NativeKeyedSubqueryJoinPlan plan) throws IOException;
        NativeKeyedSubqueryProvider forWorker(NativeSubqueryPlan workerSub);
        enum Lookup { MATCH, ABSENT, LEGACY }
        interface Session extends AutoCloseable {
            Lookup seek(long keyId);
            boolean nextMatch();
            boolean bindMatch(RowState row);
            void close();
        }
    }
    final class NativeKeyedSubqueryIndex implements NativeKeyedSubqueryProvider  // tag "generic"

and in `org.eclipse.rdf4j.sail.lmdb`: `public final class LmdbSubqueryKeyShapes { public static String oneRowPerKeyVariable(TupleExpr operand); }`.

Revision note (2026-11-05, end of Milestone 2): Progress, Surprises & Discoveries, Decision Log, Outcomes and Artifacts updated with the implementation results, the kernel-lowering regression and fix, the GRAPH-body and operand-order findings, and the full-module test outcome.

Revision note (2026-11-05): initial version written at Milestone 1 completion and Milestone 2 test-first stage, translating the approved plan into PLANS.md format; the post-compile integration decision and the environment workarounds were added to the Decision Log and Surprises sections at the same time.
