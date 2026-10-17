```md
# Make the Workbench graph explorer's interactions consistent and predictable

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.

Work happens in `/Users/havardottestad/Documents/Programming/rdf4j` on branch `GH-0000-workbench-graph-explorer`, starting at commit `dae434eae4` ("wip"). No GitHub issue number was given, so any commit uses `GH-0000`. Untracked files already present (`.agent/execplans/workbench-graph-*.md`, `.claude/`, `package-lock.json`, `tools/workbench/logs/`) belong to other work and must be left alone.


## Purpose / Big Picture

The Graph page of the RDF4J Workbench (`/rdf4j-workbench/repositories/<id>/graph`) is a React application that draws RDF resources as cards ("nodes") joined by arrows ("edges" or "links"). A review on 2026-10-17 (a manual pass in the browser plus a full inventory of every handler) found that the same gesture often means different things depending on where it is made, and that some gestures silently do nothing or undo other work. After this change a user can rely on a small set of rules:

Escape always closes the topmost thing: an open menu first, then an open details card together with the highlight it explains, then the selection. Closing a card with its × does the same as Escape. A press anywhere outside an open toolbar menu closes it. Enter on a node shows its details on both renderers; the context-menu key opens the menu at the node. The node menu acts on the node that was right-clicked, unless that node is part of the selection, in which case it acts on the selection. The node whose card is open is never drawn faded. Looking around (panning, zooming, Fit) while a layout runs no longer stops the layout halfway. Hiding nodes never makes unrelated nodes appear or the whole graph re-arrange. A ⌘/Ctrl/Shift-click or box drawn while a filter is active changes the selection the user can see. Tooltips name the shortcuts that exist and only those.

To see it working, start a server, load the "review" data below, open the Graph page, press "Load 100 statements", and try the gestures listed under Validation and Acceptance.


## Progress

- [x] (2026-10-17 14:00Z) Built the branch (`-Pquick install`), ran it on port 8082 with a review repository, exercised the page by hand.
- [x] (2026-10-17 14:15Z) Inventoried every pointer, keyboard, menu, card and dismissal behaviour in code (subagent) and verified the top claims by reading the code.
- [x] (2026-10-17 14:25Z) Baseline: `workbench-graph-explorer-interaction.spec.js` 13/13 pass against port 8082.
- [x] (2026-10-17 15:40Z) Milestone 1: dismissal and focus (Escape layering, card ×, popovers close on any outside press, large-canvas Escape).
- [x] (2026-10-17 15:40Z) Milestone 2: same gesture, same result on both renderers (Enter, context-menu key position and focus return, label clicks and reveal in large mode, macOS Ctrl-click).
- [x] (2026-10-17 15:40Z) Milestone 3: menu and selection targets (menu scope, group action closes menu, focused node not faded, modifier gestures and matrix picks under filters).
- [x] (2026-10-17 15:40Z) Milestone 4: layout and viewport (panning/zooming/Fit do not cancel layouts, node click does not freeze a layout, Hide leaf neighbours keeps positions, hidden class nodes do not reappear).
- [x] (2026-10-17 15:40Z) Milestone 5: wording (tooltips, aria labels, status messages).
- [x] (2026-10-17 16:20Z) Added on the way: the node shown in the card moves out from under it again when the canvas resizes (the sheet on phones).
- [x] (2026-10-17 15:10Z) Red: new unit tests 5/6 fail, new Playwright spec 12/12 fail on the unchanged build (logs in the session scratchpad `pw/red-final.log`).
- [x] (2026-10-17 15:20Z) Milestone 6 (added on the user's instruction "Layout engine is in-scope"): a hub with many leaves fills concentric rings instead of one ring or one column.
- [x] (2026-10-17 17:10Z) Milestone 7 (the user's follow-up "Fix the issues you deliberately didn't fix"): legend chips toggle their type and keep other filters; Undo/Redo restore filters, range texts and the card; saving or deleting a group is undoable; Delete/Backspace hide; arrow keys move between nodes with one tab stop per canvas. Red first: 3 unit and 7 Playwright cases.
- [x] Full explorer unit tests, all graph Playwright specs, front-end build; browser check; summary.


## Surprises & Discoveries

Observation: hiding every instance of a class (legend "Person", then Hide) makes the class itself appear as a lone "Person" card. Evidence: in the review repository, after Hide the canvas showed a grey "Person" card and the status bar said 6 nodes. Cause: `removeGraphNodes` (graph-operations.ts) drops the removed nodes' `rdf:type` edges, and `canvasGraph` (graph-canvas-scope.ts) only hides a class node while some loaded `rdf:type` edge still points at it.

Observation: the legend chip's tooltip says "Select the N X nodes" but it sets a Type filter (the Filters button shows a count), and that selection survives both Escape and a background click. This plan keeps the legend behaviour and makes Escape and the other gestures consistent with it (filters are cleared by "Clear", the chip again, or the filter panel), recorded in the Decision Log.

Observation: a hub with 519 leaves (one star, 520 nodes) is organised as a strip 409 px wide and 21,447 px tall, and takes 8 s; 150 leaves give one ring of 21 M px². Evidence: scratch script `star-layout.mjs` (aspect 1.6 at 400 leaves, 0.02 from 499). Cause: `placeFan` in graph-layout-fans.ts puts every unit of a fan on one rectangular ring. One ring's area grows with the square of its units, and its score (area times a weak aspect penalty) then prefers a ring one card wide on each side over a balanced one. The >1500-node multilevel path is unaffected (6,215×5,827 in 40 ms).

Observation: the node menu fades out over 150 ms and counted as open meanwhile, so an Escape right after a menu action (Playwright types fast enough) closed only the already closing menu. Evidence: the large-renderer Escape test failed 1 of 2 runs; a scratch script with 500 ms pauses passed. Fix: a menu whose close timer runs is treated as closed.

Observation: with rings, straight lines from a hub to its outer leaves pass under inner cards (300 leaves: 1,113 line/card overlaps), but the edge router threads most drawn arrows through the lanes (291 routed overlaps; 150 leaves: 1). The single ring had almost none but covered 81 M px² for 300 leaves. Doubling the gap between cards on a side halved the routed overlaps for 26% more area. The router is now most of the time for a 519-leaf star (2.9 s of 3.5 s, `detour` in graph-edge-router.ts); layout itself is milliseconds.

Observation: `workbench-graph-fixed-bnodes-live.spec.js` "stored blank nodes cross from Explore…" fails on the original build too: it opens the card at desktop width, narrows the page to 390 px, and clicks the node, which the card (now a sheet) covers. The resize reveal added here addresses the cause.

Observation: `npx playwright test` failed with "npx playwright install" because Playwright 1.64.1 needs Chromium build 1248, which was not in `~/Library/Caches/ms-playwright`. `npx playwright install chromium` in `e2e/` fixed it.


## Decision Log

- Decision: Escape is layered and identical on both renderers. Step 1: if a toolbar menu, the node menu or the Find list is open, close only that. Step 2: otherwise, if a node card or statement card is open, close it and clear the focused node and focused edge (the neighbourhood highlight goes with the card). Step 3: otherwise clear the manual selection. Filters are never cleared by Escape, matching the background click, which also leaves filters alone. Rationale: today Escape closes the card but leaves the graph dimmed with no explanation, a second Escape does nothing, Escape on the large renderer clears everything at once, and Escape pressed to close a toolbar menu also closes the card. Date/Author: 2026-10-17, Claude.
- Decision: the card's × behaves like Escape step 2. Rationale: the dimming exists to explain the card; leaving it after the card closes looks like a bug. Date/Author: 2026-10-17, Claude.
- Decision: toolbar menus close on any pointer press outside them, using a capture-phase document listener. Rationale: nodes, "+N" badges and cards stop React event propagation, so the bubble-phase listener never ran and the Layout menu stayed open over a node being dragged. Date/Author: 2026-10-17, Claude.
- Decision: Enter and Space on a node show its details on both renderers; the ContextMenu key and Shift+F10 open the node menu positioned at the node, and focus returns to the node when the menu closes. Rationale: Enter opened the menu on the large renderer and showed details on the SVG one. Date/Author: 2026-10-17, Claude.
- Decision: the node menu's scope is the right-clicked node alone unless that node is in the selection, for every item (Select neighbours, Add neighbours to selection, Invert selection, Save selection as a group, Hide, Expand). Rationale: Hide and Expand already worked that way, but the selection items used the old selection, so a menu titled "Carol" selected the neighbours of Alice and Bob. Date/Author: 2026-10-17, Claude.
- Decision: a node shown in the card (focused) or hovered is never faded by the selection dimming. Rationale: clicking an unselected node while others are selected opened its card while the node itself was drawn at 18% opacity. Date/Author: 2026-10-17, Claude.
- Decision: while a filter is active, a modifier-click or a box selection starts from what the filter selects: the filter's matches become the manual selection, the filters are cleared, and the gesture is applied. Rationale: the visible selection is the filter's; the gesture edited a hidden manual set, so nothing changed on screen and the next filter change wiped it. Date/Author: 2026-10-17, Claude.
- Decision: panning, zooming (wheel, pinch, buttons) and Fit while a layout or layout animation runs only take the camera from the animation; the layout job keeps running and nodes keep moving to their targets, and when the layout finishes it does not re-fit a view the user moved. A press on a node no longer stops a layout; the layout stops when the node actually starts to move (a drag), at which point the status says so. Rationale: a scroll right after Expand or Organize froze new nodes at their seed positions and left the status at "Organizing…". Date/Author: 2026-10-17, Claude.
- Decision: "Hide leaf neighbours" keeps every remaining position, as Hide does, and hiding never leaves a class card that was only drawn away because of `rdf:type` links. Rationale: Hide leaf neighbours re-arranged and re-fitted the whole graph; hiding all Persons surfaced a "Person" card. Date/Author: 2026-10-17, Claude.
- Decision: a fan of at least 40 single-card leaves fills concentric rectangular rings around its anchor, inner rings first, each ring's cards shifted by half a step from the ring inside it so arrows to outer cards tend to pass through gaps; the ring steps are stretched so the block approaches the requested aspect ratio; slots blocked by obstacles are skipped, and for a facing fan only slots within its span are used. Rationale: the user put the layout engine in scope after the 520-node star rendered as a thin strip; rings filled in turn grow with the number of leaves instead of its square, and the computation is linear. Fans with sub-trees and fans under 40 leaves keep the single ring, which the existing compactness and no-arrow-through-card tests pin. Date/Author: 2026-10-17, Claude.
- Decision: when the canvas changes size while a card is open, its node is revealed again (only if covered), as on opening. Rationale: on phones the card becomes a sheet over the canvas and covered the node after a resize, so it could not be pressed. Date/Author: 2026-10-17, Claude.
- Decision: the existing "pure 300-leaf fan" test measures drawn (routed) arrows instead of straight centre lines and also checks the drawing is not a column. Rationale: ring fans deliberately let straight lines pass inner cards; the canvas draws routed arrows. Date/Author: 2026-10-17, Claude.
- Decision (superseded the same day): keep the legend chip replacing all filters and keep Undo clearing filters. The user then asked for them to be fixed; see the next decisions. Date/Author: 2026-10-17, Claude.
- Decision: a legend chip adds its type to the Type filter or takes it out (types OR together, filters AND), leaving other filters alone; its tooltip says which. Rationale: replacing every other filter silently threw away the user's filters. Date/Author: 2026-10-17, Claude.
- Decision: history snapshots carry the filters and their range texts; Undo and Redo put them back with the graph and the selection, and a restored focused node comes back with its card. Saving and deleting a named group push history. Rationale: Undo cleared filters, left a dimmed graph without a card, and silently dropped groups saved after the change it undid. Date/Author: 2026-10-17, Claude.
- Decision: Delete and Backspace (outside text fields, Graph view, no menu open) hide the selection, else the node in the card, else the node with keyboard focus, exactly as Hide does (undoable). Rationale: Hide is the main editing verb and had no key. Date/Author: 2026-10-17, Claude.
- Decision: arrow keys move to the nearest node in that direction (`nodeInDirection` in graph-keyboard.ts: within about 56°, a step aside costs twice a step ahead); the card follows when open, the node is brought into view. The SVG canvas has one tab stop (roving tabindex: the node the keyboard was last on, else the card's, else the first); on the large canvas arrows start from the shown node or the node nearest the middle of the view. Rationale: Tab walked every node and there was no way to move between nodes by keyboard. Date/Author: 2026-10-17, Claude.


## Outcomes & Retrospective

Every inconsistency picked in the Decision Log is fixed and pinned by a test: new unit tests in `interaction-consistency.test.mjs` (escapeStep, contextMenuScope, selectionToEdit, selectionModifier, orphaned class cards, large-renderer label hits and screenPoint) and `layout-quality.test.mjs` (big stars), and 13 Playwright cases in `workbench-graph-explorer-consistency.spec.js`, all observed failing on the original build first. A 520-node star that drew as a 409×21,447 strip in 8 s now fills the view (about 5,400×3,200, aspect 1.6) in 3.5 s, mostly edge routing. The follow-up round fixed the four items first left for later (legend, Undo and filters, Delete, arrow keys) and two found on the way (Undo restoring focus without a card, deleted groups not undoable); 90 graph Playwright cases and 359 unit tests pass. Left for later: the edge router's cost on big stars (proposed as its own session).


## Context and Orientation

The front end lives in `tools/workbench/src/main/frontend/graph-explorer/`. `GraphExplorerApp.tsx` (about 4,200 lines) holds the state and wires every handler; references below give line numbers as of `dae434eae4`, which shift as edits land. There are two renderers. Below 500 drawn nodes (`LARGE_GRAPH_NODES` in `explorer-config.ts`) the canvas is an SVG element whose nodes are `<g class="graph-node" tabIndex=0 role="button">` elements rendered by `graph-canvas-view.tsx`; their pointer and key events call `nodeHandlerImpl` in `GraphExplorerApp.tsx`. From 500 nodes the canvas is `large-graph-canvas.tsx`, a `<canvas>` driven by the `CanvasGraph` class in `graph-canvas-controller.ts`, which reports taps through an `onSelect(id, multiple)` prop and menus through `onMenu`.

The terms used here: the "focused node" (`focusedNodeId`) is the node whose details card is shown; it also dims everything not adjacent to it ("dim-by-focus"). `inspectorOpen` says whether that card is visible. The "manual selection" (`manualSelectedNodeIds`) is what modifier-clicks, box drawing and menu items select. "Filters" (`selectedFacetValues`, managed by `use-facet-filters.ts`) select the nodes that match them; while any filter is active the effective selection `selectedNodeIds` is the filter's matches and the manual selection is ignored. The "focused edge" (`focusedEdgeId`) is the edge whose statement card is shown. The node menu (`node-menu.tsx`) opens on right-click or long-press; its `menu.scopeNodeIds` is what it acts on. Toolbar menus are `<details>` popovers in `explorer-toolbar.tsx` closed by `closeMenusOutside` and `closeMenusWithKeyboard` (document listeners near line 1390). `removeGraphNodes` in `graph-operations.ts` removes nodes and their edges; `canvasGraph` in `graph-canvas-scope.ts` decides what the canvas draws (it omits literal values and classes reached only through `rdf:type`). Layout runs in a worker (`layoutJobRef`) and its result is animated by `animateProgrammaticGraph`, which moves nodes and the camera (`transformRef`) together; `applyInteractiveTransform` and `zoomAt` are the user's camera moves.

Tests: `npm test` in `tools/workbench` runs Node unit tests under `tools/workbench/src/test/frontend/graph-explorer/` (`canvas-interactions.test.mjs` drives `CanvasGraph` with fake canvases). Playwright specs live in `e2e/tests/workbench-graph-explorer*.spec.js` and need a running server; `RDF4J_WORKBENCH_BASE_URL` and `RDF4J_SERVER_BASE_URL` point them at it.


## Plan of Work

Milestone 1 (dismissal and focus). Add a small pure module `graph-dismissal.ts` exporting `escapeStep(state)` that returns which layer an Escape press closes ("menu", "card", "selection" or "none") from booleans describing what is open. Replace the body of `closeMenusWithKeyboard` with: compute the step; for "menu" close popovers, the node menu (and let Find handle its own list, it already stops propagation); for "card" close the card and clear focused node and edge; for "selection" clear the manual selection. Make the card's `onClose` do the "card" step. Remove the Escape branch of the large canvas's `onKeyDown` so the document handler does the same thing on both renderers. Register `closeMenusOutside` with `{ capture: true }`.

Milestone 2 (same gesture, same result). In `large-graph-canvas.tsx`, Enter or Space on the focused node calls `props.onSelect(id, shift||meta||ctrl)`; ContextMenu and Shift+F10 open the menu at the node's screen position, for which `CanvasGraph` gets a public `screenPoint(id)`. In `nodeHandlerImpl.keyDown`, open the menu at the node's bounding rectangle instead of the canvas centre. When the node menu is dismissed after a keyboard opening, return focus to the node. Make `CanvasGraph.hit` also match a drawn label's box (remember the label boxes drawn in the last frame). Make `revealNode` measure the large canvas when the SVG is absent. On Apple platforms, a Ctrl-press with the primary button is the context-menu gesture: do not start a box selection for it on either renderer.

Milestone 3 (menu and selection targets). Add `contextMenuScope(nodeId, selected)` to `graph-selection.ts` and use it for every menu item; `canGroup` uses the scope; saving a group dismisses the menu. Change the dimming CSS so `.is-focused` nodes and `.touches-active` edges are exempt from `dim-by-selection`. Add `selectionToEdit(filtersActive, matches, manual)` to `graph-selection.ts`; when filters are active, `toggleNodeInSelection` and `addToManualSelection` first adopt the matches and clear the filters.

Milestone 4 (layout and viewport). Add `viewportTouchedAtRef`. `applyInteractiveTransform`, `zoomAt` and Fit set it and stop only the camera part of a running animation; they no longer cancel `layoutJobRef` or clear the layout intent. `animateProgrammaticGraph` stops writing the transform (per frame and in `finish`) once the user touched the camera after the animation started, and the layout effect uses the current transform as the target when the user touched it after the job started. In node `pointerDown`, stop the animation and layout only when the drag passes the 5 px threshold, capturing drag origins at that moment; if a layout was running, set the status "Stopped the layout to move the node." Give `collapseNode` the same `skipLayoutRevisionRef` treatment as `hideNodes`. In `removeGraphNodes`, also drop a node that was only the target of `rdf:type` edges that are now gone and has no edges left.

Milestone 6 (big fans). In `graph-layout-fans.ts`, `placeFan` first tries a new `ringsFan(request)` for 40 or more single-card units: it generates slots ring by ring around the anchor (vertical sides one card column wide, horizontal sides one card row high, corners left free), stretches the ring step so the outer ring approaches `spacing.aspectRatio`, staggers alternate rings by half a step, skips slots outside the fan's span or blocked by obstacles (relaxing obstacles as `placeFan` does when no slot is left), spreads the last ring's units evenly over its slots, and returns `ring: true` with the outermost ring as `inner`. Test: `layout-quality.test.mjs` "a hub with hundreds of leaves fills rings around it…" (150 and 519 leaves: no overlaps, aspect between 0.8 and 3.2, area under 60,000 px² per leaf, under 4 s).

Milestone 5 (wording). Undo/Redo titles carry their shortcuts ("Undo (⌘Z)"/"Undo (Ctrl+Z)"). The canvas aria label and large-canvas label state the wheel behaviour for the platform and mention double-click to zoom, long-press and Escape. Node aria labels say "Shift-, ⌘- or Ctrl-click" (platform modifier). The selection summary's Clear says "Clear the N selected nodes" when no filter is active. "Add neighbours to selection" reports "Added N neighbours to the selection". "Tidy" status reads "Tidying the layout…". Find's placeholder mentions the `/` shortcut.


## Concrete Steps

From the repository root, build once and run the server from a scratch copy of the jar (never rebuild a jar a server is running from):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick install
    cp tools/server-boot/target/rdf4j-server-boot-6.2.0-SNAPSHOT.jar <scratch>/server-boot.jar
    java -Dorg.eclipse.rdf4j.appdata.basedir=<scratch>/appdata -jar <scratch>/server-boot.jar --server.port=8082

Unit tests, from `tools/workbench`:

    npm test

Playwright, from `e2e`:

    RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:8082/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:8082/rdf4j-server npx playwright test --reporter=line --project=chromium tests/workbench-graph-explorer-consistency.spec.js

For the browser loop without restarting the server: `npm run build` in `tools/workbench`, copy `target/frontend/scripts/graph-explorer` and `target/frontend/styles/graph-explorer.css` into the running server's extracted webapp (`$TMPDIR/rdf4j-server-webapp*/rdf4j-workbench`, the directory created when that server started), and fetch the scripts with `cache: 'reload'` before reloading (the Workbench caches scripts for 600 s).


## Validation and Acceptance

New spec `e2e/tests/workbench-graph-explorer-consistency.spec.js` (written first, observed failing, then passing) on a small people/organisations/cities dataset: Escape closes the card and the dimming, a second Escape clears a ⌘-click selection; × does the same as Escape; Escape on an open Layout menu leaves the card open; pressing a node closes an open Layout menu; right-clicking an unselected node and choosing Select neighbours selects that node's neighbours; a focused node outside the selection is not faded; hiding all Persons does not draw a "Person" card; Hide leaf neighbours leaves other nodes where they were; ⌘-click while the Person filter is active adds the node to the visible selection; scrolling during Organize network still ends with the layout's status and positions. A second describe block loads 520 nodes to exercise the large renderer: Enter shows details; Escape behaves as above. New unit tests cover `escapeStep`, `contextMenuScope`, `selectionToEdit`, the `removeGraphNodes` class case and `CanvasGraph` label hits. All existing `workbench-graph-*` specs and `npm test` must still pass.


## Idempotence and Recovery

Every step is a source edit plus rebuild; re-running builds and tests is safe. The review repository is a non-persistent memory store on the scratch server; recreate it with the PUT/POST commands in Artifacts if the server restarts.


## Artifacts and Notes

Review data (repository "review"): ten people, organisations and cities with `ex:knows`, `ex:worksFor`, `ex:livesIn`, `ex:location`, `ex:partner`, one blank-node address and `geo:lat`/`geo:long` on the cities. Manual findings are in the session scratchpad `findings.md`.


## Interfaces and Dependencies

No new dependencies. New exports: in `graph-dismissal.ts`, `type EscapeLayer = "menu" | "card" | "selection" | "none"` and `escapeStep(open: { menu: boolean; card: boolean; selection: boolean }): EscapeLayer`; in `graph-selection.ts`, `contextMenuScope(nodeId: string, selected: ReadonlySet<string>): string[]` (the node first, then the rest of the selection sorted) and `selectionToEdit(filtersActive: boolean, matches: ReadonlySet<string>, manual: ReadonlySet<string>): ReadonlySet<string>`; on `CanvasGraph`, `screenPoint(id: string): { x: number; y: number } | null`.

Revision note (2026-10-17, Claude): added Milestone 7 after the user asked to fix the items first left for later; the Decision Log entry that kept them was marked superseded and the new decisions were added.
```
