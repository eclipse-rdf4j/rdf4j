# Plan RCA Summary

## PHARMA #3

- observed: DP avgLast20ms=18, Greedy avgLast20ms=17, slowdown=5.88%
- plan delta: DP stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`, Greedy stable=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`
- top key deltas: DP top=`http://example.com/theme/pharma/armDrug/110 calls=14425 avg=1.0`, Greedy top=`http://example.com/theme/pharma/armDrug/110 calls=14425 avg=1.0`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## MEDICAL_RECORDS #6

- observed: DP avgLast20ms=71, Greedy avgLast20ms=69, slowdown=2.90%
- plan delta: DP stable=`<no-join>`, Greedy stable=`<no-join>`
- top key deltas: DP top=`http://example.com/theme/medical/hasMedication/110 calls=166530 avg=2.0064552933405393`, Greedy top=`http://example.com/theme/medical/hasMedication/110 calls=166530 avg=2.0064552933405393`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## ENGINEERING #3

- observed: DP avgLast20ms=161, Greedy avgLast20ms=156, slowdown=3.21%
- plan delta: DP stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://example.com/theme/engineering/satisfies -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`http://example.com/theme/engineering/verifiedBy/110 calls=4680 avg=2.9104700854700853`, Greedy top=`http://example.com/theme/engineering/verifiedBy/110 calls=4680 avg=2.9104700854700853`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

## ELECTRICAL_GRID #1

- observed: DP avgLast20ms=93, Greedy avgLast20ms=91, slowdown=2.20%
- plan delta: DP stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`, Greedy stable=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`
- top key deltas: DP top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=364888 avg=0.2516662647168446`, Greedy top=`urn:rdf4j:learned:rdfType:http://example.com/theme/grid/Generator/111 calls=374263 avg=0.2515958029514005`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

