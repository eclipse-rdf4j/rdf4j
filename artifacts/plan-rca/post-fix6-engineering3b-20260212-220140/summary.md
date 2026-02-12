# Plan RCA Summary

## ENGINEERING #3

- observed: DP avgLast20ms=179, Greedy avgLast20ms=150, slowdown=19.33%
- plan delta: DP stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`
- top key deltas: DP top=`http://example.com/theme/engineering/verifiedBy/110 calls=3380 avg=2.9144970414201183`, Greedy top=`http://example.com/theme/engineering/verifiedBy/110 calls=2860 avg=2.9342657342657343`
- likely bucket: 2 (bound-mask/runtime cost mismatch)
- strongest causal toggle evidence: not-run

