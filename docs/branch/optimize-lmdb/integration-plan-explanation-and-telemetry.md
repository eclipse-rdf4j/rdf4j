# Plan explanation, strategy decisions, and telemetry

This guide covers shared query-explanation model changes on the pinned branch
range `4aec7e9a2223d873b1c1a7703aad4c87bf8354df` →
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. LMDB strategy-specific
eligibility and dispatch details live in
[strategy arbitration and explanation](query-arbitration-and-explanation.md);
this page documents how decisions and execution facts cross from compilation
to RDF4J's common explanation tree and Workbench presentation.

## Explanation levels and preview boundary

`QueryExplanationContext` is an internal compilation-scoped `ThreadLocal`
context. Entering a context snapshots the supplied bindings and remembers a
previous context; closing restores that previous value, including when nested
compilation fails. The `Optimized` level marks the context as a preview. During
that preview, physical dispatch can record strategy decisions and inspect each
plan node once without making those diagnostics part of ordinary query
compilation. Candidate records whose eligibility depends on values only
available at runtime are explicitly rewritten as “Runtime dependent” rather
than presented as a compile-time prediction.

`SailSourceConnection.explain` clears stale actual metrics at entry, toggles
the requested telemetry/summary detail, and closes the context at exit. In an
optimized preview, it precompiles the plan but substitutes an empty iteration
instead of consuming query results. It attaches captured strategy decisions to
transient query-model metadata for conversion into the generic plan, then clears
that metadata in `finally`. This explains physical choices without claiming a
query was executed or returning its actual rows. The non-preview levels that
execute a query retain the existing behavior expected by their level and may
populate their runtime summaries.

This separation limits preview side effects and lets diagnostics state which
facts are static versus runtime dependent. Capturing metadata, candidate lists,
and rendered output still consumes allocation and formatting time during an
explicit explain request. Ordinary query runtime does not render this output;
the cost is tied to asking for explanation and to the physical planner's
decision probes.

## StrategyDecision and GenericPlanNode

The experimental `StrategyDecision` record contains a decision point,
timestamp, mode, proposed winner, fallback, reason, and a copied list of
candidates. Each candidate can report priority, whether it can be attempted,
decision/decline reason, and a condition. Null eligibility means the choice
depends on runtime values. Its formatter produces a text summary; the generic
plan node also renders decision rows in DOT/HTML output where applicable.

`GenericPlanNode` now carries an optional physical-plan prelude, the captured
decisions, planned engine/kind and native-path annotations, and execution
summary visibility separate from detailed runtime telemetry. The `Optimized`
level retains optimizer/physical-planning annotations without showing runtime
summary metrics. Executed, Timed, and Telemetry levels enable execution
summaries; Telemetry continues to carry detailed runtime metrics. The model
now treats zero-valued non-optimizer actual metrics as visible, allowing a
real zero to differ from “not collected”. Consumers should inspect the
explanation level and metric visibility rather than infer that a missing
metric is zero.

New decision metadata is identity-keyed/transient and is not part of an
optimizer cache key. `QueryExplanationContext.inspectOnce` also deduplicates
preview inspection by plan object identity. Those details make explain
instrumentation local to one compilation even when the same query-model objects
are visited through multiple planning paths.

Sources: [generic plan model](../../../core/query/src/main/java/org/eclipse/rdf4j/query/explanation/GenericPlanNode.java),
[preview context](../../../core/query/src/main/java/org/eclipse/rdf4j/query/explanation/QueryExplanationContext.java),
[decision record](../../../core/query/src/main/java/org/eclipse/rdf4j/query/explanation/StrategyDecision.java),
[metric names](../../../core/query/src/main/java/org/eclipse/rdf4j/query/explanation/TelemetryMetricNames.java),
[query-model conversion](../../../core/queryalgebra/model/src/main/java/org/eclipse/rdf4j/query/algebra/helpers/QueryModelTreeToGenericPlanNode.java),
[Sail explain boundary](../../../core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailSourceConnection.java).
Direct changed coverage includes
[`GenericPlanNodeTest`](../../../core/query/src/test/java/org/eclipse/rdf4j/query/explanation/GenericPlanNodeTest.java)
and `QueryModelTreeToGenericPlanNodeTest`.

## Telemetry and operator summaries

`TelemetryMetricNames` is a cross-module vocabulary, not a statement that every
engine emits every metric. The generic explanation distinguishes planned
annotations, optimizer-only metrics, execution-summary metrics, and detailed
runtime telemetry. Native-engine features may record planned execution engine
or kind before execution, plus a native execution path when selected. The exact
set of LMDB metrics and their producer-side meanings are enumerated in the
query guide; here the invariant is that a new metric needs both a stable name
and an explicit visibility/association policy so it does not appear on
irrelevant algebra nodes or at the wrong explanation level.

An optimized-preview decision is a snapshot “would select now” explanation,
not a guarantee that later runtime data will pick that candidate. Conditions
depending on bindings/cardinalities discovered during execution remain labeled
runtime dependent. Captured times are source data for the record; they are not
benchmark durations.

## Workbench and developer use

Workbench query-explanation UI changes display the physical prelude,
strategy-decision rows, and level-appropriate summary details in the result
views and styles. Template/model transformation output is the rendering path;
the JavaScript/TypeScript controls choose and request explanation levels.
Server and Workbench LMDB property panels are documented in
[runtime controls](integration-runtime-controls-and-workbench.md).

When extending an explanation, keep the evidence class explicit: an estimate
is planned, a recorded counter is actual only for an execution that collected
it, and a preview's runtime-dependent condition is not a selection result.
Clear old actual metrics before reuse, snapshot mutable caller state, restore
nested request context in `finally`, and remove transient metadata after plan
conversion. Do not add all detail to ordinary `toString()` output if it is only
useful in explicit explanations.
