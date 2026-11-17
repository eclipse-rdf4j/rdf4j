# Spec: budget stops keep the best priced plan; provider work gets its own limit

Owner decision (2026-11-09): provider callbacks stay counted and boundable, but on a **separate limit** of the same
budget owner. One search-step unit means what it meant at committed HEAD (`4ab0c09438`).

Package: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/cascades/packed/`
Background and evidence: `rca-seed-search.md` (RC-1) and `rca-unknown-lifecycle.md` (group A) in this directory.

## Invariants (these are the acceptance criteria)

I1 **Anytime.** When any limit stops planning (search work, provider work, deadline, retained bytes), the planner
   returns the cheapest complete priced plan it had retained at that moment. A refused provider callback abandons
   only the candidate being costed. It never unwinds the search, and never replaces a priced plan by the unpriced
   written-order fallback.
I2 **Dimensions are independent.** A retained-byte stop refuses byte reservations only. It does not refuse search
   steps for the mandatory incumbent or provider work. A provider-work stop does not refuse byte reservations.
I3 **Search unit = HEAD unit.** `PackedPlannerLimits.workLimit` and `PackedSearchBudget.workUnits()` count search
   steps only. Provider callbacks do not consume them. The ten budget-pinned tests listed under T3 must pass with
   their literals unchanged.
I4 **Provider work is accountable.** Every provider callback and every provider-internal batch is charged to
   `providerWorkUnits`, is refused deterministically at `providerWorkLimit`, and both counters are visible wherever
   `workUnits` is published today (decision summary / optimizer metrics / plan provenance).
I5 **Zero optional search work never opens a provider** (existing `PackedPlanningBudgetTest` behaviour).
I6 **No invented prices.** Unknown stays unknown; a parent that could not be costed is offered unpriced (NaN) over its
   priced child and marks the comparison unavailable.
I7 **Cancellation and strategy failures propagate** unchanged (`QueryInterruptedException`,
   `budget.isStrategyFailure(e)`); closed handles still throw `IllegalStateException`.

## Order of work (Routine A: observe red before each production change)

### Step 1 — retention on stop (I1, I2, I6, I7), against the current single limit

Red first:
- Existing reds, already recorded in `evidence/qualify-01-core-module/surefire-reports/` (re-run them once to capture a
  fresh pre-fix snippet): `PackedBushyOutputOverlapTest` (8 cases),
  `PackedFrontierSubsetKernelContractTest#sparseLongKernelRetainsLocallyLosingPhysicalContinuationNeededBySuffix`,
  `PackedInfeasibleContinuationTest#rejectedDependentValuesContinuationRetainsLegalIndependentPlans` (case `[1]`).
- New test `PackedPlanningBudgetTest#stopNeverDiscardsAnAlreadyPricedCheaperPlan` (parameterized over the three
  `PackedJoinOrderedPrefixTest` fixtures: greedy 3-factor, dense 6-factor, reverse 9-factor). Use an observing cost
  model that records when the fixture's optimal order has been completely provider-costed. Sweep the work limit
  from 1 to the first `EXACT_COMPLETE` budget. Assert for every budget: if the optimal order was completely costed,
  then the returned plan has a price and `totalCost <=` the optimum's cost. Today this is red at 38 / 17 / 57
  budget values (rca-seed-search.md, "RcaProbe4").

Production:
1. `PackedJoinEnumerator`: in each kernel driver (`optimizeDense`, `optimizeSparseLong`,
   `optimizeInternedMultiword`, `optimizeGreedySeeds` / `optimizeGreedy`, the inherited-prefix and
   correlated / relocatable-filter lattice drivers) catch `PackedProviderWork.Exhausted` around the seed and search
   phase. If `budget.isStrategyFailure(e)` rethrow. Otherwise record the stop and fall through to the existing
   epilogue that publishes the best retained complete state (the same control flow as
   `if (!budget.tryConsume()) break`). Before relying on it, verify in every append helper that a candidate's state
   is appended only after all of its callbacks; where a helper appends earlier, truncate to the pre-call state count
   in the catch. The epilogue must stay provider-free.
2. `PackedSearchBudget`: add `tryReserveProviderWork(long)` with the body of `tryReserve` minus the
   `resourceLimitReached` refusal (I2). `ProviderWorkBinding.tryReserve` calls it. Leave `tryReserveBytes`,
   `canReserveBytes`, `ProviderMemoryBinding.tryReserve`, `tryReserveMandatoryCompletion` and
   `canReserveMandatoryCompletion` unchanged (HEAD returned priced plans with them as they are).
3. `PackedIncumbentSearch`: callbacks issued while completing a canonical parent whose children already have
   winners (`offerSelectedLogicalImplementation` and the child-before-parent incumbent pass) run in a
   mandatory-completion scope on `ProviderWorkBinding` (model it on the existing `completing` / `completionCredit`
   pair). In that scope the deadline does not refuse provider work, mirroring the documented contract of
   `tryReserveMandatoryCompletion`. If mandatory provider work is still refused, offer the parent as an unpriced
   executable fallback over its priced child (`totalCost = NaN`, `markUnavailableCostComparison()`); `Exhausted`
   must not escape `build()` from this pass.
4. `PackedCascadesPlanner.compute`: `PackedSelectedPlanContextualizer.contextualize` (≈ line 1136) is outside any
   catch. On a non-strategy `Exhausted`, keep the pre-contextualization root winner and its rows. Verify the
   contextualizer leaves the memo consistent when it aborts; if it does not, make it build into scratch state and
   swap only on success.
5. `ProviderWorkBinding.workRefusal`: a refused *work* reservation must report the dimension that refused it
   (`DEADLINE_EXPIRED`, `UNSUPPORTED`, else `WORK_REFUSED`). `RESOURCE_REFUSED` is for byte refusals. Check every
   caller in `PackedPhysicalRegion` / `PackedStrategyRequestImpl`; if one method serves both kinds, split it.

Green: the reds above plus the new sweep test; then the classes under "Regression selection".

### Step 2 — separate provider-work limit (I3, I4, I5)

Red first (no new test needed for I3): these pass at HEAD and fail today only because callbacks consume search
units. They must go green **without editing them**:
- `PackedJoinOrderedPrefixTest`: `boundedGreedySeedChoosesCheapestContextualExtension`,
  `boundedSearchReachesTheSameCostedWinnerAsUnboundedSearch`,
  `boundedSeedsLeaveEnoughBudgetForDensePlanThatBeatsEveryGreedySeed`
- `PackedSearchTest`: `boundedCorrelatedSeedAppliesSelectiveAntiJoinBeforeFiniteWeightFanout`,
  `boundedFiniteFilterSeedKeepsAnchorFirstAndUsesConnectedSuffix`,
  `budgetedSeedSearchConsidersEveryPotentialEndpointStart`,
  `contextualBoundedSeedRanksAnchorWithItsFirstExpansion`,
  `finiteAlternativeReceivesTheBoundedJoinSearchBudgetBeforeItsParentIsEnumerated`
- `PackedAccessEnablingSeedBudgetTest#seedsEveryLateAccessEnablingRecipeBeforeExhaustiveSearchInEitherInputOrder`
- `PackedPlanOrderIndependenceTest#budgetedPlanChoiceMustNotDependOnQueryTextOrder`
If one of them stays red after step 2, stop and report which and why; do not edit the test.

New red-first tests in `PackedPlanningBudgetTest`:
- `providerCallbacksDoNotConsumeSearchWork`: same query, unbounded limits, once with an ordinary provider and once
  with a provider that performs extra `PackedProviderWork.tryReserve` batches inside each callback;
  `workUnits()` is identical, `providerWorkUnits()` differs by exactly the extra batches.
- `providerWorkLimitIsDeterministicAndKeepsTheBestPricedPlan`: sweep `providerWorkLimit` from 0 to the first
  complete value with unbounded search work. Exact-fit completes; one-short reports `INCOMPLETE_WORK_LIMIT`; the
  step-1 retention assertion holds at every value; repeated runs give identical results.
- Keep the existing zero-optional-work tests green unchanged (I5).

Production:
1. `PackedPlannerLimits`: add a fourth component `providerWorkLimit` (clamped to `>= 0`, `Long.MAX_VALUE` =
   unbounded). The existing two- and three-argument constructors and every production call site pass
   `Long.MAX_VALUE`, so EXACT, AUTO (512 search steps) and BUDGETED behave as at HEAD. `unbounded()` unchanged in
   meaning. No new system property in this change.
2. `PackedSearchBudget`: fields `providerWorkLimit`, `providerWorkUnits`, `providerWorkLimitReached`.
   `tryReserveProviderWork` charges only these (plus the deadline sample). Add the mandatory variant used by the
   step-1 scope. `completionStatus()` reports `INCOMPLETE_WORK_LIMIT` when either work flag is set (no new enum
   value). Add accessors `providerWorkUnits()`, `providerWorkLimitReached()`. `workUnits()`, `tryReserve`,
   `tryConsume`, `exhausted()`, `recordExhaustedWorkStop()` keep search-only meaning.
   `permitsUnrankedAuditMaterialization()` also requires the provider limit to be unbounded and not reached.
3. `ProviderWorkBinding`: `tryReserve`, `require`, `reserveCompletion`, the completion credit and
   `remainingWorkUnits()` use the provider dimension. Opening the session still requires that the search dimension
   can admit work (`!budget.exhausted()`), so I5 holds.
4. `PackedPlanCache` (≈ line 594–599): the limits-equality reuse rule also compares `providerWorkLimit`.
5. Publish `providerWorkUnits` next to every place `workUnits` is published (find them with
   `grep -n 'workUnits()'` in core and LMDB main sources); do not rename or re-purpose existing metric names.
6. Phase-1 tests written against the unified currency (in `PackedPlanningBudgetTest`,
   `PackedPlannerResourceBudgetTest`, `PackedPhysicalRegionTest`, `PackedPhysicalStrategyContractTest`,
   `PackedPhysicalStrategyReviewFollowupTest`, `PackedRegionProviderLifecycleTest`, the continuation fixture pinned
   at allowance 55): where the test's intent is "provider work is refused deterministically", move its limit to
   `providerWorkLimit` and keep the assertion; where it pinned a combined number, re-derive it and record old and
   new values in the report. Never weaken an assertion to get green.

## Out of scope for this change

- Test rewrites for the isolated-pricing, winner-table and memory-binding groups (separate task, specs in the two
  RCA files).
- Choosing a finite default provider limit for AUTO / BUDGETED (needs measurement; roadmap phase 6).
- Reordering the incumbent pass before enumeration.
- LMDB provider changes, except call sites that must compile against the new `PackedPlannerLimits` constructor.

## Regression selection (run after each step, in the implementer's own mvnf workspace)

`PackedPlanningBudgetTest, PackedPlannerResourceBudgetTest, PackedSearchBudgetTest, PackedProviderMemoryBindingTest,
PackedRegionProviderLifecycleTest, PackedPhysicalRegionTest, PackedPhysicalStrategyContractTest,
PackedPhysicalStrategyReviewFollowupTest, PackedPlanCacheTest, PackedCacheRecipeOwnerTest,
PackedCostSessionLifecycleTest, PackedPhysicalCostContractTest, PackedSearchTest, PackedJoinOrderedPrefixTest,
PackedAccessEnablingSeedBudgetTest, PackedPlanOrderIndependenceTest, PackedBushyOutputOverlapTest,
PackedFrontierSubsetKernelContractTest, PackedInfeasibleContinuationTest, PackedMemoContractTest,
PackedCheckpointInputValidationTest, PackedCompletionResourceAdmissionTest`

Known reds that this change must leave exactly as they are (other tasks own them):
`PackedOperatorComponentRefinementTest` (2), `PackedWinnerTableTest` (4), `PackedProviderMemoryBindingTest` (2),
`PackedOptionalAlternativesTest` (2), `PackedExecutableFallbackTierTest` (1), `PackedIndependentPhysicalOracleTest`
(2), and the 177 that are also red at HEAD (list: classes with a non-zero first column in the HEAD comparison;
reports in `evidence/counterfactual-head-core-4/`).
