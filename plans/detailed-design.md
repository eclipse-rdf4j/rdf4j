# RDF4J LMDB Join Order Optimizer — Detailed Design

**Document type:** Detailed design  
**Target module:** `core/sail/lmdb`  
**Audience:** Engineers implementing planner, ordered access, and statistics support

---

## 1. Overview

This document turns the implementation design into concrete behavior.

The optimizer is built around one idea:

> For LMDB, the cost of adding the next statement pattern depends on the actual index prefixes that can be used **after considering which variables are already bound**.

The design therefore models:

1. **binding state** per position,
2. **candidate access paths** per statement pattern,
3. **rows scanned per probe**,
4. **rows returned per probe**,
5. **order properties** for merge join,
6. and **plan-state cost accumulation**.

---

## 2. Scope

### 2.1 In-scope

- inner join groups made only of `StatementPattern`
- join reordering for LMDB
- LMDB index-aware cost estimation
- ordered retrieval and merge-join enablement
- prefix statistics
- explainable chosen access paths

### 2.2 Out-of-scope for this version

- bushy plans
- adaptive re-optimization during execution
- exact skew-aware histograms
- optimizer support for SERVICE, property paths, arbitrary mixed tuple expressions
- automatic creation of new indexes

---

## 3. Query-tree traversal behavior

`LmdbJoinOrderOptimizer` must preserve the traversal behavior of RDF4J’s current `QueryJoinOptimizer` unless explicitly changed here.

### 3.1 `LeftJoin`

Behavior is unchanged:

1. optimize left side first,
2. extend bound-variable scope with left-side bindings,
3. optimize right side under that extended scope.

### 3.2 Priority join arguments

Maintain current behavior for:

- `BindingSetAssignment`
- extension / `BIND`
- subselect-like arguments that should be evaluated before normal join reordering

These are **not** part of LMDB DP search.

### 3.3 Pure statement-pattern groups

If, after removing priority arguments, the remaining flattened join group consists only of `StatementPattern`, apply LMDB-specific planning.

### 3.4 Mixed groups

If any remaining non-priority argument is not a `StatementPattern`, do **not** use LMDB DP for that group. Use generic ordering logic for that group.

This is the safety boundary for the first implementation.

---

## 4. Binding-state model

For each statement pattern position:

- `S` = subject
- `P` = predicate
- `O` = object
- `C` = context

Define a per-position state:

```text
CONST  = compile-time constant RDF value
BOUND  = variable is not constant now, but will be bound by previous join steps
UNBOUND = variable still free when this pattern executes
ABSENT  = no context variable exists for the pattern
```

### 4.1 Source of `CONST`

A position is `CONST` if:

1. `Var.hasValue()` is true, or
2. the variable name is present in incoming query bindings with a concrete value.

In the second case, the value must be treated as a compile-time constant for planning.

### 4.2 Source of `BOUND`

A position is `BOUND` if:

1. the variable has no compile-time value, and
2. its variable name is already bound by the current partial plan.

### 4.3 Source of `UNBOUND`

A position is `UNBOUND` if:

1. it has no compile-time value, and
2. its variable name is not yet bound by the current partial plan.

### 4.4 Source of `ABSENT`

Only applicable to context:

- if the statement pattern has no context variable, context state is `ABSENT`.

For index-prefix scoring, `ABSENT` behaves the same as `UNBOUND`.

---

## 5. Statement-pattern term resolution

For every statement pattern, construct a `ResolvedPatternTerms` object:

```text
subject:  PositionState + optional Value + optional variable name
predicate: PositionState + optional Value + optional variable name
object:   PositionState + optional Value + optional variable name
context:  PositionState + optional Value + optional variable name
```

This object is input to all cost and access-path logic.

### 5.1 Repeated variables inside one pattern

If the same variable occurs in multiple positions, the state of each occurrence is derived independently but linked by variable name.

Examples:

- `?x :p ?x`
  - if `?x` not yet bound: subject = `UNBOUND`, object = `UNBOUND`, plus equality constraint
  - if `?x` already bound: subject = `BOUND`, object = `BOUND`

### 5.2 Invalid constant types

Use the same defensive behavior as current cardinality code:

- subject constant must be a `Resource`
- predicate constant must be an `IRI`
- context constant must be a `Resource`

Invalid inlined values must be treated as unbound/non-indexable for planning.

---

## 6. Index model

LMDB indexes are configured as field sequences such as:

- `spoc`
- `posc`
- `sopc`
- `ospc`

Each sequence defines the lexicographic order of the encoded quad key.

### 6.1 Descriptor representation

Represent each index with:

```text
name      = original field sequence string, e.g. "spoc"
fieldSeq  = char[] or equivalent, e.g. ['s','p','o','c']
```

### 6.2 Prefix usability

For a given resolved statement pattern, the usable prefix length of an index is:

> the number of consecutive leading fields in the index whose positions are in state `CONST` or `BOUND`, stopping at the first `UNBOUND`/`ABSENT`.

Examples:

#### Example A
Pattern: `?s :p ?o`  
Current bound vars: `{?s}`  
Index: `spoc`

- `s = BOUND`
- `p = CONST`
- `o = UNBOUND`
- `c = ABSENT`

Usable prefix = `2` (`s,p`)

#### Example B
Pattern: `?s :p ?o`  
Current bound vars: `{}`  
Index: `spoc`

- `s = UNBOUND`
- `p = CONST`
- usable prefix = `0`

#### Example C
Pattern: `:a ?p :b`  
Index: `spoc`

- `s = CONST`
- `p = UNBOUND`
- `o = CONST`
- usable prefix = `1`, not `2`

The fixed object does not help because predicate blocks the prefix.

---

## 7. Access-path enumeration

For each statement pattern under a given bound-variable set:

1. enumerate every configured LMDB index,
2. compute prefix length,
3. estimate scan rows and output rows,
4. derive optional order property,
5. retain best candidates.

### 7.1 Candidate retention rule

Keep all indexes whose prefix length equals the **maximum prefix length** for that pattern and bound-variable set.

Reason:

- the optimizer must be aware of alternative orders available at the same prefix quality,
- but it must not keep obviously dominated lower-prefix candidates.

### 7.2 Exception: no order candidates for prefix length 0

If the best prefix length is `0`, the candidate set is reduced to unordered access only.

Reason:

- a full-scan order is usually not useful enough to justify merge-join planning,
- and advertising such orders leads to pathological plans.

### 7.3 Candidate fields

Each access-path candidate must contain at least:

```text
indexName
fieldSeq
prefixLen
rowsScannedPerProbe
rowsReturnedPerProbe
probeCost
orderedByVarName (nullable)
requestedStatementOrder (nullable)
usesRangeLookup (boolean)
```

### 7.4 Ordered candidate derivation

For a retained index candidate:

- if `prefixLen > 0`
- and `prefixLen < 4`
- and the first varying field after the prefix corresponds to an unbound variable,

then the candidate may expose an order.

Example:

Index = `spoc`, prefixLen = 2, first varying field = `o`  
If object variable is unbound, `orderedByVarName = objectVarName`.

If the field after the prefix is already fixed, there is a bug in prefix computation.

---

## 8. Statistics model

The optimizer needs two classes of numbers:

1. **exact constant cardinalities**
2. **average prefix fanout**

### 8.1 Exact constant cardinalities

Use LMDB’s existing exact or estimated constant-based cardinality logic through `LmdbEvaluationStatistics` and `TripleStore.cardinality(...)`.

This path is used when all relevant fixed values are compile-time constants.

### 8.2 Prefix statistics

For each index, maintain:

- `rowCount`
- `distinctPrefixCount[1]`
- `distinctPrefixCount[2]`
- `distinctPrefixCount[3]`
- `distinctPrefixCount[4]`

Derived:

- `avgRowsPerPrefix[1] = rowCount / distinctPrefixCount[1]`
- `avgRowsPerPrefix[2] = rowCount / distinctPrefixCount[2]`
- etc.

### 8.3 Why prefix distinct counts are enough

For runtime-bound prefix probes, the optimizer needs:

> “If the first `k` fields of this index are bound at execution time, how many rows do I expect per probe?”

That is exactly `avgRowsPerPrefix[k]`.

### 8.4 Snapshot format

Use an immutable in-memory snapshot:

```text
StatisticsSnapshot
  totalRows
  builtAtNanos
  dirtyCommitsAtBuild
  byIndexName -> IndexStatistics
```

Where:

```text
IndexStatistics
  indexName
  fieldSeq
  rowCount
  distinctPrefixCount[1..4]
  avgRowsPerPrefix[1..4]
```

---

## 9. Building prefix statistics

### 9.1 Build strategy

Compute prefix statistics by scanning each configured index in key order.

Because keys are already sorted lexicographically by index sequence, distinct prefix counts are easy to count in a single pass.

### 9.2 One-pass algorithm

For each index:

1. open read cursor on explicit DB
2. open read cursor on inferred DB
3. process both and aggregate totals
4. for each record:
   - decode ordered quad IDs for this index sequence
   - compare prefix lengths 1..4 against previous key
   - increment distinct counters when the prefix changes

### 9.3 Pseudocode

```text
for each index:
    rows = 0
    distinct[1..4] = 0
    prevKey = null

    for each record in explicit DB and inferred DB:
        key = decodeIndexOrderedIds(record)

        rows += 1

        if prevKey == null:
            distinct[1] += 1
            distinct[2] += 1
            distinct[3] += 1
            distinct[4] += 1
        else:
            if prefixChanged(prevKey, key, 1): distinct[1] += 1
            if prefixChanged(prevKey, key, 2): distinct[2] += 1
            if prefixChanged(prevKey, key, 3): distinct[3] += 1
            if prefixChanged(prevKey, key, 4): distinct[4] += 1

        prevKey = key
```

### 9.4 Complexity

For `m` configured indexes and `N` statements:

- build complexity is `O(mN)`
- memory is `O(m)`

This is acceptable because:

- LMDB index count is low,
- stats build is infrequent,
- result is cached.

---

## 10. Statistics refresh policy

### 10.1 Dirtying rule

Any committed transaction that adds or removes statements increments `dirtyCommitCount`.

Exact mutation counts are not required for the first implementation.

### 10.2 Refresh rule

Refresh snapshot when either condition is true:

1. no snapshot exists,
2. `dirtyCommitCount >= queryOptimizerStatisticsRefreshCommitThreshold`

### 10.3 Build timing

If `queryOptimizerStatisticsBuildOnInit = true`, build at store initialization.

Otherwise build lazily on first optimizer use.

### 10.4 Failure behavior

If stats build fails:

1. log warning,
2. keep previous snapshot if present,
3. fall back to conservative heuristics,
4. never fail query planning because stats rebuild failed.

---

## 11. Cost model

The planner needs:

- **probe scan cost**
- **probe output rows**
- **step cost**
- **plan output rows**

### 11.1 Definitions

Let:

- `outerRows` = estimated rows produced by current partial plan
- `scanRowsPerProbe` = rows LMDB must inspect per probe
- `outRowsPerProbe` = rows returned per probe
- `scanWeight` = cost of scanning one row (constant)
- `emitWeight` = cost of materializing one output row (constant)
- `probeStartup` = constant startup per probe

Recommended defaults:

```text
scanWeight = 1.0
emitWeight = 0.25
probeStartup = 5.0
```

These weights are internal constants, not public API.

### 11.2 Scan rows per probe

#### Case A: prefix length = 0

```text
scanRowsPerProbe = totalRows
```

#### Case B: prefix length > 0 and every prefix field is compile-time constant

Use exact cardinality of the prefix-only restriction.

Example:

- chosen index `spoc`
- prefixLen = 2
- subject constant, predicate constant

Then compute:

```text
scanRowsPerProbe = cardinality(s = const, p = const, o = *, c = *)
```

#### Case C: prefix length > 0 and any prefix field is runtime-bound

Use average rows for that prefix length from the chosen index:

```text
scanRowsPerProbe = avgRowsPerPrefix[prefixLen]
```

### 11.3 Output rows per probe

`outRowsPerProbe` models how many rows survive all restrictions.

#### Fast path: all fixed positions are compile-time constants

Use exact full-pattern cardinality:

```text
outRowsPerProbe = cardinality(all compile-time fixed values)
```

#### Mixed path: runtime-bound values present

Start with:

```text
outRowsPerProbe = scanRowsPerProbe
```

Then multiply by selectivity factors for all additional restrictions not already accounted for in the usable prefix.

##### 11.3.1 Constant non-prefix field

For a constant field not in the usable prefix:

```text
selectivity(field = const) = exactCardinality(field = const only) / totalRows
outRowsPerProbe *= selectivity
```

Clamp to `[0,1]`.

##### 11.3.2 Runtime-bound non-prefix field

For a runtime-bound field not in the usable prefix:

```text
selectivity(field = bound) = 1 / distinct(field)
```

Where `distinct(field)` is derived from any configured index whose first field is that field.

If no index starts with that field, use fallback:

```text
selectivity = 1 / max(10, sqrt(totalRows))
```

##### 11.3.3 Repeated-variable equality inside one pattern

For every additional occurrence of the same variable not already fixed by the prefix, multiply by an equality factor:

```text
equalitySelectivity ≈ 1 / distinct(fieldOfExtraOccurrence)
```

Use the same `distinct(field)` lookup as above.

If unavailable, reuse the generic fallback.

### 11.4 Probe cost

```text
probeCost = probeStartup + (scanRowsPerProbe * scanWeight) + (outRowsPerProbe * emitWeight)
```

### 11.5 Step cost in a left-deep plan

Adding a new statement pattern to an existing partial plan:

```text
stepCost = outerRows * probeCost
stepRows = outerRows * outRowsPerProbe
```

This works because every row from the left side produces one parameterized probe of the right-side statement pattern.

### 11.6 Cross join penalty

If the new statement pattern shares no variable with the current partial plan, the same formula already penalizes it because `outerRows` multiplies a non-parameterized scan.

No extra ad-hoc penalty is required in the LMDB DP model.

### 11.7 Merge join cost

If a merge join is possible:

```text
mergeCost = leftCost + rightScanCost + mergeCpuFactor * (leftRows + rightRows)
```

Recommended:

```text
mergeCpuFactor = 0.2
```

But for plan comparison, the implementation may simplify to:

```text
mergeStepCost = leftRows + rightRows
```

if constants are calibrated consistently.

### 11.8 Merge join output rows

Use the same output-row estimate as nested-loop addition:

```text
mergeStepRows = leftRows * outRowsPerProbe
```

because `outRowsPerProbe` is already conditioned on the shared bound variables.

---

## 12. Choosing between nested loop and merge join

A merge join candidate is valid only if:

1. merge joins are enabled,
2. current partial plan has an output order on variable `v`,
3. next statement pattern can produce the same order on `v`,
4. `v` is a join variable,
5. the size ratio satisfies:
   ```text
   max(leftRows, rightRows) / max(1, min(leftRows, rightRows)) <= MERGE_JOIN_CARDINALITY_SIZE_DIFF_MULTIPLIER
   ```
6. ordered access does not require a prefix downgrade.

If valid, compute both nested-loop and merge costs and keep the cheaper state.

---

## 13. DP plan search

### 13.1 State shape

A DP state must contain:

```text
subsetMask          // which statement patterns are included
orderedByVarName    // nullable physical property
cost                // total estimated cost of the plan
rows                // total estimated output rows
boundVarNames       // all binding names produced by the subset
planItems           // ordered plan representation used to build the final tree
```

`orderedByVarName` is part of the state key because the same subset may have different future usefulness depending on whether it preserves a join order.

### 13.2 Singleton states

For each statement pattern:

1. enumerate retained access paths,
2. create:
   - one unordered singleton state,
   - plus one ordered singleton state per supported order,
3. keep only the cheapest state per `(subsetMask, orderedByVarName)`.

### 13.3 Transition

For each existing state and each remaining statement pattern:

1. derive bound-variable set from the state,
2. enumerate access paths for that pattern under the state’s bindings,
3. compute:
   - nested-loop transition
   - optional merge-join transition
4. keep best state for resulting `(newSubsetMask, newOrderedByVarName)`.

### 13.4 Final state

Pick the cheapest full-subset state.

If multiple full states have nearly equal cost, tie-break in this order:

1. lower total cost
2. lower output rows
3. higher average prefix length
4. more merge joins
5. deterministic lexical order of original statement-pattern positions

### 13.5 DP cutoff

If pattern count exceeds threshold, switch to greedy.

---

## 14. Greedy fallback

For statement-pattern groups larger than threshold:

1. choose the best singleton statement pattern,
2. repeatedly add the cheapest next statement pattern using current bound vars,
3. preserve ordered state when beneficial,
4. consider merge join only when current state already has order.

This keeps complexity linear-ish for large groups.

---

## 15. Building the final join tree

### 15.1 Do not mutate while searching

During DP/greedy search, do **not** mutate the original query model.

Store lightweight planning metadata only.

### 15.2 Plan item representation

Each plan item should record:

```text
originalStatementPattern
chosenIndexName
chosenOrderVarName (nullable)
joinMethodToPrevious = NONE | NESTED_LOOP | MERGE
mergeJoinVarName (nullable)
```

### 15.3 Tree construction

After best plan is chosen:

1. clone or reuse optimized statement-pattern nodes in the chosen order,
2. attach metadata:
   - set planned `indexName`
   - set statement-pattern order if directly needed by execution path
3. build a right-recursive `Join` chain
4. mark joins with:
   - `setMergeJoin(true)`
   - `setOrder(var)`
   where applicable

### 15.4 Right-recursive shape

Preserve RDF4J’s current right-recursive join construction to remain compatible with iterative evaluation optimization behavior.

---

## 16. Ordered retrieval in `LmdbSailDataset`

### 16.1 `getSupportedOrders(...)`

Given concrete `subj`, `pred`, `obj`, `contexts`:

1. convert known values to LMDB IDs where possible,
2. treat unknown or multiple-context requests conservatively,
3. compute best prefix length across configured indexes,
4. if best prefix length is `0`, return empty set,
5. for each index with that best prefix length:
   - find first varying field after the prefix,
   - map field to `StatementOrder`,
   - add to result

If multiple specific contexts are supplied, treat context as not fixed for support calculation.

### 16.2 `getStatements(StatementOrder, ...)`

Select the best index satisfying all of:

1. index has best prefix length for current concrete bindings,
2. first varying field after the prefix matches requested order,
3. requested order is actually supported for this call.

Then perform the same range or full scan logic as normal statement retrieval, but using that selected index.

### 16.3 Multi-context ordering

If multiple specific contexts are requested:

1. create one per-context ordered iterator,
2. merge them with a heap-based k-way merge using statement comparator for the requested order.

Do **not** concatenate with `UnionIteration`, because that destroys global order.

### 16.4 Comparator

Implement `getComparator()` as a value comparator consistent with LMDB index key order.

Use LMDB internal value IDs whenever possible.

---

## 17. Value comparator design

### 17.1 Correctness requirement

For ordered LMDB scans, the comparator must compare values in the same order used in the underlying LMDB keys.

### 17.2 Implementation rule

If both values are LMDB values with valid internal IDs for the current value-store revision:

```text
compare(left, right) = compareUnsigned(left.getInternalID(), right.getInternalID())
```

Otherwise:

1. try resolving IDs through the value store without creating new values,
2. if both resolve, compare resolved IDs,
3. otherwise fall back to generic RDF value comparison **outside** the LMDB ordered fast path only.

### 17.3 Why internal IDs are safe

LMDB stores encoded value IDs in keys, and the varint encoding preserves numeric and lexicographic order. Therefore comparing value IDs is comparator-consistent with iterator order.

---

## 18. Planned index-name annotation

Every chosen statement pattern should be annotated with the planner’s chosen index name.

### 18.1 Purpose

- explain plans
- regression tests
- troubleshooting
- validating access-path choice

### 18.2 Runtime overwrite

If runtime execution reports a different actual index, runtime metadata wins for diagnostics.

---

## 19. Mixed join-group fallback behavior

If the group contains non-`StatementPattern` arguments after priority removal:

1. optimize child joins recursively,
2. use the generic RDF4J join-order logic for that group,
3. do not attempt LMDB DP,
4. do not expose LMDB merge-join assumptions for that group.

This avoids complex mixed-node semantics in v1.

---

## 20. Examples

### 20.1 Example 1 — runtime-bound variable changes best access path

Query fragment:

```sparql
?org  :locatedIn  :Berlin .
?person :worksFor ?org .
?person :name ?n .
```

Configured indexes:

```text
spoc,posc
```

#### Step 1
Pattern `?org :locatedIn :Berlin`

- `posc` usable prefix = `2` (`p,o`)
- choose first

Assume output rows = 100 organizations.

#### Step 2
Pattern `?person :worksFor ?org`

Current bound vars = `{org}`

- `spoc`: subject unbound => prefix `0`
- `posc`: predicate const + object bound => prefix `2`

Choose `posc`.

Now each organization produces a narrow probe, not a full scan.

#### Step 3
Pattern `?person :name ?n`

Current bound vars = `{org, person}`

- `spoc`: subject bound + predicate const => prefix `2`

Choose `spoc`.

Result: good left-deep nested-loop plan.

### 20.2 Example 2 — order support should not force downgrade

Pattern:

```sparql
?x :p ?y
```

Current bound vars = `{?x}`

Indexes:

```text
spoc,posc
```

- `spoc` prefix = `2`, next varying field = `o`, ordered by `?y`
- `posc` prefix = `1`, next varying field = `o`

Only `spoc` is retained because it has better prefix length.

Result: order by `?y` is supported from `spoc`; no downgrade.

### 20.3 Example 3 — full-scan order must not be advertised

Pattern:

```sparql
?s ?p ?o
```

Indexes:

```text
spoc,posc
```

Best prefix length = `0`.

Even though the store could physically emit a full scan in `s` or `p` order, the optimizer must return **no supported order** for this planning purpose.

---

## 21. Error handling

### 21.1 Unknown value IDs

If a compile-time constant does not exist in the value store, exact cardinality is `0` and the pattern should be strongly preferred because it collapses the join group quickly.

### 21.2 Statistics unavailable

If no statistics snapshot exists and build fails:

- use exact constant cardinality when available,
- otherwise use generic heuristics with conservative scan penalties,
- do not fail the query.

### 21.3 Ordered access unavailable through wrappers

If a wrapper dataset does not forward ordered APIs, ordered support is effectively disabled for that query shape.  
The optimizer must still produce correct unordered plans.

---

## 22. Complexity summary

### 22.1 DP search

For `n <= 8`:

- subsets: `2^n`
- states per subset: bounded by number of interesting orders + 1
- practical size is small because order dimension is at most one variable per step

### 22.2 Greedy search

Roughly `O(n^2 * i)` where `i` = number of indexes.

### 22.3 Statistics build

`O(mN)` where `m` = configured index count.

---

## 23. Verification checklist

The detailed design is fully implemented only if all statements below are true.

### Planner

- [ ] Statement patterns are costed against actual configured LMDB indexes
- [ ] Runtime-bound variables count toward usable prefix length
- [ ] Prefix length `0` never advertises order
- [ ] DP search retains order as a physical property
- [ ] Merge join compares against nested-loop alternative

### Store/runtime

- [ ] `LmdbSailDataset.getSupportedOrders(...)` implemented
- [ ] `LmdbSailDataset.getStatements(StatementOrder, ...)` implemented
- [ ] ordered multi-context reads merge globally
- [ ] value comparator is ID-based and order-consistent

### Statistics

- [ ] per-index prefix distinct counts built
- [ ] stats cached
- [ ] stats marked dirty on committed write transactions
- [ ] rebuild policy implemented

### Tests

- [ ] join order changes with index set
- [ ] join order changes with binding order
- [ ] merge join only appears when valid
- [ ] explain plan shows chosen access path
- [ ] ordered retrieval works across multiple contexts

---

## 24. Summary of design decisions

1. **LMDB-specific planner, not generic-core rewrite**
2. **Pure statement-pattern groups only** for LMDB DP in v1
3. **Runtime-bound variables count as prefix-fixed**
4. **Prefix distinct counts** are the required statistics
5. **Order is exposed only at best prefix quality and only if prefixLen > 0**
6. **Left-deep DP up to 8 patterns**, greedy above
7. **Ordered multi-context reads require k-way merge**
8. **Comparator must use LMDB internal IDs**

These are the implementation-critical decisions. Everything else is supporting detail.