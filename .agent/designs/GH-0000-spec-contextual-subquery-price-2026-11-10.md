# Context-free dependent-subquery price: should a missing contextual price become unknown?

**Question.** `PackedDependentSubqueryCosting.contextualCost` and `selectedWinner` fall back to the context-free
subquery winner when a non-zero input context has no contextual winner. Should the fallback in `contextualCost`
instead return NaN and mark the comparison unavailable?

**Short answer, measured.**
- On the LMDB theme corpus the proposed fix changes nothing: its code path is never reached.
- In core tests, 99% of the fallbacks it would hit are structural cases. There the context-free winner is the plan
  that actually executes, and turning their price into NaN crashes three correlated tests.
- Recommendation: a narrow rule plus mandatory on-demand contextual costing, described in §6.

**Where things are.**
- Everything runs on my private tree copy
  `.../scratchpad/trees/measure-r4`. It was built with `mvnf --workspace measure-r4`.
- The prototype edit is saved as `measure-auto/pdsc/prototype-broad-fix.patch`, and the copy was restored afterwards.
- Harness:
  - the `PlanBudgetProbe` theme harness with a pristine APFS-cloned store, all 14 themes, 182 queries, one planning
    each;
  - mode auto (512 search units) and mode exact;
  - the base build snapshot is in `pdsc/lib-base`.
- Instrumentation: shadow classes from `pdsc/shadow-src`, for `PackedDependentSubqueryCosting`, `PackedJoinEnumerator`
  and `PackedSearchBudget`. They log:
  - every `scalarCost` call;
  - every `selectedWinner` and `recordScalarPlans` fallback;
  - every nested contextual search and its outcome.

  They can also tag candidates, run an oracle, or apply a fix variant.
- Outputs: `pdsc/out/*.log|jsonl`. Analysis: `pdsc/analyze.py`.

## 1. Reachability (measured)

| | theme corpus, auto | theme corpus, exact | exposed core tests (871) |
|---|---|---|---|
| `scalarCost` calls with input context ≠ 0 (`contextualCost`) | **0** (181 calls, all context 0) | **0** (182, all context 0) | 287,318 |
| `contextualCost` fallbacks (context ≠ 0, no contextual winner) | **0** | **0** | **2,099** |
| … structural: subquery not `inheritedPrefixSafe` | – | – | 2,074 |
| … search budget exhausted before the nested search | – | – | 16 |
| … absorbed refusal | – | – | 9 |
| … no nested search was ever run for that context | – | – | 0 |
| `selectedWinner` fallbacks (context-free winner published to the provider) | 252 events in 10 queries | 279 in 10 | 287,732 calls |
| … structural | 216 | 276 | |
| … budget exhausted before the nested search | 36 (2 queries) | 3 (1 query) | |
| `recordScalarPlans` fallbacks (context-free winner recorded as the executed dependent plan) | 20 in 9 queries | 31 in 9 | 184 calls |
| selected plan contains a fallback-priced node (tagged) | 9 queries, all structural | 8, all structural | n/a |

**Why `contextualCost` is never reached on LMDB.** `costScheduledFilterImplementation` calls `contextualCost` only when
`!estimate.dependentSubqueriesCosted()`. LMDB's semi/anti providers set that flag (`LmdbScalarSemiAntiCosting:77`,
`LmdbMappedSemiAntiCosting:249`) and price the dependent side themselves. They use `estimateCorrelatedFactor`,
max-combined with the published dependent inputs.

**Callers in core.**
- `costDenseCorrelatedFilterTransitionCandidate` (the lattice transition): 1,998.
- `offerCanonicalCorrelatedPredicate` (≈3796): 95.
- `emitScheduledFilterOrder`: 6.

**Callers on LMDB.** All `selectedWinner` and `recordScalarPlans` fallbacks come from `offerCanonicalCorrelatedPredicate`.
There were no absorbed refusals: the provider-work limit is unbounded by default.

## 2. Effect of the proposed fix

The broad fix returns NaN and marks the comparison unavailable whenever context ≠ 0 and there is no contextual winner.

**Theme corpus.** Same harness, both modes.
- Selected plans changed (join order, index or algorithm): **0 of 182**.
- Plans that became unpriced: **0**.
- Comparison-completeness changes: **0**.
- Search-completion changes: **0**.
- Search and provider work units: identical. Auto: 40,818 and 818,440. Exact: 379,107 and 6,701,185.
- Planning time: 29.7 s → 33.0 s total in auto. That is within run-to-run noise on a busy machine.
- No plan changed, so there is nothing to list.

A publish-path analogue was also tried: do not publish the context-free winner when the shape is eligible but the
search was cut. It also gave **0 plan changes** in both modes.

**Core.** The exposed classes are `PackedPhysicalCostContractTest`, `PackedSearchTest`, `PackedPlanningBudgetTest`,
`PackedExistsReferencedDependenciesTest`, `PackedOptionalAlternativesTest`, `PackedIndependentPhysicalOracleTest`,
and every class whose name contains Correlated, Exists or DependentSubquery: 15 classes, 871 tests. They ran via mvnf
in the copy before and after the edit, and the shadow runs agree.
- Before: 20 failures, all pre-existing in `ExistsFilteredProbePreparationTest`.
- After: 24 failures. The four new reds:

| test | message | judgement |
|---|---|---|
| `PackedSearchTest#correlatedNotExistsRunsWhenItsOuterBindingsAreAssured` | `PackedMemoInvariantException: correlated DPhyp target was unreachable` | **Real loss**: planning throws. |
| `PackedSearchTest#correlatedNotExistsSchedulingIsIndependentOfSyntacticJoinOrder` | same | Real loss. |
| `PackedSearchTest#correlatedDenseSearchCostsEquivalentContinuationInRetainedState` | same | Real loss. |
| `PackedPlanningBudgetTest#expiredDeadlinePricesTheCanonicalCorrelatedPredicateContextually` | "the canonical candidate must be the priced incumbent" | Its *invariant* (a priced canonical incumbent past the deadline) is valid. The price it got before came from the context-free fallback after the deadline cut the optional nested search. Real loss unless option (a) is added. |

The three crashes come from structural cases. Every correlated-lattice transition through such a filter becomes NaN,
so no path reaches the target state.

**Narrow variant.** NaN only when the reason is *not* structural. Run over the same 871 tests with shadow classes:
**+1 red**, only `expiredDeadline…`. The DPhyp crashes disappear.

## 3. How wrong is the context-free price? (oracle: rerun the nested contextual search with unlimited budget at the fallback point)

**Core.**
- 2,093 fallback events: 2,089 cannot be resolved even with unlimited budget, because no contextual search exists for
  structural shapes.
- The 4 resolvable events: one is exact (122 against 122). Three are **1.0 context-free against 1000.0 contextual**,
  that is 1000× too low. In those, the context-free winner does not depend on outer bindings, so it is charged once.
- Each oracle costs 1 search unit and 82–174 µs.

**Theme publish path.**
- 6 non-structural events; 2 are resolvable. One is equal (30,002).
- In the other, the context-free winner is 30,002 per invocation, while the contextual winner costs 3.77e26 for
  1.89e26 invocations (about 2 per invocation). Context-free is about 15,000× **higher** there.
- Each oracle costs 1 unit and 52–142 µs. The other 4 "budget" events cannot be resolved with unlimited budget either:
  0 units, and the nested search returns at once.

**Conclusion.** The error is large when it exists, but **not systematically in one direction**, and the sample is tiny
(n = 6). In every resolvable core case it is lower, which is the bias the review described.

## 4. Alternatives

**(a) On-demand mandatory contextual search.** When an *eligible* shape has no contextual winner, run the nested search
for that context.
- **Measured cost:** 1 search unit and about 0.05–0.17 ms per event. That is 6 events on the whole corpus and 4 in the
  core suite; negligible.
- **Sound:** it produces a real contextual price.
- **What it takes:**
  - Run it only for the mandatory canonical candidate (`offerCanonicalCorrelatedPredicate`), inside the budget-stop
    spec's mandatory-completion scope. Today `contextualizeEmbeddedSubqueries` deliberately suspends that scope.
  - Give the nested enumerator a bounded mandatory admission: a leaf costs 1 unit, and a join subquery uses a single
    order rather than DPhyp.
  - Optional candidates keep NaN when cut.
- This keeps `expiredDeadline…` green honestly.

**(b) Keep the context-free price as a bound that cannot beat a contextual price.**
- **Not sound as a bound.** The data shows it can be 1000× low or 15,000× high, so it is neither a lower nor an upper
  bound.
- As a pure ranking demotion (a separate tier), it is policy-sound. But it still exposes a wrong number whenever it is
  the only candidate, and the tier interactions are known to cause regressions (E follow-up, V2).
- Not recommended.

## 5. Core tests: summary

See the table in §2. The 20 pre-existing `ExistsFilteredProbePreparationTest` failures are unchanged in every run.

## 6. Recommendation

**Do not adopt the fix as stated.**
- On LMDB it is unreachable, so the production benefit is zero.
- In core it turns 2,074 structural fallbacks into NaN and crashes correlated DPhyp.

**Structural cases** (subquery not `inheritedPrefixSafe`, for example a variable-scope change). No contextual plan can
exist. `recordScalarPlans` records the context-free winner as the dependent plan that runs once per outer row, and the
repeated-cost model in `scalarCost` prices exactly that plan. "Isolated work cannot price a bound input" does not
apply, because there is no bound-input plan to price. This is inferred from the code.

**Adopt instead:**
1. **The narrow rule.** In `scalarCost`, when context ≠ 0 and there is no contextual winner, return NaN *only if* the
   subquery is contextually eligible. Use the same predicate the nested search uses: `inheritedPrefixSafe` plus the
   supported operator shapes. Mark the comparison in the caller.
   - Apply the same eligibility rule in `selectedWinner` (publish nothing) and in `recordScalarPlans`, so that price,
     published inputs and the executed plan agree.
   - That makes an unpriced candidate displaceable by a later priced one, because priced beats unpriced. The review's
     "cheaper winner can never be displaced" problem goes away for eligible shapes.
2. **Option (a)** for the mandatory canonical candidate, so that cut nested searches do not unprice the incumbent.
3. **Make the correlated DPhyp tolerate unpriced transitions** without "target unreachable". Inferred risk: under the
   narrow rule the exposed tests did not crash, but an eligible-but-cut lattice could still have no priced path.

**Tests first.**
- **T1.** `PackedDependentSubqueryCostingTest#cutContextualSearchLeavesEligibleBoundSubqueryUnpriced`: the review's
  7.0-against-1,006.0 fixture, with the nested search cut by the work limit. Expect a NaN price and `UNAVAILABLE_COST`.
  Red today.
- **T2.** `#laterContextualPriceDisplacesTheCutCandidate`. Red today.
- **T3.** `#structurallyUnsafeSubqueryKeepsItsRepeatedContextFreePrice`: a guard against the broad fix. Green today.
- **T4.** The three `PackedSearchTest#correlatedNotExists…` and `correlatedDenseSearch…` tests must stay green.
- **T5.** Keep `PackedPlanningBudgetTest#expiredDeadline…` green; it needs (a). Add a companion that asserts the
  canonical dependent price past the deadline comes from a contextual winner.

**Measured vs inferred.**
- Measured: every count, the plan and work comparisons, the oracle ratios and costs, and the test statuses.
- Inferred: that structural cases execute the context-free plan, and therefore that its price is honest; the cost and
  effort of implementing (a); the residual lattice risk under the narrow rule.
