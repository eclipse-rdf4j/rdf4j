# LMDB common-path performance tests

This harness compares the supplied `generated-value-keys-optimized-source.zip` with this revision.
It requires a full JDK 26 installation (both `java` and `javac`) and Python 3. Native C2 disassembly
also requires GNU `objdump` on x86-64. No download or Maven resolution is needed for these isolated tests.
It does **not** build RDF4J or exercise an actual LMDB repository.

## Reproduction

Run from the new source root, with the previous ZIP extracted to `/path/to/previous-source`:

```sh
export JAVA_HOME=/path/to/jdk-26
python3 tools/lmdb-performance/run.py \
  --baseline /path/to/previous-source --out /tmp/lmdb-tuned
python3 tools/lmdb-performance/run.py \
  --source /path/to/previous-source --baseline /path/to/previous-source \
  --out /tmp/lmdb-base --compile-only
python3 tools/lmdb-performance/compare.py \
  --base /tmp/lmdb-base --tuned /tmp/lmdb-tuned \
  --out /tmp/lmdb-paired --forks 5
python3 tools/lmdb-performance/capture_c2.py \
  --build /tmp/lmdb-base --out /tmp/lmdb-c2-base
python3 tools/lmdb-performance/capture_c2.py \
  --build /tmp/lmdb-tuned --out /tmp/lmdb-c2-tuned
```

Output must be outside both source trees. Test builds can be recreated only in marked disposable
build directories. Use separate result directories for different experiments. The comparison alternates
base/tuned order by fork and retains every measured pass. Each of the 22 cases runs in five independent
JVMs per version, with ten warmup passes and seven measured passes. The reported value is the median of
the five per-fork medians, not a JMH confidence interval. `--filter` accepts comma-separated case-name
substrings. Baseline-only reference classes are never called by the timed benchmark methods.

C2 capture uses normal tiered compilation with diagnostic method printing, not forced compilation or
inlining. Every machine byte in each decoded main-code range must be present. HotSpot output can
interleave with application output; an incomplete capture is kept as `*-rejected-N.log` and retried,
never filled in from source or another compilation. Diagnostic timing is not included in benchmark results.
`--inlining` adds the optional verbose inlining trace. `--only` accepts comma-separated names from
`vector,page,utf8,sort,domain,sort-sorted,sort-reverse`.

## What is compiled

`run.py` writes source-hashed manifests distinguishing complete production files, complete extracted
production units, class-renamed baseline implementations, tests, and explicit external API doubles.

* CSF build: 28 Java files. Actual CSF classes, native memory access, packed codecs, page views, and ID
  encoding. `CoreDatatype` enum names and logging/annotation APIs are doubled. The two reference
  encoders are complete previous production files with class names changed only.
* UTF-8: the two complete production methods `decodeUtf8(ByteBuffer, int, int)` and `utf8Scratch(int)`
  are extracted with the same thread-local declaration and scratch bound. Actual JDK NIO and String
  behavior is used. The rest of `LmdbNativeValueCodec` is not compiled against RDF4J dependencies here.
* Binding view: 14 Java files, including complete `RowBindingSetView`, `NativeSlotLayout`,
  `QueryWideVarLayout`, and `SlotBindingSetView`. Binding/Value and value-source APIs are explicit doubles.
* Generated keys / native sort: 56 Java files prepared using the previous `tools/generated-keys/run.py`.
  Complete/extracted production identities, tables, DISTINCT/grouping/spill components and
  `NativeSortBuffer` are exercised. RDF model, comparator fixture, and budget APIs are explicitly
  identified doubles. The sort budget double tracks real reserve/release interactions but is not
  the full application memory manager.

The new sort and CSF algorithms are never supplied by doubles. The generated-value tests verify zero
storage calls using a source that throws if keying accesses dictionary membership or values.

## Cases and limits

Vector operations process 4,096 IDs; each packed block holds at most 256 values. Page cases have 512
root rows, variable neighbor/context counts, and reuse builder/native workspace. `page-*-write` includes
both layout measurement and writing, whereas plain `page-*` measures layout only. The datatype metadata
oracle returns a deterministic value and does not simulate LMDB latency. UTF-8 buffers are populated
outside the timed operation; there is no lookup or I/O in this test. Sort rows are prepopulated primitive
numeric keys, not full SPARQL expression comparisons. Binding-domain cases isolate replacement plus
`size()`: `false` changes already-bound values, whereas `true` also introduces unbound slots.
Fixed overlapping base binding domains are covered by correctness tests, not by these timings.

C2 and timing are x86-64 observations. Full RDF4J, Janino-generated execution, the original dataset/query,
AArch64, throughput under concurrent queries, and process RSS are not measured by this harness.

## Historical alternatives

Files in `experiments/` are independent diffs against the **previous input**, not sequential patches for
this final source. Apply each to a separate clean previous tree. The first scalar block-fill variant,
insertion thresholds, binary insertion, and merge-loop alternatives were screened; the final sort keeps
the previous general merge implementation. Historical screening logs are separated from `paired-final`
in the evidence archive. The final source/report is authoritative for retained changes.

See `LMDB_GENERAL_PERFORMANCE.md` at the source root for every result, tradeoff, exact format invariants,
and the validation boundary.
