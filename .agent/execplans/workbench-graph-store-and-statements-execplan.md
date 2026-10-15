# Graph page: a stored graph, "Add to graph" everywhere, statement details, and a split explorer

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root. It builds on `.agent/execplans/workbench-graph-explorer-execplan.md` (milestones M0–M10, which added the Graph page); everything this plan relies on from that work is repeated in "Context and Orientation".

## Purpose / Big Picture

The RDF4J Workbench (the browser UI in `tools/workbench`, served at `/rdf4j-workbench`) has a Graph page at `/rdf4j-workbench/repositories/<id>/graph` that draws a repository's resources as nodes and their links as edges. Today it always starts by running `SELECT ?source ?predicate ?target WHERE { ?source ?predicate ?target } LIMIT 100`, forgets everything when the user leaves the page, cannot be fed from other pages, and cannot show anything about a single statement.

After this work a user can: open the Graph page and find it empty the first time and exactly as they left it afterwards (the graph lives in the browser's IndexedDB, per repository); press "Add to graph" next to any resource on the Explore, Query, Types and Graphs pages (and next to a whole query result) and find those resources, statements or query results on the Graph page; find a node on the canvas by typing its name; see which nodes have more links to load ("+12") and keep expanding until everything is loaded; run CONSTRUCT and DESCRIBE queries in the graph's query editor; download the drawing as SVG or PNG and the loaded statements as Turtle; see places on the Map view when the data uses `geo:lat`/`geo:long`; and click an edge to see the statement it stands for, what is known about its predicate, and cards for every resource that reifies or refers to that statement (classic `rdf:Statement` reification, RDF 1.2 `rdf:reifies` reifiers, and statements whose object is the RDF 1.2 triple term `<<( s p o )>>`).

Developers get a graph explorer split into focused modules instead of one 6,155-line component, with the canvas rendered by memoized node and edge components so dragging, panning and hovering re-render only what changed.

To see it working: build (`npm run build` in `tools/workbench`), run the Spring Boot bundle, create a repository with data, open the Explore page for a resource, press "Add to graph", then open the Graph page.

## Progress

- [x] (2026-10-14 21:40Z) R1 pure helpers out of `GraphExplorerApp.tsx` into `explorer-types.ts`, `explorer-config.ts`, `graph-visuals.ts`, `graph-terms.ts`, `graph-operations.ts`, `load-batches.ts`, `facet-queries.ts`, `graph-literals.ts`, and `revealBoxesIncrementally` into `graph-viewport.ts` (6,155 → 5,235 lines; typecheck clean; `npm test` 148/148).
- [x] (2026-10-14 23:10Z) R2 panels as components: `data-views.tsx`, `filter-panel.tsx` + `use-facet-filters.ts`, `node-details-card.tsx`, `node-menu.tsx`, `side-panels.tsx`, `status-bar.tsx`, `explorer-toolbar.tsx` (data-driven Layout/Tools menus), `query-drawer.tsx`; `clearGraph()` replaces the inline Clear graph code; Cards and Matrix use edge indexes instead of per-cell scans (component 3,590 lines; Playwright 14/14).
- [x] (2026-10-15 00:05Z) R3 canvas: `graph-canvas-view.tsx` (`NodeView`, `EdgeView` memoized; one stable handler object; `stableCurves` keeps unchanged curve objects); dimming by canvas state classes; with WebGL only highlighted/labelled/self-loop edges stay in the SVG; WebGL colours premultiplied (light-mode edges were invisible); measured −41% main-thread time per drag step, −48% per hover (see Artifacts).
- [x] (2026-10-15 04:30Z) R4 hooks: undo/redo in `history-stack.ts` (pure, tested) + `use-undo-history.ts` (stable `push`/`undo`/`redo`, so the keyboard listener no longer re-subscribes on every change); storage in `use-stored-workspace.ts` (F1); statement data in `use-statement-details.ts` (F7). The layout effect stays in the component: it shares a dozen refs with dragging, the WebGL layer and the incremental reveal, and moving it would be a rewrite rather than a split (component 3,750 lines with all features).
- [x] (2026-10-15 01:10Z) F1 stored graph: `use-stored-workspace.ts` restores on mount and saves throttled (800 ms, deferred while the layout animates or a node is dragged, flushed on hide/unmount); `workspace-storage.ts` chains saves so a read never sees a stale record; the page starts empty ("Load 100 statements", query editor); Save/Restore workspace, Start over and Ctrl+S removed. "Add to graph": classic `graphAdditions.ts` queue + buttons on result-table IRIs, the Query page toolbar, the Explore card, Types and Graphs rows; island `graph-additions.ts` drains it on mount and on `storage` events (resources expand, statements merge, queries run). Playwright `workbench-graph-explorer-store.spec.js` 3/3; the four older graph specs now press "Load 100 statements" (16/16).
- [x] (2026-10-15 01:40Z) F2 find: `graph-find.ts` (`findNodes`, ranked) + `find-field.tsx` combobox in the canvas's top-left corner (the toolbar row had no room at 1440 px); Enter shows the first match's details and pans to it; the last option searches the repository and adds what it finds (the query drawer's separate search is gone). The repository search now matches labels that contain the term, exact first (`search-partial` fixture, red 0 → green 6). "/" focuses the field; Ctrl+Z in a text field no longer undoes the graph.
- [x] (2026-10-15 02:30Z) F3 more to load: `linkCountQuery` (per node, capped at 1,000 index steps, types and values left out, matching the canvas) via `RdfExplorer.linkCounts` (cached 5 min) and `link-counts.ts` (`loadedLinkCounts`, `loadedLinksOf`, `moreLinks`) draw a "+N" badge on nodes (all nodes up to 150 drawn, else the active/selected ones); the badge expands. `expandQuery(..., loaded)` subtracts loaded statements with `MINUS { VALUES … }`, so expanding again pages forward. Fixtures `links`, `links-capped`, `expand-more` + `GraphExplorerQueryTest#linkCountsLeaveOutTypesAndValuesAndStopAtTheCap` (red: fixture missing → green 21/21); Playwright `workbench-graph-explorer-paging.spec.js` (+12 → 90 links → "no more links").
- [x] (2026-10-14) F4 CONSTRUCT and DESCRIBE: `GraphServlet` prepares graph queries and writes their statements as `?source ?predicate ?target` rows (`GraphServletTest` 11/11: construct, describe, ASK rejected); proven end to end by the store spec's Query-page CONSTRUCT test.
- [x] (2026-10-15 03:00Z) F5 export: `graph-export.ts` (`graphTurtle` of every loaded statement, grouped by subject with the usual prefixes; `graphSvg` builds a standalone SVG from the drawn graph and its curves, so WebGL-drawn edges are included, colours resolved from CSS; `svgToPng` rasterizes it at 2x within canvas limits); Tools → Download Turtle / SVG image / PNG image. Node tests 3/3; `GraphExplorerQueryTest#theGraphExportIsTurtleThatRioReadsBackStatementForStatement` parses the generated `graph-export.ttl` with Rio (red: fixture missing → green 22/22); Playwright downloads all three.
- [x] (2026-10-15 03:20Z) F6 Map: `graph-literals.ts` `placesOf` (WKT points; geo:lat/geo:long; schema:latitude/longitude, http and https; coordinates on a schema:geo or geo:location node placed on its owner; out-of-range values dropped; one place per resource) and `mapFrame` (bounds around every place, 10 % margin, ≥0.2°); the OSM embed frames all places; the empty text no longer points at Wikidata. Node tests 4/4; Playwright `workbench-graph-explorer-map.spec.js`.
- [x] (2026-10-15 04:10Z) F7 statements: `edge-hit.ts` (`nearestEdge` over the quadratic curves, 7 CSS px) picks the edge under a background click and under the mouse (hover highlight + pointer cursor, also for edges the WebGL layer draws); `statement-details.ts` (`statementAnnotationsQuery`: RDF 1.2 reifiers `?r rdf:reifies <<( s p o )>>`, classic `rdf:Statement` reifications, references to the triple term, with a classic-only fallback remembered per endpoint; `predicateInfoQuery`, `predicateUsageQuery`), `use-statement-details.ts` and `statement-details-card.tsx` (predicate description/domain/range/usage, one card per reifier, reification and reference, each with Add to graph; blank-node reifiers add their own statements). `addToGraph` is now shared by the Workbench queue and the cards. Java fixtures over RDF 1.2 Turtle (`~` reifier, `{| |}` annotation, `rdf:Statement`, `<<( )>>` reference): red (fixtures missing) → green 27/27; Node 3/3 + 3/3; Playwright `workbench-graph-explorer-statements.spec.js` (click 4 px beside the edge → four cards → Add Carol).
- [x] (2026-10-15 04:50Z) Final: docs (`server-workbench.md` Graph section, `tools/workbench/README.md`, `STYLE-MAP.md`); `npm test` 175/175; e2e unit 756/756; Playwright graph specs 22/22 (chromium); `mvnf tools/workbench` 1,004 tests, 0 failures.

## Surprises & Discoveries

- Observation: this branch models RDF 1.2, not the older RDF-star. The value type is `org.eclipse.rdf4j.model.TripleTerm` (`core/model-api/src/main/java/org/eclipse/rdf4j/model/TripleTerm.java`); `<< s p o >>` in SPARQL and Turtle means a reified triple (a reifier node with `rdf:reifies <<( s p o )>>`), and `<<( s p o )>>` is the triple term itself, allowed only in object position. The SPARQL grammar (`core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast/sparql.jjt`, `TripleTerm()` and `ReifiedTriple()`) is always on; W3C SPARQL 1.2 eval tests pass on Memory, Native and LMDB stores (`testsuites/sparql/src/main/resources/testcases-sparql-1.2-w3c/eval-triple-terms`).
  Evidence: `basic-2.rq` `<<:a :b :c>> ?p ?o` over `<<:a :b :c>> :q :z .` returns `?p=rdf:reifies ?o=<<(:a :b :c)>>` and `?p=:q ?o=:z`.
- Observation: SPARQL Results JSON writes a triple term as `{"type":"triple","value":{"subject":…,"predicate":…,"object":…}}` by default (`SPARQLResultsJSONWriter.java:55-78`; JSON declares `SUPPORTS_TRIPLE_TERMS`), so the browser receives triple terms as nested terms.
- Observation: in headless Chromium the scripted drag is dominated by software rasterization: 40 steps take ~9 s whether or not node drop shadows or transitions are on, while the main thread is busy only ~0.6 s; main-thread task time (CDP `Performance.getMetrics`: TaskDuration, ScriptDuration, RecalcStyleDuration) is the comparable measure.
  Evidence: `NOFILTER=1 NOTRANSITION=1` drag 8.8–10.5 s vs 8.9–10.6 s; profile 6.1 s idle of 6.7 s.
- Observation: the hybrid WebGL edge layer drew unpremultiplied colours into a premultiplied-alpha canvas, so light-theme edges (#b7c6cf at alpha 0.7) composited as white; the 8 %-opacity SVG copy of every edge had been hiding it.
- Observation: the Workbench result table (`tools/workbench/src/main/webapp/scripts/ts/queryStream.ts`, class `ResultTable`, method `renderTerm` near line 5655) renders every term cell of the Query page and of the Explore, Export-preview and Remove-preview tables; there is no per-cell menu, only an external "↗" link after IRIs. The Query page builds its `ResultTable` without a repository id.

- Observation: the debounced autosave never fired after "Add to graph": the incremental layout and reveal animations change the graph and transform every frame for well over the debounce interval, and IndexedDB writes started in `pagehide` do not finish, so a reload lost the graph while an in-app route change (which saves on unmount) kept it.
  Evidence: store spec "stays after a reload" failed with the graph missing after `page.reload()`; passing after switching to a throttle that waits for `busy` to clear.
- Observation: the repository entity search matched exact labels only (`LCASE(STR(?label)) = LCASE(term)`), so typing a partial name found nothing.

## Decision Log

- Decision: the graph is stored in IndexedDB (database `graph-field-workspaces`, store `workspaces`, key `workspace:<basePath>|<repositoryId>`, the record the old manual "Save workspace" used) and saved automatically, debounced, after every change; "Save workspace"/"Restore workspace" disappear.
  Rationale: the user asked for the graph to live in the browser database; reusing the existing record keeps graphs users already saved.
  Date/Author: 2026-10-14, user request + Claude.
- Decision: other pages hand additions to the Graph page through a small queue in `localStorage` (key `rdf4j-workbench-graph-additions:<basePath>|<repositoryId>`, a JSON array of `{kind:"resource", term}`, `{kind:"statement", subject, predicate, object}` and `{kind:"query", query}` with N-Triples terms), which the Graph page drains when it mounts and whenever a `storage` event reports a change; the graph itself stays in IndexedDB, written only by the Graph page.
  Rationale: the classic Workbench scripts are global (non-module) TypeScript compiled separately from the React island, so they cannot import the island's storage code; `localStorage` is synchronous, small and testable in the Node `vm` harness, and its `storage` event lets an open Graph tab pick additions up live. One writer per store avoids two code bases racing on the IndexedDB record.
  Date/Author: 2026-10-14, Claude.
- Decision: "Add to graph" for a resource loads the resource's neighbourhood with the same query as Expand (both directions, 80 per direction); for a statement it adds exactly that statement; for a query it runs the query on the Graph page and merges the result.
  Date/Author: 2026-10-14, Claude.
- Decision: the refactor (R1–R4) comes before the features, so the features are written into the new modules; each refactor step keeps behaviour identical and is checked with typecheck, `npm test` and the Playwright graph specs.
  Date/Author: 2026-10-14, user request ("refactor and split out code … to reduce duplication and bugs and also to improve performance").

- Decision: Find lives on the canvas (top-left, the selection bar moves below it) instead of in the toolbar.
  Rationale: at 1440 px with the Workbench sidebar the toolbar has about 60 px to spare; a field there wrapped the actions onto a second row.
  Date/Author: 2026-10-15, Claude.
- Decision: a resource added from another page is focused (highlighted) on the canvas but its details card is not opened; the user has just seen those details on Explore.
  Date/Author: 2026-10-15, Claude.

## Outcomes & Retrospective

Everything asked for is in: the graph lives in the browser's database and the page starts empty; "Add to graph" is
available on Explore, result tables, the Query page, Types and Graphs; Find (4), more-to-load badges with paging
Expand (5), CONSTRUCT/DESCRIBE (6), Turtle/SVG/PNG export (7), the Map from lat/long (8) and the statement card with
predicate information and reifier/reification/reference cards (9). The refactor took `GraphExplorerApp.tsx` from
6,155 to about 3,500 lines before the features (3,750 after them) and cut main-thread time per drag step by 41 %.
Left open: the layout effect is still in the component; Find and edge picking do not apply to the 500+-node canvas
(`LargeGraphCanvas`); several Graph tabs on the same repository save over each other (last write wins); the work is
uncommitted on `GH-0000-workbench-graph-explorer` (no issue number given).

## Context and Orientation

The Workbench front end has two parts. The classic pages are lit-html TypeScript under `tools/workbench/src/main/webapp/scripts/ts/` (global scripts sharing one `workbench` namespace, compiled by `tsc -p tsconfig.classic.json` into `tools/workbench/target/frontend/scripts`). The Graph page is a React application ("the island") under `tools/workbench/src/main/frontend/graph-explorer/`, bundled by Vite into `tools/workbench/target/frontend/scripts/graph-explorer/graph-explorer.mjs`. `npm run build` in `tools/workbench` builds both; Maven runs it through `frontend-maven-plugin`. `main.tsx` mounts the island when the Workbench route `graph` (registered in `scripts/ts/workbenchRoutes.ts`) is shown.

The island's root component is `GraphExplorerApp.tsx`. It owns the graph model (`graph: Graph`, nodes and edges with `revision`), the canvas transform, selection, filters, panels and the layout job. Terms used below: a "node" is an RDF term drawn on the canvas (IRI, blank node, or literal when "Values" is on); the "drawn graph" is `canvasGraph(graph, showValues)` from `graph-canvas-scope.ts` (no literal nodes and no `rdf:type` links unless "Values" is on); the "details card" is the overlay that shows the focused node (`node-details.ts` computes its content); "Expand" loads a node's neighbours with `expandQuery` from `rdf-vocabulary.ts` through `RdfExplorer.expand` (`rdf-explorer.ts`), which batches per-node queries; the "filter model" is `facetModel` in `graph-facets.ts`.

All SPARQL the island sends goes to `GraphServlet` (`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/GraphServlet.java`) at `POST <basePath>/repositories/<id>/graph` with body `application/sparql-query`; it accepts only SELECT today and answers SPARQL Results JSON. `GraphServletTest` covers it. The generated SPARQL of the island is pinned by committed fixtures under `tools/workbench/src/test/resources/graph-explorer-queries` (written by `npm run fixtures:queries`, checked by `query-fixtures-current.test.mjs`) and executed on a MemoryStore by `GraphExplorerQueryTest`.

Tests: `npm test` in `tools/workbench` runs the island's unit tests (`src/test/frontend/graph-explorer/*.test.mjs`); `npm run typecheck` type-checks the island; `cd e2e && npm run test:unit` runs the classic scripts' Node tests (they read the compiled scripts from `tools/workbench/target/frontend/scripts`, so build first); Playwright specs `e2e/tests/workbench-graph-explorer*.spec.js` run against a server with `RDF4J_SERVER_BASE_URL=http://localhost:8080/rdf4j-server RDF4J_WORKBENCH_BASE_URL=http://localhost:8080/rdf4j-workbench npx playwright test --project=chromium tests/workbench-graph-explorer*.spec.js`; Java tests run with `python3 .codex/skills/mvnf/scripts/mvnf.py <TestClass>` (never `-am`, never `-q`).

## Plan of Work

R1 (done) moved the pure helpers into modules named for what they hold. R2 turns the large JSX regions of `GraphExplorerApp.tsx` into components with explicit props: `data-views.tsx` (Triples, Cards, Matrix, Timeline, Map, Chart; their derived data is computed inside, so it is only computed while a view is shown, and the per-cell `graph.edges.filter` scans of Cards and Matrix become one index), `filter-panel.tsx`, `node-details-card.tsx`, `node-menu.tsx`, `side-panels.tsx` (paths and groups, load history), `status-bar.tsx`, `explorer-toolbar.tsx` and `query-drawer.tsx`. Repeated statement sequences become single functions: `clearSelection()`, `closeSidePanels(except?)`, `resetGraph(next, options)` (used by clear graph, stress tests, query replace and restore), and one `toggleNodeInSelection(id)`. R3 moves the SVG canvas into `graph-canvas-view.tsx` with `NodeView` and `EdgeView` wrapped in `React.memo`; their event handlers come from one stable handler object (a ref-backed `useMemo`) so a re-render of the app does not re-render unchanged nodes; the measured scenario is dragging a node of the 393-node graph (see Validation). R4 moves undo/redo into `use-graph-history.ts` and the layout effect into `use-graph-layout.ts`.

F1 replaces the manual workspace with `use-graph-store.ts`: on mount it loads the stored record (`loadWorkspaceText` in `workspace-storage.ts`, validated by `parseWorkspacePayload` in `workspace.ts`), applies it, then drains the additions queue (`graph-additions.ts`, shared format with the classic writer `scripts/ts/graphAdditions.ts`); it saves the graph, transform, provenance, groups and focus 800 ms after the last change. No default query runs; the empty state offers a search field, the query editor and "Load the first 100 statements". The classic side adds `workbench.graphAdditions.add(context, entry)` and buttons: in `ResultTable.renderTerm` an icon button after IRI and blank-node terms (only when the table knows its repository and the Graph page is enabled), in the Explore resource card, in Types/Graphs name cells, and "Add results to graph" on the Query page result toolbar for CONSTRUCT/DESCRIBE and for SELECT results with `?source ?predicate ?target`-style columns.

F2 adds a find field at the top left of the canvas: matching loaded nodes by label and IRI as you type (arrow keys and Enter), Enter on a match focuses and reveals it; the last row "Search the repository for “…”" runs the entity search and adds (does not replace). F3 adds `degreeQuery(ids, scope, fence)` to `rdf-vocabulary.ts` (per-node capped counts of links in both directions, links meaning non-literal objects other than `rdf:type`, and of all statements), `RdfExplorer.degrees(ids)`, and a "+N" badge on drawn IRI nodes whose repository count exceeds what is loaded; `expandQuery` gains per-node exclusions (`MINUS { VALUES (?predicate ?target) {…} }`) so a second Expand loads the next links. F4 lets `GraphServlet` run graph queries and answer them as SPARQL Results JSON with variables `source`, `predicate`, `target`. F5 adds `graph-export.ts`: an SVG with computed styles inlined and a viewBox around the whole graph, a PNG rendered from that SVG (or from the large-graph canvas), and Turtle written from the loaded statements. F6 reads `geo:lat`/`geo:long` (and schema.org latitude/longitude) pairs per resource in `graph-literals.ts` and shows them on the Map with a bounding box around all points. F7 makes edges clickable (a wide transparent hit path), sets a focused edge, and shows `statement-details-card.tsx`: subject → predicate → object, the predicate's own description (label, comment, domain, range, super-properties), and cards for reifiers (`?r rdf:reifies <<( S P O )>>`), classic reifications (`?r rdf:subject S; rdf:predicate P; rdf:object O`) and direct references (`?x ?p <<( S P O )>>` other than `rdf:reifies`), each card listing its statements and offering "Add to graph".

## Concrete Steps

Working directory `tools/workbench` unless stated.

    npm run typecheck
    npm test                                  # island unit tests
    npm run fixtures:queries                  # after changing generated SPARQL
    npm run build
    cd ../../e2e && npm run test:unit         # classic scripts
    # with server-boot running on 8080:
    RDF4J_SERVER_BASE_URL=http://localhost:8080/rdf4j-server RDF4J_WORKBENCH_BASE_URL=http://localhost:8080/rdf4j-workbench npx playwright test --project=chromium tests/workbench-graph-explorer*.spec.js
    cd .. && python3 .codex/skills/mvnf/scripts/mvnf.py GraphServletTest
    python3 .codex/skills/mvnf/scripts/mvnf.py GraphExplorerQueryTest

## Validation and Acceptance

Refactor: after each of R1–R4, typecheck is clean, `npm test` passes, and the 14 Playwright graph specs pass. R3 is accepted when a scripted drag of a node in a 393-node graph (scratch Playwright script, 30 pointer moves, time per move until the next animation frame) is faster than before; the numbers go into Artifacts.

Features, each with a test that fails before and passes after: F1 — open the Graph page of a fresh repository: no node is drawn and the empty state shows; press "Add to graph" on Explore for a resource, open Graph: the resource and its neighbours are drawn; reload: the same graph is back (Playwright). F2 — typing "Ali" in the find field lists "Alice", Enter focuses it (Playwright). F3 — a node with 100 outgoing links shows "+20" after an 80-link expansion and a second Expand loads the rest (unit test of `degreeQuery`/exclusions via fixtures and `GraphExplorerQueryTest`; Playwright). F4 — `GraphServletTest` CONSTRUCT and DESCRIBE cases return SPARQL JSON with `source/predicate/target`. F5 — `graph-export.test.mjs` serialises literals with language and datatype and IRIs to Turtle; Playwright downloads an `.svg` containing every node label. F6 — `graph-literals.test.mjs` pairs lat/long per resource; the Map view of the demo lists the four cities. F7 — loading `ex:a ex:p ex:b . << ex:a ex:p ex:b ~ ex:r >> ex:source ex:wiki . _:x rdf:subject ex:a ; rdf:predicate ex:p ; rdf:object ex:b ; ex:note "old style" .` and clicking the `ex:p` edge shows a reifier card for `ex:r` with "source wiki" and a reification card with "note old style" (Playwright); the generated queries are fixtures run by `GraphExplorerQueryTest`.

## Idempotence and Recovery

All steps are additive edits under `tools/workbench` and `e2e`; rerunning builds and tests is safe. The stored graph can be cleared from the Graph page (Tools → Clear graph) or by deleting the IndexedDB database `graph-field-workspaces` in the browser's developer tools. A backup of `GraphExplorerApp.tsx` before R1 is in the session scratchpad only; git (`git diff`) is the recovery path for everything else.

## Artifacts and Notes

R1 (2026-10-14):

    $ npx tsc -p tsconfig.json --noEmit     # after moving 73 top-level declarations and pruning imports
    (no output)
    $ npm test
    # pass 148  # fail 0
    $ wc -l src/main/frontend/graph-explorer/GraphExplorerApp.tsx
    5235

R3 main-thread time (CDP metrics, 25 drag steps / 25 hover moves on the 393-node graph of the generated "big" repository, two runs each, old = the pre-R1 component with its own CSS built from the backup):

    before refactor   drag Task 0.628/0.633 s  Script 0.302/0.294  Style 0.060/0.062   hover Task 0.537/0.584  Script 0.078/0.088  Style 0.133/0.127
    after R3          drag Task 0.371/0.375 s  Script 0.199/0.204  Style 0.025/0.030   hover Task 0.283/0.291  Script 0.031/0.032  Style 0.065/0.066
    DOM elements      10,005 → 4,414

## Interfaces and Dependencies

No new npm or Maven dependencies. New island modules (all under `tools/workbench/src/main/frontend/graph-explorer/`): `graph-additions.ts` (`readAdditions(scope)`, `takeAdditions(scope)`, `AdditionEntry`), `use-graph-store.ts`, `graph-export.ts` (`graphToTurtle(graph)`, `svgSnapshot(svg, graph)`), `statement-details.ts` (`statementQueries(edge)` returning the SPARQL for predicate info, reifiers, reifications and references, and `statementCards(rows)`), `statement-details-card.tsx`, plus the components named in R2/R3. Classic: `tools/workbench/src/main/webapp/scripts/ts/graphAdditions.ts` exposing `workbench.graphAdditions.add(context: {basePath: string; repositoryId: string}, entry)`; it must be listed in the scripts the routes load (see `workbenchRoutes.ts`) wherever a page renders "Add to graph". Server: `GraphServlet` accepts `ParsedGraphQuery` (CONSTRUCT and DESCRIBE) in addition to `ParsedTupleQuery`.
