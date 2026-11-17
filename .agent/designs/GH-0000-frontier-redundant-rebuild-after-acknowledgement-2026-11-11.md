# A Frontier rebuild's own acknowledgement makes the next rebuild redundant (2026-11-11)

Status: specified, not implemented. Routine A (behaviour change: fewer rebuilds, fewer Frontier revision moves).

## Observation

Found while root-causing the intermittent
`LmdbPlanningReadLogStoreTest#aLearnedLogicalEstimateRecordsTheMappedBoundsCheckAndReplaysIt`
(`impl-readlog/a2-round2-report.txt`, raw logs in scratchpad `a2r2-evidence/flake/`):

- The fixture loads data (its commit requests a background rebuild) and then calls
  `rebuildFrontierStatistics()`, which publishes generation 5 covering epoch 5, sequence 2000.
- The scheduled rebuild then starts, sees snapshot epoch 6 at the same sequence 2000, and publishes generation 6
  1.0–4.2 s later (7 of 8 trials). No commit happened in between.

## Mechanism (by reading; to be confirmed by the implementer before any change)

- `LmdbSailStore.rebuildFrontierStatisticsV2` publishes the base (`publishCurrentOwnerBase(manifest)`; the
  manifest's covered epoch is the build snapshot's `mdb_txn_id`) and then calls `acknowledgeFrontierCoverage`.
- `TripleStore.acknowledgeFrontierMutationsThrough` prunes acknowledged journal rows in an LMDB write transaction of
  its own. That commit advances the LMDB transaction id, i.e. the snapshot epoch, without any statement change.
- `acknowledgeFrontierCoverage` reports this to the service with `recordMetadataOnlySnapshotAdvance(epoch,
  sequence)`, which only raises `latestStoreEpoch`.
- `frontierSnapshotAlreadyCovered` requires `status.coveredEpoch() == snapshot.snapshotEpoch()` and equal covered
  sequence. After the acknowledgement the snapshot epoch is one higher than the generation's, so the check fails
  and a rebuild that was already requested rebuilds everything.

The same shape may exist on the delta path (`refreshFrontierStatisticsDelta` also acknowledges after publishing)
and at open (`LmdbSailStore` ~:8472–8476).

## Why it matters

- A wasted full statistics pass after every rebuild that had a request pending.
- A second Frontier generation: the Frontier planning revision moves again, so every cached plan decision is
  invalidated twice and every learning applicability is re-keyed (learned evidence gathered between the two
  publications is stranded under the first generation's keys).
- Timing-dependent test outcomes (the flaky fixture; probably also the "planning across a Frontier publication"
  logs the A2 reviewer saw 3–22 times per run in the default profile).

## Required behaviour

1. A rebuild (or delta refresh) whose snapshot differs from the current generation's only by commits the store
   itself recorded as metadata-only at the generation's covered sequence is "already covered": no build, no new
   generation, no Frontier planning-revision move, pending-rebuild flag cleared as today.
2. The proof of "only metadata-only commits in between" must be what the code can actually know. Sequence equality
   alone is acceptable only if the implementer shows, by reading, that every commit that can change what the
   statistics describe advances the Frontier journal sequence (statement adds/removes on every write path
   including bulk and deferred paths, clears, and anything else the builder reads). Otherwise the service keeps
   the epoch up to which metadata-only advances were recorded contiguously for the current generation and the
   check uses that. An interleaved statement commit must still force the rebuild.
3. A metadata-only advance must not change the Frontier planning revision or any generation identity that cached
   decisions or learning applicabilities are keyed by.
4. Unchanged: `rebuildRequired`, availability other than READY, an overdue base refresh, journal states that
   require a base rebuild, and a snapshot with a higher sequence all still rebuild.

## Tests (red first, store level through the public API unless noted)

- After a load whose commit requested a rebuild, `rebuildFrontierStatistics()` followed by the scheduled rebuild
  running to completion leaves the generation id and the Frontier planning revision unchanged, and the service
  reports no second build. Deterministic: drive the scheduled rebuild through an existing seam or a new
  package-private one; no sleeps.
- The same with a statement commit between the two: the second rebuild (or a delta) does publish.
- The delta path and the open path, if the implementer confirms they have the same shape.
- Service-level unit tests for whatever state rule 2 adds (interleaved statement commit breaks the chain; a
  metadata-only advance at a stale sequence is refused, as today).
- `aLearnedLogicalEstimateRecordsTheMappedBoundsCheckAndReplaysIt`: 20 consecutive standalone runs green after the
  fix (it failed 2 of 11 and 3 of 8 before); if it still fails, the remaining cause is reported, not papered over.
- One mutation per rule.

## Not in this change

The acknowledgement being a separate LMDB writer; the rebuild schedule policy; anything in the plan caches.

## Implementation hand-back (2026-11-11 evening; private copy `trees/frontier-ack`, not merged)

Evidence: `/private/tmp/rdf4j-phase1-continue-20261109-2OK1vQjv/impl-frontier-ack/` (`report.txt`, `changes.diff`,
`red/`, `green/`, `two-copy-comparison.txt`, `mutations/summary.txt`, `flake-base-10/`, `flake-tree-20/`).

- Mechanism confirmed by probe (5 of 5 trials): load = txn 4 (sequence 2000), deferred-domain commit = txn 5,
  explicit rebuild publishes generation 5 covering epoch 5, acknowledgement = txn 6 at the unchanged sequence, the
  scheduled rebuild compares snapshot 6 with epoch 5 and publishes generation 6. The delta path had the same
  defect (generation 7 to 8); the open path only when the previous run stopped between publication and
  acknowledgement (5 to 6).
- Rule 2 resolved to contiguity, not sequence equality. Sequence equality is not a proof:
  `TripleStore.appendInitialImportRuns` (initial NONE run import) writes every index row with `MDB_APPEND` and
  never records a journal mutation; `LmdbNativeBulkStore` append paths bypass the journal too (new, empty
  directory only); namespace, domain, index-build and backup-boundary commits are metadata-only but nothing reports
  them as such.
- Change (5 files): `TripleStore.acknowledgeFrontierMutationsThrough` returns
  `FrontierAcknowledgementCommit(epoch, unchangedSequence)` (its own `mdb_txn_id`, taken inside its write
  transaction) or null; `LmdbStatisticsService.recordMetadataOnlyCommit` extends a `MetadataOnlyCommits(coveredEpoch,
  coveredSequence, throughEpoch)` record only when the commit is the immediate successor of the covered epoch (or
  of the last recorded one) at the covered sequence; `describesSnapshot(epoch, sequence)` replaces the epoch
  equality in `frontierSnapshotAlreadyCovered`; `status()` untouched, so no planning identity moves. Two
  package-private seams (`runScheduledFrontierRebuild`, `rebuildScheduledFrontierStatistics`). No persisted-format
  change.
- Owner review of the diff: sound. The proof rests on LMDB write-transaction ids being consecutive for committed
  transactions, so "immediate successor" means no other committed change lies between; a record keyed by covered
  epoch and sequence cannot leak to another generation's snapshot; a statement commit during the build makes the
  acknowledgement's sequence differ from the manifest's, so it is not recorded; a wrong "covered" answer can only
  serve older statistics, never change a query result. No separate independent review requested.
- Results: 13 new tests red-then-green as specified (8 new; 3 guards green both sides); two-copy comparison 282
  green in both, none green-to-red; the flaky read-log fixture 2 of 10 red on base, 20 of 20 green with the fix;
  6 mutations, all caught.
- Limits kept: after an ordinary reopen the first explicit or pending base rebuild still builds once (nothing
  persisted proves the commits in between were metadata-only; fixing that needs a format change). Namespace,
  domain and index-build commits still break the chain and rebuild as before.
- Pre-existing gap found, not fixed, not reproduced: because the initial run import is not journaled, the delta
  path and `describesUncoveredEpoch` (lease acquisition) treat equal sequences as "no statement change". If such
  an import lands after a base was built for an empty store, that base may keep being served. Needs a reproducer
  before any change.
- Merge prepared: real tree equals the copy's base for all four modified files; list
  `impl-frontier-ack/merge-list.txt`; `LmdbSailStore.java` combined with the phase B change at
  `impl-frontier-ack/merge-real/LmdbSailStore.B+frontier.java` (three-way clean, 31 + 4 changed lines).
