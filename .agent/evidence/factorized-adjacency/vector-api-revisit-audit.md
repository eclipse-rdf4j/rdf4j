# Java Vector API revisit: candidate census

This audit belongs to the living ExecPlan **Factorized adjacency IR with morsel parallelism**. It covers the
LMDB module's 406 production Java sources, including the 250 sources containing an explicit `for` or `while` loop.
The first pass searched adjacency/CSF storage, the LMDB read/write paths, the native engine, factorized execution,
IR lowering/interpreting/emission, Janino runtime helpers, joins, aggregation, SIP, sorting, value IDs, and bulk
ingest. A loop is a Vector API candidate only when its useful work is a sufficiently long, independent operation
over contiguous primitive data. Cursor state machines, pointer chasing, scatter, short tuple comparisons, and
variable-length decoding are recorded as exclusions rather than silently ignored.

## Candidate matrix

| Area | Concrete production sites | Candidate operation | Initial disposition |
|---|---|---|---|
| CSF packed vectors | `PackedLongVector` block planning, full-width decode, FOR decode, block sum, prefix decode | unsigned min/max, add/base restore, reductions, fixed-width unpack, prefix scan | Benchmark 256-lane blocks; raw-address access and arbitrary bit widths may erase SIMD gains |
| Legacy adjacency codec | `LmdbAdjacencyRunCodec.copyPackedLanes`, `copyByteLanes`, width calculation | 8/15/16/32/64-bit unpack and base add | Benchmark fixed widths; keep 15-bit and variable-width scalar unless measurements prove otherwise |
| CSF page headers | `CompactCsfPageEncoder.columnTraitFlags` and datatype classification fallback | bulk ID kind/type uniformity checks | Benchmark build-time classification; query-time uniform pages already skip per-ID work |
| Root/fiber batches | `NativeLmdbQuerySource.RootBatch`/`FiberBatch`, copied adjacency fibers | term-kind counts, unsigned ranges, transitions, multiplicity sums | Benchmark contiguous decoded arrays; promote only where a real consumer avoids another pass |
| IR/Janino filters | `evaluation.codegen.KernelRuntime` dense equality, inequality, unsigned range, column equality | masks, count-only filters, selection production | High-priority benchmark; count-only aggregate tails can avoid selection writes entirely |
| Native hash batches | `LmdbNativeHashJoin.hashBatch`, `LmdbNativePrimitiveTupleTable.hashBatch` | dense/gathered Murmur-style mixing and finalization | Benchmark dense and indirect selections separately; random table probes remain scalar |
| Factorized aggregates | `LmdbFactorizedBatch.sumLeafMultiplicity`, validity popcount, packed F-tree weights | reductions and bitmap population count | Benchmark exact overflow/validity semantics; dense mask popcount already measured at only 1.09x |
| Ordered distinct work | packed F-tree run collapse, chunk/SIP/leapfrog dedupe, order validation | adjacent transitions and sortedness checks | Transition count is promising in isolation; in-place compaction still needs scalar emission |
| SIP and set kernels | sorted domain merge/gallop, dense masks, Bloom/membership probes | block rejection, mask AND/popcount | Merge/gallop is data-dependent; dense-mask prototype failed the 1.50x gate; benchmark block rejection only |
| LMDB bulk ingest | `FastNTriplesParser` delimiter scans, packed builders, radix passes | byte delimiter masks, independent histogram transforms | Benchmark byte scanning; radix scatter and variable-length token parsing stay scalar |
| Value IDs | `ValueIds.termKind` and core-datatype bit extraction | bulk kind/datatype masks | Benchmark only batched consumers; single-ID calls are below profitable SIMD grain |
| Generated aggregates | IR aggregate update loops and Janino aggregate drains | simple count/sum reductions | Use stable backend helpers from generated code; never link generated classes directly to incubator types |

## Structural exclusions

- LMDB B-tree navigation and record iteration are native calls plus pointer-dependent cursor state. Their key
  comparisons already enter native `memcmp` and are not Java Vector API candidates.
- `System.arraycopy`, `Arrays.fill`, and primitive `Arrays.sort` are HotSpot/library intrinsics or tuned runtime
  implementations. Replacing them with an incubating API needs direct evidence and is not assumed beneficial.
- Root-domain heap merge, leapfrog intersection, galloping search, binary search, hash-table probing, and adjacency
  overlay/tombstone merge all choose the next address from the preceding comparison. SIMD can at most reject a
  contiguous block before the scalar algorithm; it cannot replace their controlling state machine.
- Context expansion, RDF term materialization, datatype lookup, expression evaluation, and Janino compilation are
  object-, branch-, or service-dominated. Only their already-decoded primitive batch boundaries qualify.
- Planner/cost-model vectors are tiny (normally a handful of dimensions), so Vector API setup exceeds useful work.
- Tuple-key equality usually compares one to four longs and is below the preferred AArch64 vector grain.
- Variable-length varint and arbitrary-width bit-packed decoding crosses data-dependent byte/word boundaries. Only
  common fixed widths and full blocks will be measured.

## Promotion rule

A production operation is promoted only if matched AArch64 JMH shows at least 1.50x kernel throughput and profiling
or an affected end-to-end benchmark proves material relevance. The provider must be isolated from scalar classes,
selected once per JVM, and loaded only when `ModuleLayer.boot()` resolves `jdk.incubator.vector`. `auto` without the
module remains exact scalar execution; generated Janino source calls only the stable scalar-facing facade.

## Existing matched evidence

At 8,192 longs, the earlier prototype measured 1.98x for adjacent transition counting, 1.85x for plain summation,
1.56x for unsigned range counting, and 1.09x for dense mask intersection/popcount. The first three therefore move
to consumer/materiality testing; the mask kernel is rejected unless a different formulation changes the result.

## 2026-08-30 expanded AArch64 screen

Matched JDK 26 forks at 8,192 elements compared scalar with `jdk.incubator.vector` unresolved against forced Vector
API with the module resolved. The screen used three one-second warmups and five one-second measurements. Ratios below
are scalar time divided by vector time; larger is better.

| Kernel | Scalar ns/op | Vector ns/op | Speedup | Decision |
|---|---:|---:|---:|---|
| Six-way parser structural-byte mask | 6,160.5 | 453.5 | 13.59x | Confirm with actual next-delimiter search and ingestion benchmark |
| Mixed RDF term-kind count | 7,387.6 | 3,537.9 | 2.09x | Confirm with exact page-trait classifier and CSF build benchmark |
| Adjacent transition count | 4,041.3 | 2,207.6 | 1.83x | Confirm only where consumer needs a count; compaction remains separate |
| Plain multiplicity sum | 1,953.8 | 1,147.0 | 1.70x | Confirm exact overflow/validity semantics and an affected aggregate |
| Uniform IRI-kind count | 3,556.4 | 2,238.0 | 1.59x | Confirm with exact page-trait classifier |
| Unsigned range count | 3,053.7 | 2,198.1 | 1.39x | Reject under the 1.50x requirement |
| Equality selection positions | 3,520.1 | 3,134.2 | 1.12x | Reject |
| Dense mask AND/popcount | 2,208.1 | 2,027.9 | 1.09x | Reject |
| Equality count / two-column equality | 2,193.4 / 2,214.6 | 2,049.0 / 2,196.1 | 1.07x / 1.01x | Reject |
| Unsigned range selection positions | 4,522.4 | 7,133.1 | 0.63x | Reject |
| Full-width long decode/add | 769.6 | 1,302.0 | 0.59x | Reject; scalar loop is already optimized |
| Unsigned short/int widening decode | 1,194.8 / 834.8 | 2,337.5 / 1,832.1 | 0.51x / 0.46x | Reject |
| Dense/indirect three-column hash | 13,814.6 / 22,610.8 | 86,665.8 / 215,128.2 | 0.16x / 0.11x | Reject |
| Unsigned byte widening decode | 1,163.3 | 100,579.5 | 0.012x | Reject; AArch64 widening conversion is unsuitable here |

Artifacts: `vector-revisit-scalar-unresolved-jdk26-aarch64.{txt,json}` and
`vector-revisit-forced-jdk26-aarch64.{txt,json}` in this evidence directory.

### Exact delimiter and end-to-end qualification

The exact `FastNTriplesParser` next-quote-or-backslash operation (not the synthetic six-way byte count) measured
5.37x at 32 bytes, 6.67x at 64, 8.64x at 128, 10.59x at 256, 12.47x at 2,048, and 12.85x at 8,192. The oracle
compared exact returned positions over empty, short, full-block, and masked-tail lengths. However, the existing
`LmdbBulkLoadBenchmark.bulkLoadNQuads` `LONG_LITERAL` scenario measured 2,803.417 ms/op for 10,000 2-KiB literals.
Both the literal scan and line-break scan together account for only roughly 11 ms at their scalar microbenchmark
cost, so even their theoretical elimination is below 0.5% of the complete load. The parser operation passes the
kernel gate but not the current materiality gate; it is not promoted on this evidence alone.

Artifacts: `vector-literal-scan-{scalar,vector}-jdk26-aarch64.{txt,json}` and
`bulk-long-literal-baseline-jdk26-aarch64.txt` in this evidence directory.

### Production-facade qualification and final decision

The production-facade benchmark invalidated the scratch page-classifier result. At 8,192 mixed core IDs, forced scalar
classification measured 4,934.2 ns/op, while the real reflective optional provider measured 11,161.7 ns/op (2.26x
slower). Classification while collecting legacy IDs measured 4,942.4 versus 10,072.9 ns/op (2.04x slower). The page
classifier was therefore removed rather than hidden behind a dataset heuristic. CSF page traits retain their original
one-pass scalar encoder.

The exact production-facade delimiter kernel did pass the 1.50x micro gate at every tested dispatch size:

| Bytes | Scalar ns/op | Vector ns/op | Speedup |
|---:|---:|---:|---:|
| 32 | 10.893 | 3.326 | 3.27x |
| 256 | 74.254 | 16.815 | 4.42x |
| 2,048 | 586.832 | 120.019 | 4.89x |
| 8,192 | 2,317.951 | 468.934 | 4.94x |

The matched affected end-to-end control did not pass materiality. `LmdbBulkLoadBenchmark.bulkLoadNQuads` with 10,000
2-KiB literals measured 2,634.580 ms/op forced scalar and 2,628.627 ms/op forced vector: a 1.0023x ratio, or 0.23
percent. This is below the three-percent promotion floor and inside the scalar run's uncertainty. The production Vector
API facade, provider, compiler-module option, parser integration, and provider tests were removed. The benchmark-only
oracle and prototypes remain as reproducible evidence; ordinary builds have no incubator-module dependency.

Artifacts: `vector-integrated-{scalar-unresolved,scalar-resolved,vector}.json`,
`vector-byte-integrated-{scalar,vector}.json`, and `vector-bulk-long-literal-{scalar,vector}.json`.

### Live-delta and comparison-free metadata controls

`DirectAdjacencyBenchmark` now contains `metadataOnlyRunSize`, which resolves run handles during trial setup and times
only `NativeAdjacency.size(handle)`. It performs no root/neighbor/context comparison and decodes no payload. With 4,096
fibers and mixed contexts, 256 counts measured 4,159.020 ns against the retained base (16.25 ns/count) and 2,782.546 ns
with a live insertion overlay (10.87 ns/count). Full neighbor copying measured 3,245.458 ns base and 3,825.783 ns with
the overlay, quantifying a 17.9-percent delta-merge cost. The arbiter must keep the first operation metadata-only; a
Vector API scan would add work and be architecturally wrong. Overlay/tombstone merge remains scalar because its next
address depends on the prior unsigned comparison.

Artifact: `vector-delta-metadata-counts.json`.

### Analytical acceptance recheck

With all retained global synopses disabled, ANALYTICS q0 followed by q1 measured 0.040 and 0.061 ms/op. q6/q7/q8
measured 6.229, 9.124, and 37.049 ms/op and the benchmark verified 24, 125, and 56 result groups. Relative to the user's
52.680, 66.785, and 217.530 ms historical controls, these are 8.46x, 7.32x, and 5.87x faster. These kernels use the
factorized adjacency IR and page/root/fiber metadata; resolving the Vector API module cannot improve a path that never
performs the rejected primitive scan.

Artifacts: `analytics-q0-q1-synopsis-off.json` and `analytics-q6-q8-synopsis-off.json`.
