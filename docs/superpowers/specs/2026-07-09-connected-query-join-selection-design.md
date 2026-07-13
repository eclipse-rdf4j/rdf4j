# Connected Query Join Selection Design

## Goal

Make the standard `QueryJoinOptimizer` prefer the cheapest remaining tuple expression that consumes an existing
binding, preventing an avoidable cross join while preserving the current cost model.

## Scope

The change applies to the greedy `selectNextTupleExpr` pass used by `StandardQueryOptimizerPipeline`. It does not
change cost estimation, join-tree construction, the optional pairwise join-estimation reorder, or the LMDB sketch
optimizer.

## Selection Rule

For each remaining tuple expression, calculate the existing cost once and determine whether it uses a variable in
the visitor's current `boundVars`. Selection is lexicographic:

1. Prefer expressions using an existing binding over expressions that do not.
2. Within that category, prefer the lowest calculated cost.
3. Preserve input order for equal costs.

When `boundVars` is empty, or no remaining expression uses one of those bindings, the optimizer selects the globally
cheapest expression exactly as it does today. This permits an unavoidable cross join only after the connected
component has been exhausted.

Statement-pattern variables come from the existing `varsMap`. Other tuple-expression forms fall back to their
reported binding names, covering expressions such as `BindingSetAssignment` without changing their cost semantics.

## Testing

Add one focused optimizer regression with three statement patterns:

- a cheap anchor selected first;
- an expensive pattern connected to the anchor;
- a numerically cheaper pattern disconnected from the anchor.

The current optimizer produces anchor, disconnected, connected. The new rule must produce anchor, connected,
disconnected. The final disconnected choice also proves that the optimizer falls back to global cost once no
connected candidate remains.

## Risk

The scan remains linear in the number of remaining expressions and adds no candidate-list allocation. The change is
localized and reversible. It intentionally prioritizes connectivity over numeric cost only when both choices exist.
