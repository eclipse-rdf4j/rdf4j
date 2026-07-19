# 11 — Property paths

Goal: arbitrary-length paths stop being the escape hatch. BFS work is shared across driving rows,
restricted by both endpoint sets, terminated early, organized as level-synchronous frontiers over sorted
ID arrays, and covers alternation/inverse/cycle shapes natively. The `GRAPH ?g` gate and sequence paths
stay excluded (00-overview).


## Current state

`PathPlan.open` builds a fresh cursor per invocation (`LmdbNativePathPlan.java:108`); all traversal
state is per-cursor (`discovered:379`, queue/pending `:168-171`); the engine's memo layers are
structurally unreachable for paths (`FactorizedTail` needs a `PatternPlan` tail, chunk pipeline an
all-pattern prefix, `KeyedMatches` has no path caller). Worse, replay is denied even when UNCORRELATED:
`memoReadMask` falls through to −1 for `PathPlan` (`LmdbNativeAggregateCompiler.java:249-304, :303`), so
`JoinCursor` never materializes-and-replays it (`LmdbNativeJoinPlans.java:594-598`), and `openRight`
reuses a cached probe only for `PatternPlan` (`:631-639`) — `?x :p+ ?y` joined under anything re-runs
the whole BFS per driving row, while the all-pairs loop (`initializeStarts:326-349`,
`nextAllPairs:290-303`) is structurally Kuzu's per-offset loop already, dying with the cursor.
Mode is chosen by endpoint boundness (`:190-206`); a path ordered after a sibling that binds an endpoint
gets per-row starts (fine); MODE_ALL_PAIRS (leading/disconnected paths) enumerates every subject of the
step predicate — for zero-length `*`, every subject-or-object in the dataset (`:355-368`) — before the
first emit, with no LIMIT awareness. No output-side restriction in ANY mode: far-endpoint sets from
later patterns never prune the BFS, and there is no early termination when all admissible targets are
found. Shape gates (`compilePath`, `LmdbNativeAggregatePlanner.java:409-437`): alternation
(`!(pathExpr instanceof StatementPattern)`, `:413-415`), same-variable ends (`:419-421`), `GRAPH ?g`
(`:426-428`, correctness-required), variable predicate (`:431-433`); a null return strips the native
engine from SIBLING patterns too (`:482, :490-493, :507-509, :519-522`). Cardinality for join ordering
is a hardcoded constant family (`estimate:90-98`). The BFS itself is node-at-a-time single-threaded;
`discovered.add(far)` gives correct at-most-once endpoint semantics (`:408`), and replay-safety is
declared (`LmdbNativeSlotPlan.java:103-105`).


## Work item 1 — Share BFS results across driving rows

1. One line first: `memoReadMask` returns `plan.producedMask()` for `PathPlan` — sound because
   `compilePath` rejects slot-bearing contexts and repeated slots (`:425`), so a path's read set is
   exactly its endpoint slots; uncorrelated paths immediately get `JoinCursor`'s
   materialize-once/replay (`:588-597`). Probe reuse in `openRight` extends to `PathPlan` the same way
   `PatternPlan` caches its probe (`:631-637`).
2. Query-scoped per-start memo: `startId → long[] reachable` (sorted), hung off query scope like
   `FactorizedTail.MemoBudget` (`LmdbNativeFactorizedTail.java:50-78`) — NEVER off the plan object
   (plans outlive evaluations and carry shared mutable caches already; the memo must not,
   `LmdbNativeJoinPlans.java:38-44` precedent). Budget-accounted (plan 12); refusal falls back to
   per-row BFS. Replaying a recorded set preserves cardinality and encounter order (ALP set semantics,
   at-most-once per source, `:408`).
3. Correlated case with repeated starts (N driving rows, D distinct starts, D ≪ N after many-to-one
   joins): the memo serves repeats O(1) — this is the multiplicative win on deep hierarchies.

Tests: replay-equivalence vs fresh-BFS per row (bag + order); memo-refusal fallback; snapshot-scope test
(memo must not survive across transactions).
Acceptance: `?a rdf:type :C . ?a skos:broader+ ?b` with 100k rows over 1k distinct starts runs 1k
traversals, not 100k (probe counters).


## Work item 2 — Endpoint restriction and early termination

1. Admissible-target set: when the far endpoint is constrained by a later pattern or VALUES, thread an
   optional `LongHashSet targets` into `PathCursor`; `bindEndpoints` (`:429-434`) filters emissions,
   and the BFS terminates when all targets are found (countdown; per-source reset in `startBfs`
   `:378-390`). Source: the SIP machinery's mask shape — but path-input masks arrive naturally via
   per-row starts already (verified: the input-side gap is confined to leading-operand paths), so
   OUTPUT-side is the missing half everywhere.
2. LIMIT awareness for MODE_ALL_PAIRS: make `initializeStarts` lazy — begin BFS from the first start
   while start enumeration continues, so a downstream LIMIT can close the cursor before the full-store
   start scan completes (`:355-368` is the current eager pre-scan). Pure iterator refactor; encounter
   order of results is already unspecified for all-pairs.

Acceptance: `?x :cites+ ?y . VALUES ?y { ... 5 values }` terminates each BFS at 5 hits (visible in
expansion counters); `?x :p* ?y LIMIT 10` on a large store returns in ms, not after a full scan.


## Work item 3 — Frontier BFS over sorted ID arrays

Replace queue-of-nodes expansion with level-synchronous frontiers:

1. Frontier as sorted `long[]` (dedup on build); expansion walks the frontier in ascending ID order,
   which turns per-node probes into sequential `seekForward` sweeps over the step predicate's index —
   or, when a CSR entry exists (plan 07), direct `runStart` slicing per frontier element with binary
   search. Sorted-array frontiers are the RDF-shaped substitute for Kuzu's dense offset bitmaps
   (verified inapplicable: value IDs are sparse).
2. `discovered` migrates from `LongHashSet` to a sorted-run structure merged per level (the level's new
   nodes are produced sorted by construction) — membership tests become binary searches over runs.
3. Parallel frontier expansion (after plan 04 §1): morsel over frontier slices per level with a
   per-level barrier; per-worker discovered deltas merged at the barrier (sorted merges). Level-sync
   preserves ALP set semantics exactly (at-most-once discovery arbitration at the merge).

Tests: equivalence vs the queue BFS across cyclic/diamond/self-loop graphs; determinism of the result
SET under parallel expansion (order is unspecified).
Acceptance: deep-hierarchy benchmark (`skos:broader+` over 1M-node trees, hub-heavy synthetics) ≥3×
serial from access-pattern locality; near-linear scaling on the parallel variant for wide frontiers.


## Work item 4 — Alternation, inverse, and same-variable cycles

1. `(a|b)+` and `(p|^p)+`: parse shape is `ALP(Union(SP, SP))` with shared endpoint vars; replace the
   single `stepPredId()` (`:418-420`) with a small `(predId, direction)[]` iterated in `expandNext`
   (`:393-416`) and `initializeStarts` (`:319-359`) — the direct analogue of Kuzu's per-level rel-table
   loop; inverse is already parameterized (`endpointPosition`, planner `:438-446`). Relax the planner
   gate (`:413-415`) for `ALP(Union(...))` trees whose leaves are all constant-predicate
   StatementPatterns with consistent endpoint variables; everything else still declines. Reachability
   over `a ∪ b` is exactly SPARQL's ALP relation — no semantic subtlety.
2. `?x :p+ ?x` (cycles): lift the same-variable gate (`:419-421`) — `RowState.bind` self-filters
   (returns `current == id` on rebind, `LmdbNativeRowState.java:43-48`), and the existing
   mark/rollback per emission (`:290-317, :429-434`) plus the start-not-marked-discovered invariant
   (`:145-148`) make each cycle emit exactly once; `?x :p* ?x` is the identity-pair path plus the
   `found == currentStart` skip (`:296-299`) — verified consistent.
3. `minLength > 1` (`:410-412`) stays — dead for parsed SPARQL (parser emits only `*`/`?`/`+`), a
   harmless safety net for programmatic algebra. NPS and sequence stay excluded.

Tests: alternation/inverse/cycle corpus vs `PathIteration` (the semantics oracle, per the module's own
javadoc discipline `:404-408`), including duplicate-solution multiplicity and zero-length edges;
existing fallback-pinning tests (`LmdbNativePropertyPathTest:209-226`) updated to pin the NEW boundary.
Acceptance: `(skos:broader|skos:broaderTransitive)+` and `(:knows|^:knows)+` run native (dispatch
tags), keeping sibling patterns native too (the `:482` stripping disappears for these shapes).


## Work item 5 — Path cardinality for the planner

Replace the hardcoded estimate family (`PathPlan.estimate:90-98`) with a fan-out-based closure
estimate: `min(reachableUpperBound, startCount × meanFanOut^depthCap)` with `meanFanOut` from plan 01
§2 and `depthCap` a small constant (4) — deliberately crude but MONOTONE in the right variables, which
is what join ordering needs (the current constants are monotone in nothing). Feed the memo's observed
reachable-set sizes back as the estimate for repeat evaluations within the query (free, exact).

Acceptance: join-order decisions around paths flip in the intended direction on the dispatch-contract
corpus (path-after-selective-pattern vs path-first shapes).
