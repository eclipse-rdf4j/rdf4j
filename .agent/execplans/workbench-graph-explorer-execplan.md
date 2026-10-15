# Add a Graph Explorer page to the RDF4J Workbench (React island, unified CSS, npm+Maven frontend build)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.

## Purpose / Big Picture

After this work, a person running the RDF4J Workbench (the browser UI shipped as `tools/workbench`, mounted at `/rdf4j-workbench`) sees a new **Graph** entry in the Repository menu. Opening `/rdf4j-workbench/repositories/<id>/graph` shows an interactive graph of the repository's data: the page runs a SPARQL SELECT with `?source ?predicate ?target` bindings, draws resources as nodes and predicates as edges, and lets the user expand neighbours, filter, switch to table/card/matrix/timeline/map/chart views, save the workspace locally and fullscreen the canvas. The page follows the Workbench light/dark theme and reuses its buttons, panels and tokens, so it looks like the rest of the Workbench rather than a foreign app. Administrators hide it like any other page (`menu.item.graph.visible=false`).

The explorer is a React application ("Graph Field") written by the repository's maintainer outside this repo (the source archive is `~/Downloads/Graph-Field-ui-audit-source.zip`; an extracted read-only copy lives in the session scratchpad under `graph-field/Graph-Field/`). Bringing it in forces a second user-visible change for developers: the Workbench frontend becomes a real npm project (`tools/workbench/package.json`) whose build Maven runs automatically through `frontend-maven-plugin`, so people who never touch npm still get a working WAR, while frontend developers can run `npm run dev`/`npm run build` directly. The existing lit-html TypeScript app is not rewritten; React and Lit coexist.

To see it working at the end: run the Spring Boot bundle (`mvn -o -Dmaven.repo.local=.m2_repo -pl tools/server-boot spring-boot:run`), create a repository with some data, open `http://localhost:8080/rdf4j-workbench/repositories/<id>/graph`, and watch a graph appear after the default query runs.

## Progress

- [x] (2026-10-14 08:30Z) M0 npm project (tsc classic + Vite island) and Maven automation, committed JS kept and proven byte-identical (WorkbenchFrontendBuildTest 4/4 green; offline module build green; verify-classic-output: 42 identical files)
- [x] (2026-10-14 08:40Z) M1 retire committed JS; harness, server-boot, CI, docs consume the build output (42 compiled files + compileTypescript.sh deleted; e2e unit suite 744/744; WorkbenchPolicyBootPackagingTest 9/9 incl. new workbenchScriptsComeFromTheBuiltWebapp; CI unit jobs on Node 22 build tools/workbench first)
- [x] (2026-10-14 08:55Z) M2 `GraphServlet` + policy + registry + descriptors (TDD: GraphServletTest 9/9 red (501) → green; WorkbenchPagePolicyTest, WorkbenchHtmlShellTest, WorkbenchDeploymentDescriptorTest, WorkbenchFrontendBuildTest 40/40; WorkbenchPolicyBootPackagingTest 10/10)
- [x] (2026-10-14 09:05Z) M3 TS route/menu/shell wiring + Node route tests (workbench-graph-route.test.js 5/5 red → green; e2e unit suite 749/749; browser smoke pending in M4)
- [x] (2026-10-14 09:20Z) M4 port the explorer as the React island (Wikidata removed, workbench transport, per-repository workspace, auto-run default query; `npm run typecheck` clean; `npm test` 127/127; browser: demo repository renders 18 nodes/23 relations, layout settles, Explore round trip clean)
- [x] (2026-10-14 09:45Z) M5 CSS reconciliation onto the Workbench design system (stylesheet regenerated scoped under .graph-explorer with tokens aliased to --workbench-*, 414 hard-coded colours mapped by luminance/hue, topbar removed, palette tokens light/dark, css-inventory.test.mjs 3/3; browser: light and dark both follow the Workbench switch; Playwright visual spec written, run pending with the e2e runtime)
- [x] (2026-10-14 10:05Z) M6 generated-query JUnit, Playwright smoke, documentation (GraphExplorerQueryTest 14/14 over committed fixtures; query-fixtures-current.test.mjs red → green; Playwright workbench-graph-explorer.spec.js 3/3 + workbench-graph-explorer-visual.spec.js 4/4 on chromium against server-boot; README section; docs section pending)
- [x] (2026-10-14 10:20Z) M7 polish: Explore link in the node inspector (routes in place to explore?resource=<iri>); fullscreen and prefers-reduced-motion already present in the ported explorer; policy hide test in WorkbenchPagePolicyTest (M2). Remaining ideas recorded in STYLE-MAP.md and Outcomes.
- [x] (2026-10-14 13:20Z) M8 design unification (user request after a screenshot audit of every control, 84 shots light/dark at 1440/1024/390): stylesheet rewritten on Workbench components (workbench-action buttons with Workbench stroke icons, workbench-segmented view tabs, workbench-popover menus, card shell), one toolbar row (view tabs merged into it, "Knowledge graph" heading dropped, stats moved to a one-line status bar), zoom controls and selection actions moved onto the canvas, compact page header for full-bleed pages; canvas 1440×900 grew from ~590 px (and overflowing the window) to ~737 px. `npm test` 131/131, typecheck clean, Playwright graph specs 7/7, CSS-reading Node unit tests 26/26.

- [x] (2026-10-14 18:40Z) M9 intuitive exploration (user request 2026-10-14 afternoon: "more intuitive without adding lots of explanations"; see "M9 — Intuitive exploration" below). `npm test` 141/141 (canvas-scope 5, node-details 4, type-colors 1, humanizeIri 1 new; semantic-summary test removed with the feature), typecheck clean, `GraphExplorerQueryTest` 16/16 (expand-fenced, expand-portable new), e2e unit 749/749, Playwright graph specs 11/11 (new `workbench-graph-explorer-interaction.spec.js` 4/4, which fails 4/4 against the session-start bundle), `tools/workbench` module verify 991/991.
  - [x] M9.1 canvas scope: the Graph view draws resources and the links between them; literal values and `rdf:type` links stay in the data (tables, details) and a "Values" toggle draws them as nodes again; demo repository 68 → 17 drawn nodes, 100 → 37 links
  - [x] M9.2 details card follows the selected node (values, links, actions) as an overlay; the focus summary overlay and the Inspect toggle are gone; a node the card would cover slides out from under it
  - [x] M9.3 double-click (double-tap) expands a node's neighbours; "Expand" is the first action in the details card, the selection bar and the node menu
  - [x] M9.4 chrome: distinct type colours and a clickable type legend; status bar without renderer/overlap jargon; stress tests only in dev builds; semantic clusters removed; instruction sentences removed; labels resolved before the first layout (initial zoom 0.64 → 1.0)

- [x] (2026-10-14 20:10Z) M10 intuitive filters (user request "make the filters more intuitive"; see "M10 — Filters" below). `graph-facets.test.mjs` 7/7 red (module missing) → green, `npm test` 148/148, typecheck clean, Playwright graph specs 14/14 (new `workbench-graph-explorer-filters.spec.js` 3/3), e2e unit 749/749, `tools/workbench` module verify 991/991 (logs/mvnf/20261014-162307-verify.log).

## Surprises & Discoveries

- Observation: the committed `tools/workbench/src/main/webapp/scripts/*.js` and `*.js.map` are byte-identical to what `tsc` 5.9.3 produces with the flags in `compileTypescript.sh` (42 files compared, zero differences, 2026-10-14). The npm `build:classic` leg can therefore be introduced without any visible change.
- Observation: the explorer's responsive breakpoints (`@media (max-width: 1080px)` drawer mode) are window-based; inside the Workbench the island is ~175px narrower than the window, so the query drawer appears at wider windows than in the standalone app. Container queries would fix it (follow-up).
- Observation: the original stylesheet used 414 colour literals, none more than four times; a per-class hand mapping was not realistic, so the generator (scratchpad `css-transform.py`, not in the repo) mapped them by luminance and hue onto the aliased tokens, and the result is maintained by hand from here on.
- Observation: under Vite 8 (rolldown) `import cola from 'webcola'` inside the layout worker yields `undefined` (webcola's `dist/index.js` is CommonJS without a default export), so every layout failed with "Cannot read properties of undefined (reading 'Layout')"; `import * as cola from 'webcola'` fixes it. The source project's bundler tolerated the default import.
- Observation: `@import "tailwindcss"` and the Google Fonts `@import` must go before Vite can build the explorer CSS (postcss-import cannot resolve them); the Workbench ships no web fonts anyway.
- Observation: `CacheFilter` caches `scripts/graph-explorer/graph-explorer.mjs` for 600 s; after redeploying, `fetch(url, {cache: 'reload'})` or a hard reload is needed to see a new bundle.
- Observation: page-data responses are NDJSON records `head, view, vars, links, rows, end` (not `result`); the `view` record carries the id under `id`; rows are `{"type":"rows","values":[[...]]}`. The shell learns the repository id from the request attribute `WorkbenchPageProtocol.REPOSITORY_ID_ATTRIBUTE` (set by `ProxyRepositoryServlet`), not from the path, so unit tests must set it to get `Graph · repo — RDF4J Workbench`.
- Observation: `tools/workbench/package.json` must NOT declare `"type": "module"`: `target/frontend/scripts/*.js` sits below it, so Node would treat the compiled classic scripts as ES modules and `require(queryCancelPolicy.js)` in `e2e/tests-unit/query-cancel-policy.test.js` lost its `module.exports` (10 failures until the key was removed).
- Observation: with `-Dmaven.repo.local=.m2_repo` the plugin caches the Node tarball at `.m2_repo/com/github/eirslett/node/22.23.3/node-22.23.3-darwin-arm64.tar.gz`; `npm ci` with `npm_config_cache=${settings.localRepository}/npm-cache` fills `.m2_repo/npm-cache` (18 MB) and later runs work offline. The plugin's own jar still needs one online Maven run. A first `npm ci` on an empty cache needs the npm registry even under `mvn -o` (npm is not Maven).
- Observation (M8): the Workbench styles every `<button>`, `<p>`, `h2`/`h3` and input on the page surface with element rules (`body.workbench-body .workbench-page-surface button` 0-2-2, `p:not(.workbench-field__help)` 0-3-2, inputs 0-7-2) that outranked the explorer's `.graph-explorer .x` rules: the context-menu scrim was painted as an opaque white button (the whole page went blank behind the node actions), status lines got 12px margins and wrapped. The island's rules are now rooted at the `#graph-explorer-app` id of its `<main>`.
- Observation (M8): `.app-shell { height: 100dvh }` inside an island sized to `100dvh - top` pushed the status bar and zoom controls below the window; the shell now takes `height: 100%` of the island.
- Observation (M8): the Workbench's `text-wrap: pretty` on every `p` overrides an inherited `white-space: nowrap`, so one-line status text needs `white-space: nowrap` on the element itself.
- Observation: `frontend-maven-plugin` latest release is 2.0.2 (Maven Central metadata, 2026-10-14); it needs Java 17+ and Maven 3.6+. Newest Node 22 LTS is v22.23.3 (nodejs.org index, 2026-09-23). The local machine has Node v22.18.0 and npm 11.14.1 via nvm.

- Observation (M9): the first layout of a query result ran on placeholder labels (IRI local names such as "alice"); for the demo repository that layout is 1849×908 units against 1048×641 with the real labels (scratch probe `layout-probe.mjs`: fit k 0.642 vs 1.000), and label hydration afterwards only repairs overlaps and keeps the viewport, so the page opened zoomed out with 8px text. `execute` and `expandNodes` now wait up to 1.5 s for the labels (`withLabels`) before merging.
- Observation (M9): the zoomed-out "semantic cluster" summary placed each type at the centroid of its nodes; type groups that are mixed in the layout share a centroid, so 393 nodes of three types became one 60px bubble ("Person 361 nodes") with two hidden behind it, and the demo's 68 nodes opened as seven overlapping bubbles.
- Observation (M9): facets and cards counted every model node, so with values hidden "Node kind: Literal" or "No type" selected nodes the canvas does not draw; facets, the selection and the Cards/Matrix views now use the drawn nodes (`filterNodes`).
- Observation (M9): `typeVisual` hashed the type IRI onto six colours; two of the demo's five types were red. Palette slots are now handed out in first-seen order, and the slot order (teal, coral, blue, amber, violet, green) keeps the similar teal and green apart.

- Observation (M10): the filter panel listed 18 facets for the 100-statement demo, alphabetically, with `type` duplicating "RDF type" and `label` offering 17 one-off names; counts never changed, so after choosing Person the City and Organization properties still looked selectable; ages and dates were checkbox lists of unique values; a search for "oslo" left the three matching facets collapsed.
- Observation (M10): predicates outside the well-known namespaces kept their camelCase local name ("worksFor") as label until a label was loaded, because the fallback was `compactTerm`; schema.org predicates read "works for" only because `schema:worksFor` failed `isHumanLabel` and was humanized later.

## Decision Log

- Decision: React island mounted in-page inside the lit-html outlet, not an iframe.
  Rationale: the user wants unified code and CSS; an iframe would duplicate chrome and styles.
  Date/Author: 2026-10-14, user decision.
- Decision: keep the existing lit-html TypeScript as classic global scripts compiled by tsc inside the npm build; do not convert to ES modules now.
  Rationale: 31k lines share one `module workbench {}` namespace loaded by `<script src>` tags and evaluated by 40+ Node harness tests in a `vm` context; converting is a separate project. The React island is an ES module (`.mjs`) loaded with `<script type="module">`, which the app already does for `workbench-lit-html.mjs`.
  Date/Author: 2026-10-14, Claude (plan approved by user).
- Decision: Maven runs the npm build via `com.github.eirslett:frontend-maven-plugin` 2.0.2 bound to `generate-resources`, output in `target/frontend`, added to the WAR through `maven-war-plugin` `webResources`; compiled JS is no longer committed after M1; `tools/server-boot` unpacks the built WAR instead of copying the source webapp.
  Rationale: user wants a proper npm project with Maven automating it for people who do not use npm directly.
  Date/Author: 2026-10-14, user decision + Claude design.
- Decision: SPARQL transport is a dedicated `GraphServlet` returning `application/sparql-results+json`, not the Query page's download mode.
  Rationale: download mode wraps output in a heartbeat stream, adds `Content-disposition: attachment` and couples the explorer to Query-page policy gates.
  Date/Author: 2026-10-14, Claude.
- Decision: Wikidata/QLever endpoint support and the two Wikidata-only API routes are removed; the explorer is scoped to the current repository.
  Rationale: user decision; smallest attack surface; the Workbench never lets the browser talk to arbitrary hosts.
  Date/Author: 2026-10-14, user decision.
- Decision (M8): restyle by rewriting `graph-explorer.css` (4.4k generated lines → 2914 hand-written) on Workbench components instead of patching the luminance-mapped rules; graph drawing rules kept.
  Rationale: the generated mapping used the outline grey as a surface and the primary colour as both fill and text (unreadable pressed buttons, dark banners in light mode, 7–10px text); a per-rule fix would have kept two parallel button systems.
  Date/Author: 2026-10-14, Claude (user asked to unify the design and keep the graph area large).
- Decision (M8): "Start over" re-runs the default query (it used to clear the graph and report the removed "Curie science network").
  Date/Author: 2026-10-14, Claude.
- Decision (M9): literal values and `rdf:type` links are hidden from the canvas, not from the model: every view except the Graph canvas still sees all triples, workspaces save everything, and the "Values" toggle (persisted per browser in `localStorage`) brings them back as nodes. Precedent: property panels in Neo4j Bloom, GraphDB's visual graph and Ontodia.
  Date/Author: 2026-10-14, Claude (user asked for a more intuitive graph without more explanations).
- Decision (M9): the semantic cluster summary is removed rather than repaired; changing representation on a zoom threshold is surprising, graphs under 500 drawn nodes are drawn as nodes at any zoom, and the large-graph canvas covers bigger ones.
  Date/Author: 2026-10-14, Claude.
- Decision (M9): "Hide" replaces "Remove"/"Delete" for taking nodes off the graph (with an eye-off icon instead of a bin), because the Workbench's own Remove page deletes statements from the repository.
  Date/Author: 2026-10-14, Claude.
- Decision (M10): filters keep highlighting matches (others dim) rather than hiding non-matches, so context stays visible; Hide in the selection bar removes matches explicitly. Filter counts are disjunctive (each filter counted with all the other filters applied), the usual faceted-search rule, so a count is exactly what checking the value would add.
  Date/Author: 2026-10-14, Claude.
- Decision (M10): a range filter is stored as the set of loaded values inside it (so selection, matching and the visual query keep one model) plus the typed bounds; the generated SPARQL uses the bounds (numeric comparison, ISO day comparison for dates), and the value set is recomputed when values are loaded or hidden.
  Date/Author: 2026-10-14, Claude.
- Decision: branch and commit label `GH-0000` (no GitHub issue number was provided).
  Date/Author: 2026-10-14, Claude per AGENTS.md.

## Follow-ups after M7 (2026-10-14)

- Dash licence check (`org.eclipse.dash.licenses` 1.1.0 on `tools/workbench/package-lock.json`, 75 items): 73 approved (47 via the Eclipse Foundation, 26 via ClearlyDefined); two need an IP review ticket: `npm/npmjs/-/webcola/3.4.0` (MIT, restricted) and `npm/npmjs/@viz-js/viz/3.17.0` (no licence detected automatically; viz.js is MIT, the embedded Graphviz is EPL-1.0). Filing the review needs a Dash IP-lab token (`-Ddash.iplab.token`), which only a committer can do.
- Drawer breakpoints are now container queries (`@container graph-explorer (max-width: ...)`): the island (`container-type: inline-size`) rather than the window decides the layout, so the query drawer behaves the same beside the Workbench sidebar as standalone.
- The large-graph 2D canvas overlay reads `--graph-explorer-canvas-*` tokens at draw time (light and dark values in the stylesheet) instead of hard-coded dark colours.

## Outcomes & Retrospective

All seven milestones are delivered on branch `GH-0000-workbench-graph-explorer` (uncommitted). A user of the
Workbench now has a Graph entry under Repository; `/repositories/<id>/graph` renders the repository as an interactive
graph in-page, themed by the Workbench tokens, with queries proxied by `GraphServlet`. Developers build the front end
with `npm run build` in `tools/workbench` or implicitly through Maven (`frontend-maven-plugin`), and the compiled
scripts are no longer committed.

Gaps and follow-ups: the explorer's drawer breakpoints are window-based (container queries would respect the
Workbench sidebar); the large-graph canvas renderer (`graph-canvas-controller.ts`) still uses its own dark colours;
the explorer's panels could adopt `workbench-island`/`workbench-popover` markup; `GraphExplorerApp.tsx` stays one
6.4k-line component; the Eclipse Dash licence review of the new npm dependencies (react, react-dom, cytoscape,
cytoscape-fcose, graphology, graphology-communities-louvain, webcola, @viz-js/viz, vite, @vitejs/plugin-react)
is still to be run before merging; no GitHub issue number exists (GH-0000).

Lessons: generate, then hand-maintain, when a stylesheet has hundreds of one-off colour literals; keep the
`package.json` of a module whose `target/` holds classic scripts free of `"type": "module"`; never rebuild a jar a
running server was started from; rolldown's CommonJS interop differs from Next's bundler for default imports.

## Context and Orientation

Terms used below:

- **Workbench**: the web UI in `tools/workbench`, packaged as `rdf4j-workbench.war`. Java servlets under `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/` answer requests; the browser side is TypeScript under `tools/workbench/src/main/webapp/scripts/ts/` compiled to JavaScript in `tools/workbench/src/main/webapp/scripts/` (today committed to git and compiled by hand with `tools/workbench/compileTypescript.sh`).
- **lit-html**: a small HTML templating library (`scripts/vendor/lit-html-3.3.3/`) the Workbench uses to render pages. The bridge `scripts/workbench-lit-html.mjs` is an ES module that publishes `window.RDF4JLitHTML`; `workbenchApp.ts` injects it as `<script type="module">`.
- **Route**: one Workbench page. `scripts/ts/workbenchRoutes.ts` registers each route with the list of scripts it needs and a mount function; `scriptMount(name)` calls `workbench.<name>.mount(outlet)` and expects a cleanup function back. `scripts/ts/workbenchViews.ts` holds titles, icons, menu entries and the page HTML; `scripts/ts/workbenchApp.ts` loads scripts and models.
- **Page model**: when the browser needs a page's data it requests the page URL with `Accept: application/vnd.rdf4j.workbench+ndjson`; servlets answer with newline-delimited JSON records built by `TupleResultBuilder`.
- **Shell**: the fixed HTML written by `WorkbenchHtmlShell.java` for a GET with `Accept: text/html`: `<div id="workbench-app" data-workbench-view=... data-workbench-base-path=...>`, `workbench-theme.js`, three stylesheets and `workbenchApp.js`.
- **Policy**: `WorkbenchPolicy.java` reads `workbench.properties` (defaults in `tools/workbench/src/main/resources/org/eclipse/rdf4j/common/app/config/defaults/workbench.properties`) and decides which pages exist in the menu. `ProxyRepositoryServlet` refuses any route whose servlet is not a visible built-in page.
- **server-boot**: `tools/server-boot`, a Spring Boot jar that embeds both the server and the Workbench; its pom copies `tools/workbench/src/main/webapp` onto the classpath as `rdf4j/workbench-webapp`, and `WebappResourceExtractor.java` unpacks that directory at startup.
- **Graph Field / the explorer**: the React app being integrated. Its client is `app/page.tsx` (6874 lines, one component `Home()`), helpers `app/*.ts`, CSS `app/globals.css` + `app/workspace-theme.css`; server routes under `app/api/` proxied SPARQL and Wikidata lookups. Runtime npm dependencies it really uses: `react`, `react-dom`, `cytoscape`, `cytoscape-fcose`, `graphology`, `graphology-communities-louvain`, `webcola`, `@viz-js/viz`.
- **mvnf**: `python3 .codex/skills/mvnf/scripts/mvnf.py <module|Class|Class#method>` is the repository's test runner (it runs a root `-Pquick` install first). Never run tests with `-am` or `-q`. All Maven commands take `-o -Dmaven.repo.local=.m2_repo`.

Key integration points, verified 2026-10-14:

- `web.xml` (`tools/workbench/src/main/webapp/WEB-INF/web.xml`) maps one servlet `WorkbenchGateway` on `/repositories/*`; sub-commands are init-params (`/explore` → `ExploreServlet` at lines 143-146, `/query` at 147-150). `tools/server-boot/src/main/java/org/eclipse/rdf4j/tools/serverboot/Rdf4jServerWorkbenchApplication.java` lines 316-334 repeat those init-params in Java.
- `WorkbenchViewRegistry.java` lines 46-66 map servlet classes to view ids; `WorkbenchHtmlShell.java` lines 40-47 (`SHORT_TITLES`) mirror `workbenchViews.ts` lines 185-190.
- `WorkbenchPolicy.java`: `builtInPages()` ~line 487, `builtInServletIds()` ~line 522, landing candidates ~line 285; `isRouteAllowed` ~line 260.
- `workbenchRoutes.ts` lines 272-311 register routes; `scriptMount` lines 177-179. `workbenchApp.ts`: `loadClassicScript` lines 648-688, lit bridge module injection lines 940-962, `installRouteRuntime` lines 982-990.
- `workbenchViews.ts`: icons 15-30, `shortTitles` 185, `titles` 202, `descriptions` 232, `repositoryLandingPages` 548, repository action menu 1421, `routeBody` 4352-4370, `repositoryScopedViews` 4380.
- CSS: `styles/workbench-refresh.css` (tokens `:root` lines 5-93, dark theme `html[data-theme="dark"]` lines 4759-4813, every rule nested under `body.workbench-body`), BEM classes `.workbench-action`, `.workbench-island`, `.workbench-popover`, `.workbench-callout`, `.workbench-badge`, `.workbench-field`, `.workbench-select-control`, `dialog.workbench-dialog`. `scripts/workbench-theme.js` sets `html[data-theme]`.
- `scripts/ts/queryStream.ts` lines 1198-1206 locate the query worker by scanning `<script>` tags for `queryStream.js`: compiled file names must not change.
- Node unit tests: `e2e/tests-unit/*.test.js` run by `cd e2e && npm run test:unit`; `script-harness.js` lines 362-366 resolve `tools/workbench/src/main/webapp/scripts/*.js` from the repo root. Playwright specs in `e2e/tests`, run by `e2e/run.sh spring-boot`.
- CI: `.github/workflows/pr-verify.yml` job `frontend-unit-tests` (lines 196-212, Node 20) and e2e jobs (Node 22); `develop-status.yml` and `main-status.yml` also run the unit job. No workflow compiles the TypeScript today.

## Plan of Work

### M0 — Frontend build foundation

Create `tools/workbench/package.json` (private, `engines.node >=22.12`), scripts `build` = `npm run build:classic && npm run build:explorer`, `build:classic` = `tsc -p tsconfig.classic.json`, `build:explorer` = `vite build`, `dev` = `vite`, `typecheck` = `tsc -p tsconfig.json --noEmit`, `test` = Node test runner over `src/test/frontend/**/*.test.mjs`. devDependencies: `typescript@5.9.3` (same as `e2e`), `vite`, `@vitejs/plugin-react`, `@types/react`, `@types/react-dom`. Runtime dependencies are added in M4. Commit `package-lock.json`.

`tools/workbench/tsconfig.classic.json` must reproduce `compileTypescript.sh` exactly: `files` = every `src/main/webapp/scripts/ts/*.ts`, `noImplicitAny`, `sourceMap`, `sourceRoot` `/rdf4j-workbench/scripts/ts`, `outDir` `target/frontend/scripts`, no `target`/`module` keys (tsc defaults), `types: []` is NOT set (the hand-written `.d.ts` files are in `files`).

`tools/workbench/vite.config.ts`: root `src/main/frontend`, `build.outDir` `../../../target/frontend`, `emptyOutDir: false`, `rollupOptions.input` = `graph-explorer/main.tsx`, `output.format` `es`, `entryFileNames` `scripts/graph-explorer/graph-explorer.mjs`, `chunkFileNames` `scripts/graph-explorer/chunks/[name]-[hash].mjs`, CSS asset → `styles/graph-explorer.css`, `worker.format` `es`, `sourcemap: true`, dev `server.proxy` of `/rdf4j-workbench` and `/rdf4j-server` to `http://127.0.0.1:8080`.

`tools/workbench/src/main/frontend/graph-explorer/main.tsx`: placeholder `mount(outlet, ctx)` that renders a heading and returns an unmount cleanup; assigns `window.workbench.graphExplorer`. `src/main/frontend/dev/index.html` is a standalone page that mounts the island for `npm run dev`.

`tools/workbench/scripts/verify-classic-output.mjs` compares `target/frontend/scripts/*.js(.map)` with the committed `src/main/webapp/scripts/` files and exits 1 on any difference (removed in M1).

Maven: root `pom.xml` `<pluginManagement>` gains `frontend-maven-plugin` 2.0.2 with `installDirectory` `${project.build.directory}`, `nodeVersion` `v22.23.3`; properties `skipFrontend` (default `false`). `tools/workbench/pom.xml` adds executions `install-node-and-npm`, `npm ci` (arguments `ci --prefer-offline --no-audit --no-fund`), `npm run build`, all at `generate-resources`, each `<skip>${skipFrontend}</skip>`, env `npm_config_cache=${settings.localRepository}/npm-cache`; a profile `frontend-install` activated when `node_modules` is missing is what runs `npm ci`, so warm trees only rebuild. `maven-war-plugin` gets `<webResources><resource><directory>${project.build.directory}/frontend</directory></resource></webResources>`. `.gitignore` adds `tools/workbench/node_modules/`.

A JUnit test `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/base/WorkbenchFrontendBuildTest.java` (written first, red) parses `tools/workbench/pom.xml` and asserts the three executions and the `webResources` entry exist, in the style of `WorkbenchDeploymentDescriptorTest`.

### M1 — Retire committed JS

Delete the compiled `src/main/webapp/scripts/*.js(.map)` (keep hand-written/vendored files), `compileTypescript.sh`, the verify script. `e2e/tests-unit/script-harness.js` resolves compiled scripts from `WORKBENCH_SCRIPT_DIR` or `tools/workbench/target/frontend/scripts`; update hard-coded callers and `run-workbench-unit-coverage.js`. CI unit job: Node 22, `npm ci && npm run build` in `tools/workbench` before `e2e` tests. `tools/server-boot/pom.xml`: depend on `rdf4j-http-workbench:war`, unpack it with `maven-dependency-plugin` into `target/workbench-webapp` (excluding `WEB-INF/lib`, `WEB-INF/classes`) and expose that directory as resource `rdf4j/workbench-webapp`. Docs: `tools/workbench/README.md`, `AGENTS.md` (`-DskipFrontend`), `site/content/documentation/tools/server-workbench.md`.

### M2 — GraphServlet (TDD)

`GraphServlet extends TransformationServlet` at `/repositories/{id}/graph`: GET html → shell; GET ndjson → page model `id, server, readable, writeable, max-query-length`; POST with `application/sparql-query` body or form `query=` → SELECT only, policy `query-execution` gate (403), length cap `graph-max-query-length` (413, default 150000), parse errors 400, output `application/sparql-results+json` via `SPARQLResultsJSONWriter`, `Cache-Control: no-store`. Register in `WorkbenchViewRegistry`, `WorkbenchPolicy` (page `graph` in group `explore` after `explore`, servlet id, landing candidate), `workbench.properties` `menu.items`, `WorkbenchHtmlShell.SHORT_TITLES`, `web.xml`, `Rdf4jServerWorkbenchApplication`. Tests first: `GraphServletTest` (eight cases listed in the approved plan), updates to `WorkbenchPagePolicyTest`, `WorkbenchDeploymentDescriptorTest`, `WorkbenchHtmlShellTest`, `ProxyRepositoryServletTest`, server-boot `WorkbenchPolicyBootPackagingTest`.

### M3 — TS wiring

`workbenchViews.ts` (icon, titles, descriptions, landing pages, repo action menu, `repositoryScopedViews`, `routeBody` case `graph` → `<div id="graph-explorer-mount" class="graph-explorer-mount">`), `workbenchRoutes.ts` (`scriptMount` passes `ctx`; register `graph` with `['graph-explorer/graph-explorer.mjs']`), `workbenchApp.ts` (`loadModuleScript` for `.mjs`), `workbench-refresh.css` (`.workbench-page-surface--full-bleed`). Node tests: `workbench-routes.test.js` lists, new `workbench-graph-route.test.js`.

### M4 — Port the explorer

Copy `app/*.ts(x)` (minus `api/`, `chatgpt-auth.ts`, `wikidata-predicates.ts`, `layout.tsx`, `rdf4j.ts`) into `src/main/frontend/graph-explorer/` with EDL headers; `page.tsx` → `GraphExplorerApp.tsx`; add `transport.ts` (POST to the servlet), `theme.ts`, `main.tsx` mount. Remove endpoint list/select, Wikidata branches, demo graph; `RdfExplorer(wikidata=false, fence=true, cacheScope=repositoryId)`; workspace storage keyed by repository. Port the pure Node unit tests to `src/test/frontend/graph-explorer/`.

### M5 — CSS reconciliation

Inventory script + `STYLE-MAP.md` mapping each explorer class to a Workbench class or a scoped `.graph-explorer` rule; tokens aliased to `--workbench-*`; delete Tailwind, Google Fonts and all element-level rules; island sized to the viewport minus its top offset; Playwright visual spec for light/dark at 1440/1024/390 px.

### M6 — Query fixtures, smoke test, docs

Export the 14 generated-query cases to `src/test/resources/graph-explorer-queries/`, port `QueryCheck.java` to `GraphExplorerQueryTest`, Playwright `workbench-graph-explorer.spec.js`, documentation section "Graph Page".

### M7 — Polish

Fullscreen, Explore/Query links from nodes, `WorkbenchPagePolicyTest.graphPageCanBeHiddenLikeOtherBuiltInPages`, reduced-motion.

### M9 — Intuitive exploration

Why: a screenshot audit (2026-10-14, demo repository with 100 triples) showed that a first-time user does not see
their data. The default query yields 68 nodes, of which 46 are literal values (labels, dates, ages, coordinates) and
5 are classes reached only through `rdf:type`; the fitted zoom (k < 0.48) therefore switches to the "semantic
cluster" summary, seven overlapping bubbles. Details of a node need the separate "Inspect" toggle, which takes width
from the canvas and says "Select a single node, then reopen the inspector"; expanding a node is hidden in the
right-click menu under a collapsed "Load connected nodes" disclosure that asks for a predicate first; a second click
on a node removes the focus again; the status bar reports "4 edge–node overlaps · Detailed SVG"; the legend says
IRI/Literal/Blank although nodes are coloured by RDF type, and two types can share a colour because the palette index
is a hash. The fix is to make the obvious gesture do the obvious thing, and to delete text rather than add it.

M9.1 — Canvas scope. New pure module `graph-explorer/graph-canvas-scope.ts` exports
`canvasGraph(graph: Graph, showValues: boolean): Graph`. With `showValues` false it returns the same revision with
literal nodes removed, edges whose target is a literal or whose predicate is `rdf:type` removed, and IRI/blank nodes
removed when every incident edge is an incoming `rdf:type` edge (a class used only as a type). With `showValues` true
it returns the graph unchanged (same object). `GraphExplorerApp.tsx` derives `drawnGraph` with it and uses it for
everything the canvas does: the layout job, fit-to-view, edge lanes/ports/curves, label placement, semantic clusters,
WebGL and the large-graph canvas, layout health and the status-bar counts. The model `graph` (and therefore the
Triples, Cards, Matrix, Timeline, Map and Chart views, the filters, workspace save/restore and provenance) keeps every
triple. A toolbar toggle "Values" (`aria-pressed`) flips `showValues`; the choice is kept in `localStorage`
(`graph-field-show-values-v1`, try/catch). Toggling re-runs the layout for the drawn graph.

M9.2 — Details card. New pure module `graph-explorer/node-details.ts` exports
`nodeDetails(node, edges, nodeById)` returning `{ values: Array<{predicate, label, items: GraphNode[]}>,
links: Array<{predicate, label, direction, items: GraphNode[]}> }` grouped by predicate in first-seen order (types
are reported through `node.types`, not as links). The existing inspector becomes this card: it opens when a node is
clicked (the click no longer toggles focus off; empty space clears it and closes the card), it overlays the canvas
instead of narrowing it, and shows the label, coloured type chips, the identifier with a copy button, the actions
Expand / Explore / Pin / Hide, the values, and the links, each linked node a button that focuses it. The
"Focused neighbourhood" overlay and the per-triple provenance lines are removed. The "Inspect" toolbar button is
renamed "Details" and reopens the card.

M9.3 — Expand. `rdf-vocabulary.ts` gains `expandQuery(ids, limit, scope, fence)` (both directions, every predicate,
`?selected ?source ?predicate ?target`, per-node `LIMIT`), `RdfExplorer.expand(ids, limit)` batches it like
`neighbors`, and a fixture `expand-fenced` joins the committed query fixtures run by `GraphExplorerQueryTest`.
`expandNodes(nodeIds, anchorId)` in the app merges the result with the existing incremental layout intent.
Double-click on a node (and a double tap that does not move) calls it; so do the card's primary button and the first
row of the node menu; "Load connected nodes" becomes "Expand by relation".

M9.4 — Chrome. Type colours come from `graph-type-colors.ts` (`createTypeColorRegistry(size)`: first-seen order, so
the first six types are always distinct); the status-bar legend lists the most frequent types of the drawn graph
with their colour, and a legend entry selects that type (the RDF type filter); the status bar keeps the message and
"N nodes · M links"; the stress-test entries in Tools exist only when `import.meta.env.DEV`; the semantic cluster
summary is removed (see Decision Log); status messages lose their "Right-click or long-press…" tails, the selection
summary its instruction line, the filter panel its "OR within a facet · AND across facets" line and per-facet source
captions, the query drawer its policy sentence. Unlabelled IRIs read as words (`humanizeIri`: "birthDate" → "birth
date", PascalCase names and acronyms keep their capitals). The WebGL edge layer of 180–499 node graphs reads the
`--graph-explorer-canvas-*` tokens like the large-graph canvas.

### M10 — Filters

`graph-explorer/graph-facets.ts` is the filter model. `facetModel({nodes, edges, nodeById, selection, typesOf,
hiddenPredicates})` returns the facets and the matching node ids: values are OR within a filter and AND across
filters; each filter's counts and coverage are taken over the nodes that match every other filter; values with no
count are dropped unless selected, and so are filters with no coverage; the Type filter comes first, then property
filters by coverage; `rdf:type` and the explorer's label properties are not offered (Type and search cover them);
a property whose ≥ 4 distinct values are all numbers or all dates gets a `range`, and `valuesInRange(facet, from,
to)` turns typed bounds into the selected value set (`NO_MATCH_VALUE` when nothing is inside). The panel shows the
match count and Clear all, the active filters as chips (× removes one), a search over filter names and values that
opens the filters with matching values, the first seven filters plus "N more filters", value previews in closed
filters, and from–to inputs for ranges. "Count in the whole repository" is offered only when a load reached its
LIMIT. The toolbar badge counts filters, not values. Fallback predicate labels are humanized (`predicateLabel`).

## Concrete Steps

Working directory is the repository root unless stated.

    # 0. once per session
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

    # M0 npm side
    cd tools/workbench && npm ci && npm run build && node scripts/verify-classic-output.mjs
    # expected last line: "classic output identical (42 files)"

    # M0 Maven side (first run WITHOUT -o to fetch the plugin and Node, then offline)
    mvn -B -ntp -Dmaven.repo.local=.m2_repo -pl tools/workbench -Pquick clean package
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl tools/workbench -Pquick clean package
    unzip -l tools/workbench/target/rdf4j-workbench.war | grep -E 'graph-explorer/graph-explorer.mjs|scripts/workbenchApp.js'

    # tests
    python3 .codex/skills/mvnf/scripts/mvnf.py WorkbenchFrontendBuildTest
    python3 .codex/skills/mvnf/scripts/mvnf.py GraphServletTest
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench
    cd e2e && npm ci && npm run test:unit

## Validation and Acceptance

- M0: `npm run build` and `mvn package` both succeed; the WAR contains the compiled classic scripts and `scripts/graph-explorer/graph-explorer.mjs`; the verify script reports zero differences; `-DskipFrontend=true` packages without Node; a second build with `-o` succeeds.
- M1: `cd e2e && npm run test:unit` is green against `tools/workbench/target/frontend/scripts`; `spring-boot:run` serves `/rdf4j-workbench/scripts/workbenchApp.js` with HTTP 200.
- M2: `GraphServletTest` red then green; `curl -X POST -H 'Content-Type: application/sparql-query' --data 'SELECT * WHERE {?s ?p ?o} LIMIT 3' http://localhost:8080/rdf4j-workbench/repositories/<id>/graph` returns SPARQL JSON.
- M3: sidebar shows "Graph"; the route mounts and unmounts the placeholder island; Node route tests green.
- M4: a real graph renders for a seeded repository; navigating Explore → Graph → Explore leaves no React root behind and no console errors.
- M5: in light and dark theme the island's background equals the Workbench surface token; no horizontal overflow at 390 px; screenshots stored as Playwright evidence.
- M6: `GraphExplorerQueryTest` green; Playwright smoke green; docs updated.

## Idempotence and Recovery

All steps are additive until M1 deletes the committed JS; that deletion is a single commit that can be reverted. `npm ci` and `mvn clean package` can be rerun freely. If `frontend-maven-plugin` cannot download Node offline, rerun once without `-o`; the tarball is cached under `.m2_repo/com/github/eirslett/`. `-DskipFrontend=true` restores a Java-only build at any time.

## Artifacts and Notes

M10 evidence (2026-10-14):

    $ node --experimental-strip-types --test src/test/frontend/graph-explorer/graph-facets.test.mjs   # before graph-facets.ts
    ERR_MODULE_NOT_FOUND .../graph-facets.ts
    $ npm test     # after
    # tests 148  # pass 148  # fail 0
    $ cd e2e && npx playwright test --project=chromium tests/workbench-graph-explorer*.spec.js
    14 passed    # first run of the filters spec: 2 failed on "worksFor" vs "works for" (fallback label), fixed in predicateLabel
    browser (demo): 7 filters + "9 more filters" (was 18 flat); Person → Type, age, birth date, home location, works for, knows;
    age 30–40 → "3 of 17 nodes match", chips "Person ×", "age: 30 – 40 ×"; "oslo" opens home location and location

M9 evidence (2026-10-14):

    $ cd tools/workbench && npm test          # with the four new test files, before the modules existed
    ERR_MODULE_NOT_FOUND graph-canvas-scope.ts / graph-type-colors.ts; query-fixtures-current: expand-fenced.rq is missing
    # tests 134  # pass 130  # fail 4
    $ npm run fixtures:queries && npm test      # after
    # tests 141  # pass 141  # fail 0
    $ python3 .codex/skills/mvnf/scripts/mvnf.py GraphExplorerQueryTest
    Summary: tests=16, failures=0, errors=0, skipped=0, time=0.511s
    $ node --test ... rdf-label-engine.test.mjs  # humanizeIri test before the fix
    expected: 'birth date'  actual: 'birth Date'   # pass 8  # fail 1
    $ cd e2e && npx playwright test --project=chromium tests/workbench-graph-explorer*.spec.js
    11 passed     # the interaction spec against the session-start bundle: 4 failed ("Oslo" resolved to 2 elements)
    $ cd e2e && npm run test:unit
    # tests 749  # pass 749  # fail 0
    $ python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs
    Tests run: 991, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS (logs/mvnf/20261014-154353-verify.log)
    browser (demo repository, 1440×900): 17 nodes · 37 links at zoom 1.0 (was 68 nodes as 7 cluster bubbles at 0.64);
    big repository (2,882 statements): double-click Org 0 → "Added 20 linked nodes.", 2,000 statements → 393 nodes drawn, no console errors

M6 evidence (2026-10-14):

    $ node --experimental-strip-types --test src/test/frontend/graph-explorer/query-fixtures-current.test.mjs   # before the export
    error: 'shacl-ontolex.rq is missing'   # pass 0  # fail 1
    $ npm run fixtures:queries && node ... query-fixtures-current.test.mjs
    wrote 14 query fixtures ...  # pass 1  # fail 0
    $ python3 .codex/skills/mvnf/scripts/mvnf.py GraphExplorerQueryTest
    Summary: tests=14, failures=0, errors=0, skipped=0, time=0.516s
    $ cd e2e && RDF4J_SERVER_BASE_URL=... RDF4J_WORKBENCH_BASE_URL=... npx playwright test --project=chromium tests/workbench-graph-explorer.spec.js tests/workbench-graph-explorer-visual.spec.js
    7 passed

M5 evidence (2026-10-14):

    $ node scripts/css-inventory.mjs --print
    used: 156, defined: 177, unscoped selectors: 0
    used but undefined: (none outside workbench-*)
    $ node --experimental-strip-types --test src/test/frontend/graph-explorer/css-inventory.test.mjs
    # pass 3  # fail 0          (red before: 2 failures, 900 unscoped selectors)
    browser (light): .graph-explorer background rgb(245,249,251) == --workbench-canvas #f5f9fb, font system-ui, no .topbar, no horizontal overflow at 1024px
    browser (dark):  html[data-theme=dark] -> --graph-explorer-literal-fill #43391f, node rect computed rgb(67,57,31)

M4 evidence (2026-10-14):

    $ cd tools/workbench && npm run typecheck      # clean
    $ npm test
    # tests 127  # pass 127  # fail 0
    $ npm run build:explorer
    scripts/graph-explorer/graph-explorer.mjs 367 kB, workers/graph-layout-worker 771 kB, workers/graph-layout-graphviz 1.41 MB (lazy), styles/graph-explorer.css 61 kB
    browser (server-boot, repository "demo", 23 triples): heading "18 nodes · 23 relations · complete batch", no status error after the webcola fix,
    Explore -> Graph round trip: islands 0 -> 1, surface gains/loses workbench-page-surface--full-bleed, no console errors

M3 evidence (2026-10-14):

    $ cd e2e && node --test tests-unit/workbench-graph-route.test.js     # before the TS changes
    # pass 0  # fail 5   (graph route registered: undefined)
    $ cd tools/workbench && npm run build:classic && cd ../../e2e && npm run test:unit   # after
    # tests 749  # pass 749  # fail 0

M2 evidence (2026-10-14):

    $ python3 .codex/skills/mvnf/scripts/mvnf.py GraphServletTest      # skeleton servlet answering 501
    Summary: tests=9, failures=9, errors=0
    - postSparqlQueryBodyStreamsSparqlResultsJson: expected: 200 but was: 501 (and 8 more)
    $ python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench -- -Dtest='GraphServletTest,WorkbenchPagePolicyTest,WorkbenchHtmlShellTest,WorkbenchDeploymentDescriptorTest,WorkbenchFrontendBuildTest'
    Summary: tests=40, failures=0, errors=0, skipped=0, time=0.987s
    $ python3 .codex/skills/mvnf/scripts/mvnf.py WorkbenchPolicyBootPackagingTest
    Summary: tests=10, failures=0, errors=0, skipped=0, time=2.837s

M1 evidence (2026-10-14):

    $ cd e2e && node --test tests-unit/workbench-scripts.test.js        # before workbench-scripts.js existed
    # pass 0  # fail 1
    $ python3 .codex/skills/mvnf/scripts/mvnf.py WorkbenchPolicyBootPackagingTest#workbenchScriptsComeFromTheBuiltWebapp
    failure: expected: 200 OK but was: 404 NOT_FOUND            # before server-boot unpacked the WAR
    $ cd e2e && npm run test:unit                                 # after
    # tests 744  # pass 744  # fail 0
    $ npm run test:unit:coverage  ->  all files | 58.00 | 77.17 | 63.14  (floors 55/70/60)
    $ python3 .codex/skills/mvnf/scripts/mvnf.py WorkbenchPolicyBootPackagingTest
    Summary: tests=9, failures=0, errors=0, skipped=0, time=3.874s

M0 evidence (2026-10-14):

    $ python3 .codex/skills/mvnf/scripts/mvnf.py WorkbenchFrontendBuildTest   # before the pom changes
    Summary: tests=4, failures=3, errors=1
    $ python3 .codex/skills/mvnf/scripts/mvnf.py WorkbenchFrontendBuildTest   # after
    Summary: tests=4, failures=0, errors=0, skipped=0, time=0.061s
    $ cd tools/workbench && npm run build && node scripts/verify-classic-output.mjs
    classic output identical (42 files)
    $ rm -rf tools/workbench/node_modules; mvn -o ... -pl tools/workbench -Pquick clean install   (node removed from PATH)
    [INFO] --- frontend:2.0.2:install-node-and-npm ... (cached tarball .m2_repo/com/github/eirslett/node/22.23.3/)
    [INFO] Running 'npm ci --prefer-offline --no-audit --no-fund' ... added 24 packages in 713ms
    [INFO] BUILD SUCCESS  Total time: 12.493 s
    $ unzip -l tools/workbench/target/rdf4j-workbench.war | grep -E 'graph-explorer.mjs|workbenchApp.js'
    scripts/graph-explorer/graph-explorer.mjs, scripts/workbenchApp.js present

## Interfaces and Dependencies

- `tools/workbench/package.json` scripts: `build`, `build:classic`, `build:explorer`, `dev`, `typecheck`, `test`.
- Browser contract: `window.workbench.graphExplorer.mount(outlet: HTMLElement, ctx: RouteMountContext): () => void` where `ctx.context.basePath`, `ctx.context.repositoryId`, `ctx.model` are provided by `workbenchRoutes.ts`.
- HTTP contract: `POST {basePath}/repositories/{id}/graph`, `Content-Type: application/sparql-query`, `Accept: application/sparql-results+json` → 200 SPARQL JSON | 400 parse/type error (text/plain) | 403 policy | 413 too long.
- Maven: `com.github.eirslett:frontend-maven-plugin:2.0.2`, Node `v22.23.3`; `tools/server-boot` depends on `org.eclipse.rdf4j:rdf4j-http-workbench:war`.
