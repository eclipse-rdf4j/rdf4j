# Factorized adjacency IR with morsel parallelism

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintain this document in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J's LMDB store already builds two ordered, fixed-predicate adjacency views. Outgoing SOC has one root per subject and an ordered `(object, context)` run. Incoming OSC has one root per object and an ordered `(subject, context)` run. The analytical theme queries should therefore operate at the coarsest physical level that preserves their SPARQL semantics: predicate-plane metadata, roots, distinct neighbor fibers, or complete quads. The current evaluator often selects adjacency but then reconstructs statement-like work, repeatedly resolves runs, or moves raw quads through the existing parallel exchange.

After this work, ANALYTICS q0 through q12 all have complete adjacency physical candidates. Metadata-only queries do not decode adjacency payloads. Root and fiber queries carry their statement multiplicity without expanding repeated contexts. Broad analytical pipelines use bounded primitive batches and root/fiber morsels rather than object-per-row state. The user can observe this through exact query results, physical-plan telemetry that names the selected grain and direction, and matched JDK 26 JMH results against forced LMDB controls.

The performance objective is at least a 10x geometric-mean improvement for ANALYTICS q3 through q12. q7 and q8 must return exactly 125 and 56 groups and improve by at least 5x individually. q0 through q2 must remain below 0.2 milliseconds. These are acceptance gates, not assumptions; no speedup is claimed until matched benchmark and profiling evidence exists.

## Progress

- [x] Replace the negative page-local source-type cache with sideways information passing: drive bounded morsels from
  the sorted `rdf:type` SOC subject domain, retain each subject's type fiber once per morsel, and semijoin/probe every
  accepted edge plane from that primitive domain. Keep a costed merge/gallop alternative for denser edge domains.
- [x] Prove the type-matrix path never consults the retained label synopsis, even when that optional facility is
  configured elsewhere; all q7/q8 performance evidence must continue to run with the synopsis disabled.
- [x] Keep physical SIP coordinates inside lease-bound CSF cursors and copy fibers directly into reusable primitive
  batches. Raw addresses do not escape into the IR: overlays and write snapshots retain logical handles, and immutable
  page/vector coordinates remain valid only while their owning cursor lease is open.
- [x] Optimize q6-q8 beyond the historical five-times-faster gates with the synopsis disabled.

- [x] (2026-08-29 14:49Z) Preserved the benchmark corpus at `/private/tmp/rdf4j-lmdb-theme-complete-20260829-5214259283` and confirmed no JMH or Maven process remained active.
- [x] (2026-08-29 14:49Z) Ran the mandatory root JDK 25 `-Pquick clean install`; every reactor module succeeded in 36.010 seconds.
- [x] (2026-08-29 14:54Z) Added and captured the failing root-fiber metadata regression: results were exact but OSC decoded four neighbor rows instead of zero.
- [x] (2026-08-29 14:59Z) Implemented authoritative direct/decoded CSF root-fiber counts and passed the exact regression with zero payload decodes in 0.422 seconds.
- [x] (2026-08-29 15:08Z) Added physical `PLANE`/`ROOT`/`FIBER`/`QUAD` grains plus fixed-capacity primitive plane/root/fiber batches; reflection and multiplicity contracts pass.
- [x] (2026-08-29 15:10Z) Re-ran the focused type-matrix suite: all eight q7/q8 shape, multiplicity, adjacency, and fallback tests pass.
- [x] (2026-08-29 18:00Z) Integrated plane/root/fiber batches into projection-free q7/q8, added bounded target deduplication, full-morsel boundary coverage, and bulk retained-run fiber decoding; the full matrix and batch suites pass.
- [x] (2026-08-29 18:09Z) Rejected the first plane-batch architecture from matched evidence, then demonstrated that a factorized fixed-predicate label synopsis improves q7 by 1.99x and q8 by 2.68x before parallelism.
- [x] (2026-08-29 18:23Z) Replaced the over-budget decoded CSR with a 2,382,672-byte packed type-pattern synopsis (3.9 percent of base adjacency) and bound exact q7/q8 type access to it.
- [x] (2026-08-29 18:46Z) Added same-snapshot root-ordinal morsels, worker-local primitive reductions, per-worker deques, stealing, failure containment, and metadata cost admission; the 13-test type-matrix suite passes.
- [x] (2026-08-29 18:46Z) Matched JDK 26 gates: q7 is 13.481 ms versus 3,257.776 ms forced LMDB (241.7x); q8 is 45.490 ms versus 7,479.201 ms (164.4x), with exact 125/56 groups.
- [x] (2026-08-29 19:50Z) Lowered q4/q11 to predicate-plane metadata, q6 to an unsigned root-domain merge, and q5/q9/q10 to an exact compressed root-domain synopsis admitted under the cumulative 10 percent metadata cap.
- [x] (2026-08-29 19:50Z) Passed the 16-test datatype, thresholded domain-group, and SOC/OSC intersection suite, including exact overlay fallback and immutable-base synopsis execution.
- [x] (2026-08-29 20:11Z) Re-encoded root multiplicities as cursor-order unsigned varints; the exact incoming synopsis now claims 5,528,432 bytes, bringing total direct-adjacency allocation to 109.13 percent of baseline.
- [x] (2026-08-29 20:15Z) Matched JDK 26 evidence: q9 is 18.473 ms versus 289.411 ms forced LMDB (15.7x); q5 is 497.526 ms versus 690.777 ms (1.39x), exposing ValueStore datatype-header lookup as its remaining bottleneck.
- [x] (2026-08-29 20:41Z) Added uniform term-kind flags to root/fiber batches and one-transaction bulk literal-header lookup; q5 improved to 420.096 ms and the exact focused contracts pass.
- [x] (2026-08-29 20:58Z) Retained datatype-count summaries and added the format-versioned core-datatype reference literal ID, with 53 stable tags, legacy/custom/ordinal-overflow fallback, reopen coverage, and bulk-writer parity.
- [x] (2026-08-29 22:34Z) Lowered q0 through q12 to reusable grain-aware operators; q12 now classifies one predicate-plane lane per predicate and shares plane multiplicity across ordinary and DISTINCT-P channels.
- [x] (2026-08-29 18:46Z) Replace quad morsels for q7/q8 with bounded root/fiber morsels and worker-local reductions.
- [x] (2026-08-29 22:16Z) Lowered q10 to an exact incoming multiplicity synopsis plus outgoing presence representation; matched execution is 0.122 ms/op with zero root/payload decode and retained adjacency at 109.79 percent of base.
- [x] (2026-08-29 22:34Z) Matched the retained datatype summary and predicate-plane grouping: q5 is 0.027 ms versus 690.777 ms forced LMDB (25,584x), and q12 is 0.036 ms versus 11,943.859 ms (331,774x).
- [x] (2026-08-29 22:47Z) Made every retained global adjacency synopsis one live, typed opt-in that defaults off; with it absent, the exact q10-shape regression uses the streaming SOC/OSC root-domain intersection and allocates no synopsis state.
- [x] (2026-08-30 04:00Z) Investigated scalar versus Java Vector API kernels in matched AArch64 forks. Transition
  counting, multiplicity summation, and unsigned range counting won in isolation, but no operation passed the complete
  microbenchmark, hardware-code, and affected-query promotion gates, so production remains scalar.
- [x] (2026-08-30 04:10Z) Ran the matched no-synopsis JDK 26 sweep. q3-q12 achieved a 3,183.2x geometric-mean
  speedup over forced LMDB; every query won, q7/q8 produced exactly 125/56 groups, and q0 followed by q1 measured
  0.037/0.058 ms per operation.
- [x] (2026-08-30 04:46Z) Passed the full JDK 25 LMDB module gate with 4,176 tests, zero failures, zero errors, and
  103 skipped tests; audited formatting, headers, debug output, and whitespace without deleting retained artifacts.
- [x] (2026-08-30 07:43Z) Started the post-implementation page-header consumer sweep, confirmed the protected
  10 GiB corpus, JDK 25/JDK 26 AArch64 runtimes, and passed the mandatory root quick clean install in 35.718 seconds.
- [x] Inventory every production CSF page/root/fiber consumer and classify whether each root/neighbor term-kind or
  literal-datatype header can reject, bulk-count, specialize, or cannot legally answer its operation.
- [x] Capture matched no-synopsis JDK 26 baselines for every analytical query whose current execution decodes values
  that an authoritative page header could avoid.
- [x] Add the smallest failing instrumentation and semantic regressions before changing each selected consumer.
- [x] Push q9's `isIRI` predicate to OSC root pages; matched no-synopsis execution improves from 36.521 +/- 0.325
  to 27.824 +/- 0.563 ms/op while preserving the exact residual filter.
- [x] Give q8 complete sideways-type and page-morsel candidates, cost them from exact CSF plane/page/root/fiber
  metadata, and arbitrate per dataset. The literal-heavy regression selects page morsels; ANALYTICS selects sideways.
- [x] Reject physical fiber-copy header propagation after its ten-iteration 46.792 +/- 0.970 ms/op result failed to
  beat the 47.119 +/- 2.431 ms/op control; remove the candidate rather than retain unproven hot-loop complexity.
- [x] Re-run q7/q8 with synopsis disabled: 10.071 +/- 2.983 and 43.125 +/- 0.727 ms/op, respectively, preserving
  benchmark validation and exceeding the user-provided historical five-times-faster gates.
- [x] (2026-08-30 09:39Z) Ran final formatting and the JDK 25 LMDB module gate: 4,274 tests, zero failures,
  zero errors, and 103 skipped tests. No retained benchmark, corpus, or evidence artifact was deleted.

## Surprises & Discoveries

- Observation: the repository already contains a correct but statement-grained morsel exchange. `LmdbNativeExchange.Morsel` owns a newly allocated `long[]` of raw quads, so reusing it unchanged would preserve the wrong execution grain and steady-state allocation.
  Evidence: `LmdbNativeExchange.produceMorsels` allocates `new long[4 * MORSEL_ROWS]` for each 1,024-row morsel and copies a short final batch.

- Observation: the current IR already has a bulk run feature named `vectorTail`, but it means copied primitive batches and generated selection loops; it does not use `jdk.incubator.vector`.
  Evidence: `LmdbNativeKernelEmitter` emits `copyNeighbors` calls and scalar generated loops, while no production source imports `LongVector` or `IntVector`.

- Observation: the direct CSF root cursor retains the exact page/local-row coordinate and lazily obtains quad multiplicity, but the public cursor contract omits the row's distinct neighbor-fiber count.
  Evidence: `ImmutablePagedQuadCsfIndex.KeyCursor` owns `firstPageId` and `firstLocalRow`, `CompactCsfPageReader` derives row-fiber boundaries, and `NativeAdjacency.KeyRunCursor` exposes only `runSize` plus payload access.

- Observation: a previous specialist rewrite made q7/q8 correct but demonstrated that logical adjacency selection is insufficient. The projection-independent subject-major heap merge remained slower than the last correct LMDB baselines.
  Evidence: `.agent/lmdb-streaming-bidirectional-aggregation-execplan.md` records 403.793 ms for q7 and 1,380.553 ms for q8 after the Cartesian-probe fix, versus 70.487 and 217.530 ms historical LMDB baselines.

- Observation: the distinct neighbor-fiber count needs no persisted-format change. Existing per-page row-fiber boundaries answer it, with one unsigned endpoint comparison per continuation boundary to avoid double-counting a neighbor whose contexts span pages.
  Evidence: `fixedPredicateDistinctNeighborUsesRootFiberMetadata` now passes with `NEIGHBOR_VALUES_DECODED = 0` and `CONTEXT_VALUES_DECODED = 0`.

- Observation: the reusable batch surface requires no steady-state routing structure. Plane, root, and fiber batches are caller-owned fixed-capacity primitive arrays; a fiber batch retains only its current run offset between fills.
  Evidence: `LmdbNativeAdjacencyBatchContractTest` validates the interface and exact root/fiber multiplicities with batch capacity one.

- Observation: Oracle's JDK 26 Vector API documentation confirms optimized AArch64 support is NEON, not SVE, and describes masked operations as blends on platforms without native general masking.
  Evidence: this narrows the experiment to full-lane blocks and scalar tails; masked short tails are not assumed wins.

- Observation: matched JDK 26 corpus runs validate q3's root-metadata architecture but reject the current q7/q8 architecture. q3 is exact at 0.136 ms/op; q7 is exact at 101.834 ms/op; q8 is exact at 3,912.597 ms/op.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q3-current-jdk26.txt`, `analytics-q7-current-jdk26.txt`, and `analytics-q8-current-jdk26.txt`.

- Observation: projection-free q7/q8 currently choose a statement-oriented fallback after costing the all-predicate subject heap. q8 then performs target-type resolution in that row stream; the result is correct but the physical shape is fundamentally too expensive.
  Evidence: q7/q8 telemetry reports `nativeExecutionPath=typeMatrix`, while the source inspection shows the only projection-free adjacency candidate is `SubjectPredicateSweep`, whose work estimate includes heap levels and whose rejection precedes cursor opening.

- Observation: plane-major batching alone is not the missing architecture. It made q7 334.017 ms and q8 1,133.124 ms; bounded target deduplication improved q8 to 964.741 ms, while bulk fiber copies were neutral at 994.535 ms because the corpus contains many short edge runs.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q7-plane-morsel-jdk26.txt`, `analytics-q8-plane-morsel-jdk26.txt`, `analytics-q8-deduplicated-morsel-jdk26.txt`, and `analytics-q8-bulk-fiber-jdk26.txt`.

- Observation: zero-copy fixed-predicate type labels are the first material q7/q8 kernel win. With the fork's adaptive-object cap raised only for diagnosis, q7 falls to 168.187 ms and q8 to 370.499 ms. The ordinary cap leaves the path inactive, and the generic key hash plus decoded CSR claims 190,497,544 bytes against only 60,625,912 bytes of direct adjacency, so that representation violates the 110 percent gate.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q7-large-csr-diagnostic-jdk26.txt`, `analytics-q8-large-csr-metrics-corrected-jdk26.txt`, and the reported `DIRECT_ADJACENCY_memoryUsedBytes` / `CSF_ADAPTIVE_CLAIMED_BYTES` metrics.

- Observation: packed type-pattern codes plus root-ordinal parallelism are the missing q7/q8 architecture. The synopsis adds 2,382,672 bytes (3.9 percent of base adjacency); matched morsel execution reaches 13.481 ms for q7 and 45.490 ms for q8, while forced LMDB takes 3,257.776 ms and 7,479.201 ms respectively.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q7-parallel-morsels-jdk26.txt`, `analytics-q8-parallel-morsels-jdk26.txt`, `analytics-q7-forced-lmdb-matched-jdk26.txt`, and `analytics-q8-forced-lmdb-matched-jdk26.txt`.

- Observation: hot-loop telemetry was itself material statement-grained work. Moving roots, fibers, borrowed slices, and synopsis slices into worker-local counters removed an atomic update per root/fiber; each successful query now publishes those totals once.
  Evidence: the matched q7/q8 telemetry retains exact aggregate totals while the production loop contains only primitive increments.

- Observation: direct-adjacency readiness does not imply that base-only metadata is authoritative. A completed transaction can be served exactly by an immutable base plus overlays; the root-domain synopsis must decline that view, while reopening the completed LMDB snapshot produces the authoritative base used by analytical benchmarks.
  Evidence: the q9 regression first reported only `DELTA` and `JAVA_METADATA` charges and two exact synopsis declines; after reopening it executes the incoming-domain kernel, and the combined 16-test suite remains green.

- Observation: q5, q9, and q10 share the same physical primitive: an ordered distinct root domain with exact aggregate quad multiplicity. Encoding that primitive once per direction is both smaller and faster than repeating an all-predicate heap merge for each query.
  Evidence: `LmdbNodeDomainSynopsis` chooses dense ordinal masks only for bounded spans and otherwise stores unsigned sparse IDs plus bit-packed multiplicities; `LmdbDatatypeHistogramTest`, `LmdbAdjacencyDomainGroupTest`, and `LmdbExistsIntersectionQueryTest` exercise the shared contract.

- Observation: the exact root-domain synopsis fits only after multiplicities are encoded as unsigned varints. On the ANALYTICS corpus the incoming domain occupies 5,528,432 bytes, total adjacency is 66,154,344 bytes versus a 60,625,608-byte base, and q9 falls from 289.411 ms forced LMDB to 18.473 ms.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q5-q9-varint-domain-synopsis-jdk26.txt` and `analytics-q5-q9-forced-lmdb-jdk26.txt`.

- Observation: one retained incoming synopsis plus a streaming SOC predicate-heap is exact but not a viable q10 kernel. It takes 665.212 ms, worse than both the earlier 358.052 ms two-stream adjacency merge and the LMDB control.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q10-one-sided-domain-synopsis-jdk26.txt`; the next candidate is a compact opposite-direction presence mask, not a second multiplicity synopsis.

- Observation: `ValueIds` already carries a six-bit term/type code. Inline numeric, date/time, boolean, and short-string codes determine literal datatype without ValueStore access; only referenced `T_LITERAL` IDs require a record-header lookup. The current q5 path opens a ValueStore read transaction for every referenced literal, explaining why root aggregation alone improves q5 only 1.39x.
  Evidence: `ValueIds.getIdType`, `ValueStore.literalDatatypeId`, and the matched q5 measurements above.

- Observation: bulk header reads remove transaction churn but still perform one LMDB lookup for every distinct referenced literal. q5 improved from 497.526 ms to 420.096 ms, which confirms that transaction reuse is useful but cannot supply the required order-of-magnitude result on its own.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q5-bulk-datatype-headers-jdk26.txt`.

- Observation: ordinary non-inline literal IDs retain only a ValueStore ordinal. The datatype is present in the record header but is absent from the ID, so page/vector classification cannot be authoritative until the header has been read.
  Evidence: `ValueIds.T_LITERAL`, `ValueStoreRecordCodec`, and the q5 bulk-header counter regression.

- Observation: adaptive timing cannot safely arbitrate an authoritative whole-query structural answer against a finer-grain engine. A cold 4.561 ms q10 sample quarantined the exact bitset intersection and caused a 27-second generic aggregate rescue even though steady-state intersection cost is about 0.1 ms.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q10-arbitration-trace-jdk26.txt` and the focused adaptive-arbitration regressions.

- Observation: q10's exact opposite-domain membership fits within the remaining routing budget when represented by per-ID-type dense/sparse presence sets rather than a second multiplicity synopsis. The outgoing presence claims 398,376 bytes; combined synopsis metadata claims 5,926,856 bytes over a 60,525,608-byte base.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q10-final-matched-jdk26.txt` reports 0.122 ms/op, zero roots visited, and total direct-adjacency memory of 66,552,768 bytes.

- Observation: q5 and q12 demonstrate why expression grain matters more than per-row SIMD for metadata-reducible shapes. Retaining datatype-group multiplicity makes q5 25,584x faster than forced LMDB, while decoding/classifying each predicate exactly once makes q12 331,774x faster; both steady-state kernels finish in under 0.04 ms.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q5-datatype-summary-matched-jdk26.txt`, `analytics-q5-q9-forced-lmdb-jdk26.txt`, `analytics-q12-plane-matched-jdk26.txt`, and `analytics-q12-forced-lmdb-matched-jdk26.txt`.

- Observation: synopsis-enabled analytical timings conceal the cost that a write-heavy deployment must repeatedly invalidate or rebuild. The scalable control must therefore run against intrinsic predicate-plane/root/fiber metadata only; retained global label, root-multiplicity, membership, and datatype summaries are now disabled together unless explicitly requested.
  Evidence: `LmdbRuntimePropertiesTest.retainedAdjacencySynopsesDefaultOffAndCanBeEnabledLive` and `LmdbExistsIntersectionQueryTest.retainedSynopsesDefaultOffAndUseStreamingSocOscRootIntersection` pass with exact results and root visits but no synopsis/presence intersection.

- Observation: exposing native page addresses through IR batches is unnecessary and would make snapshot lifetime a
  distributed correctness obligation. The direct CSF cursor already owns page/local-row coordinates and can bulk-copy
  directly into a reusable primitive fiber batch while its lease is valid; this preserves the locality benefit without
  allowing a dangling address after cursor close, overlay replacement, or mapping teardown.
  Evidence: `ImmutablePagedQuadCsfIndex.KeyCursor.copyFibers` decodes from its retained page cursor coordinates, while
  `NativeLmdbQuerySource.KeyRunCursor.copyFibers` documents that the address and lease never escape the physical cursor.

- Observation: the scalable no-synopsis architecture exceeds every analytical performance gate. Matched JDK 26
  adjacency versus forced-LMDB scores for q3-q12 yield a 3,183.2x geometric mean; q6 is 5.988 ms, q7 is 8.197 ms, and
  q8 is 40.454 ms in the complete matched sweep.
  Evidence: `.agent/evidence/factorized-adjacency/q3-q12-matched-speedups.txt` and the paired current/forced-LMDB JSON
  result files in the same directory.

- Observation: q8 has two legitimately different winners. Sideways type morsels win when resource edges dominate;
  page morsels win when uniform literal pages let header pruning remove most work before type lookup. A single
  unconditional dispatch is therefore architecturally wrong even though both candidates read the same SOC planes.
  Evidence: `LmdbTypeMatrixTest.linkageArbiterPrefersPageMorselsWhenHeadersRejectMostEdgePages` selects the page
  candidate and returns exactly 2,000 links, while ANALYTICS telemetry selects sideways and measures 43.125 ms/op.

- Observation: not every available page header is a profitable hot-loop operation. Passing a neighbor-kind trait
  through every q8 physical fiber copy replaced one scalar ID-kind test with roughly one header action on the corpus's
  predominantly one-fiber runs; the longer measurement was statistically indistinguishable from the control.
  Evidence: `.agent/evidence/factorized-adjacency/analytics-q8-arbiter-final-jdk26.txt` records both rejected variants.

## Decision Log

- Decision: Extend the current `LmdbNativeKernelIr` and `NativeLmdbQuerySource.NativeAdjacency` contracts rather than create another query-index specialist.
  Rationale: the repository already has interpreted and Janino execution, adjacency snapshots, exact overlay composition, partitioned execution, and telemetry. A second engine would duplicate correctness and lifecycle machinery.
  Date/Author: 2026-08-29 / Codex.

- Decision: Define four physical grains. `PLANE` is one predicate/direction partition with authoritative totals. `ROOT` is one S root in SOC or O root in OSC. `FIBER` is one distinct opposite endpoint under a root, regardless of repeated contexts. `QUAD` is one accepted `(S,P,O,C)` statement.
  Rationale: these are the actual levels represented by the CSF storage. Operators must not decode a finer level than their dependencies require.
  Date/Author: 2026-08-29 / Codex.

- Decision: Carry a long multiplicity on every root and fiber batch lane. Root multiplicity is accepted quad count. Fiber multiplicity is accepted context count. DISTINCT consumes ordered transitions or authoritative fiber counts; ordinary COUNT consumes multiplicity.
  Rationale: this preserves SPARQL bag semantics without expanding repeated contexts into rows.
  Date/Author: 2026-08-29 / Codex.

- Decision: Keep new retained routing metadata within 10 percent of direct-adjacency memory and do not enable the existing full node-predicate projection.
  Rationale: the user selected the 10 percent limit, and prior measurements show the full outgoing/incoming projection exceeds it before adding stable run locators.
  Date/Author: 2026-08-29 / Codex.

- Decision: Make Java Vector API work an evidence-gated investigation after allocation-free scalar batches exist. Benchmark scalar-unresolved, scalar-resolved, and forced-vector forks independently on AArch64.
  Rationale: physical-grain and algorithmic changes dominate. Vector code is retained in production only for operations whose microbenchmark improves by at least 10 percent and whose affected query aggregate improves by at least 3 percent with a confidence interval above parity.
  Date/Author: 2026-08-29 / Codex.

- Decision: Preserve LMDB as a costed physical alternative and report adjacency as selected, outranked, or ineligible. Every supported analytical shape must still receive a complete adjacency candidate.
  Rationale: availability proves correctness eligibility, not lower cost for every density or transaction state.
  Date/Author: 2026-08-29 / Codex.

- Decision: Arbitrate q8's sideways-type and page-morsel adjacency kernels from exact, snapshot-authoritative work
  bounds. Prefer the incumbent sideways kernel when intervals overlap; select page morsels only when page-header
  rejection makes its whole interval cheaper. Publish the winner and both estimates in query telemetry.
  Rationale: query text alone cannot determine the winner. Predicate density, type-domain density, and the number of
  uniform non-resource pages vary by dataset, while conservative interval domination keeps uncertain estimates from
  causing broad routing regressions.
  Date/Author: 2026-08-30 / Codex.

- Decision: Replace the projection-free q7/q8 subject heap with plane-major root morsels. Scan each accepted SOC plane in root batches, batch-resolve source `rdf:type` runs, consume edge fibers from retained run handles, and batch-resolve target type runs. q8 aggregates a bounded target batch before lookup; later morsel parallelism partitions planes/root ordinals without global atomics.
  Rationale: this uses the order already present in each SOC plane, removes O(log predicate-count) coordination per root, avoids the over-budget node-predicate projection, and turns millions of scalar type probes into sorted `findBatch` calls against the existing CSF partition lookup.
  Date/Author: 2026-08-29 / Codex.

- Decision: Treat the batch/dedup implementation as the exact cold/overlay fallback, not the final hot kernel. Retain one packed fixed-predicate label-pattern code per eligible node-ID ordinal, where each code names a shared complete neighbor/multiplicity pattern. Reuse the structured LMDB value ordinal for O(1) routing instead of retaining a second exact key hash.
  Rationale: the generic decoded CSR proves factorized label access improves both blocked kernels, but its 190 MB claim is more than three times the whole direct adjacency allocation. Pattern codes factor repeated type runs, preserve context multiplicity and multi-types, and can be refused unless their exact charge stays inside the cumulative 10 percent synopsis budget.
  Date/Author: 2026-08-29 / Codex.

- Decision: Schedule analytical adjacency work as predicate/root-ordinal windows of at most 8,192 roots on same-snapshot sibling sources. Give each worker a local deque and primitive counter table, allow opposite-end stealing, and merge only after every worker succeeds.
  Rationale: ordinal windows position directly in decoded/direct CSF key domains, preserve ordered sequential access inside each morsel, bound scratch by worker count, and avoid shared aggregation atomics. The matched q7/q8 improvement validates the scheduling grain.
  Date/Author: 2026-08-29 / Codex.

- Decision: Admit a lazy exact root-domain synopsis per SOC/OSC plane only for a complete immutable base with no applicable overlay. Use unsigned k-way construction, retain exact root multiplicity, and charge routing plus payload together with the label synopsis against one cumulative 10 percent allowance.
  Rationale: q5/q9 need one classification per distinct incoming root and q10 needs domain membership, while repeated query-time predicate heaps are both slower and redundant. Declining overlays preserves snapshot and tombstone correctness without duplicating overlay state.
  Date/Author: 2026-08-29 / Codex.

- Decision: Carry uniform term-kind and uniform literal-datatype traits on physical page/batch descriptors. An inline `ValueIds` type code is itself an authoritative datatype token; a referenced-literal datatype token is authoritative only after a ValueStore header scan. Add a sorted bulk header operation that keeps one read transaction open per batch and emit mixed/uniform flags without materializing labels.
  Rationale: q5 must reject non-literal pages and aggregate inline datatype pages at page grain. Referenced literals remain exact while avoiding 1.69 million transaction acquisitions. The traits also provide reusable coarse-grain expression placement for `isIRI`, `isLiteral`, and datatype filters.
  Date/Author: 2026-08-29 / Codex.

- Decision: Do not promote q10's one-synopsis/one-predicate-heap experiment. Retain one multiplicity-bearing incoming synopsis for q5/q9 and admit a presence-only outgoing mask only if both structures remain inside the shared 10 percent cap; otherwise use the prior exact merge/fallback.
  Rationale: matched evidence rejects the heap stream, while the outgoing subject domain is expected to need only a dense presence mask and no multiplicities for q10 membership.
  Date/Author: 2026-08-29 / Codex.

- Decision: Introduce a versioned non-inline core-literal reference encoding. A dedicated ID type stores a stable core-datatype tag in low value-field bits and the ValueStore ordinal in the remaining bits. New stores advertise the capability through a bumped store format; legacy stores continue minting `T_LITERAL`, custom datatypes always use `T_LITERAL`, and an ordinal that does not fit the reduced field falls back to `T_LITERAL`.
  Rationale: the ID then makes the common literal datatype authoritative at page/root grain without changing the ValueStore record or duplicating labels. A format bump prevents an older reader from treating the new type as inline or unknown and silently returning wrong values.
  Date/Author: 2026-08-29 / Codex.

- Decision: Retain only datatype-group multiplicities on an immutable complete OSC domain synopsis. The build scans legacy referenced-literal headers once in sorted batches; subsequent queries consume one primitive group lane per datatype. Overlay-visible snapshots decline and use the exact streaming path.
  Rationale: the retained state is proportional to datatype count rather than literal/root count, fits the metadata cap, accelerates existing stores, and naturally benefits from the new ID encoding as newly written core literals require no header lookup.
  Date/Author: 2026-08-29 / Codex.

- Decision: Treat an authoritative complete-query structural answer as grain-dominant in adaptive arbitration. It remains a normal candidate that may decline and re-rank to LMDB, but a successful exact set answer cannot be quarantined, rescued, probed, or displaced by a finer-grain engine from timing evidence.
  Rationale: timing compares implementations of the same required work; it cannot overturn a proof that one candidate answers the whole query from exact retained metadata while the rival enumerates millions of finer-grain rows. This also prevents cold setup/JIT outliers from launching unbounded analytical scans.
  Date/Author: 2026-08-29 / Codex.

- Decision: Lower deterministic per-predicate computed grouping through a reusable plane classifier and counter vector. q12's exact namespace REPLACE expression is recognized semantically, but execution consumes generic `PlaneBatch` lanes and supports any number of ordinary bound-variable counts plus DISTINCT predicate counts.
  Rationale: the expression depends only on P, so evaluating it at QUAD grain is redundant by the plane's statement multiplicity. One classifier call per predicate preserves exact bag/DISTINCT semantics and confines scratch to output groups.
  Date/Author: 2026-08-29 / Codex.

- Decision: Disable retained global adjacency synopses by default behind `rdf4j.lmdb.directAdjacency.synopsis.enabled`. The one switch governs fixed-predicate label patterns, all-predicate root multiplicities, root membership, and retained datatype summaries; it does not disable intrinsic SOC/OSC plane, root, fiber, or multiplicity metadata.
  Rationale: the default architecture must scale under heavy transactional invalidation and must expose the actual cost of ordered traversal, batching, morsel parallelism, and optional vector kernels. The earlier admitted-synopsis results remain diagnostic upper bounds and can still be reproduced by opting in.
  Date/Author: 2026-08-29 / Codex.

- Decision: Keep native memory addresses and CSF page/vector coordinates private to the cursor that owns the snapshot
  lease. The IR carries primitive lanes and opaque logical run handles; physical cursor overrides may decode or copy
  directly from mapped pages while filling those lanes.
  Rationale: this captures sequential access, prefetch, and zero-intermediate-object benefits without making every IR
  operator responsible for LMDB mapping lifetime, overlay invalidation, or use-after-close protection.
  Date/Author: 2026-08-30 / Codex.

- Decision: Do not promote a production Vector API backend in this change. Preserve the matched AArch64 experiment as
  evidence and keep scalar batch contracts ready for an independently certified backend.
  Rationale: three isolated kernels exceeded the 1.10x microbenchmark gate, but the dense intersection kernel reached
  only 1.09x, hardware instruction confirmation was unavailable, and no affected no-synopsis ANALYTICS query showed the
  required statistically credible end-to-end improvement.
  Date/Author: 2026-08-30 / Codex.

- Decision: Treat page traits as a physical proof scoped to one immutable page and one column, not as a general fact
  about a logical run or overlay-composed snapshot. Hoist a check to page grain only when the consumer can preserve
  exact bag/DISTINCT multiplicity and the visible source proves the trait remains authoritative.
  Rationale: a uniform root or neighbor term kind can safely reject or accept a complete base page, and a uniform
  literal datatype can bulk-count literal multiplicity after one representative lookup. A replacement run, tombstone,
  context restriction, or continuation can change the logical population, so blindly reusing the base flag would be a
  correctness bug rather than an optimization.
  Date/Author: 2026-08-30 / Codex.

## Outcomes & Retrospective

Implementation is complete. Every q0-q12 analytical shape has an adjacency lowering, and the acceptance measurements
use `rdf4j.lmdb.directAdjacency.synopsis.enabled=false`. q0/q1 run together at 0.037/0.058 ms. The matched q3-q12
geometric-mean speedup is 3,183.2x over forced LMDB; every individual query wins, q7/q8 return exactly 125/56 groups,
and q6/q7/q8 satisfy their historical five-times-faster ceilings at 5.988/8.197/40.454 ms in the complete sweep.

The final execution architecture uses plane/root/fiber grain, ordered SOC/OSC traversal, shared multiplicity vectors,
SIP-bounded typed root windows, dense/radix morsel-local breakers, and worker-local reductions. It does not expose raw
page addresses beyond their cursor lease and does not require the retained global synopses. CSF pages describe root and
neighbor columns independently with uniform term-kind and uniform literal-datatype traits. New referenced core literals
embed a stable datatype tag in their ID when the ordinal fits; legacy/custom literals remain exact through representative
or batched ValueStore header lookup.

The matched AArch64 Vector API experiment found useful isolated speedups but did not satisfy the complete production
promotion contract, so the shipping path remains scalar. Final JDK 25 verification passed 4,176 tests with no failures
or errors (103 skipped). The protected corpus and all benchmark, profiling, and first-failure artifacts remain intact.

## Context and Orientation

The work is confined to `core/sail/lmdb` plus benchmark support already located in that module. `NativeLmdbQuerySource` is the internal evaluation-source boundary. Its nested `NativeAdjacency` interface represents one fixed predicate and direction and owns immutable snapshot lifetime. `NativeAdjacency.KeyRunCursor` walks distinct roots while preserving the physical run handle. `LmdbDirectNativeAdjacency` implements that cursor over the paged CSF base plus exact retained-generation replacements and tombstones. `LmdbDecodedNativeAdjacency` implements the same contract over a decoded immutable CSF view.

`CompactCsfPageReader` understands one compressed page. It can derive a row's first and final fiber ordinal from `ROW_FIBER_STARTS`; their difference is the number of distinct neighbors in that page fragment. `ImmutablePagedQuadCsfIndex.KeyCursor` follows continuation pages for a logical root. It already sums `rowQuadCount` across that chain and must similarly sum row-fiber counts without decoding neighbor or context values.

`LmdbAdjacencyAggregatePlan` recognizes the current single-statement COUNT shapes. It is the first consumer to migrate: fixed-P group-O q3 must consume OSC root batches and their distinct-fiber count. `LmdbNativeKernelIr`, its interpreter, and `LmdbNativeKernelEmitter` are the common whole-query execution representation. The grain-aware nodes and batch requirements belong there, while adjacency-specific snapshot objects remain in `KernelContext`.

`LmdbNativeExchange` and the parallel aggregation classes currently exchange raw quad arrays. They remain the fallback for row-grain plans. Analytical kernels receive a second, bounded exchange carrying root/fiber batch ownership; the implementation must reuse worker buffers and partition by predicate plane plus root ordinal range.

The working tree was already dirty before this plan. Existing modifications in prefix-run, aggregate, range, type-matrix, benchmark, and test files are the starting point. Never restore, stash, clean, or overwrite them. All untracked evidence and benchmark artifacts must remain.

## Plan of Work

Milestone 1 adds the physical metadata contract. Add a regression to `LmdbDirectAdjacencyQueryTest` that rebuilds a fixed-P plane containing the same neighbor in multiple contexts, runs the q3-shaped grouped DISTINCT query, verifies the correct counts, and requires zero neighbor/context decodes. Capture the failing Surefire report before production edits. Then add `distinctNeighborCount()` to `NativeAdjacency.KeyRunCursor`, returning a negative unknown sentinel by default. Implement it for direct and decoded CSF root cursors by summing per-page row-fiber counts across continuation pages. Retained arena replacements may initially return unknown and use the exact bulk-decoding fallback. Change `LmdbAdjacencyAggregatePlan.groupedDirectional` to open root-only prefixes and consume the metadata when it is authoritative, falling back to its existing nested cursor otherwise.

Milestone 2 adds reusable batches and grain declarations. Define internal `PlaneBatch`, `RootBatch`, and `FiberBatch` carriers as primitive arrays with a mutable size and fixed capacity. A root lane contains root ID, predicate ordinal, snapshot-scoped run handle, quad multiplicity, distinct-neighbor count or unknown, and flags. A fiber lane contains neighbor ID and accepted-context multiplicity. Add bulk fill methods to the adjacency cursor contracts with allocation-free scalar defaults and coordinate-preserving CSF overrides. Extend IR nodes with a `Grain` enum and dependency metadata so validation rejects a filter or aggregate placed before its required columns or at a coarser grain than its semantics allow.

Milestone 3 lowers metadata and root-only analytics. q0 counts OSC `rdf:type` roots. q1 counts nonempty planes. q2 emits plane quad counts. q3 consumes OSC root distinct-fiber metadata. q4 emits OSC root counts per plane. q11 emits quad and both root counts per plane. q12 decodes each predicate once, evaluates namespace extraction once, and applies plane multiplicity. Each lowering must have plan telemetry and an exact generic comparison test.

Milestone 4 implements reusable sorted root/fiber algorithms. Add bounded unsigned merge, asymmetric gallop, and dense local mask kernels with a scalar oracle. Root/page batches expose an authoritative uniform `ValueIds` term kind and, when available, a uniform literal datatype. q5 rejects non-literal batches, aggregates inline and encoded-core-datatype batches at page grain, bulk-peeks legacy referenced literal headers through one ValueStore transaction per sorted batch, and retains only the resulting datatype-group multiplicities for an immutable complete base. q6 merges SOC roots and sums run multiplicities into a degree histogram. q9 merges OSC roots, applies the degree threshold, then classifies survivors. q10 intersects global SOC and OSC root domains, preferring a retained multiplicity synopsis plus a presence-only opposite-domain mask when the pair stays inside the 10 percent cap. Global structures proportional to all distinct IDs are otherwise forbidden; a dense mask is allowed only inside one morsel when its span is at most sixteen times cardinality and consumes at most 256 KiB per worker.

Milestone 4a introduces the persisted core-datatype reference ID under a store-format capability. Add an explicit, stable core-datatype tag table independent of enum ordinal. Prove round-trip/reopen behavior for a non-inline core literal, legacy-store minting, custom-datatype fallback, ordinal-overflow fallback, reference-count/reclamation behavior, and rejection by readers that do not support the bumped format. Update every reference/inlined/literal classifier to recognize the new reference family; the stored value record remains the source of the label and language tag.

Milestone 5 replaces q7/q8's specialist internals with factorized plane pipelines. For q7, process one predicate SOC plane at a time and merge its source roots with the fixed `rdf:type` SOC roots. Decode the type fiber once per matching subject and multiply type-statement and predicate-row multiplicities. For q8, bind source types once, bulk-decode the edge fibers, deduplicate targets within the bounded morsel, batch-lookup target type runs, and multiply source-type, edge, and target-type multiplicities into worker-local primitive counters. No global instance-to-type table or per-subject scan of every predicate is permitted.

Milestone 6 introduces root/fiber morsel execution. A morsel ends at the first of 8,192 roots, 65,536 fibers, or approximately 256 KiB of encoded input. Reuse the existing query-scoped executor and failure/cancellation machinery, but give each worker a deque of plane/root-ordinal ranges and reusable primitive batches. Split a supernode fiber only for associative kernels. Workers update private counter arrays or primitive maps and merge them at the aggregation breaker; the hot loop performs no global atomic updates.

Milestone 7 investigates the Vector API. First add benchmark-only implementations for packed decode, unsigned selection masks, adjacent transition counts, multiplicity sums, dense mask intersection, and block rejection. Run separate JMH forks with the module unresolved, resolved plus forced scalar, and resolved plus forced vector. Confirm AArch64 instructions with JIT evidence. Promote only winning operations behind an isolated `AdjacencyKernelBackend`; otherwise remove the production provider and preserve the rejection evidence.

Milestone 8 completes arbitration and verification. Cost plane, root, fiber, quad decode, value lookup, merge width, and scheduling overhead. Telemetry records grain, direction, metadata hits, payload decodes, set algorithm, morsels, steals, skew splits, scratch, and vector backend. Run focused regressions, the full JDK 25 LMDB module gate, matched JDK 26 q0-q12 JMH, allocation profiling, and plan snapshots. Retire old specialist code only after sequential, parallel, overlay, context, and fallback parity.

Milestone 9 audits and exploits the four independent CSF page traits across the rest of the branch. Build a source
matrix covering every production call that opens a `CompactCsfPageReader`, consumes `KeyRunCursor`/`BoundRunCursor`
root or fiber batches, copies neighbor IDs, or classifies RDF terms. For each consumer, record its physical grain,
column, predicate, whether the base-page trait remains authoritative for its visible snapshot, current decode count,
and the exact legal optimization: reject the page, accept the page, bulk-count its multiplicity, select a narrower
kernel, or no optimization. Headers must never be consulted as authoritative across an overlay replacement unless the
composed cursor supplies an exact composed trait. Capture no-synopsis JDK 26 JMH baselines before production edits.
For each selected consumer, add an instrumented regression that fails because the relevant header check or fast-path
counter is absent while preserving result parity for mixed pages, legacy literal IDs, encoded-core literal IDs,
continuation pages, and overlays. Implement the header check once outside the lane loop and keep representative ID or
ValueStore-header resolution at page grain. Retain a change only if repeated matched query measurements improve the
expected workload without regressing affected alternatives; otherwise revert that candidate while preserving the
benchmark evidence and audit conclusion.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j`. Tests use the repository runner, never Maven `-am` or `-q`:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyQueryTest#fixedPredicateDistinctNeighborUsesRootFiberMetadata --module core/sail/lmdb --retain-logs

Persist the initial red report immediately:

    python3 scripts/agent-evidence.py --command "python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyQueryTest#fixedPredicateDistinctNeighborUsesRootFiberMetadata --module core/sail/lmdb --retain-logs" --log <retained-verify-log> core/sail/lmdb/target/surefire-reports > initial-evidence.txt

After each production milestone, rerun the exact red selector before broader related classes. Run the module gate with:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Restore the preserved benchmark corpus only after clean builds are finished, using a non-destructive copy into the expected target directory and verifying the LMDB data-file sizes before JMH. Use `scripts/run-single-benchmark.sh` or the existing benchmark jar without `-am` or `-q`. Keep scalar/vector modes in different forks and result files.

## Validation and Acceptance

Correctness tests must cover ordinary and DISTINCT S/P/O counts, several aggregate channels in one query, duplicate contexts, default/named graph restrictions, explicit/inferred partitions, unsigned inline IDs, ranges, overlays, tombstones, retained snapshots, empty runs, and sequential/parallel parity. Instrumented tests must prove metadata-only execution does not access neighbor or context payloads.

All ANALYTICS results must match the generic engine. q7 and q8 must produce 125 and 56 groups. q0 through q12 must expose an adjacency candidate; selection telemetry may call it selected or outranked, but never structurally decline a supported shape.

On the preserved corpus and matched JDK 26 settings, q3 through q12 must achieve at least 10x geometric-mean speedup over forced LMDB. Every candidate must be faster than its control, q7/q8 must improve at least 5x, and q0-q2 must remain below 0.2 milliseconds. Fixed-P aggregation must allocate no object per statement. Scratch remains bounded by worker count, morsel limits, active predicates, and unavoidable output groups. New retained metadata must keep total direct-adjacency memory at or below 110 percent of the baseline.

For Milestone 9, compare each changed query against a same-revision pre-change benchmark built from the current dirty
worktree, with the same preserved corpus, JDK 26, heap, synopsis-disabled property, maintenance mode, warmup,
measurement, forks, and query parameters. Report the score and uncertainty before and after. The optimization is
accepted only when the expected query is repeatably faster, all affected semantic and telemetry tests pass, explicit
zero decode counts remain visible rather than being treated as unset, and no affected matched query regresses beyond
measurement noise.

## Idempotence and Recovery

Focused tests and builds are repeatable. `mvnf` performs the required install and keeps logs when requested. Never run `git clean`, `git reset --hard`, broad restore, or a manual stash. If a test overwrites a Surefire report, retain the first red evidence in `initial-evidence.txt` and the full log under `logs/mvnf`. If a benchmark process is active, wait rather than rebuilding or replacing its target. The preserved corpus in `/private/tmp` is read-only source evidence; never delete it.

## Artifacts and Notes

Initial build evidence is `maven-build.log`. The canonical preserved corpus is `/private/tmp/rdf4j-lmdb-theme-complete-20260829-5214259283`. Previous architectural evidence is retained in `.agent/lmdb-streaming-bidirectional-aggregation-execplan.md` and `.agent/evidence`; this plan restates every decision needed for the new implementation and does not depend on undocumented memory.

## Interfaces and Dependencies

`NativeLmdbQuerySource.NativeAdjacency.KeyRunCursor` gains an authoritative-or-unknown distinct-neighbor count and allocation-free root-batch fill. `NativeAdjacency.BoundRunCursor` gains the corresponding bound-run metadata where available. `LmdbNativeKernelIr` gains `Grain` plus plane/root/fiber producers that bind resources from the existing `KernelContext`. Scalar algorithms use only JDK primitive arrays and existing repository primitive maps. No new dependency or public RDF4J API is introduced. The only persisted-format change is the explicit, versioned core-datatype reference-ID capability; existing stores retain their legacy encoding and remain readable.

The optional Vector API provider may reference `jdk.incubator.vector` only in an isolated package and only after the benchmark promotion gates pass. Scalar code must have no static linkage to incubator classes. On AArch64, automatic selection requires the module to be present in `ModuleLayer.boot()` and the specific operation to have passed the recorded promotion gate.

Revision note: 2026-08-29 initial implementation plan created from the approved factorized-adjacency design. It incorporates the existing bidirectional aggregation work, the 10 percent memory limit, exact q7/q8 gates, root/fiber morsels, and an evidence-gated AArch64 Vector API investigation. The same day, the first root-metadata regression passed without a persisted-format change. Revised 2026-08-30 to add the branch-wide page-header consumer audit, strict snapshot-authority rules, test-first implementation, and matched before/after promotion gate requested after the initial q0-q12 completion.
