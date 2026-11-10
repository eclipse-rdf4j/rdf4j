# Stream server transaction XML operations

This ExecPlan is a living document maintained under `.agent/PLANS.md`.

## Purpose / Big Picture

The RDF4J server currently parses a complete transaction XML request into an in-memory list before beginning and executing the transaction. A client can make that list grow with the request size. After this change, the server first stages and validates a bounded transaction XML document on disk, then executes each complete operation during a second parse within one repository transaction. A later malformed XML document cannot trigger earlier operations on backends with nontransactional calls. Existing callers of the collection-returning `TransactionReader.parse` methods retain their behavior.

## Progress

- [done] Inspect parser and server transaction flow
- [done] Complete root quick clean install
- [done] Capture smallest failing server regression
- [done] Add streaming parser consumer path
- [done] Execute and roll back server operations
- [done] Guard namespace operation completion
- [done] Verify focused and module suites
- [done] Capture irreversible side-effect regression
- [done] Prevalidate bounded temporary transaction spool
- [done] Replay validated operations in transaction
- [done] Reverify suites and format scope
- [done] Review scoped candidate with parent
- [in_progress] Commit and publish reviewed change

## Surprises & Discoveries

- `TransactionSAXParser.startTag` emits namespace operations, while `endTag` emits statement, clear, and SPARQL update operations. A shared emitter must cover both callbacks in wire order.
- The initial implementation preserves that start-tag timing, but nonempty namespace elements can invoke the consumer before their end tag. A focused regression will pin complete-element callback semantics before adjusting this path.
- Review found `HTTPRepositoryConnection` namespace operations can immediately call its HTTP client even when a transaction is active. Therefore rollback cannot guarantee that one-pass streaming execution leaves zero effects after malformed later XML. The one-pass candidate is held from publication.

## Decision Log

- Decision: Keep the public collection-returning reader methods as adapters and add a consumer overload for server use. Rationale: existing callers keep their API and ordering, while the server avoids whole-request storage. Date/Author: 2026-10-23 Codex.
- Superseded decision: Begin before parsing and execute each SAX-complete operation. Review showed that rollback does not undo immediate namespace calls on some proxy backends. Date/Author: 2026-10-23 Codex.
- Decision: Supersede one-pass server execution with parse-before-execute using a bounded, temporary disk spool and a second streaming parse. Rationale: valid XML must be established before any possibly nontransactional backend call; disk replaces the unbounded heap list and a configured byte budget constrains the spool. Date/Author: 2026-10-23 Codex.

## Outcomes & Retrospective

The one-pass candidate passed 39 protocol tests and 208 server tests, but review found an atomicity gap for proxy repository namespace calls. A new focused test observed an irreversible namespace effect after later malformed XML (`side-effect-initial-evidence.txt`), before any spool production change. The server now copies no more than the configured XML byte limit to a temporary file, validates from that file before opening a connection, and replays the validated bytes with a streaming consumer. The 10-test controller class and 211-test full server module pass, including malformed, oversized, rollback, cleanup, and success cases. `initial-evidence.txt` and `namespace-initial-evidence.txt` preserve the earlier red reports; the first test's former expectation of early execution was replaced by the stronger no-side-effects assertion.

## Context and Orientation

`core/http/protocol/src/main/java/org/eclipse/rdf4j/http/protocol/transaction/TransactionReader.java` creates `SimpleSAXParser` and `TransactionSAXParser`. The latter currently appends each parsed `TransactionOperation` to a `txn` list. `tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/statements/StatementsController.java` handles `POST` with `Protocol.TXN_MIME_TYPE`, parses the list, then calls `begin`, `execute`, and `commit`. A SAX operation is complete when its element closes; namespace operations use empty elements and are emitted in `startTag`.

## Plan of Work

The first milestone added a callback to `TransactionReader` and replaced parser list retention. The server currently executes in one pass, but that must be replaced. Add a red server test in `TestStatementsController` where an early namespace call records an irreversible effect and later malformed XML must leave that effect absent. Then copy the request to a temporary file with a configurable total byte limit, parse it once with a discarding consumer to validate the complete XML, and only then begin a repository transaction and parse the file again with an executing consumer. Clean up the temporary file on every path; preserve existing HTTP exception mapping. Retain tests for callback order and successful operation order, but update any test that assumed early execution on malformed XML to the stronger zero-effects requirement. Run focused tests, both affected module suites, copyright and formatter, then review and publish a scoped draft PR.

## Concrete Steps

In this worktree `/Users/havardottestad/.codex/worktrees/security-transaction-streaming/rdf4j`, the required root quick clean install completed successfully in `maven-build.log`. Run `python3 .codex/skills/mvnf/scripts/mvnf.py TestStatementsController#<new method> --workspace transaction-streaming --retain-logs`; capture the generated Surefire summary with `scripts/agent-evidence.py` in `initial-evidence.txt`. After implementation, run focused selections and `python3 .codex/skills/mvnf/scripts/mvnf.py core/http/protocol --workspace transaction-streaming --retain-logs` and the same for `tools/server-spring`. Run `scripts/checkCopyrightPresent.sh` from `scripts` and `mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources` for formatting. Do not use `-am` or `-q` with tests.

## Validation and Acceptance

The original pre-fix test demonstrated that the server parsed before beginning. The final server behavior must preserve that parse-failure zero-effects property, without retaining all operations in heap. A valid multi-operation request must execute in document order and commit once; the reader callback/input boundary test must show that the protocol parser itself streams when used directly. The server must reject an oversized transaction before any backend operation and remove its temporary file. Existing reader roundtrips and both module suites must remain green.

## Idempotence and Recovery

Tests and quick installs can be repeated in this isolated worktree. Keep initial evidence and retained logs so later test runs do not erase pre-fix proof. Stage only files changed for this issue. If a build fails from an offline dependency, retry the same command once online; for another build failure, retry without Maven parallelism as AGENTS.md instructs.

## Artifacts and Notes

The initial install ended with `BUILD SUCCESS` on 2026-10-23 and the full output remains in `maven-build.log`. The pre-fix server test reported `Tests run: 1, Failures: 1` and `Wanted but not invoked: repositoryConnection.begin()` in `initial-evidence.txt`; its retained log is `.mvnf/workspaces/transaction-streaming/logs/20261023T002829.331448Z-24853-171f11f8/verify.log`. The namespace timing failure is in `namespace-initial-evidence.txt` and log `.mvnf/workspaces/transaction-streaming/logs/20261023T004222.474617Z-85060-28b50d0d/verify.log`. The irreversible-side-effect failure is in `side-effect-initial-evidence.txt` and log `.mvnf/workspaces/transaction-streaming/logs/20261023T005342.217607Z-10042-6ffcd98b/verify.log`. Final passing logs are `.mvnf/workspaces/transaction-streaming/logs/20261023T004615.943158Z-19163-b41e57b7/verify.log` (protocol, 39 tests) and `.mvnf/workspaces/transaction-streaming/logs/20261023T010110.979516Z-51767-bb7d67f2/verify.log` (server, 211 tests). Final formatting is in `formatting-transaction-spool.log` (`BUILD SUCCESS`); final copyright validation is in `copyright-transaction-spool.log` (`All files have valid copyright headers and SPDX lines.`). `git diff --check` is clean. The default 256 MiB XML limit may require adjustment for larger valid transactions; one large operation can still require parser memory proportional to that operation.

## Interfaces and Dependencies

Use existing `java.util.function.Consumer<TransactionOperation>` or a small project-appropriate callback in the protocol module; no new dependency is needed. The existing `SimpleSAXParser` listener can throw `SAXException`; callback execution failures must preserve the underlying `RepositoryException` for the controller's existing HTTP error mapping. The request InputStream belongs to the servlet and must not be closed by the reader.

Change note, 2026-10-23: Created this plan after confirming the initial install and source flow, before writing a regression test or production code.
Change note, 2026-10-23: Recorded the observed red Surefire result and advanced the single active step to the parser consumer path.
Change note, 2026-10-23: Completed the consumer and server changes, observed the original test green, and advanced to broader test verification.
Change note, 2026-10-23: Full affected module suites passed, but review found namespace callback timing before end tags for nonempty elements. Added a focused completion step before final verification.
Change note, 2026-10-23: Preserved a second red report, deferred namespace callbacks until end tags, and observed the reader class green. Returned to final module verification.
Change note, 2026-10-23: Final full module suites passed after the namespace correction; advanced to formatting, copyright, and scoped review before publication.
Change note, 2026-10-23: Final formatting, copyright, and whitespace checks passed; recorded exact evidence paths and the per-operation memory limit for review.
Change note, 2026-10-23: Parent review identified a concrete nontransactional proxy namespace path, invalidating one-pass server atomicity. Superseded the one-pass outcome and added red-first, bounded-spool milestones before publication.
Change note, 2026-10-23: The irreversible-side-effect regression failed against the one-pass candidate and was persisted. Advanced to the bounded spool and prevalidation implementation.
Change note, 2026-10-23: The bounded spool and replay path passed the focused controller class. Reconciled the original malformed-input test with the stronger invariant and advanced to full module verification and final checks.
Change note, 2026-10-23: The full server module passed 211 tests; updated evidence paths and retained final format/copyright as the remaining verification step.
Change note, 2026-10-23: Rechecked the final six-file diff, 39 protocol and 211 server tests, formatter, copyright, and whitespace. Submitted the parse-before-effects bounded-spool candidate for parent review.
Change note, 2026-10-23: Parent reviewed and approved the revised bounded-spool candidate for a scoped draft PR, with explicit medium compatibility impact for valid bodies above 256 MiB and residual one-operation and concurrent-spool memory/disk considerations.
