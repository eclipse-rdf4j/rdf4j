# Review notes (working)

## Merge decisions
- BindingSetAssignment: HEAD structure (no-rows => no assurance; null rows revoke; declared names bound schema) + develop getValue()!=null. Develop test explicitBindingNamesRemainAssured... inverted -> explicitBindingNamesAreNotAssuredUntilEveryRowBindsThem.
- FilterOptimizer MINUS: FINAL = always push into left arg (MINUS exports only left bindings, so equivalent), never clone into right; develop's containsAll guard dropped because VarNameCollector counts EXISTS-internal vars (broke LmdbOptimizerPipelineTest finite-membership-below-MINUS). HEAD double optimize dropped.
- SPARQLMinusIteration: develop comparison domain + HEAD bound-only fast check and fallback.
- MinusQueryEvaluationStep: HEAD adaptive/spill + develop comparisonBindingNames threaded through all paths (materialized, streaming, overflow index/compat).
- JoinQueryEvaluationStep: HEAD contract-aware hash join; develop's Difference branch dropped (isOutOfScope now includes containsDifferenceInCurrentScope -> contract hash join). IndependentJoinIteration kept (tests use it) but unused in prod.
- QueryJoinOptimizer: FINAL = develop verbatim (cost-first, connectivity tie-break). Tried porting HEAD's 'paths are run members' -> with cost-first it let `?p1 idShort "ratedPower"` jump ahead of its property-path bridge (LmdbEstimateAuditHarnessTest.defaultPipelineFiltersThresholdAtItsValueProducer red) -> reverted. Dropped HEAD tests: reorderJoinArgsNeverChoosesCartesianPairInsideConnectedComponent, reorderJoinArgsTreatsAllVariablePatternAsSegmentFence, reorderJoinArgsUsesPropertyPathAsBridgeBeforeDisconnectedPattern, reorderJoinArgsUsesZeroLengthPathAsBridgeBeforeDisconnectedPattern; adapted constant-edge test (equal cost, real constant vars).
- LmdbDistinctCursorSkipOverlayTest: CloseableIteration.EMPTY_STATEMENT_ITERATION -> IterationConstants (GH-6020).
- cp.txt / cp-full.txt still reference 6.1.0-SNAPSHOT jars.

## Candidate findings
1. BindingShapeAnalyzer.extensionShape (cascades) does not drop an overwritten extension target from `assured` before re-adding; HEAD HashJoinBindingContract uses it for lookup keys -> nullable key could be used as hash lookup (develop audit fixed same issue in Extension.getAssuredBindingNames). HashJoinIteration defends (partial-key indexes, partially bound probe scans all) so results stay correct (adversarial test passes) -> cost/plan-model issue; check other BindingShape consumers (packed planner) for correctness use of `assured`.
2. Policy: develop cost-first cartesian choice in QueryJoinOptimizer.reorderJoinArgs vs connectivity-first; disconnected all-var SP goes last -> repeated full scans under bind join. Only active when statistics.supportsJoinEstimation() (LMDB stats only), and LMDB doesn't use StandardQueryOptimizerPipeline -> low prod impact.
3. BindingSetAssignment equals/hashCode compare lazily-derived bindingNames (derived mode): equality changes after getBindingNames() is called (pre-existing).

## Overview: execution (agent) — top suspicious spots
1. DefaultEvaluationStrategy.java:612-633,1478-1492,2333-2335 shared feedback iterator per compiled node; overlapping evaluation -> ISE; stale close closes new eval; not thread-safe (LMDB cost-feedback only).
2. FilterIterator.java:353-369,442-475 + BindingAssignerOptimizer.java:47-53: per-row substitution replaces vars inside nested sub-SELECT scopes -> EXISTS result change when subselect reuses outer var name; FILTER in EXISTS body => clone+precompile per outer row (perf cliff), all stores.
3. FilterIterator.java:249-255,274-275 `minus-assured-shared` bypasses subquery scope boundary trusting planner hint (hints survive clone) -> stale hint => NOT EXISTS removes rows MINUS keeps.
4. MaterializedExistsFilterIteration.java:910-930 MaterializationCache no eviction; ADAPTIVE default for any store (FilterIterator:294-302).
5. HashJoinIteration.java:344-375 planned build side drained w/o limit/spill; :145-146 both inputs evaluated in ctor -> left leaks if right evaluate throws.
6. HashJoinBindingContract / HashJoinIteration:152-153,623-632 lookup==compat skips compat check; relies on BindingShapeAnalyzer (see extension overwrite finding).
7. LeftJoinQueryEvaluationStep.java:95-101,183-185 OPTIONAL with condition over sub-SELECT takes correlated path -> left bindings pushed into subquery (scope leak); MINUS on right handled for Join not LeftJoin.
8. FilterIteration.close synchronized + wrapped close under monitor; deadline task closes from other thread -> deadlock risk.
9. MinusQueryEvaluationStep static comparison names may miss custom nodes; spill path rescans 1M prefix + file per 4096 left block.
10. FilterIterator.java:167-224 resource-equality prebinding ignores scope; triple terms compare by value (1 vs 1.0) -> dropped rows; DelegatingSailDataset SP overload bypasses subclass filtering.
Observer effect: telemetry/cost feedback disables guard joins + fused VALUES join (JoinQueryEvaluationStep:52-53,90-124; FilterIterator:491-498) -> learned feedback measured on different operator.
LeftJoin hash hint silently ignored with condition/complex right.

## Overview: cascades/cost/LEO (agent) — top suspicious spots
1. LeoOperatorKey fingerprint collisions: >=7 factors / nested non-scope joins fingerprinted per factor w/ fresh Canonicalizer (var linkage lost) LeoOperatorKey.java:252-258,266; unary ops fingerprinted w/o params (BIND exprs, GROUP, projection, LIMIT, ORDER) :242-245; VALUES keyed by 32-bit hash :371,377. Store-wide learned corrections shared across different operators.
2. LeoConfidenceModel: EWMA alpha 0.6 linear ratio space [1e-6,1e5] :23,:91-94; non-finite/negative inputs recorded as perfect sample raising confidence :190-205; est<1 & actual 0 -> ratio 1e-6 :197; rowQErrorMax never decays :99,:186; decay stops if epoch regresses :167.
3. ScalarEvaluationEffects in standard pipeline: GeoSPARQL funcs (other jar) -> UNKNOWN -> no filter pushdown (perf regression Memory/Native); null CodeSource -> all UNKNOWN; static FunctionRegistry not strategy's :123; unaudited fn w/ constant args QUERY_STABLE :133.
4. QueryModelNormalizerOptimizer:100-164 VALUES pushdown through scoped BIND: isPlainJoinAlgebra accepts non-scoped Filters, NOT EXISTS (Not(Exists)) passes -> nested filters see pushed vars bound (semantics change?) — no direct test. ALL STORES.
5. PatternKeys.java:35-36 NPE when predicate var + constant object; FilterSelectivityKeys.patternKeyFor passes null :70-76; static CONSTANT_OBJECT_KEYS unbounded.
6. JoinFactorCostModel.java:579 exact when confidence>=1.0; :741 uncertainty 0 when any q-error metric; EvidenceProfile.semiJoin estimated zero -> hard 0 :332; MINUS 0.5 floor :372.
7. CostVector.java:139-141 NaN/Inf/neg -> MAX_VALUE saturation, ties decide; EstimateVector.filter upper bound can exceed input :127.
8. LeoEvidence.bestOf ranks Kind before confidence :126-131; unknown/typo LEO profile -> correction ON (LeoRolloutProfile:78,91); profile flags dropped :41-47.
9. OptimizationGoal absolute nanoTime deadline in hashCode :128; negative nanoTime -> no deadline :60,:109; InputBindingContext keyed on raw double :117-125.
10. FrontierStateArena.java:1916 digest uses enum identity hashes (nondeterministic across JVMs); BindingProfile static caches :55,:240-256; FiniteDomainFact:155-159 / FiniteRelationEstimate:550-554 compare lang tags case-sensitively -> zero-overlap claim "x"@en vs "x"@EN.
Also: PhysicalProperties.java:100 distinctVars containment direction (distinct over superset does not imply subset), mirrored PackedPhysicalPropertyInterner:130; BindingShapeAnalyzer treats Service transparent (SERVICE SILENT over-claims assured); HashJoinBindingContract javadoc wrong (intersection); YAML rules orphaned.
Develop-originated: LmdbSailStore stdout print in IOException catch (develop line 796) kept.

## LMDB merge (agent) — integration risks
- MERGE-INDUCED: develop fixed reader pool (127 ordinary + 1 reserved, 30s wait). HEAD planner-time nested reads still use ordinary slots while query holds one: TripleStore.repeatedVariableCardinality (createReadTxn ~1810), exactCardinality/sampleTriplesStratified (doWith), LmdbFiniteJoinSurfaceEstimator:131,173,369, LmdbFilterSelectivityStats:685,777,1342, boundedDistinctCursorSkipCardinality. Under >=127 concurrent queries -> wait 30s -> IOException. Develop moved its nested estimates to doWithPriority.
- Planner cardinality source changed: HEAD planningCardinality used HEAD's exact page walk; now develop's bounded/sampled page estimator (GH-6006) + primary/secondary index combine; page estimator disabled -> 5.3.2 sampler (was exact count for cardinality()).
- HEAD per-thread page buffer reuse dropped (develop caches LmdbPage zero-copy).
- TxnManager: version bump only on deactivating an active reader; release() no longer marks closed when reclaimed by close() (native reader leak fix); Txn.txn zeroed on abort.
- ValueStore: seedCoreDatatypes moved after nextId init in own txn; ported extended lang-tag length decoding to off-heap decoder.
- TripleStore: exactCardinality restored (develop deleted) for LmdbStatementPatternCardinalitySource; single txnManager.close() after estimator close.

## Overview: LMDB integration (agent) — top suspicious spots (HEAD lines)
Pipeline: BindingAssigner, Constant, RegexAsString, LmdbValueLookup, Compare, ConjSplitter, Disjunctive, SameTerm, UnionScopeChange, QueryModelNormalizer, LmdbOptionalNormalForm, FilterOptimizer(null,false,false), LmdbBoundSimplifier, LmdbSetSemantics(skip SERIALIZABLE), LmdbFilterSimplifier, LmdbFilterHoist, LmdbCascades(packed), OrderLimit. LmdbPipelinePlanCache wraps.
1. LmdbBoundSimplifierOptimizer.java:61-80,104-110 BOUND(?x)->false when ?x not possible in filter arg; only top-level BindingSet incoming; descends into EXISTS -> FILTER EXISTS{... FILTER(BOUND(?x))} with outer ?x -> false; !BOUND in NOT EXISTS inverted. No test.
2. LmdbFilterSimplifierOptimizer.java:519-548 makeOptionalBindingMandatory uses right arg possible names, no barrier/scope/incoming check -> spurious rows. Repro: ?s :p ?o OPTIONAL{ SELECT ?s ?w { ?s :a ?x OPTIONAL{?x :b ?w} } } FILTER(?w = :c).
3. LmdbNullRejectingOptionalSupport.java:61-89 + FilterSimplifier:134-136,191: filters in EXISTS body with witness correlated from outer row rewritten to inner join.
4. TripleStore.java:3113-3114,3229,3234 guarantee cache + dataRevision published after mdb_txn_commit; createReadTxn no lock -> dataset sees new rows while planning uses old stronger guarantee -> filter-dropping anchor misses rows ("05"^^xsd:int).
5. LmdbSailStore.java:3244-3302 tracked read txns renewed per commit, epoch captured once per pipeline (LmdbQueryOptimizerPipeline:91-93) -> anchors that dropped filters at epoch N execute on N+1.
6. LmdbDetachedPlanRefresh.java:109-131 + LmdbCascadesOptimizer:136-148 refresh hard-codes distinctCursorSkipAllowed=true.
7. LmdbFilterHoistOptimizer.java:140-152 bindsNewConditionVar uses possible names; maybe-bound var hoisted changes Compare/EXISTS results.
8. LmdbEvaluationFilterServices:91-98 -> LmdbFilterSelectivityStats:368-385 filter pass counts from dirty-overlay / FROM-restricted queries go to shared persisted stats (cost poisoning).
9. LmdbFilterSimplifierOptimizer.java:1200-1233,1261-1300 value-equality filters replaced by term enumerations then dropped; relies on RdfTermDomain.classify; fragile default guarantee.orElse(CANONICAL_INTEGER) :1215.
10. model/LmdbBNode.java:109-118 marks initialized even when resolve fails; LmdbStore.java:508 unconditional System.out.println.

## LMDB addendum (agent) — stale caches / learned state (HEAD lines)
Correction item 4: weakening reaches LMDB table before commit; cache swapped under commit write lock (TripleStore:3107-3115,3229); race remains (createReadTxn/planning no lock) lower rank.
A. (checked) LmdbFilterSelectivityStats.java:125-129,655-669,738-753 complete probe caches not cleared by reset() :227-256; with sketchEstimatorEnabled=true identity = synopsis mutationVersion (LmdbSailStore:477-479) which changes only on background rebuild publish (LmdbQuadSynopsisService:157-181,328-371) -> stale term set -> VALUES anchor (LmdbEstimatorRuntime:174-188 -> FilterSimplifier:376-383) -> rows committed later lost. Default identity statementMutationStamp (LmdbSailStore:696-704).
B. snapshotVersion()=max(dataRevision, persisted synopsis version) (LmdbEstimatorRuntime:406-410); dataRevision restarts at 0 each JVM start (TripleStore:220) -> commits may stop invalidating packed plan cache (LmdbCascadesOptimizer:359), finite-surface cache (:371-375), pipeline evidence; LEO dataEpoch same value.
C. packed plan cache guarantee version (LmdbEstimatorStorageAccess:246-256) hashes encoding+config, not contents.
D. pipeline/decision cache semantic deps omit data revision (LmdbPipelinePlanCache:366-380); UseAndRefresh serves old plan (LmdbPlanDecisionCache:1659-1700); cached plans include stored-term VALUES anchors; markEvidenceChanged no prod caller.
E. uncommitted changeset queries publish LEO (LmdbPackedCostModel:156-158); opens==1 exact fact stamped with committed stamp (LmdbOperatorFeedbackStats:509-512) served as DATABASE_EXACT (LmdbMappedFrontierLearningSession:180-187); persisted, rollback doesn't remove.
F. typed filter-learning path skips reinvocable check (LmdbEvaluationFilterServices:91-93 vs 108-112).
G. certifyRootRows zero => exact w/o completeness (LmdbPackedCostModel:257-260; LmdbEstimationEngine:287).
H. LmdbPlanDecisionCache.close ignores awaitTermination (:1601-1607); recordStoreMutation throws after durable commit on stamp regress -> rollback() on committed txn (LmdbSailStore:2357-2362); sidecars not fsynced / non-atomic fallback / corrupt dropped silently; single observation BLOCKED/QUARANTINED never evicted (PlanLifecycleStore:251-265,397).
I. SHADOW frontier mode changes planning (LmdbStorageEstimatorEvidence:153,162,269,317-319).
Other: exact probes/finite surfaces open fresh committed read txn newer than query snapshot (TripleStore:1924; LmdbFilterSelectivityStats:685,777; LmdbFiniteJoinSurfaceEstimator:131).

## Overview: LMDB estimation (agent) — top suspicious spots (HEAD lines; $L=core/sail/lmdb/.../lmdb)
Frontier V2 default AUTHORITATIVE; sketch estimator off by default. Frontier evidence never DATABASE_EXACT -> zeros affect cost only (as traced).
1. FrontierMappedStatistics.java:500-524 AGMS upper not a bound: all lanes <=0 -> point 0, upper 1, confidence 0.86 -> huge predicate-only join capped at 1 row (Omni out of keys + no centers).
2. FrontierOmniIndex.java:246-259 accepts weak/zero forward bridge; never tries exact reverse; FrontierMappedStatistics:459-462,476-478 returns point 0.
3. LmdbStatisticsService.java:188-196,260,891-903 latestStoreSequence updated at LmdbSailStore:2331 after tripleStore.commit() at 2309 -> window where new snapshots get fallback NONE with stale exact heavy counts/zeros; feedback learns against stale.
4. LmdbSailStore.java:1244-1254 delta publish failures (IOException, MEMORY_PRESSURE; dim mismatch FrontierOmniDeltaBuilder:64-69; disk envelope FrontierStatisticsDeltaBuilder:257-259) RETRY forever, never escalate to rebuild -> permanently GENERATION_TOO_STALE.
5. LmdbSailStore:1128,1165 quiet-period reset per commit -> base rebuild starves under steady writes; firstUncoveredMutationMillis only cleared on full catch-up (LmdbStatisticsService:110-112,530-533) -> planner flips mapped/scalar cost models commit-to-commit (plan instability).
6. LmdbStatisticsService.publish:84-125 / FrontierStatisticsManifestStore.publish:58-88 never GC superseded manifests/shards -> unbounded disk growth.
7. LmdbQuadSynopsisService.java:362-372 + BoundedQuadSynopsis:93-105 add between staleness check & publish dropped; rebuildRequested cleared -> CM upper=0 certain-zero while missing rows; continuous commits -> rebuild aborts forever after a delete.
8. BoundedQuadSynopsis.java:43-50 optimistic read runs probe on mutable arrays; PrimitiveBottomK.projectionSample:59-70 AIOOBE before validate().
9. LmdbQuadSynopsisService:229-245 rebuild marker only on persist; crash -> stale base loaded as current (LmdbSailStore:497-500).
10. FrontierOmniIndex:712-783,797-909,564-642 unbounded planning work (4096 keys/lane x 8192 rows/cell), recordCandidateRows not enforced; no memo; lock per call.
Lower: FrontierStatisticsGeneration:192-206 closes reused shard; FrontierSamplingQuality:53 all-zero lanes -> RSE 0 exact-looking [0,0,0]; FrontierMappedStatistics:288-300 Omni leaf not clamped by CM upper; FrontierMutationTail.append mutates published tail; two-pass build pins read txn (file growth).

## Overview: packed planner (agent) — top suspicious spots (dir .../optimizer/cascades/packed/)
PCP=PackedCascadesPlanner PLRP=PackedLogicalRuleProgram PJE=PackedJoinEnumerator PIS=PackedIncumbentSearch PPC=PackedPlanCache PBF=PackedBindingFacts
1. (V) PLRP:381-421 MINUS->NOT EXISTS ignores right-side referenced names; FilterIterator evaluateSubstitutedExists injects all left bindings. Repro: SELECT * { ?x :p ?n MINUS { ?x :q ?m FILTER(?n = ?m) } } -> MINUS keeps all rows, rewrite drops some. Derived EXISTS records only shared names (385-392).
2. (V) PPC:849 sameSource hit on snapshot.equals(candidate); fingerprint (~308-372) & TupleExpr.equals ignore ALP.minLength, GroupConcat separator, Service SILENT/baseURI/prefixes, scope-change & join flags -> cached PackedQuery materialized (PCP 199-201). Repro: ?s :p* ?o then ?s :p+ ?o same Context.
3. (V) PPC:568-575 Flight.matches admits family waiters without hasSameCacheIdentity -> waiter materializes owner's recipe with value-dependent rewrites (EmptySet, filter drop) for different constants.
4. (V code) PLRP:3616-3631 addScalarDependencies EXISTS depends only on subquery outputs, ignores referenced names -> unused-OPTIONAL pruning (3120-3130) / projection-below-ORDER drop bindings inner filter observes. Ex: SELECT DISTINCT ?s { ?s :p ?a OPTIONAL{?s :q ?b} FILTER EXISTS{?s :r ?c FILTER(?c=?b)} }.
5. (V) PackedFilterRules.java:90-92 relocatable filters pushed using POSSIBLE output masks; ex ?s :p ?y OPTIONAL{?s :q ?x} ?s :r ?x FILTER(?x=?y) loses rows; fallback when lattice stale/incomplete/>16 nodes (PIS:1215-1226) -> results depend on budget.
6. (R) PackedDomainFacts widen ORs languageBits (198); intersect kind bits 0 -> EMPTY (161-165) -> wrong filter drops/EmptySet; expandAnchorValue finite fallback (PLRP 2750-2762), code/type rule (706-755) lacks finiteDomainTermIdentitySafe.
7. (V) PPC:839-845 sameQueryContext ignores predicateRangeVersion; templates w/ range-proof alternatives offered to uncommitted-txn reads; templates share mutable PBF across threads (PackedQuery:87/104).
8. (V) failure asymmetry: LMDB falls back only on IllegalStateException; waiters join() without timeout -> CompletionException (PPC:543-544); non-finite costs -> IAE (PackedWinnerTable:718-722; PCP:739); UNSUPPORTED throws after full search (PackedPlanMaterializer:225-229); NaN -> target unreachable (PJE 4421).
9. (R) B&B assumes monotone costs (PJE 5034,6859) but objectiveScore unchecked (~9923), negative scalar costs accepted; unsaturated sums (PackedFilterRules 119,228-230); tryReserve latches workLimitReached (PackedSearchBudget 73-76); nanoTime <=0 no deadline.
10. resumeComputation swallows ISE incl. memo invariant (PCP:282); corrupt checkpoint restart reuses consumed budget (515-521); portfolio catches all (666); int 1<<ordinal masks with up to 64 factors (PJE 3616,11415).
Invariant notes: group facts derived from first member (PBF:236-282); PackedDomainFacts.put intersects all alternatives -> one unsound alternative poisons siblings; two assured impls differ on Extension (PLRP:3420 vs PBF:273).

## Verified so far (my probes)
- COLD START (pre-existing, robustness, HIGH): Frontier AUTHORITATIVE + NO_GENERATION => LmdbStorageEstimatorEvidence suppresses storage-summary/exact counts when statistics service present (lines ~153-175, 269) => all leaf estimates 1 row => optional-rhs-filter plan starts OPTIONAL body at ?a dct:language ENG (2244) per 7.8K left rows -> 17.6M rows, >60s (vs 94s whole class once READY). Same on pre-merge worktree. Generation builds ~4s after last commit (quiet period). QueryBenchmarkTest only awaits sketches (disabled) not Frontier -> order/timing-dependent stall in module run (seen once post-merge; test passes alone 16/16 in 94s).
- C12 LeftJoin cond over sub-SELECT: refuted for COUNT/GROUP BY leak shape (MemoryStore returns s1,c=2).
- C14 EXISTS body with sub-SELECT reusing outer var: MemoryStore returns [] (scoping semantics expects s1). Pre-existing (develop ProjectionQueryEvaluationStep passes parent bindings; branch substitution also descends). All stores.

- LmdbStatsLoggingTest (develop): filter to stats events (branch logs extra INFO/WARN lines).
- TripleStoreFrontierMutationJournalLifecycleTest: awaited frame LmdbUtil.transaction -> writeTransaction (develop rename).
- LMDB unit run #2: 2286 tests, 4 merge-induced failures -> all fixed (StatsLogging x2, OptimizerPipeline finite-MINUS, AuditHarness default pipeline).

## F5 execution review (agent, verified with repro tests in worktree review-f5)
CONFIRMED:
- REGRESSION vs develop: OPTIONAL over sub-SELECT with condition referencing left var -> LeftJoinQueryEvaluationStep:95 hash path only w/o condition -> correlated LeftJoinIterator -> left row leaks into sub-select (ProjectionQueryEvaluationStep:37 passes parent bindings). FILTER(?t != ?s): got {s=a,x=1} exp {s=a,t=c,x=1}; ORDER BY ?x LIMIT 1: got t=d exp none; COUNT no GROUP BY: n=1 exp 2. (develop ignored condition entirely - different bug.) Fix: out-of-scope predicate for LeftJoin regardless of condition; hash left join with residual condition.
- PRE-EXISTING: OPTIONAL { ?s :q ?y MINUS { ?y :r ?z } } -> {s=a,y=b,z=z1} exp {s=a,z=z1}; AdaptiveMinusIteration.initialize evaluates MINUS right with left row (MinusQueryEvaluationStep:254) (also develop); containsDifferenceInCurrentScope only for Join.
- PRE-EXISTING (all stores): EXISTS over sub-SELECT leaks outer bindings (ProjectionIterator:69, GroupIterator:299); branch BindingAssigner VarVisitor rewrites Vars inside nested Projection too. Also EXISTS{SELECT (COUNT..) HAVING}. Fix: scoped substitution stop at Projection.isSubquery/MultiProjection; sub-select evaluated without outer bindings; join projected vars after.
- minus-assured-shared hint (FilterIterator:249-255): executor trusts hint; hinted anti-join with non-allowlisted body probes WITH substitution -> NOT EXISTS semantics != MINUS. Unit repro: MINUS { ?s :q ?v FILTER(?v = ?o) } Difference {o=o1,s=a} vs hinted []. Planner PackedLogicalRuleProgram.safeCorrelatedMinusProbe (:604-627) accepts FILTER/EXTENSION/PROJECTION/LEFT_JOIN/GROUP bodies. LMDB end-to-end chose Difference (not reproduced e2e). Fix: never substitute in this mode; planner rejects bodies referencing out-of-scope vars.
- HashJoinIteration ctor leaks left if right evaluate throws (:145-146) (test hashJoinClosesLeftWhenRightEvaluationFails).
- Unbounded hash build side (:369-399) no limit/spill (code read).
REFUTED: extension overwrite/SERVICE SILENT over-claim -> no wrong rows (partial-key indexes); resource-equality prebinding (TripleTerm not Resource; = is term eq for IRI/bnode); adaptive EXISTS correctness; FilterIteration.close deadlock (no lock inversion; race only).
PLAUSIBLE: MaterializationCache no eviction (full partition per key after 32 keys, full body scan per outer row); shared per-node feedback iterator fail-fast ISE.
Observer effect confirmed by code: PackedPlanMaterializer:811 enables cost-feedback on every contract node -> JoinQueryEvaluationStep:52-53 disables guard joins there; fused VALUES join not gated consistently.
Repro files: review-f5 worktree PhysicalOperatorReviewReproTest (evaluation), OperatorContractReviewReproTest (memory), LmdbMinusAntiJoinReviewReproTest (lmdb); evidence scratchpad/review-f5-evidence/.

## F1 LMDB rewrite review (agent; LmdbLogicalRewriteReviewReproTest 37 tests, 19 fail; oracle = LmdbStore w/ DefaultEvaluationStrategyFactory)
CONFIRMED:
1. LmdbBoundSimplifierOptimizer:57,69,109 descends into EXISTS/LATERAL with only top-level incoming names: FILTER EXISTS{?s :q ?o FILTER(BOUND(?x))} -> [] exp s1; !BOUND in NOT EXISTS inverted; LATERAL same. (OPTIONAL nested-group case legit false.)
2. makeOptionalBindingMandatory (FilterSimplifier:519-546) possible-name check -> spurious w=:c row when RHS can leave ?w unbound (nested group / subselect barrier). Fix: require assured + not correlated.
3. Null-rejecting OPTIONAL->INNER in EXISTS/NOT EXISTS/LATERAL bodies with correlated witness (FilterSimplifier:163,191; LmdbNullRejectingOptionalSupport:212-217): EXISTS -> [] exp s1; NOT EXISTS spurious row.
5a. sameTerm treated as value equality: SameTerm accepted as anchor (:483), expandFiniteAnchor (:1215-1218) integer/boolean value expansion + filter dropped: sameTerm(?o,7) over "7"^^xsd:int returns row (exp none); boolean analog.
5b. Boolean lexical variants " true "^^xsd:boolean: FILTER(?f = true) -> [] (RDF4J = returns s1); canMaterializeObjectFilterAnchor (:1358) only datatype; no CANONICAL_BOOLEAN fact.
5c. MOST SEVERE: stale plan-cache reuse after commit: ?s :v ?o FILTER(?o = 7) over "7"^^int, commit :s2 :v "07"^^int, same query text -> only s1 (exp s1,s2); boolean + OPTIONAL guarantee variants too. LmdbPipelinePlanCache:373-396 treats dataRevision as cost evidence (Use/UseAndRefresh) not semantic dependency; explain pipelinePlanCacheHit=true with stale BSA[o="7"^^int].
5d. Lang tag case: FILTER(?o = "abc"@EN) over "abc"@en after reopen (cold cache) -> [] ; lang literals "safe" anchors (LmdbJoinPlanSupport.isSafeValuesAnchorValue) + ValueStore.getId byte-exact on disk but valueIDCache case-insensitive (:1996) -> nondeterministic across restarts; plain SP form fails cold in both engines (storage bug + standard CompareOptimizer/SameTermFilterOptimizer).
Standard pipeline (all stores): FilterOptimizer.FilterRelocator.meet(Join) (:349) pushes into LeftJoin using possible names -> { ?b :y ?w OPTIONAL{?b :z ?v} ?a :x ?v FILTER(?v = ?w) } returns [] exp 1 row (oracle + LMDB). SameTermFilterOptimizer:83 EXISTS blind spot: FILTER EXISTS{... FILTER(sameTerm(?x,:v1))} with outer ?x -> EmptySet (all stores).
REFUTED: FilterHoist; SetSemantics; OptionalNormalForm.
Files: review-f1 worktree core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbLogicalRewriteReviewReproTest.java; initial-evidence.review-f1.txt

## F2 packed rules review (agent; LMDB 31 tests/8 fail; unit 13/6 fail; oracle = unoptimized algebra via StrictEvaluationStrategy)
CONFIRMED e2e:
1. MINUS->NOT EXISTS (PLRP:381-421 guard 384-389; existsScalar 393 records only shared names; PackedPlanMaterializer:560 drops names; FilterIterator substitutes all outer bindings): SELECT ?x ?n { VALUES ?x {:a :b} ?x :p ?n MINUS { ?x :q ?m FILTER(?n = ?m) } } + 3000 :q triples -> exp [a/1, b/2] got [b/2]. Cost-dependent. Fix: refuse unless fv(R) ∩ pv(L) ⊆ shared (codec already computes referenced set PackedQueryCodec:1245).
2. addScalarDependencies (PLRP:3621-3629) uses EXISTS outputs not referenced names -> unused-OPTIONAL pruning (3066/3127) drops ?b: SELECT DISTINCT ?s { ?s :p ?v OPTIONAL{?s :q ?b} FILTER EXISTS{?s :r ?c FILTER(?c=?b)} } exp [a] got []; NOT EXISTS [a,b,c] exp [b,c]; COUNT(DISTINCT) 0 exp 1; projection-below-ORDER (1868) ORDER BY DESC(EXISTS{...?b...}) LIMIT 1 exp [b] got [a].
3. (standard FilterOptimizer, pre-existing upstream) FilterRelocator.meet(Join) :349-365 uses possible names -> pushes onto LeftJoin; LmdbNullRejectingOptionalSupport then OPTIONAL->INNER: ?s :p ?y OPTIONAL{?s :q ?x} ?s :r ?x FILTER(?x=?y) loses rows (3 and 21 factor). PackedFilterRules:90-92 same pattern PLAUSIBLE (not triggered).
4a. PackedDomainFacts.widen:198 ORs language bits; VALUES-derived facts (PackedQueryCodec.seedBindingSetAssignmentFact:537-567) language 0 = unknown -> langMatches(lang(?o),"*") over UNION{VALUES "a"@en}{?s :p ?o} -> EmptySet (packed-predicate-range-empty) exp ["a"@en].
4c. (planner level) expandAnchorValue finite fallback PLRP:2750-2762 stored.equals(value): {1,"01"^^int,1.0,2} FILTER(?o=1) -> VALUES{1} [s1] exp [s1,s2,s3]; not e2e (LMDB supplies no finite store domains).
4d. (planner level) code/type rule PLRP:706-755 without term identity check (needs cleared UNION scope flag; not e2e).
5. (planner level) PackedDomainFacts.put:65-70 intersects facts over all group members -> unsound alternative poisons siblings -> EmptySet via addEmptyDomainJoinAlternative (PLRP:2049).
REFUTED: domain intersect kind 0 (fragile), lang tag case, duplicate sensitivity of pruning, uncommitted overlays (note LmdbCascadesOptimizer:129-133 provider enabled when tripleSource==null; detached refresh ctor :147 always enables).
Files: review-f2 worktree LmdbPackedRuleSemanticsReviewReproTest (lmdb), PackedRuleSemanticsReviewReproTest (evaluation/cascades/packed), initial-evidence.review-f2.txt

## LMDB full suite after fixes
- Run 3: unit 2286 pass (7 skipped), IT 144 pass (98 skipped), 0 failures. BUILD FAILURE only from japicmp: LmdbStoreConfig get/setSketchEstimator{Subject,Predicate,Object,Context}BucketCount + ContextPairSketchesEnabled removed in branch commit d13767b49c ("code cleanup", pre-merge) — present in 6.0.0 baseline and develop -> binary-incompatible minor release. Fix: restore as @Deprecated no-ops.

## F6 estimation review (agent; worktree review-f6)
CONFIRMED:
- Cold start unit-level: AUTHORITATIVE + no generation -> leafCandidates [] & exactCandidate empty while storage has 250k rows (LmdbStorageEstimatorEvidence:162,:269 'statistics == null' gates) -> EstimateEvidenceResolver:41-42 heuristic(1.0); LmdbEstimatorStorageAccess:100-104 NaN in AUTHORITATIVE, never legacyPackedRows.
- C2 bridge zero: hub dataset true 160,000; forward point 0 (upper 8e8, conf 0) reverse 164,860 (FrontierOmniIndex:254-259; FrontierMappedStatistics:459-462,476-478; LmdbPackedCostModel:1444-1451,748-749 cost with pointRows regardless of confidence). Reduced witness ceiling 256 (prod 8192 needs ~640k rows).
- C4 SEVERE: delta publication failures RETRY forever: 16MiB budget stalls on 2nd delta; DEFAULT 512MiB budget: insert-only commits of 16k statements -> stalled permanently after 112,000 statements (~300 B/mutation). Then mappedStatisticsReady false forever (scalar model), after 16,384 uncovered mutations leaf estimates -> 1 row; restart doesn't recover. LmdbSailStore:1254-1263 RETRY mapping, :1169-1172 returns before base rebuild; FrontierStatisticsDeltaBuilder:204,247-261; dimension mismatch on -Xmx change (FrontierOmniDeltaBuilder:64-69) by code reading.
- C5 starvation: 20k-statement txn then commit every 250ms: covered sequence stays 0 for 12s, leaf estimate 1 row (frontier-v2-unavailable). requestFrontierRebuild resets quiet period (LmdbSailStore:2364, :1436-1439, :1174). Mapped/scalar flip per commit PLAUSIBLE.
- C6 manifests/shards never deleted: 12 commits -> 13 manifests, 150 shards, 253KB vs 79KB live.
- C8 unbounded planning work: one 2-pattern estimateJoin 123ms (accurate); recordCandidateRows not enforced.
- C10 SHADOW changes planning: OFF [STORAGE_SUMMARY:250000]+exact; SHADOW [] no exact.
PLAUSIBLE: C1 AGMS upper not bound (not reachable w/ BOTH_PLANES probes); C3 recordStoreCommit after commit window (LmdbSailStore:2334 vs :2361); C7 quad synopsis races (off by default); C9b Omni leaf not clamped by CM upper; C9c tail mutated in place; C9d validateShard closes live shard.
REFUTED: C9a RSE 0 fake exact.
Design recs: unknown never collapses to 1 (fallback chain Frontier -> B-tree -> bounded exact -> pessimistic ceiling); LpBound/Cai degree-norm bounds replacing productBound; max-frequency bounds (Hertzschuch CIDR'21); bidirectional join sampling (Wander Join, index-based sampling); quantile costing point^c*upper^(1-c) (Babcock&Chaudhuri); staleness-aware deltas w/ insertion-debt trigger + escalation + GC; audit lanes (auditLeafQError/auditJoinQError always NaN).
Files: FrontierEstimationReviewReproTest (frontier pkg), LmdbStorageEvidenceReviewReproTest, LmdbFrontierLifecycleReviewReproTest; initial-evidence.review-f6.txt

## F3 plan-cache review (agent; 18 tests / 10 fail; worktree review-f3)
CONFIRMED e2e:
1. PackedPlanCache sameSource (:849) + fingerprint ignoring operator params (:308-372): cached p* plan served for p+ -> zero-length rows [a|a, a|b, b|b] exp [a|b]; GROUP_CONCAT separator ',' served for '|'. Root: ArbitraryLengthPath.equals omits minLength (model :231-236), GroupConcat.equals omits separator (:36-38). Fix: authenticate with full PackedQueryCacheIdentity.createPlanIdentity; fix equals/hashCode.
5. Pipeline decision cache stale data-dependent plans: (a) exact stored-term anchor FILTER(?o=5) then commit c p "5"^^xsd:int -> [a] exp [a,c] (USE_AND_REFRESH, BSA [o=5 integer]); (b) detached refresh keeps it permanently (USE); (c) spoc-only guarantee-based filter drop same. Root: LmdbPlanDecisionCache:1687-1698 UseAndRefresh on data drift; LmdbPipelinePlanCache:246-254 drops predicate-range identity dep unless packed summary declares PREDICATE_RANGE_DATA — FilterSimplifier rewrites (:376-383, :389-396, :1170-1233) invisible; refresh replans from detachedPreparedSource cloned AFTER request-bound rewrites (LmdbCascadesOptimizer:174, LmdbQueryOptimizerPipeline:129-139); markEvidenceChanged no prod caller.
6. snapshotVersion after restart (sketch on): version stuck 4->4 after commit; packed plan cache serves EmptySet: committed :c :p 7 not visible. LmdbEstimatorRuntime:406-410 max(dataRevision(AtomicLong from 0, TripleStore:229), persisted synopsis version).
7. Exact-probe cache keyed on sketch mutationVersion (sketch on): [a] exp [c,a] (LmdbFilterSelectivityStats:127-129,742-752; LmdbSailStore:482-484; reset() doesn't clear).
4. predicateRangeVersion hashes encoding/config only (LmdbEstimatorStorageAccess:246-256; TripleStore.effectiveRdfTermDomainsVersion :434-448) -> drives 5c.
CONFIRMED planner-level:
2. Flight.matches (:568-575) admits family waiters w/o hasSameCacheIdentity: waiter FILTER(?o=7) materialized owner's EmptySet proof for FILTER(?o=9). E2E 0/300 (small window).
3. sameQueryContext (:839-845, used PackedCascadesPlanner:226-233) ignores predicateRangeVersion: providerless (dirty txn) planning gets templateHit with EmptySet.
REFUTED: epoch capture/uncommitted overlay as tested; detached refresh distinctCursorSkipAllowed. PLAUSIBLE: guarantee cache not versioned with reader snapshot (TripleStore:851-866, :717-719).
Files: LmdbPlanCacheUpdateReviewReproTest (lmdb, 13/7 fail), PackedPlanCacheFlightReviewReproTest (evaluation packed, 5/3 fail); initial-evidence.review-f3.txt

## Compliance (compliance/sparql): merged 2648 tests, 11 failures, 43 errors == pre-merge worktree IDENTICAL 54 failing ids -> no merge regressions.
Pre-existing LMDB causes: 32x LmdbPipelinePlanCache$DatasetIdentity.immutable:502 Set.copyOf NPE when dataset graphs contain null (SPARQL UPDATE with default graph) -> LmdbPARQL11UpdateComplianceTest 32 errors; 8x LmdbPipelinePlanCache$2.meet:430 List.copyOf(values) NPE for VALUES with UNDEF (any LMDB query with UNDEF!); 1x PatternKeys.predicateKey:36 NPE predicate null (property path). Others: lateral()[2] expected 3 was 1 (all stores incl memory/native), SPARQL12 tests()[6] LATERAL+OPTIONAL (all stores), builtinFunction()[20] lmdb, CustomAggregateFunctionEvaluationTest *WithDistinct (4).

## F7 cost/search/LEO review (agent; 25 repros, 23 fail)
CONFIRMED:
1. CostVector.java:150-152,85 NaN/Inf/neg -> MAX_VALUE; plus() absorbs; 1e12-row plan beats 10-row plan on seeks tie-break. EstimateVector.filter:127 upper 400 -> 800.
2. Plan depends on query text order: 24 permutations of 4-pattern star -> 24 plans / 12 join shapes when costs saturate (PJE saturatedAdd :11651-11654, ties by enumeration order); even finite 4 orientation variants. PackedSearchBudget:73-76 latch: one refused optional block -> workLimitReached with 990/1000 left, blocks mandatory completion. int 1<<ordinal masks (:3612-3619, factorHypergraph :11406-11413) 1<<33==2 spurious edges for 33-64 factor sparse regions (PLAUSIBLE/certain by Java semantics).
3. Single-flight waiter gets CompletionException (PackedPlanCache:544, PCP:216) -> LmdbCascadesOptimizer:176-186 catches only ISE -> waiter's QUERY FAILS; join no timeout. UNSUPPORTED throws only after full search, not fallback-able (FedX types only).
4. JoinFactorCostModel:741-743 q-error present -> uncertainty 0 (q-error 100 -> [1000,1000]); EvidenceProfile.semiJoin:332-334 5%-confidence zero -> rows 0 conf 0.9; MINUS 0.5 floor asymmetric; explain shows 'Filter rows=100 source=uniform-filter guarantee=database_exact' (PackedPlanMaterializer:879-896 immutable to learning).
5. QueryJoinOptimizer cost-first: connected 5-chain with cheap endpoints -> cartesian orders (p1 X p5 . p2 . p3 . p4); packed planner none. (QJO :1700-1702, :706-710.)
6. LeoOperatorKey collisions all four: 7-factor chain == triangle x chain cartesian; FILTER(?a=?b) vs (?a=?c) collide (:222-228); DISTINCT ?s vs ?s ?o (:242-245; LmdbOperatorFeedbackStats OperatorKey drops outputMask :2998-3003); VALUES 41 rows "Aa" vs "BB" 32-bit hash collision (:367-379).
7. LeoConfidenceModel all five: NaN = perfect sample (100 -> 2.01, count 1->6); alternating 100x under/over -> oscillates 71.4/28.6; empty result -> correction 1e-6 (50 -> 5e-5); one outlier + 50 accurate -> confidence 0 (rowQErrorMax never decays); epoch regression -> no decay.
8. LeoEvidence.bestOf 0-conf sketch beats 200-sample 0.95 feedback (latent: no prod caller); LeoRolloutProfile 'disabled'/'false'/'none'/'off'/'0' ENABLES correction (:78,91); ladder non-monotone.
9. ROLLBACK LEAK e2e: commit 200 :p; txn add 500, query, rollback; new connection plans rows=700 source=frontier-v2-exact-fact guarantee=database_exact (baseline 200). LmdbCascadesOptimizer:321, LmdbPackedCostModel:156-158 openCurrentView; stamped committed (LmdbOperatorFeedbackStats:500-503,1307-1315); served DATABASE_EXACT (LmdbMappedFrontierLearningSession:175-187).
PLAUSIBLE: every commit halves frontier model +2048 epoch (:636-686) -> learning useless under trickle writes; post-commit hook throw -> rollback() after durable commit (LmdbSailStore:2388-2393); typed filter path lacks filterOutcomeRecordable; lifecycle BLOCK from 1 sample (low); sidecars no fsync (fail-safe); observer effect.
REFUTED: negative provider rows on scalar path.
Files: review-f7 worktree LeoLearningReviewReproTest, CostModelInvariantReviewReproTest, PackedPlanningFailSafeReviewReproTest, QueryJoinCrossProductReviewReproTest (evaluation), LmdbRolledBackFeedbackReviewReproTest (lmdb); initial-evidence.review-f7.txt

## F8 (agent): normalizer VALUES push through scoped BIND REG confirmed; FilterRelocator possible-names PRE confirmed; ScalarEvaluationEffects GeoSPARQL pushdown REG confirmed; SameTerm EXISTS PRE confirmed; reader pool starvation confirmed (2 slots per query); sampled finite probes labelled DATABASE_EXACT (LmdbPackedCostModel:386-388). Files: repro/f8.
