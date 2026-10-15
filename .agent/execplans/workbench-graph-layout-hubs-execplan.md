# Graph explorer: hub-and-satellite organization and bounded edge curves

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log` and
`Outcomes & Retrospective` must be kept up to date as work proceeds. It follows `.agent/PLANS.md`.

## Purpose / Big Picture

The Graph page of the RDF4J Workbench (`tools/workbench`, React island under
`tools/workbench/src/main/frontend/graph-explorer`) draws RDF resources as cards and the statements between them as
arrows. Two things make the drawing hard to read today. First, "Organize network" packs everything evenly: a resource
with many statements (a hub, such as an organization with its products, label and class) sits as close to the other
hubs as to its own leaves, so the eye cannot tell which cards belong together. Second, when an arrow has to bend
around a card, the bend is sometimes a huge arc over half the drawing even though the other side of the card is empty;
worse, the explorer bends arrows around cards that are not drawn at all (literal values and classes hidden by the
Values toggle). After this change, "Organize network" keeps hubs apart, fans each hub's leaves on the side facing away
from the other hubs with leaves of the same kind next to each other, and keeps nodes that share neighbours and types
together; arrows bend only around visible cards, pick the side that needs the smallest bend, and never arc further than
three quarters of their own length. A person can see it by loading the demo graph described below, choosing Layout →
Organize network, and exporting the SVG: the organization's products, label and class sit in a fan on its far side,
the people form one tight cluster, and no arrow crosses another.

## Progress

- [x] (2026-10-15 16:05Z) M0 ExecPlan written; unit-test baseline captured: `npm test` in `tools/workbench` → 219 tests, 219 pass, 0 fail (4.6 s).
- [x] (2026-10-15 16:30Z) M1 Edge routing routes the drawn graph only (`graph-edge-routing.ts`, App call sites, worker
      filtering of `statement-link:` pseudo-edges). `edge-routing-sides.test.mjs` red (module missing, then
      `[...result.bends.keys()]` contained the pseudo id) → green; routing suites 27/27.
- [x] (2026-10-15 16:50Z) M2 Router derives detours from the cards the least-obstructed round-one detours hit (both
      sides, ≤ 6 curves, 26 candidates) (cap unchanged, see Decision Log). Red (`bend 0`: straight line kept through
      card A because every small detour hit a second card) → green; routing suites green.
- [x] (2026-10-15 17:40Z) M3 Hub-and-satellite layout (`graph-layout-satellites.ts`, `flatCandidate` in
      `organizeComponent`, `halos` in `computeNetworkLayout`). `layout-satellites.test.mjs` red (crossings 1, Dave not a
      satellite, star radii 144–231) → green; all layout suites 73/73. Demo: 0 crossings, Acme–Alice/Bob/Oslo
      311/375/379 vs spokes 233, satellites 60° clear of skeleton neighbours; shared-hubs/chain/star fixtures unchanged.
- [x] (2026-10-15 18:05Z) M4 Similarity springs (`similaritySprings`, `LayoutOptions.springs`, second flat candidate in
      `flatCandidates`). Red (`does not provide an export named 'similaritySprings'`) → green; layout suites 74/74;
      whole explorer suite 229/229 (baseline 219 + 10 new).
- [x] (2026-10-15 18:50Z) M5 Browser check on the running server-boot (:8081, bundle copied into its extracted
      webapp): restoring the user's stored Square grid (Values off) draws 3 arcs + 8 straight edges (was 4 + 7; the
      three are forced by whole cards in the straight path); Organize network with Values off: 11 straight edges, 0
      arcs, products fanned below Acme away from the people; with Values on: 27 straight edges, 0 crossings, 0 arcs, no
      console errors, the hub-and-fan structure of the preferred export. Playwright graph specs (interaction,
      combinations, store, visual) against :8081: 33 passed (1.4 min). Whole explorer unit suite 230/230.
- [x] (2026-10-15 19:05Z) M6 `npm run typecheck` clean; `scripts/checkCopyrightPresent.sh` clean; module verify
      `mvnf tools/workbench`: 1010 tests, 0 failures (GraphServletTest 13, GraphExplorerQueryTest 31; root -Pquick
      install 27 s); memory note `workbench-graph-layout-hubs.md`; nothing committed.

## Surprises & Discoveries

- Observation: the compact drawing the user dislikes is exactly what `computeGraphLayout(demo, {mode: 'network'})`
  returns today (same angles and distances as the exported SVG), so it is the default, not a stale layout.
  Evidence: Acme leaves `Organization@177°/223`, `lit-Acme@-159°/220`; Acme–Alice 139, Acme–Oslo 221 (node script).
- Observation: the 400 px upward arcs come from routing against hidden nodes. On the ten drawn cards alone
  `optimizeEdgeBends` bends Acme→Oslo by −130 (below the row) and Acme→Widget by +155; adding hidden literal and class
  cards as obstacles produces the capped arc (cap = distance × 1.25 = 400).
- Observation: plain fCoSE on the peeled skeleton is much worse (593 crossings on the 237-node `sharedHubNetwork`
  fixture) than the full existing organize path on the same skeleton (43), and skeleton + fans lands at 75 crossings
  against today's 87 with 27 % less area. The skeleton must therefore go through `organizeComponent`'s whole candidate
  search, not a bare `computeNetworkLayout` call.
- Observation: `computeGroupedLayout` does return a grouped candidate for the demo graph (the people and their labels
  form one Louvain community), but the fanned flat candidate wins on score (52 vs 85, crossings 0 vs 3).
- Observation: the live page restores the user's stored Square grid (4 columns × 3 rows, Values off) whose arcs are
  not caused by hidden nodes: Bob→Dave crosses two full rows of cards and Alice→Bob crosses two cards in its own row,
  so an arc around the block is the only body-clear route. The exported "terrible" SVG had a looser arrangement where
  hidden nodes were the cause. After the margin policy the grid keeps three arcs (Alice→Bob 174, Bob→Dave −437,
  Acme→Gadget −284); the other eight edges are straight.
- Observation: similarity springs between the four people cost crossings on the demo graph (4 with, 0 without), so
  they are a scored alternative, never unconditional.

## Decision Log

- Decision: the router counts only the card body as an obstruction; a curve inside the 9 px margin is a *graze* that
  adds 400 to the score (1200 when within 4 px), and the detour rounds also run while the best route still grazes.
  Rationale: in the user's stored Square grid (Values off) two of the four giant arcs existed only because the straight
  line passed 2–8 px from a card edge while threading the 36 px corridor between two rows; a wide arc reads far worse
  than a line that brushes a margin. Body hits stay obstruction-first (an arrow through a card looks attached to it),
  so the `layout-readability` y=110/300/1000 cases and the `geometry-kernels` exact-box snapshot are unaffected. The
  inner 4 px band keeps the router from squeezing through corridors with no clearance (test 3 would otherwise thread
  at 3 px instead of taking a clear 210 px bend).
  Date/Author: 2026-10-15 / Claude.

- Decision: every anchor gets a halo (side = 1.5 × its spoke radius), not only hubs with three or more satellites, and
  the flat candidate runs the smooth overlap removal itself and skips `refineLayoutGeometry`.
  Rationale: with no halo for single-label anchors the skeleton packed Alice and Carol 127 px apart, their labels
  overlapped, and the repair-only (greedy) overlap pass in `computeGraphLayout` shoved a label into the Acme→Oslo
  corridor (edge through card) and created two crossings; coordinate descent likewise dragged a label into its
  anchor's spoke. With halos for all anchors, VPSC overlap removal inside the candidate and no coordinate descent the
  demo has 0 crossings and 0 edges through cards.
  Date/Author: 2026-10-15 / Claude.
- Decision: the Louvain grouped candidate keeps winning where it wins today (shared-hub fixture: score 935 vs 4500+
  for either flat candidate), so community-structured medium graphs are unchanged by this plan.
  Rationale: the user's reports are exploration graphs without dense communities; there the fanned flat candidate
  wins (demo: 52 vs 85).
  Date/Author: 2026-10-15 / Claude.

- Decision: peel satellites only for "Organize network", Values-on, place-new-nodes and selection layouts; never when
  `incremental`, `refine`, `repairOnly` or named `groups` are set, and never for the pure-star shape.
  Rationale: expansion must keep the user's mental map (new nodes are already seeded into the anchor's free sector by
  `seedIncrementalNodes`); a named-group member fanned outside its box breaks the group invariants; the pure-star
  sunflower in `computeNetworkLayout` needs the full graph.
  Date/Author: 2026-10-15 / Claude.
- Decision: the grouped candidate (`computeGroupedLayout`) stays on the full graph; only the flat candidate is
  skeleton + fans, and both are scored on full positions.
  Rationale: group boxes, compaction and orientation index every node; scoring half-populated positions throws.
  Date/Author: 2026-10-15 / Claude.
- Decision: hub halos are applied only to the fCoSE element box in `computeNetworkLayout`, never via `nodeSizes`.
  Rationale: `nodeSizes` disables the grouped and hub layouts and changes the clearance used by overlap removal.
  Date/Author: 2026-10-15 / Claude.
- Decision: the detour cap stays `min(2400, max(180, distance × 1.25))`; the planned 0.75 × length cap is withdrawn.
  Rationale: `layout-readability` "routing avoids a card at y=110" pins a real need: a wide card right beside an end
  port of a 2000 px edge is cleared only by a bend of about 2000 (the quadratic offset 2t(1−t) is tiny near an end).
  The user's 400 px arcs are fully explained by hidden obstacles (M1) and the missing second candidate round (M2);
  with those fixed the demo edges bend by 130–155 on a 320–490 px edge.
  Date/Author: 2026-10-15 / Claude.

## Outcomes & Retrospective

Delivered, uncommitted on `GH-0000-workbench-graph-explorer` (5 files changed, 2 new modules, 2 new test files, 1
fixture). "Organize network" now peels leaf chains into satellites, lays the skeleton out with a halo per anchor and
fans the satellites into each anchor's free sector grouped by predicate and type; on the user's demo graph that gives
0 crossings, hub-to-hub distances 1.3–1.6 × the spoke length and every satellite at least 60° clear of the skeleton
neighbours, in the browser as in the unit tests. Arrows are routed only around drawn cards, detours are derived from
every blocker in up to six rounds, and brushing a card's margin is a small penalty rather than an obstruction, so the
giant arcs of the exported bad case are gone and the user's dense Square grid keeps only the three arcs that whole
cards force. Not changed: incremental expansion (keeps the mental map), named groups, community (Louvain) layouts,
the pure-star sunflower, the > 1500-node path. Lessons: measure the candidate scores before assuming which candidate
wins (the grouped candidate quietly won on the shared-hub fixture); the quadratic router cannot cap detours below the
edge length without losing near-endpoint clearance; the greedy repair-only overlap pass and the coordinate-descent
refinement both destroy deliberate fan geometry, so a structured candidate has to run smooth overlap removal itself
and skip refinement. Possible follow-up: orthogonal (polyline) routing for grid layouts, where a single quadratic bend
can only go around a whole row of cards.

## Context and Orientation

All paths below are relative to the repository root. The explorer's source is
`tools/workbench/src/main/frontend/graph-explorer/`; its unit tests are `tools/workbench/src/test/frontend/graph-explorer/*.test.mjs`
and run with `npm test` in `tools/workbench` (Node's own test runner with `--experimental-strip-types`, so the tests
import the `.ts` sources directly; no build is needed).

A *graph* here is `{nodes, edges, revision}` (`graph-types.ts`). A node is a card with `id`, `label`, `kind`
(`iri`, `literal` or `blank`), optional `types` (the values of its `rdf:type` statements), optional `x`/`y` and
`pinned`. An edge is one statement: `{id, source, target, predicate, label}`. The *drawn graph* is what the canvas
shows: `canvasGraph(graph, showValues)` in `graph-canvas-scope.ts` removes literal nodes, `rdf:type` edges and classes
reached only through `rdf:type` when the Values toggle is off.

Layout runs in a Web Worker: `GraphExplorerApp.tsx` (the `useEffect` around line 1077) sends the drawn graph plus
options to `startGraphLayout`, which calls `computeGraphLayout` in `graph-layout-engine.ts`. That function splits the
graph into connected components, calls `organizeComponent` for each, packs the components, removes card overlaps
(`separateLayoutRectangles`, `graph-layout-overlap.ts`), nudges cards off arrows (`improveLayoutReadability`,
`graph-layout-readability.ts`) and finally computes an arrow bend per edge (`optimizeEdgeBends`, `graph-geometry.ts`).
`organizeComponent` first tries the large-terminal-fan layout (`computeHubNeighborhoodLayout`, for a hub with 24 or
more leaves), then compares candidates: a community layout (`computeGroupedLayout`, `graph-layout-groups.ts`, which
returns `undefined` when there are no good communities) and a flat fCoSE layout (`computeNetworkLayout`,
`graph-layout-network.ts`, which runs the cytoscape-fcose force layout headlessly with a seeded random generator). Each
candidate is refined (`refineLayoutGeometry`) and scored (`layoutCandidateScore`, `graph-layout-score.ts`); the lowest
score wins.

An arrow is straight when its bend is 0 and a quadratic curve otherwise (`edgeGeometry`). `optimizeEdgeBends` picks a
bend per edge by trying candidate bends and scoring them: fewest cards hit first, then the smallest bend and the
fewest conflicts with other arrows. The renderer draws every edge of the drawn graph with the bend stored in the App's
`edgeBends` state.

Terms used below. *Satellite*: a node whose only connection to the rest of the drawn graph is one neighbour (its
*anchor*), including small chains hanging from it (a product and its label). *Skeleton*: the graph with satellites
removed. *Fan*: satellites placed on an arc around their anchor. *Halo*: the extra room reserved around a skeleton node
during the force layout so other skeleton nodes stay out of the space its fan will need.

The demo graph used throughout (23 nodes with Values on, 10 with Values off): people Alice, Bob, Carol, Dave of class
Person (Alice knows Bob and Carol, Bob knows Dave; Alice and Bob work for Acme; Carol lives in Oslo); organization Acme
of class Organization, located in Oslo, makes Widget, Gadget and Gizmo; city Oslo of class City, in Norway; every
resource has a label literal. It lives in the test fixture `fixtures/layout-demo-graph.mjs`.

## Plan of Work

M1. Create `graph-edge-routing.ts` exporting `routeDrawnEdges(graph, showValues, passes)`: take `canvasGraph(graph,
showValues)`, return an empty map when the drawn graph has `LARGE_GRAPH_NODES` or more nodes (`explorer-config.ts`),
otherwise `optimizeEdgeBends(drawn.nodes, drawn.edges, edgeRoutingPasses(drawn.edges.length, passes))`. In
`GraphExplorerApp.tsx` replace the bodies of `routedBends` (near line 578) and the drag-end routing (near line 2851)
with calls to it. Export the pseudo-edge prefix `statement-link:` from `statement-links.ts` as a constant plus an
`isStatementLinkEdge(edge)` predicate; use it where the worker payload is built (near line 1195) and in
`computeGraphLayout` so that lanes, ports, routing and the pass count use `graph.edges` without pseudo-edges.

M2. In `optimizeEdgeBends`: change `cap`; after scoring the first candidate set, when the best candidate still hits a
card, derive new candidates from the cards hit by that best curve (both sides, same formula as for the straight curve)
and score them too. Keep the 26-candidate bound per round and the deterministic candidate order.

M3. Create `graph-layout-satellites.ts` with `peelSatellites`, `fanRadius`, `placeSatellites`, `isPureStar` and
`similaritySprings`. Extend `LayoutOptions` with `halos` and `springs`. In `computeNetworkLayout` enlarge the fCoSE
element box for nodes in `halos` and add `springs` as extra cytoscape edges with function-valued `idealEdgeLength` and
`edgeElasticity`. In `organizeComponent` build the flat candidate as peel → network layout of the skeleton → fan →
full positions, before the dedupe signature, refinement and scoring; gate peeling as recorded in the Decision Log.

M4. Add the second flat candidate with springs when the gates allow; both scored with `layoutCandidateScore`.

## Concrete Steps

Working directory `tools/workbench` unless stated.

    npm test                                   # whole explorer suite, expect all passing
    node --experimental-strip-types --test src/test/frontend/graph-explorer/edge-routing-sides.test.mjs
    node --experimental-strip-types --test src/test/frontend/graph-explorer/layout-satellites.test.mjs
    node --experimental-strip-types src/test/frontend/graph-explorer/support/benchmark-layout.mjs   # prints metrics
    npm run typecheck
    (repository root) cd scripts && ./checkCopyrightPresent.sh
    (repository root) python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench   # once, at the end

## Validation and Acceptance

Run `npm test` in `tools/workbench` and expect every test to pass. The new tests fail before the change and pass
after: `edge-routing-sides.test.mjs` (hidden cards do not bend drawn arrows; a detour clears a second card on the
empty side within 0.75 × the arrow length) and `layout-satellites.test.mjs` (demo graph: 0 crossings, Acme's and
Oslo's satellites at least 60° from every skeleton neighbour and inside a 200° sector, Acme at least 1.3 × its longest
satellite spoke away from Alice and Oslo, Widget/Gadget/Gizmo adjacent on the arc, deterministic, Values-off
satellites as expected, 8-node star fanned on a full circle, incremental and grouped layouts unchanged). The
`sharedHubNetwork` benchmark must stay at or below today's 87 crossings, 30 M area and 2245 p95 edge length. In the
browser (server-boot with the rebuilt WAR), Organize network on the demo graph and export SVG: no crossing, hubs apart;
Values off, Layout → Square, drag a card: no arrow path has a control point further than 0.75 × its chord from it.

## Idempotence and Recovery

Every step is a plain file edit or a test run and can be repeated. Nothing is committed by this plan; `git diff` shows
the full change and `git checkout -- <file>` is not used (inspect with `git diff` only). If a layout threshold test
fails after M3, adjust the constants in `graph-layout-satellites.ts` (halo scale 1.3, subtree cap 8, fan span 1.1π)
and re-run the two new test files before the whole suite.

## Artifacts and Notes

M1 red → green (`node --experimental-strip-types --test src/test/frontend/graph-explorer/edge-routing-sides.test.mjs`):

    not ok 1 - src/test/frontend/graph-explorer/edge-routing-sides.test.mjs
    # Error [ERR_MODULE_NOT_FOUND]: Cannot find module '.../graph-explorer/graph-edge-routing.ts'
    ...
    ok 1 - hidden values and classes never bend the drawn arrows
    ok 2 - layout routing ignores the statement-link springs the app adds for reifiers
    # tests 27  # pass 27  # fail 0   (with layout-readability, geometry-kernels, statement-links, layout-quality, layout-hub-neighborhood)


Layout fixture metrics after M4 (`computeGraphLayout`, mode network; `layoutMetrics` from `support/benchmark-layout.mjs`):

    demo values on   overlaps=0 edgeNode=0 crossings=0 area=1.1M p95=375 curved=0  (before: crossings 1, Acme–Alice 139)
    demo values off  overlaps=0 edgeNode=0 crossings=0 area=0.4M p95=328 curved=0
    shared-hubs-237  overlaps=0 edgeNode=89 crossings=87 area=30.1M p95=2246 curved=42   (unchanged: grouped candidate wins)
    components-96    overlaps=0 edgeNode=0 crossings=0 area=4.8M p95=247 curved=0        (8-node stars now full-circle fans)
    chain-80         overlaps=0 edgeNode=0 crossings=0 area=29.9M p95=209 curved=0       (unchanged)
    star-300         overlaps=0 edgeNode=329 crossings=0 area=29.5M p95=2724 curved=181  (unchanged: pure-star sunflower)

## Interfaces and Dependencies

No new npm dependencies. cytoscape-fcose 2.2.0 accepts functions for `idealEdgeLength` and `edgeElasticity`
(`node_modules/cytoscape-fcose/README.md`). New exports: `routeDrawnEdges` (`graph-edge-routing.ts`);
`STATEMENT_LINK_EDGE_PREFIX`, `isStatementLinkEdge` (`statement-links.ts`); `peelSatellites`, `fanRadius`,
`placeSatellites`, `isPureStar`, `similaritySprings` (`graph-layout-satellites.ts`); `LayoutOptions.halos`,
`LayoutOptions.springs` (`graph-layout-engine.ts`).
