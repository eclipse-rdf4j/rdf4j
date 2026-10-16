# Value IDs and persistent records

This document covers the persistent value identity contract for the pinned `optimize-lmdb` branch. The key files are [`ValueIds`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueIds.java), [`ValueStore`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java), [`ValueStoreRecordCodec`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRecordCodec.java), [`Varint`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/Varint.java), and [`StoreProperties`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/StoreProperties.java).

**Branch delta and prerequisites:** changed storage code introduces writer-marked ordered numeric IDs, tagged core-datatype literal references, lowercase language-tag writing for opted-in stores, and the durable retired-ID/recovery path. Existing stores retain their prior ID-writing choices when the corresponding marker is absent, and the base LMDB dictionary/statement-key model remains the persistence contract; the following format details are needed before changing either writer or reader.

## Value IDs: bit fields and ordering

For ordinary composite IDs, `ValueIds.createId(type, value)` constructs `(value << 7) | (type << 1)`. In bit order:

| Bits | Meaning |
| --- | --- |
| 0 | Zero for an ordinary composite reference; set for inline-double encoding (the double payload uses the other bits). |
| 1–6 | Six-bit ID type. |
| 7–62 | 56-bit value field for the ordinary nonnegative reference domain. |

Examples: URI type `1`, ordinal `42` is `(42 << 7) | 2 = 5378`; extracting type reads bits 1–6 and extracting the value shifts right by seven. Type `0` is the namespace/pointer family. Types `1–5` identify URI, literal, blank node, RDF-star triple term, and a tagged core-datatype literal reference. Inline datatype IDs use types `16–35`; `-1` is the `T_DOUBLE` sentinel in term-kind APIs, while low-bit detection identifies inline doubles in a value ID. See the constants and helpers in [`ValueIds.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueIds.java#L20).

New stores record `numeric-id-encoding=ordered-v1` when the ordered numeric writer is enabled (the config default is enabled). For the nine signed integer families, the ordered type codes are `36..44`, and the payload is `value + 2^55`, supporting the signed range `[-2^55, 2^55)`. Comparing payloads as unsigned-position values therefore has the same order as comparing the represented integers, and order-preserving varint encoding keeps threshold ranges contiguous within each encoded type. Legacy stores without this property continue using their prior ZigZag numeric IDs: the property selects new ID minting, not a rewrite of existing dictionary keys. IDs remain self-describing, so both forms may be read from one store.

Eligible stored literals in a new v4 store can use a tagged reference ID: the ordinary dictionary ordinal is shifted left six bits, and the low six value-field bits carry a stable datatype tag. Tags currently cover 53 core XSD/RDF/GEO datatypes. A tag is used only when the datatype is eligible and the ordinal fits; zero means use the ordinary literal reference. Stores without `literal-reference-encoding=core-v1` keep minting old untagged references even after a top-level version-marker upgrade. This avoids changing already-persisted ID identity. Consult `coreDatatypeTag`, `createCoreLiteralReferenceId`, and `referenceOrdinal` in [`ValueIds.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueIds.java#L218) before adding or renumbering a tag.

## `store.properties` and compatibility

`store.properties` is at the repository root; `LmdbStore.VERSION` is currently `4`. The file records index lists and writer compatibility flags:

| Property | Current meaning | Compatibility behavior |
| --- | --- | --- |
| `version` | Top-level LMDB store version. | Startup accepts supported earlier versions, upgrades through `LmdbStore` migration logic, and rejects a future or invalid version. It is not the CSF or bulk-manifest version. |
| `triple-indexes` | Statement-index field orders in the triple environment. | An explicit requested set can trigger index reindexing; an omitted set reuses existing indexes or the new-store default. |
| `triple-term-indexes` | RDF-star term index field orders in the value environment. | Defaults are applied to new stores; existing configured indexes are retained unless a differing nonempty request triggers reindex. |
| `numeric-id-encoding=ordered-v1` | New inline numeric IDs use biased order encoding. | Absent means legacy ZigZag for new writes; existing IDs are self-describing and are not rewritten. |
| `literal-reference-encoding=core-v1` | New eligible stored literal references carry datatype tags. | Absent means old untagged references continue to be created. |
| `inline-literals=true|false` | Whether eligible literals have inline identities. | This affects identity, so reopening with the opposite setting is refused. An absent old marker is treated as historical `true`; it cannot reveal whether an old store deliberately disabled inlining. |
| `canonical-language-tags=lowercase-v1` | New language-tagged literal records lowercase language bytes. | Absent keeps historical byte-preserving language spelling. It does not rewrite old records. |

Top-level version 4 and each writer property must be treated as separate layers. When changing any durable encoding, preserve mixed legacy/new reads and the marker's actual scope; do not assume setting `version=4` proves every optional capability is present. The open/migration and marker handling is in [`LmdbStore.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java#L78) and the property accessors in [`StoreProperties.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/StoreProperties.java#L20).

## Dictionary records

The value environment's unnamed main database contains two directions for normal values: a record-data key maps to an ID, and an `ID_KEY` followed by the encoded ID maps back to the record bytes. `ValueStoreRecordCodec` defines the stable record marker bytes:

| Marker | Record payload |
| ---: | --- |
| `0` URI | Namespace ID as unsigned varint, then UTF-8 local name. |
| `1` literal | Datatype ID varint; one direction/language-length byte (or extended-length marker plus varint); optional language bytes; UTF-8 lexical label. |
| `2` blank node | UTF-8 blank-node identifier. |
| `3` RDF-star triple term | Used as an ID type; the term's component IDs and allocated term ID are indexed in the configured `term-*` indexes rather than represented as an ordinary value byte record. |
| `4` namespace | UTF-8 namespace string. |
| `5` ID key | Reverse-value index key prefix, followed by unsigned-varint ID. |
| `6` hash key | Large-record hash lookup key prefix, followed by unsigned-varint CRC32 hash. |
| `7` HASHID key | A collision key containing the hash and full ID key. |
| `8` next-ID key | Reserved allocator key prefix. |

The literal header byte uses high two bits for base direction (`00` none, `01` LTR, `10` RTL) and low six bits for a language length up to 63 bytes. A language tag longer than 63 bytes sets the low six bits to all ones and follows the header with an unsigned-varint length. The stored datatype ID is always included; a core-datatype ID tag is an optimization for interpreting the *reference*, not a substitute for the literal record.

An IRI is split into namespace and local name. This saves repeated namespace bytes in the normal dictionary and lets namespace references be shared. Namespace and datatype dependencies increment reference counts; triple terms increment references for each of their subject, predicate, and object IDs. The fast staged loader uses the same record codec so dictionary bytes do not silently differ between normal writes and bulk-load writes. Exact writer details live in [`ValueStoreRecordCodec.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRecordCodec.java#L28), ordinary persistence in [`ValueStore.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java#L3720), and triple-term key construction in [`TripleIndex.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleIndex.java#L370).

`MAX_INLINE_KEY_BYTES` is 16 bytes of complete serialized record data. At or below that threshold, the exact value record can be used as an LMDB key in the reverse dictionary direction. Larger values cannot be used as keys; the store hashes their record bytes with CRC32 and uses a collision-aware `HASH_KEY`/`HASHID_KEY` path, while the forward ID-to-record entry is reserved at its final byte length. A hash match alone is not proof of value equality: callers compare full record bytes. The 16-byte limit is a storage-key rule, not a maximum RDF value size. Longer dictionary records still consume mapped pages and require exact byte comparison on lookup.

### Database names and authoritative rows

The value environment reserves capacity for the main database, six auxiliary databases, and up to 12 RDF-star term indexes (`mdb_env_set_maxdbs` is set to `9 + 12`). The named auxiliary databases are `unused_ids`, `free_ids`, `ref_counts`, `retired_ids`, `retired_ids_seq`, and `gc_meta`; the main DB also carries allocator and dictionary records. The triple environment reserves `contexts` plus up to 48 statement indexes (24 explicit and 24 inferred). Default statement field orders for a newly created store are `spoc,posc`; actual persisted properties determine the set used when reopening. Explicit and inferred statement records use distinct named DBs.

The 4-field statement-index key is made from the four IDs in the configured field order using the storage order-preserving unsigned varint encoder; `c` is the context ID and a missing default context is encoded according to the store's context mapping. For RDF-star terms, configured `term-*` indexes use subject/predicate/object plus the triple-term ID in a four-varint key. These are physical B-tree keys; never change field order or varint ordering without migration/reindex support. See [`TripleIndex`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleIndex.java), [`IndexKeyWriters`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/util/IndexKeyWriters.java), and index selection/reindex code in [`TripleStore.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java#L320).

The optional `valueHashCacheEnabled` config creates `values/hashes.dat` and `hashes.dat.integrity`, mapping ID ordinals to cached 32-bit hashes in 256-MiB file segments. This file is separately mapped and integrity-checked; it is a cache that can be discarded/rebuilt, not a replacement for dictionary records. Do not count its mapped capacity as Java heap. See [`ValueStoreHashFile.java`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreHashFile.java#L25) and [`LmdbStoreConfig`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/config/LmdbStoreConfig.java#L108).

## Retirement and safe reuse

Dictionary IDs can be reused only after the store knows that all pinned snapshots are past the removal that retired the ID. Refcounts represent dependencies such as an IRI's namespace, a literal's datatype, and an RDF-star term's components. A value row is not reclaimed merely because one current transaction removed a statement referencing it. Active snapshot readers and derived retained snapshots can delay reclamation; stale retirement candidates are checked against current authoritative triples before garbage collection.

The durable retirement queue is indexed twice:

| DB | Key → value | Why both exist |
| --- | --- | --- |
| `retired_ids` | `Varint(id)` → `Varint(bootEpoch) \| Varint(retirementRevision)` | Latest retirement intent for an ID. |
| `retired_ids_seq` | `Varint(bootEpoch) \| Varint(revision) \| Varint(id)` → empty | Ordered drain by commit horizon. Older superseded sequence entries can remain until drain detects they no longer match the latest per-ID record. |
| `gc_meta` | Boot-epoch metadata | Separates revisions across process opens because the triple store's in-memory revision restarts. |

On reopen, no pre-crash read snapshot is alive, so older epochs are eligible to drain; each candidate is still checked against live triples before records are reclaimed. Reference-count integrity is checked/recovered before startup eviction. A missing or stale marker cannot be treated as proof that counts are safe; recovery scans and writes a checked marker tied to the observed native LMDB transaction. Sources: [`RetiredValueIdStore`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RetiredValueIdStore.java#L45), [`ValueStore` startup marker and recovery](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java#L1010), and [`LmdbSailStore.recoverRetiredValueIds`](../../../core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java#L936).

## Performance, memory, and extension notes

* Inline numeric or short-string IDs avoid dictionary lookups, but their encoded ID type and value ordering remain part of the ID contract. `inline-literals` must not be changed in place because it changes identity.
* Large values pay for their full serialized bytes, an ID-to-record row, and a hash-based reverse lookup plus collision verification. The fixed 16-byte key threshold saves no lexical bytes; it selects a different key path.
* Namespace factoring reduces repeated IRI bytes at the cost of shared records and refcount maintenance. Avoid “optimizing” it by changing URI bytes independently of the codec.
* Retired dictionary records can remain on disk while a relevant snapshot is pinned; in-memory ID caches and hash caches are separate from physical row reclamation.
* Statement and term indexes trade write and disk cost for key-order access paths. B-tree key comparisons use encoded bytes, so maintain order preservation for every numeric family that has key-range semantics.

Tests that specify these contracts include [`LmdbCoreDatatypeReferenceIdTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCoreDatatypeReferenceIdTest.java), [`LmdbOrderedNumericIdsTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbOrderedNumericIdsTest.java), [`ValueStoreCheckpointAtomicityTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreCheckpointAtomicityTest.java) for checkpoint and reference-count recovery behavior, and [`LmdbValueGcRecoveryTest`](../../../core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbValueGcRecoveryTest.java).
