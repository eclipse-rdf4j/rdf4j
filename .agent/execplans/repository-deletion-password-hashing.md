# Store a salted verifier for repository deletion

This ExecPlan is maintained under `.agent/PLANS.md`. Its Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective are living records.

## Purpose / Big Picture

Administrators can configure repository deletion protection without placing the actual deletion password in JVM options. A standalone Python generator produces a salted password verifier. Server and local Workbench accept the original password against that verifier; remote Workbench continues to follow Server authority and prompts exactly as before. Existing ordinary plaintext configuration remains compatible.

## Progress

- [done] Establish build and hashed policy regression.
- [done] Implement strict immutable verifier policy.
- [done] Complete secure generator and CLI verification.
- [done] Verify local and real HTTP boundaries.
- [done] Deliver final evidence and user handoff.

## Surprises & Discoveries

The existing policy already centralizes direct Server, SYSTEM configuration deletion, and local Workbench checks. Extending that immutable policy therefore requires no credential transport or frontend changes. All previous plaintext feature source and evidence remain present and uncommitted on develop.

The manager module has no junit-jupiter-params dependency, and this checkout's AssertJ lacks `hasStackTraceNotContaining`. The runner replaced parameterized policy annotations with ordinary JUnit loops and captured the complete startup exception trace through StringWriter for the same absence assertions. All inputs and assertions remain; no dependency was added. Both compile-only failure logs are retained under `focused-policy`. Focused tests subsequently passed.

The initial generator PTY assertions inspected stdout/stderr pipes, but terminal echo travels through the PTY master independently. Review required explicit terminal ECHO-state checks and capture of that channel before accepting the hidden-input evidence.

Piped subprocess stdin does not remove an inherited controlling terminal. The generator's intended no-terminal tests now start detached POSIX sessions so they also run correctly when a developer invokes the suite from an interactive terminal. The production generator correctly uses the controlling terminal on POSIX and guards stdin.isatty on Windows; GetPassWarning remains a hard failure.

The first live immutable jar passed HTTP and Chromium checks but differed from the final artifact after Maven repackaging. Verification was repeated against a fresh, ZIP-validated, read-only final copy with matching source/copy SHA. The authoritative final SHA is `f5f8492ecf5279409697c7a5dd69ce6e66a5b013c05e8858d592601216fa7ccd`; the earlier artifact and its evidence remain separately retained. This avoids claiming a live result for an ambiguous build artifact.

## Decision Log

Decision: Use the JDK's PBKDF2WithHmacSHA256 and Python's standard-library hashlib.pbkdf2_hmac with 600000 iterations, a fresh 16-byte salt, and a 32-byte derived hash. Rationale: OWASP recommends this PBKDF2 work factor; NIST requires salted hashing and stored algorithm/cost metadata. Argon2id is generally preferred, but PBKDF2 avoids new cryptographic, native, and Python dependencies here. This does not claim a FIPS-validated deployment. Sources verified by the parent coordinator are https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2 and https://pages.nist.gov/800-63-4/sp800-63b.html. Date/Author: 2026-10-17, user-approved scope and Codex.

Decision: The same property accepts `{pbkdf2-sha256}600000:<base64 salt>:<base64 hash>`. The salt must decode to at least 16 bytes, the hash to exactly 32 bytes, and the decimal iteration count must be between 600000 and 2147483647. Base64 uses the standard RFC 4648 alphabet and canonical padding. Rationale: the format records algorithm and work factor, supports future increases, and rejects ambiguous or damaged configuration. Date/Author: 2026-10-17, Codex.

Decision: A value beginning with a brace is an encoded configuration. Unknown or malformed encodings fail startup with safe generic errors; `{plain}` explicitly escapes a literal password, including one beginning with a brace. Empty properties and empty escaped passwords remain invalid. Rationale: silently treating a malformed hash as plaintext would create a different login credential than the administrator intended. Date/Author: 2026-10-17, user-approved contract.

Decision: Keep all passwords exact, including Unicode supplementary characters, NUL, spaces, and newlines. No trimming or normalization is allowed. The verifier itself is never a login credential. Missing and empty submissions fail protected hash policies. Date/Author: 2026-10-17, user-approved contract.

Decision: The generator has no password argument or environment option. Interactive input uses hidden, confirmed terminal input and fails closed without a terminal; `--password-stdin` reads all strict UTF-8 bytes exactly for automation. Date/Author: 2026-10-17, user-approved contract.

## Outcomes & Retrospective

The initial offline root clean install passed in 55.074 seconds. Before production edits, `RepositoryDeletionPolicyTest#acceptsIndependentlyDerivedHash` failed with one Surefire assertion failure: the original password was unauthorized against a valid independently derived verifier. The report and full log are archived under `logs/repository-deletion-password-hashing/red-policy`, and `initial-evidence.txt` was appended. The Python black-box generator regression also failed before its entrypoint existed. Focused verification passes: RepositoryDeletionPolicyTest 14, DeleteServletPasswordTest 29, and WorkbenchDeletionStartupTest 3, totaling 46 passed with no failures, errors, or skips. Prior feature evidence remains intact.

The completed generator suite passes 14 tests, including real 600001-iteration derivation, exact Unicode/NUL/newline input, random salts, no-terminal failure, safe invalid arguments, and GetPassWarning handling. A separate four-test terminal selection passes with ECHO disabled before both entries and no secret in stdout, stderr, or PTY output. Logs are `generator/python-green-review-complete.log` and `generator/pty-smoke-review-final.log`. Parent static review found no remaining crypto, parser, script, or documentation blocker.

Final serialized Maven validation passes 1241 tests with no failures, errors, or skips: full manager 27, server-spring 188, Workbench 1018, RepositoryDeletionHashIT 3, and existing plaintext RepositoryDeletionPasswordIT 5. The focused 46 are a subset and are not added to this total. The new real HTTP IT proves Unicode/NUL passwords agree with an independently derived verifier across direct deletion, SYSTEM autocommit and transaction START authorization; missing, wrong, and verifier-as-password submissions receive typed 403 and cause no deletion. Existing plaintext, corrupt-config deletion, and ordinary data-operation controls also pass. Copyright, formatter, OpenAPI YAML parse, Java signature audit, and git diff whitespace checks pass.

Actual generator-to-Server interoperability and the existing Chromium deletion flow pass against the final Boot artifact. The synthetic password is `proof-hash-🔑 ` with a trailing space; the generator's random verifier is the only configured JVM value, confirmed using jcmd. Protocol HEAD reports required=true. Both ordinary DELETE and SYSTEM context removal reject missing, wrong, and verifier-as-password credentials with safe 403 responses and retain the repository; the original password succeeds with 204 and independently verified inventory absence. The Chromium spec passes one complete flow, covering visible wrong-password retry, retained repository picker, blank masked input, typed confirmation, keyboard/focus, stale ownership, history and desktop/mobile behavior. Screenshots were inspected and no application console/page errors were observed by the spec.

The live evidence is `live/live-run-final-summary.json`, `live/http-delete-interoperability-final.txt`, `live/jvm-property-verification-final.json`, and `live/ui-delete-password-final-20261017-101208/`. Verification uses Chromium only on macOS; the JDK fixture requires `-Djava.net.preferIPv4Stack=true`. The Windows terminal guard was reviewed but not exercised on a Windows runtime. The unchanged protocol/client full suites, Node suite and four original deployment combinations retain their earlier feature evidence and were not redundantly rerun. The Playwright trace contains public synthetic input-action data; no user's credential was requested or collected. No implementation or validation blocker remains.

Operational cleanup is complete: task-owned old PID 83209 and final PID 844 stopped gracefully, and port 18188 refuses connections. All app-data, immutable jars, logs, traces, and screenshots remain preserved. `live/shutdown-final.json` records the final termination and listener check; `live/live-run-final-summary.json` now records retainedServer=false. Generator red/green, terminal, and live evidence were appended once to `initial-evidence.txt`. The checkout remains on develop with all cohesive feature work uncommitted; no dependency, branch, commit, or push was added. The parent reviewed the final evidence, source scope, documentation, and clean whitespace check, and delivered the completed feature.

## Context and Orientation

The checkout is `/Users/havardottestad/Documents/Programming/rdf4j-6`, branch develop. `core/repository/manager/src/main/java/org/eclipse/rdf4j/repository/manager/RepositoryDeletionPolicy.java` captures startup configuration and exposes `fromSystemProperty()`, `isPasswordRequired()`, `isAuthorized(String)`, and `checkPassword(String)`. Server's singleton policy is wired in `tools/server/src/main/webapp/WEB-INF/rdf4j-http-server-servlet.xml`. WorkbenchGateway captures one policy per application context; DeleteServlet uses it for local managers. Remote managers ask the remote Server instead.

The existing HTTP request header carries base64 of the actual entered UTF-8 password. Its semantics do not change. Server's direct deletion handler checks the policy before manager removal; SYSTEM requests retain only the authorization decision at transaction START and preflight all deletions before configuration side effects. The policy extension must preserve those boundaries and safe typed HTTP 403 errors.

## Plan of Work

First the designated Luna runner performs the required root quick clean install with all source editing frozen. Add a focused existing-API test to `core/repository/manager/src/test/java/org/eclipse/rdf4j/repository/manager/RepositoryDeletionPolicyTest.java` using a pinned verifier independently computed with Python at the real 600000 work factor. It must accept the actual password and reject missing, wrong, empty, and stored-verifier submissions. Run that test against the current plaintext policy, preserve the Surefire failure, append evidence, and notify the parent before production edits.

Next extend the immutable policy with strict configuration parsing and built-in PBKDF2 verification. Hash mode retains only salt, hash, and work factor. Verification clears temporary character arrays, derived bytes, and PBEKeySpec password storage. Parse errors never include the configured value or an unsafe nested exception. Expand policy tests across Unicode, whitespace, NUL, long passwords, malformed formats, startup snapshots, legacy plaintext, and explicit plain escape. Add local Workbench hashed-policy and malformed-startup tests without frontend changes.

In parallel after the same red gate, the generator worker creates `scripts/generate-repository-delete-password.py` and `scripts/tests/test_generate_repository_delete_password.py`. The worker records a Python red before implementing its script, verifies the CLI independently, and performs an actual terminal smoke test using public synthetic credentials. Script tests remain separate from Maven so Python is not required for ordinary Java unit tests.

Add real HTTP integration coverage under `tools/server/src/test/java/org/eclipse/rdf4j/http/server/RepositoryDeletionHashIT.java`. A Server configured with a generated verifier must advertise required=true, reject missing, wrong, and verifier-as-password requests, accept the original password, and enforce SYSTEM direct and transaction deletion. Keep existing plaintext integration controls. Run relevant focused tests, then full manager, server-spring, and Workbench suites. Unchanged protocol/client and frontend full suites need not repeat. If practical, run the existing Chromium deletion flow once against a hashed Boot deployment with an immutable temporary jar copy and the established macOS IPv4 fixture workaround.

The completed guide at `site/content/documentation/tools/server-workbench.md` and REST description at `site/static/documentation/reference/rest-api/rdf4j-openapi.yaml` explain the recommended verifier format, generator command and quoting, exact password semantics, `{plain}` escape, and distinction between stored verifier and entered password. Copyright, formatting, whitespace and document checks passed through the serialized build lane. Evidence consolidation and graceful task-owned runtime shutdown are complete, with logs and data preserved.

## Concrete Steps

All commands run from the checkout root unless stated. Builds use Java 25 or newer and the workspace-local Maven repository:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Preserve full build output in `maven-build.log`. Focused and full selections use the repository skill, with no tests `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py RepositoryDeletionPolicyTest#acceptsIndependentlyDerivedHash --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/repository/manager --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/server-spring --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py tools/workbench --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --it RepositoryDeletionHashIT --module tools/server --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --it RepositoryDeletionPasswordIT --module tools/server --retain-logs
    python3 -m unittest discover -s scripts/tests -p test_generate_repository_delete_password.py -v

The generator's intended interactive command is:

    python3 scripts/generate-repository-delete-password.py

It prints one verifier after two hidden matching entries. Automation uses `--password-stdin`; trailing newlines are part of that password. Install the verifier as a single shell-quoted JVM option, while users continue entering the original password.

The final actual browser command ran from `e2e` with the public synthetic fixture:

    RDF4J_WORKBENCH_BASE_URL=http://127.0.0.1:18188/rdf4j-workbench RDF4J_SERVER_BASE_URL=http://127.0.0.1:18188/rdf4j-server RDF4J_DELETION_PASSWORD='proof-hash-🔑 ' npx playwright test tests/workbench-delete-password.spec.js --project=chromium --workers=1 --trace=on

It passed one Chromium flow. The read-only executable jar copy is `/tmp/rdf4j-delete-password-final-9bggk7hh/rdf4j-server-boot-6.2.0-SNAPSHOT.jar`; its matching hash and ZIP validation are recorded in `live/server-jar-snapshot-final.json`. The fresh app-data directory is `/tmp/rdf4j-delete-password-final-9bggk7hh/appdata`. The server and cohosted Workbench were available on port 18188 during verification; these are fixture URLs, not deployment instructions.

## Validation and Acceptance

The first focused Java test must fail against the original plaintext policy, then pass without weakened assertions. Independent Python fixtures must demonstrate Java and Python agree for Unicode supplementary characters, whitespace, NUL, and long passwords at the real work factor. Unknown encoders, malformed counts and base64, short salts, wrong hash sizes, and empty protected inputs must fail safely. Startup snapshots must remain immutable and retain no raw password in hash mode.

Local Workbench must accept the actual password against a verifier and reject a malformed verifier at startup. Real HTTP checks must prove the property contains a verifier rather than the password, discovery reports required=true, and direct and SYSTEM deletion reject missing/wrong/verifier credentials while accepting the original password. Ordinary plaintext, unset, data-operation and remote-authority controls remain green. Generator tests and terminal interaction must prove no password echo or accidental fallback, exact stdin handling, fresh salts, and no secret-bearing traceback. Report focused/full suite totals and distinguish real HTTP/browser evidence from mocked Java coverage.

## Idempotence and Recovery

Keep all prior tracked and untracked work. No commits, pushes, branch changes, resets, cleanups, or broad replacements are authorized. Serialize Maven and formatting through one runner and freeze sources during builds. Archive each first report before another run can overwrite it, appending rather than replacing `initial-evidence.txt`. Missing offline dependencies allow one identical online retry; other initial build failures receive the instructed single-thread retry. Temporary application jars must be immutable copies so Maven cannot replace running archives. Stop task-owned processes gracefully and preserve their logs and app-data.

## Artifacts and Notes

New logs and archived reports use `logs/repository-deletion-password-hashing`. `red-policy` preserves the original Surefire failure; `focused-policy/green`, `focused-workbench-delete`, and `focused-workbench-startup` preserve focused greens. `full-core-repository-manager`, `full-tools-server-spring`, `full-tools-workbench`, `it-repository-deletion-hash`, and `it-repository-deletion-password` contain final module and HTTP reports. Generator logs and independently derived fixture expressions are under `generator`; actual generated-verifier, JVM property, HTTP, immutable-jar and Chromium evidence is under `live`. `final-source-sha256.txt` records the audited source hashes. Top-level `initial-evidence.txt` retains previous feature evidence and extension results. Public synthetic fixture passwords/verifiers may appear in test evidence; no user's password was collected.

## Interfaces and Dependencies

No public Java method signatures change and no external dependencies are added. Java uses `javax.crypto.SecretKeyFactory`, `javax.crypto.spec.PBEKeySpec`, and `java.security.MessageDigest.isEqual`. Python uses `hashlib`, `secrets`, `base64`, `getpass`, and standard CLI/testing libraries. The property grammar is shared through documentation and independent test fixtures, not by introducing a runtime Python dependency into Java tests.

Revision note (2026-10-17): Created this extension plan after the completed plaintext feature to preserve its existing implementation and verification history while adding salted-verifier configuration and a secure generator.

Revision note (2026-10-17): Recorded observed Surefire/Python reds, dependency-free test harness repairs, strict policy and generator completion, focused/full suite totals, independent static review, stronger PTY assertions, final-artifact actual HTTP/Chromium evidence, and completed graceful fixture shutdown. Implementation, validation, cleanup, and final parent review are complete.
