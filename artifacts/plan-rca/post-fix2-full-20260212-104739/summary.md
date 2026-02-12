# Plan RCA Summary

## ELECTRICAL_GRID #1

- observed: DP avgLast20ms=87, Greedy avgLast20ms=92, slowdown=-5.43%
- plan delta: DP stable=`http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/grid/name`, Greedy stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=439831 avg=0.2517307784126176`, Greedy top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=466450 avg=0.2493836424054025`
- likely bucket: 2 (bound-mask/runtime cost mismatch)
- strongest causal toggle evidence: not-run

## MEDICAL_RECORDS #6

- observed: DP avgLast20ms=69, Greedy avgLast20ms=70, slowdown=-1.43%
- plan delta: DP stable=`<no-join>`, Greedy stable=`<no-join>`
- top key deltas: DP top=`http://example.com/theme/medical/hasMedication/110 calls=208162 avg=2.0064709216859944`, Greedy top=`http://example.com/theme/medical/hasMedication/110 calls=208162 avg=2.0066919034213737`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## ENGINEERING #3

- observed: DP avgLast20ms=142, Greedy avgLast20ms=136, slowdown=4.41%
- plan delta: DP stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`http://example.com/theme/engineering/verifiedBy/110 calls=5850 avg=2.910940170940171`, Greedy top=`http://example.com/theme/engineering/verifiedBy/110 calls=5460 avg=2.917948717948718`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## PHARMA #3

- observed: DP avgLast20ms=15, Greedy avgLast20ms=15, slowdown=0.00%
- plan delta: DP stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`, Greedy stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`
- top key deltas: DP top=`http://example.com/theme/pharma/studiesDisease/110 calls=18049 avg=1.0`, Greedy top=`http://example.com/theme/pharma/responseRate/110 calls=18049 avg=1.0`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

