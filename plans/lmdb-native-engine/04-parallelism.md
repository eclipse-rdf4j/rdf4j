# 04 — Parallelism

Goal: parallel execution is elastic (concurrent queries share cores instead of all-or-nothing), admitted
on total work rather than root cardinality, and workers run the same vectorized machinery the serial path
runs — with the two verified soundness exclusions (worker merge walks, worker SIP seeks) preserved.


## Current state

Admission is all-or-nothing at JVM scope: `tryReserveTasks` refuses unless the FULL group fits
(`reserved > maxTasks - tasks`, `LmdbNativeParallelPipelines.java:106-120`;
`configuredThreads() = P-1`, `configuredMaxTasks() = P`, `:93-104`), so with P processors the second
concurrent parallel query needs P-1 of a budget with 1 remaining — refused on every box with P > 2; DOP is
frozen for the cursor's lifetime (released `:552-557, :762`). The aggregate path already reserves only
`threads` and runs its producer inline on the query thread (`LmdbNativeParallelAggregation.java:121,
:531-549, ~:553-560`) — the pattern the pipelines path lacks. Admission quantity is the ROOT pattern's
static estimate ≥ 50_000 (`:183-186`) with no downstream-fanout term; any LIMIT/OFFSET refuses
(`:154-156`); any non-`PatternPlan` child refuses (`:164-168`) — but top-level OPTIONAL/UNION/MINUS/paths
never reach that gate (they produce a `JoinPlan`, rejected earlier as `"not-multi-join"`, `:157-159`);
the actually-rejected children are `ValuesPlan`, `MultiValuePatternPlan`, and reshape-constructed heads.
Workers share `plan.children` by reference and fork only top-level filters (`forkWorkerPlans:257`);
`MultiValuePatternPlan` embeds a mutable memo AND a captured query-thread LMDB source
(`LmdbNativeMultiValuePatternPlan.java:27, :35`; `LmdbNativeFilters.java:340-356`) — a real race and a
cross-thread txn use if naively admitted; `ValuesPlan` alone is immutable with a fresh cursor per open
(`LmdbNativeRowPlans.java:380-414`) and has no safety basis for rejection.

Worker capability: the vectorized worker prefix exists but is default-OFF
(`externalRootCandidateEnabled()`, `LmdbNativeChunkPipeline.java:47-49`), so a worker runs the scalar
`openChainFrom` chain and its `NativeBatch` (`LmdbNativeParallelPipelines.java:632`) is only a
cross-thread output page. `tryOpenPrefixFrom` correctly strips merge walks, SIP and hash builds from
workers (`:146-152, :168, :174`) — correctness-required (non-monotonic worker key streams), not
recoverable. Batch merge/hash joins are unavailable inside workers entirely; the sort is single-threaded
end to end (plan 05); parallel aggregation merges worker tables serially on the query thread.

Range-partition mode vs morsel mode are exclusive (`partitions == null` decides,
`:456-460, :603-605`); `produceMorsels` already takes `workers` as a parameter
(`LmdbNativeExchange.java:47`) and documents that surplus workers finish early (`:141-143`).


## Work item 1 — Elastic task admission

Replace all-or-nothing with admission of the largest COMPLETE group that fits:

1. `tryReserveTasks` returns the granted size: request `(producer?, desiredWorkers)`, receive
   `grantedWorkers ∈ {0, 1, ..., desired}` where a morsel-mode grant always includes the producer slot
   (workers block on the producer's `ArrayBlockingQueue`, `:431, :605` — a producer-less grant deadlocks;
   this is the invariant, encode it in the reservation API, not in callers).
2. Adopt the aggregation pattern in the pipelines path: producer runs inline on the query thread
   (removing one reserved slot), matching `LmdbNativeParallelAggregation`'s shape.
3. Range-partition mode needs no producer; a grant of k workers claims partitions dynamically from the
   existing `ConcurrentLinkedQueue` (`:427-429`) — already correct for any k ≥ 1.
4. Floor: grant 0 → sequential (today's fallback); grant 1 in range mode is still a win (query thread +
   1 worker); grant 1 in morsel mode equals sequential, treat as 0.
5. Sizing: `desiredWorkers` comes from the cost model (plan 01 §3) rather than always P-1, so a
   medium-size query stops reserving the whole machine it cannot use.

LMDB reader capacity is not the constraint (max_readers 256 vs pool cap 65, `TripleStore.java:245`;
rationale documented at `LmdbNativeParallelAggregation.java:133-135`).

Tests: concurrency test with two simultaneous large queries asserting BOTH get `parallel` execution-path
tags with degraded-but-nonzero worker counts; existing
`earlyCloseCancelsWorkersAndLeavesStoreUsable` extended to the elastic path.
Acceptance: on 8 cores, two concurrent identical large BGPs each complete in ≤0.6× their sequential
time (versus today: one parallel, one fully sequential); bimodal latency distribution collapses.


## Work item 2 — Admission on work, not root cardinality

Replace `root.estimate(row) >= 50_000` with the plan-level work product from plan 01 §2
(`MultiJoinPlan.estimate` upgraded to chained `estimateForBoundMask`), threshold expressed as
work-units versus measured startup overhead. Keep `MIN_PARTITION_ROWS = 8192` (`:63`) as the
partition-granularity floor — it is not an admission gate (fallback to morsel mode still parallelizes,
`:604-605, :56-59`). Remove the slice veto per plan 01 §4(a). Re-admit `ValuesPlan` children (immutable,
source-free, bag-eligible when `bindsAllSlotsEveryRow`, `LmdbNativeSlotPlan.java:162`); keep rejecting
`MultiValuePatternPlan` until its embedded filter grows a `forkForParallelWorker` implementation that
clones the memo and re-resolves the source per worker — implement exactly that under the existing
contract (`LmdbNativeFilters.java:39-56`: "must not share mutable counters, memo tables, or close
ownership"), then admit it too.

Non-goal: parallelizing plans headed by reshape-constructed non-pattern children
(`reshapeLeftJoinForFactorization` heads at children[0], `LmdbNativeGroupStep.java:732-772`) — the
exchange binds raw quads via `PatternPlan.bind` (`LmdbNativeExchange.java:212, :308`) and unchecked casts
assume the root is a pattern (`LmdbNativeParallelAggregation.java:101`); a left-input-partitioning
exchange is a separate design, deferred to the backlog unless benchmarks show the OPTIONAL-headed shape
dominating real workloads (decision recorded in plan 13).

Acceptance: an 8k-row root fanning out ≥100× is admitted (today refused); a 60k-row root joining to
nothing is refused (today admitted); dispatch-contract tests pin both.


## Work item 3 — Vectorized workers by default

After plan 02 §4 lands (per-worker leases), flip `externalRootCandidateEnabled` default to on, with the
worker prefix continuing to strip merge/SIP/hash-build (correctness). Then measure and delete the flag or
keep it as a documented kill switch (plan 13 rollout table). The chunk pipeline already runs per-worker
in the factorized branch (`LmdbNativeParallelPipelines.java:601-616`); this item extends batch execution
to the non-factorized worker branch that today runs the scalar chain.

Acceptance: worker CPU profiles show batch fills replacing per-row cursor advances on the pipeline
benchmark; ≥1.3× on parallel non-factorized shapes.


## Work item 4 — Parallel sort and parallel aggregation merge

(a) Sort: per-worker local sort + parallel disjoint-slice merge. Each worker sorts its partition into
runs using the (pruned, plan 05 §2-3) sort buffer; the merge phase adopts Kuzu's comparator-agnostic
disjoint-slice scheme (`key_block_merger.cpp:69-137`): pop two sorted runs, binary-search split points so
threads merge disjoint slices of the same pair concurrently; `NativeSpillSort` already produces and
merges multiple sorted runs (`LmdbNativeSort.java:703-786`) — the merge dispatcher generalizes its
`MergedRunRows` from a serial PriorityQueue to sliced pair-merges. Per-thread comparator state is
mandatory: `AggContext` holds a plain HashMap value cache and one shared `ValueComparator`
(`LmdbNativeAggregateState.java:177-179`) — give each worker its own. Encounter-order tiebreak becomes
nondeterministic among equal keys — spec-legal; the ordinal tiebreak (`LmdbNativeSort.java:184-186`)
remains within each run.

(b) Aggregation merge: worker tables are merged serially today. Hash-partition worker outputs by group
key (radix on the existing 64-bit key hash) into W buckets during accumulation; merge bucket b across all
workers on worker b in parallel. Distinct SUM/AVG channels follow plan 10 §4's deferred-recompute design
(accumulate only the ID sets, fold at merge), which this partitioning makes single-owner per group.

Acceptance: ORDER BY over 10M rows scales ≥0.5×N workers on N=8; parallel GROUP BY merge phase drops
from serial O(groups) on the query thread to parallel, visible in the aggregation benchmark at ≥4M
groups.


## Work item 5 — Batch joins inside the parallel plan

Once C∩D is cost-arbitrated (plan 01 §3), add the composed option: per-worker hash builds over the
worker's partition when the build side is partition-local (build input reachable from the partitioned
root without crossing the partition boundary — decidable from the derived order: build pattern's
non-key terms bound only by root-produced slots), else a shared read-only build constructed once by the
query thread before workers start (build once, then N probes — Kuzu's shape), guarded by the same memory
budget as the serial build. This is the last piece of "vectorization and parallelism compose".

Acceptance: the C∩D benchmark shape (`?s :p ?x . ?s :q ?y`, both ≥1M) runs parallel WITH per-worker or
shared hash joins and beats both today's single-threaded batch join and the scalar parallel chain by ≥2×.
