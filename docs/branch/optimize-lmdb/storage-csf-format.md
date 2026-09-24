# Paged CSF adjacency base and page format

The direct adjacency base in this branch is the immutable paged compressed sparse fiber representation (`CSF`). It is derived in process memory from a pinned TripleStore snapshot; the current implementation does not serialize these pages into the repository. Paged CSF is the base representation for every enabled direct-adjacency index in this branch. The durable statement indexes remain authoritative. Main source anchors are [`LmdbPagedCsfBaseBuilder`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbPagedCsfBaseBuilder.java), [`ImmutablePagedQuadCsfIndex`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/ImmutablePagedQuadCsfIndex.java), [`CompactCsfPageFormat`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageFormat.java), [`CompactCsfPageEncoder`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageEncoder.java), and [`CsfShard`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CsfShard.java).

**Branch delta and prerequisites:** this branch makes paged CSF the direct-adjacency base and removes the former native-arena/CSR base path. The four planes are still derived from existing explicit/inferred statement B-trees, so the reader must preserve their ordering, context, and multiplicity semantics while encoding a new in-memory page representation.

## Four logical planes

The base holds four independent predicate-keyed planes:

| Plane | Row key | Neighbor | Statement set |
| --- | --- | --- | --- |
| Outgoing explicit | Subject ID | Object ID | Explicit statements |
| Incoming explicit | Object ID | Subject ID | Explicit statements |
| Outgoing inferred | Subject ID | Object ID | Inferred statements |
| Incoming inferred | Object ID | Subject ID | Inferred statements |

Within each row, the builder stores distinct neighbor IDs and the raw context IDs that connect that neighbor to the row. Default context is represented by raw context ID zero in scanner callbacks. The page's fiber counts describe row-to-neighbor structure; quad counts retain multiplicity from contexts. For example, subject `s` with `p` to `o` in two contexts contributes one outgoing `(p,s)->o` fiber but two quad/context edges; the incoming plane contains the corresponding `(p,o)->s` fiber. These IDs are raw store IDs, and row/neighbor/context ordering is unsigned. The scanner has distinct index-prefix requirements: `supportsOrderedScan` requires an `sp` prefix for outgoing and `po` for incoming, while `supportsPredicatePartitionedScan` requires `ps` for outgoing and `po` for incoming. Missing the needed prefix makes that scan capability unavailable; in particular, incoming support is predicate-object (`po`), not object-leading. See [`LmdbAdjacencyPlane`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyPlane.java) and [`LmdbAdjacencyTripleStoreScanner`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyTripleStoreScanner.java#L98).

## Build and memory admission

The builder has a sizing/planning phase before native materialization. It carries predicate/context dictionaries, walks each sorted source plane, computes row/fiber/context counts and page splits, and derives exact page capacity classes. It reserves native page/table bytes and modeled retained Java metadata before the materialization pass. If the reservation cannot be made, the index cannot become a partially published base. The build reads through a pinned LMDB snapshot. Snapshot validity checks prevent a resized/rebound read transaction from being used as if it still represented the original view; online catch-up is allowed only after the commit collector is installed.

The builder's workspace can exceed its final base footprint: source batches, planned ranges, predicate/context dictionaries, sizing tables and worker scratch exist during construction and are accounted in the adjacency memory categories. Memory accounting therefore distinguishes `BUILD_COUNTERS`, `BUILD_OUTPUT`, and `PREPARATION_OUTPUT` from the published `BASE`. The reservation model includes modeled Java metadata and is not JVM RSS. Temporary build charge transfers or releases at publication/abort; output pages remain charged while retained by a published state, a shard dependency, or a reader lease.

## `PCSF` page header (version 4)

`CompactCsfPageFormat` defines numeric magic `0x50435346` (the `PCSF` mnemonic when read most-significant byte first; the page contains bytes `46 53 43 50`, or ASCII `FSCP`, because the integer is written little-endian), page format version `4`, format hash `0x2dc970b6`, and a fixed 160-byte header. Integer fields use the page format's little-endian accessors; raw IDs in the page are 64-bit unsigned-order values. The fields through offset 79 are the page descriptor and range summary; the aligned payload sections follow.

| Offset | Width | Field |
| ---: | ---: | --- |
| 0 | 4 | Magic `PCSF` |
| 4 | 2 | Format version |
| 6 | 2 | Flags |
| 8, 12 | 4 each | Allocated page capacity; used bytes |
| 16, 20 | 4 each | Global page ID; next continuation extent ID or sentinel |
| 24, 28, 32, 36 | 4 each | Row count; neighbor-fiber count; quad/context-edge count; reserved |
| 40, 48 | 8 each | First row ID; last row ID |
| 56, 64 | 8 each | First neighbor ID; last neighbor ID |
| 72 | 8 | Common context ID when flagged |
| 80–143 | Eight pairs of 4-byte offset/length | Row IDs; row-to-fiber starts; per-row quad counts; first neighbors; neighbor tails; per-neighbor context counts; first contexts; context tails |
| 144 | 4 | Format hash |
| 148, 152, 156 | 4 each | Optional slack accelerator offset, length, and descriptor |

All eight section payloads are 8-byte aligned. A zero-length optional vector has no payload and is interpreted from flags/row summaries. The encoder checks offset/length, count, page identity and format hash when binding an untrusted page. Page capacity may be larger than `used` because the native page allocator groups pages into deterministic size classes. The format hash is a layout guard, not a cryptographic content checksum.

### Flags

| Bit | Flag | Meaning |
| ---: | --- | --- |
| 0 | Continuation | This physical page continues a logical row from the preceding extent. |
| 1 | Row-neighbor-one | Each row has exactly one neighbor. |
| 2 | Row-quad-equals-neighbor | Each row has one context/quad per neighbor. |
| 3 | Context-count-one | Each neighbor has exactly one context. |
| 4 | Common-context | The page uses the header's common context ID. |
| 5 | All ordered-integer neighbors | Neighbor IDs are all the ordered-integer inline family. |
| 6–9 | Uniform row/neighbor term kind or literal datatype | Optional uniform-value metadata for the row or neighbor axis; flags separate term-kind and literal-datatype uniformity. |

These flags let a reader infer a whole-page property without decoding every ID. When the property is not established, the relevant vector or regular ID decode remains authoritative; the flag is not permission to reinterpret a heterogeneous page.

## Payload vectors and packed integer encoding

`firstRowId` and `lastRowId` in the header are range summaries; the `rowIds` vector still contains all `rowCount` row IDs, including the first. `rowFiberStarts` gives the first fiber ordinal for each row when needed; `rowQuadCounts` preserves the number of statement/context tuples represented by a row. `firstNeighbors` contains one first-neighbor ID per row, so its length is `rowCount`, not `fiberCount`. `neighborTails` contains each row's remaining neighbor IDs as successive unsigned differences (`current - previous`); prefix-summing those deltas from that row's first neighbor reconstructs the neighbor sequence. The CSF serializer emits it in this form. `contextCounts` count contexts per neighbor, and `firstContexts`/`contextTails` encode per-fiber contexts unless the page's `COMMON_CONTEXT` flag collapses them.

Each `PackedLongVector` has an 8-byte vector header and independently decodable blocks of 256 values. Its offset directory has one 32-bit start per block plus terminal offset; each encoded block has a 12-byte block header followed by its bit-packed payload. Modes are FOR raw, FOR typed-payload, delta raw, and delta typed-payload. Hint-specific optional metadata can add a block-local radix directory for sorted random access and cumulative prefix sums for counts/deltas; the encoded block itself remains independently decodable. `READ_TAIL_PADDING` is an 8-byte allowance required by the decoder's native access contract. A vector's total bytes are not generally just `count * 8`: encoded width, number of blocks, selected indexes and terminal metadata determine it.

For a simple worked example, a row with unsigned IDs `s=100`, neighbors `o=8,11,20`, and contexts `[0,4]` for `8`, `[0]` for `11`, and `[2]` for `20` has 1 row, 3 fibers and 4 quads. `rowIds=[100]`, `firstNeighbors=[8]`, and `neighborTails=[3,9]`; prefix sums reconstruct neighbors `8,11,20`. Context counts are `[2,1,1]`, with the first context and any tails encoded per fiber. The actual bytes depend on vector plans, flags, headers, padding and allocator class and should be obtained from source stats rather than this conceptual example.

Large rows can exceed the per-page row/byte target. The splitter preserves a continuation chain keyed by the same row; later extent pages carry the continuation flag and are linked by extent ID. A continuation is not a second logical row. The builder/index tracks its exact row ordinal and contributes the chain's aggregate counts once. A physical split is allowed to bound individual page size; it does not alter the row/fiber/quad semantics.

## Shards, allocation classes, and retained-page ownership

`CsfShard` supplies a native directory with a 64-byte header and 32-byte entry per page (direct page address and row/range metadata), bounded to at most 4,096 pages per shard. A shard can reference unchanged pages from prior versions while storing newly materialized pages itself. Lookups use direct directory addresses rather than traversing the copy-on-write ownership chain. Reference-counted dependencies retain old page owners while a newer shard shares their pages. Periodic shard-local flattening copies the current page set when chain depth or retained obsolete capacity crosses configured thresholds. Source defaults are maximum chain depth 32, maximum retained-waste percentage 25%, and minimum retained-waste threshold 4 MiB; these system properties are resolved when `CsfShard` initializes.

Native CSF pages are allocated by deterministic size classes: 256-byte granularity from 256 bytes to 64 KiB (256 classes). Each page uses the first capacity class that holds its measured image. Classes allocate slabs up to 1 GiB, with an exact final tail. This avoids one native allocation per page, but the class capacity can exceed page `used` bytes. Directory/page capacity and actual encoded `used` bytes are separate values. A shard models 192 Java bytes for its own persistent object/accounting; this is not a claim about total Java heap/RSS per page.

Optional cursor-owned heap accelerators are not part of retained CSF. `CsfAdaptiveMemory` starts enabled unless `rdf4j.lmdb.csf.adaptiveAccelerators.enabled=false`; defaults are minimum heap headroom `max(32 MiB, maxHeap/64)`, maximum one object 1 MiB and total claimed bytes `max(64 MiB, min(1 GiB, maxHeap/32))`. It resolves these static properties on first class initialization. A cursor builds an accelerator only after repeated work; failed admission or allocation abandons this optional fast path. The per-array admission check is separately bounded, so concurrent admissions can briefly exceed the observed headroom threshold only by a thread-count-bounded amount.

Keep these measures separate: exact allocated native slabs/pages, logical referenced page capacity, modeled Java metadata, transient build workspace, cursor-local heap accelerators, LMDB maps/page cache, and disk. The adjacency ledger accounts native and modeled Java derived-index ownership, but does not measure operating-system resident set size.

## Lookup cost and compatibility

The page summaries bound navigation to candidate page/shard ranges; sorted row-ID vectors and optional block radix data narrow row lookup; row-fiber offsets then identify that row's neighbor span. Context cursors decode only the applicable neighbor/context vectors. A high-degree row can touch multiple continuation pages. Construction performs source scans, sizing, encoding and native allocations proportional to rows/fibers/quads and selected coverage; on-demand cursor materialization allocates reusable row/block buffers rather than one RDF `Statement` per edge. These are algorithm/data-layout observations, not measured speedup claims.

Page-format version `4` is in-memory derived-state compatibility only. If the encoder or vectors change, update version/hash validation and the structural tests together. Do not change field offsets/flag meaning while leaving `FORMAT_HASH` the same. Changing durable statement IDs, index keys or store metadata is a separate migration topic covered in [value IDs and record formats](storage-values-and-records.md).

Related contracts: [`CompactCsfPageTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageTest.java) exercises header fields, vector encoding, flags and malformed-page rejection; [`ImmutablePagedQuadCsfIndexTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/csf/ImmutablePagedQuadCsfIndexTest.java) covers shard routing and retained-page accounting; and [`PackedLongVectorTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/csf/PackedLongVectorTest.java) covers packed vector access. This task did not run them.
