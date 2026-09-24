# Workbench query refinement: locked implementation design

Date: 2026-09-24. Owner: current query-page refinement task.
This contract replaces the previous blue-link refresh. Follow it with the generated reference images; do not copy incidental imagegen spelling, syntax, coloring, enabled states, data totals, or geometry errors.

## Purpose and scope

Keep query execution and results together. Make ordinary querying simple, all options discoverable, wide data readable, and the same interface usable on phones. Preserve server query semantics, request/cancellation IDs, GET/POST, paging, downloads, Save, Explain, Compare, Diff, authentication, current defaults, and standalone result pages. Do not add a frontend framework or icon dependency for this focused XSL/TypeScript/CSS change.

## Authoritative references

- `desktop-closed.png`: primary composition, island spacing, navigation, toolbar and result treatment.
- `desktop-open.png`: full-width Save and Options bands; desktop result option groups.
- `mobile-closed.png`: compact readable context, Menu disclosure, two-row action groups, record results.
- `mobile-open.png`: same toolbar positions with full-width settings below them.
- `component-fullscreen.png`: desktop table / mobile records occupying the available viewport.
- `component-explain-compare.png`: equal desktop comparison columns, stacked mobile columns, in-flow explanation settings, compact Diff dialog.
- Component studies and palette/icon board document the separate refinements. Their earlier cobalt/dark primary accents are superseded by the tokens below.

Required corrections to generated images: Change links are slate, not teal; Explain text never uses bright blue; select labels are above fields on phones; no tiny controls or clipped long URIs; no arbitrary huge blank Diff panel; every full-screen exit has a visible text label; open disclosure chevrons point up; query toolbar does not lose Explain on mobile; retain real current branding/data and control semantics. Do not render generated prose or invented counts into the app.

## Design tokens

| Token | Value |
| --- | --- |
| Canvas | #f1f5f9 |
| Surface | #ffffff |
| Inset surface | #f8fafc |
| Ink | #0f172a |
| Navigation / secondary actions | #334155 |
| Muted text | #64748b |
| Divider | #e2e8f0 |
| Control border | #cbd5e1 |
| Primary / result URI / focus | #0f766e |
| Primary hover | #115e59 |
| Selected navigation fill | #e2e8f0 |
| Selected navigation leading rule | #334155 |
| Island radius | 12px |
| Control radius | 8px |
| Island shadow | 0 1px 2px rgba(15,23,42,.04), 0 6px 18px rgba(15,23,42,.06) |
| Font | existing system UI sans stack |
| Base | 14px / 20px |
| Field labels | 12px / 16px, weight 600 |
| Page title | 24px / 32px, weight 600 |
| Panel title | 16px / 24px, weight 600 |
| Query/results data | existing monospace stack, 13px / 20px |
| Desktop control min-height | 36px |
| Mobile/coarse-pointer target min-height | 44px |
| Control horizontal padding | 12px |
| Icon-to-label gap | 8px |
| Adjacent control gap | 12px |
| Desktop content gutters / island padding | 24px |
| Mobile content gutters / island padding | 16px |
| Island gap | 24px desktop / 16px mobile |

Use flat fills and a subtle consistent shadow. Orange remains in RDF4J branding only. All menu links and their icons use slate; Query selection uses slate fill/rule/ink. Result URIs use muted teal and a restrained underline. Preserve existing term/explore link behavior, including literal links; literal content can use ink. Keyboard focus uses a visible teal outline with spacing. Disabled controls must be clearly disabled without losing readable text. Apply color rules to visited links too.

## Icons and controls

Use a small original inline SVG symbol/template set, consistent viewBox, 1.75px stroke, rounded caps/joins, no fill, currentColor. Navigation icons are 18px; control icons 16px. Keep visible text labels; decorative icons are aria-hidden and unfocusable. Icon-only editor or Close controls have accessible labels and tooltips. Use symbols meaningful to the existing action; do not copy unrelated legacy artwork.

Navigation: server, database/repositories, plus/new, trash/delete, summary/chart, braces/namespaces, layers/contexts, shapes/types, search/explore, terminal/query, bookmark/saved, download/export, edit/update, plus/add, minus/remove, eraser/clear, info/information.
Actions: play/Execute, tree/Explain, bookmark/Save, sliders/Options, download/Download, expand/shrink Full screen, chevrons pagination and disclosures, clipboard/copy, swap arrows, comparison/Diff, X/Close, hamburger/Menu.

Buttons, summary/disclosure triggers, selects, inputs and explanation controls share the same height, type, border and radius rules. Primary Execute is teal with white label/icon. Other actions are white/slate with a pale hover fill. A disclosure has a distinct up/down chevron with 8px gap and 12px edge space. Fields and button labels cannot collide or clip. No broad overrides of unrelated pages.

## Shell and mobile navigation

Desktop uses a compact white header with logo left and existing server/repository/user context right. Context values may wrap; Change stays with its own context row. Use a roughly 200px white navigation island plus 24px gap and fluid main content. Do not draw the old heading underline.

At narrow widths the context moves below the logo, labels/values wrap without horizontal overflow, and navigation becomes a white Menu disclosure above the page title. It is closed by default on narrow screens, keyboard operable, with aria-expanded and an associated nav region. Opening it reveals every existing link and group in normal document flow. Desktop shows the nav continuously. Avoid leaking inert/hidden state across resize or navigation. Keep context visible and usable; do not invent a new connection popover.

Define responsive transitions by available space (one documented shell breakpoint is fine), test 390px, 768px and landscape. At 320px and 200% zoom the page must remain operable. Use min-width:0 on flexible regions and wrap long identifiers.

## Query island and options

Normal query island: title, editor, toolbar, then any expanded settings. The editor retains its behavior, syntax highlighting and own utilities. Use a useful initial editor height around 220px desktop / 180px mobile, and preserve its existing resize/fullscreen features. Do not replace it with a static mockup.

Toolbar desktop: Execute / Explain Query left; Save query / Options right. Mobile: Execute / Explain first row, Save query / Options second row. If localized labels need more space, wrap or use a single column; never clip or force the page wider.

Opening Save or Options does not move its trigger into another location. Expanded panels sit below the whole toolbar, each spanning the island width, in order Save then Options. Inset pale fill, 12-16px padding, section label, labelled fields. Desktop fields can share a grid row; mobile fields stack with labels above full-width inputs. Save preserves query name, private/auth state and save confirmation. Query options preserve page size, timeout, inference, Clear and language where supported. Initial server defaults/cookies remain authoritative. No new values based on mockup examples.

Native details may be used if they support this structure cleanly. Otherwise use accessible disclosure buttons with aria-expanded/aria-controls and hidden panels through a shared helper. Keep the main form and submission targets correct.

## Results island, table and records

Treat the result heading and tools as ONE component. Desktop header aligns Query Result left and Download / Result options / Full screen right. Mobile uses title plus Full screen first row, Download / Result options second row. Do not fake alignment using negative offsets across the iframe. The result document can own the complete visual component while the parent owns the frame host, loading/error lifecycle and expanded-mode state.

Open result panels appear beneath the toolbar, in flow. Download and Result options may share a two-column settings grid on wide desktop; on narrow screens they stack full width. Result options exposes labelled Layout select with Auto, Table, Records; checked Wrap values by default; existing datatype/language control. Preserve the download format, all-results and actual download action. Settings remain collapsed by default and stored view preferences survive frame replacement, paging and reload where browser storage is available. A storage denial must not break execution.

Default Layout Auto, Wrap values on. Auto uses a fitted semantic table only when the available result width meets a documented, font-relative minimum readable column budget for EVERY column. A reusable CSS token such as 12ch plus normal cell padding is acceptable; do not key decisions on fixture strings or a particular number of columns. Otherwise render Records: each result row becomes a vertically listed record with every variable/field name associated with its complete value. Do not hide columns, truncate data, use nested card mosaics, or require lateral scrolling. For exceptionally long variable names, stack label above value. Null/unbound values remain represented accurately. Support tuple and graph shapes, empty responses, long IRIs, literals, language/datatype displays and RDF-star nested terms according to existing rendering.

Explicit Table preserves a table even when cramped. Wrap values toggles wrapping live without a query request. With wrapping on, table-layout and word breaking must fit the available width, including long URLs/words. Explicitly disabling wrapping may use an internal horizontal scroller; the parent page never overflows. Records without wrapping may scroll long values within the result surface; no data is silently lost. Explain the table/no-wrap tradeoff only in brief option help if needed.

Use one DOM representation or an accessible deliberate presentation transform; avoid duplicate exposed bindings or divergent click/download behavior. A CSS display transformation must retain meaningful roles/header associations; explicit record labels should be accessible. The complete original term text and links remain available and downloads unchanged.

Result body owns long vertical data scrolling, with header/options and pagination reachable. Short results size naturally without a blank 640px hole. Expanded settings must not be clipped by a data-height cap. Table header stays useful during scroll. Pagination remains bottom right on desktop and comfortably spaced on mobile. Preserve actual enabled states and page behavior.

Auto recalculates on width changes, display options, frame replacement and fullscreen entry/exit. A ResizeObserver can observe available width; guard against height feedback/reflow loops and disconnect observers when replacing frames.

## Full screen

Provide a labelled Full screen toggle in the result header, including phones. Full screen fills the browser viewport width and height with the results component and removes surrounding app/editor chrome from that view. Visible Exit full screen restores it, and Escape exits. Keep controls, data, options and paging usable. Do not open a tab/window, navigate, rerun the query or reset result state.

The parent owns the expanded frame host across child navigation. A viewport-filling in-page mode is sufficient and should be reliable on mobile; native Fullscreen API is optional, not an acceptance requirement. If using native API, handle rejection/user-agent exit and use a viewport fallback. Prefer explicit same-origin communication with existing origin/source checks; do not bypass query-result request isolation.

The result's complete toolbar should live with the result contents so it aligns naturally. Its Full screen action can ask the parent host to expand. Preserve current result, scrolling, layout/wrap preferences and paging. Child replacement while expanded must retain the expanded host and correct exit state. Account for dynamic viewport units/safe areas. Escape from within the child document also exits. Restore focus to the current Full screen trigger after exit; old frame references must not be reused. Expanded mode must contain keyboard focus appropriately and hide/inert background from assistive navigation, restoring prior state on exit/page cleanup. Loading, empty/error and cancelled states cannot strand the user without an exit.

## Explain / Compare / Diff

Keep Explain opt-in and all existing controls. Apply the same tokens, labelled selects, controls, icons and spacing. Single mode: query editor and its explanation region, then shared explanation controls. Compare mode: equal desktop columns of editor plus explanation each, stacked at narrow widths. Copy to comparison / Swap controls form a normal toolbar above the pair, with visible accessible labels, rather than floating between columns.

Explanation settings use an in-flow Config disclosure, not an overlapping floating panel. Preserve view/level, Normal/Heatmap, download and actual graph/text/table behavior. Use restrained slate/teal for normal text/tree accents; keep meaningful heatmap/diff colors. Long textual output wraps or scrolls inside its own region without page overflow.

Diff retains query and explanation sections, existing add/remove semantics, and clear Close control. Size to content up to the viewport, then scroll internally; remove the fixed giant empty area. Mobile keeps 16px edge space, readable wrapped text, touch-sized Close and correct focus/Escape lifecycle. Do not drop Compare/Explain actions from mobile.

## Implementation and verification contract

Before production edits, align the new red tests with this public behavior: Layout is a labelled select (not an assumed radio group); Full screen may be viewport mode and can be owned by the parent host. Test visible geometry, preservation, focus, Escape and requests, not a guessed fullscreenElement implementation. Open disclosures before interacting. Preserve the original red evidence, then rerun revised semantic cases red before implementation.

Keep the ExecPlan current, one in_progress; append initial evidence. Reuse current module abstractions and isolate query styles. Consolidate superseded query CSS rather than layering contradictory overrides. Regenerate only required tracked TypeScript JS/maps with the existing toolchain.

Acceptance:
- Red-to-green automated coverage for controls, all fields in many-column Auto/Records, wrapping on/off, resize/table/records transitions, fullscreen/Escape/exit/focus, paging/replacement while expanded, and no new request/window.
- Desktop 1440, tablet768, phone390 and 320, landscape, 200% zoom; open/closed Menu, Save, Options, Download, Result options; long labels/values, many columns, empty/unbound cells.
- No default horizontal page or result scrolling; explicitly selected no-wrap/Table keeps overflow internal.
- Full original embedded lifecycle/GET/POST/paging/download/Save/Explain/Compare/cancel/error/standalone behavior retained. Check graph and boolean too.
- Complete Node suite, focused Java transformation regressions as relevant, Workbench module gate via mvnf, live Chromium with screenshots and no new console errors.
- Capture final desktop/mobile closed and all-open, narrow Menu open, wide data Auto, forced Table wrap/no-wrap, desktop/mobile fullscreen, Explain/Compare/Diff and error state. Inspect them against these references and fix differences before handoff.
- Update deterministic SVG wire diagrams from this contract (not editing mockup pixels), and retain all prompts, images, red/green logs and evidence. No commit/push/branch change.

Final handoff should give concise outcome, exact checks/counts, artifact paths and any real limitations. Design fidelity is part of completion, not deferred follow-up.
