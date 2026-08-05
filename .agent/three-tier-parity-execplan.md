# Three-tier execution parity: make adjacency always beat LMDB cursors, and Janino kernels always beat both

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md` (repository root: `/Users/havardottestad/Documents/Programming/rdf4j`). It is self-contained: everything needed to execute it is in this file. Related but non-required context lives in `plans/lmdb-native-engine/09-joins.md` (join roadmap), `plans/lmdb-native-engine/30-adjacency-consumption-survey.md` (stale census, superseded), `.agent/lmdb-join-strategy-execplan.md` (completed join-stack plan), and `.agent/adjacency-consumption-execplan.md` (adjacency consumption plan, milestones 1–5 delivered).

## Purpose / Big Picture

The LMDB store on branch `optimize-lmdb` executes SPARQL through three stacked regimes. First, a pure LMDB-cursor regime: every operator reads B+-tree index cursors (`spoc`, `posc`, …) through `TripleStore`. Second, an adjacency regime: an in-memory "direct adjacency" store (four planes per predicate — {outgoing, incoming} × {explicit, inferred} — each plane holding sorted neighbor runs per key, a sorted key domain, and delta overlays for recent commits) is built asynchronously at store open and, once live, serves lookups without touching LMDB. Third, a Janino regime: hot query shapes are compiled at runtime (via the Janino in-process Java compiler) into fused loop kernels that read the adjacency planes through monomorphic call sites.

Today the three regimes have unequal algorithm coverage, so upgrading a regime can *lose* an optimization: kernels implement exactly one join algorithm (a fused nested-loop/probe chain) while the interpreter has ten; several adjacency capabilities (in-run binary search, context columns, O(1) edge-existence) have zero consumers; and whole operator families (OPTIONAL, MINUS, EXISTS, correlated entries) never consult adjacency at all.

After this plan, the user-visible outcome is a provable performance ordering on a fixed query corpus: for every benchmarked query, the adjacency regime is at least as fast as the LMDB-cursor regime (and normally faster), and the Janino regime is at least as fast as both (and normally faster). The proof is a three-way paired benchmark harness added in Milestone 0 that runs the same corpus in all three regimes and reports any "inverted cell" — a query where a higher tier is slower beyond noise. The plan is complete when the corpus has zero inverted cells and the capability work below is delivered.

Scope boundary set by the user (2026-08-04): implement missing algorithms, shortcuts, and optimizations for all three regimes. Do NOT spend this plan on dispatch-preference reordering or on flag-lifecycle work as goals in themselves. System-property kill switches remain the branch convention for safety, and a feature's default flips to ON when its harness cells prove the ordering, but gating machinery is not the deliverable.

## Progress

- [x] (2026-08-04) Three-regime capability audit completed (three parallel deep reads of the working tree; findings embedded in Context and Orientation below).
- [x] (2026-08-04) This ExecPlan authored; all file/line anchors verified against the working tree the same day. Line numbers WILL drift as work proceeds — re-verify with `rg` before editing.
- [x] (2026-08-04) Milestone 0 harness built: `ThreeTierParityBenchmark` (41 cells over three datasets, regime as a JMH `@Param`), `ThreeTierEngagementCensusTest` (per-regime engagement witness, green), `ThreeTierParityCorpus` / `ThreeTierRegime` / `ThreeTierParityFixtures` support classes, the test-scope bridge `AdjacencyEngagementTestAccess`, and `scripts/three-tier-report.py` with `scripts/test_three_tier_report.py` (10 tests, green). Nothing wired into production.
- [x] (2026-08-04) Milestone 0 baseline complete: generated-dataset matrix (35 cells, 9 clean captures, 0 inverted / 10 OK / 60 overlap) and theme matrix (6 cells, 9 captures, 2 inverted / 1 OK / 9 overlap) recorded under Artifacts, both engagement censuses green (3 tests). The work list Milestones 1 to 10 must close is: one proven inverted cell (`themeHighlyConnectedQ4`, inverted on both rungs), four suspected inversions where a tier engages and its mean worsens (`chainJoinOpen`, `optionalHeavy`, `minusShape`, `sequencePath`), 22 of 35 generated cells with no kernel at all, and three cells needing the long-iteration protocol before any verdict (`themeAnalyticsQ9`, `cycle5ValuesCountMailboxHomepage`, `cycle5ValuesDistinctMailboxOrdered`).
- [ ] Milestone 1: streaming adjacency frontiers with in-run galloping (`lowerBound`) and context columns (`copyContexts`).
- [ ] Milestone 2: adjacency-served semijoins — O(1) EXISTS edge checks, run-intersection semijoin, membership/left-join probe integration.
- [ ] Milestone 3: adjacency coverage residuals — context-bound root scans, object-order scans, exact-empty plan pruning, row-side factorized branch cache consult.
- [ ] Milestone 4: correlated-entry batching completeness (accumulate SEMI/ANTI/multi-key/conditions; estimate-triggered single-pattern OPTIONAL build; inner correlated fragments).
- [ ] Milestone 5: mark joins for EXISTS / NOT EXISTS / MINUS.
- [ ] Milestone 6: merge and hash join upgrades (multi-key merge, smaller-run buffering, ordering propagation, payload key exclusion, bushy builds).
- [ ] Milestone 7: kernel shape coverage — wire the dead IR primitives (range filter, computed BIND, context columns, in-kernel DISTINCT, in-kernel ORDER/LIMIT/OFFSET, HAVING).
- [ ] Milestone 8: kernel hash join (HashBuild/HashProbe IR nodes).
- [ ] Milestone 9: kernel worst-case-optimal intersection (wire the dead `Intersect` node; lower cyclic cores; in-kernel galloping).
- [ ] Milestone 10: kernel robustness — per-pattern adjacency/scan mixed binding, row-rung EXISTS, aggregate-rung residual filters.
- [ ] Milestone 11: final three-way corpus sweep, default flips for proven features, retrospective.

## Surprises & Discoveries

- Observation: (2026-08-04, audit) `NativeAdjacency.lowerBound` (binary-search seek inside a sorted neighbor run) is implemented and tested but has zero consumers in the evaluation engine; `copyContexts` has zero consumers anywhere. The leapfrog join copies runs into arrays and gallops over the copies instead.
  Evidence: declaration `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java:301` and `:289`; repo-wide `rg` shows only benchmark/test callers.
- Observation: (2026-08-04, audit) Six kernel IR primitives are fully emitted and unit-tested but unreachable from production lowering: `Intersect`, `FilterRangeUnsigned`, `BindHook`, `Having`, `Emit.distinct`, and all of `OutputMods` (ORDER/LIMIT/OFFSET). `EnumerateEntry` is near-dead (one witness site).
  Evidence: `LmdbNativeKernelIr.java:443/:536/:822/:1116/:966/:910`; zero construction sites in `LmdbNativeKernelLowering.java`; emitter support present (`LmdbNativeKernelEmitter.java:2362/:2137/:2338`); only `LmdbNativeKernelIrEmitterTest` exercises them.
- Observation: (2026-08-04, audit) The historical top kernel decline `agg:unsupported:JoinPlan` (39% of pairs) is FIXED — binary joins now descend (`LmdbNativeKernelLowering.java:703-707`) and `ExtensionPlan` lowers to `BindAlias`; census went 64 declining pairs → 10 (`plans/lmdb-native-engine/23-kernel-capability-parity.md:130`).
- Observation: (2026-08-04, audit) OPTIONAL, MINUS, EXISTS, and `PatternMembershipProbe` are entirely adjacency-blind above the transparent scan layer; row-side `LmdbNativeFactorizedRows.TailBranch` does not consult `adjacencyCacheBacked()` while its aggregate-side twin does (`LmdbNativeFactorizedTailBranch.java:193`).
- Observation: (2026-08-04, audit) The `plans/lmdb-native-engine/30-adjacency-consumption-survey.md` census table is stale: eight of its nine gaps were implemented and default-enabled on 2026-08-03/04 (root scans, doubly-bound probes, clean-txn reads, parallel row path, scan aggregates, planner stats, adjacency SIP key-domain masks, path seeds + bidirectional search). Only the `<s> ?p ?o` node edge dump under the paged-CSF base still declines.
- Observation: (2026-08-04, M0) The adjacency tier's coverage of the corpus is already essentially complete: 34 of the 35 generated-dataset cells engage the planes, and the single exception is exactly the `<s> ?p ?o` node edge dump the plan had already excluded. The kernel tier is the sparse one — only 13 of the 35 compile.
  Evidence: engagement census table, `logs/mvnf/20260804-130644-verify.log:245-281`; `nodeEdgeDump` is the only row reading `lmdb only`.
- Observation: (2026-08-04, M0) The whole cyclic family declines to the interpreted leapfrog: `cycle3`, `cycle4`, `cycle5`, `cycle3CountCityInterest`, `cycle3DistinctCityOrdered`, `cycle4ValuesFilteredOrdered`, `cycle5ValuesCountMailboxHomepage` and `cycle5ValuesDistinctMailboxOrdered` all show `kernelOpens=0` while consuming enormous numbers of adjacency views (cycle5: 124258 views per execution at census scale). This is Milestone 9's target, now measured rather than inferred. `cycle3GroupedInterest` is the one cyclic cell that does compile — through the aggregate rung — and its plane lookups collapse from 2946 to 4 when it does.
  Evidence: same census table; and a benchmark-scale confirmation, `### three-tier engagement: cell=cycle3 regime=janino adjacencyState=ACTIVE planes=8617302 kernelViews=8617302 kernelOpens=0`.
- Observation: (2026-08-04, M0) `JaninoPipelineTestAccess.openedAny()` is not in fact "any": it sums the Janino pipeline rung and the two general IR rungs but omits the legacy `LmdbNativeJaninoAggregate` rung. A census built on it alone reports grouped and counted shapes as declining when they actually compile. The census now adds `JoinDispatchTestAccess.janinoAggregateOpened()`.
  Evidence: `cycle3GroupedInterest` showed `jan:planes` collapsing 2946 → 4 with `jan:kernels 0` before the fix, and `jan:kernels 1` after; `JaninoPipelineTestAccess.openedAny()` body sums only `LmdbNativeJaninoPipeline.OPENED`, `KernelExecution.OPENED` and `AGG_OPENED`.
- Observation: (2026-08-04, M0) The baseline has **zero provable inverted cells** — the plan's Milestone 0 text predicted inversions today and there are none that survive the disjoint-interval rule. What it has instead is four cells where a higher tier *engages* and its mean is consistently worse, with the error bars still wide enough to hide it: `chainJoinOpen` (0.92 → 1.25 → 1.63 ms, 1104 kernel opens), `optionalHeavy` (7.30 → 9.72 → 11.24, 177 opens), `minusShape` (4.18 → 5.59 → 6.37, 292 opens) and `sequencePath` (adjacency wins, then the kernel gives it back: 2.22 → 2.62, 668 opens). These are the shapes to re-measure under the long-iteration protocol before and after Milestones 2, 7 and 10 — they are suspected inversions, not proven ones, and they line up exactly with the operator families those milestones target.
  Evidence: `scripts/three-tier-report.py` over the eleven captures listed under Artifacts; per-cell `### three-tier engagement` lines confirm `kernelOpens > 0` for all four.
- Observation: (2026-08-04, M0) **The baseline contains one proven inverted cell, and it inverts on both rungs**: theme HIGHLY_CONNECTED q4 ("Connected: nodes with weights 1 or 2 and edges"). LMDB 108.118 ± 14.998 ms, adjacency 176.120 ± 28.253 ms, janino 479.141 ± 46.509 ms — the intervals are disjoint in both comparisons, so this is not a noise artefact. Both higher tiers engage: adjacency performs about 3.68 million plane lookups per iteration where the LMDB regime does the same work with cursor scans and no adjacency at all, and the kernel (6 opens, ~2 million plane lookups) is 4.4x slower than LMDB and 2.7x slower than the interpreted adjacency path. The shape is a low-selectivity weight predicate joined to edges, which is exactly the case where a per-row lookup loop should lose to one bulk sequential scan.
  Evidence: theme matrix under Artifacts; `### three-tier engagement: cell=themeHighlyConnectedQ4 regime=adjacency adjacencyState=ACTIVE planes=3683550 kernelViews=0 kernelOpens=0` versus `regime=lmdb adjacencyState=ABSENT planes=0`, and `regime=janino ... planes=1983446 kernelViews=14 kernelOpens=6`.
  Open question for the user, because the two candidate fixes fall on opposite sides of this plan's scope boundary: read as a *capability* gap, the adjacency tier lacks a bulk plane-scan read path for low-selectivity predicates and keeps degenerating into a per-key lookup loop — that is Milestone 1 and Milestone 3 territory and in scope. Read as a *dispatch* problem, the tier simply should not engage on this shape, and the Decision Log rules dispatch-preference work out of scope by explicit user instruction. The plan's own rule ("where a capability gap forces a bad dispatch outcome, the fix is the capability, not the ladder") points at the first reading, so Milestones 1 and 3 should target this cell; but if the bulk-read capability lands and the tier still engages and loses, closing it will need the admission decision that is currently out of scope.

- Observation: (2026-08-04, M0) Two of the six theme representatives get zero adjacency engagement in either regime: ANALYTICS q5 ("literal datatype histogram") and ANALYTICS q9 ("high in-degree objects"). Adjacency was demonstrably live in the same pass — `themeHighlyConnectedQ1` recorded 655338 plane lookups — so these are genuine declines, not a cold index. Both are unbound-predicate whole-store aggregates, and the planes are per-predicate, so there is no single plane to open; q5 additionally needs literal datatype inspection, which the id-level planes do not carry. This is a gap the plan's Milestone 1 to 3 list does not cover (it covers context-bound root scans, object-order scans and exact-empty pruning). It is the same family as the excluded `<s> ?p ?o` node edge dump, one scale up, and should be judged the same way: a decline is equality, so it only becomes a problem if the tier engages and loses.
  Evidence: theme census table, `logs/mvnf/20260804-143057-verify.log:245-252`.
- Observation: (2026-08-04, M0) The engagement witness does the job it was built for on `cycle5`: its mean rises 87.4 → 92.8 → 98.7 ms up the ladder, which reads like a kernel regression, but `kernelOpens=0` with 22.5 million adjacency views proves the kernel never engaged, so the janino and adjacency columns are the *same execution path* and the spread is pure noise. Without the witness this cell would have been filed as an inversion.
  Evidence: `### three-tier engagement: cell=cycle5 regime=janino adjacencyState=ACTIVE planes=22565884 kernelViews=22565884 kernelOpens=0`.
- Observation: (2026-08-04, M0) A JVM-scoped fixture in a forked JMH sweep leaks its temporary store: JMH forks one JVM per cell and nothing runs a trial teardown that owns a JVM-scoped resource, so the first baseline attempt stranded a 32 MB LMDB directory per cell — 29 directories and 806 MB before it was noticed, on the way to roughly 10 GB for the full sweep, on a machine with 41 GB free. `ThreeTierParityFixtures` now installs a JVM shutdown hook. Any later milestone that adds a JVM-scoped fixture needs the same hook.
  Evidence: `ls -d $TMPDIR/rdf4j-lmdb-three-tier-*` returned 29 directories mid-sweep and `du -shc` 806 MB; after the fix a full census run leaves zero.
- Observation: (2026-08-04, M0) JMH stops forwarding a forked JVM's stdout before `@TearDown(Level.Trial)` runs, so a once-per-trial engagement summary never reaches the result file. The benchmark reports the witness per measured iteration instead, which also makes it a steady-state delta rather than a total inflated by preparation.
  Evidence: a trial-teardown print produced zero matches in the captured result file; the same print at `Level.Iteration` appears in it.

## Decision Log

- Decision: The plan's acceptance metric is the "no inverted cell" rule on a fixed three-regime corpus, judged by disjoint confidence intervals over paired runs (never single scores), with the noise-floor rule for multi-second queries (longer iterations before judging).
  Rationale: the user's goal is an ordering ("adjacency basically always faster than lmdb, janino always faster than both"); orderings are only provable per-query against noise, and this branch has repeatedly been burned by 3×2s error bars.
  Date/Author: 2026-08-04 / Claude (user goal statement).
- Decision: "Always faster" is operationalized as "never slower beyond noise, on every corpus cell, and faster on the cells the tier claims to serve". A higher tier may DECLINE a shape (fall back to the tier below); a decline yields equality, which satisfies the ordering. What is forbidden is a higher tier *engaging* and losing.
  Rationale: literal universal dominance is unachievable (e.g. a kernel cannot beat an O(1) interpreted answer such as the whole-plane COUNT); declining to the better tier preserves the user's ordering without perverse re-implementations.
  Date/Author: 2026-08-04 / Claude.
- Decision: Dispatch-preference/ladder reordering and admission-signal redesign are OUT OF SCOPE, per explicit user instruction ("Focus on this instead of on gating features or changing preference"). Where a capability gap currently *forces* a bad dispatch outcome (e.g. kernels lacking hash join), the fix is the capability, not the ladder.
  Rationale: user instruction 2026-08-04.
  Date/Author: 2026-08-04 / Claude.
- Decision: Each milestone still ships behind a per-milestone system property defaulting OFF during development, flipped ON when its harness cells prove parity-or-better (the user's 2026-08-03 parity-or-better gate from the adjacency consumption plan carries over). This is safety convention, not a plan focus.
  Rationale: branch convention (`rdf4j.lmdb.wcoj.directFrontiers.enabled` lifecycle); trivially revertible steps.
  Date/Author: 2026-08-04 / Claude.
- Decision: The `<s> ?p ?o` node edge dump under the paged-CSF base stays EXCLUDED (it declines to LMDB, which is equality, not inversion). It was explicitly excluded by the user in the adjacency consumption plan; nothing in the new goal requires it.
  Rationale: prior explicit user exclusion; decline satisfies the ordering rule.
  Date/Author: 2026-08-04 / Claude.
- Decision: The three regimes are a strict ladder, each adding one tier: `lmdb` = adjacency `DISABLED` plus code generation off; `adjacency` = adjacency `PREFER` plus code generation off; `janino` = adjacency `PREFER` plus code generation on. The plan's Context described the LMDB regime only as "adjacency DISABLED"; leaving code generation on there would have measured "cursors plus kernels", which is not a tier of the ladder and would have made the bottom row of the matrix uninterpretable.
  Rationale: the ordering claim is about tier contribution, so each regime must differ from its neighbour by exactly one tier.
  Date/Author: 2026-08-04 / Claude (Milestone 0).
- Decision: The Janino regime sets `rdf4j.lmdb.janinoCodegen.thresholdRows=0` and the harness awaits compilation before measuring, instead of relying on warm-up executions to cross the default 32768-interpreted-row admission threshold. Small corpus cells (point lookups, doubly-bound probes) never reach that threshold at any warm-up count, so without this the "janino" column would silently have measured interpreted execution for a large part of the corpus.
  Rationale: a regime column must measure the regime it is named after; deterministic admission beats hoping a cell is big enough.
  Date/Author: 2026-08-04 / Claude (Milestone 0).
- Decision: One JVM measures exactly one regime, enforced at runtime by `ThreeTierRegime.pin`. Compiled kernels and built adjacency planes outlive a system-property flip, so a second regime in the same JVM would inherit the first one's state.
  Rationale: makes the most damaging possible harness bug — silently comparing a regime against itself — impossible rather than merely unlikely.
  Date/Author: 2026-08-04 / Claude (Milestone 0).
- Decision: "The 19 `AdjacencyQueryShapeBenchmark` shapes" in this plan's Milestone 0 text is resolved as the union of that benchmark's 13 `@Benchmark` methods and the six census-only shapes from `LmdbAdjacencyUsageCensusTest` that add coverage (star join, open chain join, object-object join, VALUES batch lookup, ordered neighbours, all-predicates). The plan conflated the benchmark's method count with the census's 19 scenarios; the union is what "19 shapes" was reaching for, and the two census scenarios left out (`exists_semijoin`, `triangle_wcoj`) are already covered by the corpus's own EXISTS and cycle3 cells.
  Rationale: resolves the ambiguity in favour of coverage without duplicating cells.
  Date/Author: 2026-08-04 / Claude (Milestone 0).
- Decision: The theme cells reuse the shared store at `core/sail/lmdb/target/lmdb-theme-query-benchmark/complete` read-only, behind a fingerprint check, with no rebuild path at all: a mismatch throws and tells the user to run `ThemeQueryBenchmark`. The theme half of the engagement census is opt-in behind `rdf4j.lmdb.threeTierParity.themeCensus.enabled`.
  Rationale: that store is several gigabytes, is shared with the theme regression suites, and is deliberately protected from `mvn clean`; a harness that could silently reload it would be both slow and destructive. The census half stays opt-in because the store must exist and match its fingerprint, and a default test run must not fail on a machine that has never loaded it — not because of the clock: the measured cost of the whole three-regime theme census is about 30 seconds, which also retired the drafting assumption that a theme adjacency build costs minutes and let the theme benchmark sweep keep one JVM per cell.
  Date/Author: 2026-08-04 / Claude (Milestone 0).
- Decision: Milestone 0's corpus lives in one enum (`ThreeTierParityCorpus`) shared by the benchmark and the census, and the chain-shape data generation is duplicated from `AdjacencyQueryShapeBenchmark` rather than extracted into a shared helper.
  Rationale: one corpus definition means a JMH row and a census row for the same shape are the same query by construction. Duplicating twenty lines of fixture generation keeps the harness self-contained and leaves a benchmark the user is actively running untouched.
  Date/Author: 2026-08-04 / Claude (Milestone 0).
- Decision: Milestone order is harness → adjacency completions → interpreted completeness → kernel parity. Interpreted-side improvements (M4–M6) land BEFORE the kernel milestones that must beat them (M7–M10), so the kernel work is measured against its true target.
  Rationale: "Janino always faster than both" is only meaningful against the finished interpreter; building kernels first would validate them against a target that then moves.
  Date/Author: 2026-08-04 / Claude.

## Outcomes & Retrospective

### Milestone 0 (2026-08-04)

What exists now that did not before: a single command per regime produces a per-query three-column matrix with error bars, a second command turns any set of those captures into verdicts under the disjoint-interval rule, and a test says which tier actually served each query. The plan can now be argued with evidence instead of inference.

What the instrument found, which is the point of building it before doing any optimisation work:

The adjacency tier is nearly complete on this corpus — 34 of 35 generated cells engage the planes — so Milestones 1 to 3 are not about coverage but about the quality of what is already being served. The kernel tier is the sparse one at 13 of 35, and the biggest single block is the cyclic family, which confirms Milestone 9's premise outright.

The plan predicted inversions in the generated corpus and there are none that survive the noise rule. The inversion is in the theme corpus instead, and it is severe: `themeHighlyConnectedQ4` is 1.6x slower under adjacency and 4.4x slower under Janino than under plain LMDB cursors, with disjoint intervals on both comparisons and engagement counters proving both tiers engaged. One cell like this is worth more than the 60 overlapping cells combined, because it is the exact failure the plan exists to eliminate — and it raises a scope question recorded in Surprises, since the obvious fix is admission rather than capability.

Four more cells (`chainJoinOpen`, `optionalHeavy`, `minusShape`, `sequencePath`) show a tier engaging with a consistently worse mean, still inside the error bars. They are not yet inversions and must not be reported as such, but all four sit in the operator families Milestones 2, 7 and 10 target, so they are the natural before-and-after cells for that work.

What the retrospective should carry forward as method, not result: the engagement witness paid for itself immediately by preventing a false positive (`cycle5` looks like a 13% kernel regression until `kernelOpens=0` proves both upper columns are the same code path) and by catching a false negative in the harness itself (the kernel counter omitted the legacy aggregate rung, hiding compiled grouped shapes). Two process failures are also worth carrying: a JVM-scoped JMH fixture leaks a store per forked cell unless a shutdown hook owns it, and running any compile during a measurement invalidates that capture — one capture had to be discarded and replaced for exactly that reason.

What is not done: the plan's noise floor is still the binding constraint on 60 of 70 generated comparisons and on 9 of 12 theme comparisons. Before Milestone 1 claims anything, the long-iteration protocol needs to be defined concretely for the three cells whose error bars currently exceed any plausible effect (`themeAnalyticsQ9`, `cycle5ValuesCountMailboxHomepage`, `cycle5ValuesDistinctMailboxOrdered`), or those cells cannot be used as gates at all.

## Context and Orientation

Everything below is on branch `optimize-lmdb`; the working tree carries substantial uncommitted work — never revert files you did not change. "The native engine" is the code under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`, which executes SPARQL basic graph pattern fragments directly over 64-bit LMDB value ids in primitive batches, bypassing the generic iterator machinery in `core/queryalgebra/evaluation`. All class names below live in that package unless another path is given.

Terms used throughout:

*Plane*: one in-memory adjacency table for one predicate and one direction. "Outgoing" is keyed by subject with object neighbors; "incoming" is keyed by object with subject neighbors. Each key maps to a *run*: a sorted array of (neighbor, context) pairs. Each plane also has a sorted *key domain* (every key present) and *delta generations* (overlay structures applied for commits after the base build). The implementation is `org.eclipse.rdf4j.sail.lmdb.LmdbDirectAdjacencyStore` (same module, parent package) plus `LmdbInMemoryAdjacencyIndex`, with the default storage being a paged compressed-static-function ("CSF") base.

*Probe*: `NativeProbe`, the per-row lookup surface a plan uses to open pattern scans (`NativeLmdbQuerySource.java`). `probe.adjacency(predicate, bySubject)` returns a `NativeAdjacency` view of one plane, or null when ineligible. `NativeAdjacency` exposes `find(key)`, `size(run)`, `neighborAt`, `contextAt`, `copyNeighbors`, `copyContexts`, `lowerBound(run, from, target)` (binary search within a run), `runsNeighborOrdered()`, `supportsKeyEnumeration()/keyCount()/keyAt(i)`.

*Row ladder*: the strategy cascade for streaming SELECT fragments, `LmdbNativeRowStep.openUnorderedInput` (`LmdbNativeRowStep.java:869-937`): WCOJ leapfrog → Janino/IR kernel → arbiter (batch merge/hash vs parallel pipelines) → factorized rows → adaptive filter placement → nested-loop chain. *Aggregate ladder*: `LmdbNativeGroupStep.evaluateInitialized` (`:283-457`): exists-intersection → legacy Janino aggregate → IR aggregate → WCOJ count → prefix-run groups → factorized tail vs parallel aggregation → ordered-distinct → sequential.

*Kernel*: a runtime-compiled class. Lowering (`LmdbNativeKernelLowering`) turns a `SlotPlan` tree into kernel IR nodes (`LmdbNativeKernelIr`); the emitter (`LmdbNativeKernelEmitter`) prints Java source; `LmdbNativeJaninoCodegen` compiles and caches it (async, off-thread; the triggering query runs interpreted). Kernels bind adjacency views per request via `LmdbNativeKernelBindings.requestAdjacencies` — currently all-or-nothing with one retry preferring LMDB scans (`ScanQuad` nodes).

Current capability matrix (verified 2026-08-04). Interpreted inner joins: binary/N-ary nested loop with uncorrelated-right replay (`LmdbNativeJoinPlans`), symmetric merge join (single key only, `LmdbNativeMergeJoin`), primitive hash join (2-pattern, `LmdbNativeHashJoin`), WCOJ leapfrog with adjacency frontiers and key enumeration (`LmdbNativeLeapfrogJoin`), chunk pipeline with zig-zag merge probing, per-key memos, scan-once hash flip, and SIP masks (`LmdbNativeChunkPipeline`), factorized rows/tail, parallel pipelines, prefix runs, exists-intersection. OPTIONAL: one nested-loop operator with three lazy accelerators (`LmdbNativeLeftJoinPlans`, `LmdbNativeLeftJoinPayloadProbe`, `LmdbNativeLeftJoinMemo`); no merge/WCOJ/factorization/SIP. MINUS/EXISTS: membership probes and per-row filters (`LmdbNativeRowPlans.MinusPlan`, `LmdbNativeFilters`, `LmdbNativeMembership`); EXISTS becomes a real join edge only inside the leapfrog. Correlated entries (an outer row stream re-opening a native fragment per row) are vetoed by every bulk strategy; the two counters — `LmdbNativeAccumulateJoin` (LEFT/INNER only, one shared var, no condition) and the left-join sweep in `LmdbNativeLeftJoinMemo` — exist but default off. Kernels: fused nested-loop/probe chain only, plus LeftProbe/LeftGroup, Exists (aggregate rung only), Union, PathExpand, vector tail, resumable emission; no hash, no merge, no intersection, no galloping, no context columns, no range pushdown.

Test/benchmark infrastructure you will reuse: `python3 .codex/skills/mvnf/scripts/mvnf.py <selection>` runs tests (never use `-am` or `-q` with tests); `scripts/run-single-benchmark.sh` runs one JMH benchmark (always anchor method regexes, e.g. `--method 'cycle3$'`); harnesses `LmdbAdjacencyUsageCensusTest` (19 Kuzu-parity shapes with engagement counters), `FoafCliqueDispatchCensusTest` + `JoinDispatchTestAccess` (dispatch census), `AdjacencyQueryShapeBenchmark` (shape workloads over a 5000-person FOAF store), `FoafCliqueQueryBenchmark` (cyclic shapes), `HashJoinBenchmark`, `CorrelatedBenchmark`, `ThemeQueryBenchmark` (the theme corpus; its ~2.2 GB store under `core/sail/lmdb/target` is protected from `mvn clean`). JMH noise rule: judge deltas only by disjoint intervals across paired runs; multi-hundred-ms queries need 5×10s-class iterations.

Regime switches for the harness: LMDB-cursor regime = store config `DirectAdjacencyMode.DISABLED` (or `-Drdf4j.lmdb.directAdjacency.*` kill switches); adjacency regime = default config with `-Drdf4j.lmdb.janinoCodegen.enabled=false`; Janino regime = defaults (and for benchmarks, enough warmup opens to pass the opens×4096 ≥ 32768 admission so kernels actually engage — 8 opens of a shape, plus async compile latency, so warmup iterations must exceed that).

Build discipline (repository rules): before any test run, `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install` at the root (output to `maven-build.log`); `mvnf` performs this itself. Format with `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`. New Java files carry the standard 2026 Eclipse RDF4J header plus the line `// Some portions generated by Claude` immediately below it; run `cd scripts && ./checkCopyrightPresent.sh` after creating files. Behavior-changing steps inside milestones follow red→green (smallest failing test first) even though the plan itself is Routine D.

## Plan of Work

The work is four phases. Phase 0 builds the measuring instrument. Phase A (M1–M3) finishes the adjacency tier so that any query the structures can serve is served, and served well. Phase B (M4–M6) finishes the interpreted algorithm set — this raises the bar the kernels must beat, which is why it comes first. Phase C (M7–M10) gives kernels the same algorithmic arsenal so that a compiled shape is never algorithmically inferior to its interpreted execution. M11 closes with the corpus sweep and default flips.

### Milestone 0 — Three-regime paired harness and baseline matrix

Goal: one command produces, for a fixed corpus, a three-column matrix (lmdb / adjacency / janino) of scores with error bars, plus engagement counters proving which tier actually served each query, plus an "inverted cells" list. This is the plan's acceptance instrument and also the baseline evidence.

Work: add `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThreeTierParityBenchmark.java` (JMH) whose corpus is the union of: the 19 `AdjacencyQueryShapeBenchmark` shapes, the `FoafCliqueQueryBenchmark` cyclic catalog, `CorrelatedBenchmark` shapes, one OPTIONAL-heavy shape, one MINUS shape, one EXISTS shape, one property-path shape, and roughly six theme-corpus representatives chosen from the slowest ANALYTICS/HIGHLY_CONNECTED/ENGINEERING cells (pick by reading the newest `results-*.md` under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/`). Parameterize the regime as a JMH `@Param({"lmdb","adjacency","janino"})` that sets the store config and system properties in `@Setup` (regime switches described in Context). Add a companion census test `ThreeTierEngagementCensusTest` that runs each corpus query once per regime and asserts the *engagement witness*: in the lmdb regime zero adjacency hits; in the adjacency regime a nonzero adjacency counter for served shapes; in the janino regime a kernel activation (via `LmdbNativeRuntimePlan`/`LmdbNativeAttemptMetrics` or `JoinDispatchTestAccess`) for shapes the kernel claims. Add a small report script `scripts/three-tier-report.py` that parses two or more JMH result files and prints the matrix with per-cell verdicts (`OK`, `INVERTED`, `OVERLAP`) using the disjoint-interval rule. Wire nothing into production.

Acceptance: `python3 .codex/skills/mvnf/scripts/mvnf.py ThreeTierEngagementCensusTest` passes; three paired baseline runs of the benchmark complete and `scripts/three-tier-report.py` prints the matrix; the baseline inverted/overlap cells are recorded in this plan under Artifacts (they are the work list — expect inversions today, e.g. kernel-engaged single scans and adjacency-declined context-bound scans).

As delivered (2026-08-04), the harness is five test-scope classes plus a script. `ThreeTierParityCorpus` is an enum of the 41 cells; each constant names its dataset and carries its query, and its name matches the `@Benchmark` method that measures it, so a JMH row and a census row for one shape are the same query by construction. `ThreeTierRegime` is the three-regime ladder (see Decision Log) and owns the system-property switching plus the one-regime-per-JVM pin. `ThreeTierParityFixtures` builds and JVM-caches the three stores — the 5000-person FOAF clique store with the selective/dense chain shapes, the correlated-entry store, and the shared theme store opened read-only behind a fingerprint check — and knows how to prepare a cell for measurement (in the Janino regime: execute, await compilation, execute again). `ThreeTierParityBenchmark` is one `@Benchmark` per cell over three dataset-specific `@State` subclasses, so a filtered run only opens the stores it needs; it resolves its own cell from `BenchmarkParams` in `@Setup` so preparation never lands inside a measured iteration, and prints the engagement witness per measured iteration. `ThreeTierEngagementCensusTest` runs the whole corpus once per regime and asserts cross-regime row-count parity, that the LMDB regime has no adjacency index and no adjacency engagement at all, and that the cells recorded as adjacency-served or kernel-served still are. `AdjacencyEngagementTestAccess` (package `org.eclipse.rdf4j.sail.lmdb`, test scope) is the bridge that makes the package-private adjacency metrics readable from the benchmark package.

### Milestone 1 — Streaming adjacency frontiers: in-run galloping and context columns

Goal: eliminate the copy-then-intersect pattern in the leapfrog and give every adjacency consumer seek-in-place and context access, so adjacency-served intersections do strictly less work than their LMDB equivalents.

Work, in order. First, in `LmdbNativeLeapfrogJoin`, introduce a frontier view abstraction that wraps a `NativeAdjacency` run directly (fields: the adjacency view, run handle, start, length) and implements the current frontier contract (sorted distinct iteration, duplicate counts on demand, galloping via `adjacency.lowerBound(run, from, target)`) without materializing a `long[]` copy — `copyNeighbors` remains the fallback when counts-per-value are required and the run has duplicate neighbors across contexts. The intersection loop (`Level.enter`, currently galloping over materialized arrays via the private `gallopTo`) gains a code path that gallops directly on run views. Keep the materialized path; choose per member by a simple rule (view path when `runsNeighborOrdered()` and no counts needed; materialize otherwise). Kill switch `rdf4j.lmdb.wcoj.streamingFrontiers.enabled`. Second, wire `copyContexts`: the path plan (`LmdbNativePathPlan.expandCachedLevel`, which currently reads `contextAt` per neighbor) and any frontier consumer that needs contexts should bulk-copy contexts alongside neighbors; add `copyContexts` use where a per-element `contextAt` loop exists today. Third, fix the stale javadoc at `LmdbNativeLeapfrogJoin.java:53-58` (claims direct frontiers default off; they default on).

Acceptance: leapfrog suite (`LmdbNativeLeapfrogJoinTest`, `LmdbWcojDirectFrontierTest`) green with the flag on and off; parity corpus `FoafCliqueCorrectnessTest` 27/27; paired `cycle3`/`cycle5long`-protocol runs show the flag-on side at parity or better with fewer allocations (verify via `-prof gc` allocation rate, not just time); no inverted cells introduced in the M0 matrix rows that leapfrog serves.

### Milestone 2 — Adjacency-served semijoins: EXISTS, MINUS, membership, left-join probes

Goal: the operator families that today never consult adjacency get the O(1)/O(log n) answers the planes already hold, so EXISTS/MINUS/OPTIONAL-heavy queries speed up the moment the structures are live.

Work. First, edge-existence: `StatementPatternExistsFilter` and `ExistsFilter` (`LmdbNativeFilters.java:296-555`) and `MinusCursor`'s `PatternMembershipProbe` path get a pre-step that answers a fully-keyed single-pattern existence test from `source.tryHas(...)` (already exact, `LmdbSailStore.java:4491`) before building any memo or membership set; a partially-keyed test with one bound endpoint and constant predicate answers from `adjacency.find(key)` (run present = candidate; `lowerBound` for the doubly-bound case). Second, run-intersection semijoin: for `FILTER EXISTS { ?s :p ?x }`-style filters sharing exactly one variable with a pattern whose run is also adjacency-served, compute the verdict by galloping intersection of the two sorted runs (reuse the M1 frontier views; this is the consumption-survey "intersection-based semijoins" capability). Implement as a new probe inside `LmdbNativeMembership` (`AdjacencyIntersectionProbe`) chosen when both sides expose ordered runs; decline otherwise. Third, `PatternPayloadProbe` and `RightMemoProbe` (OPTIONAL accelerators) consult `probe.adjacencyCacheBacked()` exactly as the chunk pipeline does (`LmdbNativeChunkPipeline.java:1121-1127`): when the probe is cache-backed, skip building the duplicate query-local hash/memo layers and let per-key probes hit the shared cache. Kill switch `rdf4j.lmdb.adjacencySemijoin.enabled` covering the new probes.

Acceptance: new focused tests (red first) in a `LmdbAdjacencySemijoinTest`: EXISTS verdicts and MINUS results identical to the generic evaluator across bound/unbound/context-scoped cases, with census counters proving adjacency service; `LmdbAdjacencyUsageCensusTest` EXISTS row flips from `probes YES, leading scan NO` to fully served; paired runs of the M0 EXISTS/MINUS/OPTIONAL cells show adjacency ≥ lmdb with no inversion.

### Milestone 3 — Adjacency coverage residuals and exact-empty pruning

Goal: remove the remaining shapes where the adjacency tier silently equals LMDB instead of beating it.

Work. First, context-bound root scans: `openRootScan` (`LmdbDirectAdjacencyStore.java`, decline at the `context >= 0` check) learns to serve a bound-context full-predicate scan by walking key domain × run and filtering on the context column during the walk (runs are (neighbor, context) ordered, so a context filter is a scan-with-skip; measure — if the filtered walk loses to LMDB's `cspo`-style cursor on low-selectivity contexts, keep the decline for those by a cheap plane-statistics test rather than unconditionally). Second, object-order scans: a root scan requested in O order is served from the incoming plane's key domain (object-keyed) the way S order is served from the outgoing plane; wire the order check that currently declines non-S orders. Third, exact-empty plan pruning: at plan time (in `LmdbNativePatternPlan.estimate` / the join planner's zero-row checks), a bound endpoint whose adjacency lookup returns the exact `NOT_FOUND` proof reports estimate zero, letting existing empty-plan short-circuits kill the subtree before execution; add the consult behind `rdf4j.lmdb.directAdjacency.exactEmptyPruning.enabled`. Fourth, the one-line parity fix: `LmdbNativeFactorizedRows.TailBranch` consults `adjacencyCacheBacked()` like its aggregate twin (`LmdbNativeFactorizedTailBranch.java:193`) and skips its value memo when cache-backed.

Acceptance: census rows for context-scoped scans and O-ordered scans show adjacency service; a red→green test proves a query with an empty bound endpoint executes zero probes after pruning; M0 matrix rows for these shapes flip to `OK`; full `core/sail/lmdb` module verify green (known pre-existing failures excepted — compare against the module baseline first).

### Milestone 4 — Correlated-entry batching completeness

Goal: correlated entries (the universal veto that today forces per-outer-row nested loops in every regime) get bulk execution in the general case, not just the LEFT/INNER single-key no-condition case.

Work, extending `LmdbNativeAccumulateJoin` and its SPI `BatchCorrelatedJoinProvider` (`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/evaluationsteps/BatchCorrelatedJoinProvider.java`): add SEMI and ANTI modes (EXISTS / NOT EXISTS decorrelation — accumulate outer, one fragment sweep, mark-probe; consult point is the EXISTS filter seams in `LmdbNativeFilters` and the generic `JoinQueryEvaluationStep` ladder where the SPI is already wired); add multi-key support (up to 4 shared variables, matching `PrimitiveTupleTable` widths); add join-condition support by evaluating the condition per matched pair through the existing condition-evaluator hook in `BatchCorrelationRequest`. Then close the two remaining native-side floors named in `.agent/lmdb-join-strategy-execplan.md` M3-remaining: the single-pattern `PatternPayloadProbe` 1024-probe floor gains the same estimate trigger the sweep uses (build immediately when `expectedProbes × (SEEK_COST + perProbeRows) ≥ sweepEstimate`), and inner `JoinCursor` correlated fragments get the same estimate-triggered key-unbound sweep that `RightMemoProbe` has. Add a materialization-light acceptance workload (COUNT over the correlated OPTIONAL) to `CorrelatedBenchmark` so the time gate is not Amdahl-capped by result materialization (the 2026-07-31 sessions proved `outerAccumulate` itself cannot show more than ~1.2×).

Acceptance: accumulate suite extended (SEMI/ANTI/multi-key/condition parity vs the generic evaluator, bag semantics, budget-refusal fallbacks); `LEFTJOIN_SWEEP_BUILDS`-style counters prove single fragment execution; the new COUNT workload shows ≥1.5× with the features on; the M0 correlated cells show all three regimes improving and no inversion.

### Milestone 5 — Mark joins for EXISTS / NOT EXISTS / MINUS

Goal: replace per-row re-evaluation and boxed anti-joins with build-once mark semantics (plan-09 work item 2, embedded here so this plan stays self-contained).

Work: a multi-key primitive mark table on `LmdbNativePrimitiveTupleTable` (presence only, widths 1–4). For `FILTER EXISTS`/`NOT EXISTS` whose subquery is a single pattern: build the key set once per evaluation via the raw-unbinding sweep (`openRawUnbinding`) plus the per-quad default-graph filter from `PatternPlan.bind`, and drop the dataset/GRAPH-scope disable in `LmdbNativeMembership` (the two mechanisms together reproduce scoped semantics — this is the verified fix shape from plan 09; `StatementPatternExistsFilter` holds raw `Terms` and needs its own parallel path). For richer uncorrelated EXISTS subqueries: compile the subplan, sweep once (read-set test via `memoReadMask`), mark-join; correlated subqueries route through M4's SEMI/ANTI accumulate. Retained MINUS: hash anti-join on the shared-variable key set with MINUS's disjoint-domain semantics (empty shared set ⇒ no removal); the boxed nested loop remains only for truly incompatible cases. Where M2's adjacency semijoin already serves a shape, the mark join is the fallback tier below it — same code path, different key-set source.

Acceptance: EXISTS/MINUS corpus incl. dataset/GRAPH scoping, unbound-shared-variable edges, bag-cardinality preservation (marks must not dedup the outer side); a 1M-outer `FILTER NOT EXISTS` workload moves from per-row probes to one sweep + O(1) probes (probe counters asserted); MINUS benchmark shape ≥3× in the lmdb regime and no inversion in the other two.

### Milestone 6 — Merge and hash join upgrades

Goal: the two batch joins stop leaving known performance on the table in all regimes.

Work. Merge join (`LmdbNativeMergeJoin`): buffer whichever equal-key run terminates first instead of always the right (stream both up to `maxRunRows`, keep the completed one, stream the other), eliminating `openRescan` whenever either run fits; lift the single-key gate to accept key sequences that are a prefix of both sides' index field order (reuse `leadingKeySequence` from `LmdbNativeChunkPipeline.java:275-304`), keeping the single-field restriction when a composite source with multiple active branches is in play; propagate the merge output's non-decreasing join-slot ordering to the planner (a `StatementOrder`-equivalent tag on the cursor so downstream ORDER BY/merge consumers can exploit it — the javadoc at `:32` documents the gap). Hash join (`LmdbNativeHashJoin`): exclude key slots from payload storage (the left-join probe at `LmdbNativeLeftJoinPayloadProbe.java:143-152` shows the pattern); bushy runtime builds — when the right operand is itself a join whose sweep is input-independent (the recognition predicate exists at `LmdbNativeJoinPlans.java:588-597` for replay), drain it once into `PrimitiveHashJoinTable` instead of the replay list, restoring bulk builds over sub-joins. Multi-key SIP publication in the chunk pipeline (one mask per qualifying key slot, relaxing `trySipTarget`) rides along here since merge multi-key unlocks it.

Acceptance: merge suite extended with skewed-key and multi-key cases; rescan counters drop to zero when either run fits; `?a :knows ?b . ?a :worksWith ?b` (two shared variables) gets merge/SIP treatment (dispatch tags asserted); snowflake benchmark (two selective stars) stops re-executing the second star per row (probe counters, ≥2×); `HashJoinBenchmark.probeBound` at parity or better; no inversion anywhere.

### Milestone 7 — Kernel shape coverage: wire the dead IR primitives

Goal: kernels stop declining (or degrading) on shapes the IR already supports on paper, so more of the corpus reaches the compiled tier at all.

Work, one primitive at a time, each with a red lowering test asserting the shape now lowers and a parity test against the interpreter. `FilterRangeUnsigned`: lower range-pushdown filters (the interpreter's `OrderedSlotCompareFilter` LT/LE/GT/GE cases that `lowerIdFilter` currently declines beyond EQ/NE) into the existing node; ranges over ordered domains also tighten `EnumerateDomain` bounds. `BindHook`: implement `KernelHooks.computeBind` in `LmdbNativeKernelHooks` (evaluate the compiled scalar expression via the existing `LmdbNativeScalarExpressionCompiler` machinery, return the bound id or a miss marker) and lower computed BINDs, removing the `bind-computed` decline for expressions the scalar compiler supports. Context columns: extend the emitter with `contextAt`/`copyContexts` access so patterns with a projected context variable or a constant-context restriction lower against adjacency views instead of falling to `ScanQuad`; the vector tail bulk-copies contexts beside neighbors. `Emit.distinct`: set it when the query's DISTINCT is total over emitted columns, replacing the engine-side distinct pass. `OutputMods`: lower ORDER BY (over id-sortable keys) with LIMIT/OFFSET into the kernel's existing top-k support so `ORDER BY … LIMIT k` shapes keep the whole pipeline fused. `Having`: pass the lowered HAVING filter into `buildAggregate` instead of null. Also fix the decline-reason prefix bug (bare strings from shared sites mis-attribute aggregate declines as row declines — add the `reasonPrefix` at the sites listed in the audit).

Acceptance: for each primitive, a shape that previously produced the decline string (or a `ScanQuad`/interpreted fallback) now compiles — assert via `LmdbNativeExplain`/runtime-plan strings; multiset parity vs interpreter on each shape; kernel decline census (the two ~90s gates in the kernel-decline-census instrument) shows the targeted reasons at zero; JMH on affected M0 cells shows janino ≥ adjacency.

### Milestone 8 — Kernel hash join

Goal: multi-pattern shapes where the interpreted batch hash join wins (probe-bound joins with a small build side) compile into kernels that at least match it — removing the largest "kernel engages and loses" class.

Work: add IR nodes `HashBuild(keyCols, payloadCols, source)` and `HashProbe(keyCols, table)` to `LmdbNativeKernelIr`; the emitter materializes the build side into a primitive open-addressed table (extend `codegen/KernelRuntime` with a `LongRowMap` — flat `long[]` keys+payload, stored hashes, 8-bit fingerprints, unique-key fast path — mirroring `PrimitiveHashJoinTable`'s layout so the design is done once); lowering chooses hash for a producer pair when the same cost test the interpreter uses (`LmdbNativeHashJoin.tryPlan`'s cost gate) says hash beats the probe chain, sourcing estimates from the already-threaded static estimates. The build loop is itself a kernel pipeline section (so filters lower into the build side). Memory: charge the table to the query ledger via the existing `LmdbNativeQueryMemoryScope` seam; on refusal, fall back to emitting the probe-chain plan (decline inside lowering, not at runtime).

Acceptance: red lowering test (probe-bound two-pattern shape lowers to HashBuild/HashProbe); parity corpus across widths/duplicates/contexts; `HashJoinBenchmark.probeBound` in the janino regime ≥ the interpreted hash join (this cell is currently an inversion whenever the kernel engages); no regression on chain-friendly shapes (lowering must keep choosing the probe chain there — assert via explain strings).

### Milestone 9 — Kernel worst-case-optimal intersection

Goal: cyclic shapes (triangles, cliques) compile into kernels that intersect sorted runs with galloping — making the compiled tier at least match the interpreted leapfrog instead of declining (`unsupported:LeapfrogPlan`) and ceding those shapes forever.

Work: wire the dead `Intersect` node. Lowering detects the same cyclic-core shape the interpreter does — reuse `LmdbNativeLeapfrogJoin.findCyclicCore`'s recognition (expose it as a static analysis on the flattened join bag rather than re-implementing GYO ear reduction) — and lowers the core to nested `Intersect` levels over adjacency runs, with `lowerBound` galloping emitted in the generated intersection loop (this depends on M1's run-view seek surface and M7's context columns for scoped variants). Non-core prefix/suffix patterns lower as the usual probe chain around the intersection. Frontier sources are the same three the interpreter uses: member runs (`find` + run view), key-domain enumeration for free keys, and decline to interpreted leapfrog when a member cannot be adjacency-served (a kernel-level decline, keeping the ordering by equality). Multiplicity semantics must match the interpreter's duplicate handling (duplicate edges produce duplicate results) — port the counts-on-final-level rule.

Acceptance: red lowering test (triangle lowers to `Intersect`); bag-equivalence corpus vs both the interpreted leapfrog and the generic evaluator (duplicate edges, GRAPH scoping declines, dirty-txn declines); `FoafCliqueQueryBenchmark.cycle3/cycle4/cycle5` in the janino regime at parity or better vs the adjacency regime under the long-iteration protocol; the M0 cyclic cells flip to `OK`.

### Milestone 10 — Kernel robustness: mixed binding, row-rung EXISTS, aggregate residuals

Goal: remove the all-or-nothing cliffs that currently make kernels brittle exactly where the interpreter degrades gracefully.

Work. Per-pattern mixed binding: `LmdbNativeKernelBindings.requestAdjacencies` stops failing the whole kernel when one view is missing — lowering already knows how to produce `ScanQuad` for any pattern (that is what the `preferScans` retry does globally); instead, bind per-request, and for each missing view swap only that node to its scan form (requires lowering to record the scan alternative beside each adjacency request; emit both and select at bind time via a per-node flag, or re-lower with a per-pattern prefer-scan mask — choose the simpler re-lower approach first and measure compile-cache pressure since the mask enters the shape key). Row-rung EXISTS: lower sticky EXISTS filters on the row rung to the existing `Exists` node instead of leaving them as engine-side residuals (the aggregate rung already lowers them; the row rung currently cannot, so EXISTS-bearing row shapes lose their filter pushdown when compiled). Aggregate-rung residual tier: give the aggregate rung the same residual-filter wrapper the row rung has, so one non-lowerable filter no longer declines the whole aggregate kernel — the residual applies per emitted group row before accumulation only when semantically safe (pre-aggregation filters only; HAVING stays in-kernel per M7).

Acceptance: a shape with one adjacency-ineligible pattern (e.g. one pattern under a named graph) still compiles with the other patterns adjacency-served (explain strings assert the mix); EXISTS row shapes compile with in-kernel semijoin and match interpreted results; an aggregate with one hook-unsupported filter compiles with a residual instead of declining; kernel decline census drops accordingly; no parity or ordering regressions on the M0 matrix.

### Milestone 11 — Corpus sweep, default flips, retrospective

Goal: prove the plan's headline claim end-to-end and leave the tree in its final state.

Work: re-run the full M0 harness (three paired runs per regime pair, long-iteration protocol for slow cells); for every remaining inverted or overlapping cell, either fix (small follow-ups), file the cause in Surprises with evidence, or record a justified decline (higher tier not engaging is equality — verify the engagement witness agrees). Flip the per-milestone kill switches to default ON for every feature whose cells are parity-or-better (the user's standing gate), each flip with its own red→green default test as branch convention. Run the full `core/sail/lmdb` module verify plus `core/queryalgebra/evaluation` (the SPI changes touch it), triaged against the pre-existing-failure baseline. Update `plans/lmdb-native-engine/09-joins.md` status and mark the stale survey census table in `plans/lmdb-native-engine/30-adjacency-consumption-survey.md` as superseded (pointer to this plan). Write the Outcomes & Retrospective.

Acceptance: `scripts/three-tier-report.py` over the final runs shows zero `INVERTED` cells; every `OVERLAP` cell has a recorded justification; module verifies green vs baseline; this plan's living sections complete.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j` unless stated.

Session bootstrap (every session):

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Targeted tests (examples; never `-am`, never `-q` with tests):

    python3 .codex/skills/mvnf/scripts/mvnf.py ThreeTierEngagementCensusTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeLeapfrogJoinTest
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb

Report-script tests (fast, no Maven):

    python3 scripts/test_three_tier_report.py

Benchmarks (anchor the method regex; one JMH run at a time; check for stale `/var/folders/**/jmh.lock` if a run silently produces nothing). One cell in one regime:

    ./scripts/run-single-benchmark.sh --module core/sail/lmdb --class ThreeTierParityBenchmark --method 'fullPredicateScan$' --param regime=lmdb > benchmark-results/tier-m0-lmdb-r1-$(date +%F).txt

Forked runs (the default, `--forks 1`) may sweep the regime parameter: JMH gives every parameter combination its own JVM, so omitting `--param regime=…` produces all three columns in one result file, each measured in its own JVM, which is what the one-regime-per-JVM pin requires. Pinning the parameter is mandatory only for `--forks 0`, where every cell shares the harness JVM. Passing it explicitly anyway lets the three regimes be interleaved run by run, so machine drift lands on all of them alike — that is how the baseline below was captured. Exclude the theme cells from generated sweeps so the multi-gigabyte store is left alone:

    ./scripts/run-single-benchmark.sh --module core/sail/lmdb --class ThreeTierParityBenchmark --method '.*' \
        --param regime=lmdb --warmup-iterations 2 --measurement-iterations 3 --no-build \
        --jmh-arg '-e' --jmh-arg 'theme' > benchmark-results/tier-m0-lmdb-r1-$(date +%F).txt

The theme cells need the shared store loaded first (run `ThemeQueryBenchmark` once if it is missing) and are selected with `--method 'theme.*'`. Keep at least three measurement iterations: below three, JMH prints no error bar at all and the disjoint-interval rule degenerates into comparing bare scores.

Then build the matrix. Feeding it every run of every regime is the point — the intervals widen across runs:

    python3 scripts/three-tier-report.py benchmark-results/tier-m0-*-$(date +%F).txt
    python3 scripts/three-tier-report.py --fail-on-inverted --format md benchmark-results/tier-m0-*.txt

The engagement witness for a benchmark run is in the result file itself, one line per measured iteration:

    grep '### three-tier engagement' benchmark-results/tier-m0-janino-r1-*.txt

The engagement census, including the opt-in theme half:

    python3 .codex/skills/mvnf/scripts/mvnf.py ThreeTierEngagementCensusTest
    python3 .codex/skills/mvnf/scripts/mvnf.py ThreeTierEngagementCensusTest -- -Drdf4j.lmdb.threeTierParity.themeCensus.enabled=true

Formatting and headers before any commit:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    (cd scripts && ./checkCopyrightPresent.sh)

Commits use the `GH-0000` prefix (no issue number provided; note this in handoffs). Commit at every milestone boundary at minimum.

## Validation and Acceptance

Overall acceptance is behavioral: run the Milestone 0 harness in all three regimes and the report shows, for every corpus query, `adjacency ≥ lmdb` and `janino ≥ max(lmdb, adjacency)` under the disjoint-interval rule, where a non-engaging higher tier counts as equality only if the engagement census confirms it declined rather than engaged-and-lost. Every milestone additionally carries its own red→green functional evidence (failing test first for behavior changes, Surefire/Failsafe snippets captured per repository rules) and its own paired-benchmark evidence for the cells it claims. Multiset parity against the generic evaluator is mandatory for every new execution path (the corpus tests assert bag semantics, GRAPH scoping, and dirty-transaction behavior).

## Idempotence and Recovery

Every milestone is additive behind a default-off property until its flip step, so re-running a partially completed milestone is safe: re-run the bootstrap install, re-run the milestone's focused tests, continue. If a benchmark gate fails, the feature stays off and the code remains merge-safe. If an implementation step regresses a neighboring suite, park the change (copy aside, restore HEAD versions via `git show HEAD:<path> >` — never `git checkout --`), prove the baseline, and resume. Benchmark artifacts are append-only under `benchmark-results/` with dated names; never delete untracked artifacts. The theme-benchmark store under `core/sail/lmdb/target` is protected from `clean` by the module pom — do not circumvent.

## Artifacts and Notes

Baseline artifact naming: `benchmark-results/tier-<milestone>-<regime>-r<N>-<date>.txt`; theme captures carry a `theme-` infix. Artifacts under `benchmark-results/` are append-only — never delete or overwrite one, and if a capture has to be repeated, write it to the next free suffix (`…-r1.2-…`) so the earlier evidence survives.

### Milestone 0 baseline, generated datasets (2026-08-04)

Captures, three paired runs per regime, regimes interleaved within each run so machine drift lands on all three alike. Command as recorded in Concrete Steps: 35 generated-dataset cells, 2 warm-up and 3 measurement iterations of 2 s, one fork per cell, theme cells excluded.

    benchmark-results/tier-m0-lmdb-r1-2026-08-04.txt        benchmark-results/tier-m0-lmdb-r2-…      benchmark-results/tier-m0-lmdb-r3-…
    benchmark-results/tier-m0-adjacency-r1-2026-08-04.txt   benchmark-results/tier-m0-adjacency-r2-… benchmark-results/tier-m0-adjacency-r3-…
    benchmark-results/tier-m0-janino-r2-2026-08-04.txt      benchmark-results/tier-m0-janino-r3-…    benchmark-results/tier-m0-janino-r4-…

`benchmark-results/tier-m0-janino-r1-2026-08-04.txt` is retained but EXCLUDED from the matrix: a multi-threaded `javac` ran during its measurement iterations, so its timings are contaminated. `tier-m0-janino-r4` is its replacement, which is why the janino runs are numbered 2, 3, 4.

Headline: **zero inverted cells**, 10 comparisons strictly faster, 60 indistinguishable, 0 missing (70 comparisons over 35 cells). The full matrix is reproducible with the report command in Concrete Steps; the cells that moved, and the cells that matter for later milestones, are these.

Where a higher tier already wins decisively (mean, ms, lmdb → adjacency → janino):

    predicateHistogram      3.783 → 0.032 → 0.032     both tiers OK; adjacency answers the whole-plane histogram outright
    countOnePredicate       0.823 → 0.019 → 0.018     both tiers OK
    pathUnderJoin          22.745 → 3.030 → 4.446     adjacency OK, janino OK vs lmdb
    allPredicates           0.068 → 0.024 → 0.025     adjacency OK; janino inside the error bars
    correlatedCount        29.767 → 16.914 → 3.974    janino OK; the kernel is doing the aggregate
    outerAccumulate        39.623 → 28.594 → 14.873   janino OK
    selectiveSipChain       3.234 → 1.696 → 2.395     adjacency OK; the kernel gives some back
    starJoin                7.353 → 4.108 → 3.623     monotone improvement, still inside the error bars

Suspected inversions — a higher tier engages and its mean is worse, but the intervals still overlap, so the rule says `OVERLAP`. This is the Milestone 2/7/10 work list and the first thing to re-measure under the long-iteration protocol:

    chainJoinOpen           0.916 → 1.249 → 1.639     kernel engages (1104 opens); +36% then +79% on the mean
    optionalHeavy           7.297 → 9.716 → 11.235    kernel engages (177 opens); +33% then +54%
    minusShape              4.179 → 5.594 → 6.373     kernel engages (292 opens); +34% then +53%
    sequencePath            2.504 → 2.224 → 2.621     adjacency gains, the kernel (668 opens) gives it back

Not an inversion, despite appearances — the kernel declines, so the two upper columns are the same execution path:

    cycle5                 87.383 → 92.813 → 98.712   kernelOpens=0, 22.5M adjacency views: spread is noise

Cells flagged `noisy` (mean above the 100 ms floor with error bars wider than a quarter of the score) need the long-iteration protocol before any verdict: `cycle5ValuesCountMailboxHomepage` (342 ± 119 lmdb) and `cycle5ValuesDistinctMailboxOrdered` (496 ± 332 lmdb).

### Milestone 0 baseline, theme representatives (2026-08-04)

Captures, three paired runs per regime, forked one JVM per cell, six `theme*` cells against the shared multi-theme store:

    benchmark-results/tier-m0-theme-lmdb-r{1,2,3}-2026-08-04.txt
    benchmark-results/tier-m0-theme-adjacency-r{1,2,3}-2026-08-04.txt
    benchmark-results/tier-m0-theme-janino-r{1,2,3}-2026-08-04.txt

Report: 6 cells, 12 comparisons, 1 OK, 9 OVERLAP, **2 INVERTED**. Mean, ms, lmdb → adjacency → janino:

    themeHighlyConnectedQ4    108.118 → 176.120 → 479.141   INVERTED on both rungs; see Surprises
    themeHighlyConnectedQ1    489.992 → 398.151 → 265.670   janino OK: the kernel earns 1.84x over LMDB
    themeEngineeringQ1        123.605 → 102.550 →  86.281   monotone improvement, inside the error bars
    themeAnalyticsQ5          800.135 → 777.983 → 775.779   adjacency declines entirely (see Surprises); flat
    themeAnalyticsQ8          229.484 → 230.357 → 227.750   two plane lookups total; effectively LMDB in all three
    themeAnalyticsQ9          408.557 → 431.883 → 440.728   unusable error bars (±994 on 441); needs long iterations

`themeHighlyConnectedQ4` is the plan's first proven inverted cell and the sharpest single result of Milestone 0. `themeAnalyticsQ9` must be re-measured under the long-iteration protocol before it is read at all — its janino interval is wider than its mean.

### Milestone 0 engagement census (2026-08-04)

Full per-cell table in the Surefire capture at `logs/mvnf/20260804-130644-verify.log:245-281`. Coverage: adjacency serves 34 of 35 generated-dataset cells (only `nodeEdgeDump` declines); kernels compile 13 of 35. The 22 kernel declines are the Milestone 7 to 10 work list, and the cyclic family within it is Milestone 9's.

## Interfaces and Dependencies

New or changed surfaces this plan commits to (final state):

In `core/sail/lmdb/.../evaluation/LmdbNativeLeapfrogJoin.java`: a package-private frontier view over `NativeAdjacency` runs supporting `lowerBound`-based galloping without materialization (M1).

In `core/sail/lmdb/.../evaluation/LmdbNativeMembership.java`: `AdjacencyIntersectionProbe` (M2) and the multi-key mark table entry points (M5) built on `LmdbNativePrimitiveTupleTable`.

In `core/queryalgebra/evaluation/.../evaluationsteps/BatchCorrelatedJoinProvider.java`: `Mode` gains SEMI and ANTI; `BatchCorrelationRequest` carries up to 4 shared variables and the condition evaluator is honored (M4).

In `core/sail/lmdb/.../evaluation/LmdbNativeKernelIr.java`: `HashBuild`/`HashProbe` nodes (M8); `Intersect` constructed by lowering (M9); `FilterRangeUnsigned`, `BindHook`, `Emit.distinct`, `OutputMods`, `Having` constructed by lowering (M7).

In `core/sail/lmdb/.../evaluation/codegen/KernelRuntime.java`: `LongRowMap` primitive hash table (flat keys+payload, stored hashes, fingerprints, unique-key fast path) (M8).

In `core/sail/lmdb/.../evaluation/LmdbNativeKernelBindings.java`: per-request binding with per-pattern scan substitution replacing all-or-nothing `requestAdjacencies` (M10).

New test/benchmark files (M0, all delivered 2026-08-04 and all test scope):

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/AdjacencyEngagementTestAccess.java` — public bridge over the package-private adjacency metrics, tolerating an absent index so the LMDB regime reads through the same code path:

    public static boolean adjacencyPresent(LmdbStore store)
    public static long lookupHits(LmdbStore store)
    public static long exactMisses(LmdbStore store)
    public static long plannerStatsHits(LmdbStore store)
    public static long kernelViewsServed()
    public static long engagement(LmdbStore store)
    public static String fallbackSummary(LmdbStore store)
    public static String state(LmdbStore store)
    public static boolean buildNow(LmdbStore store)

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThreeTierRegime.java` — the ladder, with `parse`, `paramValue`, `adjacencyMode`, `janinoEnabled`, `pin`/`pinnedRegime`, `applyProperties`, `applyTo(LmdbStoreConfig)` and `restore`.

`.../benchmark/ThreeTierParityCorpus.java` — the 41-cell enum with `benchmarkMethodName()`, `dataset()`, `query()`, `of(Dataset)` and `byBenchmarkMethod(String)`, plus the nested `Dataset` enum and the `FoafData` / `CorrelatedData` constant holders.

`.../benchmark/ThreeTierParityFixtures.java` — `fixture(Dataset, ThreeTierRegime)` (pinned, JVM-cached), `openIsolated(Dataset, ThreeTierRegime)` (uncached, for the census), `closeAll()`, `themeStoreDirectory()`, `themeStoreAvailable()`, and the nested `Fixture` with `execute`, `prepare`, `engagementSinceLastReport` and `close`.

`.../benchmark/ThreeTierParityBenchmark.java` — 41 `@Benchmark` methods plus the `RegimeState` base and its `FoafState` / `CorrelatedState` / `ThemeState` subclasses.

`.../benchmark/ThreeTierEngagementCensusTest.java` — `generatedDatasetCellsEngageTheTiersTheyClaim` and the opt-in `themeCellsEngageTheTiersTheyClaim`.

`scripts/three-tier-report.py` with `scripts/test_three_tier_report.py`.

Harness-only system properties (test scope, no production effect): `rdf4j.lmdb.threeTierParity.themeCensus.enabled`, `rdf4j.lmdb.threeTierParity.foafPeople`, `rdf4j.lmdb.threeTierParity.correlatedOuterRows`, `rdf4j.lmdb.threeTierParity.adjacencyTimeoutSeconds`, `rdf4j.lmdb.threeTierParity.compileTimeoutSeconds`.

System properties introduced (all default off until their flip): `rdf4j.lmdb.wcoj.streamingFrontiers.enabled`, `rdf4j.lmdb.adjacencySemijoin.enabled`, `rdf4j.lmdb.directAdjacency.exactEmptyPruning.enabled`, `rdf4j.lmdb.markJoin.enabled`, `rdf4j.lmdb.mergeJoin.multiKey.enabled`, `rdf4j.lmdb.nativeHashJoin.bushy.enabled`, `rdf4j.lmdb.janinoCodegen.rangeFilters.enabled`, `rdf4j.lmdb.janinoCodegen.bindHook.enabled`, `rdf4j.lmdb.janinoCodegen.contextColumns.enabled`, `rdf4j.lmdb.janinoCodegen.outputMods.enabled`, `rdf4j.lmdb.janinoCodegen.hashJoin.enabled`, `rdf4j.lmdb.janinoCodegen.intersect.enabled`, `rdf4j.lmdb.janinoCodegen.mixedBinding.enabled`. Existing default-off flags this plan completes and expects to flip: `rdf4j.lmdb.nativeAccumulateJoin.enabled`, `rdf4j.lmdb.leftjoin.sweep.enabled`, `rdf4j.lmdb.nativeHashJoin.byteAdmission.enabled` (byte admission rides along with M8's ledger integration since kernel hash tables charge the same scope).

Dependencies between milestones: M1 precedes M9 (run-view seek surface); M7's context columns precede M9's scoped variants; M4 precedes M5's correlated EXISTS routing; M6's multi-key merge precedes its SIP publication rider; M0 precedes everything (it is the acceptance instrument). No new external libraries; Janino and LWJGL/LMDB bindings already present.

---

Revision note (2026-08-04, initial authoring): plan created from the three-regime capability audit performed the same day; scope boundary (capabilities, not gating/preference) fixed by user instruction; acceptance operationalized as the no-inverted-cell rule after discussion of what "always faster" can mean against declining tiers.

Revision note (2026-08-04, Milestone 0 implemented): recorded what the harness actually is, in the Milestone 0 section ("As delivered") and in Interfaces and Dependencies (every new file with its public surface). Six decisions the plan had left implicit are now in the Decision Log and were all forced by building the thing: the regimes had to become a strict ladder (the plan's Context left code generation unspecified for the LMDB regime, which would have made its column mean "cursors plus kernels"); the Janino regime had to force kernel admission, because most corpus cells never reach the 32768-row default and the column would otherwise have measured interpreted execution; one JVM had to be pinned to one regime, because compiled kernels and built planes survive a property flip; "the 19 `AdjacencyQueryShapeBenchmark` shapes" had to be resolved (the plan conflated that benchmark's 13 methods with the census's 19 scenarios); the shared theme store had to be opened read-only with no rebuild path; and the corpus had to live in one enum shared by benchmark and census. Four discoveries were added to Surprises, of which two are harness bugs found and fixed while measuring (the kernel-engagement counter omitting the legacy aggregate rung, and JMH not forwarding forked stdout at trial teardown) and two are the measured baseline itself: adjacency already serves 33 of 34 generated-dataset cells, while kernels compile only 13 — with the entire cyclic family declining, which is precisely Milestone 9's premise, now measured rather than inferred. Concrete Steps gained the real, validated commands (regime must be passed explicitly, theme cells excluded from generated sweeps, at least three measurement iterations or JMH prints no error bar at all). Milestone 0 is split in Progress: the harness and the generated-dataset baseline are done; the theme-cell baseline remains.
