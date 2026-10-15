# Graph page refinement: every feature in every combination

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. It follows `.agent/PLANS.md`.

## Purpose / Big Picture

The Graph page of the RDF4J Workbench (`tools/workbench`, React island under
`src/main/frontend/graph-explorer`) gained many features in `.agent/execplans/workbench-graph-store-and-statements-execplan.md`:
a graph kept in the browser, an empty start, "Add to graph" from the other pages, Find, "+N" more-to-load badges,
CONSTRUCT/DESCRIBE, Turtle/SVG/PNG export, a Map from coordinates, and a statement card for edges. Each was tested on
its own. This plan makes sure they work together: in the three render modes (SVG below 180 drawn nodes, SVG + WebGL
edges from 180, the canvas renderer from 500), in every data view, with filters, Values, labels, groups, hiding,
pinning, undo/redo and history, after a reload, in light and dark, at phone width, with mouse, touch and keyboard, and
with RDF 1.2 data (triple terms, reifiers). Done means a person can combine any of these and nothing is lost, wrong,
stale, unreachable or broken, and the Playwright and unit suites prove the combinations that matter.

## Progress

- [x] (2026-10-15 05:40Z) C0 combination audit (browser part; code review merged below when it reports): matrix below exercised in the browser and by code review; defects listed in
  `Surprises & Discoveries` with a failure scenario each.
- [x] (2026-10-15 07:30Z) C1 first batch fixed, each red → green: D1 (edges picked on pointer-up), D2 + D18 + R10
  (`linkCountQuery` counts DISTINCT drawable links; usage, expand, card and predicate queries DISTINCT; Java fixtures
  `links-triple-terms`, `links-all-graphs`, `predicate-usage-all-graphs`), D3 (`blank-labels.ts`), D7, D8, D10, D11,
  D13 (`chart-data.ts`, per-predicate chart), D14 (`parseGraphDate`), D16 (`termCode`), D17 (scope in the payload),
  R1 (empty and long values restore; no save right after opening; unreadable record kept aside), R2 (replace keeps
  state until it succeeds; empty results change nothing), R3 (`EXCLUSION_BUDGET`), R4 (hide/pin/Values/appended
  results keep positions), R5, R6 + D12 (queue only when idle and visible, hand-back on interruption, newer graph
  from another tab on return), R8, R9, R11, R12, R13, R14, R15, facet counts scoped and language-aware, statement
  description in the preferred language, the blank-node note, "/" from every view, export errors and large extents.
  Playwright `workbench-graph-explorer-combinations.spec.js` 10/10 (twice); unit 188/188; Java 31/31.
- [x] (2026-10-15 09:40Z) User report "dragging the background lags 50–200 px behind the mouse": per-node SVG drop
  shadows were repainted every frame (432 nodes with a card open: 66 ms frames, 15 fps; without shadows 16.7 ms).
  While the view moves (`data-moving` on the canvas, set by `applyInteractiveTransform`, cleared on commit) or a node
  is dragged (`data-dragging`), shadows and overlay backdrop blur are off; above 150 drawn nodes shadows are off
  altogether (`SHADOW_NODE_LIMIT`). Measured 16.7 ms frames in every case; spec "keeps up with the pointer".
- [x] (2026-10-15 11:30Z) C1 second batch: statement access from the node card (a "Statement: …" button per value and
  link, so keyboard, the canvas renderer and Values off all reach the statement card; D5), phone reveal (D15:
  `visibleArea`/`revealPoint` keep the node clear of the bottom sheet), a row cap for queued queries (R7:
  `QUEUED_QUERY_ROW_LIMIT`, sent as `row-limit`), RDF 1.2 reification on the canvas (D4: `statement-links.ts`, dashed
  connectors from a reifier or referrer to the middle of the statement's edge, kept in the stored graph and in Turtle
  export as `<<( … )>>`). Found on the way: the servlet's `limit` parameter was read through
  `WorkbenchRequest.getParameter`, which falls back to the Query page's "limit" cookie (default 100) and silently
  capped every graph query; renamed `row-limit` and read from the raw request (GraphServletTest red → green).
- [x] (2026-10-15 11:30Z) C2 details: selection-scoped data views, a chart per numeric predicate, the Triples view
  lists statement links, blank-node names everywhere, types filled in from loaded `rdf:type` statements.
- [x] (2026-10-15 11:30Z) C3 combination specs: `workbench-graph-explorer-combinations.spec.js` (14 tests) plus the
  statements spec's on-line/label click test.
- [x] (2026-10-15 14:10Z) S1 scale (user request "Graph needs easily handle 50000 nodes and 200000 edges"): measured on
  repository `scale` (50,000 typed, labelled nodes; 200,000 links; 300,000 statements) with
  `audit/scale-interact.js` and `audit/scale-export.js`; results under `Artifacts and Notes`.
- [x] (2026-10-15 15:00Z) Final: docs (site `server-workbench.md` Graph section, `tools/workbench/README.md`,
  `STYLE-MAP.md`), explorer unit tests 206/206, classic scripts 756/756, graph Playwright specs 37/37 (chromium; the
  store spec now checks the stored label in the compact form), module verify `mvnf tools/workbench` 1,010 tests, 0
  failures (GraphExplorerQueryTest 31, GraphServletTest 13).

## Combination matrix

Features (rows) are checked against conditions (columns). A cell is checked when it has been exercised.

Features: F-store (stored graph), F-empty, F-add (Add to graph: resource, query, Types, Graphs, result-table IRI),
F-find, F-more ("+N" and paging Expand), F-construct, F-export (Turtle, SVG, PNG), F-map, F-statement (edge hover,
click, card, card add), plus the older ones: filters, Values, Labels, layout menu, groups, paths, load history, node
menu, hide, pin, undo/redo, data views (Triples, Cards, Matrix, Timeline, Map, Chart), query drawer, label languages
and graph scope, full screen.

Conditions: render mode (SVG / WebGL edges / canvas renderer), Values on/off, filters active, a node or edge focused,
nodes hidden or pinned, after undo, after reload, another tab adding, light/dark, 1440 / 1024 / 390 px, mouse /
touch / keyboard, RDF 1.2 data (reifiers, triple terms), blank nodes, literals, named-graph scope, empty repository,
query running.

## Surprises & Discoveries

Browser audit (scripts and screenshots in the session scratchpad `audit/`; data: repository `combo` with people,
organisations, places with coordinates, dates, a named graph, blank nodes, an RDF 1.2 named reifier, an annotation
reifier, a classic `rdf:Statement`, a reference to a triple term and a 90-link hub; `big` (432 nodes, WebGL edges);
`huge` (1,200 nodes, canvas renderer)):

- D1 (fixed) A click on an edge's line or label never opened the statement card: the canvas captures the pointer on
  press, so the browser retargets the click to the `<svg>` and the edge's `onClick` never runs; only clicks beside an
  edge (picked by hit-testing) worked. Evidence: "a click on the line of a link, or on its label" failed with the card
  missing; now the canvas picks the pressed edge (`data-edge-id`) or the nearest curve on pointer-up.
- D2 "+N" counts links to triple terms (a reifier's `rdf:reifies`, `ex:doubts <<( … )>>`), which the graph never
  draws, so "Claim 1 +1" and "Dave +1" can never be cleared.
- D3 Blank nodes are labelled with their raw ids ("3ca806925c26772c") on the canvas, in cards, Find and the matrix.
- D4 Triple-term objects are dropped silently: reifier and referrer nodes float unconnected, "Loaded 97 statements"
  for 100 rows, and a reifier's card cannot say what it reifies.
- D5 The statement card cannot be reached with the keyboard, nor at all in the canvas renderer (≥500 nodes).
- D7 The Labels toggle is not kept across a reload (Values is).
- D8 After a reload the restored focused node dims the whole graph while its card is closed.
- D10 "Load 1 more links" / "1 more links." (badge title and node name).
- D11 "Clear graph" is enabled on an empty graph and records an empty undo step.
- D12 An addition from another tab aborts a query or expansion running in this one (`invalidateAsyncRequests`): a
  150-statement query was lost, only Dave's statement arrived.
- D13 The Chart pools every numeric value (ages, coordinates, a year) into one meaningless histogram.
- D14 The Timeline reads the integer 2001 (`ex:since 2001`) as a date.
- D15 At phone width Find → Enter pans the node under the bottom-sheet card (the reveal assumes a card on the right).
- D16 The Triples view shows internal literal ids (`__rdf_literal__:{"type":…`) as the term's code line.
- D17 The graph scope (default / all / a named graph) is not kept with the stored graph.
- D18 In the "all graphs" scope a statement of a named graph is counted twice by `linkCountQuery` (default graph
  plus `GRAPH ?g`) and by `predicateUsageQuery`, so "+1" never clears there either.
- Code review (subagent, verified finding by finding): R1 an empty or >16 KB value made the whole stored graph
  unreadable, and the next autosave wrote the empty page over it; R2 a replacing query cleared groups, selection and
  focus before running (Undo could not restore them) and an empty result wiped the graph without Undo; R3 paged
  Expand built queries of millions of characters (8,794,027 for 40 expanded nodes) against the 150,000 limit; R4
  hide, pin, Values and appended results re-ran the whole layout; R5 an interrupted expansion left "Expanding…"
  stuck; R6 additions aborted running work, were lost when interrupted, and a hidden tab took them; R7 a queued Query
  page query has no row limit; R8 "Show in graph" kept the statement card; R9 exports from other views came out grey;
  R10 the "all graphs" scope doubled expand rows, cards and usage; R11 one network error disabled triple-term
  lookups; R12 link counts were fetched for the canvas renderer; R13 the statement card overlapped side panels on
  narrow screens; R14 adding a blank `rdf:Statement` card made an unconnected island; R15 "foaf:Person" was searched
  as an IRI.
- Clean: WebGL and canvas modes (Find, node cards, filters, export, undo, reload), dark mode, the Map with
  `schema:geo`, errors for ASK and malformed queries, Types/Graphs/result-table "Add to graph", reload of pins,
  selection, focus, hidden nodes and the viewport.

- Scale (S1), each found by measuring the 50,000-node graph, fixed and re-measured:
  - The stored graph was 120 MB of JSON (every edge repeated its terms, label and batch), over the 64 MB the page
    allows: compact format 2 (`workspace-format.ts`, tables for nodes, predicates, types and batches; an edge is four
    numbers) stores it in 11.5 MB and reads it back in 0.6 s.
  - The Triples view rendered 200,000 rows and hung the tab: it pages by 500 (`TABLE_PAGE`).
  - The Filters panel rendered 116,506 checkboxes: values are listed only for open facets, 12 at a time, with "Show
    N more of M" and a search.
  - Labels were recomputed for every node on every merge; nodes were grey on the canvas because the label pass never
    filled `types` from loaded `rdf:type` statements and the WebGL renderer had one colour for all nodes:
    `withLoadedTypes`, memoized labels, per-node colours (`IndexedGraphGl.colorize`).
  - Layout took 16–20 s with no sign it was running: fCoSE spent 15.3 s of 17.3 s on the quotient graph of 221
    neighbourhoods, which a random graph links nearly all to each other (18,292 links). Keeping each neighbourhood's
    six strongest links (`strongestLinks`) brings the whole layout to 2.2 s (in the worker) with the same picture,
    and the large canvas says "Arranging…" until it is done.
  - Every zoom or pan froze the page for 2 × 200 ms: the viewport is part of the saved payload, so each move
    re-encoded the whole graph. The encoded graph is now kept for the last graph object saved (one entry, so undo
    history does not hold old copies); moving the view costs a 30 ms database write.
  - PNG export of 50,000 nodes takes ~10 s (a 120-megapixel cap, drawn off the main thread); the status line now says
    "Creating the PNG image…" meanwhile.

## Decision Log

- Decision: unsafe stored data (an IRI that could break out of a query) still rejects the whole stored graph, as the
  existing validation tests require; only harmless values (empty labels, long literals) were wrongly rejected.
  Instead of skipping bad nodes, the page keeps an unreadable record under `workspace:<scope>:unreadable` and saves
  nothing until the user changes the graph.
  Date/Author: 2026-10-15, Claude (review finding R1).
- Decision: when two Graph tabs show the same repository, the one in view wins: only a visible tab takes "Add to
  graph" additions, and a tab coming back into view replaces its graph with a newer one saved elsewhere (Undo brings
  its own back). Simultaneous edits in two visible windows still save over each other.
  Date/Author: 2026-10-15, Claude (review finding R6).
- Decision: a replacing query that returns no statements leaves the graph unchanged instead of emptying it.
  Date/Author: 2026-10-15, Claude (review finding R2).

## Outcomes & Retrospective

Every defect of the combination audit has a test that failed before its fix; the features now hold together across
render modes, reloads, other tabs, undo, RDF 1.2 data and phone width. The 50,000-node / 200,000-edge target is met:
every interaction stays under 0.7 s of main-thread work and pan, zoom and hover run at 60 fps. The biggest wins came
from measuring rather than guessing: the drag lag was drop shadows, the 16 s layout was the coarse solver's input
size, and the zoom hitch was the autosave, not React. Left open: PNG export of 50,000 nodes takes ~10 s (120
megapixels); a save after a real graph change at that size still blocks ~0.2 s; two visible windows editing the same
repository save over each other (Decision Log).

## Context and Orientation

The explorer's state lives in `GraphExplorerApp.tsx`; pure helpers and hooks sit beside it (see
`tools/workbench/README.md`, section "The graph explorer"). The test server for browser checks is a server-boot jar
in the session scratchpad (`deploy.sh` copies a fresh `npm run build` into its extracted webapp). Unit tests:
`npm test` in `tools/workbench`; classic scripts: `cd e2e && npm run test:unit`; browser: `cd e2e && npx playwright
test tests/workbench-graph-explorer --project=chromium`; Java: `python3 .codex/skills/mvnf/scripts/mvnf.py
GraphExplorerQueryTest --module tools/workbench`.

## Plan of Work

Audit first (C0), recording each defect with its failure scenario; then fix them test-first (C1), add the missing
details (C2) and the combination specs (C3).

## Concrete Steps

See Progress; commands are those in Context and Orientation.

## Validation and Acceptance

Every defect found in C0 has a test that failed before its fix and passes after; the full suites pass.

## Idempotence and Recovery

All changes are source edits and tests; the scratchpad server can be rebuilt with the module install and restarted.

## Artifacts and Notes

Audit scripts and screenshots are in the session scratchpad (`audit/`). Scale results on the 50,000-node graph after
S1 (Chromium, 1440 × 900; "block" = longest main-thread task):

    load (300,000 statements)   5.4 s to the stats line, longest block 0.66 s
    layout                      ~5 s after the nodes appear (2.2 s in the worker), "Arranging…" meanwhile
    pan / zoom / hover          16.7 ms frames (60 fps), no long tasks
    click node, Find, Enter     ≤ 0.3 s, block ≤ 0.13 s
    filters open / by type      0.58 s / 0.55 s, block ≤ 0.51 s
    Triples / Cards view        0.23 s / 0.13 s
    hide node / undo            0.73 s / 0.70 s
    Values on (100,005 nodes)   block 0.34 s
    Turtle / SVG / PNG export   18.3 MB 0.6 s / 42.8 MB 0.6 s / 144 MB 9.6 s, blocks ≤ 0.53 s
    autosave / reload           11.5 MB, 0.18 s / 0.62 s

## Interfaces and Dependencies

No new dependencies.
