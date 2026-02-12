# Plan RCA Summary

## ENGINEERING #3

- observed: DP avgLast20ms=157, Greedy avgLast20ms=144, slowdown=9.03%
- plan delta: DP stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`, Greedy stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`
- top key deltas: DP top=`http://example.com/theme/engineering/satisfies/110 calls=3250 avg=1.0`, Greedy top=`http://example.com/theme/engineering/satisfies/110 calls=3250 avg=1.0`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

