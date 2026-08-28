# Make the node-to-predicate projection a first-class query primitive

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

Maintain this document in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF data is a set of statements, each a subject, a predicate, an object, and an optional named graph called a context. The LMDB store in this repository keeps those statements on disk in sorted B-trees, and additionally keeps an optional in-memory mirror called the direct adjacency index that can answer some query shapes far faster than the disk trees can.

Until recently that in-memory mirror could not answer one common shape at all: "given this one subject, give me every predicate and object attached to it", written in SPARQL as `<s> ?p ?o`. The mirror is organised predicate-first, so with no predicate supplied it had nothing to look up. Someone has since written a small sidecar structure that fixes exactly this — it maps a node to the sorted list of predicates that node uses — and wired it into one consumer.

After this ExecPlan, three things are true that are not true today. First, that sidecar is safe: it can no longer take the entire adjacency index down with it when memory is tight, it has an off switch, its memory cost is visible in the logs, and there is a test that proves the answers it produces are identical to the answers the authoritative disk trees produce. Second, the query compiler can see variable predicates. Today any SPARQL pattern whose predicate is a variable falls out of the compiled fast path entirely; after this work, patterns with a bound subject or object and a variable predicate compile and run in parallel like every other shape. Third, the reverse direction — "which predicates point at this object", written `?s ?p <o>` — becomes available behind its own switch.

You can see each of these working. The safety work is visible as a test that starves the memory budget and observes the store still coming up healthy. The compiler work is visible as a benchmark cell whose count of compiled variable-predicate patterns moves from zero to non-zero, and as a per-cell route ledger that records which execution route every affected shape takes in each configuration. The reverse direction is visible as a query shape that stops declining.

One sentence of this paragraph has been rewritten after the work was measured, and the original is worth keeping in view because the difference is the main finding. It read that the compiler work would be visible as a cell "whose kernel-open count moves from zero to non-zero and whose runtime drops below both interpreted tiers with non-overlapping error bars". Neither half survived contact with a measurement. The kernel-open count could not move, because the flagship cell's other half already compiles with every switch off; and the runtime rose rather than fell, by a factor of 2.85 with disjoint intervals. All three capabilities therefore ship behind switches that default to off. The mechanism, the numbers and what would have to change are in `Outcomes & Retrospective`; the short version is that a node's statements are already contiguous in the subject-ordered disk tree, so a transpose cannot beat it at a plain dump — its purpose is the wildcard traversal that cannot go to disk at all, and that consumer does not exist yet.

## Progress

- [x] (2026-08-07 20:44Z) Ran the repository-root `-Pquick clean install`; BUILD SUCCESS in 34.7 seconds, so the tree compiles before any edit.
- [x] (2026-08-07 20:44Z) Read `.agent/PLANS.md` in full and confirmed the envelope: this file's content is only the ExecPlan, so it carries no triple-backtick fence, uses indented blocks for all transcripts and code, and keeps checkboxes to this section.
- [x] (2026-08-07 20:44Z) Established the three facts the design depends on by reading source rather than assuming: `MemoryKind` is a flat enum, `findRun`/`findRunByOrdinal` resolve against the primary index and not the sidecar, and the run codec makes each object-context pair its own edge.
- [x] (2026-08-07 20:44Z) Authored this plan.
- [x] (2026-08-07 20:57Z) Milestone A1 — renamed to the node-predicate vocabulary across eight files, added the nullable accessor, documented the borrowed non-owning contract, and made an unsupported plane throw instead of answering zero. One of the three suspected contract gaps turned out not to exist; see `Surprises & Discoveries`. Focused suites green: node-predicate index 6/0/0, node-predicate updates 4/0/0, paged base builder 3/0/0, direct-adjacency query 57/0/0, commit 13/0/0, consolidation 11/0/0.
- [x] (2026-08-07 22:53Z) Milestone A2 — all four switches shipped default-off. `LmdbNodePredicateOptions` makes "incoming without outgoing" unrepresentable; `LmdbDirectAdjacencyOptions` resolves the two build-time switches once through the pure seam; `LmdbDirectAdjacencyStore.nodePredicateServingEnabled()` is re-read per call. A malformed boolean is rejected rather than silently read as off. `LmdbNodePredicateSwitchTest` 5/0/0; consolidator refusal test added to `LmdbDirectAdjacencyConsolidationTest` 12/0/0.
- [x] (2026-08-07 21:12Z) Milestone A2, containment half — a refused projection now leaves the paged base published and fully usable instead of destroying it. Suites green: paged base builder 4/0/0, node-predicate index 6/0/0, node-predicate updates 4/0/0, memory account 15/0/0, build workspace 3/0/0, direct-adjacency query 57/0/0, consolidation 11/0/0.
- [x] (2026-08-07 22:53Z) Milestone A3 — the two memory kinds now feed `memoryUsageSummary()` (so both lifecycle log lines carry them), the metrics snapshot, and the benchmark-facing test-access class, all reporting the *charged* bytes rather than `modeledJavaBytes()`. The consolidator gained a second budget drawing on the same account and the same global cap, and its projection rewrite record is logged instead of dropped. The 160-byte workspace constant now carries its derivation.
- [x] (2026-08-07 23:05Z) Milestone A4 — `LmdbNodePredicateEnumerationParityTest` compares the served multiset against the disk trees at all four lifecycle points over a deliberately built fixture (one-predicate node, 300-predicate node, inferred plane, named graphs, inlined object, overlay-only node, tombstoned node, post-base predicate), plus focused tests for the chunked and one-predicate branches and for a context restriction. 6/0/0. Lifetime and transaction coverage not yet added; see `Outcomes & Retrospective`.
- [x] (2026-08-07 23:04Z) Milestone A5 — shadow mode now compares this shape (the comparator was split so any iterator can be compared, and the no-transaction early return is documented as the parallel-worker gap it is). A dedicated `LmdbNodePredicateInconsistencyException` replaces the bare `IllegalStateException`, the store degrades only this capability, schedules the rebuild that restores it, and counts once per fault; `LmdbNodePredicateRecoveryTest` asserts the five-step sequence in order. The stale census baseline is not yet corrected — with the switches default-off the cell still declines, so the removal remains true and only its stated reason is wrong.
- [x] (2026-08-08 08:22Z) Milestone A6 — five cells added to `ThreeTierParityCorpus`, `ThreeTierParityBenchmark` and `AdjacencyQueryShapeBenchmark`, and a per-cell route ledger added to the census. The ledger pins each cell's route in the adjacency and Janino regimes, in both configurations, plus the projection's decline count and the variable-predicate lowering count; the census gained a second test that runs the same sweep with every switch on. `ThreeTierEngagementCensusTest` 4/0/0 (1 skipped, the opt-in theme half), `AdjacencyQueryShapeBenchmarkTest` 1/0/0.
- [x] (2026-08-08 08:10Z) Milestone B1 — count, existence and predicate-ordered streaming, each behind the same eligibility gate the iterator branch uses, so a count can never engage where the equivalent iterator declines. `LmdbNodePredicateShortcutTest` 7/0/0.
- [x] (2026-08-08 08:31Z) Milestone C1 — `RunView` extracted, `DynamicAdjacency` and `NodePredicates` added beside `NativeAdjacency`, `LmdbDirectNodePredicates` implements both over one plane of one published base, and the total-resolution contract is proven by fault injection in all three rungs. `LmdbNodePredicateKernelFaultTest` 3/0/0.
- [x] (2026-08-08 08:27Z) Milestone C2 — `EnumeratePredicates` and `ProbeVariable` with bounded-chunk row reads and per-node scratch, proven against the real generated Java. `LmdbNativeNodePredicateKernelTest` 8/0/0.
- [x] (2026-08-08 08:09Z) Milestone C3 — lowering dispatch, the two binding request kinds, and both parallel rungs; the binding truth table agrees with the ordinary evaluator on all fifteen shapes. `LmdbNativeVariablePredicateKernelTest` 2/0/0.
- [x] (2026-08-08 08:39Z) Milestone A4 completion — the four lifetime and transaction tests (`LmdbNodePredicateLifetimeTest` 4/0/0) and the randomised statement-level oracle for the update companion (`LmdbNodePredicateUpdatesTest` 5/0/0), both of which the earlier A4 entry recorded as outstanding.
- [x] (2026-08-08 09:05Z) Flip F1 — outgoing construction stays **off** by default. See the Decision Log.
- [x] (2026-08-08 09:05Z) Flip F2 — compiled predicate enumeration stays **off** by default. See the Decision Log.
- [x] (2026-08-07 23:13Z) Milestone D1 — the projection is plane-generic end to end: `emit` walks the plane mask, the updates companion follows `supportsPlane` instead of a hard-coded direction and stably partitions by plane with a counting sort, and the store's inline guard is narrowed to retired implementations. Parity extended to the incoming direction including an inlined literal object; a zero-cost test proves the incoming planes add real bytes when on and none when off.
- [x] (2026-08-08 09:05Z) Flip F3 — incoming planes stay **off** by default. See the Decision Log.
- [x] (2026-08-08 09:20Z) Full-module gate — `core/sail/lmdb` runs 3132 tests in 13 minutes 23 seconds with 4 skipped and one failure, `LmdbNativeFactorizedAdjacencyMemoTest.tailBranchSkipsValueMemoWhenProbeIsCacheBacked`. The previous full run of this module, on 2026-08-07, reported 3101 tests and no failures, so this work adds 31 tests and leaves one failing. That failure is triaged in `Surprises & Discoveries` and is not caused by this work: a repeat full-module run with every test class added or modified in this session excluded fails on the same test with the same assertion, and the test passes in isolation, alongside the census, and alongside its immediate predecessors. It is recorded rather than silenced.

Note on honesty of this list. Milestones A1 and A2 are partly retroactive. The sidecar and its wiring were written before this plan existed and before any test proved the wiring correct; A1 and A2 bring already-written code under contract rather than adding new behaviour. That is recorded here rather than presented as fresh work.

## Surprises & Discoveries

- Observation: A refusal to allocate the optional sidecar destroys the entire adjacency index for the lifetime of the process, not just the sidecar.
  Evidence: `LmdbPagedCsfBaseBuilder` builds the projection inside the primary build, so `LmdbAdjacencyMemoryRefusedException` propagates out of the whole build. In `LmdbDirectAdjacencyStore`, the catch clause for that exception sets `maintenanceState = MaintenanceState.MEMORY_REFUSED` and stops. The neighbouring catch clause for `IOException | RuntimeException`, ten lines above, instead sets `MaintenanceState.EMPTY` and calls `triggerBuild()`. Only the second path ever recovers.

- Observation: The benchmark cell that appears to measure this feature has no headroom and was measuring a declined path.
  Evidence: `benchmark-results/tier-m11-report-2026-08-07.txt` line 23 reads `nodeEdgeDump 0.017±0.001 0.017±0.001 0.017±0.001 OVERLAP OVERLAP` for the disk, adjacency, and compiled regimes respectively, while `benchmark-results/tier-m11-adjacency-r1-2026-08-07.txt` records `PREDICATE_ENUMERATION_INCOMPLETE=96376` for the same run. The adjacency tier was refusing to serve the shape 96,376 times and still tied on wall time, so a single-node dump cannot demonstrate this work.

- Observation: A test baseline asserts that this shape is unservable, and did not fail when the shape started being served.
  Evidence: `ThreeTierEngagementCensusTest.adjacencyServedBaseline()` calls `served.remove(ThreeTierParityCorpus.NODE_EDGE_DUMP)`, and the class comment describes that cell as one "the paged adjacency base cannot answer and which the plan deliberately leaves declining to LMDB". Because `assertRecordedServiceHolds` only checks that cells present in the recorded set still serve, a cell that begins serving unexpectedly triggers nothing.

- Observation: `MemoryKind` has no separate axis for native versus Java memory, so a single new kind cannot report both figures.
  Evidence: `LmdbAdjacencyMemoryAccount` declares the enum with the sibling constants `BASE`, `DELTA`, `PENDING`, `RETAINED_SNAPSHOT`, `BUILD_COUNTERS`, `BUILD_OUTPUT`, `CONSOLIDATION_OUTPUT`, and `JAVA_METADATA`, and the class comment states that "reclassification moves an existing charge between kinds without changing the total". Native and heap are two of the kinds, not two dimensions of each kind.

- Observation: A dynamic single-predicate probe has no dependency on the sidecar at all.
  Evidence: `LmdbInMemoryAdjacencyIndex.findRunByOrdinal` resolves through `csfBase.findLocalReference(...)`, which is the primary index. Only predicate *enumeration* needs the projection; resolving one already-known predicate does not.

- Observation: Statement multiplicity across named graphs is already expanded in the run encoding, so summing run lengths counts statements rather than distinct objects.
  Evidence: `LmdbAdjacencyRunCodec` exposes `neighborAt(catalog, runHandle, ordinal)` and `contextAt(catalog, contexts, runHandle, ordinal)` indexed by the same ordinal, so one object appearing in two named graphs occupies two positions in the run.

- Observation: An existing test was, unnoticed, asserting the very defect this plan removes. On the builder fixture the projection's charge is what sets the build's overall memory peak, so `refusesOneByteBelowTheMeasuredExactBuildHighWaterWithoutLeaking` was passing only because a projection refusal destroyed the entire build.
  Evidence: After containment that test failed with `Expecting code to raise a throwable`, because one byte below the measured high water now refuses only the optional projection and the base is published without it. The earlier reasoning in this plan — that the peak lay in the transient workspace phase and the test would therefore be unaffected — was drawn from a smaller diagnostic fixture and did not hold for the real one. The test now measures the high water with the projection's kind starved, so it describes the authoritative build alone, which is what it was always meant to assert.

- Observation: The projection's metadata charge exceeds the finished index's own `modeledJavaBytes()`, and the surplus is never released.
  Evidence: On the builder fixture the account holds 4,760 bytes under the projection's Java kind while `LmdbNodePredicateIndex.modeledJavaBytes()` reports 4,376, a difference of 384 bytes. The charge comes from the build plan, which is conservative; `assertExhausted` constrains the native side, not this one. Milestone A3 must therefore report the charged figure rather than `modeledJavaBytes()`, since the charge is what consumes the cap. This also explains an earlier arithmetic failure in the containment test, where summing the two self-reported models over-counted the projection by exactly 384 bytes.

- Observation: A memory cap cannot isolate a projection refusal, because the build's transient workspace peak dwarfs every persistent charge. The originally planned acceptance for milestone A2 was therefore impossible to write as stated.
  Evidence: A temporary diagnostic over the three-subject builder fixture reported `highWater=5682752`, `totalAfterBuild=13240`, `projNative=512`, `projJava=4352`, and `byKind={BASE=1560, JAVA_METADATA=11680}`. The projection is 4,864 bytes of a 13,240-byte persistent total, while the peak is 5.68 MB of transient build workspace released before the projection is built. Any cap low enough to refuse the projection refuses during the workspace phase instead, and any cap that clears the workspace leaves several megabytes of headroom for a five-kilobyte projection. The diagnostic was deleted after measurement.
  Consequence: the milestone A2 acceptance is restated in terms of a deliberate refusal injected at the projection's charge, and the memory-kind separation originally scheduled for milestone A3 moves into A2 so the injection can name the projection's own kinds. This also confirms the pre-existing test `refusesOneByteBelowTheMeasuredExactBuildHighWaterWithoutLeaking` is unaffected by the containment change, since its cap is breached in the workspace phase long before any projection work.

- Observation: One of the three contract gaps this plan set out to close does not exist. `resolve` forwards an unvalidated reference, but the receiving code already refuses a malformed one, so no guard was added and none is needed.
  Evidence: The new test `resolveRejectsNonPositiveReferenceAtTheCsfBoundary` passed on its first run, before any production change. `ImmutablePagedQuadCsfIndex.unpackPageId` throws `IllegalArgumentException("invalid CSF local reference: " + localReference)` when the encoded page is zero, which covers a reference of zero, or when it exceeds the maximum page id, which covers a reference of negative one after the unsigned shift. The test was kept and relabelled as a characterization test that pins the guarantee against a future change to the reference encoding, rather than being presented as a fix.

- Observation: The name being replaced is already taken elsewhere in the codebase for an unrelated structure, which strengthens the case for the rename beyond the incoming-plane argument.
  Evidence: `core/sail/extensible-store/src/main/java/org/eclipse/rdf4j/sail/extensiblestore/evaluationstatistics/ExtensibleDynamicEvaluationStatistics.java` declares `private final HLL[][] subjectPredicateIndex`, a HyperLogLog cardinality estimator with no relationship to adjacency. Two unrelated concepts shared one identifier across modules.

- Observation: The rename surface was smaller than the raw search suggested, because three files match the old term for unrelated reasons and must not be touched.
  Evidence: A repository-wide search for the old term returns 118 occurrences across 11 files, but the extensible-store statistics class, a test method named `hasTriplesTreatsZeroSubjectPredicateAndObjectAsUnboundButContextAsExact` in `TripleStoreTest`, and a test method named `selectedCoverageOnDefaultIndexesSeeksAcrossUncoveredSubjectPredicateRuns` in `LmdbAdjacencyBuildTxnFamilyTest` are all unrelated English prose or a different concept. The true surface is 81 occurrences across 8 files.

- Observation: The word "edge count" means two different things one call apart, which is a live trap for the count shortcuts in Milestone B1.
  Evidence: In `LmdbDirectNodeIterator`, `subjectPredicateCursor.edgeCount()` is the number of predicates attached to the node, while `LmdbAdjacencyRunCodec.edgeCount(catalog, runHandle)` on the very next resolution is the number of statements under one of those predicates. This plan renames the first concept to `rowSize` in all new interfaces.

- Observation: The prediction that an inlined object would have to keep declining on the incoming side is wrong, and the reason it is wrong makes the incoming planes strictly more useful than the plan assumed.
  Evidence: `LmdbInMemoryAdjacencyIndex.findRunByOrdinal` takes the `csfBase != null` branch first and resolves any raw key through `csfBase.findLocalReference`, reaching the `inlinePlaneKeys` binary search only for a retired implementation. A paged base therefore has no separate inline incoming structure at all: its incoming planes are keyed by the raw object id whether or not that id is inlined, so the projection covers them uniformly. The serving branch's `ValueIds.isReference(key)` guard was protecting the legacy path and has been narrowed to it; `incomingPlanesAgreeWithTheDiskTreesWhenEnabled` now serves and matches for an inlined literal object.

- Observation: A fold-down does not exercise the projection rewrite at all unless the commits change which predicates a node has.
  Evidence: The first version of the consolidator refusal test committed twelve additional objects under an existing predicate on an existing subject and never reached the budget: `LmdbNodePredicateUpdates.size()` was zero, so the no-change fast path shared the previous structure through `retainedCopy()`. Only commits that introduce or remove a predicate on a node produce a rewrite. This is the same fast path lifecycle point 4 of the parity suite exists to cover, and it is why that point is worth its own comparison rather than being folded into point 3.

- Observation: The flagship benchmark cell measured nothing until its classes were named. With both sides of `?s a ?t . ?s ?p ?o` unbound the planner roots the whole query on the unbound-predicate pattern, evaluates it as a scan with no subject in hand, and never consults the projection at all.
  Evidence: the first census run of the new cells printed `classPredicateMatrix 62 0 0 0 0 0 0 0 lmdb only` — zero adjacency engagement, zero kernels and, decisively, zero `PREDICATE_ENUMERATION_INCOMPLETE` declines in every regime and in both configurations. A cell that never even asks cannot show a difference when the answer changes. Supplying the classes as a `VALUES` list makes the type lookup the smaller side; the same cell then records 608 declines with the switches off and 610 plane hits with them on.
  Consequence: this is also why the plan's own `nodeEdgeDump` baseline was misleading in the opposite direction. That cell does ask — one decline per run — but answers in 17 microseconds, so the decline is invisible in the score. A cell needs both properties to be an instrument: it must ask, and it must be big enough for the answer to matter.

- Observation: "Capture a red baseline" is the wrong acceptance for two of the five new cells, because a kernel already compiles them while the projection is off.
  Evidence: with every switch off the census records `repeatedNodeDump ... jan:kernels=1 np:decl=64` and `variablePredicateJoin ... jan:kernels=1 np:decl=5`. The `VALUES`-driven and join-driven halves of those shapes compile perfectly well without any projection; what the projection changes is that the dump inside them stops declining. The route is therefore unchanged by the flip and the decline counter is what carries the claim, which is exactly why the ledger records four facts per cell rather than one.

- Observation: A lone triple pattern never compiles, so the incoming dump serves interpreted even with every switch on, and its ledger row says so.
  Evidence: with the projection on the census records `incomingEdgeDump 6 1 0 1 0 0 0 0 adjacency` — the planes serve it, no kernel opens, and no variable-predicate pattern lowers. The compiled tier takes shapes with something to fuse; one scan is not one. This is a property of the kernel admission rule rather than of this work, and the ledger pins it rather than pretending the cell will compile.

- Observation: "Each rung must fail rather than emit an incomplete result" is satisfied in two different ways depending on when the fault is noticed, and only one of them is an exception.
  Evidence: `LmdbNativeParallelKernelRows` captures a worker failure in an `AtomicReference` and, if nothing has yet reached the query thread, returns null — a decline — for every non-`Error` failure; its comment reads "Every failure before the cursor exists declines exactly — nothing has been emitted." Once pages have been handed over, `ParallelKernelRowCursor.next()` rethrows instead. Both are correct: a decline re-runs the shape interpreted, which then declines the projection too (the store has degraded) and answers from the disk trees in full. The acceptance is therefore written as "never a short answer" rather than "always throws", and the test allows either outcome while requiring the fault to have been detected, so it cannot pass vacuously.

- Observation: The incoming planes cost about the same as the outgoing ones, not "more than double" as this plan assumed throughout. The assumption was reasonable and wrong.
  Evidence: over the shared multi-theme store with full coverage, the projection's charge is 12,359,912 bytes for the outgoing planes and 24,349,520 for all four — so the incoming half adds 11,989,608 bytes, which is 0.97 times the outgoing half rather than more than 2. The reasoning behind the assumption was that the incoming row domain is distinct objects including referenced literals and is therefore the larger domain; what that overlooked is that the structure stores predicates per row, and a typical object participates in far fewer distinct predicates than a typical subject. More rows, shorter rows, similar total.
  Consequence: the F3 decision no longer rests on memory asymmetry, and its rationale is rewritten to rest on what was actually measured — that the incoming cell is unchanged by the switch, so the cost buys nothing yet, whatever its size. Build time is the sharper asymmetry: the incoming planes add 63 percentage points to a build that the outgoing planes have already doubled.

- Observation: Projection construction roughly doubles the entire base build, which is worse than the "serial tail" framing suggested.
  Evidence: over the same store, `nodePredicates=DISABLED` builds in 800.785 ms, `OUTGOING` in 1616.311 ms and `ALL_PLANES` in 2124.994 ms. The projection is not a tail on a long build; it is as long as the build. It runs single-threaded after the parallel primary build has finished, so it does not benefit from the build threads the primary index uses, and its two complete merges across every predicate's key list touch the same volume of data the parallel build just finished touching.
  Consequence: moving construction inside the parallel build is the single highest-value follow-up for anyone who wants this default flipped, and it is independent of the read-path question.

- Observation: The one failure in the full-module gate belongs to an unrelated test and is not caused by this work. It is an order-sensitive assertion on a process-wide counter, which is a shape that fails for reasons no single test controls.
  Evidence: `LmdbNativeFactorizedAdjacencyMemoTest.tailBranchSkipsValueMemoWhenProbeIsCacheBacked` asserts that `JoinDispatchTestAccess.factorizedEngaged()` increases across its query; in the full run it read 383 before and 383 after. Five things place it outside this work. It passes in isolation, 1/0/0. It passes together with both census tests, 5/0/0, so the largest new interaction is not the trigger. It passes with the two classes that immediately precede it in suite order, 45/0/0. Its query is `?s ex:p1 ?a . ?a ex:p2 ?y` — two *constant* predicates — so the variable-predicate lowering added here cannot fire for it, and every other change in this milestone set is inert without a variable predicate. Every test in the module that sets `rdf4j.lmdb.janinoCodegen.thresholdRows` without clearing it runs after it, so the obvious leak path is ruled out. Decisively, a full-module run with every test class added or modified in this session excluded still fails on the same test with the same assertion, and the same assertion failed once before this work existed, in `logs/mvnf/20260805-092453-verify.log`.
  Consequence: recorded rather than fixed. Making an unrelated test pass by adjusting it, without understanding which of the several hundred tests before it perturbs the dispatch decision, is the kind of change that hides a real defect rather than removing one. The durable fix is for that test to isolate its counter rather than assume nothing else in the JVM moved it, and it belongs to whoever owns the factorized-rows path.

- Observation: A print in a JMH `@TearDown(Level.Trial)` never reaches the result file, and this repository already knew that.
  Evidence: the first build-cost run produced its timing rows but not one `### adjacency build cost:` line. `ThreeTierParityFixtures.Fixture.engagementSinceLastReport` carries a comment saying exactly this — "JMH stops forwarding a forked JVM's output before trial teardown runs, so a once-per-trial summary never reaches the result file" — which is why the parity harness reports per iteration. Moving the print inside the measured method fixed it; the cost is a handful of lines next to a multi-second build.

- Observation: The corruption-recovery sequence cannot be observed by simply querying twice, because the rebuild it schedules is asynchronous and wins the race.
  Evidence: The first version of `LmdbNodePredicateRecoveryTest` saw the second query served again, from a base the maintenance thread had already republished. Parking the scheduled rebuild on `afterBuildScanForTest` makes the degraded window deterministic, but it also drops the whole index (`quiescentRebuild` publishes an UNAVAILABLE state before waiting for readers), so a null result during that window no longer distinguishes "this capability is degraded" from "everything is briefly gone". The test therefore asserts the gate itself alongside the outcome.

## Decision Log

- Decision: Rename `LmdbSubjectPredicateIndex` and `LmdbSubjectPredicateUpdates` to `LmdbNodePredicateIndex` and `LmdbNodePredicateUpdates`, and rename the accessor and fields to match.
  Rationale: Milestone D1 adds incoming planes, whose row coordinate is an object rather than a subject, at which point the existing name is simply wrong. The rename costs nothing today because all 74 references are package-private with no public API surface, and it costs a great deal once the branch merges. `EndpointPredicate` would be marginally more precise than `NodePredicate`, since incoming keys may be referenced literals, but `NodePredicate` matches the vocabulary of the existing `LmdbDirectNodeIterator` and consistency is worth more than marginal precision here.
  Date/Author: 2026-08-07, Claude.

- Decision: Move the memory-kind separation from milestone A3 into milestone A2, and add a package-private refusal hook to the memory account so a test can starve exactly one kind.
  Rationale: The two milestones turned out to be coupled. Containment cannot be tested through the public cap, for the reason recorded in `Surprises & Discoveries`, so the test needs to name the projection's charge specifically — which requires the projection to have its own memory kind. A hook on the account rather than on the builder keeps the seam in the place that already owns charging decisions, and `...ForTest` matches the convention already used by `beforePagedCsfPublicationForTest` and `overrideBaseFormatForCurrentThread`. The alternative, a synthetic fixture with roughly ten thousand nodes sized to make the projection exceed the workspace floor, would be slow and would break the moment the workspace floor changed.
  Date/Author: 2026-08-07, Claude.

- Decision: Keep the `resolve` characterization test even though it never failed, and add no guard behind it.
  Rationale: The guarantee it depends on lives one layer down, in the reference decoder, and is not obvious from the calling code. A test that pins a guarantee someone else provides is worth keeping; a second guard duplicating that check is not. Recording the passing-first-run outcome honestly is preferable to manufacturing a failure to justify a planned edit.
  Date/Author: 2026-08-07, Claude.

- Decision: Leave `rowCount` and `incidenceCount` answering zero for an unsupported plane, rather than making them throw alongside `findLocalReference`.
  Rationale: The same ambiguity exists in principle, but these two feed telemetry and sizing rather than query results, where "nothing stored in that plane" is a defensible answer and no caller sums across planes today. Milestone D1 makes all four planes supported anyway. Changing them now would be churn without a demonstrated failure.
  Date/Author: 2026-08-07, Claude.

- Decision: Ship all four switches defaulting to off, and treat outgoing construction, compiled enumeration, and incoming planes as three separate default decisions rather than one.
  Rationale: The three capabilities have different costs and different evidence. Outgoing construction costs build time and memory; compiled enumeration costs compiler surface and risk; incoming planes cost their own memory again on top of the outgoing side. Bundling them would mean a single benchmark number silently deciding three questions. (This decision was taken before any of the three was measured, and it earned its keep: the three ended up failing their bars for three different reasons.)
  Date/Author: 2026-08-07, Claude.

- Decision: Add a fourth switch, a per-call serving gate, distinct from the build-time construction switch.
  Rationale: A build-time option resolved once during base construction cannot protect a store that is already open. Since the risk this feature carries is wrong answers rather than slowness, there must be a switch that makes every consumer decline immediately without waiting for a rebuild.
  Date/Author: 2026-08-07, Claude.

- Decision: Catch the memory refusal around projection construction only in the base builder, and deliberately not in the consolidator.
  Rationale: In the builder a refusal currently destroys the primary index, which is a strictly worse outcome than having no sidecar. In the consolidator a refusal already falls back to bounded overlay coalescing and keeps the previous base authoritative, which is correct; catching it there would instead silently strip a capability from a base that is already live and serving.
  Date/Author: 2026-08-07, Claude.

- Decision: Give `copyRow` the node as an explicit parameter rather than deriving it from state cached by `find`.
  Rationale: Resolving a predicate to a run handle needs the node, and a row handle cannot carry both a node and a page reference in 64 bits. Caching the node on the view would break exactly the case the plan already flags as dangerous: two nested enumerations share one view, and the inner `find` would overwrite the node the outer `copyRow` still needs. Passing it makes worker and operator confinement structural instead of a matter of discipline, at the cost of one parameter.
  Date/Author: 2026-08-07, Claude.

- Decision: Represent the projection's plane set as an explicit mask carried by the built structure, not as a re-reading of configuration.
  Rationale: A base published before a configuration change keeps the planes it was built with. Deriving `supportsPlane` from current options would let a store that has just been reconfigured claim planes a live structure does not hold, which is the "decline read as empty" failure that milestone A1 closed one layer down. The mask also travels through `wrap` so a rewrite cannot quietly change what a base can answer.
  Date/Author: 2026-08-07, Claude.

- Decision: Split the query-engine surface into two interfaces — a dynamic single-predicate probe and a predicate enumerator — rather than one combined interface.
  Rationale: Resolving one known predicate goes to the primary index and works whether or not the sidecar exists. Folding it into the enumerator interface would make dynamic probing falsely depend on an optional structure, and would let a gap in the sidecar turn into a missing result rather than a decline.
  Date/Author: 2026-08-07, Claude.

- Decision: Make predicate-to-run resolution total, with no skip branch in generated code.
  Rationale: If the projection names a predicate whose primary row is absent, that is structural corruption. Skipping it would convert corruption into a silently incomplete answer. The alternative — failing loudly and degrading the store for subsequent queries — is the behaviour the row-iterator path already implements, and the two paths must not disagree.
  Date/Author: 2026-08-07, Claude.

- Decision: Restrict the compiled path to snapshots with no applicable overlay generations in its first increment.
  Rationale: With a variable predicate the compiler cannot know at bind time which predicates a kernel will touch, so it cannot pre-check overlay applicability the way the fixed-predicate path does. Merging overlay generations inside a random-access lookup is possible but belongs in a later increment; the interpreted row path already covers that case correctly, so the compiled path declines to it.
  Date/Author: 2026-08-07, Claude.

- Decision: Flip F2 — compiled predicate enumeration stays off by default, because the shape it serves gets slower rather than faster.
  Rationale: the flagship cell, which is the one cell whose route the switch actually changes, runs at 1.673 ± 0.144 ms with the switches off and 4.768 ± 0.156 ms with them on. Those intervals are disjoint by a wide margin, so this is a measured 2.85-times regression, not noise. The mechanism is understood and is not a bug to be fixed by tuning. Dumping one node's edges is the shape the subject-ordered disk tree is *best* at: the statements are already contiguous and sorted, so the whole dump is one seek plus a sequential walk. The projection replaces that with one binary search to find the node's predicate row, and then, per predicate, a catalogue lookup to turn a raw predicate id into an ordinal plus a second binary search to find that predicate's run — roughly eleven random in-memory probes for a node that the B-tree serves in one. The transpose is the right structure for a wildcard traversal that cannot go to disk at all; it is the wrong structure for a dump that can.
  Date/Author: 2026-08-08, Claude.

- Decision: Flip F1 — outgoing projection construction stays off by default.
  Rationale: everything the flip bar asks for is green except the one thing that would justify paying for it. Differential parity is clean at all four lifecycle points, the route ledger matches exactly in both configurations, the control cell shows no regression (4.447 ± 0.381 ms off versus 4.277 ± 0.113 ms on, overlapping), refusal is contained and corruption recovers. But the shapes the projection serves either get slower (the flagship, above) or do not move at all: the repeated node dump is 0.285 ± 0.029 ms off and 0.284 ± 0.009 ms on, and the incoming dump is 0.016 ± 0.001 ms in both. Turning construction on by default would spend build time and steady-state memory to make one shape worse and the rest identical. The capability is delivered, tested and one property away; the default is not the place for it yet.
  Date/Author: 2026-08-08, Claude.

- Decision: Flip F3 — incoming planes stay off by default, on measured grounds that are not the grounds this plan predicted.
  Rationale: the plan expected the decision to turn on memory asymmetry, and it does not. Measured over the multi-theme store, the incoming planes add 11,989,608 bytes against the outgoing planes' 12,359,912 — the same size, not the "more than double" this document assumed in four places, for the reason recorded in `Surprises & Discoveries`. The decision instead rests on the return: the incoming cell is 0.016 ± 0.001 ms in both configurations, unchanged, because a lone triple pattern never compiles and the interpreted incoming dump already matches the disk trees on a six-row answer. Nothing measured gets better, and the build gets 63 percentage points longer on top of a build the outgoing planes have already doubled. This is the outcome the plan named in advance as the honest one: incoming remains opt-in, and it stays in the tree because the bidirectional wildcard search this projection exists to enable needs it, not because a benchmark asked for it.
  Date/Author: 2026-08-08, Claude.

- Decision: Give the flagship benchmark cell an explicit `VALUES` list of classes instead of a free class variable.
  Rationale: measured, not assumed. The free form never consults the projection, for the reason recorded in `Surprises & Discoveries`, so as an instrument it was worthless — it would have reported "no change" whatever this work did. Naming two classes keeps the query a genuine schema-profiling shape, keeps the grouping by type and predicate that the milestone asks for, and makes the type lookup the smaller side so a bound node actually reaches the dump.
  Date/Author: 2026-08-08, Claude.

- Decision: Make the route ledger record four facts per cell — the route in the adjacency regime, the route in the Janino regime, whether the projection declines while off, and whether a variable-predicate pattern compiles while on — rather than a single expected route.
  Rationale: two of the five cells compile a kernel in both configurations, so their route does not move and a route-only ledger would have asserted nothing about them. The decline counter and the lowering counter are what actually change, and pinning all four means an unexpected change in any of them fails a test rather than passing unnoticed. It also makes the control cell's claim exact: it must stay on the same route *and* record no declines *and* compile nothing.
  Date/Author: 2026-08-08, Claude.

- Decision: State the milestone C1 fault-injection acceptance as "never a short answer" rather than "the rung always throws", and require the test to prove the fault was detected.
  Rationale: the row-parallel rung declines rather than throwing when the fault is noticed before any page reaches the query thread, and that is the correct behaviour — nothing has been emitted, so re-running from the disk trees is both safe and exact. Demanding an exception would have forced a change that made the system worse. Requiring the inconsistency counter to have moved is what keeps the weaker assertion honest: without it the test would pass on a day the injection stopped landing.
  Date/Author: 2026-08-08, Claude.

- Decision: Inject the C1 fault at the structure through the existing corruption hook rather than through a mock view.
  Rationale: a mock proves the interface contract, which is the easier half. The claim that matters is that the real store implementation, the real generated kernels and the real degradation path hold the contract together, and only a real corrupted projection exercises all three. The hook already existed for the milestone A5 recovery test, so this cost no new production surface.
  Date/Author: 2026-08-08, Claude.

- Decision: Build the randomised update oracle on complete statements and have it count its own non-vacuity.
  Rationale: the subtlety the oracle exists for — deleting one object while another statement keeps the predicate on the node — only appears if the oracle models objects. An oracle over pairs would agree with a buggy implementation for the same wrong reason. The counter is there because agreement is cheap to obtain by accident: across the seeds the test now requires at least one real rewrite and at least one deletion whose predicate survived, so "40 seeds passed" cannot mean "40 seeds did nothing".
  Date/Author: 2026-08-08, Claude.

- Decision: Treat the `?s ?p ?o` grouped-by-subject benchmark cell as a control rather than a target.
  Rationale: All three positions of that pattern are unbound, and the enumerator requires a bound key. A whole-projection root enumerator would be needed to serve it, and the projection deliberately exposes no key-domain accessor. Keeping the cell as a control proves the sidecar causes no regression on shapes it does not serve.
  Date/Author: 2026-08-07, Claude.

## Outcomes & Retrospective

Written at completion, 2026-08-08, against the three claims in the Purpose section.

The first claim holds. The sidecar is safe. A memory refusal now costs the projection and nothing else, where before it destroyed the entire adjacency index for the life of the process with no retry; there are four switches, all off by default, one of which stops a store that is already running; the two memory kinds appear in both lifecycle log lines and in the metrics snapshot, reporting the charged figure rather than the structure's self-report, because the charge is what consumes the cap; a differential suite compares the served multiset against the disk trees at four lifecycle points over a deliberately built fixture, and four further tests cover what that suite cannot see — a pinned view across a fold-down, release-after-last-lease, two workers with independent cursors, and reads taken inside a write transaction through both of its endings. A corrupted projection degrades this one capability, schedules a rebuild, and recovers, which is asserted as a five-step sequence rather than as five independent facts.

The third claim holds. The reverse direction exists, is plane-generic end to end, is covered by the parity suite including an inlined literal object, and costs nothing measurable when its switch is off. The `?s ?p <o>` cell stops declining the moment the switch is on: `incomingEdgeDump` goes from `np:decl=1, lmdb only` to `np:decl=0, adjacency`.

The second claim holds structurally and fails on its headline number, and that is the honest summary of this work. The compiler really can see variable predicates now: a pattern with a bound endpoint and a variable predicate lowers to one of two new IR nodes, compiles to real generated Java that reads the node's predicate row in bounded chunks with per-node scratch, binds all-or-nothing per direction, and runs in both parallel rungs; the binding truth table agrees with the ordinary evaluator on all fifteen shapes including every aliasing case; and the total-resolution contract holds under fault injection in all three rungs. What does not hold is the sentence in the Purpose section that says the flagship cell's "runtime drops below both interpreted tiers with non-overlapping error bars". It rises: 1.673 ± 0.144 ms becomes 4.768 ± 0.156 ms, a 2.85-times regression with disjoint intervals. All three switches therefore ship off, and this document records why rather than leaving a designed, tested and lowered capability silently disabled.

The reason is worth stating precisely, because it is a property of the problem rather than a defect to be tuned away. A node's outgoing statements are already contiguous and sorted in the subject-ordered disk tree, so dumping them is one seek and a sequential walk — the single shape a B-tree is best at. The projection replaces that with a binary search for the node's predicate row and then, per predicate, a catalogue lookup to turn a raw id into an ordinal plus a second binary search for that predicate's run: roughly eleven random in-memory probes where the tree does one sequential one. On top of that, building it doubles the base build (800.785 ms to 1616.311 ms over the multi-theme store, and 2124.994 ms with the incoming planes), because it runs on the build thread after the parallel primary build has finished — the same serial-tail shape a previous milestone spent its whole length eliminating.

What this means for the next contributor. The projection is not a faster way to do what LMDB already does well; it is the only way to do something LMDB cannot do at all, which is wildcard traversal that never touches disk. The design document this plan inlines asks for exactly that: expanding a frontier without a predicate in hand, where the alternative is trying every predicate in the store at every node reached. That consumer does not exist yet, so the primitive currently has no workload that plays to it, which is why every measured cell either regresses or is unchanged. Two concrete follow-ups would change the arithmetic. First, the per-predicate run resolution is the whole cost of the read path and is pure indirection: if a row could carry its runs alongside its predicates the dump would become one pass, but that means storing pointers into the main index, which the current design deliberately refuses because a pointer goes stale on every base rewrite — so it needs a generation-stamped handle rather than a raw one. Second, construction should move into the parallel build rather than trailing it, which would remove most of the doubled build time without changing anything about correctness.

Three smaller lessons, all of them paid for. A benchmark cell is only an instrument if it *asks the question* and is *large enough for the answer to matter*: the pre-existing `nodeEdgeDump` cell had the first property and not the second, and the first draft of the flagship had the second and not the first, and both reported "no difference" for opposite reasons. A route ledger has to record more than a route, because two of the five cells compile in both configurations and only their decline counters move. And an acceptance written as "the rung must fail" was wrong: declining before anything has been emitted is the better behaviour, so the acceptance became "never a short answer", with a detection counter to keep the weaker claim honest.

## Context and Orientation

Everything in this plan lives in the module at `core/sail/lmdb`, whose main sources are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/` and whose tests are under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/`. Paths below are given in full from the repository root the first time each file appears.

### Terms used in this plan

A *statement* is a subject, predicate, object, and optional context. Internally each of those four is an opaque 64-bit identifier, and this plan never needs to convert them back to human-readable form.

A *plane* is one of four fixed directions the adjacency mirror stores, numbered by constants in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyPlane.java`: 0 is outgoing explicit, 1 is incoming explicit, 2 is outgoing inferred, 3 is incoming inferred. "Outgoing" means keyed by subject; "incoming" means keyed by object. "Inferred" means the statement was derived by a reasoner rather than asserted directly.

A *run* is the sorted sequence of neighbours belonging to one key under one predicate in one plane — for example, all objects of subject `S` under predicate `P`. Runs are encoded in native memory and read through `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyRunCodec.java`. Crucially, a run position holds a neighbour *and* a context, so one object appearing in two named graphs occupies two positions.

A *CSF*, or compressed sparse fiber, is the layout of the main adjacency mirror, implemented in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/ImmutablePagedQuadCsfIndex.java`. It is addressed as predicate, then plane, then key, then run. That ordering is why a query with no predicate has nothing to look up.

The *projection*, which this plan is about, is the sidecar at `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSubjectPredicateIndex.java`. It is itself a CSF with exactly one synthetic predicate, where the row coordinate is a node identifier and the neighbour coordinate is a raw predicate identifier. It stores no objects, no contexts, and no pointers into the main index. A lookup therefore reads the small predicate list and then resolves each predicate through the main index normally. It is built without re-reading anything from disk, by merging the key lists that the main index already holds per predicate.

An *overlay generation* is a batch of changes from committed transactions that has not yet been folded into the base. *Consolidation*, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbPagedCsfConsolidator.java`, folds them in by producing a new base that shares unchanged pages with the old one.

A *lease* is a reference count that keeps a published base alive while a query reads it. Native memory is freed only when the last lease is released, so holding a pointer past its lease is a process crash rather than an exception.

A *kernel* is Java source generated at query time and compiled in memory by the Janino compiler, replacing interpreted operator dispatch with one fused loop. *Lowering* is the step that decides whether a query pattern can be expressed as kernel operations at all; when it cannot, it records a reason and the query runs interpreted instead.

### What exists right now

The projection is new and not yet committed to version control, along with its companion `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSubjectPredicateUpdates.java` and two test classes. Five committed files carry uncommitted modifications that wire it in: the base builder at `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbPagedCsfBaseBuilder.java`, the consolidator, the owning index at `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbInMemoryAdjacencyIndex.java`, the store facade at `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyStore.java`, and the consumer at `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectNodeIterator.java`.

That consumer is the only query-side use. It is reached from a single branch of the store's `open` method, the branch handling an unbound predicate with exactly one of subject or object bound. Everything else in the engine still cannot use the projection.

The compiled query path cannot use it because of a shape mismatch. The interface the compiler binds against, `NativeAdjacency` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java`, is fixed to one predicate at bind time. Consequently every adjacency branch in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelLowering.java` reads `pattern.p.constant` and a variable predicate falls through to a decline recorded as `pattern-shape`. Because one builder class serves both the row-producing and the aggregate rungs, that single decline removes variable-predicate patterns from compiled execution and from parallel execution simultaneously.

### Invariants inherited from the checked-in adjacency plan

`plans/lmdb-native-engine/27-in-memory-direct-adjacency.md` is checked in and is incorporated here by reference. Five of its invariants govern this work and are restated in one line each so this plan remains usable alone. I2, LMDB authority: anything missing, stale, incomplete, over budget, or disabled must fall back to the disk trees rather than answer approximately. I3, exact snapshot: a read stamped at a revision uses only a base and row versions valid for that revision. I5, commit window safety: rows affected by a commit are marked pending before that commit becomes visible. I14, lease before free: native memory is released only after it is unreachable from the active index and every lease on it has ended. I17, hard accounting: every native allocation is charged to the memory account before it is made.

### Requirement inlined from an uncommitted design document

There is a design-only document at `plans/lmdb-native-engine/33-native-graph-search.md` describing graph search over adjacency. It is not checked in, so its relevant content is restated here in full rather than referenced, as `.agent/PLANS.md` requires.

That document observes that the main index is addressed predicate-first, which suits a fixed predicate but not a wildcard: trying every predicate at every expanded node costs time proportional to the store-wide predicate count multiplied by the number of nodes reached, plus the edges reached, which it judges unacceptable on datasets with many predicates. It notes that the older, now-replaced adjacency representation contained the missing transpose — a directory from node and plane to sorted predicate-and-run groups — and that the compact representation deliberately dropped it. Its conclusion is that efficient wildcard graph search requires restoring a compact node-to-predicate-group directory, without copying any neighbour or context data. Its summary paragraph asks for "a node-to-predicate-group transpose for wildcard/large sets", used in both directions, and states that performance claims require matched benchmark runs before they are believed.

The projection this plan hardens and extends is exactly that transpose. Milestone D1's incoming planes are what its bidirectional search would need. This plan does not implement graph search; it delivers the primitive that search would later consume.

## Plan of Work

The work divides into four gates. Gate A makes the existing sidecar safe and honest and does not add capability. Gate B completes the interpreted row path. Gate C teaches the compiler about variable predicates, which is the largest win. Gate D adds the reverse direction. Three separate flip decisions sit between them, because three separate capabilities are being enabled.

Every milestone is committed separately with a file-scoped commit, so each is independently reviewable and can be reverted without disturbing its neighbours. No commit uses `git add -A`, because roughly 400 untracked artifacts sit at the repository root and must be preserved.

### Gate A, milestone A1: rename and close the API contract gaps

At the end of this milestone the vocabulary matches what the structure actually is, and three lookup methods refuse invalid input instead of returning a value that cannot be distinguished from a valid one.

The rename covers `LmdbSubjectPredicateIndex` to `LmdbNodePredicateIndex`, `LmdbSubjectPredicateUpdates` to `LmdbNodePredicateUpdates`, their two test classes to match, the accessor `LmdbInMemoryAdjacencyIndex.subjectPredicateIndex()` to `nodePredicateIndex()`, and every field and local variable spelled `subjectPredicate`. A new nullable accessor `nodePredicateIndexOrNull()` is added alongside the throwing one.

Three contract gaps close. In the index, `findLocalReference` currently returns zero both when the plane is unsupported and when the row is genuinely absent, so a caller who forgets to check the plane first silently reads an empty result as a real one; it must throw `IllegalArgumentException` for an unsupported plane. Also in the index, `resolve` accepts any reference without validation and forwards it straight into native memory; it must reject a reference of zero or less. Also in the index, `rewrite` passes a hard-coded predicate count of one where the value is available from the index itself; this is behaviour-neutral because the constructor already enforces exactly one synthetic predicate, but the literal is unreadable next to the underlying method's own bounds check.

One caller changes as a consequence. The consolidator currently asks `supportsPredicateEnumeration(0)` purely as a way of testing whether a projection exists, which is misleading and would throw for a legacy-format base; it uses the new nullable accessor instead.

Finally the accessor gains a comment stating that the reference it returns is borrowed and non-owning: only the owning index may close the projection. This matters because the consolidator's no-change fast path makes two live bases share one underlying structure through reference counting, so a consumer that closed what it merely read would free memory another base is still using.

This milestone is a behaviour-neutral refactor plus three narrow input-validation changes. The rename follows Routine B, with hit proof supplied by the three existing test classes that construct the type directly; the validation changes follow Routine A, each opened by a test that fails against current behaviour.

### Gate A, milestone A2: contain build failure, and introduce four switches

At the end of this milestone a store whose memory budget cannot accommodate the sidecar comes up healthy without it, and every new capability is off by default.

The containment change is one `catch` clause in the base builder around projection construction only: log a warning, leave the field unset, and continue building the primary index. The projection's own build already releases every partial memory charge before it throws, which an existing test proves, so the account is clean at the point the exception is caught. The consolidator is deliberately left alone, for the reason recorded in the Decision Log.

Four switches appear, all defaulting to off. Three are named in the existing direct-adjacency property family: `rdf4j.lmdb.directAdjacency.nodePredicateProjection.enabled` gates construction of the outgoing projection, `rdf4j.lmdb.directAdjacency.nodePredicateProjection.incoming.enabled` gates the incoming planes that arrive in Gate D, and `rdf4j.lmdb.directAdjacency.nodePredicateProjection.serve.enabled` gates every consumer. The fourth, `rdf4j.lmdb.janinoCodegen.nodePredicates`, gates compiled enumeration and arrives in Gate C.

The two build-time switches are resolved once in `LmdbDirectAdjacencyOptions`, which already exposes a pure resolution method taking an explicit properties object and is therefore directly testable. This deviates from the surrounding family, whose members re-read the system property on every call; doing that here would allow one store to hold some bases with the projection and some without, which is safe but makes an A/B measurement irreproducible. The serving switch, by contrast, is re-read per call precisely because it must be able to stop a store that is already running.

Because construction now defaults to off, the handful of existing tests that assert the new serving behaviour need the switch enabled. That belongs in the shared store-opening helper of the query test class, not repeated in each test.

Acceptance needs care, because the obvious phrasing is wrong. After a projection refusal the memory account does not return to its pre-build total: the primary index is still allocated and still charged. The correct statements are that the charges after a refusal equal the charges from an equivalent build with construction disabled, that the projection-specific charge is zero, and that closing the returned base returns the total to the pre-build baseline. Alongside those, the store must reach its active state rather than the refused state, and the base must report that it uses the paged format but does not support predicate enumeration.

This milestone also adds a failure-injection test on the consolidator, covering the case where the primary rewrite allocates successfully and the projection rewrite then refuses. It must show that the unpublishable primary rewrite is released, the previous base remains authoritative, bounded overlay coalescing runs instead, and no capability is silently lost.

### Gate A, milestone A3: separate accounting and rewrite observability

At the end of this milestone the sidecar's memory cost appears as its own line in the two lifecycle log messages and in the metrics snapshot, and its rewrites are visible.

Two new memory kinds are added rather than one, because the enum is flat and has no native-versus-heap axis: one for the projection's native bytes, replacing its current charge against the primary index's kind, and one for its modelled Java bytes, replacing its charge against the shared metadata kind. Today both charges land in the same kinds as the primary index, so the projection is invisible in the per-kind breakdown that both lifecycle messages already print.

The consolidator needs a second budget object, because the single budget it currently creates is shared between the primary rewrite and the projection rewrite. The critical constraint is that the overall cap stays global: two budgets must draw on one account with one limit, never two independent full allowances. A test enforces this by sizing a combined rewrite that exceeds the cap while each half alone would fit, and observing a refusal.

Two smaller additions complete the picture. The metrics snapshot gains the two byte figures, exposed to the benchmark package through the existing test-access class. And the consolidator's result gains the projection's rewrite record, which is currently created, has its index adopted, and is then dropped — so the projection's shared and replaced page counts are unobservable while the primary's are logged.

The workspace constant that was raised from 72 to 160 bytes per source row during the original wiring gains a comment deriving that figure from the three arrays involved. The test pins the accounting formula and the cap behaviour, not a measured live object size: per-field byte figures are assumptions about JVM layout rather than stable facts.

### Gate A, milestone A4: differential parity, lifetime, and transactions

At the end of this milestone there is a test that proves the answers served from the sidecar are identical to the answers served from the authoritative disk trees, across the states the structure actually passes through.

The claim being discharged, stated precisely: for every node, context, and explicit-or-inferred selection, restricted to the outgoing planes, the multiset of statements produced by the node iterator equals the multiset produced by the disk trees for the same pattern. The restriction to outgoing planes is deliberate — incoming behaviour does not exist until Gate D, so a claim covering all planes could not be discharged here and must not be written as though it were.

The test compares at four lifecycle points, because the structure is materially different at each: immediately after the base is built; with live overlay generations present, which exercises the merge inside the iterator; after a forced fold-down, which exercises the rewrite; and after a fold-down whose changes are all object or context changes under predicates that were already present, which exercises the no-change fast path that shares the previous structure instead of rewriting it.

The fixture must be built deliberately rather than randomly, because several branches are reachable only from specific shapes. It needs a node with exactly one predicate, which is the only way to exercise an optimisation in the iterator that reuses an otherwise-idle two-element buffer instead of allocating. It needs a node with more than 256 predicates, because 256 is the chunk size and this is the only way to exercise chunk refill and buffer growth. It needs inferred statements so the inferred plane is genuinely populated, named-graph and absent contexts, an inlined literal object, a node that exists only in an overlay, a node whose only predicate has been tombstoned, and a predicate first seen after the base was built. Every assertion prints the seed, following the convention of the existing differential fuzz test.

Four lifetime and transaction tests accompany it, covering ground the four lifecycle points do not. One pins an old view, publishes a consolidated new base, and keeps reading the old view while a new view reads the new one, which is the invariant about references being valid only against the root they came from. One proves the base is closed only after both leases are released. One runs two parallel workers against the same logical request and proves their cursor state does not interfere. One queries inside a write transaction after an uncommitted addition and an uncommitted deletion, and requires the path either to honour read-your-own-writes exactly or to decline to the authoritative path; it covers rollback as well as commit.

Finally the update companion's test class gains a randomised oracle. The important subtlety is that the oracle must apply changes to a set of complete statements and only then project to node-and-predicate pairs. Applying additions and deletions directly to a set of pairs would mishandle deleting one object when another statement still keeps that predicate present on that node — which is precisely the condition the production code's incidence check exists to detect. Ordering is unsigned throughout, and the identifiers used include values around zero, one, negative one, and both extremes of the signed range, since unsigned comparison of those is where sign-handling bugs live.

### Gate A, milestone A5: shadow mode, corruption recovery, and baseline truth

At the end of this milestone the store can run in a mode that checks every answer for this shape against the disk trees on the fly, a corrupted projection degrades and then recovers instead of failing forever, and the test baseline states what is actually true.

Shadow mode already exists as a whole-store mode with a working comparator that logs, records a mismatch metric, and degrades on disagreement. It is simply not wired for this shape: the branch serving it observes the mode and returns without comparing. Two edits connect it — construct the node iterator rather than the fixed-predicate iterator, and ask the disk trees for the unbound-predicate pattern. One honest limitation is recorded rather than hidden: the comparator returns early when there is no transaction, which is the parallel-worker path, so a clean shadow run does not cover parallel workers.

The corruption path needs more than a plausible gesture. When the projection names a predicate whose primary row is absent, the iterator throws mid-stream. Keeping the throw is right, because rows have already been emitted and re-running from the disk trees would duplicate them. But the follow-up must be a dedicated projection-inconsistency signal rather than reusing the revision-gap mechanism, and it must be proven to recover. The test asserts five things in sequence: the offending query fails; subsequent queries decline adjacency instead of throwing; a rebuild is scheduled; after a successful rebuild adjacency serves again; and the mismatch metric does not increment once per subsequent fallback query. Without the third and fourth of those, this would reintroduce exactly the permanent-degradation failure that milestone A2 removes.

The stale baseline is corrected here: the census stops removing the node-dump cell from its served set, and the class comment claiming the shape cannot be answered is rewritten. A superseded note is added to the survey document that originally proposed this sidecar.

### Gate A, milestone A6: benchmark cells and a route ledger

At the end of this milestone there are benchmark cells with enough headroom to show a difference, and a per-cell record of which execution route each shape takes.

Five cells are added to the three-regime corpus and to the adjacency shape benchmark, on the existing generated datasets rather than the multi-gigabyte theme fixture, so they are cheap enough to run in a loop. The flagship is a class-and-predicate matrix, which is a type lookup joined to an unbound-predicate dump of each matching instance, grouped by type and predicate. A repeated bound-node dump and a variable-predicate join follow it, plus an incoming dump that will not serve until Gate D.

The fifth cell, an out-degree histogram over completely unbound patterns, is a control rather than a target, and this is worth stating plainly because the earlier draft of this work had it backwards. All three positions of that pattern are unbound; the enumerator being built in Gate C requires a bound key; and a whole-projection root enumerator is out of scope because the projection exposes no key-domain accessor. The cell therefore proves the sidecar causes no regression on shapes it does not serve, and its lowering counter must stay at zero.

The acceptance is not a blanket claim that every new cell is red. That would be wrong, because bound-node cells may already engage the interpreted row path once construction is switched on. Instead the census asserts a per-cell ledger of the expected route and the expected decline reason at each stage, so a route changing unexpectedly fails a test rather than passing unnoticed.

One retargeting warning belongs here. It is tempting to aim this work at the analytics theme queries numbered one, two, and eleven. Reading the query catalogue shows that is wrong: the first has a constant predicate, the second is a whole-store distinct-predicate count answered by the predicate catalogue rather than any per-node structure, and only the inner existence check of the eleventh is a projection shape. Queries seven, eight, and nine are the real targets.

### Gate B, milestone B1: eligibility-gated shortcuts and predicate ordering

At the end of this milestone the interpreted path can answer several questions without materialising any statements, but only where doing so is provably exact.

The tempting claim is that a distinct-predicate count is the row length, an existence check is whether the row is non-empty, and a statement count is the sum of run lengths. Each is true only under conditions that must be stated, because the projection row records that a predicate exists *somewhere* on a node, with no memory of which context or which object it came from. Restrict the query to one named graph and the correct answer can be strictly smaller than the row length, since a predicate may exist only outside that graph.

So each shortcut carries preconditions. The distinct-predicate count equals the row length only when the object is unbound, the context is unrestricted, one plane is precisely identified, and the state is the exact merge of base and every applicable overlay. The existence check has the same preconditions, and any filtered form needs a separate primitive that respects the filters rather than a reinterpretation of this one. The statement count equals the sum of run lengths when there is no object binding and no context restriction; multiplicity across named graphs is safe here, and specifically because the run encoding already gives each object-and-context pair its own position, so no correction is needed. Outside these conditions the code iterates or declines.

Live overlay generations are the sharpest of these traps, because a base row length becomes wrong the moment an overlay adds a predicate or removes the last statement under one. Any count primitive must perform the same base-plus-generation merge the iterator performs, or refuse. Tests deliberately construct cases where the whole-row count differs from the context-filtered count, and where it differs from the post-overlay count.

Separately, predicate-ordered streaming becomes available. The projection orders rows by unsigned key and then unsigned predicate, and the iterator picks the smallest predicate across base and generations, so the stream is already in predicate order for a bound subject. The blanket refusal of ordered requests for this branch is narrowed to permit predicate ordering only. Object ordering must stay refused for the subject-bound case, because the stream is predicate-major and object order holds only within one predicate group. One calibration: the planner's advertised order support comes from the disk-tree layer, not from this store, so this removes a runtime refusal rather than changing what the planner believes.

Distinct-predicate listing and predicate histograms are explicitly not routed here; they are already served far faster by an existing prefix-run path.

### Flip F1

The decision to make outgoing construction default-on requires all of: no differential mismatches from milestone A4 or from a shadow run; the route ledger from A6 matching exactly; no regression across the whole affected benchmark corpus rather than only the new cells; an accepted build-time cost; an accepted steady-state memory percentage; and demonstrated refusal and corruption recovery.

The build-time cost deserves explicit measurement rather than assumption. Projection construction performs two complete merges across every predicate's key list, on the build thread, after the parallel primary build has finished — which is exactly the shape of serial tail that a recent piece of work spent an entire milestone eliminating. The build-only benchmark measures it with the switch on and off, through a `nodePredicates` parameter added to it for this purpose.

Outcome: outgoing construction stays off. Every prerequisite on that list is met except the one that would pay for the rest. Parity is clean, the route ledger matches in both configurations, refusal and corruption recovery are demonstrated, and the control cell shows no regression. But the build-time cost is not "accepted": construction more than doubles the whole base build, 800.785 ms to 1616.311 ms over the multi-theme store, because it is single-threaded work as large as the parallel build it follows. And the shapes it serves either regress or stand still. The full numbers and the mechanism are in `Artifacts and Notes` and `Outcomes & Retrospective`.

### Gate C, milestone C1: two interfaces and their store implementations

At the end of this milestone the query engine has a way to ask both "which predicates does this node have" and "give me the run for this node and this predicate", with a contract that makes a missing run impossible rather than skippable.

The run-reading methods currently duplicated on the fixed-predicate interface are first extracted into a shared read-only run view, which the property-path code can also use. Then two interfaces extend it. A dynamic adjacency interface offers a single method resolving a node and a raw predicate to a run; it is backed by the primary index and therefore works whether or not the sidecar exists or was refused. A node-predicates interface offers a row lookup, a row length, and a bulk row copy that returns predicates and their run handles together; it requires the sidecar.

The total-resolution contract is the correctness heart of this milestone. Every predicate returned by the bulk copy must carry a valid run handle. The copy validates each pair before returning and raises a dedicated inconsistency signal otherwise, routed to the same degradation path built in milestone A5. There is no such thing as an enumerated predicate with a missing run: that combination is corruption, never an ordinary miss. The generated code in the next milestone therefore contains no branch that skips such a predicate.

Eligibility reuses the existing completeness rule for compiled access, tightened to require zero applicable overlay generations, for the reason in the Decision Log. Ownership is explicit: a view belongs to the probe that produced it, is valid only until that probe closes, is never cached in a plan or a reusable kernel object, and is closed along with its siblings if a later binding request causes the kernel to decline. A fresh instance is allocated per request, because the cursors inside are mutable and each parallel worker creates its own probe, which makes worker confinement structural rather than a matter of discipline.

Acceptance includes a fault-injection test, which is what distinguishes a contract that is written down from one that holds. A test double returns one enumerated predicate with an invalid run handle, and the scalar kernel, the row-parallel kernel, and the aggregate-parallel kernel must each fail rather than emit an incomplete result.

### Gate C, milestone C2: compiler nodes and generated code

At the end of this milestone the compiler's intermediate representation can express predicate enumeration and dynamic probing, and can emit correct Java for both.

Two nodes are added beside the existing fixed-predicate probe. An enumerate-predicates node carries a view index, a key operand, a destination column for the predicate, a destination column for the far endpoint, and the context handling that every adjacency node already carries. A probe-variable node is the same without the enumeration, taking a predicate operand instead. Each contributes a distinct textual key, which is how generated kernels are cached and must not collide.

The generated code reads the row in bounded chunks rather than allocating an array the size of the whole row. That matters for three reasons: it preserves the chunking discipline that the large-node fixture in milestone A4 exists to exercise, it prevents a pathological node from forcing a large uncharged allocation inside every parallel worker, and it avoids narrowing a 64-bit row length to a 32-bit array size on an invariant nobody has stated. The inner loop over each run is byte-for-byte the existing fixed-predicate inner loop.

The scratch buffers are per node, not per view. Two nested enumerations in one pipeline share a view, and sharing buffers would let the inner one overwrite the predicates the outer one has not yet processed. A nested-operator test covers this directly.

Two things are deliberately left out of this increment. The vectorised tail is not wired, because the existing probe path needed a full increment to get its selection and vector alignment right and the predicate column adds a loop level the resumable state machine would have to checkpoint; both new nodes therefore report themselves as not resumable. And no root-level whole-projection enumerator is added, for the reason given under milestone A6.

### Gate C, milestone C3: lowering, bindings, and parallel rungs

At the end of this milestone a SPARQL pattern with a variable predicate and a bound endpoint compiles, and runs in parallel.

The lowering dispatch is inserted at the point where the context operands have been computed and before the domain-enumeration branches, falling through to a scan attempt and then to a stable decline reason. Patterns where the predicate term both matches a constant and binds a variable continue to be refused, because that combination's meaning lives in the pattern plan rather than in the adjacency layer.

What this milestone must get right is not the four obvious shapes but the full space of variable bindings and aliases. Every combination of fresh, supplied-through-the-initial-bindings, already-assigned-to-a-column, and aliased-to-another-position must either lower with the equality filters that make it correct, or decline with a stable reason. None may take the "fresh variable" branch and overwrite a binding that already exists. The shapes that must each be explicitly decided include a pattern whose predicate and object are the same variable, whose subject and predicate are the same variable, whose subject and object are the same variable, and one whose graph variable is also its predicate variable; a predicate variable arriving through the initial binding set rather than a column; a predicate bound at runtime to a literal or a blank node; an unbound-endpoint sentinel inside a partially bound row; repeated variables downstream of the new operator; and the explicit-versus-inferred selection. This is tested as a generated truth table compared against the ordinary evaluator, not as a handful of chosen cases.

The binding layer gains a request type per interface, following the existing pattern, with no partial variant: unlike per-predicate views, which can be individually missing, these are all-or-nothing per direction, so a missing one declines the whole open. Four call sites thread the new context arrays through: the sequential row path, the mixed-binding re-lowering path, and the two parallel rungs.

The parallel rungs need almost nothing, and the reason is worth recording. Partition roots are chosen by matching the type of the first pipeline node against three enumerating node types. An enumerate-predicates node requires a bound key, so it can never be first; the flagship pipeline still partitions on the type-lookup key window exactly as it does today, and the windowing view narrows only key enumeration, which the new node does not use. Only worker binding changes. One pessimism is accepted knowingly: a pipeline that *starts* with predicate enumeration, from a correlated outer row, runs sequentially.

### Flip F2

Engagement of the flagship cell with non-overlapping error bars is necessary but not sufficient. Independent forks are used and confidence intervals are reported on the ratio between the enabled and disabled regimes rather than on either alone. The full bar adds: no differential mismatches; the route ledger matching for every cell; no regression across the whole affected corpus including the control cell; accepted build-time and memory costs; demonstrated refusal and corruption recovery; and no new declines for the shapes actually implemented.

Outcome: compiled predicate enumeration stays off, and it fails at the first hurdle rather than at any of the added ones. The flagship's intervals are indeed non-overlapping — in the wrong direction, 1.673 ± 0.144 ms becoming 4.768 ± 0.156 ms. Everything else on the bar is green, which is worth saying plainly: the shape compiles, agrees with the interpreter on all fifteen binding combinations, engages in both parallel rungs, declines nothing it used to serve, and leaves the control cell untouched. It is simply slower, for a structural reason set out in `Outcomes & Retrospective`.

### Gate D, milestone D1: incoming planes

At the end of this milestone the projection can cover incoming planes, at no cost whatsoever when its switch is off.

The plane predicate and the emit loop generalise to all four planes, and the update companion's outgoing filter and two-way partition split become a four-way stable partition in the order the underlying rewrite expects.

The zero-cost requirement is stronger than not publishing the result: with the switch off, incoming update capture, partition arrays, rewrite work, and temporary workspace must all be skipped, proven by a test asserting no incoming charge and no incoming workspace allocation.

The parity suite from milestone A4 extends to incoming planes before any incoming consumption is switched on. One existing test is expected to change rather than pass: a case pinning the decline reason for an inlined object will shift from incomplete-enumeration to a different reason, because paged bases build no inline incoming structures and the serving branch requires a reference-typed key. The pin is rewritten to the new contract rather than deleted.

### Flip F3

The incoming default is decided separately, because the row domain for incoming planes is distinct objects including referenced literals rather than distinct subjects. It requires incoming parity green, the incoming cells engaging, and an accepted memory percentage on the theme fixture.

Outcome: incoming stays off. Parity is green and the incoming cell does stop declining, but it does not get faster — 0.016 ± 0.001 ms with the switch off and on — and the planes add 11,989,608 bytes and 63 percentage points of build time. The memory prediction that motivated separating this decision turned out to be wrong in the projection's favour and made no difference to the verdict; see `Surprises & Discoveries`. This is the honest outcome the paragraph above anticipated: incoming remains opt-in, recorded here rather than left as a designed, tested and lowered capability that is silently disabled with no explanation.

## Concrete Steps

All commands run from the repository root, `/Users/havardottestad/Documents/Programming/rdf4j`.

Before any work in a fresh session, publish every module to the workspace-local Maven repository, because test runs do not build upstream modules and would otherwise link against stale artifacts:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Expect a reactor summary ending in `BUILD SUCCESS`. The most recent run of this command took 34.7 seconds.

Run one test method, which is the tightest loop and the default while iterating:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateIndexTest#unsupportedPlaneLookupIsRejected

Run one test class:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateEnumerationParityTest

Run the whole module, which is the final gate and takes several minutes:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Two prohibitions apply to every test command in this repository and are worth repeating because violating either silently produces meaningless results: never pass `-am`, which would rebuild upstream modules and mask staleness, and never pass `-q`, which would suppress the report output that constitutes evidence.

Before any commit, check headers and then format:

    cd scripts && ./checkCopyrightPresent.sh

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

Every new Java file begins with the standard Eclipse Distribution License header carrying the year 2026, immediately followed by the line `// Some portions generated by Claude`.

Commits are file-scoped and prefixed `GH-0000`, because no GitHub issue number is associated with this branch. Never use `git add -A`; roughly 400 untracked artifacts live at the repository root and must be preserved.

The nine focused suites that cover this work, in the order they were written. Each takes under a second of test time; the twenty-odd seconds each command reports is the root install that precedes it:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateIndexTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateSwitchTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateUpdatesTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateEnumerationParityTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateLifetimeTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateRecoveryTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateShortcutTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeNodePredicateKernelTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNodePredicateKernelFaultTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeVariablePredicateKernelTest

The census, which is where the route ledger lives and which runs both configurations:

    python3 .codex/skills/mvnf/scripts/mvnf.py ThreeTierEngagementCensusTest --retain-logs

Expect `tests=4, failures=0, errors=0, skipped=1`; the skip is the opt-in theme half. The per-cell table it prints to standard output is captured in the Surefire report and is the ledger's readable form.

The flip evidence, one JMH run per cell per configuration in the Janino regime. The first invocation packages the benchmark jar, so the rest pass `--no-build`:

    ./scripts/run-single-benchmark.sh --module core/sail/lmdb \
        --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThreeTierParityBenchmark \
        --method 'classPredicateMatrix$' --param regime=janino \
        --warmup-iterations 3 --measurement-iterations 5 --forks 1 \
        --jvm-arg -Drdf4j.lmdb.threeTierParity.foafPeople=2000

For the enabled configuration, add the four switches as JVM arguments:

    --jvm-arg -Drdf4j.lmdb.directAdjacency.nodePredicateProjection.enabled=true
    --jvm-arg -Drdf4j.lmdb.directAdjacency.nodePredicateProjection.incoming.enabled=true
    --jvm-arg -Drdf4j.lmdb.directAdjacency.nodePredicateProjection.serve.enabled=true
    --jvm-arg -Drdf4j.lmdb.janinoCodegen.nodePredicates=true

The build-time and memory cost, over the shared multi-theme store, comes from the build-only benchmark's new `nodePredicates` parameter:

    ./scripts/run-single-benchmark.sh --module core/sail/lmdb \
        --class org.eclipse.rdf4j.sail.lmdb.LmdbAdjacencyBuildBenchmark --method 'build$' \
        --param nodePredicates=DISABLED --param buildThreads=auto --param coverage=FULL

Each trial additionally prints a line beginning `### adjacency build cost:` carrying the projection's native and Java bytes and its share of the total charge, which is the figure the memory half of the flip decisions turns on.

## Validation and Acceptance

Acceptance is phrased below as behaviour an operator or reviewer can observe, not as internal structure.

For milestone A1, running the three test classes that construct the projection passes both before and after the rename, and searching the tree for the old type name returns nothing. The two new validation tests fail before the change with a returned zero and an unchecked native read respectively, and pass after with the documented exceptions.

For milestone A2, a store configured with a memory budget large enough for the primary index but too small for the sidecar starts successfully, reports the paged format, reports that predicate enumeration is unsupported, and serves the unbound-predicate shape from the disk trees. Before the change the same configuration leaves the store with no adjacency index at all and no retry. Separately, setting the construction switch to false and re-running the query test produces a byte-identical result set with the incomplete-enumeration fallback counter incrementing.

For milestone A3, the two lifecycle log lines contain a distinct entry for the projection's native and Java bytes, and the fold-down log line reports the projection's shared and replaced page counts. A rewrite whose primary and projection halves each fit the cap individually but not together is refused.

For milestone A4, the parity test passes at all four lifecycle points with every assertion naming its seed. Running it before milestone A2's containment fix is not meaningful, so it is ordered after. The most likely genuine failures on first run are the node with more than 256 predicates and the node that exists only in an overlay; if both pass immediately that is recorded in `Surprises & Discoveries` rather than a failure being manufactured.

For milestone A5, a store in shadow mode with sampling set to every request increments its comparison counter for the unbound-predicate shape, which stays at zero today, and records no mismatches across the query fixture. The corruption test observes the five-step recovery sequence in order.

For milestone A6, the benchmark run produces the five new cells with error bars, and the census asserts each cell's route and decline reason. The baseline is copied into `benchmark-results/` so later comparisons have something to compare against.

For milestone B1, a query restricted to one named graph over a node whose predicates are spread across two graphs returns the smaller, correct count, where a naive row-length shortcut would return the larger one. An ordered request by predicate is served rather than declined; an ordered request by object for the same shape is still declined.

For milestones C1 through C3, the witness is the flagship cell's *variable-predicate lowering* count, not its kernel-open count. That is a correction to this section rather than a restatement of it, and the reason is measured: the flagship's `VALUES`-driven type lookup already compiles to a kernel with every switch off, so its kernel-open count is one in both configurations and could never have moved from zero. What does move from zero to non-zero is the number of patterns with a variable predicate that reached a compiled enumeration, which is the thing this gate is actually about. The fault-injection test causes all three kernel rungs to either fail or decline with the disk trees answering in full, and never to return a short result; it additionally asserts the injected fault was detected, so it cannot pass by never reaching it. The binding truth table agrees with the ordinary evaluator on every combination.

For milestone D1, the incoming dump cell stops declining, the parity suite passes for incoming planes, and with the incoming switch off the incoming memory charge is zero.

For milestone A6, the census prints a per-cell table for both configurations and asserts a four-part ledger row for each of the five new cells; the run reports `tests=4, failures=0, errors=0, skipped=1`, the skip being the opt-in theme half. The route ledger is the deliverable, not a red baseline: two of the five cells compile in both configurations, so their decline and lowering counters rather than their route carry the claim.

The final gate for the whole plan is one full-module run, expected green against the current baseline with zero failures and zero errors, plus the flip sweep and the build-cost sweep whose transcripts appear in `Artifacts and Notes`. The theme-fixture run of the three analytics queries named in milestone A6 was not performed, and should not be read as omitted evidence: those queries are targets for a projection that is enabled, every switch ships off, and the flip evidence already shows the enabled configuration losing on a cheaper corpus. Running them would measure the shipped default, which is byte-identical to the configuration measured before this work.

## Idempotence and Recovery

Every step here is safe to repeat. The Maven install is idempotent by construction. Test runs delete and regenerate their own module artifacts. The benchmark scripts write into `benchmark-results/` under distinct names and do not overwrite earlier captures.

Two things in the working tree must be preserved and are easy to destroy by accident. The loaded theme benchmark store under `core/sail/lmdb/target` is roughly 2.2 GB and is protected from `clean` by explicit configuration in the module's build file; do not defeat that protection. And the untracked artifacts at the repository root, including prior evidence files and benchmark captures, are deliberate and must not be swept up by a broad `git add` or a manual cleanup.

Each milestone is a separate file-scoped commit, so recovery from a bad milestone is a revert of one commit rather than an unpick. Because all four switches default to off through the entire plan, an operator can additionally disable any capability without reverting code; the serving switch in particular takes effect immediately on a running store, whereas the construction switch requires a restart.

If a test run appears to ignore a change, the cause is almost always a stale artifact in the workspace-local Maven repository: re-run the root install and try again. If offline resolution fails for a missing dependency or plugin, re-run the exact same command once without `-o` and then return to offline.

## Artifacts and Notes

The baseline that motivates milestone A6, taken from `benchmark-results/tier-m11-report-2026-08-07.txt`, showing the existing cell tied across all three regimes and therefore useless as an instrument:

    nodeEdgeDump        0.017±0.001    0.017±0.001    0.017±0.001    OVERLAP   OVERLAP
    predicateHistogram  3.525±0.164    0.030±0.007    0.028±0.002    OK        OK
    allPredicates       0.066±0.016    0.022±0.003    0.023±0.005    OK        OK

The matching engagement line from `benchmark-results/tier-m11-adjacency-r1-2026-08-07.txt`, showing that the tied cell was in fact refusing to serve:

    cell=nodeEdgeDump regime=adjacency adjacencyState=ACTIVE planes=0 kernelViews=0 kernelOpens=0
        fallbacks=PREDICATE_ENUMERATION_INCOMPLETE=96376

The second and third lines of the first excerpt are the reason milestone B1 explicitly does not route distinct-predicate listing or predicate histograms through the projection: those are already served by an existing prefix-run path at roughly 117 times the disk-tree speed, and adding a second route would be more work for the same answer.

The route ledger in its readable form, from `ThreeTierEngagementCensusTest`. Left half is the adjacency regime, right half the Janino regime; `np:decl` counts `PREDICATE_ENUMERATION_INCOMPLETE` fallbacks and `np:lower` counts variable-predicate patterns that compiled. With every switch off:

    cell                        rows adj:planes adj:views jan:planes jan:views jan:kernels np:decl np:lower  served by
    nodeEdgeDump                  17          0         0          0         0           0       1        0  lmdb only
    classPredicateMatrix          16          2         0          1         1           1     608        0  adjacency+janino
    repeatedNodeDump            1057          0         0          0         0           1      64        0  janino
    variablePredicateJoin         72          2         0          2         1           1       5        0  adjacency+janino
    incomingEdgeDump               6          0         0          0         0           0       1        0  lmdb only
    outDegreeHistogram          5028          0         0          0         0           0       0        0  lmdb only

...and with all four on. Every row count is identical, which is the correctness floor; what moves is that the declines go to zero, the planes start serving, and three cells compile a variable-predicate pattern:

    classPredicateMatrix          16        610         0          2         2           1       0        1  adjacency+janino
    repeatedNodeDump            1057         64         0         16        16           1       0        1  adjacency+janino
    variablePredicateJoin         72          7         0          3         2           1       0        1  adjacency+janino
    incomingEdgeDump               6          1         0          1         0           0       0        0  adjacency
    outDegreeHistogram          5028          0         0          0         0           0       0        0  lmdb only

The flip evidence, from `benchmark-results/node-predicate-flip-2026-08-08.txt`. Janino regime, 2000-person FOAF store, three warm-up and five measured iterations in one fork per cell:

    cell                    switches off        switches on         verdict
    classPredicateMatrix    1.673 ± 0.144 ms    4.768 ± 0.156 ms    2.85x slower, intervals disjoint
    repeatedNodeDump        0.285 ± 0.029 ms    0.284 ± 0.009 ms    unchanged
    incomingEdgeDump        0.016 ± 0.001 ms    0.016 ± 0.001 ms    unchanged
    outDegreeHistogram      4.447 ± 0.381 ms    4.277 ± 0.113 ms    unchanged (the control)

The build-time half, from `benchmark-results/node-predicate-build-cost-2026-08-08.txt`, over the shared multi-theme store with full coverage and the parallel build:

    nodePredicates=DISABLED      800.785 ms/op
    nodePredicates=OUTGOING     1616.311 ms/op    +102%
    nodePredicates=ALL_PLANES   2124.994 ms/op    +165%

That is the serial-tail cost the plan asked to have measured rather than assumed, and it is worse than assumed: building the outgoing projection roughly doubles the whole base build, because it runs on the build thread after the parallel primary build has finished.

The memory half comes from the same run, printed by the benchmark itself because a JMH teardown print never reaches the result file:

    ### adjacency build cost: nodePredicates=DISABLED   projectionNativeBytes=0        projectionJavaBytes=0     totalChargedBytes=60582176 projectionShare=0.00%
    ### adjacency build cost: nodePredicates=OUTGOING   projectionNativeBytes=12352768 projectionJavaBytes=7144  totalChargedBytes=72942088 projectionShare=16.94%
    ### adjacency build cost: nodePredicates=ALL_PLANES projectionNativeBytes=24338688 projectionJavaBytes=10832 totalChargedBytes=84931696 projectionShare=28.67%

The outgoing planes add 20.4 per cent to the base's steady-state charge; the incoming planes add essentially the same amount again, which contradicts this plan's standing assumption and is recorded as a discovery rather than quietly corrected.

Further transcripts are added here as milestones complete, kept short and focused on what proves success.

## Interfaces and Dependencies

The types below must exist at the end of the milestone named against each. All new adjacency types are package-private in `org.eclipse.rdf4j.sail.lmdb` unless stated; the query-engine interfaces are nested in the existing public source interface and follow its visibility.

At the end of milestone A1, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNodePredicateIndex.java`, replacing the file named for subjects:

    static LmdbNodePredicateIndex build(ImmutablePagedQuadCsfIndex adjacency,
            LmdbAdjacencyPredicateCatalog predicates, LmdbAdjacencyMemoryAccount account);
    static LmdbNodePredicateIndex wrap(ImmutablePagedQuadCsfIndex csf);
    boolean supportsPlane(int plane);
    long findLocalReference(int plane, long rawNode, ImmutablePagedQuadCsfIndex.LookupCursor cursor);
    void resolve(long localReference, ImmutablePagedQuadCsfIndex.RowCursor cursor);
    long rowCount(int plane);
    long incidenceCount(int plane);
    long nativeBytes();
    long modeledJavaBytes();
    LmdbNodePredicateIndex retainedCopy();
    ImmutablePagedQuadCsfIndex.RewriteResult rewrite(
            ImmutablePagedQuadCsfIndex.RowUpdateSource updates,
            ImmutablePagedQuadCsfIndex.MemoryBudget budget);
    void close();

`findLocalReference` throws `IllegalArgumentException` for a plane the index does not support, and returns zero only for a genuinely absent row. `resolve` throws `IllegalArgumentException` for a reference of zero or less.

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbInMemoryAdjacencyIndex.java`:

    LmdbNodePredicateIndex nodePredicateIndex();          // throws if absent
    LmdbNodePredicateIndex nodePredicateIndexOrNull();    // borrowed, non-owning; null if absent
    boolean supportsPredicateEnumeration(int plane);

At the end of milestone A2, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyOptions.java`, two resolved boolean options reachable from the existing pure resolution method, named `nodePredicateProjectionEnabled` and `nodePredicateProjectionIncomingEnabled`, both false unless the corresponding property is set. The property names are `rdf4j.lmdb.directAdjacency.nodePredicateProjection.enabled` and `rdf4j.lmdb.directAdjacency.nodePredicateProjection.incoming.enabled`. In `LmdbDirectAdjacencyStore`, a per-call predicate named `nodePredicateServingEnabled()` reading `rdf4j.lmdb.directAdjacency.nodePredicateProjection.serve.enabled`, defaulting to false, checked in the unbound-predicate serving branch and in the view factories added in milestone C1.

At the end of milestone A3, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyMemoryAccount.java`, two new constants on the existing flat enum:

    enum MemoryKind { ..., NODE_PREDICATE_NATIVE, NODE_PREDICATE_JAVA }

At the end of milestone C1, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java`, a shared run view extracted from the existing fixed-predicate interface, plus two interfaces extending it:

    interface RunView {
        long size(long runHandle);
        long neighborAt(long runHandle, long runOffset);
        long contextAt(long runHandle, long runOffset);
        int copyNeighbors(long runHandle, long runOffset, int length, long[] target, int targetOffset);
        int copyContexts(long runHandle, long runOffset, int length, long[] target, int targetOffset);
        long lowerBound(long runHandle, long fromOffset, long neighbor, long context);
        boolean runsNeighborOrdered();
    }

    interface DynamicAdjacency extends RunView {
        long NOT_FOUND = -1L;
        long NOT_COVERED = -2L;
        long runFor(long node, long rawPredicate);
    }

    interface NodePredicates extends RunView {
        long NOT_FOUND = -1L;
        long NOT_COVERED = -2L;
        long find(long node);
        long rowSize(long rowHandle);
        int copyRow(long rowHandle, long fromOffset, int length,
                long[] predicateTarget, int predicateOffset, long[] runTarget, int runOffset);
        double meanPredicateDegree();
    }

`rowSize` is the number of predicates on the node, deliberately not called an edge count, because the run view's `size` on the same object means the number of statements under one predicate. `copyRow` returns predicates and their resolved run handles together, validates every pair, and raises an inconsistency signal rather than returning a non-positive run handle. `NativeProbe` gains `nodePredicates(boolean bySubject)` and `dynamicAdjacency(boolean bySubject)`, each returning null when the view cannot be served, and each returning an object owned by that probe and invalid after it closes.

At the end of milestone C2, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelIr.java`, two node types beside the existing probe, contributing distinct cache keys of the forms `EP(n<view>,<key>->p<col>,x<col>[,g...]);` and `PV(d<view>,<key>,<pred>-><col>[,g...]);`, and a requirements accumulator extended with counts for both new view arrays.

At the end of milestone C3, in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeKernelBindings.java`, request records for both view kinds and all-or-nothing bulk request methods following the existing fixed-predicate pattern, plus corresponding arrays on the kernel context threaded through the sequential path, the mixed-binding path, and both parallel rungs.

---

Revision note, second revision, 2026-08-08. The plan is now complete: every milestone is delivered and all three flip decisions are taken. Four kinds of change were made to this document, and all four are corrections of things it previously asserted without evidence, which is why they are listed rather than folded in silently.

First, the `Purpose` section's promise for the compiler work has been rewritten. It said the work would be visible as a benchmark cell whose kernel-open count moved from zero to non-zero and whose runtime dropped below both interpreted tiers. Neither half survived measurement — the count could not move because the flagship's other half already compiles, and the runtime rose by a factor of 2.85 — so the promise now names the witness that does move, the count of compiled variable-predicate patterns, and the paragraph immediately below it keeps the original wording in view and explains what happened. Rewriting a purpose to match a result is dangerous; leaving a purpose that the delivered work does not meet is worse, and quietly deleting the original would be worst of all.

Second, all three flips resolved to "stays off", each for a different reason, and each is written into the `Decision Log` with its numbers and into its `Plan of Work` section as an outcome. The `Validation and Acceptance` entry for gate C is corrected in place to name the witness that could actually move.

Third, the plan's standing assumption that the incoming planes would cost more than double the outgoing ones is wrong: measured, they cost about the same. The three places that repeated it are corrected, and the discovery — including why the reasoning behind the assumption was plausible — is recorded rather than erased.

Fourth, the acceptance for the milestone C1 fault injection was rephrased from "each rung must fail" to "no rung may return a short answer", because the row-parallel rung correctly declines when it notices the fault before anything has been emitted, and demanding an exception would have made the system worse. The corresponding test allows either outcome and separately asserts the fault was detected, so the weaker phrasing cannot be satisfied vacuously.

Revision note, first revision, 2026-08-07. It supersedes an earlier informal draft in two substantive ways, recorded here because the reasoning matters more than the change. First, the earlier draft treated a single benchmark result as deciding one default, when in fact three separable capabilities were being enabled at once; the plan now carries three explicit flip decisions and ships every switch off. Second, the earlier draft described count and existence shortcuts as exact when they are exact only for unrestricted patterns, and described generated code that would skip a predicate whose run could not be resolved — which would have converted structural corruption into a silently incomplete answer, directly contradicting the recovery behaviour specified in milestone A5. Both are corrected above.
