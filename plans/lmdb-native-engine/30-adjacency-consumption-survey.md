# Adjacency consumption survey: what queries use the in-memory structures today, and what it takes to reach Kuzu parity

Status: survey complete (2026-08-03). This is a Routine C investigation artifact plus two permanent harnesses; no
production code was changed. It is the design input for follow-up ExecPlans.

> **SUPERSEDED (2026-08-07):** the census table below describes the tree as of 2026-08-03 and is stale — the
> three-tier parity ExecPlan (`.agent/three-tier-parity-execplan.md`, M1–M10B) closed most of the gap list
> (kernel hash join, kernel WCOJ, context columns, computed BINDs, ORDER/LIMIT sinks, EXISTS witnesses, mixed
> binding, parallel kernels). Consult that plan's Progress section for the current state.

## Motivation

Kuzu answers full predicate scans, joins, and point lookups from its in-memory adjacency lists. This branch's LMDB
store builds equivalent structures (plan 27 direct adjacency: four planes per predicate — {outgoing, incoming} ×
{explicit, inferred} — with sorted neighbor runs, a sorted key domain per plane, and delta overlays), but query
consumption is much narrower than what the structures can answer. This survey measures exactly which query shapes
engage the structures and enumerates the gaps and the new capabilities the structures unlock.

## Harnesses added (both permanent, census-style: they assert row counts, not counter values)

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyUsageCensusTest.java` — 19 Kuzu-parity
  query shapes over a deterministic 2000-person graph; prints per-query deltas of adjacency lookup hits, closed-enum
  fallback reasons, and WCOJ frontier counters. Run: `python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyUsageCensusTest`.
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/AdjacencyQueryShapeBenchmark.java` (+ smoke
  test) — the same shapes as JMH acceptance workloads over the FOAF clique store (5000 people), so each future
  adjacency-served path has a paired-run baseline comparable with the WCOJ work.

## Census result (2026-08-03, evidence: `initial-evidence-adjacency-census.txt`)

| shape | engages adjacency? | fallback reason |
|---|---|---|
| point lookup, subject bound (`<s> :p ?o`) | YES (1 hit) | — |
| point lookup, object bound (`?s :p <o>`) | YES (1 hit) | — |
| doubly-bound probe (`<s> :p <o>`) | NO | `DOUBLY_BOUND` |
| full predicate scan (`?s :p ?o`) | NO | `ROOT_SCAN` |
| `SELECT DISTINCT ?s { ?s :p ?o }` | NO | `ROOT_SCAN` |
| `COUNT(*)` over one predicate | NO | `ROOT_SCAN` |
| degree per subject (`GROUP BY ?s COUNT`) | NO | `ROOT_SCAN` |
| node edge dump (`<s> ?p ?o`) | NO by default; see the superseded note below | `PREDICATE_ENUMERATION_INCOMPLETE` |
| two-hop from seed | YES (probes) | — |
| star join | probes YES, leading scan NO | `ROOT_SCAN` |
| chain join, open-ended | probes YES, leading scan NO | `ROOT_SCAN` |
| object-object join | probes YES, leading scan NO | `ROOT_SCAN` |
| triangle (WCOJ) | YES, fully (0 scanned frontiers) | — |
| VALUES-batched lookups | YES (probes) | — |
| property path `+` reachability | ~NO (1 hit for 2000 expansions) | — (path engine bypasses) |
| EXISTS semijoin | probes YES, leading scan NO | `ROOT_SCAN` |
| `ORDER BY ?o` on bound-subject run | YES (order-compatible) | — |
| `SELECT DISTINCT ?p { ?s ?p ?o }` | NO — never consults | (no fallback recorded) |
| predicate histogram (`GROUP BY ?p`) | NO — never consults | (no fallback recorded) |

## Gap analysis: what needs to be done

Ordered by expected leverage. Each item names the blocking code seam.

1. **Adjacency-served full predicate scans (kills `ROOT_SCAN`).** `LmdbDirectAdjacencyStore.open` branch 3
   (`:1626-1628`) structurally refuses both-endpoints-free patterns even though the plane holds the complete sorted
   key domain (`ImmutablePagedQuadCsfIndex.KeyDomain`) and every run. An iterator that walks `keyAt(0..keyCount)` × run neighbors
   yields the whole predicate in (s,o) order without touching LMDB — this is Kuzu's predicate-table scan. It also
   fixes the leading-scan half of every open-ended join (star/chain/object-object/EXISTS census rows). Delta
   generations and pending tables must be merged per key (same rules `resolveRow` already implements); decline on
   gap/pending states stays as-is.
2. **Distinct/degree/count pushdown (the analytics family).** `DISTINCT ?s` over one predicate = the key domain
   verbatim (zero dedup work); per-subject degree = run sizes (`tryCount` already computes them per key);
   `COUNT(*)` = plane total (needs a per-plane statement counter, currently absent — the "complete-plane accounting"
   deferred at `LmdbDirectAdjacencyStore.java:1811`). Today these all drain LMDB through prefix runs. The prefix-run
   machinery (`LmdbPrefixRunPlan`) is the natural consult point: offer an adjacency-backed run source next to the
   LMDB index run source.
3. **Restore lookups inside a write transaction and for parallel workers.** Two eligibility holes lose serving where
   it already works elsewhere: (a) `storeTxnStarted.get()` declines *every* lookup on a connection with an open write
   txn (`READ_YOUR_WRITES`) even when the txn has not written yet — a dirty-tracking flag ("has this txn actually
   recorded a delta?") would keep read-mostly transactions served; (b) `ParallelSnapshotSource` workers
   (`LmdbSailStore.java:3204-3376`) never call `tryDirect`, so intra-query parallelism silently drops row-level
   adjacency — the session-2 sibling-view fix from the join ExecPlan covered only `probe.adjacency()` (kernel path),
   not `statements()`/`count`/`exactDegree`.
4. **Doubly-bound probes.** `tryHas`/`tryCount` already serve `<s> :p <o>` via `lowerBound`; `open()` refuses the
   same shape (`DOUBLY_BOUND`). Wire the iterator path to the same run + `lowerBound` seek (result is 0..k context
   rows). This is Kuzu's edge-existence check and matters for selective FILTER EXISTS.
5. **Node edge dump under the CSF base.** `supportsPredicateEnumeration()` is false for the default paged-CSF base
   (`LmdbInMemoryAdjacencyIndex.java:128-130`), so `<s> ?p ?o` always declines and `LmdbDirectNodeIterator` is dead
   code unless `legacyBase=true`. Either add a compact node→predicate-ordinal sidecar to the CSF build, or decide the
   shape stays LMDB-served and delete the dead iterator path.
6. **Property-path BFS over adjacency.** The census shows `:p+` reachability makes 1 adjacency hit for 2000
   expansions: `LmdbNativePathPlan` only uses adjacency in `expandCachedLevel`; the seed step is an ordered root scan
   and `expandProbeLevel`/`PathFrontierSweep` are LMDB cursors. With item 1 (key enumeration for seeds) plus
   frontier-at-a-time expansion over `find`/`copyNeighbors`, BFS becomes a pure in-memory traversal — Kuzu's biggest
   structural advantage on multi-hop workloads. The frontier can stay a sorted id set, enabling galloping
   intersection with filters/masks.
7. **Unbound-predicate statistics never consult adjacency.** `SELECT DISTINCT ?p` and the predicate histogram
   recorded *no* fallback: the serving path (prefix run over `psoc`/root scan) never reaches `tryDirect`. The
   predicate catalog answers `DISTINCT ?p` outright; with per-plane totals (item 2) the histogram is a
   sum over ordinals — microseconds instead of a full-store scan. These are exactly the ANALYTICS theme q1/q2/q11
   shapes (currently the slowest family in the theme corpus).
8. **Optimizer statistics from the structures.** `meanFanOut` hard-declines (`OptionalDouble.empty()`,
   `LmdbDirectAdjacencyStore.java:1808-1813`) pending complete-plane accounting, so the planner still samples LMDB.
   Exact per-plane row counts, key counts, and mean/max degree are one pass over the key index at build time (or
   maintained incrementally); they'd replace sampled estimates in `PatternPlan.estimate` and the join arbiter with
   exact numbers for adjacency-covered predicates.
9. **SIP masks from adjacency key domains.** `adjacencyCacheKeys()` defaults to null and is never overridden
   (`NativeLmdbQuerySource.java:156`), so `publishCsrMask` never receives an adjacency-derived mask — the join
   ExecPlan already flagged the `long[]`-borrow plumbing as dead (Surprise #1 there). The key domain is the mask;
   a domain-cursor producer was sketched as M1 work item 6 in `.agent/lmdb-join-strategy-execplan.md`.

## New capabilities the structures unlock (beyond closing gaps)

- **O(1) exact degree in query results:** `SELECT ?s ?deg` where `?deg` is a COUNT over one predicate can bind run
  sizes directly — no join, no scan (Kuzu exposes this as fast `COUNT(rel)`).
- **Intersection-based semijoins:** FILTER EXISTS with a shared variable = galloping intersection of two sorted
  neighbor runs (the WCOJ `Level.enter` machinery already does this; it is not offered outside leapfrog).
- **Bidirectional path search:** both planes exist, so shortest-path/reachability can expand from both ends and meet
  in the middle — impossible to do cheaply over LMDB cursors.
- **Exact-empty pruning at plan time:** `NOT_FOUND` proofs (already served as `EmptyRecordIterator`) can also feed
  the planner: a bound endpoint with no run kills whole join subtrees before execution.

## Suggested sequencing

Items 1+2 first (one ExecPlan: "adjacency-served scans and scan-aggregates" — they share the key-enumeration
iterator and per-plane accounting), then 3 (eligibility holes; small, Routine A each), then 6 (paths; own ExecPlan),
with 4, 5, 8, 9 as milestones attached where they fit. `AdjacencyQueryShapeBenchmark` provides the paired-run
acceptance workloads; the census test pins engagement (counters observable via
`LmdbDirectAdjacencyStore.snapshotMetrics()` and `JoinDispatchTestAccess`).

## Superseded note: the node edge dump row (2026-08-08)

The `<s> ?p ?o` row of the table above said the shape cannot engage adjacency. That is no longer a statement about
capability, and this note records the change so the table is not read as current fact.

A node-to-predicate projection now exists — a sidecar CSF with one synthetic predicate, keyed by node, holding that
node's sorted predicate list and nothing else — and the whole path above it is built: the interpreted row iterator,
count and existence shortcuts, predicate-ordered streaming, two compiler IR nodes, and both parallel kernel rungs.
The work is described in full in `.agent/node-predicate-projection-execplan.md`.

Every part of it ships behind switches that default to off, so the table's `NO` remains what an operator sees out of
the box. The reason the switches are off is worth carrying back here, because it changes what this survey should
recommend. Measured, enabling the projection makes the flagship dump-and-group shape 2.85 times *slower* (1.673 ±
0.144 ms to 4.768 ± 0.156 ms), leaves the other dump shapes unchanged, and roughly doubles the adjacency base build.
A node's outgoing statements are already contiguous and sorted in the subject-ordered disk tree, so a transpose
cannot beat a single seek plus a sequential walk; it replaces that with one lookup per predicate. The projection's
value is item 6 of this survey's own list — wildcard and bidirectional traversal that never touches disk, where the
alternative is trying every predicate in the store at every node reached — and not the dump shapes in this table.

Two changes would alter that arithmetic and are the right next steps for anyone picking this up: resolving a
predicate's run without a second lookup (which needs a generation-stamped handle, since a raw pointer into the main
index goes stale on every base rewrite), and building the projection inside the parallel build rather than after it.
