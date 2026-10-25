# Keep saved-query authorization and text bound to one query

This ExecPlan is a living document governed by `.agent/PLANS.md`.

## Purpose / Big Picture

A Workbench saved query can currently be authorized by one RDF resource and read by an independent lookup on repository, owner, and name. Duplicate records can cause the text returned to a user to belong to a different, private query. A saved-query link must return, execute, download, and prefill only text authorized for the exact query resource being read. Authorized query listing and existing HMAC-owned or shared queries must keep working.

## Progress

- [x] (2026-10-23) Trace storage and servlet reads.
- [x] (2026-10-23) Run isolated quick install.
- [x] (2026-10-23) Add and capture smallest failing regression.
- [x] (2026-10-23) Capture legacy and anonymous ownership regressions.
- [x] (2026-10-23) Reproduce anonymous name collision.
- [x] (2026-10-23) Fix atomic same-resource authorization and text retrieval.
- [x] Verify direct, execution, download, and listing paths.
- [x] Format and run focused and module tests.
- [x] Review diff and report compatibility risk.
- [ ] (in_progress) Publish draft and verify exact checks.

## Surprises & Discoveries

`QueryServlet.getQueryText` calls `selectSavedQuery` and `canRead`, then `QueryStorage.getQueryText` uses `SELECT_TEXT`, which independently selects the first text matching a tuple. `QueryStorage.saveQuery` creates random query IRIs and has no uniqueness constraint on that tuple. Existing tests deliberately preserve legacy plain-username ownership; its security and migration implications must be distinguished from this record-identity fix.

Red Surefire reports also prove that a different password for the same user name can claim an older private query and that an empty owner makes an older private query readable to an anonymous request. Those records contain no verifier capable of proving which credentials created them.

## Decision Log

- Decision: Retain a deterministic duplicate-record test and bind text retrieval and the read policy in one storage query by selected IRI. Rationale: two separate repository queries or a second lookup by owner/name can observe a different row or access state. Date/Author: 2026-10-23 Codex.
- Decision: Keep the legacy-owner migration policy separate unless a concrete test proves it can be changed without invalidating legitimate saved queries. Rationale: old records cannot prove which password created them; changing the alias would require a migration decision. Date/Author: 2026-10-23 Codex.
- Decision: Deny automatic private legacy ownership and permit shared legacy reads; retain anonymous shared-query editing because it is already a public operation. Rationale: the red tests prove private exposure, while old private records cannot be safely reassigned. A trusted administrator can migrate those records after verifying ownership out of band. Date/Author: 2026-10-23 Codex.

## Outcomes & Retrospective

The Workbench module passed 1336 tests with no failures or errors. A saved-query link now chooses a readable RDF resource before retrieving text, and the text query rechecks access on that exact resource. Private records owned only by a historical plain user name or an empty owner are no longer exposed or mutable through Workbench. This is a moderate compatibility change: operators must verify ownership and re-create those old private records under credential-derived owners. Shared queries and current private queries remain usable. Duplicate owner/name records can still arise from concurrent saves; the read path filters them by access and orders readable resources deterministically, while a separate uniqueness migration remains outside this security fix.

## Context and Orientation

`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/QueryStorage.java` stores each saved query as an RDF resource. A query IRI is its unique record identity; repository, owner, and name are searchable attributes and need not be unique. `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/QueryServlet.java` handles links and actions, including `ref=id`. `SavedQueryIdentity.owner` derives a server-keyed identifier from request-bound credentials; `legacyOwner` maps it to a historical plain username. `QueryStorageTest` exercises a `MemoryStore`; `QueryServletTest` exercises the request routes.

## Plan of Work

First add a deterministic test that presents a shared and a private record with the same owner and name, then demonstrates that the existing servlet path can return the private text after authorizing the shared IRI. Retain the initial Surefire report before production changes. Implement a storage read whose SPARQL pattern binds the selected query IRI, text, owner, and shared flag in one query and applies the same read filter there. Update the servlet to use that result in every text-consuming path. Add storage-backed duplicate tests and route tests for edit, execution, download, prefill, and listing. Preserve existing query-save behavior except where necessary to keep the read boundary sound.

## Concrete Steps

From the managed worktree root, run `python3 scripts/mvn-agent.py --workspace saved-query-acl -- -B -ntp -Pquick clean install` once for dependencies. Run `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace saved-query-acl --retain-logs QueryServletTest#<red-method>` and save its compact Surefire report to `initial-evidence.saved-query-acl.txt`. After the implementation, rerun the same selector and then `QueryStorageTest`, `QueryServletTest`, and `tools/workbench` without `-am` or `-q` during tests. The isolated reports live under `.mvnf/workspaces/saved-query-acl/build/org.eclipse.rdf4j/rdf4j-workbench/6.2.0-SNAPSHOT/surefire-reports/`.

## Validation and Acceptance

The pre-fix test must show a private text returned after a different IRI was authorized; the post-fix test must prove the returned text belongs to an authorized IRI. Existing private HMAC-owned queries and shared queries remain readable to their intended users. A private query never becomes visible in the saved-query list or any direct `ref=id` response to another user. The full Workbench module has zero new failures or a specific explained blocker.

## Idempotence and Recovery

The test runner keeps each run's logs in its named workspace. `initial-evidence.saved-query-acl.txt` retains the first red report because subsequent test runs replace Surefire files. Retain all untracked evidence and stage only reviewed source and this plan. If offline resolution misses a dependency, retry the same command once online, then return to offline operation.

## Artifacts and Notes

Initial install succeeded in the isolated workspace after the allowed online retry for `maven-dependency-plugin:3.11.0`. The red report is retained in `initial-evidence.saved-query-acl.txt`: one test, one failure, expected shared text but observed private text from a duplicate tuple.

The ownership red report is retained in `initial-evidence.saved-query-ownership.txt`: two tests, two failures for different-password and empty-owner private reads.

`initial-evidence.saved-query-collision.txt` captures the empty-owner collision (`askExists` returned true for an inaccessible private record). `initial-evidence.saved-query-order.txt` captures a private-first duplicate denying a readable shared link.

The final `tools/workbench` run completed with 1336 tests, zero failures, zero errors, and zero skips. Its retained log is `.mvnf/workspaces/saved-query-acl/logs/20261023T014940.061237Z-42133-1a68386c/verify.log`; `QueryStorageTest` had 21/0, `QueryServletTest` 90/0, and `QueryServletFlowCoverageTest` 9/0. The focused `QueryServletFlowCoverageTest#doPostRejectsUnreadableEditExplainAndMissingCancelIds` rerun also passed 1/0 after its mock matched the credential-derived owner ID. The repository formatter, copyright check, and `git diff --check` completed.

## Interfaces and Dependencies

Use existing RDF4J `RepositoryConnection`, `TupleQuery`, and `TupleQueryResult` APIs. A selected IRI should be passed to a storage operation that verifies the current principal and returns text from that same IRI in one evaluated tuple query. Do not add dependencies or public request parameters.

Revision note, 2026-10-23: Created after source tracing and the initial build to record the root cause, red-first sequence, and authorization boundary.

Revision note, 2026-10-23: Updated after retaining the first failing Surefire report; implementation is now the only in-progress step.

Revision note, 2026-10-23: Added explicit ownership-boundary red tests before production edits because the historical alias and empty-owner filters expose additional private records. Only this test capture is now in progress.

Revision note, 2026-10-23: Persisted both ownership failures and the decision to retire insecure private legacy claims; implementation is in progress.

Revision note, 2026-10-23: Focused storage tests reached 20/0, but a source review found that an old private empty-owner row can mask a new anonymous shared row with the same name. This collision is now being reproduced before changing the name lookup.

Revision note, 2026-10-23: Retained collision and private-first Surefire failures; the next implementation step must select readable records by their own IRI and keep anonymous name lookup limited to shared records.

Revision note, 2026-10-23: Implemented readable record selection and same-IRI authorized text read, updated legacy ownership policy, passed 90 servlet tests, and moved to full verification.
