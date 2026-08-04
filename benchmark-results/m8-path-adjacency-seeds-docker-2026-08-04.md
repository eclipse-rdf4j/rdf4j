# M8 adjacency-backed property-path seeds — Linux Docker gate

Date: 2026-08-04

Runtime: Linux container, SapMachine OpenJDK 26, one benchmark thread. The candidate changes only
`rdf4j.lmdb.nativePath.adjacencySeeds.enabled`; `false` retains the LMDB predicate-root seed scan and `true` enumerates
the exact in-memory adjacency key domain. Both routes use the same adjacency-backed frontier expansion.

## Profiled pair

`PropertyPathReachabilityBenchmark.executeQuery`, `variant=unboundStarts`, no warmup, 10 x 10-second measurements,
one fork, JFR CPU-time sampling:

| Seed route | Score (ms/op) | 99.9% error |
| --- | ---: | ---: |
| LMDB root scan (`false`) | 2.166 | 0.129 |
| Adjacency keys (`true`) | 1.675 | 0.104 |

The adjacency-key route is 22.7% faster. Recordings:

- `profiles/lmdb/m8-path-adjacency-seeds-off-2026-08-04.jfr`
- `profiles/lmdb/m8-path-adjacency-seeds-on-2026-08-04.jfr`

The disabled recording attributes 0.98% of CPU samples to `LmdbDirectAdjacencyRootIterator.fill`, 1.35% to
`PathCursor.nextAllPairs`, 12.07% to adjacency-delta row comparison, and 3.31% to `PathCursor.expandNext`. The enabled
recording removes the LMDB root iterator from the top 25 methods and reduces those latter shares to 0.84%, 5.73%, and
1.84%, respectively. The remaining top cost is direct adjacency lookup, as expected for the common frontier path.

## Warmed default-on gate

`PropertyPathReachabilityBenchmark.executeQuery`, three forks, 3 x 3-second warmup and 5 x 5-second measurements per
fork:

| Variant | `false` (ms/op) | `true` (ms/op) | Verdict |
| --- | ---: | ---: | --- |
| `unboundStarts` | 2.102 +/- 0.124 | 1.554 +/- 0.072 | adjacency keys 26.1% faster |

Bound-path negative controls used two forks, 2 x 2-second warmup and 3 x 3-second measurements per fork:

| Variant | `false` (ms/op) | `true` (ms/op) | Verdict |
| --- | ---: | ---: | --- |
| `selectReachable` | 0.434 +/- 0.032 | 0.438 +/- 0.075 | parity; intervals overlap |
| `countReachable` | 0.067 +/- 0.004 | 0.068 +/- 0.008 | parity; intervals overlap |
| `reverseReachable` | 0.427 +/- 0.027 | 0.429 +/- 0.034 | parity; intervals overlap |
| `AdjacencyQueryShapeBenchmark.pathReachability` | 2.488 +/- 0.133 | 2.547 +/- 0.111 | parity; intervals overlap |

The controls have a bound physical start and therefore do not enter seed enumeration. The paired results confirm that
the default property check does not measurably regress them. The affected unbound-start workload is faster in both the
profiled and warmed experiments, so M8 passes the user's parity-or-faster default-enable gate.
