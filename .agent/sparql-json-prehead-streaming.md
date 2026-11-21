# Preserve SPARQL JSON rows before the head with bounded storage

This ExecPlan is a living document maintained under `.agent/PLANS.md`.

## Purpose / Big Picture

SPARQL JSON objects can place `results` before `head`. A caller should receive every binding row in source order, including duplicates, between exactly one start and end callback. Today `AbstractSPARQLJSONParser` stores those rows in a `HashSet`, losing duplicates and retaining the entire result in heap. This change will preserve callback semantics while keeping parser heap use bounded for that order. A focused test will show the current wrong callback sequence and then pass after the repair.

## Progress

- [x] (2026-10-23 05:05Z) Inspect parser and callback contract.
- [x] (2026-10-23 05:05Z) Run isolated root quick install.
- [x] (2026-10-23 05:12Z) Add smallest failing callback regression.
- [x] (2026-10-23 05:17Z) Prove bounded pre-head retention.
- [x] (2026-10-23 05:22Z) Implement bounded ordered row replay.
- [x] (2026-10-23 05:25Z) Verify cleanup and feature interactions.
- [x] (2026-10-23 05:28Z) Prove aggregate spill budget.
- [x] (2026-10-23 05:42Z) Reserve aggregate bytes through deletion.
- [x] (2026-10-23 05:47Z) Verify focused and complete module.
- [x] (2026-10-23 05:52Z) Reproduce failed-deletion reservation leak.
- [x] (2026-10-23 05:56Z) Repair cleanup after deletion errors.
- [ ] **in_progress** Review publication and exact CI.

## Surprises & Discoveries

- `core/queryresultio/sparqljson/src/main/java/org/eclipse/rdf4j/query/resultio/sparqljson/AbstractSPARQLJSONParser.java` creates `HashSet<BindingSet> bindings` before reading the top-level fields. Before `head.vars`, it adds every row to that set. At `head.vars`, it calls `endQueryResult` inside the replay loop; it also calls `endQueryResult` after the `bindings` array. The collector cannot reveal the repeated end calls because it stores only a Boolean completion state.
- The source tree has no existing tests for `results` preceding `head`; use a callback-recording handler rather than `QueryResultCollector` to assert each event.
- The first collector-backed red run failed with `UnsupportedOperationException` because the parser invoked `endQueryResult` before `startQueryResult`; evidence is `initial-evidence.sparql-json-prehead-lifecycle.txt`. The callback-only red run failed with expected `[start:[x], row:first, row:first, row:last, end]` but actual `[end, start:[x], row:first, end, row:last, end]`; evidence is `initial-evidence.sparql-json-prehead.txt`.
- A 512-byte configured pre-head budget was ignored in the baseline (`Expected QueryResultParseException to be thrown, but nothing was thrown`); evidence is `initial-evidence.sparql-json-prehead-budget.txt`. The expanded five-case class had four expected failures and one head-first streaming pass; evidence is `initial-evidence.sparql-json-prehead-expanded.txt`.
- The first production candidate passed all five focused cases, including ordered duplicates, one lifecycle, empty bindings, nested RDF-star, and the 512-byte budget. Its retained verify log is `.mvnf/workspaces/sparql-json-prehead/logs/20261023T051936.309564Z-15718-00cb774f/verify.log`.
- A seven-case focused rerun passed 7/0/0/0 with a lazy 20,000-row source and no handler-side row collection, plus missing-header and handler-failure spool cleanup. Its retained verify log is `.mvnf/workspaces/sparql-json-prehead/logs/20261023T052243.935415Z-36064-c71e0f08/verify.log`.
- The concurrent-spool red test paused one parser before its header and parsed a second document under the same 24 KiB configured budget. Both succeeded, proving the current cap is per parser rather than aggregate. Its focused report (`Tests run: 1, Failures: 1`) is `initial-evidence.sparql-json-prehead-aggregate.txt`.
- The post-fix aggregate test uses 8,000 rows to ensure the parser has staged data before Jackson requests the withheld header bytes. It passed 1/0/0/0 with a shared 384 KiB budget; retained log: `.mvnf/workspaces/sparql-json-prehead/logs/20261023T054212.025140Z-29373-63e7b6f4/verify.log`.
- The complete focused class passed 8/0/0/0 in `.mvnf/workspaces/sparql-json-prehead/logs/20261023T054401.297023Z-31038-6097fb39/verify.log`. Copyright validation, scoped formatting, and `git diff --check` passed. The full module passed 209/0/0/0 in `.mvnf/workspaces/sparql-json-prehead/logs/20261023T054625.867700Z-51787-116a2cfc/verify.log`.
- The spill cap applies to aggregate concurrent temporary row storage. It does not bound individual JSON token allocation or `head.vars`/`head.link` lists, which are retained in memory. A general input/header budget is a separate follow-up if required for untrusted documents.
- A deterministic test replaced the open spool path with a nonempty directory before parser cleanup. The next parse failed with `SPARQL JSON pre-head storage limit exceeded: 512 bytes`, proving deletion failure skipped reservation release. Red report: `initial-evidence.sparql-json-prehead-delete-failure.txt`.
- A nested `finally` releases the aggregate reservation even if deletion fails. The same red selection passed 1/0/0/0 in `.mvnf/workspaces/sparql-json-prehead/logs/20261023T055314.288020Z-76259-ddee48ce/verify.log`; after formatting, the full focused class passed 9/0/0/0 in `.mvnf/workspaces/sparql-json-prehead/logs/20261023T055452.872961Z-78003-69440761/verify.log`. The earlier full module 209/0/0/0 precedes only this localized cleanup change.

## Decision Log

- Decision: Preserve pre-head compatibility because JSON object members have no ordering guarantee and the parser already accepts this order. Rejecting such input would change valid-use behavior. Rationale: The callback contract requires the head first even when the bytes arrive later, so rows must be deferred. Date/Author: 2026-10-23, Codex.
- Decision: Use a private temporary JSON token spill only when results arrive before `head.vars` and a handler is present. A streaming generator copies one binding object at a time, and the same value parser replays it when the header arrives. Rationale: This preserves input order, duplicates, value dialects, and caller `ValueFactory` behavior without a bespoke RDF term codec; head-first rows continue directly to the handler. Date/Author: 2026-10-23, Codex.
- Decision: Add a `LongRioSetting` for a 64 MiB default maximum pre-head spill, enforce it on the output stream before each disk write, and reject an over-budget result with a clear parse error. Rationale: It bounds temporary disk use as well as heap while allowing a caller to configure the rare valid case of a larger pre-head result. Date/Author: 2026-10-23, Codex.
- Decision: Before publication, test concurrent parses against one aggregate budget and retain reservations until each temp file is deleted. Rationale: A per-parser cap would allow many simultaneous untrusted responses to fill the temporary filesystem. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The red-first callback, empty-result, nested triple, 512-byte budget, concurrent aggregate-cap, and deletion-failure reports are preserved. A private file-backed JSON token spill with a 64 MiB default aggregate limit passed all nine focused regressions; the 209-test module suite passed before the localized cleanup repair, and its exact red selection passed afterward. The parser now emits ordered duplicates between one start and one end callback. A pre-head result beyond the configured cap now fails with a parse error; very large headers and single JSON values remain independent input-memory risks.

## Context and Orientation

`core/queryresultio/sparqljson` owns SPARQL JSON tuple and Boolean parsing. `AbstractSPARQLJSONParser.parseQueryResultInternal` walks a Jackson streaming parser and invokes a `QueryResultHandler`. `startQueryResult(List<String>)` must precede `handleSolution(BindingSet)`, and `endQueryResult()` must follow all rows once. A binding row is one object in `results.bindings`. `SPARQLResultsJSONParser` is the tuple parser used through `QueryResultIO.createTupleParser(TupleQueryResultFormat.JSON)`.

The worktree is `/Users/havardottestad/.codex/worktrees/security-sparql-json-parser/rdf4j`, branch `GH-0000-sparql-json-prehead-streaming`, based on main commit `3d292763558604affd89c7d4256e8df5720bd895`. Its `.m2_repo` is a read-through link to the original local cache, while `mvnf --workspace sparql-json-prehead` isolates writable Maven artifacts. Preserve untracked reports and logs.

## Plan of Work

The test class in `core/queryresultio/sparqljson/src/test/java/org/eclipse/rdf4j/query/resultio/sparqljson` records callbacks for a `results`-before-`head.vars` document with duplicate and distinct rows; the four red cases and one passing head-first streaming control are persisted. Replace the in-memory set with a private ordered JSON token spill/replay mechanism and move lifecycle completion to one point after all rows. Ensure spill files close/delete on success, parse failure, handler failure, and cancellation; enforce the 64 MiB default aggregate byte budget before writing and expose its `LongRioSetting` for overrides. Keep the Boolean path and direct head-first streaming intact. Add cleanup, malformed-input, and feature-interaction cases before final module verification.

## Concrete Steps

The isolated root quick install passed from this worktree with:

    python3 scripts/mvn-agent.py --workspace sparql-json-prehead -- -B -ntp -o -Pquick clean install

Run the smallest red and green test with:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace sparql-json-prehead SPARQLJSONPreHeadStreamingTest#preservesDuplicateOrderAndSingleLifecycle --retain-logs

Persist the first failing report as `initial-evidence.sparql-json-prehead.txt` before rerunning. Then run the test class and complete module:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace sparql-json-prehead SPARQLJSONPreHeadStreamingTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace sparql-json-prehead core/queryresultio/sparqljson --retain-logs

Before candidate review, run `scripts/checkCopyrightPresent.sh` from its own directory, the repository formatter, and `git diff --check`. The formatter must not overlap a live workspace Maven run.

## Validation and Acceptance

The pre-head document must emit `start`, all binding rows in source order including duplicates, and one `end`. A head-first stream must deliver its first row before the parser consumes later rows. Parser heap must not scale with the number of pre-head rows, and the spill must have a documented bound and cleanup on all exits. Existing tuple, Boolean, RDF-star, and custom JSON syntax tests must remain green. The module's Surefire summary must show zero new failures and errors.

## Idempotence and Recovery

Maven workspace runs are repeatable and leave reports under `.mvnf/workspaces/sparql-json-prehead`; preserve the first failing report separately because later runs replace class reports. Do not clean the worktree or stage evidence artifacts. If a proposed spill format changes lexical value semantics, test and revise the design before production implementation. Stage only reviewed source, tests, and this plan when publication is approved.

## Artifacts and Notes

The quick-install log is `.mvnf/workspaces/sparql-json-prehead/logs/20261023T050316.982522Z-69317-af54c9c6/maven.log`, ending in `BUILD SUCCESS` after 1:54. Red and green evidence paths will be added at their milestones.

## Interfaces and Dependencies

Use the existing Jackson streaming parser in `AbstractSPARQLJSONParser` and standard Java temporary file/stream APIs. No new dependency is planned. The spill representation must preserve every binding name and value, including nested RDF-star terms, and replay through the existing `parseValue` semantics. The parser-visible setting defaults to a 64 MiB aggregate cap across concurrent pre-head spills and can be overridden by callers.

Revision note (2026-10-23 05:05Z): Created after source inspection and isolated root quick install. The next change is a failing callback test; no production code has been edited.

Revision note (2026-10-23 05:12Z): Preserved two red reports showing early end, duplicate loss, and incorrect callback order. Moved the sole in-progress step to bounded-retention evidence; production remains unchanged.

Revision note (2026-10-23 05:17Z): Preserved the 512-byte budget red report and expanded five-case class (four red, one head-first green). Chose bounded private JSON token spill with one pass through the existing value parser on replay; moved the sole in-progress step to implementation.

Revision note (2026-10-23 05:22Z): The first implementation passed all five focused cases. Moved the sole in-progress step to cleanup and feature-interaction verification before the full module suite.

Revision note (2026-10-23 05:25Z): The seven-case class passed, including lazy 20,000-row input and failure cleanup. Raised the aggregate disk concern and moved the sole in-progress step to a red-first concurrent reservation test.

Revision note (2026-10-23 05:28Z): The concurrent reservation test failed as expected, proving aggregate disk exposure. Moved the sole in-progress step to shared reservation accounting through temporary-file deletion.

Revision note (2026-10-23 05:42Z): The aggregate reservation test passed after using the actual Java temporary-file directory and enough input to exceed Jackson's read-ahead window. Moved the sole in-progress step to focused and full module verification.

Revision note (2026-10-23 05:47Z): Focused 8/0 and full module 209/0 passed after formatting. Moved the sole in-progress step to candidate review and publication decision.

Revision note (2026-10-23 05:50Z): Independent review found that a spool deletion exception skips aggregate reservation release. Returned the sole in-progress step to a focused red cleanup regression before touching production again.

Revision note (2026-10-23 05:52Z): Preserved the focused failing deletion regression; moved the sole in-progress step to nested-finally cleanup and publication verification.

Revision note (2026-10-23 05:56Z): The cleanup regression turned green, then all nine focused tests passed after formatting. The prior 209-test module result remains the full-suite evidence for the larger change. Moved the sole in-progress step to publication and exact-head CI.
