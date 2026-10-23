# Bound buffered RDF4J Server request values

This ExecPlan is a living document maintained under `.agent/PLANS.md`. It describes the server-side request limits from the current main checkout through a tested draft pull request.

## Purpose / Big Picture

RDF4J Server currently collects some client-supplied text and repository configuration into memory without an application limit. An oversized SPARQL query, SPARQL update, namespace value, ordinary form body, query string, or configuration graph can exhaust the server before useful work begins. After this change, those buffered request values have documented operator-configurable budgets and return HTTP 413 when exceeded. Ordinary RDF statement and graph uploads continue to stream through their existing parser path.

## Progress

- [x] (2026-10-23 00:34Z) Create isolated branch and complete root clean install; offline plugin miss was retried online successfully.
- [x] (2026-10-23 00:40Z) Map buffered sinks and add red tests; nine focused cases failed before production edits and evidence was preserved in `initial-evidence.txt`.
- [x] (2026-10-23 00:54Z) Add bounded text and configuration helpers and apply them at scalar/config sinks.
- [x] (2026-10-23 00:54Z) Bound plain and compressed form parsing through one wrapper.
- [x] (2026-10-23 00:53Z) Verify focused and server module behavior; `mvnf tools/server-spring` passed 221 tests with no failures or skips.
- [x] (2026-10-23 01:03Z) Add Servlet form compatibility regressions; both failed before refinement and passed afterward.
- [x] (2026-10-23 01:09Z) Add malformed form decoding regression; malformed percent escapes escaped as an uncaught runtime error.
- [x] (2026-10-23 01:12Z) Add unsupported configuration MIME precedence tests; both failed before the format-first fix and passed afterward.
- [x] (2026-10-23 01:05Z) Verify final module and live HTTP; 226 server-spring tests and one live HTTP integration test passed.
- [x] (2026-10-23 01:06Z) Format and review scoped candidate.
- [x] (2026-10-23 01:15Z) Red-test form parameter count exhaustion.
- [x] (2026-10-23 01:17Z) Bound form fields before map growth.
- [x] (2026-10-23 01:19Z) Map live form limit before Spring.
- [x] (2026-10-23 01:21Z) Reverify module and live HTTP.
- [x] (2026-10-23 01:23Z) Format and review final candidate.
- [x] (2026-10-23 01:25Z) Obtain independent candidate review.
- [ ] (in_progress 2026-10-23 01:25Z) Publish scoped draft and verify checks.
- [ ] Publish reviewed scope and inspect CI.

## Surprises & Discoveries

- The existing `HttpCompressionFilter` limits a compressed URL-encoded form body to 4 MiB but lets an ordinary form delegate parameter parsing to the servlet container. It also parses the compressed request's query-string component before applying any limit. The filter maps decompression limit exceptions to HTTP 413.
- The first offline root clean install lacked `maven-dependency-plugin:3.11.0`. The same clean install with network access succeeded; logs remain in `maven-build-offline-missing-plugin.log` and `maven-build.log`.
- A manual focused rerun of existing Brotli tests lacked the private per-module native-extraction directory and failed with `NoSuchFileException`; the full `mvnf` module run created it and passed all tests. No assertions were weakened.
- The existing `TestServer` fixture hardcodes `./target/rdf4j-server`, so the real HTTP integration test cannot start under `mvnf --workspace`, which relocates the WAR. A focused manual module verify using the same private Maven repository and ordinary module target passed instead.
- A review found that splitting a 4 MiB form by `&` can allocate an enormous array and map. Red tests for distinct, duplicate, empty, and GET query fields are retained in `initial-evidence.txt`; the parser now scans indices with a 10,000-field default budget across query and form body.
- The live Jetty integration test returned HTTP 500 for an over-limit form even though the unit filter test returned 413: Spring handled the wrapper's runtime exception before it reached the outer filter catch. The Failsafe red report is in `initial-evidence.txt`; form parsing must occur at the filter boundary before dispatch.
- Draft PR #6222 overlaps this branch in the server-workbench documentation and StatementsController test. A read-only patch-application check confirmed those two textual conflicts; the production StatementsController hunks are separate.
- Jakarta Servlet 6.2 specifies URL-encoded body parameters for POST and a no-charset form-body fallback of US-ASCII with percent escapes decoded as ISO-8859-1. The first shared-wrapper version changes plain PUT parameter exposure and no-charset decoding, so compatibility regressions must precede the refinement.

## Decision Log

- Decision: Bound values at the point they are buffered, and use the existing compression filter for form bodies and query strings. Rationale: RDF statement uploads must keep their streaming path, while form parameter parsing happens before a controller can inspect decoded text. Date/Author: 2026-10-23, Codex.
- Decision: Keep the existing 4 MiB default form-body cap and use separate explicit budgets for scalar text and repository configuration. Rationale: scalar values and RDF configuration models have different operators and memory costs; each limit can be lowered or raised deliberately without changing streamed RDF uploads. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The red cases are preserved in `initial-evidence.txt`, including the field-count and live 500-to-413 regressions. The final server-spring suite passed 232 tests, and a Jetty WAR integration test passed live 413 and streamed-upload checks. The repository formatter passed, copyright check passed, and `git diff --check` passed. Candidate compatibility review is pending before publication.

## Context and Orientation

The server module is `tools/server-spring`; the WAR filter mapping is `tools/server/src/main/webapp/WEB-INF/web.xml`, and Spring Boot registers the same `HttpCompressionFilter` in `tools/server-boot/src/main/java/org/eclipse/rdf4j/tools/serverboot/Rdf4jServerWorkbenchApplication.java`. The filter is an HTTP component that can wrap an incoming request before controllers read it. `DefaultQueryRequestHandler` buffers SPARQL query bodies and form parameters. `StatementsController` and `TransactionController` buffer SPARQL updates and transaction queries. `NamespaceController` buffers namespace values. `DefaultRepositoryRequestHandler` and `ConfigController` call `Rio.parse` into a whole in-memory RDF `Model` for repository configuration. Their normal statement-upload methods pass an input stream directly to the repository and must remain streaming.

The worktree is `/Users/havardottestad/.codex/worktrees/security-server-text-limits/rdf4j`, branch `GH-0000-server-body-limits`. `.m2_repo` reads from the main checkout cache, while `maven.repo.local.head` and `rdf4j.build.root` point into the private `.mvnf/workspaces/server-text` directory. The initial root `-Pquick clean install` passed. Do not stage generated Maven output, ZIPs, logs, or `initial-evidence.txt`.

## Plan of Work

First add the smallest failing tests inside `tools/server-spring/src/test/java`: an ordinary uncompressed form with an unknown content length, an oversized compressed-form query string, oversized direct SPARQL query/update and namespace input, transaction query/update, and both repository-config write endpoints. Each test must demonstrate HTTP 413 or a `ClientHTTPException` with status 413 at its narrowest practical scope, and its first Surefire report must be copied into root `initial-evidence.txt` before any production behavior edit. Add boundary and allowed cases using small configured limits so tests avoid large allocations. Include a streamed RDF statement upload control.

Then add a server request-limit helper under `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server` for bounded scalar text and configuration bytes. A scalar budget counts Java text characters, whose allocation cost is directly bounded; a config budget counts source bytes before Rio materializes a graph. Both default to 4 MiB and can be set by positive system properties documented in `site/content/documentation/tools/server-workbench.md`. Controllers use this helper before evaluating any query, applying an update, changing a namespace, or materializing a repository config. Oversized values raise `ClientHTTPException(413)` before a mutation.

In `HttpCompressionFilter`, parse ordinary and compressed URL-encoded form requests through one bounded parameter path. Preserve parameter order and charset semantics, duplicate names, and request stream/reader exclusivity. Reject an over-budget raw query string before splitting or decoding it. Keep the current 4 MiB default maximum for buffered form bytes and preserve the existing compressed-form limit setting. Do not buffer or cap Turtle, RDF/XML, N-Triples, or other ordinary statement upload bodies in this filter.

## Concrete Steps

From the worktree root, run targeted tests with `python3 .codex/skills/mvnf/scripts/mvnf.py <Class#method> --workspace server-text --retain-logs`; this command does the prerequisite root quick install. Persist the initial red report with `python3 scripts/agent-evidence.py --command '<exact test command>' --log <mvnf verify.log> <surefire-report-directory> > initial-evidence.txt`, appending later distinct red cases rather than overwriting the first one. Never use `-am` or `-q` for tests. After the implementation, run each focused selector, then `python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-spring --workspace server-text --retain-logs`. Run `scripts/checkCopyrightPresent.sh`, Maven `process-resources` without `-q`, and `git diff --check`; preserve all logs and Surefire reports.

## Validation and Acceptance

Raw query and update requests, form query and update requests, namespace PUT, repository CREATE and config update accept values at or below the configured limit and answer HTTP 413 above it, even with no Content-Length or compressed transport. A query-string-only excess fails before it is split into parameters. Both plain and compressed form parameter maps retain duplicate values and valid encodings. A statement upload larger than the scalar limit still reaches its streaming parser. Module tests pass with zero new failures. The reviewed PR body states the default limit, configuration properties, measured red/green evidence, and user break risk.

## Idempotence and Recovery

All build and test commands may be retried in the private Maven workspace; first preserve any red Surefire evidence because subsequent runs replace reports. An offline Maven plugin miss should be retried once online, then subsequent commands return offline. Changes remain scoped to this branch and worktree; preserve untracked evidence and use exact-file staging. If behavior changes after a test design mistake, correct the fixture without weakening the resource-bound assertion and rerun the smallest selection.

## Artifacts and Notes

The clean-install transcript ends with `BUILD SUCCESS` in `maven-build.log`. The original offline miss is retained in `maven-build-offline-missing-plugin.log`. Initial failing tests and their report snippets will be appended here as they are observed.

## Interfaces and Dependencies

Use the servlet request and Spring controller abstractions already in `tools/server-spring`; no new external dependencies are required. The proposed helper exposes bounded text-read and text-validation methods plus a bounded configuration byte-read method, each accepting a servlet request or stream and throwing the existing `ClientHTTPException` with status 413. `HttpCompressionFilter` continues to own form parameter decoding and converts its own form limit failures to HTTP 413. The helper's precise method names should be chosen after the red tests reveal the smallest reusable interface.

Revision note: Initial plan written after source-path inventory and successful root clean install, before any production edits.
