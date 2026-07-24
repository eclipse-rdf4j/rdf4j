# 06 — Factorization

Goal: factorization stops being gated on incidental plan shape. The batched prefix engine runs with or
without a factorized tail; multiplicity is a first-class cursor property so counting branches can sit at
any depth; dead and filter-read patterns factorize wherever the dependency rule allows; the
enum-materialization latch heals; and GROUP BY key reduction reuses the machinery DISTINCT already has.


## Current state

Factorization is the engine's best-composing pillar — it hosts the chunk pipeline
(`LmdbNativeFactorizedRows.java:412`) and runs inside parallel workers
(`LmdbNativeParallelPipelines.java:601`) — but its admission is shape-brittle:

- The chunk pipeline is reachable only as the prefix of a factorization: `flatCount == n` bails
  (`LmdbNativeFactorizedRows.java:330`, `:194`) even though `tryOpenPrefix` itself requires only
  all-`PatternPlan` depths 0..flatCount (`LmdbNativeChunkPipeline.java:107-112`); the parallel paths
  already run a tail-less batched chain (`LmdbNativeParallelPipelines.java:608-615`,
  `LmdbNativeParallelAggregation.java:688-702` — including the MemoBudget recipe at `:697-698`).
- The tail peel walks a contiguous run of `PatternPlan`s from the end (`:324-329`); one
  LeftJoin/Union/Values/Path plan at or after a dead pattern kills the peel via the `reads[d] = ~0L`
  over-approximation for non-pattern children (`:303-306`), the `instanceof` peel guard (`:325`), and
  the hard cast (`:203`). Interior LIVE patterns inside the run already become ROLE_ENUM branches
  (`:216, :225-229`) — the dependency rule `(fresh[d] & laterNeeds[d+1]) == 0L` is the same rule Kuzu
  applies; the blockers are operator-kind precision and multiplicity plumbing, not position.
- `multiplicity()` lives on `FactorizedRowCursor`/`FlatRowCursor` but not on the base `RowCursor`
  (`LmdbNativeSlotPlan.java:241-246`); consumers recover it by `instanceof`
  (`LmdbNativeRowStep.java:1147-1149`, `LmdbNativeParallelPipelines.java:635-637`). An interior counting
  branch requires every chain cursor above it to carry and multiply multiplicity — the actual missing
  piece.
- OPTIONAL branches: `ROLE_COUNT` exists as a batched no-materialization scan folded into multiplicity
  (`:216, :1024-1030, :548-557, :740-745`) but only for `PatternPlan` tail branches; a `LeftJoinPlan`
  whose fresh slots are dead can only be dropped when `duplicateInsensitive`
  (`LmdbNativeAggregatePlanner.java:486-488`) — the third option (multiplicity `max(1, matchCount)`,
  exactly LeftJoin bag semantics) is missing.
- The enum refusal latch is sticky per cursor: one over-budget key sets `ordinarySuffixOnly = true`
  forever (`LmdbNativeFactorizedRows.java:487, :540`, read `:517, :524`) even though `dropEnumScratch`
  (`:959, :1074-1081`) fully released the reservation — the latch substitutes for a missing per-key
  negative memo.
- GROUP BY keys are taken verbatim (`compileGroupSlots`, `LmdbNativeAggregatePlannerBase.java:499-506`)
  with a mode cliff at arity 4 into a boxed HashMap (`LmdbNativeGroupTable.java:112-123`), while the
  identical reduction inputs exist and are used for DISTINCT next door: `fixedMask` + alias propagation
  (`LmdbNativeSlotOrder.java:38-39, :52-75`), residual key narrowing `without(keySlots, bestPrefix,
  fixedMask)` (`:350, :761-773`), `completeGroupPrefix` computing the varying set for GROUP BY
  (`:439-441`) and using it only to pick execution mode, never to shrink `groupSlots`. Reconstruction
  obligation: `toBindingSet` reads only `key.ids[i]` by `groupSlots` (`LmdbNativeGroupStep.java:812-821`)
  — narrowing without a payload channel silently drops variables.


## Work item 1 — Tail-less chunk prefix (serial path)

Add the chunk-prefix + chain-suffix composition for the serial path: when the peel yields
`flatCount == n` (no tail) or a partial prefix, open `tryOpenPrefix` over depths 0..k and continue with
`openChainFrom`-style nested-loop over depths k..n (`MultiJoinPlan.openChainFrom` consumes an external
depth-0 cursor already, `LmdbNativeJoinPlans.java:170-181` — the missing piece is the symmetric
"chunk-prefix feeds chain-suffix" adapter draining batches into per-row suffix opens). Bag cardinality is
already proven: `ChunkPrefixRowCursor.next()` emits one row per solution with `recomputeBoundMask`
(composition-analysis-verified; multiplicity handling explicit at
`LmdbNativeParallelPipelines.java:629-631`). Memory is internally bounded
(`MAX_REPLAY_MATCHES = 1<<14`, `BATCH_ROWS`, `LmdbNativeChunkPipeline.java:87-89`).

Serial entry keeps merge/SIP enabled (single globally ordered root stream — the worker-only stripping
does not apply). This also removes the last reason `externalRootCandidateEnabled` gates the parallel
variant (plan 04 §3 flips it).

Acceptance: a BGP whose optimizer order ends in a ValuesPlan / repeated-slot pattern / nested join —
today straight to adaptive/nestedLoop (`LmdbNativeRowStep.java:1261-1280`) — shows `chunkPrefix` tags
with run replay, memoized probes, SIP and merge active; benchmark on an IN-folded shape improves
accordingly.


## Work item 2 — Multiplicity on the base cursor interface

Promote `long multiplicity()` to `RowCursor` (`LmdbNativeSlotPlan.java:241-246`) with default 1;
`JoinCursor`/`FilterCursor` multiply child multiplicities into their own; consumers drop the
`instanceof` recovery (`LmdbNativeRowStep.java:1147-1149`, `LmdbNativeParallelPipelines.java:635-637`).
This unlocks: interior counting branches (a dead selective pattern at depth D compiles to a per-key
count folded into multiplicity regardless of what sits at depths > D), and the OPTIONAL count branch
(item 3). Then narrow the sink's remaining vetoes accordingly: precise `reads[d]` for
LeftJoin/Union/Values children (their read sets are computable — `memoReadMask` already enumerates these
kinds, `LmdbNativeAggregateCompiler.java:249-304`) replacing the `~0L` over-approximation where the
child's actual reads permit peeling past it.

Correctness constraints preserved verbatim: ORDER BY key-stability loop
(`LmdbNativeFactorizedRows.java:188-193`, doc `:145-148`) and the external-root `minimumFlatCount`
(`:188, :164-176`).

Tests: multiplicity-conservation property test — for random plans, Σ multiplicities of factorized
output == row count of the nested-loop reference; dispatch-contract cases for interior-dead-pattern
shapes.
Acceptance: `?s :worksAt ?org . ?org :taxId ?tid . ?org :locatedIn ?city` (dead selective `?tid`
mid-plan) stops enumerating tax-ids per row.


## Work item 3 — OPTIONAL as a counting branch

For an OPTIONAL whose right side is a single (possibly filtered) triple pattern and whose fresh slots are
outside `outputMask` and `laterNeeds`: compile to a ROLE_COUNT-style branch contributing multiplicity
`max(1, matchCount)` — exactly SPARQL LeftJoin bag semantics for discarded bindings. The peel condition
`(fresh & laterNeeds) == 0` already guarantees no later consumer observes the slots. Richer right sides
keep today's enumeration (the per-key inner-side amortizations — `RightMemoProbe`,
`PatternPayloadProbe`, `LeftJoinCursor` replay — already bound their LMDB cost; the residual is bind/emit
fan-out, which this item eliminates for the single-pattern case only).

Tests: bag-equivalence vs generic evaluator on OPTIONAL corpora with duplicate solutions, plus under
DISTINCT (where the existing drop rewrite must still win — assert rewrite priority).
Acceptance: `SELECT ?s WHERE { ?s :type :Doc . OPTIONAL { ?s :reviewedBy ?r } }` with hub documents
stops emitting k rows per document.


## Work item 4 — Heal the enum latch; guard the tail-group edge

Replace the cursor-lifetime `ordinarySuffixOnly` latch with a per-key negative memo: on
`growEnumScratch` refusal, record the offending key in a small `LongHashSet` on the branch (bounded 4096
entries, clock eviction); subsequent prefix rows with OTHER keys retry enumeration (the budget genuinely
recovered — `dropEnumScratch` releases the full reservation, `:1074-1081`); rows with a memoized key go
straight to the streaming suffix. Keep memo-saturation refusals latched (that state does not recover
until branch close, `:1005, :1103`). Also add the guard from plan 02 §5's sink work: the
`tailGroupPos` mode (`LmdbNativeFactorizedTail.java:262-263`) must not receive re-placed filters.

Acceptance: a power-law store (one 2M-fan-out key early in the cursor) keeps factorized execution for
the remaining keys — measured on a skewed synthetic dataset added to the benchmark corpus.


## Work item 5 — GROUP BY key reduction (functional dependencies, RDF-sound)

Narrow `groupSlots` using ONLY sound equivalences: `BIND(?x AS ?y)` aliasing and `sameTerm` — value
equality (`=`) merges groups SPARQL keeps separate and must never reduce. Mechanism: reuse
`fixedMask`/`withAliases` propagation (`LmdbNativeSlotOrder.java:38-75`) and `completeGroupPrefix`'s
varying-set computation (`:439-441`); reduced-out keys become payload channels — stored once per group
alongside the aggregate state so `toBindingSet` (`LmdbNativeGroupStep.java:812-821`) reconstructs them
(Kuzu's dependentKeys shape). SPARQL forces every projected non-aggregate variable into GROUP BY, so the
idiomatic `GROUP BY ?s ?name ?email ?dob` collapses to `?s` + 3 payload lanes — crossing the arity-4
cliff back into the primitive table (whose widening is plan 10 §2; the two items compound but land
independently).

Also extend the ordered-group machinery's reach while here: streaming hash-free groups
(`LmdbNativeSlotOrder.aggregate:353-375` → `LmdbNativeGroupStep.java:418-450`) are gated on
`plan.specialized()` i.e. DISTINCT-aggregate channels only (`:309-310, :254`) — admit the plain
multi-key GROUP BY whose `completeGroupPrefix` proves the group prefix ordered.

Tests: group-equivalence corpus with distinct-but-equal literals (`"1"^^xsd:integer` vs
`"01"^^xsd:integer`) asserting groups are NOT merged; alias shapes asserting reduction fires;
reconstruction round-trip.
Acceptance: the 4-key idiomatic aggregate drops the boxed HashMap path (dispatch tag), with the
corresponding benchmark win.
