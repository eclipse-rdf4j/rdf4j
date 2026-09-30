# Workbench spacing validation and evidence

The final uncommitted candidate fixes every confirmed spacing class S1–S14 and its recorded review extensions. The complete list is [audit.md](audit.md), its denominator is [source-inventory.md](source-inventory.md), and the measured matrix, RED evidence, harness classifications, and browser health observations are [browser-audit.md](browser-audit.md). The living plan is [workbench-spacing-execplan-20260929.md](../../workbench-spacing-execplan-20260929.md).

Work remains on `GH-6071-workbench-redesign` at `d6e9ea70a547b48942106cec515cdb6839c21728`. No stage, commit, push, branch change, or pull request was performed for this task. Existing artifacts remain intact.

Final repository status is 13 tracked modifications, zero staged paths, and 3,895 untracked paths: the retained 3,888-path baseline plus the seven new task test/document paths listed in the scope manifest. `git diff --check` passes. Full status and the untracked path list are preserved as `git-status-final.nul` and `git-untracked-final.txt` at the temporary evidence root.

## Final results

| Gate | Result and evidence |
| --- | --- |
| TypeScript | Repository compiler passes; generated JavaScript/maps accompany the three changed TypeScript modules. `typescript-final.log`, `typescript-review.log`. |
| Headers and formatting | Copyright/SPDX check and required offline Maven formatter pass. `copyright-v3.log`, `format-v3.log`. |
| Frontend unit tests | 239 passed, zero failed/skipped. `frontend-unit-v3.log`. |
| Workbench Java tests | 602 passed, zero failures/errors/skips through the scoped localhost-enabled `mvnf` run. `workbench-java-escalated.log`, `workbench-final-reports.tar.gz`. The restricted sandbox run's seven localhost socket errors remain preserved separately. Java source did not change after the successful gate. |
| Final package | Offline root quick installation succeeds in 17.040 seconds. `asset-install-v3.log`; root `maven-build.log` retains the latest install, with preceding logs backed up. |
| Served provenance | 42/42 JavaScript/CSS SHA-256 values match the final workspace, zero mismatches. `fixed-v3-served-asset-manifest.json`. |
| Chromium spacing | All 22 spacing cases close; the final keyboard S14 selection additionally passes. |
| Chromium finite audit | 22/22 pass. |
| Firefox spacing | All 22 spacing cases close; the final keyboard S14 selection additionally passes. |
| Firefox finite audit | Original run: 16 pass/six teardown failures. Explicit draining of the gated abort handler repairs the harness; the same six cases pass 6/6. This is composed closure, not a single clean 22-case run. |
| WebKit spacing | Original run: 21 pass/one pointer-focus setup failure. The rendered native activation probe establishes the distinction; the unchanged strict focus assertion passes through visible keyboard activation in the final S14 selection. All 22 spacing cases close through those reports. |
| WebKit finite audit | 15 pass/seven strict health failures: `ResizeObserver loop completed with undelivered notifications`. Every geometry assertion/capture completes. No page-error filtering or relaxed health assertion is used. |
| Strict saved-row focus | Separate keyboard reproducer remains RED in all three engines. It preserves the full stress workflow and exact `toBeFocused` assertion. This is a verified pre-existing functional limit, not a spacing-green claim. |
| Independent review | Root finds no remaining spacing issue after desktop/mobile, reveal/collapse/settled state, rapid reversals, visible keyboard focus, short viewport, scrolling, resize, long source, and populated saved execution checks. Root also approves the reconciled ledger, exact validation boundaries, scope manifest, and final desktop screenshot. |

Evidence filenames above are relative to `/tmp/rdf4j7-workbench-spacing-20260929`. Browser reports and artifacts are under its `browser/` directory. The exact selection argv, real process exit codes, original and revised spec hashes, source/asset provenance, composed closure counts, and retained fixtures are recorded in `browser/fixed-v3-final-closure-manifest.json` and the compact `scope-evidence-manifest.json` at the evidence root. Failed original reports are retained alongside focused closure reports; no test is skipped or marked expected-to-fail.

The finite matrix covers all 17 destinations and 18 create forms at desktop 1440 px and mobile 390/320 px in both themes. Related transitions additionally use 768 px, and S14 adds 320×568 and 768×520 reachability/scrolling checks. The same semantic spacing and containment assertions run in Chromium, Firefox, and WebKit. Native browser/OS dialogs are identified as platform-owned; DOM triggers and resulting in-page layout are covered.

## Baseline health and functional boundaries

The identical WebKit observer diagnostic is directly reproduced before and after fixes in representative tuple/graph runs with 42 baseline HEAD assets staged read-only through `git show` (29 assets used per fresh context). All four replays emit it, with zero overflow. `browser/webkit-observer-differential.json`, `browser/observer-baseline-assets/provenance.json`, and four associated traces/screenshots preserve the differential. These representative runs do not imply that each of the seven health-failing audit cases was separately compared. The unchanged observer owner calls `renderAndReport`; no unrelated production repair is included.

`browser/webkit-pointer-focus-probe.json` records actual Query Options pointer close→BODY and keyboard Enter close→the same focused BUTTON. Shared S14 close acceptance therefore uses visible keyboard activation in all engines and preserves its exact focus assertion. Main-result close focus passes. The saved-row renderer can remove/reinsert a retained article and drop focus to BODY; its strict baseline reproduction still fails before and after the spacing owner correction.

Other preserved limits are the namespace-containing query-stream protocol failure, invalid Add Turtle HTTP 500, submitted operation errors that return GET models, anonymous private-save business behavior, saved Edit's XML response, the saved Show label remaining unchanged, and a source-grounded saved-fullscreen fallback limitation without a live failure claim. Legal production-renderer replay and namespace-free fixtures provide explicit rendering coverage where a baseline business flow is unavailable. Previous optional publication browser results remain 37 pass/20 untriaged failures and were not rerun as a spacing gate. Exact evidence and scope boundaries are in the audit.

## Source scope

Production changes comprise 13 tracked files: three TypeScript sources (`create.ts`, `queryStream.ts`, `workbenchViews.ts`), their six compiler-generated JavaScript/source maps, and four stylesheets (`workbench-refresh.css`, `query.css`, `query-explanation.css`, `query-compare.css`). The patch uses shared relationship tokens, control/inset ownership, semantic field/action/output groups, responsive minimum tracks, and one sticky result-panel flow owner. It preserves natural field widths, deliberate card/disclosure insets, compact native glyphs, bounded long-source scrolling, existing motion, and data/query behavior.

New regression coverage is `e2e/tests/workbench-spacing-system.spec.js` and `e2e/tests/workbench-spacing-audit.spec.js`. These and the audit/plan files remain untracked and unstaged. The root `initial-evidence.txt` retains compact initial RED, later extension RED, and final verification summaries; original logs/reports/screenshots/traces remain on disk.

## Commands

Repository commands run from `/Users/havardottestad/Documents/Programming/rdf4j7`:

```sh
bash tools/workbench/compileTypescript.sh
(cd scripts && ./checkCopyrightPresent.sh)
mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick install
python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs
(cd e2e && npm run test:unit)
git diff --check
```

`-q` is used only for formatting. Test Maven commands never use `-q` or `-am`. Install output belongs to `maven-build.log`; full test output remains in retained test logs. The initial clean install had already completed earlier in this conversation, and reports were preserved before subsequent runner cleanup.

The browser runner is `browser/run-final-engine.cjs` at the evidence root. It runs each engine's system selection, finite audit, and strict baseline focus separately, with one worker, zero retries, trace capture, line/JSON reporters, explicit owned loopback URLs, and unique output paths. Each engine manifest records the actual argv and exit code. Focused harness/keyboard closures have their own reports and source snapshots rather than overwriting the failed originals.

## Retained preview and representative screenshots

Preview: `http://127.0.0.1:18817/rdf4j-workbench`. The owned review repository is `spacing-review-50467-mun6z6qr`; its namespace-free MemoryStore is reseeded after preview restarts and its saved metadata stays retained. Query and saved-page URLs use `/repositories/spacing-review-50467-mun6z6qr/query` and `/repositories/spacing-review-50467-mun6z6qr/saved-queries` under that preview. The final read-only fixture and process details are in the scope/evidence manifest. Artifact review is approved; the preview and fixtures remain available for user review.

The browser worker visually inspected both final representative saved captures:

- Desktop metadata, compact source, and populated two-row execution: `browser/retained-review-verified-1790727438472.png` (1440×1076 full-page image).
- Short-viewport Download panel and long neighboring headings: `browser/fixed-v3-system-chromium-results/workbench-spacing-system-S-34350-e-scroll-and-card-expansion-chromium/S14-saved-short-viewport-download-1440-light.png` (320×2066 full-page image from a 320×568 viewport). The filename retains the original variant label; its adjacent geometry records the actual resized viewport and 8 px anchor.

Optional orphan fixture cleanup was rejected by automatic approval review before execution as destructive datastore mutation without specific authorization. It was not retried; no repository was recreated or saved query deleted by that cleanup. The retained orphan metadata names old repository `workbench-spacing-system-41246-munav5fb`, query `urn:uuid:9008435d-533c-45ba-8a86-4a0abfd3b1b4`, and name `source-246-munav5fb-a-320-l`. No temporary spacing repository configuration remains. Exact prior UI-save ownership and rejection evidence are preserved in the final manifest; the retained orphan does not block spacing acceptance.
