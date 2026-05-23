# LMDB Catalog Rewrite Sweep

This ExecPlan is a living document. Keep `Progress`, `Discoveries`, `Decision Log`, and `Outcomes`
current as implementation proceeds.

## Purpose

Implement the remaining guarded LMDB catalog rewrite families as planner-visible alternatives where cost
selection matters, and as small pre-sketch normalizers where a rewrite is purely local. The intended result is
that the cascade memo and Pareto front can cost more useful algebra shapes without committing too early to
unsafe bag-semantics rewrites.

## Progress

- [x] Captured initial root quick-install evidence in `initial-evidence.txt`.
- [x] Confirmed current branch: `GH-0000-lmdb-predicate-guarantees`.
- [x] Recorded existing partial base: `FLBndI` and `LmdbBoundSimplifierOptimizer`.
- [ ] Harden `FLBndI` and pipeline ordering with guard regressions.
- [ ] Add conservative projection pushdown normalizer.
- [ ] Add cost-gated OPTIONAL RHS anchoring alternative.
- [ ] Add guarded OPTIONAL/UNION distribution alternatives.
- [ ] Add guarded MINUS alternatives.
- [ ] Add set-semantics, ASK-safe, and equality coverage.
- [ ] Run focused optimizer gate, hygiene, and theme benchmarks.

## Discoveries

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/ThemeQueryBenchmark.java`
  already has query indices `0` through `6` commented out. Treat this as pre-existing work and do not
  revert it. Benchmark coverage must either pass explicit parameters or report the active benchmark subset.
- `FLBndI` currently exists both as a direct `Filter(LeftJoin)` rewrite and as a planner-facing segment
  alternative. The planner-facing path is the architecture to preserve.
- The BOUND simplifier is already wired before `LmdbFilterSimplifierOptimizer`.

## Decision Log

- Use Routine D for the whole sweep because it is a significant optimizer feature set.
- Use Routine A inside each behavior-changing phase: add a focused failing regression before production edits.
- Keep unsafe catalog identities disabled unless under `Distinct` or ASK-shaped existence semantics.
- Prefer physical/planning alternatives in `LmdbSketchJoinOptimizer` for OPTIONAL and MINUS rewrites that need
  cost model access.

## Outcomes

To be filled after implementation and benchmark analysis.

## Context

Primary files:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSketchJoinOptimizer.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbQueryOptimizerPipeline.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbUnionFilterDistributor.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBoundSimplifierOptimizer.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/GuaranteePlanOptionProvider.java`

Existing test anchors:

- `LmdbSketchJoinOptimizerOptionalAntiJoinRewriteTest`
- `LmdbBoundSimplifierOptimizerTest`
- `LmdbOptimizerPipelineTest`
- `LmdbUnionFilterDistributorTest`
- `LmdbSketchJoinOptimizerOptionalRewriteTest`

## Plan of Work

1. Harden `FLBndI` and BOUND simplification.
   Add guard regressions for unshared variables, RHS `Extension`, RHS `Service`, RHS `Group`, and nested
   `EXISTS`. Add a pipeline-order regression proving BOUND simplification runs before filter simplification
   and sketch planning.

2. Add `LmdbProjectionPushdownOptimizer`.
   Support identity projections only. Push through `Filter`, `Join`, `Union`, `Difference`, and `LeftJoin`.
   Retain projected names, filter names, shared join names, optional condition names, and MINUS compatibility
   names. Merge nested identity projections. Skip aliases, aggregate projection elems, scope-changing nodes,
   ordering, slices, groups, services, extensions, and multi-projections.

3. Add OPTIONAL RHS anchoring.
   During `LeftJoin` planning, compare unanchored RHS planning with a physical anchored RHS plan that treats
   left-side assured/shared bindings as available lookup bindings. Select only when cost improves and output
   bindings/multiplicity remain compatible.

4. Add OPTIONAL/UNION distribution.
   Add left-union distribution when branches are binding-compatible and clone cost is acceptable. Add RHS-union
   support only for mutually exclusive branches with a conservative discriminator proof.

5. Add MINUS alternatives.
   Add a small provider for chained MINUS reorder, union distribution, repeated RHS combine, and physical
   bound-RHS planning. Require independence, clone safety, and cost wins as applicable.

6. Add set-semantics, ASK, and equality coverage.
   Under `Distinct` or ASK-shaped `QueryRoot -> Slice(limit=1)`, allow guarded idempotence and self-difference
   rewrites for deterministic pure subtrees. Add ASK-safe optional/union rewrites where existence semantics
   are preserved. Add equality regression coverage before extending production equality elimination.

## Concrete Steps

Run baseline quick install before each test group:

```bash
mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200
```

Focused hardening gate:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbOptimizerPipelineTest,LmdbSketchJoinOptimizerOptionalAntiJoinRewriteTest,LmdbBoundSimplifierOptimizerTest test
```

Final optimizer gate:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LmdbOptimizerPipelineTest,LmdbSketchJoinOptimizerOptionalRewriteTest,LmdbSketchJoinOptimizerOptionalAntiJoinRewriteTest,LmdbBoundSimplifierOptimizerTest,LmdbProjectionPushdownOptimizerTest,LmdbUnionFilterDistributorTest,LmdbSketchJoinOptimizerMinusRewriteTest,LmdbSetSemanticsOptimizerTest test
```

Hygiene:

```bash
mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
./checkCopyrightPresent.sh
git diff --check
```

Theme benchmark:

```bash
scripts/run-single-benchmark.sh --module core/sail/lmdb --class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark --method executeQuery --warmup-iterations 3 --measurement-iterations 5 --measurement-time 5s --forks 1
```

## Validation

Each behavior-changing phase gets:

- failing focused test evidence before production changes,
- post-fix focused green evidence,
- module optimizer gate after related phases,
- final hygiene,
- final theme benchmark comparison.

If a theme query regresses by more than 20%, capture query-plan snapshots for that theme/query and compare
structure plus estimates before further code changes.

## Idempotence

All rewrites must be safe to re-run. Projection pushdown should merge nested projections and skip unsupported
nodes rather than producing deeper trees each pass. Planner alternatives must avoid re-adding equivalent
alternatives by structural checks or existing memo identity.

## Artifacts

- Initial build evidence: `initial-evidence.txt`
- Benchmark output target: `/tmp/theme-query-final.txt`
- Theme history scripts:
  - `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/theme-query-benchmark-results.sh`
  - `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh`

## Interfaces

Expected new classes:

- `org.eclipse.rdf4j.sail.lmdb.LmdbProjectionPushdownOptimizer`
- `org.eclipse.rdf4j.sail.lmdb.LmdbSetSemanticsOptimizer`

Expected new or extended tests:

- `LmdbProjectionPushdownOptimizerTest`
- `LmdbSketchJoinOptimizerMinusRewriteTest`
- `LmdbSetSemanticsOptimizerTest`
- extensions to `LmdbSketchJoinOptimizerOptionalAntiJoinRewriteTest`
- extensions to `LmdbUnionFilterDistributorTest`
