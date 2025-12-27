# UNION + OPTIONAL Rewrites

This document lists the safe rewrites for UNION and OPTIONAL, the required preconditions, and why each rewrite preserves SPARQL semantics.

## U1: UNION flattening

Preconditions: the feature flag is enabled and nested UNION nodes share the same variable scope change flag. UNION is associative under bag semantics if all arms are preserved and no deduplication is introduced.

Transformation: replace nested UNIONs with a single UNION that contains all arms in order. For example, Union(Union(A,B),C) becomes Union(A,B,C).

Why safe: UNION is multiset union; flattening is a structural change that preserves all arms and therefore preserves duplicates.

Counterexample: flattening that drops or merges identical arms would be unsafe. This optimizer must never deduplicate.

## U2: UNION arm reordering by estimated cost

Preconditions: optimizer flag enabled, estimates available, and the estimated cost ratio meets the minimum confidence threshold (`rdf4j.optimizer.unionOptional.unionReorder.minRatio`, default 1.5). Reordering must not change duplicate semantics or variable scoping.

Transformation: reorder UNION arms to evaluate the cheapest arm first, while preserving all arms.

Why safe: UNION evaluation order is not semantically observable in RDF4J's evaluation model; reordering does not add or remove rows.

Counterexample: any rewrite that changes the set of arms, or reorders within a subquery where evaluation order is externally observed, would be unsafe. This rule must be gated and limited to plain UNION arms.

## O1: OPTIONAL LHS-only join reordering

Status: currently enforced by existing `QueryJoinOptimizer` behavior and guarded by tests; no new optimizer rule has been added yet.

Preconditions: only reorder joins that are entirely within the left-hand side of a LeftJoin. Do not move any part of the right-hand side or the LeftJoin itself.

Transformation: use existing join reordering on the left subtree to reduce binding set size before entering OPTIONAL.

Why safe: the LeftJoin boundary is unchanged, so null-extension and correlation semantics remain intact. Only the order of evaluation of left-side joins is adjusted.

Counterexample: moving any right-hand side pattern into the left or outside the LeftJoin changes OPTIONAL semantics and is unsafe.

## O2: Safe filter handling around OPTIONAL

Status: currently enforced by existing `FilterOptimizer` behavior and guarded by tests; no new optimizer rule has been added yet.

Preconditions: filters referencing only left-side variables may be pushed into the left subtree. Filters referencing optional variables may only be pushed within the OPTIONAL if they are already syntactically scoped there.

Transformation: apply filter pushdown only when it does not cross the LeftJoin boundary. Do not move a filter from outside OPTIONAL into the optional RHS if it references optional variables.

Why safe: filters outside OPTIONAL that reference optional variables have different semantics than filters inside OPTIONAL. Preserving scope keeps results identical.

Counterexample: FILTER outside OPTIONAL referencing an optional variable, moved inside OPTIONAL, will change the result set by dropping unbound rows.

## Deferred rewrites

Any distributive rewrite between UNION and OPTIONAL is postponed until the harness and tests demonstrate a safe win. These rewrites require additional scoping and correlation analysis and are not part of the initial rollout.
