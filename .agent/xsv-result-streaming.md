# Deliver CSV and TSV result rows as they are parsed

This ExecPlan is a living document maintained under `.agent/PLANS.md`.

## Purpose / Big Picture

Remote SPARQL CSV and TSV results should reach a caller one row at a time. Both current parsers call OpenCSV `CsvToBean.parse()`, which reads every binding row into a list before the first handler callback. A large remote result can exhaust heap, and a handler that wants to stop after the first row still waits for all later input. The new regression will make the first callback fail intentionally before any later source bytes are available; after this change that callback occurs promptly for both formats.

## Progress

- [x] (2026-10-23 06:00Z) Prepare isolated current-main worktree.
- [x] (2026-10-23 06:00Z) Run initial root quick install.
- [x] (2026-10-23 06:13Z) Add smallest first-row red regressions.
- [x] (2026-10-23 06:20Z) Implement bounded shared row delivery.
- [x] (2026-10-23 06:28Z) Verify variants and full module.
- [x] (2026-10-23 06:40Z) Add header and error regressions.
- [x] (2026-10-23 07:00Z) Check OpenCSV and PR integration.
- [ ] **in_progress** Publish and verify draft PR.

## Surprises & Discoveries

- `SPARQLResultsCSVParser.parse` and `SPARQLResultsTSVParser.parse` each call `CsvToBean.parse()` and then iterate its `List<BindingSet>`. The shared `SPARQLResultsXSVMappingStrategy` already maps one parsed line into one `BindingSet` and captures the header from a `CSVReader`.
- OpenCSV 5.12.0's `CsvToBeanIterator.next()` calls `readSingleLine()` before returning the previous bean (verified against the local jar bytecode). Replacing `.parse()` with `.iterator()` alone can still read later network bytes before invoking the current row's callback. The implementation must control row reads directly through the configured reader or another mechanism that has no lookahead.
- The first probe used a later source that threw `IOException`. OpenCSV treated that as a recoverable input error and still delivered the first row, so it did not prove callback timing. An `AssertionError` sentinel caused OpenCSV's parallel collector to spin waiting for its worker, so that run was terminated and not used as test evidence. The final deterministic sentinel records whether the later source was touched and returns EOF; it exposes ordering without throwing in OpenCSV internals.
- CSV and TSV each failed their smallest focused method with `later input was read before the first row callback ==> expected: <false> but was: <true>`. Saved reports: `initial-evidence.xsv-csv-first-row.txt` and `initial-evidence.xsv-tsv-first-row.txt`.
- A first lexical regression incorrectly expected OpenCSV to retain CRLF inside a quoted field. The old `CsvToBeanBuilder` and the direct `CSVReaderBuilder` both use the default `keepCR=false` reader setting. The test now uses an LF inside the quoted value and CRLF between records, without changing CSV lexical behavior in this memory fix.
- The separate #6209 writer-security PR sets OpenCSV `keepCarriageReturn=true` and changes the CSV parser and both format mapping strategies. OpenCSV 5.12.0's keep-CR `LineReader` treats an empty physical line as EOF. That dropped blank CSV records and broke blank lines inside quoted fields on #6209 even without this streaming change. The #6209 branch was repaired separately, red-first, at commit `c2e1c050d66f7dc83ec5a4cb3de259993af5b777`. An isolated combined checkout passed all 249 text-module tests with both changes.

## Decision Log

- Decision: Keep CSV and TSV as one cohesive parser change because they share the same eager materialization defect and row-mapping abstraction. Rationale: Both must meet the same handler lifecycle and first-row streaming contract while retaining their distinct lexical reader rules. Date/Author: 2026-10-23, Codex.
- Decision: Do not use OpenCSV `CsvToBean.iterator()` without proof of callback timing. Rationale: Its `next()` reads ahead before returning a bean, so the first handler callback may still wait for a later row. Date/Author: 2026-10-23, Codex.
- Decision: Keep the caller's `InputStream` open, as the previous `CsvToBean.parse()` path did. Rationale: Reader ownership belongs to the caller; changing closure would break parser composition. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

Focused CSV and TSV callback-order failures are preserved in the top-level `initial-evidence.txt` and format-specific red reports. The expanded focused suite passed 11/0/0/0, followed by the complete `core/queryresultio/text` module at 229/0/0/0. Both formats deliver the first row before later input is read, complete a header-only result once, and omit the end callback after a malformed later row. A blank CSV record remains one unbound row. A 100,000-row generated source completes for each format without test-side result collection. Copyright, scoped formatter, and `git diff --check` passed.

User break risk: low-to-moderate. Valid CSV and TSV rows retain existing mapping, order, and lexical conventions. Callbacks now occur while input is still being read; on a malformed later record, already delivered rows cannot be rolled back. OpenCSV's legacy CRLF normalization inside quoted CSV fields remains unchanged on this main-based branch. A single huge CSV field, TSV line, or header can still consume memory proportional to that record; a separate input-size policy would be needed for those cases.

Merge note: #6209 overlaps the CSV parser and CSV/TSV mapping strategies. This PR is deliberately based on main, and whichever PR merges second will need a small conflict resolution that keeps #6209's blank-preserving CSV reader and lexical mapping together with this PR's direct row loop. The isolated combined checkout at `/tmp/rdf4j-xsv-6209-integration` passed 249/0/0/0 in `core/queryresultio/text`.

## Context and Orientation

`core/queryresultio/text` owns SPARQL CSV and TSV tuple result parsers. A `TupleQueryResultHandler` receives `startQueryResult(bindingNames)`, then one `handleSolution(bindingSet)` per row, then `endQueryResult()`. `SPARQLResultsCSVMappingStrategy` and `SPARQLResultsTSVMappingStrategy` preserve the format-specific RDF value conversion. The TSV parser uses `SPARQLResultsTSVReader` so quoted RDF terms are split only on tab characters; ordinary CSV uses OpenCSV's CSV parser. `mvnf --workspace xsv-result-streaming` keeps Maven test output isolated in this worktree.

## Plan of Work

Add `core/queryresultio/text/src/test/java/org/eclipse/rdf4j/query/resultio/text/SPARQLResultsXSVStreamingTest.java` with a CSV and a TSV case. Each input exposes a header and one row, then throws if a parser tries to read later bytes before the handler runs. The handler throws a checked stop exception on its first row; the eager baseline should fail by reading the later source first. Preserve its Surefire report at the worktree root before changing production. Then replace the list-producing parse calls with one-row-at-a-time reads that preserve the existing header capture and mapping rules. Add success, empty, complex lexical, large lazy source, and handler-failure variants before formatting and module tests. Keep no-handler behavior unchanged.

## Concrete Steps

From `/Users/havardottestad/.codex/worktrees/security-xsv-parser-streaming/rdf4j`, the initial quick install passed with:

    python3 scripts/mvn-agent.py --workspace xsv-result-streaming -- -B -ntp -o -Pquick clean install

The retained log is `.mvnf/workspaces/xsv-result-streaming/logs/20261023T055800.870311Z-3203-dda6abb2/maven.log`, ending `BUILD SUCCESS`. Run focused red and green tests with:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace xsv-result-streaming SPARQLResultsXSVStreamingTest#csvDeliversFirstRowBeforeLaterInput --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace xsv-result-streaming SPARQLResultsXSVStreamingTest#tsvDeliversFirstRowBeforeLaterInput --retain-logs

Persist red reports to top-level `initial-evidence.xsv-*.txt`. Then run the complete test class and `core/queryresultio/text` module. Run `scripts/checkCopyrightPresent.sh`, the scoped formatter, and `git diff --check` before review. Test Maven commands must use `mvnf` without `-am` or `-q`.

## Validation and Acceptance

For CSV and TSV, the first row callback must occur before a later input source is read. If the handler fails, parsing must stop without reading that source or sending an end callback. Successful complete input must preserve headers, row order, RDF values, and one end callback. A lazy large input must invoke each callback without building an all-row collection. Existing CSV, TSV, RDF-star, and parser/writer round-trip module tests must remain green.

## Idempotence and Recovery

The worktree began clean from current `origin/main` commit `6b390ad88a9c5b8e79dd0e9afaea0154c8fbfffa`. `.m2_repo` links to the original cache; Maven workspace output and evidence files are untracked and must be preserved. Repeated `mvnf` runs replace current Surefire reports, so save each first failure immediately. Do not stage logs or evidence when publication is approved.

## Artifacts and Notes

The initial install ended `BUILD SUCCESS` after 1:56. Red reports are `initial-evidence.xsv-csv-first-row.txt` and `initial-evidence.xsv-tsv-first-row.txt`, summarized in `initial-evidence.txt`. Final focused log: `.mvnf/workspaces/xsv-result-streaming/logs/20261023T063439.648641Z-16832-03d45a47/verify.log` (11/0/0/0). Final module log: `.mvnf/workspaces/xsv-result-streaming/logs/20261023T063621.128758Z-17938-d7872a54/verify.log` (229/0/0/0).

## Interfaces and Dependencies

Use the existing OpenCSV 5.12.0 reader and parser types and the two existing mapping strategies. Do not introduce a dependency or change public parser interfaces. Keep `SPARQLResultsTSVReader` for TSV lexical semantics. The production parse methods should read each row at most once, call the handler before requesting the next row, and invoke lifecycle callbacks exactly once on success.

Revision note (2026-10-23 06:00Z): Created after inspecting both parsers, the shared mapping strategy, OpenCSV iterator behavior, and the clean current-main worktree. The only in-progress step is a red first-row regression; production remains unchanged.

Revision note (2026-10-23 06:13Z): Preserved separate CSV and TSV Surefire failures after settling on a nonthrowing read-order sentinel. Moved the sole in-progress step to one-row parser implementation.

Revision note (2026-10-23 06:20Z): The shared parser reads a row, maps it with the existing format-specific strategy, and calls the handler before requesting the next row. The focused streaming class passed 2/0/0/0; moved the sole in-progress step to variants and full-module verification.

Revision note (2026-10-23 06:28Z): The expanded focused class passed 6/0/0/0, and the complete text result module passed 224/0/0/0. Moved the sole in-progress step to final diff and compatibility review before publication.

Revision note (2026-10-23 06:40Z): Independent review requested explicit header-only and malformed-later-row lifecycle coverage, plus the mandatory top-level initial-evidence summary and #6209 parser integration check. Moved the sole in-progress step to those additional regressions before publication.

Revision note (2026-10-23 06:40Z): The expanded focused class passed 11/0/0/0 and full text module 229/0/0/0. The sole in-progress step is OpenCSV default comparison and temporary #6209 integration verification.

Revision note (2026-10-23 07:00Z): OpenCSV defaults match the old `CsvToBeanBuilder` for blank lines, quoting, and field mapping on main. #6209's keep-CR setting exposed an independent blank-line reader defect, repaired on #6209 with a separate red-first commit. The combined integration checkout passed 249/0/0/0. The sole in-progress step is separate draft publication and exact-head verification.
