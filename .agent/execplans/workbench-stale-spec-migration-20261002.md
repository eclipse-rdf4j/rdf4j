# Bring the Workbench browser specs in line with the redesigned Workbench

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It follows `.agent/PLANS.md` in this repository.


## Purpose / Big Picture

The Workbench (the browser user interface of the RDF4J server, sources in `tools/workbench/src/main/webapp`) was redesigned on branch `GH-6071-workbench-redesign`. About 130 of the roughly 420 Playwright browser tests in `e2e/tests` still fail on that branch, and they fail for reasons that say nothing about the product: they open the query result inside an iframe that no longer exists, they expect repositories such as `testrepo1` or `query-refresh-layout` to exist on a server at `localhost:8080`, or they assert markup and layout rules that the redesign replaced on purpose. Because so many tests fail for known reasons, the suite cannot be used as a gate: every run has to be compared by test name against a list of known failures (see `.agent/execplans/workbench-app-shell-and-critique-fixes-20260930.md`, section `Outcomes & Retrospective`, "What remains is the 130 baseline failures").

After this plan, a full Chromium run of `e2e/tests` against a freshly started server has no failures except ones recorded in this plan as real product bugs (each with a reason and a follow-up), and every remaining test checks something the current Workbench is meant to do. A contributor then sees a regression as a red test, not as a change in a diff of failure lists.


## Progress

- [x] (2026-10-02 08:00Z) Fixed the one baseline failure that was a real product bug and in scope for the session that started this plan: Tab from the open "Save query" toggle now moves into its pane (commit `457bfc1ce4`, `workbench-dropdown-detail-parity.spec.js` "query detail panels ..." passes in Chromium, Firefox and WebKit).
- [x] (2026-10-02 09:10Z) M1: work queue taken from `logs/m0-browser-baseline-failures.txt` and `logs/m2-browser-chromium-new-failures.txt` (a partial full run on the branch head failed only tests already on those lists; see `Decision Log`).
- [x] (2026-10-02 09:15Z) `query-structure-red.spec.js`: own repository; two tests retired, one migrated; passes in Chromium, Firefox and WebKit.
- [ ] (in progress) M2 to M4, split into seven groups of spec files worked on in parallel, each against its own fresh server (groups listed in `Artifacts and Notes`).
- [ ] M2: migrate the environment-bound specs (fixed repository names, `localhost:8080` / `8091` / `8080` defaults) to the shared helpers.
- [ ] M3: migrate or retire the specs that use the retired result iframe `#query-results-frame`.
- [ ] M4: migrate or retire the remaining specs that assert replaced markup or replaced layout rules.
- [ ] M5: full Chromium, Firefox and WebKit runs on a fresh server; record the result and the remaining real bugs.


## Surprises & Discoveries

- Observation: a server that has served several interrupted runs accumulates repositories (runs that are stopped never reach `test.afterAll`). With about 100 repositories, the windowed repository list and the Delete page's selector no longer render the fixture repository, and two router and form specs fail that pass on a fresh server.
  Evidence: on the polluted server `workbench-router.spec.js:189` fails with `expect(locator).not.toHaveCount(expected) Expected: not 0 Received: 0` and `workbench-form-options-sizing.spec.js:858` with "the delete selector includes only the fixture repository under inspection"; both pass on a fresh server built from the same jar. Always measure on a fresh `appdata.basedir`.
- Observation (product bug, fixed): when the Query page moved from XSLT to Lit (commit 11d8f194ca), Explain's Cancel action lost its cancel icon and the `.workbench-action-label` wrapper around its input; without the wrapper the input kept its field border, so a running Explain showed a bordered field inside the red outline. Fixed in commit 150a668745 by rendering `icon(runtime, 'cancel')` and the label wrapper, as the pre-Lit XSL did.
  Evidence: `workbench-second-review-red.spec.js` "query starts without an empty result island and Explain cancel has a labelled action root" failed with `expect(locator).toHaveCount(expected) Expected: 1 Received: 0` for the action icon, and passes in all three engines after the fix.
- Observation (product bug, fixed): in WebKit (Safari), Escape did not close a settings pane opened with a mouse click, because WebKit does not focus a clicked button and `dismissDisclosureOnEscape` in `template.ts` only acted on focus in the pane or on its toggle. `workbench-settings-panes.spec.js` did not see it because it focused a field in the pane before pressing Escape. Fixed in commit 3326c2af52 (focus on an element around the toggle counts too), with a unit test and a new spec test "a pane opened with a click closes with Escape" that failed in WebKit before the fix.
- Observation (not fixed, follow-up candidates from G1): an HTTP 500 answer with a non-NDJSON body is reported as "Invalid Workbench query stream: a record is not valid JSON." without the HTTP status; opening a result pane with the keyboard leaves the Query settings pane (another toolbar) open, since only a pointer press outside closes a pane.
- Observation: background shells in the agent harness are stopped after 30 minutes unless started with a longer timeout; a server started with the default limit disappears in the middle of a long run and every later test fails with connection errors. Start servers with the maximum background timeout and split long runs into shards.


## Decision Log

- Decision: a failing test is handled in one of four ways, chosen per test and recorded in `Artifacts and Notes`: (1) environment fix, when only the repository, port or set-up is wrong; (2) migration, when the behavior the test protects still exists and only the way to reach or measure it changed (for example the iframe became an in-page element); (3) retirement, when the behavior was removed or replaced on purpose by a recorded design decision, or when a newer spec already checks the same behavior (the retirement names the decision or the newer spec); (4) product bug, when the current Workbench does not do what it is meant to do: the test stays as it is (not weakened) and the bug is recorded in `Surprises & Discoveries` and fixed test-first under `AGENTS.md` Routine A, in this plan if small or as a follow-up otherwise.
  Rationale: the purpose is a suite that fails only for real problems. Weakening an assertion to make it pass would hide exactly those problems, which `AGENTS.md` forbids ("No muting tests ... weakening assertions").
  Date/Author: 2026-10-02, Claude.
- Decision: only test files (`e2e/tests/*.spec.js`, `e2e/tests/workbench-test-helpers.js`) change in M2 to M4; product code changes only for bugs of kind (4).
  Rationale: another session may be editing Workbench front-end files in the same checkout; keeping this plan to test files avoids collisions, and each commit stages only its own paths.
  Date/Author: 2026-10-02, Claude.
- Decision: M1 does not wait for a fresh full run. The work queue is the union of `logs/m0-browser-baseline-failures.txt` (139 tests) and `logs/m2-browser-chromium-new-failures.txt`; every group runs its own files on a fresh server, which shows the current failures of those files, and M5's full runs are the acceptance check.
  Rationale: a full Chromium run with this many stale tests takes well over an hour, because tests that wait for the retired iframe or a missing repository time out after 30 seconds (the spacing audit's after 4 minutes). A partial full run on the branch head (77 tests) failed only tests already on the two lists.
  Date/Author: 2026-10-02, Claude.


## Outcomes & Retrospective

(To be written at each milestone.)


## Context and Orientation

The browser tests are Playwright specs in `e2e/tests`, configured in `e2e/playwright.config.js` (one worker, projects `chromium`, `firefox` and `webkit`). They talk to a running RDF4J server that also serves the Workbench. Build and start it from the repository root:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    cp tools/server-boot/target/rdf4j-server-boot-*.jar <scratch>/server.jar
    java -Dorg.eclipse.rdf4j.appdata.basedir=<fresh empty directory> -jar <scratch>/server.jar --server.port=18812

Copy the jar before starting it, because the next build replaces the jar in place. Point the specs at the server with two environment variables, which most specs and the helpers read:

    cd e2e
    RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:18812/rdf4j-workbench \
    RDF4J_SERVER_BASE_URL=http://127.0.0.1:18812/rdf4j-server \
    npx playwright test --project=chromium --reporter=list tests/<file>.spec.js

The shared helpers are in `e2e/tests/workbench-test-helpers.js`. A spec that needs data calls `createSeededRepository(request, serverBaseUrl(), id)` in `test.beforeAll` with `id = uniqueRepositoryId('<spec name>')` and `deleteRepository` in `test.afterAll`; the seeded repository holds the BSBM sample in graph `http://example.org/graph/bsbm`. `repositoryPageUrl(id, view)` builds a page URL, `openQueryPage`, `setQueryEditor` and `runQuery` drive the Query page, and `waitForRoute(page, view)` waits until the in-page router shows a view (links and forms no longer load a new document, so `page.waitForNavigation` and load events are the wrong thing to wait for).

What changed in the Workbench, as far as the old specs are concerned:

The query result is no longer an iframe. Execute streams the result into `#query-results` on the Query page; its root is `#query-results [data-query-stream-root]`. Ids inside the result carry a suffix (for example `query-result-download-toggle-1-query-results`), so select them by class (`.query-result-download-toggle`, `.query-result-options-toggle`, `.query-result-fullscreen` is `[id^="query-result-fullscreen-"]`) or by id prefix. The result's "Options" toggle is now labelled "Display" (accessible name "Result display options"). The table is `table.data` inside `[id^="query-result-table-wrap-"]`; long results page with a "Load more" button (`.query-result-load-more`). The result count is the badge `#query-results-count` on the "Results" tab of the output card `#query-output`, and the explanation is the other tab.

The Query form's "Options" disclosure is "Query settings" (`#query-options-toggle`, `#query-options-panel`) and "Save query" is `#save-query-toggle` / `#save-query-panel`. Every settings pane (these two, the result Download and Display panes, the explanation Config pane, Explore's Display, Export/Add/Create/Server advanced settings) opens over the page below its toggle, moves nothing, closes with Escape or a press outside it, and only one pane of a toolbar is open at a time (plan M14.3, spec `e2e/tests/workbench-settings-panes.spec.js`). Tests that open two panes of one toolbar at once, expect a pane to push the content below it down, or expect a pane to be as wide as the toolbar assert the old design.

Other replaced markup, each with the decision that replaced it recorded in `.agent/execplans/workbench-app-shell-and-critique-fixes-20260930.md` (`Decision Log` and `Outcomes & Retrospective`): Summary and Information use key/value lists instead of `table.simple`; the menu icons are `svg.workbench-action-icon` (not `svg.query-nav-icon`); the left navigation is a shell with a context bar `#workbench-contextbar` and a repository switcher `#workbench-repository-switcher`; Delete asks for the typed repository id in a dialog; Update, Add, Remove and Clear stay on their page and show a tick (`waitForWriteDone`); result headers show variable names as written (no capitalization); form cards are 760px wide.

The list of the failures at the start of the previous plan is `logs/m0-browser-baseline-failures.txt`; the current list is recorded by M1 below in `logs/stale-spec-migration-m1-failures.txt`.


## Plan of Work

M1 runs the whole Chromium suite once on a fresh server built from the branch head and writes the failing test names, grouped by spec file, to `logs/stale-spec-migration-m1-failures.txt`. For each file it notes the dominant cause (environment, iframe, replaced markup, unknown). This list is the work queue and the acceptance baseline for every later milestone.

M2 handles the environment-bound specs. Each one stops depending on a pre-existing repository or a fixed port: it reads the base URLs through `workbenchBaseUrl()` / `serverBaseUrl()` and creates its own seeded or empty repository with a unique id, deleting it afterwards. Some of these specs then pass; others move on to an iframe or markup failure that M3 or M4 handles. `workbench.spec.js` and `server.spec.js`, which hard-code `localhost:8080`, are migrated the same way.

M3 handles the specs that read the result through `page.frameLocator('#query-results-frame')` or `#query-results-frame` (`rg -l "query-results-frame" e2e/tests`). Each test is read for its intent. Where the intent still applies (rows render, paging works, toolbar controls do not overlap, fullscreen toggles in place), the test is rewritten against `#query-results [data-query-stream-root]`. Where it measured something that only existed because of the iframe (frame height sizing to content, "real frame scrolling", an XSL stylesheet response being held), it is retired with a note.

M4 handles the rest: specs that assert replaced markup or layout. Each failing assertion is compared with the design decision that replaced it; the test is migrated to the new markup if the intent survives, retired if the decision removed the behavior or a newer spec already covers it, and kept failing with a recorded bug if the Workbench is wrong.

M5 runs the full suite in Chromium, Firefox and WebKit on a fresh server and records the outcome, including the remaining real bugs and engine-specific differences.


## Concrete Steps

For each spec file, in the working directory `e2e`: run the file alone on a fresh server and read every failure; open the file and decide, test by test, between environment fix, migration, retirement and product bug (see `Decision Log`); edit; rerun the file until every test passes or fails only for a recorded product bug; add one line per test to `Artifacts and Notes` saying what was done and why. Commit per spec file or per small group of related files, staging only those paths, with a message starting with `GH-6071` and no co-author lines.

Before committing a retirement, search the newer specs for the same behavior (`rg -n "<selector or behavior>" e2e/tests`) and name the covering spec in the note; if nothing covers a still-intended behavior, migrate instead of retiring.


## Validation and Acceptance

Acceptance is a full Chromium run, on a fresh server built from the branch head, in which every failing test is listed in `Surprises & Discoveries` as a real product bug with a follow-up, plus Firefox and WebKit runs whose extra failures are recorded as engine differences. The unit tests (`cd e2e && npm run test:unit`) keep their known baseline (4 failures in `query-compact-stream.test.js` and `query-stream-contract.test.js`, plus "result disclosure panels open below their trigger ..." in `query-stream-refinements.test.js`, all failing before this plan).


## Idempotence and Recovery

Every spec creates and deletes its own repository, so specs can be rerun in any order. If a run is interrupted, its repositories stay on the server; start the next run with a new empty `appdata.basedir` instead of cleaning up by hand. Retired tests are deleted from the spec file; `git log -p -- <file>` recovers them.


## Artifacts and Notes

Groups of spec files (each worked on against its own fresh server):

    G1 query result iframe: query-refresh-layout, query-refinement-new-requirements-red, query-final-live-smoke,
       query-result-lifecycle, query-result-toolbar-geometry
    G2 streaming and explanations: query-stream-packaged-compatibility, query-stream-row-store-lifecycle,
       query-timeout-after-partial-results, query-explanation-alignment, workbench-explain, workbench-query-load-more,
       workbench-disclosure-anchor, workbench-disclosure-collapse-geometry
    G3 component contracts: workbench-consistency-contract, workbench-final-closure-red, workbench-action-alignment,
       workbench-form-options-sizing
    G4 shell and routes: workbench, server, workbench-navigation-selection, workbench-live-route-parity,
       workbench-route-ui-regressions, workbench-initial-page-model, workbench-refresh-all-pages-red,
       workbench-page-components-red
    G5 earlier review specs: workbench-second-review-red, workbench-review-corrections-red,
       workbench-refresh-corrections-red
    G6 visual refinement and theme: workbench-visual-refinement, workbench-visual-refinement-followup,
       workbench-visual-refinement-pane-states, workbench-visual-refinement-operation-states, workbench-theme
    G7 spacing and motion: workbench-spacing-audit, workbench-spacing-system, workbench-motion

One line per migrated, retired or kept-failing test:

    query-structure-red.spec.js "keeps query disclosure triggers fixed while panels open": retired; it opened both
       query panes at once and expected toolbar-wide stacked panes (replaced by M14.3); workbench-settings-panes.spec.js
       checks that each pane opens without moving its toggle and that opening one closes the other.
    query-structure-red.spec.js "uses a stable result header grid and readable narrow title": retired; it measured the
       retired iframe's header; workbench-dropdown-detail-parity.spec.js "dynamic result details keep distinct triggers
       and responsive Format-style fields" checks the 390 px result toolbar (one row, no overlap, no overflow).
    query-structure-red.spec.js "hides empty query errors and keeps Explain secondary": migrated; own repository, and
       the checkbox accent is compared with the --workbench-primary token instead of the retired teal rgb(15, 118, 110).
    G5 (commit 8ae5245295): all three files create their own repositories and read the in-page result.
       workbench-refresh-corrections-red.spec.js: environment fixes only (its member-count check now looks for the two
       repositories it created, because the member list shows every repository on the server).
       workbench-review-corrections-red.spec.js "mobile result footer remains reachable after the last record": retired
       (Previous/Next footer of the retired iframe; covered by workbench-result-scrolling.spec.js "mobile records use the
       table header labels and the last record is reachable", workbench-query-load-more.spec.js "Execute requests one
       million rows by default and renders no result page controls" and the migrated second-review paging test).
       "workbench action and data typography uses the shared readable scale": the action font now equals the 13px
       --workbench-control-font-size token (introduced by commit 847789c558 after the test was written, and asserted by
       workbench-consistency-contract.spec.js); the other tests migrated to current markup with the same thresholds.
       workbench-second-review-red.spec.js: teal literals compared with the primary token; tests that had become
       vacuous (Create's Advanced details element, Explore's details element, Explain colors) now measure the current
       panes; paging test uses Load more with a 40-row batch; compare-mode labels read from the current toolbar.
       "query starts without an empty result island and Explain cancel has a labelled action root" exposed a product
       bug, fixed in commit 150a668745 (see Surprises & Discoveries). Result: 30 of 30 pass in Chromium, Firefox, WebKit.
    G1 (commit b391655f91): all five files create their own repository and read the streamed in-page result.
       query-refresh-layout.spec.js retired: "keeps a result pending while its XSL stylesheet response is held" (XSLT
       removed; load-more "query progress keeps loaded rows browsable and Load more waits for terminal metadata"),
       "sizes short results to content and caps large result scrolling" and "resizes result content through repeated
       disclosures and supports real frame scrolling" (frame height and scrolling; workbench-result-scrolling.spec.js
       "results scroll with the page under a pinned header" and dropdown-detail-parity "dynamic result details ..."),
       "keeps embedded result paging visible and groups download controls" (load-more "Execute requests one million rows
       by default and renders no result page controls", dropdown-detail-parity), "keeps the embedded result header
       aligned when result disclosures open" and "keeps the embedded result title readable on a narrow viewport" (the
       result heading is visually hidden in the output card, M3.5; toolbar row covered by dropdown-detail-parity and
       workbench-settings-panes.spec.js). The other tests migrated; checks that had become vacuous (0x0 wrappers,
       closed panes, the hidden side menu at 390 px) now measure the visible elements; screenshots go to the test output
       directory instead of tracked design folders. query-refinement-new-requirements-red.spec.js: the menu check counts
       the 17 entries of the default menu (app-shell plan decision on the default menu groups) and requires an icon on
       each, instead of "at least 18 icons". query-final-live-smoke.spec.js: one POST per execution, the ASK value, a
       "380 rows · complete" status instead of 100-row pages, a visible error callout. query-result-lifecycle.spec.js:
       the designed failure state ("Query failed." and the error callout). query-result-toolbar-geometry.spec.js:
       measures the live toolbar. Result: 21 of 21 pass in Chromium, Firefox, WebKit.


## Interfaces and Dependencies

No new dependencies. Specs use `@playwright/test` and `e2e/tests/workbench-test-helpers.js`. If a helper is missing (for example a function that waits for a streamed result to finish), add it to the helpers file with a short comment and export it, rather than copying the code into several specs.
