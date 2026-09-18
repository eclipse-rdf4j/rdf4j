# LMDB general-performance pass

## Scope and status

Baseline: the supplied `generated-value-keys-optimized-source.zip`. This revision changes five production
files: `PackedLongVector`, `CompactCsfPageEncoder`, `LmdbNativeValueCodec`, `RowBindingSetView`, and
`LmdbNativeSort`. It retains the preceding compressed-overlay formats, generated-only DISTINCT/GROUP BY
identity machinery, admission proof, and runtime-value budget. There is no new configuration switch.

The target is common work underneath several execution paths, not another mutually exclusive query
strategy. The supplied historical recording identified CSF sizing/trait work and slot replacement as
candidates, but that recording predates this source. Its sample counts are not presented as a profile
of the final implementation or as a prediction of end-to-end benefit.

The new isolated suite and the existing generated-key suite pass. Complete RDF4J compilation, actual
model/comparator integration, generated Janino execution, the original query/dataset, AArch64 execution,
and multi-query production throughput remain unverified. This is an implementation and measured
component improvement, not a claim that every part of the LMDB module is now faster or production-certified.

## Summary

* 4096-ID packed-vector planning: 1.99x to 3.04x faster in the tested distributions.
* CSF layout measurement plus native page writing: 1.37x faster, about 27% less time.
* Direct-buffer UTF-8: 4.87x faster for the short ASCII fixture, 1.88x for short Unicode, and 2.12x for
  the long Unicode fixture. The last case allocates about 4.1% more; it is not an allocation win.
* Ordered primitive sort input: 24.90x faster ascending and 19.40x descending, with half the index-array
  allocation. General random, duplicate-heavy, and fragmented-run inputs are approximately unchanged.
* Replacing an already-bound slot no longer invalidates a cached binding-domain size. The isolated
  repeated-size case improves strongly; it is not a claim about complete query execution.
* Tested CSF encodings, layout flags, fingerprints, heap/native sizes, and payload bytes are identical
  to the previous encoder. No stored index, per-record sidecar, or global cache is added.

## 1. Packed block planning: fewer passes, vectorizable reductions

The old native planner separately found a raw minimum, rescanned for residual widths, checked compound
ID types, searched typed extrema, rescanned for typed widths, and potentially made more delta passes.
The heap planner also constructed candidate records for alternative formats.

The new shared planner transforms unsigned ordering into signed ordering with `value ^ Long.MIN_VALUE`.
One reduction computes the minimum, maximum, and OR of differences from the first value. The largest
frame-of-reference residual is exactly unsigned maximum minus unsigned minimum, interpreted as an
unsigned 64-bit quantity. The subtraction can have its sign bit set; `unsignedWidth` retains that bit.
This works across `Long.MAX_VALUE` and does not narrow IDs or change their unsigned semantics.

Compound IDs are eligible only when the first value has an admissible compound type and all seven low
bits are invariant. The OR-of-XOR reduction proves that invariant across every entry. Under it, raw
unsigned extrema determine payload extrema, so typed planning does not need another scan.

For a sequential-ID hint, a separate loop still verifies unsigned monotonicity. OR of consecutive
nonnegative unsigned deltas has the same required bit width as their unsigned maximum. With invariant
low seven bits, raw differences are divisible by 128, allowing the typed delta width to be derived.
The hint is not trusted as proof: an inversion retains the already-measured FOR alternative.

Candidate order and strict-smaller selection are unchanged: RAW FOR, typed FOR, raw delta, typed delta.
Ties therefore select exactly the previous representation. Headers, alignment, padding, search-radix
metadata, prefix metadata, and reader formats are untouched. Heap planning shares the same code and
allocates only the chosen `BlockPlan` rather than several candidate records; its private scratch
constructor does not allocate a redundant 256-long staging array.

This is not a replacement of the codec by JavaFastPFOR. Its block-width packing is a useful comparison
[1], but the implementation here retains RDF4J's exact existing formats and hint semantics.

## 2. Page construction: batch source access and reject disproved metadata work

`SequentialValues.nextBlock` is a compatible default method: existing producers can continue implementing
`next()`. The private page-section producer overrides it. A section is selected once per packed block,
rather than once per lane. Each section has a single access pattern and publishes its index once at the
end. Neighbor/context tails are copied in contiguous group runs with local cursor state and a retained
previous value. Clipped row/fiber/context views and groups spanning multiple blocks are handled by the
same view accessors and are covered by byte-differential tests.

The trait scanner now proves whether datatype work can matter before invoking the lookup. If literal
representations mix legacy IDs with encoded datatype tags, or encoded tags differ, the previous scanner
would ultimately refuse the uniform-datatype flag anyway. It no longer collects and resolves legacy
IDs after that proof has failed. Entirely legacy columns still call the lookup and require complete,
nonnegative, identical datatype results. Resource kinds and literal flags keep their old conservative
semantics; this change does not assert that mixed representations have different RDF datatypes.

In the 128-row mixed fixture, layout sizing previously made two lookup calls for 341 legacy IDs and now
makes none. This is a count of actual calls into the fixture's deterministic metadata oracle, not a
claim to have measured 341 real LMDB reads. Other page cases still resolve legacy metadata when required.

Row/fiber counts and neighbor values also remain in local variables while constructing the fingerprint,
removing duplicate view accesses without changing fingerprint input order or bytes.

## 3. UTF-8: specialize at the byte-array boundary

The ByteBuffer decoder previously duplicated the buffer and passed it through the general Charset API,
then converted a CharBuffer to String. It now validates the complete requested range first. Writable
heap-backed buffers use `new String(array, arrayOffset + offset, length, UTF_8)` directly. Direct and
read-only buffers use absolute bulk `get` into the already-existing bounded thread-local byte workspace,
then the same String constructor.

OpenJDK's String implementation has specialized byte-array decoding paths [2]. Absolute bulk ByteBuffer
get leaves position unchanged and checks its range [3]. The tests additionally verify the original
position, limit, and mark, array offsets and slices, malformed replacement semantics, retained String
ownership after scratch reuse, and concurrent direct/read-only decoding. The scratch is never returned
as String storage. Bounds are checked before allocating even for deliberately enormous invalid lengths.

Memory is not universally lower. Threads that previously used only this ByteBuffer path can now populate
the existing UTF8 scratch cache: at most 64 KiB of retained staging per participating thread. Larger
inputs use temporary arrays, not an unbounded retained cache. This allocation is not charged as a new
per-record query structure. Long Unicode decoding allocates about 776 more bytes per call in the timed
fixture (4.1%) while taking about half the time. Short ASCII and Unicode allocations decrease.

Only the two complete decoder/scratch methods, their exact constants, and the ThreadLocal declaration
are compiled into the explicitly named `Utf8UnderTest` wrapper. JDK NIO/String implementations are real;
the rest of the production ValueCodec is not compiled with real RDF4J dependencies in this environment.

## 4. Binding-domain caching

A bound-to-bound replacement changes a value but not the set of bound names or its size. `slotReplaced`
now evicts domain caches only on an actual boundness transition. It still updates `boundSlotCount` and
`empty` on those transitions. Per-slot value memoization is separate and remains guarded by exact IDs.

The existing assumption that a view's base binding domain is fixed is unchanged. Tests include fixed
base domains overlapping slot names, missing/null-context slots, bound/unbound transitions, and bulk
replacement. Mutable `getBindingNames()` was not changed into a persistent cache: the optimization is
not a claim to eliminate allocations or walks that that separate API still performs.

The 16/128-slot benchmarks isolate replace-plus-size on a view without a base binding set. The `false`
cases update already-bound values; the `true` case also introduces unbound slots. Overlapping base
domains are tested for correctness, not timed here. Their large relative gain
comes from keeping an already-computed domain size rather than repeatedly scanning the slots; it must
not be extrapolated to the whole query.

## 5. Sorting: retain the general algorithm, skip unnecessary merges

Several insertion/merge variants were tested and rejected. The retained production change deliberately
leaves the previous general `sort(order, scratch, comparator)` and `merge` bodies unchanged.

Before allocating both arrays, the unbudgeted entry checks whether the entire input is already ordered
according to the real packed comparator plus stability ordinals. Ascending input needs identity indices;
strictly descending input needs reversed indices. Reversal is only valid for a strict descending run,
including tie-breaks. Equal comparator and ordinal values preserve their original input order.
OpenJDK TimSort documents the same stability requirement for reversing runs [4]; this small fast path
is independently implemented and is not a transplant of TimSort.

The budgeted entry still conservatively admits two arrays before calling the comparator, so the memory
refusal boundary does not change. On an ordered result only one array is allocated, the unused scratch
reservation is released, and the returned array remains tracked. Exceptions and close release the
appropriate reservations. The tests use the actual sort code and a reserve/release-tracking budget
double; the full application's memory manager is not exercised by that isolated test.

For random, duplicate-heavy, or fragmented-run data, detection normally stops quickly and the previous
merge sorter executes. There is a small detection cost: final random/duplicate medians are 1.0%/1.4%
slower, within the observed variability but retained in the results. This revision does not claim a
universal ORDER BY speedup, use encoded ID ordering as SPARQL ordering, or assume adjacency order proves
the order requested by a query.

## 6. Measurement protocol and every final result

The supplied Zulu JDK 26.0.2.1 ran on an AMD EPYC 9V74 x86-64 virtual machine. Five independent forks per
version/case, ten warmup passes, seven measured passes, alternating base/tuned process order, 22 cases:
220 timed JVM processes in the final comparison. Heap settings were `-Xms512m -Xmx2g`; assertions and
native access were enabled. No forced-inline, compile-only, compilation-threshold, or CPU-affinity flags
were used. Diagnostics were recorded in separate processes and are excluded from these timings.

The value is the median of five per-fork medians. The range is the minimum and maximum of those five
medians, not a confidence interval. Allocation is measured on the executing thread with ThreadMXBean,
not retained heap, process RSS, or allocations on other threads. Raw samples, commands, and all forks
are included in `paired-final/`; slower forks are not omitted.

Vector cases process 4096 IDs. CSF page fixtures have 512 root rows, three or four neighbors per row,
and two or three contexts per fiber, with reused workspace. `page-core-write` includes sizing and writing.
Short text, long text, direct/heap and malformed UTF-8 fixtures are distinct. Buffers and primitive sort
rows are prepared outside the timing. Sort rows have two numeric columns and ordinals; no real SPARQL
ValueComparator or dictionary latency is simulated. Generated-key regression tests still forbid
storage access; they are not included as new performance claims in this pass.

All timing columns below are **nanoseconds per complete operation**, not per row unless the operation
itself is one replacement or decode.

| Case | Baseline median [fork range] | Final median [fork range] | Ratio base/final | Allocation bytes/op, base -> final |
|---|---:|---:|---:|---:|
| `csf-measure-random` | 4,277.339 [4,235.144, 4,347.333] | 2,153.113 [2,096.402, 2,221.316] | 1.99x | 0.000 -> 0.000 |
| `csf-measure-typed` | 6,463.659 [6,431.714, 6,664.945] | 2,242.072 [2,194.184, 2,319.304] | 2.88x | 0.000 -> 0.000 |
| `csf-measure-typed-seq` | 11,962.652 [11,681.464, 12,546.210] | 3,929.945 [3,867.097, 4,470.910] | 3.04x | 0.000 -> 0.000 |
| `csf-page-core` | 76,954.855 [76,489.858, 89,275.378] | 57,254.313 [56,645.775, 61,925.565] | 1.34x | 0.000 -> 0.000 |
| `csf-page-core-write` | 149,779.843 [146,947.218, 169,882.070] | 109,192.373 [108,164.238, 117,082.858] | 1.37x | 0.000 -> 0.000 |
| `csf-page-legacy` | 80,327.855 [78,363.660, 86,830.778] | 64,953.310 [63,315.905, 66,821.690] | 1.24x | 0.000 -> 0.000 |
| `csf-page-mixed` | 80,675.258 [75,453.680, 84,259.153] | 59,214.613 [57,702.138, 59,755.080] | 1.36x | 0.000 -> 0.000 |
| `csf-write-typed` | 36,437.484 [36,117.469, 45,105.766] | 26,513.719 [25,663.047, 31,528.969] | 1.37x | 0.000 -> 0.000 |
| `domain-128-false` | 25.787 [25.386, 26.510] | 1.142 [1.050, 1.219] | 22.58x | 0.000 -> 0.000 |
| `domain-16-false` | 7.992 [7.438, 8.412] | 1.080 [1.070, 1.217] | 7.40x | 0.000 -> 0.000 |
| `domain-16-true` | 9.855 [9.331, 10.247] | 6.324 [6.209, 6.616] | 1.56x | 0.000 -> 0.000 |
| `sort-duplicates-16384` | 1,251,645.375 [1,242,197.656, 1,307,365.000] | 1,269,000.406 [1,238,020.406, 1,316,400.563] | 0.99x | 131,104.000 -> 131,104.000 |
| `sort-random-16384` | 1,445,041.313 [1,442,017.375, 1,502,418.438] | 1,459,839.938 [1,448,496.844, 1,479,430.219] | 0.99x | 131,104.000 -> 131,104.000 |
| `sort-random-64` | 927.438 [893.333, 958.902] | 912.098 [889.319, 942.407] | 1.02x | 544.000 -> 544.000 |
| `sort-reverse-16384` | 348,861.531 [342,812.094, 356,386.375] | 17,978.438 [16,410.438, 19,210.625] | 19.40x | 131,104.000 -> 65,552.000 |
| `sort-runs-16384` | 753,512.813 [432,678.375, 771,025.875] | 753,269.000 [395,102.438, 764,409.000] | 1.00x | 131,104.000 -> 131,104.000 |
| `sort-sorted-16384` | 269,674.563 [263,341.219, 272,852.781] | 10,829.531 [9,694.344, 15,604.219] | 24.90x | 131,104.000 -> 65,552.000 |
| `utf8-ascii` | 101.299 [99.645, 109.373] | 20.798 [19.416, 43.663] | 4.87x | 356.094 -> 102.047 |
| `utf8-heap` | 46.939 [45.490, 50.571] | 18.651 [18.230, 18.931] | 2.52x | 348.094 -> 102.047 |
| `utf8-long` | 9,860.865 [9,480.340, 10,083.090] | 4,659.214 [4,498.948, 4,863.746] | 2.12x | 19,090.563 -> 19,866.563 |
| `utf8-malformed` | 138.204 [133.139, 146.712] | 68.094 [66.446, 73.069] | 2.03x | 490.141 -> 236.094 |
| `utf8-unicode` | 143.506 [134.738, 150.643] | 76.332 [74.528, 80.080] | 1.88x | 406.047 -> 294.047 |

An ASCII fork is substantially slower than its other final forks, and fragmented-run sorting is bimodal
in this shared VM. These ranges limit how precisely the medians should be treated. The original query
has not been rerun; multiplying these component speedups or adding historical JFR sample percentages
would not establish its speedup.

## 7. C2 observations from the final source

`c2-baseline` and `c2-final` contain raw C1/C2 output, verified C2 main-code bytes, and objdump assembly.
The disassembler requires every byte in the reported main-code interval; it never infers missing code.
All recompilations and OSR versions are retained. Compiler profiles vary between independent diagnostic
runs, so sizes are observations, not a stable ABI or cycle estimate.

The most important structural result is automatic SIMD for unsigned planning. In the final normal
`PackedLongVector::planBlock(long[], int, Hint, WriteScratch)` entry, its shared core is inlined and
contains AVX-512 signed min/max reductions over sign-biased IDs:

```asm
vpminsq zmm7,zmm9,zmm7
vpmaxsq zmm8,zmm9,zmm8
vpminsq zmm7,zmm10,zmm7
vpmaxsq zmm8,zmm10,zmm8
```

These exact instructions occur in `c2-final/vector-asm/02-PackedLongVector-planBlock.asm`. The old normal
entry uses scalar comparisons/conditional moves for extrema. No Vector API or manual SIMD code was added.
This is x86-64 AVX-512 evidence, not a prediction of the same instructions or gain on AArch64 or AVX2.

Selected normal C2 captures:

| Method / warmed context | Baseline main code | Final main code | Interpretation |
|---|---:|---:|---|
| Native planner entry, including inlined shared core | 5584 B | 4016 B | Fewer passes and vectorizable reduction |
| Extracted ByteBuffer `decodeUtf8` | 5208 B | 3072 B | Specialized byte-array String path |
| Mutable binding-view `size`, bound-to-bound profile | 624 B | 128 B | Existing cached size remains valid |
| `slotReplaced`, same profile | 272 B | 160 B | No eviction on unchanged domain |
| General native `sort`, duplicate-heavy profile | 3208 B | 3208 B | Original fallback body retained |
| Full CSF `tryMeasure`, not its 21-byte wrapper | 14680 B | 21616 B | More inlined section-specific work; code footprint grows |

The page producer's block method also compiles into a larger method than the old scalar next method.
The intended advantage is dispatch/local-state amortization and simpler section loops, not universally
smaller assembly. The sorted/reverse diagnostic processes show compiled `orderedRun` and `orderedIndices`
without merge execution; their logs are separate from duplicate-heavy fallback diagnostics.

## 8. Experiments that did not survive

The first scalar producer version sped packed planning but left page work much less improved. Moving
section dispatch and cursor publication to block granularity improved the final page cases. Its baseline-
relative patch and two-fork screening measurements remain in the evidence/source bundle.

A sixteen-element linear-insertion seed and merge rewrite improved some random cases but made the
screened duplicate-heavy case roughly 29% slower. Smaller linear seeds, binary insertion, and alternative
merge layouts did not give a sufficiently reliable general win. C2 inspection showed much larger hot
sort/merge code and inlining refusals in some variants; that supports investigating code footprint but
does not prove a single-cause explanation for the timing changes.

The final version keeps only complete-run recognition and preserves the old fallback. Historical
`sort-linear*`, `sort-binary16`, `sort-noinsert`, `sort-runonly`, `sort-runsplit`, and `sort-runsplit-lazy`
patches are independent diffs against the previous source; they are not sequential patches. Screening
results use two forks and are not mixed into the final five-fork table.

## 9. Tests and remaining integration boundary

Six suites pass, totaling **7,633,451 counted checks**:

| Suite | Checks | Main coverage |
|---|---:|---|
| CSF | 3,256,479 | 3916 vectors, 576 page views, exact previous bytes, unsigned widths 0..64, modes/hints, native canaries, partial/group-spanning views |
| UTF-8 | 343,391 | Every 1-byte/2-byte input, random malformed bytes, heap/direct/read-only/sliced buffers, boundaries, source state, reuse, concurrency |
| Binding domain | 1,306,622 | Bound/unbound, fixed overlapping base domains, null-context slots, bulk replacement, exact values |
| Native sort | 1,751,262 | Stable reference sorting, equal/reversed ordinals, small/large boundaries, fragmented runs, memory refusal/releases |
| Previous generated keys | 43,337 | Zero dictionary calls, actual key tables, native DISTINCT, grouping, bounded spill and merge |
| Previous optimized keys | 932,360 | Collisions, bounds/budgets, aliases, 8-writer publication/resize, concurrent close |

The UTF-8 concurrency exercise performs 80,000 additional decode iterations inside tasks; only the
explicit checks/results are included in the counted total. Its final test strengthens direct/read-only
scratch reuse and returned-String ownership. That test-only change does not alter timed benchmark bodies
or any production file. Manifests retain the exact earlier timed harness and fresh patch-verification
builds separately.

The CSF/UTF8 test compilation contains 28 files, binding-view compilation 14, generated/sort compilation
56. These counts are compilation units, not independent production modules. Manifests label complete
production files, complete extracted units, name-only baseline copies, API doubles, and test code.
The final freshly patched-tree test execution is included separately as `patch-verification-tests`.

No external double implements compression, native reads/writes, UTF-8, hash membership, or sorting. They
do replace absent RDF model/Binding/metadata/logger APIs, numeric comparator fixtures, and budget APIs.
Therefore the suite is not equivalent to the full RDF4J module build. In particular the modified full
ValueCodec and generated Janino calling paths still need compilation/execution against the full branch.

Storage-format compatibility is demonstrated against the previous encoder corpus and reinforced by
unchanged format/tie rules; it is not a test of every possible 64-bit input. Memory safety checks and
existing ownership/budget rules are retained. No new global table or per-record index is introduced,
but the UTF8 thread-local reuse and larger compiled page code are real memory tradeoffs.

## 10. Delivery and reproduction

The main patch is against `generated-value-keys-optimized-source.zip`, the production patch changes only
five production files, and the cumulative patch is against the original judges' archive. Apply one,
not multiple patches. Source root means the directory containing `java/` and `tools/`.

```sh
git apply --check lmdb-general-performance.patch
git apply lmdb-general-performance.patch
```

All original baseline files are retained in the full source ZIP. New evidence/build outputs and Python
bytecode caches are excluded. Verification checks each patch on a fresh input, compares resulting files
to the ZIP, checks experiment patches independently, recompiles/reruns from a fresh patched tree, and
records checksums. Complete reproduction commands and dependency-double boundaries are in
`tools/lmdb-performance/README.md`. The verification JSON and SHA-256 file accompany the deliverables.

The important next integration measurement is the real query's steady-state interval, separately from
CSF/overlay build and maintenance. This revision optimizes common components; it does not change join
planning, persistent transaction semantics, LMDB's B+tree implementation, or generated payload spilling.

## Primary implementation research

[1] JavaFastPFOR `BinaryPacking`, author-maintained implementation: block maxima and packed widths.
https://raw.githubusercontent.com/lemire/JavaFastPFOR/master/src/main/java/me/lemire/integercompression/BinaryPacking.java

[2] OpenJDK `String` implementation: byte-array charset and UTF-8 constructor paths. This current source
was consulted for implementation structure; actual measurements use the supplied Zulu 26.0.2.1 binary.
https://raw.githubusercontent.com/openjdk/jdk/master/src/java.base/share/classes/java/lang/String.java

[3] Java SE 26 `ByteBuffer`: absolute bulk get, array access and bounds/position contracts.
https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/nio/ByteBuffer.html

[4] OpenJDK `TimSort`: ascending/strict-descending run recognition and stability of reversal.
https://raw.githubusercontent.com/openjdk/jdk/master/src/java.base/share/classes/java/util/TimSort.java
