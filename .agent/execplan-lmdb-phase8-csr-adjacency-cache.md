# Per-predicate CSR adjacency cache for the LMDB native engine

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. Maintained in accordance with `.agent/PLANS.md`.

## Purpose / Big Picture

Join probes in the LMDB native engine — "give me all objects for subject S under predicate P" — each cost an LMDB B+tree reposition (`MDB_SET_RANGE`) plus one JNI cursor step per matched triple. Star- and chain-shaped SPARQL queries issue millions of such probes. Kuzu answers the equivalent lookup with an O(1) array subscript into an in-memory CSR (compressed sparse row) adjacency structure. This phase gives the LMDB engine the same shape: an in-memory, per-store cache that, for hot (predicate, direction) pairs, holds the complete adjacency as three flat arrays — an open-addressed key table, a prefix-sum run index, and a neighbor/context arena — built adaptively when probe traffic proves it worthwhile.

After this change, with `rdf4j.lmdb.csrCache.enabled=true`, a query that probes `?s <p> ?o` (either side bound) more than ~1024 times triggers a one-sweep build; subsequent probes are answered without any LMDB/JNI call. `explain(Telemetry)` shows `+csr` on engaged stages, and cache behavior is observable through counters (`BUILDS`, `HITS`, `STALE_DROPS`, `EVICTIONS`, `REFUSALS`). Writes invalidate automatically via the store's `dataRevision`. Default OFF until benchmarks validate.

## Progress

- [x] (2026-07-15 21:20Z) Design finalized (probe-funnel + commit-protocol facts verified in-tree).
- [x] (2026-07-16 00:15Z) M1 DONE: cache + builder (two-pass sweep, first-encounter dense ids, revision-equality publish) + `LmdbCsrRunIterator` + probe decoration via shared `CsrProbeSupport` (per-probe reusable iterator, 256-open miss flush + flush-on-close). Tests: `LmdbCsrAdjacencyCacheTest` 8 (content+ORDER vs getTriples ground truth both directions, bound context, context-array elision, invalidation, read-your-writes bypass, per-entry cap refusal, concurrency with writer loop); `LmdbCsrCacheQueryTest` 3 end-to-end (adaptive build from probe traffic, hits on later queries, write invalidation, uncommitted visibility); fuzz green with cache forced hot.
- [x] (2026-07-16 00:20Z) M2 DONE: `adjacencyCacheBacked()` marker; ProbeStage sets `probeCacheBacked` (+`hashBuildRefused`, memo lookups and SEEN_ONCE markers skipped); TailBranch returns scan results unmemoized for cache-backed probes; `LmdbNativeAttemptMetrics.recordChunkCsrBackedProbe` + `CSR_BACKED_PROBES` counter. Explain `+csr` string deferred (engagement is discovered mid-execution; needs the deferred-strategy plumbing — successor plan).
- [x] (2026-07-16 00:25Z) M3 DONE: `tryCount`/`tryHas` on the cache (linear run scans), served from both dataset and snapshot sources (existence checks and COUNT paths). Test `countAndHasServeFromExistingEntries` (9th test in the unit class).
- [x] (2026-07-16 01:10Z) M4 stats half DONE: build sweeps record `neighborMinId`/`neighborMaxId`/`allNeighborsOrderedIntegers` per entry (pushdown soundness gate for the Phase 10 successor). Hash-build publication half deferred to the successor plan.
- [x] (2026-07-16 01:40Z) Review fix: untracked (pinned-snapshot) datasets — the estimator refresh reader — are now `csrEligible=false`: the cache tracks the latest committed revision and must not serve readers pinned to older snapshots. Parallel snapshot sources stay eligible (their lease blocks commits while open).
- [ ] Benchmarks + default-flip decision (session in progress: theme MEDICAL q8 baseline vs csr).

## Surprises & Discoveries

- (from design verification) The commit protocol makes revision validation race-free without txn-id checks: `mdb_txn_commit` → `txnManager.reset()` → `dataRevision.incrementAndGet()` all run under the txn write lock (`TripleStore.java:3480-3512`), so revision equality before/after a build sweep proves single-snapshot.
- (from design verification) posc traversal order yields spoc-order runs for BY_SUBJECT for free (stable two-pass placement), so cache emission order is byte-identical to index order in both directions — parity tests cannot distinguish cache hits from LMDB scans.

## Decision Log

- Decision: Per-store cache instance (field on `LmdbSailStore`), not a process-wide registry; a static `GLOBAL_USED_BYTES` shares the byte cap across stores.
  Rationale: avoids identityHashCode collision/leak issues of the cardinality-cache precedent; dies with the store.
  Date/Author: 2026-07-15 / approved plan.
- Decision: Adaptive inline build on the query thread, triggered by probe-open counting (local counter flushed every 256 opens; trigger `opens >= minProbes && opens * 8 >= sweeps * predCard && estBytes <= caps`), CAS loser keeps LMDB-probing (never blocks).
  Rationale: same validated philosophy as the per-stage `shouldBuildHashTable`; the build is amortized against probe work already paid, so it can never be a correlated-entry setup-cost bomb.
  Date/Author: 2026-07-15 / user choice "adaptive".
- Decision: Budget = `max(256 MiB, 25% of Runtime.maxMemory())` default, per-entry cap 64 MiB, LRU after purging stale revisions; refusal degrades to re-scanning.
  Rationale: user choice "adaptive + bigger budget".
  Date/Author: 2026-07-15 / user choice.
- Decision: Entries are per `(predicate id, direction, explicit-flag)`; contexts stored as a parallel array elided when all-zero; composite explicit+inferred needs no special casing because the two flags mirror the two LMDB dbis and the composite probe fans out per source.
  Rationale: mirrors storage exactly.
  Date/Author: 2026-07-15 / approved plan.

## Outcomes & Retrospective

(To be filled.)

## Context and Orientation

Module `core/sail/lmdb`; main sources under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/` (below `…/lmdb/`). Key facts a novice needs:

- **Probes**: `NativeLmdbQuerySource.NativeProbe { RecordIterator open(long s, long p, long o, long c); void close(); }` (`…/lmdb/evaluation/NativeLmdbQuerySource.java:105-111`). Every join operator in the engine (chunk-pipeline ProbeStage, factorized tail branches, left-join probes, path plans) obtains one via `row.source.newProbe()` and calls `open(...)` once per probe key; all store access funnels through `PatternPlan.openIterator` → `probe.open(...)` (`…/lmdb/evaluation/LmdbNativePatternPlan.java`). The two concrete implementations both live in `…/lmdb/LmdbSailStore.java`: `RetainedNativeProbe` (sequential datasets, ~:3177) and the probe returned by `ParallelSnapshotSource.newProbe()` (~:2944). Decorating those two covers the entire engine with zero call-site changes.
- **Quad ids**: subject/predicate/object/context are 64-bit dictionary ids; `-1` = unbound. Probes with `pred > 0 && ((subj > 0) ^ (obj > 0))` are the CSR-servable shape.
- **Invalidation key**: `TripleStore.dataRevision` (AtomicLong, getter `getDataRevision()` :337) increments on every committed write under the same write lock that resets read transactions (`TripleStore.java:3480-3512`).
- **Read-your-writes**: a connection with uncommitted local writes must not be served from (or build into) the cache; the existing signal is `storeTxnStarted` (see `openParallelSources` refusal, `LmdbSailStore.java:3276-3278`).
- **Reusable structure**: the open-addressing core in `…/lmdb/evaluation/LmdbNativeKeyedMatches.java:78-124` (long keys, `int[] slotPlus1`, mix hashing) — copy the ~40-line core into the cache class (KeyedMatches is evaluation-package-private and query-lifetime-scoped; the cache is store-lifetime).
- **CSR entry layout**: immutable after publish — `long[] hashKeys` + `int[] hashSlotPlus1` (open-addressed, ≤0.75 load), `int[] runStart` (prefix sums, length nKeys+1), `long[] neighbors` (length nPairs), `long[] contexts` (null iff every context id is 0), `long revision`, `long bytes`, `volatile long lastAccessNanos`. Directions: BY_SUBJECT (key=s, neighbor=o, serves `open(s,p,-1,c)`), BY_OBJECT (key=o, neighbor=s, serves `open(-1,p,o,c)`).
- **Build sweeps**: `tripleStore.getTriples(txn, -1, pred, -1, -1, explicit)` selects posc (one contiguous range). BY_OBJECT = one pass (keys arrive grouped ascending). BY_SUBJECT = two passes (count per distinct s → prefix-sum → place at cursor[slot]++; posc order yields per-subject runs ascending in (o,c) = exactly spoc emission order).
- **Iterator contract**: probe consumers speak `RecordIterator.next()/fill(long[] buffer, int maxRows)` returning positional quads (`TripleIndex.SUBJ_IDX..CONTEXT_IDX`). The cache's `LmdbCsrRunIterator` refills a reusable `long[4]`/buffer from the arrays — no JNI, no lock, no allocation per row.

## Plan of Work

### Milestone 1 — cache, builder, run iterator, probe decoration, tests

1. New file `…/lmdb/LmdbCsrAdjacencyCache.java` (package-private, package `org.eclipse.rdf4j.sail.lmdb`): the per-store cache with `ConcurrentHashMap<Long, CsrSlot>` keyed `predId << 2 | direction << 1 | (explicit?1:0)`; nested `CsrSlot` (probeOpens accumulator, `AtomicBoolean building`, failedBuilds/failRevision, `volatile CsrEntry entry`) and immutable `CsrEntry` (layout above); a static `AtomicLong GLOBAL_USED_BYTES`; static counters `BUILDS, BUILDS_DISCARDED, HITS, STALE_DROPS, EVICTIONS, REFUSALS`; config statics `enabled()` (`rdf4j.lmdb.csrCache.enabled`, default false), `minProbes()` (1024), `maxBytes()` (default `max(256 MiB, maxMemory/4)`), `maxEntryBytes()` (64 MiB).
   - `lookup(pred, direction, explicit, dataRevision)` → entry or null (drops stale entries via CAS + byte release + STALE_DROPS).
   - `recordProbes(key, count, TripleStore, Txn)` → flush path that evaluates the build trigger and, on winning the `building` CAS, runs the sweep inline; publish iff `dataRevision` unchanged across the sweep (`R_before == R_after`), else discard (BUILDS_DISCARDED) and back off (retry only after revision change, give up after 3 failures per revision).
   - Eviction on over-budget insert: purge stale-revision entries first, then LRU by `lastAccessNanos`; if still over, refuse (REFUSALS).
2. New file `…/lmdb/LmdbCsrRunIterator.java` (package-private) implementing `RecordIterator` over one entry run: fields (entry, pos, end, sTemplate/pTemplate/oTemplate, boundCtx, direction); `next()`/`fill()` write positional quads; context filtering per run when the probe binds a context (contexts==null ⇒ all zero).
3. `…/lmdb/LmdbSailStore.java`: cache field (constructed when enabled); decorate the two probe implementations — on `open(s,p,o,c)` with the CSR-servable shape, resolve the slot once per probe (predicate/direction constant per operator), cache `(entry, revisionAtCheck)` in probe fields, serve hits from a per-probe reusable `LmdbCsrRunIterator`; misses count locally and flush every 256 opens; bypass reads AND builds while the owning dataset has uncommitted writes; `ParallelSnapshotSource` validates once at probe construction (its snapshot is lease-pinned).
4. Tests (same package): `LmdbCsrAdjacencyCacheTest` — build-vs-`getTriples` ground truth (content AND order; multi-context; null-context elision; both directions), byte accounting, eviction, per-entry cap; invalidation test (build → commit write → stale drop → correct fresh results); read-your-writes test (uncommitted add visible, bypass counted); concurrency test (writer commit loop + N reader threads; every result equals some committed snapshot; `building` CAS never blocks readers). Fuzz: run `LmdbNativeDifferentialFuzzTest` with the flag on and `minProbes=1`.

### Milestone 2 — engine awareness

`NativeProbe` gains `default boolean adjacencyCacheBacked() { return false; }`; decorated probes return true after a hit-capable slot resolution. `ProbeStage` (chunk pipeline) then sets `hashBuildRefused = true` and forces memo-skip (UNCACHEABLE behavior); same skip in `LmdbNativeFactorizedTailBranch.result`'s memo/scan-once flip — with O(1) probes those layers are pure overhead and MemoBudget waste. Explain: stages append `+csr` to their engagement strings; `LmdbNativeAttemptMetrics` gains `recordCsrProbeHit/recordCsrBuild` following the child/commit/merge/publish pattern.

### Milestone 3 — existence/count probes

Serve `PatternPlan.openAsExistenceCheck`'s `count`/`has` calls (`LmdbSailStore.java:~3229-3252`) from the cache via binary search within the key's run (`neighbors[start..end)` is sorted ascending).

### Milestone 4 (optional) — already-paid sweep publication + benchmarks

Publish CSR entries from `ProbeStage.buildHashTable`'s sweep when the pattern is wildcard-context `?s p ?o` (the sweep cost is already paid; only the layout differs). Benchmarks: `ThemeQueryBenchmark` (MEDICAL_RECORDS) and `FactorizedTailStarBenchmark` flag on/off; success = large star/chain win, zero q8-style correlated regression (the cold path is unchanged until `minProbes` opens). Record numbers in `profiles/lmdb-opt/jmh/` and the flip decision here.

## Concrete Steps

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCsrAdjacencyCacheTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbNativeDifferentialFuzzTest -- -Drdf4j.lmdb.csrCache.enabled=true -Drdf4j.lmdb.csrCache.minProbes=1
    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb
    (cd scripts && ./checkCopyrightPresent.sh) && mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

## Validation and Acceptance

M1: unit + invalidation + concurrency tests green; fuzz green with the cache forced hot. M2: counter-pinned tests show memo/hash-build skips when the cache serves; explain shows `+csr`. M3: existence probes parity. Phase exit: full module verify green; benchmark deltas recorded; flag default decided.

## Idempotence and Recovery

All additive and flag-gated (default off). The only pre-M2 edits to existing files are the two probe decorations in `LmdbSailStore`; reverting them plus the two new files restores baseline.

## Artifacts and Notes

(Evidence appended per milestone.)

## Interfaces and Dependencies

    // org.eclipse.rdf4j.sail.lmdb (package-private)
    final class LmdbCsrAdjacencyCache {
        CsrEntry lookup(long pred, int direction, boolean explicit, long dataRevision);
        void recordProbes(long slotKey, int count, ...);   // flush + adaptive build trigger
        static final class CsrEntry { long[] hashKeys; int[] hashSlotPlus1; int[] runStart; long[] neighbors; long[] contexts; long revision; long bytes; }
    }
    final class LmdbCsrRunIterator implements RecordIterator { ... }
    // evaluation.NativeLmdbQuerySource.NativeProbe
    default boolean adjacencyCacheBacked() { return false; }

Properties: `rdf4j.lmdb.csrCache.enabled` (false), `.minProbes` (1024), `.maxBytes` (max(268435456, maxMemory/4)), `.maxEntryBytes` (67108864). No new external dependencies.
