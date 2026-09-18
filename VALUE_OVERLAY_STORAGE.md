# Value overlay: storage-focused revision

## Baseline, scope, and delivery

This revision is compared with the **actual `value-overlay-round2-source.zip`**, not the previous reply's description. That ZIP still used checkpointed packed lengths and scanned token commands for decoded length. It did **not** contain the claimed direct-offset or decoded-length sidecars. This revision adds neither.

Production changes relative to that ZIP are limited to:

* `PackedVector.java`: implicit dense ordinals with packed tag residuals; exact integer-slope residuals for irregular sorted IDs; verified periodic route models; removal of unused model tails.
* `PageCodec.java`: frame-of-reference encoded lengths, constant-length pages without a length index, and word-at-a-time packed prefix sums.
* `CompressedValueOverlay.java`: use the route-specific encoder when sealing route blocks.

All original source files are included. Prior-round native directory and token-trainer improvements remain included, unchanged by this revision. `VALUE_OVERLAY_ROUND2.md` is preserved as historical input, not as evidence of this revision's implementation. New tools live under `tools/value-overlay/`.

The incremental patch applies to the extracted round-2 source root. The cumulative patch applies to the original judges' source root. **Use one, not both.** Both paths produce the same delivered source files. The verification manifest records hashes and fresh-apply comparisons. Apple `__MACOSX/` resource-fork sidecars in the original upload are not source and are omitted from the source ZIP; applying the cumulative patch leaves those unrelated files untouched.

## Measured storage

All values below are exact encoded/storage-accounting byte counts, not estimates from compression ratios. Workloads are deterministic and synthetic. Full-overlay cases contain 262,144 records, with 256-entry ID/routing blocks, 64 KiB maximum pages, 32 active page builders, token compression enabled and numeric-vector encoding disabled. The optional reverse index is disabled (`reverseSlots=0`) to isolate these changes; it is not removed or changed by this revision. Three-affinity cases use regular round-robin affinity selection, not arbitrary real-world routing.

### Full overlay, native bytes used

| Corpus / ID layout / clustering | Round 2 | New | Bytes saved per record | Reduction |
|---|---:|---:|---:|---:|
| Text / tagged / one affinity | 8,086,217 | 7,654,217 | 1.648 | 5.34% |
| Text / random gaps / one affinity | 8,103,241 | 7,915,049 | 0.718 | 2.32% |
| Text / tagged / three affinities | 8,408,104 | 7,664,648 | 2.836 | 8.84% |
| Shared prefix / tagged / one affinity | 1,912,735 | 1,364,255 | 2.092 | 28.68% |
| Fixed 80-byte records / tagged / three affinities | 22,454,376 | 21,438,528 | 3.875 | 4.52% |
| Lengths 80–87 / tagged / three affinities | 23,372,096 | 22,528,312 | 3.219 | 3.61% |

**Used native bytes are not reserved slab bytes or process RSS.** The text/three-affinity case crosses a slab boundary and reserved bytes fall from 12,517,376 to 8,323,072. In several other cases reserved bytes are unchanged even though used bytes fall. The allocator's minimum slab sizes remain unchanged. No new retained per-record heap directory/cache is added; the budget's accounted retained heap is unchanged or lower in these tests. That accounting does not measure compiled code size, transient construction allocation, or full process RSS.

### ID vectors, 1,000,000 entries, including headers and required tails

| Distribution | Round 2 bytes | New bytes |
|---|---:|---:|
| Dense high bits with varying low tags | 2,000,008 | 500,024 |
| Irregular positive gaps | 2,061,800 | 1,493,840 |
| Affine | 125,024 | 93,768 |
| Irregular gaps across signed boundary | 2,061,800 | 1,493,840 |
| Full unsigned-range distribution | 6,718,704 | 6,221,952 |
| Dense cluster plus extreme final outlier | 2,000,416 | 375,520 |

The dense tagged case is approximately 75% smaller, and the positive-gap case approximately 27.5% smaller. For the particular dense tagged distribution, a full block falls from 512 to 128 bytes. The periodic three-cursor route block falls from 352 to 40 bytes; affine route blocks fall from 32 to 24 bytes.

A hypothetical 500-million-entry collection with the same dense tagged structure would save approximately 750 million bytes in its ID vectors alone. That is a conditional linear extrapolation, not a measurement on a 500-million-entry real dataset.

### Page length metadata, 64 pages of 256 records

“Length index” counts checkpoints, packed lengths/residuals, the safe tail and following alignment; it excludes the shared page header and token/prefix dictionaries.

| Corpus | Round 2 length index | New length index | Round 2 complete pages | New complete pages |
|---|---:|---:|---:|---:|
| Token-compressed text | 16,000 | 14,176 | 455,124 | 453,300 |
| Shared-prefix strings | 9,216 | 0 | 68,224 | 59,008 |
| Binary records | 20,992 | 18,944 | 1,497,962 | 1,495,914 |
| Fixed 80-byte records | 18,944 | 0 | 1,331,712 | 1,312,768 |
| Lengths 80–87 | 18,944 | 10,752 | 1,388,784 | 1,380,592 |

Zero length-index bytes does not mean zero total page metadata. Constant pages still have their ordinary page header and any dictionaries/LOB bitmap required by the chosen codec.

## Design and exactness

### 1. Store low-bit residuals, not predictable ID ordinals

The existing dense-high-bits verifier establishes, for every entry in a block:

```
(value[i] >>> shift) == (base >>> shift) + i
```

The previous implementation used this as a lookup hint but still packed the complete `value-base`. The new representation reconstructs the high bits from the entry position and packs only varying low bits, relative to the first ID. Invariant trailing bits are omitted too. It does not assume a fixed RDF4J tag width or hard-code a particular tag set.

`get`, exact membership and unsigned `lowerBound` understand this format. Membership still compares the complete reconstructed 64-bit ID. A different low tag is a miss, never an alias for the same ordinal. A failed pattern check falls back to another exact representation.

### 2. Integer-slope residuals for irregular sorted blocks

For non-dense blocks, subtract a verified integer trend before frame-of-reference packing. Conceptually:

```
value[i] = adjustedBase + i * slope + residual[i]  (mod 2^64)
```

The builder checks sorted order, signed-error representability relative to unsigned values, and residual width. It selects this representation only when narrower than ordinary FOR. Reader comparisons use reconstructed unsigned IDs, not residual order. There is no floating-point approximation, lossy normalization, sampled acceptance, or learned membership decision.

This is related to Lucene's blockwise monotonic prediction/residual design, but uses different exact integer arithmetic and unsigned validation appropriate to these IDs. Lucene's implementation uses a floating average increment; it was research inspiration, not copied source.

### 3. Constant/FOR encoded lengths

The page header already contains a word that stores a checkpoint count derivable from the record count. A new flag, `FLAG_LENGTH_FOR = 0x10`, reuses that word for the minimum encoded length. Residuals store `encodedLength - minimum`.

If the residual width is zero, the page has no checkpoint array, no packed length array and no length-array safe tail. A record's encoded start is `slot * minimum`. If residual widths are smaller but nonzero, existing checkpoints remain every 16 records. If the width does not improve, the legacy layout is retained.

The improvement is about encoded lengths; **decoded token lengths are still computed on demand**. No decoded-length array is introduced. The page chooser still uses its existing codec/minimum-saving policy, so although the metadata representation never grows for a fixed chosen encoding, this is not a proof that every possible corpus's complete page choice is monotonically smaller.

### 4. Fast prefix sums instead of an offset sidecar

Locating a variable-length record still needs the sum of at most 15 preceding lengths. The new code reads packed residuals as words, rather than repeatedly invoking a per-value native accessor:

* Widths 1–3 use bit masks and population counts.
* Width 4 uses nibble grouping; width 8 uses byte grouping.
* Widths 5–7 use one or two packed words, masks, shifts and additions.
* Widths 9–31 load several fields at once and sum them in registers.

All paths add the page's length base arithmetically. This preserves compact checkpoints without the formerly proposed per-record offset array. The implementation retains bounded FFM access and arena lifetime checks.

### 5. Verified periodic routing

Page-family clustering can create a few interleaved advancing page cursors. The route encoder tries periods 2 through 16 and accepts a model only when every locator satisfies:

```
route[i] = seed[i % period] + (i / period) * cycleStep
```

It stores the seeds and common step, using the first seed and step in the existing header. It admits the format only when the complete encoded vector is smaller than the fallback. The method is separate from ID encoding, so ID membership is not forced through variable-period arithmetic. Irregular routing uses the ordinary encoder. A three-way periodic access costs a small additional integer-division/read penalty, measured below.

### 6. Private in-memory format; unchanged persistent values

The packed-vector header remains 24 bytes. Encoding tags are: `-1` affine; ordinary `0..64` FOR/legacy hints; `-2-width` dense residual; `128+width` integer-slope residual; `256+period` periodic routes. Eight-byte read tails remain on bitpacked payloads that can overread their final field. Pure models/constant vectors do not make those reads and omit their old unused tail.

The overlay is rebuilt in memory. Persistent LMDB value records, dictionary IDs, lexical byte strings, graph/query semantics and public Java method signatures are not changed. New readers are tested against pages produced by the round-2 encoder. Old readers do not understand the new private format; do not attempt to reuse new native buffers inside an old process. There is no migration of persisted LMDB data.

## Read-time tradeoffs

JDK 26.0.2.1, x86-64 Intel Xeon Platinum 8272CL virtual machine, Serial GC. Each case ran in three independent JVMs. A case uses six warmup rounds and five measured rounds, at least 75 ms per round. The table is the median of the three per-fork medians; raw min/max and allocation measurements are preserved. Variant order alternated by fork. Each scenario is isolated to avoid cross-codec profile pollution.

| Operation | Round 2 ns/op | New ns/op | Interpretation |
|---|---:|---:|---|
| Tagged-ID rank | 46.894 | 45.806 | Approximately level |
| Tagged-ID record visit | 274.253 | 272.139 | Approximately level |
| Irregular-ID rank | 146.353 | 153.573 | 4.9% slower |
| Irregular-ID record visit | 356.130 | 386.893 | 8.6% slower |
| Three-way periodic route lookup | 20.076 | 22.907 | +2.831 ns, 14.1% slower |
| Token text decoded length | 117.720 | 113.654 | 3.5% lower median |
| Token text copy | 289.608 | 294.005 | Approximately level; +1.5% |
| Shared-prefix decoded length | 56.683 | 29.670 | 47.7% lower median |
| Shared-prefix copy | 80.988 | 49.673 | 38.7% lower median |
| Near-fixed decoded length | 58.558 | 41.832 | 28.6% lower median |
| Near-fixed copy | 91.133 | 73.469 | 19.4% lower median |

These are short dependency-free kernel experiments in a shared virtual machine, not JMH confidence intervals. In particular, the irregular-ID forks vary noticeably; do not treat small percentage differences as statistically established. Rank/visit tests contain one million IDs. Copy tests produce the complete original byte record, not only its first byte. The full-overlay audit measures retained storage, not builder throughput; extra model-verification passes add construction work that is not benchmarked here.

The original query/dataset/application has not been reproduced. This is not an end-to-end query speed claim, and the original JFR's AArch64 machine differs from this x86-64 environment. `ValueStore.findId()` reverse lookups and token decoded-length scans remain separate possible bottlenecks.

## Final C2 inspection

New logs were captured from this final implementation on JDK 26, without `dontinline` directives. `CompileCommand=print` emitted actual C2 machine bytes; the supplied `disassemble.py` verifies every byte of each main-code range and decodes those bytes with `objdump` because `hsdis` was absent. It does not invent assembly from Java source.

The final near-fixed `encodedRange` contains three `POPCNT` instructions for the three-bit residual path. The final five-to-seven-bit `mediumPrefixSum` uses one packed word load and a conditional second packed word load, masks and shifts; its decoded main code contains no `idiv`/`div`. FFM bounds and session guards remain. Tagged-ID C2 includes the final dense-residual reconstruction path. Larger overall compiled method size is not confused with a cycle estimate or claimed as a storage reduction.

Evidence includes baseline/final near-fixed logs, final token-text logs, final tagged-ID logs, their decoded main-code bytes and assembly, plus runtime details. Diagnostic capture timings are not part of the performance table.

## Correctness validation

The final two suites passed **8,999,334** and **2,477,907** counted checks, respectively. Coverage includes:

* Exact ID round trips, arbitrary packed widths 0–64, wide-field spill, dense shifts 1–62, low-tag mismatches, gaps, duplicates, unsigned crossover, outliers, integer-slope overflow guards and exact lower bounds.
* Periods 2–16, cycle perturbation/fallback, distinct payloads, random affinities and short/full pages. Full routed overlay reads check actual record bytes, not only locator arithmetic.
* All prefix widths 1–31 and prefix counts 0–15, saturated fields and unaligned native addresses.
* Constant/FOR pages through 32,768 records, empty values, LOB bitmaps and inconsistent constant-length metadata rejection.
* All RAW, FRONT, TOKEN, VECTOR and PREFIX modes, with 384 corpus pages and full/selective reads of both new pages and pages produced by the previous encoder.
* Existing directory bounds, leases, four-reader concurrency, retirement, admission refusal, cancellation, skipped records, reverse-index UNKNOWN semantics, delta shadowing, compaction and historical leases.

Native encoded bytes intentionally differ. The old page-byte-identity comparison was replaced by stronger relevant compatibility checks on decoded bytes, lengths, partial reads, backward byte access and LOB flags. The deterministic final encoded corpus hash is `dac1c08402330436d87582a72fd524d2fcaa7518513b1c2212935c5c23f6682d`.

The standalone overlay package and tools compile. This is not a claim that the full external RDF4J Maven reactor or original SPARQL benchmark ran.

## Reproduction

Use a JDK with final FFM (22+; JDK 26 is the tested version). The script needs Python 3.9+, no network and no Maven dependencies. Run it from any directory. `--out` must be outside the source and baseline trees.

```sh
export JAVA_HOME=/path/to/jdk-26
python3 tools/value-overlay/run_storage.py \
  --baseline /path/to/extracted-round2-source \
  --out /tmp/overlay-storage-check
```

This compiles the baseline independently, compiles the submitted package and tools, runs both test suites with old-page compatibility, and audits both versions. No baseline means tests/audit run for the new version only, without old-page differential checks.

```sh
# Three JVM forks per scenario, matching the recorded experiment structure.
python3 tools/value-overlay/run_storage.py \
  --baseline /path/to/extracted-round2-source \
  --out /tmp/overlay-storage-bench --bench --forks 3

# Optional emitted C2 byte capture; automatic decoding requires x86-64 and objdump.
python3 tools/value-overlay/run_storage.py \
  --baseline /path/to/extracted-round2-source \
  --out /tmp/overlay-storage-c2 --c2

# Faster benchmark subset while retaining the default regression/audit checks.
python3 tools/value-overlay/run_storage.py \
  --baseline /path/to/extracted-round2-source \
  --out /tmp/overlay-route-check --bench --forks 1 --scenarios routes
```

The source ZIP includes the complete baseline sources plus the changes, tests, audit, benchmark and C2 tools. The separate evidence ZIP includes the recorded final results; it does not include JDK binaries, compiled Java classes, fonts, or the large original JFR.

## Research consulted

* Apache Lucene `DirectMonotonicWriter`: blockwise trend removal followed by packed residuals. https://raw.githubusercontent.com/apache/lucene/main/lucene/core/src/java/org/apache/lucene/util/packed/DirectMonotonicWriter.java
* DuckDB, “Lightweight Compression in DuckDB”: constant encoding, frame-of-reference compression and block-local codec selection. https://duckdb.org/2022/10/28/lightweight-compression

These are algorithm/design references. The new implementation is specific to the supplied overlay and its exact unsigned-ID and lexical-byte semantics.
