# Contain server-side SPARQL SERVICE access

This ExecPlan is a living document maintained under `.agent/PLANS.md`. The worktree is `/Users/havardottestad/.codex/worktrees/security-service-egress/rdf4j`, on branch `GH-0000-server-service-egress` because no separate SERVICE issue number was supplied.

## Purpose / Big Picture

An RDF4J Server HTTP client must not make the server query a loopback or private network SPARQL endpoint through a `SERVICE` clause. Public endpoints should continue to work, administrators need an exact trusted-origin configuration for private endpoints, and embedded Java repositories should retain their existing behavior. The first observable proof is a server-created connection whose `SERVICE <http://127.0.0.1:port/sparql>` query currently reaches a local listener; after the fix, no request reaches it.

## Progress

- [done] create isolated branch and worktree (2026-10-23 02:39Z)
- [done] complete initial root quick install (2026-10-23 02:42Z)
- [done] capture smallest failing SERVICE query (2026-10-23 02:47Z)
- [done] trace transport and design policy injection (2026-10-23 02:53Z)
- [done] reproduce resolver sharing across stores
- [done] revise secure SERVICE injection design
- [done] trace FedX snapshot resolver path
- [done] fix snapshot resolver per connection
- [done] rerun affected suites and formatting
- [done] review candidate before publication (2026-10-23 04:12Z)
- [done] publish stacked draft PR 6233 (2026-10-23 04:18Z)
- [done] repair exact-head integration fixture (2026-10-23 05:31Z)
- [in_progress] verify and push scoped follow-up

## Surprises & Discoveries

The offline root install could not resolve `maven-dependency-plugin:3.11.0`; the exact online retry succeeded in the private `service-egress` Maven workspace. `LocalRepositoryManager` installs a shared `SPARQLServiceResolver` on newly created repositories, while `SailRepositoryConnection` can set a resolver on an individual connection. A per-connection resolver may check variable or nested SERVICE endpoint values at evaluation time, but transport-level redirect and connected-address checks are still required. The smallest server-created connection test failed before production edits, and the refined assertion proves the loopback listener received exactly one request: `Expecting AtomicInteger(1) to have value: 0 but did not`.

## Decision Log

The first test uses `RepositoryInterceptor.getRepositoryConnection` because that method creates ordinary Server request connections, then evaluates a real SPARQL SERVICE query against a loopback HTTP listener. This proves server reachability without relying on a text parser blacklist. Date/Author: 2026-10-23, Codex.

The SERVICE patch is stacked on the exact head `60e9cfc` of draft PR #6228, which covers RDF `LOAD` and DatasetRepository URL sources. A shared package-local destination policy will be extracted in this SERVICE branch so address ranges, exact origins, and pinned DNS answers cannot drift between the two paths. The HTTP transport, rather than a lexical SERVICE-only check, will enforce redirect hops and socket destinations. Date/Author: 2026-10-23, Codex.

The per-connection resolver must retain registered in-process implementations. A returned `SPARQLFederatedService`, including a cached or manually registered one, will use the secure transport; unknown URLs must not call the shared resolver because that grows its cache and can invoke custom side effects. FedX requires a per-connection resolver in its SailConnection and must recognize configured federation members without an unsafe unknown lookup. This avoids widening the shared resolver API merely to classify registrations. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The initial build and in-repo red SERVICE query tests are complete. On the stacked #6228 base, the four-test class has three intended failures (constant, variable/nested, concurrent embedded/server) and one passing registered in-process control. No production code has changed. Implementation, compatibility testing, and candidate review remain.

## Context and Orientation

`core/queryalgebra/evaluation/.../ServiceJoinIterator.java` resolves a SERVICE endpoint when evaluating a query, so bound variable endpoints can be chosen at runtime. `core/repository/sparql/.../SPARQLServiceResolver.java` creates a `SPARQLFederatedService` backed by an HTTP SPARQLRepository. `core/repository/sail/.../SailRepositoryConnection.java` exposes per-connection resolver assignment. Server requests get connections in `tools/server-spring/.../RepositoryInterceptor.java` and `DefaultRepositoryResolver.java`; transaction operations open a separate connection in `Transaction.java`. A secure boundary must cover all three and inspect each outbound HTTP destination and redirect before connecting, including DNS answers that are actually used for the socket.

## Plan of Work

First add a focused in-repo test in `tools/server-spring/src/test/java/org/eclipse/rdf4j/http/server/repository/` that starts a loopback SPARQL listener, executes a SERVICE query through a server-created connection, and asserts that the listener receives zero requests. Run this one method with `mvnf --workspace service-egress --retain-logs` and immediately preserve the Surefire failure in top-level `initial-evidence.txt`. Add sibling tests for bound-variable and nested SERVICE, transaction and resolver paths, redirects, DNS rebinding/address pins, trusted exact scheme/host/port, a public endpoint, and an embedded control before changing production behavior.

Next extract the exact-origin, public-address, and DNS-pinning logic from #6228's `ServerRemoteResourceOpener` into a package-local policy used by both paths. Install a per-connection SERVICE resolver wrapper in the ordinary, query-controller, and transaction connection creation paths. It delegates registered in-process services but uses a secured SPARQL service for standard remote services, including cached instances. A lazy HTTP session manager and its services must be closed with the connection. The secure `RDF4JHttpClient` transport must reject a disallowed redirect or private connected address, avoid forwarding credentials across origins, and preserve query POST/GET semantics and result streaming. Test failure and response cleanup.

Finally run focused, affected module, and live Server HTTP integration tests where practical. Run copyright and formatter checks, inspect the diff, and present the candidate to the parent reviewer before any commit, push, or PR creation.

## Concrete Steps

All commands run from this worktree root. The `.m2_repo` symlink is a read-through cache; workspace `service-egress` owns writable repository and build roots.

    python3 .codex/skills/mvnf/scripts/mvnf.py ServerServiceEgressTest#serverQueryDoesNotContactLoopbackService --workspace service-egress --retain-logs
    python3 scripts/agent-evidence.py --command "mvnf service-egress red selection" .mvnf/workspaces/service-egress/build/org.eclipse.rdf4j/rdf4j-http-server-spring/6.2.0-SNAPSHOT/surefire-reports > initial-evidence.txt

The red report shows the listener was contacted. On the stacked #6228 base, `red-stacked-variants-evidence.txt` records 4 tests, 3 intended failures, and the passing registered-service control. After the fix, the same methods must pass with zero new listener calls for denied Server queries; the embedded control should still obtain a result. No test invocation may use Maven `-am` or `-q`.

## Validation and Acceptance

Ordinary Server queries, alternate query-controller connections, and transaction queries must all deny untrusted loopback and private destinations before any listener request, including via redirect. Variable SERVICE endpoints must be checked at evaluation time. A trusted exact origin and a public destination remain usable, and an embedded repository connection on the same underlying repository retains its preexisting SERVICE behavior. Red and green Surefire/Failsafe reports must prove those outcomes; module and live HTTP checks must remain green.

## Idempotence and Recovery

Preserve all untracked logs and evidence. Do not reset, clean, or overwrite unrelated files. Repeated `mvnf` can replace reports, so copy the initial red snippet immediately. If offline Maven resolution fails for a missing dependency, retry once online and then return offline. The isolated worktree may be rebuilt without touching the main checkout.

## Artifacts and Notes

The failed offline root install log is `.mvnf/workspaces/service-egress/logs/20261023T023942.393263Z-95328-cf0f423a/maven.log`. The successful online retry log is `.mvnf/workspaces/service-egress/logs/20261023T023953.830541Z-95413-c16640d2/maven.log` and ends in `BUILD SUCCESS` after 1:45 min. `initial-evidence.txt` preserves the first red Surefire report; `red-listener-evidence.txt` preserves the refined one-request assertion from `.mvnf/workspaces/service-egress/logs/20261023T024619.851552Z-18638-ee07ab67/verify.log`; `red-stacked-variants-evidence.txt` preserves the four-test report from `.mvnf/workspaces/service-egress/logs/20261023T025150.249069Z-40665-eafc6e19/verify.log`. Add later green paths here as they are produced.

Revision note, 2026-10-23 02:53Z: Fast-forwarded the separate SERVICE branch to PR #6228 so shared destination policy can be extracted once. The stacked-base red class confirms dynamic, nested, cached, and concurrent exposure, while the custom registered in-process control passes. Parent design review favored transport enforcement and forbade mutating the shared repository resolver.

Revision note, 2026-10-23 03:20Z: Parent review identified two remaining risks in the first candidate: delegating unknown SERVICE URLs grows the shared resolver cache and may run custom side effects; LMDB isolation needed confirmation. The focused run reproduced cache growth and a custom resolver call, preserved in `red-cache-evidence.txt`. Concurrent embedded and server connections passed for both LMDB and NativeStore, so the per-connection setter remains valid for those stores. Unknown resolver lookup has since turned green.

Revision note, 2026-10-23 03:26Z: `red-redirect-evidence.txt` captures two 301/302 POST mismatches with the existing Apache RDF4J HTTP client; the behavior fix is green. `red-fedx-evidence.txt` captures Server-hosted FedX reaching loopback and failing to recognize an in-process member before FedX production edits. FedX is shipped in the Server WAR, so connection-local resolution must cover it.

Revision note, 2026-10-23 03:34Z: `red-preflight-evidence.txt` captures denied private SERVICE URLs being accepted at resolver creation time, allowing distinct entries to accumulate on a long-lived Server transaction. The resolver now preflights URL syntax and resolved address before creation, and the socket still performs its independent per-hop DNS validation. The focused Server SERVICE class is green (12/12), including composed query forms, repository and per-connection cache independence, LMDB/NativeStore concurrent connections, and denied distinct URLs. The focused FedX class is green (2/2) after connection-local propagation and side-effect-free configured-member recognition. Remaining work is member URL variant, full classes/modules, live HTTP coverage, format and review.

Revision note, 2026-10-23 03:48Z: The full FedX module is green (599 tests, no failures/errors, 52 skips), and Server Spring is green (251 tests, no failures/errors). `ServerOutboundResourceIT` is green (three live HTTP cases: ordinary and transaction LOAD, plus SERVICE). Its first isolated-workspace run could not start Jetty because the fixture hardcodes `./target/rdf4j-server`; the ignored target symlink now points to the packaged WAR and both focused and full IT reruns pass. The final copyright and formatter checks pass, and `git diff --check` is clean. Candidate awaits independent review before commit/push/PR.

Revision note, 2026-10-23 03:56Z: Parent review found that a captured FedX graph snapshot evaluates prepared SELECT text with the shared federation resolver. A new focused in-repo test in `ServerSnapshotServiceEgressTest` proved that a prepared SERVICE clause reached a loopback listener despite the Server connection resolver (`red-snapshot-network-evidence.txt`: AtomicInteger(1), expected 0). The normal optimizer does not render a SERVICE inside its prepared statement-pattern queries, but the `TripleSource` prepared-query path accepts arbitrary text, so snapshot evaluation must use the query strategy's per-connection resolver. This red evidence precedes the production edit.

Revision note, 2026-10-23 04:02Z: `TripleSourceBase` now passes the query strategy's resolver into `RemoteGraphSnapshot.evaluate`. The focused SPARQL-member Surefire execution passes 1/1; NativeStore and HTTPRepository-member Surefire executions skip with the existing `assumeSparqlEndpoint()` helper because neither creates this snapshot. Log: `.mvnf/workspaces/service-egress/logs/20261023T040046.433378Z-99005-41550b5f/verify.log`. Full affected suites and final formatting are pending.

Revision note, 2026-10-23 04:11Z: Final affected verification is green: FedX module 600 tests, zero failures/errors, 53 skips (`.mvnf/workspaces/service-egress/logs/20261023T040231.468321Z-19391-b8c918c1/verify.log`); Server Spring 251/0/0 (`20261023T040730.062295Z-78305-45279395/verify.log`); live `ServerOutboundResourceIT` 3/0/0 (`20261023T040903.664632Z-97634-a1cdd860/verify.log`). Copyright check passed; non-quiet formatter `format-service-snapshot.log` ends in `BUILD SUCCESS`; `git diff --check` is clean. Publication remains held for parent candidate review.

Revision note, 2026-10-23 05:08Z: Draft PR #6233 was published and attached at exact head `827a0b464cba82500e1ae948a93b2873f0360aaa`. The integration job failed two existing `LmdbServiceReplayIT` cases at the loopback SERVICE gate. Local focused Failsafe reproduced both failures before edits; `red-lmdb-service-ci-evidence.txt` preserves the report. The fixture requires explicit exact-origin trust for its own two ephemeral loopback SERVICE listeners; production denial remains the intended default.

Revision note, 2026-10-23 05:31Z: The fixture now starts both loopback SERVICE listeners before the Server policy reads the exact allowed-origin property and restores the previous setting after the class. With this fixed, the triple-growth case exposed a pre-existing fixture sizing error: the HTTP buffered-write admission conservatively projects about 109 MiB of value-map demand for 30k statements before dictionary deduplication, exceeding the original 128 MiB map at its 10% threshold. `20261023T052347.795174Z-37128-3875d914/verify.log` preserves the unchanged assertion failure and growth metrics. A 2 GiB virtual value map keeps that advisory demand below the threshold without weakening the triple-only growth assertion. The exact-code full `LmdbServiceReplayIT` passed 2/2 in `.mvnf/workspaces/service-egress/logs/20261023T052948.706192Z-60732-cb932b25/verify.log`.

## Interfaces and Dependencies

The initial implementation should prefer existing `FederatedServiceResolver` and `RDF4JHttpClient` extension points over SPARQL text screening. Review lifecycle and thread ownership before choosing a wrapper. If a new transport dependency is necessary, record its health and existing project adoption. No new dependency has been selected yet.
