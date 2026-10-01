# Group advanced repository settings by label prefix

This is a living ExecPlan for the create-repository advanced settings change. Maintain it in accordance with `PLANS.md` and the repository `AGENTS.md` instructions.

## Purpose / Big Picture

Advanced repository settings should follow meaningful shared word prefixes in their existing labels, while each setting occupies one complete row. The grouping must be generated from current field metadata, retain full labels and controls, and remain stable across repository type changes and viewport widths.

## Progress

* [done] inspect field metadata and naming hierarchy (2026-10-01)
* [done] run required offline root install (2026-10-01)
* [done] observe focused browser grouping RED (2026-10-01)
* [done] implement shared word-prefix hierarchy
* [done] verify responsive groups and form preservation
* [in_progress] hand off final audit

## Surprises & Discoveries

- The create-page enhancement currently moves all advanced rows into one flat `.workbench-advanced-fields` container. The container uses a multi-column flex layout at desktop widths, which explains both missing config groups and multiple settings on one row.
- The live LMDB form exposes labels including `Query Iteration Cache sync threshold`, `Query Evaluation Mode`, `Slow query ...`, `Triple indexes`, `Triple DB size`, several `Value ...` and `Namespace ...` settings, `Sketch estimator ...`, `Optimizer sampling ...`, and `Background raw sampling ...`. This produces shared prefixes `Query`, `Slow query`/`Slow query log`, `Triple`, `Value`, `Namespace`, `Sketch estimator`/`Sketch estimator context`/`Sketch estimator throttle`, `Optimizer sampling`/`Optimizer sampling max`, and `Background raw sampling`. Labels such as `Bulk operation size`, `Auto grow`, `Page cardinality estimator`, `Inline literals`, `Force sync`, and `No readahead` have no peer prefix and remain standalone.
- The first browser regression failed with no group nodes. After inspection, the test expectation was broadened from the initial eight multiword groups to every actual group, including one-word `Value`, `Namespace`, `Query`, and `Triple` prefixes. The implementation was added only after preserving the focused RED.
- Evidence: the new Chromium regression failed against the unmodified renderer with `shared word prefixes should become nested groups`; full output is retained at `/tmp/rdf4j7-workbench-create-groups-20261001/prefix-groups-red.log` and `prefix-groups-red-complete.log`, with the RED snippets appended to root `initial-evidence.txt`.
- The final preview uses the original appdata at `/private/tmp/rdf4j7-workbench-spacing-20260929/runtime/appdata`. Read-only route checks and viewport/disclosure interactions retained the existing `spacing-review-50467-mun6z6qr` repository; no create form was submitted.

## Decision Log

- Decision: group any complete-word prefix with at least two descendant settings, including one-word prefixes, and keep true singletons as top-level fields.
  Rationale: this uses the user-visible naming scheme without relying on broad raw namespaces or creating artificial singleton groups.
  Date/Author: 2026-10-01 / Codex.
- Decision: keep the existing advanced disclosure and move controls rather than cloning or renaming them.
  Rationale: form identity, defaults, accessibility relationships, and submission behavior already depend on those nodes.
  Date/Author: 2026-10-01 / Codex.

## Outcomes & Retrospective

The create page now builds nested groups from shared complete-word label prefixes, compressing single-child paths and leaving true singleton fields at the top level. Existing control nodes, full labels, values, and the Advanced disclosure remain intact. Every advanced setting occupies one grid row at desktop and mobile widths.

The focused create contract passed 4/4 Chromium tests with command `RDF4J_WORKBENCH_BASE_URL='http://127.0.0.1:18817/rdf4j-workbench' npx playwright test --project=chromium --workers=1 --reporter=line --output=/tmp/rdf4j7-workbench-create-groups-20261001/create-contract-chromium-green tests/workbench-create-contract.spec.js`; the log ends `4 passed (10.5s)`. It covers the 12 LMDB prefix groups, standalone singleton controls, no empty/single-setting groups, 33 separate field rows at 1280px and 390px, preserved form snapshots, and LMDB-to-memory type switching. Rendered review measured no horizontal overflow or create-page errors. The root reviewer independently confirmed the groups and edited/reverted one setting without submitting the form.

The regenerated JavaScript and source map plus CSS match the packaged Spring Boot jar and live HTTP resources byte-for-byte. Offline root install, server-boot package, copyright check, `process-resources`, TypeScript compile, Node syntax checks, and `git diff --check` passed. Screenshots are `/tmp/rdf4j7-workbench-create-groups-20261001/create-advanced-desktop-1280.png` and `/tmp/rdf4j7-workbench-create-groups-20261001/create-advanced-mobile-390.png`. The live preview is `http://127.0.0.1:18817/rdf4j-workbench/repositories/NONE/create?type=lmdb` and remains running against the original appdata as PID 76113. A final JSON manifest is in the same `/tmp/rdf4j7-workbench-create-groups-20261001/` directory.

## Context and Orientation

`tools/workbench/src/main/webapp/scripts/ts/create.ts` moves optional rows from the create form table into the shared Advanced settings disclosure. Each row retains its label and controls, including `data-config-property`, input name, selected/default value, and required markers. `tools/workbench/src/main/webapp/styles/workbench-refresh.css` owns the disclosure field layout. The focused rendered contracts live in `e2e/tests/workbench-create-contract.spec.js`.

## Plan of Work

First, extend the create-page browser contract with LMDB prefix groups, singleton retention, complete setting rows, and type-switching preservation, then run that focused test against the local Workbench and capture its initial failing report. Next, introduce a reusable word-prefix grouping routine in the create-page enhancement. It will derive branches from visible labels, compress single-child paths, group only shared prefixes, and retain each existing field as one intact DOM node. Finally, make each advanced field take the full group width, regenerate JavaScript, run focused unit/browser coverage, package the local preview, and verify desktop/mobile layout and form identity in the rendered page.

## Concrete Steps

1. [done] Run the required offline root quick clean install; its log is `/tmp/rdf4j7-workbench-create-groups-20261001/maven-build.log`.
2. [done] Add and run the LMDB grouping/layout regression against the local Workbench without submitting the form.
3. [done] Append the initial RED snippet to root `initial-evidence.txt`, preserving earlier evidence.
4. [done] Implement and test the shared prefix hierarchy and one-setting-per-row layout.
5. [done] Regenerate assets, package/refresh the preview, and verify form identity, console health, type switching, desktop/mobile layout, and screenshots.
6. [done] Deliver final scoped evidence and handoff.

## Validation and Acceptance

The in-repository browser regression must fail before implementation because the current advanced fields are flat and can share a row. After implementation, the focused rendered browser selection must pass for LMDB nested and one-word groups, true singleton controls, type switching, and one setting per row at desktop and mobile widths. Existing form snapshots must remain identical when Advanced opens. The client-side create renderer has no direct Workbench unit selection, so the rendered browser contract is the focused behavioral test. Run TypeScript regeneration, formatting/copyright checks, and local package/served-asset verification; do not run unrelated suites.

## Idempotence and Recovery

The grouping is a client-side presentation transform and must be safe when the create page initializes once per repository type. It must move, never clone, existing field nodes. If a validation step fails, preserve RED logs and unrelated artifacts, correct the grouping algorithm or its test oracle, and repeat only the affected selection.

## Artifacts and Notes

The first observed RED is appended to `initial-evidence.txt`; full logs and screenshots remain outside the repository under `/tmp/rdf4j7-workbench-create-groups-20261001/`.

## Interfaces and Dependencies

Use the existing create-page DOM enhancer and Advanced settings disclosure. Group structure is presentation-only: input `name`, `id`, `data-config-property`, `data-field-role`, labels, form values, and submit behavior remain unchanged. No new dependency or server-side config schema is needed.

Plan Note: Parent reviewed the restored original-appdata preview: all 12 nested groups and 33 setting rows remained distinct at desktop and mobile widths, and editing a value survived collapse/reopen. Acceptance and final scoped handoff are complete.
