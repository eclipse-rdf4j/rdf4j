# Make the node-to-predicate projection a first-class query primitive

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

Maintain this document in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF data is a set of statements, each a subject, a predicate, an object, and an optional named graph called a context. The LMDB store in this repository keeps those statements on disk in sorted B-trees, and additionally keeps an optional in-memory mirror called the direct adjacency index that can answer some query shapes far faster than the disk trees can.

Until recently that in-memory mirror could not answer one common shape at all: "given this one subject, give me every predicate and object attached to it", written in SPARQL as `<s> ?p ?o`. The mirror is organised predicate-first, so with no predicate supplied it had nothing to look up. Someone has since written a small sidecar structure that fixes exactly this — it maps a node to the sorted list of predicates that node uses — and wired it into one consumer.

After this ExecPlan, three things are true that are not true today. First, that sidecar is safe: it can no longer take the entire adjacency index down with it when memory is tight, it has an off switch, its memory cost is visible in the logs, and there is a test that proves the answers it produces are identical to the answers the authoritative disk trees produce. Second, the query compiler can see variable predicates. Today any SPARQL pattern whose predicate is a variable falls out of the compiled fast path entirely; after this work, patterns with a bound subject or object and a variable predicate compile and run in parallel like every other shape. Third, the reverse direction — "which predicates point at this object", written `?s ?p <o>` — becomes available behind its own switch.

You can see each of these working. The safety work is visible as a test that starves the memory budget and observes the store still coming up healthy. The compiler work is visible as a benchmark cell whose kernel-open count moves from zero to non-zero and whose runtime drops below both interpreted tiers with non-overlapping error bars. The reverse direction is visible as a query shape that stops declining.

## Progress

- [x] (2026-08-07 20:44Z) Ran the repository-root `-Pquick clean install`; BUILD SUCCESS in 34.7 seconds, so the tree compiles before any edit.
- [x] (2026-08-07 20:44Z) Read `.agent/PLANS.md` in full and confirmed the envelope: this file's content is only the ExecPlan, so it carries no triple-backtick fence, uses indented blocks for all transcripts and code, and keeps checkboxes to this section.
- [x] (2026-08-07 20:44Z) Established the three facts the design depends on by reading source rather than assuming: `MemoryKind` is a flat enum, `findRun`/`findRunByOrdinal` resolve against the primary index and not the sidecar, and the run codec makes each object-context pair its own edge.
- [x] (2026-08-07 20:44Z) Authored this plan.
- [x] (2026-08-07 20:57Z) Milestone A1 — renamed to the node-predicate vocabulary across eight files, added the nullable accessor, documented the borrowed non-owning contract, and made an unsupported plane throw instead of answering zero. One of the three suspected contract gaps turned out not to exist; see `Surprises & Discoveries`. Focused suites green: node-predicate index 6/0/0, node-predicate updates 4/0/0, paged base builder 3/0/0, direct-adjacency query 57/0/0, commit 13/0/0, consolidation 11/0/0.
- [ ] Milestone A2 — contain projection build failure and introduce all four switches (completed: the containment catch in `LmdbPagedCsfBaseBuilder`, the two projection memory kinds moved forward from A3, the `refuseKindForTest` seam, the `projectionRefusalKeepsTheAdjacencyBase` test, and the corrected high-water test; remaining: the four switches and their default-off wiring).
- [x] (2026-08-07 21:12Z) Milestone A2, containment half — a refused projection now leaves the paged base published and fully usable instead of destroying it. Suites green: paged base builder 4/0/0, node-predicate index 6/0/0, node-predicate updates 4/0/0, memory account 15/0/0, build workspace 3/0/0, direct-adjacency query 57/0/0, consolidation 11/0/0.
- [ ] Milestone A3 — rewrite observability, and reporting the charged projection bytes rather than the index's self-reported model (see the 384-byte discrepancy in `Surprises & Discoveries`). The two memory kinds this milestone was to introduce landed early, in A2.
- [ ] Milestone A4 — differential parity against the disk trees, plus lifetime and transaction coverage.
- [ ] Milestone A5 — differential shadow mode for this shape, a proven corruption-recovery path, and correction of a stale test baseline.
- [ ] Milestone A6 — benchmark cells and a per-cell route ledger, captured as a red baseline.
- [ ] Milestone B1 — count, degree, and existence shortcuts, each gated by an explicit eligibility rule, plus predicate-ordered streaming.
- [ ] Flip F1 — decide whether outgoing projection construction becomes the default.
- [ ] Milestone C1 — the two query-engine interfaces and their store implementations, with a total-resolution contract.
- [ ] Milestone C2 — the two compiler nodes and their generated code, with bounded buffers.
- [ ] Milestone C3 — pattern lowering, binding requests, and the parallel worker rungs.
- [ ] Flip F2 — decide whether compiled predicate enumeration becomes the default.
- [ ] Milestone D1 — incoming planes with zero cost when switched off, and parity extended to cover them.
- [ ] Flip F3 — decide the incoming default separately, because its memory cost is more than double the outgoing side.

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
  Rationale: The three capabilities have different costs and different evidence. Outgoing construction costs build time and memory; compiled enumeration costs compiler surface and risk; incoming planes cost more than double the outgoing memory. Bundling them would mean a single benchmark number silently deciding three questions.
  Date/Author: 2026-08-07, Claude.

- Decision: Add a fourth switch, a per-call serving gate, distinct from the build-time construction switch.
  Rationale: A build-time option resolved once during base construction cannot protect a store that is already open. Since the risk this feature carries is wrong answers rather than slowness, there must be a switch that makes every consumer decline immediately without waiting for a rebuild.
  Date/Author: 2026-08-07, Claude.

- Decision: Catch the memory refusal around projection construction only in the base builder, and deliberately not in the consolidator.
  Rationale: In the builder a refusal currently destroys the primary index, which is a strictly worse outcome than having no sidecar. In the consolidator a refusal already falls back to bounded overlay coalescing and keeps the previous base authoritative, which is correct; catching it there would instead silently strip a capability from a base that is already live and serving.
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

- Decision: Treat the `?s ?p ?o` grouped-by-subject benchmark cell as a control rather than a target.
  Rationale: All three positions of that pattern are unbound, and the enumerator requires a bound key. A whole-projection root enumerator would be needed to serve it, and the projection deliberately exposes no key-domain accessor. Keeping the cell as a control proves the sidecar causes no regression on shapes it does not serve.
  Date/Author: 2026-08-07, Claude.

## Outcomes & Retrospective

Not yet started. This section is to be written at the end of each gate and again at completion, comparing what was achieved against the Purpose section above.

## Context and Orientation

Everything in this plan lives in the module at `core/sail/lmdb`, whose main sources are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/` and whose tests are under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/`. Paths below are given in full from the repository root the first time each file appears.

### Terms used in this plan

A *statement* is a subject, predicate, object, and optional context. Internally each of those four is an opaque 64-bit identifier, and this plan never needs to convert them back to human-readable form.

A *plane* is one of four fixed directions the adjacency mirror stores, numbered by constants in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbReferenceNodeLocator.java`: 0 is outgoing explicit, 1 is incoming explicit, 2 is outgoing inferred, 3 is incoming inferred. "Outgoing" means keyed by subject; "incoming" means keyed by object. "Inferred" means the statement was derived by a reasoner rather than asserted directly.

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

The build-time cost deserves explicit measurement rather than assumption. Projection construction performs two complete merges across every predicate's key list, on the build thread, after the parallel primary build has finished — which is exactly the shape of serial tail that a recent piece of work spent an entire milestone eliminating. The build-only benchmark measures it with the switch on and off.

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

### Gate D, milestone D1: incoming planes

At the end of this milestone the projection can cover incoming planes, at no cost whatsoever when its switch is off.

The plane predicate and the emit loop generalise to all four planes, and the update companion's outgoing filter and two-way partition split become a four-way stable partition in the order the underlying rewrite expects.

The zero-cost requirement is stronger than not publishing the result: with the switch off, incoming update capture, partition arrays, rewrite work, and temporary workspace must all be skipped, proven by a test asserting no incoming charge and no incoming workspace allocation.

The parity suite from milestone A4 extends to incoming planes before any incoming consumption is switched on. One existing test is expected to change rather than pass: a case pinning the decline reason for an inlined object will shift from incomplete-enumeration to a different reason, because paged bases build no inline incoming structures and the serving branch requires a reference-typed key. The pin is rewritten to the new contract rather than deleted.

### Flip F3

The incoming default is decided separately, because the row domain for incoming planes is distinct objects including referenced literals, and the memory cost is expected to exceed double the outgoing side. It requires incoming parity green, the incoming cells engaging, and an accepted memory percentage on the theme fixture. If the economics do not justify it, the honest outcome is that incoming remains opt-in, and this plan records that rather than leaving a designed, tested, and lowered capability silently disabled with no explanation.

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

This section is updated as milestones complete, with the actual commands used and short transcripts of what they printed.

## Validation and Acceptance

Acceptance is phrased below as behaviour an operator or reviewer can observe, not as internal structure.

For milestone A1, running the three test classes that construct the projection passes both before and after the rename, and searching the tree for the old type name returns nothing. The two new validation tests fail before the change with a returned zero and an unchecked native read respectively, and pass after with the documented exceptions.

For milestone A2, a store configured with a memory budget large enough for the primary index but too small for the sidecar starts successfully, reports the paged format, reports that predicate enumeration is unsupported, and serves the unbound-predicate shape from the disk trees. Before the change the same configuration leaves the store with no adjacency index at all and no retry. Separately, setting the construction switch to false and re-running the query test produces a byte-identical result set with the incomplete-enumeration fallback counter incrementing.

For milestone A3, the two lifecycle log lines contain a distinct entry for the projection's native and Java bytes, and the fold-down log line reports the projection's shared and replaced page counts. A rewrite whose primary and projection halves each fit the cap individually but not together is refused.

For milestone A4, the parity test passes at all four lifecycle points with every assertion naming its seed. Running it before milestone A2's containment fix is not meaningful, so it is ordered after. The most likely genuine failures on first run are the node with more than 256 predicates and the node that exists only in an overlay; if both pass immediately that is recorded in `Surprises & Discoveries` rather than a failure being manufactured.

For milestone A5, a store in shadow mode with sampling set to every request increments its comparison counter for the unbound-predicate shape, which stays at zero today, and records no mismatches across the query fixture. The corruption test observes the five-step recovery sequence in order.

For milestone A6, the benchmark run produces the five new cells with error bars, and the census asserts each cell's route and decline reason. The baseline is copied into `benchmark-results/` so later comparisons have something to compare against.

For milestone B1, a query restricted to one named graph over a node whose predicates are spread across two graphs returns the smaller, correct count, where a naive row-length shortcut would return the larger one. An ordered request by predicate is served rather than declined; an ordered request by object for the same shape is still declined.

For milestones C1 through C3, the flagship benchmark cell's kernel-open count moves from zero to non-zero and its compiled time falls below both interpreted tiers with non-overlapping intervals. The fault-injection double causes all three kernel rungs to fail rather than under-report. The binding truth table agrees with the ordinary evaluator on every combination.

For milestone D1, the incoming dump cell stops declining, the parity suite passes for incoming planes, and with the incoming switch off the incoming memory charge is zero.

The final gate for the whole plan is one full-module run, expected green against the current baseline of roughly 3,073 tests with zero failures and zero errors, followed by the three-regime benchmark sweep and a theme-fixture run of the three analytics queries identified in milestone A6.

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

Revision note. This document is the first revision, created 2026-08-07. It supersedes an earlier informal draft in two substantive ways, recorded here because the reasoning matters more than the change. First, the earlier draft treated a single benchmark result as deciding one default, when in fact three separable capabilities were being enabled at once; the plan now carries three explicit flip decisions and ships every switch off. Second, the earlier draft described count and existence shortcuts as exact when they are exact only for unrestricted patterns, and described generated code that would skip a predicate whose run could not be resolved — which would have converted structural corruption into a silently incomplete answer, directly contradicting the recovery behaviour specified in milestone A5. Both are corrected above.
