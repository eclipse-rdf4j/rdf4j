# Versioned canonical IRI storage: benchmark evidence

## Findings

Format 7 performed **zero alternative-IRI lookup calls, native alternative probes and native alternative seeks in
all 120 measured canonical dictionary observations**. Against the actual pre-change format-6 binary, distinct-IRI
scalar median throughput was 4.15–38.91× the original rate, and batch throughput was 1.84–41.25×, across warm/cleared Java-cache
cases. The original binary performed thousands to more than a million alternative probes per 4,096-input invocation.
The corresponding format-7 counts were zero.

This run did **not** show a general import-throughput improvement. Normal-import medians were 0.702–0.904× the original
binary; initial-run imports ranged from 0.685× to 1.191×, and bulk imports from 0.927× to 1.090×. Caller-thread allocation
for normal/initial imports stayed essentially unchanged. Fresh prepared/assigned dictionary medians ranged from
0.80× to 1.89× and allocated approximately 24 additional bytes per repeated input or 48 bytes per distinct input.
Those paths already performed zero native alternative probes/seeks in the original binary.

The observations have substantial variability. For example, the five normal/shared canonical import observations
ranged from 29,824 to 150,768 statements/second, while the original binary ranged from 151,507 to 209,255. This is an
exploratory single-machine comparison, with one JVM per matrix. The deterministic counter reduction is stronger
evidence than any individual throughput ratio. Further profiling and independent forks are needed to attribute the
import differences; the slower measurements have been retained rather than replaced by selected reruns.

Every paired old/new dictionary file length, reverse-ID record count and namespace count was identical, including
all import paths. Each footprint was also constant across its five observations. No dictionary migration or
namespace-record rewriting was performed.

The four matrices contain **440 measured rows**: 200 current dictionary observations (80 format 2, 120 format 7),
120 original format-6 dictionary observations, and 60 import observations per binary. They were run after the final
37-class correctness selection passed **864 tests with no failures, errors or skips**. See the separate
[validation report](../validation/canonical-iri-storage.md) for correctness and release-compatibility evidence.

## Scalar and batch dictionary results

Rates are input IRIs per second. The format-6 column runs the untouched pre-change binary; the format-7 column runs
the final implementation. Ratios are medians from separate JVMs. Format 2 is reported separately below.

### Warm Java dictionary caches

| Path | Workload | Original 6 /s | Current 7 /s | 7 / 6 |
| --- | --- | --- | --- | --- |
| Scalar | Shared | 111,094 | 461,033 | 4.15× |
| Scalar | Repeated | 5,362,092 | 10,892,111 | 2.03× |
| Scalar | Nested | 17,942 | 617,394 | 34.41× |
| Scalar | Long namespace | 10,351 | 402,798 | 38.91× |
| Batch | Shared | 320,524 | 883,356 | 2.76× |
| Batch | Repeated | 1,186,839 | 3,053,569 | 2.57× |
| Batch | Nested | 17,998 | 361,067 | 20.06× |
| Batch | Long namespace | 62,001 | 491,240 | 7.92× |

### Java dictionary caches cleared once before insertion

The OS page cache was not evicted.

| Path | Workload | Original 6 /s | Current 7 /s | 7 / 6 |
| --- | --- | --- | --- | --- |
| Scalar | Shared | 125,416 | 615,013 | 4.90× |
| Scalar | Repeated | 7,801,667 | 7,876,756 | 1.01× |
| Scalar | Nested | 18,329 | 563,618 | 30.75× |
| Scalar | Long namespace | 14,467 | 251,169 | 17.36× |
| Batch | Shared | 348,377 | 639,530 | 1.84× |
| Batch | Repeated | 1,312,485 | 2,574,525 | 1.96× |
| Batch | Nested | 16,390 | 676,004 | 41.25× |
| Batch | Long namespace | 54,488 | 358,134 | 6.57× |

### Removed native work and allocation

Counters cover all 4,096 input IRIs per invocation; allocations are calling-thread bytes per input, rounded to the
nearest byte. The same native counts were observed with warm and cleared Java caches. Every corresponding format-7
alternative counter is zero. The original binary has no actual-search-entry hook, so its call count is unavailable;
the probe and seek counts below use its pre-existing native hooks.

| Path | Workload | Original 6 probes | Original 6 seeks | Allocated B/IRI, 6 → 7 |
| --- | --- | --- | --- | --- |
| Scalar | Shared | 89,002 | 8,192 | 1,728 → 904 |
| Scalar | Repeated | 18 | 2 | 49 → 49 |
| Scalar | Nested | 756,650 | 57,344 | 8,077 → 975 |
| Scalar | Long namespace | 1,375,146 | 8,192 | 7,512 → 1,920 |
| Batch | Shared | 8,209 | 8,192 | 1,558 → 670 |
| Batch | Repeated | 18 | 2 | 432 → 456 |
| Batch | Nested | 548,912 | 57,344 | 9,892 → 742 |
| Batch | Long namespace | 8,523 | 8,192 | 7,343 → 1,687 |

Repeated inputs remove little native work: their entire invocation performs only 18 alternative probes and two
alternative seeks in the original binary. Their large, variable throughput ratios should not be interpreted as
removing thousands of searches. The batch repeated case also allocates about 24 more bytes per input in format 7.

## Fresh dictionary paths

Both binaries perform zero native alternative probes/seeks on these paths. Format 7 additionally verifies zero
actual alternative-search entries. Rates are input IRIs per second; allocations are calling-thread bytes per input.

| Path | Workload | Original 6 /s | Current 7 /s | 7 / 6 | Allocated B/IRI, 6 → 7 |
| --- | --- | --- | --- | --- | --- |
| Prepared | Shared | 1,019,239 | 1,012,087 | 0.99× | 730 → 778 |
| Prepared | Repeated | 2,237,260 | 4,228,107 | 1.89× | 134 → 158 |
| Prepared | Nested | 503,676 | 833,761 | 1.66× | 874 → 922 |
| Prepared | Long namespace | 388,822 | 530,054 | 1.36× | 2,763 → 2,811 |
| Assigned | Shared | 839,858 | 991,392 | 1.18× | 783 → 831 |
| Assigned | Repeated | 5,270,587 | 4,226,846 | 0.80× | 158 → 182 |
| Assigned | Nested | 458,294 | 691,547 | 1.51× | 999 → 1,047 |
| Assigned | Long namespace | 234,618 | 362,683 | 1.55× | 3,832 → 3,880 |

## Normal, initial-run and bulk imports

Rates are input statements per second. Every import starts with an empty new store. Imports appended to a populated
store were not measured. Allocation covers only the calling Java thread, excluding worker threads and native memory;
the bulk figures are therefore not total allocation costs. Probe/call counts for imports are unavailable and recorded
as `-1` in the raw data.

| Path | Workload | Original 6 /s | Current 7 /s | 7 / 6 | Caller B/statement, 6 → 7 |
| --- | --- | --- | --- | --- | --- |
| Normal | Shared | 165,030 | 128,514 | 0.78× | 3,018 → 3,017 |
| Normal | Repeated | 188,018 | 170,058 | 0.90× | 2,338 → 2,337 |
| Normal | Nested | 140,165 | 106,570 | 0.76× | 3,541 → 3,540 |
| Normal | Long namespace | 51,189 | 35,949 | 0.70× | 8,990 → 8,989 |
| Initial | Shared | 121,753 | 101,647 | 0.83× | 3,276 → 3,275 |
| Initial | Repeated | 112,357 | 133,829 | 1.19× | 2,596 → 2,595 |
| Initial | Nested | 103,430 | 70,883 | 0.69× | 3,799 → 3,798 |
| Initial | Long namespace | 48,696 | 41,232 | 0.85× | 9,249 → 9,247 |
| Bulk | Shared | 10,194 | 9,499 | 0.93× | 22,824 → 22,731 |
| Bulk | Repeated | 11,458 | 12,174 | 1.06× | 19,025 → 18,981 |
| Bulk | Nested | 9,091 | 9,908 | 1.09× | 24,561 → 24,471 |
| Bulk | Long namespace | 6,319 | 5,856 | 0.93× | 56,449 → 56,359 |

Normal and initial-run import allocation differs by approximately one byte per statement. That does not establish
unchanged total CPU or worker allocation. These data support a narrower claim: canonical dictionary misses avoid the
alternative-prefix search. They do not support promising faster whole imports.

## Current format-2 compatibility path

These rows run format 2 through the final implementation's preserved legacy path. They are **not performance results
for the 6.1.0 release binary**. Rates are input IRIs per second; alternative calls cover the whole 4,096-input
invocation and are identical in the warm/cleared cases. Native alternative counts match the original format-6 counts
listed above for these IRI-only workloads.

| Path | Workload | Warm /s | Cleared /s | Allocated B/IRI | Alternative calls |
| --- | --- | --- | --- | --- | --- |
| Scalar | Shared | 194,743 | 187,458 | 1,704 | 4,096 |
| Scalar | Repeated | 9,799,676 | 7,117,153 | 49 | 1 |
| Scalar | Nested | 25,130 | 26,127 | 8,053 | 4,096 |
| Scalar | Long namespace | 14,708 | 18,165 | 7,488 | 4,096 |
| Batch | Shared | 371,014 | 396,956 | 1,534 | 4,096 |
| Batch | Repeated | 2,761,280 | 2,333,607 | 456 | 1 |
| Batch | Nested | 23,354 | 25,984 | 9,868 | 4,096 |
| Batch | Long namespace | 89,601 | 79,837 | 7,319 | 4,096 |

## Dictionary footprint

All 6/7 comparisons have identical bytes, reverse-ID records and namespace counts. Representative rows are below;
the raw CSVs contain every path and workload. The nested scalar case includes its 49 deliberate seed namespaces.
Fresh dictionary and import cases have no preseeded nested prefixes.

| Path | Workload | Bytes, both binaries | Records, both | Namespaces, both |
| --- | --- | --- | --- | --- |
| Scalar | Shared | 532,480 | 4,098 | 1 |
| Scalar | Repeated | 69,632 | 3 | 1 |
| Scalar | Nested | 548,864 | 4,194 | 49 |
| Scalar | Long namespace | 540,672 | 4,098 | 1 |
| Prepared | Shared | 536,576 | 4,097 | 1 |
| Assigned | Shared | 540,672 | 4,097 | 1 |
| Normal | Shared | 897,024 | 8,196 | 3 |
| Initial | Shared | 921,600 | 8,196 | 3 |
| Bulk | Shared | 884,736 | 8,196 | 3 |
| Bulk | Repeated | 425,984 | 4,101 | 3 |

## Recorded environment and binaries

The final run started at `2026-10-29T21:03:02Z` on Linux x86-64, Intel Xeon Platinum 8573C, with an eight-CPU cgroup
quota and an 8 GiB memory limit. Both runtimes used Eclipse Temurin 25.0.5+8 and identical JVM options:
`-Xms512m -Xmx1g -XX:ActiveProcessorCount=2 -XX:+UseG1GC --enable-native-access=ALL-UNNAMED -ea`.
No Maven build, formatter or other benchmark ran concurrently.

The original format-6 jar was saved from the initial unmodified build at the pinned main commit. Its loaded
`ValueStore.class` hash matches the provenance of the authentic unsupported-format fixture. The prebuilt-classpath
mode records that source verification must be done independently; that saved build and fixture provenance supply
that verification for this run. The script's default mode instead checks out and verifies the pinned commit itself.

```text
a213a0ab9a8c94d7619df40d3018206df5e53c436f151904001d3dfefddac8fd  current formats 2/7 jar
002977d189b596a543fe081ae65aa2d1e91d3953aa5e3d8b5f68446e0ed73863  original unsupported format 6 jar
f89b0ae5eb7030262be0a2d8790ce9f17d5ce0499cf8970750eded25d74856c4  original ValueStore.class; matches authentic fixture
efb0110ce84889bf9f52b70683bd0503837cb29b9c6f70e2f371c8eef4fa650a  benchmark Java source
```


## Method and scope

The current implementation supports persisted formats **2 and 7**. Its format-2 measurements exercise the current
compatibility path, not the original RDF4J 6.1.0 binary. The unreleased format 6 is measured only by an independent,
unmodified binary from HASMAC-AS/rdf4j commit `db572a45a401d9e7284b321efba0adbd6d577e6c`. It is a performance baseline;
current code rejects format-6 stores. Actual release-6.1.0 read/write/reopen compatibility is verified separately by
[the pinned fixture tests](../../core/sail/lmdb/src/test/resources/lmdb-compat/canonical-iri-fixtures.md).

`CanonicalIriStorageBenchmark` runs four matrices in separate JVMs, in this order:

1. Current dictionary code, formats 2 and 7.
2. Original pre-change dictionary code, format 6.
3. Original pre-change normal, initial-run and bulk imports, format 6.
4. Current normal, initial-run and bulk imports, format 7.

The harness is compiled separately against each binary. The selected format is checked against the live dictionary
or persisted `store.properties`, so a class compiled against the wrong `CURRENT_VERSION` fails. The script snapshots
each LMDB jar before compiling or measuring and records its SHA-256. Changes in the main checkout cannot replace that
jar during the run.

Each case processes 4,096 input records, with two full warmup rounds followed by five measured rounds. Case order is
deterministically shuffled each round. Tables report medians of those five observations; ratios divide the two
medians. Inputs and store setup are outside the measurement bracket; writes and commit are inside. Fresh dictionary
cases also include capacity reservation, assignment/preparation, encoding and publication. Normal and initial-run
imports exclude repository initialization and shutdown; bulk includes the public loader call and its publication
work. Compare absolute timings within the same path.

| Workload | Input | Dictionary preparation |
| --- | --- | --- |
| Shared | 4,096 distinct IRIs under `urn:canonical-bench:shared:` | One seed namespace |
| Repeated | One lexical IRI repeated 4,096 times | One seed namespace |
| Nested | 4,096 distinct IRIs under `urn:` followed by 48 `p:` components | All 49 prefix namespaces seeded for scalar/batch |
| Long namespace | 4,096 distinct IRIs under a namespace containing 320 `漢` characters | One long, hash-indexed namespace |

Imports use unique subject IRIs, one predicate, and these object IRIs, so the repeated-object case still contains
4,096 distinct statements. Fresh prepared/assigned dictionaries cannot be preseeded; their nested workload has the
long nested namespace without previously persisted alternative prefixes. Inputs already use the canonical boundary.
Arbitrary caller boundaries are covered by regression and compatibility tests, separately from this performance
measurement.

Literal inlining, the value overlay and force-sync are disabled. Value/triple maps start at 128 MiB with automatic
growth enabled. Bulk loading uses four partitions, one worker, a 32 MiB memory budget and 16 open files. The small
input exercises the removed work; it does not model a multi-gigabyte production import.

### What the metrics mean

- `alternative_lookup_calls` observes the actual lexical alternative-search entry point. Every canonical dictionary
  case asserts this count and both alternative native counters remain zero. The original binary lacks the new call
  hook, so its unavailable count is `-1`; its existing native probe/seek hooks remain usable.
- `alternative_native_probes` and `alternative_native_seeks` count native work inside alternative-IRI fallback only.
  They do not count all native database operations. `dictionary_lookup_calls` counts logical calls to the scalar
  transaction lookup method, not independent native probes.
- `caller_thread_allocated_bytes` uses `ThreadMXBean` around the measurement. It excludes worker-thread and native
  allocations. Import results therefore are not whole-process allocation totals. Unavailable import probe counts
  are `-1`, never zero.
- `warm` means Java dictionary caches retain seed values after setup; `cleared` means those caches are cleared once
  before insertion. Both warm naturally during the invocation. **The OS page cache is not evicted**: these are not
  OS-cold I/O measurements.
- `dictionary_bytes` is the dictionary's `data.mdb` file length, including native allocation overhead.
  `dictionary_records` counts persisted reverse-ID records; `namespace_records` counts their namespace subset.
  Inspection happens after close, outside the measurement bracket.

The dictionary throughput figures include the counting hooks. Instrumentation can affect their timings; the import
matrices use the ordinary store without that subclass. One JVM per matrix and five observations per case do not
establish tight confidence intervals. Repeated-value cases can complete in under a millisecond, making ratios
particularly sensitive to scheduling and JIT compilation. Raw individual observations are retained so variability
is visible. These are exploratory measurements on one machine, not release performance promises.

## Reproduction

Use JDK 25 or newer and Maven. The following command builds the actual release and original pre-change binary from
pinned commits in separate checkouts and Maven repositories, runs compatibility/rejection checks, snapshots the LMDB
jars, compiles each benchmark harness independently, and runs the four matrices:

```bash
scripts/benchmark-canonical-iri-storage.sh --output-dir /tmp/canonical-iri-results
```

To reuse already-built independent runtimes, provide both classpath files. Each must identify exactly one LMDB jar;
loose production classes are rejected:

```bash
scripts/benchmark-canonical-iri-storage.sh \
  --current-classpath /absolute/path/current-classpath.txt \
  --format6-classpath /absolute/path/original-format6-classpath.txt \
  --output-dir /tmp/canonical-iri-results \
  --values 4096 --warmups 2 --repetitions 5
```

Set `RDF4J_CANONICAL_COMPAT_WORKDIR` and `RDF4J_CANONICAL_BENCH_WORKDIR` to choose the separate build/evidence directories.
The output directory must not already contain these result files; the script refuses to overwrite evidence. Avoid
concurrent builds or other benchmarks during measurement. Every run saves its environment, options, source hash,
binary hashes, four CSV files, separately compiled harnesses and immutable runtime jars. Unavailable measurements use
`-1`. Re-run with more independent JVM forks and a production-sized data set before using throughput ratios for
capacity planning.

Raw result files:

- [Current dictionary formats 2 and 7](canonical-iri-results/dictionary-current-formats-2-7.csv).
- [Original pre-change dictionary format 6](canonical-iri-results/dictionary-prechange-format-6.csv).
- [Original pre-change imports](canonical-iri-results/imports-prechange-format-6.csv).
- [Current canonical imports](canonical-iri-results/imports-canonical-format-7.csv).
- [Environment, options and binary hashes](canonical-iri-results/environment.json).
