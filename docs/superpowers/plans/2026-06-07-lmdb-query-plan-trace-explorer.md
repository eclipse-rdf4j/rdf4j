# LMDB Query Plan Trace Explorer

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan follows `.agent/PLANS.md` in this repository. A future agent should be able to resume from this file alone.

## Purpose / Big Picture

After this change, an LMDB query-plan snapshot JSON file can explain more than the final optimized plan. It will contain a structured Cascades optimizer trace: rule catalog, per-expression rule evaluations, alternatives considered, alternatives accepted or discarded, winners, rejection reasons, and the final winner choice. A Workbench-hosted static page will parse that snapshot and let a developer step through alternatives, filter rules, compare candidates, and inspect why the final plan won.

The observable result is: run `QueryPlanSnapshotCli` against an LMDB query with Cascades tracing enabled, open the generated JSON in `tools/workbench/src/main/webapp/query-plan-explorer.html`, and see a populated rule matrix, alternative timeline, winner summary, and side-by-side candidate comparison.

## Progress

- [x] (2026-06-07T19:15Z) Ran required root install. Offline failed because `.m2_repo` lacked Maven artifacts; online retry succeeded.
- [x] (2026-06-07T19:16Z) Found existing snapshot capture in `testsuites/benchmark-common` and Cascades planner in `core/queryalgebra/evaluation`.
- [x] (2026-06-07T19:24Z) Added failing Cascades telemetry test; red on missing `structuredTrace()`.
- [x] (2026-06-07T19:28Z) Added structured trace recording, rule evaluation statuses, alternatives, and winners.
- [x] (2026-06-07T19:31Z) Added failing snapshot capture test; red on missing `QueryPlanSnapshot.getOptimizerTrace()`.
- [x] (2026-06-07T19:33Z) Surfaced parsed `optimizerTrace` in snapshot JSON.
- [x] (2026-06-07T19:36Z) Added failing LMDB emission test; red on missing `optimizer.cascadesTraceJson`.
- [x] (2026-06-07T19:40Z) Added static Workbench trace explorer page and focused JS parser tests.
- [x] (2026-06-07T20:35Z) Ran focused Java tests, Workbench unit tests, CLI smoke artifact parse, and desktop/mobile browser verification.

## Surprises & Discoveries

- Observation: Existing `QueryPlanCapture` already writes JSON snapshots with unoptimized, optimized, and telemetry explanations, but the optimizer trace is currently a planned string metric named `optimizer.cascadesTrace`.
  Evidence: `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan/QueryPlanCapture.java` captures explanation levels and debug metrics; `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizer.java` writes `optimizer.cascadesTrace` by joining telemetry strings.
- Observation: Cascades telemetry already has hooks for considered, costed, accepted, discarded alternatives, and winners. It does not record non-matching rules as structured data.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesTelemetry.java` contains those methods; `RuleRegistry.applicableRules` silently drops non-matches.
- Observation: `QueryRoot` planned metrics are not always copied into explanation debug metrics by the generic plan conversion path.
  Evidence: The first snapshot implementation parsed debug metrics only; the focused snapshot test stayed red until `QueryPlanCapture` scanned cloned tuple expressions for `optimizer.cascadesTraceJson`.
- Observation: The explorer script crossed the repo's approximate 500 LOC guideline when rendering and model code were combined.
  Evidence: `wc -l` reported 579 lines for `query-plan-explorer.js`; splitting DOM rendering into `query-plan-explorer-page.js` brought the files to 319 and 282 lines.

## Decision Log

- Decision: Keep the existing string telemetry metric for compatibility and add a new structured JSON metric named `optimizer.cascadesTraceJson`.
  Rationale: Existing tests and debugging flows may depend on `optimizer.cascadesTrace`; adding a second metric gives the webpage a stable payload without parsing log-like text.
  Date/Author: 2026-06-07 / Codex.
- Decision: Put the structured trace implementation in `CascadesTelemetry.Recording` and call new telemetry hooks from `RuleRegistry` and `CascadesPlanner`.
  Rationale: The generic Cascades planner owns rule evaluation and alternative selection; LMDB should only enable and attach the trace.
  Date/Author: 2026-06-07 / Codex.
- Decision: Build a static Workbench page with plain JavaScript, CSS, and a file input.
  Rationale: This avoids new frontend dependencies and lets developers open any generated snapshot artifact directly.
  Date/Author: 2026-06-07 / Codex.

## Outcomes & Retrospective

No implementation outcome yet. The first acceptance target is a failing optimizer trace test proving that matched and non-matched rules are represented as structured events.
Backend trace recording, snapshot extraction, LMDB trace metric emission, and the static explorer are implemented. The CLI smoke wrote a real LMDB trace snapshot with `optimizerTrace.formatVersion=1`, 425 rule evaluations, 52 alternatives, 5 winners, and 512 bounded events. Headless browser checks loaded that artifact in the static explorer on desktop and mobile; mobile had no page-level horizontal overflow after the responsive file-input fix.

## Context and Orientation

The Cascades optimizer is in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades`. Cascades search stores logical and physical alternatives in a memo. A memo group is a set of logically equivalent expressions. A rule can be a transformation rule, implementation rule, or enforcer rule. The planner chooses a `Winner`, which is the best physical alternative for a `WinnerKey` containing group id, required physical properties, semantic scope, and cost policy.

LMDB wires the generic Cascades planner from `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCascadesOptimizer.java`. That class reads the system property `rdf4j.optimizer.lmdb.cascades.trace`. When true, it creates `CascadesTelemetry.Recording`, passes it to `CascadesPlanner`, and currently annotates the root tuple expression with `optimizer.cascadesTrace`.

Snapshot generation is in `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/plan`. `QueryPlanCapture` calls `TupleQuery.explain(...)`, extracts debug metrics from the explanation JSON, and writes `QueryPlanSnapshot`. The benchmark CLI is `testsuites/benchmark/src/main/java/org/eclipse/rdf4j/benchmark/plan/QueryPlanSnapshotCli.java`.

Workbench static assets are under `tools/workbench/src/main/webapp`. Plain JavaScript files live in `scripts/`, CSS in `styles/`, and browser/unit tests for Workbench behavior live in `e2e/tests-unit` and `e2e/tests`.

## Plan of Work

First, add a focused failing unit test in `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/CascadesRuleEngineTest.java`. The test should create a registry with one implementation rule that matches and one transformation rule that does not match a simple `StatementPattern`. It should run `CascadesPlanner` with `CascadesTelemetry.Recording`, call the new structured trace accessor, and assert that the trace contains both `matched` and `not_matched` rule-evaluation events, plus at least one considered alternative and a chosen winner.

Second, extend `CascadesTelemetry` with structured hooks. Add default methods for rule evaluation and rule outcome. Extend `CascadesTelemetry.Recording` to store both the existing string trace and a bounded list of structured event maps. It must expose `structuredTrace()` for tests and `toJson()` for LMDB snapshot annotation. The JSON should be an object with `formatVersion`, `eventCount`, `ruleCatalog`, `ruleEvaluations`, `alternatives`, `winners`, and `events`.

Third, update `RuleRegistry`. Add an overload `applicableRules(MemoExpr expression, OptimizationGoal goal, Memo memo, CascadesTelemetry telemetry)`. It should evaluate every registered rule, call telemetry with `matched` or `not_matched`, sort matched rules exactly like the existing method, and preserve the existing three-argument method as a no-op telemetry wrapper. Update `CascadesPlanner` to use the four-argument overload everywhere it asks for applicable rules.

Fourth, update `CascadesPlanner.applyRule`. It should record duplicate skips, budget skips, no-output applications, and successful applications as structured rule outcomes. It should continue emitting the existing string trace events.

Fifth, update `LmdbCascadesOptimizer.annotate`. When recording telemetry exists, keep writing `optimizer.cascadesTrace` and also write `optimizer.cascadesTraceJson` using the new JSON method.

Sixth, update `QueryPlanSnapshot` and `QueryPlanCapture`. Add an `optimizerTrace` map field to `QueryPlanSnapshot`. After explanations are captured, read the first `optimizer.cascadesTraceJson` debug metric from optimized or telemetry explanations, parse it with Jackson into a `LinkedHashMap`, and attach it to the snapshot. If parsing fails, attach a small map with `parseError`.

Seventh, add a static explorer page:

- `tools/workbench/src/main/webapp/query-plan-explorer.html`
- `tools/workbench/src/main/webapp/scripts/query-plan-explorer.js`
- `tools/workbench/src/main/webapp/styles/query-plan-explorer.css`

The page should accept a JSON file via file input, parse the snapshot, normalize either `optimizerTrace` or legacy `debugMetrics["optimizer.cascadesTraceJson"]`, and render: summary metrics, rule filters, a rule-evaluation table, an alternative timeline, a winner panel, and two alternative comparison panes. Use no new dependencies. The page must be usable by opening the HTML file directly.

Eighth, add unit tests for the parser and view-model logic in `e2e/tests-unit/query-plan-explorer.test.js`. The JS file should export a CommonJS module when `module.exports` exists, while still attaching to `window.workbench.queryPlanExplorer` in the browser. Tests should cover parsing nested `optimizerTrace`, fallback parsing from debug metrics, event counts, rule status filters, and alternative comparison selection.

## Concrete Steps

Run from repository root `/Users/havardottestad/.codex/worktrees/71e4/rdf4j-small-things`.

1. Keep the current dirty worktree intact. Use `git status --short --untracked-files=no` before edits.

2. Add the failing optimizer trace test:

   `python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#structuredTelemetryCapturesMatchedAndSkippedRules --module core/queryalgebra/evaluation --retain-logs`

   Expected before production changes: test fails because `CascadesTelemetry.Recording` has no structured trace accessor or no matched/not-matched rule-evaluation events.

3. Implement structured trace recording and RuleRegistry telemetry.

4. Re-run the same test and expect success:

   `python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest#structuredTelemetryCapturesMatchedAndSkippedRules --module core/queryalgebra/evaluation --retain-logs`

5. Add the failing snapshot test in `testsuites/benchmark-common/src/test/java/org/eclipse/rdf4j/benchmark/common/plan/QueryPlanCaptureTest.java`. It should create an optimized explanation whose root has `optimizer.cascadesTraceJson` and assert `snapshot.getOptimizerTrace()` contains the parsed event.

6. Run that snapshot test and expect failure before modifying capture code:

   `python3 .codex/skills/mvnf/scripts/mvnf.py QueryPlanCaptureTest#capturesStructuredOptimizerTraceJson --module testsuites/benchmark-common --retain-logs`

7. Implement snapshot parsing, re-run the same test, and expect success.

8. Add webpage files and JS unit tests. Run:

   `npm run test:unit --prefix e2e`

   Expected after implementation: Node unit tests pass.

9. Run format/checks before final:

   `cd scripts && ./checkCopyrightPresent.sh`

   `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`

   The formatter command uses `-q`; this is the project’s formatter command, not a test command.

10. Run focused module tests:

   `python3 .codex/skills/mvnf/scripts/mvnf.py CascadesRuleEngineTest --module core/queryalgebra/evaluation --retain-logs`

   `python3 .codex/skills/mvnf/scripts/mvnf.py QueryPlanCaptureTest --module testsuites/benchmark-common --retain-logs`

11. Run a CLI smoke capture if time allows:

   `./.codex/skills/query-plan-snapshot-cli/scripts/run_query_plan_snapshot.sh --log /tmp/qps-trace.log -- --store lmdb --theme MEDICAL_RECORDS --query-index 0 --query-id trace-smoke --system-property rdf4j.optimizer.lmdb.cascades.trace=true`

   Expected: generated JSON contains top-level `optimizerTrace`.

12. Start or use a simple static file view for the explorer. If a server is needed, run one from `tools/workbench/src/main/webapp` and open `query-plan-explorer.html`. Verify loading a sample trace JSON populates the rule matrix and alternatives.

## Validation and Acceptance

Acceptance for backend instrumentation: the new Cascades test fails before implementation and passes after, proving that structured telemetry records matched and non-matched rules, alternatives, and winners.

Acceptance for snapshot generation: the new `QueryPlanCaptureTest` fails before implementation and passes after, proving the generated snapshot JSON has top-level `optimizerTrace`.

Acceptance for the webpage: `npm run test:unit --prefix e2e` passes, and browser verification shows that loading a trace JSON renders non-empty summary, rule rows, alternatives, winner details, and comparison panes. The explorer must not require a dev server or dependency installation.

## Idempotence and Recovery

All edits are additive or narrowly scoped. Re-running tests is safe. If Maven offline resolution fails after the successful online root install, retry once online only for dependency resolution and return to offline commands. Do not delete untracked artifacts. Do not modify the two pre-existing dirty benchmark test files unless directly required.

If production code is touched before the failing test evidence is captured, revert only the patch made for this task and restart from the failing test step. Do not revert user or other-agent changes.

## Artifacts and Notes

Initial install evidence:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -Dmaven.repo.local=.m2_repo -Pquick clean install
    Snippet:
    RDF4J: LmdbStore ................................... SUCCESS [ 10.540 s]
    RDF4J: Workbench ................................... SUCCESS [  7.591 s]
    BUILD SUCCESS

Expected trace JSON shape:

    {
      "formatVersion": "1",
      "eventCount": 12,
      "ruleCatalog": [
        {"id": "native-implementation", "kind": "IMPLEMENTATION", "phase": "PHYSICAL_IMPLEMENTATION"}
      ],
      "ruleEvaluations": [
        {"eventIndex": 0, "groupId": 0, "expressionId": 0, "operator": "StatementPattern", "ruleId": "native-implementation", "status": "matched"},
        {"eventIndex": 1, "groupId": 0, "expressionId": 0, "operator": "StatementPattern", "ruleId": "never-match", "status": "not_matched"}
      ],
      "alternatives": [
        {"eventIndex": 2, "groupId": 0, "ruleId": "native-implementation", "status": "considered", "kind": "IMPLEMENTATION"}
      ],
      "winners": [
        {"eventIndex": 5, "groupId": 0, "cost": {"rows": 1.0, "workRows": 1.0}, "approximate": false}
      ],
      "events": []
    }

## Interfaces and Dependencies

Do not add new third-party dependencies.

Add or update these interfaces:

- `CascadesTelemetry.ruleEvaluated(MemoExpr expression, CascadesRule rule, OptimizationGoal goal, boolean matched, int promise, String status)`
- `CascadesTelemetry.ruleOutcome(MemoExpr expression, CascadesRule rule, OptimizationGoal goal, String status, String reason)`
- `CascadesTelemetry.Recording.structuredTrace()`
- `CascadesTelemetry.Recording.toJson()`
- `RuleRegistry.applicableRules(MemoExpr expression, OptimizationGoal goal, Memo memo, CascadesTelemetry telemetry)`
- `QueryPlanSnapshot.getOptimizerTrace()` and `setOptimizerTrace(Map<String, Object>)`

The structured trace must be bounded by the same trace limit as the existing string trace.

Plan revision note: Created on 2026-06-07 to guide implementation of LMDB query-plan trace JSON and a static visual explorer.
