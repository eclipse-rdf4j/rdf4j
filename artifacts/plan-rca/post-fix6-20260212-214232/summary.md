# Plan RCA Summary

## ENGINEERING #3

- observed: DP avgLast20ms=132, Greedy avgLast20ms=124, slowdown=6.45%
- plan delta: DP stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`
- top key deltas: DP top=`http://example.com/theme/engineering/verifiedBy/110 calls=3900 avg=2.9`, Greedy top=`http://example.com/theme/engineering/verifiedBy/110 calls=2860 avg=2.9342657342657343`
- likely bucket: 2 (bound-mask/runtime cost mismatch)
- strongest causal toggle evidence: not-run

## MEDICAL_RECORDS #6

- observed: DP avgLast20ms=60, Greedy avgLast20ms=59, slowdown=1.69%
- plan delta: DP stable=`<no-join>`, Greedy stable=`<no-join>`
- top key deltas: DP top=`http://example.com/theme/medical/hasMedication/110 calls=124897 avg=2.006109033843887`, Greedy top=`http://example.com/theme/medical/hasMedication/110 calls=124898 avg=2.0070057166647985`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## PHARMA #3

- observed: DP avgLast20ms=15, Greedy avgLast20ms=15, slowdown=0.00%
- plan delta: DP stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`, Greedy stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`
- top key deltas: DP top=`urn:rdf4j:learned:rdfType:http://example.com/theme/pharma/ClinicalTrial/111 calls=10826 avg=1.0`, Greedy top=`http://example.com/theme/pharma/responseRate/110 calls=10840 avg=1.0`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## ELECTRICAL_GRID #1

- observed: DP avgLast20ms=77, Greedy avgLast20ms=81, slowdown=-4.94%
- plan delta: DP stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=243321 avg=0.2517497462200139`, Greedy top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=280755 avg=0.2517497462200139`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

