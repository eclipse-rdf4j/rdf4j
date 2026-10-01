# Keep Workbench Dark Mode Through First Paint

This ExecPlan is a living document maintained according to `.agent/PLANS.md`. It records the dark-refresh flash reproduction and repair while preserving the unrelated uncommitted Go to row removal.

## Purpose / Big Picture

When a Workbench user has selected dark mode, refreshing a page should paint its first styled canvas in dark colors instead of briefly showing the light canvas while scripts load. The early theme choice must work across shared Workbench routes, preserve explicit preference over the configured default, and leave light mode correct. Acceptance covers the first styled frame, the pre-stylesheet metadata boundary, and the route reaching its settled state after a real reload.

## Progress

- [x] (2026-09-30) Verify current branch and unrelated scope.
- [x] (2026-09-30) Trace served shell and late theme setup.
- [x] (2026-09-30) Add deterministic model-pending reproduction.
- [x] Add CSS-pending theme metadata case.
- [x] Reproduce early-script recovery.
- [x] Implement shared head and retry.
- [x] Verify first-frame and settled states.
- [x] Package preview and verify hashes.

## Surprises & Discoveries

- Initial observation: the shared HTML shell linked its stylesheets before declaring a theme or loading the resolver.
  Evidence: the preserved RED response showed no resolved theme metadata before `workbenchApp.js` loaded; the repaired shell now serves color-scheme/default metadata and the blocking resolver before stylesheets.
- Initial observation: the shell did not emit the configured default theme.
  Evidence: `InfoServlet` publishes `default-workbench-theme`; the repaired shell reads the same validated policy through `WorkbenchPolicyLoader`.
- Observation: the repository-list route can embed its initial page model, so waiting only for a page-model fetch can hang on that route.
  Evidence: the first browser harness run timed out waiting for an NDJSON request on `/repositories`; the query route reached the first-frame assertion.
- Initial observation: the default light canvas appeared before the late resolver ran, even though no root theme attribute had been applied yet.
  Evidence: the query-route run measured the light canvas token and a null `data-theme` before the request gate was released.
- Initial observation: stored dark preference on a light OS produced a light first styled frame on the query route.
  Evidence: Playwright WebKit expected `#080e11` and received `#f5f9fb`; the paired light preference on a dark OS passed on the repository-list route. Full report: `/tmp/rdf4j7-workbench-dark-flash-20260930/first-paint-red-final.log`.
- Observation: WebKit does not paint a viewport while its first render-blocking stylesheet is held.
  Evidence: the stylesheet-pending state has no paint entries and a blank screenshot; `/tmp/rdf4j7-workbench-dark-flash-20260930/css-pending-red.log` records the missing theme metadata before the stylesheet response. The query-route model gate remains the visible first-styled-frame proof.
- Observation: scoped source search found no Workbench Content Security Policy declaration, but browser-delivered response headers still require direct verification before design is finalized.
  Evidence: `rg` over Workbench/server Java, XML, and properties returned no `Content-Security-Policy` or `script-src` declaration.
- Observation: the earliest canvas before the server returns HTML belongs to the browser; with the first stylesheet pending, WebKit has no painted page frame to sample.
  Evidence: the bounded CSS-pending case verifies early theme and color-scheme metadata before releasing the stylesheet, while the model-pending case captures the first visible styled frame.
- Final observation: the same three theme/route combinations pass after priming and reloading, and each document loads the theme resolver once.
  Evidence: `/tmp/rdf4j7-workbench-dark-flash-20260930/webkit-reload-green.log` and the retained JSON/screenshot artifacts under `/tmp/rdf4j7-workbench-dark-flash-20260930/final-webkit/`.

## Decision Log

- Decision: gate the page-model request for query routes to capture a first-frame screenshot; gate the first stylesheet only to verify early theme metadata before CSS parsing resumes.
  Rationale: the query model gate captures the actual first styled frame, while WebKit suppresses painting when its first render-blocking stylesheet is pending.
  Date/Author: 2026-09-30 / Codex.
- Decision: preserve the existing theme resolver and configured-default precedence rather than adding separate theme-selection logic.
  Rationale: the current resolver owns local storage, media preference, defaults, and later control synchronization.
  Date/Author: 2026-09-30 / Codex, approved by Workbench coordinator.
- Decision: use a synchronous same-origin head load before the Workbench stylesheets if the approved design keeps the existing resolver as early owner.
  Rationale: this lets the resolver set `data-theme` before CSS computes the first visible palette without adding inline script or style that could violate CSP.
  Date/Author: 2026-09-30 / Codex, approved by Workbench coordinator.
- Decision: test failed early-theme-script recovery through the shared loader before changing that loader.
  Rationale: the new blocking head script exposes an existing-script error path that could otherwise leave shared runtime initialization pending forever.
  Date/Author: 2026-09-30 / Codex, required by Workbench coordinator review.
- Decision: read the deployment default from the shared validated `WorkbenchPolicyLoader` and expose a same-origin color-scheme meta before CSS.
  Rationale: explicit storage preference must win, while a deployment-configured default must remain authoritative when no explicit choice is stored.
  Date/Author: 2026-09-30 / Codex, approved by Workbench coordinator.
- Decision: WebKit Playwright results will be described as WebKit coverage, not as an actual Safari run.
  Rationale: the available automation uses Playwright's WebKit browser binary.
  Date/Author: 2026-09-30 / Codex.

## Outcomes & Retrospective

The stored-dark/light-OS refresh regression, missing pre-stylesheet metadata, and stale-script retry failure were captured before production edits. The shared shell now emits policy-backed default and color-scheme metadata and loads the existing resolver before stylesheets; the resolver updates the effective scheme and marks successful completion. The root canvas has explicit opaque light/dark ownership, and the shared loader retries an unmarked stale script through a managed element. Focused Java and Node tests pass, as do all three bounded Playwright WebKit reload cases. Served theme/app scripts and the refresh stylesheet match the built Workbench WAR. Actual Apple Safari was not available for this run. The earlier uncommitted Go to row removal and all unrelated artifacts remain preserved.

## Context and Orientation

The shared HTML shell is emitted by `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/base/WorkbenchHtmlShell.java`. Its stylesheet tokens live in `tools/workbench/src/main/webapp/styles/workbench-refresh.css`; the preference resolver is `tools/workbench/src/main/webapp/scripts/workbench-theme.js`; and `tools/workbench/src/main/webapp/scripts/ts/workbenchApp.ts` reuses the successful early resolver when loading the rest of the shared runtime. The shell reads the deployment default through `WorkbenchPolicyLoader`; the page model continues to synchronize that default with theme controls. Existing settled-state coverage lives in `e2e/tests/workbench-theme.spec.js` and `e2e/tests-unit/workbench-theme.test.js`.

The first-paint test must set a stored dark preference while emulating a light operating-system preference, hold the query model response without sleeping, and observe the page before releasing it. It must also cover an explicit light choice with a dark operating-system preference and a separate shared-shell route. A Playwright WebKit pass is not proof from Apple's Safari application.

## Plan of Work

The model-pending, CSS-pending, and failed-loader REDs were captured before production edits. The query case holds the page-model response while sampling the styled canvas; the CSS case holds the first stylesheet and checks theme/scheme metadata. WebKit emits no painted frame during that CSS gate, so its blank screenshot is not palette evidence. A unit fixture exercises an unmarked theme script whose error event has already passed. The query case uses a test-owned temporary repository. All RED reports and diagnostics remain under `/tmp/rdf4j7-workbench-dark-flash-20260930/`.

The repair puts the policy-backed default metadata and existing preference resolver in the shared head before stylesheets, preserves explicit preference precedence, and gives the root canvas an opaque light/dark palette. The shared loader recognizes the resolver's success marker and retries a stale unmarked node with handlers installed before append. Focused shell/theme tests cover the updated contracts; generated JavaScript and maps come from the TypeScript source.

Focused shell, theme, loader, and WebKit reload tests passed. Copyright, format, TypeScript generation, package, and asset-hash checks completed. The preview was restarted with the verified package and remains running; unrelated diagnostics, logs, evidence, and preview data were preserved.

## Concrete Steps

Working directory: `/Users/havardottestad/Documents/Programming/rdf4j7`.

The focused browser selection was `tests/workbench-theme-first-paint.spec.js` under Playwright WebKit with `RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:18817/rdf4j-workbench`. Each case primes its route, installs a deterministic response gate, and calls `page.reload()`; there are no arbitrary sleeps. All three cases passed. Maven commands used `.m2_repo` and offline resolution, with no `-am` or `-q` in test runs.

## Validation and Acceptance

Before repair, the query route showed the light canvas for stored dark preference under light OS settings, and the CSS-pending case had no resolved theme metadata. After repair, all three WebKit reload cases pass: dark query first styled canvas, light repository-list first canvas with dark OS, and dark resolver state before the first CSS response. CSS-pending WebKit has no paint entry while CSS is held, so that case asserts early theme/scheme state rather than claiming pixel evidence. The route settles after gate release and has one theme-script request/tag per reload. Served script/style hashes match the built WAR; the shared shell response confirms resolver-before-stylesheet ordering. No CSP change was made.

## Idempotence and Recovery

The browser test uses a per-test page context, primes the route, calls `page.reload()`, and releases each held response in a `finally` path. The query-route screenshot captures the dark canvas before the page-model response is released; the repository-list case records the light canvas while the app script is held. Screenshots and logs are retained under `/tmp/rdf4j7-workbench-dark-flash-20260930/`; no previous diagnostic or test artifact was removed.

## Artifacts and Notes

The served shell is `/tmp/rdf4j7-workbench-dark-flash-20260930/preview-shell.html`; its SHA-256 is recorded in `asset-hash-manifest.json`. RED reports and screenshots remain under `/tmp/rdf4j7-workbench-dark-flash-20260930/first-paint/`, `css-red/`, and the script-retry report. Green browser evidence is in `webkit-reload-green.log`; first-frame JSON and screenshot are `final-webkit/dark-query.json` and `final-webkit/dark-query.png`, with paired light and CSS-pending JSON states beside them. The asset-hash manifest records matching served/WAR hashes, the package JAR, preview PID, and browser-coverage boundary. `initial-evidence.txt` contains the appended RED and GREEN evidence.

## Interfaces and Dependencies

The shared early resolver must remain the sole owner of the `rdf4j-workbench-theme` preference key, the `rdf4j-workbench-theme-default` metadata, the `prefers-color-scheme` media query, and the `data-theme` root attribute. `workbenchApp.ts` must continue to connect controls and apply fetched deployment defaults after loading the model without overriding a stored explicit preference. The existing local Playwright installation supplies WebKit; no new dependency is planned.
