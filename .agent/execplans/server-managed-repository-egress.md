# Constrain outbound requests from Server-managed remote repositories

This ExecPlan is a living document under `.agent/PLANS.md`. It applies to the isolated worktree `/Users/havardottestad/.codex/worktrees/security-managed-repository-egress/rdf4j`, branch `GH-0000-managed-repository-egress`, based on the published SERVICE egress branch at `80c99601fda78b9c822ef56bbd2fda489f233bf1`. No issue number was provided, so `GH-0000` is the repository's placeholder convention.

## Purpose / Big Picture

An RDF4J Server client can submit a repository configuration that points an `HTTPRepository` or `SPARQLRepository` at another URL. If a later query from the Server makes that request to a loopback or private address, the Server has become an unintended network client. After this change, Server-managed remote repositories must obey the same per-hop address and exact trusted-origin policy already used for Server SPARQL SERVICE. An embedded `LocalRepositoryManager` or programmatic remote repository must retain its existing behavior. A live Server HTTP test will submit a configuration, query it, and observe that an untrusted private endpoint receives no request.

## Progress

- [done] trace server configuration creation routes (2026-10-23 05:39Z)
- [done] capture initial build and live red (2026-10-23 05:45Z)
- [done] reproduce nested and alternate transports (2026-10-23 05:52Z)
- [done] finalize shared transport and connector scope (2026-10-23 05:56Z)
- [done] implement server-managed outbound boundary (2026-10-23 06:28Z)
- [done] verify nested routes and redirect cases (2026-10-23 06:34Z)
- [done] verify managed plugin class boundaries (2026-10-23 06:46Z)
- [done] run affected module test suites (2026-10-23 07:13Z)
- [done] rerun formatter and copyright gates (2026-10-23 07:15Z)
- [in_progress] review candidate and publish draft
- [todo] monitor exact published head checks

## Surprises & Discoveries

The public `PUT /repositories/{id}` handler, `POST /repositories/{id}/config`, and the synthetic SYSTEM configuration repository all call the same `LocalRepositoryManager.addRepositoryConfig`. `LocalRepositoryManager.createRepositoryStack` recursively instantiates repository configs, including HTTP and SPARQL remotes, and gives each `SessionManagerDependent` its shared `HttpClientSessionManager`. The Server's `LocalRepositoryManagerFactoryBean` creates this manager before it serves requests. A policy installed there should cover nested delegated repositories and all three write routes without changing embedded managers.

The bundled Elasticsearch Lucene Sail is a distinct outbound client. `ElasticsearchIndex` creates its own Rest5 transport, defaults to localhost, and does not use the manager's `HttpClientSessionManager`; it needs its own server-managed guard. An attacker can also select `ElasticsearchIndex` from a generic `LuceneSailConfig` through `lucene.index`. Therefore the Server guard belongs at the shared Lucene index creation path, not just the Elasticsearch-specific Sail config. Server-managed Sail configs also accept reflective analyzer, similarity, and evaluation-strategy factory class names, all of which run a static initializer and constructor before type checking; a Server-only class allowlist is needed before repository instantiation.

## Decision Log

- Decision: Base this worktree on the SERVICE egress branch so a single origin, address, DNS, and redirect policy can be reused. Rationale: separate public/private classifiers would drift. Date/Author: 2026-10-23, Codex.
- Decision: Prove the remote-config request from a running Server before any production edit. Rationale: accepting a configuration is not proof that its later query reaches the named target. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The initial live test submitted an `HTTPRepositoryConfig` through the public Server API and then queried the managed repository. The loopback mock received two requests despite an expected zero. `initial-evidence.txt` preserves that Failsafe failure. Separate red reports establish SPARQLRepository, delegated repository, FedX inline members, Elasticsearch Rest5, generic Lucene-to-Elasticsearch index override, and reflective plugin-class sinks. The Server-created manager now uses the stacked per-hop HTTP transport, passes it to FedX members, and attaches a pinned-DNS/direct-route/no-redirect Rest5 customizer to a selected ElasticsearchIndex. Live tests pass for default private denial, exact trusted scheme/port, redirected private destinations, config restart and SYSTEM commit routes, embedded manager independence, bundled plugin classes, and exact administrator-approved custom classes. `ManagedRepositoryEgressIT` passed 16/16; `ManagedRepositoryTrustedOriginIT` passed 9/9. Full `tools/server` verification passed 96 tests, zero failures/errors, one skipped (`server-full-green-evidence.txt`). A class-identity regression prompted replacing a manager subclass with a pre-init runtime config hook; existing Server Spring tests passed 251/251. Federation passed 600 tests with 53 skipped; manager, Lucene API, Lucene Sail, and Elasticsearch Sail modules passed. Copyright and formatter passed; `git diff --check` is clean.

Compatibility risk is medium for deployments that intentionally use private managed backends or custom SearchIndex/analyzer/similarity/evaluation classes. Administrators must configure exact trusted origins and exact approved plugin classes before upgrade. No new external dependency was added. Remaining transport limits: permitted HTTPS to HTTP redirects are allowed by the shared Server HTTP client, but cross-origin Authorization/Cookie headers are stripped; unknown custom SearchIndex plugins can perform their own I/O once approved; the optional ElasticsearchStore module is outside the standard Server bundle. The Rest5 DNS API receives only a hostname, so mixed trusted and untrusted origins sharing one Elasticsearch hostname fail closed for that hostname.

## Context and Orientation

The Server WAR maps repository creation in `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/handler/DefaultRepositoryRequestHandler.java`, config update in `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/config/ConfigController.java`, and SYSTEM configuration commits in `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/RepositoryConfigRepository.java`. Their shared manager is created by `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/manager/LocalRepositoryManagerFactoryBean.java`. `core/repository/manager/src/main/java/org/eclipse/rdf4j/repository/manager/LocalRepositoryManager.java` creates repository stacks and injects its shared HTTP session manager. The remote factories in `core/repository/http` and `core/repository/sparql` use the config URLs without a Server-specific destination check. Existing Server egress classes in `tools/server-spring/.../repository/` validate URL syntax, redirects, and the addresses actually sent to HTTP sockets; the exact-origin property is `rdf4j.server.remoteResourceAllowedOrigins`.

## Plan of Work

First write a live Failsafe test in `tools/server/src/test/java/org/eclipse/rdf4j/http/server/` that submits a managed remote repository through Server HTTP, queries it, and asserts that an untrusted loopback target sees zero requests. Run only the new test and immediately save the Failsafe failure snippet in top-level `initial-evidence.txt`. Extend the test to cover HTTPRepository, SPARQLRepository, a delegated wrapper, explicit trusted origin, cross-origin redirect, and update or SYSTEM configuration routes where feasible. Keep a separate list of Elasticsearch transport behavior and determine whether it can be safely covered here or requires another PR.

Then adapt the existing Server outbound HTTP transport to serve manager-created remote repositories, install it on the Server-created manager before initialization, and leave direct library managers untouched. The enforcement point must validate each redirect URL and each socket's resolved address, and must not transfer credentials across origins. If configuration validation is also needed, place it at a shared Server manager boundary so PUT, config POST, and SYSTEM commits behave consistently. Do not rely on a one-time DNS check during configuration.

Finally run the new focused test, affected module tests, a live HTTP path, copyright and formatter checks, and inspect the exact diff. State user break risk and migration for intentional private endpoints using exact administrator-configured origins. Publish a separate draft PR stacked on #6233 only after independent parent review, then verify local, remote, and PR head SHAs and inspect exact-head ECA and CI checks.

## Concrete Steps

Run commands from `/Users/havardottestad/.codex/worktrees/security-managed-repository-egress/rdf4j`. The `.m2_repo` symlink reads the shared cache, while `--workspace managed-repo-egress` keeps Maven writes private.

    python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-spring --workspace managed-repo-egress --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --it ManagedRepositoryEgressIT#rejectsPrivateHttpRepository --workspace managed-repo-egress --retain-logs
    python3 scripts/agent-evidence.py --command "<the failed command>" <Failsafe report directory> > initial-evidence.txt

The first command should finish `BUILD SUCCESS`; the new test should fail before production edits because its private endpoint receives a Server request. After the fix, rerun the exact test, then its class and affected modules. Do not use `-am` or `-q` with tests. Keep full `mvnf` logs and untracked red evidence.

## Validation and Acceptance

The Server may persist a configured remote repository, but a later query through that repository must not contact an untrusted loopback/private address, including after an HTTP redirect. An exact administrator-trusted HTTP(S) origin should remain usable, with the configured scheme and port distinguished. The same policy must apply to both HTTPRepository and SPARQLRepository, including delegated stacks, while a direct embedded repository still has its ordinary behavior. A route test must prove that the shared Server manager is reached from HTTP configuration submission. A separate inventory must explicitly classify the bundled Elasticsearch Sail transport.

## Idempotence and Recovery

No production edit is allowed before the first in-repo red Failsafe report has been observed and copied to `initial-evidence.txt`. Do not reset, clean, delete, or stage evidence artifacts. If a test fixture fails before reaching the remote request, repair the fixture and run it again before any production change. The parent is reviewing publication milestones; keep this worktree isolated from #6233 and #6238.

## Artifacts and Notes

The initial private Maven build log, red Failsafe reports, final green summaries (`managed-egress-final-green-evidence.txt`, `trusted-origin-nine-green-evidence.txt`, `server-full-green-evidence.txt`), final formatter and copyright logs, and exact diff are retained in this worktree. CI head inventory will be recorded after publication.

## Interfaces and Dependencies

Prefer the existing `ServerOutboundDestinationPolicy` and Server HTTP transport on the stacked branch. No new dependency is expected. `LocalRepositoryManagerFactoryBean` is a server-only creation point, while `LocalRepositoryManager` and direct `HTTPRepository`/`SPARQLRepository` are embedded-library APIs whose behavior should remain unchanged. The HTTP client/session manager interfaces are in `core/http/client` and are already used by both remote repository types.

Revision note, 2026-10-23 05:39Z: Created this plan after tracing the three configuration routes and the nested repository manager factory chain, before a live reproducer or production edit. The transport and Elasticsearch decisions remain open until tests establish their actual behavior.

Revision note, 2026-10-23 05:45Z: Initial live `HTTPRepository` red report and log preserved. The first fixture run exposed a `TestServer` packaged-WAR path mismatch in the isolated Maven workspace; the ignored `tools/server/target/rdf4j-server` symlink now points to the private build. The corrected test reached the sink and observed two private HTTP requests. Parent requested FedX members and bundled ElasticsearchSail Rest5 transport in this same managed-config inventory.

Revision note, 2026-10-23 05:52Z: Live tests established five distinct Server-managed outbound paths: HTTPRepository (two loopback requests), SPARQLRepository (one), FedX SPARQLEndpoint (one), FedX RemoteRepository (one), and bundled ElasticsearchSail Rest5 (two). Each focused failure is preserved in a separate `*-red-evidence.txt` file with full retained Maven logs. The manager-owned client does not reach FedX's independently constructed member repositories or Elasticsearch's Rest5 client. The parent received a design milestone proposing per-hop manager transport, FedX session propagation, and a runtime-only Rest5 policy injection.

Revision note, 2026-10-23 05:56Z: A ContextAware delegate wrapping SPARQLRepository also sent one private request; `nested-red-evidence.txt` preserves it. The implementation starts with Server-created manager transport and FedX session propagation, then treats Elasticsearch's Rest5 initialization with its own Server-only configuration hook. A static configuration URL check alone would miss DNS changes and redirects, so the Rest5 path uses request-time DNS validation and forbids redirects.

Revision note, 2026-10-23 06:40Z: The initial 11-case live egress class and six-case trusted-origin class are green in `managed-egress-eleven-green-evidence.txt` and `trusted-origin-six-green-evidence.txt`. A separate FedX dataConfig file case passed with an in-root members file. `index-override-red-evidence.txt` showed a generic Lucene config selecting ElasticsearchIndex and sending two loopback requests; its focused green report is preserved. `unapproved-index-red-evidence.txt` proved arbitrary index class selection was accepted. `plugin-class-red-evidence.txt` shows analyzer, similarity, and evaluation factory test classes were initialized and constructed before type checking in a 15-test live run. The Server-only class gate and subsequent verification are in progress.
