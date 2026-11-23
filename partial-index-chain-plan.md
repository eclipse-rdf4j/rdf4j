# Resolve partial indexes through binding-aware chains

This living ExecPlan follows root PLANS.md.

## Purpose / Big Picture

Allow `cs,sp,psoc` to resolve context-subject pairs through subject-predicate pairs and finally complete statements. Existing two-field storage stays unchanged. All configured partial indexes can resolve through other configured partial indexes and ultimately any configured full index.

## Progress

- [x] (2026-10-01) Trace configuration, resolution, maintenance and costing.
- [x] (2026-10-01) Add and observe smallest failing chain test.
- [x] (2026-10-01) Implement binding-aware plans and generic support probes.
- [x] (2026-10-01) Verify initial low-level suite: 22 tests passed.
- [x] (2026-10-01) Fix reproduced context-first support probe regression.
- [x] (2026-10-01) Verify expanded regressions: 60 unit tests passed.
- [x] (2026-10-01) Format the repository; build and focused verification pass.
- [x] (2026-10-01) Launch full-module verification and compare observed failures with historical evidence.
- [ ] in_progress: Full-module runner remains active; final totals not yet collected.

## Context and Orientation

In core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb, TripleStore owns configured indexes and selects read paths. PartialIndex stores distinct pairs (`sp`, `op`, `cs`); TripleIndex stores complete subject/predicate/object/context records. A bound field has a nonnegative identifier; -1 is unbound, and context 0 denotes the default graph. PartialIndex currently opens a full counterpart directly and assumes its first two fields reverse the pair when checking deletion support. TripleIndex.parseStatementIndexSpecList enforces those hard-coded companions. Tests are in core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/TripleStorePartialIndexesTest.java.

## Plan of Work and Milestones

First add a test configuring cs,sp,psoc, exercising graph isolation and deleting the last context supporter while another graph retains sp support. Observe configuration rejection before production edits. Then add an immutable resolution plan: search possible partial stages whose leading field is bound and whose second field is not. Each adds a binding, so recursion terminates within four fields. Prefer plans with the longest bound prefix on the terminal full index, breaking ties by fewer stages. Preserve full-index preference when selecting the initial access path. A selected initial partial may have both fields bound; later stages must add a binding. Compose existing lazy pair iterators with the planned child instead of recursively reselecting the initial index. Finally generalize deletion support to a native full-index existence probe, retaining original pair constraints and checking all decoded fields inside the existing write transaction, without reader locks.

Expose resolution depth to optimizer costing using the same plan selected for iteration. Extend tests to all binding masks, statement kinds, chunks, promotion, rollback, reopen/backfill and alternate full terminal indexes. Update configuration documentation and outdated companion-rejection tests.

## Decision Log

On 2026-10-01 choose arbitrary configured full-index fallback instead of requiring specific companions. This is correct even if it needs a scan, avoids implicit full indexes, and separates validity from performance. Keep supported partial names unchanged; generalization concerns resolution, not new projection formats. Plan chains by accumulated bindings, not static index-name dependencies, since already-bound intermediate fields should skip work.

## Surprises & Discoveries

Support probes cannot use read iterators: cache replay holds the write lock. Intermediate pairs cannot prove graph-specific support. Existing unrelated benchmark/testsuite edits must remain untouched.

The initial root clean build succeeded but took 18:21 minutes, rather than the anticipated 30 seconds. A redundant mvnf prerequisite install was stopped; the focused regression was run manually after the completed root build. It failed as expected with `Partial index 'cs' requires full index 'scpo'` (one test, one error). Full evidence is saved in initial-evidence-partial-index-chain.txt and logs/partial-index-chain-before.log. The copyright check reports an existing missing header in untouched util/ChunkCodecTest.java, not in the new planner.

The full TripleStorePartialIndexesTest selection passed: 22 tests, zero failures/errors (logs/mvnf/20261001-120551-verify.log). Review identified that the cached ChunkInput decoder does not propagate shared context prefixes to non-anchor tuples. The new contextFirstFullIndexPreservesNonAnchorSupport test reproduces premature removal of a supported cs pair: expected 2, actual 1. Evidence is appended to initial-evidence-partial-index-chain.txt. This probe only needs forward decoding, so use the existing streaming constructor rather than changing shared decoder behavior outside this feature.

Expanded regression verification passed (logs/mvnf/20261001-122637-verify.log): TripleStorePartialIndexesTest 25, LmdbStorePartialIndexesTest 6, LmdbIndexAwareJoinOrderPlanningTest 10, StorePropertiesTest 4, TripleStoreTest 11, LmdbStoreReindexTest 1, RemoveAddTest 1, DefaultIndexTest 2; every class has zero failures and errors. Integration verification additionally reported 103 tests, 99 skipped, zero failures/errors. Reports are preserved under logs/partial-index-chain-focused/{surefire-reports,failsafe-reports} before the full-module runner clears targets.

Repository formatting passed in 3:48 minutes (logs/partial-index-chain-format.log); the subsequent root quick install passed in 3:36 minutes. Full-module verification is logged in logs/mvnf/20261001-123716-verify.log. It again passed TripleStorePartialIndexesTest (25), LmdbStorePartialIndexesTest (6), and LmdbIndexAwareJoinOrderPlanningTest (10). Observed failures in LegacyCardinalityEstimatorCompatibilityTest, TripleStoreAlignedSortResetTest, LmdbStoreErrorHandlingTest, TripleStoreAutoGrowTest, and LmdbOptimisticIsolationTest match methods failing in the previously recorded isolated-HEAD comparison logs/partial-index-baseline-all-failures.log. These use full-only configurations. Two estimator failures have different actual values, so this is method-level historical correspondence, not a fresh unchanged-baseline run. The runner remains active at the latest check; do not claim final full-suite totals or an all-green module. No unrelated failures were muted or patched.

## Concrete Steps and Validation

Working directory: /home/ken/Projects/rdf4j-ws/rdf4j. Use Java 25 and offline Maven with -Dmaven.repo.local=.m2_repo. Initial prerequisite command: mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install, logging to maven-build.log. Run focused tests with python3 .codex/skills/mvnf/scripts/mvnf.py TripleStorePartialIndexesTest#contextSubjectPairsResolveThroughSubjectPredicatePairs --retain-logs. Persist the first failing report at root without overwriting existing initial evidence. Broaden to TripleStorePartialIndexesTest,LmdbStorePartialIndexesTest,LmdbIndexAwareJoinOrderPlanningTest and then the LMDB module. Tests must not use -am or -q. Expect the new regression to fail with missing scpo before the fix and pass afterwards. Returned records must be unique, match all original bindings including context 0, and stale cs pairs must disappear after deletion while sp remains supported elsewhere.

## Idempotence and Recovery

No storage migration is necessary: projection and full record encodings remain unchanged. Reopen/backfill follows existing initialization. Retain untracked logs and unrelated edits; do not commit or push. On failure inspect Surefire reports and narrow the selection before retrying. No new dependencies.

## Interfaces and Dependencies

Use existing LMDB native cursors and ChunkInput for support probes, and RecordIterator/Txn for lazy resolution. The immutable plan holds partial stages and one TripleIndex terminal; its iterator supplies each PartialIndex with the remaining plan. Configuration requires at least one full index, while triple-term parsing stays full-only.

## Outcomes & Retrospective

The implementation introduces IndexResolutionPlan, shares its path/depth between execution and optimizer costing, composes PartialIndex iterators, selects a full support index by projected bindings, and retains native write-transaction support probes. Configuration accepts any full terminal; documentation and rejection tests reflect this. Expanded targeted verification passes, including the requested cs-to-sp-to-psoc chain, alternate op-to-cs-to-psoc chain, shortest-plan ties, all binding masks, graph isolation, deletion support, promotion, rollback, early closure, backfill and terminal replacement. Storage encoding and dependencies are unchanged. Formatting and builds pass. Full-module verification has reported historically recorded full-index-only failures and remains active at handoff. A full terminal without a useful bound prefix remains correct but can require scans; optimizer hop costing is a heuristic, not measured intermediate pair cardinality. Existing unrelated benchmark/testsuite edits are preserved.

Revision (2026-10-01): advance to verification after preserving the pre-fix failure; record long build time and existing header findings.

Revision (2026-10-01): add a focused regression and streaming-decoder remedy for context-first full support indexes, and update user-facing documentation.

Revision (2026-10-01): add alternate op-to-cs-to-psoc planning, shortest-chain tie selection, and early-close regression coverage. Advance back to expanded verification after the streaming support fix.

Revision (2026-10-01): preserve 60 passing unit regressions and integration reports; advance to formatting and full-module verification.

Revision (2026-10-01): record formatting/build success, feature passes in the broader run, and historically corroborated unrelated failures; qualify the still-running module verification rather than claiming final totals.





