# Expand automatic RDF input decompression

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

RDF4J server and Workbench users can currently upload only a small set of automatically decompressed inputs, and archive handling is limited to ZIP. After this work, the same upload paths accept BZip2, XZ, LZMA, framed LZ4, framed Snappy, Unix compress (`.Z`), and TAR, including nested combinations such as `.tar.gz`, while retaining gzip, zlib/deflate, Brotli, Zstandard, and ZIP. A mixed-format archive is imported member by member in one transaction, and any member failure rolls the whole upload back.

The change is deliberately input-only. HTTP `Content-Encoding` negotiation and response compression remain restricted to the existing `RioCompression` enum values. Core modules discover Apache Commons Compress reflectively, so embedded RDF4J applications keep their current dependency graph and behavior when the optional libraries are absent. The server distributions and Workbench package the optional libraries and therefore expose the expanded behavior.

## Progress

- [x] (2026-08-29 21:00Z) Inspected current compression, loader, Workbench, HTTP filter, packaging, and tests.
- [x] (2026-08-29 21:00Z) Completed JDK 25 root quick clean install; `maven-build.log` records `BUILD SUCCESS` in 29.200 seconds.
- [x] (2026-08-29 21:06Z) Added focused BZip2-signature and plain-TAR tests; both fail on the unsupported behavior and are preserved in `initial-evidence.txt`.
- [x] (2026-08-29 21:18Z) Implemented reflective codec decoding, the typed decoder limit, recursive ZIP/TAR dispatch, draining, and shared archive budgets; focused Rio and dispatcher tests pass.
- [x] (2026-08-29 21:27Z) Delegated `RDFLoader` and Workbench stream upload parsing to the dispatcher; mixed-member commit and rollback tests pass.
- [x] (2026-08-29 21:37Z) Added runtime/test dependency placement, controller/filter limit mapping, Zstandard-TAR coverage, and user-facing documentation.
- [x] (2026-08-29 21:44Z) Extended Workbench URL imports through the dispatcher and reran the focused servlet class: 20 tests passed.
- [x] (2026-08-29 21:53Z) Completed formatting, copyright, packaged-WAR inspection, and retained-log verification for all six requested modules with zero failures or errors.
- [x] (2026-08-29 21:56Z) Completed the final JDK 25 root quick clean install (`BUILD SUCCESS`, 27.407 seconds) and clean diff audit.
- [x] (2026-08-29 22:28Z) Reopened verification to exercise every supported codec and archive form through the WAR server, Workbench upload servlet, and Spring Boot HTTP server.
- [x] (2026-08-29 22:29Z) Captured a fresh JDK 25 root clean-install baseline; `maven-build.log` records `BUILD SUCCESS` in 26.153 seconds.
- [x] (2026-08-29 22:38Z) Added the shared 18-case input matrix and exercised it through the WAR server, Workbench Add servlet with a real repository, and Spring Boot; initial focused reports were preserved before the production dependency change.
- [x] (2026-08-29 22:38Z) Added missing Brotli/native and Zstandard runtime libraries to the standalone Workbench WAR; the identical Workbench selector now passes 18 tests and the dependency-placement selector passes.
- [x] (2026-08-29 22:43Z) Reran full server-spring (128), Workbench (407), WAR server (57 with one platform skip), and Spring Boot (45) verification with zero failures or errors; inspected all three packaged applications for every decoder library and Brotli native artifact.
- [x] (2026-08-29 22:46Z) Rechecked formatting, all three packaged applications, and the complete diff; the final JDK 25 root quick clean install passed in 26.296 seconds.

## Surprises & Discoveries

- Observation: `RDFLoader` already drains unread ZIP entry tails and shares byte/depth accounting across recursive calls.
  Evidence: `core/repository/api/src/main/java/org/eclipse/rdf4j/repository/util/RDFLoader.java` calls `drain(entryStream)` in a `finally` block and passes one `RDFInputDecompressionBudget` through recursion.
- Observation: `RioCompression` enum membership also defines supported HTTP content codings.
  Evidence: `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/HttpCompressionEncoding.java` builds request and response encodings from existing `RioCompression` values; new enum constants would unintentionally change the protocol surface.
- Observation: Apache Commons Compress 1.28.0 needs Commons IO for TAR stream support and accepts an optional decoder-memory limit for XZ, LZMA, and Unix compress.
  Evidence: locally inspected 1.28.0 APIs expose `CompressorStreamFactory(int memoryLimitInKb)` and TAR classes reference Commons IO bounded streams.
- Observation: the first baseline invocation failed before Maven because the reduced PATH omitted `/opt/homebrew/bin`; no code was touched.
  Evidence: the corrected command used `/opt/homebrew/bin/mvn` and completed successfully under Temurin 25.0.1.
- Observation: the repository loader currently passes a null format directly to `Rio.createParser` for an InputStream, producing a null-key failure before it can inspect a TAR member name.
  Evidence: `RDFLoaderTest.loadsTurtleFromTarArchive` fails at `RDFLoader.loadInputStreamOrReader` with `Cannot invoke Object.hashCode() because key is null`.
- Observation: server-spring imports Commons IO directly, so declaring Commons IO with Maven `runtime` scope removes a required compile-time classpath entry even though the dependency is also packaged at runtime.
  Evidence: the first Workbench selector stopped in the root build with missing `IOUtils` symbols in three server-spring controllers; restoring default compile scope made the build and selector run.
- Observation: the HTTP auto-detection wrapper retained the typed decoder limit as a nested cause but classified the outer malformed-compression wrapper first.
  Evidence: `HttpCompressionFilterTest.mapsDecoderMemoryLimitToPayloadTooLarge` initially received HTTP 400 instead of 413; prioritizing typed limit causes made the focused and full filter tests pass.
- Observation: controller-level parser error translation discarded the typed decoder-limit classification.
  Evidence: `TestStatementsController.shouldMapDecoderMemoryLimitToPayloadTooLarge` initially received status 400; both RDF upload controllers now recognize the nested typed cause and return 413.
- Observation: repository, Workbench, WAR, and Spring Boot test suites bind local ports and cannot complete inside the restricted filesystem/network sandbox.
  Evidence: their first broad attempts failed only with `java.net.SocketException: Operation not permitted`; identical permitted reruns completed with zero failures and errors.
- Observation: the requested dependency versions are current project releases and fit the runtime baseline.
  Evidence: Apache lists Commons Compress 1.28.0 as its current Java 8+ feature/maintenance release, and Tukaani lists XZ for Java 1.12 as the current Java 8-compatible release fixing a significant 1.10/1.11 bug.
- Observation: packaged-entry-point coverage is narrower than dispatcher coverage.
  Evidence: `ProtocolIT` and `Rdf4jServerWorkbenchApplicationTest` each upload only BZip2, while `AddServletCoverageTest` covers gzip and TAR but not the other codecs.
- Observation: the standalone Workbench artifact did not package the retained Brotli and Zstandard decoders even though its documentation claimed those upload formats.
  Evidence: the new Workbench matrix failed with `Compression library is not available: com.github.luben.zstd.Zstd`, and the dependency-placement test listed every Brotli artifact plus `zstd-jni` as missing. The unchanged WAR server and Spring Boot matrices both passed all 18 cases.

## Decision Log

- Decision: Introduce `RDFInputDispatcher` in `core/rio/api` as an `@InternalUseOnly` API, with Apache Commons classes reached only through reflection.
  Rationale: both repository loading and Workbench need the same recursive member semantics, while core must not expose new required runtime dependencies.
  Date/Author: 2026-08-29 / Codex
- Decision: Keep `RioCompression` enum values and `HttpCompressionEncoding` unchanged.
  Rationale: the feature concerns RDF request bodies and uploaded files, not additional HTTP content codings or response negotiation.
  Date/Author: 2026-08-29 / Codex
- Decision: Keep JDK `ZipInputStream` for ZIP and use Commons Compress only for TAR and additional codecs.
  Rationale: this preserves existing ZIP behavior when optional dependencies are absent and limits the reflective integration surface.
  Date/Author: 2026-08-29 / Codex
- Decision: Use a shared archive-entry count for ZIP and TAR, while continuing to honor the existing ZIP-specific setting for ZIP entries.
  Rationale: `MAX_ARCHIVE_ENTRIES` protects mixed/nested archives as a whole without weakening configurations that already set a smaller ZIP limit.
  Date/Author: 2026-08-29 / Codex
- Decision: Identify codec signatures where the format has a stable signature and use suffixes for formats without reliable signature detection, while stripping query and fragment text only for suffix matching.
  Rationale: non-markable streams and URL-like source names remain usable, and filename case/query/fragment handling remains predictable.
  Date/Author: 2026-08-29 / Codex
- Decision: Keep Commons IO at default compile scope in server-spring and runtime scope in Workbench.
  Rationale: server-spring production code already directly imports Commons IO, while both scopes package the library for server deployment; Workbench only needs it reflectively at runtime.
  Date/Author: 2026-08-29 / Codex
- Decision: Prove the same named format matrix at every packaged ingestion boundary, using a filename-bearing archive member for codecs such as LZMA that cannot be identified reliably from a bare request body.
  Rationale: direct server requests have no source filename, so reliable signatures should be tested directly while suffix-only detection must be exercised through ZIP or TAR member names; Workbench uploads can test both because their submitted filename is available.
  Date/Author: 2026-08-29 / Codex

## Outcomes & Retrospective

Implementation and endpoint acceptance verification are complete. The dispatcher covers the six added codecs, recursive ZIP/TAR traversal, mixed member formats, fallback selection, unread tails, nesting, archive-entry limits, and decoder-memory limits. RDFLoader, Workbench stream and URL uploads, the HTTP filter, and both RDF upload controllers are integrated. A shared 18-case matrix now proves plain RDF, every supported compression codec, ZIP/TAR, and all required compressed-TAR forms through the WAR server, Workbench Add servlet, and Spring Boot HTTP server. The matrix exposed and fixed the standalone Workbench WAR's missing Brotli and Zstandard runtime libraries. All focused selectors and the full server-spring (128 tests), Workbench (407), WAR server (57 with one platform skip), and Spring Boot (45) suites passed, as did formatting, copyright, packaged dependency inspection, and the final JDK 25 root quick clean install. Existing `RioCompression` enum membership and HTTP content-coding negotiation remain unchanged, and core has no new non-test Commons/XZ dependencies.

## Context and Orientation

`core/rio/api/src/main/java/org/eclipse/rdf4j/rio/helpers/RioCompression.java` is the current low-level codec detector. It recognizes gzip, Zstandard, and zlib signatures, detects gzip, Brotli, Zstandard, and deflate suffixes, and reflectively loads optional Brotli/Zstandard implementations. Its enum values are also consumed by the server HTTP compression layer, so they must not be expanded.

`core/repository/api/src/main/java/org/eclipse/rdf4j/repository/util/RDFLoader.java` currently buffers streams, recognizes ZIP, recursively unwraps a codec through `RioCompression`, and parses one terminal stream. `RDFInputDecompressionBudget` and `RDFLoaderSettings` in that package enforce expanded-byte, ratio, nesting-depth, and ZIP-entry limits. A container entry means one member inside ZIP or TAR. A terminal stream means a stream that is no longer a supported codec or container and is ready for an RDF parser.

`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/AddServlet.java` owns the upload transaction. It currently decompresses a source by filename once and then invokes `RepositoryConnection.add` once. It must instead use the shared dispatcher and invoke `add` for each recognized terminal archive member without committing between members. This matters for `HTTPRepositoryConnection`, where each call becomes a separate server request but all calls still belong to the same Workbench transaction.

`tools/server-spring` supplies server-side compression dependencies. `tools/server` consumes it for the WAR, and `tools/server-boot` consumes both server-spring and Workbench artifacts. Runtime dependencies therefore belong directly in `tools/server-spring/pom.xml` and `tools/workbench/pom.xml`. Core POMs may include them only with test scope so tests can generate and decode fixtures.

## Plan of Work

First, add test-scoped dependency management and focused tests without changing production behavior. Extend `RioCompressionTest` or add a dispatcher-focused test in `core/rio/api` that covers every new signature and suffix, short and non-markable streams, malformed and concatenated data, missing-library behavior, and decoder-memory exhaustion. Add repository loader tests that create TARs and compressed TAR aliases, mix RDF syntaxes, include unknown and special entries, nest ZIP and TAR, leave unread entry tails, and exceed depth and aggregate entry limits. Run the smallest codec and TAR selectors while the production code is unchanged; both must fail for the expected unsupported behavior. Immediately create root `initial-evidence.txt` from Surefire reports before later runs overwrite them.

Next, add `RDFInputDispatcher` to `core/rio/api`. Its constructor accepts `ParserConfig`; its `dispatch(InputStream, String, RDFFormat, EntryConsumer)` method recursively unwraps supported codecs and traverses ZIP/TAR. `EntryConsumer` accepts the terminal stream, source member name, and detected or fallback `RDFFormat` and may throw RDF4J checked exceptions. Detection buffers enough prefix bytes for all signatures, never assumes mark support from callers, preserves the original stream bytes on short inputs, and uses a source name stripped of URL query and fragment components for case-insensitive suffix matching.

The dispatcher uses existing JDK gzip/zlib/ZIP implementations and current Brotli/Zstandard support. A small reflective Commons adapter constructs BZip2, XZ, LZMA, framed LZ4, framed Snappy, Unix compress, and TAR streams without linking production bytecode to Commons types. Configure the adapter with `org.eclipse.rdf4j.rio.compression.maxDecoderMemoryKiB`, defaulting to 65,536 KiB. Translate library memory-limit failures into a public typed RDF4J exception carrying limit metadata so HTTP exception mapping can classify it as a decompression-limit response rather than malformed RDF or an internal server error.

Move the recursive budget needed by both modules into the Rio dispatcher layer or expose a small internal equivalent there. Add `RDFLoaderSettings.MAX_ARCHIVE_ENTRIES`, default 50,000, and retain `MAX_ZIP_ENTRIES`. Count every ZIP and TAR entry against the aggregate limit; additionally apply the ZIP-specific setting to ZIP entries exactly as before. Always drain an entry after the consumer returns or throws so compressed and expanded byte counters advance monotonically. Skip TAR directories and special files. Skip terminal regular members whose name does not identify RDF when no fallback is present, but recurse into recognized codecs and containers. If an archive traversal produces no RDF terminal member, throw a clear parse exception. An explicit fallback format applies to unnamed or otherwise unknown regular members.

Then simplify `RDFLoader` to create the dispatcher with its `ParserConfig` and parse every terminal callback using its existing RDF handler and base URI semantics. Change `AddServlet` to use the same dispatcher and call `RepositoryConnection.add` once per callback, while leaving its existing begin/commit/rollback envelope intact. A failure from any callback must reach the existing catch/rollback path. Update Spring MVC exception handling to recognize the typed decompression-limit exception and return the same request-limit response class used by existing expansion/depth limits. Keep explicit unsupported `Content-Encoding` responses at HTTP 415.

Add Commons Compress 1.28.0, Commons IO, and XZ for Java 1.12 to root dependency management. Add direct runtime dependencies to server-spring and Workbench, and test-only dependencies to core/rio/api and core/repository/api. Extend dependency-placement tests so an accidental compile/runtime dependency in core fails and both packaged ingestion paths are proven complete. Update the relevant server/Workbench documentation with supported formats, server-only optional-library availability, TAR member selection, nested limits, and the decoder-memory property.

Finally, run the focused selectors that were red, then module verification for core/rio/api and core/repository/api, Workbench transaction tests, server filter tests, WAR and Spring Boot upload tests, and retained-log verification for every module in scope. Run copyright and formatting checks, inspect the dependency trees or packaged archives for exact placement, rerun the root JDK 25 quick clean install, and audit the diff for API/HTTP compatibility.

For the reopened endpoint-matrix milestone, define one canonical set of small Turtle payload fixtures covering plain input, gzip, zlib/deflate, Brotli, Zstandard, BZip2, XZ, LZMA, framed LZ4, framed Snappy, Unix compress, ZIP, TAR, and the required compressed-TAR variants. Exercise that set through `tools/server/src/test/java/org/eclipse/rdf4j/http/server/ProtocolIT.java`, `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/commands/AddServletCoverageTest.java`, and `tools/server-boot/src/test/java/org/eclipse/rdf4j/tools/serverboot/Rdf4jServerWorkbenchApplicationTest.java`. A bare server request can test formats with reliable signatures; suffix-only formats must be placed in a named ZIP or TAR member so the same production suffix logic is exercised without inventing a new HTTP content coding. Workbench supplies a submitted filename and therefore tests top-level suffix detection directly. Each invocation must import a format-specific subject so stale repository state cannot make another case pass.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j7` with `JAVA_HOME=/Users/havardottestad/.sdkman/candidates/java/25.0.1-tem` and that JDK's `bin` first on PATH. Maven commands always use `-Dmaven.repo.local=.m2_repo`; tests never use `-am` or `-q`.

The completed baseline command is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use the repository runner for focused and module verification, retaining logs:

    python3 .codex/skills/mvnf/scripts/mvnf.py RioCompressionTest#<redMethod> --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py RDFLoaderTest#<redTarMethod> --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/rio/api --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/repository/api --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-spring --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/server --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-boot --retain-logs

If Apache Commons Compress 1.28.0 or XZ 1.12 is absent from `.m2_repo`, rerun only the dependency-fetching build once without offline mode, then return to offline commands. Immediately after the initial expected failures, preserve evidence with `scripts/agent-evidence.py` in `initial-evidence.txt` and retain the full mvnf logs under `logs/mvnf/`.

Before final verification, run `scripts/checkCopyrightPresent.sh` from `scripts`, then `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`. Finish with the same root JDK 25 quick clean install and inspect `git status --short`, `git diff --check`, and the full diff.

## Validation and Acceptance

Focused codec tests must prove every added format works by signature where reliable and by every required suffix otherwise. They must also prove short and non-markable streams preserve bytes, malformed data reports decompression failure, concatenated streams obey the library format semantics, optional-library absence leaves existing JDK formats working, and inputs demanding more than 65,536 KiB of decoder memory raise the typed limit exception with configured/default metadata.

Loader tests must import a plain TAR, every required compressed TAR alias, mixed RDF formats, and nested ZIP/TAR combinations. They must show unknown terminal members and TAR special files are skipped without preventing recognized RDF members from loading, explicit fallback formats parse unnamed/unknown regular members, archives without any RDF fail, unread member tails are drained, nesting limits apply, and one 50,000-entry aggregate counter spans ZIP and TAR while the old ZIP limit remains effective.

Workbench tests must observe one `RepositoryConnection.add` call per recognized member, a single commit after all succeed, and rollback with no commit when a later member fails. Server filter tests must observe signature-based RDF request decompression while `Content-Encoding: bzip2`, `xz`, or another newly added file codec still returns 415. WAR, Workbench, and Spring Boot tests must each exercise plain RDF, gzip, zlib/deflate, Brotli, Zstandard, BZip2, XZ, LZMA, framed LZ4, framed Snappy, Unix compress, ZIP, TAR, and required compressed-TAR forms through their real production dispatch path and observe the format-specific statement.

Every listed mvnf module run must finish with zero Surefire/Failsafe failures and retained logs. Dependency inspection must show no new non-test Commons/XZ dependency in core/rio/api or core/repository/api, and direct packaged dependencies in server-spring and Workbench. The final root quick install must report `BUILD SUCCESS` under JDK 25.

## Idempotence and Recovery

All builds and tests are safe to repeat. Preserve all untracked artifacts, especially `initial-evidence.txt`, `maven-build.log`, and retained logs. Never clean the Git worktree to recover. If a dependency is missing offline, fetch it once as described and retry offline. If a test run overwrites Surefire output, use the retained mvnf log and preserved `initial-evidence.txt`. If an implementation direction fails, revert only the exact newly added hunks with `apply_patch`, record the discovery here, and keep the red test unchanged.

## Artifacts and Notes

Baseline evidence:

    [INFO] BUILD SUCCESS
    [INFO] Total time: 29.200 s (Wall Clock)

The rejected first baseline attempt contained only `zsh: command not found: mvn` because PATH omitted Homebrew; the corrected invocation added `/opt/homebrew/bin` and used Temurin 25.0.1.

## Interfaces and Dependencies

In `core/rio/api`, add an internal public API shaped as:

    @InternalUseOnly
    public final class RDFInputDispatcher {
        public RDFInputDispatcher(ParserConfig parserConfig);
        public void dispatch(InputStream input, String sourceName, RDFFormat fallbackFormat,
                EntryConsumer consumer) throws IOException, RDF4JException;

        @FunctionalInterface
        @InternalUseOnly
        public interface EntryConsumer {
            void accept(InputStream input, String sourceName, RDFFormat format)
                    throws IOException, RDF4JException;
        }
    }

The exact checked exception declaration may be narrowed to existing RDF4J exception types if all callers remain natural and no exceptions are swallowed. Do not add new `RioCompression` enum values or change any existing public method signature.

Add a typed decoder limit exception in the Rio API, annotated `@InternalUseOnly` unless HTTP mapping requires it to be public API. It must expose the configured memory limit in KiB and distinguish a decoder-memory rejection from malformed compressed bytes.

Use Apache Commons Compress 1.28.0 for BZip2, XZ, LZMA, framed LZ4, framed Snappy, Unix `.Z`, and TAR. Use XZ for Java 1.12 because Commons delegates XZ/LZMA decoding to it. Package Commons IO as the runtime support Commons TAR requires. Core production sources must refer to these libraries only through reflection; core POM dependencies are test-scoped. Existing JDK gzip/zlib/ZIP and reflective Brotli/Zstandard behavior must remain available without Commons.

Revision note (2026-08-29, Codex): Created the initial self-contained implementation plan after repository reconnaissance and the successful JDK 25 baseline build. It records the approved input-only scope, reflective dependency boundary, shared archive semantics, red-first evidence, and verification gates.

Revision note (2026-08-29 21:06Z, Codex): Recorded the two focused red tests, their retained evidence, the one-time XZ 1.12 fetch, and the existing null-format loader failure discovered by the TAR test.

Revision note (2026-08-29 21:37Z, Codex): Updated progress after the reflective dispatcher, RDFLoader/Workbench integration, server limit mapping, dependency placement, documentation, and focused green tests were completed; recorded the Commons IO scope and nested-limit classification discoveries.

Revision note (2026-08-29 21:53Z, Codex): Recorded Workbench URL integration, the successful six-module verification matrix, sandbox-only socket failures and permitted reruns, dependency health and packaged-WAR evidence, and the final remaining root gate.

Revision note (2026-08-29 21:56Z, Codex): Marked the ExecPlan complete after the final root quick clean install and compatibility/diff audit passed.

Revision note (2026-08-29 22:28Z, Codex): Reopened the plan after the user requested proof that every supported format works from the WAR server, Workbench, and Spring Boot; added the complete endpoint matrix and the filename-bearing-container rule for suffix-only codecs.

Revision note (2026-08-29 22:38Z, Codex): Recorded the red Workbench packaging evidence, the narrow runtime-dependency fix, and green 18-case focused matrices for all three requested ingestion paths.

Revision note (2026-08-29 22:46Z, Codex): Completed the endpoint acceptance milestone after full module verification, packaged artifact inspection, formatting, diff audit, and the final JDK 25 root quick clean install.
