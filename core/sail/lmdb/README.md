# LMDB estimation and derived statistics

Frontier Statistics V2 is the Frontier implementation used by the LMDB planner. A query with an eligible mapped generation opens a session pinned to that generation. When statistics are disabled, unavailable, too stale, or ineligible for the transaction's isolation level, planning uses conventional scalar estimation and its retained estimation services. Serializable planning retains its established observation footprint.

V2 maintains exact statement totals, Count-Min frequencies, projected-distinct statistics, heavy-predicate and heavy-object evidence, Omni design and audit lanes, and center samples. Fast-AGMS join projections cover subjects, objects, and contexts; predicates identify groups rather than adding a fourth projection. Current builders use HLL accumulators to produce projected-distinct scalar shards. Missing optional scalar evidence reports unavailable evidence and does not invalidate otherwise usable mandatory statistics.

Committed insertions and deletions flow through the durable mutation journal and signed delta generations. Rollback does not publish uncommitted mutations. Pinned query views remain valid across generation publication. Learning and active planner caches continue to use the retained V2 evidence and generation identities.

## Configuration

`frontierEstimatorMode` selects `AUTHORITATIVE`, `SHADOW`, or `OFF`. `frontierSynopsisBudgetBytes` is V2's persistent budget, and `frontierHeapBudgetBytes` is its sole explicit heap budget. The default heap budget is one quarter of the maximum JVM heap. `frontierStatisticsMaxLagMillis` controls permissible lag. Internal build profiles determine Omni lane counts and delete reserve.

The retained cache settings are `frontierCacheEvidenceBudgetBytes`, `frontierPlanCacheMaximumVariants`, `frontierPlanCacheRefreshThreads`, and `frontierPlanCacheMaximumCanaryFraction`. Surviving cache allowances retain their original fractions of the configured cache budget: exact finite surfaces 1/16, heuristic filter passes 1/32, pipeline plans 1/16, and packed plans 1/4. The remainder is not redistributed.

Quad estimation remains independently controlled by `sketchEstimatorEnabled`, `sketchEstimatorMemoryBudgetBytes`, `sketchEstimatorThrottleEveryN`, `sketchEstimatorThrottleMillis`, and `sketchEstimatorEvidenceMode`. `sketchEstimatorColdSynopsisCapacity` retains its cold-filter capacity semantics. Quad synopsis persistence is unchanged.

## Rebuilding and persistence

Call `LmdbStore.rebuildFrontierStatistics()` after initialization and outside an active write transaction. The method returns `FrontierStatisticsStatus` directly, including availability, fallback reason, generation identity, coverage, and build diagnostics. Disabled V2 returns its existing unavailable status.

Statistics manifests use container version 6 exclusively. This version protects changed shard ordinals and compact signed Fast-AGMS coordinates. Incompatible derived generations follow the normal unavailable-and-rebuild lifecycle from authoritative LMDB data. There is no migration reader and no sweep of old synopsis directories. Existing quad data remains independent of this statistics format.

Count-Min allocates 30 tables: 15 nonempty bound masks for each of the explicit and inferred statement planes. Mask zero uses exact totals. Hash domains, lane counts, mask identities, and counter semantics are preserved.

## Verification

Run focused tests with the checkout's `mvnf` runner, which installs the current modules before testing:

    python3 .codex/skills/mvnf/scripts/mvnf.py FrontierStatisticsBuilderTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbFrontierStoreLifecycleTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPackedCostModelV2SessionTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Manifest version, signed delta, configuration, mapped learning, snapshot pinning, corruption recovery, quad persistence, and cold-filter tests provide focused coverage alongside the full module suite.
