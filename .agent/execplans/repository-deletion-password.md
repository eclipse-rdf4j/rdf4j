# Require an optional password before deleting repositories

This ExecPlan is maintained in accordance with `.agent/PLANS.md`. It is a living document: Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective must reflect the current implementation and evidence.

## Purpose / Big Picture

Administrators can start RDF4J Server or a local Workbench with `-Dorg.eclipse.rdf4j.repository.deletePassword=secret`. Deleting an entire repository then requires that exact password. Workbench discovers a remote Server's requirement and prompts without needing the same JVM property itself. Ordinary removal of statements, graphs, and SPARQL data remains unchanged. Leaving the property unset preserves existing behavior; setting it to an empty string fails application startup without printing the value.

## Progress

- [done] Establish initial build and failing regression.
- [done] Implement shared policy and HTTP transport.
- [done] Enforce server and SYSTEM deletion boundaries.
- [done] Complete Workbench guarded deletion user flow.
- [done] (2026-10-17 07:33Z) Verify modules and live browser behavior.
- [done] Complete documentation and final evidence handoff.

## Surprises & Discoveries

`HEAD /repositories/{id}` currently dispatches to the query handler, which requires a query and initializes the repository. Discovery therefore uses the existing `HEAD /protocol` endpoint, which does neither.

The legacy `SYSTEM` repository is a configuration facade, implemented by `RepositoryConfigRepository`. Removing its configuration statements can delete real repositories at commit. Protecting only HTTP DELETE would leave this alternate deletion route open.

`WorkbenchRequest.getParameter()` can fall back to saved cookies and servlet defaults. A deletion password must be read from explicit submitted parameter values, never that convenience method.

Command servlets are created lazily by Workbench routing. Reading the policy only in DeleteServlet.init would fail to reject an empty property at application startup and could create inconsistent snapshots. Recorded startup regressions now pass after moving the snapshot into load-on-startup WorkbenchGateway and its ServletContext.

The JDK factory installs a proxy-only Authenticator, which turns an unauthenticated Basic challenge into IOException before the protocol session receives a response. The final tests prove real Basic/deletion header coexistence on both transports and separately prove the protocol's 401 type using a synthetic transport response. Existing JDK authentication behavior was not changed.

The native Workbench HTML shell encodes its initial page model as URL-safe base64. Its denial regression now decodes that actual model and asserts selected ID, both repository-picker choices, password requirement, safe error, and absence of both submitted and configured secrets.

The SYSTEM transaction deletion fixture originally round-tripped configuration blank nodes through N-Triples. The writer transforms some blank-node labels, so this removed only part of the configuration and correctly raised a configuration error rather than a deletion refusal. The final fixture uses the transaction protocol's supported wildcard N-Quads representation of clear(context), exercising actual whole-repository deletion without depending on generated blank-node identities.

Live Boot launches on this macOS JDK 26 initially listened but reset all IPv4 requests. The browser worker isolated this to dual-stack networking; launching the task stacks with `-Djava.net.preferIPv4Stack=true` made their protocol endpoints respond. This runtime workaround is used only for verification and is not a production change.

Live keyboard verification caught focus loss: disabling the Delete opener during the safety request caused the browser to drop focus before the dialog captured it. After a focused Node/browser red, the opener remains focusable and the existing pending-request set prevents duplicates. Keyboard Escape/focus restoration then passed live.

Live denied-deletion verification found the error row was lost from the visible model when the repository picker prepared its windowed rows. The smallest JUnit regression expected safe `error-message` metadata but observed an empty value; its red is archived under `red-workbench-inline-error`. The completed fix publishes safe metadata while preserving the legacy error row, and preserves old-model errors across picker paging. Tests and independent live review confirm one visible safe refusal and one discovery-failure callout.

Running Boot directly from a target jar while Maven installs overwrite that jar can break later nested-class loading. Verification stacks were therefore launched from validated immutable copies in `/tmp`, with matching served generated-script SHA. All task-owned stacks have now stopped gracefully; their logs and data remain on disk.

A browser resize assertion after Back initially had an empty repository selection, for which disabling Delete was correct. The diagnostic and test readiness were corrected. An independent Node regression did prove static disabled bindings could override live controls on rerender; the shared post-render synchronization now derives the action state from live controls without copying the password into model state. Independent browser review confirmed a selected repository with a nonempty password remains enabled across actual desktop-to-mobile resize.

One intermediate full Node run hit an existing row-store-maintenance timeout while Maven and browsers were running. The focused maintenance selection passed 14 tests, and the final unchanged full Node command passed alone after other verification completed. The failed log is preserved; no row-store code or timeout was changed.

## Decision Log

Decision: The policy is immutable startup configuration, with exact nonempty matching and an explicitly empty value rejected. Rationale: existing installations remain compatible and a misconfigured protection switch does not silently disable protection. Date/Author: 2026-10-17, Codex.

Decision: Server policy is authoritative for remote Workbench connections; Workbench policy applies only to its local managers. Rationale: a standalone Workbench must not need the server's configured secret. Date/Author: 2026-10-17, user-approved contract.

Decision: Send base64 of UTF-8 password bytes in `X-RDF4J-Repository-Delete-Password`, only on the deletion request. Advertise the boolean requirement using `X-RDF4J-Repository-Delete-Password-Required` on HEAD /protocol. Rationale: Unicode and whitespace are preserved, secrets stay out of URLs and generic parameter logging, and older servers remain compatible through an absent response header. Date/Author: 2026-10-17, Codex.

Decision: SYSTEM transactions retain only the authorization decision from START. Commit first computes all resulting configurations and checks every pending deletion before applying any change. Rationale: no secret or shared mutable authorization state is retained, and unauthorized mixed operations cannot partially update configurations. Date/Author: 2026-10-17, user-approved contract.

Decision: No commits, pushes, branch changes, or unrelated cleanup are authorized. All Maven and formatting operations are serialized through the designated verification runner. Date/Author: 2026-10-17, user scope and coordination.

## Outcomes & Retrospective

Implementation and technical verification are complete. The optional property protects direct Server deletion, legacy SYSTEM configuration removal, and local Workbench deletion; remote Workbench follows Server authority. Ordinary data operations and embedded LocalRepositoryManager APIs retain their behavior. The guide and REST OpenAPI document the property, headers, typed failure, and SYSTEM START authorization.

The initial offline root quick clean install passed in 42.174 seconds. Both original existing-API regressions failed before production edits: the Server expected a ClientHTTPException but none was thrown; Workbench expected 403 but received 302. Startup/duplicate-password and inline-error regressions were also recorded before their fixes. Reports remain under `red-server`, `red-workbench`, `red-workbench-startup`, and `red-workbench-inline-error`, with compact summaries in `initial-evidence.txt`.

Full Maven verification ran 1,441 tests: 1,433 passed, eight skipped, zero failures/errors. Module totals are core/http/protocol 36, core/http/client 177 with eight skips, core/repository/manager 18, tools/server-spring 188, and tools/workbench 1,016. Selected real HTTP integration tests add five RepositoryDeletionPasswordIT tests and one LmdbInvalidIndexDeletionIT test. Copyright validation, the required formatter, OpenAPI YAML parsing, generated JavaScript/source-map hash consistency, and git diff whitespace checks passed. This is affected-module plus selected-IT validation, not the entire repository's test suite.

The final full Node command passed 757 of 757 tests, with no skips. Four traced Chromium deployment flows passed: Workbench with no property against protected Server, Workbench with a different property against protected Server, unprotected Workbench/Server, and differently configured Workbench against unprotected Server. They verify direct HTTP 403, safe inline retry state, picker/ID retention, blank secrets, successful deletion and independent repository inventory absence, duplicate/stale guards, history, keyboard/focus, and responsive layouts.

Independent parent browser review confirmed typed-ID gating, settled repeated opening/cancellation, Escape focus return, unchanged form geometry, clean mobile dialog layout, the actual visible wrong-password error, cleared input/disabled retry, and correct live-control state across desktop/mobile resize. It also independently confirmed the same differently configured Workbench shows the password field for a protected remote Server and omits it for an unprotected remote Server. No UI blocker remains.

Browser coverage is Chromium only. The macOS JDK 26 fixtures required `-Djava.net.preferIPv4Stack=true`; this is a verification-environment workaround, not a production change. Local file-manager guarding is covered by Java tests, while live deployment flows use HTTP Servers. Protocol 401 mapping has synthetic transport coverage; actual Basic/deletion header coexistence was tested on both HTTP transports.

Verification used localhost ports 18185 (unprotected), 18186 (protected), and 18187 (different Workbench property), with the public fixture password `proof-delete-🔑 ` including its trailing space. Review repository `deletion-review-protected` and all task-owned data were preserved. After final automated suites and independent review, task-owned PIDs 9895, 38088, 38416, 74835, 74838, and 76884 stopped gracefully and their ports have no listeners. Evidence remains under `logs/repository-deletion-password/frontend/`, including runtime logs, screenshots, and traces.

The checkout remains on develop with only cohesive feature source, tests, documentation, generated assets, and this plan changed. No commits, pushes, branch changes, dependency additions, or unrelated cleanup occurred. The initial feature handoff is complete. The subsequently authorized salted-verifier extension has its own current plan in `.agent/execplans/repository-deletion-password-hashing.md`.

## Context and Orientation

The checkout is `/Users/havardottestad/Documents/Programming/rdf4j-6`, on `develop`, initially clean. `core/repository/manager` supplies managers used by Server and Workbench. `core/http/protocol` defines wire constants and typed failures. `core/http/client` creates HTTP requests; `RemoteRepositoryManager` creates short-lived protocol sessions internally. `tools/server-spring` contains controllers shared by WAR and Boot deployments. `tools/workbench` contains servlet handlers, page models, and TypeScript; its checked-in JavaScript and source maps must be regenerated together. Server Spring beans are wired in `tools/server/src/main/webapp/WEB-INF/rdf4j-http-server-servlet.xml`.

`DefaultRepositoryRequestHandler.handleDeleteRepositoryRequest()` performs ordinary whole-repository deletion. `RepositoryInterceptor` deliberately skips initialization for this route so corrupt repositories remain deletable. `RepositoryConfigRepository.commit()` is the second deletion boundary for legacy SYSTEM operations. A SYSTEM transaction's connection is created when `TransactionStartController` processes START and reused by later transaction requests.

## Plan of Work

The implementation began with the mandatory root quick clean install and existing-API Server/Workbench regressions. Missing-password deletion failed the new 403/no-removal assertions, and the reports were preserved before production edits.

The completed shared implementation adds immutable `RepositoryDeletionPolicy`, typed `RepositoryDeletionDeniedException`, header constants, and stable `ErrorType.REPOSITORY_DELETION_DENIED`. Old delete/remove APIs are preserved alongside per-call overloads. Submitted credentials never enter a manager or shared header map; HEAD discovery distinguishes successful absence from transport/HTTP errors.

Server Spring wiring shares one policy snapshot. Direct deletion checks it before removal; SYSTEM facades retain only a request authorization decision and preflight the full deletion set before effects. Nested transaction denials map to HTTP 403 with safe error text. Tests cover bypass routes, malformed credentials, mixed operations, startup snapshots, and corrupt configurations.

WorkbenchGateway validates and captures startup configuration. DeleteServlet consumes that snapshot for local managers, discovers the remote Server policy, reads only explicit request password values, and returns safe retry metadata plus legacy rows. The masked field, typed confirmation, request ownership, duplicate guard, completion promises, and post-render action synchronization preserve secret-free cached models. Generated assets were refreshed under serialized build freezes.

Focused red/green cycles led into full affected modules and selected real HTTP integration tests. Four actual Chromium deployment flows and independent parent QA completed the end-to-end checks. Future changes should rerun the relevant selections below and preserve evidence before reports are replaced.

## Concrete Steps

Run commands from the checkout root, using Java 25 or newer and the local `.m2_repo` repository. The runner starts with:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Keep its complete output in `maven-build.log`. Test selections use:

    python3 .codex/skills/mvnf/scripts/mvnf.py RepositoryControllerTest#deleteRequiresConfiguredPassword --retain-logs

Keep test output under `logs/mvnf`, and preserve the initial Surefire result before later runs replace reports. Never use `-am` or `-q` for tests. Missing offline dependencies permit one exact online retry; other build failures receive the instructed serial retry. Formatting and copyright checks run only while all source editing is frozen.

The final full-module and integration commands were:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/http/protocol --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/http/client --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/repository/manager --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-spring --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --it RepositoryDeletionPasswordIT --module tools/server --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --it LmdbInvalidIndexDeletionIT --module tools/server --retain-logs

Generate assets from the root and run frontend tests from e2e:

    PATH="$PWD/e2e/node_modules/.bin:$PATH" bash tools/workbench/compileTypescript.sh
    cd e2e
    npm run test:unit

The checked-in browser spec is `e2e/tests/workbench-delete-password.spec.js`. With test deployments already running, reproduce a protected remote flow from e2e using:

    RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:18185/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:18186/rdf4j-server RDF4J_DELETION_PASSWORD='proof-delete-🔑 ' npx playwright test tests/workbench-delete-password.spec.js --project=chromium --workers=1 --trace=on

For the other verified combinations, use Workbench 18187 against Server 18186, or omit RDF4J_DELETION_PASSWORD and use Server 18185 with Workbench 18185 or 18187. These ports are historical verification settings and currently have no running task-owned servers. The executable Boot artifact is `tools/server-boot/target/rdf4j-server-boot-6.2.0-SNAPSHOT.jar`; copy it into a fresh temporary directory before starting verification so later Maven installs cannot overwrite a running jar. Give each fixture its own app-data directory and configure the existing accepted-server-prefixes setting to allow only the known test Servers.

## Validation and Acceptance

Policy tests cover unset, explicitly empty, exact whitespace, and Unicode. Controller tests cover absent, wrong, malformed, and correct request credentials; direct HTTP deletion must remain independent of repository initialization. SYSTEM tests cover direct and transaction START authorization, COMMIT-only rejection, and a mixed edit/delete operation whose unauthorized commit applies no partial effects. Ordinary configuration creation/editing remains possible without credentials.

Transport tests verify HEAD discovery for absent/true/false headers, independent Basic authentication, exact request encoding, deletion-only credentials, concurrent per-call isolation, and typed 403 failures. Workbench tests verify local/remote authority, explicit request credentials, native/page-data retry errors, no secret cookies or router state, cancellation, stale dialogs, and post-render control state. The completed live checks include settled opening/closing, keyboard/focus, repeated reversals, responsive layout, history, and enabled/disabled remote authority; their Chromium/runtime limits are stated above.

## Idempotence and Recovery

Preserve every preexisting tracked or untracked artifact. Do not reset, clean, stash, broadly stage, or overwrite unrelated files. Keep build operations serialized and use source freezes to avoid compiling a mixed edit state. If a behavioral implementation accidentally precedes a failing test, remove only that implementation patch and return to reproduction. Preserve first evidence under a unique task directory; append to an existing `initial-evidence.txt` rather than overwrite it.

## Artifacts and Notes

Evidence is preserved in `logs/repository-deletion-password/`, retained Maven reports/logs, and top-level `initial-evidence.txt`. Full-module archives are `full-core-http-protocol`, `full-core-http-client`, `full-core-repository-manager`, `full-tools-server-spring`, and `full-tools-workbench`; real HTTP archives are `it-server-green` and `it-lmdb-server`. The final Node log is `frontend/node-unit.log`; each browser flow has its own `browser-*.log`, trace zip, and labeled desktop/mobile screenshots. Final source audit and git diff whitespace checks passed. There are no remaining implementation or validation blockers.

## Interfaces and Dependencies

In `org.eclipse.rdf4j.repository.manager.RepositoryDeletionPolicy`, provide `fromSystemProperty()`, `isPasswordRequired()`, `isAuthorized(String)`, and `checkPassword(String)`. `checkPassword` throws `org.eclipse.rdf4j.http.protocol.RepositoryDeletionDeniedException`, a `RepositoryException` subtype. No new external dependency is needed.

`RDF4JProtocolSession` gains `isRepositoryDeletionPasswordRequired()` and `deleteRepository(String, String)`. `RemoteRepositoryManager` gains the equivalent discovery method and `removeRepository(String, String)`. Original signatures preserve existing unprotected operation.

Revision note, 2026-10-17: created the implementation plan from the accepted contract and recorded source ownership, serialized verification, red-first gate, and complete application boundaries.

Revision note, 2026-10-17: recorded the successful initial build and both intended Surefire assertion failures, then advanced to production implementation after releasing the source freeze.

Revision note, 2026-10-17: recorded core/server implementation, focused passing evidence, the HTTP-auth fixture correction, and the discovered lazy Workbench startup boundary requiring a second red-first refinement cycle.

Revision note, 2026-10-17: recorded the three intended startup/duplicate-password assertion failures, application startup snapshot refinement, precise JDK-versus-protocol authentication evidence boundary, and final serialized verification workflow.

Revision note, 2026-10-17: advanced to final validation, recorded native-shell and SYSTEM wire-fixture corrections, final Node evidence, and the live runtime's IPv4 workaround and retained review endpoints. Guide and OpenAPI documentation are implemented pending syntax/final review.

Revision note, 2026-10-17: recorded live focus and windowed-error discoveries, the additional metadata red-first gate, immutable runtime recovery, and completed real HTTP/core module validation. OpenAPI syntax and required copyright/format checks have passed.

Revision note, 2026-10-17 07:33Z: consolidated prior status into completed outcomes, recorded all final suite totals and independent UI sign-off, clarified the resize diagnostic and exact verification limits, and recorded graceful fixture shutdown with evidence/data preserved. Parent delivery subsequently completed; new hashing work is tracked separately.
