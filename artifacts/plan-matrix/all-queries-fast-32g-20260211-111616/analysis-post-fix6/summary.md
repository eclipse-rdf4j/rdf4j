# Plan Matrix Summary

- Total comparisons: 176
- Faster or equal vs legacy: 88
- Slower vs legacy: 88
- Geomean speedup (legacy/runtime): 1.2077x
- Regressions above 20.0%: 24

## Winner Counts
- DP_TOP1: 20
- DP_TOP2: 29
- LEGACY: 39

## Worst Regressions
- LIBRARY#8 (COLD_ISOLATED DP_TOP2 seed=-1): 1060.63% (high_uncertainty)
- ELECTRICAL_GRID#2 (COLD_ISOLATED DP_TOP2 seed=-1): 518.56% (high_uncertainty)
- ELECTRICAL_GRID#2 (COLD_ISOLATED DP_TOP1 seed=-1): 503.84% (high_uncertainty)
- PHARMA#6 (COLD_ISOLATED DP_TOP2 seed=-1): 476.79% (high_uncertainty)
- LIBRARY#8 (COLD_ISOLATED DP_TOP1 seed=-1): 432.68% (high_uncertainty)
- LIBRARY#7 (COLD_ISOLATED DP_TOP1 seed=-1): 129.77% (high_uncertainty)
- LIBRARY#7 (COLD_ISOLATED DP_TOP2 seed=-1): 126.15% (high_uncertainty)
- HIGHLY_CONNECTED#8 (COLD_ISOLATED DP_TOP1 seed=-1): 102.48% (high_uncertainty)
- HIGHLY_CONNECTED#8 (COLD_ISOLATED DP_TOP2 seed=-1): 102.02% (high_uncertainty)
- ENGINEERING#5 (COLD_ISOLATED DP_TOP2 seed=-1): 82.47% (missing_estimate)
- TRAIN#1 (COLD_ISOLATED DP_TOP2 seed=-1): 64.09% (high_uncertainty)
- TRAIN#1 (COLD_ISOLATED DP_TOP1 seed=-1): 62.24% (high_uncertainty)
- ENGINEERING#1 (COLD_ISOLATED DP_TOP1 seed=-1): 57.51% (high_uncertainty)
- ENGINEERING#1 (COLD_ISOLATED DP_TOP2 seed=-1): 55.87% (high_uncertainty)
- LIBRARY#9 (COLD_ISOLATED DP_TOP1 seed=-1): 54.25% (high_uncertainty)
- ENGINEERING#8 (COLD_ISOLATED DP_TOP1 seed=-1): 37.92% (high_uncertainty)
- LIBRARY#9 (COLD_ISOLATED DP_TOP2 seed=-1): 34.63% (high_uncertainty)
- ENGINEERING#3 (COLD_ISOLATED DP_TOP1 seed=-1): 28.89% (high_uncertainty)
- PHARMA#7 (COLD_ISOLATED DP_TOP2 seed=-1): 26.54% (high_uncertainty)
- PHARMA#4 (COLD_ISOLATED DP_TOP2 seed=-1): 25.06% (high_uncertainty)

## Plan Signature Split
- same-plan-but-slower: 2
- different-plan-slower: 2
- unknown-signature: 20
