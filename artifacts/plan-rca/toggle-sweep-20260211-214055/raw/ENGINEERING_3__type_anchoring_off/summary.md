# Plan RCA Summary

## ENGINEERING #3

- observed: DP avgLast20ms=170, Greedy avgLast20ms=140, slowdown=21.43%
- plan delta: DP stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`http://example.com/theme/engineering/verifiedBy/110 calls=3120 avg=2.9`, Greedy top=`http://example.com/theme/engineering/verifiedBy/110 calls=2600 avg=2.918846153846154`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

