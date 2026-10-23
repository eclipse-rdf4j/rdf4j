# Integrate RDF parsing into SPARQL queries

This ExecPlan is a living document maintained under `.agent/PLANS.md`. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current. This work uses Routine D for a complex feature, while reproducing shared behavior defects before transferring production changes.

## Purpose / Big Picture

Applications will be able to parse inline Turtle, TriG, N-Triples, and N-Quads documents inside SPARQL without modifying their underlying repository. An opt-in Sail wrapper exposes parsing as row-producing property functions and as a local SERVICE whose graph patterns query a private parsed dataset. SELECT, CONSTRUCT, and ordinary INSERT WHERE can consume the resulting bindings. Each document must parse completely within explicit limits before rows are returned, and independent invocations must retain separate blank-node identities.

## Progress

- [x] (2026-10-23) Verify archive and clean baseline context.
- [x] (2026-10-23) Complete initial root offline clean install.
- [x] (2026-10-23) Reproduce shared evaluator defects before integration.
- [x] (2026-10-23) Transfer feature through semantic cherry picks.
- [x] (2026-10-23) Validate final scoped dependency composition controls.
- [x] (2026-10-23) Format and verify final parser module.
- [x] (2026-10-23) Complete independent implementation and scope review.
- [ ] [in_progress] Publish approved patch and verify heads.

## Surprises & Discoveries

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

Decision: Reconstruct archival commits in a temporary checkout at the declared baseline, then cherry-pick into the authorized branch. Transfer the evaluator regression first and capture an automated failure before production changes. Rationale: this preserves main's intervening work and obeys test-first requirements. Date/Author: 2026-10-23, Codex.

Decision: The existing `build_verify` worker owns every Maven invocation, formatting, and copyright checks, serially. Rationale: Maven install and test outputs share state, so parallel builds would produce unreliable evidence. Date/Author: 2026-10-23, Codex.

Decision: Register the new artifact in `bom/pom.xml` but keep runtime/storage aggregators unchanged. Rationale: applications opting into the module should use the same BOM version management as other Sail artifacts. Date/Author: 2026-10-23, Codex.

Decision: Default benchmarking to the current module; make `BASELINE_COMMIT` optional and explicit. Run specialized verification on affected PR paths, with optional manual JMH execution. Rationale: absent historical feature commits must not break ordinary builds or imply fresh performance comparisons. Date/Author: 2026-10-23, Codex.

Decision: Preserve expression-only SERVICE names in synthetic LATERAL imports, separately from mandatory document configuration dependencies. Rationale: FILTER, BIND, EXISTS, and NOT EXISTS may consume an incoming binding without adding its name to the SERVICE's tuple outputs; treating all service names as required inputs would create false cycles between independent calls sharing output names. This correction follows the retained three-backend failure and is undergoing verification. Date/Author: 2026-10-23, Codex.

Decision: Repair tuple equality in `DefaultEvaluationStrategy` and receiving SERVICE ownership in `FilterOptimizer`, rather than masking the defects in the new Sail. Rationale: both shared defects have smallest-scope automated reproductions and apply to other tuple functions/delegated endpoints. The broadened shared-evaluator scope requires the full `core/queryalgebra/evaluation` suite before final review. Date/Author: 2026-10-23, Codex.

Decision: Replace whole-body name subtraction with the shared scoped binding analysis after 12 failing backend reproductions. Rationale: possible outputs do not prove that a value exists at a scalar read, and graph-local declarations do not require outer values. A private rewritten body exposes nested parser argument roles while avoiding mutation of the actual SERVICE body. Keep configuration errors inside the invocation and retain the existing required-cycle rejection. Date/Author: 2026-10-23, Codex.

## Outcomes & Retrospective

Initial root clean install passed on current main in 58.695 seconds. No tests were run by that build. Integration and fresh test evidence remain to be completed.

The complete archival patch integrated without conflicts, and the initial shared-evaluator regression now passes. Independent review identified candidate failures around nested query groups, expression-only SERVICE correlation, and repeated tuple output constants/API bindings. The first two are being executed before fixes; the third will receive direct evaluator and backend tests next.

All confirmed runtime defects have been corrected and the final selected 3,726-case correctness scope is green with 39 existing skips. The scope includes general tuple-result equality, SERVICE optimizer ownership, scalar correlation, nullable producers, enclosing/private subqueries, graph-local EXISTS/MINUS names, and nested property-call inputs across MemoryStore, NativeStore, and LMDB. The nested OPTIONAL scope candidate passed unchanged and did not receive a speculative production patch. Publication awaits final operational checks and the parent's independent review.

Implementation and local verification are complete. The reviewable scope is exactly 49 paths, listed in `logs/rdf-parser-integration/final-scope.txt`; only unrelated pre-existing `.agent/query-renderer-traversal-execplan.md` and `maven-build.before-root-clean-install.log` remain untracked. Those files are untouched. The workspace remains uncommitted for the parent's independent review and authorized publication. The index still contains the archival transfer snapshot plus intent-to-add entries; publication must explicitly stage the final 49-path workspace scope, not assume the cached patch is current. The build worker remains available through an event-driven wait for any review correction.

The parent independently reviewed and accepted the final shared tuple/filter fixes, scoped binding requirements, regressions, BOM, helpers, and workflow. No unresolved finding remains. Publication is authorized and pending: stage exactly the 49 manifest paths, audit the staged diff and unrelated artifacts, commit `GH-6259 Add inline RDF parsing to SPARQL queries`, push the issue branch without force, create a draft PR against `main` closing #6259, attach it to the task, and verify local/remote/PR head equality. This committed plan snapshot records publication in progress; the final task handoff will supply the actual commit, PR URL, and check snapshot after those operations succeed. Do not claim publication before tool confirmation, amend the commit, or change this plan after publication merely to record its URL.

## Context and Orientation

The repository is a Java 25+ multi-module Maven project. `core/sail/pom.xml` lists Sail modules. Sail is RDF4J's storage abstraction; a wrapper can transform query algebra, the tree of operations representing a query, before delegating to its underlying store. The new module is `core/sail/rdf-parser`, package `org.eclipse.rdf4j.sail.rdfparser`. `RdfParserSailConnection` rewrites property calls with `PropertyFunctionRewriter`; `RdfDocumentParser` validates bounded inline input with Rio; `RdfParseService` evaluates a private dataset through `ParsedDatasetSource`. Shared recursive limits and strict serialization syntax live in `core/rio/api`, `core/rio/turtle`, and `core/rio/ntriples`. Shared triple-term binding correction lives in `core/queryalgebra/evaluation/.../NativeTripleTermQueryEvaluationStep.java`.

The extracted archive is `/tmp/rdf4j-parser-reviewed-cuv_rf9n`. Its `APPLY.md`, `VERIFICATION.md`, complete patch, and module README explain the original delivery. GitHub issue is #6259 at https://github.com/eclipse-rdf4j/rdf4j/issues/6259. The authorized integration branch is `GH-6259-rdf-parser-query-extensions`. Preserve every pre-existing untracked file, including `.agent/query-renderer-traversal-execplan.md` and build logs. Never broadly stage, clean, reset, restore, or stash.

## Plan of Work

First add `NativeTripleTermMatchingTest` through a tests-only temporary commit and run its repeated-variable regression against unchanged production. Persist the initial report and full logs under `logs/rdf-parser-integration`, and at root `initial-evidence.txt`. After the failure is observed, reconstruct the complete delivery at its declared baseline in a temporary checkout, commit it there, and cherry-pick without committing into the integration workspace. Resolve conflicts by comparing both histories, retaining current main semantics as well as the new behavior.

Review every production path for binding scope, uncertain OPTIONAL/UNION outputs, list rewriting, parser safety, query/result ownership, and SERVICE isolation. Add focused regression tests before fixing confirmed defects. Adapt documentation, examples, verification helper, benchmark helper, and workflow to current commands and available history. Add required Codex signatures and explicit imports to touched Java sources without changing existing copyright years.

Once sources are stable, ask `build_verify` to run focused evaluator and parser tests, all new module tests, affected Rio module suites, formatting, header checks, and final build/verification. Retain XML and text reports so repeated runs cannot erase initial evidence. Hand the complete reviewed scope to the parent for independent review before the final workspace commit, push, and PR.

## Concrete Steps

Run commands from `/Users/havardottestad/Documents/Programming/rdf4j` unless stated otherwise. Git transfer uses a temporary detached checkout under `/tmp` at archive baseline; `git apply --check` must succeed there before applying. Commit only explicitly listed task files. Create the authorized branch once, then use `git cherry-pick --no-commit <temporary-commit>` to transfer each archival step without publishing temporary history.

The build worker runs tests with the preferred runner, for example:

    python3 .codex/skills/mvnf/scripts/mvnf.py NativeTripleTermMatchingTest#repeatedComponentVariablesFilterBeforeBinding --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/rdf-parser --retain-logs
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

Use existing RDF4J first-party Sail, query algebra, model, Rio, repository, and backend artifacts. The archive uses the existing JMH version for test benchmarks. No new external dependency is authorized. `RdfParserSail` wraps an existing Sail with `RdfParseOptions`; tuple functions use the `http://rdf4j.org/function/parse#` vocabulary and private SERVICE endpoint `urn:rdf4j:parse`. Formats remain explicitly limited to Turtle, TriG, N-Triples, and N-Quads. The shared `BasicParserSettings.MAX_NESTING_DEPTH` default remains effectively unlimited for compatibility, while the new Sail explicitly supplies finite per-document limits.

Revision note (2026-10-23): Created integration plan after archive inspection and successful initial root clean install, before any production transfer.

Revision note (2026-10-23): Recorded fresh four-case evaluator failure, advanced integration milestone, and corrected runner examples after confirming mvnf infers only one source target and does not accept comma-separated module directories.

Revision note (2026-10-23): All 43 archival files transferred without conflicts through temporary commits `2f27ed09c2` and `6051308d58`; added public artifact to BOM, scoped signatures/import hygiene, and two candidate regressions for independent review findings before any corrective production edits.

Revision note (2026-10-23): Corrected orchestration after the initial build worker completed and a queued message did not restart it. Retired that worker after the user-specified 30-minute idle boundary, spawned fresh Luna Max `verify_parser`, and confirmed actual execution plus same-selection evaluator green. The new worker remains running with event-driven waits across source handoffs.

Revision note (2026-10-23): Added general tuple-row equality and receiving SERVICE ownership repairs with retained direct red/green evidence; expanded expression correlation, later-producer scheduling, and subquery isolation tests. Specialized CI/helper now include the new generic evaluator regressions as well as native triple-term cases.

Revision note (2026-10-23): Reopened scoped dependency review after parent/independent analysis found whole-body output subtraction cannot distinguish scalar reads from graph-local declarations. Added focused reproduction controls first, while unchanged shared code completes full evaluator verification. Any correction must reuse shared binding/scope facts and retain rejection of genuine required-input cycles.

Revision note (2026-10-23): Completed matching 12-case green, final 237-case module green, post-format 66-case green, full shared/Rio verification, header/format/syntax/diff checks, and bounded current-only JMH smoke. Advanced to the independent review/publication gate without committing or pushing the integration workspace.

Revision note (2026-10-23): Parent final review accepted the exact 49-file scope and authorized scoped commit, push, and draft PR creation. Recorded all implementation/review/validation work complete and publication pending before final staging, preserving a truthful pre-publication snapshot.
