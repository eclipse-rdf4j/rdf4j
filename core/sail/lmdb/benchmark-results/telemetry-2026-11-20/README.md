# Exploratory LMDB Telemetry comparison — 2026-11-20

This is a small before/after experiment for the telemetry diagnostics change, using one identical JMH fixture against baseline main and the patched working tree. **Compare revisions within the same API mode.** All timing comparisons have wide, overlapping JMH error intervals. This run does not establish a production speedup, a statistically reliable latency regression, or an overhead budget.

The clearest allocation change is in Telemetry COUNT: approximately 95 KB/op more Java heap allocation in this sample. This experiment measures the whole explanation call and does not attribute that increase to a particular collection or report-construction component. Ordinary VALUES evaluation also allocated approximately 2.54% more bytes/op; the evidence does not justify claiming zero overhead outside Telemetry.

## Revisions and artifacts

- Baseline: `2b53e4d637b698b1f5eda9d842e60e384b59ebe9` (HASMAC main), in an independent detached worktree and Maven dependency cache. Its tracked source tree remained clean.
- Branch: `GH-0000-lmdb-telemetry-diagnostics`, the telemetry working-tree changes based on that same commit, after the focused **217 tests / 0 failures / 0 errors / 0 skipped** gate. The enclosing change contains the tested source.
- [Identical benchmark fixture](../../src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/LmdbTelemetryOverheadBenchmark.java): SHA-256 `0b6a57c12142ded94925b1ece92266681c7d0fcb14aaa9ea1f4bfac0e331f742` in both worktrees.
- [Raw baseline JMH JSON](baseline.json) and [raw branch JMH JSON](branch.json) retain all six cases per revision, per-iteration samples, confidence intervals, and secondary GC metrics.
- Baseline benchmark jar SHA-256: `5b2f646c7d84d30f1543ddfa9921042607430b901578ac74521d53b8ca2707bd`.
- Branch benchmark jar SHA-256: `8e2e743a25aece69d492a033301bd99eac95646dceae98a843ef4c8e51e0393c`.

The baseline reactor was rebuilt with `clean install` before packaging its benchmark, replacing the seeded cache's project jars with baseline artifacts. The baseline benchmark jar was checked to exclude `QueryExecutionProfile`; the branch jar contains it. The jar hashes identify the measured artifacts; they are not source commit identifiers.

## Timing results

The error shown is JMH's **99.9% error bound from five measurement iterations in one fork**, in milliseconds per operation. Iteration-level uncertainty does not capture variation between independent JVM forks, machines, or production query populations.

| API mode | Shape | Baseline mean ± error (ms/op) | Telemetry branch mean ± error (ms/op) |
| --- | --- | ---: | ---: |
| `evaluate` | COUNT | 2.774 ± 3.893 | 1.917 ± 3.197 |
| `evaluate` | FANOUT | 108.055 ± 48.826 | 103.578 ± 35.690 |
| `evaluate` | VALUES | 71.611 ± 17.225 | 61.233 ± 18.626 |
| `telemetry` | COUNT | 2.891 ± 4.442 | 4.000 ± 1.114 |
| `telemetry` | FANOUT | 106.460 ± 53.528 | 130.159 ± 93.772 |
| `telemetry` | VALUES | 66.405 ± 20.822 | 70.022 ± 36.831 |

The direction of the point estimates alone is insufficient for a performance claim. For example, baseline COUNT Telemetry was `2.891 ± 4.442 ms/op`, while the branch was `4.000 ± 1.114 ms/op`. FANOUT Telemetry had especially broad uncertainty (`106.460 ± 53.528` versus `130.159 ± 93.772 ms/op`).

## Java allocation results

These are JMH `gc.alloc.rate.norm` means and 99.9% error bounds, in **bytes per operation**. This profiler estimates Java heap allocation; native LMDB allocations and resident memory are outside this measurement.

| API mode | Shape | Baseline mean ± error (B/op) | Telemetry branch mean ± error (B/op) |
| --- | --- | ---: | ---: |
| `evaluate` | COUNT | 287,585 ± 4,916 | 283,986 ± 4,883 |
| `evaluate` | FANOUT | 138,803,193 ± 219,108 | 138,867,324 ± 474,816 |
| `evaluate` | VALUES | 48,642,122 ± 141,557 | 49,877,839 ± 453,425 |
| `telemetry` | COUNT | 520,091 ± 7,388 | 614,808 ± 7,331 |
| `telemetry` | FANOUT | 69,234,594 ± 8,644,589 | 72,709,103 ± 12,203,066 |
| `telemetry` | VALUES | 52,701,887 ± 129,312 | 52,980,585 ± 153,832 |

Telemetry COUNT increased by about 94,717 B/op (18.2% of the baseline mean). Telemetry FANOUT and VALUES point estimates increased by about 3.47 MB/op and 0.279 MB/op respectively; FANOUT's allocation uncertainty is broad. Ordinary VALUES increased by about 1.24 MB/op (2.54%). These measurements motivate a longer controlled allocation/performance study if a strict overhead budget is needed.

**The two API modes have different consumption paths.** `evaluate` drains public result objects through a JMH `Blackhole`; `explain(Telemetry)` performs its internal row consumption and constructs the explanation model. This distinction already exists at baseline: FANOUT allocates approximately 139 MB/op in ordinary evaluation and 69 MB/op in Telemetry. A cross-mode ratio would combine those API-path differences with instrumentation and report-construction work, so this report does not use it as an isolated instrumentation-cost estimate.

## Workload, native routes, and coverage

Each trial creates a deterministic 50,004-statement store: 10,000 roots, four outgoing edges per root to shared targets, one label per root, and four target tags. Setup verifies ordinary `evaluate()` row counts and value checksums before timing. Both revisions and both modes reported the following native route labels in their unmeasured Telemetry setup snapshots:

| Shape | Rows consumed per operation | Result check | Observed native route |
| --- | ---: | --- | --- |
| COUNT | 1 | Count equals 40,000 | `indexedCount` |
| FANOUT | 40,000 | Tag checksum equals 60,000 | `nestedLoop` |
| VALUES | 10,000 | `LCASE(CONCAT(label,label))` checksum | `batch` |

These labels were retained from all six unmeasured Telemetry setup snapshots in each run; the ordinary timed result drain was not separately profiled. The native evaluator and direct adjacency were enabled, with adjacency mode `PREFER`, a 256 MiB adjacency budget, and successful readiness waits. Enabling Janino does not establish that these routes used a compiled fused kernel: the observed setup coverage is specifically `indexedCount`, `nestedLoop`, and `batch`.

The same fixture uses only APIs already present at baseline. Default row consumption is used for `explain(Level)`. VALUES forces lexical expression work in the query itself. The fixture does not exercise the new explicit values/serialization options, report JSON/HTML rendering, network transport, cold compilation, or parallel-worker behavior; it is not a broad spill/memory-pressure workload.

Before JMH warmup, each trial performs the ordinary-evaluation row/checksum verification and four unmeasured ordinary-evaluation/Telemetry pairs to populate applicable caches. All 12 cases passed those setup checks and the native-route presence assertion. Baseline ran first, followed by the branch; revision order was not randomized.

## Environment and settings

- JMH 1.37; Eclipse Adoptium OpenJDK 25.0.5, VM `25.0.5+8-LTS`.
- Linux 6.18.57, x86-64, Intel Xeon Platinum 8573C.
- One JMH thread, one fork per case, average-time mode, milliseconds/op.
- Three 1-second warmup iterations and five 1-second measurement iterations per case.
- JVM arguments: `-Xms1G -Xmx1G`; JMH selected compiler blackholes.
- GC profiler enabled. Builds and tests were paused throughout both sequential measurements. CPU affinity was not pinned, and dedicated-host isolation was not established.
- Fixture settings: `rdf4j.lmdb.nativeQueryEngine.enabled=true`, `rdf4j.lmdb.parallel.enabled=false`, `rdf4j.lmdb.janinoCodegen.enabled=true`, `rdf4j.lmdb.janinoCodegen.synchronous=true`, `rdf4j.lmdb.janinoCodegen.thresholdRows=0`.

## Exact commands

The session used Maven 3.9.16 and Java 25 from these toolchain paths:

```bash
export JAVA_HOME=/workspace/scratch/6c7f03e66aab/toolchain/jdk-25
export PATH="$JAVA_HOME/bin:/workspace/scratch/6c7f03e66aab/toolchain/maven/bin:$PATH"
```

Baseline dependency reset, in `/workspace/scratch/96ffdb7d9155/rdf4j-telemetry-base`:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -Pquick -DskipTests -DskipITs clean install
```

Benchmark packaging, in each worktree after its own dependencies were installed:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Pbenchmarks,quick -DskipTests -DskipITs package
```

The cache was already populated, so these commands ran offline. A reproduction with an empty dependency cache must first resolve those dependencies.

Baseline measurement, from the baseline worktree:

```bash
scripts/run-single-benchmark.sh \
  --module core/sail/lmdb \
  --class org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbTelemetryOverheadBenchmark \
  --method runQuery --no-build \
  --warmup-iterations 3 --measurement-iterations 5 --forks 1 \
  --param roots=10000 -- \
  -prof gc -w 1s -r 1s -rf json \
  -rff /workspace/scratch/96ffdb7d9155/rdf4j/core/sail/lmdb/benchmark-results/telemetry-2026-11-20/baseline.json
```

Branch measurement, from `/workspace/scratch/96ffdb7d9155/rdf4j`, used the same command and changed only the result path to:

```text
/workspace/scratch/96ffdb7d9155/rdf4j/core/sail/lmdb/benchmark-results/telemetry-2026-11-20/branch.json
```

The script used the already-built `core/sail/lmdb/target/jmh-benchmarks.jar` in each worktree and expanded the JMH selection to `org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbTelemetryOverheadBenchmark.runQuery`. Large stdout logs and benchmark jars are excluded from this evidence directory.
