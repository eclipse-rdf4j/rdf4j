# 09 — Join algorithms

Goal: the join layer reaches Kuzu parity where it matters for RDF: lean hash-table internals, real
mark/anti joins for EXISTS/NOT EXISTS/MINUS, multi-key merge and SIP, cost-chosen build sides, a
worst-case-optimal intersect for cyclic BGPs, and decorrelation via outer-side accumulation.


## Status (2026-07-31)

A code-verified audit (four independent deep reads against HEAD of `optimize-lmdb`) established which
work items below are delivered. Execution of the remaining items now lives in
`.agent/lmdb-join-strategy-execplan.md`; this file stays as roadmap context.

DELIVERED (verified in code):

- Work item 1 (hash join internals) — substantially complete in `LmdbNativeHashJoin`: flat primitive
  key/payload layout, stored per-entry hashes, 8-bit fingerprints (`:403-404, :560`), unique-key fast
  path (`:216, :450`), estimate-based smaller-side build selection (`:90-98`), cost-vs-INLJ admission
  (`:103-106`). Remaining from item 1: payload key-slot exclusion not re-audited. Admission is still a
  row-count cap (`DEFAULT_MAX_BUILD_ROWS = 1 << 20`, `:34`), replaced by byte-aware ledger admission in
  the ExecPlan (its Milestone 2); overflow today discards the partial build and re-scans both sides
  (`:310`, `:202-208`, counter `CAP_FALLBACKS` `:38`).
- Work item 4 (worst-case-optimal multiway intersect) — delivered and exceeded by
  `LmdbNativeLeapfrogJoin`: general GYO ear-reduction cyclic-core detection (`findCyclicCore` `:371`),
  Leapfrog Triejoin over variables, pushed filters, direct EXISTS edges, multiplicity counting,
  parallel execution. Remaining gaps → ExecPlan Milestone 1: frontiers are always materialized +
  sorted (`scanFrontier` `:1154`), intersection is pattern-order not smallest-first with per-level
  allocation (`Level.enter` `:1012`), dispatch is structural-first (`LmdbNativeRowStep.java:865`)
  rather than cost-arbitrated, and multiplicity is emitted by row repetition (`:704-707, :749-757`).
- Item 3.3 partial groundwork: single-slot SIP masks from completed hash builds are live
  (`trySipTarget` `LmdbNativeChunkPipeline.java:408`, seek-ahead via `masksVerdict` `:713-743`). The
  zero-copy adjacency-key borrow (`borrowSortedKeys` `:478` / `publishCsrMask` `:1044`) is DEAD in
  production — no `NativeProbe` overrides `adjacencyCacheKeys()`; superseded by the ExecPlan's
  domain-cursor producer.

REMAINING (→ ExecPlan milestones):

- Work item 3 (merge smaller-run buffering, multi-key merge/SIP) — merge still always buffers the
  right run and rescans on overflow (`startRun` `LmdbNativeMergeJoin.java:341`, `openRescan` `:444`);
  single-key gate still in place (`:91-95`). Now evidence-gated (ExecPlan Milestone 4: only if
  overflow rescans ≥15% of operator CPU on representative workloads).
- Work item 5 (accumulate/decorrelation) — nothing accumulates the outer stream; all bulk operators
  reject correlated entry. NOTE: the line citations below (`:376-379, :1229`) have drifted; the
  correlated-entry seams on HEAD are `LmdbNativeRowStep.java:583-586`, `:880-894`, `:959-964`. This is
  ExecPlan Milestone 3 (accumulate–semijoin–probe, INNER → EXISTS/NOT EXISTS → OPTIONAL).
- Work items 2 and 6 — not re-audited in this pass; unchanged as written.


## Current state

Hash join (`LmdbNativeHashJoin`): payload materializes every right-produced slot including the key
(duplicated; `payloadSlots:111` — contrast `LmdbNativeLeftJoinPayloadProbe.java:143-152` which already
excludes keys); jagged `long[][] keys` layout, no stored hash (growth re-derives), no fingerprint, no
unique-key fast path; build side hardwired to the second child with a SYMMETRIC 4096 floor on both sides
(`:32, :61-64`) — inverted economics: when probe is small and build large, vetoing the hash join is
correct (the alternative is an INDEX nested-loop via `((PatternPlan) right).open(row, rightProbe)`,
`LmdbNativeJoinPlans.java:631-639`, not a rescan), but when build is small and probe large the floor
wrongly refuses. Merge join buffers the RIGHT side unconditionally and rescans via `openRescan`
(`LmdbNativeMergeJoin.java:289-310, :392-408`) instead of buffering whichever equal-key run is smaller;
the single-bit key gate (`:71-76`) is forced by the composite-source `getIndexName` defect (plan 07 §2
fixes the root cause). EXISTS with multiple shared variables and retained MINUS fall to a boxed
HashMap/nested-loop anti-join; the membership build is disabled under any dataset/GRAPH scope, and
`compileDirectExists`/`ExistsFilter` (`LmdbNativeAggregateFilterCompiler.java:386-397`) re-evaluate
per row where a build-once mark join would serve. Every join is binary; cyclic BGPs pay full
intermediate materialization (`children.length == 2` in both batch joins; the flatten at
`LmdbNativeSlotPlan.java:47-53`); `FactorizedTail.independentBranches` structurally excludes the cyclic
shape (`LmdbNativeFactorizedTail.java:461-485, :500`). The storage primitives for multiway intersection
exist: `PatternCursor.seekForward` (`LmdbNativePatternTerms.java:225-229` →
`RecordIterator.seekForward`), sorted emission under `spoc,posc` for both (s+p bound) and (p+o bound)
shapes, varint order == numeric order. Correlated entry degrades eight dispatch sites to per-row
nested-loop (provenance: commit `19ebab53d2`, a real latency regression fix); the amortizations that
exist (`JoinCursor.record`, `RightMemoProbe`, `PatternPayloadProbe`) are inner-side only — nothing
accumulates the OUTER stream; `derivedFactorizedPlan` is memoized by boundMask
(`LmdbNativeJoinPlans.java:112-126`) but `tryCreate`'s per-open analysis is not. OPTIONAL's runtime hash
build waits for 1024 observed nested-loop probes instead of consulting available estimates.


## Work item 1 — Hash join internals

1. Payload: exclude `keySlots` from `payloadSlots` (the left-join probe already shows the pattern);
   intersect with a liveness mask lowered from the projection (`requiredAggregateNames` plumbing,
   `LmdbNativeAggregatePlanner.java:186-210`).
2. Layout: flat `long[width*capacity]` keys, stored hash per entry (no rehash on growth), 8-bit
   fingerprint beside heads (shared design with plan 03 §2 — implement once, use in both tables).
3. Unique-key fast path: during build, detect key uniqueness (a single insert-collision flag); unique
   builds skip chain-walk bookkeeping on probe and the cursor's mid-chain resume state.
4. Build-side selection + asymmetric floor: replace the symmetric 4096 floor with a cost comparison
   using plan 01 §2 estimates — `cost_hash = build + probe` vs `cost_inlj = probeRows × seekCost`;
   choose build side as the smaller input when both children are eligible (the build sweep
   `openRawUnbinding` works for either side; today's hardwiring to child[1] is arbitrary).

Tests: equivalence corpus across widths/duplicates/context keys; forced-growth tests (hash retention);
build-side flip cases pinned in dispatch-contract tests.
Acceptance: probe-bound join benchmark ≥1.5× from layout+fingerprint alone; small-build/large-probe
shapes flip to hash join and win accordingly.


## Work item 2 — Mark joins for EXISTS / NOT EXISTS / MINUS

Replace per-row re-evaluation and boxed anti-joins with build-once mark semantics:

1. Multi-key primitive mark table: reuse `PrimitiveTupleTable` (widths 1–4; width extension in plan 10
   §2 serves both) storing presence only.
2. `FILTER EXISTS { pattern }` / `NOT EXISTS`: when the subquery is a single pattern (the
   `compileDirectExists` case) build the key set once per evaluation via the raw-unbinding sweep and
   probe per row — extending `PatternMembershipProbe` (`LmdbNativeMembership.java:104-129`); route the
   build through `openRawUnbinding` PLUS the per-quad default-graph filter from `PatternPlan.bind`
   (`:433-435`) and drop the dataset/GRAPH-scope disable (`:47-51`) — the two mechanisms together
   reproduce scoped semantics (this is the verified fix shape; `StatementPatternExistsFilter` holds raw
   Terms and needs its own parallel path).
3. Richer EXISTS subqueries: compile the subplan, sweep it once when uncorrelated
   (`memoReadMask`-style read-set test), mark-join; correlated subqueries keep `ExistsFilter` per-row
   evaluation until item 5's accumulate lands (then: accumulate outer, semi-join).
4. Retained MINUS: hash anti-join on the shared-variable key set with SPARQL MINUS's
   disjoint-domain-compatible semantics (empty shared set ⇒ no removal) — the boxed nested-loop remains
   only for the truly incompatible cases.

Tests: EXISTS/MINUS corpus incl. dataset/GRAPH scoping, unbound-shared-variable edges, bag-cardinality
preservation (marks must not dedup the OUTER side).
Acceptance: `FILTER NOT EXISTS` over a 1M-row outer with a selective inner pattern goes from per-row
probes to one sweep + O(1) probes; MINUS benchmark shape ≥3×.


## Work item 3 — Merge join: smaller-run buffering and multi-key

1. Buffer whichever equal-key run terminates first (stream both up to `maxRunRows`, keep the completed
   one, stream the other), eliminating `openRescan` whenever either run fits — removes the rescan's
   repeated LMDB seeks on skewed keys.
2. Multi-key: after plan 07 §2 fixes `getIndexName` delegation, the composite source's full-4-field
   merge engages; lift `Long.bitCount(keyMask) != 1` (`:71-76`) to accept key SEQUENCES that are a
   prefix of both sides' index field order (`leadingKeySequence` already validates exactly this for the
   chunk pipeline, `LmdbNativeChunkPipeline.java:256-283` — reuse it). The sound condition when a
   composite source is active with multiple branches remains single-field; gate on "single active
   ordered branch OR verified full-key merge" per the verified analysis.
3. Multi-key SIP in the pipeline: publish one mask per qualifying key slot (relaxing
   `trySipTarget:412-415`; build is already multi-key `:863-886, :1306-1308`; consumer already ANDs,
   `:710-736`; `SipMask.tryCreate` stops truncating to `key.ids[0]` at `:489-491`; per-slot publication
   conditions per the verified analysis at `:416-418`). The merge-walk gate (`tryPlanMerge:203-206`)
   KEEPS its restriction — its per-order-field rationale is real.

Acceptance: `?a :knows ?b . ?a :worksWith ?b` (two shared variables — the MOST selective joins) gets
merge/SIP treatment (dispatch tags); skewed-key merge benchmark drops its rescan seeks.


## Work item 4 — Leapfrog multiway intersect (worst-case-optimal)

New N-ary operator for the cyclic shape binary joins handle worst:

1. Detection (runtime compiler, not the DP): a variable produced fresh by ≥2 patterns whose OTHER
   positions are all bound by the already-planned prefix — the triangle/diamond closing step. Detected
   over the derived order in `MultiJoinPlan`; no sketch involvement.
2. Operator: `LeapfrogCursor` holding k prefix-bound `PatternCursor`s each emitting the shared variable
   in ascending ID order (guaranteed under `spoc,posc` for (s+p)- and (p+o)-bound shapes; assert index
   availability, decline otherwise); classic leapfrog: max of current keys, `seekForward` the laggards,
   emit on agreement. `seekForward` exists on the cursor surface (`LmdbNativePatternTerms.java:225-229`);
   CSR-served sides get it from plan 07 §1.
3. Admission: cost the intersect (Σ min-run estimates, plan 01 §2 fan-outs) against the binary plan;
   engage only on detected cycles — acyclic shapes never see it.
4. Batch integration: the operator emits into `NativeBatch` like other stages; multiplicity 1 per
   emission (set semantics of the intersect variable are exactly BGP semantics here since each pattern
   contributes one binding).

Tests: triangle/diamond corpus vs generic evaluator (bag equivalence — duplicate edges must produce
duplicate triangles: the cursors enumerate duplicates as the store does); adversarial skew (one dense
side); decline cases (missing index order).
Acceptance: triangle query over a 10M-edge social graph moves from intermediate-materialization scaling
to Σ-min-run scaling — the benchmark asserts the store-probe count, not just time.


## Work item 5 — Accumulate: decorrelate the eight degraded sites

1. Interim (cheap, immediate): a `boundMask`-keyed cache around `LmdbNativeFactorizedRows.tryCreate`
   mirroring the existing `orderCache` (`LmdbNativeJoinPlans.java:112-126`) — correlated re-opens stop
   re-running `analyzeSplit` and re-allocating branch scaffolding per outer row.
2. Structural: an outer-side `Accumulate` — buffer the outer binding stream (budgeted, plan 12), then
   run ONE hash/merge/batch join against the native fragment, restoring every bulk strategy at
   correlated entries. Placement: where the generic evaluator re-enters the native fragment per binding
   (the `NativeBareRowsStep`/correlated-entry seam, `LmdbNativeRowStep.java:376-379, :1229`). Semantics:
   the accumulated join must reproduce per-row evaluation order-insensitively — sound for inner joins
   and for the EXISTS semi-joins from item 2; OPTIONAL accumulation follows the left-join hash shape
   with per-outer-row match tracking. Overflow: budget refusal falls back to today's per-row path.
3. OPTIONAL costing: let the left-join hash build consult estimates (plan 01 §2) instead of waiting
   1024 observed probes — build immediately when `estimatedOuterRows × rowsPerProbe` exceeds the build
   cost; keep the observational trigger as the no-estimate fallback.
   STATUS (2026-07-31): DONE for multi-pattern OPTIONAL rights — but the mechanism differed from the
   sketch above. For a fragment right (the acceptance shape), NO hash-build floor was reachable:
   `PatternPayloadProbe` is single-pattern-only and the chunk pipeline never sees a `LeftJoinPlan`
   root; the per-row cost was `RightMemoProbe`'s per-distinct-key fragment runs. Fix: estimate-triggered
   key-unbound sweep in `RightMemoProbe` (`LmdbNativeLeftJoinMemo.java`), trigger
   `expectedProbes × (SEEK_COST + perProbeRows) ≥ sweepEstimate`, flag
   `rdf4j.lmdb.leftjoin.sweep.enabled` (default off), counter `LEFTJOIN_SWEEP_BUILDS`, tests
   `LmdbNativeLeftJoinSweepTest`. Still open: the same estimate trigger for the single-pattern
   `PatternPayloadProbe` 1024-probe floor and for inner `JoinCursor` correlated fragments.

Tests: correlated-entry corpus (VALUES-driven, OPTIONAL-driven, subselect-driven) asserting bag
equivalence and single-execution of the accumulated join (probe counters); overflow fallback test.
Acceptance: the `19ebab53d2` regression query stays at its fixed latency; the accumulate benchmark
(1000 outer bindings × heavy inner fragment) collapses from 1000 fragment executions to 1.


## Work item 6 — Bushy runtime builds (bounded scope)

Let the hash join build over a SUB-JOIN when the right operand is itself a `JoinPlan`/`MultiJoinPlan`
whose sweep is input-independent: build by draining `right.open(unbound)` once (`JoinCursor` already
materializes-and-replays independent right subtrees, `LmdbNativeJoinPlans.java:588-597` — the
recognition predicate exists; the change is routing it into `PrimitiveHashJoinTable` instead of the
replay list). This recovers the practical value of bushy plans (build a selective star once, probe from
the other star) with NO enumerator change — the shape arises exactly where `SlotPlan.join` already
produced nested join nodes (`LmdbNativeSlotPlan.java:55`). DP-level bushy enumeration remains out of
scope (00-overview).

Acceptance: snowflake benchmark (two selective stars joined on one variable) stops re-executing the
second star per row (probe counters), ≥2× on the shape.
