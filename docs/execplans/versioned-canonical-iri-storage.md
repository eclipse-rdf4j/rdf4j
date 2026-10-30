# Persist a canonical IRI encoding in LMDB format 7

This living ExecPlan follows `.agent/PLANS.md`. Update Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective as evidence arrives.

## Purpose / Big Picture

New LMDB stores must store an IRI (an RDF identifier) in exactly one representation of its full lexical string, regardless of the namespace/local-name boundary supplied by its Java implementation. A dictionary miss can then establish absence without searching alternative namespace prefixes. Released format 2 stores retain their encoding and compatibility lookup behavior. Unreleased format 6 is unsupported and must be rejected before writable native storage is opened. This change performs no data migration and does not rewrite namespace references.

## Progress

- [x] (2026-10-29) Inspect main and confirm format availability.
- [x] (2026-10-29) Reproduce canonical invariant with focused tests.
- [x] (2026-10-29) Implement persisted policy and shared decomposition.
- [x] (2026-10-29) Cover scalar snapshot and bulk encoding.
- [x] (2026-10-29) Verify real old-binary round-trip compatibility.
- [x] (2026-10-29) Measure the initial implementation and validate 751 related regression cases.
- [x] (2026-10-29) Apply the user clarification: support only formats 2 and 7, reject 6, and pass 864 regressions across 37 classes.
- [x] (2026-10-29) Measure the final 2/7 implementation against the isolated original format-6 binary: 440 observations with all canonical alternative counters zero.
- [x] (2026-10-29) Publish the reviewed branch and draft pull request #144; package complete changed source files and a binary-safe patch.

## Surprises & Discoveries

New-format initial imports use `.lmdb-initial-import-v7.incomplete`, because the old format 6 binary deletes its old marker before checking format metadata. A forced, version-tagged reset state inside that marker makes another interruption during reset recoverable without discarding the format sidecars. Standalone bulk checkpoints now use state schema 3, and publication markers/manifests use publication schema 3; both reject old-loader interpretation before destructive recovery. Historical checkpoint schema 2 and publication schema 2 belong to unreleased format 6 and are now rejected before cleanup or publication. These schema numbers are distinct from the store format number.

The starting commit is `db572a45a401d9e7284b321efba0adbd6d577e6c` on `HASMAC-AS/rdf4j/main`, where `LmdbStoreFormat.CURRENT_VERSION` was 6. Native retirement metadata contains its own version fence, so preserving only `store.properties` would be insufficient. The original initial-import recovery deleted the interrupted generation before version validation; validation and the recovered format must survive that reset.

The environment initially provided Java 17 and no Maven. The project requires Java 25. Eclipse Temurin 25.0.5+8 and Maven 3.9.11 were installed outside the repository, with dependencies cached in the repository's `.m2_repo` as required by `AGENTS.md`.

The first targeted runtime run executed 406 tests with nine assertion failures and no errors or skips. Canonical identity, collision, rollback/reuse and bulk invariants passed. Three snapshot assertions used legacy-only payload or scalar-only observation, and four prepared-store assertions lacked a real namespace snapshot in their setup. The remaining two failures exposed an invalid initial format-2 fixture: the actual 6.1.0 binary could not reopen its own output with the producer's implicit index settings. Fixture generation now explicitly supplies matching historical index descriptors and verifies a fresh original-binary reopen before packaging. No production metadata compatibility rule is relaxed.

The corrected release fixture also exposed timestamp-only metadata churn when discovery temporarily sorted index descriptors and initialization restored their original order. `StoreProperties.save` now retains the original sidecar bytes when the complete effective property map equals the loaded map. Actual changes still use the existing atomic write path. Additional focused tests failed before rejecting non-regular metadata (9 failures) and dangling owner sidecars (4 failures); the completed policy suite passes all 62 cases.

The first run after narrowing support to 2 and 7 passed 329 of 330 cases. The paired snapshot/remap fixture had lost its original object-leading `ospc` index when it switched from an archived format-6 store to a newly created format-7 store. Without that index, bounded authoritative liveness correctly requeues uncertain object IDs; the drain helper timed out, including in an isolated serial retry. Restoring the original `spoc,posc,ospc` configuration fixes the fixture without changing production code, assertions or the timeout. The revised lifecycle suite passes all 13 cases, and the full snapshot-lifetime suite passes all 40 cases.

## Decision Log

Use persisted format 7 and retain released legacy format 2; derive canonical IRI capability only from the format version. No runtime setting can enable or disable this invariant. Format 7 was unallocated on the inspected main. The user clarified on 2026-10-29 that format 6 was an unreleased intermediate format and need not be supported. This supersedes the original 2/6/7 dispatch: reject 6 safely, including prepared stores, direct dictionary constructors and interrupted bulk imports. Keep an authentic format-6 fixture only for byte-preserving rejection tests, and its original binary only as a performance baseline and forward-rejection check.

Provide one `IriStorageEncoding.split(IRI, LmdbStoreFormat)` helper returning a `Split(namespace, localName)` record. Format 7 derives both from `IRI.stringValue()` and `URIUtil.getLocalNameIndex`, while format 2 retains caller getters. This keeps namespace allocation and local-name bytes aligned without normalization. Decision made 2026-10-29.

The first measurements found redundant component allocation in fresh assignment and bulk dependency collection. After the codec (3 tests) and bulk invariant (4 tests) suites passed, add namespace-only and local-name-only projections to the same helper. All three entry points share one canonical boundary function; writers needing both components still use one captured `Split`. Preserve the initial raw measurements and repeat the same tests and benchmark matrix after this allocation reduction. Decision made 2026-10-29.

Use branch `GH-0000-canonical-iri-format-7` because the request has no issue number. Do not merge to main. Build and test locally, then publish reviewable source and a diff. Decision made 2026-10-29.

An independent dictionary comparison supports basing identity on the full lexical value. Apache Jena TDB2's [`NodeLib.setHash`](https://github.com/apache/jena/blob/main/jena-tdb2/src/main/java/org/apache/jena/tdb2/lib/NodeLib.java) hashes `n.getURI()` with a term-kind discriminator; [`NodeTableNative.accessIndex`](https://github.com/apache/jena/blob/main/jena-tdb2/src/main/java/org/apache/jena/tdb2/store/nodetable/NodeTableNative.java) treats an index miss as absence before optional allocation. These primary sources were inspected on 2026-10-29. RDF4J retains its namespace compression and collision verification; format 7 gives that existing dictionary one representation per lexical IRI, which makes an ordinary verified miss conclusive. No Jena code or hash scheme is imported.

## Outcomes & Retrospective

The baseline regression `CanonicalIriFormatGateTest.newStoreRequiresCanonicalFormatSeven` failed with expected 7 but actual 6 (1 test, 1 failure, 0 errors). The report and exact command are preserved in `initial-evidence.txt`. It now passes. Before the user narrowed support to 2 and 7, the related regression run passed **751 tests across 33 classes, with zero failures, errors or skips** in 10:36. These results are historical evidence; the revised policy must pass a new run. The canonical suite passes all 28 cases, and the six authentic-fixture/original-binary cases pass without skips. The initial implementation allowed both pinned binaries to reopen current writes and clear operations without changing persisted policy. Positive round-trip compatibility is now required only for the actual 6.1.0 format-2 binary. Older binaries must still reject completed/interrupted format 7 without changing persisted files. All 38 prepared-store cases pass, as do the 20 versioned bulk-workspace cases including the old format-6 binary's rejection of marker-only canonical publication.

The 125-module Java quick-profile reactor build passed on the serial retry in 4:29; the earlier parallel attempt encountered an unrelated WAR assembly temporary-file race. The final LMDB/dependency quick install passed in 1:02 after all production edits. Tests use those freshly installed classes. Formatting and `git diff --check` pass. The final 2/7-only regression run passed **864 tests across 37 classes, with zero failures, errors or skips**, in 15:01. Its complete XML/text reports were cross-checked and archived under `target/canonical-evidence/format-2-and-7-final/`. All three authentic release-specific cases ran with the real 6.1.0 binary, and the original format-6 binary rejection check also ran without a skip. The revised module/dependency quick install passed in 33.760 seconds and final Spotless checking passed in 7.870 seconds. The initial benchmark matrices are archived, including slower fresh/import cases. Revised measurements compare current formats 2 and 7 with the authentic pre-change format-6 binary; current code never reopens format 6 for benchmarking.

The final benchmark completed all **440 measured observations** in four separate JVM matrices. All **120 format-7 dictionary observations** recorded zero alternative calls, native probes and seeks. Against the actual pre-change binary, distinct scalar throughput medians ranged from 4.15–38.91 times the old rate and distinct batch medians from 1.84–41.25 times. Normal-import medians were slower (0.702–0.904 times the old rate), initial-run imports ranged from 0.685–1.191 times and bulk from 0.927–1.090 times. Fresh prepared/assigned paths allocated 24 extra bytes per repeated input or 48 per distinct input. All paired dictionary footprints matched. The report retains all observations and documents substantial variation, separate-JVM/JIT and instrumentation effects, Java-cache-only clearing, and calling-thread-only allocation. These results prove removal of alternate work without promising a general import speedup.

## Context and Orientation

The module is `core/sail/lmdb`. Its Java package `org.eclipse.rdf4j.sail.lmdb` contains `ValueStore`, which assigns numerical identities and writes dictionary records, and `LmdbStoreFormat`, which captures immutable persisted capabilities before opening native storage. `StoreProperties`, `LmdbStore`, `LegacyStoreDiscovery`, and `RetiredValueIdStore` resolve metadata and version fences. `PreparedBulkStore` validates already built generations; `LmdbInitialRunImport` recovers interrupted initial imports. The `bulk` subpackage stages values, allocates dependencies, and writes native records directly. All of these must agree on a generation's format.

An IRI record references a namespace record by ID and stores the remaining local-name bytes. A canonical split follows the first hash, otherwise the last slash, otherwise the last colon. Only the boundary changes: case, Unicode, escaping, and the original characters are preserved. Empty local names are valid. Alternative IRI lookup searches possible namespace prefixes when old stores might contain different splits.

## Plan of Work

First add tests using existing APIs that fail on format 6: newly created stores must report 7, arbitrary splits must produce a single ID, and ordinary misses must make no alternative probes. Preserve Surefire failure evidence in `initial-evidence.txt` before production edits.

Then change the persisted policy, reject unsupported or malformed versions before native mutation, and preserve the exact format through open, clear, prepared-store admission, interrupted-import recovery, and overflow. Introduce the shared helper and apply it to scalar namespace allocation, record codec, snapshot capture, accelerated fresh-value paths, datatype and triple dependencies, bulk records, and reservation estimates. Guard every alternative-lookup call site for canonical stores while retaining old paths for released format 2.

Finally run release-specific compatibility fixtures, using the real 6.1.0 commit `46a2d41ab16fead609884ee655e26e6baa1ce325`. Verify format-2 writes remain readable by 6.1.0 and old binaries reject format 7 without changing it. Verify current code rejects the authentic intermediate format-6 fixture without touching its files. Measure format-specific insertion and lookup workloads, report counters and timing without extrapolating beyond measured cases, and run focused neighboring regressions.

## Concrete Steps

Run from the repository root after sourcing `/workspace/scratch/ad14de109faf/toolchain/env.sh`. All Maven invocations use `-Dmaven.repo.local=.m2_repo`; prefer offline after downloading missing dependencies. Install required reactor modules with tests skipped using `-Pquick` before running tests. Tests never use `-am` or `-q`. A representative focused command is:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=ValueStoreCanonicalIriTest,LmdbCanonicalIriFormatPolicyTest,VersionedIriBulkEncodingTest test

Keep build logs and Surefire reports. The complete final test selection, commands, evidence and limits are recorded in [the validation report](../validation/canonical-iri-storage.md). Benchmark settings, raw results and executable reproduction are recorded in [the benchmark report](../benchmarks/canonical-iri-storage.md).

## Validation and Acceptance

Observe version 7 for genuinely new stores, one ID for every split of a lexical IRI, distinct IDs for distinct strings, exact reconstruction, and zero alternative searches in scalar, batch, snapshot, and accelerated paths. Exercise delimiter precedence, empty local names, Unicode, supplementary characters, long records, actual CRC collisions, datatype IRIs, nested triple terms, rollback, reopen, and ID reuse. Test released format-2 stores and assert their persisted version remains unchanged after writes and clear. Assert format-6 stores and historical bulk workspaces are rejected without modification. Prepared and overflow generations must match their owner. Future or malformed metadata must fail before native files change.

Performance validation compares formats independently with identical workloads. Record throughput, allocation, dictionary counts, namespace counts, and observed alternative native probes/seeks. Distinguish Java-cache clearing from an actual cold operating-system page cache.

## Idempotence and Recovery

All tests operate on temporary directories. Real historical fixtures are copied before new or old binaries open them. No user's store is opened and no migration is performed. Keep untracked logs and artifacts; do not delete them to clean the tree. If baseline builds need network dependencies, retry online once, then resume offline. Changes live on a dedicated branch.

## Artifacts and Notes

Initial failing evidence is retained in `initial-evidence.txt`. Authentic ZIP fixtures under `core/sail/lmdb/src/test/resources/lmdb-compat/` include producer commit and class hashes: `canonical-iri-format-2.zip` is used for positive compatibility, and `unsupported-format-6.zip` is used only for safe rejection. Final validation and performance results will be recorded with reproducible commands. The handoff will include complete changed source files and a git patch.

## Interfaces and Dependencies

Use existing RDF4J `URIUtil`, Java records, and existing LMDB hooks; add no production dependency. The shared helper is public only for internal bulk-package use and annotated `@InternalUseOnly`. `LmdbStoreFormat.usesCanonicalIriEncoding()` exposes the persisted invariant. `ValueStoreRecordCodec` gains an explicit format-aware or pre-split encoder so all record writers share the same local-name representation.

Revision note (2026-10-29): created after repository and metadata audit, before production changes.

Revision note (2026-10-29): recorded completed encoding and policy implementation, first successful LMDB build, and independent review findings about interruption fences. Format-specific verification is now in progress.

Revision note (2026-10-29): the user clarified that unreleased format 6 does not need support. Before changing production, `LmdbCanonicalIriFormatPolicyTest.unreleasedFormatSixIsRejectedByFormatResolvers` failed with 1 test and 1 failure because the expected rejection was absent; its Surefire report is archived at `baselines/initial-reports/reject-unreleased-format-six-red.txt` outside this repository. The active milestone removes format-6 admission and recovery, adapts legacy tests to format 2 or fresh format 7, and repeats the affected validation.

Revision note (2026-10-29): completed the 2/7-only implementation and 864-case regression run. Source and test changes are frozen; the active milestone measures the final binary in isolation before publication.

Revision note (2026-10-29): the final isolated benchmark and all regression gates are complete. The active milestone publishes the reviewed implementation and its evidence; no further code or measurement changes are planned.

## Published handoff

The implementation is published in [draft pull request #144](https://github.com/HASMAC-AS/rdf4j/pull/144), from `GH-0000-canonical-iri-format-7` into HASMAC-AS/rdf4j `main` at `db572a45a401d9e7284b321efba0adbd6d577e6c`. The implementation commit is `75753ac89716dbd60fac6f45c244743028ac0697`; its remote Git tree exactly matches the reviewed local staged tree. A documentation-only publication record follows that commit. No issue number was supplied, so the repository's `GH-0000` convention is used.

The handoff includes all 59 changed files and a binary-safe patch against the pinned main commit. Artifact checks verify each ZIP member against its staged Git blob and verify that applying the patch to a temporary index at the baseline reproduces the complete staged Git tree. The validation report, benchmark report, four raw result files, environment metadata, authentic fixtures and reproduction scripts are part of the branch and source package.

Revision note (2026-10-29): publication and packaging are complete. There are no unfinished implementation or validation steps. The measured import regressions and benchmark limitations remain explicit in the report and PR.
