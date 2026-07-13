# Native LMDB Left Join Improvement

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (checked into this repository).

## Purpose / Big Picture

The LMDB native query engine currently evaluates SPARQL `OPTIONAL` (`LeftJoin` in RDF4J algebra) with a dependent nested-loop cursor: for every left row, it opens the right side again. This is correct for well-designed queries in the common case, but it misses two important properties. First, badly-designed OPTIONAL queries can bind optional-only variables outside the optional subtree; RDF4J's generic evaluator handles that shape specially, while the native cursor currently does not. Second, the shipped `JoinCursor` already has useful optimizations, such as reusable LMDB probes and replaying uncorrelated right-side rows, that `LeftJoinCursor` does not yet share.

After this plan is complete, native LMDB OPTIONAL evaluation will have a correctness gate that refuses unsafe algebra and delegates dynamic base-binding cases to the generic evaluator. Then it will gain the same low-risk cursor machinery as `JoinCursor`, and finally it will add a guarded hash/materialization path for common single-pattern OPTIONALs such as `?x :p ?optionalValue`. Every performance stage remains optional at runtime through an `rdf4j.lmdb.*` system-property kill switch, and every stage is validated by differential tests that compare native-on results with `-Drdf4j.lmdb.nativeQueryEngine.enabled=false`.

## Progress

- [x] (2026-07-03T14:33Z) Read `.agent/PLANS.md`, confirmed tracked tree was clean before edits, and ran the required root quick install.
- [x] (2026-07-03T14:34Z) Authored this ExecPlan from the staged left-join plan and current source layout.
- [x] (2026-07-03T14:38Z) Stage 1 red captured: `LmdbNativeLeftJoinWellDesignedTest#badlyDesignedSiblingFallsBackForGroupRoot` failed because native compilation incremented `COMPILED` for a badly-designed OPTIONAL.
- [x] (2026-07-03T14:41Z) Stage 1 implemented: compile-time OPTIONAL well-designedness pre-pass and evaluate-time generic delegation for optional-only base bindings.
- [x] (2026-07-03T14:44Z) Stage 1 focused suite green: `LmdbNativeLeftJoinWellDesignedTest` 5/5.
- [x] (2026-07-03T14:52Z) Stage 1 regression gates green: `LmdbRealEstateNativeParityTest` 13/13, `LmdbNativeRowStreamTest` 16/16, `LmdbNativeMembershipJoinTest` 6/6, and unit-only `LmdbNative*Test` 100/100.
- [x] (2026-07-03T16:10Z) Stage 2 implemented: `LeftJoinCursor` now reuses pattern probes, materializes replayable uncorrelated rights under `rdf4j.lmdb.leftjoin.replay.enabled`, and `memoReadMask` handles nested `LeftJoinPlan`.
- [x] (2026-07-03T16:36Z) Stage 3a/3b implemented: correlated single-pattern OPTIONALs can build a payload hash under `rdf4j.lmdb.leftjoin.hash.enabled`, including `FilterPlan(PatternPlan)` cases with known masks and overflow-to-key-set degrade.
- [x] (2026-07-03T16:42Z) Stage 3a/3b verification green after formatting: native unit sweep, REAL_ESTATE parity, and REAL_ESTATE count IT.
- [x] (2026-07-03T16:51Z) Stage 3c implemented and verified: correlated multi-pattern OPTIONAL rights memoize per key under `rdf4j.lmdb.leftjoin.memo.enabled`.
- [x] (2026-07-03T16:54Z) Stage 5 completed: removed Roaring membership prototype branch/dependency; membership modes are now `off|hash`.
- [x] (2026-07-03T17:06Z) Review fix completed: absent value-store constants inside an OPTIONAL right side now compile as an empty subplan without making the whole native root empty.

## Surprises & Discoveries

- Observation: `LmdbNativeAggregateCompiler.tryCompile` handles both aggregate roots and row-stream roots. The BGP compiler, `LmdbNativeQueryCompiler`, has no LeftJoin support, so all engine work for this plan belongs in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregateCompiler.java`.
  Evidence: `tryCompile` first invokes aggregate compiler logic for `Group`, `Filter(Group)`, and other tuple roots; source inspection on 2026-07-03 showed the row-root path in `compileRowRoot`.
- Observation: `LmdbNativeEvaluationStrategy.precompile` returns the native step directly and only falls back before a native step is returned. A dynamic base-binding refusal inside `NativeGroupStep.evaluate` or `NativeRowsStep.evaluate` therefore needs an explicit generic delegate that bypasses native compilation.
  Evidence: `LmdbNativeEvaluationStrategy.precompile(TupleExpr, QueryEvaluationContext)` calls `LmdbNativeAggregateCompiler.tryCompile`; if that returns non-null, it returns the step immediately.
- Observation: the working tree had no tracked changes before this work, but several untracked ExecPlan artifacts already existed at the repository root. This plan must not remove or normalize them.
  Evidence: `git status --short --untracked-files=no` printed no tracked changes; full status listed untracked `execplan-*.md` files.
- Observation: REAL_ESTATE query 5 does not compile natively on the current branch, independent of the new well-designedness gate.
  Evidence: the first coverage guard expected all 13 REAL_ESTATE queries to increment `COMPILED`; after the binder-only fix, q0-q4 incremented but q5 did not. The guard was changed to assert no native coverage shrink compared with `rdf4j.lmdb.leftjoin.wellDesignedCheck=false`.
- Observation: running the broad pattern through `mvnf` as a module verify with `-Dtest=LmdbNative*Test` also executes unrelated Failsafe ITs unless `-DskipITs` is passed.
  Evidence: `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs -- '-Dtest=LmdbNative*Test'` failed in benchmark/theme ITs with `LMDB sketch-based join estimator is not available`; the unit-only rerun with `-DskipITs` passed 100 tests.
- Observation: an OPTIONAL whose right side contains a constant IRI absent from the value store currently returns no left rows in the native row-stream path, even with Stage 2 replay disabled.
  Evidence: the initial Stage 2 empty-right test used `OPTIONAL { ex:missing ex:tag ?tag }`; native replay-disabled rows were `[]` while generic rows were the five null-extended left rows. The Stage 2 replay test was narrowed to `OPTIONAL { ex:global ex:probe ?tag }`, where both constants exist and the right side is still empty. The absent-constant shape is a separate correctness issue.
- Observation: a right-side filter such as `FILTER(?tag = ex:tagA)` is compiled away before the Stage 3 `FilterPlan(PatternPlan)` path can see it.
  Evidence: `compileFilterIntoStatementPattern` folds constant equality filters into value-constrained pattern plans. The Stage 3 condition test therefore uses `FILTER(BOUND(?s))`, which remains a known-mask `FilterPlan(PatternPlan)` and exercises payload-hash filtering without changing the statement pattern.

## Decision Log

- Decision: Implement Stage 1 before any performance stage.
  Rationale: Differential tests for replay, hash, and memo paths are only meaningful if native OPTIONAL semantics match the generic evaluator for badly-designed queries. Stage 3 must also not build a hash keyed on a variable that should have forced generic `BadlyDesignedLeftJoinIterator` behavior.
  Date/Author: 2026-07-03 / Codex.
- Decision: Keep dynamic fallback as an explicit generic precompile hook on `LmdbNativeEvaluationStrategy`, rather than trying to return `null` from `evaluate`.
  Rationale: compilation has already succeeded by the time external `BindingSet` values are visible. The generic delegate must call `StrictEvaluationStrategy` behavior without re-entering native compilation.
  Date/Author: 2026-07-03 / Codex.
- Decision: Stage 2 ports `JoinCursor` behavior nearly verbatim before adding new hash structures.
  Rationale: reusable probes and uncorrelated replay are already proven for inner joins, are easy to guard, and make `LeftJoinCursor` simpler to compare against the later hash path.
  Date/Author: 2026-07-03 / Codex.
- Decision: RoaringBitmap should not be part of the left-join payload design.
  Rationale: the existing Milestone 3.5 measurements showed that materializing once was the win; Roaring and the in-repo long hash set were within noise at the measured cardinalities. A bitmap also cannot carry optional payload bindings.
  Date/Author: 2026-07-03 / Codex.
- Decision: The Stage 1 static pre-pass checks whether optional-only names are bound outside the `LeftJoin` subtree, not whether they are merely read outside it.
  Rationale: REAL_ESTATE q0 safely reads an optional variable in a filter after the OPTIONAL; refusing that query would shrink native coverage without fixing a well-designedness bug. The unsafe static ingress is a sibling or extension that binds the optional-only variable before the left join opens.
  Date/Author: 2026-07-03 / Codex.

## Outcomes & Retrospective

Stage 1 is complete. The native compiler now refuses statically badly-designed OPTIONAL roots and keeps the existing path for safe OPTIONALs. Native group and row-stream steps also carry optional-only names and lazily delegate to a generic precompiled step when external base bindings bind one of those names. The original red was a compile-count failure on a badly-designed aggregate root; after the fix, the full new suite and existing focused native gates are green.

Stage 2 is complete. `LeftJoinCursor` now mirrors `JoinCursor` for plain-pattern probe reuse and replay of uncorrelated right sides, with null-extension semantics during replay. `memoReadMask` also treats `LeftJoinPlan` as the union of its children's read masks, unless either child is unknown or `rdf4j.lmdb.leftjoin.replay.enabled=false`. The focused Stage 2 red was a structural replay assertion on an uncorrelated OPTIONAL; after the fix, the Stage 2 cursor class and a 71-test native gate are green.

Stage 3a/3b is complete. The first hash path is a primitive, append-only `long key -> payload rows` map built from a single `PatternPlan` scan once the adaptive probe threshold is crossed. It engages only for unrestricted, non-repeated single-pattern rights, and it falls back to Stage 2 when disabled, not applicable, keyed on a different slot, or oversized. `FilterPlan(PatternPlan)` rights with known masks are supported by applying the native filter after binding each payload row. Payload overflow degrades to a miss-only key set; if that key set exceeds `rdf4j.lmdb.membership.maxSize`, the hash path disables and the existing INLJ path remains. Stage 3c acceptance remains a green per-key memo path for correlated multi-pattern rights. Stage 5 acceptance is a clean build without the Roaring dependency and with membership tests updated to the surviving `hash|off` modes.

Stage 3c is complete. Non-single-pattern right sides with a known `memoReadMask` and at least one read slot produced by the left side now use a lazy per-key memo under `rdf4j.lmdb.leftjoin.memo.enabled`. The first occurrence of a key streams the existing right plan while recording produced slots up to a bounded row cap; later occurrences replay those rows with the same mark/bind/rollback discipline used elsewhere. Oversized keys store a sentinel and keep using the existing INLJ path. Single-pattern rights remain assigned to the payload hash path or the Stage 2 path.

Stage 5 is complete. The RoaringBitmap prototype dependency and `roaring` membership branch were removed after the Stage 3 hash/memo implementation kept the materialization strategy on primitive in-repo structures. `rdf4j.lmdb.membership.impl` now supports `off|hash`; unsupported values disable the membership materialization and fall back to the existing probe path.

Review follow-up is complete. The earlier surprise about `OPTIONAL { ex:missing ex:probe ?tag }` dropping all left rows was caused by an impossible statement pattern setting a compiler-wide empty-root flag. That flag was removed; an impossible statement pattern now compiles to `EmptyPlan` locally, so ordinary joins/root row streams still become empty, while `LeftJoin(left, EmptyPlan)` correctly preserves and null-extends left rows.

## Context and Orientation

Everything in this plan is in the RDF4J repository at `/Users/havardottestad/Documents/Programming/rdf4j`.

The native LMDB engine is selected by `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeEvaluationStrategy.java`. Its `precompile` method first asks `LmdbNativeAggregateCompiler.tryCompile` for a native aggregate or row-stream plan, then asks `LmdbNativeQueryCompiler.tryCompile` for simpler basic-graph-pattern plans, and finally calls the generic `StrictEvaluationStrategy` implementation. The property `rdf4j.lmdb.nativeQueryEngine.enabled=false` disables this native route and is the reference mode for all differential tests.

The main implementation file is `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregateCompiler.java`. The compiler maps query variables into primitive slot indexes, compiles tuple algebra into `SlotPlan` objects, and opens `RowCursor` implementations to stream row ids. A `LeftJoin` algebra node corresponds to SPARQL `OPTIONAL`. A row is "null-extended" when the optional right side finds no match: the left row is emitted with right-side variables unbound.

Important current locations in `LmdbNativeAggregateCompiler.java`:

- `tryCompile` creates a `Compiler` and increments `COMPILED` when native compilation succeeds.
- `compileGroup` compiles aggregate roots.
- `compileRowRoot` compiles non-aggregate SELECT roots with projection, ordering, distinct, and slicing wrappers.
- `compileTuple` handles algebra nodes, including `Join`, `LeftJoin`, `Union`, `Difference`, `Filter`, `Exists`, and statement patterns.
- `rightOnlyBindingNames(LeftJoin)` computes right-side names not bound by the left side, but does not include condition-only variables and therefore is not sufficient for the Stage 1 well-designedness check.
- `JoinCursor` already has retained `NativeProbe` support for `PatternPlan` and materialize-and-replay support when the right side does not read left-produced slots.
- `LeftJoinCursor` currently opens `right.open(row)` for every left row and only tracks whether the current left row matched.
- `memoReadMask` describes which slots a plan reads. It handles `PatternPlan`, `ValuesPlan`, `MultiValuePatternPlan`, `MultiJoinPlan`, `JoinPlan`, `UnionPlan`, and `FilterPlan`; it currently returns unknown for `LeftJoinPlan`.
- `PatternMembershipProbe` is a materialized existence probe for membership-style `MINUS` and `EXISTS` plans. Its measured win came from scanning once instead of probing per row.
- `FactorizedTail.Branch` contains an adaptive scan-once cost model. It flips from repeated seeks to a single scan when repeated probe cost exceeds the static pattern estimate.

RDF4J's generic OPTIONAL implementation is in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/LeftJoinQueryEvaluationStep.java`. It computes optional-only variables from the right argument and the condition, compares them against external bindings, and dispatches badly-designed cases to the generic-safe path. Stage 1 mirrors that safety boundary.

## Plan of Work

Stage 1 is the correctness prerequisite. Add `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeLeftJoinWellDesignedTest.java` with differential tests that compare native-on to native-off. The tests must include a badly-designed sibling OPTIONAL shape, a row-stream root version of the same shape, a prepared query with an external binding on an optional-only variable, and a condition-variable case where the variable appears only in the OPTIONAL condition. The test must also assert that REAL_ESTATE native coverage does not shrink versus `rdf4j.lmdb.leftjoin.wellDesignedCheck=false`, using `LmdbNativeAggregateCompiler.COMPILED`, so the static refusal does not become too conservative.

Implement Stage 1 in `LmdbNativeAggregateCompiler.Compiler`. Before compiling either a group root or a row-stream root, run a pre-pass over the root tuple expression. For every `LeftJoin`, compute `optionalOnly` as all variable names in the right argument plus all variable names in the condition, minus names bound by the left argument. For each such name, collect variable names in the root tree while skipping this exact `LeftJoin` subtree. If an optional-only name appears outside the subtree, native compilation must return `null` so the generic evaluator handles the query. Otherwise, store all optional-only names in the compiled native step for dynamic evaluation-time checking.

For dynamic Stage 1 fallback, add a package-private `genericPrecompile(TupleExpr, QueryEvaluationContext)` method to `LmdbNativeEvaluationStrategy` that calls `super.precompile(expr, context)`. Thread `strategy`, `originalExpr`, `context`, and a compact `String[] optionalOnlyNames` into `NativeGroupStep` and `NativeRowsStep` only when optional-only names exist. In `evaluate(BindingSet bindings)`, if the incoming bindings bind any optional-only name, lazily create and reuse the generic step, then evaluate that generic step. If no optional-only names exist, or no external binding touches them, keep today's native path. Gate this behavior with `rdf4j.lmdb.leftjoin.wellDesignedCheck`, default `true`; when set to `false`, the native compiler should use today's behavior for debugging.

Stage 2 brings `LeftJoinCursor` to parity with `JoinCursor`. First, add retained `NativeProbe` handling for `PatternPlan` right sides and close the probe in `LeftJoinCursor.close`. Second, add uncorrelated materialize-and-replay under `rdf4j.lmdb.leftjoin.replay.enabled`, default `true`. The constructor should compute `readMask = memoReadMask(right)` and enable replay only when the right side reads no slot produced by the left side. The first left row streams from the live right cursor while recording right-produced slots up to `MAX_MATERIALIZED_ROWS`. Later left rows replay those recorded values with mark/bind/rollback logic; if no replayed row binds successfully, the left row is null-extended. Empty materialized right side means every later left row null-extends immediately. Third, teach `memoReadMask` that `LeftJoinPlan` reads the union of its left and right read masks, unless either is unknown. The `LeftJoinPlan.open` method must pass `left.producedMask()` into the cursor the same way `JoinPlan` does.

Stage 3 adds the substantive hash-left-join path for common correlated single-pattern OPTIONALs. Add a private `PatternPayloadProbe` near `PatternMembershipProbe`. It applies only when the right side is a single eligible `PatternPlan`, or a `FilterPlan` wrapping an eligible `PatternPlan` with a known filter mask. Per left row, exactly one varying pattern slot must already be bound; that slot is the key, and remaining varying slots are payloads. After an adaptive threshold controlled by `rdf4j.lmdb.leftjoin.hash.minProbes` (default `1024`), build a long-key to payload-row arena by scanning the pattern once with `openRawUnbinding(row, 1L << keySlot, probe)`. The payload arena is capped by `rdf4j.lmdb.leftjoin.hash.maxRows` (default `1000000`). On payload overflow, degrade to a miss-only key set; if the key set also exceeds the existing membership cap, disable the hash path and continue with Stage 2 INLJ behavior. Gate the whole payload path with `rdf4j.lmdb.leftjoin.hash.enabled`, default `true`.

Stage 3 also adds a lazy per-key memo for correlated multi-pattern right sides under `rdf4j.lmdb.leftjoin.memo.enabled`, default `true`. Use the right side's known read mask to build a `GroupKey` from correlation slot values. Cache recorded right rows per key with a too-large sentinel. This is intentionally lazy; eager unbound scanning of arbitrary multi-pattern rights is out of scope because it can materialize unbounded intermediate rows.

Stage 5 removes the Roaring membership prototype branch and dependency. Update membership tests so they compare `off` and `hash` modes plus generic reference. Keep the `LongMembership` abstraction if it still keeps the code clean, but shrink the property `rdf4j.lmdb.membership.impl` to `hash|off`.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`.

Start every work session with the required quick install:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Stage 1 red test:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinWellDesignedTest#badlyDesignedSiblingFallsBackForGroupRoot --module core/sail/lmdb --retain-logs

Expected before the fix: the test fails because native-on and native-off results differ, or because a compile-count assertion shows native accepted a query that must fall back.

Stage 1 focused green:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinWellDesignedTest --module core/sail/lmdb --retain-logs

Stage 1 regression gates:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRealEstateNativeParityTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeRowStreamTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeMembershipJoinTest --module core/sail/lmdb --retain-logs

Stage 2 focused tests, after adding `LmdbNativeLeftJoinCursorTest`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinCursorTest --module core/sail/lmdb --retain-logs

Stage 3 focused tests, after adding `LmdbNativeLeftJoinHashTest`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinHashTest --module core/sail/lmdb --retain-logs

Formatting and hygiene before handoff:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    git diff --check

The formatter command uses `-q`, but it is not a test command. Never use `-q` or `-am` for test runs.

## Validation and Acceptance

Stage 1 is accepted when the new well-designedness tests fail before the implementation, pass after it, and the same tests pass with `rdf4j.lmdb.leftjoin.wellDesignedCheck=false` only for the cases that intentionally demonstrate today's native behavior. The REAL_ESTATE parity test must still show all 13 queries matching generic results, and its native compilation assertions must confirm that Stage 1 did not shrink baseline native coverage.

Stage 2 is accepted when uncorrelated OPTIONAL differential tests cover matches, no matches, fanout greater than one, condition-wrapped right sides, bind conflicts during replay, and OPTIONAL nested under Join, MINUS, and EXISTS. The tests must pass with replay enabled and with `rdf4j.lmdb.leftjoin.replay.enabled=false`, proving the kill switch returns to the Stage 1 path.

Stage 3 is accepted when hash tests force `rdf4j.lmdb.leftjoin.hash.minProbes=0`, observe `LEFTJOIN_HASH_BUILDS`, and prove correct hit, miss, fanout, condition, overflow-degrade, exclusion, and multi-pattern memo cases. The full native gate at this point is `LmdbRealEstateNativeParityTest`, `RealEstateLmdbQueryCountIT`, `LmdbNativeMembershipJoinTest`, `LmdbNativeFactorizedTailAggregationTest`, `LmdbNativeRowStreamTest`, and `LmdbNativeSlotLayoutIntegrationTest`.

Performance acceptance for Stage 2 and Stage 3 uses the same theme-query instruments already used by the REAL_ESTATE optimization plan: `LmdbRealEstateQueryTimingTest` for the quick inner loop and `ThemeQueryBenchmark` JMH runs at stage boundaries. Primary query targets are REAL_ESTATE q11/q12, ENGINEERING q8, SOCIAL_MEDIA q8, and HIGHLY_CONNECTED q1. Regression watch targets are REAL_ESTATE q2/q3 and membership-heavy q3/q4/q7.

## Idempotence and Recovery

All planned changes are ordinary tracked source or test files. Rerunning the tests is safe. Runtime kill switches give a quick way to isolate a bad optimization without reverting unrelated correctness work:

- `rdf4j.lmdb.leftjoin.wellDesignedCheck=false`
- `rdf4j.lmdb.leftjoin.replay.enabled=false`
- `rdf4j.lmdb.leftjoin.hash.enabled=false`
- `rdf4j.lmdb.leftjoin.hash.minProbes=<n>`
- `rdf4j.lmdb.leftjoin.hash.maxRows=<n>`
- `rdf4j.lmdb.leftjoin.memo.enabled=false`

If a Stage 1 implementation accidentally changes production code before a failing in-repo test is captured, stop, revert that production edit, and resume from the failing test. If a later performance path produces wrong results, keep the correctness gate in place and disable only that optimization path while diagnosing.

The repository may contain unrelated untracked plan or summary files. Do not delete them. Before staging or committing, inspect `git status --short` and stage only the files created or modified for this plan.

## Artifacts and Notes

Initial root quick install evidence from 2026-07-03:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    Report: maven-build.log
    Snippet:
    [INFO] LmdbStore ................................... SUCCESS [  3.092 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time: 27.611 s

Milestone 3.5 measurement from the existing REAL_ESTATE optimization plan:

    q3 off/hash/roaring: 60.1 / 47.1 / 49.0 ms
    q4 off/hash/roaring: 97.3 / 79.4 / 74.8 ms
    q7 off/hash/roaring: 52.1 / 38.6 / 39.8 ms

This supports keeping the materialization idea while dropping the Roaring dependency.

Stage 1 red/green evidence:

    Red command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinWellDesignedTest#badlyDesignedSiblingFallsBackForGroupRoot --module core/sail/lmdb --retain-logs
    Red snippet: Tests run: 1, Failures: 1, Errors: 0, Skipped: 0; expected COMPILED delta 0L but was 1L.
    Green command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinWellDesignedTest#badlyDesignedSiblingFallsBackForGroupRoot --module core/sail/lmdb --retain-logs
    Green snippet: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
    Focused suite: LmdbNativeLeftJoinWellDesignedTest 5/5 green.
    Existing gates: LmdbRealEstateNativeParityTest 13/13 green; LmdbNativeRowStreamTest 16/16 green; LmdbNativeMembershipJoinTest 6/6 green; unit-only LmdbNative*Test 100/100 green.

Stage 2 red/green evidence:

    Red command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinCursorTest#uncorrelatedOptionalMaterializesAndReplaysFanout --module core/sail/lmdb --retain-logs
    Red snippet: Tests run: 1, Failures: 1, Errors: 0, Skipped: 0; expected LEFTJOIN_REPLAY_MATERIALIZATIONS to increase, but it stayed 0.
    Green command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinCursorTest#uncorrelatedOptionalMaterializesAndReplaysFanout --module core/sail/lmdb --retain-logs
    Green snippet: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
    Focused suite: LmdbNativeLeftJoinCursorTest 4/4 green.
    Existing gates: LmdbNativeLeftJoinCursorTest, LmdbNativeLeftJoinWellDesignedTest, LmdbRealEstateNativeParityTest, LmdbNativeRowStreamTest, LmdbNativeMembershipJoinTest, LmdbNativeFactorizedTailAggregationTest, and LmdbNativeSlotLayoutIntegrationTest 71/71 green.
    Broad native unit sweep: LmdbNative*Test with -DskipITs 104/104 green.

Stage 3a/3b red/green evidence:

    Red command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinHashTest#correlatedPatternOptionalBuildsPayloadHash --module core/sail/lmdb --retain-logs
    Red snippet: Tests run: 1, Failures: 1, Errors: 0, Skipped: 0; expected LEFTJOIN_HASH_BUILDS to increase, but it stayed 0.
    Green command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinHashTest#correlatedPatternOptionalBuildsPayloadHash --module core/sail/lmdb --retain-logs
    Green snippet: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
    Focused suite: LmdbNativeLeftJoinHashTest 5/5 green.
    Existing gates: LmdbNativeLeftJoinHashTest, LmdbNativeLeftJoinCursorTest, LmdbNativeLeftJoinWellDesignedTest, LmdbRealEstateNativeParityTest, LmdbNativeRowStreamTest, LmdbNativeMembershipJoinTest, LmdbNativeFactorizedTailAggregationTest, and LmdbNativeSlotLayoutIntegrationTest 76/76 green.
    Post-format native unit sweep: LmdbNative*Test with -DskipITs 109/109 green.
    Post-format REAL_ESTATE parity: LmdbRealEstateNativeParityTest 13/13 green.
    Post-format REAL_ESTATE count IT: RealEstateLmdbQueryCountIT 1/1 green.

Stage 3c red/green evidence:

    Red command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinHashTest#multiPatternOptionalMemoizesRepeatedCorrelationKey --module core/sail/lmdb --retain-logs
    Red snippet: Tests run: 1, Failures: 1, Errors: 0, Skipped: 0; NoSuchFieldException: LEFTJOIN_MEMO_MATERIALIZATIONS.
    Green command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinHashTest#multiPatternOptionalMemoizesRepeatedCorrelationKey --module core/sail/lmdb --retain-logs
    Green snippet: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
    Focused suite: LmdbNativeLeftJoinHashTest 6/6 green.
    Post-format native unit sweep: LmdbNative*Test with -DskipITs 110/110 green.
    Post-format REAL_ESTATE parity: LmdbRealEstateNativeParityTest 13/13 green.
    Post-format REAL_ESTATE count IT: RealEstateLmdbQueryCountIT 1/1 green.

Stage 5 evidence:

    Search command: rg -n "roaring|Roaring|RoaringBitmap" core/sail/lmdb pom.xml core/sail/lmdb/pom.xml
    Search snippet: no matches.
    Green command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeMembershipJoinTest --module core/sail/lmdb --retain-logs
    Green snippet: Tests run: 6, Failures: 0, Errors: 0, Skipped: 0.
    Final native unit sweep: LmdbNative*Test with -DskipITs 110/110 green.
    Final REAL_ESTATE parity: LmdbRealEstateNativeParityTest 13/13 green.
    Final REAL_ESTATE count IT: RealEstateLmdbQueryCountIT 1/1 green.

Review fix red/green evidence:

    Red command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinCursorTest#absentConstantOptionalRightNullExtendsLeftRows --module core/sail/lmdb --retain-logs
    Red snippet: Tests run: 1, Failures: 1, Errors: 0, Skipped: 0; expected five null-extended left rows, but native returned [].
    Green command: python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeftJoinCursorTest#absentConstantOptionalRightNullExtendsLeftRows --module core/sail/lmdb --retain-logs
    Green snippet: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
    Focused suite: LmdbNativeLeftJoinCursorTest 5/5 green.
    Native unit sweep: LmdbNative*Test with -DskipITs 111/111 green.
    REAL_ESTATE parity: LmdbRealEstateNativeParityTest 13/13 green.
    REAL_ESTATE count IT: RealEstateLmdbQueryCountIT 1/1 green.

## Interfaces and Dependencies

New or changed Java interfaces for Stage 1:

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeEvaluationStrategy.java`, add:

    QueryEvaluationStep genericPrecompile(TupleExpr expr, QueryEvaluationContext context) {
        return super.precompile(expr, context);
    }

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregateCompiler.java`, add private compiler helpers with stable intent:

    private OptionalOnlyNames wellDesignedOptionalOnlyNames(TupleExpr root)
    private boolean isWellDesignedLeftJoinRoot(TupleExpr root, Set<String> optionalOnlyNames)
    private static Set<String> leftJoinOptionalOnlyNames(LeftJoin leftJoin)
    private static Set<String> bindingNamesOutside(TupleExpr root, QueryModelNode excluded)

The exact helper names may change to match local style, but the behavior must not: condition variables are included, each `LeftJoin` subtree is skipped when checking outside references, and static refusal returns `null`.

`NativeGroupStep` and `NativeRowsStep` need constructor arguments sufficient to do lazy dynamic fallback:

    private final LmdbNativeEvaluationStrategy strategy;
    private final TupleExpr originalExpr;
    private final QueryEvaluationContext context;
    private final String[] optionalOnlyNames;
    private QueryEvaluationStep genericStep;

The fields may be omitted or left null when `optionalOnlyNames.length == 0`, preserving the common path.

Stage 2 should add `NativeLmdbQuerySource.NativeProbe rightProbe` to `LeftJoinCursor` and an `openRight()` helper that mirrors `JoinCursor.openRight()`.

Stage 3 should add a test-observability counter near existing counters:

    static final AtomicLong LEFTJOIN_HASH_BUILDS = new AtomicLong();

Use no new external dependencies for Stage 3. Stage 5 removes the Roaring dependency rather than adding one.

## Change Notes

- 2026-07-03 / Codex: Initial ExecPlan created from the staged native left-join improvement request and current source inspection. The plan deliberately starts with Stage 1 correctness before performance work.
- 2026-07-03 / Codex: Stage 1 completed and documented. Static refusal was refined from "any outside variable reference" to "outside binder" after the REAL_ESTATE q0 coverage guard caught over-refusal; the coverage guard now compares native coverage with the well-designedness check off versus on.
- 2026-07-03 / Codex: Stage 2 completed and documented. `LeftJoinCursor` now has pattern probe reuse, uncorrelated replay with OPTIONAL null-extension semantics, and a replay kill switch. A separate absent-value-store-constant OPTIONAL issue was observed but kept out of Stage 2.
- 2026-07-03 / Codex: Stage 3a/3b completed and documented. The payload hash keeps primitive flat arrays, preserves NOT_APPLICABLE fallback behavior, and degrades oversized payload scans to a miss-only key set before disabling.
- 2026-07-03 / Codex: Stage 3c completed and documented. The lazy memo path is limited to correlated, known-mask, non-single-pattern rights; oversized per-key row streams fall back to the existing INLJ path.
- 2026-07-03 / Codex: Stage 5 completed and documented. The Roaring prototype dependency and property branch were removed; membership differential tests now cover the supported `off` and `hash` modes.
- 2026-07-03 / Codex: Review follow-up fixed the absent-constant OPTIONAL bug discovered during Stage 2. Empty statement patterns are now local empty subplans instead of a compiler-wide empty-root state.
