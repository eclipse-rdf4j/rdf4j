# M7 adjacency SIP Docker gate — 2026-08-04

Environment: Linux container, JDK 26, G1, 16 GiB heap, one benchmark thread. The acceptance runs used three forks,
three 3-second warmups, and five 5-second measurements per fork. Janino, merge, parallel execution, and direct
adjacency root scans were disabled so the compared route was a seekable LMDB root with adjacency-backed bound probes.

| Workload | SIP off (ms/op) | SIP on (ms/op) | Runtime-plan result |
| --- | ---: | ---: | --- |
| selective chain, corrected | 4.852 +/- 0.217 | 4.548 +/- 0.437 | `ACTIVATED`; 566 checks, 503 rejected rows |
| dense chain | 3.559 +/- 0.181 | 3.495 +/- 0.151 | `STATIC_COST_REJECTED`; domain and root both 5,000 |

The first truthful-route selective pair was 4.560 +/- 0.382 off versus 5.339 +/- 0.392 on. Paired Docker JFR showed
that each mask seek invalidated up to 1,024 rows already decoded into the chunk stage's raw buffer. The on profile added
`nmdb_cursor_get`, `TripleIndex.keyToQuadMatchStatus`, `TripleIndex.expectedValue`, and `LmdbRecordIterator.fill` to the
top CPU methods despite eliminating about 3,900 empty adjacency probes per query.

The root-cause fix caps active-mask prefetch at the exact remaining consecutive-miss budget before the next seek. In the
post-fix JFR, membership checks fell from 1,391 to 566 and the redundant LMDB cursor/decode methods disappeared from the
top CPU list. The corrected warmed matrix is parity-or-faster for both predeclared use cases, so
`rdf4j.lmdb.sip.adjacencyMasks.enabled` is default-on; explicit `false` remains the rollback switch.

Recordings:

- `profiles/lmdb/m7-sip-selective-isolated-off-2026-08-04.jfr`
- `profiles/lmdb/m7-sip-selective-isolated-on-2026-08-04.jfr`
- `profiles/lmdb/m7-sip-selective-prefetch-cap-on-2026-08-04.jfr`
