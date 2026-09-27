# Restore the Develop Light Theme for Query and Explain

This ExecPlan follows `.agent/PLANS.md` and remains the living record for this task. It preserves the user’s existing Workbench changes and changes only the light code-editor/Explain color treatment.

## Purpose / Big Picture

In the light theme, Query should again use the CodeMirror/YASQE default theme currently present on the `develop` branch, and Explain output should use the same semantic colors. Users can verify this without reloading by switching Light and Dark on the Query page: Light restores the white/black editor, gray gutter and current develop token colors; Dark retains the existing refreshed dark palette in the editor and both explanation views.

## Progress

- [x] (2026-09-27) Compare local and upstream develop theme sources.
- [x] (2026-09-27) Add rendered regression and record red.
- [x] (2026-09-27) Restore light editor and Explain colors.
- [x] (2026-09-27) Verify theme changes in both browsers.
- [x] (2026-09-27) Capture views and complete evidence.
- [x] (2026-09-27) Verify editor caret across themes.
- [x] (2026-09-27) Match Explain tokens to rendered editor spans.

## Surprises & Discoveries

- The checked-in develop theme is provided by `styles/yasqe.min.css`, not by the old `query.css`: `.cm-s-default` defines syntax tokens and YASQE defines the white editor, gray gutter, line numbers, and focus-sensitive selection.
- Current `styles/workbench-refresh.css` applies its new code palette to both light and dark modes through unscoped CodeMirror rules. Current `styles/query-explanation.css` maps Explain token classes to the new Workbench colors, so merely removing the CodeMirror override would leave Explain visually inconsistent.
- `develop` and `origin/develop` resolve to `dc712dc0086b53ca113432144479bec45da360aa` in this checkout.
- The real-page Chromium regression observed the refresh palette in Light: editor foreground `#2f4047`, gutter `#f5f9fb`, keyword `#20576a`, variable3 `#594b9a`, string `#3e6b32`, number `#5d4a9e`, and selected text `#def3fb`; Explain node/variable/value/metric classes also used refreshed colors. Dark observations matched the existing dark code palette.
  Evidence: `/private/tmp/workbench-light-theme-red-final-20260927.log` reports one failed test and prints the full `QUERY_THEME_COLORS` snapshots.
- The first parity test incorrectly selected the `cm-variable-3` URI span as the query variable. Exact DOM inspection shows `?subject` is `.cm-atom` (`#221199`), `<urn:name>` is `.cm-variable-3` (`#008855`), and `"Example"` is `.cm-string` (`#aa1111`). The follow-up compares those exact rendered texts with the matching Explain tokens. Numeric tokens remain `#164` (`rgb(17, 102, 68)`).
- YASQE's more-specific cursor rule keeps the caret black in both themes. The rendered regression confirms this baseline behavior, so the surrounding dark palette remains unchanged.
- Follow-up inspection must identify CodeMirror tokens by their rendered text and type, rather than assuming every `cm-variable-*` span is a SPARQL variable. Explain names, IRIs, and literal values need to track their corresponding editor token categories, while preserving existing text, compaction, titles, and dark colors.

## Decision Log

- Decision: Scope the develop palette to light-mode editor presentation and map Explain semantic token classes to the matching CodeMirror token categories. Keep the existing dark-mode declarations and dark token values intact.
  Rationale: The user requests parity with develop for light mode and consistency between Query and Explain; this avoids changing the surrounding Workbench palette or the accepted dark theme.
  Date/Author: 2026-09-27, Codex.
- Decision: Test real Query and Text/structured Explain output, including focused and unfocused selection and a live light/dark/light switch.
  Rationale: Computed styles from the running YASQE editor and actual explanation renderers verify the behavior while preserving the user's theme preference workflow.
  Date/Author: 2026-09-27, Codex.
- Decision: Keep the editor theme in shared CSS variables and use that light palette for Text and JSON Explain, while retaining the existing dark values.
  Rationale: One palette source prevents editor and explanation views from drifting apart; dark remains the separately accepted palette.
  Date/Author: 2026-09-27, Codex.

## Outcomes & Retrospective

The red semantic-parity browser regression showed that the actual query variable was `cm-atom`, while the prior test had mistaken the IRI's `cm-variable-3` class for the variable. The corrected output maps Explain subject tokens to the develop `cm-atom` color, IRI-valued plan tokens to the rendered `cm-variable-3` color, and literal values to `cm-string`. The renderer keeps the original `value` kind and compaction/title behavior, adding only IRI/literal presentation classes; dark IRI and literal values inherit the same prior dark value color. Verification passed in Chromium and WebKit, the focused highlighter unit file (15/15), and all frontend units (96/96). Current real-app screenshots are `/private/tmp/workbench-query-explain-semantic-light-text-20260927.png`, `/private/tmp/workbench-query-explain-semantic-light-json-20260927.png`, `/private/tmp/workbench-query-explain-semantic-dark-text-20260927.png`, and `/private/tmp/workbench-query-explain-semantic-dark-json-20260927.png`. Red/green reports and the focused package build are appended in `initial-evidence.txt` and `maven-build.log`.

## Context and Orientation

The repository is `.`. `tools/workbench/src/main/webapp/styles/yasqe.min.css` supplies the vendored `cm-s-default` CodeMirror appearance on develop. `styles/workbench-refresh.css` overlays current theme styles on Workbench CodeMirror and its syntax tokens; its `html[data-theme="dark"]` block contains the current dark palette. `styles/query-explanation.css` defines Explain text and structured-view token categories through `--query-code-*` custom properties. `scripts/ts/queryExplanationHighlighter.ts` emits semantic token classes for actual Explain output and its checked-in JavaScript counterpart is generated source. The e2e browser tests in `e2e/tests/workbench-explain.spec.js` already create an isolated test repository, run Explain, and exercise both highlighted text and structured output.

## Plan of Work

The browser regression in `e2e/tests/workbench-explain.spec.js` was observed failing against the task-owned packaged page before production style edits. It sets a query with keyword, variable, string, and numeric tokens; inspects editor surface, gutter, focus-sensitive selection, and token colors; runs actual Text and JSON Explain output; and switches Light to Dark to Light without page reload.

After recording the red results, shared editor variables in `styles/workbench-refresh.css` restore develop's light CodeMirror surface, gutter, selection, and syntax categories while retaining the existing dark values. `styles/query-explanation.css` maps the Text Explain variable, IRI, and literal categories to the actual matching editor tokens. `queryExplanationHighlighter.ts` retains `kind: value` for display compaction and raw-title behavior while adding rendered-only IRI/literal subtype classes; JSON rendering and token classification are otherwise unchanged. The Workbench and server-boot package were rebuilt; focused Chromium/WebKit tests, the highlighter unit file, and full frontend unit suite passed. Actual Query/Text/JSON Explain views were captured in both themes. TypeScript regeneration and `git diff --check` are clean.

## Concrete Steps

All commands run from the repository root unless a working directory is stated. Preserve the current task-owned Workbench fixture at `http://127.0.0.1:8086/rdf4j-workbench`; only rebuild or restart that fixture if the packaged stylesheet changed. The browser regression is run from `e2e` with the existing Playwright installation and the repository/server base URLs set to the owned loopback fixture. Store Playwright logs and screenshots in a unique directory under `/private/tmp`, outside the Playwright managed output directory. Append a short command/report/snippet block to `initial-evidence.txt`; never replace the existing file.

Before editing production styles, the focused browser test must fail on at least one light-mode computed color while passing its fixture setup. After the CSS change, run the same test in Chromium and WebKit. Expected light values include `#fff` editor background, `#000` editor text, `#f7f7f7` gutter, `#ddd` gutter border, `#999` line number, focused `#d7d4f0` selection and unfocused `#d9d9d9` selection, with the YASQE token colors recorded in this test. Dark assertions must continue to observe the current dark palette without navigation or reload.

## Validation and Acceptance

Acceptance is met by a rendered test of live YASQE and populated Text/JSON Explain. The exact Light comparisons are `?subject` (`cm-atom`, `#221199`) ↔ Explain subject, `<urn:name>` (`cm-variable-3`, `#008855`) ↔ Explain IRI, and `"Example"` (`cm-string`, `#aa1111`) ↔ Explain literal. Editor surface, gutter, focused/unfocused selection, metrics, and JSON colors remain asserted; live Light/Dark/Light switching preserves dark values. Chromium and WebKit each pass the focused test; the highlighter unit file passes 15/15 and full frontend unit suite passes 96/96. Screenshots capture Query plus Text and JSON Explain in both themes. TypeScript is regenerated; `git diff --check` is clean. No unrelated Java module tests were needed for this frontend change.

## Idempotence and Recovery

The regression reuses the existing test setup that deletes only its own `testrepo1` fixture before recreation. Keep screenshots and logs under a unique `/private/tmp` directory so a rerun does not overwrite earlier evidence. Do not switch branches, commit, push, stop unrelated processes, delete artifacts, or modify the CodeMirror/YASQE vendor file. If the packaged fixture is stale, rebuild and restart only the recorded port-8086 process and keep its existing appdata directory.

## Artifacts and Notes

Baseline source: `git show develop:tools/workbench/src/main/webapp/styles/yasqe.min.css` contains the `cm-s-default` palette; `git show develop:tools/workbench/src/main/webapp/styles/query-explanation.css` contains the previous semantic Explain palette. Red/green reports, screenshot paths, build output, and final checks are recorded in the workspace `initial-evidence.txt`, `/private/tmp`, and append-only `maven-build.log`.

## Interfaces and Dependencies

No new dependency, framework, or runtime API is needed. Keep the checked-in `yasqe.min.css` vendor asset unchanged. The test uses existing Playwright and the Workbench theme selector `#workbench-theme`; styles should continue to follow the already-present `html[data-theme="dark"]` attribute. Preserve existing semantic token classes and base `value` kind; add only rendered IRI/literal classes to value spans so Text and structured Explain retain their existing data and interactions.

Plan revision (2026-09-27): The initial light-theme pass restored the develop editor surface/token palette and added Text/JSON Explain mappings. The follow-up corrected an invalid class-based variable assumption through exact text/type assertions, added semantic IRI/literal display subtypes while retaining base value/compaction behavior, and maps Explain subject to `cm-atom`. Both follow-up regressions were observed red before production edits, then passed after the fix. TypeScript generation, 15 focused highlighter unit tests, all 96 frontend units, Chromium and WebKit visual contracts, package rebuild, screenshots, and diff check are complete.

## Follow-up: semantic token parity

The prior regression inferred the `?subject` color from a class-based list of `cm-variable*` spans and incorrectly selected the URI. Live CodeMirror inspection established the exact text/class/color for `?subject`, `<urn:name>`, and `"Example"`; the red browser test asserted parity with Explain before production edits. A red unit test then established the need for URI/literal presentation subtypes. The completed renderer adds those classes only to the span while retaining the base `value` kind, compaction, raw title, and text. Dark URI/literal colors remain the same as the pre-change dark value color.
