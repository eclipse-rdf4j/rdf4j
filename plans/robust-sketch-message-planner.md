# Robust Sketch Message Planner for LMDB

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

`PLANS.md` is checked in at `PLANS.md`. This document must be maintained in accordance with that file.

## Purpose / Big Picture

After this change, LMDB-backed query planning should choose better join orders for representable flat inner-join queries because the sketch estimator will use canonical message passing instead of the current pairwise heuristic. A user will be able to see the difference by running the existing join-order planner tests, the optimizer safety tests, and the LMDB theme accuracy test suite: pathological chain queries should pick the selective chain order while unsupported query shapes still fall back to the old safe logic.

The current implementation estimates joins mostly from single-pattern and pair-pattern Theta sketches. The new implementation keeps `SketchBasedJoinEstimator` as the public runtime shell, but moves the actual robust planning logic into a package-private sketch optimizer package that materializes unary and binary factor synopses and computes canonical messages keyed by `(subsetMask, rootVariable)`. The visible outcome is improved join ordering on supported shapes without changing the public optimizer interfaces.

## Progress

- [x] (2026-03-18 22:53 CET) Read `PLANS.md`, current estimator wiring, and the `join_optimizer/` prototype.
- [x] (2026-03-18 22:53 CET) Ran root `mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install` successfully and saved the trailing proof to `initial-evidence.txt`.
- [x] (2026-03-18 22:53 CET) Confirmed `core/sail/base/pom.xml` already brings in `org.apache.datasketches:datasketches-java:7.0.1`, and the local jar already contains the tuple `arrayofdoubles` APIs needed by the new algebra.
- [x] (2026-03-18 23:18 CET) Added the first focused failing tests for canonical-message planning, unsupported-shape fallback, persistence lifecycle, and LMDB pathological-chain ordering.
- [x] (2026-03-18 23:28 CET) Implemented `org.eclipse.rdf4j.sail.base.sketchoptimizer` with `StoreSynopsis`, `PatternSynopsisAdapter`, `MessagePassingJoinPlanner`, and `SketchFingerprint`.
- [x] (2026-03-18 23:33 CET) Rewired `SketchBasedJoinEstimator` and `SketchJoinOrderReorderer` to use the new planner for supported flat inner-join segments and safe fallback otherwise; `LmdbEvaluationStatistics` kept its existing adapter semantics.
- [x] (2026-03-18 23:35 CET) Replaced the old persistence format with a versioned synopsis snapshot and rebuild-on-old-format behavior.
- [x] (2026-03-18 23:49 CET) Validated unit tests, optimizer safety tests, LMDB end-to-end accuracy coverage, and outer-binding robust-cardinality support.

## Surprises & Discoveries

- Observation: the current persisted sketch state does not store the variable-conditioned neighborhood synopses required by the prototype `push` operator.
  Evidence: `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SketchBasedJoinEstimator.java` currently persists `REC_SINGLE_TRIPLE`, `REC_SINGLE_CPL`, `REC_PAIR_TRIPLE`, `REC_PAIR_COMP1`, and `REC_PAIR_COMP2`, and `bindingsSketch(...)` reads complement sketches rather than per-binding neighborhoods.

- Observation: the prototype optimizer is exact only for connected, acyclic, unary/binary query graphs where each extension shares exactly one variable with the already connected subset.
  Evidence: `join_optimizer/README.md` states that assumption directly, and `join_optimizer/JoinOrderOptimizer.java` throws when it cannot find a connected, uniquely attached extension.

- Observation: serial installs matter in this repository because concurrent root installs can corrupt `.m2_repo` snapshot jars and surface false `ZipException` failures in later targeted test runs.
  Evidence: a prior overlapping build produced `ZipException opening "rdf4j-repository-manager-6.0.0-SNAPSHOT.jar": zip END header not found`, while the serial rerun completed with `BUILD SUCCESS`.

- Observation: tuple-sketch serialization cannot rely on the library object stream in this runtime because it drags in incubator-foreign classes on reload.
  Evidence: the first persistence attempt failed with `NoClassDefFoundError: jdk/incubator/foreign/MemorySegment`; the working implementation writes and reads the tuple-sketch payload explicitly instead.

- Observation: outer bindings did not need a separate fallback path once factor scaling was applied before planning.
  Evidence: `estimateRobustCardinalitySupportsInitiallyBoundVars` initially failed with `expected: <true> but was: <false>`, then passed after scaling supported factors by inverse distinct-count for bound variables inside `PatternSynopsisAdapter`.

## Decision Log

- Decision: create a new internal package `org.eclipse.rdf4j.sail.base.sketchoptimizer` and keep all new synopsis and message types package-private.
  Rationale: the public SPI already exists in `JoinOrderPlanner` and `QueryJoinOptimizer`; the refactor should improve behavior without widening the external surface.
  Date/Author: 2026-03-18 / Codex

- Decision: use `join_optimizer/` only as an algorithm specification, not as production source.
  Rationale: the prototype has standalone model classes, package-less source files, and stricter query-shape assumptions than the runtime code. Porting concepts while adapting them to RDF4J avoids shipping duplicate top-level code.
  Date/Author: 2026-03-18 / Codex

- Decision: the new planner is primary only for flat inner-join segments that reduce to unary/binary factors after constants and outer bindings are applied; unsupported shapes must return `Optional.empty()` and preserve the old fallback.
  Rationale: the prototype’s guarantees hold only on representable shapes. Forcing it onto cycles, left joins, or multi-shared-variable attachments would produce weak or incorrect estimates.
  Date/Author: 2026-03-18 / Codex

- Decision: use the existing 64-bit `valueFingerprint(...)` domain as the stable key space for retained tuple-sketch hashes, never LMDB internal ids.
  Rationale: fingerprints already survive persistence boundaries and avoid coupling the new synopsis layer to LMDB implementation details.
  Date/Author: 2026-03-18 / Codex

- Decision: introduce a new versioned snapshot format and deliberately ignore old `join-estimator.rjes*` sidecars so the estimator rebuilds instead of trying to emulate the old pair-complement layout.
  Rationale: the stored data model changes from pairwise complement sketches to unary/binary factor synopses with neighborhood data. Compatibility shims would add risk without preserving the new planner’s algebra.
  Date/Author: 2026-03-18 / Codex

- Decision: support `initiallyBoundVars` in the robust path by scaling supported factors with inverse distinct-count estimates instead of forcing immediate fallback.
  Rationale: the planner’s factor graph stays representable after outer bindings are applied, and a scaled-factor model keeps the robust path active for selective correlated joins without introducing key-space rewrites.
  Date/Author: 2026-03-18 / Codex

## Outcomes & Retrospective

Functional milestone reached. The estimator now uses the message-passing planner as the primary path for supported flat inner-join segments, persists a versioned synopsis snapshot, preserves the old safe fallback for unsupported shapes, and keeps robust cardinality available when outer bindings are present. The biggest implementation risk turned out to be persistence portability rather than planner algebra: tuple sketches had to be serialized explicitly to avoid incubator-memory linkage at reload time. Final targeted verification is green across `core/sail/base`, `core/sail/lmdb`, and `QueryJoinOptimizerReorderSafetyTest`.

## Context and Orientation

The current public runtime entry point is `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SketchBasedJoinEstimator.java`. It owns estimator lifecycle, persistence, incremental rebuilds, cardinality helpers, and join-order planning hooks. `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SketchJoinOrderReorderer.java` currently runs a left-deep dynamic-programming search for small join lists and a greedy search for larger lists using `TuplePlanEstimate` and `JoinStepEstimate`. `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java` is the LMDB runtime adapter; it exposes `supportsJoinEstimation()` and delegates `planJoinOrder(...)` to the estimator. The public optimizer SPI stays in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/JoinOrderPlanner.java`, and `QueryJoinOptimizer` already knows how to fall back when a planner returns `Optional.empty()`.

The prototype lives in `join_optimizer/JoinEstimator.java` and `join_optimizer/JoinOrderOptimizer.java`. In prototype terms, a “degree sketch” is a tuple sketch keyed by bindings of one variable where the single double summary stores the multiplicity of rows for that binding. A “neighborhood sketch” is a tuple sketch for a binary pattern conditioned on one bound side: if pattern `p(x,y)` is fixed to `x=a`, the neighborhood sketch on `y` stores each reachable `y` binding with its multiplicity. A “message” is a tuple sketch on one variable that summarizes a connected subset of already chosen patterns. “Canonical” means that the message for one connected subset and one root variable must be the same no matter which valid left-deep order first reached that subset. The prototype achieves this by memoizing messages with `(subsetMask, rootVariable)`.

For this repository, a “supported flat inner-join segment” means a list of `TupleExpr` leaves under inner joins where each leaf can be reduced to one unary or binary factor after applying constants from `StatementPattern`, outer bindings from the caller, filter selectivity heuristics on `Filter(StatementPattern)`, and exact binding-domain restrictions from `BindingSetAssignment`. A leaf that still has arity greater than two, participates in a cycle, shares more than one variable with an already connected subset, or sits under unsupported wrappers must cause the adapter to reject the segment so the optimizer keeps the current safe path.

The key existing tests are `core/sail/base/src/test/java/org/eclipse/rdf4j/sail/base/SketchBasedJoinEstimatorJoinOrderPlannerTest.java`, `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizerReorderSafetyTest.java`, and the persistence coverage in `core/sail/base/src/test/java/org/eclipse/rdf4j/sail/base/SketchBasedJoinEstimatorPersistenceTest.java`. A later milestone must also update the LMDB theme accuracy integration test in `core/sail/lmdb/src/test/java/...` once the exact file is identified during implementation.

## Plan of Work

Start by adding the smallest focused tests that prove the gap between the current estimator path and the new requirements. The first test group should live in `SketchBasedJoinEstimatorJoinOrderPlannerTest` or a new adjacent test class in the same module. It must capture at least three behaviors: message invariance for the same connected subset and root variable, correct DP order on simple chain or star fixtures, and safe rejection of unsupported shapes such as cycles or multi-shared-variable extensions. These tests should fail before production changes, either because the new planner classes do not exist yet or because the current logic chooses a different order or does not expose the required behavior.

After the first red tests exist, add `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/sketchoptimizer/StoreSynopsis.java`. This package-private type is the internal synopsis model. It should wrap the tuple `arrayofdoubles` sketches and expose the exact data the message algebra needs: a degree sketch for each factor/root-variable pair, a neighborhood sketch lookup for each binary factor and bound original key, exact row counts for fully grounded factors, and a retained-hash resolver that maps sketch hash keys back to the original 64-bit fingerprints. Keep the storage model stable around fingerprints, not LMDB ids.

Then add `PatternSynopsisAdapter.java` in the same package. This adapter should inspect `TupleExpr` leaves and either produce a supported factor graph or reject the segment. It must understand raw `StatementPattern`, `Filter(StatementPattern)` using the existing filter-selectivity heuristics from `SketchBasedJoinEstimator`, and `BindingSetAssignment` as an exact unary factor. It must apply constants and outer bindings before deciding factor arity. It must reject `LeftJoin`, unsupported wrappers, cycles, disconnected components that can only be joined by cross products, and leaves whose attachment to the current connected subset uses more than one shared variable.

Next add `MessagePassingJoinPlanner.java`. This package-private class ports the prototype’s tuple-sketch algebra and planning flow into RDF4J. It should memoize canonical messages by `(subsetMask, rootVariable)`, implement `multiply`, `push`, `dot`, exact DP for supported segments up to a fixed mask width, and greedy planning for larger supported segments. It must return enough information for both join-order planning and cardinality estimation. When the caller requests dynamic programming but the supported segment exceeds the bitmask limit, the planner should return `Optional.empty()` rather than silently downgrading, so `QueryJoinOptimizer` keeps its current fallback behavior.

With the internal package in place, rework `SketchBasedJoinEstimator.java` into an adapter shell. The public methods `isReady()`, `staleness()`, `planJoinOrder(...)`, and `cardinality(...)` must remain. The estimator should delegate supported flat inner-join planning and cardinality requests to `MessagePassingJoinPlanner`, while unsupported shapes continue to use the existing heuristic logic. Keep lifecycle, rebuild scheduling, throttling, and transaction interaction in this class. Remove pair-specific debug and helper APIs that only make sense for the old persistence model.

Then rewrite `SketchJoinOrderReorderer.java` as a thin bridge over `MessagePassingJoinPlanner`. It should no longer own the DP or greedy enumeration itself. Its job becomes: build the supported segment through `PatternSynopsisAdapter`, invoke the message planner with the caller’s requested algorithm, and translate the chosen order back into the `JoinOrderPlan` public type. The public behavior must remain unchanged for fallback cases.

Finally, replace the snapshot format inside `SketchBasedJoinEstimator` persistence. The new format should be versioned, easy to reject, and tailored to the synopsis data needed by `StoreSynopsis`. On startup, old `join-estimator.rjes*` sidecars should be treated as unreadable old-format inputs; the estimator should rebuild and then write the new snapshot. Update persistence tests to verify this lifecycle rather than old resident-record details.

## Concrete Steps

Work from the repository root: `/Users/havardottestad/Documents/Programming/rdf4j-stf`.

Baseline compile, already completed and safe to rerun:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Expected proof:

    [INFO] BUILD SUCCESS
    [INFO] Total time:  20.987 s (Wall Clock)

Create or update the focused red tests. Use the repository’s preferred runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorJoinOrderPlannerTest --retain-logs --stream

If the new coverage lands in a different class, run that exact class or method instead. Save the first failing Surefire snippet in the thread and keep `initial-evidence.txt` at repo root.

After the first red test is recorded, implement the new package-private classes and rewire the estimator. Re-run the most focused class first, then the safety and persistence coverage:

    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorJoinOrderPlannerTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerReorderSafetyTest --retain-logs --stream
    python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorPersistenceTest --retain-logs --stream

Locate and update the LMDB theme accuracy integration test once the exact path is confirmed:

    rg -n "Theme|pathological|relative-error|join order" core/sail/lmdb/src/test/java

Then run the exact test class or method that covers the theme accuracy scenario.

Before final handoff, run the formatter and a final serial root install:

    cd scripts && ./checkCopyrightPresent.sh
    cd /Users/havardottestad/Documents/Programming/rdf4j-stf && mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources
    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

## Validation and Acceptance

Acceptance is behavioral, not structural. The change is complete only when the following are true.

The focused planner tests prove that the same connected subset and root variable produce the same canonical message regardless of construction order, and that the DP planner chooses the expected left-deep order for simple acyclic chain or star fixtures.

The unsupported-shape tests prove that cycles, multi-shared-variable attachments, `LeftJoin`, and unsupported wrappers do not invent sketch plans. Instead, `MessagePassingJoinPlanner` or the adapter returns no supported plan and the public optimizer path falls back exactly as it does today.

The persistence test proves the new lifecycle: a snapshot is written in the new format, a restart reloads it and reports readiness, supported join-order and cardinality estimates survive reload, and old low-level resident-record expectations are gone.

The LMDB accuracy test proves the new path is exercised end to end. The relative error must stay within the existing 20 percent bound, and the pathological theme query must now choose the selective chain order rather than the weaker heuristic order.

The public SPI must remain unchanged: `JoinOrderPlanner` and `QueryJoinOptimizer` signatures stay the same, `LmdbEvaluationStatistics.supportsJoinEstimation()` still reports readiness through the estimator, and unsupported cases still use the old optimizer fallback.

## Idempotence and Recovery

The commands in this plan are safe to rerun. Re-running the root install refreshes `.m2_repo` with the latest local snapshots. Re-running the targeted tests overwrites Surefire reports, which is why the first build proof was copied to `initial-evidence.txt` at repository root.

Snapshot migration is intentionally one-way. If old `join-estimator.rjes*` sidecars exist, the new code should ignore them and rebuild fresh state; this keeps retries safe because no manual conversion is required. If a partial implementation leaves the estimator unable to read its own new snapshot, delete only the newly written snapshot files produced by this branch and rerun the rebuild path. Do not delete unrelated repository files.

## Artifacts and Notes

Initial compile proof, preserved in `initial-evidence.txt`:

    [INFO] RDF4J: Sail base implementations ................... SUCCESS
    [INFO] RDF4J: LmdbStore ................................... SUCCESS
    [INFO] BUILD SUCCESS

Prototype assumption to preserve in runtime guards:

    connected, acyclic, unary/binary statement-pattern queries
    where each extension shares exactly one variable with the current connected prefix

Current persistence-model mismatch that forces a rebuild design:

    bindingsSketch(...) currently derives selectivity from complement Theta sketches;
    the new push algebra needs variable-conditioned neighborhood tuple sketches instead.

## Interfaces and Dependencies

Use the existing `org.apache.datasketches:datasketches-java:7.0.1` dependency already declared in `core/sail/base/pom.xml`. The implementation must use the on-heap tuple APIs in `org.apache.datasketches.tuple.arrayofdoubles`.

In `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/sketchoptimizer/StoreSynopsis.java`, define a package-private synopsis container that exposes degree sketches, neighborhood sketches, exact grounded row counts, and retained-hash resolution over the existing 64-bit fingerprint domain.

In `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/sketchoptimizer/PatternSynopsisAdapter.java`, define a package-private adapter that takes `List<TupleExpr>`, outer bindings, and filter heuristics from the estimator shell and produces either a supported synopsis graph or a rejection result with a concrete reason for fallback.

In `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/sketchoptimizer/MessagePassingJoinPlanner.java`, define a package-private planner that owns canonical-message memoization by `(subsetMask, rootVariable)`, exact DP, greedy fallback, and helper methods needed for join cardinality and `JoinOrderPlan` construction.

`core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SketchBasedJoinEstimator.java` must remain the public shell for lifecycle and persistence. `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SketchJoinOrderReorderer.java` becomes a thin integration layer. `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java` remains the runtime adapter and must not change its public semantics.

Plan revision note: created on 2026-03-18 because this task is a significant refactor and repository rules require an ExecPlan before implementation. The first revision captures the validated baseline, the storage-model gap, and the concrete implementation sequence.
