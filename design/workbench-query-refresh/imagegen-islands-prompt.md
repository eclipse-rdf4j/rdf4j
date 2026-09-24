# Tailwind islands mockup

Mode: built-in image_gen. This is the latest visual direction, reflecting the user's request for stronger shadows and islands.

Source output: `/Users/havardottestad/.codex/generated_images/01a0cd2e-e5d2-7f80-87d4-3599f2514de0/exec-311a76a3-fcc7-4a84-9930-99cc51b61098.png`

Reference image: `concept-a-tailwind.png` (brand, controls, and workflow).

## Exact prompt

Use case: ui-mockup. Revise the attached RDF4J Workbench mockup to go SIGNIFICANTLY FURTHER toward a contemporary Tailwind CSS application, specifically with softly elevated WHITE ISLANDS and obvious but tasteful SHADOWS. Treat the attached image as the edit target for product content and workflow; preserve the RDF4J orange-and-black brand, the grouped navigation, query editor, settings, action buttons and embedded results.

This must be visibly more designed than a flat legacy page. Use a solid slate-100 (#f1f5f9) application canvas. Keep a white compact 72px header. Below the header, create three clearly separated elevated white islands with breathing space: (1) a compact left navigation island, (2) the complete query workspace island on the right, (3) the query results island beneath it. The gray canvas must be visibly exposed between and around islands. Use 24px outer gutters and 24px gaps, 12–16px rounded-xl island corners, thin subtle inset border/ring in slate-900/5, and soft layered Tailwind shadow-md/soft shadow-lg elevations, approximately 0 1px 3px rgba(15,23,42,.08) plus 0 8px 24px rgba(15,23,42,.06). Shadows must be visible around the cards, especially below and to the sides, while remaining professional. No gradients or glass. Do not split every setting into another card. Make the surfaces feel tactile and organized, not decorative.

Typography: crisp Inter-like sans-serif, slate-900 headings, slate-600 supporting text. 28px semibold “Query Repository” heading above the main islands. Comfortable 24px island padding, 14px medium labels, 8px corner form controls with 36px height and subtle control shadows. Dark slate-900 primary Execute button with white text, quiet white bordered secondary actions. Blue-600 links, checkboxes and focus accents. Orange only in the authentic RDF4J logo. Active Query navigation: pale blue/slate rounded inset selection with dark text, NOT coral or orange. Distinct from GraphDB: no solid coral nav row or action buttons, no stacked query tabs or right icon rail.

Maintain the existing app information and useful controls. Header: “rdf4j / workbench” brand left; readable small RDF4J Server / Repository / User context right. Navigation categories RDF4J Server, Explore, Modify, System with current labels.

Query island: small “Query” heading then white monospace SPARQL editor with a very light slate gutter and line numbers, ~240px high:
SELECT ?s ?p ?o
WHERE {
  ?s ?p ?o .
}
LIMIT 100
Below it, a well-spaced compact settings band with Results per page = 100, Query timeout (seconds) = 0, Include inferred statements checked, Save privately (do not share) unchecked. Checkbox and its own label stay together. Then labeled action controls: Clear, Execute, Explain Query, Save Query, Query name input. All editor/settings/actions remain within ONE query island.

Results island: “Query Result” heading inside its top padding. A clean two-row toolbar. Row 1: Download format = SPARQL/CSV, Download limit = All, Download button. Row 2: Results per page = 100, Results offset = 0, Previous / Next, Show data types & language tags checkbox. Keep EVERY control, including Download limit. Then a readable table with aligned s, p, o columns, 5 realistic RDF rows, blue URIs and dark quoted literals, soft slate header, fine horizontal row dividers, barely tinted alternating rows. Fit the card naturally around the short results table and toolbar with 24px bottom padding, not a huge blank area. No fake charts, statistics, avatars, decorative badges, marketing content or browser/device frame.

Output one crisp full desktop application screenshot, approximately 1440px wide, enough height for all three islands to be visible. Emphasize the elevated island composition and material shadows; this is a bolder Tailwind-style refinement while keeping RDF4J's workflow.
