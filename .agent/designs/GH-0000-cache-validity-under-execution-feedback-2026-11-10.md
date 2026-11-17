# Plan-cache validity under execution feedback — measured problem and proposed design (2026-11-10)

Status: proposal, awaiting the owner's decision. Nothing here is implemented.
Evidence: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/measure-final/summary.txt` and `followup/summary.txt`
(theme workload, 182 queries, store default configuration: native evaluation off, adaptive evidence, 2 ms sampling
cap; machine load 12–90 throughout).

## What was measured
1. `LmdbOperatorFeedbackStats.planningRevision()` returns `feedbackEpoch`, and `nextFeedbackEpoch()` runs once per
   accepted runtime-feedback publication (9,807 of 9,807; `publishInScope` line 703 and seven more call sites). The
   epoch is also the recency stamp handed to `observeResolved(..., epoch)`. `LmdbEstimatorRuntime.leoRevision()`
   mixes it with the filter-selectivity and learned-filter revisions and the provider generation; the decision
   cache's `EvidenceSnapshot` carries that value, and a replay-only entry is served only on identical evidence.
2. 44–54 % of accepted publications change no stored value. Another 9–17 % set their `changed` flag without moving a
   recorded value (lifecycle entry, semi-anti counts, censored bounds, an exact fact rewritten with the same value:
   485 identical rewrites against 2 real changes).
3. Where a model value does change, the planner-visible value (exp of the posterior mean) moves by a median of 6 %
   in the first rounds and 2 % by round 5, p90 15–40 %; of 411 keys updated in at least three rounds 315 shrink
   monotonically, 27 decrease non-monotonically, 69 change by exactly zero, none grow or plateau.
4. Of 4,278 model writes that changed a value, 2,131 were read by a later planning, 47 only by a detached refresh,
   2,100 by nothing.
5. Consequence for the cache. Public API, same query four times in a row: after a completed evaluation of the same
   query the next replay-only lookup missed 142 of 142 times; after a timed-out one (no feedback) it hit 29 of 32.
   Round-robin over queries: 0 replay-only hits in 116 lookups. Ranked entries hit, but in round-robin all 94 hits
   were stale serves with a detached re-plan, so the planning CPU is spent anyway. Re-planning an unconditional hit
   would have avoided: about 810–860 s over seven rounds of the full workload.
6. Capacity is a separate limit: about 25 families of about 300 KB fit in the default 4 MiB pipeline cache, so a
   rotation over 182 queries evicts every family before its next turn regardless of validity.
7. Under load 1 in 3 first plannings was cut by the 2 ms sampling cap and, by the `reproducible()` contract,
   published nothing. Not measured on a quiet machine.

## Diagnosis
One counter has two jobs. As a recency stamp for observations it must tick on every observation. As the version
of the evidence a planning depends on it must change exactly when a planning could come out differently. Using the
first for the second makes every completed query invalidate every evidence-exact cache entry in the store. This is
the same mistake as the decision cache's evidence epoch (fixed in cache round 6): a clock is not an evidence
version.

## Proposal, in three steps of increasing reach
Step 1 and step 2 are exact (no tolerance, no behaviour change other than fewer re-plannings). Step 3 is a policy.

**Step 1 — the planning revision advances only when planner-visible learned state changed.**
Keep `feedbackEpoch` as the observation clock. Add a separate planning-evidence revision in
`LmdbOperatorFeedbackStats` that advances only when a publication changed a value a planning can read: a model
posterior, an exact fact's rows, a lifecycle decision input, semi-anti counts, censored bounds. `planningRevision()`
returns it. Make every `changed` report truthful (an exact fact rewritten with the same rows is not a change). The
same audit applies to `LmdbFilterSelectivityStats.planningRevision()` and `LmdbLearnedFilterSelectivity`.
Expected effect: removes about half of the revision movements. It does not by itself rescue a query whose own
evaluation nudges its own cells, nor a rotation over many queries.
Tests first: a publication that changes nothing leaves the planning revision unchanged and a replay-only entry
served; each kind of real change advances it; persistence/restart keeps the two counters apart.

**Step 2 — a cached decision depends on the learned facts its planning read, not on the global revision.**
During a planning scope record the learned cells that were read (model `exactPosterior`/`posterior`, exact facts,
filter cells, lifecycle decisions) as (cell identity, cell version); cells get a version that advances when their
planner-visible value changes (step 1's notion). The entry stores the read set compactly (sorted ids plus one
combined digest; charged to the entry). Validity check on lookup: the global planning revision is the fast path
(unchanged → valid, as today); if it moved, compare the read set's versions. A cell absent at build time and
present now counts as changed (a planning that found no learned value must be invalidated when one appears), so
the read set includes the keys that were looked up and missed.
Expected effect: one query's execution no longer invalidates other queries' entries; about half of all changed
values are read by nobody. Cost to weigh: per-entry memory (read sets of hundreds of cells) against the 300 KB an
entry already costs, and a validity check proportional to the read set on the slow path only.
Tests first: executing query B leaves A's replay-only entry served when B wrote no cell A read; a write to a cell A
read invalidates A; a first observation for a key A looked up and missed invalidates A.

**Step 3 — materiality: small movements of a read value do not invalidate (policy, tunable).**
After steps 1–2 a query's own evaluation still moves its own cells by a few percent each time, shrinking but not
reaching zero. Proposed rule: an entry stays valid while every learned value it read is within a tolerance of the
value read at build time, compared in log space against the build-time value (cumulative, so slow drift still
invalidates eventually); exact facts and lifecycle decisions are never tolerated, only compared for equality.
Ranked entries already have this in a principled form (the stability envelope); the tolerance is the replay-only
substitute where no cost comparison exists. Optional: on a tolerated-but-moved hit submit a rate-limited detached
re-plan and replace the entry only if the plan differs.
The tolerance is a judgement, not a derivation. Starting point: ln(1.25); measure plan differences between the
served plan and a fresh planning over the theme workload at several tolerances before choosing.
Tests first: a read value moved within tolerance → served; beyond → miss; cumulative drift beyond tolerance → miss;
an exact fact change of any size → miss.

## Revision after the read/write inventory (2026-11-10, owner approved all three steps)
Inventory: `.agent/research/GH-0000-phase1-continuation-2026-11-09/learned-state-read-write-inventory.md`.
What it changes:

- **Step 1 stands**, with a sturdier mechanism: the revision is the sum of component mutation counters incremented
  next to each mutation, not a flag evaluated afterwards. It also has to cover changes that happen without any
  publication (evictions of evidence-bearing cells at precompilation), which today's clock misses, and one legacy
  reader whose value decays with the clock itself. Spec:
  `GH-0000-cache-validity-step1-truthful-planning-revision-2026-11-10.md`.
- **Step 2 cannot use per-cell versions.** What a planning reads is not a cell value but
  f(cell statistics, model decay clock, the calibration shared by the applicability, existence of sibling keys,
  the planner's own `features` and `predicted` inputs). A write to cell Y moves the decay clock and with it the
  value read from an untouched cell X (up to 2x after 4,096 ticks where the correction cap binds); another query's
  observations can flip X's gate through the shared calibration; the same cell is read under several `predicted`
  values in one planning. A version on X sees none of this.
  Replacement, same intent ("a cached decision depends on what its planning read"): **validation by re-read**. The
  planning records each learned read it makes at the choke points (five synchronized OFS methods reached from
  `LmdbMappedFrontierLearningSession`, which already pins each decision per key) as (read kind, key, inputs,
  outcome as consumed by costing). When the global revision has moved, the entry is valid iff re-executing the
  recorded reads against the current state, atomically under the feedback monitor and without side effects, yields
  the same outcomes. This is the standard argument for a memoised deterministic computation: if every recorded read
  returns what it returned, the planning would make the same reads in the same order and produce the same plan; it
  needs the planning to be `reproducible()`, which publication already requires. It is exact for decay, calibration,
  sibling scans, evictions and absent-versus-empty cells without having to model them. A validated entry is
  re-stamped with the revision it was validated at, so the next lookup takes the fast path.
  Consequences to design: the reads that mutate (the lifecycle decision admits entries) need a side-effect-free
  twin; exact facts and lifecycle decisions must be pinned per planning like the model decisions; a planning whose
  revision moved while it ran can be validated the same way at publication instead of being refused; reads that are
  not recorded (the legacy `estimate`, filter selectivity with its planning-time live sampling) make an entry
  fall back to exact-revision validity unless they are shown to have returned nothing; the cost of validation
  (some reads scan all keys) must be measured against the planning it saves, and held under one monitor
  acquisition.
  Open until measured: how often the filter-selectivity component of the revision moves in the default
  configuration. If often, filter reads have to join the read log for step 2 to pay off.
- **Step 3 becomes a comparison rule inside the re-read**: same gate class and same null/non-null, numeric outcomes
  within the tolerance of the build-time outcome in log space, exact facts and lifecycle state equal. Outcomes are
  per (read, input), not per cell.
- Not read by any production planning, so not part of validity: typed and legacy semi/anti counts, plan
  candidates, shadow and multiplier state, LEO surface stats, rule steering.

## Sizing measurement (2026-11-10, `measure-validity/summary.txt`; machine load 37–100, counts not timings)
- **Step 1 alone does little on this workload.** A completed evaluation publishes about 8 times; 55–71 % of the
  publications change nothing, but at least one admits a model observation in 94–98 % of evaluations. The feedback
  component would still move after almost every completed query. (It does help shapes whose only learned output is
  an exact fact; see the step-1 store test.)
- **The filter component moves in about 18 % of evaluations in steady state** (runtime filter feedback 27–28 and
  prepared outcomes 5 per 182; live and background sampling only in the first round). An entry that falls back to
  exact-revision validity whenever that component moved would not survive a rotation, so **filter reads have to be in
  the read log** (median 2, p90 12, max 27 per planning).
- **Almost no model decision returns an estimate**: 875 of 916,396 (0 of about 1.58 million physical decisions in the
  logical population, in any run). The global revision moves constantly while the outcome costing consumes is "no
  estimate" almost everywhere. This is why validation by outcome is the right granularity, and it raises a separate
  question (why physical learning never yields an estimate; under investigation).
- **Read log**: median 466 model decisions per planning (p90 1,949, max 8,928), 19 exact-fact reads, 36 lifecycle
  decisions. It needs a compact encoding (group the dimensions of one key).
- **Re-read cost** is a median 14 % of the planning (p90 33 %) because every decision scans a whole key map for a
  boolean that only selects a gate label. Fix first: `GH-0000-flm-applicability-index-2026-11-10.md`. That is also a
  planning-time saving of the same size, independent of caching.
- **What a re-read would have validated** (comparison of consecutive plannings of the same query, caches off): all
  reads identical in 27–54 % of plannings in rotation and 40–69 % for the same query repeated; no plan change in
  any of them. Every plan change (27) falls in plannings whose read set differs. Numeric-only differences are rare
  (0–3 per round), so **step 3's tolerance may add little**; to be decided by the direct replay measurement.
- **Planning is not a function of the recorded reads alone.** 33 of 182 queries carry parser-generated variable names
  that change on every parse and are embedded in physical and event learning keys, so each preparation creates cells
  nothing reads again and the physical map hits its 4,096 cap by round 4 (a defect in its own right; under
  investigation). A further 152 comparisons (rotation) differ in read set with no attributed cause and carry 14 of the
  19 plan changes; budget cuts under load are suspected, not shown.
- **Next**: a direct measurement of the validation decision (replay planning N's log before planning N+1, then
  compare plans), with the planner's own reproducibility flag and a no-feedback control, before the step-2 spec.

## Direct measurement of the validation decision (2026-11-11, `measure-validity/summary-replay.txt`)
Replay of the previous planning's recorded reads before the next planning, then comparison of the two plans; caches
off; theme workload; one JVM at a time at load 21–26 (median).
- **No wrong serve in any schedule**, exact or with tolerance up to ln 1.5.
- **Served** (lifecycle `regressionLimit` excluded): rotation 281 of 728 (39 %), same query repeated 299 of 544
  (55 %), no feedback in between 1,134 of 1,456 (78 %). Parse-stable queries 41 % and 58 %, parser-random 29 % and
  40 %.
- **`regressionLimit` must not be compared.** `PlanLifecycleStore.decision` derives it from the siblings' `planned`
  upper bounds, and each decision in a planning overwrites its own `planned`, so the value depends on call order: a
  planning's own log replays to a different limit in 681 of 910 plannings. Plan choice does not consume it in the
  default rollout. Comparing it would halve the hit rate for nothing. (That the contract carries an order-dependent
  limit is a lifecycle defect to fix separately.)
- **Gate labels without an estimate must not be compared** either: 1,039 reads flipped between
  INSUFFICIENT_SUPPORT and INAPPLICABLE with no feedback at all (cells leased by the previous precompilation).
- **Tolerance (step 3)**: +6 (rotation) and +34 (repeat) serves at ln 1.25, +26 and +38 at ln 1.5, all correct; with
  gate labels ignored rotation reaches 345 at ln 1.5 with no wrong serve and 389 at ln 2 with 2 wrong (largest
  differences 0.598 and 0.596). Decision: ln 1.25, a system property, compared against the build-time outcome.
- **Planning is a function of the recorded reads.** Every divergence between consecutive plannings is preceded by a
  changed recorded read (logical value or presence 161 and 65, an exact fact becoming current 47 and 19, a learned
  filter surface 43 and 25, a cached sample that the previous planning itself wrote 27 and 38), by a 2 ms sampling
  cut on one side (28 and 17), or by a parser-random name (95 and 69). None has an identical prefix.
- **The cached-sample artefact**: a planning that takes a live sample caches it, so the next planning reads the same
  value from the cache through a different read. Record the pair as one read (sampling key → value, whatever the
  source) and replay it as the pure cached read.
- **Not reproducible**: 3.5 % of plannings, all from the 2 ms live-sample cap, almost all in the first round;
  already excluded from publication by `reproducible()`.
- **Not replayable without side effects**: live sample, exact-zero proof, distinct values. All three are functions
  of the data at an unchanged data revision; they need no validation beyond the data revision and reproducibility.
- **Cost**: replay is 21.7 % of a planning, 82–86 % of it the whole-map scans (now removed); filter reads are
  0.1–0.2 % of the replay.

## Alternatives considered
- Stale-serve with detached refresh for replay-only entries (treat them like ranked ones). Small change, fixes
  request latency, but every completed evaluation still triggers a background planning, so CPU is not saved, and a
  plan the feedback has just shown to be bad is served once more. Could be layered on step 2 as the optional
  refresh in step 3.
- The tier-dominance rule (more decisions become ranked). Changes selected plans (17 when measured), and ranked
  entries are themselves served stale after every execution; it does not address invalidation.
- A larger cache alone. Raises the round-robin result from 0 to 30 of 182 hits per round; the rest is invalidation.

## Also needed, independent of the above
- Cache share of the evidence budget: at about 300 KB per family the theme workload needs about 55 MB to hold every
  query. Decide after validity is fixed; shrinking entries further is the other lever.
- Benchmarks do not exercise any of this: the benchmark configuration (native evaluation on) publishes feedback for
  10 of 182 evaluations, and the benchmark preparation path keys its plans as query evaluation mode STANDARD with
  `read.planes=1`, where the public API uses STRICT and `read.planes=3` (with the API default `includeInferred`).
  A cache measurement must use the public API and the store default configuration.
- A quiet-machine measurement of how often the 2 ms sampling cap cuts a first planning.

## Possible defect seen in passing
One logical exact-cardinality key flips between 1 and 45,337 rows depending on whether LIBRARY q10 or q11 executed
last. Exact facts are trusted as certain, so either two different expressions share a logical digest or the fact is
recorded under the wrong scope. Under separate investigation.
