# Construct persistent adjacency directly during an initial NONE import

This living ExecPlan follows `.agent/PLANS.md`. Keep Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective current.

## Purpose / Big Picture

A first explicit transaction at isolation NONE (the existing non-atomic bulk import mode) should build the final outgoing and incoming adjacency pages, not insert every statement into temporary native delta tables and immediately delete/rewrite them. The existing import owner, failure marker, paired dictionary/triple commits and retained sorted runs remain authoritative. Existing nonempty stores and inferred writes retain their ordinary mutation paths. Correct results, exact metadata, recovery and current-revision query readiness are acceptance criteria alongside faster loading.

## Progress

- [x] (2026-11-02 01:36Z) Created clean isolated worktree at published 17781a9; captured full-scale native insertion and compaction CPU evidence.
- [x] (2026-11-02 01:44Z) Captured actual async native-work red: 1 failure, 0 errors; exact live/reopen 4,096 rows; 8,192 delta puts/deletes, 67,596 metadata puts, 67,722 point metadata gets. Initial spill-setup-only failure is archived separately.
- [x] (2026-11-02 01:58Z) Captured fan-in 129>32 red; direct builder and bounded fan-in pass 41 targeted cases. Captured 5 boundary reds, then all 5 passed with bounded interrupt checkpoints and reduction-record validation.
- [x] (2026-11-02 02:12Z) Independent model/admission cases pass 4/4; a frozen-JAR 550K-input A–B–B–A sample verifies exact live/cold models and current-revision readiness.
- [x] (2026-11-02 02:18Z) Captured three real merge ownership/cancellation failures, then all four resource controls passed. Boundary controls pass 7/7.
- [x] (2026-11-02 02:31Z) All eight actual native recovery/process-crash controls pass. Earlier setup-only reverse-lookup/shell-ID failures and wrong-thread resize instrumentation are preserved; the final tests observe real fresh assignments and native growth on the coordinator.
- [x] (2026-11-02 02:35Z) Coherent Maven format/compile/test gate: 116/116 across 15 suites. Final frozen-JAR 550K ABBA confirms exact live/cold results and adjacency readiness: mean commit 7.058→1.609 s, input-to-ready 12.418→7.033 s.


- [ ] [in_progress] Publish the independently reviewed increment with a fresh remote lease, then observe the new full-scale CI. Parent explicitly authorized superseding the old still-running diagnostic job after the local gates; its original terminal exception remains unobserved.

## Surprises & Discoveries

The full Theme fixture contains 14,772,659 input statements. On published 17781, final commit is still active at 600 seconds: the ingest thread uses 299.7 CPU seconds per 300 wall seconds in native delta deletion from compactPartition. Earlier samples show scalar storeTripleDirect delta and metadata puts. This is active work, not an observed lock cycle. The previous canonical main CI loaded the same fixture in 29.042 seconds, but different CI hardware prevents a controlled speed ratio.

LmdbSortedRuns.Merge opens all input files and uses 256 KiB direct buffers per file. LmdbRunCollector may grow provisional uncommitted rows beyond its nominal chunk budget. Neither may be described as strictly bounded without addressing those paths. Shared value-run consolidation must preserve duplicate records because the value importer diagnoses duplicate keys.

## Decision Log

The optimized scope is only the existing fresh explicit initial-NONE owner. A zero logical count is not sufficient admission: pages/deltas must actually be empty and metadata must contain only the format certificate in the same native writer. No residual authority is discarded.

Use two unsigned-coordinate orders, PSOC and POSC. S/P/O/C mean subject, predicate, object and context. A page holds up to 1,024 neighbor/context pairs for one predicate/direction. Final metadata counts must include each role incidence (an ID used twice in one statement contributes twice), and context 0 denotes the default graph but is not a reference incidence.

Retain derived per-distinct-statement callbacks on one outgoing pass so fan-out and in-memory adjacency invalidation cannot publish a false empty delta. Do not broaden the general bulk-loader or cache-transfer protocol.

## Outcomes & Retrospective

The direct dense fixture now writes 8 pages, 332 metadata keys once, zero deltas/dirty keys and 3 point metadata reads. Forty-one targeted cases pass. Preliminary 110K-row ABBA measurements (two forks/version, same JVM/config, retained 5-second stack watcher) show mean commit 1.295→0.540 seconds and input-to-adjacency-ready 4.785→3.891 seconds. These are not full-scale predictions. All four independent models pass after correcting their setup-only invalid budget to the existing 256 MiB minimum. Frozen-JAR 550K-input measurements show mean commit 6.506→1.772 seconds and input-to-adjacency-ready 11.513→7.915 seconds, with exact live/cold membership. Post-commit adjacency waiting itself increased, so the total rather than commit alone is the useful end-to-end metric. Actual assignment observation now proves retained IDs without querying deliberately deferred reverse lookups. All eight recovery/crash cases pass, and an expanded 108-case import/value-run gate passes; the combined full-lifecycle gate passed all 116 cases. Final frozen-JAR confirmation passed all four forks and retained exactness; source and compiled hashes are archived. No publication has occurred.

## Context and Orientation

All paths below are relative to the repository root. `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` owns InitialRunImportFinish and its retained replay work. `LmdbInitialRunImport.java` owns the marker, sorter and run cleanup. `LmdbRunCollector.java` and `LmdbSortedRuns.java` collect, sort and merge fixed four-long records. `TripleStore.appendInitialImportRuns` currently merges only PSOC and calls scalar storeTripleDirect for every distinct quad. `LmdbPersistentAdjacency.java` stores pages, delta records and exact metadata; `PersistentCsfPageCodec` encodes final pages.

## Plan of Work

First add LmdbInitialRunImportDirectAdjacencyWorkTest under the module test package. Observe actual native calls on the asynchronous writer using existing lifecycle hooks, delegating every call to LMDB. A dense 4,096-quad fixture with duplicates must have exact live and reopened RDF membership before checking work. Its desired final structure is 8 pages and 332 non-format metadata keys, each written once, no edge-delta puts/deletes and no dirty-directory keys. Preserve the failing report.

Then add a direct persistent writer with a same-writer physical emptiness check, sequential PSOC/POSC page streams, and a fixed-memory external reduction of context and reference counts. Consolidate retained input runs with bounded fan-in and buffers before returning replay-owned immutable runs. Do not deduplicate value records. Complete pages and metadata within the existing single triple writer; existing rollback/replay owns failures. Independent reviewers supply disjoint model/admission and native failure/crash tests after the concrete internal API is agreed.

## Concrete Steps

Use Java 25 and Maven 3.9.11 via `source /tmp/rdf4j-tools/env.sh`. In the isolated direct-load worktree run:

    mvn -o -f core/sail/lmdb/pom-repairs.xml -Dmaven.repo.local=/workspace/scratch/bfa8fbb86df5/rdf4j-persistent-adjacency-repairs/.m2_repo -Dtest=LmdbInitialRunImportDirectAdjacencyWorkTest -DfailIfNoTests=false -Dcheckstyle.skip -Drdf4j.lmdb.test.maxHeap=3g test

The local untracked POM preserves Mockito agent arguments and limits one fork to 3 GiB. Do not use -am or -q. No other process may mutate that cache during this task. Archive logs and XML under repair-evidence/direct-load. There is less than 1 GiB free overlay space; avoid full-scale local fixtures and additional worktrees/cache copies. Small temporary fixtures may use /tmp with explicit memory accounting.

## Validation and Acceptance

The new test must first fail on measured scalar delta work, then pass without weakened exactness. Run existing initial import, terminal cleanup, admission ordering, native replay and adjacency-ready suites. Independently verify both physical directions, metadata, default/named contexts, role multiplicities, unsigned coordinates, duplicate runs and cold reopen. Inject real native page and metadata failures and preserve close-before-abort-before-resize, exactly-once publication and retained replay. Measure identical bounded datasets before/after; keep full-scale CI observations distinct from matched measurements.

## Idempotence and Recovery

Only owned temporary stores may be reset; preserve all untracked evidence. A failed native writer must be aborted before resize and re-executed from retained immutable runs by the existing owner. No new helper may acknowledge a partially built generation. Partial consolidation files remain cleanup-owned and must not replace authoritative retained lists until complete. Do not push over external changes or cancel the productive diagnostic CI.

## Artifacts and Notes

CI run 41370697767 job 125041082151 supplies phase and stack evidence. Source base 17781a9 is clean. Native structural-red receipts and implementation hashes will be added when captured. The earlier repair worktree retains all previous passing reports.

## Interfaces and Dependencies

Use existing LMDB bindings, PersistentCsfPageCodec and replay ownership. The reviewed internal writeInitialExplicit helper accepts retained PSOC/POSC runs, an owned scratch parent, and the existing derived-event callback. No public API or test-only production hook is added. New bounded metadata spill records must retain signed long bit patterns and compare keys unsigned. Required physical DBIs are adjacency-pages, adjacency-metadata and adjacency-deltas.

Revision note: Initial plan recorded the observed two-pass scale regression and the authorized narrow scope before production changes. Subsequent updates record actual red/green and performance receipts, with recovery and full-scale validation still open.
