```md
# Prove that LMDB adjacency and snapshot reads never lose or invent rows under concurrent writes

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It is maintained in accordance with `.agent/PLANS.md` at the repository root.


## Purpose / Big Picture

The LMDB store in `core/sail/lmdb` answers many statement lookups from the "direct adjacency" index: an in-memory copy of the LMDB triple indexes, built from a pinned LMDB snapshot and then kept current by applying each commit's changes (a "delta"). A user relies on every answer being exactly what LMDB holds for the reader's snapshot. In October 2026 CI caught one answer that was missing a committed row. The cause was a build that resolved its list of covered predicates before pinning its snapshot (fixed in commit `d43939d99e`). The user asked for much broader analysis and test coverage, so that heavy concurrent writes, reads, commits and rollbacks in any overlap cannot lose data or answer a query with inconsistent data.

After this work, the module has three kinds of evidence. The first is a randomized concurrent stress test: writers commit and roll back at every isolation level, readers query inside SNAPSHOT transactions, and a chaos thread forces adjacency rebuilds. Every answer is checked against LMDB's own indexes for the same snapshot, and the final store against a model of everything that committed. The second is a deterministic matrix: each kind of write is injected at each phase of an adjacency build through test hooks, and the answers are then checked. The third is two independent code audits of the adjacency and snapshot code, with each finding either fixed (with a failing test first) or explained. Anyone can run `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyConcurrencyStressTest --module core/sail/lmdb` and see it pass. A longer soak runs through a system property.


## Progress

- [x] (2026-10-26 05:30Z) Reproduced the CI missing-row failure deterministically (`LmdbDirectAdjacencyQueryTest#selectedPredicateFirstCommittedWhileABuildStartsKeepsItsBaseRows`), fixed it, and committed `d43939d99e`. 506 adjacency tests pass.
- [x] (2026-10-26 05:40Z) Started two read-only audits: adjacency build, catch-up and publication; LMDB snapshot and transaction lifecycle.
- [x] (2026-10-26 07:40Z) Randomized concurrent stress test (`LmdbDirectAdjacencyConcurrencyStressTest`) with the per-snapshot cross-path oracle, SHADOW comparisons on every probe and a final model check. It found three defects, each fixed after a deterministic failing test:
  - a quiescent rebuild that yielded to live views was never resumed once a gap changed the maintenance state (`LmdbDirectAdjacencyRecoveryTest`);
  - another NONE connection's rollback cleared the replay decision of a writer waiting for adjacency admission (`LmdbAdjacencyWriteAdmissionTest`);
  - rolled-back NONE changes may stay, as `IsolationLevels.NONE` allows, so the test resyncs a writer's model after a NONE rollback.
  After the fixes it passed 3 rounds of 30-second cases under 24 busy processes.
- [x] (2026-10-26 08:40Z) Triaged the audit findings. Five confirmed defects were fixed after a failing test:
  - the predicate domain dropped predicates whose deltas cancelled out (`LmdbAdjacencyPredicateDomainTest`);
  - node-domain answers ignored pending tables (`LmdbAdjacencyNodeDomainTest`);
  - an interrupted first import wiped committed namespaces (`LmdbInitialRunImportTest#namespacesCommittedBeforeTheFirstImportSurviveAnInterruptedImport`);
  - value GC recorded a collected term's components at the term's horizon (`LmdbRetiredValueCascadeTest`).
- [x] (2026-10-26 10:10Z) Reader reset after an interrupted dictionary commit: a dictionary commit interrupted after its native commit skipped the reader reset and lost the interrupt. `ValueStoreInterruptedCommitTest` failed before the fix. Both reset paths now take the write lock uninterruptibly and restore the interrupt.
- [x] (2026-10-26 10:00Z) The suspected missing wakeup for a catch-up waiting on a gap was not reproduced. A connection write waits for its own delta's publication (`endLogicalCommitBatch`) before it releases write admission, so the cutover cannot close admission while that handoff is pending. No production change was made; the attempted test is parked outside the repository.
- [x] (2026-10-26 10:10Z) The deterministic hook matrix (`LmdbDirectAdjacencyBuildInterleavingTest`) passes all 30 cases: 2 coverages × 3 phases × 5 write kinds.
- [ ] `LmdbRealEstateNativeParityTest` query 4 on CI: native COUNT 1 against 3,104, seen at `1415c7d53b` and `fc528bf4d1`. It did not reproduce in 40 local runs, 20 of them under load, with the `7250c3da97` fixes.
- [ ] Full module run, loaded repeats, push, CI.


## Surprises & Discoveries

- Observation: the store's built-in SHADOW mode (`DirectAdjacencyMode.SHADOW`) compares the adjacency answer with LMDB on the same read transaction for bound-node probes. Setting `rdf4j.lmdb.directAdjacency.shadowSampleEvery=1` makes that comparison run for every probe and count mismatches in `LmdbAdjacencyMetrics.Snapshot.shadowMismatches`. SHADOW mode never serves the adjacency answer, so it complements, rather than replaces, a PREFER-mode check.
  Evidence: `LmdbDirectAdjacencyStore.shadowCompare`.
- Observation: `getStatements(null, null, null)` can itself be served from adjacency (`rdf4j.lmdb.directAdjacency.rootScan.enabled` defaults to true). A full scan used as ground truth must therefore turn root scans off.


## Decision Log

- Decision: the PREFER-mode oracle compares each pattern's answer with the matching subset of a full LMDB scan made inside the same SNAPSHOT transaction.
  Rationale: both reads use one snapshot, so any difference is a wrong answer. The check needs no internal hooks and goes through the public Repository API, including the store's routing between adjacency and LMDB.
  Date/Author: 2026-10-26, Claude Code.
- Decision: each writer owns its subjects and its named graph, while predicates and objects are shared.
  Rationale: the final model is then simply the union of each writer's committed changes, whatever order commits interleave in. The adjacency rows keyed by shared objects and predicates still see concurrent updates from every writer.
  Date/Author: 2026-10-26, Claude Code.


## Outcomes & Retrospective

(To be written at completion.)


## Context and Orientation

Code lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. `TripleStore` owns the LMDB triple indexes and a monotonically increasing `dataRevision`, bumped by every committed triple transaction while the TxnManager's lock manager write lock is held. `TxnManager` hands out LMDB read transactions ("readers"), pools them, and resets them after each commit. `LmdbDirectAdjacencyStore` builds the adjacency index from a family of readers pinned at one revision (`LmdbAdjacencyBuildTxnFamily`). It then catches up by applying queued commit deltas until it reaches the current revision, and publishes the result. After that, each commit's delta is applied synchronously. A published state records `baseRevision` (the snapshot it was built from) and `appliedRevision` (the last delta applied). A read is "exact" when the published state's applied revision equals the reader's snapshot revision. Otherwise the read falls back to LMDB with a closed-enum reason. With SELECTED coverage, only configured predicates are indexed. A predicate that does not exist yet when a build starts is "unresolved" and becomes covered when its first delta arrives.

Test hooks are package-private `volatile Runnable ...ForTest` fields on `LmdbDirectAdjacencyStore`, for example `beforeBuildSnapshotForTest`, `afterBuildScanForTest`, `beforeOnlineCatchUpForTest`, `afterWriteAdmissionClosedForTest`, `beforePagedCsfPublicationForTest` and `beforeQuiescentBuildForTest`. `scheduleQuiescentRebuild()` forces a rebuild.


## Plan of Work

First, add `LmdbDirectAdjacencyConcurrencyStressTest` in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/`. It is parameterized by coverage (FULL, SELECTED) and mode (PREFER, SHADOW), and uses fixed seeds and a few seconds per case. Three writers each run transactions of one to six random adds and removes on their own subjects. A transaction may also clear the writer's own named graph or introduce a late predicate. Isolation is chosen at random among NONE, READ_COMMITTED, SNAPSHOT_READ and SNAPSHOT, and about a fifth of the transactions roll back. Two readers open SNAPSHOT transactions and run the cross-path oracle. A chaos thread forces rebuilds at random intervals. At the end, the test waits for adjacency to be ready and checks three things: the store's full contents equal the model, every pattern equals the matching subset of the model, and SHADOW mode recorded comparisons and no mismatches.

Second, add a deterministic hook matrix test. For each hook and each write kind, it runs the write from another thread while the hook holds the build at that point, then checks the oracle and the model. The write kinds are: first commit of a selected predicate, an add to an existing row, a removal of a base row, a rolled-back add, and clear of a named graph. A hook that closes write admission blocks writers by design, so the test does not wait for the write inside that hook.

Third, triage the audit reports. Each credible defect gets a failing test first, then the smallest root-cause fix.


## Concrete Steps

From the repository root:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyConcurrencyStressTest --module core/sail/lmdb --retain-logs

A longer soak (milliseconds per case):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbDirectAdjacencyConcurrencyStressTest --module core/sail/lmdb -- -Drdf4j.lmdb.adjacencyStress.millis=60000


## Validation and Acceptance

The stress test and matrix pass at their default settings, pass under CPU load (24 busy processes on 16 cores), and pass a one-minute soak per case. A deliberately reintroduced defect fails them: for example, moving `resolveCoverage()` back before the pin in `LmdbDirectAdjacencyStore.buildOnce`. The full `core/sail/lmdb` suite stays green.


## Idempotence and Recovery

The tests use `@TempDir` stores and restore every system property they change, so they can be rerun at will.


## Artifacts and Notes

The CI failure fixed by `d43939d99e` reported `baseRevision=1`, `lastCutoverQueuedCommits=1`, `lastCutoverRevisions=0`, and a missing `(s1, p1, o1)` in the default graph.


## Interfaces and Dependencies

No production interfaces change. The tests use the Repository API, `LmdbStore.getBackingStore()`, `LmdbSailStore.directAdjacencyStore()`, `LmdbDirectAdjacencyStore.snapshotMetrics()`, `scheduleQuiescentRebuild()`, and the `...ForTest` hooks named above.
```
