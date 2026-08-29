# Backport security hardening without compatibility regressions

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must stay current while the work proceeds. Maintain it in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

This branch starts at RDF4J `main` commit `4354c1c77fa3e87bcc7214b079828e8f9eaf110a` and backports only security hardening that can preserve existing public linkage, configuration, persistence, URL behavior, and ordinary valid requests. After the work, the server system-information pages will no longer expose host secrets; HTTP request decompression and recursive RDF archive expansion will be bounded against resource-exhaustion payloads. Existing public Java types and concrete method source contracts must remain usable, ordinary compressed requests and RDF imports below the configured ceilings must behave as before, and no remote-network, repository-layout, or Workbench migration is introduced.

The result is observable through focused tests that fail on unmodified `main`, pass after the backport, and include compatibility controls. Existing module suites, japicmp, and the root quick install must remain green. A security fix necessarily rejects the exploit class it closes; in this plan, that intentional rejection is not considered a compatibility regression, while rejection of ordinary inputs, removal of public API, changed persistent state, mandatory deployment configuration, or altered unrelated defaults is a regression.

## Progress

- [x] (2026-08-29 08:56Z) Created `GH-5988-main-compatible-security-hardening` from local `main` commit `4354c1c77f`.
- [x] (2026-08-29 08:56Z) Ran the mandatory root quick clean install; the full reactor passed in 30.575 seconds.
- [x] (2026-08-29 09:09Z) Captured diagnostic disclosure red evidence and a green legacy nested-controller linkage control.
- [x] (2026-08-29 09:09Z) Captured HTTP decompression red evidence and a green concrete `init(FilterConfig)` source-contract control.
- [x] (2026-08-29 09:09Z) Captured aggregate ZIP-budget red evidence and a green ordinary RDF resource-load control.
- [x] (2026-08-29 09:23Z) Implemented the three adjusted boundaries; all retained identical red-to-green selectors pass.
- [x] (2026-08-29 10:25Z) Completed the post-patch compatibility review; captured two additional RDF accounting reproductions before correction.
- [x] (2026-08-29 10:27Z) Corrected nested-codec denominator and unread ZIP-tail accounting; both identical selectors and all 12 RDF budget tests pass.
- [x] (2026-08-29 10:33Z) Passed copyright, formatting, and affected module gates: server-spring 122/122, repository-api 50/50, and server 38 passed with one platform skip.
- [x] (2026-08-29 10:35Z) Passed the final full-reactor quick clean install in 30.906 seconds and completed the exact-scope/API audit.

## Surprises & Discoveries

- Observation: The source hardening branch passes its focused affected suites but its broad CI run exposed unrelated compatibility failures in the remote-resource changes and a japicmp failure in the diagnostic change.
  Evidence: `tools/server-spring/target/japicmp/japicmp.diff` on the source branch reports four removed public nested controller classes, while the recorded broad run reported legitimate `file:` and loopback URLs rejected by the default public-network policy.

- Observation: The source HTTP decompression change adds `ServletException` to the concrete `HttpCompressionFilter.init(FilterConfig)` declaration.
  Evidence: The source branch japicmp report marks the new checked exception as a source-compatibility change even though the `Filter` interface permits it.

- Observation: The source recursive RDF budget stores a shared aggregate counter in each wrapper's mark snapshot and assigns that global counter back on reset.
  Evidence: `RDFInputDecompressionBudget.AccountingInputStream.reset()` on the source branch can discard charges made through a sibling nested wrapper after the mark; a focused regression must settle this before porting.

- Observation: A read-only prepatch review independently classified the three selected boundaries as portable only with the compatibility adjustments in this plan.
  Evidence: The reviewer identified the same removed controller types and checked `init` exception, and found that the source RDF wrapper's shared-counter reset can erase sibling charges. It rejected the remote-network, repository-ID, and Workbench credential/session commits as incompatible without migrations.

- Observation: Japicmp treats adding deprecation metadata to an existing public class as a patch-level incompatibility even when all names and descriptors remain present.
  Evidence: The first postpatch diagnostic selector passed its Surefire assertion but Maven failed japicmp with `ANNOTATION_DEPRECATED_ADDED` for all four legacy nested controller types. Removing both the annotation and Javadoc deprecation tag made the identical selector and japicmp pass.

- Observation: Removing Workbench's filename-aware pre-decompression would break uploads when Workbench targets an older remote RDF4J server.
  Evidence: `HTTPRepositoryConnection.add(InputStream, ...)` sends the supplied bytes directly with the RDF MIME type and no `Content-Encoding`; an older server therefore receives gzip bytes as Turtle. The experimental handoff and its test changes were removed from the candidate diff.

- Observation: The first RDF implementation counted bytes from every nested compressed layer in the expansion-ratio denominator.
  Evidence: `RDFLoaderDecompressionBudgetTest#rejectsCompoundedNestedExpansionAgainstOriginalSource` wraps a high-ratio terminal payload behind compressible intermediate codec bytes; the implementation accepted it because intermediate expanded bytes diluted the original-source ratio. The focused selector failed with one expected assertion failure in `logs/mvnf/20260829-102300-verify.log`.

- Observation: `ZipInputStream.closeEntry()` drains an unread entry tail directly from the ZIP stream after an early-terminating RDF parser returns.
  Evidence: A registered test parser reads one byte from a ZIP entry with a one-MiB tail. `RDFLoaderDecompressionBudgetTest#accountsForZipEntryBytesUnreadByEarlyTerminatingParser` expected the 64-KiB expanded-byte ceiling to fail, but no throwable was raised in `logs/mvnf/20260829-102515-verify.log`.

## Decision Log

- Decision: Use Routine D and retain a failing-test-before-production gate for each behavior-changing boundary.
  Rationale: The work spans server rendering, servlet request IO, and recursive RDF parsing. Independent red/green evidence keeps each security invariant and compatibility control auditable.
  Date/Author: 2026-08-29 / Codex

- Decision: Limit the production backport to diagnostic redaction, HTTP decompression limits, and recursive RDF decompression limits.
  Rationale: Remote-resource defaults break file, loopback, private-client, and private-federation behavior; repository-ID changes require migration; Workbench changes invalidate cookies and require new key storage. Those changes cannot satisfy this branch's compatibility contract without either retaining the vulnerability or adding a deployment migration.
  Date/Author: 2026-08-29 / Codex

- Decision: Preserve the four existing public nested controller types as deprecated compatibility facades while removing sensitive values from rendered models.
  Rationale: The security boundary is what the HTTP pages render, not whether same-JVM callers can link the historical view-model class names. Keeping the names and methods avoids binary/source breakage.
  Date/Author: 2026-08-29 / Codex

- Decision: Keep `HttpCompressionFilter.init(FilterConfig)` free of newly declared checked exceptions.
  Rationale: Callers compiled or sourced against the concrete class must retain the old contract. Invalid configuration may still fail filter initialization through a repository-native unchecked configuration error while normal servlet-container startup semantics remain fail-closed.
  Date/Author: 2026-08-29 / Codex

- Decision: Do not copy commits wholesale from `GH-5988-security-hardening-2-9`.
  Rationale: The source commits contain the exact compatibility regressions this branch exists to avoid. Tests and production changes will be ported selectively after `main` failures are captured.
  Date/Author: 2026-08-29 / Codex

- Decision: Include the isolated `AddServlet` handoff that passes compressed uploads to `RepositoryConnection.add` unchanged.
  Rationale: Superseded after tracing remote repository topology; see the following decision.
  Date/Author: 2026-08-29 / Codex

- Decision: Retain Workbench pre-decompression and exclude the upload handoff from this branch.
  Rationale: Workbench supports `RemoteRepositoryManager`, and remote connections forward the stream without compression metadata. The handoff is safe for a same-version local repository but not for older remote deployments. Preserving compatibility takes priority; closing the remaining Workbench ratio-accounting bypass needs a topology-aware design.
  Date/Author: 2026-08-29 / Codex

- Decision: Measure RDF expansion ratio only against bytes read from the caller's original top-level source stream.
  Rationale: Treating expanded bytes from an intermediate codec layer as additional compressed input weakens the aggregate ratio geometrically under nested compression. The numerator remains all newly expanded bytes, while exactly one top-level source wrapper supplies the denominator.
  Date/Author: 2026-08-29 / Codex

- Decision: Drain every ZIP entry through its expanded accounting wrapper before calling `closeEntry()`.
  Rationale: RDF parsers may validly stop before EOF, and directory entries can still contain adversarial payload bytes. The archive traversal must account for the full uncompressed entry even when the selected parser does not consume it.
  Date/Author: 2026-08-29 / Codex

## Outcomes & Retrospective

The branch now contains three compatibility-adjusted hardening boundaries: allowlisted diagnostic-page models and escaped retained values; bounded HTTP request decompression with stable 400/413 mapping; and aggregate recursive RDF decompression limits. Two concrete RDF accounting gaps found by the required post-patch review were reproduced before correction and are covered permanently: nested codec bytes cannot dilute the original-source expansion ratio, and unread ZIP entry tails remain charged when a parser terminates early.

Existing public descriptors for both diagnostic controllers, their four nested compatibility types, `HttpCompressionFilter`, and `RDFLoader` match the 6.0.0 baseline. New settings and model types are additive. Configuration remains optional, no persisted representation changes, and no deployment-topology changes are present. The only intentional runtime behavior change is rejection of disclosure or input exceeding the new decompression safety ceilings; ordinary inputs under the generous defaults retain their prior semantics.

Remote-resource defaults, repository identifiers/deletion, Workbench credential/session changes, and the experimental Workbench compressed-stream handoff remain excluded. The last item is a known residual topology-specific bypass: changing it without a protocol-aware handoff would send compressed RDF bytes without metadata to older remote servers and violate this plan's compatibility requirement.

Verification completed with matching focused red/green evidence, `tools/server-spring` 122/122 green, `core/repository/api` 50/50 green, and `tools/server` 38 passing with one platform skip. Copyright and formatting checks passed. The final full-reactor `-Pquick clean install` passed every module in 30.906 seconds. The exact branch diff has no whitespace errors, no staged changes, and no files in HTTP clients, federation, repository management/layout, or Workbench. The branch is intentionally left uncommitted and unpushed for user review.

## Context and Orientation

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j7`. This is a multi-module Maven repository. Maven commands must use the workspace-local `.m2_repo`. Tests must never use `-am` or `-q`; `.codex/skills/mvnf/scripts/mvnf.py` is the preferred focused and module test runner because it installs current reactor artifacts before verification. Initial failing Surefire evidence must be appended to the ignored top-level `initial-evidence.txt` before production code changes can begin.

The source material is branch `GH-5988-security-hardening-2-9`. Commit `9bb9190dba` hardens diagnostic pages, `b795297cc9` bounds HTTP request decompression, and `909e19ba8a` bounds recursive RDF decompression. They are design inputs, not patches to apply unchanged.

Diagnostic controllers live in `tools/server-spring/src/main/java/org/eclipse/rdf4j/common/webapp/system`. They build models used by JSPs under `tools/server/src/main/webapp/WEB-INF/views/system`. On `main`, those models include arbitrary JVM properties, environment variables, operating-system user names, and the application-data path. The security boundary is the rendered HTTP response. Compatibility includes the public nested `ServerInfo` and `MemoryInfo` class names and methods that shipped previously.

HTTP request decompression lives in `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/compression/HttpCompressionFilter.java`. The filter wraps compressed request streams and also handles forms. The backport may add an internal shared request budget and public configuration names, but it must preserve the existing class and method descriptors and the source-level `init(FilterConfig)` throws contract. Ordinary gzip, deflate, Brotli, and Zstandard requests within the defaults must still reach the filter chain unchanged.

Recursive RDF loading lives in `core/repository/api/src/main/java/org/eclipse/rdf4j/repository/util/RDFLoader.java`. Compressed layers and ZIP entries recursively call the loader. One top-level load must share byte, expansion-ratio, nesting, and entry counters. Compatibility includes existing constructors and overloads, all RDF formats, plain streams, file URLs and custom URL handlers, stream closure, and transaction rollback. No remote-resource policy belongs in this branch.

## Plan of Work

First, add focused tests copied conceptually from the source hardening branch but extended with compatibility assertions. For diagnostics, prove the controllers and JSPs currently expose forbidden host data, and add linkage coverage that constructs every historical nested public class and calls its methods. Run the smallest failing security method and capture its Surefire report before editing production.

For HTTP decompression, extend `HttpCompressionFilterTest` with exact-boundary success, one-byte-over-limit failure, malformed input status, and ordinary codec controls. Add a compile-time test or direct concrete invocation showing that `new HttpCompressionFilter().init(config)` remains callable without catching a checked exception. The security method must fail on `main`; the compatibility method should pass before and after.

For recursive RDF decompression, add tests for aggregate ZIP entries, nesting, byte and ratio ceilings, plain-stream one-to-one accounting, and a nested mark/reset case that proves a wrapper cannot rewind charges made by another wrapper. Preserve transaction rollback coverage where the existing repository API test harness makes it proportionate. Run the smallest failing method and persist its report before production edits.

Then implement each boundary minimally. Build a shared allowlisted diagnostic model but keep deprecated nested controller facades with their original constructors and accessors. Remove only sensitive model entries and use escaped JSP rendering. Add HTTP and RDF budgets using overflow-safe counters, one shared budget per request/load, bounded incremental reads, stable errors, and configurable ceilings. In the RDF budget, make mark/reset accounting local to each wrapper or otherwise monotonic at the aggregate level; never assign a shared global counter backward.

After focused red/green tests, challenge every changed helper by tracing direct callers and both sides of each new condition. Confirm an ordinary input that previously succeeded still succeeds. Confirm no equivalent decompression path bypasses the shared budget. Confirm no public class or method is removed or gains a checked exception. Incorporate only concrete reviewer findings, then rerun identical selectors and affected modules.

## Concrete Steps

The initial root command already passed:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Use focused selectors through `mvnf`, retaining logs:

    python3 .codex/skills/mvnf/scripts/mvnf.py SystemInformationSecurityTest#controllersExposeOnlyApprovedSystemInformation --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py HttpCompressionFilterTest#rejectsExpandedRequestAboveConfiguredLimit --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py RDFLoaderDecompressionBudgetTest#sharesBudgetAcrossZipEntries --retain-logs

The exact method names may be refined to match the tests added on this branch. Immediately after each first failing run, use `scripts/agent-evidence.py` against the owning module's Surefire directory and append the compact command, report path, and failure summary to `initial-evidence.txt` with `apply_patch`.

After each production edit, rerun the identical selector, then its test class. At final verification, run the complete `tools/server-spring`, `tools/server`, `core/repository/api`, and `tools/workbench` modules if Workbench's existing `AddServlet` integration is touched. Run japicmp through the repository's established verify path and inspect `tools/server-spring/target/japicmp/japicmp.diff` for removed classes, methods, or newly declared checked exceptions.

Before finalizing Java and JSP changes, run:

    cd scripts && ./checkCopyrightPresent.sh
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The `-q` flag is permitted only for formatting, never for tests. Finish with the root clean quick install command, `git diff --check`, `git status --short`, and a complete diff review. Preserve all untracked artifacts.

## Validation and Acceptance

Diagnostic acceptance requires both system pages to omit environment variables, arbitrary JVM properties, operating-system user names, and application-data paths. Retained application, runtime, operating-system, and aggregate-memory values must render escaped. All four historical public nested controller types must remain loadable with their constructors and accessors so japicmp does not report removal.

HTTP decompression acceptance requires exact-limit valid requests to pass and over-limit or malformed compressed requests to fail before unbounded allocation or downstream processing. All supported codecs and request access paths share one budget. The concrete `HttpCompressionFilter.init(FilterConfig)` declaration must remain source-compatible, and ordinary existing filter tests must remain unchanged and green.

RDF decompression acceptance requires every recursive compression layer and ZIP entry in one load to share aggregate byte, ratio, nesting, and entry ceilings. Exact boundaries and plain streams pass; excess payloads fail with a stable parse error and preserve rollback. Aggregate counters are monotonic across nested wrapper mark/reset operations. Existing URL schemes, constructors, parser configuration, stream closure, and ordinary RDF imports remain unchanged.

Final acceptance requires matching pre-fix failure and post-fix success for every security behavior, passing compatibility controls, green affected module suites, no japicmp regression, successful copyright and formatting checks, a green root quick install, and a final diff limited to the ExecPlan, focused tests, diagnostic rendering, and decompression implementation. No remote access, HTTP client, federation, repository ID, Workbench credential/session, or persistent-format change is permitted.

## Idempotence and Recovery

The build, tests, formatters, and checks are safe to rerun. Test inputs must use in-memory bytes and temporary directories and must not connect to external services or alter user data. If offline resolution fails for a missing artifact, rerun the identical command once without `-o`, then return offline.

If production code is modified before its smallest failing test and persisted Surefire evidence, stop, revert only the known patch with `apply_patch`, set the plan back to the reproduction, and resume. Never use `git reset`, `git restore`, `git clean`, or delete untracked artifacts. Unexpected working-tree changes are user-owned; work around them unless they overlap this plan.

## Artifacts and Notes

Branch creation:

    git switch -c GH-5988-main-compatible-security-hardening main
    Switched to a new branch 'GH-5988-main-compatible-security-hardening'
    HEAD 4354c1c77fa3e87bcc7214b079828e8f9eaf110a

Initial build:

    Reactor: Eclipse RDF4J 6.0.1-SNAPSHOT
    BUILD SUCCESS
    Total time: 30.575 s
    Finished at: 2026-08-29T10:56:32+02:00

No production code had been changed when this ExecPlan was created. Add compact red and green evidence here as each milestone completes; keep the canonical initial failures in `initial-evidence.txt` and retained logs under `logs/mvnf`.

Focused pre-change evidence:

    SystemInformationSecurityTest#controllersExposeOnlyApprovedSystemInformation: tests=1, failures=1
    HttpCompressionFilterTest#rejectsExpandedRequestAboveConfiguredLimit: tests=1, failures=1
    RDFLoaderTest#sharesExpandedByteBudgetAcrossZipEntries: tests=1, failures=1
    SystemInformationSecurityTest#legacyPublicControllerTypesRemainUsable: tests=1, failures=0
    HttpCompressionFilterTest#concreteInitRetainsNoCheckedException: tests=1, failures=0
    RDFLoaderTest#testTurtleJavaResource: tests=1, failures=0

The canonical snippets and retained-log paths are in `initial-evidence.txt`.

Focused post-change evidence:

    SystemInformationSecurityTest#controllersExposeOnlyApprovedSystemInformation: tests=1, failures=0
    HttpCompressionFilterTest#rejectsExpandedRequestAboveConfiguredLimit: tests=1, failures=0
    RDFLoaderTest#sharesExpandedByteBudgetAcrossZipEntries: tests=1, failures=0
    SystemInformationSecurityTest: tests=3, failures=0
    HttpCompressionFilterTest: tests=16, failures=0
    RDFLoaderDecompressionBudgetTest: tests=10, failures=0
    RDFLoaderTest: tests=7, failures=0

Post-patch review reproductions captured before their production correction:

    RDFLoaderDecompressionBudgetTest#rejectsCompoundedNestedExpansionAgainstOriginalSource: tests=1, failures=1
    RDFLoaderDecompressionBudgetTest#accountsForZipEntryBytesUnreadByEarlyTerminatingParser: tests=1, failures=1

Matching post-correction evidence:

    RDFLoaderDecompressionBudgetTest#rejectsCompoundedNestedExpansionAgainstOriginalSource: tests=1, failures=0, log logs/mvnf/20260829-102642-verify.log
    RDFLoaderDecompressionBudgetTest#accountsForZipEntryBytesUnreadByEarlyTerminatingParser: tests=1, failures=0, log logs/mvnf/20260829-102716-verify.log
    RDFLoaderDecompressionBudgetTest: tests=12, failures=0, log logs/mvnf/20260829-102748-verify.log

Affected module evidence:

    tools/server-spring: tests=122, failures=0, errors=0, log logs/mvnf/20260829-102926-verify.log
    core/repository/api: tests=50, failures=0, errors=0, log logs/mvnf/20260829-103035-verify.log
    tools/server: tests=39, failures=0, errors=0, skipped=1, log logs/mvnf/20260829-103154-verify.log

The first repository-api and server attempts were constrained by the filesystem/network sandbox and failed only while binding local test ports. Their unchanged reruns with local-port access produced the green evidence above. `tools/server-spring/target/japicmp/japicmp.diff` contains only additive `SystemInformation` classes; no existing class, method, field, annotation, or checked-exception contract is removed or changed.

## Interfaces and Dependencies

Do not add external dependencies. Reuse Java IO streams, servlet APIs, existing RDF4J `RioSetting` types, and the codec libraries already present in the affected modules. New Java files must carry the exact current-year Eclipse RDF4J header and `// Some portions generated by Codex` immediately below it.

Preserve every existing public constructor and method descriptor in the diagnostic controllers, `HttpCompressionFilter`, and `RDFLoader`. New configuration is additive. Internal budget helpers may use package-private types. Exceptions must remain within existing declared API contracts; new checked exceptions must not escape formerly narrower concrete methods.

Revision note (2026-08-29 / Codex): Created the initial self-contained plan after branching from `main` and completing the mandatory root quick install. The plan deliberately excludes the remote-network, repository-ID, and Workbench migrations that caused or inherently require compatibility changes.

Revision note (2026-08-29 / Codex): Recorded the three red security reproductions, three green compatibility controls, and the independent prepatch review. Clarified that the isolated Workbench upload handoff is part of RDFLoader enforcement while all Workbench credential/session migrations remain excluded.

Revision note (2026-08-29 / Codex): Recorded implementation and focused post-change evidence. Removed deprecation metadata from compatibility facades after japicmp demonstrated that even the annotation was outside the patch-release compatibility contract.

Revision note (2026-08-29 / Codex): Removed the experimental Workbench upload handoff after tracing `HTTPRepositoryConnection` and finding that it would send compressed bytes without metadata to older remote servers. Recorded this as a deliberately unresolved topology-specific bypass rather than retaining an incompatible patch.

Revision note (2026-08-29 / Codex): Recorded the post-patch review's nested-ratio and unread-ZIP-tail findings, captured focused failing tests before production edits, and corrected both at the shared RDF input boundary. The matching selectors and the complete 12-test budget class pass.

Revision note (2026-08-29 / Codex): Completed affected module verification, the final full-reactor quick install, public-descriptor comparison, forbidden-scope audit, and whitespace audit. Recorded the final compatibility matrix and the deliberately excluded Workbench topology case.
