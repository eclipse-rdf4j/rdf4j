# Render query explanations from JSON with syntax and hotspot highlighting

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document is maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

The Workbench query page currently displays a Text explanation as one server-formatted string. After this change, an interactive Text explanation requests the server's existing JSON plan and reconstructs the same human-readable tree in the browser. The visible text, whitespace, connectors, annotations, metric order, and trailing newline remain identical to `GenericPlanNode.toString()`, while individual parts can be highlighted safely. A small `Syntax / Hotspots` control lets a user switch between semantic syntax colors and a heatmap without another network request. Explicit JSON and DOT views continue to work as they do today, and a server-rendered legacy Text explanation remains plain text.

The behavior is visible on the Workbench query page: run an explanation with Text selected, observe the familiar tree with syntax colors, activate Hotspots, and observe the same text with node-line heat intensity based on the selected explanation level. In comparison mode both panes share one scale.

## Progress

- [x] (2026-08-06 10:14Z) Inspected the query controller, XSL template, styles, Java formatter, browser harness, and existing test suites.
- [x] (2026-08-06 10:18Z) Added the smallest failing template and browser contract tests and retained the pre-change evidence.
- [x] (2026-08-06 10:42Z) Added shared Java/JavaScript parity fixtures and the pure TypeScript formatter/highlighter module.
- [x] (2026-08-06 10:42Z) Routed interactive Text through JSON and preserved generated plaintext for copy, download, and diff.
- [x] (2026-08-06 10:42Z) Added the accessible Syntax/Hotspots control, adaptive metrics, shared comparison normalization, and styling.
- [x] (2026-08-06 11:05Z) Compiled TypeScript and passed 56 Node tests, 229 `core/query` tests, 387 Workbench tests, and both Chromium explanation specs.
- [x] (2026-08-06 11:05Z) Attempted Browser-plugin QA, recorded its local-navigation block, and completed rendered desktop/comparison and narrow-width QA through the repository Playwright runner.

## Surprises & Discoveries

- Observation: `query.ts` already distinguishes the selected request format from the response format in `StableExplanation`, but presentation currently follows `responseFormat`.
  Evidence: `StableExplanation` has `requestedFormat` and `responseFormat`; `renderPanePresentation` passes `responseFormat` to `renderExplanation`.
- Observation: JSON serialization already returns display-ordered projection children and includes the derived `selfTimeActual` property.
  Evidence: `GenericPlanNode.getPlans()` returns `orderedPlansForDisplay()`, and `setSelfTimeActual` documents that JSON clients may receive the derived value.
- Observation: exact parity requires more than tree indentation. `GenericPlanNode.toString()` filters and orders base, planned, actual, string, and derived telemetry metrics and uses distinct number and time formatting rules.
  Evidence: `GenericPlanNode.getHumanReadable`, `appendCostAnnotation`, `appendMapTelemetry`, and the human-readable formatting helpers in `core/query/src/main/java/org/eclipse/rdf4j/query/explanation/GenericPlanNode.java` define the complete contract.
- Observation: JSON currently loses the original order of `ProjectionElemList` children because `getPlans()` always returns `orderedPlansForDisplay()`, while Text intentionally keeps original order when a list belongs to `MultiProjection`.
  Evidence: `getHumanReadable` passes `ordered=false` for a `ProjectionElemList` child of `MultiProjection`, but Jackson recursively serializes that child's sorted `getPlans()` result.
- Observation: `costEstimate` and `resultSizeEstimate` are marked `@JsonIgnore`, so an Optimized JSON response lacks the values needed both for exact Text reconstruction and the selected heat metric.
  Evidence: the shared Java fixture deserialized those properties as null and produced Text without either annotation.
- Observation: Jackson serializes positive-infinite plan doubles as the string `"Infinity"`, while Text renders them as `∞` and may use them in derived telemetry.
  Evidence: `GenericPlanNodeTest.toJsonSerializesInfinitePlanningEstimateForTextReconstruction` and the shared `Telemetry-infinite-derived` fixture cover formatting and derived metrics.
- Observation: panes can briefly display different explanation levels while a two-pane refresh resolves one request at a time.
  Evidence: the staggered-response Node test initially normalized a Timed self-time value against the previous Optimized cost maximum.
- Observation: the in-app Browser surface blocks both `localhost` and `127.0.0.1` navigation in this environment.
  Evidence: both Browser-plugin navigation attempts returned `net::ERR_BLOCKED_BY_CLIENT`; the repository Playwright runner could reach the same healthy server and completed the visual checks.

## Decision Log

- Decision: Keep the public Workbench and RDF4J server endpoints unchanged; only the AJAX serializer maps selected Text to transport JSON.
  Rationale: the server already exposes the required JSON, while legacy full-page Text rendering must remain plain.
  Date/Author: 2026-08-06 / Codex
- Decision: Keep selected format and transport format separate throughout state and rendering.
  Rationale: the UI, staleness signature, download extension, copy behavior, and diff semantics must remain Text even though the response payload is JSON.
  Date/Author: 2026-08-06 / Codex
- Decision: Put formatting and DOM rendering in `queryExplanationHighlighter.ts` and expose a narrow `workbench.queryExplanationHighlighter` API.
  Rationale: exact formatting is independently testable and `query.ts` does not need another large formatting subsystem.
  Date/Author: 2026-08-06 / Codex
- Decision: The toggle is global, defaults to Syntax on each page load, is visible only for Text, and changes presentation without refetching.
  Rationale: both comparison panes need one mode and one heat scale, while the explain request remains unaffected.
  Date/Author: 2026-08-06 / Codex
- Decision: Hotspot values are `selfTimeActual` for Timed, `resultSizeActual` for Executed and Telemetry, and `costEstimate` for Optimized. Unoptimized or metric-free plans use semantic syntax styling.
  Rationale: these values match the most useful information available at each explanation level.
  Date/Author: 2026-08-06 / Codex
- Decision: Make `GenericPlanNode.getPlans()` return the underlying child order and keep sorting exclusively in the existing display/DOT helpers.
  Rationale: JSON must be structurally lossless for the browser to reproduce the Java formatter's context-sensitive projection ordering; a child-plan accessor should expose actual order rather than presentation order.
  Date/Author: 2026-08-06 / Codex
- Decision: Include the existing `costEstimate` and `resultSizeEstimate` bean properties in GenericPlanNode JSON.
  Rationale: these are already part of the plan object and Text output, and JSON cannot serve as the lossless representation required by this feature while suppressing them.
  Date/Author: 2026-08-06 / Codex
- Decision: Share a heat maximum only while both visible pane summaries use the same metric.
  Rationale: staggered responses must not normalize self-time, row count, and cost values against one another; each pane uses its local maximum during the brief mixed-level state.
  Date/Author: 2026-08-06 / Codex

## Outcomes & Retrospective

Interactive Text explanations now use the server JSON plan while retaining Text as the selected and persisted presentation format. The browser reconstructs the exact Java tree string, renders semantic spans with safe DOM APIs, supports a global Syntax/Hotspots mode with adaptive and comparison-safe normalization, and keeps copy, download, and diff operations on generated plaintext. Explicit JSON/DOT and legacy full-page Text presentation paths remain intact.

Shared Java/JavaScript fixtures now protect tree geometry, context-sensitive projection order, join labels, timeouts, scope/algorithm annotations, base/planned/actual/derived telemetry, non-finite values, numeric thresholds, time units, and final newlines. Final verification passed 56 Node tests, 229 `core/query` tests, 387 Workbench tests, and two Chromium Workbench explanation tests. Screenshots at 1280px comparison width and 700px single-pane width showed readable heat rows, a clear metric legend, preserved tree geometry, and a visible keyboard focus ring; the browser console remained free of errors.

The in-app Browser could not access local addresses in this environment, so visual QA used the repository's own installed Playwright 1.49 runner after the required Browser-plugin attempt. No production limitation remains from that tooling constraint.

## Context and Orientation

`tools/workbench/src/main/webapp/scripts/ts/query.ts` owns query-page state, AJAX explanation requests, presentation, copying, downloading, comparison, and the JSON object-tree view. The generated `tools/workbench/src/main/webapp/scripts/query.js` and source map are produced by `tools/workbench/compileTypescript.sh`; generated JavaScript must not be edited directly.

`tools/workbench/src/main/webapp/transformations/query.xsl` renders the format and level controls, the `<pre>` used by Text, and script tags. `tools/workbench/src/main/webapp/styles/query-explanation.css` owns the explanation surface. `e2e/tests-unit/query-browser-harness.js` provides a lightweight DOM and AJAX harness for Node tests. `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/transformations/QueryTemplateTest.java` protects static template integration. Browser-level tests live under `e2e/tests`.

The Java formatting contract is `GenericPlanNode.toString()` in `core/query/src/main/java/org/eclipse/rdf4j/query/explanation/GenericPlanNode.java`. “Display plaintext” below means the exact string this method would return for the same JSON-visible plan. “Transport format” means the value sent as `explain-format`; “requested format” means the Text/DOT/JSON choice visible to the user.

## Plan of Work

First add a failing `QueryTemplateTest` method that requires the new highlighter script and accessible mode control, plus failing Node tests that require Text to serialize as JSON transport while remaining a Text presentation. Run the Java method through `mvnf`, retain its Surefire report, and write the compact pre-change result to top-level `initial-evidence.txt` before editing production files. Run the focused Node tests and retain their failing output in the implementation notes.

Add a failing Java regression proving JSON preserves original child order inside `MultiProjection`, then make `GenericPlanNode.getPlans()` return underlying order while existing Text and DOT helpers continue to apply their display sorting rules. Add shared fixture pairs containing representative JSON plans and expected plaintext. A Java contract test will deserialize each JSON plan into `GenericPlanNode` and confirm `toString()` equals the fixture text. A Node test will feed the same JSON into the TypeScript formatter and require identical text. Fixtures cover timeouts, nested binary branches and alternating connector glyphs, join side labels, statement pattern `s/p/o/c` labels, projection ordering, multi-projection nesting, scope and algorithm annotations, finite and infinite numbers, K/M boundaries, time units, and representative optimizer/runtime telemetry maps.

Create `tools/workbench/src/main/webapp/scripts/ts/queryExplanationHighlighter.ts`. Define typed plan values, `HighlightMode`, hotspot metric metadata, tokenized lines, and a render result containing a `DocumentFragment`, exact plaintext, selected metric, and local maximum. Port the complete Java display contract rather than parsing a formatted string. Build all rendered nodes with `document.createElement`, `createTextNode`, and `textContent`. Semantic mode assigns stable classes for connectors, node types, annotations, variable labels/values, metric names/values, and join-side labels. Hotspot mode assigns line heat classes or a normalized CSS custom-property value while preserving the same child text nodes. The module accepts an optional shared maximum.

Update `query.ts` so AJAX Text serializes as JSON transport, but `RequestSignature.format` remains `text`. Extend `StableExplanation` with display plaintext and parsed plan data, add a highlighted Text view, and choose presentation from `requestedFormat`. Explicit JSON continues through the existing interactive object tree; DOT is unchanged. Copy and diff read display plaintext. Downloads use the requested format to select `.txt` and `text/plain`. If Text receives invalid JSON, render the raw content safely as plain text and expose the existing error/status path without interpreting markup.

Add the segmented control to `query.xsl`, load `queryExplanationHighlighter.js` before `query.js`, and add concise locale labels if the template convention requires them. Add styling in `query-explanation.css`: accessible focus, pressed state, semantic tokens, readable connectors, a restrained amber-to-red heat palette, and a small legend naming the active metric. Toggle state is a presentation-only variable in `query.ts`; it is not part of query inputs and never marks an explanation stale. In comparison mode compute the maximum across both parsed plans and render both panes with it, including rerendering the primary pane when the comparison response arrives.

Update the Node harness to load the generated highlighter before `query.js` and register the new controls. Extend unit and Playwright tests for request mapping, exact text, inert HTML-like values, toggle state and no-refetch behavior, shared comparison scaling, copy/download/diff, invalid JSON fallback, keyboard/ARIA behavior, and unchanged JSON/DOT paths.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-temp` unless a command changes directory explicitly. Tests never use Maven `-am` or `-q`.

Run the focused pre-change Java test and retain reports:

    python3 .codex/skills/mvnf/scripts/mvnf.py QueryTemplateTest#textExplanationShouldLoadJsonHighlighterAndModeControl --retain-logs
    python3 scripts/agent-evidence.py --command "python3 .codex/skills/mvnf/scripts/mvnf.py QueryTemplateTest#textExplanationShouldLoadJsonHighlighterAndModeControl --retain-logs" tools/workbench/target/surefire-reports > initial-evidence.txt

Run focused Node contracts from `e2e`:

    node --test tests-unit/query-explanation-highlighter.test.js tests-unit/query-page.test.js tests-unit/query-internals.test.js

After implementation, compile TypeScript:

    tools/workbench/compileTypescript.sh

Check source headers before formatting, then format repository sources:

    scripts/checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Run focused and module validation:

    node --test e2e/tests-unit/query-explanation-highlighter.test.js e2e/tests-unit/query-page.test.js e2e/tests-unit/query-internals.test.js e2e/tests-unit/query-ui-internals.test.js
    python3 .codex/skills/mvnf/scripts/mvnf.py QueryTemplateTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs
    cd e2e && npx playwright test tests/workbench-explain.spec.js --project=chromium --reporter=line

Start the supported local Workbench test environment using the existing `e2e` scripts, then use the Browser plugin for rendered desktop and narrow-width checks. The exact command will be recorded after inspecting the existing test-server scripts so this plan remains accurate.

## Validation and Acceptance

The new Java structural test must fail before production changes and pass afterward. Shared Java and Node fixtures must prove the Java and TypeScript formatters return exactly the same string, including final newline. Node page tests must show a selected Text request sends `explain-format=json`, retains requested format Text, displays highlighted spans, copies and downloads plaintext, and toggles modes without adding an AJAX request. A malicious-looking type or metric value such as `<script>alert(1)</script>` must appear as literal text with no element created.

For hotspot behavior, tests must show the selected metric by explanation level, zero-to-maximum normalization, syntax fallback without valid data, and the same normalized value for equal metrics across comparison panes. Explicit JSON must still render the collapsible object tree and DOT must still render through Viz.

The browser flow under test is: Workbench query page -> run a Text explanation -> see the exact highlighted tree -> activate Hotspots -> see identical text with a labeled heat scale and no new request -> open comparison -> see both panes use one scale. Page identity, meaningful DOM, absence of framework overlays, console health, screenshot evidence, keyboard interaction, and desktop/narrow layouts must all be checked.

## Idempotence and Recovery

The tests, TypeScript compiler, formatter, and Maven commands are repeatable. Generated JavaScript and source maps are regenerated only from TypeScript sources. If a Maven offline dependency is missing, repeat the exact command once without offline mode and then return to offline mode. Do not delete or overwrite the two unrelated untracked POM diff files. Do not use destructive Git commands. If production code is changed before the failing evidence is captured, revert only this task's production edits and restart from the failing test milestone.

## Artifacts and Notes

The initial root quick clean install completed successfully before implementation. Two unrelated untracked files existed at the outset and remain out of scope: `pom-6.0.0-M3-to-6.0.0.diff` and `pom-6.0.0-M3-to-6.0.0-excluding-release-version-bumps.diff`.

The first failing Maven result is persisted in `initial-evidence.txt`. It reports one test and one expected failure because the control and script are absent. The focused Node contract also failed three of three tests because `queryExplanationHighlighter.js` did not yet exist. Final focused and module logs are retained under `logs/mvnf/`. Browser screenshots and temporary diagnostic scripts belong outside the repository.

## Interfaces and Dependencies

The new module exports through `workbench.queryExplanationHighlighter` without adding a package dependency. Its stable API provides a `format(plan)` operation returning exact plaintext and tokenized lines, a `getHotspot(plan, level)` operation returning the metric name and maximum finite non-negative value, and a `render(plan, options)` operation returning a `DocumentFragment`, plaintext, metric metadata, and local maximum. Render options contain `level`, `mode`, and optional `sharedMaximum`.

`query.ts` retains `ExplainFormat = 'text' | 'dot' | 'json'` and adds a presentation-only `HighlightMode = 'syntax' | 'hotspot'`. `StableExplanation` gains generated display plaintext and parsed JSON plan data. HTTP endpoints do not change. GenericPlanNode JSON becomes lossless by preserving child ordering and exposing its existing `costEstimate` and `resultSizeEstimate` properties. No new third-party dependencies are introduced.

Revision note (2026-08-06): Created the initial implementation-ready ExecPlan after inspecting the repository and resolving transport, state, parity, heat-scale, accessibility, and validation decisions.

Revision note (2026-08-06 10:18Z): Recorded the failing Java and Node contracts and advanced implementation to the pure formatter milestone.

Revision note (2026-08-06 10:25Z): Documented the context-sensitive projection-order information loss and the required lossless JSON correction before implementing the formatter.

Revision note (2026-08-06 10:30Z): Recorded that planning estimates are currently suppressed from JSON and added their round-trip inclusion to the transport contract.

Revision note (2026-08-06 10:42Z): Marked formatter, transport, and toggle milestones complete after 54 Node unit tests and focused Java contracts passed; advanced to broad compilation and test verification.

Revision note (2026-08-06 11:05Z): Recorded final parity fixes, module and browser verification, visual QA evidence, the local Browser-plugin limitation, and completed outcomes.
