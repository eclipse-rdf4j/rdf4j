# RDF4J LMDB Join Order Optimizer — Engineering Design Document

**Document type:** Engineering design document  
**Audience:** Maintainers, performance engineers, release owners, tech leads  
**Project:** LMDB-specific SPARQL join planning in RDF4J

---

## 1. Executive summary

The RDF4J LMDB store already knows how to choose a good index **once a statement lookup is executed**, but the default join optimizer still behaves largely as if storage were generic.

That mismatch causes real problems:

- the optimizer can pick the wrong first pattern,
- it can ignore the configured LMDB index set,
- it cannot properly value runtime-bound variables as indexable probes,
- and it cannot exploit ordered access because LMDB’s ordered APIs are currently stubbed.

This project addresses that gap by introducing an **LMDB-local physical planner** and the minimum runtime support needed to use it.

The resulting design:

- limits code churn outside LMDB,
- preserves current behavior for non-LMDB stores,
- is feature-flagged,
- supports staged rollout,
- and provides strong testability and observability.

---

## 2. Why this work matters

### 2.1 User-visible impact

Bad join order is one of the most expensive planner mistakes in RDF systems because a small logical reordering can change execution from:

- a few thousand narrow prefix probes

to

- repeated full or near-full scans.

When this happens in LMDB, the store is blamed even though the underlying runtime index selection is often fine. The real issue is that planning did not expose the right bound values to the runtime access path.

### 2.2 Engineering value

This project improves more than raw performance.

It also gives the team:

- a clearer separation between logical and physical planning,
- explainable access-path decisions,
- reproducible plan behavior under different index configurations,
- and a foundation for later work such as adaptive planning or index advising.

---

## 3. Problem framing

### 3.1 Current asymmetry

Current LMDB behavior has three distinct layers:

1. **storage layer**  
   index-aware at runtime

2. **statistics layer**  
   partly LMDB-aware, but only for compile-time constants

3. **join optimizer**  
   generic and largely store-agnostic

The join optimizer is the weak link.

### 3.2 Failure modes we are explicitly targeting

1. A statement pattern is selective only **after** a previous join binds one of its variables.
2. The generic optimizer treats that pattern as weak because the value is not a compile-time constant.
3. LMDB actually has an index that would make the parameterized probe cheap.
4. The optimizer starts elsewhere and creates a huge intermediate result.

Other failure modes:

- merge join is impossible to exploit because ordered access is not implemented,
- configured custom index sets do not materially influence join order,
- explain output does not show enough planner intent.

---

## 4. Constraints

### 4.1 Technical constraints

1. The implementation must fit into RDF4J’s existing evaluation-strategy and optimizer-pipeline architecture.
2. The LMDB store already has established internal classes (`LmdbStore`, `LmdbSailStore`, `TripleStore`, `LmdbEvaluationStatistics`) and the design should reuse them.
3. Public API expansion should be minimal.
4. Correctness is non-negotiable.
5. Planner failure must not make queries fail.

### 4.2 Organizational constraints

1. The change should be reviewable by a normal-sized team.
2. It should be mergeable without a broad generic-optimizer redesign.
3. It should support rollback if performance regressions appear in edge workloads.

### 4.3 Performance constraints

1. Planner overhead must remain modest.
2. Statistics rebuild must not dominate every query.
3. Ordered access must not regress non-ordered cases.

---

## 5. Alternatives considered

### Alternative A — do nothing

#### Description

Keep generic join ordering and rely on runtime `TripleStore.getBestIndex(...)`.

#### Pros

- zero implementation cost
- zero rollout risk

#### Cons

- core problem remains unsolved
- runtime index selection arrives too late to fix bad join order
- known LMDB workload regressions remain possible

#### Decision

Rejected.

---

### Alternative B — patch generic `QueryJoinOptimizer` for all stores

#### Description

Teach RDF4J’s generic optimizer about index sets, binding-sensitive access paths, and order-aware planning.

#### Pros

- one optimizer for all stores
- no store-local duplication

#### Cons

- far larger blast radius
- generic abstractions are not yet rich enough
- likely requires cross-store API design before implementation
- risk of destabilizing non-LMDB stores

#### Decision

Rejected for this project.

This may be a future architectural direction, but it is the wrong first move.

---

### Alternative C — subclass generic `QueryJoinOptimizer`

#### Description

Create `LmdbJoinOrderOptimizer extends QueryJoinOptimizer` and override a few hooks.

#### Pros

- less code duplication in theory

#### Cons

- current generic optimizer stores critical planning state in private inner classes
- required behavior changes are deeper than a few hooks
- subclassing would be fragile across upstream changes
- hard to combine DP search, order-state tracking, and LMDB-only access-path logic cleanly

#### Decision

Rejected.

We will **copy and adapt** the relevant logic into an LMDB-local optimizer.

---

### Alternative D — LMDB-local planner via custom strategy factory and pipeline

#### Description

Add a store-specific evaluation-strategy factory and optimizer pipeline that replaces only the join-ordering stage for LMDB.

#### Pros

- contained code change
- exploits existing factory/pipeline extension points
- preserves other stores
- easy to feature-flag
- easy to roll back

#### Cons

- some duplication of generic optimizer logic
- requires maintaining LMDB-local planner code

#### Decision

Chosen.

This is the correct implementation boundary.

---

### Alternative E — exact per-value multi-dimensional statistics

#### Description

Maintain full histograms or exact distinct/frequency maps for all useful prefixes and value combinations.

#### Pros

- more accurate selectivity estimates
- better skew handling

#### Cons

- high storage complexity
- expensive updates
- difficult delete semantics
- large implementation scope
- unnecessary for first delivery

#### Decision

Rejected for v1.

---

### Alternative F — prefix distinct-count statistics only

#### Description

Maintain per-index row counts and distinct prefix counts for prefix lengths 1..4.

#### Pros

- simple
- directly useful for parameterized-prefix fanout
- one pass per index to build
- low memory footprint

#### Cons

- average-based, not skew-aware
- approximate for some non-prefix filters

#### Decision

Chosen.

This is the best complexity-to-value trade-off for v1.

---

### Alternative G — bushy join enumeration

#### Description

Search arbitrary join trees.

#### Pros

- potentially better for some disconnected or symmetric workloads

#### Cons

- much larger search space
- less aligned with parameterized nested-loop execution
- more complex physical-property tracking
- more code and more regression risk

#### Decision

Rejected for v1.

Use left-deep DP for small groups and greedy for larger groups.

---

## 6. Why the chosen design is the right one

The chosen design is appropriate because it matches the actual performance bottleneck.

The LMDB store does **not** need a brand-new storage engine or a generic statistics framework first.  
It needs the planner to stop throwing away physically meaningful information that is already present:

- configured indexes,
- prefix scoring,
- and the fact that earlier joins bind later probes.

By keeping the solution LMDB-local, the team can ship meaningful value sooner and evaluate it in production-like workloads before deciding whether a generic RDF4J abstraction should later emerge from it.

---

## 7. Architecture decision summary

### ADR-1 — Planner lives in LMDB module

**Decision:** `LmdbJoinOrderOptimizer` resides in `core/sail/lmdb`.  
**Reason:** access to `TripleStore` and internal stats without widening generic APIs.

### ADR-2 — Strategy factory owns pipeline selection

**Decision:** default LMDB strategy factory is replaced.  
**Reason:** cleanest insertion point, minimal disruption.

### ADR-3 — Prefix statistics are cached snapshots

**Decision:** use in-memory cached snapshots with dirty-commit rebuild policy.  
**Reason:** avoids write-path complexity and makes planner behavior deterministic.

### ADR-4 — Ordered access is implemented in LMDB dataset

**Decision:** finish the `SailDataset` ordered APIs for LMDB.  
**Reason:** without this, order-aware planning is incomplete.

### ADR-5 — Mixed groups fall back

**Decision:** LMDB DP only for pure `StatementPattern` join groups in v1.  
**Reason:** strict safety boundary.

---

## 8. Rollout strategy

### 8.1 Feature flags

The implementation must ship behind config flags in `LmdbStoreConfig`.

Required flags:

- optimizer enabled
- merge join enabled
- prefix statistics enabled
- build stats on init
- refresh threshold
- DP threshold

### 8.2 Rollout phases

#### Phase 0 — dark launch in tests/benchmarks

- code merged
- feature enabled only in targeted test configurations
- broad benchmark coverage collected

#### Phase 1 — opt-in production evaluation

- feature available but not necessarily default in every downstream deployment
- compare explain plans and benchmark traces
- collect regressions

#### Phase 2 — default on for LMDB

- feature enabled by default
- documented rollback path remains

#### Phase 3 — tuning and cleanup

- tune weights/thresholds if necessary
- decide whether public config surface should be expanded or reduced

### 8.3 Rollback path

Rollback must be one config change:

```text
queryOptimizerEnabled = false
```

When disabled, LMDB should use standard optimizer pipeline behavior.

---

## 9. Compatibility and migration

### 9.1 Backward compatibility

This project is backward compatible in the following sense:

- query results remain unchanged,
- existing LMDB indexes remain valid,
- existing repositories do not need reindexing,
- user-provided custom evaluation strategy factories still win.

### 9.2 Behavioral changes

Expected changes:

- different join orders for some queries,
- new explain-plan metadata,
- ordered statement-access methods implemented,
- possible appearance of merge joins in execution plans.

### 9.3 Migration cost

No data migration is required.

Statistics snapshot is ephemeral and rebuildable.

---

## 10. Operational considerations

### 10.1 Startup behavior

If `queryOptimizerStatisticsBuildOnInit = false`, startup cost remains almost unchanged.

If true, expect a one-time scan over configured indexes.

### 10.2 Warm-up behavior

The first optimized query after startup may trigger stats build if lazy build is enabled.

This is acceptable because it is deterministic and measurable.

### 10.3 Write-heavy workloads

Statistics are refreshed by dirty-commit threshold, not after every commit.  
This is intentional. The planner can tolerate stale-but-reasonable stats better than the write path can tolerate rebuilds on every mutation.

### 10.4 Read-heavy workloads

This is the primary target scenario and should benefit most.

---

## 11. Observability and diagnostics

### 11.1 Required logs

At debug level, log:

1. stats snapshot build start/end and duration
2. number of indexes scanned
3. join group size and whether LMDB DP or generic fallback was used
4. candidate access paths for each chosen step
5. final ordered plan
6. merge-join acceptance/rejection reason

### 11.2 Explain-plan expectations

A useful explain plan should expose:

- statement-pattern order in the final join sequence
- chosen index per statement pattern
- whether the join is merge or nested loop
- estimated rows and/or cost if available

### 11.3 Benchmark telemetry

Benchmarks should capture:

- planning time
- execution time
- result counts
- chosen index sequence
- number of merge joins
- number of full-scan patterns

---

## 12. Risk register

### Risk 1 — planner regressions for mixed or unusual queries

**Cause:** new planner scope too broad or insufficient fallback.  
**Mitigation:** restrict LMDB DP to pure `StatementPattern` groups in v1.

### Risk 2 — ordered access implementation incomplete due to wrapper datasets

**Cause:** an intermediate dataset wrapper fails to delegate order methods.  
**Mitigation:** audit all LMDB-used wrappers and add delegation tests.

### Risk 3 — comparator mismatch causes invalid merge join

**Cause:** ordered iterator sort order and comparator not aligned.  
**Mitigation:** compare by LMDB internal IDs only for LMDB ordered path; verify with dedicated tests.

### Risk 4 — stats build too expensive on large repositories

**Cause:** full index scans at refresh time.  
**Mitigation:** lazy build, dirty threshold, config flag, preserve previous snapshot on rebuild failure.

### Risk 5 — stale stats cause suboptimal plans in write-heavy workloads

**Cause:** refresh policy intentionally coarse.  
**Mitigation:** configurable threshold, safe fallback, benchmark with write/read mix.

### Risk 6 — code duplication with generic optimizer drifts over time

**Cause:** LMDB planner copies generic traversal logic.  
**Mitigation:** keep copied surface small, document divergence points, periodically diff against generic optimizer.

### Risk 7 — full-scan ordered paths accidentally reintroduced

**Cause:** order-support logic too permissive.  
**Mitigation:** explicit invariant: never advertise order when best prefix length is zero.

---

## 13. Benchmark program

### 13.1 Benchmark goals

The benchmark program must answer:

1. Did planning improve query execution time?
2. Is planning overhead acceptable?
3. Are chosen plans stable and explainable?
4. Did any workload regress?

### 13.2 Dataset mix

Use at least:

- synthetic star dataset
- synthetic chain/path dataset
- mixed schema/instance data
- context-heavy multi-graph dataset
- one real-ish workload dataset if available internally

### 13.3 Query mix

Use at least:

1. highly selective anchor + fanout
2. same query with different bound entry points
3. predicate-heavy lookups
4. object-heavy lookups
5. join on subject
6. join on object
7. cases where merge join should help
8. cases where merge join should be rejected

### 13.4 Index-set matrix

Benchmark each query mix under:

- default LMDB indexes
- one subject-oriented custom index set
- one object-oriented custom index set
- one richer index set with alternative same-prefix order choices

### 13.5 Metrics

Mandatory metrics:

- planning latency
- execution latency
- total rows produced
- chosen join order
- chosen indexes
- merge-join count
- full-scan count

---

## 14. Review strategy

### 14.1 Code review slices

Split review into small, comprehensible PRs if possible:

1. strategy factory + pipeline wiring
2. stats manager + triple-store snapshot build
3. ordered dataset support + comparator + ordered union
4. LMDB optimizer core
5. tests and benchmarks

### 14.2 Reviewer mix

At minimum involve:

- one LMDB store maintainer
- one query-evaluation/optimizer maintainer
- one performance/benchmark reviewer

### 14.3 Review questions

Reviewers should explicitly ask:

1. Can this ever change results?
2. Does ordered support ever lie?
3. Can this produce a worse-than-before full scan when a good prefix exists?
4. Is the fallback path clear and reliable?
5. Is there any write-path penalty hidden in the design?

---

## 15. Release criteria

The feature is release-ready only if all of the following are true.

### Correctness

- all existing LMDB query tests pass
- new optimizer tests pass
- merge-join correctness tests pass
- multi-context ordered iteration tests pass

### Stability

- fallback path is exercised in tests
- feature flags work
- no deadlocks or rebuild races in stats manager

### Performance

- representative workloads improve or stay flat
- no catastrophic regressions remain unexplained
- planner overhead remains acceptable

### Diagnostics

- explain plan shows enough information to debug regressions
- logs reveal whether LMDB planner or generic fallback was used

---

## 16. Documentation requirements

Before release, publish at least:

1. feature overview
2. config options
3. explanation of index-sensitive join planning
4. note that ordered APIs are now implemented for LMDB
5. troubleshooting guidance:
   - how to disable feature
   - how to enable debug logging
   - how to compare plans under different index sets

---

## 17. Long-term follow-up items

These are explicitly **not required** for this delivery, but the design should not block them.

### 17.1 Adaptive runtime re-optimization

If a probe fanout is dramatically different from estimate, future work could replan remaining joins.

### 17.2 Persistent stats snapshot

If first-query stats rebuild is too expensive on huge stores, persist snapshot to disk.

### 17.3 Skew-aware stats

Heavy hitters or sampled histograms could improve estimates for extreme skew.

### 17.4 Generic abstraction extraction

If similar logic later appears in NativeStore or other backends, a generic physical-planner abstraction may be worth extracting.

### 17.5 Index advisor

Once access-path telemetry exists, an offline advisor could recommend missing indexes.

---

## 18. Decision log

| Decision | Status |
|---|---|
| Keep implementation local to LMDB | Accepted |
| Use custom strategy factory and pipeline | Accepted |
| Copy/adapt generic join traversal instead of subclassing | Accepted |
| Use prefix distinct-count snapshots | Accepted |
| Enable ordered access in `LmdbSailDataset` | Accepted |
| Restrict LMDB DP to pure statement-pattern groups in v1 | Accepted |
| Use left-deep DP up to threshold, greedy above | Accepted |
| Require feature flag rollback path | Accepted |

---

## 19. Concrete acceptance criteria

This project is successful when all of the following can be demonstrated.

### Criterion A — index-sensitive planning

Given the same query and data but different configured LMDB indexes, the optimizer produces different physically appropriate join orders.

### Criterion B — binding-sensitive planning

Given the same join group but a different earlier binding sequence, the optimizer chooses a different next statement pattern when prefix probes become available.

### Criterion C — order-aware planning

When two inputs can produce the same useful order at best prefix quality, merge join may appear.  
When they cannot, it does not.

### Criterion D — safe fallback

Mixed join groups continue to work and use generic behavior.

### Criterion E — operational safety

A config change can disable the feature entirely and restore old behavior.

---

## 20. Recommended implementation policy

1. **Ship correctness and observability first.**
2. **Tune constants only after benchmark evidence.**
3. **Do not broaden planner scope until ordered runtime support is proven stable.**
4. **Preserve deterministic decisions and explicit tie-break rules.**
5. **Keep rollback easy.**

---

## 21. Final engineering recommendation

Proceed with the LMDB-local planner.

It is the smallest change that actually fixes the right problem:

- the join optimizer currently does not know what LMDB can really do,
- and the storage layer already contains enough information to make materially better choices.

The engineering risk is manageable because the design is:

- bounded,
- feature-flagged,
- fallback-friendly,
- benchmarkable,
- and incrementally reviewable.