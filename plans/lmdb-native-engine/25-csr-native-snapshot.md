# Native SNAPSHOT support: pinned read transactions + CSR generation chains

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained per `.agent/PLANS.md`.
Successor to `24-csr-mvcc-delta-overlay.md` (commit-merge, complete): because every commit-merge generation is a
complete immutable flat `CsrEntry`, snapshot support reduces to retaining superseded entries briefly and selecting
by revision — no merge-on-read, no reader-side version checks in the edge loop.

## Purpose / Big Picture

A transaction at isolation level SNAPSHOT must observe one consistent store snapshot for its entire lifetime —
natively, at this layer: both its LMDB B+tree reads and its CSR adjacency hits serve exactly the revision it
started at, even while later commits land. Today the store always serves latest-committed (`TxnManager.reset()`
renews read txns on every commit) and the CSR serves only the newest generation; SNAPSHOT semantics lean entirely
on the sail-base changeset layer, and mid-transaction commits from other connections leak into reads.

## What already exists (verified 2026-07-27)

- `LmdbSailDataset` owns ONE `Txn` for its whole lifetime; `LmdbSailSource.dataset(IsolationLevel)` receives the
  level and currently ignores it (except the estimator-refresh special case).
- `TxnManager.createReadTxnUntracked()` creates commit-surviving pinned txns (`active` value FALSE ⇒ skipped by
  `reset()`); they still participate in map-resize `deactivate()/activate()`, which moves their snapshot — but
  `Txn.version()` increments on every such transition, so snapshot invalidation is *detectable*.
- Commits hold the txn write lock across `mdb_txn_commit … dataRevision.incrementAndGet()`, so a read txn created
  under the read lock observes a `(B+tree snapshot, dataRevision)` pair that is mutually consistent.
- Commit-merge (plan 24): touched shapes are marked before the revision bump; merged entries are complete
  immutable flat arrays — a retained superseded entry IS a servable snapshot generation as-is (kernels included).
- `csrEligible=false` already exists for datasets whose txn is pinned (estimator refresh) — the exact hole this
  plan fills for SNAPSHOT datasets.
- Intra-query parallelism creates sibling untracked txns intended to share the source's snapshot but acquiring a
  fresh one — safe under latest-serving, a tear under SNAPSHOT (guarded below).

## Design

### Isolation mapping (per user decision: nothing above SNAPSHOT uses the CSR)

- **SNAPSHOT** → pinned dataset: untracked read txn + captured `snapshotRevision`, CSR serves via generations.
- **SERIALIZABLE** → unchanged txn behavior, `csrEligible=false` (bypasses the CSR entirely).
- **SNAPSHOT_READ (default) and below** → completely unchanged (tracked, reset-on-commit, latest-only CSR).
- Kill switch `rdf4j.lmdb.csrCache.snapshotServing` (default true): off ⇒ SNAPSHOT datasets behave exactly as
  today.

### Txn layer

- `Txn` gains `snapshotRevision` (−1 = unpinned) and records its creation `version()`.
- `TxnManager.createReadTxnPinned(LongSupplier dataRevision)`: under `lockManager.readLock()` — begin/renew the
  read txn, capture the revision, register untracked. The read lock excludes the commit critical section, making
  the pair exact.
- **Resize hazard, fail-loud:** map growth (`deactivate/activate`) silently renews pinned txns onto a newer
  snapshot. Pinned datasets check `txn.version()` against the creation version at read-entry points and throw a
  clear `SailException` ("snapshot invalidated by store map resize — retry the transaction") instead of serving a
  torn snapshot. SNAPSHOT permits aborts; it does not permit tearing.
- `TxnManager.minPinnedSnapshotRevision()`: min over active pinned txns' revisions, `Long.MAX_VALUE` when none —
  the reclamation watermark (active map is small; iteration under its monitor).

### CSR generation chains

- `CsrEntry` gains `volatile long validUntilRevision = Long.MAX_VALUE` and `volatile CsrEntry previousGeneration`
  (newest→older chain hanging off the slot head; only heads live in the eviction clock).
- An entry serves snapshot S iff `revision <= S < validUntilRevision`. `markTouchedShapes` (already inside the
  commit critical section, pre-bump) additionally stamps the head's `validUntilRevision = nextRevision` (tighten
  only). Reads of these fields by a pinned reader at revision R happen-after the marks of every commit ≤ R.
- `lookup(pred, direction, explicit, snapshotRevision)`: `snapshotRevision < 0` ⇒ existing latest-only path,
  branch-free for unpinned readers. Pinned ⇒ walk the (short) chain for the covering generation; miss ⇒ null ⇒
  the caller's pinned LMDB txn makes the B+tree fallback correct *by construction* — retention is purely a
  performance decision, exactly like the rest of the cache.
- **Retention policy (zero cost when unused):** on merge publish, the superseded head is chained ONLY when
  `minPinnedSnapshotRevision() < nextRevision` (an active pinned reader could need it) AND the bytes fit without
  evicting anything (`reserveBytesNoEvict`) AND the chain is below `rdf4j.lmdb.csrCache.maxGenerations`
  (default 3). Otherwise it is dropped as before. With zero open SNAPSHOT transactions the commit path is
  byte-for-byte the plan-24 behavior.
- Invalidation drops (overflow, kill switch, merge failure, eviction, close) release the whole chain — pinned
  readers fall back to LMDB. Pruning: at each `applyCommittedDelta`, chain nodes with
  `validUntilRevision <= minPinnedSnapshotRevision()` are unlinked and credited (plus the cap). All byte
  accounting flows through one chain-release helper used by every unlink site.

### Consult-site plumbing (LmdbSailStore)

Every CSR consult passes the dataset txn's `snapshotRevision`: `CsrProbeSupport` (probes, kernel adjacency,
`recordKernelDemand` gating), root-scan partitions, and — critically — `orderedIntegerDomain`: a merged entry's
recomputed neighbor min/max can be *narrower* than the pinned snapshot's data, so range pushdown from the wrong
generation would silently drop rows; it must consult the snapshot's generation or decline. `fanOutMean`/
`exactDegree` (estimation-only) also consult the snapshot generation for consistency. Parallel sibling txns for a
pinned dataset verify `snapshotRevision` equality at lease time; mismatch ⇒ decline parallelism for that query
(serial fallback), never mixed snapshots.

## Implementation checklist

1. `TxnManager`/`Txn`: `snapshotRevision`, creation-version capture, `createReadTxnPinned`,
   `minPinnedSnapshotRevision()`.
2. `LmdbCsrAdjacencyCache`: chain fields, snapshot-aware `lookup` overload (+ `countRun`-driven consults),
   retention in `replaceEntry` (watermark + no-evict reservation + cap), pruning in `applyCommittedDelta`,
   chain-aware byte release in every unlink path, kill switch.
3. `LmdbSailStore`: `dataset(level)` mapping; `LmdbSailDataset` pinned mode + `ensureSnapshot()` version check at
   read entry points; plumb `snapshotRevision` through `CsrProbeSupport`, root scans, ordered-domain, fan-out
   helpers; parallel-sibling revision guard; SERIALIZABLE ⇒ `csrEligible=false`.
4. Tests (`LmdbCsrSnapshotTest` unit + repo-level additions):
   - Pinned lookup at R0 returns the pre-commit generation byte-identically while latest returns the merged
     entry; content parity vs reference probes at both revisions.
   - Repo-level SNAPSHOT transaction: warm cache, concurrent commit from another connection, re-query inside the
     pinned transaction excludes the new statement (and includes it after commit+new txn); parity with the
     cache-disabled run (pinned-LMDB fallback correctness).
   - Retention gating: no pinned readers ⇒ nothing retained (bytes match plan-24 behavior); reader close + next
     commit prunes (bytes credited); cap enforced with oldest dropped and fallback correct.
   - Ordered-domain narrowing: removal shrinking min/max must not push a wrong range window onto a pinned reader.
   - Resize/version-bump: simulated `setActive(false/true)` on the pinned txn makes dataset reads throw.
   - Unpinned regression: existing suites (six CSR classes) stay green.
5. Format, headers, evidence, plan updates.

## Key risks

- **Torn-snapshot windows**: any consult path that reaches the cache without the dataset's snapshot revision
  serves latest silently. Mitigation: `csrEligible` is replaced at the dataset boundary by an explicit
  snapshot-revision value flowing through `NativeLmdbQuerySource`; grep-audit every `csrCache.` consult.
- **LMDB long-reader cost**: pinned txns block page reclamation (DB growth under churn) and hold reader slots —
  inherent to LMDB snapshots; surfaced via the existing MDB_READERS_FULL handling and documented.
- **Map resize breaks snapshots**: fail-loud by design (version check); the alternative (blocking resize on
  active pinned readers) risks writer starvation.
- **Estimator-refresh dataset** stays `csrEligible=false` (its snapshot is arbitrarily old; serving it from
  generations is pointless retention pressure).

## Progress

- [x] (2026-07-27) Ground truth verified (txn modes, resize/version mechanics, lock ordering, consult sites,
  parallel sibling gap); design locked; this plan authored.
- [x] (2026-07-27) Implementation complete: `Txn.snapshotRevision` + volatile `version`,
  `createReadTxnPinned(LongSupplier)` (revision captured under the read lock), `minPinnedSnapshotRevision()`;
  CSR generation chains (`validUntilRevision`, `previousGeneration`), snapshot-aware `lookup` +
  `tryScan/tryOrderedScan/tryCount/tryHas/tryPlanRootScanPartitions/meanFanOut/exactDegree` overloads, retention
  gated on the pinned watermark + `reserveBytesNoEvict` + `maxGenerations` cap, per-commit pruning, chain-aware
  budget release on every unlink path; `LmdbSailStore.dataset(level)` mapping (SNAPSHOT pins, SERIALIZABLE
  bypasses CSR, defaults unchanged), `ensureSnapshot()` fail-loud version check at read entry points, revision
  plumbed through every consult site, sibling txns inherit the pinned revision after the `mdb_txn_id` equality
  check. Kill switch `rdf4j.lmdb.csrCache.snapshotServing`.
- [x] (2026-07-27) **Root-cause fix in sail-base** (found by the red end-to-end test): `SailSourceBranch` caches
  the backing dataset in `snapshot` and only ever released it on close — sound only because reset-on-commit
  backends made the cached dataset silently track latest. With a genuinely pinned backing dataset, the branch's
  invariant ("snapshot + unflushed changes = latest") broke at flush time: flushed changes left the overlay while
  the cached snapshot stayed old (observed as a fresh SNAPSHOT transaction reading pre-commit state through the
  store-lifetime `SnapshotSailStore` branch). Fix: `flush()` retires the cached snapshot (also on the failure
  path); retired datasets close when observers drain and on branch close. Behavior-neutral for unpinned backends
  (they re-derive an equivalent live view).
- [x] (2026-07-27) Tests green: `LmdbCsrSnapshotTest` 8/8 (pinned-generation serving with same-instance retention,
  no-retention fast path, prune-on-reader-close, untouched-shape serving, fresh-build serving older pinned
  readers via the lastTouched clause, chain cap, kill switch, repo-level SNAPSHOT stability end-to-end);
  all seven CSR classes 57/57; core/sail/base 29/29; nativerdf regression sweep run for the sail-base change.

## Surprises & Discoveries

- `createReadTxnUntracked` + `Txn.version()` + the estimator-refresh `csrEligible` carve-out mean the platform
  was already shaped for exactly this feature; the missing pieces are revision capture, generation retention, and
  plumbing.
- The parallel-sibling path was already snapshot-exact — it compares `mdb_txn_id` between source and siblings
  and refuses on mismatch under a lease that blocks commits/resets; pinned datasets only needed revision
  propagation onto siblings, and a stale pinned dataset auto-declines parallelism via the existing id check.
- **sail-base's `derivedFromSnapshot` comment "this object already has at least snapshot isolation" documents an
  assumption the LMDB backend never satisfied**: the cached backing dataset was supposed to be a stable snapshot,
  but reset-on-commit made it a live view — which the branch's flush logic in turn depended on. Native pinning
  surfaced the contradiction; the retire-on-flush fix restores the documented invariant for both kinds of
  backends.
- The end-to-end failure mode was subtle: the shared store-lifetime branch flushed the writer's changes only
  after the reader closed (autoFlush is observer-gated), so the stale cached snapshot plus an emptied overlay
  served pre-commit state to a genuinely fresh SNAPSHOT transaction.

## Decision Log

- Decision: Generations = retained full entries (not deltas); retention gated on an active-pinned watermark, a
  no-evict reservation, and a small cap — zero commit-path overhead when no SNAPSHOT transaction is open, and
  eviction pressure never pays for history.
  Rationale: commit-merge already produces complete immutable entries; LMDB-fallback correctness via the pinned
  txn makes retention purely optional.
  Date/Author: 2026-07-27 / Claude (Fable) with hmottestad.
- Decision: SNAPSHOT pins; SERIALIZABLE bypasses the CSR (user directive); SNAPSHOT_READ default stays on today's
  reset behavior to keep the default path's blast radius at zero.
  Date/Author: 2026-07-27 / Claude.
- Decision: Fail-loud on resize-invalidated snapshots (detectable via `Txn.version()`), never serve torn data.
  Date/Author: 2026-07-27 / Claude.

## Outcomes & Retrospective

(pending)
