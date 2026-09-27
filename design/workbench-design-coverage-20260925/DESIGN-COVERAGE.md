# RDF4J Workbench design brief and coverage map

## Purpose and boundaries

Create a modern, precise Workbench design for configuration, query, import, result, and repository-management tasks. The Workbench is a developer utility: prioritize clear hierarchy, compact controls, aligned labels, readable long values, progressive disclosure, and predictable keyboard operation. Use restrained surfaces with fewer nested containers. Keep dense configuration usable instead of hiding meaningful settings behind decorative treatments.

The user’s palette constraint is strict: use cool slate, navy, and the computed complementary blue for primary actions, selection, checked controls, and focus. **Use no orange anywhere except the existing RDF4J logo.** This includes syntax colors, warning/error treatments, status marks, borders, badges, icons, and illustrations. The calculated CSS tokens and measured 8-bit contrast evidence live in [PALETTE.md](PALETTE.md) and `palette.json`; use those exact hex values for CSS and generation prompts. Do not claim that color mathematics proves aesthetic quality.

Use Tailwind CSS conventions and contemporary shadcn/ui and Radix patterns as visual references for spacing, accessible states, and control composition. These are design references only. No Tailwind, React, shadcn/ui, Radix, or other runtime migration or dependency change is authorized by this brief. Preserve the current server endpoints, parameters, defaults, validation, response types, and native keyboard semantics. Do not invent new product actions or change backend defaults. Image text and query snippets below are source-backed where identified; any illustrative result data must be labeled as sample data and must not be presented as an RDF4J default.

The first generated query exploration was rejected and is archived only for provenance. Two later light/graphite concepts remain alternative explorations. The copied slate-blue query candidate is a working visual reference, **not an approved final design**. Generated rasters are approximate, including their color and logo rendering. See [the image catalog](index.html) for asset status; the exact CSS preview and original logo are authoritative. `image-prompts.json` keeps the prompt attempts and selected asset paths for each board.

Preserve the original logo unchanged from [`tools/workbench/src/main/webapp/images/logo.png`](../../tools/workbench/src/main/webapp/images/logo.png), a 132×80 RGBA PNG. A byte-for-byte copy is included as [the logo reference](images/rdf4j-logo-reference.png). The orange mark in this exact asset is the sole exception to the palette rule. Do not use generated approximations, recolor, crop, or redraw it. `product.png` is a separate product asset and is not a replacement for `logo.png`.

## Source of truth and rendering flow

The navigation and common page shell are defined by `tools/workbench/src/main/webapp/transformations/template.xsl` (navigation entries around lines 220–366). The menu groups Server; Repositories with Create/Delete; Explore with Summary, Namespaces, Contexts, Types, Explore, Query, Saved Queries, Export; Modify with Update, Add, Remove, Clear; and System with Information. Preserve the server/repository/user context and Change links. Keep grouped navigation discoverable when collapsed, with current context and selected route apparent. There is no Settings route, Overview route, or Import label: the source actions are Summary and Add. Some routes are disabled according to repository read/write permissions. `repositories.xsl`, `contexts.xsl`, and `types.xsl` render data tables; `list.xsl` and `table.xsl` are shared table fragments rather than independent routes.

Create rendering is metadata-backed. `CreateServlet.service` selects `create.xsl` for the type chooser, `create-federate.xsl` for Federation, and `create-template.xsl` for the other 17 templates. The live values come from the repository `ConfigTemplate` Turtle files under `core/repository/api/src/main/resources/org/eclipse/rdf4j/repository/config/`, and `CreateTemplateConfig` selects the first template value as the default. `create.js` keeps fields with repository-ID/title roles in the core view and moves optional configuration into Advanced. Older per-backend XSL files remain in the tree; use the live Turtle templates and `CreateServletTest` / `CreateTemplateConfigTest` contracts for generated text and defaults, not a stale standalone XSL form.

## Workbench defaults to preserve

| Control | Current default or rule | Source |
| --- | --- | --- |
| Query language | SPARQL | `tools/workbench/src/main/webapp/WEB-INF/web.xml`; selected from the request/info value in `query.xsl` |
| Results per page | 100 | `WEB-INF/web.xml`; `template.xsl` offers All, 10, 50, 100, and 200 |
| Include inferred statements | On (`true`) | `WEB-INF/web.xml`, `query.xsl` |
| Query timeout | 60 seconds when no request or configured value is supplied; a saved/request value such as 0 remains 0 | `WEB-INF/web.xml`, `query.xsl` |
| Download format | `application/rdf+xml` for both Accept and Content-Type | `WEB-INF/web.xml`; actual format choices come from runtime info |
| Result layout | `auto` | `tuple.xsl`, `graph.xsl` |
| Wrap values | On | `tuple.xsl`, `graph.xsl` |
| Show datatypes | On for tuple results; Explore’s show-datatypes checkbox is on | `tuple.xsl`, `explore.xsl` |
| Explain format and level | Text and Optimized | `query.xsl`; output may be Text, DOT, or JSON, and level may be Unoptimized, Optimized, Executed, Telemetry, or Timed |
| Explain text highlighting | Normal/syntax; Heatmap is optional | `query.xsl` |
| Add source and format | File selected; format autodetect | `add.xsl` |
| Add context override | Off by default; when explicitly enabled, the supplied Context replaces parsed contexts. With no override, preserve embedded contexts where the format supports them; contextless data goes to the default graph. Base URI affects relative URI resolution only. | `add.xsl`, `AddServlet`, `RepositoryConnection.add` |
| Add transaction isolation | Empty/server default option until a value is supplied | `add.xsl` and its servlet-provided isolation options |
| Delete repository | Blank option initially; SYSTEM repository is excluded | `delete.xsl` |
| Remove examples | Disclosure initially closed; subject, predicate, context are request-backed and object starts empty | `remove.xsl` |
| Clear context | Request-backed or empty; warning is visible | `clear.xsl` |
| Namespaces | Prefix and namespace inputs empty; existing-prefix chooser has a blank option | `namespaces.xsl` |
| Saved-query timeout | 0 only when stored query has no timeout; remaining query metadata is per record | `saved-queries.xsl` |
| Repository Summary config | Turtle config disclosure closed by default | `summary.xsl` |

For Add, the context override is opt-in. With no explicit `context`/override, keep parser-provided contexts and let contextless statements use the repository default graph. Base URI only resolves relative identifiers and must not be converted into a graph context. The Context input stays empty and disabled until the user enables the override. URL and Text source values begin blank.

On a fresh query, render the editor empty unless a query is supplied by the current request or saved-query action. The code sample and people/IRIs in the working reference image are illustrative; they are not defaults. If a sample query is needed to make the visual target legible, label it as sample and use syntactically valid, generic SPARQL. Keep Run query directly below the editor and Explain beside it below the editor; do not move either above the editor or hide Run. Preserve the existing Clear action on Query Options; it currently calls `workbench.query.resetNamespaces()` and is not a “restore defaults” action.

## Complexity and disclosure

Default screens should show task-essential fields and primary actions with useful selected-value summaries. Put less frequent controls behind concise, named disclosures or popovers such as Advanced, Query options, Display, and Download. Provide paired default-closed and expanded design states for complex repository forms, query/explain settings, result panels, imports, and configuration text. Indicate non-default settings when a disclosure is closed. Preserve all fields, labels, keyboard operation, and behavior; never hide essential query fields or Run query, and do not substitute unnamed ellipsis menus for important configuration. Navigation groups may collapse while retaining current page and server/repository context.

## All 18 repository configuration variants

The seven memory and seven native entries are one shared metadata-backed form pattern with distinct template identity and capability. Show one readable full form for each family and a separate legible configuration board that maps all family variants and their exact titles/defaults; do not use a seven-thumbnail collage. Pair each complex form's uncluttered default-closed view with a separate, legibly expanded Advanced view. Fields unique to a backend must be shown at useful size. For other fields, show the actual rendered Advanced disclosure and its source defaults.

| Type | Default ID | Default title | Template-specific visible meaning / options |
| --- | --- | --- | --- |
| `memory` | `memory` | Memory store | Persist=true, Sync delay=0, evaluation STRICT (STANDARD available); iteration cache sync threshold 10000; slow-query thresholds 0/0 and log path blank |
| `memory-lucene` | `memory-lucene` | Memory store with Lucene Support | Memory shared options plus Lucene-backed text index |
| `memory-rdfs` | `memory-rdfs` | Memory store with RDFS inferencing | Memory shared options plus RDFS inferencing |
| `memory-rdfs-dt` | `memory-rdfs-dt` | Memory store with RDF Schema and direct type inferencing | Memory shared options plus direct type hierarchy behavior |
| `memory-rdfs-lucene` | `memory-rdfs-lucene` | Memory store with RDFS inferencing and Lucene index | Memory shared options plus RDFS and Lucene |
| `memory-customrule` | `memory-customrule` | Memory store with custom graph query inferencing rule | Query language SPARQL; Rule query defaults to `CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(false) }`; optional matcher blank; memory shared options |
| `memory-shacl` | `memory-shacl` | Memory store with SHACL | Memory shared options plus SHACL |
| `native` | `native` | Native store | Triple indexes=`spoc,posc`; evaluation STRICT; iteration cache sync threshold 10000; slow-query thresholds 0/0 and log path blank |
| `native-lucene` | `native-lucene` | Native store with Lucene Support | Native shared options plus Lucene-backed text index |
| `native-rdfs` | `native-rdfs` | Native store with RDF Schema inferencing | Native shared options plus RDFS inferencing |
| `native-rdfs-dt` | `native-rdfs-dt` | Native store with RDF Schema and direct type inferencing | Native shared options plus direct type hierarchy behavior |
| `native-rdfs-lucene` | `native-rdfs-lucene` | Native store with RDFS entailment and Lucene Support | Native shared options plus RDFS and Lucene |
| `native-customrule` | `native-customrule` | Native store with custom graph query inferencing rule | Query language SPARQL; same false-filter CONSTRUCT and blank optional matcher; native shared options |
| `native-shacl` | `native-shacl` | Native store with SHACL support | Native shared options plus SHACL |
| `remote` | `SYSTEM@localhost` | SYSTEM repository @localhost | RDF4J Server location=`http://localhost:8080/rdf4j-server`; Remote repository ID=`SYSTEM` |
| `sparql` | `endpoint@localhost` | SPARQL endpoint proxy @localhost | Query endpoint and update endpoint are blank; show placeholders `http://example.org/sparql` and `http://example.org/update` as hints, never as values |
| `federate` | `fed` | Federation | Available repository member checkboxes initially unselected; validation asks for at least two and prevents an ID collision |
| `lmdb` | `lmdb` | LMDB Store | Full LMDB tunables are listed in the LMDB boards below; standard query evaluation is selected |

All seven Memory templates share Persist=true, Sync delay=0, Query Evaluation Mode=STRICT (STANDARD is available), cache threshold 10000, slow-query thresholds 0, and blank slow-query log path. All seven Native templates share Triple indexes=`spoc,posc`, Query Evaluation Mode=STRICT (STANDARD available), cache threshold 10000, slow-query thresholds 0, and blank slow-query log path. Custom-rule templates use the same SPARQL rule default and blank matcher above. Feature differences are selected by repository type/template; do not add invented feature checkboxes.

The active SPARQL endpoint template stores both endpoint values blank and supplies example placeholders. An older static XSL has `http://` as a value, but the active generic renderer uses the current Turtle config default. Keep the active empty defaults. The LMDB template has many fields, so reveal real groups and retain the exact selected values: cache threshold 2097152; slow-query thresholds 0/0 and path blank; indexes `spoc,posc,ospc`; bulk size 256; triple/value DB sizes 1073741824 each; value/value-ID caches 4096 each; namespace/namespace-ID caches 128 each; auto-grow=true; page cardinality estimator=true; value eviction interval 300000; value hash cache=false; inline literals=true; sketch estimator enabled=true; subject/predicate/object/context buckets 4096/64/4096/16; context-pair sketches=false; sketch throttle every N=1048576 and millis=2; optimizer sampling=true with max 2 ms/4096 rows; background raw sampling=true with max 10 ms/cycle; force sync=false; no readahead=false; query evaluation STANDARD. Preserve the “unset” option for sketch subject buckets supported by the config metadata.

## Board inventory: 32 required image assets

Each prompt in `board-specs.json` requests a specific page or component target. When one image contains two route/page views, the prompt requires separate, legible, individually titled full-page compositions; do not blend them into one generic layout. The 7 Memory and 7 Native templates are the only repeated page variants condensed into a representative full page plus a readable variant/default board, as their shell and option model are shared.

| ID | Board title | Route(s) / required target |
| --- | --- | --- |
| B01 | Shared shell | Common `template.xsl` page shell, actual navigation groups, collapsed/expanded menu with context apparent, desktop/mobile |
| B02 | Component atlas | Source-backed fields, single-row selects, File/URL/Text radios, checkbox choices, textarea, disclosure, helper, validation, feedback, empty/results states, focus, and disabled/danger actions; no mult-row listbox; not a page mockup |
| B03 | Server connection | `/NONE/server`; endpoint/helper/error, auth collapsed and expanded |
| B04 | Repositories index | `/repositories`; populated and empty list, readable/writeable status |
| B05 | Repository delete | `/delete`; blank selector, SYSTEM excluded, validation/confirmation/feedback states |
| B06 | Create chooser and base form | `/create` without type plus generic type-selected form; separate view targets, Advanced closed/open |
| B07 | Memory baseline | `/create?type=memory`; core fields, Advanced closed summary, and expanded Advanced view |
| B08 | Memory variants | `/create` component board naming all seven Memory types, IDs/titles, feature differences, shared closed/expanded patterns, custom-rule fields |
| B09 | Native baseline | `/create?type=native`; core fields, indexes, Advanced closed summary, and expanded Advanced view |
| B10 | Native variants | `/create` component board naming all seven Native types, IDs/titles, feature differences, shared closed/expanded patterns, custom-rule fields |
| B11 | Remote repository | `/create?type=remote`; local identity and remote server/repository settings |
| B12 | SPARQL endpoint repository | `/create?type=sparql`; blank endpoints with distinct placeholders |
| B13 | Federation repository | `/create?type=federate`; unchecked members, min-two and ID-collision validation |
| B14 | LMDB repository | `/create?type=lmdb`; identity, triple indexes `spoc,posc,ospc`, evaluation mode, Advanced closed summary |
| B15 | LMDB advanced options | `/create?type=lmdb`; separately expanded Advanced view with all tunables, selected defaults, optional unset state |
| B16 | Query workspace | `/query`; empty/request-backed editor, Run/Explain below editor, cancel and results, Query options closed |
| B17 | Query settings and save | `/query`; closed summaries plus expanded Query Options and Save Query panels, source defaults and namespace Clear action |
| B18 | Explain configuration and views | `/query` explain surface; Config closed summary/popover, defaults, format/level, highlighting/properties, Text/DOT/JSON and compare/diff states |
| B19 | Tuple results | Embedded query-result tuple view; row metadata, paging, layout and default-closed/expanded Display/Download panels |
| B20 | Graph results | Embedded graph result view; table/records layouts and default-closed/expanded Display/Download panels |
| B21 | Boolean results | Embedded ASK true/false result states with optional result controls closed |
| B22 | Embedded result options | Tuple and graph download/result-option panels, closed summaries, formats, limit, layout, wrapping, datatypes, paging/fullscreen context |
| B23 | Standalone Export | `/export`; Accept selector/download, default-closed and expanded limit options, populated/empty table |
| B24 | Contexts and Types | `/contexts` and `/types`; two individually titled, separate full-page table targets and empty states |
| B25 | Add file mode | `/add`; File selected, autodetect, Upload, context override off, embedded contexts preserved, default-graph behavior, Advanced closed and expanded |
| B26 | Add URL/Text modes and advanced | `/add`; blank URL/Text states, autodetect, context override off, embedded contexts preserved, Base URI independent, disabled Context until opt-in, servlet-provided Isolation Level, Advanced closed/open |
| B27 | SPARQL Update | `/update`; editor, execute, error/feedback and keyboard-focused state |
| B28 | Clear and Remove | `/clear` and `/remove`; separate, individually titled dangerous forms; warning/examples/error states |
| B29 | Explore | `/explore`; resource, named result-options disclosure closed/open, datatype display, results and paging |
| B30 | Namespaces | `/namespaces`; prefix input/existing prefix chooser, namespace field, update/delete and table/empty |
| B31 | Saved Queries | `/saved-queries`; populated and empty states, execute/bookmark/show/edit/delete-confirm, metadata and closed/open text panels |
| B32 | Summary and Information | `/summary` and `/information`; separate full-page read-only views, Config Turtle closed/open and runtime/application sections |

## Interaction and content guardrails

- Keep source labels and associations intact. Make the full clickable label work for checkboxes/radios and keep focus visible with calculated blue tokens from [PALETTE.md](PALETTE.md). Use semantic buttons/selects/inputs and a consistent keyboard order.
- Use short controls for short numeric/enumerated values, useful width for identifiers, paths, and URLs, and genuinely roomy editors for SPARQL or rule text. Long URIs and config text should remain copyable and inspectable; never clip content to make a mockup look tidy.
- Build tables with real column hierarchy, row states, and responsive access to long RDF terms. Keep empty messages and errors tied to their source roles. Do not invent filters, analytics, actions, fictional metadata, or a confirmation step where source only has a warning/validation interaction.
- Danger controls remain clearly labeled and deliberate; show confirmation/feedback states without submitting Clear, Remove, or Delete during design review.
- Include 1440, 768, 390, and 320 px targets where a page has controls or dense data. Preserve responsive containment and label readability; the currently inventoried routes use no explicit mult-row listbox; any future such source control must retain its authored size.
- Treat every runtime-provided selection (download formats, query languages, repositories, isolation levels, federation members, current contexts, and query metadata) as dynamic. Display the source default only when the source provides it; otherwise use the actual empty/blank/default state.

## Generated asset status

The complete board set now contains 32 source-mapped boards with both light and dark visual targets (64 images total). `image-prompts.json` preserves the exploratory history and records each completed board's light/dark prompt, local image path, generated-source path, byte count, SHA-256, and visual review note. The worker manifests retain per-batch generation and source-fidelity evidence. `index.html` is the browser gallery and exact-token CSS preview; the original logo PNG and palette tokens remain authoritative where a generated raster approximates text, color, or the logo.
