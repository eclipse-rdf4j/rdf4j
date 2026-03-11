# SketchBasedJoinEstimator join-order refactor

This file contains the exact changes I recommend in `SketchBasedJoinEstimator.java` to make join reordering use one clear algorithm.

## 1) Remove the old planner machinery

Delete these members entirely:

- `DP_PARETO_CAP`
- `planJoinOrderByPairExpansion(...)`
- `sharesJoinVariable(TupleExpr, TupleExpr)`
- `estimatePlannerPrefixRows(...)`
- `estimatePlannerListRows(...)`
- `tupleExprsForOrder(...)`
- `planJoinOrderGreedy(...)`
- `planJoinOrderDynamicProgramming(...)`
- `addPlannerState(...)`
- `selectBestPlannerState(...)`
- `isBetterPlannerState(...)`
- `comparePlannerStates(...)`
- `dominates(...)`
- `seedState(...)`
- `appendState(...)`
- `JoinPlannerState`
- `PlannerTuple`
- `estimateJoinRowsUsingPublicCardinality(...)`
- `cloneTupleExpr(...)`

## 2) Replace `planJoinOrder(...)`

```java
public Optional<JoinOrderPlanner.JoinOrderPlan> planJoinOrder(List<TupleExpr> args, Set<String> initiallyBoundVars,
		JoinOrderPlanner.Algorithm algorithm) {
	if (!isReady() || args == null || args.isEmpty()) {
		return Optional.empty();
	}

	return new SketchJoinOrderReorderer(this).plan(args, initiallyBoundVars, algorithm);
}
```

## 3) Replace `toPlannerTuple(...)` with `toPlannerTupleEstimate(...)`

```java
private TuplePlanEstimate toPlannerTupleEstimate(TupleExpr tupleExpr, Set<String> initiallyBoundVars) {
	TuplePlanEstimate estimate;
	if (tupleExpr instanceof BindingSetAssignment) {
		estimate = estimateBindingSetAssignment((BindingSetAssignment) tupleExpr);
	} else {
		PatternEstimateInput input = asSketchCompatibleInput(tupleExpr);
		if (input == null) {
			return null;
		}
		StatementPattern pattern = input.pattern;
		double baseRows = normalizeRows(estimatePatternRows(pattern));
		double outputRows = normalizeRows(applyFilterMultiplier(baseRows, input.filterMultiplier));
		Map<String, VarPlanStats> varStats = new HashMap<>();
		Set<String> seenVars = new HashSet<>();
		for (Var var : pattern.getVarList()) {
			if (var == null || var.hasValue() || var.getName() == null || !seenVars.add(var.getName())) {
				continue;
			}
			JoinEstimate joinEstimate = estimate(getComponent(pattern, var),
					getValueOrNull(pattern.getSubjectVar()),
					getValueOrNull(pattern.getPredicateVar()),
					getValueOrNull(pattern.getObjectVar()),
					getValueOrNull(pattern.getContextVar()));
			double distinct = clampDistinct(joinEstimate.distinct, baseRows);
			if (Double.isFinite(distinct) && distinct > 0.0d) {
				varStats.put(var.getName(), new VarPlanStats(distinct, joinEstimate.bindings));
			}
		}
		estimate = new TuplePlanEstimate(baseRows, outputRows, input.filterMultiplier, varStats);
	}
	return applyInitiallyBoundVars(estimate, initiallyBoundVars);
}
```

## 4) Add the helper surface used by the new reorderer

```java
TuplePlanEstimate planEstimateForJoinOrdering(TupleExpr tupleExpr, Set<String> initiallyBoundVars) {
	return toPlannerTupleEstimate(tupleExpr, initiallyBoundVars);
}

JoinStepEstimate estimateJoinStepForJoinOrdering(TuplePlanEstimate left, TuplePlanEstimate right) {
	return estimateJoinStep(left, right);
}

TuplePlanEstimate joinedPlanEstimate(JoinStepEstimate step) {
	return new TuplePlanEstimate(step.outputRows, step.outputRows, 1.0d, step.varStats);
}

boolean hasSharedJoinVariable(TuplePlanEstimate left, TuplePlanEstimate right) {
	for (String varName : left.varStats.keySet()) {
		if (right.varStats.containsKey(varName)) {
			return true;
		}
	}
	return false;
}
```

## 5) Simplify `estimateJoinStep(...)`

Replace the current signature and body with this version.

```java
private JoinStepEstimate estimateJoinStep(TuplePlanEstimate left, TuplePlanEstimate right) {
	if (left.outputRows <= 0.0d || right.baseRows <= 0.0d) {
		return new JoinStepEstimate(0.0d, 0.0d, Collections.emptyMap());
	}

	double disconnectedRows = estimateDisconnectedJoinRows(left.outputRows, right.baseRows);
	Map<String, SharedVarEstimate> sharedVars = new HashMap<>();
	double rawRows = Double.POSITIVE_INFINITY;
	for (Map.Entry<String, VarPlanStats> entry : left.varStats.entrySet()) {
		VarPlanStats rightStats = right.varStats.get(entry.getKey());
		if (rightStats == null) {
			continue;
		}
		SharedVarEstimate shared = estimateSharedVarJoin(left.outputRows, right.baseRows, entry.getValue(),
				rightStats, disconnectedRows);
		sharedVars.put(entry.getKey(), shared);
		rawRows = Math.min(rawRows, shared.rows);
	}

	double rawWorkRows;
	if (sharedVars.isEmpty()) {
		rawRows = disconnectedRows;
		rawWorkRows = estimateDisconnectedJoinWorkRows(left.outputRows, right.baseRows);
	} else {
		rawRows = Math.min(rawRows, disconnectedRows);
		rawWorkRows = rawRows;
	}

	double outputRows = normalizeRows(applyFilterMultiplier(rawRows, right.localFilterMultiplier));
	double workRows = normalizeRows(applyFilterMultiplier(rawWorkRows, right.localFilterMultiplier));

	Map<String, VarPlanStats> mergedStats = new HashMap<>();
	for (Map.Entry<String, VarPlanStats> entry : left.varStats.entrySet()) {
		String varName = entry.getKey();
		SharedVarEstimate shared = sharedVars.get(varName);
		if (shared != null) {
			mergedStats.put(varName, new VarPlanStats(clampDistinct(shared.distinct, outputRows), shared.sketch));
		} else {
			mergedStats.put(varName, new VarPlanStats(clampDistinct(entry.getValue().distinct, outputRows),
					entry.getValue().sketch));
		}
	}
	for (Map.Entry<String, VarPlanStats> entry : right.varStats.entrySet()) {
		if (mergedStats.containsKey(entry.getKey())) {
			continue;
		}
		mergedStats.put(entry.getKey(),
				new VarPlanStats(clampDistinct(entry.getValue().distinct, outputRows), entry.getValue().sketch));
	}
	return new JoinStepEstimate(outputRows, workRows, mergedStats);
}
```

## 6) Update the call sites that still reference the old signature

### `cardinalityWithInitiallyBoundVars(...)`

```java
private double cardinalityWithInitiallyBoundVars(List<TupleExpr> tupleExprs, Set<String> initiallyBoundVars) {
	if (!isReady() || tupleExprs == null || tupleExprs.isEmpty()) {
		return -1;
	}

	Set<String> bound = initiallyBoundVars == null || initiallyBoundVars.isEmpty() ? Collections.emptySet()
			: Set.copyOf(initiallyBoundVars);
	TuplePlanEstimate currentEstimate = null;
	for (TupleExpr tupleExpr : tupleExprs) {
		TuplePlanEstimate nextEstimate = estimateTupleExprPlan(tupleExpr);
		if (nextEstimate == null) {
			return -1.0d;
		}
		if (!bound.isEmpty()) {
			nextEstimate = applyInitiallyBoundVars(nextEstimate, bound);
		}

		if (currentEstimate == null) {
			currentEstimate = nextEstimate;
			continue;
		}

		JoinStepEstimate step = estimateJoinStep(currentEstimate, nextEstimate);
		currentEstimate = new TuplePlanEstimate(step.outputRows, step.outputRows, 1.0d, step.varStats);
	}

	return currentEstimate == null ? -1.0d : normalizeRows(currentEstimate.outputRows);
}
```

### `estimateJoinCardinalityFromTuplePlans(...)`

```java
private double estimateJoinCardinalityFromTuplePlans(TupleExpr leftArg, TupleExpr rightArg, boolean leftJoin) {
	TuplePlanEstimate leftEstimate = estimateTupleExprPlan(leftArg);
	TuplePlanEstimate rightEstimate = estimateTupleExprPlan(rightArg);
	if (leftEstimate == null || rightEstimate == null) {
		return -1.0d;
	}
	JoinStepEstimate step = estimateJoinStep(leftEstimate, rightEstimate);
	double rows = leftJoin ? Math.max(leftEstimate.outputRows, step.outputRows) : step.outputRows;
	return normalizeRows(rows);
}
```

### `estimateJoinedTupleExprPlan(...)`

```java
private TuplePlanEstimate estimateJoinedTupleExprPlan(TupleExpr leftArg, TupleExpr rightArg, boolean leftJoin) {
	TuplePlanEstimate leftEstimate = estimateTupleExprPlan(leftArg);
	TuplePlanEstimate rightEstimate = estimateTupleExprPlan(rightArg);
	if (leftEstimate == null || rightEstimate == null) {
		return null;
	}
	JoinStepEstimate step = estimateJoinStep(leftEstimate, rightEstimate);
	double outputRows = leftJoin ? Math.max(leftEstimate.outputRows, step.outputRows) : step.outputRows;
	return new TuplePlanEstimate(outputRows, outputRows, 1.0d, step.varStats);
}
```

### `estimateTupleExprPlan(...)`

Replace the first lines with:

```java
private TuplePlanEstimate estimateTupleExprPlan(TupleExpr tupleExpr) {
	if (tupleExpr == null) {
		return null;
	}

	TuplePlanEstimate plannerEstimate = toPlannerTupleEstimate(tupleExpr, Collections.emptySet());
	if (plannerEstimate != null) {
		return plannerEstimate;
	}
```

## 7) Relax the visibility of the two nested DTOs used by the new reorderer and add row getters

Change:

```java
private static final class TuplePlanEstimate
```

to:

```java
static final class TuplePlanEstimate
```

and add:

```java
double outputRows() {
	return outputRows;
}
```

Change:

```java
private static final class JoinStepEstimate
```

to:

```java
static final class JoinStepEstimate
```

and add:

```java
double outputRows() {
	return outputRows;
}

double workRows() {
	return workRows;
}
```

That is enough surface area for `SketchJoinOrderReorderer` while keeping the join-estimation internals private.
