# Packed CSF I/O: exact format, fewer memory operations

Baseline: `lmdb-general-performance-source.zip`. The production diff is limited to
`csf/PackedLongVector.java` and `csf/LeBytes.java`. The source ZIP retains the complete
previous implementation, including its generated-value keys, value overlay, and tools.

## Reproduce

Use a JDK 26 installation, Python 3, and GNU `objdump` for disassembly. No Maven,
external downloads, assembler plugin, or additional Java dependency is used by these
component tools. Put outputs outside both source trees.

```sh
export JAVA_HOME=/path/to/jdk-26
BASE=/path/to/extracted/lmdb-general-performance-source
OUT=/tmp/lmdb-packed-results

python3 tools/lmdb-packed/regressions.py --baseline "$BASE" --out "$OUT/tests"
python3 tools/lmdb-packed/run.py --baseline "$BASE" --out "$OUT/tuned" --compile-only
python3 tools/lmdb-packed/run.py --source "$BASE" --baseline "$BASE" --out "$OUT/base" --compile-only

# Each timing is an independent JVM; before/after order alternates between forks.
python3 tools/lmdb-packed/compare.py --base "$OUT/base" --tuned "$OUT/tuned" \
  --out "$OUT/timings" --forks 5

# Diagnostics run separately from timing. No forced compilation or inlining.
python3 tools/lmdb-packed/capture_c2.py --build "$OUT/base" --out "$OUT/c2-base" --only write,heap,control
python3 tools/lmdb-packed/capture_c2.py --build "$OUT/tuned" --out "$OUT/c2-tuned" --only write,heap,control

# Reproduce rejected experimental constant-width helper bodies for review (not in the release).
python3 tools/lmdb-packed/generate_narrow.py > "$OUT/narrow-helpers.java.txt"
```

`compare.py --filter write-13,copy-3,heap-get-13` selects exact named cases. Its optional
`--cpu N` pins to one allowed Linux logical CPU; default runs do not set affinity.
The delivered report identifies the final sustained subset and retains the broader,
shorter screening runs separately. No selected fork is discarded. All 39 available
cases can be rerun with the command above.

### Measurement units

Vector fixtures contain 4,096 IDs. `write` resets a preallocated producer, plans and
writes a previously sized vector into reused native memory; separate sizing and
native allocation are outside timing. `encode` includes heap allocation and planning.
`copy` writes all 4,096 decoded IDs into a reused array; `slice` decodes 257 IDs from a
moving start to a nonzero output offset. `heap-get` and `native-get` each perform
4,096 pseudorandom scalar lookups per operation, not one lookup. `page-core-write`
measures both sizing and writing a full CSF page. No dataset or query evaluation is
included. The declared integer width describes input data; the production planner
selects its normal raw/typed or delta representation.

The final harness warms for **at least ten passes and one second**, calibrates passes
toward 50 ms, then measures seven passes. The median within each JVM and median across
forks are reported. Thread CPU time is an additional diagnostic, not a replacement
for wall time. Fixtures and permutations are generated before timing, outputs escape,
and selected result values contribute to a volatile checksum. Short exploratory
runs are preserved with their earlier harness source rather than relabeled as
sustained results. These are dependency-free component experiments, not JMH confidence
intervals or end-to-end query measurements.

## What is compiled

The complete production CSF package, `ValueIds`, and `LmdbRuntimeProperties` are
compiled. The reference encoder, vector codec, **and little-endian helper** come from
the baseline with class/helper names renamed; no alternative compression algorithm
implements the oracle. External RDF CoreDatatype enumeration, logger, and annotations
are explicit API doubles. No double supplies packing, native I/O, CSF layout or
fingerprint behavior. Build manifests record origins and source hashes.

The existing UTF-8, binding-domain, generated-key, and native-sort suites are also run.
Their manifests distinguish full production files, extracted production methods,
and model/factory/engine doubles. They do **not** compile the complete RDF4J dependency
graph or generated Janino query code. The historical `CsfPerfSuite` asserts that the
old, pre-previous-revision trait scanner performs redundant dictionary lookups. That
obsolete expectation does not apply to this immediate baseline. `PackedIoSuite`
instead invokes the inherited vector comparisons and page comparison function,
adds broader pages and encodings, and avoids that historical assertion.

## Safety and storage

The writer now emits whole little-endian words, then exact final bytes. It never
assumes writable native tail padding. Native reads retain the pre-existing eight-byte
readable-tail contract; the released native decoders are unchanged. Experimental
narrow unpackers were rejected after sustained mixed-profile regressions. Heap reads do
not assume padding and explicitly handle both a ninth spill byte and the exact final
partial word. Tests use dirty, unaligned memory, guarded regions and arrays, random
slice starts, all supported widths, typed IDs and unsigned extrema. Encoded vector
and page bytes must match the baseline.

No new ID index, decoded-length array, cache, format flag, or per-record storage is
introduced. The static VarHandle and changed helper/compiled code have fixed
process/code-cache costs; unchanged data storage is not a claim of zero total memory
cost. The full page allocation clear remains; only redundant vector payload clearing
is removed. The returned heap arrays and native allocation sizes are unchanged.

## Experimental patches

Files in `experiments/` are **standalone production diffs against the same baseline**,
not patches to apply sequentially. Do not stack them or apply them on top of the
released source. They preserve rejected alternatives and dispatch experiments;
`07-conservative-native.patch` is the released production candidate. Width-specialized
native readers are preserved only as rejected experiments, not shipped in production. The accompanying
README explains each variant. Only the selected candidate appears in the main
production diff.
