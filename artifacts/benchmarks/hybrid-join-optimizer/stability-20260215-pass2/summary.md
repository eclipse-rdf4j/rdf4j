# Hybrid Join Optimizer Benchmark Summary

## Scenario

Single benchmark method executed in two optimizer modes with identical JMH settings.

## Results

| Mode | Avg | Median | P95 | P99 | Samples | Unit |
|---|---:|---:|---:|---:|---:|---|
| legacy | 7.736326 | 7.973801 | 8.718960 | 8.807230 | 10 | ms/op |
| hybrid | 8.230408 | 8.644463 | 9.290227 | 9.439168 | 10 | ms/op |

Hybrid vs legacy avg delta: 6.39% (negative is faster).

Planning-time share: not available in this JMH output. Use explain/profiling artifacts for planner split.

Raw artifacts: `legacy.log`, `hybrid.log`, `legacy.json`, `hybrid.json`, `metrics.csv`.
