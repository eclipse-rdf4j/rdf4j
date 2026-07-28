# Build a bounded-memory LMDB bulk loader

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and
`Outcomes & Retrospective` must be kept up to date as work proceeds.

This document is maintained in accordance with `.agent/PLANS.md` from the repository root.

## Purpose / Big Picture

RDF4J's LMDB Sail currently ingests RDF through ordinary transactions. Even the optimized fresh-store path retains
RDF model objects and a global value map, then performs database work in statement batches. After this change, a user
can build a new query-ready LMDB store from an RDF file or stream with a memory ceiling that does not grow with the
input. The loader parses or transcodes RDF into compressed temporary records, globally deduplicates values one hash
partition at a time, resolves statements without random LMDB probes, sorts every configured database by its exact
append order, validates the completed store, and only then publishes it.

The observable Java entry point is `org.eclipse.rdf4j.sail.lmdb.bulk.LmdbBulkLoader`. The observable command-line
entry point is an executable artifact from a new `tools/lmdb-bulk-load` module. A successful command reports the
number of parsed and unique statements and leaves a store that produces the same RDF and SPARQL results as ordinary
LMDB ingestion. An interrupted or failed command never exposes a partially built store.

## Progress

- [x] (2026-07-26 21:47Z) Read `.agent/PLANS.md`, the Kuzu reference implementation, the LMDB storage code, and the
  high-performance Java and Maven-runner instructions.
- [x] (2026-07-26 21:47Z) Run the required root `-Pquick clean install`; the reactor completed successfully.
- [x] (2026-07-26 21:47Z) Create this self-contained ExecPlan before production changes.
- [x] (2026-07-26 21:58Z) Add failing then passing tests for the public loader contract and an end-to-end
  query-ready store with language-tag canonicalization.
- [x] (2026-07-26 21:58Z) Establish the safe vertical scaffold: Rio ingestion into an isolated generation, reopen
  validation, manifest publication, and incomplete-marker rejection.
- [x] (2026-07-26 22:49Z) Implement canonical terms, Rio staging, fast N-Triples/N-Quads parsing, compressed partition buckets, and
  deterministic value assignment.
- [x] (2026-07-26 22:49Z) Implement component and dependency resolution, exact ValueStore record generation, and bounded byte-key sorting.
- [x] (2026-07-27 00:10Z) Implement independent per-index tuple sorting, contexts, metadata, the shaded CLI,
  configurable native transaction bounds, checksummed publication recovery, and an exclusive loader lock.
- [x] (2026-07-27 02:46Z) Complete focused red/green milestones for parser/Rio parity, gzip input, deterministic
  forced spills, locality/generic resolution parity, exact ValueStore/index validation, encoded-key fallback,
  transaction byte limits, cancellation, map growth, publication recovery, and SPARQL/reopen parity.
- [x] (2026-07-27 04:09Z) Verify the 22-test loader contract and seven-test CLI selection, format sources, validate
  headers, run both JMH arms on JDK 25, and retain a 25-second JFR recording.
- [x] (2026-07-27 04:06Z) Run the full LMDB module gate. The new bulk-loader tests pass; the module remains red in
  four unrelated native-query assertions (one strategy-decline census and three feature-flag subprocess-output
  assertions), recorded in `logs/mvnf/20260727-014837-verify.log`.

## Surprises & Discoveries

- Observation: LMDB's ValueStore is not a single bidirectional dictionary. Its main database mixes raw value keys,
  ID keys, primary hash keys, and hash-collision keys; it also owns a separate `ref_counts` database and separate
  RDF-star `term-*` indexes.
  Evidence: `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java` opens these databases and emits
  the record families in `persistPreparedValues`.

- Observation: an LMDB value record cannot always be generated when the canonical RDF term is first seen. IRI data
  contains a namespace ID, literal data contains a datatype IRI ID, and RDF-star term keys contain three component
  IDs. All IDs must therefore be assigned before dependency-bearing records are encoded.
  Evidence: `ValueStore.uri2data`, `ValueStore.literal2data`, and `ValueStore.persistTripleTerms` encode those IDs.

- Observation: RDF4J literal equality treats language tags case-insensitively, but the legacy ValueStore serialization
  preserves language-tag spelling.
  Evidence: `SimpleLiteral.equals` uses `equalsIgnoreCase`, while `ValueStore.literal2data` currently writes
  `literal.getLanguage()` unchanged.

- Observation: `TripleIndex.toKey` writes four order-preserving unsigned SQLite4 varints. Tuple sorting is valid only
  while unsigned ID order and the encoded byte comparator remain equivalent.
  Evidence: `TripleIndex`, `IndexKeyWriters`, and `Varint.writeUnsigned` define the current encoding.

- Observation: a complete generation can be created and reopened through the existing LMDB lifecycle before any
  custom append writer exists. This provides a useful vertical scaffold for publication tests while the transactional
  interior is replaced milestone by milestone.
  Evidence: `LmdbBulkLoaderContractTest.loadsAndPublishesAQueryReadyDeduplicatedStore` passes after reopening the
  published target and observing two unique statements from four input records.

- Observation: LMDB named-database catalog entries occupy the unnamed main database used by `ValueStore`, above the
  value-record family byte range. Opening auxiliary databases before loading main records makes `MDB_APPEND` invalid
  for the otherwise correctly sorted value stream.
  Evidence: the first native literal test lost URI reverse ID 8450 under `MDB_APPEND`; the same sorted stream passed
  reverse validation with `MDB_NOOVERWRITE`. Auxiliary databases, which are truly empty, continue to use append mode.

- Observation: an LMDB cursor must be closed before its write transaction is committed. Committing first invalidates
  the cursor, and the subsequent `mdb_cursor_close` caused an intermittent macOS
  `BUG IN CLIENT OF LIBMALLOC: pointer being freed was not allocated` abort.
  Evidence: phase-scoped loader runs isolated the abort to `ValueStore.appendBulkBatch`; moving `commit()` after the
  cursor scope made the full eight-test loader contract selection repeatably green.

- Observation: a marker without an OS lock cannot distinguish a crashed loader from a live loader. A second process
  could therefore enter recovery and delete the first process's generation.
  Evidence: `preventsConcurrentLoadersFromRecoveringALiveGeneration` reproduced the race before the loader-owned file
  lock was added.

- Observation: safe publication recovery needs content identity, not only artifact names. Version 2 manifests now
  record each top-level artifact's type, recursive byte size, SHA-256 digest, and the result statistics needed to
  return from a completed restart without consuming input again.
  Evidence: `resumesAndValidatesAPartiallyPromotedNativeStoreWithoutReadingInputAgain` injects a failure after one
  top-level move, then resumes and validates the native store while `refusesRecoveryWithoutDeletingForeignFiles`
  proves unlisted files stop recovery untouched.

- Observation: the variable-length ValueStore sorter cannot retain one Java object and one byte array per record
  without making the heap budget proportional to the run size.
  Evidence: `ExternalByteKeySorterTest` now exercises a bounded native arena plus primitive offset/length/order arrays,
  heap fallback, bounded-fan-in multi-pass merging, and deterministic arena release.

- Observation: tuple-order equivalence is a property of the current statement-key codec, not a permanent storage
  invariant.
  Evidence: `StatementIndexBulkRecords` uses the fixed-width radix sorter only when
  `TripleIndex.usesUnsignedTupleOrder()` advertises equivalence; otherwise it sorts exact encoded index keys through
  `ExternalByteKeySorter`.

- Observation: record-count transaction limits alone do not bound LMDB write transactions containing long values or
  encoded keys.
  Evidence: ValueStore, RDF-star term-index, tuple-index, and encoded-index append paths now all apply the configured
  byte ceiling as well as the record ceiling.

- Observation: JMH `EVENTS` counters in single-shot mode are summed across measured invocations.
  Evidence: the ten-iteration 1,000-row JFR smoke run reported 10,000 aggregate rows and 2,560 aggregate unique rows.
  The benchmark contract now documents division by `Cnt` for mean per-load values and uses JFR for trial-wide memory
  peaks.

## Decision Log

- Decision: implement the feature as a new additive loader and leave transactional ingestion as the incremental path.
  Rationale: the bulk algorithm assumes an empty store and globally sorts all values and statements; mixing it with
  mutation semantics would invalidate deterministic assignment and append ordering.
  Date/Author: 2026-07-26 / Codex.

- Decision: place the Java API and engine in `core/sail/lmdb`, but place the shaded executable and concrete Rio parser
  bundle in `tools/lmdb-bulk-load`.
  Rationale: library users can supply any Rio parser through the service registry without forcing every parser and its
  third-party dependencies onto all `rdf4j-sail-lmdb` consumers.
  Date/Author: 2026-07-26 / Codex.

- Decision: add a `canonical-language-tags=lowercase-v1` store property and apply it to both normal and bulk writes for
  newly created stores; stores without the property retain legacy behavior.
  Rationale: bulk and later incremental lookups must agree, while existing stores may contain uppercase serialized
  tags and cannot be reinterpreted safely.
  Date/Author: 2026-07-26 / Codex.

- Decision: port Kuzu's algorithms and record-oriented architecture, not Chronicle state or Kuzu storage formats.
  Rationale: LMDB requires exact LMDB keys and its own ID, collision, reference-count, context, and metadata rules.
  Date/Author: 2026-07-26 / Codex.

- Decision: use offline sort-and-scan algorithms with primitive or byte-slab runs under one global budget.
  Rationale: this changes the memory slope from global object maps to bounded memory plus external I/O, which is the
  primary performance requirement; JVM micro-optimizations come only after this algorithmic change.
  Date/Author: 2026-07-26 / Codex.

- Decision: keep a temporary Rio-to-transactional implementation behind the final API while building the staged
  engine, and replace it before declaring the core milestone complete.
  Rationale: publication, target-safety, API ownership, and canonical-language behavior become independently
  testable without pretending the current writer satisfies the bounded-memory requirement.
  Date/Author: 2026-07-26 / Codex.

- Decision: replace the transactional scaffold with a loader-owned `LmdbNativeBulkStore` as soon as the resolved
  value and ID-quad spools exist.
  Rationale: exact staged IDs now flow into native ValueStore records and TripleStore quads without random value
  probes; ordinary incremental transactions remain untouched.
  Date/Author: 2026-07-26 / Codex.

- Decision: until named-database creation is deferred, use sorted `MDB_NOOVERWRITE` writes for the unnamed ValueStore
  database and `MDB_APPEND` for empty auxiliary databases.
  Rationale: correctness takes precedence over an invalid append hint. Deferring catalog creation is tracked as part
  of the remaining exact append/index work.
  Date/Author: 2026-07-26 / Codex.

- Decision: validate every append-loaded ValueStore, RDF-star term-index, and statement-index record by a second
  sequential cursor scan against the sorted source before publication.
  Rationale: count-only validation cannot detect a missing key paired with an unexpected key; the sequential scan
  remains bounded-memory and verifies exact key/value equality, uniqueness, and exhaustion.
  Date/Author: 2026-07-27 / Codex.

- Decision: decompress gzip before dispatching to either the fast parser or Rio without closing caller-owned streams.
  Rationale: parser-mode selection must not change compressed-input behavior, and the ownership contract applies to
  wrappers as well as the original stream.
  Date/Author: 2026-07-27 / Codex.

## Outcomes & Retrospective

The staged LMDB bulk loader, shaded CLI, native record writers, publication recovery, and benchmark are implemented
end to end. The loader parses fast N-Triples/N-Quads or transcodes Rio input into the same compressed canonical
records; globally assigns deterministic IDs partition by partition; resolves statement and value dependencies from
disk artifacts; externally sorts exact ValueStore and configured statement-index records; append-loads bounded LMDB
transactions with map-growth replay; sequentially validates the emitted databases; and publishes only a checksummed,
reopenable generation. Transactional ingestion remains unchanged except for the versioned language-tag
canonicalization behavior of newly created stores.

Focused verification is green: the loader contract runs 22 tests and the CLI runs seven. These selections cover parser
parity and diagnostics, gzip, namespaces, RDF-star dependencies and term indexes, inline values, hash collisions and
hash-cache modes, deterministic spill behavior, all statement-key permutations, encoded-key fallback, duplicate
quads and contexts, direct-memory fallback, transaction byte bounds, repeated map growth, cancellation, SPARQL
queries, marker refusal, resumable publication, and preservation of foreign files. The full LMDB module verify
completed but is not globally green because four unrelated native query-engine tests fail on the existing workspace;
none enters the bulk package.

The JDK 25 benchmark harness executed both paths. A 1,000-row high-duplication smoke run recorded 256 unique rows per
load. The normal path took 230.9 ms for its one measured invocation; the ten-iteration JFR bulk smoke averaged
2,494 ms/load with large variance because fixed 256-partition setup dominates this tiny dataset. These are harness
validation numbers, not performance claims. The CLI registry includes every current non-legacy Rio parser module,
including HDT and NDJSON-LD. The retained recording is
`profiles/lmdb/lmdb-bulk-loader-smoke-fixed.jfr` (25 seconds, allocation/GC/CPU/file/direct-buffer events).

## Context and Orientation

The target module is `core/sail/lmdb`. `LmdbStore` owns the public Sail lifecycle and the root data directory.
`LmdbSailStore` coordinates transactional ValueStore and TripleStore writes. `ValueStore` maps RDF values to encoded
long IDs and back. `TripleStore` owns statement indexes and context counts. `TripleIndex` converts a subject,
predicate, object, and context tuple into the byte key for one configured permutation. `StoreProperties` persists
format choices required when reopening a store.

The reference implementation is in `/Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/kuzu`. The
reusable pieces are `KuzuFastRdfParser`, `BulkTermBucketWriter`, `BulkStatementBucketWriter`,
`BulkBucketOutputLimiter`, `BulkLz4`, `BulkTermKeyIdCache`, `BucketedStatementEncoder`,
`ExternalLongTupleSorter`, and `LongTupleRadixSorter`. These sources demonstrate raw UTF-8 term spans, deterministic
route hashing, concatenated LZ4 bucket files, bounded open outputs, direct-mapped repeated-key caches, external fixed
tuple sorting, and loser-tree merging. They must be renamed and integrated with LMDB abstractions; Chronicle maps and
Kuzu's role-separated dictionary semantics must not be copied.

A canonical term is a byte encoding whose equality is RDF4J value equality. It contains a type tag and
length-delimited payloads: the full unescaped IRI, blank-node identifier, or literal label, effective datatype,
lowercase language tag, and base direction. An RDF-star triple is recursively encoded from its subject, predicate,
and object terms. An internal namespace key uses a separate tag so it can share partition mechanics without being
mistaken for an RDF Value.

A partition is one of a power-of-two number of compressed files selected from the low bits of Kuzu's stable route
hash. Equal canonical keys always select the same partition. A run is a bounded in-memory batch sorted and written to
disk. A loser tree is an array-based tournament used to select the smallest current record during a k-way merge
without allocating a priority-queue node per output row.

LMDB append mode requires each key to be greater than the previous key under LMDB's unsigned byte comparator.
Consequently ValueStore records must be sorted by the final serialized key, and each statement index must be sorted
in its own configured component order.

## Plan of Work

Begin each externally observable milestone with a focused failing test and preserve its report before production
changes. Keep old transactional tests green throughout.

First add the public API shape and language-canonicalization tests. Define `LmdbBulkLoader` as a final class with a
builder taking a target `Path` and `LmdbStoreConfig`. The builder exposes parser mode, memory bytes, partition count,
maximum open files, temporary directory, cancellation `BooleanSupplier`, and the two LMDB transaction bounds.
`load(Path, RDFFormat)` owns its input stream; `load(InputStream, String, RDFFormat)` does not close its input.
`Result` reports parsed statements, unique stored statements, persisted RDF values, inline values, temporary bytes,
and elapsed milliseconds. `ParserMode.AUTO` selects the fast parser only for N-Triples and N-Quads. A syntax failure
is never retried through another parser.

Add `canonical-language-tags` to `StoreProperties`. Newly created stores record `lowercase-v1`; legacy stores without
the key preserve input bytes. Route all normal fresh and incremental literal serialization through the same
conditional ASCII-folding helper that the bulk canonical encoder uses. Reopening a newly created store and looking up
`"x"@EN` after storing `"x"@en` must return the same ID.

Port the Kuzu fast parser and staging primitives into an internal `bulk` package. The parser reads ordinary or gzip
N-Triples/N-Quads, writes canonical byte spans without RDF model objects or Strings on its hot path, reports line and
column diagnostics, and checks interruption/cancellation on buffer refills and every bounded statement interval.
Rio fallback receives `Statement` objects but immediately encodes and releases them. Both routes emit compressed
statement records with a monotonically increasing ordinal and canonical subject, predicate, object, and optional
context. They also emit canonical value occurrences. Before emitting dependencies, apply the byte-native inline
literal encoder; a successfully inlined value needs a lookup entry but no persistent datatype dependency. Otherwise
emit IRI namespace keys, literal datatype IRIs and their namespaces, and recursive RDF-star components. Only
top-level statement predicates set the reserved-predicate role bit.

Use Kuzu's stable 64-bit route hash and low-bit routing. The default is 256 partitions and the builder accepts only a
positive power of two. Reuse the repository's existing `at.yawk.lz4:lz4-java:1.10.4`; do not add another LZ4
implementation. Use 256 KiB blocks and an access-ordered output limiter whose cap is the minimum of the configured
file limit, possible outputs, 1024, and the number affordable from one eighth of the memory budget.

Finalize partitions in increasing number. Accumulate canonical keys and role masks in a bounded byte-slab hash table.
When full, sort its distinct entries lexicographically and write a run. Merge runs with bounded fan-in, coalescing
equal keys and OR-ing role masks. Assign inline IDs directly. Assign persistent IDs with existing `ValueIds` counters.
Enable the 64-entry predicate URI window before URI allocation; predicate-role IRIs receive remaining reserved IDs,
and ordinary URI allocation begins at 65. Write an immutable partition lookup artifact containing a disk-backed
open-addressed slot table and append-only canonical-key bytes. ID assignment must not vary with input order, run size,
or parser route.

Create a generic dependency resolver. Consumer records contain an owner, component slot, and canonical key and are
routed to the key's partition. Consumers include statement components, IRI namespaces, literal datatypes, and
RDF-star components. Load one partition artifact at a time and use a direct-mapped hash-plus-key cache before the
artifact lookup. Emit fixed resolved records. Sort statement components by ordinal and slot and assemble a big-endian
five-long spool containing ordinal, subject, predicate, object, and context. Reject missing, duplicate, or incomplete
component records. Once the generic path is correct, add the Kuzu locality optimization: subject-routed statements
resolve every term in the loaded partition, carry resolved IDs, hash unresolved spans once, and rebucket by object;
all remaining components use the generic resolver.

Extract package-internal encoding helpers from `ValueStore` so normal and bulk writers share exact value data, ID
keys, hash keys, collision keys, and reference-count keys. Generate raw-data-to-ID and ID-to-data records. Values no
longer than `MAX_KEY_SIZE` use raw data keys. Larger values use CRC32: the first value in deterministic assignment
order uses `HASH_KEY`; remaining values with the same CRC use `HASHID_KEY` plus ID. Emit one reference-count delta
from every distinct IRI to its namespace, every non-inline literal to its datatype, and every RDF-star triple to each
component; externally sort and reduce these deltas. Build every configured RDF-star term index from resolved
component IDs. If value hash caching is enabled, populate `hashes.dat` with byte-native RDF4J hash semantics verified
against model objects.

Implement a variable-length byte-key sorter using bounded byte arenas plus primitive offset/length arrays. Runs and
merged output use unsigned lexicographic comparison. Implement bounded multi-pass loser-tree merge; never open more
runs than the configured fan-in. Delete each consumed run immediately and delete all owned runs on close or failure.
Use exact leading-byte ranges only where the LMDB comparator proves range order, never as a shortcut for arbitrary
collision keys.

Port and harden Kuzu's fixed-width tuple sorter. Cap a run by both requested rows and its share of the global direct
memory budget, including radix auxiliary storage. Use stable LSD radix passes from the last tuple column to the first
with unsigned long digits. Fall back to comparison sort for small runs or direct-memory allocation failure. Store run
longs in big-endian order, merge with bounded fan-in and a loser tree, and directly move a single run.

Add an exhaustive property test over all 24 statement permutations and representative IDs on every varint-length
boundary. Compare unsigned tuple order with unsigned byte order from `TripleIndex.toKey`. Advertise tuple-order
equivalence from the current key writer; if a future writer does not advertise it, feed encoded keys to the byte-key
sorter instead.

Scan the resolved ID spool once and feed one sorter per configured statement index, dividing the direct-memory budget
among them. During each final merge, remove duplicate quads and append to that index's explicit database. Create the
inferred database empty. Build context counts, including context zero, from the first configured index's unique
stream, and require all later indexes to emit the same unique count.

Bulk-load LMDB in transactions bounded by 100,000 records or 64 MiB. Use `MDB_APPEND` only after asserting each key is
greater than the last committed key. If a write returns `MDB_MAP_FULL`, abort that bounded transaction, grow the
isolated environment through existing LMDB sizing helpers, and replay only that transaction. Create empty
free/unused-ID structures and persist store version, statement indexes, triple-term indexes, numeric-ID encoding,
inline-literal mode, and canonical language mode. Estimator snapshots are deliberately absent so existing startup
rebuild behavior remains authoritative.

Add publication protection. Accept only an absent path or a directory with no entries. Reject symlinks. Under a
loader lock, create an incomplete marker and a manifest listing every owned artifact. Build a complete generation
inside the target, while optional spill files may live under the configured temporary directory. Close all handles,
open the generation as `LmdbStore`, and stream-validate value forward/reverse mappings, hash groups, reference counts,
RDF-star term indexes, equal statement-index counts, contexts, namespaces, properties, and reconstructed next-ID
counters. Promote manifest-listed paths into the target, force files and the directory, and remove the marker last.
`LmdbStore` must reject a root containing the marker. On restart, finish publication only if all artifacts match and
validation succeeds; otherwise delete only manifest-owned paths and rebuild. Unexpected paths cause refusal.

Finally add `tools/lmdb-bulk-load`. Keep argument parsing dependency-free. Bundle the current non-legacy Rio parsers
and merge service descriptors in a shaded executable. Support store, input/stdin, format, parser mode, base URI,
statement and term indexes, memory, partitions, open files, temporary directory, inline literals, and hash-cache
options. Return status 0 for success, 2 for usage, 130 for cancellation, and 1 for load failure.

## Concrete Steps

Run all commands from `/Users/havardottestad/Documents/Programming/rdf4j`.

Before each test phase, use the repository runner, which performs the required root install without enabling tests in
reactor dependencies:

    python3 .codex/skills/mvnf/scripts/mvnf.py <TestClass#method> --module core/sail/lmdb --retain-logs

Never add Maven `-am` or `-q` to a test command. Preserve the first focused failure in `initial-evidence.txt` using
`scripts/agent-evidence.py` and the Surefire report named by the runner.

After focused tests pass, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb --retain-logs

For the CLI module, run:

    python3 .codex/skills/mvnf/scripts/mvnf.py tools/lmdb-bulk-load --retain-logs

Run formatting only after the relevant tests are green:

    cd scripts && ./checkCopyrightPresent.sh
    cd ..
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

The formatter command is repository-mandated and is not a test command. Run the final quick install with the template
from `AGENTS.md`, then rerun the two affected module suites.

Run the eventual benchmark with:

    scripts/run-single-benchmark.sh --module core/sail/lmdb \
      --class org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbBulkLoadBenchmark \
      --method bulkLoadNQuads

Repeat with `--enable-jfr` only when a throughput or allocation result needs diagnosis. Report results as JDK 25
measurements and do not generalize JIT behavior to other JDKs without evidence.

## Validation and Acceptance

The first API test creates a small N-Quads input containing duplicate statements, one IRI in subject and object roles,
case-varied language tags, a named graph, and an RDF-star term. Before implementation it fails because
`LmdbBulkLoader` does not exist. After implementation it reports the original parsed count and the deduplicated stored
count, and an `LmdbStore` opened on the target returns the exact unique statement set.

Parser differential tests feed the same valid and invalid N-Triples/N-Quads corpus through fast and Rio modes. Valid
canonical records and final statements are identical. Invalid input reports matching line and column locations.
Other registered Rio formats produce the same graph through the common staging pipeline.

Storage parity tests build one store normally and one through the loader. Every test value resolves in both
directions. The existing CRC32 collision fixtures remain distinct. Inline values have no persistent value record.
RDF-star terms resolve through every configured term index. All configured statement indexes contain the same unique
quads, and context counts match.

Constrained tests set tiny run sizes, a small memory budget, low file-descriptor fan-in, and small LMDB maps. They
must produce the same IDs and results as generous settings while forcing partition spills, multi-pass merges, and map
growth. A forked test uses a dataset whose canonical dictionary exceeds its heap and completes without
`OutOfMemoryError`.

Fault-injection tests cancel or fail parsing, partition merge, resolution, ValueStore loading, each statement-index
merge, validation, and promotion. Before promotion, only owned artifacts disappear. During promotion, the marker
remains; a retry either validates and finishes or removes only manifest-listed artifacts. A foreign file always
causes refusal.

Repository and SPARQL parity tests execute the same query corpus against normal and bulk stores, including default and
named graphs and RDF-star. Results are equal. The module suites and CLI suite finish with zero Surefire/Failsafe
failures.

The benchmark records throughput, peak heap/direct memory, temporary bytes, final store size, and map-growth count for
normal and bulk ingestion. The feature may merge on correctness and bounded-memory evidence; no claim that it is
faster is made until repeatable JMH results support that statement.

## Idempotence and Recovery

All test and build commands are repeatable. The loader never modifies a nonempty store. Generation, bucket, and run
paths contain a loader UUID and appear in the manifest before use. Cleanup resolves each candidate path and verifies
that it is an owned descendant before deletion. A marker from an unsupported manifest version or a target containing
unlisted files is an error requiring human inspection.

An interrupted implementation can resume by reading this document's `Progress`, inspecting `git status`, and running
the most focused test for the first unchecked item. Do not discard untracked files already present in the repository.

## Artifacts and Notes

The initial root quick install completed successfully before this plan was created. Its full output is in the
workspace's `maven-build.log`.

The Kuzu reference tree is read-only input for this work:

    /Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/kuzu

The implementation must carry the normal RDF4J source header and `// Some portions generated by Codex` on every new
Java source file.

## Interfaces and Dependencies

In `core/sail/lmdb`, define:

    public final class LmdbBulkLoader {
        public static Builder builder(Path target, LmdbStoreConfig config);
        public Result load(Path input, RDFFormat format) throws IOException;
        public Result load(InputStream input, String baseUri, RDFFormat format) throws IOException;
    }

    public enum ParserMode {
        AUTO, FAST, RIO
    }

    public record Result(
            long parsedStatements,
            long storedStatements,
            long persistedValues,
            long inlineValues,
            long temporaryBytes,
            long elapsedMillis) {
    }

The nested `Builder` supplies validated defaults: 256 partitions; memory
`max(32 MiB, min(1 GiB, Runtime.maxMemory()/4))`; 256 KiB LZ4 blocks; 100,000 records and 64 MiB per LMDB
transaction. It snapshots the mutable `LmdbStoreConfig` settings required by the build before consuming input.

Use `at.yawk.lz4:lz4-java:1.10.4`, already present elsewhere in this repository. Use the JDK and existing Eclipse
Collections/LWJGL dependencies for primitive state and LMDB access. Do not add Chronicle, another compression library,
a CLI framework, or runtime code generation.

Revision note (2026-07-26): Initial ExecPlan created from the accepted design after grounding Kuzu staging mechanics
and LMDB's current record layout. The plan separates the public API, storage engine, and executable module so each can
be tested incrementally without changing transactional ingestion.

Revision note (2026-07-26 21:58Z): Recorded the first failing/green API and publication slice and the deliberate
transactional scaffold. The staged core remains the active milestone and must replace that scaffold before completion.
