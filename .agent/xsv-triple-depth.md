# Parse deeply nested RDF-star result terms safely

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Update its progress, findings, decisions, and evidence whenever the implementation changes direction.

## Purpose / Big Picture

A remote SPARQL result may contain an RDF-star triple term, written as `<<( subject predicate object )>>`, nested inside another triple term. CSV and TSV result parsers currently recurse for every nested term. A hostile result can exhaust the Java call stack before the parser returns an RDF value or a normal parse error. After this work, valid nested terms should parse within a documented resource policy, and malformed nesting should fail with a checked parsing error rather than `StackOverflowError`. This must work for both the CSV result syntax and the TSV syntax without changing ordinary values.

## Progress

- [x] (2026-10-23 07:10Z) Prepare isolated main-based worktree.
- [x] (2026-10-23 07:22Z) Reproduce deep parser failures.
- [x] (2026-10-23 07:25Z) Design iterative parsing boundaries.
- [x] (2026-10-23 07:35Z) Implement root-cause parser repair.
- [x] (2026-10-23 07:50Z) Verify focused and module suites.
- [ ] **in_progress** Review compatibility and publish draft.

## Surprises & Discoveries

- `core/queryresultio/text/.../csv/SPARQLResultsCSVMappingStrategy.java` calls its own `parseTripleTerm` recursively through `parseInnerTriple`. It advances by repeatedly slicing a remaining string. CSV terms include unquoted IRIs and CSV-style quoted components, which differ from TSV's N-Triples-like syntax.
- `core/queryresultio/text/.../tsv/SPARQLResultsTSVMappingStrategy.java` sends triple terms to `core/rio/api/.../helpers/NTriplesUtil.java`, where `parseTripleInternal` recursively calls itself for each nested triple and repeatedly slices the remaining string.
- Valid 8192-level N-Triples, TSV, and CSV cases each failed with `StackOverflowError` in the corresponding recursive method. A malformed 8192-level N-Triples term also failed with `StackOverflowError` instead of `IllegalArgumentException`. Full Surefire reports were copied to `initial-evidence.*.txt` before later test runs.
- RDF4J `TripleTerm` implements `Value` rather than `Resource`; a nested triple in subject position is invalid in this model. Initial valid-subject fixtures failed correctly, so the tests were corrected to use nested objects and assert subject rejection.
- #6209 and #6241 conflict in `SPARQLResultsCSVParser`; the disposable integration checkout resolves it by keeping #6209's blank-record preserving reader and #6241's row-at-a-time `parseRows` loop. This parser-depth patch also conflicts with #6209's CSV `NTriplesUtil.unescapeString` change and TSV test imports; both were preserved in the combined checkout.

## Decision Log

- Decision: Keep this fix separate from #6209 CSV/TSV writer escaping and #6241 row streaming. Rationale: Parser nesting is a distinct trust-boundary and compatibility issue. Date/Author: 2026-10-23, Codex.
- Decision: Start with parser-backed valid and malformed nested terms in the in-repository test suites before altering production. Rationale: The stack failure and expected lexical semantics need direct evidence. Date/Author: 2026-10-23, Codex.
- Decision: Use an index cursor and explicit unfinished-term frames separately in NTriplesUtil and the CSV mapper. Keep lexical conversion format-specific; no new public parser API. Require each local `)>>` delimiter when its frame completes. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The parser stack failure is reproduced and preserved. Final `core/rio/api` module tests passed 141/0/0/8; `core/queryresultio/text` passed 224/0/0/0. The disposable #6230+#6209+#6241+candidate checkout passed 254/0/0/0 text tests. A scratch integration assertion also parsed an 8192-level term and exercised #6230's `equals`, `hashCode`, and `stringValue` without recursive failure (1/0/0/0). The remaining work is independent diff/risk review and draft publication.

## Context and Orientation

`core/rio/api/src/main/java/org/eclipse/rdf4j/rio/helpers/NTriplesUtil.java` parses N-Triples-style triple terms and is called by the SPARQL TSV result mapping strategy. `core/queryresultio/text/src/main/java/org/eclipse/rdf4j/query/resultio/text/csv/SPARQLResultsCSVMappingStrategy.java` parses CSV triple terms independently because CSV has different lexical conventions. `TripleTerm` is the RDF4J model value for a triple nested in another RDF value. Both paths receive text from potentially remote query results. Tests belong in `core/rio/api/src/test/java/org/eclipse/rdf4j/rio/helpers/NTriplesUtilTest.java` and `core/queryresultio/text/src/test/java/org/eclipse/rdf4j/query/resultio/text/` or the existing CSV/TSV test packages.

The worktree is `/Users/havardottestad/.codex/worktrees/security-xsv-triple-depth/rdf4j`, initially at main commit `6b390ad88a9c5b8e79dd0e9afaea0154c8fbfffa`, on branch `GH-0000-xsv-triple-depth`. Its `.m2_repo` points to the existing local dependency cache. Maven tests must use `mvnf` without `-am` or `-q`; retain the first red Surefire report in a top-level `initial-evidence.txt` before rerunning.

## Plan of Work

First, add a focused test that constructs a deeply nested valid triple term from the innermost term outward and parses it through `NTriplesUtil.parseTriple`; count the parsed levels iteratively so the assertion itself does not recurse. Add malformed variants with missing or mismatched delimiters. Add CSV and TSV parser-backed tests using their public tuple parse path, including nesting as a subject and an object and interaction with quoted or escaped components. Capture the smallest failing Surefire reports before production edits.

Then replace recursion and repeated whole-suffix slicing at the shared N-Triples parsing boundary with a forward cursor and an explicit stack of unfinished triple frames. Preserve the existing value factory, numeric callback, URI, blank-node, literal, whitespace, and full-input validation semantics. Apply the same cursor/frame discipline to the CSV-specific lexical path while keeping its existing CSV term conversion. Avoid a parser-only special case that merely masks `StackOverflowError`. If model construction or adjacent writers impose a separate limit, record that boundary and make the parser behavior deterministic and documented rather than silently accepting a term that cannot be used.

Finally, run focused tests, then `core/rio/api` and `core/queryresultio/text` module suites. Run the copyright checker, repository formatter, and `git diff --check`. Review the diff against #6209 and #6241 changes without blending those PR scopes. Publish a separate draft only after red/green evidence and independent review.

## Concrete Steps

From the worktree root, run the required offline quick install before tests:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Run smallest tests with:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace xsv-triple-depth NTriplesUtilTest#deepNestedTripleTerm --retain-logs

Run the matching CSV/TSV focused methods after adding them. `mvnf` performs its own root quick install and keeps build/test output under `.mvnf/workspaces/xsv-triple-depth/`. Persist the first failure report immediately as `initial-evidence.txt` and keep the full logs. After production edits, rerun the exact selection, then module tests. No Maven test command may use `-am` or `-q`.

## Validation and Acceptance

The new tests must fail on the unmodified parsers with a stack or uncontrolled error. After repair, a valid deeply nested value returns the expected nested `TripleTerm` structure, including subject and object nesting, and a malformed counterpart raises a normal parse exception. Existing shallow CSV/TSV and N-Triples term tests retain their RDF identities and lexical behavior. A parser callback must not terminate the JVM or worker thread with `StackOverflowError` on attacker-controlled nesting.

## Idempotence and Recovery

Keep all untracked build logs and evidence. If a test run overwrites the current Surefire report, the preserved `initial-evidence.txt` is authoritative for the initial failure. Do not amend previous PRs, touch the original checkout, or delete unexpected files. If a design branch proves incompatible with valid term semantics, update the decision log and return to the smallest red test before editing more production code.

## Artifacts and Notes

The initial root quick install writes full output to `maven-build.log`. Add concise red and green Surefire snippets, exact commands, and final module counts here as they become available.

## Interfaces and Dependencies

Use existing RDF4J `ValueFactory`, `TripleTerm`, `Resource`, `IRI`, and `Value` types. Do not add dependencies or widen public parsing APIs without a demonstrated need. Parser frames and cursors should be private implementation details. Keep the CSV and TSV public parser interfaces unchanged.

Revision note (2026-10-23 07:10Z): Created the main-based worktree and recorded the two recursive parser paths. The sole in-progress step is the red in-repository reproduction; no production code has changed.
Revision note (2026-10-23 07:22Z): Preserved four red Surefire reports. The sole in-progress step is iterative parser design; no production code has changed.
Revision note (2026-10-23 07:25Z): Confirmed CSV shallow syntax and preserved its corrected deep red report. The sole in-progress step is parser implementation.
Revision note (2026-10-23 07:35Z): Iterative parsing is implemented, and formatter/copyright check passed. The sole in-progress step is full verification.
Revision note (2026-10-23 07:50Z): Final module and combined integration gates passed; sole in-progress step is review and publication. Keep all evidence and disposable checkout intact.
