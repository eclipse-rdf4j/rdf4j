# Optimizer planning

This README is an inventory pointer, not an ExecPlan. There is exactly one optimizer ExecPlan:
`GH-0000-frontier-estimator-cost-model-roadmap.md`. It contains all architecture, implementation, research,
verification, rollout, and performance work. Record future optimizer planning changes there; do not create another
ExecPlan. Git history is the archive for completed, consolidated, and superseded plans.

## Governing roadmap

`GH-0000-frontier-estimator-cost-model-roadmap.md` is the sole architectural and execution source of truth. Its
non-negotiable direction
is:

- preserve Frontier sketches, distributions, exact finite relations, intervals, lineage, and uncertainty through
  costing instead of reducing them to a scalar early;
- use one Cascades-style DPhyp search with exact continuation-equivalence classes and Pareto frontiers over multiple
  cost dimensions and physical properties;
- learn logical cardinality and physical residual costs from completed execution using a LEO-derived design that
  validates, scopes, versions, and safely applies observations;
- retain typed predicate ranges and other proven facts through the optimizer and use them to authorize only sound
  rewrites;
- maintain a complete, implementation-linked catalog of rewrites and optimizations with their exact safety
  conditions; and
- keep the optimizer's working representation packed, immutable, ID-based, and cheap to traverse, materializing a
  `TupleExpr` only for the selected plan.

## Consolidated into the sole plan

The following tracked documents were merged into the sole plan and deleted on 2026-08-06:

- `GH-0000-frontier-full-potential-roadmap.md`
- `GH-0000-frontier-state-continuity-cache-and-leo.md`
- `GH-0000-packed-idempotent-cascades.md`
- `GH-0000-learned-feedback-plan-flip-production.md`
- `GH-0000-research-learned-feedback-plan-flip.md`

Their completed detail remains in Git history. Every unresolved sketch, continuation/Pareto, DPhyp, predicate-range,
rewrite-catalog, learned-cost, rollout, corpus, and performance obligation is now in the sole plan. The packaged
learned-plan-flip research artifact at repository root remains supporting evidence.

## Removed from the active plan set

The cleanup on 2026-08-06 removed the following tracked ExecPlans. Their content remains recoverable from Git.

### Superseded or directionally incompatible

| Removed plan | Reason |
|---|---|
| `GH-0000-cascades-hypergraph-reference-hardening.md` | It declared itself superseded by the packed cutover and retained open work against deleted memo classes. |
| `GH-0000-lmdb-dphyp-hypergraph-join-planner.md` | Its v1 contract retained one multiplicative scalar row estimate per subset and a degenerate one-winner Pareto set. |
| `GH-0000-unified-next-steps.md` | It explicitly deferred Pareto retention and feedback to an optional later branch. |
| `GH-0000-unified-self-reactivating-query-optimizer.md` | Its object-memo implementation was deleted by the packed cutover; remaining tasks targeted that removed architecture. |
| `GH-0000-lmdb-architecture-closeout.md` | Its umbrella status and red gates were superseded by later green cutover and Frontier campaigns. |
| `GH-0000-lmdb-estimation-engine-rewrite.md` | Its cutover finished; its remaining measurement work was absorbed and then overtaken by Frontier-specific plans. |
| `GH-0000-frontier-query-index.md` | Its unchecked verification/benchmark boxes were completed and recorded by the later Frontier roadmap. |

### Completed implementation records

The following plans had no open implementation work and are historical evidence rather than living instructions:

- `GH-0000-concurrent-maven-workspaces.md`
- `GH-0000-explain-provenance-and-plan-drift.md`
- `GH-0000-frontier-evidence-fusion-and-shape-coverage.md`
- `GH-0000-frontier-omnisketch-rdf4j.md`
- `GH-0000-frontier-physical-cost-and-semi-anti-planning.md`
- `GH-0000-lmdb-bounded-cold-evidence.md`
- `GH-0000-lmdb-omni-p1-p2-remediation.md`
- `GH-0000-lmdb-omni-surface-hard-cut.md`
- `GH-0000-lmdb-omni-vertex-witness-bridges.md`
- `GH-0000-lmdb-planner-god-class-split.md`
- `GH-0000-or-values-memo-local.md`
- `GH-0000-shared-hash-key-contract-and-costing.md`

## Other planning-shaped documents

- `hypergraph-plan.md` is a non-governing MySQL reference. Its scalar formulas and heuristic degradation examples do
  not override the governing state-preservation and exact-frontier contracts.
- `optimizer-rca-and-roadmap-2026-07-07.md` is a historical RCA. Its old roadmap is superseded, especially the
  requirement that estimators return only one `PlanCost` value.
- `papers2/papers/docs/sparql_query_rewrite_catalog.md` is a useful literature seed, but not the required complete
  repository rewrite/optimization catalog.
- `papers2/papers/docs/neumann-birler-umbra-query-optimizer-research.md` is the aligned research synthesis behind the
  roadmap's DPhyp/CD-E, estimator-state, cost-vector, LEO-plus, rewrite-safety, and working-representation decisions;
  it is evidence and rationale, not a second ExecPlan.
- `profiles/lmdb-opt/theme-audit-2026-08-04/` contains untracked design/audit evidence. In particular, its exact
  continuation-frontier, DPhyp subset-kernel, planner-invariant, group-cardinality, and rewrite-semantics documents
  are aligned inputs. They are evidence artifacts, not competing governing plans, and this cleanup does not modify
  them.
