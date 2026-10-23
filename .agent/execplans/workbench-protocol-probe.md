# Bound Workbench protocol probes to accepted destinations

This ExecPlan follows `.agent/PLANS.md` in the isolated worktree `/Users/havardottestad/.codex/worktrees/security-workbench-validator/rdf4j`, branch `GH-0000-workbench-protocol-probe`, stacked on the open #6210 Workbench prefix containment PR. It is a separate Workbench change from the Server outbound policy pull requests. The initial offline root quick clean install passed and its full output is in `maven-build.log`.

## Purpose / Big Picture

An operator can permit an absolute RDF4J Server URL in Workbench with `accepted-server-prefixes`. Workbench verifies the selected backend by reading its `/protocol` response. Today that verification follows HTTP redirects automatically and reads the entire first line without a limit. A permitted endpoint can therefore send the Workbench process to an unapproved destination, and an oversized first line can consume memory. After this change, each redirect target must itself match an accepted prefix before any request to it, and a protocol version must arrive within finite time and a small line budget. A direct or approved redirected endpoint with a normal numeric version should remain usable.

## Progress

- [done] prepare isolated branch and build (2026-10-23 09:39Z)
- [done] reproduce redirect and line issues (2026-10-23 09:46Z)
- [in_progress] align #6210 and rerun red
- [todo] implement per-hop bounded probe
- [todo] verify Workbench compatibility variants
- [todo] format review and publish draft
- [todo] monitor exact published checks

## Surprises & Discoveries

`ServerValidator.isValidServer` first checks the selected URL against `accepted-server-prefixes`, then `canConnect` calls `new URL(server + "/protocol").openStream()` and `BufferedReader.readLine()`. The default accepted prefix is the relative `/rdf4j-server`; arbitrary remote selection requires an administrator to configure an absolute prefix. Java URL connections can follow redirects before Workbench checks the destination. The current line read has no Workbench byte or character budget. On current main, the first focused Surefire test failed 1/1 because the unapproved redirect still made validation true (`initial-evidence.txt`); the second failed 1/1 because 65 zeroes still made validation true (`line-initial-evidence.txt`).

## Decision Log

- Decision: Validate each probe redirect against the same configured prefix policy before opening it. Rationale: an administrator can deliberately list another trusted endpoint, while a redirect to an unlisted private address or file URL remains unapproved. Date/Author: 2026-10-23, Codex.
- Decision: Keep the existing numeric first-line protocol contract and add a small fixed character budget plus finite connection/read deadlines. Rationale: a protocol version fits in a few characters, while unlimited reading and indefinite waits are unnecessary for validation. Date/Author: 2026-10-23, Codex.
- Decision: Stack this fix on #6210. Rationale: #6210 is still open and changes `ServerValidator` and its test class for canonical file-prefix behavior; using its head lets this PR share the current accepted-prefix boundary without conflicting parallel edits. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

Not complete. The initial build is green. Two focused test methods are red on main before production edits and their Surefire reports are preserved. The test-only branch must be rebased onto #6210 and the focused red rerun there before production changes.

## Context and Orientation

`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/proxy/ServerValidator.java` validates a Workbench backend selection. Its `checkServerPrefixes` method applies administrator-provided URL prefixes. Its `canConnect` method performs the HTTP probe. `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/proxy/ProxyUtilityCoverageTest.java` already uses a local `HttpServer` to exercise valid, invalid, and default-prefix selections. The proposed tests should use two test-owned loopback servers: the first is accepted and redirects, while the second counts whether it was contacted. These loopback fixtures simulate an unapproved private destination; no Internet service is needed.

## Plan of Work

First add the smallest test in `ProxyUtilityCoverageTest` where an accepted first server redirects `/protocol` to a second server that is absent from the accepted prefixes. The test must assert validation fails and the second server receives no request. Run only that method using `mvnf --retain-logs`, preserve the Surefire failure in top-level `initial-evidence.txt`, and do not edit production until that report exists. Add a second red case where a syntactically valid numeric first line exceeds the intended budget, then positive direct and same-origin redirect cases.

Next change `ServerValidator.canConnect` to open HTTP connections with automatic redirects disabled. Resolve each `Location` against the current URL, accept only HTTP(S) targets that match `checkServerPrefixes`, and reject malformed, unlisted, or excessive redirect chains before opening another connection. Set finite connect and read timeouts. Read at most the version-line budget plus one character before parsing it as an integer, then disconnect each connection. Preserve the existing behavior for direct and explicitly accepted endpoints. No global RDF4J HTTP client or embedded library behavior should change.

Finally run focused tests, the Workbench Java module suite, copyright and formatter checks, and `git diff --check`. Review the actual Workbench backend selection path to ensure a rejected probe cannot be used as a valid selected server. Give the parent a scoped diff and red/green evidence for independent review before publishing a draft PR from this branch. After publication compare local, remote, and PR head SHAs, ECA, and every exact-head CI check.

## Concrete Steps

Run from `/Users/havardottestad/.codex/worktrees/security-workbench-validator/rdf4j`:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-validator --retain-logs ProxyUtilityCoverageTest#serverValidatorRejectsRedirectOutsideAcceptedPrefix
    python3 scripts/agent-evidence.py --command '<same mvnf command>' <Workbench Surefire report directory> > initial-evidence.txt

The first command should fail before production changes. `mvnf` uses the private Maven write head and performs the required quick install. Tests must not use Maven `-am` or `-q`. After the fix, run the new methods, the class, then `tools/workbench` with `mvnf --retain-logs`. Run `scripts/checkCopyrightPresent.sh` and offline Maven `process-resources` without `-q` before publication.

## Validation and Acceptance

With only the first origin configured, its redirect to another loopback origin must not cause any request to the second origin. A same-origin redirect under the accepted prefix and a direct numeric response must pass. An oversized numeric first line must fail without reading the whole line. Malformed or unsupported redirect locations and redirect loops must fail. Prefix rejection must remain before any network request. The existing relative default and an explicitly configured local private backend must remain valid.

## Idempotence and Recovery

All HTTP fixtures bind ephemeral local ports and stop in `finally`. Preserve untracked evidence and logs; never clean, reset, or broadly stage this worktree. If offline Maven resolution fails for a missing artifact or plugin, rerun the same command once online, then return offline. If a red test fails because its fixture did not serve a redirect, fix the fixture and rerun before attributing it to production behavior.

## Artifacts and Notes

`maven-build.log` has the initial build. `initial-evidence.txt` will retain the first in-repo Surefire failure, while later compact evidence and full Maven logs stay in this worktree.

## Interfaces and Dependencies

Use JDK `HttpURLConnection` and `URL` for the narrow Workbench probe; no new dependency is needed. Keep `ServerValidator.isValidServer(String)` as the caller-facing validation result. The redirect helper should use the existing `checkServerPrefixes` policy and must never call `getInputStream()` on an unapproved target. A bounded first-line reader should retain `Integer.parseInt` acceptance for ordinary numeric versions.

Plan change note (2026-10-23): created after the isolated baseline build and source trace, before any test or production edit. Updated after two focused red reports and the decision to stack on #6210's accepted-prefix implementation.
