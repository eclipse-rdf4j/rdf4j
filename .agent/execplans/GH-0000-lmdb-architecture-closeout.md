# Complete LMDB Compatibility and Optimizer Architecture

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document follows `.agent/PLANS.md` from the repository root. It is the umbrella plan for completing the current
LMDB optimizer, planner, and estimator backlog on branch `GH-0000-lmdb-predicate-guarantees`. A future contributor must
be able to resume using this file alone.

## Purpose / Big Picture

The branch already contains a working LMDB Cascades optimizer, a default-on DPhyp hypergraph join enumerator, and an
Omni sketch estimator. The branch is not finished because binary comparison cannot load the released experimental
sketch types, estimator persistence can publish the same temporary snapshot concurrently, and three very large
facades still contain most of the implementation. When this plan is complete, the released stable API comparison is
green, estimator snapshots are race-free, statistics and sketch services have explicit package-private ownership,
and one hypergraph planner owns LMDB join ordering with deterministic fallbacks. The observable result is a fully green
LMDB and query-evaluation verify, green japicmp reports, deterministic planner A/B benchmarks, and no disabled
estimate-audit contract tests.

## Progress

- [x] (2026-07-11 08:30Z) Audited the current branch, the retained module evidence, COVES leftovers, binary-comparison
  failure, and open architecture plans.
- [x] (2026-07-11 08:35Z) Chose one draft PR with sequential milestone commits; SIP, WCOJ, robust planning, and
  mid-query replanning remain later research.
- [x] (2026-07-11 09:36+02:00) Removed COVES-only generated artifacts and dedicated logs, removed `mysql/`,
  `Archive.zip`, and branch-added `CLAUDE.md`, preserved the full-module log, and completed the clean root install.
- [x] (2026-07-11 10:16+02:00) Repaired LMDB binary comparison with complete isolated old/new classpaths, advanced
  the reactor to `6.1.0-SNAPSHOT`, and passed both LMDB and query-evaluation japicmp gates offline.
- [x] (2026-07-11 10:28+02:00) Added a deterministic concurrent-persistence regression, serialized the complete
  publication cycle, and passed the focused selector plus all 51 persistence tests.
- [ ] (2026-07-11 10:51+02:00) Extracted the first estimator service slice: ingest policy, persistence-cycle
  coordination, scope lifecycle, fixed-order plan costing, frequency arithmetic, and OMNI membership estimation now
  have the requested package-private owners. Remaining deep OMNI/persistence kernels still need migration.
- [x] (2026-07-11 11:15+02:00) Composed the statistics facade behind the five requested package-private services and
  passed matching 100-test memoization coverage.
- [x] (2026-07-11 11:15+02:00) Made shared Cascades `EstimateVector` canonical for factor estimates, migrated every
  production consumer, removed dead `JoinCostVector`, retained the released nested vector descriptor as a deprecated
  forwarding API, and passed both compatibility gates.
- [ ] (2026-07-11 11:34+02:00) Routed the registry through top-level `LmdbGuaranteeOptionRule` and extracted focused
  guarantee costing, materialization, and diagnostics collaborators. The package-visible delegate still owns option
  enumeration and its private planner; removing that planner is the next behavior-changing slice.
- [x] (2026-07-11 15:33+02:00) Added opaque-factor dependency hyperedges and operator TES requirements for
  FILTER/EXISTS, conditioned OPTIONAL, LATERAL, and property paths; admitted atomic MINUS factors and passed all 19
  DPhyp adapter tests. Interesting-order physical-property states remain.
- [x] (2026-07-11 11:48+02:00) Added required-node state metadata and admitted property paths through endpoint-binder
  requirements; dependency-crossing hash/outer orientations are rejected and parameterized inner paths are retained.
- [x] (2026-07-11 12:39+02:00) Replaced pair-budget fallthrough with a deterministic connected greedy fallback inside
  the hypergraph package; zero-budget searches remain DPhyp-owned and expose `optimizer.dphypDegraded=1`.
- [x] (2026-07-11 14:07+02:00) Added and wired bounded `PlanTemplateCache` with canonical fingerprints, initial
  bindings, full optimization goal/properties, relevant DPhyp configuration, and monotonic statistics-scope version.
- [x] (2026-07-11 16:03+02:00) Keyed DPhyp frontiers by node set, required outer nodes, and `PhysicalProperties`;
  retained interesting orders, preserved nested-loop outer order, and passed all 11 core costing tests plus 19 adapter
  tests.
- [x] (2026-07-11 17:57+02:00) Made enabled DPhyp authoritative for two-factor, exact, over-cap, zero-variable, path,
  and bounded-finite-anchor islands; moved template caching around DPhyp, made the kill switch decline to the standard
  pipeline, removed 1.20 cross-planner arbitration, and passed 19 adapter, 15 admissibility, and 55 optimizer tests.
- [x] (2026-07-11 17:59+02:00) Restored the generic theme plan/run benchmark to sketches disabled, added explicit
  DPhyp false/true A/B parameters, and set the requested three-fork/five-measurement default; the LMDB quick reactor
  compile passed.
- [x] (2026-07-11 22:33+02:00) Re-enabled all estimate-audit contracts, repaired exact-zero and disconnected-product
  estimates, and passed the complete 300-query generated corpus with the worst-q-error limit of 10.
- [ ] (2026-07-12 00:43+02:00) Final LMDB verification is not green: sparse q6 still routes its UNION/correlated-anti
  shape through generic bound-lookup alternatives and reports 169.2B work rows; selected LeftJoin plans retain LEO
  feedback evidence but do not fuse it into `CostVector`. Focused regressions and the exact reports are retained.
- [x] (2026-07-12 01:25+02:00) Made correlated anti implementations compositional and excluded standalone
  bound-lookup enumeration from DPhyp-owned islands. Sparse q6 is green after fresh and stale-snapshot startup, and
  the 27-test context/admissibility/anti selector plus LMDB japicmp pass.
- [ ] Physically delete unreachable connected and guarantee mini-planners.
- [ ] Restore estimate-audit contracts, benchmarks, hygiene, and full verification.

## Surprises & Discoveries

- Observation: The tracked COVES test sources are gone, but ignored local evidence, compiled classes, copied resources,
  reports, and dedicated logs remain.
  Evidence: `rg -n -i 'coves|BgpSketchEstimatorComparison' initial-evidence.txt core/sail/lmdb/target logs/mvnf`.
- Observation: The first LMDB verify that could resolve the released 6.0.0 LMDB jar failed before compatibility
  analysis because `SketchBasedJoinEstimator$PatternFilterSamplingEstimator` was absent from the comparison classpath.
  The released `SketchBasedJoinEstimator` is annotated `@Experimental`; the missing class alone is not evidence that a
  stable compatibility facade is required.
  Evidence: `logs/mvnf/20260711-060605-verify.log` and the `6.0.0` source tag.
- Observation: A retained full LMDB run passed 2,088 unit tests and 131 integration tests but logged two snapshot
  `NoSuchFileException`s from main and scheduled persistence callers using the same generation temporary path.
  Evidence: `logs/mvnf/20260710-195725-verify.log`.
- Observation: The inherited japicmp 0.18.3 implementation resolves entries in `oldClassPathDependencies` and
  `newClassPathDependencies` as single artifacts; after the released query-evaluation jar was fetched, comparison
  advanced to a missing transitive `IterationWrapper` class and showed only that jar on the old classpath.
  Evidence: the 0.18.3 `JApiCmpMojo.resolveArtifact(...)` bytecode and the 2026-07-11 LMDB verify output.
- Observation: Twelve synchronized `persistIfDirty()` callers deterministically race in
  `OmniWitnessPersistenceStore.writeSnapshot(...)` because payload publication occurs before `persistLock`; multiple
  callers move the same generation's fixed `.tmp` path.
  Evidence: `logs/mvnf/20260711-082159-verify.log` and the focused Surefire persistence report.
- Observation: The estimator's fixed-order implementation did not use its work-adjuster or deferred-filter parameters;
  moving step construction into `SketchJoinOrderService` preserves that existing behavior and makes the unused
  extension seam explicit for later removal or implementation.
  Evidence: matching 8-test `LmdbFiniteValuesJoinSurfacePlanningTest` runs before and after extraction.
- Observation: `JoinCostVector` had no production caller; its only executable consumer was its dedicated comparator
  test. Historical benchmark text still contains the old diagnostic rendering and is intentionally preserved.
  Evidence: `rg -n 'JoinCostVector' core/sail/lmdb/src/main/java core/sail/lmdb/src/test/java` before deletion.
- Observation: The remaining guarantee rule is roughly 1,000 lines and mixes rule matching, option generation,
  fixed/dynamic join planning, materialization, and diagnostics. A top-level registry rule can delegate without changing
  rule identity, providing a green seam before the private mini-planner is removed test-first.
  Evidence: matching registry and `LmdbCascadesContextPropagationTest` runs.
- Observation: Adding an opaque dependency edge alone is insufficient if ordinary shared-variable edges incident to
  that factor remain: the enumerator can bypass the dependency through the simple edge. Shared selectivity must remain
  a TES predicate while dependency hyperedges provide the only connectivity for that opaque node.
  Evidence: the red/green `opaqueRequiredVarBecomesHyperedgeDependency` regression.
- Observation: Undirected dependency hyperedges alone still allow the cost receiver to flip a dependent path to the
  hash/outer side. Required-node metadata must participate in physical-plan admissibility, not connectivity alone.
  Evidence: the red/green `propertyPathWithLaterEndpointBinderIsParameterizedInner` regression.
- Observation: Collecting no condition variables can return an immutable empty set; subtracting assured bindings from
  it failed only in the broader adapter selector after focused correlated tests passed.
  Evidence: the red/green full `LmdbHypergraphJoinPlannerTest` run around `admitsSafeNonScopeChangingFilterFactors`.
- Observation: A flat map keyed by complete DPhyp state would make every `hasSeen` and pair lookup scan all retained
  states. A node-set-indexed frontier preserves the complete key while keeping enumeration lookup proportional to the
  alternatives for one node set.
  Evidence: `CostingReceiver`'s `Map<Long, Map<StateKey, JoinPlan>>` and the 11-test core costing selector.
- Observation: Treating every path endpoint producer as a conjunctive dependency can disconnect the graph and force a
  path to wait for both endpoints, although either endpoint is sufficient. Choosing the cheapest endpoint producer and
  retaining other endpoint equalities as connectivity preserves legal alternatives.
  Evidence: the red/green `connectedPlannerDoesNotHardWaitForMoreExpensivePathEndpoint` selector.
- Observation: Deterministic greedy fallback initially considered nested lookups only for explicitly parameterized
  nodes, delaying an ordinary but cheaply bound bridge behind padding scans. Greedy must compare hash and lookup access
  for every connected singleton, just like exact costing.
  Evidence: the red/green `greedyPlannerBuildsFiniteAnchorBeforeExpensiveBoundBridgeLookup` selector.
- Observation: Full-corpus state contamination exposed exact-zero, independent OPTIONAL, and disconnected-filter
  estimate regressions that isolated templates did not reproduce. Exact statement zero repair, non-exact-zero
  parameterized rejection, and disconnected-component row preservation close all 300 generated queries.
  Evidence: `logs/mvnf/20260711-201453-verify.log` through the green full-corpus run and focused q44/q74 reports.
- Observation: Sparse q6 is not entering the DPhyp rule at all. Its UNION and correlated `NOT EXISTS` leave the
  connected prefix under generic bound-lookup rules, producing `plannedAntiExistsInputRows=1.45817075712E10` and
  root `plannedCostWorkRows=1.692472959332E11` even after a fresh or stale-snapshot rebuild.
  Evidence: `logs/mvnf/20260711-224114-verify.log` and `LmdbSparsePrefixCostTest`'s Surefire report.
- Observation: Completed LeftJoin feedback is retained in `optimizer.leoEvidence`, but the selected plan no longer
  carries `plannedEstimateFusion=operator_feedback` or the structured operator-feedback q-error metrics.
  Evidence: the two failures in `LmdbOperatorFeedbackPlanningTest` from the final LMDB inventory.

## Decision Log

- Decision: Repair japicmp with separate old and new dependency classpaths before adding or restoring APIs.
  Rationale: japicmp must load the released dependency graph before the existing experimental/internal exclusions can
  be applied. Broad ignores or dummy compatibility types would hide real stable breaks.
  Date/Author: 2026-07-11 / Codex.
- Decision: Keep the inherited japicmp version, enumerate both complete isolated classpaths, and enforce binary/source
  compatibility through the existing semantic-version gate.
  Rationale: Separate mode disables the implicit project classpath, so both the old and new query-evaluation/LMDB
  closures and compared archives must be present. A module-local trial of 0.26.1 adds no resolution benefit. The user
  directed the reactor snapshot to advance to `6.1.0-SNAPSHOT`, so the inherited semantic-version enforcement can remain
  unchanged while comparing to released `6.0.0`.
  Date/Author: 2026-07-11 / Codex.
- Decision: Keep one draft PR but land independently green milestone commits.
  Rationale: This is the user's requested delivery shape; green boundaries retain review and rollback points without a
  history rewrite or force-push.
  Date/Author: 2026-07-11 / Codex.
- Decision: Retain `JoinFactorCostModel.EstimateVector` and `FactorCostEstimate.getEstimateVector()` as deprecated
  forwarding descriptors while making `getNormalizedEstimateVector()` canonical internally.
  Rationale: The nested type and getter were released as stable public API in 6.0, so removing them would contradict
  the zero-stable-incompatibility acceptance criterion. All production consumers now use the shared Cascades type.
  Date/Author: 2026-07-11 / Codex.
- Decision: Use `BagEstimate` as the statistics/evidence currency, Cascades `EstimateVector` as the normalized estimate
  boundary, and `CostVector` as the only ranking currency.
  Rationale: These types already carry the required evidence, uncertainty, and objective fields; introducing another
  cost record would perpetuate the duplicate-currency problem.
  Date/Author: 2026-07-11 / Codex.
- Decision: Preserve the DPhyp kill switch, but when enabled keep degradation inside the hypergraph planner.
  Rationale: The kill switch is the rollback boundary; silently falling into another join-order implementation while
  enabled would prevent one-planner acceptance and make benchmark attribution ambiguous.
  Date/Author: 2026-07-11 / Codex.
- Decision: Serialize the complete `persistIfDirty()` cycle with a dedicated cycle lock rather than widening
  `persistLock` or inventing unique temporary-file names.
  Rationale: Payload and manifest files form one publication transaction. A cycle lock prevents scheduled and direct
  publishers from interleaving while preserving the existing rule that state-lock work does not hold `persistLock`.
  Date/Author: 2026-07-11 / Codex.
- Decision: Correlated anti rules declare legality and physical properties but do not precompute a complete subtree
  cost; Cascades derives the operator estimate from the selected memo input winner.
  Rationale: Pricing the logical subtree during rule application ignored DPhyp's physical child and retained stale
  bound-lookup work even after a better child plan was selected.
  Date/Author: 2026-07-12 / Codex.

## Outcomes & Retrospective

Implementation is in progress. Update this section at every milestone with the observable behavior delivered, retained
evidence, remaining gaps, and any deviation from the decisions above.

The persistence milestone now prevents scheduled and direct callers from moving the same temporary snapshot file.
The dedicated cycle lock preserves the former `persistLock` boundary, and the latest lazily loaded OMNI snapshot is
readable after the concurrent run. The focused selector passed in 0.483 seconds and the 51-test persistence class
passed in 10.167 seconds; retained logs are `logs/mvnf/20260711-082611-verify.log` and
`logs/mvnf/20260711-082713-verify.log`.

The first estimator facade slice is also complete. `SketchEstimatorIngestService` owns add/delete/deferred-update
policy; `SketchEstimatorPersistenceManager` owns publication serialization; `SketchOptimizationScope` owns nested
thread-local scope lifecycle; `SketchJoinOrderService` owns fixed-order step construction and summary costing;
`FrequencySketchEstimator` owns net frequency arithmetic; and `OmniSketchEstimatorService` owns retained-witness
membership estimation. Matching scope, persistence, rebuild-parity, fixed-order, and OMNI selectors are green.

The statistics composition and currency boundary are complete. `LmdbPlannerServices` composes
`LmdbCardinalityEstimator`, `LmdbJoinFactorCostEstimator`, `LmdbJoinOrderEstimator`,
`LmdbExecutionFeedbackRecorder`, and `LmdbEstimatorScope`; concrete-statistics detection remains centralized there.
`FactorCostEstimate` now creates one canonical shared `EstimateVector`, while the released nested descriptor forwards
the same values. `CostVector` is the remaining production ranking currency. The 100-test memoization selector and both
japicmp module gates pass.

The first guarantee-rule split is green: `LmdbGuaranteeOptionRule` is the registry entry, and
`LmdbGuaranteeCostEstimator`, `LmdbGuaranteeMaterializer`, and `LmdbGuaranteeDiagnostics` own their focused concerns.
The existing option-enumeration delegate intentionally remains until ordinary memo-alternative tests define the
behavioral replacement for its private join planner.

DPhyp now admits factors certified by the island owner instead of rejecting every non-statement factor. Opaque input
requirements are mapped to dependency hyperedges, and incident equality selectivities remain delayed TES predicates so
they cannot provide an illegal shortcut. The focused regression and the full 14-test DPhyp adapter class pass.

Parameterized path admission is also complete at the DPhyp adapter/core boundary. The graph records per-node outer
requirements; costing rejects dependency-crossing hash joins and reversed nested loops, retaining only an inner lookup
whose outer covers the endpoint requirement. All 15 adapter tests and 9 core costing tests pass.

Exact DPhyp enumeration now uses a configurable deterministic pair budget. Exhaustion invokes an O(n²) connected
greedy plan builder inside `HypergraphOptimizer`, never the legacy connected planner, and stamps explicit degradation
evidence. The full 16-test DPhyp adapter class passes.

Plan templates now use a bounded typed cache rather than raw scoped map entries. Cache identity includes factor
fingerprints, bindings, goal/properties, DPhyp configuration and statistics snapshot scope; the identity regression and
all 100 statistics memoization tests pass.

Opaque operator eligibility is now explicit. FILTER conditions (including EXISTS trees) and conditioned OPTIONALs
derive required variables after subtracting their internally assured bindings; those variables become dependency
hyperedges and TES predicates. MINUS remains an atomic anti-join factor, while existing LATERAL input requirements and
property-path endpoint requirements share the same parameterized-inner enforcement. All 19 adapter tests pass.

DPhyp state retention now distinguishes required outer nodes and shared Cascades `PhysicalProperties` within each node
set. Scan alternatives can retain interesting orders, nested loops preserve the outer ordering, hash joins explicitly
destroy it, and parameterized singleton requirements clear only when a legal outer supplies them. The full core costing
and LMDB adapter selectors pass.

Enabled DPhyp is now the connected-rule dispatch for all supported island sizes. Exact-cap and pair-budget exhaustion
degrade through deterministic greedy costing; tiny disjoint VALUES anchors receive a bounded common-bridge
simplification; ground filters enter only after the runtime component; and paths select the cheapest sufficient
endpoint. Disabling DPhyp makes the Cascades rule decline to the standard pipeline. A legal Cascades winner is no longer
replaced through the former 1.20 standard-plan cost comparison. The obsolete connected-DP helpers are now unreachable
and still need physical deletion together with the guarantee rule's private planner.

`ThemeQueryPlanRunBenchmark` now separates DPhyp and sketch configuration. Its generic default keeps sketches off,
the DPhyp parameter exposes both rollback and authoritative paths, and its annotations provide three forks and five
measurement iterations for the requested plan/run comparisons.

The estimate-audit closeout is green. All previously disabled audit contracts are active, the 30-template smoke corpus
and complete 300-query stateful corpus pass, and focused regressions cover exact-zero nested filters, independent
OPTIONALs, and disconnected selective/low-threshold filters. Obsolete left-deep assertions encountered by full-module
verification were replaced with connectedness, no-Cartesian-work, bounded-work, direct-access, and semantic-rule
proof assertions.

Final module acceptance remains open. The last complete LMDB inventory identified two genuine production gaps after
the test migrations: sparse q6's UNION/correlated-anti shape bypasses DPhyp ownership and produces astronomical generic
bound-lookup work, and completed LeftJoin feedback is visible only as LEO evidence instead of being fused into the
selected ranking vector. These require architectural fixes; no metric cap, assertion weakening, or feature disable was
used to hide them.

The sparse-q6 production blocker is closed. `LmdbCorrelatedNotExistsAntiFilterRule` and the correlated MINUS
implementation now retain memo inputs with zero precomputed rule cost, so concrete unary costing consumes the selected
child's `BagEstimate`. DPhyp-owned islands no longer expose `lmdb-inner-join-bound-lookup` as a competing whole-island
implementation. The fresh/stale sparse selector passes all three tests, and the neighboring 27 rule/context tests pass;
retained focused logs begin at `logs/mvnf/20260711-231334-verify.log` and the green sparse log is
`logs/mvnf/20260711-231807-verify.log`.

## Context and Orientation

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/SketchBasedJoinEstimator.java` is the sketch facade and
currently owns ingestion, persistence, Count-Min/FastAGMS, Omni estimation, join ordering, and optimization-scope state.
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java` is the LMDB statistics facade
and also implements cardinality, factor costing, join ordering, feedback, and caches. `LmdbCascadesRuleProvider.java`
is the rule registry and still contains the guarantee-option implementation. `LmdbHypergraphJoinPlanner.java` adapts
plain connected statement-pattern islands to the DPhyp implementation under `org.eclipse.rdf4j.sail.lmdb.hypergraph`.

Maven uses JDK 25 and the workspace-local `.m2_repo`. Test commands never use `-am` or `-q`. `mvnf` performs the
required root quick install before a selected verify. Direct compatibility verifies run tests skipped and may use
`-pl ...` without `-am`. The root formatter may use `-q` because it is not a test run.

An estimate is the predicted row/work/evidence state. A cost is the comparable physical-plan score derived from that
estimate. A total eligibility set (TES) names the factors that must be present before a non-inner-join predicate may be
applied. An interesting order is a physical row ordering worth retaining even when it is not the cheapest unordered
plan. A parameterized path requires named bindings from an outer plan before it can execute cheaply.

## Plan of Work

First remove only approved leftovers: stale COVES evidence and generated artifacts, the empty source resource directory,
and the unrelated `mysql/`, `Archive.zip`, and branch-added `CLAUDE.md`. Preserve full-module logs even when they mention
the former test. Run a clean build so Maven compiler-status files no longer mention deleted sources.

Next reproduce binary comparison with tests skipped and save the failure in `initial-evidence.txt`. Configure the LMDB
japicmp execution with `oldClassPathDependencies` on released query-evaluation and `newClassPathDependencies` on the
current query-evaluation artifact. Run both module comparisons. Experimental/internal differences remain governed by
the existing annotations. Any remaining stable public descriptor is preserved by restoring its owner or a real
deprecated forwarding API; never use `ignoreMissingClasses`, a broad package exclusion, or a dummy stub.

Before changing persistence, add a deterministic test that starts two `persistIfDirty()` calls against one estimator,
blocks the first at snapshot publication, admits the second, and proves both calls complete without a missing temporary
file and with a readable latest generation. Serialize publication through a dedicated persistence-cycle lock while
leaving ingestion state unlocked. Keep mutation-version semantics so a mutation arriving during persistence remains
dirty after the captured cycle completes.

Split `SketchBasedJoinEstimator` behind package-private `SketchEstimatorIngestService`,
`SketchEstimatorPersistenceManager`, `FrequencySketchEstimator`, `OmniSketchEstimatorService`,
`SketchJoinOrderService`, and `SketchOptimizationScope`. The facade retains construction, configuration, lifecycle,
existing external methods, the current LMDB property prefix, and both legacy prefixes. Move algorithms without copying
state or adding allocation to ingestion/probe hot paths.

Split `LmdbEvaluationStatistics` behind package-private `LmdbCardinalityEstimator`,
`LmdbJoinFactorCostEstimator`, `LmdbJoinOrderEstimator`, `LmdbExecutionFeedbackRecorder`, and `LmdbEstimatorScope`,
composed by `LmdbPlannerServices`. Eliminate concrete-statistics downcasts outside that composition boundary. Migrate
statistics results to `BagEstimate`, normalize them to Cascades `EstimateVector`, and rank only `CostVector`. Remove
`JoinFactorCostModel.EstimateVector` after all callers use the shared type and compatibility remains green.

Move the remaining guarantee implementation into top-level `LmdbGuaranteeOptionRule` with focused package-private
costing, materialization, and diagnostics collaborators. Keep rule id, priority, registry order, and proof metadata.
Guarantee choices become ordinary memo alternatives; delete the rule's private join-order planner.

Complete DPhyp by deriving opaque-factor hyperedges from `opaqueRequiredVars`; model OPTIONAL, MINUS, EXISTS, and
LATERAL with conflict rules and TES; admit property paths through endpoint requirements; and key DP states by node set,
required outer bindings, and existing `PhysicalProperties`. Add a bounded `PlanTemplateCache` keyed by canonical factor
fingerprint, initial binding mask, optimization goal, relevant configuration, and statistics snapshot version. Replace
wall-clock degradation with deterministic state/pair budgets and connected graph simplification.

Finally make the hypergraph planner authoritative for enabled LMDB join islands. Remove the statistics bounded planner,
guarantee mini-planner, redundant commute/associate enumeration for owned islands, and the 1.20 standard/Cascades
arbitration. With DPhyp disabled, the standard pipeline remains the explicit rollback. Restore the generic theme
benchmark's upstream sketches-disabled default and add explicit sketch/DPhyp A/B parameters to
`ThemeQueryPlanRunBenchmark`.

Re-enable estimate-audit contract and q-error tests. Keep the full-corpus worst q-error at 10 and targeted bounds at 4.
Replace obsolete plan-string/left-deep assertions with result equivalence, connectivity, no unexpected Cartesian work,
bounded work, and cost dominance. Finalize the related Omni, god-class, and DPhyp plans and record Phase-6 research as
deferred.

## Concrete Steps

All commands run from the repository root.

1. Before each test session, run the root clean-install template from `AGENTS.md`. For focused tests prefer:

       python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs

2. Binary gates, with tests skipped:

       mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipTests verify
       mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -DskipTests verify

3. Focused estimator gates:

       python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorPersistenceTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py SketchBasedJoinEstimatorRebuildParityTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py OmniJoinEstimatorTest --retain-logs

4. Focused statistics and rule gates:

       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbEvaluationStatisticsMemoizationTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBoundJoinProductBlendTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCascadesOptimizerTest --retain-logs
       python3 .codex/skills/mvnf/scripts/mvnf.py LmdbRuleRegistryCoverageTest --retain-logs

5. DPhyp gates begin with the existing hypergraph unit classes and new focused regressions for opaque factors, TES,
   property paths, parameterized paths, interesting orders, cache identity, and deterministic budget fallback.

6. A/B benchmark after functional greens:

       scripts/run-single-benchmark.sh --module core/sail/lmdb \
         --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryPlanRunBenchmark --method planQuery \
         --forks 3 --measurement-iterations 5 --param dphypEnabled=false

   Repeat with `dphypEnabled=true`, then repeat both for `runQuery` on the selected theme queries.

7. Final gates:

       python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs
       mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation verify
       cd scripts && ./checkCopyrightPresent.sh
       mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
       git diff --check

## Validation and Acceptance

Cleanup is accepted when no COVES/BGP-comparison path or identifier remains outside preserved full-module logs and the
unrelated BSBM dictionary word. Compatibility is accepted when both module verifies produce japicmp reports with no
missing classes and no stable binary/source break. Persistence is accepted when the deterministic concurrent test and
the full module produce no snapshot `NoSuchFileException`.

Each behavior-neutral extraction must show matching pre/post green selectors and direct hit proof. Every new DPhyp
capability begins with a failing in-repo regression. Metamorphic result tests cover OPTIONAL, MINUS, EXISTS, LATERAL,
property paths, and disconnected queries. Enabled DPhyp must not fall into another join-order implementation; bounded
searches degrade deterministically without wall-clock decisions.

Final acceptance requires green LMDB and query-evaluation modules, green japicmp, formatter, copyright, and diff checks;
no weakened assertions; no disabled estimate-audit contract tests; and benchmark evidence for both DPhyp states. The
three facades must contain orchestration rather than owned algorithms, and searches must show no duplicate private join
planner or 1.20 arbitration.

## Idempotence and Recovery

Cleanup targets only named files and generated COVES artifacts. Preserve unrelated untracked artifacts. Every structural
milestone is committed only after its matching selectors pass. If a behavior-neutral extraction changes a plan or
result, stop, restore the pre-change shape for that slice, add a failing regression, and continue as behavior-changing
work. Do not rewrite history, force-push, reset, clean the repository, or weaken compatibility exclusions.

If offline dependency resolution fails, rerun the exact command once without `-o`, then return offline. If a long LMDB
suite fails, keep its log and reports, rerun the smallest failing selector, and update Progress and Discoveries before
editing.

## Artifacts and Notes

Keep the current task's first failure in root `initial-evidence.txt`. Keep focused and full logs under `logs/mvnf` with
their command and report snippets recorded in this file. Do not treat an offline japicmp warning that cannot resolve the
old artifact as a compatibility pass.

The required root clean install completed with `BUILD SUCCESS` in 2:05. The pre-fix LMDB comparison failed in
`japicmp:cmp` because it could not load
`SketchBasedJoinEstimator$PatternFilterSamplingEstimator`; the exact command and failure are preserved in
`initial-evidence.txt`.

After the reactor version was advanced at the user's direction, the 6.1 root quick clean install completed with
`BUILD SUCCESS` in 37.025 seconds. The LMDB compatibility command completed with `BUILD SUCCESS` in 16.829 seconds and
the query-evaluation compatibility command completed with `BUILD SUCCESS` in 6.469 seconds. Both ran offline; the
generated reports contain no missing classes and mark all reported modifications source- and binary-compatible.

The concurrent persistence selector failed before the fix with 1 test, 1 failure, and 0 errors. Its assertion captured
`NoSuchFileException` while moving `omni-witness-b-2.dat.tmp` to `omni-witness-b-2.dat`; the retained Maven log is
`logs/mvnf/20260711-082159-verify.log`.

## Interfaces and Dependencies

No third-party dependency is added. Japicmp classpath dependencies are build-only comparisons of existing RDF4J
artifacts. New estimator/statistics/rule/cache collaborators are package-private. Existing public facades, stable method
descriptors, configuration keys, rule IDs, and Explain metric names remain intact. Shared `BagEstimate`, Cascades
`EstimateVector`, `CostVector`, `PhysicalProperties`, and `OptimizationGoal` are reused rather than duplicated.

Revision note (2026-07-11, Codex): Created the umbrella closeout plan after the user approved one draft PR and the
current-backlog scope. Reason: replace several stale overlapping plans with one restartable implementation sequence while
retaining those plans as historical evidence.
