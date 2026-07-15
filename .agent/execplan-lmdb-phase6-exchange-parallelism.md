# Phase 6: one Exchange parallelism substrate — shared morsel machinery, all-sinks aggregation merges

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`.
This is Phase 6 of the approved umbrella plan (user plan file
`users-havardottestad-documents-programm-binary-squid.md`); Phases 0–5 are complete (latest:
`.agent/execplan-lmdb-phase5-orderby-topk-on-chunks.md`).

## Purpose / Big Picture

The native engine has two intra-query parallel frameworks that duplicate their scan-distribution machinery and
split their capabilities. `LmdbNativeParallelPipelines`
(`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeParallelPipelines.java`)
parallelizes plain SELECT row production; `LmdbNativeParallelAggregation` (same package) parallelizes GROUP BY —
but only when *every* aggregate is a plain COUNT, because its worker merge (`AggState.mergeCountsFrom`) can only
add counts and union distinct-id sets. Both split the root scan into "morsels" (batches of raw quads) pushed
through a bounded queue from one producer thread to worker threads — and each carries its own copy of the `Morsel`
class, the worker-side `MorselCursor`, the queue `offer` loop, and the producer loop. After this phase: one
`LmdbNativeExchange` class owns the morsel machinery (both frameworks become consumers of it, the duplicates are
deleted), and aggregation parallelism extends from COUNT-only to every mergeable aggregate — SUM, AVG, MIN, MAX,
and COUNT(DISTINCT) all run on worker threads and merge exactly (value-typed DISTINCT aggregates stay sequential:
per-worker distinct-then-accumulate cannot merge without double counting). Observable: a `SUM(?price) GROUP BY
?vendor` over a large scan records the `parallelAggregation` strategy and matches the sequential result; the
`PARALLEL_RUNS` counter ticks for value-typed aggregates for the first time.

## Progress

- [x] (2026-07-14) Survey: the two frameworks' duplicated members (Morsel/MorselCursor/offer/produce in
  `LmdbNativeParallelAggregation` vs the same machinery plus pool/reservation statics in
  `LmdbNativeParallelPipelines`), the COUNT-only merge (`AggState.mergeCountsFrom`, `allCounts` gates in
  `tryEvaluate`), Phase 3's `NativeGroupTable.mergeFrom` (already the single merge entry point).
- [x] (2026-07-14 20:45Z) Milestone A: `AggState.mergeFrom` handles every kind (counts add overflow-checked,
  distinct channel sets union, SUM/AVG running sums via `MathUtil PLUS` + avgCounts add, MIN/MAX comparator winner
  with extremeIds, typeErrors OR — a poisoned partial poisons the merge); `mergeCountsFrom` replaced;
  `parallelMergeable` gate (refuses only `distinct && kind != COUNT`) replaces `allCounts` in `tryEvaluate`.
  Red→green: `valueAggregatesRunParallel` + `valueAggregatesWithoutGroupingRunParallel` (red: "Expecting actual: 4L
  to be greater than: 4L" — the COUNT-only gate refused; green after) + `distinctValueAggregateStaysSequential`
  pins the remaining refusal. Fixture gained a p3 numeric branch with exact-arithmetic values (integers/decimals
  only) so merge order cannot perturb SUM/AVG. Suite 17/17.
- [x] (2026-07-14 21:00Z) Milestone B: new `LmdbNativeExchange` (Morsel, MorselCursor with optional cancellation,
  offer/throwIfAborted, `produceMorsels` — the superset producer: scans an already-seeded row or just poisons for
  an unseedable base, short-morsel trim, failure+cancel aware). Both frameworks rebased; deleted: the aggregation's
  private `Morsel`/`MorselCursor`/`offer`/producer loop and the pipelines' copies. Pool/reservation/flag statics
  stay on `LmdbNativeParallelPipelines` (control plane) — the Exchange owns the data plane; range partitioning
  lands there later (Decision Log). Routine B evidence: pre-green = Milestone A's 17/17 + the Phase 5 module verify
  (1515/1 known); post-green 106/0 across ParallelPipelines/ParallelAggregation/fuzz/ChunkPipeline/
  FactorizedTailAggregation/CountStar/NonCount; Hit Proof: both suites assert PARALLEL_RUNS/PARALLEL_ROW_RUNS
  across the rebased paths.
- [x] (2026-07-14 21:20Z) Milestone C: phase exit — fuzz 11/11 with parallel on and off; full module verify 1518
  tests / 1 failure (the known pre-existing `LmdbEvaluationStatisticsMemoizationTest` one) / 0 errors; theme ITs
  green (plan-snapshot 2/2, smoke 10/10); formatter + copyright clean; memory updated.

## Surprises & Discoveries

(To be filled as work proceeds.)

## Decision Log

- Decision: the umbrella's "deletes both existing parallel frameworks" is delivered as *substrate unification*
  (one Exchange class owns scan distribution; the two frameworks shrink to sink drivers over it) rather than a
  wholesale rewrite: the frameworks' sink logic (row batching with output backpressure vs group tables with
  merges) is genuinely different, already thin, and battle-tested by dedicated suites. Full sink unification rides
  the Phase 7 cost/operator work if still wanted. Date/Author: 2026-07-14 / Claude Code.
- Decision: range-partitioned root scans with split-key sampling (the umbrella's producer-less design) are
  deferred: the shared-queue producer already provides dynamic load balancing ("stealing"), the change needs new
  TripleStore split-key support, and validating a ≥0.7×cores target requires the docker benchmark loop rather
  than this laptop. The Exchange class is the seam where range partitioning lands later. Date/Author: 2026-07-14 /
  Claude Code.
- Decision: value-typed DISTINCT aggregates (e.g. SUM(DISTINCT ?x)) remain sequential: each worker would
  distinct-then-accumulate locally, and merging the accumulations double-counts ids seen by several workers.
  COUNT(DISTINCT) merges exactly (distinct-id set union), so it stays parallel. Date/Author: 2026-07-14 / Claude.

## Outcomes & Retrospective

Phase 6 is complete (2026-07-14). Aggregation parallelism extends from COUNT-only to every mergeable aggregate:
`AggState.mergeFrom` merges sums, average counts, extremes, type-error poisoning, plain counts, and distinct-id
sets exactly, and the gate refuses only value-typed DISTINCT aggregates (a genuine semantic limit, documented at
the gate). The morsel machinery exists once (`LmdbNativeExchange`); both parallel frameworks are sink drivers over
it, and their duplicated Morsel/cursor/producer code is gone. Deliberate deviations from the umbrella's letter,
recorded in the Decision Log: the frameworks' sink classes remain (substrate unified, sinks thin and separately
tested) and range-partitioned producer-less scans are deferred to the Exchange seam — both because the correctness
risk of a wholesale rewrite outweighed the structural gain inside this phase, and scaling targets need the docker
benchmark loop to validate. With this, the user's hard gate (Phases 3–6) is fully met: every phase closed with
differential fuzz across flag combinations, a full module verify (only the one documented pre-existing failure),
and green theme ITs.

## Context and Orientation

All paths under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`.

- **Morsel model**: the query thread opens N+1 same-snapshot LMDB sources (`openParallelSources`), submits N
  workers to a shared pool, then runs a producer loop that fills `long[4 × 1024]` quad batches ("morsels") from the
  root pattern's raw scan and offers them to an `ArrayBlockingQueue` (a poison pill per worker ends the stream). A
  worker-side `MorselCursor` binds quads from the queue into its private `RowState` and drives the rest of the
  plan chain. Failure propagation via an `AtomicReference<Throwable>`; pool-wide task accounting via
  `TaskReservation`.
- **`LmdbNativeParallelAggregation`**: gates in `tryEvaluate` (filters-free all-pattern MultiJoin, `allCounts`,
  ≥2 threads, root estimate ≥ `rdf4j.lmdb.parallel.minRootEstimate`); workers build a Phase 3 `NativeGroupTable`
  (optionally through a per-worker `FactorizedTail`, which since Phase 3 handles value-typed aggregates) and
  `mergeResults` folds tables with `NativeGroupTable.mergeFrom` → `AggState.mergeCountsFrom` (COUNT-only: adds
  `counts`, unions distinct channel sets — no sums/avgCounts/extremes/typeErrors handling).
- **`LmdbNativeParallelPipelines`**: SELECT-row parallelism plus the shared statics both frameworks use
  (`pool()`, `configuredThreads()`, `tryReserveTasks`, `closeSources`, `MORSEL_ROWS`, `enabled()`); has its own
  `Morsel`, `MorselCursor`, `offer` (with an extra cancellation flag), and producer.
- **`AggState`** (`LmdbNativeAggregateState.java`): per-group accumulator — `counts[]`, `sums[]` (Literal running
  sums via `MathUtil.compute(PLUS)`), `avgCounts[]`, `extremes[]`/`extremeIds[]` (with `ctx.comparator`),
  `typeErrors[]` (poisoned SUM/AVG omit their binding), distinct channel sets (`LongHashSet`, union-able).
- **Tests**: `LmdbNativeParallelAggregationTest` (PARALLEL_RUNS assertions), `LmdbNativeParallelPipelinesTest`,
  `LmdbNativeDifferentialFuzzTest` (the fuzz's aggregate round), `LmdbNativeNonCountAggregateTest`.

## Plan of Work

Milestone A — all-sinks merges. In `AggState`, add `mergeFrom(AggState other)` handling every kind: channel sets
union (as today); non-distinct COUNT adds with the overflow-checked `FactorizedTail.addCounts`; SUM/AVG merge
running sums with `MathUtil.compute(PLUS)` (null-aware) and add `avgCounts`; `typeErrors` OR (a poisoned partial
poisons the merge); MIN/MAX keep the winning extreme via `ctx.comparator` (ids compared first, like `addExtreme`).
Keep `mergeCountsFrom` delegating to it or replace its call sites (`NativeGroupTable.mergeFrom` is the only
consumer). In `LmdbNativeParallelAggregation.tryEvaluate`, replace the `AggregateSpec.allCounts` gate with
`parallelMergeable`: refuse only specs with `distinct && kind != COUNT`. Red→green: new tests in
`LmdbNativeParallelAggregationTest` — SUM/AVG/MIN/MAX GROUP BY over the large fixture assert `PARALLEL_RUNS`
ticked AND equality with the sequential result (flag the pool off for the sequential reference); a
SUM(DISTINCT ...) query asserts the gate still refuses (PARALLEL_RUNS unchanged). Fuzz green.

Milestone B — the Exchange substrate. New `LmdbNativeExchange.java`: `Morsel` (quads + rows + POISON), `offer`
(with optional cancellation), `MorselCursor` (binding row cursor over the queue), and
`produceMorsels(PatternPlan root, RowState producerRow, queue, workers, failure[, cancelled])` — the superset of
the two existing producers. Rebase `LmdbNativeParallelAggregation` (delete its `Morsel`, `MorselCursor`, `offer`,
`produce`) and `LmdbNativeParallelPipelines` (delete its copies; keep its pool/reservation statics or move them to
the Exchange — mechanical either way, pick the smaller diff) on it. Behavior-neutral (Routine B evidence: pre/post
green on both parallel suites + Hit Proof via the suites' PARALLEL_RUNS/PARALLEL_ROW_RUNS assertions).

Milestone C — phase exit. Fuzz green (flag combinations: parallel on/off); `LmdbNativeParallelAggregationTest`,
`LmdbNativeParallelPipelinesTest`, aggregation suites green; full `core/sail/lmdb` verify (one known pre-existing
failure); theme ITs; formatter + copyright; ExecPlan retrospective + memory update.

## Concrete Steps

From `/Users/havardottestad/Documents/Programming/rdf4j` (`set -o pipefail`; one mvnf at a time; protect the theme
benchmark store from root `clean`):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelAggregationTest   # A red→green, B pre/post
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeParallelPipelinesTest     # B pre/post
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest      # oracle
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb                      # phase exit

## Validation and Acceptance

Acceptance as behavior: on the parallel-aggregation fixture, `SELECT (SUM(?v) AS ?s) ... GROUP BY ?g` with ≥2
configured threads and a root estimate above the parallel threshold produces exactly the sequential result while
`PARALLEL_RUNS` increases; `SUM(DISTINCT ?v)` leaves `PARALLEL_RUNS` unchanged; both parallel suites and the fuzz
stay green before and after the Exchange extraction; full module verify green modulo the known pre-existing
failure.

## Idempotence and Recovery

`rdf4j.lmdb.parallel.enabled=false` disables both frameworks throughout; the merge-gate lift is one predicate that
can be restored to `allCounts` if a merge unsoundness appears; the Exchange extraction is mechanical movement with
both suites as the safety net.

## Interfaces and Dependencies

Modified: `LmdbNativeAggregateState.java` (`AggState.mergeFrom`), `LmdbNativeParallelAggregation.java` (gate +
rebase), `LmdbNativeParallelPipelines.java` (rebase), `LmdbNativeGroupTable.java` (merge call), tests. New:
`LmdbNativeExchange.java`. Reuses: `FactorizedTail.addCounts`, `MathUtil`, `NativeGroupTable`, the pool/reservation
machinery.
