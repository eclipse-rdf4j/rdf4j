# 02 — One filter path

Goal: "filter" becomes a property of any batch or row stream — one vectorized kernel serving both
execution engines, decode-free comparison wherever IDs suffice, and adaptive placement available on every
chain instead of owning one dispatch rung.


## Current state

Two vectorized engines implement the same filter loop independently: `FilterStage.fill`
(`LmdbNativeChunkPipeline.java:1606-1629`) duplicates `LmdbNativeSpecialization.interpretFilter`
(`LmdbNativeSpecialization.java:108-120`) — same `NativeBatch`, same
`copyToRow`/`recomputeBoundMask`/`accept`/selection-compaction — and never calls the specializer, so the
JIT-generated kernel's sole production call site is `LmdbNativeBatch.java:356` (rung C only). The
generated kernel is keyed on `slotCount` alone (`LmdbNativeSpecialization.java:313`) though the filter's
true read set `requiredMask` is computed and has zero consumers
(`LmdbNativeExpressionRuntime.java:80-82`).

Filter absorption disqualifies batch joins wholesale: `SlotPlan.filter`
(`LmdbNativeSlotPlan.java:206-214`) routes any mask-computable filter into `MultiJoinPlan.filters`, and
both batch joins refuse on `plan.filters.length != 0` (`LmdbNativeHashJoin.java:43`,
`LmdbNativeMergeJoin.java:53`) — inverted capability: a volatile/EXISTS filter (mask −1,
`LmdbNativeAggregateFilterCompiler.java:458-471`) becomes a `FilterPlan` wrapper and the batch join
SURVIVES; a pure placeable filter is absorbed and KILLS it.

Decode-free comparison is shadowed twice: `compileBoolean` tries the expression compiler first and
returns on success (`LmdbNativeAggregateFilterCompiler.java:362-366`), so the `Compare` branch at
`:404-409` — sole producer of `CachedCompareFilter` — is unreachable for anything the expression compiler
handles; and `cachedCompareIfPossible` (`:574-584`) returns null for slot-vs-slot even though the
symmetric ordered-integer kernel ships at `LmdbNativeRowStep.orderCompare:616-619`. Constants are
re-decoded per row: `compileValue` (`LmdbNativeScalarExpressionCompiler.java:50-52, :59-62`) does not
fold, while `compileString` already does (`:111-114`); the decode allocates
`new BigDecimal(new BigInteger(label))` per row (`LmdbNativeValueCodec.java:543-556`).

Adaptive filter placement is rung F with one call site (`LmdbNativeRowStep.java:1277`), structurally
unreachable whenever batch/parallel/factorized admit; rung E pins every filter at earliest legal depth
(`derive(mask, true)` → `LmdbNativeJoinPlans.java:236-238`) — the worst placement for exactly the filter
adaptive would select. Placement envelopes are computed on every `derivedFactorizedPlan` call
(`:244-262`) and discarded; workers fork filter copies including adaptive metadata
(`LmdbNativeParallelPipelines.java:274-275`) and never use them. `AdaptiveFilterSession` opens depth 0
itself (`LmdbNativeAdaptiveFilterPlacement.java:186`); `NativeFilterLease` is an unsynchronized
`IdentityHashMap` with mutable `used` flags (`NativeFilterLease.java:24-27`).

The factorization sink refuses to move any pattern whose fresh variable a filter reads
(`LmdbNativeJoinPlans.java:376`, `filterReadMask` union with unknown-widening at `:362-365`, plus a
`Double.isFinite(staticEstimate)` veto at `:370-373`), justified by a stale comment (`:343-344, :106-111`)
whose constraint commit `19ebab53d2` removed.


## Work item 1 — One vectorized filter kernel

Steps:

1. Change `FilterStage.fill` to delegate to `LmdbNativeSpecialization.applyFilter`. The two loops are
   semantically identical (verified line-by-line in the composition analysis); the only integration work
   is passing the stage's `NativeBatch` and selection state through the specializer's entry, which already
   takes exactly those (`LmdbNativeBatch.java:356` call shape).
2. Key `KernelKey` on `(slotCount, readMask)` using `requiredMask`
   (`LmdbNativeExpressionRuntime.java:80-82`); the kernel then copies only masked slots into `RowState`.
   Slots outside the mask must still be written as UNKNOWN OR the kernel must be gated to filters whose
   `NativeBooleanFilter` provably reads only masked slots — choose the gate: it is already computable
   (`placeableFilterMask` machinery) and avoids a subtle habit of half-initialized rows.
3. Review the specialization admission constants with the new, much larger call-site population:
   the 32_768-row observation threshold and 32-entry global kernel cache
   (`LmdbNativeSpecialization.java:58-60`). Make the cache per-store rather than JVM-global, sized 128,
   and expose hit/miss counters through `LmdbNativeAttemptMetrics`.

Add the missing vectorized entry point on the filter interface itself:
`default int selectBatch(NativeBatch batch, int[] sel, int n, RowState scratch)` on
`NativeBooleanFilter` (`LmdbNativeFilters.java`) with the default implementing today's per-row loop, and
specialized overrides for the ID-only filters (`CachedCompareFilter`, membership/IN filters,
`sameTerm`) that read column arrays directly and never touch `RowState`. The specializer consumes
`selectBatch` when present, the interpreter loop otherwise.

Tests: filter-equivalence property test — for a corpus of compiled filters and random batches, the
kernel path, the `selectBatch` path and the row-at-a-time path produce identical selections.
Acceptance: chunk-pipeline filter throughput on the theme benchmark's filtered shapes improves
measurably (expected 1.2–1.8× on the filter stage; the win is bounded because `accept` was already
non-allocating), and the JIT kernel's execution count (new counter) becomes nonzero on factorized plans.


## Work item 2 — Filters stop disqualifying batch joins

Steps:

1. Delete `|| plan.filters.length != 0` from `LmdbNativeHashJoin.tryOpen:43` and
   `LmdbNativeMergeJoin.tryOpen:53`.
2. Wrap the returned `BatchCursor` in the existing `FilterBatchCursor` (`LmdbNativeBatch.java:334`,
   constructed today only at `LmdbNativeRowPlans.java:101-103`) applying `plan.filters`. For a 2-child
   plan, `derive` already places every absorbed filter at depth 1
   (`filterDepth[f] = max(earliest, min(planned, last))`, `LmdbNativeJoinPlans.java:236-238`, and
   `last == planned == 1`), so the wrapped composition reproduces the current nested-loop placement
   bit-for-bit — not merely equivalently.
3. Trap: the hash join's build-overflow fallback opens `plan.open(row)`
   (`LmdbNativeHashJoin.java:125`), which applies the plan's filters itself via
   `openChain`/`applyFilters`. Wrap only the non-fallback path (or open the fallback against a
   filter-stripped plan clone); otherwise filters double-apply — idempotent but wasteful.
4. All other gates stay untouched; they are verified correctness requirements: `statementOrder`
   obligations (`LmdbNativeMergeJoin.java:67-70`), `hasRepeatedSlot` (`:64-66`), single-bit `keyMask`
   (`:71-76`; lifted separately in plan 09 §3 after the `getIndexName` fix), context-key conditions
   (`:78-86`; `LmdbNativeHashJoin.safeContextKeys:68-80`), `hasRuntimeBoundSlot` (`:50-55`).

Tests: dispatch-contract — `?s :p ?o . ?s :q ?v . FILTER(?v > 10)` (the most common non-trivial BGP)
must now take `mergeJoin`/`hashJoin` tags; result-equivalence against the generic evaluator across the
filter corpus; the fallback path exercised by forcing build overflow with
`rdf4j.lmdb.nativeHashJoin.maxBuildRows=64`.
Acceptance: that shape improves ≥2× at 1M-triple predicates versus the current nested-loop chain.


## Work item 3 — Decode-free comparison everywhere it is sound

Steps:

1. Fold constants in `compileValue` (`LmdbNativeScalarExpressionCompiler.java:50-52, :59-62`) to a
   captured `DecodedValue` at compile time, mirroring `compileString:111-114`.
2. Extend `cachedCompareIfPossible` (`LmdbNativeAggregateFilterCompiler.java:574-584`) with a
   slot-vs-slot arm: runtime-guard `ValueIds.isOrderedInteger(a) && isOrderedInteger(b)` (exactly as
   `LmdbNativeFilters.java:476` guards the constant side) and fall through to the materializing fallback
   otherwise. Reuse the comparison logic of `orderCompare` (`LmdbNativeRowStep.java:616-619`) — extract
   it to a shared static on `ValueIds` so both call sites use one implementation.
3. Fix the shadowing: in `compileBoolean`, try `compileCompare` for `Compare` nodes BEFORE the generic
   expression compiler (reorder `:404-409` ahead of `:362-366`), or teach the expression compiler to
   emit `CachedCompareFilter` itself. Reordering is the smaller change; verify with a test asserting the
   compiled filter class for `FILTER(?a < ?b)` and `FILTER(?a < 10)`.
4. Propagate boundness guarantees: `assuredMask` exists with one consumer; thread it into compiled
   predicates so slots the plan proves always-bound skip their per-row UNKNOWN checks. Mechanical:
   compile-time specialization choosing between checked/unchecked accessors.
5. Widen native compilation coverage: `If`/`Coalesce` (compile as branch on the compiled condition with
   error-as-false semantics ONLY where `cannotProduceError` holds, matching the existing `Not` guard
   discipline at `:377-385`), and the remaining high-frequency builtins (`STRSTARTS`, `STRENDS`,
   `CONTAINS` over inlined strings; `ABS`, `CEIL`, `FLOOR`, `ROUND` over ordered integers). Every one
   declines to the generic path when its argument cannot be proven inline-safe
   (`guaranteedInline`, `LmdbNativeExpressionCompiler.java:112-114` pattern).

Correctness: `=` is value equality, not term identity — the EQ/NE raw-id shortcut must keep its
`safeResourceId` guard (`LmdbNativeAggregateFilterCompiler.java:559-561`); three-valued logic discipline
per the existing `Not` comment (`:377-385`) applies to every new compiled expression.
Tests: expression-equivalence corpus vs the generic evaluator including NaN, mixed numeric types,
unbound operands, and error-producing subexpressions.
Acceptance: `FILTER(?a < ?b)` and constant-compare filters show zero per-row allocation in an
allocation-profiled benchmark run; filter-heavy theme shapes improve accordingly.


## Work item 4 — Adaptive placement becomes a decorator

Depends on: the in-flight filter-authority work (optimizer boundary initial, movement only after a
complete bounded observation window) landing first; this item changes WHERE adaptive can exist, never
WHEN it may move.

Steps:

1. Add an `openChainFrom`-shaped entry: `AdaptiveFilterSession.openFrom(RowCursor depth0, RowState row)`
   consuming an externally supplied depth-0 cursor, leaving today's self-opening entry
   (`LmdbNativeAdaptiveFilterPlacement.java:186`) as the trivial wrapper. Template:
   `MultiJoinPlan.openChainFrom` (`LmdbNativeJoinPlans.java:170-181`).
2. Make the lease per-attempt and per-thread: `NativeFilterLease`'s `used` flags are unsynchronized
   (`NativeFilterLease.java:24-27`); construct one lease per worker/attempt, never shared. Workers
   already fork filter copies with adaptive metadata (`LmdbNativeParallelPipelines.java:274-275`) — use
   those forks.
3. Host a session per parallel worker (inside `runWorker`,
   `LmdbNativeParallelPipelines.java:590-654`) over the worker's morsel-fed chain, and one on the
   factorized suffix chain (`openSuffix`, `LmdbNativeJoinPlans.java:150-164`).
4. Stop pinning: rung E consumes the placement envelopes it already receives
   (`ordered.placement`, computed at `LmdbNativeJoinPlans.java:244-262`) instead of forcing
   `filterDepth[f] = earliest` — initial placement is the optimizer boundary (per the in-flight work),
   movement is the decorator's job.
5. Rung F remains as the fallback for plain nested-loop chains; with 1–4 done, the D∩F and E∩F ladder
   losses dissolve without arbitration.

Correctness: observation-window and rollback semantics are inherited verbatim from the in-flight work;
each worker's window is its own (no cross-thread aggregation of observations in v1 — simpler and sound,
revisit only if per-worker sample sizes prove too small, which telemetry will show).
Tests: extend `LmdbNativeAdaptiveFilterPlacementTest` with a worker-hosted session case asserting the
same movement decision the sequential session makes on the same data; race-detector run (JCStress-style
stress via the existing concurrent test patterns) on the per-worker lease.
Acceptance: a parallel-admitted query with an expensive misplaced filter shows filter movement in
telemetry and improves toward the sequential-adaptive baseline.


## Work item 5 — Narrow the factorization sink's filter veto

Steps: replace `(exclusive & filterReadMask) == 0L` (`LmdbNativeJoinPlans.java:376`) with a veto only on
filters whose mask intersects more than one prospective branch's fresh slots — the same rule the split
machinery already re-derives downstream (`LmdbNativeFactorizedRows.java:322`,
`LmdbNativeFactorizedTail.java:306-330`); keep a guard for the one surviving narrow case
(`LmdbNativeFactorizedTail.java:262-263`: single group slot, fresh from last pattern, all-COUNT
aggregates — a re-placed filter there causes refusal, worse than not sinking); drop the
`Double.isFinite(staticEstimate)` precondition (`:370-373`) — with +∞ the floor comparison at `:395` is
false so the pattern sinks, and the no-floor fallback (`:399-407`) still selects a valid keep; fix the
stale comments (`:343-344, :106-111`); update
`LmdbNativeFactorizedSinkTest#filterReadLegStaysPutSoTheTailGateSurvives` (`:83-98`) to pin the new rule.

Acceptance: `?s :name ?n . ?s :price ?p . FILTER(?p < 100)` factorizes `:price` into a filtered counting
branch (dispatch-contract assertion) and the shape improves on the theme benchmark.
