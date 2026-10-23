# Protect whole-repository deletion with an optional password

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Its Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective sections must be updated whenever work changes direction or reaches a milestone.

## Purpose / Big Picture

An administrator can opt in to a separate password for deleting an entire repository. Without the setting, existing deletion behavior stays available. With it, direct HTTP `DELETE /repositories/{id}`, removal of a configuration through the legacy `SYSTEM` repository, and Workbench deletion require the password. A remote Workbench asks its Server whether protection is enabled and sends the password only with that deletion. Users can verify the behavior by attempting deletion with no, wrong, and correct passwords and observing that a refused repository remains present.

## Progress

- [x] (2026-10-22 22:00Z) Inspect current checkout and prior PR.
- [x] (2026-10-22 22:00Z) Complete initial quick clean install.
- [x] (2026-10-22 22:04Z) Capture smallest failing server regression.
- [x] (2026-10-22 22:14Z) Implement shared policy and REST guard.
- [x] (2026-10-22 22:24Z) Guard SYSTEM configuration deletion boundaries.
- [x] (2026-10-22 22:30Z) Carry credentials through remote clients.
- [x] (2026-10-22 22:41Z) Add Workbench prompt and safe failures.
- [x] (2026-10-22 22:44Z) Verify generator docs tests and UI.
- [x] (2026-10-22 23:29Z) Resolve candidate review findings.
- [in_progress] Publish and verify draft PR.

## Surprises & Discoveries

- The first offline root clean install stopped before compilation because `maven-dependency-plugin:3.11.0` was absent from this worktree's read-through cache. The prescribed online retry succeeded; both attempts are retained in `maven-build.log`.
- The older PR #6166 targeted the former `tools/workbench/src/main/webapp/scripts/ts/` frontend layout. Current authoritative TypeScript resides under `tools/workbench/src/main/frontend/classic/`, and Maven compiles generated scripts into `target/frontend`; generated JavaScript is not committed.
- The first `SYSTEM` integration-test attempt stopped before its assertion: `TestServer` expects an unpacked WAR at `tools/server/target/rdf4j-server`, while isolated `mvnf` placed it under `.mvnf/workspaces/repository-deletion/build/.../rdf4j-server`. The Failsafe report shows Jetty's `jarWebApp` null path. A worktree-local symlink at the expected test path is needed for this runner; this is a harness path repair, not a production change.
- The repaired `SYSTEM` test exposed the intended behavioral failure: HTTP 204 for an unauthenticated repository configuration delete where 403 was required. Its first cleanup also failed because `TestServer.stop()` attempts uncredentialed deletion; the regression now removes test repositories with the password before stopping Jetty.

## Decision Log

- Decision: Reimplement the older PR's behavior on current main, using its security contracts as a reference instead of cherry-picking 49 files from a different base. Rationale: the old PR is based on `develop`, and the Workbench frontend path and code have moved. Date/Author: 2026-10-22, Codex.
- Decision: Keep the policy optional and server authoritative. When the JVM property is absent, deletion stays open; a remote Workbench discovers policy with `HEAD /protocol`; local Workbench applies its own startup policy. Rationale: this preserves legacy behavior and prevents Workbench configuration from overriding a remote Server. Date/Author: 2026-10-22, Codex.
- Decision: Protect whole-repository deletion only. Do not add a password to ordinary statement removal, `clear`, or SPARQL updates. Rationale: these are different operations with established authorization behavior. Date/Author: 2026-10-22, Codex.
- Decision: Skip PBKDF2 verification for read-only SYSTEM HTTP methods, while retaining the START-time authorization decision for write transactions. Rationale: an arbitrary header on GET or HEAD must not consume 600,000 KDF rounds, and deferring verification across START/COMMIT would retain the password beyond the request. Date/Author: 2026-10-23, Codex.
- Decision: Disable redirects only on credential-bearing repository DELETE requests in both supported HTTP transports. Rationale: a 307/308 response otherwise forwards the custom header to another origin; ordinary requests and passwordless DELETE retain their redirect behavior. Date/Author: 2026-10-23, Codex.
- Decision: Wrap Tab at the boundaries of the shared native confirmation dialog, including visible controls in its body. Rationale: independent browser review observed a transient BODY focus stop after the final action. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The initial root build succeeded after fetching the missing Maven plugin. Direct REST, deployed SYSTEM, read-only SYSTEM KDF, two-origin redirect, and dialog focus red reports are preserved in `initial-evidence.txt`; the rendering red report is in `frontend-initial-evidence.txt`. Focused and full client, manager, Server, Workbench Java, and frontend suites pass. The deployed Server integration class passes 5/5, and independent browser review approved desktop and 390-pixel responsive deletion, cancel/reopen, focus restoration, and Tab wrapping. Copyright, formatter, and `git diff --check` pass. The remaining work is scoped publication and exact-head verification.

## Context and Orientation

The Maven root is this repository checkout. `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/handler/DefaultRepositoryRequestHandler.java` handles direct repository DELETE. `RepositoryInterceptor.java` creates the special `RepositoryConfigRepository` for `SYSTEM`; its connection's `commit()` currently applies configuration removals and updates in sequence. `tools/server/src/main/webapp/WEB-INF/rdf4j-http-server-servlet.xml` wires the Server controllers. `core/http/protocol/Protocol.java` defines shared HTTP names. `core/http/client/RDF4JProtocolSession.java` sends DELETE, and `core/repository/manager/RemoteRepositoryManager.java` exposes it to callers. `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/DeleteServlet.java` serves and submits the delete page. Its browser view and confirmation logic are in `tools/workbench/src/main/frontend/classic/workbenchViews.ts` and `delete.ts`; the router sends forms without persisting their passwords.

An accepted password setting is `org.eclipse.rdf4j.repository.deletePassword`. The preferred stored value is `{pbkdf2-sha256}<iterations>:<base64-salt>:<base64-hash>` with PBKDF2-HMAC-SHA256, at least 600,000 iterations, at least 16 salt bytes, and 32 hash bytes. An explicit `{plain}` prefix permits passwords beginning with `{`; an unprefixed nonempty value remains a compatibility option. An empty or malformed property must fail application startup without printing the secret. The request header `X-RDF4J-Repository-Delete-Password` carries base64 of exact UTF-8 password bytes; it is per request, never a persistent client header. `X-RDF4J-Repository-Delete-Password-Required` on `HEAD /protocol` advertises the immutable Server policy. A refusal is HTTP 403 with a safe typed error. A legacy Server without the capability header means no password is required; a failed discovery request does not mean unprotected.

## Plan of Work

First add a focused test in `tools/server-spring/src/test/java/org/eclipse/rdf4j/http/server/repository/RepositoryControllerTest.java` that sets the opt-in property, issues direct DELETE without a password, and asserts a 403 refusal and no manager removal. Run it alone through `mvnf` and immediately save the Surefire failure in root `initial-evidence.txt` before editing production code. Add nearby missing, wrong, correct, and default-unset variants without weakening the original assertion.

Add a strict immutable `RepositoryDeletionPolicy` in `core/repository/manager`, a safe refusal type and header constants in `core/http/protocol`, and request-scoped decoding in `tools/server-spring`. Wire one startup snapshot through Server's XML configuration to `ProtocolController`, `RepositoryController`, and `RepositoryInterceptor`. For direct DELETE, check the credential before the repository manager is called. For `SYSTEM`, capture the authorization decision when its repository instance is created at transaction START, and preflight every changed repository configuration before any update or deletion is applied. This prevents an unauthorized mixed transaction from partially changing configurations. Test malformed encodings, duplicate headers, exact Unicode and whitespace, hash verification, and startup configuration errors.

Extend `RDF4JProtocolSession` with a per-call deletion-password overload and `HEAD /protocol` discovery, then expose both through `RemoteRepositoryManager`. Keep old methods delegating with no password. Verify that credentials are not reused on later calls, and that an absent capability header remains compatible.

For Workbench, discover the remote Server policy when rendering the delete page; apply the local startup policy only for embedded/local managers. Add a masked password input only when required, retain the selected repository and safe error on a refused deletion, and clear the password after submission, logout, or page replacement. Keep typed repository-ID confirmation and prevent duplicate submissions. Update the current TypeScript sources, Java servlet tests, Node UI tests, and browser E2E coverage. Inspect the rendered dialog's focus, rapid cancellation/resubmission, and responsive layout independently before publication.

Add a dependency-free generator that reads hidden confirmed input or exact UTF-8 stdin, never a command argument or environment value. Document setup and migration in the Server/Workbench guide and OpenAPI file. Run copyright and formatting checks, affected Java modules, Node unit tests, and feasible deployed HTTP and browser flows. Review the exact diff with the parent before scoped commit and draft PR publication.

## Concrete Steps

Run commands from `/Users/havardottestad/.codex/worktrees/security-repository-deletion/rdf4j`. Maven must use the worktree's `.m2_repo` read-through symlink plus `mvnf --workspace repository-deletion` so changed artifacts and test outputs stay private. The initial root install used `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -Dmaven.repo.local=.m2_repo -Dmaven.repo.local.head="$PWD/.mvnf/workspaces/repository-deletion/repository" -Drdf4j.build.root="$PWD/.mvnf/workspaces/repository-deletion/build" -Pquick clean install` after the offline missing-plugin retry and ended `BUILD SUCCESS`.

Run the smallest red test with `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace repository-deletion RepositoryControllerTest#<method> --retain-logs`. Record its exact report using `python3 scripts/agent-evidence.py --command '<same command>' <report-directory> > initial-evidence.txt`. Never use Maven `-am` or `-q` when testing. Run focused methods first, then affected modules with `mvnf --workspace repository-deletion <module> --retain-logs`. Before finalizing, run `cd scripts && ./checkCopyrightPresent.sh`, then Maven `process-resources` without `-q` using the same local-repository head and build root, then inspect `git diff --check` and staged scope. Preserve all reports and untracked artifacts.

## Validation and Acceptance

With no property, a direct DELETE and a Workbench delete continue to succeed. With a generated verifier, direct DELETE and `SYSTEM` configuration removal return 403 for no password, a wrong password, a malformed header, and the stored verifier supplied as if it were the password; the repository remains available. The exact original password deletes it. A `SYSTEM` transaction's START authorization controls a later commit, and a refused mixed update/delete transaction changes nothing. A remote manager discovers the capability from `HEAD /protocol` and sends the password on only one request. Workbench shows a masked field only for protected Servers, requires typed ID confirmation, leaves a refused repository selected, and never stores the password in browser history or its page model. Generated verifier output can be used by the final Server artifact. Tests and E2E observations must establish these outcomes, with any unavailable browser or deployment test called out explicitly.

## Idempotence and Recovery

All work remains in this isolated branch and workspace. Repeating a test may replace the active Surefire report, so retain the first red snippet in `initial-evidence.txt` immediately. A failed offline dependency resolution is retried once online, then subsequent commands return offline. Do not delete untracked artifacts, reset the checkout, or overwrite tracked files with copies from the older PR. If a new test exposes a design gap, update this plan's decision and progress sections before editing implementation.

## Artifacts and Notes

`maven-build.log` contains the first offline missing-plugin failure and the successful initial online root install. `initial-evidence.txt` retains the first Surefire failure: `RepositoryControllerTest#configuredPasswordRejectsDirectDeletionWithoutCredential`, tests=1, failures=1, "Expecting code to raise a throwable." The corresponding method passed after the guard. `RepositoryDeletionPolicyTest` passed 14 tests and `RepositoryControllerTest` passed 13 tests. The older PR #6166 is a design reference; its GitHub metadata and patches were inspected read-only. Record module totals, formatting result, final diff, PR URL, and exact SHA here as work proceeds.

## Interfaces and Dependencies

Use existing JDK cryptography (`SecretKeyFactory`, `PBEKeySpec`, `MessageDigest.isEqual`) and Base64 utilities; add no dependency. The policy is an immutable startup object shared by Server controllers, while `RepositoryDeletionAuthorization` interprets one request's header. Public client additions are `RDF4JProtocolSession.deleteRepository(String, String)`, `RDF4JProtocolSession.isRepositoryDeletionPasswordRequired()`, `RemoteRepositoryManager.removeRepository(String, String)`, and `RemoteRepositoryManager.isRepositoryDeletionPasswordRequired()`. Existing overloads retain their signatures. The Browser TypeScript uses current `tools/workbench/src/main/frontend/classic/` modules and Maven's generated assets, not tracked legacy JavaScript files.

Revision note (2026-10-22 22:00Z): Created this plan after source orientation and the required initial build. The worktree is clean except for build artifacts, branch creation, and this plan; production code has not changed.

Revision note (2026-10-22 22:04Z): Added and ran the direct Server refusal test. It failed because current DELETE succeeds without checking the opt-in property; persisted its Surefire evidence before production changes and moved Progress to the policy/REST milestone.

Revision note (2026-10-22 22:14Z): The immutable policy and direct DELETE path passed focused tests, including exact Unicode, malformed headers, duplicate headers, and default-unset behavior. The next active milestone is SYSTEM preflight and startup wiring.

Revision note (2026-10-22 22:17Z): The first deployed `SYSTEM` test was blocked by TestServer's hard-coded `tools/server/target/rdf4j-server` path under isolated Maven output. Create an ignored worktree-local symlink to the unpacked WAR and rerun the unchanged test before implementing SYSTEM behavior.

Revision note (2026-10-22 22:24Z): The unchanged deployed `SYSTEM` test then failed as expected with 204 instead of 403; its report was appended to `initial-evidence.txt`. A request-scoped decision now reaches the SYSTEM repository instance and `commit()` preflights every pending deletion before applying any configuration. The deployed IT passed 1/1 and the focused transaction class passed 4/4. Continue with per-call client transport and discovery.

Revision note (2026-10-22 22:30Z): The HTTP protocol session has a per-call credential header and `HEAD /protocol` capability discovery. The remote manager forwards only the invocation's credential. `RDF4JProtocolSessionTest` passed 72 tests (4 skipped); `RemoteRepositoryManagerTest` passed 8, including concurrent isolation. Move to Workbench servlet and current frontend sources.

Revision note (2026-10-22 22:41Z): A Workbench direct POST red test returned 302 instead of 403 before servlet edits; its Surefire report was appended to `initial-evidence.txt`. The corrected focused method passed, the 29-test servlet class and three startup tests passed, and the full frontend suite passed 648/648 after an in-repo rendering regression changed from red to green. Workbench page models carry only the required-policy bit and safe refusal, while the masked input is read live and cleared after submission. Continue with verifier generator, docs, broad modules, and deployed/browser checks.

Revision note (2026-10-22 22:44Z): The generator passed 14 tests, the deployed Server integration class passed 5 tests, and the full server-spring module passed 219 tests. Copyright and `git diff --check` passed. Independent review identified a read-only SYSTEM KDF cost and possible credential forwarding by client redirects; add red regressions for both before production edits and resolve them before candidate review.

Revision note (2026-10-22 23:29Z): The read-only SYSTEM and cross-origin redirect tests failed before their fixes, then passed. The focused dialog Tab test and CSS-hidden body variant likewise failed before their fixes and now pass 10/10 as a class. Full suites: Workbench Java 1,362/1,362; Workbench frontend 648/648; server-spring 220/220; repository manager 47/47; HTTP client 198 tests, 8 existing skips, no failures; deployed Server IT 5/5. Client API and JDK transport tests passed their focused modules. The generator passed 14/14. The isolated Boot review JAR needed generated frontend assets copied to Workbench's ignored `target/frontend` because Boot packaging uses that fixed path; the reviewed loopback candidate returned HTTP 200 for page and scripts. The parent independently verified rendered dialog and responsive behavior and approved publication. The root formatter and copyright checker succeeded; `git diff --check` is clean.
