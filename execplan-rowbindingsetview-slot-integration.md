# Integrate RowBindingSetView with Precompiled Slot Indices

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` in this repository. It is intentionally self-contained: a future worker should
be able to restart from this file, the current working tree, and the commands below.

## Purpose / Big Picture

The LMDB native query engine keeps intermediate query rows as primitive LMDB value ids in fixed slot arrays. Today those
rows are exposed to surrounding RDF4J operators through `BindingSet` name lookups, so nested native fragments and generic
operators above native fragments repeatedly translate variable names to slots at runtime. After this work, the native
engine will decide query-wide variable indices once during precompile, map each plan's dense slot layout to that
query-wide layout, and read native row views by index wherever possible. The observable behavior should stay the same:
native-enabled and native-disabled query results match, plans stay semantically unchanged, and targeted LMDB regression
tests pass. The performance signal is that string scans, per-row `HashMap` construction, and repeated `idOf` conversion
disappear from slot seeding and parent-operator access paths.

## Progress

- [x] (2026-07-03 11:44Z) Read `.agent/PLANS.md`, the repo AGENTS instructions, and the relevant skills.
- [x] (2026-07-03 11:44Z) Confirmed branch state: `optimize-lmdb...origin/optimize-lmdb` with no tracked dirt in
  `git status --short --untracked-files=no`.
- [x] (2026-07-03 11:44Z) Ran the required root quick install before tests:
  `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`.
- [x] (2026-07-03 11:44Z) Inspected current native query, aggregate, evaluation-context, and query-wide context code.
- [x] (2026-07-03 11:48Z) Captured the current `core/sail/lmdb` baseline with retained logs and persisted compact
  evidence to `initial-evidence.txt`.
- [x] (2026-07-03 11:55Z) Added `LmdbNativeSlotLayoutIntegrationTest` first and captured the intended focused red
  before production edits.
- [x] (2026-07-03 12:01Z) Introduced `QueryWideVarLayout`, `LmdbQueryEvaluationContext`,
  `NativeSlotLayout`, `NativeRowSeeder`, and the shared top-level `RowBindingSetView`.
- [x] (2026-07-03 12:01Z) Threaded `NativeSlotLayout` through `LmdbNativeQueryCompiler` and
  `LmdbNativeAggregateCompiler`.
- [x] (2026-07-03 12:01Z) Added the root `LmdbNativeEvaluationStrategy.precompile(TupleExpr)` override and
  query-layout context wrapping.
- [x] (2026-07-03 12:01Z) Extended and guarded `SlotBindingSetView` / `SlotAwareQueryEvaluationContext`; bound filters
  now use compiled context accessors.
- [x] (2026-07-03 12:25Z) Ran copyright, formatting, focused tests, compliance probes, `core/sail/lmdb` verify, and
  diff hygiene.
- [x] (2026-07-03 12:31Z) Updated `RowBindingSetView` to maintain cached `isEmpty()` eagerly and cache `size()`
  lazily for stable and mutable views.
- [x] (2026-07-03 12:59Z) Refined `RowBindingSetView` empty maintenance to explicit slot mutation hooks instead of
  generic cache invalidation.

## Surprises & Discoveries

- Observation: This checkout already has package-private `SlotBindingSetView` and `SlotAwareQueryEvaluationContext`, but
  the latter stores only a plan-local `Map<String,Integer>` and applies a slot number to any `SlotBindingSetView`.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/SlotAwareQueryEvaluationContext.java` branches on
  `bs instanceof SlotBindingSetView` without a plan-layout identity check.
- Observation: The two row-view implementations are still nested and near-duplicate. The aggregate view has
  `sizeCache` and `invalidateTransientCaches()`, while the query compiler view has `Serializable` and a mutable-view
  `HashMap`.
  Evidence: `LmdbNativeQueryCompiler.RowBindingSetView` starts around line 1446; `LmdbNativeAggregateCompiler.FastRowBindingSetView`
  starts around line 4159.
- Observation: `DefaultEvaluationStrategy.precompile(TupleExpr)` builds `ArrayBindingBasedQueryEvaluationContext` only
  when the root is a `QueryRoot`, using the public static
  `ArrayBindingBasedQueryEvaluationContext.findAllVariablesUsedInQuery(QueryRoot)` method.
  Evidence: `core/queryalgebra/evaluation/.../DefaultEvaluationStrategy.java` around line 447 and
  `ArrayBindingBasedQueryEvaluationContext.java` around line 420.
- Observation: The current LMDB module baseline has one failure, not the older 28-failure note from prior memory.
  Evidence: `initial-evidence.txt` records `tests=1172, failures=1, errors=0, skipped=3`; the failing method is
  `LmdbEvaluationStatisticsMemoizationTest.recordsLearnedFilterPassRatioForExternalBoundPatternLocalFilter`.
- Observation: The black-box slot-layout integration scenarios are already green on current code, but the focused
  slot-aware context guard is red.
  Evidence: `LmdbNativeSlotLayoutIntegrationTest` reports `tests=6, failures=1`; the failure is
  `slotAwareContextFallsBackForForeignSlotView`, expected `"fallback"` but read `"wrong-slot"` through the foreign slot.
- Observation: The native-enabled `LmdbSPARQL11QueryComplianceTest` suite still has broader native-engine failures after
  this patch, while the same selector with `-Drdf4j.lmdb.nativeQueryEngine.enabled=false` is green.
  Evidence: native-enabled reports `tests=176, failures=5, errors=6`; native-disabled reports
  `tests=176, failures=0, errors=0, skipped=0`.

## Decision Log

- Decision: Keep plan-local native slots dense and add query-wide mapping arrays instead of replacing slot numbering
  with query-wide indices.
  Rationale: Native filters, produced masks, and bound masks use 64-bit plan-local slot masks with `MAX_NATIVE_SLOTS=60`;
  query-wide variables are unbounded and would break that representation.
  Date/Author: 2026-07-03 / Codex
- Decision: Treat `NativeSlotLayout` object identity as the plan-layout identity token and `QueryWideVarLayout` object
  identity as the query-layout identity token.
  Rationale: The engine needs to reject views from different plan layouts or previous queries quickly and fall back to
  name-based access when identities differ.
  Date/Author: 2026-07-03 / Codex
- Decision: Put new helper classes in `org.eclipse.rdf4j.sail.lmdb` with package-private visibility.
  Rationale: The change is entirely inside `core/sail/lmdb`; no core/queryalgebra API change is needed.
  Date/Author: 2026-07-03 / Codex

## Outcomes & Retrospective

Implemented the slot-index integration inside `core/sail/lmdb`. The two nested row views are now one top-level
`RowBindingSetView`, backed by `NativeSlotLayout`. Native and aggregate seeding both use `NativeRowSeeder`; matching
native views seed through query-index and native-id arrays, while mismatched layouts, previous-query views, plain
`BindingSet`s, and different id spaces fall back to name/value access. Parent generic operators now precompile against
`LmdbQueryEvaluationContext`, so matching native views are consumed by query-wide index. `SlotAwareQueryEvaluationContext`
is guarded by plan-layout identity and delegates foreign-plan views back to the query-wide context.
`RowBindingSetView.isEmpty()` is maintained eagerly at construction and by explicit slot mutation hooks:
`slotBound()`, `slotCleared()`, and `slotsReplaced()`. `size()` is cached lazily and evicted when those actual row
changes happen.

The focused guard regression went red before production edits and green after the implementation. Focused LMDB native
row/group/bound/parity/regression suites are green. Full `core/sail/lmdb` verification still has the same single
baseline failure captured before the patch:
`LmdbEvaluationStatisticsMemoizationTest.recordsLearnedFilterPassRatioForExternalBoundPatternLocalFilter`. The module
test count increased from 1172 to 1178 because `LmdbNativeSlotLayoutIntegrationTest` was added.

`LmdbSPARQL11QueryComplianceTest` with the native engine enabled still exposes broader native-engine compliance failures
(`tests=176, failures=5, errors=6`), while the same selector with the native engine disabled is green. An exploratory
diagnosis pointed at native handling around a `BindingSetAssignment` / `VALUES` join shape; that is outside this
slot-layout patch and no exploratory production changes were kept.

Benchmarks and JFR profiling were not run in this pass. The semantic proof is the red/green integration selector,
focused native suites, formatting/copyright checks, and unchanged broad LMDB baseline identity.

## Context and Orientation

The module is `core/sail/lmdb`. The native query engine compiles selected SPARQL algebra fragments into slot plans that
store row values as LMDB value ids. A slot is a small plan-local integer for one variable name, and a row is a `long[]`
where `UNKNOWN` means absent and `NULL_CONTEXT_ID` (`0L`) means a present null graph/context binding. A `BindingSet` is
RDF4J's name-based view of query results. The native engine exposes slot rows as `BindingSet` views so generic RDF4J
operators can consume them.

The main files are:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeQueryCompiler.java`: basic graph-pattern native
  compiler, `NativePlan`, row seeding, simple filters, and the nested `RowBindingSetView`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeAggregateCompiler.java`: aggregate and row-root
  native compiler, slot plans, `initializeRow`, `NativeGroupIteration.initialize`, aggregate `RowState`, and the nested
  `FastRowBindingSetView`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeEvaluationStrategy.java`: strategy entry point
  that tries aggregate and normal native compilers before falling back to `StrictEvaluationStrategy`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/SlotBindingSetView.java`: current by-plan-slot access
  interface for native row views.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/SlotAwareQueryEvaluationContext.java`: current context
  wrapper for generic filters inside native plans.
- Read-only reference files are
  `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/ArrayBindingBasedQueryEvaluationContext.java`
  and
  `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`.

## Plan of Work

First, capture the current LMDB module baseline with `mvnf` and store compact evidence in `initial-evidence.txt`. The
branch is expected to have existing LMDB failures; compare later verification against this baseline rather than assuming
zero failures.

Second, add `LmdbNativeSlotLayoutIntegrationTest` before production edits. The test class will create small LMDB
repositories, load a compact fixture, evaluate native-enabled and native-disabled query variants, and compare canonical
binding rows. It must cover nested seeding, mixed-layout `UNION` consumed by a generic parent expression, `FILTER EXISTS`
with a generic operator over a nested native fragment, an external `evaluate(bindings)` case fed from a previous
differently-shaped query, and `BOUND(?external)`. The first focused run is expected to expose at least the latent
wrong-slot scenario or prove the characterization is already green before the refactor.

Third, introduce the package-private layout helpers. `QueryWideVarLayout` stores immutable query-wide variable names and
an `indexOf(String)` map. `NativeSlotLayout` stores a plan's `slotNames`, `slotsByName`, `nativeBindingNames`, optional
`queryLayout`, `slotToQueryIndex`, and `queryIndexToSlot`. `LmdbQueryEvaluationContext` wraps the
`ArrayBindingBasedQueryEvaluationContext` and returns accessors that read `SlotBindingSetView` by query index only when
`view.queryLayout() == layout`; otherwise they delegate. `NativeRowSeeder` seeds native row slots from base bindings and
uses the query-index/id fast path only when the base view shares query layout identity and LMDB id space.

Fourth, move the row view into top-level `RowBindingSetView` and delete the nested view classes. The shared view keeps
the aggregate-side `sizeCache` and `invalidateTransientCaches()` behavior, uses `NativeSlotLayout.slotsByName` for O(1)
name lookup on mutable and stable snapshots, exposes plan-layout and query-layout identity, and implements by-query-index
value/binding/native-id access. Native id lookup may recurse into a base view only when query layout identity and id
space both match; `NULL_CONTEXT_ID` remains present.

Fifth, thread `NativeSlotLayout` through both compilers. Builders create the layout after slots are known. Native plans,
row states, row-root steps, group steps, and slot plans receive the same layout object for identity. Seeding sites switch
to `NativeRowSeeder.seed(...)`, then recompute or derive bound masks exactly as before. Pattern ordering, masks, and
filter depths are not intentionally changed.

Sixth, add `LmdbNativeEvaluationStrategy.precompile(TupleExpr)` mirroring the default strategy's root behavior, but
wrap the array-binding context in `LmdbQueryEvaluationContext` and do not call `super.precompile(expr)` from the override.
The static variable collector mutates `QueryRoot` variable names into interned canonical strings and must run once per
root precompile. Direct callers of `precompile(expr, context)` still work with `queryLayout == null` and fall back to
existing behavior.

Seventh, guard `SlotAwareQueryEvaluationContext` with `NativeSlotLayout`. Its by-slot branches require
`view.planLayout() == planLayout`; otherwise they delegate. Since the delegate is now normally `LmdbQueryEvaluationContext`,
other native plans' views can still be read safely by query index. Bound filters capture
`Predicate<BindingSet> baseHas = context.hasBinding(name)` at compile time rather than calling `base.hasBinding(name)` by
string on every row.

Finally, run copyright, formatter, focused tests, module verification, and selected benchmarks or profiling commands as
time allows. Record all evidence and any baseline deltas in this plan.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j`.

The initial build command already completed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Capture baseline and persist compact evidence:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
    python3 scripts/agent-evidence.py --command "python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs" \
      --log logs/mvnf/<retained-lmdb-verify-log> core/sail/lmdb/target/surefire-reports \
      core/sail/lmdb/target/failsafe-reports > initial-evidence.txt

Run the new focused integration selector before production edits:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeSlotLayoutIntegrationTest --module core/sail/lmdb --retain-logs

After implementation, run the same selector, then the nearby suites named by the request:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeSlotLayoutIntegrationTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeGroupByTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeRowStreamTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeMembershipJoinTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeQueryBoundFilterTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRealEstateNativeParityTest --module core/sail/lmdb --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSPARQL11ComplianceRegressionTest --module core/sail/lmdb --retain-logs

Before final handoff, run copyright and formatting. The formatter command uses `-q`, but it is not a test command:

    scripts/checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    git diff --check

## Validation and Acceptance

Acceptance is behavioral equality and layout-safety:

Native-enabled and native-disabled results must match for the new integration scenarios. The `FILTER EXISTS` nested
native-fragment scenario must not read a nested plan's slot through an outer plan slot number. `BOUND(?external)` must
continue to see external base bindings. A base row retained from a previous query must be rejected by query-layout
identity and still resolve correctly through name fallback. `NULL_CONTEXT_ID` must remain a present binding in all native
id accessors.

Acceptance is also performance-shaped:

Native-to-native seeding between matching query layouts should use `slotToQueryIndex` and `nativeIdByQueryIndex` rather
than name scans and `source.idOf(value)`. Generic parent operators above native fragments should use
`LmdbQueryEvaluationContext` query-index accessors rather than string-name fallback for matching views. The fallback
paths remain in place for direct `precompile(expr, context)` callers, layout mismatches, previous-query views, non-native
binding sets, and different LMDB id spaces.

## Idempotence and Recovery

All test and install commands are safe to repeat. `mvnf` removes stale module test reports and performs the required root
quick install before verify. Do not use `-am` or `-q` for test commands. If offline dependency resolution fails, rerun the
same command once without `-o`, then return to offline mode. If broad LMDB verification shows the known baseline failures,
compare against `initial-evidence.txt` and retained logs instead of widening the patch.

Do not delete untracked artifacts. If production code is edited before the new integration test is run, stop and recover
by reverting only this task's production edits, then run the focused test first.

## Artifacts and Notes

Initial root quick install evidence:

    [INFO] Reactor Summary for Eclipse RDF4J 6.0.0-SNAPSHOT:
    [INFO] RDF4J: LmdbStore ................................... SUCCESS [  3.240 s]
    [INFO] BUILD SUCCESS
    [INFO] Total time:  28.381 s (Wall Clock)

## Interfaces and Dependencies

All new classes are package-private in `org.eclipse.rdf4j.sail.lmdb`:

- `QueryWideVarLayout`: immutable query-wide name/index mapping with `int indexOf(String name)`, `String nameOf(int qi)`,
  and `int size()`.
- `LmdbQueryEvaluationContext`: `QueryEvaluationContext` wrapper that delegates all methods except `hasBinding`,
  `getBinding`, and `getValue`.
- `NativeSlotLayout`: plan-local layout and query-wide mapping carrier with `slotNames`, `slotsByName`,
  `nativeBindingNames`, `queryLayout`, `slotToQueryIndex`, and `queryIndexToSlot`.
- `NativeRowSeeder`: seeding helper returning false when a plain base binding value has no id in the target LMDB source.
- `RowBindingSetView`: shared `AbstractBindingSet` implementation and `SlotBindingSetView` implementation.

`SlotBindingSetView` grows these package-private methods: `NativeSlotLayout planLayout()`,
`QueryWideVarLayout queryLayout()`, `boolean hasBindingByQueryIndex(int qi)`, `Value valueByQueryIndex(int qi)`,
`Binding bindingByQueryIndex(int qi)`, `long nativeIdByQueryIndex(int qi)`, and
`boolean sameIdSpace(NativeLmdbQuerySource source)`.

Revision note, 2026-07-03 / Codex: initial ExecPlan created from the user request plus direct source inspection of the
current `optimize-lmdb` checkout.

Revision note, 2026-07-03 / Codex: updated baseline progress after `core/sail/lmdb` verify produced one current failure
and `initial-evidence.txt` was written.

Revision note, 2026-07-03 / Codex: recorded the new integration test and focused red evidence for the unguarded
`SlotAwareQueryEvaluationContext` foreign-view access path.

Revision note, 2026-07-03 / Codex: updated final implementation outcome, verification evidence, and the remaining
native-enabled compliance red classification.

Revision note, 2026-07-03 / Codex: recorded the follow-up `RowBindingSetView` `isEmpty()` and `size()` cache update.

Revision note, 2026-07-03 / Codex: refined `isEmpty()` maintenance so generic cache eviction no longer recalculates it.
