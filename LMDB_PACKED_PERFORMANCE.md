# LMDB packed-I/O performance pass

Baseline: **lmdb-general-performance-source.zip**. Released production candidate:
**07-conservative-native.patch**. Only `csf/PackedLongVector.java` and `csf/LeBytes.java`
change in production. The complete previous module, generated-key implementation,
overlay formats and tools are retained. No new production configuration flag exists.

This pass replaces byte-at-a-time packing with word-at-a-time emission, removes
redundant native payload clearing, and speeds up bounds-checked heap reads. Native
scalar/bulk decoding is deliberately left unchanged after mixed-profile experiments
showed regressions. These are component results, not an end-to-end query speedup.

## Final measured comparison

The final table comes exclusively from `experiments/final-results/` in the evidence
ZIP, using build roots `sustained-base` and `final-tuned`. Earlier `release`,
`paired-release`, `sustained-tuned`, and `sustained-results` directories belong to
rejected candidate 06 or earlier screenings. They are not relabeled as final results.

There are **23 cases × 2 versions × 5 independent JVM forks = 230 timed JVMs**.
Order alternates baseline/modified between forks. Each process uses at least ten
warmup passes **and at least one second of warmup**, calibrates repetitions toward
50 ms/pass, and records seven measured passes. The table gives the median of five
per-fork medians, and the full min–max range of those five medians. No fork is
removed. Inputs and seeds are identical; each fork uses a fresh JVM. There is no
forced compilation, inlining or JIT threshold change.

Platform: supplied **Zulu JDK 26.0.2.1**, Linux x86-64 on an **Intel Xeon Platinum
8573C**, five visible virtual CPUs and a four-CPU cgroup quota. This is a shared VM,
not the AMD machine from prior reports and not the original AArch64 machine.
The environment and actual commands are included. Wall time is primary; per-thread
CPU time is recorded separately. The latter does not make CPU frequency, JIT layout,
or GC effects disappear. Results are exploratory microbenchmarks, not JMH confidence
intervals or full application performance.

### Units and coverage

Each vector contains **4,096 IDs**. `write` includes producer reset, staging, planning
and writing into a reused, already-sized native region. Separate measurement and
native allocation are outside that timer. `encode` includes the actual heap
allocation and planning. `heap-get`/`native-get` perform **4,096 pseudorandom scalar
lookups per operation**, including normal reader/header access, not one lookup.
`copy` decodes the complete 4,096 IDs, and `slice` decodes **257 IDs** from a moving
start into a nonzero output offset. The page case includes both sizing and writing
one complete CSF page. Fixtures are cache-hot/reused, not random accesses across a
multi-billion-ID store.

`typed13` means fixed-tag IDs whose payload was generated with 13 varying bits;
`delta31` means sorted unsigned values drawn from a 31-bit domain. The production
planner selects its normal encoding, so these labels are input distributions rather
than an override forcing a particular encoded mode. `mixed` cycles 26 fixtures over
widths 1, 3, 7, 13, 16, 24, 31, 40, 56, 57, 58, 63 and 64. Different calibrated iteration
counts can give slightly different weights to the first fixtures in the last partial
cycle; this also explains tiny mixed-encode average-allocation differences. Each
individual encoded array and byte length remains identical in differential tests.

| Case | Baseline ns/op | Modified ns/op | Baseline / modified | Baseline fork range | Modified fork range | Allocated B/op, before → after |
|---|---:|---:|---:|---:|---:|---:|
| copy-3 | 2,511.642 | 2,567.185 | 0.978× | 2,487.9–2,665.8 | 2,515.3–4,514.5 | 0.000 → 0.000 |
| copy-56 | 7,843.586 | 4,465.671 | 1.756× | 4,838.1–9,909.5 | 4,368.3–12,455.2 | 0.000 → 0.000 |
| copy-8 | 3,004.258 | 2,957.754 | 1.016× | 2,883.4–4,237.8 | 2,898.8–3,160.7 | 0.000 → 0.000 |
| copy-mixed | 3,665.385 | 3,693.637 | 0.992× | 3,541.9–7,528.1 | 3,529.3–7,225.2 | 0.000 → 0.000 |
| copy-typed13 | 3,471.159 | 3,551.618 | 0.977× | 3,400.5–3,584.4 | 3,487.4–3,690.1 | 0.000 → 0.000 |
| copy-typed3 | 3,836.845 | 3,833.294 | 1.001× | 3,763.4–6,559.1 | 3,696.3–4,211.8 | 0.000 → 0.000 |
| encode-13 | 42,262.954 | 7,447.602 | 5.675× | 30,235.0–56,804.9 | 4,923.4–10,481.8 | 7,792.000 → 7,792.000 |
| encode-mixed | 99,512.804 | 10,881.845 | 9.145× | 59,764.8–121,481.4 | 8,738.7–16,803.1 | 18,147.000 → 18,185.625 |
| heap-get-13 | 115,071.848 | 43,533.023 | 2.643× | 95,054.8–146,205.7 | 39,522.9–61,240.9 | 0.000 → 0.000 |
| heap-get-63 | 274,502.506 | 55,715.157 | 4.927× | 183,924.7–349,163.4 | 54,607.8–78,715.9 | 0.000 → 0.000 |
| heap-get-mixed | 135,765.533 | 60,998.834 | 2.226× | 110,421.8–181,297.9 | 46,131.6–98,459.4 | 0.000 → 0.000 |
| native-get-13 | 11,085.573 | 11,567.828 | 0.958× | 10,927.5–18,819.2 | 10,968.0–14,220.3 | 0.000 → 0.000 |
| page-page-core-write | 166,258.068 | 135,484.833 | 1.227× | 111,907.1–177,085.0 | 101,588.1–196,966.0 | 0.000 → 0.000 |
| slice-3 | 256.162 | 352.398 | 0.727× | 182.1–365.1 | 181.3–399.6 | 0.000 → 0.000 |
| slice-mixed | 369.954 | 417.728 | 0.886× | 303.8–429.7 | 307.0–481.3 | 0.000 → 0.000 |
| write-13 | 28,516.228 | 5,959.380 | 4.785× | 26,686.7–47,223.4 | 5,717.9–10,936.4 | 0.000 → 0.000 |
| write-3 | 28,070.468 | 6,265.052 | 4.480× | 17,894.8–32,410.5 | 5,776.6–12,620.3 | 0.000 → 0.000 |
| write-63 | 87,747.889 | 7,339.660 | 11.955× | 83,029.5–146,254.6 | 6,336.6–8,247.7 | 0.000 → 0.000 |
| write-64 | 83,754.721 | 3,610.507 | 23.197× | 76,552.5–117,865.7 | 3,351.3–6,172.9 | 0.000 → 0.000 |
| write-8 | 17,107.746 | 5,616.146 | 3.046× | 15,537.2–26,090.6 | 5,040.7–9,162.0 | 0.000 → 0.000 |
| write-delta31 | 57,187.427 | 7,612.473 | 7.512× | 36,367.2–75,691.5 | 7,290.9–13,231.0 | 0.000 → 0.000 |
| write-mixed | 64,415.815 | 7,768.839 | 8.292× | 51,310.9–92,269.6 | 6,955.3–11,113.4 | 0.000 → 0.000 |
| write-typed13 | 30,537.169 | 7,641.781 | 3.996× | 26,868.6–41,494.5 | 5,854.1–12,821.9 | 0.000 → 0.000 |

The native read cases are **controls**: their production algorithms were restored
byte-for-byte at the source-method level. They are included to expose instability
and any indirect compilation/profile effects, not to claim a new native read
algorithm. Similarly, `page-core-write` is reported even when the large vector-only
packing gain does not translate into a full-page improvement. The input producer,
page sizing, traits, fingerprinting and other encoding remain part of that operation.
A small favorable or unfavorable median on a control is not evidence of a source-level
speedup to that control.

## 1. Word-at-a-time emission

The previous encoder invoked a loop for every residual. That loop computed the
remaining bits in a byte, loaded the destination byte, ORed in its fragment and
stored the byte. Wide residuals repeated that read/modify/write several times. Both
heap and native writers used this path.

The replacement accumulates bits in one local `long`, emits each complete
little-endian word exactly once, and writes only the final partial bytes. It covers
all four existing modes: raw/typed frame-of-reference and raw/typed deltas. Selection,
width, base, tag, layout, block sizes and tie-breaking are unchanged. Delta input
preserves the first value in the existing header and accumulates subsequent exact
differences. Unsigned bit patterns and wraparound are preserved.

A boundary-aligned residual needs a special carry case: Java masks shift distances,
so shifting a `long` by 64 is not a way to obtain zero. The encoder explicitly clears
the carry when the word ends exactly at bit 64. It never overwrites the input array.
No extra staging array or allocation was added.

### Clearing and region ownership

Because every payload byte is now assigned rather than ORed with its previous
contents, `writeNative` no longer zeroes the whole vector first. Headers and all
directory entries are assigned; optional radix/prefix metadata, which may contain
holes, remains explicitly cleared. The complete page allocation clear is unchanged.

A packed block owns **no writable padding**. Complete-word writes are emitted only
when all eight bytes belong to the payload. Final bytes are written individually.
The existing eight-byte readable-tail contract is unchanged; no extra tail is
allocated. Tests deliberately use dirty native memory and leave nonzero canaries
immediately after the declared output.

The writer also rejects a negative producer size, computes the ceiling block count
without overflowing `int`, checks header/directory capacity before writing, and
checks each planned block before its payload store. A refused short output can
contain an already-written header/prefix, but the writer never crosses the declared
region in these tests. Refusal is not an atomic transaction or a publishable page.

## 2. Heap reads: bounded words rather than byte assembly

`LeBytes.getLong`/`putLong` use a static final little-endian byte-array-view VarHandle
in **plain** access mode. This permits unaligned offsets while retaining the Java
array's whole-access bounds checks. It is not an unchecked Unsafe array access.

The heap `readBits` path first checks that an eight-byte word exists. It loads and
shifts that word, reads the ninth spill byte when a wide unaligned field requires it,
and masks the result. If fewer than eight bytes remain, it retains the exact
byte-reading tail. Heap encodings have no readable padding, and the implementation
does not assume otherwise. This improves `ArrayReader` access without changing the
already-optimized native scalar bit reader.

For the standard single-width heap-encode case, allocation is unchanged. Native
write, scalar read, full copy and slice measurements allocate zero bytes in their
measured loops. The new static VarHandle and changed compiled methods have fixed
process/code-cache costs; unchanged per-record storage is not a claim of zero total
memory cost. No heap directory, decoded-length array, reverse-index entry, or
persistent LMDB bytes are added.

## 3. Rejected native-reader experiments

Seven source snapshots are included as independent cumulative patches against the
same baseline. Word packing survived throughout. Heap bounded-word reads and the
VarHandle survived; speculative native read rewrites did not.

An independent-load native loop gave C2 many address calculations and variable
shifts. A generic eight-at-a-time loop also created substantial temporary state.
Constant-width helpers for widths one through eight reduced those variable shifts
and helped isolated narrow cases. The final such candidate moved dispatch to the
already-decoded header and left wider helper bodies intact.

That was not sufficient. The longer-warmup mixed-slice case regressed materially,
and the additional dispatcher/helper code enlarged the compiled code. I did not
establish whether dispatch, code layout, or another factor caused the mixed-slice
result. The released candidate conservatively restores **all native decoding methods**,
not just the wide fallback. Constant-width Java is not automatically the best default
for a polymorphic-width CSF page workload. The generator is retained only for
reproducing the rejected experiment.

Both fast screening and sustained candidate-06 comparisons are in the evidence. A
same-binary control experiment also showed significant variability in some short
runs, prompting the longer minimum-duration warmup. The shipping table is a separate
complete set, not the best result selected from those earlier runs.

## 4. Actual C2 inspection

The evidence contains original emitted machine bytes, their checked extraction and
GNU `objdump` disassembly. Diagnostics run separately from measurements. The source
uses no forced inlining, Vector API dependency, handwritten machine code or altered
compilation threshold. Captures labeled `c2-final-*` correspond to the shipping code
and the sustained harness. Earlier native-specialization captures are rejected
experiments, not final native kernels.

The baseline native `writeBits` C2 code includes a byte load and byte store inside its
fragment loop. The replacement emits native word stores from its accumulator, with
byte stores only for the exact tail. This eliminates explicit destination-byte
loads; it does not imply that hardware stores require no cache ownership traffic.

The little-endian VarHandle path lowers to a bounds-checked 64-bit array load rather
than eight byte loads/shifts. The general heap reader still contains ninth-byte and
partial-tail fallbacks. Some compiled methods grow because of inlining and guards.
Static code size alone is not the performance acceptance criterion.

### c2-final-base

```text
control-asm
01 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::nativeGet main_code_bytes=368 instructions=89 calls=5

heap-asm
01 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::readBits main_code_bytes=424 instructions=104 calls=5
02 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector$ArrayReader::get main_code_bytes=1744 instructions=402 calls=20
03 org.eclipse.rdf4j.sail.lmdb.csf.LeBytes::getLong main_code_bytes=288 instructions=66 calls=4

write-asm
01 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::writeBits main_code_bytes=208 instructions=57 calls=2
02 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::writeBlockNative main_code_bytes=560 instructions=134 calls=6
03 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::writeBlockNative main_code_bytes=552 instructions=131 calls=6
04 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::writeNative main_code_bytes=3856 instructions=849 calls=28

```

### c2-final-tuned

```text
control-asm
01 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::nativeGet main_code_bytes=368 instructions=89 calls=5

heap-asm
01 org.eclipse.rdf4j.sail.lmdb.csf.LeBytes::getLong main_code_bytes=184 instructions=44 calls=4
02 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::readBits main_code_bytes=760 instructions=176 calls=9
03 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector$ArrayReader::get main_code_bytes=2048 instructions=465 calls=24

write-asm
01 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::packBlock main_code_bytes=1008 instructions=248 calls=6
02 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::writeBlockNative main_code_bytes=872 instructions=218 calls=8
03 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::packBlock main_code_bytes=720 instructions=185 calls=5
04 org.eclipse.rdf4j.sail.lmdb.csf.PackedLongVector::writeNative main_code_bytes=4128 instructions=925 calls=28

```

## 5. Correctness and integration limits

Six component suites pass **26,631,072 counted checks** on the freshly patched source:

| Suite | Counted checks |
|---|---:|
| Packed I/O: added checks | 18,912,127 |
| Inherited vector/page comparisons used by Packed I/O | 3,341,973 |
| UTF-8 regression | 343,391 |
| Binding domain regression | 1,306,622 |
| Existing generated-key regression | 43,337 |
| Optimized generated-key regression | 932,360 |
| Native stable-sort regression | 1,751,262 |

The packed suite covers **8,098 vector configurations**: 3,916 inherited plus 4,182
explicit width/size/type/hint combinations, and **544 page views**. Multiple reads,
slices and encodings are checked per configuration. Raw widths 0–64, typed widths
0–57, unsigned boundaries, delta candidates, nonzero source/output offsets, dirty
unaligned memory, exact heap limits and tail bytes are exercised. The reference
vector codec, encoder and little-endian helper are full baseline production files
with names changed for side-by-side compilation. Encoding bytes, byte lengths,
fingerprints, flags and decoded values must agree. Independent expected-byte tests
also cover the shared native primitives and little-endian constants.

The complete production CSF package compiles against explicitly labeled external
CoreDatatype/logger/annotation doubles. No double implements compression, native
I/O or CSF layout. Other retained suites compile actual components and extracted
production units with their documented model/engine/factory doubles. Source manifests
record the distinctions. The historical full CSF test driver has an assertion about
redundant trait lookups in an older baseline; the new driver invokes its vector/page
comparison functions without applying that obsolete expectation to the already-fixed
immediate baseline.

The complete RDF4J dependency graph, real-model query integration, generated Janino
execution, original benchmark dataset/query and AArch64 execution remain unverified.
The tests do not establish an end-to-end speedup, a complete security audit of all
native memory code, or performance on a multi-billion-ID working set. The previously
implemented generated-only DISTINCT/GROUP BY path is unchanged, and its component
tests still enforce zero dictionary lookups/reads in admitted generated-key cases.

## 6. Apply and reproduce

The primary patch is against `lmdb-general-performance-source.zip`, from its root
containing `java/`. The production-only patch contains the same two Java changes;
the full patch additionally includes tools, tests and this report. The cumulative
patch is against the original judges' non-resource-fork source. Apply one, not several.

```sh
git apply --check lmdb-packed-performance.patch
git apply lmdb-packed-performance.patch
```

See `tools/lmdb-packed/README.md` for commands, input units and compiler flags.
The release verification manifest confirms that each patch applies to its stated
baseline, full and cumulative output equal the source ZIP byte-for-byte, no original
source files were dropped, and the freshly patched tree compiles and passes all six
component suites. SHA-256 checksums cover the deliverables.

## Primary references and independent implementation

1. OpenJDK/Oracle JDK 26 `MethodHandles.byteArrayViewVarHandle`: plain access modes,
   endian selection and array bounds. https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/lang/invoke/MethodHandles.html#byteArrayViewVarHandle(java.lang.Class,java.nio.ByteOrder)
2. JavaFastPFOR `BitPacking.java`: fixed-width dispatch and unrolled bitpacking as a
   research comparison for the rejected narrow-reader experiment.
   https://raw.githubusercontent.com/fast-pack/JavaFastPFOR/master/src/main/java/me/lemire/integercompression/BitPacking.java
3. DuckDB's bitpacking helper: word-oriented packing/unpacking as another primary
   engineering comparison. https://raw.githubusercontent.com/duckdb/duckdb/main/src/include/duckdb/common/bitpacking.hpp

The accumulator, bounded heap reader and experimental generator were independently
written around this module's unsigned 64-bit, typed/delta, and memory-ownership
contracts. No third-party code block or new dependency was imported. Source access
was on 2026-09-13. Project facts and measured results above come from the supplied
code and delivered evidence, not these external references.

## Rejected candidate example

| Case | Baseline ns/op | Modified ns/op | Baseline / modified | Baseline fork range | Modified fork range | Allocated B/op, before → after |
|---|---:|---:|---:|---:|---:|---:|
| slice-mixed | 311.680 | 427.515 | 0.729× | 308.4–321.5 | 298.2–476.1 | 0.000 → 0.000 |

This row is **candidate 06**, not the released candidate 07.
