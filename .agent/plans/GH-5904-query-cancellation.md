# Cancellable regular queries across Workbench and HTTP Server

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be updated as implementation proceeds.

This document follows `.agent/PLANS.md` and implements the architecture recorded on 2026-08-18 in [eclipse-rdf4j/rdf4j#5904](https://github.com/eclipse-rdf4j/rdf4j/issues/5904#issuecomment-5326860004).

## Purpose / Big Picture

Today RDF4J can cancel a tracked query explanation, but an ordinary SPARQL query continues to consume server resources after a client merely abandons the HTTP response. After this change, Workbench can start a regular query with a unique request id and retain a control page with a Cancel action. A separate cancellation request with the same id will stop local evaluation or be forwarded through an `HTTPRepository` to the remote RDF4J Server that owns the actual worker. Direct RDF4J HTTP clients can use the same protocol and public repository cancellation method.

The visible proof is a long-running tuple, graph, or boolean query launched from Workbench against either a local repository or an HTTP-backed repository: the controller window shows an enabled Cancel action while the result opens in a per-query named result window; clicking Cancel sends a cancellation request, interrupts the active worker, closes its repository connection, and prevents a cancellation-induced error page from replacing useful UI. If a replicated Workbench or Server instance does not own the request id it returns HTTP 404, and the Workbench browser retries a failed regular-query or explanation cancellation up to twenty times so round-robin routing can reach the owning replica. Every query and explanation gets a fresh UUID, and the UUID is part of the result-window name so queries started from multiple browser tabs do not share or overwrite one result target.

This implementation intentionally covers ordinary non-transactional repository query endpoints. Transaction URLs continue to reject tracked query ids and cancellation, matching the existing tracked-explanation boundary, because closing the transaction-owned connection would silently invalidate unrelated work in that transaction.

## Progress

- [x] (2026-08-18) Inspect existing explanation cancellation end to end.
- [x] (2026-08-18) Publish intended architecture on GitHub issue 5904.
- [x] (2026-08-18) Write this self-contained ExecPlan before production edits.
- [x] (2026-08-18) Add shared coordinator and request-context tests.
- [x] (2026-08-18) Implement generic cancellation primitives and compatibility wrapper.
- [x] (2026-08-18) Add protocol request-id and cancellation tests.
- [x] (2026-08-18) Add HTTP query propagation and cancellation tests.
- [x] (2026-08-18) Implement HTTPRepository query tracking and cancel API.
- [x] (2026-08-18) Add Server endpoint and lifecycle tests.
- [x] (2026-08-18) Implement Server registration, rendering cleanup, and forwarding.
- [x] (2026-08-18) Add Workbench servlet and browser contract tests.
- [x] (2026-08-18) Implement Workbench tracked execution and Cancel controls.
- [x] (2026-08-18) Run focused tests, affected modules, formatting, and final audit.
- [x] (2026-08-18) Reopen plan for replicated cancellation and tab isolation.
- [x] (2026-08-18) Add failing 404, retry, and UUID-isolation tests.
- [x] (2026-08-18) Implement backend not-found cancellation responses.
- [x] (2026-08-18) Implement bounded frontend retry and per-query windows.
- [x] (2026-08-18) Re-run focused and affected-module verification.

## Surprises & Discoveries

- Observation: Server tuple and graph queries are only partially evaluated in `AbstractQueryRequestHandler`; result iteration continues later in `QueryResultView.render`.
  Evidence: `AbstractQueryRequestHandler.handleQueryRequest` returns a `ModelAndView` containing the open result and connection, and `QueryResultView.render` closes the connection only after serialization.
  Consequence: a regular-query cancellation handle must remain registered through view rendering, reattach to the rendering thread, and complete only in the view's final cleanup. Completing it when the controller returns would create an uncancellable streaming window.

- Observation: Workbench regular execution navigates the current page with `document.location.href` or a normal form POST.
  Evidence: `tools/workbench/src/main/webapp/scripts/ts/query.ts` implements `doSubmit` as a full-page navigation, while explanation requests use AJAX and keep cancellation controls alive.
  Consequence: cancellable regular execution must keep a controller document alive. The implementation will submit the existing GET or POST query to a named secondary result window and keep the editor window responsible for Cancel. This reuses the existing XML/XSL result rendering without introducing a second client-side result renderer.

- Observation: `SPARQLProtocolSession` background tuple and graph result APIs can outlive the call that creates the HTTP response stream.
  Evidence: `sendTupleQuery` and `sendGraphQuery` return background-backed result objects for the result-returning overloads.
  Consequence: propagation is based on a request context and server-side id registry, not on aborting one client HTTP request. The exact active `RDF4JProtocolSession` is registered while a query call is being opened or synchronously consumed; `HTTPRepository.cancelQuery` also has a fresh-session fallback so cancellation remains available while background consumption continues.

- Observation: the connected GitHub application could read issue 5904 but lacked comment permission, and the sandboxed CLI initially could not reach GitHub.
  Evidence: the application returned HTTP 403; `gh issue comment` succeeded once run with approved network access.
  Consequence: no code impact. The canonical architecture comment is `issuecomment-5326860004`.

- Observation: closing a local or remote connection during cancellation does not guarantee a `QueryInterruptedException`; a backend may instead surface `RepositoryException` or an I/O failure.
  Evidence: the focused Workbench cancellation test reproduced a cancellation-induced `RepositoryException` after the worker was interrupted.
  Consequence: cancellation suppression is keyed to the coordinator handle's inactive state and covers RDF4J and I/O failures, while uncancelled failures retain their existing behavior.

- Observation: the explanation UI updates every element with the `query-explain-cancel` class as one group.
  Evidence: `hideExplainCancelButtons` and related rendering helpers select `.query-explain-cancel` globally.
  Consequence: the regular-query Cancel control uses its own `query-cancel` class so explanation state transitions cannot hide an active regular-query control.

- Observation: browser window names are shared across tabs in the same browsing context group even though each tab has independent JavaScript variables.
  Evidence: the first implementation used the constant `rdf4j-query-result` as the target for every query.
  Consequence: each regular query now generates its UUID before opening a result window and appends that UUID to the result-window name. GET and long-query POST execution use the same unique target.

## Decision Log

- Decision: Introduce a generic `CancellableOperationCoordinator` and keep `AsyncExplainCoordinator` as a compatibility facade.
  Rationale: regular queries and explanations need identical race-safe registration, interruption, connection close, downstream callback, completion, and shutdown behavior. Renaming or changing the existing public explanation class and nested handle would create avoidable source or binary compatibility risk.
  Date/Author: 2026-08-18 / Codex.

- Decision: Use `query-request-id` and `cancel-query=true` on the existing repository query endpoint.
  Rationale: this mirrors the deployed explanation protocol, works with existing repository authorization and routing, and can be forwarded through HTTP-backed repositories without inventing a separate controller hierarchy. Existing explanation parameters remain unchanged.
  Date/Author: 2026-08-18 / Codex.

- Decision: Return HTTP 404 when a cancellation id is unknown or already completed.
  Rationale: in a replicated deployment, 404 distinguishes the replica that does not own the query from the replica that accepted cancellation. Workbench can therefore retry failed cancellation requests through round-robin routing. A cancellation that reaches the owner still returns HTTP 204.
  Date/Author: 2026-08-18 / Codex.

- Decision: Reject blank ids and duplicate active ids with HTTP 400.
  Rationale: blank ids cannot identify work and duplicate live ids would allow one client to cancel unrelated work. This also matches the explanation coordinator semantics.
  Date/Author: 2026-08-18 / Codex.

- Decision: Keep transactional query cancellation out of this change and explicitly reject tracking parameters on transaction URLs.
  Rationale: transaction execution owns a long-lived connection and serialized executor. Closing that connection to cancel one query can invalidate the transaction, so it requires a separate contract for rollback, transaction survivability, and concurrent cancel dispatch.
  Date/Author: 2026-08-18 / Codex.

- Decision: Workbench executes tracked regular queries in a named secondary result window while the originating query page remains the controller.
  Rationale: current result rendering is server-side XML/XSL and full-page. A secondary same-origin window preserves existing GET, long-query POST, content negotiation, downloads, history, and result templates while leaving a reliable page from which to issue the independent cancel POST.
  Date/Author: 2026-08-18 / Codex.

- Decision: Retry a failed browser cancellation at most twenty times after the initial request, without retrying successful 2xx responses.
  Rationale: jQuery reports HTTP 404, other non-2xx responses, and network failures through the same failure callback. A bounded sequence of twenty retries gives round-robin routing repeated opportunities to reach the owner without creating an unbounded request loop.
  Date/Author: 2026-08-18 / Codex.

- Decision: Generate one fresh UUID before every query/explanation request and derive each regular-query result-window name from that UUID.
  Rationale: browser tabs have independent JavaScript state but named windows are shared within a browsing context group. A fixed name can make tabs target the same result window; a UUID-derived name isolates every query and every tab without shared storage or coordination.
  Date/Author: 2026-08-18 / Codex.

## Outcomes & Retrospective

The architecture landed across all intended layers. A generic coordinator and nestable request context now back both the existing explanation facade and regular queries. The RDF4J HTTP protocol carries `query-request-id` on non-transactional queries and accepts a `cancel-query=true` POST. `HTTPRepository` exposes `cancelQuery`, and tuple, graph, and boolean queries register their active protocol session while the request is issued. Server tracks evaluation through lazy tuple/graph rendering, forwards cancellation through HTTP-backed repositories, closes connections, suppresses only cancellation-induced response errors, and shuts registries down with its controller. Transaction endpoints explicitly reject this contract. Workbench retains the editor as a controller, opens normal results in a UUID-specific named window for both GET and long POST, and exposes Cancel until that result completes or is canceled.

Focused tests captured the expected pre-change failures. The replication-support extension changed both Workbench and Server so an unknown regular-query or explanation id returns HTTP 404, while cancellation of a known active id returns 204. Workbench now shares one failure-driven cancellation helper across regular queries and explanations; it makes the initial request and permits at most twenty retries. Both request types use a fresh UUID, and every regular-query result-window name contains that UUID, isolating simultaneous queries from separate tabs.

Final module verification passed with 110 tests in `core/http/client` (8 skipped), 12 in `core/repository/http`, 113 in `tools/server-spring`, and 389 in `tools/workbench`, all with zero failures and errors. Copyright checks, TypeScript generation, formatting, `git diff --check`, and the final root quick clean install also passed.

The deliberate scope boundary remains transactional query cancellation; safely canceling one operation without invalidating its transaction needs a separate transaction-survivability contract. Registries are JVM-local; the reopened milestone adds bounded browser retries for round-robin deployments rather than a distributed registry. A live manual browser/server exercise was not added to the first implementation run; the Workbench secondary-window behavior is covered as a browser source contract plus servlet concurrency tests, while the HTTP wire shape is exercised against MockServer.

## Context and Orientation

The shared HTTP client module is `core/http/client`. `AsyncExplainCoordinator` currently stores active explanation handles in a `ConcurrentHashMap`; cancelling a handle marks it inactive, interrupts its attached thread, closes its attached `RepositoryConnection`, and optionally invokes a remote cancel callback. `QueryExplanationRequestContext` carries an explanation id through code that cannot change the public Query API. `RDF4JProtocolSession` adds that id to a non-transactional explanation request and can send a separate cancellation POST.

The RDF4J HTTP repository implementation is `core/repository/http`. `HTTPRepository` owns the protocol-session factory and the active explanation session map. `HTTPTupleQuery`, `HTTPGraphQuery`, and `HTTPBooleanQuery` call `RDF4JProtocolSession` methods. This layer must expose `cancelQuery(String)` and propagate the new regular-query context across all three query types and both result-returning and handler-based evaluation forms.

The Server implementation is `tools/server-spring`. `AbstractRepositoryController` dispatches cancellation before parsing a normal query. `AbstractQueryRequestHandler` opens a repository connection, prepares/evaluates a query, and returns a `ModelAndView`. Boolean results finish during controller evaluation, but tuple and graph iteration continues in `QueryResultView`. `QueryResultView` already owns the final connection and circuit-breaker cleanup, so it is the correct owner for completing the cancellation handle after streaming.

The Workbench implementation is `tools/workbench`. `QueryServlet` fully evaluates and serializes a normal query while its repository connection is open. The TypeScript source `src/main/webapp/scripts/ts/query.ts` builds normal GET URLs and falls back to a form POST for long URLs; `src/main/webapp/transformations/query.xsl` supplies Execute and explanation controls. Generated `src/main/webapp/scripts/query.js` is produced by the module build and must remain consistent with the TypeScript source according to the existing frontend build.

`QueryCircuitBreaker` is separate from explicit user cancellation. Both systems attach to the same worker and connection. A tracked regular query must remain registered with both until serialization completes. Circuit-breaker timeout continues to produce 503 and `Retry-After`; explicit cancellation suppresses cancellation-induced error output because the controlling request already received 204.

## Plan of Work

Milestone 1 establishes reusable primitives and preserves explanation behavior. Add the smallest failing tests in `core/http/client` for a generic coordinator: cancellation before attachment, cancellation during execution, duplicate ids, completion/cancel races, downstream callback, connection close, and shutdown. Add a nestable `QueryRequestContext` test. Implement `CancellableOperationCoordinator` with neutral operation/request naming, then refactor `AsyncExplainCoordinator` into a thin facade whose public methods and nested `Handle` retain their current signatures and behavior. Add `QUERY_REQUEST_ID_PARAM_NAME` and `CANCEL_QUERY_PARAM_NAME` to `Protocol`.

Milestone 2 carries identity through HTTP repositories. Extend `RDF4JProtocolSessionTest` first so ordinary non-transactional tuple, graph, and boolean requests are expected to include `query-request-id`, transaction requests are expected not to include it, and `cancelQuery` is expected to POST `cancel-query=true` plus the normalized id to the repository query URL with normal headers. Add HTTP repository tests for validation, use of an active session, fallback session creation, and registration cleanup. Implement `QueryRequestContext`; teach `RDF4JProtocolSession.getQueryMethod` and `cancelQuery`; add the public `HTTPRepository.cancelQuery(String)` API and an active regular-query session map; and wrap every evaluation overload in `HTTPTupleQuery`, `HTTPGraphQuery`, and `HTTPBooleanQuery` so the current id is registered for the synchronous portion of execution.

Milestone 3 makes Server queries cancellable for the complete result lifetime. Add failing `DefaultQueryRequestHandlerTest`, `RepositoryControllerTest`, and `QueryResultView` tests for parameter validation, active-owner cancellation, duplicate registration, worker interruption, connection close, canceled-response suppression, tuple/graph streaming attachment, boolean completion, remote `HTTPRepository` forwarding, shutdown, and non-interference with untracked requests. Implement `QueryRequestHandler.handleCancelQuery` and dispatch it before explanation cancellation and query parsing. In `AbstractQueryRequestHandler`, register tracked non-explanation queries, activate `QueryRequestContext` during evaluation, and put the live handle plus coordinator into the model. Extend `QueryResultView` so it reattaches the rendering thread, skips rendering if cancellation won the race, and completes the handle in the same final cleanup that closes the connection and completes the circuit breaker. On controller exceptions, complete the handle locally and suppress only errors caused after explicit cancellation. Add handler shutdown and invoke it from the controller or owning singleton lifecycle used for explanation cleanup.

Milestone 4 exposes the path in Workbench. Add failing `QueryServletTest` and `QueryTemplateTest` coverage for blank/duplicate ids, known and unknown cancellation ids, local worker interruption and connection close, remote forwarding, canceled-output suppression, UUID inclusion for GET and POST, secondary-window targeting, and Cancel state transitions. Add a regular-query coordinator to `QueryServlet`; when `query-request-id` is present, register before `QueryEvaluator.extractQueryAndEvaluate`, activate `QueryRequestContext`, attach the worker and connection, and complete in `finally`. Add `action=cancel-query` handling before normal work. Extend `query.ts` so Execute generates a UUID with the existing strong generator, opens a named result window synchronously, includes `query-request-id` in both constructed GET URLs and long-query POST forms, exposes a dedicated regular-query Cancel button, and posts `action=cancel-query` with the id. Preserve Save and Explain behavior. Update `query.xsl`, styles, localization if needed, and generated JavaScript using the existing frontend build.

Milestone 5 verifies the whole slice. Run the smallest focused tests after each implementation increment, followed by affected module suites without Maven `-am` or test `-q`. Run the repository formatter/resource step, the copyright checker, and a final root quick install. Inspect `git diff --check`, tracked/untracked status, and the final diff for accidental generated or user-owned changes. Manually exercise a local repository and an HTTP-backed repository if the server/workbench boot artifact is available: start a deliberately long query, cancel from Workbench, observe 204 on the control request and early worker/connection termination, then repeat through an HTTP proxy repository.

Milestone 6 makes cancellation tolerant of replicated Workbench and Server deployments. First change focused Workbench and Server tests so unknown regular-query and explanation ids require HTTP 404, while known active ids continue to require HTTP 204. Add a Workbench browser-source contract that requires one initial cancellation request plus no more than twenty retries for every jQuery failure, shared by regular and explanation cancellation. Require both request paths to generate a fresh UUID and require the regular-query result-window name to include that UUID rather than using a fixed cross-tab name. Then implement those semantics in `QueryServlet`, `AbstractQueryRequestHandler`, and `query.ts`, regenerate `query.js` and its source map, and repeat focused and module verification.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-temp`.

1. Maintain the living plan before each milestone transition. Keep exactly one item `in_progress` in the task plan and update this file's `Progress`, `Surprises & Discoveries`, and `Decision Log` when reality differs from the plan.

2. Before the first production edit in each behavior slice, add and run the smallest focused failing test with the repository runner, for example:

       python3 .codex/skills/mvnf/scripts/mvnf.py CancellableOperationCoordinatorTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py RDF4JProtocolSessionTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py DefaultQueryRequestHandlerTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py QueryServletTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py QueryTemplateTest --retain-logs

   `mvnf` performs the required root quick install. Do not add `-am` or `-q` to a test run. Preserve the failing report before implementing the matching behavior if later runs would overwrite it.

3. Add new production classes with the exact current RDF4J source header and `// Some portions generated by Codex`, then immediately run:

       cd scripts && ./checkCopyrightPresent.sh

4. After focused greens, run affected modules with retained logs:

       python3 .codex/skills/mvnf/scripts/mvnf.py core/http/client --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py core/repository/http --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-spring --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs

5. Format and perform the final build/audit:

       mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
       mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
       git diff --check
       git status --short

## Validation and Acceptance

The coordinator tests must show that at most one live handle owns an id, cancel is idempotent, cancel-before-attach cannot start work, cancel-during-execution interrupts and closes exactly once, completion removes the mapping without invoking downstream cancellation, and shutdown cancels every active operation. Existing explanation tests must remain green through the compatibility facade.

Protocol tests must observe `query-request-id` on ordinary non-transactional tuple, graph, and boolean query requests made inside `QueryRequestContext`, must not observe it outside the context or on a transaction URL, and must observe a cancellation POST containing `cancel-query=true` and the normalized id. HTTPRepository tests must demonstrate active-session and fallback-session cancellation and cleanup for success and failure.

Server tests must demonstrate that a tracked local query can be canceled while evaluating and while tuple/graph results are being serialized; cancellation interrupts the active worker, closes the connection, completes both registries, and does not emit a misleading 500/503 response after cancellation. Unknown regular-query and explanation ids return 404, known active ids return 204, blank ids and duplicate live ids return 400, normal queries are unchanged, and a tracked query through `HTTPRepository` invokes the matching cancellation method with the same id. Tuple, graph, and boolean paths are all covered.

Workbench tests must demonstrate that tracked normal execution registers and cleans up the same way for local and HTTP repositories, that both cancellation actions return 404 for unknown ids, and that the browser contract retains a controlling page, opens a UUID-specific result target for GET and long POST, includes a fresh UUID, exposes Cancel only while active, and sends cancellation before abandoning the result window/request. Both regular-query and explanation cancellation use the same bounded retry helper, which retries every failed response at most twenty times and stops after success. Save, Explain, compare, pagination, downloads, and query URL-length behavior remain covered by their existing tests.

Manual acceptance, when runnable, is: execute a deliberately long query from Workbench; verify a separate result window begins loading while the query page shows Cancel; click Cancel; observe wrong replicas return 404 until an owning replica returns 204 and the result stops early; repeat once with a local repository and once with an `HTTPRepository` target. Start queries from two browser tabs and verify their request UUIDs and result-window names differ and neither tab replaces the other tab's result window. A completed query followed by Cancel returns 404 and affects no later query.

## Idempotence and Recovery

All test and build commands are safe to rerun. Cancellation of a known active handle remains idempotent internally, while the HTTP endpoint returns 404 after that handle has been removed. Coordinator completion uses conditional map removal, so late completion cannot remove a newer handle that reused the same id after the earlier operation ended. Frontend retries are bounded and only continue from the failure callback.

If a test run fails because an artifact is missing offline, rerun the exact install once without `-o`, then return offline. If a module test exposes a lifecycle window not covered here, update `Surprises & Discoveries` and the relevant milestone before changing production code.

Do not use destructive Git commands or delete untracked artifacts. If generated `query.js` diverges from `query.ts`, use the existing Workbench frontend build rather than hand-maintaining two implementations. If cancellation wins after response bytes were committed, the server can only stop further work and close resources; tests should assert no new error body is intentionally written, not that already-sent bytes can be recalled.

## Artifacts and Notes

Canonical issue: https://github.com/eclipse-rdf4j/rdf4j/issues/5904

Architecture comment: https://github.com/eclipse-rdf4j/rdf4j/issues/5904#issuecomment-5326860004

The initial root quick clean install for this task completed successfully before this plan was written; its output is in `maven-build.log`. Existing untracked workspace artifacts belong to the user and must remain untouched.

## Interfaces and Dependencies

Add these protocol constants in `org.eclipse.rdf4j.http.protocol.Protocol`:

    public static final String QUERY_REQUEST_ID_PARAM_NAME = "query-request-id";
    public static final String CANCEL_QUERY_PARAM_NAME = "cancel-query";

Add `org.eclipse.rdf4j.http.client.CancellableOperationCoordinator` with registration, cancellation, completion, execution/attachment, shutdown, an active-state handle, and optional cancellation scope support. `AsyncExplainCoordinator` remains public and delegates to it without changing its existing public surface.

Add a nestable `org.eclipse.rdf4j.http.client.QueryRequestContext` whose activation exposes the current regular-query request id only for the current thread and restores the previous value on close.

Add to `RDF4JProtocolSession`:

    public void cancelQuery(String queryRequestId)

Add to `HTTPRepository`:

    public void cancelQuery(String queryRequestId) throws RepositoryException

Add to `QueryRequestHandler`:

    boolean handleCancelQuery(HttpServletRequest request, HttpServletResponse response) throws Exception;

Extend the `QueryResultView` model contract with the generic cancellation handle/coordinator needed to keep a tracked query active until serialization cleanup. No new third-party dependencies are required.

Revision note (2026-08-18): Initial plan written after tracing explanation cancellation, HTTP propagation, Server lazy rendering, Workbench navigation, and posting the agreed architecture to issue 5904. No production code had been edited.

Revision note (2026-08-18): Implementation completed with all milestones and affected-module verification green. The final design retains the stated transaction and JVM-local registry boundaries.

Revision note (2026-08-18): Reopened the plan at the user's request to add HTTP 404 ownership signaling, twenty browser retries after cancellation errors, fresh UUIDs for every query/explanation, and UUID-derived result-window names for multiple-tab isolation.
