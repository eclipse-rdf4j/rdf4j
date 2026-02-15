# Hybrid Join Optimizer Benchmark Summary

## Scenario

Single benchmark method executed in two optimizer modes with identical JMH settings.

## Results

| Mode | Avg | Median | P95 | P99 | Samples | Unit |
|---|---:|---:|---:|---:|---:|---|
| legacy | 7.746145 | 7.986161 | 8.997158 | 9.054099 | 10 | ms/op |
| hybrid | 7.953331 | 8.782430 | 8.980440 | 9.006357 | 10 | ms/op |

Hybrid vs legacy avg delta: 2.67% (negative is faster).

Planning-time share: not available in this JMH output. Use explain/profiling artifacts for planner split.

Raw artifacts: `legacy.log`, `hybrid.log`, `legacy.json`, `hybrid.json`, `metrics.csv`.
