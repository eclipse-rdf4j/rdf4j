# Remove the legacy on-heap CSR adjacency implementation

This ExecPlan is a living document maintained according to `.agent/PLANS.md` (repository root). The sections
`Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work
proceeds. This is a Routine D change (significant refactor); per the repository rules no pre/post evidence blocks are
required, but the module verify at the end must be green modulo the pre-existing failures recorded below.

## Purpose / Big Picture

The LMDB sail (`core/sail/lmdb`) has two derived in-memory adjacency accelerators. The legacy one is an on-heap CSR
cache ("CSR" = compressed sparse row): `LmdbCsrAdjacencyCache` keeps one complete on-heap index per
`(predicate, direction, explicit)` and is enabled by default through the system property
`rdf4j.lmdb.csrCache.enabled` (default `true`). The new one is the strictly in-memory, off-heap direct adjacency
index of `plans/lmdb-native-engine/27-in-memory-direct-adjacency.md` (`LmdbDirectAdjacencyStore` and the
`Lmdb*Adjacency*` classes), complete in its in-repo scope but disabled by default
(`LmdbStoreConfig.directAdjacencyMode = DISABLED`).

After this change the CSR implementation no longer exists and the direct adjacency index takes over as the default
acceleration path (user decision 2026-07-28): a plain `LmdbStore` opens with `directAdjacencyMode = PREFER`, full
coverage, AUTO memory cap (50% of effective `-Xmx`, resolved once), and an automatic asynchronous build on start.
Query results never change: every read that used to arbitrate `direct → CSR → LMDB` arbitrates `direct → LMDB`, and
LMDB stays authoritative with exact fallback whenever the direct index cannot prove coverage.

Observable outcome: `rg -n "Csr" core/sail/lmdb/src/main` reports no adjacency-cache symbols;
`core/sail/lmdb` verify is green modulo the five pre-existing failures recorded at clean HEAD `00f199cccf` in
`post-evidence-direct-adjacency-m9-module-verify.txt` (kernel-decline census gate, StrategyPriorityTest, three
factorized fork-parity tests); and a default-config store serves bound probes from the direct index once its build
publishes (provable through `LmdbAdjacencyMetrics`).

## Context and Orientation

All paths are repository-relative; every CSR reference below was verified 2026-07-28.

The legacy CSR class set, all in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`:

- `LmdbCsrAdjacencyCache.java` (~2,460 lines; nested `CsrEntry`, `CsrSlot`, `KeyCounts`, `CommitMerger`; many static
  counters; all `rdf4j.lmdb.csrCache.*` system properties).
- `LmdbCsrCommitDelta.java` (commit event collector; consumed only by `TripleStore` and the cache).
- `LmdbCsrRunIterator.java` (bound-key probe iterator; used by the cache and `LmdbSailStore.CsrProbeSupport`).
- `LmdbCsrScanIterator.java` (dense-key root/ordered scan iterator; used only by the cache).
- Nested in `LmdbSailStore.java`: `CsrNativeAdjacency` (the CSR adapter for the kernel SPI
  `NativeLmdbQuerySource.NativeAdjacency`) and `CsrProbeSupport` (per-probe CSR front end).
- Nested in `TripleStore.java`: `CsrCommitListener` plus fields `csrCommitListener`/`csrCommitDelta`, the tee calls
  in `recordFanOutAdded`/`recordFanOutRemoved`, `setCsrCommitListener`, `drainCsrCommitDelta`, the
  `begin(...)` call in `startTransaction()`, the `beforeRevisionBump` call and the `reset()` calls in
  `endTransaction(boolean)`.

Main-source touch points outside those classes:

- `LmdbSailStore.java`: field `csrCache` and construction (`LmdbCsrAdjacencyCache.enabled()`,
  `setCsrCommitListener`); the eager/auto-warm machinery (`csrEagerPredicates`, `csrEagerBuildExecutor`,
  `csrEagerBuildPending`, `scheduleEagerCsrBuild`, `runEagerCsrBuild`); close/shutdown; the SERIALIZABLE
  `csrBypass`/`csrEligible` gate; commit-time `applyCommittedDelta(drainCsrCommitDelta())`; estimator helpers
  `fanOutMean`, `fanOutDegree`, `orderedIntegerDomain`; `ParallelSnapshotSource` (CSR-only paths); and the
  `LmdbSailDataset` arbitration sites for statements/ordered statements/root-scan partitions/partitioned
  scan/count/has/meanFanOut/exactDegree plus `RetainedNativeProbe`'s CSR arm.
- `LmdbRootScanPartition.java`: hybrid class — the `LmdbKeyRange` half is the LMDB split-key partition used by the
  parallel pipelines and must survive; the CSR-slice half (fields `csrEntry/csrPredicate/csrContext/csrExplicit/
  csrDirection`, factory `csrSlice`, accessors, `isCsrSlice`) is removed.
- `evaluation/LmdbNativeAttemptMetrics.java`: CSR cache counters (`CSR_CACHE_ADMISSIONS/REFUSALS/AUTO_WARM_BUILDS/
  PREDICTIVE_EVICTIONS/ADMISSION_SKIPS`), `recordCsrCache*` methods, `csrCacheMetrics()` and the `CsrCacheMetrics`
  record are removed. The probe-level telemetry (`chunkCsrBackedProbes`, `recordChunkCsrBackedProbe`, explain key
  `nativeChunkCsrBackedProbesActual`) and `LmdbNativeChunkPipeline`'s SIP-mask plumbing (`publishCsrMask`) key off
  `NativeProbe.adjacencyCacheBacked()/adjacencyCacheKeys()`, which only the CSR probe support ever set; they remain
  compiled but dormant (see Decision Log).
- `evaluation/LmdbNativePathPlan.java`: `csr`-named booleans track "frontier served from an in-memory adjacency
  view" and are fed by the same probe SPI; they stay (dormant) with the SPI.
- Javadoc: `LmdbDirectAdjacencyIterator.java` links `{@link LmdbCsrRunIterator}` and must be reworded.
- Comment-only mentions elsewhere are updated opportunistically when the file is touched anyway.

Configuration: CSR has no `LmdbStoreConfig`/`LmdbStoreSchema` surface at all; it is entirely system-property driven
(`rdf4j.lmdb.csrCache.*`), so removal deletes properties, not schema. Direct adjacency has the config surface from
plan 27: `directAdjacencyMode` (DISABLED default today), `directAdjacencyCoverage`, `directAdjacencyPredicate`,
`directAdjacencyMaxBytes` (0 = AUTO), `directAdjacencyBuildOnStart` (false default today, validated to require a
non-DISABLED mode).

Functionality that only CSR provides today, and its fate (user accepted the trade by choosing removal + direct
default):

- Root/predicate-wide scan slices (`tryPlanRootScanPartitions` CSR slices, `tryScan` with both endpoints unbound,
  `tryOrderedScan` root forms): direct adjacency intentionally declines these; the pre-existing LMDB key-range
  partitioner (`TripleStore.planRootSplitKeys` + `getTriplesRange`) remains and continues to feed the parallel
  pipelines.
- `orderedIntegerDomain` range pushdown: CSR-only; the SPI stays (default `null`) and the consumer degrades to no
  pushdown. Possible future direct implementation from the codec's ordered-integer tag bit.
- Kernel key enumeration (`supportsKeyEnumeration/keyCount/keyAt`): only `CsrNativeAdjacency` offered it; kernels
  that need key enumeration decline cleanly via `LmdbNativeKernelBindings` and fall back.
- SIP masks / `adjacencyCacheBacked()`/`adjacencyCacheKeys()`: only the CSR probe set them; the SPI defaults stay
  and the downstream plumbing goes dormant.
- Popularity-based auto-warm/eager predicate warmup and predictive eviction: no direct equivalent; direct is
  build-on-start with full/selected coverage.
- `ParallelSnapshotSource` acceleration: it was CSR-only and now reads pure LMDB; giving it a direct read view is
  explicitly out of scope here (plan 27 says parallel siblings decline by design).
- `meanFanOut`: `LmdbSailStore.fanOutMean` consulted CSR first, then `TripleStore.meanFanOut`
  (`LmdbFanOutStats`, CSR-independent). `LmdbDirectAdjacencyStore.meanFanOut` exists but was never wired; this plan
  wires it in as the first choice so optimizer statistics keep an exact source when the direct index serves.

## Plan of Work

Milestone R1 — direct adjacency becomes the default. In `config/LmdbStoreConfig.java` flip the field default to
`DirectAdjacencyMode.PREFER` and convert `directAdjacencyBuildOnStart` to explicit-set tracking (`Boolean`, null =
unset) whose getter resolves: explicit value if set, otherwise `directAdjacencyMode != DISABLED`. Validation keeps
rejecting only an explicit `true` combined with `DISABLED`. Export writes `DIRECT_ADJACENCY_MODE` only when the mode
differs from the new PREFER default, and `DIRECT_ADJACENCY_BUILD_ON_START` only when explicitly set (mirroring the
existing coverage explicit-set precedent), so untouched defaults still export nothing and every explicitly parsed
pair round-trips. Update `LmdbStoreConfigTest` default/round-trip/validation vectors first and use them as the
failing-then-passing gate for this milestone. Acceptance: `LmdbStoreConfigTest` green; a store constructed from a
default config resolves PREFER/FULL/AUTO/build-on-start.

Milestone R2 — strip `LmdbSailStore`. Remove the `csrCache` field and construction, `setCsrCommitListener` call,
eager/auto-warm executor and methods, close/shutdown block, `applyCommittedDelta(drainCsrCommitDelta())`,
`CsrNativeAdjacency`, `CsrProbeSupport`, and every `csrCache`/`csr != null` arm in `ParallelSnapshotSource`,
`LmdbSailDataset`, and `RetainedNativeProbe`. Rename `csrBypass`/`csrEligible` to `adjacencyBypass`/
`adjacencyEligible` (direct adjacency already shares the eligibility gate). `fanOutMean` consults the dataset's
direct read view (`LmdbDirectAdjacencyStore.meanFanOut`) first, then `TripleStore.meanFanOut`; `fanOutDegree`
collapses into the existing direct-first `exactDegree` path; `orderedIntegerDomain` returns `null`.
`planRootScanPartitions` keeps only the LMDB split-key path; the partitioned `statements` overload rejects nothing
anymore (the CSR-slice branch and its guards disappear).

Milestone R3 — strip `TripleStore` and `LmdbRootScanPartition`. Remove the CSR listener/delta members, interface,
tee calls, transaction begin/commit/rollback hooks, and `drainCsrCommitDelta`. Reduce `LmdbRootScanPartition` to the
key-range form. Acceptance for R2+R3: module compiles; `LmdbDirectAdjacency*` suites, `TripleStoreTest`, and
`LmdbSailStoreTest` (minus the deleted CSR-coupled test method) green.

Milestone R4 — metrics and evaluation cleanup. Remove the CSR cache counters, `recordCsrCache*` methods,
`CsrCacheMetrics` record and `csrCacheMetrics()` from `LmdbNativeAttemptMetrics`, and the sole external caller
(`recordCsrCacheAutoWarmBuild` inside the deleted warmup). Leave the dormant probe-level SPI and its consumers
untouched. Fix the `{@link LmdbCsrRunIterator}` javadoc in `LmdbDirectAdjacencyIterator`.

Milestone R5 — delete the four CSR classes and the CSR test surface. Delete `LmdbCsrAdjacencyCache`,
`LmdbCsrCommitDelta`, `LmdbCsrRunIterator`, `LmdbCsrScanIterator`. Delete the CSR test files:
`LmdbCsrAdjacencyCacheTest`, `LmdbCsrCommitMergeTest`, `LmdbCsrPredictiveEvictionTest`, `LmdbCsrSnapshotTest`,
`LmdbCsrCacheQueryTest`, `LmdbCsrAutoWarmTest`, `LmdbCsrEagerPredicateTest`, `LmdbCsrPartitionedScanTest`,
`LmdbCsrAdjacencyBulkCopyTest`, `evaluation/LmdbNativeCsrCacheMetricsTest`, `evaluation/LmdbCsrSipMaskTest`, and
the now-unreferenced helper `CsrProbeWarmupAccess`. Patch incidental users: remove the CSR-counter test method from
`LmdbSailStoreTest`; delete `LmdbNativeRangePushdownTest` (its feature, CSR `orderedIntegerDomain`, is gone); strip
now-inert `rdf4j.lmdb.csrCache.*` property lines from the kernel/benchmark tests (they rely on the direct index
being default-on from R1 for kernel coverage; where a test needs a deterministic published index, use the existing
`buildNowForTest`-style seams instead of sleeps).

Milestone R6 — format, audit, verify. Run the copyright check and formatter, `rg` audits (no `Csr` symbol in main;
no `csrCache` property), then module verify for `core/sail/lmdb` (and `core/sail/base` untouched sanity). Compare
failures against the five pre-existing ones recorded in `post-evidence-direct-adjacency-m9-module-verify.txt`;
triage anything new to root cause. Update plan 27's living sections with a pointer to this plan, update this plan's
Progress/Outcomes, and hand off.

## Progress

- [x] (2026-07-28) Plan 27 closed out (M4B skipped by user decision); root `-Pquick` clean install green.
- [x] (2026-07-28) Complete dependency census of CSR references (main, config, tests, docs) captured into Context.
- [x] (2026-07-28) User decision recorded: direct adjacency becomes the default path (PREFER + build-on-start).
- [x] (2026-07-28) R1: config default flip + `LmdbStoreConfigTest` gate. Red 3 failures (default still DISABLED,
  explicit DISABLED/false not round-tripping), then green 92/92 after explicit-set tracking for mode/buildOnStart.
- [x] (2026-07-28) R2: `LmdbSailStore` stripped — cache field/construction, warmup executor, `CsrNativeAdjacency`,
  `CsrProbeSupport`, all arbitration arms, `fanOutMean` rewired to `LmdbDirectAdjacencyStore.meanFanOut` first,
  `fanOutDegree` collapsed into direct-first `exactDegree`, `orderedIntegerDomain` degraded to null,
  `csrEligible`/`csrBypass` renamed `adjacencyEligible`/`adjacencyBypass`.
- [x] (2026-07-28) R3: `TripleStore` CSR listener/delta/tee/commit hooks removed; `LmdbRootScanPartition` reduced to
  the key-range form. `TripleStoreTest` 29/29, `LmdbSailStoreTest` 41/41 green.
- [x] (2026-07-28) R4: CSR cache counters/`CsrCacheMetrics` removed from `LmdbNativeAttemptMetrics`; javadoc link
  fixed in `LmdbDirectAdjacencyIterator`; dormant probe-level SPI retained per Decision Log.
- [x] (2026-07-28) R5: four CSR classes and twelve CSR test files deleted (incl. `LmdbNativeRangePushdownTest` with
  its CSR-only feature and the unreferenced `CsrProbeWarmupAccess`); `LmdbSailStoreTest` CSR-counter test removed;
  inert `rdf4j.lmdb.csrCache.*` lines stripped from kernel/benchmark tests and helpers repointed at the direct
  index.
- [x] (2026-07-28) Switchover gap found and fixed: unpinned tracked datasets (`snapshotRevision == -1`, the default
  isolation for ordinary connections) never acquired an exact direct view, so kernels lost adjacency and the two
  OPTIONAL kernel tests failed (`adjacency-unavailable` 282× in the diagnostic run — the executable red). The
  dataset now acquires its view at the current data revision for unpinned tracked transactions; the pre-existing
  reset-on-commit version fence in `tryDirect`/`directEligible` guarantees the view is never newer than the LMDB
  snapshot it accompanies. `LmdbNativeKernelExecutionTest` 5/5, `LmdbDirectAdjacencySnapshotTest` 4/4,
  `LmdbDirectAdjacencyCommitTest` 10/10, `LmdbDirectAdjacencyQueryTest` 15/15 green after the fix.
- [x] (2026-07-28) User-reported regression: MEDICAL_RECORDS theme query 0 at 6.15 ms/op, ~4x slower than the CSR
  baseline, with the irAggregate kernel still engaging. The attached JFR
  (`ForkedMain_2026_07_28_235346.jfr`) shows the direct read path paying a full
  `LmdbAdjacencyRunCodec.resolve` (21.2% of samples) on every edge access plus the locator walk
  (`pageRef` 9.0%, `findGroupRun` 8.7%) and per-access varint re-scans — CSR was one flat on-heap array load.
- [~] Perf fix implemented (2026-07-29), verification pending: `LmdbDirectAdjacencyIterator` now decodes in
  256-edge chunks through the codec's batch `copy` (one run-view resolution per chunk instead of two per edge),
  and `LmdbDirectNativeAdjacency` materializes runs of at most 8,192 edges once per handle into reusable flat
  buffers serving `size`/`neighborAt`/`contextAt`/`copyNeighbors`/`copyContexts`/`lowerBound` (unsigned
  (neighbor, raw-context) binary search preserved); larger supernode runs keep the codec batch path. Remaining:
  targeted suites + theme benchmark re-measurement.
- [ ] R6: format, audits, module verify, plan updates, handoff (module verify running).

## Surprises & Discoveries

- The CSR cache has zero `LmdbStoreConfig`/`LmdbStoreSchema` surface; it is entirely system-property driven, so the
  config schema only gains behavior (new defaults), never loses vocabulary.
- `LmdbDirectAdjacencyStore.meanFanOut` was implemented but never called; the removal wires it in (it currently
  declines internally — a Milestone-8 note in plan 27 — but the seam is now live).
- Observation: plan 27's serving model only covered pinned SNAPSHOT datasets; ordinary connections (SNAPSHOT_READ
  and below → unpinned tracked transactions, `snapshotRevision == -1`) were entirely CSR territory. Removing CSR
  without extending direct serving to them silently reduced every default read to raw LMDB and permanently
  disabled kernel adjacency.
  Evidence: `LmdbNativeKernelExecutionTest#optionalArmEngagesWithNullExtension` failed with 282
  `adjacency-unavailable` declines; `acquire(-1)` always fell back with `SNAPSHOT_BEFORE_BASE`.
- The safety argument for unpinned serving already existed: tracked transactions are reset (version bump) inside
  the commit critical section before `dataRevision` advances, and `tryDirect` already fences on
  `txn.version() != pinnedTxnVersion`, so a view acquired at the creation-time revision can never serve after the
  transaction was renewed onto a newer snapshot.

## Decision Log

- Decision: remove CSR outright rather than deprecating it behind a flag.
  Rationale: explicit user instruction ("remove the legacy csr implementation") after plan 27 completion.
  Date/Author: 2026-07-28 / user and Claude.
- Decision: direct adjacency becomes the default (PREFER, FULL coverage, AUTO cap, build on start).
  Rationale: CSR was on by default; a plain removal would regress every default install to raw LMDB scans. User
  selected "Direct becomes default" when asked explicitly. Plan 27's disabled-by-default rollout stance is
  superseded for this codebase; its PREFER sign-off gates targeted 20B-scale deployments, not this decision.
  Date/Author: 2026-07-28 / user and Claude.
- Decision: `directAdjacencyBuildOnStart` becomes explicit-set tracked (null = unset resolves to
  `mode != DISABLED`).
  Rationale: a bare `true` default would make an explicitly DISABLED mode fail the existing validation; resolving
  from the mode keeps "defaults export nothing" and the validation matrix intact, mirroring the coverage precedent.
  Date/Author: 2026-07-28 / Claude.
- Decision: keep the dormant probe-level SPI (`adjacencyCacheBacked`, `adjacencyCacheKeys`, SIP-mask and
  path-frontier plumbing, `chunkCsrBackedProbes` telemetry) instead of excising it.
  Rationale: it is the natural seam for a future direct-adjacency reimplementation (key rosters need enumeration,
  which direct declines by design today); excising it would churn kernel/pipeline code far beyond the removal's
  blast radius for no behavioral gain.
  Date/Author: 2026-07-28 / Claude.
- Decision: `orderedIntegerDomain` degrades to `null` (feature loss) rather than being reimplemented on direct.
  Rationale: the direct codec records ordered-integer runs per group, not per plane; a plane-wide domain would be
  new feature work outside a removal plan. `LmdbNativeRangePushdownTest` is deleted with the feature.
  Date/Author: 2026-07-28 / Claude.
- Decision: `ParallelSnapshotSource` reads pure LMDB after removal; wiring a direct read view into it is out of
  scope.
  Rationale: plan 27 records that parallel sibling sources decline by design; changing that is a feature, not a
  removal.
  Date/Author: 2026-07-28 / Claude.
- Decision: unpinned tracked datasets acquire their direct read view at the current data revision, relying on the
  reset-on-commit version fence for exactness, instead of keeping direct serving pinned-SNAPSHOT-only.
  Rationale: the user's goal is a switchover, not only a deletion; without this, every ordinary connection
  (SNAPSHOT_READ default) loses acceleration and kernels can never bind adjacency. The fence makes the served view
  provably no newer than the accompanying LMDB snapshot; a commit interleaving anywhere between transaction begin
  and view acquisition bumps the tracked transaction's version and serving declines.
  Date/Author: 2026-07-28 / user (goal) and Claude (mechanism).

## Outcomes & Retrospective

- (pending)
