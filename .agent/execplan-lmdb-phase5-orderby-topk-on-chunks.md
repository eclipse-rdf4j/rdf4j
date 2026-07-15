# Phase 5: ORDER BY / Top-K on chunks — sort flat rows, expand lazily

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`.
This is Phase 5 of the approved umbrella plan (user plan file
`users-havardottestad-documents-programm-binary-squid.md`); Phases 0–4 are complete (see
`.agent/execplan-lmdb-phase3-joins-aggregation-on-chunks.md` and
`.agent/execplan-lmdb-phase4-sort-order-exploitation.md`).

## Purpose / Big Picture

Today ORDER BY switches the native SELECT engine into its dumbest mode. `NativeRowsStep.evaluate`
(`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java`, ~line 224) routes any
query with order slots into `evaluateAll`, which opens the plain row-chain cursor — every solution row is fully
enumerated (a star with three unprojected legs of 10 matches each produces 1000 physical rows per subject) and then
packed and sorted. None of the factorized machinery (Phase 2/3/4) engages. After this phase, when every ORDER BY
key lives in the factorized plan's *flat* section (the prefix the chunk pipeline drives), the engine sorts only the
flat rows — each carrying its multiplicity and references to its enumeration branches' value runs — and expands
them lazily in sorted order. Under LIMIT it keeps only the best `offset+limit` flat rows in a bounded heap, which
is *exact* because a flat row's expansion never changes its keys and zero-match flat rows are already skipped by
the factorized cursor (every kept flat row expands to at least one result row). Observable: the explain metric
records `orderedFactorizedSort` / `orderedFactorizedTopK` instead of `orderedSpillSort` / `orderedTopK`, new
engagement counters tick, results equal the generic evaluator, and a star-shaped ORDER BY + LIMIT stops paying the
full cross-product enumeration.

The SPARQL order semantics stay exactly where they are: the existing `PackedRowComparator` built in `evaluateAll`
(value-comparator fallback plus the inline-id native fast path in `orderCompare`) is reused unchanged — no
memcmp/radix key tricks, which the umbrella explicitly rejects as unsound under SPARQL cross-type ordering.

## Progress

- [x] (2026-07-14) Survey: `evaluateAll` order path (top-K ≤100k via `NativeTopKBuffer`, else `NativeSpillSort`),
  `LmdbNativeFactorizedRows.Cursor` per-prefix-row branch processing (`TailResult{count, values}`; zero-count rows
  `continue prefixLoop` — the exactness guarantee for flat top-K), `NativeSortBuffer` packed layout (fixed width,
  stable merge sort, ordinal tiebreak), `LmdbNativeLongArena` + `FactorizedTail.MemoBudget` for bounded value
  storage.
- [x] (2026-07-14 19:50Z) Milestone A: `FlatRowCursor`/`openFlat` on `LmdbNativeFactorizedRows` (sibling of the
  odometer cursor: identical prefix-row processing — prefix-only tail filters, zero-count skip, COUNT
  multiplication — exposing multiplicity + enum value runs), plus `branchFreshMask()`, `enumBranchCount()`,
  `enumValueSlots()` accessors and a shared `openPrefix`/`closeOwnerPrefixDepths`.
- [x] (2026-07-14 19:50Z) Milestone B: `tryEvaluateOrderedFactorized` in `NativeRowsStep.evaluateAll` — packed rows
  `slots + multiplicity + (arenaOffset, count) per enum branch`, sorted with the unchanged `PackedRowComparator`;
  top-K via `NativeTopKBuffer.wouldAccept` (admission tested on the order-key columns before paying the arena
  copy); lazy expansion via `expandSortedFlatRows` (odometer over arena runs × multiplicity, emitCap-bounded);
  budget/arena refusal falls through to the classic paths; strategies `orderedFactorizedSort`/`orderedFactorizedTopK`
  + counters; flag `rdf4j.lmdb.orderedFactorizedRows.enabled`.
- [x] (2026-07-14 19:55Z) Milestone C: `LmdbNativeOrderedFactorizedTest` 6/6 (full-sort/top-K/COUNT-multiplicity
  engagement with tie-safe assertions — multiset equality + non-decreasing keys; branch-order-key, DISTINCT, and
  flag-off fallbacks pinned) + fuzz ordered round `randomOrderedBasicGraphPatterns` (?a anchored as subject so the
  order keys bind resources; multiset + monotone-key oracle). The round immediately caught that mixed date/dateTime
  ORDER BY diverges between engines — root cause is the core `ValueComparator` being non-transitive on
  indeterminate XML calendar comparisons (pre-existing, engine-independent, reproduces with the factorized path
  disabled); spawned as chip task_4b308cb7 and excluded from the oracle by construction.
- [x] (2026-07-14 20:15Z) Milestone D: phase exit — fuzz 11/11 with the flag on and off; full module verify 1515
  tests / 1 failure (the known pre-existing `LmdbEvaluationStatisticsMemoizationTest` one) / 0 errors; theme ITs
  green (plan-snapshot 2/2, smoke 10/10); formatter + copyright clean. Benchmark evidence deferred to the docker
  loop per the Plan of Work (engagement counters + the structural argument recorded instead — flat rows ≪ expanded
  rows on branch-heavy shapes).

## Surprises & Discoveries

(To be filled as work proceeds.)

## Decision Log

- Decision: engage the factorized order path only when the plan factorizes with ≥1 branch, the query is not
  DISTINCT, the entry is not correlated (mirrors `NativeRowsIteration.initialize`'s gate), and every order slot is
  outside every branch's fresh-slot mask (order keys ⊆ flat section). Everything else falls back to the existing
  enumerated sort — this phase adds a fast path, it never removes one. Rationale: DISTINCT-after-projection
  interacts with expansion dedup in ways the classic path already handles; a branch-slot order key would require
  sorting *within* expansions, which is exactly the enumeration this phase avoids. Date/Author: 2026-07-14 /
  Claude Code.
- Decision: branch value runs are materialized into a query-local `LmdbNativeLongArena` guarded by a fresh
  `FactorizedTail.MemoBudget`; budget/arena refusal aborts the factorized attempt and reruns the classic path from
  scratch (correct, rare, and cheap relative to the giant queries that trigger it). The alternative — re-evaluating
  branches per sorted row after the scan — avoids the arena but doubles store probes; recorded as a future option
  if budget refusals show up in practice. Top-K evictions leave dead arena entries (append-only arena); the budget
  treats them as spent, which is conservative and keeps the memory bound hard. Date/Author: 2026-07-14 / Claude.
- Decision: spill-to-disk is not implemented for the factorized sort (the arena references don't serialize into
  `NativeSpillSort`'s fixed-width runs). Unbounded-row ORDER BY without LIMIT falls back to the classic spill path
  when the budget refuses; with the default budget the in-memory factorized sort covers the same sizes the classic
  in-memory sort covered. Date/Author: 2026-07-14 / Claude Code.

## Outcomes & Retrospective

Phase 5 is complete (2026-07-14). ORDER BY stopped bypassing the factorized engine: when the order keys live in
the flat section, only flat prefix rows are sorted (with the unchanged SPARQL comparator and its inline-id fast
path) while multiplicities and enum value runs ride along in a budget-guarded arena and expand lazily in sorted
order; ORDER BY + LIMIT keeps a bounded heap of flat rows with pre-admission testing so rejected rows never pay
the arena copy — exact because expansion preserves order keys and empty-branch rows are skipped before admission.
Every refusal (DISTINCT, branch order keys, correlated entry, budget) falls through to the untouched classic
paths. The new ordered fuzz round immediately paid for itself by catching a pre-existing, engine-independent
divergence: the core ValueComparator is non-transitive on indeterminate date/dateTime comparisons, making mixed
temporal ORDER BY output sort-algorithm-dependent (chip task_4b308cb7). Lesson: differential ordered oracles must
restrict keys to types whose ordering is genuinely total, or they test the comparator's pathologies rather than
the engine.

## Surprises & Discoveries (addendum)

- The upstream cost optimizer can root a *larger* pattern (its explain cost said otherwise) and turn the "obvious"
  chain prefix into branches — the first engagement fixture ordered by ?s, which the chosen plan bound in a
  branch. Ordered-path engagement depends on the algebra-level join order, not just the query shape.

## Context and Orientation

All paths under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/` unless noted. Terms:

- **Row step / `NativeRowsStep`** (`LmdbNativeRowStep.java`): the compiled physical plan for a native SELECT.
  `evaluate(bindings)` returns a streaming `NativeRowsIteration` when there is no ORDER BY; with order slots it
  calls `evaluateAll(bindings)` (~line 294), which materializes, sorts, and slices. `emitCap = offset+limit` rows;
  a bounded top-K engages when `emitCap ≤ 100_000` (strategy label `orderedTopK`), otherwise `NativeSpillSort`
  (`orderedSpillSort`). The `PackedRowComparator` closure (~line 305) compares order slots with a native inline-id
  fast path (`orderCompare`) and the SPARQL `ValueComparator` fallback — reuse it verbatim.
- **Factorized rows / `LmdbNativeFactorizedRows`**: `tryCreate(multiJoin, derived, row, seedMask, sourceSlots,
  distinct)` splits the ordered plan into a *flat prefix* (driven by the Phase 2 chunk pipeline where possible —
  including Phase 4's merge walks and SIP masks, which come along for free here) and trailing *branches*
  (`TailBranch`, roles ENUM/COUNT/EXISTS). Its `Cursor.next()` per prefix row: evaluates every branch
  (`branch.result(row, owner)` → `TailResult{count, values}`), *skips the prefix row when any branch has zero
  matches* (inner-join semantics — this is what makes flat top-K exact), multiplies COUNT branches into
  `multiplicity`, stores ENUM results, then runs an odometer emitting one physical row per enum combination.
  `TailResult.values` buffers are reused across prefix rows and must be copied by any consumer that retains them.
- **Sort substrate** (`LmdbNativeSort.java`): `NativeSortBuffer(slotCount, expected)` packs fixed-width long rows
  with insertion ordinals (stable), `sortedOrder(comparator)` merge-sorts indexes; `NativeTopKBuffer(slotCount, k,
  comparator)` is a bounded max-heap over the same packed layout; `NativeSpillSort` spills fixed-width runs.
  The comparator interface is `PackedRowComparator.compare(long[] left, int leftOffset, long[] right, int
  rightOffset)` — row offsets are multiples of the buffer's `slotCount`, so a *wider* packed row (slots + extra
  columns) works with the same comparator as long as order slots keep their positions at the row base.
- **Arena + budget**: `LmdbNativeLongArena.append(long[], from, len)` returns an offset or `REFUSED`;
  `FactorizedTail.MemoBudget.tryReserve(entries, values)` bounds total retained values.
- **Correctness oracle**: `LmdbNativeDifferentialFuzzTest` (native vs `rdf4j.lmdb.nativeQueryEngine.enabled=false`).
  Its slice round asserts multiset containment because unordered LIMIT is nondeterministic; ordering by *all*
  projected variables makes exact list comparison sound (ties are between identical rows).

## Plan of Work

Milestone A — flat-row emission. In `LmdbNativeFactorizedRows`, add a package-private `FlatRowCursor openFlat(RowState
row)` (and a small `FlatRow` view): a sibling of `Cursor` that performs the identical prefix-row processing
(prefix-only tail filters, branch evaluation, zero-count skip, COUNT multiplication) but stops before the odometer:
one `next()` per surviving prefix row, exposing `multiplicity()`, `enumCount()`, and per enum branch `valueSlots(b)`
/ `values(b)` / `count(b)` (the reused `TailResult` buffers — documented as valid only until the next `next()`).
Reuse the existing engagement/description plumbing (`describeEngagement`, `prefixStrategy`, `mergeStages`). No
behavior change to the existing cursor; covered by Milestone B's tests plus the fuzz.

Milestone B — factorized ORDER BY. In `NativeRowsStep.evaluateAll`, after the comparator is built and before the
existing top-K/spill paths, attempt the factorized route behind `rdf4j.lmdb.orderedFactorizedRows.enabled`
(default true, read per query): the same `multiJoin`/`derivedFactorizedPlan` dance as
`NativeRowsIteration.initialize` (skip when the entry is correlated, when `distinct`, or when `tryCreate` returns
null or a plan with zero branches), then check every order slot against the union of branch fresh masks (expose the
branch fresh masks or a `branchFreshMask()` accessor from `LmdbNativeFactorizedRows`). Packed row layout: `width =
slotCount + 1 + 2 × enumBranches` — the full slot row first (so the comparator works unchanged), then multiplicity,
then per enum branch `(arenaOffset, count)`. Scan via `openFlat`: copy each flat row's enum values into the arena
(budget-reserved; `REFUSED`/reserve-failure → close everything and fall through to the classic paths), append the
packed row to a `NativeSortBuffer` — or, when `emitCap ≤ 100_000`, offer it to a `NativeTopKBuffer(width,
emitCap, comparator)` (admission is exact per the zero-count skip). After the scan, walk the sorted packed rows:
restore the slot section into a scratch row, odometer over the arena runs (values written into their branch value
slots), repeat each combination `multiplicity` times, project through the same emission helper the classic path
uses, stop at `emitCap`. Record strategies `orderedFactorizedTopK` / `orderedFactorizedSort` and bump new static
counters (`NativeRowsStep.ORDERED_FACTORIZED_SORTS`, `ORDERED_FACTORIZED_TOPK`) for engagement tests.

Milestone C — ordered differential coverage. New focused test class `LmdbNativeOrderedFactorizedTest`
(`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/`): star and chain shapes with unprojected legs under
ORDER BY (full sort) and ORDER BY + LIMIT/OFFSET (top-K), each asserting exact ordered equality against the generic
evaluator (order keys total-order the results) plus engagement counter deltas; a branch-slot order key and a
DISTINCT query asserting the gate *refuses* (classic strategies recorded, results equal). Extend
`LmdbNativeDifferentialFuzzTest` with an ordered round: random shapes ordered by all projected variables, exact
list equality (sound because the order is total up to identical rows).

Milestone D — phase exit. Fuzz green with the flag on and off; full `core/sail/lmdb` module verify (expect the one
known pre-existing failure); theme ITs (`LmdbImprovedQueryPlanSnapshotIT`, `ThemeQueryBenchmarkSmokeIT`); formatter
+ copyright; ExecPlan retrospective + memory update. Benchmark evidence: a quick JMH comparison on a star ORDER BY
+ LIMIT shape (flag on vs off) via `scripts/run-single-benchmark.sh` if a suitable benchmark exists or is cheap to
add; otherwise the engagement tests plus the structural argument (flat rows ≪ expanded rows) are recorded and a
benchmark is left to the docker loop (note in Decision Log).

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j` (`set -o pipefail` with piped mvnf; one mvnf at a time;
protect `core/sail/lmdb/target/lmdb-theme-query-benchmark` from any root `clean`):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeOrderedFactorizedTest    # Milestones B+C red→green
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest     # oracle after every milestone
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb                     # phase exit

## Validation and Acceptance

Acceptance as behavior: a star query with two unprojected legs under `ORDER BY ?s LIMIT 10` returns exactly the
generic evaluator's rows in the same order while the explain metric shows `orderedFactorizedTopK` and the
engagement counter ticks; the same query without LIMIT shows `orderedFactorizedSort`; a query ordering by a
branch-enumerated variable still runs the classic `orderedTopK`/`orderedSpillSort` and stays correct. Fuzz
(including the new ordered round) green with the flag on and off; module verify green modulo the known pre-existing
failure; theme ITs green.

## Idempotence and Recovery

The whole path sits behind `rdf4j.lmdb.orderedFactorizedRows.enabled`; every refusal falls through to the untouched
classic paths, and budget refusal mid-scan closes the factorized cursor and reruns classically. If the exactness
argument for flat top-K is ever violated in fuzz, disable the top-K arm (one condition) and keep the full-sort arm.

## Interfaces and Dependencies

Modified: `LmdbNativeFactorizedRows.java` (FlatRowCursor + branchFreshMask accessor), `LmdbNativeRowStep.java`
(factorized order route in `evaluateAll`, counters), `LmdbNativeDifferentialFuzzTest.java` (ordered round). New:
`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeOrderedFactorizedTest.java`. Reuses:
`NativeSortBuffer`/`NativeTopKBuffer`, `PackedRowComparator` + `orderCompare` fast path, `LmdbNativeLongArena`,
`FactorizedTail.MemoBudget`, the chunk pipeline (and with it Phase 4's merge walks and SIP masks).
