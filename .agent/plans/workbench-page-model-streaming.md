# Stream native Workbench page models with bounded memory

This ExecPlan is a living document and follows `.agent/PLANS.md`. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` current as work proceeds.

## Purpose / Big Picture

Large native Workbench form responses can currently exhaust server heap before the browser sees the page. The response wrapper retains the entire page model in a byte array, copies it, and the HTML shell creates another complete Base64 string. After this change, a large successful or error page still carries the same `data-workbench-initial-model` value, while the server stages the model with bounded memory and streams its Base64 representation into HTML. A user can verify this by submitting a large native form, receiving the same page and error details, and by running the focused tests below.

## Progress

- [x] (2026-10-23 08:01Z) Inspect native model routes and buffers.
- [x] (2026-10-23 08:01Z) Run initial root quick install.
- [x] (2026-10-23 08:04Z) Add smallest failing memory regression.
- [x] (2026-10-23 08:04Z) Capture first Surefire evidence permanently.
- [x] (2026-10-23 08:24Z) Implement bounded model staging and cleanup.
- [x] (2026-10-23 08:38Z) Verify response parity and failure lifecycle.
- [x] (2026-10-23 08:44Z) Run focused and Workbench module tests.
- [x] (2026-10-23 08:50Z) Format, review, and report candidate.
- [ ] (in_progress) Publish draft and verify exact head.

## Surprises & Discoveries

- `WorkbenchPageModelResponseWrapper` buffers native POST page models in `ByteArrayOutputStream`, then calls `body.toByteArray()`; `WorkbenchHtmlShell.writeInitialPageModel` Base64-encodes into another full string. `WorkbenchServlet.writeUnauthorizedPageModel` has a second potentially large path because it echoes a posted configuration value. `WorkbenchGateway.writeServerPageModel` and `WorkbenchServlet.writeRepositoryNotFound` create smaller page models through the same shell method.
- The initial isolated root quick install succeeded in the `workbench-page-model` Maven workspace.
- The focused regression failed before production changes: `TupleServletPageDataTest.largeNativePageModelSpillsAndRoundTripsWithoutRetainingItInHeap` reported `Tests run: 1, Failures: 1, Errors: 0` and `Expected size: 1 but was: 0 in: []`. The first report is preserved in top-level ignored `initial-evidence.txt`; full log is `.mvnf/workspaces/workbench-page-model/logs/20261023T080237.398688Z-91107-811df439/verify.log`.
- The unauthorized configuration path independently failed its spill assertion before that route changed (`initial-evidence.unauthorized.txt`). A writer-close compatibility test later exposed an intermediate failure in the staging wrapper (`initial-evidence.writer-close.txt`); preserving wrapper ownership corrected it. The focused wrapper class then passed seven tests, and `WorkbenchServletTest` passed 36 tests.
- An HTML client-disconnect regression failed before the shell checked `PrintWriter` errors (`initial-evidence.client-disconnect.txt`). The shell now detects a broken writer during chunked Base64 output, stops reading the staged model, and lets the wrapper delete the temporary file. The focused page-model test class passed 9 tests afterward.

## Decision Log

- Decision: Keep the browser's inline page-model protocol and the existing HTML shell shape. Replace in-memory whole-model retention/copies with a request-owned staging stream and chunked Base64 output.
  Rationale: Browser behavior and response semantics can remain compatible while heap use becomes bounded. A per-request temporary file must be deleted on success, redirect, servlet failure, and writer failure.
  Date/Author: 2026-10-23 Codex.
- Decision: Keep this change separate from Workbench query result streaming and metadata listing work.
  Rationale: Those paths have distinct lifecycle and compatibility concerns; draft PR #6232 already streams tuple and graph query rows.
  Date/Author: 2026-10-23 Codex.
- Decision: Treat a servlet closing its response writer as a flush of the staged model, retaining wrapper ownership until `complete()` or the enclosing try-with-resources exits.
  Rationale: The old byte-array staging remained readable after writer close. A red test showed the first file-backed version accidentally rejected that valid lifecycle.
  Date/Author: 2026-10-23 Codex.

## Outcomes & Retrospective

The native wrapper, direct shell, and unauthorized configuration paths now stage large page models outside the heap and preserve the existing inline Base64 attribute. Four regressions failed before their corresponding production edits and now pass. The final Workbench module gate passed 1,338 tests with zero failures, errors, or skips; its compact summary is in retained top-level `final-evidence.txt` and the full log is `.mvnf/workspaces/workbench-page-model/logs/20261023T084029.480816Z-98766-d2cddb5d/verify.log`. Copyright checking, formatting, and `git diff --check` also passed. A single large posted configuration still exists as a request parameter string before model rendering; this patch bounds model staging and Base64 encoding, not multipart/request parsing. Large page models now consume temporary disk until the response finishes, so a full temporary filesystem causes a request error rather than heap retention. Independent review found no blocker and approved scoped draft publication. Output and request quotas remain a separate compatibility decision.

## Context and Orientation

The module is `tools/workbench`. `WorkbenchPageModelResponseWrapper` in `src/main/java/org/eclipse/rdf4j/workbench/base` wraps native form POST responses so `TransformationServlet` can finish a page model before deciding whether to send a redirect, error, or HTML shell. `WorkbenchHtmlShell` embeds that model as URL-safe Base64 in one HTML attribute. `WorkbenchServlet` and `WorkbenchGateway` under `proxy` also write inline models. The browser reads the attribute and decodes the page data in `src/main/frontend/classic/workbenchApp.ts`. `WorkbenchPageResultWriter` writes the structured page records.

The worktree is `/Users/havardottestad/.codex/worktrees/security-workbench-page-model-streaming/rdf4j`, branch `GH-0000-workbench-page-model-streaming`, based on current main `6b390ad88a9c5b8e79dd0e9afaea0154c8fbfffa`. Its `.m2_repo` is a read-through link to the original checkout cache. Use `--workspace workbench-page-model` for test isolation and retain all logs and evidence. No production code changes precede the failing in-repo test.

## Plan of Work

First add a focused regression to an existing Workbench test class that writes a page model beyond a small bounded staging threshold. It must prove the model leaves heap-backed staging and that the HTML attribute still decodes byte-for-byte. Add failure and redirect variants to verify temporary-file cleanup and preserved response status. Persist the first failing Surefire report in top-level `initial-evidence.txt` before editing production.

Then add a request-private page-model staging abstraction in `tools/workbench`, with a small in-memory prefix and a temporary file for larger models. The owner closes and deletes it in every servlet exit path. Update the wrapper to use it and close it even if servlet execution fails. Make `WorkbenchHtmlShell` accept an input stream, use the existing URL-safe no-padding encoder, and stream ASCII Base64 to the response writer without building a whole string. Migrate every inline page-model call site to the shared stream path; especially avoid full copies of the posted configuration in the unauthorized response. Keep small, direct page-data responses and native query execution semantics unchanged.

## Concrete Steps

From the worktree root, the initial build command was `python3 scripts/mvn-agent.py --workspace workbench-page-model -- -B -ntp -o -Pquick clean install`; it ended `BUILD SUCCESS`. For focused tests run `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-page-model <TestClass#method> --retain-logs`, without `-am` or `-q`. After each run inspect only the reported full-GAV Surefire directory and preserve the first red snippet in `initial-evidence.txt`. For the module gate run the same command with `tools/workbench` as the selector. Before final review run `cd scripts && ./checkCopyrightPresent.sh`, the repository formatter, and `git diff --check`. Stage only source, tests, and this plan after independent review.

## Validation and Acceptance

A large native POST page model must render the same decoded page-record bytes as before, without a whole-model byte array or Base64 string in the server. A redirect sends no inline model and deletes staging. An exception before `complete`, a response write failure during `complete`, and an unauthorized configuration POST all release staging. Small page models and ordinary page-data responses remain byte-compatible. Focused tests must be red before production change and green afterward; the Workbench module suite must complete with no new failures. The first red and final green reports and full logs remain in the worktree.
