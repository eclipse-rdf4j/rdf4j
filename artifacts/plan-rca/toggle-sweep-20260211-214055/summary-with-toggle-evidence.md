# RCA Toggle Sweep Summary

## PHARMA #3

- baseline slowdown: 45.00% (DP vs Greedy)
- strongest causal toggle evidence: `scan_cost_off` => slowdown 38.10% (delta +6.90 pp, improves)
- plan delta under best toggle: DP=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease` | Greedy=`http://example.com/theme/pharma/hasResult -> http://example.com/theme/pharma/hasArm -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/pharma/studiesDisease`

## ELECTRICAL_GRID #1

- baseline slowdown: -14.13% (DP vs Greedy)
- strongest causal toggle evidence: `tie_break_off` => slowdown -21.88% (delta +7.74 pp, improves)
- plan delta under best toggle: DP=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/grid/feeds -> http://example.com/theme/grid/name` | Greedy=`http://example.com/theme/grid/name -> http://example.com/theme/grid/feeds -> http://www.w3.org/1999/02/22-rdf-syntax-ns#type`

## ENGINEERING #3

- baseline slowdown: 21.58% (DP vs Greedy)
- strongest causal toggle evidence: `rebalance_unsupportedTargetTypeTriplet_off` => slowdown 12.84% (delta +8.74 pp, improves)
- plan delta under best toggle: DP=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies` | Greedy=`http://www.w3.org/1999/02/22-rdf-syntax-ns#type -> http://example.com/theme/engineering/satisfies`

## MEDICAL_RECORDS #6

- baseline slowdown: 35.48% (DP vs Greedy)
- strongest causal toggle evidence: `scan_cost_off` => slowdown 32.81% (delta +2.67 pp, improves)
- plan delta under best toggle: DP=`<no-join>` | Greedy=`<no-join>`

