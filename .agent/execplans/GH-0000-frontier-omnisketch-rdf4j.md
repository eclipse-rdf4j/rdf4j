# Implement theorem-safe Frontier OmniSketch in RDF4J

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept current as implementation proceeds. Maintain this document in accordance
with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J's LMDB query optimizer can currently retain scalar cardinalities and marginal sketches, but it cannot retain
the sampled distribution of the bindings that survive a complex partial query. That means it cannot reliably reuse
the same evidence while comparing several possible additions to a star, crossing a bridge to another star, or
repeating that process through a practical multi-star query. It is especially easy to become unsound around
`EXISTS`, `NOT EXISTS`, `MINUS`, and `OPTIONAL`, where an empty sample is not proof of absence and where bound-variable
masks change the SPARQL bag semantics.

This work adds a companion estimator named Frontier OmniSketch. A frontier is the ordered tuple of variables that a
future query fragment may still inspect. For each exact SPARQL bound-variable mask, the estimator retains an
immutable query-local random finite measure: database-exact heavy mass plus weighted residual particles. A particle
contains exact snapshot-scoped LMDB term IDs; hashes choose samples but never decide RDF equality. Every supported
linear transformation carries enough state to cost the next addition without reconstructing the prefix. Unsupported
or statistically degenerate transformations return a classified unresolved/degraded result and preserve the current
scalar estimator as the safe fallback.

The completed behavior is visible in three ways. Exact finite-bag tests demonstrate SPARQL semantics and sampling
identities. LMDB planning tests show the same canonical evidence-state ID being reused while alternative factors are
costed in different physical exploration orders. Theme benchmark coverage shows every query executing correctly and
publishing either theorem-safe Frontier evidence or a stable, explicit fallback reason. The feature is ultimately
enabled by default, but only supported evidence is authoritative; default enablement never turns an unresolved
random product, sampled absence, stale snapshot, or exhausted refinement into a trusted estimate.

## Progress

- [x] (2026-07-25 05:23Z) Close the packed-planner prerequisite: query-evaluation passes 936 tests and LMDB passes
  1,544 tests with zero failures or errors; preserve the report in `initial-evidence.packed-prereq-green.txt`.
- [x] (2026-07-25 05:30Z) Record independent implementation provenance, literature links, proof-package checksums,
  and theorem/conditional/empirical boundaries under `.agent/research/frontier-omnisketch/`.
- [x] (2026-07-25 05:34Z) Audit `BagEstimate`, packed costing, LMDB optimization-scope lifetime, cache boundaries,
  zero short-circuiting, snapshot identity, and theme-query shapes.
- [x] (2026-07-25 05:37Z) Capture the Milestone 1 red contract in
  `initial-evidence.frontier-oracle-red.txt`: `FrontierOracleContractTest#exposesExactFiniteBagAlgebra` fails 1/1
  because the exact rational and finite-bag oracle types do not yet exist.
- [x] (2026-07-25 05:51Z) Establish the exact Java finite-bag and sampling oracle. Twenty-one focused tests pass,
  including 512 single bags, 16,384 ordered pairs, 133,632 actually executed semantic assertions, and exact
  identities for 8, 64, 209, variance 36, support-fits variance zero, covariance bias, and resampling.
- [x] (2026-07-25 05:53Z) Capture the first production-state red contract in
  `initial-evidence.frontier-state-red.txt`: `BagEstimateEvidenceStateTest` fails 1/1 because the query-local
  `EvidenceStateRef` sidecar contract does not yet exist.
- [x] (2026-07-25 06:02Z) Capture the sampled-zero soundness regression in
  `initial-evidence.frontier-zero-red.txt`: a zero point estimate with a positive certified upper bound currently
  makes `EstimateContext.hasNoExecutions()` return true.
- [x] (2026-07-25 07:20Z) Add immutable evidence summaries, ordered layouts and bound-mask strata, opaque state
  references, deterministic snapshot/lane state keys, primitive exact/residual payloads, provenance, canonical
  replay, eviction, and hard query-memory accounting without changing scalar estimate identity. Preserve red/green
  evidence in `initial-evidence.frontier-state-*.txt`, `initial-evidence.frontier-payload-*.txt`,
  `initial-evidence.frontier-core-audit-red.txt`, and `initial-evidence.frontier-core-edge-{red,green}.txt`.
- [x] (2026-07-25 07:31Z) Format and verify the complete query-evaluation module after the state-core work:
  982 tests pass with zero failures, errors, or skips. Preserve the run in
  `initial-evidence.frontier-core-module-green.txt`.
- [x] (2026-07-25 07:36Z) Add and verify the durable 128-bit LMDB store identity. It survives restart and unrelated
  metadata updates, transient stores cannot claim durable identity, and malformed identity does not prevent scalar
  metadata loading. Preserve red/green evidence in `initial-evidence.frontier-store-id-*.txt`.
- [x] (2026-07-25 07:47Z) Add the pinned raw-ID LMDB snapshot source. It owns one untracked transaction, preserves
  native epoch across ordinary commits, separates explicit/inferred planes, rejects wildcard exact probes, prevents
  iterator-array escape, and invalidates instead of mixing snapshots after transaction renewal. Preserve its
  four-case red/green contract in `initial-evidence.frontier-snapshot-*.txt`.
- [x] (2026-07-25 07:58Z) Add the eight-property Frontier LMDB configuration surface and RDF vocabulary with the
  initial development default `AUTHORITATIVE`, zero persistent bytes, 64 MiB query memory, two design and two audit lanes, 4,096
  refinement units, target RSE 0.25, and defensive proposal epsilon 0.1. Preserve its red/green contract in
  `initial-evidence.frontier-config-*.txt`.
- [x] (2026-07-25 08:10Z) Make pinned scan steps and exact probes atomic with transaction renewal, move native epoch
  checks out of the per-row hot path, reject re-entrant close, and document provisional scan callbacks. Preserve the
  two new race/lifecycle regressions in `initial-evidence.frontier-snapshot-hardening-{red,green}.txt`.
- [x] (2026-07-25 08:13Z) Add the workbench repository-template fields and the persistent-service zero-budget
  boundary. Default-authoritative with zero persistent bytes performs no filesystem access or snapshot open.
  Preserve red/green evidence in `initial-evidence.frontier-lmdb-template*.txt` and
  `initial-evidence.frontier-synopsis-zero-budget*.txt`.
- [x] (2026-07-25 08:26Z) Close the remaining pinned-source resize holes: exceptional cursor cleanup is protected,
  deactivation advances the renewal generation before reactivation can fail, and public clients can classify
  invalidation and provisional callbacks. Preserve the regressions in
  `initial-evidence.frontier-snapshot-close-generation-{red,green}.txt`.
- [x] (2026-07-25 08:29Z) Establish the crash-safe manifest selector: bounded checksummed read-back, immutable
  payload descriptors with safe derived filenames, required atomic replacement and directory force, and orphan
  payload isolation. Preserve the crash-stage red/green evidence in
  `initial-evidence.frontier-manifest-orphan*.txt`.
- [x] (2026-07-25 08:35Z) Remove the nested transaction read-lock acquisition from each pinned LMDB cursor step
  while preserving the public iterator path, cursor-renewal atomicity, and compatibility fallback. Preserve the
  focused red/green contract in `initial-evidence.frontier-pinned-cursor-hotpath-*.txt`.
- [x] (2026-07-25 08:37Z) Give manifest failures stable typed classifications. Every proper byte prefix is
  `TRUNCATED`, same-length checksum failure and trailing data are `CORRUPT`, a checksum-valid unknown format is
  `VERSION_MISMATCH`, and oversized input is rejected before allocation as `BUDGET_EXCEEDED`. Preserve the six-case
  red/green contract in `initial-evidence.frontier-manifest-validation*.txt`.
- [x] (2026-07-25 08:45Z) Make normal persistent LMDB initialization create and durably retain the Frontier store
  UUID before any synopsis generation may refer to it. A real shutdown/restart retains the same identity; preserve
  red/green evidence in `initial-evidence.frontier-store-bootstrap-*.txt`.
- [x] (2026-07-25 08:50Z) Extend the versioned manifest with durable store UUID, native snapshot epoch,
  schema/hash/seed versions, design/audit lane counts, direction/role coverage, and configured build budget. Raw
  recovery and validated service loads are separate; eleven mismatch/round-trip tests plus the six validation and
  crash-selector tests are green in `initial-evidence.frontier-manifest-*.txt`.
- [x] (2026-07-25 08:53Z) Add the bounded streaming payload-block codec. It emits length-delimited primitive blocks
  with per-block CRC32C and a whole-file SHA-256 descriptor, checks total byte caps before touching input, checks
  block/record caps before body allocation, and classifies truncation, corruption, version, and budget failures.
  Preserve its five-case red/green contract in `initial-evidence.frontier-payload-block-*.txt`.
- [x] (2026-07-25 09:02Z) Make `store.properties` publication crash-safe before wiring the persistent service. A
  forced sibling temporary file is atomically replaced and the parent directory is forced before dirty state clears;
  failed replacement preserves the prior durable bytes and remains retryable. Preserve red/green and broadened
  evidence in `initial-evidence.frontier-store-properties-atomic*.txt`.
- [x] (2026-07-25 09:03Z) Add the first packed Frontier session contract without changing winner selection:
  `PackedCostModel` remains a SAM, its default scalar session separates leaf/append/refine calls, reusable estimate
  and context slots reset primitive state IDs to zero, and sparse-long replacement/growth keeps the aligned state.
  Preserve its five-test red/green evidence in `initial-evidence.frontier-packed-session-*.txt`.
- [x] (2026-07-25 09:03Z) Make positive-budget synopsis bootstrap bounded and fail-closed. It validates manifest
  identity before opening LMDB, compares one pinned epoch, then streams block CRC and full-file digest verification
  under a 64 KiB block cap. OFF, zero budget, missing, stale, and invalid evidence remain non-throwing statuses.
  Preserve its five-test red/green evidence in `initial-evidence.frontier-service-bootstrap-*.txt`.
- [x] (2026-07-25 12:21Z) Thread aligned primitive evidence-state IDs through packed dense, sparse-long, and
  multiword subset kernels, contextualization, physical metadata, and selected paths. Cold sessions close on success
  and failure; detached recipes and store-wide cache entries remain handle-free. Preserve focused evidence in
  `initial-evidence.frontier-packed-{lifecycle,kernel}*.txt`.
- [x] (2026-07-25 12:48Z) Build and publish the first bounded bidirectional base synopsis format from a pinned LMDB
  snapshot. Center-coordinated design/audit lanes retain complete selected adjacency in both directions with exact
  term IDs, named-graph and explicit/inferred flags, stored inclusion probabilities, block CRCs, a whole-file
  digest, and a publication epoch recheck. Preserve its rebuild contract in
  `initial-evidence.frontier-rebuild-{red,green}.txt`.
- [x] (2026-07-25 13:05Z) Make real LMDB insertions and deletions fail closed against stale base evidence, expose an
  explicit store rebuild API, and prove restart behavior. A clean generation becomes durably
  `DIRTY_INSERTION`/`DIRTY_DELETION` on the first actual mutation; rebuild clears both markers only after atomic
  manifest publication. Preserve evidence in
  `initial-evidence.frontier-{insert-dirty,delete-corrected,store-rebuild}*.txt`.
- [x] (2026-07-25 13:14Z) Add theorem-safe linear query-local transforms. Exact RHS multiplicity kernels cover inner,
  existence, negative existence, MINUS-compatible, and optional outer multiplicities; projection is mask-correct and
  coalesces exact bag duplicates; restriction evaluates each retained mapping once; union adds measures without a
  random-random product. Sampled zero remains estimated, exact zero remains database exact, and all scratch arrays
  are charged. Preserve evidence in `initial-evidence.frontier-{linear,relational}*.txt`.
- [x] (2026-07-25 17:16Z) Integrate the query-local Frontier session into selected packed plans. Leaves and canonical
  append/probe states now reach dense, sparse-long, and multiword search; selected-plan contextualization retains
  aligned state IDs, canonical logical-subset identity, and assured outer bindings without storing handles in
  recipes or caches. Preserve lifecycle, kernel, plan-cache, join-order, and integration evidence in
  `initial-evidence.frontier-packed-*.txt` and `initial-evidence.frontier-*-green.txt`.
- [x] (2026-07-25 17:16Z) Complete the currently theorem-safe production transform surface: exact/sampled leaf
  materialization, canonical repeated bridges, forks and exact cycle probes, exact filter restriction, projection,
  compatible union, and explicit scalar degradation for unsupported OPTIONAL and MINUS transformations. Correlated
  OPTIONAL planning now demotes invalid global hash joins and retains assured outer keys for later indexed probes.
  Preserve the focused red/green evidence in `initial-evidence.frontier-correlated-optional-hash-*.txt` and
  `initial-evidence.frontier-nested-optional-binding-*.txt`.
- [x] (2026-07-25 17:16Z) Execute all 117 catalog queries on bounded deterministic theme fixtures, compare exact
  result bags between scalar/OFF and authoritative Frontier stores, and audit every optimized plan for either a
  theorem-safe state or a typed nonempty fallback. The gate passes in 9.617 seconds; evidence is
  `initial-evidence.frontier-theme-coverage-green.txt`. Default-authoritative zero-budget stores now publish
  `disabled_zero_budget` plus `persistent_disabled_zero_budget` rather than silently omitting classification.
- [x] (2026-07-25 19:22Z) Implement bounded persistent exact-heavy discovery and additive insert generations.
  Space-Saving proposes orientation-specific candidates, a separate full pass measures candidates exactly, and only
  confirmed complete adjacencies that fit the exact partition are removed from the residual. Inserts publish ordered
  immutable exact payloads after LMDB/value commit and snapshot membership validation; deletions still dominate and
  invalidate. Manifest v3 reads legacy v2, verifies the full base-plus-insert chain under one budget, and recovers a
  post-publication dirty marker only after epoch and payload verification. Preserve red/green, crash, rollback,
  deletion-dominance, mixed-heavy, and planning evidence in
  `initial-evidence.frontier-{insert-generation,additive,heavy,synopsis-service}*.txt`.
- [x] (2026-07-25 19:00Z) Promote exact per-outer production transforms for OPTIONAL, EXISTS, NOT EXISTS, and
  MINUS. EXISTS/NOT EXISTS and MINUS use snapshot-exact bounded probes; MINUS requires an overlapping bound domain.
  OPTIONAL performs a linear tuple expansion, preserves duplicate-producing RHS bags, and emits actual matched and
  unmatched mask strata. Hard work exhaustion and non-probeable continuations degrade explicitly. Preserve
  red/green and composition evidence in `initial-evidence.frontier-{correlated-exists,minus,optional,nested}*.txt`;
  the 28-test planning integration class and bounded 117-query theme corpus are green.
- [x] (2026-07-25 19:30Z) Run the feasible sizing and empirical promotion audit and reject default promotion.
  `scripts/frontier-payload-sizing.py` models the actual 96-byte record, 144-byte selection envelope, block framing,
  and four-lane/two-direction 1B/10B tiers. A real bounded LMDB run audited 374 algebra pieces across 30 generated
  queries: 210 used Frontier, false authoritative zeros were zero, query-memory peak was 3.88 MB, p95 q-error was
  26.59, worst q-error was 1330.27, and 16 exact insert commits measured 58.42 ms p95. No certified interval or
  runtime-plan oracle exists, so interval coverage and optimizer regret remain explicitly unmeasured. The failed
  q-error and incremental-latency gates are already sufficient to retain the zero-byte default. Preserve the exact
  scope, sizing tables, commands, and claim boundary in
  `.agent/research/frontier-omnisketch/CALIBRATION.md` and
  `initial-evidence.frontier-bounded-calibration-green.txt`.
- [x] (2026-07-25 19:45Z) Complete architecture and broad verification. Cached recipes contain no live state or
  arena handles; query-evaluation passes 999 tests; LMDB unit and integration verification passes 1,663 tests with
  114 skips and zero failures or errors. Preserve the final runs in
  `initial-evidence.frontier-query-evaluation-final-green.txt` and
  `initial-evidence.frontier-lmdb-final-green.txt`. A full formatter pass completed before the last helper
  extraction; the user explicitly skipped the repeated standalone final pass, after which scoped source-header and
  `git diff --check` audits remained clean.
- [x] (2026-07-25 20:15Z) Roll Frontier out as LMDB's primary synopsis after the explicit product decision to
  supersede the failed empirical promotion gate. The default remains `AUTHORITATIVE`, the persistent maximum becomes
  512 MiB, and a missing positive-budget generation is built during store initialization. A default store captures
  subsequent commits as exact additive generations and publishes `lmdb-frontier` as its planned estimate source;
  OFF, zero-budget, dirty, unsupported, and failed-build states retain the scalar/sketch fallback. Preserve the
  rollout red/green contracts in `initial-evidence.frontier-{default-rollout,auto-bootstrap}*.txt` and the
  end-to-end priority proof in `initial-evidence.frontier-primary-default-green.txt`.
- [x] (2026-07-26 00:17Z) Harden the default-on operational path. Payload blocks are buffered instead of issuing
  one digest/checksum write per primitive; exact insert chains stop at eight immutable deltas and consolidate lazily
  from a pinned snapshot on the next supported authoritative query; property-path sessions and serializable
  observation-preserving sessions fall back atomically to scalar. Legacy scalar-estimator audits opt out explicitly.
  Focused rollout verification passes 58 tests, the audit class passes 39 tests, and optimistic isolation passes 115
  tests. The final LMDB module run executes 1,651 tests with zero errors and one unrelated failure:
  `ThemeQueryBenchmarkSparseParamTest` still expects `SPARSE` in benchmark defaults. Preserve the red/green evidence
  in `initial-evidence.frontier-{bounded-insert,serializable}*.txt`,
  `initial-evidence.frontier-rollout-audit-green.txt`, and
  `initial-evidence.frontier-rollout-lmdb-broad-final.txt`.
- [x] (2026-07-26 00:58Z) Make the production default Frontier-only. The legacy sketch synopsis is now an explicit
  compatibility opt-in in Java and repository-template defaults; default stores expose no
  `SketchBasedJoinEstimator`; the nine-theme planning/execution benchmarks run Frontier-only by default; and all
  117 bounded Theme queries preserve result bags with classified Frontier evidence/fallback. Identical Medical
  q0--q10 JMH runs show aggregate planning 4.967 percent faster and execution 1.194 percent faster. A five-sample
  q2/q3 repeat is within +1.006 percent and -2.125 percent respectively, and normalized q2/q3 physical plans are
  structurally identical apart from generated variable IDs. Preserve the contracts and benchmark record in
  `initial-evidence.frontier-only-{default-red,default-green,fallback-green,theme-benchmark}.txt`.
- [x] (2026-07-25 23:55Z) Complete rollout verification and compatibility-fixture migration. Default-path tests and
  benchmarks no longer wait for or rebuild an absent legacy estimator; tests whose subject is explicitly the old
  synopsis now opt in. The full LMDB unit phase passes 1,651 tests with 17 skips. Its only Failsafe errors were two
  stale null dereferences in `LmdbSketchAwareFilterPlacementIT`; the matching 13-case IT passes after explicit
  opt-in, and the 39-case estimator audit passes. Preserve the consolidated record in
  `initial-evidence.frontier-only-{broad-red,legacy-compat-green,final-verification}.txt`.

## Surprises & Discoveries

- Observation: the universal fixed-budget goal is mathematically impossible. A bounded particle set can be
  measure-unbiased while producing zero on most runs after an adversarial many-to-many bridge chain.
  Evidence: the supplied proof package derives relative variance `chi-square(target || proposal) / K` and a delayed
  reward chain whose required particle count grows exponentially with bridge depth. The implementation must diagnose
  effective-sample-size collapse and refine or degrade; unbiasedness alone is not an accuracy claim.

- Observation: the supplied exhaustive script reports 133,632 semantic assertions but executes only 117,248.
  Evidence: it checks five identities for each of 512 bags and seven, not eight, identities for each of 16,384 bag
  pairs. The Java port adds an independent direct-specification `EXISTS` assertion and increments its counter only
  after each assertion executes, making the advertised `2,560 + 16,384 * 8 = 133,632` count real.

- Observation: `EstimateContext.hasNoExecutions()` currently treats every scalar `prefixEstimate.rows() == 0` as an
  exact empty relation.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/EstimateContext.java` short-circuits on the
  scalar value alone. A sampled zero with a positive upper bound must not suppress work. After Frontier plumbing,
  only `invocationCount == 0` or `EvidenceZeroStatus.EXACT_ZERO` may establish this short circuit.

- Observation: current LMDB snapshot versioning is not by itself a durable persistent-synopsis identity.
  Evidence: `LmdbEstimatorRuntime.snapshotVersion()` incorporates process-local `TripleStore.dataRevision`, which
  resets on reopen. Exact term IDs are meaningful only for the matching store. The persistent Frontier manifest
  therefore needs a durable store identity, committed snapshot epoch, term/index schema version, lane-hash version,
  and dirty generation; an unverifiable identity makes Frontier evidence unavailable rather than guessed fresh.

- Observation: a naked packed integer state ID is unsafe across planning sessions and caches.
  Evidence: dense, sparse, and multiword packed tables are query-local, while `PackedPlanRecipe` and
  `PackedPlanCache` survive beyond them. A state reference must contain an arena identity plus a positive local ID,
  and no state handle may be serialized into a recipe or store-wide cache entry.

- Observation: packed plan cache hits bypass cold planning.
  Evidence: `PackedCascadesPlanner` returns a cached recipe before it opens the cold planner. A cached physical recipe
  cannot expose a prior session's evidence-state ID. If evidence telemetry is required on a hot hit, a fresh
  query-local state must be reconstructed separately.

- Observation: the usual LMDB index configuration is subject-first and predicate/object-first, not necessarily
  object-first.
  Evidence: heavy-center discovery and reverse bridge support cannot assume arbitrary exact object adjacency scans.
  The synopsis builder must use an available exact orientation, an explicit second scan, or a configured object-first
  index, and must never subtract an unconfirmed heavy candidate from the residual.

- Observation: the theme corpus is dominated by operators whose exact result may remain supported even when their
  Frontier distribution does not.
  Evidence: the current 117-query inventory contains 102 `OPTIONAL`, 32 `UNION`, 26 `EXISTS`, 14 `NOT EXISTS`, 22
  `MINUS`, 103 `DISTINCT`, 100 aggregate, 26 grouped/HAVING, one property-path, and four repeated-variable queries.
  “Full coverage” must mean correct execution plus an honest supported/degraded classification, not pretending that
  generic `DISTINCT`, nonlinear aggregation, or arbitrary property paths are theorem-safe particle transforms.
  Forty-seven queries contain multiple stars, 65 contain multiple written bridges, 18 contain cycles, and 12 contain
  reciprocal bridge pairs. The largest shapes contain 20 statement patterns, six stars, nine bridges, or 15
  `OPTIONAL` operators. Because 113 roots contain `DISTINCT` and/or aggregation, the coverage harness must also audit
  every algebra descendant so a supported star/bridge state is not hidden by an honestly degraded nonlinear root.

- Observation: positional mask payloads make silent mask sorting unsound.
  Evidence: canonicalizing masks `[11, 01]` to `[01, 11]` without applying the same permutation to exact/residual
  count arrays can attach `OPTIONAL` or `UNION` rows to the wrong bound domain. `FrontierMaskStrata.of` therefore
  requires already-canonical unsigned-lexicographic input and never silently permutes caller-owned payload columns.

- Observation: observed residual-particle count is not a randomness-taint test.
  Evidence: a state can contain positive database-exact heavy mass and retain zero sampled residual particles while
  still being `MEASURE_UNBIASED`; an empty sample is not proof that residual population mass is absent. Generic
  child transforms conservatively inherit randomness from the parent guarantee, not from the number of retained
  residual atoms.

- Observation: deterministic replay requires canonical payload representation, not only deterministic seeds.
  Evidence: the same finite measure emitted in reverse LMDB traversal order previously produced a different digest
  and failed restoration. Exact and residual entries are now allocation-free sorted per mask stratum before hashing;
  duplicate database-exact tuples are rejected until operator builders provide an explicitly budgeted coalescer.

- Observation: a hard query-memory cap must charge control handles and library sort scratch as well as primitive
  payload arrays.
  Evidence: zero-byte reservations, live leases, writer objects, and `Arrays.sort(Object[])` scratch could otherwise
  allocate outside `allocatedBytes()`. Reservations, writers, leases, payload blocks, key objects, and declaration
  scratch now all contribute conservative fixed or array-derived charges, and arena close rejects live children.

- Observation: persistent synopsis ownership and query snapshot identity meet at different LMDB layers.
  Evidence: `LmdbSailStore` owns exact term IDs, mutation locks, commit hooks, workers, and close ordering, while
  `LmdbSailDataset` privately owns the read transaction used by query evaluation. A service that checks only the
  latest store epoch can race a commit after the query dataset opens. Frontier must be owned by `LmdbSailStore`, but
  the optimizer session must receive the epoch of the exact read transaction it will execute.

- Observation: checking snapshot identity immediately before and after a cursor call does not make the cursor call
  safe.
  Evidence: the deterministic renewal regression acquired the transaction-manager write lock inside
  `RecordIterator.next()` and renewed the untracked transaction before the old implementation's post-check. Frontier
  now holds an outer read lock across version check, cursor step, and primitive-ID copy. Sink callbacks remain
  unlocked and therefore provisional until the complete scan returns successfully.

- Observation: transaction renewal must be marked at deactivation, not after reactivation.
  Evidence: LMDB can reset an untracked transaction and then fail to renew it. The previous version counter changed
  only after successful renewal, which allowed a pinned source to touch a reset cursor while still observing its old
  version. `Txn.version()` now advances before reset; successful reactivation does not count the same invalidation
  twice.

- Observation: making a cursor step atomic by wrapping an existing `RecordIterator.next()` is correct but retains a
  nested LMDB read-lock acquisition for every scanned triple.
  Evidence: the source already holds the transaction read lock across version checks and the cursor step.
  `RecordIterator.nextWithTxnReadLockHeld()` now provides a compatibility fallback, while `LmdbRecordIterator`
  overrides it with the native cursor path and ordinary callers continue to use the lock-acquiring `next()`.

- Observation: checksum-first manifest validation cannot distinguish an incomplete durable write from corruption.
  Evidence: every nontrivial prefix of a valid manifest has a checksum mismatch, but operational recovery needs the
  stronger `TRUNCATED` classification. The decoder therefore validates the bounded envelope and declared length
  before CRC, then reserves `CORRUPT` for structurally complete mismatches and trailing bytes.

- Observation: the old manifest generation and its single payload generation are currently coupled, which is enough
  for the first immutable base payload but not for the final base-plus-insert chain or deletion-only selector.
  Evidence: a dirty-deletion publication must advance the selector without writing payload data, and one clean
  selector will eventually reference base, heavy, strata, bounds, and zero or more insert generations. The current
  one-descriptor manifest remains an intentionally narrow bootstrap format and will be generalized before mutation
  hooks are connected.

- Observation: multi-gigabyte Frontier persistence cannot use the manifest's bounded `byte[]` file operation.
  Evidence: benchmark tiers reach 50 GB, while the manifest is deliberately bounded to 64 KiB. Payload blocks now
  stream through a fixed header and eight-byte primitive encoding buffer; only one configured block is materialized
  during decoding.

- Observation: a selected global hash join inside a correlated OPTIONAL can discard an inherited binding and expand
  a cheap per-row probe into a full independent scan.
  Evidence: MEDICAL_RECORDS q12 exceeded 18 minutes before completion. The smallest regression selected
  `HashJoinIteration` with a 630K-row independent child; after contextual hash demotion it passes in 0.764 seconds,
  and the real benchmark query completes planning and execution in 1.952 seconds.

- Observation: the two denormalized SOCIAL_MEDIA catalog queries are not usable as full-scale semantic regression
  fixtures. q11 combines roughly 1.4 million posts with user follows and two independent follow-expanding OPTIONAL
  branches, while its catalog expected row count remains the placeholder `1`; historical runs timed out.
  Evidence: the full fixture still hit the 120-second query limit after every leaf became an `[S,P]` probe. A
  bounded deterministic fixture executes the same algebra and exact OFF-versus-authoritative bag comparison in
  1.261 seconds; the full 117-query bounded suite passes in 9.617 seconds.

- Observation: default-authoritative, zero-persistent-budget sessions intentionally suppressed their own typed
  fallback, leaving many otherwise correctly costed queries with no Frontier classification.
  Evidence: `authoritativeZeroSynopsisBudgetPublishesStableFallback` failed with expected
  `disabled_zero_budget` but actual `null`. Exposing the already typed status outside OFF mode makes the focused
  test and all 117 classification audits pass.

- Observation: broad LMDB verification caught two repository contracts that focused Frontier tests did not:
  `LmdbEvaluationStatistics` exceeded the 500-line adapter limit, and the benchmark default had all themes except
  `MEDICAL_RECORDS` commented out.
  Evidence: the first broad run executed 1,519 tests with two failures. Extracting deterministic filter-evidence
  ordering reduced the adapter to 480 lines, restoring the full nine-theme JMH parameter list fixed the benchmark
  contract, and the repeated full module run passed 1,649 tests.

- Observation: the legacy-enabled Medical planning baseline has a highly skewed cost distribution.
  Evidence: `ThemeQueryPlanRunBenchmark.planQuery` measures q0/q1 near 0.25 ms, q2/q6/q8 near 0.47--0.58 seconds,
  and q3 at 10.605 seconds, while q3 execution is 151.506 ms. Frontier-only comparison must report each query rather
  than hide the long-tail optimizer cost in one average.

- Observation: loading the complete nine-theme benchmark fixture can exceed the 512 MiB Frontier payload maximum.
  Evidence: the first clean legacy-enabled JMH setup logged
  `Frontier exact insert generation was not published after LMDB commit: BUDGET_EXCEEDED`. A comparison that claims
  Frontier estimates must verify the generation status or use bounded per-theme fixtures; silently timing scalar
  fallback is not Frontier evidence.

- Observation: the apparent three-sample Medical q2 planning regression was benchmark noise, while the q3 outlier
  predates retirement of the legacy synopsis.
  Evidence: a two-warmup/five-measurement repeat reports q2 at 586.027 +/- 26.667 ms with the legacy synopsis and
  591.925 +/- 19.459 ms Frontier-only (+1.006 percent), while q3 improves from
  9710.447 +/- 241.620 ms to 9504.063 +/- 37.830 ms (-2.125 percent). Frontier-only q2/q3 publish READY
  authoritative evidence and produce the same rendered SPARQL and normalized physical trees as the legacy-assisted
  run.

## Decision Log

- Decision: implement Frontier OmniSketch as an additive state layer and companion LMDB synopsis; do not enlarge
  `DistributionSketch` into relational state and do not port the OmniSketch C++ code.
  Rationale: existing marginal sketches remain valuable scalar evidence, while a frontier measure has different
  lifetime, equality, mask, snapshot, and composition contracts. Independent Java code also avoids importing an
  implementation whose license and RDF/SPARQL semantics are not established here.
  Date/Author: 2026-07-25 / Codex.

- Observation: the implemented persistence record is 96 bytes rather than the plan's provisional 40-byte packed
  occurrence, and the builder reserves a 144-byte selection envelope with 25 percent stochastic headroom. The
  proposed 0.5/2.5/5 GB tiers at 1B rows therefore model residual-center rates of only
  0.032552/0.162760/0.325521 percent under four lanes and two directions.
  Evidence: `scripts/frontier-payload-sizing.py` and
  `.agent/research/frontier-omnisketch/CALIBRATION.md`.
  Date/Author: 2026-07-25 / Codex.

- Decision: make measure unbiasedness, not merely unbiased total cardinality, the correctness contract.
  Rationale: an optimizer retains a state precisely so later continuations can inspect its key distribution. For every
  supported continuation `f`, the obligation is `E[hatPhi(f)] = Phi(f)`; cardinality is only `f = 1`.
  Date/Author: 2026-07-25 / Codex.

- Decision: keep database-exact heavy mass separate from sampled residual mass and retain inherited uncertainty after
  deterministic propagation from a random parent.
  Rationale: enumerating a sampled source's adjacency removes current bridge-mutation variance but cannot erase the
  randomness already present in that source's weight. Combining the two would understate variance and could create a
  false exact zero.
  Date/Author: 2026-07-25 / Codex.

- Decision: use exact probes as the production-safe way to combine a sampled prefix with a right star or
  existence/difference operator. Random-random frontier multiplication remains unresolved unless lanes are proven
  independent or a covariance-aware construction is implemented.
  Rationale: same-lane products add covariance bias. Exact RHS evaluation per retained outer mapping is linear in the
  random measure and supports practical chains, trees, forks, `EXISTS`, `MINUS`, and `OPTIONAL` without that bias.
  Date/Author: 2026-07-25 / Codex.

- Decision: canonical evidence states are keyed by logical factor subset, ordered frontier, bound-mask strata,
  semantic/correlation scope, continuation objective, snapshot identity, and lane role, then seeded by a versioned
  stateless hash of that key.
  Rationale: physical join exploration order must not change sampled particles or give one candidate a luckier draw.
  Evicted payloads can be rematerialized exactly while immutable parents and summaries remain available.
  Date/Author: 2026-07-25 / Codex.

- Decision: preserve the public `EvidenceProfile` record and scalar `BagEstimate` equality/hash identity. An
  `EvidenceStateRef` is an optional sidecar and query-local state IDs are never cache identity outside their arena.
  Rationale: Frontier is additive and must not silently invalidate existing callers or convert live session handles
  into long-lived cache keys. Operator-specific transforms preserve/replace the sidecar; unsupported generic scalar
  arithmetic drops it and records a stable degradation.
  Date/Author: 2026-07-25 / Codex.

- Decision: use two design lanes and two held-out audit lanes. Design results may guide exploration; audit results
  validate the selected state once and are not fed into an unbounded later adaptive search.
  Rationale: ordinary confidence intervals are not reusable-holdout guarantees. Separating lane roles gives the
  optimizer useful paired comparisons without claiming robustness that the estimator does not have.
  Date/Author: 2026-07-25 / Codex.

- Decision: the latest user instruction supersedes the earlier “OFF by default” rollout target. During development,
  tests explicitly exercise `OFF` and `SHADOW`; final promotion changes the default to `AUTHORITATIVE`.
  `AUTHORITATIVE` means “use supported theorem-safe evidence, otherwise invoke the existing scalar estimator,” never
  “trust every Frontier result.” A zero persistent-synopsis budget continues to mean no new on-disk payload, so exact
  finite/probeable Frontier states may participate while sampled synopsis-dependent states fall back.
  Rationale: this satisfies default enablement without inventing a required disk allocation before the 1B/10B
  experiments or weakening soundness on unsupported shapes.
  Date/Author: 2026-07-25 / Codex.

- Decision: an arbitrary deletion invalidates nonnegative Frontier evidence until rebuild; inserts form a new
  additive generation with the heavy set frozen.
  Rationale: subtracting sampled mass or changing a data-dependent heavy partition without a signed estimator breaks
  the stated nonnegative-measure proof. Explicit unavailability is safer than silently stale evidence.
  Date/Author: 2026-07-25 / Codex.

- Decision: replayable payload states require a complete predeclared canonical key universe, and callers use
  `FrontierStateArena.find` before allocating a writer.
  Rationale: dynamic payload interning makes numeric state IDs depend on physical exploration order and reconstructs
  an already-resident payload merely to discover it is cached. Predeclaration sorts structural keys once; lookup
  makes incremental costing cheap and keeps payload construction deterministic. Dynamic `intern` remains available
  only for scalar/query-local diagnostic states.
  Date/Author: 2026-07-25 / Codex.

- Decision: generic payload transforms may not emit database-exact atoms from a measure-unbiased parent.
  Rationale: exact adjacency enumeration removes current mutation variance but cannot prove that output mass came
  only from the parent's database-exact branch. Specialized star/bridge builders may later preserve exact-heavy
  lineage using a stronger typed proof; the generic arena chooses conservative residual representation.
  Date/Author: 2026-07-25 / Codex.

- Decision: `LmdbSailStore` owns the persistent Frontier service; `LmdbStore` only creates/persists the durable UUID
  and wires configuration, and temporary spill stores created from transient `StoreProperties` never create a
  persistent Frontier service.
  Rationale: only `LmdbSailStore` spans exact term-ID access, mutation serialization, post-commit publication,
  background-worker lifetime, and safe close ordering. Giving persistence to a narrower component would either lose
  mutation visibility or attach a non-durable identity to overflow stores.
  Date/Author: 2026-07-25 / Codex.

- Decision: key persisted Frontier generations by durable store UUID plus native LMDB read transaction ID, and also
  retain the query-local transaction renewal version as an in-process invalidation guard.
  Rationale: `mdb_txn_id` survives process restart and identifies the exact LMDB snapshot; `Txn.version()` does not
  survive restart but detects auto-grow deactivate/reactivate renewal even when the native ID happens not to change.
  An untracked reader remains pinned across ordinary commits. Any renewal invalidates the entire candidate build.
  Native IDs may conservatively invalidate after initialization-only writes; that costs rebuild work but cannot make
  stale evidence authoritative.
  Date/Author: 2026-07-25 / Codex.

- Decision: resolve a zero persistent-synopsis budget before consulting any filesystem or snapshot collaborator.
  Rationale: zero remains an explicit authoritative-with-scalar-fallback opt-out even though the later production
  default is positive. Exact query-local/probeable states may still participate; sampled persistent evidence must stay
  unavailable without creating directories, opening read transactions, scheduling work, or deleting an older
  synopsis.
  Date/Author: 2026-07-25 / Codex.

- Decision: make `manifest.bin` the only live-generation selector and never infer recency from payload filenames.
  Rationale: payloads are immutable, streamable, and published before the small manifest. A crash before required
  atomic manifest replacement may leave an orphan but cannot supersede the last complete generation. Descriptor
  filenames are derived from validated kind and generation rather than read as paths from persisted data.
  Date/Author: 2026-07-25 / Codex.

- Decision: expose a lock-aware cursor step at the internal `RecordIterator` boundary rather than weakening the
  transaction locking inside `LmdbRecordIterator`.
  Rationale: normal iterator consumers still require `next()` to acquire the transaction read lock. Only callers
  that demonstrably hold that same lock use the new path, avoiding per-row nested locking without broadening native
  LMDB cursor races.
  Date/Author: 2026-07-25 / Codex.

- Decision: separate raw manifest decoding from compatibility-validated loading.
  Rationale: crash recovery and diagnostics must inspect a structurally valid manifest without claiming it is usable
  for the current store, while the production service must compare every identity field before exposing evidence.
  A mismatch is a typed unavailable status, never a best-effort reinterpretation of snapshot-scoped term IDs.
  Date/Author: 2026-07-25 / Codex.

- Decision: payload corruption protection uses both per-block CRC32C and whole-file SHA-256.
  Rationale: CRC catches and localizes block corruption during streaming, while the manifest's SHA-256 descriptor
  binds the exact ordered sequence of blocks and prevents an otherwise valid block from being substituted or
  reordered. Neither hash establishes RDF equality; stored term IDs do.
  Date/Author: 2026-07-25 / Codex.

- Decision: publish LMDB store metadata with the same forced-temporary, atomic-selector pattern required by the
  Frontier manifest.
  Rationale: the durable store UUID is part of every synopsis identity. Directly truncating `store.properties`
  could destroy the prior UUID before the service starts. Dirty state therefore clears only after temporary-file
  force, atomic replacement, and parent-directory force all succeed.
  Date/Author: 2026-07-25 / Codex.

- Decision: introduce the packed session and one aligned sparse state column before changing planner lifecycle.
  Rationale: legacy cost-model lambdas and all existing winner costs remain unchanged while the new ownership and
  no-cache-escape contracts become executable. Dense and multiword propagation, one-open/one-close lifecycle, and
  provider integration remain separate test-first slices.
  Date/Author: 2026-07-25 / Codex.

- Decision: positive-budget bootstrap verifies persisted evidence without building or retaining it.
  Rationale: startup can classify a clean generation safely before the builder and query reader exist. Manifest
  identity is checked before LMDB is touched; the pinned epoch precedes bounded streaming payload verification; all
  uncertifiable I/O leaves scalar estimation available.
  Date/Author: 2026-07-25 / Codex.

- Decision: until additive insert generations are implemented, every actual insertion durably invalidates the
  base-only generation just as an arbitrary deletion does, but with a distinct typed status.
  Rationale: leaving a pre-insert sample `READY` would make authoritative estimates stale and is unsound. Conservative
  fallback loses availability but preserves theorem safety and makes the future additive-generation optimization an
  explicit format upgrade rather than an implicit correctness dependency.
  Date/Author: 2026-07-25 / Codex.

- Decision: publish each committed insertion transaction as an exact immutable generation and keep the base heavy
  partition frozen until rebuild.
  Rationale: exact positive mass composes additively with either an exact or Horvitz--Thompson residual base without
  changing the base inclusion design. The pre-commit marker remains authoritative until membership checks, payload
  force, manifest replacement, and directory force all succeed. Any arbitrary deletion still invalidates the chain.
  Date/Author: 2026-07-25 / Codex.

- Decision: retain the zero-byte persistent default after the bounded calibration.
  Rationale: false-zero and query-memory checks passed, but p95 q-error 26.59 exceeded the target of 5 and exact
  insertion p95 58.42 ms exceeded the target of 1 ms. Sampled states currently expose no certified nominal interval,
  and no exhaustive runtime-plan oracle was available for regret. Failing mandatory gates makes promotion invalid
  regardless of the unmeasured gates.
  Date/Author: 2026-07-25 / Codex.

- Decision: supersede the zero-byte default with a 512 MiB persistent maximum and automatically build a missing
  positive-budget base generation during store initialization.
  Rationale: the user's explicit rollout decision makes Frontier the primary supported estimate source now. The
  512 MiB value is a hard maximum rather than an eager allocation; actual payload bytes remain bounded by the data.
  Existing scalar/sketch estimation remains the cold and unsupported-state fallback. This is an operational product
  decision, not evidence that the earlier q-error, insertion-latency, interval, regret, or billion-row gates passed.
  Date/Author: 2026-07-25 / Codex.

- Decision: bound the immutable exact-insert chain at eight generations and consolidate it lazily on the next
  supported authoritative planning session.
  Rationale: a generation per transaction made default-on stores pay payload construction, manifest replacement, and
  directory force for every tiny commit. Retaining the durable dirty marker keeps stale evidence unavailable; a
  pinned full rebuild captures all committed inserts once, and the existing epoch equality check still rejects a
  mismatched execution snapshot.
  Date/Author: 2026-07-25 / Codex.

- Decision: disable Frontier planning atomically when serializable observation order must be preserved.
  Rationale: exact refinement probes are speculative planning reads. Running them through the transaction dataset
  widened its observed-state set and caused false `SailConflictException`s in operations that are safe under the
  established scalar planner. Scalar fallback preserves both isolation semantics and the existing observation
  footprint.
  Date/Author: 2026-07-25 / Codex.

- Decision: make `SketchBasedJoinEstimator` and its `LmdbQuadSynopsisService` an explicit compatibility opt-in
  instead of a heap-dependent default.
  Rationale: Frontier is now the primary synopsis. A Frontier-only store may still use exact LMDB page/cardinality
  summaries, exact index probes, finite-relation surfaces, and conservative algebra heuristics because those are not
  the retired probabilistic synopsis. Unsupported Frontier transforms must not silently consult the old quad
  synopsis. Explicit `sketchEstimatorEnabled=true` remains available for compatibility tests and controlled
  baseline comparisons.
  Date/Author: 2026-07-26 / Codex.

- Decision: carry a selected plan's inherited assured bindings as one separate query-local relation ID in
  `PackedCostContext`; never append that wrapper relation to the logical factor prefix.
  Rationale: OPTIONAL and UNION may preserve some assured outer symbols while adding unassured outputs. Physical
  access-path selection needs the preserved symbols, but Frontier state identity and logical-subset composition must
  not pretend the wrapper is another simultaneously joined factor. Scope barriers clear the separate context.
  Date/Author: 2026-07-25 / Codex.

- Decision: make theme correctness coverage compare complete result bags on bounded deterministic versions of all
  nine theme datasets, with scalar/OFF LMDB as the semantic reference and authoritative LMDB as the subject.
  Rationale: this executes the exact 117 catalog algebras, including the denormalized shapes, without treating
  placeholder row counts or billion-row materialization as a correctness oracle. Full-scale runtime and regret
  remain separate empirical promotion gates and are not weakened by the bounded semantic test.
  Date/Author: 2026-07-25 / Codex.

- Decision: publish `DISABLED_ZERO_BUDGET` and its stable fallback reason in SHADOW/AUTHORITATIVE modes, while OFF
  mode remains silent.
  Rationale: zero is a supported explicit persistent-budget opt-out. A synopsis-dependent estimate must explain why it used
  the scalar path; suppressing this typed state violated the plan's stable-fallback contract.
  Date/Author: 2026-07-25 / Codex.

- Decision: keep filter-evidence quality ordering in a separate stateless package helper rather than compressing the
  LMDB statistics facade to satisfy its line-count contract.
  Rationale: the facade is an interface adapter, while validity, confidence, and evidence-count ordering form a
  cohesive deterministic policy. The extraction preserves behavior, has direct pre/post test coverage, and keeps
  the adapter below its enforced 500-line boundary.
  Date/Author: 2026-07-25 / Codex.

## Outcomes & Retrospective

The packed-planner prerequisite, research provenance, independent Java correctness oracle, theorem-safe query-local
state core, packed state transport, and first persistent base-generation lifecycle are complete. The oracle's 21
tests include the corrected 133,632 finite-bag assertions and exact rational sampling identities. The production
state layer adds exact summary-label invariants, optional `BagEstimate` sidecars, sampled-zero-safe LMDB
short-circuiting, exact term-ID tuple payloads, mask strata, stable design/audit seeds, predeclared canonical IDs,
provenance checks, raw-particle diagnostics, canonical replay, eviction/rematerialization, and conservative hard-cap
accounting. The first linear transform layer now covers exact-probe multiplicities, projection, restriction, and
union without random-random products.

The persistent service can build, atomically publish, verify, invalidate, explicitly rebuild, and restart a bounded
bidirectional center-coordinated payload. Its bounded candidate pass and exact recount freeze complete heavy
adjacencies outside the sampled residual. Ordered exact insertion payloads advance the manifest epoch without
changing that base design; rollback removes only its transaction marker, crash recovery validates the selected
chain, and deletion remains a durable fail-closed boundary. The query reader compares the manifest epoch against the
exact query dataset epoch and composes exact-heavy, sampled residual, and exact insert records into one measure.

The packed runtime now opens one query-local Frontier session, transports aligned state IDs through every subset
kernel, materializes snapshot-matched leaves, reuses canonical append/probe states, and applies the supported linear
transforms. Selected-plan contextualization preserves inherited binding assurances across nested OPTIONALs without
polluting logical state identity. The bounded theme gate executes all 117 catalog queries with exact result-bag
parity between OFF and authoritative modes and requires a supported state or typed fallback on every optimized plan.
The final broad verification passes 999 query-evaluation tests and 1,663 LMDB unit/integration tests. An explicit
cache-boundary audit finds no Frontier state IDs, arena tokens, leases, or state references in `PackedPlanRecipe` or
`PackedPlanCache`.
The bounded 30-query calibration is recorded, but it fails p95 q-error and incremental-latency promotion gates.
Physical billion-row build/disk measurements, certified confidence calibration, and optimizer regret remain
unmeasured and therefore are not production claims. A later explicit product decision nevertheless promotes
Frontier to the default with a 512 MiB persistent maximum and automatic initial generation; the failed calibration
remains a rollout risk rather than being reinterpreted as passing evidence. Default-on maintenance is bounded to
eight exact delta generations and then lazily consolidated. Serializable planning and property-path sessions retain
atomic scalar fallback so Frontier cannot widen the transaction observation set or mix unsupported path costs.

The later Frontier-only rollout removes the legacy `SketchBasedJoinEstimator` from the default runtime rather than
retaining two probabilistic synopses. Explicit opt-in remains available for controlled compatibility comparisons.
The clean-build Medical q0--q10 comparison is competitive: summed planning time improves 4.967 percent, summed
execution time improves 1.194 percent, eight of eleven execution cases improve, and no execution regression exceeds
2.110 percent. Longer q2/q3 planning runs narrow the comparison to +1.006 and -2.125 percent. The q2/q3 selected
plans remain structurally identical, while the Frontier-only telemetry proves READY authoritative Frontier
evidence. The approximately 9.5-second q3 planning cost remains a separate absolute optimizer-latency risk.
The rollout verification also distinguishes persistent and transient LMDB stores: persistent Theme fixtures publish
READY authoritative evidence, while transient model fixtures without a persistent properties file fail closed with
`STORE_MISMATCH` because they cannot own a durable store UUID. That safe fallback is visible and is not counted as a
successful Frontier measurement.

The eventual outcome must not be summarized as “fixed memory estimates every query.” The accurate production claim
is narrower: supported operations compose as bounded-memory, measure-unbiased frontier transformations under checked
support, sampling, independence, snapshot, and SPARQL assumptions; degeneration is observable; deterministic bounds
guard severe underestimates; and refinement or explicit scalar fallback is mandatory when those assumptions fail.

## Context and Orientation

The Maven module `core/queryalgebra/evaluation` contains backend-neutral cardinality and packed-planner APIs.
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cost/BagEstimate.java`
is the scalar estimate object. It wraps the public `EvidenceProfile` and currently has no relational state.
`DistributionSketch` is a marginal or tuple distribution synopsis retained inside scalar evidence; it is not a live
frontier.

The package
`core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed/`
contains the allocation-sensitive packed planner. `PackedCostModel` is a functional interface. `PackedCostContext`
is the borrowed prefix input, `PackedCostEstimate` is the reusable output slot, and `PackedJoinEnumerator` has dense,
sparse-long, and multiword search paths. `PackedLongSubsetTable` stores sparse subset state.
`PackedPhysicalMetadataArena` retains metadata for selected packed expressions. `PackedPlanRecipe` and
`PackedPlanCache` outlive the query-local search and therefore must never contain Frontier handles.

The module `core/sail/lmdb` owns RDF term IDs, indexes, sketches, storage configuration, and exact probes.
`LmdbEstimationEngine` computes semantic `BagEstimate` values. `EstimateContext` carries the current prefix.
`LmdbPackedCostModel` adapts LMDB evidence to packed primitive slots. `LmdbEstimatorRuntime.OptimizationScope` is
thread-local and depth-counted for one optimization; it is the natural owner of the query-local Frontier session.
`LmdbSailStore` owns persistent store services and the current sketch estimator. The new persistent Frontier synopsis
is a separate versioned file and manifest tied to the same committed snapshot, not a replacement for current
Count-Min, filter, range, or quad synopses.

A “star” is a group of triple patterns sharing one center term. A “bridge” is a triple-pattern relation that moves
the retained frontier from one star's center to another. A “bound mask” records exactly which variables are bound in
a SPARQL solution mapping; it matters because an `OPTIONAL` unmatched row has a different domain from a matched row.
A “lane” is one independent randomized estimator replicate. “ESS” is effective sample size,
`(sum weights)^2 / sum(weight^2)`, and warns when a few particles dominate. A “certified upper bound” is deterministic
degree or norm evidence kept independently from the random point estimate.

For one frontier/mask partition, write the state as

    hatPhi = H + sum_i W_i delta_(X_i)

where `H` is database-exact finite mass and each `X_i` is an exact tuple of LMDB term IDs with nonnegative weight
`W_i`. For a bridge relation `E(a,b)`, `m_i` independent with-replacement draws from a proposal `q_i` emit

    sum_i (W_i / m_i) sum_j (E(a_i, B_ij) / q_i(B_ij)) delta_(B_ij)

and the implementation must check `q_i(b) > 0` whenever `E(a_i,b) > 0`. Without-replacement sampling uses
first-order inclusion probabilities for Horvitz--Thompson weights; a conventional variance estimate additionally
needs second-order inclusion probabilities. Do not interchange draw probabilities and inclusion probabilities.

For a continuation `f`, repeated variance separates into inherited, mutation, and resampling terms:

    V_t(f) = V_(t-1)(K_E f) + E[M_t(f)] + E[R_t(f)].

The continuation-optimal proposal is proportional to `E(a,b) * abs(f(b))`. Production proposals approximate it
with degree, characteristic, hot-predicate, query-lookahead, and exact-heavy information while retaining a defensive
full-support mixture epsilon. The default epsilon is `0.1` and validation requires `0 < epsilon <= 1`.

SPARQL outer kernels use the exact RHS match multiplicity `r(mu)`: inner join contributes `r(mu)`, `EXISTS`
contributes `1[r(mu)>0]`, `NOT EXISTS` contributes `1[r(mu)=0]`, and `OPTIONAL` contributes `max(1,r(mu))`.
`MINUS` removes an outer mapping only when a compatible RHS mapping exists and their bound domains overlap.
Thresholding an estimated RHS count is not an unbiased existence test; these operators use exact per-outer probes or
return unresolved when the deterministic probe budget is exhausted.

## Plan of Work

### Milestone 1: exact Java correctness oracle

Create test-only classes under
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/frontier/oracle/`. Implement normalized
`BigInteger` rational arithmetic, immutable sorted partial bindings, exact integer-multiplicity bags, ordered layouts,
bound masks, mask-stratified frontier measures, weighted rational measures, and finite sampling designs. This code is
authored independently; the proof package supplies mathematical fixtures, not source.

Start with a reflection-based `FrontierOracleContractTest#exposesExactFiniteBagAlgebra`, run it red, and preserve the
Surefire report in `initial-evidence.frontier-oracle-red.txt`. Then add typed tests for projection, natural join,
union, filter, correlated and uncorrelated existence, `NOT EXISTS`, `MINUS` domain overlap, matched/unmatched
`OPTIONAL`, tuple frontiers, and mask strata.

`FrontierFiniteBagExhaustiveTest` enumerates all 512 binary bags over the nine partial mappings obtained when each of
`x` and `y` is unbound, term zero, or term one. It executes five assertions per bag. It then enumerates all 16,384
ordered bag pairs over the seven-mapping subuniverse containing empty, one-variable, compatible, and incompatible
two-variable mappings. It executes eight assertions per pair, including an independent direct `EXISTS`
specification, and asserts an actually executed total of 133,632.

`FrontierExactExpectationTest` proves with rational enumeration: star cardinality 8; exact bridge result 64; repeated
bridge result 209; one-bridge mutation variance 36; unequal-probability mutation; multinomial expectation and forced
variance `3/2`; zero-variance support-fits retention; two-stage path expectation 10; same-lane product expectation 62
versus true and cross-lane 31; resampling non-idempotence probabilities `1/4` and `3/8`; and chi-square relative
variance 31 for one particle and `31/8` for eight. Reject zero particle capacity, zero defensive support, proposal
support holes, negative multiplicity/weight, double Horvitz--Thompson adjustment, and false exact zero.

Acceptance is deterministic green tests with no Monte Carlo needed for correctness. A separately seeded statistical
regression may check empirical means, coverage, ESS, and weight concentration, but it is secondary evidence.

### Milestone 2: immutable state plumbing

Before production edits, add focused red contracts in
`core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cost/`.
Introduce `EvidenceGuarantee`, `EvidenceZeroStatus`, `EvidenceIntervalKind`, `EvidenceStateSummary`,
`EvidenceStateRef`, `FrontierLayout`, and `FrontierStateArena` beside `BagEstimate`. Mark the public experimental
types with RDF4J's `@Experimental`.

`EvidenceStateSummary` contains point, lower, and upper rows; confidence and interval kind; inherited, mutation, and
resampling variance; ESS; maximum normalized particle weight; certified upper bound; guarantee; zero status; and a
stable degradation reason. Enforce that only database-exact evidence can create `EXACT_ZERO`, and that such a summary
has point/lower/upper zero. `EvidenceStateRef` contains an arena token and positive state ID. State ID zero in packed
columns means no state.

`FrontierLayout` is ordered and rejects duplicate variable slots. `FrontierStateArena` uses primitive parallel
arrays for state summary columns, layout IDs, mask-stratum slices, exact-heavy slices, residual particle slices,
weights, flattened term-ID tuples, canonical keys, parent/provenance edges, seed versions, and lane roles. Every
temporary emission/coalescing table is explicitly charged against the 64 MiB default query budget. Evict particle
payloads, not summaries or provenance, and reproduce evicted payloads from the canonical key and stateless seed.

Add an optional `EvidenceStateRef` sidecar to `BagEstimate`. Existing constructors remain scalar-only and the public
`EvidenceProfile` record is unchanged. Exclude the sidecar from scalar `equals`, `hashCode`, and cached physical-plan
identity. Metric/work-only copies may retain the same state. A row/schema/bag transformation retains a sidecar only
through an explicit arena operation; generic scalar arithmetic drops it and supplies a degradation classification.

Change LMDB zero handling only after a red
`LmdbEstimationEngineTest#estimatedZeroPrefixDoesNotShortCircuitAsExactEmpty`. State summaries, not scalar source
strings, decide exact zero. Add same-scalar/different-state cache tests. Query-local IDs may key only session-local
caches that also validate the arena token; store-wide caches use canonical semantic keys or bypass Frontier.

Acceptance is unchanged existing scalar plans plus green layout, mask, exact-term-ID equality, snapshot/lane key,
eviction/rematerialization, capacity, support, zero, equality, and cache contracts.

### Milestone 3: persistent RDF companion synopsis

Add a versioned Frontier synopsis service under
`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/frontier/`. Keep the existing scalar and Count-Min services
unchanged. Store data in a separate payload file plus an atomically replaced manifest. The manifest names durable
store identity, committed snapshot epoch, exact term/index schema, hash/seed version, lane count, directions, heavy
generation, base/additive generations, dirty/deletion state, build parameters, payload sizes, checksums, and clean
completion marker. Corruption, truncation, identity mismatch, version mismatch, dirty deletion, or interrupted build
makes Frontier unavailable and leaves scalar estimation operational.

Build subject-to-object and object-to-subject entity universes from exact term IDs with role-neutral lane hashes. A
bounded discovery pass proposes heavy centers; exact index probes freeze their full adjacency and multiplicity; a
second complete scan collects coordinated residual entity samples, exact predicate degrees for retained centers,
characteristic strata, exact heavy neighbors, exact small adjacencies, and bottom-`L` samples for large adjacencies.
Store named first- and, when variance needs it, second-order inclusion probabilities. Heavy and residual sets are
disjoint by construction.

An insert creates an additive generation under the frozen heavy partition. Any arbitrary deletion writes the dirty
marker before mutation visibility and prevents Frontier use until rebuild. Tests cover deterministic rebuild,
manifest replacement, crash recovery, base-plus-insert composition, deletion invalidation, stale snapshots, hash
versions, corruption, truncation, and reverse orientations.

### Milestone 4: bag-correct stars, bridges, and exact probes

Implement LMDB-specific arena operations:

    int coordinatedStar(...);
    int bridgeTransfer(...);
    int probeFactor(...);
    int resolveOuterKernel(...);
    int project(...);
    int restrict(...);
    int union(...);

A `joinAndProject` facade dispatches among these operations but returns `UNRESOLVED` instead of multiplying
correlated random states without a proof of independence.

For a star with retained centers `S` and center inclusion probability `pi_s`, estimate residual bag mass as

    sum_(s in S) product_i d_i(s) / pi_s.

All coordinated arms use one center inclusion event and apply `1/pi_s` once. Repeated/shared leaf variables require
exact tuple keys and compatibility joins, never a product of marginal degrees. Exact-heavy centers contribute
database-exact mass separately.

For each bridge source particle, enumerate small fanout exactly or take `m_i` full-support neighbor draws and emit
`W_i * E(a_i,b) / (m_i * q_i(b))`. Coalesce within an explicitly bounded primitive table. If residual support fits
the output capacity, retain it unchanged and add zero resampling variance. Otherwise use multinomial resampling as
the reference conditionally unbiased path. Later priority sampling is allowed only with its exact inclusion design
and tests.

Adding a right star evaluates its complete factor exactly for each retained target whenever LMDB can probe it. This
supports repeated stars, chains, trees, and forks without a random-random product. Every path records inherited,
mutation, and resampling uncertainty, ESS, maximum weight, bound, support checks, work units, and degradation.

### Milestone 5: canonical reuse independent of join order

Represent a canonical state key by logical factor subset, ordered frontier, mask strata, semantic and correlation
scope, continuation objective, snapshot identity, and lane role. Construct each logical subset once using a
deterministic hypergraph decomposition: collapse maximal same-center stars; prefer the legal next expansion with the
smallest certified degree upper bound; minimize required frontier width; and break ties by canonical packed relation
ID. These decisions use deterministic metadata, not realized particles.

Every append request for the same logical subset returns the canonical state even when reached through a different
physical derivation. Preserve immutable parent states. Payload eviction retains key, summary, seed, and provenance,
and rematerialization must produce byte-identical particles. Exact tuple-cycle closing is supported when both
endpoints remain on the frontier and LMDB can probe the closing edge.

### Milestone 6: packed planning session integration

Keep `PackedCostModel` a functional interface and add a default `openSession(PackedQueryView)` method returning a
`PackedCostSession`. Open one session only on a cold planning call and close it with try-with-resources on success or
failure. Legacy lambda models receive a scalar adapter session.

Extend `PackedCostContext` with current prefix state ID, required frontier layout/mask, continuation objective, and
lane role. Extend `PackedCostEstimate` and `PackedPhysicalMetadataArena` with aligned primitive columns for output
state ID, lower/upper/confidence, guarantee, zero status, and stable degradation code. `clear()` and `reset()` always
restore state ID zero. Copy/restore paths must carry every aligned field.

Thread state through dense DP, `PackedLongSubsetTable`, the multiword path, incumbent search, and selected-plan
contextualization. Cost every candidate addition against the same canonical prefix state. Preserve cost and tie
ranking behavior in the plumbing milestone. After left-deep parity, enumerate connected proper bipartitions for
bushy physical plans; cardinality still comes from the canonical logical state. If a child-child random product
would be required, reconstruct through the canonical probe/bridge path or return unresolved.

Do not add state handles to `PackedPlanRecipe`, `PackedPlanCache.Context`, or cache entries. A hot recipe cache hit
has no prior session state. Tests cover lambda compatibility, one open/close per cold plan, failure close, aligned
growth/replacement in dense/sparse/multiword kernels, physical exploration-order identity, and cache non-escape.

### Milestone 7: SPARQL operator semantics

Implement linear projection, union, and evaluable filter/restriction over particles. Partition every state by exact
bound mask. `EXISTS` and `NOT EXISTS` probe correlated RHS existence per sampled outer mapping and retain its outer
multiplicity. `MINUS` additionally requires nonempty overlap of bound domains. `OPTIONAL` emits matched mappings with
their actual RHS bindings and unmatched mappings in distinct mask strata. A live projected RHS variable therefore
requires sampled or exact matched tuple bindings, not only a match count.

Filters that need discarded frontier values, exhausted negative probes, unsupported width, or non-probeable
continuations invalidate composition with a stable reason. Generic `DISTINCT`, `COUNT DISTINCT`, nonlinear
aggregates, arbitrary property paths, service-side unknown data, recursively composed random-random joins, and
unclosed cycles remain scalar/degraded fallbacks until a later theorem-safe implementation exists. The query result
remains fully supported by RDF4J; only Frontier evidence degrades.

Tests compose nested `UNION`, `OPTIONAL`, `EXISTS`, `NOT EXISTS`, and `MINUS`; mixed masks; named graphs; repeated
variables; duplicate-producing joins; reverse bridges; multiple stars/bridges; forks; and exact cycle closure.

### Milestone 8: refinement, bounds, intervals, and telemetry

Evaluate uncertainty for the continuation currently being costed. Trigger deterministic refinement when a sampled
zero has a positive upper bound, target relative standard error `0.25` is missed, maximum weight/ESS diagnoses
collapse, or competing-plan intervals overlap. The default budget is 4,096 work units. Spend units by adding
neighbor draws, exactly probing dominant particles, improving a lookahead proposal, or enumerating selected
high-impact sources. Never exceed the budget.

Keep deterministic degree and Holder-style upper bounds separate from random points. Label an interval certified
only when its boundedness, independence, or variance-envelope assumptions are actually checked. Design lanes rank
plans and form paired comparisons. Two held-out audit lanes validate the selected state once. Exposing an audit to
later choices requires a fresh audit batch or a future reusable-holdout method.

Add shadow telemetry for q-error, signed error, false sampled zero versus exact zero, interval coverage, ESS,
maximum weight, inherited/mutation/resampling variance, refinement work, fallback reason, memory, cache reuse, and
paired plan-cost difference.

### Milestone 9: configuration, theme coverage, and default enablement

Add `frontierEstimatorMode = OFF | SHADOW | AUTHORITATIVE`,
`frontierSynopsisBudgetBytes` default 512 MiB, `frontierQueryMemoryBudgetBytes` default 64 MiB, two design lanes, two
audit lanes, `frontierRefinementWorkUnits` default 4,096, target relative standard error `0.25`, and defensive
proposal epsilon `0.1`. Round-trip RDF configuration and validate nonnegative budgets, positive lane counts and
capacities, and `0 < epsilon <= 1`.

Development and calibration tests set modes explicitly. First obtain semantic and plan parity in `SHADOW`, then
promote the configuration default to `AUTHORITATIVE`. In authoritative mode a supported state may replace the scalar
cardinality; an unresolved/degraded state always uses the existing scalar estimate while publishing its Frontier
classification. With zero synopsis budget, no new persistent payload is built; exact/query-local operations remain
available and synopsis-dependent operations fall back. The default positive budget automatically publishes the
initial base generation; a zero budget remains an explicit opt-out.

Inventory every current theme query by operator and topology. `LmdbFrontierThemeCoverageIT` executes all 117 queries
and requires correct results plus either a theorem-safe state or a nonempty stable fallback reason. Add synthetic
coverage absent from the corpus: named graphs, repeated variables in one pattern, mixed bound masks, wide tuple
frontiers, reverse bridges, multi-bridge forks, exact cycles, nested negative/optional/union combinations, stale
snapshots, and exhausted probes. Work shape by shape to reduce fallback without weakening classification.

Promotion requires no semantic mismatch or false `EXACT_ZERO`; approximately 92--98 percent empirical coverage for
results labeled 95 percent intervals; supported acyclic queries through eight edges with p95 q-error below 5; p95
runtime regret below 2x and no benchmarked plan above 10x the oracle; cached incremental additions below 1 ms p95;
query memory within 64 MiB; deterministic refinement within its budget; and a stable reason/guarantee for every
fallback.

### Milestone 10: persistence sizing, cleanup, and final verification

Benchmark one-billion-triple synopsis payload budgets near 0.5, 2.5, and 5 GB, and ten-billion-triple budgets near 5,
25, and 50 GB. These are experiments, not required-size claims. They approximate 0.1, 0.5, and 1 percent payload
under four lanes, two directions, 40 packed bytes per occurrence, and 1.4 storage amplification. Measure an
additional 20--40 percent for heavy tables, strata, bounds, manifest, and metadata. Select the smallest tier meeting
accuracy and regret gates; if none does, report that result.

After behavior is green, remove duplicate adapters, consolidate state-key and budget accounting, document invariants,
run architecture checks for forbidden object-backed packed hot state, format all touched files, run complete
query-evaluation and LMDB modules, and inspect the final diff for session handles, debug output, stale temporary
artifacts, and accidental changes to user-owned files. Do not remove the current scalar fallback or claim support
for deferred nonlinear operators during cleanup.

### Milestone 11: Frontier-only default and competitive Theme planning

Make the legacy `SketchBasedJoinEstimator` default false in `LmdbStoreConfig`, the repository configuration template,
and the Theme planning/execution benchmark parameters. Preserve explicit true as a compatibility switch. The default
`LmdbSailStore` must therefore pass `null` for the legacy `LmdbQuadSynopsisService` while continuing to construct a
READY positive-budget Frontier generation. Supported factors use `lmdb-frontier`; unsupported factors use exact
storage summaries, exact probes, or conservative scalar algebra without consulting the legacy synopsis.

Run the Medical Theme queries 0--10 through both `ThemeQueryPlanRunBenchmark.planQuery` and
`ThemeQueryPlanRunBenchmark.runQuery` before and after the change with identical JDK, forks, warmups, iterations,
query variant, DPhyp mode, and store data. Compare each query's planning and execution latency. Capture representative
optimized-plan snapshots for the slow and regression-sensitive queries, verify unchanged result counts, and classify
operator/join-order differences rather than treating estimate-only drift as a regression. Do not call the
Frontier-only result competitive when a benchmark silently reports `BUDGET_EXCEEDED`; use a per-theme fixture or
otherwise prove that Frontier is READY for the measured store.

## Concrete Steps

Run every command from `/Users/havardottestad/Documents/Programming/rdf4j-small-things` with JDK 25. Never use Maven
`-am` or `-q` for tests. `mvnf` performs the required quick install before its focused verify.

For each behavior-changing slice, first add the smallest reflection or typed failing test, then run:

    JAVA_HOME=/Users/havardottestad/.sdkman/candidates/java/25-zulu/zulu-25.jdk/Contents/Home \
    MAVEN_OPTS='-Xms256m -Xmx4G -XX:+UseParallelGC -XX:ParallelGCThreads=2 -XX:+EnableDynamicAgentLoading' \
    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace <unique-workspace> TestClass#testMethod --retain-logs

Expect one Surefire failure naming the missing contract. Immediately preserve it as
`initial-evidence.<slice>.txt` with the exact workspace log and report paths. Only then edit production code. Re-run
the identical selector and expect:

    Tests run: 1, Failures: 0, Errors: 0
    BUILD SUCCESS

For Milestone 1, start with workspace `frontier-oracle-red`, then run:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace frontier-oracle \
      FrontierOracleContractTest,FrontierFiniteBagExhaustiveTest,FrontierExactExpectationTest --retain-logs

For core state and packed slices, use unique workspaces and expand from method to class, then:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace frontier-core \
      core/queryalgebra/evaluation --retain-logs

For LMDB slices, expand from the focused class to:

    python3 .codex/skills/mvnf/scripts/mvnf.py --workspace frontier-lmdb \
      core/sail/lmdb --retain-logs

Before formatting, run:

    cd scripts
    ./checkCopyrightPresent.sh

Return to the repository root, then run the repository formatter:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is not a test; the repository's mandated form includes `-q`. After formatting, rerun the
focused classes and both affected modules. Use `git diff --check`, `git status --short`, and targeted `rg` checks to
verify whitespace, no debug output, and no state fields in `PackedPlanRecipe` or `PackedPlanCache`.

Update this ExecPlan after each milestone: check completed progress with a UTC timestamp, add evidence-based
discoveries and decisions, summarize the outcome, record exact commands/logs, and append a revision note at the
bottom.

## Validation and Acceptance

Correctness acceptance begins with deterministic proof obligations. The finite-bag suite must report 512 bags,
16,384 ordered pairs, and exactly 133,632 executed semantic assertions. Rational tests must report the exact expected
values 8, 64, 209, 36, 10, 31, 62, `3/2`, `1/4`, and `3/8`. Zero capacity, support holes, invalid epsilon, double
adjustment, stale snapshots, deletion generations, and false exact zero must fail closed.

State acceptance requires byte-identical canonical IDs and particles regardless of physical exploration order,
ordered tuple-frontier and bound-mask identity, exact term-ID equality, role-neutral lane coordination,
heavy/residual disjointness, explicit inherited/mutation/resampling variance, deterministic rematerialization, and
hard temporary/persistent memory caps. A sampled zero may never produce `EXACT_ZERO`.

Planner acceptance requires dense, sparse-long, and multiword tables to retain the correct aligned state while
preserving existing winner costs/ties. One cold planning call opens and closes one session, including exceptions.
Cached recipes contain no arena token or state ID. Repeated incremental additions reuse immutable canonical prefix
states and meet the sub-millisecond p95 target after empirical calibration.

SPARQL acceptance compares exact RDF4J results with the Java oracle for nested and duplicate-producing combinations.
`EXISTS`, `NOT EXISTS`, `MINUS`, and `OPTIONAL` preserve outer bag multiplicity and bound masks; `MINUS` observes
nonempty domain overlap. A negative branch without enough exact probes is unresolved, never inferred absent.

Theme acceptance executes all 117 current queries without result regression. Each estimate exposes guarantee, zero
status, bounds/confidence where valid, and either a supported state or stable fallback reason. Unsupported
`DISTINCT`, nonlinear aggregates, and property paths are successful coverage only when their query results are
correct and their Frontier evidence is explicitly degraded; they are not counted as theorem-safe transforms.

Final empirical acceptance uses the promotion gates in Milestone 9 and the disk tiers in Milestone 10. Do not claim a
required disk size, universal relative accuracy, production confidence calibration, or optimizer regret result
before those measurements exist.

## Idempotence and Recovery

All query-local arenas are disposable. Closing or abandoning a planning session releases its payload; re-running the
same canonical key under the same snapshot and seed version reproduces it. State ID zero always means absent. A
partially implemented state column must be added and copied in all dense/sparse/multiword reset, grow, replace,
restore, and selected-metadata paths before enabling the producer.

Persistent synopsis construction writes a new generation to a temporary file, verifies length/checksum/identity,
fsyncs as required by the existing LMDB persistence pattern, and atomically replaces the manifest last. A crash
before manifest replacement leaves the prior clean generation usable. A crash after a dirty-deletion marker leaves
Frontier unavailable until rebuild. Never delete the scalar synopsis during Frontier recovery.

Focused Maven workspaces are safe to rerun. Preserve red evidence before a green run overwrites reports. If an
offline dependency is missing, rerun the exact install once without `-o`, then return offline. Do not use destructive
Git commands, delete unexpected artifacts, or modify the user-staged `core/sail/lmdb/cp.txt` and `cp-full.txt`.

If an authoritative estimate causes a semantic mismatch, false exact zero, snapshot mismatch, uncapped allocation,
or unsupported random-random product, first add the smallest reproduction, set the affected transformation to a
reasoned unresolved fallback, and repair the invariant before broadening support. Do not disable validation or
weaken the oracle.

## Artifacts and Notes

The packed functional prerequisite is preserved in:

    initial-evidence.packed-prereq-green.txt
    .mvnf/workspaces/packed-prereq-lmdb-green/logs/20260725T051002.584826Z-31519-be17abfa/verify.log
    Summary: tests=1544, failures=0, errors=0, skipped=114

Research provenance is:

    .agent/research/frontier-omnisketch/README.md
    .agent/research/frontier-omnisketch/SOURCE_LEDGER.md
    .agent/research/frontier-omnisketch/CHECKSUMS.md

The supplied proof archive is identified there by SHA-256
`bbdcd88f1797fdb0a6495d5a6e2f7b72448e41ee8fc4b7eed99865d8e408a9ca`. Its Python/C++ sources and PDFs are not
vendored or translated. `THEORY.md` and `PROOF_STATUS.md` are retained as checksum-addressed design provenance, not
as repository source.

Record future evidence here as compact command, report, and summary triples. Keep full Maven output in the named
workspace logs rather than pasting it into this plan.

## Interfaces and Dependencies

In `org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cost`, define:

    enum EvidenceGuarantee {
        DATABASE_EXACT,
        MEASURE_UNBIASED,
        CERTIFIED_BOUND_ONLY,
        SCALAR_FALLBACK,
        UNRESOLVED
    }

    enum EvidenceZeroStatus {
        EXACT_ZERO,
        ESTIMATED_ZERO,
        POSITIVE,
        UNRESOLVED
    }

    enum EvidenceIntervalKind {
        NONE,
        EXACT,
        POINTWISE,
        SIMULTANEOUS,
        PAIRED,
        ASYMPTOTIC,
        HEURISTIC
    }

`EvidenceStateSummary` exposes point/lower/upper rows, confidence, interval kind, three variance components, ESS,
maximum weight fraction, certified upper bound, guarantee, zero status, and degradation reason.
`EvidenceStateRef` is an immutable opaque `(arena token, positive state ID)` handle.
`FrontierLayout` exposes an ordered unique variable layout.
`FrontierStateArena` is `AutoCloseable`, owns immutable states and primitive payload/provenance columns, validates
handles, resolves summaries, enforces budgets, evicts/rematerializes payloads, and supplies canonical stateless seeds.

Add to `BagEstimate`:

    public Optional<EvidenceStateRef> evidenceState();
    public BagEstimate withEvidenceState(EvidenceStateRef state);
    public BagEstimate withoutEvidenceState();

Existing public constructors remain and create scalar-only estimates. `EvidenceProfile` remains unchanged.

In the packed package, preserve `PackedCostModel` as the single-abstract-method interface and add:

    default PackedCostSession openSession(PackedQueryView query) {
        return PackedCostSession.scalar(this, query);
    }

`PackedCostSession` is `AutoCloseable`; it estimates leaves, appends logical factors, refines operators, resolves
summaries, and returns evidence-state IDs through reusable slots. Exact method parameters are primitive IDs/views and
reusable output objects, not allocated per-candidate records.

In LMDB configuration add `FrontierEstimatorMode` and the fields from Milestone 9. In the LMDB Frontier package add
the seven operations listed in Milestone 4 plus manifest/build/read services. Use only JDK and existing RDF4J/LMDB
dependencies; add no third-party runtime library.

Revision note (2026-07-25 / Codex): initial self-contained ExecPlan created after the packed functional prerequisite
passed. It incorporates the theorem's positive and impossibility results, independent source provenance, the
corrected exhaustive assertion contract, LMDB snapshot/index constraints, packed cache/session boundaries, the
latest default-enable instruction with sound scalar fallback, theme coverage semantics, and empirical disk/accuracy
gates.

Revision note (2026-07-25 05:51Z / Codex): recorded Milestone 1 red-to-green completion, the corrected exhaustive
assertion total, exact sampling fixtures, preserved evidence, and the transition to immutable production state
plumbing.

Revision note (2026-07-25 07:20Z / Codex): completed the immutable state/payload core after two independent
theorem-safety reviews. Recorded predeclared payload IDs, canonical mask and particle order, stable lane-code vectors,
randomness-taint rules, derived ESS/max-weight diagnostics, exact-heavy empty-residual semantics, replayable
provenance constraints, live-child lifecycle checks, and hard-cap control-object accounting.

Revision note (2026-07-25 17:16Z / Codex): recorded packed runtime integration, supported production transforms,
correlated-OPTIONAL physical-context fixes, explicit zero-budget fallback telemetry, and the green 117-query bounded
semantic parity/classification gate. Separated these completed correctness obligations from the still-unmeasured
large-scale empirical and disk-sizing promotion gates.

Revision note (2026-07-26 00:34Z / Codex): added the Frontier-only default milestone after the product owner directed
retirement of the legacy synopsis. Recorded the clean-build prerequisite, legacy-enabled Medical planning/execution
baseline, the red default contract, the 10.6-second q3 planning outlier, the complete-fixture 512 MiB budget failure,
and the requirement to prove READY Frontier evidence before claiming benchmark competitiveness.

Revision note (2026-07-26 00:58Z / Codex): completed the Frontier-only default milestone. Recorded Java/template and
Theme benchmark defaults, compatibility opt-in behavior, focused and 117-query fallback verification, identical
q2/q3 selected-plan structure, full q0--q10 execution/planning comparisons, the longer q2/q3 timing repeat, and the
remaining absolute q3 planning-latency risk.

Revision note (2026-07-25 23:55Z / Codex): completed broad rollout verification. Migrated stale default-path waits
to legacy-optional helpers, made legacy-specific estimator tests opt in explicitly, recorded the 1,651-test green
unit phase plus post-fix green audit/IT selections, and documented the fail-closed transient-store identity behavior.
