# Graph explorer style map

How the explorer's stylesheet (`graph-explorer.css`, built to `styles/graph-explorer.css`) relates to the Workbench
design system (`src/main/frontend/static/styles/workbench-refresh.css`). `scripts/css-inventory.mjs --print` lists the
current class inventory; `src/test/frontend/graph-explorer/css-inventory.test.mjs` enforces the rules below.

## Rules

1. Every selector is scoped under `.graph-explorer` (the island root) or `html[data-theme="dark"] .graph-explorer`.
   The stylesheet stays linked after the user leaves the Graph page, so nothing may restyle Workbench elements.
2. No element-level rules (`*`, `html`, `body`, `button`, ...), no `@import`, no web fonts: the island inherits the
   Workbench typography (`--workbench-font`, `--workbench-code-font`, system stacks).
3. Controls are Workbench components, not look-alikes. A class name the TSX uses is either a `workbench-*` class
   (styled by the Workbench) or defined in `graph-explorer.css`.
4. Rules for the explorer's chrome are rooted at `.graph-explorer #graph-explorer-app` (the island's `<main>`). The
   Workbench styles every `<button>`, `<p>`, heading and input on the page surface with element rules (for example
   `body.workbench-body .workbench-page-surface button`, specificity 0-2-2; `p:not(.workbench-field__help)`, 0-3-2),
   and those outranked plain `.graph-explorer .x` rules. Buttons that carry `workbench-action` classes are restyled
   only for the pressed state and a few compact sizes.
5. The explorer's short token names are aliases of Workbench tokens, so the light/dark switch and any future palette
   change of the Workbench apply to the explorer automatically:

   | explorer token | Workbench token |
   | --- | --- |
   | `--ink`, `--body`, `--muted` | `--workbench-ink`, `--workbench-body`, `--workbench-muted` |
   | `--canvas`, `--panel`, `--panel-strong`, `--selected` | `--workbench-canvas`, `--workbench-surface`, `--workbench-inset`, `--workbench-selected` |
   | `--line`, `--line-soft` | `--workbench-outline`, `--workbench-soft-rule` |
   | `--accent`, `--accent-strong`, `--on-accent` | `--workbench-primary`, `--workbench-primary-hover`, `--workbench-primary-text` |
   | `--coral`, `--coral-surface`, `--sand`, `--sand-surface`, `--success` | `--workbench-danger(-surface)`, `--workbench-warning(-surface)`, `--workbench-success` |
   | `--sans`, `--mono` | `--workbench-font`, `--workbench-code-font` |
   | `--radius`, `--control-radius`, `--control-height`, `--shadow` | `--workbench-radius`, `--workbench-control-radius`, `--workbench-control-height`, `--workbench-shadow` |

6. The graph palette is the explorer's own: `--graph-explorer-type-1..6-{stroke,fill}` (handed out to RDF types in the
   order they are first seen, so the first six types of a session never share a colour; untyped resources use the
   neutral loading colour), `--graph-explorer-literal-*`,
   `--graph-explorer-blank-*`, `--graph-explorer-loading-*`, with light values under `.graph-explorer` and dark values
   under `html[data-theme="dark"] .graph-explorer`. `GraphExplorerApp.tsx` references them as `var(...)` strings in
   SVG presentation attributes and inline styles, so the theme switch recolours nodes live. The node drop shadow
   (`feDropShadow`) takes `--graph-explorer-node-shadow(-opacity)`.

## Component map

| Explorer element | Workbench component |
| --- | --- |
| Page title | The Workbench page header; on a full-bleed page (`workbench-page-surface--full-bleed`) it is one compact row (32px icon tile, 20px title, description on the same line, hidden below 600px) — rules in `workbench-refresh.css`. |
| Shell (`#graph-explorer-app.app-shell`) | A Workbench card: surface, soft rule, radius, shadow, on the canvas-coloured island (16px bottom gutter). |
| Toolbar (`.graph-toolbar`) | One row: `Query editor` (`workbench-action--secondary`), the view tabs, history (`--ghost --icon`), Values / Labels / Filters (joined `--secondary` toggles), Layout and Tools menus, full screen (`--ghost --icon`). Icons come from `icons.tsx` (Workbench paths where the Workbench has the icon). |
| View tabs (`.primary-view-tabs`, `.view-menu`) | `workbench-segmented`: joined segments; the selected one has the selected fill and the 3px primary underline. |
| Pressed toggles, open menus | The same selected-segment look (`--selected` fill, 3px primary underline). |
| Menus (`.toolbar-menu-popover`, `.view-menu-popover`) | `workbench-popover` with `workbench-popover__option`-style rows (title + muted description, selected fill on hover); destructive rows in danger colour behind a rule. |
| Canvas overlays (selection bar, zoom, large-graph hint) | Small translucent cards (surface 94%, soft rule, overlay shadow). The selection bar is one row: the count, then Clear / Expand / Hide as `--secondary` buttons. |
| Details card (`.node-details`) | The selected node's card, floating at the top right of the canvas (a sheet along the bottom below 640px) so the graph never reflows: coloured type names, title, identifier with copy, Expand (`--primary`) / Explore / Pin / Hide, then a two-column list of values and of links (links are pill buttons with the linked node's type colour; `←` marks incoming links). |
| Find (`.graph-find`) | A Workbench search input on the canvas's top-left corner (translucent overlay card look, search icon inside); its matches are a `workbench-popover` listbox with label + muted type/IRI rows and the repository search last, in primary colour behind a soft rule. The selection bar sits below it. |
| "+N" badge (`.node-more`) | An 18px pill on a node's top-right corner: surface fill, the node's type colour as outline, 10.5px/700 muted count; primary fill on hover. |
| Statement card (`.statement-details`) | The details card for an edge: "Statement" kind line, predicate title, the subject → object pills, predicate IRI with copy and Explore, description, a two-column list (domain, range, usage), then "About this statement" with one bordered card per reifier / `rdf:Statement` / reference (kind in muted 11px, name, add button, its own two-column rows). |
| Statement buttons (`.node-details-statement`) | A quiet 22px icon button after each value and link in the details card, shown on hover and focus (always on touch); opens the statement card. |
| Statement links (`.statement-link`) | A dashed 1.2px line in the edge colour from a reifier or referrer to a small dot at the middle of the statement's edge; accent colour, 2px, when selected. |
| Chart series (`.chart-series`), more rows (`.table-more`) | The chart's property chooser as joined `--secondary` toggles (name + muted count); "Show N more" under a paged table as a `--secondary` button. |
| Moving canvas (`[data-moving]`, `[data-dragging]`, `.has-many-nodes`) | While the view moves or a node is dragged, node shadows and the overlays' backdrop blur are off (they cost 50 ms a frame); above 150 drawn nodes shadows are always off. |
| Empty state (`.empty-state`) | Centred title, one muted line, `--primary` "Load 100 statements" and `--secondary` "Open query editor". |
| Status bar (`.graph-footer`) | One 32px line: status, drawn graph size, and the type legend (type colour + name as quiet buttons that select the type; pressed = selected fill). |
| Side panels (filters, paths and groups, load history) | 300px panels with a header (15px title, muted count) and a `--ghost --icon` close button; inputs and checkboxes are the Workbench's. |
| Filter panel (`#graph-filter-sidebar`) | Header with the match count and Clear all; active filters as removable chips (selected fill, primary-tinted border); a search field; one disclosure per filter whose closed summary previews its values (or its min – max), checkbox rows with a count bar in the value's colour, from–to inputs for numbers and dates; "N more filters" as a quiet primary text button. |
| Data views | Sticky one-row header (title, count, actions); the triple table follows `table.data` (inset header row, soft rules); entity cards are Workbench cards with a type-coloured top rule. |
| Node actions (`.predicate-menu`) | A `workbench-popover` at the pointer (a bottom sheet below 640px): the node's name, then rows like `workbench-popover__option` (Expand, Details, Expand by relation ▾, selection actions, Hide in danger colour), groups separated by soft rules. |
| Query drawer | Workbench fields (`workbench-field`, 12px/600 labels, Workbench inputs), `--primary` Run query, bordered disclosures for the filter query and the label, language and graph settings; "?" help buttons open in the flow (help-tip.tsx). |

## Layout

The island root (`.graph-explorer`) is sized by `main.tsx` to the viewport height below its own top edge
(`--graph-explorer-height: calc(100dvh - <top>px)`, measured again on window resize and whenever the elements above
it change size), keeps a 16px bottom gutter, and the Graph page's `#workbench-page-surface` gets
`workbench-page-surface--full-bleed` (no width cap, padding or card chrome) while the island is mounted. The page never
scrolls: on `#workbench-app[data-workbench-view="graph"]` the Workbench footer is hidden and the document's vertical
overflow is clipped (rules in `workbench-refresh.css`, which apply before the island loads).

## Responsive layout

The island (`container-name: graph-explorer`) and the graph panel (`container-name: graph-panel`) are size
containers, so the explorer reacts to its own width beside the Workbench sidebar and beside an open query drawer:

- graph panel ≤ 1040px: toolbar buttons show only their icons (names stay in `aria-label` and `title`);
- graph panel ≤ 820px: the menu chevrons and the layout state in the status bar are hidden, side panels overlay the canvas;
- graph panel ≤ 640px: the view tabs take their own row, menus open as bottom sheets, the status bar shows only the status;
- island ≤ 900px: the query drawer overlays the graph; island ≤ 640px: node actions open as a bottom sheet.

Touch devices and windows ≤ 900px get the Workbench's 44px control height. `prefers-reduced-motion` stays a media query.

## Large-graph canvas

`graph-canvas-controller.ts` reads `--graph-explorer-canvas-*` (edge, node, label plate, group, labels, selection)
from the island at draw time and passes the edge and node colours into the WebGL layer, so the 500+ node renderer
follows the theme too; the WebGL edge layer of 180–499 node graphs reads the same edge and node tokens.
