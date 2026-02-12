# Plan Matrix Summary

- Total comparisons: 528
- Faster or equal vs legacy: 342
- Slower vs legacy: 186
- Geomean speedup (legacy/runtime): 1.3963x
- Regressions above 20.0%: 67

## Winner Counts
- DP_TOP1: 44
- DP_TOP2: 35
- DP_TOP3: 54
- LEGACY: 43

## Worst Regressions
- LIBRARY#8 (WARM_SHARED DP_TOP3 seed=7): 791.80% (high_uncertainty)
- LIBRARY#8 (WARM_SHARED DP_TOP2 seed=7): 790.58% (high_uncertainty)
- LIBRARY#8 (COLD_ISOLATED DP_TOP2 seed=-1): 437.14% (high_uncertainty)
- LIBRARY#8 (COLD_ISOLATED DP_TOP3 seed=-1): 431.47% (high_uncertainty)
- SOCIAL_MEDIA#3 (WARM_SHARED DP_TOP2 seed=7): 395.28% (missing_estimate)
- PHARMA#6 (WARM_SHARED DP_TOP3 seed=7): 353.96% (high_uncertainty)
- LIBRARY#4 (WARM_SHARED DP_TOP3 seed=7): 347.19% (missing_estimate)
- PHARMA#6 (WARM_SHARED DP_TOP2 seed=7): 336.72% (high_uncertainty)
- ELECTRICAL_GRID#2 (WARM_SHARED DP_TOP2 seed=7): 306.51% (high_uncertainty)
- LIBRARY#8 (WARM_SHARED DP_TOP1 seed=7): 304.99% (high_uncertainty)
- ELECTRICAL_GRID#2 (WARM_SHARED DP_TOP3 seed=7): 301.59% (high_uncertainty)
- ELECTRICAL_GRID#2 (WARM_SHARED DP_TOP1 seed=7): 299.98% (high_uncertainty)
- ELECTRICAL_GRID#2 (COLD_ISOLATED DP_TOP3 seed=-1): 297.75% (high_uncertainty)
- ELECTRICAL_GRID#2 (COLD_ISOLATED DP_TOP1 seed=-1): 242.06% (high_uncertainty)
- ELECTRICAL_GRID#2 (COLD_ISOLATED DP_TOP2 seed=-1): 219.97% (high_uncertainty)
- PHARMA#6 (COLD_ISOLATED DP_TOP2 seed=-1): 209.83% (high_uncertainty)
- PHARMA#6 (COLD_ISOLATED DP_TOP3 seed=-1): 194.90% (high_uncertainty)
- LIBRARY#3 (WARM_SHARED DP_TOP2 seed=7): 167.51% (high_uncertainty)
- SOCIAL_MEDIA#6 (WARM_SHARED DP_TOP1 seed=7): 149.79% (missing_estimate)
- LIBRARY#8 (COLD_ISOLATED DP_TOP1 seed=-1): 141.09% (high_uncertainty)

## Plan Signature Split
- same-plan-but-slower: 12
- different-plan-slower: 0
- unknown-signature: 55
