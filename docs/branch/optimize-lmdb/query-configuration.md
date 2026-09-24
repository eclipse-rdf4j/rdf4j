# LMDB query configuration reference

This page describes query controls in the pinned LMDB source. A property is a gate at a particular compile, planning, proposal, bind/open, or memory-admission point; it is not a promise that the named route can serve every shape. Use [routing and hosts](query-routing-and-hosts.md), [strategy families](query-strategy-families.md), [physical access](query-physical-access.md), [joins and factors](query-joins-and-factors.md), [paths](query-paths-and-special-operators.md), [code generation](query-ir-and-codegen.md), and [cost adaptation](query-cost-learning-and-adaptation.md) to understand the surrounding exact fallback. The branch delta catalogued as Q14 exposes query controls and records their consumers; the registry and per-feature property reads are not a promise that every setting is new, dynamically sampled, or consistent across all call sites.

## Runtime-property registry

[LmdbRuntimeProperties](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRuntimeProperties.java) is an allowlist of boolean query controls that the runtime UI/API considers safe to switch between queries. list() returns the current effective snapshot; set(name, enabled) accepts only listed keys, writes canonical true or false into the JVM system properties, and returns the new registry state. Unknown names throw IllegalArgumentException. The registry's defaultEnabled is metadata from the registry entry; it does not make all direct property consumers share one parser or sampling time.

For entries using Java Boolean.parseBoolean, only a PRESENT case-insensitive string equal to true is true; any other present value is false. When the property is absent, the registry entry uses its declared default, while a direct consumer uses its call-site default (which can disagree with the registry, as the wildcard-predicate row shows). `unlessFalse` entries are enabled by default and become false only for exact lowercase false; `unlessFalseIgnoreCase` entries become false for any case spelling of false. These parser distinctions matter when setting properties outside LmdbRuntimeProperties.set.

The entries are grouped below by the registry parser/default in LmdbRuntimeProperties. In each suffix list, prepend rdf4j.lmdb. to get the full property name. The separately called-out wildcardPredicates row records a current registry/consumer default mismatch rather than choosing one value silently.

| Registry default and parser | Property suffixes |
|---|---|
| On; Java boolean parser | irAggregate.having.enabled, irAggregateParallel.enabled, irKernelParallel.enabled, janinoCodegen.aggResidual, janinoCodegen.bindHooks, janinoCodegen.contextColumns, janinoCodegen.distinctNumericAggregates, janinoCodegen.enabled, janinoCodegen.hashJoin, janinoCodegen.mixedBinding, janinoCodegen.nodePredicates, janinoCodegen.outputMods, janinoCodegen.rowExists, janinoCodegen.scanSources, janinoCodegen.unionSources, janinoCodegen.wcoj, costCalibration.record, adaptiveFilterPlacement.enabled, kernelDistinct.enabled, kernelInterpreter.enabled, kernelInterpreter.warmup, nativeSpecialization.enabled, directAdjacency.boundProbe.enabled, directAdjacency.cleanTxnReads.enabled, directAdjacency.exactEmptyPruning.enabled, directAdjacency.nodePredicateProjection.serve.enabled, directAdjacency.parallelRowPath.enabled, directAdjacency.plannerStats.enabled, directAdjacency.rootScan.enabled, directAdjacency.scanAggregates.enabled, leftjoin.hash.enabled, leftjoin.memo.enabled, leftjoin.replay.enabled, mergeJoin.enabled, nativeHashJoin.bushyBuild.enabled, nativeHashJoin.enabled, parallel.enabled, parallel.rangePartition.enabled, nativePath.adjacencySeeds.enabled, nativePath.bidirectional.enabled, nativePath.memo.enabled, nativePath.targets.enabled, nativeBatch.enabled, nativeLazyResults.enabled, native.rangePushdown, nativeQueryEngine.enabled |
| On; exact lowercase false disables | factor.transport.enabled, factor.borrowed.enabled, janinoCodegen.distinctRootExists, janinoCodegen.probeCloseSeek, janinoCodegen.resumable, janinoCodegen.vectorTail, bareFragments.enabled, chunkPipeline.enabled, chunkPipeline.externalRoot.experimental, chunkPipeline.merge.enabled, chunkPipeline.sip.enabled, factorizedReorder.enabled, factorizedRows.chunkedPrefix.enabled, factorizedRows.enabled, factorizedSink.enabled, factorizedTail.enabled, packedFtree.enabled, packedFtree.bulkRuns.enabled, packedFtree.parallel.enabled, packedFtree.structuredGroups.enabled, orderedFactorizedRows.enabled, islands.memo.enabled, prefixRun.enabled, skipScan.enabled, statementSeek.enabled, nativePath.enabled |
| On; case-insensitive false disables | computedValueResolutionCache.enabled, fragments.enabled, costCalibration.enabled, adaptiveHedge.enabled, adaptiveHedge.normalGuard.enabled, adaptiveProbe.enabled, costModel.persist.enabled, hotCounters, adjacencySemijoin.enabled, wcoj.directFrontiers.enabled, wcoj.enabled, generalPath.enabled, islands.enabled, rootPipeline.enabled, learnedFilterSelectivity.enabled, nativeExpressions.enabled, sip.adjacencyMasks.enabled |
| Off; Java boolean parser | janinoCodegen.planBridge, janinoCodegen.synchronous, directAdjacency.synopsis.enabled, adjacencySemijoin.trackProbes, leftjoin.sweep.enabled, markJoin.enabled, nativeAccumulateJoin.enabled, nativeHashJoin.byteAdmission.enabled, wcoj.streamingFrontiers.enabled |

### Known default disagreement: wildcard-predicate code generation

The registry sets janinoCodegen.wildcardPredicates to default-off, and [LmdbRuntimePropertiesTest.experimentalPerformanceTiersAreReportedDefaultOff](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRuntimePropertiesTest.java) asserts that registry value. The active query consumer [LmdbNativeKernelIr.wildcardPredicatesEnabled()](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelIr.java) calls Boolean.parseBoolean(System.getProperty(key, "true")), so with the property absent it returns true. The lowering creates projection-free wildcard IR regardless; the flag controls the Janino emitter, not whether the IR can be interpreted. With explicit false, AUTO can use the interpreter only when that tier is enabled; an explicitly compiled request can retry through exact scans only when `janinoCodegen.scanSources` is enabled and that shape is supported. Thus a bare LmdbRuntimeProperties.list() may display disabled while the direct Janino consumer allows the route. Explicit true allows Janino; explicit false disables its wildcard emitter; this documentation records the source disagreement, not a runtime resolution.

The registry default should not be treated as a universal statement about direct consumers. For example, nativeQueryEngine.enabled and nativeQueryEngine.forceRootGeneric are constructor-time strategy choices; NativeExecutionContext snapshots value-resolution-cache enablement for one evaluation; and adjacencySynopsisEnabled() samples the property at a query-access boundary. `hotCounters` is initialized once when `LmdbRuntimeProperties` loads, from the then-current system property. `LmdbRuntimeProperties.set` updates both the property and a volatile fast-path field, but a later direct `System.setProperty` does not update that field; `list()` can therefore report a value that differs from the active counter gate. Other consumers directly sample their property during compilation, candidate construction, open, or initialization. A change affects consumers that sample it at a later boundary; it does not re-plan every decision already made for an active query.

## Non-registry switches and read boundaries

These non-registry controls are intentionally not in the boolean allowlist. Their names and direct parsers are part of current source, but they do not receive the registry's normalized list/set contract.

| Property | Default and parser | Consumer boundary / effect |
|---|---|---|
| rdf4j.lmdb.nativeQueryEngine.forceRootGeneric | false; Boolean.getBoolean. | Captured by LmdbNativeEvaluationStrategy construction; later changes do not change an existing strategy. Selects the generic claim-suppressed root posture. |
| rdf4j.lmdb.nativeQueryEngine.forceInteriorIslands | false; Boolean.getBoolean. | Sampled by the aggregate planner. Forces interior generic island conversion where the compiler has a decline point. |
| rdf4j.lmdb.islands.forceReuseMode | Unset; exact symbolic values REEVALUATE_PER_INPUT or MEMOIZE_BY_KEY affect eligible generic island testing/diagnosis. | Sampled at generic-island open; illegal/noneligible behaviors remain guarded by tuple facts. Production defaults choose the semantics-driven mode. |
| rdf4j.lmdb.nativeLateral.enabled | On unless exact case-insensitive false. | Sampled by planner when considering native LATERAL; otherwise the scope-change subtree falls through to the island path. |
| rdf4j.lmdb.nativeTripleTerms.enabled | On unless exact case-insensitive false. | Sampled at native triple-term compilation/value codec use, in addition to source scan capability. |
| rdf4j.lmdb.nativeFunctions.enabled and nativeFunctions.{strings,dateParts,casts,hashes,rdfTerm}.enabled | Each defaults on; exact case-insensitive false opts out. | Function-registration/capability gate; added families are separately kill-switchable while the core expression set remains available. |
| rdf4j.lmdb.datePart.inlineFastPath.enabled | On unless exact case-insensitive false. | Per date-part registration/fast-path decision. |
| rdf4j.lmdb.computedIntern.rowPath.enabled | On unless exact case-insensitive false. | Captured by planner construction; permits row-path computed BIND values that need evaluation-local runtime IDs. |
| rdf4j.lmdb.customAggregates.native.enabled | On unless exact case-insensitive false. | Sampled while aggregate calls compile; unsupported registry/arity still declines to generic evaluation. |
| rdf4j.lmdb.factor.selection.enabled | On unless exact lowercase false. | Read at factor-row transport construction; controls whether selection state may travel with eligible factor sidecars. |
| rdf4j.lmdb.packedFtree.algebra.enabled | On unless exact lowercase false. | Sampled by structural factor-algebra admission; also requires packed f-tree and factor-row gates. |
| rdf4j.lmdb.generatedKeys.enabled | On unless exact case-insensitive false. | Read when a generated-key plan is activated; this specializes proven duplicate elimination, not arbitrary DISTINCT semantics. |
| rdf4j.lmdb.leftjoin.wellDesignedCheck | true; Java boolean parser. | Sampled at left-join/island planning to decide whether the conservative well-designedness guard is required. |
| rdf4j.lmdb.costCalibration.explore | false; Boolean.getBoolean. | Sampled by cost calibration; this is opt-in work-calibration exploration, separate from adaptive strategy probes. |
| rdf4j.lmdb.costModel.bestObserved | On unless exact case-insensitive false. | Sampled when a per-store latest-complete-observation ledger is created. |
| rdf4j.lmdb.costModel.trace | false; Boolean.getBoolean. | Debug trace gate; not an ordinary UI runtime property. |
| rdf4j.lmdb.janinoCodegen.debug | false; Boolean.getBoolean. | Debug diagnostics only, read at the relevant failure/report site. |
| rdf4j.lmdb.janinoCodegen.failOnError | false; Java boolean parser. | Read when code generation or instantiation fails. True turns those failures into validation failures; by default the failed shape remains cached and a caller may use its supported interpreter/decline route. Execution-time kernel errors still propagate; strict mode wraps non-control failures as validation errors. This is a strict-validation control, not a production eligibility promise. |
| rdf4j.lmdb.janinoCodegen.dumpDir | Unset; path string. | Read when generated source is dumped after compilation. If set, the codegen service creates the directory and writes generated `.java` diagnostics there; filesystem errors are logged and ignored. |
| rdf4j.lmdb.membership.impl | hash; only exact string off disables membership probing. | Read while an eligible pattern membership probe is constructed. |
| rdf4j.lmdb.wcoj.debugParallel | false; Boolean.getBoolean. | Debug-only parallel leapfrog diagnostics, sampled once when its parallel helper class initializes. |
| rdf4j.lmdb.factorizedRows.keysOnlyRoot.enabled | On unless exact lowercase false. | Sampled when factorized row execution considers the DISTINCT-only keys-root shortcut; absent means enabled, and capability checks still reject unsafe shapes. |
| rdf4j.lmdb.wildcardAdjacencyBatch.enabled | true; Java boolean parser. | Read when the wildcard-adjacency batch proposal is considered; false removes that candidate while preserving other registered routes. |
| rdf4j.lmdb.packedProjectionWindows.enabled | true; Java boolean parser. | Sampled when a packed projection cursor opens. It enables bounded windows for independently demanded packed projections; it does not alter the result semantics. |
| rdf4j.lmdb.janinoCodegen.boundedOrder, `.boundedGroups`, `.countSpecialization` | On unless exact lowercase false. | Sampled while the shape-only kernel IR is built. These gates select bounded top/order/group and small count-guard specializations when their shape predicates pass; disabling them leaves the corresponding general kernel path. |
| rdf4j.lmdb.janinoCodegen.factorPlans, `.factorWindows`, `.factorMarginals` | On unless exact lowercase false. | Sampled during IR/factor projection construction. These gate factor-plan lowering, prefix-local count guards, and reusable weighted factor projections; a setting cannot make an ineligible factor shape eligible. |
| rdf4j.lmdb.janinoCodegen.factorGuardPeeling | false; Boolean.getBoolean. | Opt-in aggregate lowering read when considering selected-factor filter peeling. It only applies to eligible count-only filtered aggregates and also requires factorPlans not be explicitly false. |
| rdf4j.lmdb.janinoCodegen.distinctDeadValueDemotion | true; Java boolean parser. | Sampled during lowering of eligible DISTINCT pipelines. It permits a keys-only enumeration when dead values cannot affect the observable result; false or malformed text disables this rewrite. |
| rdf4j.lmdb.janinoCodegen.weightedComputedGroups | On unless exact lowercase false. | Read during aggregate-lowering admission; permits a bounded weighted COUNT shortcut for supported computed grouped values, with structural checks retaining the ordinary aggregate route otherwise. |

## Numeric memory, batch, sort and compiler settings

Numeric settings are plain system properties, not part of the registry. KiB, MiB, and GiB below use powers of 1,024. Values labelled per query or rows are counts, not byte guarantees. System-property changes affect only future read points unless the row says the value is frozen/captured.

| Property | Default, units and validation | Read time and practical bound |
|---|---|---|
| rdf4j.lmdb.queryMemory.maxBytes | max(1 GiB, Runtime.maxMemory()/2) bytes; a positive configured value is honored, invalid/nonpositive falls back. | Frozen in the process singleton on its first initialization. Global ledger for reservations explicitly made by query operators; it is not a cap on all heap. |
| rdf4j.lmdb.queryMemory.perQueryMaxBytes | max(1, globalLimit/4) bytes, then clamped to the global limit; a positive configured value is honored, invalid/nonpositive falls back. | Same first-singleton initialization boundary. Per-query subledger inside the global bound. |
| rdf4j.lmdb.runtimeValues.maxBytes | max(16 MiB, min(512 MiB, maxMemory/8)) bytes; explicit value must parse as positive long. | Read when an evaluation first needs to construct its runtime-value budget. A runtime interner refusal is an evaluation error; values do not spill. Nested native roots share the query owner's budget. |
| rdf4j.lmdb.nativeBatch.rows | 16,384 rows; clamped to 1–65,536. nativeBatch.enabled defaults true. | Read when a batch is configured/created. Row arrays scale with slot width × capacity; row count is not a byte ceiling. |
| rdf4j.lmdb.nativeGroup.maxBytes | 64 MiB; values below 4,096 or malformed values throw IllegalArgumentException. | Read when a count-group store is created. It governs that operator's arena/spill-workspace reservation, not runtime Values or all query memory. |
| rdf4j.lmdb.nativeSort.maxBytes | 64 MiB; malformed values use default, configured values are floored at 16 bytes. | Read when a spill-sort store configures itself; the sorter spills bounded packed rows into temporary runs. |
| rdf4j.lmdb.native.fused.maxBytes | 8 MiB by default. | Read into a static budget when LmdbFusedSipFactorizedRuntime initializes; session suballocations share it. |
| rdf4j.lmdb.nodePredicateRoute.maxBytes | 4 MiB; bytes. | Read for route construction. The exact projected-array estimate must fit before build; refusal preserves the global predicate route. |
| rdf4j.lmdb.nodePredicateRoute.minimumSavings | 0.25; modeled-work fraction. | Read with route construction; the estimated route must clear this saving before its extra projected memory/work is admitted. |
| rdf4j.lmdb.janinoCodegen.thresholdRows | 128 rows; maxEntries defaults 512 with a minimum of one. | Threshold is checked at kernel request; entry cap is sampled when a new shape is inserted. One shared process cache stores id-free compiled classes; failed shapes stay cached until eviction. |
| rdf4j.lmdb.nativeSpecialization.thresholdRows | 32,768 rows; maxEntries 128 and maxGeneratedBytes 512 KiB by default. | Read at request/eviction/compile publication. The process LRU keys (slotCount, readMask); a byte or entry refusal uses the interpreter. This uses Java Class-File API hidden classes, not Janino. |
| rdf4j.lmdb.janinoCodegen.vectorSize | 2,048 rows. | KernelRuntime's compiled-kernel batch helper uses its class-level configured size. |
| rdf4j.lmdb.packedFtree.vectorRows | 2,048 rows, normalized to the nearest lower power of two after clamping to 64–65,536. | Static physical block width when the packed f-tree class initializes; it scales each active variable's packed vector. |
| rdf4j.lmdb.packedFtree.rootBatchRows | 1,024 rows; clamped at least 64 and at most vector width. | Class initialization; root refill batch, not the capacity of all descendants. |
| rdf4j.lmdb.packedFtree.initialNodeRows | Defaults to vector width; clamped 64–vector width. | Class initialization; initial node-vector allocation size. |
| rdf4j.lmdb.packedFtree.exactOrderVariables | 9 variables, minimum 3. | Class initialization; cutoff for exact f-tree variable-order search before beam use. |
| rdf4j.lmdb.packedFtree.orderBeamWidth | 128 alternatives, minimum 8. | Class initialization; search frontier width, not an operator row cap. |
| rdf4j.lmdb.packedFtree.parallel.minRootsPerPartition | 1,024 roots; positive configured values accepted. | Read at packed grouped-sink partition admission. It is a minimum partition work count. |
| rdf4j.lmdb.packedFtree.parallel.morselRows | Defaults to the packed root-partition threshold (1,024 roots); minimum 1. | Read when packed root morsel ranges are formed; target roots per morsel, not a cap on a worker's packed input state. |
| rdf4j.lmdb.packedFtree.parallel.outputRows | 256 output rows; clamped 1–4,096. | Read when a packed worker session opens. Sets rows per result page; the page arrays are included in a query-memory reservation when row output is used. |
| rdf4j.lmdb.packedFtree.parallel.queuePagesPerWorker | 2 pages; clamped 1–16. | Read when a packed worker session opens. Bounds queued output pages per worker; the reservation scales with workers × pages × page width. |
| rdf4j.lmdb.packedProjectionBuild.maxLanes | 2,048 lanes, valid range 1–65,536; out of range throws. packedProjectionBuild.enabled and .nested.enabled default true. | Read when the builder opens; lane cap bounds staged intermediate construction, not borrowed source storage. The builder is considered only for an admitted single nonterminal spine. |

## Join, membership, path and parallel thresholds

| Property | Default and units | Read point and refusal behavior |
|---|---|---|
| rdf4j.lmdb.nativeHashJoin.minRows / .maxBuildRows | 4,096 estimated/probe rows; 1,048,576 build rows. | Read at hash candidate construction/open. Shape, correlation, key width, and the secondary row cap still apply. |
| rdf4j.lmdb.mergeJoin.minRows / .maxRunRows | 4,096 rows; 16,384 rows per run. | Read when an ordered merge proposal is considered. Requires compatible ordered inputs and bounded runs. |
| rdf4j.lmdb.leftjoin.hash.minProbes / .maxRows | 1,024 probes; 1,000,000 build rows. | Read during OPTIONAL payload-hash admission; source/probe eligibility remains required. |
| rdf4j.lmdb.leftjoin.sweep.maxRows | 1,000,000 rows. | Read for the opt-in single ordered sweep path; over-cap shapes retain another OPTIONAL route. |
| rdf4j.lmdb.markJoin.missThreshold / .maxKeys / .maxSweepRows | 64 misses; 4,000,000 keys; 16,000,000 rows. | Read at mark-join admission. markJoin.enabled defaults false via Boolean.getBoolean; this strategy remains opt-in. |
| rdf4j.lmdb.membership.missThreshold / .maxSize | At least 64 observed probes; 4,000,000 keys. | Runtime build admission for a repeated pattern probe. membership.impl defaults hash; exact off disables this layer. |
| rdf4j.lmdb.adjacencySemijoin.streamMinProbes / .streamMinRejects / .streamMinRejectionPercent | 64 probes; 8 rejects; 12 percent. | Captured into stream-admission settings as the operator is opened; starts the ordered merge-semijoin only after enough observed misses/rejects. |
| rdf4j.lmdb.adjacencySemijoin.streamDenseMinProbes / .streamMinKeyCount | 512 probes; 2,048 keys. | Same admission record; dense ordered domains use a stricter floor. streamKeys defaults on unless case-insensitive false. |
| rdf4j.lmdb.nativePath.targets.maxValues / .memo.maxValues | Targets default 65,536 values; path memo defaults 1,048,576 values (clamped to the factor-tail cap). | Read when a path operation chooses target/memo support; over-budget result retention bypasses the memo/target fast path rather than truncating semantics. |
| rdf4j.lmdb.nativePath.frontier.parallelMin / rdf4j.lmdb.wcoj.parallelMinCandidates | 512 nodes/candidates each. | Read when path frontier or leapfrog parallel expansion is considered. Parallel executor/source/resource checks are separate. |
| rdf4j.lmdb.prefixRun.parallelMinEstimate / rdf4j.lmdb.existsIntersection.parallelMinEstimate | 500,000 estimated rows each. | Read when prefix-run or existence-intersection partitioning is considered. Below the threshold the serial route remains. |
| rdf4j.lmdb.parallel.threads / .maxTasks | Threads default max(1, CPUs - 1), clamped 0–64; maxTasks defaults CPUs + 1, clamped 1–65. | Read per worker admission. The actual pool is process-wide, fixed at 65 daemon workers with a 65-entry queue; per-query grants are also bounded by a task reservation. |
| rdf4j.lmdb.parallel.rangePartition.factor | 4, clamped 1–16. | Read when computing over-partition target; target is also capped by 64 and by one partition per 4,096 estimated rows. |
| rdf4j.lmdb.parallel.minWorkEstimate | 2,048 native work units; finite nonnegative configured values accepted. | Read at parallel proposal admission; admission only lets the strategy compete—the startup-cost price and arbiter may still choose serial. |
| rdf4j.lmdb.parallel.startupWork | 12,500 native work units; finite nonnegative configured values accepted. | Read when parallel strategy work is priced. This is a modeled intercept, not a latency measurement. |

Packed factor rows/tree sizing and property-path gates have additional exact semantics in [joins and factors](query-joins-and-factors.md) and [paths](query-paths-and-special-operators.md). In particular, enabling chunk-pipeline flags alone does not make chunkPipeline reachable: current strategy source has no proposal site for that tag.

## Cost learning, probe and hedge tuning

The two adaptive config records are created for their owning arbiter/store-model state; an active query, probe or worker does not re-read every numeric setting. The boolean enabled switches are also in the runtime registry, but the system() config snapshot determines the current model/arbiter instance behavior.

| Namespace/property | Default and units |
|---|---|
| rdf4j.lmdb.costModel.noiseFloorLog, .noiseInitialLog | 0.05 and 0.09 log-runtime noise values. |
| rdf4j.lmdb.costModel.varianceFloor.{exact,family,global} | 0.02, 0.01, 0.005 log-runtime variance. |
| rdf4j.lmdb.costModel.priorVariance.{exact,family,global} | 1.0, 0.5, 0.25 log-runtime variance. |
| .huberSigmas, .decayHalfLifeMillis | 3.0 standard deviations; 600,000 ms. |
| .displacementLogGamma, .displacementLogGammaOrderLosing, .cooldownTtlMillis | 0.10, 0.35 log-runtime margins; 300,000 ms. |
| .changeDetector.delta, .changeDetector.lambda | 0.25, 25.0. |
| .persist.staleHalfLifeMillis, .persist.kappa, .persist.precisionCap | 86,400,000 ms; 0.8; 400.0. |
| rdf4j.lmdb.adaptiveProbe.alpha, .gamma, .maxSlowdownFraction, .lambdaInfo, .minWinProbability | 0.05, 1.25, 0.8, 0.1, 0.02. Fractions must be between zero and one; gamma must be at least one. |
| adaptiveProbe.maxDeadlineMillis, .minDeadlineMicros, .cancelBoundMillis | 10,000 ms; 500 µs; 10 ms. |
| adaptiveProbe.bufferRows, .maxPerQuery, .minSpacingDecisions | 4,096 rows; 1 probe; 4 decisions. |
| adaptiveProbe.minSpacingMillis, .cooldownBaseMillis, .coldStartCooldownMillis | 10 ms; 30,000 ms; 100 ms. |
| adaptiveHedge.launchOverheadMicros, .interferenceFraction, .evidenceWeight, .contentionDiscount | 200 µs; 0.25; 0.5; 0.5. Fractions must be in (0,1). |
| adaptiveHedge.maxConcurrentHedges, .maxHedgesPerQuery, .overshootQuarantineFactor | max(1, CPUs/4); 1; 4.0. |
| adaptiveHedge.exchangePages, .exchangePageRows | 4 pages; 1,024 rows/page. |
| adaptiveHedge.normalGuard.minTriggerMillis, .triggerMargin, .unpricedTriggerFactor, .remainingQuantile, .backupQuantile, .minBackupEvidence | 10 ms; 1.25; 8.0; 0.75; 0.95; 5 samples. |

Posterior/probe/hedge numeric readers trim property text, reject malformed/non-finite values and use the source default when positivity/range checks fail. The log-model quantities are not milliseconds unless their suffix says so. Probe safety-credit alpha is a budget parameter; it is not a guaranteed hard cap on charged time because mandatory first trials and overshoot can create debt. The hedge permit pool is process-wide; the bounded probe scheduler is per store-cost-model. The [cost model guide](query-cost-learning-and-adaptation.md) describes evidence lifetimes and cost-model.lncm persistence.

## Defaults that are easy to mistake for fallback promises

- A feature defaulting on still requires shape recognition, source capabilities, current generation, memory admission, and successful bind/open. A disabled specialist usually returns to its ordinary native or generic route; it does not mean the query becomes unsupported.
- The Java boolean parser accepts true, not on. The unlessFalse parser has the opposite polarity: almost every value except exact lowercase false enables it. Do not copy a property's parser from a similarly named neighbor.
- A candidate can be selected and later decline before producing output; only a registered alternative is then available. After a result has escaped, errors/cancellation close and fail the chosen route rather than replaying the whole query.
- Numeric row/value caps and process-level cache entry counts are cardinality limits. They do not prove a Java-heap byte bound unless a documented reservation is charged. See [memory](query-memory-and-result-lifecycle.md) and [cache ownership](query-plan-caches-and-invalidation.md).
- Construction, on-disk layout, store path, capacity/size, fatal-error policy, debug output, and measurement knobs are excluded from the runtime allowlist. Rebuilding an active store or changing its layout is not what this registry's set() API does.

## Source and configuration test map

[LmdbRuntimePropertiesTest](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRuntimePropertiesTest.java) covers registry membership, default-off experiments, read/write behavior, and selected consumer-default agreement; it does not establish agreement for every registered property. [LmdbNativeAdaptiveCostModelTest](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAdaptiveCostModelTest.java) tests adaptive toggle defaults and opt-out. [LmdbNativeProbeSchedulerTest](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeProbeSchedulerTest.java), [LmdbNativeHedgePolicyTest](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeHedgePolicyTest.java), and [LmdbNativePosteriorStoreTest](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePosteriorStoreTest.java) characterize representative scheduler, hedge-policy, and posterior behavior, not every numeric property parser. Direct per-feature gate tests are linked in their family guides and [query test map](query-test-map.md). These are source links only; no tests/config mutations were executed during this documentation work.
