# ANALYTICS calibration after enabling the outgoing node-predicate index

This run recalibrated the ANALYTICS Theme benchmark after enabling construction and serving of the outgoing
node-predicate projection by default. The run used commit `48a43d9129`, Eclipse Temurin JDK 26, `-Xms1G -Xmx16G`,
Janino enabled, and synchronous adjacency construction.

The existing per-store calibration was moved to `cost-model.lncm.pre-node-predicate-20260902` before the run, so the
first pass started with a cold cost model. The newly populated sidecar has SHA-256
`a516f03a6568de41746dc6319c0c9dda1892207aaf3dcc101fedc708b6e191e7`. This binary is deliberately not committed:
the calibration is specific to the machine, JVM, data store, and runtime configuration. This report is the durable
repository artifact.

## Cold calibration pass

The pass ran all thirteen ANALYTICS queries with three 500 ms warmups, one 500 ms measurement, and one fork:

```text
java -jar core/sail/lmdb/target/jmh-benchmarks.jar \
  org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery \
  -p themeName=ANALYTICS \
  -p z_queryIndex=0,1,2,3,4,5,6,7,8,9,10,11,12 \
  -p z_z_janinoEnabled=true \
  -wi 3 -w 500ms -i 1 -r 500ms -f 1 \
  -jvmArgsAppend "-Drdf4j.benchmark.profiling=true -Drdf4j.lmdb.themeQueryBenchmark.waitForDirectAdjacency=true" \
  -rf json \
  -rff core/sail/lmdb/target/analytics-node-predicate-calibration-20260902.json
```

| Query | Score (ms/op) |
| ---: | ---: |
| 0 | 0.035 |
| 1 | 0.065 |
| 2 | 0.100 |
| 3 | 0.148 |
| 4 | 0.112 |
| 5 | 0.130 |
| 6 | 6.168 |
| 7 | 11.067 |
| 8 | 48.258 |
| 9 | 29.555 |
| 10 | 0.157 |
| 11 | 0.123 |
| 12 | 0.036 |

## Focused calibrated verification

Queries 8 and 10 were then rerun against the populated sidecar with three 500 ms warmups, three one-second
measurements, and one fork. The JSON result is
`core/sail/lmdb/target/analytics-node-predicate-calibration-focused-20260902.json`.

| Query | Score (ms/op) | Raw measurement iterations (ms/op) | Acceptance target |
| ---: | ---: | --- | ---: |
| 8 | 45.948 | 47.168, 46.401, 44.274 | 49.01 |
| 10 | 0.294 | 0.334, 0.288, 0.259 | 33.01 |

The large displayed 99.9% confidence intervals (`+/- 27.354` and `+/- 0.696`) are an artifact of only three samples;
the raw measurements are included above. Every measured q8 iteration meets its target, and q10 is more than two
orders of magnitude below its target. q10's first warmup took about 1,011 ms while constructing its lazy retained
domain views; its later warmups were 1.212 and 0.464 ms/op, and all measured iterations were below 0.335 ms/op.

The full-data q10 diagnostic selected compiled parallel `EnumerateNodeDomainIntersection` IR with 16 disjoint
partitions, 15 workers started, peak simultaneous activity of 15, and non-zero work from all 15 workers. It performed
zero per-statement EXISTS filter tests and zero adjacency-root scans. This proves that the repaired IR path both
activates and executes concurrently.

This is calibration evidence and a focused one-fork performance check, not the final five-fork acceptance run.
