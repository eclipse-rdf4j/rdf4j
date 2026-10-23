# Stream large Workbench Explore pages without retaining every statement

This ExecPlan is a living document under `.agent/PLANS.md`. Keep its progress, discoveries, decisions, and outcomes current as work proceeds.

## Purpose / Big Picture

The Workbench Explore page shows every statement mentioning a chosen RDF value. A request with `limit_explore=0` intentionally means unlimited rows, but the server currently stores every matching `Statement` in a Java list before writing any row. A large repository or a large positive page limit can exhaust the server heap. After this work, Explore must still show the same ordered, de-duplicated rows, count and paging information, blank-node origins, and error behavior, while using bounded heap during one evaluation. A regression will demonstrate that an unlimited request delivers its first large row before reading the next one.

## Progress

- [x] (2026-10-23 08:52Z) Inspect Explore route and result cursor.
- [x] (2026-10-23 08:54Z) Create isolated branch and install dependencies.
- [x] (2026-10-23 08:59Z) Add smallest failing streaming regression.
- [x] (2026-10-23 08:59Z) Preserve first red Surefire report.
- [x] (2026-10-23 09:10Z) Repair prerequisite spool release edge.
- [x] (2026-10-23 09:36Z) Design bounded Explore result storage.
- [x] (2026-10-23 09:47Z) Implement bounded and unlimited result paths.
- [x] (2026-10-23 09:35Z) Verify focused and full Workbench tests.
- [x] (2026-10-23 09:42Z) Add failing unlimited origins regression.
- [x] (2026-10-23 09:44Z) Configure scan origins before streaming.
- [x] (2026-10-23 09:47Z) Rerun focused and full module gates.
- [x] (2026-10-23 09:54Z) Review candidate with parent before publishing.
- [ ] (in_progress) Publish draft and verify exact head.

## Surprises & Discoveries

- `ExploreServlet.service` maps zero to `Integer.MAX_VALUE`; its `page()` runs `processResource` on a separate repository connection and returns a `ResultCursor` with an `ArrayList<Statement>`. The connection closes before `service` writes metadata, cookie, or rows. The count-only cursor uses `render=false` and does not append rows.
- The worktree starts at main commit `6b390ad88a9c5b8e79dd0e9afaea0154c8fbfffa`; the separate published query-streaming PR #6232 is not present on main. Its `QueryResultSpool` includes a private term codec and aggregate disk budget. Reusing it may require a clearly documented stacked branch.
- The initial isolated root quick install passed with `python3 scripts/mvn-agent.py --workspace workbench-explore -- -B -ntp -o -Pquick clean install`; full log is under `.mvnf/workspaces/workbench-explore/logs/20261023T085250.792536Z-53058-da197a7a/maven.log`.
- `ExploreServletPageDataTest.unlimitedPageDeliversFirstLargeRowBeforeScanningTheNext` failed on unmodified production code with `Tests run: 1, Failures: 0, Errors: 1` because the response contained only head/view/vars/links when the second statement was about to be consumed. The Surefire summary is preserved in `initial-evidence.txt`; full log is `.mvnf/workspaces/workbench-explore/logs/20261023T085813.110731Z-56745-fc46dd02/verify.log`.
- In published #6232's `QueryResultSpool.close`, `Files.deleteIfExists(path)` runs before `budgetedOutput.release()` in the same try. If deletion throws, the static 64 MiB reservation is leaked. The parent reviewer asked for a red-first follow-up on #6232 before Explore reuses that spool.
- The #6232 follow-up now has red-first evidence and a passing 1,347-test Workbench module run at commit `a54f9d1797b094d99e2b7b31733d3f2de95fb9c5`; local, remote, and PR heads match and ECA passed. The Explore branch can safely use that corrected spool after an explicit stack.
- The Explore branch now stacks on the corrected #6232 commit. Its test-only first commit was rebased onto that head without conflicts. A page-data result writer already supports an exact terminal count after rows; the legacy XML writer also writes metadata at document end.
- For a single-pass unlimited scan, an exact count cannot be known before its first row. The parent reviewer chose the #6232 pattern: send the cookie as zero/unknown before output and place the exact count in terminal metadata. Bounded pages retain the pre-row count/cookie using a budgeted spool.
- The ordinary page-data writer batches 32 rows irrespective of their string size. Direct unlimited Explore output now flushes after each row so a large first row reaches the response before the next input read and at most one row is retained by that writer.
- Focused Explore page-data tests passed with 50 tests after the patch, including bounded spool budget recovery, trusted-versus-equal-looking-unproven blank nodes, and timeout cleanup; the browser-consumer regression passed 31 tests with an unknown cookie and exact terminal count.
- The first full Workbench module run reached 1,353 tests with one failure: an old Explore coverage assertion expected a caller-supplied `know_total=9` on an unlimited page even though the repository contained seven matches. The new exact terminal contract intentionally reports seven and a zero pre-stream cookie; that test now checks both explicitly. The full failure is retained in `.mvnf/workspaces/workbench-explore/logs/20261023T092718.570525Z-11276-f1b3ba25/verify.log`.
- Parent review found that direct unlimited rows can leave the page writer configured with the outer response connection's value factory rather than the scan connection's stored-ID resolver. The ordinary MemoryStore fixture shares a factory across both connections and stayed green; a connection-local provenance fixture failed with `reference=false` before the fix. The red Surefire report is retained in `initial-evidence.unlimited-origins.txt`. A row-sink preparation hook now passes scan origins before the first direct row.
- The corrected focused page-data suite passed 56 tests; the complete Workbench module passed 1,357 tests with zero failures, errors, or skips (`.mvnf/workspaces/workbench-explore/logs/20261023T094431.920785Z-44103-e0eb20dc/verify.log`). Copyright, formatting, and `git diff --check` passed. The cursor still uses `int` counters and an `Integer.MAX_VALUE` sentinel; a mathematically exact overflow regression would need either billions of calls or a new production count-aggregation seam. This deserves separate review before widening the cursor API in the memory patch.

## Decision Log

- Decision: Keep this work in `GH-0000-workbench-explore-streaming` under `/Users/havardottestad/.codex/worktrees/security-workbench-explore-streaming/rdf4j` and leave the published page-model and query branches untouched during the initial red regression.
  Rationale: Explore has distinct four-scan de-duplication and page semantics; a separate red test must establish the failure before any production change.
  Date/Author: 2026-10-23 Codex.
- Decision: Preserve single evaluation and zero-means-unlimited behavior, including row order and origins. Compare direct streaming for unlimited pages with request-private bounded staging for pages whose total must be known before output.
  Rationale: Repeating a volatile or changing repository scan can alter result order and count; holding unlimited rows in heap or an unbounded temporary spool merely moves the resource failure.
  Date/Author: 2026-10-23 Codex.
- Decision: Fix the prerequisite #6232 budget-release edge on its own published branch and revalidate its exact head before stacking Explore.
  Rationale: A reusable spool must not deny later requests after a temporary-file deletion error; keeping this fix in #6232 preserves clean PR ownership.
  Date/Author: 2026-10-23 Codex.
- Decision: Stack Explore on the corrected #6232 head and reuse its private row codec and aggregate 64 MiB staging budget, exposing only the small internal API required across packages.
  Rationale: Copying the term codec and reservation policy would create two drifting security boundaries. The draft Explore PR will target the #6232 branch and state the dependency.
  Date/Author: 2026-10-23 Codex.
- Decision: Unlimited `limit_explore=0` uses one scan with direct row output, an unknown count cookie before output, and the exact count in terminal metadata. A positive page limit stages its requested window within the existing disk budget to preserve pre-row metadata and cookie timing.
  Rationale: A second scan can observe different data and doubles work; an unbounded spool moves the denial-of-service risk from heap to disk. The parent reviewer approved the timing tradeoff and requested a browser-consumer regression.
  Date/Author: 2026-10-23 Codex.

## Outcomes & Retrospective

The Explore cursor now sends selected unlimited rows directly to the page writer and flushes each row before fetching another; a positive limit stores only its requested window in #6232's request-private, aggregate-budgeted spool. The first-row timing and connection-local origin failures were preserved before their production fixes. Both cancellation paths prevent an abandoned worker from writing after response completion, and the spool retains blank-node authority only from negotiated `RepositoryBNode` provenance or a local stored-ID resolver. The browser receives an unknown count cookie before an unlimited stream and an exact terminal total; bounded pages preserve pre-row count/cookie timing. The focused page-data suite passed 56 tests, and the final Workbench module passed 1,357 tests with no failures/errors/skips. The focused browser-consumer suite passed 31 tests before the origin-only revision. Copyright, formatter, and diff whitespace checks passed. Parent independent review approved publication; exact-head PR checks remain.

## Context and Orientation

`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/ExploreServlet.java` serves Explore. Its `ResultCursor` counts de-duplicated statements and holds rendered rows. `processResource` scans a resource in subject, predicate, object, then context roles, avoiding duplicate quads across those scans. `TupleResultBuilder` in `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/TupleResultBuilder.java` writes either the compact Workbench page-data stream or a requested legacy SPARQL result format. `WorkbenchPageResultWriter` can place metadata in a terminal end record; browsers parse the same page-data stream. Existing result-shape tests are in `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/commands/ExploreServletPageDataTest.java` and `ExploreServletCoverageTest.java`. The published #6232 branch contains `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/QueryResultSpool.java`, a request-private disk row store with a 64 MiB aggregate reservation.

## Plan of Work

First add an in-repo regression in `ExploreServletPageDataTest` that requests `limit_explore=0`, yields one large statement, and proves that the response contains its row before `processResource` observes a later statement. Run only that method with `mvnf`, capture its Surefire failure, and preserve it in top-level `initial-evidence.txt`. Add bounded-page, four-role de-duplication, count/cookie, origin, writer-failure, and large-row variants before changing production behavior.

Then replace list retention at the `ResultCursor` ownership boundary. Unlimited pages should write rows as they arrive, keep a running count, and report final count in a terminal record. Bounded pages needing count or `has-more` before rows stage only the requested window in #6232's request-private, size-budgeted row spool, replay it once, and release the reservation on all exits. The Explore PR will stack on #6232. Keep the role scan order, de-duplication, repository-origin value factory, offset, and early-stop behavior. Preserve legacy XML/CSV/TSV output shape where possible and grade the unavoidable unlimited cookie timing change as medium compatibility risk.

## Concrete Steps

From this worktree root, run focused tests with `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-explore ExploreServletPageDataTest#<method> --retain-logs`. The runner does the required quick install before the module verify and reports the full-GAV Surefire path; no test invocation may use `-am` or `-q`. Immediately after the first failing run, use `python3 scripts/agent-evidence.py --command '<same command>' --log <verify.log> <reported-surefire-directory> > initial-evidence.txt`. Preserve all evidence, logs, and unexpected files. After implementation run the focused class, then `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-explore tools/workbench --retain-logs`. Run `cd scripts && ./checkCopyrightPresent.sh`, then `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`, then `git diff --check` before independent review.

## Validation and Acceptance

The unlimited streaming regression must fail before production edits and pass afterward, with the first row observable before a later row is read. Existing 300-row offset/limit tests, exact total and `has-more` semantics, and SPARQL XML output must stay green. New tests must prove bounded-page staging never retains all statements in heap, cleanup on normal and exceptional exits, and exact row/term order across the four scans. The full Workbench module should pass with no new Surefire/Failsafe failures. A human can inspect the compact page-data end record and verify that its final total matches the number of unique emitted statements.

## Idempotence and Recovery

All tests run in the named Maven workspace, preserving report and log history. Only the named Explore worktree is edited. If a red test reveals that the current page-data writer cannot emit before completion, revise the design in this plan before touching production code. If #6232 must be used, fix its release-on-delete-failure edge on that published PR first, then stack only this separate Explore branch and declare the base in the draft PR.

## Artifacts and Notes

Initial build result:

    BUILD SUCCESS; total time 01:27 min; 2026-10-23T10:54:20+02:00.

Final Workbench verification:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-explore tools/workbench --retain-logs
    Tests run: 1355, Failures: 0, Errors: 0, Skipped: 0
    .mvnf/workspaces/workbench-explore/logs/20261023T093319.409739Z-95037-2bf16bec/verify.log

## Interfaces and Dependencies

Do not add a new library. Keep the result-staging format private and term serialization independent of Java object serialization. If the #6232 spool is reused across packages, expose only the smallest internal API and annotate it `@InternalUseOnly`; its budget must be released even when temporary-file deletion fails. Preserve the public `ExploreServlet.ResultCursor` behavior expected by existing tests unless changing it is essential for streaming and supported by new regressions.

Plan revision (2026-10-23): created after source inspection and initial build so a future worker can reproduce the red-first workflow and the #6232 dependency decision. Updated after preserving the failing Explore report, publishing the parent-approved prerequisite cleanup repair, stacking on #6232, and recording the parent-approved unlimited count timing tradeoff.
