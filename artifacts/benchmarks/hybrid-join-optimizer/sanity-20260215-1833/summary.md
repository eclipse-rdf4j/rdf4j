# Hybrid Join Optimizer Benchmark Summary

## Scenario

Single benchmark method executed in two optimizer modes with identical JMH settings.

## Results

| Mode | Avg | Median | P95 | P99 | Samples | Unit |
|---|---:|---:|---:|---:|---:|---|
| legacy | 8.676613 | 8.669169 | 8.999922 | 9.034825 | 5 | ms/op |
| hybrid | 9.027879 | 9.053286 | 9.139225 | 9.141976 | 5 | ms/op |

Hybrid vs legacy avg delta: 4.05% (negative is faster).

Planning-time share: not available in this JMH output. Use explain/profiling artifacts for planner split.

Raw artifacts: `legacy.log`, `hybrid.log`, `legacy.json`, `hybrid.json`, `metrics.csv`.
