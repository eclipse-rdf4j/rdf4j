# Show useful errors for streamed query timeouts

This ExecPlan follows `.agent/PLANS.md` and remains the single in-progress plan for this task. It is a focused follow-up to the completed Workbench rendering migration: it preserves the current Lit page shell and NDJSON transport while making a query timeout after visible result rows understandable and safe to retry.

## Purpose / Big Picture

When a Workbench query emits rows and then reaches its configured execution timeout, the user must see that the displayed rows are partial, the request has stopped, and how to adjust the timeout before retrying. The page must not describe incomplete rows as a completed total or imply that the results can be downloaded as a complete answer. The behavior will be demonstrated through an actual packaged Workbench query whose result stream is interrupted after rows have rendered, plus focused client and servlet tests.

## Progress

- [x] (2026-09-29 06:52 +02:00) Inspect dirty tree and query stream paths.
- [x] (2026-09-29 06:52 +02:00) Run required root clean install.
- [x] (2026-09-29 07:00 +02:00) Reproduce post-row servlet timeout failure.
- [x] (2026-09-29 07:08 +02:00) Reproduce packaged timeout in browser.
- [x] Add server and client error contracts.
- [x] Repair typed timeout and partial-result UI.
- [x] Distinguish incomplete EOF and user cancel.
- [x] Cover tuple and graph error paths.
- [x] (2026-09-29 08:19 +02:00) Verify packaged timeout browser flows.
- [x] Run affected modules and frontend gates.
- [x] Format, summarize evidence, preserve artifacts.
- [x] Parent reviews packaged timeout behavior.
- [x] Add timeout markers and partial navigation regressions.
- [x] Repair timeout and breaker classification.
- [x] Keep partial Records locally navigable.
- [x] Rerun focused and affected gates.
- [x] Refresh evidence and review runtime.
- [x] Add narrow error-wrap browser regression.
- [x] Wrap long result errors accessibly.
- [x] Rebuild package and verify mobile.
- [x] Complete parent mobile visual review.
- [x] Stop owned server and handoff.

## Surprises & Discoveries

- Observation: `QueryResultRenderer.render()` currently reports a generic streaming error after a failure, but it computes the usual page/count label and leaves ordinary result controls visible. `QueryResultRenderer.fail()` also marks the result complete for display-state purposes, while `QueryResultState.complete` is used to represent protocol termination. These states must be separated so incomplete data is not presented as a successful total.
  Evidence: `tools/workbench/src/main/webapp/scripts/ts/queryStream.ts`, methods `QueryResultRenderer.render()` and `fail()`.
- Observation: `QueryServlet.evaluateQuery()` rethrows `HTTPQueryEvaluationException` unless it wraps malformed SPARQL. The servlet's committed page-data error handler can emit an NDJSON terminal error after earlier batches, but its message currently needs to distinguish query timeout from generic upstream/truncation errors.
  Evidence: `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/QueryServlet.java`, methods `evaluateQuery()` and `writeCommittedPageDataError()`.
- Observation: Existing streamed query browser coverage lives in `e2e/tests/query-stream-packaged-compatibility.spec.js`; focused client contracts live in `e2e/tests-unit/query-stream-refinements.test.js`. The required clean install completed successfully before this task's tests.
  Evidence: `maven-build.log` ends with `BUILD SUCCESS` at 2026-09-29T06:52:53+02:00.
- Observation: The first servlet regression sends 32 typed rows through the live Workbench page writer, then raises `QueryInterruptedException` from the iterator. The writer ends the response with `error`, but the terminal record has no timeout code; the focused Surefire test fails exactly on that missing classification.
  Evidence: `initial-evidence.txt` and `tools/workbench/target/surefire-reports/org.eclipse.rdf4j.workbench.commands.QueryServletHeartbeatTest.txt` report `expected: "timeout" but was: ""` after confirming the 32-row record.
- Observation: A packaged query with `query-timeout=1` showed 32 rows in the browser while busy, then failed after the server logged `QueryInterruptedException: Query evaluation took too long`. The remote binary tuple-result path truncated to EOF, which Workbench reported as a generic error; the result status still called 55,108 rows a total and exposed Download. This confirms the real remote seam and distinguishes the server timeout from transport EOF by matching the request and server log.
  Evidence: `e2e/query-timeout-after-partial-results-browser-20260929.log` records first rows at 739 ms and terminal at 9,927 ms; `e2e/query-timeout-server-20260929.log` records the server timeout at 07:04:25.971 and the client EOF at 07:04:29.991.
- Observation: After rows have streamed, a typed timeout now uses an error terminal record with code `timeout`, leaves the result incomplete, labels retained rows as partial, hides completion counts and downloads, and clears busy/cancel state. Generic EOF is rendered as incomplete with conditional timeout guidance and without exposing the Java exception; cancellation keeps distinct wording.
  Evidence: `QueryServletHeartbeatTest` passes 19 cases; `e2e/tests-unit/query-stream-refinements.test.js` passes the timeout/EOF/cancel contract; full Workbench frontend unit suite passes 211/211.
- Observation: Tuple/graph failure variants now cover a direct CONSTRUCT timeout and an HTTPRepository-style graph EOF after 32 emitted statements; only a typed timeout is called a timeout.
  Evidence: `QueryServletHeartbeatTest.constructTimeoutAfterStatementsKeepsTheGraphRowsAndTypedTimeout`, `remoteConstructEofAfterStatementsIsIncompleteAndDoesNotClaimTimeout`, and `remoteTruncatedTupleStreamAfterRowsIsIncompleteNotTimeout`.
- Observation: At 390px, the timeout recovery alert had a 338px client width but a 707px scroll width because its `<pre>` defaulted to `white-space: pre`; the existing `overflow-wrap: anywhere` cannot wrap that white-space mode. `pre-wrap` preserves line breaks while allowing normal and unbroken-token wrapping.
  Evidence: `e2e/query-timeout-mobile-wrap-red-20260929.log` fails on `707 > 338`; `e2e/query-timeout-mobile-wrap-green-20260929.log` passes with `338 == 338`, `white-space: pre-wrap`, and 16 rendered line fragments for a long unbroken token.
- Observation: Parent's final packaged review at 390px confirmed the live timeout message visibly wraps across three lines, stays within the alert width, and leaves results idle with no completion/download controls.
  Evidence: Parent live review confirmed 338px client width and 338px scroll width after 87,066 streamed rows; the same green browser log independently proves a timeout followed by successful rerun.

## Decision Log

- Decision: Preserve terminal NDJSON framing and report only timeouts that are identified from the server's actual query-timeout exception/cause chain. A premature EOF or arbitrary HTTP transport failure remains a generic incomplete-stream error.
  Rationale: A transport truncation can follow a timeout but is not proof of one; labeling all stream failures as timeout would mislead users and mask network faults.
  Date/Author: 2026-09-29 / Codex.
- Decision: Keep already-rendered rows available for inspection, add explicit partial-result language, and suppress completion-only count/download semantics when terminal state is an error.
  Rationale: Retained rows help the user understand progress, while clearly marking them partial prevents treating them as a complete query answer.
  Date/Author: 2026-09-29 / Codex.
- Decision: Let the RDF4J server emit the existing binary tuple-result error record after a committed timeout, then classify only the parsed canonical timeout record in Workbench.
  Rationale: Binary tuple results already define an error marker and the parser already recognizes it. A committed HTTP response cannot change its status, and raw EOF cannot prove why it stopped; an explicit in-band marker preserves rows and distinguishes server timeout from unrelated truncation without changing the format version.
  Date/Author: 2026-09-29 / Codex.

## Outcomes & Retrospective

The RDF4J binary tuple servlet now writes its existing typed evaluation-error marker after a committed timeout, allowing Workbench to classify a remote tuple timeout without treating arbitrary EOF as proof. Workbench emits stable `timeout` or `incomplete` error records; the renderer keeps partial rows navigable, removes completion-only counts/downloads, clears busy/cancel state, and separates user cancellation, timeout, circuit-breaker, and generic truncation messages. Actual packaged Chromium tests showed SELECT partial rows before a typed timeout, idle/cancel cleanup, hidden complete-result controls, and a successful rerun; CONSTRUCT interrupted after streamed statements remains correctly labeled incomplete because raw RDF EOF cannot prove timeout. A 390px regression now verifies the full recovery instruction wraps; a long unbroken-token probe confirms wrapping without losing a newline. Focused verification is green (frontend unit suite 213/213; Workbench module 514/514; server-spring 168/168; server-boot 65/65 plus its integration test). Copyright, formatting, and diff checks passed. The isolated packaged review server remains available for parent review.

## Context and Orientation

The Workbench query page submits a browser-owned query request to `QueryServlet`. For the interactive result view, the request negotiates the internal typed NDJSON media type. `QueryServlet` evaluates a local or remote repository query and `WorkbenchPageResultWriter` flushes row batches before a terminal `end` or `error` record. In the browser, `queryStream.ts` parses records, stores rows outside the DOM, and updates the result view. A timeout after a row batch therefore crosses repository evaluation, servlet exception classification, the terminal NDJSON record, and UI state/count/download controls.

The query timeout is specified in seconds by the existing `query-timeout` field and is interpreted by `QueryEvaluator`. `QueryInterruptedException` is the RDF4J timeout/cancellation exception; remote HTTP query failures may wrap it in `HTTPQueryEvaluationException`. An incomplete HTTP response without that cause is a transport failure and must retain generic wording.

## Plan of Work

The smallest failing tests and real browser reproduction are preserved in `initial-evidence.txt`. The server emits a typed terminal error for a tuple timeout; raw RDF graph EOF remains generically incomplete. Workbench retains and labels streamed rows as partial, disables success-only totals/downloads and server paging, leaves local Records navigation available, and clears busy/cancel state.

Parent's packaged mobile review has confirmed the wrapped recovery instruction after partial rows. The task-owned review server was stopped gracefully after deleting only the task-created parent review repository from its isolated app-data directory. Existing timeout, cancellation, stale-run, circuit-breaker, typed data, and graph-vs-tuple distinctions remain intact.

## Concrete Steps

Work in `/Users/havardottestad/Documents/Programming/rdf4j7`. The required initial command has completed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

It produced `maven-build.log` with `BUILD SUCCESS`. Before any production edit, run the new focused failing in-repository test and append its compact report to `initial-evidence.txt`. Use `python3 .codex/skills/mvnf/scripts/mvnf.py <selection> --retain-logs` for Java tests; the runner serializes the required install and verify steps. Do not pass `-am` or `-q` to tests. Use the repository Playwright project for the real-browser reproduction, with unique repository IDs and the existing isolated app-data pattern under `/private/tmp`.

After implementation, compile TypeScript through the Workbench script so `scripts/*.js` and source maps match the `.ts` source. Run the focused client contracts, `QueryServlet` focused selection, then the Workbench module test gate and any directly affected packaged-browser specs. Keep full Maven and browser logs; append concise red and green excerpts to `initial-evidence.txt`.

## Validation and Acceptance

The red browser run must show all of the following before production changes: the query result table contains at least one data row; the results region is still busy; the request has not reached a terminal record; after the configured deadline, the server logs a typed timeout and the browser records the resulting transport/UI state. The current red shows the server timeout followed by remote binary EOF and misleading partial-result controls. This sequence is reproduced with a real disposable repository and an actual one-second timeout, not a simulated timeout.

After the fix, the same run must show visible retained rows explicitly labeled partial/incomplete, an actionable timeout message, `aria-busy=false`, hidden/disabled cancellation, no successful total/page count or complete-result download, and a successful subsequent rerun. A user-triggered cancel must remain labeled cancelled rather than timed out. A generic truncated stream must remain a generic stream error. For an HTTPRepository-backed request, classify a wrapped query-timeout cause as timeout; classify an unrelated HTTP/EOF failure generically.

## Idempotence and Recovery

All test repositories use generated identifiers in the isolated test server, and teardown deletes only those task-created repositories. Keep the previous UI refinement and XSLT migration files untouched except for necessary generated query-stream assets. Do not delete unrelated files, stop servers not started by this task, or commit/push. If a browser timeout fixture proves flaky, switch to a controlled repository/evaluation fixture and retain the failing evidence instead of weakening the outcome assertions.

## Artifacts and Notes

The required clean-install log is `maven-build.log`. The initial and final compact test evidence belongs in the existing root `initial-evidence.txt`. New timeout-specific browser logs and screenshots should use distinct names under `e2e/` or the existing task visual artifact directory, and generated repositories must be removed during teardown.

## Interfaces and Dependencies

No new dependency or public API is planned. The existing internal NDJSON record with `type: "error"`, HTTP `status`, and `message` carries server timeout classification after streaming has begun. `QueryResultRenderer` will expose the error/partial state without treating it as a successful terminal result. Any change to the wire shape must remain backwards-compatible with generic NDJSON consumers and preserve cancellation request IDs.
