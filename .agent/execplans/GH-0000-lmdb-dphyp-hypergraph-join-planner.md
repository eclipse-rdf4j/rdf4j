# DPhyp hypergraph join planner for the LMDB Cascades optimizer

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It must be maintained in accordance with `.agent/PLANS.md` (from the repository root).

The design source for this work is `hypergraph-plan.md` (repository root), an analysis of MySQL's hypergraph join optimizer with a database-agnostic implementation plan. This ExecPlan adapts that plan to this repository. All algorithm knowledge needed to implement the work is embedded below; `hypergraph-plan.md` is useful background reading but not required.

## Purpose / Big Picture

The LMDB SAIL plans SPARQL joins with `LmdbCascadesConnectedJoinPlanner` (in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`). Despite its estimate-source name ("lmdb-cascades-connected-hypergraph"), that planner enumerates only *connected left-deep prefixes*: plans of the shape `((A ⋈ B) ⋈ C) ⋈ D` where every new factor attaches to the accumulated prefix. It never considers *bushy* plans such as `(A ⋈ B) ⋈ (C ⋈ D)`, which can be dramatically cheaper when two selective islands should each be reduced before being combined.

After this change, the repository contains a true hypergraph join optimizer in the new package `org.eclipse.rdf4j.sail.lmdb.hypergraph`: a DPhyp subgraph enumerator (the "DPhyp" algorithm from Moerkotte & Neumann, *Dynamic Programming Strikes Back*, also used by MySQL 8's hypergraph optimizer) plus a dynamic-programming costing receiver that considers all connected join trees — bushy included — without ever proposing a Cartesian product. A flag-gated adapter (`LmdbHypergraphJoinPlanner`, default **off**) plugs it into the existing Cascades planner as an alternative search strategy that produces the exact same `Plan` output record, so everything downstream (rule registration, cost vectors, EXPLAIN) keeps working.

You can see it working by running the new unit tests (they run without any LMDB store), and by enabling `-Drdf4j.optimizer.lmdb.cascades.connectedJoin.dphyp=true` and observing plans whose `algorithm` is `dphyp-bushy` in the connected-join trace.

## Progress

- [x] (2026-07-06) Repository reconnaissance: existing planner entry points, estimate API, test conventions, header conventions.
- [x] (2026-07-06) ExecPlan authored (this file).
- [x] (2026-07-06 21:50Z) Milestone A: core data structures — `NodeSets`, `Hypergraph` (+ DOT printer) with unit tests (15 tests green).
- [x] (2026-07-06 21:50Z) Milestone B: DPhyp enumerator `SubgraphEnumerator` + naive DPsub oracle + acceptance/property tests (12 tests green, incl. 200-random-graph oracle parity).
- [x] (2026-07-06 21:50Z) Milestone C: `PlanHypergraph`, `JoinPlan`, `CostingReceiver`, `HypergraphOptimizer` facade + costing tests (16 tests green; bushy-beats-left-deep proven).
- [ ] Milestone D: LMDB adapter `LmdbHypergraphJoinPlanner` behind system property, hooked into `LmdbCascadesConnectedJoinPlanner.plan(...)`, with unit tests using a synthetic cost model.
- [ ] Test evidence captured (Surefire snippets in `Artifacts and Notes`), full module regression triaged against known-red baseline, committed.

## Surprises & Discoveries

- The existing planner already names its estimate source "hypergraph" but is left-deep-prefix only; the new code must use distinct naming (`dphyp`) everywhere to avoid confusion.
- `JoinFactorCostModel.estimateFactorCost(factor, CostContext)` is prefix-oriented: it estimates one factor given bound variables and prefix rows. There is no direct API for "rows of joining two arbitrary subtrees", so the adapter derives per-edge selectivities from pairwise conditional probes (see Decision Log) and keeps one canonical row estimate per node set.
- `Join` in RDF4J algebra is binary and evaluation handles arbitrary trees, but this repo's LMDB evaluation can execute the right side of a Join per left-row (bound join). The adapter therefore costs a bushy right side under an explicit hash/materialized-join model; execution-side operator choice remains RDF4J's. Flag stays default-off until benchmarked (Milestone E, future).
- The working tree at the start of this work already contained staged deletions of finished execplans and the staged design doc `hypergraph-plan.md`; commits for this work must add only its own files (`git commit -- <paths>`), leaving the user's staged state untouched.
- DPhyp emits each csg-cmp pair once with *one* connecting edge index even when several hyperedges cross the cut. The costing receiver therefore derives join cardinality from the predicate list (every predicate whose TES becomes newly satisfied), not from the reported edge, which also makes cycle predicates apply exactly once with no extra bookkeeping.

## Decision Log

- Decision: implement the core in a new self-contained package `org.eclipse.rdf4j.sail.lmdb.hypergraph` with no dependencies on LMDB classes or RDF4J algebra; the adapter lives in `org.eclipse.rdf4j.sail.lmdb` beside the existing planner.
  Rationale: mirrors MySQL's split (payload-free `Hypergraph` vs `JoinHypergraph`), keeps the enumerator unit-testable in milliseconds, and lets future callers (e.g. memory store) reuse it.
  Date/Author: 2026-07-06 / Claude Code.
- Decision: `NodeSet` is a primitive `long` (max 64 nodes; adapter caps at a configurable limit, default 14, well below the existing planner's 20-factor exact-DP limit).
  Rationale: same as MySQL's `NodeMap`; every hot operation is a bitwise op. The adapter declines larger islands so the existing planner (greedy above 20) handles them.
  Date/Author: 2026-07-06 / Claude Code.
- Decision: connectivity is the edge structure's job; *cardinality* is carried entirely by a predicate list where every join predicate has a Total Eligibility Set (TES) and a selectivity. A join of subsets (S1, S2) applies exactly those predicates whose TES is contained in S1 ∪ S2 but in neither side alone.
  Rationale: this makes "apply every predicate exactly once" a theorem instead of bookkeeping (a predicate's TES becomes newly-satisfied at exactly one cut in any join tree), which is MySQL's cycle-predicate promotion generalized. We do not need predicates attached to operators because the output is a reordered `Join` tree; RDF4J derives join conditions from shared variables.
  Date/Author: 2026-07-06 / Claude Code.
- Decision: one canonical output-row estimate per node set, computed multiplicatively (`rows(S1) × rows(S2) × Π selectivity(newly satisfied predicates)`), shared by all plans for that set. Nested-loop lookup probes from the LMDB estimator influence *cost* only.
  Rationale: row estimates that differ between join orders for the same set destabilize dynamic programming (MySQL warns about exactly this). Deriving edge selectivities once up front keeps every tree's estimate for a set identical.
  Date/Author: 2026-07-06 / Claude Code.
- Decision: v1 keeps a degenerate Pareto set (cheapest plan per node set) but routes all insertion through a `propose(...)` tournament with an explicit dominance function, so adding dimensions (ordering, parameterization, rescan cost — hypergraph-plan.md Milestones 2 and 6) is a local change.
  Rationale: with canonical rows per set, no stored parameterized paths, and no interesting-order tracking yet, all v1 dominance dimensions collapse to cost; building the frontier machinery now would be dead code that cannot be tested honestly.
  Date/Author: 2026-07-06 / Claude Code.
- Decision: the adapter (v1) only accepts islands where every factor is a plain `StatementPattern` (no property paths, no opaque factors with required-bound variables, no zero-variable factors), with 3..limit runtime factors forming a connected variable graph; anything else returns `Optional.empty()` and falls through to the existing planner unchanged.
  Rationale: those factor classes need hyperedge modelling (requires-bound = hyperedge; paths = endpoint rules) that is designed but not yet wired; declining keeps the flag safe to enable per-query while the existing planner remains the universal fallback. Future milestones extend coverage.
  Date/Author: 2026-07-06 / Claude Code.
- Decision: hook placement is inside `LmdbCascadesConnectedJoinPlanner.plan(...)` after factor canonicalization, before the template cache/dp/greedy branch; the DPhyp path bypasses the `PlanTemplate` cache in v1.
  Rationale: reuses ownership checks, factor flattening and canonicalization; the template cache encodes left-deep `State` objects and cannot represent bushy trees without rework (future milestone).
  Date/Author: 2026-07-06 / Claude Code.

## Outcomes & Retrospective

(To be written at milestone completion.)

## Context and Orientation

This repository is a fork of Eclipse RDF4J. The LMDB SAIL (`core/sail/lmdb`) contains a Cascades-style SPARQL optimizer. The pieces relevant here, all under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`:

- `LmdbCascadesConnectedJoinPlanner.java` — plans a "join island" (a maximal reorderable group of inner-join factors, produced by flattening nested `Join` nodes). Entry point:

      static Optional<Plan> plan(TupleExpr joinIsland, Set<String> initialBoundVars,
              JoinFactorCostModel costModel, EvaluationStatistics fallbackStatistics,
              EstimationTier estimationTier)

  It flattens factors via `LmdbJoinIslandConnectivity.flattenFactors(...)`, canonicalizes them, then runs `dpPlan` (exact DP over connected left-deep prefixes, ≤ 20 factors) or `greedyPlan`. The result record is `Plan(TupleExpr tupleExpr, CostVector cost, EstimateSnapshot estimate, int factorCount, String algorithm, int zeroVarFactorCount)` and is consumed by `LmdbConnectedHypergraphJoinImplementationRule` in `LmdbCascadesRuleProvider.java`.
- `LmdbJoinIslandConnectivity.java` — decides which islands the connected planner may own (`connectedJoinProviderCanOwn`), flattens factors, and knows which factor kinds are "opaque" (BIND, OPTIONAL, UNION, SERVICE, …) plus their required-bound variables (`opaqueRequiredVars`).
- `LmdbJoinPlanSupport.java` — variable utilities: `runtimeBindingNames(TupleExpr)` (unbound, non-`_const_` variables) and `plannerBindingNames(Set<String>)`.
- `JoinFactorCostModel` (in `core/queryevaluation/...optimizer/`) — the estimate API. `estimateFactorCost(factor, CostContext)` returns `FactorCostEstimate` with `getOutputRows()` (total rows after joining this factor onto the described prefix) and `getWorkRows()` (rows touched doing so). `CostContext.forOptimization(boundVars, prefixRows, nested, forOptimization, finiteBindingValues, prefixFactors)` describes the prefix.

Terms used below. A *node* is one join factor (v1: one SPARQL statement pattern). A *node set* is a `long` bitmask over node indexes 0..n-1. A *hyperedge* is a pair of disjoint non-empty node sets (left, right) meaning "a subplan containing all of left may be joined to a subplan containing all of right"; a *simple edge* has one node on each side. A *csg-cmp pair* is (connected subgraph, connected complement): two disjoint connected node sets joined by some hyperedge. The *SES* (syntactic eligibility set) of a predicate is the set of nodes it mentions; the *TES* (total eligibility set) is the set of nodes that must be present before applying it is semantically valid (for inner joins TES = SES). *DPhyp* enumerates every csg-cmp pair of a hypergraph exactly once, in an order where both halves of every pair have been emitted (and therefore planned) before the pair itself — which is exactly what dynamic programming over node sets needs.

## Plan of Work

Everything new lives in `core/sail/lmdb`. New main-source package `org.eclipse.rdf4j.sail.lmdb.hypergraph` with six classes, plus one adapter class in the parent package and a three-line hook in the existing planner. Every new file carries the standard EDL header (copy from `LmdbJoinIslandConnectivity.java`) followed by the agent signature line `// Some portions generated by Claude Code`.

Milestone A — core data structures. `NodeSets` (static `long` helpers: `bit`, `isSubset`, `overlaps`, `lowestBit`, `lowestIndex`, `nodesBelowInclusive`, iteration, `describe`). `Hypergraph`: growable node list; `addEdge(left,right)` validates disjoint non-empty sides and stores *two directed arcs* (even index = as-given, odd = swapped) exactly like MySQL, so any node on an edge can treat itself as being on the near side; per-node arc lists split into simple and complex, plus a cached `simpleNeighborhood` bitmask per node; `toDot(names)` renders simple edges as plain graph edges and complex hyperedges through a point-shaped helper node. Tests: `NodeSetsTest`, `HypergraphTest` in the mirrored test package.

Milestone B — the enumerator. `SubgraphEnumerator.enumerateConnectedPartitions(Hypergraph, Receiver)` with

      interface Receiver {
          boolean hasSeen(long nodeSet);
          boolean foundSingleNode(int nodeIdx);
          boolean foundSubgraphPair(long left, long right, int edgeIdx); // true aborts
      }

implementing DPhyp faithfully. In pseudocode (B(i) = all nodes with index ≤ i; neighborhood(S, X) = for every directed arc whose near side ⊆ S and far side disjoint from S ∪ X, take the lowest-index node of the far side as a representative):

      solve: for seed i = n-1 down to 0:
          foundSingleNode(i); emitCsg({i}); enumerateCsgRec({i}, B(i))
      emitCsg(S): X = S ∪ B(lowestIndex(S)); N = neighborhood(S, X)
          for i in N descending:
              if some edge connects S and {i}: foundSubgraphPair(S, {i}, edge)
              enumerateCmpRec(S, {i}, X ∪ (N ∩ B(i)))
      enumerateCsgRec(S, X): N = neighborhood(S, X)
          for each non-empty S' ⊆ N ascending: if hasSeen(S ∪ S'): emitCsg(S ∪ S')
          for each non-empty S' ⊆ N ascending: enumerateCsgRec(S ∪ S', X ∪ N)
      enumerateCmpRec(S, C, X): N = neighborhood(C, X)
          for each non-empty C' ⊆ N ascending:
              if hasSeen(C ∪ C') and some edge connects S and (C ∪ C'):
                  foundSubgraphPair(S, C ∪ C', edge)
          for each non-empty C' ⊆ N ascending: enumerateCmpRec(S, C ∪ C', X ∪ N)

"Some edge connects A and B" means an undirected edge with one side fully inside A and the other fully inside B. `hasSeen` doubles as the connectivity oracle: a set is connected exactly when the receiver has planned it, which the emission order guarantees. Tests (`SubgraphEnumeratorTest`): chain emits exactly {A}|{B}, {B}|{C}, {A,B}|{C}, {A}|{B,C} and never {A}|{C}; star; triangle cycle; hyperedge {A,B}—{C} emitted only once {A,B} exists; a `RecordingReceiver` asserts the DP invariant (both sides seen before every pair) and no duplicate unordered pairs; and a randomized cross-check against `NaiveEnumerator` (test-side DPsub oracle: iterate all masks ascending, split each connected mask into connected halves crossing an edge) over ~200 random graphs with mixed simple/hyper edges, comparing exact pair sets.

Milestone C — costing. `PlanHypergraph` wraps a `Hypergraph` with per-node `cardinality` and name, and a list of `JoinPredicate(long tes, double selectivity, String label)` (selectivities clamped into (0,1]); `Double.NaN` cardinalities are rejected. `JoinPlan` is the access-path analog: kind SCAN | HASH_JOIN | NESTED_LOOP, node set, canonical `rows`, `cost`, children, and for nested loops the lookup work per outer row. `CostModel` interface (scan cost, hash build/probe constants, lookup hook `lookupCost(node, boundNodes, outerRows)` returning NaN when no lookup source exists) with a `DefaultCostModel`. `CostingReceiver` implements `Receiver`: `foundSingleNode` proposes a scan with rows = node cardinality × Π selectivity of predicates whose TES is that single node; `foundSubgraphPair` computes the canonical row count for the union (once, cached per set), proposes hash join in both directions plus nested loop when the right side is a single node with a lookup source, and `propose(...)` keeps the non-dominated (v1: cheapest) plan per set. `HypergraphOptimizer.bestPlan(PlanHypergraph, CostModel)` runs the enumeration and returns the winning `JoinPlan` for the full set (empty when the graph is disconnected or a receiver budget aborts). Tests (`CostingReceiverTest`, `HypergraphOptimizerTest`): Milestone-1 acceptance from hypergraph-plan.md (chain considers both associations and picks by cost), bushy-beats-left-deep on a two-island chain of four nodes with the middle edge unselective, triangle cycle applies all three predicate selectivities exactly once (rows check), delayed hyperpredicate (TES spanning three nodes) applied at exactly the first join where its TES is satisfied, abort on budget.

Milestone D — LMDB adapter. `LmdbHypergraphJoinPlanner` (package `org.eclipse.rdf4j.sail.lmdb`), flag `rdf4j.optimizer.lmdb.cascades.connectedJoin.dphyp` (default false) and node cap `...connectedJoin.dphyp.maxFactors` (default 14). Entry:

      static Optional<LmdbCascadesConnectedJoinPlanner.Plan> tryPlan(List<TupleExpr> factors,
              Set<String> initialBoundVars, JoinFactorCostModel costModel,
              EvaluationStatistics fallbackStatistics, EstimationTier tier, Trace trace)

It verifies v1 scope (every factor a `StatementPattern`, 3..cap runtime factors, connected shared-variable graph, no factor already fully bound to zero runtime vars), builds nodes (base cardinality via `estimateFactorCost` with only `initialBoundVars` bound), adds one simple edge per factor pair sharing ≥1 runtime variable with a predicate whose selectivity is derived from a conditional probe (estimate factor j with factor i's variables bound and prefixRows = rows(i): selectivity = outputRows(j|i) / (rows(i) × rows(j)), clamped), maps `CostModel.lookupCost` onto per-outer-row conditional probes, runs `HypergraphOptimizer`, converts the winning `JoinPlan` into a (possibly bushy) tree of cloned factors under `Join` nodes, and wraps it into `Plan` with a `CostVector` and `EstimateSnapshot` mirroring the fields the left-deep `State.toPlan` fills, `algorithm = "dphyp-bushy"`. The hook in `LmdbCascadesConnectedJoinPlanner.plan(...)` tries the adapter first when the flag is on and falls back on `Optional.empty()`. Tests (`LmdbHypergraphJoinPlannerTest`): synthetic `JoinFactorCostModel` (deterministic per-pattern cardinalities); flag off → empty; non-StatementPattern factor → empty; disconnected island → empty; star island → plan containing every factor exactly once (verified by walking the tree); a stats shape where bushy wins → asserts the produced tree is not left-deep; flag-on plan and flag-off plan bind the same variable set.

Milestone E (future, not in this change): opaque factors as hyperedges from `opaqueRequiredVars`, property-path endpoint rules, `PlanTemplate`-style caching for bushy shapes, parameterized path storage, interesting orders, benchmark comparison via `ThemeQueryPlanRunBenchmark`, and a default-on decision.

## Concrete Steps

All commands run from the repository root. Use the mvnf skill for tests (it does a root `-Pquick` offline install first):

      python3 .codex/skills/mvnf/scripts/mvnf.py NodeSetsTest
      python3 .codex/skills/mvnf/scripts/mvnf.py HypergraphTest
      python3 .codex/skills/mvnf/scripts/mvnf.py SubgraphEnumeratorTest
      python3 .codex/skills/mvnf/scripts/mvnf.py CostingReceiverTest
      python3 .codex/skills/mvnf/scripts/mvnf.py HypergraphOptimizerTest
      python3 .codex/skills/mvnf/scripts/mvnf.py LmdbHypergraphJoinPlannerTest

Manual fallback (offline, workspace-local repo — required flags):

      mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest=SubgraphEnumeratorTest test

Never pass `-q` or `-am` when running tests. Evidence snippets come from `core/sail/lmdb/target/surefire-reports/`.

## Validation and Acceptance

Unit level: the six new test classes pass; `SubgraphEnumeratorTest#matchesNaiveOracleOnRandomGraphs` is the load-bearing one (exact csg-cmp parity with a brute-force oracle over random hypergraphs, plus DP-order and no-duplicate invariants on every emission). Costing level: `HypergraphOptimizerTest#bushyPlanWinsWhenIslandsAreSelective` fails if the enumerator or receiver silently degrades to left-deep. Integration level: `LmdbHypergraphJoinPlannerTest` proves the flag gate (off → planner behaves exactly as before; the hook only runs when `Boolean.getBoolean` sees the property) and plan well-formedness (all factors exactly once, same binding names). Regression level: run the module suite and compare failures against the known-red baseline recorded in the team memory (16 pre-existing failing classes on this branch, unrelated); no new failures attributable to this change with the flag off.

## Idempotence and Recovery

All changes are additive except the small hook in `LmdbCascadesConnectedJoinPlanner.plan(...)`, which is guarded by a default-off system property; reverting is deleting the new files and the hook lines. Tests mutate no global state other than temporarily setting the system property inside try/finally (same pattern as `LmdbCascadesOptimizerTest`). Re-running any milestone's steps is safe.

## Artifacts and Notes

Surefire evidence, Milestones A–C (2026-07-06 21:49Z, `mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -DskipITs -Dtest='NodeSetsTest,HypergraphTest,SubgraphEnumeratorTest,CostingReceiverTest,HypergraphOptimizerTest' verify`):

    Running org.eclipse.rdf4j.sail.lmdb.hypergraph.SubgraphEnumeratorTest
    Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.105 s
    Running org.eclipse.rdf4j.sail.lmdb.hypergraph.HypergraphTest
    Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
    Running org.eclipse.rdf4j.sail.lmdb.hypergraph.HypergraphOptimizerTest
    Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
    Running org.eclipse.rdf4j.sail.lmdb.hypergraph.NodeSetsTest
    Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
    Running org.eclipse.rdf4j.sail.lmdb.hypergraph.CostingReceiverTest
    Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
    Results: Tests run: 43, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

The only failure on the first compile was a test-code lambda capture (`iteration` not effectively final); the paper-faithful DPhyp transcription passed the 200-random-graph oracle parity test on its first execution.

Seed/extension estimate conventions mirrored from `dpPlan` for the adapter: seeds use `estimateStep(factors, i, initial, 0.0d, false, List.of(), ...)`; extensions use `estimateStep(factors, j, boundVars, prefixRows, true, prefixFactors, ...)`.

## Interfaces and Dependencies

No new external dependencies. End state (main sources, package `org.eclipse.rdf4j.sail.lmdb.hypergraph`):

      final class NodeSets            // static long-bitmask helpers
      final class Hypergraph          // nodes, duplicated directed arcs, simple/complex split, toDot
      final class SubgraphEnumerator  // static boolean enumerateConnectedPartitions(Hypergraph, Receiver)
          interface Receiver { boolean hasSeen(long); boolean foundSingleNode(int); boolean foundSubgraphPair(long,long,int); }
      final class PlanHypergraph      // nodes with cardinality, JoinPredicate(tes, selectivity, label)
      final class JoinPlan            // SCAN|HASH_JOIN|NESTED_LOOP, nodes, rows, cost, children
      interface CostModel             // scanCost, hashJoinCost pieces, lookupCost(node, boundNodes, outerRows)
      final class CostingReceiver     // DP map long -> best JoinPlan, propose(...) tournament
      final class HypergraphOptimizer // Optional<JoinPlan> bestPlan(PlanHypergraph, CostModel)

Package `org.eclipse.rdf4j.sail.lmdb`:

      final class LmdbHypergraphJoinPlanner   // flag-gated adapter producing LmdbCascadesConnectedJoinPlanner.Plan

Revision note (2026-07-06): initial version, written before implementation. Progress checkboxes and Artifacts must be filled in with real evidence as milestones land, never in advance.
