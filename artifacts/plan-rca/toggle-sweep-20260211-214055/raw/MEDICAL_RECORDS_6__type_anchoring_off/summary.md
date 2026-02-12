# Plan RCA Summary

## MEDICAL_RECORDS #6

- observed: DP avgLast20ms=87, Greedy avgLast20ms=65, slowdown=33.85%
- plan delta: DP stable=`<no-join>`, Greedy stable=`<no-join>`
- top key deltas: DP top=`http://example.com/theme/medical/hasMedication/110 calls=99918 avg=2.0064552933405393`, Greedy top=`http://example.com/theme/medical/hasMedication/110 calls=99918 avg=2.0064552933405393`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

