# LMDB partial pair indexes

This living ExecPlan follows PLANS.md.

## Purpose / Big Picture

Allow `SP,OP,PSOC,POSC` as statement indexes. SP stores distinct subject/predicate pairs and OP stores distinct object/predicate pairs. Subject-only and object-only reads enumerate pairs instead of scanning all statements, then fetch complete statements through PSOC and POSC respectively. A pair remains until its last supporting statement of the same explicit/inferred kind is deleted.

## Progress

- [x] (2026-09-30) Trace configuration, storage, iterators and estimators.
- [x] (2026-09-30) Create and observe failing regression test: four tests, three unsupported-SP errors.
- [x] (2026-09-30) Implement auxiliary databases and resolving iterator.
- [x] (2026-09-30) Verify low-level deletion, promotion, rollback and reindexing: ten feature tests pass.
- [x] (2026-09-30) Format and run focused tests: 32 unit tests pass, including 13 partial-index feature tests.
- [x] (2026-09-30) Run module verification and reproduce all reported failures and native crash against HEAD LMDB sources.
- [ ] in_progress: Deliver final handoff; implementation and verification are complete.

## Surprises & Discoveries

Full indexes use chunked four-field records; the triple-term value store shares their parser. Partial indexes must not enter that parser or statement cardinality estimates. Deletion iterators defer physical chunk removal until close, so support checks must run after close.

The offline baseline install lacked org.tukaani:xz:1.12. The prescribed online retry completed successfully. Cache replay holds the transaction-manager write lock, so support checks use a direct native full-index cursor, not a read-locking LmdbRecordIterator. Auxiliary backfill can queue idempotent full records with zero context delta in TxnRecordCache to reuse safe map-growth replay.

Expanded tests found that buffered deletion in LmdbRecordIterator used canonical SPOC values to seek index-ordered chunks. Its removal seek now transforms the quad through the selected index. Resolved partial reads also enforce the original four-field pattern explicitly, including default context zero. Low-level test stores must explicitly load/save StoreProperties; the repository owns this lifecycle rather than the TripleStore convenience constructor.

Independent caller tracing found ValueStore writes the shared statement-index metadata before TripleStore reads the old configuration, which can skip both full and partial backfill. A repository-level migration regression was added before changing metadata ownership. Optimizer access-path enumeration also needs to expose the selected auxiliary path and account for pair enumeration plus full tuple expansion.

The ten low-level feature tests now pass, including all binding masks in default/named contexts and 30,000-statement reindexing with map growth. Repository reopen exposed a second metadata defect: ValueStore created mandatory term indexes but saved a null raw configuration. It now saves the effective full term-index set. StoreProperties dirty flags accumulate changes instead of allowing unchanged setters to erase pending metadata updates; focused metadata and custom-term migration regressions cover this.

LegacyCardinalityEstimatorCompatibilityTest reports historical row estimates inconsistent with the existing chunked storage and hard-disabled page estimator. An isolated HEAD LMDB baseline against installed upstream dependencies is being verified before classifying these as preexisting. Its initial root -pl invocation failed because the scratch archive intentionally contains only the LMDB module and parent hierarchy; retry with -f core/sail/lmdb/pom.xml to avoid scanning absent sibling modules.

Final module verification reported 628 tests, 20 failures, two isolation errors and two skipped tests, then crashed in CardinalityTest at LMDB.nmdb_dcmp. Isolated HEAD commit 24e4b59b0d8308f9b19cde5ccb5258d75d61021a, using the same installed upstream dependencies, reproduced the exact failing-method sets across all six failing classes, the same expected/actual estimator values and the same native crash. Existing pre-task crash artifacts also identify nmdb_dcmp/CardinalityTest. These unrelated defects remain unchanged.

## Decision Log

Use separate auxiliary pair databases with ordinary varint pair keys and empty values, leaving full-index encoding unchanged. Require PSOC for SP and POSC for OP; validate the entire specification before opening index databases. Keep full indexes in TripleStore.indexes and auxiliary indexes separately so existing identity, garbage collection and estimation paths remain valid. Mixed-layout aligned writes initially use individual insertion to preserve duplicate and promotion semantics.

## Context and Orientation

All implementation files are under core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb. TripleStore owns transactions, indexes and writes; TripleIndex parses full permutations and orders them. LmdbRecordIterator expands full-index chunks into four-element statement arrays. RecordIterator is the read contract. A new PartialIndex owns pair databases and resolves pairs using a full LmdbRecordIterator in the caller's transaction. Transaction locking protects native operations during map growth.

## Plan of Work

First add TripleStorePartialIndexesTest under the module's matching test package and observe rejection of the new specification. Add a statement-only parser to TripleIndex without changing the full-only parser used by ValueStore. Split full and partial specs in TripleStore initialization/reindexing. Build newly added auxiliary indexes from a full index in the setup write transaction and remove obsolete auxiliary databases. Insert projections after full writes; on deletion collect distinct affected pairs and check full counterparts after iterator close. Cache replay uses the same projection maintenance. A partial iterator seeks one pair at a time under the transaction lock and expands it through its counterpart, applying original omitted-field constraints. Full indexes win prefix-score ties.

Add LmdbStorePartialIndexesTest to exercise actual repository creation, migration, unspecified reopen configuration, SPARQL reads and removal. Remove ValueStore's writes to statement-index metadata while retaining its triple-term metadata handling. Publish the applicable partial access path from TripleStore.indexAccessPaths; cost it as pair enumeration plus complete-statement expansion, without treating pair counts as statement cardinalities. Keep direct-lookup shortcuts exclusive to full indexes.

## Concrete Steps

From repository root with JDK 25+, run the required baseline `mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install`. Then run `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=TripleStorePartialIndexesTest verify`, recording logs and Surefire reports. Repeat installation before direct test runs. Finally run the LMDB module verify without test selection. Never use -am or -q with tests.

Final focused verification used `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=TripleStorePartialIndexesTest,LmdbStorePartialIndexesTest,StorePropertiesTest,TripleStoreTest,LmdbStoreReindexTest,RemoveAddTest,DefaultIndexTest verify` and passed. The root quick installation and module Spotless checks passed. Baseline verification extracted HEAD's LMDB module, parent POM hierarchy and eclipse-settings into a temporary directory, linked the existing dependency cache, and used `mvn -o -Dmaven.repo.local=.m2_repo -f core/sail/lmdb/pom.xml -Dtest=<same-failing-classes>,CardinalityTest,LmdbOptimisticIsolationTest verify`. A module-local eclipse-settings link was needed because direct -f execution changes Maven's resource root. No baseline artifacts were installed over working artifacts.

## Validation and Acceptance

The new tests must fail on unsupported SP/OP before implementation, then pass with complete tuple expansion, multiple supporting statements, last-support deletion, explicit/inferred separation, promotion, rollback, persisted reopen and adding/removing partial indexes. Inspect pair database statistics as well as returned statement sets to prove stale pairs are removed. Existing full-only tests must continue passing. No performance claim is made without a benchmark.

## Idempotence and Recovery

Tests use temporary stores. Builds and tests may be repeated. Preserve unrelated working-tree and untracked files; do not commit or push. Existing stores require no full-index format migration. Invalid dependency configurations fail before indexing. On failed validation fix the smallest observed root cause and rerun the same selection.

## Artifacts and Notes

Build output is retained in maven-build.log and feature test output in logs/partial-index-tests.log. Initial failing evidence is preserved in initial-evidence-partial-indexes.txt without overwriting existing evidence:

	Tests run: 4, Failures: 0, Errors: 3, Skipped: 0
	SailException: invalid value 'sp' in index specification: SP,OP,PSOC,POSC

Final evidence is retained in logs/partial-index-focused-evidence.txt, logs/partial-index-focused-final.log, logs/partial-index-module-tests.log, logs/partial-index-baseline-all-failures.log and logs/partial-index-validation-summary.txt. The validation summary includes the baseline commit and asserts equality of the exact failing-method sets. The source-header checker reported only the preexisting, untouched util/ChunkCodecTest.java missing header; new and edited feature files have their required headers. Unrelated working-tree files and all crash artifacts were preserved.

## Interfaces and Dependencies

No new dependencies. PartialIndex provides explicit/inferred database handles, projection insertion, support reconciliation and a RecordIterator resolving pairs through its required TripleIndex. TripleIndex.parseStatementIndexSpecList accepts auxiliary specs, while parseIndexSpecList remains full-only.

## Outcomes & Retrospective

Implementation is complete. Configuring PSOC,POSC,SP,OP now creates distinct auxiliary pair databases, resolves complete quads, retains pairs until their final same-kind supporter disappears, exposes resolving access paths to costing, and backfills newly enabled indexes safely through map growth. Repository creation, migration, unspecified reopen, SPARQL reads and metadata changes are covered by passing focused tests. Existing full-index chunks and full-only triple-term parsing are unchanged.

The complete module is not green because its baseline has the same 20 failures, two errors and native CardinalityTest crash. No new failure was found in the baseline comparison. Mixed-layout stores deliberately use individual writes instead of the aligned bulk-write optimization; query acceleration has not been benchmarked. The main lessons are to preserve metadata ownership across ValueStore/TripleStore, flush deferred full-index deletion before checking pair support, and never retain write cursors across map growth.

Revision 2026-09-30: record baseline dependency recovery, initial failure evidence and implemented storage/locking decisions; proceed to validation.

Revision 2026-09-30: expanded validation revealed existing non-SPOC deletion and shared metadata ownership defects; add repository-level regression and optimizer integration before final verification.

Revision 2026-09-30: record ten passing low-level tests, effective term metadata persistence and accumulated dirty flags. Final verification includes an isolated baseline comparison; no estimator algorithms are changed.

Revision 2026-09-30: finish focused verification, reproduce every module failure and native crash against isolated HEAD LMDB sources, preserve evidence and prepare final handoff. No test assertions or compatibility estimator algorithms were weakened.




