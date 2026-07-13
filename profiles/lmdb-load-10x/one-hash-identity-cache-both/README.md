# Single-hash identity front cache

Packed value preparation now places a fixed 16,384-slot, direct-mapped identity cache in front of the semantic
`ObjectIntHashMap`. An identity hit avoids RDF value hashing and equality; misses still use the semantic map, so equal
but distinct values and cache collisions retain the original behavior. The cache slot is computed once and reused when
the resolved ID is installed.

The clean-host, exact JDK 26/G1/2 GiB five-warmup/five-measurement checks are:

| Isolation | Artifact | Score | Target |
| --- | --- | ---: | ---: |
| NONE | `../one-hash-identity-cache/jmh-5x5-run-1.json` | 72.232 ms/op | 78.10 ms/op |
| READ_COMMITTED | `rc-5x5.json` | 76.170 ms/op | 83.11 ms/op |

The NONE result uses the same single-hash cache code as the shared candidate; the RC cache was enabled afterward. A
paired confirmation run under `final-paired-run-1.json` was invalidated by a concurrent 16 GiB Maven reactor and is
intentionally not acceptance evidence.

The before-RC-cache async profile under `../one-hash-identity-cache/async-rc-cpu` attributes the dominant Java cost to
semantic-map lookup, probing, and value hashing. The earlier NONE cache profile under
`../none-identity-value-cache/async-cpu` shows that identity lookup displaced `String.equals` as the hot lookup path.
