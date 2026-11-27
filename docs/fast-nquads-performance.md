# Fast N-Quads parsing and staging: changes and measurements

This change reduces repeated scanning, validation, and allocation in RDF4J's existing byte-oriented N-Triples/N-Quads parser and improves its existing parallel canonical-staging path. Against corrected baseline commit `2a843fff41f53a118ce92295be76a4af515a58a0`, the latest block-parser comparison measures **1.45–3.40× higher throughput in nine synthetic cases**. The escaped-IRI case is near-neutral: its 1.08× mean ratio has overlapping reported error intervals. These measurements cover parsing and canonical staging; they establish no end-to-end LMDB import speedup.

## What changed

**Word scanning.** `FastByteScan` uses Java `VarHandle` loads to inspect eight bytes at a time for line endings, quotes, backslashes, IRI endings, and non-ASCII bytes. This is **64-bit SWAR**, not hardware SIMD or the incubating Vector API. Loads stay within the supplied slice; remaining bytes use a scalar loop. The zero-byte detector's earliest match is exact, although borrow propagation can mark later lanes. Line counting consequently finds successive matches instead of counting detector bits.

**Validation and canonical bytes.** The baseline already emitted canonical `byte[]` terms without constructing Java strings or RDF model objects. The new ordinary-IRI path combines delimiter scanning with the existing strict IRI validator. Escaped, rejected, and unterminated IRIs return to the scalar parser from the original token start, retaining diagnostic columns. Literal scanning skips UTF-8 validation only for proven ASCII bytes; non-ASCII and escaped content retain full validation. Language tags are folded directly into the final canonical buffer. Stream parsing reuses its 4 MiB input buffer and uses a smaller gzip-sniffing buffer.

**Bounded vocabulary reuse.** Each parser has separate predicate, graph-IRI, and datatype caches: a last-hit shortcut and 32 direct-mapped slots per role, with raw tokens limited to 256 bytes. Every hit checks complete token bytes and the closing delimiter; fingerprint collisions affect hit rate, not term identity. Entries are validated before insertion. Escaped keys own copies, and unescaped keys share immutable canonical payloads. Sinks may retain these arrays but must not modify them. Blank-node labels and literal values are not cached this way. The slot index uses high bits of a mixed byte fingerprint.

**Parallel work and framing.** Workers now supply physical-line counts from parsing, removing the coordinator's separate full-input counting pass. Ordered commitment rebases failures to absolute lines while preserving columns, document boundaries, BOM eligibility, and blank-node scope. A produced-minus-committed sequence window bounds queued, running, and completed-but-uncommitted blocks together; a slow early block cannot cause unlimited later results to accumulate after their task leases are released. Carry tails avoid an intermediate copy when safe. Record writes reserve once and use big-endian word stores for the unchanged 13-byte header.

## Block-parser measurements

The Linux runtime exposes nine logical CPUs, affinity 0–8, and approximately 9.7 GiB of physical memory. CPU and memory quota files are unavailable, so this is a measurement of the exposed environment rather than a claim about dedicated physical cores.

Both sides use identical benchmark code and deterministic, in-memory corpora of 4,096 statements, with the parser reused across invocations. JMH 1.37 runs on OpenJDK **25.0.5+8**, one benchmark thread, **two forks, three one-second warmups, four one-second measurements, and `-Xms512m -Xmx512m`**, with the GC profiler. `@OperationsPerInvocation(4096)` makes the reported operation one statement. Throughput below is millions of statements per second; `±` is JMH's reported score error. Allocation is bytes per statement, including emitted canonical arrays.

| Corpus | Baseline Mstmt/s | Candidate Mstmt/s | Mean ratio | Allocated B/stmt, baseline → candidate |
|---|---:|---:|---:|---:|
| `IRI` | 2.657 ± 0.095 | 6.407 ± 0.304 | 2.41× | 216.0 → 112.0 |
| `LANGUAGE` | 2.755 ± 0.210 | 6.455 ± 0.408 | 2.34× | 287.8 → 159.8 |
| `ESCAPED` | 1.848 ± 0.119 | 6.286 ± 0.167 | 3.40× | 304.0 → 136.0 |
| `UNICODE` | 2.562 ± 0.116 | 5.152 ± 0.335 | 2.01× | 294.1 → 166.1 |
| `UNIQUE` | 2.482 ± 0.078 | 3.605 ± 0.104 | 1.45× | 224.0 → 224.0 |
| `ESCAPED_IRI` | 1.899 ± 0.118 | 2.048 ± 0.250 | 1.08× | 224.0 → 224.0 |
| `VOCABULARY` | 2.489 ± 0.152 | 4.788 ± 0.155 | 1.92× | 216.0 → 119.0 |
| `LONG_LITERAL` | 0.399 ± 0.021 | 0.907 ± 0.079 | 2.27× | 2519.8 → 2415.8 |
| `SHORT` | 6.558 ± 0.392 | 10.789 ± 0.767 | 1.65× | 128.0 → 64.0 |
| `TRIPLE_TERM` | 1.930 ± 0.278 | 3.906 ± 0.337 | 2.02× | 528.0 → 352.0 |

The cases cover ordinary IRIs, language tags, escaped literal/datatype content, Unicode IRIs and labels, unique predicates/graphs, escaped IRIs with unique vocabulary, a rotating 16-predicate/eight-graph vocabulary, long ASCII literals, short URNs, and RDF 1.2 triple terms. Subjects remain unique. The unique-vocabulary rows are useful limits: allocation remains unchanged while ordinary unique IRIs improve through scanning; escaped unique IRIs still use the scalar path. Long literals retain their large output arrays, so allocation falls only about 4% despite higher throughput.

The comparison uses `baseline-v3-block.json` and `candidate4-block.json`. The baseline rerun was roughly 10–20% faster than the initial baseline on several cases, reducing the apparent ratios. Earlier, slower baseline numbers are excluded from this table. This observed run-to-run variation and the escaped-IRI uncertainty matter when interpreting the means; the table is evidence for these workloads on this runtime, not a universal multiplier.

## Canonical staging measurements and limits

The staging benchmark uses 131,072 statements per invocation, a 27.4 MiB ASCII corpus, one JMH thread, and 1/2/4/8 production workers. Every configuration has 16 partitions, a 256 MiB loader budget, 1 MiB parallel input blocks, and no compression. Subjects and labels are unique in both distributions; `REPEATED` has 32 predicates/four graphs, while `UNIQUE` has unique predicates and graphs.

The latest pair is `baseline-v3-staging-ascii.json` versus `candidate4-staging-ascii.json`: OpenJDK 25.0.5+8, two forks, two one-second warmups, four two-second measurements, and a fixed 1 GiB heap. These direct runner invocations collected no allocation metrics; the reproduction helper enables the GC profiler by default.

| Vocabulary | Workers | Baseline Mstmt/s | Candidate Mstmt/s | Mean ratio |
|---|---:|---:|---:|---:|
| `REPEATED` | 1 | 0.653 ± 0.081 | 0.799 ± 0.061 | 1.22× |
| `REPEATED` | 2 | 1.589 ± 0.235 | 2.086 ± 0.278 | 1.31× |
| `REPEATED` | 4 | 2.622 ± 0.183 | 3.160 ± 0.533 | 1.21× |
| `REPEATED` | 8 | 2.740 ± 0.234 | 3.310 ± 0.142 | 1.21× |
| `UNIQUE` | 1 | 0.506 ± 0.024 | 0.546 ± 0.029 | 1.08× |
| `UNIQUE` | 2 | 0.843 ± 0.044 | 0.937 ± 0.044 | 1.11× |
| `UNIQUE` | 4 | 1.054 ± 0.108 | 1.227 ± 0.057 | 1.16× |
| `UNIQUE` | 8 | 0.976 ± 0.162 | 1.260 ± 0.107 | 1.29× |

Mean gains are 1.08–1.31×; several reported intervals overlap. Candidate scaling largely plateaus between four and eight workers on this workload. The older staging baseline is excluded, consistent with using the latest parser baseline rerun.

Timed work includes parsing, validation, canonicalization, routing, de-duplication, predicate counting, writes, flush/close, and lazy worker startup. Corpus generation, workspace/scheduler setup, result checks, and cleanup are outside timing. Input is in memory and output uses buffered temporary-file writes. LMDB index construction, cold-file reads, durable-write latency, and full database-import throughput are outside this measurement. Invocation-level fixture allocations can appear in staging GC metrics.

The new caches reserve 64 KiB per worker and parser format. The sequence window bounds retained result count, **not exact heap use**. Existing task-memory estimates do not precisely account for canonical-record expansion, frame sizes, or retained growable worker buffers. Scheduler reservation peaks therefore must not be presented as measured total heap consumption.

## Remaining sampled costs

A 100-second JFR recording of repeated-vocabulary ASCII staging with four workers collected 25,125 Java execution samples. `CanonicalTermCodec.routeHash64` accounted for 40.80% of their top frames, `ByteKey.equals` for 7.11%, and `countPredicate` for 3.91%. Stable hashing and predicate-count lookups remain prominent sampled Java costs. These shares cover the whole recording, including invocation fixtures; they are not wall-clock or measured CPU-time percentages. The stable route hash remains unchanged. The recording contained no JFR data-loss events.

## References and review

The implementation choices were informed by primary-source inspection:

- [Jackson's UTF-8 parser](https://github.com/FasterXML/jackson-core/blob/3.x/src/main/java/tools/jackson/core/json/UTF8StreamJsonParser.java) and [byte-quad canonicalizer](https://github.com/FasterXML/jackson-core/blob/3.x/src/main/java/tools/jackson/core/sym/ByteQuadsCanonicalizer.java): lookup on byte representations and full key verification motivate bounded validated vocabulary reuse.
- [simdutf's scalar UTF-8 implementation](https://github.com/simdutf/simdutf/blob/master/include/simdutf/scalar/utf8.h): word-sized ASCII skipping followed by scalar Unicode validation supports the portable scanning approach.
- [Oxigraph's line chunker](https://github.com/oxigraph/oxigraph/blob/main/lib/oxttl/src/chunker.rs) and [N-Quads parser](https://github.com/oxigraph/oxigraph/blob/main/lib/oxttl/src/nquads.rs): line-aligned independent work informs parallel framing; RDF4J retains CR, LF, CRLF, and absolute-error semantics.
- [simdjson's `parse_many` design](https://github.com/simdjson/simdjson/blob/master/doc/parse_many.md): its staged parsing pipeline informs work placement. This patch does not implement simdjson's structural index or SIMD algorithms.

Added regression tests exercise word alignment/tails, malformed UTF-8 and exact diagnostic columns, cache collisions and mutable-input reuse, parser reuse after failure, multi-input state, ordered failures, cancellation, and bounded admission behind a latched slow worker.

## Verification

The final integrated checkout, based on the existing main-merge commit `f5710cbcf1050d7613aee308addddef6c8d770a8`, passed module verification across all 40 LMDB bulk-package test classes: **1,114 tests, 1,112 passed, zero failures, zero errors, two conditional skips**. Coverage includes 479 RDF 1.2 conformance cases, exhaustive Unicode scalar checks, canonical-byte compatibility, reopened-store equality, malformed-input non-publication, and parallel resource/cancellation behavior. The two skips are the Windows launcher on Linux and a historical format-six compatibility check requiring an older binary. The formatting gate passed, and final production source hashes match the measured candidate.

The clean whole-reactor build compiled LMDB but stopped in the unrelated Workbench frontend plugin because Maven Resolver API/util 1.9.24 artifacts were absent from the offline cache. An online retry failed Maven Central DNS resolution. The subsequent LMDB-and-dependency install and LMDB module `verify` both succeeded; this report does not claim a successful whole-reactor build on the merged head.

See [reproducible benchmark instructions](fast-nquads-benchmarks.md) for isolated builds, identical harness verification, runtime snapshots, parameter selection, and raw evidence. The [machine-readable results](benchmarks/fast-nquads-results.json) preserve exact means, reported errors, allocation values, ratios, runtime settings, and source-result hashes for the tables.
