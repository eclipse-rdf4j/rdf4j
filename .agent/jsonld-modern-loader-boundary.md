# Enforce modern JSON-LD loader policy at each request and bound its shared cache

This ExecPlan is a living document maintained under `.agent/PLANS.md`.

## Purpose / Big Picture

An RDF4J JSON-LD parser in secure mode should fetch only the exact context URLs allowed by its whitelist. The bundled JSON-LD HTTP loader follows redirects and JSON-LD alternate links internally, so a whitelisted URL can currently send the parser to an unlisted destination. After this change, a local HTTP test will show that the unlisted endpoint receives no request, while allowed contexts still parse. The shared document cache must not retain an unbounded amount of document data or return a document fetched under a different security policy.

## Progress

- [x] (2026-10-23 04:30Z) Inspect loader, dependency, and settings.
- [x] (2026-10-23 04:30Z) Reproduce redirect and alternate bypasses.
- [x] (2026-10-23 04:35Z) Add related cache and legitimate regressions.
- [x] (2026-10-23 04:38Z) Enforce policy at outgoing request boundary.
- [x] (2026-10-23 04:40Z) Bound and isolate shared document cache.
- [x] (2026-10-23 04:46Z) Run focused and complete JSON-LD suites.
- [x] (2026-10-23 04:55Z) Prove wide-container cache bound.
- [x] (2026-10-23 04:57Z) Reject oversized containers before queueing.
- [x] (2026-10-23 05:00Z) Rerun focused and module verification.
- [ ] **in_progress** Review final diff and publication.

## Surprises & Discoveries

- `no.hasmac.jsonld.loader.DefaultHttpLoader` follows 301/302/303/307 responses and JSON-LD alternate links by making another `no.hasmac.jsonld.http.HttpClient.send` call. RDF4J currently checks only the original URI in `CachingDocumentLoader.loadDocument`.
- The current static Guava cache has `maximumSize(1000)` but no weight budget. Its loader creates fresh `DocumentLoaderOptions`, dropping caller options, and keys only on the original URI.
- The first in-repository red test has two failures: both an HTTP redirect and an alternate link reached an unlisted target. The full report is in `initial-evidence.jsonld-modern-loader.txt` and the retained `jsonld-modern-loader` Maven workspace.
- The expanded red run had five tests: four failures exposed redirect, alternate-link, cache-policy, and request-option defects; the fully whitelisted redirect succeeded. Its report is `initial-evidence.jsonld-modern-loader-expanded.txt`.
- The first post-change run revealed Guava's `UncheckedExecutionException` wrapping a policy exception on cache misses. Unwrapping only an underlying `RDFParseException` restored the documented parse-error type. The next focused run passed 6 tests, including a 9-million-character document loaded twice without cache retention.
- Candidate review found the estimator adds every array/object child to its temporary deque even when container size alone exceeds the budget. This can duplicate a very wide DOM's references in memory. A deterministic mock-container regression will prove it stops before iterating those children.
- The wide-object and relative-URI suite failed as expected (`Tests run: 11, Failures: 2`); its retained snippet is `initial-evidence.jsonld-wide-object-relative.txt`. Java 26 `ArrayDeque.addAll` invokes `Collection.forEach`, so the array mock now rejects that call. Its isolated red report is `initial-evidence.jsonld-wide-array.txt` (`Tests run: 1, Failures: 1`).
- The final focused security class passed 11/0/0/0 after the estimator checked container size before enqueueing children and direct scheme-less URIs became parse errors. The final complete modern JSON-LD module passed 593/0/0/4.

## Decision Log

- Decision: Use the dependency's public `HttpLoader` constructor with a checked `no.hasmac.jsonld.http.HttpClient` delegate, so every outgoing HTTP(S) request is checked immediately before transport. Direct whitelisted `file:` URLs continue through the existing scheme router; secure mode off remains unrestricted. This is narrower than forking the dependency's redirect parser and covers both redirects and alternate links. Date/Author: 2026-10-23, Codex.
- Decision: Keep a shared cache but key entries by URI, effective security policy, and the request-affecting loader options. Bound the cache by approximate retained document weight as well as count. A separate later PR can define a hard per-document/network byte limit for parsing large responses, which is broader than cache retention. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

Request-time policy enforcement and cache isolation are implemented. The final modern JSON-LD module run passed 593 tests with 0 failures, 0 errors, and 4 existing skips. The new security test class passed 11/0/0/0, including parser-level rejection before the redirected endpoint received a request and bounded estimation of wide containers. The cache remains shared but now separates effective policies and request profiles, holds about 16 MiB of estimated JSON document weight, and does not retain one oversized document. Explicitly whitelisted redirect and alternate chains, allowed local files, secure-mode opt-out, and custom document loader tests pass. A hard per-document response-body byte limit and the ordinary JSON-LD parser's own DOM memory use remain separate work.

## Context and Orientation

The owning module is `core/rio/jsonld`. `core/rio/jsonld/src/main/java/org/eclipse/rdf4j/rio/jsonld/JSONLDParser.java` constructs `CachingDocumentLoader` unless the caller supplied a custom `JSONLDSettings.DOCUMENT_LOADER`. `core/rio/jsonld/src/main/java/org/eclipse/rdf4j/rio/jsonld/CachingDocumentLoader.java` checks `SECURE_MODE` and `WHITELIST` and optionally uses a static Guava cache. `no.hasmac:hasmac-json-ld:0.10.2` supplies `HttpLoader`, `DefaultHttpClient`, and `SchemeRouter`. An alternate link is an HTTP `Link` header that points a non-JSON response to its JSON-LD representation.

`core/rio/jsonld/src/test/java/org/eclipse/rdf4j/rio/jsonld/CachingDocumentLoaderSecurityTest.java` contains loopback HTTP reproductions. The parser's explicit custom document loader remains the caller's opt-out from RDF4J's built-in policy; this plan does not change that API.

## Plan of Work

First extend the regression test to prove cache-enabled behavior, allowed redirects/links, policy isolation, and caller options, retaining a failing Surefire report before production edits. Then route HTTP(S) through a checked client so each actual request URI must be allowed in secure mode. The unchecked scheme router remains for direct allowed file URLs and secure-mode opt-out. Replace URI-only cache keys with immutable effective-policy and option values, and use a document-size-based Guava weight limit to prevent the global cache retaining 1,000 arbitrarily large contexts. Preserve the existing one-hour expiry and at-most-1,000-entry limit.

## Concrete Steps

From this worktree root, the initial offline quick install already passed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

The red test command was:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace jsonld-modern-loader CachingDocumentLoaderSecurityTest --retain-logs

Its first result was `Tests run: 2, Failures: 2, Errors: 0`, with `Expected RDFParseException to be thrown, but nothing was thrown` for both unlisted-target cases. Re-run that command after each implementation step, followed by `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace jsonld-modern-loader core/rio/jsonld --retain-logs`. Preserve each initial red report before a rerun. Run `./checkCopyrightPresent.sh` from `scripts`, the formatter command from `AGENTS.md`, and `git diff --check` before review.

## Validation and Acceptance

The local HTTP regression must show that a secure loader refuses a redirect or alternate link to an unlisted URI before the second endpoint receives a request, with and without caching. It must accept a fully whitelisted chain, and secure-mode opt-out/custom loader behavior must continue. Cache-enabled loading must respect caller options and cannot return data fetched under a different whitelist. A document larger than the cache budget can still be parsed, but is not retained; a normal small context remains cached. The complete `core/rio/jsonld` module tests should pass with no new failures.

## Idempotence and Recovery

The branch is isolated. Maven outputs and red evidence remain untracked/ignored; do not clean them. `mvnf` can be re-run after preserving the first failure report. Stage only reviewed source, test, and this ExecPlan for a commit. If a proposed cache estimator has unacceptable behavior or a dependency API blocks request interception, update this plan before choosing another approach.

## Artifacts and Notes

The first report is `initial-evidence.jsonld-modern-loader.txt`; the full baseline verify log is `.mvnf/workspaces/jsonld-modern-loader/logs/20261023T042900.117329Z-12703-46d11c37/verify.log`. The initial worktree commit is `3d292763558604affd89c7d4256e8df5720bd895` from `main`.

## Interfaces and Dependencies

The required dependency APIs are `no.hasmac.jsonld.loader.HttpLoader(no.hasmac.jsonld.http.HttpClient)`, `no.hasmac.jsonld.http.DefaultHttpClient.defaultInstance()`, and `HttpClient.send(URI, String)`. Guava `CacheBuilder.maximumWeight(...).weigher(...)` sets the shared retention budget. No new dependency is needed.

Revision note (2026-10-23 04:30Z): Created after observing the two red tests and inspecting the redirect implementation; the plan separates request authorization from later per-document byte-limit work.

Revision note (2026-10-23 04:35Z): Added and ran cache isolation, options, and legitimate control tests before editing production code; moved the sole in-progress step to request-boundary implementation.

Revision note (2026-10-23 04:40Z): The request hook and weighted cache passed the initial focused suite after preserving Guava's checked exception contract. Added parser-level and fully whitelisted alternate-link controls, updated setting text, and moved the in-progress step to full verification.

Revision note (2026-10-23 04:46Z): Final module verification passed 590/0/0/4; moved the sole in-progress step to candidate review and publication decision.

Revision note (2026-10-23 04:50Z): Candidate review found a wide-container temporary-memory gap; opened a new in-progress red-first regression step before publication.

Revision note (2026-10-23 04:55Z): Package-visible weight calculation passed same-selection pre/post green at 8/0/0/0 with hit proof through the oversized-document cache test. The new wide-array, wide-object, and relative-URI tests are red; moved the sole in-progress step to the implementation correction.

Revision note (2026-10-23 04:57Z): The 11-test focused class passed with early queue bounds and scheme-less URI error handling; moved the sole in-progress step to final module verification.

Revision note (2026-10-23 05:00Z): Final modern JSON-LD module passed 593/0/0/4 after the wide-container correction; moved the sole in-progress step to candidate review and publication decision.
