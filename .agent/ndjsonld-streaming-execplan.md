# Stream NDJSON-LD records through both parsers

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Read this file alone to resume the work. NDJSON-LD means one JSON-LD document on each input line. A record is one such line, including a line whose JSON value is an array. The modern parser uses `no.hasmac:hasmac-json-ld`; the legacy parser uses `com.github.jsonld-java:jsonld-java`.

## Purpose / Big Picture

An RDF handler should receive statements from the first NDJSON-LD record before the parser requests a later record. Today both implementations read every line and retain all parsed records before producing a statement; large or open-ended inputs grow memory without bound and cannot deliver incremental results. After this change, both parsers must retain at most the current record and parser-scoped blank-node bookkeeping while preserving JSON-LD meaning, namespace callbacks, security settings, and one start/end lifecycle per parse.

## Progress

- [x] (2026-10-22 21:35Z) Inspect clean worktree and create branch.
- [x] (2026-10-22 21:35Z) Run initial root quick clean install.
- [x] (2026-10-22 21:41Z) Add smallest modern and legacy streaming regressions, run them red, and persist Surefire evidence.
- [x] (2026-10-22 21:46Z) Characterize arrays, blank-node identity, namespace and context behavior with regression tests.
- [x] (2026-10-22 22:03Z) Refactor both parser sessions and process records incrementally.
- [x] (2026-10-22 22:32Z) Bound placeholder collision selection and rerun full suites.
- [x] (2026-10-22 22:47Z) Characterize context overrides, correct callbacks and rerun gates.
- [ ] (in_progress) Review candidate and publish authorized draft PR, then verify exact-head CI.

## Surprises & Discoveries

- Both NDJSON-LD subclasses currently override a whole-document acquisition hook: modern `getDocument` builds a JSON array and duplicates array-line elements; legacy `getJSONObject` collects every line in a `LinkedList`. A per-line call to `super.parse` would reset the parser state and issue repeated lifecycle callbacks.
- The modern JSON-LD library has a per-conversion blank-node identifier generator. Repeating conversions may reuse generated labels, so cross-record identity requires deliberate tests and mapping.
- Each focused reader test fails before the first statement: `Requested a later record before delivering the first statement`. The failures are preserved in `initial-evidence.modern.txt` and `initial-evidence.legacy.txt`.
- The legacy baseline does not emit `ex` for a JSON-LD 1.1 `@prefix` declaration. The review gate requires the callback contract, so the streaming implementation emits explicit prefixes per record and records the observable correction separately in risk notes. Red evidence is in `initial-evidence.legacy-namespace.txt`.
- Record streaming still allocates a complete line and its JSON DOM. A giant individual line can use unbounded memory; a shared configurable JSON-LD document/record size limit should be considered in a separate PR, including the ordinary DOM-based JSON-LD parser.
- JSON-LD context arrays may contain `null` to reset active context. Red Surefire reports for both parsers are preserved in `initial-evidence.modern-context-array.txt` and `initial-evidence.legacy-context-array.txt`; the latter exposed a `Map.of` null crash in the first implementation.
- Reusing explicit `_:same` within one array caused duplicate statements until the rewrite reused one placeholder per label. Same-record, nested, and cross-record identity now have parser-backed tests; red reports are in `initial-evidence.modern-repeated-id.txt` and `initial-evidence.legacy-repeated-id.txt`.
- JSON-LD 1.1 allows `@prefix:true` for an IRI without a trailing delimiter. Both parser tests failed red with `expected: <urn:ex> but was: <null>`; reports are in `initial-evidence.modern-prefix-no-delimiter.txt` and `initial-evidence.legacy-prefix-no-delimiter.txt`. Explicit prefixes now avoid the heuristic delimiter check.
- The first placeholder-prefix choice converted the entire expanded record to a string and could rescan that copy once per crafted candidate prefix. Independent review requested a linear collision choice over actual `@id`/`@type` IRIs with a many-collision parser regression.
- Final review identified possible stale namespace callbacks when a context-array term is redefined or set to null. Parser-backed tests must distinguish actual JSON-LD statement semantics from callback extraction before correction.
- Both focused context-array override regressions failed red with an unwanted `ex` callback, saved in `initial-evidence.modern-context-override.txt` and `initial-evidence.legacy-context-override.txt`. A legacy test formulation that referenced `ex:p` after nulling `ex` exposed a dependency `Context.expandIri` NPE; its report is kept separately as `initial-evidence.legacy-context-override-invalid-reference.txt`. The callback assertions remain and an unaffected `urn:p` statement proves parsing continues.

## Decision Log

- Decision: Refactor the parent JSON-LD parse sessions to offer a record-processing hook instead of duplicating their parser configuration in NDJSON-LD subclasses. Rationale: this preserves the existing loader, base URI, context and error settings while keeping start/end callbacks around the entire stream. Date/Author: 2026-10-22 Codex.
- Decision: Preserve full JSON-LD processing per record, including arrays and nested values, and stream only between records. Rationale: JSON-LD semantics inside a record may require the whole JSON value, but retaining later records is unnecessary. Date/Author: 2026-10-22 Codex.
- Decision: Expand each record with the existing parser options, replace only expanded explicit blank-node identifiers with collision-free per-record IRI placeholders, and translate those RDF resources back through parser-scoped blank-node creation. Map library-generated blank-node identifiers to fresh nodes within each record. Rationale: both JSON-LD libraries regenerate blank-node names for each conversion, so plain per-line conversion can alias unrelated records or lose repeated explicit identities. Date/Author: 2026-10-22 Codex.

## Outcomes & Retrospective

The baseline root quick install succeeds. Streaming, namespace, repeated-label, context-array, no-delimiter prefix, and context-override regressions failed as intended and their reports are preserved. After context-override correction, focused classes pass (modern 16/16, legacy 17/17); complete modern module passes 596 tests with 4 skips and legacy passes 353 tests with 3 skips. Publication and exact-head CI remain.

## Context and Orientation

Work only in `/Users/havardottestad/.codex/worktrees/security-ndjsonld-streaming/rdf4j` on branch `GH-0000-ndjsonld-streaming`. The current main source is clean before edits. `core/rio/jsonld/src/main/java/org/eclipse/rdf4j/rio/jsonld/JSONLDParser.java` owns the modern parse lifecycle, options, secure document loader, namespace extraction and RDF value creation; its NDJSON-LD subclass is `core/rio/jsonld/src/main/java/org/eclipse/rdf4j/rio/ndjsonld/NDJSONLDParser.java`. The corresponding legacy files are under `core/rio/jsonld-legacy/src/main/java/org/eclipse/rdf4j/rio/jsonld/legacy/JSONLDParser.java` and `.../ndjsonld/legacy/NDJSONLDParser.java`. The two `NDJSONLDParserTest.java` classes live under each module's matching `src/test/java` tree. An RDF handler receives `startRDF`, zero or more namespaces and statements, then `endRDF`. `AbstractRDFParser.createNode` provides parser-scoped blank-node identity until `clear()`.

The root worktree `.m2_repo` is a symlink to the original checkout's Maven cache. Use `mvnf --workspace ndjsonld-streaming --retain-logs` for isolated repository head, build files, test reports and logs; never run tests with `-am` or `-q`. Keep untracked logs and evidence. Do not edit the CSV/TSV security worktree or the original checkout.

## Plan of Work

First add a focused method to each NDJSON-LD test class. Its `Reader` supplies one complete JSON-LD line and rejects a request for the next line until a handler has seen the first statement. Run each method and save its failing Surefire result at the worktree root before modifying production code. Add behavioral variants for a line containing an array, nested objects and values, blank nodes explicitly labeled across lines, anonymous blank nodes that must stay distinct, namespaces, graph contexts, malformed later lines and configured secure loaders. Preserve existing assertions.

Then expose one reusable parse-session hook in each parent JSON-LD parser. Keep option and document-loader construction, error translation, `clear()`, and handler lifecycle in the parent. The ordinary JSON-LD path processes its one document; the NDJSON-LD path reads one line, parses it into one bounded record, and passes it to the same session. Modern and legacy callback/value factories must maintain explicit blank-node identity across records while preventing newly generated nodes from aliasing nodes from other records or explicit labels. Update this plan with the tested mapping design before implementing it. Emit statements before reading the next line. Handle an array as one record without duplicating its values.

## Concrete Steps

From the worktree root, the baseline install already ran as:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use these commands for the red and green loops:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace ndjsonld-streaming --retain-logs --module core/rio/jsonld NDJSONLDParserTest#firstRecordReachesHandlerBeforeLaterInput
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace ndjsonld-streaming --retain-logs --module core/rio/jsonld-legacy NDJSONLDParserTest#firstRecordReachesHandlerBeforeLaterInput
    python3 scripts/agent-evidence.py --command '<exact failed command>' <workspace Surefire directory> > initial-evidence.<module>.txt

The red result must show the later input was requested before the first statement. After the change, both methods and the complete `core/rio/jsonld` and `core/rio/jsonld-legacy` module suites must pass. Capture their Surefire summaries in final evidence files. Run `cd scripts && ./checkCopyrightPresent.sh`, then `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`, and inspect `git diff --check` and the exact file list before a scoped commit. This formatting command uses `-q` only outside tests.

## Validation and Acceptance

The primary observable test passes only if a handler receives the first statement while the custom input source still withholds the next record. A two-record file with the same explicit blank-node `@id` must use the same RDF blank node; two anonymous records must use distinct RDF blank nodes. Nested values, arrays, graph contexts and namespaces must preserve their meaning. Error and secure-loader tests must prove the parent parser options still apply. Ordinary JSON-LD parser suites must remain green because the shared session changes affect those parsers too.

## Idempotence and Recovery

The Maven workspace keeps reports and logs for review. Repeat focused tests without deleting unrelated artifacts. If offline resolution misses a cached artifact, retry the same Maven operation once online and return offline. If a red test unexpectedly passes, strengthen only its evidence condition before touching production code. If a later test fails, keep its report and fix the responsible parser behavior instead of muting the assertion. The branch and worktree are isolated; do not reset or clean them.

## Artifacts and Notes

The initial `maven-build.log` records a root `BUILD SUCCESS` in about one minute. The modern and legacy focused Surefire reports both show `Tests run: 1, Failures: 1, Errors: 0` with the input-withholding assertion; `initial-evidence.modern.txt` and `initial-evidence.legacy.txt` preserve their summaries. `.m2_repo` is a read-through symlink and `.mvnf/workspaces/ndjsonld-streaming/` holds isolated reports.

## Interfaces and Dependencies

Do not add dependencies. Keep the existing public `RDFParser.parse(InputStream|Reader, baseURI)` interfaces. The new parent parser hook may be protected and internal to the `jsonld` and `jsonld-legacy` modules. The modern implementation must retain `JsonLdOptions`, `CachingDocumentLoader`, `JSONLDSettings`, and `RdfConsumer`/`RdfValueFactory` behavior. The legacy implementation must retain configured Jackson `JsonFactory`, `JsonLdOptions`, `JsonLdProcessor.toRDF`, and `JSONLDInternalTripleCallback` semantics. Use existing `AbstractRDFParser.createNode` mapping where appropriate.

## Revision Notes

- 2026-10-22: Initial plan written after source inspection and baseline install; the exact blank-node mapping design awaits red-first characterization tests.
- 2026-10-22: Both parser implementations failed the smallest incremental-read test and their Surefire results were saved before any production edit. The active work is semantic characterization.
