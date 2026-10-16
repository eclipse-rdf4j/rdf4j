# Graph explorer: a research-based layout engine for "Organize network"

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log` and
`Outcomes & Retrospective` must be kept up to date as work proceeds. It follows `.agent/PLANS.md` (repository root)
and replaces the network-layout pipeline described in `.agent/execplans/workbench-graph-layout-hubs-execplan.md` and
`.agent/execplans/workbench-graph-layout-quality-execplan.md`; a reader does not need those plans, everything needed is
repeated here.

## Purpose / Big Picture

The Workbench's Graph page (an RDF explorer at `/repositories/{id}/graph`) draws resources as rounded cards, about
110–280 px wide and 38–46 px tall, joined by arrows. When a resource with many neighbours is expanded, the current
"Organize network" layout spreads the neighbours over a 3,400 × 2,100 px area with arrows of 700–1,400 px, and bends or
re-routes arrows in long orthogonal detours. The user's export of such a graph (one hub with 28 leaves, a three-card
chain and two separate pairs) is the motivating example. After this plan, the same graph is drawn in well under a third
of that area with straight arrows, leaves arranged around their hub on the four sides of a box-shaped ring, no card on
top of another and no arrow through a card, and the other example shapes (trees, communities, bipartite graphs, a
conference with papers and authors) are drawn at least as readably as before, measured by a scorecard.

To see it working: build the Workbench front end (`npm run build` in `tools/workbench`), open the Graph page, add a
resource, expand it, and choose Layout → Organize network. Or, without a browser, run the scorecard script described in
`Concrete Steps`, which prints metrics and writes SVG/PNG drawings for every example graph.

## Progress

- [x] (2026-10-15 19:40Z) Context: branch fast-forwarded to the graph explorer tip (`121701849c`); unit suite 257/257.
- [x] (2026-10-15 20:10Z) Research survey (see Decision Log) and baseline scorecard of the current engine.
- [x] (2026-10-15) M1 Size-aware stochastic-gradient stress solver (`graph-layout-stress.ts`): border-to-border
      targets, pivot-MDS start, overlap/occlusion hinges, anchors, rigid fan bodies, half-plane clamps, local search
      against crossings (`untangle`).
- [x] (2026-10-15) M2 Structure decomposition and box-ring fans (`graph-layout-structure.ts`, `graph-layout-fans.ts`),
      small leaf fans on an ellipse.
- [x] (2026-10-15) M3 Organize pipeline v2 (`graph-layout-organic.ts`) with shared-hub sectors and polyomino packing
      (`graph-layout-packing.ts`); `computeGraphLayout` uses it for network mode up to 1,500 cards without named groups.
- [x] (2026-10-15) M4 Port-aware edge router (`graph-edge-router.ts`), used by the worker and by `routeDrawnGraph`.
- [x] (2026-10-15) M5 Incremental expansion (held fans, the expanded card stays, the app's movable set extracted to
      `graph-layout-expansion.ts` with a fairer budget), tidy (`refineOrganicLayout`) and selection-local layouts.
- [x] (2026-10-15) M6 (partial) Gates re-baselined and tests rewritten as behavioural ones (268 unit tests). The old
      network modules were not removed: named groups, graphs over 1,500 cards and repair-only passes still use them.
- [x] (2026-10-15) M7 Browser verification on the Graph page (Organize network and a three-step expansion of the
      screenshot-shaped graph); `mvnf tools/workbench` green (1010 tests).
- [x] (2026-10-16) Graph Explorer Playwright specs run against a local server: 47/47 after fixing the Values placement
      and connector clicks (see Surprises), plus the double-click under the details card (selectNode lets presses
      through and defers its "slide out from under the card" pan for the double-click window).

## Surprises & Discoveries

- Observation: the user's screenshot is reproduced exactly by "Organize network" on the reconstructed graph: the hub
  special case (`computeHubNeighborhoodLayout`) does not apply because the hub is not a pure star (one chain hangs off
  it), so fCoSE lays out 37 cards with a 70 px ideal edge length plus card-sized repulsion, and the bend optimizer and
  stepped router add curves and detours.
  Evidence (baseline scorecard, `user-hub`): 3382 × 2148 px, median visible arrow 732 px, 8 curved or routed arrows.
- Observation: with cards about 4.8 times wider than tall, a column of leaves beside a hub cannot be drawn with arrows
  aimed at card centres: the centre ray to a card near the top of the column enters through its bottom side and cuts
  the card below. Arrows that end on the side of the card facing the hub never do.
- Observation: Dijkstra compared Float64 keys against a Float32 distance row and produced NaN positions ("Invalid spatial
  bounds"); distances are now computed in a Float64 row and copied into the Float32 matrix.
- Observation: fans of two or three leaves inflated the core's footprints (shared hubs grew to 46M px²); only cards
  with at least four pendant children fan (`FAN_MIN`), smaller ones stay in the stress layout.
- Observation: a hub linked to almost everything collapses all graph distances, so stress cannot separate its
  neighbourhoods; hub link lengths (cola-style symmetric difference) and the shared-hub sector layout fix that.
- Observation (browser, after the engine was wired in): expanding the hub, then the chain card below it, then the next
  chain card moved the hub about 1,800 px away from its own ring of leaves. The app lets the hub move (two links from
  the expanded card) but not most of its leaves; the leaves stayed in the core as fixed cards, the hub re-fanned only
  its few movable children, and that fan's estimated box collided with the hub's own fixed ring. Evidence: unit test
  `expanding a card two links from a drawn hub leaves the hub and its fan in place` failed with "the hub moved 233 px"
  before the fix.
- Observation: the app's movable-set rule counted the new cards against its "about half the drawing" budget, so
  expanding a hub with 28 new leaves left only two of the nine cards already drawn free to move; the new ring had to
  grow around pinned cards (5.7M px², arrows of 1,300 px on the user's graph). Counting only the drawn cards against
  the budget gives 2.20M px² with no crossing.
- Observation: a scorecard harness that does not pass `newNodeIds` makes seeded new cards look already placed, so
  their fans are held; the app always passes them.
- Observation (Playwright, Graph Explorer specs against a local server): two specs that pass on the old engine failed
  on the new one. Turning Values on asks for a layout of only the new value and class cards (no layout mode); the new
  engine had sent every local network layout to the "organize a selection as a block" path, so the values were packed
  as a detached block and the class card could end up under the footer. And a click on a reifier's dashed connector
  only opened its statement when it fell within a few pixels of the statement's own arrow (the canvas hit-tests edges,
  not connectors); the new layout draws reifiers a little further from their statement. Both fixed: local layouts
  without a mode place the cards in context (incremental, everything else fixed; a class card whose links are
  collapsed starts among its instances), and the hit test counts a connector as its statement.

## Decision Log

- Decision: replace the fCoSE-based network pipeline (candidates, satellites, hub special case, coordinate-descent
  refinement, readability and routed repair passes) with one stress-based engine for graphs up to 1,500 cards.
  Rationale: the current pipeline is a chain of special cases with size thresholds (24 leaves, 55 %, 64-node cores, 350
  nodes) and each produces a different drawing style; the motivating graph falls between them. Stress minimisation by
  stochastic gradient descent (Zheng, Pawar and Goodman, "Graph Drawing by Stochastic Gradient Descent", IEEE TVCG
  2019) reaches lower stress than majorisation, needs no good start, is deterministic with a seeded shuffle, and takes
  extra terms (Ahmed et al., "(SGD)²", IEEE TVCG 2022) such as overlap and edge-card clearance. Overlap removal by the
  same machinery (Giovannangeli et al., "FORBID", GD 2022, and the TVCG 2024 follow-up that benchmarks nine overlap
  removal algorithms) keeps the stress layout's structure. Node-edge repulsion follows ImPrEd (Simonetto et al.,
  Computer Graphics Forum 2011). Learned layouts (CoRe-GD, ICLR 2024; "Inductive Graph Layout with Implicit Neural
  Fields", arXiv 2608.08876, August 2026) need training or fitting per graph and do not beat SGD stress at this size;
  they were rejected for a browser worker.
  Date/Author: 2026-10-15 / Claude.
- Decision: stress targets are measured between card borders, not centres.
  Rationale: a reader sees the visible part of an arrow. The target distance between cards i and j at graph distance d,
  along the current direction u between them, is r_i(u) + r_j(u) + d·L + (d−1)·2·r̄(u), where r(u) is the distance from a
  card's centre to its border along u, r̄ the same for an average card, and L the visible arrow length. Wide cards then
  sit side by side at their width plus L and stacked at their height plus L, which is what makes drawings compact.
  Date/Author: 2026-10-15 / Claude.
- Decision: pendant trees (the parts of a component that hang off its 2-core by a single link: leaves, chains, small
  trees) are drawn constructively around their anchor on a box-shaped ring, not by the force or stress layout.
  Rationale: industrial organic layouts (yFiles' substructure styles) and the user's preferred export both draw leaves
  as a fan on the free side of their hub; stress places a hub's 28 leaves on a circle whose radius is set by leaf–leaf
  terms, which wastes the space wide cards need least (above and below each other). A ring whose sides are an inner
  rectangle around the hub, with leaves stacked in columns on its left and right sides and side by side on its top and
  bottom sides, keeps every arrow inside the rectangle, so fan arrows never cross each other or a card.
  Date/Author: 2026-10-15 / Claude.
- Decision: the edge router chooses, per arrow, the cheapest clean geometry: centre-aimed straight line, then a straight
  line between the card sides facing each other, then a smooth detour around cards; parallel arrows keep their lanes.
  Rationale: see the second Surprise; a straight line with different end points is better than any detour, and long
  stepped detours and big arcs were what made the old drawings look tangled.
  Date/Author: 2026-10-15 / Claude.

- Decision: extending a drawing (an expansion), a placed fanned card is held (it and its fan stay; only branches with
  new cards are drawn anew around the card they hang from, outward and clear of everything drawn; new children of the
  held card go to free places on its ring) when one of its ring members may not move or when it gained no children.
  A held ring's inside is an obstacle for other cards. A placed fanned card that gained children (the expanded card)
  is fixed during the stress pass, so the cards around it make room instead of the card jumping away.
  Rationale: the mental map; the expanded card is where the user is looking.
  Date/Author: 2026-10-15 / Claude.
- Decision: the app's movable set for an expansion moves to `graph-layout-expansion.ts` (`expansionMovableIds`, the
  same rule) with one change: the budget of about half the drawing (at least 32 cards) counts only the cards drawn
  before the expansion.
  Rationale: see the Observation above; the rule is now unit-tested.
  Date/Author: 2026-10-15 / Claude.
- Decision: keep the previous network pipeline for named groups, graphs over 1,500 cards and repair-only passes.
  Rationale: out of this plan's scope; a follow-up can move them to the organic engine.
  Date/Author: 2026-10-15 / Claude.

## Outcomes & Retrospective

Scorecard at the end (aspect 1.6; baseline in Artifacts):

    user-hub         2.02M px² (was 7.26M)  arrows through cards 0  crossings 0  curved 0  median arrow 338 (was 732)
    user-hub-expand  2.20M px² (was 2.90M)  0 / 0 / 0, the expanded hub does not move
    geo-tree         3.96M px² (was 10.93M) 0 / 0 / 0
    conference       2.22M px² (was 3.18M)  through cards 0 (was 27), crossings 280 (was 272), curved 13 (was 75)
    students         1.28M px² (was 2.58M)  through cards 0 (was 7), crossings 300 (was 253), curved 7 (was 34)
    hub-75           12.29M px² (was 6.58M) through cards 0, curved 0 (was 53): a larger, clean C-shaped ring
    shared-hubs      22.95M px²             through cards 18, crossings 196, curved 38: the weakest shape

The motivating graph is drawn without overlaps, crossings, curved arrows or arrows through cards, at 28 % of the old
area (1581 × 1280 px); the 1.5M px² target of `Validation and Acceptance` was not reached (2.02M). Expansions keep
every drawn card in place in the replayed app flow (hub, chain card, next chain card). Remaining weak spots: shared
hubs (crossings and arrows through cards), dense many-to-many graphs (3–19 % more crossings than before: conference
280 vs 272, students 300 vs 253), very large single fans (hub-75 area), and links revealed between two members of a
held ring (2–4 crossings until Organize network is run).

## Context and Orientation

All front-end code of the Graph page lives in `tools/workbench/src/main/frontend/graph-explorer/` (TypeScript, React
19, bundled by Vite into `tools/workbench/target/frontend`). Unit tests live in
`tools/workbench/src/test/frontend/graph-explorer/*.test.mjs` and run with Node's test runner directly on the
TypeScript sources (`npm test` in `tools/workbench`, which runs `node --experimental-strip-types --test`).

The graph model is `Graph` in `graph-types.ts`: `nodes` (id, label, kind `iri`/`literal`/`blank`, optional `x`, `y`,
`pinned`) and `edges` (id, source, target, predicate, label). A card's size comes from `graphNodeSize(node)` in
`graph-geometry.ts` (width from the label, 112–280 px; height 46 px, 38 for literals). The page draws what
`canvasGraph(graph, showValues)` in `graph-canvas-scope.ts` returns. Layouts run in a Web Worker
(`graph-layout-worker.ts`, started by `startGraphLayout` in `graph-layout-client.ts`) that calls
`computeGraphLayout(graph, options)` in `graph-layout-engine.ts` and returns `LayoutResult`: `positions` (card centres),
`bends` (per arrow, the sideways offset of a quadratic curve's control point; non-zero only for parallel arrows after
this plan) and `routes` (per arrow, a polyline from the source card's border to the target card's border, honoured by
the renderer only while both cards keep the centres it was computed for; see `edgeGeometry` in `graph-geometry.ts`).

`LayoutOptions` (in `graph-layout-engine.ts`) carries the request: `mode` (`network`, `hierarchy` via Graphviz dot,
`square` grid), `aspectRatio` of the viewport, `incremental` with `movableNodeIds` (after an expansion: only these
cards may move), `localNodeIds` (lay out a selection only), `refine`/`repairOnly` (tidy an existing drawing, e.g. after
labels changed size), `groups` (user-named groups, laid out by `computeGroupedLayout` in `graph-layout-groups.ts`),
`seed`. Pinned cards never move. Edges whose id starts with `statement-link:` are springs the app adds so a reifier sits
near its statement; they shape the layout but are not drawn or routed. Links to "class hubs" (classes with six or more
drawn instances, `graph-class-links.ts`) are collapsed: not drawn or routed.

Terms used below. Component: a maximal set of cards connected by links. 2-core: what remains of a component after
repeatedly removing cards with one link; empty for a tree. Pendant tree: a tree hanging off a 2-core card (its anchor)
by one link. Unit: a pendant child of an anchor together with everything hanging below it, drawn as one rigid shape.
Fan: the arrangement of an anchor's units around it. Stress: the sum over card pairs of w·(distance − target)², the
standard measure of how faithfully a drawing shows graph distances. SGD: stochastic gradient descent, here moving one
pair of cards at a time toward its target distance with a shrinking step size.

## Plan of Work

Milestone 1 adds `graph-layout-stress.ts`: a deterministic solver that, given card sizes, links, optional fixed cards,
optional anchor positions (to keep a previous drawing recognisable) and options, computes hop distances inside each
component, initialises by pivot MDS (classical scaling on distances to up to 64 pivot cards, Brandes and Pich 2006),
and runs SGD over all pairs of a component with the border-to-border targets above, the step-size schedule of Zheng et
al. (η from 1/w_min down to 0.01/w_max over the epochs, exponential decay), plus hinge terms in the second half of the
schedule that push apart cards closer than their size plus a clearance (FORBID-style) and push cards off links that cut
them (ImPrEd-style). Unit tests check determinism, that a path is drawn straight with visible arrow length L, that
pinned cards stay, and that a dense pile ends without overlaps.

Milestone 2 adds `graph-layout-structure.ts` (components, 2-core peeling, pendant forests with parent and child lists,
root choice for trees by the centroid) and `graph-layout-fans.ts` (unit shapes for the four outward directions,
bottom-up; a fan places an anchor's units on the four sides of an inner rectangle, columns on the left and right, rows
on top and bottom, searching the rectangle size that fits all units with the smallest outer box near the requested
aspect ratio, skipping places blocked by other cards or links). Tests: the user's hub graph is drawn without overlaps
or arrows through cards in a box under 1.5M px².

Milestone 3 adds `graph-layout-organic.ts`, the network pipeline: per component, decompose; compute units and each
anchor's fan footprint; lay out the 2-core by stress with each anchor's footprint as its card box; place the fans with
the core links as obstacles; polish by a short stress run with the composed positions as anchors; separate any
remaining overlaps exactly (`separateLayoutRectangles` in `graph-layout-overlap.ts`); then pack components with a
skyline packer that targets the viewport's aspect ratio. `computeGraphLayout` uses it for `network` mode up to 1,500
cards without named groups. Larger graphs keep `computeMultilevelLayout`; named groups keep `computeGroupedLayout`;
`hierarchy` and `square` are unchanged.

Milestone 4 adds `graph-edge-router.ts`: lane bends for parallel arrows (as today, `edgeBendForLane`), and for every
other arrow the first clean option among the centre-aimed straight line, a straight line between facing sides, and a
shortest detour around padded cards through a visibility graph of their corners (drawn as a polyline with rounded
corners). The worker and `routeDrawnGraph` in `graph-edge-routing.ts` both use it.

Milestone 5 moves incremental expansion (new units fanned around their anchor, other new cards at the barycentre of
their placed neighbours, then a stress polish where unmovable cards are fixed and movable old cards are anchored), refine
and repair (polish with strong anchors), and selection-local layouts (pipeline on the selection, placed by
`layoutSubset` in `graph-layout-selection.ts`) to the new engine.

Milestone 6 re-baselines `layout-gates.test.mjs` on the new numbers, rewrites tests that pinned internals of removed
modules as behavioural tests, and deletes modules no longer used. Milestone 7 verifies in the browser and runs the
module verify.

## Concrete Steps

Scorecard (session scratchpad harness, not committed): `run-layouts.mjs <label>` runs `computeGraphLayout` over the
user's reconstructed graph, its expansion flow and fifteen fixture shapes from
`tools/workbench/src/test/frontend/graph-explorer/fixtures`, printing per case: overlaps, arrows through cards (as
drawn, with ports, bends and routes), crossings, curved arrows, width × height, density (card area / box area) and
visible arrow lengths. Unit tests:

    cd tools/workbench
    npm test

Typecheck: `npm run typecheck` in `tools/workbench`. Module verify (also builds the front end with Maven):
`python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench` from the repository root.

## Validation and Acceptance

Acceptance is behavioural. On the user's graph, Organize network draws 37 cards with no overlap, no arrow through a
card and no curved arrow, in a box under 1.5M px² (baseline 7.26M). On every fixture shape, overlaps stay 0, arrows
through cards and crossings do not exceed the current gate budgets, and the area does not grow by more than 10 %
unless crossings or arrows through cards drop. `npm test` passes; the Graph page organizes and expands without errors.

## Idempotence and Recovery

All steps are additive until Milestone 6; the old pipeline stays callable until then, so `computeGraphLayout` can be
switched back by restoring its network branch. The scorecard writes only to the session scratchpad.

## Artifacts and Notes

Baseline scorecard (current engine, aspect 1.6):

    user-hub        3382×2148  7.26M px²  median arrow 732  curved 8   crossings 0
    user-hub-expand 1970×1471  2.90M px²  median arrow 319  curved 10  crossings 0
    geo-tree        3663×2985 10.93M px²  median arrow 109  curved 1   crossings 0
    conference      2022×1574  3.18M px²  arrows through cards 27  crossings 272  curved 75
    students        1615×1597  2.58M px²  arrows through cards 7   crossings 253  curved 34
    hub-75          2733×2406  6.58M px²  median arrow 831  curved 53

## Interfaces and Dependencies

No new npm dependencies. New modules and their main exports:

    graph-layout-stress.ts:    stressLayout(problem, options): StressStats (mutates problem.x / problem.y),
                               drawingCost(problem), untangle(problem, passes, options), segmentHitsBox(...)
    graph-layout-structure.ts: decomposeComponent(neighbors, members, keep?): Decomposition, componentsOf(neighbors)
    graph-layout-fans.ts:      unitShapes(children, size, spacing), placeFan(request): Fan
    graph-layout-packing.ts:   packComponents(items, aspectRatio, clearance): offsets
    graph-layout-organic.ts:   computeOrganicLayout(graph, options): LayoutResult, refineOrganicLayout(graph, options)
    graph-layout-expansion.ts: expansionMovableIds(nodes, edges, anchorId, newIds): Set<string>
    graph-edge-router.ts:      routeEdges(nodes, edges): {bends, routes}

`LayoutOptions` gained `newNodeIds` (the cards an expansion added; the app passes them).
