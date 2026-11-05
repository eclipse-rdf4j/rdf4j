# Rework Workbench Explore around concurrent SPARQL queries

This is a living ExecPlan maintained according to `.agent/PLANS.md`. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current. The user approved implementation on 2026-11-03. Work in `/Users/havardottestad/Documents/Programming/rdf4j-merge`; preserve the current branch, unrelated changes, and untracked artifacts. Do not commit or push without a separate request.

## Purpose / Big Picture

Explore must retrieve resource statements and counts exclusively with SPARQL for every repository. Users should see independent Outgoing, Incoming, Used as predicate, and Graph contents sections, plus independently loaded resource summaries. Slow counts must not delay rows. Each pageable listing initially caches at most 10,000 results in the browser. Previous/Next reads that cache, and an explicit Load more reruns only that listing with LIMIT 1000000. Each listing retains at most 1,000,000 results. Stored blank nodes supplied by RDF4J Server must retain their certified identity across Workbench pages, result stores, and navigation.

## Progress

- [x] (2026-11-03 10:26Z) Inspect checkout and establish worker ownership; tracked tree initially clean, Java 27 available.
- [x] Establish production dependency build and initial backend/frontend failing regression evidence; unrelated untracked LMDB tests prevent the unchanged root test compilation.
- [x] Implement independent SPARQL sections and cancellation.
- [x] Implement browser caches, pagination, and expansion foundations.
- [x] Integrate stable blank node transport and identity.
- [x] Repair review findings and verify affected suites.
- [x] Repair completion cancellation and verify package.
- [x] Record final evidence and complete handoff.

## Surprises & Discoveries

The old ExploreServlet uses four exportStatements scans and materializes page rows. TupleServlet also holds a metadata connection while its delegate runs. Neither behavior should remain for a section request.

RDF4J's unqualified triple pattern reads all contexts, whereas its GRAPH rdf4j:nil extension selects the physical default context. Generic SPARQL endpoints, server-managed SPARQL proxies, and mixed federations do not necessarily support that extension. A named graph can itself use a reserved nil IRI; enumerate GRAPH ?context and filter its identity for Graph contents instead of binding the active graph selector to the alias.

Federation StatementSourcePattern unions member rows without guaranteeing unique quads. Listing and count queries therefore need matching DISTINCT semantics. Distinct values occurring in default and named graphs cannot be counted by adding separate distinct counts.

RDF4J already implements opt-in binary X-RDF4J-BNode-Origin transport. Stored nodes have certified repository identity; generated/forwarded nodes can remain response-local. Existing HTTPRepository opt-in only handles direct connections, and classic page-model ingestion does not scope uncertified nodes. Clear and Remove have graph-choice connections that miss origin configuration.

Existing RowStore memory limits are per store. The twelve independent lists and expansion staging need a page-wide budget. app.loadModel may resolve with a model.error and partial rows; successful promise resolution alone must not commit expansion.

The initial exact root quick clean install failed compiling pre-existing untracked LMDB Pr5951 review tests because org.eclipse.rdf4j.sail.backup is absent. Preserve these files. The required retry without -T 1C is delegated to Luna; if it reproduces, install production dependencies with -Dmaven.test.skip=true only for the install phase, then run affected-module tests with test execution enabled. Use manual targeted verification if mvnf cannot isolate the install-only flag. Evidence is in maven-build.initial-origins-failure.log.

The existing page-model reader rejects progress records while the reusable Workbench heartbeat writes them. Accept validated keepalive progress events in the page reader, including before a count's metadata arrives. Forced heartbeat tests must distinguish that real integration gap from the writer's valid deferred empty-variable count metadata behavior.

## Decision Log

2026-11-03, user: caps apply per listing, not across the whole page. Namespace/configuration metadata API reads remain allowed; all statement and count reads use SPARQL.

2026-11-03, user: roles overlap naturally. A self-link belongs in every matching role. Expansion requires an explicit Load more button. Summary fields and class/property lists use dedicated SPARQL queries.

2026-11-03, user: request stable blank nodes from RDF4J Server and preserve them throughout Workbench. If initial results contain uncertified blank nodes, expansion replaces that listing with a fresh result snapshot and resets its page, rather than guessing identity or disabling expansion.

2026-11-03, implementation contract: use four listing/scalar request slots and four count slots per page; server admission must queue fairly within its deadline rather than declaring immediate timeout at saturation. Count values are decimal strings. A successful expansion is terminal; it cannot trigger another larger request.

2026-11-03, correctness: use SELECT DISTINCT over complete quad projections and matching COUNT(DISTINCT *) for statement sections, including federations. Value lists count DISTINCT ?value. Only the at-most-10,000 initial identities require membership testing during the single expansion; no million-key main-thread set is needed.

2026-11-03, cancellation review: acknowledge a valid Explore cleanup request with HTTP 204 when its remote cancellation receives a typed query-not-found response, while retaining the coordinator's failed-cancellation state and automatic retries. A remote 404 cannot distinguish a completed query from one not registered yet. Do not mark that remote cancellation successful or add a separate reservation protocol. Preserve the existing bounded retry policy (21 attempts with 250 ms between retries); cancellation after an arbitrarily delayed upstream registration cannot be guaranteed by the current protocol.

## Outcomes & Retrospective

The implementation and acceptance checks are complete. Explore now obtains resource rows, counts, labels, and comments through independent SPARQL requests. Each listing has its own cache, pagination, expansion, count, and failure state. The browser retains at most 10,000 initial results or 1,000,000 expanded results per listing, shares the estimated 256 MiB storage budget, spills through IndexedDB, and commits expansion only after successful stream completion. Namespace/configuration lookups remain allowed metadata operations. Certified stored blank-node identities survive the classic page model, worker storage, Query, Explore, Graph, and graph-choice reads; uncertified nodes remain response-local.

The final Java suites passed: Workbench 1,535 tests with no failures/errors/skips, HTTP client 197 tests with no failures/errors and eight existing skips, and repository-http 24 tests with no failures/errors/skips. The HTTP skips concern existing Apache-only retry/redirect assumptions and two Sesame tests marked for migration. The real BNodeOriginHttpTest passed all three Memory/Native/LMDB cases, and the native Workbench query bridge passed both store cases. Classic frontend tests passed 977/977 and Graph Explorer tests passed 667/667; the complete frontend build passed. Full Java logs include /private/tmp/explore-cancel-registration-workbench-full.log, /private/tmp/explore-cancel-http-client-full.log, and /private/tmp/explore-cancel-repository-http-full.log. Frontend logs include /private/tmp/explore-qa/owned-contract-suffix-classic.log and logs/explore-fixture-fullfrontend-explorer-1.log.

Test-first evidence is preserved in initial-evidence.backend.txt, initial-evidence.frontend.txt, initial-evidence.origins.txt, initial-evidence.preferences.txt, initial-evidence.validation.txt, initial-evidence.deadline.txt, initial-evidence.disconnect.txt, initial-evidence.cancel-completion.txt, and initial-evidence.cancel-registration.txt, with root initial-evidence.txt aggregating the milestones. The initial statement-count regression failed before backend production edits. Clear/Remove graph-choice transport, empty selector validation, blocked acquisition, heartbeat disconnect, retained window budget, destructive unload, and completion/registration cancellation repairs each have corresponding failing and passing evidence. The last regression, cancellationBeforeRemoteQueryRegistrationStillCancelsTheLaterQuery, failed with one test/one failure before the final repair; the matching focused selection subsequently passed four tests, and all 18 concurrency cases passed in the full suite.

Independent Sol implementation and follow-up reviews are clear. They found and drove repairs for deadline ownership during blocked configuration/connection acquisition, heartbeat/publication lock inversion, page-window budget ownership, combined worker/memory window cleanup after dispatch, and cancellation retries during delayed remote registration. The final cancellation behavior acknowledges valid cleanup separately from remote completion, preserving automatic retry state after the local response has finished. Existing upstream protocol limits remain: cancellation retries make at most 21 attempts with 250 ms between failures, plus HTTP duration. A remote 404 does not prove completion, and registration after that retry budget cannot be guaranteed cancelled. No new reservation protocol or arbitrary retry policy was introduced.

The final offline frontend-enabled production install succeeded across all 125 reactor modules. It used the documented install-only -Dmaven.test.skip=true workaround because preserved pre-existing untracked LMDB Pr5951 review tests import the absent org.eclipse.rdf4j.sail.backup package; affected-module tests themselves ran with test execution enabled. No full root test-suite pass is claimed. The initial unchanged root clean install and exact serial retry both reproduced that unrelated blocker. No new dependencies were introduced. Implementation validation preceded the user’s separate request to branch, commit, push, and open a draft pull request.

Final package provenance is recorded in /private/tmp/explore-final-package-cancel-fixed-evidence.json. All 22 changed/new production Java/TypeScript source hashes stayed unchanged across the final build. Five frontend asset hashes match target/frontend, tools/workbench/target/rdf4j-workbench.war, and the boot jar's actual exploded Workbench entries under BOOT-INF/classes/rdf4j/workbench-webapp/scripts/. The boot jar contains no literal nested WAR. Its SHA-256 is 8a9e6254695838deb050b562f46a9e4be82234a5a21d43bfc2fce20857fbcbed; the WAR SHA-256 is 3c579e7c140d91c809894be31c6372e5f3635b54d6ab579f88c94b4b54bf057a. The earlier failed-browser package evidence remains byte-identical at /private/tmp/explore-final-package-evidence.previous-20261103.json.

All six Chromium browser acceptance scenarios passed in 18.0 seconds against that exact final boot jar. The run verified canonical resolved resources for 29 independent initial section requests, HTTP concurrency maxima of four listing/scalar and four count requests, cached pagination without requests, explicit expansion and the final five rows of a 10,005-row listing, all overlapping self-link roles on desktop/mobile, Query-to-Explore-to-Graph certified blank-node identity, invalid/empty-resource recovery, and navigation cancellation. No unexpected page, console, request, or HTTP errors remained. The command, screenshots, network summary, and six traces are retained in /private/tmp/explore-qa/final-cancel-fixed/playwright.log, /private/tmp/explore-qa/final-cancel-fixed/browser-evidence.json, and /private/tmp/explore-qa/final-cancel-fixed-results/. Browser plugin support was unavailable, so the repository Playwright runner was used; Chromium required approved unsandboxed execution after the sandbox Mach bootstrap restriction. Root also inspected the desktop final-page and mobile self-link screenshots.

The final copyright check, scoped Java formatting, and git diff --check passed. All unrelated and untracked files remain preserved. Root owned the final isolated runtime, using data directory /private/tmp/rdf4j-explore-runtime.sFP6RV and log server-cancel-fixed.log on port 18080, and gracefully stopped PID 47871/session 21569 after browser acceptance. Browser fixtures were removed by their own tests. Separate queries remain independent live reads, so concurrent repository changes can temporarily make counts differ from cached listings.

## Context and Orientation

Backend entry points are `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/ExploreServlet.java`, `base/TupleServlet.java`, and `proxy/WorkbenchServlet.java`. Existing cancellation and response ownership live in QueryServlet and CancellableOperationCoordinator, with QueryRequestContext and QueryResponseHeartbeat providing request tracking and disconnect handling.

Classic browser entry points are `tools/workbench/src/main/frontend/classic/explore.ts`, `workbenchViews.ts`, and `workbenchApp.ts`. `queryStream.ts` and `queryStreamWorker.ts` provide incremental NDJSON reading and RowStore storage. A RowStore supports append/read/count/truncate/dispose and can spill rows to IndexedDB, the browser's disk-backed database. Result tables already read bounded windows.

Origin transport is in `core/repository/http`, `core/http/client`, server query result views, and Workbench TupleResultBuilder/QueryEvaluator. The Graph explorer already distinguishes certified references from result-local scoped nodes; preserve that distinction when sharing typed identity behavior with classic pages.

Root owns this plan, integration review, evidence aggregation, and final verification decisions. Terra backend worker owns Explore Java and tests plus operation routing, and coordinates serialized Maven work. Terra frontend worker owns classic Explore rendering/controller/storage/identity and frontend tests. Luna worker owns the initial root build and Java HTTP origin plumbing plus Clear/Remove graph-choice fixes. Agree ownership before touching a shared file, especially BrowseList and TupleResultBuilder. Review workers use Sol/Astra. No worker may change branches or commit/push.

## Interfaces and Dependencies

Main GET `explore?resource=...` returns resource/page metadata, namespaces, and the existing info link, without resource row/count queries. Section requests use the same route and NDJSON page protocol:

    GET explore?resource=...&section=outgoing&kind=rows&load=initial&query-request-id=...
    GET explore?resource=...&section=outgoing&kind=count&scope=default&query-request-id=...
    POST explore  action=cancel-query&query-request-id=...

Section names are `outgoing`, `incoming`, `predicate`, `graph`, `types`, `superClasses`, `subClasses`, `properties`, `superProperties`, `subProperties`, `domain`, `range`, `label`, and `comment`. `kind` is rows or count. Listing `load` is initial or more and selects exactly 10000 or 1000000; labels/comments are rows-only scalar queries with LIMIT 1. Listing omission of load defaults to initial. Outgoing/incoming/predicate counts require scope default or named; graph has one named-graph count, while value-list counts span their complete query dataset. Scalars accept repeated `language` parameters in browser preference order, safely bound into the ranking expression.

The locked browser storage interfaces are queryStream.createRowStoreBudget(maxBytes), optional RowStoreOptions.memoryBudget, optional app.loadModel options containing rowStoreOptions and filterRows, and queryStream.createRowIdentityIndex(repositoryScope, budget) returning a promise of add(rows), filter(rows), and dispose(). The index retains only initial baseline identities and performs typed comparisons in a worker. Existing loadModel callers remain compatible. Budget accounting is an estimate of owned retained/in-flight data, not an unverifiable exact browser heap measurement.

Role response variables are subject, predicate, object, context. Value-list/scalar variables contain only value. Count responses contain no rows and metadata `explore-count` as an exact decimal string. Common metadata is `explore-resource` (the existing encoded resolved operand), `explore-graph-mode` (physical or dataset), `explore-section`, and `explore-kind`; listing responses include `explore-limit`. Echo query-request-id when provided. Frontend always generates a unique ID for every section request. Server may generate an ID for direct requests without one. Section responses do not link/fetch info again. Valid progress records are keepalives, not terminal success; accept them between data records and before delayed count metadata.

Reject legacy counts=true without a section. offset, know_total, and limit_explore never control server retrieval or bypass caps. Preserve ordinary resource URLs and prefixed-name resolution. Validate malformed resources, section/kind/load/scope combinations, and inapplicable term positions without a statement API fallback. Metadata reads remain on the main response rather than every subsection connection.

There are no new third-party dependencies. Shared Java helpers and frontend types are internal unless an existing API must be extended for per-connection origin negotiation; preserve public signatures wherever possible.

## Plan of Work

### Milestone 1: build and failing contracts

Run the mandatory root quick clean install before tests, retaining full output in maven-build.log. Create smallest-scope automated regressions for each owned behavior before changing its production code. Backend failures must include Surefire/Failsafe report snippets. Frontend failures retain Node output. Preserve first-run evidence in initial-evidence.backend.txt, initial-evidence.origins.txt, and initial-evidence.frontend.txt; root aggregates the initial evidence into initial-evidence.txt without overwriting pre-existing evidence. Keep logs and untracked artifacts.

### Milestone 2: independent streaming SPARQL sections

Replace Explore's scans and result ArrayList with metadata-only loading plus the section interface. Each data request prepares one query on its actual origin-enabled connection and streams bounded rows. Use seven simple statement-count components: default/named for outgoing, incoming, predicate, plus graph. Use eight distinct-member counts for types and the seven RDFS lists. Counts remain independent of row limits and rows remain usable if a count fails. Never sum overlapping role counts into a unique-statement total.

Outgoing matches resource as subject; incoming matches object; predicate matches predicate; graph enumerates GRAPH ?context and filters sameTerm(?context, ?resource). All roles include self-links and inferable rows where supported. Role rows use complete quad DISTINCT and statement-count components use corresponding distinct bindings. For physical-mode stores, query default context explicitly and named contexts separately. Enable that mode only for established Memory/Native/LMDB context semantics through supported wrappers. Treat proxies, federations, and unknown implementations as exposed query datasets and clearly label that mode. No guessed URL capabilities or content-based heuristics.

The eight value patterns are resource rdf:type ?value; resource rdfs:subClassOf ?value; ?value rdfs:subClassOf resource; ?value rdfs:domain resource; resource rdfs:subPropertyOf ?value; ?value rdfs:subPropertyOf resource; resource rdfs:domain ?value; resource rdfs:range ?value. Preserve current self-exclusions and distinct displayed members. Labels/comments query the resource's respective RDFS property, filter suitable literal values, and rank each browser language's exact/base match before the next browser language, then untagged, English, fallback. These scalar queries may ORDER BY their language rank; listing queries use no ORDER BY or OFFSET.

Use existing cancellation components, with Explore operation owner routing and a distinct operation kind. The five-minute deadline includes admission and execution. Counts queue fairly when their repository is busy. Close query results and connections on success, failure, timeout, cancellation, and disconnect, without retaining a second metadata connection. Keep emitted data bounded; no new federation-wide fallback scans are allowed in Workbench.

### Milestone 3: independent browser caches

Create shared collection-controller behavior for twelve lists, with separate committed and staged stores, count/loading/error states, page position, request IDs, and lifecycle ownership. Start sections without awaiting one global result. Use four listing/scalar and four count slots. Sum role count components only when both are complete; keep exact decimal values without JS Number rounding.

Use local 100-row statement pages and 40-row value pages. Remove All/unbounded UI and server pagination requests. Types become a paginated value list rather than an unbounded chip array. Dedicated summaries must not depend on whether the first statement batch happened to contain the resource's label or schema rows. Remove the previous complete-store summary rescans on each page action.

On Load more, fetch exactly LIMIT 1000000. For reusable initial identities, filter against the entire initial cache, preserve its order, and append unseen new rows up to the one-million retained cap. Use a bounded worker-side initial identity index, exact typed equality, and collision verification if hashing. Query DISTINCT guarantees uniqueness within the new response. If initial rows contain any uncertified blank node, including within nested triple terms, commit the fresh expanded snapshot instead and reset the page. Commit only an explicit successful terminal response without model.error; failure/abort leaves the initial cache intact. Never request another expansion after success.

Share the existing 256 MiB memory allowance across all page-owned stores, staging, dedup identities, serialization copies, and pending batches. Spill through existing IndexedDB support and fail locally/retryably when storage cannot progress. Never load a million rows into one main-thread array or DOM. Dispose every collection store and cancel every active request on route cleanup; stale generations cannot publish rows.

### Milestone 4: stable blank node plumbing

Enable existing RDF4J binary origin negotiation on every actual Workbench read connection, independently of browser writer selection. Unwrap delegating HTTP connections safely with cycle protection. Prefer origin-compatible binary formats per connection without mutating shared repository preferences. Only acknowledged transport may confer stored-node authority. Configure Clear/BrowseList discovery worker connections and Remove graph-choice connections, carrying the actual originating ValueFactory to their result writer. Keep ordinary download formats parseable and their labels document-local.

Add/reuse canonical typed term identity for classic Workbench, matching Graph explorer semantics. Certified nodes use repository identity and raw stored ID, regardless of response scope; uncertified nodes use a fresh response scope and label. Include term kind, full literal lexical/datatype/language/direction identity, graph context, and nested triple components. Assign scope recursively during page-model ingestion; preserve certification and scope through packed terms and worker storage. Display strings, copy strings, exploreResource, and N-Quads serialization must not serve as equality keys. Keep unverified nodes visible without promoting them into reusable query/explore/graph operands.

### Milestone 5: review and end-to-end verification

Obtain Sol/Astra review of final implementation and repair actionable findings with focused regressions. Run affected Java modules, frontend build and tests, then browser coverage against packaged RDF4J Server and Workbench. Preserve actual command/report evidence. Broaden tests only for remaining cross-module risks or failures. Record any genuine infrastructure blocker precisely; do not claim tests that were not run.

## Concrete Steps

All shell commands run from the repository root unless noted. Initial build uses pipefail, mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install, with tee maven-build.log and the AGENTS error/Reactor Summary filter. Missing offline dependencies permit one exact online retry before returning offline; other build failures retry without -T 1C. Long commands run as background execution sessions.

Java tests use the mvnf skill with retained logs, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py ExploreServletPageDataTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs

Use workspace-local .m2_repo on every Maven command. Never run tests with -am or -q. Backend coordinator serializes Maven runs and formatting across workers. Each test owner captures first failing output before production edits and post-fix output from the same focused selection. Run copyright checks immediately for new Java files and before formatting; use exact Eclipse headers/current creation year and the Codex signature. Run the repository formatter only after workers finish editing their files, review its diff, and keep unrelated formatting out of the final patch.

Frontend validation in tools/workbench starts with npm run build:classic for focused Node loops, then npm run build and npm test for the finished frontend. Browser verification follows e2e/README.md and the existing packaged server targets, including Explore navigation, Query blank-node results, and Graph handoff. Keep all test/browser artifacts.

## Validation and Acceptance

The backend suite must assert no exportStatements/getStatements/size access for resource data, metadata-only initial loading, all roles and overlapping self-links, graph aliases and copies across contexts, literal/prefixed/blank/triple operands, inference, escaped values, distinct summary counts, language selection after the former 64-candidate cutoff, limits and invalid parameters, exact large counts, remote HTTP/proxy/federation behavior, fair saturation, timeout, cancellation, and connection cleanup. Include a federation with duplicate member quads and a generic endpoint whose named rdf4j:nil graph differs from its default dataset.

Frontend tests cover zero/initial/cap boundaries, explicit expansion, pagination without requests, reordered/changed responses, certified dedup, uncertified snapshot replacement, failed/partial terminal records, independent count/list failures, unknown counts, retries, stale navigation, all-store disposal, shared memory/storage failure, and no million-row DOM or main-thread set. Verify exact count strings beyond Number.MAX_SAFE_INTEGER.

Real loopback Server/Workbench checks obtain the same stored blank node across query/page/graph requests on Memory, Native, and LMDB. Include wrapped HTTP connections, non-binary preferences, missing/invalid acknowledgement, stored and generated same-label nodes, nested triples, Unicode/escaped IDs, and Clear/Remove graph choices. Uncertified nodes remain response-local and trigger approved snapshot replacement where needed.

Success is visible: initial Explore navigation launches independent SPARQL requests, each section renders without waiting on all counts, page changes make no repository request, Load more reruns only the selected listing with the larger limit, and no listing exceeds one million retained rows. Counts and data are independent live reads; concurrent writes may temporarily make their totals differ. Limits apply to returned/stored listing results, not count work or undocumented internal federation materialization.

## Idempotence and Recovery

Preserve initial evidence before later runs replace reports. A failed expansion disposes its stage and keeps the committed cache. A failed test/build is investigated at the owning module without deleting unrelated artifacts, relaxing assertions, or restoring unrelated changes. Do not use destructive Git operations or manual stash. Changes are uncommitted until separately authorized.

## Artifacts and Notes

Initial tracked HEAD was ec7e48248e on main. Initial Java runtime was Zulu OpenJDK 27.0.1. Maven full build output belongs in maven-build.log; retained verify logs and report paths are supplied by mvnf. Evidence and review outcomes will be appended as milestones finish.

Revision 2026-11-03: created the implementation ExecPlan from the approved plan and locked the shared wire contract before parallel production edits.

Revision 2026-11-03: recorded the unrelated untracked LMDB compilation blocker and non-destructive, install-only recovery strategy; the new feature's targeted tests remain mandatory.

Revision 2026-11-03: recorded HTTP origin red/green evidence, the frontend controller milestone, and the heartbeat page-protocol integration requirement found during review.

Revision 2026-11-03: recorded backend 31-test progress, pending Clear/Remove red evidence and storage handoff, locked storage interfaces, and the broader frontend fixture migration work.

Revision 2026-11-03: recorded full-module and interoperability results, interim browser/runtime handoff, explicit-empty validation evidence, and the independent review's two lifecycle repairs with clear file and Maven ownership.

Revision 2026-11-03: recorded final affected-suite greens, resolved disconnect/window/disposal ownership review findings, copyright/targeted formatting, source freeze, and final packaging/runtime handoff.

Revision 2026-11-03: recorded the built-package hash verification and the rapid-navigation cleanup cancellation race found by final browser acceptance; implementation remains open until its backend regression and rebuilt-package browser verification pass.

Revision 2026-11-03: recorded the completion repair's passing suites, the independent review's delayed-registration regression, and the decision to preserve existing cancellation retries while acknowledging cleanup separately from remote completion.

Revision 2026-11-03: replaced stale milestone outcomes with final test totals, source/package provenance, six-case browser acceptance, preserved root-build limitations, cancellation protocol bounds, and owned-runtime shutdown.

Revision 2026-11-03: the user separately authorized creating a branch, committing, pushing, and opening a draft pull request. Use GH-0000 because no issue number was supplied; publish only this implementation, its tests, and this ExecPlan, preserving unrelated local artifacts.
