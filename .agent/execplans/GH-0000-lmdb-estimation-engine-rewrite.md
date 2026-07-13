# Replace LMDB Estimation with a Unified Engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must remain current throughout implementation. This document follows `.agent/PLANS.md`
from the repository root and supersedes `.agent/execplans/GH-0000-lmdb-planner-god-class-split.md` for the two LMDB
estimator facades.

## Purpose / Big Picture

LMDB query planning currently routes estimation through two classes that together exceed twenty-nine thousand lines:
`SketchBasedJoinEstimator` and `LmdbEvaluationStatistics`. Their duplicate overloads, object-heavy sketch state,
query-specific corrections, persistence, lifecycle, costing, and planning logic make it difficult to reason about why
a query received an estimate and make large workloads consume too much heap. After this work, every algebra node is
estimated through one normalized engine, every quad lookup is described by one probe shape, semantic row estimates
remain separate from physical work costs, and synopsis memory is bounded by configuration.

The result is observable in four ways. The two original classes are thin adapters under 500 lines. A one-million-row
incremental workload completes with a one-gigabyte heap while estimator state stays at or below 384 MiB. The existing
300-query audit has no false exact zeros, keeps worst q-error at or below 10, and improves p95 q-error by at least 20
percent. The planning benchmark improves its geometric mean by at least 25 percent without a representative-query
regression above 5 percent.

## Progress

- [x] (2026-07-13 04:58Z) Confirmed the requested plan from the attached source and selected Routine D.
- [x] (2026-07-13 04:58Z) Recorded the dirty branch state without resetting or overwriting user changes.
- [x] (2026-07-13 04:58Z) Identified the two facades as 17,586 and 11,655 lines respectively.
- [x] (2026-07-13 04:58Z) Created this replacement ExecPlan and superseded the old extraction plan.
- [x] (2026-07-13 09:07Z) Characterized public behavior, retained the available historical accuracy/heap artifacts,
  and recorded that a fresh comparable performance baseline could not be captured without the deferred harness.
- [x] (2026-07-13 05:15Z) Added a failing architecture contract before production changes.
- [x] (2026-07-13 05:27Z) Implemented and directly tested the bounded primitive quad synopsis.
- [x] (2026-07-13 06:55Z) Implemented and directly tested the canonical engine, evidence resolver, and access-cost
  model.
- [x] (2026-07-13 08:20Z) Cut both facades over, migrated production callers, and kept DPhyp as the sole LMDB
  join-order implementation.
- [x] (2026-07-13 08:20Z) Deleted unreachable legacy estimators, wrappers, payloads, strategy implementations, and
  implementation-detail tests.
- [x] (2026-07-13 09:07Z) Passed structural checks, direct JDK 25 compilation, 151 focused estimator/lifecycle tests,
  40 shared estimate-math tests, 68 end-to-end LMDB optimizer/store tests, and the one-million-addition `-Xmx1g`
  budget contract.
- [ ] Run fresh q-error, interval-coverage, JMH, allocation, and retained-heap promotion measurements.
- [ ] Run the deferred repository Maven gates only if the user's no-Maven constraint is lifted.

## Surprises & Discoveries

- Observation: The earlier god-class split introduced service classes that hold a reference to the facade and delegate
  straight back into it, so dependency ownership did not move.
  Evidence: `LmdbCardinalityEstimator`, `LmdbJoinFactorCostEstimator`, `LmdbJoinOrderEstimator`,
  `LmdbEstimatorScope`, `SketchEstimatorIngestService`, `SketchEstimatorPersistenceManager`, and
  `SketchJoinOrderService` each store one of the two facades.
- Observation: The compiled sketch facade exposes 860 methods and the statistics facade exposes 566 methods; source
  inspection finds many overload families representing the same operation with different carrier types.
  Evidence: `javap -p` against the current branch build reported those method counts before this rewrite.
- Observation: The current `OmniJoinEstimator` uses an object graph with one `ValueWeights` map per observed value.
  At one million incremental rows this shape exceeds a one-gigabyte heap.
  Evidence: retained-heap evidence recorded in `.agent/execplans/GH-0000-lmdb-bounded-cold-evidence.md` includes about
  2.27 million `ValueWeights` objects and roughly 577 MiB of primitive backing arrays before JVM overhead.
- Observation: Existing tests frequently use reflection to access private fields and methods of the two facades. Those
  tests preserve implementation accidents and cannot be the compatibility boundary for a replacement.
  Evidence: searches for `getDeclaredField` and `getDeclaredMethod` in the LMDB estimator tests identify configuration,
  persistence, budget, binding-cache, and join-product tests.
- Observation: The user explicitly prohibited Maven commands in this task. The implementation must therefore use
  source audits and direct Java compilation/execution where possible and must not claim the repository Maven gates
  passed.
  Evidence: the conversation contains the instruction `no maven commands`.
- Observation: A complete Count-Min table for every one of the sixteen bound-component masks stays small enough to
  update all masks uniformly; no shape-specific frequency implementation is needed.
  Evidence: direct JDK 25 tests exercised all masks and one million additions while the reported primitive allocation
  remained inside a 32 MiB test budget.
- Observation: A normalized context can retain more than sixty-four bound variables without another overload family.
  Evidence: the direct engine contract interned one hundred names into the shared `BindingUniverse`, produced a
  100-symbol `BindingMask`, and found the symbol at index 99.
- Observation: The shared `EstimateMath.distinct` result differs from a naive smallest-variable-distinct heuristic for
  multi-variable bags.
  Evidence: the focused engine test's initial hard-coded expectation was 2 rows while the supplied evidence and shared
  formula correctly produced 4; the contract now uses `EstimateMath` as the oracle.
- Observation: Property-path evidence is naturally reported per endpoint lookup, while the optimizer asks for total
  semantic rows over all prefix invocations. Identity-path guarantees also require endpoint identity, not merely two
  independently bound endpoints.
  Evidence: the initial property-path contracts returned 4 instead of 40 rows for ten invocations, returned 1 instead
  of 5 for a one-bound zero-length path, and incorrectly marked independently bound endpoints exact.
- Observation: `VALUES ... UNDEF` exposed two inconsistent finite-relation assumptions: immutable tuple construction
  rejected null cells, and exact join composition treated an unbound cell as a conflicting value.
  Evidence: the initial finite-relation contract failed in `StableValueTuple` with a `NullPointerException`; a separate
  disconnected OPTIONAL contract returned 3 rows instead of the exact Cartesian result of 2.
- Observation: The old low-heap test conflated the optional in-memory synopsis lifecycle with availability of semantic
  LMDB estimation.
  Evidence: after the unified cutover the low-heap process correctly omitted the optional synopsis but still exposed
  `LmdbEvaluationStatistics`; the contract now checks those two independent responsibilities.

## Decision Log

- Decision: Replace implementation ownership instead of extracting more facade methods.
  Rationale: Pass-through wrappers preserve the same cyclic dependency and keep private method families alive.
  Date/Author: 2026-07-13, Codex.
- Decision: Keep `BagEstimate` as the only semantic estimate inside the engine and use `EstimateMath` for algebra
  composition.
  Rationale: These shared types already encode bag multiplicity, variable evidence, finite relations, and distribution
  sketches, avoiding a second LMDB-only algebra model.
  Date/Author: 2026-07-13, Codex.
- Decision: Keep DPhyp as the only connected-join search implementation and remove estimator-owned join search.
  Rationale: Estimation should price candidates, not duplicate the planner that enumerates them.
  Date/Author: 2026-07-13, Codex.
- Decision: Use one production synopsis and normalize legacy strategy strings with one deprecation warning.
  Rationale: Runtime strategy branches are the source of duplicate algorithms and divergent semantics; comparative
  algorithms remain useful only as benchmark fixtures.
  Date/Author: 2026-07-13, Codex.
- Decision: Allocate the configured memory budget across fixed primitive structures at construction time and reject
  invalid budgets.
  Rationale: A count-based object map cannot offer a deterministic retained-heap ceiling.
  Date/Author: 2026-07-13, Codex.
- Decision: Publish immutable base snapshots and accept additions into a bounded primitive delta; any deletion marks
  the synopsis stale and schedules a rebuild.
  Rationale: Exact decrement support requires retaining unbounded per-key history, while rebuilding gives simple,
  auditable invalidation semantics.
  Date/Author: 2026-07-13, Codex.
- Decision: Preserve stable RDF4J and `LmdbStoreConfig` surfaces, but remove experimental sketch methods and migrate
  internal callers and tests.
  Rationale: The experimental API is excluded from compatibility checking and retaining hundreds of overloads would
  defeat the architecture target.
  Date/Author: 2026-07-13, Codex.
- Decision: Introduce no new dependency and no runtime code generation.
  Rationale: The JDK and existing primitive-support libraries are sufficient; the dominant problem is data layout and
  duplicated control flow, not expression dispatch.
  Date/Author: 2026-07-13, Codex.
- Decision: Keep exact-probe permission separate from the estimation tier in `EstimateContext`.
  Rationale: `EXACT` and `DECISION_EXACT` describe what evidence a caller may use, while the existing winner-overlap
  decision determines whether an expensive probe can affect the result. Both conditions must be true.
  Date/Author: 2026-07-13, Codex.
- Decision: Model property paths from one global path evidence source, then apply endpoint identity, finite-binding,
  and invocation transforms in `LmdbPropertyPathEstimator`.
  Rationale: This keeps semantic cardinality uniform across callers and avoids another family of shape-specific path
  estimators. A complete zero remains exact only for a positive-length direct probe.
  Date/Author: 2026-07-13, Codex.
- Decision: Centralize `UNDEF` semantics in `FiniteRelationEstimate` and reuse that evidence for JOIN, OPTIONAL, and
  MINUS composition.
  Rationale: Null is an unbound RDF solution-mapping cell, not a Java collection error or a value that conflicts with
  every bound value. One compatibility implementation prevents the algebra operators from diverging again.
  Date/Author: 2026-07-13, Codex.
- Decision: Permit low-heap stores to omit the optional synopsis allocation while retaining the unified semantic
  estimation engine.
  Rationale: The configured memory gate governs evidence storage, not optimizer correctness or interface availability.
  Date/Author: 2026-07-13, Codex.

## Outcomes & Retrospective

The production cutover is complete. `LmdbEvaluationStatistics` is 378 lines and `SketchBasedJoinEstimator` is 222
lines. Both now adapt interfaces and lifecycle to one package-private engine and synopsis. The largest new estimation
implementation is 410 lines, below the 700-line boundary. Lower layers do not reference either facade, join-order
entry points delegate to DPhyp, and the architecture contract finds no benchmark names, test identifiers, reflective
hooks, or query-specific corrections in production estimator sources.

The rewrite removed the duplicate OMNI, FastAGMS, Count-Min, tuple, join-sketch, cold-state, private-planner,
pass-through-service, and legacy payload implementations from production. Legacy strategy configuration values now
normalize to the unified strategy with one process-wide deprecation warning. Format v8 uses primitive sections and a
checksum; a v7 file contributes only durable store identity before the synopsis rebuilds from LMDB.

Direct JDK 25 verification passed 151 focused estimator and native lifecycle tests, 40 shared estimate-math tests, and
68 end-to-end LMDB optimizer/store tests. Every LMDB test source compiled against fresh shared and LMDB production
classes. The million-addition synopsis contract completed under `-Xmx1g` with a 32 MiB configured test budget. The
contracts found and fixed three architectural edge cases during the rewrite: total property-path cardinality must
scale per-lookup evidence by invocations, `UNDEF` must remain unbound through exact finite algebra, and low-heap
synopsis allocation must not disable semantic estimation.

This is a structurally and functionally verified implementation, not a promoted performance result. No fresh
300-query q-error/coverage audit, representative-query JMH comparison, allocation profile, JFR/async-profiler run, or
retained-heap measurement has been captured. Surefire, Failsafe, query-evaluation verification, japicmp, and the Maven
formatter also remain deferred by the user's no-Maven constraint. Those gates must pass before claiming the accuracy,
speed, retained-heap, API-compatibility, or release-promotion targets in this plan.

## Context and Orientation

The affected code is in Maven module `core/sail/lmdb`. `LmdbSailStore` owns LMDB storage and constructs estimator
infrastructure. `LmdbEvaluationStatistics` adapts storage evidence to the shared query optimizer interfaces.
Before this rewrite, `SketchBasedJoinEstimator` owned both sketch lifecycle and much of semantic estimation; it now
adapts only the unified synopsis lifecycle. The shared class
`BagEstimate` represents multiset, or bag, cardinality together with variable and distribution evidence.
`EstimateMath` combines those estimates for relational algebra. A `BindingUniverse` assigns stable integer positions to
query variables and a `BindingMask` stores any number of bound positions in primitive words. DPhyp is the existing
dynamic-programming hypergraph join planner and remains responsible for enumerating connected join orders.

The new dependency direction is strict:

    LmdbEvaluationStatistics / SketchBasedJoinEstimator
        -> LmdbEstimationEngine / QuadSynopsisLifecycle
        -> evidence sources, primitive synopsis snapshots, exact LMDB access

No class below the facade line may import, accept, store, or call either facade. `LmdbPlannerServices` becomes a view of
engine-owned services, not a collection of wrappers around `LmdbEvaluationStatistics`.

An estimate is semantic when it predicts how many solution mappings an algebra expression returns, including duplicate
multiplicity. A cost is physical when it predicts storage reads, seeks, invocations, temporary memory, or work. These
numbers must never be blended to force a preferred join order. An evidence candidate is a row estimate plus its source,
snapshot identity, completeness, confidence, calibrated interval, and freshness. Exact storage evidence and complete
finite relations always outrank sampled evidence. An incomplete sample may estimate a very small positive count but
may never claim an exact zero.

## Plan of Work

### Milestone 1: characterize contracts and add boundaries

Record current source size, compiled surface, production callers, existing accuracy evidence, and retained-heap
evidence in this file. Add `LmdbEstimatorArchitectureTest` before changing production. It must locate the production
source tree and assert that, at completion, each facade is at most 500 lines, each new estimator implementation class is
at most 700 lines, no implementation references a facade, and production estimator sources contain no benchmark query
names, test IRIs, query IDs, reflective hooks, or query-specific correction tables. During intermediate milestones the
size assertions may target the additive classes only; tighten them atomically with cutover rather than weakening the
final assertions.

Add behavior-level contract tests that use public storage/query paths or package-private collaborators directly. Cover
all four quad positions, repeated variables, named contexts, duplicates, exact versus sampled zero, finite VALUES,
joins, optional joins, minus, union, filters, grouping, distinct, projection, slices, paths, triple terms, feedback,
restart, corrupt persistence, additions, deletions, and rebuild. Do not add new reflection against either facade.

### Milestone 2: build a bounded primitive quad synopsis

Create package `org.eclipse.rdf4j.sail.lmdb.estimation`. Define `QuadProbe` as an immutable value with a four-bit bound
mask, four-bit projection mask, four stable component IDs, and a snapshot identity. Reject masks outside four bits and
unbound IDs supplied for bound positions. Define `QuadEvidence` as row evidence plus zero or more distribution sketches
keyed by projected mask.

Implement `PrimitiveCountMin`, `PrimitiveSpaceSaving`, and `PrimitiveBottomK` in separate files, each under 700 lines.
Use flat primitive arrays and deterministic 64-bit mixing. `PrimitiveCountMin` provides conservative scalar frequency
estimates. `PrimitiveSpaceSaving` retains the hottest conditioning hashes in fixed-capacity parallel arrays.
`PrimitiveBottomK` keeps the lowest unsigned witness hashes and associated four-ID rows in contiguous slabs without a
per-row object. Every structure reports allocated bytes and rejects inserts after its fixed capacity is exhausted in a
defined, deterministic way.

Implement `QuadSynopsisSnapshot` as immutable arrays plus metadata and `QuadSynopsisDelta` as a bounded append-only
addition layer. Implement `QuadSynopsis` as the query boundary and `QuadSynopsisBuilder` as the mutable rebuild owner.
Partition the configured budget before allocation, reserve persistence and metadata headroom, and assert that the sum
of allocated arrays never exceeds `sketchEstimatorMemoryBudgetBytes`. The default is 268,435,456 bytes. Additions update
the delta until its budget is full, then request a rebuild. Deletions never decrement approximate tables; they make the
current snapshot ineligible and request a background rebuild. Publication swaps one immutable snapshot atomically.

Implement `QuadSynopsisPersistence` using format version 8. The file contains a magic header, payload version, durable
store identity, snapshot version, section lengths, primitive sections, and a checksum over the payload. A version-7
file contributes only its durable identity; estimator payload bytes are discarded and rebuilt. A corrupt, truncated,
or incompatible payload produces a rebuild request rather than partial evidence.

### Milestone 3: build one estimation engine

Create package-private `EstimateContext` with `BindingUniverse`, `BindingMask`, complete finite-binding evidence, a
prefix `BagEstimate`, invocation count, evidence policy, `JoinFactorCostModel.EstimationTier`, snapshot version, and a
metrics preference. Provide one builder or factory at interface boundaries; do not add overloads accepting variable
sets, string arrays, and masks for the same operation.

Create package-private `LmdbEstimationEngine` with the canonical method:

    BagEstimate estimate(TupleExpr expression, EstimateContext context)

Its visitor estimates leaf statement patterns from exact LMDB evidence, complete finite relations, or one `QuadProbe`.
It composes every algebra operator using `EstimateMath`: inner join, left join, difference, union, filter, group,
distinct, projection, extension, ordering, and slice. Unsupported nodes receive one documented conservative fallback
that preserves child evidence rather than a query-specific correction. Query-scoped caching keys are normalized
expression shape, binding mask, finite-binding identity, policy, tier, and snapshot version.

Create `EstimateEvidenceResolver`. Exact current-snapshot LMDB evidence ranks first, then complete finite-relation
evidence, then current-snapshot candidates ordered by smallest calibrated relative interval, confidence, freshness, and
fixed source priority. It rejects wrong-snapshot and stale candidates. It refuses exact zero from incomplete samples.
At `EXACT` and `DECISION_EXACT` tiers it invokes an exact LMDB probe only when the existing winner-interval overlap
predicate reports that exact evidence can change the chosen alternative.

Create `LmdbAccessCostModel` returning an immutable access estimate with semantic rows, rows per invocation,
invocations, total work rows, seeks, and memory rows. It may inspect index shape and bound positions but may not modify
semantic rows. Convert to `EstimateVector`, `StatisticsEstimate`, and `CostVector` only in adapters.

Move execution feedback and Explain telemetry behind engine-owned interfaces. Feedback is evidence with a query shape,
snapshot, observed rows, and sample count; it passes through the same resolver and cannot bypass exact evidence.

### Milestone 4: cut over and remove legacy ownership

Rewrite `SketchBasedJoinEstimator` as a lifecycle adapter that constructs, loads, rebuilds, updates, persists, and closes
the synopsis. Keep only methods used by `LmdbSailStore` and the new engine. Normalize `omni`, `fastagms`, `countmin`,
`countmin-dual`, `tuple`, and `joinsketch` to the unified strategy, logging one deprecation warning per process for a
legacy value. Remove all query estimation, algebra traversal, join ordering, strategy branching, and internal test hooks
from this facade.

Rewrite `LmdbEvaluationStatistics` as an adapter over `LmdbEstimationEngine`, `LmdbAccessCostModel`, feedback, and scope
state. Implement the stable optimizer interfaces by constructing one `EstimateContext`, calling the canonical method,
and converting at the boundary. Delegate join-order entry points to DPhyp or decline when the DPhyp adapter cannot own
the input. Do not enumerate permutations or joins inside this class.

Rework `LmdbPlannerServices` to store engine-owned service interfaces. Remove
`LmdbCardinalityEstimator`, `LmdbJoinFactorCostEstimator`, `LmdbJoinOrderEstimator`, `LmdbEstimatorScope`,
`SketchEstimatorIngestService`, `SketchEstimatorPersistenceManager`, and `SketchJoinOrderService` if they remain pure
pass-through wrappers. Migrate each production caller directly to the correctly owned service.

After production is wired and direct tests cover the replacement, delete `OmniJoinEstimator`, old sketch algorithms
from main sources, duplicate cold/sketch state, version-1-through-7 payload readers except the minimal v7 identity
migrator, facade-private planners, redundant caches, orphan carriers, and benchmark-specific correction code. Move any
useful comparative legacy estimator only into benchmark test sources. Replace or delete tests whose only contract is a
private field, method, nested class, or exact heuristic constant.

### Milestone 5: enforce and measure

Tighten `LmdbEstimatorArchitectureTest` to the final limits and scan all LMDB production estimator sources. Confirm
both facades are at most 500 lines and no new estimator implementation class exceeds 700. Confirm no lower layer names
either facade and no second join enumerator is reachable from LMDB production planning.

Run deterministic exact-oracle tests over small generated datasets and randomized statement insertion orders. Run the
300-query audit in snapshot-only and adaptive modes, interval-coverage audit, query-plan benchmark, synopsis probe
benchmark, full-query benchmark, rebuild benchmark, incremental-ingest benchmark, and one-million-row heap workload.
Use the same JVM, dataset, warmup, and measurement settings as the captured baseline. Profile allocation when planning
moves materially and inspect JFR or async-profiler evidence before attributing a cause.

## Concrete Steps

All commands run from the repository root
`/Users/havardottestad/Documents/Programming/rdf4j-small-things`. The current user constraint forbids Maven commands,
so do not invoke Maven or wrappers that invoke Maven during this implementation session.

Before every edit group, inspect tracked changes:

    git status --short --untracked-files=no
    git diff --stat

Use direct source checks during implementation:

    git diff --check
    wc -l core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java \
      core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/SketchBasedJoinEstimator.java
    rg -n "SketchBasedJoinEstimator|LmdbEvaluationStatistics" core/sail/lmdb/src/main/java
    rg -n "benchmark|example\\.(org|com)|query[-_ ]?[0-9]+|getDeclared(Field|Method)" \
      core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb

Compile changed Java directly against the already-built module and dependency classpath when available. Record the
exact generated classpath and command in `Artifacts and Notes`; direct compilation is a syntax/linkage check, not a
substitute for the repository test lifecycle.

When the user later lifts the no-Maven constraint, use the repository's `mvnf` skill for focused and module tests, then
run query-evaluation verification, API compatibility, formatting, and copyright checks. Never use `-am` or `-q` for
tests. Preserve the first report summary in `initial-evidence.txt` as required by `AGENTS.md`.

## Validation and Acceptance

Functional acceptance requires unchanged exact SPARQL results and bag multiplicities for the algebra and lifecycle
matrix listed above. Estimation acceptance requires no false exact-zero evidence, worst q-error at most 10, p95 q-error
at least 20 percent better than the captured baseline, and at least 93 percent empirical coverage for nominal 95
percent intervals.

Performance acceptance requires at least 25 percent better geometric-mean planning latency and synopsis probe
throughput. No representative planning query may regress by more than 5 percent, and full-query execution, rebuild,
and ingestion may regress by no more than 5 percent. The million-row incremental workload must finish under `-Xmx1g`
and retain no more than 384 MiB of estimator state. Allocation profiling must show lower planning allocation; any
material benchmark movement must have matching profiler evidence.

Structural acceptance requires both facades at or below 500 source lines, all new implementation classes at or below
700, facade-to-engine-to-store dependency direction, no production benchmark/test/query-specific identifiers, one
production synopsis strategy, and DPhyp as the only LMDB join-order implementation.

Repository acceptance requires LMDB Surefire and Failsafe, query-evaluation verification, japicmp, formatting,
copyright, and diff checks to pass on JDK 25. Because Maven is prohibited in the active task, these gates remain
explicitly unverified until that constraint changes; do not describe the implementation as promoted before they run.

## Idempotence and Recovery

All additive milestones are safe to repeat. New persistence writes use a temporary file, checksum it, and atomically
replace the previous v8 snapshot only after success. A failed load leaves LMDB data untouched and requests a rebuild.
Deletion invalidation never mutates an immutable published snapshot.

The worktree was dirty before this plan. Never use reset, restore, clean, checkout-over-file, or blanket formatting.
Before modifying an already-dirty file, inspect its diff and preserve all unrelated hunks. If a cutover is interrupted,
leave the additive engine in place and restore compilation by finishing adapters; never resurrect the copied legacy
implementation or add a runtime switch. Keep all untracked artifacts.

## Artifacts and Notes

Initial branch: `GH-0000-lmdb-predicate-guarantees` at `e10568a46a`.

Initial facade sizes:

    17,586  SketchBasedJoinEstimator.java
    11,655  LmdbEvaluationStatistics.java
    29,241  total

Existing recorded accuracy evidence from the branch is a 300-query worst q-error at or below 10. Paired cold evidence
recorded snapshot-only p95 4.0 and worst 17.333 with two false exact zeros; adaptive p95 4.0 and worst 9.0 with up to
four false exact zeros across runs. The replacement must capture a fresh, comparable pre-cutover baseline before the
old code is removed; these historical values are context, not a valid promotion comparison.

Existing retained-heap evidence shows the object-per-value OMNI representation failing the million-row, one-gigabyte
heap scenario. The replacement's budget is measured from actual allocated primitive arrays and verified again through
heap evidence; a calculated budget alone is insufficient.

Direct JDK 25 milestone evidence, without Maven:

    EstimateEvidenceResolverTest: 4 tests successful, 0 failed, 63 ms
    LmdbEstimationEngineTest:      5 tests successful, 0 failed, 81 ms
    LmdbAccessCostModelTest:       2 tests successful, 0 failed, 66 ms

The production engine layer compiled directly against the existing module outputs. `LmdbEstimationEngine` is 281
lines, `EstimateEvidenceResolver` is 118 lines, `EstimateContext` is 129 lines, and `LmdbAccessCostModel` is 86 lines.
These are linkage and collaborator-contract checks, not substitutes for the deferred repository lifecycle gates.

Direct synopsis evidence (2026-07-13): `QuadSynopsisTest` passed 8 tests and
`QuadSynopsisPersistenceTest` passed 3 tests through JUnit Platform on JDK 25. The tests cover deterministic hashing,
Count-Min monotonicity, Space-Saving retention, bottom-k order independence, all quad masks, additions, deletion
invalidation, a million-row bounded update loop, v8 checksum round-trip/corruption, and v7 identity-only migration.
This direct run is not a replacement for the deferred Surefire/Failsafe gates.

Final direct JDK 25 evidence (2026-07-13), without Maven:

    Fresh shared estimate output:       /tmp/query-estimate-classes-unified-20260713-8
    Fresh LMDB production output:       /tmp/lmdb-production-classes-unified-20260713-8
    Fresh LMDB test compilation output: /tmp/lmdb-all-tests-compile-unified-20260713-8
    LMDB production sources compiled:   188, 0 errors
    Focused estimator/lifecycle tests:  151 successful, 0 failed
    Shared EstimateMath tests:           40 successful, 0 failed
    End-to-end optimizer/store tests:    68 successful, 0 failed
    Million-addition -Xmx1g contract:     1 successful, 0 failed, about 385 ms

Final structural measurements:

    378  LmdbEvaluationStatistics.java
    222  SketchBasedJoinEstimator.java
    410  LmdbEstimatorRuntime.java
    364  LmdbQuadSynopsisService.java
    292  LmdbPropertyPathEstimator.java
    281  LmdbEstimationEngine.java

`LmdbEstimatorArchitectureTest` passed as part of the 151-test slice. `scripts/checkCopyrightPresent.sh` reported that
all files have valid copyright headers and SPDX lines. `git diff --check` passed. Expected injected I/O exceptions in
failure-atomicity tests and broad-classpath SLF4J/LWJGL warnings did not fail the test runs.

## Interfaces and Dependencies

Create these package-private production types under
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/estimation` unless an existing shared type is named:

    record QuadProbe(byte boundMask, byte projectedMask, long subjectId, long predicateId,
            long objectId, long contextId, long snapshotIdentity)

    interface QuadSynopsis {
        QuadEvidence probe(QuadProbe probe);
        long snapshotVersion();
        long allocatedBytes();
        boolean isCurrent(long snapshotIdentity);
    }

    record EstimateContext(BindingUniverse universe, BindingMask boundMask,
            Optional<FiniteRelationEstimate> finiteBindings, BagEstimate prefixEstimate,
            double invocations, SketchEvidencePolicy evidencePolicy,
            JoinFactorCostModel.EstimationTier estimationTier, long snapshotVersion,
            MetricsPreference metricsPreference)

    final class LmdbEstimationEngine {
        BagEstimate estimate(TupleExpr expression, EstimateContext context);
    }

    record LmdbAccessEstimate(double semanticRows, double rowsPerInvocation, double invocations,
            double totalWorkRows, double seeks, double memoryRows)

    final class LmdbAccessCostModel {
        LmdbAccessEstimate estimate(TupleExpr expression, EstimateContext context, BagEstimate semanticEstimate);
    }

Use shared `BagEstimate`, `EstimateMath`, `FiniteRelationEstimate`, `BindingUniverse`, `BindingMask`,
`StatisticsEstimate`, `EstimateVector`, and `CostVector`; do not create LMDB copies. Use existing JDK and repository
dependencies only. Keep every implementation class under 700 lines and every dependency pointing away from facades.

Plan revision note (2026-07-13, Codex): Created the replacement plan from the user's supplied specification, added
repository-specific context and recovery rules, recorded the active no-Maven constraint, and closed the structural
and direct functional milestones after the facade cutover, legacy deletion, and direct JDK 25 verification. Accuracy,
benchmark, profiler, retained-heap, and repository lifecycle promotion gates remain open.
