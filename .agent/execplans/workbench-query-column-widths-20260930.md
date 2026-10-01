# Keep Streamed Result Columns Stable

This living ExecPlan follows `.agent/PLANS.md`. It describes a shared Workbench result-renderer repair and must preserve the already-dirty Go to row removal and dark first-paint work.

## Purpose / Big Picture

When a user scrolls a streamed query result, the table columns should keep the same boundaries. The shared renderer will set widths from the column headings and the first ten logical result rows, then keep those widths independent of later loaded rows and the currently mounted virtual window. The rule applies to main and saved results and must not change query requests, row virtualization, loading, or scrolling.

## Progress

- [x] (2026-09-30) Verify branch and preserve dirty work.
- [x] Trace shared table and virtual window.
- [x] Add and run first width RED.
- [x] Implement stable shared sample widths.
- [x] Verify first-ten and scroll stability.
- [x] Verify saved batches and schema edges.
- [x] Reproduce wrap-mode width regression.
- [x] Measure through visible table constraints.
- [x] Reproduce mobile header constraint.
- [x] Preserve readable header widths.
- [x] Verify wrapping and nowrap behavior.
- [x] Regenerate package and compare served assets.
- [x] Close plan and retain all evidence.

## Surprises & Discoveries

- Observation: result tables use automatic browser table layout while `QueryResultRenderer.renderRows` mounts only a scroll-dependent virtual range.
  Evidence: `query.css` and `workbench-refresh.css` set `table-layout: auto`; `queryStream.ts` computes each range in `prepareWindow` and replaces table rows in `renderRows`.
- Observation: main and saved query results share `QueryResultRenderer`, so width ownership belongs in that class rather than either query caller.
  Evidence: both routes mount the shared `queryStream` renderer and share its table/record layout state.
- Observation: the focused native test reaches row 119 and the second header boundary moves from 175.94px to 50.83px as the virtual window changes.
  Evidence: `/tmp/rdf4j7-workbench-column-widths-20260930/first-red-network-enabled.log`; the RED is appended to `initial-evidence.txt`.
- Observation: WebKit advanced on the first wheel event but later events missed the scrollport after its viewport geometry changed; hovering the scrollport again before each native wheel event restored progress.
  Evidence: `/tmp/rdf4j7-workbench-column-widths-20260930/native-webkit-hover-retry.log` and `native-webkit-saved-final.log`; both focused WebKit cases passed after this harness correction.
- Observation: Playwright is the repository's available browser path for this task; the Browser skill/plugin is not available in the tool catalog.
  Evidence: the session exposes the unified CUA integration but no Browser plugin skill; this repo has Playwright-based `e2e/tests`.
- Observation: independent review found that the offscreen max-content probe makes a long value in the first ten rows force a 6,880.59px locked table at a 974px viewport when wrapping is enabled. The probe also does not inherit the visible wrapper's mobile table display rule.
  Evidence: parent review of the served preview on branch `GH-6071-workbench-redesign`; preserve exact measurements in the new in-repo RED before changing the probe.
- Observation: a later long literal in the middle of a fixed-width nowrap table is painted past its cell and over the following column.
  Evidence: `/tmp/rdf4j7-workbench-column-widths-20260930/nowrap-later-column-check.log`; the text bounds end at 4,476.53px while the next column starts at 806.05px. This RED is also appended to `initial-evidence.txt`.
- Observation: after constraining a wrapped table to its 332px mobile viewport, the first header was allowed to break each character onto its own line while the long first-ten value wrapped correctly.
  Evidence: `/Users/havardottestad/Documents/Programming/rdf4j7/e2e/test-results/workbench-query-load-more--3f46c--within-the-result-viewport-chromium/attachments/query-column-width-wrap-mobile-png-f1b3111417cb13d2c64df817431351c698979d5f.png`; the new focused RED will assert the rendered header line count.
- Observation: the focused mobile header RED measured five line fragments for `Number` and one for `Label` at a 332px result viewport.
  Evidence: `/tmp/rdf4j7-workbench-column-widths-20260930/mobile-header-red.log`, appended to `initial-evidence.txt` before the header constraint change.
- Observation: Chromium, Firefox, and WebKit agree on the fixed column boundaries, viewport-constrained wrapping, single-line column headings, and clipped nowrap cells. WebKit's result table may have fractional CSS pixels after resize, so the viewport-width assertion allows a one-pixel layout tolerance while all measured column boundaries remain exactly compared.
  Evidence: `/tmp/rdf4j7-workbench-column-widths-20260930/native-width-wrap-final-r4.log` (15/15); Chromium geometry and visual attachments are in `/tmp/rdf4j7-workbench-column-widths-20260930/native-width-wrap-visual.json` and the r4 screenshot output directory.

## Decision Log

- Decision: reproduce with a native browser test containing 120 rows, short varied first-ten values, and much longer later values; compare header boundaries at the first, last, and returned-first window.
  Rationale: this makes the virtual window change while the logical sample remains the same and exercises the user's reported scroll behavior.
  Date/Author: 2026-09-30 / Codex.
- Decision: after RED, centralize the sample and its invalidation in `QueryResultRenderer`; later rows and scrolling must not recalculate the sample.
  Rationale: main and saved results use the same renderer and must follow one rule.
  Date/Author: 2026-09-30 / Codex.
- Decision: determine per-column constraints from the result schema plus only row-store indexes zero through nine, and apply those constraints to the rendered table independently of the mounted row range.
  Rationale: reading the virtual window would make widths depend on scroll position; reading the row store's fixed prefix gives identical inputs for main, saved, and later streamed windows.
  Date/Author: 2026-09-30 / Codex.
- Decision: use repeatable native wheel events until the intended edge row is visible, and restore the viewport that `openQueryPage` actually configures.
  Rationale: large single wheel deltas may mount a virtual row before it enters the viewport, while the old helper assumed two events and the test must observe rendered visibility, not only DOM presence.
  Date/Author: 2026-09-30 / Codex.
- Decision: re-hover the scrollport before every incremental wheel event and retain Home/End as a native fallback if input no longer advances.
  Rationale: dynamic virtual row content can change the scrollport geometry; refreshing its pointer target keeps wheel input aimed at the active viewport, and edge keys remain accessible user interactions.
  Date/Author: 2026-09-30 / Codex.

## Outcomes & Retrospective

The root cause was automatic table sizing from the virtual window. `QueryResultRenderer` now reads only the first ten logical rows, measures the actual formatted cells in a probe that uses the same `.query-result-table-wrap` constraints as the visible table, and locks widths across scrolling. Wrapped values use the available width, table headings remain single line, and nowrap later values clip with ellipsis while retaining their full title text.

The original scroll RED, first-ten cutoff RED, wrap-width RED, nowrap-overlap RED, and mobile-header RED are preserved in `initial-evidence.txt`. The focused Chromium/Firefox/WebKit width suite passed 15/15, including main and saved results, first-ten comparison, native scrolling, saved batches, empty and short results, schema change, Table/Records, resize, 974px and 390px wrapping, and later nowrap-cell clipping. `query-stream-refinements.test.js` passed 36/36. Workbench install and Server Boot package succeeded; the fresh preview at `http://127.0.0.1:18817/rdf4j-workbench/query` ran as PID 19607, and final served JS, source map, query CSS, and refresh CSS hashes match the checked-in assets. Parent live review confirmed a 974px wrapped table in a 974px viewport with stable columns through End and Home. Final build, browser logs, screenshots, and hashes are summarized in `/tmp/rdf4j7-workbench-column-widths-20260930/final-manifest.json`.

## Context and Orientation

`tools/workbench/src/main/webapp/scripts/ts/queryStream.ts` defines `QueryResultRenderer`. It creates the table, headers, virtual spacers, and rows. `renderRows` reads only the window returned by `prepareWindow`; `renderHeaders` changes when result variables change. `tools/workbench/src/main/webapp/styles/query.css` and `styles/workbench-refresh.css` define the result table's sizing. `e2e/tests/workbench-query-load-more.spec.js` already provides a disposable repository, real query execution, and native scroll helpers. The checked-in TypeScript is the source; regenerate `queryStream.js` and its source map with `tools/workbench/compileTypescript.sh` after TypeScript edits.

The width sample means only header cells and logical result indexes zero through nine. Fewer than ten rows use the available rows; an empty result uses its headings alone. A new renderer or a changed schema starts a new sample. Later batches may extend the row store but may not widen the sample. A viewport or presentation change may recompute presentation from that same sample, never from the visible virtual range.

## Plan of Work

First add a focused Playwright regression alongside the existing load-more tests. Run only that test on the current packaged Workbench and preserve its initial failing report. The test should execute one 120-row query whose first ten values are short and varied and whose final rows contain long strings, capture the table's actual column boundaries, scroll to the last row with native wheel input, and check both the last window and the return to row zero.

After observing the failure, implement sample-based widths in the shared renderer. Keep header and at most ten logical rows as the only width inputs. Reapply those widths only when that sample, schema, or viewport changes; do not derive widths from the active row range. Preserve the existing loading and scrolling paths. Extend focused checks for later streamed batches, zero and fewer-than-ten results, saved results, Table/Records switching, schema changes on rerun, and viewport transitions.

Regenerate JavaScript and source maps from TypeScript, run the narrow unit and browser selections, format and check headers, package Workbench and server boot, restart only the owned preview process, and verify that served assets match the package. Keep all existing unrelated work and artifacts.

## Concrete Steps

Working directory: `/Users/havardottestad/Documents/Programming/rdf4j7`.

Use the repository Playwright runner from `e2e`, targeting the five focused width tests with Chromium, Firefox, and WebKit against the packaged local Workbench. Save complete output and geometry under `/tmp/rdf4j7-workbench-column-widths-20260930/`. Do not use `-am` or `-q` in test commands. Maven commands use the local `.m2_repo`; keep test and build logs.

## Validation and Acceptance

The RED must fail before production changes because scrolling to long later rows changes a rendered header boundary. After the fix, the first, last, and returned-first measurements must match, and scrolling must issue no extra query. Companion checks cover fewer than ten and zero rows, row 11 cutoff, saved batches, schema replacement, Table/Records, viewport changes, constrained wrapping, mobile header readability, and clipping for nowrap overflow. Run on the packaged local Workbench and verify JS/map/CSS hashes after restart.

## Idempotence and Recovery

The Playwright fixture owns a uniquely named temporary repository and removes only that repository and any saved query it creates. Do not clean or delete existing untracked files, diagnostics, preview data, or logs. If a browser assertion fails, retain the JSON geometry and report, correct only a stale test oracle, and preserve the first observed RED.

## Artifacts and Notes

The initial RED report and geometry will be appended to the ignored root `initial-evidence.txt` and retained under `/tmp/rdf4j7-workbench-column-widths-20260930/`. The previous dark-mode task artifacts are under `/tmp/rdf4j7-workbench-dark-flash-20260930/` and remain untouched.

## Interfaces and Dependencies

No new runtime dependency or public API is required. The renderer already owns the row store, schema, and virtual-window lifecycle. Browser coverage uses the repository's installed Playwright and existing test fixture; it does not claim direct testing in Apple's Safari.
