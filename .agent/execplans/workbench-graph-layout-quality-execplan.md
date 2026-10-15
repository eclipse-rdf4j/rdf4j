# Graph explorer: layout quality pass after the example datasets

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log` and
`Outcomes & Retrospective` must be kept up to date as work proceeds. It follows `.agent/PLANS.md` and builds on
`.agent/execplans/workbench-graph-layout-hubs-execplan.md` (hub-and-satellite organize, drawn-only edge routing, nested
fans, class hubs), which a reader should skim first for the vocabulary (satellite, skeleton, fan, halo, class hub).

## Purpose / Big Picture

Six example repositories (a tree of places, a conference with papers and authors, students and courses, a product
catalog, social clubs, people with many literal values) showed where the Graph page's "Organize network" still draws
poorly. After this plan: with Values on, label values that only repeat a card's title are not drawn and a class with
many instances no longer draws a spoke to each of them (the links appear when one of those cards is hovered or
selected); trees use far less area; the drawing is turned to fit the viewport and re-organizing keeps its orientation;
cards that a routed arrow still crosses are moved aside; arrows that no single bend can route around cards follow a
stepped path through the gaps instead of a huge arc; graphs with one dominant hub or a bipartite shape (students and
courses) are drawn on rings around the hub with far fewer crossings; and readability budgets per graph shape guard all
of this in the unit tests.

## Progress

- [x] (2026-10-15 22:40Z) M0 Gate test `layout-gates.test.mjs` with the current numbers as budgets (9/9).
- [x] (2026-10-15 23:00Z) M1 Values on: label literals that repeat their subject's title are not drawn (`canvasGraph`
      → `withValues`, cached per graph). Red: canvas-scope test 'the title of Oslo is its card' → green 6/6.
- [x] (2026-10-15 23:40Z) M2 Values on: links to class hubs collapsed (`graph-class-links.ts`), not routed by the app or
      the worker, revealed as dashed `.class-link` lines for the hovered, focused or selected cards. Red: unit
      `class-links.test.mjs` (module missing) and Playwright `workbench-graph-explorer-layout.spec.js` (label literal
      drawn: received 1; 8 type links drawn) → green (unit 246/246, spec 2/2).
- [x] (2026-10-16 00:30Z) M3 Organize cost: only the best raw flat candidate is refined (`refineLayoutGeometry` was the
      cost, not the springs). Same results on every fixture, total fixture organize time 1933 → 1617 ms.
- [x] (2026-10-16 00:30Z) M4 Disproven: no fixture shows the length terms picking a worse candidate; scoring unchanged.
- [x] (2026-10-16 02:00Z) M5 `compactFans`: fans widened into their free angle (neighbour chords kept), shrunk toward
      their anchors, skeleton bodies slid toward their neighbours; each step only with card clearance, no added crossing
      or edge through a card, and linked hubs ≥ 1.3 × their longest spoke apart. Tree 13.0 → 11.4M px², demo 1.3 →
      1.0M px². Red: 'area 13.0M' → green.
- [x] (2026-10-16 02:40Z) M6 `graph-layout-orientation.ts`: turn to the viewport's long axis when it fills ≥ 8 % more and
      adds no conflicts after re-separation (never with named groups); then the mirror image closest to the previous
      positions, centred where they were. Red: 'tall viewport: 2192×46', 'cards moved 466 px' → green 3/3.
- [x] (2026-10-16 04:00Z) M7 `graph-layout-routed-repair.ts`: route, find cards a routed curve crosses, move each to the
      cheapest spot in 8 directions × 7 distances that clears the curve and card clearance, lowers the card's routed
      hits and adds no straight conflict; 3 rounds; after the readability pass in Organize and Tidy. Routed arrows
      through cards: conference 33 → 21, shared hubs 32 → 22, social 2 → 1. Red: '33 routed arrows (was 33)' → green.
- [x] (2026-10-16 05:30Z) M8 `graph-stepped-routing.ts`: arrows whose best bend still crosses a card or arcs further than
      0.8 × the card distance get a stepped route (A* over lines through the middle of the gaps, corner cost 70, shared
      legs penalised, rounded corners); `EdgeCurve.points`, `EdgeRoute` (ignored once a card moves), routes in
      `LayoutResult` and app state, hit-testing, midpoint, labels, WebGL and export follow them. Red: 'some arrow needs
      a stepped route', stale route and hit/WebGL tests → green (unit 257/257). Routed arrows through cards: students
      4 → 1, products 1 → 0, shared hubs 22 → 13; the stored Square grid's Bob→Dave and Acme→Gadget run through gaps.
- [x] (2026-10-16 06:30Z) M9 Disproven and removed: a rings candidate (dominant hub in the centre or a bipartite graph's
      small side inside, barycentre sweeps seeded from the force layout, best sweep kept) lost to the force layout on
      every shape (crossings: conference 354 vs 288, students 332 vs 300, products 254 vs 95). Red tests and module
      deleted; nothing ships.
- [x] (2026-10-16 07:30Z) M10 Gate budgets lowered to the new numbers; explorer unit suite 257/257; typecheck and headers
      clean; Playwright graph specs 45/45 (all `workbench-graph-explorer-*.spec.js`, chromium, :8081); module verify
      `mvnf tools/workbench` 1010 tests, 0 failures. Browser: place tree with Values on draws 111 of 197 nodes (label
      values hidden), class cards among instances, hover reveals 'District 1-1-1 — type → District', status
      'Organized the network.'; gesture demo in Square: 10 straight arrows and 1 stepped route, no arcs.

## Surprises & Discoveries

- Observation: the springs candidate was not the organize cost; refining every plain candidate was (students
  577 → 378 ms, products 337 → 221 ms when only the best raw candidate is refined).
- Observation: compaction's shrink step never fires: fans are already as tight as their arcs allow; the room is in
  fans that use 1.1π around an anchor with one neighbour. Spreading at constant arc length shortens the chord between
  neighbouring satellites and makes their sub-fans collide; keeping the chord works.
- Observation: in dense drawings most single-card moves off a routed arrow clash with a neighbour (conference graph:
  515 of 580 rejected candidates), which bounds what the post-routing repair can do locally.
- Observation: barycentre sweeps on concentric rings stall in their first fixed point (a crossing-free cyclic
  bipartite graph kept 266 crossings from a scrambled start) and, even seeded from the force layout, lose to it.

## Decision Log

- Decision: no radial layering.
  Rationale: the force layout already reaches 0 crossings where a crossing-free circular order exists (a cyclic
  enrolment test graph), and on the random fixtures every rings variant had 10–170 % more crossings and more edges
  through cards. Barycentre sweeps on circles also stall in the first fixed point (266 crossings from a scrambled
  start on the cyclic graph). The remaining crossings of the conference and students graphs come from their random
  structure, not from the layout family.
  Date/Author: 2026-10-16 / Claude.

- Decision: the post-routing repair may spread a dense drawing (conference graph 2.4M → 3.2M px²); its area budget
  was raised to 3.5M px².
  Rationale: an arrow drawn through a card reads as a link to that card; removing 12 such arrows is worth the room.
  Most rejected moves in dense drawings clash with neighbours (515 of 580 on the conference graph), so a local move
  cannot do more; the radial layering milestone addresses that shape structurally.
  Date/Author: 2026-10-16 / Claude.

- Decision: fan compaction keeps linked hubs at least 1.3 × their longest spoke apart, which limits the tree's area
  gain to 12 % (28 % without the rule).
  Rationale: without it Acme moved 193 px from Alice with 234 px spokes, undoing the hub separation the user asked for
  (the demo test failed). The tree's remaining room is bounded by sibling district fans touching; a dedicated
  radial tree layout would be needed to pack it tighter.
  Date/Author: 2026-10-16 / Claude.
- Decision: a turn to the viewport is kept only if crossings plus edges through cards do not increase after the cards
  are separated again; named groups are never turned.
  Rationale: cards are about 2.4 times wider than tall, so a quarter turn changes the clearance each pair needs; on
  the students graph it raised edges through cards from 24 to 31, and it broke named group boxes.
  Date/Author: 2026-10-16 / Claude.

- Decision: the similarity-springs candidate stays unconditional and unlimited; only the best raw flat candidate is
  refined by coordinate descent.
  Rationale: per-candidate measurements (session scratchpad `candidates.mjs`, `springs-exp.mjs`) showed the springs
  candidate costs about one fCoSE run (13–290 ms) while `refineLayoutGeometry` on every plain candidate dominated.
  Gating springs on crossings removed the similarity grouping the user asked for (the springs test failed), and
  keeping only each node's 3 strongest springs raised the conference graph from 290 to 355 crossings. Refining only
  the best raw candidate keeps every fixture's result identical and saves 16 %.
  Date/Author: 2026-10-16 / Claude.
- Decision: candidate scoring keeps its length terms (item "fair scoring" dropped).
  Rationale: the grouped candidate wins only where it also has fewer crossings (social 61 vs 62, shared hubs 116 vs
  858); where fans are better (demo 0 vs 3 crossings) they already win. No fixture shows a mis-pick.
  Date/Author: 2026-10-16 / Claude.

## Outcomes & Retrospective

Seven of the eleven proposed improvements shipped; two shipped smaller than hoped and two were disproven. With Values
on, title values and class spokes no longer clutter the drawing (place tree: 197 → 111 cards). Organize is 16 %
faster with identical results; fans are compacted while hubs keep their separation (tree 13.0 → 10.9M px², demo 1.3 →
0.84M px²); the drawing turns to the viewport when that costs no readability and keeps the side it had on screen;
cards are moved off routed arrows, and arrows no single bend can draw take stepped routes through the gaps (routed
arrows through cards, organize: conference 49 → 21, shared hubs 33 → 13, students 4 → 1, products 1 → 0). Gate
budgets now guard nine graph shapes. Not shipped: scoring by stretch (no mis-pick found) and radial layering (lost on
every fixture). The random bipartite shapes (students × courses, conference papers × authors) keep ~300 crossings;
the measurements say that is their structure rather than the layout. Lessons: measure candidates before redesigning
the scorer; a local move cannot clear a dense drawing; rotating a drawing of wide cards is not free.

## Context and Orientation

The explorer lives in `tools/workbench/src/main/frontend/graph-explorer`; unit tests in
`tools/workbench/src/test/frontend/graph-explorer` run with `npm test` in `tools/workbench` (Node's test runner with
`--experimental-strip-types`). `canvasGraph(graph, showValues)` (`graph-canvas-scope.ts`) decides which nodes and edges
the canvas draws; with Values off it drops literals, `rdf:type` links and type-only classes. The layout runs in a
worker: `computeGraphLayout` (`graph-layout-engine.ts`) → `organizeComponent` → candidates (`computeGroupedLayout`,
`flatCandidates` = peel satellites + fCoSE skeleton + fans, `graph-layout-satellites.ts`) scored by
`layoutCandidateScore` (`graph-layout-score.ts`) → overlap removal → `improveLayoutReadability` → `routeEdges` →
`optimizeEdgeBends` (`graph-geometry.ts`, one quadratic bend per edge). The app (`GraphExplorerApp.tsx`) renders edges
from `edgeCurves` (built by `edgeGeometry`) in SVG, with WebGL from 180 nodes and a canvas renderer from 500.
`layoutMetrics` (`support/benchmark-layout.mjs`) measures crossings, edges through cards, routed edges through cards
and area; the shape fixtures are in `fixtures/layout-shapes.mjs` and `fixtures/layout-demo-graph.mjs`.

## Plan of Work

Each milestone starts with a failing test (Routine A) and ends with `npm test` green and the gate test within budget.

## Concrete Steps

    cd tools/workbench
    node --experimental-strip-types --test src/test/frontend/graph-explorer/layout-gates.test.mjs
    npm test
    npm run typecheck

## Validation and Acceptance

`npm test` passes; every new behaviour has a test that failed before its change. The gate budgets end lower than at
M0 for the shapes each milestone targets. In the browser (server-boot on :8081 with the rebuilt bundle) the example
repositories render without the problems listed in Purpose.

## Idempotence and Recovery

All steps are edits plus test runs; nothing is committed by this plan unless the user asks.

## Artifacts and Notes

M0 baseline (`computeGraphLayout` mode network, aspect 1.6; type links excluded from the measures):

    demo values on       crossings=0   edgeNode=0   routed=0  area=1.3M
    demo values off      crossings=0   edgeNode=0   routed=0  area=0.5M
    geo tree             crossings=0   edgeNode=0   routed=0  area=13.0M
    geo tree values      crossings=1   edgeNode=0   routed=0  area=20.7M
    conference hub       crossings=290 edgeNode=140 routed=49 area=2.4M
    student courses      crossings=300 edgeNode=24  routed=4  area=2.6M
    product catalog      crossings=95  edgeNode=7   routed=1  area=2.1M
    social communities   crossings=85  edgeNode=20  routed=2  area=3.2M
    shared hubs 237      crossings=87  edgeNode=89  routed=33 area=30.1M

## Interfaces and Dependencies

No new dependencies.
