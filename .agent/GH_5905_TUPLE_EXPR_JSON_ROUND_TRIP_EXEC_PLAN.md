# Implement canonical TupleExpr JSON round trips through every optimizer

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept current while work proceeds. It follows `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

After this work, an RDF4J caller can export a supported `TupleExpr` to readable, versioned JSON and import it as a fresh algebra tree. The feature is demonstrated both by focused codec tests and by running the repository's manifest-backed SPARQL query corpus through JSON at parse time and after every standard optimizer. The decoded tree is deliberately fed to the next optimizer, proving that the representation preserves the state needed by the complete evaluation pipeline rather than merely producing a similar-looking tree.

The observable success criteria are that the JSON module's focused tests pass, that the compliance corpus executes every decoded checkpoint with the same query-observable result as an un-serialized control at the same optimizer stage, and that final decoded plans continue to satisfy supported manifest expectations. Nine RDF/SPARQL compatibility cases, five unapproved or withdrawn SPARQL 1.0 entries, and one malformed local SRX result skip only their final manifest comparison.

## Progress

- [x] (2026-07-14 20:12Z) Inspect attachments, parser patch, JSON format, optimizer pipeline, and SPARQL manifests.
- [x] (2026-07-14 20:12Z) Run the required pre-change offline root clean install; all reactor projects passed in 41.922 seconds.
- [x] (2026-07-14 20:17Z) Import the supplied parser module, add Codex signatures, integrate reactor/BOM entries, and pass all nine parser tests.
- [x] (2026-07-14 20:18Z) Add and observe the focused golden exporter contract failing because `TupleExprJsonWriter` does not yet exist.
- [x] (2026-07-14 20:33Z) Implement the canonical whitelist-based `TupleExprJsonWriter`, checked write exception, and the missing read accessor for the `Join.cacheable` semantic flag.
- [x] (2026-07-14 20:33Z) Complete exhaustive codec, canonicalization, limits, ownership, and unsupported-state tests; all 16 JSON-module tests pass.
- [x] (2026-07-14 21:03Z) Build the single-session optimizer checkpoint harness, real-MemoryStore strategy tests, and focused feed-forward tests.
- [x] (2026-07-14 22:05Z) Integrate SPARQL 1.0, 1.1, and 1.2 manifest shards; execute 607 queries at 19 checkpoints and lock the complete 712-entry census.
- [x] (2026-07-14 22:16Z) Pass focused and affected-module tests, scoped copyright/signature checks, formatting, the full SPARQL compliance module, and the final offline root clean install.

## Surprises & Discoveries

- Observation: The supplied attachment implements a strict importer but no exporter. It adds `core/queryalgebra/json`, a version-1 envelope, parser overloads for Jackson trees, strings, readers, and streams, and a whitelist covering the native tuple/value-expression model.
  Evidence: `/tmp/codex-remote-attachments/019f6069-bea8-71f2-b793-2baf2d446467/542AAA70-A473-438D-A70A-A74135ADE017/1-GH-5905-tuple-expr-json-parser.patch` applies cleanly to the current working tree.

- Observation: The current standard pipeline has eighteen production optimizer entries. With Java assertions enabled it also inserts `ParentReferenceChecker` instances, which are verification instrumentation and must not become separate JSON stages.
  Evidence: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java` constructs eighteen production entries and conditionally surrounds them with parent checkers.

- Observation: SPARQL parsing can attach a transient `QueryScopeSeed` to `QueryRoot`. The JSON format intentionally omits it, while the scope resolver can reconstruct conservative facts from the imported algebra and the preserved optimization tags.
  Evidence: `core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/QueryRoot.java` declares the seed transient, and `ScopeResolver` accepts a null seed.

- Observation: The original 712 census combines 607 executable query cases, five queries using syntax the current parser rejects, 66 entailment-regime evaluations, and 34 protocol tests. The syntax-only set is the two obsolete SPARQL 1.0 decimal forms plus three local bounded-path extensions. The protocol entries have no `qt:query`; the entailment cases are filtered because a plain `MemoryStore` does not implement their RDF/RDFS/OWL regimes. Executable counts are 247 SPARQL 1.0 W3C, 115 local SPARQL 1.1, 179 SPARQL 1.1 W3C, 9 local SPARQL 1.2, and 57 SPARQL 1.2 W3C. Seven endpoint-dependent SERVICE cases remain separately excluded.
  Evidence: `TupleExprJsonCorpusCensusTest` traverses the manifests and proves `607 + 5 + 66 + 34 = 712`. `Basic - Term 6`, `Basic - Term 7`, and local `sparql11-sequence-04` through `-06` fail in `SPARQLParser` before a `TupleExpr` exists.

- Observation: Optimizer stages are not all individually equivalent to the unoptimized algebra. In the approved SPARQL 1.0 `join-combo` cases, `UnionScopeChangeOptimizer` deliberately repairs transitional scope semantics, changing the observable intermediate result to the correct final one.
  Evidence: The initial corpus run reported `join-combo-1` changing from one row to two and `join-combo-2` from zero rows to one at checkpoint 9. The corrected harness compares serialized and un-serialized trees at the same stage; the exact approved `join-combo-1` fixture is green through both the ordinary pipeline and all checkpoints.

- Observation: Jackson-created immutable argument lists exposed a model mutation defect in `NAryValueOperator`: optimizer child replacement called `ListIterator.set` and did not restore the replacement parent.
  Evidence: `TupleExprJsonParserTest#parsedNaryChildrenCanBeReplacedWithValidParentLinks` failed with `UnsupportedOperationException` before the model fix and passes after copying arguments into an `ArrayList` and assigning the replacement parent.

- Observation: Native aggregate algebra may reuse a value-expression object as a semantic alias in more than one field. This is not a recursive cycle and must be expanded into independent JSON subtrees because version 1 has no reference syntax.
  Evidence: W3C GROUP_CONCAT cases exposed shared `ExtensionElem` and `ValueConstant` aliases. `TupleExprJsonWriterCoverageTest#serializesSharedNodesAsIndependentSubtrees` failed under the old ever-seen guard and passes with an active-recursion guard.

- Observation: The local BSBM query-5 expected SRX is malformed: indentation and newlines are embedded inside every product `<uri>`, so its expected IRIs differ from the valid query output even though the diagnostic rendering appears almost identical.
  Evidence: Direct inspection of `testcases-sparql-1.1/bsbm/bsbm-bi-q5.srx` shows whitespace before each closing `</uri>`; same-stage JSON/control comparisons pass and only the final manifest assertion fails.

- Observation: `Bound` is not a `UnaryValueOperator`, even though its JSON shape is unary; it owns a `Var` child directly. The exhaustive discriminator test exposed and pinned this distinction.
  Evidence: The first `TupleExprJsonWriterCoverageTest` run failed at `TupleExprJsonWriter.java:290` with `ClassCastException: Bound cannot be cast to UnaryValueOperator`; the dedicated encoder now passes.

- Observation: The repository-wide copyright helper recursively enters the numerous `.claude/worktrees` directories, so a full invocation did not terminate promptly. A scoped equivalent checked every changed Java source without traversing those unrelated worktrees.
  Evidence: The initial helper invocation was interrupted without filesystem changes after prolonged traversal. The final scoped audit checked 20 Java files for the canonical copyright header, SPDX line, and Codex signature with zero failures.

## Decision Log

- Decision: The exporter will be a stateless explicit whitelist named `TupleExprJsonWriter`, with `TupleExprJsonWriteException` extending `IOException`.
  Rationale: This is symmetric with the supplied parser and prevents parent links, caches, third-party subclasses, or future Java fields from leaking into the format accidentally.
  Date/Author: 2026-07-14 / Codex.

- Decision: The writer always emits the `rdf4j-tuple-expr` version-1 envelope, compact UTF-8, deterministic field order, lexically sorted unordered sets/maps, and list order for semantically ordered values.
  Rationale: One native tree must have a stable portable representation suitable for diffs and exact export-import-export assertions. The parser may continue accepting a bare node for compatibility.
  Date/Author: 2026-07-14 / Codex.

- Decision: The optimizer harness will perform the initial JSON checkpoint before opening `OptimizationSession`, then run all production optimizers and post-optimizer checkpoints in one normal session.
  Rationale: Clearing the omitted `QueryScopeSeed` before the session opens proves the imported tree's fallback behavior, while one session preserves the actual pipeline's query-local analysis semantics.
  Date/Author: 2026-07-14 / Codex.

- Decision: Every checkpoint compares the imported plan against a separate un-serialized control plan after the same optimizer, not against one unoptimized baseline. Context-aware wrappers delegate each real optimizer inside one session; only a JSON transplant invalidates the tested session's node-identity analysis.
  Rationale: Transitional optimizer states may legitimately differ from the unoptimized tree. Same-stage control detects serialization loss without requiring every optimizer to be a semantic fixed point, and wrappers avoid injecting fake legacy optimizers into session state.
  Date/Author: 2026-07-14 / Codex.

- Decision: A stable native `QueryRoot` acts as the mutable carrier. Each decoded root contributes its child and common root state to that carrier, and the carrier's transient seed is replaced with the decoded seed, normally null.
  Rationale: RDF4J optimizers receive a fixed root reference and mutate it. Replacing the carrier's imported contents lets subsequent optimizers consume freshly decoded nodes without inventing multiple optimizer sessions.
  Date/Author: 2026-07-14 / Codex.

- Decision: Deterministic tuple results use bag comparison unless the manifest requires order; graph results use RDF graph isomorphism; boolean results compare truth values. Volatile queries execute every stage but use an audited function-specific invariant policy.
  Rationale: These comparisons match query-observable semantics while avoiding false failures from blank-node identifiers and legitimately changing RAND, UUID, STRUUID, NOW, or BNODE values.
  Date/Author: 2026-07-14 / Codex.

- Decision: All 607 manifest-backed cases that can be parsed into and executed as a `TupleExpr` on the required plain `MemoryStore` run checkpoint equivalence. The census separately locks five syntax-only cases, 66 entailment entries, and 34 protocol entries. Nine compatibility cases, five unapproved/withdrawn entries, and one malformed expected-result file skip only final comparison; seven endpoint-dependent SERVICE cases remain outside the broad corpus and receive focused structural codec coverage.
  Rationale: A protocol record or unparseable query cannot enter an optimizer, and running entailment manifests on a non-entailing store would make expected results invalid. Keeping every category in the census makes the correction explicit while maximizing hermetic algebra coverage.
  Date/Author: 2026-07-14 / Codex.

## Outcomes & Retrospective

Implementation is complete. The pre-change reactor baseline and final reactor build are green, the requested public API is delivered, and the executable manifest corpus passes every JSON checkpoint.

The parser foundation milestone is complete: the supplied module is present, its new Java sources carry the required 2026 header and Codex signature, and `TupleExprJsonParserTest` passes 9 tests with no failures or errors.

Exporter TDD is now in its recorded red state. `TupleExprJsonWriterTest#roundTripsSuppliedExample` ran through Surefire and failed with one error, `ClassNotFoundException: org.eclipse.rdf4j.query.algebra.json.TupleExprJsonWriter`, proving the requested public behavior is absent before production implementation.

The exporter milestone is complete. The writer emits only the version-1 envelope, explicitly dispatches all 29 tuple and 46 value-expression discriminators, preserves non-default semantic/planner state, canonicalizes unordered collections, emits `UNDEF` as null, enforces identity/depth/string/finite-number invariants, and never owns caller streams. The final formatted `core/queryalgebra/json` run passes 18 tests with no failures or errors; the retained log is `logs/mvnf/20260714-221103-verify.log`.

The optimizer harness milestone is complete. Its fake-optimizer test proves decoded nodes feed the next optimizer, and its real strategy test executes ordered tuple, graph, ASK, volatile-BNODE, and the approved transitional union-scope case at all nineteen checkpoints against a real `MemoryStore`. The focused strategy class reports six green tests. The corpus census correction distinguishes 607 executable queries from five syntax-only queries, 66 non-MemoryStore entailment entries, and 34 non-query protocol entries in the original 712 inventory.

The corpus milestone is complete. The SPARQL 1.0, 1.1, and 1.2 shards execute 247, 294, and 66 cases respectively. All 607 executable cases pass the baseline plus eighteen post-optimizer round trips, for 11,533 JSON checkpoints. The complete `compliance/sparql` verification passes 3,263 tests with zero failures or errors and three existing skips; the retained log is `logs/mvnf/20260714-221137-verify.log`.

Final hygiene and build verification are complete. Formatting exited successfully, `git diff --check` reports no errors, and the scoped copyright/signature audit passes all 20 changed Java sources. SHA-256 checks before and after formatting prove the three unrelated dirty Java files are byte-for-byte unchanged. The final offline root `-Pquick clean install` reports `BUILD SUCCESS` for the complete reactor in 1:43; its full output is retained in `maven-build.log`.

## Context and Orientation

The supplied patch creates `core/queryalgebra/json`, whose main package is `org.eclipse.rdf4j.query.algebra.json`. `TupleExprJsonParser` maps a documented JSON object graph into native classes from `core/queryalgebra/model`. The new writer belongs beside the parser and depends only on the model/query APIs and Jackson; optimizer/evaluation dependencies must remain test-only and outside this core module.

`StandardQueryOptimizerPipeline` in `core/queryalgebra/evaluation` builds the actual optimizer instances from the active evaluation strategy, triple source, and statistics. `OptimizerPipelineRunner` opens one `OptimizationSession` and invokes each optimizer. The corpus harness belongs in `compliance/sparql`, which already depends on the SPARQL testsuite, MemoryStore, parser, repository, and evaluation stack. It will add a test dependency on the new JSON module rather than creating a reverse dependency from a core module to a testsuite.

`SPARQLQueryComplianceTest` in `testsuites/sparql` loads manifest entries, uploads their default and named graph data, prepares each query, and compares tuple, graph, or boolean results with manifest files. It exposes `testParameterListener`, which the new corpus tests can use to capture query URL, query type, ordering, dataset, and volatility metadata before constructing the test-specific MemoryStore.

A checkpoint means one export-import boundary. Checkpoint zero occurs immediately after query parsing and before optimization. Checkpoints one through eighteen occur after each production optimizer. At a checkpoint the harness serializes the current root, parses a fresh root, serializes it again, verifies the two JSON trees are equal, verifies all child-parent links, executes the imported root with optimization disabled, compares it with an un-serialized control root at the identical stage, then makes the imported tree the input to the next optimizer.

## Plan of Work

First import the supplied parser patch as one coherent foundation. Add the required Codex signature comment beneath every new Java copyright header, copy the supplied example JSON into test resources, and merge the format document's canonical-export rules into the module README. Confirm aggregation in `core/queryalgebra/pom.xml`, dependency management in `bom/pom.xml`, and the new module POM. Do not modify the unrelated dirty files under `core/queryalgebra/equivalence`, `core/queryalgebra/model`, or `.claude/worktrees`.

Before writing exporter production code, add `TupleExprJsonWriterTest` with the smallest public contract: parse the supplied example, export it to a JSON tree, parse the result, and assert export-import-export JSON equality. Run that method and record its failure. Then add `TupleExprJsonWriteException` and `TupleExprJsonWriter`.

The writer will build Jackson object nodes explicitly. It will maintain per-call path, depth, and identity-cycle state. It will reject a node unless its concrete class is a supported native class; `instanceof` alone must not permit arbitrary subclasses. It will mirror every field consumed by `TupleExprJsonParser`, including common metadata and non-default semantic flags. It will serialize IRI, blank node, literal (including language direction), and RDF-star triple values. It will iterate `BindingSetAssignment` once, serialize names deterministically, and write absent row bindings as JSON null. It will reject non-finite estimates, structural cycles, strings beyond the parser's limit, and nesting beyond the parser's maximum. Writer and stream overloads flush their own Jackson generator state but never close the caller's target.

Expand focused tests to cover every parser-supported tuple/value-expression discriminator, all RDF value variants, non-default metadata, deterministic set/map ordering, UNDEF rows, output overloads, target ownership, depth/string bounds, cycles, malformed required state, unsupported subclasses, transient omissions, and reconstructed parent links. Use the supplied example as a golden JSON-tree resource. Add a discriminator inventory assertion so parser and writer coverage cannot drift silently.

Next create test-only checkpoint support in `compliance/sparql`. A custom `DefaultEvaluationStrategyFactory` creates a strategy that overrides `optimize`. It builds an un-serialized control clone, performs checkpoint zero, and transplants imported contents into a stable `QueryRoot`. It obtains the real optimizer list from `StandardQueryOptimizerPipeline`, removes only `ParentReferenceChecker` instrumentation, and asserts the eighteen-stage class/order inventory. Context-aware wrappers delegate each production optimizer inside one `OptimizerPipelineRunner` session, evaluate the corresponding stage, perform the checkpoint, and invalidate the tested session after fresh node identities are transplanted. A separate control session records the outcome after each same-stage optimizer without serialization.

Add focused harness tests with fake optimizers. One test records child identities to prove optimizer N+1 receives nodes created by checkpoint N. Another uses a counter-backed no-optimizer evaluator to prove checkpoint evaluation does not recursively invoke the wrapped optimizer pipeline. Parent validation must run before export and after import, and failures must report the manifest URI, query URL, stage number/name, summarized outcomes, and first differing JSON pointer.

Add a protected `shouldCompareExpectedResult(String testName)` hook to `SPARQLQueryComplianceTest`, returning true by default. When false, its dynamic case still fully consumes the actual result so the final imported plan executes, but it does not read or compare the manifest result. Centralize the existing nine compatibility exception names so the normal compliance suites and JSON corpus cannot drift.

Create three corpus test shards in `compliance/sparql` for SPARQL 1.0, combined local/W3C SPARQL 1.1, and combined local/W3C SPARQL 1.2. They use the existing manifest traversal and dataset upload behavior, exclude only the `service` subdirectory, and assert executable counts of 247, 294, and 66 respectively, plus a total query guard of 607. A separate audit asserts the five syntax-only queries, 66 filtered entailment entries, 34 non-query protocol entries, and full total of 712. The shards do not add the normal ignored-test list, so every parseable hermetic case runs checkpoints. Supported cases retain final manifest comparison; audited compatibility, manifest-status, and malformed-result cases return false from the new hook.

The test context classifies parsed query type and scans its value expressions. Deterministic tuple stages use `QueryResults.equals`, with a second positional comparison when the manifest marks the result ordered. Graph stages convert valid `subject`, `predicate`, `object`, and optional `context` bindings to statements and compare with `Models.isomorphic`. Boolean stages compare whether the iteration has a result. Any deterministic failure compares exception category and cause category, not message text. Volatile cases must appear in a checked allowlist and use per-function validators for result cardinality, binding names, RDF kind/datatype, RAND range, UUID/STRUUID shape, NOW datatype and within-evaluation consistency, and BNODE identity relationships. Discovery of a new volatile case without policy is a failure.

Finally run focused and module verification, copyright checks, formatter/import sorting, the affected module suites, and the required final root clean install. Review only intended diffs and preserve all pre-existing tracked and untracked artifacts.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-stf`. Test commands must never use Maven `-am` or `-q`, and every Maven command must use `.m2_repo`.

The completed pre-change baseline command was:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

It produced `BUILD SUCCESS` with total time `41.922 s`; the full output is in `maven-build.log`, and the compact record is appended to `initial-evidence.txt`.

After adding the first writer test, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py TupleExprJsonWriterTest#roundTripsSuppliedExample --retain-logs

Expect a focused failure before `TupleExprJsonWriter` exists or satisfies the contract. After implementation, rerun the same selection and expect one passing test, then run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/json --retain-logs

Run focused checkpoint harness tests by their class/method selectors, then execute the three compliance shards through Failsafe using `mvnf --it` if named `*IT`, or the ordinary selector if named `*Test` under the compliance module's Failsafe configuration. Finish with:

    python3 .codex/skills/mvnf/scripts/mvnf.py compliance/sparql --retain-logs

Before final verification, run `scripts/checkCopyrightPresent.sh` from the `scripts` directory. Format and sort imports/resources with:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Then repeat the full offline root clean install command and inspect `git status --short` and scoped diffs.

## Validation and Acceptance

The JSON module is accepted when the supplied example exports as a version-1 envelope, parsing it returns a fresh native tree with correct parents, a second export produces an equal Jackson tree and identical compact string, every supported discriminator has a passing round-trip fixture, and every documented rejection produces `TupleExprJsonWriteException` with useful type/path context.

The optimizer harness is accepted when its focused identity test proves each next optimizer receives the prior decoded child graph, its inventory lists the eighteen production stages in the repository's current order, and it evaluates checkpoint roots without recursively optimizing them.

The corpus is accepted when shard census tests report 247, 294, and 66 executable entries, all 607 query entries reach nineteen checkpoints, and the audit separately locks five syntax-only, 66 entailment, and 34 protocol entries for a full manifest inventory of 712. Deterministic same-stage outcomes must remain equal, every volatile case must have an explicit invariant policy, all supported final imported plans must match manifest results, and only audited compatibility or invalid-manifest cases skip final expected comparison. A stage failure must identify the exact query and optimizer and provide a compact structural or semantic difference.

Final acceptance requires green focused tests, green `core/queryalgebra/json` verification, green `compliance/sparql` verification, clean copyright/format checks, and a green offline root clean install. Unrelated pre-existing working-tree changes must remain byte-for-byte untouched.

## Idempotence and Recovery

The supplied patch was checked with `git apply --check` before use. If importing it partially fails, stop and inspect the affected files rather than overwriting tracked content. All subsequent edits are additive or narrow modifications and can be repeated after inspecting `git diff`. Build outputs under `target`, `.m2_repo`, `logs`, `maven-build.log`, and existing evidence files are retained as required.

If offline dependency resolution fails, rerun the exact failed install once without `-o`, then return to offline operation. If the parallel root install fails for a non-resolution reason, rerun the same command without `-T 1C`. If a corpus failure reveals a legitimate volatile expression, update the allowlist only after recording the query, function, and stable observable invariant in this plan's Decision Log; never broadly weaken deterministic comparisons.

## Artifacts and Notes

Supplied inputs are:

    /tmp/codex-remote-attachments/019f6069-bea8-71f2-b793-2baf2d446467/542AAA70-A473-438D-A70A-A74135ADE017/1-GH-5905-tuple-expr-json-parser.patch
    /tmp/codex-remote-attachments/019f6069-bea8-71f2-b793-2baf2d446467/542AAA70-A473-438D-A70A-A74135ADE017/2-tuple-expr-example.json
    /tmp/codex-remote-attachments/019f6069-bea8-71f2-b793-2baf2d446467/542AAA70-A473-438D-A70A-A74135ADE017/3-GH-5905-tuple-expr-json-parser-source.zip
    /tmp/codex-remote-attachments/019f6069-bea8-71f2-b793-2baf2d446467/542AAA70-A473-438D-A70A-A74135ADE017/4-GH-5905-tuple-expr-json-format.md

The pre-change evidence appended to `initial-evidence.txt` is:

    Command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    Report: maven-build.log
    Summary: BUILD SUCCESS; total time 41.922s.

## Interfaces and Dependencies

The production module exposes:

    public final class TupleExprJsonWriter {
        public JsonNode toJsonNode(TupleExpr tupleExpr) throws TupleExprJsonWriteException;
        public String toJson(TupleExpr tupleExpr) throws TupleExprJsonWriteException;
        public void write(TupleExpr tupleExpr, Writer target) throws IOException;
        public void write(TupleExpr tupleExpr, OutputStream target) throws IOException;
    }

    public class TupleExprJsonWriteException extends IOException

`TupleExprJsonWriter` uses the Jackson dependencies already introduced by the parser patch and the RDF4J model/query APIs. It has no dependency on evaluation, repositories, testsuites, or compliance modules. The compliance module adds a test-scoped dependency on `rdf4j-queryalgebra-json`.

`SPARQLQueryComplianceTest` gains one protected default-true test-support hook:

    protected boolean shouldCompareExpectedResult(String testName)

No existing caller changes behavior unless it overrides that hook. The existing Java-serialization-based benchmark codec and endpoint-dependent SERVICE integration tests remain unchanged.

Revision note (2026-07-14 20:12Z): Created the self-contained execution plan from the approved implementation plan and recorded the green pre-change reactor baseline.

Revision note (2026-07-14 20:17Z): Recorded successful parser import, repository integration, source signatures, and the green nine-test parser baseline.

Revision note (2026-07-14 20:18Z): Recorded the focused exporter contract and its expected pre-implementation Surefire failure.

Revision note (2026-07-14 20:33Z): Recorded the completed canonical writer, exhaustive codec coverage, the `Bound` model-shape discovery, and the green 16-test JSON-module verification.

Revision note (2026-07-14 22:16Z): Recorded the completed corpus, corrected 712-entry census, final 18-test JSON verification, 3,263-test SPARQL compliance verification, scoped hygiene audit, and green final reactor clean install.
