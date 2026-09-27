# Make Workbench Forms Compact and Consistent

This living ExecPlan follows `.agent/PLANS.md`. Keep its Progress, Surprises & Discoveries, Decision Log, Outcomes & Retrospective, and Plan Revision Notes current. The Workbench source and test commands below are sufficient to complete and verify this change without relying on conversation history.

## Purpose / Big Picture

Workbench configuration and option forms should use the same clear label alignment, control height, typography, spacing, and responsive behavior throughout the application. Short values such as repository IDs and numeric options should fit their purpose; addresses and query text should retain room to be edited; buttons should stay as wide as their labels require. After this change, operators can move between server settings, repository setup, query options, import/export, update, explore, and result controls without each area feeling sized by a different rule.

The visible behavior can be checked in an isolated RDF4J Workbench server by comparing the form routes at desktop and 320px/390px widths in Chromium and WebKit. Labels remain associated with their controls, controls remain keyboard-operable, and the existing request names, values, defaults, query settings, paging, import/export behavior, and editor behavior remain intact.

## Progress

- [x] (2026-09-25 20:07+02:00) Preserved current edits and mapped templates.
- [x] (2026-09-25 20:19+02:00) Add cross-surface failing geometry contract.
- [x] (2026-09-25 22:05+02:00) Extend red tests across option surfaces.
- [x] (2026-09-25 22:25+02:00) Normalize shared forms, panels, and controls.
- [x] (2026-09-25 21:53+02:00) Capture reviewed WebKit panel examples.
- [x] Add regression for explicit multirow select.
- [x] Fix row-aware shared control heights.
- [x] Verify core sizing and interaction gates.
- [x] Rerun full Workbench module gate.
- [x] (2026-09-26 20:58Z) Extend rendered coverage to audited forms.
- [x] (2026-09-26 20:58Z) Run expanded responsive browser checks.
- [x] (2026-09-26 20:58Z) Fix only reproduced layout defects.
- [x] (2026-09-26 20:58Z) Refresh coverage ledger and handoff.

## Surprises & Discoveries

- Observation: Workbench pages are generated from many XSL transformations but share one primary stylesheet and page wrapper. Query and query-result documents add their own `query.css` rules.
  Evidence: `transformations/template.xsl` loads `styles/workbench-refresh.css`; `transformations/query.xsl` and embedded tuple/graph result documents also load `styles/query.css`.
- Observation: Repository creation has 18 backend types, many emitted from table-based XSL templates. The shared stylesheet currently makes their text/select/textarea controls up to 600px wide, even when the field has a short `size` attribute.
  Evidence: `e2e/tests/workbench-create-contract.spec.js` enumerates 18 types; representative create templates use `size="16"`, `size="48"`, and `size="4"`, while `workbench-refresh.css` sets those controls to `width: min(100%, 600px)`.
- Observation: Namespace prefix and selector controls share a `workbench-inline-controls` class with no stylesheet rule, while single-field Server and Export forms use the same two-column grid as multi-field forms.
  Evidence: `transformations/namespaces.xsl`, `server.xsl`, and `export.xsl` compared with class rules in `workbench-refresh.css`; browser geometry will determine the visible correction.
- Observation: The starting worktree contains user changes alongside the previous query alignment repair. Preserve the default query-timeout edits in `WEB-INF/web.xml` and `query.xsl`, the timeout test changes in `QueryResultTemplateTest.java`, and all existing untracked artifacts.
  Evidence: the initial tracked diff lists those paths before any work on this plan.
- Observation: The rendered create page expands a 16-character repository ID to 600px and a 4-character sync delay to 572px, while a 48-character title is 592px. Chromium and WebKit both reproduce it.
  Evidence: `FORM_SIZING {"type":"memory","id":{"width":600,"height":36},"title":{"width":592,"height":36},"delay":{"width":572,"height":36}}` in `/private/tmp/workbench-form-sizing-red-20260925.log`; baseline full-page crops at `/private/tmp/workbench-forms-create-baseline-1440-20260925.png` and `/private/tmp/workbench-forms-create-baseline-320-20260925.png`.
- Observation: Query Save and Options disclosures also stretch to the full 1134px editor width, and the 32-character query name field grows to 651px.
  Evidence: `QUERY_DISCLOSURE_SIZING {"savePanel":1134,"queryName":651,"optionsPanel":1134,"settings":1110}` in `/private/tmp/workbench-query-disclosure-sizing-red-20260925.log`; the in-repository browser assertion fails because Save should follow its field widths.
- Observation: Native single-selects in embedded result panels and standalone result configuration still need an explicit shared height, and the standalone Export options wrapper includes excess space around one paging selector.
  Evidence: Parent visual review of `/private/tmp/workbench-form-polish-final-20260925/webkit-query-results-options-desktop.png` and `webkit-export-options-desktop.png` on 2026-09-25.
- Observation: Add Advanced's padded wrapper spends horizontal inset both on the disclosure child margins and the options-body padding; Create's last-row selectors also match nested advanced-field rows, and Explain Config uses a fixed 34rem panel.
  Evidence: Parent visual review of `webkit-add-import-options-narrow.png`, Create Advanced, and Explain Config captures on 2026-09-25; selectors at `workbench-refresh.css:1410, 1878`, and fixed width at `query-explanation.css:223`.
- Observation: Two-engine regressions confirm these are rendered geometry defects, not CSS declaration assumptions: WebKit single-selects remain 21px at desktop and 320px, and a synthetic four-row listbox collapses to 44px at 320px.
  Evidence: `MAIN_SINGLE_SELECTS`, `EMBEDDED_CONTROL_GEOMETRY`, and `MULTIROW_SELECT_HEIGHT` reports in `/private/tmp/workbench-select-wrapper-red-20260925.log`.
- Observation: Create's dynamically moved last advanced field is laid out as `display:flex` with its select 28px above the label bottom; Explain Config measures 544px at desktop, while Add Advanced's 238px field touches the panel's right border at 320px.
  Evidence: `CREATE_ADVANCED_FIELD_RHYTHM`, `EXPLAIN_CONFIG_BOUNDS`, and `ADD_ADVANCED_INSET` reports in `/private/tmp/workbench-create-explain-red-20260925.log` and `/private/tmp/workbench-select-wrapper-red-20260925.log`.
- Observation: The complete current contract now checks every creation template, server credentials, Add, Export, Update, Explore, Namespaces, Query, embedded result settings and download panels in both engines at four widths; all 10 tests pass before adding the query disclosure width assertion.
  Evidence: `/private/tmp/workbench-form-sizing-final-green-20260925.log`, ending `10 passed (14.4s)`; Update editor remains 918px at 1440px and 262px at 320px.
- Observation: Standalone Export Result options remained 202–207px tall after the first width correction because a generic 20px island padding was inherited around the outer details summary and options body.
  Evidence: The red assertion in `/private/tmp/workbench-export-height-red-20260925.log` reports a 203px desktop panel and 49px below-field gap. After assigning padding ownership to the summary/body, `/private/tmp/workbench-export-height-green-v2-20260925.log` reports a 141px-wide panel at 162–163px desktop and 175px at 320px, with 36px/44px summary and select targets.
- Observation: The complete sizing contract now passes in Chromium and WebKit after rebuilding the packaged Workbench CSS; it covers all 18 create types, server/settings/forms at four widths, native single-row/listbox geometry, Add insets, embedded result controls, Create advanced rhythm, and Explain Config bounds.
  Evidence: `/private/tmp/workbench-form-options-all-engines-20260925.log`, ending `18 passed (28.4s)`; refreshed WebKit captures are under `/private/tmp/workbench-form-polish-revised-20260925/`.
- Observation: Excluding only `select[multiple]` from shared explicit heights still flattens a single-selection native listbox declared with `size="4"`; query options also have duplicate height rules that can override the shared row-aware control policy.
  Evidence: Parent source review of `workbench-refresh.css:1174` and `query.css:294, 1189`; the regression adds non-multiple size-four listboxes in both main and embedded results and compares single-row controls at 768px before CSS edits.
- Observation: A red in-repo contract confirms that `size="4"` selects are collapsed to 36px at desktop and 44px at 768px/320px in both main and embedded query results; single-row controls share 36px above the existing 900px breakpoint and 44px below it.
  Evidence: `/private/tmp/workbench-single-row-listbox-red-v3-20260925.log`, with `NONMULTIPLE_LISTBOX_HEIGHTS` at 1440/768/320 and `2 failed` before the production edit.
- Observation: The row-aware shared selectors now leave explicit size-four listboxes at their native four-row height while preserving shared 36px desktop and 44px responsive single-row controls; query settings defer select heights to this shared rule.
  Evidence: `/private/tmp/workbench-single-row-listbox-green-20260925.log` reports 83–84px for main and embedded `size="4"` selects at 1440/768/320, 44px query-option selects at 768px, and `2 passed` in Chromium/WebKit.
- Observation: The first full Workbench module gate after the browser suites found one source-contract failure for Explain Config's prior `max-width: 100%` responsive fallback, plus five local-socket bind errors under the restricted sandbox.
  Evidence: `logs/mvnf/20260925-201223-verify.log` reports `Tests run: 452, Failures: 1, Errors: 5, Skipped: 0`; Surefire identifies `QueryTemplateTest.propertyVisibilityConfigShouldUseResponsiveFlowBeforeItCanOverflow`, and all five `ProxyUtilityCoverageTest` errors say `SocketException: Operation not permitted`.
- Observation: Explain Config retains its accepted intrinsic desktop width and stays within its 320px viewport after restoring the containing-block cap, in both browser engines.
  Evidence: `/private/tmp/workbench-explain-config-containing-block-green-20260925.log` reports 331px at 1440px and 263px at 320px with no document overflow; `2 passed` across Chromium/WebKit.
- Observation: The existing Explain interaction spec hard-coded localhost:8080, so its intended second-port fixture run could not start until its base URL was made configurable like the other Workbench browser specs.
  Evidence: The first attempt at `/private/tmp/workbench-explain-interaction-green-20260925.log` failed before page setup with connection refused on 8080; after using `RDF4J_WORKBENCH_BASE_URL`, `/private/tmp/workbench-explain-interaction-green-20260925-v2.log` passes in Chromium and WebKit against 8082.
- Observation: The required formatting/resources goal, repository copyright check, and whitespace check all pass after the final CSS and test-fixture edits.
  Evidence: `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` exits 0; `/private/tmp/workbench-copyright-final-20260925.log` ends with `All files have valid copyright headers and SPDX lines.`; `git diff --check` exits 0.
- Observation: The final `mvnf` Workbench module verify succeeds with loopback access, and the previous module contract failure plus sandbox socket-bind restrictions are resolved.
  Evidence: `logs/mvnf/20260925-202714-verify.log` reports 452 tests, 0 failures/errors, followed by Failsafe `No tests to run`; `initial-evidence.txt` retains the first red and final green evidence. The append-only `maven-build.log` prefix was byte-verified unchanged before the new root-install log.

## Decision Log

- Decision: Treat Workbench forms as a shared visual system while retaining distinct semantic sizes for short, standard, long-address, and multiline values.
  Rationale: The user's request covers all configuration and option surfaces; shared primitives avoid route-specific offsets and keep repeated choices consistent without making every field full width.
  Date/Author: 2026-09-25 / Codex.
- Decision: Use the existing 36px desktop and 44px touch tokens as target heights, with native controls and existing teal focus treatment retained.
  Rationale: The repository already defines these tokens and native select behavior; the WebKit browser checks catch platform sizing differences.
  Date/Author: 2026-09-25 / Codex.
- Decision: Add and run browser geometry/interaction assertions before production-style edits.
  Rationale: The issue is visible layout behavior across XSL pages and is best reproduced in a real browser at the affected breakpoints.
  Date/Author: 2026-09-25 / Codex.
- Decision: Keep the root Maven install log append-only and avoid the `mvnf` root install wrapper when it would truncate the existing log.
  Rationale: `maven-build.log` already contains user and prior verification evidence; a fresh root install must not erase it.
  Date/Author: 2026-09-25 / Codex.
- Decision: Constrain the Explain Config popover by its containing block with intrinsic sizing and `max-width: 100%`.
  Rationale: This keeps short configurations compact, allows long property labels to use available room, and prevents the panel from escaping its actual parent on narrow screens.
  Date/Author: 2026-09-25 / Codex.
- Decision: Size text/select/textarea controls from native `size`, `rows`, and `cols` metadata, add those attributes to long URI/address fields that lack them, and give number inputs a compact type-based size.
  Rationale: The forced-width rules discard XSL's existing sizing intent, while unannotated endpoint inputs inherit the browser's short default. Semantic metadata preserves usable width without field-name heuristics.
  Date/Author: 2026-09-25 / Codex.
- Decision: Use content-sized form grids and disclosure panels with a shared tighter row rhythm, while preserving roomy editor controls and full-width file pickers.
  Rationale: Browser measurements show stretch alignment and full-track overrides inflate credentials and option limits; the baseline screenshot also shows large per-field gaps and adjacent radio choices.
  Date/Author: 2026-09-25 / Codex.

## Outcomes & Retrospective

Core implementation and module verification are complete: shared Workbench control geometry follows semantic field sizing, shared desktop/touch-height tokens, consistent field rhythm, and intrinsic option widths. Single-row native selects use the shared size while explicit multi-row/listbox selects retain their native height. Query Explain Config is intrinsically sized and capped by its containing block. The module gate passed 452 tests. The supplemental rendered coverage for every Create type and remaining administration forms was completed and accepted in the final review; it changed no production behavior beyond the already accepted sizing repair.

## Coverage Ledger

| Form or option surface | Coverage |
| --- | --- |
| Server connection and credentials | Native field sizing, aligned rows, labels, and responsive bounds on the Server route. |
| Repository creation | All 18 backend templates plus type selection; short IDs, long titles/addresses, numeric controls, nested Advanced fields, and last-row stacking. |
| Query | Save, Explain, and Options panels; timeout, page size, inferred checkbox values, label association/focus, disclosure keyboard use, and Explain property/highlight settings. |
| Query results | Embedded tuple/graph layout and paging controls, downloads and disclosure, standalone Export Result options, row-aware native single-selects, and listbox preservation. |
| Import and administration | Add source tabs and Advanced insets, Export formats/contexts/types/download/limits, Update editor, Explore constraints, Namespaces, Clear, Remove, Delete, Saved Queries, and the Summary config disclosure are rendered; destructive forms are inspected without submission. |
| Responsive and native rendering | Main routes exercised at 1440, 768, 390, and 320px in Chromium and WebKit; no horizontal document overflow; shared controls measured at 36px desktop and 44px at/below 900px, while `size="4"` listboxes remain 83–84px. |

The full browser contract is `/private/tmp/workbench-form-options-final-both-engines-20260925.log` (`18 passed`). The existing query/results interaction selection is `/private/tmp/workbench-query-existing-interaction-regressions-20260925.log` (`6 passed`), and the populated Explain property-control interaction is `/private/tmp/workbench-explain-interaction-green-20260925-v2.log` (`2 passed`). The full Workbench module uses Surefire: `452 passed`, `0 failures`, `0 errors`, `0 skipped`; no Failsafe tests were reported. Its retained verify log is `logs/mvnf/20260925-202714-verify.log`.

Reviewed WebKit crops: `/private/tmp/workbench-form-polish-revised-20260925/webkit-export-options-desktop.png`, `/private/tmp/workbench-form-polish-revised-20260925/webkit-add-import-options-narrow.png`, `/private/tmp/workbench-form-polish-revised-20260925/webkit-create-advanced-desktop.png`, and `/private/tmp/workbench-form-polish-revised-20260925/webkit-explain-config-desktop.png`. The final containing-block CSS change preserves the measured 331px desktop and 263px 320px Explain panel bounds in both engines.

## Context and Orientation

Workbench markup is rendered from the XSL files in `tools/workbench/src/main/webapp/transformations/`. Ordinary Workbench pages use `template.xsl` and the shared `styles/workbench-refresh.css`; query editing and tuple/graph query results also use `styles/query.css`, including inside the embedded result iframe. `e2e/tests/` contains Playwright browser checks, and `workbench-create-contract.spec.js` already visits all 18 repository creation types. The browser suite uses an isolated Spring Boot server; this task must use its own fresh app-data directory and port 8082, leaving any existing listener on 8080 untouched.

The form inventory is grouped by behavior. Server connection and optional credentials live in `server.xsl`. Repository creation includes the type chooser, 18 backend-specific templates, and the data-driven `create-template.xsl`. Query language, editor actions, Save/Explain/Options settings, and embedded results live in `query.xsl`; tuple, graph, and boolean result renderers expose result options, downloads, and paging. Add/import uses source tabs, file/URL/text fields, format selection, optional settings, and upload actions. Export combines format selection, result limits, downloads, and result tables. Update uses a large editor. Explore, Namespaces, Delete, Remove, Clear, Saved Queries, repository lists, and summary/config details provide the remaining configuration, option, and administration surfaces.

Existing shared tokens include `--workbench-control-height`, `--workbench-touch-height`, and `--workbench-control-gap`. The work should adjust shared field/control rules and only touch XSL where semantic field roles or label associations are necessary to size or align correctly. It must not change the separate dirty timeout default hunk or user-owned `WEB-INF/web.xml` and `QueryResultTemplateTest.java` changes.

## Plan of Work

First, extend a focused Playwright spec to measure control rectangles, row/label alignment, natural versus stretched widths, viewport overflow, and label focus on the main route families and every repository creation variant. Run it against the current source in both Chromium and WebKit and retain the failing report before changing production CSS or XSL. Capture initial renders for representative complex forms so later screenshots show the actual before/after relationship.

Next, use the failing measurements to refine shared control and field sizing in `workbench-refresh.css`, `query.css`, and the Explain Config panel. Reuse the current tokens and page primitives; assign content-sized widths to short values, enough width to URL/path/text fields, and roomy widths/heights to editors. Normalize only single-row selects so native multiple/listbox controls remain multi-row. Fit standalone result-option wrappers to their fields, give disclosure content one owner for horizontal insets, scope create action-row styling to actual action rows, and size Explain Config around its dynamic contents. Correct alignment or responsive stacking in shared grid/inline/action rules. Add semantic sizing hooks in shared XSL templates only where existing metadata cannot distinguish field purposes. Keep radio/checkbox affordances, native selects, focus visibility, label associations, and all input names, defaults, and handlers unchanged.

Finally, run the browser contract at 1440px, 768px, 390px, and 320px in Chromium and WebKit, then capture representative server, create, query/result, Add, Export, Update, Explore, and Namespaces screens. Refresh the source/form coverage ledger. Run the required offline root quick clean install with output appended to `maven-build.log`, then focused XSL/Workbench checks and the complete Workbench module verify without `-am` or `-q`; use local loopback permission for server tests when required. Finish with format, copyright, and whitespace checks and inspect the final tracked diff.

## Milestones

Milestone one produces a source and rendered inventory plus a browser regression that fails against the current layout. The regression must identify concrete measured width/alignment mismatches, visit all 18 repository creation types, and show no change to existing control values or label activation.

Milestone two produces a cohesive shared sizing repair. After the change, desktop rows align their labels and control centers, single-row selects share the token height in main and embedded surfaces, short controls no longer occupy address-sized widths, long-value fields remain usable, and action controls size to their labels. Standalone Export options and Explain Config fit their content; Add Advanced has even insets; nested Create fields retain the same rhythm as neighboring fields. At 390px and 320px, forms wrap or stack without clipped controls or horizontal page overflow. A parent review receives representative desktop/mobile captures and the coverage ledger.

Milestone three closes verification. The same browser selection passes in Chromium and WebKit, focused template/servlet tests pass, the complete Workbench module passes, and the formatter plus `git diff --check` pass. Evidence and limitations are recorded here and in the append-only root `initial-evidence.txt`.

## Concrete Steps

From the repository root, add the new browser assertions to `e2e/tests/workbench-form-options-sizing.spec.js` (or the existing creation contract if shared setup makes that materially smaller). Run the selection from `e2e/` with `RDF4J_SERVER_BASE_URL=http://127.0.0.1:8082/rdf4j-server` and `RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8082/rdf4j-workbench`, using `./node_modules/.bin/playwright test` with `--project=chromium --project=webkit --reporter=line` and a unique `/private/tmp` output directory. Store the command output in a unique `/private/tmp` log and append compact red evidence to `initial-evidence.txt` before production edits.

For each build after the initial build, run the repository's offline root clean install using `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`, with output captured via `tee -a maven-build.log`. Run the focused browser checks without `-am` or `-q`; run Java tests with the workspace `.m2_repo` configured. For the final module gate, run the full Workbench `verify` after the root install, without `-am` or `-q`, and preserve its log/reports. If sandboxed tests cannot bind loopback, retry the same authorized gate with loopback access and document both results.

Before completion, run `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`, `cd scripts && ./checkCopyrightPresent.sh`, and `git diff --check`. Check `git status --short --untracked-files=no` and inspect only the intended source/test/plan changes; preserve existing untracked files and artifacts.

## Validation and Acceptance

The baseline browser test must demonstrate the reported sizing/alignment inconsistencies before production edits. The post-change test must pass in both Chromium and WebKit and compare actual DOM geometry rather than CSS declarations. It must visit Server, the base creation chooser, all 18 specialized repository creation types (including advanced settings), Query disclosures, Add source/options states, Export settings, Update editor/actions, Explore options, Namespaces controls, and tuple/graph result option/download panels. For each measured page it records control bounds and viewport scroll width at 1440px, 768px, 390px, and 320px.

At desktop widths, labels in a form group align on a shared column and controls in a row have a common center and token height; short ID/numeric/limit controls remain content-sized, address and query fields have practical editing width, and buttons/selects do not fill a track without a user need. At mobile widths, fields remain fully visible, hit targets meet the touch token where applicable, wrapping preserves the page width, and no form has more than 1px horizontal overflow. Export options compactness is measured against the 36px/44px summary and select heights plus a content allowance, preserving touch targets on narrow screens. Clicking or keyboard-activating labels and option disclosures continues to focus/toggle the corresponding native control. Form snapshots before and after opening disclosures retain values, checked states, disabled states, and selected options.

Capture full form/panel screenshots at representative desktop and 390px/320px widths in both engines where the native-rendering distinction matters. The Workbench focused tests and full module suite must report zero new failures/errors; record Failsafe status separately from Surefire. The root quick profile must succeed, the formatter and copyright check must pass, and the final diff must pass `git diff --check` without changing the pre-existing timeout edits or unrelated user artifacts.

## Idempotence and Recovery

The browser fixture must use a unique repository ID and a unique fresh app-data directory under `/private/tmp`; clean up only that test-owned repository through its test teardown and leave the app-data directory and all evidence intact. Never stop or change any listener on 8080. Stop only the preview process started for this task. Before any test that can rewrite the boot JAR, stop the owned preview, rebuild, then start it again to avoid lazy reads from a JAR being rewritten. If a browser test fails before the product assertion, preserve its log and correct the harness before calling it a regression. If Maven fails on a sandbox-only local bind, preserve reports, request loopback-enabled execution for the same authorized command, and report the successful rerun as an environment-restriction resolution.

No runtime library, public API, configuration behavior, repository creation semantics, form parameter, query default, paging, download, or editor behavior is intentionally changed. If satisfying a sizing assertion would require changing any of those semantics, record the issue and keep the patch to presentation only.

## Artifacts and Notes

The starting worktree includes the previous query-options alignment repair in `query.css`, `query.xsl`, and `e2e/tests/query-refresh-layout.spec.js`; it also includes existing changes in `WEB-INF/web.xml`, `QueryResultTemplateTest.java`, and an additional timeout default hunk in `query.xsl`. Preserve all of them. Existing untracked files are user artifacts; do not reset, clean, rename, overwrite, or delete them. Add only task-specific test logs, screenshots, and evidence with unique names.

At each milestone, add exact command lines, Surefire/Playwright summary snippets, report/log locations, screenshot paths, and source coverage counts here. `initial-evidence.txt` was kept append-only so the original red and green results remain inspectable.

## Interfaces and Dependencies

The implementation uses the existing XSLT 1.0 templates, `workbench-refresh.css`, `query.css`, and the already-installed Playwright package and Chromium/WebKit browsers. No new dependency is permitted. Existing XSL field metadata (`id`, type, role, size, rows, cols, placeholder) and native HTML form controls are the semantic basis for sizing; preserve server request names and form handlers. If a template needs a semantic width class, generate it from actual field role/type metadata rather than a route-specific pixel offset.

## Plan Revision Notes

2026-09-25: Created a supplemental plan for the user's broader request to make configuration and option controls precise across all Workbench surfaces. The source audit confirmed reusable height/spacing tokens and identified full-width short create fields, an unstylized namespace inline pair, and a shared grid used by single-field forms as concrete items for red-first browser measurement. Preserved the existing completed Workbench consistency plan and all starting dirty changes.
2026-09-25 20:19+02:00: Added a failing Playwright geometry contract and captured create-form baseline crops. Chromium and WebKit both report a 600px ID field and 572px numeric delay against a 592px title field, confirming that the shared create width rule overrides the fields' native size metadata. The contract includes all 18 backend templates and broader route/breakpoint checks; production edits may now begin.
2026-09-25 21:03+02:00: A follow-up rendered geometry assertion exposed Query Save and Options disclosures stretching to the full editor width; recorded the red 1134px panel and 651px query-name field before changing their CSS. The first full two-engine pass is green for 10 tests; current scoped task adds compact query-page panels and their narrow-width coverage before screenshots and the Workbench module gate.
2026-09-25 22:05+02:00: Parent review of final crops identified four additional shared-layout issues: native select height in result panels, oversized standalone Export paging options, asymmetric Add Advanced insets, and Create/Explain Config sizing. The plan is revised to measure these behaviors in-browser before the next production edit.
2026-09-25 21:54+02:00: Parent review then isolated inherited padding that left standalone Export Result options around 200px tall. The new geometry assertion reproduced this before the scoped padding fix; the final 18-test Chromium/WebKit contract passes and refreshed WebKit captures cover Export, narrow Add, Create Advanced, and Explain Config.
2026-09-25 21:54+02:00: Final source review identified that `[multiple]` alone does not identify multi-row selects (`size="4"` can omit it), and query-settings has duplicate 900px height overrides. Extend the red contract to both main/embedded listboxes and 768px controls before final verification.
2026-09-25 21:55+02:00: The red regression verifies both listbox contexts and confirms the existing shared 900px height breakpoint at 768px across Add, Export, Query Options, embedded result options, and downloads. The 900px query-result layout intentionally stacks fields, so center-alignment checks apply only above that breakpoint.
2026-09-25 22:07+02:00: Updated shared selectors to target default single-row selects and explicit `size="1"` selects only, kept native listboxes out of fixed heights, and removed duplicate query-settings select heights. The focused test is green; full cross-surface browser and existing query interaction regressions plus Workbench module verify remain.
2026-09-25 22:13+02:00: The final two-engine sizing suite passed 18 tests and the existing query/result interaction selection passed 6 tests. The first full Workbench gate exposed the Explain Config source-contract fallback mismatch and sandbox socket-bind restrictions; restore the containing-block `max-width: 100%` contract, verify narrow rendered geometry, then rerun under loopback-capable execution.
2026-09-25 22:20+02:00: Restored the actual containing-block cap and confirmed the accepted 331px/263px panel bounds in Chromium and WebKit. The legacy Explain interaction test hard-codes port 8080; make its base URL follow the existing E2E environment variables so its existing narrow/property controls can be exercised against the isolated 8082 fixture without touching 8080.
2026-09-25 22:21+02:00: The configurable Explain interaction selection passes in both engines with an executed query plan, property controls, focus navigation, and highlighting toggles; final module verification is now the only in-progress gate.
2026-09-25 22:22+02:00: Formatting/resources processing, copyright/SPDX validation, and `git diff --check` pass. Run the Workbench module selection with loopback-capable execution because five tests bind local server sockets under the sandbox.
2026-09-25 22:28+02:00: The final `mvnf tools/workbench --retain-logs` verify passed 452 tests with loopback access; Failsafe reported no tests to run. The root `maven-build.log` prior bytes match exactly and the fresh install output was appended. The owned port-8082 preview is stopped; representative WebKit crops and all browser/Maven reports are retained for parent review.
2026-09-25 22:31+02:00: Parent coverage review found all 18 Create templates were measured only at desktop and Clear, Remove, Delete, Saved Queries, and the Summary config disclosure were not yet rendered by the suite. Extend the real-browser contract across all requested widths and distinguish source-only inventory from rendered coverage; inspect fixture-owned data without submitting destructive actions.
2026-09-26 20:58Z: Completed the all-18 Create loop at desktop, tablet, 390px, and 320px, and rendered the remaining Clear, Remove, Delete, Saved Queries, and Summary configuration forms in desktop/narrow layouts. Parent's final source/screenshot review accepted the closure; the coverage row and progress ledger now reflect rendered evidence rather than a pending audit.
