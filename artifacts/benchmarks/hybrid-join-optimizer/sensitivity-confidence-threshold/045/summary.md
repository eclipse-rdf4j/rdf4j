# Hybrid Join Optimizer Benchmark Summary

## Scenario

Single benchmark method executed in two optimizer modes with identical JMH settings.

## Results

| Mode | Avg | Median | P95 | P99 | Samples | Unit |
|---|---:|---:|---:|---:|---:|---|
| legacy | 6.843909 | 7.025952 | 7.929080 | 8.004494 | 10 | ms/op |
| hybrid | 7.213447 | 7.266405 | 8.533059 | 8.617575 | 10 | ms/op |

Hybrid vs legacy avg delta: 5.40% (negative is faster).

Planning-time share: not available in this JMH output. Use explain/profiling artifacts for planner split.

Raw artifacts: `legacy.log`, `hybrid.log`, `legacy.json`, `hybrid.json`, `metrics.csv`.
