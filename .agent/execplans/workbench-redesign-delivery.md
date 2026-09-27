# Deliver the RDF4J Workbench Redesign

This is the living delivery plan for the accumulated Workbench redesign in the repository root. It follows `.agent/PLANS.md` and preserves unrelated working-tree changes and every untracked artifact. The goal is a focused, reviewable commit, a pushed issue-numbered branch, a draft PR against the confirmed development base, and a PR comment with real packaged-app screenshots and accurate test evidence. Do not merge.

## Purpose / Big Picture

The Workbench redesign brings its configuration forms, query workflow, results, navigation, and themes into a consistent compact responsive interface while preserving existing behavior. The accumulated changes also include upload-context semantics, independent all-results downloads, N-Quads repository export with compression choices, deployable menu/page/query-feature policy, compare-pane lifecycle, and server-side enforcement. The user should be able to install the pushed branch, run the Workbench, exercise these flows, and review representative real screenshots and test results from the draft PR.

## Progress

- [x] (2026-09-27) Search redesign issues and PRs.
- [x] (2026-09-27) Create issue and name branch.
- [x] Audit accumulated source and artifacts.
- [x] Normalize embedded native select margins.
- [x] Fix narrow results toolbar overflow.
- [x] Match embedded result header ink.
- [x] Verify mount-aware repository menu links.
- [x] Run affected module and browser gates.
- [x] Capture curated final screenshots.
- [x] Commit, push, draft PR, comment.

## Surprises & Discoveries

- The only close issue-search result is closed/stale issue #470 about an old jQuery UI theme; it does not describe this redesign or its backend/configuration scope.
- The first sandboxed `gh auth status` read reported an invalid token, and connector issue creation returned HTTP 403. The repository owner confirmed that the authenticated CLI session with `repo` scope works through the authorized network path, and CLI issue creation succeeded.
- Issue #6071 was created after open/closed searches found no suitable current redesign issue; the branch is now `GH-6071-workbench-redesign`.
- The untracked tree has 3,375 paths totaling about 367.6 MB: 2,630 PNGs, 645 logs, and a much smaller set of source, test, documentation, planning, and manifest files. Curate self-contained design deliverables and reviewed actual-app screenshots; preserve every other artifact locally.
- The working tree contains accumulated Workbench implementation changes plus thousands of untracked artifacts. Preserve all; publish only source, tests, documentation, plans, and a small reviewed screenshot evidence set, not transient browser/Maven logs or intermediate design iterations.
- The final repository-mount regression showed that Servlet PATH mappings are relative to the servlet context and take precedence over shortened `pathInfo`. `WorkbenchPageUrl` builds the mount from `contextPath + mappedPath` without stripping a matching prefix; all 14 focused policy tests pass.
- The final packaged gates passed: Workbench 544/544 tests, Server Boot 57 unit and 2 integration tests, and focused Server Spring 9/9 tests. Final packaged Chromium and WebKit navigation checks each pass 1/1.
- Fourteen reviewed application screenshots are under `docs/workbench-redesign-evidence/`. The self-contained design gallery contains 32 boards with paired light/dark references (64 linked PNGs); intermediate prompts, exploratory mockups, logs, and unlinked images remain local.
- The audited delivery commit `8a93c1be2410158adedac5c257e32b452177546b` contains 190 files, including source, generated assets, tests, plans, the curated gallery, and 14 packaged-app screenshots. The selected gallery assets total 85,345,272 bytes; the exact app logo asset is hash-verified in the gallery.
- The commit is pushed to `origin/GH-6071-workbench-redesign`. Draft PR #6072 targets `develop`; its comment #5854968498 contains the final test counts and ten embedded packaged-app captures, with the complete fourteen-image set linked.

## Decision Log

- Decision: Search open and closed issues before creating a new one, and do not reuse issue #470.
  Rationale: #470 is an old closed request for the jQuery UI overcast theme, not the current redesign or full-functionality scope.
  Date/Author: 2026-09-27, Codex.
- Decision: Use the authenticated GitHub CLI for authorized issue and PR writes after the connector rejected issue creation.
  Rationale: The sandboxed token failure was caused by network isolation; the authorized CLI path confirms repository scope.
  Date/Author: 2026-09-27, Codex.
- Decision: Create issue #6071 and rename the existing branch to `GH-6071-workbench-redesign`.
  Rationale: Searches found no suitable existing issue; the approved body records the complete UI, behavior, policy, and verification scope.
  Date/Author: 2026-09-27, Codex.

## Outcomes & Retrospective

Issue #6071 tracks the redesign. The full audited implementation and curated evidence are committed on `GH-6071-workbench-redesign`, pushed to `origin`, and available in draft PR #6072 against `develop`. The PR comment contains test summaries and real application screenshots. No merge was requested or performed.

## Context and Orientation

The starting branch was `fix-query-results-same-tab-regression`, with origin `https://github.com/eclipse-rdf4j/rdf4j.git`; it is now `GH-6071-workbench-redesign`. Issue #6071 tracks the full Workbench redesign. The dirty tree accumulates multiple completed Workbench tasks and must be audited as a whole against `origin/develop` at `dc712dc0086b`, including both tracked edits and untracked files.

The primary completed implementation plan is `.agent/execplans/workbench-redesign-completion.md`. Related focused plans include `.agent/execplans/workbench-form-options-sizing.md`, `.agent/execplans/workbench-motion.md`, `.agent/execplans/workbench-disclosure-anchors.md`, and `.agent/execplans/workbench-light-query-theme.md`. These plans, test sources, implementation files, and downstream configuration documentation are candidates for delivery. Historical logs, temporary fixtures, test output, browser traces, generated intermediate mockups, and personal path data are not to be staged automatically.

The real-app fixture ran on loopback port 8086 with isolated app data. Preserve that fixture through final review. Use the repository's `mvnf` workflow for enabled Maven tests, with the required `.m2_repo`, offline preference, and no `-am` or `-q` in test commands. Quick package/build commands may use `-am` only while tests are skipped.

## Plan of Work

First, inventory the branch's complete tracked diff and every untracked path by purpose, size, and type. Compare committed history with the intended development base and distinguish completed implementation from unrelated diagnostics or generated noise. Keep the working tree intact while choosing the exact deliverable set.

Second, issue discovery and branch naming are complete. Confirm the PR base from repository history and GitHub while auditing the complete source and artifact inventory.

Third, rebuild and exercise the packaged Workbench. Run the Workbench module tests, affected Server Boot/configuration tests, and focused Server Spring tests for the update/upload Base URI fix. Run the full frontend unit suite and the relevant real-browser functional/motion/theme cases. Do not substitute previous logs for tests that the accumulated final source has invalidated. Record red-first evidence already preserved in `initial-evidence.txt` and append new evidence without replacing earlier results. The final mount-mapping addition has a failing report and passing policy/module reports summarized in `initial-evidence.txt`.

Fourth, capture actual packaged-app screenshots for Query/Explain, Query options, comparison close, upload/export, configuration, and representative mobile layouts in light and dark themes. Review each image and keep a concise curated set in a dedicated documentation evidence directory. Verify that every selected file is safe to publish and contains no local credentials, user paths, or transient logs.

Finally, format and check the complete intended source diff, stage only the audited source/tests/docs/plans and curated screenshots, create a commit beginning `GH-<issue-number>`, push the issue-numbered branch with tracking, and open a draft PR targeting the confirmed development branch. Attach the PR to the task. Post one concise PR comment containing test counts, actual screenshot embeds from the committed evidence files, and limitations. Verify the pushed SHA, PR head/base/title/body, comment, screenshot URLs, and remaining local staged/unstaged status; leave unrelated and transient artifacts untouched. This work is complete: the branch was pushed, PR #6072 was created and attached, and comment #5854968498 was verified against the committed screenshot paths.

## Concrete Steps

Run commands from the repository root unless stated otherwise. Begin with `git status --short`, `git diff --stat`, `git diff --cached --stat`, `git log --oneline --decorate`, and `git merge-base HEAD origin/develop`. Use `git ls-files --others --exclude-standard` plus a read-only inventory script to classify untracked paths; never use `git clean` or broad `git add -A`.

Use the existing `gh-read-inspector` script for supported detailed issue/PR reads, and GitHub search for discovery. The `yeet` publication skill requires authenticated `gh`; `gh auth status` was verified through the authorized network path. Use `gh` with escalation for repository writes when sandbox network isolation requires it.

Use `python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs`, followed by targeted Server Boot and Server Spring test selections as warranted by the final code. For the packaged-browser run, keep the Workbench bound only to loopback and use the existing 8086 fixture. Preserve complete test logs under `/private/tmp` or append-only repository evidence, then use their report summaries in the PR.

## Validation and Acceptance

The local delivery is ready when the complete change set is reviewed against `origin/develop`, the intended Workbench, Server Boot, Server Spring, and frontend suites pass or have precise documented skips, and real Chromium/WebKit flows confirm query/explain, options, upload, export/download, configuration enforcement, comparison lifecycle, dark/light themes, reduced-motion behavior, and narrow-screen usability. Fresh screenshots must come from the rebuilt packaged Workbench and be checked for sensitive local data. The final Workbench module report records 544 tests with no failures, errors, or skips. Server Boot records 57 unit tests and 2 integration tests with no failures, errors, or skips. Focused Server Spring records 9 tests with no failures, errors, or skips. Final packaged navigation selection is green in both browser engines.

The publication is ready when an actual Workbench redesign issue exists, the local branch is named `GH-<issue>-workbench-redesign`, the source/docs/tests and curated screenshots are committed with a `GH-<issue>` message, remote branch SHA equals local HEAD, and a draft PR links the issue and targets the verified development branch. The PR comment must display the committed screenshots and exact final test evidence. No merge is part of this task.

## Idempotence and Recovery

All discovery, verification, and screenshot capture can be repeated without deleting user data. Rebuild and restart only the owned 8086 process, retaining its isolated app-data directory. Preserve all prior logs, screenshots, plans, and untracked artifacts. If GitHub authentication fails, leave the branch and local patch intact; do not switch to a guessed issue number, force-push, or overwrite credentials. After access is restored, repeat issue search to avoid a duplicate, create or select the real issue, and resume branch naming and publication.

## Artifacts and Notes

The final reviewed packaged-app captures are listed in `docs/workbench-redesign-evidence/README.md`; ten are embedded in PR comment #5854968498. The source-grounded 32-board light/dark catalog is `design/workbench-design-coverage-20260925/index.html`; it links 64 PNGs and includes the original logo, palette calculations, source-fidelity specifications, and sanitized manifest. Machine-local report logs remain outside the source deliverable; compact red/green evidence is preserved in the local root `initial-evidence.txt`.

Issue search commands were run through GitHub search for all states, including `"workbench redesign"`, `"workbench refresh"`, `"workbench theme"`, `"workbench UI"`, `"redesign Workbench"`, and `"Workbench layout"`. The closest pre-existing result, #470, is closed with stale/completed state and only asks for an old jQuery UI theme. New issue #6071 was created with the full functionality scope.

## Interfaces and Dependencies

No product dependency is added by this delivery. Use the installed GitHub connector only for operations it authorizes, `gh` only after its authentication is valid, the repository's `mvnf` skill for Maven tests, the existing Playwright installation for browser automation, and Git's ordinary non-force branch/push operations. If one publisher cannot perform both Git and issue/PR writes, use the available authorized path for each operation and verify every remote result rather than assuming success.

Plan revision (2026-09-27): Completed final route mapping regressions and reran Workbench and Server Boot module gates plus packaged Chromium/WebKit navigation. Captured and reviewed fourteen packaged-app screenshots, curated the self-contained 32-board gallery, and staged only audited deliverables. Commit `8a93c1be2410158adedac5c257e32b452177546b` was pushed; draft PR #6072 targets `develop`, was attached to the task, and comment #5854968498 publishes the test summary and actual-app captures. The plan is complete; the PR remains draft and unmerged.
