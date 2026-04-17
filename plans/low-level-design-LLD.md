# RDF4J LMDB Join Order Optimizer — Low-Level Design (LLD)

**Document type:** Low-level design  
**Target module:** `core/sail/lmdb`  
**Audience:** Engineers implementing and reviewing code  
**Implementation mode:** file-by-file, class-by-class

---

## 1. Goal of this LLD

This LLD specifies the concrete code structure required to implement an LMDB-specific join order optimizer in RDF4J.

It answers:

- which files to add,
- which existing files to change,
- which methods to add or override,
- what each method must do,
- how data flows between planner and runtime execution.

---

## 2. Package layout

All new optimizer classes should live in:

```text
org.eclipse.rdf4j.sail.lmdb
```

### Why same package?

`TripleStore` and `LmdbEvaluationStatistics` are package-private/internal and the optimizer must access their internals directly without expanding public API more than necessary.

---

## 3. New files

Add the following files.

### 3.1 `LmdbEvaluationStrategyFactory.java`

```java
package org.eclipse.rdf4j.sail.lmdb;

public class LmdbEvaluationStrategyFactory extends StrictEvaluationStrategyFactory {
    public LmdbEvaluationStrategyFactory();
    public LmdbEvaluationStrategyFactory(FederatedServiceResolver resolver);

    @Override
    public EvaluationStrategy createEvaluationStrategy(
            Dataset dataset,
            TripleSource tripleSource,
            EvaluationStatistics evaluationStatistics);
}
```

### 3.2 `LmdbQueryOptimizerPipeline.java`

```java
package org.eclipse.rdf4j.sail.lmdb;

public class LmdbQueryOptimizerPipeline implements QueryOptimizerPipeline {
    public LmdbQueryOptimizerPipeline(
            EvaluationStrategy strategy,
            TripleSource tripleSource,
            EvaluationStatistics evaluationStatistics);

    @Override
    public Iterable<QueryOptimizer> getOptimizers();
}
```

### 3.3 `LmdbJoinOrderOptimizer.java`

```java
package org.eclipse.rdf4j.sail.lmdb;

public final class LmdbJoinOrderOptimizer implements QueryOptimizer {
    public LmdbJoinOrderOptimizer(
            LmdbEvaluationStatistics lmdbStatistics,
            boolean trackResultSize,
            TripleSource tripleSource,
            LmdbStoreConfig config);

    @Override
    public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings);
}
```

Use nested static records/classes for:

- `ResolvedPatternTerms`
- `ResolvedTerm`
- `AccessPath`
- `PlanItem`
- `PlanState`
- `IndexStatsRef`

### 3.4 `LmdbOptimizerStatisticsManager.java`

```java
package org.eclipse.rdf4j.sail.lmdb;

final class LmdbOptimizerStatisticsManager {

    static final class StatisticsSnapshot { ... }

    static final class IndexStatistics { ... }

    LmdbOptimizerStatisticsManager(TripleStore tripleStore, LmdbStoreConfig config);

    StatisticsSnapshot getSnapshot();

    void maybeRefresh();

    void forceRefresh();

    void markDirtyCommit();
}
```

### 3.5 `LmdbOrderedValueComparator.java`

```java
package org.eclipse.rdf4j.sail.lmdb;

final class LmdbOrderedValueComparator implements Comparator<Value> {
    LmdbOrderedValueComparator(ValueStore valueStore);

    @Override
    public int compare(Value left, Value right);
}
```

### 3.6 `LmdbOrderedStatementUnionIteration.java`

```java
package org.eclipse.rdf4j.sail.lmdb;

final class LmdbOrderedStatementUnionIteration
        extends LookAheadIteration<Statement>
        implements IndexReportingIterator {
    LmdbOrderedStatementUnionIteration(
            List<CloseableIteration<? extends Statement>> sources,
            Comparator<Statement> statementComparator,
            String indexNameHint);

    @Override
    protected Statement getNextElement();

    @Override
    protected void handleClose();

    @Override
    public String getIndexName();
}
```

### 3.7 `LmdbIndexDescriptor.java`

```java
package org.eclipse.rdf4j.sail.lmdb;

record LmdbIndexDescriptor(String name, char[] fieldSeq) { }
```

---

## 4. Existing files to change

### 4.1 `LmdbStore.java`

#### Changes

1. Default evaluation strategy factory becomes `LmdbEvaluationStrategyFactory`.
2. Add dirty-statistics hook.

#### Code changes

Add:

```java
void markOptimizerStatisticsDirty() {
    if (backingStore != null) {
        backingStore.getOptimizerStatisticsManager().markDirtyCommit();
    }
}
```

Modify `getEvaluationStrategyFactory()`:

```java
public synchronized EvaluationStrategyFactory getEvaluationStrategyFactory() {
    if (evalStratFactory == null) {
        evalStratFactory = new LmdbEvaluationStrategyFactory(getFederatedServiceResolver());
    }
    evalStratFactory.setQuerySolutionCacheThreshold(getIterationCacheSyncThreshold());
    evalStratFactory.setTrackResultSize(isTrackResultSize());
    evalStratFactory.setCollectionFactory(getCollectionFactory());
    return evalStratFactory;
}
```

### 4.2 `LmdbStoreConnection.java`

#### Changes

Mark optimizer stats dirty after successful commit when statements changed.

#### Code changes

In `commitInternal()`:

```java
@Override
protected void commitInternal() throws SailException {
    boolean statementsChanged = sailChangedEvent.statementsAdded() || sailChangedEvent.statementsRemoved();
    try {
        super.commitInternal();
    } finally {
        if (txnLock != null && txnLock.isActive()) {
            txnLock.release();
        }
    }

    if (statementsChanged) {
        lmdbStore.markOptimizerStatisticsDirty();
    }

    lmdbStore.notifySailChanged(sailChangedEvent);
    sailChangedEvent = new DefaultSailChangedEvent(lmdbStore);
}
```

No change on rollback.

### 4.3 `LmdbSailStore.java`

#### Changes

1. Create/store `LmdbOptimizerStatisticsManager`
2. Return `LmdbEvaluationStatistics` with stats manager
3. Add ordered statement iterator support
4. Implement dataset order APIs

#### New field

```java
private final LmdbOptimizerStatisticsManager optimizerStatisticsManager;
private final Comparator<Value> orderedValueComparator;
```

#### Constructor changes

After `tripleStore` initialization:

```java
this.optimizerStatisticsManager = new LmdbOptimizerStatisticsManager(tripleStore, config);
this.orderedValueComparator = new LmdbOrderedValueComparator(valueStore);
```

#### Getter

```java
LmdbOptimizerStatisticsManager getOptimizerStatisticsManager() {
    return optimizerStatisticsManager;
}
```

#### Evaluation statistics

Change:

```java
return new LmdbEvaluationStatistics(valueStore, tripleStore, optimizerStatisticsManager);
```

#### Overload `createStatementIterator`

Existing method remains and delegates:

```java
CloseableIteration<? extends Statement> createStatementIterator(
        Txn txn,
        Resource subj,
        IRI pred,
        Value obj,
        boolean explicit,
        Resource... contexts) throws IOException {
    return createStatementIterator(txn, subj, pred, obj, explicit, null, contexts);
}
```

Add overload:

```java
CloseableIteration<? extends Statement> createStatementIterator(
        Txn txn,
        Resource subj,
        IRI pred,
        Value obj,
        boolean explicit,
        StatementOrder statementOrder,
        Resource... contexts) throws IOException
```

#### Overload algorithm

1. Resolve subject/predicate/object IDs exactly as existing method.
2. Build `contextIDList` exactly as existing method.
3. If `statementOrder == null`:
   - preserve current behavior.
4. Else:
   - if `contextIDList.size() == 1`
     - call `tripleStore.getTriplesOrdered(...)`
   - else
     - for each context ID, create ordered iterator using `getTriplesOrdered(...)`
     - wrap in `LmdbOrderedStatementUnionIteration`
     - use `statementOrder.getComparator(orderedValueComparator)` as statement comparator

#### `LmdbSailDataset` methods

Replace stubs with:

```java
@Override
public CloseableIteration<? extends Statement> getStatements(
        StatementOrder statementOrder,
        Resource subj,
        IRI pred,
        Value obj,
        Resource... contexts) throws SailException {
    try {
        return createStatementIterator(txn, subj, pred, obj, explicit, statementOrder, contexts);
    } catch (IOException e) {
        throw new SailException("Unable to get ordered statements", e);
    }
}

@Override
public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
    try {
        long subjID = resolveIdOrUnknown(subj);
        long predID = resolveIdOrUnknown(pred);
        long objID = resolveIdOrUnknown(obj);

        long contextIDForSupport;
        if (contexts == null || contexts.length != 1) {
            contextIDForSupport = LmdbValue.UNKNOWN_ID;
        } else {
            contextIDForSupport = contexts[0] == null ? 0L : resolveIdOrUnknown(contexts[0]);
        }

        return tripleStore.getSupportedOrders(subjID, predID, objID, contextIDForSupport);
    } catch (IOException e) {
        logger.warn("Unable to determine supported orders", e);
        return Set.of();
    }
}

@Override
public Comparator<Value> getComparator() {
    return orderedValueComparator;
}
```

Add helper:

```java
private long resolveIdOrUnknown(Value value) throws IOException { ... }
```

### 4.4 `LmdbEvaluationStatistics.java`

#### Changes

Add stats-manager and accessors for optimizer.

#### New fields

```java
private final LmdbOptimizerStatisticsManager optimizerStatisticsManager;
```

#### Constructor

```java
public LmdbEvaluationStatistics(
        ValueStore valueStore,
        TripleStore tripleStore,
        LmdbOptimizerStatisticsManager optimizerStatisticsManager) {
    this.valueStore = valueStore;
    this.tripleStore = tripleStore;
    this.optimizerStatisticsManager = optimizerStatisticsManager;
}
```

#### Add package-private accessors

```java
ValueStore getValueStore() {
    return valueStore;
}

TripleStore getTripleStore() {
    return tripleStore;
}

LmdbOptimizerStatisticsManager getOptimizerStatisticsManager() {
    return optimizerStatisticsManager;
}

double cardinality(Resource subj, IRI pred, Value obj, Resource context) throws IOException {
    ...
}

long getValueId(Value value) throws IOException {
    if (value == null) {
        return LmdbValue.UNKNOWN_ID;
    }
    return valueStore.getId(value);
}
```

Do **not** change semantic behavior of existing `getCardinality(StatementPattern)` override except to call package-private `cardinality(...)`.

### 4.5 `TripleStore.java`

#### Changes

1. Expose immutable index descriptors
2. Implement order-aware index selection
3. Expose supported orders
4. Add ordered-triples access method
5. Add statistics snapshot builder

#### Add helper record/class

Inside or next to `TripleStore`:

```java
static final class IndexSelection {
    final TripleIndex index;
    final String indexName;
    final int prefixLen;
    final boolean rangeSearch;
    final StatementOrder order; // nullable
}
```

#### Add descriptor getter

```java
List<LmdbIndexDescriptor> getIndexDescriptors() {
    ArrayList<LmdbIndexDescriptor> list = new ArrayList<>(indexes.size());
    for (TripleIndex index : indexes) {
        list.add(new LmdbIndexDescriptor(new String(index.getFieldSeq()), index.getFieldSeq().clone()));
    }
    return List.copyOf(list);
}
```

#### Add support-order method

```java
Set<StatementOrder> getSupportedOrders(long subj, long pred, long obj, long context) {
    int bestPrefix = 0;
    for (TripleIndex index : indexes) {
        bestPrefix = Math.max(bestPrefix, index.getPatternScore(subj, pred, obj, context));
    }

    if (bestPrefix == 0 || bestPrefix >= 4) {
        return Set.of();
    }

    EnumSet<StatementOrder> result = EnumSet.noneOf(StatementOrder.class);
    for (TripleIndex index : indexes) {
        if (index.getPatternScore(subj, pred, obj, context) != bestPrefix) {
            continue;
        }
        char nextField = index.getFieldSeq()[bestPrefix];
        switch (nextField) {
            case 's' -> result.add(StatementOrder.S);
            case 'p' -> result.add(StatementOrder.P);
            case 'o' -> result.add(StatementOrder.O);
            case 'c' -> result.add(StatementOrder.C);
            default -> throw new IllegalStateException("Unexpected field: " + nextField);
        }
    }
    return result;
}
```

#### Add ordered index selection

```java
IndexSelection selectBestIndex(long subj, long pred, long obj, long context) { ... }

IndexSelection selectBestIndexForOrder(
        long subj, long pred, long obj, long context, StatementOrder requestedOrder) { ... }
```

`selectBestIndexForOrder(...)` algorithm:

1. compute `bestPrefix` across all indexes
2. if `bestPrefix == 0`, return `null`
3. retain indexes with `prefixLen == bestPrefix`
4. keep those whose first varying field after prefix matches requested order
5. choose first retained index in configured order
6. return `IndexSelection`

#### Add ordered triples method

```java
RecordIterator getTriplesOrdered(
        Txn txn,
        StatementOrder statementOrder,
        long subj,
        long pred,
        long obj,
        long context,
        boolean explicit) throws IOException
```

Implementation:

1. call `selectBestIndexForOrder(...)`
2. if selection is null:
   - fall back to `getTriples(...)` or throw `IllegalArgumentException`
   - recommended: fall back to `getTriples(...)` and log debug
3. use `getTriplesUsingIndex(...)` with selected index and selected range-search mode

#### Add stats snapshot builder

```java
LmdbOptimizerStatisticsManager.StatisticsSnapshot buildOptimizerStatisticsSnapshot() throws IOException
```

Implementation:

1. iterate `indexes`
2. scan explicit and inferred DBs
3. compute rows + prefix distinct counts
4. populate immutable snapshot

#### Add scan helper

```java
private LmdbOptimizerStatisticsManager.IndexStatistics buildIndexStatistics(TripleIndex index) throws IOException
```

#### Prefix-change helper

```java
private static boolean prefixChanged(long[] prev, long[] current, int prefixLen) { ... }
```

### 4.6 `config/LmdbStoreConfig.java`

#### Add fields

```java
private boolean queryOptimizerEnabled = true;
private int queryOptimizerDynamicProgrammingThreshold = 8;
private boolean queryOptimizerMergeJoinEnabled = true;
private boolean queryOptimizerStatisticsEnabled = true;
private boolean queryOptimizerStatisticsBuildOnInit = false;
private int queryOptimizerStatisticsRefreshCommitThreshold = 100;
```

#### Add getters/setters

```java
public boolean getQueryOptimizerEnabled();
public void setQueryOptimizerEnabled(boolean value);

public int getQueryOptimizerDynamicProgrammingThreshold();
public void setQueryOptimizerDynamicProgrammingThreshold(int value);

public boolean getQueryOptimizerMergeJoinEnabled();
public void setQueryOptimizerMergeJoinEnabled(boolean value);

public boolean getQueryOptimizerStatisticsEnabled();
public void setQueryOptimizerStatisticsEnabled(boolean value);

public boolean getQueryOptimizerStatisticsBuildOnInit();
public void setQueryOptimizerStatisticsBuildOnInit(boolean value);

public int getQueryOptimizerStatisticsRefreshCommitThreshold();
public void setQueryOptimizerStatisticsRefreshCommitThreshold(int value);
```

### 4.7 RDF config schema/factory files (if repository-config serialization is required)

Update:

- `config/LmdbStoreSchema.java`
- `config/LmdbStoreFactory.java`
- any config export/parser classes used by RDF4J LMDB config

Add IRIs:

```text
lmdb:queryOptimizerEnabled
lmdb:queryOptimizerDynamicProgrammingThreshold
lmdb:queryOptimizerMergeJoinEnabled
lmdb:queryOptimizerStatisticsEnabled
lmdb:queryOptimizerStatisticsBuildOnInit
lmdb:queryOptimizerStatisticsRefreshCommitThreshold
```

### 4.8 Audit any LMDB-used `SailDataset` wrappers

Any wrapper between query evaluation and `LmdbSailDataset` must forward:

- `getStatements(StatementOrder, ...)`
- `getSupportedOrders(...)`
- `getComparator()`

If an existing wrapper drops these, add delegation.

---

## 5. `LmdbEvaluationStrategyFactory` implementation

### 5.1 Behavior

If LMDB optimizer feature flag is enabled and `evaluationStatistics instanceof LmdbEvaluationStatistics`, install `LmdbQueryOptimizerPipeline`.  
Otherwise install standard pipeline.

### 5.2 Method body

```java
@Override
public EvaluationStrategy createEvaluationStrategy(
        Dataset dataset,
        TripleSource tripleSource,
        EvaluationStatistics evaluationStatistics) {

    StrictEvaluationStrategy strategy = new StrictEvaluationStrategy(
            tripleSource,
            dataset,
            getFederatedServiceResolver(),
            getQuerySolutionCacheThreshold(),
            evaluationStatistics,
            isTrackResultSize());

    if (evaluationStatistics instanceof LmdbEvaluationStatistics lmdbStats
            && lmdbStats.getOptimizerStatisticsManager() != null) {
        strategy.setOptimizerPipeline(
                new LmdbQueryOptimizerPipeline(strategy, tripleSource, lmdbStats));
    } else {
        strategy.setOptimizerPipeline(
                new StandardQueryOptimizerPipeline(strategy, tripleSource, evaluationStatistics));
    }

    strategy.setCollectionFactory(collectionFactorySupplier);
    return strategy;
}
```

---

## 6. `LmdbQueryOptimizerPipeline` implementation

### 6.1 Required optimizer order

Preserve the same optimizer order as `StandardQueryOptimizerPipeline` and replace only the join optimizer.

```java
List<QueryOptimizer> optimizers = List.of(
    StandardQueryOptimizerPipeline.BINDING_ASSIGNER,
    StandardQueryOptimizerPipeline.BINDING_SET_ASSIGNMENT_INLINER,
    new ConstantOptimizer(strategy),
    new RegexAsStringFunctionOptimizer(tripleSource.getValueFactory()),
    StandardQueryOptimizerPipeline.COMPARE_OPTIMIZER,
    StandardQueryOptimizerPipeline.CONJUNCTIVE_CONSTRAINT_SPLITTER,
    StandardQueryOptimizerPipeline.DISJUNCTIVE_CONSTRAINT_OPTIMIZER,
    StandardQueryOptimizerPipeline.SAME_TERM_FILTER_OPTIMIZER,
    StandardQueryOptimizerPipeline.UNION_SCOPE_CHANGE_OPTIMIZER,
    StandardQueryOptimizerPipeline.QUERY_MODEL_NORMALIZER,
    StandardQueryOptimizerPipeline.PROJECTION_REMOVAL_OPTIMIZER,
    new LmdbJoinOrderOptimizer(lmdbStats, strategy.isTrackResultSize(), tripleSource, config),
    StandardQueryOptimizerPipeline.ITERATIVE_EVALUATION_OPTIMIZER,
    StandardQueryOptimizerPipeline.FILTER_OPTIMIZER,
    StandardQueryOptimizerPipeline.ORDER_LIMIT_OPTIMIZER
);
```

If `evaluationStatistics` is not LMDB-specific, fall back to standard pipeline.

---

## 7. `LmdbJoinOrderOptimizer` implementation

### 7.1 Internal nested types

Use nested static types inside `LmdbJoinOrderOptimizer`.

#### `enum PositionState`

```java
enum PositionState {
    CONST,
    BOUND,
    UNBOUND,
    ABSENT
}
```

#### `record ResolvedTerm`

```java
record ResolvedTerm(
        char field,
        PositionState state,
        Value value,
        String varName) {
}
```

#### `record ResolvedPatternTerms`

```java
record ResolvedPatternTerms(
        StatementPattern statementPattern,
        ResolvedTerm s,
        ResolvedTerm p,
        ResolvedTerm o,
        ResolvedTerm c) {

    ResolvedTerm forField(char field) { ... }
}
```

#### `record AccessPath`

```java
record AccessPath(
        String indexName,
        char[] fieldSeq,
        int prefixLen,
        double scanRowsPerProbe,
        double outRowsPerProbe,
        double probeCost,
        String orderedByVarName,
        StatementOrder requestedOrder) {
}
```

#### `record PlanItem`

```java
record PlanItem(
        StatementPattern statementPattern,
        String chosenIndexName,
        String chosenOrderVarName,
        JoinMethod joinMethodToPrevious,
        String mergeJoinVarName) {
}
```

#### `enum JoinMethod`

```java
enum JoinMethod {
    NONE,
    NESTED_LOOP,
    MERGE
}
```

#### `record PlanStateKey`

```java
record PlanStateKey(int subsetMask, String orderedByVarName) { }
```

#### `record PlanState`

```java
record PlanState(
        int subsetMask,
        String orderedByVarName,
        double cost,
        double rows,
        Set<String> boundVarNames,
        List<PlanItem> items) {
}
```

### 7.2 Main constructor

```java
public LmdbJoinOrderOptimizer(
        LmdbEvaluationStatistics lmdbStatistics,
        boolean trackResultSize,
        TripleSource tripleSource,
        LmdbStoreConfig config) {
    ...
}
```

Store:

- `lmdbStatistics`
- `genericStatistics = lmdbStatistics`
- `tripleSource`
- `trackResultSize`
- `dpThreshold`
- `mergeJoinEnabled`

### 7.3 `optimize(...)`

Implementation:

1. `tupleExpr.visit(new JoinVisitor(bindings));`

### 7.4 `JoinVisitor`

Use a visitor similar in structure to the existing `QueryJoinOptimizer`, but do **not** subclass the generic optimizer. Copy the relevant logic and modify join planning.

Maintain field:

```java
private Set<String> boundVars = new HashSet<>();
private final BindingSet initialBindings;
```

### 7.5 `meet(LeftJoin)`

Same behavior as generic optimizer.

### 7.6 `meet(StatementPattern)`

Set result size estimate using `lmdbStatistics.getCardinality(node)`.

### 7.7 `meet(Join)`

Algorithm:

1. capture `origBoundVars`
2. flatten join args
3. separate priority args:
   - extensions / BIND
   - subselects
4. optimize priority args in current behavior
5. inspect remaining args
6. if all remaining args are `StatementPattern`
   - plan via `planPureStatementPatterns(...)`
7. else
   - use copied generic greedy ordering logic
8. rebuild join tree
9. restore `boundVars`

### 7.8 `planPureStatementPatterns(...)`

Signature:

```java
private TupleExpr planPureStatementPatterns(
        List<StatementPattern> patterns,
        Set<String> outerBoundVars,
        BindingSet initialBindings)
```

Algorithm:

1. if `patterns.size() <= dpThreshold`
   - `PlanState best = planDp(...)`
2. else
   - `PlanState best = planGreedy(...)`
3. build final join tree from `best`
4. return tree

### 7.9 `resolvePatternTerms(...)`

Signature:

```java
private ResolvedPatternTerms resolvePatternTerms(
        StatementPattern sp,
        Set<String> boundVarNames,
        BindingSet initialBindings)
```

Rules:

- if `var.hasValue()` => `CONST`
- else if `initialBindings.hasBinding(var.getName())` => `CONST`
- else if `boundVarNames.contains(var.getName())` => `BOUND`
- else => `UNBOUND`
- if `contextVar == null` => `ABSENT`

### 7.10 `enumerateAccessPaths(...)`

Signature:

```java
private List<AccessPath> enumerateAccessPaths(
        StatementPattern sp,
        Set<String> boundVarNames,
        BindingSet initialBindings)
```

Algorithm:

1. resolve terms
2. loop over `lmdbStatistics.getTripleStore().getIndexDescriptors()`
3. compute prefix length per index
4. find `bestPrefix`
5. if `bestPrefix == 0`
   - create one unordered access path only, based on the best default index and no order property
6. else
   - retain only indexes with prefix == `bestPrefix`
   - create one candidate per retained index
7. deduplicate by `(indexName, orderedByVarName)`

### 7.11 `computePrefixLen(...)`

```java
private int computePrefixLen(ResolvedPatternTerms terms, char[] fieldSeq) { ... }
```

Implementation:

```java
int prefix = 0;
for (char field : fieldSeq) {
    PositionState s = terms.forField(field).state();
    if (s == PositionState.CONST || s == PositionState.BOUND) {
        prefix++;
    } else {
        break;
    }
}
return prefix;
```

### 7.12 `computeOrderedByVar(...)`

```java
private String computeOrderedByVar(ResolvedPatternTerms terms, char[] fieldSeq, int prefixLen)
```

Rules:

- if `prefixLen <= 0 || prefixLen >= 4` => `null`
- `char nextField = fieldSeq[prefixLen]`
- if `terms.forField(nextField).state() == PositionState.UNBOUND` => its var name
- else => `null`

### 7.13 `estimateScanRowsPerProbe(...)`

```java
private double estimateScanRowsPerProbe(
        ResolvedPatternTerms terms,
        AccessPathSeed seed)
```

Use:

- `totalRows` if prefix `0`
- exact prefix cardinality if all prefix fields are `CONST`
- stats snapshot average rows if any prefix field is `BOUND`

### 7.14 `estimateOutRowsPerProbe(...)`

```java
private double estimateOutRowsPerProbe(
        ResolvedPatternTerms terms,
        AccessPathSeed seed,
        double scanRowsPerProbe,
        StatisticsSnapshot snapshot)
```

Use exact full cardinality when all fixed fields are compile-time constants; otherwise apply selectivity factors.

### 7.15 `estimateProbeCost(...)`

```java
private double estimateProbeCost(double scanRowsPerProbe, double outRowsPerProbe)
```

Formula:

```java
return 5.0 + scanRowsPerProbe + (0.25 * outRowsPerProbe);
```

### 7.16 `planDp(...)`

Signature:

```java
private PlanState planDp(
        List<StatementPattern> patterns,
        Set<String> outerBoundVars,
        BindingSet initialBindings)
```

Implementation outline:

1. assign bit position to each statement pattern
2. create singleton states
3. dynamic-program across subset sizes
4. store best state by `(subsetMask, orderedByVarName)`
5. return best full-subset state

### 7.17 `createSingletonStates(...)`

For each pattern:

- enumerate access paths
- for each path:
  - `rows = path.outRowsPerProbe()`
  - `cost = path.probeCost()`
  - `boundVarNames = outerBoundVars + pattern.getBindingNames()`
  - `orderedByVarName = path.orderedByVarName()`
  - `items = List.of(new PlanItem(...))`

Also keep an unordered singleton state even if ordered states exist.

### 7.18 `transition(...)`

Signature:

```java
private Stream<PlanState> transition(
        PlanState state,
        StatementPattern next,
        int nextBit,
        BindingSet initialBindings)
```

Generate:

- nested-loop state
- optional merge state

#### Nested loop

```java
newCost = state.cost() + (state.rows() * accessPath.probeCost())
newRows = state.rows() * accessPath.outRowsPerProbe()
newOrderedBy = null
```

#### Merge join

Allowed only if:

- `state.orderedByVarName() != null`
- `state.orderedByVarName().equals(accessPath.orderedByVarName())`
- shared binding names contain that var
- merge join feature enabled
- cardinality ratio acceptable

Then:

```java
newCost = state.cost() + state.rows() + accessPath.outRowsPerProbe()
newRows = state.rows() * accessPath.outRowsPerProbe()
newOrderedBy = state.orderedByVarName()
```

### 7.19 `planGreedy(...)`

Signature:

```java
private PlanState planGreedy(
        List<StatementPattern> patterns,
        Set<String> outerBoundVars,
        BindingSet initialBindings)
```

Algorithm:

1. choose cheapest singleton state
2. while remaining patterns exist:
   - evaluate all transitions from current state
   - pick cheapest
3. return final state

### 7.20 `buildJoinTree(...)`

Signature:

```java
private TupleExpr buildJoinTree(PlanState state)
```

Algorithm:

1. clone statement patterns in chosen order
2. apply statement metadata:
   - `sp.setIndexName(chosenIndexName)`
   - if `chosenOrderVarName != null`, and direct statement ordering is required for runtime, call `sp.setOrder(var)`
3. build right-recursive joins
4. if `joinMethodToPrevious == MERGE`
   - `join.setMergeJoin(true)`
   - `join.setOrder(varFor(mergeJoinVarName))`

### 7.21 Var resolution helper

```java
private Var varForName(StatementPattern sp, String varName)
```

Returns the actual `Var` instance from `sp` matching the name.

---

## 8. `LmdbOptimizerStatisticsManager` implementation

### 8.1 Fields

```java
private final TripleStore tripleStore;
private final LmdbStoreConfig config;
private final ReentrantLock rebuildLock = new ReentrantLock();
private final AtomicLong dirtyCommitCount = new AtomicLong();
private volatile StatisticsSnapshot snapshot;
```

### 8.2 `getSnapshot()`

```java
StatisticsSnapshot getSnapshot() {
    maybeRefresh();
    return snapshot;
}
```

### 8.3 `maybeRefresh()`

```java
void maybeRefresh() {
    if (!config.getQueryOptimizerStatisticsEnabled()) {
        return;
    }

    StatisticsSnapshot current = snapshot;
    if (current == null
            || dirtyCommitCount.get() >= config.getQueryOptimizerStatisticsRefreshCommitThreshold()) {
        forceRefresh();
    }
}
```

### 8.4 `forceRefresh()`

```java
void forceRefresh() {
    if (!rebuildLock.tryLock()) {
        return;
    }
    try {
        snapshot = tripleStore.buildOptimizerStatisticsSnapshot();
        dirtyCommitCount.set(0);
    } catch (IOException e) {
        // log and keep previous snapshot
    } finally {
        rebuildLock.unlock();
    }
}
```

### 8.5 `markDirtyCommit()`

```java
void markDirtyCommit() {
    dirtyCommitCount.incrementAndGet();
}
```

---

## 9. `LmdbOrderedValueComparator` implementation

### 9.1 Fields

```java
private final ValueStore valueStore;
private final Comparator<Value> fallback = new ValueComparator();
```

### 9.2 `compare(...)`

Implementation:

```java
@Override
public int compare(Value left, Value right) {
    if (left == right) {
        return 0;
    }

    long leftId = resolveId(left);
    long rightId = resolveId(right);

    if (leftId != LmdbValue.UNKNOWN_ID && rightId != LmdbValue.UNKNOWN_ID) {
        return Long.compareUnsigned(leftId, rightId);
    }

    return fallback.compare(left, right);
}
```

### 9.3 `resolveId(...)`

```java
private long resolveId(Value value) {
    if (value instanceof LmdbValue lv) {
        long id = lv.getInternalID();
        if (id != LmdbValue.UNKNOWN_ID) {
            return id;
        }
        try {
            lv.init();
            return lv.getInternalID();
        } catch (Exception ignore) {
            // fall through
        }
    }

    try {
        return valueStore.getId(value);
    } catch (IOException e) {
        return LmdbValue.UNKNOWN_ID;
    }
}
```

---

## 10. `LmdbOrderedStatementUnionIteration` implementation

### 10.1 Purpose

Globally merge multiple already-sorted ordered iterators into a single sorted iterator.

### 10.2 Data structures

```java
private static final class Head {
    final CloseableIteration<? extends Statement> source;
    final Statement statement;
}
```

Use:

```java
private final PriorityQueue<Head> queue;
private final List<CloseableIteration<? extends Statement>> allSources;
private final String indexNameHint;
```

### 10.3 Constructor

1. prime every source iterator
2. insert first statement of each non-empty source into the queue
3. comparator = compare `Head.statement` using provided statement comparator

### 10.4 `getNextElement()`

1. poll smallest head
2. emit its statement
3. advance same source
4. if source has next, push replacement head

### 10.5 `getIndexName()`

Return:

- exact index name if all inputs used same index
- otherwise `ordered-merge(<indexNameHint>)`

---

## 11. `TripleStore` statistics-scan details

### 11.1 Record decoding

For stats scanning, decode keys in index order directly into a `long[4]`.

Use existing varint decoding helpers; do not allocate per row.

### 11.2 Single-pass distinct counting

Maintain:

```java
long[] prev = new long[4];
boolean first = true;
long rows = 0;
long[] distinct = new long[5]; // ignore 0
```

For each key:

```java
rows++;
if (first) {
    distinct[1]++; distinct[2]++; distinct[3]++; distinct[4]++;
    copy current -> prev;
    first = false;
} else {
    if (prefixChanged(prev, current, 1)) distinct[1]++;
    if (prefixChanged(prev, current, 2)) distinct[2]++;
    if (prefixChanged(prev, current, 3)) distinct[3]++;
    if (prefixChanged(prev, current, 4)) distinct[4]++;
    copy current -> prev;
}
```

### 11.3 Prefix changed function

```java
private static boolean prefixChanged(long[] prev, long[] current, int prefixLen) {
    for (int i = 0; i < prefixLen; i++) {
        if (prev[i] != current[i]) {
            return true;
        }
    }
    return false;
}
```

---

## 12. Ordered access path selection in `TripleStore`

### 12.1 Requested-order matching

Map requested `StatementOrder` to expected first varying field:

- `S` -> `'s'`
- `P` -> `'p'`
- `O` -> `'o'`
- `C` -> `'c'`

### 12.2 Selection algorithm

```text
bestPrefix = max(patternScore(index))

if bestPrefix == 0:
    return null

for index in configured order:
    if patternScore(index) != bestPrefix:
        continue
    if index.fieldSeq[bestPrefix] == requestedField:
        return index
return null
```

Configured-order iteration preserves deterministic tie-breaking.

---

## 13. Build-order of implementation tasks

Implement in this exact order.

### Phase 1 — Plumbing

1. add config fields
2. add `LmdbEvaluationStrategyFactory`
3. add `LmdbQueryOptimizerPipeline`
4. wire `LmdbStore` to use the factory

### Phase 2 — Statistics

5. add `LmdbOptimizerStatisticsManager`
6. add `TripleStore.buildOptimizerStatisticsSnapshot()`
7. add dirty-marking from `LmdbStoreConnection`

### Phase 3 — Ordered runtime support

8. implement `TripleStore.getSupportedOrders(...)`
9. implement `TripleStore.getTriplesOrdered(...)`
10. add `LmdbOrderedValueComparator`
11. add `LmdbOrderedStatementUnionIteration`
12. implement `LmdbSailDataset` ordered methods

### Phase 4 — Planner

13. implement `LmdbJoinOrderOptimizer` with:
    - generic traversal
    - pure statement-pattern DP/greedy planning
    - mixed-group fallback
14. integrate planner into pipeline

### Phase 5 — Tests and polish

15. explain/plan metadata
16. debug logging
17. full regression and benchmark suite

---

## 14. Test classes to add

### 14.1 Unit tests

#### `LmdbOptimizerStatisticsManagerTest`

- builds snapshot for default indexes
- counts prefix distinct values correctly
- refreshes after dirty threshold

#### `TripleStoreOrderSupportTest`

- reports supported orders correctly for:
  - `spoc`
  - `posc`
  - mixed configured index sets
- returns empty set for prefix length `0`

#### `LmdbOrderedValueComparatorTest`

- internal-ID ordering works
- equal IDs compare equal
- unknown IDs fall back safely

#### `LmdbJoinOrderOptimizerAccessPathTest`

- runtime-bound vars contribute to prefix length
- lower-prefix ordered path is rejected
- prefix length `0` suppresses ordered candidates

### 14.2 Integration tests

#### `LmdbJoinOrderOptimizerIntegrationTest`

Use real LMDB store and assert:

- different index sets yield different join orders
- same query with different leading selective pattern yields different plan
- bound-variable-sensitive planning occurs

#### `LmdbMergeJoinIntegrationTest`

- ordered scan support is visible to execution
- merge join selected only on valid conditions

#### `LmdbOrderedMultiContextIterationTest`

- multiple specific contexts remain globally ordered

#### `LmdbMixedJoinFallbackTest`

- mixed join group uses generic fallback and stays correct

### 14.3 Explain-plan / metadata tests

#### `LmdbPlanMetadataTest`

- statement patterns carry chosen index name
- merge-join flag and order variable present where expected

---

## 15. Benchmarks to add

### 15.1 Query-shape set

Benchmark at least:

1. subject-star
2. object-star
3. predicate-object chain
4. path / snowflake
5. cross-join-avoidance case
6. context-heavy case

### 15.2 Index configurations

Run at least:

- `spoc,posc`
- `spoc,sopc,posc`
- `spoc,posc,opsc`
- workload-specific custom configuration

### 15.3 Success criteria

- LMDB-specific optimizer must not regress correctness
- median planning time increase acceptable
- execution-time improvement on targeted workloads visible
- no catastrophic full-scan plans where good prefix probes exist

---

## 16. Logging points

Add debug logs at:

1. stats snapshot build start/end
2. dirty-count refresh trigger
3. join group entering LMDB DP
4. join group falling back to generic planning
5. access paths enumerated for each statement pattern
6. final chosen plan for each join group
7. ordered-scan fallback when requested order cannot be satisfied

---

## 17. Thread safety

### 17.1 Stats manager

- `snapshot` is volatile
- rebuild guarded by `ReentrantLock`
- dirty count is atomic

### 17.2 Planner

Planner is request-scoped.  
No shared mutable planner state beyond immutable config/statistics snapshot references.

### 17.3 Comparator

`LmdbOrderedValueComparator` is stateless apart from store reference and is thread-safe if `ValueStore.getId(...)` is thread-safe for reads in the current LMDB store design.

---

## 18. Edge cases to cover explicitly

1. unknown compile-time constant value ID => cardinality `0`
2. context var absent
3. multiple specific contexts
4. null context
5. repeated variable in one statement pattern
6. merge-join candidate with huge cardinality imbalance
7. no stats snapshot available
8. stats snapshot stale but rebuild skipped due to lock contention
9. user-provided custom evaluation strategy factory
10. feature flags disabled

---

## 19. Definition of done for the code

The code is done only if:

- [ ] LMDB uses `LmdbEvaluationStrategyFactory` by default
- [ ] LMDB-specific pipeline is installed
- [ ] `LmdbJoinOrderOptimizer` plans pure statement-pattern groups
- [ ] `TripleStore` exposes index descriptors and order support
- [ ] `LmdbSailDataset` implements ordered retrieval
- [ ] prefix statistics are built and refreshed
- [ ] tests in sections 14 and 15 pass
- [ ] explain metadata is present
- [ ] feature flag can disable the new planner cleanly

---

## 20. Final coding guidance

1. **Copy and adapt** generic join-optimizer logic; do not try to subclass it deeply.
2. **Keep optimizer LMDB-local**.
3. **Do not advertise order for prefix length 0**.
4. **Do not concatenate ordered per-context iterators**; always merge them.
5. **Use internal value IDs for comparator consistency**.
6. **Never let stats failure fail a query**.
7. **Keep deterministic tie-breaking** everywhere.