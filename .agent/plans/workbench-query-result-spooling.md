# Stream Workbench legacy query results with bounded memory

This ExecPlan is a living document maintained under `.agent/PLANS.md`.

## Purpose / Big Picture

Large legacy Workbench tuple and graph query responses currently retain every result in Java lists before rendering. A remote query can exhaust the Workbench heap even when the page shows only a small window. After this change, known-total, unlimited, and download responses emit rows as they arrive. Unknown-total bounded browser pages count once while storing only their selected window in a private temporary disk file until the paging cookie can be set. Rows, order, result count metadata, and download limits remain the same. Unlimited legacy pages and downloads no longer set a total-count cookie.

## Progress

- [done] 2026-10-23 02:48Z Inspect query and writer semantics.
- [done] 2026-10-23 02:54Z Add failing tuple and graph streaming regressions.
- [done] 2026-10-23 03:05Z Implement bounded disk result replay.
- [done] 2026-10-23 03:08Z Cover paging and failure variants.
- [done] 2026-10-23 03:14Z Run focused and module verification.
- [done] 2026-10-23 03:20Z Reproduce unlimited download disk risk.
- [done] 2026-10-23 03:23Z Resolve download count and spool policy.
- [done] 2026-10-23 03:38Z Stream unlimited legacy pages.
- [done] 2026-10-23 03:42Z Verify BNode output and rerun tests.
- [done] 2026-10-23 03:51Z Review revised candidate and handoff.
- [done] 2026-10-23 04:10Z Reproduce bounded spool budget risk.
- [done] 2026-10-23 04:11Z Implement shared disk budget and cleanup.
- [done] 2026-10-23 04:15Z Verify XML metadata and module tests.
- [done] 2026-10-23 04:21Z Review revised candidate and handoff.
- [in_progress] 2026-10-23 04:28Z Publish reviewed draft and verify head.
- [todo] Check exact-head ECA and CI.

## Surprises & Discoveries

- Binary tuple output keeps a namespace map for all unique namespaces, and binary RDF output can retain value metadata. Using either as a long-lived spool would leave a crafted unique-value stream unbounded in memory.
- The browser page-data protocol already streams. The legacy result path must set an unknown total-count cookie before rendering, which requires one count pass and temporary storage of the selected output rows. Re-evaluating a volatile query would change semantics.
- The JDK no-directory temporary-file overload did not use the test runner's configured `java.io.tmpdir` after Maven had set that property. Passing the configured directory explicitly keeps each test run isolated and makes cleanup observable.
- `TupleResultBuilder` starts legacy graph results with three names, while graph rows pass a fourth context value. This pre-existing non-null-context failure is a separate correctness issue; this memory patch preserves the current header and fourth-value call.
- Independent review identified that an unknown-total legacy download with no cap stages its entire result on disk. The default download cap is zero. The same problem applies to an unknown-total legacy page with `limit_query=0`; both paths now stream without a pre-response cookie.
- `total_result_count` is consumed by browser paging controls. Native downloads run in a hidden frame, and an unlimited legacy page has no page window. The existing total-count XML metadata can be appended after streaming, but a response cookie cannot reliably be added after output begins.
- A private spool's plain BNode replay could turn a remote/generated wire ID into a reusable reference if later passed to a repository-aware encoder. The spool now restores every BNode as a non-reference generated wrapper while preserving its wire ID and legacy XML bytes. Normal page-data query execution bypasses the spool entirely.
- A large positive `limit_query` could still stage an arbitrarily large selected window. The spool now uses a shared 64 MiB active-file byte allowance, configurable through `rdf4j.workbench.query.spool.maxBytes`; it reserves before disk writes and releases only after deletion. Ordinary bounded pages and direct streams keep their output semantics, while oversized bounded pages fail before result rendering.

## Decision Log

- Decision: Stream directly when the result count is known or no bounded browser page is requested; spool only selected rows for an unknown-total bounded browser page.
  Rationale: Known-total paging already rewrites the SPARQL query through `PagedQuery`, while bounded unknown-total browser paging must preserve the count cookie and single evaluation.
  Date/Author: 2026-10-23 Codex.
- Decision: Use a small disk codec for RDF values rather than Java object serialization or long-lived RDF4J binary writers.
  Rationale: The codec can keep memory bounded independent of unique namespaces, retain literal language direction, contexts, and nested RDF-star terms, and avoid executing deserialization gadget code.
  Date/Author: 2026-10-23 Codex.
- Decision: Keep the legacy graph header unchanged in this memory patch.
  Rationale: A non-null context already fails in the existing builder because its header has three columns. Adding a fourth column changes the response schema and belongs in a separate red-first correctness change after checking the intended graph page format.
  Date/Author: 2026-10-23 Codex.
- Decision: Do not spool downloads or unlimited legacy pages, regardless of whether the total is known.
  Rationale: Neither route needs a pre-response paging cookie. Direct streaming avoids replacing heap exhaustion with unbounded temporary-disk use; an unlimited page still emits exact total-count metadata at response end.
  Date/Author: 2026-10-23 Codex.
- Decision: Restore staged blank nodes as non-reference generated nodes with their original wire IDs.
  Rationale: Legacy writers serialize those IDs only. The private spool must not mint repository-reference authority while reconstructing values.
  Date/Author: 2026-10-23 Codex.
- Decision: Set a process-wide default disk allowance of 64 MiB for active selected-window spools, with a positive-byte JVM property override.
  Rationale: `limit_query` is client controlled; a row count limit does not bound bytes, and a per-file limit alone does not prevent concurrent disk exhaustion. The existing query circuit breaker uses a deployment system-property budget pattern, while no Workbench policy key governs temporary query storage.
  Date/Author: 2026-10-23 Codex.

## Outcomes & Retrospective

The original known-total tuple/graph regressions, unlimited download and page regressions, spool authority regression, and disk-budget regression all failed before their corresponding production changes; retained reports are listed below. Focused evaluator tests pass 31 tuple and 17 graph cases; final Workbench module verification passes 1,346/0/0/0. Bounded unknown-total browser pages still set their count cookie and stage only selected rows within a shared 64 MiB default disk budget; direct routes write rows immediately. The per-request spool file closes and deletes on success, writer failure, breaker cancellation, and budget refusal. A late query failure can truncate an already-started streamed response. Unlimited legacy pages and downloads omit the pre-response count cookie; unlimited pages retain terminal count metadata, verified through the real XML writer. The pre-existing graph context/header mismatch remains a separate correctness task.

## Context and Orientation

`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/QueryEvaluator.java` chooses legacy tuple, graph, or browser page-data responses. The browser page-data path is already streaming. `evaluateTupleQuery` and `evaluateGraphQuery` collected complete Java lists in the original legacy path. `know_total` is a previous total count supplied by the browser: when positive, `PagedQuery` can rewrite limit and offset before evaluation. With an unknown count and bounded page size, the result is counted to the end and the cookie is set before the response writer starts. `TupleResultBuilder` renders the selected result format. `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/util/QueryEvaluatorTest.java` and `QueryEvaluatorCoverageTest.java` exercise those paths.

## Plan of Work

Add a test in `QueryEvaluatorTest` showing that a known-total tuple row reaches the builder before the query result supplies its next row, then an analogous graph test. Run the narrowest test before production edits and save the Surefire failure to `initial-evidence.txt`. Then replace list collection. Known-total, unlimited, and download results stream through the existing builder. Unknown-total bounded browser pages iterate once, count all rows, write only the requested window to a private temporary file, close the query result, set the cookie, and replay the file through the same builder. The disk codec must encode null, IRI, blank node, datatype or language literal with base direction, and recursive triple terms. The graph variant must retain context. A single row or term may still be large, but total row count must not grow heap use. On success, cancellation, query error, output writer error, or replay error, close and delete the spool.

Add regressions for known-total and unknown-total tuple/graph paths, offset and download caps, unlimited streaming, nested RDF-star values, BNode trust, and cleanup under errors. Keep count metadata, ordering, and one query evaluation. Run focused tests, all Workbench module tests, copyright and formatting checks, and `git diff --check`. Preserve all ignored evidence files.

## Concrete Steps

From the worktree `/Users/havardottestad/.codex/worktrees/security-workbench-query-streaming/rdf4j`, the initial offline root `-Pquick clean install` has succeeded and wrote `maven-build.log`. Run the focused regression with `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-query-streaming --retain-logs QueryEvaluatorTest#<method>`; inspect `tools/workbench/target/surefire-reports/org.eclipse.rdf4j.workbench.util.QueryEvaluatorTest.txt`. Do not use `-am` or `-q` in test runs. After the change, rerun the same selection, its classes, then `tools/workbench` through `mvnf`. Check copyright with `scripts/checkCopyrightPresent.sh` and format with the repository Maven process-resources command.

## Validation and Acceptance

The initial streaming test must fail because the second source row is consumed before the first writer callback. It must pass afterward. A known-total query should invoke the query once and render each row before reading a later row. An unknown-total bounded browser page should invoke the query once, count all rows, set the cookie before output, then replay only the selected page with unchanged RDF wire terms and order. Unlimited pages and downloads should write the first row before reading the next; unlimited pages should report the final total in response metadata. Files created for staging must be absent after successful output and after failures. The Workbench module tests should report zero failures.

## Idempotence and Recovery

The isolated worktree contains all source changes. Maven reruns may overwrite Surefire reports, so copy the first failure snippet immediately into `initial-evidence.txt`. Never remove untracked evidence. Temporary spool files belong to a single evaluation and are closed and deleted with a `finally` or try-with-resources path. If a test fails due to a missing Maven artifact, retry the same command online once, then return offline. If module tests reveal a behavior mismatch, add a focused regression before revising the implementation.

## Artifacts and Notes

Initial root install: `maven-build.log` ends in `BUILD SUCCESS`. Tuple red report is retained in `initial-evidence.workbench-query-streaming.txt`; graph red report is retained in `initial-evidence.workbench-query-graph.txt`. Both fail because list collection reads the second source row before the first writer callback. The final focused verify log is `.mvnf/workspaces/workbench-query-streaming/logs/20261023T030559.202676Z-23838-12d7c708/verify.log` (39/0/0/0). The full module verify log is `.mvnf/workspaces/workbench-query-streaming/logs/20261023T030910.424316Z-81344-346b422d/verify.log` (1337/0/0/0).
Further red reports: `initial-evidence.workbench-query-download.txt`, `initial-evidence.workbench-query-graph-download.txt`, `initial-evidence.workbench-query-bnode.txt`, `initial-evidence.workbench-query-unlimited-page.txt`, and `initial-evidence.workbench-query-unlimited-graph-page.txt`. The latest focused logs are `.mvnf/workspaces/workbench-query-streaming/logs/20261023T033920.176919Z-3231-4f9dcc07/verify.log` (27/0/0/0) and `.mvnf/workspaces/workbench-query-streaming/logs/20261023T034055.989755Z-4889-9a1049af/verify.log` (17/0/0/0).
Final full-module verify log: `.mvnf/workspaces/workbench-query-streaming/logs/20261023T040218.810624Z-19281-d48d0c29/verify.log` (1346/0/0/0). Disk-budget red report: `initial-evidence.workbench-query-spool-budget.txt`; focused evaluator class passed 30/0/0/0 in `.mvnf/workspaces/workbench-query-streaming/logs/20261023T035639.913209Z-95311-97fdf700/verify.log`, and the later single over-budget evaluator test passed in `.mvnf/workspaces/workbench-query-streaming/logs/20261023T035900.812463Z-96845-5745e1a6/verify.log`.

## Interfaces and Dependencies

Use `QueryEvaluator` and a package-private helper under `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/` for the temporary spool. Use Java `Files`, buffered data streams, and RDF4J model `ValueFactory` only; no new Maven dependency. Keep the helper local to the Workbench request and encode recursively so RDF-star statement terms round-trip. Public response types and request parameters must stay unchanged.

## Revision Note

- 2026-10-23: Created this plan after inspecting list materialization, paging, cookie timing, and binary writer cache behavior.
- 2026-10-23: Recorded two failing source-to-writer timing regressions; the graph test also carries a deeply nested RDF-star value, so an iterative spool codec is required for unknown-total replay.
- 2026-10-23: Added private disk staging and direct known-total streaming, then broadened tests for paging, large streams, deep RDF-star, directed language literals, and cleanup. The first class run had 36 passes and one temp-directory locator failure; its focused rerun passed after using the configured temp root.
- 2026-10-23: Focused evaluator pair passed 39/0/0/0. Query duration on known-total streaming excludes time spent in response writer callbacks; the old path measured before rendering.
- 2026-10-23: Copyright check and root `process-resources` formatter succeeded. Full Workbench verification passed 1337/0/0/0; tracked scope remains the evaluator and two test classes, plus the new private spool helper and this ExecPlan.
- 2026-10-23: Review returned an unlimited-download disk-pressure concern. Reopened the plan to inspect the count-cookie contract, add a failing regression for direct download streaming, and verify BNode wire/provenance behavior before final candidate review.
- 2026-10-23: Red-tested tuple/graph downloads and unlimited legacy pages before direct streaming, and red-tested spool BNode authority before restoring non-reference wrappers. Focused tuple/graph classes passed 27/0 and 17/0. Full-module verification is queued after final formatting.
- 2026-10-23: Copyright and formatter checks passed; final full Workbench module passed 1342/0/0/0. `git diff --check` is clean. Candidate awaits independent review before commit or publication.
- 2026-10-23: Second review found a user-controlled large positive page limit could exhaust temporary disk. Added a red 32-byte budget regression (`initial-evidence.workbench-query-spool-budget.txt`), then a shared byte quota with cleanup and documented override. Focused tests cover concurrent active spools, over-budget evaluator cleanup, and parseable real Workbench XML with terminal count metadata. Copyright, formatter, `git diff --check`, and full Workbench module passed (1346/0/0/0).
