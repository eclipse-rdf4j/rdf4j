# Make adaptive filter placement planning a memoized tree analysis

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.

## Purpose / Big Picture

Every SPARQL query with a FILTER is prepared through `DefaultEvaluationStrategy.precompile`, which since GH-6066 plans alternative placements for each filter before the first row flows. That planning currently grows roughly cubically in the query size: a basic graph pattern of 16 triple patterns followed by 16 FILTERs takes about 200 ms to prepare on a 2026 laptop, against 0.16 ms for the same pattern without the filters. After this change the same planning stays within a small constant factor of the plain prepare and grows at most quadratically, which is observable by running the test `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/AdaptiveFilterPlanningCostTest.java`: it fails before the change and passes after it, while every existing placement test keeps passing unchanged.

## Progress

- [x] (2026-10-11 19:00Z) Reproduction test `AdaptiveFilterPlanningCostTest` written on branch `GH-6066-filter-placement-precompile-cost` (off `origin/develop` 63322dbff3); red at develop HEAD (214 ms vs 0.156 ms, growth 108x for 4x size).
- [x] (2026-10-11 19:05Z) Baseline captured: `FilterPlacementAnalyzerTest` 40, `AdaptiveFilterPlacementTest` 34, `AdaptiveFilterPlacementControllerTest` 7, `FilterInValuesOptimizerTest` 8, all green before any production change.
- [x] (2026-10-11 19:40Z) Generalized `FilterPlacementRules`: a `Condition` summary (names, sub-query flag, EXISTS names, scope flag, memoized retained names) plus (gate argument, gate input frame) replace the physically inserted `Filter`; the old two-argument entry points delegate.
- [x] (2026-10-11 19:45Z) Replaced `FilterPlacementAnalyzer.Layout` with `Skeleton` (one clone, one never-invalidated analysis) and a per-filter `Probe` (memoized site frames, downward moves, gate inputs via a throw-away spliced gate). First result: 214 ms to 20 ms at n=16, all 89 pre-existing tests green, cost test still red (ratio 116, growth 31x).
- [x] (2026-10-11 20:05Z) Shared the skeleton across fact-transparent, non-scope-changing filters (the moving filter's own clone stands in for its argument as a child of a site); memoized `ReadOnlyContext.withOutput` by facts identity and `OutputFacts.only` by name set in `QueryAlgebraBindingAnalysis`; cached frame hash codes; combined memoized binding names for Join/LeftJoin/Union and pass-through unary operators. Result: 5.7 ms at n=16, ratio 31.5, growth 14.7x; 91 of 91 tests green.
- [x] (2026-10-11 20:10Z) Spotless applied to both modules; copyright check clean; scratch profiling test removed.
- [x] (2026-10-11 20:25Z) Full module verifies green: `core/queryalgebra/evaluation` 1330 tests, 0 failures, 1 skipped; `core/queryalgebra/model` 85 tests, 0 failures, 1 skipped.
- [x] (2026-10-11 20:35Z) Committed as 835288fb74 (test) and df0035e730 (fix); pushed; PR https://github.com/eclipse-rdf4j/rdf4j/pull/6144 against develop.
- [x] (2026-10-11 21:00Z) Phase 2 constant-factor trims measured on a 30-second steady-state loop of the n=16 shape: 9143 to 11900 precompiles (3.28 ms to 2.52 ms each, 23% faster), growth 14.7x to 11.1x; `disjoint` short-circuits single-gate cuts; the shared skeleton reuses the `isCurrent()` snapshot instead of a second clone; binding-name and retained-name memos hold immutable sets so downstream `Set.copyOf` is free; `retainedBindingNames` returns the candidate's names directly when they already cover the condition; `conditionVisibility` copies only when a name must be dropped; `ReadOnlyContext.equals` has an identity fast path. 91 of 91 placement tests green.
- [x] (2026-10-11 21:15Z) Phase 2 verified (evaluation 1330, model 85, zero failures), committed as 06b71aa735 and pushed.
- [x] (2026-10-11 21:45Z) Phase 3 memos measured on the same 30-second loop: 11900 to 14758 precompiles (2.52 ms to 2.03 ms each, 24% faster), growth 11.1x to 10.6x; single-gate `Cut`s are memoized per node and moving the only gate skips the list rebuild; `Condition` memoizes `Dependencies` per condition frame; `PlanningContext.ordinary` memoizes `adaptiveBoundaryReason` per join. 91 of 91 placement tests green.
- [x] (2026-10-11 21:55Z) Phase 3 verified (evaluation 1330 tests, zero failures), committed and pushed to the PR branch.

## Surprises & Discoveries

- Observation: the standalone precompile of fuzz-generator trees averages only 0.2 ms, yet the equivalence fuzz soak slowed 10x.
  Evidence: JFR method timing on the real soak path: 440 `Rdf4jEvaluator.evaluate` calls per judged pair; `precompile` averages 0.229 ms of the 0.244 ms per evaluate; `AdaptiveFilterEvaluationStep.planningContext` alone averages 0.120 ms.
- Observation: planning time grows roughly cubically for the ordinary "BGP then FILTERs" shape.
  Evidence: n=4 4.3 ms, n=8 36 ms, n=16 381 ms on the fuzz branch; without filters 0.02 to 0.16 ms.
- Observation: a Filter is transparent to the binding analysis: `QueryAlgebraBindingAnalysis.childInput(Filter, arg, input)` returns `input` unchanged, and the rules only read the gate's own input frame plus facts of nodes at or below the gate. The one position-dependent value is the gate's input when the gate sits as a Join's right operand (`joinRightInput` inspects the right child's scope flag and subtree).
- Observation: removing the per-step re-analysis alone was not enough. With one clone and one analysis per filter the stacked shape still grew 31x for a 4x query, because every (filter, site) pair rebuilt frames whose cost is the number of variables in scope, and every filter rebuilt the island's facts.
  Evidence: `[filter-planning] n=16 withFilters=20.135 ms plain=0.174 ms ratio=115.6` and `growth=31.3x` after the first rewrite; JFR then showed 57% of samples inside `ReadOnlyContext.withOutput`.
- Observation: `Filter.getBindingNames()` is not a plain pass-through: it adds the EXISTS sub-query's binding names when the condition is an `Exists`. Memoized name combination must exclude that case.
- Observation: upward moves (a filter proposed above its origin) read facts of the subtree that contained the filter, which the original Layout computed with the filter removed. Only filters whose condition contains `BOUND` or an `isIRI`-family test can change facts, so only those need a private clone.

## Decision Log

- Decision: keep the breadth-first enumeration of cuts (sets of gate sites) and the public `Envelope`, `Alternative`, `Site` API untouched; only make each enumeration step a constant-time memoized lookup.
  Rationale: the runtime adapter depends on the order of `singletonAlternatives()` and on the exact `Site` frames; a different enumeration would change behaviour that 40 analyzer tests pin down.
  Date/Author: 2026-10-11, Claude.
- Decision: use one private skeleton clone per filter, with that filter removed, instead of one shared clone with all filters present.
  Rationale: upward moves (a filter proposed above its original position) read facts of the subtree that contained the filter; with the filter still present those facts carry its own narrowing (`filterFacts`), which the current implementation never sees because the Layout removes the filter first. One clone per filter is O(n) per filter and keeps results identical.
  Date/Author: 2026-10-11, Claude.
- Decision: compute the gate's input frame by splicing a throw-away `Filter` node above the site for the duration of one `childInput` call, and memoize the result per site.
  Rationale: `joinRightInput` and `childInput(LeftJoin, right)` inspect the child's scope flag and subtree; reproducing that logic in the analyzer would duplicate it and drift. The splice is O(1), touches no analysis cache except adding one entry keyed by the throw-away node, and is undone immediately.
  Date/Author: 2026-10-11, Claude.

- Decision: share one skeleton (clone plus analysis) across every filter that neither changes variable scope nor narrows facts, and give only the other filters a private clone with themselves detached.
  Rationale: with one clone per filter the analysis of the island was rebuilt F times, which kept the stacked-filter shape super-quadratic (31x growth for a 4x query). A filter whose condition contains no `BOUND` or `isIRI`-family node adds no facts (`applyFilterFacts` handles nothing else), so every other node's facts are identical with or without it; the only remaining difference is structural, and that is confined to the moving filter appearing as a child of a site, which `Probe.target` maps to its argument (and `rightKeepsScopeRoot` is computed on that argument). Scope-changing filters still differ at a Join's right operand, so they keep the private clone.
  Date/Author: 2026-10-11, Claude.
- Decision: memoize inside `QueryAlgebraBindingAnalysis` rather than only in the analyzer: `ReadOnlyContext.withOutput` by facts identity, `OutputFacts.only` by projected name set, and the frame hash code.
  Rationale: frame construction copies eight collections twice; with a shared skeleton the same (input, facts) and (facts, names) pairs recur for every filter and candidate, and the rules call these through public methods the analyzer cannot intercept. All three are pure functions of immutable inputs, so the caches cannot change results; they are synchronized because frames may be shared.
  Date/Author: 2026-10-11, Claude.
- Decision: combine binding names from memoized operands only for `Join`, `LeftJoin`, `Union` and the pass-through unary operators (`Distinct`, `Reduced`, `Slice`, `Order`, `QueryRoot`, and `Filter` without an EXISTS condition), matched by exact class.
  Rationale: the model recomputes a Join's names from both operands on every call, so one names lookup per node was quadratic in the chain depth. `Filter.getBindingNames` adds the EXISTS sub-query's names, so that case falls back to the model; subclasses may override, so exact class checks are required.
  Date/Author: 2026-10-11, Claude.

## Outcomes & Retrospective

Planning a 16-pattern BGP with 16 FILTERs went from 214 ms to 5.7 ms (37x), and from 108x to 14.7x growth for a 4x larger query; in steady state (JIT warm) one such precompile takes about 4.7 ms. The 89 pre-existing placement tests pass unchanged, which was the point of keeping the enumeration order and the `Site` frames bit-for-bit: the rewrite only changed how each step is computed. The first, pure "one clone per filter" version already removed the cubic re-analysis but still failed the quadratic bound; the decisive step was recognizing that fact-transparent filters can share one analysis, after which memoizing frame construction in the model paid off across filters. Remaining cost is dominated by `bindingsStableAt`, which still materializes a destination frame per candidate; making it check only the dependency names would be the next constant-factor win if ever needed.

    [filter-planning] n=16 withFilters=5.707 ms plain=0.181 ms ratio=31.5
    [filter-planning] n=4 0.343 ms, n=16 5.024 ms, growth=14.7x
    [mvnf] Summary: tests=91, failures=0, errors=0, skipped=0, time=0.931s

After the phase-2 trims (steady-state loop of 30 seconds, n=16; 9143 precompiles before, 11900 after):

    [profile] precompiles=11900
    [filter-planning] n=16 withFilters=5.392 ms plain=0.188 ms ratio=28.8
    [filter-planning] n=4 0.377 ms, n=16 4.168 ms, growth=11.1x

After the phase-3 memos (same loop, 14758 precompiles):

    [profile] precompiles=14758
    [filter-planning] n=16 withFilters=3.883 ms plain=0.188 ms ratio=20.7
    [filter-planning] n=4 0.341 ms, n=16 3.613 ms, growth=10.6x

What remains is spread thin: first-time frame and facts construction for the island (inherent, about a fifth), the per-site rule evaluations, and the standard prepare path's own recursive `getBindingNames` calls in `JoinQueryEvaluationStep.isNoNewBindingGuard` and `FilterIterator.supply` (about a tenth, outside the planner).

## Context and Orientation

The query algebra is a tree of `TupleExpr` nodes (`core/queryalgebra/model`). A `Filter` node has a condition (a `ValueExpr`) and one argument subtree. "Placement" means where along the tree a filter's condition is evaluated; pushing it closer to the leaves lets fewer rows flow. A "gate" is a hypothetical Filter node carrying the same condition at a candidate position; a "cut" is a set of gates that together cover every output path of the original filter (for example one gate in each UNION arm); a "site" is the node a gate would wrap.

`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/FilterPlacementAnalyzer.java` enumerates legal placements. `FilterPlacementRules.java` in the same package holds the semantic checks (can the filter move into a join child, across a union, through an extension, and so on). Both consult `core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/helpers/QueryAlgebraBindingAnalysis.java`, which memoizes two facts per node: the input frame flowing into a node (`contextAt`) and the output facts a node produces given an input (`outputFacts`). `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/AdaptiveFilterEvaluationStep.java` consumes the analyzer during `DefaultEvaluationStrategy.precompile` (its `PlanningContext` constructor and `prepare` method).

The current `FilterPlacementAnalyzer.Layout` clones the whole subtree, builds a fresh analysis, physically inserts gates at every cut it examines and calls `bindingAnalysis.invalidate()` after each insertion, which clears all memoized facts. Each filter gets four separate Layouts (envelope search, `alternatives()`, `earliestCompleteCut`, `potentialSites`). That is the cubic cost.

## Plan of Work

In `FilterPlacementRules.java`, add a constructor and a static `conditionVisibility` overload taking the logical `Filter` (for its condition and scope flag), the `TupleExpr` the gate would wrap, the gate's input frame, the analysis, and a `Function<TupleExpr, Set<String>>` that returns a node's binding names. Replace every use of `filter.getArg()` with the gate argument, every `TupleExprs.isVariableScopeChange(filter)` with the precomputed flag, `FilterIterator.isPartOfSubQuery(filter)` with the same check on the gate argument (its ancestors are the gate's ancestors), and every `getBindingNames()` call on tuple nodes with the supplied function. In `retainedBindingNames`, start the upward walk from the candidate with the first iteration using the logical filter's scope flag, and treat `child == logical` as a non-breaking step; this reproduces the original walk exactly for both a materialized filter and a prospective gate. Keep the old two-argument constructor and `conditionVisibility(Filter, analysis)` as delegates.

In `FilterPlacementAnalyzer.java`, add a per-analyzer `bindingNames(TupleExpr original)` memo. Replace `Layout` with `Probe`: its constructor clones the snapshot once, maps originals to clones, detaches the moving filter's clone (keeping it as `logical`), and builds one `QueryAlgebraBindingAnalysis.withInputContextAndPossibleInputs(scratch, rootInput, Set.of())`. `Probe.site(original)` returns a memoized `Site` from the new `conditionVisibility` overload with `gateInput(siteClone)`. `Probe.downward(original)` is the existing operator switch, memoized, with a `FilterPlacementRules` built for that site. `gateInput` splices a throw-away gate above the site, calls `analysis.childInput(parent, gate, contextAt(parent))`, undoes the splice and memoizes. `Envelope` creates one Probe and shares it with `Search`, `potentialSites` and `earliestCompleteCut`; `reverseMoveIsLegal` becomes `probe.downward(parent)` containing the replaced sites; `earliestCompleteCut` tracks the current cut in a local list instead of reading `layout.filters`.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs -- -Dtest='FilterPlacementAnalyzerTest,AdaptiveFilterPlacementTest,AdaptiveFilterPlacementControllerTest,FilterInValuesOptimizerTest,AdaptiveFilterPlanningCostTest' -Dsurefire.failIfNoSpecifiedTests=false

Expected before the change: 89 passed, 2 failed (both in `AdaptiveFilterPlanningCostTest`). Expected after: 91 passed.

    mvn -o -Dmaven.repo.local=.m2_repo -q -Pformatting spotless:apply -pl core/queryalgebra/evaluation
    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs

## Validation and Acceptance

`AdaptiveFilterPlanningCostTest` prints `[filter-planning] n=16 withFilters=... plain=... ratio=...`; acceptance is the ratio at or below 50 and the n=4 to n=16 growth at or below 24x. The 89 pre-existing placement tests must stay green, and the whole `core/queryalgebra/evaluation` module verify must report zero new failures.

## Idempotence and Recovery

All edits are confined to two production files and are safe to repeat. If any analyzer test fails after the change, the semantics of one rule differ between the materialized and prospective gate; compare against the original Layout behaviour described above and fix the emulation rather than the test.

## Artifacts and Notes

Evidence snippets are appended below as work proceeds.

## Interfaces and Dependencies

No new dependencies. Public API of `FilterPlacementAnalyzer` (`Site`, `Alternative`, `Envelope`, `snapshotRoots`, `analyze`, `analyzeAll`, `isCurrent`) is unchanged. `FilterPlacementRules` stays package-private and gains:

    FilterPlacementRules(Filter logical, TupleExpr gateArgument,
        QueryAlgebraBindingAnalysis.ReadOnlyContext gateInput,
        QueryAlgebraBindingAnalysis analysis, Function<TupleExpr, Set<String>> bindingNames)
    static ConditionVisibility conditionVisibility(Filter logical, TupleExpr gateArgument,
        QueryAlgebraBindingAnalysis.ReadOnlyContext gateInput,
        QueryAlgebraBindingAnalysis analysis, Function<TupleExpr, Set<String>> bindingNames)
