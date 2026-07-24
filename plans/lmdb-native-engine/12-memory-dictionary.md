# 12 — Memory management and dictionary access

Goal: one byte-accounted memory authority governs every query-side structure, with spill or graceful
fallback on refusal — never OOM; and the ID↔value dictionary boundary stops being a per-value,
per-transaction cost via batching, byte-aware caches, and the already-built persistent hash file.


## Current state

Ten independent per-operator caps — six property-tunable (`nativeHashJoin.maxBuildRows`,
`mergeJoin.maxRunRows`, `nativeSort.maxBytes` 64 MB, `nativeSpecialization.maxGeneratedBytes`,
`csrCache.maxBytes`/`maxEntryBytes`), four hardcoded (`MAX_MATERIALIZED_ROWS`,
`MAX_MEMO_ROWS = 1<<14`, `MAX_REPLAY_MATCHES = 1<<14`, `MEMO_MAX_ENTRIES`/`MEMO_MAX_STORED_VALUES`)
— none sharing accounting across operators or queries; caps are ROW counts, so byte footprint varies
~an order of magnitude with row width (`MAX_NATIVE_SLOTS = 60`). Uncapped: `LongAggStateMap`
(`LmdbNativeRowState.java:131-142`) and `NativeGroupTable` (plan 10 §1). Only `NativeSpillSort` spills;
every other structure abandons-and-falls-back, leaving memory idle while a sibling structure hits its
fixed cap. The best in-repo template is the CSR cache: process-global `GLOBAL_USED_BYTES`,
CAS `reserveBytes` with `evictOne` (`LmdbCsrAdjacencyCache.java:50, :322-337`) — versus
`FactorizedTail.MemoBudget` which only refuses (`LmdbNativeFactorizedTail.java:44-79`). The
ordered-factorized sort aborts at the shared ~8 MiB memo ceiling (`MEMO_MAX_STORED_VALUES = 1L<<20`
longs, `:39`) instead of spilling, despite `NativeSpillSort` sitting beside it.

Dictionary: every ID→value read is its own LMDB read transaction; materializing one IRI costs two
independent reads (namespace + local); the radix-sorted BATCH probe exists but only on the write path;
value→ID resolution of entry bindings is per-value. Caches are entry-count-sized with fixed defaults —
`VALUE_CACHE_SIZE = 256*1024` entries (unbounded per-entry bytes; `cacheValueIn`,
`ValueStore.java:1135-1157`, has only a null check), `VALUE_ID_CACHE_SIZE = 128`,
`NAMESPACE_CACHE_SIZE = 64`, `NAMESPACE_ID_CACHE_SIZE = 32` (`config/LmdbStoreConfig.java:43-73`).
The persistent value-hash file is fully implemented — mmap via `java.lang.foreign` in 256 MB segments,
CRC32C sidecar, revision guard, freed-ID clearing (`ValueStoreHashFile.java:37-204`;
`ValueStore.java:595-601, :996-998, :3196-3208`) — and ships disabled
(`valueHashCacheEnabled = false`, `LmdbStoreConfig.java:103`), introduced `= false` from birth
(`5a2b6386ad`). The in-memory hash cache works regardless of the flag (`:981-984, :1016`); the residual
gap is first-touch-per-session, cross-restart persistence, and single-slot-bucket eviction
(`:1064-1065`). Native-engine impact is narrow (the engine hashes raw IDs); it bites on paths escaping
to materialized Values.


## Work item 1 — The memory authority

1. `LmdbQueryMemoryManager` (new, `org.eclipse.rdf4j.sail.lmdb`): process-global byte ledger with
   per-query sub-ledgers. API: `Reservation reserve(bytes, Reclaimable)` /
   `Reservation tryGrow(...)` / `release(...)`; `Reclaimable` exposes `spill()` (preferred) or
   `shed()` (abandon-and-fallback) so the manager can reclaim under pressure in LRU order — the CSR
   cache's reserve/evict generalized, with the CSR cache itself becoming the first registered
   reclaimable pool (its budget folds into the global one; its entry-cap policy stays local, plan 07
   §4).
2. Budgets: global default `max(1 GB, heap/2)`; per-query default `global/4` (both properties). Row
   caps convert to byte reservations (row count × row width × 8 at reserve time); the four hardcoded
   caps become derived limits under the same ledger (their constants remain as sanity ceilings).
3. Wire, in order of exposure: `LongAggStateMap` and `NativeGroupTable` (uncapped today — plan 10 §1's
   spill is the reclaim path); the ordered-factorized sort (replace the 8 MiB abort with a
   `NativeSpillSort`-backed tier — the budget-aware constructor protocol already exists,
   `LmdbNativeSort.java:364-371, :396-397`); hash-join builds (spill = partition build to disk runs,
   probe per partition — classic Grace fallback; v1 may keep abandon-and-fallback but through the
   ledger so the SIZING is global even before spill lands); memos and replay buffers (refusal
   degrades exactly as today, but budget-priced).
4. Concurrency contract: reservations are per-query-cursor lifetime; cancellation releases via the
   existing close paths; the ledger never blocks — refusal is immediate and the caller degrades
   (matching today's attempt-failure protocol, e.g. `BUDGET_REFUSED` → `return null`).

Tests: ledger unit tests (concurrent reserve/release/reclaim); N-concurrent-heavy-queries harness
asserting global ceiling held with all queries completing (degraded, not failed); leak test —
after every corpus query closes, ledger returns to zero.
Acceptance: plan 00's done-criterion 4 — every unbounded structure accounted, refusal degrades,
no OOM on the adversarial concurrency workload; large single queries USE available memory (the 1<<20
hash-build cap and 64 MB sort budget stop binding on big-heap machines).


## Work item 2 — Byte-aware dictionary caches

Per-entry size guard in `cacheValueIn` (lexical length test; oversized values bypass the cache);
caches denominated in bytes not entries with adaptive defaults derived from heap
(value cache `heap/64`, others proportionally); namespace caches sized to survive real vocabularies
(64 namespaces is below common dataset counts — default 1024 entries, byte-capped). Keep eviction
approximate-FIFO (measured adequate) but instrument hit rates through `LmdbNativeAttemptMetrics` so
plan 13 can validate sizing against the benchmark corpus.

Acceptance: no more unbounded heap exposure from mega-literals in the value cache (test with 100 MB
literals); dictionary-heavy result materialization shows improved hit rates on defaults.


## Work item 3 — Batched dictionary resolution at materialization boundaries

The engine works in raw IDs until a consumer needs `Value`s (projection to BindingSets, sort with
non-inline keys, group emission, RDF-star term construction). At those boundaries:

1. Collect the distinct unresolved IDs of the outgoing page (batch/page granularity already exists at
   every boundary: `NativeBatch` pages, exchange output pages, sort output runs, group emission).
2. Sort by store order and resolve in ONE read transaction with a single cursor sweep — the write path
   already implements exactly this radix-sorted batch probe pattern; port it to the read side as
   `ValueStore.resolveBatch(long[] ids, Value[] out)`.
3. Fill the value cache from the sweep, then materialize the page.
   IRI namespace+local pairs resolve within the same transaction (removing the two-txn IRI cost).

Same shape for value→ID on entry: `resolveIds(Value[] in, long[] out)` for VALUES clauses and external
binding sets at query start.

Tests: equivalence + txn-count assertions (one read txn per page, not per value); concurrency with
writers (snapshot discipline unchanged — the sweep runs in one read snapshot like any read).
Acceptance: SELECT with 100k materialized rows drops per-value txn overhead (wall-clock and txn
counters); federation/client-style consumers see the same.


## Work item 4 — Enable the persistent value-hash file

Flip `valueHashCacheEnabled` default to true after: verifying the CRC/integrity path under kill-restart
tests (the machinery exists: `hasValidIntegrity`, `discardExistingContents`); benchmarking the
first-touch win (hash-of-ID without dictionary lookup benefits GROUP BY/DISTINCT arity>4 escapes,
Model/Set construction, client dedup — the boxed paths); and confirming write-amplification is
acceptable on the write benchmark (hashes are 4 bytes/value, written once per new value). If the write
cost shows anywhere it matters, keep default-off and record the measured justification in plan 13's
rollout table — the criterion is measurement, not the current unexplained default. Consider widening
the in-memory cache's single-slot buckets to 2-way while touching it (`:1064-1065` unconditional
overwrite is the cheap-eviction pathology).

Acceptance: restart-heavy workload (open store, hash-heavy query, close, repeat) shows first-touch
hashing amortized across sessions; kill-restart fuzzing never serves a stale hash (revision guard
holds).
