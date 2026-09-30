# Keep Workbench query action buttons aligned

This living ExecPlan follows `.agent/PLANS.md`. Maintain its Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective sections. The work uses the repository's test-before-production rule. No branch change, commit, push, or artifact cleanup is authorized.

## Purpose / Big Picture


Editor Save query/Options and result Download/Options/Full screen currently use different card insets and separate layout owners. They end on different edges, and the main result puts Full screen in a different row from the other result actions even when there is room. Give the cards one content-inset rule and the related actions one reusable row layout, so changes to labels, content, visible actions, or viewport width do not recreate those differences. Keep panels associated with their trigger, keyboard behavior intact, and controls reachable when rows wrap.

## Progress


- [done] Trace shared card and action ownership.
- [done] Capture desktop and saved fullscreen failures.
- [done] Add responsive and disclosure regression failures.
- [done] Implement shared structural action layout ownership.
- [done] Integrate fullscreen manager into shared result code.
- [done] Resolve saved fullscreen spacing ownership.
- [done] Capture settled Firefox focus-transfer RED.
- [done] Repair shared Firefox focus transfer.
- [done] Validate settled fullscreen focus repair.
- [done] Reproduce saved fullscreen surface transparency.
- [done] Repair saved fullscreen surface ownership.
- [done] Verify packaged assets and final evidence.
- [done] Deliver final reviewed work handoff.
- [done] Reconcile final evidence and scoped handoff.

## Surprises & Discoveries


The four supplied screenshots show editor controls, Full screen, and result Options ending on different edges. At 1280 px the actual editor form and result card share their outer edges (232–1256), while the editor form's content edge is 1231 and the result card's is 1239, an 8px inset mismatch. The nested renderer adds 4px, putting result Options at 1235. Full screen is at y663.5 while Download and Options are at y706.4, a 78.9px span from first to last action edge. The first native measurement accidentally selected the zero-padding `#query-page` wrapper; the corrected regression targets the rendered `#query-form` card and now reports the real mismatch. The earlier mobile disclosure-pointer repair did not unify these card or action-row owners.

The fixture `FakeElement.hidden` property does not synchronize to its attribute. The renderer ownership unit therefore needed to walk ancestor `hidden`/`inert` properties when it counts visible headings; after correcting that oracle, the same focused test passes with the production patch. Its original pre-patch ownership RED remains preserved separately.

The saved fullscreen RED measured `top: 16px` despite `position: fixed`: the saved-query selector assigned a high-specificity `margin-top`, which overrode the shared result component's fullscreen margin reset. The repair changes the saved route to configure the result section-gap token consumed by the component, leaving margin ownership with the shared result component.

The settled native Firefox check confirms a separate cross-document focus transfer contract. The main result target enters fullscreen and takes body-lock ownership, but a hidden fallback iframe that was explicitly revealed and focused remains `document.activeElement`. Direct parent-button `focus()` cannot move focus away from that active iframe in Firefox; blurring the iframe or focusing the parent window before the control does. Chromium and WebKit pass the same scenario.

## Decision Log


Decision: reproduce content-edge, row, gap, and height contracts in native browser tests before changing production. Rationale: static selector assertions missed the measured nested padding and separate main-result header; acceptance must exercise actual layout. Date/author: 2026-09-30, coordinator.

Decision: use shared card-inset and action-group ownership instead of compensating offsets, equal button widths, or viewport-specific named action slots. Rationale: intrinsic labels and action visibility should determine wrapping, while a common owner determines spacing and row edges. Main and saved result callers must use the same ownership model. Date/author: 2026-09-30, coordinator.

Decision: during a streamed result, the renderer owns one visible and accessible result title and Full screen control; the legacy outer header returns when the renderer is disposed or fallback content is shown. Rationale: moving a Lit-owned shell node into dynamic renderer markup breaks ownership, while two simultaneously visible headers split the action row. Date/author: 2026-09-30, coordinator/root.

Decision: verify saved-query Full screen through the saved result target's own button, including entry and exit. Rationale: the existing saved result may route to the main page's `#query-results`, which does not exist on that route; that behavior must be reproduced before the shared ownership repair. Date/author: 2026-09-30, coordinator/root.

## Outcomes & Retrospective


The shared result toolbar now owns the title and action row for main and saved results. The shared fullscreen manager owns the actual result target, focus, Escape handling, and body lock; its focus transfer releases a focused iframe before returning focus to the parent trigger. Saved-route result surfaces now inherit the shared query palette tokens, so their fullscreen overlay is opaque and masks the Workbench chrome. The native RED measured `rgba(0, 0, 0, 0)` before that token-owner repair.

Validation is complete: native main/saved geometry and lifecycle passed 6/6 across Chromium, Firefox, and WebKit; the focused post-fix saved fullscreen case passed 3/3 across those engines. The focused fullscreen unit selection passed 3/3, query-result lifecycle passed 20/20, and frontend unit coverage passed 271/271. TypeScript generation, Maven formatting, Workbench install, Server Boot package, copyright check, and `git diff --check` passed. Final static assets matched source-to-WAR-to-served for 42/42 files with no mismatches. Main desktop and corrected saved fullscreen screenshots, RED/GREEN logs, and geometry attachments are retained in the task evidence directories and the preview asset manifest.

## Context and Orientation


The checkout is `/Users/havardottestad/Documents/Programming/rdf4j7`, branch `GH-6071-workbench-redesign`, HEAD `d062a1c96485f5aa8e269a3a3dc4c0e6e8ca5d31`. Tracked source is initially clean; 3,887 unrelated untracked artifacts must remain preserved. The existing loopback preview uses port 18817 and retained appdata under `/private/tmp/rdf4j7-workbench-spacing-20260929/runtime/appdata`.

`tools/workbench/src/main/webapp/scripts/ts/workbenchViews.ts` renders the main query editor and an outer result header. `scripts/ts/queryStream.ts` (under the same webapp directory) builds the dynamic result toolbar and its disclosures for both main and saved queries. `scripts/ts/query.ts` owns the main fullscreen integration. `styles/query.css` currently owns independent query/result padding, named action grids, and responsive overrides. `styles/workbench-refresh.css` owns shared visual tokens and disclosure styling. The shared disclosure implementation supplies trigger/panel IDs, accessibility, animation and measured pointer anchoring; preserve that contract.

The coordinator owns complex diagnosis and source edits. Existing Luna worker `/root/commit_push_workbench` owns test authoring/execution, routine commands, all build/preview/recovery, and evidence capture. The root reviewer independently checks rendered desktop/mobile opening, closing, focus, rapid reversal, and fullscreen. No parallel heavy workflow is allowed.

## Plan of Work


First trace both layout trees and choose the reusable action-row contract. In `e2e/tests/workbench-action-alignment.spec.js`, begin with the smallest native RED at 1280px in Chromium, then add bounded Chromium/Firefox/WebKit coverage for main and saved result rows, 390px/320px wrapping, content/scrollbar and inherited token changes, hidden actions, disclosure transitions, and saved/fullscreen entry and exit. Measure settled geometry from DOM rectangles. Also update `e2e/tests-unit/query-stream-refinements.test.js` with a focused renderer-header/fullscreen ownership contract. Capture each initial failure in the task evidence folder and append concise summaries to root `initial-evidence.txt` before production changes.

The implementation will make one card inset own each content edge and remove renderer padding that adds a second inset. Main and saved results will give related actions one structural layout owner; Full screen must not stay in a separate outer header while Download/Options use an inner toolbar. Shared row/group classes will own gaps, stretch alignment and wrapping rather than repeated named-grid rules. Editor actions will use the same row/group spacing contract. Preserve title association, feature visibility, fullscreen IDs and callbacks, disclosure associations, and row-store rendering geometry.

Run the unchanged focused regression after the patch, then directly related disclosure/fullscreen/layout tests and relevant frontend tests. TypeScript edits must be compiled with `bash tools/workbench/compileTypescript.sh`; generated JavaScript/maps are never hand-edited. Use header checks, offline formatting and a quick packaged preview. A CSS/layout change does not require repeating million-result measurements or backend functional suites unless an actual source change creates a new dependency. Record exact source/served hashes and fresh screenshots, and obtain root's independent visual review.

## Concrete Steps


All repository commands run from `/Users/havardottestad/Documents/Programming/rdf4j7`, except Playwright commands in `e2e`. The Browser plugin is unavailable in this session, so use the repository's existing Playwright setup and record this fallback. Before tests, use the required offline root quick clean install with `-Dmaven.repo.local=.m2_repo`; Maven tests never use `-q` or `-am`. Build/format commands use the same local repository. Keep prior build/test reports and logs before commands overwrite them.

The first native RED is `e2e/tests/workbench-action-alignment.spec.js`, test `main desktop actions share the card content edge and one row`, run from `e2e` with `RDF4J_SERVER_BASE_URL=http://127.0.0.1:18817/rdf4j-server RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:18817/rdf4j-workbench node node_modules/@playwright/test/cli.js test tests/workbench-action-alignment.spec.js --project=chromium --grep "main desktop actions" --reporter=line --retries=0 --workers=1 --output=/tmp/rdf4j7-workbench-button-alignment-20260930/main-desktop-red`. The browser is a real Chromium context using the recorded owned preview, not a mocked layout. Preserve its full log, geometry attachment and screenshot. Later selections expand to the other browser projects and saved-query lifecycle.

Observed native RED: `main-desktop-red-report.log` reports 8px card/content-edge mismatch, a 78.921875px result-action vertical span, result Options 4px before its card edge, and unequal gaps. Geometry and screenshot are retained under `/tmp/rdf4j7-workbench-button-alignment-20260930/main-desktop-red-report/`. The earlier wrapper-selector attempt is retained separately; it is diagnostic, not the acceptance baseline.

Observed renderer unit RED: command `node --test --test-name-pattern='result toolbar preserves typed native downloads, panels, and fullscreen handoff' tests-unit/query-stream-refinements.test.js` fails because the main stream borrows the shell's Full screen button (`notStrictEqual` fails). The new permanent unit assertions also cover streamed title labeling and legacy header restoration. Log: `/tmp/rdf4j7-workbench-button-alignment-20260930/renderer-header-red.log`.

Observed saved-route RED: command from `e2e`: `RDF4J_SERVER_BASE_URL=http://127.0.0.1:18817/rdf4j-server RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:18817/rdf4j-workbench node node_modules/@playwright/test/cli.js test tests/workbench-action-alignment.spec.js --project=chromium --grep "saved query fullscreen" --reporter=line --retries=0 --workers=1 --output=/tmp/rdf4j7-workbench-button-alignment-20260930/saved-fullscreen-red-confirmed`. On the real saved query page `#query-results` is absent and the result target is `#saved-query-results-0`; clicking its visible Full screen control leaves `data-fullscreen` unset. Log and screenshot are in `/tmp/rdf4j7-workbench-button-alignment-20260930/saved-fullscreen-red-confirmed/`.

Expanded Chromium RED coverage now resizes the same main result to 1280/390/320 px, measures card/action edges and wrapping, checks natural button widths and inherited control-gap changes, exercises panel switching/trigger stability, changes long labels/title and forces an internal result-table scrollport. The saved-query test also measures all three widths, clicks the saved target's Full screen control and Escape exit, and verifies disclosure position/peer behavior. Both full baseline logs and attached geometry/screenshots are in `/tmp/rdf4j7-workbench-button-alignment-20260930/main-responsive-red/` and `saved-responsive-red/`. Browser tests are isolated to the uniquely owned MemoryStore and delete the saved query/repository in teardown.

## Validation and Acceptance


At desktop width, related result actions occupy the same action row when their natural widths fit. Main and saved callers have the same content-edge, gap and row-height rules. Every wrapped action row ends at its card content edge without forcing equal button widths. The main editor and result cards share their inset rather than matching through copied offsets. Changing labels, card/control spacing tokens, scrollbar presence, or action visibility must preserve these rules. At 390px and 320px, all actions and panels remain reachable and document width does not overflow.

Opening or closing a disclosure, switching between panels, rapid reversal, fullscreen entry/exit, and saved-query show/hide must preserve aligned settled rows, focus, trigger/panel associations, and the single query request. Query result storage and evaluation semantics remain covered by the already completed functional gates; do not claim those gates were rerun for this task. Final handoff includes observed native RED/GREEN, directly affected verification, preview manifest, before/after screenshots, and any remaining limitation.

## Idempotence and Recovery


Tests create uniquely named owned repositories and use supported API operations. Retain user fixtures and unrelated repositories. The preview worker verifies exact process identity before replacing the owned server and preserves appdata, JARs and logs. MemoryStore statements are volatile across restart; restore the documented 120-row review fixture through its supported seed script after a new preview starts. Do not manually stash, reset, restore, clean, or delete unrelated files. A failed assertion must be diagnosed, not muted or replaced by an arbitrary wait.

## Artifacts and Notes


The four original screenshots are preserved in the clipboard directory named by the user task. Evidence is stored under `/tmp/rdf4j7-workbench-button-alignment-20260930/`; prior Maven logs, initial evidence, and Workbench/HTTP-client Surefire reports were copied there before the root clean install. Initial baseline: editor/result card right 1256px; editor Options right 1231px; outer Full screen right about 1239px; nested result Options right about 1235px at 1280px viewport. These are reproduction measurements, not hard-coded acceptance coordinates. The first native test asserts shared card content insets, editor/result action right edges, and a single equal-height/equal-gap desktop result row.

## Interfaces and Dependencies


Use existing TypeScript DOM/rendering helpers, shared disclosure abstraction, CSS custom properties and native flex/grid layout. Add no dependency. Shared layout classes and a shared card-inset token are internal presentation contracts; no query API, timeout, compression, result-storage or pagination behavior changes are intended.

Revision note (2026-09-30): created the plan for the new user-reported button alignment task after publication of the earlier load-more work. The earlier pointer fix and large-result verification are context, not proof of this new action-row contract.
