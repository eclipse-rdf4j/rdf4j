# Complete the Workbench Redesign and Data Semantics

This living ExecPlan follows `.agent/PLANS.md`. Keep its Progress, Surprises & Discoveries, Decision Log, Outcomes & Retrospective, and Plan Revision Notes current. It supersedes the unfinished scope in `.agent/execplans/workbench-form-options-sizing.md` while preserving that historical plan and its evidence.

## Purpose / Big Picture

This work makes Workbench compact, consistent, responsive, configurable, and safe while preserving the original RDF4J capabilities. Operators can keep graph contexts embedded in uploaded data, download all query results by default, export repositories as N-Quads with optional Gzip or ZIP, close the comparison pane without disturbing the main query, and deploy a validated properties file that controls menus, pages, query capabilities, and theme defaults. Every disabled page/action is enforced by the server, not only hidden in the interface. Light and dark modes use a blue/slate palette calculated as complementary to the unchanged RDF4J logo; orange remains only in that original asset.

The completed behavior is observed through focused red/green repository tests, Workbench and server-boot tests, browser interactions at desktop and narrow widths in Chromium and WebKit, downloadable files that decompress and parse, and a generated light/dark design catalog covering each route family and option state. No framework or runtime dependency migration is part of this work.

## Progress

- [x] (2026-09-26) Refresh root quick build and inventory preserved artifacts.
- [x] (2026-09-26) Add failing upload context and override tests.
- [x] (2026-09-26) Implement opt-in upload context override.
- [x] (2026-09-26) Add failing query-download behavior tests.
- [x] (2026-09-26) Implement independent query-download defaults.
- [x] (2026-09-26) Add failing export compression tests.
- [x] (2026-09-26) Implement export-specific compression outputs.
- [x] (2026-09-26) Add failing compare-close tests.
- [x] (2026-09-26) Implement accessible compare-pane close lifecycle.
- [x] (2026-09-26) Align compare headers and targets.
- [x] (2026-09-26) Generate both-theme board targets.
- [x] (2026-09-26) Merge asset hashes and gallery mapping.
- [x] Specify configuration keys and page policy tests.
- [x] Add failing deployment theme binding test.
- [x] Guard and test client-only query features.
- [x] Correct vendor matching highlight palette.
- [x] Pass complete frontend unit suite, 95/95.
- [x] Pass query XSL feature tests, 2/2.
- [x] Expose theme default through Info.
- [x] Expose configured navigation through Info.
- [x] Expose query policy values to templates.
- [x] Enforce route, alias, action, and feature policy.
- [x] Fill equal-width compare editor columns.
- [x] Add system and remembered theme tests.
- [x] Add dark theme and responsive interface states.
- [x] Complete packaged visual and route acceptance.
- [x] Run Workbench, Boot, and Spring module gates.
- [x] Verify Chromium and WebKit interactions.
- [x] Update docs, catalog, and test evidence.
- [x] Reproduce namespaces, cache, Explain policy gaps.
- [x] Reproduce blank Explain format policy bypass.
- [x] Repair Boot context-mounted server routing.
- [x] Deliver final verified handoff.
- [x] Complete final workspace audit and evidence.

Exactly one unfinished item must be marked `in_progress`; change it before starting each new implementation phase.

## Surprises & Discoveries

- Observation: RDF4J already preserves embedded RDF contexts when `RepositoryConnection.add` receives no explicit contexts, and puts contextless statements into the default graph. `AddServlet` only supplies contexts when the request includes an explicit `context` parameter. The current user-facing form instead checks a “use base URI as context” option by default and browser code fills it automatically.
  Evidence: `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/AddServlet.java` and the `RepositoryConnection.add` contract; verify live source before changing it.
- Observation: query result download size currently falls back to the page limit if the download-specific value is absent, and the same XSL control template supplies the page-size and download defaults. A positive page limit is applied only when the authored SPARQL query lacks its own LIMIT/OFFSET.
  Evidence: `QueryEvaluator.getResultLimit`, `template.xsl` limit-selection template, and `PagedQuery`.
- Observation: Repository export already streams the whole repository, while query results use an unrelated shared default RDF format. Compression support currently applies to HTTP response negotiation, not an exported file archive.
  Evidence: `ExportServlet` and the shared `default-Accept` servlet parameter. Export must use its own default format and downloadable compression implementation.
- Observation: the central Workbench gateway is shared by WAR and Spring Boot packaging. The repository proxy has conditional-GET/304 handling before normal servlet dispatch, so an access policy added only inside individual page servlets would be bypassable.
  Evidence: `WorkbenchGateway`, `ProxyRepositoryServlet.service`, and `Rdf4jServerWorkbenchApplication`.
- Observation: Query page entry includes implicit GET execution, GET explanation, POST save/edit/execute/explain/cancel actions, and saved-query references; request parameters are normalized by `WorkbenchRequest`, whose effective values can include duplicate parameters, cookies, and legacy URL parameters.
  Evidence: `QueryServlet` and `WorkbenchRequest`. Feature tests must use that effective request model and cover aliases, not only a single `action=exec` parameter.
- Observation: Query and Update XSL currently read `/namespaces` for internal prefix metadata. Hiding the Namespaces page must not make those allowed pages fail, and metadata access must not exempt the interactive page route.
  Evidence: `query.xsl` and `update.xsl` namespace document lookups.
- Observation: the initial sizing and visual exploration were superseded by a source-mapped 32-board light/dark catalog, an exact-token preview, and a real Workbench redesign. The shipped palette uses calculated blue/slate tokens while preserving the original logo bytes.
  Evidence: `design/workbench-design-coverage-20260925/`, packaged screenshots under `/private/tmp/workbench-packaged-acceptance-final-20260926/`, and `e2e/tests/workbench-theme.spec.js`.
- Observation: the comparison Close button must share the same editor-label header row in both panes. A one-sided row shifted the right CodeMirror editor by exactly 44px; a symmetric label/Close row fixes the root layout cause and preserves a 44px touch target at 320px.
  Evidence: red and green geometry runs in `initial-evidence.txt`; screenshots `/private/tmp/rdf4j-compare-close-desktop.png` and `/private/tmp/rdf4j-compare-close-mobile-320.png`.
- Observation: compare mode currently declares three CSS grid tracks while rendering only its primary and secondary panes. This creates a 314.4px unused right track at desktop and makes the panes/editors differ by 46.8px.
  Evidence: red-first geometry contract in `e2e/tests/workbench-explain.spec.js`; report `/private/tmp/rdf4j-compare-editor-width-red-20260926.log`.
- Observation: after the desktop grid fix, the visible Close label overflows the 320px secondary pane by 5.34px beside its editor label; its hit target remains 44px high. The narrow header must compact or wrap without clipping.
  Evidence: `/private/tmp/rdf4j-compare-close-mobile-overflow-red-20260926.log`, appended to `initial-evidence.txt`.
- Observation: the rebuilt packaged UI resolves the dark brand, mobile header, form-label fill, and active navigation grouping issues. The broader Chromium/WebKit geometry suite then exposed a real 8px WebKit document overflow for the Memory Custom Rule form at 320px, plus an Add Text radio label that intercepts pointer activation in both engines. Parent visual review also identified low-contrast dark chevrons, an unnecessarily wide mobile logo plate, and a blank saved-query empty state.
  Evidence: `/private/tmp/workbench-packaged-acceptance-final-20260926/`; failed Playwright run was reproduced in `/private/tmp/workbench-form-sizing-recheck-20260926.log`. Add regression coverage before fixing these findings, and keep the final verification scoped to affected browser cases plus the planned module gates.
- Root-cause clarification: Add's Text radio label correctly selects its clipped native input; the original `.check()` failures were test-harness misuse. The actual upload failure is separate: the textarea `onchange` handler re-runs the source switcher, which resets a user-selected TriG format to Turtle on blur. The existing end-to-end graph-context test now asserts format retention after blur, failed before the XSL handler is removed.
  Evidence: red output `/private/tmp/workbench-add-format-reset-red2-20260926.log`; server trace in `/private/tmp/rdf4j-workbench-acceptance-20260926-01/Server/logs/main.log` reports TurtleParser “Expected an RDF value here, found '{' [line 2]”.
- Observation: the typed policy core passes its first six focused cases, but independent review identified incomplete startup validation and menu-group/page/alias contracts that still need red-first coverage before the configuration policy is complete.
  Evidence: `WorkbenchPolicyTest` green in `logs/mvnf/20260926-094650-verify.log`; route and namespace metadata red evidence in `initial-evidence.txt`; review contract is recorded in the task handoff.
- Observation: the focused route/landing/policy suite first exposed four failures, then passed 60 tests after landing selection, alias/mount handling, and disabled-landing copy were corrected. The final Boot integration also verifies the actual non-root mapped root, hidden Namespaces metadata path, and denial before conditional responses.
  Evidence: red log `logs/mvnf/20260926-125451-verify.log`; focused final Boot green log `/private/tmp/workbench-boot-policy-final3-escalated-20260926.log`; full module logs `/private/tmp/server-boot-module-final-escalated-20260926.log`; summaries are appended to `initial-evidence.txt`.
- Observation: the packaged browser rendered 16 distinct routes in desktop light, desktop dark, and mobile dark, and exposed missing dark-result cell styling, dark form-label fills, a transparent dark-mode brand plate, a wrapped mobile header, and uncollapsed navigation groups with a duplicate Server label.
  Evidence: red in-repository Playwright cohort `/private/tmp/workbench-visual-red-cohort-20260926.log`; captured routes/screenshots `/private/tmp/workbench-packaged-acceptance-20260926/` and JSON `/private/tmp/workbench-packaged-acceptance-20260926/routes-and-fixture.json`.
- Observation: the design directory now has 32 source-mapped boards with both light and dark targets and an exact-token CSS preview; the prior exploration images remain in the prompt history but are labeled superseded/rejected.
  Evidence: `design/workbench-design-coverage-20260925/image-prompts.json` records 64 asset hashes and prompts; `DESIGN-COVERAGE.md` lists the source-faithful contents and variant defaults.
- Observation: no early page theme state or user override exists yet. The browser contract now asserts system-following without stored choice, saved Light persistence, System reacting to OS changes, and Dark overriding an OS change.
  Evidence: red browser report `/private/tmp/rdf4j-workbench-theme-red-20260926.log`, appended to `initial-evidence.txt`.
- Observation: the Add screen renders its context checkbox checked by default even though `AddServlet` does not inject a context if the effective `context` request value is absent. The backend's existing no-override path already allows TriG named contexts and contextless triples to be stored according to the repository API.
  Evidence: red report `tools/workbench/target/surefire-reports/org.eclipse.rdf4j.workbench.commands.AddServletTest.txt` (`AddServletTest.addContextOverrideIsOptIn`, 1 failure because the checkbox output contains `checked="true"`); existing parser/context characterization in `AddServletCoverageTest` passes 48 tests, including added TriG default/named graph and explicit override checks. Compact red and green-baseline evidence was appended to root `initial-evidence.txt`.
- Observation: YASQE injects its editor-fullscreen control after the XSL template renders, so a hidden policy flag must also hide that runtime control and override its F11 shortcut in both compare panes.
  Evidence: the focused browser-harness regression failed before the runtime guard and passed afterward; `/private/tmp/workbench-editor-fullscreen-red.log` and `/private/tmp/workbench-query-client-controls-green.log`.
- Observation: Repository export supports the independent N-Quads default and downloads None/Gzip/ZIP without changing query-result negotiation.
  Evidence: `ExportServletCoverageTest` (5 tests), `InfoServletTest` (5 tests), `QueryResultTemplateTest` (19 tests), and `QueryEvaluatorTest` (10 tests) passed in retained logs under `logs/mvnf/`; red and green snippets are preserved in `initial-evidence.txt`.
- Observation: The first comparison-close markup inserted a separate header above only the secondary editor, misaligning editor top edges and leaving its mobile button below the touch-size token.
  Evidence: Parent browser review identified the asymmetric row before final acceptance; the existing comparison-close browser test will measure editor top-edge alignment and 44px mobile target containment before the shared header fix.

## Decision Log

- Decision: Keep upload context replacement opt-in; absent an override, preserve parsed contexts and let contextless statements use the default graph.
  Rationale: This is RDF4J repository API behavior and avoids silently flattening TriG/N-Quads data. Base URI continues to affect parser resolution only.
  Date/Author: 2026-09-26 / Codex.
- Decision: Decouple page size from result-download size. Default page size remains 100; default download limit is zero (all rows), starting at the beginning; explicit query-authored LIMIT/OFFSET and explicit download limits remain effective.
  Rationale: Pagination is a view constraint, while downloading is a separate user request. Reusing the page limit truncates downloads unexpectedly.
  Date/Author: 2026-09-26 / Codex.
- Decision: Use an export-specific N-Quads default and `None`, `Gzip`, or `ZIP` downloadable compression choices, with `None` as default.
  Rationale: Exporting a repository has different format defaults from query-result serialization. Export compression must produce a file that the browser saves and standard tools can unpack.
  Date/Author: 2026-09-26 / Codex.
- Decision: Close comparison through the existing compare-mode lifecycle, resetting only compare state and returning focus to its trigger.
  Rationale: This path already cancels pending explanations and clears stale diff/secondary state while preserving the primary query and results.
  Date/Author: 2026-09-26 / Codex.
- Decision: Store flat, stable-key `workbench.properties` values in three override layers: packaged defaults, a downstream classpath override, then an external Workbench configuration file under the AppConfiguration data directory's `conf` directory. Validate once at startup and fail startup on invalid policy.
  Rationale: Properties are already used by RDF4J configuration loaders, need no dependency, work in WAR and Spring Boot packaging, and can be overridden by embedded consumers without editing RDF4J resources.
  Date/Author: 2026-09-26 / Codex.
- Decision: Treat hidden pages and disabled query capabilities as server authorization policy. Apply it before proxy cache/304 processing, redirects, and dispatch; keep UI visibility as a second, usability layer. Return 404 for hidden pages and 403 for disabled operations.
  Rationale: Hidden controls are not security boundaries; route aliases, HTTP methods, cached responses, cookies, and redirects must obey the same policy.
  Date/Author: 2026-09-26 / Codex.
- Decision: Keep a distinct internal namespace metadata contract for Query/Update and continue blocking the `/namespaces` page when configured hidden.
  Rationale: Internal metadata consumption is not authorization to expose the interactive page.
  Date/Author: 2026-09-26 / Codex.
- Decision: Theme selection follows system preference by default, supports remembered Light/Dark/System override, and uses deployment defaults when no user choice is saved. Existing query timeout default of 60 seconds and other effective values remain source-faithful.
  Rationale: The user requested system-following behavior with a remembered override and deployment policy; theme must be selected before first paint to avoid flashing.
  Date/Author: 2026-09-26 / Codex.
- Decision: Use the existing complementary hue and measured light tokens in `design/workbench-design-coverage-20260925/palette.json`; derive and document dark tokens reproducibly. Never recolor, invert, approximate, or replace the original logo asset.
  Rationale: The original logo is the only orange element and remains byte-for-byte intact; the exact palette and contrast calculations are more authoritative than rasterized mockup pixels.
  Date/Author: 2026-09-26 / Codex.
- Decision: Keep both original brand images unchanged on one neutral light plate in dark mode, keep form labels transparent, apply dark tokens to embedded result tables, use a one-row mobile brand/theme header, and render menu groups as native disclosures with only the active multi-item group open.
  Rationale: Packaged screenshots showed unreadable black branding, striped form labels, white result cells, wasted mobile header height, and an always-expanded menu; the fixes preserve source assets, labels, and routes while correcting those rendered causes.
  Date/Author: 2026-09-26 / Codex.
- Decision: Break this cross-module task into independent behavior cohorts and serialize Maven builds/tests. Add and run a smallest failing in-repository test before each behavioral production change.
  Rationale: Upload, query, export, route policy, UI, and packaging have independent observable contracts; phased red/green evidence localizes regressions and respects the repository's test workflow.
  Date/Author: 2026-09-26 / Codex.

## Outcomes & Retrospective

This section records the implemented behavior and the final verification scope. Prior geometry and timeout work remains part of the completed UI rather than a substitute for backend, policy, theme, and design completion.

Upload-context status: the Add form leaves the context override unchecked, keeps Base URI separate from repository graph assignment, and enables override only when selected. Focused `AddServletTest` (8 tests) and `AddServletCoverageTest` (48 tests) pass; the packaged Chromium/WebKit form suite also verifies default TriG graph preservation and explicit replacement. Evidence is in `initial-evidence.txt`, retained Maven logs, and `/private/tmp/workbench-final-playwright-green-20260926.log`.

Query-download status: two new focused tests fail before production edits. `QueryEvaluatorTest#shouldDownloadAllResultsFromTheBeginningWhenNoDownloadLimitIsSelected` shows that Accept downloads inherit the page limit and page offset when `download_limit` is absent; `QueryResultTemplateTest#queryResultDownloadsDefaultToAllIndependentlyOfPageSize` shows the download selector currently inherits the 100-row page default. Both report snippets are preserved in `initial-evidence.txt`, with full logs under `logs/mvnf/20260926-070950-verify.log` and `logs/mvnf/20260926-071037-verify.log`.

Query-download implementation status: QueryEvaluator treats an Accept download with no explicit download_limit as unlimited and resets UI paging offset to zero; any authored SPARQL LIMIT/OFFSET remains in the query text. The result XSL gives only the query-download limit selectors an All/zero default, while page size and other list limits retain the server's default of 100. Focused red/green logs are under `logs/mvnf/20260926-070950-verify.log`, `...071037...`, `...071549...`, and `...071629...`; the compact evidence and a report-path correction are appended to `initial-evidence.txt`.

Export status: the repository-export control now defaults to N-Quads independently of the query-result `Accept` default. Export supports plain RDF, a downloadable Gzip file, or a ZIP containing one named RDF file; repository serialization and named graph contexts are retained. `ExportServletCoverageTest` 5, `InfoServletTest` 5, `QueryResultTemplateTest` 19, and `QueryEvaluatorTest` 10 passed in retained logs. Red and green evidence is in `initial-evidence.txt` and `logs/mvnf/`.

Compare-close status: the rendered-template and browser regressions verify the compact accessible Close action, equal editor columns/header alignment, cancellation of pending explanations, rejection of stale responses, focus restoration, clean reopen, and diff dismissal while retaining the primary result. The behavior passed in Chromium and WebKit against the final packaged fixture. Logs include `/private/tmp/rdf4j-compare-close-red-20260926-v4.log`, `/private/tmp/rdf4j-compare-close-green-20260926-v2.log`, and `/private/tmp/workbench-compare-close-final3-20260926.log`.

Configuration edge-case status: hidden Namespaces remains denied for all verbs and conditional requests while Query/Update obtain prefixes through the internal namespace metadata endpoint. Route policy executes before the proxy 304 shortcut, and empty/whitespace Explain format values map to the evaluator-effective Text format before authorization. A non-root Spring Boot context-path regression exposed the Boot remote-server URL escaping its mount; Boot now mounts that URL under the configured context. The final Boot focused class passes 6 tests; Workbench passes 541 tests; Spring Boot passes 59 tests; Server Spring passes 166 tests with 1 skipped. Logs and snippets are recorded in `initial-evidence.txt` and `/private/tmp/workbench-boot-context-path-red-20260926.log`, `/private/tmp/workbench-boot-policy-final3-escalated-20260926.log`, `/private/tmp/workbench-module-final-after-gaps-20260926.log`, `/private/tmp/server-boot-module-final-escalated-20260926.log`, and `/private/tmp/server-spring-module-final-20260926.log`.

Final browser status: the packaged Workbench layout/theme suite passed 34 tests in Chromium and WebKit at desktop, tablet, and narrow widths; the comparison-close/pending-work scenario passed 2/2 across both engines. The 320px table `scrollWidth` reading can exceed its client width by 3px from empty table border spacing, but visible controls and labels remain contained and document overflow is zero. The theme test now clears localStorage only on its first navigation so the explicit preference can be meaningfully tested across reloads. Evidence: `/private/tmp/workbench-final-playwright-green-20260926.log`, `/private/tmp/workbench-compare-close-final3-20260926.log`, and the frontend unit summary (95 passed, 0 failed).

Final gates and artifact audit: `scripts/checkCopyrightPresent.sh`, offline Maven `process-resources`, TypeScript compilation, `git diff --check`, and the 95-test frontend unit suite passed. The Server Spring module's sole skip is `QueryResponseHeartbeatHandlerTest.realSocketDisconnectCancelsANormalRequestWithoutASuppliedRequestId`; its report records an assumption abort because loopback HTTP binding returned `Operation not permitted` in the sandbox, not a test assertion failure. The complete untracked-file inventory was saved to `/private/tmp/workbench-final-untracked-audit-20260926.txt` (3,369 entries); all generated screenshots, design boards, diagnostics, and previous artifacts were preserved.

## Context and Orientation

The repository is `.`. Workbench server code and tests are in `tools/workbench/src/main/java` and `tools/workbench/src/test/java`; its browser-facing XSL templates, TypeScript, JavaScript, and CSS are under `tools/workbench/src/main/webapp`. The e2e Playwright project is under `e2e/`. Spring Boot packaging is in `tools/server-boot`. The existing generated design directory is `design/workbench-design-coverage-20260925`.

`WorkbenchRequest` wraps an HTTP request and resolves parameters using Workbench-specific precedence; server policy must check the resolved value. `ProxyRepositoryServlet` is the shared proxy entry that handles conditional requests and routes. `WorkbenchGateway` prepares servlet instances and is the shared integration point for WAR and Spring Boot. `AppConfiguration` loads application properties and exposes the chosen data directory. A “page policy” means a startup-loaded rule deciding whether a visible route can be requested. A “query feature policy” means a rule deciding whether one query action or option is available and accepted by the server. The policy is separate from the menu rendering.

The source files that require first inspection include `commands/AddServlet.java`, `commands/QueryServlet.java`, `commands/ExportServlet.java`, `util/QueryEvaluator.java`, `util/WorkbenchRequest.java`, `proxy/WorkbenchGateway.java`, `proxy/ProxyRepositoryServlet.java`, `transformations/template.xsl`, `transformations/query.xsl`, `transformations/add.xsl`, `transformations/export.xsl`, `styles/workbench-refresh.css`, `styles/query.css`, `scripts/ts/query.ts`, the generated `scripts/js/query.js`, `WEB-INF/web.xml`, and `tools/server-boot/.../Rdf4jServerWorkbenchApplication.java`.

The starting checkout has pre-existing tracked edits in the query alignment tests/styles/templates, Add/Clear/Remove/Server templates, servlet `web.xml`, and `QueryResultTemplateTest`; it also has extensive generated screenshots and design artifacts. Do not reset, clean, overwrite, rename, or delete them. In particular, preserve the explicit query-timeout default of 60 seconds. `initial-evidence.txt` and `maven-build.log` already contain prior evidence; append only. Use `.m2_repo` and never pass `-am` or `-q` to enabled tests. Use a unique test-owned app-data directory and port; never touch an unrelated port-8080 process.

## Plan of Work

Refresh the required root quick build and record the exact worktree state. Keep the initial evidence file and Maven install log append-only. Then implement upload contexts, query-download defaults, repository export compression, comparison close behavior, and Workbench configuration as separate red-first cohorts. For each cohort, inspect current code and tests, add the smallest test that expresses the missing behavior, run only that selection, preserve its report, then implement the root cause and rerun the same selection. After focused green, run the affected module or packaging checks.

Design and UI work follows source semantics rather than inventing controls. Complete light and dark image-generation targets and catalog; then update the actual XSL/TypeScript/CSS surfaces, retaining all functions and clear defaults. Add tests for theme selection, accessible disclosures, route/menu policy, responsive sizing, and compare interactions before modifying the relevant production behavior. Keep generated TypeScript outputs synchronized with the repository compiler and keep the exact logo PNG untouched.

The configuration should be a flat properties API with stable IDs. It must support menu item visibility, label, order, group, icon, project link; page hide by canonical route; individual query capabilities; default theme; and theme mode policy. The loader precedence is packaged `org/eclipse/rdf4j/common/app/config/defaults/workbench.properties`, optional downstream classpath `org/eclipse/rdf4j/common/app/config/workbench.properties`, then external `<AppConfiguration data dir>/conf/workbench.properties`. Keys and value formats must be documented with a complete default file and consumer example. Avoid embedded JSON or a new library. Apply one immutable validated policy to WAR and Boot. Page route checks must precede proxy 304 handling, redirects, and command dispatch. Hidden pages deny all verbs and aliases. Disabled actions/options must be denied at every server entry, including direct download, saved-query references, duplicate parameters, cookies, and legacy forms. `/info` remains available; namespace prefix lookup gets a narrow internal path that does not reveal `/namespaces`.

After all behavior cohorts are green, run full Workbench and affected server-boot module tests with the `mvnf` workflow, JavaScript unit/build checks, packaging verification, and actual browser checks in Chromium and WebKit. Exercise default and expanded disclosures, light/dark/system mode, desktop/tablet/320px/390px, page policy before 304, representative Workbench features, uploaded graph contexts, all-results download, and decompressed/parsed N-Quads exports. Refresh the plan with exact evidence, remaining gaps, and final coverage counts.

## Concrete Steps

At the repository root, run the required offline quick clean install using `.m2_repo`; append terminal output to `maven-build.log` rather than truncating it. For tests use `python3 .codex/skills/mvnf/scripts/mvnf.py <module-or-selector> --retain-logs`, without `-am` or `-q`. Save red and green report snippets by appending to the existing `initial-evidence.txt`; never replace it. Ensure each test command and report path is written here after execution.

For UI checks, Browser is absent in this session, so use the existing Playwright installation and recorded fallback. Reuse the fixture harness with fresh app-data and loopback port 8082 only when that port is unowned; do not interrupt another process or use 8080. Capture WebKit and Chromium screenshots outside tracked source unless they belong to the already established design artifact directory. Imagegen assets go under the existing design workspace with unique stable board names; do not overwrite preserved explorations or images. Update its image manifests and gallery only after validating each generated board against source-controlled field/default/route semantics.

Before formatting Java/XML resources, run `cd scripts && ./checkCopyrightPresent.sh`. Keep generated TypeScript and JavaScript aligned with `tools/workbench/compileTypescript.sh`. Required final checks are the focused and full affected Maven tests, applicable server-boot test, Workbench frontend unit/build tests, browser validation, `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`, copyright/SPDX validation, and `git diff --check`. Test invocation flags `-q` and `-am` are prohibited whenever tests are enabled; the formatter command above is resource processing, not a test run.

## Validation and Acceptance

Upload acceptance: a file with embedded graphs preserves all supplied graph names by default; contextless triples are in the default graph; explicit override replaces parsed contexts; Base URI affects parsing without silently becoming a context. Tests cover at least Turtle, TriG, and N-Quads where the format supports them.

Query result acceptance: default page view remains 100; default download selection is All (zero cap), independent of page size, and starts at the beginning. An authored SPARQL LIMIT/OFFSET remains in the executed query and is never stripped; an explicit positive download cap continues to apply.

Export acceptance: default format is N-Quads only for repository export; available compression is None/Gzip/ZIP and defaults to None. The browser receives a named downloadable file with the correct media type and extension. Gzip output can be decompressed, ZIP contains one parseable RDF member, and both retain named graphs. All export routes continue to serialize the full repository.

Compare acceptance: Close is visible, labeled, keyboard-accessible, and restores focus to its trigger. Closing aborts secondary work and closes comparison diff state while retaining the left query and results. Closing during a pending response and reopening cannot show stale content.

Policy acceptance: shipped defaults, classpath override, external override, WAR, and Boot all load the same keys; bad values fail startup. Hiding a menu item/group or page denies its canonical route and aliases for every HTTP method, including conditional requests that otherwise would yield 304. A visible action cannot bypass a disabled feature through GET execution, POST variants, alternate result downloads, query cookies, duplicate values, saved-query references, or legacy parameter form. Hidden `/namespaces` stays denied while Query/Update continue to obtain necessary prefix metadata from the separate internal contract. Menu ordering/grouping/labels/icons/project links are rendered from the validated flat properties. The system theme follows OS preferences by default; user override persists; returning to System follows later OS changes. Controls stay functional at desktop 1440px, tablet 768px, and mobile 390px/320px in Chromium and WebKit.

Visual acceptance: all 32 source-mapped design boards are present for both light and dark themes or explicitly justified if one target is a shared component board. Board content shows source-true route names, functional controls, and defaults. The actual page preserves the exact `tools/workbench/src/main/webapp/images/logo.png` bytes; no other orange surface exists. Every function in the prior navigation/actions inventory remains present and configurable. Query Run and Explain stay below the editor; complex options are named and closed by default, expose summaries of non-default values, and remain accessible with keyboard focus. Disabled options cannot be activated directly at the server.

## Idempotence and Recovery

The test runner removes stale target output before its selection, so save compact evidence before rerunning a failing selection. Do not allow overlapping Maven commands or process-resources formatters. Keep full retained logs under `logs/mvnf/` and reports under module target directories. If loopback socket binding fails under the sandbox, retain the first report and rerun the same authorized test with loopback access; classify the first error as environment-only only if the rerun passes. If a browser fixture cannot start, inspect the owned session/port and use a fresh test-owned port; never kill an unowned server. Preserve all prior generated images and untracked artifacts; create new uniquely named files rather than replacing them. No commit, branch change, push, or cleanup is part of this task.

## Artifacts and Notes

The previous geometry and timeout evidence lives in `initial-evidence.txt`, `maven-build.log`, `logs/mvnf/`, `.agent/execplans/workbench-form-options-sizing.md`, and `design/workbench-design-coverage-20260925/`. Add the exact current test command, report snippet, screenshot paths, board-manifest state, and any blockers below as work progresses. The new design directory should retain the exact logo reference and calculated light/dark palette; generated images are visual targets and cannot replace exact source assets.

## Interfaces and Dependencies

Use the existing RDF4J `AppConfiguration` and `ConfigurationUtil` resource loading conventions, Java `Properties` for the flat override file, existing servlet lifecycle and gateway extension points, Java standard `GZIPOutputStream`/`ZipOutputStream` for export payloads unless existing abstractions prove more suitable, current Workbench XSL/TypeScript/CSS, existing Playwright, and current RDF parsers/writers. Do not add runtime or test dependencies without first checking whether existing RDF4J/Java infrastructure can express the contract.

## Plan Revision Notes

2026-09-26: Created one consolidated living plan because the user extended the prior visual alignment task into five independently testable data/configuration behaviors, a complete theme redesign, and the remaining design-board set. Kept the previous sizing plan and its evidence unchanged as a historical record. Added parent review's request to cover `WorkbenchRequest` value precedence, QueryServlet's GET/POST/saved-query entry points, proxy 304 order, namespace metadata separation, disabled-feature dependencies, and default usability to the acceptance contract.
2026-09-26: Completed the required offline root quick clean install and started the first upload-context cohort. A failing stylesheet test demonstrates the checked-by-default context override; added in-memory TriG tests to characterize preserved named/default contexts and explicit replacement. The backend already honors the repository contract; only the form's default/JavaScript handling should change unless a new failing backend case proves otherwise.
