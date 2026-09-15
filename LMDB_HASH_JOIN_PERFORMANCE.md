# Native and IR hash-join performance pass

Baseline: `lmdb-packed-performance-source.zip`. Date: 2026-09-13. Runtime: supplied Zulu JDK 26.0.2.1, x86-64, Intel Xeon Platinum 8573C in a shared VM. Five production files are changed; previous source, formats, generated-key work, and fallback strategies are retained.

## Status and scope

This is an implemented and component-tested optimization of the existing hash-join infrastructure, not a new query-plan strategy. It affects native scalar/batched probes, the IR `LongRowMap`, duplicate build handling, and existing borrowed factor consumers. No persistent encoding, value-overlay metadata, term identity, dictionary lookup protocol, or configuration flag is added. The full RDF4J/Janino dependency build and original end-to-end benchmark have not been reproduced. All external API doubles and extracted production units used in the offline build are labeled.

## Final performance matrix

The table contains every final case, including neutral and slower results. Times are nanoseconds per build/probe input row. Chain cases additionally consume the matching payload chain. Each value is the median of five independent JVM medians; ranges show the slowest and fastest fork, not confidence bounds. Allocation is current-thread allocated bytes per row and is not retained memory.

| Case | Baseline ns/row | Modified ns/row | Baseline / modified | Baseline fork range | Modified fork range | Allocated B/row, baseline → modified |
|---|---:|---:|---:|---:|---:|---:|
| `build-unique` | 97.891 | 86.073 | 1.137× | 94.253–100.036 | 82.811–89.190 | 128.006 → 124.003 |
| `ir-build-unique` | 84.063 | 83.244 | 1.010× | 82.907–87.186 | 79.842–84.332 | 112.006 → 108.002 |
| `build-composite` | 118.005 | 116.233 | 1.015× | 116.699–132.151 | 114.195–187.393 | 223.995 → 219.989 |
| `ir-build-composite` | 110.498 | 103.356 | 1.069× | 104.012–157.358 | 102.423–106.890 | 207.994 → 203.988 |
| `build-duplicates` | 37.067 | 32.396 | 1.144× | 34.322–59.753 | 31.038–34.899 | 30.503 → 30.251 |
| `ir-build-duplicates` | 32.580 | 30.355 | 1.073× | 31.907–33.395 | 29.765–32.629 | 29.503 → 29.250 |
| `build-count-only` | 34.283 | 26.055 | 1.316× | 31.609–80.745 | 24.949–27.526 | 30.503 → 14.251 |
| `ir-build-count-only` | 29.086 | 23.925 | 1.216× | 28.646–32.110 | 22.543–26.834 | 29.503 → 13.251 |
| `probe-hit` | 26.596 | 14.036 | 1.895× | 26.297–26.886 | 13.866–14.240 | 0.000 → 0.000 |
| `probe-miss` | 25.547 | 17.661 | 1.446× | 24.718–25.661 | 16.671–20.489 | 0.000 → 0.000 |
| `probe-mixed` | 27.818 | 21.568 | 1.290× | 27.556–28.458 | 21.093–21.702 | 0.000 → 0.000 |
| `ir-probe-hit` | 23.696 | 14.492 | 1.635× | 23.517–24.288 | 14.108–15.346 | 0.000 → 0.000 |
| `ir-probe-miss` | 21.359 | 19.466 | 1.097× | 21.027–47.519 | 19.010–19.819 | 0.000 → 0.000 |
| `ir-probe-mixed` | 30.532 | 20.895 | 1.461× | 29.545–31.153 | 20.267–21.503 | 0.000 → 0.000 |
| `batch-mixed` | 25.164 | 19.510 | 1.290× | 23.778–25.987 | 19.140–19.818 | 0.000 → 0.000 |
| `batch-composite` | 27.543 | 27.868 | 0.988× | 26.586–27.833 | 27.425–29.846 | 0.000 → 0.000 |
| `batch-scattered` | 30.389 | 30.350 | 1.001× | 29.455–30.789 | 29.801–33.245 | 0.000 → 0.000 |
| `batch-count` | 18.712 | 16.905 | 1.107× | 18.466–18.891 | 16.044–17.126 | 0.000 → 0.000 |
| `hash-single` | 1.553 | 1.554 | 0.999× | 1.507–1.565 | 1.512–1.639 | 0.000 → 0.000 |
| `hash-composite` | 4.035 | 4.022 | 1.003× | 3.982–4.167 | 3.927–4.260 | 0.000 → 0.000 |
| `hash-scattered` | 4.679 | 4.834 | 0.968× | 4.663–4.856 | 4.680–4.941 | 0.000 → 0.000 |
| `chain-native` | 68.808 | 55.615 | 1.237× | 63.903–71.120 | 53.966–63.673 | 0.000 → 0.000 |
| `chain-ir` | 66.330 | 57.567 | 1.152× | 65.449–72.370 | 56.016–57.909 | 0.000 → 0.000 |
| `probe-high-load` | 39.312 | 27.729 | 1.418× | 38.033–41.627 | 26.711–28.094 | 0.000 → 0.000 |
| `ir-probe-high-load` | 38.026 | 25.996 | 1.463× | 36.329–38.669 | 25.649–26.701 | 0.000 → 0.000 |
| `batch-high-load` | 35.103 | 26.907 | 1.305× | 31.542–80.357 | 25.246–29.767 | 0.000 → 0.000 |
| `probe-small` | 25.369 | 19.176 | 1.323× | 23.965–26.286 | 18.284–21.081 | 0.000 → 0.000 |
| `batch-small` | 17.823 | 12.620 | 1.412× | 17.648–18.537 | 12.490–13.233 | 0.000 → 0.000 |
| `probe-composite` | 40.175 | 36.942 | 1.088× | 39.332–42.636 | 35.534–37.488 | 0.000 → 0.000 |
| `ir-probe-composite` | 41.614 | 35.483 | 1.173× | 39.712–42.381 | 34.595–36.943 | 0.000 → 0.000 |
| `shape-cycle` | 28.674 | 29.792 | 0.962× | 28.332–30.361 | 27.875–33.189 | 0.000 → 0.000 |
| `threshold-duplicates` | 18.064 | 17.752 | 1.018× | 17.492–18.729 | 16.374–20.601 | 24.022 → 8.008 |

### Methodology and boundaries

The final matrix is 32 cases × 2 versions × 5 forks = 320 timed JVM executions. Versions alternate order within each case. Each JVM warms for at least ten operations and one second, calibrates repetition counts toward 50 ms per measured pass, and measures seven passes. No CPU affinity, diagnostic assembly logging, background compilation jobs, or concurrent regression suites were deliberately run alongside the final matrix. The shared host can still introduce substantial interference. Every command, per-pass sample, wall-time median, current-thread CPU median, and allocation result is retained in `release-bench/`. This is a dependency-free diagnostic harness, not JMH or a statistical confidence-interval study.

Build cases allocate a fresh production table and grow it as rows arrive. Input arrays are prepared outside the timer; primitive array allocation, insertion, duplicate chains, and rehashing are timed. Unique one-column builds contain 131,072 rows/groups; composite builds contain 65,536 rows/groups with three key and three payload columns. Duplicate/count-only builds have 131,072 rows and 8,192 groups. The threshold fixture has 24 groups and 131,072 zero-column-payload rows.

Probe cases reuse built tables and a 16,384-lane selection. The high-load cases use 196,608 keys and 262,144 buckets (75% occupied); small cases use 128 keys. The mixed cases have 50% misses; hit-only and miss-only controls are separate. Batch timing includes the existing hashBatch, initial entry/head gathering, and actual probing. Scattered selections exercise noncontiguous physical rows. The shape-cycle fixture alternates one-column and three-column/scattered tables in the same process and call path. Hash-only controls use exactly the original hash implementation. Duplicate-chain cases include walking and reading the payloads, not only locating a bucket.

The timed inputs are primitive long IDs. Query parsing, optimizer work, reading LMDB, reading the value overlay, generating data, expression evaluation, and result Value materialization are not timed. Tables and selected probes are reused and cache-hot; this is not a cold-RAM multi-billion-record workload. Performance on AArch64 remains unmeasured. No claim about whole-query speedup or hardware-counter causes follows from these results.

The first five-fork matrix (`final-bench/`) measured historical candidate 09. It exposed room to simplify scalar one-column probing, motivating candidate 10. The report above uses only the separate complete candidate-10 matrix (`release-bench/`). All first-matrix results and earlier two-fork screens are retained; no slow final forks were discarded.

## Layout and probe changes

### Packed full-hash/head word with compact negative filtering

Previously each bucket used a byte occupancy array, a byte fingerprint array, an int full-hash array and an int head array: ten data bytes per bucket across four arrays. The replacement uses a long entry containing the full 32-bit hash in its low half and the head payload ordinal plus one in its high half, plus one control byte. Zero entry/control means empty. A present control contains a presence bit and seven high hash bits. The new pair costs nine data bytes per bucket. Tail and duplicate-count arrays remain separate; full 64-bit key tuples and payload columns remain uncompressed.

The key is never the hash. A control match is followed by a full-hash comparison and exact equality of every key column. Integer head ordinals were already int-indexed; packing them does not narrow RDF dictionary IDs. The all-zero hash and key are valid. Decoding an empty packed head yields -1; prepared head probes can exploit that fact without returning an empty bucket as a successful bucket/count lookup.

Packing without a byte control was a useful but rejected intermediate design. Miss-heavy scalar probes benefited from retaining compact negative filtering rather than fetching a word for every occupied bucket. Scalar probes now check the control byte first. Batch probes use an already-staged full entry for the first bucket, then use the packed table when following collisions. This deliberately keeps different mechanics for different access patterns without introducing competing join strategies.

Single-column scalar lookup loads its input value once, computes the existing hash once, and compares `keys[bucket]` directly. Composite lookups retain their tuple loop. The native low-32-bit hash/hook and IR folded-32-bit hash remain unchanged. This dispatch is on the table schema, not on a benchmark-specific predicate, datatype or dataset. Multi-column/mixed-shape results remain in the final matrix.

### Reuse dead batch scratch instead of allocating more staging

After hashBatch has filled int hashes, its long intermediate-hash scratch is dead. The native cursor reuses those long slots for the first candidate entry and passes the staged word to head, count and bucket consumers. The previous separate int candidate-head array is removed. No new per-batch allocation or wider retained scratch is introduced. This is software staging through ordinary indexed loads, not an explicit hardware prefetch instruction.

At the default 16,384-lane capacity, auxiliary hash/probe scratch falls from 327,680 to 262,144 data bytes: 65,536 bytes (20%) less per cursor. Input/output NativeBatch columns and other cursor state are separate. Tests execute exact extracted refill/probe methods against real batches across small refill boundaries, sparse selections, empty/end-of-stream cases and mixed consumer calls.

## Build and memory changes

A duplicate payload row does not consume a new bucket. Both native and IR insertion now test whether the key is new before growing the index at the 75% threshold. A table with 24 distinct keys in 32 buckets therefore does not double merely because another duplicate arrives. True insertions still grow before occupancy exceeds 75%, preserving terminating linear probes.

Zero-column payloads no longer allocate and repeatedly expand an unused long payload arena. Row links remain, so duplicate multiplicity and existing chain iteration still work. Each appended row writes its own -1 next sentinel; unused next capacity and tails no longer need blanket -1 initialization. Allocation failures during growth do not publish a half-replaced set of backing arrays. Rehashing consumes stored full hashes instead of recalculating keys.

### Deterministic table-array accounting

| Fixture | Table | Baseline data bytes | Modified data bytes | Reduction |
|---|---|---:|---:|---:|
| `build-unique` | Native | 8,388,608 | 8,126,464 | 3.12% |
| `build-unique` | IR | 7,340,032 | 7,077,888 | 3.57% |
| `build-composite` | Native | 7,340,032 | 7,208,960 | 1.79% |
| `build-composite` | IR | 6,815,744 | 6,684,672 | 1.92% |
| `build-count-only` | Native | 1,998,848 | 933,888 | 53.28% |
| `build-count-only` | IR | 1,933,312 | 868,352 | 55.08% |
| `threshold-duplicates` | Native | 1,574,528 | 525,088 | 66.65% |
| `threshold-duplicates` | IR | 1,574,272 | 524,960 | 66.65% |

These are exact primitive backing-array data counts checked independently by reflection in the regression suite. They exclude object and array headers, source inputs, probe scratch, abandoned growth arrays awaiting GC, JVM code, and other query state. They are neither RSS nor a heap-dump retained-size measurement.

The native build estimator now rounds both bucket capacity and payload-row capacity to their actual powers of two, accounts for zero payload width correctly, and returns Long.MAX_VALUE for impossible array dimensions. Negative dimensions are rejected. This fixes previous underestimation between payload growth boundaries and unnecessary bucket rounding at exact load thresholds. The estimate still covers data arrays only, not every object header or cursor scratch byte. Existing reserve/admission/fallback behavior is retained; reservations are not automatically shrunk to the final number of distinct keys. Therefore a smaller physical table does not imply that every admission reservation releases that entire difference.

## Factorization, concurrency and semantics

PrimitiveHashJoinFactorSource and HashJoinFactorCursor now use capacity/occupied/head accessors instead of removed raw arrays. Their correlated payload tuples, duplicate counts, insertion order, repeatable copy/rewind behavior and lifetimes remain. No early flattening, extra payload copy, new join strategy, new lookup into the value store, or new interpretation of unbound values was added. Existing upstream admission continues to determine whether primitive join semantics apply.

Builds remain single-writer. A completed table can be read concurrently after safe publication by its existing owner; the seal flag protects borrowed views from later add calls but is not a new Java memory-publication primitive. Prepared words are valid only while the table is unchanged, as guaranteed by build-then-probe use. The test suite uses properly published completed tables and eight reader tasks. It does not claim concurrent table insertion or a formal memory-model proof.

## C2 observations

The final C2 artifacts are in `c2-release/`; baseline artifacts are in `c2-base/`. The first candidate is separately retained in `c2-tuned/`, and the fused hash experiment in `c2-fused/`. Method-print commands are diagnostics only: no forced inlining, compilation threshold override, -Xcomp, Vector API or Unsafe access was added. The inherited disassembler validates every emitted main-code byte and fails on incomplete ranges before using objdump. All captured recompilations are retained.

Last captured normal C2 compilation per method/case (not OSR). Static decoded instruction counts include cold paths; these are not executed-instruction counts or total code-cache sizes.

| Compilation | Baseline main bytes / decoded instructions | Final main bytes / decoded instructions |
|---|---:|---:|
| Native lookup, one-key mixed probes | 1,800 / 396 | 976 / 224 |
| IR lookup, one-key mixed probes | 1,056 / 249 | 744 / 175 |
| Batch diagnostic caller with inlined prepared lookup | 4,392 / 954 | 3,376 / 745 |
| Native growBuckets | 5,824 / 1,308 | 3,792 / 870 |
| Native add | 3,168 / 730 | 3,632 / 836 |
| Unchanged three-column hashBatch | 2,368 / 506 | 2,368 / 506 |

The one-column helper is inlined in the captured lookup callers; independently compiled native/IR helper bodies also exist (544/640 main-code bytes). Counting a smaller wrapper alone would be misleading. The scalar helper listing shows a control-byte test before the combined word load, a full hash comparison, a full 64-bit key comparison and upper-word head extraction. Uncommon traps preserve exact behavior on collisions not frequent in that profiling run. Regression tests separately force full-hash collisions.

All bytes of the following excerpt come from final `probe-mixed-asm/01-PrimitiveHashJoinTable-lookupScalarSingle.asm`; intervening branch/bounds/key-check instructions are omitted here, not removed from the compiled method:

```asm
mov    rax,QWORD PTR [rbp+r9*8+0x10]   # combined hash/head entry
mov    r11d,eax
cmp    r11d,ecx                       # compare the full low-word hash
# ... bounds check and full 64-bit key comparison ...
shr    rax,0x20                       # extract the head only after equality
mov    eax,eax
dec    eax                            # reverse the head+1 encoding
```

The scalar one-column path removes the counted input-hash and key-arity loops from the probe. It keeps control/entry/key bounds checks and hash collision verification. The emitted entry load provides both the full-hash comparison operand and the head bits. Batch lookup captures include the production lookup inlined into the diagnostic Fixture.run caller; that caller is not a complete compiled RDF4J query. Compiled add() can become larger despite less array initialization and faster measured builds; machine-code size is supporting evidence, not a throughput estimate.

The fused multi-column hash experiment made fewer Java passes but produced a larger normal scalar C2 method and slower hash-only screening results. Captures do not establish lost SIMD vectorization: the baseline and fused multi-column loops inspected here are scalar. The final hashBatch method is restored exactly, and hash-only cases are controls rather than an optimization claim.

## Regression coverage and build boundaries

Seven distinct component suites passed **62,630,347 counted assertions/checks**. Repeated runs of the same generated-key suites are not counted again. These are looped assertions, not 62 million independently designed test cases.

| Suite | Counted checks |
|---|---:|
| Join regression | 35,999,275 |
| Packed I/O and inherited vector/page checks | 22,254,100 |
| UTF-8 | 343,391 |
| Binding domains | 1,306,622 |
| Generated keys | 43,337 |
| Optimized generated keys | 932,360 |
| Native sort | 1,751,262 |

The added suite uses independent HashMap/list tuple and chain oracles, full-width long extrema, zero and composite keys, forced full-hash collisions, 75% growth boundaries, stable duplicate order, chain statistics, selected/scattered batches, zero-width payloads, native/IR result parity, copied and borrowed factor iteration, scope close, concurrent published readers, checked capacity arithmetic, and array-byte accounting. It includes actual production refill/probe methods in a test shell. The original generated-key suites retain their no-dictionary-read/membership assertions.

The offline join build compiles 68 Java files: 31 complete production files plus explicitly identified extracted production classes/methods, test shells and external API doubles. NativeBatch is the complete class extracted unchanged from its multi-class source. Five complete modified production files pass the JDK parse phase separately. Parsing is not dependency type checking. Full HashJoinFactorCursor/HashJoinBatchCursor type-graph integration, generated Janino queries, the full RDF4J dependency build, real repository/LMDB data, the original query and AArch64 execution remain unverified.

Existing full-repository integration suites including LmdbNativeHashJoinChainStatsTest, LmdbNativeBatchedProbeTest, LmdbNativeAdaptiveHashJoinTest and LmdbNativeKernelIrEmitterTest should be run on the full branch. No private removed table fields were referenced by the supplied versions of these tests; legacy headBatch/int-head prepared lookup signatures remain available.

## Experiment history and reproducibility

Ten production-source experiment patches under `tools/lmdb-join/experiments/` are independent alternatives against the immediate packed-I/O baseline. They cover packed-only layouts, hash bit placement, fused hashing, prepared one-key loops, match-first probing, unfiltered exact probes, byte controls, original hashing restoration, and final scalar one-key control probing. Apply one to a fresh baseline, not in sequence or on top of this release. Earlier variants are not claimed to pass optimization invariants added to the final suite.

Run the commands in `tools/lmdb-join/README.md` to rebuild baseline/candidate components, reproduce the full matrix and C2 captures, and run the inherited packed-I/O, UTF-8, binding-domain, generated-key and native-sort suites. Source manifests label every compiled unit. Evidence source copies permit inspecting test-double boundaries without guessing from the report.

## Primary-source design references

Abseil Swiss Tables Design Notes: https://abseil.io/about/design/swisstables — explains compact presence/hash metadata and exact equality after filtering. The current Java patch borrows the compact control-byte idea, not Abseil's SIMD group-probing algorithm or its hash format.

CedarDB, Simple, Efficient, and Robust Hash Tables for Join Processing: https://cedardb.com/blog/simple_efficient_hash_tables/ — motivation for considering negative probes and compact join metadata. The delivered table retains its existing open addressing and duplicate chains; it is not an implementation of CedarDB's unchained table or Bloom-filter layout.

Cockroach Labs, 40x faster hash joiner with vectorized execution: https://www.cockroachlabs.com/blog/vectorized-hash-joiner/ — staged column/batch work and separation of initial candidate gathering from exact matching. No speedup number from that implementation is claimed for this Java code.

## Applying and verifying the submission

Use `lmdb-hash-join-performance.patch` against `lmdb-packed-performance-source.zip`, from the directory containing java/. The production-only alternative changes just five Java files. The cumulative alternative targets the original judges' source. Apply one patch, not several. The complete ZIP preserves every immediate-baseline file. Fresh-patch and ZIP verification, checksums and regression logs are supplied separately; the verification manifest states exactly which builds/tests were run.

```bash
git apply --check lmdb-hash-join-performance.patch
git apply lmdb-hash-join-performance.patch
```
