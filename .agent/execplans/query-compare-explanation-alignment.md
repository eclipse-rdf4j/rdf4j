# Align comparison explanations

This ExecPlan follows `.agent/PLANS.md` and remains the implementation record for this change. In Query comparison mode, the two explanations should begin at the same vertical position even when either query editor contains more lines than the other. The alignment should follow the page's layout rather than a pixel offset, adapt when the query editors or errors change, preserve the close/reopen animation, and return to independent stacked flow on narrow screens.

## Progress

- [x] Inspect compare pane structure and CSS.
- [x] Add failing asymmetric browser geometry regression.
- [x] Align desktop editor and explanation rows.
- [x] Reproduce one-sided status displacement.
- [x] Share nested explanation content rows.
- [x] Verify states, close, narrow flows.
- [x] Reproduce wide plan overflow.
- [x] Constrain nested grid inline sizing.
- [x] Verify width, states, narrow flow.
- [x] Run tests, format, final hygiene.

## Purpose / Big Picture

When users compare query plans, their eyes should be able to scan the two explanations side by side from the same starting line. The editor and error rows may have different natural heights, but the explanation row should start at the same height in both panes. The same alignment must hold whichever query is longer and after live editor changes. At narrow widths the panes stack and keep ordinary page flow.

## Context and Orientation

`tools/workbench/src/main/webapp/transformations/query.xsl` creates `#query-primary-pane` and `#query-compare-pane` using one `query-pane` template. Each pane contains an editor row, an error row, and an explanation row; only the primary pane has an explanation-controls row. `tools/workbench/src/main/webapp/styles/query-compare.css` places the panes in a two-column grid on desktop and changes that grid to one column at 900px and below. `e2e/tests` contains Playwright tests against a live RDF4J server and browser geometry assertions.

The existing parent grid aligned only the tops of the panes, leaving editor, error, and explanation rows to flow independently. On desktop, a shared grid aligns the corresponding pane rows, and nested subgrid rows align each explanation's label, status, toolbar, and plan surface. The editor and explanation contents stay top-aligned inside their shared tracks. At narrow widths, the panes return to normal stacked flow.

## Plan of Work

First add a Playwright regression in `e2e/tests` which opens compare mode, populates one editor with a substantially taller query, renders both explanations, and measures the starts of both editors and explanation contents. Repeat with the taller query on the opposite side, then change only one editor to expose a real stale status and a parser-error status. Capture red measurements before each corresponding CSS change in `initial-evidence.txt`.

Then change only the compare layout rules in `query-compare.css`. Use seven CSS Grid tracks for the editor, error, explanation label, status, toolbar, plan surface, and primary controls. The pane, explanation row, and field use nested subgrids to share corresponding track starts. Keep the existing close animation's opacity/visibility and pane nodes. Below the existing 900px breakpoint, restore regular one-column flow so each pane's own content determines its height.

Finally run the browser test in both height directions, verify real one-sided stale and parser-error states, close/reopen compare mode, and exercise narrow stacking. Save final wide and narrow screenshots in `output/playwright/`. Run the Workbench module suite, compile frontend assets only if any TypeScript changes are required, and finish with copyright, formatter, and diff checks.

## Decision Log

- Decision: Use nested subgrid rows for the editor, error, explanation label, status, toolbar, surface, and optional primary controls. Rationale: matching only the outer explanation row left its plan surface offset by a one-sided status message. Date/Author: 2026-09-27 / Codex.
- Decision: Keep pane elements in the DOM and use the current narrow breakpoint. Rationale: compare close/reopen transitions already animate pane geometry and opacity; replacing panes with `display: contents` or changing the responsive threshold could disrupt those existing behaviors. Date/Author: 2026-09-27 / Codex.

## Surprises & Discoveries

- Observation: The parent comparison grid had `align-items: start`, while each pane laid out its editor, error, and explanation rows independently. Evidence: `query-compare.css` originally defined only parent column tracks and `.query-compare-pane { min-width: 0; }`.
- Observation: The primary pane alone includes a controls row after its explanation. Evidence: the `query-pane` invocation in `query.xsl` passes `$showControls=true()` only for `#query-primary-pane`.
- Observation: A real browser shows a 365px explanation-top offset when the right editor is more than 120px taller; the test captured a screenshot before asserting. Evidence: `logs/query-explanation-alignment-red-geometry.log` and `initial-evidence.txt`.
- Observation: A shared outer row alone still stretched the explanation's inner implicit tracks, moving the label 169px and plan surface 338px in the left-taller case. The shared editor row also moved the short editor down 182.5px until its inner tracks were top-aligned. Evidence: `logs/query-explanation-alignment-diagnostic.log`, `logs/query-explanation-alignment-editor-red.log`, and `initial-evidence.txt`.
- Observation: Editing only one query exposes a real stale status that shifted its plan surface by 38.375px. Nested subgrid tracks for status, toolbar, and surface keep both plan surfaces aligned for stale and parser-error messages on either pane. Evidence: `logs/query-explanation-alignment-stale-surface-red.log` and `logs/query-explanation-alignment-asymmetric-error-final.log`.
- Observation: Long plans exposed intrinsic minimum sizing in the nested grids: the compare surface expanded the document to 2169px at a 1440px viewport. Setting zero-minimum inline tracks and `min-width: 0` on the nested grid items keeps each surface within its pane. Evidence: `logs/query-explanation-alignment-width-red.log` and `logs/query-explanation-alignment-width-green.log`.

## Validation and Acceptance

The browser geometry test captures red evidence for the original editor-height drift, the one-sided stale-surface drift, and intrinsic-width overflow. It passes with editor, explanation-row, label, and plan-surface starts within one CSS pixel in both query-height directions; both surfaces remain inside their panes and the document stays at the 1440px viewport width. One-sided stale and parser-error status messages, compare close/reopen, and stacked panes at 390px without horizontal overflow also pass.

Run the browser test against an isolated local RDF4J server and use an ephemeral in-memory repository. Run relevant Workbench tests using the repository's `mvnf` workflow with `.m2_repo`; do not run Maven test phases concurrently or with `-am`/`-q`. Preserve the red output in `initial-evidence.txt`, green output in retained logs, and all pre-existing/untracked artifacts.

## Idempotence and Recovery

The Playwright test creates a uniquely named ephemeral repository and deletes it after the test. It must not overwrite user data or other test fixtures. Keep all earlier export-related changes, generated assets, screenshots, and logs. If the browser server cannot bind or start, record the precise restriction and use the existing approved loopback test path rather than substituting a DOM-only assertion for browser geometry.

## Artifacts and Notes

The final evidence includes red browser measurements and focused green snippets in `initial-evidence.txt`, the passing geometry test in `logs/query-explanation-alignment-width-green.log`, the 557-test Workbench run in `logs/mvnf/20260927-170524-verify.log`, and screenshots in `output/playwright/query-compare-explanation-{right-taller,left-taller,reopened,narrow}-20260927.png`. Copyright, formatter, and diff checks passed. Existing export feature changes and all prior untracked files remain untouched.

## Interfaces and Dependencies

The layout change is private to the Workbench query comparison page. The browser test uses the existing Playwright test configuration in `e2e/playwright.config.js` and a live local Workbench endpoint. No server, query, or frontend script API should change.
