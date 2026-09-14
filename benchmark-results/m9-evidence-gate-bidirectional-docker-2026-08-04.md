# M9 evidence gate and bidirectional property-path search — Linux Docker

Date: 2026-08-04

Runtime: Linux container, SapMachine OpenJDK 26, one benchmark thread. The candidate changes only
`rdf4j.lmdb.nativePath.bidirectional.enabled`; `false` retains exact forward adjacency breadth-first search and `true`
allows an existence path with both endpoints bound to meet in the middle over exact outgoing and incoming adjacency.
The benchmark store is bulk-loaded with direct adjacency disabled, reopened with FULL/PREFER direct adjacency and a
256 MiB cap, and waits for exact publication before measurement. This makes the measured route deterministic rather
than racing asynchronous adjacency construction.

## Evidence audit

- Run-intersection semijoin: already delivered in the more general `LmdbNativeExistsIntersection` operator documented
  by `plans/lmdb-native-engine/14-exists-distinct-intersection.md`. Its ANALYTICS q10 gate improved from 2975.712 ms to
  162.8 ms (about 18x), and focused parity coverage already exists. M9 therefore does not duplicate it.
- Degree binding: M5 already binds the standalone degree aggregation from exact run sizes. The required theme-telemetry
  witness of an uncovered degree subpattern inside a larger query was not found, so no additional recognition rule is
  funded.
- Exact-empty pruning: endpoint-bound `PatternPlan.estimate` already asks `source.exactDegree`; dictionary constants
  absent from LMDB already decline native compilation. No workload demonstrated material planning or startup savings,
  so the speculative pruning operator is dropped.
- Bidirectional path search: funded. `boundExistence` builds a depth-five, fan-out-eight tree (about 37,000 edges),
  chooses a deepest leaf as the target, and makes forward-only BFS visit more than ten times the meet-in-middle
  frontier.

## Profiled deep-path pair

`PropertyPathReachabilityBenchmark.executeQuery`, `variant=boundExistence`, no warmup, 10 x 10-second measurements,
one fork, JFR CPU-time sampling:

| Search route | Score (ms/op) | 99.9% error |
| --- | ---: | ---: |
| Forward adjacency BFS (`false`) | 2.999 | 0.176 |
| Bidirectional adjacency (`true`) | 0.013 | 0.001 |

The meet-in-the-middle route is about 230.7x faster (99.57%). Recordings:

- `profiles/lmdb/m9-path-bidirectional-exact-off-2026-08-04.jfr`
- `profiles/lmdb/m9-path-bidirectional-exact-on-2026-08-04.jfr`

The disabled profile is dominated by `unsignedContains` (54.30%) and `PathDiscoveredRuns.contains` (20.11%). The
enabled profile removes that visited-set workload; `tryBidirectionalExistence` is only 2.46% and query parsing becomes
the largest remaining cost. Earlier recordings without deterministic reopen/readiness are retained as diagnostic
artifacts but are not acceptance evidence because they exercised LMDB fallback.

## One-hop negative control

The initial warmed three-fork one-hop pair was 0.010 +/- 0.001 ms/op off and 0.011 +/- 0.001 ms/op on. Although the
intervals overlap, a direct-forward membership check was added so a one-edge hit never opens the reverse view or BFS
frontiers. The repeated pair was 0.010 +/- 0.001 ms/op both off and on. The deep-path win and one-hop parity satisfy the
user's parity-or-faster default-enable gate, so the feature is default-on and explicit `false` restores forward-only
search.
