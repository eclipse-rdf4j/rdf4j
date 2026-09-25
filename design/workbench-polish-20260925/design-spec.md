# RDF4J Workbench consistency polish

This specification applies one shared component system across all Workbench route families and embedded query results. The accepted image references are concept-query.png, concept-add.png, concept-information.png, and concept-mobile-saved.png in this directory. Their native dimensions are 1487x1058, 1505x1045, 1504x1046, and 783x2008 pixels respectively. The mobile Saved Queries image is a tall state reference; verify actual responsive behavior at 320px, 390px, and 768px as well as desktop at 1440px.

## Visual system

Use one set of centrally owned tokens and shared component rules for the main Workbench shell, page surfaces, fields, buttons, disclosures, table headers and rows, and query-result controls. Apply those rules to the embedded result document too. Scope the selectors to Workbench page and embedded-result roots so unrelated legacy application pages retain their styles. Keep the existing native font family.

The palette is fixed: canvas #f1f5f9; surface #fff; inset surface #f8fafc; ink #0f172a; navigation and secondary ink #334155; muted ink #64748b; fine separators #e2e8f0; control border #cbd5e1; primary action #0f766e with hover/pressed #115e59; danger #b42318. White surfaces stay white. Use one light shadow such as 0 2px 8px rgba(15,23,42,.05), a 10px surface radius, and a 7px control radius. Avoid gradients in shared controls and surfaces.

Keep the sidebar at 184px. Desktop uses 24px page gutters, surface insets, and inter-surface spacing. Narrow pages use 12px outer gutters and 16px inner padding. Main headings are 28px desktop and 24px mobile at semibold weight; section headings are 18px, Information subsections 16px, body text 14px with 1.5 line-height, controls/navigation 13px, labels 12px semibold, and code 14px desktop/13px narrow.

Controls use 36px minimum height on desktop and 44px on mobile. Shared padding, gaps, radius, border, and icon alignment apply to buttons, input actions, label actions, summaries, selects, and text fields. Keep keyboard behavior, visible focus rings, and reduced-motion support. Existing outline icons use a consistent 16–18px optical size and 1.75px stroke; change only mismatched metaphors such as saved-query Execute, Show/Hide, and Edit.

## Shared shell and page families

The shared header keeps the RDF4J mark, server/repository/user context, and existing routes and values. Change links use slate styling without literal square brackets. The navigation and active-route treatment use the shared ink, borders, spacing, and icon family.

Shared page surfaces, form labels, buttons, fields, disclosures, and table rules match across query/results, repository administration, Explore/lists, creation, data transfer, update/removal/clear, saved queries, system information, server/login, and empty/error/loading/cancelled states. Query editor, Explain/Compare/Diff controls, embedded tuple/graph results, paging, and downloads keep their current hierarchy and behavior while using the same component typography and spacing.

Tables use a consistent subtle header and horizontal row separators on repositories, namespaces, contexts, types, Explore, and query results. Preserve Auto/Table/Records choices, wrapping, complete term values, paging, term links, and frame scrolling.

## Add RDF

Present the existing File, URL, and Text native radio choices as three equal-width segments. The selected segment is clear through its border/fill and remains reachable by keyboard. Keep the existing radio names, values, labels, field states, and File/URL/Text workflows. Style the native file selector button with the common control tokens. Preserve Format, helper text, Advanced settings, and Upload order and behavior.

## Saved Queries

Desktop actions use flexible natural widths. At narrow mobile widths, use two equal columns with 8px gaps: Execute and Link on row one, Show/Hide and Edit on row two, and Delete alone in the first cell on row three. Every action target is at least 44px high. Leave 16px before metadata. Detail metadata uses aligned label/value rows with horizontal separators, wrapping titles and user/source content, and no heavy boxed grid. Keep query code readable and unclipped.

## System Information

Application, Runtime, and Memory all appear inside one white information surface. Separate sections with fine horizontal lines. On desktop, label/value rows align to a 160px label column. On mobile, columns adapt and long runtime values wrap. Display only the runtime values already supplied by the application; do not invent metrics.

## Intentional correction to generated references

The generated images communicate page composition and hierarchy. Their incidental colors and decorative details do not override the locked visual system: Change links remain slate instead of teal; surfaces and controls remain flat and restrained instead of gaining gradients; the primary, secondary, and danger colors follow the exact values above. No content, route, field, default, navigation, or workflow may be invented or removed to match an image detail.

## Copy and behavior boundaries

Keep all existing visible labels, headings, helper text, repository and server values, runtime values, and user data. The images are illustrative samples, not replacement copy or seed data. Preserve the Workbench query execution and cancellation lifecycle, result rendering and paging, native downloads, radio selection, file chooser, saved-query actions, existing form fields, and server-side behavior. A restored paged-result heading may read `Query Result (1–3 of 3)` where the concept abbreviates it to `Query Result (3)`; retain the range because it reflects existing pagination semantics.
