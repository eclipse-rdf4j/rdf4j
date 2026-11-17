# Second review of the planner budget change (round-2 diff, 2026-11-10) and decisions

Read-only review; nothing was run. Line numbers are in the working tree after the round-2 merge.
PIS = PackedIncumbentSearch, PJE = PackedJoinEnumerator, PCP = PackedCascadesPlanner, PSB = PackedSearchBudget.
"Decision" lines are the coordinator's.

**G1. F2 suspends the canonical correlated predicate, which is the default implementation of an assured correlated
predicate. Medium; mechanism confirmed by reading.**
PIS 964–979 calls the new suspended wrapper (1164–1179). On refusal it returns 0, so pricing falls through to the
context-free default (globally costed dependent subquery) inside the mandatory scope. Past the deadline, a
correlated FILTER / SEMI / ANTI group with no winner now gets that context-free price instead of its contextual
one; round 1 priced it contextually. `defaultOfferRefused` is not set, so the group's inputs become current and it
is never retried. With a non-latching refusal, on a re-offer the context-free candidate competes with an existing
contextual incumbent — what the comment at PIS 969–973 forbids.
Suggested fix: keep this call in the mandatory scope for a group with no winner (suspend only its nested
sub-search); or on refusal set `defaultOfferRefused` and skip the context-free fall-through when bindings are
assured.
Decision: FIX. For a group with no winner, the canonical correlated predicate IS the default implementation and
stays mandatory; only its nested optional sub-search is suspended. A context-free price must never be offered for
an assured correlated predicate. Test first with an expired deadline.

**G2. F1 is not closed for scalar sessions. Medium-low, confirmed.**
With `costSession == null`, the sparse epilogue re-emits through `emitSelectedOrder` (PJE 8986–8992), which
reserves `factors.length-1` units (PJE 10308); a search-stopped budget refuses, so the retained full-set state is
still dropped. The dense scalar epilogue (PJE 7693) has the same problem (predates this round).
Decision: FIX both. Publishing an already retained complete state must not need a fresh optional reservation.

**G3. F4 is half undone. Medium-low, confirmed.**
PIS 247 (alternatives loop) and PIS 1695 (worklist) call `rememberLogicalInputContinuationGenerations`
unconditionally, overriding `continuationsPriced=false` from PIS 816. The new local absorptions (memoized/hash
offers 1143–1154, correlated wrapper 1164–1179) and absorbers inside contextual sub-searches do not set
`defaultOfferRefused`, so inputs of abandoned candidates are still recorded as current.
Suggested fix: `offerLogicalImplementation` reports whether it fully priced; skip lines 247 and 1695 when not.
Decision: FIX.

**G4. F3 residual. Low-medium, plausible.**
A transition is marked explored after a normal return even when a refusal was absorbed in a nested contextual
search inside it (5150 → absorbers at 2201 and 5859). With a non-latching refusal that contextual alternative is
never revisited and the lattice can still be certified (4380 ff.). Separately, a half-finished transition
(dependent child retained, hash refused) stays unexplored and its parent displaceable (1287); resuming re-costs it
and may keep a duplicate continuation.
Suggested fix: a refusal counter on the budget; mark explored only if it did not change during the transition.
Decision: FIX the certificate hole. For the duplicate-continuation case, establish whether a duplicate can
actually be retained and whether it is harmful (cost, memory, or a wrong tie-break); fix if so.

**G5. The new F12 throw is reachable from unguarded callers. Low-medium, plausible.**
Throw at PJE 11022–11026. `contextualChildWork` (10964/10985) passes `memo.winnerTotalCost(...)`, and the comment
at 10950 admits contextual work can be unknown. The inherited singletons at 5931 and 6458 keep `factorWork` as NaN
when `unavailableFactorWork` accepts an event's objective. Before this round that gave "unavailable" (NaN); now a
`PackedMemoInvariantException` escapes `optimize()` as a planning failure (breaks I1).
Decision: FIX, highest priority together with G1. Unknown work stays unknown at every caller; the invariant throw
is only for values that are negative or otherwise impossible. Add a test per caller.

**G6. `providerStopRecorded` over-reports. Low, confirmed.**
`require` sets it (PSB 79–84) even when planner code consumes the refusal internally
(PackedStrategyRequestImpl 686–704, 810, 861) and the callback completes normally. It is sticky across F6's fresh
memo: a rejected replay's flags (`providerStopRecorded`, or `providerWorkLimitReached` from a refused large block)
can make a complete fresh search report `INCOMPLETE_WORK_LIMIT`.
Decision: FIX. The completion status must describe the search whose result is returned: a refusal that was
consumed without truncating anything, and stops that belong to a rejected replay attempt, must not mark a complete
fresh search incomplete. Conversely F5's invariant stays: every absorbed truncation leaves the result incomplete.

**G7. F6 edges. Low.**
The guard `eventCount ≤ allowance` (PCP 1134) understates replay cost (each replayed invocation charges three
realize calls, the callback and an objective call); refusal handling covers it. PCP 1252 still feeds replay units
into `optimizer.planCacheCumulativeWorkUnits` (PCP 666), mixing provider units into a search-unit metric.
Decision: FIX the metric mixing; make the pre-check honest or remove it if the refusal path is the real guard.

**G8. F8b is partial. Low.** The catches at PJE 4333, 4382, 5859, 6294, 6382 and 9689 still report partial units.
Decision: FIX.

**G9. Hot path.** `new SavedFactorFrame[0]` field initialiser allocates per `PackedJoinEnumerator` instance, and
nested enumerators are created per dependent-factor contextualisation. Use a shared static empty array.
Decision: FIX.

**G10.** The javadoc above `sparseKernelSearchStopPublishesTheRetainedCompletePlan` describes the F4 test.
Decision: FIX.

## No defect found
F1 provider path (all five exits converted; epilogue provider- and budget-free); F2 suspend/resume pairing and
nesting, local catches absorb only planner refusals, wide-seed `tryConsume`; F3's `exactStateKeys` mark at pop is
valid; F5 and F7 classification; F6 scalar and detached-refresh resume go through the same site; F11 frame stack
(push before try, pop in finally, no observation after pop, audit cross-checked against code; the assert has no
side effects but only detects frame aliasing).
