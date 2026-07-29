# In-memory direct adjacency for 5–20 billion RDF statements

This document is both an ExecPlan and an implementation specification. It is intentionally more detailed than a normal
ExecPlan: the implementer should not have to choose the storage model, memory layout, snapshot protocol, class
boundaries, fallback behavior, configuration, rollout sequence, or test matrix.

The adjacency data is **strictly process-local and in-memory**. It must never be memory-mapped, serialized,
checkpointed, journaled, or written to a temporary file. LMDB remains the only persistent representation. On every
process start the adjacency begins empty and is rebuilt from an LMDB snapshot while queries continue through the
ordinary LMDB path.

The living sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept
current while work proceeds. This plan is maintained according to `.agent/PLANS.md`. It is a Routine D change. Where a
milestone changes externally visible behavior, create and observe its smallest focused failing test before editing
production code. Never use `-am` or `-q` for a test run.

## Purpose / Big Picture

Replace the current whole-predicate, on-heap CSR representation as the scale path with a compressed, node-centric,
off-heap adjacency index. A bound graph expansion such as `(subject, predicate) -> objects` or
`(object, predicate) -> subjects` should go from the RDF4J value ID directly to a compact node header, locate the
predicate/direction group, and decode an immutable neighbor run. It must not open an LMDB cursor, allocate an object per
edge, use a Java hash table for ordinary reference IDs, or require one array whose length is the statement count.

When the bound node is a reference ID, the same header also supports `(subject, ?predicate)` and
`(object, ?predicate)` expansion by enumerating its plane entries. This is node-local adjacency enumeration, not a
predicate-wide/root index.

The structure deliberately follows ArcadeDB's useful index-free-adjacency shape:

    RDF4J reference value ID
        -> arithmetic type/page/slot
        -> compact node header
        -> outgoing or incoming predicate entry
        -> immutable compressed neighbor/context run

An ordinary RDF statement has no separate in-memory edge record. It contributes two incidences:

    subject header / outgoing predicate -> object
    object header  / incoming predicate -> subject

The named-graph context, when nonzero, is an aligned compressed **context-ordinal** side column in each incidence; the
snapshot's immutable context catalog maps it back to the raw RDF4J ID. Avoiding raw 64-bit context repetition and a
standalone edge record is essential to approaching 10–12 bytes per statement.

LMDB remains authoritative. Every adjacency lookup at snapshot revision `S` has exactly two legal outcomes:

1. return an in-memory version proven to represent `S`; or
2. decline and use the existing LMDB iterator attached to the same pinned transaction.

There is no legal path that mixes a newer adjacency generation with an older LMDB snapshot. Snapshot isolation is a
primary invariant.

## Capacity target

For 20,000,000,000 statements and a hard allowance of 256 GiB:

    hard bytes per statement
      = 256 * 2^30 / 20,000,000,000
      = 13.7438953472

For an **effective configured cap of exactly 256 GiB**, the current steady index must target at most 86% of the limit:

    steady target                220.16 GiB
    steady bytes per statement    11.8197504
    update/snapshot/build reserve  35.84 GiB

Reference envelopes:

| Statements | Cap | Hard bytes/statement | 86% steady bytes/statement |
| ---: | ---: | ---: | ---: |
| 1.5 billion | 64 GiB | 45.8130 | 39.3992 |
| 5 billion | 256 GiB | 54.9756 | 47.2790 |
| 20 billion | 256 GiB | 13.7439 | 11.8198 |

The 20-billion case is therefore the binding design point; a representation that passes it has substantial headroom at
five billion under the same cap.

If the deployment means decimal 256 GB, the hard budget is 12.8 bytes per statement. Configuration is in bytes, so the
operator can select either value explicitly.

When `directAdjacencyMaxBytes` is unset/zero, resolve the cap once at store construction to exactly 50% of the
effective JVM maximum heap:

    effectiveMaxBytes = (Runtime.getRuntime().maxMemory() >>> 1) & ~7L

This rounds down to an eight-byte boundary; never recalculate it while the store is open. `Runtime.maxMemory()` is
authoritative and can be slightly below the command-line `-Xmx` value, so `-Xmx512g` produces an AUTO cap slightly
below or, on some runtimes, equal to 256 GiB; it is not an exact 256-GiB promise. Likewise, `-Xmx64g` resolves at or
slightly below 32 GiB. A positive configured byte count overrides the automatic value and is the way to request an
exact 256-GiB cap. The native adjacency allocation is outside the Java heap, so deployment sizing must allow process
RSS for `Xmx + adjacency cap + LMDB mappings + JVM/native overhead`; basing the default on Xmx is a policy, not a claim
that the bytes live inside Xmx.

Every sizing report and acceptance check uses the resolved `effectiveMaxBytes`, never the nominal `-Xmx` text:

    hundreds        = effectiveMaxBytes / 100
    remainder       = effectiveMaxBytes % 100
    steadyLimitBytes = hundreds * 86 + (remainder * 86) / 100
    peakLimitBytes   = effectiveMaxBytes

The worked `220.16 GiB` limit below therefore applies only when `effectiveMaxBytes == 256 GiB`. AUTO on a JVM that
reports less uses the correspondingly smaller computed limit.

This is a data-dependent target, not a universal guarantee. A graph with one unique subject, object, predicate, and
named graph per statement carries more information than the budget can encode twice in a general-purpose format. The
builder must calculate the exact encoded size and peak working memory for the real dataset before publishing. If the
index does not fit, it stays unavailable and LMDB remains correct; the implementation must never overcommit native
memory or silently drop required incidences.

## User-visible result

After completion:

- direct adjacency is disabled by default and changes nothing until configured;
- the store starts immediately with LMDB serving all queries;
- an asynchronous or explicitly requested in-memory build publishes only after it reaches one exact revision;
- a bound subject/object probe uses direct adjacency in `PREFER` mode and transparently falls back to LMDB;
- a bound reference node with an unbound predicate can enumerate its exact outgoing/incoming groups; an inlined object
  with an unbound predicate falls back;
- SNAPSHOT transactions keep their opening view across commits, delta consolidation, and rebuild attempts;
- SERIALIZABLE continues to bypass derived adjacency;
- read-your-writes continues to bypass adjacency while `storeTxnStarted` is true;
- ordinary commits create immutable row versions for touched groups, not copies of whole predicates;
- supernodes use ordered range chunks so one update does not copy the whole adjacency list;
- closing the store closes every native `Arena`; restarting discards and rebuilds everything;
- no global statement count, edge ordinal, degree, or byte offset is narrowed to `int`;
- metrics report native bytes, build bytes, retained snapshot bytes, delta bytes, revision lag, and fallback reasons.

## Non-negotiable invariants

Use identifiers `I1` through `I18` in design comments and tests.

1. **I1 — memory only.** Adjacency code performs no file create, write, map, move, force, or delete. It does not add an
   LMDB database. The only source for a rebuild is the existing authoritative LMDB indexes.
2. **I2 — LMDB authority.** Missing, stale, corrupt, incomplete, over-budget, or disabled adjacency always falls back
   to LMDB and never changes query results.
3. **I3 — exact snapshot.** A read stamped `S` uses only a base and row versions valid for `S`. Failure to prove
   coverage returns `NOT_COVERED`, not an empty run.
4. **I4 — exact empty.** `NOT_FOUND` means the index proves the row is empty at `S`. It is never used for missing
   coverage.
5. **I5 — commit window safety.** Before a commit revision becomes visible, affected rows are marked pending. Until
   their immutable versions publish, those rows fall back; untouched rows may continue using older versions.
6. **I6 — no per-edge objects.** The steady representation is primitive and off-heap. No `Statement`, boxed `Long`,
   collection node, or Java object exists per incidence, group, or vertex.
7. **I7 — no edge-scale `int`.** Statement/incidence counts, degrees, revisions, run ordinals, and virtual addresses
   are `long`. `int`/u32 is allowed only for a format-bounded dimension such as a predicate ordinal, 128-edge block,
   4,096-slot page, or caller batch; it must never be derived by narrowing an edge-scale value.
8. **I8 — bounded working memory.** Build, commit apply, and consolidation memory are limited independently of total
   statement count. An over-limit operation degrades adjacency instead of risking OOME.
9. **I9 — sorted runs.** Every materialized group is unsigned `(rawNeighborId, rawContextId)` order. Equal raw
   neighbors are adjacent. Stored context ordinals never define row order.
10. **I10 — set semantics.** Add means ensure-present, remove means ensure-absent, and the final operation wins within a
    transaction.
11. **I11 — explicit/inferred separation.** The four planes—outgoing explicit, incoming explicit, outgoing inferred,
    incoming inferred—never alias. Promotion/demotion produces fresh-build-equivalent rows.
12. **I12 — context exactness.** Context ordinal `0` is the null graph. Every nonzero ordinal resolves through the
    read view's immutable catalog to one raw context ID. Bound-context filtering and unbound enumeration are exact.
13. **I13 — immutable publication.** A base, delta generation, run, and chunk are fully written before a volatile
    reference makes them readable. Published bytes are never modified.
14. **I14 — lease before free.** An off-heap arena is closed only after it is unreachable from the active index and its
    final read lease is released.
15. **I15 — build revision.** A base built at `B` cannot serve `S < B`. It publishes only after every commit from
    `B + 1` through the publication revision has been applied without a gap.
16. **I16 — overflow is a gap.** Losing any required commit event disables adjacency for that revision and later
    revisions until a fresh base is built. It never attempts a best-effort merge.
17. **I17 — hard accounting.** Base, deltas, pending commits, retained snapshots, builder counters, output arenas, and
    Java metadata are all charged before allocation.
18. **I18 — checked native access.** Every decoded offset, length, rank, block count, and ordinal is range-checked before
    accessing a `MemorySegment`.

## Context and orientation

The implementation must integrate with these existing symbols:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCsrAdjacencyCache.java` currently owns one complete
  on-heap `CsrEntry` per `(predicate, direction, explicit)` and short snapshot-generation chains.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbCsrCommitDelta.java` collects exact mutation events only
  for predicates that had live CSR entries at transaction start.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java` has the single genuine-mutation choke
  points `recordFanOutAdded` and `recordFanOutRemoved`, process-local `dataRevision`, `CsrCommitListener`, and the commit
  ordering in `endTransaction(boolean)`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnManager.java` pins LMDB read transactions, stamps
  `snapshotRevision`, reports `minPinnedSnapshotRevision()`, and invalidates pinned snapshots on map resize.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java` constructs/closes the cache, applies
  deltas after authoritative commit, bypasses during `storeTxnStarted`, and passes snapshot revisions to every cache
  consult.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java` defines the current
  int-indexed `NativeAdjacency` contract used by interpreted and generated kernels.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueIds.java` encodes a six-bit type and a 56-bit payload
  for most values. URI, referenced literal, blank node, and RDF-star triple IDs have allocated payloads suitable for
  arithmetic paging. Inlined values need a separate incoming-key structure.
- `plans/lmdb-native-engine/24-csr-mvcc-delta-overlay.md` documents exact mutation capture and commit merge.
- `plans/lmdb-native-engine/25-csr-native-snapshot.md` documents pinned LMDB snapshots and snapshot-aware CSR
  generations. Preserve those correctness properties.

The current CSR estimate is approximately 28 bytes per incidence, or about 56 bytes per statement for two directions.
Its global `long[]` and `int[]` fields also cannot represent a predicate with more than roughly 2.1 billion incidences.
The scale problem is structural, not a larger-cache-setting problem.

Implementations that report roughly twice the bidirectional capacity usually avoid several costs that the current CSR
pays simultaneously: a per-predicate open-addressed key table, a second dense-key array, per-row starts, raw 64-bit
neighbors, optional raw 64-bit contexts, and retained whole-predicate generations. Some also assume 32-bit local IDs,
no named graphs, or no snapshot versions, so their headline number is not an equivalent RDF contract.

This design gets the comparable density without weakening RDF semantics by paying the reference-node locator once
across all predicates, using six-byte group entries at 100 predicates, bit-packing neighbor/context blocks, storing no
edge object, and versioning only touched rows. The exact sizer keeps those savings honest for the real ID/context
distribution.

ArcadeDB's relevant ideas are direct record identity, vertex-owned incoming/outgoing edge chunks, lightweight edges, and
special supernode storage:

- <https://docs.arcadedb.com/arcadedb/concepts/basics>
- <https://docs.arcadedb.com/arcadedb/reference/supernodes>
- <https://docs.arcadedb.com/arcadedb/tutorials/what-is-arcadedb>

Use ordered range chunks rather than hash stripes. LMDB already serializes writes, while RDF4J native operators benefit
from ordered neighbor runs.

## High-level architecture

The active in-memory index is:

    LmdbDirectAdjacencyStore
      MemoryAccount
      AtomicLong emergencyGapFromRevision
      AtomicReference<LmdbAdjacencyPublishedState>
        InMemoryAdjacencyIndex
          baseRevision
          BasePredicateDictionary
          BaseContextDictionary
          ReferenceNodeLocator
          InlineIncomingIndex
          BaseArena
        OverlaySet
          PredicateCatalog
          ContextCatalog
          ArenaCatalog
          ConsolidatedOverlay
          DeltaGeneration[] ordered by revision
        Coverage
        PendingCommitSet
        serving state / gap

The base is immutable. A committed revision with covered net changes creates an immutable `DeltaGeneration` containing
complete replacement group runs (or tombstones) for changed rows; a net-neutral/unselected revision advances without a
generation. A lookup searches applicable delta generations newest to oldest and then the base. A consolidation merges
several generations while preserving versions required by pinned snapshots.

The atomic publication unit is:

    final class LmdbAdjacencyPublishedState {
        long epoch();                         // monotonically increasing, process-local
        InMemoryAdjacencyIndex base();        // nullable only while unavailable
        LmdbAdjacencyOverlaySet overlays();
        Coverage coverage();
        PendingCommitSet pending();
        long gapFromRevision();               // Long.MAX_VALUE means no gap
        AdjacencyServingState servingState();
        boolean tryRetain();
        void release();
    }

Fields other than the close-bit/refcount word are final. The publisher owns one reference. Constructing a replacement
first retains each shared owner, and publication transfers the new publisher reference; only then is the old publisher
reference released. An EMPTY/BUILDING state has no base and can contain bounded build-catch-up commits, but every query
declines.

Do not mutate base headers in place. That would require an eight-byte atomic head pointer per group and consume tens of
gigabytes at billion-group scale. Compact base headers use five-byte run references; updateability lives in bounded
overlay generations.

The lifecycle is:

    EMPTY
      -> BUILDING
      -> CATCHING_UP
      -> ACTIVE

    ACTIVE
      -> CONSOLIDATING
      -> ACTIVE

    ACTIVE or BUILDING or CATCHING_UP
      -> DEGRADED_GAP
      -> QUIESCING_FOR_REBUILD
      -> BUILDING

    ACTIVE
      -> APPLY_STALLED
      -> QUIESCING_FOR_REBUILD
      -> BUILDING

    any state
      -> CLOSED

Use these closed enums:

    MaintenanceState {
        EMPTY, BUILDING, CATCHING_UP, ACTIVE, CONSOLIDATING,
        APPLY_STALLED, DEGRADED_GAP, QUIESCING_FOR_REBUILD,
        MEMORY_REFUSED, FAILED_CORRUPT, CLOSED
    }

    AdjacencyServingState {
        UNAVAILABLE,       // every new acquisition is a fallback view
        ROW_EXACT,         // run the revision/pending/coverage proof
        CLOSED
    }

Maintenance state drives work/metrics. The immutable published serving state drives readers. A state with a base and a
later gap remains `ROW_EXACT` so snapshots before the gap can still serve; the row resolver rejects snapshots at or
after it. Entering quiescence publishes `UNAVAILABLE` for new acquisitions while existing retained `ROW_EXACT` views
finish.

All states except `ACTIVE` may serve some older pinned revisions, but new/latest queries use LMDB unless coverage is
proved. State transitions and reasons are metrics.

## Memory ownership and virtual arena

Use JDK 25 `java.lang.foreign.Arena` and `MemorySegment`; do not use `Unsafe`, direct `ByteBuffer` cleaner tricks, mmap,
or a new native dependency.

`LmdbAdjacencyArena` owns one JDK `Arena.ofShared()` and allocates native memory in bounded regions. A confined arena is
incorrect because the maintenance thread publishes bytes that query threads subsequently read.

    final class LmdbAdjacencyArena implements AutoCloseable {
        static final long MAX_REGION_BYTES = 1L << 30; // 1 GiB

        long allocateRef(long bytes, long alignment);
        MemorySegment slice(long encodedRef, long bytes);
        long allocatedBytes();
        long capacityBytes();
        void close();
    }

`allocateRef` returns a local encoded reference in a logical virtual address space, even though physical storage is a
list of at most 1-GiB `MemorySegment`s. Address zero and encoded reference zero are reserved as null. Every allocation
is eight-byte aligned.

Store addresses as unsigned 40-bit units:

    encodedRef = virtualByteAddress >>> 3
    virtualByteAddress = encodedRef << 3

Five bytes address eight TiB, well above the 256-GiB limit. `readU40` and `writeU40` are the only encoding helpers.
Resolution is:

    regionIndex = virtualByteAddress >>> 30
    offset       = virtualByteAddress & ((1L << 30) - 1)

All multibyte fields are explicitly little-endian. Define shared unaligned u16/u32/u64 layouts/VarHandles once; never
use platform-native byte order. `readU40` combines five unsigned bytes, and `writeU40` rejects values outside
`0..0xffffffffff`; allocation-produced references are additionally required to be nonzero.

One allocation may not cross a 1-GiB virtual-region boundary. Runs are already capped by supernode chunking; large
primitive tables use segmented accessors. If an allocation does not fit the current region, charge its unused tail as
alignment/region slack and begin the next virtual region. `slice(ref, bytes)` rejects null, overflow, a cross-region
range, access after close, and a range beyond the physical segment.

Variable records that are not defined as segmented—principally one node header—must be at most 1 GiB. Preflight reports
`RECORD_TOO_LARGE` and declines that coverage if this bound is exceeded; it must not narrow the length or create a
cross-region pseudo-slice. The 100-predicate target is far below this guard.

Immutable outputs are sized before allocation. Construct their arena with the exact simulated region capacities:
full 1-GiB regions plus the exact final tail, including boundary slack. Dynamically growing workspaces may add smaller
charged regions, but must not allocate a 1-GiB region merely to hold a small delta. The production arena therefore
accepts a sizing plan; tests inject small region sizes without a production-only branch.

`allocatedBytes` is logical bytes handed to callers; `capacityBytes` is actual native segment capacity. Hard-limit
accounting and high-water metrics use `capacityBytes`. The sizing report exposes both and includes their difference as
alignment/region slack.

An arena is append-only. It is reclaimed as a unit when its owning base, delta generation, build workspace, or
consolidated overlay loses its last lease. There is no general native free list and no reuse of an address from a live
arena.

Use separate arenas for:

- build counters;
- the unpublished base;
- each commit/batched delta generation;
- each consolidation output;
- pending mutation arrays when they spill off heap.

This ownership boundary makes reclamation explicit and prevents a stale five-byte reference from resolving into reused
memory.

### Arena catalog and opaque run handles

A u40 reference is local to one arena. It is sufficient in base headers and in a generation's own row directory because
the owner is implicit, but it is not a complete query handle and it cannot identify an unchanged supernode chunk owned
by an older arena.

Each immutable `OverlaySet` therefore owns an immutable `ArenaCatalog`:

    slot 0      base arena
    slot 1..N   consolidated and delta arenas retained by this overlay set

Required API:

    final class LmdbAdjacencyArenaCatalog implements AutoCloseable {
        int size();
        LmdbAdjacencyArena arena(int unsignedSlot);
        long packHandle(int unsignedSlot, long localU40Ref);
        int unpackSlot(long runHandle);
        long unpackLocalRef(long runHandle);
        LmdbAdjacencyArenaCatalog appendRetained(LmdbAdjacencyArena arena);
        void close(); // releases this catalog's owner references
    }

Slots are unsigned bytes. Adding a delta copy-appends one catalog entry and preserves every existing slot.
Consolidation copies reachable non-base payloads into its output arena and creates a short new catalog. If a catalog
would require slot 256, consolidate or rebuild before publication; never wrap a slot.

The direct implementation's opaque `long` run handle is:

    bits 47..40    u8 arenaSlot
    bits 39..0     u40 localRunRef
    bits 63..48    zero

    runHandle = ((long) arenaSlot << 40) | localRunRef

Valid handles are positive and cannot collide with `NOT_FOUND = -1` or `NOT_COVERED = -2`. `DirectNativeAdjacency`
holds the read view's `ArenaCatalog`, decodes the slot, retains no independent owner, and resolves the local reference
through that catalog. Base-header u40 values become handles with slot zero; a delta/consolidated lookup supplies its
catalog slot.

An `OverlaySet` retains every arena in its catalog, including arenas referenced only by an unchanged supernode chunk.
An old overlay set releases those owners only after its last read-view lease closes. This is the rule that prevents a
cross-generation chunk reference from becoming dangling.

`InMemoryAdjacencyIndex` stores the slot-zero base arena reference used by base accessors, but that field is a
non-owning alias. The published state's catalog/reference graph is the sole lifetime authority; do not close an arena
from a base or generation object's `close` method.

`LmdbAdjacencyMemoryAccount` must reserve before every arena allocation:

    enum MemoryKind {
        BASE,
        DELTA,
        PENDING,
        RETAINED_SNAPSHOT,
        BUILD_COUNTERS,
        BUILD_OUTPUT,
        CONSOLIDATION_OUTPUT,
        JAVA_METADATA
    }

It exposes charged, allocated, released, high-water, and refused bytes per kind. A reservation failure is a normal
derived-index refusal. Each allocation is charged exactly once. When an arena stops being current but remains leased by
an old snapshot, reclassify its charge as `RETAINED_SNAPSHOT`; do not add a second charge for the same bytes.
Likewise, successful publication reclassifies `BUILD_OUTPUT` to `BASE` and `CONSOLIDATION_OUTPUT` to `DELTA` without
changing total charged bytes.

## Predicate dictionary and four adjacency planes

Build an immutable base predicate dictionary before sizing the base:

    raw predicate RDF4J ID <-> dense predicate ordinal

Assign base ordinals in unsigned raw-ID order. Every `OverlaySet` carries an immutable `PredicateCatalog` whose base
prefix is that dictionary. A commit that introduces predicates assigns new ordinals in unsigned raw-ID order within
the commit and copy-appends them to the catalog. Publish the new catalog and its `DeltaGeneration` in the same
`LmdbAdjacencyPublishedState` swap.

The base lookup table is a sorted primitive u64 raw-ID array; ordinal is its array position. Extensions contain a sorted
primitive `(rawId, ordinal)` array for raw-ID lookup plus an append-only ordinal-to-raw array. No Java hash entry or
object exists per predicate. Existing ordinals never change within one base lifetime. A new ordinal that exceeds the
base's one-/two-byte width appears only in u32 delta directories; a base-header lookup immediately proves it absent.
A rebuild may choose a wider base representation and a new ordinal assignment because old read views retain the old
catalog. Predicate IDs are not semantically reused while an old pinned snapshot can observe them because value-ID
retirement honors the same snapshot watermark.

The format supports at most `0xffff_ffff` predicate ordinals. Count predicates in a long and refuse FULL publication
before narrowing if that bound is exceeded. Store/compare four-byte ordinals unsigned.

`Coverage` is immutable and state-local:

    mode = FULL
      -> every raw predicate, all four planes

    mode = SELECTED
      -> sorted u64 selectedRawPredicateIds, all four planes
         configured-but-unresolved IRI set
         sorted u64 classifiedUnselectedRawPredicateIds

For SELECTED, UNKNOWN raw IDs are not covered until classification publishes a replacement state. There is no
row-level coverage bit and no partial plane for a selected predicate.

Node headers have four planes in this fixed order:

    0 OUTGOING_EXPLICIT
    1 INCOMING_EXPLICIT
    2 OUTGOING_INFERRED
    3 INCOMING_INFERRED

Choose one predicate/count width for the base:

| Predicate count | Width |
| ---: | ---: |
| `<= 255` | 1 byte |
| `<= 65,535` | 2 bytes |
| otherwise | 4 bytes |

The thresholds are count thresholds, not maximum-ordinal thresholds: a plane containing 256 groups cannot encode its
count in one byte even though ordinals 0–255 fit. The stated 100-predicate dataset uses one byte. A header stores four
plane counts using the same width, followed by each plane's entries. An entry is:

    predicate ordinal   predicateWidth bytes, little-endian
    run reference       5 bytes, little-endian u40

Entries are strictly predicate-ordinal ascending within a plane. Header byte length is:

    4 * predicateWidth
      + totalGroupCount * (predicateWidth + 5)
      + padding to eight-byte alignment

To locate `(node, predicate, plane)`, read four counts, sum preceding plane counts, and binary-search only the selected
plane. With 100 predicates this is at most seven comparisons and the common node has only a handful of groups. Do not
materialize a Java header or entry object.

## Context ordinal catalog

Do not store raw 64-bit context IDs once per incidence. The existing authoritative `contexts` DBI already enumerates
currently used nonzero context IDs at a pinned TripleStore snapshot.

At base build:

1. reserve ordinal `0` for the default graph/raw context `0`;
2. scan `TripleStore.getContexts(pinnedTxn)` in unsigned raw-ID order;
3. assign named-context ordinals `1..C`;
4. append raw IDs to a segmented temporary u64 array charged as `BUILD_COUNTERS`;
5. use that temporary array for Pass-1 raw-to-ordinal lookup;
6. copy it once into `baseRawContextByOrdinal` in the base arena during Pass 2, switch lookup to the base copy, and free
   the temporary array before Pass 3.

Because the base array is raw-ID sorted, base lookup is binary search and ordinal is index plus one.

Commits can introduce contexts. Before delta sizing, collect previously unseen nonzero raw context IDs, sort them
unsigned, and append one immutable logical segment:

    firstOrdinal     u64
    count            u64
    arenaSlot        u8
    rawIdsRef        u40

IDs are sorted within a segment, so `ordinalForRaw` binary-searches the base and then at most
`maxDeltaGenerations` extension segments; `rawForOrdinal` finds the segment by its ordinal interval and indexes one
u64. Segments never renumber during a base lifetime. Consolidation copies live extension arrays into its output arena
while preserving segment boundaries/first ordinals. A rebuild may assign fresh raw-sorted ordinals because it rewrites
all runs and old read views retain the old catalog.

The catalog API is:

    interface ContextCatalog {
        long size();                         // includes ordinal zero
        long ordinalForRaw(long rawContext); // -1 when absent
        long rawForOrdinal(long ordinal);
    }

Run payloads store context ordinals. Their conceptual ordering remains unsigned `(rawNeighborId, rawContextId)`;
post-build appended ordinals need not be raw-ID ordered. Encoders consume raw-sorted pairs, translate each context to an
ordinal, and preserve pair order. `lowerBound` and bound-context comparison compare the decoded raw context, not ordinal
numeric order. A sequential cursor caches the last context segment to make ordinal-to-raw translation cheap.

If context segment allocation or catalog publication fails for a committed new context, the affected revision follows
the pending/global-gap rules; never write a raw context into an ordinal column.

## Reference node locator

Reference IDs are `T_URI`, `T_LITERAL`, `T_BNODE`, or `T_TRIPLE`. Use:

    PAGE_SHIFT = 12        // 4,096 slots

For a raw reference ID:

    type       = ValueIds.getIdType(id)
    payload    = ValueIds.getValue(id)
    pageNo     = payload >>> PAGE_SHIFT
    slotInPage = payload & 0xfff

`ReferenceNodeLocator` has one top-level page-reference array per reference type. The arrays are sized from the maximum
payload observed at base revision `B` during the sizing scan. They are segmented, primitive, and off-heap; they do not
contain one Java object per page. The index is flat: there is no shard level. A page reference is an encoded u40 address
or zero.

`pageNo >= topPageCount(type)` is a normal base-row absence, not a corrupt-reference or I18 failure. The resolver checks
snapshot overlays before the base. Therefore, under a continuous covered view, a reference node allocated after `B`
either has a matching overlay row/tombstone or is provably empty for the requested group at the snapshot. The locator
must return zero without reading native memory for an out-of-range page. Only an in-range nonzero page reference whose
decoded address or page bytes fail validation is corruption and forces fallback.

Every final locator page is:

    occupancy bitmap        4096 bits = 512 bytes
    rank prefix             64 * u16  = 128 bytes
    header references       activeCount * 5 bytes

`rankPrefix[word]` is the number of set bits before that 64-bit bitmap word. For a present slot:

    rank = rankPrefix[word]
         + Long.bitCount(bitmapWord & bitsBelow(slotWithinWord))

    headerRef = readU40(referenceBytes + rank * 5)

At full occupancy this costs `5.15625` bytes per active node. Sparse-page overhead is calculated exactly in the sizing
pass.

The build-time count page is different and temporary:

    occupancy bitmap        512 bytes
    four u8/u16/u32 plane counts for each of 4096 slots

After sizing, copy each node's four counts into its final header, repurpose count cells as per-plane insertion cursors,
and free the complete counter arena after the base publishes. Do not retain build counts in the steady index.

## Inlined object incoming index

Subjects cannot be inlined literals, but objects can. Their 56-bit payloads are not densely allocated, so they do not
use `ReferenceNodeLocator`.

Build one `InlinePlaneIndex` for each **nonempty** `(predicateOrdinal, explicit/inferred)` incoming plane, directly from
a predicate-leading object scan. It stores keys in unsigned object-ID order:

    65,537-entry u32 radix directory keyed by top 16 raw-ID bits
    segmented u40 block-reference array
    blocks of at most 256 object keys

Each radix entry is a **block ordinal**, not a key ordinal; entry `r + 1` is the exclusive block end. Start a new block
when the top-16-bit prefix changes, even if the previous block has room. Refuse publication if block count exceeds
`0xffff_ffff` before narrowing.

A base plane with zero inline keys allocates no 65,537-entry radix directory, block-reference array, or key block.
Coverage metadata still records the plane as exactly covered and empty at `B`; later rows are supplied by overlays.
`inlineRadixBytes` charges directories only for nonempty planes and reports their count explicitly. This is
load-bearing for FULL coverage with large predicate domains: at 65,537 u32 entries, each allocated directory is
262,148 bytes before alignment, so preflight—not lazy allocation after publication—must reject a dataset whose
nonempty-plane directory cost exceeds the cap.

An inline key block is:

    u8 countMinusOne                    // 0..255
    u8 keyDeltaBitWidth                 // 0..64
    u16 reserved
    u64 baseRawKey                      // also the binary-search fence key
    bit-packed unsigned (key - base) for count keys, including zero first delta
    padding to eight-byte alignment
    u40 localRunRef[count]
    padding to eight-byte alignment

Lookup reads the radix range, binary-searches fence keys, decodes one 256-key block, and verifies the complete 64-bit
object ID. The block-reference and per-key-run-reference arrays are segmented when needed; no single allocation crosses
an arena region. It is exact, off-heap, and contains no general hash table or per-key object.

FULL coverage includes every inline incoming group. SELECTED coverage includes all four planes, including inline
incoming groups, for each configured predicate IRI. Any other predicate is `NOT_COVERED`; there is no automatic or
row-by-row eviction.

## Group run codecs

Every group is a complete sequence sorted by unsigned `(rawNeighborId, rawContextId)`. Use:

    0 SMALL_VARINT       1..15 incidences
    1 BLOCK_FOR          16..1,048,575 incidences and encoded bytes <= 64 MiB as an ordinary run;
                         up to 131,072 incidences/128 MiB as a directory child
    2 CHUNK_DIRECTORY    >=1,048,576 incidences or encoded bytes > 64 MiB
    3 RESERVED           reject if decoded

Every run starts with a physically separate tag byte:

    bits 0..1 codec
    bit 2     at least one context is nonzero
    bit 3     all neighbors satisfy ValueIds.isOrderedInteger
    bits 4..7 zero

The tag byte precedes the fields shown for each codec below. It is included in every encoded-size calculation,
component offset, bounds check, and alignment decision. It is never fused with `edgeCount`, `blockShift`, or `level`,
even though some currently unused tag bits could hold those values.

`SMALL_VARINT` is:

    u8 tag                               // codec = SMALL_VARINT
    u8 edgeCount                         // 1..15
    u16 neighborByteLength
    u16 contextByteLength                // zero when contexts absent
    u16 reserved                         // zero
    neighbor stream
    optional context stream
    zero padding to eight-byte alignment

The first neighbor is a raw unsigned `Varint`; later values are unsigned deltas from the previous neighbor. Contexts
are unsigned **context-ordinal** varints in row order. Scanning at most 15 entries for random access is acceptable.

`BLOCK_FOR` is:

    u8 tag                               // codec = BLOCK_FOR
    u8 blockShift = 7
    u16 reserved0 = 0
    u32 reserved1 = 0                    // aligns edgeCount at offset 8
    u64 edgeCount
    u32 blockCount = ceil(edgeCount / 128)
    u32 blockPayloadByteLength
    u32 blockOffsets[blockCount + 1]
    block payloads

Each block is:

    u8 countMinusOne
    u8 neighborBitWidth                  // 0..64
    u8 contextBitWidth                   // 0..64
    u8 reserved
    u64 neighborBase                     // first neighbor
    if contexts present:
        u64 contextOrdinalBase           // unsigned minimum ordinal in block
    bit-packed neighbor-base deltas for count lanes
    optional bit-packed context-base deltas for count lanes
    zero padding to eight-byte alignment

Bit packing is lane order, least-significant bit first: lane `i` begins at bit offset `i * width` in a little-endian
u64 word stream. A value crossing a word consumes the high remainder of the first word and low remainder of the next.
Width zero stores no words; width 64 stores one raw u64 per lane. Unused high bits in the final word and alignment bytes
are zero. The neighbor stream includes a zero first delta.

Unsigned subtraction/addition is modulo 64 bits. Width 64 is the incompressible fallback. Block-offset overhead is
`4 / 128 = 0.03125` bytes per incidence.

An empty delta row has no run allocation. Its row-directory/version-list tombstone flag is set and local run reference
is zero. Codec value 3 remains invalid so corrupt bytes cannot masquerade as an empty row.

The sizing and writing paths share a codec state machine so predicted and emitted sizes cannot differ:

    final class LmdbAdjacencyRunCodec {
        static long encodedBytes(SortedPairSource source, ContextCatalog contexts);
        static long encode(SortedPairSource source, ContextCatalog contexts,
                LmdbAdjacencyArena target); // returns local u40
        static long edgeCount(ArenaCatalog catalog, long runHandle);
        static long neighborAt(ArenaCatalog catalog, long runHandle, long ordinal);
        static long contextAt(ArenaCatalog catalog, ContextCatalog contexts, long runHandle, long ordinal);
        static int copy(...);
        static long lowerBound(...);
    }

All decode methods take the arena/context catalogs captured by the read view and validate arena slot, local reference,
context ordinal, codec, count, block index, byte range, and target capacity.

Sizer and encoder require a strictly increasing unsigned `(rawNeighbor, rawContext)` source. An equal pair or
descending pair is a build/apply invariant failure, not silently deduplicated by the codec.

Sequential iteration creates a reusable `RunCursor` that resolves the catalog slot and native segment once per
ordinary run or once per supernode chunk. The per-edge loop operates on the resolved segment/base offset; it does not
repeat catalog lookup, reference counting, state reads, or region-index calculation.

## Supernodes

A group becomes `CHUNK_DIRECTORY` at 1,048,576 incidences or 64 MiB encoded. Use unsigned pair ranges:

- target chunk incidences: 65,536;
- target chunk bytes: 64 MiB;
- split when a chunk exceeds 131,072 incidences or 128 MiB;
- during consolidation, merge adjacent chunks when their combined size is at most 65,536 incidences and 64 MiB;
- chunk boundaries are complete `(rawNeighborId, rawContextId)` pair boundaries.

On first promotion, stream a chunk boundary before the next pair when adding it would exceed either target. At the
incidence threshold this normally creates 16 chunks. Because RDF statements are a set, a pair is one incidence and a
chunk can always be split within the 128-MiB hard child limit, even when one neighbor has many contexts. These rules are
deterministic; do not choose boundaries from thread count or current free memory.

The directory is:

    u8 tag                               // codec = CHUNK_DIRECTORY
    u8 level = 0
    u16 reserved = 0
    u32 chunkCount
    u64 directoryByteLength
    u64 totalEdgeCount
    repeated:
        u64 firstNeighbor
        u64 firstRawContext
        u64 edgeStart
        u64 edgeCount
        u8 arenaSlot
        u40 chunkRunRef
        u16 reserved

Chunks use `SMALL_VARINT` or `BLOCK_FOR`. A commit rewrites only affected range chunks plus the small directory. Old
chunks remain immutable while a snapshot can reference them. The explicit arena slot is interpreted through the
read-view catalog described above. Range routing and directory binary search compare unsigned
`(rawNeighbor, rawContext)`; concatenating chunks preserves run order.

## Exact memory model and preflight

For one full base define:

    T       statements
    I       2 * T incidences
    Vref    active reference nodes
    Gref    reference-node groups
    Ginl    inline incoming groups
    P       predicates
    C       nonzero named-context IDs in the base snapshot

The sizing result must report:

    referenceLocatorBytes
    nodeHeaderBytes
    referenceRunBytes
    inlineRadixBytes
    inlineKeyBytes
    inlineRunBytes
    contextBytesIncludedInRuns
    contextCatalogBytes
    predicateDictionaryBytes
    arenaAlignmentBytes
    javaMetadataBytes
    totalBaseBytes
    buildCounterBytes
    buildContextDomainBytes
    encoderWorkingBytes
    capturedDeltaBytes
    peakBuildBytes
    bytesPerStatement
    bytesPerIncidence

At `P <= 256`, the approximate fixed reference-node terms are:

    locator page     actual occupancy cost; 5.15625 bytes/node only when full
    node counts      4 bytes/node in header
    group entry      6 bytes/group
    context catalog  8 bytes/named context plus segmented-table padding

Run bytes must come from the real codec sizer, not a guessed average.

Publication gates:

1. `totalBaseBytes <= steadyLimitBytes`, where the options resolver computed the exact 86% integer limit;
2. `peakBuildBytes <= effectiveMaxBytes`;
3. post-publication base plus captured/applied deltas plus retained snapshot arenas `<= maxBytes`;
4. every arithmetic operation uses `Math.addExact`/`multiplyExact` or an equivalent checked helper;
5. failure leaves the old index or EMPTY state unchanged.

For the requested 20-billion-statement deployment with an effective cap of exactly 256 GiB, acceptance requires:

    totalBaseBytes <= 220.16 GiB
    peakBuildBytes <= 256 GiB

on the representative production distribution. With AUTO, substitute the resolver's `steadyLimitBytes` and
`effectiveMaxBytes` for those two constants. If either fails, FULL adjacency does not fit this dataset under the given
limit. Use explicit SELECTED-predicate coverage or a larger budget; do not weaken the gate.

### Worked 20-billion budget worksheet

The implementer must include this worksheet in the sizing report rather than reporting only a total. With 40 billion
incidences and a 220.16-GiB steady target, the complete index has:

    5.9098750 bytes per incidence

before reserving build/update space. For the stated `P = 100`, suppose the production scan reports one billion active
reference nodes and two billion reference-node groups. The approximate fixed terms are:

    full-page locator refs/bitmaps/ranks    5.15625 GB  =  4.80 GiB
    four one-byte counts per node           4.00000 GB  =  3.73 GiB
    six bytes per reference group          12.00000 GB  = 11.18 GiB
    ---------------------------------------------------------------
    illustrative fixed subtotal            21.15625 GB  = 19.70 GiB

That leaves about 200.46 GiB, or 5.38 bytes/43.05 bits per incidence, for encoded neighbors, contexts, run headers,
inline-object indexes, sparse locator overhead, top arrays, alignment, and primitive metadata. This distribution may
fit if neighbor deltas are compact and contexts are usually null.

If instead the scan reports five billion active reference nodes and ten billion groups, the same three terms consume
about 98.52 GiB and leave only 3.27 bytes/26.1 bits per incidence before the other terms. That distribution is much less
likely to fit. These examples are not sizing promises; they show why `Vref`, `Gref`, inline keys, context cardinality,
and measured neighbor/context-ordinal block bit widths must appear beside the exact `totalBaseBytes`.

### Deterministic base allocation order

The sizing simulator and writer use exactly this allocation sequence in the one base arena:

1. base predicate dictionary/reverse array and base context raw-ID-by-ordinal array;
2. segmented top-level reference locator arrays, type order URI, LITERAL, BNODE, TRIPLE;
3. final locator pages and their node headers, type order above then ascending payload;
4. inline radix directories, block-reference arrays, key blocks, and per-key local-run-ref slots for nonempty planes
   only, ordered by predicate ordinal then explicit before inferred;
5. run payloads in scan order: outgoing explicit, incoming explicit, outgoing inferred, incoming inferred.

Metadata allocated in steps 1–4 is filled later in place but never resized. `LmdbAdjacencyArenaSizingPlan` simulates
eight-byte alignment and 1-GiB boundary tails for every allocation and records component start/end refs plus total
physical region capacities. Passes 2–3 repeat the same sequence and assert each component end ref and final
`capacityBytes`; a mismatch aborts publication. This is how the "exact" sizing claim remains executable rather than an
estimate. Every reserved field, record pad, alignment gap, and unused region tail inside the charged capacity is zero;
parallel and single-thread builds of the same snapshot are byte-for-byte identical.

## Base build algorithm

The build writes no files and must not require external sorting. It relies on existing LMDB index order.

Before Pass 0, enable commit capture, create/register the primary pinned LMDB read transaction, and stamp base revision
`B`. Pass 0 and the context DBI scan use that primary. The single-thread reference path uses it for Passes 1–3; the
parallel path uses the exact same-snapshot transaction family specified below. A map resize/version invalidation of any
member aborts the unpublished build. Do not reacquire a newer transaction between sizing and encoding.

### Required source orders

Prefer:

- `spoc` (or any subject, predicate, object, context-compatible order) for outgoing groups;
- `posc` for incoming groups, because it produces `(predicate, object)` groups and sorted subjects;
- explicit and inferred DBIs separately.

The default `spoc,posc` configuration is sufficient. If no configured index can stream one required grouping, do not
create an in-memory global sort of billions of statements. Mark that direction `NOT_BUILDABLE_WITH_INDEX_CONFIG` and:

- refuse activation for FULL; or
- in SELECTED mode, refuse activation if a selected predicate cannot be built in all four planes.

### Build work and restart-ETA contract

Let `T` be the number of statements at base revision `B`. Explicit and inferred DBIs partition the statement set, so
the four logical streams in one pass—outgoing/incoming × explicit/inferred—perform `2 * T` statement visits, not
`4 * T`. Pass 1 and Pass 3 together therefore perform:

    sourceStatementVisits = 4 * T

At `T = 20,000,000,000`, that is 80 billion LMDB cursor decodes, plus Pass 0 predicate seeks/context scan, Pass 2
header work, validation, and catch-up. The builder must expose these as separate counters/timers; “four scans” is never
reported as one dataset pass.

The deployment gate is a complete 20B build and publication in at most 12 hours on the target host:

    targetBuildNanos = 12 * 60 * 60 * 1_000_000_000
    minimum scan-only aggregate rate
      = 80,000,000,000 / 43,200
      = 1,851,851.852 source-statement visits/second

Because fixed work and catch-up also consume time, merely reaching that scan-only rate is insufficient; the measured
end-to-end duration is authoritative. Before the full run, execute Pass-1 and Pass-3 dry-run instrumentation over at
least 200 million representative statements per required source order, using the target JVM, LMDB storage, index
configuration, and build-thread count. Report:

    estimatedBuildSeconds =
        estimatedPass0Seconds
      + (2 * T / measuredPass1AggregateVisitsPerSecond)
      + estimatedPass2Seconds
      + (2 * T / measuredPass3AggregateVisitsPerSecond)
      + estimatedValidationAndCatchupSeconds

The pilot estimate must be at most ten hours, leaving two hours of operational margin, before scheduling the full
20B validation. The full measured publish must still meet 12 hours. Failure keeps rollout in `SHADOW`/LMDB and is a
design-gate failure, not a warning to accept. At the same sustained rates a comparable 5B rebuild should be about
one-quarter of the scan time. Every restart serves LMDB while this ephemeral rebuild runs.

### Pass 0 — predicate dictionary

Walk the leading predicate domain from `posc` in both explicit and inferred DBIs by seeking from one predicate-prefix
successor to the next; do not decode every statement in this pass. Merge the two unsigned streams and assign
unsigned-ascending ordinals without duplicates. Count predicates and choose the base predicate width. In FULL mode
include every predicate. In SELECTED mode resolve the configured IRI set through ValueStore before the scans, include
those raw IDs, and retain unresolved configured IRIs in `Coverage`. Value-ID retirement must keep those IDs stable for
the pinned TripleStore watermark; `posc` at `B` remains the authority for whether the predicate had statements.

In the same pass, scan `TripleStore.getContexts(pinnedTxn)`, skip raw context zero, and build the base context dictionary
in iterator order. `contexts` keys use the existing order-preserving unsigned Varint encoding, so no sort is needed.

### Pass 1 — exact sizing and node counts

For every included explicit/inferred outgoing and incoming group:

1. stream its already raw-ID-ordered `(rawNeighbor, rawContext)` pairs through `RunCodecSizer`, translating context to the
   base ordinal;
2. add exact run bytes;
3. set the reference node's build-page occupancy bit and increment the appropriate plane count once; or
4. for an inline object, add the exact inline key/run/radix accounting.

Use `long` counters. No pair is retained after the group sizer consumes it.

This pass also records maximum active reference payload per type and which inline-object planes exist. Do not perform a
separate full statement scan for those facts.

After the scans:

1. traverse active build pages in value-ID order;
2. calculate each final locator-page size;
3. calculate each node header from its four counts;
4. add dictionary, top-level locator arrays, alignment, and bounded Java metadata;
5. calculate peak bytes including the still-live build counters and worst captured commit delta;
6. reserve the complete base output before Pass 2.

If any gate fails, close the build transaction and counter arenas and leave the active index unchanged.

### Pass 2 — allocate headers

Allocate the base dictionary/catalog arrays, final locator pages, inline metadata, and node headers exactly. Copy the
temporary context domain into its base segment, verify it, switch the builder's catalog view to the base segment, and
release the temporary context array. For every active node:

1. copy its four plane counts into the header;
2. record the header's u40 reference in the final locator page;
3. reset the temporary four count cells to insertion cursors.

Build the final bitmap rank prefixes. The base is still unpublished.

### Pass 3 — encode runs and fill header entries

Repeat the four ordered scans. For each group:

1. translate each raw context through the base catalog and encode the complete run into the base run arena;
2. find the node's preallocated header and plane start;
3. use that plane's temporary cursor to choose the next entry;
4. write predicate ordinal and u40 run reference;
5. increment the cursor;
6. for inline objects, append the key/run pair to its already-sized plane index.

Predicate order from `spoc`/`posc` and unsigned dictionary order must make entries ascending without a sort. Assert in
development builds and verify in tests that every written ordinal exceeds the previous one in that plane.

At the end, verify:

- every cursor equals its Pass-1 count;
- every reserved output byte is either written or alignment padding;
- statement/incidence totals match the source scans;
- every nonzero context encountered resolves to one catalog ordinal and round-trips to the same raw ID;
- random and boundary rows decode identically to LMDB;
- the pinned transaction is still valid.

Close the build-counter arena. The base remains unpublished until catch-up succeeds.

### Parallel execution after the single-thread reference path

The byte format and single-thread builder are the oracle, but production construction is not left single-threaded.
After the Milestone-4A reference path is green, implement a bounded four-stream builder:

    buildThreads = min(configuredBuildThreads, nonemptyLogicalStreams)
    configuredBuildThreads default = min(4, Runtime.getRuntime().availableProcessors())
    legal configured range = 1..4

The four logical streams are outgoing explicit, incoming explicit, outgoing inferred, and incoming inferred. A stream
is never split in version 1; on the common explicit-only dataset two workers scan `spoc` and `posc` concurrently. This
preserves group order, bounds reader use, and normally reaches the LMDB/storage bandwidth limit without an edge-scale
merge structure.

Snapshot-family creation is exact:

1. create the primary pinned build transaction and revision `B`;
2. acquire the transaction manager read lock, which excludes commits and map-resize renewal;
3. re-read `dataRevision`, require `B`, and capture the `ValueStore` payload high-water value while this lock is held;
4. open one untracked read transaction per additional active worker, verify every `mdb_txn_id` equals the primary
   transaction ID, and stamp each sibling with `B`;
5. release the manager read lock immediately after the complete family exists;
6. confine each transaction/cursor pool to its worker thread and record its initial map/version token;
7. at existing cancellation/group boundaries, abort the whole unpublished build if any transaction ID, snapshot
   revision, or version token changes.

Do not share one LMDB transaction or cursor between workers, and do not hold the transaction-manager read lock for the
hours-long build. Commits may proceed after family creation; pinned readers retain `B`, capture supplies later
revisions, and map-resize renewal invalidates/aborts the family.

Pass 1 uses one shared build-node workspace. The package-private `ValueStore` payload high-water value captured in step
3 is `nextId` exclusive. It is an upper bound for every reference type at `B`, not a steady-index size. Allocate four
segmented build-only arrays of aligned u64 count-page references, each with `ceil(highWater / 4096)` slots; charge these
arrays explicitly. The final locator still uses the smaller per-type maximum active payload measured by Pass 1 and
compact u40 page references.

    buildPageDirectoryBytes =
        4 * ceilDiv(highWater, 4096) * 8
        + segment-directory bytes
        + region/alignment slack

All terms use checked arithmetic. For illustration, `highWater = 20,000,000,000` makes the leading array term
156,250,016 bytes (about 149.0 MiB), not an edge-scale object graph. If the exact charged term does not fit build
reserve, refuse before starting worker scans.

Install a count page under one of 256 fixed striped locks keyed by `(type, pageNo)`: recheck the u64 page-reference slot
inside the lock, allocate/zero exactly one page when absent, and publish its reference before unlocking. These locks are
constant Java metadata, not one object per node/page. Once installed:

- each logical stream is the sole writer of its plane-count cell, so count increments are ordinary width-correct
  u8/u16/u32 accesses;
- occupancy bitmap words use atomic bitwise OR because different plane workers can discover the same node;
- each worker keeps checked local run-byte/group/inline totals and per-type active-payload maxima;
- the coordinator joins workers, checked-reduces their totals/maxima, and only then reads bitmap/count bytes.

Pass 2 remains single-coordinator work and allocates the exact deterministic layout. The sizing plan records four
disjoint run-component ranges—including alignment and 1-GiB boundary tails—in the fixed order outgoing explicit,
incoming explicit, outgoing inferred, incoming inferred.

Pass 3 gives each stream worker a bounded writer over only its preallocated component range. The worker replays the
same per-group size/alignment state machine, writes run bytes at its deterministic local reference, and advances only
its plane's header cursor. Header plane ranges and inline plane indexes are disjoint. It may neither call the shared
append allocator nor cross its component end. After join, the coordinator verifies all four component end references,
header cursors, counts, context round trips, and source totals before catch-up/publication.

The build coordinator runs on `maintenanceExecutor`; a separate fixed `buildExecutor` exists only for these scan
workers. Delta apply, consolidation, catch-up publication, and lifecycle transitions remain serialized by the
coordinator. On first worker failure/cancellation, cancel the family, close every sibling and unpublished arena, retain
interrupt status, and publish nothing. Store close shuts down both executors and waits for worker ownership cleanup.

### Online catch-up

Install the complete commit collector before acquiring build snapshot `B`. Retain committed in-memory deltas with
revision `> B` while the build scans. After Pass 3:

1. apply deltas `B + 1` through current revision in strict order;
2. if any revision overflowed or is absent, discard the unpublished base and retry;
3. acquire the TripleStore transaction-manager read lock;
4. confirm no commit can interleave and the caught-up revision equals `dataRevision`;
5. publish one `LmdbAdjacencyPublishedState` containing the base, catalogs, and catch-up generations;
6. release the lock.

For a 20-billion-row initial load, the recommended operational path is a quiescent/offline in-memory build immediately
after loading. An online build is correct but its captured commits must fit the configured reserve for the entire build.
If they do not, it aborts rather than writing a spill file.

## Commit delta collection

Do not change `LmdbCsrCommitDelta` into an unfiltered 20-billion-scale collector. Add
`LmdbDirectAdjacencyCommitDelta`, owned by `TripleStore` only while direct adjacency is enabled:

    final class LmdbDirectAdjacencyCommitDelta {
        void begin(long startRevision, long maxBytes);
        void recordAdd(long subject, long predicate, long object, long context, boolean explicit);
        void recordRemove(long subject, long predicate, long object, long context, boolean explicit);
        SealedDirectDelta seal(long committedRevision);
        void reset();
        boolean overflowed();
    }

Tee both collectors inside `recordFanOutAdded` and `recordFanOutRemoved`. That is the single mutation choke point and
already excludes writes that did not change the statement set.

The direct collector is derived-state best effort: reservation/allocation failure sets `overflowed` and makes later
record calls no-ops; it must not fail the authoritative statement mutation. Argument/invariant bugs still fail fast in
tests, but an exhausted adjacency budget becomes the revision-gap path at commit.

In FULL mode capture every event. In SELECTED mode consult an atomically published, sorted primitive raw-predicate
decision table: `SELECTED` captures, `UNSELECTED` skips, and `UNKNOWN` captures conservatively. The maintenance thread
classifies UNKNOWN IDs by resolving their IRI and copy-publishes the decision table. The writer never performs a
ValueStore lookup or allocates while classifying.

Store events in primitive off-heap column pages, not growable Java arrays:

    subject[capacity]         u64
    predicate[capacity]       u64
    object[capacity]          u64
    context[capacity]         u64
    flags[capacity]           u8       // explicit and add

`flags` bit 0 is explicit and bit 1 is add; bits 2–7 are zero.

Use capacities 1,024, 8,192, then 65,536 events for every later page. A full regular page is 2,162,688 bytes. Keep
primitive page-reference/capacity arrays in the collector; never one Java object per page or event. `eventCount` is
long, and the event ordinal is also the transaction sequence used for last-operation-wins. The fixed 2-GiB absolute
collector ceiling below guarantees a sealed delta has fewer than 2^32 events, so radix-sort scratch may use charged
off-heap u32 event ordinals without narrowing a valid commit.

Charge page capacity, sorting scratch, and touched-row storage before growth. Resolve two distinct limits:

    defaultCommitMaxBytes =
        min(64 MiB, max(8 MiB, floor(maxAdjacencyBytes / 1000)))

    absoluteCommitMaxBytes =
        min(2 GiB, max(8 MiB, floor(maxAdjacencyBytes / 100)))

The system property selects `commitMaxBytes`. Unset uses `defaultCommitMaxBytes`; an explicit positive value may lower
or raise the default but must not exceed `absoluteCommitMaxBytes`. Reject zero-as-explicit, negative, or over-ceiling
values during options construction. At the 256-GiB target the default is 64 MiB and the opt-in absolute ceiling remains
2 GiB. This deliberately makes the ordinary writer-stall envelope much smaller while retaining a measured escape
hatch for unusual bulk transactions.

Before accepting an event, project the worst seal peak—event pages, two row tokens per event in two u64 radix buffers,
and two 24-byte unique row entries per event—plus page/alignment slack. The leading approximation is `33 + 32 + 48 =
113` bytes/event; therefore 64 MiB can contain at most 593,883 events even before slack is charged. Mark overflow
before the exact projected peak exceeds `commitMaxBytes`; do not interpret the limit as event-page bytes alone. There
is no disk spill. When the next growth would exceed it:

1. set `overflowed`;
2. release event pages as soon as touched-state safety permits;
3. retain the fact that this logical commit is a gap;
4. at commit, make the active index invalid for the overflowing revision and all later revisions;
5. schedule a clean rebuild.

An overflowed transaction may still commit to LMDB. It only disables acceleration.

`seal` transfers ownership to an immutable `SealedDirectDelta`; `TripleStore` installs a fresh collector before the next
write transaction. The sealed object owns its event arena until apply completes or the index enters a gap. Rollback
closes the unsealed arena. The background applier must never observe pages that the writer can reset or reuse.

Prepare/seal the event pages and touched table before calling the authoritative LMDB commit while the existing
transaction-manager writer exclusion is held. If LMDB commit fails, close the sealed direct delta and publish neither
pending state nor revision. If sealing overflowed, or if pending-state publication fails after a successful LMDB
commit, the writer must still publish an allocation-free global gap marker
through the preallocated `AtomicLong emergencyGapFromRevision` before advancing `dataRevision`. Update it with a CAS
minimum loop and no lambda/allocation. It makes every row at that revision and later fall back even if immutable-state
allocation failed. Never continue with a partial touched table.

In SELECTED mode, a commit containing only predicates already classified UNSELECTED has no covered-row effects. Under
`publicationLock`, copy-advance `appliedRevision` to `nextRevision` without a generation before advancing
`dataRevision`. If conservatively captured UNKNOWN events later classify as entirely unselected, the applier performs
the same no-generation advance while atomically removing their pending entry. Revision continuity is explicit even
when no adjacency bytes change.

### Touched rows and commit ordering

Each event affects:

    outgoing row = (subject, predicate, outgoing plane for explicit flag)
    incoming row = (object, predicate, incoming plane for explicit flag)

Before `dataRevision` advances, publish a `PendingDirectDelta` with a sorted, deduplicated off-heap row-key table and the
target revision. This happens in the existing commit critical section through a new listener:

    interface DirectAdjacencyCommitListener {
        void beforeRevisionBump(SealedDirectDelta delta, long nextRevision);
    }

The table entry is:

    u64 rawKey
    u64 rawPredicateId
    u8 plane
    u8[7] reserved

Entries are sorted by unsigned `(rawKey, plane, rawPredicateId)`. Pending data deliberately uses raw predicate IDs, not
ordinals: a commit can mark rows before an ordinal exists, and a read already has the raw predicate ID needed for the
binary search. No unresolved-marker side table is required.

Every nonempty `PendingDirectDelta` also stores these final primitive fences beside its table owner:

    u64 rowCount
    u64 minRawKey
    u64 maxRawKey
    u8 planeMask
    u8[7] reserved

`minRawKey`/`maxRawKey` are the first/last table keys in unsigned order, and bit `plane` is set in `planeMask` when the
table contains that plane. These are table metadata, not repeated per entry. A bound-row check first rejects
`revision > S`, then rejects an unsigned raw key outside the fence, then rejects a missing plane bit; only then may it
binary-search the 24-byte entries. An unbound-predicate node check uses the same fences and lower-bounds
`(rawKey, plane, 0)` only when they pass. The metadata is validated against the first/last entry at seal time and is
charged with the pending owner. An empty covered commit publishes no pending table and uses the no-generation revision
advance.

Build the table during `seal`, before revision publication:

1. append two u64 tokens per event, `(eventOrdinal << 1) | directionBit`;
2. LSD-radix-sort the tokens by derived unsigned `(rawKey, plane, rawPredicateId)`, reading fields from event pages;
3. stream-deduplicate equal rows into the exact 24-byte table;
4. close both token sort buffers before publishing pending state.

`commitMaxBytes` is a cap over the event pages, both token buffers, final pending table, and later apply scratch—not
merely the event columns. Reserve the peak transition before allocating it. If it does not fit, use the allocation-free
global gap path. The radix key is 17 bytes (`rawKey` u64, `plane` u8, `rawPredicateId` u64), so sealing performs 17 LSD
byte passes with derived-key reads from the event columns. Record seal duration, event count, unique-row count, and
bytes because this exact work runs inside writer exclusion. Token buffers and the 24-byte table use segmented primitive
accessors, so their total count is long even though each radix pass operates on bounded slabs.

Do not add a mid-sort timeout: abandoning a partially sealed transaction cannot recover touched-row proof. Bound the
stall with `commitMaxBytes` and enforce the measured rollout gate in Milestone 9. Bulk loading at the target scale must
occur before direct adjacency is ACTIVE unless its configured cap has passed that gate. When a completed seal exceeds
`sealWarnMillis`, increment `sealWarnings` and emit one rate-limited operational warning; the warning never changes
authoritative commit success or clears pending evidence.

Visibility ordering is load-bearing:

    seal event pages and touched table
      -> authoritative LMDB commit
      -> atomically publish state with PendingDirectDelta
      -> volatile dataRevision update
      -> readers may observe revision
      -> build immutable DeltaGeneration if rows changed
      -> atomically publish one state containing:
           appended PredicateCatalog when needed
           appended ContextCatalog when needed
           appended DeltaGeneration when at least one row changed
           appliedRevision advanced
           pending marker removed

A reader at the new revision checks pending rows before using an older version. A row not present in any pending delta
between the index publication revision and its snapshot is unchanged and may safely use that older version.

The existing `TxnManager` writer exclusion must cover LMDB commit, pending-or-gap publication, `dataRevision` update,
and transaction reset/release. Consequently a newly pinned LMDB reader cannot observe the committed database image
while receiving a pre-pending revision/state pair.

If an apply task throws before publication while its sealed events and exact pending table remain intact, close only
the unpublished output, leave the pending marker in place, and transition to `APPLY_STALLED`. Bound rows absent from
all pending tables remain provably usable; touched rows and all native kernels fall back. Schedule a quiescent rebuild.
Use `DEGRADED_GAP` only when event/touched information was lost or published bytes are untrustworthy. Do not clear
pending in a `finally` block.

## Applying a committed delta

Process sealed commits strictly by revision on one adjacency-applier executor. Later commits may queue but cannot
publish before earlier ones.

Before sorting a commit, collect its previously unseen raw predicate and nonzero context IDs. Sort each unsigned and
prepare copy-appended `PredicateCatalog` and `ContextCatalog` objects. Reserve the new generation's arena slot in the
prospective `ArenaCatalog`; the new context raw-ID segment lives in that arena. All three catalogs remain unpublished
until the complete generation validates.

Use two directional passes over the same sealed event pages rather than duplicating every event:

1. outgoing sort key:
   `(subject unsigned, plane, predicate ordinal, object unsigned, context unsigned, sequence)`;
2. incoming sort key:
   `(object unsigned, plane, predicate ordinal, subject unsigned, context unsigned, sequence)`.

Use an LSD radix sort over off-heap u32 event-ordinal buffers, dereferencing fields from the sealed column pages. Its
scratch capacity is charged to `PENDING` before apply. Within equal statement identity, keep the greatest event
ordinal. Do not allocate comparator objects or boxed row keys.

Allocate three `eventCount * u32` segmented buffers. Sort outgoing into the first using the second as scratch; then
reuse the second and third to sort incoming, leaving one final ordered buffer per direction plus one reusable scratch
buffer. Keep the two finals through sizing and encoding, then release all three. Refuse before allocation if their
charged peak exceeds the sealed commit allowance.

Apply has two streaming subpasses over the sorted mutation ordinals. The first resets old-run cursors as needed, proves
which rows change, computes exact replacement/chunk/directory bytes, simulates arena boundaries, and reserves the
complete generation plus row directory. The second repeats the same merges and encodes into that fixed arena. Retain
the sorted mutation ordinals between subpasses; retain no decoded old row. Assert predicted versus emitted row counts,
component refs, and bytes before publication.

Generation arena order is: predicate extension metadata, context raw-ID extension segment, fixed row directory, then
replacement run/chunk payloads in row-directory key order. The sizing plan and encoder use this same sequence.

For each row at revision `R`:

1. locate its complete state at `R - 1` through the internal snapshot lookup;
2. merge the sorted changes with the existing sorted run;
3. if the final set equals the old set, emit no row version;
4. otherwise emit one complete replacement run/chunk directory into a new delta arena, or a zero-ref tombstone row
   entry;
5. append one sorted row-directory entry for that net-changed row.

The merge is idempotent:

- present + add -> present once;
- absent + add -> present once;
- present + remove -> absent;
- absent + remove -> absent.

For an ordinary run, stream the old decoder and mutation iterator into the new encoder; do not materialize the old
degree in two `long[]` arrays. For a chunked group, route mutations by range and rewrite only affected chunks plus the
directory.

Publish one immutable `DeltaGeneration` only after both directions are complete:

    final class DeltaGeneration {
        long revision();
        long rowCount();
        long allocatedBytes();
        int arenaSlot();
        long find(long rawKey, int plane, int predicateOrdinal); // packed handle, NO_VERSION, or ROW_TOMBSTONE
        LmdbAdjacencyArena arena();
    }

The row directory is sorted by unsigned `(rawKey, plane, predicateOrdinal)` and has fixed 24-byte entries:

    u64 rawKey
    u32 predicateOrdinal
    u8 plane
    u8 flags                  // bit 0 tombstone
    u16 reserved
    u40 runRef
    u8[3] padding

`find` uses an allocation-free binary search. A generation contains at most one version per row and only for rows whose
final set differs from `R - 1`. The stored u40 is local to this generation; `find` combines it with `arenaSlot()` to
produce the opaque direct run handle.

Generation objects do not own independent read leases. Their enclosing immutable state retains the arena catalog, and
the catalog owns the arena lifetimes. During construction the builder owns the new arena; successful state publication
transfers that ownership to the new catalog, while failure closes the unpublished catalog/arena.

Generation publication and pending-marker removal are one `LmdbAdjacencyPublishedState` swap. They must not be separate
atomic writes. If every captured mutation is net-neutral or unselected, publish no generation: atomically advance
`appliedRevision` and remove pending, and discard prospective predicate/context catalog segments that no output run
needs. Coverage classification may still publish as ordinary Java metadata.

## Overlay set and consolidation

An `OverlaySet` contains:

    immutable PredicateCatalog
    immutable ContextCatalog
    immutable ArenaCatalog
    optional ConsolidatedOverlay
    zero to eight DeltaGeneration objects, revision ascending
    highest continuous applied revision

Lookups inspect applicable delta generations newest first, then the consolidated overlay, then base.

`OverlaySet` has a small owner refcount because pending-only published states can share it. One overlay owner reference
owns one `ArenaCatalog`; releasing the last overlay reference closes that catalog, which releases its arena-owner
references. `PendingCommitSet` follows the same pattern for pending-table arenas. This two-level ownership avoids
incrementing hundreds of arena counters for every pending-only state copy.

Trigger consolidation before adding a ninth delta generation, when delta bytes exceed 5% of base bytes, or when any
row-version depth exceeds eight. Let:

    W = TxnManager.minPinnedSnapshotRevision()

If no pinned snapshots exist, treat `W = Long.MAX_VALUE`.

K-way merge sorted row directories. For each row retain:

- every version with revision `> W`; and
- the newest version with revision `<= W`, if one exists.

Those versions answer every active pinned snapshot `S >= W` and all future snapshots. Copy every retained ordinary run
and chunk directory into a fresh consolidation arena. Copy every reachable non-base chunk as well, rewrite its
directory slot/reference, and deduplicate copies by old `(arenaSlot, localRunRef)`. Base chunks may remain slot-zero
references because the base arena is retained for the index lifetime. The output catalog is then only `[base,
consolidation]`, so old delta arenas can be reclaimed after old read views close.

Also copy every live context-extension raw-ID array into the consolidation arena and rewrite its segment arena/ref
without changing `firstOrdinal` or segment boundaries. Runs therefore keep the same context ordinals.

The consolidated row directory is 24 bytes:

    u64 rawKey
    u32 predicateOrdinal
    u8 plane
    u8 flags
    u16 versionCount          // 0xffff means extended u32 count table
    u40 versionListRef
    u8[3] padding

Each version-list entry is 16 bytes:

    u64 revision
    u40 runRef
    u8 flags                  // tombstone
    u16 reserved

The `versionListRef` and every version-entry `runRef` are local to the consolidation arena; any cross-arena chunk
ownership remains explicit inside a chunk directory. Versions are revision-descending. Consolidation writes and
verifies the complete output before swapping `LmdbAdjacencyPublishedState`. The old state and its arenas close only after
their last `AdjacencyReadView` lease closes.

If consolidation output plus live old generations would exceed the reserve, do not start it. Publish a
`UNAVAILABLE` state so new/latest reads fall back, record maintenance reason `MEMORY_REFUSED`, transition to
`QUIESCING_FOR_REBUILD`, and perform the no-overlap rebuild protocol below. There is no second, undocumented policy
choice.

## Snapshot isolation

### Read-view acquisition

Add to `LmdbSailDataset`:

    private final AdjacencyReadView adjacencyView;

For a SNAPSHOT dataset:

1. create/register the pinned LMDB transaction with `TxnManager.createReadTxnPinned`;
2. while that transaction remains registered, call `directAdjacencyStore.acquire(snapshotRevision)`;
3. store either a retained exact view or a lightweight fallback view;
4. on close, release `adjacencyView` before closing the LMDB transaction.

Registration before acquisition matters. Base replacement/consolidation checks
`minPinnedSnapshotRevision()` before dropping history, so it cannot retire a needed view in that interval.

`acquire` reads one `AtomicReference<LmdbAdjacencyPublishedState>`, never independent overlay and pending references:

    repeat up to 3 times:
        state = published.get()
        if state.tryRetain() is false:
            continue
        emergencyGap = emergencyGapFromRevision.get()
        if published.get() != state:
            state.release()
            continue
        if emergencyGap <= snapshotRevision:
            state.release()
            return fallbackView(snapshotRevision, REVISION_GAP)
        return new AdjacencyReadView(snapshotRevision, state)
    return fallbackView(snapshotRevision, STATE_CHURN)

The equality check gives acquisition a clear linearization point and prevents a close/rebuild transition from admitting
a late reader. Falling back after bounded contention is always correct. The retained state contains the matching base,
predicate catalog, context catalog, arena catalog, overlays, pending rows, coverage, and gap marker; a view never
combines pieces from different publications.

`emergencyGapFromRevision` is the sole deliberate extra read. Reading it before the final state-identity check is
load-bearing: a rebuild publishes a new state before clearing the marker, so a reader cannot combine an old state with
the cleared marker. The commit writer stores the marker before the volatile
`dataRevision` update, so a transaction stamped at or after the failed revision cannot legally miss it. A successful
rebuild publishes its new state first and clears the emergency marker second.

Parallel siblings retain the same `AdjacencyReadView`; they do not acquire the latest one. Preserve the existing
`mdb_txn_id` equality guard and also require the same `LmdbAdjacencyPublishedState` identity.

For SERIALIZABLE, keep adjacency ineligible. For ordinary resettable transactions, acquire a latest read view when a
probe or native kernel binds and close it with that object.

Map resize may renew and invalidate a pinned LMDB transaction. Every dataset read entry point calls `ensureSnapshot()`
before adjacency use. On version mismatch, throw the existing retryable error; never continue on adjacency alone.

### Base and generation intervals

An in-memory base built at revision `B` can serve only `S >= B`. The overlay set must contain a continuous applied path
from `B + 1` through its `appliedRevision`, except rows proven untouched by pending commits.

For `(row, S)`:

1. if mode/coverage/isolation is ineligible, return `NOT_COVERED`;
2. if `S < baseRevision`, return `NOT_COVERED`;
3. if `gapFromRevision <= S`, return `NOT_COVERED`;
4. for every pending commit with `revision <= S`, binary-search its touched-row table:
   - touched -> `NOT_COVERED`;
   - absent from all -> row is unchanged beyond `appliedRevision`;
5. inspect non-consolidated generations with `revision <= S`, newest first;
6. inspect the consolidated version list for newest `revision <= S`;
7. row tombstone -> `NOT_FOUND`;
8. otherwise inspect the base node/inline index;
9. base absence for a covered predicate/plane -> `NOT_FOUND`.

Map the raw predicate ID through the retained state's `PredicateCatalog` before generation/base search. If it is absent
and no applicable pending table contains that raw predicate, FULL—or SELECTED for a configured predicate—proves the row
empty. If a pending table contains it, step 4 already returned `NOT_COVERED`.

`NOT_FOUND` is exact empty. `NOT_COVERED` means invoke LMDB on the same transaction.

### Kernel completeness rule

A bound single-row `RecordIterator` may use pending-row proof. A native kernel can touch keys not known at bind time and
cannot safely restart after emitting partial output. Therefore `NativeProbe.adjacency(predicate, direction)` may return
the direct view only when:

    snapshotRevision <= overlaySet.appliedRevision
    no gap <= snapshotRevision
    the complete predicate/direction plane is covered

Otherwise it may use the current fully materialized CSR view or decline to the ordinary native/LMDB strategy.

### Old snapshots during rebuild

A published `LmdbAdjacencyPublishedState` is reference-counted. Rebuild never closes its owners while any
`AdjacencyReadView` uses it. To honor the hard cap without holding two 220-GiB bases:

1. enter `QUIESCING_FOR_REBUILD`;
2. make new/latest acquisitions return fallback;
3. let existing exact views finish;
4. stop applying new direct generations and keep only bounded commit capture;
5. when the old published-state lease count reaches zero, close all base/overlay arenas;
6. acquire replacement snapshot `B`; only after it is pinned, close pending/event owners with revision `<= B` because
   that LMDB image contains them;
7. build the replacement from LMDB and retain captures `> B`;
8. publish only after catch-up is continuous.

If commit capture overflows while waiting, record a gap and build from a newer snapshot after quiescence. Query
correctness remains LMDB-backed.

This deliberately chooses temporary loss of acceleration over allocating two complete bases.

## Lookup algorithms

Do not reuse the public `NOT_FOUND` sentinel to mean "this overlay has no version." Internal overlay search uses:

    NO_VERSION    = -3L
    ROW_TOMBSTONE = -4L

Only the complete row resolver translates `ROW_TOMBSTONE` to public `NOT_FOUND`.

Reference lookup is structurally:

    long findReferenceRun(AdjacencyReadView view, long rawKey, int plane, int predicateOrdinal) {
        long overlaid = view.overlays().find(rawKey, plane, predicateOrdinal, view.snapshotRevision());
        if (overlaid == ROW_TOMBSTONE) {
            return NOT_FOUND;
        }
        if (overlaid != NO_VERSION) {
            return overlaid;
        }

        if (!ValueIds.isReference(rawKey)) {
            return NOT_COVERED;
        }
        long payload = ValueIds.getValue(rawKey);
        long pageRef = view.base().locator().pageRef(type(rawKey), payload >>> 12);
        if (pageRef == 0 || !occupied(pageRef, payload & 0xfff)) {
            return NOT_FOUND;
        }
        long headerRef = rankedHeaderRef(pageRef, payload & 0xfff);
        return findHeaderEntry(headerRef, plane, predicateOrdinal);
    }

The implementation does not create a `RowVersion` object; the pseudocode fixes ordering and meaning.

Bound probe flow:

1. reject direct adjacency when disabled, write transaction open, IDs unknown, isolation ineligible, or no exact view;
2. route reference key to node locator or inline object to `InlinePlaneIndex`;
3. check gap and pending touched rows;
4. if context is bound and nonzero, map raw context through the view catalog; catalog absence now proves no match;
5. find delta/consolidated/base run;
6. `NOT_COVERED` -> existing `TripleStore.getTriples(txn, ...)`;
7. `NOT_FOUND` -> reusable empty iterator;
8. run handle -> reusable `LmdbDirectAdjacencyIterator`.

### Bound-node, unbound-predicate enumeration

Serve an unbound predicate only when the key is a reference ID and coverage is FULL. SELECTED cannot answer `?predicate`
completely, and inlined objects do not have node headers, so both cases fall back before emitting a row.

For reference `(rawKey, plane, ?predicate, S)`:

1. apply the same base-revision, gap, isolation, and map-version checks as a bound predicate;
2. for every pending revision `<= S`, range-seek `(rawKey, plane, *)` in its sorted table; any match makes the complete
   operation `NOT_COVERED`;
3. create one cursor for the base header's sorted plane entries, one for the consolidated row-key range, and one for
   each applicable delta generation's row-key range;
4. k-way merge predicate ordinals using fixed primitive cursor slots; for one ordinal, choose the newest version
   `<= S`, skip a row tombstone, or use the base entry when no version exists;
5. map the ordinal back to raw predicate ID through the retained catalog and iterate its run before advancing to the
   next group.

`NodeGroupCursor` owns fixed arrays sized `2 + maxDeltaGenerations` and one reusable quad; it allocates neither per
predicate nor per edge. All eligibility/pending checks finish before the first result, so it never needs a mid-stream
LMDB fallback. This implements ArcadeDB-like adjacency enumeration for one node while leaving predicate-wide key/root
enumeration to CSR or LMDB.

Group order is catalog-ordinal order. A post-build predicate receives an appended ordinal and therefore does not
preserve unsigned raw-predicate order. If the caller requires a predicate-leading result order, decline before emitting
and use LMDB; unordered graph-pattern evaluation may use the node iterator.

`LmdbDirectAdjacencyIterator` mirrors `LmdbCsrRunIterator`:

- one reusable four-long `quad`;
- `long position` and `long end`, never global `int`;
- no allocation per row;
- context-ordinal decoding through the retained catalog and exact raw-context filtering;
- batch `fill(long[], int)`;
- `seekForward` through codec lower bound;
- `close` releases no arena independently because its owning read view outlives it.

The run is always unsigned `(rawNeighbor, rawContext)` ordered. An ordered request whose selected LMDB index requires
context before neighbor must fall back.

## Native adjacency API migration

The existing `NativeAdjacency` uses a dense `int` and global int run offsets. It cannot represent a supernode or
predicate plane beyond `Integer.MAX_VALUE` incidences. Replace its internal contract in
`NativeLmdbQuerySource.java`:

    interface NativeAdjacency {
        long NOT_FOUND = -1L;
        long NOT_COVERED = -2L;

        long find(long key);
        long size(long runHandle);
        long neighborAt(long runHandle, long runOffset);
        long contextAt(long runHandle, long runOffset);

        default int copyNeighbors(long runHandle, long runOffset, int length,
                long[] target, int targetOffset) { ... }

        default int copyContexts(long runHandle, long runOffset, int length,
                long[] target, int targetOffset) { ... }

        default long lowerBound(long runHandle, long fromOffset, long neighbor, long context) {
            return -1L;
        }

        default boolean supportsKeyEnumeration() {
            return false;
        }

        default long keyCount() {
            return -1L;
        }

        default long keyAt(long keyOrdinal) {
            throw new UnsupportedOperationException();
        }

        default boolean runsNeighborOrdered() {
            return false;
        }
    }

`CsrNativeAdjacency` adapts without changing `CsrEntry`: encode `dense + 1L` as its run handle, derive the existing int
start/end after checked narrowing, and retain key enumeration. `DirectNativeAdjacency` uses the packed
`arenaSlot + u40 localRunRef` handle and its read view's arena catalog. It does not support predicate-wide key
enumeration in version 1.

Update interpreted and generated consumers:

    old:
        int dense = a.denseIdOf(key);
        for (int p = a.runStart(dense); p < a.runEnd(dense); p++) ...

    new:
        long run = a.find(key);
        if (run != NativeAdjacency.NOT_FOUND) {
            long n = a.size(run);
            for (long p = 0; p < n; p++) ...
        }

Vectorized consumers copy in `int`-sized chunks:

    int batch = (int) Math.min(remaining, targetCapacity);
    a.copyNeighbors(run, runOffset, batch, target, 0);

Audit and update:

- `LmdbNativeJaninoAggregate`;
- `LmdbNativeJaninoPipeline`;
- `LmdbNativeKernelIrEmitter`;
- property-path implementations;
- generated benchmark kernels;
- every `denseIdOf`, `runStart`, `runEnd`, `neighborAt`, `contextAt`, `copyRun`, and `keyCount` call found by `rg`.

Root enumeration still requires `supportsKeyEnumeration()`. Direct adjacency version 1 returns false, so unbound
`?s p ?o` roots continue through LMDB or the existing whole-predicate CSR. Chained bound expansions use direct
adjacency. This is deliberate: a predicate roster would duplicate one key per non-empty group and consume budget that
does not improve the Arcade-style vertex-to-neighbor path.

This limitation requires explicit workload acceptance at the target scale. Before `PREFER`, produce a root-workload
report from the representative query mix containing:

- percentage of query executions containing predicate-bound, subject/object-unbound roots;
- percentage of total query wall time spent in those LMDB root scans;
- LMDB root-scan p50/p95/p99 latency and rows scanned for the dominant predicates;
- direct-adjacency hit rate and time saved in the downstream bound expansions;
- whether a bounded legacy CSR is retained for any root and the bytes subtracted from the direct cap.

The deployment owner must record this exact sign-off with name/date:

    ROOT_ENUMERATION_ACCEPTED:
    Direct adjacency v1 accelerates node-bound expansion only. Predicate-root enumeration remains LMDB/explicitly
    budgeted CSR, and the measured target workload is acceptable without an in-memory predicate key roster.

Without that sign-off, the target remains `SHADOW`; lack of key enumeration is not silently treated as an accepted
20B workload. If the report is unacceptable, a separately sized predicate-roster design is required before `PREFER`.

## Store integration

Add an internal provider boundary so query code does not grow two sets of ad-hoc conditions:

    interface LmdbAdjacencyProvider extends AutoCloseable {
        AdjacencyReadView acquire(long snapshotRevision);
        RecordIterator tryOpen(AdjacencyReadView view,
                long subject, long predicate, long object, long context, boolean explicit);
        NativeLmdbQuerySource.NativeAdjacency adjacency(AdjacencyReadView view,
                long predicate, boolean bySubject, boolean explicit);
        OptionalLong exactDegree(...);
        OptionalDouble meanFanOut(...);
        int tryHas(...);       // -1 decline, 0 false, 1 true
        long tryCount(...);    // -1 decline
        AdjacencyMetrics snapshotMetrics();
    }

`LmdbDirectAdjacencyStore` implements it. Keep `LmdbCsrAdjacencyCache` intact during migration and use this arbitration:

1. bound node with predicate bound: direct provider first, CSR second, LMDB last;
2. bound reference node with predicate unbound: direct node iterator first only under FULL coverage, otherwise CSR/LMDB;
3. full native adjacency for a chained kernel: direct when complete for snapshot, CSR second;
4. root/key enumeration: CSR then LMDB;
5. ordered full scans/partitions: existing CSR or LMDB;
6. count/has/exact degree: direct first when row covered;
7. global mean fanout/ordered integer domain: direct per-plane statistics only when complete, otherwise existing source.

At construction in `LmdbSailStore`:

    directAdjacency = config.mode != DISABLED
        ? new LmdbDirectAdjacencyStore(tripleStore, storeTxnStarted, config)
        : null;

Register the direct commit listener independently of `csrCache.commitListener()`. In `close`, stop build/apply/consolidate
executors, prevent new acquisitions, wait for active views while emitting rate-limited warning logs, and close every
arena before closing TripleStore. The warning interval is bounded; the safety wait is not converted into a force-close
timeout. A leased arena must never be closed underneath a reader.

`TripleStore` keeps separate CSR and direct listener/delta fields; do not overload the predicate-filtered
`LmdbCsrCommitDelta`. Its transaction lifecycle is:

1. `startTransaction`: begin both configured collectors;
2. each genuine mutation: tee to fan-out stats, CSR collector, and direct collector;
3. before the first LMDB commit attempt in `endTransaction(true)`: seal direct events/touched rows;
4. after the final triple LMDB commit/reset but under the TxnManager write lock: call both `beforeRevisionBump`
   listeners, then increment `dataRevision`;
5. rollback or LMDB commit failure: close/reset both uncommitted deltas;
6. expose `drainDirectAdjacencyCommitDelta()` alongside the existing CSR drain.

The current `updateFromCache()` only replays already-recorded `Record` values into LMDB and does not call
`recordFanOutAdded/Removed`, so step 3 includes the record-cache/map-growth path. Add an assertion that the collector is
sealed before replay and a forced-map-growth regression test proving no event is added after sealing.

`LmdbSailStore` drains/enqueues the sealed direct delta only after triple store, value store, and fresh-value publication
all succeed. This matters because maintenance may need to resolve a newly introduced predicate IRI. If a later
authoritative-store step fails after TripleStore already advanced its revision, call
`directAdjacency.markGap(committedRevision)` and retain/finally close the sealed delta; never clear the pending marker
as if the adjacency generation had applied.

Pass the dataset's `AdjacencyReadView` through `CsrProbeSupport`'s replacement provider support, count/has, exact-degree,
ordered-domain, and parallel siblings. Preserve `storeTxnStarted` bypass.

## Configuration

Add public enums in `org.eclipse.rdf4j.sail.lmdb.config`:

    enum DirectAdjacencyMode {
        DISABLED,
        SHADOW,
        PREFER
    }

    enum DirectAdjacencyCoverage {
        FULL,
        SELECTED
    }

Add to `LmdbStoreConfig`, export/parse through `LmdbStoreSchema`, and test:

    DirectAdjacencyMode directAdjacencyMode = DISABLED
    DirectAdjacencyCoverage directAdjacencyCoverage = FULL
    Set<IRI> directAdjacencyPredicates = Set.of()
    long directAdjacencyMaxBytes = 0       // AUTO: 50% of effective -Xmx
    boolean directAdjacencyBuildOnStart = false

Schema IRIs:

    directAdjacencyMode
    directAdjacencyCoverage
    directAdjacencyPredicate              // repeat once per selected IRI
    directAdjacencyMaxBytes
    directAdjacencyBuildOnStart

Validation:

- zero means AUTO and is valid in every mode;
- a positive explicit value below 256 MiB is rejected;
- negative sizes fail configuration;
- unknown enum labels fail parsing;
- FULL requires an empty predicate selection;
- SELECTED requires at least one predicate IRI;
- copy the selection on set/get and export IRIs in lexical order for deterministic configuration;
- build-on-start with DISABLED is rejected;
- defaults export nothing and preserve existing behavior.

Use:

    Set<IRI> getDirectAdjacencyPredicates();                 // unmodifiable copy
    LmdbStoreConfig setDirectAdjacencyPredicates(Collection<? extends IRI> predicates);

Reject null collections/elements and non-IRI RDF configuration values. RDF config parsing accepts repeated
`directAdjacencyPredicate` statements and deduplicates equal IRIs.

At store construction, `LmdbDirectAdjacencyOptions` resolves AUTO with
`(Runtime.getRuntime().maxMemory() >>> 1) & ~7L` and records both requested and effective values.
If a non-disabled mode resolves below 256 MiB, keep the store usable through LMDB, set direct adjacency to
`MEMORY_REFUSED`, and issue one clear startup warning. Do not silently clamp the result upward. All 86% steady-state
and 100% peak gates use the immutable effective value. Implement percentage calculations with quotient/remainder
integer arithmetic so multiplying a very large byte limit cannot overflow.

`SHADOW` builds and performs sampled result comparisons but never answers a query. `PREFER` answers when covered and
falls back otherwise. A package-private `REQUIRED` test mode may make `NOT_COVERED` fail tests; do not expose it as a
production setting.

SELECTED is deliberately explicit, not heuristic. Resolve configured predicate IRIs through ValueStore while the
builder's TripleStore snapshot is pinned and build all four planes for every resolved predicate. ID-retirement
watermarks keep those references stable. Keep unresolved configured IRIs in the coverage definition. When a commit
first introduces one, the applier resolves the previously unseen raw predicate ID, proves that it was absent at the
base revision, appends it to the predicate catalog, and merges the captured first commit against empty rows. If absence
cannot be proved, record a gap and rebuild; do not read a later LMDB image into an older generation. Known unselected
raw predicates are skipped by the collector; an unseen raw predicate is captured until the maintenance thread
classifies it, so selection never causes a false negative. A probe for an unselected predicate returns `NOT_COVERED`,
never `NOT_FOUND`.

The absence proof is concrete: the complete Pass-0 `posc` predicate-domain scan contains no raw ID, and either the
configured IRI was unresolved at `B` or it resolved to that same absent raw ID. Store this base-domain membership fact
in the immutable predicate catalog; do not infer it from the current ValueStore alone.

Low-level tuning remains system properties collected once into immutable `LmdbDirectAdjacencyOptions`:

    rdf4j.lmdb.directAdjacency.commitMaxBytes             default computed above
    rdf4j.lmdb.directAdjacency.sealWarnMillis             default 1000
    rdf4j.lmdb.directAdjacency.maxDeltaGenerations       default 8
    rdf4j.lmdb.directAdjacency.supernodeEdges             default 1048576
    rdf4j.lmdb.directAdjacency.supernodeChunkEdges        default 65536
    rdf4j.lmdb.directAdjacency.supernodeTargetBytes       default 67108864
    rdf4j.lmdb.directAdjacency.buildThreads               default min(4, availableProcessors), range 1..4
    rdf4j.lmdb.directAdjacency.buildTargetMillis          default 43200000
    rdf4j.lmdb.directAdjacency.buildRetryMillis           default 60000
    rdf4j.lmdb.directAdjacency.shadowSampleEvery          default 10000

Format constants such as page shift, block shift, u40 references, and plane order are not tunable; changing them creates
another in-memory format implementation and test matrix.

`buildTargetMillis` controls progress diagnostics and the projected-ETA warning; it does not kill a valid pinned build.
For the requested 20B deployment, changing it does not waive the fixed 12-hour rollout gate without an explicit
operational decision recorded in this plan.

For the requested deployment:

    new LmdbStoreConfig()
        .setDirectAdjacencyMode(DirectAdjacencyMode.PREFER)
        .setDirectAdjacencyCoverage(DirectAdjacencyCoverage.FULL)
        .setDirectAdjacencyMaxBytes(256L << 30)
        .setDirectAdjacencyBuildOnStart(true);

Alternatively, omit `setDirectAdjacencyMaxBytes` and run with `-Xmx512g`; AUTO then resolves from the JVM-reported
`Runtime.maxMemory()` and will normally be slightly below the explicit 256-GiB cap. Confirm the exact effective value
through startup diagnostics/metrics before applying the sizing gate. An explicit 256-GiB override is permitted with a
smaller Xmx, but the operator must still provide enough non-heap address space and RSS headroom.

At the target scale disable the old on-heap CSR so the direct index, its snapshots, and its workspaces are the only
adjacency structures charged against the 256-GiB allowance. If an operator deliberately reserves `X` GiB for a small
legacy CSR during rollout, configure the direct cap to at most `256 - X` GiB; the two independent limits must not each
be set to 256 GiB.

## Metrics and diagnostics

Expose immutable metrics through the existing native-attempt metrics path and one package-private store accessor:

    state
    baseRevision
    appliedRevision
    currentDataRevision
    gapFromRevision
    emergencyGapFromRevision
    revisionLag
    statementCount
    incidenceCount
    predicateCount
    contextOrdinalCount
    referenceNodeCount
    referenceGroupCount
    inlineGroupCount
    contextCatalogBytes
    baseBytes
    deltaBytes
    pendingBytes
    retainedSnapshotBytes
    buildCounterBytes
    buildContextDomainBytes
    buildOutputBytes
    consolidationBytes
    javaMetadataBytes
    totalChargedBytes
    highWaterBytes
    configuredMaxBytes          // zero means AUTO
    effectiveMaxBytes
    automaticMemoryLimit
    bytesPerStatement
    commitMaxBytes
    sealsCompleted
    lastSealEventCount
    lastSealUniqueRowCount
    lastSealBytes
    lastSealNanos
    maxSealNanos
    sealWarnings
    buildsStarted/completed/aborted
    activeBuildThreads
    pass1SourceVisits
    pass1Nanos
    pass3SourceVisits
    pass3Nanos
    buildElapsedNanos
    projectedBuildNanos
    deltaGenerationsPublished
    consolidationsStarted/completed/refused
    lookupHits
    exactMisses
    fallbacksByReason
    activeViews
    oldestPinnedRevision

Fallback reasons are a closed enum:

    DISABLED
    BUILDING
    SNAPSHOT_BEFORE_BASE
    REVISION_GAP
    PENDING_ROW
    PLANE_NOT_COVERED
    INLINE_NOT_COVERED
    PREDICATE_ENUMERATION_INCOMPLETE
    SERIALIZABLE
    READ_YOUR_WRITES
    MAP_RESIZE_INVALIDATED
    MEMORY_REFUSED
    INDEX_ORDER_INCOMPATIBLE
    KERNEL_REQUIRES_COMPLETE_REVISION
    STATE_CHURN

Do not log per lookup. Log state transitions and one rate-limited summary.

## Concurrency and happens-before rules

Use one single-threaded `maintenanceExecutor` to coordinate base build, delta apply, overlay consolidation, and rebuild.
Only Passes 1 and 3 may fan out to the bounded `buildExecutor` defined above; the coordinator joins all scan workers
before changing phase or publication state. Commit capture remains on the existing writer thread. This avoids
build/apply/consolidation publication races and gives strict revision order without broad reader locks.

Every long scan/sort checks a store-owned cancellation flag at group boundaries, every 65,536 pairs inside a
supernode, and at radix-pass boundaries, and preserves interrupt status. Cancellation closes its pinned transaction and
unpublished arenas in `finally`; it never publishes a partial component. Do not poll per edge in the hot query decoder.

The only publication lock is package-private `ReentrantLock publicationLock` in `LmdbDirectAdjacencyStore`. It protects
writer-side state transitions and ownership transfer; readers never take it.

Publication fields:

    AtomicReference<LmdbAdjacencyPublishedState> published
    AtomicLong emergencyGapFromRevision
    volatile MaintenanceState maintenanceState

Required orderings:

1. Base builder writes every byte, validates it, constructs one immutable state, then `published.set(state)`.
2. Commit listener constructs the complete pending row table, locks `publicationLock`, copy-adds it to the current
   immutable state, publishes that replacement, unlocks, and only then allows `dataRevision` to be written. If this
   cannot complete, it CAS-mins `emergencyGapFromRevision` before the revision write.
3. Delta applier writes every run/directory byte and the prospective predicate, context, and arena catalogs outside the
   lock. Under the lock it verifies that the current base/applied revision still matches, preserves later pending
   commits, then publishes one state containing the generation and catalogs with only this revision's pending entry
   removed.
4. Consolidator constructs the complete new overlay/catalog outside the lock, verifies its input state under the lock,
   publishes one replacement state, then retires the publisher lease on the old state.
5. `AdjacencyReadView` successfully retains one complete state and rechecks identity before returning it.
6. Closing publishes a non-retainable `CLOSED` state before retiring the previous publisher lease, then waits for
   existing state reference counts and closes arenas.
7. A rebuild publishes a continuous replacement state before resetting `emergencyGapFromRevision` to
   `Long.MAX_VALUE`; no other path raises or clears the emergency marker.

Use `AtomicReference` release/acquire semantics; do not add a volatile read inside the edge decoder. A read view captures
stable references once, so the per-edge loop sees immutable bytes with no revision branch.

The pending-set portion of a state may copy a small immutable array of pending commits. Bound the number at
`maxDeltaGenerations + 2`; if the maintenance executor falls farther behind, enter `DEGRADED_GAP` and stop direct
serving instead of turning every lookup into an unbounded scan.

`LmdbAdjacencyPublishedState` owns one publisher reference plus read-view references. A replacement state explicitly
retains every shared base/arena owner before it is published; retiring the old publisher reference cannot close memory
still reachable from either the replacement or an old view. Its refcount uses a close bit plus count in one atomic
word, so `tryRetain` cannot resurrect a state whose final release has begun.

## Failure behavior table

| Failure | Required behavior |
| --- | --- |
| native reservation refused before build | abort unpublished build; current index unchanged |
| `Arena.allocate` fails despite reservation | close unpublished arenas, reconcile accounting, set `MEMORY_REFUSED` |
| build snapshot invalidated by LMDB resize | abort and retry after configured delay |
| source scan or codec throws during build | close unpublished arenas; LMDB continues |
| captured build delta overflows | abort build; retry at a newer revision |
| commit delta overflows with active index | publish revision gap before new revision is visible; latest falls back |
| delta application throws before publication | retain exact pending/events; enter `APPLY_STALLED`; schedule rebuild |
| sealed events or touched table lost/incomplete | set global gap before revision visibility; latest falls back |
| consolidation reservation refused | keep old state exact for its leased readers; latest falls back; schedule quiescent rebuild |
| supernode replacement exceeds reserve | retain pending row, enter `APPLY_STALLED`, rebuild instead of overcommitting |
| read detects malformed offset/codec | mark index instance failed; current/new queries fall back |
| close races with query | existing lease finishes; no arena closes early |
| process terminates | all adjacency disappears; next process begins EMPTY |

No failure path creates a file or changes authoritative LMDB data.

## File-by-file implementation map

New production files under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`:

| File | Responsibility |
| --- | --- |
| `LmdbDirectAdjacencyStore.java` | lifecycle, state machine, acquisition, publication, provider API, executors |
| `LmdbDirectAdjacencyOptions.java` | resolve AUTO memory cap and immutable low-level tuning |
| `LmdbAdjacencyArena.java` | chunked native allocation, u40 virtual addresses, checked resolution, close |
| `LmdbAdjacencyArenaSizingPlan.java` | deterministic alignment/region simulation and exact capacities |
| `LmdbAdjacencyArenaCatalog.java` | immutable arena slots, packed direct handles, owner retention |
| `LmdbAdjacencyMemoryAccount.java` | hard reservations and per-kind metrics |
| `LmdbAdjacencyCoverage.java` | immutable FULL/SELECTED raw-predicate classification |
| `LmdbAdjacencyContextCatalog.java` | base/extension context ordinals and raw-ID translation |
| `LmdbAdjacencyRunCodec.java` | SMALL/BLOCK/chunk sizing, encode, decode, copy, lower bound |
| `LmdbReferenceNodeLocator.java` | top arrays, bitmap/rank pages, compact node-header search |
| `LmdbInlineIncomingIndex.java` | radix/block exact lookup for inlined object keys |
| `LmdbAdjacencyBaseBuilder.java` | Passes 0–3, source-order validation, exact sizing, catch-up |
| `LmdbAdjacencyBuildTxnFamily.java` | same-`mdb_txn_id` worker transactions, version validation, family close |
| `LmdbAdjacencyBuildWorkspace.java` | striped count-page install, plane counts, active maxima, component writers |
| `LmdbDirectAdjacencyCommitDelta.java` | complete per-write-transaction primitive event capture and overflow |
| `LmdbAdjacencyDeltaApplier.java` | directional radix sorts, streaming row merges, delta generation creation |
| `LmdbAdjacencyOverlaySet.java` | predicate/context/arena catalogs, generation lookup, consolidated version lists |
| `LmdbAdjacencyPublishedState.java` | atomic base/overlay/pending snapshot and close-bit reference count |
| `LmdbAdjacencyReadView.java` | snapshot revision, retained base/overlay/pending view, fallback sentinel |
| `LmdbDirectAdjacencyIterator.java` | bound row `RecordIterator` |
| `LmdbDirectNodeIterator.java` | FULL-coverage bound-node/unbound-predicate group merge |
| `LmdbDirectNativeAdjacency.java` | long-handle `NativeAdjacency` adapter |
| `LmdbAdjacencyMetrics.java` | immutable metrics record and fallback counters |

New public configuration files:

| File | Responsibility |
| --- | --- |
| `config/DirectAdjacencyMode.java` | `DISABLED`, `SHADOW`, `PREFER` |
| `config/DirectAdjacencyCoverage.java` | `FULL`, `SELECTED` |

Existing production edits:

| File | Exact change |
| --- | --- |
| `TripleStore.java` | own/direct collector; tee two fan-out methods; listener registration; pending mark before revision bump; drain sealed delta |
| `LmdbSailStore.java` | construct/close provider; apply after full commit; dataset read views; probe/count/has/kernel arbitration; parallel lease sharing |
| `TxnManager.java` | same-snapshot build-family helper under the existing read lock; no revision-format change |
| `ValueStore.java` | package-private payload-high-water accessor read while writer exclusion is held; no persisted format change |
| `NativeLmdbQuerySource.java` | replace int-dense adjacency SPI with long run handles |
| `LmdbCsrAdjacencyCache.java` | no representation rewrite; only adapter/access changes required by the new SPI |
| `LmdbCsrRunIterator.java` | no semantic change |
| native interpreted/codegen classes | migrate dense/run offsets to long handle + local offset |
| `LmdbStoreConfig.java` | five stable direct-adjacency settings, validation, parse/export |
| `LmdbStoreSchema.java` | five schema IRIs |

Do **not** edit `StoreProperties`, create an adjacency directory, add an LMDB DBI, or add a serialization dependency.

## Interfaces and dependencies

No new external dependency is allowed. Use:

- JDK 25 `Arena`, `MemorySegment`, `ValueLayout`, `VarHandle`;
- existing `Varint`;
- existing `RecordIterator`;
- existing `TxnManager` and pinned snapshot revision;
- existing LMDB index scans and `TripleStore.getTriples`;
- existing primitive utilities where they meet long-count requirements.

Add to `ValueIds` only if it improves central correctness:

    static boolean isReference(long id) {
        if (isDouble(id)) {
            return false;
        }
        int type = getIdType(id);
        return type >= T_URI && type <= T_TRIPLE;
    }

An inlined numeric/string value may still have an even low bit; do not infer "reference" from the low bit alone.

Core internal interfaces:

    interface SortedPairSource {
        boolean next();
        long neighbor(); // raw RDF4J value ID
        long context();  // raw RDF4J context ID; codec translates to ordinal
    }

    interface AdjacencySourceScanner extends AutoCloseable {
        void scanPredicates(PredicateConsumer consumer) throws IOException;
        void scanOutgoing(boolean explicit, GroupConsumer consumer) throws IOException;
        void scanIncoming(boolean explicit, GroupConsumer consumer) throws IOException;
        long snapshotRevision();
        long snapshotId();                    // LMDB mdb_txn_id
        void ensureSnapshotValid() throws IOException;
        void close();
    }

    interface AdjacencySourceFamily extends AutoCloseable {
        AdjacencySourceScanner primary();      // Pass 0/context scan and buildThreads=1
        AdjacencySourceScanner scanner(int plane);
        int activeStreamCount();
        long snapshotRevision();
        long payloadHighWaterExclusive();
        void cancel();
        void close();
    }

    interface GroupConsumer {
        void begin(long key, long predicate, int plane);
        void pair(long neighbor, long context);
        void end();
    }

`LmdbAdjacencyBuildTxnFamily` implements the production family and creates one scanner/transaction per active logical
stream. The production scanner wraps LMDB `RecordIterator`s and is thread-confined. Tests provide primitive synthetic
families/scanners to exercise counts above `Integer.MAX_VALUE` without allocating billions of rows.

## Detailed implementation milestones

Each milestone is independently testable. Keep exactly one item in progress in this plan. Before the first command in a
new implementation session, run the repository-required root clean install. Commands are from repository root.

### Milestone 0 — preserve baseline and fix the evidence boundary

Description: capture the current snapshot/CSR/config baseline before introducing any direct-adjacency production type.
Milestone 0 adds no feature test and changes no production code.

Run:

    mvn -B -ntp \
      -Dmaven.compiler.showWarnings=false \
      -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Then:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbCsrSnapshotTest --retain-logs
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStoreConfigTest --retain-logs

Record initial evidence using `scripts/agent-evidence.py` as required by `AGENTS.md`.

The evidence rule is explicit: a Java compilation error caused by a nonexistent future API is **not** a red test.
Every implementation red must compile, execute under Surefire or Failsafe, and leave a relevant failure snippet in
`target/*-reports/`. The full no-file behavioral test belongs to Milestone 5, after its configuration/build signatures
exist but before store integration is implemented.

Acceptance: both baseline selections are green, their report snippets are copied into `initial-evidence.txt`, and no
production file has changed.

### Milestone 1 — configuration, accounting, and native arena

The first red is
`LmdbStoreConfigTest#parsesAndExportsDirectAdjacencySettingsFromRawRdfProperties`. It deliberately references no new
Java constant, enum, getter, or builder. Using only the existing `LmdbStoreConfig`, RDF `Model`, and value factory, it
constructs the future `directAdjacencyMode`, `directAdjacencyCoverage`, and `directAdjacencyMaxBytes` IRIs from the
existing LMDB configuration namespace string, parses `PREFER`, `FULL`, and a numeric limit, exports the configuration,
and asserts those same property/value pairs are present. On the pre-feature tree this must compile and fail in
Surefire because the properties are not round-tripped. Capture that report before adding configuration production
code.

For a wholly new internal production type that no compiling test can name, use a two-red bootstrap:

1. a compile-safe `Class.forName`/constructor-signature contract test fails in Surefire while the type is absent;
2. add only the minimal type/signature, then add and run the typed behavioral test before implementing its behavior.

Neither a test-compilation failure nor a production stub with untested behavior satisfies the gate.

Further tests first:

- `LmdbStoreConfigTest#directAdjacencyDefaultsDisabled`;
- `LmdbStoreConfigTest#directAdjacencyUnsetLimitResolvesToHalfEffectiveXmx`;
- resolver vectors for odd heap sizes, eight-byte rounding, explicit override, and AUTO below 256 MiB;
- commit-limit vectors for the 8-MiB floor, computed default, 64-MiB default ceiling, 2-GiB absolute ceiling, explicit
  lower/raise, and over-ceiling rejection;
- parse/export round trips for every mode/coverage and deterministic repeated selected-predicate IRIs;
- FULL-with-selection and SELECTED-without-selection validation failures;
- invalid max bytes and build-on-start combinations;
- `LmdbAdjacencyMemoryAccountTest` for reserve/release/high-water/refusal and checked overflow;
- `LmdbAdjacencyArenaTest` across region boundary, u40 boundary, alignment, cross-region rejection, shared-thread access,
  close, double close, and invalid reference;
- `LmdbAdjacencyArenaSizingPlanTest` proving simulated refs/capacity exactly match allocator output across boundary
  padding;
- `LmdbAdjacencyArenaCatalogTest` for handle pack/unpack, slot zero, slot 255, overflow refusal, owner retain/release, and
  negative sentinel separation.

Implement the two enums, schema/config fields, `LmdbDirectAdjacencyOptions`, `LmdbAdjacencyMemoryAccount`,
`LmdbAdjacencyArena`, `LmdbAdjacencyArenaSizingPlan`, and `LmdbAdjacencyArenaCatalog`. Make the max-byte resolver a
package-private pure function that accepts `(configuredBytes, effectiveJvmMaxBytes)` so tests do not mutate or assume
the test JVM's real Xmx.

Arena test sizes must stay small: inject a 4-KiB test region size through a constructor while the production constant
remains 1 GiB. This is a real allocator parameter, not a test-specific branch.

Acceptance:

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbStoreConfigTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyMemoryAccountTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyArenaTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyArenaSizingPlanTest
    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbAdjacencyArenaCatalogTest

All pass; no file-writing API appears in new adjacency classes (`rg` audit in Milestone 9).

### Milestone 2 — codecs, locator pages, and headers

Tests first in:

- `LmdbAdjacencyRunCodecTest`;
- `LmdbAdjacencyContextCatalogTest`;
- `LmdbReferenceNodeLocatorTest`;
- `LmdbInlineIncomingIndexTest`.

Codec vectors:

- exact tag-byte offsets for all three codecs and rejection of fused/omitted tags;
- degrees 1, 15, 16, 127, 128, 129;
- repeated neighbors with multiple contexts;
- null-only contexts (side column absent);
- mixed null/named contexts;
- appended context ordinals whose numeric order differs from raw context order;
- unsigned IDs crossing signed-long order;
- `ValueIds.isDouble`, ordered integers, reference literals;
- widths 0, 1, 7, 8, 31, 32, 63, 64;
- lower-bound before first, exact, between, after last;
- chunk boundary within one neighbor's long context sequence, preserving raw pair order;
- chunk directory whose unchanged chunks live in base and an older delta arena;
- malformed/truncated headers rejected.

Locator vectors:

- slots 0, 63, 64, 4095;
- empty, one-bit, sparse, and full pages;
- rank prefixes and u40 references;
- predicate-count width boundaries 255/256 and 65,535/65,536;
- all four planes;
- 100 predicates;
- missing node versus missing group;
- payload beyond the base top-array bound returns base absence without a native read, while an overlay for that
  post-`B` node still wins.

Inline vectors:

- radix prefixes 0, 1, 32768, 65535;
- unsigned boundary IDs;
- one and multiple blocks;
- missing-key verification;
- explicit/inferred separation;
- covered empty plane allocates no radix directory, while a later overlay row remains visible.

Context catalog vectors:

- default graph ordinal zero;
- empty, one, and 1,024 base contexts;
- extension raw IDs before/between/after base IDs while ordinals append;
- raw-to-ordinal and ordinal-to-raw misses/bounds;
- old catalog unchanged after extension;
- consolidation copy preserves ordinals and rewrites arena slots.

Acceptance: byte-for-byte size estimate equals actual encoded allocation for every randomized vector; decode equals
input; malformed memory never reads out of range.

### Milestone 3 — long-handle native adjacency SPI

Tests first:

- update `LmdbCsrAdjacencyBulkCopyTest` to the new handle API;
- update `LmdbNativePropertyPathTest` test doubles;
- emitter source tests expect long handles and long loop offsets;
- add a synthetic run with logical size `Integer.MAX_VALUE + 17L` whose decoder computes values lazily, proving no int
  narrowing without allocating it.

Implement the new `NativeAdjacency` interface, `CsrNativeAdjacency` adapter, and update every `rg` call site.

Run the narrow codegen/interpreted classes named by the changed tests before any direct-store integration.

Acceptance:

- old CSR query results unchanged;
- key enumeration still works for CSR;
- generated source contains `long run`/`long` loop position;
- copy lengths remain bounded `int`;
- no raw `(int)` cast of degree/run position without a preceding checked bound.

### Milestone 4A — exact single-thread base builder

Tests first in `LmdbAdjacencyBaseBuilderTest`:

1. explicit default graph, both directions;
2. inferred-only and explicit promotion/demotion source images;
3. 100 predicates on one node;
4. sparse value-ID pages;
5. reference literal and inlined literal objects;
6. named contexts and equal-neighbor multiple contexts;
7. empty store;
8. incompatible index configuration refuses FULL;
9. exact sizing refusal before output allocation;
10. injected snapshot invalidation aborts without publication;
11. captured online commits catch up in order;
12. captured-delta overflow aborts;
13. SELECTED builds every plane for configured predicates and no rows for unselected predicates;
14. configured predicate absent at B is classified correctly on its first captured commit;
15. no files created.

Implement Passes 0–3 and initial `LmdbAdjacencyPublishedState` publication with `buildThreads = 1`. This is the
deterministic byte-format oracle and bounded-memory reference path; it is not the final 20B operational path.

Differential oracle: for every built row, compare emitted quads to
`tripleStore.getTriples(pinnedTxn, s,p,o,c,explicit)` with direct adjacency disabled.

Acceptance: builder tests pass and a small repository-level store can build, query both directions, close, reopen empty,
and rebuild.

### Milestone 4B — same-snapshot parallel builder and ETA gate

Tests first in `LmdbAdjacencyParallelBaseBuilderTest`:

1. one-, two-, and four-stream builds are byte-for-byte identical, including zero padding;
2. explicit-only uses two workers; mixed explicit/inferred uses four without plane aliasing;
3. a node discovered by all four streams receives one locator page/header with four exact counts;
4. concurrent first access to one count page installs exactly one page and leaks no losing allocation;
5. captured payload high-water bounds the build workspace while final top arrays shrink to active maxima;
6. sibling transaction-ID or revision mismatch aborts before a scan;
7. one sibling map/version invalidation cancels every worker and publishes nothing;
8. one worker codec/source failure closes all transactions, tasks, and unpublished arenas;
9. each Pass-3 writer rejects a write outside its preallocated component range;
10. component end references, source totals, header cursors, and context round trips match the single-thread oracle;
11. close during a blocked worker releases both executors without a sleep-based test;
12. checked pilot-ETA arithmetic covers zero rate, overflow, 80-billion visits, ten-hour pilot margin, and twelve-hour
    full gate.

Implement the transaction-family creation, payload-high-water accessor, striped build workspace, disjoint Pass-3
writers, bounded executor, metrics, and cancellation protocol exactly as specified above. Use package-private latches
for interleavings; never share an LMDB cursor/transaction across worker threads.

Run a build benchmark over at least 200 million representative source-statement visits per required source order on
deployment-like hardware. Record one-, two-, and four-thread Pass-1/Pass-3 rates, even when empty inferred streams
reduce the active worker count.

Acceptance: parallel output equals the reference format; all ownership/interleaving tests pass; the target pilot
projects at most ten hours. Final 20B `<= 12 h` acceptance remains a Milestone-9 rollout gate and cannot be inferred
solely from a synthetic test.

### Milestone 5 — bound-probe integration

Tests first in `LmdbDirectAdjacencyQueryTest`:

- subject-bound and object-bound;
- bound reference subject/object with predicate unbound across multiple predicates;
- unbound-predicate inline object and SELECTED coverage fall back before first result;
- context bound/unbound;
- exact empty versus uncovered fallback;
- read-your-writes bypass;
- SERIALIZABLE bypass;
- ordered compatible seek;
- incompatible requested order falls back;
- count, has, exact degree;
- inline incoming;
- root scan intentionally falls back;
- `SHADOW` compares but never serves;
- `PREFER` serves and records a hit.

Also add `LmdbDirectAdjacencyEphemeralTest#buildAndCloseCreatesNoAdjacencyFiles` before wiring the provider into the
store. Its source now compiles against the Milestone-1 configuration surface and Milestone-4 builder, but it fails in
Surefire because store-level build/close/reopen behavior is not integrated yet. Snapshot the recursive relative file
list inside the temporary store directory after an ordinary committed dataset but before enabling direct adjacency;
build, query, close, reopen, and compare the exact set of relative paths. Do not compare file sizes because LMDB may
update its existing files. Assert:

- no path name contains `adjacency`, `csr`, `segment`, `manifest`, `journal`, or `checkpoint`;
- the reopened direct store starts in `EMPTY`/`BUILDING`, never `ACTIVE` from old memory;
- results remain correct through LMDB before the new build completes.

Implement `LmdbAdjacencyProvider`, `LmdbDirectAdjacencyStore`, iterator, read view, constructor/close wiring, and provider
arbitration in `LmdbSailStore`.

Acceptance: cache-off/direct-on results equal direct-off LMDB results across explicit/inferred and contexts.

### Milestone 6 — commit versions and snapshot isolation

Tests first in `LmdbDirectAdjacencySnapshotTest` and `LmdbDirectAdjacencyCommitTest`.

Required interleavings:

1. warm/build at R0; pin S=R0; commit add R1; old reader excludes add, new reader includes it;
2. same for removal;
3. pending window injected between revision bump and generation publish: touched row falls back, untouched row serves;
4. add/remove/re-add same quad in one commit;
5. new node and new predicate;
6. removal empties group and publishes tombstone;
7. explicit/inferred promotion and demotion touch correct planes;
8. named context materializes and later disappears; its appended ordinal publishes atomically with the row, and an old
   snapshot retains the prior context catalog;
9. delta apply failure retains exact pending state: touched rows/complete kernels fall back and untouched bound rows
   still serve;
10. delta overflow makes R and later fallback while S<R still uses old index;
11. rollback publishes neither pending marker nor generation;
12. parallel siblings share revision/base/overlay identity;
13. map resize invalidates pinned dataset before adjacency access;
14. snapshot `S < baseRevision` falls back;
15. no pinned readers leaves no unnecessary historical row version after consolidation.
16. reader paused during apply can observe either `{old overlay + pending}` or `{new overlay + pending removed}`, never
    `{old overlay + pending removed}`;
17. new-predicate pending lookup uses the raw predicate ID before catalog extension publishes;
18. close/rebuild racing `tryRetain` either returns a fully retained view or fallback, never a closed arena.
19. injected pending-state allocation failure sets the emergency gap before revision visibility, and only a published
    continuous rebuild clears it.
20. unbound-predicate node enumeration with any pending `(rawKey, plane, *)` row falls back before emitting; after
    publication its k-way group merge includes additions and excludes tombstones at the correct snapshot.
21. pending-table raw-key and plane fences skip native table access for misses, including ten retained tables; keys
    inside a fence still find exact newest/oldest-table hits and misses.
22. malformed pending metadata that disagrees with its first/last entry is rejected before publication.
23. default-cap and explicitly raised-cap seal overflow paths publish a gap without partially publishing a table.

Implement complete collector, listener, applier, pending set, `LmdbAdjacencyPublishedState`, generation lookup, and dataset
leases. Use package-private latch hooks around publication in tests; do not add timing sleeps.

Acceptance: all snapshot tests pass with direct serving proven by metrics where expected and LMDB fallback proven in the
pending/gap cases.

### Milestone 7 — overlay consolidation and quiescent rebuild

Tests first:

- revisions R1..R12 force more than eight generations;
- readers pinned at R2, R7, and R12 retain exactly needed versions after consolidation;
- closing R2 allows older payload reclamation;
- consolidation allocation refusal leaves old set valid;
- retained views delay arena close;
- consolidation rewrites non-base chunk slots and reduces the active arena catalog to base plus output;
- rebuild makes new acquisitions fallback, waits old lease, frees old base before allocating new output;
- commits during rebuild catch up or abort on bounded overflow;
- repeated failed rebuilds leak zero charged bytes;
- close during build/apply/consolidate terminates executors and frees all arenas.

Implement k-way consolidation and state-machine rebuild. Add package-private lifecycle counters so tests can assert
allocation/close balance without reflection.

Acceptance: high-water memory never exceeds configured limit in adversarial tests.

### Milestone 8 — supernodes and native kernels

Tests first:

- threshold promotion;
- ordered chunk boundaries;
- insert/remove in first/middle/last chunk;
- split/merge;
- one neighbor with enough contexts to cross multiple chunk pair ranges;
- snapshot before and after chunk replacement;
- property paths and nested kernel adjacency over direct runs;
- long logical degree;
- kernel declines when applied revision lags snapshot;
- direct view has no root enumeration and planner uses CSR/LMDB root safely.

Implement chunk update/consolidation and `LmdbDirectNativeAdjacency`; finish interpreted/Janino migration.

Acceptance: nested graph traversals match LMDB, no per-edge allocation, and ordered-run proofs remain true.

### Milestone 9 — shadow validation, performance, and full audit

Shadow differential sampling must compare complete materialized result rows, including context and explicit flag, without
changing returned results. A mismatch:

1. increments a mismatch counter;
2. logs predicate/direction/revision and stable row hashes, not RDF values;
3. changes state to `DEGRADED_GAP`;
4. routes future queries to LMDB;
5. never attempts to repair one row heuristically.

Add `DirectAdjacencyBenchmark` cases:

    degree: 1, 4, 16, 128, 4096, 1,048,576
    predicates: 1 and 100
    contexts: none, 4, 1024; base-only and one appended catalog segment
    source: base, one delta, consolidated overlay
    pending table count: 0, 1, 4, 10
    pending disposition: raw key outside every fence, in-fence miss,
                         hit in newest table, hit in oldest table
    operation: find, scalar decode, copy, lowerBound, count, has, bound-node group enumeration

The complete pending matrix is mandatory for degrees 1, 16, and 128; other degrees need only 0 and 10 tables. Count
native pending-entry reads separately so a fence-miss benchmark proves zero table reads rather than merely appearing
fast. A pending hit includes the subsequent LMDB fallback in its end-to-end score.

Add `DirectAdjacencySealBenchmark` with `1,024`, `65,536`, `262,144`, and the exact maximum event count admitted by the
resolved default cap. Run both worst-case unique touched rows and duplicate-heavy events. Report the 17-pass sort/seal
time, event rate, row rate, native peak bytes, p50, p95, and maximum. Repeat at an explicitly raised cap before allowing
that cap in production.

Add `DirectAdjacencyBuildBenchmark` for Pass 1 and Pass 3 with one, two, and four logical streams, explicit-only and
mixed explicit/inferred data, sparse/dense reference pages, and contexts absent/present. The production-scale run uses
the real LMDB scanners, not only synthetic sources: first the required 200-million-visit sample per source order, then
one complete 20B SHADOW build.

Compare current CSR and LMDB on the same data. Use the repository benchmark helper for one method at a time and JFR when
investigating:

    ./scripts/run-single-benchmark.sh \
      core/sail/lmdb \
      '.*DirectAdjacencyBenchmark.lookupDegree128.*'

Hard gates:

- zero allocation per decoded edge after warmup;
- base lookup/decode has no lock and no JNI;
- all global positions are long;
- representative 20B sizing report is `<= steadyLimitBytes` (`220.16 GiB` only for an exact 256-GiB effective cap);
- representative peak build estimate is `<= effectiveMaxBytes`;
- the target pilot projects `<= 10 h`, and one complete 20B build reaches publication in `<= 12 h`; report all
  80-billion source visits, Pass-1/Pass-3 aggregate rates, fixed-pass time, validation time, and catch-up time;
- with ten pending tables, an outside-all-fences degree-1 lookup performs zero pending-entry reads and its median
  latency is at most 1.20 times the zero-pending direct lookup under the same fork;
- with ten pending tables, the degree-1 in-fence miss remains faster than the direct-disabled LMDB bound probe, and a
  pending hit including LMDB fallback is at most 1.25 times that LMDB baseline;
- on the target 20B host, worst-case-unique sealing at the configured `commitMaxBytes` has p95 at or below
  `sealWarnMillis` (default 1,000 ms) and no measured sample above twice that value;
- the root-workload report is complete and its exact named/date acceptance line is recorded;
- no adjacency file activity.

If a pending or seal latency gate fails, rollout remains `SHADOW`. For pending lookup, replace the pending-set lookup
shape and rerun the matrix; do not hide the result with a degree heuristic. For sealing, lower `commitMaxBytes` until
the same cap-bound benchmark passes. An operator may raise the 64-MiB default only after the raised value passes this
gate on the deployment hardware.

Performance guidance, not a correctness substitute:

- degree-16 and degree-128 base traversal should reach at least 70% of current flat-CSR throughput;
- it should beat LMDB bound-probe throughput on the benchmark host;
- consolidation/apply throughput must be reported, not hidden behind a pass/fail assertion.

Static audits:

    rg -n "FileChannel|MappedByteBuffer|map\\(|Files\\.write|Files\\.create|OutputStream|RandomAccessFile" \
      core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/Lmdb*Adjacency*.java

Expected: no adjacency persistence/file-writing match. Imports used only by unrelated source scanners are not in the new
adjacency storage classes.

    rg -n "denseIdOf|runStart\\(|runEnd\\(|copyRun\\(" \
      core/sail/lmdb/src/main core/sail/lmdb/src/test

Expected: no old `NativeAdjacency` API call remains outside deliberately retained CSR internals.

Run focused snapshot, query, codec, builder, config, kernel, and existing CSR suites, then:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

Run `core/sail/base` tests because snapshot dataset lifecycle is shared:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/base --retain-logs

Before handoff:

    cd scripts
    ./checkCopyrightPresent.sh

Then from root:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The `-q` here is formatter/resource processing, not a test run.

## Validation and acceptance

The feature is complete only when all of these are true:

### Correctness

- randomized differential tests cover adds, removes, re-adds, contexts, explicit/inferred, empty/new groups, inline
  objects, custom index configurations, and supernodes;
- FULL bound-node/unbound-predicate enumeration matches LMDB across base, pending fallback, deltas, and consolidation;
- every direct answer equals the pinned LMDB oracle at the same revision;
- pending, gap, budget, resize, and build states fall back rather than answer partially;
- SNAPSHOT tests cover commits and consolidation while the reader remains open;
- SERIALIZABLE and read-your-writes bypass;
- root scans do not accidentally treat lack of key enumeration as an empty predicate.

### Memory

- an unset cap resolves once to exactly half the effective JVM max heap, rounded down to eight bytes;
- a positive configured cap overrides AUTO without being clamped to Xmx;
- the real codec sizer equals emitted bytes;
- memory reservations precede allocation;
- no global per-edge/per-node/group Java objects;
- `totalBaseBytes <= steadyLimitBytes`;
- `peakBuildBytes <= effectiveMaxBytes`;
- current + delta + pending + retained + consolidation never exceeds `maxBytes`;
- every arena close has matching accounting release;
- logical tests exceed `Integer.MAX_VALUE` without huge allocation.

### Ephemerality

- no adjacency-specific file or LMDB DBI is created;
- close frees all arenas;
- reopen starts EMPTY/BUILDING and uses LMDB until rebuilt;
- no restart code tries to recover an old adjacency;
- no mmap or serialization dependency is introduced.

### Performance

- hot base decode allocates zero per edge;
- edge loop reads immutable primitive bytes with no locks/revision checks;
- lookup and decode benchmarks are recorded against CSR and LMDB;
- build and delta-apply throughput are recorded with exact hardware/JVM settings;
- the 200-million-visit target pilot projects at most ten hours and one complete 20B build publishes within twelve
  hours;
- pending-table and writer-seal latency gates pass at the rollout configuration;
- the root-workload report and named/date `ROOT_ENUMERATION_ACCEPTED` sign-off exist before `PREFER`;
- the representative 20-billion distribution meets the size gates or the plan reports that FULL coverage cannot meet
  the budget.

## Idempotence and recovery

Because adjacency is ephemeral, recovery always means "drop derived memory and continue from LMDB":

- An unpublished build can be closed and rerun safely.
- A failed apply leaves the revision pending/gapped and cannot expose partial rows.
- Applying a sealed delta to a fresh unpublished output is idempotent by ensure-present/ensure-absent semantics.
- A failed consolidation leaves the old `OverlaySet` published.
- A failed rebuild leaves no new index; new queries already use LMDB.
- Process restart discards all adjacency state by definition.

Do not try to salvage partially written native arenas after a failure. Close the owning arena, release its complete
charge, and rebuild.

## Operational rollout

1. Ship with `DISABLED` default.
2. Enable `SHADOW` with a small max size on tests and medium datasets; run sampled parity for at least one full workload.
3. Run exact sizing/preflight on the representative large dataset.
4. Set the 20B deployment to an effective 256-GiB cap and verify base/peak gates against that effective value.
5. Run the 200-million-visit-per-source pilot, require a `<= 10 h` projection, then require one complete 20B
   build-and-publish in `<= 12 h`.
6. Run `SHADOW` through commits, snapshots, and at least one consolidation; pass the pending and seal latency gates at
   the configured commit cap.
7. Produce the root-workload report and obtain the exact `ROOT_ENUMERATION_ACCEPTED` sign-off above.
8. Switch to `PREFER` and disable the old CSR for the 256-GiB target. If a bounded CSR is retained for root enumeration,
   subtract its explicit allowance from the direct cap.
9. Alert on nonzero revision lag, gap state, shadow mismatch, memory refusal, aborted build, seal warnings, and
   long-lived retained snapshot bytes.

At 20B, schedule the first build when writes are quiescent. Since the user explicitly rejects persistence, every restart
incurs this rebuild. The measured `<= 12 h` duration, thread count, statement-visit rates, and LMDB fallback behavior
during that window are deployment characteristics that must appear in the runbook.

## Artifacts and notes to record during implementation

Keep in this plan or linked evidence files:

- the `startTransaction`/mutation-path audit proving both direct collector tee points are complete;
- exact configured LMDB index orders used by the builder;
- sizing component report for small, representative, and target datasets;
- peak native-memory report;
- build scan/encode throughput and ETA;
- full 20B wall-clock build breakdown and 80-billion source-visit counters;
- direct/CSR/LMDB JMH tables;
- root-workload report and `ROOT_ENUMERATION_ACCEPTED` sign-off;
- snapshot/consolidation interleaving evidence;
- the no-file static audit output;
- any baseline test failures already present on the branch.

Do not write raw adjacency dumps as diagnostics. Stable counts, hashes, histograms, and JFR recordings are sufficient.

## Progress

- [x] (2026-07-28) Inspected current CSR layout, commit delta, snapshot generations, transaction pinning, query SPI,
  value-ID encoding, iterator behavior, and configuration patterns.
- [x] (2026-07-28) User clarified that adjacency data must be strictly in-memory; discarded the persistent-sidecar
  design before implementation.
- [x] (2026-07-28) User set the default adjacency cap to 50% of effective Xmx, with an explicit byte override.
- [x] (2026-07-28) Fixed the node-centric base layout, exact sizing/peak gates, immutable delta model, snapshot protocol,
  context ordinals, atomic publication unit, and no-overlap rebuild policy.
- [x] (2026-07-28) Authored this implementation workup.
- [x] (2026-07-28) Completed static ephemerality, ownership, publication, and configuration consistency audit.
- [x] (2026-07-28) Incorporated independent review findings: executable red-test evidence, flat locator bounds, explicit
  codec tags, pending fences, seal/build latency gates, parallel rebuild, sparse inline planes, and root-scan sign-off.
- [x] (2026-07-28) Captured implementation baseline (Milestone 0: `LmdbCsrSnapshotTest` 10 green,
  `LmdbStoreConfigTest` 67 green; `initial-evidence-direct-adjacency-m0.txt`) and the first executable reds
  (`LmdbStoreConfigTest#parsesAndExportsDirectAdjacencySettingsFromRawRdfProperties` 1 failure and the
  Class.forName two-red bootstrap `LmdbDirectAdjacencyBootstrapTest` 7 failures;
  `initial-evidence-direct-adjacency-m1-red.txt`, `initial-evidence-direct-adjacency-m1-bootstrap-red.txt`).
- [x] (2026-07-28) Milestone 1 complete: `DirectAdjacencyMode`/`DirectAdjacencyCoverage`, five
  `LmdbStoreConfig`/`LmdbStoreSchema` settings with validation, `LmdbDirectAdjacencyOptions` (AUTO resolver, exact 86%
  steady limit, commit-cap resolvers, tuning properties), `LmdbAdjacencyMemoryAccount`, `LmdbAdjacencyArena` (u40,
  injectable region size, owner refcount), `LmdbAdjacencyArenaSizingPlan` (shared bump state machine),
  `LmdbAdjacencyArenaCatalog`. Green: config 89, options 13, account 7, arena 10, sizing plan 6, catalog 6, bootstrap
  7 (`post-evidence-direct-adjacency-m1.txt`); no-file rg audit clean.
- [x] (2026-07-28) Milestone 2 complete: `SortedPairSource`/`ContextCatalog` interfaces,
  `LmdbAdjacencyContextCatalog` (base + copy-on-write extension segments + consolidation copy),
  `LmdbAdjacencyRunCodec` (SMALL_VARINT/BLOCK_FOR/CHUNK_DIRECTORY with physical tag byte; one shared sizing/writing
  state machine; randomized predicted==allocated equality; supernode chunking at the 65,536-incidence target; decode
  supports cross-arena chunk slots), `LmdbReferenceNodeLocator` (bitmap/rank pages, width-1/2/4 headers, out-of-range
  page as base absence), `LmdbInlineIncomingIndex` (radix/block exact lookup, empty plane allocates nothing),
  `ValueIds.isReference`, segmented `LmdbAdjacencyArena.U40Table`. Two-red bootstrap red captured
  (`initial-evidence-direct-adjacency-m2-bootstrap-red.txt`); green: context catalog 7, codec 27, locator 9, inline 7,
  bootstrap 4 (`post-evidence-direct-adjacency-m2.txt`); no-file rg audit clean.
- [x] (2026-07-28) Milestone 3 complete (Routine B: pre/post green on the same selections + hit proof).
  `NativeAdjacency` replaced with the long-run-handle contract (NOT_FOUND/-1, NOT_COVERED/-2, find/size/neighborAt/
  contextAt/copyNeighbors/copyContexts/lowerBound/supportsKeyEnumeration/keyCount/keyAt/runsNeighborOrdered);
  `CsrNativeAdjacency` adapts with handle = dense + 1 and checked narrowing; `LmdbNativeJaninoPipeline`,
  `LmdbNativeJaninoAggregate`, `LmdbNativeKernelEmitter` (incl. vector tails and leapfrog long positions),
  `LmdbNativePathPlan`, `LmdbNativeKernelBindings`, and all test doubles migrated; generated loops use long
  handles/positions with int only for bounded batch copies. New `longRunHandlesSurviveBeyondIntRange` proves
  Integer.MAX_VALUE+17 logical sizes without narrowing. Pre-green: BulkCopy 2, PropertyPath 42, IrEmitter 40,
  CacheTest 21 (`initial-evidence-direct-adjacency-m3-pre-green.txt`). Post-green: BulkCopy 3, PropertyPath 42,
  IrEmitter 40, CacheTest 21, Specialization 9, CeilingParity 3, JaninoPipeline 3, JaninoAggregate 2,
  KernelExecution 5, KernelAggregate 7, JaninoCodegen 6, CsrCacheQuery 6 (`post-evidence-direct-adjacency-m3.txt`).
  rg audit: old API calls remain only inside retained `CsrEntry` internals (LmdbCsrAdjacencyCache/LmdbCsrRunIterator).
- [x] (2026-07-28) Milestone 4A core complete: push-mode run-codec encoder, `AdjacencySourceScanner` (with
  contexts/ordered-scan capability probes), `LmdbReferenceNodeLocator.TwoPhaseBuilder` (Pass-2 declare with exact
  counts + Pass-3 fillEntry via zero-scan cursors), `LmdbAdjacencyBuildWorkspace` (off-heap bitmap + u32 count pages,
  BUILD_COUNTERS charged, lazily paged), `LmdbAdjacencyPredicateCatalog`, `LmdbAdjacencyCoverage`,
  `LmdbAdjacencyMemoryRefusedException`, `LmdbAdjacencyBaseBuilder` (Passes 0–3, memory gate with region-granular
  reservation before any base allocation, Pass-1 vs Pass-3 exact byte/count cross-checks, BUILD_OUTPUT→BASE
  reclassification), `LmdbInMemoryAdjacencyIndex` (base-row resolver with NOT_FOUND/NOT_COVERED semantics).
  `LmdbAdjacencyBaseBuilderTest` 12/12 green with a synthetic ordered scanner and a differential pair-set oracle
  (`post-evidence-direct-adjacency-m4a.txt`), covering vectors 1–10, 13 plus charge-release accounting. Deferred, by
  dependency: vectors 11/12 (online catch-up, capture overflow) and 14 (SELECTED first-commit classification) need the
  Milestone-6 commit collector; vector 15 and the store-level build/query/reopen smoke land with Milestone-5 store
  integration, which also brings the production LMDB scanner (`getTriples` spoc/posc wrappers) and the ValueStore
  payload high-water in place of the reference path's generous default.
- [~] Milestone 5 in progress (2026-07-28): `LmdbAdjacencyTripleStoreScanner` implemented — spoc/posc-prefixed
  `StatementOrder`-selected index scans over one pinned read transaction, contexts-DBI domain scan, merged distinct
  predicates, map-resize/version + revision-advance snapshot guards (revision guard is deliberately strict until the
  Milestone-6 collector provides online catch-up), and `supportsOrderedScan` prefix validation.
  `LmdbDirectAdjacencyEphemeralTest` green (2 tests, `post-evidence-direct-adjacency-m5-scanner.txt`): a real
  `TripleStore` build answers both directions byte-exactly against `tripleStore.getTriples` on the same pinned
  transaction, rebuilds cleanly, releases every memory charge, creates no file (recursive relative listing unchanged;
  no adjacency/segment/manifest/journal/checkpoint names), and a `spoc`-only configuration refuses the incoming
  direction. STILL TO DO for Milestone 5: `LmdbAdjacencyProvider`/`LmdbDirectAdjacencyStore` lifecycle + published
  state, dataset read views and `LmdbSailStore` arbitration (probe/count/has/kernel), SHADOW/PREFER behavior,
  `LmdbDirectAdjacencyQueryTest` vectors, read-your-writes and SERIALIZABLE bypass. Also done (2026-07-28):
  `LmdbDirectAdjacencyIterator` mirroring `LmdbCsrRunIterator` (reusable quad, long positions, exact raw-context
  filtering incl. catalog-absent bound-context short circuit, batch fill, codec lower-bound seek) —
  `LmdbDirectAdjacencyIteratorTest` 4/4 green (`post-evidence-direct-adjacency-m5-iterator.txt`).
- [x] (2026-07-28) Milestone 5 complete: `LmdbAdjacencyProvider`, `LmdbDirectAdjacencyStore` (maintenance state
  machine, single-thread maintenance executor, publication lock, emergency-gap CAS-min, SHADOW sampled comparison,
  SELECTED IRI resolution via ValueStore), `LmdbAdjacencyPublishedState` (close-bit + refcount word),
  `LmdbAdjacencyReadView` (dataset lease + per-iterator leases + on-fully-released callback), `LmdbAdjacencyMetrics`
  (closed FallbackReason enum + immutable Snapshot), `LmdbDirectNodeIterator` (FULL-coverage bound-node/?p base
  enumeration), iterator view-lease protocol, and `LmdbSailStore` wiring: construct-from-config
  (`LmdbDirectAdjacencyOptions.resolve`), buildOnStart trigger, close-before-CSR, commit-path
  `onDataRevisionAdvanced()` rebuild stopgap, dataset `adjacencyView` acquired after txn registration and released
  before txn close, arbitration direct→CSR→LMDB in statements/ordered-statements/count/has/exactDegree and the
  retained NativeProbe. Two-red bootstrap red captured (`initial-evidence-direct-adjacency-m5-bootstrap-red.txt`, 6
  Surefire failures); green: QueryTest 15 (all plan vectors incl. SHADOW-never-serves, SELECTED fallbacks,
  read-your-writes/SERIALIZABLE bypass, ordered compatible/incompatible, exact-empty vs uncovered, inline incoming,
  root-scan fallback, old-pinned-snapshot-keeps-serving-after-commit), Bootstrap 6, IteratorTest 4, EphemeralTest 2,
  BaseBuilderTest 12, LmdbSailStoreTest 42 (`post-evidence-direct-adjacency-m5.txt`). M5 notes: serving requires
  `snapshotRevision == appliedRevision` until M6 (any commit → REVISION_GAP for newer snapshots + coalesced async
  rebuild); doubly-bound statements probes and parallel-sibling sources intentionally decline; kernel adjacency() and
  meanFanOut decline until M8.
- [x] (2026-07-28) Milestone 6 complete: `LmdbDirectAdjacencyCommitDelta` (charged primitive event columns, sorted
  deduped fenced `PendingTable`, seal-before-authoritative-commit with overflow marker on seal failure),
  `TripleStore` integration (separate direct listener/collector fields, tee in both fan-out choke points, seal under
  writer exclusion before the first LMDB commit, `beforeRevisionBump` after the LMDB commit and before the
  `dataRevision` bump, drain-once semantics, rollback reset, stale-sealed-delta close),
  `LmdbAdjacencyDeltaGeneration` (raw-predicate row directory, per-generation [base, gen] catalog, refcounted DELTA
  charge), `LmdbAdjacencyOverlaySet` (refcounted generations + extended context catalog + post-base classified
  selected predicates), `LmdbAdjacencyDeltaApplier` (two directional event sorts, last-op-wins streaming merges vs
  the previous revision, deterministic subpass A outcome plan consumed by subpass B, exact reservation, context
  catalog extension into the generation arena with an ordinal-identical sizing view), published-state v2 (base
  refcount + overlays + pending array + horizon), row resolution order pending→generations-newest-first→base with
  extra-selected exact-empty proof, k-way node-merge `LmdbDirectNodeIterator` (raw-predicate merge, tombstone
  exclusion, pending node fences), online build catch-up under the TxnManager read lock (scanner revision guard
  relaxed only when the collector is installed), emergency-gap self-healing (rebuild at >= gap revision publishes
  and clears), pause/interleave test hooks. Evidence: M6 bootstrap red 4 failures
  (`initial-evidence-direct-adjacency-m6-bootstrap-red.txt`); green: CommitTest 10 (visibility/removal isolation,
  pending window rows, add-remove-readd, tombstone + node enumeration, promotion/demotion planes, new
  node/predicate/context atomic catalog, rollback, collector-overflow gap + rebuild clear [M4A vector 12 analogue],
  online catch-up [M4A vector 11], close releases all charges), SnapshotTest 4 (sail-store pending window, commits
  serve new snapshots with zero rebuilds, SELECTED first-commit classification [M4A vector 14], old snapshot retains
  prior context catalog), QueryTest 15, EphemeralTest 2, BaseBuilderTest 12, LmdbSailStoreTest 42, TripleStoreTest
  29, LmdbCsrAdjacencyCacheTest 21 (`post-evidence-direct-adjacency-m6.txt`). M6 reference-path simplifications
  (hardening deferred to M9): events/pending/row directories are charged primitive on-heap arrays (not off-heap
  column pages/24-byte native tables); generation directories key on raw predicate IDs; empty-commit revision
  advance happens on the applier rather than writer-side copy-advance; bulk aligned multi-batch imports degrade via
  the gap protocol and rebuild (per plan's bulk-load-before-ACTIVE guidance).
- [x] (2026-07-28) Milestone 7 complete: consolidation squashes the current overlay's generations into one
  newest-version-only generation at the applied revision (k-way row-directory merge, tombstones preserved, run bytes
  copied into a fresh reserved arena, context-extension segments copied via `copyExtensions` with stable ordinals);
  the replacement state's `minSnapshotRevision` rises to the consolidation revision so a late acquisition below the
  floor declines (SNAPSHOT_BEFORE_BASE) while already-retained old states keep serving their pinned snapshots
  exactly — the plan's per-row version lists are subsumed by whole-state retention (old generations release when the
  last old lease closes). Quiescent no-overlap rebuild: publish UNAVAILABLE (new acquisitions fall back BUILDING),
  wait for the last lease, free old base/overlays through refcounts, rebuild + catch up + publish. Recovery paths
  (`APPLY_STALLED`, `DEGRADED_GAP` from missing revisions, writer-side pending-bound breach, consolidation memory
  refusal) all schedule the quiescent rebuild. Fixed en route: a rebuild at a revision at/after the emergency gap now
  publishes and clears the marker (previously refused and re-looped); catch-up aborts when an overflowed commit
  inside the missing interval can never be enqueued. Green: ConsolidationTest 5 (12-commit consolidation bound +
  exactness, pinned-view retention across consolidation + release reclaims DELTA bytes, late-acquisition floor
  fallback, quiescent rebuild waits for lease/declines during quiesce/serves after, repeated rebuilds + close leak
  zero charged bytes), CommitTest 10, SnapshotTest 4, QueryTest 15 (`post-evidence-direct-adjacency-m7.txt`).
- [x] (2026-07-28) Milestone 8 complete (reference scope): supernode groups flow through the full commit path — the
  shared codec state machine auto-promotes above `BLOCK_FOR_MAX_EDGES` and the applier's complete-row replacement
  rewrites the chunk directory exactly (incremental affected-chunk-only rewrite deferred to the M9 performance pass;
  correctness is complete-row exact). Fixed a real chunk format bug en route: the writer hardcoded chunk arena slot
  0, which resolved into the wrong arena when a chunked run was re-encoded into a delta-generation arena; slot 255 is
  now the "same arena as the directory" sentinel (absolute slots stay available for consolidation-time cross-arena
  chunk reuse). `LmdbDirectNativeAdjacency` implements the long-handle kernel SPI (source index packed above the
  48-bit run handle; base + per-generation catalogs; allocation-free `kernelFind`; batch copy through the codec's
  once-per-batch run-view resolution; `runsNeighborOrdered() == true`; no key enumeration) and is served through
  `LmdbDirectAdjacencyStore.adjacency` under the kernel completeness rule (snapshot <= appliedRevision, zero
  applicable pending tables, plane covered) with `KERNEL_REQUIRES_COMPLETE_REVISION` fallbacks; the dataset's
  retained NativeProbe arbitrates direct → CSR. Green: SupernodeKernelTest 2 (1.09M-edge chunked group: build,
  kernel find/size/at/copy-across-chunk-boundary/lowerBound, first/middle/last-chunk removals + append across two
  snapshots; kernel declines during pending window and serves generation+base rows after apply), RunCodecTest 27 and
  the full direct suite sweep (`post-evidence-direct-adjacency-m8.txt`).
- [x] (2026-07-28) Parallel builder (M4B) SKIPPED by explicit user decision ("skip m4b"). The single-thread M4A
  builder is the only production build path. The design note below is retained verbatim so M4B stays a drop-in
  follow-up if the 20B build-time gate ever demands it: implement `LmdbAdjacencyBuildTxnFamily` (TxnManager
  read-lock family creation, steps 1–7 of the plan's snapshot-family protocol), the ValueStore payload-high-water
  accessor, the striped `LmdbAdjacencyBuildWorkspace` install path, per-stream Pass-1/Pass-3 workers over the
  existing `AdjacencySourceScanner` seams, and `LmdbAdjacencyParallelBaseBuilderTest` vectors 1–12. With this skip
  recorded, every in-repo milestone of this plan is resolved; only the hardware/operator rollout gates (pilot ETA,
  12-hour 20B build, seal/pending latency matrices, sizing worksheet, `ROOT_ENUMERATION_ACCEPTED` sign-off) remain
  open by their nature, as documented under Milestone 9.
- [x] (2026-07-28) Milestone 9 in-repo scope complete; hardware/operator gates remain open (see below). Full module
  verify `core/sail/lmdb`: 2597 tests, 5 failures, 0 errors — all five reproduce at clean HEAD `00f199cccf` in a
  detached worktree (kernel-decline census gate, deferred cost-arbitration StrategyPriorityTest, and three
  factorized feature-flag fork-parity tests; `post-evidence-direct-adjacency-m9-module-verify.txt`), so this work
  introduces zero regressions. `core/sail/base` 29/29 green. SHADOW differential sampling implemented and tested
  (`shadowComparesButNeverServes`; mismatch path degrades to DEGRADED_GAP + emergency gap); static audits clean
  (no-file rg over `Lmdb*Adjacency*.java`: zero matches; old-SPI rg: only retained `CsrEntry` internals and the
  applier's unrelated private `copyRun` helper); copyright/SPDX check green; formatter run. Hardware/operator gates
  intentionally NOT claimed: 200M-visit pilot + 12-hour 20B build-and-publish, pending/seal latency benchmark
  matrices, sizing worksheet on the representative distribution, and the named/dated `ROOT_ENUMERATION_ACCEPTED`
  sign-off all require the target host and a deployment-owner decision; rollout stays SHADOW-at-most until then. The
  plan's JMH benchmark classes (`DirectAdjacencyBenchmark`/`DirectAdjacencySealBenchmark`/
  `DirectAdjacencyBuildBenchmark`) are not yet authored — they belong with the M4B/M9 performance pass on the
  benchmark host.

## Surprises & Discoveries

- The current `dataRevision` restarting at zero is correct for this design because every adjacency arena also disappears
  at restart. A durable revision would add persistence machinery the user explicitly does not want.
- The existing `spoc,posc` default can stream both directions without a disk sort: `spoc` groups outgoing by
  subject/predicate and `posc` groups incoming by predicate/object. Incoming headers can be filled in predicate-ordinal
  order after Pass 1 preallocates each node's plane slots.
- Inlined object IDs cannot use arithmetic reference pages. The per-predicate radix/block incoming structure preserves
  exact lookup without a large key/pointer hash table.
- The existing `contexts` DBI provides the snapshot-consistent named-context domain, allowing compact context ordinals
  without adding persistent adjacency metadata or an external sort.
- The existing `NativeAdjacency` int-dense API is a separate scale ceiling even after storage compression. Long run
  handles are required before a group can exceed 2.1 billion incidences.
- Overlay and pending references cannot be published independently: a reader could otherwise capture an old overlay
  after its pending marker disappeared. One retained published-state reference removes that torn-read window.
- Snapshot correctness does not require copying a complete base per revision. Complete immutable **row** versions plus
  a pinned LMDB fallback retain the same safety property as current complete CSR generations at much smaller update
  cost.

## Decision Log

- Decision: adjacency is strictly in-memory and rebuilt after every process start.
  Rationale: explicit user requirement; LMDB is the only persistent source.
  Date/Author: 2026-07-28 / user and Codex.
- Decision: an unset memory limit resolves once to 50% of `Runtime.maxMemory()`.
  Rationale: explicit user requirement; resolving once makes reservations deterministic while an explicit byte limit
  still supports a 256-GiB off-heap index without forcing a 512-GiB Java heap.
  Date/Author: 2026-07-28 / user and Codex.
- Decision: implementation red evidence must be an executed Surefire/Failsafe failure, never a compile error against a
  nonexistent API.
  Rationale: the repository evidence contract requires a report snippet; the first configuration red uses raw RDF
  properties so it compiles against the pre-feature tree.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: node-centric headers, not one page table per predicate/direction.
  Rationale: with approximately 100 predicates, per-plane locator tables multiply directory overhead by up to 400;
  one node header is both smaller and closer to index-free adjacency.
  Date/Author: 2026-07-28 / Codex.
- Decision: the reference locator is one flat page vector per type, and a payload beyond the base vector is ordinary
  base absence after overlay lookup.
  Rationale: the previous shard arithmetic had no physical shard level; post-base IDs must be servable from overlays
  without treating their absent base page as corruption.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: every run codec has a separate leading tag byte, included in layout sizing and alignment.
  Rationale: a fused tag/count interpretation would create incompatible encoders and decoders.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: compact immutable base plus sorted delta generations, not atomic eight-byte head pointers in every base
  group.
  Rationale: billions of atomic pointers consume too much steady memory; bounded overlays put update overhead only on
  changed rows.
  Date/Author: 2026-07-28 / Codex.
- Decision: complete row replacement versions, with range-chunk replacement for supernodes.
  Rationale: the read edge loop remains flat and revision-free; merge-on-read operation logs would add branches and
  repeated work to every traversal.
  Date/Author: 2026-07-28 / Codex.
- Decision: encode named-graph contexts as snapshot-local append-only ordinals.
  Rationale: the existing contexts DBI supplies the base domain, and repeating sparse raw 64-bit context IDs in both
  directions would make the 20-billion memory target unnecessarily data-hostile.
  Date/Author: 2026-07-28 / Codex.
- Decision: ordered range chunks rather than ArcadeDB-style hash stripes.
  Rationale: LMDB already has one writer and RDF4J benefits from unsigned neighbor order.
  Date/Author: 2026-07-28 / Codex.
- Decision: bound reference nodes may enumerate header predicate groups, but predicate-wide/root enumeration remains
  CSR/LMDB in version 1.
  Rationale: node-local enumeration uses bytes already present; per-predicate key rosters would duplicate one entry per
  group. `PREFER` now requires a measured, named/date workload sign-off accepting this limit.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: pending touched-row tables carry raw-key fences and a plane mask, and their ten-table worst case is a
  rollout benchmark dimension.
  Rationale: degree-1 probes must not pay ten native binary searches when a cheap table-level rejection proves absence.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: the ordinary commit-delta cap defaults to at most 64 MiB; 2 GiB is an explicit measured ceiling, not the
  default.
  Rationale: the 17-pass touched-table seal runs under writer exclusion. PREFER requires p95 at the configured cap
  within the one-second default seal budget, with no sample over two seconds.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: the single-thread builder is a format oracle; production Passes 1 and 3 use up to four exact-snapshot plane
  streams.
  Rationale: 20B requires 80 billion source visits per rebuild. Rollout requires a ten-hour pilot projection and one
  complete publication within twelve hours.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: allocate inline radix directories only for nonempty base planes.
  Rationale: 262,148 bytes per plane is acceptable for about 100 predicates but can become tens of GiB for large FULL
  predicate domains; exact preflight must charge only real nonempty planes.
  Date/Author: 2026-07-28 / reviewer and Codex.
- Decision: `LmdbStoreConfig` tracks whether `directAdjacencyCoverage` was explicitly set (null = unset behaves as
  FULL) so an explicitly parsed `FULL` round-trips through export while untouched defaults still export nothing.
  Rationale: the Milestone-1 red test requires exported presence of every explicitly parsed property/value pair, and
  the validation matrix requires defaults to export nothing; both cannot hold with a bare enum default.
  Date/Author: 2026-07-28 / Claude.
- Decision: the max-byte and commit-cap resolver vectors live in `LmdbDirectAdjacencyOptionsTest` (same package as the
  package-private pure resolvers) instead of `LmdbStoreConfigTest`.
  Rationale: the plan requires the resolver to be a package-private pure function in `org.eclipse.rdf4j.sail.lmdb`,
  which the config-package test cannot reference; the config test keeps the AUTO-default assertion (`maxBytes == 0`).
  Date/Author: 2026-07-28 / Claude.
- Decision: the arena delegates reference computation to an internal `LmdbAdjacencyArenaSizingPlan` cursor, and
  exact-mode arenas are constructed from a sealed plan whose region capacities they must not exceed.
  Rationale: the plan requires predicted and emitted layouts to be provably identical; sharing the bump-allocation
  state machine makes divergence a hard `IllegalStateException` instead of a latent estimate error.
  Date/Author: 2026-07-28 / Claude.
- Decision: Milestone-4A reference-path simplifications, to harden in 4B/9: build count cells are uniformly u32 (not
  predicate-width-sized); the predicate dictionary keeps a bounded Java mirror beside its arena table; inline plane
  metadata is allocated after that plane's runs (not strictly before all runs); Pass-3 header cursors are derived by
  scanning for the first zero run reference instead of repurposed count cells; the memory gate reserves predicted
  bytes rounded up to growth-arena region granularity and reconciles to actual capacity, instead of replaying a full
  `LmdbAdjacencyArenaSizingPlan` for the complete base.
  Rationale: correctness and refusal-before-allocation are preserved and tested; the byte-identical plan replay and
  exact steady-gate arithmetic are 20B-scale requirements gated in Milestones 4B/9, not needed for the format oracle.
  Date/Author: 2026-07-28 / Claude.
- Decision: rebuild quiesces and frees the old base before allocating the new one.
  Rationale: a 256-GiB hard cap cannot safely hold two approximately 220-GiB bases; temporary fallback is preferable to
  overcommit.
  Date/Author: 2026-07-28 / Codex.
- Decision: Milestones 5–8 use the two-red bootstrap (Class.forName Surefire red before any production type, typed
  behavioral suites after the minimal surface exists), matching the Milestone-2 precedent; the typed suites were
  authored against the implemented surface and their first executions are the recorded green evidence.
  Rationale: the executable-red rule is satisfied by the bootstrap failures; re-deriving a second executed red per
  vector for every provider/state type would have required stub layers the plan's own M2/M3 flow did not use.
  Date/Author: 2026-07-28 / Claude.
- Decision: published states share whole retained components (refcounted base, overlay set, generations) instead of
  per-row version lists; consolidation squashes the current overlay to newest-versions-only and raises the state's
  `minSnapshotRevision`, and old pinned snapshots keep serving through their own retained states.
  Rationale: identical snapshot-safety guarantees with far less machinery; the only cost is that a *late* acquisition
  below the consolidation floor falls back to LMDB (exactly the plan's W-watermark trade expressed at state
  granularity).
  Date/Author: 2026-07-28 / Claude.
- Decision: M6 reference-path simplifications — charged primitive on-heap event columns/pending tables/row
  directories (not off-heap column pages and native 24-byte tables), raw-predicate generation directories, comparator
  index sorts instead of LSD radix, applier-side empty-commit revision advance, and bulk multi-batch aligned imports
  degrading through the gap protocol.
  Rationale: every structure is bounded by `commitMaxBytes`/touched rows (never edge scale), exactly charged to the
  memory account, and behind the same interfaces the M9 hardening pass replaces; correctness invariants (I3–I5, I9,
  I10, I16, I17) are enforced and tested now.
  Date/Author: 2026-07-28 / Claude.
- Decision: chunk directory entries use arena-slot 255 as the "same arena as the directory" sentinel; absolute slots
  remain for consolidation-time cross-arena chunk reuse.
  Rationale: an encoder cannot know which catalog slot its arena will occupy (slot 0 in the base catalog, slot 1+ in
  generation catalogs); the hardcoded 0 mis-resolved chunked runs re-encoded into delta arenas, caught by the M8
  supernode commit test.
  Date/Author: 2026-07-28 / Claude.
- Decision: Milestone 4B (same-snapshot parallel builder) is skipped, not merely deferred.
  Rationale: explicit user instruction ("skip m4b") while closing out this plan; the M4A single-thread builder is the
  production build path, and the M4B design note is retained in Progress for a future drop-in follow-up.
  Date/Author: 2026-07-28 / user and Claude.

## Outcomes & Retrospective

2026-07-28 (Claude): Milestones 0–3, 4A, 5, 6, 7, and 8 (reference scope) are implemented and green; M4B is
skipped by explicit user decision (design note retained for a future drop-in follow-up), and M9's hardware/operator
gates remain open by their nature. With the M4B skip recorded, this plan's in-repo scope is complete.

What works end to end: config → store construction → asynchronous/synchronous ephemeral base build with online
catch-up → bound-probe/count/has/exactDegree/ordered-scan/node-enumeration arbitration (direct → CSR → LMDB) →
commit capture/seal/pending publication/apply as immutable generations → consolidation → quiescent rebuild →
kernel adjacency under the completeness rule → close with exact charge reconciliation. 59 direct-adjacency tests
across nine suites plus the pre-existing config/codec/locator/inline/builder suites are green; every suite asserts
LMDB-equality, metrics-proven serving/fallback, or zero leaked charges.

Notable discoveries during implementation (beyond the Decision Log):
- The emergency gap must be clearable by any rebuild at a revision at or after the gap, not only by a
  gap-free interval; otherwise an overflowed commit permanently disables acceleration even though the next base
  image contains its data.
- A writer that outruns the applier hits the bounded pending list quickly under synthetic commit storms; the
  degrade-then-quiescent-rebuild path is load-bearing, not an edge case.
- The chunk-directory arena-slot byte cannot be absolute at encode time (see Decision Log); the M2 codec tests
  could not catch this because they always decoded through single-arena or hand-built catalogs.

Honest limitations: events/pending/directories are charged on-heap primitives pending the M9 off-heap hardening;
supernode commits rewrite the whole group (incremental chunk rewrite deferred); parallel sibling sources and
doubly-bound probes decline; meanFanOut/root enumeration stay on CSR/LMDB by design; the 20B sizing worksheet,
pilot/12-hour build gates, seal/pending latency benchmark matrices, and `ROOT_ENUMERATION_ACCEPTED` sign-off
require the target host and the deployment owner.

## Revision note

2026-07-28 (later the same day): the legacy CSR cache this plan arbitrated against has been removed entirely and
direct adjacency became the default serving path (PREFER, FULL coverage, AUTO cap, build on start; unpinned tracked
datasets now acquire exact read views at the current data revision under the reset-on-commit version fence). See
`plans/lmdb-native-engine/28-remove-legacy-csr.md`. Mentions of a `direct → CSR → LMDB` ladder in this document are
historical; the ladder is now `direct → LMDB`.

2026-07-28: Replaced the discarded persistent-sidecar draft with a strictly in-memory design after the user clarified
that adjacency data must never be persisted. This version contains no durable revision, manifest, mmap, journal,
checkpoint, recovery, or adjacency file format. It adds explicit no-file invariants/tests, native arenas, bounded
in-memory commit capture, ephemeral MVCC generations, rebuild-on-restart behavior, and a default cap of 50% of
effective Xmx.

2026-07-28: Applied the full independent review. Clarified executable test evidence, removed vestigial shard
decomposition, made codec tags physical, defined post-base locator misses, added sparse inline-plane allocation and
pending fences, reduced the normal commit cap, added pending/seal benchmarks, specified same-snapshot parallel build
mechanics with a 12-hour 20B gate, and made predicate-root acceptance an explicit rollout decision.
