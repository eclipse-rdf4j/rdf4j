# Persist generic result oracles for native Workbench tests

This living ExecPlan follows `.agent/PLANS.md` and Routine D, with focused test-first evidence. Work is on `GH-0000-workbench-native-result-fixtures`, based on exact main `d422c3be34b8bbd4eba244723e3983347eb8f54c`. Latest user direction (2026-10-31) supersedes the earlier PR #6311 integration plan: publish Workbench-only changes on a new branch from HASMAC-AS/rdf4j main and provide a PR creation link; do not create the PR.

## Purpose / Big Picture

Native graph-explorer tests should exercise native query execution and compare its answers to reviewed stored generic-engine answers, rather than re-run the slow generic engine for every native variant. A result bag is a collection retaining duplicate counts. A stored oracle is that expected bag plus query/data identity. Unordered LIMIT permits any correctly sized sub-bag of the complete unsliced answer, not just the first rows from one engine. Normal tests must fail on missing or stale fixtures, never silently regenerate them.

## Progress

- [x] (2026-10-30 18:42Z) Isolate exact main and profile numeric slice.
- [x] (2026-10-30 18:45Z) Observe five failing fixture contract tests.
- [x] (2026-10-30 19:29Z) Implement codec and pass fifteen contracts.
- [x] Replace native matrix reference engine calls.
- [x] (2026-10-30 19:55Z) Generate all 456 reviewed fixtures (7,856,729 bytes), with per-file original revision preserved.
- [x] (2026-10-30 20:44Z) Verify full fresh-JVM module: 2,527 tests, zero failures/errors, one opt-in generator skipped; matrix 294.8 seconds, module 7m27s. Matched numeric timing passed.
- [x] Independent final review cleared; original local commit 98e781f recorded.
- [x] (2026-10-31 06:46Z) Full HASMAC-base validation passed: 2,527 tests, zero failures/errors, one opt-in skip; matrix 278.0 seconds, module 7m05s.
- [in_progress] Publish verified Workbench-only branch and return a PR creation link (do not create PR).

## Surprises & Discoveries

The shared six-store setup took 1.21 seconds and one Node batch 0.219 seconds. `range-29-default` took 28.28 seconds for indexes and 26.90 seconds for adjacency; about 54 of those 55 seconds were initial generic query evaluation and two generic membership witness scans. Native evaluation, telemetry and evidence were subsecond. Date-range generic evaluation had not completed after more than 200 seconds when interrupted to pivot to the user-authorized implementation. The profile is a bounded subset, not a full-suite benchmark. Temporary profile instrumentation was saved outside the checkout and removed before implementation.

## Decision Log

Use explicit offline fixture generation, never lazy golden-file updates in ordinary tests. Store full unsliced bags for deterministic unordered outer LIMIT, preserve literal lexical form/datatype/language/base direction, blank-node IDs fixed by the shared dataset, nested triple terms, unbound variables and duplicate counts. Reject nonrepeatable queries during generation instead of freezing volatile answers. Hash exact generated query, canonical dataset and shared input quads so stale goldens fail loudly. Keep native result repetitions and executed-plan assertions intact. These decisions implement the user's explicit request to measure native behavior against stored generic results; optimizing the generic engine is out of scope.

## Outcomes & Retrospective

Implementation in progress. First five codec contracts passed. Four matrix cases then failed immediately on missing fixtures, proving no generic fallback. Independent review added lossless blank-ID fingerprints, deterministic dataset field order, clock/custom-volatility rejection, parsed-query comparison-mode validation and strict JSON producer/schema handling. The combined policy hardening suite failed as expected (13 tests, 6 failures), then all 13 passed. Per-file provenance regression tests subsequently failed as expected (15 total, 2 failures); all 15 contracts then passed. Full explicit generation passed in 1,485 seconds. The first full Java replay completed 694 other tests with no assertion failures, but its GraphExplorer fork was killed with exit 137 at 20:07Z under concurrent machine load. This is an incomplete validation, not a query failure or confirmed OOM diagnosis. Reports were preserved; retry will run serialized with a 3GB heap. A serialized fresh JVM subsequently passed all 1,020 native-matrix tests (one opt-in generation test skipped) in 311.9 seconds. The complete module ran 2,527 tests but two unrelated Java-page tests failed because skipped frontend bootstrap left compiled classic scripts absent. Building the existing classic TypeScript sources fixed both cases (2/2 passed). The final full-module replay passed on adopted source: 2,527 tests, zero failures/errors, one opt-in generator skipped. Matrix time was 294.8 seconds and module Maven time 7m27s. No production engine changes or commits yet.

## Context and Orientation

`tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/commands/GraphExplorerNativeVariantsTest.java` owns six shared LMDB repositories, generator-backed query cases, two native backends, a repeated generic result helper, and special-case regression tests. `WorkbenchNativeQueryAssertions` writes evidence and asserts efficient executed plans. Add `NativeQueryExpectedResults.java` in the same test package and `NativeQueryExpectedResultsTest.java` for deterministic persistence and assertion contracts. Store fixture files under `tools/workbench/src/test/resources/org/eclipse/rdf4j/workbench/commands/graph-explorer-native-results/`.

## Plan of Work

First run the codec contract tests with a throwing stub and persist Surefire failure evidence. Implement the helper to write/read versioned typed JSON with stable ordering and identity hashes, then make those tests pass. Add a dedicated opt-in generation test in the variants class which invokes the real frontend query generators and generic reference once per unique query/dataset. Ordinary matrix tests load fixtures, execute both native engines twice and telemetry twice, and retain the existing physical-work assertions. Replace the additional slow range77 generic call using the same stored answer. Preserve cheap independent special-case semantic controls unless measurement or scope warrants conversion. Add documentation explaining regeneration, review and fixture invalidation. Finally measure the same numeric slice and run the full modified variants suite and remaining Workbench coverage as feasible.

## Concrete Steps

Run from repository root with Java25 and Maven3.9.11. Use offline Maven and the private `.m2_repo`. Root install prerequisite is `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick -DskipFrontend=true install`, logging to `maven-build.log`. Focused contract tests use `mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl tools/workbench -DskipFrontend=true -Dtest=NativeQueryExpectedResultsTest test`. Native slow-class tests explicitly disable the skip profile with `-P-skipSlowTests`. No test runs use `-am` or `-q`. Final generation and validation commands will be recorded after their interfaces exist.

## Validation and Acceptance

Roundtrip exact RDF terms including nested triples, language-direction literals, fixed blank nodes, unbound bindings and duplicates. Missing files, malformed schema, altered query, altered dataset or altered fixture data must fail clearly. Exact comparisons reject changed multiplicity; unordered LIMIT validates cardinality and subset multiplicity. Normal matrix runs must not invoke generic reference query evaluation for persisted cases. Record before/after on the same selected numeric case and both native backends, but do not extrapolate it as a full-suite speedup. All existing native plan efficiency assertions stay enabled.

## Idempotence and Recovery

Generation is opt-in and writes deterministic output; interrupted generation can be resumed. Existing fixtures are not silently overwritten by normal tests. Preserve all untracked logs and profile artifacts. Do not modify the independent cleanup worker's checkout or Maven cache. Keep instrumentation outside committed source; only adopted test support and fixtures belong in this branch.

## Artifacts and Notes

Initial profiling and patches are in sibling `workbench-profile-evidence/`. A passing root build at exact d422 took1m46s. Numeric baseline phase evidence is in `representative.log`; the broader exploratory run was deliberately interrupted rather than reported as passing.

## Interfaces and Dependencies

Use existing Jackson3, RDF4J model and AssertJ dependencies only. Helper API is `Identity(query,dataset,fixtureFingerprint)`, `write(Path,Identity,List<BindingSet>,Long unorderedLimit)` and `read(Path,Identity)` returning `Expected` with `rows()`, `unorderedLimit()` and `assertMatches(actual,label)`. Null limit means exact full bag; non-null means unordered capped sub-bag with expected count `min(limit,totalRows)`.

Revision 2026-10-30 19:27Z: per-file generator origins are preserved across resumed generation, while manifest generationRevision describes only the current run. Exact-main dependency isolation was verified by private repository paths, distinct inodes, installed/build LMDB matching SHA256 and clean core source. Correctness generation uses a verified3GB JVM bound while another worker runs isolated safety tests; final timings must be uncontended with original heap settings.

Revision 2026-10-30 20:35Z: independent final review validated all 456 oracle files, exact manifest/origin sets, 69,989 expanded rows, and typed triple terms. Matched numeric pair measured 0.707572 seconds (indexes) plus 0.324948 seconds (adjacency), versus original 28.284 and 26.901 seconds: 1.03252 versus 55.185 seconds, about 53-fold for this selection only. Both used Java25 and 8GiB maximum heap; the environment restarted between runs, so this is a bounded local diagnostic, not a controlled CI or full-suite speedup. Temporary timing instrumentation was removed before the final full-module replay. The serialized 3GB correctness run showed GC pressure but finished the matrix; no retained-object/leak diagnosis is claimed.

Final validation evidence (2026-10-30 20:44Z), after prerequisite root install and classic TypeScript bootstrap:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl tools/workbench -P-skipSlowTests -DskipFrontend=true -DskipITs test
    Tests run: 1020, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 294.8 s -- GraphExplorerNativeVariantsTest
    Tests run: 2527, Failures: 0, Errors: 0, Skipped: 1
    BUILD SUCCESS; Total time: 07:27 min

This full Java run used `_JAVA_OPTIONS=-Xmx3g` and uncontended execution. Profiling code is absent from the final source. Reviewer independently checked all resources and adopted code, finding no blocker. No engine production changes, frontend source changes, or timing thresholds were added. The slow tag remains unchanged. Explicit one-time regeneration took about 25 minutes; generic-engine cost is intentionally moved out of normal matrix execution, not optimized. Existing nested-LIMIT/ORDER bag contracts remain unchanged, with fresh-JVM replay passing. Use the measured matched subset rather than extrapolating a full-suite speedup across CI machines.

Revision 2026-10-31 06:32Z: fetched and inspected HASMAC main fe21b29fcd36f022409325acfb14257979599cf6. It differs from the original d422 base only in three adjacency idle/read-admission source/test files. Workbench-only cherry-pick applies cleanly, with no cleanup branch changes. The generated oracle origins intentionally remain d422; full verification on the new main base is required before publication.
