# 07 — CSR adjacency cache and storage access

Goal: the CSR cache serves every access shape it can answer — ordered scans, degree queries, range
pruning, SIP mask sourcing — with admission and eviction policies that scale to the machines it runs on,
instead of being an accelerator only for unordered probe traffic.


## Current state

The cache composes with vectorization (both iterators implement bulk `fill`) and nested-loop probes;
seven consult sites in `LmdbSailStore.java` (`tryScan:2983/:3228`, `tryServe:3067/:3392`,
`tryCount:3105/:3446`, `tryHas:3120/:3466`). Verified limitations:

- **Ordered paths never consult it.** `LmdbNativePatternPlan.java:203-204` returns
  `source.statements(statementOrder, ...)` before the probe branch (`:206`); the ordered overload
  (`LmdbSailStore.java:3245-3262`) has no consult — although a `BY_OBJECT` entry reproduces exactly the
  posc emission order (`LmdbCsrScanIterator.java:15-18`), i.e. `StatementOrder.O` is already served and
  falls through. Standalone merge joins can therefore never be CSR-served
  (`LmdbNativeMergeJoin.orderedSide` re-plans both operands ordered, `:126-127`).
- **Zone map dead.** `neighborMin`/`neighborMax`/`allOrderedIntegers` computed per build
  (`LmdbCsrAdjacencyCache.java:263-282`, finals `:464-470`), zero readers; javadoc names the intent
  ("soundness gate for numeric range pushdown", `:466-469`). Runs are sorted (`:447-451` — emergent from
  the default `spoc,posc` index set, `TripleStore.java:129`; user-configurable via
  `config/LmdbStoreConfig.java:160`), yet `countRun` and `hasInRun` are unconditional linear walks
  (`:499-515, :518-533`) and `countRun(key, -1, -1)` — reached from the `count(-1,p,o,-1)` path
  (`LmdbSailStore.java:3105/:3446` → `:388-398`) — recomputes what `runStart[dense+1]-runStart[dense]`
  holds. `LmdbCsrRunIterator` inherits the no-op `seekForward` (`RecordIterator.java:65-67`); only the
  scan iterator implements it (`LmdbCsrScanIterator.java:112-136`, whose within-run refinement is itself
  linear `:137-148`).
- **SIP sourcing refused.** A CSR-served probe stage sets `hashBuildRefused = true`
  (`LmdbNativeChunkPipeline.java:1013-1017`) — right about the memo (the cache answers O(1)), wrong
  about the mask (SIP's payoff is ROOT-scan pruning, which the cache does not refund), and
  `CsrEntry.keysByDense` is already the ascending key array `SipMask` wants.
- **Composite-source merge degradation.** `OrderedRecordIterator.components()` merges on the full
  4-field key when all iterators report the same `getIndexName()` (`OrderedRecordIterator.java:74-95`),
  but `NativeSourceReadLockedRecordIterator` (`LmdbSailStore.java:3700-3826`) does not override
  `getIndexName()` and inherits `""` (`RecordIterator.java:53-55`) while the planner validates against
  the true 4-char name (`CompositeNativeLmdbQuerySource.java:167-179`) — planner and iterator disagree;
  this is what keeps the merge-join key gate single-field (plan 09 §3 depends on this fix).
- **Policy inconsistencies.** Admission: `total < minProbes()` (1024, `:78-81, :146`), amortization test
  `total * SEEK_COST_KEYS < SWEEP_PASSES * predCard` (`:159` ⇒ predCard/4), entry cap
  `estimateBytes > maxEntryBytes()` (64 MB flat, `:89-92, :162-165`) — while the global budget is
  heap-proportional `max(256 MB, heap/4)` (`:83-87`): on 32 GB heap, an 8 GB budget fillable only by
  ~128 small entries, and any predicate above ~2.4M pairs (64 MB / 28 B) permanently uncacheable.
  Eviction is an O(n) LRU scan; any write anywhere starts `storeTxnStarted` and disables the cache for
  all readers, invalidating every entry; bytes leak on store close (`GLOBAL_USED_BYTES` not credited).
  Build aborts count toward `MAX_BUILD_FAILURES_PER_REVISION = 3` (`:62, :150-152, :238-242`).


## Work item 1 — Consume the zone map; O(1) degree; run binary search

1. Entry-level early-out: a bound-neighbour probe with `constant > neighborMaxId` (or `<` min) answers
   zero rows in O(1); exact-ID equality pruning is sound even ungated (IDs unique per value); numeric
   RANGE pruning additionally requires `allNeighborsOrderedIntegers` (raw-long min/max == numeric min/max
   only then, `ValueIds.java:100-125`). Note bounds accumulate unconditionally (`:280-281`) independent
   of the ordered flag (`:282`), and empty entries report false with sentinel bounds (`:307, :463`).
2. `countRun(key, -1, -1)` returns the prefix-sum delta directly — O(1) replacing O(degree); the hub
   case (`COUNT(*)` over `?x rdf:type :Person`) is the headline win.
3. Binary search in `hasInRun`/`countRun` with a bound neighbour, and a run-level binary-search
   `seekForward` on `LmdbCsrRunIterator` (write it fresh — the scan iterator's key-level search does not
   transfer; its within-run refinement is linear). Build-time guard: assert the run sort invariant
   against the actual index field order used by the build sweep (`:231, :266, :269-294`) so a
   non-default index config (`pocs` etc.) disables the binary paths instead of corrupting them.
4. Per-run min/max is `neighbors[runStart[d]]` / `neighbors[runStart[d+1]-1]` — O(1), no extra storage;
   do NOT port Kuzu's per-chunk zone maps (their chunks are unsorted on the filtered column; these runs
   are sorted, so binary search strictly dominates skip hints).

Tests: unit tests per operation against handcrafted entries including empty runs, single-element runs,
and the non-default-index config (asserting fallback); equivalence vs linear walks under randomized
fuzz.
Acceptance: `SELECT (COUNT(*)) WHERE { ?x rdf:type :Person }` on a 5M-member class answers from the
prefix sum (µs, was ms); range-pruned probes visible in telemetry.


## Work item 2 — CSR serves ordered scans

Add an order-direction-matched consult to the ordered overload (`LmdbSailStore.java:3245-3262`): serve
`StatementOrder.O` from a BY_SUBJECT entry's... — precisely: serve the orders each entry PROVABLY
reproduces under the build sweep's index order (BY_SUBJECT runs ascend (object, context); BY_OBJECT runs
ascend (subject, context), `:447-451`); the consult verifies the requested order against the entry's
recorded emission order (record it on `CsrEntry` at build time rather than re-deriving). With item 1's
`seekForward` on both iterators, a CSR-served ordered scan is also merge-seekable — making standalone
merge joins (`LmdbNativeMergeJoin`) and zig-zag walks CSR-backed for the first time.

Also fix the composite wrapper: `NativeSourceReadLockedRecordIterator` delegates `getIndexName()` to its
wrapped iterator, so `OrderedRecordIterator`'s full-4-field merge engages when the planner already
validated it (unblocking plan 09 §3's multi-key merge gate).

Tests: order-verification wrapper (assert nondecreasing under the requested order) on CSR-served
ordered scans across both directions and mixed-context entries; merge-join dispatch-contract case
showing a CSR-served side.
Acceptance: a merge join whose one side is a hot cached predicate stops paying LMDB cursor walks for
that side (visible in store-probe counters); ordered-DISTINCT ADJACENT over a cached predicate likewise.


## Work item 3 — SIP masks sourced from CSR stages

When a probe stage is CSR-served, publish the mask directly from `CsrEntry.keysByDense` (already
ascending) instead of refusing at `hashBuildRefused` (`LmdbNativeChunkPipeline.java:1013-1017`) — zero
build cost, the sweep is skipped entirely, and root-scan pruning is recovered. Keep refusing the hash
MEMO for CSR stages (the cache genuinely answers repeat probes O(1)). Per-stage only; sibling stages
unaffected (`ScanStage.masks` already accumulates a list, `:638, :710-736`).

Acceptance: the SIP benchmark shape with a cacheable middle predicate shows root `MASK_SKIP` counts with
zero hash-build cost.


## Work item 4 — Admission and eviction policy

1. Entry cap proportional: `maxEntryBytes = max(64 MB, maxBytes()/16)` so large machines can cache hub
   predicates (the 2.4M-pair ceiling disappears on big heaps); keep the flat default on small heaps.
2. Eviction: replace the O(n) LRU scan with an intrusive clock (second-chance) list — eviction becomes
   O(1) amortized and scans no longer serialize on the global lock; credit `GLOBAL_USED_BYTES` on store
   close (fix the leak) and on entry drop paths (`:316, :134` already do; audit the close path).
3. Write invalidation: keep the conservative whole-cache disable on `storeTxnStarted` (per-predicate
   invalidation requires write-path predicate tracking — implement only the cheap half: on commit,
   re-enable immediately and drop ONLY entries whose predicate appeared in the transaction's write set if
   that set is already available from `TxnRecordCache`; otherwise drop all, as today). Never weaken
   snapshot correctness: entries revalidate against the latest committed revision (`:123, :350-352`),
   and the sketch-refresh reader exclusion (`LmdbSailStore.java:1547-1552`) stays.
4. Keep `minProbes`/amortization admission as-is (measured as reasonable), but export admission/refusal
   counters (`:309` pattern) through `LmdbNativeAttemptMetrics` so plan 13 can validate the policy under
   the benchmark workloads rather than by argument.

Acceptance: cache effectiveness (hit bytes / total probe bytes) on the benchmark corpus improves on
large-heap runs; no correctness regressions under the concurrent write/read test; close-leak test
(create/close stores in a loop, assert `GLOBAL_USED_BYTES` returns to baseline).


## Work item 5 — CSR under partitioned parallel scans (stretch)

Range-partitioned parallel root scans bypass the cache (`LmdbSailStore.java:3291` → `getTriplesRange`,
no consult; partitions are raw LMDB byte-key ranges). Provide a positional slice API: partition a cached
predicate by dense-key index ranges (`runStart` gives exact row counts per key, enabling equal-work
splits — better than byte-range guessing), each worker iterating its slice of `keysByDense`. This also
improves partition balance for skewed predicates. Marked stretch: implement after plan 04 items land,
only if scan-bound parallel shapes show CSR-miss cost in profiles.

Acceptance (if built): parallel scan of a cached hub predicate shows zero LMDB cursor reads and
near-equal per-worker row counts on a skewed dataset.
