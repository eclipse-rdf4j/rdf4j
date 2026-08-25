# Build a decision-, certificate-, and continuation-aware LMDB query-plan cache

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept current as implementation proceeds.

This document follows `.agent/PLANS.md`. It is intentionally self-contained: a contributor should be able to resume the work from this file and the repository without relying on the conversation that created it.

## Purpose and user-visible outcome

RDF4J's LMDB optimizer currently has two independently serving caches. `LmdbPipelinePlanCache` retains a detached final `TupleExpr` for an exact query context and treats committed-data and statistics revisions as part of the cache key. The packed optimizer has its own `PackedPlanCache`, which retains exact or structural packed recipes and performs some stale-cost validation. Those policies duplicate ownership, discard executable plans whenever quality evidence changes, and do not retain the decision evidence or search continuation needed to decide whether an old plan is still safe, whether another retained plan is now better, or whether optimization can resume without repeating completed work.

After this work, an LMDB store owns one `LmdbPlanDecisionCache`. A repeated exact invocation normally takes an expected constant-time alias path. A related parameterized invocation authenticates a collision-safe query family, computes a small decision-sensitive feature vector, and dispatches across at most four guarded variants. Each variant owns an immutable champion, diverse challengers, a decision certificate, evidence dependencies, a conservative stability envelope, runtime evidence, and an optional detached packed-search checkpoint. A lookup returns one of three explicit policies: use the plan, use it while scheduling refresh, or synchronously plan before use. A statistics or data revision affects plan quality, not executable semantics. Removing an index, physical provider, recipe generation, or other hard capability invalidates before execution.

The cache is an optimization only. Unsupported parameterization, collisions, stale publication races, corrupt checkpoints, refresh failures, shutdown races, or any internal cache exception fail closed to the existing ordinary synchronous optimizer. They do not fail a query.

This remains an interpreted/batched execution system. The work does not introduce runtime Java source or bytecode generation.

## Scope and boundaries

The first implementation is specific to `core/sail/lmdb`. Store policy, admission, evidence invalidation, variants, refresh scheduling, canaries, lifecycle integration, diagnostics, configuration, and eviction live in that module. Reusable representations for parameterized packed identity, immutable optimization decisions, candidate portfolios, recosting, and detached resumable search live in `core/queryalgebra/evaluation` under the packed optimizer package.

Plan families and checkpoints are process-local. Existing persisted LMDB learning and correction evidence remains persisted and may inform a newly built decision after restart. The decision cache itself starts empty after restart.

Remote `SERVICE` algebra and one-shot or non-repeatable binding iterables continue to bypass retention. The LMDB optimizer currently receives already-authorized algebra and has no security-principal contract, so no principal is added to the family key. If that upstream contract changes, it becomes a hard semantic dependency.

Live, guarded challenger canaries are included. Hedged execution, mid-query materialization and resume, cardinality-triggered cancellation, and execution-plan switching are not included. Guard breaches only affect subsequent dispatch, canary eligibility, and refresh priority.

No legacy/new cache mode will be exposed. A temporary internal adapter may keep the existing serving path alive while a milestone is incomplete, but the final state has one cache owner and one validity policy. A zero evidence budget disables retention and leaves ordinary synchronous planning unchanged.

## Repository state and preservation rule

At the start of implementation on 2026-08-24, `HEAD` is `61127e7634` on branch `GH-0000-lmdb-predicate-guarantees`; the upstream shown by Git is `origin/GH-0000-lmdb-predicate-guarantees` at `fb16ef0ba4`. The tracked index and working tree are clean. The checkout contains a very large unrelated untracked corpus, including existing ExecPlans, optimizer research documents, `papers2/`, benchmark profiles and LMDB stores under `profiles/`, generated/native test material below `core/sail/lmdb/${rdf4j.test.tmpRoot}/`, scripts, ZIP bundles, and prior evidence files. These files are user-owned. Do not clean, restore, move, overwrite, stage, or otherwise normalize them. Before editing an overlapping tracked file, inspect `git status --short --untracked-files=no` and `git diff -- <path>` again. If another actor changes the same tracked file, preserve the new work and reconcile deliberately.

The required initial command completed successfully with JDK 25 on 2026-08-24:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install

The reactor reported `BUILD SUCCESS`, total time `50.669 s`. This is a compile/package baseline with tests skipped by the quick profile; it is not correctness evidence for this feature.

## Progress

- [x] (2026-08-24) Read `.agent/PLANS.md`, select Routine D, inspect the cache and packed-search architecture, and record the implementation contract.
- [x] (2026-08-24) Audit branch, tracked state, unrelated untracked artifacts, existing plan-cache paths, packed decision/certificate support, runtime feedback, lifecycle state, configuration, and benchmarks.
- [x] (2026-08-24) Run the required offline root `-Pquick clean install`; reactor green in 50.669 seconds.
- [x] (2026-08-24) Add focused failing tests for parameterized family identity and safe current-invocation materialization, then cover typed schemas, stable repeated slots, binding rows, and collision-authenticated existing behavior.
- [x] (2026-08-24) Implement packed family authentication, parameter schema/vector, structural parameter references, detached publication identity, family single flight, and recipe materialization against the current concrete query. Existing 40-test packed cache class remains green.
- [x] (2026-08-24) Add focused failing tests for immutable LMDB family versions and all lookup decisions, including semantic invalidation, quality drift, stale publication, lifecycle/resource guards, close, and zero-budget failure fallback.
- [x] (2026-08-24) Implement the immutable `LmdbPlanDecisionCache` owner with orthogonal state, exact aliases, guarded family lookup, atomic publication, evidence events, runtime observations, and two-version rollback retention. Six direct cache tests are green.
- [ ] Finish moving packed and exact entries beneath the unified family owner while preserving compatibility metrics; the current pipeline class is now a transitional facade but packed publication still needs the shared portfolio/checkpoint owner.
- [x] (2026-08-24) Add focused failing tests for bounded materializable portfolios and LMDB feature dispatch, robust fallback, deterministic merge, and the default four-variant budget.
- [ ] Complete portfolio recosting and calibrated promotion. Detached champion plus up to three executable, stratum-diverse challengers, conservative interval state, guarded dispatch, robust fallback, deterministic adjacent merge, interval-gated promotion, two retired rollback versions, and deterministic guarded canary selection are implemented.
- [x] (2026-08-24) Add and round-trip bounded variant, refresh-thread, and canary-fraction configuration; add a store-owned priority executor with per-family/variant single flight and daemon shutdown.
- [x] (2026-08-24) Add focused failing tests for detached work-limited checkpoints and safe re-entry that reaches the uninterrupted exact winner.
- [ ] Refactor packed search into a resumable phase machine and implement immutable memo/frontier detach/rehydration. Work-limited scalar and evidence-equivalent provider-backed decisions now reconstruct the selected winner DAG in a fresh memo and resume enumeration with fewer new work units; provider events are replayed into the current private session before restoration, with recost work reported separately and included in cumulative work. Changed provider digests reopen ordinary search and reach the uninterrupted current-evidence winner. The complete 46-test packed cache class and 40-test subset-kernel class are green. Property/context interner definitions, cost-pruning attribution, and mid-phase loop cursors still reopen conservatively.
- [x] (2026-08-24) Add focused failing tests for plan-version runtime tokens, complete/censored publication, guarded deterministic canaries, interval promotion, rollback, refresh alias/checkpoint preservation, generation-stable fine-grained evidence routing, configured scheduler shutdown, and aggregate planner budget accounting.
- [ ] Finish feedback/scheduler/diagnostic integration. Existing LMDB feedback publication now deduplicates plan tokens, classifies complete versus censored executions, and updates the variant posterior; cache configuration owns scheduler lifecycle; canary and lifecycle gates are implemented. Cache-hit explanations now propagate search completion, bound kind, evidence epoch, and stability-envelope result from the selected immutable variant. Detached automatic refresh submission, complete JFR coverage, and tiered eviction remain.
- [x] (2026-08-25) Profile and repair the MEDICAL_RECORDS repeated-query regression. Preserve aggregate decision-cache admission across routing segments and consult the authoritative lifecycle policy before hard-blocking a plan from runtime feedback. Q3 returned from 1.45 seconds to 86.6 milliseconds and Q0 to 35.6 milliseconds in the short controlled benchmark; the post-diagnostic cache/feedback group is green with 52 tests.
- [ ] Remove duplicate serving policy and run focused, module, integration, benchmark, allocation, and JFR acceptance checks.
- [ ] Audit final diff/status, record exact verification boundaries, and complete the retrospective. Do not stage or publish without a separate user request.

## Surprises & Discoveries

- Observation: the plan's warning about substantial tracked modifications was stale by the time implementation began. The current commit `61127e7634` incorporates that work and the tracked tree is clean, while the unrelated untracked corpus remains enormous.
  Evidence: `git status --short` showed only `??` entries; `git diff --stat` and `git diff --cached --stat` showed no tracked patch.

- Observation: the current architecture already contains more reusable pieces than the old cache names imply. `PackedPlanRecipe` is detached and materializable, `PackedDecisionCertificate` retains candidate and lower-bound state, `PackedCostingReplay` recosts a recipe, and runtime feedback already distinguishes termination and censored observations. The implementation should extend these contracts rather than duplicate them.
  Evidence: source inspection of the packed optimizer and LMDB feedback packages before this ExecPlan was created.

- Observation: `PackedIncumbentSearch.build()` is monolithic but its code already follows identifiable phases. The checkpoint work must make those transitions and loop cursors explicit without retaining a live mutable `PackedMemo`.
  Evidence: current canonical seeding, logical-alternative installation, access seeding, join/correlation enumeration, changed-input propagation, and root certification blocks in `PackedIncumbentSearch`.

- Observation: concrete RDF values were not the only obstacle to stable parameterized recipes. `PackedObjectPool` interns equal values, so a query with the same RDF value in two positions previously had a different object graph from the same shape with two distinct values. Assigning one structural `PackedParameterReference` per occurrence before interning makes recipe ordinals independent of invocation-local value equality.
  Evidence: `PackedQueryFamilyIdentityTest.repeatedAndIndependentConstantSlotsHaveOneStableFamilyShape` passes with two slots for both invocations.

- Observation: exact query templates and family authentication need different retention semantics. An exact alias is permitted to retain exact invocation values, while a published family key must not. `PackedQueryFamilyIdentity.detached()` therefore retains only authentication tokens and the schema and rejects vector/concrete-query access.
  Evidence: `PackedQueryFamilyIdentityTest.sameRdfShapeSharesFamilyWhileVectorsRemainInvocationLocal`.

- Observation: LMDB's committed-data, Frontier, LEO, and learned-correction revisions can be removed from hard cache identity without weakening execution safety because the executable template's dataset, binding/configuration semantics, optimizer/evaluation generations, and capability compatibility remain separately authenticated. Moving those revisions into the evidence snapshot changes the same cached version from `USE` to `USE_AND_REFRESH` instead of producing a miss.
  Evidence: the original `committedDataRevisionKeepsExecutableChampionAndRequestsRefresh` run failed on an exact miss, and the unchanged test now passes through `LmdbPlanDecisionCache` with `QualityState.SUSPECT`.

- Observation: a cache exception must be handled at both policy boundaries. `LmdbPlanDecisionCache` returns `REPLAN_BEFORE_USE` for ordinary invalidity, while its pipeline facade also catches unexpected cache failures and falls back to normal synchronous planning. This preserves the query even if retained state is corrupt or publication races.
  Evidence: `LmdbPlanDecisionCacheTest.zeroBudgetAndCloseFailClosedWhileActiveVersionsRemainUsable` and the existing pipeline miss path.

- Observation: a final Pareto frontier can contain only the scalar champion even when useful executable alternatives were costed. A cache portfolio therefore cannot equate “challenger” with “final Pareto survivor.” Retaining one deterministic dominated representative per operator/form stratum supplies diverse, materializable challengers without retaining the entire candidate population.
  Evidence: the first implementation of `cachedDecisionRetainsBoundedDistinctMaterializablePortfolio` still failed with one candidate; the unchanged test passed after stratum sampling, executable-identity deduplication, and a four-candidate bound.

- Observation: canonical executable fingerprints are not available for every legal RDF4J `Value` implementation. Existing hash-unsafe test values are deliberately executable but unsupported by the canonical packed identity codec. Portfolio deduplication needs a local physical-control fallback for such exact-only queries, and that fallback must never authorize cross-query reuse.
  Evidence: the first broadened `PackedPlanCacheTest` run had 2 errors from `unsupported RDF value in packed query identity`; the subsequent 41-test run is green with control-only local portfolio identities.

- Observation: the current pipeline cache's planning callback closes over `LmdbQueryOptimizerPipeline`, whose fields include the connection-owned `TripleSource` and `EvaluationStrategy`. Submitting that callback to the store daemon would retain transaction-bound state after lookup returns, even if the `TupleExpr` itself were cloned.
  Evidence: the focused refresh-facade test proved that no background continuation is currently submitted; source inspection of `LmdbQueryOptimizerPipeline.optimize` and its captured fields proves that directly submitting the existing callback would violate the detached-publication invariant. Automatic refresh must therefore resume a detached packed checkpoint or use a fresh store-owned planning context.

- Observation: the first checkpoint implementation authenticated and rebound correctly but repeated all deterministic work. A selected winner DAG is sufficient to avoid replaying canonical scalar costing even before the larger frontier snapshot exists: recipe rows are child-before-parent, so detached goal/cost columns can reconstruct the incumbent in a new memo and enumeration can continue from that phase.
  Evidence: `detachedCheckpointReentryMatchesUninterruptedExactWinner` first failed with `resumed=14, uninterrupted=14, checkpoint=5`; the unchanged assertion now passes and the full 43-test `PackedPlanCacheTest` class is green. Provider-backed state still deliberately takes the fresh path because replaying old evidence-sensitive costs without recosting would be unsound.

- Observation: evidence reverse-index routing must exclude its mutable generation. A changed key such as one predicate/statistics shard is still the same dependency identity at a newer generation; indexing the full dependency record prevented a fine-grained change from finding its family.
  Evidence: `fineGrainedEvidenceChangeMatchesStableIdentityAcrossGenerationAdvance` failed with `USE` and now returns `USE_AND_REFRESH` after routing by `(kind, identity)` while retaining generation on the certificate dependency.

- Observation: the configured evidence ceiling was previously granted once to `PackedPlanCache` and independently allocated again across LMDB planner caches. The total therefore approached twice the intended aggregate budget.
  Evidence: `packedAndLmdbPlannerCachesShareOneExactAggregateBudget` failed before the store/settings split and now proves the per-cache allocations sum exactly to an odd configured budget and to zero when retention is disabled.

- Observation: exact aliases are published after the family CAS, so alias version identity must advance atomically with every family version and late installation must be monotonic. Otherwise a valid alias becomes spuriously stale, or an older publication can overwrite a newer target.
  Evidence: the first broadened `LmdbPlanDecisionCacheTest` run had two stale-alias failures; all 12 tests pass after family-version reversioning and monotonic `ConcurrentHashMap.compute` installation.

- Observation: retaining the old sixteen-way per-segment byte share after moving ownership into one aggregate `LmdbPlanDecisionCache` rejects a valid large decision before the global owner can apply its real budget. Routing segments are concurrency/hash partitions, not independent retention authorities.
  Evidence: `planThatFitsAggregateDecisionBudgetIsNotRejectedByRoutingSegmentShare` failed because the plan fit the aggregate budget but not one sixteenth of it; the unchanged test passes after removing the facade's segment-share gate and leaving byte admission to the unified owner.

- Observation: monitoring-only lifecycle feedback was being interpreted as an authoritative hard block. A single complete execution whose operator work exceeded the diagnostic regression limit set `PlanExecutionAggregate.lifecycleBlocked`, causing the exact alias to alternate from hit to synchronous replan even though the active rollout profile had `planLifecycleEnforced=false`.
  Evidence: a temporary real-query diagnostic showed misses/hits/misses and a second family replan after the first cached observation; `monitoringOnlyLifecycleRegressionRemainsPosteriorEvidenceInsteadOfBlockingThePlan` then failed with `expected: <false> but was: <true>`. After consulting `PlanLifecycleStore.Decision` only for contracts admitting `ADMIT_LIFECYCLE`, the real query remained at one miss with two subsequent hits.

- Observation: the MEDICAL_RECORDS Q3 regression was planning time, not execution time or a changed physical plan. The JFR stack repeatedly entered `LmdbPipelinePlanCache.getOrCompute`, `optimizeUncached`, packed Cascades, and Frontier costing; the hottest methods were `FrontierStatisticsShard$BlockReader.mappedData` (28.75 percent) and `FrontierCenterIndex.matches` (24.67 percent).
  Evidence: pre-fix Q3 measured 1450.480 milliseconds per operation with `/tmp/rdf4j-async-profiler/medical-q3-decision-cache-20260824/q3-jdk.jfr`; after the cache/lifecycle repairs the same short harness measured 86.619 milliseconds per operation. Q0 measured 35.572 milliseconds against the user-supplied same-plan reference of 37 milliseconds. The async-profiler wrapper did not flush its advertised recording, so the retained diagnosis uses JDK Flight Recorder from the same benchmark process.

- Observation: provider-backed checkpoint restoration needs two distinct work counters. Replaying the selected decision's current evidence is cache recost/validation work; enumeration work is the unfinished search. Combining them in `PlanningMetrics.workUnits` obscured the avoided enumeration even though provider calls fell, while omitting recost from cumulative work would be dishonest.
  Evidence: `providerBackedCheckpointRecostsIncumbentBeforeResumingExactSearch` first failed with `resumed=11, uninterrupted=11, interrupted=8`. After replay and memo restoration, provider transitions fell but the test still failed until recost work moved to `optimizer.planCacheRecostWorkUnits`; the cumulative metric includes checkpoint, recost, and resumed-enumeration work. The unchanged focused test and the complete 44-test class then passed.

- Observation: event digests are a conservative evidence-equivalence boundary for the current checkpoint layer. The selected recipe can be restored with live provider states only when every replayed event digest matches; otherwise the partially completed cost space must reopen because the checkpoint does not yet attribute individual pruning records.
  Evidence: the temporary same-package diagnostic showed six identical cached/current event digests and successful memo restoration for the stable provider case and was removed. `providerBackedCheckpointReopensSearchWhenEvidenceDigestChanges` passes by matching the uninterrupted plan and total cost under a changed provider.

- Observation: corrupt provider continuation identity currently falls back cleanly even when the malformed row is encountered after valid rows. The fallback performs the same provider transitions and deterministic work as uninterrupted planning, so the speculative partial-memo hazard is not observable in the current search implementation and does not justify a production patch.
  Evidence: `corruptProviderCheckpointIsRejectedBeforeMutatingTheFreshMemo` mutates a late detached required-property ordinal and passes with identical plan, provider-call count, and enumeration work. The test remains as a fail-closed regression guard.

- Observation: bounded portfolio cardinality does not by itself bound portfolio construction cost. Each challenger was being extracted with a second full memo-wide decision certificate; on the 66-factor arbitrary-width proof query this repeated global certificate reconstruction and crossed the existing 120-second test limit.
  Evidence: the broad evaluation-module run failed only `arbitraryWidthBushyDependentJoinContinuesRightRecipeUnderWholeLeftPrefix`, with the timeout stack in `PackedMemo.decisionDraft` from challenger `PackedPlanRecipe.extract`. Challenger recipes now retain their executable DAG and reachable costing/evidence trace but not a redundant global certificate. The unchanged focused proof completes in 103.365 seconds, the full 40-test subset-kernel class passes, and all 46 packed cache tests pass.

- Observation: diagnostic values cannot be reconstructed reliably from the final materialized `TupleExpr`; they are properties of the selected immutable variant and its publication epoch. The transitional facade previously stamped feedback identity but silently dropped search completion, bound kind, evidence epoch, and stability-envelope coverage.
  Evidence: `materializedCacheHitStampsValueOnlyPlanVersionFeedbackToken` first failed with `expected: <EXACT_COMPLETE> but was: <null>`. The lookup decisions now carry the selected certificate metadata, and both the cache-hit and committed-data-drift envelope tests pass; the complete 12-test pipeline-cache class is green.

## Decision Log

- Decision: use Routine D and retain test-first milestone gates even though Routine D does not require pre/post evidence snippets.
  Rationale: this is a cross-module behavioral feature with concurrency, lifecycle, memory, and query-correctness risk. Small failing tests before each behavior surface make each transition reviewable and protect the old serving path until the replacement is safe.
  Date/Author: 2026-08-24 / Codex.

- Decision: preserve a single logical cache policy while allowing a temporary internal adapter during construction.
  Rationale: callers need a safe migration path, but exposing two modes would create permanent validity ambiguity. The old exact and packed caches cease to make independent serving decisions by the final milestone.
  Date/Author: 2026-08-24 / user and Codex.

- Decision: make full identity authentication authoritative and hashes routing-only.
  Rationale: parameterized reuse magnifies the consequence of a collision. Segment hashes can choose a bucket, but equality must authenticate the complete normalized structure, parameter schema, and semantic context before recipe materialization.
  Date/Author: 2026-08-24 / user and Codex.

- Decision: separate `SemanticValidity`, `QualityState`, and `DeploymentRole` in both model and code.
  Rationale: a data/statistics revision can make estimates stale without making a recipe unexecutable; capability removal can make a recipe unexecutable even if historical quality was excellent; challenger status is independent of both.
  Date/Author: 2026-08-24 / user and Codex.

- Decision: exact aliases are acceleration indexes into family/variant/version state, never independently authoritative entries.
  Rationale: an exact hit must still respect hard dependencies, lifecycle blocks, evidence quality, guards, and retained-version availability.
  Date/Author: 2026-08-24 / user and Codex.

- Decision: immutable publication plus compare-and-set is the concurrency boundary.
  Rationale: active executions can safely retain old Java object graphs without cache locks; refresh can run outside the serving lock; stale work cannot overwrite a newer family version.
  Date/Author: 2026-08-24 / user and Codex.

- Decision: wall-clock latency remains diagnostic/benchmark evidence for this delivery.
  Rationale: normalized cardinality, work, access, and peak-memory observations already have typed contracts and are less environmentally noisy. Promotion and poisoning must not hinge solely on elapsed time.
  Date/Author: 2026-08-24 / user and Codex.

- Decision: use the existing `LmdbPipelinePlanCache` only as an integration facade during the migration, with `LmdbPlanDecisionCache` as the sole serving entry store now.
  Rationale: the facade still supplies current optimizer call signatures and single-flight ownership, but its former exact-entry map no longer makes independent reuse or invalidation decisions. This lets each behavior transition remain focused-testable before the final class/name cleanup.
  Date/Author: 2026-08-24 / Codex.

- Decision: select packed challengers by distinct operator/form stratum before filling remaining slots by estimated cost.
  Rationale: this preserves join/access/implementation diversity rather than retaining several cheap executable duplicates. Full executable identity still deduplicates candidates, and the selected champion always occupies slot zero.
  Date/Author: 2026-08-24 / Codex.

- Decision: merge specialized applicability regions only when they differ along at most one overlapping dimension, have identical non-cardinality guards and executable fingerprints, and have overlapping runtime residual intervals.
  Rationale: a convex multi-dimensional union can silently cover an unobserved hole. The stricter one-dimensional rule is deterministic and conservative.
  Date/Author: 2026-08-24 / Codex.

- Decision: do not submit `LmdbPipelinePlanCache.getOrCompute`'s existing computation callback to the refresh executor.
  Rationale: that callback captures connection-scoped execution objects and would keep a transaction/snapshot provider alive beyond its request. The scheduler accepts only detached continuation work; wiring automatic refresh therefore follows checkpoint detach/rehydration rather than preceding it.
  Date/Author: 2026-08-24 / Codex.

- Decision: detach the selected winner DAG as the first immutable checkpoint layer and resume only the evidence-independent scalar enumeration path from it.
  Rationale: this is a real reusable structural checkpoint—defensive primitive arrays plus an immutable recipe, reconstructed into a private memo—and it measurably avoids repeated canonical work. Provider-backed costs and pruning are not replayed until their dependencies can be recosted and invalid records reopened; those requests continue through ordinary safe planning rather than using stale bounds.
  Date/Author: 2026-08-24 / Codex.

- Decision: route reverse evidence dependencies by stable `(kind, identity)` and keep generation in the dependency value and certificate.
  Rationale: generation is what changed and therefore cannot be part of the lookup identity used to discover affected families.
  Date/Author: 2026-08-24 / Codex.

- Decision: split the existing aggregate planner evidence budget centrally in `LmdbFrontierPlannerSettings`, including the packed cache share, and assign the arithmetic remainder to one cache.
  Rationale: central ownership makes the sum exact for zero, odd, and default budgets and prevents the packed cache from independently consuming the full configured ceiling.
  Date/Author: 2026-08-24 / Codex.

- Decision: make routing segments capacity/concurrency partitions only; the unified `LmdbPlanDecisionCache` is the sole byte-admission authority.
  Rationale: dividing one aggregate byte ceiling among hash segments creates false misses for complex but affordable plans and reintroduces independent cache policy at the facade.
  Date/Author: 2026-08-25 / Codex.

- Decision: derive `lifecycleBlocked` from the authoritative `PlanLifecycleStore.Decision`, gated by the feedback contract's `ADMIT_LIFECYCLE` capability, rather than directly comparing every monitoring target with its regression limit.
  Rationale: rollout profiles intentionally separate diagnostic/posterior feedback from enforcement. Monitoring evidence may make quality suspect or prioritize refresh, but it cannot hard-invalidate an executable cached recipe when lifecycle enforcement is disabled.
  Date/Author: 2026-08-25 / Codex.

- Decision: replay detached provider events into the new private cost session before restoring an incumbent, and restore only when the current event digests authenticate evidence equivalence.
  Rationale: checkpoint objects retain no provider/session state. Replay supplies query-local current evidence IDs and metadata; exact digest equality justifies retaining the detached costs. Any changed or incompletely attributable event reopens ordinary search rather than reusing a stale pruning claim.
  Date/Author: 2026-08-25 / Codex.

- Decision: report checkpoint recost work separately from resumed enumeration work and include both in cumulative deterministic work.
  Rationale: this follows the existing stale-plan validation accounting model, exposes the actual search work avoided, and still accounts for every provider replay transition across the checkpoint lifecycle.
  Date/Author: 2026-08-25 / Codex.

- Decision: retain the full search decision certificate only on the champion; challenger recipes retain the executable DAG plus their reachable costing/evidence trace.
  Rationale: the family owns one search certificate, while each challenger needs to remain materializable and recostable. Rebuilding the same memo-wide certificate for every challenger is redundant and made portfolio capture scale with the entire search space rather than the bounded retained portfolio.
  Date/Author: 2026-08-25 / Codex.

- Decision: carry search-completion, bound, publication-epoch, and envelope state on `LookupDecision` rather than infer them in the pipeline facade.
  Rationale: lookup selects the family version and variant under one immutable snapshot. Re-reading mutable owner state or reverse-engineering the final algebra could report metadata for a different publication and would make exact aliases diagnostically misleading.
  Date/Author: 2026-08-25 / Codex.

## Architecture and invariants

### Pipeline boundary and identity

`LmdbQueryOptimizerPipeline` is split into three conceptual stages. Before any optimizer runs, the cache may attempt an exact-source alias lookup using the complete source identity plus semantic context. On a miss it runs the existing value-sensitive prefix through `LmdbFilterHoistOptimizer`. Only then does it construct a `PackedQueryFamilyIdentity` and query the family cache at the Cascades boundary. This order is required because binding assignment, constant folding, comparison simplification, set-semantics rewrites, and filter simplification may turn superficially similar source queries into different normalized algebra.

The packed package gains these immutable concepts:

    PackedQueryFamilyIdentity
      canonicalIdentity: complete collision-authenticated normalized structure
      parameterSchema: PackedParameterSchema
      parameterVector: PackedParameterVector
      concreteQuery: PackedQuery

`PackedParameterSchema` records stable slot ordinals, the RDF value category, datatype or language semantics where relevant, source binding names, boundness, and `BindingSetAssignment` row/column shape. `PackedParameterVector` holds only invocation-local values corresponding to that schema. Parameterization occurs after normalization and only in semantics-preserving value positions. Operator kind, function identity, comparison and evaluation semantics, dataset, required result properties, initial binding shape, inference/evaluation mode, and feature generations remain part of the family context.

Anonymous internal variables may be alpha-normalized when they are not root-visible. Root-visible variables, binding names, and observable result shape remain exact. Binding rows are parameterized by stable shape and materialized from the current invocation. A recipe references structural and parameter-slot ordinals; it never retains a mutable binding iterable or RDF values from an earlier invocation.

The identity API must make it difficult to use a routing hash as authority. Equality/authentication compares canonical bytes or an equivalently complete immutable representation and the schema. Tests inject collisions to prove that a bucket match cannot authorize reuse.

### LMDB ownership model

The package-private LMDB owner exposes this functional surface:

    LmdbPlanDecisionCache
      lookupExact(...)
      lookupFamily(...)
      publishBuild(...)
      publishRefresh(...)
      observeExecution(...)
      markEvidenceChanged(...)
      statistics()
      close()

The durable published object graph is immutable apart from atomic owner references and bounded counters:

    PlanFamilyKey
      canonicalPackedIdentity
      datasetIdentity
      initialBindingSchema
      requiredResultProperties
      inferenceMode
      optimizerFeatureGeneration
      evaluationFeatureGeneration
      environmentClass

    PlanFamily
      key
      AtomicReference<FamilyVersion> current
      refreshClaim
      admission and retention counters

    FamilyVersion
      monotonically increasing version
      semanticDependencies
      dispatcher
      retainedStructuralCheckpoint
      exactAliases
      publicationEvidenceEpoch

    PlanVariant
      variantId
      applicabilityGuard
      champion
      challengers
      searchCertificate
      evidenceSnapshot
      stabilityEnvelope
      runtimePosterior
      qualityState

    PlanVersion
      physicalRecipe
      optional detached exact TupleExpr template
      physicalFingerprint
      estimatedCostInterval
      deploymentRole
      buildEvidenceEpoch
      optimizerGeneration

The state axes are exactly:

    SemanticValidity = VALID | HARD_INVALID
    QualityState = CONFIDENT | PROVISIONAL | SUSPECT | REFRESHING
    DeploymentRole = CHAMPION | CHALLENGER | RETIRED

Every lookup returns a sealed `LookupDecision`:

- `USE` only when the invocation guard covers the current feature vector, the champion is executable, lifecycle permits it, its quality is confident, and current evidence remains within its stability envelope.
- `USE_AND_REFRESH` when the champion is executable but provisional/suspect and estimated regret is within the configured bound, or when a globally guarded robust fallback safely covers an invocation outside specialized regions.
- `REPLAN_BEFORE_USE` when a hard dependency is invalid, the recipe is incompatible, lifecycle blocks it, no plan guard covers safely, a resource guard is violated, no robust fallback exists, or a retained challenger is already known to dominate.

Any internal exception produces a miss/replan result and is recorded diagnostically. It is never allowed to escape as the reason a query fails.

Single-flight claims cover one family build and one family/variant refresh. They never cover query execution. Publication uses compare-and-set against the family version observed by the builder. Before publishing, the cache rechecks hard dependencies and evidence epochs. If quality evidence moved, it performs a cheap recost or publishes provisional; it never labels stale work confident. Active executions hold ordinary strong references to immutable versions. The cache retains the current and two previous champions for immediate rollback.

### Semantic and evidence dependencies

Hard semantic dependencies include the normalized identity/schema, dataset and initial binding semantics, required result/evaluation properties, LMDB index and physical-capability generations, packed recipe and optimizer-rule generations, evaluation strategy generation, relevant optimizer configuration, and immutable provider identity needed by a physical recipe. No retained object may hold an LMDB transaction, cursor, `TripleSource`, mutable estimator session, snapshot, or mutable query execution state.

Quality dependencies are typed and versioned. They include a conservative global committed-data generation; predicate, context, and statistics-shard generations; leaf, join, and correlation keys; LEO and Frontier correction keys; plan-lifecycle keys; and physical-capability generation. Capability removal is hard. Ordinary data, statistics, learned correction, and lifecycle-evidence movement marks a plan suspect or provisional and triggers validation/recosting.

The LMDB cache maintains a bounded reverse index from evidence keys to family identifiers. Fine-grained events affect indexed families. A broad event increments a global evidence epoch, avoiding an unbounded cache walk; lookups validate lazily. If dependency attribution is incomplete, validation is conservative.

### Decisions, candidates, envelopes, and variants

`PackedDecisionCertificate` evolves into or is wrapped by an immutable `PackedOptimizationDecision`. It carries the selected recipe, at most three materializable challengers, cost intervals, physical fingerprints, completion and bound kinds, evidence/pruning epochs, cost-pruning margins, and an optional checkpoint. A lower bound is exposed only when the search can defend it. `BEST_KNOWN_UNBOUNDED` is used when no global bound exists.

Candidate retention is diversity-aware. It prefers distinct join orders, access paths, join algorithms, memory profiles, startup/latency profiles, and plans near crossovers. Near-duplicate cheapest candidates do not crowd out useful alternatives.

Risk settings retain their existing defaults and clamps: initial confidence `0.99`, minimum `0.51`, maximum `0.999`, and maximum expected regret `0.01`. The total planner evidence budget remains the existing `frontierCacheEvidenceBudgetBytes`, default `64 MiB`; the decision cache receives a rebalanced share rather than increasing the aggregate ceiling.

Plausible gain is the positive difference between the champion's calibrated upper estimate and the best alternative's calibrated lower estimate. Refresh requires positive plausible gain and expected future uses sufficient to amortize remaining optimization work. Promotion is stricter: a challenger's calibrated upper bound must beat the champion's calibrated lower bound by the meaningful-gain floor or calibrated complete runtime evidence must establish equivalent interval domination. Censored evidence alone cannot promote.

`PlanStabilityEnvelope` contains only dimensions used by the certificate. Champion and challengers are recosted at current low/high evidence bounds, pairwise crossover points are located for sensitive dimensions, and safe ranges are intersected. Interactions that cannot be bounded yield a narrow envelope and refresh, never a broad unsupported claim.

The dispatcher sees a compact feature vector: relevant leaf and intermediate cardinalities, binding mask and parameter-type schema, existing high-degree/skew classifications, and required row/limit goal. It does not store permanent raw-value ranges. A family has at most the configured number of variants, default four including one robust global fallback. A split requires credible different winners and avoided regret greater than maintenance cost. Adjacent compatible regions with the same champion merge deterministically.

### Resumable packed search

`PackedIncumbentSearch` becomes an explicit resumable state machine:

    SEED_CANONICAL
    INSTALL_LOGICAL_ALTERNATIVES
    SEED_ACCESS_ALTERNATIVES
    ENUMERATE_JOIN_AND_CORRELATION_REGIONS
    PROPAGATE_CHANGED_INPUTS
    CERTIFY_ROOT
    EXACT_COMPLETE

An immutable primitive-array-backed `PackedSearchCheckpoint` contains the normalized parameterized template, logical groups/equivalences/properties, physical implementations, rule state, dependency topology, phase and loop cursors, candidates and winners, continuation keys/open worklist, separate structural and cost pruning records, dependency generations and margins for cost pruning, and cumulative deterministic work/retained bytes.

A refresh creates a private query-local memo by rehydrating the checkpoint and applying the current parameter vector and evidence snapshot. It never mutates the published checkpoint or family version. Structural impossibility and property-dominance pruning may survive evidence changes. Cost pruning survives only if dependencies and margins still justify it. Missing attribution reopens all affected cost-pruned work.

Completion remains precise: `EXACT_COMPLETE` means the generated search space is exhausted for the captured feature generation. Work, deadline, resource, and unsupported outcomes stay distinct. A resource-limited or unsupported result may carry a safe fallback but cannot satisfy an exact request. A newly added rule/capability changes search-space generation, makes old exactness provisional, and appends work to the continuation without hard-invalidating the incumbent.

### Refresh, runtime evidence, and canaries

The LMDB store owns a priority refresh scheduler. The default is one daemon planning thread. Priority grows with request rate, expected regret per use, and probability that search can help, and decreases with estimated remaining optimization cost. A family/variant has at most one active refresh claim. Serving only reads immutable state and does not hold a cache lock while planning, recosting, or resuming.

Packed recipe materialization stamps `LmdbRuntimeFeedbackDescriptor` with an immutable token containing family, variant, plan-version, and invocation-region identifiers. The token is an identity value, not a pointer to a mutable cache entry. Existing feedback publication feeds complete cardinality/work/access/memory/resource residuals into the variant posterior. Cancellation, timeout, failure, abandonment, LIMIT, and partial consumption are censored: their lower-bound and safety evidence is retained, but they are not complete runtimes. Wall-clock latency is diagnostic only.

At most `frontierPlanCacheMaximumCanaryFraction`, default `0.01`, of eligible invocations may take a challenger. Selection is deterministic. A canary requires guard coverage plus safe predicted upper resource and regret bounds. It is forbidden during hard dependency movement, outside the guard, after lifecycle block/quarantine, or where evidence is incomplete/censored. A lifecycle/resource breach stops challenger serving immediately. Promotion retains the prior two champions for rollback.

Shutdown stops admission, cancels/drains refresh tasks, publishes no new state after closure, and releases scheduler resources. It does not invalidate immutable objects already held by active executions.

### Admission, eviction, configuration, and diagnostics

First use admits a probation entry with the executable recipe, certificate, and compact portfolio. A second hit or sufficient saved planning work promotes it to a full family eligible to retain checkpoint/frontier state.

Eviction within a family removes, in order, disposable aggregated diagnostics, retired exact templates, low-value challengers, open continuation/frontier, structural checkpoint, then the family. Family ranking combines expected future hits, saved planning work, continuation value, safe-fallback value, and retained bytes; recency is only a tie-breaker. All planner caches together remain at or below `frontierCacheEvidenceBudgetBytes`.

Configuration adds and round-trips:

- `frontierPlanCacheMaximumVariants`, integer default `4`, range `1..16`.
- `frontierPlanCacheRefreshThreads`, integer default `1`, range `1..16`.
- `frontierPlanCacheMaximumCanaryFraction`, double default `0.01`, range `0.0..0.25`.

Explanations add lookup outcome, family/variant/version identifiers, quality and role, refresh reason, search completion and bound kind, resumed work, evidence epoch, canary status, and envelope result. Existing `optimizer.pipelinePlanCacheHit`, `optimizer.cascadesPlanCacheHit`, and `optimizer.cascadesQueryTemplateCacheHit` remain but are derived from the same returned decision/version. Statistics and JFR events expose admission, lookup, refresh, promotion, rollback, invalidation, checkpoint, and eviction behavior without retaining query values.

## Milestone plan and acceptance

### Milestone 1: family identity and safe materialization

First add focused packed tests that demonstrate the current implementation cannot identify two normalized queries as one family while keeping different parameter vectors. Include RDF terms and typed literals, alpha-equivalent hidden variables, root-visible variables, binding masks/types, binding assignment shape and rows, nested expressions, datasets/semantic context, collision injection, unsupported slots, and failure fallback. Keep the old cache serving while this surface is incomplete.

Implement `PackedParameterSchema`, `PackedParameterVector`, and `PackedQueryFamilyIdentity` in the packed package. Parameterize after the normalization prefix. Extend recipe references/materialization so every value and binding row comes from the current `PackedQuery` and schema-compatible vector. Add an exact alias representation that points to a future family/variant/version owner but can initially be exercised in isolation.

Acceptance: the focused tests fail before code and pass afterward; existing `PackedPlanCacheTest` remains green; current exact materialization never retains or leaks values from the earlier invocation; collision tests prove full authentication.

### Milestone 2: immutable family owner and lookup decisions

Add `LmdbPlanDecisionCacheTest` first. Cover every row of the lookup policy, exact alias delegation, semantic versus quality invalidation, single-flight equivalent misses, publication races, immutable old-version retention, and cache-failure fallback.

Implement the LMDB model and store owner with bounded segments, immutable versions, hard and quality dependencies, reverse evidence indexing, exact aliases, CAS publication, and the three sealed decisions. Integrate enough of the optimizer pipeline to publish and reuse a champion while the old adapters still protect unsupported cases. Ensure lifecycle checks happen before any alias, posterior lease, or cached-plan return.

Acceptance: all lookup tests pass, data/statistics drift yields safe `USE_AND_REFRESH` rather than semantic deletion, physical capability removal returns `REPLAN_BEFORE_USE`, and active holders see a stable old version during promotion/eviction.

### Milestone 3: unified cache ownership

Move current packed `PlanEntry`/`QueryEntry` state underneath the family version, and make the outer exact cache an alias index into that state. Remove independent admission, validity, and serving choices from `PackedPlanCache` and `LmdbPipelinePlanCache` while temporarily retaining narrow compatibility facades if call-site migration requires them. Split `LmdbQueryOptimizerPipeline` into exact lookup, normalization prefix, and family/Cascades stages.

Acceptance: one lookup result controls all three compatibility hit metrics; no exact or packed entry can serve after its family/version is invalid; exact cloning and cross-execution immutability tests remain green; unsupported cases synchronously use the ordinary pipeline.

### Milestone 4: portfolios, recosting, envelopes, and variants

Add failing tests for diverse root candidates, updated cost intervals and dependency dimensions, envelope crossover boundaries, robust fallback, deterministic dispatch, region split/merge, strict promotion, and maximum variants.

Introduce `PackedOptimizationDecision`, candidate diversity scoring, richer `PackedCostingReplay` output, `PlanStabilityEnvelope`, compact dispatch features, applicability guards, and bounded variant construction. Keep all recosting and region maintenance outside serving locks.

Acceptance: up to three materially distinct challengers remain executable; candidate/certificate lower bounds are honest; an unsafe or dominated champion is not served; the family never exceeds its configured variant limit; repeated construction is deterministic.

### Milestone 5: detached resumable search

Add failing tests comparing work-limited and deadline-limited resume against uninterrupted exact search, including OPTIONAL, MINUS, EXISTS, NOT EXISTS, subqueries, UNION, filters, and nested combinations. Tests must distinguish all completion outcomes and prove conservative reopening of cost pruning.

Refactor the search into the seven explicit phases. Add checkpoint detach and memo rehydration using immutable primitive arrays and explicit cursors. Attribute pruning dependencies and margins. Preserve cumulative deterministic work so resumed and uninterrupted evidence is comparable.

Acceptance: resumed search reaches the same exact winner/fingerprint/certificate as uninterrupted search for the same feature generation; structural pruning survives validly; stale or incompletely attributed cost pruning reopens; no checkpoint retains a live memo, transaction, provider session, or invocation RDF value.

### Milestone 6: feedback, refresh, canary, and rollback

Add failing LMDB tests for feedback tokens, complete versus censored observations, refresh priority and single flight, evidence races, guarded deterministic canaries, promotion, immediate lifecycle/resource blocking, two-version rollback, exception isolation, eviction during active use, and shutdown thread/resource cleanup.

Implement the daemon scheduler, refresh tasks, version token propagation, posterior updates, canary selection, promotion/rollback, and close behavior. Cheap recosting precedes resumed search where sufficient. Publications revalidate hard and evidence state.

Acceptance: censored observations cannot promote; at most the configured safe fraction canaries; evidence races cannot publish stale confidence; refresh failure has no query correctness effect; shutdown leaves no refresh thread or LMDB resource leak.

### Milestone 7: operations, configuration, diagnostics, and complete switch

Add round-trip/range tests for the three settings, aggregate byte-budget tests, tiered admission/eviction tests, explanation/metric/JFR tests, and compatibility metric tests. Route every supported retained request through the decision cache, remove duplicate serving policy, and document any compatibility facade that remains source-only.

Acceptance: zero budget disables retention; all planner retention remains within the configured aggregate; diagnostics carry no RDF values; the old classes no longer own independent policy; configuration round-trips and rejects out-of-range values consistently with existing settings.

## Test-first and evidence procedure

Before touching each behavior surface, add the smallest focused automated test and run it with `mvnf` using `--retain-logs`. Do not alter production code until that test has been observed failing for the intended missing behavior. Keep the assertion unchanged through implementation. Do not use `-am` or `-q` when tests run.

Use selectors such as:

    python3 .codex/skills/mvnf/scripts/mvnf.py PackedQueryFamilyIdentityTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py PackedPlanCacheTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPlanDecisionCacheTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbPackedPlanCacheTest --retain-logs

`mvnf` performs the required root quick install and then a focused module verify without test-time `-am`. Copy the exact Surefire/Failsafe report path and the relevant summary into this plan or an evidence file before later runs overwrite it. Routine D does not require the Routine A initial-evidence artifact, so do not overwrite any existing top-level evidence file merely to satisfy a filename convention.

After focused green, broaden in this order: touched class, neighboring packed or LMDB cache classes, full `core/queryalgebra/evaluation`, full `core/sail/lmdb`, then Theme/integration correctness. Keep results classified as focused green, module green/red, integration green/red, aborted, blocked, or unrun.

End-to-end comparison runs retention disabled and enabled with complete result-bag equality. Cover JOIN, UNION, FILTER, OPTIONAL, MINUS, EXISTS, NOT EXISTS, paths, subqueries, DISTINCT, GROUP, LIMIT, incoming bindings, named graphs, and committed-data changes. Every priming, cache-window preparation, canary, and measured query execution has the existing five-second timeout.

## Performance validation

Extend `LmdbFrontierPlanningBenchmark` with one-method benchmarks for cold build, exact-alias `USE`, parameter-family dispatch, cheap recost, and resumed search. Run one method at a time through:

    scripts/run-single-benchmark.sh <fully-qualified-benchmark-method>

Use identical Java, dataset, warmup, fork, and measurement settings for paired results. For Theme execution, keep priming outside the timed region and use `--enable-jfr`. The exact alias target is within five percent of the prior exact cache unless repeated evidence proves improvement. Certified family `USE` must not perform full enumeration. Fixed-plan execution must have no statistically credible regression above five percent on representative selective, broad, OPTIONAL/MINUS, and EXISTS cases.

The performance handoff must state workload, algorithm, data structures, execution model, hot path, exact commands, paired benchmark distributions, allocation differences, JFR/JIT evidence, and confidence. No faster/no-regression claim is made without repeatable measurements and matching profile evidence.

## Implementation order and dependency constraints

The order is deliberate. Parameterized identity and safe recipe rebinding precede family reuse because a cache owner cannot safely dispatch across values without them. The immutable family model precedes refresh because refresh needs a publication and ownership boundary. Portfolios and recosting precede variants/envelopes because guards need competing plans and cost dimensions. Checkpointing follows an executable unified decision because a continuation is only valuable when attached to a safe incumbent. Feedback tokens follow stable family/variant/version identity. Duplicate policy is removed only after exact aliases, family dispatch, refresh, failure fallback, configuration, and shutdown are green.

Do not let an intermediate shortcut become the final design. In particular, do not key a family only by hashes, treat all revisions as semantic, retain old RDF values in a recipe, hold serving locks during planning, retain a mutable memo, infer an optimality gap without a bound, promote on latency alone, canary outside a guard, or let a compatibility cache authorize execution independently.

## Idempotence and recovery

All diagnostic commands are read-only and repeatable. Focused tests may overwrite their own report directory; retain logs and record the decisive snippet before the next run. Maven build output belongs in `maven-build.log` as required by repository instructions.

If an offline build lacks a dependency/plugin, rerun the exact command once without `-o`, then return offline. For another root install failure, retry without `-T 1C` and diagnose the exact error. Do not clean untracked files or reset the repository.

If production code is accidentally edited before the milestone's failing test is captured, stop, revert only the known new edit with a targeted `apply_patch`, and resume from the failing test. Never use `git restore`, `git checkout --`, `git reset --hard`, or `git clean` for recovery.

If a refresh/checkpoint path proves unsafe, the recoverable runtime state is an ordinary synchronous plan plus the immutable incumbent. Disable publication of the incomplete path internally, preserve the tests and discoveries here, and continue with the safe milestone boundary; do not expose a user-facing second cache mode.

If concurrent or user changes appear in an overlapping file, inspect the exact path diff, preserve those changes, and rebase the local patch conceptually. Stop for direction only if the two intents cannot be reconciled without choosing which work to discard.

## Expected source areas

Packed reusable work belongs under:

    core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/packed/
    core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/packed/

LMDB policy and integration belongs under:

    core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/
    core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/

Likely integration points include `PackedQueryCodec`, `PackedPlanRecipe`, `PackedDecisionCertificate`, `PackedCostingReplay`, `PackedMemo`, `PackedIncumbentSearch`, `PackedCascadesPlanner`, `LmdbQueryOptimizerPipeline`, `LmdbCascadesOptimizer`, `LmdbPipelinePlanCache`, `LmdbSailStore`, `LmdbFrontierPlannerSettings`, `LmdbRuntimeFeedbackDescriptor`, `LmdbOperatorFeedbackStats`, and existing configuration/explanation/JFR classes. Reinspect each file and its local diff immediately before modification.

## Outcomes & Retrospective

Implementation is in progress. On completion, summarize the final cache ownership model, exact and family hot paths, semantic/quality behavior, resume semantics, feedback/canary safety, retained-memory accounting, files changed, focused and broad verification, benchmark/profile evidence, remaining limitations, and any deviations from this plan. If a milestone remains incomplete, state it explicitly rather than implying the full design shipped.

## Revision note

2026-08-24: Initial ExecPlan created from the approved architecture. Recorded actual repository state and the successful required quick-install baseline. No production source had been changed for this feature at this point.

2026-08-24: Completed the packed parameter-family foundation. The initial focused test failed because a second IRI invocation missed the recipe, then passed after structural parameterization. Added direct schema/detachment tests and broadened `PackedPlanCacheTest`; 3 and 40 tests respectively are green. Full module verification remains pending.

2026-08-24: Added the immutable LMDB decision-cache owner and routed the existing pipeline facade through it. The initial committed-data-revision test failed because the old key treated the revision as semantic identity; after separating evidence from hard semantics, the same test passes as `USE_AND_REFRESH`. `LmdbPlanDecisionCacheTest` is green with 6 tests and `LmdbPipelinePlanCacheTest` is green with 9 tests. The packed portfolio/checkpoint still needs to move beneath this owner before the transitional facade can be removed.

2026-08-24: Added `PackedOptimizationDecision` and cache-entry portfolio ownership. Its test progressed from a missing API, through a one-candidate failure, to green with a champion and bounded distinct challengers; the complete `PackedPlanCacheTest` is green with 41 tests. Added bounded LMDB variant publication, conservative merge, and robust-fallback refresh semantics; `LmdbPlanDecisionCacheTest` is green with 7 tests. Recost/promotion and detached memo checkpointing remain incomplete.

2026-08-24: Added configuration round-trip/range coverage for maximum variants, refresh threads, and canary fraction, plus a priority refresh executor whose focused scheduler test proves per-variant single flight and shutdown. A proposed pipeline-facade refresh test correctly failed because no callback was submitted, but inspection showed that submitting the existing callback would retain connection-owned planning objects. Recorded the safe dependency: detached packed checkpoint/resume must precede automatic facade refresh.

2026-08-24: Added an immutable detached `PackedSearchCheckpoint` foundation and package-private re-entry against the current authenticated invocation. The API tests first failed on absent checkpoint/resume methods, then passed; the full `PackedPlanCacheTest` is green with 43 tests. Re-entry opens a fresh query-local memo and records resumed/cumulative deterministic work, so it is a safe lifecycle boundary but not yet memo/frontier reuse. The remaining milestone must detach and restore the structural/search arrays plus phase cursors before LMDB refresh may call it.

2026-08-25: Profiled the reported MEDICAL_RECORDS Q0/Q1/Q3 slowdown and found repeated optimization after runtime feedback, not slower execution of the reported unchanged plans. Fixed two general ownership violations: the transitional facade no longer rejects decisions against a per-routing-segment fraction of the aggregate byte budget, and monitoring-only lifecycle residuals no longer become hard plan blocks. The unchanged focused regressions are green, as are complete `LmdbPipelinePlanCacheTest` (12), `LmdbRuntimeFeedbackTargetTest` (12), and `LmdbPlanDecisionCacheTest` (15) selections. Short same-settings spot checks measured Q3 at 86.619 milliseconds per operation and Q0 at 35.572; Q1 measured 118.606 only with the benchmark's existing plan guard explicitly disabled, so it is not claimed as a direct baseline comparison. Full module/integration and publication-grade benchmark repetitions remain pending.

2026-08-25: Extended checkpoint re-entry through provider-backed costing without retaining a provider or session. The focused test first proved the old path restarted all eleven provider transitions. The new path replays detached evidence into the current private session, authenticates unchanged event digests, rebuilds the detached incumbent with live evidence metadata, and resumes exact enumeration; recost work is separate but cumulative. The unchanged focused test and complete 44-test packed cache class passed, and a changed-evidence variant separately proves conservative reopen matches the uninterrupted current-evidence plan and cost. This layer still supports only property-agnostic/zero-context winner DAGs; interner definition detachment, attributable pruning records, explicit open worklists, and mid-phase cursors remain incomplete.

2026-08-25: Broad evaluation verification exposed a portfolio-construction timeout on the 66-factor subset-kernel proof. Root challengers were each rebuilding a full memo-wide decision certificate. Challenger detachment now keeps only its executable DAG and reachable costing trace while the champion owns the family certificate. The unchanged focused proof, complete 40-test subset-kernel class, and complete 46-test packed cache class pass. The earlier full evaluation-module run remains classified red because it contains the pre-fix timeout and has not yet been rerun to completion.

2026-08-25: Completed the requested cache-hit explanation fields for search completion, bound kind, evidence epoch, and stability-envelope result by propagating the selected immutable variant metadata through `LookupDecision`. The focused test first failed on a null search-completion value and then passed unchanged; the committed-data drift case reports an outside envelope, all 12 pipeline-cache tests pass, and the post-change LMDB cache/feedback group is green with 52 tests.

2026-08-25: Detached checkpoint resume now preserves the complete optimizer-created logical overlay, including helper groups that are not directly named by the selected winner DAG. The focused FILTER/Extension/JOIN test first showed resumed work equaled a rebuild, then exposed the missing source expression and finally the missing memo group topology. The primitive checkpoint now authenticates and replays logical expressions in insertion order, rebuilds the incumbent in a private fresh memo, and discards that memo before ordinary planning if corruption is found. The unchanged focused test, the five-case scalar/provider/evidence/corruption group, and all 47 packed cache tests pass. Explicit open worklists, mid-phase loop cursors, and attributable cost-pruning records remain incomplete.

2026-08-25: The first post-checkpoint full evaluation-module run completed 1,444 tests with one deterministic-work regression: ordinary search reported 194 rather than 191 units because the restored-incumbent shortcut also affected non-resumed planning. Scoped that shortcut to checkpoint re-entry; the unchanged ordinary-search and logical-resume tests pass together, and the complete affected `PackedSearchTest` plus `PackedPlanCacheTest` selection is green with 139 tests. The full module has not been rerun after this final narrowing. The post-install LMDB owner/pipeline/feedback/budget/config selection is green with 58 tests.

2026-08-25: Rebuilt the current JMH jar and repeated the reported MEDICAL_RECORDS regression cell with two two-second warmups, three two-second measurements, and one fork. Normal-policy Q3 measured 99.588 milliseconds per operation and Q0 measured 29.851, restoring the user's same-plan references (~97 and 37) from the >1,000 millisecond regression. Q1 triggered the 512.1M-versus-100M Cartesian-work risk guard; an explicitly guard-disabled diagnostic measured 123.214 milliseconds per operation. The guard process exited before timed execution but did not terminate its JMH child, which later wrote a separate `q1.json`; that orphaned result is not accepted. The pre-fix Q3 JFR attributes 28.75 percent of execution samples to `FrontierStatisticsShard.BlockReader.mappedData` and 24.67 percent to `FrontierCenterIndex.matches`, confirming repeated Frontier planning rather than a changed physical execution plan.
