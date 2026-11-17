# Cache validity, step 1 — a planning revision that is a version, not a clock (spec, 2026-11-10)

Parent design: `GH-0000-cache-validity-under-execution-feedback-2026-11-10.md` (approved by the owner, all three
steps). Facts used here are in `.agent/research/GH-0000-phase1-continuation-2026-11-09/learned-state-read-write-inventory.md`
(sections A, B, C, F, G, H; line numbers there are for the working tree of 2026-11-10 and will drift).
OFS = `LmdbOperatorFeedbackStats`, FLM = `FrontierLearningModel`, PLS = `PlanLifecycleStore`.

## Problem
`OFS.planningRevision()` returns `feedbackEpoch`. That counter is the observation clock: it ticks once per accepted
typed publication before any mutation, it stamps observations, and it is the time base of the model's recency decay.
`LmdbEstimatorRuntime.leoRevision()` mixes it into the value every plan-cache entry is compared against, so a
publication that changed nothing a planner can read still invalidates every entry.

## Invariant (the contract of `planningRevision()` after this change)
Soundness, mandatory: if any production planning read of OFS-owned learned state could return a different result at
instant t2 than at t1 (same arguments, same data revision, same provider objects), then `planningRevision()` at t2
differs from its value at t1. The production planning reads are the five synchronized OFS methods called from
`LmdbMappedFrontierLearningSession` (`logicalDimensionDecision`, both `physicalDimensionDecision` overloads,
`exactCardinalityFact`, `planLifecycleDecision`) and the legacy `estimate` (inventory C.1, C.3).
Precision, the purpose: the cases listed under "Must not advance" leave it unchanged.
Shape: non-negative, never decreases within a process, never repeats a value after a change.
The observation clock (`feedbackEpoch`) keeps every current behaviour: stamps, decay time base, persistence.

## Mechanism
Do not derive the revision from a `changed` flag computed after the fact (a throw between a mutation and the bump
would leave a change without a version). Make it the sum of component mutation counters, each incremented in the
same block as the mutation it versions, all under the monitors that already guard those mutations:

    planningRevision() = base + legacyTicks + model.planningVisibleMutations() + exactFactMutations
                         + lifecycleObservedMutations + structuralMutations

- `base`: the persisted `feedbackEpoch` restored at construction (so a loaded sidecar still yields a positive value,
  which tests use as the proof that it loaded). Not persisted separately; no sidecar format change (versions 24/25
  stay byte-identical).
- `model.planningVisibleMutations()` (new counter in FLM, never reset, survives `clear()`): incremented by every
  mutation that can change the result of `logicalDecision`, `physicalDecision` (both), or the legacy `posterior`:
  an admitted absolute or residual observation; a censored-bound change (already truthful); `decay` with a factor
  other than 1; `clear` when anything was stored; calibration observation; eviction of a calibration that holds
  observations; eviction of a cell that holds observations or a censored bound. Creation or eviction of an empty
  cell or an empty calibration must not increment unless you find that it changes a decision's estimate, bounds or
  gate class (report what you find: the inventory, H.8, says an empty cell can turn INAPPLICABLE into
  INSUFFICIENT_SUPPORT, both without an estimate; if that is the only effect, it does not count, because validity is
  about what costing consumes, not about the gate label in an annotation).
- `exactFactMutations`: incremented when a fact's planner-visible value changes: rows differ, or the fact was not
  current before (no rows, or its stamp differed from the stamp it is now written with) and is current now; and when
  a current fact is evicted or cleared. An identical rewrite of a current fact does not increment.
- `lifecycleObservedMutations`: incremented when a feedback observation or a store mutation changes what
  `PLS.decision` can return: the delta of `PLS.revision` across `lifecycleStore.observeResolved(...)` and across
  `markAllStale()`. Admissions and `planned` overwrites done by planning and precompilation stay excluded, as they
  are today by documented decision (`OFS` javadoc on `planningRevision`). You must prove, by reading `PLS.observe`
  and `PLS.decision` (and `bestEligibleAlternativeUpper`), that every field `observe` writes and `decision` reads is
  covered by a `PLS.revision` bump; if one is not, bump for it and say so.
- `legacyTicks`: every site that advances the clock today other than the typed publication keeps advancing the
  planning revision one for one: the legacy observe path (`nextFeedbackEpoch()` sites other than `publishInScope`,
  including the prepared-publication path), `reset()` failure, `clearLearnedState`, `recordStoreMutation`. These are
  not the measured problem (explain/telemetry evaluation, commits, resets) and their tests stay as they are.
- Clock-coupled legacy reader: `learnedByOperator` confidence decays with `feedbackEpoch` itself (inventory B.2), so
  while `learnedByOperator` is non-empty every clock tick, including a no-op typed publication, must advance the
  planning revision.
- Typed semi/anti distinct counts are written on publication and read by no production code. They must not advance
  the revision while unread, and the invariant must not depend on a grep staying true: the read method
  (`typedSemiAntiDistinctEstimate`) sets a sticky flag, and from then on a write to these counts advances the
  revision.
- The typed publication itself (`publishInScope`) advances only the clock; the planning revision moves through the
  component counters above. Filter cells are versioned by `LmdbFilterSelectivityStats.planningRevision()`, which is
  its own component of `leoRevision()`; do not double count them here.

`learningModelRevision()` keeps returning the model's decay clock (`FLM.revision()`); add a separate getter for the
new counter if a test needs it. Add a package-private getter for the observation clock.

## Must not advance (each gets a test)
1. A typed publication in which nothing is admitted (refused features, released or old-generation cells, a bound open
   on an unconditioned key with no other component).
2. An identical rewrite of a current exact fact.
3. A lifecycle observation that causes no transition, no verified change and no last-good change.
4. A typed semi/anti distinct write while no reader has ever asked.
5. Creation of empty cells by precompilation (lease, exact-fact cell, lifecycle resolve).

## Must advance (each gets a test)
6. An admitted model observation; a censored-bound change.
7. An exact fact whose rows change; a fact that becomes current with the same rows as a stale one.
8. A lifecycle transition.
9. A typed semi/anti distinct write after the reader was called once.
10. Any legacy-path observation, a commit (`recordStoreMutation` with evidence), a reset (existing tests).
11. A no-op typed publication while `learnedByOperator` holds an entry.
12. Eviction of an evidence-bearing model cell by a precompilation lease; eviction of a current exact fact; eviction
    of a calibration with observations. (Today these are silent; this is a correctness gain, test-first.)
13. Restart: `planningRevision() > 0` after loading a sidecar whose epoch was positive; sidecar bytes unchanged.

## Store-level test
Through the public API on a real store with the default configuration: find the smallest query whose completed
evaluation publishes typed feedback that changes no planner-visible value on its second evaluation (candidate: a
shape whose only learned output is an exact fact). Assert that `leoRevision()` is unchanged by that second
evaluation and that the next lookup is served from the cache (replay-only or ranked, whatever the decision is),
where today it is not. If no such shape exists because every completed evaluation admits a model observation, say so
with the evidence; that is an expected possibility and bounds what step 1 can deliver on its own.

## Existing tests
Tests that assert on `planningRevision()`/`leoRevision()` (inventory E.7, F). For each one that goes red, decide and
report: outdated (it pinned "every publication ticks") or a real regression of the invariant. Do not weaken an
assertion to get green; rewrite an outdated test around the invariant it names and show it red under a one-line
product mutation.

## Out of scope for this step (do not change)
`leoRevision()`'s composition and its torn multi-monitor read; `PackedPlanCache.newerGeneration` ordering LEO hashes;
`LmdbFilterSelectivityStats` and `LmdbLearnedFilterSelectivity` revisions (step 1b); the plan caches themselves
(step 2); the sidecar format; the public API; anything per row or per open on the execution path.

## Cost
O(1) per publication and per mutation; one long addition per `planningRevision()` call. No new allocation.

## Deliverables
Failing tests first with their Surefire snippets, then the change; `changed-files.txt`, cumulative `changes.diff`,
`evidence.txt`; three consecutive green runs of every touched or added test class; one run of the test classes named
in inventory E.7 and F compared against a pre-change run of the same selection in the same private copy; a list of
anything in this spec that the code contradicted.
