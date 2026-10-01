# Make fullscreen results start with wrapping disabled

This ExecPlan is a living plan and follows `.agent/PLANS.md`. It records the user-visible behavior, reproduction, implementation, and verification for this task.

## Purpose / Big Picture

When a Workbench query result enters fullscreen, long values should begin on one line so the larger view uses its available width. The user may turn wrapping on or off while fullscreen is active. Leaving fullscreen must restore the previous normal-view setting and leave its saved preference unchanged. Main results, saved-query results, and supported embedded result documents must follow the same rule.

## Progress

* [done] inspect shared renderer and legacy frame paths
* [done] run required offline root clean install
* [done] add smallest failing wrap lifecycle test
* [done] implement shared temporary presentation state
* [done] verify lifecycle and rendered routes
* [done] package assets and audit preview
* [done] hand off screenshots for parent review

## Surprises & Discoveries

The shared `workbench.resultFullscreen` manager currently changes only fullscreen classes, accessibility labels, focus, and scroll locking. `QueryResultRenderer` owns the live `data-wrap` attribute and checkbox. Its checkbox handler saves every change through `query.applyResultPresentationState`, so fullscreen defaults and temporary toggles currently have no separate preference lifetime.

The older embedded result document uses `queryResult.ts` and reports checkbox changes to `query.ts` with `postMessage`. It needs its own temporary fullscreen wrap snapshot, and its message must not overwrite the parent’s normal-view preference.

The in-repository RED now confirms the visible behavior: after entering fullscreen with a tuple row result, `data-wrap` remains `true` instead of becoming `false`. Its report is retained at `/tmp/rdf4j7-workbench-fullscreen-no-wrap-20261001/fullscreen-wrap-red.log` and was appended to root `initial-evidence.txt` before any production edit.

## Decision Log

* Decision: use the existing fullscreen manager to notify a registered result renderer whenever any caller enters, exits, hands off, or disposes a fullscreen target. The renderer will snapshot its current wrapping value, apply a temporary no-wrap state, allow temporary checkbox changes without saving, and restore the snapshot on exit.
  Rationale: main and saved results already share `QueryResultRenderer`, while the manager already centralizes Escape, owner switching, and disposal transitions.
  Date/Author: 2026-10-01 / Codex.
* Decision: keep the legacy iframe behavior in `queryResult.ts` and tag its presentation messages as temporary while fullscreen is active.
  Rationale: legacy result pages do not use the shared renderer, so the parent must distinguish their fullscreen-only display changes from normal preference updates.
  Date/Author: 2026-10-01 / Codex.
* Decision: route renderer preference writes through the pre-fullscreen wrap snapshot and mark legacy iframe display messages as temporary.
  Rationale: changing Layout while fullscreen must not persist the temporary wrapping value, and the parent must preserve normal wrap state for every iframe message.
  Date/Author: 2026-10-01 / Codex.

## Outcomes & Retrospective

The shared renderer now registers target-scoped callbacks with `resultFullscreen`, snapshots normal wrapping at entry, synchronizes its checkbox and root attribute, and restores the snapshot for Escape, owner handoff, and disposal. Its central preference save path uses the snapshot while fullscreen, and temporary checkbox changes are not saved. The legacy iframe snapshots/restores its wrap value and tags display messages so the parent can keep only layout updates. Focused tests pass (query-stream refinements 37/37; query-result lifecycle 22/22). Native Chromium checks confirmed populated main result cells compute to `white-space: nowrap` at fullscreen entry; wrap toggles and a fullscreen Layout change do not overwrite the normal `wrap=true` preference; Escape restores normal state without a query rerun. The existing saved query returned zero rows, so its native route check covers renderer state and checkbox synchronization; it also exits fullscreen with normal wrapping restored. No page or console errors occurred. All six query/queryResult/queryStream JavaScript and source-map assets match the generated sources, Workbench WAR, server boot JAR, and HTTP-served preview. Full evidence is in `/tmp/rdf4j7-workbench-fullscreen-no-wrap-20261001/final-manifest.json`.

The parent also independently ran a two-row `VALUES` query through the real editor and confirmed normal `data-wrap=true` changes to fullscreen `data-wrap=false` with computed cell `white-space: nowrap`; the unchecked control stays toggleable, Escape restores `data-wrap=true`, and result count/time remain unchanged. The main fullscreen screenshot was reviewed and accepted.

## Context and Orientation

`tools/workbench/src/main/webapp/scripts/ts/queryStream.ts` defines `workbench.resultFullscreen` and the shared `QueryResultRenderer`. The renderer is used by the main query page and saved-query result forms. Its `wrapControl`, internal `wrap` value, and root `data-wrap` attribute drive visible wrapping; `savePresentationPreferences()` updates the main query page’s normal presentation state.

`tools/workbench/src/main/webapp/scripts/ts/query.ts` connects the shared fullscreen manager to the legacy iframe and owns the normal main-query layout/wrap state. `tools/workbench/src/main/webapp/scripts/ts/queryResult.ts` runs inside the older embedded result document and synchronizes its controls with the parent using `rdf4j-query-display-state` and `rdf4j-query-fullscreen-state` messages.

The focused unit suite is `e2e/tests-unit/query-stream-refinements.test.js`. It compiles the TypeScript renderer into a temporary directory and exercises it with the repository’s `FakeDocument`. Native browser checks use the existing local Workbench preview at port 18817 and the original appdata `/private/tmp/rdf4j7-workbench-spacing-20260929/runtime/appdata`; use only read-only SELECT queries and do not save, delete, or modify repositories or saved queries.

## Plan of Work

Renderer-level and legacy iframe regressions now cover both normal wrapping preferences, fullscreen entry, temporary checkbox changes, a Layout save during fullscreen, Escape, owner handoff, disposal/replacement, and parent/child message handling. The observed RED reports are appended to root `initial-evidence.txt`.

The shared renderer now registers with `resultFullscreen`, so its temporary presentation follows manager entry, exit, Escape, owner handoff, and disposal transitions. Its preference save method uses the pre-fullscreen wrap snapshot so layout changes cannot persist temporary no-wrap. Disposal exits fullscreen before unregistering the callback.

The legacy embedded result path now applies no-wrap on fullscreen entry, allows temporary user toggles, restores its captured setting on exit, and marks child display messages as fullscreen-only. The parent keeps accepted layout changes but does not overwrite normal wrapping. Regenerate TypeScript assets, run focused lifecycle/unit tests and bounded native main/saved checks, package the preview, and compare served JavaScript/source maps to the generated sources.

## Concrete Steps

1. [done] Run the root offline quick clean install. Log: `/tmp/rdf4j7-workbench-fullscreen-no-wrap-20261001/maven-build.log`.
2. [done] Add and run the renderer wrap-lifecycle RED; save its report snippet to root `initial-evidence.txt` before editing production.
3. [done] Implement manager registration and renderer restore behavior.
4. [done] Add legacy embedded temporary-wrap synchronization without preference writes.
5. [done] Run focused unit/browser checks, format, package, and verify served hashes.
6. [done] Hand off screenshots for parent review.

## Validation and Acceptance

The RED must fail before production because fullscreen leaves wrapping enabled and its normal preference writer records fullscreen-only toggles. After the fix, the focused renderer lifecycle cases must show `data-wrap="false"` and an unchecked control on entry, allow the user to re-enable wrapping while fullscreen without changing normal preference state, and restore the exact previous true or false setting on exit. A second fullscreen owner and renderer disposal must restore their prior wrapping state with no leaked fullscreen state.

The native browser checks cover the main query result and saved-query result, including a read-only main result with long literals. They verify the checkbox, root `data-wrap`, computed no-wrap cells, Escape exit, layout changes, and normal preference after exit. The saved query on the retained fixture currently yields zero rows, so its native assertion covers shared renderer state and the checkbox rather than a populated saved-result cell. The legacy iframe path is covered by its focused lifecycle test. TypeScript generation, root install/package, formatting, copyright check, final diff check, and generated/package/served hashes are complete.

## Idempotence and Recovery

The tests use in-memory rows and read-only SELECT queries. They do not save query state to repositories or modify repository contents. If preview execution fails, keep the original appdata and restart only the local server with the same path. Keep all unrelated worktree changes and artifacts; no branch changes, cleanup, commit, or push are in scope.

## Artifacts and Notes

Initial RED evidence belongs in `/Users/havardottestad/Documents/Programming/rdf4j7/initial-evidence.txt`; full test logs, browser screenshots, and final hash manifest belong under `/tmp/rdf4j7-workbench-fullscreen-no-wrap-20261001/`.

## Interfaces and Dependencies

Extend `workbench.resultFullscreen` with a target-scoped presentation callback registration and unregister function. `QueryResultRenderer` registers one callback for its target and unregisters during disposal. In `queryResult.ts`, keep the legacy checkbox API and message shape compatible while adding a fullscreen marker that lets the parent avoid persisting temporary wrap changes. No new dependency or public server API is needed.

Plan Note: Created on 2026-10-01 after source tracing and the required offline root install; design is scoped to fullscreen wrap state and the supported legacy iframe path.
Plan Note: Updated on 2026-10-01 after observing the focused renderer RED; production changes may now begin.
Plan Note: Updated on 2026-10-01 after focused green suites, native route checks, package hashes, and parent rendered review; all work is complete.
