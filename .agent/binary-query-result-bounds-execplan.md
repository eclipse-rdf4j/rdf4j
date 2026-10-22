# Bound binary query-result parsing to received data

This living ExecPlan follows `.agent/PLANS.md`. Keep its progress, decisions, evidence, and outcomes current while working.

## Purpose / Big Picture

A remote query endpoint can send a binary tuple result with enormous claimed column counts, string lengths, or namespace IDs. The current parser allocates directly from those claims and retains namespace mappings across documents. After this change, malformed results fail with checked parse or I/O errors while valid large values, sparse namespace IDs, zero-column empty rows, and ordinary writer output still parse. The new malformed-input tests and the full binary query-result module suite demonstrate the behavior.

## Progress

- [x] [done] Create clean branch and install dependencies.
- [x] [done] Add smallest malformed input red tests.
- [x] [done] Add lifecycle and row-state variants.
- [x] [done] Implement bounded parser state and reads.
- [x] [done] Verify focused and full module suites.
- [x] [done] Reproduce nested parser stack exhaustion.
- [x] [done] Replace recursive triple term parsing.
- [x] [done] Reverify module and review scope.
- [x] [done] Obtain final candidate review.
- [ ] [in_progress] Publish scoped draft PR.
- [ ] [todo] Monitor exact-head CI checks.

## Surprises & Discoveries

`BinaryQueryResultParser.parse` checks `formatVersion > FORMAT_VERSION && formatVersion < 1`, which is impossible. It creates an `ArrayList` with untrusted `columnCount`, and `readStringV2` calls `IOUtil.readBytes(in, length)`, which immediately allocates the entire claimed size. Namespace IDs index an array with negative and off-by-one faults, and the array persists between parses. With zero columns, a value record can append forever because the tuple size cannot equal zero; TABLE_END also silently drops a partial row. The format's namespace IDs are nonnegative and must be declared before QNAME use. The writer emits nonnegative sequential namespace IDs.

## Decision Log

- Decision: Use a separate managed worktree at `/Users/havardottestad/.codex/worktrees/security-binary-query-result-parser/rdf4j`, branch `GH-0000-binary-query-result-bounds`, from main `3d292763558604affd89c7d4256e8df5720bd895`. Rationale: keep this fix independent of the Binary RDF parser PR and all other work. Date/Author: 2026-10-23, Codex.
- Decision: Keep the common small namespace-ID path in an array and store large sparse IDs separately. Rationale: namespace memory should grow with declarations, while normal writer output should remain fast. Date/Author: 2026-10-23, Codex.
- Decision: Grow UTF-8 byte storage only after corresponding bytes have been received. Rationale: an untrusted length cannot cause a huge immediate allocation, while large valid strings remain supported without an arbitrary cap. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The first safe format-version test failed in Surefire and is preserved in `initial-evidence.txt`. A selected set of ten malformed-input/lifecycle tests then failed in ten cases, preserved in `binary-query-variants-evidence.txt`. A separate extreme column-count test made the unfixed Surefire fork exit with an out-of-memory error before writing a report; it remains as a post-fix verification case. Both stack-exhaustion variants failed before the iterative parser change and are preserved in `recursion-initial-evidence.txt` and `nested-triple-initial-evidence.txt`. The final full module suite passed 81 tests with no failures, errors, or skips. Copyright, formatting, and diff checks passed. Candidate review and publication remain.

## Context and Orientation

`core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultParser.java` reads the binary tuple format sent by remote RDF4J query services. The format header includes the version and count of column names. Version 2 and later strings carry a signed 32-bit byte length. Namespace records assign an integer ID to a URI prefix; QNAME records refer to it. `BinaryQueryResultWriter.java` writes current version 4 and sequential namespace IDs. `core/common/io/src/main/java/org/eclipse/rdf4j/common/io/IOUtil.java` is the general byte reader, but this change should keep bounded reading inside the binary result parser rather than change the shared method.

## Plan of Work

Create `BinaryQueryResultParserMalformedTest.java` in the module's existing test package. First test an extreme claimed column count with no headers, and an extreme string length with only a byte of body; run the smallest selection and persist Surefire failures in top-level `initial-evidence.txt` before editing production. Add cases for invalid versions, negative lengths, sparse/negative/undeclared namespaces, parser reuse after success and failure, zero-column value records, partial rows, valid zero-column empty rows, and valid large strings. Then remove untrusted capacity hints, read strings incrementally, use checked namespace lookups, reset parser state on every parse boundary, and reject invalid row transitions.

## Concrete Steps

At this worktree root, the required initial install is `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`; it passed and writes `maven-build.log`. Run tests with `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace binary-query-result --retain-logs --module core/queryresultio/binary BinaryQueryResultParserMalformedTest#<method>`. Use the module path alone for its full suite. Never use `-am` or `-q` with tests. After edits, run `scripts/checkCopyrightPresent.sh` and root `mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -T 2C process-resources`, then inspect the scoped diff.

## Validation and Acceptance

The red tests fail on the original parser and pass unchanged after the fix. An extreme length or count with a short body yields a checked EOF or parse error without a claimed-size allocation. A QName using a namespace from a prior parse fails as undeclared; positive sparse namespace IDs and valid large literals still round-trip. A zero-column result accepts EMPTY_ROW but rejects value markers, and a partial nonzero row fails at TABLE_END. The full module has no new Surefire or Failsafe failures.

## Idempotence and Recovery

Keep all untracked evidence and Maven logs. A failed offline dependency resolution gets one retry online and then returns to offline. Re-running tests is safe because `mvnf` isolates build output in its named workspace. Stage only the reviewed parser, test, and plan files; do not modify the original checkout or other worktrees.

## Artifacts and Notes

The initial install log is `maven-build.log`. The first Surefire report is `.mvnf/workspaces/binary-query-result/build/org.eclipse.rdf4j/rdf4j-queryresultio-binary/6.2.0-SNAPSHOT/surefire-reports/org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultParserMalformedTest.txt`, copied to `initial-evidence.txt` before later test runs. The ten-case red selection is copied to `binary-query-variants-evidence.txt`. Hold publication until candidate review; draft PR creation, SHA equality, and CI checks follow.

## Interfaces and Dependencies

Keep `BinaryQueryResultParser`'s public constructors, `parse(InputStream)`, and format compatibility for versions 1 through `FORMAT_VERSION`. Use existing JDK arrays and collections only. Keep `IOUtil.readBytes` unchanged for its other callers. Invalid content must produce checked `IOException` or `QueryResultParseException`, not unchecked index or allocation failures.

Plan revision note (2026-10-23): Created after source review and the required root quick install. No production or test source has been changed yet.
Plan revision note (2026-10-23): The first extreme-column test crashed the unfixed Surefire fork without a report, so a bounded unsupported-version test was run red and preserved first. Ten further safe malformed, lifecycle, and row-state variants all failed in a Surefire report before any production edit. Begin the parser implementation.
Plan revision note (2026-10-23): Focused parser regressions now pass: 18 tests, 0 failures/errors. The added green cases cover namespace reassignment, nested triple terms, and versions 1 through 4. The full module passed: 79 tests, 0 failures/errors/skips. Format and review the candidate next.
Plan revision note (2026-10-23): Parent review identified attacker-controlled recursion in namespace markers and nested triple terms. Hold publication, add red tests against the current candidate, then replace recursive parsing before repeating validation.
Plan revision note (2026-10-23): A 100,000-declaration namespace chain and 50,000 nested triple terms independently produced StackOverflowError in Surefire. Reports were preserved in `recursion-initial-evidence.txt` and `nested-triple-initial-evidence.txt` before the parsing change.
Plan revision note (2026-10-23): The two stack-exhaustion regressions now pass with iterative namespace and triple parsing (2 tests, 0 failures/errors). Re-run the full parser/module suites and formatting checks before renewed review.
Plan revision note (2026-10-23): Full binary query-result module passed 81 tests, 0 failures/errors/skips after iterative parsing; copyright and root formatter passed and diff check is clean. Sent candidate to parent for final review, holding publication until response.
Plan revision note (2026-10-23): Parent approved final parser design and evidence. Publish the reviewed three-file scope as a draft PR, then verify exact-head checks.
