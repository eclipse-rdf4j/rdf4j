# Make paged adjacency authoritative in LMDB

This living ExecPlan follows `.agent/PLANS.md`. Update Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective as work proceeds.

## Purpose / Big Picture

RDF4J currently persists each statement in multiple permutation indexes and separately derives compressed adjacency pages in memory. Replace the permutation databases with three authoritative databases in the same statement LMDB environment: `adjacency-pages`, `adjacency-metadata`, and `adjacency-deltas`. The compressed pages and recent additions/deletions must survive reopen and process failure under the existing forceSync policy. Every read, including queries without the optional memory acceleration, must observe a consistent LMDB snapshot of all three databases. Rollback must discard all effects, including count changes and inferred-to-explicit promotion.

## Progress

- [x] (2026-11-01) Inspect current main and recover previous design.
- [x] (2026-11-01) Establish build and failing persistence contract.
- [x] (2026-11-01) Implement safe persisted page encoding and storage.
- [x] (2026-11-01) Integrate authoritative transactional adjacency storage.
- [x] (2026-11-01) Verify migration, snapshots, rollback, and recovery.
- [x] (2026-11-01) Format and package complete source plus patch.
- [x] (2026-11-01) Deliver verified source and patch.

## Surprises & Discoveries

Current main is `467b285c5345d590d5e7726ced9fe057e825b428`. Earlier work was reported as unpushed; no persistence artifact or branch was recoverable from the connected sources. Implementation therefore starts from this current main, preserving its latest map-resize corrections.

The current direct-adjacency delta collector is best-effort and can overflow. It cannot be the source of durable deltas. Native CSF pages contain relative section offsets, while shard directories contain process addresses. Only logical keys and encoded page payloads may be persisted. The packed decoder reads eight bytes beyond a packed tail; persisted values need bounded validation and padding before use.

The dictionary and statement databases have separate LMDB environments. Existing commit ordering commits dictionary data before publishing statements. This work retains that ordering and the existing retirement/liveness protocol. The three new statement databases share one transaction; no new cross-environment atomicity claim is made.

Bounded range planning changes legitimate work metrics. The full grouped-query fixture crosses the 2,048-row threshold for proposing a parallel alternative, adding 26 cursor probes and four point probes while the chosen prefix-run execution retains exactly 11 cursor probes and two representative quads. Tests now account for bounded planning separately from constant execution work. The two-quad snapshot fixture now has four nonempty build ranges, while both materialization passes still visit exactly four source rows.

Two authentic legacy fixtures deliberately omit store.properties. Their discovery test now declares sidecar presence explicitly while retaining all migration, statement, namespace, logical-order, marker, and reopen assertions. A mixed class/method Surefire selection omitted the nested optimistic-isolation suite; the final focused run selected that suite without method filters and executed all 117 cases. Stale reports were excluded from consolidated totals.

## Decision Log

Decision: use exactly three authoritative adjacency databases, keyed by raw predicate IDs and explicit/inferred outgoing/incoming planes. Rationale: predicate ordinals and native addresses are in-memory identities, while raw IDs remain meaningful across reopen. Date/author: 2026-11-01, Codex.

Decision: store small edge additions and tombstones in the delta database and compact touched page intervals into the existing CSF page format, packing neighboring small rows together. Rationale: updating one edge must not require rewriting an entire high-degree row on each commit. Pages, delta removal, and metadata updates happen under one writer transaction. Date/author: 2026-11-01, Codex.

Decision: preserve forceSync flags, logical mutation replay, dictionary publication ordering, and the existing Txn reader ownership mechanism. Rationale: these are the existing persistence and isolation contract, not optional acceleration behavior. Date/author: 2026-11-01, Codex.

Decision: retain the configuration's legacy index specification only as compatibility input/ordering descriptors where necessary; do not create or write permutation databases in the new format. Existing stores migrate within a single LMDB transaction before dropping old statement databases. Date/author: 2026-11-01, Codex.

Decision: keep valid `triple-indexes` sidecar text as logical scan preferences across reopening; the native authority manifest independently names `psoc,posc`. Four orders stream from persisted directories (`spoc`, `posc`, `psoc`, `opsc`); additional configured orders use the existing bounded spill sorter with snapshot and renewal guards. Date/author: 2026-11-01, Codex.

Decision: validate in `/tmp/rdf4j-persistent-adjacency-validation`, copied from the current checkout, because workspace synchronization restored stale compiled classes during a concurrent root build and introduced `.rsync-tmp` files in test cleanup. No production workaround is needed. The isolated root clean install completed successfully; final evidence will record source hashes and copy reports back to the deliverable. Date/author: 2026-11-01, Codex.

## Outcomes & Retrospective

The authoritative replacement is implemented: persisted bounded CSF pages, durable insertion/tombstone deltas, exact metadata, transactional compaction, four streaming scan orders, bounded sorting for other configured orders, migration, and old-writer rejection markers. The complete Java reactor build passes with front-end generation disabled, and formatting and copyright checks pass. No runtime dependency was added.

The consolidated run executed 1,156 cases in 48 suites and found four test-only expectation or fixture-precondition failures. After correcting those tests, the focused verify run executed 242 cases with zero failures, errors, or skips, including all 117 nested optimistic-isolation cases. Production source hashes are identical between the two runs. The latest results for all 1,273 cases across 49 selected test suites are passing; the full LMDB test suite and throughput benchmarks were not run. Full reports, exact commands, per-case result provenance, and source hashes are preserved in the handoff evidence.

The external compatibility process against old commit a7b7cedd1c954dc55fa8265f7e6fc4d215fdb1aa was not run because its compiled classpath is unavailable. Pinned-source inspection, authentic legacy fixtures, and all 13 current fence regression cases support the rejection design; actual attempted writes by that old process remain unverified. The code is ready for review, with complete module source and an apply-checked patch prepared for handoff. No commit or push has been performed under the repository's explicit-authorization rule.

## Context and Orientation

All implementation code is in `core/sail/lmdb`. `TripleStore.java` owns the statement LMDB environment, write transaction, mutation replay, count and query entry points. `TxnManager.java` owns reader handles and guards against use during map growth. `LmdbSailStore.java` coordinates dictionary and statement publication and the optional in-memory adjacency projection. `csf/CompactCsfPageEncoder.java` and `csf/CompactCsfPageReader.java` define the compressed sparse-fiber (CSF) format: one row identifies a node; each neighbor identifies an edge; a sorted context run identifies the graphs containing that edge. `LmdbRecordIterator.java` and prefix-run iterators expose statements to query evaluation. The new persistence codec must reuse this page encoding while validating lengths and format before unsafe reads.

## Plan of Work

First build with Java 25 and Maven using the workspace `.m2_repo`. Add `LmdbPersistentAdjacencyContractTest` to commit data, reopen, and inspect native database names. Observe the current implementation failing the requirement that the three adjacency databases replace permutations.

Next implement `csf/PersistentCsfPageCodec.java` for bounded portable persistence of one page. Implement `LmdbPersistentAdjacency.java` to manage page, metadata, and delta handles opened in the same transaction. Add and remove operations write outgoing and incoming delta keys and exact metadata counts in the provided transaction. Scans merge base pages and deltas with latest state taking precedence, preserving statement set semantics. Compaction writes new page bytes and deletes their absorbed deltas atomically. Reader cursors use the supplied transaction and never consult current-state global metadata.

Integrate the new backend in `TripleStore.java`, including scalar/batch writes, replay, count and liveness queries, raw/ranged/retained/prefix scans, bulk ingest, and initialization. Existing stores must be read with their existing native index manifest, migrated completely, and only then have those databases dropped in that same transaction. A failed migration aborts to the old committed store. Protect format markers against unsupported or corrupt bytes.

Finally run the focused persistence tests plus existing growth, mixed-plane, snapshot, value reclamation, isolation, query and process-crash suites. Review any obsolete expectations about physical permutation indexes without weakening semantic assertions. Deliver complete changed source and a Git patch, clearly stating scope and remaining limits. Repository policy requires explicit instruction before commit/push; perform all implementation and verification first.

## Concrete Steps

Working directory is `/workspace/scratch/a18270bf6d6e/rdf4j`. The toolchain is installed outside the repository under `../toolchain`; Java must be version 25 or later. Every Maven invocation uses `-Dmaven.repo.local=.m2_repo`. Initial root build uses `-Pquick clean install`, with tests skipped, and starts offline; fetch missing dependencies online then return offline. Tests run only in the LMDB module and never use `-am` or `-q`.

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbPersistentAdjacencyContractTest -DskipITs verify

Exact commands and evidence will be recorded below as the environment is established.

## Validation and Acceptance

The new contract test must fail on untouched main, then pass after integration. Additions, removals, inferred promotion, duplicate inserts, and clear must produce identical statement sets before and after reopen. An old read transaction must retain its original view while another writer commits and compacts. A rollback must leave pages, counts, and deltas unchanged. Process termination must recover either the old or new complete transaction, never a partial adjacency direction or mismatched metadata. Existing large-transaction map growth must replay all logical writes without prematurely publishing any. Migration must remove old physical statement indexes only after new data is complete. Value liveness checks must not reclaim a value still referenced by any visible quad.

## Idempotence and Recovery

New-format initialization validates the durable format marker and opens existing handles. Migration is transactional and rerunnable after abort. Development tests use temporary stores, never user data. Preserve unrelated files and untracked artifacts. No remote branch update, PR, or commit is required to validate this feature.

## Artifacts and Notes

Retain `initial-evidence.txt`, build logs, focused test logs, and the final patch. The changed-source archive contains complete files, not excerpts. Record the base commit so the patch can be applied reproducibly.

## Interfaces and Dependencies

Reuse existing LWJGL LMDB, Eclipse Collections primitive containers, Java 25, and existing CSF encoding; add no runtime dependency. `LmdbPersistentAdjacency` accepts an existing native transaction for every operation. Its principal interfaces are `open(long txn)`, `add/remove/contains(long txn, s, p, o, c, explicit)`, `scan(Txn txn, s, p, o, c, explicit)`, `scan(long txn, ...)`, `count(long txn, ...)`, and `compact(long txn)`. All database handles refer to the TripleStore environment.

Revision note: initial plan records current main, unrecoverable prototype, agreed three-database format, transaction requirements, and parallel ownership before production changes.

Initial gate: root quick install passed. LmdbPersistentAdjacencyContractTest: Tests run 1, Failures 1, Errors 0, Skipped 0. Existing DBs lack adjacency-pages/adjacency-metadata/adjacency-deltas. Production implementation may proceed.

Second implementation checkpoint: multirow page packing bounds each compaction step to one existing page plus at most 1024 deltas, avoiding per-row page overhead. Metadata types9/10 record pending counts and latched compaction candidates per predicate/direction; normal commits visit candidates only. A one-page decoded cache belongs to an immutable reader and is discarded on renewal. Writer scans do not cache page values. Migration validates populated legacy sources before dropping any old tree.

Final verification evidence: adjacency-focused-correction-verification.log reports Tests run: 242, Failures: 0, Errors: 0, Skipped: 0 and BUILD SUCCESS. VERIFICATION_RESULTS.json records the latest result for each of 1,273 distinct cases, without double-counting reruns or a stale optimistic-isolation report. The initial one-case contract failed on unchanged main because the three adjacency databases did not exist; that same contract passes in the consolidated run.

Revision note: completed implementation and validation, documented bounded-planning test attribution and authentic fixture preconditions, recorded exact run totals and the external old-writer limitation, and advanced the single active step to artifact packaging.

Handoff checkpoint: the archive contains all 1,916 module files, including complete Java sources, test resources, and the module POM. The 46-file patch applies to base 467b285c5345d590d5e7726ced9fe057e825b428 and reproduces every changed file byte for byte. The final active step is delivery of the verified archive and patch; implementation, validation, and formatting are complete. The branch uses GH-0000 because no issue number was supplied. No commit or remote publication is authorized by the present request under AGENTS.md.
