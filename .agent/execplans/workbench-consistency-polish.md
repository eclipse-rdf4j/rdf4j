# Polish Workbench with one shared component system

This ExecPlan follows .agent/PLANS.md. Keep it self-contained and update its progress, discoveries, decisions, outcomes, and concrete steps as work proceeds.

## Purpose / Big Picture

Workbench pages should feel like parts of one application even when they are rendered by different XSL transformations or inside the query result iframe. This change applies one shared visual system to the query and embedded result surfaces, repository administration, import and export, creation, exploration and tables, saved queries, system information, and server/error pages. Someone reviewing the result can compare the query, Add, Information, and mobile Saved Queries screens against the accepted concepts in design/workbench-polish-20260925/ and see the same control sizing, typography, borders, focus treatment, spacing, and table rules.

The implementation keeps RDF4J's existing server and repository context, navigation, page copy, form names and values, defaults, handlers, query lifecycle, result paging, links, and downloads. CSS and XSL should reuse shared Workbench primitives rather than append a separate final override for each page. The user-facing behavior remains the existing behavior.

## Progress

- [x] (2026-09-25 00:38+02:00) Inspected and preserved dirty Workbench artifacts.
- [x] (2026-09-25 00:38+02:00) Built root quick profile offline.
- [x] (2026-09-25 00:38+02:00) Started isolated preview app data.
- [x] (2026-09-25 00:39+02:00) Copied and inspected four accepted concepts.
- [x] (2026-09-25 00:57+02:00) Added five red shared-style browser contracts.
- [x] Consolidate shared Workbench tokens and primitives.
- [x] Apply route-specific layouts and responsive rules.
- [x] Recheck rendered states and fidelity ledger.
- [x] Run Node and Workbench verification gates.
- [x] Correct shared control and metadata gaps.
- [x] Audit route families at desktop and mobile.
- [x] Reproduce embedded paging title error.
- [x] Restore embedded title target.
- [x] Rerun browser and module gates.
- [x] Verify saved-query code wrapping.
- [x] Refresh preview, ledger, screenshots.
- [x] Parent reviews final concept comparisons.
- [x] Reproduce final review defects.
- [x] Repair saved, editor, field primitives.
- [x] Refresh final comparison captures.
- [x] Parent reviews refreshed final artifacts.
- [x] Add shared chevron regression and reproduce red.
- [x] Replace glyph arrows across all expandable controls.
- [x] Correct narrow iframe control track.
- [x] Verify chevrons at all viewports.
- [x] Rerun focused browser and template checks.
- [x] Parent reviews final chevron captures.
- [x] Inventory navigation routes and selectors.
- [x] Add failing routewide selected-state browser test.
- [x] Unify active-link appearance across routes.
- [x] Verify menu destinations at both widths.
- [x] Capture selected states and record review.
- [x] Parent reviews selected-navigation captures.

## Surprises & Discoveries

- Observation: The expected fixture preview was no longer running, and the offline Jetty Maven plugin was not cached.
  Evidence: lsof showed no listener on ports 8080–8084; Maven reported that jetty-ee11-maven-plugin 12.1.10 could not be resolved offline.
- Observation: Running the built Spring Boot JAR works as the local preview runtime when started with the authorized loopback permission.
  Evidence: the isolated process listens on 127.0.0.1:8080; the Workbench query route returns HTTP 200 and the repository reports size 6.
- Observation: Both in-app browser and connected Chrome blocked the local preview navigation.
  Evidence: the parent observed net::ERR_BLOCKED_BY_CLIENT in each browser; the parent authorized Playwright Chromium under frontend-app-builder hard rule 10.
- Observation: The generated concepts contain incidental teal Change links and stronger gradients than the locked palette permits.
  Evidence: concept-query.png, concept-add.png, and concept-information.png show those differences; the parent design lock explicitly sets slate Change links and a flat, restrained palette.
- Observation: The focused Maven runner rewrites the server-boot JAR while the local preview is serving from that JAR, leaving the running classloader with truncated lazy resources.
  Evidence: after the focused Surefire run, `e2e/workbench-refinement-preview-loopback-20260925.log` recorded `java.io.EOFException: Unexpected end of ZLIB input stream` from `ProxySettings.load`; the browser then received shell-less servlet errors even though the port remained open. Stopping the task-owned preview and starting the rebuilt JAR restored HTTP 200 Workbench pages.
- Observation: The embedded tuple document omitted the title element that `tuple.js` uses to update result paging ranges.
  Evidence: a post-build Chromium SELECT returned the expected result but raised `TypeError: Cannot read properties of null (reading 'innerHTML')` at `scripts/tuple.js:29`, where `#title_heading` is required when the result limit is positive; the embedded XSL currently renders the visible title as an un-id'd `h2`.
- Observation: The expanded saved-query preview wraps long SPARQL lines at narrow widths.
  Evidence: the new Chromium contract confirms CodeMirror's `lineWrapping` is enabled and the code content's scroll width does not exceed its viewport at 390px; no production edit was needed.
- Observation: The final visual audit found saved rows too tightly joined, editor utility icons overlaying long first-line text, and several mobile text/select fields below the 44px target.
  Evidence: the parent identified each mismatch in the current final3 renders for Saved Queries, Update, and Namespaces; the new red-first browser assertions must reproduce these exact geometry and consistency failures before shared CSS/icon-template edits.
- Observation: The editor overlap reproduces in the saved-query preview at 390px, and the Update YASQE utilities use teal strokes instead of the shared slate token.
  Evidence: `e2e/workbench-final-review-utilities-red3-20260925.log` fails because a saved-editor glyph intersects a visible utility icon; `e2e/workbench-final-review-utilities-red2-20260925.log` records the Update utility stroke as `rgb(15, 118, 110)` instead of `rgb(100, 116, 139)`. The repository helper caps saved query names at 30 characters, so the editor-contract fixture now uses a short name and sets a long query value in the real saved CodeMirror instance.
- Observation: mobile Namespaces form controls retain an explicit 36px height despite the shared 44px minimum-height media rule.
  Evidence: `e2e/workbench-final-review-red4-20260925.log` reports the Prefix field at 36px at a 320px viewport; the same focused selection reports zero spacing between saved entries and a checkmark on Update's Execute action.
- Observation: the isolated preview repository had no stored data at the final route sweep, despite the earlier fixture description.
  Evidence: the Server repository `/size` endpoint returned `0`; the default Explore, Namespaces, and Export screenshots showed empty tables. The supplemental pass then added five Turtle statements only to this task-owned MemoryStore so those three table families could be reviewed with real rows.
- Observation: the first long-result capture put its paging controls below the 1000px viewport.
  Evidence: the 1440x1000 screenshot showed populated rows but clipped the bottom paging area; the additional 1440x1200 capture shows rows 1–10 of 25, Previous/Next controls, the closed result-options disclosure, and the white embedded document through its bottom edge.

- Observation: Query and embedded result disclosures still lack the intended vector chevron, and the earlier affordance assertions accepted any nonempty pseudo-element text.
  Evidence: The new focused Surefire test fails because the shared XSL icon helper has no chevron branch, and both Chromium contracts fail because the query and iframe buttons contain no decorative SVG. A first browser attempt hit the stale preview classloader after Maven rewrote the JAR; restarting the task-owned process restored HTTP 200 before this valid red run.
- Observation: At 320px, the embedded result toolbar gives Download a roughly 100px grid track beside the 163px Options track, so its action icon and chevron protrude beyond the button.
  Evidence: The Chromium contract reports a negative right inset at 320px; direct DOM geometry measures the chevron ending 23px past the Download button edge. The same control has the expected inset at 390px.
- Observation: The initial Workbench source and preview applied Query-specific active styles to both the navigation list item and its anchor, duplicating the shared selected-link treatment.
  Evidence: The red Chromium contract measured a Query-only list-item fill, 3px border, and 3px anchor offset; the route matcher still correctly marked each of the 17 destinations with `aria-current="page"`.
- Observation: The final source removes the Query-specific active list-item and anchor rules, leaving the shared navigation anchor rule to style the selected route.
  Evidence: The preview was rebuilt after the stylesheet correction; the same routewide browser contract then passed all 34 route/viewport states at 1440px and 390px, including the embedded query result and representative Query/Add hover and keyboard focus states.

## Decision Log

- Decision: Render expandable control chevrons through the existing `workbench-action-icon` XSL template, including native `<details>` summaries, and rotate the same SVG for open state.
  Rationale: One vector path and shared stroke rules prevent route-specific Unicode baseline and weight differences while retaining native keyboard and ARIA behavior.
  Date/Author: 2026-09-25 / Codex
- Decision: Stack embedded result disclosure controls below 360px while retaining the title/fullscreen row.
  Rationale: The full Download label, action icon, chevron, and 12px edge padding cannot fit beside Options in the available 320px width; stacking preserves all controls and their labels.
  Date/Author: 2026-09-25 / Codex

- Decision: Treat the supplied token values and component rules as the implementation source of truth, and treat the four accepted images as page anatomy and hierarchy references.
  Rationale: The design lock explicitly corrects incidental colors and gradients in generated images while preserving their page composition.
  Date/Author: 2026-09-25 / Codex
- Decision: Keep shared rules scoped to Workbench page and embedded-result roots, with explicit component variants where workflows differ.
  Rationale: This keeps legacy pages outside Workbench and avoids divergent per-page copies while preserving distinct form behavior.
  Date/Author: 2026-09-25 / Codex
- Decision: Use the Playwright Chromium project for browser regressions and visual captures after both Browser/IAB entry points failed with net::ERR_BLOCKED_BY_CLIENT.
  Rationale: The parent verified the Browser failure and authorized this fallback under frontend-app-builder hard rule 10.
  Date/Author: 2026-09-25 / Codex
- Decision: Keep Maven package mutation and browser preview execution sequential; restart the preview after each Maven root quick install before browser access.
  Rationale: The server is launched from the package Maven rewrites, and an active JVM can then read truncated JAR resources through its lazy loader.
  Date/Author: 2026-09-25 / Codex
- Decision: Preserve the shared selected-link design on the anchor itself and remove the Query-only selected-list-item decoration.
  Rationale: Parent visual review confirmed the selected treatment is the shared slate fill, 3px slate left indicator, 7px rounded corners, semibold ink label, and compensated padding that keeps icon/text fixed. Route family nesting may retain its existing indentation, while the selected treatment and its layout position remain identical.
  Date/Author: 2026-09-25 / Codex

## Outcomes & Retrospective

The red-first browser gate is complete. `initial-evidence.txt` preserves the baseline: the query action computed 14px/700/8px instead of 13px/600/7px, Add source segment radius was 0px, Information had three independent islands rather than one shared container, saved metadata had 0px top separation, and an embedded table header stayed at 14px/700. The mobile saved-query action grid already met its two-column placement and 44px target geometry, so that existing behavior remains part of the passing regression contract. The standalone Playwright log is `e2e/workbench-consistency-red4-20260925.log`; no Surefire/Failsafe wrapper exists for these browser specs. Production edits start only after this observed red run.

Implementation now uses shared Workbench tokens and scoped primitives across the page shell, fields/actions, Add source segments, Information, tables, saved-query metadata/actions, query/compare/explanation, and iframe results. The design-lock contract passes 7/7 in `e2e/workbench-consistency-green-final-20260925.log`; the Node unit suite passes 88/88 in `e2e/test-unit-workbench-consistency-20260925.log`. The first broader existing-browser selection passed 21/24: its header-alignment test exposed a 3.1875px top-padding offset now corrected in `query.css`; the federate-count assertion saw three repositories because the isolated preview includes an extra third fixture, and the old all-pages seed test hardcodes repository `query-refinement-baseline-20260923-2365` which is absent from this task's isolated preview. Rerun the affected selections and record the fixture-bound limitations without deleting preview data. Copyright validation and the resource formatter completed successfully.

The final follow-up repaired the native chooser's outer geometry, utility disclosure typography, saved metadata separators, shared code font, and embedded white iframe coverage. The subsequent Chromium smoke exposed a missing embedded title selector used by tuple paging; the new XSL title id has a red Surefire/browser reproduction and green focused results. Final3 route captures include all nineteen route entries and every creation form. A last mobile saved-query assertion confirms code wrapping and no horizontal editor overflow without requiring any production change. The final consistency suite passes 15/15, and the Workbench module passes 446 tests with no failures/errors. The isolated preview remains available for the parent to inspect.

The parent's final visual acceptance reviewed the refreshed Query, Add, Information, Saved Queries, populated Explore/Namespaces/Export, Update, and long-result renders. The accepted result heading reads `Query Result (1–3 of 3)` rather than the concept's `Query Result (3)` because the range preserves existing pagination semantics. The final post-style/icon browser contract passed 20/20 and `QueryTemplateTest` passed 49/49. The earlier Node 88/88 and Workbench module 446/446 gates predate these final CSS/icon refinements; they were not repeated because the closing source changes were presentation-only. The one broader existing-browser run remains 22/24 due to two fixture-bound expectations: federation count two versus the preview's three repositories, and a legacy route targeting an absent repository. The fidelity ledger records the exact failures, latest screenshot checks, numeric design-spec corrections, and the parent's accepted anatomy/hierarchy review without claiming pixel identity. The prompt brief contains summaries of the four accepted image-generation targets, not verbatim prompts.

The final disclosure-chevron pass replaces typography-dependent arrows in Query, embedded result controls, Explain configuration, native details summaries, and mobile navigation with the same decorative SVG path (`m6 9 6 6 6-6`). The red-first template contract failed before the shared helper was added (`QueryTemplateTest`: 1 failure of 50); the strengthened browser checks also failed before implementation because the expected SVG was absent, then caught a real 320px Download-track overflow. The 320px layout now stacks Download and Options while preserving both controls and their labels. Final evidence is green: `QueryTemplateTest` 50/50 in `logs/mvnf/20260925-053544-verify.log` and focused Chromium 4/4 in `e2e/workbench-chevron-final-20260925.log`. Twenty-one desktop, 390px, and 320px captures (including open/closed and separately labeled keyboard-focus states) are under `design/workbench-polish-20260925/final-chevron-v6-20260925/`. The parent reviewed Query desktop closed/open, embedded results desktop open and 320px closed, Add desktop closed/mobile open, and mobile Menu open; geometry, centering, right inset, and narrow control readability were accepted. Copyright and resource-format checks passed, and final `git diff --check` is clean. IAB and connected Chrome remain blocked by `net::ERR_BLOCKED_BY_CLIENT`; Playwright Chromium is the authorized fallback. Parent visual review is complete.

The menu-selection follow-up is in progress. Its browser contract will navigate the 17 actual anchor destinations from `template.xsl` at desktop and mobile widths, verify that exactly the correct link receives `aria-current="page"`, and compare the shared selected fill, indicator, radius, text style, and icon position. It will also exercise hover and keyboard focus on selected links. The expected defect is the extra square/full-height decoration on the Query list item; the shared anchor treatment remains the visual source of truth, and route/group indentation stays intact.

## Context and Orientation

The affected Maven module is tools/workbench. Page markup is produced by XSL in tools/workbench/src/main/webapp/transformations, with shared page structure in template.xsl and a shared stylesheet at tools/workbench/src/main/webapp/styles/workbench-refresh.css. Query-page styles also live in query.css and query-compare.css; embedded tuple and graph result documents use tuple.xsl and graph.xsl and must match the parent query surface. Existing generated TypeScript and server transformations are already dirty and belong to the user's work; preserve them and their behavior.

The accepted references are concept-query.png (1487x1058), concept-add.png (1505x1045), concept-information.png (1504x1046), and concept-mobile-saved.png (783x2008), all under design/workbench-polish-20260925/. The fourth image is a tall mobile Saved Queries state; its native width is not a mobile browser breakpoint. Also capture actual 1440x1000 desktop and 390x1000/320x900 narrow viewports.

The active local preview uses the freshly built tools/server-boot/target/rdf4j-server-boot-6.2.0-SNAPSHOT.jar on 127.0.0.1:8080 with app data isolated under /private/tmp/rdf4j-workbench-preview-20260925. Its task-owned review repository is workbench-preview-20260925. The final supplemental review fixture stores five Turtle statements with two namespaces for populated Explore, Namespaces, and Export captures; this data remains isolated under the temporary app-data directory. Start it with:

    java -Dorg.eclipse.rdf4j.appdata.basedir=/private/tmp/rdf4j-workbench-preview-20260925 -jar tools/server-boot/target/rdf4j-server-boot-6.2.0-SNAPSHOT.jar --server.port=8080

Open http://127.0.0.1:8080/rdf4j-workbench/repositories/workbench-preview-20260925/query. The associated Server API is http://127.0.0.1:8080/rdf4j-server. Keep the preview isolated and do not delete its app data during this task.

## Plan of Work

First add a new Playwright contract file that compares equivalent controls and table headers across query, embedded results, Add, creation, Saved Queries, Information, and administration routes. It must fail against the current dirty implementation before any production CSS/XSL change. Include source-radio keyboard selection, the single Information surface with all actual values, narrow Saved Queries action geometry, and body/result-table overflow. Preserve this test file as a task artifact.

Then define or consolidate the shared tokens and primitives in the existing Workbench stylesheet. Shared page/header/surface/control/table rules must cover full Workbench documents and the embedded result document while remaining scoped away from unrelated legacy pages. Prefer editing the current owning rules and deleting duplicate overrides over adding another appended page-specific layer. Preserve existing runtime/HTML semantics and accessible focus behavior.

Refine the Add Source controls as an equal-width segmented presentation around the existing native File/URL/Text radios. Style the file selector button through the same control rules. Make Saved Queries actions two equal columns on narrow screens with Delete alone in the first cell of the next row; preserve the current action handlers. Place all system information sections inside one surface with separators and aligned responsive label/value rows. Apply the same subtle horizontal table header/row treatment to repository lists, namespaces, contexts, types, Explore, and embedded query results. Keep the full component system active on create variants, summary, server/login, export, update, remove, clear, delete, loading, error, and empty states.

After edits, regenerate TypeScript assets only if TypeScript changes are necessary. Run the required resource formatter and copyright check, the exact failing-then-passing Playwright selector, affected existing browser selections, the full Node unit suite, and the Workbench Maven module through mvnf. Keep logs and evidence append-only. Capture accepted concepts and current implementation screenshots and record at least five concrete fidelity comparisons, an above-the-fold copy diff, and the exact reason Playwright was authorized.

## Concrete Steps

Work from /Users/havardottestad/Documents/Programming/rdf4j7. Preserve the current tracked and untracked state. The root quick clean install has already passed on JDK 26 using the required local Maven repository; its full log is maven-build.log, and the previous ignored log was retained at /private/tmp/rdf4j7-maven-build-pre-workbench-refinement-20260925.log.

Use the repository Playwright project from e2e for browser contracts and screenshots. Its base URL can be set with RDF4J_WORKBENCH_BASE_URL and RDF4J_SERVER_BASE_URL. Do not run the whole suite with the Playwright reporter's default output over the existing test-results or playwright-report directories; use a task-specific output directory or retain direct logs under e2e with unique names.

For Maven verification, use python3 .codex/skills/mvnf/scripts/mvnf.py after the module selector is known. The mvnf skill deletes only target test reports for the selected module, performs the required root install, and records verify logs under logs/mvnf. Do not combine tests with -am or -q. Preserve initial-evidence.txt by appending, never truncating.

## Validation and Acceptance

The new regression must fail before production edits because the same action, field, disclosure, and table primitives render with inconsistent computed font family/size/weight, minimum height, radius, border, focus ring, or spacing across page families. After the repair, compare those properties across query actions, Add Source/upload, a create form, a saved-query action, repository administration, and the embedded result iframe.

At 390px, File/URL/Text remain keyboard-operable native radios, the selected segment is visible, Saved Queries actions occupy two columns with 44px targets and 8px gaps, Delete is alone on the next row, metadata has 16px separation and wraps, and no horizontal overflow appears. At 320px, 390px, 768px, and 1440px, verify the shared shell, controls, tables, and open states remain readable. The Information route must contain one white information island holding Application, Runtime, and Memory values with separators and 160px desktop label/value alignment; mobile rows must adapt without dropping actual values.

The palette follows the design lock: canvas #f1f5f9, white surface #fff, inset #f8fafc, ink #0f172a, secondary/navigation #334155, muted #64748b, fine separators #e2e8f0, control edge #cbd5e1, flat primary #0f766e/#115e59, and danger #b42318. Use the existing native font family; preserve 184px sidebar, 24px desktop gutter/insets and gap, 12px mobile outer and 16px inner spacing, 10px surface radius, 7px control radius, 36px desktop and 44px mobile control heights, 14px body text with 1.5 line-height, and the locked heading, label, code, and icon scales.

Acceptance also requires zero lost fields, defaults, query/result lifecycle behavior, paging, download, navigation, keyboard activation, or runtime values; focused Chromium and Node suites pass; the Workbench Maven module passes with zero failures/errors; copyright, formatter, and whitespace checks pass; and latest screenshots are inspected with view_image against the accepted concepts. Record at least five comparisons, including copy, geometry, palette, typography, spacing, responsive action layout, or table/result surfaces. State the above-fold visible-copy comparison and the Browser-to-Playwright fallback reason.

## Idempotence and Recovery

All design artifacts, tests, logs, and source edits must remain in the checkout or its authorized temporary data directory. Do not reset, clean, restore, branch-switch, commit, push, or remove existing artifacts. Use unique filenames for new logs and screenshots. If an edit changes behavior, maintain red-first evidence before touching production. If the root build or a test fails, retain its reports and diagnose the specific failure before repeating a changed command. Stop only the preview process started for this task when the parent no longer needs the fixture; do not stop processes identified only from stale PID files.

## Artifacts and Notes

Accepted concepts are stored beside this plan in design/workbench-polish-20260925/. Preview logs are e2e/workbench-refinement-preview-loopback-20260925.log. The offline Jetty plugin failure is retained in e2e/workbench-refinement-preview-20260925.log. The server appdata directory is /private/tmp/rdf4j-workbench-preview-20260925 and must remain isolated from the user's normal RDF4J data.

Design contract: design/workbench-polish-20260925/design-spec.md. Visual comparisons, copy check, native/actual screenshot dimensions, parent acceptance, and browser fallback record: design/workbench-polish-20260925/fidelity-ledger.md. Prompt summaries for the four accepted concepts are in design/workbench-polish-20260925/imagegen-prompt-brief.md. The final route and comparison captures are under design/workbench-polish-20260925/route-audit-20260925-final-review/.

Verification: `initial-evidence.txt` preserves the red-first and green checks. The final post-style/icon Chromium contract passed 20/20 (`e2e/workbench-final-review-contract-final-20260925.log`) and focused `QueryTemplateTest` passed 49/49 (`logs/mvnf/20260925-022439-verify.log`). The Node suite (88/88) and Workbench module (446/446) passed earlier, before the final style/icon edits; they were not rerun after presentation-only changes. The broad existing browser run has two fixture-bound failures, documented in the fidelity ledger and evidence: an expected two federation members against three in the preview, and a route pointing to a repository absent from the fixture. The final route audit reports 19 route families, 80 captures, 36 creation forms, and zero measured overflow. Copyright, formatter, and whitespace checks passed; the root offline quick clean install is retained in `maven-build.log`.

Review fixture remains available at http://127.0.0.1:8080/rdf4j-workbench/repositories/workbench-preview-20260925/query. IAB and connected Chrome both returned `net::ERR_BLOCKED_BY_CLIENT`; the parent authorized Playwright Chromium for screenshots and browser checks. An earlier broad browser run rewrote the already-dirty query-refresh desktop and expanded screenshots and added a modified narrow screenshot; all are retained, not restored or removed. Final browser checks excluded those capture cases.

## Interfaces and Dependencies

No new runtime dependency or public Java/API contract is introduced. XSL transformations preserve their existing request parameter names, form names, field defaults, IDs, JavaScript handlers, route targets, and result semantics. Native inputs remain the source of radio and file-selection behavior; CSS changes their presentation only. The shared stylesheet owns the visual variables and reusable Workbench control/surface/table primitives. Playwright is already configured in e2e/package.json and uses the repository's existing Chromium installation.

## Plan Revision Notes

2026-09-25: Created after the parent locked the cross-page consistency design. Recorded the generated references, the intentional palette correction over incidental image details, the isolated preview, dirty-artifact protection, TDD sequence, browser fallback evidence, and acceptance gates before production edits.
2026-09-25: Added the five-test Playwright contract before production edits and observed the baseline mismatches. The test setup waits for XSL-produced page surfaces and targets the iframe result table from its actual result-layout container; it runs in Playwright because no in-repo Surefire/Failsafe bridge exists for these browser tests.
2026-09-25: Completed shared route/iframe polish, captured the final concept and breakpoint renders, recorded red-first XSL and browser evidence, and finished the Node, Chromium, and Workbench module gates. Left the task-owned 8080 preview running for parent review.
2026-09-25: The final browser smoke exposed the missing embedded tuple title target. Add a focused Java/XSL regression first, preserve the page title's paging-range update, then rerun the browser and module checks before handing refreshed screenshots to the parent.
2026-09-25: Final visual inspection showed the expanded saved-query code preview potentially clipping at 390px. Verify it in Chromium and, if confirmed, add a red-first regression before changing the editor presentation.
2026-09-25: The focused mobile editor check passed: long saved SPARQL wraps within the 390px editor with no horizontal content overflow, so no code change was necessary. Final3 route screenshots and the 15-test Chromium suite are recorded in the fidelity ledger; the active item is parent review of the refreshed concept/render comparisons.
2026-09-25: Parent's final render review found two remaining Saved Queries issues and three additional shared-primitive inconsistencies: entry separation, editor icon overlap/color/metaphor, and sub-44px mobile text/select fields on Namespaces/server/create/remove. Reproduced them in browser contracts, repaired the shared CSS/XSL primitives, and passed the focused contract. The final supplemental captures now include populated Query, Add, Information, Saved, Explore, Namespaces, and Export states plus an expanded-height paged result with its controls visible. The preview repository was empty at route-audit time, so five Turtle statements were added only to its isolated MemoryStore for those populated review screenshots.
2026-09-25: Parent accepted the refreshed concept/render comparisons and populated route states. Closed the ExecPlan after documenting the restored `Query Result (1–3 of 3)` heading, final20/49 post-edit gates, pre-refinement Node/module gates, and two fixture-bound broad-browser failures. Added non-verbatim prompt summaries for the four concepts and recorded visual acceptance without claiming pixel-identical output. Final `git diff --check` passed; no commits or pushes were made.
2026-09-25: A follow-up review found that expandable controls still use typography-dependent Unicode arrows, and the query disclosures' 1.6px horizontal padding pushes the arrow against the border. Reopen this plan for a narrow shared-chevron correction: use the existing XSL SVG helper with the exact 24-viewBox path, 16px controls/18px menu, 1.75px rounded stroke, 8px gap, 12px edge inset, and a 180-degree open-state rotation. Cover Query Save/Options, embedded Download/Options, Explain Config, native details summaries, and mobile Menu without changing keyboard or ARIA behavior. First strengthen the browser contracts and add a focused template regression, then capture red evidence before source edits; finish with focused browser, Surefire, and open/closed screenshots at desktop, 390px, and 320px. The IAB and connected Chrome failure `net::ERR_BLOCKED_BY_CLIENT` remains the documented reason for Playwright Chromium.
2026-09-25: Completed the focused shared-chevron repair, recorded the pre-edit Surefire and browser failures plus the 320px overflow regression, and passed the post-edit 50-test XSL selection and 4-test Chromium selection. Preserved final open/closed/focus captures under `design/workbench-polish-20260925/final-chevron-v6-20260925/`. Parent reviewed the representative concept/render pairs and accepted the chevron geometry and narrow layout; the ExecPlan is complete. Prior artifacts and dirty user work remain untouched.
2026-09-25: Reopened the completed Workbench plan for the selected-navigation consistency follow-up. Parent supplied the accepted shared selected-link tokens and identified Query-specific list-item and anchor overrides. The focused Playwright contract failed on Query at both 1440px and 390px while the other 16 destinations matched. The source was updated to remove the duplicate Query rules while preserving route/group indentation and the shared anchor design.
2026-09-25: Rebuilt the boot JAR with the offline quick profile, restarted the isolated review fixture, and reran the full selected-navigation contract. It passed for all 17 destinations at desktop and mobile sizes, including representative Query/Add keyboard focus and hover states and the embedded Query result. The browser spec now creates and deletes its own uniquely named MemoryStore fixture rather than depending on the review repository; the corrected fixture run also passes. Captured fresh 1440x1000 and 390x1000 Query/Add screenshots; the preview stays available at `http://127.0.0.1:8080/rdf4j-workbench/repositories/workbench-preview-20260925/query`.
2026-09-25: Parent reviewed all four selected-navigation captures and accepted the consistent rounded anchor highlight across Query and Add at desktop and mobile. The navigation follow-up is complete; no additional product-code or screenshot work remains.
