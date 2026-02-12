# Plan RCA Summary

## ENGINEERING #3

- observed: DP avgLast20ms=162, Greedy avgLast20ms=136, slowdown=19.12%
- plan delta: DP stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`, Greedy stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`http://example.com/theme/engineering/satisfies/110 calls=1560 avg=1.0`, Greedy top=`http://example.com/theme/engineering/verifiedBy/110 calls=3120 avg=2.9`
- likely bucket: 2 (bound-mask/runtime cost mismatch)
- strongest causal toggle evidence: not-run

