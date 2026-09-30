# Give Workbench spacing and alignment consistent meaning

This ExecPlan is a living document maintained according to `.agent/PLANS.md`. Its Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective sections must reflect the current work. Work stays on `GH-6071-workbench-redesign`; this task does not authorize a commit or push.

## Purpose / Big Picture

Workbench users should be able to see which labels, inputs, and actions belong together and where a different topic begins. Today several shared and page-specific rules assign different button margins, padding, and gaps to equivalent controls. This work audits every actual route and every revealable control, lists the inconsistencies before changing production, and repairs them through one semantic spacing system. Colors, content, query behavior, motion, sticky headers, and native control accessibility remain within their existing contracts.

## Progress

- [x] (2026-09-29) Establish branch, clean tracked baseline, instructions.
- [x] (2026-09-30) Inventory all routes and measured states.
- [x] (2026-09-30) Publish complete initial inconsistency ledger.
- [x] (2026-09-30) Observe smallest spacing regression failures first.
- [x] (2026-09-30) Implement shared semantic spacing and alignment.
- [x] (2026-09-30) Verify every finding and coverage cell.
- [x] (2026-09-30) Obtain independent rendered interaction milestone review.
- [x] (2026-09-30) Complete validation and evidence handoff.

During active execution exactly one item was marked `in_progress`. All steps are now complete after independent artifact approval. Inventory, initial publication, and observed failing regression evidence preceded every production owner correction.

## Surprises & Discoveries

The current baseline is commit `d6e9ea70a547b48942106cec515cdb6839c21728`. The preceding publication ran a successful root clean install and passed all 602 Workbench Java tests and all 239 frontend unit tests. A separate optional browser run had 37 passes and 20 failures. Its mostly motion-related assertions and one Firefox dynamic-disclosure failure are baseline observations, not newly introduced spacing regressions. The preserved output is `logs/workbench-publication-20260929-6q7uoypn/browser-tests.log`.

`tools/workbench/src/main/webapp/styles/workbench-refresh.css` declares 36px desktop controls and a 44px touch-height token, with 8px control, 12px form, and 4px field gaps. Rendered controls can have larger outer heights or edge distances because borders, margins, padding, and flex distribution contribute. Measurement therefore uses actual element rectangles as well as computed style and selector ownership.

The first rendered route/create pass collected 502 captures across 17 route destinations and 18 repository backend forms at 1440px, 390px, and 320px in both themes. These route documents stayed contained without page overflow. The subsequent conditional query, editor, picker, and operation passes completed the initial ledger. The current query renderer mounts results inline under `[data-query-stream-root]` and retains a hidden iframe; audit selectors follow the actual renderer rather than the historical iframe assumption.

Live navigation uses server-supplied `defaults/workbench.properties`: five groups and three multi-item disclosures (Repositories, Explore, Modify), with query/saved/export under Explore. `workbenchViews.ts` also has a six-group/four-disclosure fallback. The coverage denominator distinguishes these configurations. Before interpreting the baseline, a loopback provenance check compared all 42 served JavaScript/CSS assets with the workspace: 42 SHA-256 matches and no mismatches. The manifest is `/tmp/rdf4j7-workbench-spacing-20260929/baseline-served-asset-manifest.json`; repeat this check after rebuilding/restarting for fixed captures.

A separately preserved pre-existing protocol failure occurs when a query fixture has namespace mappings: `Invalid Workbench query stream: namespaces must be prefix/name pairs declared before rows.` Its baseline evidence is under `browser/baseline-query-v2/0001-query-tuple-long-paged-1440-light.{json,png}` in the temporary evidence directory. Geometry uses a namespace-free query fixture and separate namespace-picker captures. This task does not repair protocol behavior; final limitations must retain this distinction.

Baseline field association exposed a shared cascade error: native text inputs had a 5.33px label distance while decorated selects had the intended 4px. Legacy `styles/default/screen.css` assigns inputs/selects a 1pt margin; modern text controls did not reset it. Advanced disclosures also used the label-association gap for successive field rows. Independent query review observed about 8px between Execute and Explain, about 18px between Save query and Options, and an 8px right inset between Options and the editor. These observations were included in the complete measured ledger before production changes.

## Decision Log

Decision (2026-09-30): preserve the existing 55vh cap for long read-only saved sources. The candidate passed strict one-line content-height equality, but a 25-line wrapped case at 390 px has 554 px of content in a 550 px scroll cap, producing the expected 552 px bordered editor. The first long-source assertion incorrectly demanded 556 px despite the documented bounded-scrolling exception. Refine only that long-case assertion to the smaller of content and computed cap plus borders, and require scroll reachability; preserve the original 302 px versus 31 px RED and strict short-source equality.

Independent root candidate review passed desktop action gaps/end alignment, Config opening and keyboard closing through settled hidden/zero-height state, rapid Save reversals with trigger focus, Share containment at 320 px in ordinary/fullscreen editors, and Memory Create required/advanced/action layouts at 1280/320 px. Root also measured a mixed checkbox-row issue: the logical label was only 21 px high beside 36 px inputs, shifting its compact glyph center about 8 px down. A focused mixed-row RED preceded its shared-role correction. Read-only source review added finite existing-class checks for Explore's optional gap, Remove/Clear mobile field/action rhythm, and compact close/tree inset ownership; rendered measurements preceded each correction.

Decision (2026-09-30): extend verification with the independent review findings before final closure. `browser/red-review-extensions.log` records six genuine in-repository failures before their owners change: Explore's 0 px subgroup, legacy mobile form rhythm (15 px field distance and 6.5 px association), compare close padding of 7 px/12 px, unequal 26×36 px branch versus 24×24 px leaf slots, saved metadata meeting source at 0 px, and a 21 px Save checkbox label beside 36 px controls. Preserve these as existing S1/S3/S4/S5/S6 extensions plus S13. Repair shared control-row and component ownership, not individual control offsets. The same run's S7 failure is a virtualized fixture lookup error after ten successfully measured cards; the worker corrects that supported public scrolling setup separately.

Independent root saved-source review confirmed short-source compactness, long-source reachability through its final line, responsive heading/action containment, and complete removal of detail space after Hide. A further shared result-panel defect was reproduced before correction: the separately flowing panel track did not travel with its sticky toolbar, producing a 19.43 px desktop anchor and a −16.14 px overlap at 320 px after another saved card was revealed. The correction preserves sticky behavior while keeping the trigger and panel in a single flow owner through scrolling and resizing.

Decision (2026-09-30): S14 is confirmed before correction by `browser/red-grid-and-anchor.log`: main Download anchor 13.578125 px versus 8 px. Stress geometry also records saved initial 19.40625 px, scrolling −147.8125 px, and open resize −80.453125 px. The same focused run proves a 14 px mobile field-group distance in Server/Add/Export versus the 12 px shared role (S3). Move the result panel track into its sticky toolbar grid, reset additive heading/panel box spacing, and give narrow stacked layouts an active panel row immediately after its trigger. Follow rendered `[hidden]` state during existing close transitions. Preserve report evidence and existing motion rather than adding offsets or scroll listeners.

The permanent finite matrix exposed a missed supplemental paged Explore relation-group overflow at 320 px: its legacy presentation table extends to x=323.875 px while the card content ends at x=291 px. S12 is added to the published ledger, with `browser/red-relation.log` and its geometry/screenshot/trace proving the defect before that layout owner is edited. The initial list and prior captures remain preserved. Six other baseline matrix failures came from a fixture assuming 50 rows instead of selecting the live default of 100; the fixture now explicitly selects 50.

Decision: use repository Playwright. Rationale: the Browser plugin and browser skill are absent; the frontend-testing-debugging skill directs this fallback. No browser or JavaScript dependencies will be added. Date/author: 2026-09-29, Codex.

Decision: split source inventory and browser audit into disjoint workers, with this coordinator owning production CSS/TypeScript and all builds/server lifecycle. Rationale: a complete inventory and measured evidence can progress independently while preventing conflicting shared-style edits. Date/author: 2026-09-29, Codex.

Decision: classify spacing by relationship, rather than by route-specific offsets. Initial relationships to evaluate are label/helper association (4px), related controls/actions (8px), successive fields and affected content to actions (12px), subgroups (16px), and separate topics/sections (24px). Rationale: smaller distances should convey association and larger distances should convey separation. Any justified exception must be documented and tested; the final contract follows the complete baseline audit. Date/author: 2026-09-29, Codex.

## Outcomes & Retrospective

The first shared implementation compiled and passed all 239 Node tests and all 602 Workbench Java tests. The sandbox Java attempt reproduced seven localhost socket denials; its reports are preserved separately, and the scoped localhost-enabled rerun passed with zero failures/errors/skips. Copyright/SPDX, formatting, and offline quick installation passed. The owned preview was restarted on the same port/data with the new package, and all 42 served JS/CSS assets matched the workspace in `fixed-served-asset-manifest.json`. Rendered closure was still open at that first implementation milestone.

The review-extension implementation also compiled, passed 239 Node tests and required formatting/headers, and completed an offline root quick installation in 17.294 seconds. The restarted `fixed-v2` preview had 42/42 served asset hashes equal to source. Its browser closure included S1–S14, existing-class review instances, the corrected bounded S7 contract, and one short-viewport S14 reachability check. Java sources remain unchanged after the successful 602-test module gate. The stable review repository remains owned; MemoryStore statements require reseeding after each restart, independently of retained repository configuration and saved-query metadata.

Independent root review is complete with no remaining spacing finding: 16 px saved metadata/source separation, two populated result rows, 8 px anchors through 320×568 scroll and 768×520 resize, no horizontal overflow, settled zero-space collapse, centered 36/44 px mixed logical rows, and visible keyboard close/focus for Save and main Result options. Final main Options and toolbar right edges both equal 1235 px, with no trailing inset. Root approved the reconciled ledger, validation boundaries, scope manifest, and final desktop screenshot. The two representative saved screenshots remain retained; the preview and fixtures stay available for user review.

Final `fixed-v3` production validation passes: TypeScript compilation, required copyright/SPDX and formatting, 239/239 Node tests, and offline root quick installation (17.040 seconds). All 42 served JS/CSS SHA-256 values match the workspace. Every S1–S14 spacing selection closes in Chromium, Firefox, and WebKit through the preserved initial and focused reports. Chromium's finite audit passes 22/22. Firefox's first finite audit passes 16/22 and encounters six teardown failures; an explicit drain of the controlled abort handler repairs that harness, and the same six cases pass 6/6 without changing geometry assertions. WebKit's finite audit reports 15 passes and seven strict page-error health failures; all geometry assertions and captures complete. Its identical `ResizeObserver loop completed with undelivered notifications` diagnostic is directly reproduced in recorded tuple/graph baseline replays using 42 HEAD assets (29 used per fresh context), before and after fixes. Those representative differential replays do not imply that each of the seven cases was separately compared. The strict saved-row focus reproducer remains RED in all three engines and is reported separately. Final scope/evidence reconciliation and root artifact review are complete; no further source, test, cleanup, or publication work is pending for this task.

Decision (2026-09-30, root): keep a verified pre-existing saved-list focus loss separate from S14 geometry. A row-window update moves the retained article on disclosure resize and drops focus to BODY; the same focused-control failure exists before S14 correction. Do not change the shared row renderer solely to make this audit entirely green. Preserve the strict reproducer and classify/report its RED explicitly, while running the complete spacing/settled-layout coverage separately. A separate saved fullscreen fallback is source-grounded at baseline HEAD, without a live failure claim; no functional fullscreen change is included.

The finite Chromium gate found a candidate regression in the new legacy mobile field owner: an implicit auto grid track retained native `size=48` input width (364 px) inside a 262 px field at 320 px. Initial-route baseline had no overflow. `browser/red-mobile-containment.log` confirms the smallest existing legacy-field selection failed before correction: Remove input right=393 px versus field right=361 px at 390 px. The shared mobile cell now has an explicit `minmax(0, 1fr)` column, allowing existing `max-width:100%` to resolve without forcing unrelated natural field widths equal. The candidate RED is preserved, and final finite geometry containment passes.

The bounded source review exposed one remaining S2 instance, confirmed before correction in `browser/red-result-end-edge.log`: desktop main Options ends at x=1387 px while its toolbar ends at x=1395 px. The unused internal fullscreen track retained an extra 8 px gap because the main route uses an external fullscreen control. Desktop toolbars without a rendered internal fullscreen control now reserve only their three relevant columns. Compact JSON text geometry is an intentional native typography difference: Range center is −0.796875 px vertically and −0.0078125 px horizontally, unchanged in a temporary flex-centered counterfactual; no optical offset or further production change is justified.

The complete initial ledger records eleven confirmed spacing/alignment classes S1–S11, published before production changes. The finite source-derived state matrix is measured or has a specific platform/authentication/baseline-functional limit; partial scratch failures remain distinct from later successful rendering coverage. The first regression run observed ten genuine failures (S1–S9 and S11). Its initial S10 outer-wrapper check missed the clipped textarea; a corrected strict popup/text bound check then observed failures for primary, compare, and update editors. All eleven classes therefore have preserved in-repository RED evidence before production. Logs and traces are in `browser/red-system.log`, `browser/red-popup.log`, and their corresponding results directories under the temporary evidence root. Later S12–S14 and review extensions also have observed RED; all fourteen spacing classes are now corrected and verified.

## Context and Orientation

The workspace is `/Users/havardottestad/Documents/Programming/rdf4j7`. `tools/workbench/src/main/webapp/scripts/ts/workbenchViews.ts` renders full routes, `queryStream.ts` renders dynamic query results, `template.ts` creates and controls shared disclosures, `query.ts` manages query/explanation/compare state, and `create.ts` moves advanced repository fields. Java page-model handlers under `tools/workbench/src/main/java` supply their data. A disclosure is a control that reveals a panel. A semantic spacing role is the relationship a distance communicates, such as a label belonging to its input or a new section introducing another topic.

Shared styling lives in `tools/workbench/src/main/webapp/styles/workbench-refresh.css`; query, explanation, and comparison styling also lives in `query.css`, `query-explanation.css`, and `query-compare.css` in the same directory. Legacy stylesheets and vendor editor styles also contribute to the cascade, meaning that later or more specific rules can change an earlier rule's result. Generated JavaScript and source maps under `scripts/` must be regenerated from TypeScript with the repository compiler.

The durable user-requested list will be `design/workbench-spacing-20260929/audit.md`, synthesized from `source-inventory.md` and `browser-audit.md` in that directory. Temporary measurements, screenshots, traces, browser logs, and runtime data go under `/tmp/rdf4j7-workbench-spacing-20260929`. Never remove prior artifacts. The source inventory worker owns only `source-inventory.md`; the browser worker owns only `browser-audit.md` and the new spacing test files under `e2e/tests/`. The coordinator owns this plan, the final audit, production files, builds, and the private preview server.

## Plan of Work

The first milestone derives the entire matrix from route registries, renderers, create-repository templates, conditional controls, shared menus and disclosures, query results, saved queries, and explanation/comparison surfaces. The browser worker then measures visible and revealed states at desktop and mobile sizes using fresh owned repositories. Every finding records the route, state, control, reproduction, actual and intended spacing or alignment, likely cause, correction, and verification status. The audit distinguishes source-only candidates, browser-confirmed defects, intentional exceptions, and unavailable states. It must include every create variant, empty and populated data, long or wrapped values, operation errors/confirmation/loading states, and all shared/dynamic renderer controls.

The second milestone turns each defect class into the smallest appropriate failing repository browser test. Repeated instances share a general contract but every audited instance still has a closure entry. The baseline failing output and screenshots are preserved before production changes. Existing baseline browser failures are retained and never weakened or muted to obtain a green spacing result.

The implementation milestone gives existing layout components explicit spacing roles and removes accidental additive control margins. It adjusts component styles and, only where needed to express real grouping, TypeScript markup. It preserves desktop/touch control sizing as measured, focus behavior, narrow-screen reachability, and field names/handlers. It does not add motion or unrelated aesthetic changes. The coordinator regenerates scripts only if TypeScript changes.

The closure milestone reruns the exact red tests, then the exhaustive matrix, targeted shared-style checks in light/dark and Chromium/Firefox/WebKit, frontend unit tests, and relevant Maven validation. The root reviewer receives a live preview URL and representative opening, closing, settled, focus, rapid-reversal, and responsive flows. The server remains running until that review is complete. Every confirmed audit finding must have a verified correction or a specific unresolved blocker; a sample of fixes is insufficient.

## Concrete Steps

Run repository commands from `/Users/havardottestad/Documents/Programming/rdf4j7`, unless a command explicitly names another working directory. Before edits inspect `git status --short --untracked-files=no` and `git diff`. Obtain route and control inventory with focused `rg` searches in the files named above and the actual Java/template registries.

Use the already installed Playwright package in `e2e/`. Exact baseline and final commands will be recorded after the inventory names the new tests. The expected pattern is:

    RDF4J_SERVER_BASE_URL=http://127.0.0.1:<owned-port>/rdf4j-server RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:<owned-port>/rdf4j-workbench node node_modules/@playwright/test/cli.js test tests/workbench-spacing-audit.spec.js --project=chromium --reporter=line --retries=0 --output=/tmp/rdf4j7-workbench-spacing-20260929/browser/baseline-results

The owned port and final concrete URL are saved in `/tmp/rdf4j7-workbench-spacing-20260929/preview.json`; replace the placeholder with that value. Launch only the owned Spring Boot jar on `127.0.0.1`, with fresh appdata in the same temporary evidence directory. Never stop or modify another runtime or use user repositories for destructive actions. The initial source is already installed by the successful root clean install earlier in this conversation. Before testing changed assets, refresh the installation and restart only the owned preview while preserving its data.

For TypeScript changes run:

    bash tools/workbench/compileTypescript.sh

After checking source headers with `scripts/checkCopyrightPresent.sh`, run required formatting:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The `-q` flag here is for formatting only; it must never be used for tests. All Maven invocations use `.m2_repo` and offline resolution first. Install/build output is saved to `maven-build.log`, with its previous contents preserved before replacement. The supported root installation is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick install

Capture full output in `maven-build.log` and print only errors plus the reactor summary. Long commands run in persistent background sessions. If an offline dependency is missing, retry once online and return offline. If a non-resolution error occurs, retry the exact installation without `-T 1C`.

Frontend validation runs from `e2e/`:

    npm run test:unit

Relevant Java validation uses the read `mvnf` skill:

    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs

The runner refreshes the root installation and saves test logs under `logs/mvnf/`. Tests never use `-am` or `-q`. Preserve reports before any cleaning or runner replacement. Append compact new evidence to the existing top-level `initial-evidence.txt`; do not replace its historical contents.

## Validation and Acceptance

The coverage matrix marks every actual page/state/control family as checked, together with the viewports and finding IDs. All pages are checked at 1440px desktop and 390px/320px mobile, with 768px checks for layout transitions. Shared-style regressions also exercise light/dark and all three supported browser engines. Measurements include control heights, border and padding insets, label-to-control distances, control/action edge gaps, topic separation, deliberate alignment, overflow, and surrounding layout after opening and closing.

All confirmed inconsistencies in `audit.md` must be corrected and verified; source-only candidates must be measured or explicitly resolved as intentional exceptions. Labels and fields remain associated, focus returns to persistent controls, responsive controls remain reachable, and query/data output contracts remain intact. The new smallest tests fail before the fix and pass afterward using the same selection. The full frontend suite and relevant Workbench tests pass, while any pre-existing motion/other browser failures retain their exact status and evidence. The root independent rendered review must be recorded before final sign-off.

## Idempotence and Recovery

The preview owns fresh uniquely named repositories and appdata; repeated runs create and delete only fixtures they own. Keep all existing artifacts. Archive reports before cleaning. If a server port is already in use, choose another free loopback port and record the new URL rather than stopping the listener. If sandbox socket or browser execution restrictions prevent validation, retry the same trusted local workflow with the required escalation and preserve both results. Production changes remain unstaged and uncommitted throughout this task. Any substantive issue outside spacing/alignment is recorded as a baseline or separate blocker, without silent changes to assertions or user data.

## Artifacts and Notes

The final evidence index will link the complete inconsistency list, full route/state coverage, red and green reports, before/after measurements and screenshots, exact build/test commands, browser health observations, intentional differences, and independent review. Baseline publication evidence remains in `logs/workbench-publication-20260929-6q7uoypn` and the existing root `initial-evidence.txt`.

## Interfaces and Dependencies

Use existing CSS custom properties and layout components, existing TypeScript modules, native DOM controls, the current bundled editor, the repository's installed Playwright, and the existing Maven modules. Add no dependency. A CSS spacing role may be a shared custom property or component rule, but its meaning must be stated in the audit and its physical result tested across the applicable component families. No public Java API or protocol/data format change is planned.

Plan revision (2026-09-29): created the self-contained audit-first plan and ownership boundaries before production work; recorded baseline validation and browser failures so new spacing evidence remains distinguishable.

Plan revision (2026-09-29): recorded the completed route/create baseline pass, actual inline query renderer, and measured native-input/disclosure/action cascade candidates while conditional-state auditing continues. No production edits or new test results are claimed.

Plan revision (2026-09-29): reconciled configured navigation against its fallback, verified served asset provenance, and retained a separately scoped namespace query-stream functional failure without widening production work.

Plan revision (2026-09-30): completed the bounded source-derived rendering matrix and published the full S1–S11 initial list, including Config, saved-editor/header and Share-popup findings, before tests/production; advanced the sole in_progress item to observed smallest regression failures. Final control closure subsequently confirmed saved Edit's XML response as a separate baseline functional limitation.
