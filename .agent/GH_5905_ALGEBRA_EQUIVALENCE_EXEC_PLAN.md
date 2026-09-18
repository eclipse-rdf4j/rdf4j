# Integrate the GH-5905 algebra equivalence checker

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept current while implementation proceeds. Maintain this document according to `.agent/PLANS.md`.

## Purpose / Big Picture

After this change, RDF4J contributors can compare two native query-algebra trees and ask whether they are equivalent for a selected semantic profile before applying an optimizer rewrite. The checker is conservative: it reports equivalence only when a replayable proof succeeds, reports non-equivalence only when exact evaluation produces a counterexample, and otherwise reports an unknown result. It is an experimental core module and is not added to RDF4J's standard optimizer pipeline.

The result is visible by running the new module tests. They exercise RDF4J runtime semantics, SPARQL 1.1 semantics, draft SPARQL 1.2 semantics, collision-safe fingerprints, proof verification, immutable evidence, bounded counterexample generation, and a safe rewrite gate.

## Progress

- [x] (2026-07-12 13:50Z) Created `GH-5905-algebra-equivalence` from develop commit `8d5e6369ac`.
- [x] (2026-07-12 13:50Z) Recorded scope, design constraints, tests, and validation in this ExecPlan.
- [x] (2026-07-12 13:53Z) Ran the mandatory root offline clean install; all reactor modules succeeded in 27.315 seconds and the full output is in `maven-build.log`.
- [x] (2026-07-12 14:25Z) Imported and repackaged 46 archive implementation sources plus its baseline test class; the 22 imported tests pass.
- [x] (2026-07-12 14:25Z) Registered `rdf4j-queryalgebra-equivalence` in the query-algebra reactor and RDF4J BOM using RDF4J-only dependencies.
- [x] (2026-07-12 17:44Z) Added focused failing regressions before each grouped behavior repair and retained the initial 16-failure report in `GH-5905-failing-evidence.txt`.
- [x] (2026-07-12 17:44Z) Repaired runtime joins, sequence barriers, exact evaluation, canonical encodings, profile validation, snapshots, proof traces, and rewrite cloning.
- [x] (2026-07-12 17:44Z) Added the explicit native-algebra coverage audit and deterministic exact-evaluation differential suite.
- [x] (2026-07-12 18:20Z) Passed repository copyright validation and formatting, then reran the module: 56 tests, zero failures, zero errors, zero skipped; retained the compact result in `GH-5905-final-evidence.txt`.
- [x] (2026-07-12 18:20Z) Passed the final offline root quick clean install in 27.158 seconds and audited the final scope while preserving every unrelated untracked artifact.

## Surprises & Discoveries

- Observation: The supplied archive is a standalone Maven project and bundles distribution files and JARs that must not become RDF4J source dependencies.
  Evidence: Its source tree contains the checker implementation under `dev.sparql.algebra.equivalence`, plus a standalone `pom.xml`, `lib`, `dist`, and scripts. Only source concepts and tests are appropriate for this repository.

- Observation: RDF4J runtime `Join` is not unconditionally commutative because the right operand is evaluated with bindings emitted by the left operand.
  Evidence: `JoinIterator` passes each left-side binding set into evaluation of the right argument. By contrast, specification-profile normalization models the W3C algebra rule where both operands receive the same incoming mapping.

- Observation: RDF4J's normal `SailConnection.evaluate` path applies the configured optimizer pipeline, which can erase the algebra difference a counterexample is intended to observe.
  Evidence: Exact counterexample evaluation therefore needs a MemoryStore configured with an empty `QueryOptimizerPipeline` before initialization.

- Observation: Several archive canonical forms are diagnostic strings rather than injective encodings.
  Evidence: Comma-joined name sets, unescaped blank-node identifiers, and stringified VALUES rows can map distinct RDF structures to the same key.

- Observation: RDF4J represents an ordinary literal's absent base direction as `Literal.BaseDirection.NONE`, not `null`.
  Evidence: The first full module run rejected an ordinary SPARQL 1.1 literal as directional and generated an invalid-profile failure until all detectors compared against `NONE`.

- Observation: RDF4J runtime evaluation cannot always serve as specification-profile counterexample evidence.
  Evidence: A programmatic Join whose right Filter read a left binding produced a stable RDF4J witness even though W3C Join evaluates both operands under the same incoming mapping. Such Join/LeftJoin correlation witnesses are now conservatively declined for W3C targets.

## Decision Log

- Decision: Add a dedicated module at `core/queryalgebra/equivalence` with artifact ID `rdf4j-queryalgebra-equivalence`.
  Rationale: It needs both query-algebra evaluation and MemoryStore for optional exact counterexample search; placing it inside the evaluation module would introduce a dependency cycle.
  Date/Author: 2026-07-12 / Codex

- Decision: Make proof-only checking the default and bounded generated-model search opt-in.
  Rationale: A failed proof is not evidence of non-equivalence, and automatically creating hundreds of MemoryStores for every unproved optimizer candidate is unsuitable for a rewrite gate.
  Date/Author: 2026-07-12 / Codex

- Decision: Default the observation model to bags and incoming bindings to arbitrary bindings.
  Rationale: RDF4J query results preserve multiplicity, and optimizer rewrites must remain valid inside a larger plan where variables may already be bound.
  Date/Author: 2026-07-12 / Codex

- Decision: Name the implementation profile `RDF4J_RUNTIME` and retain separate `SPARQL_1_1`, `SPARQL_1_2_DRAFT`, and shared-profile targets.
  Rationale: Runtime behavior is not permanently tied to RDF4J 6 or to a final SPARQL 1.2 recommendation. SPARQL 1.2 is still a W3C Working Draft as of this implementation.
  Date/Author: 2026-07-12 / Codex

- Decision: Expose `FunctionSafetyPolicy` rather than a deterministic-function policy.
  Rationale: Safe normalization requires purity plus permission to relocate and duplicate evaluation, not merely a repeatable return value.
  Date/Author: 2026-07-12 / Codex

- Decision: Apply `AlgebraRewrite` only to a clone and snapshot every accepted before/after tree.
  Rationale: Caller mutation must not invalidate a previously verified proof or silently change the rewrite later returned by the gate.
  Date/Author: 2026-07-12 / Codex

- Decision: Treat `Literal.BaseDirection.NONE` as an ordinary RDF literal and only `LTR`/`RTL` as draft RDF 1.2 directional literals.
  Rationale: This matches RDF4J's model contract and prevents SPARQL 1.1 from rejecting all literals.
  Date/Author: 2026-07-12 / Codex

- Decision: Do not use RDF4J runtime Join/LeftJoin correlation as W3C counterexample evidence.
  Rationale: A runtime-only evaluation difference cannot soundly establish non-equivalence under specification semantics.
  Date/Author: 2026-07-12 / Codex

## Outcomes & Retrospective

The new experimental `rdf4j-queryalgebra-equivalence` module is registered in the query-algebra reactor and RDF4J BOM and uses only RDF4J dependencies. Its curated public API covers conservative equivalence checks, semantic summaries and collision-safe fingerprints, replayable proofs, immutable evidence, and clone-isolated rewrite verification. It is not installed in the standard optimizer pipeline.

The implementation addresses the reviewed defects: runtime-correlated joins are not commuted, specification joins retain their semantic rules, runtime Reduced is a sequence barrier, counterexamples bypass optimizers, canonical identity encodings are injective, RDF 1.2 values are profile checked throughout cases and generation, caller-owned state is snapshotted, proof traces are replayed exactly, ambiguous ordering is rejected, and multiset/deduplication paths use hash-based accounting.

The imported 22-test baseline grew to 56 tests across eight classes. The first grouped regression run produced 16 failures out of 18 tests and is retained in `GH-5905-failing-evidence.txt`; after the repairs, `python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/equivalence --retain-logs` reports 56 tests with zero failures or errors. Repository copyright validation and formatting passed, and the final offline root `-Pquick clean install` completed successfully for the entire reactor, including the new module.

The remaining limitation is deliberate conservatism: opaque or unsupported native algebra nodes, ambiguous canonical ordering, unproved candidates when bounded search is disabled, and runtime-only witnesses under specification targets return `UNKNOWN` rather than risking an unsound equivalence or non-equivalence claim. No performance claim is made without a benchmark; the implementation merely removes known quadratic multiset and deduplication algorithms.

## Context and Orientation

RDF4J represents parsed and optimized queries as mutable trees rooted at `TupleExpr`, with scalar expressions represented by `ValueExpr`. The existing model lives in `core/queryalgebra/model`; runtime evaluators and optimizers live in `core/queryalgebra/evaluation`; MemoryStore lives in `core/sail/memory`. The new checker belongs beside those modules at `core/queryalgebra/equivalence` because it consumes the public algebra model and can optionally execute exact finite cases without becoming part of the default optimizer pipeline.

An equivalence proof in this module means both input trees normalize to one collision-safe canonical representation using only rules authorized by the chosen semantic target. A counterexample means exact, non-optimized evaluation of the two supplied trees produces different outcomes for one immutable dataset, incoming binding set, and observation mode. `UNKNOWN` means neither proof nor counterexample was found and is the required conservative answer.

The query-algebra aggregator is `core/queryalgebra/pom.xml`. RDF4J's dependency-management catalog is `bom/pom.xml`. The new module POM inherits from the query-algebra aggregator and uses only existing RDF4J artifacts: common annotations, model, query APIs, query-algebra model and evaluation, the SPARQL parser, Sail APIs, and MemoryStore. No archive JAR, script, standalone POM, report, or distribution file is imported.

Public types live in `org.eclipse.rdf4j.query.algebra.equivalence` and the package is annotated `@Experimental`. Implementation helpers live in `org.eclipse.rdf4j.query.algebra.equivalence.internal` and are marked internal. Every new Java file uses the 2026 Eclipse RDF4J header followed immediately by `// Some portions generated by Codex`.

## Plan of Work

First run the repository's required root offline quick clean install. Preserve its complete log in `maven-build.log`; if offline dependency resolution alone fails, retry the same build once online and return to offline operation. Do not use `-am` or `-q` for any test run.

Extract the user-supplied archive only into `/tmp` for inspection. Add the Java sources and tests to `core/queryalgebra/equivalence`, changing the package root from `dev.sparql.algebra.equivalence` to `org.eclipse.rdf4j.query.algebra.equivalence`, adding repository headers, and conforming to RDF4J formatting. Add a repository-native module POM instead of the archive POM. Register the module in `core/queryalgebra/pom.xml` and its artifact in `bom/pom.xml`.

Port the archive tests, adjusting target names and making generated counterexample search explicit. Before grouped production repairs, add focused tests that expose the unsound or mutable behavior. The tests must distinguish runtime join behavior from specification-profile commutativity; cover contextual Filter, Extension, and Projection operands; establish runtime `REDUCED` as a sequence barrier; and prove that Slice/Reduced differences remain discoverable by bounded exact evaluation.

Replace every canonical identity string with an injective length-prefixed encoding. Encode binding names, RDF values including blank-node identifiers and RDF-star triple terms, Lateral input sets, and VALUES metadata without delimiter ambiguity. Retain separate readable rendering only for diagnostics. Replace quadratic multiset equality and deduplication with hash-counting and insertion-order-preserving hash sets.

Detect draft SPARQL 1.2 features in the whole semantic surface: algebra node types, constant values, variables with assigned values, VALUES rows, datasets, incoming bindings, and generated evaluation cases. SPARQL 1.1 and the shared SPARQL 1.1-compatible target reject triple terms and directional language literals. Draft SPARQL 1.2 accepts them. The bounded generator emits only values valid for its selected target.

Snapshot every caller-owned mutable input. `EvaluationCase` copies dataset graph selections, bindings, and statements. Equivalence evidence and counterexamples expose immutable snapshots. `VerifiedRewrite` stores clones and returns clones. `VerifiedRewriteEngine` accepts an `AlgebraRewrite`, invokes it on a clone of the original, verifies the candidate, and returns detached before/after trees only after proof succeeds.

Keep proof steps only from the normalization path that was actually selected. When two candidate canonical forms have equal ordering keys but are structurally different, decline the normalization instead of choosing an ambiguous order. `ProofKernel` must replay and reject missing, reordered, altered, or otherwise tampered proof steps.

Add a native algebra coverage audit in tests. It enumerates every concrete RDF4J `TupleExpr` and `ValueExpr` available to the module and requires an explicit classification as supported or conservatively opaque. Add a deterministic differential suite that takes every fixed runtime example reported `EQUIVALENT` and checks identical exact outcomes across a matrix containing default and named graphs, incoming and partial bindings, duplicates, and RDF 1.2 terms where valid.

Consolidate documentation into the module README and package documentation. Describe actual behavior only, state that SPARQL 1.2 is a W3C Working Draft, explain the conservative status meanings and opt-in search cost, and avoid archive coverage claims that cannot be reproduced.

## Concrete Steps

Run commands from `/Users/havardottestad/Documents/Programming/rdf4j-stf`.

The mandatory initial build is:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '<repository warning/error/summary filter>'

During test-first iteration, run the smallest method or class through:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs

For final module verification, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/equivalence --retain-logs

Validate new and edited headers before formatting:

    cd scripts
    ./checkCopyrightPresent.sh

Format repository sources from the root:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Finally repeat the mandatory root offline quick clean install and inspect `git status --short` plus the complete diff. The unrelated Kuzu and review artifacts that were untracked at the start must remain untouched and untracked.

## Validation and Acceptance

The branch must contain a reactor-registered and BOM-managed `rdf4j-queryalgebra-equivalence` module that compiles without external dependencies. Its focused and module-wide tests must pass on JDK 25.

Acceptance requires observable tests for each semantic guarantee. Runtime join normalization must refuse a commutation when one operand reads a binding produced by the other, while SPARQL specification profiles may prove the same abstract join commutative. Runtime Reduced must preserve sequence-sensitive distinctions. Counterexamples must evaluate the exact supplied trees with no optimizer. Adversarial delimiters in names and RDF values must produce distinct fingerprints. SPARQL 1.1 must reject RDF 1.2 values while draft SPARQL 1.2 accepts them. Mutation after constructing a case or accepting a rewrite must not alter stored evidence or returned plans. Tampered proofs must fail replay. The coverage audit must classify all native concrete algebra nodes. Every fixed runtime equivalence example must survive the differential evaluation matrix.

The final module command is expected to end with Maven `BUILD SUCCESS` and Surefire reports with zero failures and errors. The final root quick build is expected to report success for the complete reactor.

## Idempotence and Recovery

The build and test commands are safe to rerun. The archive is staged only under `/tmp`; deleting or recreating that scratch extraction does not affect the repository. Source imports are made as reviewable patches, not by overwriting tracked files. If a test exposes a design flaw, keep the smallest failing test, record the observation here, and fix the root cause before broadening verification.

Do not reset, clean, restore, stash, delete, or rename pre-existing artifacts. If an unexpected tracked change appears, inspect it and stop only if it overlaps this module. No commit, push, or pull request is part of this plan.

## Artifacts and Notes

The source archive is `/Users/havardottestad/Downloads/rdf4j6-sparql12-algebra-equivalence-1.0.0.zip`. It is an input artifact only. The implementation does not add it or any bundled binary to Git.

Initial branch state was develop commit `8d5e6369ac`. Existing unrelated untracked paths included `.agent/KUZU_CORRECTNESS_PERFORMANCE_EXEC_PLAN.md`, `.claude/`, `baseline-failing-tests.txt`, `core/sail/kuzu/`, Kuzu logs/reviews/archives, and an RDF4J history bundle. They are outside this plan.

## Interfaces and Dependencies

The public package must provide these curated experimental interfaces and immutable result types:

    AlgebraEquivalenceChecker
    CheckOptions
    EquivalenceResult
    EquivalenceStatus
    EquivalenceEvidence
    Counterexample
    ObservationMode
    SemanticsTarget
    SemanticSummary
    SemanticAnalyzer
    SemanticFingerprint
    SemanticFingerprinter
    SemanticFingerprintIndex
    ProofKernel
    AlgebraRewrite
    VerifiedRewriteEngine
    VerifiedRewrite
    FunctionSafetyPolicy

`SemanticsTarget` contains `RDF4J_RUNTIME`, `SPARQL_1_1`, `SPARQL_1_2_DRAFT`, and a shared-profile target restricted to rules valid in both specification profiles. `CheckOptions` defaults to bag observation, arbitrary incoming bindings, and generated bounded search disabled.

`FunctionSafetyPolicy` must answer whether a function call is pure and may safely be relocated or duplicated. Its name and documentation must not imply that deterministic return values alone are enough.

`AlgebraRewrite` is a functional interface accepting a mutable `TupleExpr` clone and returning the candidate `TupleExpr`. `VerifiedRewriteEngine` never gives a caller-owned tree to the rewrite, and it accepts a rewrite only when the checker returns a verified equivalence proof. `VerifiedRewrite` snapshots both plans and returns clones from its accessors.

The module depends only on RDF4J artifacts at `${project.version}`: `rdf4j-common-annotation`, `rdf4j-model`, `rdf4j-query`, `rdf4j-queryalgebra-model`, `rdf4j-queryalgebra-evaluation`, `rdf4j-queryparser-sparql`, `rdf4j-sail-api`, and `rdf4j-sail-memory`, plus test dependencies inherited from RDF4J's parent configuration.

Revision note (2026-07-12): Created the initial executable plan from the approved GH-5905 integration scope so implementation can resume from this file alone.

Revision note (2026-07-12 13:53Z): Recorded the successful pre-change reactor baseline before importing the archive.

Revision note (2026-07-12 14:25Z): Recorded the green imported baseline and completed module/reactor/BOM integration before defect-focused TDD.

Revision note (2026-07-12 17:44Z): Recorded completed defect repairs, expanded audits, and two additional soundness findings from the broad test pass.

Revision note (2026-07-12 18:20Z): Recorded persisted final module evidence, copyright/format validation, the successful final root reactor build, and the completed scope audit.
