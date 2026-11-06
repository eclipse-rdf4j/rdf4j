# Parse SPARQL XML results without retaining whole responses

This ExecPlan is a living document maintained under `.agent/PLANS.md`.

## Purpose / Big Picture

A remote SPARQL XML result currently makes `AbstractSPARQLXMLParser` mark a buffered stream with `Integer.MAX_VALUE` whenever boolean parsing is attempted. The mark retains all bytes read until the parser finishes or resets, so a long response can consume request-proportional heap even when result rows are otherwise streamed to a handler. After this change, a boolean response and a tuple response can both be parsed without retaining the entire response. The observable proof is a regression whose instrumented input stream detects growing read buffers before the fix and bounded reads after it; ordinary boolean and tuple result callbacks remain correct.

## Progress

- [done] Inspect XML parser and result listeners
- [done] Complete root quick clean install
- [done] Add smallest failing retention regression
- [done] Implement single-pass result dispatch
- [done] Verify focused and module suites
- [done] Format and review scoped change
- [in_progress] Publish reviewed draft PR

## Surprises & Discoveries

- `SPARQLBooleanXMLParser` invokes `parseQueryResultInternal(in, true, false)`, yet the shared code still marks the stream and resets after a SAX error. A boolean-only parse never needs replay.
- The base parser also supports `(true, true)`: it first parses as boolean, then resets and reparses a tuple result. On a long valid tuple result, the first pass and retained mark can scale with the entire response. A single SAX parse can route events based on the `<boolean>` or `<results>` element.

## Decision Log

- Decision: Cover both boolean-only and combined boolean-or-tuple parse modes, not just the boolean subclass. Rationale: both modes currently set the unbounded mark; fixing only one leaves the remote tuple exposure. Date/Author: 2026-10-23 Codex.
- Decision: Use the existing `SimpleSAXParser` listener abstraction for one-pass dispatch and preserve handler callbacks and input ownership. Rationale: the parser already exposes SAX events, so rewinding the input is unnecessary. Date/Author: 2026-10-23 Codex.

## Outcomes & Retrospective

The required root quick clean install completed successfully in `maven-build.log`. Two focused tests now fail before any production edit: the boolean-only parser requested a 1,048,576-byte read, and combined tuple parsing requested a 524,288-byte read. Their Surefire snippets are preserved in `initial-evidence.txt` and `combined-initial-evidence.txt`. The one-pass dispatch passed the two focused retention tests (2/0). A third regression checks combined-mode boolean callbacks and absence of tuple callbacks. The full module suite passed 150 tests with zero failures/errors and one preexisting skip; formatter, copyright, diff review, and parent review are complete; publication remains.

## Context and Orientation

The module is `core/queryresultio/sparqlxml`. `AbstractSPARQLXMLParser.parseQueryResultInternal` wraps the supplied `InputStream` in `BufferedInputStream`, calls `mark(Integer.MAX_VALUE)` for boolean attempts, runs a boolean SAX parser, then resets and runs a tuple SAX parser if the first pass rejects tuple results. `SPARQLBooleanSAXParser` accepts the `<boolean>` element and rejects `<results>`; `SPARQLResultsSAXParser` streams `<result>` elements through the caller's query result handler and rejects `<boolean>`. `SimpleSAXParser` in `core/common/xml` delivers start/end document and start/end tag callbacks. The parser currently closes the input through `UncloseableInputStream.doClose()` when finished; preserve this existing ownership behavior.

## Plan of Work

First add a focused test in `core/queryresultio/sparqlxml/src/test/java/org/eclipse/rdf4j/query/resultio/sparqlxml` that feeds a long valid boolean XML document through `SPARQLBooleanXMLParser` and a long valid tuple XML document through the combined mode. Instrument the underlying stream to record or reject a read request larger than a modest fixed buffer budget; this makes the current mark-driven buffer growth fail without allocating a huge test document. Also assert the parsed boolean or tuple callback values, so the test verifies semantic preservation. Run the smallest failing test with `mvnf`, save the Surefire snippet immediately to root `initial-evidence.txt`, and keep the full log.

Then replace the mark/reset strategy in `AbstractSPARQLXMLParser`. For boolean-only and tuple-only calls, parse once with the corresponding existing listener. For combined mode, feed shared preamble events to both listeners and select one when `<boolean>` or `<results>` appears, then finish only the selected listener. Avoid allocating a response-sized event queue. Preserve the public parser API, SAX error wrapping, handler exception mapping, and current input closure. Verify malformed documents and both result kinds with focused tests, then run the entire module suite.

## Concrete Steps

Work from `/Users/havardottestad/.codex/worktrees/security-sparql-xml-mark/rdf4j` on branch `GH-0000-sparql-xml-streaming`. `.m2_repo` links to the original Maven cache. The root quick install command already succeeded:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use `python3 .codex/skills/mvnf/scripts/mvnf.py <Class>#<method> --workspace sparql-xml-streaming --retain-logs` for focused tests. Preserve red reports with `python3 scripts/agent-evidence.py --command '<exact command>' <module report paths> > initial-evidence.txt`. After code changes, rerun the same selection, then `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryresultio/sparqlxml --workspace sparql-xml-streaming --retain-logs`. Run `scripts/checkCopyrightPresent.sh` from `scripts`, then `mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources` without `-q`, and `git diff --check`. Do not use `-am` or `-q` for tests.

## Validation and Acceptance

Before the fix, the long-response regression must fail in a Surefire report because the buffered parser requests progressively larger reads. After the fix, the same stream must parse the boolean and tuple result correctly without requests growing with response length. Existing boolean, tuple, and RDF-star XML result tests must pass in the full module suite. The input stream must be closed on completion as before, while the parser must not retain all result bytes through a mark.

## Idempotence and Recovery

The isolated branch can repeat installs and tests safely. Retain the initial red evidence before green runs overwrite Surefire reports. Stage only this plan, focused tests, and necessary parser files. If offline resolution misses a dependency, retry the identical command once online and return offline. If a build fails for another reason, retry the install without parallel Maven execution as instructed in AGENTS.md.

## Artifacts and Notes

The root install reported `BUILD SUCCESS` at 2026-10-23T03:10+02:00 in `maven-build.log`. Final formatter `formatting-sparql-xml.log` ended `BUILD SUCCESS`; `copyright-sparql-xml-final.log` confirms valid headers and SPDX; `git diff --check` is clean. The first focused test failed 1/1 with `parser requested 1048576 bytes` in `.mvnf/workspaces/sparql-xml-streaming/logs/20261023T011240.538264Z-55166-affc3ee2/verify.log`; the second failed 1/1 with `parser requested 524288 bytes` in `.mvnf/workspaces/sparql-xml-streaming/logs/20261023T011433.563502Z-56698-1737e40c/verify.log`. Surefire reports were persisted before the next test run. The first offline `mvnf` setup lacked `maven-dependency-plugin:3.11.0`; the required single online retry fetched it and reached the test.

## Interfaces and Dependencies

Use only existing Java, `SimpleSAXParser`/`SimpleSAXListener`, `SPARQLBooleanSAXParser`, and `SPARQLResultsSAXParser` abstractions. No new dependency or public configuration is required. The one-pass dispatch must preserve `QueryResultHandler` callbacks and `QueryResultParseException`/`QueryResultHandlerException` behavior on relevant paths.

Change note, 2026-10-23: Created this plan after source inspection and the required quick install, before any test or production edit.
Change note, 2026-10-23: Captured boolean-only and combined tuple red Surefire reports before production edits and advanced the sole active step to single-pass dispatch.
Change note, 2026-10-23: Implemented one-pass dispatch and observed both red tests green in `.mvnf/workspaces/sparql-xml-streaming/logs/20261023T011713.736563Z-58696-5fdcbc9c/verify.log`; added combined boolean callback coverage and advanced to full module verification.
Change note, 2026-10-23: Full `core/queryresultio/sparqlxml` verification passed 150 tests, zero failures/errors, one skip in `.mvnf/workspaces/sparql-xml-streaming/logs/20261023T011930.120489Z-79027-48866836/verify.log`; advanced to final quality checks.
Change note, 2026-10-23: Formatter, copyright, and whitespace checks passed; parent reviewed the one-pass dispatch and approved scoped draft publication with very low valid-user break risk.
