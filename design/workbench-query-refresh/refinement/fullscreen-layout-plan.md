# Result layout and fullscreen preparation

Status: read-only design preparation for the reopened query-page refinement, 2026-09-24.

## Result layout contract

The default result presentation should be `Auto` with `Wrap values` enabled. `Auto` keeps a real table while every column remains readable at the available inline width. When the reusable readability rule cannot be satisfied, it changes to a stacked record presentation in which each record exposes every column name and value vertically. The decision must use available result-surface width and the rendered column content, not query-specific column counts or fixture names. Values remain complete in the DOM and in downloads.

The result options must expose explicit `Auto`, `Table`, and `Records` choices. `Table` preserves table semantics and allows an internal horizontal scroll region when the user explicitly disables wrapping. The result surface owns that scroll; the page and the query editor must not acquire horizontal overflow. `Wrap values` applies to both table cells and records, updates the current result without executing the query again, survives paging, and remains keyboard accessible. Existing datatype controls, pagination, limits, links, and downloads stay in the result surface and keep their current request behavior.

## Fullscreen lifecycle recommendation

The parent query page should own the fullscreen interaction around the result surface (the result-frame host plus its header and controls). A parent-owned button can call `requestFullscreen()` directly from its click handler, satisfying the transient user-activation requirement without depending on a child-frame click or a postMessage round trip. The parent listens for `fullscreenchange` and `fullscreenerror`, updates the button label and accessible state, and restores focus to the initiating button after Escape or another user-agent exit. `document.fullscreenElement` is the source of truth. Entering or leaving fullscreen must not navigate, open a tab, rerun the query, clear the result, or reset paging/options.

Keeping the host in the parent also avoids making the result iframe's fullscreen permission part of the normal result protocol. If a future implementation moves the control into the same-origin result document, the iframe must opt in with the appropriate fullscreen permission/attribute and still synchronize state through the existing source and request-ID checks. That is less robust than the parent-owned control. If the API is unavailable or rejected, retain an in-page expanded mode with the same state and focus contract rather than opening a window.

The behavior is grounded in the [MDN `requestFullscreen()` reference](https://developer.mozilla.org/en-US/docs/Web/API/Element/requestFullscreen), the [MDN Fullscreen API guide](https://developer.mozilla.org/en-US/docs/Web/API/Fullscreen_API/Guide), and the [WHATWG Fullscreen Standard](https://fullscreen.spec.whatwg.org/). These sources require a user activation for requests, define `fullscreenchange`/`fullscreenerror`, and describe Escape/user-agent exits and iframe permission handling.

## Evidence and acceptance seeds

The current implementation fails the new semantic controls/fullscreen requirements in `e2e/query-refinement-new-requirements-red-followup.log`: all four Chromium cases are red. The first two cannot find a result-layout group or wrapping control, the fullscreen case cannot find a fullscreen button, and the 390px opened-settings case measures a panel at 232px scroll width against a 158px client width. The test source is `e2e/tests/query-refinement-new-requirements-red.spec.js`; it is intentionally semantic and does not lock the current `details` markup.

The later implementation must turn these into browser behavior checks at 1440px, 768px, and 390px, including repeated result-option disclosure cycles, horizontal and vertical resize, a wide multi-column result with every value reachable, fullscreen/Escape/focus restoration, no page overflow, and no query rerun or new tab. The full Explain/Compare/Diff baseline is retained in `design/workbench-query-refresh/refinement/baseline/explain-compare-manifest.json` and its PNGs; those flows must remain available while the result surface is refined.

No production source was changed during this preparation pass.
