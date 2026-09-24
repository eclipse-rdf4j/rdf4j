# Workbench query refresh concepts

These PNGs are visual references generated with the built-in imagegen tool. They are not production assets and must not be rendered by the Workbench UI.

## Concept A: restrained vertical workflow

Reference: [concept-a-restrained.png](concept-a-restrained.png)

The editor, query settings, actions, and latest results stay in one vertical reading order. It keeps the existing interaction model easiest to recognize, gives the result table a clear full-width surface, and keeps the orange RDF4J accent on the existing logo while using dark slate for primary actions and blue for links and focus. This is the most conservative direction and the lowest-risk fit for the current XSL structure.

The selected refinement is [concept-a-tailwind-simple.png](concept-a-tailwind-simple.png), generated with the built-in imagegen tool from [imagegen-simple-prompt.md](imagegen-simple-prompt.md). It keeps the same vertical structure while collapsing infrequent query and result controls into native disclosures. The reference uses white workspace islands on a slate canvas, soft shadows, dark-slate Execute, blue links and focus, and the existing orange RDF4J logo. The image is a design reference, not a production asset; the implementation keeps the real Workbench labels, controls, and request behavior.

## Concept B: compact action-first workflow

Reference: [concept-b-compact.png](concept-b-compact.png)

Actions move above the editor and the controls become denser. This uses the available desktop width efficiently and reduces vertical travel, but it changes the current form order and gives the query name field a less natural relationship to Save Query. Narrow layouts still need careful wrapping.

## Concept C: options beside the editor

Reference: [concept-c-options.png](concept-c-options.png)

The editor and query options share a bounded workspace, with results in a separate surface below. It makes settings visible while editing and creates a strong app-like hierarchy, but it introduces a desktop two-column breakpoint and needs a clean collapse to a wrapped vertical form on narrow screens. It carries the most XSL/CSS layout risk.

## Shared constraints

- Keep the existing RDF4J branding, header, repository context, and navigation.
- Keep the editor and latest results vertically stacked; do not add result tabs or history.
- Limit the redesign to scoped query-page and embedded-result classes so other Workbench pages retain their current styling.
- Reserve orange for the existing RDF4J logo; use dark slate primary actions, blue links/focus, subdued neutral surfaces, readable type, and compact but accessible controls.
- Desktop navigation stays at or below 200px, with 24px main gutters and a compact header/repository context.
- On narrow screens, the header wraps, the existing navigation becomes a wrapped section list above content without adding a new interactive toggle, the main content uses the full width, controls wrap, and result tables scroll inside the result surface.
- Preserve Compare/Explanation availability and behavior, including their existing controls and cancellation paths.

## Concept A wire diagrams

The selected direction is documented with deterministic, code-authored SVG wires and rendered PNG previews:

- Desktop wire: [wire-desktop.svg](wire-desktop.svg) · [wire-desktop.png](wire-desktop.png)
- Narrow wire: [wire-narrow.svg](wire-narrow.svg) · [wire-narrow.png](wire-narrow.png)

The desktop wire keeps the header compact, the navigation near 190px, and the query/result surfaces vertically stacked with 24px main gutters. It places loading and Cancel above the result frame and keeps errors in the result status line. The narrow wire wraps the header and navigation above a full-width workspace, wraps controls, and confines table overflow to the result surface.

The default-state wires show Query Options and Save query collapsed, with Execute, Explain Query, Previous, and Next available immediately. An expanded-options state is represented by [wire-expanded-options.svg](wire-expanded-options.svg) and [wire-expanded-options.png](wire-expanded-options.png); it documents the existing page-size, timeout, inferred-statements, save-name, privacy, download, and datatype controls without inventing new actions.

## Implementation previews

The final Chromium QA run captured real three-row tuple results at both target widths:

- Desktop 1440px: [implementation-desktop.png](implementation-desktop.png)
- Narrow 390px: [implementation-narrow.png](implementation-narrow.png)

The implementation previews are verification artifacts, not rendered application assets. The refreshed final-state previews are [implementation-desktop.png](implementation-desktop.png),
[implementation-narrow.png](implementation-narrow.png), and the expanded disclosure state
[implementation-expanded.png](implementation-expanded.png). The final 12-case Chromium layout run is
recorded in `e2e/query-refresh-layout-final-review-green.log`.
