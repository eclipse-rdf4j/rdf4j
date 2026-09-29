# Refine the Workbench Visual System Across Pages

This ExecPlan is a living document and follows `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current. There must be exactly one `[in_progress]` item in `Progress` at each stopping point.

## Purpose / Big Picture

People use Workbench to browse repositories, write and compare queries, inspect explanations, configure stores, upload data, and export whole repositories. This work will make those existing workflows easier to scan and operate while preserving the current RDF4J Workbench visual identity and all behavior. A person can verify the result by opening the normal Workbench routes at desktop and narrow widths, using their forms and actions, and seeing aligned controls, readable tables and panels, and no clipped or overflowing primary content.

## Progress

- [x] (2026-09-27 17:50Z) Capture fresh route, variant, theme, and viewport baselines: 17 built-in routes, all 18 creation variants, and 357 rendered states.
- [x] (2026-09-27 19:55Z) Add failing browser checks for observed shared-control and page-family defects.
- [x] (2026-09-27 20:36Z) Refine shared spacing, action, field, and explanation-toolbar rules.
- [x] (2026-09-27 20:36Z) Refine empty table states and route-family layouts.
- [x] (2026-09-27 20:36Z) Capture and inspect corrected visual states.
- [x] (2026-09-28 01:41Z) Fix shared select, form, result, compare, and disclosure motion.
- [x] (2026-09-28 01:42Z) Recheck visual states and full Workbench verification.
- [x] (2026-09-28 01:53Z) Format, check copyright, and audit the diff.
- [x] (2026-09-28 00:23Z) Verify zoom, route review, and browser smoke.
- [x] Reproduce dark compare navigation overlay.
- [x] Fix dark compare navigation overlay.
- [x] Verify result-frame pagination reachability.
- [x] Review dark panes and operation coverage.
- [x] Refresh ledger and complete parent review.
- [x] Capture full zoom viewport screenshots.
- [x] Finalize evidence and clean task runtime.
- [x] Deliver final evidence handoff.

## Surprises & Discoveries

- Observation: The Workbench uses server-rendered XSLT templates and shared CSS rather than a client-side component framework. Route coverage therefore needs live Workbench pages and browser checks against their rendered DOM.
  Evidence: `tools/workbench/src/main/webapp/transformations/template.xsl`, `tools/workbench/src/main/webapp/styles/workbench-refresh.css`, and `e2e/playwright.config.js`.
- Observation: Several historical screenshot directories and server processes already exist. Their images are not evidence for this task, and their processes must remain untouched.
  Evidence: existing untracked design and E2E artifacts; listeners reported on 8082, 8083, and 8086 during initial inspection.
- Observation: The new browser contracts reproduced the observed crowding and a few style leaks. The Namespaces danger action itself correctly uses the shared color and surface tokens in light mode, so its apparently bright appearance is not a separate override to patch.
  Evidence: `e2e/visual-refinement-regressions-red-rerun2.log`; in-repo assertions report `rgb(180, 35, 24)` for wrapper and button text and `rgb(255, 255, 255)` surface.
- Observation: The initial geometry pass found no route/form surface overflow, but visual inspection found localized density and alignment issues that overflow assertions alone cannot detect.
  Evidence: Root review found the export preview button touching its empty-state panel, the Information metadata value column lacking explicit value padding, and a cramped Add format-select chevron; fresh screenshots are in the baseline directory.
- Observation: Moving empty messages into a table body row does not alter pagination or selection behavior when confined to pages without row-count consumers.
  Evidence: `explore.js` counts `#explore-results table.data tbody tr` for its pagination state, while namespace selection uses only `#prefix-select` options and export handles preview limits independently. The new contract keeps Explore unchanged and confirms empty tuple/graph results still have zero rows and no preview-only status.
- Observation: The narrow query disclosure visual issue comes from the disclosure wrappers being flattened with `display: contents`; their panels then participate in the toolbar grid after both mobile triggers, and the shared anchor calculation right-aligns a shorter Options panel. The Save pane also places its submit button before its fields. The Create route builds its Advanced summary in `create.ts` with text only, unlike the XSLT-native Add and Export disclosures.
  Evidence: `e2e/visual-refinement-disclosure-red2.log` reports a 58px Save-panel/trigger gap at 320px, Options before the Save panel, a 111px Options-panel/trigger left offset at 390px, a 221px panel inside a 332px toolbar, and no Advanced chevron at either viewport tested.
- Observation: The latest shared-surface review identifies four remaining regressions: query and embedded native selects have no common chevron wrapper; expanded result disclosures meet the first result row; Create Advanced content begins flush against its summary and radio options are unlabeled; and compare mode hides navigation content without releasing the desktop shell column.
  Evidence: `initial-evidence.txt`, `e2e/visual-refinement-shared-followups-red.log`, and `e2e/visual-refinement-compare-nav-red2.log` show 10px select padding without a chevron, a 0px disclosure gap, a 0px summary/content gap and radius with no radio labels, and hidden-nav compare content at x=232.
- Observation: Disclosure close keyframes set `height: 0` but leave computed block padding and borders in the animated box until `hidden` is applied. Query Options also retains its grid row gap until that moment, and the result layout keeps its conditional 8px margin; native details receives an outer border-box measurement as a content-box CSS height, leaving a 2px final step.
  Evidence: `e2e/workbench-disclosure-collapse-geometry-red3.log` records 24.01px residual panel height and a 32.38px toolbar/downstream shift at 390px; result layout shifts 34px; Add/Create end 2px below their natural closed height. Parent independently sampled Create at animation frame 176.6ms versus settled 184.1ms and measured the same 2.04px step.
- Observation: A shared select wrapper can remain visible after its feature-disabled native select is hidden, and a compare action wrapper can collapse to a visible 2px box when only its child button is hidden. The repository already uses relational selectors for shared component state, so the fix belongs in the wrapper visibility rules.
  Evidence: the focused pre-fix browser run recorded `display:inline-flex` plus a visible select chevron and a `display:flex` compare action wrapper; Chromium, Firefox, and WebKit now report both wrappers hidden.
- Observation: In dark compare mode, the navigation disclosure remains in the DOM while hidden. The global dark surface rule gave it an opaque background and let it intercept pointer events over the left query editor even though the links were visually hidden.
  Evidence: `e2e/workbench-visual-refinement-dark-compare-nav-red2.log` reports the disclosure itself as the hit target; the post-fix check reports a transparent background, `pointer-events:none`, and `hitNavigation:false` at 1440px and 390px. The refreshed screenshots are in `final/dark-expanded/` and the independent reviewer accepted the fix.
- Observation: Result pagination is not missing on mobile; the embedded result frame is a 560px-high independent scroll region, and its Next control sits near the end of a 3,918px document.
  Evidence: `e2e/workbench-visual-refinement-result-frame-pagination-final.log` measures the button at y3875 before scrolling and y517 after scrolling within the frame, fully inside its 560px viewport.
- Observation: Playwright's CSS-scale screenshot at 200% page zoom captured only a 720px raster even though the physical browser viewport was 1440px wide. Chromium's page-level DevTools capture preserved the full native viewport at 1440x861 while the page reported 720x430 CSS pixels and DPR 2.
  Evidence: `e2e/workbench-zoom-native-viewport-final.log` records all 11 captures; direct image review confirms both page edges, query editors/explanations, result options, Add Advanced, and Export controls are present.

## Decision Log

- Decision: Preserve current visual identity and use the accepted compact-but-breathable rhythm: desktop controls 36px, touch controls 44px, shared SVG icons 16px, and spacing steps of 4px, 8px, 12px, 16px, and 24px for field labels, adjacent controls, form rows, logical groups, and major sections.
  Rationale: These values were accepted for the requested visual refinement and match existing shared Workbench tokens.
  Date/Author: 2026-09-27, Codex.
- Decision: Use repository Playwright with one isolated Workbench/Server instance because the Playwright configuration has no server autostart and the Browser plugin is unavailable in this environment.
  Rationale: This exercises the actual server-rendered templates without touching user repositories or relying on stale screenshots.
  Date/Author: 2026-09-27, Codex.
- Decision: Treat existing route and theme suites as coverage inputs, then add focused geometry and interaction assertions for actual defects before changing production styles.
  Rationale: A broad visual pass should strengthen reusable contracts while avoiding brittle screenshot-only or one-page fixes.
  Date/Author: 2026-09-27, Codex.
- Decision: Keep visual dispositions beside the automated route/overflow ledger; pages can fit the viewport and still have cramped controls or awkward gutters.
  Rationale: Parent review of fresh screenshots found concrete defects that the initial document-width assertions did not detect.
  Date/Author: 2026-09-27, Codex.
- Decision: Review dark expanded panels and operation outcomes as supplemental captures outside the default-route image count; destructive operations are exercised only against task-created disposable repositories.
  Rationale: A complete visual handoff needs distinct evidence for interactive states without changing or deleting user repository data.
  Date/Author: 2026-09-28, Codex.

## Outcomes & Retrospective

The 357-image Chromium inventory spans all 17 built-in routes and 18 creation variants; direct review records every route and variant at the light/dark 1440/390 quartet. Supplemental direct review covers 51 dark expanded captures and 19 operation-state captures. Firefox and WebKit each passed a 35-route/variant smoke, and an actual 200% page-zoom pass produced 11 full native-viewport images for the query editor, results/options, both compare panes and explanations, Add Advanced, and Export. Each image is 1440x861 device pixels for a 1440px physical window; Chromium reports a 720x430 CSS viewport and DPR 2. The isolated runtime exposes only five built-in navigation entries and no consumer-defined custom pages; `federate`, `remote`, and `sparql` do not have configurable Advanced panels. Responsive route capture is Chromium-only. Parent’s independent dark Compare interaction review passed at 1440px and 390px, including menu click-through and navigation. Full Workbench verification, focused browser checks, and code hygiene are recorded in the adjacent ledgers and retained E2E logs.

## Context and Orientation

The Workbench server renders page templates from `tools/workbench/src/main/webapp/transformations/`, with the shared shell in `transformations/template.xsl` and shared styles in `styles/workbench-refresh.css`. Query and embedded-result surfaces add `styles/query.css`, `styles/query-compare.css`, and `styles/query-explanation.css`. Existing browser checks are under `e2e/tests/` and use `e2e/playwright.config.js`; they expect an already-running server and use `RDF4J_WORKBENCH_BASE_URL` and `RDF4J_SERVER_BASE_URL` to select it.

The page inventory is derived from the 17 built-in routes in `WorkbenchPolicy`: `server`; `repositories`, `create`, `delete`; `summary`, `namespaces`, `contexts`, `types`, `explore`, `query`, `saved-queries`, `export`; `update`, `add`, `remove`, `clear`; and `information`. The `create` route has 18 supported repository configurations. Query result templates add tuple, boolean, and graph result views; explanation can show text, DOT, JSON, configuration, and compare states. Configured custom routes, if present, must be inventoried from the running configuration rather than assumed absent.

The artifact directory for new screenshots and the coverage ledger is `/Users/havardottestad/.codex/visualizations/2026/09/27/01a0e2fb-705d-7ee2-9a69-d25d3da4d594/workbench-refinement/`. Existing untracked files and running processes are outside this task and must be preserved.

## Plan of Work

First start one isolated server with a separate data directory and a port not already in use. Use Playwright to capture the shell and every listed route at 1440px and 390px in light and dark themes; inspect all routes additionally at 320px, 768px, 1024px, 1920px, and the relevant CSS breakpoint neighbors. Capture each repository creation variant, advanced form, and meaningful result or operation state using disposable fixtures. Record route, state, theme, viewport, screenshot path, console outcome, and horizontal-overflow/control-bound findings in a Markdown ledger. Do not call old images current evidence.

Next, use fresh screenshots and measured DOM geometry to identify concrete defects. Add the smallest Playwright regression for each shared defect family, run it and save its failure summary in the repository’s `initial-evidence.txt`, then change the shared rule or component that causes the issue. Begin with the agreed query toolbar spacing and shared actions/fields, then work through navigation and forms, repository pages, browse tables, query/results/explanations, modifications, and export. Keep labels, keyboard operation, focus indicators, touch targets, state transitions, and the narrow stacked layouts intact.

After each family, rerun its focused browser checks and capture fresh desktop and narrow screenshots. Finish by executing the full Workbench browser/unit checks and module test suite, compiling generated TypeScript assets only if TypeScript changes, running copyright and formatter checks, and reviewing `git diff --check` plus the final scope. Do not commit or push without a later explicit request.

## Concrete Steps

From the repository root, use the single isolated server and Playwright setup recorded in the evidence ledger. Focused browser checks use the repository CLI, for example:

    RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:<port>/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:<port>/rdf4j-server e2e/node_modules/.bin/playwright test --config=e2e/playwright.config.js --project=chromium e2e/tests/<focused-spec>.spec.js --reporter=line

The Workbench Maven test runner is:

    python3 .codex/skills/mvnf/scripts/mvnf.py workbench --retain-logs

Do not pass `-am` or `-q` to tests. `mvnf` installs current modules into `.m2_repo` before its verification run. Run formatter and copyright checks only after updating production and test files; do not overwrite or remove unrelated evidence.

## Validation and Acceptance

The route ledger must account for each built-in route, all 18 create variants, any configured custom routes discovered, query result/explanation modes, meaningful operation states, light/dark/system themes, and the required viewport matrix. Missing coverage must name the exact route/state and reason; it must not be counted as inspected.

Browser checks must exercise visible primary controls and keyboard focus, assert no document-level horizontal overflow at narrow widths, assert primary actions and form controls remain within their containing region, and preserve existing navigation, save, query, compare, upload, export, and modification behavior. For each corrected behavior, a focused check must fail before the production change and pass afterward. Fresh screenshots must be opened with `view_image` and referenced in the Markdown ledger.

The final code must pass focused browser regressions, relevant existing E2E/unit suites, and the Workbench Maven module checks; no newly introduced test failures may be hidden. TypeScript-generated assets must be refreshed if TypeScript source changes. Copyright/header, formatter, and `git diff --check` results must be recorded. The visual ledger must state concrete inspected points for spacing, typography, colors, icon alignment, control sizing/focus, table or panel layout, responsive behavior, and motion/theme behavior, along with any remaining intentional deviation.

## Idempotence and Recovery

Playwright fixtures must use unique repository identifiers and delete only repositories they created. The isolated server must use a new private data directory and only the task-owned process may be stopped. If a browser test or formatter fails, preserve its report and repair the cause before rerunning; do not delete old logs, screenshots, or untracked artifacts. If a route or state cannot be reached, record its exact URL, setup failure, and attempt rather than weakening the route matrix.

## Artifacts and Notes

Store task-owned fresh images, the route/state matrix, and concise finding/fix notes in the artifact directory above. Store test and build logs using existing repository conventions. Append pre-fix failure snippets to the existing `initial-evidence.txt` file; do not replace prior entries.

## Interfaces and Dependencies

Use the existing Workbench CSS custom properties and XSLT action/control templates wherever possible. Use Playwright’s installed Chromium, Firefox, and WebKit projects for browser verification; do not add dependencies or introduce a replacement UI framework. A route’s rendered DOM and computed layout, not a generated static mock, are the behavior under test.
