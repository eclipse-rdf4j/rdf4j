# Keep Query Columns Readable in Automatic Layout

This living ExecPlan follows `.agent/PLANS.md` and the repository `AGENTS.md`. It preserves the already-dirty Go to row, dark first-paint, first-ten column sampling, create-setting grouping, and fullscreen no-wrap work. Do not commit, push, change branches, remove artifacts, or alter the original preview appdata.

## Purpose / Big Picture

Users should be able to read every query result column without the browser collapsing a short identifier column to a few pixels. When wrapped values are enabled and the available width cannot give every column the shared readable minimum, Auto should show result cards. When wrapping is disabled, Auto should keep the table and let its minimum-width columns scroll horizontally. Explicit Table and Records choices remain in effect at every viewport width.

## Progress

* [done] inspect automatic layout and samples
* [done] run required root quick install
* [done] capture browser layout regression
* [done] implement shared minimum allocation policy
* [done] validate resize wrap and fullscreen
* [done] package preview and audit hashes
* [done] protect long headers in table and cards

## Surprises & Discoveries

The shared streamed renderer already computes the readable budget from 16 rendered character widths plus header-cell horizontal padding and 2px, but uses that total only to select Auto layout. `setTableColumnWidths` then assigns fixed widths from sampled natural widths and distributes spare width proportionally, so a short ID column can stay below the readable minimum even when the minimum for every column fits. Auto currently makes the same Table/Records decision regardless of wrap state.

The legacy embedded result page duplicates the 16-character budget in `queryResult.ts`, checks `tableWrap.clientWidth` even while that table is hidden, and also ignores wrap state. It therefore needs the same minimum policy and a stable visible-root width so it can recover from Records to Table after a resize. Long variable headers are unwrapped in tables, so their intrinsic rendered text width must also contribute to each minimum. The same long label can appear in Records cards and must wrap within its label track.

## Decision Log

* Decision: preserve the existing 16-character readable-width rule and expose its count through one CSS custom property consumed by both streamed and legacy measurement paths. Rationale: avoid divergent heuristics while keeping the already-established sizing rule. Date/Author: 2026-10-01 / Codex.
* Decision: Auto + Wrap selects Table only when the sum of per-column minimums fits; Auto + no-wrap always selects Table; explicit Table and Records retain their selected mode. Rationale: cards preserve readable wrapped values when space is insufficient, while no-wrap values require a horizontally scrollable table. Date/Author: 2026-10-01 / Codex.
* Decision: for wrapped Table, allocate at least each column's minimum first, then distribute remaining width according to the first-ten sample's above-minimum demand; preserve minimum-width overflow for explicit Table and no-wrap. Rationale: small identifier columns must not remain narrow or take equal space when long value columns need more room. Date/Author: 2026-10-01 / Codex.
* Decision: each column minimum is the maximum of the existing 16-character budget and the intrinsic rendered nowrap header width, plus the existing cell padding. Rationale: fixed table layout must protect real header content, not only a nominal character count. Date/Author: 2026-10-01 / Codex.
* Decision: Cards may wrap a long field-name label within its label track. Rationale: the intrinsic header width can push Auto into cards, where unbroken labels must not paint into the value column. Date/Author: 2026-10-01 / Codex.
* Decision: measure header text in an offscreen probe attached to the existing result root, with computed font and text-transform styles applied in both result paths. Rationale: a body-level Canvas/span probe missed typography inherited from the result root and raw `textContent` omits CSS-transformed glyphs. Date/Author: 2026-10-01 / Codex.

## Outcomes & Retrospective

The in-repository RED failed before production changes: on a 1134px result root, the 151.8px minima produced fixed widths `[31.8, 1038.2, 64.0]`. After the fix, rendered header cells measure `[151.80, 830.41, 151.80]` and the table stays 1134px wide. Focused native checks also show Auto switches to Records below the minimum, returns to Table after widening, stays Table for no-wrap, and restores the previous Auto choice after fullscreen. Saved results and the legacy embedded path follow the shared minimum policy. Follow-up WebKit REDs confirmed that a long unwrapped header must contribute its measured width in Table mode, and that it must wrap within the Records label track. A later boundary RED showed that a body-level text probe missed the typography inherited from the result root: at 825px, raw Canvas minima fit (824.05px), while the rendered CSS minimum does not (825.00px). Both paths now measure an offscreen probe attached to their result root. WebKit confirms Auto switches to Records at that rendered boundary and back to Table when widened; it also confirms the long header fits without overlap and the Cards label stays within its track. Initial RED details remain in root `initial-evidence.txt`; logs, served hashes, and the final manifest are under `/tmp/rdf4j7-workbench-column-minimum-20261001/`. Screenshots are in `/var/folders/0m/x3jzy4t15djd88znchzzx1_h0000gn/T/rdf4j7-workbench-column-minimum-20261001/`.

## Context and Orientation

`tools/workbench/src/main/webapp/scripts/ts/queryStream.ts` contains `chooseAutoLayout`, the shared `QueryResultRenderer`, its first-ten width sample, resize observer, and `setTableColumnWidths`. The renderer creates result tables for both the main query route and saved-query route. `tools/workbench/src/main/webapp/scripts/ts/queryResult.ts` is the older embedded result-page path. The shared rules are in `tools/workbench/src/main/webapp/styles/query.css`; generated JavaScript and source maps are produced by `bash tools/workbench/compileTypescript.sh`.

The first-ten sample consists of headers and logical result indexes 0 through 9 only. It is not the virtualized visible window. Resize, Auto/Table/Records changes, and fullscreen must reuse that sample; later rows and batches must not affect the sample. With fewer than ten rows, use the available rows; with zero rows, headers alone define the minimum. A changed result schema starts a fresh sample.

## Plan of Work

First run the required root offline quick clean install. Add an in-repository focused regression and run it against the unchanged production implementation. Preserve its compact failure output in `initial-evidence.txt`. The first RED should use an asymmetric schema (a tiny ID and a long value column) and assert actual rendered column geometry against the 16-character minimum.

After the RED, define one shared CSS minimum-character token and have both TypeScript paths derive each column's minimum from the active result font and header padding. In the streamed renderer, Auto must account for wrapping, use the stable visible root width, and keep the sampled minimum widths hard during constrained allocation. Resize should recompute effective layout and column allocation from the same first-ten sample. In the legacy embedded path, Auto must use the visible result root width rather than the hidden table wrapper and apply the same wrap/no-wrap policy.

Add only focused coverage for the equality boundary, asymmetrical widths, narrow-to-wide recovery, explicit Table/Records, no-wrap horizontal scrolling, fullscreen's temporary no-wrap state, first-ten stability, and the legacy layout path. Regenerate JS/maps, run focused frontend tests and browser checks, format/check headers, rebuild the package, restart the existing preview using its original appdata, and verify served assets. Keep all unrelated dirty files and artifacts intact.

## Concrete Steps

Working directory: `/Users/havardottestad/Documents/Programming/rdf4j7`.

1. [done] Run root offline quick clean install. Log: `/tmp/rdf4j7-workbench-column-minimum-20261001/maven-build-initial.log`.
2. [done] Add and run focused Chromium regression. Its fixture refused to overwrite its unique repository ID and cleaned up only the ID it created.
3. [done] Append RED summary and preserve artifacts. Prior evidence and all untracked artifacts remain intact.
4. [done] Implement shared minimum and allocation policy.
5. [done] Add focused resize, wrap, saved, and legacy checks; regenerate TypeScript assets.
6. [done] Format, package, refresh preview, and audit hashes.
7. [done] Hand off final evidence and screenshots.
8. [done] Reproduce transformed header minimum mismatch.
9. [done] Measure headers with rendered CSS transforms.
10. [done] Validate transformed header measurement.
11. [done] Complete parent review and acceptance.
12. [done] Return final acceptance handoff.

## Validation and Acceptance

The initial regression must fail before production changes because a short column's rendered width is below the current 16-character minimum. After the fix, every Table column is at least the shared minimum, including intrinsic nowrap header text after CSS text transforms. Auto + Wrap uses Table at the exact summed-minimum boundary and Records below it; resizing back wide returns to Table. Auto + no-wrap remains Table with a horizontal scrollport. Explicit Table and Records do not change mode during resize. Long field labels wrap in Records rather than covering their paired values. First-ten sample boundaries remain stable while scrolling through at least 100 rows and after later batches. Fullscreen temporary no-wrap uses Table and restores the prior Auto behavior on exit. The embedded result path uses the same CSS-faithful minimum and recovers from hidden Records to Table after widening. Query count and values remain unchanged.

## Idempotence and Recovery

Use only a uniquely named E2E repository fixture that checks the ID is absent before creation and deletes only its own repository after the run. Do not add or delete saved queries on the original repository. Keep the running preview on port 18817 and appdata `/private/tmp/rdf4j7-workbench-spacing-20260929/runtime/appdata`. If a test fails, preserve the first RED and its geometry before correcting only an inaccurate oracle. Do not delete builds, logs, browser traces, screenshots, or unrelated untracked entries.

## Artifacts and Notes

Initial RED evidence belongs in root `initial-evidence.txt`. Retain the exact test report, geometry JSON, screenshots, served-asset hash report, and final scope manifest in `/tmp/rdf4j7-workbench-column-minimum-20261001/`.

Final focused validation: `node --test tests-unit/query-stream-refinements.test.js tests-unit/query-result-lifecycle.test.js` from `e2e` passed 61/61. The bounded WebKit run selected `wrapped table columns meet the shared readable minimum`, `Auto accounts for unwrapped variable headers`, and `Auto uses cards below the wrapped column minimum and tables for no-wrap`; 3/3 passed. At the capitalization boundary, viewport 883 yielded result-root width 825 and Auto selected Records. Widening recovered Table. The rendered legacy fixture reported `table-layout:auto` and computed widths above all intrinsic header minimums; its focused lifecycle unit asserts the uppercase W width. Final screenshots include `long-header-webkit-green.png` and `long-header-mobile-current.png` in the OS temp screenshot directory. Final served/source hashes and preview route checks are recorded in `final-manifest.md`.

## Interfaces and Dependencies

No new package dependency or server API is required. Keep the existing public layout selector values `auto`, `table`, and `records`. A single CSS-backed readable-character count must feed both the shared streamed renderer and the legacy result script; the result renderer remains responsible for selected/effective layout, wrapping state, table widths, virtual scrolling, and fullscreen restoration.

Plan Note: Created on 2026-10-01 after inspecting existing automatic layout, 16-character budgets, and first-ten sampled-width allocation; no production code had changed for this task at that point.
Plan Note: Updated after the required offline root clean install succeeded; the next step is to observe a focused browser failure before production changes.
Plan Note: Updated after Chromium measured 31.78px for the short ID column against its 151.80px minimum while all minima fit within 1134px; RED is preserved in initial-evidence.txt and production work may now begin.
Plan Note: Added the shared CSS readable-character token, minimum-bounded width allocator, wrap-aware Auto choice, legacy visible-root sizing, and focused pure-policy assertions; validating rendered and lifecycle behavior next.
Plan Note: Focused unit contracts pass (3/3); Chromium rendered checks pass for minimum geometry, narrow/wide Auto transitions, no-wrap, fullscreen restoration, saved results, and first-ten schema/scroll stability. Final package and served hashes are recorded in the task evidence directory.
Plan Note: Scoped final manifest: `/tmp/rdf4j7-workbench-column-minimum-20261001/final-manifest.md`; final screenshot files are outside the repository in the OS temp directory.
Plan Note: Parent review requested the two affected full Node suites, focused WebKit browser cases, and a check that long nowrap headers cannot overlap neighboring columns; legacy minimum enforcement was checked against its rendered auto-layout table CSS.
Plan Note: New WebKit regression reproduced a genuine long-header overlap at a 792px result root: the unwrapped header text spans 490.74px but its fixed column is 441.22px; the computed complete-word header minimum is 512.74px, and the next header begins before the first label ends. This RED preceded the shared minimum calculation change.
Plan Note: WebKit confirmed table minimums include rendered long-header width, and legacy `table-layout:auto` honors its per-header inline `min-width`. The narrow Cards screenshot exposed an adjacent long field-label overlap; the native regression now requires card label containment, fixed with label wrapping.
Plan Note: Parent review exposed that body-level offscreen spans inherited different typography than the actual result root. The boundary RED was preserved; moving the probe into the shared rendered root makes its Range width match the live header (499.41px), and the main and legacy paths now use that same root-scoped probe pattern.
Plan Note: Final focused validation passed the two Node suites (61/61) and three WebKit cases (3/3); root quick install, formatter, copyright, diff check, original-route HTTP 200, and all five served/source hashes passed. Parent reviewed the final build, including the CSS-capitalized long header and narrow Cards/no-wrap transitions, and accepted the behavior.
Plan Note: A focused WebKit boundary RED now uses a lowercase-leading `workbench_...` header. Raw Canvas minima total 824.05px at a root width of 825px, while CSS-rendered header minima total 825.00px; Auto incorrectly stays Table. The matching legacy unit RED shows its current minimum omits the capitalized `W` width.
Plan Note: Both result paths now measure header text in an offscreen span with the computed font and text-transform properties, so hidden tables remain measurable without manually capitalizing characters. TypeScript assets were regenerated; the focused legacy lifecycle test is green.
