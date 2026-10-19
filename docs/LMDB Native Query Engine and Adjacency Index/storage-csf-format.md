# Paged CSF adjacency base and page format

The direct adjacency base in this branch is the immutable paged compressed sparse fiber representation (`CSF`). It is derived in process memory from a pinned TripleStore snapshot; the current implementation does not serialize these pages into the repository. Paged CSF is the base representation for every enabled direct-adjacency index in this branch. The durable statement indexes remain authoritative. A CSF page groups rows, each row groups distinct neighbors, and each neighbor groups the contexts in which that edge occurs. Its byte image stores those three levels in compressed columns rather than repeating a complete statement record for every context.

Main source anchors are [`LmdbPagedCsfBaseBuilder`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbPagedCsfBaseBuilder.java), [`ImmutablePagedQuadCsfIndex`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/ImmutablePagedQuadCsfIndex.java), [`CompactCsfPageFormat`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageFormat.java), [`CompactCsfPageEncoder`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageEncoder.java), [`CompactCsfPageReader`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageReader.java), and [`CsfShard`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CsfShard.java). The examples below describe the current source statically; their byte calculations are explanatory examples, not captured output from a running store.

**Branch delta and prerequisites:** this branch makes paged CSF the direct-adjacency base and removes the former native-arena/CSR base path. The four planes are still derived from existing explicit/inferred statement B-trees, so the reader must preserve their ordering, context, and multiplicity semantics while encoding a new in-memory page representation.

## Four logical planes

The base holds four independent predicate-keyed planes:

| Plane | Row key | Neighbor | Statement set |
| --- | --- | --- | --- |
| Outgoing explicit | Subject ID | Object ID | Explicit statements |
| Incoming explicit | Object ID | Subject ID | Explicit statements |
| Outgoing inferred | Subject ID | Object ID | Inferred statements |
| Incoming inferred | Object ID | Subject ID | Inferred statements |

Each page belongs to one `(predicate ordinal, plane)` partition. Predicate selection happens outside the page; the header does not contain a predicate ID. Within a partition, rows are strictly ascending by unsigned raw ID. Within a row, `(neighbor ID, context ID)` pairs are strictly ascending, so a neighbor occurs once as a fiber and its distinct contexts are grouped underneath it. Default context is raw ID zero. This is ID order, with no promise of RDF lexical or SPARQL value order.

Consider these explicit statements:

```trig
@prefix : <urn:example:> .

:alice :knows :bob, :carol .
:bob :label "Bob" .
:carol :label "Carol" .
GRAPH :team { :alice :knows :bob }
```

The outgoing explicit partition for `:knows` has this logical shape:

```text
predicate :knows, plane outgoing explicit
  row :alice
    neighbor :bob   -> contexts [default, :team]
    neighbor :carol -> contexts [default]
```

It has **1 row, 2 neighbor fibers, and 3 quad/context edges**. The incoming partition for the same predicate has two rows: `:bob -> :alice` with `[default, :team]`, and `:carol -> :alice` with `[default]`. It has **2 rows, 2 fibers, and 3 quads**. `:label` lives in separate predicate partitions. Explicit rows do not populate inferred planes unless inferred statements also exist in the source store.

For example, reading all contexts for outgoing `(:knows, :alice)` yields pairs `(:bob, default)`, `(:bob, :team)`, `(:carol, default)`. A fiber-oriented consumer can instead receive neighbors `[:bob, :carol]` and weights `[2, 1]`. The weight records context multiplicity; the caller still applies the query's dataset/context semantics. A restriction to stored context ID zero sees the two default-context pairs. A `GRAPH :team` restriction sees only `(:bob, :team)`. The SPARQL default graph can merge stored contexts depending on the dataset; see [dataset and context handling in physical access](query-physical-access.md).

The scanner has distinct index-prefix requirements: `supportsOrderedScan` requires an `sp` prefix for outgoing and `po` for incoming, while `supportsPredicatePartitionedScan` requires `ps` for outgoing and `po` for incoming. Missing the needed prefix makes that scan capability unavailable; incoming support is predicate-object (`po`). See [`LmdbAdjacencyPlane`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyPlane.java) and the two capability methods in [`LmdbAdjacencyTripleStoreScanner`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyTripleStoreScanner.java).

### A page with two rows and all eight columns

To expose the offsets and resets, use a separate synthetic page for one predicate and one outgoing plane. Synthetic IRI IDs follow `IRI(n) = (n << 7) | 2`; for example `IRI(1)=130` and `IRI(2)=258`. This only illustrates the existing compound-ID layout. Actual dictionary assignment depends on the store, and these numbers imply no lexical ordering. Context zero still means the default context; `770` and `898` are synthetic named-graph IRI IDs.

```text
row 130
  neighbor 386 -> contexts [0, 770]
  neighbor 514 -> contexts [0]
  neighbor 642 -> contexts [898]
row 258
  neighbor 130 -> contexts [0]
  neighbor 514 -> contexts [0, 770]
```

The page has `rowCount=2`, `fiberCount=5`, and `quadCount=7`. Its logical columns are:

| Column | Values | Meaning |
| --- | --- | --- |
| `rowIds` | `[130, 258]` | Every row ID, including the first one summarized in the header. |
| `rowFiberStarts` | `[0, 3]` | Row 0 owns fibers `[0,3)`; row 1 owns `[3,5)`. The final bound is header `fiberCount=5`. |
| `rowQuadCounts` | `[4, 3]` | Number of context edges for each row. These are counts, not offsets. |
| `firstNeighbors` | `[386, 130]` | One first neighbor per row; this column need not be sorted across rows. |
| `neighborTails` | `[128, 128, 384]` | Within-row differences: `514-386`, `642-514`, then `514-130` for row 1. |
| `contextCounts` | `[2, 1, 1, 1, 2]` | Number of contexts per fiber. |
| `firstContexts` | `[0, 0, 898, 0, 0]` | One first context per fiber. |
| `contextTails` | `[770, 770]` | Differences for the two fibers with a second context: each is `770-0`. |

Neighbor differences restart from the next row's `firstNeighbors` value. There is no stored difference from row 0's last neighbor `642` to row 1's first neighbor `130`. Context differences likewise restart at each fiber's first context. These columns are produced by `CompactCsfPageEncoder.VectorValues` and reconstructed by `CompactCsfPageReader`.

To access row `258`, `findRow` searches `rowIds` and finds page-local row index `r=1`. Its fiber start is `s=3`, its end is the page's terminal fiber count `5`, and its neighbor-tail start is `s-r=2`. Therefore `neighborAtFiber(1,0)=130` and `neighborAtFiber(1,1)=130+neighborTails[2]=514`. Fiber 4 starts at flattened context ordinal `2+1+1+1=5`; its context-tail start is `5-4=1`, so its second context is `firstContexts[4]+contextTails[1]=770`.

Row-local **quad ordinals** expand contexts, whereas local **fiber ordinals** select distinct neighbors:

| Row | Quad ordinal | Fiber ordinal | `(neighbor, context)` |
| --- | ---: | ---: | --- |
| `130` | 0 | 0 | `(386, 0)` |
| `130` | 1 | 0 | `(386, 770)` |
| `130` | 2 | 1 | `(514, 0)` |
| `130` | 3 | 2 | `(642, 898)` |
| `258` | 0 | 0 | `(130, 0)` |
| `258` | 1 | 1 | `(514, 0)` |
| `258` | 2 | 1 | `(514, 770)` |

For row `130`, `lowerBound(0, 514, 0)` returns quad ordinal `2`. For row `258`, `lowerBound(0, 514, 770)` returns `2`. `seekFollowingFiber` can reach the latter by subtracting the first fiber's one context and retaining a skip of one context inside the second fiber. Sequential copying carries the previously decoded neighbor and consumes only new differences. These methods provide exact pair order; a neighbor lookup alone does not require decoding context IDs.

## Build and memory admission

The builder has a sizing/planning phase before native materialization. It carries predicate/context dictionaries, walks each sorted source plane, computes row/fiber/context counts and page splits, and derives exact page capacity classes. It reserves native page/table bytes and modeled retained Java metadata before the materialization pass. If the reservation cannot be made, the index cannot become a partially published base. The build reads through a pinned LMDB snapshot. Snapshot validity checks prevent a resized/rebound read transaction from being used as if it still represented the original view; online catch-up is allowed only after the commit collector is installed.

The builder's workspace can exceed its final base footprint: source batches, planned ranges, predicate/context dictionaries, sizing tables and worker scratch exist during construction and are accounted in the adjacency memory categories. Memory accounting therefore distinguishes `BUILD_COUNTERS`, `BUILD_OUTPUT`, and `PREPARATION_OUTPUT` from the published `BASE`. The reservation model includes modeled Java metadata and is not JVM RSS. Temporary build charge transfers or releases at publication/abort; output pages remain charged while retained by a published state, a shard dependency, or a reader lease.

## `PCSF` page header (version 4)

`CompactCsfPageFormat` defines numeric magic `0x50435346` (the `PCSF` mnemonic when read most-significant byte first; the page contains bytes `46 53 43 50`, or ASCII `FSCP`, because the integer is written little-endian), page format version `4`, format hash `0x2dc970b6`, and a fixed 160-byte header. All multi-byte header/vector/block fields use little-endian accessors. IDs are 64-bit bit patterns compared unsigned; the compressed representation below can use fewer bits per value. Every offset in this table is a decimal offset from the beginning of the page.

| Offset | Width | Field |
| ---: | ---: | --- |
| 0 | 4 | Magic `PCSF` |
| 4 | 2 | Format version |
| 6 | 2 | Flags |
| 8, 12 | 4 each | Allocated page capacity; used bytes |
| 16, 20 | 4 each | Encoder page ID; next-extent field (current encoder writes `NO_PAGE=-1`) |
| 24, 28, 32, 36 | 4 each | Row count; neighbor-fiber count; quad/context-edge count; reserved |
| 40, 48 | 8 each | First row ID; last row ID |
| 56, 64 | 8 each | First physical fiber's neighbor; last physical fiber's neighbor |
| 72 | 8 | Common context ID when flagged |
| 80–143 | Eight pairs of 4-byte offset/length | Row IDs; row-to-fiber starts; per-row quad counts; first neighbors; neighbor tails; per-neighbor context counts; first contexts; context tails |
| 144 | 4 | Format hash |
| 148, 152, 156 | 4 each | Optional slack accelerator offset, length, and descriptor |

Each present section starts at an 8-byte-aligned page offset. Its recorded length is the exact packed-vector byte length, which may not be a multiple of eight. The next section starts after alignment padding. An omitted section has both offset and length zero; its meaning is inferred from flags or terminal counts. `CompactCsfPageReader.validatePage` checks header invariants, section bounds/counts, packed layout and accelerator descriptors before binding an untrusted page; shard-owned validated pages can subsequently use `bindTrusted`.

`firstRow` and `lastRow` bound the sorted rows. `firstNeighbor` and `lastNeighbor` are endpoints of the flattened physical fiber sequence, **not global minimum/maximum neighbors on a multi-row page**. In the two-row example they are `386` and `514`, although neighbor `130` is also present. For a single-row extent the neighbors are sorted and these endpoints also bound that extent's neighbor range. The format hash guards the layout; it is not a cryptographic content checksum.

Header page IDs and physical navigation also need to be distinguished. The current accumulator measures pages with encoder page ID zero, while the immutable root assigns global page positions through shard/page directories. Row references and extent traversal use those root positions. The byte example below chooses page ID `7` to demonstrate the header field; it does not imply every materialized page carries its root position in that field.

### Flags

| Bit | Flag | Meaning |
| ---: | --- | --- |
| 0 | Continuation | This physical page continues the same logical row; physical `rowCount` is one. |
| 1 | Row-neighbor-one | Each row has exactly one neighbor; omit `rowFiberStarts`, infer start `r`. |
| 2 | Row-quad-equals-neighbor | Each row's quad count equals its fiber count; omit `rowQuadCounts`. |
| 3 | Context-count-one | Each neighbor has exactly one context; omit `contextCounts`. |
| 4 | Common-context | Every fiber has one identical context; omit `firstContexts` and use header `commonContext`. |
| 5 | All ordered-integer neighbors | Neighbor IDs are all the ordered-integer inline family. |
| 6 | Uniform row term kind | All rows have the same RDF term kind. |
| 7 | Uniform row literal datatype | All literal rows have the same established datatype; other row kinds may coexist. |
| 8 | Uniform neighbor term kind | All neighbors have the same RDF term kind. |
| 9 | Uniform neighbor literal datatype | All literal neighbors have the same established datatype; other neighbor kinds may coexist. |

These are whole-physical-page properties. `COMMON_CONTEXT` requires `CONTEXT_COUNT_ONE`; a neighbor with two contexts cannot use the common-context shortcut. `neighborTails` is omitted when `fiberCount==rowCount`, and `contextTails` is omitted when `quadCount==fiberCount`. `rowIds` and `firstNeighbors` are always present. Thus a singleton edge in one graph needs only two vectors. Mixed datatypes or unresolved legacy literal datatype lookups prevent the applicable datatype flag; a literal-datatype flag alone does not mean the entire axis contains literals. See `columnTraitFlags` in the encoder and the uniformity tests in [`CompactCsfPageTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageTest.java).

## Payload vectors and packed integer encoding

The page columns above each use [`PackedLongVector`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/PackedLongVector.java). This adds a second compression layer: a column's logical values, such as neighbor differences `[128,128,384]`, are themselves block-compressed. Do not confuse the **CSF differences between neighboring IDs** with the vector codec's optional **delta mode**.

### Vector and block layout

A newly encoded vector with `N` values has `B=ceil(N/256)` independently decodable blocks. Offsets in this table are relative to the vector, not the page:

| Vector offset | Width | Content |
| ---: | ---: | --- |
| 0 | 4 | Logical value count `N`. |
| 4 | 4 | Block count `B` (plain count in newly emitted vectors). |
| 8 | `4*(B+1)` | Block-start offsets followed by one terminal offset, all relative to the vector. |
| `8+4*(B+1)` | Variable | Consecutive encoded blocks; blocks do not require individual 8-byte alignment. |

Within each block:

| Block offset | Width | Content |
| ---: | ---: | --- |
| 0 | 1 | Low two bits: codec mode; high six bits: compound type for typed modes. |
| 1 | 1 | Packed residual width `w` in bits, from 0 to 64. |
| 2 | 2 | Logical count `c`, from 1 to 256. |
| 4 | 8 | Base value (raw ID/value or shifted compound payload, depending on mode). |
| 12 | `ceil(L*w/8)` | Packed residual lanes; `L=c` for FOR modes, `L=c-1` for delta modes. |

Residual lane zero occupies the least-significant `w` bits first; subsequent lanes follow immediately across byte/word boundaries. Width zero has no payload bytes. With current vector emission, total length is `8+4*(B+1)+sum(12+ceil(L*w/8))`. Padding to the next page section is outside this length.

| Mode | Name | Reconstruction |
| ---: | --- | --- |
| 0 | FOR raw | `value[i] = base + residual[i]`. FOR means frame of reference: the block shares its unsigned minimum. |
| 1 | FOR typed payload | `payload[i] = base + residual[i]`, then `value[i] = (payload[i] << 7) | (type << 1)`. |
| 2 | Delta raw | First value is `base`; later values add consecutive packed differences. |
| 3 | Delta typed payload | First payload is `base`; later payloads add differences, then restore the common type bits. |

`planBlock` begins with FOR raw and replaces it only when another plan uses fewer **bytes**, so equal-sized alternatives retain the earlier mode. Typed encoding is eligible only if every value is even and has the same low seven bits. It preserves the ID bit pattern exactly; an odd inline-double ID or differing type bits retain raw encoding. Delta modes are considered only for `SORTED_SEQUENTIAL_IDS`, after verifying unsigned monotonicity. Current page sections use random-access or cumulative/unsorted hints and consequently emit FOR blocks, including the column named `neighborTails`.

The reader still accepts historical vectors whose block-count word has internal radix/prefix metadata flags. Current `searchIndexed` and `prefixIndexed` both return `false`; **new vectors do not contain those internal sidecars**. Current optional accelerators instead occupy page capacity slack or admitted cursor-local heap. A current vector's count word, directory and blocks can therefore be read directly from the tables above without assuming hidden radix/prefix bytes.

### Raw and typed byte examples

For the mathematical vector `[8,11,20]` with an unsorted/raw-compatible hint, the unsigned minimum is `8`, the residuals are `[0,3,12]`, and `w=4`. This example illustrates numeric packing independently of RDF dictionary assignment. It produces **30 bytes**:

```text
vector-relative offset   bytes                         interpretation
00                       03 00 00 00                   N = 3
04                       01 00 00 00                   B = 1
08                       10 00 00 00                   block starts at 16
12                       1e 00 00 00                   terminal offset = 30
16                       00 04 03 00                   mode 0, width 4, count 3
20                       08 00 00 00 00 00 00 00       raw base = 8
28                       30 0c                         residual nibbles [0,3,12]
```

Offsets in this annotated vector dump are decimal. The first payload byte `30` packs residual 0 in its low nibble and residual 3 in its high nibble; `0c` holds residual 12 in the next low nibble. Decoding lane 2 reads bits 8 through 11, obtains 12, and returns `8+12=20`.

Now encode synthetic IRI IDs `[130,514,1154]`, whose payloads are `[1,4,9]` and whose common type is `1`. The typed plan uses base payload `1`, residuals `[0,3,8]`, width 4 and two payload bytes. Raw FOR would require width 11 and five payload bytes. The typed vector also totals **30 bytes**; its header/directory match the preceding example, but the block is:

```text
16                       05 04 03 00                   05 = mode 1 | (type 1 << 2)
20                       01 00 00 00 00 00 00 00       payload base = 1
28                       30 08                         residual nibbles [0,3,8]
```

Lane 2 restores `((1+8)<<7)|(1<<1)=1154`. The shared type appears in byte `05` once per block rather than in every packed lane.

For comparison, a vector-library call with `SORTED_SEQUENTIAL_IDS` and the eight consecutive synthetic IRI IDs `[130,258,386,514,642,770,898,1026]` selects delta typed mode: block bytes begin `07 01 08 00`, the base is payload `1`, and payload byte `7f` stores seven one-bit differences, all equal to one. Its total is 29 bytes. Typed FOR would use three payload bytes and total 31. Page row vectors use `SORTED_RANDOM_ACCESS_IDS`, so that same row-ID sequence uses typed FOR in a page.

### Direct access and seeking across blocks

To read vector ordinal `i`, `nativeGet` computes block `i>>>8`, lane `i&255`, reads that block's offset from the directory, then decodes the lane. FOR can obtain a lane directly; delta mode must reconstruct its prefix within the selected block. The directory removes any need to decode preceding blocks just to find the target block.

For an exact boundary example, the sorted row vector `[IRI(1), ..., IRI(257)]` has two blocks. Block 0 holds payloads 1 through 256 as typed FOR, width 8: 12 header bytes plus 256 residual bytes. Block 1 holds the singleton `IRI(257)=32898` as raw FOR, width zero: 12 header bytes. Raw wins the singleton tie. Its **300-byte** layout is:

```text
offset 0:    count 257              01 01 00 00
offset 4:    block count 2          02 00 00 00
offset 8:    block 0 starts at 20   14 00 00 00
offset 12:   block 1 starts at 288  20 01 00 00
offset 16:   terminal offset 300   2c 01 00 00
offset 20:   block 0 header         05 08 00 01 01 00 00 00 00 00 00 00
offset 32:   residual bytes         00 01 02 ... fe ff
offset 288:  block 1 header         00 00 01 00 82 80 00 00 00 00 00 00
```

Ordinal 255 goes to block 0, lane 255 and restores `(256<<7)|2=32770`. Ordinal 256 goes directly to offset 288, lane zero and returns base `32898`. An unsigned row-ID search first selects the applicable sorted block, then searches its packed FOR lanes; a page-slack radix can further narrow that lane interval. In contrast, `firstNeighbors` may be unsorted across rows, so it is indexed by the already-found row ordinal.

The cumulative columns serve a different lookup need. To reconstruct the `k`th neighbor of row `r`, the reader adds that row's first neighbor to the sum of `k` neighbor-tail values beginning at `rowFiberStart(r)-r`. To find a fiber's first context, it sums earlier context counts; to reconstruct a later context, it sums that fiber's context tails. Page-slack or hot-cursor prefix anchors can skip whole blocks of those sums, but the result remains the exact sum of logical lanes. Prefix sums restart relative to the requested row/fiber through range subtraction, even when an anchor summarizes the whole flattened vector.

### Exact bytes for a singleton page

Take one synthetic IRI edge: row `130`, neighbor `258`, context `0`, with encoder page ID `7` and no continuation. Structural flags are `2+4+8+16`; both axes have uniform IRI kind, adding `64+256`. Thus flags are `0x015e`. Each of the two present singleton vectors is raw FOR, width zero, and 28 bytes long. No accelerator is eligible for this page.

| Page offsets | Content | Byte count |
| --- | --- | ---: |
| `[0,160)` | Header | 160 |
| `[160,188)` | `rowIds=[130]` | 28 |
| `[188,192)` | Zeroed section alignment | 4 |
| `[192,220)` | `firstNeighbors=[258]` | 28 |
| `[220,224)` | Zeroed final alignment | 4 |
| `[224,256)` | Unused capacity; outside the encoded image | 32 |

The complete 160-byte header below uses **hexadecimal offsets** in its left column. Every row contains 16 bytes:

```text
000  46 53 43 50 04 00 5e 01 00 01 00 00 e0 00 00 00
010  07 00 00 00 ff ff ff ff 01 00 00 00 01 00 00 00
020  01 00 00 00 00 00 00 00 82 00 00 00 00 00 00 00
030  82 00 00 00 00 00 00 00 02 01 00 00 00 00 00 00
040  02 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00
050  a0 00 00 00 1c 00 00 00 00 00 00 00 00 00 00 00
060  00 00 00 00 00 00 00 00 c0 00 00 00 1c 00 00 00
070  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
080  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
090  b6 70 c9 2d 00 00 00 00 00 00 00 00 00 00 00 00
```

At decimal offsets 8 and 12, `00 01 00 00` means capacity `256` and `e0 00 00 00` means used `224`. Offset 20 contains `ff ff ff ff`, the `NO_PAGE=-1` sentinel. The row/neighbor/quad counts are all one. Section descriptors at 80 and 104 hold `(160,28)` and `(192,28)`; all omitted descriptors are zero. Offset 144 holds the little-endian format hash. Accelerator offset, length and descriptor are zero.

The vector at page offset 160 is:

```text
vector offset 00: 01 00 00 00 01 00 00 00    count 1, block count 1
vector offset 08: 10 00 00 00 1c 00 00 00    block offset 16, terminal 28
vector offset 16: 00 00 01 00                raw FOR, width 0, count 1
vector offset 20: 82 00 00 00 00 00 00 00    base 130
```

The vector at 192 is identical except for its base bytes `02 01 00 00 00 00 00 00`, representing neighbor `258`. The context is recovered directly from header `commonContext=0`. Reading this row needs no context vector, no row-start vector, and no residual payload.

### Exact section sizes for the two-row page

The earlier two-row/all-column example illustrates more than the singleton shortcuts. All sections are present, flags are `0x0140` (uniform row and neighbor IRI kinds), and the current plans produce:

| Section | Page offset | Length | Mode, base, width | Packed residual bytes |
| --- | ---: | ---: | --- | --- |
| `rowIds` | 160 | 29 | Typed FOR, payload base 1, width 1 | `02` |
| `rowFiberStarts` | 192 | 29 | Raw FOR, base 0, width 2 | `0c` |
| `rowQuadCounts` | 224 | 29 | Raw FOR, base 3, width 1 | `01` |
| `firstNeighbors` | 256 | 29 | Typed FOR, payload base 1, width 2 | `02` |
| `neighborTails` | 288 | 29 | Typed FOR, type 0, payload base 1, width 2 | `20` |
| `contextCounts` | 320 | 29 | Raw FOR, base 1, width 1 | `11` |
| `firstContexts` | 352 | 35 | Raw FOR, base 0, width 10 | `00 00 20 38 00 00 00` |
| `contextTails` | 392 | 28 | Raw FOR, base 770, width 0 | No payload |

For `firstContexts`, the nonzero value `898=0x382` occupies bits 20 through 29, giving `00 00 20 38 00 00 00`. `neighborTails` can use typed mode because its numeric differences share seven zero low bits; type 0 here describes compression of numbers, not RDF terms in the neighbor axis. Alignment advances the final cursor from 420 to `used=424`; capacity rounds to `512`. The page has 88 unused capacity bytes. None of its vectors has more than one block, and it has fewer than 64 rows, so current accelerator admission adds no bytes.

### Capacity slack and readable tail padding

`used` includes the 160-byte header, vector images, alignment and any admitted page accelerators. Capacity is the first 256-byte class that holds the aligned baseline image, through a maximum of 65,536 bytes. `selectFreeAccelerators` spends only slack in that already-selected class. It cannot move a page to a larger class.

For a concrete accelerator example, a page with 64 rows `[IRI(1),...,IRI(64)]`, each with singleton neighbor `258` and context zero, has a 76-byte typed row vector at 160 and a 28-byte constant neighbor vector at 240. Baseline used is `272`, capacity is `512`, and slack is `240`. The row radix can grow to 7 bits, using 128 bytes; there are no cumulative columns to index. Header accelerator fields are therefore offset `272`, length `128`, descriptor `4`, and used becomes `400`. Capacity remains `512`; the remaining 112 bytes are still unused.

The descriptor has four three-bit codes, at shifts 0, 3, 6 and 9: row radix, neighbor-tail prefix, context-count prefix, and context-tail prefix. Row-radix code zero means absent; codes 1 through 4 mean 4 through 7 radix bits. Prefix code zero means absent; codes 1 through 7 mean stride shifts 0 through 6, with an anchor every `2^shift` blocks. Row radix is eligible from 64 rows; prefix provisioning requires at least two blocks. The accelerator payload follows that same order. See [`CompactCsfPageAccelerators`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageAccelerators.java) and `selectFreeAccelerators` in the encoder.

`READ_TAIL_PADDING=8` is a native decoding access allowance, not eight extra encoded bytes per vector or per page. Wide native reads can load bytes after a packed payload and mask away unrelated bits. The current shard writer/composer allocates eight readable trailing bytes after the shard's logical allocation so the final page also satisfies this contract. Heap-vector readers instead handle the final partial word explicitly. The encoded lengths and page capacity fields exclude that trailing access allowance.

## Overflow rows and continuation pages

Here an **overflow page** means an in-memory CSF continuation extent for a large logical adjacency row. LMDB's own on-disk overflow pages belong to its separate storage format. CSF continuations are bounded encoded pages with the same header/vector layout just described.

The builder limits physical pages to at most 1,024 rows and 64 KiB capacity. It first tries row boundaries: a multi-row page that does not fit is split into a fitting row prefix and the remainder. For a single row that does not fit, `PartitionAccumulator.emitSingleRow` chooses fitting fiber prefixes. If one neighbor's context list does not fit, `emitSplitContextFiber` chooses fitting context-coordinate prefixes for that same neighbor. Large input rows are also emitted in chunks once their rough scratch estimate reaches 256 KiB; that estimate controls build scratch, not the encoded page capacity. See `Builder.pair`, `emitRows`, `emitSingleRow`, and `emitSplitContextFiber` in `ImmutablePagedQuadCsfIndex`.

Each extent is independently encoded. It stores its own row ID, first neighbor(s), context counts and first contexts; differences never need a previous page's last decoded value. Only the first extent represents a new logical row. Later extents set `FLAG_CONTINUATION`, have physical `rowCount=1` and `firstRow==lastRow`, and contribute zero new rows to the partition's key domain.

### An extent seam inside one neighbor's contexts

The following tiny fragments make the seam visible; these few values fit in a real page, so the illustrated split positions are conceptual, not the splitter's output for this small data set. In a real overflow row, each fragment holds many more fibers or contexts. Suppose the immutable root places this row in global page positions 40, 41 and 42:

| Root page position | Continuation | Logical row ID | Physical fibers | Physical quads | New logical rows |
| ---: | --- | ---: | --- | ---: | ---: |
| 40 | No | `130` | `386:[0,770]`, `514:[0]` | 3 | 1 |
| 41 | Yes | `130` | `642:[0,770]` | 2 | 0 |
| 42 | Yes | `130` | `642:[898]`, `898:[0]` | 2 | 0 |

The complete logical row is `386:[0,770]`, `514:[0]`, `642:[0,770,898]`, `898:[0]`. Its result has **7 quads and 4 distinct neighbors**. Physical fiber counts total `2+1+2=5`, because neighbor `642` appears in two adjacent fragments. `KeyCursor.countDistinctNeighbors` subtracts that repeated seam neighbor. Fiber copying can return weighted fragments `642` with weights 2 and 1; their sum is the exact multiplicity 3. Pair copying returns each `(neighbor, context)` once, with no duplicate or dropped context at the seam.

Page 41 has `firstNeighbors=[642]`, `firstContexts=[0]`, `contextCounts=[2]`, `contextTails=[770]`. Page 42 restarts with `firstNeighbors=[642]`, `neighborTails=[256]`, and `firstContexts=[898,0]`; it has one context per physical fiber, so `contextCounts` and `contextTails` are omitted there. Common-context and other structural flags are recomputed per physical page; they need not match across an extent chain.

The byte representation of the page-41 fragment can be calculated independently of the preceding page. Its flags are `0x0143`: continuation, row-neighbor-one, and uniform IRI kind on both axes. At header offset 6 this is `43 01`. Header offsets 24, 28 and 32 hold physical counts `1`, `1`, `2`, with bytes `01 00 00 00`, `01 00 00 00`, `02 00 00 00`. The graph IDs differ, so there is no common-context flag. Its six present singleton vectors each have length 28 and use raw FOR with width zero:

| Section | Page offset | Base value stored at vector offset 20 |
| --- | ---: | --- |
| `rowIds` | 160 | `130`: `82 00 00 00 00 00 00 00` |
| `rowQuadCounts` | 192 | `2`: `02 00 00 00 00 00 00 00` |
| `firstNeighbors` | 224 | `642`: `82 02 00 00 00 00 00 00` |
| `contextCounts` | 256 | `2`: `02 00 00 00 00 00 00 00` |
| `firstContexts` | 288 | `0`: `00 00 00 00 00 00 00 00` |
| `contextTails` | 320 | `770`: `02 03 00 00 00 00 00 00` |

Each vector has the same 16-byte header/directory and four-byte block prefix as the singleton-page example. Alignment rounds the last vector's end from 348 to `used=352`; capacity is `512`. `rowFiberStarts` and `neighborTails` are omitted. The next-extent field still contains `ff ff ff ff`. Nothing in these bytes requires page 40's previous neighbor or context; the root supplies the continuation relationship. For page 42, flags change to `0x014d`, or `4d 01`, because its two physical fibers each have one context and quad count equals fiber count.

### Finding the first extent and locating an edge

The root first selects the predicate/plane partition, then routes by unsigned first-row fences to a shard and its page directory. Each shard has a 64-byte header and 32-byte page entries:

| Entry-relative offset | Width | Meaning |
| ---: | ---: | --- |
| 0 | 8 | Direct native page address. |
| 8 | 8 | First row ID fence. |
| 16 | 8 | Row-ordinal routing metadata within the shard. |
| 24 | 4 | Number of new logical rows represented by the page. |
| 28 | 4 | Unused entry bytes. |

Continuation pages repeat their row fence and have zero new logical rows. Exact fence lookup selects the first matching fence, so a lookup of row `130` reaches its first extent rather than returning a continuation as a new key. Row-ordinal lookup excludes zero-row pages/shards; continuation metadata never allocates another logical key ordinal. The root's reference identifies the first page position and page-local row index, not a flat quad offset. Source anchors are `findLocalReferenceByPartition`, `localReferenceAtOrdinal`, [`CsfRadixDirectory.floorNative`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CsfRadixDirectory.java), and `CsfShard.pageUniqueRows`.

Current traversal advances to **the next global page position** and verifies its continuation flag and repeated row ID. It can cross a shard boundary because the root resolves global page positions back into shard entries. The header's `NEXT_EXTENT_AT` field remains `NO_PAGE=-1` in newly encoded pages; current row/key cursors do not use it as an on-page linked-list pointer. A following normal row, a changed row ID, or end of the root stops the chain. Shards ordinarily defer their target-size split until a row ends, but the hard maximum of 4,096 pages can put one logical row across shards.

Resolving a `RowCursor` for the example walks its extents once and records page positions `[40,41,42]` and starting **row-local quad ordinals** `[0,3,5]`. This small cursor-owned extent directory is separate from the shard's native page directory. `edgeCount=3+2+2=7`. A scalar access to ordinal `5` binary-searches the extent starts, loads page 42, and decodes its local quad ordinal `5-5=0`, yielding `(642,898)`. Ordinal `4` maps to page 41, local ordinal `1`, yielding `(642,770)`.

`lowerBound(0,642,898)` visits applicable extents in order and returns `5`; `copy(2,4,...)` spans page 40's last pair, both page 41 pairs, and page 42's first pair, yielding `(514,0)`, `(642,0)`, `(642,770)`, `(642,898)`. Sequential fiber copying retains its extent/fiber/context position and advances across seams. Thus continuation pages preserve row identity, unsigned pair ordering, context filtering and multiplicity while bounding each physical encoded image.

## Shards, allocation classes, and retained-page ownership

`CsfShard` supplies the page directory above, bounded to at most 4,096 pages per shard, plus a native radix directory for page-fence routing. A shard can reference unchanged pages from prior versions while storing newly materialized pages itself. Lookups use direct directory addresses rather than traversing the copy-on-write ownership chain. Reference-counted dependencies retain old page owners while a newer shard shares their pages. Periodic shard-local flattening copies the current page set when chain depth or retained obsolete capacity crosses configured thresholds. Source defaults are maximum chain depth 32, maximum retained-waste percentage 25%, and minimum retained-waste threshold 4 MiB; these system properties are resolved when `CsfShard` initializes.

`NativeSlabAllocator.classForUsedBytes` and `capacityForClass` define deterministic page size classes: 256-byte granularity from 256 bytes to 64 KiB (256 classes). Current materialization uses `CsfShard.Writer`: it allocates one contiguous shard image containing the aligned directory/routing area and the sum of its page capacities, plus the trailing 8-byte decoder allowance. The composer likewise allocates its own directory and newly owned page slots, while shared entries point directly to retained earlier allocations. These pages are not independently allocated LMDB pages. `NativeSlabAllocator` also contains a class-slab allocation implementation, but the current root builder does not instantiate that allocator. Capacity selection and shard allocation should therefore be read as separate steps.

For the singleton byte example, the encoded page occupies 224 bytes within its 256-byte slot. That slot is part of a shard allocation which also holds routing metadata and any other page slots. Reporting 224 encoded bytes, 256 referenced page-capacity bytes, or total owned shard bytes answers three different questions. A shard models 192 Java bytes for its own persistent object/accounting; this is not a claim about total Java heap/RSS per page.

Optional cursor-owned heap accelerators are not part of retained CSF. `CsfAdaptiveMemory` starts enabled unless `rdf4j.lmdb.csf.adaptiveAccelerators.enabled=false`; defaults are minimum heap headroom `max(32 MiB, maxHeap/64)`, maximum one object 1 MiB and aggregate claimed bytes `max(64 MiB, min(1 GiB, maxHeap/32))`. It resolves these static properties on first class initialization. A cursor builds an accelerator only after repeated work; failed admission or allocation abandons this optional fast path. Some accelerators reserve an aggregate claim with `tryClaim`; their optional arrays also pass per-object and sampled-headroom admission. Cursor-local arrays can use per-object admission alone. Concurrent per-array admissions can exceed the sampled headroom threshold, although each array still observes the per-object cap. These are safeguards, not a hard JVM heap or RSS limit.

Keep these measures separate: owned native shard allocations, logical referenced page capacity, modeled Java metadata, transient build workspace, cursor-local heap accelerators, LMDB maps/page cache, and disk. The adjacency ledger accounts native and modeled Java derived-index ownership, but does not measure operating-system resident set size.

## Lookup cost and compatibility

For a base-row lookup, the concrete path is predicate ordinal and direction/explicitness plane → shard fences → native page entry → sorted `rowIds` lookup → row-local fiber span → neighbor differences and, when required, context columns. For the `:knows` example, a bound `:alice` routes to the outgoing row and returns weighted `:bob`/`:carol` fibers or the three exact context pairs. A later join step can use each neighbor ID as a row key in the outgoing `:label` partition. A bound object instead selects the incoming partition and follows the same physical machinery with subject IDs as neighbors.

This document describes the base portion of that path. Committed overlays and transaction-local changes can replace or extend the visible row, and query operators must respect graph and binding semantics while consuming it. Continue with the [adjacency lifecycle and visible-row lookup examples](storage-adjacency-lifecycle.md), [native physical access paths](query-physical-access.md), and [join and factor examples](query-joins-and-factors.md).

Construction performs source scans, sizing, encoding and native allocations proportional to rows/fibers/quads and selected coverage; on-demand cursor materialization allocates reusable row/block buffers rather than one RDF `Statement` per edge. A high-degree row can touch multiple continuation pages. These are algorithm/data-layout observations, not measured speedup claims.

Page-format version `4` is in-memory derived-state compatibility only. If the encoder or vectors change, update version/hash validation and the structural tests together. Do not change field offsets/flag meaning while leaving `FORMAT_HASH` the same. Changing durable statement IDs, index keys or store metadata is a separate migration topic covered in [value IDs and record formats](storage-values-and-records.md).

Source tests that specify these contracts include [`CompactCsfPageTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageTest.java), particularly `variableCardinalitiesStoreRandomAccessPrefixOffsetsAndRoundTrip` and `pageSlackAcceleratorsNeverChangeTheSelectedNativeCapacityClass`; [`ImmutablePagedQuadCsfIndexTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/csf/ImmutablePagedQuadCsfIndexTest.java), particularly `splitsOneSupernodeIntoLinkedFibreExtents`, `splitsOneNeighborFibreAtContextCoordinateBoundaries` and `keyCursorCountsDistinctNeighborsAcrossContinuationPages`; and [`PackedLongVectorTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/csf/PackedLongVectorTest.java), particularly `blockBoundariesRoundTripAt255256And257Values` and `newlyEncodedVectorsNeverCarryDatasetSizedInternalSidecars`. These are static source anchors for this documentation change; no code, tests or builds were run.
