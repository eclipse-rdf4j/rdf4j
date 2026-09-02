# Take full advantage of the in-memory direct-adjacency structures: scans, probes, aggregates, statistics, paths, and new operators

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md` (repository root: `/Users/havardottestad/Documents/Programming/rdf4j`). The survey that motivated it is `plans/lmdb-native-engine/30-adjacency-consumption-survey.md`; this plan is self-contained — you do not need to read the survey to execute this plan.

## Purpose / Big Picture

The LMDB store on branch `optimize-lmdb` builds complete in-memory adjacency structures at startup (plan 27, "direct adjacency"): for every predicate, sorted neighbor lists in both directions, held off-heap and kept current through commit deltas. Today only three query shapes consume them: a lookup with the predicate and exactly one endpoint bound, per-row join probes, and the worst-case-optimal triangle join's frontiers. Everything else — full predicate scans, `DISTINCT`/`COUNT`/degree aggregates, doubly-bound existence probes, queries inside write transactions, parallel query workers, and the query planner's statistics — reads LMDB B-trees off disk even though the identical data sits in memory in a better layout. Graph databases such as Kuzu answer all of these from adjacency lists; that is the bar.

After this plan, a user running SPARQL against an LMDB store sees: `SELECT ?s ?o WHERE { ?s :p ?o }` and its `DISTINCT`/`COUNT`/`GROUP BY` variants answered from memory at parity with or faster than LMDB; point probes `<s> :p <o>` answered from one binary search; queries no longer losing adjacency service just because a write transaction is open or the engine parallelized; the optimizer using exact instead of sampled cardinalities for adjacency-covered predicates; property paths traversing memory end to end; and two new execution tricks (run-intersection semijoins, adjacency-key semijoin masks) that have no LMDB equivalent. Every new path sits behind a system-property kill switch, defaults off until its acceptance gate passes, and must show exact multiset parity with the generic evaluator.

How to see it working at any point: run the census test (prints a per-query-shape table of adjacency engagement and fallback reasons) and the benchmark harness (times the same shapes):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyUsageCensusTest
    ./scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.AdjacencyQueryShapeBenchmark --method fullPredicateScan

## Progress

- [x] (2026-08-03) Survey completed: 19-shape census harness (`LmdbAdjacencyUsageCensusTest`) and JMH acceptance workloads (`AdjacencyQueryShapeBenchmark` + smoke test) added and green; gap census captured in `initial-evidence-adjacency-census.txt`; survey report at `plans/lmdb-native-engine/30-adjacency-consumption-survey.md`.
- [x] (2026-08-03) This ExecPlan authored; all seam line anchors verified against HEAD of `optimize-lmdb` the same day.
- [x] (2026-08-03 12:00Z) Milestone 0 complete: pre/post census, kernel-bind and aggregated path-expansion counters, path attribution, and 11 isolated dated JMH baselines under `benchmark-results/adjacency-shapes-baseline-*-2026-08-03.txt`.
- [x] (2026-08-03) Milestone 1 complete and default-enabled with an explicit `false` kill switch. Three fresh
  `fullPredicateScan` pairs and affected DISTINCT/COUNT/degree workloads were all at parity or faster than LMDB.
- [x] (2026-08-03) Milestone 2 complete and default-enabled: lower-bound point probes preserve contexts and inlined
  objects, census `DOUBLY_BOUND=0`, and three off/on pairs held 0.010 ms/op parity.
- [x] (2026-08-03) Milestone 3 complete and default-enabled: a shared atomic dirty marker flips only on genuine
  commit-delta events; clean store writers serve, dirty add/remove writers decline, and rollback resets eligibility.
- [x] (2026-08-03) Milestone 4 complete and default-enabled: every same-snapshot worker owns an exact adjacency view;
  its row/probe/count/has/degree surface is adjacency-backed, while range partitions retain their bounded LMDB scan.
- [x] (2026-08-03) Milestone 5 complete and default-enabled: immutable snapshot plane counts feed subject/predicate
  prefix cursors and exact COUNT/DISTINCT/degree/predicate aggregates; every paired acceptance workload beat LMDB.
- [ ] Milestone 6: exact optimizer statistics are implemented behind a default-off property; finish the plan-snapshot
  audit and paired acceptance measurements before deciding whether to default-enable them.
- [ ] Milestone 7: SIP semijoin masks from adjacency key domains.
- [ ] Milestone 8: property paths fully over adjacency (seeding, workers, telemetry-verified).
- [ ] Milestone 9 (evidence-gated): run-intersection semijoins, degree binding, bidirectional path search, exact-empty plan pruning.

## Surprises & Discoveries

- Observation: The census shows `path_reachability` (`:p+` from a bound start) making exactly 1 metered adjacency hit for 2000 node expansions, yet `LmdbNativePathPlan` HAS a complete adjacency fast path (`expandSlice` at `LmdbNativePathPlan.java:1238-1252` and `expandCachedLevel` at `:1799` walk `cached.find/size/neighborAt` per frontier node). The reading is ambiguous because kernel-path serving is unmetered: `LmdbDirectAdjacencyStore.adjacency(...)` returns a view without recording a hit, and per-`find` metering is deliberately banned on that hot path. Milestone 0 exists to resolve exactly this before anyone "fixes" the path engine.
  Evidence: census row `path_reachability 2000 rows, adjHit=1, no fallback`; code read of `LmdbNativePathPlan.expandSlice` 2026-08-03.
- Observation: The unbound-predicate analytics shapes (`SELECT DISTINCT ?p`, predicate histogram) recorded NO fallback reason at all — the paths that serve them (prefix runs / root-scan partitions) never call `tryDirect`, so the adjacency store never even learns it was bypassed. Fallback counters only measure consultation points that exist; absence of a fallback is not evidence of service.
  Evidence: census rows `all_predicates` and `predicate_histogram` show `-` in the fallback column while `full_predicate_scan` shows `ROOT_SCAN=1`.
- Observation: The bounded-start `path_reachability` shape already expands entirely from direct adjacency. The new coarse counters attribute 2,001 frontier-node expansions to cached adjacency, zero to `PathFrontierSweep`, and zero to per-node cursors; the single pre-existing `adjHit` is the one kernel-view bind, not one serviced node.
  Evidence: `logs/mvnf/20260803-115411-verify.log` census row `path_reachability ... knBind=1 paAdj=2001 paSwp=0 paCur=0`; focused census passed with 1 test, 0 failures.
- Observation: The repository-wide copyright checker currently exits nonzero for pre-existing POM findings in unrelated modules, while reporting no finding for the new `PathDispatchTestAccess.java` or either edited Java source.
  Evidence: `(cd scripts && ./checkCopyrightPresent.sh)` output on 2026-08-03 listed existing `pom.xml` files only.
- Observation: A lookup-per-key M1 iterator regressed `fullPredicateScan` by about 35% (paired scores around 2.62 ms
  on versus 1.95 ms off). A CPU profile attributed the avoidable part to repeated `KeyDomain.keyAt`,
  `CompactCsfPageReader` construction, per-key `find`, and separate neighbor/context decoding. A sequential page cursor
  with fused pair copies removed those costs and brought the candidate to ~1.82 ms, but the follow-up profile shows the
  adjacency iterator itself at only 0.44% of samples; public `NativeProjectedBindingSet` construction and native-row
  projection dominate. The SELECT-and-drain workload therefore has an output-materialization Amdahl ceiling and did
  not reach the plan's 1.5x acceptance threshold without an invalid consumer-specific shortcut.
  Evidence: `/tmp/rdf4j-async-profiler/adjacency-root-scan-cpu-21402.txt`,
  `/tmp/rdf4j-async-profiler/adjacency-root-scan-cpu-sequential-*.txt`, and M1 off/on benchmark outputs dated
  2026-08-03.
- Observation: Repository-level `begin()` is lazy and a duplicate repository add is suppressed before the LMDB store
  writer opens. The precise M3 reproduction therefore uses the backing `SailSink`: approving an already-present quad
  opens a real store writer without recording a commit-delta event. Safety coverage reads the live LMDB write
  transaction directly after a genuine add/remove and separately proves the row path declines.
  Evidence: `initial-evidence-adjacency-clean-txn-red.txt` and the three M3 tests in
  `LmdbDirectAdjacencyQueryTest`.
- Observation: `AdjacencyQueryShapeBenchmark.fullPredicateScan` is a single-pattern query, while
  `LmdbNativeParallelPipelines` deliberately rejects anything that is not a multi-join. It therefore measures M1, not
  M4. The real M4 gates are a forced two-pattern chain (which proves worker-row hits and generic multiset parity) and
  `ParallelismBenchmark.parallelOverlap`; the ordinary census separately confirms the M1 root scan remains served.
  Evidence: `LmdbNativeParallelPipelines.tryCreate`'s `not-multi-join` branch,
  `LmdbAdjacencyUsageCensusTest.forcedParallelChainServesBoundWorkerLookups`, and the dated M4 benchmark artifacts.
- Observation: The first M4 implementation read the rollout property and entered a synchronized lazy-view accessor on
  every worker probe. One noisy pair showed a 6.7% central-score regression. Capturing the flag once per sibling and
  using a volatile resolved-view fast path removed both per-row costs; the optimized three-run mean was 1.5% faster.
  Evidence: the rejected `m4-parallelOverlap-{off,on}-r*-2026-08-03.txt` runs and accepted
  `m4-parallelOverlap-optimized-{off,on}-r*-2026-08-03.txt` runs.
- Observation: The first M5 subject-prefix cursor performed a full adjacency lookup for every key. Reusing the paged
  CSF base's sequential root cursor removes that repeated page/key resolution while preserving the same delta-aware
  row composition, which turned the DISTINCT-subject workload from a regression into a clear win.
  Evidence: `benchmark-results/m5-distinctSubjects-*-2026-08-03.txt` and
  `benchmark-results/m5-opt-distinctSubjects-*-2026-08-03.txt`.
- Observation: Aggregate eligibility must be decided from the algebra and coverage proof before opening any scan.
  Exact-empty predicates, explicit/inferred merging, selected-predicate coverage, and inlined incoming object domains
  all need distinct handling; an incomplete object key domain must decline rather than silently undercount.
  Evidence: the M5 focused cases in `LmdbDirectAdjacencyQueryTest` and
  `initial-evidence-adjacency-{empty-count,predicate-analytics,subject-aggregates}-red.txt`.

## Decision Log

- Decision: Execute in ten milestones (0–9) ordered so that each unlocks or de-risks the next: observability first, then the root-scan iterator (whose key-enumeration machinery milestones 5 and 8 reuse), then the small eligibility fixes, then aggregates/statistics, then masks, then paths, then the evidence-gated new operators.
  Rationale: the survey's leverage ordering; the root-scan iterator and plane accounting are shared infrastructure for everything after them.
  Date/Author: 2026-08-03 / Claude (from user-approved survey items 1, 2, 3, 4, 6, 7, 8, 9; item 5 — node edge dumps under the CSF base — was explicitly excluded by the user).
- Decision: Every milestone ships behind its own system property, default OFF until that milestone's acceptance gate passes: `rdf4j.lmdb.directAdjacency.rootScan.enabled` (M1), `...directAdjacency.boundProbe.enabled` (M2), `...directAdjacency.cleanTxnReads.enabled` (M3), `...directAdjacency.parallelRowPath.enabled` (M4), `...directAdjacency.scanAggregates.enabled` (M5), `...directAdjacency.plannerStats.enabled` (M6), `rdf4j.lmdb.sip.adjacencyMasks.enabled` (M7), `rdf4j.lmdb.nativePath.adjacencySeeds.enabled` (M8). Default-enable originally required a 1.5x primary-workload win; the user superseded that threshold on 2026-08-03 with parity-or-better across the benchmarked queries and use cases, plus correct engagement counters.
  Rationale: matches the branch convention (`rdf4j.lmdb.wcoj.directFrontiers.enabled` went through exactly this lifecycle) and keeps every step trivially revertible.
  Date/Author: 2026-08-03 / Claude.
- Decision: Milestone 0 adds coarse-grained counters only (per-operator-bind and per-frontier, never per-neighbor-lookup). A `LongAdder` increment per `find()` call is banned.
  Rationale: `kernelFind` is an allocation-free hot path; the WCOJ work already proved per-DFS-node bookkeeping regresses real queries 60-70% (join ExecPlan, Surprises).
  Date/Author: 2026-08-03 / Claude.
- Decision: Milestone 1 serves root scans from the OUTGOING (subject-keyed) plane only; object-keyed enumeration joins the plan in Milestone 5 (distinct-objects) where the inlined-object complication is handled deliberately.
  Rationale: subjects are always reference ids, so the subject plane's key domain is complete by construction; object key domains additionally involve `ImmutablePagedQuadCsfIndex.KeyDomain` for inlined literals — a correctness trap that deserves its own red tests.
  Date/Author: 2026-08-03 / Claude.
- Decision: Path expansion counters count frontier nodes but perform one atomic addition per serial frontier level or parallel worker slice; kernel serving counts one successful adjacency-view bind.
  Rationale: this makes mechanism attribution comparable across serial and parallel execution without placing atomics inside per-node or per-neighbor hot loops.
  Date/Author: 2026-08-03 / Codex.
- Decision: Keep the M1 root-scan property default OFF and continue with independent milestones after its correctness
  implementation, rather than weakening the 1.5x gate or specializing execution for `Stream.count()`.
  Rationale: the optimized adjacency scan is faster than the LMDB source, but end-to-end binding materialization masks
  the gain; query-consumer detection would be a benchmark-specific semantic leak. Later aggregate pushdown has a
  legitimate way to avoid row materialization for COUNT/DISTINCT shapes.
  Date/Author: 2026-08-03 / Codex.
- Decision: Supersede the 1.5x default-enable threshold with the user's parity-or-better gate: enable a feature by
  default when fresh paired measurements show every benchmarked query/use case is as fast as LMDB or faster.
  Rationale: explicit user acceptance criterion. No-result regressions, semantic parity, engagement proof, and longer
  reruns for noisy/contended measurements remain mandatory.
  Date/Author: 2026-08-03 / Håvard and Codex.
- Decision: Default-enable M2 after all three `doublyBoundProbe` pairs measured 0.010 ms/op both off and on; retain
  `...boundProbe.enabled=false` as the rollback switch.
  Rationale: exact latency parity satisfies the user gate, while the served path eliminates LMDB reads and preserves
  multi-context, exact-empty, and inlined-object behavior.
  Date/Author: 2026-08-03 / Codex.
- Decision: Default-enable M3 and retain `...cleanTxnReads.enabled=false` as the conservative rollback switch.
  Rationale: the normal read-only path retains the same first `storeTxnStarted` atomic check; only the previously
  declined active-writer branch consults the new dirty marker. Focused add/remove/rollback tests and the complete
  wrapped census prove safety and engagement, so no separate throughput benchmark is informative for this
  eligibility-only change.
  Date/Author: 2026-08-03 / Codex.
- Decision: Default-enable M4 and retain `...parallelRowPath.enabled=false` as the LMDB-only worker rollback switch.
  Rationale: the forced-parallel chain has exact generic/sequential multiset parity and proportional worker-row hits;
  the optimized one-million-row `parallelOverlap` pairs were -2.647%, -2.668%, and +0.896%, for a -1.500% three-run
  mean. The only positive pair is statistical parity and remains far inside the plan's 5% no-regression bound.
  Date/Author: 2026-08-03 / Codex.
- Decision: Default-enable M5 and retain `...scanAggregates.enabled=false` as the LMDB rollback switch.
  Rationale: three paired runs improved DISTINCT subjects (0.605-0.623 versus 0.951-0.998 ms/op), degree grouping
  (1.301-1.462 versus 1.562-1.835), COUNT (0.014 versus 0.715-0.721), and predicate histogram (0.023 versus
  2.060-2.234). ANALYTICS q1/q2/q11 also improved pairwise, with exact generic-evaluator parity and explicit census
  engagement. This satisfies the user's parity-or-better default gate across every benchmarked use case.
  Date/Author: 2026-08-03 / Codex.
- Decision: Keep M6 `...plannerStats.enabled` default OFF at the checkpoint commit.
  Rationale: exact per-plane fan-out and pinned-snapshot behavior are implemented and focused-test green, but the
  required query-plan snapshot audit and paired query benchmarks have not yet been completed.
  Date/Author: 2026-08-03 / Codex.

## Outcomes & Retrospective

Milestone 0 observability is implemented and pre/post green on the same census selection. It established that bounded-start property-path expansion already uses adjacency end to end. All 11 benchmark methods have isolated JDK 25 baselines; the short-run intervals are deliberately not used for acceptance decisions, which remain governed by paired, disjoint intervals.

Milestone 1 now has a delta-aware predicate root iterator, an immutable-base sequential CSF cursor, fused pair decoding,
and parity coverage for ordering, empty results, contexts, delta insert/delete, dirty-writer decline, and continuation-page
supernodes. Census engagement is correct. Under the user-revised parity gate, three primary pairs favored adjacency
(1.935/1.961/1.992 ms versus 2.127/2.064/2.251 ms), DISTINCT and COUNT also improved, and the long degree rerun was
1.550 ms on versus 1.592 ms off. Root scanning is default-on; setting the property to `false` restores LMDB fallback.

Milestone 2 serves doubly-bound iterator requests from one outgoing run lower-bound. Default, named-graph, delta,
exact-empty, and inlined-object variants are green; the census point probe now records one hit and no `DOUBLY_BOUND`.
Three paired latency runs were identical at 0.010 ms/op, so the feature is default-on with an explicit false kill switch.

Milestone 3 replaces the coarse any-writer decline with a shared atomic first-mutation marker owned alongside
`storeTxnStarted`. A real no-op store writer now serves adjacency by default; a genuine add/remove still declines and
the LMDB writer contains the exact uncommitted state; rollback clears both lifecycle flags. The 37-test query corpus is
green, and a complete 19-shape census wrapped in an open clean writer showed hits for lookup, point-probe, root-scan,
and path shapes with no `READ_YOUR_WRITES` decline.

Milestone 4 gives every `ParallelSnapshotSource` the same exact-view and transaction-version proof as the parent
dataset. Unordered/ordered statements, retained probes, counts, existence checks, and exact degrees now serve directly;
range/partition scans remain on LMDB so their half-open boundaries cannot be violated. The flag is captured per sibling
and the already-resolved view is read without a monitor in the hot loop. The 40-test direct corpus, 3-test census, 22-test
parallel pipeline corpus, 5-test range corpus, and benchmark smoke test are green. Across three one-million-row paired
runs, adjacency averaged 324.403 ms/op versus LMDB's 329.343 ms/op, so M4 is default-on with an explicit false switch.

Milestone 5 publishes immutable per-plane quad/key counts across base builds, commit deltas, pinned views, and
consolidation. Delta-aware subject and predicate prefix cursors let native aggregate planning serve exact COUNT,
DISTINCT-subject, degree, DISTINCT-predicate, and predicate-histogram shapes without materializing statement rows.
Selected coverage and explicit/inferred unions are exact; unsupported inline-object key domains decline to LMDB.
Every focused and analytics acceptance pair was faster than LMDB, so scan aggregates are default-on with an explicit
false switch.

Milestone 6 has begun: covered current snapshots can expose exact outgoing/incoming mean fan-out from the published
plane counts, including asymmetric delta-only updates and pinned historical snapshots. The implementation remains
default-off until its query-plan snapshot and end-to-end performance gates are complete.

## Context and Orientation

Everything below is on branch `optimize-lmdb`. Full paths are relative to the repository root `/Users/havardottestad/Documents/Programming/rdf4j`. Line numbers were verified 2026-08-03 and may drift a few lines; always anchor by the named method, not the number.

Definitions used throughout:

- "The store" is the LMDB SAIL in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. Statements are quads of 64-bit value ids (subject, predicate, object, context). Small literals (integers, etc.) are INLINED into the id itself; everything else is a REFERENCE id. `ValueIds.isReference(id)` distinguishes them.
- "Direct adjacency" is the in-memory structure family owned by `LmdbDirectAdjacencyStore` (same package). For each predicate it holds four PLANES — {outgoing, incoming} × {explicit, inferred}. A plane maps a KEY (the subject for outgoing, the object for incoming) to a RUN: the sorted list of (neighbor id, context id) pairs for that key. Runs are unsigned-sorted by (neighbor, context). Each plane also has a KEY DOMAIN (`ImmutablePagedQuadCsfIndex.KeyDomain`): all distinct keys, unsigned-sorted, randomly addressable by ordinal. Base data lives in off-heap `MemorySegment`s (the default base representation is the paged "CSF" index, `csf/ImmutablePagedQuadCsfIndex`); commits append immutable DELTA GENERATIONS (`LmdbAdjacencyDeltaGeneration`) holding replacement runs or tombstones, consulted newest-first before the base (`resolveRow` in `LmdbDirectAdjacencyStore`).
- A READ VIEW (`LmdbAdjacencyReadView`) is a per-dataset lease pinning one revision; `view.isExact()` says the structure exactly matches the dataset's LMDB snapshot. Datasets acquire views in `LmdbSailStore` (search for `adjacencyView`, around `:3455`).
- The ROW PATH is `LmdbDirectAdjacencyStore.open(...)` (`:1542`), reached via `LmdbSailStore.tryDirect(...)` (`:3497`) from three call sites: the two `NativeLmdbQuerySource.statements(...)` overloads (`:3673`, `:3721`) and `RetainedNativeProbe.open(...)` (`:3884`, the probe every join operator uses). It returns a `RecordIterator` of quads or null; null falls back to LMDB and records a `FallbackReason` in `LmdbAdjacencyMetrics` (closed enum: ROOT_SCAN, DOUBLY_BOUND, READ_YOUR_WRITES, INDEX_ORDER_INCOMPATIBLE, PREDICATE_ENUMERATION_INCOMPLETE, ...). Successful serves increment `lookupHits`. Read counters via `direct.snapshotMetrics()` (package-private; tests must live in package `org.eclipse.rdf4j.sail.lmdb`).
- The KERNEL PATH is `LmdbDirectAdjacencyStore.adjacency(...)` (`:1703`) returning a `NativeLmdbQuerySource.NativeAdjacency` (interface at `evaluation/NativeLmdbQuerySource.java:179-250`: `find`, `size`, `neighborAt`, `contextAt`, `copyNeighbors`, `copyContexts`, `lowerBound`, `supportsKeyEnumeration`, `keyCount`, `keyAt`, `runsNeighborOrdered`). It is consumed by the WCOJ leapfrog join (`evaluation/LmdbNativeLeapfrogJoin`, methods `adjacencyFrontier`/`enumeratedKeyFrontier`), the Janino codegen kernels, and the property-path engine (`evaluation/LmdbNativePathPlan`). Kernel-path serving is currently UNMETERED (see Surprises).
- The NATIVE ENGINE is `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/` — executes SPARQL fragments over raw ids in primitive batches, on by default (`rdf4j.lmdb.nativeQueryEngine.enabled`). The GENERIC EVALUATOR (set that property to `false`) is the parity oracle for every change in this plan.
- A PREFIX RUN (`LmdbPrefixRunPlan`, planned by `TripleStore.prefixRunPlan` via `LmdbSailStore` around `:4185`) is an LMDB-index loose scan that jumps between distinct prefixes — how `DISTINCT ?s { ?s :p ?o }` is served today.
- MASTER GATES that appear in every serve decision: the view must be non-null, `servesSnapshot()`, and (row path) `isExact()`; an active store transaction declines with READ_YOUR_WRITES only after its first genuine statement mutation (or always when `...cleanTxnReads.enabled=false`); config mode must be PREFER (`DirectAdjacencyMode`, default PREFER with FULL coverage); kernel path additionally requires the view's revision to be completely applied (no pending tables).
- Harnesses from the survey: `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyUsageCensusTest.java` (19 query shapes over a deterministic 2000-person graph; prints per-shape adjacency hits, fallback-reason deltas, WCOJ frontier counters; asserts row counts only) and `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/AdjacencyQueryShapeBenchmark.java` (JMH methods `fullPredicateScan`, `distinctSubjects`, `countOnePredicate`, `degreePerSubject`, `predicateHistogram`, `pointLookupOut/In`, `doublyBoundProbe`, `nodeEdgeDump`, `twoHopFromSeed`, `pathReachability` over the 5000-person FOAF clique store shared with `FoafCliqueQueryBenchmark`).

House rules that bind every milestone (from `CLAUDE.md`): run the root clean install before any tests (command below); run tests one at a time through `python3 .codex/skills/mvnf/scripts/mvnf.py Class#method`; NEVER pass `-am` or `-q` to a Maven test run; every behavior change is Routine A (write the smallest failing test FIRST, capture its Surefire snippet, then implement, then capture green — evidence files at the repo root, e.g. `initial-evidence-<topic>-red.txt`); new Java files get the standard 2026 Eclipse header plus the line `// Some portions generated by Claude` and must pass `cd scripts && ./checkCopyrightPresent.sh`; format with `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources` before finishing. Benchmark verdicts use paired runs and disjoint error intervals only — never single scores (theme/JMH short runs are noisy; see the join-strategy plan's protocol).

    Root install (repo root, ≥60s timeout):
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 | tee maven-build.log | awk '/\[WARNING\]/{next} /\[ERROR\]/{print;next} /Reactor Summary/{summary=1} summary{print}'

## Plan of Work

### Milestone 0 — Baselines and engagement observability

Scope: no behavior changes; counters and baseline numbers only. At the end, every adjacency consultation point is observable, the path-engine ambiguity (Surprises) is resolved, and dated baseline JMH numbers exist for every `AdjacencyQueryShapeBenchmark` method.

Work: (a) add three static `AtomicLong` counter families, following the existing pattern in `LmdbNativeLeapfrogJoin` (`PLANNED`/`OPENED`/`FRONTIERS_*` with a `resetMetrics()`): in `LmdbDirectAdjacencyStore.adjacency(...)`, count non-null returns per bind ("kernel views served") — one increment per operator bind, NOT per lookup; in `evaluation/LmdbNativePathPlan`, count frontier expansions by mechanism — served from a cached `NativeAdjacency` (the `cached != null` branch in `expandSlice`/`expandCachedLevel`), via `PathFrontierSweep`, or via per-node `openStep` cursors; expose all through a new public test bridge `evaluation/PathDispatchTestAccess` and extend the existing `evaluation/JoinDispatchTestAccess` if more convenient. (b) Extend `LmdbAdjacencyUsageCensusTest` with these columns. (c) Run the census and record which mechanism actually serves `path_reachability` in this plan's Surprises section. (d) Capture baselines: one `run-single-benchmark.sh` invocation per `AdjacencyQueryShapeBenchmark` method, outputs under `benchmark-results/adjacency-shapes-baseline-<method>-<date>.txt`.

Acceptance: census prints the new columns; the `path_reachability` row unambiguously attributes its expansions; baseline files exist. This milestone is Routine B (counters are behavior-neutral; hit proof = census output showing them moving).

### Milestone 1 — Adjacency-served full predicate scans

Scope: `?s :p ?o` (both endpoints free, predicate bound, no fixed context) is served from the outgoing plane instead of declining ROOT_SCAN. This also serves the driving scan of open-ended joins (star/chain/object-object shapes in the census).

The mechanism: the outgoing plane's key domain enumerates every subject in unsigned-ascending order (`keyCount`/`keyAt`); for each key, `resolveRow` yields the run (pending tables → delta generations newest-first → base — reuse the existing resolution, do not reimplement it); the run enumerates (object, context) sorted pairs. Emitting (keyAt(i), predicate, neighbor, context) for i = 0..keyCount-1 therefore yields every explicit quad of that predicate exactly once, sorted by (subject, object, context) in raw-id order — which is also what the `spoc` LMDB index produces, so `StatementOrder.S` requests can be served too (verify against `LmdbDirectAdjacencyIterator`'s existing `orderCompatible` semantics before claiming order support; if in doubt, serve unordered first and add order in a follow-up commit inside this milestone).

Work: write the failing test first, in package `org.eclipse.rdf4j.sail.lmdb` (model it on `LmdbDirectAdjacencyQueryTest`, which shows store setup with `LmdbStoreConfig("spoc,posc").setDirectAdjacencyMode(PREFER)`, synchronous build via `direct.buildNowForTest()`, and metrics assertions via `direct.snapshotMetrics()`): assert that after enabling `rdf4j.lmdb.directAdjacency.rootScan.enabled`, a `?s :p ?o` evaluation increments `lookupHits` and does NOT increment `fallbacks(ROOT_SCAN)`, with multiset-identical results. Then implement: a new class `LmdbDirectAdjacencyRootIterator` (package `org.eclipse.rdf4j.sail.lmdb`, sibling of `LmdbDirectAdjacencyIterator` — read that class first; it shows how a serving iterator resolves runs, merges delta generations, filters context, and reports `RecordIterator` quads) driven by the key domain; wire it into `open(...)`'s branch 3: when subject and object are both unbound, predicate bound and resolved, context unrestricted, no repeated slot, order null-or-S, and the flag is on → serve; every other branch-3 shape keeps its current fallback. Delta-aware key enumeration must include keys that exist ONLY in delta generations or pending tables (a key added after the base build): read how `LmdbNativeLeapfrogJoin.enumeratedKeyFrontier` handles enumerability and copy its decline rule — if the composed view cannot enumerate exactly, decline to LMDB rather than under-report.

Parity corpus (each a small red-test first): base-only data; adds after build (delta-only keys); removes after build (tombstoned keys must not resurface); duplicate edges in multiple named graphs (context multiplicity preserved); GRAPH-scoped query declines to LMDB; dirty write transaction declines (until M3); supernode runs (>65k edges for one key — generator loop, exercises the chunk-directory codec); empty predicate (exact-empty, zero rows, still a "serve"); flag off → old behavior byte-identical.

Acceptance: `AdjacencyQueryShapeBenchmark.fullPredicateScan` is at parity with or faster than flag-off LMDB in fresh paired runs; census rows `full_predicate_scan`, `star_join`, `chain_join_open`, `object_object_join`, `exists_semijoin` show `ROOT_SCAN=0` with hits > 0; full parity corpus green; `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` shows no new failures.

### Milestone 2 — Doubly-bound probes on the iterator path

Scope: `<s> :p <o>` iterator requests are served by run lookup + `lowerBound` seek instead of declining DOUBLY_BOUND. `tryCountInternal` (`LmdbDirectAdjacencyStore.java:1828`) already implements exactly this arithmetic for counts — mirror it.

Work: failing test first (a doubly-bound `statements(...)` call increments `lookupHits`, not `fallbacks(DOUBLY_BOUND)`, results identical incl. multi-context multiplicity: the same (s,o) edge in three graphs yields three quads). Implement inside `open(...)` branch handling: when predicate and BOTH endpoints are bound (and context unrestricted or bound — a bound context narrows the run scan), resolve the subject's outgoing run, `lowerBound` to the object, emit the contiguous (neighbor==object) context rows. `NOT_FOUND` on the key or object → exact-empty serve. Inlined objects are legal here (the run stores raw ids; no key-domain involvement), but add a red test for an inlined-literal object anyway. Flag `rdf4j.lmdb.directAdjacency.boundProbe.enabled`.

Acceptance: `AdjacencyQueryShapeBenchmark.doublyBoundProbe` improves or holds (this is a latency shape; the gate is no-regression plus counters — DOUBLY_BOUND=0 in the census `point_probe` row); parity corpus green.

### Milestone 3 — Precise read-your-writes gate

Scope: today `storeTxnStarted.get()` (checked in `open`, `adjacency`, `tryCount`/`tryHas`/`exactDegree` gates — grep `storeTxnStarted` in `LmdbDirectAdjacencyStore`) declines EVERY lookup while any write transaction is open on the store, even before it has written anything. Refine to: decline only once the transaction has actually recorded a statement change.

The seam: `TripleStore` tees every write into the adjacency commit delta via `recordAdd`/`recordRemove` (around `TripleStore.java:3313-3325`, hook installed at `LmdbSailStore.java:837`). Add a "dirty" flag that flips on the first recorded change of the active write transaction and resets on commit/rollback (find the reset by following where the commit delta is drained — `LmdbSailStore` around `:1914`, `drainDirectAdjacencyCommitDelta` — and where rollback discards it). The gate becomes `storeTxnStarted.get() && txnDirty.get()`, behind `rdf4j.lmdb.directAdjacency.cleanTxnReads.enabled`.

Danger to respect (write the red test for it FIRST): the decline exists so a transaction reading its own uncommitted writes cannot get stale answers. The red test opens a write transaction, adds a statement, and asserts the same-transaction read sees it (this must pass before AND after — it passes today via the LMDB fallback; after the change the dirty flag forces the same fallback). A second test: open a write transaction, do NOT write, run a lookup, assert `lookupHits` increments (fails before the change — that is the milestone's failing test). A third: write, rollback, then read on the same connection — served again (flag reset). Concurrency check: the flag must be the same `AtomicBoolean` family as `storeTxnStarted` (one writer at a time holds the store's write lock — confirm by reading how `storeTxnStarted` itself is set/cleared, and set the dirty flag at the same ownership level).

Acceptance: the three tests above; census run with a wrapping open-but-clean transaction shows served lookups; full lmdb module verify green. Gate: counter proof plus no-regression (no benchmark gate — this is an eligibility fix).

### Milestone 4 — Row-path adjacency for parallel query workers

Scope: `ParallelSnapshotSource` (`LmdbSailStore.java:3082`), the per-worker source used by intra-query parallelism, never calls `tryDirect` — its `statements(...)`, `count`, `has`, `exactDegree` all go straight to LMDB, so a parallelized query silently loses everything milestones 1-3 add. Only `newProbe().adjacency(...)` (`:3313-3324`) was fixed previously (per-sibling refcounted view acquired at the parent view's proven revision — read that block first; it is the pattern to reuse, including the `isExact` + captured-transaction-version fence).

Work: failing test first: a query shape that (a) parallelizes (see how `LmdbWcojDirectFrontierTest`'s `parallelWorkersServeFrontiersFromDirectAdjacency` forces parallelism — reuse its technique) and (b) performs bound-endpoint lookups in workers; assert `lookupHits` increments proportionally to worker lookups (fails today). Implement: route `ParallelSnapshotSource.statements(...)` (both overloads), `count`, `has`, `exactDegree` through the same serve logic as `tryDirect`, using the sibling view + the same map-resize fence (`txn.version() != pinnedTxnVersion` → decline MAP_RESIZE_INVALIDATED). Flag `rdf4j.lmdb.directAdjacency.parallelRowPath.enabled`. Careful: worker threads are not the dataset's owning thread — verify `LmdbAdjacencyReadView` is documented thread-confined or refcounted-shareable by reading its class comment, and if confined, acquire one view per worker exactly as the kernel-path fix does.

Acceptance: the parallel census/benchmark shapes (`fullPredicateScan` at 5000 people parallelizes; verify via M0 counters) keep their M1 wins when parallelism engages; `ParallelismBenchmark` (existing, same package) shows no regression >5% in three paired runs; parity corpus green under forced parallelism.

### Milestone 5 — Per-plane accounting and scan-aggregate pushdown

Scope: the analytics family. Three sub-steps, each Routine A.

(5a) Plane accounting: maintain, per plane, the exact quad count and distinct-key count, established at base build and updated at every delta apply and consolidation. The build already sizes everything exactly (the sizing passes in `LmdbPagedCsfBaseBuilder`), so base numbers are free; delta application (`LmdbAdjacencyDeltaApplier`) knows each replacement run's old and new sizes — thread the difference through. Store the numbers in the published state (`LmdbAdjacencyPublishedState`) so they are snapshot-consistent per view. Red test: after build, plane count equals the store's actual statement count for that predicate/direction; after an add and a remove, still exact. This is the "complete-plane accounting" whose absence is documented at `LmdbDirectAdjacencyStore.java:1809-1813` (`meanFanOut` declines "until Milestone 8" — that comment refers to plan 27's numbering, not this plan's).

(5b) Adjacency-backed distinct/degree runs: `SELECT DISTINCT ?s { ?s :p ?o }` is the key domain verbatim; `SELECT ?s (COUNT(?o) ...) GROUP BY ?s` is keys × run sizes. Serve both by giving the native engine an adjacency-backed alternative where it currently plans prefix runs: find the consult point by tracing who calls `prefixRunPlan` (grep `prefixRunPlan` in `core/sail/lmdb/src/main/java/` — `LmdbSailStore` around `:4185` and `evaluation/` callers) and add an adjacency branch that yields the same row contract from `keyAt` enumeration (+ `exactDegree` for the count column). Eligibility: flag on, view exact, predicate resolved, subject-keyed (DISTINCT over objects stays on LMDB until the inlined-object story below), context unrestricted. Red tests: engagement counter + multiset parity incl. delta-only and tombstoned keys.

(5c) COUNT and predicate analytics: `SELECT (COUNT(*)) { ?s :p ?o }` binds the plane total from 5a (consult point: the native aggregate path — start from how `LmdbNativeGroupStep`/the aggregate compiler recognize a single-pattern COUNT, or serve it as a one-row adjacency "scan" if that is smaller); `SELECT DISTINCT ?p { ?s ?p ?o }` enumerates `LmdbAdjacencyPredicateCatalog`; the predicate histogram sums plane totals per ordinal. The unbound-predicate shapes need a NEW consult because nothing calls `tryDirect` for them today (see Surprises) — put it at the same rung where the native engine currently chooses the root-scan/prefix-run plan for unbound-predicate patterns. Eligibility extra: FULL coverage only (SELECTED coverage cannot answer store-wide analytics), explicit-only queries (inferred plane totals are separate — add them only when the query includes inferred statements; if the store has an inferencer stacked, decline).

Inlined-object caution (applies whenever an OBJECT-side key domain would be enumerated): incoming planes cover inlined object keys through `ImmutablePagedQuadCsfIndex.KeyDomain`, a separate exact index. Object-side enumeration must merge both key sources or decline. In this milestone, DISTINCT/degree over objects simply declines to LMDB; leave a red test documenting the decline so the follow-up is pinned.

Acceptance: `distinctSubjects`, `degreePerSubject`, `countOnePredicate`, `predicateHistogram` are each at parity with or faster than their flag-off LMDB measurements in paired runs; ANALYTICS theme queries q1/q2/q11 (`--theme-query ANALYTICS:1` etc. via `ThemeQueryBenchmark`) are no slower, and census shows the whole family served with zero ROOT_SCAN.

### Milestone 6 — Exact optimizer statistics

Scope: with 5a's accounting, `meanFanOut` (`LmdbDirectAdjacencyStore.java:1809`, currently a hard `OptionalDouble.empty()`) returns exact quadCount/keyCount per plane, and `LmdbSailStore.fanOutMean` (`:3024-3033`) stops falling through to sampled `tripleStore.meanFanOut` for adjacency-covered predicates. Flag `rdf4j.lmdb.directAdjacency.plannerStats.enabled`.

Work: failing test: with the flag on, `fanOutMean` for a covered predicate returns the exact value (construct a store where sampling would err — e.g. one hub with 1000 edges among 1000 single-edge keys — and assert exactness). Then audit consumers before enabling: grep `fanOutMean`/`meanFanOut` in `evaluation/` — `PatternPlan.estimate`, join arbitration, and the sweep/hash admission formulas consume these estimates; exact numbers CHANGE PLANS. The acceptance is therefore corpus-level: run the theme plan-snapshot comparison (`QueryPlanSnapshotCli --all-theme-queries`, see `testsuites/benchmark/.../plan/QueryPlanSnapshotCli.java`) before/after and review every plan diff; any regressed cell >10% blocks default-enable. Expect churn: budget time to bisect plan flips with `LmdbThemeQueryRegressionIT`'s anchors.

### Milestone 7 — SIP semijoin masks from adjacency key domains

Scope: sideways information passing. Today a completed downstream hash build publishes a sorted `long[]` mask to upstream scans (`evaluation/LmdbNativeChunkPipeline`, `SipMask.tryCreate` / `publishCsrMask`); the alternative producer — "the set of keys that exist for predicate P" straight from the adjacency key domain — is dead plumbing: `NativeLmdbQuerySource.adjacencyCacheKeys()` defaults to null and nothing overrides it (`evaluation/NativeLmdbQuerySource.java:156`).

Work: replace the `long[]`-returning `adjacencyCacheKeys()` contract with a small domain-cursor interface (unsigned-ascending iteration + `seekAtLeast`; the join-strategy ExecPlan's Milestone 1 work item 6 sketched exactly this shape as `LmdbNativeIdDomain` — adopt that name and signature from `.agent/lmdb-join-strategy-execplan.md`, Interfaces section), implement it over `ImmutablePagedQuadCsfIndex.KeyDomain` enumeration, have `RetainedNativeProbe` supply it when a probe's pattern has an adjacency-covered predicate, and let `publishCsrMask`/`SipMask` consume the cursor instead of a borrowed array (binary-search membership → `seekAtLeast` membership). Failing test first: a two-pattern chain where the downstream predicate's key domain excludes most upstream rows; assert the upstream scan's emitted-row counter drops when the flag `rdf4j.lmdb.sip.adjacencyMasks.enabled` is on, with identical results. Delete the dead `long[]` variant in the same change (Routine B leg, hit proof via chunk-pipeline tests).

Acceptance: the chain-shape microbenchmark (add a method to `AdjacencyQueryShapeBenchmark` if none fits) shows a win on selective chains and NO regression on non-selective ones (mask consult must bail cheaply — copy the existing 8-consecutive-miss seekForward heuristic in `masksVerdict`); chunk-pipeline suite green.

### Milestone 8 — Property paths fully over adjacency

Scope: informed by M0's counters (do not start before M0's census attribution is recorded). The path engine (`evaluation/LmdbNativePathPlan`) already expands frontiers from a cached `NativeAdjacency` when `probe.adjacency(...)` returns one; the remaining LMDB legs are (a) the SEED step for an unbound start (an ordered root scan via `source.statements(order, UNKNOWN, predicate, UNKNOWN, context)` around `:755`), (b) `PathFrontierSweep`/`openStep` fallbacks whenever the adjacency view is null (write transactions — fixed by M3; parallel path workers construct their OWN probes, verify they benefit from M4's sibling views), and (c) whatever else M0's counters reveal.

Work: seed from key enumeration when the start variable is unbound (the key domain IS the set of subjects with that predicate — same iterator as M1), behind `rdf4j.lmdb.nativePath.adjacencySeeds.enabled`; then re-run the census and `PropertyPathReachabilityBenchmark` (exists in the benchmark package) plus `AdjacencyQueryShapeBenchmark.pathReachability` for the gate. If M0 shows expansions ALREADY served from adjacency, this milestone's remaining value is the seed + eligibility breadth; record whatever is found in Surprises and resize the milestone honestly (shrinking it is success, not failure).

Acceptance: path counters show zero sweep/cursor expansions on adjacency-covered predicates in a read-only transaction; `pathReachability` paired-run gate; path parity corpus (cycles, `*` vs `+` semantics, GRAPH scoping, zero-length paths) green — reuse the existing path tests as the corpus, plus red tests for any new seed path.

### Milestone 9 — Evidence-gated new operators

Do not start any of these without the stated evidence; each is its own Routine A mini-arc with its own flag when funded.

- Run-intersection semijoin: FILTER EXISTS sharing one variable between two adjacency-covered patterns = galloping intersection of two sorted runs (the leapfrog `Level.enter` machinery outside leapfrog). Gate: census/theme telemetry showing EXISTS shapes on the nested-loop path with adjacency-covered predicates and material time share.
- Degree binding: `?s (COUNT(?o) ...)` bound directly from run sizes without expansion — largely delivered by 5b; the remainder is recognizing the shape inside larger queries. Gate: theme telemetry showing degree subpatterns inside bigger plans.
- Bidirectional path search: expand `:p+`/shortest-path from both ends (both planes exist), meet in the middle. Gate: M8 done and a workload where forward-only BFS visits ≥10× the meet-in-middle frontier (construct in `PropertyPathReachabilityBenchmark`).
- Exact-empty plan pruning: `NOT_FOUND` on a bound endpoint at plan time kills the containing join subtree before execution (today it only serves an empty iterator at run time). Gate: a demonstrated workload where empty-branch detection saves planning/startup work — check `PatternPlan.estimate` first; if estimates already yield zero there, this is dead — record and drop.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

Before any session: the root install (command in Context and Orientation). Then reproduce the current census:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyUsageCensusTest --retain-logs

Expected (2026-08-03 state, abridged — the full table is in `initial-evidence-adjacency-census.txt`):

    lookup_out(s bound)      3  adjHit=1                       -
    point_probe(s+o bound)   1  adjHit=0   DOUBLY_BOUND=1
    full_predicate_scan   6000  adjHit=0   ROOT_SCAN=1
    distinct_subjects     2000  adjHit=0   ROOT_SCAN=1
    triangle_wcoj         2000  adjHit=10010 (frontiers)       -
    all_predicates           5  adjHit=0   (no fallback recorded)

Per milestone: red test via `python3 .codex/skills/mvnf/scripts/mvnf.py <TestClass>#<method> --retain-logs`, persist the failing snippet with `python3 scripts/agent-evidence.py --command "<cmd>" core/sail/lmdb/target/surefire-reports > initial-evidence-<topic>-red.txt`, implement, re-run to green (`evidence-<topic>-green.txt`), then the neighbor suites named in the milestone, then paired benchmarks:

    ./scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.AdjacencyQueryShapeBenchmark --method 'fullPredicateScan$' [--jvm-arg -Drdf4j.lmdb.directAdjacency.rootScan.enabled=true]

Always anchor the method regex with `$` (an unanchored prefix silently mixes benchmarks into one run — this burned the WCOJ baseline once). Interleave off/on runs three times each; judge only disjoint intervals. A stale zero-byte `/var/folders/.../T/jmh.lock` silently aborts runs — delete it if a run produces no output. Store outputs as `benchmark-results/<milestone>-<method>-{off,on}-r{1,2,3}-<date>.txt`.

Formatting and headers before every commit:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    (cd scripts && ./checkCopyrightPresent.sh)

Branch/commit labels: reuse the current branch's convention; without an issue number use `GH-0000 <imperative summary>` and note the missing number in the handoff.

## Validation and Acceptance

Global rules, every milestone: exact multiset parity against the generic evaluator (`-Drdf4j.lmdb.nativeQueryEngine.enabled=false` produces the oracle rows; compare as multisets, order-insensitive unless the query has ORDER BY); the census test keeps passing with row counts unchanged; `python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb` introduces no new failures relative to the branch baseline (one known pre-existing failure exists: `LmdbThemeQueryRegressionIT.medicalPatientsWithMedsOrObservationsExcludingCodeAvoidsUnboundLeftGuards`, a sketch-planner assertion owned by other-branch work — do not chase it); default-enabling any flag requires fresh paired evidence that every benchmarked query and use case is at parity with or faster than flag-off LMDB. Noisy or contended cells get longer 5×10s-class runs before judgment.

## Idempotence and Recovery

Every milestone is additive behind a property and keeps an explicit rollback value; M1-M4 are default-on after passing their gates and setting their properties to `false` restores the prior path. Benchmark baselines use dated filenames and are never overwritten. If offline Maven resolution fails, rerun the exact command once without `-o`, then return offline. If a full-module verify fails on a test you did not touch, exonerate your changes with park-and-restore A/B (copy modified files aside, restore HEAD versions via `git show HEAD:<path> > <path>`, re-run, restore) before assuming causation. The ~5.5GB ThemeQueryBenchmark store under `core/sail/lmdb/target/lmdb-theme-query-benchmark` survives `clean` (pom-protected) — never delete it. Never revert user-owned uncommitted modifications encountered in the tree.

## Artifacts and Notes

Existing evidence: `initial-evidence-adjacency-census.txt` (census table, 2026-08-03), `initial-evidence-adjacency-shapes-smoke.txt` (benchmark smoke test). The census harness itself documents the counter-reading idiom (snapshot before, run, snapshot after, print deltas) — copy it for new engagement tests rather than inventing another.

## Interfaces and Dependencies

No new external dependencies anywhere in this plan. New/changed internal surfaces (final names may be refined; record refinements in the Decision Log):

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyRootIterator.java` (M1): a `RecordIterator` over (key domain × resolved runs) for one (predicate, plane), delta-aware via the store's existing `resolveRow`, emitting quads in (subject, object, context) unsigned raw-id order.

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeIdDomain.java` (M7, name shared with the join-strategy plan):

    interface LmdbNativeIdDomain extends AutoCloseable {
        long cardinalityUpperBound();
        boolean advance();
        boolean seekAtLeast(long id);
        long current();
        void close();
    }

Per-plane accounting (M5a) surfaces on `LmdbAdjacencyPublishedState` (exact `quadCount(plane, predicateOrdinal)` and `keyCount(plane, predicateOrdinal)`), consumed by `tryCount` (whole-plane case), the M5c aggregate fast path, and `meanFanOut` (M6).

System properties introduced: `rdf4j.lmdb.directAdjacency.rootScan.enabled`, `rdf4j.lmdb.directAdjacency.boundProbe.enabled`, `rdf4j.lmdb.directAdjacency.cleanTxnReads.enabled`, and `rdf4j.lmdb.directAdjacency.parallelRowPath.enabled` are default-on after passing their gates; the remaining milestone properties stay default-off until their own gates pass: `rdf4j.lmdb.directAdjacency.scanAggregates.enabled`, `rdf4j.lmdb.directAdjacency.plannerStats.enabled`, `rdf4j.lmdb.sip.adjacencyMasks.enabled`, `rdf4j.lmdb.nativePath.adjacencySeeds.enabled`.

---

Revision note (2026-08-03, Claude): Initial authoring from the adjacency consumption survey (census evidence of the same date) with all seam anchors re-verified against HEAD of `optimize-lmdb`. Survey item 5 (node edge dumps / CSF predicate enumeration) excluded per user instruction; items 1, 2, 3, 4, 6, 7, 8, 9 mapped to milestones 1, 2, 3+4, 8, 5, 6, 7, 9 respectively, with a new Milestone 0 added because the census cannot currently distinguish "kernel path served but unmetered" from "not served" (see Surprises).

Revision note (2026-08-03, Codex): Recorded Milestone 0's pre/post census evidence, counter semantics, copyright-checker baseline, and the discovery that bounded-start path expansion is already fully adjacency-backed; per-method benchmark baselines remain.

Revision note (2026-08-03, Codex): Completed Milestone 0 after capturing all 11 isolated `AdjacencyQueryShapeBenchmark` baselines; summarized scores in `evidence-adjacency-baselines.txt`.

Revision note (2026-08-03, Codex): Implemented and profiled M1. Replaced the rejected lookup-per-key iterator with a
sequential paged-CSF cursor and fused pair decoding; recorded the output-materialization ceiling and retained the
default-off gate because the required 1.5x end-to-end result-drain speedup was not met.

Revision note (2026-08-03, Codex): Applied Håvard's acceptance override: features may default-enable when all benchmarked
queries and use cases are at parity with or faster than LMDB. The earlier 1.5x threshold is historical, not binding.

Revision note (2026-08-03, Codex): Completed and default-enabled M1 after three fresh primary pairs plus affected
DISTINCT/COUNT/degree checks all measured at parity or faster; retained `...rootScan.enabled=false` as the kill switch.

Revision note (2026-08-03, Codex): Completed and default-enabled M2 after focused red/green coverage, census proof,
and three exact-parity `doublyBoundProbe` benchmark pairs.

Revision note (2026-08-03, Codex): Completed and default-enabled M3 with a shared atomic first-mutation marker,
writer-transaction safety coverage, rollback lifecycle coverage, the 37-test direct-query corpus, and a clean-writer
wrapped 19-shape census.

Revision note (2026-08-03, Codex): Completed and default-enabled M4. Worker siblings now use their own exact,
version-fenced adjacency views across the complete row surface; forced-parallel parity and proportional hit coverage are
green, and the optimized three-pair `parallelOverlap` mean is 1.5% faster than LMDB.
