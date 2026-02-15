# Hybrid Join Optimizer Benchmark Summary

## Scenario

Single benchmark method executed in two optimizer modes with identical JMH settings.

## Results

| Mode | Avg | Median | P95 | P99 | Samples | Unit |
|---|---:|---:|---:|---:|---:|---|
| legacy | 8.571300 | 8.466149 | 8.828764 | 8.860996 | 3 | ms/op |
| hybrid | 8.836064 | 8.978302 | 9.024747 | 9.028876 | 3 | ms/op |

Hybrid vs legacy avg delta: 3.09% (negative is faster).

Planning-time share: not available in this JMH output. Use explain/profiling artifacts for planner split.

Raw artifacts: `legacy.log`, `hybrid.log`, `legacy.json`, `hybrid.json`, `metrics.csv`.
