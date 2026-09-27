# Anchor Workbench disclosure panels

This ExecPlan is a living document and follows `.agent/PLANS.md`. Keep its Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective sections current; exactly one Progress item stays in progress until the task is complete.

## Purpose / Big Picture

Workbench configuration panels should appear to belong to the button that opened them. Query Options, Save Query, and embedded-result Options and Download currently place their expanded panels at the toolbar's left edge, far from right-aligned triggers. After this change, the single expanded panel in each toolbar occupies an in-flow row directly below the controls, aligns to its trigger, shows a restrained connector reaching the trigger's actual horizontal position, and remains within the available width on desktop, tablet, and narrow mobile screens. Opening panels continues to use existing reversible disclosure motion and accessibility behavior.

## Progress

- [x] (2026-09-27) Add and run failing rendered anchor regression.
- [x] Implement shared in-flow panel anchoring and connector.
- [x] Verify actions, motion, focus, and responsive geometry.
- [x] Capture desktop-dark and 320px visual evidence.
- [x] Reproduce anchored rapid disclosure switching.
- [x] Enforce one open panel per toolbar.
- [x] Verify values, focus, visuals, and motion.
- [x] Capture light and dark viewport examples.
- [x] Run frontend units and final checks.

## Surprises & Discoveries

- Observation: Query panels are assigned full-width named grid areas beginning at the toolbar's left edge, and the Options panel owns a third grid row even when Save Query is closed. Embedded result panels live in a separate left-aligned container after their toolbar.
  Evidence: `tools/workbench/src/main/webapp/styles/query.css` defines the three query grid rows and `.query-result-disclosure-panels`; the related markup is in `transformations/query.xsl`, `transformations/tuple.xsl`, and `transformations/graph.xsl`.
- Observation: `workbench.setDisclosureExpanded` already owns requested visibility, focus restoration, inert/ARIA state, and reversible motion for these panels. The layout repair should reuse it and keep panel semantics unchanged.
  Evidence: `tools/workbench/src/main/webapp/scripts/ts/template.ts` exports `setDisclosureExpanded`; `scripts/ts/query.ts` uses it for Query Options and Save Query.
- Observation: At 320px the more-specific desktop query-toolbar rule held its disclosure buttons at 36px, overriding the intended touch target; this left a 16.75px trigger-to-panel gap. A responsive shared token rule restores the 44px touch target and makes the panel begin within the tested attachment distance.
  Evidence: Before that correction, `workbench-disclosure-anchor.spec.js` reported `buttonMinHeight:"36px"`, `buttonBottom:778.953125`, `panelTop:795.703125`; after correction the four-width Chromium and WebKit runs passed.
- Observation: Two expanded disclosures in one toolbar cannot both retain an unambiguous immediate pointer relationship; the lower panel's pointer may cross the first panel. Each toolbar should keep only the most recently opened disclosure visible while preserving values and immediate focus/semantic state transitions.
  Evidence: Parent review identified the crossing pointer; before the production change, the expanded regression timed out because `#save-query-panel` remained visible after opening Query Options. Final Chromium and WebKit runs prove the previous panel is hidden and each new panel attaches to its trigger.
- Observation: Changing the embedded page-size selector reloads its result frame, which would conflate a value-retention test with paging behavior. The test instead changes the local Layout display option, whose value remains meaningful without fetching another result page.
  Evidence: The first updated browser run timed out because the test attempted to select page size 10 while the first visible result select was Layout; the corrected test identifies `#result-layout` and selects `records`.
- Observation: The shared disclosure animation asks `window.getComputedStyle` for browser styles; the lightweight unit-test window stub did not implement that browser API.
  Evidence: The first `npm run test:unit` run failed only `Config uses radio controls and closes its panel on an outside click` with `TypeError: window.getComputedStyle is not a function`. Adding a small computed-style implementation to `e2e/tests-unit/script-harness.js` brought the complete frontend suite to 95/95.

## Decision Log

- Decision: Use an in-flow grid row for each visible panel rather than an overlay popover.
  Rationale: The disclosure must push following results/editor content down instead of covering it, while hidden panels must not reserve blank rows.
  Date/Author: 2026-09-27 / Codex.
- Decision: Keep embedded-result panels in the existing in-flow container immediately after the sticky toolbar, and share one positioning/connector mechanism across query and embedded-result panels.
  Rationale: Moving them inside the sticky toolbar would make an expanded panel stick over result content while scrolling. Existing `aria-controls`/`aria-labelledby` IDs already pair each panel with its real trigger; a shared geometry update can follow that pair through responsive wrapping and resize without changing the sticky toolbar's behavior.
  Date/Author: 2026-09-27 / Codex.
- Decision: Permit one expanded disclosure per toolbar group; Query and embedded Results remain independent groups. Close an existing visible sibling synchronously when a new one opens, retaining its DOM values, focus flow, and the new panel's normal reversible opening motion.
  Rationale: Multiple panels in one toolbar make the lower connector cross another panel and no longer attach directly to its trigger. Synchronous close removes the old row before opening the new one, even when its previous motion was active, without delaying the action or focus change.
  Date/Author: 2026-09-27 / Codex.
- Decision: Give the embedded-result pointer the same fill as its panel (`--query-soft`).
  Rationale: A connector filled with the page surface looked like a cutout on the result panel. Matching the panel fill preserves the connected-surface appearance in light and dark themes.
  Date/Author: 2026-09-27 / Codex.

## Outcomes & Retrospective

Complete. The original detached-panel regression failed before the first fix: `#query-options-toggle` centered at x=1173.6 while its panel spanned x=257–991.7, so its trigger center was outside the panel. The follow-up one-open regression also failed before the final behavior change because the Save Query panel stayed visible after Query Options opened; its captured assertion is in `/private/tmp/workbench-disclosure-evidence-20260927.md`.

The shared Workbench disclosure setter now immediately closes visible siblings in the same toolbar, preserving their field values and semantic accessibility state. Query and embedded-result groups are independent. Result pointers use the result panel's actual surface fill. The existing measured geometry continues to align panel edge, connector, and motion origin from the live trigger rectangle.

Verification completed: Chromium and WebKit each passed `workbench-disclosure-anchor.spec.js` at 1440, 768, 390, and 320 CSS pixels; the suite includes keyboard activation, rapid switching, one-visible-panel-per-toolbar, independent groups, value retention, focus, pointer attachment, and viewport containment. Existing Query Options reversible-motion tests passed in both engines (2 total). The frontend unit suite passed all 95 tests after the browser test stub was brought up to date. TypeScript compilation regenerated checked-in JavaScript, and Workbench install plus Server Boot package builds succeeded with tests skipped. `git diff --check` is clean; no Java test suite was rerun for this frontend-only change.

The red switching test observed Save Query still visible after Query Options opened. Chromium/WebKit/motion snippets and the red assertion are preserved in `/private/tmp/workbench-disclosure-evidence-20260927.md`; the frontend unit report is `/private/tmp/workbench-disclosure-unit-final-green-2-20260927/unit.log`. Final inspected captures are in `/private/tmp/workbench-disclosure-anchor-final-20260927/screenshots/`, including the direct toolbar crop `query-options-toolbar-dark.png`, `query-options-1440-dark.png`, `query-options-320-dark.png`, `result-options-1440-light.png`, and `result-options-320-dark.png` plus light/dark counterparts. Maven build output was appended to the preserved workspace `maven-build.log`.

## Context and Orientation

The repository is `.`. `tools/workbench/src/main/webapp/transformations/query.xsl` renders Query Options and Save Query. `transformations/tuple.xsl` and `transformations/graph.xsl` render embedded result controls. Their shared styles are in `styles/query.css`; disclosure state and animation are in `scripts/ts/template.ts` and query state wiring is in `scripts/ts/query.ts`. Generated JavaScript in `scripts/*.js` is checked in and must stay synchronized with TypeScript when TypeScript changes. `e2e/tests/` contains Playwright end-to-end tests; use the existing application fixture on loopback port 8086 and create a uniquely named temporary repository through its RDF4J REST API. The working tree already contains unrelated user/task edits and untracked artifacts; preserve all of them.

## Plan of Work

First add a Playwright test that opens a real query result, expands Query Options and Save Query and embedded-result Options and Download, and measures each visible panel against the button that controls it. The test must fail against the current left-aligned layout before any production edit. It also verifies that switching hides the previous panel in that toolbar, retains values, and keeps each panel inside the viewport.

Then change query toolbar layout so only visible panels create in-flow rows below the controls and each panel aligns to its associated trigger. Keep embedded-result panels in their current in-flow container below the sticky controls toolbar, make each visible panel its own row, and apply the same anchor treatment. A small shared geometry helper may set the connector location and panel inline position from the current button and panel rectangles; it must update on responsive layout changes, never use fixed per-control offsets, and must not change visibility ownership or action timing. Preserve reduced-motion behavior and existing button focus/ARIA/inert semantics. When a disclosure opens, close other visible panels in the same toolbar immediately, without clearing their values. Remove redundant nested horizontal rules only where the new connected surface replaces them.

Finally run the focused Playwright regression in Chromium and WebKit at 1440px, 768px, 390px, and 320px, switching between disclosures in each group and checking keyboard/focus/value retention. Capture light and dark desktop and 320px examples. Compile TypeScript and ensure its generated JavaScript is synchronized if scripts change, then run focused frontend checks and repository formatting/header/diff checks appropriate to the touched files.

## Concrete Steps

Run commands from the repository root. The browser test is invoked through the existing Playwright installation in `e2e`, with `RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8086/rdf4j-workbench` and `RDF4J_SERVER_BASE_URL=http://127.0.0.1:8086/rdf4j-server`; select only the new spec and browser project while developing. Keep screenshots in `/private/tmp`; keep compact browser stdout evidence outside Playwright's managed `--output` directory, which it cleans during a run. The task-owned server is already running; do not stop or restart it unless a changed package requires it, and never touch a process on another port.

Compile the Workbench frontend with the repository's existing TypeScript build command after editing TypeScript. Run focused Playwright interaction cases after compilation; do not run unrelated Java modules for this stylesheet/template/TypeScript change. Finish with `git diff --check`, header/format checks applicable to touched sources, and an untracked-artifact inventory without deleting anything.

## Validation and Acceptance

The regression must fail before the layout fix because an expanded panel's horizontal range does not contain the center of its own trigger or because it is not immediately below that trigger. After the fix, every expanded panel's trigger center lies within the panel, the connector center tracks that same point within two CSS pixels, and the panel begins within twelve CSS pixels of the trigger. Within each toolbar, switching to a different panel hides the previous panel synchronously while retaining field values; query and embedded-result groups remain independent. At 1440px, 768px, 390px, and 320px, the page and each panel must stay within the viewport with no horizontal document overflow. Keyboard activation, focus, and reduced-motion/reversal behavior remain intact. Verify in Chromium and WebKit. Capture and inspect light and dark desktop and 320px screenshots with a disclosure expanded.

The focused browser report must include the pre-fix failure and post-fix passing result. State exactly which focused suites, browser engines, and viewport widths ran; do not claim the full Java or Maven suite was run.

## Idempotence and Recovery

The test creates a process/time-unique repository and deletes only that same repository in its `afterAll`, so reruns do not depend on or overwrite user data. Screenshot and report outputs go under a unique directory in `/private/tmp`. If the owned fixture is unavailable, diagnose it before starting anything and only restart the known port-8086 task-owned process. Keep all existing tracked modifications, generated files, and untracked artifacts.

## Artifacts and Notes

Recorded Playwright commands used the existing e2e install and task-owned loopback fixture; concise actual command summaries are in `/private/tmp/workbench-disclosure-evidence-20260927.md`:

- Chromium: `RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8086/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:8086/rdf4j-server WORKBENCH_DISCLOSURE_SCREENSHOT_DIR=/private/tmp/workbench-disclosure-anchor-final-20260927/screenshots node_modules/.bin/playwright test tests/workbench-disclosure-anchor.spec.js --project=chromium --reporter=line --workers=1 --output=/private/tmp/workbench-disclosure-anchor-final-20260927`
- WebKit: `RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8086/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:8086/rdf4j-server WORKBENCH_DISCLOSURE_SCREENSHOT_DIR=/private/tmp/workbench-disclosure-switch-webkit-20260927/screenshots node_modules/.bin/playwright test tests/workbench-disclosure-anchor.spec.js --project=webkit --reporter=line --workers=1 --output=/private/tmp/workbench-disclosure-switch-webkit-20260927`
- Motion: `RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8086/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:8086/rdf4j-server node_modules/.bin/playwright test tests/workbench-motion.spec.js --project=chromium --project=webkit --grep 'query options animate reversibly' --reporter=line --workers=1 --output=/private/tmp/workbench-disclosure-motion-final-20260927`
- Unit: `npm run test:unit` from `e2e`; 95 passed, 0 failed.

Final browser results: Chromium `1 passed (8.5s)`, WebKit `1 passed (8.9s)`, motion `2 passed (4.2s)`. Both anchor runs exercised 1440/768/390/320px. TypeScript compilation reported `Replaced repository JavaScript files with compiled TypeScript versions.` Workbench install and Server Boot package both succeeded; their output is appended in `maven-build.log`. Final `git diff --check` passed; no Java tests were run.

Plan revision (2026-09-27): Keep result panels outside the sticky toolbar after source review showed that putting them inside it would make settings remain pinned over the results during scroll. The current `aria-labelledby` reference plus measured trigger/panel geometry preserves the user-visible association without that sticky overlay risk.

## Interfaces and Dependencies

No new dependency or framework is needed. Reuse Playwright from `e2e/package.json`, `workbench.setDisclosureExpanded` from `scripts/ts/template.ts` for visibility/animation semantics, and the existing query/result trigger IDs. If a shared positioning helper is introduced, it should accept an `HTMLButtonElement` trigger and its corresponding `HTMLElement` panel, measure the current geometry, and update the panel's connector custom property. It must be harmless when the trigger is hidden/disabled or the panel is hidden, and refresh after resize/layout changes without writing visibility state.
