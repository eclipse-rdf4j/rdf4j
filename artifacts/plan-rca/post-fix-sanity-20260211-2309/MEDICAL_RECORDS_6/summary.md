# Plan RCA Summary

## MEDICAL_RECORDS #6

- observed: DP avgLast20ms=64, Greedy avgLast20ms=59, slowdown=8.47%
- plan delta: DP stable=`<no-join>`, Greedy stable=`<no-join>`
- top key deltas: DP top=`http://example.com/theme/medical/hasMedication/110 calls=208162 avg=2.0066919034213737`, Greedy top=`http://example.com/theme/medical/hasMedication/110 calls=208163 avg=2.0064132434678594`
- likely bucket: 6 (DP overhead/instability dominates)
- strongest causal toggle evidence: not-run

