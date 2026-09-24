# Tailwind-directed mockup prompts

Mode: built-in image_gen tool. The existing Concept A image was used as a brand and workflow reference. Originals remain in the generated-images directory.

Design references: [Tailwind colors](https://tailwindcss.com/docs/colors), [Tailwind utility styling](https://tailwindcss.com/docs/styling-with-utility-classes). The [GraphDB reference](https://bsdd.ontotext.com/img/graphdb-sparql.png) was inspected for visual differentiation, not used as image-generation input.

## Palette refinement

Source output: `/Users/havardottestad/.codex/generated_images/01a0cd2e-e5d2-7f80-87d4-3599f2514de0/exec-8064ed9f-7bee-4137-9f6a-06efe52caf92.png`

Refine the attached RDF4J Workbench Concept A UI prototype into the final restrained visual direction. Preserve RDF4J's existing orange-and-black logo, grouped text-based navigation, Query Repository title, SPARQL editor, settings and ordinary labeled controls, and embedded results directly below the editor. Use the visual discipline of a generic Tailwind utility interface WITHOUT inventing a new app or copying a vendor: white and very light slate (#f8fafc) surfaces, slate ink (#0f172a), secondary slate text (#475569), fine cool-gray rules (#e2e8f0), restrained 6px corners, system sans-serif and a monospace code editor. Execute is a dark slate button with white text; secondary buttons are white with gray borders. Selected Query navigation has a light slate background and dark text with a small slate leading rule. Links and keyboard focus accents are blue (#2563eb). Orange appears ONLY in the existing RDF4J logo, not in buttons, navigation, checkboxes or result bars. No coral, gradients, saturated sidebar blocks, stacked document tabs, right-side icon rail, or mint result status bar. Keep the screenshot a useful, compact desktop query workspace: editor roughly 240px high, compact settings with Results per page, Query timeout (seconds), Include inferred statements and Save privately (do not share), labeled Clear/Execute/Explain Query/Save Query controls plus query name. Include all existing result controls, including Download format, Download limit, Download, Results per page, Previous/Next pagination and Show data types & language tags. Table with three aligned columns s, p, o and realistic RDF values, 5-8 readable rows; result area fits the short table without excessive blank space. Preserve conservative spacing and recognizable RDF4J identity. This is a flat high fidelity UI design prototype at desktop width, not a device mockup. Render crisp, legible labels.

## Fresh Tailwind mockup

Source output: `/Users/havardottestad/.codex/generated_images/01a0cd2e-e5d2-7f80-87d4-3599f2514de0/exec-7736310d-770f-40fb-acee-079fd8ade910.png`

Use case: ui-mockup.
Create a NEW high-fidelity desktop mockup of the RDF4J Workbench query page, rendered like a real modern app built with Tailwind CSS. The attached image is a reference ONLY for RDF4J branding, existing navigation groups, and required controls. Recompose and restyle the interface; do not simply recolor the reference.

Design direction: a restrained, practical developer tool. Tailwind-like visual system: bg-slate-50 page canvas, bg-white header/sidebar/editor/results, text-slate-900 headings, text-slate-600 secondary text, border-slate-200 dividers, rounded-lg 8px controls/surfaces, a very subtle shadow-sm on editor surface, font-sans in a clean Inter-like style, font-mono for code. Use a consistent 4px spacing scale, 24px main padding, 16px gaps, 36px control heights, 14px medium-weight field labels, 28px semibold page heading. Avoid glossy gradients, heavy borders, excessive cards or giant whitespace. The primary Execute button is slate-900 with white text. Links and focus accents are blue-600. Orange is reserved for the existing RDF4J logo. The overall palette should read cool neutral slate, white, dark ink and quiet blue.

Layout at approximately 1440px wide: a compact 72px white header with the authentic orange-and-black rdf4j / workbench brand at left, server and repository context as small readable text at right. A 192px white left sidebar separated by a fine vertical border, text navigation grouped under RDF4J Server, Explore, Modify, System. Existing navigation labels, with Query selected using a pale slate rounded background, dark bold text and a subtle slate left rule. No solid colored sidebar, no coral/orange interaction styling, no stacked query tabs, no right-side icon rail. This must be visually distinct from GraphDB.

Main workspace: heading “Query Repository”. One coherent editor workspace. A small “Query” label, then a large clean white syntax-highlighted SPARQL editor with line numbers, around 240px tall and 8px corners. Example code:
SELECT ?s ?p ?o
WHERE {
  ?s ?p ?o .
}
LIMIT 100

Below the editor, compact aligned settings that retain Results per page (100), Query timeout (seconds) (0), Include inferred statements (checked), Save privately (do not share) (unchecked). Keep each checkbox next to its own label. Then one useful row of labeled controls: Clear, dark Execute, Explain Query, Save Query, and a Query name input. Preserve the original workflow and all controls.

Below, “Query Result” as a compact section heading followed by a tidy two-row toolbar that actually contains every control: first row Download format dropdown (SPARQL/CSV), Download limit dropdown (All), Download button; second row Results per page (100), Results offset (0), Previous and Next buttons, Show data types & language tags checkbox. Do not omit Download limit. Render realistic form controls, no decorative badges.

Results table directly below: three correctly aligned columns s, p, o, with 6 realistic RDF rows containing blue URIs and dark literal values. Fine horizontal row dividers, very light slate table header, no heavy cell boxing. The result surface fits the table compactly. Use readable text and polished alignment, clean hierarchy, plenty of useful editor width. No fake charts, avatars, marketing copy, ornamental illustrations, extra features or browser/device frame. Output only the full app mockup.
