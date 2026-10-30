# Versioned canonical IRI storage: validation evidence

## Final scope and validation status

The supported persisted formats are **2 and 7**. Format 7 derives namespace and local name from the full lexical IRI
with `URIUtil.getLocalNameIndex`; it does not normalize or otherwise change that string. Format 2 retains the actual
6.1.0-compatible encoding and the current compatibility lookup behavior. The product version remains `6.2.0-SNAPSHOT`.

The released 6.1.0 `ValueStore` trusts the caller's split and has no `AlternativeIriLookup`. This change preserves its
disk representation within the current transaction, snapshot, reclamation and legacy-lookup machinery; it does not
replace that machinery with the old release implementation.

The user clarified that **format 6 was unreleased and does not need support**. Current code rejects format 6,
including completed prepared stores, standalone dictionaries, historical initial-import markers, and historical
bulk workspaces, before writable native storage or destructive recovery. There is no migration, dictionary rewrite,
or namespace-reference-count rewrite.

| Persisted format | Current behavior |
| --- | --- |
| 2 | Read and write the released legacy format; opening, clearing, and overflow preserve its policy. |
| 7 | Create new stores with canonical IRI encoding; direct/hash lookup misses perform no alternative-prefix search. |
| 6 or another unsupported version | Reject before writable native opening; retain persistent artifacts unchanged. |
| Missing or malformed metadata | Apply strict legacy discovery or reject; a nonempty unknown generation cannot silently become format 7. |

Validation uses Linux x86-64, Eclipse Temurin **25.0.5+8**, and Maven **3.9.11**. The implementation starts from
HASMAC-AS/rdf4j main commit `db572a45a401d9e7284b321efba0adbd6d577e6c`. Tests use temporary stores and copies of
historical fixtures. The [execution plan](../execplans/versioned-canonical-iri-storage.md) records decisions and
workflow.

**Final validation of the 2+7-only policy passed: 864 tests across 37 classes, with zero failures, errors, or skips,
in 15:01.** Its fresh LMDB dependency/module quick install passed in **33.760 seconds**; formatting completed in
**9.128 seconds**, and the full copyright check passed. The final Spotless check passed in **7.870 seconds** and
reported **1,625 Java files clean**, including the corrected snapshot fixture.

The earlier **751 tests / 33 classes** result is historical: it tested the superseded implementation that still
admitted format 6. The earlier 125-module root quick install and LMDB dependency/module quick install also predate
the narrowing; their results are retained below with that limitation. There are no outstanding failures in the
final selected regression run.

## Test-first evidence

### Canonical-format baseline gate

Before any initial production edit, the new-format gate failed against the unmodified implementation:

```text
Test set: org.eclipse.rdf4j.sail.lmdb.CanonicalIriFormatGateTest
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
CanonicalIriFormatGateTest.newStoreRequiresCanonicalFormatSeven
org.opentest4j.AssertionFailedError: expected: <7> but was: <6>
```

The initial command and report locations are recorded in repository-root `initial-evidence.txt`. This gate passed in
the final revised-policy run: **1 test, 0 failures, 0 errors, 0 skips**.

### Rejection of the unreleased format

Before narrowing production support, the new format-6 rejection test was compiled against the existing production
classes and run in isolation. The captured Surefire result was:

```text
Test set: org.eclipse.rdf4j.sail.lmdb.LmdbCanonicalIriFormatPolicyTest
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
LmdbCanonicalIriFormatPolicyTest.unreleasedFormatSixIsRejectedByFormatResolvers
java.lang.AssertionError:
Expecting code to raise a throwable.
```

The focused invocation used
`-Dtest=LmdbCanonicalIriFormatPolicyTest#unreleasedFormatSixIsRejectedByFormatResolvers` with `surefire:test`, after
compiling only the test with `javac -proc:none`. The complete log is `canonical-iri-reject-six-red.log`; the exact
report is archived in the validation workspace at `baselines/initial-reports/reject-unreleased-format-six-red.txt`.
Production admission was narrowed only after this failure was observed. The new assertions cover both descriptor
factories and both supported-version predicates. It passes in the final 58-case policy suite.

### Malformed sidecar gates

Two additional regressions were observed before their production guards were introduced. Their original parameter
matrices included then-supported format 6, so those initial counts are historical; current positive setups use only
2 and 7.

| Focused regression | Captured RED | Observed failure | Current coverage |
| --- | ---: | --- | --- |
| `nonRegularMetadataCannotBeTreatedAsAbsent` | 9 tests, 9 failures | `Expecting code to raise a throwable.` Metadata directories, dangling links, and links to directories were treated as absent. | 6 cases: formats 2/7 × the three invalid path shapes. |
| `danglingOwningSidecarsCannotBecomeStandaloneDictionaries` | 4 tests, 4 failures | `Expecting code to raise a throwable.` A dangling owner sidecar could be ignored when no sibling triple store remained. | 2 cases: format 7 × either dangling owner sidecar. |

The focused commands used `-Dtest=LmdbCanonicalIriFormatPolicyTest#<method>` and `surefire:test`, after compiling the
new test against the then-current production classes. Reports are retained at
`baselines/initial-reports/nonregular-metadata-policy-red.txt` and
`baselines/initial-reports/dangling-owner-metadata-policy-red.txt`. All original cases passed in the historical policy
run; all revised cases pass in the final 58-case policy suite. Valid symlinks to regular metadata files remain readable.

## Revised persisted-policy coverage

Source: [LmdbCanonicalIriFormatPolicyTest](../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCanonicalIriFormatPolicyTest.java).
All **58 invocations passed**, with zero failures, errors, or skips. The table accounts for every parameterized
invocation rather than counting only test methods.

| Test method | Inputs / contract | Cases |
| --- | --- | ---: |
| `newGenerationUsesFormatSeven` | New-store and transient descriptor defaults are 7. | 1 |
| `unreleasedFormatSixIsRejectedByFormatResolvers` | Format 6 is rejected by both factories and both support predicates. | 1 |
| `authenticUnreleasedFormatSixRejectsOrdinaryPreparedAndRecoveryAdmissionWithoutMutation` | Authentic completed, interrupted, and RESET6 stores retain every persistent byte; prepared admission returns no certificate. | 3 |
| `authenticUnreleasedFormatSixRejectsOwningAndStandaloneDictionaryAdmissionWithoutMutation` | Authentic format-6 dictionary, with owner metadata or with only its native fence, is rejected unchanged. | 2 |
| `authenticUnreleasedNativeGenerationCannotBecomeLegacyWhenItsMetadataIsMissing` | Removing owner sidecars cannot turn a format-6 native generation into legacy format 2. | 1 |
| `lowLevelFormatsRejectUnknownOrMalformedVersions` | `1`, `3`, `4`, `5`, `8`, `-1`, `garbage`, empty, `7.0`, `+7`, `07`, leading-space `7`. | 12 |
| `initializingNewOutputCannotUpgradeExistingProperties` | Neither released format-2 metadata nor unsupported format-6 metadata can be rewritten as new output. | 2 |
| `loadedMetadataWithoutVersionCannotBecomeNewOutput` | Loaded metadata with no version is invalid. | 1 |
| `nonRegularMetadataCannotBeTreatedAsAbsent` | Formats 2/7 × directory, dangling symlink, symlink to directory; persistent bytes remain unchanged. | 6 |
| `regularFileMetadataSymlinksRemainReadable` | Formats 2/7 retain valid metadata symlink behavior. | 2 |
| `invalidInterruptedImportCannotDeletePersistedArtifacts` | Versions `6`, `8`, `garbage`, and empty reject before cleanup. | 4 |
| `interruptedImportWithoutMetadataCannotBecomeCanonical` | Missing policy cannot authorize destructive recovery or format-7 creation. | 1 |
| `writeClearReopenAndOverflowKeepTheOwningFormat` | Formats 2/7 retain policy through writes, clear, overflow, close, and reopen. | 2 |
| `completedCanonicalGenerationRetainsPreparedAdmission` | A completed format-7 generation keeps its sealed prepared startup. | 1 |
| `standaloneDictionariesRecoverTheirPersistedFormat` | Standalone formats 2/7 retain IDs and lexical values after reopening. | 2 |
| `standaloneConstructorReadsOwningSailMetadata` | Format 7 uses the owner's complete capabilities. | 1 |
| `standaloneConstructorRejectsUnknownOwningMetadataBeforeMutation` | Unknown owner format fails before writable native setup. | 1 |
| `danglingOwningSidecarsCannotBecomeStandaloneDictionaries` | Format 7 with dangling parent `store.properties` or `lmdbrdf.ver`, without a `triples` sibling. | 2 |
| `standaloneConstructorCannotLoseOwningCapabilitiesWhenBothSidecarsAreMissing` | A remaining triple-store owner prevents a metadata-free modern fallback. | 1 |
| `standaloneConstructorKeepsMetadataFreeLegacyOwnerInFormatTwo` | An identifiable legacy owner remains format 2. | 1 |
| `interruptedResetRetainsFormatAcrossAnotherInterruption` | Format 7 retries a durable reset after another crash leaves an unfinished replacement native file. | 1 |
| `formatSevenCannotUseTheOldBinarysDestructiveRecoveryMarker` | Format 7 rejects the historical initial-import marker. | 1 |
| `malformedOrMismatchedResetStateRejectsBeforeMutation` | RESET6, garbage, and an unknown reset state are rejected. | 3 |
| `interruptedImportRejectsMismatchedNativeFormatBeforeDeleting` | Format-7 metadata paired with native fences 2, 6, or 8 fails without cleanup. | 3 |
| `preparedMetadataCannotRelabelAnExistingGeneration` | A completed format-7 generation cannot be relabeled 2, 6, or 8. | 3 |
| **Completed policy suite** | **0 failures, 0 errors, 0 skips** | **58** |

```text
Test set: org.eclipse.rdf4j.sail.lmdb.LmdbCanonicalIriFormatPolicyTest
Tests run: 58, Failures: 0, Errors: 0, Skipped: 0
```

The prechange format-6 opener deletes its recognized interrupted initial-import generation before validating the
store version. Format 7 therefore uses `.lmdb-initial-import-v7.incomplete`; a forced, version-tagged RESET state is
persisted atomically before native deletion. Current code never resumes the historical marker. Prepared certificates
retain their serialization and bind the exact format through their fingerprints, but only format 7 receives modern
prepared admission. Bulk workspace and publication schema 3 identify canonical output; historical schema 2 is rejected
before cleanup, publication, or fallback to another revision.

## Authentic fixtures and original binaries

Both fixtures were written by independent binaries from the pinned commits below. Neither was manufactured by
relabeling a format-7 store. Each original writer reopened and verified its own generated fixture in a separate
process before packaging.

| Archive | Purpose under the final policy | Original writer / persisted format | Pinned commit |
| --- | --- | --- | --- |
| `canonical-iri-format-2.zip` | Positive read/write/reopen compatibility | Eclipse RDF4J 6.1.0 / 2 | `46a2d41ab16fead609884ee655e26e6baa1ce325` |
| `unsupported-format-6.zip` | Rejection without modifying persistent files | HASMAC-AS/rdf4j prechange main / 6 | `db572a45a401d9e7284b321efba0adbd6d577e6c` |

The manifests record producer commits, loaded `ValueStore.class` and producer-class SHA-256 hashes, original
properties hashes, persisted versions, inlining policy, and dictionary IDs. The
[fixture provenance](../../core/sail/lmdb/src/test/resources/lmdb-compat/canonical-iri-fixtures.md) includes archive
hashes and regeneration instructions. The renamed negative format-6 archive retains its exact original bytes and
SHA-256 `e69814ab34cb2cbb1915850c6b7426c44c52c150fd446daeb1a7db4a8f1c50e9`.

Both fixtures disable literal inlining and contain ten referenced values, arbitrary IRI boundaries, long/hash-indexed
records, Unicode including a supplementary character, datatype IRIs, nested triple terms, and a namespace declaration.
The format-6 fixture has a valid certificate from its original writer, so rejection coverage includes genuinely
completed prepared input rather than only malformed metadata.

### Explicit 6.1.0 configuration workaround

The unmodified 6.1.0 writer has an index-property dirty-flag issue: default settings can omit `store.properties` or
the term-index descriptor, and term-index initialization can assign the statement-index descriptor from an unset
runtime setting. The original binary reproduced the reopen failure on its own output. The producer therefore
requests an empty statement-index string only at first creation, which the release resolves to `spoc,posc`, and
explicitly requests `spoc,cspo` term indexes. Every reopen explicitly supplies those same effective settings:
statement indexes `spoc,posc`, term indexes `spoc,cspo`.

No fixture properties are edited to manufacture compatibility. Records, metadata, and original-binary read/write
operations come from the unchanged release. The comparison excludes that release's default-configuration reopen
bugs; it tests disk compatibility under matching explicit historical settings. The develop-baseline test does not
substitute for this release-specific evidence.

### Revised release-compatibility checks

[LmdbCanonicalIriCompatibilityTest](../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCanonicalIriCompatibilityTest.java)
now defines three cases, all based on released format 2:

| Contract | Cases |
| --- | ---: |
| Read the authentic fixture, verify IDs/statements, look up and reinsert alternative splits, write and reopen; preserve original properties bytes. | 1 |
| The original 6.1.0 binary reads current writes and writes back; current code clears; the original binary reopens and writes again without a format change. | 1 |
| The original 6.1.0 binary rejects both completed and interrupted format-7 stores without changing persistent files. | 1 |

The pinned format-2 classpath was supplied and all three cases passed without skips:

```text
Test set: org.eclipse.rdf4j.sail.lmdb.LmdbCanonicalIriCompatibilityTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

Persistent-file comparisons use SHA-256 snapshots and exclude transient lock files. The separate versioned bulk
workspace suite passed all **26 cases without failures, errors, or skips**. The pinned prechange format-6 classpath
was also supplied, so its real bulk reader executed the check that rejects canonical publication before a manifest
exists. That is an old-reader rejection check, not current format-6 admission support.

Rebuild the pinned release and run the positive format-2 and negative format-6 checks with:

```bash
scripts/verify-canonical-iri-compatibility.sh
```

To include the independent prechange binary's forward-rejection check:

```bash
scripts/verify-canonical-iri-compatibility.sh --with-prechange-baseline
```

## Historical verification before support was narrowed

The earlier `canonical-iri-final-tests.log` ends with:

```text
Tests run: 751, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 10:36 min
```

That run selected **33 classes** and completed with zero skips, including 28 canonical-identity cases, the original
62-case policy matrix, six authentic-fixture/original-binary cases, 38 prepared-store cases, and 20 versioned bulk
workspace cases. Both original-binary classpaths were supplied. Those six historical compatibility cases included
positive format-6 compatibility, which was subsequently removed. These results document the implementation history;
they must not be presented as the final 2+7-only result.

The historical root quick install completed all **125 selected build modules** on its serial retry in **4:29**. The
historical final LMDB dependency/module quick install passed in **1:02**, and its last formatting pass changed zero
files. These installs skipped tests. Historical reports are archived under `target/canonical-evidence/final/`; the
initially named “final” directory belongs to this earlier run.

## Final revised verification

The final `canonical-iri-2-and-7-final-tests.log` reports:

```text
Tests run: 864, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 15:01 min
```

The XML and text reports were cross-checked against the completed log and archived under
`target/canonical-evidence/format-2-and-7-final/`. Its `summary.json` records each class's invocation count, result,
and duration. All classes below completed with **zero failures, errors, and skips**; their counts sum to 864.

| Test class | Cases |
| --- | ---: |
| `CanonicalIriFormatGateTest` | 1 |
| `FreshValueReservationTest` | 8 |
| `LmdbBulkArtifactIntegrityTest` | 9 |
| `LmdbBulkLoadGenerationTest` | 33 |
| `LmdbBulkLoadPublicationRecoveryTest` | 19 |
| `LmdbBulkLoaderContractTest` | 45 |
| `LmdbCanonicalIriCompatibilityTest` | 3 |
| `LmdbCanonicalIriFormatPolicyTest` | 58 |
| `LmdbCoreDatatypeReferenceIdTest` | 10 |
| `LmdbDatatypeHistogramTest` | 7 |
| `LmdbFreshValueSessionLifetimeTest` | 8 |
| `LmdbIndexMetadataCompatibilityTest` | 20 |
| `LmdbIndexRebuildGrowthTest` | 8 |
| `LmdbInitialNoneFreshImportTest` | 4 |
| `LmdbInitialRunImportMarkerSyncTest` | 3 |
| `LmdbInitialRunImportTest` | 24 |
| `LmdbLegacyPreparedBulkCompatibilityTest` | 4 |
| `LmdbNativeFormatFenceTest` | 17 |
| `LmdbOrderedNumericIdsTest` | 6 |
| `LmdbPreparedBulkStoreTest` | 38 |
| `LmdbQueryReadyBulkLoadTest` | 13 |
| `LmdbRetiredValueCascadeTest` | 1 |
| `LmdbSailStoreOverlayStartupTest` | 5 |
| `LmdbSnapshotLifetimeTest` | 40 |
| `LmdbStoreFormatCompatibilityTest` | 36 |
| `LmdbZeroFractionDateTimeTest` | 10 |
| `SnapshotPositiveLookupCacheTest` | 147 |
| `SnapshotPositiveLookupLifecycleTest` | 13 |
| `StorePropertiesTest` | 6 |
| `UnfinishedBulkLoadTest` | 6 |
| `ValueStoreBulkRecordsTest` | 7 |
| `ValueStoreCanonicalIriTest` | 28 |
| `ValueStoreIdCacheTest` | 47 |
| `ValueStoreIriRecordCodecTest` | 3 |
| `ValueStoreTest` | 147 |
| `VersionedBulkWorkspaceFormatTest` | 26 |
| `VersionedIriBulkEncodingTest` | 4 |
| **Total: 37 classes** | **864** |

The canonical identity tests use observation hooks to assert that format-7 misses never enter alternative-IRI search.
They cover lexical edge cases, delimiter precedence, every split of the same string, long/hash records, collisions,
datatype IRIs, nested triple terms, rollback, repeated writes, reopen, and real ID reuse. Additional format-parameterized
coverage uses released format 2 or freshly created format 7, including snapshot retirement, index metadata, actual
native map growth, and completed bulk publication.

### Snapshot fixture correction during final validation

The initial ten-class focused run recorded **330 tests, 1 failure, 0 errors, 0 skips**. Its policy, release-compatibility,
canonical identity, and bulk-workspace cases passed. The failure was a timeout in the paired snapshot-remap lifecycle
test; an isolated retry reproduced it. Replacing the historical format-6 fixture with a fresh format-7 store had lost
the original `ospc` index: the old fixture used `spoc,posc,ospc`, while the replacement initially used the two-index
default. Without the object-leading index, bounded liveness checks repeatedly deferred unresolved object IDs.

The replacement fixture now explicitly requests the original three indexes. The existing ten-second timeout and
assertions are unchanged, and the correction required no production change. In the final run,
`SnapshotPositiveLookupLifecycleTest` passed all **13 cases in 1.545 seconds**. The broader
`LmdbSnapshotLifetimeTest` passed **40 cases**; index-metadata and actual index-rebuild-growth suites passed **20** and
**8** cases respectively. The focused failure log remains available at `canonical-iri-2-and-7-focused-tests.log`.

### Reproduction command

The final test invocation used the following selection and properties, as recorded in the archived Surefire XML.
The installed main classes came from the fresh successful dependency/module quick install; test compilation ran
normally. For another checkout, replace the toolchain and producer-classpath paths with corresponding local outputs.

```bash
source /workspace/scratch/ad14de109faf/toolchain/env.sh

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -Dmaven.compiler.showWarnings=false -Dmaven.main.skip=true \
  -pl core/sail/lmdb \
  -Dtest=CanonicalIriFormatGateTest,ValueStoreCanonicalIriTest,ValueStoreIriRecordCodecTest,LmdbCanonicalIriFormatPolicyTest,LmdbCanonicalIriCompatibilityTest,VersionedIriBulkEncodingTest,VersionedBulkWorkspaceFormatTest,ValueStoreIdCacheTest,SnapshotPositiveLookupCacheTest,SnapshotPositiveLookupLifecycleTest,LmdbStoreFormatCompatibilityTest,LmdbNativeFormatFenceTest,LmdbInitialRunImportTest,LmdbInitialRunImportMarkerSyncTest,LmdbSailStoreOverlayStartupTest,LmdbDatatypeHistogramTest,StorePropertiesTest,ValueStoreTest,FreshValueReservationTest,LmdbFreshValueSessionLifetimeTest,LmdbRetiredValueCascadeTest,LmdbPreparedBulkStoreTest,LmdbLegacyPreparedBulkCompatibilityTest,LmdbQueryReadyBulkLoadTest,LmdbInitialNoneFreshImportTest,LmdbCoreDatatypeReferenceIdTest,LmdbOrderedNumericIdsTest,LmdbZeroFractionDateTimeTest,LmdbBulkLoadPublicationRecoveryTest,LmdbBulkLoadGenerationTest,LmdbBulkArtifactIntegrityTest,UnfinishedBulkLoadTest,ValueStoreBulkRecordsTest,LmdbSnapshotLifetimeTest,LmdbIndexMetadataCompatibilityTest,LmdbIndexRebuildGrowthTest,LmdbBulkLoaderContractTest \
  -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=2G \
  -DskipITs \
  -Dformatter.skip=true -Dimpsort.skip=true -Djapicmp.skip=true -Denforcer.skip=true \
  "-Drdf4j.lmdb.canonical.compat.classpath.format2=$(cat /workspace/scratch/ad14de109faf/baselines/format2/classpath.txt)" \
  "-Drdf4j.lmdb.canonical.compat.classpath.format6=$(cat /workspace/scratch/ad14de109faf/baselines/format6/classpath.txt)" \
  test > canonical-iri-2-and-7-final-tests.log 2>&1
```

The sourced toolchain sets Maven itself to `MAVEN_OPTS="-Xmx3g -XX:ActiveProcessorCount=4"`; the final command
does not override Surefire's configured fork arguments. The preceding main-source build used
`-pl core/sail/lmdb -am -Pquick -Dmaven.test.skip=true -DskipFrontend=true install`. The test command deliberately
omits `-am`. A final Spotless check after the snapshot fixture correction passed in
**7.870 seconds**, with **1,625 Java files clean**. The copyright check also passed.

## Limits of this evidence

- The final verification selects **37 LMDB-related classes**, not the full LMDB suite, a full repository test run,
  or an integration-test run; `-DskipITs` is explicit. Quick installs establish buildability and skip tests.
- Positive historical compatibility is required only for the actual 6.1.0 format-2 producer and the recorded
  configuration cases. Format 6 is an unsupported negative fixture and an independently executed performance baseline.
- Recovery tests exercise defined interruption points and child-process failures on Linux. They do not certify every
  power-loss scenario or filesystem/platform combination.
- The internal standalone-dictionary seam has no full-store metadata. If every trace of an owning store is removed,
  it cannot reconstruct that missing owner's independent literal capabilities. Recognizable owner sidecars and
  sibling triple stores are validated and cannot silently fall back to that seam.
- Performance results are reported separately. Revised measurements compare formats 2/7 under current code with the
  original format-6 binary in a separate JVM; current code does not reopen format 6 to benchmark it. Allocation scope,
  Java-cache versus OS-cache conditions, binary hashes, and slower cases must remain explicit. Correctness results
  alone are not a throughput claim.
