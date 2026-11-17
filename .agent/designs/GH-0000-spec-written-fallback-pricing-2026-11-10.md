# Spec: written-order JOIN fallback and comparison completeness

Status: investigation and prototypes only. Nothing under the repository or `scratchpad/trees/` was touched. All
prototypes are scratch shadow classes compiled against the snapshot jars in `measure-auto/lib`. Scratch root, called
`RCA` below: `/private/tmp/claude-501/-Users-havardottestad--claude-spares-spare-5bPeTt/157f627d-35d8-4517-9d49-1f48286fcd13/scratchpad/rca/`.

**Bottom line (verified by running).**
- The proposal as stated ("compose the written JOIN with the enumerator's exact-context result") cannot work by lookup:
  0 of 2717 lookups found an enumerator result for the incumbent's exact context.
- If the context is instead costed afresh, completeness improves (59 queries become COMPLETE), but the selected plan
  changes for 17 queries. Two of those changes are severe cost regressions. A costed-tier variant also crashes two
  plannings.
- A different rule is plan-neutral by construction and reaches every query this class can reach (49 of 49):
  *tier dominance* in `PackedMemo.planQualityDraft`. It is specified below as the recommended change, with the
  soundness argument and the owner decision it needs.
- Even with it, LMDB decision-cache hits are capped by a separate capacity problem: the pipeline decision cache is
  4 MiB by default, about 1 MiB per family, and saturation turns into bypass without eviction. That is listed as risk R1
  and needs its own fix.

---

## 1. Classification (exact mode, all 14 themes, 182 queries, two plannings each)

Harness: the coordinator's `PlanBudgetProbe` (`measure-auto/harness`), driven by my own runner `RCA/myrun.sh`. It
uses an APFS clone of `stores/pristine`, `-Drdf4j.optimizer.lmdb.cascades.mode=exact`, and the shadow classes in
`RCA/classify-classes`. Every `markUnavailableCostComparison()` call site is logged, and every unpriced root-decision
candidate is classified by its NaN origin; the classifier walks down through unpriced child winners to the first node
whose children are all priced. Analysis script: `RCA/analyze_classify2.py`. Log: `RCA/theme-out/classify2.log`.

The unmodified working tree reproduces the coordinator's numbers: 145 UNAVAILABLE_COST, 31 COMPLETE, 6 not Cascades.
On the second planning, 131 are FRESH and 14 are PACKED_CACHE; 28 of the 31 COMPLETE queries hit the pipeline cache.

**Root-decision unpriced candidates, by kind** (a query can have several):

| kind | queries | origin (operator#implementation) | tier |
|---|---|---|---|
| written-order JOIN fallback, refused by `refineWrittenJoinCandidate` | 112 (106 alone) | `JOIN#1` (LOGICAL_IMPLEMENTATION) | EXECUTABLE_FALLBACK in all 333 instances; selected in 17 |
| OPTIONAL written fallback without contextual evidence | 15 | `LEFT_JOIN#1` | COSTED |
| unpriced parent over priced children | 3 | `FILTER#1` | EXECUTABLE_FALLBACK |

**Other sites that mark the comparison, by query.** These are not root candidates. They are found by logging every
`markUnavailableCostComparison()` caller, and they keep a query UNAVAILABLE no matter what happens at the root.

| site | meaning | queries |
|---|---|---|
| `PackedIncumbentSearch.offerSelectedLogicalImplementation:821` | OPTIONAL right side has no contextual evidence (`UNAVAILABLE_OPTIONAL_CONTEXT`) | 66 |
| `PackedWinnerTable.offerHashedWithMetadata:237` | NaN total offered on the COSTED tier (mostly the same OPTIONALs) | 61 |
| `PackedJoinEnumerator.refinePhysicalJoinCandidate:10674` | enumerated dependent join whose provider reported unknown factor work | 45 |
| `PackedCascadesPlanner.compute:1174` | the selected root itself is unpriced | 16 |
| `PackedCostOrderedStateQueue.offer:60` | an enumeration state has a NaN cost | 11 |
| `PackedJoinEnumerator.continueRetainedFactorUnderPrefix:7281` | a continuation has unknown factor work | 8 |
| `PackedWinnerTable.appendContinuationWithMetadata:203` | an unpriced continuation was appended | 3 |
| `PackedJoinEnumerator.costDenseInheritedSingleton:5762` | an inherited singleton has unknown work | 1 |

**Reach of a fix that touches only the written-order JOIN fallback.**

| class | queries | description |
|---|---|---|
| A | **49** | the only unknowns are unpriced written-JOIN fallbacks at the root. Fixable. |
| B | 63 | written-JOIN fallback plus other unknowns. Stays UNAVAILABLE. |
| C | 33 | no written-JOIN involvement. Stays UNAVAILABLE. |

Per theme (A/B/C): ADAPTIVE 13/0/0, ANALYTICS 2/0/0, DATA_TRANSFORMATION 4/1/5, ELECTRICAL 5/6/2, ENGINEERING 4/5/2,
EXPLORATION 0/0/4, HIGHLY_CONNECTED 0/7/5, LIBRARY 4/7/2, MEDICAL 2/8/1, PHARMA 5/3/2, REAL_ESTATE 2/6/4,
SOCIAL_MEDIA 2/7/1, SPARSE 0/10/2, TRAIN 6/3/3.

MEDICAL in detail:
- q7 and q10 are class A.
- q0, q1, q3, q5 and q9 carry enumerated dependent joins with unknown factor work and/or OPTIONAL right sides without
  contextual evidence.
- q4, q11 and q12 have OPTIONAL right sides without contextual evidence.
- q2 has both an OPTIONAL written fallback and a written JOIN fallback at the root.

What remains after any written-JOIN fix, by count of affected queries:
- OPTIONAL right sides without contextual evidence: 66.
- Genuinely unknown provider work on *distinct* enumerated dependent alternatives: 45 + 8 + 11 + 1.
- Unpriced selected roots: 16.
- Unpriced FILTER parents: 3.

These are separate problems. The second group is the design working as intended: a physically distinct alternative
is unpriced.

## 2. Design analysis

### 2a. Composing with the enumerator's exact-context result (the proposal)

**Where "exact context" comes from.** It is the interned evidence context `PackedMemo.internEvidenceContext(...)`. Its
key is:
- the prefix relation *sequence* (collapsed to its exact-extensional state when
  `evidenceStates().isExactExtensional`);
- the per-relation contribution rows;
- the prefix rows;
- the evidence state id;
- the binding layout id;
- the correlation mask id;
- the semantic scope mask id;
- the input cost-event id.

The enumerator stores contextual factor winners under that id: `contextualizeLeaf` at `PackedJoinEnumerator:2590`
uses `memo.findWinner(group, any, 0, inputContextId, 0)`, and the factor-append path at `:8178` uses
`offerContextualFactorWinner`.

The incumbent's written JOIN builds its context differently, in `contextualizeJoinRight` (`PackedIncumbentSearch:1858`):
- the prefix comes from `collectPhysicalPrefixRelations(leftWinner)`;
- contribution rows are null;
- the binding layout and the correlation mask are both `relAssuredMaskId(left)`;
- the cost event is the left winner's.

**Measured.** A lookup-only prototype (`RCA/fix4-src`, `-Drca.fix.lookupOnly=true`) interned the incumbent's context
and looked it up: **0 of 2717** written-JOIN lookups found an enumerator result. Pure reuse is therefore impossible
unless context construction is unified across both paths.

**Costing afresh under the written context.** The alternative is to call `contextualizeJoinRight`, which runs
`optimizeWithInheritedPrefix` (lookup first, otherwise one provider costing) and then re-run
`refineWrittenJoinCandidate` with the contextual right-side winner. That is what the `!exploreReorderings` path
already does (`PackedIncumbentSearch:847-855`).

**Prototype V1:** drop the `!exploreReorderings` gate (`RCA/fix-src`).
- 57 queries move UNAVAILABLE→COMPLETE.
- **22 selected plans change.**
- **2 plannings fail.** LIBRARY q9 throws "invalid packed candidate row". SPARSE q6 throws "winner costs must be finite
  … startup=Infinity".
- The core suite gains **32 new failures** (checkpoint/resume, audit and continuation tests).
- Rejected.

**Prototype V2:** reprice only when `refineWrittenJoinCandidate` returned NaN, and keep the result on the
EXECUTABLE_FALLBACK tier (`RCA/fix2-src`).
- No failures. 59 queries move UNAVAILABLE→COMPLETE. 86 of 90 COMPLETE queries hit the pipeline cache on the second
  planning (default budget).
- **17 selected plans change** relative to the unmodified run:
  - 6 change join order: ELECTRICAL_GRID q3, HIGHLY_CONNECTED q10, SOCIAL_MEDIA q5, SPARSE q0, q5, q10.
  - 11 keep the same order but change physical metrics: algorithm, index, lookup components.
- Two are severe regressions. SPARSE q0's root cost goes from 2.13e6 to 6.7e28, and SPARSE q5's from 1.76e6 to 4.5e37.
- HIGHLY_CONNECTED q3 goes from 848,601 to 1,488,543, with an independent-hash join replaced by dependent iteration.
- 10 events show the written order costed *cheaper* than the costed winner of the same group. That is direct evidence
  that the two contexts price the same strategy differently.

Mechanism (inferred from the code and the regressions):
- Candidates composed over an executable-fallback child are themselves offered on the EXECUTABLE_FALLBACK tier
  (`PackedMemo:1003-1004`, `hasExecutableFallbackChild`).
- `PackedWinnerTable.candidateIsBetter` (`:619-627`) ranks priced over unpriced first, then COSTED over
  EXECUTABLE_FALLBACK, then cost.
- Once the repriced fallback makes a previously unpriced COSTED-tier ancestor priced, tier precedence selects it over
  a cheaper, priced, fallback-tier plan.
- `contextualizeJoinRight` also publishes contextual winners into the memo, and later passes can reuse them.

Conclusion: pricing the written fallback is **not plan-neutral** under the current tier rules. It changes plans in a
way that can be much worse. It should not land as the fix for this class.

### 2b. Recommended: tier dominance at the root decision

**Rule.** In `PackedMemo.planQualityDraft`, the loop at ≈2584-2587 currently calls
`budget.markUnavailableCostComparison()` for every unpriced root candidate. Do not mark when **all** of the following
hold:
1. the candidate's `decisionTrace.comparisonTier(index) == PackedWinnerTable.EXECUTABLE_FALLBACK`;
2. the candidate is not the policy-selected decision (`!decisionMatchesWinner(index, selectedDecisionWinnerId)`);
3. the policy-selected candidate of the same root decision has finite, non-negative `candidateCost` and `lowerBound`,
   and is on the `COSTED` tier.

The candidate is still counted as unranked and still sampled for audits: no other bookkeeping changes.

Implementation: pre-scan the root decision once, with `sameDecision(index, representative)` and
`decisionMatchesWinner`, to find the selected candidate's tier and price before the existing loop. This is the shadow
`RCA/fix5-src/.../PackedMemo.java`, about 20 lines.

Record the exemption per trace index as a `tierDominated(traceIndex)` accessor on `PackedPlanQualityDraft`, and
consult it in two more places that mark the same candidate after materialising it. Without them, completeness depends
on the limits (§3c):
- the unranked-alternative loop of `PackedCascadesPlanner.buildOptimizationDecision` (≈1431-1434);
- the audit loop of `buildPlanQualityAudit` (≈1584-1586).

In both loops, skip the mark for a tier-dominated index and still `continue`, so the candidate is not ranked. This
part was not prototyped.

**Why it is sound.**
- Comparison completeness certifies that the selection policy's decision is fully determined by available prices.
- Under `candidateIsBetter`, a candidate on the EXECUTABLE_FALLBACK tier can never outrank a priced COSTED-tier
  candidate, priced or not. If priced, the tier comparison loses; if unpriced, the priced-first rule loses.
- So the unknown price of such a candidate cannot change the selection. Exempting it removes no information the
  policy could use, and the selected plan, its price and its lower bound are unchanged by construction.

**When it can still report COMPLETE with a physically distinct executable alternative unpriced.** Exactly when that
alternative is on the executable-fallback tier and a priced costed plan was selected. That is the tier policy's own
choice, not a gap in comparison. The roadmap's sentence (ExecPlan l.629-630) is "an unpriced *relevant* alternative
prevents a cheapest-plan certificate". This rule defines "relevant" as "able to outrank the selected candidate under
the policy". **Owner decision needed:**
- If COMPLETE must instead mean "every executable plan priced", this rule is wrong, and only real pricing (2a, after
  unifying contexts and revisiting tier precedence) can deliver it.
- If the certificate semantics are "cheapest under the ranking policy", the rule is exact.

**What it cannot affect.**
- Unpriced COSTED-tier candidates: OPTIONAL fallbacks, enumerated dependents.
- An unpriced or fallback-tier selected plan.
- Every non-root marker in the table above: OPTIONAL context, enumerated unknown work, state queue, continuations,
  `compute:1174`.

All of these still mark.

**Physical identity.** Unchanged. The fallback keeps its own recipe identity: fingerprint `f7cc70a5…` in the oracle
fixture, distinct from the enumerated `33692a66…`. Nothing is deduplicated or merged, and no plan or annotation
changes.

**Tier and ranking.** Untouched. This is a pure certificate change.

**Budgets.** No provider callbacks and no search work. The pre-scan is O(root-decision candidates); about 2-30 per
query in these runs.

**Composition with the budget-stop spec.**
- That spec's I6 marks unpriced parents at *offer* time (`markUnavailableCostComparison()`). That is a non-root site,
  so it still marks. This rule never certifies a stopped search: `searchCompletion` stays separate, and BoundKind
  needs `EXACT_COMPLETE`.
- With provider work on its own limit, nothing here consumes it.

### 2c. Strict physical-identity dedup in `planQualityDraft` / `buildOptimizationDecision`

Rejected as an additional step, because it never fires for this class. The written fallback's `PhysicalIdentity`
always differs from the enumerated twin's: implementation form `LOGICAL_IMPLEMENTATION` (1) against
`JOIN_IMPLEMENTATION` (2), plus access/lookup fields (verified in the E follow-up). Making it fire would mean loosening
identity, which is the withdrawn candidate-equivalence check (ExecPlan l.518-523). Keep the existing ranked-alternative
dedup in `buildOptimizationDecision:1426-1430` as it is.

## 3. Prototype evidence

### Core probe (`RCA/src/.../RcaCommonJoin.java`, providers with explicit physical costs)

Run against the unmodified classes and against `RCA/fix5-classes`. Outputs: `RCA/common-base.txt` and
`RCA/common-fix5.txt`.

| shape | explicit leaf only | explicit leaf + contextual | explicit leaf + unknown contextual work (distinct alternative unpriced) |
|---|---|---|---|
| bound 2-pattern | UNAVAILABLE → COMPLETE | UNAVAILABLE → COMPLETE | stays UNAVAILABLE |
| 3-pattern chain | UNAVAILABLE → COMPLETE | UNAVAILABLE → COMPLETE | stays UNAVAILABLE |
| 3-pattern star | UNAVAILABLE → COMPLETE | UNAVAILABLE → COMPLETE | stays UNAVAILABLE |

- Totals, physical fingerprints and plans are byte-identical to the unmodified run; the files differ only in the
  `cmp=` field.
- Scalar providers and disconnected joins: COMPLETE before and after.

### Theme harness, default LMDB configuration (`RCA/theme-out/fix5.jsonl` against `classify2.jsonl`; `RCA/compare_runs.py`)

- **49 UNAVAILABLE→COMPLETE**, which is exactly class A. 96 stay UNAVAILABLE; 31 are COMPLETE before and after.
- **0 of 176 selected plans differ.** Both `physicalSha` (join order, algorithms, index, lookup components) and
  `compactSha` were compared.
- No planning errors.

Second planning:
- 29 PIPELINE_CACHE, 42 FRESH and 9 PACKED_CACHE among the 80 COMPLETE queries.
- 21 of the 49 newly COMPLETE queries were published and hit. 28 were never published.
- Publication tracing (`RCA/lmdbshadow-src`, log `RCA/theme-out/fix5pubAll2.log`) shows why the 28 failed:
  - 190 `bypass` events. `decisionCache.tryRetainTransientIdentity` refuses once the decision cache retains about
    4.19 MB in 3 families. The pipeline budget is `frontierCacheEvidenceBudgetBytes / 16`, which is 64 MiB / 16 =
    4 MiB (`LmdbFrontierPlannerSettings:27-33`).
  - Families retain about 1 MB each, although `entryBytes` is about 60 KB.
  - A bypass never publishes and so never evicts, so the cache stays saturated.
- In isolation, the newly COMPLETE queries publish and hit. That was checked for 4 queries (ADAPTIVE q0, q1;
  ANALYTICS q7; MEDICAL q7): all `publishBuild=true` and second planning `PIPELINE_CACHE`.

Large-budget runs (`-Drca.evidenceBudget=1 GiB` via a shadow `ProbeConfigs`), unmodified against tier dominance:
see §3b.

### 3b. Large cache budget

`-Drca.evidenceBudget=1073741824` raises the pipeline budget to 64 MiB. It was applied identically to the unmodified
classes and to V5. Outputs: `RCA/theme-out/baseBig.jsonl` and `RCA/theme-out/fix5Big.jsonl`.

- **49 UNAVAILABLE→COMPLETE, and 0 of 176 selected plans differ.**
- Second planning: **all 49 newly COMPLETE queries are PIPELINE_CACHE hits** (79 of 80 COMPLETE overall; 30 of 31
  before the change).
- The larger budget alone changes no plan: unmodified at the default budget against unmodified at the large budget
  gives 0 plan differences.

### 3c. Core packed-package suite (104 classes, 1807 tests, `RCA/run-packed.sh`)

The JUnit Launcher ran over test classes compiled read-only from the repository. Outputs: `RCA/packed-*.txt`.

**Unmodified baseline:** 28 failures. These are the known reds, plus `OptimizerRuleCatalogTest`, which cannot find the
repository root from the scratch class path.

**V5 (tier dominance) adds 6 failures:**
- `PackedPhysicalCostContractTest#knownDependentWorkStillCompetesWithIndependentAlternatives`: pins
  `UNAVAILABLE_COST` and `BEST_KNOWN_UNBOUNDED`. The only unpriced root candidate there is the tier-dominated written
  fallback. Old-rule pin (b).
- `PackedSearchTest#unrankedAuditMaterializationRequiresAnExplicitSampleAndUnboundedOwner` [2] work, [3] deadline,
  [4] memory: the first assertion pins `UNAVAILABLE_COST` as a precondition for "an unranked candidate exists". The
  intent is carried by `omittedUnrankedCandidateCount() > 0`. Old-rule pin (b).

  Case [1] (unbounded, `samples = 0`) stays UNAVAILABLE under the draft-only prototype. jdb-free tracing shows the
  marker comes from the alternative loop in `buildOptimizationDecision` (≈1431-1434), which materialises the unranked
  fallback when `permitsUnrankedAuditMaterialization()`. **The exemption must therefore also be applied in that loop,
  and in the audit loop at ≈1584-1586.** Otherwise completeness depends on the limits.
- `PackedFrontierSubsetKernelContractTest#arbitraryWidthBushyDependentJoinContinuesRightRecipeUnderWholeLeftPrefix` and
  `#sparseLongBushyStateContinuesThroughLegalSingletonSuffix`: in-test timeouts while four JVMs ran concurrently.
  Rerun quietly, both pass with and without V5. Timings were 32 s against 28 s, and 81 s against 62 s.

**V2 (repricing) adds 8 failures:** the same pins, plus
`PackedSearchTest#dependentBushyAlternativeWithoutContextualAuthorityRemainsUnpriced`.

**V1 adds 32**, including about 20 checkpoint/resume/refresh tests.

## 4. Tests first

The red-today claims for T1, T4 and T5 are verified with the scratch prototypes. The test classes themselves were not
written.

**T1. New `PackedComparisonCompletenessTest#explicitPhysicalConnectedJoinsReportCompleteComparison`**, parameterised:
- shape ∈ {bound two-pattern, three-pattern chain, three-pattern star};
- provider ∈ {explicit leaf only, explicit leaf plus contextual bound quotes}, as in `RCA/src/.../RcaCommonJoin.java`.

Assert `exactComplete()`, `totalPrice().isPresent()`, `comparisonCompleteness() == COMPLETE`,
`boundKind() == VALID_LOWER_BOUND`, and `searchLowerBound() <= totalCost()`.

Red today: `UNAVAILABLE_COST` (verified).

**T2. Same class, `#unpricedDistinctDependentAlternativeKeepsComparisonUnavailable`.** The provider reports unknown
contextual work for the bound right side (`setLocalPhysicalCostPreservingUnknown(NaN, …)`). Assert
`UNAVAILABLE_COST` for all three shapes. Green today; it is a guard.

**T3. Same class, `#selectedExecutableFallbackKeepsComparisonUnavailable`.** When the selected root is itself the
unpriced or fallback-tier written order (no priced alternative), assert `UNAVAILABLE_COST`. Green today; it is a
guard, and `compute:1174` covers it.

**T4. `PackedMemoContractTest#tierDominatedUnpricedFallbackDoesNotDegradeTheRootComparison`.** A memo with plan-quality
state has one root decision containing a priced COSTED winner and a NaN EXECUTABLE_FALLBACK candidate. Assert that
`planQualityDraft` leaves `hasUnavailableCostComparison()` false. Two variants must still mark:
- a NaN COSTED-tier candidate;
- a fallback-tier *selected* candidate.

Red today for the first case.

**T5. `PackedSearchTest#auditAndPlanningAgreeOnComparisonCompleteness`.** Run the T1 query once through
`auditPlanQuality(…, samples = 1, unbounded())` and once through normal planning. The completeness must be equal.
Red under a draft-only implementation; this is the reason for the extra loops in §2b.

**T6. LMDB, new `LmdbPipelinePlanCacheDecisionHitTest#connectedTwoPatternJoinIsServedFromTheDecisionCacheOnSecondPlanning`.**
- Open a real `LmdbStore` in a `@TempDir` with the default config and add a few hundred triples: `?s :p1 ?k`,
  `?k :p2 ?o`.
- Plan `SELECT ?s ?o WHERE { ?s :p1 ?k . ?k :p2 ?o }` twice through the same path as `PlanBudgetProbe`:
  `LmdbBenchmarkQueryPlan.openPreparation(...).optimize()` or `SailTupleQuery.explain(Optimized)`.
- First planning: assert `optimizer.cascadesCostComparisonCompleteness == COMPLETE`, and that the decision-cache
  `statistics().publications()` increased by 1.
- Second planning: assert `optimizer.pipelinePlanCacheHit == "true"`, or receipt path `PIPELINE_CACHE`.

Red today: the summary is UNAVAILABLE and `publishDecision` refuses. Inferred from the theme runs; the test was not
written.

**T7. LMDB, separate task R1: `#saturatedDecisionCacheStillAdmitsANewFamily`.** Fill the pipeline budget, then plan a
new COMPLETE query and assert that it is published, with an eviction if needed. Inferred red: 190 bypasses with no
eviction were observed.

**Existing tests to rewrite (pins of the old rule):**
- `PackedPhysicalCostContractTest#knownDependentWorkStillCompetesWithIndependentAlternatives`: expect `COMPLETE` and
  `VALID_LOWER_BOUND`. Keep the price-present and dependent-iteration assertions.
- `PackedSearchTest#unrankedAuditMaterializationRequiresAnExplicitSampleAndUnboundedOwner` (all 4 cases once the full
  exemption lands): replace the `UNAVAILABLE_COST` precondition with `COMPLETE`. Keep the unranked-omission and
  admission-reason assertions.

**Must stay green.** All of these were verified green under V5 in the full package run:
- the `PackedPhysicalCostContractTest` unknown/isolated pins (`unknownDependentWork*`, `unknownOptionalRightWork*`,
  `isolatedOptionalRightWork*`, `mixedOptionalInputDoesNotCertifyAnIsolatedRightPrice`, `aFullyPricedSingleAccess…`);
- `PackedSearchTest#dependentBushyAlternativeWithoutContextualAuthorityRemainsUnpriced`;
- the checkpoint/resume/refresh tests in `PackedCheckpointInputValidationTest` and `PackedPlanCacheTest`;
- `PackedExecutableFallbackTierTest`;
- `PackedWinnerTableTest#costedTierStillPrecedesFallbackWhenOnlyCostedMemoryIsUnknown`.

## 5. Risks

**R1. Decision-cache capacity, verified and the largest effect.**
- At the default 64 MiB evidence budget, the pipeline decision cache gets 4 MiB.
- Families retain about 1 MB each (while `entryBytes` is about 60 KB), so it saturates after 3 or 4 families.
- `tryRetainTransientIdentity` then refuses, the pipeline bypasses, and a bypass never publishes or evicts. The cache
  stays stuck.
- Result: with the default budget only 21 of the 49 newly COMPLETE queries hit; with a large budget all 49 do.
- A separate fix is needed: evict on admission pressure, or account the retained per-family evidence. Without it, the
  completeness fix delivers less than half its value in the default configuration.

**R2. Semantics, owner decision.** COMPLETE becomes "cheapest under the ranking policy". An unpriced executable-fallback
alternative that the policy can never select no longer blocks the certificate (ExecPlan l.629-630, "relevant"). If the
owner wants "every executable plan priced", reject V5. Then only real pricing works, and it first needs unified
evidence-context construction (0 of 2717 overlap today) and a tier-precedence review (the V2 regressions).

**R3. Hot path.**
- The rule itself costs an O(root candidates) pre-scan, and nothing else.
- Newly COMPLETE queries no longer short-circuit `buildOptimizationDecision` (≈1393/1403). They now materialise the
  challenger portfolio, as COMPLETE queries already do.
- Deterministic work units are identical across all 176 queries (verified).
- Wall time is inconclusive: within-run noise is ±20%, and the two run pairs disagree (median ratio 1.33 against
  0.96). Measure it with the JMH theme benchmark.

**R4. Resumed enumeration and checkpoints.** No search, memo or checkpoint state changes. However, these queries now
reach paths that UNAVAILABLE previously blocked:
- `supportsStaleValidation`, which runs realized-regret stale validation (`PackedCascadesPlanner:400-404`);
- family/structural reuse (`PackedPlanCache:585-639`);
- durable publication under bounded deadlines (`:457-463`).

Their tests pass, but they now see many more real queries.

**R5. LMDB consumers.** These are newly active for about 49 of 176 queries:
- `publishDecision` publishes;
- `LmdbDetachedPlanRefresh` (≈240) refreshes in the background instead of throwing;
- `LmdbPlanDecisionCache.observeExecution` records champion/challenger observations, enabling regret feedback and
  learning.

No consumer was found that relied on the refusal. However, the cross-query feedback poisoning history in this
repository (memory notes) makes more learned feedback a known risk area; watch refresh and observation behaviour on
the theme benchmark.

**R6. Composition with the budget-stop spec.**
- I6 marks an unpriced parent at offer time, which is a non-root site, so it still marks.
- `searchCompletion` stays separate. `VALID_LOWER_BOUND` still requires `EXACT_COMPLETE`.
- No provider work is consumed.

Land V5 after that spec. Its step 1.3 may add more executable-fallback parents at the root, and those correctly stay
exempt only when a priced COSTED winner was selected.

**R7. The rejected pricing variants.** Do not land V1 or V2 as they stand. They are kept in `RCA/fix-src` and
`RCA/fix2-src` as evidence:
- plan changes in 22 and 17 queries, including root-cost regressions to 6.7e28 and 4.5e37;
- two planning exceptions (V1);
- extra provider callbacks;
- memo side effects through `contextualizeJoinRight`.

## Verified against inferred

**Verified by running:**
- every count in §1 and §3;
- the 0 of 2717 lookup result;
- plan identity for V5;
- the decision-cache saturation trace;
- the isolated publication of 4 queries;
- the package-suite diffs;
- the quiet timeout reruns.

**Inferred from code:**
- the V2 regression mechanism (tier precedence plus memo side effects);
- that the extra loops in §2b remove the limit dependence (tracing located the marker, but that change was not
  prototyped);
- T6's red/green behaviour;
- R5 behaviour beyond publication.
