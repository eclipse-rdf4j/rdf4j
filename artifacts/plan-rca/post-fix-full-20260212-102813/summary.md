# Plan RCA Summary

## ENGINEERING #3

- observed: DP avgLast20ms=158, Greedy avgLast20ms=158, slowdown=0.00%
- plan delta: DP stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`http://example.com/theme/engineering/verifiedBy/110 calls=5980 avg=2.9081939799331105`, Greedy top=`http://example.com/theme/engineering/verifiedBy/110 calls=5460 avg=2.917948717948718`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## ELECTRICAL_GRID #1

- observed: DP avgLast20ms=84, Greedy avgLast20ms=90, slowdown=-6.67%
- plan delta: DP stable=`http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/grid/name`, Greedy stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=429111 avg=0.2493434099801683`, Greedy top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=467925 avg=0.2517497462200139`
- likely bucket: 2 (bound-mask/runtime cost mismatch)
- strongest causal toggle evidence: not-run

## PHARMA #3

- observed: DP avgLast20ms=20, Greedy avgLast20ms=19, slowdown=5.26%
- plan delta: DP stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`, Greedy stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`
- top key deltas: DP top=`http://example.com/theme/pharma/studiesDisease/110 calls=18049 avg=1.0`, Greedy top=`http://example.com/theme/pharma/responseRate/110 calls=18049 avg=1.0`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## MEDICAL_RECORDS #6

- observed: DP avgLast20ms=71, Greedy avgLast20ms=69, slowdown=2.90%
- plan delta: DP stable=`<no-join>`, Greedy stable=`<no-join>`
- top key deltas: DP top=`http://example.com/theme/medical/hasMedication/110 calls=208162 avg=2.0061250372306185`, Greedy top=`http://example.com/theme/medical/hasMedication/110 calls=208163 avg=2.006663047707806`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

