# Plan RCA Summary

## ELECTRICAL_GRID #1

- observed: DP avgLast20ms=72, Greedy avgLast20ms=88, slowdown=-18.18%
- plan delta: DP stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/grid/feeds -> http://example.com/theme/grid/name`, Greedy stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`http://example.com/theme/grid/feeds/110 calls=56208 avg=0.9998932536293766`, Greedy top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=223884 avg=0.2493434099801683`
- likely bucket: 1 (intermediate explosion from estimate miss)
- strongest causal toggle evidence: not-run

