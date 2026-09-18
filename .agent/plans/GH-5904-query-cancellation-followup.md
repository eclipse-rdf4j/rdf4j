# Make query cancellation reliable and detect abandoned responses

This ExecPlan is a living document maintained under `.agent/PLANS.md`. It extends the implementation described in `.agent/plans/GH-5904-query-cancellation.md` and records the follow-up to PR 5981. The reviewed starting commit is `b85cd8346fc545f4f6d1eac6dc92e57f5077fa92` on `GH-5904-query-cancellation`.

## Purpose / Big Picture

Canceling a query must reach its remote owner even if closing the local HTTP connection waits for more results. A failed downstream cancellation must remain retryable, and a repository endpoint must not cancel another repository's query. Workbench must recognize completion after its result window navigates. For formats that permit it, Server and Workbench will periodically flush harmless whitespace while waiting for results; a failed write will cancel evaluation and forward cancellation to an HTTP-backed repository. Clients need not supply a tracking identifier to benefit from disconnect detection.

## Progress

- [x] (2026-09-18 19:20Z) Reproduce the four review findings and inspect the live PR head.
- [x] (2026-09-18 19:20Z) Research registered writers and primary format specifications.
- [x] (2026-09-18 19:16Z) Complete the required baseline root quick clean install.
- [x] (2026-09-18 20:14Z) Verify Workbench completion and pagination with 56 passing frontend unit tests and Chromium GET/POST navigation checks.
- [x] (2026-09-18 20:14Z) Verify the cancellation state machine and shared heartbeat: 29 focused tests, full HTTP client suite (133 tests, 8 skipped), and HTTP repository suite (13 tests), all without failures or errors.
- [x] Verify Server integration: 24 focused and 138 full-module tests, with no failures, errors, or skips; includes real socket disconnect and writer/parser format matrix.
- [x] Correct TriX's advertised settings after the format matrix exposed its omitted XML-declaration capability; regression failed before the fix, then all 50 TriX module tests passed.
- [x] Verify Workbench response probing, lazy-result handling, HEAD exclusion, and failure cleanup: 403 full-module tests, with no failures, errors, or skips; includes nine heartbeat integration tests.
- [x] Review the combined production changes and affected-module regression coverage.
- [x] Verify whitespace-prefixed, declaration-free XML in Chromium 131: production `tuple.xsl` transforms GET/POST results, emits completion notifications, and preserves total-count metadata.
- [x] Finish final copyright, formatting, and root quick build checks: all passed; the formatter made no further changes and the root quick clean install completed in 27.410 seconds.
- [x] Prepare the reviewed 55-file change for publication on the existing PR branch. Record the committed revision and remote-head verification in the final task response.

## Surprises & Discoveries

`HTTPRepositoryConnection.close()` can wait for its background parser. The real HTTP regression saw zero downstream cancellation calls until the response stream was manually released. Forwarding must precede potentially blocking close.

Server's query handler is shared by all repository URLs. A request identifier alone therefore does not identify a repository's query. Failed downstream cancellation currently disappears behind HTTP 204, and the next attempt gets 404.

A listener attached to an initially blank result window does not survive navigation. Chromium reproduced this for GET and POST. Completion must be signaled from the final result document or another navigation-independent mechanism.

XML whitespace before an XML declaration is invalid. The built-in XML writers support suppressing their optional declaration; a declaration-free UTF-8 XML document can have leading whitespace and stylesheet processing instructions. JSON and Turtle-family text also accept a leading ASCII space. NDJSON requires spaces or tabs rather than empty newline records. Boolean text cannot accumulate arbitrary whitespace because its parser reads only 16 characters. CSV, TSV, binary tuple, and spreadsheets have no generic harmless repeated prefix. BinaryRDF has encoded comment records but requires writer-owned framing, so raw prefixes do not apply.

Flushing a servlet response commits status and headers. Workbench currently writes the total-result-count cookie after collecting results. Accurate counts must also travel in result-document metadata, with client-side pagination preferring that metadata. Failures after response commitment cannot change the status and must not append an unrelated error page or a successful empty result.

## Decision Log

On 2026-09-18, retain the existing non-transactional boundary. Transaction connections own other work and cannot be closed for this protocol without defining transaction survivability. Keep existing public cancellation entry points compatible and add repository scoping at the shared Server handler.

On 2026-09-18, make cancellation state explicit: active work can complete normally or begin cancellation; one remote attempt runs at a time, with callers waiting outside lifecycle locks. Remote failure remains retryable even after the local worker finishes. Closing the local connection runs once independently, after the forwarding attempt has started, because a failed remote cancellation must not block its own retry response. Failed cancellation is retried automatically up to twenty times with 250 ms between attempts, matching the browser policy. Success or exhaustion removes the retained handle; shutdown stops retries and prevents new registrations. No network or connection-close callback executes under the state lock.

On 2026-09-18, use one shared output policy in the HTTP client module, which both Server and Workbench already depend on. The policy recognizes known result formats and actual writer capabilities, with unknown formats disabled. It serializes periodic probes with real output and stops before ordinary serialization writes. For XML, configure the actual writer to omit its optional declaration before any writes. Do not strip or rewrite serialized bytes.

On 2026-09-18, cover lazy result evaluation by fetching and retaining the first tuple/graph item and its metadata while the probe remains active, before Server hands results to the serializer. Workbench's existing materialization occurs before output and is covered by the same monitor. Leave the monitor active until actual serializer output, since some graph writers buffer the whole model. Do not insert arbitrary bytes between serializer writes, where they might split a lexical token.

On 2026-09-18, distinguish successful completion, stopping, and failure in the monitor API. For successful empty NDJSON-LD after probes, emit a legal empty JSON-LD record (`[]` followed by a newline). On failure after probes, invalidate a still-writable textual response with a NUL byte, which is illegal in all supported grammars, and preserve the original exception. This prevents whitespace-only Turtle from disguising a failed query as an empty success. Before any probe, preserve normal HTTP error handling.

On 2026-09-18, reserve cancellation before propagating a detected write failure. The monitor invokes a nonblocking signal once, outside the output lock; integrations call `coordinator.cancelAsync(handle)`, which marks that exact handle as pending synchronously and dispatches HTTP forwarding, connection close, and retries off-thread. Scheduling the entire signal later is unsafe: request cleanup could complete and remove the handle before forwarding is reserved. A normal endpoint cancellation still waits for its forwarding attempt so that HTTP 204 means acknowledgement rather than a queued attempt.

On 2026-09-18, preserve Workbench pagination through explicit result metadata. Existing cookie/query-parameter behavior remains the fallback for other pages. Negotiate content types, validate query parameters and JSONP callbacks, and set response headers before allowing a probe to flush.

On 2026-09-18, stop the monitor before inspecting whether a failed response has committed. Otherwise a scheduled probe could flush between the inspection and error handling, leaving a whitespace-only response on failure. Keep normal HTTP errors available before commitment and preserve the original exception through cleanup.

On 2026-09-18, advertise TriX's existing `INCLUDE_XML_PI` setting in `getSupportedSettings()`. Its writer already honored that setting; the missing capability declaration prevented safe opt-in. Preserve the heartbeat's actual-writer capability check and TriX's default output rather than adding a format-specific bypass.

## Outcomes & Retrospective

Implementation and affected-module verification are complete. The baseline root quick clean install passed. The first cancellation implementation passed focused selectors (9 coordinator, 16 Server, and 29 Workbench tests), but parent review found additional lifecycle races; those intermediate passes do not establish final integration acceptance. The revised core implementation passed 18 coordinator and 11 heartbeat tests; an additional actual-writer regression brought the heartbeat selector to 12 passing tests. The full HTTP client suite passed with 133 tests, zero failures/errors and 8 skips; its first sandboxed attempt could not bind MockServer ports, and the identical permitted rerun passed. The HTTP repository suite passed all 13 tests, including a real HTTP regression where forwarding returns while the background result parser remains blocked. TriX passed all 50 tests after a recorded failing capability regression. Server passed 24 focused and 138 full-module tests, including a permitted real socket-disconnect test with no skips. Workbench passed 403 full-module tests, with no failures, errors, or skips, after its socket-limited run was repeated with permission to bind local test ports. Workbench frontend tests passed 56/56. Chromium 131 confirmed completion signaling after HTML and XML GET/POST navigation; the XML fixture used production `tuple.xsl`, the browser `application/xml` content type, and whitespace-prefixed XML without a declaration. Transformed results retained the request identifier and total-count metadata. This browser check used a focused fixture rather than a deployed Workbench server. Final copyright and formatting checks passed, with no formatter changes. The final root quick clean install passed in 27.410 seconds; this validates the build but does not run the full repository test suite. The staged change contains only the 55 approved source, test, generated browser asset, documentation, and plan files.

## Context and Orientation

`core/http/client/.../CancellableOperationCoordinator.java` registers a worker, connection, and remote callback for each operation. `AsyncExplainCoordinator` is its compatibility facade. `tools/server-spring/.../handler/AbstractQueryRequestHandler.java` prepares and evaluates queries, then transfers ownership of tuple/graph results to `QueryResultView` and its three subclasses for serialization. The handle and new response monitor must remain valid across that transfer.

`tools/workbench/.../commands/QueryServlet.java` owns the request and connection; `util/QueryEvaluator.java` selects writers and collects browser results. `TupleResultBuilder` and `WorkbenchTupleResultWriter` emit browser XML and metadata. The browser code is in `tools/workbench/src/main/webapp/scripts/ts/query.ts`, `paging.ts`, and the tuple/graph/boolean XSL transformations. Generated JavaScript must be regenerated with the existing TypeScript build.

## Plan of Work

First, add failing cancellation regressions and repair forwarding order, retryable failure state, and repository-scoped lookup. Exercise both regular queries and explanations where they share the primitive. In parallel, add browser regression coverage for completion after GET/POST navigation and repair the completion protocol, including stale notifications and result-window isolation. Update outdated test fakes without weakening assertions.

Then add a shared response monitor and tests for accepted and rejected formats, repeated probes, output races, disconnect callbacks, and lifecycle shutdown. Integrate it before evaluation in Server and after writer selection in Workbench. Register an internal identifier for an otherwise untracked regular query, propagate it through `QueryRequestContext`, and reuse the cancellation coordinator on disconnect. Retain final content headers, compression behavior, and cleanup. Transfer count metadata to the result page before pagination initialization.

Finally, the parent reviewer inspects the full diff and the actual failure/success evidence. Run focused checks and affected module suites, exercise real HTTP disconnects and Chromium navigation, perform copyright/format checks, then commit and push the existing PR branch. Do not stage review scratch output or change unrelated checkouts.

## Concrete Steps

All commands run from this isolated `rdf4j` checkout. The coordinator serializes Maven builds and owns the writable `.m2_repo` cache. Start with the required root build in the background, saving full output to `maven-build.log`:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Run focused tests with explicit module and class selectors, without `-q` or `-am`. Install changed dependencies separately with tests skipped before dependent tests. If a dependency is missing offline, retry that command once online, then return to offline mode. Record exact commands and outcomes in `initial-evidence-query-cancellation-followup.txt` and the final verification notes. Use the existing Workbench package scripts for TypeScript, frontend unit tests, and browser coverage. The final plan revision will record actual selectors and totals.

## Validation and Acceptance

A cancellation reaches the remote endpoint before any blocking local close. A downstream failure is observable and another request can retry it; concurrent completion cannot silently discard pending cancellation. Two repositories can use the same request identifier independently. Completed GET and POST result windows clear only their matching control state. Result metadata overrides stale count cookies.

For supported tuple, boolean, and graph writers, multiple probes plus empty/nonempty serialized results parse to the original values. XML and NDJSON remain syntactically valid. Unsupported formats receive no injected bytes. A real client disconnect while evaluation or the first lazy result is blocked interrupts and closes the operation, including downstream forwarding. HEAD and invalid requests do not receive a body. Compression, normal completion, explicit cancellation, and exceptions do not leak a monitor or race serializer output. Error statuses remain intact before commitment; errors after commitment terminate the response without disguising failure as an empty success.

## Idempotence and Recovery

Preserve all untracked review/browser artifacts. Reuse only the dedicated checkout and build cache. Never reset, clean, or overwrite unrelated work. Recheck origin's PR head before push, and stop to reconcile concurrent changes if it moved. Failed checks remain recorded separately from later passing runs; a blocked platform or integration check is not described as green.

## Artifacts and Notes

Reproduction sources and logs are in the parent workspace's `review-evidence/backend/`. Primary format research is in `review-evidence/format-research.md`; it includes grammar links and exact writer/parser anchors. Those scratch artifacts are not automatically committed. The tracked plan and focused regression tests must carry enough explanation to maintain the implementation without them.

## Interfaces and Dependencies

Use the existing `FileFormat`, `WriterConfig`, `XMLWriterSettings`, query/result interfaces, servlet API, and JDK concurrency primitives. Add no external dependencies. The response monitor must own scheduling and cancellation of probes, expose an output stream or equivalent synchronized transition to serialization, and accept a disconnect action independent of servlet classes. Supported-format detection must be explicit and reusable between Server and Workbench.

Revision 2026-09-18: created after reproductions and format research, before production heartbeat implementation. The plan separates cancellation correctness from transport probing while recording their shared lifecycle and HTTP constraints. Refined the monitor lifecycle to preserve empty NDJSON results and make failures observable after headers have been committed.
