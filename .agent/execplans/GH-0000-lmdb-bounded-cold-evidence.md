# Improve LMDB Sketching with Bounded Cold Evidence

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md`. It is self-contained and is the implementation record for bounded cold filter evidence, snapshot identity, and evidence-policy isolation in the RDF4J LMDB query planner.

## Purpose / Big Picture

RDF4J's LMDB store already uses weighted Omni sketches, composite-key witnesses, context-aware statistics, incremental mutation handling, persisted snapshots, and learning from execution outcomes. Those facilities remain the foundation. The missing capability is a trustworthy, bounded source of correlated filter evidence immediately after a store opens, before adaptive learning has accumulated and without issuing a live sampling read during planning.

After this work, an LMDB store can bind every persisted or learned estimate to a durable store identity and committed mutation version. A query optimization scope explicitly chooses either snapshot-only evidence or adaptive evidence. When enabled, a deterministic cold synopsis retains a bounded bottom-k sample of complete statement rows and evaluates SPARQL filters with RDF4J's normal expression evaluator so subject, predicate, object, context, and repeated-variable correlations are preserved. The feature remains disabled by default unless the accuracy, coverage, footprint, rebuild, and planning-latency gates in this plan all pass.

The behavior is observable through focused tests, the 300-query estimate audit, persisted-restart tests, footprint diagnostics, and JMH benchmarks. Snapshot-only runs must never consume learned feedback or perform live sampling. Adaptive runs may use current-snapshot feedback and may perform live sampling only when the best available non-I/O estimate has confidence below `0.60`.

## Progress

- [x] (2026-07-12 12:03Z) Read `.agent/PLANS.md`, inspect the existing persistence-manager extraction, and run the mandatory root `-Pquick clean install`; the build completed successfully.
- [x] (2026-07-12 12:03Z) Create this dedicated ExecPlan before behavior-changing production edits.
- [x] (2026-07-12 12:58Z) Add and observe the smallest failing snapshot-identity, evidence-policy, and additive-configuration contract tests; all three produced focused Surefire errors for the deliberately absent APIs.
- [x] (2026-07-12 13:37Z) Implement durable 128-bit snapshot identity, committed mutation version, additive configuration, confidence/freshness selection, and query-scope policy propagation.
- [x] (2026-07-12 13:37Z) Replace filter and LEO sidecar size/mtime matching with exact snapshot-identity matching; gate learned filter and LEO read/training paths in snapshot-only mode while preserving adaptive correction and decay.
- [x] (2026-07-12 15:02Z) Add and observe deterministic bottom-k, publication, invalidation, persistence, corruption, long-value, context, and filter-semantics contract tests.
- [x] (2026-07-12 16:08Z) Implement the bounded cold synopsis, rebuild observer, checksummed persistence, RDF4J filter evaluation, evidence selection, and footprint diagnostics.
- [x] (2026-07-12 17:49Z) Extend the estimate audit with paired snapshot-only and adaptive runs over identical persisted data and queries; restore the default `spoc,posc` index set after the focused q17 contract exposed that forcing `spoc` alone invalidated the audit's cardinality assumptions.
- [x] (2026-07-12 18:05Z) Investigate the requested per-`rdf:type` subject sketch. Omni already maintains the requested logical `(type -> subject hashes)` posting in both its subject-star PO surface and reverse-edge predicate surface; adding a third standalone per-type sketch would duplicate evidence.
- [x] (2026-07-12 21:46Z) Run focused and LMDB unit verification, the 300-query audit, and the specified JMH promotion gates; keep capacity zero because accuracy and cold-planning latency gates fail.
- [x] (2026-07-12 21:46Z) Validate the follow-up god-class-split review, remove confirmed dead code/imports, add direct metadata/Omni coverage, and reject invalid fixed-order factor estimates consistently.

## Surprises & Discoveries

- Observation: The attached standalone source is useful only as clean-room design input because its archive contains no license metadata and it replaces RDF4J facilities with a custom parser, repair store, serialization layer, Bloom filter, and endpoint KMV.
  Evidence: Review of `/Users/havardottestad/.codex/worktrees/d5c4/rdf4j-small-things/spectra-adaptive-4.2.0/src.zip`; no source text will be copied.

- Observation: Small probes against the attached implementation exposed reservoir dilution, lost exactness at the KMV boundary, no structural sample below sixteen triples, and Java modified-UTF failure for a long legal RDF literal.
  Evidence: The clean-room review probe reported `globalReservoir.rolesWithSamples=16/2000`, `endpointKBoundary.complete=false`, `structuralCap.15=0`, `structuralCap.16=1`, and `longLiteral.write=UTFDataFormatException`.

- Observation: The current branch is already extracting persistence and Omni responsibilities from `SketchBasedJoinEstimator`; duplicating those components would conflict with live work.
  Evidence: The working-tree diff constructs `SketchEstimatorPersistenceManager` with the estimator and delegates metadata validation and Omni snapshot I/O to it.

- Observation: The mandatory root sanity build is green before this plan's changes.
  Evidence: `maven-build.log` ends with `BUILD SUCCESS`; the LMDB module completed in 8.587 seconds and the reactor completed in 55.852 seconds wall-clock time.

- Observation: Reflection-based contract tests allow a genuinely failing in-repository Surefire run before introducing additive Java types and methods, instead of stopping at test-compilation failure.
  Evidence: `SketchSnapshotIdentityTest#fixedWidthRoundTripPreservesStoreAndMutationIdentity` failed with `ClassNotFoundException: SketchSnapshotIdentity`; `SketchEvidencePolicyTest#snapshotOnlyDoesNotConsultLearnedOrLiveEvidence` failed with `ClassNotFoundException: SketchEvidencePolicy`; `LmdbStoreConfigColdEvidenceTest#defaultsAreAdaptiveAndColdSynopsisIsDisabled` failed with `NoSuchMethodException: LmdbStoreConfig.getSketchEstimatorEvidenceMode()`.

- Observation: Deferred mutations previously advanced neither the persisted evidence identity nor invalidated the old primary snapshot, so a quick shutdown could make a changed LMDB store reuse stale sketches and learned sidecars on restart.
  Evidence: `LmdbSailStoreEstimatorPersistenceTest#deferredMutationInvalidatesUnloadedPersistedSnapshot` failed with the original metadata identity still present. Moving mutation-version marking before readiness branches and deleting the primary snapshot when exact incremental updates are deferred made the focused test pass.

- Observation: A size/mtime collision is reproducible and demonstrates why file metadata is not sufficient evidence identity.
  Evidence: The stale filter sidecar test overwrites the current sidecar with old bytes and restores the old metadata timestamp while metadata size stays fixed. Exact snapshot identity rejects it; the test passes only after the mutation version is persisted.

- Observation: The deterministic cold synopsis meets its static footprint gates at maximum capacity before Java object headers: 6,144 retained rows occupy 294,912 published primitive bytes, the serialized payload is 294,968 bytes, and builder plus published primitive peak is 589,824 bytes.
  Evidence: `LmdbColdFilterSelectivityStatsTest` verifies the reported `SketchFootprint` values and the 512 KiB published / 1 MiB peak limits.

- Observation: Estimator metadata accidentally persisted the byte size of the snapshot directory as the approximate RDF statement count, producing trillion-scale join estimates after restart.
  Evidence: `SketchBasedJoinEstimatorPersistenceTest#persistedApproximateStoreSizeUsesStatementCountRatherThanSnapshotBytes` failed with expected `12` but actual `2,104,620`; persisting `approxStoreSize.get()` made the focused contract green.

- Observation: The paired audit initially forced the single `spoc` index, so the exact LMDB statement-cardinality provider had no predicate-led index and correctly returned store-wide bounds for bound-predicate patterns. This was an audit-fixture problem, not a persisted Omni-key problem.
  Evidence: Paired q17 reported every bound-predicate statement pattern as 408 rows. A proposed stable key-scope change did not affect the failure and was reverted. Running the same persisted audit with LMDB's normal `spoc,posc` indexes passes.

- Observation: The requested per-type subject sketch already exists as generic Omni state. For every statement the ingest path adds the subject hash to `SUBJECT_STAR[orderedTupleHash(predicate, object)]` and to `EDGE_REVERSE[predicateKey][objectHash]`. For `rdf:type`, both are logically `type -> sampled subjects`.
  Evidence: `SketchBasedJoinEstimator.updateOmniJoinEstimator` performs both updates, and `estimateOmniPatternWitnesses` probes the subject-star PO posting for a bound predicate/object subject pattern. Both postings are exact while small and compact large postings into bottom-k witnesses.

- Observation: Predicate/context `SketchTermKey` values used a process-local `ValueStoreRevision` identifier as their persisted scope, making mapped edge sketches unreachable after restart even though the payload loaded successfully.
  Evidence: The persisted paired q7 contract failed at q-error 18 while the same full-rebuild state passed before shutdown. Keeping the revision only as the thread-local cache epoch and persisting LMDB ID keys in scope zero made both persisted q7 and q17 contracts green.

- Observation: The preserved non-experimental 300-query audit remains within its q-error ceiling, while the paired opt-in synopsis run fails promotion: snapshot-only p95 is 4.0, worst is 17.333, and it produced two planner-level false exact zeros; adaptive p95 is 4.0, worst is 9.0, and its false exact zeros varied from zero in an earlier run to four in the final unit run.
  Evidence: `LmdbEstimateAuditHarnessTest#auditsEntireGeneratedCorpusAcrossNestedPieces` passes all 300 queries and nested pieces. The final `[lmdb-paired-audit]` line in `logs/mvnf/20260712-211316-verify.log` reports four adaptive false exact zeros and verifies the production default remains zero when a promotion gate fails.

- Observation: Cold-covered filter planning is slower than a deliberately uncached live LMDB sample in the current prototype.
  Evidence: JDK 25 JMH measured `coldCoveredFilter` at 2,298.512 microseconds/op and `uncachedLiveFilter` at 1,094.531 microseconds/op on 20,000 rows. `target/benchmark-results/cold-vs-live.md` reports live as 52.381 percent faster; promotion required cold to be at least twice as fast.

- Observation: The cold rebuild observer adds about 7.5 percent at 20,000 rows, within the provisional 10-percent gate, while maximum-capacity footprint remains within its static byte limits.
  Evidence: `lmdbRebuildOnce` measured 43,546.590 microseconds/op at capacity zero and 46,808.421 at capacity 6,144. The three-iteration error bars are wide, so this is provisional rather than promotion-quality evidence.

- Observation: The existing 1-million-row incremental Omni benchmark exhausts its fixed 1 GiB heap independently of the cold synopsis. A diagnostic heap dump contains 2.27 million `ValueWeights` objects (207.8 MiB shallow) plus roughly 577 MiB of their `long[]` and `double[]` storage.
  Evidence: `incrementalBatchFlush` failed with `OutOfMemoryError: Java heap space` in `AttributeIndex.updateHash` through `updatePredicateContext`. `/tmp/java-oom-root-cause/20260712-213556/rerun/oom-heap-71546.hprof` and the parsed histogram classify retained exact Omni postings as the dominant heap consumer. The same control at 100,000 rows measures 498.706 milliseconds/op.

- Observation: The follow-up review correctly identified three orphan guarantee collaborators, a self-contained dead path-endpoint helper cluster, eleven unused imports, missing direct Omni arithmetic tests, and inconsistent invalid factor-estimate handling. Its claims that metadata validation and the owned-rule guard had zero coverage were overstated: version/strategy snapshot tests and focused DPhyp guard assertions already existed.
  Evidence: Repository-wide symbol searches found no callers for `LmdbGuaranteeCostEstimator`, `LmdbGuaranteeDiagnostics`, or `LmdbGuaranteeMaterializer`; the invalid-cost contract initially failed with a `NullPointerException` after NaN output was accepted. `LmdbCascadesConnectedRuleAdmissibilityTest` already asserts guard behavior with DPhyp enabled and disabled.

- Observation: Complete LMDB Surefire verification is green after the review fixes, while the broader Failsafe phase still contains branch-level physical-plan expectation failures unrelated to cold evidence.
  Evidence: `logs/mvnf/20260712-211316-verify.log` reports 2,153 tests, zero failures, zero errors, and 36 skipped with `-DskipITs`. A prior full `verify` reached Failsafe after 2,148 green unit tests and reported eight plan-shape failures across medical, pharma, social, train, and flagged-theme regression ITs before its command wrapper was closed without an aggregate Maven result.

## Decision Log

- Decision: Retain the current weighted Omni estimator and all existing bag, Omni-surface, and filter-pass estimate contracts.
  Rationale: The current estimator handles joins, contexts, mutation lifecycle, persistence, and RDF4J algebra integration more completely than the standalone source. The new feature addresses cold filter evidence, not join-estimator replacement.
  Date/Author: 2026-07-12 / Codex

- Decision: Treat the attached archive as clean-room inspiration only and implement all code against RDF4J's existing types and semantics.
  Rationale: The archive has no license metadata, and copying its custom infrastructure would weaken rather than improve the current architecture.
  Date/Author: 2026-07-12 / Codex

- Decision: Implement in test-first vertical slices even though the overall routine is Routine D.
  Rationale: Repository policy requires the smallest failing in-repo test before each externally observable behavior change. The ExecPlan coordinates the significant refactor, while tests gate each slice.
  Date/Author: 2026-07-12 / Codex

- Decision: Bind evidence to a persistent random 128-bit store identifier plus a committed mutation version, not file metadata.
  Rationale: File size and modification time are neither unique nor transactionally coupled to RDF statements. A store identifier prevents cross-store reuse; the committed version prevents reuse after mutation.
  Date/Author: 2026-07-12 / Codex

- Decision: Keep cold synopsis capacity zero by default until every promotion gate passes on JDK 25.
  Rationale: This provides the identity, policy, diagnostics, and tests without silently changing production planning when accuracy or performance evidence is incomplete.
  Date/Author: 2026-07-12 / Codex

- Decision: Store complete primitive LMDB ID tuples and evaluate filters through RDF4J's existing expression evaluator.
  Rationale: Complete rows retain intra-row correlation and avoid invented SPARQL semantics. Primitive arrays bound allocation and avoid per-entry object and canonical-string overhead.
  Date/Author: 2026-07-12 / Codex

- Decision: Snapshot-only mode also suppresses LEO training and plan-candidate recording, not only LEO reads.
  Rationale: This prevents a snapshot-only audit from contaminating a later adaptive run and makes strict evidence isolation observable. Adaptive mode retains the existing learning, confidence, correction, and decay behavior.
  Date/Author: 2026-07-12 / Codex

- Decision: A mutation received while sketches are unloaded invalidates the primary snapshot immediately.
  Rationale: There is no safely publishable updated sketch state in that branch. Deleting metadata forces a rebuild from authoritative LMDB data and prevents stale identity reuse across restart.
  Date/Author: 2026-07-12 / Codex

- Decision: Do not add a third dedicated per-type subject sketch as part of this work.
  Rationale: The existing reverse-edge and subject-star PO postings already implement the requested value-to-subject bottom-k synopsis for every predicate/object pair, including `rdf:type`. A future improvement should first measure and consolidate that duplicate state, then consider a smaller type-specific retention policy only if type-heavy workloads prove the existing 2,048-witness nominal size is too costly.
  Date/Author: 2026-07-12 / Codex

- Decision: Keep cold-synopsis capacity zero in the production default.
  Rationale: Although deterministic correctness, persistence, primitive footprint, and provisional rebuild overhead pass, the paired snapshot audit has false-zero/worst-q-error failures and cold planning is about 2.1 times slower than uncached live sampling. The explicit promotion rule requires every gate, so no default activation is permissible.
  Date/Author: 2026-07-12 / Codex

- Decision: Apply only the confirmed bounded findings from the follow-up refactor review.
  Rationale: Removing unreachable classes/helpers/imports is behavior-neutral, direct metadata and Omni tests close real coverage gaps, and rejecting non-finite/negative factor estimates fixes a demonstrated branch divergence. Collapsing pass-through facades would overlap ongoing extraction work without fixing a bug. Restoring the deleted fixed-plan Cartesian override would recreate a second planner authority; any future Cartesian-risk policy belongs in a dedicated surviving Cascades cost-model design with its own red planner contract.
  Date/Author: 2026-07-12 / Codex

## Outcomes & Retrospective

Milestones one through five are implemented. A fixed-width 24-byte identity persists two random store-ID words and the captured mutation version in estimator metadata. Filter and LEO sidecars use exact identity, timestamp-only changes no longer invalidate them, forced size/mtime collisions cannot revive stale filter evidence, and unloaded/deferred mutations delete the unusable primary snapshot. Snapshot-only filter and LEO isolation is green; adaptive live sampling occurs only below confidence `0.60`.

The package-private `ColdFilterSynopsis` retains at most 6,144 complete primitive LMDB rows with deterministic bottom-k selection, fixed-width checksummed persistence, exact identity, mutation invalidation, RDF4J-native expression evaluation, 32-row support eligibility, and the existing 256-observation sampled-zero safeguard. Focused tests cover deterministic rebuild order, complete/incomplete evidence, restart, corrupt/truncated input, a 70,000-character legal literal, numeric promotion above `2^53`, language tags, regex/boolean precedence, correlated values, named contexts, mutation, and footprint limits. The paired q17 snapshot/adaptive restart contract is green under the normal LMDB index set.

The requested per-type side quest requires no third structure: existing Omni state already maintains two equivalent logical type-to-subject witness surfaces. The restart-key fix makes those persisted surfaces reachable. The 1-million-row incremental OOM also sharpens the follow-up: consolidate the duplicate surfaces and replace per-value object-heavy exact retention with a truly bounded primitive policy before adding any type specialization.

The preserved 300-query baseline passes. Paired policy measurements and JMH reject promotion, so capacity remains zero. Focused cold, policy, identity, persistence, review-parity, metadata-validator, and Omni-service suites are green. The final LMDB Surefire gate passes 2,153 tests with zero failures or errors; formatting and diff checks pass. Full Failsafe is not green because eight existing physical-plan regression assertions remain in the dirty planner branch, and the earlier full-verify wrapper exited without an aggregate Maven summary after recording those reports.

## Context and Orientation

The relevant Maven module is `core/sail/lmdb`. `LmdbSailStore` wires the LMDB value store, triple store, query statistics, sketch estimator, filter sampling, and learned operator feedback. `LmdbStoreConfig` owns additive store configuration. `LmdbEvaluationStatistics` exposes statistics to RDF4J query optimizers.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/SketchBasedJoinEstimator.java` is the existing estimator facade. It owns the published sketch state and currently selects learned, sampled, and heuristic filter evidence. It is large, so this plan moves only filter-evidence selection into a focused collaborator and builds on the branch's existing `SketchEstimatorIngestService`, `SketchEstimatorPersistenceManager`, `SketchOptimizationScope`, `SketchJoinOrderService`, `FrequencySketchEstimator`, and `OmniSketchEstimatorService` extractions.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbFilterSelectivityStats.java` owns live LMDB filter sampling and RDF value materialization. It will also own the cold synopsis because the synopsis stores internal LMDB value IDs, which require the value and triple stores to turn into RDF4J values and complete statement bindings.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbOperatorFeedbackStats.java` stores learned execution feedback, including LEO corrections. LEO means learning from execution outcomes: measured execution cardinalities adjust later estimates and decay over time. This logic remains intact, but reads and writes must be gated by evidence policy and exact snapshot identity.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/SketchEstimatorMetadata.java` is the persisted sketch metadata header. `SketchEstimatorPersistenceManager.java` coordinates publication, and `PersistenceMutationCycle.java` tracks mutations covered by a published snapshot. Snapshot identity in this plan means a value containing two random 64-bit words that identify the LMDB store and one non-negative 64-bit committed mutation version. Two identities match only when all three words match.

An evidence policy is an internal planning rule. `SNAPSHOT_ONLY` may use exact store statistics, current persisted sketches, the immutable cold synopsis, and deterministic heuristics. It must not consult LEO or filter-learning sidecars and must not issue live sampling reads. `ADAPTIVE` may consider current-snapshot learned and cached evidence, rank candidates by confidence and freshness, and issue a live sample only if the strongest non-I/O candidate has confidence below `0.60`.

The cold synopsis is a deterministic bottom-k sample. Every complete statement ID tuple receives a stable unsigned 64-bit rank hash and a second stable tie-break hash. The builder keeps the lexicographically smallest `(rank, tieBreak, subject, predicate, object, context)` tuples up to its configured capacity. Because selection depends on tuple values rather than scan order, rebuilding the same snapshot in a different order produces the same retained set. The published form stores parallel primitive arrays and no statement, value, string, collection-entry, or per-row wrapper objects.

The attached standalone implementation was previously extracted only for review under `/tmp/spectra-adaptive-4.2.0-review`. It is not a build input and is not required to execute this plan.

## Plan of Work

Milestone one establishes exact evidence identity and policy boundaries. First add package-level contract tests for a new immutable `SketchSnapshotIdentity`, persistence mutation versions, sidecar rejection across mutations and stores, and query scopes that forbid learned or live evidence in snapshot-only mode. Run the narrowest tests and retain the failing Surefire reports before editing production. Then add the identity to estimator metadata, restore it when a snapshot loads, publish the captured mutation target only after a successful metadata transaction, and expose the current committed identity through the estimator. Extend `LmdbStoreConfig` additively with evidence mode and cold-synopsis capacity. Carry the configured mode in `SketchOptimizationScope`. Replace filter and LEO sidecar file-size/mtime scopes with identity values. Re-run the same selections and then the affected persistence tests.

Milestone two creates the bounded data structure independently of LMDB. Add failing tests for determinism across input order, unsigned bottom-k selection and full-tuple collision tie-breaking, capacities zero and one, completeness when total rows do not exceed capacity, sample incompleteness above capacity, exact footprint accounting, bounded serialization, checksums, truncated/corrupt rejection, and primitive long values. Implement `ColdFilterSynopsis` as a package-private class outside the facade with a mutable builder and an immutable published snapshot. The default production capacity remains zero. The builder may allocate parallel `long[]` arrays for rank, tie-break, subject, predicate, object, and context; it must not allocate one object per retained statement. Its serialized format uses fixed-width or bounded length-prefixed primitives and a checksum, never `DataOutput.writeUTF` and never reflection.

Milestone three integrates publication with full rebuilds. Add failing tests in the existing rebuild and persistence suites for successful publication, mutation during rebuild, failed rebuild, add/delete invalidation, no synopsis-only rebuild, restart, stale identity, and corrupt or truncated payload recovery. Add a narrow optional rebuild-observer interface to `SketchEstimatorIngestService` or the facade's existing rebuild seam. `LmdbFilterSelectivityStats` supplies an observer only when capacity is positive. The observer captures the starting mutation version, accepts complete LMDB statement ID rows during the existing scan, and publishes only if the scan completes successfully and the version remains unchanged. Any later successful store commit invalidates the published synopsis. Invalid or missing synopsis payloads fail closed and leave Omni snapshots usable.

Milestone four integrates RDF4J filter evaluation and evidence selection. Add RDF4J-native failing cases for numeric promotion beyond `2^53`, language-tag matching, regex and boolean precedence, correlated repeated variables, OPTIONAL, MINUS, EXISTS, named contexts, long literals, mutation, restart, and feedback isolation. A synopsis estimate is eligible when at least 32 retained rows match the statement-pattern bindings. A sampled zero is never presented as exact unless the underlying complete population proves zero; incomplete zero evidence retains the current minimum requirement of 256 observations and the existing Wilson-bound behavior in `FilterPassEstimate`. Evaluate the prepared condition with RDF4J's existing evaluation strategy. Move candidate collection and confidence/freshness selection from `SketchBasedJoinEstimator.estimateFilterPass` into a focused filter-evidence service without changing `BagEstimate`, `OmniSketchSurfaceEstimate`, or `FilterPassEstimate` public contracts.

Milestone five extends diagnostics and the paired audit. Add `SketchFootprint` diagnostics reporting exact published allocated bytes, exact serialized bytes, retained rows, total observed rows, completeness, builder-plus-published peak bytes, and predicate-support quantiles. Extend `LmdbEstimateAuditHarness` so the same generated data and query list run once under snapshot-only policy and once under adaptive policy. Report supported-filter p50/p95 q-error, worst q-error, interval coverage, exact-zero mistakes, live-sampling count, and cold-synopsis support.

Milestone six measures and decides promotion. Verify the full LMDB module with `mvnf`. Run the 300-query audit and preserve its worst q-error ceiling of 10. Build the benchmark artifact and use `scripts/run-single-benchmark.sh` for `rebuildOnceSlow`, `incrementalBatchFlush`, existing Omni probes, and a new cold-filter benchmark comparing covered cold planning with an uncached live scan. Use JFR only if a result moves materially, is unstable, or fails a gate. Promote the configured default capacity to at most 6,144 only when all promotion gates pass; otherwise retain zero and record the failed gates here.

## Concrete Steps

All commands run from `/Users/havardottestad/Documents/Programming/rdf4j-small-things` unless stated otherwise. Never use Maven `-am` or `-q` when tests run. Every Maven invocation uses `-Dmaven.repo.local=.m2_repo`, and tests run offline unless dependency resolution proves that impossible.

The mandatory initial build has already run:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o \
      -Dmaven.repo.local=.m2_repo -Pquick clean install 2>&1 \
      | tee maven-build.log \
      | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'

Expected baseline tail:

    [INFO] RDF4J: LmdbStore ................................. SUCCESS
    [INFO] BUILD SUCCESS

For each behavior-changing slice, add the narrowest test with the exact 2026 RDF4J source header and the line `// Some portions generated by Codex`, then run the source-header check:

    (cd scripts && ./checkCopyrightPresent.sh)

Run the new identity and policy tests first and expect them to fail before production changes:

    python3 .codex/skills/mvnf/scripts/mvnf.py SketchSnapshotIdentityTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbSketchEvidencePolicyTest --retain-logs

Capture the failing summary from `core/sail/lmdb/target/surefire-reports/` in the running task before implementing the slice. After implementation, run the exact same selections and expect zero failures.

Run the standalone synopsis contracts before LMDB integration and expect the first run to fail:

    python3 .codex/skills/mvnf/scripts/mvnf.py ColdFilterSynopsisTest --retain-logs

After the data structure passes, add and run focused integration selections such as:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbColdFilterSelectivityStatsTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py SketchJoinEstimatorPersistenceTest --retain-logs

The exact class names may be aligned with existing test naming discovered during implementation, but every rename must be recorded in `Decision Log` and these commands must be updated.

Run format and import ordering only after tests are green:

    (cd scripts && ./checkCopyrightPresent.sh)
    mvn -o -Dmaven.repo.local=.m2_repo -T 2C process-resources

Run the complete LMDB verification through the preferred runner:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Locate the 300-query audit method with `rg -n "300|EstimateAudit" core/sail/lmdb/src/test` and run the narrowest matching class or method through `mvnf`. Preserve the generated paired snapshot-only/adaptive result artifact.

Build the benchmark artifact with the repository helper's documented workflow and run one method at a time:

    ./scripts/run-single-benchmark.sh SketchEstimatorIngestionBenchmark.rebuildOnceSlow
    ./scripts/run-single-benchmark.sh SketchEstimatorIngestionBenchmark.incrementalBatchFlush
    ./scripts/run-single-benchmark.sh OmniJoinEstimatorBenchmark.exactSingleValueProbe
    ./scripts/run-single-benchmark.sh ColdFilterSynopsisBenchmark.coldCoveredFilter
    ./scripts/run-single-benchmark.sh ColdFilterSynopsisBenchmark.uncachedLiveFilter

Record raw benchmark output paths and calculate ratios from matching forks and parameters. Do not infer performance from unit-test duration.

## Validation and Acceptance

Identity is accepted when a persisted store keeps the same two 64-bit store-identity words across restart, increments the committed version only for successfully published mutations, and rejects filter or LEO sidecars from another store or an earlier mutation. A failed or interrupted publication must not advertise a newer identity.

Policy isolation is accepted when snapshot-only optimization returns estimates without reading learned feedback and without invoking live sampling, while adaptive optimization considers current-snapshot learned/cached evidence and invokes live sampling only when every available non-I/O candidate has confidence below `0.60`. Tests must use invocation counters or fail-fast providers, not timing, to prove the absence of calls.

The synopsis is accepted when equivalent statement sets rebuilt in different orders serialize to identical retained tuples, all rows are present when total rows are at most capacity, no more than capacity rows are retained otherwise, and a second hash plus full tuple produces deterministic ordering even under forced rank collisions. Capacity zero performs no retention and is the default until promotion.

Publication is accepted when only an unchanged, successfully completed full scan becomes visible. A concurrent mutation, failure, add, or deletion invalidates the synopsis. Invalidation must not force a rebuild; adaptive mode falls back to existing evidence. Restart loads only a checksum-valid payload with exactly matching snapshot identity. A corrupt, oversized, or truncated payload is ignored or quarantined without preventing the primary sketch snapshot from loading.

SPARQL behavior is accepted when RDF4J-native tests cover numeric promotion above `2^53`, language matching, regex precedence, correlated filters, OPTIONAL, MINUS, EXISTS, named contexts, legal long literals, mutations, persistence, and feedback isolation. Eligible estimates require at least 32 matching retained rows. Incomplete zero-hit evidence cannot become an exact zero, retains the existing 256-observation safeguard, and exposes a non-zero upper uncertainty bound.

Diagnostics are accepted when `SketchFootprint` byte totals equal the arrays and serialized payload actually allocated/written, retained and total row counts are exact, completeness is correct, builder-plus-published peak is reported, and predicate-support quantiles are deterministic.

The synopsis can become active by default only if one measurement set on JDK 25 satisfies every gate: supported-filter snapshot-only p95 add-one q-error improves at least 20 percent over snapshot-only heuristics; adaptive p95 regresses no more than 5 percent from the current adaptive baseline; there are zero false exact-zero estimates; empirical 95 percent interval coverage is at least 93 percent; published footprint is at most 512 KiB; builder plus published peak is at most 1 MiB; rebuild time regresses less than 10 percent; unaffected Omni microbenchmarks regress less than 5 percent; covered cold-filter planning is at least twice as fast as an uncached live scan; and the full 300-query audit keeps worst q-error at or below 10.

If any promotion gate fails or cannot be measured reproducibly, acceptance is still possible for the identity, policy, diagnostics, persistence, and disabled prototype, but `LmdbStoreConfig` must retain cold-synopsis capacity zero by default and this plan must name the failed or missing gates.

## Idempotence and Recovery

All tests, rebuilds, audit runs, and benchmarks are repeatable. Bottom-k selection is deterministic for the same complete tuple set. Sidecar writes use an atomic temporary-file-and-replace pattern already used by the module where available; an interrupted write leaves the last valid payload or no payload.

Do not delete untracked files or reset the dirty working tree. Existing branch edits belong to the user and must be preserved. If a new change overlaps the facade-extraction work, read the current diff and extend its seam rather than restoring an older version.

If a red test unexpectedly passes, strengthen it until it distinguishes the missing behavior before editing production. If production code is accidentally edited before observing the required red test, revert only that new hunk with `apply_patch`, update `Progress`, and restart that slice from the test. If offline dependency resolution fails, rerun the exact build once without `-o`, then return to offline operation.

If synopsis loading fails, fail closed: discard only the synopsis and retain primary sketch loading. If publication sees a changed mutation version, discard the candidate builder without touching the previously invalidated synopsis and do not initiate another scan. If benchmarks are noisy, increase forks or measurements using the helper and record both raw runs; add JFR only for a material or failing gate.

## Artifacts and Notes

The pre-change build artifact is `maven-build.log`. Focused `mvnf --retain-logs` runs write under `logs/mvnf/`, and Surefire reports are under `core/sail/lmdb/target/surefire-reports/`. The existing top-level `initial-evidence.txt` predates this plan and must not be overwritten; retain new evidence in the task transcript and uniquely named logs/artifacts.

The clean-room source-review probe is contextual evidence only. Its useful design lessons are deterministic selection, strict evidence isolation, explicit snapshot identity, checksum-bounded primitive serialization, and truthful footprint reporting. Its implementation code is not an artifact of this plan.

## Interfaces and Dependencies

No new dependency and no runtime code generation may be introduced. Use JDK 25 primitives and existing RDF4J/LMDB APIs.

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/sketch/SketchSnapshotIdentity.java`, define an immutable value with this conceptual API, adjusting visibility only as package ownership requires:

    public record SketchSnapshotIdentity(long storeIdHigh, long storeIdLow, long mutationVersion) {
        public boolean sameStore(SketchSnapshotIdentity other);
        public void writeTo(DataOutput output) throws IOException;
        public static SketchSnapshotIdentity readFrom(DataInput input) throws IOException;
    }

Validate that mutation version is non-negative. Store IDs may contain any bit pattern; a newly created store uses `UUID.randomUUID()` once, converts it to two longs, and persists those words. Do not regenerate them on restart.

In the sketch package, define an internal evidence enum with exactly `SNAPSHOT_ONLY` and `ADAPTIVE`. `LmdbStoreConfig` gains additive getter/setter configuration for this mode and a cold-synopsis capacity in the inclusive range zero through 6,144. Zero disables construction and persistence. The configured policy is copied into `SketchOptimizationScope.State` when query optimization begins.

Extend `SketchEstimatorMetadata` with the two store-ID longs, the committed mutation version, and any bounded synopsis reference needed by the persistence store. Increment its format version. `SketchEstimatorPersistenceManager` writes metadata containing the captured target version only after all payloads and indexes flush successfully, then calls `PersistenceMutationCycle.markPersistedThrough(target)`. Snapshot load restores the identity and mutation-cycle floor before evidence providers can read sidecars.

Change `LmdbFilterSelectivityStats` and `LmdbOperatorFeedbackStats` to receive a current-identity supplier or equivalent narrow capability from `LmdbSailStore`. Their sidecar header stores all three identity longs. A missing identity disables persistence and learned reuse; it never falls back to size/mtime matching.

In `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ColdFilterSynopsis.java`, define a package-private immutable snapshot and package-private builder. The snapshot exposes row count, total row count, completeness, primitive ID access needed by `LmdbFilterSelectivityStats`, serialization, and `SketchFootprint`. The builder accepts `(subjectId, predicateId, objectId, contextId)`, computes rank and tie-break internally, and retains at most the configured capacity in primitive arrays.

Introduce one narrow optional rebuild-observer interface at the existing full-rebuild boundary. It has begin, accept-complete-row, complete, and abort semantics; names may follow the existing ingest service. The full scan must not know filter expressions or value materialization. `LmdbFilterSelectivityStats` owns the observer implementation and publishes only after it verifies the starting mutation version still matches.

Create a focused filter-evidence selector outside `SketchBasedJoinEstimator`. It consumes exact/current sketch, cold, learned, cached, live, and heuristic candidates as suppliers so forbidden sources are not invoked. It returns the existing `FilterPassEstimate`. For adaptive policy it compares confidence and freshness before I/O; it invokes the live supplier only when the best non-I/O confidence is less than `0.60`. For snapshot-only it never evaluates learned or live suppliers.

Add `SketchFootprint` as an immutable diagnostic with at least published allocated bytes, serialized bytes, retained rows, total rows, completeness, builder-plus-published peak bytes, and predicate-support quantiles. Byte calculations must derive from actual primitive widths, array lengths, and format fields rather than estimated Java object sizes.

Revision note (2026-07-12 12:03Z): Created the initial self-contained implementation plan after reviewing the current dirty facade/persistence extraction and completing the mandated green root build. The plan deliberately makes the cold synopsis opt-in until quantitative promotion evidence exists.

Revision note (2026-07-12 12:58Z): Recorded the first three focused red contracts and their Surefire evidence before beginning identity or policy production changes.

Revision note (2026-07-12 13:37Z): Completed the snapshot-identity and evidence-policy milestone, recorded the deferred-mutation root cause and strict LEO isolation decision, and preserved the focused green suite counts.

Revision note (2026-07-12 18:05Z): Recorded the completed cold-synopsis and paired-audit implementation, the persisted row-count regression and audit-index diagnosis, maximum-capacity footprint, and the per-type subject-sketch finding.

Revision note (2026-07-12 21:48Z): Recorded the durable LMDB-key fix, preserved and paired audit results, JMH promotion failure, rebuild controls, incremental Omni heap exhaustion evidence, final disabled-default decision, and refined per-type follow-up.

Revision note (2026-07-12 21:46Z): Closed implementation and Surefire verification, recorded the disabled promotion outcome and residual Failsafe plan-shape failures, and incorporated the validated follow-up review cleanup and coverage contracts.
