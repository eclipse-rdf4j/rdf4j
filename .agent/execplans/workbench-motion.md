# Add Subtle Workbench Motion

This ExecPlan is a living document maintained in accordance with `.agent/PLANS.md`. Its Progress section must keep exactly one item marked `in_progress` until final acceptance. It is self-contained and names the browser, source, and verification paths needed to complete the work.

## Purpose / Big Picture

Small, consistent motion will make Workbench controls feel responsive and make changes in open/closed, loading, and comparison state easier to follow. After this work, keyboard and pointer users can expand a panel, reverse it quickly, open or close comparison, and see results appear with brief feedback. The same transitions must finish immediately when reduced motion is requested, while requests, cancellation, focus, and accessibility state continue to respond at once.

The change should be visible in the real packaged Workbench page, not just a mockup. A Playwright sequence will capture a disclosure as it opens and closes, a comparison panel change, and the same controls with reduced motion enabled. Chromium and WebKit runs will verify the rendered states and that existing query/compare behavior remains intact.

## Progress

- [x] (2026-09-26 20:58Z) Inventory Workbench motion surfaces.
- [x] (2026-09-26 21:11Z) Add failing browser motion regression.
- [x] Implement owned reversible motion adapter.
- [x] Integrate disclosures, navigation, and query states.
- [x] Reproduce motion lifecycle regressions.
- [x] Release effects and reverse loading.
- [x] Verify browsers, behavior, and packaged page.
- [x] Complete evidence and plan handoff.

## Surprises & Discoveries

- Observation: The packaged visual review found two lifecycle gaps: completed owned Web Animations retain their `fill: forwards` effect after the ownership record is removed, and the loading indicator branches on its physical `hidden` attribute while its exit transition is still running. Compare mode also retains a 1rem desktop grid gap after its second column collapses.
  Evidence: `template.ts:startOwnedMotion()` removes the animation record but does not cancel the finished effect; `query.ts:setResultLoading()` tests `loadingElement.hidden`; `.query-compare-layout` retains `gap: 1rem` for the `1fr 0fr` collapsed state.
- Observation: The existing compare geometry regression can observe a transient unequal layout because it measures immediately after the compare editor appears, during the new 220ms grid transition.
  Evidence: `/private/tmp/workbench-motion-compare-regression-20260926` reports pane-width differences of 958px in Chromium and 814.875px in WebKit; the assertion ran while the grid's `grid-template-columns` transition was still in progress.
- Observation: Primary Query Execute and repository/form actions were not included in the short keyboard press transition contract; the initial rendered test observed only the browser default `transition-property: all` with no explicit short `box-shadow` transition.
  Evidence: `/private/tmp/workbench-motion-pressed-red-20260927.log` reports two failing engine cases; the first assertion expected `box-shadow` and received `all`.
- Observation: Animating a primary action's foreground and background while the theme changes can create a low-contrast intermediate color pair. Keeping the press effect to an inset shadow preserves the chosen theme's foreground/background pair throughout the press and theme change.
  Evidence: `/private/tmp/workbench-motion-pressed-green-diagnostic-20260927.log` measured 1.70:1 on WebKit during dark Query Execute theme transition; the final rendered test asserts at least 4.5:1 for active Query Execute and Create actions in light and dark.

- Observation: The shared `template.ts` script is the common Workbench controller and is loaded by the ordinary page template as well as embedded result pages. Its existing `installDisclosureToggles()` currently changes `hidden` immediately, while its `installWorkbenchNavigation()` owns native `<details>` state and closes sibling groups from the `toggle` event.
  Evidence: `tools/workbench/src/main/webapp/scripts/ts/template.ts` contains both installers; `tools/workbench/src/main/webapp/transformations/template.xsl` emits the navigation `<details>` elements.
- Observation: `renderQueryPageState()` runs repeatedly for query progress and explanation updates. It toggles the compare layout and diff-modal classes, so animation lifecycle must be driven by actual state changes or by CSS transitions that naturally do nothing when the class value is unchanged.
  Evidence: `tools/workbench/src/main/webapp/scripts/ts/query.ts` calls `renderQueryPageState()` from request, input, and explanation paths and invokes it again through `syncCompareModeVisibility()`.
- Observation: The stylesheet already has isolated chevron rotation, comparison panel transitions, explanation-settings entrance animation, and indefinitely rotating explanation spinners. Durations and reduced-motion treatment differ between stylesheets; compare spinners do not share the existing reduced-motion rule.
  Evidence: `workbench-refresh.css`, `query-compare.css`, and `query-explanation.css` contain separate transition/animation declarations and media rules.
- Observation: The new rendered contract fails on the real packaged Workbench in both Chromium and WebKit because opening Query Options, a native navigation group, and Diff starts no motion on the affected panel/list/modal.
  Evidence: `/private/tmp/workbench-motion-red-chromium-v2-20260926.log` and `/private/tmp/workbench-motion-red-webkit-20260926.log` each report `4 failed`; the first failure is `activeMotionCount(panel)` expected `> 0`, received `0`, at `workbench-motion.spec.js:64`. The test also reached the real query result and Explain flow before checking Compare/Diff.

## Decision Log

- Decision: Add focused browser regressions before changing animation cleanup, query-loading reversal, or the collapsed compare gap. Release a finished Web Animation after its final layout has been committed to normal styles; track loading by requested semantic state and reverse any in-flight fade; animate the compare gap to zero along with the second column.
  Rationale: A completed `fill: forwards` effect can override natural sizing after its handle is forgotten. `hidden` is not the requested state while an exit fade is pending. A one-editor layout should reclaim the gap beside the absent comparison pane.
  Date/Author: 2026-09-26 / Codex.
- Decision: Make the existing compare geometry test wait for the actual CSS transition to finish before asserting final equal columns.
  Rationale: Motion intentionally introduces intermediate widths, so the existing steady-state geometry assertion must sample a settled layout instead of a transition frame.
  Date/Author: 2026-09-26 / Codex.
- Decision: Give shared primary action wrappers and Query actions an explicit 120ms `box-shadow` press transition only. The active inset outline uses current text color, preserving the light/dark foreground and background colors while making keyboard activation visible; reduced motion disables the transition.
  Rationale: Primary actions include both `input[type=button|submit]` inside shared Workbench action wrappers and the Query toolbar's direct button. Color interpolation during theme switching can pass through low contrast, so press feedback should be independent from foreground/background changes.
  Date/Author: 2026-09-27 / Codex.

- Decision: Put explicit ownership of visibility animations in the shared `workbench` namespace in `template.ts`, then call that API from query state transitions. The adapter will track only animations it creates, will settle them on a live reduced-motion preference change, and will not call `getAnimations()` to cancel editor or vendor animations.
  Rationale: Query result rendering and editor libraries already own their own work; cancelling every animation on a target or document could interrupt CodeMirror, pan/zoom, or unrelated effects. The shared template script is present on both main and embedded Workbench documents.
  Date/Author: 2026-09-26 / Codex.
- Decision: Apply ARIA, `inert`, focus return, and action/request cancellation synchronously with the user's state change; only visual settling may continue briefly.
  Rationale: Animation duration must not become a gate for closing a modal, cancelling comparison work, restoring focus, or exposing the correct state to assistive technology.
  Date/Author: 2026-09-26 / Codex.
- Decision: Use named durations near 120ms for pressed feedback, 180ms for disclosures, and 220ms for outer panel/layout motion, with a shared easing curve. Reduced motion removes position, scale, and spin effects and settles only adapter-owned animations.
  Rationale: These values are short enough to preserve direct manipulation while allowing users to perceive a state transition; they follow the accepted project design direction. Native input behavior and large query/DOT work areas remain unchanged.
  Date/Author: 2026-09-26 / Codex.
- Decision: Preserve native `<details>` navigation semantics and keep `installWorkbenchNavigation()` as the single writer for group open state. The animation must be coordinated inside its existing toggle lifecycle rather than adding a second accordion controller.
  Rationale: Browser-native keyboard interaction, existing sibling closing, and the active route group are already implemented there.
  Date/Author: 2026-09-26 / Codex.
- Decision: Avoid animating CodeMirror elements and DOT/SVG internals; motion may affect their existing outer page/pane wrappers only.
  Rationale: Editor and graph libraries own internal sizing, transforms, and redraw lifecycle. A shared transform on their internals could break those integrations.
  Date/Author: 2026-09-26 / Codex.

## Outcomes & Retrospective

The shared adapter owns only Workbench-created visibility animations, releases them after committing final layout, reverses on requested-state changes, and settles them when `prefers-reduced-motion` changes. Query/options/form/navigation/diff/compare state and focus/ARIA/inert behavior remain synchronous; result loading now follows requested state rather than a stale `hidden` attribute. Collapsing Compare removes its gap as well as its second column. Primary action motion now covers both wrapped form inputs and the Query toolbar button, responds to keyboard Space, and leaves the theme foreground/background pair unchanged while pressed.

Verification completed against the rebuilt packaged app on the task-owned loopback fixture:

- Red first: `/private/tmp/workbench-motion-lifecycle-red-v2-20260927.log` captured retained finish effects, frozen/narrow sizing, stale loading visibility, and the collapsed compare gap. `/private/tmp/workbench-motion-pressed-red-20260927.log` captured the missing explicit primary-action press transition in Chromium and WebKit.
- Green: `/private/tmp/workbench-motion-lifecycle-green-v3-20260927.log` passed all 6 lifecycle cases; `/private/tmp/workbench-motion-pressed-green-v2-20260927.log` passed both engine cases for Space activation and >=4.5:1 active foreground/background contrast in light and dark themes. The full motion suite `/private/tmp/workbench-motion-final-20260927.log` passed 16 tests, and the existing compare-close/preserve-results test `/private/tmp/workbench-motion-compare-final-v2-20260927.log` passed 2 tests.
- Frontend unit suite `/private/tmp/workbench-motion-unit-20260927.log`: 95 passed, 0 failed. TypeScript compilation `/private/tmp/workbench-motion-typescript-20260927.log` passed. Two offline root `-Pquick clean install` package builds completed successfully and are appended to `maven-build.log`; the final Workbench/CSS package was served from that output on port 8086.
- Resource/format processing `/private/tmp/workbench-motion-format-20260927.log` reached `BUILD SUCCESS`; `./scripts/checkCopyrightPresent.sh`, `node --check e2e/tests/workbench-motion.spec.js`, and `git diff --check` passed. No Java source was changed by the motion follow-up; Java module tests were not repeated for the final CSS-only action feedback change.
- Actual-app transition captures remain in `/private/tmp/workbench-motion-sequence-20260926/`; they show Query Options open/close midpoints, Compare opening/open, and Create Advanced closed/open on mobile. The later final action regression exercised the primary controls in the current packaged app in both themes.

The fixture remains available at `http://127.0.0.1:8086/rdf4j-workbench` with app data under `/private/tmp/workbench-motion-appdata-v3-20260927`; it is the owned preview session 22599/PID 252 and is intentionally left running at the parent's request. No unrelated tracked edits or untracked artifacts were removed. A separate in-app-browser attempt was blocked by the client before localhost loaded (`ERR_BLOCKED_BY_CLIENT`); this is a preview-tool limitation, not a product result, and the already-green direct Playwright suite was not repeated.

Closing a disclosure becomes inert and inaccessible synchronously even if its brief visual exit is still running. Live reduced-motion changes finish only adapter-owned animations; editor/vendor animations are not cancelled. No fixed waits control product correctness; browser tests observe actual animation state and settled geometry. Java module tests were not rerun for this frontend-only motion follow-up, as authorized by the parent; the latest packaged root build, frontend unit suite, and cross-engine browser coverage passed. Parent reviewed the final source and visual sequence and accepted the implementation.

## Context and Orientation

The repository root is `.`. Workbench TypeScript source is under `tools/workbench/src/main/webapp/scripts/ts`; generated JavaScript and source maps beside it are build outputs and must be regenerated with `bash tools/workbench/compileTypescript.sh`, never edited by hand. Shared page styling is in `tools/workbench/src/main/webapp/styles/workbench-refresh.css`; query forms, embedded result panels, and result containers use `query.css`; comparison and explanation states have their own `query-compare.css` and `query-explanation.css` files. The common XSL wrapper is `tools/workbench/src/main/webapp/transformations/template.xsl`, and query markup/state is in `query.xsl` and `scripts/ts/query.ts`.

An “owned animation” here means a browser Web Animation started and retained by the shared Workbench motion adapter. Only that adapter may cancel or settle it. A “disclosure” is an existing named control that reveals or hides its associated settings panel. The Playwright project is under `e2e/`, with `e2e/playwright.config.js` and already installed Chromium/WebKit engines. Browser tests must target a fresh loopback fixture on a port owned by this task; do not use or stop any existing listener.

## Plan of Work

First add a focused Playwright test in `e2e/tests/workbench-motion.spec.js`. It will open and close the actual Query Options panel using pointer and keyboard input, inspect active browser animations rather than sleeping, reverse the transition while it is running, and assert that `aria-expanded`, `hidden`, `inert`, and focus match the requested state. It will open a native navigation group and confirm the existing sibling-close rule. It will open and close Compare and Diff, checking immediate focus/ARIA/inert state and preserving the primary query/results. It will switch the live page preference to reduced motion while a disclosure animation is active and verify the final state and absence of positional, scale, or spin motion. Run this test against the current packaged application first and retain the failure report before changing production code.

Then add a small shared adapter in `template.ts` with stable duration/easing tokens, explicit per-element animation ownership, no-op handling when the requested visibility is already current, safe reversal from the rendered intermediate height/opacity, and final-state cleanup. The helper should immediately remove `hidden` before opening, immediately apply `inert` and `aria-hidden` before closing, restore focus to a trigger if focus was inside a closing panel, and apply the final `hidden` state only after an ordinary visual exit completes. Expected cancellation of an adapter-owned `Animation.finished` promise is handled as normal reversal; unexpected failures remain observable. A live reduced-motion listener settles only animations retained by this adapter and applies their latest requested state synchronously.

Integrate the helper with the existing `.query-disclosure__toggle` installer and the explanation Config panel. Coordinate native group animation with `installWorkbenchNavigation()` and its one existing open-state owner. Query editor, embedded results, explanation modes, compare pane, and diff-modal motion must start only for semantic state changes; do not start or restart animation from every progress/render call. Ensure closing Compare cancels its pending explanation immediately, restores focus to the trigger, marks the secondary pane inaccessible immediately, and then allows a short outer-pane/layout transition. Ensure Diff locks/unlocks background and restores focus immediately while its visual fade completes. Animate query result/status appearance at the outer result/status containers only. Add button pressed feedback and unify existing disclosure chevrons, comparison layout, explanation panel, and spinners around the shared tokens. Do not transform editor or graph internals.

Finally regenerate TypeScript outputs, rebuild/package Workbench, run the focused browser suite in Chromium and WebKit, execute the existing query/compare interaction regression, run frontend unit tests and the Workbench module gate, and capture actual browser screenshots at transition midpoints and final states. Inspect that narrow layouts remain contained and that the dark/light palette, logo, and existing page geometry remain unchanged. Update this plan with measured evidence and a completion retrospective.

## Concrete Steps

From `.`, preserve the existing dirty tree and create only the new test, plan, generated TypeScript outputs, and necessary focused source changes. Start with the failing regression from `e2e/` using the already installed Playwright executable and a unique test-owned loopback Workbench base URL. Capture its log under `/private/tmp/workbench-motion-red-20260926.log`; append a compact red excerpt to root `initial-evidence.txt` without replacing existing evidence.

After the red test is recorded, edit only TypeScript sources and CSS/XSL needed for the shared motion contract. Run `bash tools/workbench/compileTypescript.sh` from the repository root. Rebuild through the repository's required offline Maven workflow with `.m2_repo`; keep `maven-build.log` append-only, and do not run tests with `-am` or `-q`. Run Playwright from `e2e/` with `RDF4J_WORKBENCH_BASE_URL` pointed at this task's packaged loopback fixture, selecting both `chromium` and `webkit`, a line reporter, and a unique output directory under `/private/tmp`. Run `npm run test:unit` from `e2e/`. Run the focused pre-existing compare-close/query interaction selection and, after the focused browser greens, the Workbench module suite via `mvnf --retain-logs`. Do not stop a process unless its PID/session and port were created for this task.

Before handoff, run the required resource/formatter processing with `.m2_repo`, `scripts/checkCopyrightPresent.sh`, and `git diff --check`. Save screenshots of the actual Query page with Query Options closed/open, Compare closed/open or closing, and reduced motion applied; use Playwright animation state polling rather than fixed-delay sleeps. Retain logs, screenshots, generated sources, and all pre-existing untracked artifacts.

## Validation and Acceptance

The new regression must fail before production changes because the current disclosure changes `hidden` immediately and has no reversible visibility animation or focus-return contract. After the implementation, in Chromium and WebKit the opened Query Options panel reports an adapter-owned running animation while its final state is not yet settled; a rapid reverse ends with the panel hidden and `aria-expanded="false"`, `inert` applied, and focus on its trigger. Keyboard activation opens the same panel and keeps focus visible. Navigation retains one open multi-item group and the active page group. Closing Compare synchronously aborts secondary work, restores focus, marks the secondary pane `aria-hidden` and inert, preserves the primary result, and completes its short visual collapse; Diff focus/background semantics are immediate while the overlay fades.

When the test changes `prefers-reduced-motion` to `reduce` during an owned animation, that animation settles to the latest requested state without waiting for a duration. Computed styles show no positional or scale motion, and all Workbench-owned spinner rotation is disabled; ARIA, visible content, and open/closed indicators still communicate state. The live preference change must not cancel animations on CodeMirror, SVG/DOT, or unrelated targets. Mobile widths remain within the viewport, desktop editors continue to fill their work area, and no user action waits for visual motion before performing its request/cancellation.

The focused browser selection and the existing query/compare interaction regression pass in both Chromium and WebKit. Frontend unit tests, TypeScript compilation, and the Workbench module suite pass. Formatting, copyright, and whitespace checks pass. Save the exact Playwright summaries, Maven report/log paths, screenshots, and any environment-only failure/retry distinction in this plan and `initial-evidence.txt`.

## Idempotence and Recovery

The browser test uses one fresh repository/app-data fixture owned by this task and does not submit destructive actions against any other repository. Use a fresh unused loopback port after checking listeners; do not touch ports owned by prior fixtures (including 8080, 8082, 8083, 8134, or 8135). If a test fails before reaching its product assertion, preserve the log and fix the harness before treating the result as product evidence. If Maven tests cannot bind loopback inside the default sandbox, preserve that report and rerun the same authorized selection with loopback access. Never delete pre-existing untracked files or replace existing evidence logs. A cancelled or superseded owned animation is expected during rapid reversal; final state must still be deterministic. If an unexpected animation fault is observed, keep it visible in the browser error/console channel and fix its cause instead of swallowing it.

## Artifacts and Notes

The pre-existing tree contains accepted Workbench redesign, query/data/configuration changes, tests, generated TypeScript outputs, design boards, and untracked artifacts. None are owned by this motion plan unless a motion regression requires a narrowly scoped change; preserve all of them. The plan's initial red evidence, final browser logs, screenshots/video, Maven logs, and final artifact inventory will be added here as they are created.

## Interfaces and Dependencies

Use only browser-standard TypeScript/DOM APIs and the existing Playwright installation. The shared API belongs in namespace `workbench` in `tools/workbench/src/main/webapp/scripts/ts/template.ts`; query code in `scripts/ts/query.ts` uses that API. It must expose a visibility operation for a specific `HTMLElement` and desired boolean state, store the animation handles it owns, finish or cancel those handles when reduced motion changes, and make final visibility/accessibility state deterministic. CSS motion tokens belong in `workbench-refresh.css`; query-specific appearance belongs in the existing query stylesheets. Do not add dependencies or replace native HTML controls/disclosures with a new UI framework.

## Plan Revision Notes

2026-09-26 20:58Z: Created the motion ExecPlan after inventorying the shared template installers, query render/update flow, compare lifecycle, and existing stylesheet transitions. Chose a shared adapter with explicit animation ownership because query rendering is frequent and vendor/editor animations must remain outside Workbench cancellation. The pre-existing form-sizing plan's final rendered-coverage ledger was also reconciled with the already accepted browser closure so this document is the only active plan.
2026-09-26 21:11Z: Added `e2e/tests/workbench-motion.spec.js` and ran it against an isolated packaged fixture in both engines before production changes. Chromium and WebKit each show four behavior failures: no reversible Query Options animation, no owned animation to settle after a live reduced-motion change, no animated native group content, and no Diff overlay fade. Harness setup creates a UUID-like absent repository and runs a real query/Explain before checking compare, so these are product contract failures rather than missing test data or hidden controls.
