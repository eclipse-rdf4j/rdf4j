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
- [ ] (in_progress) Design bounded Explore result storage.
- [ ] Implement bounded and unlimited result paths.
- [ ] Verify focused and full Workbench tests.
- [ ] Review candidate with parent before publishing.

## Surprises & Discoveries

- `ExploreServlet.service` maps zero to `Integer.MAX_VALUE`; its `page()` runs `processResource` on a separate repository connection and returns a `ResultCursor` with an `ArrayList<Statement>`. The connection closes before `service` writes metadata, cookie, or rows. The count-only cursor uses `render=false` and does not append rows.
- The worktree starts at main commit `6b390ad88a9c5b8e79dd0e9afaea0154c8fbfffa`; the separate published query-streaming PR #6232 is not present on main. Its `QueryResultSpool` includes a private term codec and aggregate disk budget. Reusing it may require a clearly documented stacked branch.
- The initial isolated root quick install passed with `python3 scripts/mvn-agent.py --workspace workbench-explore -- -B -ntp -o -Pquick clean install`; full log is under `.mvnf/workspaces/workbench-explore/logs/20261023T085250.792536Z-53058-da197a7a/maven.log`.
- `ExploreServletPageDataTest.unlimitedPageDeliversFirstLargeRowBeforeScanningTheNext` failed on unmodified production code with `Tests run: 1, Failures: 0, Errors: 1` because the response contained only head/view/vars/links when the second statement was about to be consumed. The Surefire summary is preserved in `initial-evidence.txt`; full log is `.mvnf/workspaces/workbench-explore/logs/20261023T085813.110731Z-56745-fc46dd02/verify.log`.
- In published #6232's `QueryResultSpool.close`, `Files.deleteIfExists(path)` runs before `budgetedOutput.release()` in the same try. If deletion throws, the static 64 MiB reservation is leaked. The parent reviewer asked for a red-first follow-up on #6232 before Explore reuses that spool.
- The #6232 follow-up now has red-first evidence and a passing 1,347-test Workbench module run at commit `a54f9d1797b094d99e2b7b31733d3f2de95fb9c5`; local, remote, and PR heads match and ECA passed. The Explore branch can safely use that corrected spool after an explicit stack.

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

## Outcomes & Retrospective

The source path is confirmed, initial build passed, and a deterministic first-row streaming test failed before production changes. The prerequisite #6232 cleanup repair is published. Explore implementation and full verification remain.

## Context and Orientation

`tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/ExploreServlet.java` serves Explore. Its `ResultCursor` counts de-duplicated statements and holds rendered rows. `processResource` scans a resource in subject, predicate, object, then context roles, avoiding duplicate quads across those scans. `TupleResultBuilder` in `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/TupleResultBuilder.java` writes either the compact Workbench page-data stream or a requested legacy SPARQL result format. `WorkbenchPageResultWriter` can place metadata in a terminal end record; browsers parse the same page-data stream. Existing result-shape tests are in `tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/commands/ExploreServletPageDataTest.java` and `ExploreServletCoverageTest.java`. The published #6232 branch contains `tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/QueryResultSpool.java`, a request-private disk row store with a 64 MiB aggregate reservation.

## Plan of Work

First add an in-repo regression in `ExploreServletPageDataTest` that requests `limit_explore=0`, yields one large statement, and proves that the response contains its row before `processResource` observes a later statement. Run only that method with `mvnf`, capture its Surefire failure, and preserve it in top-level `initial-evidence.txt`. Add bounded-page, four-role de-duplication, count/cookie, origin, writer-failure, and large-row variants before changing production behavior.

Then replace list retention at the `ResultCursor` ownership boundary. Unlimited pages should write rows as they arrive, keep a running count, and report final count in a terminal record. Bounded pages needing count or `has-more` before rows can stage only the requested window in a request-private, size-budgeted row spool, replay it once, and release the reservation on all exits. Investigate reusing #6232's codec and budget without duplicating it; if stacking is necessary, state the dependency in the PR. Keep the role scan order, de-duplication, repository-origin value factory, offset, and early-stop behavior. Preserve legacy XML/CSV/TSV output shape where possible and grade any unavoidable cookie/metadata timing change.

## Concrete Steps

From this worktree root, run focused tests with `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-explore ExploreServletPageDataTest#<method> --retain-logs`. The runner does the required quick install before the module verify and reports the full-GAV Surefire path; no test invocation may use `-am` or `-q`. Immediately after the first failing run, use `python3 scripts/agent-evidence.py --command '<same command>' --log <verify.log> <reported-surefire-directory> > initial-evidence.txt`. Preserve all evidence, logs, and unexpected files. After implementation run the focused class, then `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace workbench-explore tools/workbench --retain-logs`. Run `cd scripts && ./checkCopyrightPresent.sh`, then `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`, then `git diff --check` before independent review.

## Validation and Acceptance

The unlimited streaming regression must fail before production edits and pass afterward, with the first row observable before a later row is read. Existing 300-row offset/limit tests, exact total and `has-more` semantics, and SPARQL XML output must stay green. New tests must prove bounded-page staging never retains all statements in heap, cleanup on normal and exceptional exits, and exact row/term order across the four scans. The full Workbench module should pass with no new Surefire/Failsafe failures. A human can inspect the compact page-data end record and verify that its final total matches the number of unique emitted statements.

## Idempotence and Recovery

All tests run in the named Maven workspace, preserving report and log history. Only the named Explore worktree is edited. If a red test reveals that the current page-data writer cannot emit before completion, revise the design in this plan before touching production code. If #6232 must be used, fix its release-on-delete-failure edge on that published PR first, then stack only this separate Explore branch and declare the base in the draft PR.

## Artifacts and Notes

Initial build result:

    BUILD SUCCESS; total time 01:27 min; 2026-10-23T10:54:20+02:00.

## Interfaces and Dependencies

Do not add a new library. Keep the result-staging format private and term serialization independent of Java object serialization. If the #6232 spool is reused across packages, expose only the smallest internal API and annotate it `@InternalUseOnly`; its budget must be released even when temporary-file deletion fails. Preserve the public `ExploreServlet.ResultCursor` behavior expected by existing tests unless changing it is essential for streaming and supported by new regressions.

Plan revision (2026-10-23): created after source inspection and initial build so a future worker can reproduce the red-first workflow and the #6232 dependency decision. Updated after preserving the failing Explore report and publishing the parent-approved prerequisite cleanup repair.
