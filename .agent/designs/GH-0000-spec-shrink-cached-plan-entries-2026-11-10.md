# Spec: shrink cached plan entries in the LMDB decision cache

**Status.** Read-only design. Nothing under the repository or `scratchpad/trees/` was edited, and nothing was built
from the original tree.

**Scratch root.** `SR` = `/private/tmp/claude-501/-Users-havardottestad--claude-spares-spare-5bPeTt/157f627d-35d8-4517-9d49-1f48286fcd13/scratchpad/rca-shrink/`.
- Shadow copies: `LmdbPlanDecisionCache`, `LmdbPipelinePlanCache`, core `AbstractQueryModelNode`, plus tracing
  (`RcaShrink`, `RcaReads`).
- Harness: a probe copy with heap, execution and interleaving extras.
- Runner: `SR/run.sh` and `SR/run2.sh`.
- Logs: `SR/out/*`.
- Analysis scripts: `SR/inv.py` and `SR/hits.py`.

**Build used.** `measure-auto/out-implcache/` (`cp.txt`, `lib/`). Before shadowing, I compiled unmodified copies of
`LmdbPipelinePlanCache`, `LmdbPlanDecisionCache`, `LmdbDetachedPlanRefresh`, `LmdbQueryOptimizerPipeline`,
`LmdbCacheValueCopies`, `FactorCostCacheKey` and core `AbstractQueryModelNode` from the current working tree. Their
`javap -p -c` output is byte-identical to that build. The replay-only and capacity merge is therefore what was measured.

**Workload.** Theme harness, pristine 16.5M-triple store, default LMDB configuration, 182 queries (176 cacheable) unless
stated. The harness uses `snapshot-only` estimator evidence, so learned feedback is **not** exercised by any
measurement below.

**Labels.**
- **[M]**: measured by running.
- **[R]**: read in code; current working tree, cited by method because `LmdbPipelinePlanCache` and
  `LmdbPlanDecisionCache` are being reworked.
- **[I]**: inferred.

## Bottom line

1. **What a family's modeled bytes are [M].**

   | | Modeled | Real heap freed when the cache is cleared |
   |---|---|---|
   | Per family | mean 541.5 KB, range 157–992 KB | ≈ 167 KB |
   | 181 families | 98.3 MB | 30.2 MB |

   The modeled figure is 3.25× the real heap. The largest charges:
   - **24% — worst-case diagnostic overcharge.** `104 + 2 × maxRenderedChars` per explanation wrapper. The rendered
     String never exists in the cache [R].
   - **12.5% — `entryBytes` structural estimate.** It double-charges nodes that are already charged per object and
     charges a source graph the family does not retain [R].
   - **15% — frozen explanation snapshots.**
   - **18% — per-node metric maps,** modeled at 96 B per entry against roughly 40 B real.
   - **≈ 5% — JVM-interned constant strings** (metric names, literal diagnostic fragments).
2. **Exact replay needs very little [M].** A census of planned-metric reads while executing 12 replayed plans finds
   only about 15 metrics: physical hints, row and work estimates, id-filter, exact-values and assumption markers.
   - **EXPLAIN:** `toString()` and `toJson()` contain no explanation diagnostics, either fresh or cached.
   - **Where diagnostics are reachable:** only through direct metric access on the explained `TupleExpr`. That is used
     by tests and by the benchmark tooling `QueryPlanCapture`.
3. **Prototype results [M].** Variant "strip diagnostics + exact entry" on the default 4 MiB budget:

   | | Before | After |
   |---|---|---|
   | Family size (modeled) | mean 541 KB | **262 KB** (84–571 KB) |
   | Family size (real) | 167 KB | **114 KB** |
   | Families resident at the end | 6 | **20** |
   | Second-planning hits | 38 | **55** |
   | Third-planning hits | 175 | **175** |
   | Evictions | 151 | 132 |

   - **0 selected-plan differences.**
   - 12 sampled queries execute from replayed plans with row counts identical to fresh execution.
   - The explanation-preserving accounting-only variant (exact diagnostic charge + exact entry) gives mean 343 KB,
     14 resident families and 49 second-planning hits.
4. **Capacity still dominates cyclic workloads [M].** Each of the 176 queries run once per pass, three passes, at
   4 MiB gives **0 hits in passes 2 and 3** for every variant. LRU with a working set larger than the cache thrashes.
   With a large budget the same interleaving gives 175 hits, so the cause is capacity, not evidence churn (one set of
   revisions throughout). Shrinking raises capacity about 2.7× (6 → about 20 resident families, more with Tier 2). The
   theme working set still needs about 46 MB at 262 KB per family. See §7.

---

## 1. Inventory: what a cached family retains

### Retained objects [R]

**Family record.** A `PlanFamily` (key `PlanFamilyKey`) points at a `FamilyVersion` holding:
- `SemanticDependencies` (whose `providerCacheIdentity` is the `InputIdentity`);
- a `VariantDispatcher` → `PlanVariant`, which holds:
  - a champion `PlanVersion` (`ExactTupleRecipe` template, fingerprint, interval, role, epochs, optional
    `DetachedRefreshWork`);
  - challengers and rollbacks, each a `PlanVersion` with its own template;
  - a `SearchCertificate`, `EvidenceSnapshot`, `PlanStabilityEnvelope`, `RuntimePosterior` and challenger posteriors;
- exact aliases (`ExactInvocationKey` → `ExactAlias`);
- an optional retained structural checkpoint (null for builds).

Index entries live in `exactAliases`, `recency` and `reverseEvidence`.

**How bytes are charged.**
- **`publicationBytes`** (`LmdbPlanDecisionCache.publishBuildLocked`, the `providerMemory.accountCacheModels(recipe.template)`
  … `publicationBytes = ...` block) = `entryBytes + refreshWork.retainedBytes() + ProviderRetainedMemory`. The provider
  memory covers, de-duplicated by identity and in this order: template models (`LmdbCacheValueCopies.account`),
  semantic dependencies, family key, exact key, and refresh identity.
- **`entryBytes`** (`LmdbPipelinePlanCache.publish`) = `512 + 2 × flight.sourceRetainedBytes + FactorCostCacheKey.estimatedStructuralRetainedBytes(template)`.

### Measured inventory [M] (`SR/out/v0.log`, 175 families: 155 replay-only, 20 ranked; independent categorizing ledger)

The categorized sum equals each family's `retainedBytes()` exactly (ratio 1.0000).

| Component | Mean KB | Range KB | Share | Kind of figure | Also retained elsewhere? |
|---|---|---|---|---|---|
| `entryBytes` fixed 512 | 0.5 | 0.5 | 0.1% | constant | — |
| `entryBytes` structural estimate (2 × source + template estimate) | 67.5 | 16.9–238.7 | 12.5% | **estimate, not backed by objects.** The template nodes are charged again per object below, and the family keeps only the source's `StructuralKey` (charged under identity), not the source `TupleExpr` [R]. | n/a |
| Plan structure (nodes 256+32·children, names, values) | 18.2 | 4.8–66.2 | 3.4% | estimate (real node objects ≈ 15 KB/family [M]) | no |
| Feedback contracts and descriptors | 46.1 | 0–189 | 8.5% | upper bound | partly. The contract objects are exclusive (real ≈ 2–3 KB/family [M]). The key objects inside descriptors are not freed when the cache is cleared [M: absent from the heap delta], so they are referenced elsewhere [I]. |
| Metric maps (HashMap modeled 192 + 96 per entry) | 97.3 | 29–230 | 18.0% | estimate (overcharge) | no (real ≈ 41 KB/family including boxes [M]) |
| Metric keys (names) | 15.7 | 5.7–27 | 2.9% | **overcharge**: 99% are canonical JVM-interned constants [M] | yes, the JVM string table |
| Metric numbers (boxed, 24 B each) | 12.1 | 2.7–30.7 | 2.2% | estimate (real `Double` is 16 B) | no |
| Learning-key string metrics | 19.4 | 0–59.7 | 3.6% | exact per object | possibly shared with descriptor keys (`externalForm()` is cached in the key object [R]) |
| Other string metrics | 8.8 | 2.4–25.8 | 1.6% | exact per object (25% interned) | partly |
| Diagnostic wrappers (`104 + 2 × maxRenderedChars`) | 131.3 | 21–255 | **24.3%** | **worst-case upper bound of a String that is never created in the cache** [R; see §3 A1] | n/a |
| Diagnostic frozen snapshot (`OwnedStringDiagnostic` arrays and strings) | 81.4 | 9.1–158.9 | 15.0% | exact per object, but 11% are interned literal fragments | real ≈ 43 KB/family [M]. The core `PackedPlanCache` keeps the recipe, not this snapshot. |
| Planning metadata (graph domains and similar) | 10.1 | 2.3–48.3 | 1.9% | estimate | no |
| `InputIdentity` source `StructuralKey` | 25.2 | 12.4–73.1 | 4.7% | estimate (real int arrays ≈ 14 KB [M]) | no |
| `InputIdentity` ranges, filter, dataset, bindings | 1.4 | 0.3–5.2 | 0.3% | estimate | no |
| Family, exact and semantic key objects | 0.9 | 0.9 | 0.2% | estimate | no |
| Refresh work, `retainedBytes()` (ranked only) | 0.7 (ranked 2–14) | 0–13.8 | 0.1% | estimate | no |
| Refresh identity (ranked only) | 4.9 (ranked 20–79) | 0–79 | 0.9% | estimate | shares the `InputIdentity` (de-duplicated) |
| Challengers and rollbacks | 0 in this run (no refresh happened) | — | — | each one is another template, charged the same way | — |
| Certificate, evidence, envelope, posterior, alias and index records | **not charged** | ~1 KB [I] | — | uncharged | — |

**Family size by kind.** Replay-only families average 557.8 KB; ranked families average 414.4 KB.

**Real heap [M]** (`SR/out/h0.log` and `h4.log`). Histogram delta from clearing the cache, 4 GiB evidence budget,
181 families. Heap-used delta 30,237,744 B, matching the histogram delta of 30,224,888 B.

| Real component | Before (v0) | After (v4: strip + exact entry) |
|---|---|---|
| Diagnostic arrays (`[J`, `String[]`) | 43.3 KB/family | 3.5 |
| Strings (`String` + `[B`) | 36.2 | 22.4 |
| Metric maps (`HashMap$Node`, tables, `HashMap`, `EntrySet`, `Double`) | 41.2 | 41.1 |
| int arrays (identity keys) | 13.8 | 13.8 |
| Plan-node objects | 15.0 | 15.3 |
| Feedback contracts and prediction vectors | 2.2 | 2.2 |
| Metadata | 1.3 | 1.3 |
| GC filler noise | 2.0 | 3.2 |
| Classes outside the top 30 | ≈ 10 | ≈ 10 |
| **Total** | **167.0** | **114.4** |

---

## 2. Consumers after publication

Classes: **EXEC** = needed for correct execution of a replayed plan; **LEARN** = needed so a replayed plan feeds
learning exactly as a fresh plan does (correctness of learning, not of results); **CERT** = ranked-entry certificate
uses only; **DIAG** = diagnostic only.

| Component | Readers (file, method) | Class |
|---|---|---|
| `InputIdentity` and keys | `LmdbPlanDecisionCache.lookupExact` (alias map equality via `ExactInvocationKey`/`PlanFamilyKey`/`InputIdentity.equals`); `lookupVersion` → `SemanticDependencies.matchesCurrent`; `publishBuild` canonicalization; `appendCompatibleExactAlias`; `submitRefreshRequest` (`RefreshProviderIdentity`); `LmdbDetachedPlanRefresh` (`RetainedIdentity`) | EXEC (key correctness) |
| Plan structure | `ExactTupleRecipe.materialize` → `LmdbCacheValueCopies.copy` → `LmdbQueryOptimizerPipeline.optimize` (`install`, `annotatePipelineCacheHit`, `annotateFeedbackPublication`) → precompile and evaluate; `sameExecutableTemplate` (structural `equals`; metrics are not compared, see `BinaryTupleOperator.equals` [R]) | EXEC |
| Feedback contracts and descriptors | `LmdbPipelinePlanCache.stampPlanExecutionToken` on hits; runtime target resolution during evaluation (`LmdbEvaluationStatistics` → `LmdbOperatorFeedbackStats.resolveRuntimeFeedbackTarget`); `observeExecution` via `publishRuntimeFeedbackTargetsInScope` | LEARN, plus observation recording |
| Execution-read metrics. Measured census **[M]** (`SR/out/r0.log`, 12 replayed executions): `joinAlgorithmHint` (`JoinQueryEvaluationStep.isHashJoinHint`, `NativeBGPOrigin.lexicalJoin`), `filterAlgorithmHint` and `semiAntiKind` (`FilterIterator`), `plannedCardinalityRows`/`plannedCostWorkRows`/`plannedWorkRows` (`JoinQueryEvaluationStep$RuntimeEstimate.of`), `optimizer.idFilter` (`LmdbValueIdFilter`), `optimizer.exactValues` (`LmdbNativeAggregateValuesCompiler`), `plannedIndexAccessMode` (`LmdbCascadesOptimizer$5.meet`), `plannedFilterPassRatio` (`LmdbFilterSimplifierOptimizer.annotateFilter`), `optimizer.planAssumption.*` (`LmdbPlanAssumptions$Collector`), `optimizer.cascadesApplied` (`LmdbPipelinePlanCache.retainable`). Further readers by grep **[R]**, not exercised: planned execution engine, distinct-requirement metrics (`LmdbDistinctRequirement`), graph-distinct (`LmdbGraphDistinctAccess`), path endpoint mode, `plannedEstimateFusion`, `optimizer.frontierSemanticPopulation`, dataset-restricted and committed-snapshot markers, `optimizer.semiAntiHitProbability`, `optimizer.dependent*` | | EXEC |
| Learning-read metrics **[R]**: `LmdbOperatorFeedbackStats` (frontier learning key, cost-event digest, state digest, guarantee, logical group key, physical learning key, logical key, applicability, residual key, LEO candidate and `LEO_*` metrics, objective and lifecycle bounds, repeated invocations, cost-event prefix rows and objective); `LmdbMappedFrontierLearningSession` | | LEARN |
| Explain-visible planned metrics (`TelemetryMetricNames.isPlannerEstimateMetric`: `plannedEstimate*`, `plannedCardinality*`, `plannedCost*`, `plannedObjective*`, `plannedFilter*`, ...) | `QueryModelTreePrinter.appendPlannedMetrics`, `GenericPlanNode.orderedStringMetricsPlanned` | DIAG, but **user-visible** |
| Remaining planned metrics (cost-event numerics, rule attribution such as `cascadesRule`/`RuleKind`/`Proofs`/`RejectedAlternatives`, frontier bundle, lifecycle diagnostics) | bulk `getStringMetricsPlanned` in `QueryModelTreeToGenericPlanNode`; benchmark tooling; tests | DIAG |
| Diagnostics (`optimizer.packedExplanation`, `...Json`, `cascadesTraceJson`, set in `PackedPlanMaterializer.materialize`) | Direct metric access on the explained or installed `TupleExpr`: `LmdbDetachedPlanningOwnershipTest#actualPackedPipelineKeepsARealWarmHitAndSchemaTwoDiagnostics`, `LmdbOptimizerPipelineTest` (trace helper), `LmdbAASQuery3MappedInputIdentityTest`, `LmdbSocialQ7OptimizedPlanIT`, `PackedPlanDeferredDiagnosticTest`, `QueryPlanCaptureTest`; benchmark-common `QueryPlanCapture`/`QueryPlanExplanation` (`getDebugMetrics`). **Not** in EXPLAIN `toString()` or `toJson()` at `Optimized` or `Executed`, fresh or cached [M: jshell plus the 6-query harness sample] | DIAG |
| `entryBytes` estimate | accounting only | — |
| Refresh work and identity | `submitRefresh` → `executeDetachedRefresh` → `LmdbDetachedPlanRefresh.execute` | CERT |
| Challengers and rollbacks | `selectCanary`/`safeCanary`, `publishRefresh` (promotion), `shedOneRetentionTier` | CERT |
| Certificate, evidence, envelope, posterior | `lookupVersion` (evidence stability, posterior guards); `annotateDecision`/`annotateCertificate`; `observeExecution` | EXEC (validity) and CERT |
| Aliases | `lookupExact`, `appendCompatibleExactAlias`, `removeAliases`/`replaceAliases` | EXEC |

Not LMDB family components: plan-quality audit and stale validation live in the core `PackedPlanCache` and
`PackedCascadesPlanner`. The family never retains a packed recipe or detached evidence.

**Census limitation.** Learned evidence was off and only 12 query shapes ran, so LEARN readers and unexercised EXEC
readers come from code reading.

---

## 3. Design per component

Ordering puts behaviour-neutral accounting first, then owner-approved memory drops.

### Tier 0: exact accounting

No behaviour change and no EXPLAIN change; reduces modeled bytes only.

**A1. Diagnostic wrappers: charge what exists.**
- **Today** `AbstractQueryModelNode.accountDetachedPlanningState` charges `104 + 2 × maxRenderedCharacters`. That
  covers "its maximum future memoized String", in case the wrapper is ever resolved.
- **Never resolved in place [R].**
  - Nothing resolves a *cache-owned* template's wrapper in place: `ExactTupleRecipe.template` is private and is used
    only by `materialize()` (`LmdbCacheValueCopies.copy` → `copyDetachedPlanningStateTo`). That reads `resolved` under
    the wrapper lock and creates *new* detached wrappers.
  - `retainedBytes()` only accounts.
  - `sameExecutableTemplate` compares a fresh copy.
- **Change.** Make that guarantee structural. Add a non-memoizing mode to `DeferredStringMetric`: `resolve()` renders
  without storing. `copyDetachedPlanningStateTo` creates non-memoizing wrappers *only* for cache-owned copies. Give
  `ExactTupleRecipe` its own path, `LmdbCacheValueCopies.copyForCache`, rather than the materialize path. Charge
  `104` plus the snapshot's real arrays.
- **Effect.** −131 KB/family modeled; real heap unchanged.
- **Overlap with the second round.** In `LmdbPlanDecisionCache` only the `ExactTupleRecipe` constructor line changes.

**A2. `entryBytes` = `ENTRY_OBJECT_BYTES` only.**
- Drop `2 × flight.sourceRetainedBytes + estimatedStructuralRetainedBytes(template)` (`LmdbPipelinePlanCache.publish`).
- Template nodes are already charged per object (256 + 32 × children). The family retains only the source's
  `StructuralKey`, charged under `InputIdentity`.
- Optionally fold a fixed charge for the uncharged records (certificate, evidence, posterior, alias entries) into
  `ENTRY_OBJECT_BYTES`.
- **Effect.** −67.5 KB/family modeled.
- **Measured with A1** (`v3`): mean 343.5 KB (93.5–687.7), versus 541.5 KB before.

**A3. Do not charge metric-name strings.**
- They come from a closed vocabulary of compile-time constants: 99% are canonical-interned [M]. The change is to
  `AbstractQueryModelNode.accountMap` (keys only).
- **Effect.** −15.6 KB/family.

**A4. Align map and box models with the compressed-oops layout.**
- Today: map 192 + 96 per entry, box 24 B.
- Proposed: `HashMap` 48 + table (16 + 4 × capacity), 32 B per node; `Double`/`Long` 16 B.
- **Effect.** Metric maps plus numbers go from 109 KB to about 41 KB modeled (the measured real figure) [I].

**Projection, Tier 0 only (no memory change).** About 541 → 260 KB modeled [I]. That is about 16 families in 4 MiB.

### Tier 1: drop diagnostics from cache-owned templates

Owner-approved shrink. Real memory −53 KB/family.

**B1.** In the cache-only copy path (`LmdbCacheValueCopies.copyForCache`, called by `ExactTupleRecipe`), replace each
deferred diagnostic (`optimizer.packedExplanation`, `optimizer.packedExplanationJson`, `optimizer.cascadesTraceJson`)
with a short constant, for example `"omitted: replayed from the plan cache"`. Do this for ranked and replay-only
entries alike, because no certificate use reads them.
- Implementation detail: `setStringMetricPlanned(name, constant)` removes the deferred wrapper without rendering [R].
  A core API `removeDeferredStringMetricPlanned(name)` would be cleaner.
- The plan returned to the *current* request keeps its diagnostics. The prototype did exactly this.

**What a user sees.**
- EXPLAIN `toString()` and `toJson()` at `Optimized` and `Executed` are **unchanged** [M].
- **Visible change:** `explanation.tupleExpr().getStringMetricPlanned("optimizer.packedExplanation*")` on a *cached*
  plan returns the constant instead of the schema-2 trace. This affects one LMDB test and the benchmark capture
  tooling.

**Alternatives that preserve diagnostics on cached plans:**
- **(a) Opt-in store config** `retainCachedPlanDiagnostics` (default false). Benchmark capture sets it, and A1 then
  keeps the charge exact.
- **(b) A1 alone, without B1.** No real-memory saving.
- **(c) Re-plan when a cached plan's diagnostics are read.** Not recommended. Re-planning is deterministic for replay
  entries [M: 0 differences across 6 runs], but it costs a full planning (mean about 50 ms here) and needs the
  request's dataset, bindings and read snapshot at render time, which may already be closed.

**Measured (v4 = B1 + A2):**
- modeled mean 262.2 KB (84–571), real 114 KB;
- 0 plan differences;
- replay execution identical [M].

### Tier 2: compact frozen metrics

Structural; no visible change. Real memory about −30 KB/family [I].

**C1.**
- Cache-owned copies store each node's planned metrics as frozen parallel arrays: interned names, a `double[]`/`long[]`,
  and a `String[]`.
- `materialize()` expands them back into HashMaps on the hit copy. The cost is about 800–1300 map puts per hit,
  microseconds [I].
- **Saving [I].** It removes most of the measured 41 KB/family of map overhead. A compact table costs about 13 B per
  entry, so roughly −28 to −32 KB/family real.
- **Charges.** Arrays are charged exactly, and names are not charged (A3).
- **Change surface.** Core `AbstractQueryModelNode`: a frozen metric holder, `copyDetachedPlanningStateTo` and
  accounting. LMDB changes only through `copyForCache`.

### Tier 3, optional (owner): drop diagnostic-only metric families from cached templates

**D1.** Allow-list kept on cached templates:
- EXEC readers (§2 census + grep);
- LEARN readers;
- `isPlannerEstimateMetric` metrics (user-visible EXPLAIN).

Everything else (cost-event numerics, rule attribution, frontier bundle, lifecycle diagnostics) is dropped.
- **Risk.** An incomplete allow-list silently changes execution or learning. Gate it with the read-census test (T8)
  and a differential plan, row and feedback test.
- **Saving.** Not measured; measure before deciding [I].

### Components deliberately kept, and why

| Component | Why kept |
|---|---|
| `InputIdentity`, keys, aliases | Key and validity correctness. |
| Plan structure, execution-read metrics | Needed to execute the replayed plan. |
| Feedback contracts and descriptors | Plan-execution token, observation recording, and learning parity with fresh plans. They are exclusively owned only in part: modeled 46 KB against ≈ 2–3 KB of real exclusive objects [M]. A deterministic "exclusive only" charge is not available, so keep the upper bound and record it as a known overcharge. |
| Refresh work, challengers, rollbacks | Ranked only (already so after the merge); certificate uses only. |
| Certificate, evidence, posterior | Validity checks; tiny. |

### Sharing

Interned metric names and literal diagnostic fragments are already shared by the JVM. A3 simply stops charging them.
No cross-family sharing is proposed: it would break "retained equals the sum of families" unless charged through a
reference-counted ledger (as `PackedProviderIdentityBudget` does), and the measured gain does not justify that.

**Accounting invariants kept.**
- Every charge is computed once, at publication, from objects the family owns.
- `ExactTupleRecipe.retainedBytes()` recomputes the same value at any time. This requires A1's "never resolved in
  place" guarantee and that templates are immutable after `copyForCache`.
- So `retained + pending + doorkeeper ≤ max` and "retained = Σ family `retainedBytes()`" both hold.

---

## 4. Expected effect

### Default 4 MiB budget, sequential (each query planned 3 times back to back) [M]

| Variant | Family modeled mean (range) | Real per family | Resident at end | 2nd-planning hits | 3rd-planning hits | Evictions | Plan diffs |
|---|---|---|---|---|---|---|---|
| v0 (current merged) | 541.5 KB (157–992) | 167 KB | 6 | 38 | 175 | 151 | 0 |
| v3 = A1 + A2 (no visible change) | 343.5 KB (94–688) | 167 KB | 14 | 49 | 175 | 132 | 0 |
| v4 = B1 + A2 | 262.2 KB (84–571) | 114 KB | 20 | 55 | 175 | 132 | 0 |
| v4 + A1(n/a) + A3 + A4 [I] | ≈ 180 KB | 114 KB | ≈ 23 | — | — | — | — |
| + C1 [I] | ≈ 150 KB | ≈ 85 KB | ≈ 27 | — | — | — | — |

- Second-planning hits are capped by the doorkeeper policy: a first sighting publishes only into free space.
- Third-planning hits are 175 of 175 in every variant.
- Per-run plan-time totals for the second and third plannings are in `SR/hits.py` output.

### Interleaved: 3 passes over 176 distinct queries, 4 MiB [M]

| Variants | Pass 2 hits | Pass 3 hits | Diagnosis |
|---|---|---|---|
| p0, p3, p4 | 0 | 0 | 314 / 286 / 294 evictions: LRU cyclic thrash. In pass 1, two duplicate keys were proven repeats and evicted the earliest families. |
| Same interleaving, 4 GiB budget | **175** | — | A single, constant set of planning revisions throughout. Capacity, not evidence churn, is the cause. |

### Correctness [M]

- 0 selected-plan differences in all 9 runs. The physical signature is identical across attempts and against v0.
- 12 sampled queries, via `evaluate()` and `explain(Executed)`, replayed from the cache in both v0 and v4 with row
  counts identical to fresh execution.
- `DATA_TRANSFORMATION` q0 was not pipeline-eligible, and `EXPLORATION` q0 exceeded the 30 s limit. Both behaved the
  same way in each variant.

### Real heap [M]

| | v0 | v4 |
|---|---|---|
| Modeled | 98.3 MB | 47.4 MB |
| Real | 30.2 MB | 20.7 MB (−31%) |

---

## 5. Tests to write first

| Test | Assertion | Why red today |
|---|---|---|
| T1 `LmdbPlanDecisionCacheTest#familyRetainedBytesEqualTheSumOfItsRetainedObjectCharges` | Publish a real pipeline template (metrics, one deferred diagnostic, one feedback contract). Walk the family with an independent ledger: the template via a package-private `ExactTupleRecipe.accountTo(account)`, identity, keys, refresh identity, plus `ENTRY_OBJECT_BYTES`. Assert equality with `statistics().retainedBytes()`. | `entryBytes` adds `2 × source + template estimate` that no object backs (measured 67.5 KB/family). |
| T2 `#cacheOwnedDiagnosticIsChargedOnlyForItsSnapshot` (A1) | Diagnostic charge = 104 + snapshot arrays and strings. | Today it adds `2 × maxRenderedChars`. |
| T3 `#cacheOwnedTemplateIsNeverResolvedInPlace` (A1 guard) | Materialize twice, render the hit's diagnostics, re-account the recipe: bytes unchanged, and the cached wrapper is still unresolved. | Green today; becomes a guard once A1 removes the upper bound. |
| T4 `LmdbPipelinePlanCacheTest#cachedTemplateCarriesNoDiagnosticSnapshot` (B1) | The hit's `optimizer.packedExplanation` equals the placeholder. The fresh plan still has a schema-2 trace. | — |
| T5 `LmdbDetachedPlanningOwnershipTest#actualPackedPipelineKeepsARealWarmHitAndSchemaTwoDiagnostics` | Rewrite: the fresh plan keeps schema-2 diagnostics; the warm hit gets the placeholder (or the trace when `retainCachedPlanDiagnostics=true`). | — |
| New `#explainTextAndJsonOfACachedPlanEqualTheFreshPlan` | Pins user-visible EXPLAIN: `toString()`/`toJson()` equal modulo the `planCache*` and `pipelinePlanCacheHit` annotations. | Green today [M]; must stay green. |
| T6 `#replayedPlanKeepsExecutionAndLearningMetrics` | Every metric in the EXEC and LEARN sets of §2 has the same value on the materialized hit as on the fresh plan. | Guard. |
| T7 Real store with adaptive evidence on, `#replayedPlanFeedsLearningLikeAFreshPlan` | Execute fresh, then hit. Same logical, physical and applicability keys reach `LmdbOperatorFeedbackStats`, and the plan-execution token is recorded via `observeExecution`. | Guard. |
| T8 `#metricReadCensusOfReplayedExecutionIsRetained` | Recording read hook as in the prototype (a test-only `QueryModelNode` observer). Every metric read while executing a replayed plan is present on the cached template. | Guard for D1 and C1. |
| T9 `#rankedRefreshPublishesShrunkChampionAndChallenger` | A ranked entry refreshes through `UseAndRefresh`. Champion and challenger templates are shrunk, their charges recompute equal, and promotion intervals are unchanged. | — |
| T10 `#metricNamesAreNotCharged` (A3), `#metricMapChargeMatchesLayoutModel` (A4), `#frozenMetricsRoundTripExactly` (C1) | — | — |

---

## 6. Risks and ordering

**Order of work.**
1. A2 and A1, with T1–T3.
2. B1, with T4, T5 and the EXPLAIN pin.
3. A3 and A4.
4. C1.
5. D1, only on owner request.

Each step can be merged on its own.

**Overlap with the pending second round.**
- In `LmdbPlanDecisionCache`: one line in the `ExactTupleRecipe` constructor (call `copyForCache`), and optionally an
  accessor for T1.
- In `LmdbPipelinePlanCache`: one expression in `publish` (`entryBytes`).
- Everything else lands in `LmdbCacheValueCopies`, which is not under rework, and in core `AbstractQueryModelNode`.

**Capacity policy and doorkeeper.**
- Smaller families raise free space, so more first sightings publish without eviction (second-planning hits
  38 → 55).
- Shrinking does not fix LRU thrash on cyclic working sets larger than capacity [M: 0 hits]. A frequency-aware victim
  rule (TinyLFU: admit a proven repeat only if its frequency beats the victim's) would keep a stable subset resident.
  That is a separate capacity-policy change.
- The doorkeeper charge is 64 + 16 × 256 = 4,160 B, negligible.

**Ranked entries.**
- Refreshed templates, champions and challengers go through `ExactTupleRecipe`, so they get the same shrink.
- `retainedPlanBytes`, `variantRetainedBytes` and `shedOneRetentionTier` recompute charges from the recipe. A1 must
  keep that value constant: templates are immutable after `copyForCache`, and wrappers are non-memoizing.

**Lazy reads on other threads.**
- Hit copies get fresh detached wrappers over the immutable `OwnedStringDiagnostic` snapshot. Concurrent materialize
  calls only take the original wrapper's monitor to read `resolved`.
- With B1 there is nothing lazy left on cached templates.
- A1's non-memoizing wrapper must stay `synchronized` or immutable.

**Test and tool fallout.** Direct readers of diagnostics on cached plans (§2) need the opt-in or an updated
expectation.

---

## 7. Pipeline share of the evidence budget (owner decision)

Numbers [M unless marked]:
- **Theme working set:** 176 cacheable families.
- **Modeled bytes needed to hold it all:** 95 MB (v0), 60 MB (v3), 46 MB (v4), about 30 MB with all tiers [I].
- **Real heap for the same set:** 30 MB (v0), 21 MB (v4).
- **Default split** (`LmdbFrontierPlannerSettings`): pipeline /16 = 4 MiB, packed /4 = 16 MiB, exact /16, filter /32,
  out of 64 MiB.

| Option | What it means | Fit |
|---|---|---|
| **O1. Keep /16 (4 MiB)** | Fine for hot sets of 15 to 27 distinct queries after the shrink. Larger cyclic sets get 0 hits under LRU. | 16–27 families |
| **O2. Pipeline /8 (8 MiB)** | Funded by lowering packed from /4 to /8, so total allocation is unchanged. A pipeline hit skips the whole pipeline; a packed hit skips only the search. Measure the packed hit-rate loss before choosing. | ≈ 30–55 families |
| **O3. Pipeline /4 (16 MiB), or raise the evidence default to 128 MiB keeping proportions** | Adds about 4–12 MB of real heap at the shrunk real size. | ≈ 60–110 families (most of the theme set) |
| **O4. Size by entry count** | Pipeline budget = target families × measured mean modeled bytes (for example 64 × 180 KB ≈ 11.5 MiB), checked by a startup self-test. | By design |

**Recommendation.**
- Land Tiers 0 and 1 first.
- Then adopt O2 if measurements show that packed-cache hit loss is small. Otherwise take O3 via the evidence default.
- Pair either with the TinyLFU victim rule, because no size short of the full working set rescues cyclic LRU.
