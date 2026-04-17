# RDF4J LMDB Join Order Optimizer — Technical Design Specification

**Document type:** Technical design specification  
**Audience:** Implementers, reviewers, QA, performance test owners  
**Normative language:** SHALL, SHOULD, MAY

---

## 1. Purpose

This specification defines the normative behavior of the RDF4J LMDB-specific join order optimizer, its supporting statistics, and the ordered-access runtime support needed for execution.

Anything marked SHALL is required for conformance to this spec.

---

## 2. Definitions

### 2.1 Terms

- **BGP**: basic graph pattern represented as a join group of `StatementPattern`
- **access path**: a combination of an LMDB index, usable prefix, and optional output order
- **usable prefix**: consecutive leading index fields that are fixed by `CONST` or `BOUND`
- **ordered candidate**: access path that can return bindings ordered by one unbound variable
- **partial plan**: a left-deep plan built from a subset of statement patterns
- **prefix statistics**: per-index row counts and distinct prefix counts
- **ordered runtime path**: `SailDataset.getStatements(StatementOrder, ...)` path

### 2.2 Position names

- `S` = subject
- `P` = predicate
- `O` = object
- `C` = context

---

## 3. Conformance scope

An implementation conforms to this specification if it satisfies all mandatory requirements in:

- section 4 (functional requirements)
- section 5 (planner behavior)
- section 6 (statistics)
- section 7 (runtime ordered access)
- section 8 (configuration)
- section 9 (error handling)
- section 10 (test requirements)

---

## 4. Functional requirements

### FR-001 — LMDB-specific pipeline

The LMDB store SHALL use an LMDB-specific optimizer pipeline by default when no custom evaluation strategy factory was explicitly supplied.

### FR-002 — Generic fallback preservation

If a custom evaluation strategy factory was explicitly provided in `LmdbStoreConfig`, LMDB SHALL preserve that configuration and SHALL NOT replace it automatically.

### FR-003 — Pure statement-pattern scope

The LMDB-specific join enumerator SHALL apply only to flattened inner join groups whose non-priority members are all `StatementPattern`.

### FR-004 — Mixed-group fallback

If a join group contains any non-priority member that is not a `StatementPattern`, the implementation SHALL use generic join-order behavior for that group.

### FR-005 — Bound-variable awareness

The LMDB-specific planner SHALL distinguish between:
- compile-time constants,
- runtime-bound variables,
- unbound variables.

### FR-006 — Index-set awareness

The planner SHALL enumerate candidate access paths from the actual configured LMDB index set.

### FR-007 — Prefix-based access-path selection

The planner SHALL use leading-prefix usability as the primary index-quality measure.

### FR-008 — Scan-vs-output distinction

The planner SHALL estimate both:
- rows scanned per probe,
- rows returned per probe.

### FR-009 — Ordered access support

The runtime LMDB dataset SHALL implement:
- `getStatements(StatementOrder, ...)`
- `getSupportedOrders(...)`
- `getComparator()`

### FR-010 — Merge-join correctness

The planner SHALL enable merge join only when both inputs can produce the same valid order and the runtime comparator is consistent with iterator order.

### FR-011 — Safe failure

Planner-statistics failures SHALL degrade plan quality conservatively but SHALL NOT fail query execution.

### FR-012 — Explainability

The planner SHALL expose the chosen index name per planned statement pattern.

---

## 5. Planner specification

## 5.1 Query traversal

### PR-001

The optimizer SHALL traverse the query model and optimize join nodes recursively.

### PR-002

For `LeftJoin`, the optimizer SHALL:
1. optimize the left argument first,
2. extend the bound-variable set with the left argument’s bindings,
3. optimize the right argument under that extended bound-variable set.

### PR-003

The optimizer SHALL preserve current priority treatment for:
- `BindingSetAssignment`
- extension / `BIND`
- subselect-like join arguments

These SHALL NOT be fed into the LMDB-specific statement-pattern DP search.

---

## 5.2 Binding-state rules

### PR-010 — CONST classification

A statement-pattern position SHALL be classified as `CONST` if:
1. its `Var` already has a compile-time value, or
2. its variable name exists in incoming query bindings with a concrete value.

### PR-011 — BOUND classification

A statement-pattern position SHALL be classified as `BOUND` if:
1. it is not `CONST`, and
2. its variable name is already bound by earlier steps in the current partial plan.

### PR-012 — UNBOUND classification

A statement-pattern position SHALL be classified as `UNBOUND` if:
1. it is not `CONST`, and
2. it is not `BOUND`.

### PR-013 — ABSENT classification

If the statement pattern has no context variable, context SHALL be classified as `ABSENT`.

### PR-014 — Invalid constant types

If a subject constant is not a `Resource`, predicate constant is not an `IRI`, or context constant is not a `Resource`, the implementation SHALL treat that position as not indexable for planning purposes.

---

## 5.3 Index enumeration rules

### PR-020 — Candidate index source

The planner SHALL enumerate candidate indexes from the configured LMDB triple indexes only.

### PR-021 — Usable prefix length

For an index field sequence `f1 f2 f3 f4`, usable prefix length SHALL equal the number of consecutive leading fields in `{CONST, BOUND}` state, stopping at the first `UNBOUND` or `ABSENT`.

### PR-022 — Best-prefix retention

The planner SHALL retain candidate indexes whose usable prefix length equals the maximum usable prefix length for the statement pattern under the current bound-variable set.

### PR-023 — Lower-prefix rejection

The planner SHALL reject candidate indexes whose usable prefix length is strictly less than the best available prefix length for that statement pattern and bound-variable set.

### PR-024 — Prefix-zero order suppression

If the best usable prefix length is zero, the planner SHALL NOT advertise any order property for that statement pattern.

### PR-025 — Deterministic tie-breaking

When multiple candidate indexes have equal usable prefix length and equivalent cost, the planner SHALL tie-break using configured LMDB index order.

---

## 5.4 Ordered-candidate rules

### PR-030 — Ordered candidate eligibility

A candidate access path MAY expose an order property only if:
1. its prefix length is greater than zero,
2. its prefix length is less than four,
3. the first varying field after the usable prefix corresponds to an unbound variable.

### PR-031 — Ordered variable derivation

The ordered variable SHALL be the variable bound to the first varying field after the usable prefix.

### PR-032 — Ordered candidate deduplication

If multiple retained indexes produce the same ordered variable and materially identical probe costs, the planner SHOULD keep only the cheapest one.

### PR-033 — No ordered downgrade

The planner SHALL NOT choose an ordered access path that requires a prefix downgrade relative to the best available prefix length.

---

## 5.5 Statistics usage rules

### PR-040 — Constant-only exact cardinality

If all fixed positions relevant to a cardinality estimate are compile-time constants, the planner SHALL use `LmdbEvaluationStatistics` / `TripleStore.cardinality(...)` for exact or existing-estimator cardinality.

### PR-041 — Runtime-bound prefix fanout

If a usable prefix contains any runtime-bound position, the planner SHALL use average rows per prefix from prefix statistics for `rowsScannedPerProbe`.

### PR-042 — Prefix-zero scan estimate

If usable prefix length is zero, `rowsScannedPerProbe` SHALL equal total row count from the current stats snapshot, or a conservative equivalent if stats are unavailable.

### PR-043 — Output rows exact fast path

If all fixed positions in a statement pattern are compile-time constants, `rowsReturnedPerProbe` SHALL use exact full-pattern cardinality.

### PR-044 — Output rows mixed path

If runtime-bound values are involved, `rowsReturnedPerProbe` SHALL be estimated from:
1. `rowsScannedPerProbe`, then
2. selectivity factors for additional non-prefix fixed fields.

### PR-045 — Constant non-prefix selectivity

A constant non-prefix field SHOULD use:
```text
exactCardinality(field = const only) / totalRows
```
clamped to `[0,1]`.

### PR-046 — Runtime-bound non-prefix selectivity

A runtime-bound non-prefix field SHOULD use:
```text
1 / distinct(field)
```
where `distinct(field)` is derived from an index whose first field is that field.

### PR-047 — Distinct-field fallback

If no index begins with the field needed for `distinct(field)`, the planner SHALL use a conservative fallback:
```text
1 / max(10, sqrt(totalRows))
```
or a stricter equivalent.

### PR-048 — Repeated-variable intra-pattern equality

If the same variable appears multiple times in one statement pattern and not all occurrences are fixed by the usable prefix, the planner SHOULD apply an additional equality selectivity factor.

If omitted in v1, the omission SHALL be documented and covered by tests proving correctness is unaffected.

---

## 5.6 Cost formulas

### PR-050 — Probe cost formula

The implementation SHALL compute probe cost using a formula equivalent in shape to:

```text
probeCost = probeStartup + scanRowsPerProbe * scanWeight + rowsReturnedPerProbe * emitWeight
```

The exact constants MAY be internal and configurable only in code.

### PR-051 — Nested-loop step cost

For a left-deep addition of statement pattern `p` to partial plan `L`:

```text
stepCost = rows(L) * probeCost(p | bindings(L))
stepRows = rows(L) * rowsReturnedPerProbe(p | bindings(L))
```

### PR-052 — Cross-join penalty semantics

If `p` shares no binding variable with `L`, the implementation SHALL still use the same nested-loop formula.  
The multiplicative effect of `rows(L)` on a non-parameterized scan SHALL serve as the cross-join penalty.

### PR-053 — Ordered-state as physical property

The planner SHALL treat output order as a physical property of the partial plan state key.

---

## 5.7 Plan search rules

### PR-060 — Left-deep search

The LMDB-specific planner SHALL search only left-deep plan shapes in v1.

### PR-061 — DP threshold

For pure statement-pattern join groups of size `<= queryOptimizerDynamicProgrammingThreshold`, the planner SHALL use dynamic programming over subsets.

### PR-062 — Greedy fallback above threshold

For larger pure statement-pattern join groups, the planner SHALL use greedy growth from the cheapest singleton plan.

### PR-063 — Singleton state creation

The planner SHALL create:
- one unordered singleton state,
- and zero or more ordered singleton states

for each statement pattern.

### PR-064 — State key

A DP state key SHALL include:
- subset membership,
- ordered variable name or null.

### PR-065 — Transition generation

For each state transition, the planner SHALL consider:
- nested-loop addition,
- optional merge-join addition

when merge eligibility rules are satisfied.

### PR-066 — Deterministic final choice

If multiple full-plan states have equal cost within floating-point tolerance, the implementation SHALL use deterministic tie-breaking.

---

## 5.8 Merge-join rules

### PR-070 — Merge-join eligibility

A merge join MAY be generated only if all conditions hold:

1. merge join feature flag enabled
2. current partial plan has ordered property on variable `v`
3. next statement pattern can produce ordered property on same `v`
4. `v` is a shared join variable
5. cardinality ratio is acceptable
6. ordered access does not require prefix downgrade
7. runtime ordered access is available

### PR-071 — Cardinality-ratio guard

The acceptable cardinality ratio SHALL use the same conceptual guard as RDF4J’s current merge-join heuristic, defaulting to a multiplier of `10` unless configured otherwise in code.

### PR-072 — Merge output rows

Merge-join output rows SHALL use the same output-row estimate as the nested-loop alternative over the same binding assumptions.

### PR-073 — Merge plan annotation

A merge join in the final plan SHALL set:
- `join.setMergeJoin(true)`
- `join.setOrder(var)`

---

## 5.9 Plan materialization rules

### PR-080 — Search/model separation

The planner SHALL NOT mutate the original query model while evaluating candidate states.

### PR-081 — Final tree shape

After the best plan is chosen, the planner SHALL build a right-recursive `Join` tree.

### PR-082 — Planned index annotation

Each chosen statement pattern in the final plan SHALL have the planned index name recorded via `setIndexName(...)` or equivalent metadata hook.

### PR-083 — Statement-pattern order annotation

If runtime ordered retrieval requires statement-pattern-local order metadata in addition to `Join.order`, the final plan construction SHALL set that metadata on the relevant statement patterns.

### PR-084 — Existing result-size estimates

The planner SHOULD continue to populate result-size estimates on statement patterns and joins where existing RDF4J infrastructure expects them.

---

## 6. Statistics specification

## 6.1 Snapshot contents

### ST-001

The optimizer statistics snapshot SHALL contain:
- total row count,
- one entry per configured LMDB index.

### ST-002

Each per-index stats entry SHALL contain:
- index name
- field sequence
- total row count
- distinct prefix count for lengths 1, 2, 3, 4
- derived average rows per prefix for lengths 1, 2, 3, 4

### ST-003

All snapshot data used by the planner SHALL be immutable after publication.

---

## 6.2 Snapshot build algorithm

### ST-010

The implementation SHALL compute per-index prefix distinct counts by scanning the index in key order.

### ST-011

Distinct prefix count for length `k` SHALL increment when the first `k` decoded fields differ from the previous key.

### ST-012

Stats build SHOULD aggregate explicit and inferred statement DBs into one snapshot unless query-evaluation scope-specific stats are explicitly implemented.

### ST-013

Temporary decode buffers SHALL be reused; the implementation SHOULD avoid per-row allocation.

---

## 6.3 Snapshot lifecycle

### ST-020

A stats snapshot SHALL be built:
- eagerly at initialization if configured,
- otherwise lazily on first optimizer use.

### ST-021

Any committed transaction that changes statements SHALL mark the stats snapshot dirty.

### ST-022

Dirtying MAY be tracked by commit count rather than exact mutation count.

### ST-023

The stats manager SHALL rebuild the snapshot when dirty count reaches configured threshold.

### ST-024

If rebuild fails, the implementation SHALL keep the previous snapshot if available.

### ST-025

Stats rebuild failure SHALL NOT fail query execution.

---

## 7. Runtime ordered-access specification

## 7.1 Dataset-level order support

### RT-001

`LmdbSailDataset.getSupportedOrders(...)` SHALL return the set of statement orders producible at best prefix quality for the concrete bindings supplied to the call.

### RT-002

If best prefix length is zero, `getSupportedOrders(...)` SHALL return an empty set.

### RT-003

`LmdbSailDataset.getComparator()` SHALL return a comparator consistent with LMDB ordered-iterator output.

### RT-004

`LmdbSailDataset.getStatements(StatementOrder, ...)` SHALL produce statements in that order when the order is supported.

---

## 7.2 TripleStore order support

### RT-010

`TripleStore` SHALL expose the configured index descriptors to the LMDB planner.

### RT-011

`TripleStore` SHALL expose a method to determine supported orders from concrete bindings.

### RT-012

`TripleStore` SHALL expose an ordered-access method that chooses the best index matching:
- best prefix length,
- requested order as first varying field after prefix.

### RT-013

If no best-prefix index supports the requested order, the implementation SHALL NOT silently claim the order is supported.

It MAY:
- fall back to unordered access with debug logging, or
- reject the ordered request internally.

---

## 7.3 Multi-context ordered iteration

### RT-020

If multiple specific contexts are requested for an ordered call, the implementation SHALL merge per-context ordered iterators using a global k-way merge.

### RT-021

The implementation SHALL NOT concatenate ordered per-context iterators using plain union/append semantics.

### RT-022

The merge comparator SHALL be derived from:
```text
statementOrder.getComparator(valueComparator)
```
where `valueComparator` is LMDB-order-consistent.

---

## 7.4 Comparator rules

### RT-030

For the LMDB ordered fast path, value comparison SHALL prefer LMDB internal value IDs.

### RT-031

If both values have valid LMDB internal IDs, comparison SHALL use unsigned numeric ordering of those IDs or an equivalent total order consistent with LMDB key order.

### RT-032

If one or both IDs are unavailable, the implementation MAY fall back to a generic RDF value comparator only outside the guaranteed LMDB ordered path.

### RT-033

The implementation SHALL have tests proving comparator consistency with iterator order.

---

## 8. Configuration specification

## 8.1 Required configuration fields

### CFG-001

`LmdbStoreConfig` SHALL expose:

- `queryOptimizerEnabled`
- `queryOptimizerDynamicProgrammingThreshold`
- `queryOptimizerMergeJoinEnabled`
- `queryOptimizerStatisticsEnabled`
- `queryOptimizerStatisticsBuildOnInit`
- `queryOptimizerStatisticsRefreshCommitThreshold`

### CFG-002

Default values SHALL be:

```text
queryOptimizerEnabled = true
queryOptimizerDynamicProgrammingThreshold = 8
queryOptimizerMergeJoinEnabled = true
queryOptimizerStatisticsEnabled = true
queryOptimizerStatisticsBuildOnInit = false
queryOptimizerStatisticsRefreshCommitThreshold = 100
```

### CFG-003

If RDF config serialization is supported for LMDB store configuration, these fields SHALL also be serializable/deserializable through LMDB config schema/factory classes.

---

## 9. Error-handling specification

### ERR-001 — Unknown compile-time constant

If a compile-time constant cannot be resolved to an LMDB value ID, exact cardinality SHALL be treated as zero.

### ERR-002 — Stats unavailable

If no stats snapshot exists and building one fails, the planner SHALL fall back to conservative heuristic estimates.

### ERR-003 — Comparator resolution failure

If internal IDs cannot be resolved for values on the non-guaranteed path, generic fallback comparison MAY be used, but merge-join correctness MUST remain preserved.

### ERR-004 — Unsupported ordered request

If ordered retrieval is requested but not actually satisfiable at runtime, the implementation SHALL fall back safely and SHALL NOT claim the order was produced.

### ERR-005 — Mixed-group unsupported optimization

Mixed join groups outside the LMDB-specific scope SHALL use generic behavior.

### ERR-006 — Lock contention during stats rebuild

If another thread is already rebuilding stats, the current thread MAY continue using the previous snapshot.

---

## 10. Test specification

## 10.1 Mandatory unit tests

### TEST-001

Prefix statistics build SHALL be tested against a small deterministic dataset where distinct prefix counts are known exactly.

### TEST-002

Access-path prefix calculation SHALL be tested for:
- constants
- runtime-bound variables
- non-prefix constants
- absent context

### TEST-003

Supported-order reporting SHALL be tested for:
- best prefix > 0
- best prefix = 0
- multiple same-prefix indexes
- requested multiple contexts

### TEST-004

LMDB ordered comparator SHALL be tested against known LMDB value IDs.

---

## 10.2 Mandatory integration tests

### TEST-010

The same query under different index sets SHALL produce different chosen statement orders when physically appropriate.

### TEST-011

The same query under different earlier bound-variable states SHALL produce different next-pattern choices when parameterized probes become available.

### TEST-012

Merge join SHALL appear only when:
- same order available on both sides,
- prefix quality not downgraded,
- size ratio acceptable.

### TEST-013

When multiple contexts are requested, ordered result streams SHALL remain globally ordered.

### TEST-014

Mixed join groups SHALL remain correct and shall use generic fallback.

---

## 10.3 Mandatory metadata tests

### TEST-020

Chosen statement patterns SHALL carry planned index names.

### TEST-021

Final merge joins SHALL be annotated with `mergeJoin=true` and the correct order variable.

### TEST-022

Runtime actual index reporting SHALL remain available.

---

## 10.4 Mandatory benchmark tests

### TEST-030

Benchmark suite SHALL include:
- star joins
- path joins
- object-heavy joins
- context-heavy joins
- cross-join-avoidance cases

### TEST-031

Benchmarks SHALL compare at least two LMDB index configurations.

### TEST-032

Benchmarks SHALL capture:
- planning time
- execution time
- result count
- chosen plan/index sequence

---

## 11. Non-functional requirements

### NFR-001 — Correctness first

The implementation SHALL preserve query results.

### NFR-002 — Determinism

For a fixed query, dataset, bindings, and index configuration, the planner SHALL make deterministic choices.

### NFR-003 — Isolation

The feature SHALL not change planning behavior for non-LMDB stores.

### NFR-004 — Observability

The implementation SHALL provide enough metadata and logging to diagnose planner regressions.

### NFR-005 — Rollback

The feature SHALL be disableable by configuration.

### NFR-006 — Modest planner overhead

The planner SHOULD keep optimization overhead reasonable for typical BGP sizes.

### NFR-007 — No write-path regression by design

Statistics maintenance SHALL avoid expensive exact update work on every write in v1.

---

## 12. Invariants

### INV-001

No supported order SHALL be advertised when best usable prefix length is zero.

### INV-002

A merge join SHALL never be emitted without a comparator-consistent ordered runtime path.

### INV-003

The planner SHALL never depend on stats freshness for correctness.

### INV-004

If ordered access through wrappers is unavailable, the planner SHALL still generate a valid unordered plan.

### INV-005

Configured LMDB index order SHALL define deterministic tie-breaking among equal-cost equal-prefix candidates.

---

## 13. Reference algorithms

## 13.1 Prefix length

```text
prefixLen = 0
for field in index.fieldSeq:
    if state(field) in {CONST, BOUND}:
        prefixLen++
    else:
        break
```

## 13.2 Ordered variable

```text
if prefixLen == 0 or prefixLen == 4:
    no order
else:
    nextField = index.fieldSeq[prefixLen]
    if state(nextField) == UNBOUND:
        orderedVar = variable(nextField)
    else:
        no order
```

## 13.3 DP transition

```text
for each state:
    for each remaining pattern:
        for each access path:
            nestedLoopTransition(...)
            if mergeEligible(...):
                mergeTransition(...)
            keep best by (subset, orderedVar)
```

---

## 14. Out-of-scope clarifications

The following are explicitly non-required for conformance to v1 of this spec:

1. bushy join enumeration
2. exact skew-aware histograms
3. adaptive runtime re-planning
4. automatic index recommendation or creation
5. optimizer-local handling for arbitrary non-`StatementPattern` nodes beyond safe fallback

---

## 15. Implementation conformance checklist

An implementation is conformant when every statement below is true.

### Planner wiring

- [ ] FR-001 satisfied
- [ ] FR-002 satisfied

### Planner behavior

- [ ] PR-010 to PR-084 satisfied

### Statistics

- [ ] ST-001 to ST-025 satisfied

### Runtime ordered access

- [ ] RT-001 to RT-033 satisfied

### Config

- [ ] CFG-001 to CFG-003 satisfied

### Error handling

- [ ] ERR-001 to ERR-006 satisfied

### Tests

- [ ] TEST-001 to TEST-032 satisfied

### Non-functional

- [ ] NFR-001 to NFR-007 satisfied

### Invariants

- [ ] INV-001 to INV-005 satisfied

---

## 16. Final normative statement

The LMDB join optimizer is considered complete only when it behaves as a **physical planner** that is:

- aware of the actual configured LMDB indexes,
- aware of which variables are already bound,
- aware of when ordered access is truly available,
- safe under stale or missing statistics,
- and correct under all fallback conditions.