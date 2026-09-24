# Workbench-wide design rollout

Date: 2026-09-24. User scope: apply the refined design to every Workbench page, using imagegen to create and align each page before implementation.

## Design authority

The established query-page contract at `../workbench-query-refresh/refinement/design-spec.md` defines the shared palette, typography, spacing, islands, icons, form controls, disclosure behavior and responsive accessibility. The closed desktop and mobile query prototypes are the visual anchors. This broader scope removes the eventual query-only styling boundary: common shell and controls should become shared once all affected page designs are prepared. Page-specific behavior and data layouts remain local.

The query refinement may proceed against its already-generated references. Each remaining page requires an inventory entry, an inspected real baseline, a generated design, and a short implementation contract before production changes for that page. Shared-shell implementation must wait until its affected pages have corresponding references. Retain existing artifacts and preserve current server semantics.

## Page design principles

- Keep the real RDF4J identity, route hierarchy, labels and workflows. Use slate navigation and muted teal content links; no GraphDB coral/chrome.
- Use the same compact contextual header, desktop navigation island and mobile Menu disclosure everywhere. Highlight the actual current page.
- Prefer one useful main island per operation, with additional islands only for genuinely separate tasks or results. Do not split individual form fields into cards.
- Put the common action and required fields first. Keep advanced options in a labelled disclosure with sensible existing defaults. Never hide required fields, validation errors or destructive-action consequences.
- A wide table follows the query results' Auto/Table/Records and wrap pattern where useful; small administrative tables can use a responsive labelled record layout directly. Do not force default sideways page scrolling.
- Destructive operations use clear labels, existing confirmations and restrained danger styling local to that action. Do not add extra confirmation steps to ordinary operations.
- Selection/detail pages use a consistent heading, short context, clear primary action, aligned fields, useful empty states and unchanged server navigation. Uploads retain actual file/URL choices and format inference.
- Long URLs, prefixes, repository IDs and filenames wrap within their containers. User data is never silently truncated or omitted.
- Success, pending, validation and error states share the same tone, icon treatment and accessible status handling. Preserve existing technical error detail in a discoverable region.
- Every page has a mobile treatment: 44px targets, stacked labels/fields, wrapped context, meaningful reading order, no clipped options. Inspect 390px and cover 320px/768px/landscape/zoom in representative shared-layout tests.
- Keep functionality available without new third-party CDN/runtime dependencies. Original inline outline icons are shared. Preserve XSL/TypeScript/Java abstractions and progressive rendering.

## Design-before-implementation checklist

The operational coordinator owns the page inventory and capture manifest under this directory. Root owns generated mockups and page contracts. Inventory must include routes, their XSL templates, form variants, standalone query result types and notable empty/error/dialog states.

For each page/family, the manifest records:
1. Current desktop and mobile screenshots, fixture route, template and state.
2. Generated page design path and exact prompt, grounded in inspected baselines and the shared query reference.
3. Any correction to incidental imagegen text/layout/color errors.
4. Authorized implementation scope and retained behavior.
5. Final desktop/mobile screenshots and red-to-green or existing-contract verification.

Closely related repository creation variants may share a composition, but every variant must be explicitly inventoried and represented in the generated page/component designs before its implementation. Do not silently claim coverage from a generic dashboard mockup.

## Delivery

## Coverage and final alignment decisions

The inspected route inventory covers all 19 routes/states and all 18 repository creation variants. Batches 01–07 contain a separate generated desktop/mobile page board for every non-query user-facing page and creation variant. Batch 08 supplies shared ASK, empty, error, running, cancelled, tuple and graph result component designs. The query page also retains its earlier closed/open desktop/mobile, controls, fullscreen and Explain/Compare references. The source audit found no additional user-facing Workbench route outside this coverage. The raw `info` XML endpoint and host-provided missing-repository fallback are not application pages to redesign.

All affected page designs are now prepared; shared-shell and page implementation may proceed. Preserve each batch JSON's exact prompt, reference paths and page contract with the copied images in the catalog. These final consistency corrections apply wherever generated pixels disagree:

- Use the locked slate navigation/secondary-action colors and teal data-link/primary-action colors, never sampled blue tint. Change links are slate; selected navigation uses a slate fill and rule.
- Render one labelled mobile Menu disclosure below the header, with no duplicate header hamburger. Header context and repository IDs wrap completely.
- Keep every field label above its control. Common form spacing and field widths apply to every repository type. Type/ID/title remain vertically stacked in short forms; advanced settings may use a readable two-column grid on desktop and one column on mobile.
- Repository Advanced settings are closed by default; essential endpoints, federation members and custom rules stay visible. Create/Cancel remain outside the disclosure. Preserve every field, exact default and tri-state choice, including LMDB's Repository default selection. Long forms scroll vertically; no fields disappear to match a cropped mockup.
- Controls use the locked outline icon family, size, stroke, spacing and focus treatment. Mobile controls retain 44px targets. Teal is flat, with the agreed hover treatment rather than generated gradients.
- Result state specimens are alternatives, not simultaneous sections. Never render sample data, mockup captions, fabricated counts or raw request metadata. ASK false is a neutral successful answer. Populated graph verification uses valid RDF subjects.
- Result options remain inside their Results island, below its stable toolbar. Preserve the established Layout select, Wrap values control and actual supported paging/datatype fields; do not invent a preferred-datatype selector from the generated board. Fullscreen hides surrounding application chrome and uses the entire viewport, with a visible exit control and Escape/focus restoration.
- The main query toolbar and Explain/Compare layout follow the established query contract; incidental one-button-per-row variations in the result-state board do not override it. Empty/error messages must remain readable without clipping or invented backend behavior.

## Implementation and verification

Implement cohesive shared styles/components after design coverage, then page-specific layouts. Verify every inventoried route, all navigation destinations, forms, validation/feedback, responsive behavior, original query/result lifecycle and real data operations on disposable fixtures. Preserve all existing untracked artifacts. Do not commit, push, switch branch or delete user repositories.

The living execution plan remains `../../.agent/execplans/workbench-embedded-results.md`, expanded to this scope by the coordinator. It keeps one in_progress step while active and records exact red/green evidence, meaningful milestones and true limitations.
