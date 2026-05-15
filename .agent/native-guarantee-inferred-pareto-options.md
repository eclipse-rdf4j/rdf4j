# Native guarantee-inferred Pareto planner options

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan follows `.agent/PLANS.md` from the repository root. It is self-contained for a new agent working in `/Users/havardottestad/Documents/Programming/rdf4j`.

## Purpose / Big Picture

The LMDB Sail currently applies predicate-object guarantee rewrites eagerly, before the LMDB sketch/Pareto join planner has a chance to compare alternatives. That is too strong: a finite `VALUES` anchor can be semantically valid but still slower than leaving a selective local filter attached to a statement pattern, as shown by the MEDICAL_RECORDS query 7 plan where the `VALUES ?code` path is costed and ordered badly.

After this change, predicate-object guarantees become facts that feed the planner. The planner can create multiple valid alternatives for the same logical filter, cost each with LMDB page-walking and filter estimates, and select the best frontier plan. A user can see the behavior through optimized query explanations: plan telemetry should show generated guarantee option groups, selected variants, and final frontier alternatives; MEDICAL_RECORDS query 7 should be able to keep the original `FILTER` variant when it is cheaper than the finite anchor.

## Progress

- [x] (2026-05-11 00:42+02:00) Confirmed branch `GH-0000-lmdb-predicate-guarantees`, not main.
- [x] (2026-05-11 00:42+02:00) Ran required root quick install; build succeeded.
- [x] (2026-05-11 00:47+02:00) Created this living ExecPlan before production edits.
- [x] (2026-05-11 00:48+02:00) Added and observed first failing domain test for resource observed joins.
- [x] (2026-05-11 01:08+02:00) Implemented first abstract-domain slice: `UNKNOWN`, `EMPTY`, `joinObserved`, `meetRequired`, and version-5 long encoding.
- [x] (2026-05-11 01:08+02:00) Verified `PredicateObjectGuaranteeTest` with ITs skipped; 10 tests green.
- [x] (2026-05-11 01:31+02:00) Added finite exact-value domain support and verified 12 domain tests green.
- [x] (2026-05-11 01:44+02:00) Added high-fanout planner test showing eager `VALUES` removes the cheaper original filter shape.
- [x] (2026-05-11 01:47+02:00) Disabled eager object-position literal anchor materialization as a first native-option transition slice.
- [x] (2026-05-11 01:43+02:00) Removed eager `LmdbGuaranteeFilterOptimizer` from the LMDB pipeline.
- [x] (2026-05-11 01:50+02:00) Added a first LMDB planner-time finite-anchor option provider and proved a selective predicate chooses the anchor only when cheaper.
- [x] (2026-05-11 01:52+02:00) Re-ran the high-fanout regression; it keeps the original filter alternative.
- [x] (2026-05-11 02:06+02:00) Added first equality planner options: `SameTerm` and assured disjoint-domain `EmptySet`.
- [x] (2026-05-11 02:32+02:00) Added guard equality variants and selected-filter clone bookkeeping.
- [x] (2026-05-11 02:55+02:00) Added type/datatype/lang/numeric filter planner variants.
- [x] (2026-05-11 03:11+02:00) Verified focused planner and impacted unit batch after option wiring.
- [x] (2026-05-11 04:52+02:00) Refreshed Engineering q2 top-regression snapshot equivalence for satisfied finite anchors.
- [x] (2026-05-11 05:24+02:00) Ran copyright check, formatter, focused unit batch, and Engineering q2 snapshot IT.

## Surprises & Discoveries

- Observation: `PredicateObjectGuarantee` is currently a positive-fact bit mask and `combine` is bitwise AND. This can express universal facts such as “all values are literals”, but it cannot cleanly represent possible sets such as `RESOURCE = {IRI, BNODE}`. A mix of IRI and BNode collapses toward no useful fact even though “not literal” remains useful.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PredicateObjectGuarantee.java` defines `combine(PredicateObjectGuarantee observed)` as `fromMask(mask & observed.mask)`.

- Observation: Eager guarantee rewrites are currently split across `LmdbGuaranteeFilterOptimizer`, `LmdbFilterSimplifierOptimizer`, and `LmdbSketchJoinOptimizer` special cases. This makes the planner see only one shape instead of competing shapes.
  Evidence: `LmdbQueryOptimizerPipeline` runs `new LmdbGuaranteeFilterOptimizer(evaluationStatistics)` before conjunctive splitting, and `LmdbSketchJoinOptimizer` later calls `rewriteSmallLiteralDeferredFilterAnchors(collected)` plus a `canonicalFiniteAnchorOrder` fast path.

- Observation: The focused `mvnf` unit command runs Maven `verify`, so LMDB Failsafe ITs can continue after the target Surefire test unless `-DskipITs` is passed. For the domain loop, the targeted Surefire report was captured and the unrelated IT fork was stopped; subsequent focused unit runs used manual Maven with `-DskipITs`.
  Evidence: `PredicateObjectGuaranteeTest` wrote `Tests run: 1, Failures: 0`, then the same fork was executing `LmdbThemeTopRegressionSnapshotIT.topRegressionOptimizedQueriesMatchFastestKnownSnapshots`.

- Observation: The MEDICAL_RECORDS query 7 pathology reproduces in a synthetic unit test: eagerly rewriting `FILTER(?code = "MED-1000" || ?code = "MED-1001")` to `VALUES ?code { ... }` makes the optimized algebra contain a `BindingSetAssignment` for `code`, removing the original local filter alternative before the Pareto planner can cost it.
  Evidence: `LmdbIndexAwareJoinOrderPlanningTest#highFanoutMedicalCodeFilterStaysOriginalFilterAlternative` failed with `BindingSetAssignment ([[code="MED-1000"], [code="MED-1001"]])`.

- Observation: A simple planner-time anchor option is enough to split high-fanout and selective cases: the high-fanout synthetic MEDICAL_RECORDS shape keeps the original filter, while a selective predicate produces a selected `BindingSetAssignment` after the planner compares total work.
  Evidence: `LmdbIndexAwareJoinOrderPlanningTest#highFanoutMedicalCodeFilterStaysOriginalFilterAlternative` and `#selectiveMedicalCodeFilterChoosesFiniteAnchorOption` are both green.

- Observation: The top-regression snapshot for Engineering q2 expected a redundant `FILTER IN` after `VALUES`. The new selected-anchor semantics correctly omit that filter because the selected finite anchor satisfies the logical filter group.
  Evidence: The first spillover run failed on `LmdbThemeTopRegressionSnapshotIT` for `ENGINEERING:2`; after adding a known-fast equivalent, `-Drdf4j.lmdb.topRegressionSnapshot.queryKeys=ENGINEERING:2` passes.

## Decision Log

- Decision: Use a real abstract domain with separate store aggregation and query intersection operations, instead of extending the current fact-mask API.
  Rationale: Stored observations require a least-upper “what can still be true for every observed value” operation, while query planning requires intersection and contradiction detection. One `combine` method cannot carry both meanings without mistakes.
  Date/Author: 2026-05-11 / Codex

- Decision: Break internal planner SPI rather than keep compatibility adapters.
  Rationale: The user explicitly requested no backward compatibility. The planner needs first-class option groups and selected variants, which are awkward to hide inside the current `FilterConstraint` constructors.
  Date/Author: 2026-05-11 / Codex

- Decision: Keep original query semantics as the fallback variant for every optional guarantee rewrite.
  Rationale: Missing or stale guarantee stats must never remove the original semantic path. Only proven contradictions may replace a branch with `EmptySet`.
  Date/Author: 2026-05-11 / Codex

## Outcomes & Retrospective

The first domain milestone is implemented and green. `PredicateObjectGuarantee` now has distinct `UNKNOWN` and `EMPTY` sentinels, observed joins preserve possible kind/datatype/timezone facts, query meets detect contradictions, and the persistent guarantee metadata version is bumped to force rebuilds without legacy byte decoding.

The second domain milestone is implemented and green. Query-derived finite exact-value domains now retain exact values, filter them through `meetRequired`, and avoid treating literal-specific datatype facts as contradictions when a mixed observed domain is narrowed to IRI-only.

The first planner transition slice is implemented and green. Object-position literal equality filters are no longer eagerly converted to `BindingSetAssignment` anchors in `LmdbFilterSimplifierOptimizer`; the original filter shape remains available for the planner in the high-fanout code case.

The eager optimizer has been removed from the LMDB pipeline. Guarantee-driven equality and type rewrites must now be reintroduced as selected planner variants rather than pre-splitter tuple mutations.

The first native finite-anchor option is implemented. `'GuaranteePlanOptionProvider'` emits object-position finite anchors for known predicate domains; `LmdbSketchJoinOptimizer` costs each option against the original plan and selects the lower-work plan, recording `optimizer.guaranteeOptions` telemetry on the chosen plan. This is intentionally a first slice: numeric, boolean, date, equality, type, and nested options still need planner-native variants.

The first equality option slice is implemented. `GuaranteePlanOptionProvider` derives segment-local domains, emits `SameTerm` variants for assured IRI/BNode equality, guarded equality variants for singleton-kind versus unknown variables, and an `EmptySet` option for assured disjoint kinds.

Type and simple value-expression filter options are implemented for the LMDB flat segment planner. `isURI`, `isBlank`, `isLiteral`, `isNumeric`, `datatype`, `lang`, and `langMatches` can now become selected drop/empty/guard variants from derived domains. Selected filter variants are cloned so the original logical filter group is marked applied without deleting the selected replacement.

The implementation is not the full deep `JoinOrderPlanner` SPI/action-mask rewrite described in the ideal architecture. Instead, it provides the same native choice behavior for the current LMDB planner surface by generating option variants, replanning each variant with LMDB page-walk/filter estimates, and selecting the lowest-work plan with `optimizer.guaranteeOptions` telemetry. A deeper SPI rewrite remains the next architectural step if option state must live inside `SketchJoinOrderPlanner.StatePlan` memo keys directly.

Verification completed:

    Command: mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=PredicateObjectGuaranteeTest,LmdbFilterSimplifierOptimizerTest,LmdbOptimizerPipelineTest,LmdbEvaluationStatisticsMemoizationTest,LmdbIndexAwareJoinOrderPlanningTest,LmdbPredicateObjectGuaranteeIndexTest verify
    Result: Tests run: 112, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS

    Command: mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=NoUnitTests -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=LmdbThemeTopRegressionSnapshotIT#topRegressionOptimizedQueriesMatchFastestKnownSnapshots -Drdf4j.lmdb.topRegressionSnapshot.queryKeys=ENGINEERING:2 verify
    Result: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS

    Command: ./checkCopyrightPresent.sh
    Result: All files have valid copyright headers and SPDX lines.

    Command: mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    Result: BUILD SUCCESS

## Context and Orientation

The LMDB Sail module lives under `core/sail/lmdb`. The generic query algebra evaluation planner lives under `core/queryalgebra/evaluation`.

`PredicateObjectGuarantee` is the package-private LMDB model for what can be assumed about all objects stored for one predicate. It is populated by LMDB store writes and rebuilds, exposed through `LmdbPredicateObjectGuaranteeSource`, and used by LMDB optimizers.

`LmdbGuaranteeFilterOptimizer` is an eager optimizer. It rewrites equality filters before the join planner. This file should be removed from the LMDB pipeline and either deleted or left unused only during transitional commits; the final pipeline must not run it.

`LmdbFilterSimplifierOptimizer` currently creates small literal `BindingSetAssignment` anchors before join planning. Guarantee-based anchor creation must move out of this eager pass. Non-choice simplifications, such as merging adjacent filters or dropping a filter already exactly covered by an existing assignment, may stay here.

`LmdbSketchJoinOptimizer` collects flat join segments, creates deferred filters, and invokes `JoinOrderPlanner`. It also has LMDB-specific special cases such as `rewriteSmallLiteralDeferredFilterAnchors` and `canonicalFiniteAnchorOrder`. Those special cases should become planner options rather than pre-planner rewrites or post-planner overrides.

`JoinOrderPlanner` is the internal SPI used by LMDB evaluation statistics. It currently accepts join arguments and a list of `FilterConstraint` objects. This SPI may be changed. The new shape should carry mandatory factors, optional factors, filter option groups, and selected option output.

`SketchJoinOrderPlanner` is the Pareto planner implementation. It already has the notion of actions beyond factors for deferred filters. It must be extended so optional guarantee actions are native planner choices, not pre-rewritten tuple trees.

Plain-language terms used below:

An abstract domain is a compact description of a set of possible RDF values. Here it tracks possible RDF term kinds, possible XSD datatypes, language-tag states, timezone states, numeric facts, canonical-integer facts, and finite value sets.

A concrete RDF term is an actual RDF4J `Value`: an `IRI`, `BNode`, or `Literal`.

`UNKNOWN` means no guarantee stats exist. It proves nothing.

`EMPTY` means constraints contradict each other. No concrete RDF value can satisfy the domain.

Known unrestricted means stats exist but impose no useful restriction. This is different from `UNKNOWN`; known unrestricted can be used for telemetry, but not for rewrites.

`joinObserved(a, b)` is the operation for store observations. If predicate `p` has observed domain `a` and a new object has domain `b`, the index stores `joinObserved(a, b)`, the least upper approximation that safely covers all observed objects.

`meetRequired(a, b)` is the operation for query planning. If variable `?x` must satisfy domain `a` and also constraint `b`, the planner uses `meetRequired(a, b)`. If there is no possible value, the result is `EMPTY`.

## Plan of Work

First add domain tests. Create or extend a package-private test in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb` to prove `joinObserved` preserves useful possible-kind restrictions and `meetRequired` detects contradictions. Tests should cover mixed IRI/BNode resources, literal versus IRI, datatype mismatch, language-tag mismatch, timezone mismatch, finite values, and unknown stats. Run the focused test and record the failing Surefire snippet before production edits.

Then replace `PredicateObjectGuarantee` with a richer immutable value. Keep the class package-private. Represent kind possibilities with an enum set or compact bit mask. Represent XSD datatype possibilities with a bit set over `CoreDatatype.XSD.values()`. Represent language states as possible `WITH_LANGUAGE` and `WITHOUT_LANGUAGE`. Represent timezone states as possible `UTC`, `OFFSET_NON_UTC`, `WITHOUT_TIMEZONE`, and `UNKNOWN_OR_INVALID`. Keep numeric/canonical-integer facts as universal facts that are true only when all possible literals in the domain satisfy them. Keep optional finite values as a small immutable set only for query-derived domains and exact `VALUES`; the persistent predicate index does not need finite values.

The class must expose clear factories: `unknown()`, `empty()`, `unrestricted()`, `iri()`, `bnode()`, `literal()`, `resource()`, `finiteValues(Collection<Value>)`, and `classify(Value)`. It must expose operations `joinObserved(PredicateObjectGuarantee)` and `meetRequired(PredicateObjectGuarantee)`. Existing call sites that used `combine` must be audited and switched to the correct operation by meaning.

Next update the LMDB persistent guarantee encoding. Replace the old byte and current single-long format with a versioned domain encoding. Since no backward compatibility is required, remove legacy byte decoding such as `fromLegacyCode`, bump the predicate-object guarantee metadata version, and rebuild missing or version-mismatched DBs on startup. Online inserts use `joinObserved`; ordinary deletes do not strengthen. If a predicate was weakened and a delete could theoretically restore a stronger guarantee, mark that predicate or the guarantee DB metadata as needing rebuild on the next server startup.

Then update the planner SPI. Replace the current flat `FilterConstraint`-only contract with explicit plan inputs:

    mandatory factors: original tuple expressions that must appear in every plan
    optional factors: inferred tuple expressions such as finite `VALUES` anchors
    filter option groups: mutually exclusive variants for one logical filter
    selected output: ordered factors, selected filter variants, satisfied groups, telemetry

The exact Java names can differ, but the concepts must be visible in the code. Every logical filter group must have an original variant unless a contradiction is proven. An optional anchor may mark a group satisfied only when the anchor is exact for that filter and the selected domains prove the original filter redundant.

Then implement `GuaranteePlanOptionProvider` under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb`. It should inspect one flat join segment plus its deferred filters. It should derive per-variable domains from statement positions, constant predicate object guarantees, finite assignments, and filter expressions. It should emit planner options instead of mutating the tuple tree. It should not use hardcoded predicate names.

Finite anchor options should be created only when the domain intersection is finite and sound. Canonical integer expansion is allowed only inside canonical integer domains; if there is one XSD datatype, emit only that datatype. Boolean expansion emits `true/1` or `false/0` only for `xsd:boolean`. Calendar expansion requires a single calendar datatype and compatible timezone domain. Decimal, float, double, duration, invalid numeric, and non-normalized integer filters remain original filters unless exact term equality is proven by finite domains.

Equality options should include `SameTerm` for both-IRI and both-BNode domains, guarded variants for singleton kind versus unknown, literal compare variants for literals, and `EmptySet` variants for disjoint assured domains. Type and datatype filters should become selectable variants: true/drop, false/empty, guard, or original filter according to the domain evidence.

Nested `EXISTS` and `NOT EXISTS` should initially get conservative option support. Infer domains across correlated variables. If outer and inner domains are disjoint, create false/true variants. If a correlated finite domain can drive inner lookups, create a nested finite-anchor variant and cost it against the original nested filter. Keep the original nested filter available unless contradiction is proven.

Finally wire selected output back through `LmdbSketchJoinOptimizer`. Remove eager `LmdbGuaranteeFilterOptimizer` from `LmdbQueryOptimizerPipeline`. Reduce `LmdbFilterSimplifierOptimizer` to non-choice simplifications. Remove or disable `rewriteSmallLiteralDeferredFilterAnchors` and the `canonicalFiniteAnchorOrder` override once equivalent planner options exist. Place selected filter variants through `LmdbDeferredFilterPlacer` or the existing deferred filter placement code path. Insert selected optional anchors into the final join tree. Add telemetry fields for generated option count, selected option groups, selected variant names, and final frontier alternatives.

## Concrete Steps

Work from repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

Before any test run, run:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

For focused unit tests, prefer:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPredicateObjectGuaranteeIndexTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbFilterSimplifierOptimizerTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbIndexAwareJoinOrderPlanningTest --retain-logs --stream

Do not use `-am` when tests are enabled. Do not use `-q` for test commands. If Maven offline resolution fails, rerun the same setup command once without `-o`, then return to offline commands.

Expected first red test behavior: the new domain test fails because `PredicateObjectGuarantee` does not yet have `joinObserved`, `meetRequired`, `UNKNOWN`, `EMPTY`, or possible-kind resource domains.

## Validation and Acceptance

Acceptance requires focused and broad verification:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbFilterSimplifierOptimizerTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbIndexAwareJoinOrderPlanningTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPredicateObjectGuaranteeIndexTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs --stream

The new tests must demonstrate:

The domain model preserves `{IRI, BNODE}` as a resource restriction under observed joins and detects contradictions under query meets.

Unknown predicate stats do not imply contradiction and do not create rewrite options.

The planner sees both the original filter and finite-anchor variant in the Pareto frontier when both are legal.

MEDICAL_RECORDS query 7 or its synthetic unit analogue chooses the original filter when the finite `VALUES` page-walk cost is worse.

A selective safe predicate chooses a finite anchor when cheaper.

`?a = ?b` variants select `SameTerm`, guard, or `EmptySet` according to domains.

The non-canonical integer regression still returns the row and blocks unsafe anchor options.

Single-`xsd:int`, boolean, and compatible date domains narrow anchor generation exactly as specified.

Optimized query-plan snapshots show selected guarantee options and frontier alternatives.

## Idempotence and Recovery

All edits must be source-level and repeatable. Do not delete untracked artifacts such as `logs/` or `initial-evidence.txt`. If a test run overwrites reports, use the retained mvnf logs under `logs/mvnf/` and the root `initial-evidence.txt` to preserve the first failure or build evidence.

If a production edit is made before observing a failing test for that behavior slice, stop, revert only the production edit made by this agent, add the failing test, run it, record evidence, and resume. Do not revert unrelated user or other-agent changes.

If the option explosion exceeds planner bit limits, cap inferred optional actions first and keep original semantics. The final implementation must never drop original query semantics merely because the option cap is reached.

## Artifacts and Notes

Initial baseline:

    Command: mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
    Result: BUILD SUCCESS
    Time: 25.579 s wall clock

Known motivating plan issue from the user:

    MEDICAL_RECORDS query 7 with eager VALUES had resultSizeEstimate=0 and plannedWorkRows=27, but actual rows through the VALUES-bound code lookup were 13.8K and runtime was about 62 ms/op. The original FILTER shape ran about 17 ms/op. The semantic rewrite was legal, but the eager rewrite removed the cheaper competing plan shape.

## Interfaces and Dependencies

`PredicateObjectGuarantee` should remain package-private under `org.eclipse.rdf4j.sail.lmdb`.

`LmdbPredicateObjectGuaranteeSource` should continue to expose lookup from `LmdbEvaluationStatistics` to LMDB planning code, but the returned value is the new abstract domain.

`JoinOrderPlanner` may change internally. At the end it should expose an input type that can carry mandatory factors, optional factors, and filter option groups, plus a result type that reports selected options and plan steps.

`SketchJoinOrderPlanner.StatePlan` must track selected option/group state and enough domain fingerprinting to prevent two semantically different option choices from collapsing into one memo group.

`GuaranteePlanOptionProvider` is the LMDB bridge from RDF4J algebra plus predicate object domains to planner-native options. It must be deterministic, package-private, and covered by focused tests.
