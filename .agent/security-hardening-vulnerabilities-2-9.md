# Harden remote access, repository paths, credentials, and decompression

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept current as work proceeds. Maintain this document in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

After this work, an RDF4J server or Workbench deployment can process untrusted requests without revealing host secrets, reaching private network services through RDF URLs, escaping the repository data directory through repository identifiers, exposing reusable credentials to browser script, sharing authenticated remote managers between users, or consuming unbounded resources while decompressing HTTP or RDF input. Existing trusted local `File` and `InputStream` imports remain available. Administrators can deliberately permit named private remote targets, share Workbench encryption keys across a cluster, and tune decompression ceilings through documented configuration.

The result is observable through focused regression tests. Before each behavior-changing production edit, the smallest in-repository Surefire test for that behavior must be added and run to failure. The failing report snippet must be preserved before the production edit, then the identical selection must pass after the edit. Final acceptance requires the affected module suites, root quick install, formatting, copyright checks, generated TypeScript consistency, and a clean audit of the intended diff.

## Progress

- [x] (2026-08-21 00:00Z) Inspected `.agent/PLANS.md`, verified a clean `main` worktree, and mapped the principal server, Workbench, repository, HTTP client, and RDF loader code paths.
- [x] (2026-08-21 00:00Z) Authored this initial self-contained security ExecPlan; no production files were changed.
- [x] (2026-08-21 04:46Z) Ran the mandatory root quick clean install; all reactor modules passed in 26.253 seconds and full output is preserved in `maven-build.log`.
- [x] (2026-08-21 04:50Z) Added and failed two vulnerability #2 tests for controller-model disclosure and JSP disclosure/escaping; preserved both Surefire summaries in `initial-evidence.txt`.
- [x] (2026-08-21 04:53Z) Replaced broad diagnostic models with shared allowlisted view objects, removed sensitive JSP sections, escaped retained string values, and passed both matching methods plus the two-test class.
- [x] (2026-08-21 05:02Z) Added the shared policy and seven focused tests covering special IPv4/IPv6 ranges, mapped addresses, mixed DNS answers, global exceptions, allowlists, URI validation, downgrade, and invalid configuration; all seven pass.
- [x] (2026-08-21 05:06Z) Added the public policy to HTTP configuration and `RDFLoader`; preserved the loopback-loader failure and passed the matching test plus all seven `RDFLoaderTest` methods.
- [x] (2026-08-21 05:10Z) Preserved failing JDK initial-target, redirect-target, and cross-origin credential tests; explicit policy-aware redirects now pass all three security tests.
- [x] (2026-08-21 05:18Z) Preserved Apache initial and redirect failures; the policy-aware HC5 strategy prevents the redirect connection and both focused tests pass.
- [x] (2026-08-21 05:23Z) Preserved default and injectable `SPARQLServiceResolver` failures; private services are rejected before caching and injected policies propagate to owned clients, with both focused tests passing.
- [x] (2026-08-21 06:31Z) Added explicit private-target SPARQL `LOAD`, constant/bound `SERVICE`, and Workbench URL-import coverage; the matching focused tests pass without opening a private connection.
- [x] (2026-08-21 05:33Z) Preserved traversal and read/write/delete symlink failures; one shared repository-ID validator now enforces syntax, normalized containment, and non-symlink repository directories, with all focused methods passing.
- [x] (2026-08-21 05:58Z) Preserved Workbench codec, rotation, cookie, CSRF, session isolation, XSS, and authorization failures; AES-GCM keyrings, strict cookies, server-side identity, session-owned managers, and POST-only server changes now pass their focused suites.
- [x] (2026-08-21 05:58Z) Removed client credential handling and untrusted HTML sinks from TypeScript/XSL and regenerated checked-in JavaScript and source maps with `tools/workbench/compileTypescript.sh`.
- [x] (2026-08-21 06:01Z) Preserved the unbounded HTTP-body failure; one shared request budget now covers streams, readers, forms, and signature detection, maps malformed compression to 400 and limit exhaustion to 413/connection abort, validates configuration at startup, and passes all 12 filter tests.
- [x] (2026-08-21 06:13Z) Preserved the aggregate ZIP-entry failure; public `RDFLoaderSettings` and one recursive per-load budget now enforce bytes, ratio/grace, depth, and entry count, with the seven-test focused budget class green.
- [x] (2026-08-21 06:13Z) Preserved the Workbench pre-decompression failure, removed that bypass, and verified a breached multi-entry import leaves a real MemoryStore empty; all nine `AddServletTest` tests pass.
- [x] (2026-08-21 06:14Z) Documented public-only remote access and allowlists, unsafe-ID offline renaming, credential keyrings and legacy-cookie invalidation, and both decompression budget families in the 6.0.0 release notes.
- [x] (2026-08-21 06:41Z) Ran focused tests, affected module suites, copyright and formatting checks, the root quick clean install, generated-TypeScript consistency checks, and the final diff audit.
- [x] (2026-08-21 06:50Z) Preserved and fixed final nested-symlink deletion and plain-stream ratio regressions; matching focused tests and both owning module suites pass.
- [x] (2026-08-22 05:20Z) Preserved the post-hardening `ProxyTest` failure, injected exact-host policies into synthetic proxy and local MockServer fixtures without changing production defaults, and passed the focused test plus all 96 HTTP client tests.

## Surprises & Discoveries

- Observation: `tools/server-spring/src/main/java/org/eclipse/rdf4j/common/webapp/system/SystemInfoController.java` currently adds all JVM properties and environment variables to its model and exposes `user.name`; `SystemOverviewController` separately exposes the OS user.
  Evidence: `handleRequest` adds `javaProps` and `envVars`, and both nested `ServerInfo` classes read `System.getProperty("user.name")`.

- Observation: the current JDK HTTP backend configures `java.net.http.HttpClient` with automatic redirects. That mechanism does not call RDF4J code before connecting to each redirect target, so it cannot satisfy per-hop address validation.
  Evidence: `core/http/client-jdk/src/main/java/org/eclipse/rdf4j/http/client/jdk/JdkRDF4JHttpClientFactory.java` selects `HttpClient.Redirect.ALWAYS` when redirects are enabled.

- Observation: rebuilding a redirect request from the original default and per-request headers forwards credentials to a different origin unless the redirect loop explicitly strips them.
  Evidence: the preserved `doesNotForwardCredentialsAcrossAuthorityRedirect` failure showed `Authorization: Bearer secret` on the second request; the fixed three-test JDK security class passes.

- Observation: Workbench currently stores Base64 `username:password` in a browser-readable cookie, decodes it in TypeScript, writes the username through `innerHTML`, and mutates one shared `RemoteRepositoryManager` before requests.
  Evidence: `tools/workbench/src/main/webapp/scripts/ts/server.ts`, `template.ts`, and `saved-queries.ts` read or create the cookie; `WorkbenchServlet.setCredentials` decodes it and calls `RemoteRepositoryManager.setUsernameAndPassword`.

- Observation: Workbench servlet caches are global maps keyed by server and repository, not by `HttpSession`. Encrypting cookies alone would not prevent concurrent users from racing credential mutation on a shared manager.
  Evidence: `WorkbenchGateway.servlets` and `WorkbenchServlet.repositories` are instance-wide maps.

- Observation: HTTP decompression support already covers gzip, deflate, Brotli, Zstandard, signature detection, reader access, and form parsing, but the form path calls `readAllBytes()` and no common expansion budget wraps all paths.
  Evidence: `HttpCompressionFilter.CompressedHttpServletRequestWrapper.readDecompressedFormParameters` reads the full expanded body.

- Observation: `RDFLoader` recursively unwraps compression and ZIP entries without carrying shared state, allowing nesting and separate entries to reset any local count.
  Evidence: `core/repository/api/src/main/java/org/eclipse/rdf4j/repository/util/RDFLoader.java` calls `load` recursively from both compressed-stream and ZIP-entry branches.

- Observation: `AbstractRepositoryConnection.add(InputStream, ...)` already creates a local transaction when none is active and rolls it back for any loader `IOException` or runtime parse failure.
  Evidence: the real-MemoryStore Workbench regression breaches the second ZIP entry and observes repository size zero, including when no explicit isolation-level parameter was supplied.

- Observation: the first focused test invocation could not reach Surefire because `.m2_repo` lacked `surefire-junit-platform:3.5.4`. The required single online retry also hit sandbox DNS restrictions, and the approved network retry downloaded the provider successfully. Subsequent focused runs are offline again.
  Evidence: `logs/mvnf/20260821-044827-verify.log`, `logs/mvnf/20260821-044858-verify.log`, and the first valid failing report from `20260821-044933-verify.log`.

- Observation: server, repository-manager, and Workbench integration fixtures intentionally connect to loopback endpoints, which the new production default correctly rejects and the filesystem sandbox can independently block.
  Evidence: the fixtures now scope `org.eclipse.rdf4j.remote.allowedHosts=localhost` to their test lifecycle and restore the prior value; socket-bearing suites were rerun with the required sandbox permission and passed.

- Observation: five older Workbench tests treated a client-controlled `server-user` parameter as authenticated identity, and one expected Workbench to pre-decompress an upload before `RDFLoader`.
  Evidence: after updating only those fixtures to set the authenticated request attribute and expect raw compressed bytes to reach `RDFLoader`, the complete 397-test Workbench suite passed. A separate transform test proves a malicious authenticated username renders as escaped text.

- Observation: the legacy recursive directory helper follows nested directory symlinks, and the first hardened manager implementation checked only the repository root.
  Evidence: `refusesNestedSymbolicLinkDuringRecursiveDeletion` failed before the final patch; the manager now performs a no-follow tree preflight before state mutation and rechecks during deletion, and all 16 manager tests pass.

- Observation: the first recursive RDF budget charged top-level uncompressed input only as expanded data, so a plain stream beyond the grace window appeared to have an infinite ratio.
  Evidence: `acceptsPlainInputAtOneToOneRatio` failed at a configured 1:1 ceiling; composing compressed/source and expanded accounting for plain top-level input fixed the method, and all 48 repository-api tests pass.

- Observation: HTTP client tests use intentionally non-public origins: `ProxyTest` sends `rdf4j.invalid` through MockServer as a forward proxy, while protocol-session tests connect directly to loopback MockServer endpoints.
  Evidence: the default public-only policy correctly rejected both fixture types. Exact-host test policies now preserve the intended proxy/protocol coverage, `ProxyTest` passes 1/1, and the HTTP client module passes 96 tests with 8 skips.

## Decision Log

- Decision: Use Routine D for the whole change, while applying Routine A's failing-test-before-production gate independently to each behavior-changing milestone.
  Rationale: the change crosses public APIs, network IO, redirects, filesystem deletion, cryptography, sessions, concurrency, servlet behavior, and decompression. One monolithic reproduction would obscure regressions and make evidence unusable.
  Date/Author: 2026-08-21 / Codex

- Decision: Put the shared network contract and implementation in `core/common/io` under `org.eclipse.rdf4j.common.net`, with hostname resolution injected into the public-only implementation.
  Rationale: `RDFLoader`, service resolution, Workbench, and both HTTP backends need one policy without depending on server or repository implementation modules. Resolver injection makes mixed-answer and rebinding-adjacent cases deterministic in tests.
  Date/Author: 2026-08-21 / Codex

- Decision: Resolve every hostname on every validation immediately before connection and reject the entire target if any returned address is not globally routable, unless the hostname or address is explicitly allowed.
  Rationale: accepting a target when only one DNS answer is public permits clients to select a private answer. Revalidating redirects and new connections reduces DNS rebinding exposure without pinning stale DNS indefinitely.
  Date/Author: 2026-08-21 / Codex

- Decision: Disable automatic redirects in the JDK backend and run a bounded explicit redirect loop when RDF4J redirect following is enabled. Use an RDF4J-aware redirect strategy or execution interceptor in Apache HC5.
  Rationale: neither backend may connect to a redirect target before policy validation, and HTTPS-to-HTTP downgrade rejection needs both the previous and next URI.
  Date/Author: 2026-08-21 / Codex

- Decision: Treat hostname and CIDR allowlists as narrow exceptions to address-class rejection, not exceptions to URI syntax, scheme, user-info, redirect-count, or TLS-downgrade checks.
  Rationale: an allowlist must restore intentional private-network access without permitting malformed or credential-bearing URLs or weakening transport rules.
  Date/Author: 2026-08-21 / Codex

- Decision: Store Workbench credential managers and repository-servlet caches in an `HttpSession`-owned state object and close that object through session unbinding/invalidation.
  Rationale: credentials are session identity. A global cache keyed only by server or repository cannot be made race-free by synchronizing mutation because requests from different users would still reuse authenticated state.
  Date/Author: 2026-08-21 / Codex

- Decision: Do not migrate legacy Base64 cookies. Delete them and require a fresh POSTed sign-in.
  Rationale: legacy cookie contents are attacker-readable and unauthenticated. Attempting to distinguish or migrate them would preserve the insecure trust boundary.
  Date/Author: 2026-08-21 / Codex

- Decision: Count expanded bytes at the consumer-visible boundary and compressed bytes at the source boundary, applying the ratio only after the configured grace byte count. Share counters across recursive RDF layers and ZIP entries.
  Rationale: each byte must be charged once per relevant dimension, and recursive calls must not create fresh ceilings that allow archive multiplication.
  Date/Author: 2026-08-21 / Codex

- Decision: Do not create commits or change branches unless the user authorizes that Git state change; use `GH-0000` if later asked to commit because no issue number was supplied.
  Rationale: the repository instructions require consent for branch changes and define `GH-0000` as the fallback issue label.
  Date/Author: 2026-08-21 / Codex

## Outcomes & Retrospective

Vulnerabilities #2 through #9 now have production implementations, regression evidence, release documentation, and focused plus affected-module verification. Diagnostic views expose only an allowlist; remote URL access is public-only by default across loaders, HTTP clients, `LOAD`, and `SERVICE`; repository identifiers are validated and contained; Workbench credentials are authenticated-encrypted and session-isolated; HTTP request expansion is bounded; and recursive RDF expansion shares one load budget. The final root quick clean install completed successfully across the reactor. No branch, commit, or push was created.

## Context and Orientation

This is a multi-module Maven repository. Commands in this plan run from `/Users/havardottestad/Documents/Programming/rdf4j7`. Maven must use the workspace-local repository `.m2_repo`. Tests must never use `-am` or `-q`. The preferred test runner is `.codex/skills/mvnf/scripts/mvnf.py`; it refreshes local artifacts before a focused verify and can retain full logs. The top-level `initial-evidence.txt` must preserve the first failing Surefire or Failsafe report for each behavior milestone because later runs overwrite report files. Keep untracked artifacts, including evidence and diagnostics.

Vulnerability #2 is in the Spring server system-information pages. `tools/server-spring/src/main/java/org/eclipse/rdf4j/common/webapp/system/SystemInfoController.java` builds the detailed model. `SystemOverviewController.java` builds the overview model. JSPs live in the `tools/server` web application, principally `tools/server/src/main/webapp/WEB-INF/views/system/info/overview.jsp` and `tools/server/src/main/webapp/WEB-INF/views/system/overview.jsp`. JSTL `c:out` escapes HTML; direct expression output does not provide the required explicit guarantee.

Vulnerabilities #3 and #4 concern server-side request forgery, meaning an attacker-controlled URL causing RDF4J to connect to a private or special network address. `core/common/io` is the lowest shared module and already owns `org.eclipse.rdf4j.common.net`. `core/repository/api/.../RDFLoader.java` loads URL, file, stream, reader, compressed, and ZIP RDF sources. SPARQL `LOAD` reaches it through `core/repository/sail/.../SailUpdateExecutor.java`. Federated `SERVICE` reaches remote endpoints through `core/repository/sparql/.../SPARQLServiceResolver.java` and HTTP session code. Workbench URL imports enter through `tools/workbench/.../commands/AddServlet.java`. HTTP backend configuration is in `core/http/client-api/.../RDF4JHttpClientConfig.java`, with implementations in `core/http/client-jdk` and `core/http/client-apache5`.

A globally routable address is an address intended for public Internet routing. The policy must reject IPv4 and IPv6 unspecified, loopback, link-local, private/site-local, multicast, carrier-grade NAT, documentation/example, benchmarking, reserved, IPv4-mapped private IPv6, and any other range not designated for global unicast. Java's `InetAddress` convenience predicates are necessary but insufficient; explicit prefix tables and mapped-address normalization are required. The URI itself must be absolute HTTP or HTTPS, have a host, have no user-info, and use a valid port. Redirects must also reject HTTPS-to-HTTP downgrade.

Vulnerability #5 concerns using a repository identifier as a filesystem component. `core/repository/api/.../RepositoryConfig.java` owns repository configuration identity. `core/repository/manager/.../LocalRepositoryManager.java` maps that identity below `<base>/repositories`, reads and writes `config.ttl`, initializes repository directories, and deletes repositories. The validator must reject bad syntax before filesystem use, normalize the base and child paths, verify containment, and reject a repository directory that is itself a symbolic link. The containment and symlink checks must be repeated immediately before configuration writes and recursive deletion because an earlier validation can become stale.

Vulnerabilities #6 and #7 span Workbench browser, servlet, and repository-manager state. `WorkbenchGateway.java` selects servers and currently owns a global server-to-servlet cache. `WorkbenchServlet.java` owns a global repository cache and mutates remote manager credentials. `CookieHandler.java` currently emits ordinary servlet cookies. `server.xsl` renders the sign-in form. `template.ts`, `server.ts`, and `saved-queries.ts` currently expose or trust client-side credential identity. `QueryStorage.java`, `SavedQueriesServlet.java`, and `QueryServlet.java` implement saved-query authorization. TypeScript is authoritative; run `tools/workbench/compileTypescript.sh` and never patch generated JavaScript directly.

An authenticated-encryption cookie uses AES-256-GCM to both hide and authenticate fields. Its wire value is `v1.<key-id>.<nonce>.<ciphertext>`, with URL-safe Base64 without padding for binary components. The authenticated additional data is the exact cookie name plus the normalized Workbench context path, separated unambiguously. The encrypted payload contains a versioned binary or length-prefixed representation of normalized server URI, username, password, issue epoch seconds, and expiry epoch seconds. Do not concatenate fields with a delimiter that could occur in user input. Reject unknown key IDs, authentication failures, malformed payloads, server/context mismatch, expiry, issue time unreasonably in the future, and values above conservative field-size limits.

The local keyring belongs in the Workbench application-data directory and contains an active 256-bit AES key plus creation metadata and retired decryption keys. Create files atomically with owner-only permissions before publishing them. Rotate the active local key after 90 days and retain retired keys for the configured credential cookie lifetime plus 24 hours. The property `org.eclipse.rdf4j.workbench.credential-key-file` selects a cluster-shared administrator-managed keyring; shared keyrings are read-only to Workbench and contain one active key plus prior decryption keys. Unsupported format, duplicate IDs, invalid key length, insecure permissions, missing active key, or unreadable content must fail servlet initialization. On platforms where POSIX permissions are unavailable, accept only an owner-readable file after platform-appropriate owner checks can be proven; otherwise fail closed.

Vulnerability #8 is in `tools/server-spring/.../compression/HttpCompressionFilter.java` and `HttpCompressionEncoding.java`. The filter handles explicit `Content-Encoding` and signature auto-detection, then exposes a decompressed `ServletInputStream`, reader, or parsed form. A decompression budget is exceeded when expanded bytes exceed the configured absolute ceiling, or when expanded bytes beyond the grace window exceed the allowed ratio to compressed bytes. Form data has an additional 16 MiB expanded ceiling; total expanded request data defaults to 4 GiB; ratio defaults to 200:1 after 1 MiB. Malformed compressed data maps to HTTP 400. Budget exhaustion maps to HTTP 413 if the response is uncommitted; if committed, propagate an IO failure so the container aborts the connection rather than appending an error document.

Vulnerability #9 is in `RDFLoader`. Its budget defaults are 4 GiB aggregate decompressed bytes, 200:1 expansion ratio after 1 MiB, nesting depth 8, and 50,000 ZIP entries. “Aggregate” means all entries and recursive layers in one top-level load share the same state. Each ZIP entry increments the entry counter before parsing. Each compression wrapper increments depth before opening and decrements it when that recursive branch ends; exceeding the maximum throws a stable `RDFParseException`-compatible error. Explicit local `File` and `InputStream` APIs remain supported but are still subject to archive resource limits when compressed content is detected.

## Plan of Work

First establish the baseline with the required root clean install. Read `.agent/skills/mvnf/SKILL.md` before the first test invocation. Keep exactly one step in progress in both the conversational plan and this `Progress` section. For every following milestone, add the smallest regression test, run only that test, capture the Surefire report and a compact failure snippet in `initial-evidence.txt`, and only then edit production code. After the fix, rerun the exact selection and append the green report snippet to the living plan's Artifacts section.

For diagnostic disclosure, extend or add tests under `tools/server-spring/src/test/java/org/eclipse/rdf4j/common/webapp/system` that instantiate both controllers and assert their models do not contain environment/property collections or OS-user data. Add rendering coverage in `tools/server` that supplies characters such as `<`, `>`, `&`, quotes, and apostrophes for each retained value and asserts escaped output. Remove `javaProps`, `envVars`, and `user` from controller models and nested data classes. Retain only application name/version, Java vendor/VM/version, OS name/version/architecture, and aggregate used/maximum memory. Replace all retained JSP interpolation with `c:out`.

For the network primitive, add `RemoteResourceAccessPolicy`, `HostnameResolver`, and `PublicNetworkAccessPolicy` under `core/common/io/src/main/java/org/eclipse/rdf4j/common/net`. The policy interface must validate an initial `URI` and a redirect transition and return normally only when connection is allowed. The implementation loads `org.eclipse.rdf4j.remote.allowedHosts` and `org.eclipse.rdf4j.remote.allowedCidrs`, trims comma-separated entries, canonicalizes DNS hostnames with IDNA and lower-case rules, normalizes IP literals, parses IPv4 and IPv6 CIDRs without DNS, and fails construction on every invalid entry. Exact hostname exceptions apply to that hostname's current answers; exact IP and CIDR exceptions apply to matched addresses. Do not implement suffix or wildcard matching. Add exhaustive tests under `core/common/io/src/test/java/org/eclipse/rdf4j/common/net` for URI syntax, schemes, user-info, ports, address classes, mixed answers, mapped IPv6, redirects, downgrade, exact exceptions, CIDR boundaries, and malformed properties.

For network integration, add overloads to `RDFLoader` and `SPARQLServiceResolver` that accept a `RemoteResourceAccessPolicy`, while existing constructors create the default public-only policy. Preserve trusted `File`, `InputStream`, and `Reader` entry points. Ensure URL loads validate immediately before opening and validate every redirect rather than relying on `URLConnection` automatic redirects. Make SPARQL `LOAD`, constant and bound `SERVICE`, Workbench URL import, and remote repository paths inherit the policy. Add the policy to `RDF4JHttpClientConfig` with `Builder.remoteResourceAccessPolicy(...)`, getter, copy-builder support, and a public-only default. In the JDK backend, configure redirect handling to `NEVER` and explicitly rebuild repeatable requests through at most `maxRedirects`, validating before every send and rejecting downgrade. In Apache HC5, validate the initial resolved request and each redirect before route connection; preserve repeatable-body semantics and existing redirect status behavior. Add focused initial-target and redirect-chain tests in `core/http/client-jdk` and `core/http/client-apache5`, plus integration tests for URL loader, Sail update `LOAD`, service resolver, and Workbench add servlet. Use injected resolvers and loopback test servers only through explicit test policies or allowlists so tests do not weaken production defaults.

For repository IDs, add a public or package-shared `RepositoryIdValidator` in `core/repository/api/src/main/java/org/eclipse/rdf4j/repository/config` for syntax, and manager-side path methods that combine validation with filesystem containment and link checks. `RepositoryConfig.setID` may retain assignment compatibility, but `validate` and all manager entry points must reject null/blank, `.`/`..`, any dot segment, control and NUL characters, slash, backslash, absolute, UNC, and drive-prefixed values. Permit ordinary Unicode identifiers that do not contain those constructs. In `LocalRepositoryManager`, resolve `<base>/repositories` and the candidate to normalized absolute paths, require the candidate's parent relationship to the base, and reject an existing candidate directory if `Files.isSymbolicLink(candidate)`. Recheck before `config.ttl` writes and recursive deletion. Directory enumeration must skip or report unsafe legacy names without migrating them. Tests cover Unix and Windows spellings independent of host OS, normalization boundaries, valid Unicode, symlink escape, reads, configuration replacement, and removal.

For Workbench credentials, create small focused classes rather than growing gateway servlets: a credential value object, AES-GCM codec, keyring parser/store, and per-session Workbench state under `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/proxy` or a dedicated `security` subpackage. Use `SecureRandom`, `SecretKeySpec`, `Cipher.getInstance("AES/GCM/NoPadding")`, 96-bit nonces, 128-bit authentication tags, and constant-time library authentication. Never log credentials or decrypted payloads. Initialize the keyring in `WorkbenchGateway.init`; fail initialization on invalid configuration. Set credential cookies through explicit `Set-Cookie` construction or Servlet cookie APIs capable of `HttpOnly`, `SameSite=Strict`, context path, 30-day max age, and conditional `Secure`. Clear legacy values with max age zero. When a retired key decrypts a valid cookie, issue a new active-key cookie in the response.

Replace global credential-bearing cache ownership with a session state object stored under a private request-session attribute. Key managers by normalized server within that one session, and key repository servlets below their manager. When credentials change, close the previous manager and repository servlets before replacement. Implement `HttpSessionBindingListener` or an equivalent lifecycle hook so expiry/invalidation closes all managers. Do not mutate credentials on shared instances. The change-server endpoint serves the form on GET but accepts state change only on POST. Generate a cryptographically random per-session CSRF token, emit it through escaped XML/XSL metadata, compare it to the posted token in constant time, and reject missing or mismatched tokens with HTTP 403 without changing cookies or state.

Post `server-user` and `server-password` normally from `server.xsl`; remove browser Base64 conversion and all credential-cookie reads from TypeScript. Expose only the authenticated username as an escaped XML binding populated from server-side session state. Render it with `xsl:value-of`, using markup chosen in the stylesheet for the unauthenticated state rather than inserting HTML text. Saved-query delete/edit permission is computed server-side from the authenticated request/session attribute. Ignore or reject client-supplied `server-user` for authorization. Replace application-owned `innerHTML` uses fed by untrusted or cookie-derived content with `textContent`, form values, DOM nodes, or server-rendered escaped XSL. Regenerate `server.js`, `template.js`, `saved-queries.js`, and their maps using `tools/workbench/compileTypescript.sh`.

For HTTP decompression, introduce a reusable counting/budget stream in `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/compression`. It must count raw bytes below the codec and expanded bytes above the codec, check with overflow-safe arithmetic on every bulk or single-byte read, and throw distinct checked exceptions for malformed content and budget exhaustion. One request budget instance must be reused by explicit encodings, auto-detection, `getInputStream`, `getReader`, and form parsing. Parse filter init parameters and matching JVM properties at filter initialization, reject non-numeric, non-positive, inconsistent, overflowed, NaN, or infinite values, and document precedence. Use defaults of 16 MiB form expanded bytes, 4 GiB total expanded bytes, 200 ratio, and 1 MiB grace. Replace form `readAllBytes()` with bounded incremental reading. Catch failures around the full downstream chain so status mapping happens before commit when possible.

For recursive RDF input, add `RDFLoaderSettings` under `core/repository/api/src/main/java/org/eclipse/rdf4j/repository/util` with typed `RioSetting` constants for maximum aggregate expanded bytes, maximum expansion ratio, ratio grace bytes, maximum nesting depth, and maximum ZIP entries. Use property keys beginning `org.eclipse.rdf4j.rio.loader.` and defaults 4 GiB, 200, 1 MiB, 8, and 50,000. Reject invalid configured values before consuming input. Add an internal per-top-level-load budget and pass it through every recursive load call. Wrap compressed source and expanded streams so ratio accounting cannot be reset. Count ZIP entries globally and charge all entry output to the same aggregate counter. Keep stable public error text prefixes such as `RDF input decompression limit exceeded:` followed by the limit name; do not expose local paths or secrets.

Remove Workbench's separate pre-decompression step in `AddServlet` so every uploaded and URL source enters `RDFLoader` once and shares its full budget. Add repository-level tests that begin with an empty repository, attempt a multi-entry archive whose later entry breaches a limit, observe the stable parsing failure, and assert the repository remains empty after rollback. Cover exact boundary success and one-byte/one-entry/one-level excess, unknown lengths, multi-entry aggregate exhaustion, nested gzip/ZIP combinations, closure of every stream on success and failure, and optional Brotli/Zstandard cases when their codecs are available.

Finally update `site/content/release-notes/6.0.0.md` only if this checkout's release-note policy still targets 6.0.0; otherwise add the next appropriate release note after inspecting current project convention. Document the two remote allowlist properties, default public-only behavior, repository-ID incompatibility and offline rename procedure, keyring property and format, one-time credential-cookie invalidation, HTTP filter parameters/properties, RDF loader settings, defaults, and lowering limits. Run the final validation described below and update all living sections with actual evidence and deviations.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j7`.

Run the mandatory initial build with at least a 60-second timeout:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

If it fails because an artifact is missing offline, rerun the identical command once without `-o`, then return to offline mode. For any other failure, rerun without `-T 1C` and record the discovery.

Before the first test, read the test-runner skill:

    sed -n '1,260p' .codex/skills/mvnf/SKILL.md

For each new regression, run the narrowest class or method and retain logs. Examples below are selectors, not a license to combine unrelated tests:

    python3 .codex/skills/mvnf/scripts/mvnf.py SystemInfoControllerTest#doesNotExposeHostSecrets --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PublicNetworkAccessPolicyTest#rejectsMixedPublicAndPrivateDnsAnswers --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py RDFLoaderTest#rejectsRedirectToPrivateAddress --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LocalRepositoryManagerTest#rejectsSymlinkRepositoryDirectory --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py CredentialCookieCodecTest#rejectsTampering --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py HttpCompressionFilterTest#rejectsExpandedBodyAboveLimit --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py RDFLoaderTest#sharesBudgetAcrossNestedZipEntries --retain-logs

Immediately after each intended failing run, preserve the actual report before any production edit. Use `scripts/agent-evidence.py` to extract compact evidence to a temporary file, then add that text to `initial-evidence.txt` with `apply_patch` so prior milestones are retained. Never overwrite earlier failure sections. Each evidence section records command, report path, and 1–30 lines showing the failure summary.

After the production fix, rerun the identical selector and capture its passing report in this document's Artifacts section. Then broaden to the owning test class and module. The likely affected modules are `core/common/io`, `core/http/client-api`, `core/http/client-jdk`, `core/http/client-apache5`, `core/http/client`, `core/rio/api`, `core/repository/api`, `core/repository/manager`, `core/repository/sail`, `core/repository/sparql`, `tools/server-spring`, `tools/server`, and `tools/workbench`. Resolve the exact `mvnf` module selector from the script rather than guessing if discovery reports ambiguity.

Before finalizing Java or XML changes, run:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command uses `-q` only for formatting, never for tests. After TypeScript edits run:

    tools/workbench/compileTypescript.sh
    git diff --exit-code -- tools/workbench/src/main/webapp/scripts '*.generated-if-applicable'

Do not use that illustrative final path glob literally if it does not exist. Instead inspect `git diff -- tools/workbench/src/main/webapp/scripts` and verify generated JavaScript and maps correspond exactly to TypeScript changes.

Finish with the root quick clean install command above, focused module suites through `mvnf --retain-logs`, `git status --short`, `git diff --check`, and a full intended diff review. Do not delete untracked evidence or diagnostics.

## Validation and Acceptance

Diagnostic acceptance is met when the detailed and overview system pages contain only application name/version, Java vendor/VM/version, OS name/version/architecture, and aggregate memory. Tests prove that environment variables, arbitrary JVM properties, user name, and application data paths are absent, and malicious retained values render as text rather than markup.

Network acceptance is met when the default policy allows public HTTP(S) targets and rejects every special/non-global address class, mixed DNS answers, user-info, malformed or non-HTTP URIs, and HTTPS-to-HTTP redirects. Exact hostname, exact IP, IPv4 CIDR, and IPv6 CIDR exceptions restore only the configured target. Invalid allowlist configuration prevents initialization. URL RDF import, SPARQL `LOAD`, constant and bound `SERVICE`, Workbench URL import, JDK HTTP, and Apache HTTP all reject a private initial target and a public-to-private redirect before connection. Explicit `File` and `InputStream` imports still pass.

Repository acceptance is met when unsafe IDs are rejected consistently by configuration and manager APIs on any operating system spelling; valid Unicode IDs work; symlink repository directories cannot be read, written, or deleted through the manager; and a legacy unsafe directory is not migrated. Errors identify the invalid repository ID without exposing unrelated filesystem layout.

Workbench acceptance is met when browser script cannot read credentials, cookie values have the versioned AES-GCM format, tampering and expiry are rejected, old keys decrypt only within retention and trigger re-encryption, external keyrings validate strictly, and cookie attributes are `HttpOnly`, `SameSite=Strict`, context-scoped, 30 days, and conditionally `Secure`. A legacy cookie is cleared and cannot authenticate. Malicious usernames render as text. Saved-query permission comes from server-authenticated session state. Two simultaneous sessions using the same server but different credentials keep distinct managers. Credential changes and session invalidation close old resources. GET cannot change servers, and POST without the correct session CSRF token returns 403.

HTTP decompression acceptance is met when gzip, deflate, Brotli, and Zstandard, where available, pass at and below limits and fail above them for explicit encoding, detected signatures, streams, readers, chunked/unknown-length input, and forms. The form-specific 16 MiB limit and total 4 GiB limit are distinct. Ratio enforcement begins after 1 MiB. Bad compressed data returns 400; limit exhaustion returns 413 before commit or aborts an already committed connection. Invalid configuration fails filter initialization.

RDF archive acceptance is met when all recursive compression layers and ZIP entries share aggregate bytes, ratio, depth, and entry counts. Exact boundaries pass and the next byte, ratio unit, nesting level, or entry fails with a stable parsing error. Streams close on every path. Workbench no longer pre-decompresses. A failed import leaves no partial statements in the repository.

Final acceptance requires zero new failures in every affected module suite, a successful root quick install, successful copyright and formatting checks, consistent generated TypeScript artifacts, and no unintended tracked or untracked changes. Report focused green validation separately from any broad suite that was blocked, aborted, or not run.

## Idempotence and Recovery

The build, focused tests, formatter, copyright checker, and TypeScript compiler are safe to rerun. The local keyring tests must use temporary directories and deterministic clocks; they must never read or alter a developer's real application-data directory. Network tests must use injected resolvers and local ephemeral servers with explicit test policies, never external DNS or cloud metadata endpoints.

If production code is edited before its failing behavior test and preserved report, stop immediately, revert only that known patch with `apply_patch`, set the sole in-progress step back to the missing reproduction, and resume. Never use `git reset`, `git restore`, `git clean`, or delete untracked artifacts. If an unexpected working-tree change appears, treat it as user-owned, avoid overlapping it, and stop for direction only if it prevents safe progress.

When a focused test fails for a reason unrelated to the intended assertion, fix the test fixture or narrow the selector before touching production. When a dependency is unavailable offline, follow the single online retry rule. When a long module run is active, leave it running in the background and report progress at least every 60 seconds.

Filesystem race resistance is necessarily bounded by Java APIs. Recheck normalized containment and symbolic-link state at the last possible point before writes/deletion. Prefer `SecureDirectoryStream` for recursive deletion when the provider supports it; otherwise fail closed on any encountered symbolic link and document the remaining platform limitation in `Surprises & Discoveries` rather than silently weakening checks.

## Artifacts and Notes

Planning boundary:

    Command: git status --short --branch
    Output: ## main...origin/main

Baseline build:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    Output: BUILD SUCCESS; Total time 26.253 s; finished 2026-08-21T06:46:50+02:00

No production changes existed when this plan was authored. Add compact pre-fix and post-fix Surefire/Failsafe snippets here as milestones complete. Keep the canonical pre-fix copies in top-level `initial-evidence.txt` and full Maven logs under `logs/mvnf` or the runner-selected log directory.

Vulnerability #2 pre-fix evidence:

    Controller selection: tests=1, failures=1, errors=0, skipped=0; unexpected model keys javaProps and envVars.
    JSP selection: tests=1, failures=1, errors=0, skipped=0; application data directory remained rendered and retained strings were direct expressions.

Vulnerability #2 post-fix evidence:

    Controller selection: tests=1, failures=0, errors=0, skipped=0, time=0.431s.
    JSP selection: tests=1, failures=0, errors=0, skipped=0, time=0.041s.
    Full SystemInformationSecurityTest: tests=2, failures=0, errors=0, skipped=0, time=0.411s.

Final affected-module evidence:

    core/common/io: tests=109, failures=0, errors=0, skipped=0.
    core/http/client-api: tests=15, failures=0, errors=0, skipped=0.
    core/http/client-jdk: tests=3, failures=0, errors=0, skipped=0.
    core/http/client-apache5: tests=2, failures=0, errors=0, skipped=0.
    core/repository/api: tests=48, failures=0, errors=0, skipped=0.
    core/repository/manager: tests=16, failures=0, errors=0, skipped=0.
    core/repository/sail: tests=13, failures=0, errors=0, skipped=0.
    core/repository/sparql: tests=31, failures=0, errors=0, skipped=0.
    tools/server-spring: tests=115, failures=0, errors=0, skipped=0.
    tools/server: tests=39, failures=0, errors=0, skipped=1.
    tools/workbench: tests=397, failures=0, errors=0, skipped=0.
    Constant and bound SERVICE evaluation: tests=1, failures=0, errors=0, skipped=0.
    Malicious-username XSL transform: tests=1, failures=0, errors=0, skipped=0.
    core/http/client follow-up: tests=96, failures=0, errors=0, skipped=8.

Final repository checks:

    TypeScript compiler: generated JavaScript/map diff hash unchanged before and after regeneration.
    Copyright checker: all files have valid copyright headers and SPDX lines.
    Formatter: process-resources completed successfully.
    Diff hygiene: git diff --check completed with no output.
    Root quick clean install: BUILD SUCCESS; total time 26.086 s; finished 2026-08-21T08:50:21+02:00.

## Interfaces and Dependencies

In `core/common/io`, provide these public concepts with final names adjusted only if existing naming conventions require it:

    public interface RemoteResourceAccessPolicy {
        void checkInitial(URI target) throws IOException;
        void checkRedirect(URI source, URI target) throws IOException;
    }

    @FunctionalInterface
    public interface HostnameResolver {
        InetAddress[] resolve(String hostname) throws UnknownHostException;
    }

    public final class PublicNetworkAccessPolicy implements RemoteResourceAccessPolicy {
        public PublicNetworkAccessPolicy();
        public PublicNetworkAccessPolicy(HostnameResolver resolver);
    }

The no-argument implementation reads `org.eclipse.rdf4j.remote.allowedHosts` and `org.eclipse.rdf4j.remote.allowedCidrs` once and fails closed on invalid values. Provide an explicit factory or constructor taking parsed allowlists for deterministic tests if that avoids global-property mutation.

In `RDF4JHttpClientConfig`, provide:

    public RemoteResourceAccessPolicy getRemoteResourceAccessPolicy();
    public Builder remoteResourceAccessPolicy(RemoteResourceAccessPolicy policy);

`defaultConfig`, `newBuilder`, and `toBuilder` must preserve a non-null public-only default. Both HTTP factories and clients must honor it for every connection and redirect.

In `RDFLoader` and `SPARQLServiceResolver`, retain source-compatible existing constructors and add overloads accepting `RemoteResourceAccessPolicy`. The existing constructors delegate to the public-only policy. Prefer constructor injection over mutable setters so a request cannot race policy replacement.

In `RDFLoaderSettings`, expose immutable typed `RioSetting` instances. Exact Java types are `Long` for byte counts, `Double` for ratio if fractional administrative values are accepted or `Long` if the implementation deliberately supports integer ratios only, and `Integer` for depth and entry count. Defaults and property keys are part of the public compatibility contract. Values come first from `ParserConfig`, then the existing `RioConfig` system-property mechanism, then defaults.

No new cryptography dependency is required; use the JDK cryptography APIs. No new network-range dependency should be introduced unless an implementation review proves the standard library cannot meet correctness and a quick dependency health/adoption review is recorded. Existing Brotli and Zstandard optional support remains in place. Servlet APIs, Spring test support, JUnit, AssertJ, and Mockito already used by the owning modules should be reused.

Revision note (2026-08-21): created the initial ExecPlan after repository mapping. It resolves redirect handling, allowlist scope, session cache ownership, cookie migration, and shared-budget semantics up front because those decisions determine the safe test and implementation order.

Revision note (2026-08-21): recorded the successful mandatory baseline build and advanced the sole active implementation step to the first vulnerability #2 reproduction.

Revision note (2026-08-21): completed vulnerability #2 with matching red/green evidence. The implementation uses shared allowlisted view objects so future JSP edits cannot accidentally regain access to `AppConfiguration.dataDir`, arbitrary properties, environment variables, or the process user.
