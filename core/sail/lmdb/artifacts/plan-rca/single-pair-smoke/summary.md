# Plan RCA Summary

## PHARMA #3

- observed: DP avgLast20ms=19, Greedy avgLast20ms=18, slowdown=5.56%
- plan delta: DP stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`, Greedy stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`
- top key deltas: DP top=`http://example.com/theme/pharma/armDrug/110 calls=18049 avg=1.0`, Greedy top=`http://example.com/theme/pharma/responseRate/110 calls=18045 avg=1.0`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## MEDICAL_RECORDS #6

- observed: DP avgLast20ms=65, Greedy avgLast20ms=65, slowdown=0.00%
- plan delta: DP stable=`<no-join>`, Greedy stable=`<no-join>`
- top key deltas: DP top=`http://example.com/theme/medical/hasMedication/110 calls=208163 avg=2.0062451059986643`, Greedy top=`http://example.com/theme/medical/hasMedication/110 calls=208162 avg=2.0064709216859944`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## ENGINEERING #3

- observed: DP avgLast20ms=150, Greedy avgLast20ms=150, slowdown=0.00%
- plan delta: DP stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`, Greedy stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`
- top key deltas: DP top=`http://example.com/theme/engineering/satisfies/110 calls=3250 avg=1.0`, Greedy top=`http://example.com/theme/engineering/satisfies/110 calls=3250 avg=1.0`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## ELECTRICAL_GRID #1

- observed: DP avgLast20ms=57, Greedy avgLast20ms=90, slowdown=-36.67%
- plan delta: DP stable=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/grid/feeds -> http://example.com/theme/grid/name`, Greedy stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`http://example.com/theme/grid/feeds/110 calls=117099 avg=0.9998975226090744`, Greedy top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=457107 avg=0.24941862627349834`
- likely bucket: 1 (intermediate explosion from estimate miss)
- strongest causal toggle evidence: not-run

