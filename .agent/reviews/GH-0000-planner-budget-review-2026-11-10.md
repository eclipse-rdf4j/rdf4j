# Independent review of the planner budget change (2026-11-10) and decisions

Reviewer read the working tree against the pre-change tree; nothing was run. Line numbers are in the working tree
after the merge. PJE = PackedJoinEnumerator.java, PIS = PackedIncumbentSearch.java, PCP = PackedCascadesPlanner.java,
PSB = PackedSearchBudget.java. "Decision" lines are the coordinator's.

## Findings

**F1. Sparse kernel throws away priced complete plans on a search, byte or arena stop (I1). Medium-high, confirmed.**
PJE 8723 returns on `!tryConsume()`. PJE 8865, 8873, 8894 and 8901 return on a resource stop or a full arena. All
leave the method from inside the try, so they skip the epilogue at 8927–8945 that publishes `states.find(fullSet)`.
Scenario: a 17–64-factor join with a bounded `workLimit` runs out of budget during the last cardinality level; the
full-set states already retained are dropped even when cheaper than the root incumbent. The logic predates the
change (only re-indented), but is now inconsistent with the provider-stop path, which does reach the epilogue. The
new tests use at most 9 factors and never reach this kernel.
Suggested fix: replace the returns with a labelled `break` out of the cardinality loop plus a `stopped` flag that
skips `optimizeGreedy`, as `providerStopped` already does.
Decision: FIX, test-first with a fixture of at least 17 factors.

**F2. The mandatory-completion scope also covers optional work. Medium, confirmed.**
PIS 826–843 enters the scope around the whole of `offerSelectedLogicalImplementation`. Inside it run:
`offerMemoizedPositiveOptional` and `offerIndependentPositiveOptionalHash` (PIS 1114–1118, after the incumbent is
already offered); `contextualizeOptionalRight` (PIS 897) and `contextualizeJoinRight` (PIS 929), both entering
`optimizeWithInheritedPrefix`, whose wide seed reserves with the deadline-free `tryReserveMandatoryCompletion(1L)`
(PJE 6281); `offerCanonicalCorrelatedPredicate` (PIS 941). Every provider admission in these calls ignores the
deadline, and with the production default (`providerWorkLimit = Long.MAX_VALUE`) nothing bounds it.
Suggested fix: leave the scope once the incumbent is offered, before PIS 1114, and around the contextual
sub-searches; keep it only for the calls that price the default implementation.
Decision: FIX. The scope must cover exactly the bounded callbacks that price the default implementation of a group
that has no winner. Add a test with an already-expired deadline that proves optional offers and contextual
sub-searches make no provider call after the incumbent is priced.

**F3. A retained correlated lattice keeps the abandoned transition marked explored → possible false completeness
certificate. Medium, plausible.**
The explored bit is set before any callback: PJE 5063 (`costDenseCorrelatedAppend`) and 4989 (filter transition).
`exactStateKeys` is set when a state is popped, before its extensions are costed (PJE 4918). After the refusal is
absorbed (PJE 4326, 4375, 3576) the lattice is retained (PJE 4330, 4403, 3581). In the same run the alternatives
phase resumes the seed; its open cut excludes explored bits (PJE 4895, 4919), so the abandoned transition is never
costed. If the remaining targets settle, `searchCompleted` becomes true (PJE 4380), and a
`CompletedCorrelatedLattice` and its coverage certificate are recorded (PJE 4445–4465); then filter placement is
skipped (PIS 1367) and covered regions are treated as searched (PIS 650). Trigger: a refusal that does not latch
(a large block such as `PackedCostVectorArena` 135 `require(5*STRIDE)`, `PackedQueryView require(characters)`, or
provider batches) followed by smaller admitted ones, or the strategy-stop path in F5.
Suggested fix: set explored and exact marks only after the transition completes, or record the in-flight mark and
undo it in the catch before retaining.
Decision: FIX. First confirm with a test (non-latching refusal on one transition, later transitions admitted):
no `CompletedCorrelatedLattice` certificate may be recorded for a lattice with an abandoned transition. If you can
prove the scenario is unreachable, show the proof instead and add an assertion that documents it.

**F4. A refused re-offer marks the group's inputs as current although they were never priced. Medium-low, confirmed.**
PIS 833–838 returns the old incumbent; `offerLogicalImplementation` then still calls `rememberLogicalInputs` (805)
with the new child winners and continuation generations. Same after the continuation catch (797–802).
`logicalInputsAreCurrent` is then true, so neither the worklist (1580–1586, 1641–1646) nor the build loop (217–222)
re-prices the parent, even while provider work is still available.
Suggested fix: skip remembering inputs when the catch returned a pre-existing incumbent, or remember that
incumbent's actual children.
Decision: FIX, test-first.

**F5. An absorbed stop can leave no "incomplete" flag → truncated search reported EXACT_COMPLETE. Medium-low,
plausible (no production provider uses strategy requests).**
`tryReserve` refuses without touching the budget when the active strategy request is stopped (PCP 822–824). A
request is stopped by a byte refusal that does not latch (PCP 1071–1073 uses `canReserveBytes`). `require` then
records the refusal (PCP 848) and the drivers absorb it. `absorbProviderStop` (PJE 2024–2029) only calls
`recordExhaustedWorkStop`, and the PIS catches set nothing, so `completionStatus()` (PSB 307–321) stays
`EXACT_COMPLETE`.
Suggested fix: record a provider stop in the absorb paths whenever no other stop flag is set.
Decision: FIX. Invariant: every absorbed stop leaves `completionStatus() != EXACT_COMPLETE`. Test it at each kind
of absorb site.

**F6. Checkpoint replay. Low-medium, confirmed (structure predates the change).**
PCP 1114–1127 guards replay only on search work and reserves `eventCount` search units for provider events, which
contradicts I3. Replay callbacks are admitted on the provider dimension, outside the mandatory scope. If the
deadline passes during replay, or `providerWorkLimit` is smaller than the replay needs, the `Exhausted` escapes
`compute()` (only the exceptions at 1128–1146 are caught) and lands in PCP 741–745, which returns the unpriced
whole-query fallback.
Suggested fix: treat the refusal like a rejected checkpoint, restart on a fresh memo, and pre-check the provider
allowance.
Decision: FIX. Replay charges the provider dimension; a refused replay is a rejected checkpoint, never the
unpriced whole-query fallback when a fresh search could still run.

**F7. Wrong refusal reason inside the mandatory scope. Low, confirmed.**
`workRefusal` (PCP 993–1002) checks the deadline first, but the mandatory scope never refuses for the deadline, so a
provider-limit refusal there after the deadline latched is reported as `DEADLINE_EXPIRED`.
Decision: FIX.

**F8. Accounting and publication gaps (I4). Low.**
- Published `providerWorkUnits` (PCP 1232) includes the completion credit that `close()` returns later
  (1021–1023): can over-report by one.
- The absorbing wrappers return 0 (PJE 2040, 2730, 3398, 3534) and the seed's work is lost at PJE 5583, so
  `deterministicWorkUnits` and the checkpoint's cumulative work under-count.
- LmdbCascadesOptimizer (667, 682) and the `optimizer.planCache*WorkUnits` plan metrics publish `workUnits` with no
  provider counterpart.
Decision: FIX the first two in core. The LMDB metric is a separate small change (not for the core implementer).

**F9. `isBounded()` (PSB 323–330) ignores `providerWorkLimit`.** A search limited only by provider work skips the
bounded seeding phases (PJE 2091, 4241, 3387; PIS 561).
Decision: NO CHANGE now. Including it would make a provider-limited run seed and cost prefixes twice, so an exact
fit would no longer be reproducible, and the production default leaves the provider limit unbounded. Record it.

**F10. A byte stop that latches during `openSession` makes `exhausted()` true at PCP 714–717 and forces the unpriced
whole-query fallback (predates the change).**
Decision: NO CHANGE now. Record it.

## No defect found
Catch sites publish no half-costed plan (states appended and winners offered only after that state's callbacks;
epilogues make no provider calls); scope enter/leave pairing and nesting; budget arithmetic (no negative counts,
double return or overflow); refusal identity and behaviour after close; compatibility constructors, cache-reuse
check and flight equality include the new limit; no new per-candidate allocation, boxing or virtual call. One
stale comment in `PackedPlanCacheTest` still says "seven provider calls".

## Behaviour changes within the contract (for the roadmap)
- The search `workLimit` no longer bounds provider callbacks or LMDB per-read `require(1)`; only the deadline does
  (and not inside the mandatory scope). Detached refresh runs with no deadline.
- Session open and the completion credit moved to the provider dimension; a 1-unit search runs with the provider.
- Mandatory incumbents are priced past the deadline.
- A non-latching refusal stops the whole kernel phase, not just the candidate.
- A provider-limit stop does not stop later phases from starting (`exhausted()` stays false).
- An `Exhausted` thrown by the provider itself is still turned into a stop at `compute()`.
- `PlanningMetrics`, `SearchCounterSnapshot`, `PackedPlanDecisionSummary` and `PackedPlannerLimits` gained a
  component; `PackedPlannerLimits.equals` includes `providerWorkLimit`.
