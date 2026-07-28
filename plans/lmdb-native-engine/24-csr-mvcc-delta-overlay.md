# CSR commit-merge: updatable adjacency cache — commits update entries in place, never wipe-and-rebuild

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained per `.agent/PLANS.md`.
**Status: v3 FINAL DESIGN — single complete implementation, no phases.** v1 (entry-level fold) and v2 (VG-CSR
generations) were design iterations; v3 supersedes both after two decisive findings (below) collapsed the problem.

## Purpose / Big Picture

`LmdbCsrAdjacencyCache` currently drops **every** entry of **every** predicate on **every** committed write
(`committedWrite()` → `dropAllEntries`), then re-pays a full two-pass LMDB sweep (JNI cursor traffic ∝ predicate
pair count) once probe traffic re-crosses the adaptive trigger. Goal: **a commit never wipes a CSR entry again.**
Untouched shapes keep their entries with zero work; touched shapes get their entries updated by an in-memory merge
of the commit's net delta — byte-identical to what a fresh sweep at the new revision would build. The LMDB sweep
survives only for cold builds (first demand, post-eviction, or explicit fallback).

## Two findings that collapse the design

1. **No reader can pin an old store snapshot.** `TripleStore.endTransaction(commit=true)` calls
   `txnManager.reset()`, which renews every resettable open read txn *precisely so iterators see the new data*
   (`TxnManager.reset()`, `Txn.reset()` → `mdb_txn_reset`/renew). The store layer always serves latest-committed;
   isolation is implemented above the store (RDF4J changeset branching). Per user decision, the cache needs to
   support nothing above SNAPSHOT — anything stricter bypasses the CSR entirely. Therefore: **no generation
   chains, no reader registration, no old-snapshot serving.** Serving the latest merged entry is always correct;
   in-flight iterators keep their immutable arrays alive via ordinary GC (same as today's behavior on drop).
2. **The index key encoding is order-preserving.** LMDB keys are SQLite4-style unsigned varints
   (`Varint.calcListLengthUnsigned`, 241-boundary), so memcmp order == component-wise unsigned numeric order in
   the index's component permutation. A merge can therefore compute, in pure Java, the exact byte order the LMDB
   sweep would emit — new pairs and new keys can be placed **byte-identically** to a fresh build.

With those two facts, full "MVCC" collapses to: **collect the commit's net delta per cached shape; at commit,
merge it into new immutable entries; keep everything else untouched.** Zero read-path overhead (entries stay flat
immutable arrays, no visibility branches, no merge-on-read), zero cost for untouched shapes, commit overhead
O(changeset ∩ cached shapes) + O(touched entry sizes) at memory bandwidth (no JNI).

## Invariants (unchanged from today, enforced by tests)

- I1. Cache hits are byte-identical in emission order to the corresponding LMDB index scan.
- I2. The probe/scan fast path is O(1), allocation-free, with no visibility logic in the edge loop.
- I3. Refusal anywhere (overflow, merge failure, budget) degrades to dropping the affected entries → LMDB
  re-scan + adaptive rebuild (today's behavior), never to wrong results.
- I4. Read-your-writes: `storeTxnStarted` bypass unchanged.
- I5. A merged entry is field-for-field byte-identical to the entry a fresh two-pass sweep would build at the
  same revision (arenas, prefix sums, key order, open-addressed table layout, flags, bounds, metadata).

## Design

### 1. Delta collection (TripleStore)

New `CsrCommitDeltaCollector` owned by `TripleStore`, active only when the cache registers itself:

- **Tee points:** inside `recordFanOutAdded(s,p,o,c?…)` / `recordFanOutRemoved(…)` — verified to fire exactly on
  genuine net mutations on every path (`storeTriple` explicit/inferred incl. inferred-demotion, `storeTriplesAligned`,
  `removeTriples(ByContext)`, recordCache-resize paths; `updateFromCache` replay does NOT re-fire them). No
  `mdb_drop` bulk-clear shortcuts exist; every mutation enumerates records. (Note: fanout recording passes
  s/p/o + explicit; the tee needs the context id too — thread it through from the call sites, which all have it.)
- **Two accumulations, all primitive, zero boxing:**
  - `touchedShapes`: open-addressed long set of `(pred << 1 | explicit)` with a last-key fast path (bulk loads
    hit the same predicate repeatedly). Always collected (cheap; bounded by distinct predicates).
  - `events`: parallel growable long arrays `(pred|explicit|op packed header, key… )` — concretely
    `s[], p[], o[], c[], flags[]` appended only when `pred` passes the **predicate filter snapshot** (below).
- **Predicate filter:** the cache maintains a volatile immutable primitive hash set of predicates that have ≥1
  live entry, swapped copy-on-write on entry publish/drop (rare). `startTransaction()` snapshots the current
  filter; only events for snapshot predicates are recorded. A cold cache ⇒ near-zero collection cost even for
  bulk loads (one contains() per mutation on a small immutable array set).
  - *Mid-transaction publish race:* an entry published during an open write txn (adaptive build on a query
    thread — builds read a snapshot read-txn, so the entry is consistent with pre-txn data) belongs to a
    predicate absent from the snapshot ⇒ its events were not collected ⇒ at commit that shape is **dropped**,
    not merged. Detected exactly: shape ∈ `touchedShapes` but pred ∉ filter snapshot.
- **Caps:** `rdf4j.lmdb.csrCache.deltaMaxEvents` (default 1<<20) on events and a same-order cap on
  `touchedShapes`; overflow sets a flag ⇒ commit falls back to `dropAllEntries` (today's behavior, I3).
- **Lifecycle:** cleared on `rollback()` and after the delta is drained post-commit. Single-threaded by
  construction (all mutations + commit run on the store's writer thread / under `sinkStoreAccessLock`).

### 2. Commit protocol (ordering is load-bearing)

- **Mark before bump.** In `TripleStore.endTransaction(commit=true)`, immediately before
  `dataRevision.incrementAndGet()`: `listener.beforeRevisionBump(touchedShapes, nextRevision)` → cache sets
  `slot.lastTouchedRevision = nextRevision` (volatile) for both directions of each touched `(pred, explicit)`.
  Validity rule (below) makes touched entries refuse from the instant readers can observe the new revision;
  untouched entries keep serving throughout. Visibility: the marks happen-before the `dataRevision` volatile
  write on the same thread; renewed read txns are handed out through synchronized TxnManager state after it.
- **Merge after commit.** In `LmdbSailStore.flush()`, where `csrCache.committedWrite()` runs today (triple +
  value stores committed and authoritative): `csrCache.applyCommittedDelta(delta)` — for each touched shape with
  a live entry: merge and publish a new entry stamped `newRevision` (healing lookups), or drop that entry on any
  failure (I3). Then the collector is cleared. Failures between bump and merge (exception paths) leave entries
  marked-stale ⇒ lazily dropped — safe.
- **Rollback:** collector cleared; no marks were set (marking happens only inside the commit success path,
  immediately adjacent to the revision bump).
- **Kill switch:** `rdf4j.lmdb.csrCache.commitMerge` (default **true**); `false` restores exactly today's
  `dropAllEntries` behavior.

### 3. Validity rule (replaces revision-equality)

`CsrSlot` gains `volatile long lastTouchedRevision` (default 0). An entry is current iff
`entry.revision >= slot.lastTouchedRevision` (every touch either re-stamps via merge or drops; untouched shapes'
data is provably unchanged since any build revision). Audit and update every staleness check:
`lookup` (line ~226), eviction sampling (~798), second-chance clock (~844), predictive-eviction priority (~563).
`dataRevision` equality remains only where it means "recompute per-revision cost caches"
(`predicateCardinalityRevision`, fail-backoff `failRevision`) — semantics unchanged. This rule also closes the
window between revision bump and merge publication (touched: `entry.revision < mark` ⇒ refuse; untouched: serve).

### 4. The merge (core, ~one new file)

Per touched shape with live entry — inputs: old `CsrEntry`, netted events for `(pred, explicit)` projected onto
the direction (`key = s|o`, `neighbor = o|s`, `context = c`); guard `entry.buildIndexName` still equals
`tripleStore.getIndexName(-1, pred, -1, -1)` (index set is static; mismatch ⇒ drop).

1. **Net the events:** sort `(key, neighbor, context, seq)`; last op per distinct quad wins with idempotent
   semantics — add = ensure-present, remove = ensure-absent (makes netting correct regardless of pre-state,
   including add→remove of a never-present quad and remove→re-add resurrection).
2. **Comparators from `buildIndexName`:** `runComparator` = index component order projected onto
   (neighbor, context); `keyRankComparator` = full component order (for first-encounter placement). All
   `Long.compareUnsigned`, mirroring the varint byte order (finding 2).
3. **Per-key run merge:** two-pointer sorted merge of the old run with the key's adds minus removes under
   `runComparator` — old runs are by construction sorted this way (a run is the sweep subsequence for one key).
4. **Key order (`keysByDense`):** first-encounter order = rank of each key's **first remaining pair** under
   `keyRankComparator`. Key-primary sweeps (`scanEmissionOrder != null` ⇔ key is the first varying component):
   order is simply key-ascending — merge sorted key lists, O(K). General case: clean keys keep their relative
   order (their first pairs are unchanged and already rank-sorted); dirty keys (changed first pair, new keys) are
   sorted by rank and merged with the clean sequence — O(K + D log D). Keys emptied by removals are dropped.
5. **Rebuild derived state exactly as `build()` does:** new arenas filled in the new key order (contexts
   materialized iff any context ≠ 0, mirroring build's lazy rule — and de-materialized when none remain);
   `runStart` prefix sums; open-addressed table reproduced by re-running the `KeyCounts` insertion algorithm
   (same initial 1<<10 capacity, same 3/4 growth trigger) over the new `keysByDense` ⇒ byte-identical layout;
   `neighborMin/Max`, `allNeighborsOrderedIntegers && nPairs > 0` recomputed in the fill pass;
   `runsNeighborContextOrdered`/`runEmissionOrder`/`scanEmissionOrder` re-derived from the index name (data-
   independent). Entry byte cap enforced like build (`nPairs*16 > maxEntryBytes` ⇒ drop); budget: reserve new
   bytes, publish (slot-monitor swap + clock relink), release old bytes.
6. **Empty result** (all pairs removed): publish the same empty entry a fresh sweep of an empty predicate would
   produce — the shape stays cached and "never rebuild" holds.

Cost: O(E log E + K + P) per touched entry, pure memory bandwidth, transient 2× entry memory. Counters:
`MERGES`, `MERGE_FAILURES`, `DELTA_OVERFLOWS` beside the existing `BUILDS`/`STALE_DROPS`
(a healthy steady state shows MERGES rising while BUILDS stays flat).

### 5. What deliberately does not change

Read path (probe/scan/count/exists iterators, kernels, root scans, ordered-scan gating), entry layout, eviction
policy and budget, `storeTxnStarted` bypass, eager/auto-warm scheduling (`buildEagerly` already skips live
entries — merged entries now survive, so eager rebuild work disappears on its own), fanout stats, estimators.

## Implementation checklist (single pass)

1. `TripleStore`: `CsrCommitDeltaCollector` (primitive arrays, filter snapshot, caps); context threading into
   the two tee methods; hooks in `startTransaction` (snapshot), `endTransaction` (mark-before-bump),
   `rollback` (clear); `drainCsrDelta()` accessor; listener registration API.
2. `LmdbCsrAdjacencyCache`: `lastTouchedRevision` + validity-rule audit (4 sites); predicate-filter maintenance
   on publish/drop; `beforeRevisionBump`; `applyCommittedDelta` (netting + per-shape dispatch, per-shape
   try/catch → drop on failure); the merge itself (new package-private class `LmdbCsrEntryMerger`); counters;
   kill switch; `committedWrite()` retained for the fallback paths.
3. `LmdbSailStore`: replace the unconditional `csrCache.committedWrite()` with drain + `applyCommittedDelta`
   (fallback to `committedWrite()` when the switch is off or the delta overflowed).
4. Tests (`LmdbCsrCommitMergeTest` + extensions to existing cache tests):
   - **Differential byte-identity (I5):** randomized stores; build entries; apply randomized mutation commits
     (adds/removes/re-adds, new keys, emptied keys, named graphs both materializing and de-materializing
     contexts, explicit+inferred incl. inferred-demotion, empty-entry edge); assert merged entry equals a
     freshly-built entry field-by-field on every array and flag; assert query results match cache-off.
   - **Survival:** commit touching pred B ⇒ pred A's entry is the *same object* and still serves; commit
     touching pred A ⇒ entry replaced via merge with `BUILDS` unchanged and `MERGES` incremented (no sweep).
   - **Fallbacks:** overflow ⇒ drop-all; mid-txn-published entry ⇒ dropped not corrupted; kill switch off ⇒
     today's behavior; merge failure injection ⇒ drop + correct queries.
   - Both directions over both default index regimes (key-primary and non-key-primary), exercised via the
     store's actual index config.
5. Format, headers, module verify (`mvnf core/sail/lmdb`), plan-doc updates.

## Key risks

- **Byte-identity of key order in non-key-primary sweeps** rests on "first-encounter rank = rank of first
  remaining pair" — enforced by the differential test across randomized removals of run heads (the exact case
  where a key's rank changes).
- **Completeness of the tee**: any future mutation path that bypasses `recordFanOut*` silently corrupts merges.
  Mitigation: the tee lives *inside* those methods (single choke point) + the differential test suite runs the
  full mutation API surface.
- **Commit-latency inflation for huge cached entries under tiny commits** (O(entry) copy per touching commit):
  accepted — strictly cheaper than today's drop+sweep in aggregate; if it ever matters, the v2 overlay design
  layers on top without rework (the delta collector and validity rule are exactly what it needs).

## Progress

- [x] (2026-07-27) v1/v2 design iterations; superseded.
- [x] (2026-07-27) Findings 1+2 verified in code (txn reset semantics; varint order preservation); v3 design.
- [x] (2026-07-27) Implementation complete: `LmdbCsrCommitDelta` collector + `TripleStore` tee/hooks
  (context threaded through `recordFanOut*`, filter snapshot at `startTransaction`, mark-before-bump in
  `endTransaction`, reset on abort); cache validity rule (`CsrSlot.lastTouchedRevision`, 4 staleness sites),
  `commitListener`/`markTouchedShapes`/`applyCommittedDelta`/`replaceEntry`, `CommitMerger` (byte-identical
  in-memory merge incl. first-encounter re-ranking and `KeyCounts` table replication); `LmdbSailStore` wiring.
- [x] (2026-07-27) `LmdbCsrCommitMergeTest` 12/12 green — randomized differential byte-identity vs fresh builds on
  `spoc,posc` AND `spoc,pcos` (context-before-neighbor regime), untouched-entry same-instance survival with zero
  sweep rebuilds, empty-entry serving (strictly better than fresh builds, which refuse empty predicates),
  context materialize/de-materialize parity, net-noop and re-add netting, inferred-demotion dual-shape merge,
  rollback no-op, overflow fallback, kill switch, mid-transaction-publish drop. Evidence:
  `initial-evidence-csr-commit-merge.txt`.
- [x] (2026-07-27) Full lmdb module verify: the sweep surfaced exactly five CSR-test failures, all intended
  behavior changes, all fixed: `LmdbCsrAdjacencyCacheTest`/`LmdbCsrPredictiveEvictionTest` harnesses now register
  the commit listener (production wiring; without it commits never invalidate under the touched-shape rule) and
  the staleness scenario touches the entry's own predicate (unrelated-predicate commits no longer stale it);
  `LmdbCsrCacheQueryTest.committedWriteImmediatelyCreditsAllCachedBytes` →
  `committedWriteMergesEntriesInPlaceWithoutRebuilds` (bytes stay charged, MERGES>0, fresh results with BUILDS
  unchanged); `LmdbCsrAutoWarmTest`/`LmdbCsrEagerPredicateTest` post-commit rebuild waits replaced with
  merge-in-place assertions. Re-run of all six CSR classes: 49/49 green. The five remaining module failures
  (`LmdbNativeKernelDeclineCensusTest`, `LmdbNativeStrategyPriorityTest`, `LmdbNativeFeatureFlagForkTest` ×3) are
  documented pre-existing branch failures (same list in `initial-evidence-cost-dispatch-p1.txt` and
  `initial-evidence-m1-learned-filter-selectivity.txt` from before this change).
- [x] (2026-07-27) Static-review fixes applied (see "Review findings & fixes"); rebuild + re-run of all six CSR
  classes on the fixed build: 49/49 green (`final-evidence-csr-commit-merge.txt`); module formatted, headers
  verified.

## Surprises & Discoveries

- `TxnManager.reset()` renews open read txns on commit so iterators see new data — the store layer never serves
  old snapshots, which eliminates the entire generation/reclamation surface from the design.
- The fanout-stats tee points (`recordFanOutAdded/Removed`) already fire exactly on net mutations on every write
  path — the delta collector inherits their correctness for free (they lack the context id, which must be
  threaded through).

## Decision Log

- Decision: Commit-time in-memory merge (v3) over merge-on-read overlays (v1) and grouped generations (v2).
  Rationale: with latest-committed-only serving (user decision: nothing above SNAPSHOT uses the CSR) and
  order-preserving key encoding, commit-merge achieves "never wipe, never sweep" with zero read-path changes,
  zero read overhead, and a fraction of the code; overlay/grouping remain compatible future layers if per-commit
  entry-copy cost ever surfaces.
  Date/Author: 2026-07-27 / Claude (Fable) with hmottestad.
- Decision: Tee inside `recordFanOutAdded/Removed` rather than at each call site; idempotent op semantics
  (ensure-present/ensure-absent) with last-op-wins netting so pre-state is irrelevant.
  Date/Author: 2026-07-27 / Claude.
- Decision: Default ON (`rdf4j.lmdb.csrCache.commitMerge=true`) with a kill switch and per-shape drop fallbacks;
  overflow and any merge anomaly degrade to today's exact behavior.
  Date/Author: 2026-07-27 / Claude.

## Review findings & fixes (2026-07-27 static review)

- **High — quadratic commit-merge processing (fixed):** `applyCommittedDelta` rescanned the whole sorted event
  array per touched shape (`shapeEventRange`), Θ(N²) for N touched cached shapes while lookups are bypassed.
  Replaced with one grouping pass building a shape→[from,to) map; the per-shape loop never rescans.
- **Medium — size-neutral merges could evict other touched entries (fixed):** `replaceEntry` reserved the full
  replacement size while the old entry stayed charged, so near the budget a merge could evict other
  already-marked touched entries before their own merge. Now the old entry's charge rolls into the replacement
  and only a positive size delta is reserved; shrinking/neutral merges can never trigger evictions.
- **Medium — regret-scorer cache mixed incompatible experiments (fixed):** cache now carries a full measurement
  fingerprint (JMH settings, arm flag table, JVM, HEAD revision + sha256 of the module's uncommitted diff);
  mismatching caches are moved aside as `.stale`, never silently reused.
- **Medium — regret-scorer accepted failed runs (fixed):** non-zero exit codes are rejected before parsing,
  stderr tails are surfaced, `build_pending` clears only after a confirmed successful run, and only successful
  measurements are cached.

## Surprises & Discoveries (implementation)

- Fresh adaptive builds refuse empty predicates (`predicateCardinality <= 0` gate), so a merged-to-empty entry has
  no fresh-build counterpart — the merge keeps serving emptiness from memory, which is strictly better; the
  differential test asserts its semantics directly instead of via the oracle.
- The eager/auto-warm "rebuild after commit" machinery became dead weight for cached shapes in one stroke: merged
  entries survive the commit, so the post-commit eager rebuild sweep simply never fires (its tests now assert
  merge-in-place instead).
- Registration cannot live in the cache constructor: a second cache instance over the same store (the test oracle
  pattern) would steal the listener and corrupt the first cache's invalidation. Store-level wiring stays in
  `LmdbSailStore`; unit harnesses register explicitly.

## Outcomes & Retrospective

- Implemented in one pass as designed: commits now update CSR entries in place (byte-identical to fresh sweeps),
  untouched shapes keep their entries by reference, and the LMDB sweep runs only for cold builds. Fallbacks
  (overflow, kill switch `rdf4j.lmdb.csrCache.commitMerge=false`, merge anomaly, uncollected mid-txn publish)
  reproduce the old drop behavior exactly.
- Verification: `LmdbCsrCommitMergeTest` (12 tests — randomized differential byte-identity on two index regimes
  incl. context-before-neighbor, survival-by-reference, inferred demotion, context re/de-materialization, netting,
  rollback, fallbacks) + updated existing suites; six CSR classes 49/49; module sweep clean apart from the five
  documented pre-existing branch failures.
