
# Sketch-based Join Order Optimizer

Files:
- `JoinEstimator.java`: DataSketches-backed sketch algebra and synopsis model.
- `JoinOrderOptimizer.java`: exact DP optimizer plus greedy fallback.

Dependency (current Maven Central release):
```xml
<dependency>
  <groupId>org.apache.datasketches</groupId>
  <artifactId>datasketches-java</artifactId>
  <version>9.0.0</version>
</dependency>
```

The source uses the on-heap tuple-sketch APIs from
`org.apache.datasketches.tuple.arrayofdoubles`.

Core assumption:
- base statement-pattern synopses are materialized offline;
- every retained sketch hash can be mapped back to the original term id by a `SketchKeyResolver`;
- the exact DP is for connected, acyclic, unary/binary statement-pattern queries where each extension shares exactly one variable with the current connected prefix.

Typical flow:
1. Build `PatternSynopsis` objects offline using `PatternSynopsisBuilder`.
2. Register them in `InMemorySynopsisCatalog`.
3. Provide a `SketchKeyResolver` that resolves retained sketch hashes to original term ids.
4. Create `JoinEstimator`.
5. Run `JoinOrderOptimizer.optimizeDp(...)`.
