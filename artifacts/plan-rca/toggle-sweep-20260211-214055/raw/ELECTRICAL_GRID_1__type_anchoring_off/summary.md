# Plan RCA Summary

## ELECTRICAL_GRID #1

- observed: DP avgLast20ms=112, Greedy avgLast20ms=95, slowdown=17.89%
- plan delta: DP stable=`http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/grid/name`, Greedy stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=187142 avg=0.2516805420482842`, Greedy top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=223896 avg=0.2493836424054025`
- likely bucket: 2 (bound-mask/runtime cost mismatch)
- strongest causal toggle evidence: not-run

