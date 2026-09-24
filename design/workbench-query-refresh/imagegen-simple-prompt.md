# Simplified Tailwind query mockup

Mode: built-in image_gen. This is the latest visual direction, with query, saving, download, and result settings collapsed by default.

Source output: `/Users/havardottestad/.codex/generated_images/01a0cd2e-e5d2-7f80-87d4-3599f2514de0/exec-99a09725-ba97-4462-8035-33a98eb550cc.png`

Reference image: `concept-a-tailwind-islands.png`.

## Exact prompt

Use case: ui-mockup. Edit the attached elevated Tailwind-style RDF4J Workbench mockup to make the DEFAULT UI substantially SIMPLER through progressive disclosure, while retaining the slate canvas, white islands, soft layered shadows, rounded-xl corners and RDF4J identity.

Preserve the overall three-island composition: compact left navigation island, query workspace island on the right, results island below. Solid slate-100 canvas visible between cards, 24px gaps, white surfaces, slate-900 type and Execute button, blue links/focus, orange only in the original RDF4J logo. No coral or orange controls, no GraphDB-like tabs/icon rail. Keep the brand and compact server/repository header.

Default state must feel quiet and immediately useful. In the query island show a small “Query” heading and the full-width SPARQL editor with line numbers, about 250px tall:
SELECT ?s ?p ?o
WHERE {
  ?s ?p ?o .
}
LIMIT 100
Below the editor show ONE compact action row: a dark “Execute” primary button, a white outlined “Explain Query” button, then quiet “Save query” with a small down chevron. Align a quiet “Options” control with a small down chevron at the far right. All disclosures are CLOSED. Do not show the Results per page field, Query timeout, inferred checkbox, private checkbox, query-name input, or Clear button in this default view. Those controls exist behind “Options” or “Save query” and must not clutter the screenshot. No summary chips, settings badges or explanatory filler text.

The results island heading is “Query Result”. Its compact heading/toolbar area includes a quiet “Download” with down chevron and “Result options” with down chevron. These disclosures are CLOSED. Hide Download format, Download limit, results page-size selector, offset input and datatype checkbox from the default view. Retain simple Previous and Next pagination controls near the table, with Previous disabled for this initial page. Do not invent new result-mode tabs or charts.

A readable three-column results table with headers s, p, o and 7 realistic RDF rows, blue linked URI values and dark quoted literals. A soft slate table header, thin horizontal row dividers and very lightly alternating rows. Table and toolbar occupy one white island with soft shadow and 24px internal padding. The card fits its contents without excess blank space. Keep navigation labels and current Query selection as in the reference; no new decorative content or giant icons.

The new screenshot should visibly demonstrate: a calm editor, obvious Execute action, immediately readable embedded results, settings out of the way but easy to find. Do not draw any expanded menu, settings panel, popover, annotations, or explanatory callout. Render one crisp desktop application screenshot around 1440px wide, no browser/device frame.
