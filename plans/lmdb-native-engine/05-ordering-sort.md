# 05 — Ordering and sort

Goal: ORDER BY stops being the least-optimized path in the engine. The ordered fork drives its input
through the same strategies as the unordered path, sorts only live columns via a key/payload split, uses
radix passes where keys are homogeneous ordered integers, handles expression order keys natively, and the
engine exploits index order it already possesses instead of sorting.


## Current state

`orderSlots.length != 0` forks into `evaluateAll` (`LmdbNativeRowStep.java:186-189`), which never calls
`openBatch`, never consults parallel or adaptive dispatch; its three branches are ordered-factorized
(`:293-296`, correctly gated — the only strategy that survives ordering), bounded top-K
(`:300-327`, heap, `emitCap <= 100_000`), and full sort (`:329-349`) whose input is a plain nested-loop
`arg.open(row)` (`:309, :335`). Sorting moves FULL-width rows: slots allocated for every named variable
with no liveness test (`slot(name)`, `LmdbNativeAggregatePlannerBase.java:596-603`;
`layout.freeze` at `LmdbNativeAggregatePlanner.java:249`); the live sets ARE computed and discarded
(`sourceSlots:225-228`, `orderSlots:229-233`); buffers take `row.slots.length`
(`LmdbNativeRowStep.java:305-306, :332-333, :337, :341`); arena and spill budget divide by full width
(`LmdbNativeSort.java:73, :690-692`). `compareRows` strides the full row (`:183-184`); spill rows are
`(slotCount+1)*8` bytes (`:691`). No radix path: `NativeSortBuffer.sort` is serial bottom-up merge sort
(`:162-177`) while a real LSD radix sorter with pass-skipping ships unused by the query side
(`LeadingFieldSorters.java:23-94`, package-private in the parent package, zero references from
`evaluation/`). Expression order keys abandon the fused native sort: `supportedOrder` requires bare Vars
(`LmdbNativeAggregatePlanner.java:305-311`), consulted at `:138-140, :166-168, :178-180` — the join tree
still compiles natively (base strategy re-enters children), but sorting falls to the generic
`OrderIterator` over materialized BindingSets. Index order is exploited only via the
`urn:rdf4j:stable:index-order` sentinel (`LmdbOrderByOptimizer.java:83-93`) and for DISTINCT/REDUCED
(`LmdbNativeEvaluationStrategy.prepare`, `:96-112`); plain `ORDER BY ?x` never maps to an
order-delivering scan; merge-join eligibility is discovered per-open at runtime
(`LmdbNativeMergeJoin.tryOpen:52-90`) after order-blind choices were already made (the standard pipeline
deliberately blinds core RDF4J's order planning: `OrderBlindTripleSource.getSupportedOrders → Set.of()`,
`LmdbStandardQueryOptimizerPipeline.java:44, :84`).


## Work item 1 — The ordered path drives strategies

`orderedTopK` and `orderedFullSort` feed from `arg.open(row)`. A full sort makes producer encounter order
irrelevant, so the input may come from ANY strategy:

1. Extract a shared `RowCursor openUnorderedInput(row)` used by both branches that runs the same
   proposal dispatch as `NativeRowsIteration.initialize()` rungs C/D/E (skipping A/B — prefix-run and
   ordered-distinct are order-producing strategies meaningless below a sort, and by construction absent
   here: `prefixRunPlan` is only built when `orderSlots.length == 0`,
   `LmdbNativeAggregatePlanner.java:242`).
2. Under top-K the input additionally benefits from bounded consumption — keep the heap consumer-side;
   early close already propagates through batch and parallel cursors (plan 01 §4a evidence).
3. Ordered-factorized keeps its priority and its correctness gates (`requiredPrefixMask` stability,
   branch-bound order-key refusal, `LmdbNativeRowStep.java:412-418`) — it is tried first, exactly as
   today (`:293-296`).

The only semantic delta: tie order among equal sort keys becomes nondeterministic when the input ran
parallel (SPARQL leaves it unspecified; the ordinal tiebreak `LmdbNativeSort.java:184-186` still makes
each single run deterministic).

Tests: result-set equivalence (as multisets, and as sequences modulo equal-key ties) vs the current
path across the ORDER BY corpus; dispatch-contract assertions that a large ordered BGP shows
`parallel`+`orderedFullSort` tags together.
Acceptance: `SELECT ... WHERE { large BGP } ORDER BY ?x` — the canonical report query — scales with
cores for the producer phase (was: pinned to one core end-to-end).


## Work item 2 — Sort only live columns

Map plan slots onto a dense live subset before packing: `live = sourceSlots ∪ orderSlots`
(∪ `enumValueSlots` on the ordered-factorized path — `expandSortedFlatRows` writes branch values back
into the packed row, `LmdbNativeRowStep.java:552-556, :487`). Build `int[] liveToPlan` remap once at
plan time; `NativeSortBuffer`/`NativeSpillSort`/`NativeTopKBuffer` operate on `live.length`-wide rows;
`project()` (which reads only `sourceSlots`, `:638-649`) and the distinct tracker (keyed on
`sourceSlots`, `:307, :589`) get remapped indices. Safety is already proven: non-live slots are dead
after the sort (the one full-row consumer, bare-fragment `RowBindingSetView`, is always constructed with
`orderSlots = new int[0]` and never enters the sort path, `:138-139, :186-189, :654-655`).

Acceptance: a 12-slot plan projecting 3 and ordering by 1 moves 4 longs/row instead of 12 through sort
and spill — 3× more rows fit before the 64 MB budget spills; benchmark shows the corresponding win.


## Work item 3 — Key/payload split

After item 2, split further: the comparison loop touches only `orderSlots`; payload lanes move once.
Layout: keys packed contiguous (`live` reordered so order keys are a prefix), `compareRows` strides
`orderSlots.length`, payload attached by row index within the buffer (no back-pointer indirection needed
in-memory since rows are fixed-width; spill runs write keys-then-payload per row so the merge reads keys
without deserializing payload). Bounded win (≤64 slots total by construction — slot indices feed 64-bit
masks) but it compounds with item 2 on the comparison-heavy merge path.

Acceptance: sort CPU profile shows comparison loads confined to the key prefix; measurable on wide-row
ORDER BY shapes.


## Work item 4 — Radix path for homogeneous ordered-integer keys

Wire `LeadingFieldSorters`' LSD radix (pass-skipping via `differingBits:33-47`, `isSorted` early-out
`:25-27`) into the UNBOUNDED sort path only (`NativeSpillSort` in-memory run sorting — the top-K heap is
O(n log k) and a radix full sort would regress it):

1. Widen visibility (package-private `:16` in the parent package → shared internal utility).
2. Precondition pass: O(n) scan proving every key satisfies `ValueIds.isOrderedInteger` and is bound —
   `orderCompare` returns null for UNKNOWN/unordered (`LmdbNativeRowStep.java:611-621`), so mixed columns
   fall back to comparator sort. Cost of the precheck is one pass, negligible against ~8 counting passes
   saved versus ~20 comparison passes with potential decodes.
3. Sign handling: the fast paths compare signed while buckets are unsigned bytes
   (`LeadingFieldSorters.java:103-139, :175, :186`) — correct only for non-negative keys, which holds for
   `ValueIds.getValue` on ordered IDs (`ValueIds.java:59-74, :112-114`); assert the precondition.
4. DESC: reverse pass after the stable sort (stability makes it exact); multi-key: radix the leading key,
   comparator-resolve tie ranges (the tie-narrowing structure of Kuzu's `findTies` — ranges only, never
   re-sorting resolved spans).
5. Add cancellation checks per pass (the sort loops must observe close, matching the engine's existing
   iterator discipline).

Acceptance: `ORDER BY ?o` over 10M numeric literals (ordered-v1 store) ≥2.5× vs comparator sort; mixed
or string keys show zero regression (fallback proven by dispatch tag).


## Work item 5 — Expression order keys and order-aware physical planning

(a) Computed order-key slots: allocate a slot per order expression, populate via the existing computed
`CopyBinding` machinery (`CopyBinding.computed`, `LmdbNativeRowState.java:344-374`; `ExtensionPlan`,
`LmdbNativeRowPlans.java:200-224`; `compileInlineId` covers MathExpr/Str/Lang/Datatype/STRLEN/casing,
`LmdbNativeExpressionCompiler.java:94-117, :255-263`), relax `supportedOrder` to accept any expression
`compileInlineId` proves inline-safe (`guaranteedInline:112-114` — the existing safe-decline discipline),
and match SPARQL error semantics for erroring order expressions against the generic `OrderIterator`
(errors order lowest; encode as a sentinel below all ordered-integer codes, test against the generic
path). `ORDER BY DESC(STR(?o))`, `ORDER BY ?a + ?b`, `ORDER BY LCASE(?label)` then keep the fused native
sort.

(b) Order-aware physical choice — runtime-side only (sketch DP untouched per 00-overview): extend
`LmdbStableOrderPlanner` (which already resolves order-delivering sides and emits swap directives,
`LmdbStableOrderPlanner.java:129-140, :301-306`) to recognize plain `ORDER BY ?x` when the derived plan's
leading scan can deliver `?x`-order from an index (`TripleIndex.supportsOrder:216`,
`getBestCompatibleOrderedIndex:554`, `StatementOrder` already a `PatternPlan` field,
`LmdbNativePatternPlan.java:35`), and mark the sort ELIMINABLE: `evaluateAll` then streams with a
verification comparator in test builds (assert nondecreasing) and no sort in production. Scope guard:
only single-key ASC/DESC over a pattern-produced slot with no intervening order-destroying operator
(joins ordered on the same leading slot preserve it — exactly the property merge join already requires;
reuse its `orderedSide` re-planning machinery, `LmdbNativeMergeJoin.java:126-127`). Everything else keeps
the sort. This recovers the highest-value slice of "interesting orders" without touching the DP.

Acceptance: `SELECT ?s ?o WHERE { ?s :p ?o . ?s :q ?v } ORDER BY ?s` runs with zero sort (dispatch tag
`orderedScan`), verified ordered; shapes outside the scope guard show unchanged plans in the snapshot
corpus.
