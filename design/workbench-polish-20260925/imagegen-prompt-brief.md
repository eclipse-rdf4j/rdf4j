# Selected concept prompt summaries

These are concise summaries reconstructed from the four accepted images and the locked design specification. They are not the exact prompts sent to image generation. The user's intent was to make the existing RDF4J Workbench feel consistent across pages while preserving routes, copy, fields, defaults, data, and workflows.

## Shared constraints for all four screens

Use the existing RDF4J Workbench shell, mark, navigation, and native font family. Keep the 184px sidebar; use a 24px desktop gutter/inset/gap and 12px mobile outer gutter with 16px inner padding. The palette is fixed: canvas `#f1f5f9`, true-white surface `#fff`, inset `#f8fafc`, ink `#0f172a`, secondary/navigation `#334155`, muted `#64748b`, separators `#e2e8f0`, control edge `#cbd5e1`, flat primary `#0f766e`/`#115e59`, danger `#b42318`. Keep the 10px surface and 7px control radii, one light shadow, 28px/24px desktop/mobile page headings, 18px section headings, 14px body text at 1.5 line height, 13px controls/navigation, 12px semibold labels, 14px/13px code, and 36px/44px desktop/mobile controls. Use 16–18px outline icons with a 1.75px stroke, visible keyboard focus, and reduced-motion support. Treat screenshot composition as the visual reference; these numeric rules correct incidental image colors and effects.

## Query Repository — desktop, 1487×1058

Show the shared Workbench header and navigation beside a clear Query Repository heading. Give the SPARQL editor a prominent white surface with Query label, Execute and Explain actions, and Save query and Options utilities. Place a distinct Query Result surface below with a compact toolbar, a three-row illustrative result table, and paging. Preserve the real query editor, execution lifecycle, result controls, and paging behavior. The selected concept labels its heading `Query Result (3)`; a live populated result may show `Query Result (1–3 of 3)` to retain the existing page-range semantics.

## Add RDF — desktop, 1505×1045

Reuse the same shell, heading, surfaces, fields, and actions. Present File, URL, and Text as three equally sized source choices with a clear selected state. Keep Format and its helper text, Advanced settings, the source-specific inputs, and Upload in their current workflow order. Preserve native, labeled, keyboard-operable radios and the native file chooser; the visual treatment must not replace their behavior.

## System Information — desktop, 1504×1046

Keep the shared shell and show Application, Runtime, and Memory together in one white information surface. Separate sections with fine horizontal rules. Align labels and values using a 160px desktop label column; let long values wrap on narrow screens. Display only the dynamic values already supplied by RDF4J.

## Saved Queries — mobile, 390×1000 logical composition

Keep the existing mobile navigation and saved-query content in one calm, readable column. Use two equal action columns with Execute/Link, Show/Hide/Edit, and Delete alone on the next row. Use 44px targets and 8px action gaps, followed by a 16px metadata gap. Present metadata as aligned label/value rows with fine separators and wrapping content. Keep expanded query code readable and within the viewport. The accepted raster is 783×2008 pixels, approximately a 2× image of the 390×1000 logical composition, not a tablet breakpoint.
