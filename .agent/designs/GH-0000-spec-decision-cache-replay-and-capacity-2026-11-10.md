# Spec: replay-only decision-cache admission (Part 1) and decision-cache capacity (Part 2)

**Status.** Investigation and scratch prototypes only. Nothing under the repository or `scratchpad/trees/` was
touched, and no mvnf build was run from the repository sources.

**Scratch root.** `RR` = `/private/tmp/claude-501/-Users-havardottestad--claude-spares-spare-5bPeTt/157f627d-35d8-4517-9d49-1f48286fcd13/scratchpad/rca-replay/`.
- Shadow sources: `RR/src`. They are copies of `LmdbPipelinePlanCache`, `LmdbPlanDecisionCache` and a new `RcaTrace`.
- Runner: `RR/run.sh`. It uses the coordinator's theme harness, `PlanBudgetProbe`, the pristine 16.5M-triple store
  and the default LMDB configuration (no forced cascades mode).
- Outputs: `RR/out/*.jsonl` and `RR/out/*.log`.
- Analysis scripts: `RR/analyze.py` and `RR/sizes.py`.

**Source identity.** Before shadowing, I compiled unmodified copies of `LmdbPipelinePlanCache`, `LmdbPlanDecisionCache`
and `LmdbDetachedPlanRefresh` against `measure-auto/lib`. Their `javap -p -c` output is byte-identical to the
snapshot classes, so the prototypes differ from the measured build only by the edits described below. The
repository sources of these three files were still identical to my copies at write-up.
`LmdbOperatorFeedbackStats` has since been modified in the working tree by another agent, so lines in that file are
cited by method name.

**Labels.**
- **[run]**: verified by running code.
- **[read]**: verified by reading code; line numbers are for the current working tree unless marked HEAD.
- **[inferred]**: reasoned, not executed.

## Bottom line

1. **Replay-only admission is sound and is already the design of the core cache [read].**
   - An exact LMDB hit requires every versioned planner input to be unchanged:
     - the data, Frontier, LEO/learned and predicate-range revisions;
     - the configuration (planner system properties);
     - the dataset, bindings, structural source and evaluation flags.
   - The plan is published only if those revisions did not move during planning.
   - Re-planning is deterministic **[run]**: across 6 runs × 182 queries × 3 plannings, the physical plan was identical
     on every attempt, including 131–132 full recomputations per baseline run.
   - The core `PackedPlanCache` already serves unpriced exact-source recipes as replay-only entries: same limits, no
     deadline, no family reuse, no stale validation.
   - **One exception:** a search cut by a wall-clock deadline (`INCOMPLETE_DEADLINE`) is not deterministic and must
     not be admitted.
2. **Prototype result [run].**
   - With replay-only admission and enough capacity, **155 of 155** `UNAVAILABLE_COST` queries are served from the
     LMDB decision cache on the second planning, **0 of 176** selected plans change, and all 20 COMPLETE hits are
     kept.
   - Second-planning time over 182 queries drops from 13.9 s to 0.45 s.
   - With the default 4 MiB pipeline budget and no capacity fix, only **11 of 155** are served, and the COMPLETE hits
     fall from 18 to 0 (crowd-out).
   - With the capacity fix from Part 2 (in-flight reserve split) at the **default** budget: **155 of 155** are served
     and 0 plans change. On this back-to-back workload, 170 LRU evictions do the work.
3. **R1 root causes [run, read].** There are two independent problems, and both predate the uncommitted work.
   - **(a) Real bug, the stuck cache.**
     - Flight admission (`tryRetainTransientIdentity`) treats every retained family as non-evictable.
     - Publish-time eviction (`makeRoom`) fills the budget to within a few KB.
     - After that every admission is refused, and no code path can evict again.
   - **(b) Outdated sizing assumption.**
     - Families are 0.16–0.99 MB (mean 0.54 MB).
     - About 80% of that is retained diagnostics and telemetry on the cached template: the packed explanation text and
       JSON are charged at their worst-case rendered size, plus per-node metric maps and strings. The executable plan
       structure is about 13%.
     - 4 MiB therefore holds about 7 families.

---

# Part 1: replay-only admission

## 1a. What an "identical request" is today for an LMDB exact hit

### Key construction (all [read])

**`LmdbQueryOptimizerPipeline.context` (`LmdbQueryOptimizerPipeline.java:438–460`).** No context, and so no cache use,
in any of these cases:
- the store has no authenticated read planes (`frontierReadPlaneMask()==0`);
- the bindings or dataset identity cannot be captured;
- the source cannot be detached or structurally keyed.

The context is then built from:
- `revision()` (`:473–476`) = `Revision(runtime.capturePlanningRevisions(), runtime.predicateRangeVersion(), runtime.adaptiveEvidenceAllowed())`;
- `executionSnapshotPresent` and the snapshot epoch;
- `DatasetIdentity` and `BindingIdentity` (the values themselves);
- `PredicateRangeIdentity.capture(...)`;
- `planningConfiguration()` (`:478–506`): every `rdf4j.optimizer.*` and `org.eclipse.rdf4j.query.algebra.*` system
  property (including cascades mode, budget and timeoutMillis), the read-plane mask, the experimental-optimizations
  flag and the native-engine flag;
- `preserveObservationOrder`, `trackResultSize`, `trackTime`, the evaluation mode and the runtime-telemetry flag;
- the filter read `FamilyIdentity`;
- `.withSource(structural key)`.

**`LmdbEstimatorRuntime.capturePlanningRevisions` (`LmdbEstimatorRuntime.java:646–655`)** takes:
- `dataRevision = snapshotVersion()`, the logical revision of the pinned planning view;
- `frontierRevision = frontierPlanningRevision()` (`:629–644`): statistics status (availability, fallback,
  generation, covered epoch/sequence), synopsis version and adjacency statistics generation;
- `leoRevision()` (`:612–623`): the mix of `LmdbOperatorFeedbackStats.planningRevision()` (= `feedbackEpoch`),
  `LmdbFilterSelectivityStats.planningRevision()`, `LmdbLearnedFilterSelectivity.planningRevision()` and the provider
  generation;
- `learnedEvidenceRevision`, the mix of the three above.

**`LmdbPipelinePlanCache.getOrComputePlan`:**
- `:128–131`: bypass unless enabled, the revision is current at entry, the context is retainable and the inputs are
  reusable (no SERVICE, no one-shot VALUES, no query-stable functions such as NOW()).
- `:148–152`: `exactInvocationKey(key)` (`:457–460`) = `ExactInvocationKey(familyKey, structural query key, SemanticContext)`.
  - `familyKey` (`:450–455`): query key, dataset, bindings, observation-order flag, evaluation mode, configuration and
    `InputIdentity`.
  - `SemanticContext` (`:462–467`): snapshot-present flag, dataset, bindings, configuration, observation order,
    trackResultSize, trackTime, evaluation mode and telemetry.
  - `InputIdentity.equals` (`:891–912`) deliberately excludes the predicate-range *contents*. They are covered by
    `predicateRangeVersion` and, for guarantee-dependent plans, by the hard index dependency.
- `:221`: `boolean stable = context.revision().equals(currentRevision.get())`. A plan is only published if no revision
  moved during its own planning. For example, a new filter sample bumps the filter planning revision (see
  `LmdbFilterSelectivityStats`, where a changed sample increments `planningRevision`). So the build evidence is
  exactly the evidence the planner read.

### Validity checks on lookup

`LmdbPlanDecisionCache.lookupExact` (`:1301–1314`) → `lookupVersion` (`:1987–2074`):
- the exact alias exists and the family version is current;
- not `HARD_INVALID`;
- `SemanticDependencies.matchesCurrent` (`:387–403`) holds, comparing provider identity (`InputIdentity`), dataset,
  bindings, result and evaluation properties, `physicalProviderIdentity(predicateRangeVersion)`, the configuration and
  the optimizer generations. It also compares the index identity when the plan is guarantee-dependent, and the
  committed data revision when it is snapshot-dependent (`LmdbPipelinePlanCache.hardDependencies`, `:367–378`);
- the alias's variant and plan version still exist;
- no lifecycle block or resource-guard breach in the runtime posterior;
- `evidenceStable` (`:2033–2035`): `publicationEvidenceEpoch == globalEvidenceEpoch` **and**
  `PlanStabilityEnvelope.covers` (`:598–617`), which is **full equality** of `EvidenceSnapshot`
  (`:426–438`: learned, data, Frontier and LEO revisions and their dependency set).

`Use` is returned only when the variant is `CONFIDENT` and the evidence is stable (`:2049–2060`).

### Does an exact hit imply every input the planner read is unchanged?

**Yes, for every versioned input [read].** Data, Frontier statistics and synopsis, adjacency statistics, learned
operator feedback, LEO, filter samples and outcomes, learned filter selectivity, provider generation, predicate-range
version and planner configuration are all in the key or the stability envelope.

Learned feedback cannot change between two plannings without invalidating the entry: by contract every completed
runtime observation and every actionable lifecycle transition advances `feedbackEpoch`. See the javadoc of
`LmdbOperatorFeedbackStats.planningRevision()` and the `feedbackEpoch++` and `publishPrepared(() -> feedbackEpoch++)`
sites.

The memo caches consulted during planning are deterministic memoizations keyed by the snapshot, as is the core packed
cache **[inferred]**:
- the exact finite-surface cache;
- the heuristic filter-pass cache;
- the core `PackedPlanCache`.

**Gaps, and how they are handled:**
- **Wall-clock deadline.** The only nondeterministic search input found. In LMDB a deadline comes only from
  `rdf4j.optimizer.lmdb.cascades.timeoutMillis` (`LmdbCascadesOptimizer.withExplicitConfiguredDeadline`, `:1110–1127`).
  Root goals otherwise use `Long.MAX_VALUE` (`:450–455`). The timeout is part of the key through
  `planningConfiguration()`, but a search *cut* by it (`INCOMPLETE_DEADLINE`) is not reproducible. Replay-only
  admission must refuse it. The core already refuses unpriced reuse with any finite deadline
  (`PackedPlanCache.PlanEntry.reusableFor`).
- **The harness ran with learned evidence disabled** (`snapshot-only`; adaptive evidence off). So the determinism
  measurement does not exercise learned feedback. That path relies on the revision contract above **[read]**.

**Determinism, measured [run].**
- `measure-auto/out/Acache.jsonl`, plus my runs `base`, `baseBig`, `splitBase`, `replay`, `replayBig` and
  `splitReplay`: 182 queries, attempts 1, 2 and 3 have identical `physicalSha` for **182/182** in every run.
- Each baseline run recomputes 128–132 `UNAVAILABLE_COST` queries from scratch on attempt 2 and still produces the
  identical plan.

## 1b. Every use of a cached decision beyond exact replay

The production LMDB call sites are only `lookupExact`, `submitRefresh`, `publishBuild`, `observeExecution` and
`tryRetain`/`releaseTransientIdentity` [read; grep]. `lookupFamily`, `publishVariant`, `scheduleRefresh` and
`markEvidenceChanged` are used only by tests.

| # | Use | Where | Authority it needs | What a decision that is today `UNAVAILABLE_COST` did at HEAD |
|---|---|---|---|---|
| U1 | Exact replay (`Use`) | `lookupVersion:2049–2060` | identity plus stable evidence; no ranking | Served |
| U2 | Stale reuse plus detached refresh (`UseAndRefresh`) | `lookupVersion:2061–2072`; `LmdbPipelinePlanCache:160–167`; `submitRefresh:1716–1753` | a regret bound saying the old champion is near-optimal under changed evidence, and comparable prices for the refresh decision | Served stale and refreshed, using whatever interval the summary carried |
| U3 | Refresh promotion | `publishRefresh:1511–1634`; `calibratedPromotionDominates:2427–2432` | finite, comparable champion/challenger intervals | Compared the intervals |
| U4 | Canary routing to challengers | `selectCanary:2088–2104`; `safeCanary:2106–2120` | finite interval regret | Possible once refresh had added challengers |
| U5 | Robust family fallback and variants | `lookupVersion:2035–2047`; `publishVariant` | applicability guards and regret | Not wired in production |
| U6 | Alias append for another exact invocation of the same family | `appendCompatibleExactAlias:1449–1501` | identical physical fingerprint **and** identical template; no ranking | Allowed |
| U7 | Build blocking | `publishBuildLocked:1386–1396`, `:1419–1425` | n/a | A VALID family blocks any later build for its key |
| U8 | Execution observations | `observeExecution:1870–1923`, fed by `LmdbOperatorFeedbackStats.publishRuntimeFeedbackTargetsInScope` through the plan-execution token, which is stamped only on cache hits (`LmdbPipelinePlanCache.annotateDecision:398–414`) | none; a measurement | Recorded into the family posterior |
| U9 | Certificate annotations on the served plan (`optimizer.planCacheSearchCompletion`, `...SearchBoundKind`) | `annotateCertificate:416–423` | none in production; no reader found | Annotated |
| C1 | Core stale-plan validation and certified reuse | `PackedCascadesPlanner` stale-validation block (≈340–380) and audit (≈400–404); `PlanEntry.supportsStaleValidation` | ranking certificate | Allowed whenever `exactComplete` held |
| C2 | Core family reuse across concrete invocations | `PlanEntry.matchesFamily` | comparable price | Allowed |
| C3 | Core reuse under different limits | `PlanEntry.reusableFor` | comparable price | Allowed for `EXACT_COMPLETE` |

Notes on the HEAD column:
- **U1, U2, U3:** at HEAD the interval was either a finite proxy (roadmap 510–512: unknown work saturated to a finite
  number, a finite 427 assigned to an unpriced fallback) or construction threw (roadmap 506–507: an unpriced winner
  "threw on an invalid interval"). A priced but incomparable winner received `VALID_LOWER_BOUND` whenever the search
  was `EXACT_COMPLETE`. HEAD `PackedPlanDecisionSummary.from` set `complete = completionStatus == EXACT_COMPLETE`.
- **U8:** this is the "observations dropped" gate the coordinator saw at `observeExecution:1873–1875`. That code is
  unchanged since HEAD (`LmdbPlanDecisionCache` has no diff). It now drops observations only because the
  `publishDecision` guard leaves no family.

The other new gates on the same status:
- `LmdbPipelinePlanCache.publishDecision:307–314` refuses publication.
- `LmdbDetachedPlanRefresh:240–244` refuses refresh.
- `PackedCascadesPlanner` publishes durably only when `reusableWorkLimitedResult || exactComplete && (comparable || no deadline)`;
  otherwise it publishes a transient entry.
- `PlanEntry.reusableFor`, `supportsStaleValidation` and `matchesFamily` in `PackedPlanCache` (≈585–639) now require a
  comparable price for everything except identical-limit, no-deadline replay.

`git diff HEAD` confirms the core is already replay-only for unpriced entries; its test
`PackedCacheRecipeOwnerTest` (≈148–155) asserts "the matching incomplete request must replay its cached plan ... an
exact-key replay cannot invent a cached price".

## 1c. Proposed change: replay-only entries in the LMDB decision cache

**Principle.** A decision whose comparison is unavailable may be retained **only as an executable replay of the
identical request under identical evidence**. It never acts as a ranking certificate, a stale-reuse bound, a refresh
champion or a canary baseline.

### Smallest correct implementation

**1. Flag.** Add `DecisionAuthority { RANKED, REPLAY_ONLY }` to `LmdbPlanDecisionCache.SearchCertificate`. Default is
`RANKED`; add a compatibility constructor. It travels with `Use`, the variant and the annotations, which keeps the
change local. A new `BoundKind` value would also work, but `BEST_KNOWN_UNBOUNDED` already means "incomplete search,
ranked", so a separate axis is clearer.

**2. Publish (`LmdbPipelinePlanCache.publishDecision:307–314`).** Replace the `return false` guard:
- If the comparison is unavailable (`!selectedCostInterval().available() || comparisonCompleteness != COMPLETE`):
  - refuse if `summary.searchCompletion() == INCOMPLETE_DEADLINE`, or if the planning goal had a finite deadline
    (mirrors `PackedPlanCache.reusableFor`);
  - otherwise publish with `EstimatedCostInterval.unbounded()`;
  - use certificate `(searchCompletion(summary), BEST_KNOWN_UNBOUNDED, NaN, epoch, epoch, 0, workUnits, REPLAY_ONLY)`;
  - set `detachedRefreshWork = null`.
- This is exactly the existing `summary == null` (UNSUPPORTED_SEMANTICS) publication shape, plus the explicit flag.

**3. Lookup (`lookupVersion`).** If the variant certificate is `REPLAY_ONLY`:
- return `Use` only for an exact-alias request (`requestedVariantId != 0`) when the variant is `CONFIDENT` and
  `evidenceStable`;
- skip robust fallback, `UseAndRefresh` and canary;
- otherwise `replan("replay-only-not-identical")`.

Today the null refresh work already turns U2 into `detached-refresh-unavailable`. The explicit branch keeps that true
even if refresh work is ever attached.

**4. Certificate uses.**
- `submitRefresh` and `scheduleRefresh` return `UNAVAILABLE` for a replay-only variant.
- `publishRefresh` and `publishVariant` refuse to write into a replay-only family.
- `selectCanary` returns null for one.
- The `LmdbDetachedPlanRefresh:240–244` gate stays: an unavailable refresh never becomes a ranked challenger.

**5. Replacement (`publishBuildLocked:1386–1396` and `:1419–1425`).** A retained family whose current variant is
`REPLAY_ONLY` never blocks a build. Treat it like a non-VALID family and replace it.
- Exception: append an alias via `appendCompatibleExactAlias` only when the incoming build is also replay-only with an
  identical template.
- A ranked VALID family is never displaced by a replay-only build. The existing rule already guarantees this.
- Because re-planning is deterministic, a COMPLETE decision for the same key appears only after an evidence or
  configuration change. In that case the replay-only entry is not evidence-stable, the lookup re-plans, and step 5 lets
  the new build replace it.
- The accounting uses the existing `previousBytes` path (retained += new − previous).

**6. Retained bytes.** Same as any build: `publicationBytes = entryBytes + provider memory` (template models,
dependencies, keys), with no refresh work and no refresh identity. Measured mean **541 KB** per family on the theme
corpus [run]; see Part 2.

**7. Annotations.**
- Add `optimizer.planCacheAuthority=REPLAY_ONLY` next to `planCacheSearchBoundKind=BEST_KNOWN_UNBOUNDED`.
- The served template keeps its own `optimizer.cascadesCostComparisonCompleteness=UNAVAILABLE_COST`, so no consumer
  can read the hit as complete.

### Execution observations

**Decision: record them.**
- **Consumer.** Only the family's `RuntimePosterior` in `LmdbPlanDecisionCache.observeExecution`. It updates
  complete and censored counts, residual bounds, `lifecycleBlocked` and `resourceGuardBreached`.
- **Effects on a replay-only entry.** `lookupVersion` reads only the block and guard flags, and only to **re-plan**.
  Residuals feed variant merging (not in production) and nothing that ranks. Interval-based canary and promotion are
  disabled for replay-only entries.
- **Learning is untouched.** Cross-query learners (operator feedback, the FrontierLearningModel, LEO surfaces, filter
  selectivity) are fed by the runtime feedback targets of *every* executed plan, cached or fresh. The token only routes
  an extra summary to the decision cache. Caching therefore adds no new learning channel. **[read]**
- **No new poisoning route.**
  - The poisoning history ("grass is greener" exact-key corrections, BSA cumulative incoherence) lives in those
    learners.
  - A replay-only hit executes the same plan fresh planning would have chosen [run: 0 plan differences].
  - As soon as a learner publishes, `feedbackEpoch` and hence `leoRevision` move, the entry stops being
    evidence-stable, and the request re-plans with the new evidence. That is exactly fresh-planning behaviour.
  - The unsafe variant would be U2 (stale reuse), which shields a plan from new evidence. It is disabled for replay-only
    entries.
- **Why record rather than drop.** Without observations, a misbehaving replay-only entry could not be retired by the
  lifecycle or resource guard.

## 1d. Tests

### Existing tests

**`LmdbPipelinePlanCacheTest#unavailableComparisonNeverPreventsExecutionOrBecomesACachedCertificate(boolean pricedWinner)`.**
- Today it asserts: both calls are misses, 2 computations, `entryCount()==0`, for both an unpriced and a
  priced-but-incomparable winner.
- After the change: rename it to `...ReplaysOnlyTheIdenticalRequestWithoutCertificateAuthority`. Assert:
  - cold miss, then identical-request **hit**, with 1 computation and `entryCount()==1`;
  - the served plan carries `planCacheAuthority=REPLAY_ONLY`, `planCacheSearchBoundKind=BEST_KNOWN_UNBOUNDED`, and its
    own `UNAVAILABLE_COST`;
  - `refreshTaskCount(cache)==0`;
  - with `context(18L)` (new data revision) the request is a miss and not `UseAndRefresh`, there are 2 computations,
    and the entry is replaced (`entryCount()==1`).

**Core counterparts.** No change needed. They already pin replay-only semantics:
- `PackedCacheRecipeOwnerTest` (≈100–155): unpriced `INCOMPLETE_WORK_LIMIT` exact replay hit, no invented price;
- `PackedPlanCacheTest#checkpointWithUnavailableFreshPriceReplansAnExecutableUnpricedResult` and
  `#staleValidationWithUnavailableFreshPriceReplansInsteadOfReusingOrFailing`: no stale reuse and no validation
  certificate.

### Tests to write first (red today)

| Test | What it asserts |
|---|---|
| T1 `LmdbPipelinePlanCacheTest#unavailableComparisonReplaysOnlyTheIdenticalRequestWithoutCertificateAuthority(boolean)` | The rewrite above. Red today: second call misses. |
| T2 `#replayOnlyEntryNeverRefreshesOrServesStaleUnderChangedLearnedEvidence` | Publish replay-only with refresh work supplied. Change only the Frontier or LEO revision. Expect a re-plan, `refreshUses==0`, no submitted task. |
| T3 `#deadlineCutUnavailableDecisionIsNotRetained` | Summary `INCOMPLETE_DEADLINE`: no publication. |
| T4 `#laterRankedDecisionReplacesReplayOnlyFamily` | Replay-only entry, evidence change, COMPLETE summary: published and served as RANKED. |
| T5 `#replayOnlyBuildDoesNotDisplaceRankedFamily` | |
| T6 `#replayOnlyHitRecordsLifecycleObservationAndReplansWhenBlocked` | `observeExecution` with `lifecycleBlocked=true`: the next identical request re-plans. |
| T7 Real store `LmdbReplayOnlyDecisionCacheTest#connectedTwoPatternJoinIsReplayedFromTheDecisionCacheWithoutCertificate` | See the fixture below. |
| T8 `PackedPlanDecisionSummary` / `LmdbPlanDecisionCache` | A `REPLAY_ONLY` certificate rejects `VALID_LOWER_BOUND` and a finite bound. |

**T7 fixture.** Pattern after `LmdbBenchmarkPlanningReceiptTest#pipelineCacheReceiptDoesNotAttributeStoredConstructionWork`:
- `MODE_PROPERTY=budgeted`, `BUDGET_PROPERTY=1`. With these, the unopened provider cannot price the mandatory
  written fallback, as in the core recipe-owner test.
- Two statements, `LmdbQueryOptimizerPipeline.optimize(new QueryRoot(join()))` twice on the same strategy.

T7 assertions:
- **Precondition:** cold `optimizer.cascadesCostComparisonCompleteness=UNAVAILABLE_COST` and
  `pipelinePlanCacheHit=false`.
- **Second planning:** `pipelinePlanCacheHit=true`, an identical physical signature, `planCacheAuthority=REPLAY_ONLY`,
  `planCacheSearchBoundKind=BEST_KNOWN_UNBOUNDED`, still `UNAVAILABLE_COST` on the served plan, and
  `decisionCache.statistics().refreshUses()==0`.
- **After a commit:** the third planning is not a hit, and the family is replaced.

**Green-test risk [inferred].**
- Ranked entries are unchanged.
- Tests that pin "unavailable is never cached" flip by intent: T1's predecessor, plus any LMDB test asserting
  `entryCount()==0` or `pipelinePlanCacheHit=false` for unpriced plans (grep for `pipelinePlanCacheHit` in
  `LmdbBenchmarkPlanningReceiptTest`, `LmdbDetachedPlanningOwnershipTest` and `LmdbPackedPlanCacheTest`).
- In the harness, `PlanningExpectation` validation of a `PIPELINE_CACHE` receipt for an UNAVAILABLE plan must accept
  "cached, incomplete comparison".

## 1e. Prototype on the theme harness [run]

**Shadow `rca.replayOnly=true`.** Steps 1–2 as specified, using the existing `summary == null` publication shape
(unbounded interval, `BEST_KNOWN_UNBOUNDED`, null refresh work, `INCOMPLETE_DEADLINE` refused). Under unchanged
evidence, lookup restrictions are implied by the null refresh work.

Results: 182 queries, 3 plannings each, default LMDB configuration.

| Run | Budget | `UNAVAILABLE_COST` on 2nd planning | COMPLETE on 2nd planning | Plan differences vs unmodified |
|---|---|---|---|---|
| `base` (shadow, behaviour-neutral) | 64 MiB → 4 MiB | 132 FRESH, 23 PACKED_CACHE, **0 pipeline** | 18 pipeline, 3 fresh | — |
| `replay` | 4 MiB | **11 pipeline**, 120 FRESH, 24 PACKED | **0 pipeline** (12 fresh, 9 packed) | 0 |
| `baseBig` | 4 GiB → 256 MiB | 0 pipeline (128 FRESH, 27 PACKED) | 20 pipeline | 0 |
| `replayBig` | 256 MiB | **155 pipeline** | 20 pipeline | **0** |
| `splitReplay` (plus the Part 2 fix) | **4 MiB** | **155 pipeline** | 20 pipeline | **0** |

- Attempts 1, 2 and 3 are identical in every run, and no run had a planning error.
- Second-planning time over all 182 queries: 13.9 s (`base`), 0.45 s (`replayBig`), 0.39 s (`splitReplay`).
- `replayBig` retains 175 families with 94.8 MB of modeled bytes.

---

# Part 2: capacity (R1)

## 2a. Budget split [read]

- `LmdbStoreConfig.FRONTIER_CACHE_EVIDENCE_BUDGET_BYTES = 64 MiB` (`:337`).
- `LmdbFrontierPlannerSettings` constructor (`:26–38`, blame d13767b49c, 2026-09-09) splits it:

| Cache | Share | Default size |
|---|---|---|
| Exact finite-surface | /16 | 4 MiB |
| Heuristic filter | /32 | 2 MiB |
| **Pipeline** | **/16** | **4 MiB** |
| Packed plan cache | /4 | 16 MiB |

  That is 26 MiB allocated out of 64 MiB.
- `LmdbPipelinePlanCache(long, ...)` (`:71–76`) creates `LmdbPlanDecisionCache(256 families, 4 MiB)`. Its in-flight
  `PackedProviderIdentityBudget` limit is **the same** 4 MiB (`LmdbPlanDecisionCache:1277`).

**Verdict: intended policy, but its sizing assumption is outdated.** The split predates the decision-cache identity
and diagnostic accounting (see 2b), and was sized when an entry was about its `entryBytes` (tens of KB).

## 2b. Why a family is 0.2–1 MB [run, per-publication byte breakdown by `RcaTrace`]

The breakdown recomputes the same accounting as `publishBuildLocked` with a recording `RetainedMemoryAccount`.

| | Ranked families (`baseBig`, n=20) | Replay-only families (`replayBig`, n=175) |
|---|---|---|
| Family size | min 233 KB, median 391 KB, max 710 KB | min 157 KB, median 561 KB, mean 541 KB, max 992 KB |
| Total | — | **94.76 MB** |

Composition of the 94.76 MB in `replayBig`:

| Component | Bytes | Share |
|---|---|---|
| `entryBytes` (512 + 2 × source + template structural estimate) | 11.90 MB | 12.6% (mean 68 KB) |
| Identity (`InputIdentity`, keys, semantic dependencies) | 4.70 MB | 5% |
| **Template provider accounting** (`LmdbCacheValueCopies.account`) | **77.11 MB** | **81%** |

Inside the template accounting:

| Item | Bytes | Share of total | What it is |
|---|---|---|---|
| `DeferredStringMetric` | **22.90 MB** | 24% | `optimizer.packedExplanation` plus `optimizer.packedExplanationJson`/`cascadesTraceJson` (one shared producer), set by `PackedPlanMaterializer.materialize:146–150`. Each is charged at `104 + 2 × maximumRenderedCharacters` (`AbstractQueryModelNode.accountDetachedPlanningState:1063–1079`). That maximum is the worst-case rendering, with JSON strings counted at 6× (`OwnedStringDiagnostic`). |
| `long[]` and `String[]` | 4.55 MB + 4.54 MB | ~10% | Exactly 2 large arrays of each per family: the frozen `OwnedStringDiagnostic` snapshots. |
| `String` | 18.54 MB | — | Per-node metric maps and strings, and diagnostic text. Largest named metrics: `optimizer.logicalLearningKey` 1.22 MB, `frontierLogicalGroupKey` 1.02 MB, `physicalResidualKey` 0.71 MB, `learningApplicability` 0.65 MB, `frontierPhysicalLearningKey` 0.46 MB. |
| `HashMap` | 17.97 MB | — | Per-node metric maps. |
| Boxed `Double` | 2.10 MB | — | Double metrics. |
| Plan nodes | < 0.5 MB | — | `Var`, `StatementPattern`, ... |

Refresh work, for ranked families only: 4–14 KB retained, plus a 20–79 KB identity.

Admission reservations: 27–113 KB, median 44 KB (`128 + 80/object + modeled bytes`).

**At HEAD [read; HEAD cannot be measured without a build].**
- None of the accounting code differs from HEAD (`git status` is clean for `LmdbPlanDecisionCache`,
  `LmdbCacheValueCopies`, `FactorCostCacheKey`, `PackedPlanMaterializer`, `AbstractQueryModelNode`,
  `OwnedStringDiagnostic` and `PackedProviderIdentityBudget`).
- Worst-case diagnostic charging and the flight admission both came in with 89e3e9c0e7 (2026-10-23). The diagnostics
  date from 330a8c4ff7 / af761ff23a (August).
- So family composition at HEAD is the same model **[inferred]**. HEAD also published every decision, so it saturated
  at least as early.

**Secondary overlap [read; small].** The template structure is charged twice: once in `entryBytes`
(`estimatedStructuralRetainedBytes(template)`), and again per node (256 + 32 × children) in the cache-model account.
The source graph appears in `entryBytes` twice and once more in `InputIdentity`. Together these are ≤ 13% of the total.

**Verdict: outdated assumption, plus a design inefficiency rather than an accounting bug.** The accounting is honest
about what is retained. What is retained is mostly diagnostics and telemetry, charged at worst case, for a cache whose
job is to replay an executable plan.

**Fix options (owner decision):**
- **(i) Do not retain the deferred diagnostics in decision-cache templates.** Strip `optimizer.packedExplanation*`
  and `cascadesTraceJson` in `LmdbPipelinePlanCache.detachedTemplate`. On a hit, annotate
  `optimizer.packedExplanation=<retained by original planning only>`. Saves about 34% (~32 MB of 94.8 MB) **[inferred
  from the breakdown]**.
- **(ii) If diagnostics must survive hits:** charge them at actual rendered size once resolved, or keep one shared
  snapshot per recipe rather than text and JSON copies.
- **(iii) Strip per-node learning-key string metrics** (`optimizer.*LearningKey`, `*ResidualKey`,
  `*Applicability`, `*GroupKey`) from cached templates, if the runtime feedback descriptors already carry those keys.
  Check first: the descriptors may be what execution feedback actually uses.

Even after (i)+(iii), the theme corpus needs about 40–50 MB for all 176 families **[inferred]**. So the 4 MiB pipeline
share is a policy question in its own right (see 2d).

## 2c. Full-cache behaviour [run, read]

**Mechanism [read].**
1. Admission (`LmdbPipelinePlanCache:177–179` → `tryRetainTransientIdentity:1291–1295` →
   `PackedProviderIdentityBudget.tryRetain(identity, reservedBytes = retainedBytes.get())`) refuses when
   `additional > limit − pending − retained`. In that check, retained families count as **non-evictable**.
2. Eviction exists only in `makeRoom` (`:2138–2157`; LRU via `recency` with `shedOneRetentionTier`/`evict`). It is
   called only from the publication paths: `publishBuildLocked`, `appendCompatibleExactAlias`, `publishRefresh` and
   `publishVariant`. It evicts just enough to fit the incoming bytes, so the budget ends nearly full.
3. When `max − retained` is smaller than the smallest flight reservation (27 KB), every admission is refused, and a
   refused flight never reaches publication.
4. No other path can free bytes:
   - refresh submission uses the same `tryRetain(identity, retainedBytes)` (`:1832`) and is refused too;
   - `hardInvalidate` keeps the family's bytes;
   - lookups never evict.
5. Existing families stay servable until their evidence changes. After that they re-plan on every request and can
   never be refreshed or replaced. The cache cannot recover until `close()`.

**Observed [run].**

| Run | State when it stuck | Bypasses after that |
|---|---|---|
| `base` (4 MiB) | after 8 evictions: 10 families retaining **4,187,566 of 4,194,304** bytes (6.7 KB free) | 18, no further eviction |
| `replay` (4 MiB) | 6 families at 4,184,186 bytes | **510**, 0 further evictions |

In `replay` the early replay-only families also crowded out every COMPLETE family (18 → 0 hits).

**Verdict: real bug.** It predates the uncommitted work: the admission rule is from 89e3e9c0e7 (2026-10-23), and
`makeRoom` and the LRU are from cf33557fc0 / bf52625fbc (August). Admission and eviction disagree about whether
retained families are evictable.

## 2d. Fix spec and policy guarantees

**F1. In-flight reserve split** (prototyped in `RR/src/.../LmdbPlanDecisionCache.java`, flag `rca.reserveSplit`).
- `inflightReserve = maximumRetainedBytes / 8` and `retainedLimit = maximumRetainedBytes − inflightReserve`.
- `pendingProviderIdentities = new PackedProviderIdentityBudget(inflightReserve)`.
- Admission and refresh submission call `tryRetain(identity, 0L)`, so they are never blocked by retained families.
- `makeRoom` and every post-`makeRoom` check use `retainedLimit − incoming` in place of `max − pending − incoming`.
- `publicationBytes`, `updatedBytes` and `refreshedRetainedBytes` are compared against `retainedLimit`.

Measured at the **default** 4 MiB [run]:
- `splitBase`: 0 bypasses; COMPLETE hits 20 (was 18).
- `splitReplay`: 0 bypasses; 155 of 155 unavailable plus 20 COMPLETE hits; 170 LRU evictions; 0 plan differences.

**Caveat.** The harness plans each query three times back to back, which LRU handles trivially. An interleaved
workload larger than capacity (4 MiB ≈ 7 families of this corpus) will churn.

**F2. Anti-thrash admission.** Publish a *new* family only if either:
- it fits in free retained space without eviction, or
- the same exact key missed at least once before, tracked in a bounded doorkeeper of recent miss fingerprints with
  modeled bytes charged to the reserve.

LRU eviction then only removes families to admit a key that has proven reuse. Optionally make the LRU segmented
(probation and protected), as the core `PackedPlanCache` frequency arrays do.

**F3. Shrink families** (2b (i)–(iii)) **and revisit the pipeline share.** The share is the owner's call: for example
budget/8 instead of /16, or a size derived from mean family size.

**Guarantees the policy must give:**
- **No stuck state.** Admission never depends on evictable bytes.
- **Bounded retention.** `retained ≤ retainedLimit` and `pending ≤ inflightReserve` at every step, so
  `retained + pending ≤ max`.
- **No thrash.** Eviction happens only to publish a family that has shown reuse. A stream of one-off queries cannot
  flush a hot set that fits.
- **Deterministic accounting.** All charges are modeled bytes taken under `admissionLock`, `retainedBytes` equals the
  sum of the families' `retainedBytes()`, and the eviction order is a pure function of the access sequence.
- **Replay-only entries compete under the same policy as ranked ones.** Optionally, under pressure, evict replay-only
  entries before ranked ones.

**Tests to write first:**

| Test | What it asserts | Today |
|---|---|---|
| T9 `LmdbPlanDecisionCacheTest#saturatedCacheStillAdmitsAndEvictsLeastRecentFamily` | Budget for about 2 families. Publish 3 via `LmdbPipelinePlanCache` with synthetic computations. The third is published, the first is evicted, and the cache never bypasses. | Red: bypass, observed |
| T10 `#retainedFamiliesNeverBlockFlightAdmission` | Retained bytes at `retainedLimit`: `tryRetainTransientIdentity` still succeeds. | — |
| T11 `#inflightReserveBoundsConcurrentFlights` | Reserve exhausted → bypass; released after publish or abandon. | — |
| T12 `#accountingInvariantHoldsAcrossPublishEvictRefresh` | `retained + pending ≤ max`, and `retained == Σ family bytes`. | — |
| T13 `#oneOffMissDoesNotEvictAHotFamily` | Requires F2. | — |
| T14 (if 2b (i) is adopted) `#cachedTemplateDoesNotRetainDeferredDiagnostics` | Family bytes ≤ `entryBytes` + identity + slack. | — |

**Green-test risk.** `LmdbPipelinePlanCacheTest#planThatFitsAggregateDecisionBudget...` depends on the admission
threshold; see my earlier RCA (`rca-lmdb-cache-feedback.md`, group 1). Under F1 its budget must cover
`retainedLimit ≥ entry` and `reserve ≥ transient`. Use `budget = 2·entry − 1` together with an explicit reserve check.
The other cache tests use a 1 MB budget and are unaffected **[inferred]**.
