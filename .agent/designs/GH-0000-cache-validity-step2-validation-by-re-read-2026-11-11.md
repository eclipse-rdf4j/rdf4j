# Cache validity, steps 2 and 3 — a cached plan stays valid while its planning's learned reads still answer the same (spec, 2026-11-11)

Parent design: `GH-0000-cache-validity-under-execution-feedback-2026-11-10.md` (owner approved all three steps; read
its sections "Revision after the read/write inventory", "Sizing measurement" and "Direct measurement of the
validation decision" first). Step 1 is merged (`GH-0000-cache-validity-step1-truthful-planning-revision-2026-11-10.md`).
Maps: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/design-validity/cache-touchpoints.md` (where each cache
uses the LEO revision; file:line throughout) and
`.agent/research/GH-0000-phase1-continuation-2026-11-09/learned-state-read-write-inventory.md`.
A measurement already recorded and replayed these reads in a private tree; its instrumentation is a worked example
of every read site, of a side-effect-free lifecycle peek and of pure filter re-reads, not a design:
`/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/measure-validity/instrumentation-replay.diff`, results in
`summary-replay.txt` beside it.
OFS = `LmdbOperatorFeedbackStats`, FLM = `FrontierLearningModel`, PLS = `PlanLifecycleStore`,
FSS = `LmdbFilterSelectivityStats`, LFS = `LmdbLearnedFilterSelectivity`, ER = `LmdbEstimatorRuntime`,
PPC = `LmdbPipelinePlanCache`, PDC = `LmdbPlanDecisionCache`, HFPC = `LmdbHeuristicFilterPassCache`,
DPR = `LmdbDetachedPlanRefresh`, WCC = `LmdbPlanningWallClockCuts`.

## Principle
A reproducible planning is a deterministic function of the query, the data and statistics it read (versioned by the
data revision, the Frontier revision and the provider generation) and the learned state it read. If every learned
read the planning made returns today what it returned then, a fresh planning would make the same reads and choose
the same plan (measured: no divergence between consecutive plannings without a changed recorded read, a sampling
cut or a parser-random name; 0 wrong serves in 2,728 comparisons). So an entry whose only stale evidence component
is learned state is valid iff its recorded learned reads replay to the same outcomes.

Soundness is mandatory: a served entry's plan is the plan a fresh reproducible planning would choose, up to the
tolerance of step 3. Anything this change cannot prove falls back to today's rule (identical evidence), never to
"assume valid".

## Scope
- Validation lives in the pipeline cache and its decision cache (PPC → PDC): the only layer whose hit covers every
  optimizer stage. The packed plan cache (core) and HFPC keep the LEO revision as identity, unchanged.
- No change to plan choice, to what is learned, to sidecar formats, or to the public API.
- Two phases, delivered and reviewed separately. Phase A changes no cache behaviour.

## Phase A — record and replay

### A1. The read log
A planning-scoped, thread-local log beside WCC (inner scopes join the outermost; every publisher already brackets
on WCC: PPC, DPR, HFPC, the uncached path). Recording happens **at the owners of the state**, not at their callers,
so no reader can bypass it: the OFS planning read methods (`logicalDimensionDecision`, both
`physicalDimensionDecision`, `exactCardinalityFact`, `planLifecycleDecision`, legacy `estimate`), the FSS read
methods that return learned or cached-sample state, and LFS. Outside a planning scope recording is a no-op.

A read is **learned** iff its result can change while the data revision, the Frontier revision and the provider
generation are unchanged. Classify every public and package-visible read method of OFS, FSS and LFS in the
deliverable table as one of:
- **replayable**: recorded as (kind, key, the inputs the outcome depends on, the outcome as the planner consumes
  it) and re-executable without any side effect;
- **version-guarded**: not recorded individually; the log records that the planning used it and the owner exposes a
  truthful version of exactly that state, which must be equal at validation. Use this for the legacy operator
  estimate (`OFS.estimate`; add `legacyPlanningVersion()` = the clock-coupled and legacy components of step 1's sum)
  and for LFS (its `planningRevision()`), both absent or empty in the default configuration;
- **data-derived**: a function of the data at the current data revision whose only state is a cache of that function
  (exact-zero proof, distinct values, cold synopsis if it reads no learned state). Not recorded;
- **not reachable from planning**: say why.
Anything you cannot place makes the log **incomplete** (a flag with a reason), and an incomplete log is never used
for validation.

Outcomes, as consumed:
- model decision: whether an estimate is returned; if so `corrected`, `lower`, `upper`. The gate label is not part
  of the outcome (it flips without feedback and only feeds an annotation).
- exact fact: the rows, or absent.
- lifecycle decision: exactly the fields that can change plan choice or the executed plan, excluding
  `regressionLimit` (it depends on the order of decisions inside one planning: a planning's own log replays to a
  different limit in 681 of 910 plannings) and excluding text that only feeds annotations. Read `applyLifecycle`
  and list them. Replay uses a side-effect-free peek in PLS (no admission, no eviction, no `planned` overwrite, no
  group creation, no transition; an absent entry answers as a freshly admitted one, a STALE one as it would after
  the transition).
- filter reads: the returned value at the granularity the planner consumes. Where a continuous value only feeds a
  threshold test (a confidence compared with a constant), the outcome is the result of the test.
- sampling: a planning that takes a live sample caches it, so the next planning reads the same value through the
  cached-sample read. Record the pair as **one** read (sampling key → value, whatever the source; "none" when no
  usable sample exists) and replay it as the pure cached-sample read. A cached sample that is gone replays as
  changed unless the recorded outcome was "none".

Size. Group reads by key so that the nine dimensions of one physical cell share one key reference, and store
predicted inputs and values only for reads that returned an estimate (875 of 916,396 in the measurement; a read
without an estimate replays as "still no estimate", which does not depend on `predicted`). Target: a retained log
of at most 48 KB for the median theme query; report the distribution. A log above a cap (default 4,096 key groups;
system property) is dropped and the planning counts as having an incomplete log.

Splicing. HFPC returns a cached `FilterPassEstimate` without making the reads. Store the log slice of the
computation with the HFPC entry and append it on a hit. A packed-plan-cache hit skips the cost session: mark the log
incomplete (LMDB can see the hit through the Cascades plan metrics).

### A2. Replay
`replay(log, tolerance)` returns identical / within tolerance / changed (with the first changed read's kind and
index) / incomplete. It stops at the first changed read. It takes no lock for longer than one read: consistency
comes from versions. Capture the component revisions (below), re-execute the reads, capture again; if a component
moved in between, the result is "changed during replay" and the caller treats it as not validated. Step 1 makes
this exact for OFS; state in the report whether FSS's `planningRevision()` is truthful enough for the same argument
(every change to a replayable FSS read advances it; an advance without a change only costs a retry) and fix what
is missing on the soundness side.

Comparison (step 3):
- discrete parts equal: presence of an estimate, exact-fact presence and rows, lifecycle fields, sample presence,
  threshold-test results, version guards;
- model values within the tolerance in `ln(1 + v)` space (the metric the measurement validated), each of
  `corrected`, `lower`, `upper`, against the **recorded** value (so drift accumulates and eventually invalidates);
- filter pass ratios within the tolerance in plain log space (`|ln a − ln b|`); zero equals only zero;
- tolerance from a system property, default `ln 1.25`; `0` means bit-exact. No wrong serve was measured up to
  `ln 1.5`; two appeared at `ln 2`.

### A3. Revision components
`ER.capturePlanningRevisions()` additionally carries the four inputs of the LEO hash separately (OFS, FSS, LFS or
absent, provider generation). `leoRevision` and `learnedEvidenceRevision` keep their values and meaning. "Only
learned state differs" between two captures means: data revision, Frontier revision, provider generation, predicate
range version and `adaptiveEvidenceAllowed` equal, and the LEO revision different.

### A tests (store-level through the public API unless noted; each red-capable test red first)
1. Self-replay: for a set of queries covering a leaf, a join, a FILTER with a learned surface, OPTIONAL, a query
   that live-samples, a HAVING query and a property path, the log of a planning replays as identical immediately
   after that planning.
2. Purity: a replay changes nothing: OFS `planningRevision()`, observation clock, FLM key count and cell
   observation counts, PLS `revision()` and `decisionInputVersion()`, FSS revision and its cache sizes, HFPC size
   are equal before and after; no sampler ran.
3. Each kind of change is detected, and nothing else is: a model estimate appears; a model value moves beyond the
   tolerance; within it (served, and bit-exact mode refuses it); an exact fact becomes current; its rows change; a
   lifecycle field in the compared set changes; `regressionLimit` alone changes (identical); a gate label alone
   changes (identical); a learned filter surface appears; a pass ratio moves beyond and within the tolerance; a
   cached sample disappears; the planning's own live sample is cached (identical); legacy estimate state changes
   while the planning used the legacy estimate (changed) and while it did not (identical); cumulative drift in
   steps each within the tolerance ends as changed.
4. Completeness: an HFPC hit carries its slice (a second planning at the same revision replays the same reads as
   the first); a packed-cache hit yields an incomplete log; a log over the cap is incomplete.
5. Concurrency: a publication that changes a recorded read between the two captures of a replay yields "changed
   during replay", never "identical" (drive it deterministically with an existing observer hook or a test-only
   seam on the validator, not with sleeps).
6. Unit tests for the grouped encoding (round trip of every read kind; byte accounting equals the charged bytes).
One-line mutations: each comparison rule and each recording site has a test that turns red without it.

## Phase A — review round (2026-11-11)
Independent review verdict: phase A changes no behaviour beyond the two declared items, but a complete log that
replays as identical does not yet imply "a fresh planning makes the same reads". Evidence:
`/private/tmp/claude-501/-Users-havardottestad--claude-spares-spare-5bPeTt/157f627d-35d8-4517-9d49-1f48286fcd13/scratchpad/review-readlog-evidence/NOTES.md`.
Decisions (each fix test-first; a test that cannot be made red is reported as such):

- **R1 Detached refresh served by the packed cache.** `LmdbDetachedPlanRefresh` opens the planning scope and calls
  the packed planner's refresh, which can return an exact, family or waiter hit. Mark the log incomplete there when
  the refreshed plan's metrics report a cache hit, as `LmdbCascadesOptimizer` does. Then prove by reading (and say
  so) that every route that returns a packed result without running the cost session marks the log incomplete,
  including packed checkpoint resume: either show that a resumed search re-reads through the owners everything
  behind the memo it restores, or mark the log incomplete on resume.
- **R2 Lifecycle state behind the two captures.** A lease release or resolve changes whether an absent entry gets
  the capacity-protected answer without moving any version, and planning-time admissions only set `dirty`. The
  planning revision deliberately excludes these (a planning's own admission must not invalidate it). So the
  **replay's** capture, not the planning revision, gains a lifecycle component: PLS exposes a version that moves on
  every change to anything `peekDecision` reads (entries, group cells, `planned`, states, lease counts); OFS exposes
  it; `Log.replay` captures it beside the planning revisions before and after. A concurrent planning's admission
  then costs a "changed during replay", never a wrong "identical". Record as a known limitation (not fixed here):
  under lifecycle enforcement the exact-revision caches (packed, HFPC, pipeline at equal revision) have the same
  blind spot today.
- **R3 Tolerance and consumers that threshold.** `LmdbMappedFrontierLearningSession.correctionIntersectsMappedBounds`
  decides whether a learned row count replaces the raw estimate; a move inside the tolerance can flip it. The mapped
  session reports that consumer check to the log, attached to the model read it tested: the structural bounds it
  compared against and the boolean result. Replay recomputes the same (shared, static) predicate from the replayed
  estimate and the recorded bounds; a different boolean is CHANGED at every tolerance. Search the consumers of the
  three model decisions for any other test of this kind (a comparison of `corrected`, `lower`, `upper` or the
  variance against a bound that switches between two estimates rather than scaling one) and treat each the same
  way; list them. Lifecycle: the envelope a fresh planning would pass is rebuilt from the model values, so when
  `planLifecycleEnforced()` the effective tolerance is 0 (WITHIN_TOLERANCE is reported as CHANGED); with enforcement
  off nothing consumes the difference.
- **R4 Low-confidence samples.** `LmdbFilterPassSelection` re-samples whenever the cached sample's confidence is
  below its threshold, so such a planning is not a function of the cached state. A recorded sample outcome whose
  confidence is below that threshold (use the same constant) makes the log incomplete. A recorded "none" stays
  replayable only when the planning took no wall-clock cut (it is unpublishable otherwise); state where that is
  enforced.
- **R5 Self-contained HFPC slices.** Inside `recordSlice` the per-scope estimate caches must not answer from
  entries computed before the mark (bypass reads of those caches for the duration, or give their entries the slice
  of their own computation and splice on a hit; choose the smaller change and say which). Test: an estimate
  computed earlier in the same optimization scope, then the HFPC computation; the stored slice contains the reads.
- **R6 No strong references to providers.** A log (and so an HFPC entry or, later, a plan variant) must not keep a
  superseded provider alive: hold the three owners weakly, and let guard reads use the log's owner instead of a
  key slot. A cleared reference replays as CHANGED (provider replaced).
- **R7 Provider identity check order.** In `Log.replay(runtime, …)` capture first, then compare provider identity,
  then re-read, then capture again, so a provider swap at any point yields CHANGED or "changed during replay".
- **R8 Recorder details.** Intern the detached copy of a dictionary-owned pattern key per recorder so repeated
  reads share one group; state the read cap as what it is (a total of groups × 16) or make it per group; add the
  test that a 1-ulp change at tolerance 0 is CHANGED (the bit-exact guard is not an equivalent mutant: `log1p`
  maps adjacent doubles to one value).
- **R9 LFS.** Establish by reading whether `LmdbLearnedFilterSelectivity.planningRevision` can be advanced outside
  its monitor; fix if so (it is the version the guard relies on).

Result of the round (implementer hand-back, `impl-readlog/review-round-red-green-mutation.txt`): R1–R8 done, R9
needed no change; 79 tests; R5 implemented as "recheck" (an older per-scope entry is recomputed for its reads while
a slice is open; the cached value is still returned; a mismatch or an entry without value equality marks the log
incomplete). Two mutation sites are covered by reading only (price admission, semantic-estimate recheck).

Second review (snapshot of 08:47, merged into the working tree 2026-11-11 as a change without consumers): all
round-1 findings closed; the R5 hole was real (a retained filter-pass slice complete but missing its legacy guard)
and is closed; plans identical with and without the recheck. Carried into the next round:
- **R10 A "none" sample from a randomised draw.** A zero-hit reservoir draw below the full sample size returns
  "none" at random, so a fresh planning can return a value. "None" stays replayable only when it is deterministic
  (the whole population was scanned, or there was no budget to sample); otherwise the log is incomplete.
- **R11 The R5 recheck must not sample.** A recheck recomputes the estimate path, which by reading can reach the
  live sampler (cache write, revision bump, background vote, wall-clock cut). Suppress foreground sampling during
  a recheck; if the recomputation would need one, mark the log incomplete.
- **R12** Kill the `r5-engine-site` mutation with the reviewer's fixture (AUTHORITATIVE, live sampling off,
  `?s ex:name ?n FILTER(STRSTARTS(?n,"Person 1")) FILTER(STRENDS(?n,"7"))`: every retained filter-pass slice
  carries its legacy guard); cover `r3-price-site` with a unit fixture on the mapped session.
- **R13** Slice stamps come from one global counter, not a per-recorder one.
- For phase B: the posterior variance also feeds the objective interval (lifecycle application → metadata arena →
  decision-summary cost intervals → certificates and refresh intervals). A revalidated entry keeps its recorded
  intervals; decide there whether a within-tolerance serve may reuse them for ranked and refresh decisions.

## Phase A2 — compact witnesses for reads that found nothing (2026-11-11)

Measurement (`impl-readlog/`, 182 theme plannings): a published variant is charged 230–310 KB (median); its log
adds 202–217 KB beside it (median; p90 0.7–1.9 MB), counting only bytes the variant does not already hold. 67.4 %
of key groups are all-absent (57 % of the bytes), 31.9 % are lifecycle groups, 0.7 % have a present outcome. 62 %
of standalone key bytes are keys equal to an earlier, different key object of the same log. Of 185,274 model reads
without an estimate, 99.9 % had no exact evidence in that dimension, 0.1 % had one or two observations, none had
three or more. By reading (`FrontierLearningModel` 606–677, 773–812): a model decision returns an estimate only if
the exact cell of its posterior key has at least three observations in that dimension; no other cell, calibration,
pooled or legacy posterior can produce one.

So a read that found nothing does not need its key objects, only a way to ask the owner "is there still nothing
under this key". Targets on the same workload: incremental log bytes at most 48 KB median and at most a quarter of
the variant's charge at p90; recording at most 2 % of planning time; replay no slower than now.

Rules:
1. **Model reads.** The owner tells the recorder, with each read, whether the exact cell of the read's posterior
   key (the key the model actually looks up, after the source-rows normalisation of the residual) has fewer than
   the gate's minimum exact observations in that dimension. A key group all of whose reads are of that kind is
   stored as a 128-bit digest per distinct posterior key instead of its key objects. Replay asks the owner for the
   exact observation count under that digest and dimension: below the minimum is IDENTICAL, anything else CHANGED
   (conservative: a cell with enough observations that the gate still refuses is re-planned once and then recorded
   with full keys). The model keeps a digest index over its exact cells, maintained wherever cells are created,
   evicted, cleared or loaded. Compute a key's digest once (cache it on the immutable key object, or reuse a digest
   the key already carries); a collision can only make two keys share an answer in the conservative direction, so
   state the argument for the index's collision handling.
   The premise is pinned by a test: over randomised model states, every decision that returns an estimate has at
   least the minimum exact observations in that dimension under the same posterior key. A change that introduces
   pooling must turn it red.
2. **Exact facts.** A read without a current fact is stored as the digest of the fact's key; replay asks the owner
   whether a fact under that digest is current now, by the same currency rule as the read (a stale fact whose
   stamp matches again is current).
3. **Lifecycle.** Establish what `PlanLifecycleStore` actually keys entries and group cells by. If those identities
   are already compact (digests), record them instead of the learning keys; otherwise give the store a digest
   index and a peek by digest. The compared fields and the peek's semantics do not change.
4. **Everything that keeps key objects** (groups with a present outcome, refused reads with enough observations,
   filter reads) interns them by equality within the recorder, so one planning holds each distinct key once.
5. A switch (system property, default compact) records full keys instead; it exists for the differential test and
   for diagnosis.

Tests (red first where possible): the premise test of rule 1; a differential test that drives randomised sequences
of owner mutations (observations, evictions, fact changes, lifecycle transitions, leases, store mutation) against
a compact and a full log of the same planning and requires `verdict(compact)` to equal `verdict(full)` or to be
CHANGED where the full log says IDENTICAL or WITHIN_TOLERANCE, never the reverse; each index maintenance site has
a one-line mutation that turns a test red; purity of the digest probes; byte accounting equals the charged bytes;
the existing 79 tests stay green in both modes. Measurement: the incremental-bytes table again, recording and
replay time, on the theme workload.

Left for phase B: interning present keys against the published variant's identities, and charging the log in the
decision cache.

### A2 hand-back and decisions (2026-11-11, private copy `trees/readlog`, not merged)

Implemented: rules 1, 2, 4, 5 and R10–R14 (`impl-readlog/a2-*.txt`, `changes-since-merge.diff`, 14 files). Observed
on the theme workload: incremental log bytes 201.8 KB → 39.5 KB median (target 48 KB met), p90 of incremental over
the variant's charge 2.18 → 0.294 (target 0.25 missed), recording 0.22 % of planning time, replay within noise of
full keys. Rule 4 alone gives 61.5 KB. Soundness of rules 1 and 2 does not rest on collision probability: every
cell is listed under its key's digest, so a collision only adds cells and can only turn IDENTICAL into CHANGED.
Rule 3 as written was not implemented, correctly: a digest peek of lifecycle state can be flipped either way by a
colliding entry. Lifecycle-group keys are now 95 % of the remaining incremental key bytes.

Decisions:
- **Rule 3 replaced (3′): a lifecycle read is state-free while lifecycle is not enforced.** By reading: with
  `enforce == false`, `PlanLifecycleStore.decision` and `peekDecision` return `eligible = true` and
  `comparisonCost = envelope.point()` in every branch (capacity-protected, snapshot-only, monitoring), and
  `PackedMemo` consults eligible, last-good and the comparison cost only behind
  `physicalMetadata.lifecycleEnforced(...)` (`lifecycleExcluded`, `lifecycleComparisonCost`, `verifiedLastGood`,
  `robustComparisonPrimary`); `PackedDependentSubqueryCosting` likewise behind `ownerEnforced`. So a lifecycle read
  made while the rollout does not enforce is recorded as one keyless guard for the whole log ("lifecycle not
  enforced"); replay answers CHANGED if `planLifecycleEnforced()` is true now, IDENTICAL otherwise. A read made under
  enforcement keeps its keys and the exact comparison as today. What a served plan may then carry stale is the
  `optimizer.lifecycleLastGood` planned metric, in the same class as the state, reason, last-good digest and
  regression limit the replay already does not compare. The premise is pinned by tests: over randomised store
  states the non-enforced decision has `eligible` and bit-equal `comparisonCost == envelope.point()`; and a memo
  test that two estimates differing only in the last-good and eligible metrics with enforcement off produce the
  same winner. A change that makes monitoring consult lifecycle state must turn one of them red.
- **R14 extended to learned surfaces.** `LEARNED_SURFACE` ratios reach the same unguarded threshold
  (`learnedSurfaceFilterPass` → `prefer()` → `isSelectiveSmallLiteralFilterAnchor`, ratio ≤ 0.75), so they compare
  bit-exact as well. Their evidence count already compares exactly, so this costs a re-plan only when a ratio moves
  at an unchanged count.
- **p90 target** is re-measured after 3′; with phase B's "a log never costs a family" rule the ratio bounds how
  many families keep a log, not how many families fit.
- The intermittent prerequisite failure of `aLearnedLogicalEstimateRecordsTheMappedBoundsCheckAndReplaysIt`
  (predates A2) is root-caused in the same round; no sleep, no retry, no muting.

### A2 independent review (2026-11-11, `review-a2-evidence/NOTES.md`)

Verdict: rules 1, 2, 4, 5 and R10, R11, R13 mergeable; R14 after the learned-surface fix (in round 2). Probes: 1,404
compact serves in 15 eight-round profile runs and 661 per forty-round run (365 IDENTICAL, 296 WITHIN_TOLERANCE)
compared with an independent fresh planning, 0 different plans; rule 1 premise 0 violations in 193,208 checks;
6,000 equal event-key pairs built differently, 0 digest differences; compact vs full keys disagree only towards
CHANGED (0–12 of 140 per run, all the anticipated "three observations but calibration below 32" case).

Decisions on its findings:
- **What WITHIN_TOLERANCE claims (restated; B0 amended; owner confirmed 2026-11-11: keep the tolerance and
  measure).** The reviewer found, by reading, a discrete rule fed by
  a tolerance-compared value: `PackedJoinEnumerator.hashJoinCost` prices a hash join at +∞ unless
  `leftRows + rightRows >= 256`, and those rows carry the learned corrected row count. A correction moving inside
  the tolerance across that sum flips whether a fresh planning admits the hash join. The core planner has more
  such rules than a guard per site can enumerate, and exact model values would remove every within-tolerance serve
  (296 of 661 in the probe). So the claim is: IDENTICAL means the decision a fresh reproducible planning would
  make, and a difference is a bug; WITHIN_TOLERANCE means the plan a fresh planning made at a learned state in
  which every value read is within the tolerance of the current one, with every guarded switch (bounds check,
  price admission, ratios, samples, counts, presence) unchanged. A fresh planning now may differ where an
  unguarded discrete rule sits between the two states. That is the price of step 3 and it is set by measurement,
  not assumed: the audit reports identical-verdict mismatches (must be zero) separately from within-tolerance
  differences, and the B measurement reports the rate of the latter and, for each, the fresh planning's price of
  both plans. A material rate lowers the tolerance or adds a guard at the rule that caused it. Within-tolerance
  serves stay limited to replay-only variants, and the log keeps its recorded values, so drift does not
  accumulate across re-stamps.
- **Known unguarded discrete consumers of tolerance-compared values** (kept as a list here; extend when found):
  the hash-join input-row floor above. To trace in round 2: `LmdbFilterSelectivityStats` sampling-budget floor
  (~:2369) and `LmdbEstimationEngine` ~:370.
- **Frontier publication between a planning and its replay.** Exact-fact keys carry the Frontier revision, so a
  log recorded before a Frontier publication can replay IDENTICAL while a fresh planning reads new-revision keys.
  Phase B never serves it (the stamped evidence and the request differ in Frontier revision, which is not a
  learned-state-only difference); a phase B test pins exactly this with a log that does replay IDENTICAL.
- Owner-side cost: the digest indexes are maintained for every cell whether or not a recorder exists (60–90 bytes
  per cell, under 1 MB in all); accepted. Event-cell digests share one memo during a sidecar load (round 2).
- Not adopted: a second witness for "calibration below 32" (precision only).

### A2 round 2 hand-back (2026-11-11, `impl-readlog/a2-round2-*.txt`, private copy, merge pending)

Rule 3′, learned-surface exactness, the per-load digest memo and the two traces are done; 135 read-log tests green
in both modes, 11 of 11 mutations killed, core `PackedMemoContractTest` 62 green.

| theme workload | incremental median | p90 | incremental / charge p90 | recording share |
|---|---|---|---|---|
| phase A (merged) | 201.8 KB | 733 KB | 2.18 | — |
| A2 compact | 39.5 KB | 128.2 KB | 0.294 | 0.22 % |
| round 2 compact | 3.4 KB | 9.2 KB | 0.028 | 0.38 % |

Both targets met; replay median 0.09 ms (1.09 ms with full keys). Measured under the default (monitoring)
rollout only: an enforcing rollout keeps lifecycle keys and was not measured.

- Rule 3′ premise re-verified by the implementer and pinned (randomised store states; owner test for withheld
  evidence; a core memo test, 128 trials, that the three lifecycle metrics change neither winner nor comparison
  cost with enforcement off). `lastGood` is not constant under monitoring; the differential test allows exactly
  two divergences: compact CHANGED at the guard after enforcement turns on, and full CHANGED where only a
  monitoring `lastGood` flipped (the accepted stale metric). Phase B's audit signature does not include it.
- Publication path, not a planning read: `LmdbOperatorFeedbackStats.lifecycleBlocked` treats a BLOCKED or
  QUARANTINED entry as a block whatever the enforcement flag, and that reaches the decision cache's quality and
  demotion logic. Recorded; unchanged by this work.
- Traces: the sampling-budget floor (`LmdbFilterSelectivityStats` ~:2369) and the SERVICE floor / GROUP collapse
  (`LmdbEstimationEngine` ~:339–370) take no tolerance-compared value. Untraced and added to the list of possible
  discrete consumers: the engine's invocation count comes from the core planner's outer prefix rows
  (`LmdbEstimateContextBuilder` ~:93); if those carry a learned corrected row count, the
  `invocationCount() > 1.0` rules in `LmdbFactorCostAssembler` (~:118, :271) are of the hash-join-floor class.
- **Defect found by the flaky-fixture RCA (production, not fixed here):** a Frontier rebuild publishes with
  `coveredEpoch` = its snapshot epoch and then acknowledges coverage with an LMDB write of its own
  (`LmdbSailStore.rebuildFrontierStatisticsV2` → `acknowledgeFrontierCoverage`), which advances the epoch at an
  unchanged statement sequence. `frontierSnapshotAlreadyCovered` demands epoch equality, so a rebuild already
  scheduled by the load's commit rebuilds everything once more and publishes another generation 1–4 s later (7 of
  8 trials). Consequences: a wasted full rebuild after every synchronous rebuild with a pending request, a second
  Frontier revision move (every cached decision and learning applicability re-keyed), and the intermittent
  `aLearnedLogicalEstimateRecordsTheMappedBoundsCheckAndReplaysIt` (its training splits across the two
  applicabilities). Fix candidates: coverage decided by the statement sequence when only metadata moved the epoch;
  or the synchronous rebuild consumes the pending scheduled request. Separate Routine A change, to be specified.

## Phase B — the pipeline cache serves an entry whose log still replays (spec, 2026-11-11)

Prerequisites: phase A merged; phase A2 (compact witnesses) and R10–R13 merged. File:line references for every
site named here are in the touchpoints map (sections 3–7 and 9). PPC = `LmdbPipelinePlanCache`, PDC =
`LmdbPlanDecisionCache`, DPR = `LmdbDetachedPlanRefresh`.

### B0. What is claimed
For a request at captured planning revisions R and a cached variant V stamped with evidence E: if E and R differ
only in learned state (`onlyLearnedStateDiffers`: data revision, Frontier revision, provider generation, predicate
range version and `adaptiveEvidenceAllowed` equal), V's log is complete, and the log replays with both replay
captures equal to R, then V is the decision a fresh reproducible planning at R would make (IDENTICAL), or one
within the tolerance of it (WITHIN_TOLERANCE). Everything else keeps today's rule. Nothing in this phase may make
an entry servable that is not servable today for a reason other than a learned-state difference.

### B1. The variant carries its log
- `PlanVariant` gains the log (null when the planning's log is incomplete or over the retention cap below). Every
  constructor path that builds a variant from a planning passes it: build publication, refresh publication.
- Bytes: the log's incremental bytes are charged through the same identity-deduplicating account as the variant's
  provider identities, in `publishBuildLocked`, `variantRetainedBytes` and the refresh path, so the charged bytes
  equal a recomputation at any time (existing invariant; extend its test).
- Retention cap: a log whose incremental bytes exceed `rdf4j.lmdb.planCache.readLog.maxBytes` is not retained (the
  variant is stored without it and behaves as today). Default: set from the A2 measurement so that at least 90 % of
  theme logs are retained; report the value and the distribution.
- A variant never holds a provider strongly through its log (R6).

### B2. Lookup
Where `PDC.lookupVersion` computes `evidenceStable`: when the evidence is not stable, differs only in learned
state, and the current variant has a log, the lookup does not decide alone. Shape (names are the implementer's):
1. Under the existing locking, the lookup returns a "validate" outcome carrying the observed `FamilyVersion`.
2. PPC, holding no cache lock (not `admissionLock`, not a segment monitor), inside the request's planning read
   scope and provider view, replays the log through a validator the request supplies (PDC keeps no runtime
   reference). The replay's first and second captures must both equal R; otherwise the outcome is "not validated".
3. Outcomes:
   - IDENTICAL → re-stamp and serve `Use`, for replay-only and ranked variants alike, with no refresh scheduled.
   - WITHIN_TOLERANCE → replay-only variant: re-stamp and serve `Use`. Ranked variant: today's path for a
     learned-evidence move (stale serve with refresh), no re-stamp; its recorded cost intervals feed certificates
     and refresh decisions and are not re-derived here.
   - CHANGED, CHANGED_DURING_REPLAY, INCOMPLETE, or captures ≠ R → today's path, unchanged (miss for replay-only,
     stale serve with refresh for ranked).
4. Re-stamp = compare-and-set of `family.current()` from the observed version to an equal version whose variant
   carries evidence R; the version number, the variant's plan, recipe, refresh work, intervals and log are kept
   (precedent: `observeExecution`). If the CAS fails because a concurrent `observeExecution` replaced the object
   but version number, variant evidence object and log object are the ones validated, retry (bounded, 3); any
   other change → take today's path for this request. An in-flight refresh whose publication then fails its
   identity CAS as stale is accepted and counted.
5. After a re-stamp, a lookup at R is an ordinary exact hit. Whatever the served template reports as its evidence
   revision (the plan-cache evidence epoch stamped on a hit) must be R, not E; execution-feedback tokens issued
   before the re-stamp stay valid (same version number).
6. Reasons and counters: distinct reason strings for "revalidated" and "revalidated within tolerance", and
   counters for validated identical, validated within tolerance, changed (by first changed read kind), changed
   during replay, incomplete, no log, re-stamp CAS lost, refresh lost to a re-stamp, and replay nanoseconds;
   exposed where the cache's existing counters are.

Unchanged on purpose: the entry gate (a revision that moved between context capture and the gate still bypasses
the lookup), the waiter gate, the flight key (LEO stays in it), tombstones, `newerThan`/`mayReplace`, `servesAt`
for everything but the stamped evidence, the packed cache and the heuristic filter-pass cache.

### B3. Publication
The post-planning check refuses a planning whose revision moved while it ran. New: if the start and end captures
differ only in learned state, the planning's own log is complete, and it replays IDENTICAL at tolerance 0 with
both replay captures equal to the end capture C, the planning is published under C. (A planning's own live sample
moves the filter revision; its log records the sample pair as one read and replays identical.) Anything else is
refused as today. The same rule applies to a detached refresh's own publication check.

### B4. Detached refresh
The refresh thread opens the log scope (it already does through the wall-clock-cut scope), takes its log before
the scope ends, carries it in `RefreshComputation` and `RefreshPublication`, and the replacement variant stores
it. A refresh served by the packed cache has an incomplete log (R1) and stores none.

### B5. Switches
- `rdf4j.lmdb.planCache.readLog.validation` (default on): off → no log is retained on variants and lookups and
  publications behave exactly as before this phase (the rewritten tests' "before" behaviour is tested under it).
- `rdf4j.lmdb.planCache.readLog.audit` (default off): on a revalidated serve the request is also planned afresh,
  without publication, and the fresh plan is compared structurally (operators, join order, access paths, physical
  algorithms; not estimates or annotations) with the served one; mismatches are counted and logged at WARN with
  both plans' digests. It exists for tests and for the remeasurement and may cost what a planning costs.
- Tolerance: the phase A property, default `ln 1.25`.

### B tests (store level through the public API unless noted; each red first; one-line mutation per rule)
1. Unrelated learning: after query Q is cached, executing other queries that teach nothing Q read leaves Q's next
   lookup `Use` with the revalidated reason and the same plan; the variant's stamped evidence is now the request's.
2. Relevant learning: feedback that changes a read in Q's log gives today's outcome (replay-only: re-plan; ranked:
   stale serve with refresh), with the changed-read counter naming the kind.
3. Tolerance: a move inside it serves a replay-only variant and takes today's path for a ranked one; bit-exact
   mode refuses both; cumulative drift ends as changed.
4. Publication rescue: a first planning that live-samples is published (it is refused with the switch off) and
   the next lookup is an exact hit; a planning during which a recorded read really changed is still refused.
5. Incomplete logs (packed-cache hit, wall-clock cut, cap, low-confidence sample, non-deterministic "none"),
   a log over the retention cap, and a provider swap: exactly today's behaviour.
6. Re-stamp: version number kept; an execution token issued before it is still accepted; the served template
   reports the request's revision; a second lookup is an exact hit without replay.
7. Races, driven by hooks, not sleeps: `observeExecution` between observe and CAS (retry succeeds); a publication
   that changes a recorded read between the replay's captures (not validated); a refresh in flight across a
   re-stamp (its publication fails as stale, cache consistent, counter moves); two requests validating the same
   variant concurrently (one re-stamp, both served).
8. Memory: charged bytes equal a recomputation after publish, re-stamp, refresh and eviction; eviction order and
   capacity tests updated only for the added bytes, with before/after numbers.
9. Audit on a small mixed workload (leaf, join, filter, OPTIONAL, path, HAVING, EXISTS; several rounds with
   execution in between): zero structural mismatches, and a non-zero number of revalidated serves.
10. Switch off: the tests listed in the touchpoints map section 9 as "must be rewritten" pass in their old form
    under the switch, and in their rewritten form (learned-only move with an unchanged log serves; with a changed
    read misses) with it on. Tests at risk in section 9 are run and each change of outcome is explained.
11. Locking: a test or assertion that no cache lock is held during a replay.

### B measurement (after merge, by a separate run)
Through the public API with the store's default configuration and the audit switch on, on the theme workload:
rotation, same-query repetition and no-feedback pairs. Report hit rates before (switch off) and after, validated
serves by verdict, audit mismatches (must be zero), replay time per lookup, cache bytes and family counts, and how
much the 4 MiB capacity (about 25 families before logs) limits the rotation.

## Phase B — implementation hand-back (2026-11-11, private copy, not merged)

Implemented in `trees/phaseB` against the phase A tree (10 lmdb files; evidence in
`/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/impl-phaseB/`). 141 tests green three times, 29 one-line
mutations killed, audit on the mixed workload 22 revalidated serves with 0 structural mismatches, two-copy
comparison without a phase B outcome change. Independent review in progress; merge order is A2 first, then B
(B adds one accessor to the log, which A2's encoding change must absorb).

**B4 is withdrawn as specified (accepted).** A refreshed variant must not keep the refresh's log, for two reasons
the implementer established by reading:
1. `publishRefresh` keeps the recalibrated incumbent as champion unless the refresh's candidate beats it by at
   least 1 %. The refresh's log vouches for the candidate, not the incumbent.
2. The refresh plans the source as prepared by the original request's request-bound rewrites; their learned reads
   are in the original planning's log, not the refresh's.
So only build-published variants carry a log; a refreshed variant behaves as before this phase. The refresh still
uses its own log for its own publication check (B3), which widens nothing in kind: a refresh already vouched for
its start evidence without re-running the request-bound rewrites.

**Consequence to measure.** A ranked family loses revalidation at its first refresh and keeps today's behaviour
until a build republishes it. If the B measurement shows this dominates, the follow-on is a refreshed variant
whose basis is the original planning's request-bound slice (kept with the detached refresh work) joined with the
refresh's log, allowed only when the published champion is the refresh's own selection (promoted, or physically
equal to the incumbent). Not designed further until measured.

**Other deviations accepted:** the log is charged through the identity-deduplicating provider account at
publication and at every portfolio recomputation rather than in `variantRetainedBytes` (C4, invariant tested); a
re-stamp races a refresh publication only between the publication's read and its compare-and-set (C2); a refresh
at an unchanged LEO is served by the packed cache, so its log is incomplete and it cannot be rescued (C3); waiters
of a rescued planning still plan for themselves (C6).

## Phase B — review round (2026-11-11)

Independent review (notes: scratchpad `review-phaseB-evidence/NOTES.md`; scratch tests
`LmdbReviewPhaseB{Audit,Capacity,Race}ScratchTest` in `trees/review-phaseB`). Soundness probe: 20 query families,
10 rounds with execution feedback, 7 configurations, every revalidated serve compared with an independent fresh
planning on a signature stronger than the audit's: 385 revalidated serves (69 within tolerance), 0 mismatches.
Verdict: mergeable after the fixes below.

- **F1 (blocking) — a log never costs a family.** Measured at the default budget on a 24-query rotation without
  learning: 22 families / 44 of 96 `Use` with the switch off, 10 families / 14 of 96 with it on (~188 KB → ~388 KB
  per family). Rule: admission, OVERSIZED, NO_ROOM and family eviction are decided on the publication's bytes
  without its log, exactly as with the switch off. Logs live in the slack:
  1. before any family is evicted to make room (build, refresh, variant or alias growth), logs of other families
     are dropped, coldest first, until there is room or no log is left;
  2. after admission, the publication keeps its log when it fits the headroom; otherwise colder families' logs
     are dropped, coldest first, to fit it; if it still does not fit, it is published without its log
     (`read-log-no-room`);
  3. `shedOneRetentionTier` gets a first tier that drops the variants' logs.
  A variant whose log was dropped keeps its basis with the reason and takes today's path (counted). Invariant to
  test: on the reviewer's rotation the switch-on family count, `Use` count and evictions are no worse than
  switch-off; charged bytes equal a recomputation after every drop. A2 stays a hard prerequisite for merging B.
- **F2 — adopt the reviewer's race test**: a global evidence-epoch bump during the replay must refuse the re-stamp
  (the `revalidatable` epoch check is not an equivalent mutant: removing it leaves the implementer's tests green).
- **F3 — the audit must be independent and say when it is not.** Its planning bypasses the packed plan cache and
  the heuristic filter-pass cache (neither read nor publish). Where a bypass cannot be had cleanly, or the
  planning reports a packed hit or resumed work, the audit is inconclusive: counted separately, never as a match.
  Its remaining side effects (live samples, lifecycle admissions) are documented at the switch.
- **F4 — a log is kept only for an outermost planning**: mark at recorder depth 1 with nothing recorded before
  it. A nested pipeline planning (none in production today) keeps no log (`nested-planning`).
- **F5 — ratio reads compare bit-exact (assigned to the log's owner as R14).** A filter pass ratio feeds
  structural thresholds (`LmdbJoinPlanSupport.isSelectiveSmallLiteralFilterAnchor` ≤ 0.75, deciding whether
  `LmdbFilterSimplifierOptimizer` keeps a redundant filter). Counts are already compared exactly, so a ratio moves
  inside the tolerance only when a sample of the same size is replaced; exactness costs almost nothing and removes
  the need to enumerate threshold consumers. FILTER_RATIO, TEMPLATE_RATIO, PATTERN_RATIO and SAMPLE values:
  tolerance 0 always. The tolerance stays for model values, whose consumers are guarded by `CONSUMER_BOUNDS` /
  `CONSUMER_EXACT`.
- **F6** — the detached refresh's rescue check catches a replay exception, counts a failed replay and keeps the
  build evidence (today's publication).
- **F7** — a request whose replay validated its own revision but found the variant already re-stamped to another
  revision is served without a re-stamp when champion and log object are unchanged and the variant is servable
  (its own replay proved its revision; a request at an older revision cannot get here because its captures would
  not match).
- **F8** — no validator or audit lambda is allocated with the switch off.

**Consequence recorded, to measure:** an identical revalidation of a ranked family serves `Use`, so the refresh
that every learned move used to submit no longer runs for it. With identical reads a refresh would search the same
inputs; execution regressions still reach the variant through `observeExecution` (not CONFIDENT → today's path).
The B measurement reports refresh submissions and promotions with the switch on and off.

### Review-round hand-back (2026-11-11, `impl-phaseB/round2/`, private copy, not merged)

F1–F4 and F6–F10 implemented; 178 tests green three times, 29 of 29 mutations killed, no pass→fail against the
base copy. Read by the coordinator (production part of `changes-review-round.diff`):
- F1: `retainedBytes` never includes a log; `readLogBytes` is tracked apart and every charging path ends with
  `fitReadLogs()` (drop coldest logs until families + logs + reservations + doorkeeper fit). Eviction and shedding
  conditions read `retainedBytes` only, so they fire exactly when they do with the switch off; the log-drop shed
  tier costs one extra loop iteration and changes no decision. A re-stamp racing a drop fails its CAS and is then
  refused by the log-identity check. Capacity on the reviewer's 24-query rotation: 22 families / 23 of 96 `Use` /
  98 evictions with the switch on and off (24 queries sit at the budget's edge, so switch-off runs alone range
  23–72 `Use`; the permanent test asserts a 16-query rotation: 16 / 48 / 0 on both).
- F3: the audit plans a miss's search against a private empty packed cache and bypasses the filter-pass cache
  through a thread-confined scope; a fresh plan that still reports reused packed work is counted inconclusive.
- F7: `revalidatable` no longer compares the stamp; the same log object with another stamp can only be a re-stamp
  (a refresh stores no basis, a build stores a new log).
- F10: all three cases were already refused; pinned.
Open item found on the way (pre-existing, not phase B): planning through the uncached packed entry point
(`planner.optimize` without a cache, taken when no cache context is available) chose a different plan than a
cache-miss search for 8 of 47 audited requests under the experimental-full profile. To be root-caused separately.
A focused re-review of the round (accounting under concurrency, F3 scope, F7) runs before the merge.

Re-review (2026-11-11, `review-phaseB-r2-evidence/NOTES.md`): mergeable after one fix. Accounting held in a
concurrent stress (6 threads, 21,600 operations, 24 quiescent checks, 0 violations) and a seeded 400-step
switch-on/off differential (identical outcomes, resident sets, evictions, bytes); soundness probe on the round-2
code: 414 revalidated serves (128 within tolerance), 0 mismatches against an independent fresh planning, 0
inconclusive audits; later plannings no longer hit what the audit published (0 of 78 and 0 of 43; round 1: 49 of
64 and 34 of 46). With phase-A-sized logs most logs are refused for no room on a full rotation at the default
budget (54–61 of them); to be re-measured with compact logs.
- **F11 (blocking): a log drop must never change a cache decision.** `dropLogs` replaces the `FamilyVersion`
  object; `publishRefresh` then fails its identity check and the refresh is lost as stale where the switch-off
  cache publishes it (reproduced deterministically); `hardInvalidate`'s single compare-and-set can miss once the
  same way. Rule: a version that differs from the observed one only by read-log bases is the observed one for
  every identity check, decided by reference comparison of all other components (no new fields, no charge
  change), and the commit continues from the current version so no dropped log is resurrected. Every site that
  compares or compare-and-sets `family.current()` is classified. Rejected alternative: logs held outside the
  version object in mutable slots — it would break the compare-and-set discipline the accounting proof rests on.
- Round 3 also ports phase B onto the A2 encoding (`trees/base-phaseB-a2`), re-derives the retention-cap default
  from the A2 round 2 distribution and re-runs the capacity table with compact logs.

Round 3 hand-back (2026-11-11, `impl-phaseB/round3/`, merge pending; 21 files against `base-phaseB-a2`):
- F11 as specified: `sameExceptReadLog(s)` by reference; applied in `publishRefresh`, `publishVariant` (commit
  rebuilt on the current version by `rebased`) and `hardInvalidate` (retries from the current version while it
  differs only by dropped logs); every other site is a re-reading retry loop, runs under the admission lock, or
  compares version numbers (`site-classification.txt`). Four red-first tests, the reviewer's stress test adopted
  (2.3 s), 8 of 10 mutations killed and the two survivors equivalent. Read by the coordinator: a version equal
  except for its logs can only come from `dropLogs` (a re-stamp also replaces the evidence object).
- Port: only `LmdbPlanningReadLog` and `LmdbHeuristicFilterPassCache` changed on both sides, merged cleanly;
  `Log.accountRetainedMemory` now charges digest groups (red on the plain merge: 1,832 charged of 1,934);
  `markAtPlanningStart` refuses after a witness or the lifecycle guard. 327 tests in both modes, three runs each;
  the only red is the known intermittent caused by the redundant Frontier rebuild (also 2 of 6 on the base copy).
- Retention cap stays 256 KiB: under the default rollout every theme log is retained (median 9.2 KB, p90 27.2 KB,
  max 105.5 KB incremental as charged by the cache); with lifecycle keys kept, as under enforcement, 96.7 %.
- Capacity with compact logs: 16-query rotation 16 / 48 / 0 with the switch on and off, 10 logs kept; 24-query
  rotation 23 / 23 / 49 on both, no log refused for room any more.
- **Measured limit, no design change yet:** 6 of 16 and 16 of 23 resident variants never had a log because their
  planning was answered by the packed plan cache (incomplete by rule R1). Such a variant takes today's path at its
  first learned move and gets a log from that full planning. The B measurement reports variants without a log by
  reason; if packed-hit publications dominate under execution feedback, the packed entry should carry its
  planning's log (as the filter-pass cache carries its slice).

## Not in this change
Removing the LEO revision from the packed cache or HFPC identity; relaxing the entry gate; the cache's share of the
evidence budget; the order dependence of the lifecycle `regressionLimit`; the torn multi-monitor `leoRevision()`
read beyond what A2's two captures need.

### Merge and B measurement protocol (2026-11-11 evening)

Phase B (21 files) and the Frontier acknowledgement fix (5 files) were merged into the working tree together
after requal13 released it (A2 requalification: no case red 3/3 in one tree only). Requalification of the merge:
`evidence/requal14-summary.txt` (selections, incl. validation off against the pre-merge base) and
`evidence/module15-summary.txt` (whole `core/sail/lmdb`, once per tree), base `trees/base-frontier-ack`.

B measurement (private copy `trees/measure-b`, scratch harness, nothing merged; evidence `impl-phaseB/measure-b/`):

- Workload: the theme catalog on clones of the pristine theme store, through the public repository API, default
  configuration and default rollout. Every configuration starts from a fresh clone (no learned sidecars carried).
- Step per query: plan (explain), evaluate with a 5 s limit (produces execution feedback), plan again.
- Rotations: (T) per theme: all queries of one theme for 4 rounds, then the next theme, so the working set fits
  the cache; (C) whole catalog for 4 rounds, so the cache is capacity-bound.
- Configurations: validation off; validation on; validation on with audit on. Each rotation and configuration is
  run twice to show run-to-run variance.
- Reported per configuration and rotation, per round:
  1. lookups by outcome and refresh reason; hits served by revalidation; `servedWithoutRestamp`;
  2. replay verdicts (identical / within tolerance / changed, changed by first differing read kind);
  3. audit: mismatches under an identical verdict (must be 0; any one is a bug, report the query, both plans and
     the differing read), within-tolerance differences (count over within-tolerance serves; for each: query, both
     plan structures, the fresh price of both plans, and which discrete rule if identifiable), inconclusive;
  4. replay time (sum, median, p99 per lookup) next to planning time for the same lookups, and total planning time
     per round on against off;
  5. cache state at the end of each round: families, variants, retained bytes, read-log bytes, logs kept, logs
     dropped by reason, resident variants without a log by reason (packed-cache hit, nested planning, over cap,
     dropped for slack, refreshed variant, ...);
  6. refresh submissions, completions and promotions, on against off;
  7. evaluation: per query and round the wall time and outcome (ok / timeout / error), and the totals on against
     off, so a plan served by revalidation that executes worse is visible.
- Acceptance for keeping validation on by default: audit mismatches 0; no query whose evaluation gets
  consistently slower with validation on (both runs, more than run-to-run variance); hit rate after learned-state
  changes higher than with validation off. The within-tolerance difference rate is reported, not gated (owner
  decision 2026-11-11).

### Interaction found by requalification: two F10 tests relied on the redundant Frontier rebuild (2026-11-11 night)

`evidence/requal14-interim.txt` (clean runs before the semaphore exhaustion): in the merged tree
`LmdbPlanCacheRevalidationStoreTest#aLogRecordedBeforeAFrontierPublicationIsNeverServedAtTheNewFrontierRevision` and
`#aDetachedRefreshSpanningAFrontierPublicationKeepsItsBuildEvidence` are red at their coverage prerequisite ("new
Frontier statistics" / "the Frontier revision moved during the refresh": the revision did not move). Both produce
"a Frontier publication" by calling `rebuildFrontierStatistics()` on an unchanged store. Before the Frontier
acknowledgement fix that published a second generation (the defect); now it is correctly "already covered". The
tests are outdated, the product is right. Everything else in the plan-cache selection matched the pre-merge base
(0 mismatches among shared cases, default and full keys; with validation off the only reds are tests of the new
feature).

The real situation these tests stand for: a commit, then a planning cached at the new data but the old Frontier
generation, then the Frontier publication that covers the commit. Rewrite, test-side only:

- Fixture option "hold scheduled Frontier refreshes", built like
  `LmdbFrontierAcknowledgementRebuildTest.Harness` (override `runScheduledFrontierRebuild` through
  `LmdbStore.createBackingStore`), so the publication happens exactly where the test puts it. `settle()` must not
  block on a held refresh; if `StartupStatus.backgroundWorkPending()` counts it, the fixture needs a settle
  variant that ignores the held refresh, by existing accessors only.
- Sequence for the first test (both switch settings): add one statement (commit; refresh held), cache LEAF,
  evaluate OTHER, settle, explicit `rebuildFrontierStatistics()` (publishes, the journal sequence advanced), then
  the lookup. Prerequisites asserted: the Frontier revision moved; the learned revision moved; no statement commit
  and no change of the statement mutation stamp or predicate-range version between caching and the lookup (so the
  Frontier move is the only data-side difference); with validation on, the log still replays identical.
- Sequence for the detached-refresh test: add the statement and capture `before` with the refresh held; the
  after-planning hook evaluates LEAF elsewhere and runs the explicit rebuild elsewhere.
- Each test must turn red when the guard it stands for is removed: one mutation per test (the Frontier-revision
  condition in the revalidation eligibility; the build-evidence rule in the detached refresh), run in a mutation
  copy, restored and hash-checked.
- No sleeps, no production change. If the hold cannot be done without a new production seam, report instead.

### F10 rewrite hand-back (2026-11-11 22:50; private copy `trees/f10fix`, one test file, not merged yet)

Evidence `impl-f10fix/` (`report.txt`, `changes.diff`, `snippets.txt`, `mutation-M*.json`). Red before on the
untouched copy (both cases, same messages as requal14), green after, the class 15/15 in five default runs and one
full-key run. Mutations, each in a separate copy and restored: dropping the Frontier-revision condition from
`LmdbPipelinePlanCache.Revision.onlyLearnedStateDiffers` turns test 1 red (the switch-on run replays, re-stamps
and serves as revalidated) and test 2 too; publishing a detached refresh under its current instead of its build
evidence turns test 2 red; a rescue rule without the learned-only condition turns test 2 red, which shows the
refresh's log does replay identical across the publication and only the Frontier condition keeps the build
evidence. Merge waits for requal14 (it hash-checks the working tree).

Deviation from the spec's sequences, forced by a product fact found on the first green attempt: while the
published Frontier generation does not cover a commit, the Frontier fallback reason is `CONFIDENCE_TOO_WIDE`,
`LmdbPackedCostModel.mappedStatisticsReady` rejects the mapped session, planning uses the conventional cost
session, and only `LmdbMappedFrontierLearningSession` creates runtime feedback contracts. So an execution in the
window between a commit and the publication that covers it teaches nothing. Test 1 therefore learns after the
publication; test 2 caches the family and learns before the commit. The diagnosis is by reading plus the
red-then-green; not traced directly.

Open design observation (not in this change): between a commit and its Frontier publication (a) no learned state
can move, (b) planning runs on the conventional cost session, so a plan may differ from the one planned a moment
earlier or later, and (c) the Frontier planning revision changes twice (fallback reason, then generation), each
invalidating cached decisions. With the scheduled refresh running normally the window is short; under a steady
stream of commits it may be the normal state. Whether the committed mutation tail is meant to keep the mapped
session usable in that window is to be read before judging. The B measurement has no commits and does not see
this.
