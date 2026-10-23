# Integrate RDF parsing into SPARQL queries

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current. This work uses Routine D for a complex feature, while reproducing shared behavior defects before transferring production changes.

## Purpose / Big Picture

Applications will be able to parse inline Turtle, TriG, N-Triples, and N-Quads documents inside SPARQL on any Sail, through ordinary RDF4J query execution. Applications must not add a parser Sail, wrapper or opt-in configuration. Parsing produces rows through property functions and a local SERVICE whose graph patterns query a private parsed dataset. SELECT, CONSTRUCT, and ordinary INSERT WHERE can consume the resulting bindings. Each document must parse completely within explicit limits before rows are returned, and independent invocations must retain separate blank-node identities.

## Progress

- [x] (2026-10-23) Verify archive and clean baseline context.
- [x] (2026-10-23) Complete initial root offline clean install.
- [x] (2026-10-23) Reproduce shared evaluator defects before integration.
- [x] (2026-10-23) Transfer feature through semantic cherry picks.
- [x] (2026-10-23) Validate final scoped dependency composition controls.
- [x] (2026-10-23) Format and verify final parser module.
- [x] (2026-10-23) Complete independent implementation and scope review.
- [x] (2026-10-23) Publish approved patch and verify heads.
- [x] (2026-10-23) Reproduce plain LMDB parser omission directly.
- [x] (2026-10-23) Design common all-Sail execution integration path.
- [x] (2026-10-23) Integrate shared parser runtime and rewriting.
- [x] (2026-10-24) Verify plain Sails stacks and strategies.
- [x] (2026-10-24) Format sources and audit final build.
- [ ] [in_progress] Review staged scope and publish follow-up.

## Surprises & Discoveries

The user's follow-up requires `new LmdbStore()` to provide parsing without `RdfParserSail`. All existing LMDB parser tests construct that wrapper, so they do not prove the requested default. The parser module currently has a test dependency on LMDB; adding LMDB's required parser runtime dependency therefore requires moving those cross-backend tests downstream, not dropping their coverage.

Before any production edit, the user expanded the requirement to all Sails: “Rdfparser functions and stuff should not require a new sail. It should be part of rdf4j and work on any sail.” The planned LMDB-specific runtime dependency and constructor options are superseded. The design must use a common query layer and must distinguish common evaluator support from arbitrary custom Sail query engines.

The optional LMDB query-result cache reuses fresh parser blank nodes across independent evaluations even with the existing wrapper. `RdfParserIntegrationTest#cachedQueriesRetainFreshParsedBlankNodes` ran two cases with two failures: the wrapped LMDB case reused the same blank node, and direct LMDB returned no row. Evidence is preserved in `logs/rdf-parser-lmdb/cache-red/` and appended to `initial-evidence.txt`.

The matching plain MemoryStore/NativeStore/LMDB selection now passes 3/3 cases with no errors. The first common-runtime run exposed QueryRoot's prohibition on clearing its parent through `setParentNode(null)`; the rewriter now reuses existing roots and only detaches non-root children. Evidence is retained at `logs/rdf-parser-lmdb/all-sails-queryroot-fix-green/`.

`RdfDocumentParserTest#expiredQueryDeadlineAbortsParsing` deterministically expired a public query deadline before parsing. The one-case red reported that no `QueryInterruptedException` was thrown. The parser budget now captures the active deadline and checks it alongside its own elapsed-time and interrupt limits. The matching one-case selection passed; red and green are retained beneath `deadline-red/` and `deadline-green/pass-attempt/` in the follow-up log directory.

Independent review found that a SERVICE URL supplied through VALUES or a stored statement is unknown before optimization, so a syntax-only parser detector cannot exclude it from the result cache. `RdfParserCacheVolatilityTest` reproduced four failures: two dynamic parser SERVICE variants, BNODE and UUID all reused supposedly fresh values. LMDB now uses the existing query-safety repeatability analysis with the store's query execution policy, as well as parser syntax detection, before cache lookup. Evidence is in `logs/rdf-parser-lmdb/cache-volatility-red/`; the matching four-case green is retained in `cache-volatility-green/` and the full compliance suite passed.

FedX's fresh 13-case suite reproduced three failures and five errors before its production edits: local parser work could become a whole-query SingleSourceQuery, service-body control joins could be rewritten before local receipt, and a member named after the reserved endpoint could replace local execution. The two-file repair normalizes before source selection, guards both whole-query pushdown paths, and leaves the reserved local SERVICE intact before service specialization. Evidence is retained at `logs/rdf-parser-lmdb/fedx-red/`.

Fresh `RdfParserIntegrationTest#tupleParsingIsReadOnly` evidence confirms four cases ran with one failure: direct LMDB returned zero rows instead of the two parsed triples. The three existing wrapped backends passed. The initial reports and full verify log are preserved under `logs/rdf-parser-lmdb/initial-red/` and appended to root `initial-evidence.txt` before production edits.

The supplied archive targets `3d292763558604affd89c7d4256e8df5720bd895`, while this integration starts from main `74a966e1612272f920458806fbd9f32e57ee2c1b`. Its historical feature commits are absent locally. Therefore complete tracked files must not be copied over current sources. The archive's 80 complete-file checksums and 43 delivered source/support checksums were verified by the parent before this plan began. Historical test totals in the archive are not fresh local evidence.

The historical benchmark helper requires an absent feature commit. It must be adapted into a reproducible current implementation harness with an optional explicitly supplied baseline, and the archived workflow must be adapted to the current branch and Maven policy.

Fresh baseline execution of `NativeTripleTermMatchingTest#repeatedComponentVariablesFilterBeforeBinding` ran four parameterized cases; all four failed with `java.lang.AssertionError: variable already bound: x` at the shared evaluator's row conversion. This is a runtime reproduction in the unchanged production checkout. Evidence is retained at `logs/rdf-parser-integration/initial-red/` and appended to the pre-existing root `initial-evidence.txt`.

The identical evaluator selection passed after archival production transfer: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` (0.186 seconds). Fresh reports and full log are retained in `logs/rdf-parser-integration/evaluator-green/`.

The initial independent-review regression class ran 45 cases with zero assertion failures and three errors. All three backend variants of `serviceRetainsExpressionOnlyCorrelatedInputs` failed with `RDF service requires one configuration node with parse:content and parse:format`. Reports are retained in `logs/rdf-parser-integration/review-red/`. The candidate nested OPTIONAL scope regression passed on all three backends, so no speculative scope correction was applied. Added direct tuple-result equality tests and a backend repeated-prebound-output regression before any generic evaluator repair.

`TupleFunctionResultBindingTest` reproduced four failures among eight cases: generic tuple evaluation checked only incoming bindings, then added duplicate result names without checking earlier columns. The minimal general correction reads constraints from the row being built. The identical eight-case selection subsequently passed; evidence is in `tuple-results-red/` and `tuple-results-green/` under the integration log directory.

SERVICE runtime diagnostics showed its entire body had become `BindingSetAssignment ([]) (new scope)` before invocation. A direct generic test, `FilterOptimizerTest#serviceFiltersRemainOwnedByReceivingEvaluator`, then proved that FilterOptimizer replaced the receiving body's original Filter and StatementPattern with that empty assignment. The test covers an ordinary delegated endpoint plus SILENT. The optimizer's scope walk and three filter passes now stop at SERVICE boundaries so the receiving evaluator interprets its own inputs. Temporary diagnostics were removed. Matching green and expanded backend controls are pending.

The matching generic SERVICE ownership selection passed (one test). The expanded 54-case module review then passed every prior regression and exposed only the three backend variants of a SERVICE expression input fed by a later tuple producer: expected one row, got zero. Free body inputs now join configuration inputs for dependency scheduling, while projected/private subquery names remain isolated. The added false-cycle control makes a SERVICE produce the document consumed by a later tuple call whose output name matches a private subquery variable. Focused verification and broad suites are running.

The full new Sail module now passes 228 tests with zero failures, errors, or skips (16.968 seconds). This includes all three backing stores and the added producer/private-scope controls. Fresh report trees and logs are retained in `logs/rdf-parser-integration/module-green/`. The five Rio suites and full shared-evaluator suite remain in progress under the single build worker.

All five affected Rio suites have passed: API 139 tests/8 skips, Turtle 339/22, TriG 360/4, N-Triples 223/2, and N-Quads 230/2. Total is 1,291 tests with 38 existing skips and no failures or errors. Reports are retained per module in `logs/rdf-parser-integration/rio-*-green/`. The full evaluator suite is running. Final review identified additional high-value scoped dependency controls (enclosing SELECT, graph-local NOT EXISTS/MINUS variables, nullable OPTIONAL/UNION/UNDEF outputs); tests were added without changing production, and will execute after the independent evaluator suite. The earlier 228-case module green precedes these expanded assertions.

The complete shared evaluator suite passed 2,198 tests, zero failures/errors, and one existing skip (26.353 seconds), retained in `logs/rdf-parser-integration/evaluation-green/`. No shared code changed afterwards. The final 12-case backend dependency selection then reproduced six failures and six errors: enclosing SELECT and nullable OPTIONAL inputs returned zero rows; graph-local NOT EXISTS/MINUS names created false cycles; nested property calls inside SERVICE received an unbound document. Reports are retained in `logs/rdf-parser-integration/scoped-input-red/`.

`ServiceRequest.bindingRequirements` now analyzes one private, configuration-stripped body after rewriting nested property calls. Shared `QueryAlgebraBindingAnalysis` supplies each scalar read's guaranteed local bindings and semantic output facts. Graph-pattern declarations are not scalar reads. Private projection visibility uses shared `TupleExprs.projectsSourceName`, and traversal stays inside the owned body. Nullable output names retain their producer identity even when an expression also reads them. Genuine required-input cycle rejection remains in the existing dependency scheduler. Matching focused and final module verification are running.

The final matching 12-case dependency selection passed. The complete review class passed 66/66, and the final parser module passed 237/237 with no failures, errors, or skips (12.148 seconds), retained at `logs/rdf-parser-integration/module-final-green/`. The distinct selected final scope is 3,726 tests: 237 new-module cases, 1,291 Rio cases, and 2,198 evaluator cases. There are zero failures/errors and 39 existing skips; focused subset reruns are not added to that count. Header/format/build/helper/JMH smoke verification remains in progress.

Final operational checks passed: repository copyright/SPDX scan, both shell helpers' `bash -n`, workflow YAML parsing with Ruby Psych, root `process-resources` formatter, clean diff checks, and post-format root quick install. The post-format review class passed 66/66 (5.981 seconds); reports/logs are in `logs/rdf-parser-integration/review-post-format/`. The current-only JMH helper completed 14 parameterized rows in about 128 seconds, without a baseline comparison. Raw JSON/log/classpath artifacts are copied to `logs/rdf-parser-integration/jmh-current-smoke/`; the original run remains in `core/sail/rdf-parser/target/benchmarks/run.hqbarK/`. No fresh speedup claim follows from this harness smoke.

## Decision Log

- Decision: Supersede the proposed LMDB-only support extraction before production implementation; design standard parser support in common RDF4J query execution and evaluation instead. Do not add a parser-specific LMDB dependency or constructor/configuration API.
  Rationale: The user explicitly requires all Sails without a new Sail or opt-in. The initial direct LMDB failure remains useful red evidence, but backend-specific integration does not satisfy the expanded goal. Runtime code belongs in the common evaluator; backend integration tests may move to a downstream compliance module to avoid cycles.
  Date/Author: 2026-10-23 / Codex.

Decision: Reconstruct archival commits in a temporary checkout at the declared baseline, then cherry-pick into the authorized branch. Transfer the evaluator regression first and capture an automated failure before production changes. Rationale: this preserves main's intervening work and obeys test-first requirements. Date/Author: 2026-10-23, Codex.

Decision: The existing `build_verify` worker owns every Maven invocation, formatting, and copyright checks, serially. Rationale: Maven install and test outputs share state, so parallel builds would produce unreliable evidence. Date/Author: 2026-10-23, Codex.

Decision: Register the new artifact in `bom/pom.xml` but keep runtime/storage aggregators unchanged. Rationale: applications opting into the module should use the same BOM version management as other Sail artifacts. Date/Author: 2026-10-23, Codex.

Decision: Default benchmarking to the current module; make `BASELINE_COMMIT` optional and explicit. Run specialized verification on affected PR paths, with optional manual JMH execution. Rationale: absent historical feature commits must not break ordinary builds or imply fresh performance comparisons. Date/Author: 2026-10-23, Codex.

Decision: Preserve expression-only SERVICE names in synthetic LATERAL imports, separately from mandatory document configuration dependencies. Rationale: FILTER, BIND, EXISTS, and NOT EXISTS may consume an incoming binding without adding its name to the SERVICE's tuple outputs; treating all service names as required inputs would create false cycles between independent calls sharing output names. This correction follows the retained three-backend failure and is undergoing verification. Date/Author: 2026-10-23, Codex.

Decision: Repair tuple equality in `DefaultEvaluationStrategy` and receiving SERVICE ownership in `FilterOptimizer`, rather than masking the defects in the new Sail. Rationale: both shared defects have smallest-scope automated reproductions and apply to other tuple functions/delegated endpoints. The broadened shared-evaluator scope requires the full `core/queryalgebra/evaluation` suite before final review. Date/Author: 2026-10-23, Codex.

Decision: Replace whole-body name subtraction with the shared scoped binding analysis after 12 failing backend reproductions. Rationale: possible outputs do not prove that a value exists at a scalar read, and graph-local declarations do not require outer values. A private rewritten body exposes nested parser argument roles while avoiding mutation of the actual SERVICE body. Keep configuration errors inside the invocation and retain the existing required-cycle rejection. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

The standard RDF4J follow-up is implemented and locally verified. Plain MemoryStore, NativeStore, LMDB, generic/custom factories and ordinary stacks use parser property functions and the reserved local SERVICE without a parser Sail or runtime opt-in artifact. Runtime code is in the existing evaluator; downstream `compliance/rdf-parser` preserves all prior behavior assertions and expands the backend matrix. Independent review found no unresolved implementation blocker. The existing PR 6262 and issue 6259 must be updated after the parent's staged-scope review; no commit or push of this follow-up has occurred yet.

Fresh distinct verification totals are 4,314 tests, zero failures or errors, and 158 skips: parser compliance 430/0 skips, evaluator 2,198/1 skip, Sail base 611/0, repository Sail 54/0, selected LMDB units plus its Failsafe integration suite 407/105, and full federation 614/52. The 17 new FedX invocations are included in the 614 and must not be added again. LMDB's complete unit module was not selected; its six selected unit classes contributed 131 cases, and its Failsafe suite contributed 276. `LmdbOptimisticIsolationTest` discovered zero JUnit cases and is not independent isolation evidence. Full report trees, exact commands and logs are under `logs/rdf-parser-lmdb/`, indexed in root `initial-evidence.txt`.

Final header/SPDX/immediate Codex signature checks passed for all 35 changed existing Java sources. The root formatter, post-format root quick clean install, shell syntax, workflow YAML parse and diff whitespace checks passed. Actual offline dependency trees show evaluator compile dependencies on all four supported Rio formats and no obsolete parser Sail artifact. The relocated current-only benchmark helper exited zero and produced 14 summary rows; its original artifacts remain in `compliance/rdf-parser/target/benchmarks/run.iFHR4N`, with a preserved copy under `logs/rdf-parser-lmdb/jmh-current-helper-smoke/`. This is helper-migration validation without a baseline or speedup claim.

The pre-stage follow-up manifest is `logs/rdf-parser-lmdb/scoped-manifest.json`; it includes moved/untracked sources and tracked deletions. Preserve the two unrelated pre-existing untracked files `.agent/query-renderer-traversal-execplan.md` and `maven-build.before-root-clean-install.log`. Stage only explicit manifest paths, inspect rename detection and staged whitespace, then leave publication to the parent's gate. Source and local verification have no remaining required work before that gate.

The original archival integration preceded this follow-up and was published at `06f4570d3b13508e417f548fae553105a75a4493` in draft PR 6262. Its earlier 49-path scope and 3,726-case verification remain historical evidence under `logs/rdf-parser-integration/`; they are not the current follow-up scope. Do not create another issue or pull request, amend or force-push. After approval, commit the reviewed follow-up, push normally, update the existing PR/issue wording to built-in RDF4J parsing, and verify local/remote/PR head equality. Do not claim those publication actions before tool confirmation.

## Context and Orientation

The repository is a Java 25+ multi-module Maven project. Sail is RDF4J's storage abstraction; query algebra is the tree of operations representing a query. Standard parser runtime now lives in `core/queryalgebra/evaluation`, package `org.eclipse.rdf4j.query.algebra.evaluation.rdfparser`. `RdfParserQuerySupport` normalizes property calls with `PropertyFunctionRewriter`; `RdfDocumentParser` validates bounded inline input with Rio; `RdfParseService` evaluates a private dataset through `ParsedDatasetSource`. Backend tests and documentation live in `compliance/rdf-parser`. Shared recursive limits and strict serialization syntax live in `core/rio/api`, `core/rio/turtle`, and `core/rio/ntriples`. Shared triple-term binding correction lives in `core/queryalgebra/evaluation/.../NativeTripleTermQueryEvaluationStep.java`.

The extracted archive is `/tmp/rdf4j-parser-reviewed-cuv_rf9n`. Its `APPLY.md`, `VERIFICATION.md`, complete patch, and module README explain the original delivery. GitHub issue is #6259 at https://github.com/eclipse-rdf4j/rdf4j/issues/6259. The authorized integration branch is `GH-6259-rdf-parser-query-extensions`. Preserve every pre-existing untracked file, including `.agent/query-renderer-traversal-execplan.md` and build logs. Never broadly stage, clean, reset, restore, or stash.

## Plan of Work

First add `NativeTripleTermMatchingTest` through a tests-only temporary commit and run its repeated-variable regression against unchanged production. Persist the initial report and full logs under `logs/rdf-parser-integration`, and at root `initial-evidence.txt`. After the failure is observed, reconstruct the complete delivery at its declared baseline in a temporary checkout, commit it there, and cherry-pick without committing into the integration workspace. Resolve conflicts by comparing both histories, retaining current main semantics as well as the new behavior.

Review every production path for binding scope, uncertain OPTIONAL/UNION outputs, list rewriting, parser safety, query/result ownership, and SERVICE isolation. Add focused regression tests before fixing confirmed defects. Adapt documentation, examples, verification helper, benchmark helper, and workflow to current commands and available history. Add required Codex signatures and explicit imports to touched Java sources without changing existing copyright years.

Once sources are stable, ask `build_verify` to run focused evaluator and parser tests, all new module tests, affected Rio module suites, formatting, header checks, and final build/verification. Retain XML and text reports so repeated runs cannot erase initial evidence. Hand the complete reviewed scope to the parent for independent review before the final workspace commit, push, and PR.

## Concrete Steps

Run commands from `/Users/havardottestad/Documents/Programming/rdf4j` unless stated otherwise. Git transfer uses a temporary detached checkout under `/tmp` at archive baseline; `git apply --check` must succeed there before applying. Commit only explicitly listed task files. Create the authorized branch once, then use `git cherry-pick --no-commit <temporary-commit>` to transfer each archival step without publishing temporary history.

The build worker runs tests with the preferred runner, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py NativeTripleTermMatchingTest#repeatedComponentVariablesFilterBeforeBinding --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py compliance/rdf-parser --retain-logs
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/rio/api,core/rio/turtle,core/rio/trig,core/rio/ntriples,core/rio/nquads verify
    python3 .codex/skills/mvnf/scripts/mvnf.py NativeTripleTermMatchingTest --retain-logs -- -Dtest=NativeTripleTermMatchingTest,EvaluationStrategyWithTripleTermsTest

All Maven commands use `.m2_repo`, offline first; test runs must never contain `-am` or `-q`. Root quick install may use `-am` only with tests skipped. Retain full install output in `maven-build.log` and test logs separately. Before final formatting run `scripts/checkCopyrightPresent.sh`, then the root formatter `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`. Formatting is serialized with all builds.

## Validation and Acceptance

The evaluator regression must fail before the shared fix and pass afterwards, including bound whole terms, incompatible types, repeated components, and term/component alias constraints. The new module must pass parsing, safety, property rewrite, dependency scheduling, optimization, private dataset, and backend integration tests on MemoryStore, NativeStore, and LMDB. All directly affected Rio suites must pass with configured nesting limits, malformed syntax, recovery defaults, and ordinary parser compatibility. Run exact final selections again after fixes or formatting that affect compiled sources. Record exact totals and skips from report XML rather than historical archive counts.

Inspect a runnable API example or existing integration scenario demonstrating parsing into bindings, private SERVICE graph matching, and insertion through the user's normal transaction. Verify helpers with shell syntax checks and workflow YAML parsing. Do not claim a new speedup without a measured before/after path; historical performance material must be labeled accordingly.

## Idempotence and Recovery

Read Git status before every grouped edit. Temporary staging checkouts may be retained as evidence. If cherry-pick conflicts occur, inspect ancestor/current/archive fragments and manually merge only the conflicting scope. Preserve untracked artifacts and logs. Offline missing dependency errors may be retried once online, then return offline. Other build failures require diagnosis, not blind repetition. The parent performs publication only after independent review.

## Artifacts and Notes

Archive: `/tmp/rdf4j-parser-reviewed-cuv_rf9n`; temporary reconstruction checkout: `/tmp/rdf4j-parser-transfer-6259`; initial current-main build: `maven-build.log`; prior log backup: `maven-build.before-root-clean-install.log`; fresh integration logs: `logs/rdf-parser-integration`; first automated failure: `logs/rdf-parser-integration/initial-red/`, appended to pre-existing `initial-evidence.txt`. Full source scope initially comprises 43 archive files, including 30 Java files, plus this integration plan, BOM registration, and justified regressions/adaptations.

## Interfaces and Dependencies

Use existing RDF4J first-party Sail, query algebra, model, Rio, repository, and backend artifacts. The archive uses the existing JMH version for test benchmarks. No new external dependency is authorized. Standard evaluator support uses finite `RdfParseOptions.DEFAULT`; tuple functions use the `http://rdf4j.org/function/parse#` vocabulary and private SERVICE endpoint `urn:rdf4j:parse`. Formats remain explicitly limited to Turtle, TriG, N-Triples, and N-Quads. The shared `BasicParserSettings.MAX_NESTING_DEPTH` default remains effectively unlimited for compatibility, while parser query functions explicitly supply finite per-document limits.

Revision note (2026-10-23): Created integration plan after archive inspection and successful initial root clean install, before any production transfer.

Revision note (2026-10-23): Recorded fresh four-case evaluator failure, advanced integration milestone, and corrected runner examples after confirming mvnf infers only one source target and does not accept comma-separated module directories.

Revision note (2026-10-23): All 43 archival files transferred without conflicts through temporary commits `2f27ed09c2` and `6051308d58`; added public artifact to BOM, scoped signatures/import hygiene, and two candidate regressions for independent review findings before any corrective production edits.

Revision note (2026-10-23): Corrected orchestration after the initial build worker completed and a queued message did not restart it. Retired that worker after the user-specified 30-minute idle boundary, spawned fresh Luna Max `verify_parser`, and confirmed actual execution plus same-selection evaluator green. The new worker remains running with event-driven waits across source handoffs.

Revision note (2026-10-23): Added general tuple-row equality and receiving SERVICE ownership repairs with retained direct red/green evidence; expanded expression correlation, later-producer scheduling, and subquery isolation tests. Specialized CI/helper now include the new generic evaluator regressions as well as native triple-term cases.

Revision note (2026-10-23): Reopened scoped dependency review after parent/independent analysis found whole-body output subtraction cannot distinguish scalar reads from graph-local declarations. Added focused reproduction controls first, while unchanged shared code completes full evaluator verification. Any correction must reuse shared binding/scope facts and retain rejection of genuine required-input cycles.

Revision note (2026-10-23): Completed matching 12-case green, final 237-case module green, post-format 66-case green, full shared/Rio verification, header/format/syntax/diff checks, and bounded current-only JMH smoke. Advanced to the independent review/publication gate without committing or pushing the integration workspace.

Revision note (2026-10-23): Parent final review accepted the exact 49-file scope and authorized scoped commit, push, and draft PR creation. Recorded all implementation/review/validation work complete and publication pending before final staging, preserving a truthful pre-publication snapshot.

## Standard RDF4J follow-up: context and implementation

The original wrapper implementation was published in draft PR 6262 at commit `06f4570d3b13508e417f548fae553105a75a4493`. The user requires parser functions and the local parsed-data SERVICE to be part of RDF4J and work on any Sail without requiring another Sail or configuration opt-in. Plain MemoryStore, NativeStore, LMDB and ordinary stacks must therefore use the standard runtime automatically. No LMDB-specific parser dependency or options API will be added.

Move the eight reusable runtime classes from `core/sail/rdf-parser/src/main/java/org/eclipse/rdf4j/sail/rdfparser` into `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/rdfparser` and update their package. These classes are `RdfDocumentParser`, `RdfParseOptions`, `RdfParseVocabulary`, `RdfParseTupleFunction`, `RdfParseService`, `ServiceRequest`, `PropertyFunctionRewriter`, and `ParsedDatasetSource`. Remove the unreleased `RdfParserSail`, its connection wrapper and its resolver decorator; no released API compatibility is required. Replace resolver decoration with standard evaluator interception of the reserved endpoint. Add the existing first-party Rio Turtle, TriG, N-Triples and N-Quads dependencies to the evaluator. The inspected Rio dependency graph is acyclic.

Create public internal helper `RdfParserQuerySupport` in the new package. Its immutable default functions have the four public vocabulary URIs and finite `RdfParseOptions.DEFAULT`; it holds no store, connection or result model. Register these standard builtins when `TupleFunctionRegistry` is constructed. A parser invocation still creates its own parser and blank-node allocation; each result owns its parsed dataset. Standard support therefore has no per-store registry registration, initialization or shutdown lifecycle. The helper exposes `rewrite(TupleExpr, BindingSet)` for borrowed algebra, `rewriteOwned(TupleExpr, BindingSet)` for private algebra, `isParserQuery(QueryModelNode)`, `functions()`, and `service()`. Ordinary queries return their exact input identity. Mark privately normalized roots with transient query-model metadata so multiple common entry points do not rewrite a parser query repeatedly; clones retain this marker.

At `DefaultEvaluationStrategy.optimize`, normalize parser algebra with actual API bindings before optimization, including when a custom optimizer pipeline replaces the standard one. `getService` resolves `urn:rdf4j:parse` locally and delegates every other endpoint unchanged; SERVICE compilation must use that method rather than the raw delegate field. Tuple functions remain visible through the standard registry to both generic compilation and LMDB's native tuple compiler.

At `SailSourceConnection.evaluateInternal`, normalize after its private clone and before strategy creation/optimization; this supports raw Sail connections. In `SailRepositoryConnection`, provide a common static normalization helper used by tuple, graph and boolean query execution, update WHERE evaluation, and explanation. This preserves a custom Sail delegate's evaluator while supplying standard extension algebra. The contract is that independent custom evaluators execute supported RDF4J algebra; an engine that entirely replaces the algebra semantics cannot be forced to implement a new operator. Do not silently substitute generic storage scans or invent a capability framework.

LMDB's `LmdbStoreConnection.evaluateInternal` performs result-cache and replay setup before calling the base connection. Exclude parser queries from its optional result cache before lookup, and normalize its existing private clone before replay/native planning. This is an ordering correction in an actual overriding path, not backend registration. Keep the notifying connection type, snapshots, transactions, replay, native/generic/custom strategy selection, and explanation capture. Preserve fresh parsed blank nodes across repeated cached-hint evaluations and raw Sail calls.

For cache classification, also use `QueryEvaluationUtility.querySafetySnapshot(tupleExpr, store.getQueryExecutionPolicy()).isRepeatable(tupleExpr)`: the shared analysis rejects SERVICE even when its endpoint is only bound later, and rejects inherently fresh functions such as BNODE and UUID. Do not try to infer a dynamic endpoint's value with a special parser heuristic.

FedX overrides optimization rather than calling the default evaluator. Normalize its private clone before `GenericInfoOptimizer` and source selection, prevent whole-query single-endpoint pushdown when local parser work exists, and keep the reserved SERVICE local in `ServiceOptimizer`. FedX otherwise retains its ordinary endpoint selection, tuple compilation, resolver and vectored SERVICE execution. The independent Sol reviewer owns those files and first writes a focused red regression; all Maven execution remains serialized through the Luna worker.

Move the existing parser module tests, docs, examples, and supported helpers into downstream `compliance/rdf-parser`, change test packages to the evaluator runtime package, remove the obsolete Sail module entry and public BOM artifact, and add the compliance module. Preserve every parser test and assertion, including all three backing stores, malformed-input limits and nested scope/dependency regressions. Direct low-level tests may share the runtime package across the test classpath. The compliance artifact is a test host, not an opt-in runtime dependency. Update CI and helpers to run its relocated suites with standard artifacts.

## Standard RDF4J follow-up: milestones and acceptance

The plain LMDB omission first reproduced as four cases with one failure. The expanded plain-store selection now reproduces as three cases with three failures, one each for MemoryStore, NativeStore and LMDB, all returning zero rows instead of two. Both initial reports and the cache identity failure are retained under `logs/rdf-parser-lmdb/` and appended to root `initial-evidence.txt` before production edits.

After migration, run the same focused selection on plain backends and require all cases green. Expand the backend matrix to LMDB default/native/generic/custom factories and transparent/transforming stacks. Add raw Sail evaluation, SELECT/ASK/CONSTRUCT/UPDATE/explain, application resolvers, repeated prepared queries, blank-node freshness, rollback and repository-factory creation coverage. Keep parser bounds finite and verify independent invocations do not share state. Run the entire relocated parser suite, relevant existing evaluator/backend lifecycle/strategy/cache/explanation suites, and FedX single/multiple-member controls.

All Maven work is serialized through the Luna Max verification worker, using `mvnf --retain-logs`, `.m2_repo`, offline first, and no `-am` or `-q` with enabled tests. The initial root clean install already completed in this conversation. Preserve fresh reports and logs beneath `logs/rdf-parser-lmdb/`. Run source-header checks, the repository formatter, and final focused verification. Parent review gates publication; no broad staging, cleanup, reset, branch changes, or deletion of unrelated artifacts is permitted.

Revision note (2026-10-23): Superseded the unimplemented LMDB-only design following the user's all-Sail requirement, recorded fresh three-backend red evidence, and adopted standard evaluator builtins, shared execution normalization, downstream compliance tests, actual LMDB cache/replay ordering and FedX integration. The parent approved removal of the unmerged wrapper and runtime module; no production changes preceded the new red evidence.

Revision note (2026-10-23): Completed common runtime migration, standard builtins, root-preserving/idempotent normalization and all execution hooks. Captured matching three-store green, deadline red/green, FedX pre-fix red and dynamic SERVICE/BNODE/UUID cache red before the corresponding repairs. Expanded existing backend suites to default/generic/custom LMDB and a transparent stack, and added raw API, current bindings, ASK, custom delegate/pipeline, resolver, inference-stack, factory/reopen and explanation coverage. Verification is the single active milestone.

Revision note (2026-10-24): Completed all follow-up verification and independent review with 4,314 distinct tests and 158 skips, final formatter/header/build/static checks, actual transitive-dependency proof and the relocated 14-row current-only JMH helper smoke. Replaced stale original-publication instructions with the existing PR/issue follow-up gate. Only staged-scope review and authorized publication remain active.
