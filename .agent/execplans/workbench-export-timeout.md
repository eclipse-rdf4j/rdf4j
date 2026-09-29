# Make Workbench repository exports survive long transfers

This ExecPlan is maintained according to `.agent/PLANS.md`. The Workbench export page should let a user download the full repository without the Workbench's HTTP client ending a response that is still transferring. Its timeout control defaults to 43,200 seconds and applies to the upstream HTTP response used by an HTTP-backed repository connection. A value of zero means no timeout. The statement-table preview remains a separate, explicitly requested operation limited to 100 statements by default.

## Purpose / Big Picture

When a user downloads N-Quads from Workbench, the page streams statements from its repository connection into the browser response. For a remote HTTP repository, Workbench also reads an upstream HTTP response while doing that. A short timeout on that upstream client can stop a valid transfer after the browser download has begun. The page's timeout must reach that specific upstream request and must not change a shared HTTP client's settings for other requests.

The behavior is observable with a loopback HTTP endpoint that flushes one N-Quads statement, pauses longer than a configured 100 ms client socket timeout, and then sends another statement. A direct HTTP request with a longer timeout receives the complete file. A Workbench export configured for five seconds must also receive both statements, a one-second export must terminate during a longer pause, and zero must permit an unbounded wait. The same client's 100 ms setting must remain in effect for another connection.

## Progress

- [x] Add explicit preview, full-repository download, gzip, and timeout form controls.
- [x] Keep preview retrieval separate and default preview to 100 statements.
- [x] Prevent export XML metadata from leaking into HTML.
- [x] Reproduce Workbench mid-response timeout over real HTTP.
- [x] Apply isolated upstream response timeout and zero semantics.
- [x] Verify backend timeout isolation and stream completion.
- [x] Align export controls and verify narrow layouts.
- [x] Capture red UI regression and apply styles.
- [x] Recheck icon actions and download behavior.
- [x] Run module tests, format, and hygiene.

## Surprises & Discoveries

- `HTTPRepositoryConnection.getStatements` collects all remote statements, but Workbench preview uses `exportStatements` with a handler that stops at the row limit. A loopback regression confirms the preview handler receives only 100 statements and closes its response before a 100,000-statement source finishes.
- `TimeLimitRDFHandler` checks an elapsed deadline while RDF callbacks run. It cannot by itself change the timeout on the HTTP client that is reading a remote response. The user's report that the browser download begins and then stops requires a mid-body transport regression, not only a delayed-header test.
- `HTTPRepositoryConnection` already implements `HttpClientDependent`, and each connection owns its protocol session. A client decorator can therefore add a timeout to that connection's requests without editing the repository-wide shared client configuration.
- `HttpRequest` already supports an optional per-request response timeout, copied through redirected requests. The JDK backend maps it to `java.net.http.HttpRequest.Builder.timeout`; that builder rejects `Duration.ZERO`, so the meaning of “unlimited” needs an explicit backend check and test.
- Before the last CSS pass, the desktop browser measured the format field at 150px while its native select was 113px; compression was 78px while its select was 69px, and both had native arrows instead of a matching disclosure chevron. The timeout label determined the first grid track. A rendered-template regression and a real-browser layout assertion failed before the select wrapper/grid styling (`logs/mvnf/20260927-153344-verify.log`).

## Decision Log

- Decision: Keep the full download as a native response stream and use a separate form submission for the preview. Rationale: exporting the full repository must not buffer it in a Workbench heap, and changing the preview selector must not trigger a large request. Date/Author: 2026-09-27 / Codex.
- Decision: Use a request-scoped timeout on the connection's HTTP client, alongside the RDF callback deadline already applied by Workbench. Rationale: the request-scoped timeout governs waiting for upstream response data, while the callback deadline governs elapsed export work. Neither requires changing the shared repository client for unrelated connections. Date/Author: 2026-09-27 / Codex, approved by task coordinator.
- Decision: Reproduce with a short configured socket timeout and a delayed mid-body response. Rationale: the exact timeout setting in the user's deployment is not yet known, so the regression proves the same mechanism under a deterministic local setting without claiming that this exact setting caused the reported incident. Date/Author: 2026-09-27 / Codex.
- Decision: Preserve `timeout=0` as unlimited. Rationale: this matches Workbench timeout controls and the existing RDF4J HTTP client configuration contract, where zero means no timeout. Date/Author: 2026-09-27 / Codex.

## Outcomes & Retrospective

The preview, streamed N-Quads/gzip download, and per-connection HTTP response timeout are implemented and verified. The format and compression controls now share aligned widths and the Workbench disclosure chevron; at 390px and 320px the form stacks without horizontal overflow, and the action icons remain inside their clickable labels. Final checks passed: the Workbench module (557 tests), JDK HTTP client timeout test, e2e unit tests (98), TypeScript compilation, source copyright check, repository formatter, and `git diff --check`.

The native browser download form sends the timeout through RDF4J's existing `timeout` request parameter because a normal HTML form cannot set an arbitrary HTTP header. Workbench applies it as a per-request HTTP response timeout on the upstream repository connection, plus its callback elapsed-time guard. The transport timeout is an idle-response interval and cannot interrupt a stalled blocking socket before the HTTP client reports its timeout. The exact deployment timeout that caused the reported interruption remains unknown; the loopback regression proves the mechanism using a deterministic short client timeout.

## Context and Orientation

`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/ExportServlet.java` handles the Workbench export form. Its download branch opens a `RepositoryConnection`, streams every statement through an RDF writer, and optionally wraps the browser output in gzip or ZIP. Its preview branch emits at most the selected number of statements to the page.

`core/repository/http/src/main/java/org/eclipse/rdf4j/repository/http/HTTPRepositoryConnection.java` uses an `RDF4JProtocolSession` for remote repository requests and implements `HttpClientDependent`, which provides `getHttpClient()` and `setHttpClient()`. `core/http/client-api/src/main/java/org/eclipse/rdf4j/http/client/spi/HttpRequest.java` carries a per-request response timeout. Apache HC5 and JDK implementations live in `core/http/client-apache5` and `core/http/client-jdk`. The client's response timeout is a maximum idle interval while waiting for response data; Workbench's RDF callback timeout is an elapsed export budget. The export page sends its timeout value with the request so Workbench can apply both relevant controls.

The principal regressions belong in `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/commands/ExportServletCoverageTest.java`, which already exercises a real HTTPRepository through a local HTTP server, and backend-specific tests in the `core/http/client-*` modules if zero-timeout behavior needs correction. The loopback test is local-only and needs permission for a local listening socket in this sandbox.

## Plan of Work

First, extend the Workbench loopback coverage to include a direct request that receives the complete delayed N-Quads body, followed by an HTTPRepository export through Workbench using a client configured with a 100 ms socket timeout. The server must flush a statement before pausing, so the regression demonstrates a response that has already begun rather than a connection that merely delays headers. Assert that a five-second export completes with both statements. Add a one-second selection with a longer body pause to prove the selected export budget is still honored, an unlimited selection to verify zero, and a second repository connection that retains the client's 100 ms behavior.

Run the narrow Workbench regression and preserve its failing report before changing production code. If the response pause does not reliably reproduce, adjust only the local server coordination or timing until the test proves that it receives the first chunk and times out while awaiting the next one; do not change production code to make the test green.

After the red test, wrap the `RDF4JHttpClient` returned by the export connection's `HttpClientDependent` interface. The wrapper copies each request with `HttpRequest.copyOf`, sets a per-request response timeout for positive seconds, and delegates execution. It must not close the delegate, mutate the shared repository client, change unrelated headers or request bodies, or affect other connections. Restore the original client in a `finally` block after the export. For local repositories, retain the RDF callback deadline and avoid introducing HTTP-client behavior.

Treat zero as an explicit no-timeout value through the HTTP client SPI. Verify Apache HC5 and JDK behavior; if the JDK backend falls back to a shorter configured socket timeout or rejects zero, make the smallest documented SPI/backend change so zero suppresses both per-request and configured socket deadlines for that request. Add a backend regression rather than relying on the Workbench's currently selected provider.

Finally run the Workbench export, servlet, and XSL tests, the affected HTTP client backend tests, the complete Workbench module tests, TypeScript compilation, and the existing browser harness at desktop and mobile sizes. Verify the default page is empty, changing the preview count makes no request, the retrieve button loads no more than 100 statements, and a gzip N-Quads download includes more than 100 statements and named graph contexts. Run the repository copyright, formatter, and diff checks and report test evidence separately from any environment-limited browser check.

## Concrete Steps

Run from the repository root, `/Users/havardottestad/Documents/Programming/rdf4j7`.

1. Use `.codex/skills/mvnf/scripts/mvnf.py` to run only the new real HTTP timeout test. Local loopback binding may require sandbox escalation. Save its Surefire report and compact summary in `initial-evidence.txt` before further runs overwrite it.
2. After observing the test fail, update `ExportServlet` and add backend-specific tests only where the existing API does not provide the required zero-timeout semantics. Do not run Maven concurrently; the repository uses `.m2_repo` and serial Maven jobs.
3. Rerun the exact regression. Then run the focused HTTP client tests, the affected Workbench test classes, and `mvnf.py tools/workbench --retain-logs`.
4. Compile generated TypeScript assets with `cd tools/workbench && bash compileTypescript.sh`; check source headers with `cd scripts && ./checkCopyrightPresent.sh`; then run the required formatter, `git diff --check`, and a final tracked/untracked status audit.
5. Exercise the existing Workbench browser harness with a repository containing more than 100 statements in default and named graphs. Verify the preview flow and the entire gzip download on desktop and mobile viewports.

Expected transport evidence is a direct request that receives both RDF statements, a Workbench request with a selected budget greater than the pause that receives both, and a short-budget request that fails only after the first statement is written to the client response. The second connection must continue to observe the configured 100 ms client timeout.

## Validation and Acceptance

The feature is complete when the real-HTTP test fails before the Workbench connection decorator and passes afterward. Positive timeout seconds must override a shorter client socket timeout on the export connection, while the shorter setting remains unchanged for another connection. Zero must bypass timeouts in both HTTP client backends. The complete Workbench test suite must pass, generated JavaScript must match TypeScript, and browser evidence must show a separate 100-row preview and a full gzip N-Quads download with named graphs.

The loopback setup proves the transport behavior under a controlled short timeout; it does not establish which proxy or client setting caused the user's exact production interruption. The response timeout is an idle-data timeout, while the Workbench callback wrapper is an elapsed-time guard. Any remaining distinction or backend limitation must be stated clearly in the handoff.

## Idempotence and Recovery

The loopback tests start and stop an ephemeral local HTTP server and shut down their HTTPRepository instances in `finally` blocks. Rerunning them creates no persistent data. If a local socket is denied, retry only through the already-authorized loopback test environment and record the precise restriction; do not replace the regression with a mock that does not exercise the HTTP parser or socket.

## Artifacts and Notes

The initial export regressions and the XSL metadata red/green summaries are recorded in `initial-evidence.txt`. Maven run logs are retained under `logs/mvnf/`, and the root quick-install output is in `maven-build.log`. The timeout reproduction and its outcomes will be appended here as it progresses.

## Interfaces and Dependencies

The intended Workbench adapter is private or package-private and operates only on `HttpClientDependent` connections. It uses the existing `RDF4JHttpClient`, `HttpRequest.copyOf(original, original.getUri())`, and `HttpRequest.Builder.responseTimeout(Duration)` APIs. The delegate owns the shared HTTP transport and must remain open when the per-connection wrapper is removed. HTTP client implementations must define `Duration.ZERO` as the explicit infinite timeout for a single request, independent of any shorter configured socket timeout.

Plan revision: updated on 2026-09-27 after the user clarified that the browser download starts and stops mid-response; replaced the earlier assumption that an RDF callback deadline alone could solve the reported timeout.
