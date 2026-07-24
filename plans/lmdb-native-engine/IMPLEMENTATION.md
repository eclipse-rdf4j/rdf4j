# Complete the LMDB native query engine program

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`,
and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance
with `.agent/PLANS.md` and implements the checked-in workstream specifications
`plans/lmdb-native-engine/00-overview.md` through `plans/lmdb-native-engine/13-verification.md`.

## Purpose / Big Picture

The LMDB query evaluator will choose compatible execution mechanisms by measured cost, compose filtering,
batching, parallelism, factorization, ordering, joins, aggregation, and paths where semantics allow, and
bound every query-side allocation through one memory authority. Completion is observable through the
fixed equivalence, dispatch, snapshot, lifecycle, and benchmark gates in `13-verification.md`, with every
program-level target recorded in `ACCEPTANCE.md` and every behavior-risk switch recorded in `ROLLOUT.md`.

## Progress

- [x] (2026-07-19 20:41Z) Read `.agent/PLANS.md` and all fourteen workstream documents completely.
- [x] (2026-07-19 20:41Z) Ran the mandatory root `-Pquick clean install`; all reactor modules succeeded.
- [x] (2026-07-19 20:48Z) Audited plan-defining symbols against the branch; the workstreams remain open.
- [x] (2026-07-19 21:26Z) Restored repeated-probe factorized admission with a paid-probe floor and
  captured focused red/green evidence.
- [x] (2026-07-19 21:28Z) Restored Maven workspace output isolation and proved two concurrent LMDB
  focused tests leave the legacy module `target` untouched.
- [x] (2026-07-19 21:50Z) Admitted parallel MIN/MAX(DISTINCT) while preserving exact-result parity and
  encounter-order fallback for comparator-equal, RDF-term-distinct extrema.
- [x] (2026-07-19 22:18Z) Completed parallel SUM/AVG(DISTINCT) by deferring arithmetic to the merged
  distinct-ID set, with cross-worker duplicate parity and late floating-point fallback.
- [x] (2026-07-20 04:40Z) Froze plan 13 §1.5's 24 LMDB-only compliance failures and added a fail-closed
  Failsafe report gate; the current 17-failure subset passes with no new identities.
- [x] (2026-07-20 04:46Z) Unified plan 02 §1's chunk and batch filter selection behind one vectorized
  kernel, with per-store bounded specialization, direct ID-column filters, and hit/miss/execution counters
  exposed through `LmdbNativeAttemptMetrics`.
- [x] (2026-07-20 04:52Z) Completed plan 08 §3's exact-width native varint loads; focused parity is
  green and paired JMH reports 1.053 ms/op versus 3.047 ms/op for the byte-at-a-time baseline.
- [x] (2026-07-20 08:26Z) Implemented plan 02 §2's dispatch and correctness contract: absorbed filters
  survive merge/hash admission, and hash build-cap fallback applies and closes each filter exactly once.
  Focused join and decline-telemetry tests are green; the 1M-triple performance threshold is unmeasured.
- [x] (2026-07-20 09:50Z) Completed plan 02 §3's sound decode-free comparison path: constants fold once,
  ordered slot pairs and constants stay on raw IDs, other types use the materializing fallback, boundness
  proofs remove UNKNOWN checks, and guarded scalar coverage preserves three-valued semantics. Compiler,
  native/generic equivalence, and neighboring filter-specialization tests are green; allocation-profiled
  acceptance remains unmeasured.
- [x] (2026-07-20 09:08Z) Added plan 13 §2's aggregation benchmark axis with individually targetable
  many-group, few-group, DISTINCT, six-key, and spill-candidate methods; its fixed digest smoke is green,
  while forced-spill wiring remains pending plan 10 §1's production budget control.
- [x] (2026-07-20 10:17Z) Added plan 13 §2's numeric range benchmark axis with direct native/generic
  selectors at 0.1%, 1%, and 10%; fixed digest parity and plan engagement are green, while date-v2 and
  the 10M-row/10x performance acceptance remain explicitly unmeasured.
- [x] (2026-07-20 09:36Z) Completed plan 03 §1 by replacing per-row tuple/group/DISTINCT atomic writes
  with thread-confined counters and idempotent publication at table, sort, and cursor ownership boundaries;
  the focused and neighboring 18-test selection is green.
- [x] (2026-07-20 09:50Z) Completed plan 07 §1's CSR zone-map rejection, O(1) prefix-sum degree,
  build-order-guarded binary count/existence, and run-level seek; the exact red/green selector and the
  35-test CSR/range/iterator neighbor selection are preserved, while performance acceptance is unmeasured.
- [x] (2026-07-20 11:14Z) Completed plan 07 §2's direction-matched ordered CSR scans, exact build-emission
  order proof, merge-seekable iterator metadata, and read-lock wrapper delegation; both cache directions,
  mixed contexts, fallback, merge dispatch, and ordering neighbors are 82/82 green, while throughput is
  unmeasured because no paired JMH run was made.
- [x] (2026-07-20 12:18Z) Completed plan 07 §3 by publishing a CSR-served probe stage's immutable sorted
  key vector as one borrowed root SIP mask while continuing to refuse its redundant hash memo. Root
  `MASK_SKIP`, zero hash-build-count, native/generic parity, and borrowed/owned lifecycle coverage are
  68/68 green; no paired SIP benchmark was run, so throughput acceptance remains unmeasured.
- [x] (2026-07-20 16:02Z) Completed plan 07 §4 with a heap-proportional entry cap, a per-store intrusive
  second-chance clock, exact commit/close byte credit, and process-local admission/refusal metrics. The
  21-test cache class, six-test store lifecycle class, and focused metrics test are green; large-heap
  cache-effectiveness timing remains unmeasured.
- [x] (2026-07-20 17:28Z) Completed plan 07 §5 with borrowed half-open CSR dense-key slices whose
  boundaries use exact `runStart` pair counts, including same-snapshot and composite-source payload routing.
  The skew fixture yields four exact 16-row partitions and four in-memory CSR scan dispatches; the focused
  test and 27-test CSR/range neighbor selection are green, while throughput remains unmeasured.
- [x] (2026-07-20 09:50Z) Implemented plan 08 §1's CSR-gated ordered-integer range lowering and added
  its default-on `rdf4j.lmdb.native.rangePushdown` rollout switch; boundary, mixed-type refusal,
  disabled-switch, and generic-parity tests are green, while range performance is unmeasured.
- [x] (2026-07-20 10:18Z) Completed plan 01 §2's bounded maintained predicate fan-out statistics and
  CSR exact/mean sources, including promotion-safe write accounting and chained native estimates; the
  focused estimator/write-path selection is 10/10 green, while estimator overhead remains unmeasured.
- [x] (2026-07-20 10:21Z) Completed plan 01 §1's frozen execution-path vocabulary and query-local
  winning/decline trace, including merge, hash, parallel, factorized, batch, and generic ladder reasons;
  dispatch, Explain, and adaptive-neighbor coverage is 40/40 green.
- [x] (2026-07-20 10:41Z) Completed plan 01 §4: only short finite slices decline parallel admission;
  ORDER BY top-K now uses width-aware lazy memory admission, bounded reducing spill with safe boundary
  filtering, and an array-safe overflow fallback. The focused 16-test pipeline and 7-test sort classes
  are green; the million-row performance acceptance remains unmeasured.
- [x] (2026-07-20 10:42Z) Added plan 13 §2's parallelism benchmark axis over one fixed C∩D fixture:
  sequential, two-query, four-query, serial hash-batch, and parallel-overlap methods are individually
  targetable; metadata and deterministic engagement/digest smoke tests are green, while both timing
  acceptance ratios remain explicitly unmeasured.
- [x] (2026-07-20 10:50Z) Completed plan 03 §2 with flat primitive hash-join keys, stored full hashes and
  fingerprints, and owner-confined batch scratch for separate hash, head-load, and collision-chain passes.
  Tuple, DISTINCT, EXISTS, and hash-join consumers retain exact bag/order behavior; the focused and
  neighboring 46-test selection is green, while the 16M-build and proportional-scaling gates are unmeasured.
- [x] (2026-07-20 11:12Z) Added plan 13 §2's ordering benchmark axis with individually targetable
  full-sort, top-K, radix-eligible, expression-key, and order-elimination methods. Fixed ordered digests,
  selector plan evidence, and shared JMH metadata are green; timing and allocation targets are unmeasured.
- [x] (2026-07-20 11:28Z) Added plan 13 §2's remaining hash-join, materialization, and correlated
  benchmark axes: six build/probe/mark/anti-join selectors, fully inspected SELECT/TripleTerm boundaries,
  and OPTIONAL/path-under-join workloads. Direct deterministic smoke and shared JMH metadata are green;
  no timing or allocation acceptance claim has been made.
- [x] (2026-07-20 11:32Z) Completed plan 06 §4's enum-refusal recovery: a failed materialization now
  falls back only for the offending key, using a lazy 4,096-entry primitive negative memo with clock
  eviction. The focused red/green, capacity-eviction contract, full 43-test owner class, and 56 lifecycle/
  ordering neighbors are green; the skewed-dataset timing acceptance remains unmeasured.
- [x] (2026-07-20 11:45Z) Completed plan 11 §1's repeated-start path sharing: uncorrelated paths use
  existing join materialize/replay, while correlated paths use an evaluation-local primitive memo keyed
  by resolved start and direction with bounded entry/value reservations and exact refusal fallback. Bag,
  encounter-order, backward, snapshot, telemetry, and cleanup coverage is 59/59 green; timing and
  allocation-profile acceptance remain unmeasured.
- [x] (2026-07-20 11:51Z) Completed plan 04 §1's elastic task admission: range queries receive the
  largest available worker group down to one, morsel queries require at least two workers, and their
  producer runs cooperatively on the query thread without consuming a pool reservation. Row and aggregate
  paths use and report the actual grant; admission, concurrent degradation, caller-thread ownership,
  feature-flag, partition, and lifecycle coverage is green, while 8-core latency acceptance is unmeasured.
- [x] (2026-07-20 13:47Z) Completed plan 04 §2's plan-level work admission: the chained estimate is
  compared with the measured startup-work intercept, bag-safe VALUES children are admitted, and each
  multi-value worker owns its fallback memo/value cache while resolving through its worker source. The
  focused reds and 121-test parallel/priority/lifecycle selection are green; performance is unmeasured.
- [x] (2026-07-20 16:50Z) Completed plan 04 §3's external-root worker vectorization after plan 02 §4's
  per-worker adaptive ownership landed: row and aggregate workers now batch their non-factorized prefix by
  default, with literal `false` on the existing property retaining the scalar fallback. Default/kill-switch,
  refill, restriction, adaptive, flag, startup-failure, and cleanup coverage is 124/124 green; the 1.3x
  speed and worker CPU-profile gates remain unmeasured.
- [x] (2026-07-20 17:07Z) Completed plan 02 §4's adaptive decorator: sessions can consume an externally
  supplied root, parallel workers own independent filter forks and leases, factorized ordinary suffixes
  reuse one non-owning session, and factorized plans retain the optimizer-selected initial depth. Focused
  seam, worker, suffix, depth, and deterministic four-worker overlap tests plus the 29-test owner class are
  green; movement timing against the sequential-adaptive baseline remains unmeasured.
- [x] (2026-07-20 17:31Z) Completed plan 10 §2's wide primitive group keys: the flat tuple table now
  supports every positive native-ID width, retains its unrolled one-to-four-key probes, and uses loop probes
  beyond four; every multi-key native group uses tuple COUNT/state storage and the boxed group-key mode is
  removed. Eight-key direct and query-level dispatch/parity tests plus the 10-test owner class are green;
  width-eight allocation and throughput acceptance remain unmeasured.
- [x] (2026-07-20 12:08Z) Completed plan 01 §3's row C∩D arbitration with single-use deferred batch
  and parallel proposals, merge-before-hash batch dominance, costs based on chained work and the actual
  elastic worker grant, losing-reservation release, and both-cost telemetry. Cost, dispatch, join,
  partition, snapshot, and lifecycle coverage is 52/52 green; the paired 8-core speed target is unmeasured.
- [x] (2026-07-20 13:47Z) Completed plan 01 §5's aggregate arbitration port: parallel aggregation and
  factorized tails now expose single-use proposals, compare serial leg work with total plan work over the
  actual elastic grant, report both costs and the losing reason, and cleanly fall back after a dynamic
  parallel refusal. Focused red/green plus 75 aggregate/lifecycle/proposal tests are green; performance is
  unmeasured.
- [x] (2026-07-20 12:13Z) Completed plan 02 §5's narrow factorization-sink filter rule: a filter tied
  to one prospective branch travels with it, a cross-branch filter vetoes every touched candidate, unknown
  masks remain conservative, +infinity estimates may sink, and the filtered fresh tail-group guard still
  refuses. Focused and 106 factorization/fuzz neighbors are green; theme timing is unmeasured.
- [x] (2026-07-20 12:31Z) Completed plan 11 §2's endpoint restriction and lazy all-pairs startup:
  bounded evaluation-local target sets from later VALUES/pattern leaves cross nested joins without owning
  bag multiplicity, reset their cutoff countdown per source, and refuse UNDEF or oversized sets exactly;
  all-pairs identity/start scans now yield after one raw row and retain a separate enumeration probe. The
  68-test path/join/row-step selection and expansion/start-scan counters are green; large-store latency is
  unmeasured.
- [x] (2026-07-20 17:22Z) Completed plan 11 §3's sorted frontier BFS: each level is an unsigned-sorted
  primitive run, discovered IDs live in log-structured sorted tiers, ordered LMDB access uses forward-seek
  sweeps, revision-valid CSR entries expose direct adjacency slices, and wide levels use worker-confined
  same-snapshot sources with a per-level barrier and balanced sorted merge. The 41-test path class and 59
  CSR/row/join neighbors are green; the one-million-node ≥3× and near-linear parallel gates are unmeasured.
- [x] (2026-07-20 18:55Z) Completed plan 11 §§4–5: compatible constant-predicate `Union` trees compile
  to compact predicate/direction step arrays consumed by every serial and parallel frontier path, while
  direct same-variable algebra retains native cycle filtering. Path costing now combines direction-aware
  mean fan-out through a capped closure model and uses exact sizes from completed evaluation-local memos.
  The 46 path/planner tests and 35 row/join/fan-out neighbors are green; performance remains unmeasured.
- [x] (2026-07-20 17:51Z) Added plan 12 §1's process memory-authority foundation: exact global and
  per-query byte ledgers, configurable heap-derived ceilings, immediate refusal, LRU spill-before-shed
  reclamation, grow/partial-release operations, and cursor-close leak cleanup. The focused concurrent
  six-test contract is green; operator/CSR wiring and the corpus-wide zero-leak gate remain open.
- [x] (2026-07-20 18:05Z) Closed plan 12 §2's unbounded-single-entry exposure: an initialized value with
  a lexical form above one Mi-character is refused by the shared ID-to-value cache, while resolution and
  ordinary cache hits remain unchanged. The exact 100 MiB red/green and two-test cache class are preserved;
  adaptive byte-sized defaults and dictionary hit-rate telemetry remain open.
- [x] (2026-07-20 19:54Z) Completed plan 05 §§1–3: ordered materializers now reuse the ordinary
  batch/parallel/factorized/adaptive producer ladder after the ordered-factorized attempt; sort rows map
  plan slots into a key-first live subset; and spill merge compares a contiguous key block while deferring
  payload decoding. Full producer, packed-sort, row-stream, ordered-factorized, Explain, and benchmark
  smoke selections are green. Core scaling and paired wide-row/profile acceptance remain open.
- [ ] Build the remaining continuous verification corpus.
- [x] Complete Phase I: 01§1-2, 02§1-3, 03§1-2, 04§1, 07§1-3.
- [ ] Complete Phase II: 01§3-4, 02§4, 04§2-3, 05§1-3, 06§1-2, 07§4-5, 10§1-2.
- [ ] Complete Phase III: 04§4-5, 05§4-5, 06§3-5, 08, 09§1-3, 10§3-5, 11, 12.
- [ ] Complete Phase IV: 09§4-6 and finish wiring the global memory authority.
- [ ] Run the program exit review and close every acceptance and rollout ledger entry.

## Surprises & Discoveries

- Observation: The initial repository-wide quick build succeeds on the active JDK 25 runtime.
  Evidence: `maven-build.log` ends with `BUILD SUCCESS` after all reactor modules, including LmdbStore.
- Observation: The workstream documents describe the current branch accurately; marker audits found the
  old all-or-nothing task reservation, global slot-count-only specialization cache, factorized-only
  multiplicity, dead CSR zone-map fields, and no acceptance or rollout ledgers.
  Evidence: `rg` matches remain at `LmdbNativeParallelPipelines.tryReserveTasks(int)`,
  `LmdbNativeSpecialization.CACHE`, and `LmdbNativeFactorizedRows.ordinarySuffixOnly`, while searches for
  `StrategyProposal`, `LmdbFanOutStats`, and `LmdbQueryMemoryManager` return no production definitions.
- Observation: The initial full `core/sail/lmdb` verify has one ordinary suite failure in the in-flight
  runtime-admission work: 1,024 completed paid probes do not admit the expected bounded factorized trial.
  Evidence: `initial-evidence.txt` preserves the failure from
  `LmdbNativeChunkHashBuildTest.factorizedAdmissionCountsRepeatedCompletedProbesTowardTheObservationFloor`.
- Observation: The workspace-aware runners were present, but the active root POM lacked the
  property-activated `workspace-build-root` profile, so concurrent workspace IDs still shared ordinary
  module `target` directories and could remove one another's compiled tests.
  Evidence: the first agent runs failed before Surefire with missing test classes; the restored profile's
  two-workspace live check placed both reports under distinct `.mvnf/workspaces/*/build` roots while the
  legacy `core/sail/lmdb/target` modification time remained unchanged.
- Observation: `09-joins.md` contains a sixth bushy-build work item that the initial phase checklist
  omitted even though the immutable numbered plan and acceptance gates require it.
  Evidence: the storage/join audit found 09§6 open; Phase IV above now tracks 09§4-6 explicitly.
- Observation: The historical full SPARQL compliance run has exactly 24 LMDB-only failures, while the
  current isolated run has 17 failures and every one belongs to that historical set.
  Evidence: `logs/mvnf/20260718-144033-verify.log` records 2,648 tests with 24 failures at HEAD
  `42e6ab6f2e`; workspace `agent-compliance-baseline` records the same 2,648 tests with 17 failures,
  and `check-lmdb-compliance-baseline.py` reports 17/24 remaining with all five LMDB suites present.
- Observation: CSR run order is a property of the actual predicate-sweep index, not the configured
  direction alone: a fixed-key subsequence is binary-searchable by `(neighbor, context)` only when the
  index places the neighbor field before context; `pocs` violates this for BY_OBJECT runs.
  Evidence: `nonNeighborOrderedBuildKeepsLinearFallbackParity` observes the unsorted `[20,30,10]` run,
  refuses seek without consuming a row, and matches 500 randomized linear-reference probes.
- Observation: An ordered CSR entry is safe only when the requested scan and its recorded build sweep
  agree on the complete sequence of varying quad fields; matching only `StatementOrder.S` or `.O` is
  insufficient when a custom index places context before the neighbor.
  Evidence: the mixed-context `pocs` case refuses the incompatible bound-subject scan, serves the
  compatible full object order, reports `pocs`, and preserves exact LMDB sequence parity.
- Observation: Passing measured fan-out into ordered-promotion costing exposed a mixed-domain comparison:
  one join order could use a measured degree while the other silently used pseudo-cardinality fallback,
  vetoing two established ordered aggregation plans even with CSR disabled.
  Evidence: the two isolated `LmdbNativeOrderedDistinctTest` failures reproduce with
  `rdf4j.lmdb.csrCache.enabled=false`; the cost-level regression is green after the gate uses measured
  costs only when both prefixes have direct estimates and rejects a promotion when coverage is asymmetric.
- Observation: A CSR-served stage already owns the complete sorted set of its legal probe keys, even though
  the old cache-detection branch refused the query-local hash build before that build could publish SIP.
  Evidence: `LmdbCsrSipMaskTest.hotCsrProbePublishesRootSipMaskWithoutHashBuild` records CSR-backed probes,
  a new root SIP mask and masked rows, an unchanged hash-build counter, and exact generic-result parity.
- Observation: `TxnRecordCache` records complete quad mutations but exposes no predicate write set, so
  plan 07 §4's optional predicate-selective commit invalidation cannot be implemented as its cheap half.
  Evidence: its public/package API has record store/remove/state/iteration operations but no written-predicate
  view; the plan-authorized drop-all path now credits every entry before `storeTxnStarted` re-enables lookups.
- Observation: Raw LMDB root splitting interpolates encoded key bytes rather than predicate-run row counts,
  so unrelated ID-space gaps can collapse requested partitions and hide severe degree skew.
  Evidence: `initial-evidence.agent-csr-partitioned.txt` records the skewed 64-row hub producing only two of
  four requested raw `posc` partitions; the warmed CSR prefix sums produce four 16-row slices.
- Observation: The range-pushdown disabled-switch test exposed that the property contract existed in
  the test but the lowering entry point did not consult it.
  Evidence: `initial-evidence.agent-csr-zone-map.txt` records the optimized plan unexpectedly containing
  `range=posc`; the matching 35-test post selection is green after the compiler guard.
- Observation: `STRSTARTS`, `STRENDS`, and `CONTAINS` were already compiled through the zero-copy value
  codec for both inline and stored strings, broader than plan 02 §3's inline-only anchor.
  Evidence: `LmdbNativeExpressionFilterTest.inlineStringPredicatesMatchGenericEvaluation` is green with
  native compilation and zero `lazyValue` calls, so the existing broader sound path was retained.
- Observation: Current parallel row admission exposes no query-local actual-worker metric, but a
  successful `parallelPipelines` path still implies the complete configured group was admitted.
  Evidence: `ParallelismBenchmark` exposes `workersPerQuery` as a JMH parameter and its concurrent
  telemetry probes observe a parallel path for every simultaneous query; plan 04 §1 must replace the
  configured count with granted-worker telemetry when elastic admission lands.
- Observation: Running the entire morsel producer synchronously during cursor startup can deadlock even
  though producer ownership is correct: bounded workers may fill their output queue before the consumer's
  first `next()` can drain it.
  Evidence: the final row cursor pumps at most one input morsel or available poison sequence from each
  query-thread entry, and the full cleanup class proves cancellation, producer/worker close, and failure
  suppression remain exact without a producer future or pool reservation.
- Observation: Plan 03 §1 had already made tuple/DISTINCT metric ownership explicit and the primitive
  tuple table already stored dense flat keys; the remaining batch hazard was that an insertion or growth
  can invalidate head candidates prepared earlier in the same input batch.
  Evidence: the tuple-table batch regression forces collision-heavy growth, while the production table's
  mutation version makes `findPrepared` reload a head whenever a prior row changed the table.
- Observation: `GenericPlanNode.getLongMetricActual` returns `null` when an execution-path counter is
  absent; benchmark telemetry walkers cannot safely unbox every requested metric.
  Evidence: the first `OrderByBenchmarkTest` run reached evidence capture, then failed while unboxing an
  absent sort metric; treating absence as zero made the same five-selector smoke green.
- Observation: enum scratch growth refusal already returned the full physical reservation, but the
  cursor-level `ordinarySuffixOnly` flag prevented every later key from attempting to reuse that budget.
  Evidence: the three-key regression probes keys 100 and 200, falls back for 200, then observes key 300
  incorrectly bypass the probe until the refusal state becomes branch-local.
- Observation: a path result cannot be sorted before replay without changing the native cursor's observable
  encounter order, even though reachability itself has set semantics; the existing `pending` array already
  retains the complete BFS discovery sequence after exhaustion.
  Evidence: the memo-enabled and forced-fresh repeated-start tests compare exact sequences and bag
  multiplicity, while refusal performs three fresh traversals with the same sequence.
- Observation: a retained native probe invalidates its previous iterator on the next `open`, so lazy
  all-pairs start enumeration cannot share the BFS probe while its store scan is suspended.
  Evidence: `allPairsStarReadsOneRowBeforeEarlyClose` observes one raw enumeration row before close, and
  the full native/generic path class remains green with a separately owned enumeration probe.
- Observation: costing parallel execution needs a real elastic grant, but opening a losing batch cursor
  would transfer close ownership of the plan filters and opening a losing parallel cursor would clone
  filters, sources, and tasks.
  Evidence: C∩D proposals now reserve only the worker count needed for the cost, defer every cursor-owned
  resource, release an unused reservation exactly once, and pass the batch-wins overlap plus lifecycle suites.
- Observation: cloning a multi-value plan is insufficient unless its fallback filter also stops capturing
  the compiler source and receives fresh memo and accepted-value caches for every worker.
  Evidence: the worker-fork regression observes independent memo misses and proves all lazy value resolution
  uses the worker `RowState.source`, while the original query source remains untouched.
- Observation: the factorization sink's unioned filter-read mask could not distinguish a filter local to
  one candidate branch from a filter coupling multiple branches, so it pinned both shapes in the prefix.
  Evidence: the focused red retained the filtered leg in the old order; branch-local mask accounting now
  sinks it with its filter while the explicit cross-branch and count-only-tail guards remain green.
- Observation: existing aggregate lifecycle tests used tiny COUNT shapes that were intentionally eligible
  for both factorization and parallelism, so correct cost arbitration stopped entering the parallel
  mechanics those tests meant to isolate.
  Evidence: the first owner-class run had eight result-correct failures only on `PARALLEL_RUNS` assertions;
  adding an otherwise-observed DISTINCT extrema aggregate made factorization ineligible while preserving
  every parallel cleanup, preflight, range, and flag contract.
- Observation: the external-root chunk chain already served factorized, ordinary row, and aggregate worker
  branches behind one shared gate, but owner setups forced the candidate on and encoded the old default-off
  contract; a one-row cancellation fixture also waited for a scalar probe before publishing end-of-input.
  Evidence: the default-on red observed zero chunk engagements, while the completed owner tests exercise
  both worker families and the repaired cancellation fixture performs the same query-thread producer pump
  as `next()` before cancelling at its intended blocked tail probe.
- Observation: once ORDER BY reuses the ordinary producer ladder, execution paths are necessarily
  compositions such as `factorizedRows | orderedTopK` or `parallelPipelines | orderedFullSort`; treating
  `nativeExecutionPath` as a single exact winner made otherwise-correct Explain tests fail.
  Evidence: the broad Explain red showed five exact-prefix assertions against composed telemetry; the
  updated 19-test class requires producer facts, ordered consumer facts, and retained substrate counters.
- Observation: a one-column full-sort profile cannot validate the plan 05 §3 wide-row load-locality target.
  Evidence: the 50,000-row CPU profile sampled `orderCompare` and `NativeSortBuffer.merge` at 0.16% each,
  but had no wide payload lanes or memory-load attribution; the acceptance row therefore remains open.

## Decision Log

- Decision: Execute the checked-in phase graph exactly, while constructing plan 13's harness before the
  first behavior-changing production milestone.
  Rationale: The harness is explicitly a Phase I prerequisite and is the only reliable way to detect
  semantic or dispatch drift across this unusually broad engine program.
  Date/Author: 2026-07-19 / Codex.
- Decision: Keep the sketch estimator and cascades-style planner code untouched.
  Rationale: `00-overview.md` makes this a binding scope exclusion; all costing and physical decisions
  are runtime-side.
  Date/Author: 2026-07-19 / Codex.
- Decision: Preserve the correctness-required exclusions enumerated in `00-overview.md`.
  Rationale: Worker merge/SIP restrictions, ordered DISTINCT adjacency, factorized key stability, GRAPH
  path exclusions, and related gates have verified semantic foundations and are not optimization gaps.
  Date/Author: 2026-07-19 / Codex.
- Decision: Repair and prove Maven workspace isolation before restarting parallel implementation work.
  Rationale: distinct workspace labels are not safe if Maven still writes shared `target` trees; the
  attached known-good POM supplied the missing profile and cross-module path indirections.
  Date/Author: 2026-07-19 / Codex.
- Decision: Reuse the existing default-on `rdf4j.lmdb.parallel.enabled` kill switch for parallel
  DISTINCT extrema rather than introduce a shape-specific property.
  Rationale: the change only removes an over-broad admission veto; extrema already allocate no DISTINCT
  channel, merge by exact comparator winner, and fall back when parallel encounter order could change
  the chosen RDF-term representative.
  Date/Author: 2026-07-19 / Codex.
- Decision: Defer parallel DISTINCT SUM/AVG arithmetic until result materialization, then fold the
  merged primitive ID set rather than merge per-worker arithmetic states.
  Rationale: unioning the existing worker sets removes cross-worker duplicates before arithmetic;
  integer and decimal addition is hash-order-safe, while the existing encounter-order fallback restarts
  floating-point inputs sequentially when the final fold decodes them.
  Date/Author: 2026-07-19 / Codex.
- Decision: Compare compliance failures by Failsafe's `class#test-name` identity, require reports for
  all five LMDB suites, accept only a subset of the frozen 24, and reject every unknown failure or error.
  Rationale: exception text is unstable and a raw failure-count ceiling misses identity replacement;
  report identities are deterministic, while required-suite checks prevent partial runs from passing.
  Date/Author: 2026-07-20 / Codex.
- Decision: Ship exact-width native varint loads without a feature flag.
  Rationale: the width/alignment corpus and the benchmark-baseline equivalence test prove bit-identical
  decoding, while paired 10-by-2-second JMH runs improve average time by 65.441%; plans 08 §3 and 13 §4
  explicitly classify this decode substitution as unguarded.
  Date/Author: 2026-07-20 / Codex.
- Decision: Record a build-derived `runsNeighborContextOrdered` bit on every CSR entry and use binary
  count, existence, and seek only when that proof holds; retain the linear/no-seek fallback otherwise.
  Rationale: exact-ID entry min/max rejection is sound for every value ID, but binary search requires raw
  run order; keeping this separate from `allNeighborsOrderedIntegers` preserves mixed-type exact probes
  while reserving that stronger flag for numeric range interpretation.
  Date/Author: 2026-07-20 / Codex.
- Decision: Persist the CSR build index plus run/scan emission order and serve an ordered request only
  when its planner-selected index has the same varying-field sequence; delegate the exact index name
  through store wrappers.
  Rationale: ordered merge, seek, and adjacent consumers require the full quad ordering contract, while
  an ordinary LMDB fallback remains correct for every unproved or custom-index shape.
  Date/Author: 2026-07-20 / Codex.
- Decision: Compare ordered-promotion work in one estimate domain: use measured fan-out when both candidate
  prefixes have direct estimates, structural fallback when neither does, and reject promotion when exactly
  one prefix has direct coverage.
  Rationale: this retains plan 01 §2's measured cost input without comparing a measured value on one
  side against an unrelated pseudo-cardinality on the other.
  Date/Author: 2026-07-20 / Codex.
- Decision: Expose CSR probe keys as a borrowed immutable vector only when one concrete adjacency entry owns
  every probe result; publish it at most once per chunk stage and leave composite/mixed ownership as
  {@code null}.
  Rationale: borrowing `keysByDense` avoids copying or sorting primitive key storage and charges no memo
  budget, while the single-entry ownership gate prevents an incomplete member vector from pruning valid
  composite rows.
  Date/Author: 2026-07-20 / Codex.
- Decision: Own one intrusive second-chance ring per CSR cache, mutate entry ownership only under that
  store's lock, and keep byte admission process-global through atomic accounting. Drop all entries after a
  successful commit because no predicate write set is exposed, and make close an idempotent full credit.
  Rationale: the ring removes the O(n) map scan and global serialization while immutable detached entries
  remain safe for in-flight readers; dropping before `storeTxnStarted` becomes false preserves snapshot
  correctness, and process-local admission/refusal snapshots let benchmarks observe policy decisions.
  Date/Author: 2026-07-20 / Codex.
- Decision: Represent a warmed root partition as a tagged borrowed `CsrEntry` plus a half-open dense-key
  span, choose each legal run-edge boundary nearest the equal share of remaining prefix-sum rows, and
  preserve that payload when composite sources remap member ordinals. Prefer BY_OBJECT for ordinary root
  emission, fall back to BY_SUBJECT when it is the available immutable entry, and retain raw range planning
  on every cache miss or disabled root-scan switch.
  Rationale: the partition plan copies no key/pair arrays, iteration is disjoint and cursor-free, exact pair
  counts balance skew independently of encoded-ID spacing, and the established fallback and rollout controls
  preserve correctness when no compatible entry exists.
  Date/Author: 2026-07-20 / Codex.
- Decision: Represent enum-materialization refusals as bounded 64-bit key fingerprints in a primitive
  set plus a 4,096-slot clock, and treat a rare fingerprint collision as a performance-only fallback.
  Rationale: fallback runs the exact ordinary suffix, so collisions cannot alter results; fixed capacity
  prevents skewed inputs from turning refusal history into an unbounded query allocation.
  Date/Author: 2026-07-20 / Codex.
- Decision: Gate Compare-to-key-range lowering with the dynamic, default-on
  `rdf4j.lmdb.native.rangePushdown` property.
  Rationale: disabling the behavior must return to the existing residual-filter path without changing
  query results; the switch is a rollout control, not evidence of measured performance.
  Date/Author: 2026-07-20 / Codex.
- Decision: Specialize slot comparisons only after both runtime IDs prove ordered-integer encoding, and
  otherwise invoke the precompiled generic predicate; compile `IF` only when its condition cannot error.
  Rationale: raw value-field comparison is exact across ordered integer subtypes, while every mixed,
  unbound, resource, floating, and error-producing case must retain RDF4J value and three-valued semantics.
  Date/Author: 2026-07-20 / Codex.
- Decision: Measure both parallelism targets over identical one-to-one C∩D data and query text, changing
  only query concurrency and the explicitly pinned batch/parallel dispatch controls.
  Rationale: this makes the sequential/concurrent and serial-batch/parallel-overlap pairs comparable,
  while setup-time digests and telemetry reject semantic or execution-path drift without embedding a
  performance assertion in the harness.
  Date/Author: 2026-07-20 / Codex.
- Decision: Keep batched hash scratch on the owning cursor, keyed-match store, or DISTINCT tracker and
  validate preloaded tuple heads with a table-mutation version before traversing a collision chain.
  Rationale: owner confinement removes per-batch allocation without introducing shared mutable state;
  version validation preserves correctness when earlier rows insert, rehash, or clear the same table.
  Date/Author: 2026-07-20 / Codex.
- Decision: Measure all five ordering selectors over one fixed-width, reverse-insertion fixture and stream
  an order-sensitive digest while recording selector-specific optimized/runtime evidence during trial setup.
  Rationale: identical data isolates the ordering shape, fixed-width lexical values make expected order
  deterministic, and current full-sort fallbacks can coexist with future radix and order-elimination paths
  without converting an unmeasured strategy into an acceptance claim.
  Date/Author: 2026-07-20 / Codex.
- Decision: Own one primitive path-result memo per correlated `JoinCursor`, identify it by the exact
  `PathPlan` object, and key entries by resolved source ID plus forward/backward direction; retain only fully
  exhausted BFS encounter-order arrays after both entry and value reservations succeed.
  Rationale: cursor ownership prevents snapshots or concurrent evaluations from sharing mutable results,
  completed-only insertion preserves early-close semantics, and immediate refusal leaves the ordinary BFS
  as an exact bag/order fallback.
  Date/Author: 2026-07-20 / Codex.
- Decision: Reserve only admitted parallel workers, with a mode-specific minimum of one for independent
  range partitions and two for morsel consumers; run the single morsel producer cooperatively on the query
  thread and expose the granted worker count in both parallel strategy labels.
  Rationale: largest-fit worker groups keep simultaneous large queries parallel without oversubscribing the
  fixed pool, while excluding the caller-owned producer from the budget makes reservation accounting match
  actual pool occupancy and keeps the stable `parallelPipelines`/`parallelAggregation` path vocabulary.
  Date/Author: 2026-07-20 / Codex.
- Decision: Represent the row C∩D overlap as single-use deferred proposals; the parallel proposal holds
  only its actual elastic task reservation while costing, and releases it if batch wins.
  Rationale: batch filter ownership and parallel worker/source ownership must transfer only to the selected
  cursor; this preserves merge-before-hash dominance, permits exact granted-worker cost, and makes a losing
  proposal resource-neutral.
  Date/Author: 2026-07-20 / Codex.
- Decision: Port the same proposal contract to aggregate dispatch, creating the factorized proposal first
  so it owns the probed tail, then admitting parallel work only far enough to obtain its actual worker grant;
  ties remain factorized and a selected parallel proposal that cannot open falls back to the retained tail.
  Rationale: neither losing strategy may open readers, worker plans, tasks, or tail cursors, while deterministic
  tie-breaking and dynamic fallback preserve the old exact aggregate ladder semantics.
  Date/Author: 2026-07-20 / Codex.
- Decision: Store every two-or-more-slot native group key in `PrimitiveTupleTable`, retaining unrolled
  comparisons through width four and looping above it; remove `NativeGroupTable`'s boxed generic mode.
  Rationale: native group keys are exclusively long ID slots, so no genuinely non-ID key requires object
  hashing, while one flat key array plus stored hashes/fingerprints preserves the existing dense group index
  and parallel merge contract at arbitrary width.
  Date/Author: 2026-07-20 / Codex.
- Decision: Gate parallel row admission on `MultiJoinPlan.estimate` against the 250,000-work startup
  intercept, retaining `minRootEstimate` as a total-work compatibility alias and keeping 8,192 rows solely
  as range partition granularity; admit immutable bag-safe VALUES and worker-forked multi-value children,
  while continuing to require a statement-pattern exchange root.
  Rationale: plan work captures suffix fan-out and dead tails that root cardinality cannot, while the root
  and worker-ownership constraints preserve raw-quad exchange semantics and mutable-state confinement.
  Date/Author: 2026-07-20 / Codex.
- Decision: Let a factorization candidate move with filters that read only its own fresh slots, but veto
  every candidate touched by a filter spanning multiple prospective branches; keep unknown masks and the
  filtered fresh count-only group-tail case conservative.
  Rationale: a branch-local filter remains semantically adjacent after sinking, whereas cross-branch reads
  require a shared prefix and the narrow group-tail layout still cannot place the filter safely.
  Date/Author: 2026-07-20 / Codex.
- Decision: Construct an adaptive placement lease/session at each worker attempt and reuse one non-owning
  session across a factorized cursor's ordinary suffix openings; preserve the optimizer-selected initial
  depth and let the decorator alone move within the precomputed legal envelope.
  Rationale: worker filter forks make the unsynchronized lease state thread-confined, while non-owning
  suffix decoration avoids double-close across retained branch/prefix owners and keeps rollback/telemetry
  publication at one cursor lifecycle boundary.
  Date/Author: 2026-07-20 / Codex.
- Decision: Represent path breadth levels and discovered state as unsigned-sorted primitive runs, use
  revision-valid immutable CSR slices when present, and otherwise drive one forward-seek sweep per level;
  parallel levels own one same-snapshot source/probe per worker and merge only after the level barrier.
  Rationale: sparse value IDs rule out dense bitmaps, sorted runs preserve locality without boxed queues,
  worker confinement satisfies LMDB cursor ownership, and barrier-time sorted merging performs one exact
  SET-discovery arbitration before the next breadth level begins.
  Date/Author: 2026-07-20 / Codex.
- Decision: Lower a compatible constant-predicate alternation tree to one small immutable `PathStep` array
  and merge all alternative edges at the existing per-level unsigned sort/dedup barrier; refuse sequences,
  negated sets, variable predicates/contexts, and incompatible endpoint or context terms.
  Rationale: one shared frontier keeps exact SPARQL SET reachability and cycle handling without materializing
  RDF values, while the narrow shape gate preserves generic evaluation for algebra outside this contract.
  Date/Author: 2026-07-20 / Codex.
- Decision: Estimate bounded path closure from direction-aware mean fan-out with a four-level cap and the
  static reachable upper bound, then prefer the exact stored size of a completed evaluation-local path memo.
  Rationale: the planner remains I/O-free and reacts monotonically to store statistics, while a lazy linked
  reference to the already-budgeted memo avoids mutable plan-global state and duplicate result storage.
  Date/Author: 2026-07-20 / Codex.
- Decision: Make `rdf4j.lmdb.chunkPipeline.externalRoot.experimental` default on after per-worker adaptive
  ownership, while retaining its existing name and treating literal `false` as the documented rollout kill
  switch; keep merge walks, SIP masks, and hash builds disabled for independently scheduled worker roots.
  Rationale: one shared gate already covers the row and aggregate handoff without widening APIs, but no paired
  benchmark or worker CPU profile exists yet to justify deleting the compatibility switch or the worker
  restrictions that require one globally ordered root stream or would duplicate complete build structures.
  Date/Author: 2026-07-20 / Codex.
- Decision: Centralize query-side byte claims in one lazily initialized process authority, with
  `rdf4j.lmdb.queryMemory.maxBytes` defaulting to `max(1 GiB, maxHeap/2)` and
  `rdf4j.lmdb.queryMemory.perQueryMaxBytes` defaulting to one quarter of that global limit.
  Rationale: exact reservations make cross-operator/query pressure observable and bounded; synchronous
  LRU spill-then-shed callbacks reuse existing graceful fallbacks without waiting for another cursor, and
  idempotent cursor-close cleanup provides the common leak barrier required before operator wiring.
  Date/Author: 2026-07-20 / Codex.
- Decision: Refuse shared ID-to-value cache entries whose lexical form exceeds one Mi-character without
  changing value resolution or the store's durable representation.
  Rationale: a 100 MiB literal previously occupied one nominal cache slot and created unbounded retained
  heap; a cheap lexical-length guard removes that exposure without adding synchronization or affecting
  ordinary values, and the remaining plan 12 §2 work can size aggregate cache budgets independently.
  Date/Author: 2026-07-20 / Codex.
- Decision: Build one immutable ORDER BY live-row map at plan construction, put every unique order slot in
  its contiguous prefix, append projected and factorized-enum slots once, and store spill-run keys ahead of
  a separately streamed payload region.
  Rationale: all comparison offsets then stay inside `keyWidth`; projection and DISTINCT use one inverse
  remap; and merge arbitration can read keys without decoding payload rows that are not yet emitted.
  Date/Author: 2026-07-20 / Codex.
- Decision: Reuse each producer's existing rollout switch below ORDER BY rather than add a sort-specific
  dispatch switch; leave dense key/payload rows unguarded as a semantics-preserving representation.
  Rationale: the only semantic latitude is parallel tie order, already controlled by the shared parallel
  switch, while a second gate would duplicate producer policy and make composed dispatch harder to reason
  about.
  Date/Author: 2026-07-20 / Codex.

## Outcomes & Retrospective

The program is in progress. The repository starts from a clean successful quick build, and the living
tracking artifacts now exist. Plan 07 §1 now consumes its CSR metadata with guarded asymptotic fast paths,
and plan 08 §1 has a tested rollback switch; their correctness selections are green, but neither CSR nor
range throughput acceptance has been measured. Plan 02 §3 now routes proven ordered comparisons without
decoding while preserving generic fallbacks and expression error semantics; its correctness selections are
green, but allocation-profiled acceptance has not been measured. Outcomes will be updated at every
completed phase and at program exit. Plan 13 now also has a targetable parallelism axis with deterministic
metadata, result, execution-path, and configured-worker checks; its two numeric speedup gates remain open
until paired JMH measurements are recorded.
Plan 03 §2 now uses flat primitive storage and three owner-confined probe passes across hash joins and
tuple-backed consumers. Collision, growth, bag, DISTINCT-order, filter, grouping, and counter tests are
green; throughput, allocation, and JIT acceptance remain unmeasured because no paired JMH/JFR run was made.
Plan 07 §2 now serves only ordered CSR scans whose requested varying-field sequence exactly matches the
recorded build emission order, exposes the iterator's true index through the read-lock wrapper, and lets
the standalone merge path consume a warmed cached side. The 82-test CSR, wrapper, merge, ordered-DISTINCT,
and cost-neighbor selection is green; the stated cursor-walk throughput improvement remains unmeasured
because no paired JMH run was made.
Plan 07 §3 now reuses a warmed CSR entry's `keysByDense` vector as one per-stage root SIP mask without
copying or sorting primitive key storage or reserving query memo capacity, and it retains the existing
refusal of a redundant stage hash build. The focused counter shape proves root rows are masked with no hash
build and exact LMDB/generic parity; the 68-test CSR/cache/chunk ownership selection is green. The speedup
remains unmeasured because no paired JMH run was made.
Plan 07 §4 now scales the default entry cap with the global budget, replaces map-wide LRU searches with a
per-store second-chance clock, and returns exact owned bytes on eviction, invalidation, commit, and close.
Successful commit drops the whole cache before lookups are re-enabled because no predicate write set is
available; rollback retains revision-valid entries. The cache, real-store lifecycle, concurrent snapshot,
and admission/refusal metrics coverage is green. Large-heap hit-byte effectiveness remains open because no
paired benchmark-corpus run was made.
Plan 07 §5 now plans warmed full-predicate root work in O(P log K) over immutable CSR prefix sums and opens
each disjoint slice directly over the shared primitive arrays, with no partition array copies or LMDB range
cursor. The skew regression records four requested partitions with row counts `[16, 16, 16, 16]`, 64 unique
rows, and four CSR root-scan dispatches; 27 CSR policy and raw/composite range neighbors are green. No paired
JMH, allocation, JFR, or JIT run was made, so throughput and allocation improvements remain unmeasured.
Plan 13 now also has five directly selectable ordering workloads and annotation-generated metadata checks.
Their deterministic sequence and current-path smoke is green; no JMH timing, allocation profile, radix
speedup, or order-elimination acceptance claim has been made.
Plan 11 §1 now collapses N correlated path traversals to D traversals for D distinct resolved starts, with
expected O(1) primitive lookups, and uses the existing join replay path when the path is uncorrelated.
Completed BFS arrays retain exact encounter order, budget refusal re-runs the path, and cursor close releases
all reserved entries and values. The 59-test correctness/lifecycle selection is green; the 100k/1k workload,
allocation profile, and JIT behavior remain unmeasured.
Plan 11 §3 now processes one unsigned-sorted primitive frontier at a time, checks prior discoveries through
log-structured sorted runs, slices immutable CSR adjacency directly when available, and otherwise reuses one
ordered seek cursor per level. Wide levels divide contiguous frontier morsels among long-lived same-snapshot
workers and merge their private sorted deltas only after the barrier, preserving deterministic SET discovery
and exact cycle/diamond/self-loop semantics. The 41 path and 59 neighboring tests are green; neither the
one-million-node deep/hub ≥3× target nor wide-frontier near-linear scaling has been measured.
Plan 11 §§4–5 now apply every compatible constant-predicate alternative at each breadth level across direct
CSR, ordered-seek, scalar-probe, lazy all-pairs, and worker-confined parallel paths. Direct same-variable
algebra binds both endpoints through the ordinary row contract, so only cycles survive. Direction-aware
fan-out estimates cross the tested sibling-cost boundary, remain capped by the static reachable population,
and become exact after a completed memo is observed in the same evaluation. The final 46 path/planner and 35
row/join/fan-out tests are green; deep/hub timing, scaling, allocation, JFR, and JIT behavior are unmeasured.
Plan 04 §1 now admits the largest useful worker group that fits the shared fixed-pool budget and keeps the
only morsel producer on the caller. Concurrent range queries degrade from four to one worker instead of
falling fully sequential, morsel mode never dispatches a lone consumer, and strategy telemetry reports the
actual grant. Focused admission plus row/aggregation lifecycle and partition neighbors are green; the stated
8-core p95/throughput thresholds remain unmeasured because no paired parallelism JMH run was made.
Plan 01 §5 now arbitrates factorized and parallel aggregate execution from deferred, single-use proposals.
Small overlap chooses serial factorization, records both estimated costs and the parallel higher-cost decline,
and returns the complete elastic reservation; non-overlap fixtures retain parallel behavior, and setup failure
still falls back without publishing speculative counters. The focused red and 75-test aggregate lifecycle
selection are green; no aggregate timing or allocation claim was measured.
Plan 04 §2 now admits an 8,000-row root with measured 100x suffix fan-out and refuses a 60,000-row root
whose suffix has no work. Immutable VALUES children can participate, while multi-value fallback filters
clone worker-local memo/value caches and resolve against the worker snapshot. The 121-test admission,
priority, flag, partition, aggregation, and lifecycle selection is green; no paired JMH/JFR run was made.
Plan 04 §3 now sends ordinary row and aggregate worker prefixes through the existing external-root batch
chain by default after each worker has its own adaptive lease/session. Refill correctness, exact native and
generic parity, explicit scalar fallback, and the continued absence of worker hash builds, merge walks, and
SIP masks are covered by the 124-test owner/lifecycle selection. The stated 1.3x speedup and worker CPU
profile acceptance remain open because no benchmark or profile was run.
Plan 02 §4 now decorates sequential, morsel-fed worker, and factorized ordinary-suffix chains without
sharing mutable lease state or filter-close ownership. The worker movement decision matches the sequential
session on identical data; a deterministic four-thread overlap test proves independent worker sessions and
exactly-once filter close, while factorized placement starts at the optimizer boundary and preserves rows.
The 29-test adaptive owner class is green; the stated expensive-filter timing improvement remains open
because no paired benchmark was run.
Plan 01 §3 now compares the truly overlapping row batch and parallel strategies by work cost while
retaining merge-before-hash inside the batch proposal. Only the selected cursor acquires filter/source/task
ownership, and an unselected parallel proposal returns its elastic reservation. The 52-test cost, dispatch,
join, snapshot, partition, and lifecycle selection is green; the stated 8-core two-times speedup remains
unmeasured pending the paired benchmark.
Plan 10 §2 now stores all multi-slot native group keys in one flat primitive table, using stored full hashes
and fingerprints with unrolled narrow comparisons and a wide loop thereafter. An eight-key UNION aggregate
matches the generic evaluator and reports `primitiveTupleGroups`; the 10-test primitive grouping class is
green. The width-eight allocation profile and within-1.5-times probe-throughput target remain open because
no paired benchmark/profile was run.
Plan 02 §5 now sinks a filtered leg when the filter is branch-local, including unknown static estimates,
without moving filters that couple branches or entering the unsupported filtered fresh count-only group-tail
shape. The 106-test factorization and differential-fuzz selection is green; the filtered theme-query speed
target remains unmeasured.
Plan 05 §§1–3 now let blocking ORDER BY consumers select the same C/D/E producer as ordinary rows without
entering prefix-run or ordered-DISTINCT strategies, while preserving ordered-factorized priority. A plan-time
map packs only `source ∪ order ∪ enum` lanes with unique keys first; classic top-K/full sort, factorized
expansion, projection, and DISTINCT all use the remap. Spill runs hold a key region and separately streamed
payload region, so merge comparison never deserializes payload before a run wins. The 12-slot acceptance
shape records width 12→4 and 256 copied values for 64 rows, and forced spill reconstructs exact payloads.
The forked 50,000-row full-sort smoke measured 405.866 ms/op; its one-column CPU profile is non-qualifying
for wide-row locality, so core scaling, paired byte-throughput, and load-profile acceptance remain open.

## Context and Orientation

The implementation lives primarily in `core/sail/lmdb`. Native query planning and execution are under
`src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`; LMDB storage, CSR adjacency, value encoding, and
dictionary access are under `src/main/java/org/eclipse/rdf4j/sail/lmdb`. Tests and JMH benchmarks are
under the corresponding `src/test/java` tree. `00-overview.md` defines the immutable scope and dependency
graph. Each numbered workstream defines exact class and method anchors, soundness constraints, focused
tests, and measurable acceptance criteria. `13-verification.md` defines the cross-cutting gates.

An execution path is the physical strategy that produces a query fragment, such as nested-loop, batch
hash join, parallel pipeline, or factorized rows. A proposal is an admissible strategy paired with an
estimated cost and a deferred cursor opener. A CSR adjacency entry stores predicate-specific keys,
neighbors, and prefix offsets in flat arrays. A query memory reservation is a byte-accounted claim that
must spill, shed, or fall back immediately when refused.

## Plan of Work

First create the equivalence, dispatch, snapshot, lifecycle, and benchmark harnesses and record the
pre-existing compliance baseline. Then complete Phase I in work-item order: execution telemetry and
fan-out statistics; filter unification, batch-join filters, and decode-free comparisons; tuple-table
counter cleanup and batched probes; elastic task admission; CSR zone-map, degree, seek, ordered access,
and SIP sourcing. Each behavior-changing item begins with its smallest focused repository test and is
validated before the next dependent item.

Phase II introduces proposal-based cost arbitration, slice and top-K changes, adaptive filter
decoration, worker batching, ordered producer reuse, live sort layouts, base-cursor multiplicity, cache
policy, and budgeted primitive group tables. Phase III adds parallel sort/merge/build, radix and
order-aware execution, deeper factorization, range and encoding work, joins, aggregation coverage,
paths, and memory/dictionary batching. Phase IV adds leapfrog intersection, outer accumulation, and
completes global budget wiring. The final pass reruns every gate and audits the interaction matrix.

## Concrete Steps

Work from the repository root. Before test work, refresh the workspace-local Maven repository with the
mandatory root quick install already recorded above. Run focused and module tests through:

    python3 .codex/skills/mvnf/scripts/mvnf.py ClassName#method --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run a single benchmark through:

    scripts/run-single-benchmark.sh --module core/sail/lmdb --class <fqcn> --method <method>

Run plan snapshots with the repository's query-plan snapshot CLI workflow and compare JMH results with
the checked-in JMH comparison tool. Never use Maven `-am` or `-q` for tests. All Maven invocations use
`-Dmaven.repo.local=.m2_repo`, and offline mode is preferred.

## Validation and Acceptance

Every focused correctness test and the full `core/sail/lmdb` module suite must pass, with the frozen
LMDB-only compliance baseline not growing. Equivalence corpora compare bags, and ORDER BY corpora compare
sequences modulo equal-key ties. Dispatch tests assert winning strategy and decline reasons. Snapshot
diffs are restricted to declared target shapes. Lifecycle tests prove cancellation, snapshot isolation,
and zero memory-ledger leakage. Each performance claim requires paired JMH runs with a three-percent
noise floor; profile claims require JFR or equivalent evidence on the active JDK. Program completion
requires every row in `ACCEPTANCE.md` to be closed and every switch in `ROLLOUT.md` to have a measured
decision.

## Idempotence and Recovery

All builds, tests, snapshots, and benchmarks are repeatable. Test artifacts and logs are retained; no
untracked artifact is deleted. Each feature has a correct fallback on budget refusal, unsupported shape,
or disabled rollout switch. If a focused test exposes a regression, keep the failing test, revert only
the current surgical production edit, update this plan's discovery and decision logs, and resume from
the root cause. Persist benchmark inputs and compare runs rather than replacing them.

## Artifacts and Notes

The initial build transcript is `maven-build.log`. Compact verification evidence is persisted in
`initial-evidence.txt` when the first verify selection runs. The admission regression's initial evidence
is `initial-evidence.root-prereq.txt`; the workspace profile's structural red is
`initial-evidence.workspace-build.txt`. The DISTINCT-extrema Routine A evidence is
`initial-evidence.agent-distinct-extrema.txt`, with focused and class-level green summaries in the
matching `post-evidence.agent-distinct-extrema*.txt` files. The deferred DISTINCT SUM/AVG Routine A
evidence is `initial-evidence.agent-distinct-sum.txt`, with focused, floating-fallback, and class-level
green summaries in `post-evidence.agent-distinct-sum*.txt`. Isolated Maven logs and reports live under
`.mvnf/workspaces`. The compliance baseline is documented in `COMPLIANCE-BASELINE.md`, with its
machine-readable 24 identities in `compliance/sparql/lmdb-compliance-baseline.json`. Its current-suite,
checker-red, checker-green, and actual-gate evidence is preserved in
`initial-evidence.agent-compliance-baseline.txt`, `initial-evidence.agent-compliance-checker.txt`, and
the matching `post-evidence.agent-compliance-*.txt` files.
The scan benchmark's native-address fixture red/green is preserved in
`initial-evidence.root-scan-benchmark.txt` and `post-evidence.root-scan-benchmark.txt`. Its paired raw
JMH logs and generated comparison are under `profiles/lmdb-native-engine/scan/`; the checked comparison
reports a 65.441% lower average decode time for exact-width loads.
Plan 07 §1's CSR seek red and plan 08 §1's missing rollout-gate red are preserved in
`initial-evidence.agent-csr-zone-map.txt`; their matching focused and 35-test green summaries are in
`post-evidence.agent-csr-zone-map.txt`, with retained Maven logs under workspace `agent-csr-zone-map`.
Plan 07 §2's missing ordered-consult red and its mixed-domain ordered-promotion neighbor red are preserved
in `initial-evidence.agent-csr-ordered.txt` and `initial-evidence.agent-csr-ordered-neighbor.txt`; the
matching 82-test green summary is `post-evidence.agent-csr-ordered.txt`, with retained Maven logs under
workspace `agent-csr-ordered`.
Plan 07 §3's missing CSR-sourced root-mask red is preserved in `initial-evidence.agent-csr-sip.txt`; its
focused two-test green and 68-test CSR/cache/chunk ownership summary are in
`post-evidence.agent-csr-sip.txt` and `post-evidence.agent-csr-sip-owned.txt`, with retained Maven logs
under workspace `agent-csr-sip`.
Plan 07 §4's proportional-cap, eager-commit-credit, close-leak, and missing-metrics reds are preserved in
`initial-evidence.agent-csr-cache-policy-{cap,commit,close,metrics}.txt`. The matching 21-test cache,
six-test lifecycle, and focused metrics greens are in `post-evidence.agent-csr-cache-policy-{cache,lifecycle,metrics}.txt`,
with retained Maven logs under workspace `agent-csr-cache-policy`.
Plan 07 §5's raw-byte partition-collapse red is preserved in
`initial-evidence.agent-csr-partitioned.txt`; its focused green and combined 27-test CSR/cache/range green
are in `post-evidence.agent-csr-partitioned-focused.txt` and
`post-evidence.agent-csr-partitioned-neighbors.txt`, with retained Maven logs under workspace
`agent-csr-partitioned`.
Plan 02 §3's compiler-class red is preserved in `initial-evidence.agent-filter-decode.txt`; its compiler,
expression-equivalence, and neighboring green summaries are in `post-evidence.agent-filter-decode-*.txt`,
with full logs and reports under workspace `agent-filter-decode`.
Plan 13 §2's parallelism metadata pre-green is preserved in
`initial-evidence.agent-parallelism-benchmark.txt`; the matching metadata post-green, five-selector
digest/engagement smoke, and generated-method hit proof are in
`post-evidence.agent-parallelism-benchmark.txt`, with full logs under workspace
`agent-parallelism-benchmark`.
Plan 13 §2's ordering metadata pre-green and nullable-metric smoke failure are preserved in
`initial-evidence.agent-orderby-benchmark.txt`; the matching five-selector green, shared metadata green,
and generated-method hit proof are in `post-evidence.agent-orderby-benchmark.txt`, with full logs under
workspace `agent-orderby-benchmark`.
Plan 13 §2's hash-join, materialization, and correlated benchmark pre-greens are preserved in
`initial-evidence.root-{hash-join,materialization,correlated}-benchmark.txt`; the matching deterministic
selector, query-path hit-proof, and shared metadata greens are in the corresponding
`post-evidence.root-*-benchmark.txt` files, with retained reports under their named mvnf workspaces.
Plan 03 §2's two focused Routine A failures are preserved in
`initial-evidence.agent-batched-probe.txt` and `initial-evidence.agent-batched-probe-tuples.txt`; the
matching 46-test post-change summary is `post-evidence.agent-batched-probe.txt`, with retained logs and
reports under workspace `agent-batched-probe`.
Plan 10 §2's four-key constructor-cliff failure is preserved in
`initial-evidence.root-primitive-group-wide.txt`; its eight-key direct/query greens and 10-test owner-class
summary are in `post-evidence.root-primitive-group-wide.txt`, with retained logs and reports under workspace
`root-primitive-group-wide`.
Plan 06 §4's cursor-wide enum-latch failure is preserved in `initial-evidence.root-enum-latch.txt`; its
focused healing, 4,096-entry clock-eviction, 43-test owner, and 56-test neighbor greens are in
`post-evidence.root-enum-latch.txt`, with retained logs under workspace `root-enum-latch`.
Plan 11 §1's repeated-start traversal failure is preserved in
`initial-evidence.agent-path-sharing.txt`; the 28-test path class and 31 join/row-lifecycle neighbor greens
are in `post-evidence.agent-path-sharing.txt`, with retained logs under workspace `agent-path-sharing`.
Plan 11 §3's sorted-level, direct-CSR, seek-sweep, unsigned-order, and parallel-worker failures are preserved
in `initial-evidence.agent-path-frontier.txt`; the focused greens, 41-test path class, and 59 CSR/row/join
neighbors are in `post-evidence.agent-path-frontier.txt`, with retained logs under workspace
`agent-path-frontier`.
Plan 11 §§4–5's alternation-dispatch, direct same-variable algebra, fan-out closure, and memo-observed exact
estimate failures are preserved in `initial-evidence.agent-path-late.txt`; the final 46 path/planner tests
and 35 row/join/fan-out neighbors are in `post-evidence.agent-path-late.txt`, with retained logs under
workspace `agent-path-late`. No performance or profiling artifact was produced for these sections.
Plan 04 §1's reservation-surface, concurrent-degradation, caller-thread producer, and granted-worker-label
failures are preserved in `initial-evidence.agent-elastic-admission*.txt`; its final focused and neighboring
green summary is `post-evidence.agent-elastic-admission.txt`, with retained logs and reports under workspace
`agent-elastic-admission`.
Plan 04 §2's total-work, plan-child, and multi-value worker-fork failures are preserved in
`initial-evidence.agent-parallel-plan-admission.txt`, `initial-evidence.agent-parallel-plan-children.txt`,
and `initial-evidence.agent-parallel-plan-fork.txt`; its final 121-test green summary is
`post-evidence.agent-parallel-plan-admission.txt`, with retained logs under workspace
`agent-elastic-admission`.
Plan 04 §3's default-on external-root row-worker failure and prerequisite-integrated reconfirmation are
preserved in `initial-evidence.agent-parallel-root-handoff.txt`; its final 124-test row, aggregate, adaptive,
flag, startup-failure, and cleanup summary is `post-evidence.agent-parallel-root-handoff.txt`, with retained
logs under workspace `agent-parallel-root-handoff`.
Plan 02 §4's missing external-root entry, worker decoration, factorized initial-depth, and factorized-suffix
session failures are preserved in `initial-evidence.root-adaptive-decorator.txt`,
`initial-evidence.root-adaptive-worker.txt`, `initial-evidence.root-adaptive-factorized-depth.txt`, and
`initial-evidence.root-adaptive-factorized-suffix.txt`. Their focused greens, the four-worker overlap
stress, and the 29-test owner-class summary are consolidated in
`post-evidence.root-adaptive-ownership.txt`, with retained logs under workspace `root-adaptive-decorator`.
Plan 01 §3's missing proposal and missing both-cost telemetry reds are preserved in
`initial-evidence.root-strategy-arbitration.txt`; its cost, single-use ownership, focused dispatch, and
52-test join/parallel neighbor greens are in `post-evidence.root-strategy-arbitration.txt`, with retained
logs under workspace `root-strategy-arbitration`.
Plan 01 §5's eager-parallel aggregate dispatch failure is preserved in
`initial-evidence.root-aggregate-arbitration.txt`; focused cost/decline/release evidence and the final
75-test aggregate lifecycle summary are in `post-evidence.root-aggregate-arbitration.txt`, with retained
logs under workspace `root-aggregate-arbitration`.
Plan 02 §5's branch-local-filter sinking failure is preserved in
`initial-evidence.root-filter-sink.txt`; its owner-class and 106-test factorization/fuzz greens are in
`post-evidence.root-filter-sink.txt`, with retained logs under workspace `root-filter-sink`.
Plan 05 §§1–3's missing ordered parallel producer and missing 12→4 live layout reds are preserved in
`initial-evidence.agent-ordering-core.txt`. Focused greens are in
`post-evidence.agent-ordering-{dispatch,layout}.txt`; integrated producer, Explain, row-stream,
ordered-factorized, packed-sort, and benchmark-smoke summaries are consolidated in
`post-evidence.agent-ordering-final.txt`, with retained Maven logs under workspace `agent-ordering-core`.
The isolated 50,000-row JMH result is recorded in that final evidence file, and the diagnostic CPU profile
is `/tmp/rdf4j-ordering-profile/java-command-cpu-20032.txt`; neither is a paired acceptance measurement.
Performance results, JFR recordings, plan snapshots, and the closing interaction-matrix review remain
checked-in or linked from `ACCEPTANCE.md`.

## Interfaces and Dependencies

The required end-state interfaces and types are specified in the numbered workstreams. Central additions
include `LmdbFanOutStats`, proposal-based dispatch carrying opener/cost/tag, vectorized
`NativeBooleanFilter.selectBatch`, elastic task reservations that report granted workers, base
`RowCursor.multiplicity`, range-aware pattern/source APIs, and `LmdbQueryMemoryManager` reservations with
reclaimable spill or shed behavior. No sketch-estimator dependency is introduced. New third-party
dependencies are not expected; existing primitive arrays, LMDB APIs, and repository utilities are used.

Revision note (2026-07-19, Codex): Created the program-level living ExecPlan because the numbered
workstreams are immutable implementation specifications and lacked the mandatory progress, discovery,
decision, and retrospective sections needed for restartable execution.

Revision note (2026-07-19, Codex): Recorded the prerequisite admission repair, restored and proved
workspace-isolated Maven outputs from the attached known-good POM, and corrected Phase IV to include the
previously omitted plan 09§6 bushy-build work item.

Revision note (2026-07-19, Codex): Completed plan 10 §4.1 by admitting duplicate-insensitive DISTINCT
extrema to parallel aggregation and recording focused red/green plus representative-fallback evidence.

Revision note (2026-07-19, Codex): Completed plan 10 §4.2 with merged-set deferred recomputation for
parallel DISTINCT SUM/AVG and recorded cross-worker-duplicate and floating-fallback evidence.

Revision note (2026-07-20, Codex): Completed plan 13 §1.5 by freezing the exact 24-failure LMDB SPARQL
compliance baseline and adding a tested, fail-closed Failsafe report-comparison gate.

Revision note (2026-07-20, Codex): Completed plan 02 §1's shared vectorized filter kernel and closed
plan 08 §3's scan-decode acceptance row with focused parity plus paired JMH evidence.

Revision note (2026-07-20, Codex): Implemented plan 02 §2 by decorating admitted merge/hash batch
cursors with the shared filter path and opening hash build-overflow fallback through a filter-free plan;
focused dispatch, equivalence, exactly-once ownership, neighboring join, and decline-trace tests pass.

Revision note (2026-07-20, Codex): Completed plan 03 §1 with thread-confined tuple and DISTINCT metrics,
including worker-table metric merge and exactly-once publication from every owned ordered-row lifecycle.
Focused red/green evidence is preserved in `initial-evidence.root-hot-loop-counters.txt` and
`post-evidence.root-hot-loop-counters.txt`.

Revision note (2026-07-20, Codex): Completed plan 07 §1 with exact-ID zone rejection, prefix-sum degree,
actual-index-order-guarded binary run operations, and seek fallback parity; recorded plan 08 §1's
default-on range-pushdown rollout switch and explicit unmeasured performance status.

Revision note (2026-07-20, Codex): Completed plan 02 §3 with compile-time constant decoding, guarded
ordered-ID slot comparison and generic fallback, assured-bound predicate specialization, safe IF/COALESCE
and builtin coverage, plus generic-equivalence coverage for NaN, signed zero, mixed numerics, unbound
operands, and error-producing expressions; allocation-profiled acceptance remains unmeasured.

Revision note (2026-07-20, Codex): Completed plan 01 §2 with bounded per-predicate HyperLogLog samples,
CSR exact/mean fan-out lookup, promotion-aware explicit/inferred write accounting, and chained plan
estimates. Focused red/green evidence is preserved in `initial-evidence.fanout.txt` and
`post-evidence.fanout.txt`.

Revision note (2026-07-20, Codex): Completed plan 01 §1 by freezing path literals in
`LmdbNativeAttemptMetrics`, publishing deterministic query-local winning and decline traces through
Explain, and pinning the dispatch ladder. The final 40-test neighboring selection is preserved in
`post-evidence.root-dispatch-final.txt`.

Revision note (2026-07-20, Codex): Completed plan 01 §4 by sharing the short-finite-slice predicate with
parallel admission and replacing the fixed 100,000-row top-K cliff with width-aware lazy admission.
Large safe K values reduce bounded sorted runs and pre-filter against a proven boundary; unsafe array
capacities retain full external sort. Red/green evidence is preserved in
`initial-evidence.root-slice-topk.txt` and `post-evidence.root-slice-topk.txt`.

Revision note (2026-07-20, Codex): Added plan 13 §2's `ParallelismBenchmark` with targetable sequential,
two/four-query, serial hash-batch, and parallel-overlap methods over the same million-row-per-predicate
fixture. The harness exposes worker counts and execution paths, but makes no unmeasured timing claim.

Revision note (2026-07-20, Codex): Completed plan 03 §2 with flat primitive hash-join storage, cached
fingerprints/full hashes, reusable three-pass probe scratch, and mutation-safe tuple probes wired into
DISTINCT and EXISTS consumers. Correctness is green; benchmark and profile acceptance remain unmeasured.

Revision note (2026-07-20, Codex): Completed plan 07 §2 with build-recorded CSR emission order,
direction-matched ordered consults, merge-seekable index metadata, wrapper delegation, and a homogeneous
ordered-promotion cost gate. Correctness and dispatch selections are green; performance remains
unmeasured without paired JMH evidence.

Revision note (2026-07-20, Codex): Completed plan 07 §3 by publishing a concrete CSR entry's borrowed
sorted key vector once per probe stage as a root SIP mask, preserving hash-memo refusal, zero query-budget
ownership, idempotent cleanup, and exact generic parity. Counter and 68-test owner evidence is green;
performance remains unmeasured without paired JMH evidence.

Revision note (2026-07-20, Codex): Added plan 13 §2's `OrderByBenchmark` with targetable full-sort,
top-K, radix-eligible, expression-key, and order-elimination methods over one deterministic fixture.
Ordered digest and plan-engagement smoke plus shared benchmark metadata are green; performance remains
explicitly unmeasured.

Revision note (2026-07-20, Codex): Completed plan 11 §1 with uncorrelated join replay, retained probe
reuse, and an evaluation-local bounded primitive memo for correlated repeated starts. Exact bag/order,
backward, refusal, snapshot, telemetry-release, and neighboring lifecycle tests are green; benchmark,
allocation, and JIT acceptance remain unmeasured.

Revision note (2026-07-20, Codex): Completed plan 04 §1 with largest-fit, mode-aware worker reservations,
query-thread cooperative morsel production, actual-grant sizing in row and aggregate callers, and actual
worker telemetry. Concurrent degradation and lifecycle correctness are green; performance remains
explicitly unmeasured.

Revision note (2026-07-20, Codex): Completed plan 11 §2 with bounded later-endpoint target sets,
per-source target cutoff, target-specific memo isolation, and lazy all-pairs identity/start enumeration.
Focused red/green and 68 path/join/lifecycle tests are preserved in
`initial-evidence.agent-path-targets.txt`, `initial-evidence.agent-path-lazy-starts.txt`, and
`post-evidence.agent-path-targets.txt`; large-store timing remains explicitly unmeasured.

Revision note (2026-07-20, Codex): Completed plan 04 §2 with total-work admission, immutable VALUES
children, worker-confined multi-value fallback state, and worker-snapshot value resolution. Focused
red/green and 121 parallel/priority/lifecycle tests are preserved; performance remains unmeasured.

Revision note (2026-07-20, Codex): Completed plan 01 §5 by porting deferred cost proposals, actual-grant
parallel cost, total-work admission, decline/cost telemetry, unused-resource release, and dynamic fallback
to the aggregate factorized/parallel overlap. The focused red and 75-test aggregate lifecycle selection are
preserved; performance remains explicitly unmeasured.

Revision note (2026-07-20, Codex): Completed plan 04 §3 by making the existing external-root worker batch
handoff default on for row and aggregate execution after per-worker adaptive ownership, retaining literal
`false` as the scalar fallback and preserving the no-hash/no-merge/no-SIP worker restrictions. The frozen
red and 124-test green evidence are checked in; performance and CPU profiles remain unmeasured.

Revision note (2026-07-20, Codex): Completed plan 07 §4 with a proportional default entry cap, intrusive
per-store second-chance eviction, eager all-entry commit invalidation, exact close/drop accounting, and
native-attempt admission/refusal snapshots. Focused cache-policy and concurrent lifecycle evidence is green;
large-heap cache-effectiveness timing remains explicitly unmeasured.

Revision note (2026-07-20, Codex): Completed plan 07 §5 with zero-copy dense-key CSR root partitions,
prefix-sum pair balancing, direct in-memory slice iteration, and payload-safe same-snapshot/composite
routing. Focused skew and 27 neighboring tests are green; no throughput, allocation, or profile claim is
made without paired measurement.

Revision note (2026-07-20, Codex): Completed plan 02 §4 with externally rooted adaptive sessions,
per-worker filter/lease ownership, one factorized-suffix session, optimizer-boundary initial placement, and
a deterministic four-worker overlap/close stress. Correctness and lifecycle evidence is green; the paired
expensive-filter performance comparison remains explicitly unmeasured.

Revision note (2026-07-20, Codex): Completed plan 11 §3 with unsigned-sorted level frontiers,
log-structured discovered runs, ordered seek sweeps, direct revision-valid CSR adjacency, and barrier-merged
parallel frontier morsels. Focused red/green plus 100 path/CSR/row/join tests are preserved; both serial and
parallel performance acceptance rows remain open pending conforming measurements.

Revision note (2026-07-20, Codex): Completed plan 11 §§4–5 with compatible alternation/inverse lowering,
native direct same-variable cycles, direction-aware capped fan-out closure costs, and exact observations from
completed evaluation-local path memos. Focused red/green and final 81-test evidence is preserved; no timing,
allocation, JFR, or JIT acceptance claim is made.

Revision note (2026-07-20, Codex): Completed plan 10 §2 by generalizing primitive tuple keys beyond width
four and removing the boxed native group-key mode. Direct width-eight and full query dispatch/parity are
green; the allocation profile and width-eight/width-two throughput comparison remain explicitly unmeasured.

Revision note (2026-07-20, Codex): Completed plan 05 §§1–3 with shared unordered producer dispatch below
ordered consumers, dense key-first live rows, remapped projection/DISTINCT/factorized enum slots, and
key-only spill merge with deferred payload reads. Correctness and structural telemetry are green; the
50,000-row JMH/profile smoke is intentionally not used to close unpaired wide-row or core-scaling targets.
