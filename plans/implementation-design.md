# RDF4J LMDB Join Order Optimizer — Implementation Design

**Document type:** Implementation design  
**Target module:** `core/sail/lmdb`  
**Target codebase:** Eclipse RDF4J LMDB store  
**Status:** Ready for implementation  
**Audience:** RDF4J maintainers and engineers implementing the LMDB-specific optimizer

---

## 1. Purpose

This document defines the implementation design for an **LMDB-specific SPARQL join order optimizer** in RDF4J.

The optimizer must make join-order decisions using:

1. the **actual LMDB index set** configured for the store,
2. the **variables that are already bound** at each point in the join plan,
3. the difference between **compile-time constants** and **runtime-bound values**, and
4. whether an access path can also provide a **useful output order** for a merge join.

The design is intentionally scoped to the RDF4J LMDB store and does **not** modify RDF4J’s generic optimizer behavior for other stores.

---

## 2. Problem statement

Today, LMDB already makes an index-aware decision at **statement retrieval time**, but not at **join-order planning time**.

### Current behavior summary

- `TripleStore` already selects the best runtime index using `getBestIndex(...)`.
- `TripleIndex.getPatternScore(...)` already measures usable **leading bound prefix length**.
- `LmdbEvaluationStatistics` estimates statement cardinality from **compile-time constants only**.
- The default optimizer pipeline still uses the generic `QueryJoinOptimizer`, which:
  - tracks which variables are bound,
  - but does **not** cost specific LMDB access paths,
  - does **not** reason about configured LMDB indexes,
  - and does **not** account for runtime-bound variables as indexable prefix bindings.
- `LmdbSailStore.LmdbSailDataset` does **not** currently implement:
  - `getStatements(StatementOrder, ...)`
  - `getSupportedOrders(...)`
  - `getComparator()`

### Consequence

The query planner can choose an order that is logically plausible but physically poor for LMDB.  
That creates avoidable full scans, poor nested-loop probe patterns, and missed opportunities for order-preserving access and merge join.

---

## 3. Goals

### 3.1 Functional goals

The implementation **shall**:

1. Replace the generic join ordering logic for LMDB with an LMDB-aware optimizer.
2. Plan statement-pattern joins using the **configured LMDB indexes**.
3. Distinguish:
   - compile-time constants,
   - runtime-bound variables,
   - unbound variables.
4. Choose access paths using **leading-prefix usability** over the real LMDB index set.
5. Estimate both:
   - **rows scanned per probe**, and
   - **rows returned per probe**.
6. Prefer plans that avoid repeated high-cost scans.
7. Expose statement orders that LMDB can actually produce without pathological downgrade.
8. Enable merge join when:
   - both sides can produce the same useful order,
   - the order does not require a harmful access-path downgrade,
   - and the size ratio is reasonable.
9. Fall back safely to existing behavior when the LMDB-specific path is unavailable or unsupported.

### 3.2 Engineering goals

The implementation should:

1. Minimize changes outside `core/sail/lmdb`.
2. Preserve RDF4J’s existing optimizer pipeline structure.
3. Preserve correctness first; performance improvements must never change query results.
4. Be testable at:
   - unit level,
   - integration level,
   - explain/plan level,
   - benchmark level.

---

## 4. Non-goals

This project does **not** include:

1. a generic optimizer rewrite for all RDF4J stores,
2. adaptive runtime re-optimization mid-query,
3. an automatic index advisor that creates or drops indexes,
4. bushy join enumeration for the first implementation,
5. broad optimizer changes for property paths, SERVICE, or non-statement tuple expressions,
6. exact selectivity modeling for every skew pattern.

Those may be added later, but they are not required for this delivery.

---

## 5. Design principles

### 5.1 Physical planning, not logical-only planning

Join order must be chosen as a function of:

- current bound variables,
- candidate access path,
- expected probe cost,
- expected output rows,
- and available order.

The planner must optimize **`(current subplan, next statement pattern, access path)`**, not just statement-pattern order.

### 5.2 Runtime-bound variables are physically meaningful

If a variable becomes bound by an earlier join, then LMDB can often use it as a prefix key at execution time even though the value is not known at compile time.

This is the central planning distinction:

- `CONST` = value known at optimization time
- `BOUND` = value unknown now, but guaranteed to be bound when this statement pattern executes
- `UNBOUND` = value not yet bound

### 5.3 Prefix quality dominates index usability

For an LMDB index with field sequence like `spoc`, the best access paths are those that can bind the longest consecutive prefix from the start of the index.  
A later fixed field does not compensate for an earlier unbound field.

### 5.4 Order is secondary to prefix quality

An access path that provides a merge-join-friendly order is useful **only if** it does not destroy prefix quality.  
The optimizer must not choose an ordered full scan over a good prefix range scan.

### 5.5 Fallback over fragility

If a join group is outside the intended scope, the implementation must degrade to RDF4J’s current behavior instead of producing a partially correct but unstable plan.

---

## 6. Target architecture

```mermaid
flowchart TD
    A[SPARQL TupleExpr] --> B[LmdbEvaluationStrategyFactory]
    B --> C[LmdbQueryOptimizerPipeline]
    C --> D[LmdbJoinOrderOptimizer]

    D --> E[LmdbOptimizerStatisticsManager]
    D --> F[TripleStore index catalog]
    D --> G[LmdbEvaluationStatistics exact constant cardinality]

    D --> H[Chosen join order + access-path metadata]
    H --> I[Evaluation / execution]

    I --> J[LmdbSailDataset.getStatements(order,...)]
    J --> K[TripleStore ordered or unordered iterator]
    K --> L[IndexReportingIterator / actual index name]
```

### Core components

1. **`LmdbEvaluationStrategyFactory`**  
   LMDB-specific factory that injects an LMDB-specific optimizer pipeline by default.

2. **`LmdbQueryOptimizerPipeline`**  
   Same optimizer sequence as the standard pipeline, except generic `QueryJoinOptimizer` is replaced with `LmdbJoinOrderOptimizer`.

3. **`LmdbJoinOrderOptimizer`**  
   Main planner for LMDB BGP join groups.
   - Left-deep dynamic programming for small pure statement-pattern groups.
   - Greedy fallback for larger groups.
   - Generic fallback for mixed groups.

4. **`LmdbOptimizerStatisticsManager`**  
   Maintains cached prefix statistics for configured indexes.

5. **`TripleStore` additions**  
   Expose index descriptors, ordered access, and statistics snapshot building.

6. **`LmdbSailDataset` order support**  
   Implements ordered access, supported-order reporting, and a value comparator aligned to LMDB key order.

---

## 7. Current-state audit and required deltas

| Area | Current state | Required change |
|---|---|---|
| Strategy factory | `LmdbStore` defaults to `StrictEvaluationStrategyFactory` | Default to `LmdbEvaluationStrategyFactory` unless user supplied a custom factory |
| Join optimizer | Generic `QueryJoinOptimizer` only | Add `LmdbJoinOrderOptimizer` and wire it into LMDB pipeline |
| Cardinality | `LmdbEvaluationStatistics` sees constants only | Keep exact constant estimation, add prefix statistics for runtime-bound modeling |
| Index awareness | Runtime only (`TripleStore.getBestIndex`) | Expose index metadata to the planner |
| Ordered scans | LMDB dataset stubs return unsupported/null | Implement order support and comparator |
| Multi-context ordered reads | Existing code unions per-context iterators unsorted | Add k-way merge for ordered multi-context iteration |
| Plan observability | Runtime actual index already visible | Also annotate planned access path / desired order |

---

## 8. Chosen design

### 8.1 Strategy and pipeline wiring

#### Decision

Implement a **store-specific factory and pipeline** instead of changing RDF4J’s generic optimizer.

#### Why

- Minimal blast radius.
- LMDB-specific logic can use LMDB internals without generic abstractions.
- Easier to preserve behavior for other stores.
- Easier to back out behind a feature flag.

#### Implementation

- Add `LmdbEvaluationStrategyFactory extends StrictEvaluationStrategyFactory`.
- Override `createEvaluationStrategy(...)`.
- Create the strategy exactly as today, then call:
  - `strategy.setOptimizerPipeline(new LmdbQueryOptimizerPipeline(strategy, tripleSource, evaluationStatistics));`
- In `LmdbStore.getEvaluationStrategyFactory()`, instantiate `LmdbEvaluationStrategyFactory` instead of generic `StrictEvaluationStrategyFactory` when no custom factory was configured.

---

## 9. Join-group planning model

### 9.1 Scope of LMDB-specific planning

LMDB-specific join planning applies to **flattened inner join groups** when all non-priority join arguments are `StatementPattern`.

The optimizer will preserve RDF4J’s current handling for:

- `LeftJoin`
- extensions / `BIND`
- subselects
- other non-statement tuple expressions

If a join group contains mixed tuple-expression kinds, LMDB-specific planning is disabled for that group and the current generic behavior is used.

### 9.2 Binding state model

For every statement-pattern position `(s,p,o,c)` under a partial plan, classify the position as:

- **CONST**: compile-time known RDF value
- **BOUND**: variable value not known now, but guaranteed to be bound by the time the pattern executes
- **UNBOUND**: not yet bound
- **ABSENT**: no context variable exists in this pattern

This state is evaluated **per candidate subplan**.

### 9.3 Access-path model

For each configured LMDB index:

1. Map pattern positions into index field order.
2. Compute usable prefix length:
   - count leading fields whose state is `CONST` or `BOUND`
   - stop at first `UNBOUND`
3. Estimate:
   - `rowsScannedPerProbe`
   - `rowsReturnedPerProbe`
   - optional `orderedBy` variable
4. Keep the best candidates for planning.

### 9.4 Plan search model

- **DP threshold:** `8` statement patterns
- **For n <= 8:** left-deep dynamic programming
- **For n > 8:** greedy growth seeded by best singleton pattern
- **Fallback:** generic behavior if group is mixed or if statistics are unavailable and cannot be built

This is chosen because RDF4J’s execution model already strongly favors left-deep plans for parameterized index nested loops, while keeping the search space manageable.

---

## 10. Ordered access and merge join

### 10.1 Order support rule

LMDB may expose an order only when:

1. the chosen index has **best available prefix length** for the current binding signature,
2. that prefix length is **greater than zero**,
3. the first varying field after the prefix corresponds to an unbound variable,
4. the dataset wrapper can actually produce globally sorted output.

This rule prevents the optimizer from advertising useless orders based on full scans.

### 10.2 Runtime ordered access

Implement in `LmdbSailDataset`:

- `getStatements(StatementOrder, ...)`
- `getSupportedOrders(...)`
- `getComparator()`

When multiple specific contexts are requested, the implementation must:

- create one ordered iterator per context,
- then merge them with a comparator-consistent k-way merge,
- rather than using the current unsorted `UnionIteration`.

### 10.3 Comparator

Use a comparator based on LMDB internal value IDs.

Rationale:

- LMDB index keys are ordered by encoded value IDs.
- LMDB’s varint encoding preserves lexicographic and numeric order.
- Therefore comparison by internal ID is consistent with iterator order.

Fallback to generic value comparison is allowed only outside the ordered-LMDB fast path.

---

## 11. Statistics design

### 11.1 What statistics are required

For every configured index, maintain:

- total rows,
- distinct count for prefix length 1,
- distinct count for prefix length 2,
- distinct count for prefix length 3,
- distinct count for prefix length 4,
- derived average rows per prefix:
  - `avgRows(prefixLen) = totalRows / distinct(prefixLen)`

These are sufficient for the first implementation.

### 11.2 Why this is enough for v1

For runtime-bound prefix probes, the optimizer needs **average fanout**, not exact per-value counts.

This is exactly what prefix distinct counts provide.

### 11.3 Statistics lifecycle

- Build lazily on first optimizer use, or eagerly if configured.
- Cache in memory.
- Mark stale after any committed transaction that changes statements.
- Rebuild on next optimization once stale commit count passes threshold.

This avoids write-path complexity while keeping estimates current enough.

---

## 12. Explainability and observability

The implementation must expose enough information for debugging and regression detection.

### Planned metadata

Each planned statement pattern should carry:

- chosen index name (planned),
- estimated rows scanned,
- estimated rows returned,
- whether the access path was chosen for order,
- whether the surrounding join is nested loop or merge join.

### Runtime metadata

LMDB already reports the actual index used via the iterator path.  
Runtime metadata should continue to overwrite or confirm planned hints.

### Logging

Add debug logging for:

- chosen join order,
- bound variable set per step,
- candidate access paths considered,
- rejected merge-join candidates,
- statistics rebuild events.

---

## 13. Configuration

Add the following to `LmdbStoreConfig`:

| Property | Type | Default | Purpose |
|---|---:|---:|---|
| `queryOptimizerEnabled` | boolean | `true` | master feature flag |
| `queryOptimizerDynamicProgrammingThreshold` | int | `8` | DP vs greedy cutoff |
| `queryOptimizerMergeJoinEnabled` | boolean | `true` | allow merge-join planning |
| `queryOptimizerStatisticsEnabled` | boolean | `true` | enable prefix stats |
| `queryOptimizerStatisticsBuildOnInit` | boolean | `false` | eager stats build |
| `queryOptimizerStatisticsRefreshCommitThreshold` | int | `100` | stale-to-rebuild threshold |

If RDF config serialization is part of your deployment, also extend LMDB config schema/factory to read and write these values.

---

## 14. Compatibility

### Preserved behavior

- Results must remain identical.
- Existing LMDB index configuration remains valid.
- Existing custom evaluation strategy factory configuration still wins if explicitly set.

### Changed behavior

- Default LMDB query planning becomes index-aware.
- Ordered statement retrieval is now implemented.
- Explain/plan output includes LMDB-specific access-path details.

---

## 15. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Statistics build is expensive on huge stores | slower first optimized query | lazy build + rebuild threshold + config |
| Ordered access through wrapper datasets is not forwarded | merge join never triggers | audit every `SailDataset` wrapper used by LMDB and delegate order methods |
| Comparator mismatches iterator order | incorrect merge join | compare by LMDB internal IDs only for ordered LMDB path |
| Mixed join groups regress | planner instability | generic fallback for mixed groups |
| Overeager order advertisement | poor plans | advertise orders only when best prefix length > 0 and no harmful downgrade |
| Stale stats after many writes | suboptimal plans | dirty commit threshold + rebuild |

---

## 16. Deliverables

The implementation is complete only when all of the following exist:

1. `LmdbEvaluationStrategyFactory`
2. `LmdbQueryOptimizerPipeline`
3. `LmdbJoinOrderOptimizer`
4. `LmdbOptimizerStatisticsManager`
5. `TripleStore` index-descriptor and ordered-access support
6. `LmdbSailDataset` ordered retrieval, supported orders, comparator
7. unit tests
8. integration tests
9. explain-plan tests
10. performance benchmarks
11. rollback / feature-flag path

---

## 17. Acceptance summary

This design is accepted when:

1. LMDB chooses different join orders based on configured indexes and bound-variable state.
2. Plans differ appropriately for different index sets.
3. Runtime ordered access works.
4. Merge join appears only when it is both supported and beneficial.
5. Mixed groups remain correct and stable.
6. Benchmarks show improvement on representative LMDB workloads.

---

## 18. Implementation snapshot

### Mandatory code changes

- `LmdbStore`
- `LmdbStoreConnection`
- `LmdbSailStore`
- `LmdbEvaluationStatistics`
- `TripleStore`
- `LmdbStoreConfig`
- new optimizer/statistics classes in `org.eclipse.rdf4j.sail.lmdb`

### Mandatory new tests

- prefix statistics build
- access path enumeration
- join order change under different index sets
- runtime ordered scans
- merge join on LMDB
- fallback for mixed groups
- multi-context ordered iteration

---

## 19. Final recommendation

Implement this as an **LMDB-local physical planner** that reuses RDF4J’s existing pipeline architecture but replaces only the join-ordering stage for pure statement-pattern join groups.

That gives the team:

- a contained code change,
- correct physical planning for LMDB,
- order-aware execution support,
- and a safe fallback path when a query is outside the intended scope.