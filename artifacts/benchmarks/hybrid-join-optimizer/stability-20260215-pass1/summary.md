# Hybrid Join Optimizer Benchmark Summary

## Scenario

Single benchmark method executed in two optimizer modes with identical JMH settings.

## Results

| Mode | Avg | Median | P95 | P99 | Samples | Unit |
|---|---:|---:|---:|---:|---:|---|
| legacy | 7.807856 | 7.981470 | 8.707214 | 8.738141 | 10 | ms/op |
| hybrid | 8.227683 | 8.873921 | 9.098347 | 9.098645 | 10 | ms/op |

Hybrid vs legacy avg delta: 5.38% (negative is faster).

Planning-time share: not available in this JMH output. Use explain/profiling artifacts for planner split.

Raw artifacts: `legacy.log`, `hybrid.log`, `legacy.json`, `hybrid.json`, `metrics.csv`.
